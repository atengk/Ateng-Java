package local.ateng.java.spark.catalog;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;

/**
 * Spark 集成 Iceberg
 * https://iceberg.apache.org/docs/nightly/spark-getting-started/
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class Iceberg {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        // 设置应用程序的名称
        conf.setAppName("Spark 集成 Iceberg");
        // 指定hive仓库中的默认位置
        conf.set("spark.sql.warehouse.dir", "hdfs://server01:8020/hive/warehouse");
        // 设置 Iceberg Catalog
        conf.set("spark.sql.catalog.iceberg_catalog", "org.apache.iceberg.spark.SparkCatalog");
        conf.set("spark.sql.catalog.iceberg_catalog.warehouse", "s3://iceberg-bucket/warehouse");
        conf.set("spark.sql.catalog.iceberg_catalog.s3.endpoint", "http://192.168.1.13:9000");
        conf.set("spark.sql.catalog.iceberg_catalog.io-impl", "org.apache.iceberg.aws.s3.S3FileIO");
        conf.set("spark.sql.catalog.iceberg_catalog.catalog-impl", "org.apache.iceberg.jdbc.JdbcCatalog");
        conf.set("spark.sql.catalog.iceberg_catalog.uri", "jdbc:postgresql://192.168.1.10:32297/iceberg");
        conf.set("spark.sql.catalog.iceberg_catalog.jdbc.user", "postgres");
        conf.set("spark.sql.catalog.iceberg_catalog.jdbc.password", "Lingo@local_postgresql_5432");
        conf.set("spark.sql.catalog.iceberg_catalog.jdbc.schema-version", "V1");
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
        spark.catalog().setCurrentCatalog("iceberg_catalog");

        // 创建一个名为 "spark" 的数据库，如果不存在则创建
        spark.sql("CREATE DATABASE IF NOT EXISTS spark;");

        // 显示当前 SparkSession 中的所有数据库
        spark.sql("show databases").show();

        // 切换到 "spark" 数据库
        spark.sql("use spark");

        // 在 "spark" 数据库中创建一张名为 "my_user" 的表，如果表不存在
        spark.sql("CREATE TABLE IF NOT EXISTS my_user (\n" +
                "  id BIGINT NOT NULL,\n" +
                "  name STRING,\n" +
                "  age INT,\n" +
                "  score DOUBLE,\n" +
                "  birthday TIMESTAMP,\n" +
                "  province STRING,\n" +
                "  city STRING,\n" +
                "  create_time TIMESTAMP\n" +
                ") USING iceberg;");

        // 显示当前数据库中的所有表
        spark.sql("show tables").show();

        // 向 "my_user" 表中插入三条记录，分别表示三位用户的信息
        spark.sql("INSERT INTO my_user VALUES \n" +
                "(1, 'Alice', 30, 85.5, TIMESTAMP '1993-01-15 10:00:00', 'Beijing', 'Beijing', TIMESTAMP '2023-07-01 10:00:00'),\n" +
                "(2, 'Bob', 25, 90.0, TIMESTAMP '1998-06-20 14:30:00', 'Shanghai', 'Shanghai', TIMESTAMP '2023-07-01 11:00:00'),\n" +
                "(3, 'Carol', 28, 95.0, TIMESTAMP '1995-12-05 09:45:00', 'Guangdong', 'Guangzhou', TIMESTAMP '2023-07-02 09:00:00');\n");

        // 查询 "my_user" 表中的所有数据并打印
        spark.sql("SELECT * FROM my_user;").show();

        // 查询 "my_user" 表中记录的总数并打印
        spark.sql("SELECT count(*) FROM my_user;").show();

        // 停止SparkSession，释放资源
        spark.stop();
    }
}
