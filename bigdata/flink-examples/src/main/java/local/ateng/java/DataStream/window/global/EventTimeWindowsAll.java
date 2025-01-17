package local.ateng.java.DataStream.window.global;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.triggers.TriggerResult;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在普通 DataStream 上定义 Window。 Window 根据某些特征（例如，最近 5 秒内到达的数据）对所有流事件进行分组。
 * DataStream → AllWindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#%e6%bb%91%e5%8a%a8%e7%aa%97%e5%8f%a3sliding-windows
 *
 * 这段代码使用 Flink 从 Kafka 读取消息流
 * 并对消息进行基于事件时间的窗口处理。
 * 窗口大小为 60 秒（1 分钟），每 60 秒触发一次窗口计算，
 * 计算窗口中的数据并输出。
 * 例如，窗口中的第一个元素、最后一个元素、窗口大小以及当前时间都会被输出。
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-16
 */
public class EventTimeWindowsAll {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 定义水印策略：WatermarkStrategy 可以在 Flink 应用程序中的两处使用，第一种是直接在数据源上使用，第二种是直接在非数据源的操作之后使用。
        // 允许最多 5 秒的事件时间乱序，使用 createTime 字段为事件时间戳（毫秒）
        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner(
                        (event, recordTimestamp) -> {
                            // 解析 JSON 格式的事件，并获取事件时间
                            UserInfoEntity user = JSONObject.parseObject(event).toJavaObject(UserInfoEntity.class);
                            long timestamp = LocalDateTimeUtil.toEpochMilli(user.getCreateTime());
                            return timestamp;
                        });

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, watermarkStrategy, "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                // 设置水印策略为事件时间
                //.assignTimestampsAndWatermarks(watermarkStrategy)
                .windowAll(GlobalWindows.create())
                .trigger(new Trigger<String, GlobalWindow>() {
                    // 定义窗口长度为60秒
                    private static final long WINDOW_LENGTH = 60 * 1000;

                    @Override
                    public TriggerResult onElement(String element, long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 使用 PartitionedState 存储窗口的状态
                        ValueState<Long> lastTimerState = ctx.getPartitionedState(
                                new ValueStateDescriptor<>("lastTimer", Long.class)
                        );

                        // 如果窗口没有注册过定时器
                        if (lastTimerState.value() == null) {
                            // 计算下一次触发时间：事件时间 + 60秒
                            long nextTriggerTime = timestamp + WINDOW_LENGTH;

                            // 注册事件时间定时器，60秒后触发
                            ctx.registerEventTimeTimer(nextTriggerTime);

                            // 更新窗口状态，标记定时器已注册
                            lastTimerState.update(1L);

                            // 打印下一次定时器触发的时间（调试用）
                            System.out.println("下一次定时器触发时间: " + DateUtil.date(nextTriggerTime));
                        }

                        // 继续等待事件时间触发
                        return TriggerResult.CONTINUE;
                    }

                    @Override
                    public TriggerResult onProcessingTime(long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 不设置基于处理时间的触发
                        return TriggerResult.CONTINUE;
                    }

                    @Override
                    public TriggerResult onEventTime(long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 执行窗口计算的逻辑
                        System.out.println("事件时间触发，窗口计算中...");

                        // 清除当前窗口的定时器状态
                        ValueState<Long> lastTimerState = ctx.getPartitionedState(
                                new ValueStateDescriptor<>("lastTimer", Long.class)
                        );

                        // 删除窗口状态中的定时器信息
                        lastTimerState.clear();

                        // 在这里执行窗口计算的逻辑
                        return TriggerResult.FIRE_AND_PURGE; // 触发窗口计算并清除状态
                    }

                    @Override
                    public void clear(GlobalWindow window, TriggerContext ctx) throws Exception {

                    }

                })
                .apply(new AllWindowFunction<String, JSONObject, GlobalWindow>() {
                    @Override
                    public void apply(GlobalWindow globalWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        JSONObject json = JSONObject.of("maxTimestamp", globalWindow.maxTimestamp());
                        JSONArray jsonArray = JSONArray.of();
                        for (String string : iterable) {
                            jsonArray.add(JSONObject.parseObject(string));
                        }
                        int size = jsonArray.size();
                        json.put("data^", jsonArray.get(0));
                        json.put("data$", jsonArray.get(size - 1));
                        json.put("size", size);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
