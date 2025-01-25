package local.ateng.java.spark.task;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 查询数据数量
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
@Component
@Slf4j
public class SQLCount {

    @Scheduled(cron = "10 * * * * ?")
    @Async
    public void run() {
        // 获取环境
        SparkSession spark = SpringUtil.getBean("sparkSession", SparkSession.class);

        // 执行SQL查询
        Dataset<Row> ds = spark.sql("SELECT COUNT(*) FROM my_user");

        // 显示查询结果
        System.out.println("SQL计算结果：");
        ds.show();

    }
}
