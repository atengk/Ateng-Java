package local.ateng.java.spark.sql;

import cn.hutool.core.util.StrUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

/**
 * SQL填充
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class SqlFill {
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

        // 执行SQL查询
        String sql = "SELECT COUNT(*) FROM my_user WHERE province = '{}'";
        Dataset<Row> ds = spark.sql(StrUtil.format(sql,"重庆市"));

        // 显示 DataFrame 的结构
        ds.printSchema();

        // 显示查询结果
        ds.show();

        // 停止SparkSession，释放资源
        spark.stop();
    }
}
