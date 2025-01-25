package local.ateng.java.spark.task;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 读取HDFS文件并计算行数
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
@Component
@Slf4j
public class RDDCount {

    @Scheduled(cron = "0 * * * * ?")
    @Async
    public void run() {
        // 获取环境
        JavaSparkContext sc = SpringUtil.getBean("sparkContext", JavaSparkContext.class);

        // 将数据并行化为RDD
        JavaRDD<String> textFileRDD = sc.textFile("hdfs://server01:8020/data/my_user.csv");

        // 使用count操作，获取RDD中行数。
        long count = textFileRDD.count();

        // 打印结果
        System.out.println("RDD计算结果: " + count);

    }
}