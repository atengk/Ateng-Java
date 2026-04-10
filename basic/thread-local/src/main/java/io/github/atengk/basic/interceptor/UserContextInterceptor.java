package io.github.atengk.basic.interceptor;

import io.github.atengk.basic.model.User;
import io.github.atengk.basic.holder.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * 用户上下文拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 模拟从请求中解析用户信息（如 JWT / Token）
        Long userId = 1001L;
        String username = "testUser";

        User user = new User(userId, username);

        // 放入 ThreadLocal
        UserContextHolder.set(user);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        // 请求结束必须清理
        UserContextHolder.clear();
    }
}
