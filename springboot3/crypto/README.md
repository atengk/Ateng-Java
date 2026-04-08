# 接口加密解密

标准方案是：

* **HTTPS（TLS）**：传输层加密（必须）
* **签名（防篡改 + 身份）**
* **对称加密（保护敏感字段或整体 payload）**
* **时间戳 + nonce（防重放）**

## 基础配置

### 生成 Key

**创建工具类**

```java
package io.github.atengk.crypto.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.extra.spring.SpringUtil;
import io.github.atengk.crypto.config.CryptoProperties;

import java.nio.charset.StandardCharsets;

/**
 * Key 生成工具类
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class CryptoGenerateUtil {

    /**
     * 生成 AES Key（Base64）
     */
    public static String generateAesKey() {
        return Base64.encode(RandomUtil.randomBytes(16));
    }

    /**
     * 生成 AES Key（Hex）
     */
    public static String generateAesKeyHex() {
        return HexUtil.encodeHexStr(RandomUtil.randomBytes(16));
    }

    /**
     * 生成签名 Key
     */
    public static String generateSignKey() {
        return SecureUtil.sha256(RandomUtil.randomString(32));
    }

    /**
     * 生成随机 IV
     */
    public static String generateIv() {
        return Base64.encode(RandomUtil.randomBytes(16));
    }
}
```

**测试类生成key**

```java
package io.github.atengk.crypto;

import io.github.atengk.crypto.util.CryptoGenerateUtil;
import org.junit.jupiter.api.Test;

/**
 * Key 生成
 */
public class KeyGeneratorTests {

    @Test
    public void AES_KEY() {
        String key = CryptoGenerateUtil.generateAesKey();
        System.out.println(key);
        // QD2RQPTG8ujbImZVwYeVeQ==
    }

    @Test
    public void SIGN_KEY() {
        String signKey = CryptoGenerateUtil.generateSignKey();
        System.out.println(signKey);
        // 676182be2b2adc09ab80a989f305222c454b7002dfb0b4fa3ef97c230ff94f2c
    }

    @Test
    public void IV() {
        String iv = CryptoGenerateUtil.generateIv();
        System.out.println(iv);
        // paY8aTRpCzpppJ5hwb64pw==
    }

}

```

### 加密配置属性

**加密配置属性**

通过上方测试类生成的加密配置填写到以下配置中

```yaml
---
# 加密配置属性
crypto:
  aes-key: QD2RQPTG8ujbImZVwYeVeQ==
  sign-key: 676182be2b2adc09ab80a989f305222c454b7002dfb0b4fa3ef97c230ff94f2c
  iv: paY8aTRpCzpppJ5hwb64pw==

```

**加密配置属性类**

```java
package io.github.atengk.crypto.config;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 加密配置属性
 * <p>
 * 用于加载 AES Key、签名 Key、IV
 *
 * @author 孔余
 * @since 2026-01-29
 */
@Component
@Data
@ConfigurationProperties(prefix = "crypto")
public class CryptoProperties {

    /**
     * AES 密钥（Base64 或明文 16/24/32 位）
     */
    private String aesKey;

    /**
     * 签名密钥
     */
    private String signKey;

    /**
     * 初始向量 IV
     */
    private String iv;

    /**
     * 初始化校验（企业级必须）
     */
    @PostConstruct
    public void validate() {

        if (StrUtil.isBlank(aesKey)) {
            throw new IllegalArgumentException("crypto.aes-key 不能为空");
        }

        if (StrUtil.isBlank(signKey)) {
            throw new IllegalArgumentException("crypto.sign-key 不能为空");
        }

        if (StrUtil.isBlank(iv)) {
            throw new IllegalArgumentException("crypto.iv 不能为空");
        }

        /*
         * AES Key 校验（解码后必须 16/24/32 字节）
         */
        byte[] keyBytes = Base64.decode(aesKey);
        int keyLength = keyBytes.length;

        if (keyLength != 16 && keyLength != 24 && keyLength != 32) {
            throw new IllegalArgumentException("crypto.aes-key 解码后长度必须为 16/24/32 字节");
        }

        /*
         * IV 校验（解码后必须 16 字节）
         */
        byte[] ivBytes = Base64.decode(iv);

        if (ivBytes.length != 16) {
            throw new IllegalArgumentException("crypto.iv 解码后长度必须为 16 字节");
        }
    }
}

```

### 加密请求体

```java
package io.github.atengk.crypto.dto;

import lombok.Data;

/**
 * 加密请求体
 * <p>
 * 用于前后端统一传输加密数据，字段说明如下：
 * 1. data：业务数据经过 AES 加密后的字符串（Base64 编码）
 * 2. timestamp：请求时间戳（毫秒），用于防重放攻击
 * 3. nonce：随机字符串（一次性使用），用于防重放攻击
 * 4. sign：签名值（HmacSHA256），用于防篡改校验
 *
 * 请求处理流程：
 * 1. 服务端先校验 timestamp（时间窗口）
 * 2. 校验 nonce 是否重复（Redis）
 * 3. 校验 sign 是否正确
 * 4. 最后解密 data 得到原始业务数据
 *
 * @author 孔余
 * @since 2026-01-29
 */
@Data
public class EncryptRequest {

    /**
     * 加密数据（AES 加密后的 Base64 字符串）
     */
    private String data;

    /**
     * 时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 随机字符串（防重放）
     */
    private String nonce;

    /**
     * 签名（HmacSHA256）
     */
    private String sign;
}
```

### 加密工具类

```java
package io.github.atengk.crypto.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.AES;
import io.github.atengk.crypto.config.CryptoProperties;

import java.nio.charset.StandardCharsets;

/**
 * 加密工具类
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class CryptoUtil {

    private static byte[] AES_KEY;
    private static byte[] AES_IV;
    private static byte[] SIGN_KEY;

    private static AES AES_INSTANCE;
    private static HMac HMAC_INSTANCE;

    /**
     * 初始化（由 Spring 调用）
     */
    public static void init(CryptoProperties properties) {

        AES_KEY = Base64.decode(properties.getAesKey());
        AES_IV = Base64.decode(properties.getIv());
        SIGN_KEY = properties.getSignKey().getBytes(StandardCharsets.UTF_8);

        AES_INSTANCE = new AES("CBC", "PKCS5Padding", AES_KEY, AES_IV);
        HMAC_INSTANCE = new HMac(HmacAlgorithm.HmacSHA256, SIGN_KEY);
    }

    /**
     * AES 加密
     */
    public static String encrypt(String data) {

        if (StrUtil.isBlank(data)) {
            return StrUtil.EMPTY;
        }

        return AES_INSTANCE.encryptBase64(data);
    }

    /**
     * AES 解密
     */
    public static String decrypt(String data) {

        if (StrUtil.isBlank(data)) {
            return StrUtil.EMPTY;
        }

        return AES_INSTANCE.decryptStr(data);
    }

    /**
     * 生成签名
     */
    public static String sign(String method,
                              String path,
                              String data,
                              long timestamp,
                              String nonce) {

        String content = buildSignContent(method, path, data, timestamp, nonce);

        return HMAC_INSTANCE.digestHex(content);
    }

    /**
     * 校验签名（常量时间比较）
     */
    public static boolean verify(String method,
                                 String path,
                                 String data,
                                 long timestamp,
                                 String nonce,
                                 String sign) {

        if (StrUtil.isBlank(sign)) {
            return false;
        }

        String localSign = sign(method, path, data, timestamp, nonce);

        return constantTimeEquals(localSign, sign);
    }

    /**
     * 构建签名字符串
     */
    private static String buildSignContent(String method,
                                           String path,
                                           String data,
                                           long timestamp,
                                           String nonce) {

        return "method=" + normalizeMethod(method) +
                "&path=" + normalizePath(path) +
                "&data=" + safe(data) +
                "&timestamp=" + timestamp +
                "&nonce=" + safe(nonce);
    }

    /**
     * Method 标准化
     */
    private static String normalizeMethod(String method) {
        return StrUtil.toUpperCase(StrUtil.blankToDefault(method, "GET"));
    }

    /**
     * Path 标准化
     */
    private static String normalizePath(String path) {

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
     * 防止 null
     */
    private static String safe(String str) {
        return ObjectUtil.defaultIfNull(str, "");
    }

    /**
     * 常量时间比较（防时序攻击）
     */
    private static boolean constantTimeEquals(String a, String b) {

        if (a == null || b == null) {
            return false;
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;

        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }
}
```

### 初始化类加密

```java
package io.github.atengk.crypto.config;

import io.github.atengk.crypto.util.CryptoUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * 加密初始化配置
 *
 * @author 孔余
 * @since 2026-01-29
 */
@Configuration
public class CryptoInitConfig {

    private final CryptoProperties cryptoProperties;

    public CryptoInitConfig(CryptoProperties cryptoProperties) {
        this.cryptoProperties = cryptoProperties;
    }

    @PostConstruct
    public void init() {
        CryptoUtil.init(cryptoProperties);
    }
}

```

### 防重放工具类

```java
package io.github.atengk.crypto.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 防重放工具类
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class ReplayAttackUtil {

    /**
     * 默认时间窗口（毫秒）
     */
    private static final long DEFAULT_EXPIRE_TIME = 5 * 60 * 1000;

    /**
     * 允许的最大未来偏移（毫秒）
     */
    private static final long MAX_FUTURE_TIME = 60 * 1000;

    /**
     * Redis Key 前缀
     */
    private static final String NONCE_PREFIX = "crypto:nonce:";

    /**
     * 校验时间戳
     */
    public static void checkTimestamp(Long timestamp) {

        checkTimestamp(timestamp, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 校验时间戳（支持自定义窗口）
     */
    public static void checkTimestamp(Long timestamp, long expireTime) {

        if (ObjectUtil.isNull(timestamp)) {
            throw new RuntimeException("timestamp 不能为空");
        }

        long now = System.currentTimeMillis();

        /*
         * 1. 防止过期请求
         */
        if (now - timestamp > expireTime) {
            throw new RuntimeException("请求已过期");
        }

        /*
         * 2. 防止未来时间攻击（客户端时间伪造）
         */
        if (timestamp - now > MAX_FUTURE_TIME) {
            throw new RuntimeException("非法请求（时间异常）");
        }
    }

    /**
     * 校验 nonce（默认）
     */
    public static void checkNonce(String nonce,
                                  StringRedisTemplate redisTemplate) {

        checkNonce(null, nonce, redisTemplate, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 校验 nonce（支持 appId 隔离）
     */
    public static void checkNonce(String appId,
                                  String nonce,
                                  StringRedisTemplate redisTemplate,
                                  long expireTime) {

        if (StrUtil.isBlank(nonce)) {
            throw new RuntimeException("nonce 不能为空");
        }

        /*
         * 构建 Redis Key
         * 格式：
         * crypto:nonce:{appId}:{nonce}
         */
        String key = buildNonceKey(appId, nonce);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMillis(expireTime));

        /*
         * null 也视为失败（极端情况）
         */
        if (!Boolean.TRUE.equals(success)) {
            throw new RuntimeException("重复请求");
        }
    }

    /**
     * 构建 nonce key
     */
    private static String buildNonceKey(String appId, String nonce) {

        if (StrUtil.isBlank(appId)) {
            return NONCE_PREFIX + nonce;
        }

        return NONCE_PREFIX + appId + ":" + nonce;
    }
}
```

### 接口加解密注解

```java
package io.github.atengk.crypto.annotation;

import java.lang.annotation.*;

/**
 * 接口加解密注解
 *
 * @author 孔余
 * @since 2026-01-29
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Crypto {

    /**
     * 是否解密请求
     */
    boolean decrypt() default true;

    /**
     * 是否加密响应
     */
    boolean encrypt() default true;
}
```



## 请求解密配置

### Request 包装类（支持多次读取 Body）

代码功能：缓存请求体，支持后续重复读取

```java
package io.github.atengk.crypto.config;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取 Request
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        body = request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {

        ByteArrayInputStream inputStream = new ByteArrayInputStream(body);

        return new ServletInputStream() {

            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }
        };
    }

    public String getBody() {
        return new String(body, StandardCharsets.UTF_8);
    }
}

```

### 解密后 Request 替换

代码功能：将解密后的 JSON 作为新的请求体

```java
package io.github.atengk.crypto.config;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 解密后请求包装
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class DecryptedHttpServletRequest extends CachedBodyHttpServletRequest {

    private final byte[] newBody;

    public DecryptedHttpServletRequest(HttpServletRequest request, String body) throws IOException {
        super(request);
        this.newBody = body.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {

        ByteArrayInputStream inputStream = new ByteArrayInputStream(newBody);

        return new ServletInputStream() {

            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }
        };
    }
}

```



### 解密过滤器（核心实现）

代码功能：统一解密请求 + 验签 + 防重放

```java
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
```

### FilterRegistrationBean 注册

将 DecryptFilter 注册到 Spring 容器并指定拦截规则

```java
package io.github.atengk.crypto.config;

import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;

/**
 * 加密过滤器配置
 *
 * @author 孔余
 * @since 2026-01-29
 */
@Configuration
public class CryptoFilterConfig {

    /**
     * 注册解密过滤器
     */
    @Bean
    public FilterRegistrationBean<Filter> decryptFilter(
            StringRedisTemplate redisTemplate,
            List<HandlerMapping> handlerMappings) {

        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();

        registration.setFilter(new DecryptFilter(redisTemplate, handlerMappings));

        /*
         * 拦截路径（按需调整）
         */
        registration.addUrlPatterns("/*");

        /*
         * 执行顺序（建议靠前）
         */
        registration.setOrder(1);

        registration.setName("decryptFilter");

        return registration;
    }
}
```



## 响应加密（ResponseBodyAdvice）

企业级推荐：统一返回自动加密

### 响应加密处理器

代码功能：统一对响应结果进行 AES 加密 + 包装

```java
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

```

## 测试使用

### 后端接口

```java
package io.github.atengk.crypto.controller;

import io.github.atengk.crypto.annotation.Crypto;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/crypto")
public class TestController {

    @Crypto
    @GetMapping("/get")
    public String get(@RequestParam String name) {
        return "GET 接收参数：" + name;
    }

    @Crypto(decrypt = true, encrypt = false)
    @PostMapping("/post")
    public Map<String, Object> post(@RequestBody Map<String, Object> body) {
        return body;
    }

    @PostMapping("/ignore")
    public Map<String, Object> ignore(@RequestBody Map<String, Object> body) {
        return body;
    }
}

```

### 前端调用

#### 核心加密工具（前端）

代码功能：实现 AES + 签名（与后端 CryptoUtil 对齐）

```ts
import CryptoJS from "crypto-js"

const AES_KEY = "QD2RQPTG8ujbImZVwYeVeQ=="
const IV = "paY8aTRpCzpppJ5hwb64pw=="
const SIGN_KEY = "676182be2b2adc09ab80a989f305222c454b7002dfb0b4fa3ef97c230ff94f2c"

function normalizePath(path: string) {
  return path.replace(/\/+/g, "/").replace(/\/$/, "") || "/"
}

export function encrypt(data: any) {
  const json = JSON.stringify(data)

  const encrypted = CryptoJS.AES.encrypt(
    json,
    CryptoJS.enc.Base64.parse(AES_KEY),
    {
      iv: CryptoJS.enc.Base64.parse(IV),
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }
  ).toString()

  return encrypted
}

export function decrypt(data: string) {
  const decrypted = CryptoJS.AES.decrypt(
    data,
    CryptoJS.enc.Base64.parse(AES_KEY),
    {
      iv: CryptoJS.enc.Base64.parse(IV),
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }
  )

  return decrypted.toString(CryptoJS.enc.Utf8)
}

export function sign(method: string, path: string, data: string, timestamp: number, nonce: string) {
  const content =
    `method=${method.toUpperCase()}` +
    `&path=${normalizePath(path)}` +
    `&data=${data}` +
    `&timestamp=${timestamp}` +
    `&nonce=${nonce}`

  return CryptoJS.HmacSHA256(content, SIGN_KEY).toString()
}
```

------

#### Axios 封装（核心）

代码功能：自动处理加密请求 + 解密响应（按接口控制）

```ts
import axios from "axios"
import { encrypt, decrypt, sign } from "./crypto"

const service = axios.create({
  baseURL: "/api",
  timeout: 10000
})

/**
 * 请求拦截（加密）
 */
service.interceptors.request.use(config => {

  const needEncrypt = config.headers?.["X-Encrypt"] === true

  if (!needEncrypt) {
    return config
  }

  const data = config.data || {}

  const encrypted = encrypt(data)

  const timestamp = Date.now()
  const nonce = Math.random().toString(36).substring(2)

  const path = config.url || ""

  const signature = sign(
    config.method || "POST",
    path,
    encrypted,
    timestamp,
    nonce
  )

  config.data = {
    data: encrypted,
    timestamp,
    nonce,
    sign: signature
  }

  return config
})

/**
 * 响应拦截（解密）
 */
service.interceptors.response.use(res => {

  const needDecrypt = res.config.headers?.["X-Decrypt"] === true

  if (!needDecrypt) {
    return res
  }

  const encrypted = res.data

  const decrypted = decrypt(encrypted)

  try {
    res.data = JSON.parse(decrypted)
  } catch {
    res.data = decrypted
  }

  return res
})

export default service
```

------

#### 接口调用示例（对应你 Controller）

------

1. `/crypto/get`（仅响应加密）

```ts
import service from "./request"

/**
 * GET：不加密请求，只解密响应
 */
export function testGet(name: string) {
  return service.get("/crypto/get", {
    params: { name },
    headers: {
      "X-Decrypt": true
    }
  })
}
```

------

2. `/crypto/post`（仅请求解密）

```ts
/**
 * POST：加密请求，不解密响应
 */
export function testPost(data: any) {
  return service.post("/crypto/post", data, {
    headers: {
      "X-Encrypt": true
    }
  })
}
```

------

3. `/crypto/ignore`（完全不加密）

```ts
/**
 * 明文接口
 */
export function testIgnore(data: any) {
  return service.post("/crypto/ignore", data)
}
```

------

#### 调用示例（页面中）

```ts
// GET（自动解密）
testGet("Ateng").then(res => {
  console.log(res.data)
})

// POST（自动加密）
testPost({ name: "Ateng", age: 18 }).then(res => {
  console.log(res.data)
})

// 明文
testIgnore({ hello: "world" }).then(res => {
  console.log(res.data)
})
```

------



