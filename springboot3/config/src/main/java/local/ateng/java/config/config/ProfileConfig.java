package local.ateng.java.config.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ProfileConfig {

    @Bean
    @Profile("dev")
    public String devBean() {
        return "开发环境Bean";
    }

    @Bean
    @Profile("prod")
    public String prodBean() {
        return "生产环境Bean";
    }
}
