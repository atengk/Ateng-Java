package local.ateng.java.spark.rdd.operator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

/**
 * 读取HDFS文件，分组操作
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class OperatorReduceByKey {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        conf.setAppName("读取HDFS文件，分组操作");
        // 设置运行环境
        String masterValue = conf.get("spark.master", "local[*]");
        conf.setMaster(masterValue);
        // 创建Spark上下文
        JavaSparkContext sc = new JavaSparkContext(conf);

        // 将数据并行化为RDD
        JavaRDD<String> textFileRDD = sc.textFile("hdfs://server01:8020/data/my_user.csv");

        // 聚合
        JavaPairRDD<String, Long> rdd = textFileRDD
                .mapToPair(line -> {
                    String[] fields = line.split(",");
                    return new Tuple2<>(fields[5], 1L);
                })
                .reduceByKey((line1, line2) -> line1 + line2);

        // 输出文件内容
        rdd.foreach(line -> System.out.println(line));

        // 关闭Spark上下文
        sc.close();

    }
}