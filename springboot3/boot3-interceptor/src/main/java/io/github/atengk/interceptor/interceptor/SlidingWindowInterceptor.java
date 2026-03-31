package io.github.atengk.interceptor.interceptor;

import io.github.atengk.interceptor.annotation.SlidingWindowLimit;
import io.github.atengk.interceptor.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 滑动窗口限流拦截器
 */
public class SlidingWindowInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;

    public SlidingWindowInterceptor(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;

        SlidingWindowLimit limit = method.getMethodAnnotation(SlidingWindowLimit.class);
        if (limit == null) {
            return true;
        }

        // 构造KEY
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        String key = "sliding_limit:" + ip + ":" + uri;

        // 当前窗口请求数
        Long count = redisUtil.slidingWindowCount(key, limit.time());

        if (count != null && count > limit.count()) {
            response.setStatus(429);
            response.getWriter().write("请求过于频繁（滑动窗口）");
            return false;
        }

        return true;
    }
}
