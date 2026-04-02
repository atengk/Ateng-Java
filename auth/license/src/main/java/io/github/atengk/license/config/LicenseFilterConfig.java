package io.github.atengk.license.config;

import io.github.atengk.license.filter.LicenseFilter;
import io.github.atengk.license.service.LicenseService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * License 过滤器配置
 */
@Configuration
public class LicenseFilterConfig {

    @Bean
    public FilterRegistrationBean<LicenseFilter> licenseFilter(LicenseService licenseService) {

        FilterRegistrationBean<LicenseFilter> bean = new FilterRegistrationBean<>();

        bean.setFilter(new LicenseFilter(licenseService));
        bean.addUrlPatterns("/*");
        bean.setOrder(1);

        return bean;
    }
}
