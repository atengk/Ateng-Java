package io.github.atengk.basic.interceptor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.basic.holder.IdempotentTokenContextHolder;
import io.github.atengk.basic.util.IdempotentUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * 幂等拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class IdempotentInterceptor implements HandlerInterceptor {

    private static final String HEADER_TOKEN = "X-Idempotent-Token";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        String token = request.getHeader(HEADER_TOKEN);

        if (StrUtil.isBlank(token)) {
            throw new RuntimeException("缺少幂等 Token");
        }

        // 放入 ThreadLocal
        IdempotentTokenContextHolder.set(token);

        // 幂等校验
        IdempotentUtil.checkAndSave(token);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        IdempotentTokenContextHolder.clear();
    }
}
