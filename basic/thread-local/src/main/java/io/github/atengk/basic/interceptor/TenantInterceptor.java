package io.github.atengk.basic.interceptor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.basic.holder.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * 租户拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String HEADER_TENANT = "X-Tenant-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 1. 从请求头获取
        String tenantId = request.getHeader(HEADER_TENANT);

        // 2. 兜底默认租户
        if (StrUtil.isBlank(tenantId)) {
            tenantId = "default";
        }

        // 3. 放入 ThreadLocal
        TenantContextHolder.set(tenantId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContextHolder.clear();
    }
}