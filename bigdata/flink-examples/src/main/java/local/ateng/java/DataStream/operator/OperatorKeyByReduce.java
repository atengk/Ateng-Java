package local.ateng.java.DataStream.operator;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 数据流转换 KeyBy Reduce
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/
 * DataStream → KeyedStream → DataStream
 * 在逻辑上将流划分为不相交的分区。具有相同 key 的记录都分配到同一个分区。在内部， keyBy() 是通过哈希分区实现的。有多种指定 key 的方式。
 * 在相同 key 的数据流上“滚动”执行 reduce。将当前元素与最后一次 reduce 得到的值组合然后输出新值。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class OperatorKeyByReduce {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
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

        // 从 Kafka 数据源读取数据，设置水印策略为 BoundedOutOfOrderness，最大乱序时间为 3 秒，命名为 "Kafka Source"
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(3)), "Kafka Source");

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
                .reduce(new ReduceFunction<JSONObject>() {
                    @Override
                    public JSONObject reduce(JSONObject json1, JSONObject json2) throws Exception {
                        return JSONObject.of(
                                "province", json1.getString("province"),
                                "score", json1.getDouble("score") + json2.getDouble("score"));
                    }
                });

        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
