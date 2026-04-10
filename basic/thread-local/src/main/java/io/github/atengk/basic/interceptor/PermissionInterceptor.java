package io.github.atengk.basic.interceptor;

import io.github.atengk.basic.holder.PermissionContextHolder;
import io.github.atengk.basic.holder.UserContextHolder;
import io.github.atengk.basic.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 权限拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    public PermissionInterceptor(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 从用户上下文获取用户ID（依赖前面 UserContextHolder）
        Long userId = UserContextHolder.getUserId();

        // 查询权限
        Set<String> permissions = permissionService.getPermissions(userId);

        // 放入 ThreadLocal
        PermissionContextHolder.set(permissions);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        PermissionContextHolder.clear();
    }
}
