package local.ateng.java.actuator.config;

import local.ateng.java.actuator.filter.ShutdownFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<ShutdownFilter> actuatorShutdownFilter() {
        FilterRegistrationBean<ShutdownFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ShutdownFilter());
        registrationBean.addUrlPatterns("/actuator/shutdown");  // 只拦截 /actuator/shutdown 端点
        registrationBean.setOrder(1);  // 设置过滤器顺序
        return registrationBean;
    }
}

