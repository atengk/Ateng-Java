package local.ateng.java.spark.sql.write;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

import java.util.Properties;

/**
 * 将查询的数据输出到Kafka
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class Kafka {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        // 设置应用程序的名称
        conf.setAppName("将查询的数据输出到KafkaL");
        // 指定hive仓库中的默认位置
        conf.set("spark.sql.warehouse.dir", "hdfs://server01:8020/hive/warehouse");
        // 设置运行环境
        String masterValue = conf.get("spark.master", "local[*]");
        conf.setMaster(masterValue);
        // 创建一个SparkSession对象，同时配置SparkConf，并启用Hive支持
        SparkSession spark = SparkSession
                .builder()
                .config(conf)
                .enableHiveSupport()
                .getOrCreate();

        // 执行SQL查询
        String sql = "SELECT \n" +
                "    province, \n" +
                "    COUNT(id) AS cnt, \n" +
                "    CURRENT_TIMESTAMP() AS create_time \n" +
                "FROM \n" +
                "    my_user\n" +
                "GROUP BY \n" +
                "    province;\n";
        Dataset<Row> ds = spark.sql(sql);

        // 将 DataFrame 转换为 JSON 格式（Kafka 需要值是二进制 JSON）
        Dataset<Row> messageDF = ds.selectExpr(
                "concat(CAST(CURRENT_TIMESTAMP AS STRING), '_', province) AS key",
                "to_json(struct(*)) AS value"
        );

        // 配置 Kafka 参数
        String kafkaServers = "192.168.1.10:9094";  // Kafka 集群地址
        String topic = "ateng_spark_output";  // Kafka 目标 Topic

        // 将结果写入 Kafka
        messageDF
                .write()
                .format("kafka") // 使用 Kafka 数据源格式
                .option("kafka.bootstrap.servers", kafkaServers)  // Kafka 服务器地址
                .option("topic", topic)  // Kafka 目标 Topic
                .save();  // 写入 Kafka

        // 停止SparkSession，释放资源
        spark.stop();
    }
}
