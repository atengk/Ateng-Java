package io.github.atengk.basic.config;

import io.github.atengk.basic.interceptor.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UserContextInterceptor userContextInterceptor;
    @Autowired
    private TraceIdInterceptor traceIdInterceptor;
    @Autowired
    private IdempotentInterceptor idempotentInterceptor;
    @Autowired
    private TenantInterceptor tenantInterceptor;
    @Autowired
    private PermissionInterceptor permissionInterceptor;
    @Autowired
    private LogContextInterceptor logContextInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor).addPathPatterns("/**");
        registry.addInterceptor(traceIdInterceptor).addPathPatterns("/**");
//        registry.addInterceptor(idempotentInterceptor).addPathPatterns("/**");
//        registry.addInterceptor(tenantInterceptor).addPathPatterns("/**");
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/**");
        registry.addInterceptor(logContextInterceptor).addPathPatterns("/**");
    }
}
