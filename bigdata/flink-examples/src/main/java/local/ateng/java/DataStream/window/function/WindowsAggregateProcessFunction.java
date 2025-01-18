package local.ateng.java.DataStream.window.function;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
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
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 增量聚合的 ProcessWindowFunction
 * ProcessWindowFunction 可以与 ReduceFunction 或 AggregateFunction 搭配使用， 使其能够在数据到达窗口的时候进行增量聚合。当窗口关闭时，ProcessWindowFunction 将会得到聚合的结果。
 * 这样它就可以增量聚合窗口的元素并且从 ProcessWindowFunction` 中获得窗口的元数据。
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#%e5%a2%9e%e9%87%8f%e8%81%9a%e5%90%88%e7%9a%84-processwindowfunction
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-18
 */
public class WindowsAggregateProcessFunction {

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
                .aggregate(new AggregateFunction<Tuple2<String, Double>, Tuple2<String, Double>, Tuple2<String, Double>>() {

                    // 初始化聚合值，返回初始状态
                    @Override
                    public Tuple2<String, Double> createAccumulator() {
                        return Tuple2.of("", 0.0);
                    }

                    // 增量聚合阶段：根据每个输入值更新聚合值
                    @Override
                    public Tuple2<String, Double> add(Tuple2<String, Double> t1, Tuple2<String, Double> t2) {
                        return Tuple2.of(t1.f0, t1.f1 + t2.f1);
                    }

                    // 最终聚合阶段：输出聚合结果
                    @Override
                    public Tuple2<String, Double> getResult(Tuple2<String, Double> t) {
                        return t;
                    }

                    // 合并聚合状态：如果窗口分为多个子窗口，这里合并不同的状态
                    @Override
                    public Tuple2<String, Double> merge(Tuple2<String, Double> t1, Tuple2<String, Double> t2) {
                        return Tuple2.of(t1.f0, t1.f1 + t2.f1);
                    }
                }, new ProcessWindowFunction<Tuple2<String, Double>, Tuple1<String>, String, TimeWindow>() {
                    @Override
                    public void process(String key, Context context, Iterable<Tuple2<String, Double>> iterable, Collector<Tuple1<String>> collector) throws Exception {
                        long windowStart = context.window().getStart();  // 窗口的开始时间
                        long windowEnd = context.window().getEnd();      // 窗口的结束时间
                        long currentProcessingTime = context.currentProcessingTime();  // 获取当前处理时间
                        long currentWatermark = context.currentWatermark();  // 获取当前水位线
                        // ReduceFunction 已经聚合的结果通过 ProcessWindowFunction 处理，所有iterable这里只有一个值
                        Tuple2<String, Double> t = iterable.iterator().next();
                        String string = StrUtil.format("windowStart={},windowEnd={},currentProcessingTime={},currentWatermark={},data={},size={}",
                                DateUtil.date(windowStart), DateUtil.date(windowEnd),
                                DateUtil.date(currentProcessingTime), DateUtil.date(currentWatermark),
                                key + ":" + t.f0 + ":" + t.f1, iterable.spliterator().estimateSize());
                        collector.collect(Tuple1.of(string));
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
