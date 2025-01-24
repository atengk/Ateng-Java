package local.ateng.java.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        conf.setAppName("RDD Example");
        // 设置运行环境
        String masterValue = conf.get("spark.master", "local[*]");
        conf.setMaster(masterValue);
        // 创建Spark上下文
        JavaSparkContext sc = new JavaSparkContext(conf);

        // 自定义一组数据
        String[] data = {
                "Hello Spark World",
                "Spark is awesome",
                "Hello again Spark"
        };

        // 将数据并行化为RDD
        JavaRDD<String> inputData = sc.parallelize(Arrays.asList(data));

        // 1. 将每行数据拆分为单词
        JavaRDD<String> words = inputData.flatMap(line -> Arrays.asList(line.split(" ")).iterator());

        // 2. 转换为键值对 (word, 1)
        JavaPairRDD<String, Integer> wordPairs = words.mapToPair(word -> new Tuple2<>(word, 1));

        // 3. 按键聚合，计算每个单词的出现次数
        JavaPairRDD<String, Integer> wordCounts = wordPairs.reduceByKey(Integer::sum);

        // 4. 打印结果
        wordCounts.collect().forEach(result -> {
            System.out.println(result._1 + " : " + result._2);
        });

        // 停止Spark上下文
        sc.stop();
    }
}