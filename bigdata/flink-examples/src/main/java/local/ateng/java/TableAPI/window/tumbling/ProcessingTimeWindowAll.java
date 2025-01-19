package local.ateng.java.TableAPI.window.tumbling;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import static org.apache.flink.table.api.Expressions.$;
import static org.apache.flink.table.api.Expressions.lit;

/**
 * 基于处理时间的windowAll滚动窗口
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-18
 */
public class ProcessingTimeWindowAll {
    public static void main(String[] args) throws Exception {
        // 创建流式执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 启用检查点，设置检查点间隔为 5 秒，检查点模式为 精准一次
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        // 设置并行度为 3
        env.setParallelism(3);

        // 创建流式表环境
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 使用 TableDescriptor 定义 Kafka 数据源
        TableDescriptor sourceDescriptor = TableDescriptor.forConnector("kafka")
                .schema(Schema.newBuilder()
                        .column("id", DataTypes.BIGINT())
                        .column("name", DataTypes.STRING())
                        .column("age", DataTypes.INT())
                        .column("score", DataTypes.DOUBLE())
                        .column("birthday", DataTypes.TIMESTAMP())
                        .column("province", DataTypes.STRING())
                        .column("city", DataTypes.STRING())
                        .column("createTime", DataTypes.TIMESTAMP(3)) // 事件时间字段
                        .columnByExpression("proc_time", "PROCTIME()") // 处理时间字段
                        .columnByMetadata("timestamp", DataTypes.TIMESTAMP(3))  // Kafka 的时间戳
                        .columnByMetadata("partition", DataTypes.INT())
                        .columnByMetadata("offset", DataTypes.BIGINT())
                        .build())
                .option("topic", "ateng_flink_json")  // Kafka topic 名称
                .option("properties.group.id", "ateng_flink_table_api")  // 消费者组 名称
                .option("properties.bootstrap.servers", "192.168.1.10:9094")  // Kafka 地址
                .option("format", "json")  // 数据格式，假设 Kafka 中的数据是 JSON 格式
                // 'earliest-offset', 'latest-offset', 'group-offsets', 'timestamp' and 'specific-offsets'
                .option("scan.startup.mode", "latest-offset")  // 从最早的偏移量开始消费
                .build();

        // 创建一个临时表 'my_user'，这个表通过 data generator 连接器读取数据
        tableEnv.createTemporaryTable("my_user", sourceDescriptor);

        // 使用 Table API 读取表
        Table table = tableEnv.from("my_user");

        // 定义窗口操作
        Table windowedTable = table
                .window(Tumble.over(lit(1).minutes()).on($("proc_time")).as("w")) // 定义 1 分钟滚动窗口
                .groupBy($("w")) // 窗口分组实现windowAll
                .select(
                        $("w").start().as("window_start"), // 窗口开始时间
                        $("w").end().as("window_end"), // 窗口结束时间
                        $("score").avg().as("avg_score"), // 平均分
                        $("age").max().as("max_age"), // 最大年龄
                        $("id").count().as("user_count") // 用户数量
                );

        // 执行操作
        TableResult tableResult = windowedTable.execute();

        // 打印结果
        tableResult.print();

        // 执行任务
        env.execute("处理时间滚动窗口");

    }
}
