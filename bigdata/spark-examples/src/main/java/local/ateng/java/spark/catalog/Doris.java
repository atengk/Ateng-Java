package local.ateng.java.spark.catalog;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;

/**
 * Spark Doris Catalog
 * https://doris.apache.org/zh-CN/docs/ecosystem/spark-doris-connector#spark-doris-catalog
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class Doris {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        // 设置应用程序的名称
        conf.setAppName("将查询的数据输出到Doris");
        // 指定hive仓库中的默认位置
        conf.set("spark.sql.warehouse.dir", "hdfs://server01:8020/hive/warehouse");
        // 设置 Doris Catalog
        conf.set("spark.sql.catalog.doris_catalog", "org.apache.doris.spark.catalog.DorisTableCatalog");
        conf.set("spark.sql.catalog.doris_catalog.doris.fenodes", "192.168.1.12:9040");
        conf.set("spark.sql.catalog.doris_catalog.doris.query.port", "9030");
        conf.set("spark.sql.catalog.doris_catalog.doris.user", "admin");
        conf.set("spark.sql.catalog.doris_catalog.doris.password", "Admin@123");
        // 设置运行环境
        String masterValue = conf.get("spark.master", "local[*]");
        conf.setMaster(masterValue);
        // 创建一个SparkSession对象，同时配置SparkConf，并启用Hive支持
        SparkSession spark = SparkSession
                .builder()
                .config(conf)
                .enableHiveSupport()
                .getOrCreate();
        // 设置当前Catalog
        spark.catalog().setCurrentCatalog("doris_catalog");

        // 查询 databases
        spark.sql("show databases").show();

        // 查询数据
        spark.sql("select * from kongyu.my_user_doris limit 10").show();

        // 停止SparkSession，释放资源
        spark.stop();
    }
}
