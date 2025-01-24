package local.ateng.java.spark.sql.udf;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;

/**
 * 用户自定义函数：计算某一列的数据长度
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class StringLengthSQL {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        // 设置应用程序的名称
        conf.setAppName("查询数据数量");
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

        // 注册 UDF
        spark.udf().register("stringLengthUDF", new StringLengthUDF(), DataTypes.IntegerType);

        // 执行SQL查询
        Dataset<Row> ds = spark.sql("SELECT name,province,stringLengthUDF(province) FROM my_user LIMIT 10");

        // 显示 DataFrame 的结构
        ds.printSchema();

        // 显示查询结果
        ds.show();

        // 停止SparkSession，释放资源
        spark.stop();
    }
}
