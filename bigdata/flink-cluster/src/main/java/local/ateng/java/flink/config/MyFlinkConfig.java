package local.ateng.java.flink.config;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flink Env
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-22
 */
@Configuration
public class MyFlinkConfig {
    /**
     * 执行环境
     *
     * @return flinkEnv
     */
    @Bean
    public StreamExecutionEnvironment flinkEnv() {
        return StreamExecutionEnvironment.getExecutionEnvironment();
    }

    /**
     * 流式表环境
     *
     * @param env
     * @return flinkTableEnv
     */
    @Bean
    public StreamTableEnvironment flinkTableEnv(StreamExecutionEnvironment env) {
        return StreamTableEnvironment.create(env);
    }

}
