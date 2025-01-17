package local.ateng.java.DataStream.window.session;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在普通 DataStream 上定义 Window。 Window 根据某些特征（例如，最近 5 秒内到达的数据）对所有流事件进行分组。
 * DataStream → AllWindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#%e6%bb%91%e5%8a%a8%e7%aa%97%e5%8f%a3sliding-windows
 *
 * 这段代码的功能如下：
 * 从 Kafka 主题读取数据流，以字符串形式消费事件。
 * 使用 事件时间会话窗口，窗口间隔为 2 分钟：
 * 如果事件之间的时间间隔超过 2 分钟，则窗口关闭，新窗口开启。
 * 每个窗口触发时，提取窗口的时间范围、数据数量、首尾事件，并构建一个结果 JSON 输出。
 * 最终的窗口处理结果打印到控制台。
 * 这种会话窗口特别适合处理 非固定间隔的数据流，如用户会话数据分析或系统事件日志聚合。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
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
                // 会话窗口：2分钟窗口数据
                // 表示会话窗口的间隔为 2 分钟。也就是说，窗口会根据事件之间的时间间隔划分。如果两个事件之间的时间差超过了 2 分钟，那么当前的会话窗口就结束，接下来的事件会形成新的窗口。
                .windowAll(EventTimeSessionWindows.withGap(Duration.ofMinutes(2)))
                .apply(new AllWindowFunction<String, JSONObject, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow timeWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
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
