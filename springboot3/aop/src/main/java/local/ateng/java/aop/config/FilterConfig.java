package local.ateng.java.aop.config;

import local.ateng.java.aop.filter.RepeatableFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 过滤器配置类
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-20
 */
@Configuration
public class FilterConfig {

    /**
     * 注册一个自定义的 RepeatableFilter，它可以使请求体可重复读取
     * @return
     */
    @Bean
    public FilterRegistrationBean<RepeatableFilter> repeatableFilter() {
        FilterRegistrationBean<RepeatableFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RepeatableFilter());
        registrationBean.addUrlPatterns("/*");  // 设置需要拦截的URL模式
        registrationBean.setName("repeatableFilter");
        registrationBean.setOrder(FilterRegistrationBean.LOWEST_PRECEDENCE);
        return registrationBean;
    }

}
