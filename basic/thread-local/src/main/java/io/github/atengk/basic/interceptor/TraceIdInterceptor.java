package io.github.atengk.basic.interceptor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.basic.holder.TraceIdContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * TraceId 拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 1. 优先从请求头获取（网关透传）
        String traceId = request.getHeader(HEADER_TRACE_ID);

        // 2. 如果没有则生成
        if (StrUtil.isBlank(traceId)) {
            traceId = TraceIdContextHolder.getOrCreate();
        } else {
            TraceIdContextHolder.set(traceId);
        }

        // 3. 回写响应头（方便前端/调用方获取）
        response.setHeader(HEADER_TRACE_ID, traceId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TraceIdContextHolder.clear();
    }
}