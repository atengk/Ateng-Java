package local.ateng.java.TableAPI.query;

import org.apache.flink.connector.datagen.table.DataGenConnectorOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import static org.apache.flink.table.api.Expressions.*;

public class TableAPIQueryAll {
    public static void main(String[] args) throws Exception {
        // 创建流式执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置并行度为 1
        env.setParallelism(1);

        // 创建流式表环境
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 使用 TableDescriptor 定义数据源
        final TableDescriptor sourceDescriptor = TableDescriptor.forConnector("datagen")
                .schema(Schema.newBuilder()
                        .column("id", DataTypes.BIGINT())
                        .column("name", DataTypes.STRING())
                        .column("age", DataTypes.INT())
                        .column("score", DataTypes.DOUBLE())
                        .column("birthday ", DataTypes.DATE())
                        .column("province", DataTypes.STRING())
                        .column("city", DataTypes.STRING())
                        .column("create_time", DataTypes.TIMESTAMP_LTZ())
                        .build())
                .option(DataGenConnectorOptions.ROWS_PER_SECOND, 1L)  // 每秒生成1条数据
                // 配置字段的生成约束
                .option("fields.id.min", "1")  // id 最小值
                .option("fields.id.max", "1000")  // id 最大值
                .option("fields.name.length", "10")  // name 字符串长度 10
                .option("fields.age.min", "18")  // age 最小值 18
                .option("fields.age.max", "80")  // age 最大值 80
                .option("fields.score.min", "50.0")  // score 最小值 50.0
                .option("fields.score.max", "100.0")  // score 最大值 100.0
                .option("fields.province.length", "5")  // province 字符串长度 5
                .option("fields.city.length", "5")  // city 字符串长度 5
                .build();

        // 创建一个临时表 'my_user'，这个表通过 data generator 连接器读取数据
        tableEnv.createTemporaryTable("my_user", sourceDescriptor);

        // 使用 Table API 读取表
        Table table = tableEnv.from("my_user");

        // 选择表中的所有列
        Table result = table.select($("*"));

        // 执行操作
        TableResult tableResult = result.execute();

        // 打印结果
        tableResult.print();

        // 执行任务
        env.execute("TableAPI使用示例");
    }
}
