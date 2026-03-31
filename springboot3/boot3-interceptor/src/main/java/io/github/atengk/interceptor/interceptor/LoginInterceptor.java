package io.github.atengk.interceptor.interceptor;


import io.github.atengk.interceptor.context.UserContext;
import io.github.atengk.interceptor.util.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器（为权限校验提供用户信息）
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("未登录");
            return false;
        }

        Long userId = TokenUtil.getUserId(token);
        String role = TokenUtil.getRole(token);

        if (userId == null || role == null) {
            response.setStatus(401);
            response.getWriter().write("Token无效");
            return false;
        }

        // 写入上下文
        UserContext.set(userId, role);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
