# WebClient

`WebClient` 是 Spring WebFlux 提供的一个用于执行 HTTP 请求的异步非阻塞客户端。它不仅支持同步操作，还可以进行异步操作，适用于高并发场景。`WebClient` 支持响应式编程，通过 `Mono` 和 `Flux` 来处理单值和多值的异步响应。



## 基础配置

### 添加依赖

在 Spring Boot 项目中使用 WebClient，你需要添加以下依赖（Spring Boot 3 默认包含了 spring-boot-starter-webflux，如果你已经在使用 WebFlux 相关的功能，应该已经包含了）。

```xml
<!-- webflux依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### 配置 WebClient

你可以在 Spring 配置类中创建一个 WebClient 的 Bean，这将使得 WebClient 可注入到应用的任何地方。WebClient 本身是线程安全的，所以可以重用这个单例。

```java
package local.ateng.java.http.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
```



## 使用WebClient

### 发起异步请求

```java
package local.ateng.java.http.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController("/web-client")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WebClientController {
    private final WebClient.Builder webClientBuilder;

    /**
     * 发起异步请求
     * 异步请求是 WebClient 的核心特性，适合高并发场景。
     * 当你发起请求时，WebClient 会返回一个 Mono（表示单个值）或者 Flux（表示多个值），可以进一步链式调用其他操作。
     *
     * @return
     */
    @GetMapping("/async")
    public Mono<String> getDataAsync() {
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();
        return webClient.get()
                .uri("/posts/1")
                .retrieve()
                .bodyToMono(String.class);
    }

}
```

### 创建测试类

```java
package local.ateng.java.http;

import io.netty.channel.ChannelOption;
import local.ateng.java.http.service.WebClientService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WebClientTests {
    private final WebClient.Builder webClientBuilder;
}
```

### 发起同步请求（阻塞）

WebClient 默认支持异步请求，但你也可以选择同步方式（通过 `.block()` 进行阻塞操作）。这种方式适合在一些简单的场景下使用。

```java
    @Test
    public void test01() {
        // 发起同步请求（阻塞）
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();
        String result = webClient.get()
                .uri("/posts/1") // 请求路径
                .retrieve() // 发起请求并获取响应
                .bodyToMono(String.class) // 响应体转换为 String
                .block();// 阻塞直到响应返回
        System.out.println(result);
    }
```

### 请求参数

```java
    @Test
    public void test02() {
        // 请求参数
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();
        String result = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/posts")
                        .queryParam("userId", 2)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(result);
    }
```

### 错误处理

```java
    @Test
    public void test03() {
        // 错误处理
        WebClient webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com")
                .build();
        String result = webClient.get()
                .uri("/posts/999")  // Non-existent resource
                .retrieve()
                .onStatus(status -> status.value() == 404, clientResponse -> {
                    // 处理 404 错误
                    return Mono.error(new RuntimeException("Resource not found"));
                })
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    // 其他错误处理
                    return Mono.just("An error occurred: " + e.getMessage());
                })
                .block();
        System.out.println(result);
    }
```

### 请求头

你可以通过 headers() 方法来设置请求头，例如设置 Content-Type 或者 Authorization 等：

```java
webClient.get()
         .uri("/posts/1")
         .header("Authorization", "Bearer token")
         .retrieve()
         .bodyToMono(String.class)
         .block();
```

### 请求超时

```java
    @Test
    public void test04() {
        WebClient webClient = webClientBuilder
                .baseUrl("https://jsonplaceholder.typicode.com")
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(5))  // 设置超时
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000))) // 设置连接超时
                .build();
        String result = webClient.get()
                .uri("/posts/1")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(result);
    }
```



## 过滤器

### 日志记录过滤器

可以记录请求和响应的详细信息

**过滤器**

```java
    // 请求日志记录
    private ExchangeFilterFunction logRequest() {
        return (request, next) -> {
            log.info("Request: " + request.method() + " " + request.url());
            request.headers().forEach((name, values) ->
                    values.forEach(value -> log.info(name + ": " + value)));
            return next.exchange(request);
        };
    }

    // 响应日志记录
    private ExchangeFilterFunction logResponse() {
        return (request, next) -> next.exchange(request)
                .doOnTerminate(() -> {
                    log.info("Response: " + request.method() + " " + request.url());
                });
    }
```

**使用过滤器**

```java
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient
                .builder()
                .filter(logRequest())  // 添加请求日志
                .filter(logResponse()) // 添加响应日志
                ;
    }
```



### 认证过滤器

为每个请求自动添加认证头

**过滤器**

```java
    // 认证头过滤器
    private ExchangeFilterFunction authHeaderFilter() {
        return (request, next) -> {
            ClientRequest modifiedRequest = ClientRequest.from(request)
                    .header("Authorization", "Bearer 1234567890")  // 添加 Authorization 头
                    .build();
            return next.exchange(modifiedRequest);
        };
    }
```

**使用过滤器**

```java
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient
                .builder()
                .filter(authHeaderFilter()) // 添加认证头
                ;
    }
```



### 重试过滤器

实现基于 HTTP 状态码（例如 5xx 错误）的自动重试机制

**过滤器**

```java
    // 重试过滤器
    public ExchangeFilterFunction retryOnError() {
        return (request, next) -> attemptRequest(request, next, new AtomicInteger(0));
    }

    private Mono<ClientResponse> attemptRequest(ClientRequest request, ExchangeFunction next, AtomicInteger retryCount) {
        return next.exchange(request)
                .flatMap(response -> shouldRetry(response)
                        .flatMap(shouldRetry -> {
                            if (shouldRetry && retryCount.incrementAndGet() <= 3) {
                                log.info("Retrying: " + request.method() + " " + request.url());
                                return Mono.delay(Duration.ofSeconds(2))
                                        .flatMap(aLong -> attemptRequest(request, next, retryCount));
                            }
                            return Mono.just(response);
                        }));
    }

    private Mono<Boolean> shouldRetry(ClientResponse response) {
        if (response.statusCode().is5xxServerError()) {
            return Mono.just(true); // 服务器错误，直接重试
        }
        if (response.statusCode().is4xxClientError()) {
            return response.bodyToMono(String.class)
                    .map(body -> {
                        // 解析 body，判断是否需要重试
                        return body.contains("ok");
                    }).defaultIfEmpty(false); // 防止 body 为空时报错
        }
        return Mono.just(false); // 其他情况不重试
    }
```

**使用过滤器**

```java
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient
                .builder()
                .filter(retryOnError())  // 添加重试逻辑
                ;
    }
```

