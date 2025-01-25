package local.ateng.java.SQL.catalog;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * 创建HiveCatalog
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/table/hive/overview/
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-25
 */
public class Hive {
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
        tableEnv.getConfig().set("pipeline.name", "创建HiveCatalog");

        // 创建HiveCatalog
        tableEnv.executeSql("CREATE CATALOG hive_catalog WITH (\n" +
                "    'type' = 'hive',\n" +
                "    'default-database' = 'my_database',\n" +
                "    'hive-conf-dir' = 'hdfs://server01:8020/hive/conf'\n" +
                ");");
        tableEnv.executeSql("USE CATALOG hive_catalog;");

        // 执行SQL操作
        tableEnv.executeSql("SHOW TABLES").print();
    }
}
