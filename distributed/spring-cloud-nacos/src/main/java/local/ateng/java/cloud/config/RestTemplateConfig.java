package local.ateng.java.cloud.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    @LoadBalanced // 让 RestTemplate 具有负载均衡能力
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

