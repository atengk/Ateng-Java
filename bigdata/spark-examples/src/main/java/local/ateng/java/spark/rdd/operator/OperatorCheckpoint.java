package local.ateng.java.spark.rdd.operator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

/**
 * 读取HDFS文件，缓存数据和数据检查点
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class OperatorCheckpoint {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        conf.setAppName("读取HDFS文件，缓存数据和数据检查点");
        // 设置运行环境
        String masterValue = conf.get("spark.master", "local[*]");
        conf.setMaster(masterValue);
        // 创建Spark上下文
        JavaSparkContext sc = new JavaSparkContext(conf);

        // 将数据并行化为RDD
        JavaRDD<String> textFileRDD = sc.textFile("hdfs://server01:8020/data/my_user.csv");

        // 使用cache或persist操作，将 textFileRDD 缓存到内存中，确保 Spark 在后续操作中不会重新计算该 RDD，而是直接从缓存中读取。
        textFileRDD.cache(); // 或 textFileRDD.persist(StorageLevel.MEMORY_ONLY());
        // 数据检查点：针对 textFileRDD 做检查点计算
        sc.setCheckpointDir("hdfs://server01:8020/spark/checkpoint");
        textFileRDD.checkpoint();

        // 输出文件内容
        textFileRDD.foreach(line -> System.out.println(line));

        // 关闭Spark上下文
        sc.close();

    }
}