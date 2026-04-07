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
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.extra.spring.SpringUtil;
import io.github.atengk.crypto.config.CryptoProperties;

import java.nio.charset.StandardCharsets;

/**
 * 加密工具类
 * <p>
 * 功能：
 * 1. AES 加密解密（带 IV）
 * 2. HmacSHA256 签名
 * 3. 签名校验
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class CryptoUtil {

    private static final CryptoProperties cryptoProperties = SpringUtil.getBean(CryptoProperties.class);

    /**
     * AES Key（16/24/32字节）
     */
    private static final byte[] AES_KEY = Base64.decode(cryptoProperties.getAesKey());
    
    /**
     * IV（必须和前端一致）
     */
    private static final byte[] AES_IV = Base64.decode(cryptoProperties.getIv());
    
    /**
     * 签名 Key
     */
    private static final byte[] SIGN_KEY = cryptoProperties.getSignKey().getBytes(StandardCharsets.UTF_8);

    /**
     * AES 加密（CBC + PKCS5Padding）
     */
    public static String encrypt(String data) {
        AES aes = new AES("CBC", "PKCS5Padding", AES_KEY, AES_IV);
        return aes.encryptBase64(data);
    }

    /**
     * AES 解密
     */
    public static String decrypt(String data) {
        AES aes = new AES("CBC", "PKCS5Padding", AES_KEY, AES_IV);
        return aes.decryptStr(data);
    }

    /**
     * 生成签名（推荐结构化拼接）
     */
    public static String sign(String data, long timestamp, String nonce) {

        String content = buildSignContent(data, timestamp, nonce);

        HMac mac = new HMac(HmacAlgorithm.HmacSHA256, SIGN_KEY);
        return mac.digestHex(content);
    }

    /**
     * 校验签名
     */
    public static boolean verify(String data, long timestamp, String nonce, String sign) {

        String localSign = sign(data, timestamp, nonce);

        return localSign.equalsIgnoreCase(sign);
    }

    /**
     * 构建签名字符串（避免拼接歧义）
     */
    private static String buildSignContent(String data, long timestamp, String nonce) {

        /*
         * 使用 key=value 结构，避免：
         * data=12 + 34 和 data=1 + 234 冲突
         */
        return "data=" + data +
                "&timestamp=" + timestamp +
                "&nonce=" + nonce;
    }

}
```

### 防重放工具类

```java
package io.github.atengk.crypto.util;


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

    private static final long EXPIRE_TIME = 5 * 60 * 1000;

    /**
     * 校验时间戳
     */
    public static void checkTimestamp(Long timestamp) {

        long now = System.currentTimeMillis();

        if (timestamp == null || Math.abs(now - timestamp) > EXPIRE_TIME) {
            throw new RuntimeException("请求已过期");
        }
    }

    /**
     * 校验 nonce（必须唯一）
     */
    public static void checkNonce(String nonce, StringRedisTemplate redisTemplate) {

        if (StrUtil.isBlank(nonce)) {
            throw new RuntimeException("nonce 不能为空");
        }

        String key = "crypto:nonce:" + nonce;

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(success)) {
            throw new RuntimeException("重复请求");
        }
    }
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
    public FilterRegistrationBean<Filter> decryptFilter(StringRedisTemplate redisTemplate) {

        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();

        registration.setFilter(new DecryptFilter(redisTemplate));

        /*
         * 拦截路径（按需调整）
         */
        registration.addUrlPatterns("/api/*");

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
package io.github.atengk.crypto.advice;

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
        return true;
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

### 前端对接规范（Vue3 / TS）

加密流程

```vue
import CryptoJS from "crypto-js"

const AES_KEY = "xxx"
const IV = "xxx"
const SIGN_KEY = "xxx"

export function encrypt(data: any) {
  const json = JSON.stringify(data)

  const encrypted = CryptoJS.AES.encrypt(
    json,
    CryptoJS.enc.Utf8.parse(AES_KEY),
    {
      iv: CryptoJS.enc.Utf8.parse(IV),
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }
  ).toString()

  const timestamp = Date.now()
  const nonce = Math.random().toString(36).substring(2)

  const sign = CryptoJS.HmacSHA256(
    `data=${encrypted}&timestamp=${timestamp}&nonce=${nonce}`,
    SIGN_KEY
  ).toString()

  return {
    data: encrypted,
    timestamp,
    nonce,
    sign
  }
}
```

