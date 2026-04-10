package io.github.atengk.basic.filter;

import io.github.atengk.basic.context.RequestContext;
import io.github.atengk.basic.context.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 多租户上下文过滤器
 *
 * 在请求入口绑定 tenantId / userId / traceId
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class TenantContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String tenantId = httpRequest.getHeader("X-TENANT-ID");
        String userId = httpRequest.getHeader("X-USER-ID");
        String traceId = httpRequest.getHeader("X-TRACE-ID");

        if (traceId == null || traceId.isEmpty()) {
            traceId = RequestContext.generateTraceId();
        }

        String finalTenantId = tenantId;
        String finalUserId = userId;
        String finalTraceId = traceId;

        ScopedValue.where(TenantContext.TENANT_ID, finalTenantId)
                .where(RequestContext.USER_ID, finalUserId)
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