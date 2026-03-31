package io.github.atengk.interceptor.interceptor;

import io.github.atengk.interceptor.annotation.RateLimit;
import io.github.atengk.interceptor.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 固定窗口限流拦截器
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;

    public RateLimitInterceptor(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;

        // 获取注解
        RateLimit rateLimit = method.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        // 构造限流KEY（IP + URI）
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        String key = "rate_limit:" + ip + ":" + uri;

        // 计数
        Long count = redisUtil.increment(key, rateLimit.time());

        // 超过限制
        if (count != null && count > rateLimit.count()) {
            response.setStatus(429);
            response.getWriter().write("请求过于频繁，请稍后再试");
            return false;
        }

        return true;
    }
}
