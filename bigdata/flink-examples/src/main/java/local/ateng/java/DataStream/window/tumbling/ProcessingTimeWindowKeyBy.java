package local.ateng.java.DataStream.window.tumbling;

import cn.hutool.core.date.DateUtil;
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
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在已经分区的 KeyedStreams 上定义 Window。Window 根据某些特征（例如，最近 5 秒内到达的数据）对每个 key Stream 中的数据进行分组。
 * KeyedStream → WindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/#window
 *
 * 这个 Flink 程序的主要功能是：
 * 从 Kafka 读取 JSON 格式的用户数据，每条数据包含 province（省份）和 score（分数）信息。
 * 将数据按 province 字段进行分组，并基于处理时间（ProcessingTime）使用滚动窗口（每 1 分钟一个窗口）对每个省份的 score 进行聚合。
 * 每个窗口内，程序计算该省份的总分数，并输出窗口的开始时间、结束时间、总分数以及当前时间。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class ProcessingTimeWindowKeyBy {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(3);
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

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 算子
        SingleOutputStreamOperator<JSONObject> operator = streamSource
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
                .window(TumblingProcessingTimeWindows.of(Duration.ofMinutes(1)))
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
                            count ++;
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
