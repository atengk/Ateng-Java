package io.github.atengk.crypto.config;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.crypto.annotation.Crypto;
import io.github.atengk.crypto.dto.EncryptRequest;
import io.github.atengk.crypto.util.CryptoUtil;
import io.github.atengk.crypto.util.ReplayAttackUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.List;

/**
 * 解密过滤器
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class DecryptFilter implements Filter {

    private final StringRedisTemplate redisTemplate;
    private final List<HandlerMapping> handlerMappings;

    public DecryptFilter(StringRedisTemplate redisTemplate,
                         List<HandlerMapping> handlerMappings) {
        this.redisTemplate = redisTemplate;
        this.handlerMappings = handlerMappings;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        /*
         * 1. 快速放行（方法 + ContentType）
         */
        if (!shouldProcess(req)) {
            chain.doFilter(request, response);
            return;
        }

        /*
         * 2. 获取 HandlerMethod
         */
        HandlerMethod handlerMethod = getHandler(req);
        if (handlerMethod == null) {
            chain.doFilter(request, response);
            return;
        }

        /*
         * 3. 判断是否需要解密
         */
        Crypto crypto = getCrypto(handlerMethod);
        if (ObjectUtil.isNull(crypto) || !crypto.decrypt()) {
            chain.doFilter(request, response);
            return;
        }

        /*
         * 4. 包装请求
         */
        CachedBodyHttpServletRequest wrapper = new CachedBodyHttpServletRequest(req);
        String body = wrapper.getBody();

        if (StrUtil.isBlank(body)) {
            throw new RuntimeException("请求体不能为空");
        }

        try {

            /*
             * 5. 解析请求体
             */
            EncryptRequest encryptRequest = parseRequest(body);

            /*
             * 6. 参数完整性校验
             */
            validateRequest(encryptRequest);

            /*
             * 7. 防重放
             */
            ReplayAttackUtil.checkTimestamp(encryptRequest.getTimestamp());
            ReplayAttackUtil.checkNonce(encryptRequest.getNonce(), redisTemplate);

            /*
             * 8. 验签（method + path）
             */
            boolean verify = CryptoUtil.verify(
                    normalizeMethod(req.getMethod()),
                    normalizePath(req),
                    encryptRequest.getData(),
                    encryptRequest.getTimestamp(),
                    encryptRequest.getNonce(),
                    encryptRequest.getSign()
            );

            if (!verify) {
                throw new RuntimeException("签名校验失败");
            }

            /*
             * 9. 解密
             */
            String decryptData = CryptoUtil.decrypt(encryptRequest.getData());

            if (StrUtil.isBlank(decryptData)) {
                throw new RuntimeException("解密失败");
            }

            /*
             * 10. 替换请求体
             */
            HttpServletRequest newRequest =
                    new DecryptedHttpServletRequest(wrapper, decryptData);

            chain.doFilter(newRequest, response);

        } catch (Exception e) {
            throw new RuntimeException("请求解密失败: " + e.getMessage());
        }
    }

    /**
     * 判断是否需要处理
     */
    private boolean shouldProcess(HttpServletRequest req) {

        String method = req.getMethod();

        if (!"POST".equalsIgnoreCase(method)
                && !"PUT".equalsIgnoreCase(method)
                && !"PATCH".equalsIgnoreCase(method)) {
            return false;
        }

        String contentType = req.getContentType();

        return StrUtil.isNotBlank(contentType)
                && contentType.toLowerCase().contains("application/json");
    }

    /**
     * 获取 @Crypto 注解
     */
    private Crypto getCrypto(HandlerMethod handlerMethod) {

        Crypto crypto = handlerMethod.getMethodAnnotation(Crypto.class);

        if (crypto == null) {
            crypto = handlerMethod.getBeanType().getAnnotation(Crypto.class);
        }

        return crypto;
    }

    /**
     * 解析请求体
     */
    private EncryptRequest parseRequest(String body) {

        try {
            return JSONUtil.toBean(body, EncryptRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("请求体格式错误");
        }
    }

    /**
     * 参数校验
     */
    private void validateRequest(EncryptRequest req) {

        if (ObjectUtil.isNull(req)
                || StrUtil.isBlank(req.getData())
                || ObjectUtil.isNull(req.getTimestamp())
                || StrUtil.isBlank(req.getNonce())
                || StrUtil.isBlank(req.getSign())) {

            throw new RuntimeException("加密参数不完整");
        }
    }

    /**
     * 标准化 Method
     */
    private String normalizeMethod(String method) {
        return StrUtil.toUpperCase(method);
    }

    /**
     * 标准化 Path
     */
    private String normalizePath(HttpServletRequest req) {

        String path = req.getRequestURI();

        if (StrUtil.isBlank(path)) {
            return "/";
        }

        path = path.replaceAll("//+", "/");

        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    /**
     * 获取 HandlerMethod
     */
    private HandlerMethod getHandler(HttpServletRequest request) {

        try {
            for (HandlerMapping mapping : handlerMappings) {
                HandlerExecutionChain chain = mapping.getHandler(request);
                if (chain != null && chain.getHandler() instanceof HandlerMethod) {
                    return (HandlerMethod) chain.getHandler();
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}