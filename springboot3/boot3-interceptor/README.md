# SpringBoot3 拦截器



## 登录鉴权拦截（Token / Session 校验）

通过拦截器统一校验请求中的 Token（或 Session），未登录请求直接拦截返回，登录用户信息写入上下文，供后续使用。

### 1️⃣ 用户上下文工具类（ThreadLocal）

```java
package com.example.demo.context;

/**
 * 用户上下文（用于在一次请求中存储用户信息）
 */
public class UserContext {

    /**
     * 使用 ThreadLocal 存储当前线程的用户信息
     */
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 清理，防止内存泄漏（必须在请求结束调用）
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
```

------

### 2️⃣ Token 工具类（示例：简单模拟，实际可用 JWT）

```java
package com.example.demo.util;

/**
 * Token 工具类（示例用，实际项目建议使用 JWT）
 */
public class TokenUtil {

    /**
     * 模拟解析 Token
     * 规则：token = userId（简单演示）
     */
    public static Long parseToken(String token) {
        try {
            return Long.parseLong(token);
        } catch (Exception e) {
            return null;
        }
    }
}
```

------

### 3️⃣ 登录拦截器

```java
package com.example.demo.interceptor;

import com.example.demo.context.UserContext;
import com.example.demo.util.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 请求进入Controller之前执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 从请求头中获取 Token（常见做法：Authorization 或 token）
        String token = request.getHeader("Authorization");

        // Token 为空，直接拦截
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("未登录");
            return false;
        }

        // 解析 Token
        Long userId = TokenUtil.parseToken(token);

        // 解析失败，说明 Token 非法
        if (userId == null) {
            response.setStatus(401);
            response.getWriter().write("Token无效");
            return false;
        }

        // 存入上下文（后续业务可直接获取）
        UserContext.setUserId(userId);

        return true;
    }

    /**
     * 请求完成后执行（一定会执行）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        // 清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}
```

------

### 4️⃣ 拦截器配置

```java
package com.example.demo.config;

import com.example.demo.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns(
                        "/login",       // 登录接口放行
                        "/error",
                        "/static/**"
                );
    }
}
```

------

### 5️⃣ 测试 Controller

```java
package com.example.demo.controller;

import com.example.demo.context.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 */
@RestController
public class TestController {

    /**
     * 需要登录的接口
     */
    @GetMapping("/test")
    public String test() {

        // 从上下文获取用户ID
        Long userId = UserContext.getUserId();

        return "当前用户ID：" + userId;
    }
}
```

✔ 请求示例

```http
GET /test
Authorization: 1001
```

✔ 返回结果

```text
当前用户ID：1001
```

------

## 接口访问日志记录（请求参数 + 响应耗时）

通过拦截器统一记录接口访问日志：包含请求路径、请求参数、客户端IP、执行耗时等信息，便于排查问题与性能分析。

### 1️⃣ 请求包装类（解决请求体只能读一次问题）

```java
package com.example.demo.wrapper;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * HttpServletRequest 包装类
 * 解决 request.getInputStream() 只能读取一次的问题
 */
public class RequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public RequestWrapper(HttpServletRequest request) throws IOException {
        super(request);

        // 读取请求体并缓存
        InputStream is = request.getInputStream();
        this.body = is.readAllBytes();
    }

    /**
     * 重写输入流
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bis = new ByteArrayInputStream(body);

        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bis.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {}

            @Override
            public int read() {
                return bis.read();
            }
        };
    }

    /**
     * 获取请求体字符串
     */
    public String getBody() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
```

------

### 2️⃣ 日志拦截器

```java
package com.example.demo.interceptor;

import com.example.demo.wrapper.RequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * 接口日志拦截器
 */
public class LogInterceptor implements HandlerInterceptor {

    /**
     * 线程变量：记录开始时间
     */
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    /**
     * 请求进入时
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 记录开始时间
        START_TIME.set(System.currentTimeMillis());

        return true;
    }

    /**
     * Controller执行后（但未返回视图）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        // 计算耗时
        long cost = System.currentTimeMillis() - START_TIME.get();

        // 获取请求信息
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String ip = getClientIp(request);

        // 获取参数
        Map<String, Object> params = new HashMap<>();

        // GET参数
        request.getParameterMap().forEach((k, v) -> params.put(k, v));

        // POST JSON参数（需使用包装类）
        String body = "";
        if (request instanceof RequestWrapper) {
            body = ((RequestWrapper) request).getBody();
        }

        // 打印日志（实际项目建议使用 log 框架）
        System.out.println("==== 接口访问日志 ====");
        System.out.println("URI: " + uri);
        System.out.println("Method: " + method);
        System.out.println("IP: " + ip);
        System.out.println("Params: " + params);
        System.out.println("Body: " + body);
        System.out.println("耗时: " + cost + " ms");
        System.out.println("=====================");

        // 清理 ThreadLocal
        START_TIME.remove();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}
```

------

### 3️⃣ 过滤器（将 request 替换为可重复读取的包装类）

```java
package com.example.demo.filter;

import com.example.demo.wrapper.RequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * 请求包装过滤器
 */
public class RequestWrapperFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            RequestWrapper wrapper = new RequestWrapper((HttpServletRequest) request);

            // 使用包装后的 request
            chain.doFilter(wrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
```

------

### 4️⃣ 注册拦截器 + 过滤器

```java
package com.example.demo.config;

import com.example.demo.filter.RequestWrapperFilter;
import com.example.demo.interceptor.LogInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 注册日志拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LogInterceptor())
                .addPathPatterns("/**");
    }

    /**
     * 注册请求包装过滤器
     */
    @Bean
    public FilterRegistrationBean<RequestWrapperFilter> requestWrapperFilter() {

        FilterRegistrationBean<RequestWrapperFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestWrapperFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(1); // 优先级要高

        return bean;
    }
}
```

------

### 5️⃣ 测试 Controller

```java
package com.example.demo.controller;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.web.bind.annotation.*;

/**
 * 测试接口
 */
@RestController
public class TestController {

    @PostMapping("/test")
    public String test(@RequestBody JSONObject body) {
        return "ok";
    }
    
}
```

```text
==== 接口访问日志 ====
URI: /test
Method: POST
IP: 127.0.0.1
Params: {}
Body: {"name":"test","age":18}
耗时: 12 ms
=====================
```

------

## 接口防重复提交（幂等性控制）

通过拦截器 + 自定义注解 + Redis，实现接口幂等控制：同一用户在短时间内重复提交相同请求时直接拦截，避免重复下单/重复支付等问题。

------

### 1️⃣ 自定义注解

```java
package com.example.demo.annotation;

import java.lang.annotation.*;

/**
 * 防重复提交注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /**
     * 防重复时间窗口（秒）
     */
    int expire() default 5;
}
```

------

### 2️⃣ Redis 工具类（简化版，基于 StringRedisTemplate）

```java
package com.example.demo.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 */
@Component
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    public RedisUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * setIfAbsent（SETNX）
     */
    public boolean setIfAbsent(String key, String value, long expireSeconds) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }
}
```

------

### 3️⃣ 防重复提交拦截器

```java
package com.example.demo.interceptor;

import com.example.demo.annotation.RepeatSubmit;
import com.example.demo.context.UserContext;
import com.example.demo.util.RedisUtil;
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
        if (request instanceof com.example.demo.wrapper.RequestWrapper) {
            String body = ((com.example.demo.wrapper.RequestWrapper) request).getBody();
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
```

------

### 4️⃣ 拦截器注册

```java
package com.example.demo.config;

import com.example.demo.interceptor.RepeatSubmitInterceptor;
import com.example.demo.util.RedisUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RedisUtil redisUtil;

    public WebConfig(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**");
        registry.addInterceptor(new RepeatSubmitInterceptor(redisUtil))
                .addPathPatterns("/**");
    }
}
```

------

### 5️⃣ 使用示例（Controller）

```java
package com.example.demo.controller;

import com.example.demo.annotation.RepeatSubmit;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 */
@RestController
public class OrderController {

    /**
     * 模拟下单接口（防重复提交）
     */
    @RepeatSubmit(expire = 10) // 10秒内不能重复提交
    @PostMapping("/order")
    public String createOrder() {

        // 模拟业务处理
        return "下单成功";
    }
}
```

------

✅ 请求示例

```http
POST /order
Authorization: 1001
Content-Type: application/json

{"productId":1,"count":1}
```

------

✅ 效果

- ✅ 第一次请求：正常执行
- ❌ 10秒内重复请求：返回

```text
请勿重复提交
```

------

## 接口限流（简单计数 / 滑动窗口）

通过拦截器 + Redis，实现接口限流：限制单位时间内请求次数，防止接口被刷、恶意请求或突发流量冲击。

------

### 🧩 方案一：简单计数限流（固定窗口）

------

#### 1️⃣ 自定义注解

```java
package com.example.demo.annotation;

import java.lang.annotation.*;

/**
 * 限流注解（固定窗口）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 时间窗口（秒）
     */
    int time() default 60;

    /**
     * 最大请求次数
     */
    int count() default 10;
}
```

------

#### 2️⃣ Redis 工具类

```java
package com.example.demo.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 */
@Component
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    public RedisUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 自增并设置过期时间
     */
    public Long increment(String key, long expireSeconds) {

        Long count = redisTemplate.opsForValue().increment(key);

        // 第一次设置过期时间
        if (count != null && count == 1) {
            redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        }

        return count;
    }
}
```

------

#### 3️⃣ 限流拦截器（固定窗口）

```java
package com.example.demo.interceptor;

import com.example.demo.annotation.RateLimit;
import com.example.demo.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 固定窗口限流拦截器
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;

    public RateLimitInterceptor(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;

        // 获取注解
        RateLimit rateLimit = method.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        // 构造限流KEY（IP + URI）
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        String key = "rate_limit:" + ip + ":" + uri;

        // 计数
        Long count = redisUtil.increment(key, rateLimit.time());

        // 超过限制
        if (count != null && count > rateLimit.count()) {
            response.setStatus(429);
            response.getWriter().write("请求过于频繁，请稍后再试");
            return false;
        }

        return true;
    }
}
```

------

#### 4️⃣ 注册拦截器

```java
package com.example.demo.config;

import com.example.demo.interceptor.RateLimitInterceptor;
import com.example.demo.util.RedisUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RedisUtil redisUtil;

    public WebConfig(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new RateLimitInterceptor(redisUtil))
                .addPathPatterns("/**");
    }
}
```

------

#### 5️⃣ 使用示例

```java
package com.example.demo.controller;

import com.example.demo.annotation.RateLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 */
@RestController
public class TestController {

    /**
     * 10秒内最多访问5次
     */
    @RateLimit(time = 10, count = 5)
    @GetMapping("/limit/test")
    public String test() {
        return "请求成功";
    }
}
```

------

### 🧩 方案二：滑动窗口限流（更精准）

------

#### 1️⃣ 自定义注解

```java
package com.example.demo.annotation;

import java.lang.annotation.*;

/**
 * 滑动窗口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlidingWindowLimit {

    /**
     * 时间窗口（秒）
     */
    int time() default 60;

    /**
     * 最大请求数
     */
    int count() default 10;
}
```

------

#### 2️⃣ Redis 工具类（ZSet实现）

```java
package com.example.demo.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类（滑动窗口）
 */
@Component
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    public RedisUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 滑动窗口计数
     */
    public Long slidingWindowCount(String key, long windowSeconds) {

        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000;

        // 删除窗口外数据
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // 当前请求加入
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);

        // 设置过期时间
        redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);

        // 返回当前窗口内数量
        return redisTemplate.opsForZSet().zCard(key);
    }
}
```

------

#### 3️⃣ 限流拦截器（滑动窗口）

```java
package com.example.demo.interceptor;

import com.example.demo.annotation.SlidingWindowLimit;
import com.example.demo.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 滑动窗口限流拦截器
 */
public class SlidingWindowInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;

    public SlidingWindowInterceptor(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;

        SlidingWindowLimit limit = method.getMethodAnnotation(SlidingWindowLimit.class);
        if (limit == null) {
            return true;
        }

        // 构造KEY
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        String key = "sliding_limit:" + ip + ":" + uri;

        // 当前窗口请求数
        Long count = redisUtil.slidingWindowCount(key, limit.time());

        if (count != null && count > limit.count()) {
            response.setStatus(429);
            response.getWriter().write("请求过于频繁（滑动窗口）");
            return false;
        }

        return true;
    }
}
```

------

#### 4️⃣ 注册拦截器

```java
package com.example.demo.config;

import com.example.demo.interceptor.SlidingWindowInterceptor;
import com.example.demo.util.RedisUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RedisUtil redisUtil;

    public WebConfig(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new SlidingWindowInterceptor(redisUtil))
                .addPathPatterns("/**");
    }
}
```

------

#### 5️⃣ 使用示例

```java
package com.example.demo.controller;

import com.example.demo.annotation.SlidingWindowLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 */
@RestController
public class TestController {

    /**
     * 10秒内最多5次（滑动窗口）
     */
    @SlidingWindowLimit(time = 10, count = 5)
    @GetMapping("/limit/sw")
    public String test() {
        return "请求成功";
    }
}
```

------

### ✅ 效果对比

| 方案     | 特点                     |
| -------- | ------------------------ |
| 固定窗口 | 实现简单，但边界突刺问题 |
| 滑动窗口 | 更平滑精准，生产更常用   |

------

## 权限校验（RBAC / 角色权限控制）

通过 **拦截器 + 自定义注解 + RBAC模型**，实现接口权限控制：根据当前用户角色判断是否有访问权限，无权限直接拦截。

------

### 1️⃣ 用户上下文（存储用户ID + 角色）

```java
package com.example.demo.context;

/**
 * 用户上下文
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    /**
     * 设置用户信息
     */
    public static void set(Long userId, String role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 获取角色
     */
    public static String getRole() {
        return ROLE.get();
    }

    /**
     * 清理
     */
    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
```

------

### 2️⃣ Token 工具类（模拟解析用户 + 角色）

```java
package com.example.demo.util;

/**
 * Token工具类（示例）
 * 格式：userId:role 例如 1001:ADMIN
 */
public class TokenUtil {

    public static Long getUserId(String token) {
        try {
            return Long.parseLong(token.split(":")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRole(String token) {
        try {
            return token.split(":")[1];
        } catch (Exception e) {
            return null;
        }
    }
}
```

------

### 3️⃣ 登录拦截器（写入用户信息）

```java
package com.example.demo.interceptor;

import com.example.demo.context.UserContext;
import com.example.demo.util.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器（为权限校验提供用户信息）
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.getWriter().write("未登录");
            return false;
        }

        Long userId = TokenUtil.getUserId(token);
        String role = TokenUtil.getRole(token);

        if (userId == null || role == null) {
            response.setStatus(401);
            response.getWriter().write("Token无效");
            return false;
        }

        // 写入上下文
        UserContext.set(userId, role);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
```

------

### 4️⃣ 权限注解

```java
package com.example.demo.annotation;

import java.lang.annotation.*;

/**
 * 权限注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 允许访问的角色
     */
    String[] value();
}
```

------

### 5️⃣ 权限拦截器

```java
package com.example.demo.interceptor;

import com.example.demo.annotation.RequireRole;
import com.example.demo.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 权限拦截器（RBAC）
 */
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;

        // 获取注解
        RequireRole requireRole = method.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            return true;
        }

        // 当前用户角色
        String userRole = UserContext.getRole();

        if (userRole == null) {
            response.setStatus(401);
            response.getWriter().write("未登录");
            return false;
        }

        // 是否在允许角色内
        boolean allowed = Arrays.asList(requireRole.value()).contains(userRole);

        if (!allowed) {
            response.setStatus(403);
            response.getWriter().write("无权限访问");
            return false;
        }

        return true;
    }
}
```

------

### 6️⃣ 拦截器配置

```java
package com.example.demo.config;

import com.example.demo.interceptor.AuthInterceptor;
import com.example.demo.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 登录拦截器（先执行）
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/login");

        // 权限拦截器（后执行）
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/**");
    }
}
```

------

### 7️⃣ 使用示例

```java
package com.example.demo.controller;

import com.example.demo.annotation.RequireRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 */
@RestController
public class AdminController {

    /**
     * 仅 ADMIN 可访问
     */
    @RequireRole({"ADMIN"})
    @GetMapping("/admin")
    public String admin() {
        return "管理员接口";
    }

    /**
     * ADMIN / USER 均可访问
     */
    @RequireRole({"ADMIN", "USER"})
    @GetMapping("/user")
    public String user() {
        return "用户接口";
    }
}
```

------

✅ 请求示例

```http
GET /admin
Authorization: 1001:ADMIN
```

------

✅ 效果

- ✅ ADMIN 访问 `/admin` → 成功
- ❌ USER 访问 `/admin` → `403 无权限访问`
- ❌ 未登录 → `401 未登录`

------

## 接口签名校验（防篡改 / 防重放）

通过 **拦截器 + 自定义注解 + 签名算法 + Redis防重放**，校验请求是否合法：

- 防篡改：参数被修改会导致签名不一致
- 防重放：同一请求只能使用一次（nonce + 时间戳控制）

------

### 1️⃣ 自定义注解

```java
package com.example.demo.annotation;

import java.lang.annotation.*;

/**
 * 接口签名校验注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SignCheck {

    /**
     * 过期时间（秒）
     */
    int expire() default 60;
}
```

------

### 2️⃣ Redis 工具类（防重放）

```java
package com.example.demo.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Component
public class RedisUtil {

    private final StringRedisTemplate redisTemplate;

    public RedisUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 防重放（SETNX）
     */
    public boolean setIfAbsent(String key, long expireSeconds) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
}
```

------

### 3️⃣ 签名工具类

```java
package com.example.demo.util;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 签名工具类
 */
public class SignUtil {

    private static final String SECRET = "demo_secret_key"; // 服务端密钥

    /**
     * 生成签名
     */
    public static String generateSign(Map<String, String> params) throws Exception {

        // 1. 参数排序
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        // 2. 拼接字符串
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if ("sign".equals(key)) continue; // 排除sign本身
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                sb.append(key).append("=").append(value).append("&");
            }
        }

        // 3. 拼接密钥
        sb.append("key=").append(SECRET);

        // 4. MD5
        return md5(sb.toString());
    }

    /**
     * 从 request 获取参数
     */
    public static Map<String, String> getParams(HttpServletRequest request) {

        Map<String, String> map = new HashMap<>();

        request.getParameterMap().forEach((k, v) -> {
            if (v.length > 0) {
                map.put(k, v[0]);
            }
        });

        return map;
    }

    /**
     * MD5
     */
    private static String md5(String str) throws Exception {

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
```

------

### 4️⃣ 签名拦截器

```java
package com.example.demo.interceptor;

import com.example.demo.annotation.SignCheck;
import com.example.demo.util.RedisUtil;
import com.example.demo.util.SignUtil;
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
```

------

### 5️⃣ 拦截器配置

```java
package com.example.demo.config;

import com.example.demo.interceptor.SignInterceptor;
import com.example.demo.util.RedisUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RedisUtil redisUtil;

    public WebConfig(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new SignInterceptor(redisUtil))
                .addPathPatterns("/api/**");
    }
}
```

------

### 6️⃣ 使用示例

```java
package com.example.demo.controller;

import com.example.demo.annotation.SignCheck;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 */
@RestController
public class ApiController {

    /**
     * 需要签名校验
     */
    @SignCheck(expire = 60)
    @GetMapping("/api/test")
    public String test() {
        return "访问成功";
    }
}
```

------

✅ 请求示例

```http
GET /api/test?name=test&timestamp=1774943475&nonce=abc123&sign=xxx
```

------

✅ 签名规则

```text
1. 参数按字典序排序
2. 拼接：key=value&key=value
3. 最后拼接：&key=SECRET
4. MD5生成sign
```

------

✅ 效果

- ❌ 参数被篡改 → `签名错误`
- ❌ 超时请求 → `请求已过期`
- ❌ 重复请求 → `重复请求`
- ✅ 正常请求 → 返回成功

------

## 请求上下文注入（用户信息 ThreadLocal）

通过 **拦截器 + ThreadLocal**，在请求开始时注入用户信息（如 userId、username），在业务代码中可随时获取，无需层层传参，请求结束自动清理。

------

### 1️⃣ 用户上下文工具类

```java
package com.example.demo.context;

/**
 * 用户上下文（ThreadLocal实现）
 */
public class UserContext {

    /**
     * 用户ID
     */
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /**
     * 用户名
     */
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    /**
     * 设置用户信息
     */
    public static void set(Long userId, String username) {
        USER_ID.set(userId);
        USERNAME.set(username);
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 获取用户名
     */
    public static String getUsername() {
        return USERNAME.get();
    }

    /**
     * 清理（必须）
     */
    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
    }
}
```

------

### 2️⃣ Token 工具类（模拟解析）

```java
package com.example.demo.util;

/**
 * Token工具类
 * 格式：userId:username 例如 1001:tom
 */
public class TokenUtil {

    public static Long getUserId(String token) {
        try {
            return Long.parseLong(token.split(":")[0]);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getUsername(String token) {
        try {
            return token.split(":")[1];
        } catch (Exception e) {
            return null;
        }
    }
}
```

------

### 3️⃣ 上下文注入拦截器

```java
package com.example.demo.interceptor;

import com.example.demo.context.UserContext;
import com.example.demo.util.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文注入拦截器
 */
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 从请求头获取Token
        String token = request.getHeader("Authorization");

        if (token != null && !token.isEmpty()) {

            Long userId = TokenUtil.getUserId(token);
            String username = TokenUtil.getUsername(token);

            if (userId != null && username != null) {
                // 注入上下文
                UserContext.set(userId, username);
            }
        }

        return true;
    }

    /**
     * 请求完成后清理 ThreadLocal
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
```

------

### 4️⃣ 拦截器配置

```java
package com.example.demo.config;

import com.example.demo.interceptor.UserContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new UserContextInterceptor())
                .addPathPatterns("/**");
    }
}
```

------

### 5️⃣ 使用示例（Controller）

```java
package com.example.demo.controller;

import com.example.demo.context.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口
 */
@RestController
public class UserController {

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user/info")
    public String getUserInfo() {

        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();

        return "用户ID：" + userId + "，用户名：" + username;
    }
}
```

------

✅ 请求示例

```http
GET /user/info
Authorization: 1001:tom
```

------

✅ 返回结果

```text
用户ID：1001，用户名：tom
```

------

⚠️ 关键点

- ✅ 必须 `afterCompletion` 清理 ThreadLocal（防内存泄漏）
- ✅ 不要在子线程直接使用（需传递上下文）
- ✅ 适合存：用户信息、TraceId、租户ID等

------

## 跨域 / Header 统一处理（补充非网关场景）

通过 **拦截器 + 过滤器**，统一处理跨域（CORS）和响应头（如 Token、TraceId 等），适用于未接入网关或需要服务内部补充 Header 的场景。

------

### 🧩 方案一：拦截器实现（简单统一 Header 处理）

------

#### 1️⃣ Header 处理拦截器

```java
package com.example.demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Header统一处理拦截器
 */
public class HeaderInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // ========================
        // 1. 设置跨域响应头（基础版）
        // ========================
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type,Authorization,Trace-Id");
        response.setHeader("Access-Control-Max-Age", "3600");

        // ========================
        // 2. 处理预检请求（OPTIONS）
        // ========================
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(200);
            return false;
        }

        // ========================
        // 3. TraceId处理（链路追踪）
        // ========================
        String traceId = request.getHeader("Trace-Id");

        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }

        // 回写到响应头
        response.setHeader("Trace-Id", traceId);

        return true;
    }
}
```

------

#### 2️⃣ 拦截器配置

```java
package com.example.demo.config;

import com.example.demo.interceptor.HeaderInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new HeaderInterceptor())
                .addPathPatterns("/**");
    }
}
```

------

### 🧩 方案二：过滤器实现（推荐，处理跨域更标准）

------

#### 1️⃣ CORS 过滤器

```java
package com.example.demo.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 跨域过滤器（推荐方式）
 */
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // ========================
        // 1. 设置跨域头
        // ========================
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type,Authorization,Trace-Id");
        res.setHeader("Access-Control-Allow-Credentials", "true");
        res.setHeader("Access-Control-Max-Age", "3600");

        // ========================
        // 2. 处理预检请求
        // ========================
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}
```

------

#### 2️⃣ TraceId 过滤器（统一链路ID）

```java
package com.example.demo.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId过滤器
 */
public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // 获取或生成 TraceId
        String traceId = req.getHeader("Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }

        // 写入响应头
        res.setHeader("Trace-Id", traceId);

        chain.doFilter(request, response);
    }
}
```

------

#### 3️⃣ 过滤器注册

```java
package com.example.demo.config;

import com.example.demo.filter.CorsFilter;
import com.example.demo.filter.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filter配置
 */
@Configuration
public class FilterConfig {

    /**
     * CORS过滤器
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new CorsFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(1); // 优先级最高

        return bean;
    }

    /**
     * TraceId过滤器
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {

        FilterRegistrationBean<TraceIdFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new TraceIdFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(2);

        return bean;
    }
}
```

------

✅ 请求示例

```http
OPTIONS /api/test
Origin: http://localhost:3000
```

------

✅ 响应头效果

```text
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
Access-Control-Allow-Headers: Content-Type,Authorization,Trace-Id
Trace-Id: 8f3c9c2e-xxxx
```

------

⚠️ 关键点

- ✅ **跨域建议用 Filter 实现（优先级更高）**
- ✅ 拦截器适合做 Header 补充，不适合完整 CORS
- ✅ 生产环境不要用 `*`（需指定域名）
- ✅ TraceId 建议贯穿日志体系（配合 MDC）

------

## 灰度发布 / AB测试控制（按用户或Header分流）

通过 **拦截器 + 自定义注解 + 分流规则**，实现请求灰度控制：

- 按用户ID分流（稳定灰度）
- 按Header分流（手动控制）
- 支持A/B版本切换（如 v1 / v2）

------

### 1️⃣ 灰度注解

```java
package com.example.demo.annotation;

import java.lang.annotation.*;

/**
 * 灰度控制注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrayRelease {

    /**
     * 灰度比例（0-100）
     */
    int percent() default 10;

    /**
     * Header控制Key（可选）
     */
    String header() default "Gray-Version";

    /**
     * 灰度版本标识
     */
    String version() default "v2";
}
```

------

### 2️⃣ 用户上下文（用于获取用户ID）

```java
package com.example.demo.context;

/**
 * 用户上下文
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
```

------

### 3️⃣ 灰度拦截器

```java
package com.example.demo.interceptor;

import com.example.demo.annotation.GrayRelease;
import com.example.demo.context.UserContext;
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
```

------

### 4️⃣ 拦截器配置

```java
package com.example.demo.config;

import com.example.demo.interceptor.GrayReleaseInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new GrayReleaseInterceptor())
                .addPathPatterns("/**");
    }
}
```

------

### 5️⃣ 使用示例（Controller 分流）

```java
package com.example.demo.controller;

import com.example.demo.annotation.GrayRelease;
import com.example.demo.interceptor.GrayReleaseInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 灰度测试接口
 */
@RestController
public class GrayController {

    /**
     * 灰度接口（10%用户走新版本）
     */
    @GrayRelease(percent = 10, version = "v2")
    @GetMapping("/api/gray")
    public String gray(HttpServletRequest request) {

        boolean isGray = Boolean.TRUE.equals(
                request.getAttribute(GrayReleaseInterceptor.GRAY_FLAG)
        );

        if (isGray) {
            // 新版本逻辑
            return "v2 新版本逻辑";
        } else {
            // 老版本逻辑
            return "v1 老版本逻辑";
        }
    }
}
```

------

✅ 请求示例

✔ 按用户ID灰度（自动）

```http
GET /api/gray
Authorization: 1001
```

------

✔ 强制进入灰度（Header控制）

```http
GET /api/gray
Gray-Version: v2
```

------

✅ 效果

- ✅ 10%用户 → 进入 v2
- ✅ 指定 Header → 强制进入 v2
- ✅ 其他用户 → 走 v1

------

⚠️ 关键点

- ✅ 用户ID取模 → **稳定灰度（不会跳来跳去）**
- ✅ Header控制 → **测试/回滚利器**
- ✅ request传递标记 → **避免重复计算**
- ✅ 可扩展：按IP、设备、地区分流

------

🚀 可扩展方向（生产增强）

- 灰度用户白名单（Redis配置）
- 动态比例（配置中心控制）
- 多版本（v1/v2/v3）
- 灰度链路日志（配合TraceId）

------



