package local.ateng.java.SQL.catalog;

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
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * 创建IcebergCatalog并写入模拟数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-25
 */
public class Iceberg {
    public static void main(String[] args) throws Exception {
        // 创建流式执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 启用检查点，设置检查点间隔为 120 秒，检查点模式为 精准一次
        env.enableCheckpointing(120 * 1000, CheckpointingMode.EXACTLY_ONCE);
        // 设置并行度为 3
        env.setParallelism(3);
        // 创建流式表环境
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
        // 设置 JobName
        tableEnv.getConfig().set("pipeline.name", "创建IcebergCatalog并写入模拟数据");

        // 创建IcebergCatalog
        tableEnv.executeSql("CREATE CATALOG iceberg_catalog\n" +
                "WITH (\n" +
                "    'type'='iceberg',\n" +
                "    'catalog-impl'='org.apache.iceberg.jdbc.JdbcCatalog',\n" +
                "    'io-impl'='org.apache.iceberg.aws.s3.S3FileIO',\n" +
                "    'uri'='jdbc:postgresql://192.168.1.10:32297/iceberg?user=postgres&password=Lingo@local_postgresql_5432',\n" +
                "    'warehouse'='s3://iceberg-bucket/warehouse',\n" +
                "    's3.endpoint'='http://192.168.1.13:9000'\n" +
                ");");
        tableEnv.executeSql("USE CATALOG iceberg_catalog;");

        // 创建数据库和表
        tableEnv.executeSql("CREATE DATABASE IF NOT EXISTS flink;");
        tableEnv.executeSql("USE flink;");
        tableEnv.executeSql("CREATE TABLE IF NOT EXISTS flink.my_user (\n" +
                "  id BIGINT NOT NULL,\n" +
                "  name STRING,\n" +
                "  age INT,\n" +
                "  score DOUBLE,\n" +
                "  birthday TIMESTAMP(3),\n" +
                "  province STRING,\n" +
                "  city STRING,\n" +
                "  create_time TIMESTAMP_LTZ(3)\n" +
                ") WITH (\n" +
                "  'write.format.default' = 'parquet'\n" +
                ");");

        // 查询表
        tableEnv.executeSql("SHOW TABLES").print();

        // 创建数据生成器源，生成器函数为 MyGeneratorFunction，生成 Long.MAX_VALUE 条数据，速率限制为 1000 条/秒
        DataGeneratorSource<UserInfoEntity> source = new DataGeneratorSource<>(
                new MyGeneratorFunction(),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(1000),
                TypeInformation.of(UserInfoEntity.class)
        );
        // 将数据生成器源添加到流中
        DataStreamSource<UserInfoEntity> stream =
                env.fromSource(source,
                        WatermarkStrategy.noWatermarks(),
                        "Generator Source");

        // 将 DataStream 注册为动态表
        tableEnv.createTemporaryView("default_catalog.default_database.my_user", stream,
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

        // 写入数据到Iceberg表中
        String querySql = "insert into my_user\n" +
                "select\n" +
                "  id,\n" +
                "  name,\n" +
                "  age,\n" +
                "  score,\n" +
                "  birthday,\n" +
                "  province,\n" +
                "  city,\n" +
                "  createTime\n" +
                "from\n" +
                "default_catalog.default_database.my_user;";
        tableEnv.executeSql(querySql);

        // 查询数据
        tableEnv.sqlQuery("select * from default_catalog.default_database.my_user").execute().print();

    }
}
