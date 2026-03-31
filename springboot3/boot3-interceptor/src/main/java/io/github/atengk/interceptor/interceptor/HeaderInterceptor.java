package io.github.atengk.interceptor.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Header统一处理拦截器
 */
public class HeaderInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // ========================
        // 1. 设置跨域响应头（基础版）
        // ========================
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type,Authorization,Trace-Id");
        response.setHeader("Access-Control-Max-Age", "3600");

        // ========================
        // 2. 处理预检请求（OPTIONS）
        // ========================
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(200);
            return false;
        }

        // ========================
        // 3. TraceId处理（链路追踪）
        // ========================
        String traceId = request.getHeader("Trace-Id");

        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }

        // 回写到响应头
        response.setHeader("Trace-Id", traceId);

        return true;
    }
}
