package local.ateng.java.spark.sql;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
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

    public void run() {
        // 获取环境
        SparkSession spark = SpringUtil.getBean("sparkSession", SparkSession.class);

        // 执行SQL查询
        Dataset<Row> ds = spark.sql("SELECT COUNT(*) FROM my_user");

        // 显示查询结果
        System.out.println("SQL计算结果：");
        ds.show();

        // 将结果写入到 HDFS 的 CSV 文件
        ds.write()
                .mode(SaveMode.Overwrite)
                .option("header", "true") // 启用表头
                .option("delimiter", ",") // 设置分隔符
                .option("quote", "\"") // 指定引用符
                .option("escape", "\\") // 指定转义符
                .csv("hdfs://server01:8020/data/spark/output");

        // 停止SparkSession，释放资源
        spark.stop();
    }
}
