package io.github.atengk.interceptor.interceptor;

import io.github.atengk.interceptor.annotation.RequireRole;
import io.github.atengk.interceptor.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 权限拦截器（RBAC）
 */
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;

        // 获取注解
        RequireRole requireRole = method.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            return true;
        }

        // 当前用户角色
        String userRole = UserContext.getRole();

        if (userRole == null) {
            response.setStatus(401);
            response.getWriter().write("未登录");
            return false;
        }

        // 是否在允许角色内
        boolean allowed = Arrays.asList(requireRole.value()).contains(userRole);

        if (!allowed) {
            response.setStatus(403);
            response.getWriter().write("无权限访问");
            return false;
        }

        return true;
    }
}
