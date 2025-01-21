package local.ateng.java.SQL.window.sliding;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * FlinkSQL基于处理时间的滑动窗口使用示例
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-20
 */
public class ProcessingTimeWindowGroupBy {
    public static void main(String[] args) throws Exception {
        // 创建流式执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 启用检查点，设置检查点间隔为 5 秒，检查点模式为 精准一次
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
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
        // 滑动窗口：2分钟窗口数据，1分钟刷新一次数据（整个数据区间就是前2分钟）
        String querySql = "SELECT\n" +
                "  window_start,\n" +
                "  window_end,\n" +
                "  window_time,\n" +
                "  province,\n" +
                "  avg(score) as avg_score,\n" +
                "  max(age) as age_max,\n" +
                "  count(id) as id_count\n" +
                "FROM TABLE(\n" +
                "  HOP(\n" +
                "    TABLE my_user_kafka,\n" +
                "    DESCRIPTOR(procTime),\n" +
                "    INTERVAL '1' MINUTE,\n" +
                "    INTERVAL '2' MINUTE\n" +
                "  )\n" +
                ")\n" +
                "GROUP BY window_start, window_end, window_time, province;";
        Table result = tableEnv.sqlQuery(querySql);

        // 执行操作
        TableResult tableResult = result.execute();

        // 打印结果
        tableResult.print();

        // 执行任务
        env.execute("FlinkSQL基于处理时间的滑动窗口使用示例");

    }
}
