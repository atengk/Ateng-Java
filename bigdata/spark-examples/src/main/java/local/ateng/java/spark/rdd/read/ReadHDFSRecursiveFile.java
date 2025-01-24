package local.ateng.java.spark.rdd.read;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

/**
 * 递归读取HDFS文件
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class ReadHDFSRecursiveFile {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        conf.setAppName("读取HDFS文件");
        // 启用递归读取
        conf.set("spark.hadoop.mapreduce.input.fileinputformat.input.dir.recursive", "true");
        // 设置运行环境
        String masterValue = conf.get("spark.master", "local[*]");
        conf.setMaster(masterValue);
        // 创建Spark上下文
        JavaSparkContext sc = new JavaSparkContext(conf);

        // 将数据并行化为RDD
        JavaRDD<String> textFileRDD = sc.textFile("hdfs://server01:8020/data/flink/sink");

        // 输出文件内容
        long count = textFileRDD.count();
        System.out.println(count);
        //textFileRDD.foreach(line -> System.out.println(line));

        // 关闭Spark上下文
        sc.close();

    }
}