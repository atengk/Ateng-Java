package local.ateng.java.spark.rdd.operator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

/**
 * 读取HDFS文件并数据转换
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class OperatorMap {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        conf.setAppName("读取HDFS文件并数据转换");
        // 设置运行环境
        String masterValue = conf.get("spark.master", "local[*]");
        conf.setMaster(masterValue);
        // 创建Spark上下文
        JavaSparkContext sc = new JavaSparkContext(conf);

        // 将数据并行化为RDD
        JavaRDD<String> textFileRDD = sc.textFile("hdfs://server01:8020/data/my_user.csv");

        // 使用map操作，对每一行数据进行转换，创建新的RDD。例如，将每一行数据按逗号分割并获取特定列的值。
        JavaRDD<String> transformedRDD = textFileRDD.map(line -> line.split(",")[1]);

        // 输出文件内容
        transformedRDD.foreach(line -> System.out.println(line));

        // 关闭Spark上下文
        sc.close();

    }
}