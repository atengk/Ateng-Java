package local.ateng.java.DataStream.sink;

import cn.hutool.core.bean.BeanUtil;
import local.ateng.java.function.MyGeneratorFunction;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.opensearch.sink.FlushBackoffType;
import org.apache.flink.connector.opensearch.sink.OpensearchSink;
import org.apache.flink.connector.opensearch.sink.OpensearchSinkBuilder;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.http.HttpHost;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.Requests;

import java.util.Map;

/**
 * 数据生成连接器，用于生成模拟数据并将其输出到OpenSearch中
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-14
 */
public class DataGeneratorOpenSearch {
    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置并行度为 1，仅用于简化示例
        env.setParallelism(1);

        // 创建数据生成器源，生成器函数为 MyGeneratorFunction，每秒生成 1000 条数据，速率限制为 3 条/秒
        DataGeneratorSource<UserInfoEntity> source = new DataGeneratorSource<>(
                new MyGeneratorFunction(),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(10),
                TypeInformation.of(UserInfoEntity.class)
        );

        // 将数据生成器源添加到流中
        DataStreamSource<UserInfoEntity> stream =
                env.fromSource(source,
                        WatermarkStrategy.noWatermarks(), // 不生成水印，仅用于演示
                        "Generator Source");

        // Sink
        OpensearchSink<UserInfoEntity> sink = new OpensearchSinkBuilder<UserInfoEntity>()
                .setHosts(new HttpHost("192.168.1.12", 20018, "http"))
                .setEmitter(
                        (element, context, indexer) ->
                                indexer.add(createIndexRequest(element)))
                .setBulkFlushMaxActions(1000)
                .setBulkFlushInterval(3000)
                .setBulkFlushMaxSizeMb(10)
                .setBulkFlushBackoffStrategy(FlushBackoffType.EXPONENTIAL, 5, 1000)
                .build();

        // 将数据打印到控制台
        stream.print("sink opensearch");
        // 写入数据
        stream.sinkTo(sink);

        // 执行 Flink 作业
        env.execute("生成模拟数据并写入OpenSearch");
    }

    private static IndexRequest createIndexRequest(UserInfoEntity element) {
        Map map = BeanUtil.toBean(element, Map.class);
        return Requests.indexRequest()
                //.id(String.valueOf(element.getId()))
                .index("my_user")
                .source(map);
    }
}
