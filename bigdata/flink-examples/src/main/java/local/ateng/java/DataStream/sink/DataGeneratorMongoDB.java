package local.ateng.java.DataStream.sink;

import com.alibaba.fastjson2.JSONObject;
import com.mongodb.client.model.InsertOneModel;
import local.ateng.java.function.MyGeneratorFunction;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.mongodb.sink.MongoSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.bson.BsonDocument;

/**
 * 数据生成连接器，用于生成模拟数据并将其输出到MongoDB中
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-14
 */
public class DataGeneratorMongoDB {
    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置并行度为 1，仅用于简化示例
        env.setParallelism(1);

        // 创建数据生成器源，生成器函数为 MyGeneratorFunction，每秒生成 1000 条数据，速率限制为 3 条/秒
        DataGeneratorSource<UserInfoEntity> source = new DataGeneratorSource<>(
                new MyGeneratorFunction(),
                1000,
                RateLimiterStrategy.perSecond(3),
                TypeInformation.of(UserInfoEntity.class)
        );

        // 将数据生成器源添加到流中
        DataStreamSource<UserInfoEntity> stream =
                env.fromSource(source,
                        WatermarkStrategy.noWatermarks(), // 不生成水印，仅用于演示
                        "Generator Source");

        // MongoDB Sink
        MongoSink<UserInfoEntity> sink = MongoSink.<UserInfoEntity>builder()
                .setUri("mongodb://root:Admin%40123@192.168.1.10:33627")
                .setDatabase("kongyu_flink")
                .setCollection("my_user")
                .setBatchSize(1000)
                .setBatchIntervalMs(3000)
                .setMaxRetries(3)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .setSerializationSchema(
                        (input, context) -> new InsertOneModel<>(BsonDocument.parse(JSONObject.toJSONString(input))))
                .build();

        // 将数据打印到控制台
        stream.print("sink mongodb");
        // 写入数据
        stream.sinkTo(sink);

        // 执行 Flink 作业
        env.execute("生成模拟数据并写入MongoDB");
    }
}
