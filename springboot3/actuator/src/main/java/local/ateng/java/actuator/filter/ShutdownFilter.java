package local.ateng.java.actuator.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;

@Component
public class ShutdownFilter implements Filter {

    private static final String USERNAME = "admin";  // 设置账号
    private static final String PASSWORD = "Admin@123";  // 设置密码

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 过滤器初始化方法，通常不需要做任何事
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 检查请求是否是对 /actuator/shutdown 的请求
        if (httpRequest.getRequestURI().startsWith("/actuator/shutdown")) {
            // 获取请求头中的 Authorization 信息
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                // 如果没有 Authorization 头或者不是 Basic 认证
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.getWriter().write("Unauthorized");
                return;
            }

            // 解码 Basic 认证信息
            String base64Credentials = authHeader.substring(6);
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));
            String[] values = credentials.split(":", 2);

            // 检查用户名和密码
            if (!values[0].equals(USERNAME) || !values[1].equals(PASSWORD)) {
                // 如果用户名或密码不匹配
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.getWriter().write("Invalid username or password");
                return;
            }
        }

        // 认证成功后，继续请求处理
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 过滤器销毁方法，通常不需要做任何事
    }
}

