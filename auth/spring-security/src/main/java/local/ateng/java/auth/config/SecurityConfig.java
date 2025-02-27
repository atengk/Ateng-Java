package local.ateng.java.auth.config;

import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.auth.constant.AppCodeEnum;
import local.ateng.java.auth.filter.JwtAuthenticationFilter;
import local.ateng.java.auth.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.PrintWriter;
import java.util.Set;

/**
 * SpringSecurity 配置
 *
 * @author 孔余
 * @EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
 * @EnableMethodSecurity：确保在配置类中启用方法级安全，其中 prePostEnabled = true 用于启用 @PreAuthorize 注解的支持。
 * securedEnabled = true：如果需要支持 @Secured 注解，确保此选项启用。
 * @email 2385569970@qq.com
 * @since 2025-02-26
 */
@Configuration
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SecurityConfig {
    // 配置放行的接口列表
    public static final Set<String> ALLOWED_URLS = Set.of("/user/login", "/actuator/**", "/public/**", "/user/refresh-token");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置 Spring Security 的过滤链，定义各种安全策略和规则。
     *
     * @param httpSecurity HttpSecurity 对象，用于配置安全相关的选项
     * @return 配置好的 SecurityFilterChain
     * @throws Exception 可能抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // 配置 URL 路径的权限控制
                .authorizeHttpRequests(router -> {
                    // 开放登录和 actuator 端点
                    router
                            .requestMatchers(ALLOWED_URLS.toArray(new String[0])).permitAll()  // 放行的接口
                            // 限制 /system/** 只能被拥有 "admin" 角色的用户访问
                            .requestMatchers("/system/**").hasRole("admin")
                            // 限制 /user/add 只能被拥有 "user:add" 权限的用户访问
                            .requestMatchers("/user/add").hasAuthority("user.add")
                            // 其他请求需要认证
                            .anyRequest().authenticated();
                })
                // 禁用表单登录
                .formLogin(form -> form.disable())
                // 禁用默认登出功能
                .logout(config -> config.disable())
                // 禁用默认的 HTTP Basic 认证
                .httpBasic(httpBasic -> httpBasic.disable())
                // 设置 session 管理为无状态（适用于 JWT）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 添加自定义 JWT 认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 禁用 CORS 和 CSRF（通常用于无状态认证）
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                // 配置异常处理，未授权和权限不足时的处理
                .exceptionHandling(exceptionHandling -> {
                    exceptionHandling.accessDeniedHandler(accessDeniedHandler());  // 权限不足时调用
                    exceptionHandling.authenticationEntryPoint(authenticationEntryPoint());  // 未认证时调用
                });

        return httpSecurity.build();  // 返回配置好的过滤链
    }


    /**
     * 不使用SpringSecurity的账号密码验证，而是自定义验证账号密码信息
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> null;
    }

    /**
     * 创建自定义的 AuthenticationEntryPoint，用于处理未登录（401）状态下的请求。
     * 当用户未登录时，返回一个 JSON 格式的错误信息，提示未登录。
     *
     * @return 返回一个实现了 AuthenticationEntryPoint 接口的 Bean，处理未登录的异常。
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=utf-8");
            PrintWriter printWriter = response.getWriter();
            printWriter.write(Result.error(AppCodeEnum.AUTH_USER_NOT_LOGIN.getCode(), AppCodeEnum.AUTH_USER_NOT_LOGIN.getDescription()).toString());
            printWriter.flush();
        };
    }

    /**
     * 创建自定义的 AccessDeniedHandler，用于处理权限不足（403）状态下的请求。
     * 当用户在登录后，但没有足够权限时，返回一个 JSON 格式的错误信息，提示权限不足。
     *
     * @return 返回一个实现了 AccessDeniedHandler 接口的 Bean，处理权限不足的异常。
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=utf-8");
            PrintWriter printWriter = response.getWriter();
            printWriter.write(Result.error(AppCodeEnum.AUTH_INVALID_AUTHENTICATION.getCode(), AppCodeEnum.AUTH_INVALID_AUTHENTICATION.getDescription()).toString());
            printWriter.flush();
        };
    }

}

