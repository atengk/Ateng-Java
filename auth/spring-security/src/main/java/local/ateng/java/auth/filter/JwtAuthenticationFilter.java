package local.ateng.java.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义的 JwtAuthenticationFilter 过滤器，用于从请求中提取 JWT Token 并进行认证。
 * 该过滤器会在每次请求时执行一次，验证请求中的 Token 是否有效，并将认证信息存储到 SecurityContext 中。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-26
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 过滤器的核心逻辑，处理每个请求并进行身份验证。
     *
     * @param request     请求对象
     * @param response    响应对象
     * @param filterChain 过滤器链，用于调用下一个过滤器
     * @throws ServletException 如果发生 Servlet 异常
     * @throws IOException      如果发生 I/O 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 从请求中提取 Token
        String token = getTokenFromRequest(request);

        // 用于演示，实际情况需要验证 Jwt Token 的真实性
        if ("1234567890".equals(token)) {
            // 角色和权限对象集合
            List<GrantedAuthority> authorities = new ArrayList<>();
            // 角色，注意：Spring Security默认角色需加ROLE_前缀
            //authorities.add(new SimpleGrantedAuthority("ROLE_" + "admin"));
            // 权限
            //authorities.add(new SimpleGrantedAuthority("user.add"));
            // 构造一个 UsernamePasswordAuthenticationToken 对象，并将其设置到 SecurityContext 中
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "admin", "******", authorities);
            authentication.setDetails("我是阿腾");
            // 将认证信息存入 Spring Security 的上下文中
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 继续执行过滤器链，传递请求和响应
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Token，通常是 Bearer Token。
     *
     * @param request 请求对象
     * @return 提取到的 Token，如果没有则返回 null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 从请求头中获取 "Authorization" 字段
        String header = request.getHeader("Authorization");

        // 检查 Authorization 头部是否包含 Bearer Token
        if (header != null && header.startsWith("Bearer ")) {
            // 提取 Token 部分
            return header.substring(7);  // 去掉 "Bearer " 前缀
        }

        // 如果没有找到 Token，则返回 null
        return null;
    }
}

