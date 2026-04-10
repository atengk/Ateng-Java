package io.github.atengk.basic.interceptor;

import io.github.atengk.basic.holder.LogContextHolder;
import io.github.atengk.basic.holder.TraceIdContextHolder;
import io.github.atengk.basic.holder.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * 日志上下文拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class LogContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 可结合前面示例（TraceId + UserContext）
        String traceId = TraceIdContextHolder.getOrCreate();
        Long userId = UserContextHolder.getUserId();

        LogContextHolder.put("traceId", traceId);
        LogContextHolder.put("userId", String.valueOf(userId));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LogContextHolder.clear();
    }
}
