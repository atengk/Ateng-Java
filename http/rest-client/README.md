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

### 编辑配置

```java
package io.github.atengk.restclient.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
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
                // ✅ 可改成你项目的网关地址
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
        // 创建连接池管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        // 最大连接数
        connectionManager.setMaxTotal(100);
        // 每个主机的最大连接数
        connectionManager.setDefaultMaxPerRoute(20);

        // 构建 HttpClient
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
                        // 连接超时
                        .setConnectTimeout(Timeout.ofSeconds(10))
                        // 响应超时
                        .setResponseTimeout(Timeout.ofSeconds(30))
                        .build())
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

