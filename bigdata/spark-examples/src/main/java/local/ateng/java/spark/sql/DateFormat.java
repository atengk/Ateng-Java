package local.ateng.java.spark.sql;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * 时间序列分析
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class DateFormat {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        // 设置应用程序的名称
        conf.setAppName("时间序列分析");
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
        Dataset<Row> ds = spark.sql("SELECT date_format(TO_DATE(birthday), 'yyyy-MM') AS date,\n" +
                "       SUM(score) AS score_sum,\n" +
                "       COUNT(id) AS id_count\n" +
                "FROM my_user\n" +
                "WHERE TO_DATE(birthday) BETWEEN '1990-01-01' AND '2000-01-01'\n" +
                "GROUP BY date_format(TO_DATE(birthday), 'yyyy-MM')\n" +
                "ORDER BY date DESC;");

        // 显示 DataFrame 的结构
        ds.printSchema();

        // 显示查询结果
        ds.show();

        // 停止SparkSession，释放资源
        spark.stop();
    }
}
