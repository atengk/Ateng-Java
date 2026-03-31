package io.github.atengk.interceptor.interceptor;

import io.github.atengk.interceptor.context.UserContext;
import io.github.atengk.interceptor.util.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文注入拦截器
 */
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 从请求头获取Token
        String token = request.getHeader("Authorization");

        if (token != null && !token.isEmpty()) {

            Long userId = TokenUtil.getUserId(token);
            String username = TokenUtil.getUsername(token);

            if (userId != null && username != null) {
                // 注入上下文
                UserContext.set(userId, username);
            }
        }

        return true;
    }

    /**
     * 请求完成后清理 ThreadLocal
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
