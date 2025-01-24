package local.ateng.java.spark.sql.write;

import cn.hutool.core.util.StrUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

/**
 * 写入HDFS为Parquet文件
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class HDFS2Parquet {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        // 设置应用程序的名称
        conf.setAppName("写入HDFS为Parquet文件");
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
        String sql = "SELECT * FROM my_user WHERE province = '{}'";
        Dataset<Row> ds = spark.sql(StrUtil.format(sql, "重庆市"));

        // 将结果写入到 HDFS 的 Parquet 文件
        ds.write()
                .mode(SaveMode.Overwrite)
                .parquet("hdfs://server01:8020/data/spark/output");

        // 停止SparkSession，释放资源
        spark.stop();
    }
}
