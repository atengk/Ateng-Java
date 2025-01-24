package local.ateng.java.spark.rdd.operator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

/**
 * 读取HDFS文件，合并数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class OperatorUnion {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        conf.setAppName("读取HDFS文件，合并数据");
        // 设置运行环境
        String masterValue = conf.get("spark.master", "local[*]");
        conf.setMaster(masterValue);
        // 创建Spark上下文
        JavaSparkContext sc = new JavaSparkContext(conf);

        // 将数据并行化为RDD
        JavaRDD<String> textFileRDD = sc.textFile("hdfs://server01:8020/data/my_user.csv");
        textFileRDD.cache();

        // 使用filter操作，筛选出满足特定条件的数据。例如，只保留包含特定关键词的行。
        JavaRDD<String> filtered1RDD = textFileRDD.filter(line -> line.contains("重庆"));
        JavaRDD<String> filtered2RDD = textFileRDD.filter(line -> line.contains("成都"));

        // 使用union操作，将两个RDD合并为一个。
        JavaRDD<String> combinedRDD = filtered1RDD.union(filtered2RDD);

        // 打印RDD中的内容
        combinedRDD.foreach(line -> System.out.println(line));

        // 关闭Spark上下文
        sc.close();

    }
}