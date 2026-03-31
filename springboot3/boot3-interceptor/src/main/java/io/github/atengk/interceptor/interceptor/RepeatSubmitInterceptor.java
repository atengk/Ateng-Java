package io.github.atengk.interceptor.interceptor;

import io.github.atengk.interceptor.annotation.RepeatSubmit;
import io.github.atengk.interceptor.context.UserContext;
import io.github.atengk.interceptor.util.RedisUtil;
import io.github.atengk.interceptor.wrapper.RequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 防重复提交拦截器
 */
public class RepeatSubmitInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;

    public RepeatSubmitInterceptor(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 只拦截方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;

        // 判断是否有注解
        RepeatSubmit repeatSubmit = method.getMethodAnnotation(RepeatSubmit.class);
        if (repeatSubmit == null) {
            return true;
        }

        // 获取用户ID（必须登录场景）
        Long userId = UserContext.getUserId();
        if (userId == null) {
            response.setStatus(401);
            response.getWriter().write("未登录");
            return false;
        }

        // 构造唯一KEY：用户ID + URI + 参数摘要
        String uri = request.getRequestURI();
        String paramStr = buildParamString(request);
        String key = "repeat_submit:" + userId + ":" + uri + ":" + md5(paramStr);

        // 尝试写入 Redis（原子操作）
        boolean success = redisUtil.setIfAbsent(key, "1", repeatSubmit.expire());

        if (!success) {
            response.setStatus(429);
            response.getWriter().write("请勿重复提交");
            return false;
        }

        return true;
    }

    /**
     * 构造参数字符串
     */
    private String buildParamString(HttpServletRequest request) {

        StringBuilder sb = new StringBuilder();

        // GET参数
        request.getParameterMap().forEach((k, v) -> {
            sb.append(k).append("=");
            for (String val : v) {
                sb.append(val);
            }
            sb.append("&");
        });

        // JSON参数（如果使用了前面的 RequestWrapper）
        if (request instanceof RequestWrapper) {
            String body = ((RequestWrapper) request).getBody();
            sb.append(body);
        }

        return sb.toString();
    }

    /**
     * MD5摘要（用于缩短KEY）
     */
    private String md5(String str) throws Exception {

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            String s = Integer.toHexString(0xff & b);
            if (s.length() == 1) {
                hex.append('0');
            }
            hex.append(s);
        }
        return hex.toString();
    }
}
