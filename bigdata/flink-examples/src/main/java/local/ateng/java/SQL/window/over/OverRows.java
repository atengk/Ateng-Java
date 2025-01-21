package local.ateng.java.SQL.window.over;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * Over聚合（行间隔）
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-21
 */
public class OverRows {
    public static void main(String[] args) throws Exception {
        // 创建流式执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 启用检查点，设置检查点间隔为 5 秒，检查点模式为 精准一次
        //env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        // 设置并行度为 3
        env.setParallelism(3);
        // 创建流式表环境
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

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
                "  createTime TIMESTAMP(3),\n" +
                "  procTime AS PROCTIME() \n" +
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

        // 查询数据
        // Over聚合（行间隔）
        // ROWS 间隔基于计数。它定义了聚合操作包含的精确行数。下面的 ROWS 间隔定义了当前行 + 之前的 100 行（也就是101行）都会被聚合。
        String querySql = "SELECT\n" +
                "  avg(score) OVER w as avg_score,\n" +
                "  max(age) OVER w as age_max,\n" +
                "  count(id) OVER w as id_count\n" +
                "FROM my_user_kafka\n" +
                "WINDOW w AS (\n" +
                "  ORDER BY procTime\n" +
                "  ROWS BETWEEN 100 PRECEDING AND CURRENT ROW)";
        Table result = tableEnv.sqlQuery(querySql);

        // 执行操作
        TableResult tableResult = result.execute();

        // 打印结果
        tableResult.print();

        // 执行任务
        env.execute("Over聚合（行间隔）");

    }
}
