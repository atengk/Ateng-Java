package io.github.atengk.config;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QLExpress 配置类
 *
 * @author Ateng
 * @date 2026/04/07
 */
@Configuration
public class QLExpressConfig {

    /**
     * 全局唯一执行器
     */
    @Bean
    public Express4Runner express4Runner() {
        return new Express4Runner(InitOptions.DEFAULT_OPTIONS);
    }
}
