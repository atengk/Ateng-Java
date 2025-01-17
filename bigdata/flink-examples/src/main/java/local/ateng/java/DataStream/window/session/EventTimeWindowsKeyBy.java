package local.ateng.java.DataStream.window.session;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在已经分区的 KeyedStreams 上定义 Window。Window 根据某些特征（例如，最近 5 秒内到达的数据）对每个 key Stream 中的数据进行分组。
 * KeyedStream → WindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/#window
 *
 * 这段代码实现了以下功能：
 * Kafka 数据读取：从 Kafka 消费 JSON 格式事件数据。
 * 数据分组：按 province 分组，独立处理每个省份的数据流。
 * 会话窗口：基于 事件时间 的 2 分钟会话窗口。
 * 窗口聚合：
 * 累计每个窗口内的 score 总和和事件数量。
 * 提取窗口时间范围和分组键。
 * 结果输出：将每个窗口的聚合结果输出到控制台。
 * 这种逻辑常用于场景：
 * 实时分析用户行为，按地区（province）统计得分。
 * 分析会话行为，比如用户活跃时间段的统计。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class EventTimeWindowsKeyBy {

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
                .map(new MapFunction<String, JSONObject>() {
                    @Override
                    public JSONObject map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return JSONObject.of(
                                "province", userInfoEntity.getProvince(),
                                "score", userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<JSONObject, String>() {
                    @Override
                    public String getKey(JSONObject jsonObject) throws Exception {
                        return jsonObject.getString("province");
                    }
                })
                // 会话窗口：2分钟窗口数据
                // 表示会话窗口的间隔为 2 分钟。也就是说，窗口会根据事件之间的时间间隔划分。如果两个事件之间的时间差超过了 2 分钟，那么当前的会话窗口就结束，接下来的事件会形成新的窗口。
                .window(EventTimeSessionWindows.withGap(Duration.ofMinutes(2)))
                .apply(new WindowFunction<JSONObject, JSONObject, String, TimeWindow>() {
                    @Override
                    public void apply(String str, TimeWindow timeWindow, Iterable<JSONObject> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        Double score = 0.0;
                        Long count = 0L;
                        for (JSONObject jsonObject : iterable) {
                            score += jsonObject.getDouble("score");
                            count++;
                        }
                        json.put("province", str);
                        json.put("score", score);
                        json.put("count", count);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
