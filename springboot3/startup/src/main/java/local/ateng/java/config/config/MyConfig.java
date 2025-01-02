package local.ateng.java.config.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MyConfig {

    /**
     * @Bean方法：定义并初始化Spring容器中的Bean。
     * @return String
     */
    @Bean
    public String myBean() {
        return "This is a bean";
    }

    /**
     * @PreDestroy：在Bean销毁前执行的方法。
     */
    @PreDestroy
    public void cleanup() {
        log.info("在Bean销毁前执行的方法");
    }
}

