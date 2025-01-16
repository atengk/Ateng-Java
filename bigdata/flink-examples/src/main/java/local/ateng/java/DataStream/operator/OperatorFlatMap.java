package local.ateng.java.DataStream.operator;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 数据流转换 FlatMap
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/
 * DataStream → DataStream
 * 输入一个元素同时产生零个、一个或多个元素
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class OperatorFlatMap {

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
        SingleOutputStreamOperator<String> operator = streamSource
                .flatMap(new FlatMapFunction<String, String>() {
                    @Override
                    public void flatMap(String str, Collector<String> collector) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        String name = userInfoEntity.getName();
                        String city = userInfoEntity.getCity();
                        collector.collect(StrUtil.format("{}-{}", city, name));
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
