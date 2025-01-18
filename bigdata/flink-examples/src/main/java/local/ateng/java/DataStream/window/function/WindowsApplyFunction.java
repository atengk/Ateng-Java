package local.ateng.java.DataStream.window.function;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * WindowFunction（已过时）
 * 在某些可以使用 ProcessWindowFunction 的地方，你也可以使用 WindowFunction。
 * 它是旧版的 ProcessWindowFunction，只能提供更少的环境信息且缺少一些高级的功能，比如 per-window state。 这个接口会在未来被弃用。
 *
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#windowfunction%e5%b7%b2%e8%bf%87%e6%97%b6
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-18
 */
public class WindowsApplyFunction {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
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

        // 窗口
        SingleOutputStreamOperator<Tuple1<String>> operator = streamSource
                .map(new MapFunction<String, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return Tuple2.of(userInfoEntity.getProvince(), userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, Double>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Double> t) throws Exception {
                        return t.f0;
                    }
                })
                // 1分钟滚动窗口
                .window(TumblingProcessingTimeWindows.of(Duration.ofMinutes(1)))
                .apply(new WindowFunction<Tuple2<String, Double>, Tuple1<String>, String, TimeWindow>() {
                    @Override
                    public void apply(String key, TimeWindow timeWindow, Iterable<Tuple2<String, Double>> iterable, Collector<Tuple1<String>> collector) throws Exception {
                        long windowStart = timeWindow.getStart();  // 窗口的开始时间
                        long windowEnd = timeWindow.getEnd();      // 窗口的结束时间
                        Double score = 0.0;
                        for (Tuple2<String, Double> t : iterable) {
                            score += t.f1;
                        }
                        String string = StrUtil.format("windowStart={},windowEnd={},data={}",
                                DateUtil.date(windowStart), DateUtil.date(windowEnd),
                                key + ":" + score);
                        collector.collect(Tuple1.of(string));
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
