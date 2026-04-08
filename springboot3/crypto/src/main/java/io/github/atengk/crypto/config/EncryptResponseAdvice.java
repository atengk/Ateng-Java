package io.github.atengk.crypto.config;

import io.github.atengk.crypto.annotation.Crypto;
import io.github.atengk.crypto.util.CryptoUtil;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应加密
 *
 * @author 孔余
 * @since 2026-01-29
 */
@RestControllerAdvice
public class EncryptResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        Crypto crypto = returnType.getMethodAnnotation(Crypto.class);

        if (crypto == null) {
            crypto = returnType.getContainingClass().getAnnotation(Crypto.class);
        }

        return crypto != null && crypto.encrypt();
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  org.springframework.http.server.ServerHttpRequest request,
                                  org.springframework.http.server.ServerHttpResponse response) {

        String json = cn.hutool.json.JSONUtil.toJsonStr(body);

        String encrypt = CryptoUtil.encrypt(json);

        return encrypt;
    }
}
