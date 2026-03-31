package io.github.atengk.interceptor.config;

import io.github.atengk.interceptor.filter.TraceIdFilter;
import io.github.atengk.interceptor.filter.CorsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filter配置
 */
@Configuration
public class FilterConfig {

    /**
     * CORS过滤器
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new CorsFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(1); // 优先级最高

        return bean;
    }

    /**
     * TraceId过滤器
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {

        FilterRegistrationBean<TraceIdFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new TraceIdFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(2);

        return bean;
    }
}
