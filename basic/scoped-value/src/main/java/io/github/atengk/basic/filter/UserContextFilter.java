package io.github.atengk.basic.filter;

import io.github.atengk.basic.context.RequestContext;
import io.github.atengk.basic.context.TenantContext;
import io.github.atengk.basic.context.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 用户登录态过滤器
 *
 * 模拟从请求头解析 token 并构建用户上下文
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String userId = httpRequest.getHeader("X-USER-ID");
        String username = httpRequest.getHeader("X-USERNAME");
        String tenantId = httpRequest.getHeader("X-TENANT-ID");
        String traceId = httpRequest.getHeader("X-TRACE-ID");

        if (traceId == null || traceId.isEmpty()) {
            traceId = RequestContext.generateTraceId();
        }

        UserContext.UserInfo userInfo = new UserContext.UserInfo(
                userId,
                username,
                List.of("admin", "user"),
                List.of("user:read", "user:write")
        );

        String finalTraceId = traceId;
        String finalTenantId = tenantId;
        String finalUserId = userId;

        ScopedValue.where(UserContext.USER, userInfo)
                .where(RequestContext.USER_ID, finalUserId)
                .where(TenantContext.TENANT_ID, finalTenantId)
                .where(RequestContext.TRACE_ID, finalTraceId)
                .run(() -> {
                    try {
                        chain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
