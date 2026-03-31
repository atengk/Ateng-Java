package io.github.atengk.interceptor.interceptor;

import io.github.atengk.interceptor.annotation.GrayRelease;
import io.github.atengk.interceptor.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 灰度发布拦截器
 */
public class GrayReleaseInterceptor implements HandlerInterceptor {

    /**
     * 灰度标记（供Controller使用）
     */
    public static final String GRAY_FLAG = "GRAY_FLAG";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;

        GrayRelease gray = method.getMethodAnnotation(GrayRelease.class);
        if (gray == null) {
            return true;
        }

        boolean isGray = false;

        // ========================
        // 1. Header优先控制
        // ========================
        String headerValue = request.getHeader(gray.header());
        if (gray.version().equals(headerValue)) {
            isGray = true;
        }

        // ========================
        // 2. 用户ID分流（稳定）
        // ========================
        if (!isGray) {
            Long userId = UserContext.getUserId();

            if (userId != null) {
                // 取模实现稳定灰度
                int mod = (int) (userId % 100);
                if (mod < gray.percent()) {
                    isGray = true;
                }
            }
        }

        // 写入 request，供后续使用
        request.setAttribute(GRAY_FLAG, isGray);

        return true;
    }
}
