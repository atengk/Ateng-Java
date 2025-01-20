package local.ateng.java.SQL.SourceAndSink;

import local.ateng.java.SQL.entity.UserInfoEntity;
import local.ateng.java.SQL.function.MyGeneratorFunction;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class Kafka {
    public static void main(String[] args) throws Exception {
        // 创建流式执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置并行度为 3
        env.setParallelism(3);
        // 创建流式表环境
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 创建数据生成器源，生成器函数为 MyGeneratorFunction，生成 Long.MAX_VALUE 条数据，速率限制为 3 条/秒
        DataGeneratorSource<UserInfoEntity> source = new DataGeneratorSource<>(
                new MyGeneratorFunction(),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(3),
                TypeInformation.of(UserInfoEntity.class)
        );
        // 将数据生成器源添加到流中
        DataStreamSource<UserInfoEntity> stream =
                env.fromSource(source,
                        WatermarkStrategy.noWatermarks(),
                        "Generator Source");

        // 将 DataStream 注册为动态表
        tableEnv.createTemporaryView("my_user", stream,
                Schema.newBuilder()
                        .column("id", DataTypes.BIGINT())
                        .column("name", DataTypes.STRING())
                        .column("age", DataTypes.INT())
                        .column("score", DataTypes.DOUBLE())
                        .column("birthday", DataTypes.TIMESTAMP(3))
                        .column("province", DataTypes.STRING())
                        .column("city", DataTypes.STRING())
                        .column("createTime", DataTypes.TIMESTAMP(3))
                        .build());

        // 创建表
        String createSql = "CREATE TABLE my_user_kafka( \n" +
                "  my_event_time TIMESTAMP(3) METADATA FROM 'timestamp' VIRTUAL,\n" +
                "  my_partition BIGINT METADATA FROM 'partition' VIRTUAL,\n" +
                "  my_offset BIGINT METADATA FROM 'offset' VIRTUAL,\n" +
                "  id BIGINT NOT NULL,\n" +
                "  name STRING,\n" +
                "  age INT,\n" +
                "  score DOUBLE,\n" +
                "  birthday TIMESTAMP(3),\n" +
                "  province STRING,\n" +
                "  city STRING,\n" +
                "  createTime TIMESTAMP(3)\n" +
                ")\n" +
                "WITH (\n" +
                "  'connector' = 'kafka',\n" +
                "  'properties.bootstrap.servers' = '192.168.1.10:9094',\n" +
                "  'properties.group.id' = 'ateng_sql',\n" +
                "  -- 'earliest-offset', 'latest-offset', 'group-offsets', 'timestamp' and 'specific-offsets'\n" +
                "  'scan.startup.mode' = 'latest-offset',\n" +
                "  'topic' = 'ateng_flink_json',\n" +
                "  'format' = 'json'\n" +
                ");";
        tableEnv.executeSql(createSql);

        // 写入数据到目标表
        String insertSql = "insert into my_user_kafka select * from my_user;";
        tableEnv.executeSql(insertSql);

        // 查询数据
        String querySql= "select * from my_user_kafka";
        Table result = tableEnv.sqlQuery(querySql);

        // 执行操作
        TableResult tableResult = result.execute();

        // 打印结果
        tableResult.print();

        // 执行任务
        env.execute("Kafka使用示例");

    }
}
