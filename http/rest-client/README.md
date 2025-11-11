# RestClient

从 **Spring Framework 6**（即 **Spring Boot 3**）开始，Spring 引入了一个全新的同步 HTTP 客户端：
 👉 **`org.springframework.web.client.RestClient`**

它是对老旧的 `RestTemplate` 的现代化替代，语法更简洁、更符合函数式风格，也内置了对响应式配置的兼容。

**特点：**

- 基于 `HttpClient` / `OkHttp` / `JDK HttpClient` 实现；
- 支持 Builder 链式调用；
- 可全局配置超时、拦截器、转换器；
- 可自定义序列化与反序列化方式。

Spring Boot 3 已默认包含 RestClient，**无需额外依赖**。



## 使用RestClient

### GET 请求

```java
    /**
     * GET 请求
     */
    @Test
    void test() {
        RestClient client = RestClient.create();

        String url = "https://jsonplaceholder.typicode.com/posts/1";
        ResponseEntity<String> response = client.get()
                .uri(url)
                .retrieve()
                .toEntity(String.class);

        System.out.println(response.getBody());
    }
```

### GET 请求（带参数）

```java
    /**
     * GET 请求（带参数）
     */
    @Test
    void test1() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 1);

        String result = RestClient.create()
                .get()
                .uri("https://jsonplaceholder.typicode.com/posts?userId={userId}", params)
                .retrieve()
                .body(String.class);

        System.out.println(result);
    }
```

### POST 请求（提交 JSON）

```java
    /**
     * POST 请求（提交 JSON）
     */
    @Test
    void test2() {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Spring Boot 3 RestClient");
        body.put("body", "This is a test post");
        body.put("userId", 1);

        String result = RestClient.create()
                .post()
                .uri("https://jsonplaceholder.typicode.com/posts")
                .body(body)
                .retrieve()
                .body(String.class);

        System.out.println(result);
    }
```

### PUT 请求（更新资源）

```java
    /**
     * PUT 请求（更新资源）
     */
    @Test
    void test3() {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Updated Title");

        ResponseEntity<Void> response = RestClient.create()
                .put()
                .uri("https://jsonplaceholder.typicode.com/posts/{id}", 1)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        System.out.println(response.getStatusCode());
    }
```

### DELETE 请求

```java
    /**
     * DELETE 请求
     */
    @Test
    void test4() {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Spring Boot 3 RestClient");
        body.put("body", "This is a test post");
        body.put("userId", 1);

        ResponseEntity<Void> response = RestClient.create()
                .delete()
                .uri("https://jsonplaceholder.typicode.com/posts/{id}", 1)
                .retrieve()
                .toBodilessEntity();

        System.out.println(response.getStatusCode());
    }
```

### Header 与 Query 参数设置

```java
    /**
     * Header 与 Query 参数设置
     */
    @Test
    void test5() {
        String result = RestClient.builder()
                .defaultHeader("Authorization", "Bearer 123456")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("jsonplaceholder.typicode.com")
                        .path("/posts")
                        .queryParam("page", 1)
                        .queryParam("size", 10)
                        .build())
                .retrieve()
                .body(String.class);

        System.out.println(result);
    }
```

### 自定义错误处理

```java
    /**
     * 自定义错误处理
     */
    @Test
    void test6() {
        try {
            RestClient.create()
                    .get()
                    .uri("https://api.example.com/error")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            System.out.println("Error: " + e.getStatusText());
            System.out.println("Body: " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            System.out.println("Connection Error: " + e.getMessage());
        }
    }
```



## 全局配置

### 添加依赖

```xml
<!-- Apache HttpClient 5 -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

### 编辑配置

```java
package io.github.atengk.restclient.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * 定义 RestClient Bean（可直接注入使用）
     *
     * @return RestClient 对象
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                // 可改成你项目的网关地址
                .baseUrl("https://jsonplaceholder.typicode.com")
                .requestFactory(httpRequestFactory())
                .defaultHeader("User-Agent", "SpringBoot3-RestClient")
                .build();
    }

    /**
     * 创建 HttpClient 请求工厂
     *
     * @return ClientHttpRequestFactory 对象
     */
    private ClientHttpRequestFactory httpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(httpClient());
    }

    /**
     * 创建 HttpClient 实例
     *
     * @return HttpClient 对象
     */
    private HttpClient httpClient() {
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        // 设置最大连接总数
                        .setMaxConnTotal(200)
                        // 设置每路由最大连接数
                        .setMaxConnPerRoute(50)
                        // 设置空闲连接验证间隔
                        .setValidateAfterInactivity(TimeValue.ofSeconds(30))
                        .build();

        RequestConfig requestConfig = RequestConfig.custom()
                // 连接建立超时
                .setConnectTimeout(Timeout.ofSeconds(5))
                // 响应超时
                .setResponseTimeout(Timeout.ofSeconds(10))
                // 从连接池获取连接超时
                .setConnectionRequestTimeout(Timeout.ofSeconds(2))
                .build();

        // 构建 HttpClient
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                // 清理过期连接
                .evictExpiredConnections()
                // 清理空闲连接
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
```

### 使用

如果有多个 Bean，可以使用 `@Qualifier("atengRestClient")` 指定。

```java
package io.github.atengk.restclient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@SpringBootTest
public class SpringTests {
    @Autowired
    private RestClient restClient;

    @Test
    void test() {
        ResponseEntity<String> response = restClient.get()
                .uri("/posts/1")
                .retrieve()
                .toEntity(String.class);

        System.out.println(response.getBody());
    }


}
```



## 使用拦截器

### 创建并注册拦截器

#### 创建拦截器

```java
package io.github.atengk.restclient.interceptor;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MyRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        // 在请求之前执行的操作，比如添加请求头、日志记录等
        System.out.println("请求 URL: " + request.getURI());
        System.out.println("请求方法: " + request.getMethod());

        // 继续执行请求
        return execution.execute(request, body);
    }
}
```

#### 注册拦截器

```java
@Bean
public RestClient restClient() {
    return RestClient.builder()
            // 可改成你项目的网关地址
            .baseUrl("https://jsonplaceholder.typicode.com")
            .requestFactory(httpRequestFactory())
            .requestInterceptors(list -> {
                list.add(new MyRequestInterceptor());
            })
            .defaultHeader("User-Agent", "SpringBoot3-RestClient")
            .build();
}
```

### 重试拦截器

```java
package io.github.atengk.restclient.interceptor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RestClient 拦截器：用于在请求失败时自动进行重试，并支持指数退避策略。
 *
 * <p>支持以下两类失败情况自动重试：
 * <ul>
 *     <li>网络异常（如连接超时、读取超时等）</li>
 *     <li>服务端错误（状态码 5xx）</li>
 * </ul>
 *
 * <p>支持通过静态方法 {@link #setMaxRetries(int)} 动态设置最大重试次数，具备线程安全性。
 * 默认采用指数退避策略，避免高并发下请求雪崩。
 *
 * @author 孔余
 * @since 2025-07-30
 */
@Component
@Slf4j
public class RetryInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 最大重试次数（支持线程安全修改）
     * 默认为 3 次
     */
    private static final AtomicInteger MAX_RETRIES = new AtomicInteger(3);

    /**
     * 初始重试等待时间（单位：毫秒），每次重试会指数增长
     */
    private static final long INITIAL_INTERVAL_MS = 300;

    /**
     * 最大重试等待时间（单位：毫秒），用于限制指数退避的上限
     */
    private static final long MAX_INTERVAL_MS = 5000;

    /**
     * 设置最大重试次数（必须大于 0）
     *
     * @param retries 新的最大重试次数
     */
    public static void setMaxRetries(int retries) {
        if (retries > 0) {
            MAX_RETRIES.set(retries);
        } else {
            log.warn("设置的最大重试次数无效：{}", retries);
        }
    }

    /**
     * 拦截请求并执行重试逻辑
     *
     * @param request   当前请求对象
     * @param body      请求体内容字节数组
     * @param execution 请求执行器，用于继续调用链
     * @return 请求响应
     * @throws IOException 当超过最大重试次数后仍然失败，则抛出异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        int attempt = 0;

        while (true) {
            try {
                // 正常执行请求
                return execution.execute(request, body);

            } catch (HttpStatusCodeException e) {
                // 处理服务端错误（5xx）重试
                if (shouldRetry(e.getStatusCode().value()) && attempt < MAX_RETRIES.get()) {
                    attempt++;
                    long waitTime = calculateBackoffTime(attempt);
                    log.warn("请求失败（状态码：{}），第 {} 次重试：{}，等待 {} 毫秒",
                            e.getStatusCode().value(), attempt, request.getURI(), waitTime);
                    sleep(waitTime);
                } else {
                    log.error("请求失败，状态码：{}，不再重试：{}", e.getStatusCode(), request.getURI());
                    throw e;
                }

            } catch (IOException e) {
                // 网络异常重试
                if (attempt < MAX_RETRIES.get()) {
                    attempt++;
                    long waitTime = calculateBackoffTime(attempt);
                    log.warn("网络异常，第 {} 次重试：{}，等待 {} 毫秒，异常信息：{}",
                            attempt, request.getURI(), waitTime, e.getMessage());
                    sleep(waitTime);
                } else {
                    log.error("网络异常，重试结束：{}，异常信息：{}", request.getURI(), e.getMessage());
                    throw e;
                }
            }
        }
    }

    /**
     * 判断当前状态码是否应当进行重试
     *
     * @param statusCode HTTP 状态码
     * @return 是否应当重试
     */
    private boolean shouldRetry(int statusCode) {
        // 默认只重试服务端错误（5xx）
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * 根据当前重试次数计算指数退避时间，并限制最大等待时间
     *
     * @param attempt 当前重试次数（从 1 开始）
     * @return 等待时间（单位：毫秒）
     */
    private long calculateBackoffTime(int attempt) {
        long waitTime = (long) (INITIAL_INTERVAL_MS * Math.pow(2, attempt - 1));
        return Math.min(waitTime, MAX_INTERVAL_MS);
    }

    /**
     * 安全执行线程等待，如果被中断则恢复线程中断状态
     *
     * @param millis 等待时间（单位：毫秒）
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}


```

### 认证拦截器

```java
package io.github.atengk.restclient.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestClient 拦截器：用于添加统一的认证 Token（如 JWT）到请求头中。
 * <p>
 * 注意：此实现为模拟 Token 获取逻辑，实际生产中应接入真实的缓存（如 Redis）或配置中心。
 *
 * @author 孔余
 * @since 2025-07-30
 */
@Component
@Slf4j
public class AuthInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 拦截请求，添加 Authorization 头。
     *
     * @param request   原始请求
     * @param body      请求体字节数组
     * @param execution 请求执行器（用于继续调用链）
     * @return 响应结果
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        // 获取 Token（可替换为从 Redis、配置中心或上下文中获取）
        String token = getToken();

        // 日志记录请求 URI 和添加 token 的行为（仅开发调试阶段启用）
        log.debug("Adding Authorization token to request: {}", request.getURI());

        // 若 token 为空，可选择记录日志或抛出异常（视业务场景而定）
        if (token == null || token.isEmpty()) {
            log.warn("Authorization token is missing.");
            // 你也可以选择抛出自定义异常
            // throw new IllegalStateException("Missing authorization token");
        }

        // 设置请求头
        HttpHeaders headers = request.getHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        // 执行请求
        return execution.execute(request, body);
    }

    /**
     * 获取 Token 的方法（可扩展为从缓存或配置服务获取）
     *
     * @return JWT Token 字符串
     */
    private String getToken() {
        // 模拟：返回一个硬编码 Token（请替换为实际获取逻辑）
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    }
}

```

### 日志拦截器

```java
package io.github.atengk.restclient.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * RestClient 拦截器：记录请求和响应的详细日志
 *
 * @author 孔余
 * @since 2025-07-30
 */
@Component
@Slf4j
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 拦截请求并打印日志
     *
     * @param request   当前请求
     * @param body      请求体
     * @param execution 拦截器执行器（用于传递调用链）
     * @return 响应结果
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        long startTime = System.currentTimeMillis();

        log.info("====== 请求开始 ======");
        log.info("请求地址: {}", request.getURI());
        log.info("请求方式: {}", request.getMethod());
        log.info("请求头: {}", request.getHeaders());
        // 记录请求体，仅限于有请求体的情况（如 POST、PUT）
        if (body != null && body.length > 0) {
            log.info("请求体: {}", new String(body, StandardCharsets.UTF_8));
        } else {
            log.info("请求体: 无");
        }
        ClientHttpResponse response;
        try {
            // 执行请求
            response = execution.execute(request, body);
        } catch (IOException e) {
            log.error("请求执行异常: {}", e.getMessage(), e);
            throw e;
        }

        // 包装响应体，避免响应流只能读取一次的问题
        ClientHttpResponse wrappedResponse = new BufferingClientHttpResponseWrapper(response);
        String responseBody = StreamUtils.copyToString(wrappedResponse.getBody(), StandardCharsets.UTF_8);

        log.info("响应状态: {}", wrappedResponse.getStatusCode());
        log.info("响应头: {}", wrappedResponse.getHeaders());
        log.info("响应体: {}", responseBody);
        log.info("耗时: {} ms", System.currentTimeMillis() - startTime);
        log.info("====== 请求结束 ======");

        return wrappedResponse;
    }

    /**
     * 响应包装类，用于缓存响应体以便多次读取
     */
    private static class BufferingClientHttpResponseWrapper implements ClientHttpResponse {

        private final ClientHttpResponse response;
        private byte[] body;

        public BufferingClientHttpResponseWrapper(ClientHttpResponse response) throws IOException {
            this.response = response;
            this.body = StreamUtils.copyToByteArray(response.getBody());
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }

        @Override
        public void close() {
            response.close();
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return response.getHeaders();
        }

        @Override
        public java.io.InputStream getBody() {
            return new ByteArrayInputStream(body);
        }
    }
}

```

### 
