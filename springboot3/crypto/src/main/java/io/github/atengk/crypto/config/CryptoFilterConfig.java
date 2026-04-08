package io.github.atengk.crypto.config;

import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;

/**
 * 加密过滤器配置
 *
 * @author 孔余
 * @since 2026-01-29
 */
@Configuration
public class CryptoFilterConfig {

    /**
     * 注册解密过滤器
     */
    @Bean
    public FilterRegistrationBean<Filter> decryptFilter(
            StringRedisTemplate redisTemplate,
            List<HandlerMapping> handlerMappings) {

        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();

        registration.setFilter(new DecryptFilter(redisTemplate, handlerMappings));

        /*
         * 拦截路径（按需调整）
         */
        registration.addUrlPatterns("/*");

        /*
         * 执行顺序（建议靠前）
         */
        registration.setOrder(1);

        registration.setName("decryptFilter");

        return registration;
    }
}