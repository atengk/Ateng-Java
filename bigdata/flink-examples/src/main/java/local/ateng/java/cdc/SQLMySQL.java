package local.ateng.java.cdc;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * MySQL CDC 连接器 #
 * MySQL CDC 连接器允许从 MySQL 数据库读取快照数据和增量数据。本文描述了如何设置 MySQL CDC 连接器来对 MySQL 数据库运行 SQL 查询。
 * https://nightlies.apache.org/flink/flink-cdc-docs-release-3.2/zh/docs/connectors/flink-sources/mysql-cdc/#datastream-source
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-21
 */
public class SQLMySQL {
    public static void main(String[] args) throws Exception {
        // 创建流式执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置并行度为 3
        env.setParallelism(3);
        // 创建流式表环境
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 创建表
        String createSql = "CREATE TABLE my_user (\n" +
                "    db_name STRING METADATA FROM 'database_name' VIRTUAL,\n" +
                "    table_name STRING METADATA  FROM 'table_name' VIRTUAL,\n" +
                "    operation_ts TIMESTAMP_LTZ(3) METADATA FROM 'op_ts' VIRTUAL,\n" +
                "    id BIGINT,\n" +
                "    name STRING,\n" +
                "    age INT,\n" +
                "    score DOUBLE,\n" +
                "    birthday TIMESTAMP(3),\n" +
                "    province STRING,\n" +
                "    city STRING,\n" +
                "    create_time TIMESTAMP(3),\n" +
                "    PRIMARY KEY(id) NOT ENFORCED\n" +
                ") WITH (\n" +
                "    'connector' = 'mysql-cdc',\n" +
                "    'scan.startup.mode' = 'latest-offset', -- 从最晚位点启动\n" +
                "    'hostname' = '192.168.1.10',\n" +
                "    'port' = '35725',\n" +
                "    'username' = 'root',\n" +
                "    'password' = 'Admin@123',\n" +
                "    'database-name' = 'kongyu_flink',\n" +
                "    'table-name' = 'my_user_mysql'\n" +
                ");";
        tableEnv.executeSql(createSql);

        // 查询数据
        String querySql = "select * from my_user";
        Table result = tableEnv.sqlQuery(querySql);

        // 执行操作
        TableResult tableResult = result.execute();

        // 打印结果
        tableResult.print();

        // 执行任务
        env.execute("Flink CDC MySQL 使用示例");

    }

}
