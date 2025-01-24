package local.ateng.java.spark.rdd.operator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

/**
 * 读取HDFS文件，复杂聚合
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class OperatorCombineByKey {
    public static void main(String[] args) {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        conf.setAppName("读取HDFS文件，复杂聚合");
        // 设置运行环境
        String masterValue = conf.get("spark.master", "local[*]");
        conf.setMaster(masterValue);
        // 创建Spark上下文
        JavaSparkContext sc = new JavaSparkContext(conf);

        // 将数据并行化为RDD
        JavaRDD<String> textFileRDD = sc.textFile("hdfs://server01:8020/data/my_user.csv");

        // 复杂聚合
        JavaPairRDD<String, Long> rdd = textFileRDD
                .mapToPair(line -> {
                    String[] fields = line.split(",");
                    return new Tuple2<>(fields[5], 1L);
                })
                .combineByKey(
                        // createCombiner: 初始化累加器 (score, 1)
                        count -> new Tuple2<>(count, 1),
                        // mergeValue: 合并新值到当前累加器 (累加分数, 累加数量)
                        (acc, count) -> new Tuple2<>(acc._1 + count, acc._2 + 1),
                        // mergeCombiners: 合并分区的累加器
                        (acc1, acc2) -> new Tuple2<>(acc1._1 + acc2._1, acc1._2 + acc2._2)
                ).mapValues(value -> value._1 + value._2);

        // 输出文件内容
        rdd.foreach(line -> System.out.println(line));

        // 关闭Spark上下文
        sc.close();

    }
}