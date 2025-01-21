package local.ateng.java.cdc;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.cdc.connectors.mysql.source.MySqlSource;
import org.apache.flink.cdc.connectors.mysql.table.StartupOptions;
import org.apache.flink.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * MySQL CDC 连接器 #
 * MySQL CDC 连接器允许从 MySQL 数据库读取快照数据和增量数据。本文描述了如何设置 MySQL CDC 连接器来对 MySQL 数据库运行 SQL 查询。
 * https://nightlies.apache.org/flink/flink-cdc-docs-release-3.2/zh/docs/connectors/flink-sources/mysql-cdc/#datastream-source
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-21
 */
public class DataStreamMySQLCDC {

    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 启用检查点，设置检查点间隔为 5 秒，检查点模式为 精准一次
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        // 设置并行度为 3
        env.setParallelism(3);

        // 创建 MySQL 数据源
        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("192.168.1.10")
                .port(35725)
                .scanNewlyAddedTableEnabled(true) // 启用扫描新添加的表功能
                .databaseList("kongyu_flink") // 设置捕获的数据库， 如果需要同步整个数据库，请将 tableList 设置为 ".*".
                .tableList("kongyu_flink.my_user_mysql") // 设置捕获的表
                .username("root")
                .password("Admin@123")
                .startupOptions(StartupOptions.latest()) // 从最晚位点启动
                .deserializer(new JsonDebeziumDeserializationSchema()) // 将 SourceRecord 转换为 JSON 字符串
                .build();

        // 从 source 中读取数据
        DataStreamSource<String> dataStream = env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source");

        // 打印计算结果
        dataStream.print("output");

        // 执行流处理作业
        env.execute("Flink CDC MySQL 使用示例");

    }

}
