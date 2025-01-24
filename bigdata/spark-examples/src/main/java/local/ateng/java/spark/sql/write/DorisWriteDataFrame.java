package local.ateng.java.spark.sql.write;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

/**
 * 将查询的数据输出到Doris
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class DorisWriteDataFrame {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        // 设置应用程序的名称
        conf.setAppName("将查询的数据输出到Doris");
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

        // 创建Doris视图
        ds.write().format("doris")
                .option("doris.table.identifier", "kongyu.sink_my_user_spark")
                .option("doris.fenodes", "192.168.1.12:9040")
                .option("user", "admin")
                .option("password", "Admin@123")
                .mode(SaveMode.Append)
                .save();

        // 停止SparkSession，释放资源
        spark.stop();
    }
}
