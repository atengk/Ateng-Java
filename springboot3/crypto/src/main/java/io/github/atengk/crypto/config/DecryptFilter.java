package io.github.atengk.crypto.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.crypto.dto.EncryptRequest;
import io.github.atengk.crypto.util.CryptoUtil;
import io.github.atengk.crypto.util.ReplayAttackUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;

/**
 * 解密过滤器
 * <p>
 * 功能：
 * 1. 仅拦截 JSON 请求（POST / PUT / PATCH）
 * 2. 自动跳过 GET / DELETE / 文件上传
 * 3. 支持白名单接口
 * 4. 防重放 + 验签 + 解密
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class DecryptFilter implements Filter {

    private final StringRedisTemplate redisTemplate;

    public DecryptFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        /*
         * 1. 请求方式过滤（只处理有 Body 的请求）
         */
        String method = req.getMethod();
        if (!"POST".equalsIgnoreCase(method)
                && !"PUT".equalsIgnoreCase(method)
                && !"PATCH".equalsIgnoreCase(method)) {

            chain.doFilter(request, response);
            return;
        }

        /*
         * 2. Content-Type 过滤（只处理 JSON）
         */
        String contentType = req.getContentType();
        if (StrUtil.isBlank(contentType)
                || !contentType.toLowerCase().contains("application/json")) {

            chain.doFilter(request, response);
            return;
        }

        /*
         * 3. 白名单接口（按需扩展）
         */
        String uri = req.getRequestURI();
        if (uri.contains("/login")
                || uri.contains("/captcha")
                || uri.contains("/public")) {

            chain.doFilter(request, response);
            return;
        }

        /*
         * 4. 包装请求（只在需要时）
         */
        CachedBodyHttpServletRequest wrapper = new CachedBodyHttpServletRequest(req);
        String body = wrapper.getBody();

        if (StrUtil.isBlank(body)) {
            chain.doFilter(request, response);
            return;
        }

        try {

            /*
             * 5. 转换请求体
             */
            EncryptRequest encryptRequest = JSONUtil.toBean(body, EncryptRequest.class);

            if (encryptRequest == null
                    || StrUtil.isBlank(encryptRequest.getData())) {

                throw new RuntimeException("非法加密请求");
            }

            /*
             * 6. 防重放
             */
            ReplayAttackUtil.checkTimestamp(encryptRequest.getTimestamp());
            ReplayAttackUtil.checkNonce(encryptRequest.getNonce(), redisTemplate);

            /*
             * 7. 验签
             */
            boolean verify = CryptoUtil.verify(
                    encryptRequest.getData(),
                    encryptRequest.getTimestamp(),
                    encryptRequest.getNonce(),
                    encryptRequest.getSign()
            );

            if (!verify) {
                throw new RuntimeException("签名校验失败");
            }

            /*
             * 8. 解密
             */
            String decryptData = CryptoUtil.decrypt(encryptRequest.getData());

            if (StrUtil.isBlank(decryptData)) {
                throw new RuntimeException("解密失败");
            }

            /*
             * 9. 替换请求体
             */
            HttpServletRequest newRequest =
                    new DecryptedHttpServletRequest(wrapper, decryptData);

            chain.doFilter(newRequest, response);

        } catch (Exception e) {

            /*
             * 统一异常（避免直接 500）
             */
            throw new RuntimeException("请求解密失败: " + e.getMessage());
        }
    }
}