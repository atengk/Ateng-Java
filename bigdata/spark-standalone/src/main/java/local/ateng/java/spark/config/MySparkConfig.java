package local.ateng.java.spark.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spark Config
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-25
 */
@Configuration
@Slf4j
public class MySparkConfig {

    /**
     * 创建Spark配置
     *
     * @return Spark上下文
     */
    @Bean
    public SparkConf sparkConf() {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        // 设置应用名称
        conf.setAppName("SparkOnSpring");
        // 关闭 Web UI
        conf.set("spark.ui.enabled", "false");
        // 指定hive仓库中的默认位置
        //conf.set("spark.sql.warehouse.dir", "hdfs://server01:8020/hive/warehouse");
        // 设置运行环境
        String masterValue = conf.get("spark.master", "local[*]");
        conf.setMaster(masterValue);
        return conf;
    }

    /**
     * 创建Spark上下文
     *
     * @param conf
     * @return JavaSparkContext
     */
    @Bean(destroyMethod = "stop")
    public JavaSparkContext sparkContext(SparkConf conf) {
        return new JavaSparkContext(conf);
    }

    /**
     * 创建Spark Session
     *
     * @param conf
     * @return SparkSession
     */
    @Bean(destroyMethod = "stop")
    public SparkSession sparkSession(SparkConf conf) {
        return SparkSession
                .builder()
                .config(conf)
                .enableHiveSupport()
                .getOrCreate();
    }

}
