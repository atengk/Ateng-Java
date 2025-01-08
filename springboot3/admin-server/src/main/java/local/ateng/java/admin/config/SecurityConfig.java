package local.ateng.java.admin.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * admin 监控 安全配置
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-08
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig{

    private final String adminContextPath;

    // 使用构造函数注入获取 Admin Server 的上下文路径
    public SecurityConfig(AdminServerProperties adminServerProperties) {
        this.adminContextPath = adminServerProperties.getContextPath();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        // 设置认证成功后的处理器
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminContextPath + "/applications");
        successHandler.setAlwaysUseDefaultTargetUrl(true);

        return httpSecurity
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))  // 禁用 X-Frame-Options 以允许 H2 控制台显示
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(new AntPathRequestMatcher(adminContextPath + "/assets/**")).permitAll()  // 静态资源
                        .requestMatchers(new AntPathRequestMatcher(adminContextPath + "/login")).permitAll()  // 登录页面
                        .requestMatchers(new AntPathRequestMatcher("/actuator")).permitAll()  // Actuator 端点
                        .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()  // Actuator 子端点
                        .anyRequest().authenticated()  // 其他请求需要认证
                )
                .formLogin(formLogin -> formLogin
                        .loginPage(adminContextPath + "/login").successHandler(successHandler)  // 配置自定义登录页面
                )
                .logout(logout -> logout.logoutUrl(adminContextPath + "/logout"))  // 配置注销 URL
                .httpBasic(Customizer.withDefaults())  // 启用 HTTP Basic 认证
                .csrf(AbstractHttpConfigurer::disable)  // 禁用 CSRF 保护
                .build();
    }
}
