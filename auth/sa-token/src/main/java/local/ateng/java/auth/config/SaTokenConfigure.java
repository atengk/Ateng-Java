package local.ateng.java.auth.config;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import local.ateng.java.auth.constant.AppCodeEnum;
import local.ateng.java.auth.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 权限认证 配置类
 * https://sa-token.cc/doc.html#/use/at-check
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-24
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(SaTokenConfigure.class);

    /**
     * 注册 Sa-Token 拦截器，打开注解式鉴权功能
     * https://sa-token.cc/doc.html#/use/route-check
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry
                .addInterceptor(
                        new SaInterceptor(handle -> {
                            // 登录校验
                            SaRouter
                                    .match("/**")
                                    .check(r -> StpUtil.checkLogin());
                            // 角色验证
                            SaRouter.match("/user/**", r -> StpUtil.checkRole("user"));
                            SaRouter.match("/admin/**", r -> StpUtil.checkRole("admin"));
                            SaRouter.match("/goods/**", r -> StpUtil.checkRole("goods"));
                            // 权限验证（更细粒度的权限需要使用注解实现）
                            SaRouter.match("/user/**", r -> StpUtil.checkPermission("user.get"));
                            SaRouter.match("/admin/**", r -> StpUtil.checkPermission("admin.*"));
                            SaRouter.match("/goods/**", r -> StpUtil.checkPermission("goods.*"));
                        }).isAnnotation(true) // 注解鉴权（在Token正常的情况下，注解鉴权优先级高于路由拦截鉴权）
                )
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/demo/**");
    }

    /**
     * 注册 Sa-Token 全局过滤器
     * https://sa-token.cc/doc.html#/up/global-filter
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .setAuth(obj ->
                        SaRouter
                                .match("/actuator/shutdown")
                                .check(() -> SaHttpBasicUtil.check("admin:Admin@123"))
                )
                .setError(e -> {
                    log.error(e.getMessage());
                    return Result.error(AppCodeEnum.OPERATION_CANCELED.getCode(), AppCodeEnum.OPERATION_CANCELED.getDescription());
                });
    }
}
