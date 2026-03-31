# SpringBoot3 过滤器



## 请求日志记录（Request/Response Logging）

```java
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * 请求日志过滤器
 *
 * 功能：
 * 1. 记录请求 URL、Method、Header、Body
 * 2. 记录响应状态码
 * 3. 统计接口耗时
 *
 * 注意：
 * 1. 支持 Body 可重复读取
 * 2. 避免流只能读取一次的问题
 *
 * @author atengk
 */
@Component
public class RequestLoggingFilter implements Filter {

    /**
     * 请求体缓存包装类
     */
    private static class CachedBodyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {

        /**
         * 缓存请求体
         */
        private final byte[] cachedBody;

        /**
         * 构造方法
         *
         * @param request 原始请求
         * @throws IOException IO异常
         */
        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        /**
         * 重写输入流
         *
         * @return ServletInputStream
         */
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(this.cachedBody);
            return new ServletInputStream() {

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

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        /**
         * 重写 reader
         *
         * @return BufferedReader
         */
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        /**
         * 获取请求体字符串
         *
         * @return 请求体
         */
        public String getBody() {
            return new String(this.cachedBody, StandardCharsets.UTF_8);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        long startTime = System.currentTimeMillis();

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(httpRequest);

        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        StringBuilder headerBuilder = new StringBuilder();
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = httpRequest.getHeader(headerName);
            headerBuilder.append(headerName)
                    .append(": ")
                    .append(headerValue)
                    .append("; ");
        }

        String requestBody = wrappedRequest.getBody();

        System.out.println("========== Request Start ==========");
        System.out.println("URI: " + requestUri);
        System.out.println("Method: " + method);
        System.out.println("Headers: " + headerBuilder);
        System.out.println("Body: " + requestBody);

        chain.doFilter(wrappedRequest, response);

        int status = httpResponse.getStatus();
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Status: " + status);
        System.out.println("Duration(ms): " + duration);
        System.out.println("========== Request End ==========");
    }
}
```

## 接口访问耗时统计（Performance Monitor）

```java id="k3m9x2"
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 接口性能监控过滤器
 *
 * 功能：
 * 1. 统计每个接口请求耗时
 * 2. 支持慢请求告警（超过阈值打印警告日志）
 * 3. 输出请求路径 + 方法 + 耗时
 *
 * 说明：
 * 1. 适用于接口性能排查
 * 2. 可扩展对接日志系统（如 ELK、SkyWalking）
 *
 * @author atengk
 */
@Component
public class PerformanceMonitorFilter implements Filter {

    /**
     * 慢请求阈值（毫秒）
     */
    private static final long SLOW_REQUEST_THRESHOLD = 500L;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        long startTime = System.currentTimeMillis();

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        try {
            chain.doFilter(request, response);
        } finally {

            long duration = System.currentTimeMillis() - startTime;

            if (duration > SLOW_REQUEST_THRESHOLD) {
                System.out.println("【SLOW REQUEST】URI: " + requestUri +
                        " Method: " + method +
                        " Duration(ms): " + duration);
            } else {
                System.out.println("URI: " + requestUri +
                        " Method: " + method +
                        " Duration(ms): " + duration);
            }
        }
    }
}
```

## Token 鉴权过滤（JWT / Token 校验）

```java id="p8x4m1"
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Token 鉴权过滤器（基于 JWT）
 *
 * 功能：
 * 1. 从请求头获取 Token
 * 2. 校验 Token 合法性
 * 3. 非法请求直接拦截返回 401
 *
 * 说明：
 * 1. 示例为简化版 JWT 校验逻辑（生产建议使用 jjwt / sa-token / spring security）
 * 2. 支持白名单接口放行
 *
 * @author atengk
 */
@Component
public class TokenAuthFilter implements Filter {

    /**
     * 请求头 Token Key
     */
    private static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Token 前缀
     */
    private static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 白名单路径（无需鉴权）
     */
    private static final String[] WHITE_LIST = {
            "/login",
            "/register",
            "/error"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestUri = httpRequest.getRequestURI();

        if (isWhiteList(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        String authorization = httpRequest.getHeader(HEADER_AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(TOKEN_PREFIX)) {
            unauthorized(httpResponse, "Token 缺失或格式错误");
            return;
        }

        String token = authorization.substring(TOKEN_PREFIX.length());

        if (!validateToken(token)) {
            unauthorized(httpResponse, "Token 无效或已过期");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 判断是否白名单
     *
     * @param uri 请求路径
     * @return 是否放行
     */
    private boolean isWhiteList(String uri) {
        for (String path : WHITE_LIST) {
            if (uri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Token 校验（示例实现）
     *
     * @param token token
     * @return 是否有效
     */
    private boolean validateToken(String token) {

        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 未授权返回
     *
     * @param response 响应
     * @param message 错误信息
     * @throws IOException IO异常
     */
    private void unauthorized(HttpServletResponse response, String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        String body = "{\"code\":401,\"message\":\"" + message + "\"}";
        response.getWriter().write(body);
    }
}
```

## 防重复提交（结合 Redis）

```java id="r9k2p7"
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * 防重复提交过滤器（基于 Redis）
 *
 * 功能：
 * 1. 防止短时间内重复提交相同请求
 * 2. 支持基于 URL + 参数 + Token 生成唯一标识
 * 3. 利用 Redis SETNX 实现分布式锁
 *
 * 说明：
 * 1. 适用于表单提交、下单等场景
 * 2. 默认 5 秒内相同请求视为重复
 *
 * @author atengk
 */
@Component
public class RepeatSubmitFilter implements Filter {

    /**
     * Redis Key 前缀
     */
    private static final String KEY_PREFIX = "repeat_submit:";

    /**
     * 过期时间（秒）
     */
    private static final long EXPIRE_SECONDS = 5L;

    private final StringRedisTemplate stringRedisTemplate;

    public RepeatSubmitFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 请求体缓存包装
     */
    private static class CachedBodyRequest extends jakarta.servlet.http.HttpServletRequestWrapper {

        private final byte[] body;

        public CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {

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

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        public String getBody() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        CachedBodyRequest wrappedRequest = new CachedBodyRequest(httpRequest);

        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        String body = wrappedRequest.getBody();
        String token = httpRequest.getHeader("Authorization");

        String key = KEY_PREFIX + buildKey(uri, method, body, token);

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(EXPIRE_SECONDS));

        if (Boolean.FALSE.equals(success)) {
            reject(httpResponse);
            return;
        }

        chain.doFilter(wrappedRequest, response);
    }

    /**
     * 构建唯一 Key
     *
     * @param uri    请求路径
     * @param method 请求方法
     * @param body   请求体
     * @param token  用户标识
     * @return key
     */
    private String buildKey(String uri, String method, String body, String token) {

        StringBuilder builder = new StringBuilder();
        builder.append(uri).append(":")
                .append(method).append(":")
                .append(Objects.toString(token, "")).append(":")
                .append(body.hashCode());

        return Integer.toHexString(builder.toString().hashCode());
    }

    /**
     * 拒绝重复提交
     *
     * @param response 响应
     * @throws IOException IO异常
     */
    private void reject(HttpServletResponse response) throws IOException {

        response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
        response.setContentType("application/json;charset=UTF-8");

        String body = "{\"code\":429,\"message\":\"请勿重复提交\"}";
        response.getWriter().write(body);
    }
}
```

## XSS 攻击过滤（参数清洗）

```java id="x7q2z9"
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * XSS 过滤器
 *
 * 功能：
 * 1. 过滤请求参数中的 XSS 攻击脚本
 * 2. 支持 Query 参数、Form 参数、Body 参数清洗
 *
 * 说明：
 * 1. 通过包装 HttpServletRequest 实现参数重写
 * 2. 仅做基础过滤，生产建议结合专业安全框架
 *
 * @author atengk
 */
@Component
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        XssRequestWrapper wrappedRequest = new XssRequestWrapper(httpRequest);

        chain.doFilter(wrappedRequest, response);
    }

    /**
     * XSS 请求包装类
     */
    private static class XssRequestWrapper extends HttpServletRequestWrapper {

        /**
         * 缓存 Body
         */
        private byte[] body;

        public XssRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
            this.body = cleanXss(new String(this.body, StandardCharsets.UTF_8))
                    .getBytes(StandardCharsets.UTF_8);
        }

        /**
         * 参数清洗
         */
        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return cleanXss(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            for (int i = 0; i < values.length; i++) {
                values[i] = cleanXss(values[i]);
            }
            return values;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> map = super.getParameterMap();
            map.forEach((key, values) -> {
                for (int i = 0; i < values.length; i++) {
                    values[i] = cleanXss(values[i]);
                }
            });
            return map;
        }

        /**
         * 重写 Body
         */
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {

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

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        /**
         * XSS 清洗核心逻辑
         *
         * @param value 原始值
         * @return 清洗后
         */
        private String cleanXss(String value) {

            if (value == null) {
                return null;
            }

            String result = value;

            result = result.replaceAll("<", "&lt;");
            result = result.replaceAll(">", "&gt;");
            result = result.replaceAll("\\(", "&#40;");
            result = result.replaceAll("\\)", "&#41;");
            result = result.replaceAll("'", "&#39;");
            result = result.replaceAll("\"", "&quot;");

            result = result.replaceAll("eval\\((.*)\\)", "");
            result = result.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"");
            result = result.replaceAll("script", "");

            return result;
        }
    }
}
```

## 跨域处理（CORS Filter 自定义版）

```java id="c4n8v2"
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 跨域过滤器（CORS 自定义版）
 *
 * 功能：
 * 1. 统一处理跨域请求
 * 2. 支持预检请求（OPTIONS）快速放行
 * 3. 可配置允许的域名、方法、Header
 *
 * 说明：
 * 1. 适用于前后端分离项目
 * 2. 优先级建议高于鉴权过滤器
 *
 * @author atengk
 */
@Component
public class CorsFilter implements Filter {

    /**
     * 允许的来源
     */
    private static final String ALLOW_ORIGIN = "*";

    /**
     * 允许的请求方法
     */
    private static final String ALLOW_METHODS = "GET,POST,PUT,DELETE,OPTIONS";

    /**
     * 允许的请求头
     */
    private static final String ALLOW_HEADERS = "Content-Type,Authorization";

    /**
     * 是否允许携带 Cookie
     */
    private static final String ALLOW_CREDENTIALS = "true";

    /**
     * 预检请求缓存时间（秒）
     */
    private static final String MAX_AGE = "3600";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader("Origin");

        if (origin != null) {
            httpResponse.setHeader("Access-Control-Allow-Origin", ALLOW_ORIGIN);
            httpResponse.setHeader("Access-Control-Allow-Methods", ALLOW_METHODS);
            httpResponse.setHeader("Access-Control-Allow-Headers", ALLOW_HEADERS);
            httpResponse.setHeader("Access-Control-Allow-Credentials", ALLOW_CREDENTIALS);
            httpResponse.setHeader("Access-Control-Max-Age", MAX_AGE);
        }

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}
```

## 请求参数统一包装（可重复读取 Body）

```java id="w6t3k9"
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 请求包装过滤器（支持 Body 可重复读取）
 *
 * 功能：
 * 1. 解决 HttpServletRequest InputStream 只能读取一次问题
 * 2. 支持后续 Filter / Interceptor / Controller 多次读取 Body
 * 3. 提供统一获取 Body 方法
 *
 * 说明：
 * 1. 适用于日志记录、签名校验、加解密等场景
 * 2. 建议作为全局基础 Filter 提前执行
 *
 * @author atengk
 */
@Component
public class RequestWrapperFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(httpRequest);

        chain.doFilter(wrapper, response);
    }

    /**
     * 请求包装类
     */
    public static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        /**
         * 缓存请求体
         */
        private final byte[] body;

        /**
         * 构造方法
         *
         * @param request 原始请求
         * @throws IOException IO异常
         */
        public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        /**
         * 重写输入流
         *
         * @return ServletInputStream
         */
        @Override
        public ServletInputStream getInputStream() {

            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);

            return new ServletInputStream() {

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

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        /**
         * 重写 reader
         *
         * @return BufferedReader
         */
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        /**
         * 获取请求体字符串
         *
         * @return body
         */
        public String getBody() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }
}
```

## IP 白名单 / 黑名单过滤

```java id="m2p7x5"
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * IP 访问控制过滤器（白名单 / 黑名单）
 *
 * 功能：
 * 1. 支持 IP 白名单控制（优先）
 * 2. 支持 IP 黑名单拦截
 * 3. 获取真实客户端 IP（支持代理场景）
 *
 * 说明：
 * 1. 白名单不为空时，仅允许白名单访问
 * 2. 黑名单始终生效
 * 3. 可扩展为 Redis 动态配置
 *
 * @author atengk
 */
@Component
public class IpFilter implements Filter {

    /**
     * IP 白名单
     */
    private static final Set<String> WHITE_LIST = new HashSet<>();

    /**
     * IP 黑名单
     */
    private static final Set<String> BLACK_LIST = new HashSet<>();

    static {
        WHITE_LIST.add("127.0.0.1");
        WHITE_LIST.add("192.168.1.100");

        BLACK_LIST.add("192.168.1.200");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);

        if (!WHITE_LIST.isEmpty() && !WHITE_LIST.contains(clientIp)) {
            reject(httpResponse, "IP 不在白名单");
            return;
        }

        if (BLACK_LIST.contains(clientIp)) {
            reject(httpResponse, "IP 已被封禁");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 获取客户端真实 IP
     *
     * @param request 请求
     * @return IP 地址
     */
    private String getClientIp(HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");

        if (isEmpty(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isEmpty(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isEmpty(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isEmpty(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isEmpty(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 是否为空
     *
     * @param str 字符串
     * @return 是否为空
     */
    private boolean isEmpty(String str) {
        return str == null || str.length() == 0 || "unknown".equalsIgnoreCase(str);
    }

    /**
     * 拒绝访问
     *
     * @param response 响应
     * @param message  错误信息
     * @throws IOException IO异常
     */
    private void reject(HttpServletResponse response, String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        String body = "{\"code\":403,\"message\":\"" + message + "\"}";
        response.getWriter().write(body);
    }
}
```

## 统一请求编码处理（UTF-8 Filter）

```java id="u8f2k4"
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 编码统一过滤器（UTF-8）
 *
 * 功能：
 * 1. 统一设置请求编码为 UTF-8
 * 2. 统一设置响应编码为 UTF-8
 * 3. 防止中文乱码问题
 *
 * 说明：
 * 1. 建议作为最先执行的过滤器
 * 2. Spring Boot 虽默认 UTF-8，但该过滤器用于兜底
 *
 * @author atengk
 */
@Component
public class EncodingFilter implements Filter {

    /**
     * 默认编码
     */
    private static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        request.setCharacterEncoding(DEFAULT_ENCODING);
        response.setCharacterEncoding(DEFAULT_ENCODING);

        response.setContentType("application/json;charset=" + DEFAULT_ENCODING);

        chain.doFilter(request, response);
    }
}
```

## 链路追踪 TraceId 注入（日志追踪）

```java id="t5r9v3"
package io.github.atengk.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 链路追踪过滤器
 *
 * 功能：
 * 1. 为每个请求生成唯一 TraceId
 * 2. 支持从请求头透传 TraceId（用于微服务链路）
 * 3. 将 TraceId 放入 MDC，方便日志统一打印
 *
 * 说明：
 * 1. 日志框架需配置 %X{traceId}
 * 2. 推荐放在最前面执行
 *
 * @author atengk
 */
@Component
public class TraceIdFilter implements Filter {

    /**
     * 请求头 TraceId Key
     */
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * MDC Key
     */
    private static final String MDC_TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String traceId = httpRequest.getHeader(HEADER_TRACE_ID);

        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }

        MDC.put(MDC_TRACE_ID, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    /**
     * 生成 TraceId
     *
     * @return traceId
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
```
