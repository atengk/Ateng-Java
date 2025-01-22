package local.ateng.java.flink.stream;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.flink.entity.UserInfoEntity;
import local.ateng.java.flink.function.MyGeneratorFunction;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.springframework.stereotype.Component;

/**
 * DataStream：生成模拟数据并写入Kafka
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-22
 */
@Component
public class DataStreamGeneratorToKafka {

    public void run() throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = SpringUtil.getBean("flinkEnv", StreamExecutionEnvironment.class);
        // 启用检查点，设置检查点间隔为 5 秒，检查点模式为 精准一次
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        // 设置并行度为 3
        env.setParallelism(3);

        // 创建 DataGeneratorSource 生成模拟数据
        DataGeneratorSource<UserInfoEntity> source = new DataGeneratorSource<>(
                new MyGeneratorFunction(), // 自定义的生成器函数
                Long.MAX_VALUE, // 生成数据的数量
                RateLimiterStrategy.perSecond(10), // 生成数据的速率限制
                TypeInformation.of(UserInfoEntity.class) // 数据类型信息
        );

        // 将生成的 UserInfoEntity 对象转换为 JSON 字符串
        SingleOutputStreamOperator<String> stream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Generator Source")
                .map(user -> JSONObject.toJSONString(user));

        // 配置 KafkaSink 将数据发送到 Kafka 中
        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("192.168.1.10:9094") // Kafka 服务器地址和端口
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("ateng_flink_json") // Kafka 主题
                        .setValueSerializationSchema(new SimpleStringSchema()) // 数据序列化方式
                        .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE) // 传输保障级别
                .build();

        // 将数据发送到 Kafka
        stream.sinkTo(sink);

        // 执行程序
        env.execute("生成模拟数据并写入Kafka");
    }

}
