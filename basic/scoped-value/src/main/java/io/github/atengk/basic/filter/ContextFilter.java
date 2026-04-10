package io.github.atengk.basic.filter;

import io.github.atengk.basic.context.RequestContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 请求上下文初始化过滤器
 *
 * 在每个请求进入时初始化 ScopedValue 上下文
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class ContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String userId = httpRequest.getHeader("X-USER-ID");
        String traceId = httpRequest.getHeader("X-TRACE-ID");

        if (traceId == null || traceId.isEmpty()) {
            traceId = RequestContext.generateTraceId();
        }

        String finalUserId = userId;
        String finalTraceId = traceId;

        ScopedValue.where(RequestContext.USER_ID, finalUserId)
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
