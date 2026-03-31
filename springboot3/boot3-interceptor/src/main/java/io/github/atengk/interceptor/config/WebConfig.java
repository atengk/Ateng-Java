package io.github.atengk.interceptor.config;

import io.github.atengk.interceptor.filter.RequestWrapperFilter;
import io.github.atengk.interceptor.interceptor.*;
import io.github.atengk.interceptor.util.RedisUtil;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final RedisUtil redisUtil;

    public WebConfig(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }
    /**
     * 注册日志拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GrayReleaseInterceptor())
                .addPathPatterns("/**");
    }

    /**
     * 注册请求包装过滤器
     */
//    @Bean
    public FilterRegistrationBean<RequestWrapperFilter> requestWrapperFilter() {

        FilterRegistrationBean<RequestWrapperFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestWrapperFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(1); // 优先级要高

        return bean;
    }
}
