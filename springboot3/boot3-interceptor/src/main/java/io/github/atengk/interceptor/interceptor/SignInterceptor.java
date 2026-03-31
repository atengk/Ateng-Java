package io.github.atengk.interceptor.interceptor;

import io.github.atengk.interceptor.annotation.SignCheck;
import io.github.atengk.interceptor.util.RedisUtil;
import io.github.atengk.interceptor.util.SignUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 签名校验拦截器
 */
public class SignInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;

    public SignInterceptor(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        response.setCharacterEncoding("UTF-8");

        SignCheck signCheck = method.getMethodAnnotation(SignCheck.class);
        if (signCheck == null) {
            return true;
        }

        Map<String, String> params = SignUtil.getParams(request);

        String clientSign = params.get("sign");
        String timestamp = params.get("timestamp");
        String nonce = params.get("nonce");

        // 基础参数校验
        if (clientSign == null || timestamp == null || nonce == null) {
            response.setStatus(400);
            response.getWriter().write("缺少签名参数");
            return false;
        }

        // 时间戳校验（防止过期）
        long now = System.currentTimeMillis() / 1000;
        long reqTime = Long.parseLong(timestamp);

        if (Math.abs(now - reqTime) > signCheck.expire()) {
            response.setStatus(400);
            response.getWriter().write("请求已过期");
            return false;
        }

        // 防重放（nonce）
        String nonceKey = "sign_nonce:" + nonce;
        boolean first = redisUtil.setIfAbsent(nonceKey, signCheck.expire());

        if (!first) {
            response.setStatus(400);
            response.getWriter().write("重复请求");
            return false;
        }

        // 服务端生成签名
        String serverSign = SignUtil.generateSign(params);

        // 校验签名
        if (!serverSign.equals(clientSign)) {
            response.setStatus(400);
            response.getWriter().write("签名错误");
            return false;
        }

        return true;
    }
}
