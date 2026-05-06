# WebClient

WebClient 是 Spring Framework 提供的 HTTP 客户端组件，主要用于在 Spring Boot 应用中调用外部 HTTP 接口。它属于 Spring WebFlux 体系，基于 Reactor 提供函数式、链式、响应式的调用方式，适合第三方 REST 接口调用、服务间 HTTP 通信、高并发远程调用、异步编排和流式响应处理等场景。Spring Framework 官方文档说明，WebClient 是完全非阻塞的，支持流式传输，并复用 WebFlux 服务端使用的编解码能力。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html?utm_source=chatgpt.com))

## WebClient 概述

本节主要说明 WebClient 在 Spring Boot 3 项目中的定位、与 RestTemplate 的区别，以及实际开发中适合使用 WebClient 的典型场景。

### WebClient 的定位

WebClient 是 Spring WebFlux 模块提供的响应式 HTTP 客户端，用于发起 HTTP 请求并处理响应结果。它暴露的是函数式、链式 API，可以通过 `get()`、`post()`、`put()`、`delete()` 等方法构建请求，并通过 `retrieve()`、`exchangeToMono()`、`exchangeToFlux()` 等方法处理响应。Spring Framework 6.2 API 文档也明确说明，WebClient 是一个非阻塞、响应式客户端，底层可以使用 Reactor Netty 等 HTTP 客户端实现。([Home](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/web/reactive/function/client/WebClient.html?utm_source=chatgpt.com))

在 Spring Boot 3 项目中，WebClient 通常作为统一的 HTTP 调用客户端使用。例如系统需要调用认证服务、订单服务、支付平台、短信平台、对象存储服务或其他第三方开放接口时，可以基于 WebClient 封装统一的请求入口。

WebClient 的核心特点如下：

| 特性         | 说明                                                        |
| ------------ | ----------------------------------------------------------- |
| 非阻塞调用   | 请求发起后不需要一直占用当前线程等待响应                    |
| 响应式返回   | 常见返回类型为 `Mono<T>` 或 `Flux<T>`                       |
| 链式 API     | 请求方法、请求地址、请求头、请求体、响应处理可以连续编排    |
| 支持同步使用 | 可以通过 `block()` 将响应式结果转为同步结果                 |
| 支持流式响应 | 适合 SSE、文件流、长连接响应等场景                          |
| 可统一封装   | 可以统一处理 BaseUrl、Header、Token、超时、重试、日志和异常 |

### WebClient 与 RestTemplate 的区别

RestTemplate 是传统的同步阻塞式 HTTP 客户端，适合简单、直接的服务端 HTTP 调用。WebClient 是响应式 HTTP 客户端，支持非阻塞调用、异步编排和流式处理。Spring 早期 WebFlux 文档中也明确对比过：相对 RestTemplate，WebClient 是非阻塞、响应式的，能够支持更高并发，并同时支持同步和异步场景。([Home](https://docs.spring.io/spring-framework/docs/5.0.x/spring-framework-reference/web-reactive.html?utm_source=chatgpt.com))

| 对比项     | WebClient                                            | RestTemplate                                      |
| ---------- | ---------------------------------------------------- | ------------------------------------------------- |
| 所属体系   | Spring WebFlux                                       | Spring Web                                        |
| 调用模型   | 非阻塞、响应式，也可同步调用                         | 同步阻塞                                          |
| 返回类型   | `Mono<T>`、`Flux<T>`                                 | 普通 Java 对象、`ResponseEntity<T>`               |
| API 风格   | 函数式、链式 API                                     | 模板方法 API                                      |
| 并发能力   | 更适合高并发和异步调用                               | 更适合普通同步调用                                |
| 流式处理   | 支持                                                 | 支持能力较弱                                      |
| 典型方法   | `retrieve()`、`exchangeToMono()`、`exchangeToFlux()` | `getForObject()`、`postForObject()`、`exchange()` |
| 新项目建议 | 异步、响应式、流式场景优先考虑                       | 老项目维护或简单同步调用场景继续使用              |

需要注意的是，在 Spring Boot 3 新项目中，如果只是普通同步 REST 调用，也可以考虑 Spring 6 提供的 `RestClient`。如果项目已经使用 WebFlux，或者接口调用需要异步组合、流式处理、统一响应式编排，则 WebClient 更合适。

### 适用场景

WebClient 适用于以下场景：

| 场景                 | 说明                                              |
| -------------------- | ------------------------------------------------- |
| 调用第三方 REST 接口 | 例如短信、支付、地图、认证、开放平台接口          |
| 微服务 HTTP 通信     | 服务之间通过 HTTP 调用接口                        |
| 高并发远程调用       | 非阻塞模型可以减少线程等待                        |
| 多接口组合调用       | 可使用 `Mono.zip()`、`flatMap()` 组合多个远程调用 |
| 异步任务调用外部接口 | 适合定时任务、消息消费任务中的远程调用            |
| 流式响应处理         | 适合 SSE、文件下载、流式数据返回                  |
| 统一 HTTP 客户端封装 | 统一处理请求头、Token、日志、异常、超时、重试等   |

不建议盲目使用 WebClient 的场景如下：

| 场景               | 说明                                              |
| ------------------ | ------------------------------------------------- |
| 只是简单同步调用   | 可以优先考虑 `RestClient` 或保留现有 RestTemplate |
| 团队不熟悉 Reactor | `Mono`、`Flux` 使用不当会增加维护成本             |
| 大量使用 `block()` | 如果全程阻塞调用，WebClient 的非阻塞优势会下降    |
| 没有统一超时配置   | 外部接口异常时可能导致请求堆积                    |
| 日志未脱敏         | 请求头、Token、手机号、身份证号等敏感信息可能泄露 |

## 环境准备

本节给出 Spring Boot 3 项目使用 WebClient 的基础环境要求、依赖引入方式和基础配置方式。后续章节中的 GET 请求、POST 请求、统一封装、异常处理、重试和日志打印都可以基于这里的配置继续扩展。

### Spring Boot 3 版本要求

Spring Boot 3 要求 Java 17 起步。不同 Spring Boot 3.x 小版本对 Java 兼容上限、Spring Framework 最低版本和 Gradle 支持范围会略有差异。例如 Spring Boot 3.3.16 要求至少 Java 17，并要求 Spring Framework 6.1.24 或更高版本；Spring Boot 3.5 快照文档也说明需要 Java 17 或更高版本，并要求 Maven 3.6.3 或更高版本。([Spring Enterprise 文档](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

推荐环境如下：

| 环境             | 推荐版本                    | 说明                                            |
| ---------------- | --------------------------- | ----------------------------------------------- |
| JDK              | 17+                         | Spring Boot 3 最低要求 Java 17                  |
| Spring Boot      | 3.2+ / 3.3+ / 3.4+ / 3.5+   | 建议使用仍在维护的 Spring Boot 3.x 版本         |
| Maven            | 3.6.3+                      | Spring Boot 3.x 常用 Maven 最低要求             |
| Gradle           | 按具体 Spring Boot 版本选择 | 不同 3.x 小版本支持范围略有差异                 |
| Spring Framework | 6.x                         | Spring Boot 3 基于 Spring Framework 6           |
| Jakarta EE       | 9+ / 10+ 相关包名           | Spring Boot 3 已从 `javax.*` 迁移到 `jakarta.*` |

可以通过以下命令检查本地 Java 和 Maven 版本：

```bash
# 查看 Java 版本，Spring Boot 3 至少需要 Java 17
java -version

# 查看 Maven 版本，建议使用 Maven 3.6.3 或更高版本
mvn -version
```

如果 Java 主版本低于 17，需要先升级 JDK，否则 Spring Boot 3 项目无法正常编译或启动。

### WebFlux 依赖引入

WebClient 来自 Spring WebFlux。Spring Boot 项目中通常通过 `spring-boot-starter-webflux` 引入 WebClient、Reactor 以及默认的响应式 HTTP 客户端实现。Spring Framework 文档说明，WebClient 底层需要 HTTP 客户端库执行请求，内置支持 Reactor Netty、JDK HttpClient、Jetty Reactive HttpClient 和 Apache HttpComponents 等实现。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html?utm_source=chatgpt.com))

以下依赖放在项目的 `pom.xml` 中。

```xml
<dependencies>
    <!-- Spring WebFlux：引入 WebClient、Reactor、响应式 Web 支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Hutool：常用工具类，后续可用于字符串、集合、JSON、编码等辅助处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.40</version>
    </dependency>

    <!-- Lombok：简化配置类、DTO、日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：用于单元测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Reactor Test：用于测试 Mono、Flux 等响应式返回值 -->
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目本身是 Spring MVC 项目，也可以单独引入 `spring-boot-starter-webflux` 来使用 WebClient。引入该依赖不代表必须把整个项目改造成响应式架构，WebClient 可以仅作为 HTTP 客户端使用。

### 基础配置说明

基础配置建议分为两部分：第一部分放在 `application.yml` 中，用于维护外部接口地址、超时时间和默认请求头；第二部分通过配置类创建全局 `WebClient` Bean，供业务代码统一注入使用。

推荐目录结构如下：

```text
src
└── main
    ├── java
    │   └── io
    │       └── github
    │           └── atengk
    │               └── webclient
    │                   ├── config
    │                   │   ├── WebClientProperties.java
    │                   │   └── WebClientConfig.java
    │                   └── WebClientApplication.java
    └── resources
        └── application.yml
```

以下配置用于维护 WebClient 的基础参数。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    # 应用名称，便于日志、监控和链路追踪识别
    name: springboot3-webclient-demo

webclient:
  # 外部接口基础地址，实际项目建议按 dev、test、prod 环境拆分
  base-url: https://api.example.com

  # 连接超时时间，单位毫秒
  connect-timeout: 3000

  # 响应超时时间，单位毫秒
  response-timeout: 5000

  # 默认 User-Agent
  user-agent: springboot3-webclient-demo

  # 是否开启基础请求日志，生产环境需要注意敏感信息脱敏
  enable-log: true
```

以下配置属性类用于读取 `application.yml` 中的 `webclient` 配置。

文件位置：`src/main/java/io/github/atengk/webclient/config/WebClientProperties.java`

```java
package io.github.atengk.webclient.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebClient 基础配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@ConfigurationProperties(prefix = "webclient")
public class WebClientProperties {

    /**
     * 外部接口基础地址
     */
    private String baseUrl;

    /**
     * 连接超时时间，单位毫秒
     */
    private Integer connectTimeout = 3000;

    /**
     * 响应超时时间，单位毫秒
     */
    private Integer responseTimeout = 5000;

    /**
     * 默认 User-Agent
     */
    private String userAgent = "springboot3-webclient-demo";

    /**
     * 是否开启请求日志
     */
    private Boolean enableLog = true;
}
```

以下配置类用于创建全局 WebClient Bean，并设置 BaseUrl、默认请求头、连接超时、响应超时和基础请求日志。

文件位置：`src/main/java/io/github/atengk/webclient/config/WebClientConfig.java`

```java
package io.github.atengk.webclient.config;

import cn.hutool.core.util.StrUtil;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient 基础配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(WebClientProperties.class)
public class WebClientConfig {

    private final WebClientProperties webClientProperties;

    /**
     * 创建全局 WebClient 实例
     *
     * @param builder Spring Boot 自动配置的 WebClient.Builder
     * @return WebClient 实例
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webClientProperties.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(webClientProperties.getResponseTimeout()));

        WebClient.Builder webClientBuilder = builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, webClientProperties.getUserAgent());

        if (StrUtil.isNotBlank(webClientProperties.getBaseUrl())) {
            webClientBuilder.baseUrl(webClientProperties.getBaseUrl());
        }

        if (Boolean.TRUE.equals(webClientProperties.getEnableLog())) {
            webClientBuilder.filter(logRequest());
        }

        log.info("WebClient初始化完成，baseUrl：{}，connectTimeout：{}ms，responseTimeout：{}ms",
                webClientProperties.getBaseUrl(),
                webClientProperties.getConnectTimeout(),
                webClientProperties.getResponseTimeout());

        return webClientBuilder.build();
    }

    /**
     * 打印基础请求日志
     *
     * @return 请求过滤器
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("WebClient发起请求，method：{}，url：{}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }
}
```

完成基础配置后，可以通过以下命令检查项目是否能正常编译和启动。

```bash
# 编译项目，检查依赖和 Java 版本是否正常
mvn clean compile

# 启动项目，观察 WebClient 初始化日志
mvn spring-boot:run
```

启动成功后，如果控制台输出类似日志，说明全局 WebClient Bean 已经创建成功：

```text
WebClient初始化完成，baseUrl：https://api.example.com，connectTimeout：3000ms，responseTimeout：5000ms
```

基础配置阶段只需要完成以下目标：依赖可用、配置可读取、WebClient 可注入、超时时间已设置、基础日志可观察。连接池参数、统一异常解析、响应状态码处理、请求重试、Token 注入和日志脱敏可以放到后续章节继续展开。



## WebClient 基础使用

本节介绍 WebClient 的基础调用方式，包括实例创建、GET 请求、POST 请求、请求参数、请求头以及响应结果处理。WebClient 提供函数式、链式 API，常见调用流程是创建客户端、构建请求、发起请求、解析响应。Spring 官方文档说明，WebClient 支持 `retrieve()`、`exchangeToMono()`、`exchangeToFlux()` 等响应处理方式，也支持通过 `bodyValue(Object)` 或 `body(Publisher, Class)` 发送请求体。([Home](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/web/reactive/function/client/WebClient.html?utm_source=chatgpt.com))

### 创建 WebClient 实例

WebClient 可以通过静态工厂方法或 Builder 创建。Spring Framework 官方文档说明，最简单的创建方式是 `WebClient.create()`、`WebClient.create(String baseUrl)`，也可以通过 `WebClient.builder()` 设置默认 Header、Cookie、过滤器、编解码器、客户端连接器等配置。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-builder.html?utm_source=chatgpt.com))

常见创建方式如下：

```java
// 创建默认 WebClient 实例
WebClient webClient = WebClient.create();

// 创建带 BaseUrl 的 WebClient 实例
WebClient webClient = WebClient.create("https://api.example.com");

// 使用 Builder 创建 WebClient 实例
WebClient webClient = WebClient.builder()
        .baseUrl("https://api.example.com")
        .defaultHeader(HttpHeaders.USER_AGENT, "springboot3-webclient-demo")
        .build();
```

在 Spring Boot 3 项目中，更推荐将 WebClient 配置成 Bean，然后在业务类中通过构造方法注入使用。这样可以统一管理 BaseUrl、默认 Header、超时时间、日志、异常处理和编解码配置。

文件位置：`src/main/java/io/github/atengk/webclient/service/RemoteUserService.java`

以下服务类演示如何注入全局 WebClient，并封装用户相关的远程接口调用。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.vo.RemoteUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 远程用户接口调用服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteUserService {

    private final WebClient webClient;

    /**
     * 根据用户编号查询用户信息
     *
     * @param userId 用户编号
     * @return 用户信息
     */
    public RemoteUserVO getUserById(Long userId) {
        log.info("开始调用远程用户详情接口，userId：{}", userId);

        RemoteUserVO userVO = webClient.get()
                .uri("/users/{userId}", userId)
                .retrieve()
                .bodyToMono(RemoteUserVO.class)
                .block();

        if (userVO == null || StrUtil.isBlank(userVO.getUsername())) {
            log.warn("远程用户详情接口返回为空，userId：{}", userId);
            return null;
        }

        log.info("远程用户详情接口调用完成，userId：{}，username：{}", userId, userVO.getUsername());
        return userVO;
    }
}
```

上面示例中的 `block()` 会将响应式结果转成同步结果，适合传统 Spring MVC Controller、定时任务、普通 Service 等同步业务场景。如果当前代码运行在 WebFlux 的事件循环线程中，应避免随意使用 `block()`，否则会破坏非阻塞调用模型。

### GET 请求调用

GET 请求主要用于查询数据。WebClient 发起 GET 请求时，通常使用 `get()` 指定请求方法，使用 `uri()` 指定请求地址，使用 `retrieve()` 获取响应结果，再通过 `bodyToMono()` 将响应体转换为指定类型。

文件位置：`src/main/java/io/github/atengk/webclient/service/RemoteOrderService.java`

以下服务类演示 GET 请求的两种常见写法：路径变量查询和查询参数查询。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.webclient.vo.RemoteOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

/**
 * 远程订单接口调用服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteOrderService {

    private final WebClient webClient;

    /**
     * 根据订单编号查询订单详情
     *
     * @param orderId 订单编号
     * @return 订单详情
     */
    public RemoteOrderVO getOrderById(Long orderId) {
        log.info("开始调用远程订单详情接口，orderId：{}", orderId);

        RemoteOrderVO orderVO = webClient.get()
                .uri("/orders/{orderId}", orderId)
                .retrieve()
                .bodyToMono(RemoteOrderVO.class)
                .block();

        log.info("远程订单详情接口调用完成，orderId：{}", orderId);
        return orderVO;
    }

    /**
     * 根据用户编号查询订单列表
     *
     * @param userId 用户编号
     * @param status 订单状态
     * @return 订单列表
     */
    public List<RemoteOrderVO> listOrders(Long userId, String status) {
        log.info("开始调用远程订单列表接口，userId：{}，status：{}", userId, status);

        List<RemoteOrderVO> orderList = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders")
                        .queryParam("userId", userId)
                        .queryParam("status", status)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RemoteOrderVO>>() {
                })
                .block();

        if (CollUtil.isEmpty(orderList)) {
            log.info("远程订单列表接口返回为空，userId：{}，status：{}", userId, status);
            return Collections.emptyList();
        }

        log.info("远程订单列表接口调用完成，userId：{}，订单数量：{}", userId, orderList.size());
        return orderList;
    }
}
```

GET 请求中常见的 `uri()` 写法如下：

```java
// 固定路径
.uri("/users")

// 路径变量
.uri("/users/{userId}", userId)

// 查询参数
.uri(uriBuilder -> uriBuilder
        .path("/users")
        .queryParam("keyword", "admin")
        .queryParam("pageNum", 1)
        .queryParam("pageSize", 10)
        .build())
```

如果请求参数较多，建议使用 `uriBuilder`，不要手动拼接 URL 字符串。这样可以减少特殊字符、空格、中文参数带来的编码问题。

### POST 请求调用

POST 请求主要用于创建资源、提交表单、发送 JSON 请求体等场景。WebClient 发送 JSON 请求时，通常使用 `post()` 指定请求方法，通过 `contentType(MediaType.APPLICATION_JSON)` 设置请求体类型，并通过 `bodyValue()` 传入请求对象。

文件位置：`src/main/java/io/github/atengk/webclient/dto/CreateUserRequest.java`

以下 DTO 用于封装新增用户请求参数。

```java
package io.github.atengk.webclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增用户请求参数
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;
}
```

文件位置：`src/main/java/io/github/atengk/webclient/vo/RemoteUserVO.java`

以下 VO 用于接收远程用户接口返回结果。

```java
package io.github.atengk.webclient.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 远程用户响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class RemoteUserVO {

    /**
     * 用户编号
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/webclient/service/RemoteUserCreateService.java`

以下服务类演示如何使用 WebClient 发起 POST JSON 请求。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.dto.CreateUserRequest;
import io.github.atengk.webclient.vo.RemoteUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 远程用户创建接口调用服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteUserCreateService {

    private final WebClient webClient;

    /**
     * 创建远程用户
     *
     * @param request 新增用户请求参数
     * @return 用户信息
     */
    public RemoteUserVO createUser(CreateUserRequest request) {
        if (request == null || StrUtil.isBlank(request.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        log.info("开始调用远程用户创建接口，username：{}", request.getUsername());

        RemoteUserVO userVO = webClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RemoteUserVO.class)
                .block();

        log.info("远程用户创建接口调用完成，username：{}", request.getUsername());
        return userVO;
    }
}
```

POST 请求中常见请求体写法如下：

```java
// 发送 JSON 对象
.bodyValue(request)

// 发送响应式请求体
.body(Mono.just(request), CreateUserRequest.class)

// 发送表单数据时，可使用 MultiValueMap 配合 MediaType.APPLICATION_FORM_URLENCODED
.contentType(MediaType.APPLICATION_FORM_URLENCODED)
.bodyValue(formData)
```

如果是普通业务对象，优先使用 `bodyValue(request)`，代码最直接；如果请求体本身来自异步数据源，可以使用 `body(Publisher, Class)`。

### 请求参数与请求头设置

实际开发中，请求通常需要携带查询参数、路径变量、认证 Token、租户编号、请求来源、链路追踪 ID 等信息。WebClient 支持在单次请求中设置 Header，也支持在全局 Builder 中设置默认 Header。

文件位置：`src/main/java/io/github/atengk/webclient/service/RemoteAuthUserService.java`

以下服务类演示如何同时设置路径变量、查询参数和动态请求头。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.vo.RemoteUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 携带认证信息的远程用户接口调用服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteAuthUserService {

    private final WebClient webClient;

    /**
     * 携带 Token 查询用户信息
     *
     * @param userId 用户编号
     * @param token 访问令牌
     * @param tenantId 租户编号
     * @return 用户信息
     */
    public RemoteUserVO getUserWithToken(Long userId, String token, String tenantId) {
        if (StrUtil.isBlank(token)) {
            throw new IllegalArgumentException("访问令牌不能为空");
        }

        log.info("开始调用远程用户接口，userId：{}，tenantId：{}", userId, tenantId);

        RemoteUserVO userVO = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tenants/{tenantId}/users/{userId}")
                        .queryParam("includeRole", true)
                        .build(tenantId, userId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Tenant-Id", tenantId)
                .header("X-Client-Source", "springboot3-webclient-demo")
                .retrieve()
                .bodyToMono(RemoteUserVO.class)
                .block();

        log.info("远程用户接口调用完成，userId：{}，tenantId：{}", userId, tenantId);
        return userVO;
    }
}
```

常用请求参数与请求头设置方式如下：

| 类型         | 写法                                       | 说明                    |
| ------------ | ------------------------------------------ | ----------------------- |
| 路径变量     | `.uri("/users/{id}", id)`                  | 适合 RESTful 资源路径   |
| 查询参数     | `.queryParam("keyword", keyword)`          | 适合 GET 查询条件       |
| 单个 Header  | `.header("X-Token", token)`                | 适合动态 Header         |
| 批量 Header  | `.headers(headers -> {...})`               | 适合一次设置多个 Header |
| Content-Type | `.contentType(MediaType.APPLICATION_JSON)` | 声明请求体格式          |
| Accept       | `.accept(MediaType.APPLICATION_JSON)`      | 声明期望响应格式        |

批量设置 Header 示例：

```java
webClient.get()
        .uri("/users")
        .headers(headers -> {
            headers.setBearerAuth(token);
            headers.add("X-Tenant-Id", tenantId);
            headers.add("X-Trace-Id", traceId);
        })
        .retrieve()
        .bodyToMono(String.class);
```

对于 Token、租户编号、请求来源等通用 Header，建议在后续配置封装中通过 `defaultHeader()`、`defaultRequest()` 或 `ExchangeFilterFunction` 统一处理，避免业务代码中重复设置。

### 响应结果处理

WebClient 常用的响应处理方式有 `bodyToMono()`、`bodyToFlux()`、`toEntity()`、`onStatus()`、`exchangeToMono()` 等。Spring 官方文档将 `retrieve()` 作为常用响应获取方式，同时也提供 `exchangeToMono()` 和 `exchangeToFlux()` 用于更细粒度地访问响应状态、响应头和响应体。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html?utm_source=chatgpt.com))

常见响应处理方式如下：

```java
// 只关心响应体
Mono<RemoteUserVO> result = webClient.get()
        .uri("/users/{userId}", userId)
        .retrieve()
        .bodyToMono(RemoteUserVO.class);

// 关心响应状态码、响应头、响应体
Mono<ResponseEntity<RemoteUserVO>> entity = webClient.get()
        .uri("/users/{userId}", userId)
        .retrieve()
        .toEntity(RemoteUserVO.class);

// 处理列表响应
Mono<List<RemoteUserVO>> list = webClient.get()
        .uri("/users")
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<RemoteUserVO>>() {
        });
```

对于需要根据状态码进行异常处理的场景，可以使用 `onStatus()`。

文件位置：`src/main/java/io/github/atengk/webclient/exception/RemoteCallException.java`

以下异常类用于封装远程接口调用异常。

```java
package io.github.atengk.webclient.exception;

import lombok.Getter;

/**
 * 远程接口调用异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
public class RemoteCallException extends RuntimeException {

    /**
     * HTTP 状态码
     */
    private final Integer statusCode;

    /**
     * 创建远程接口调用异常
     *
     * @param statusCode HTTP 状态码
     * @param message 异常信息
     */
    public RemoteCallException(Integer statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
```

文件位置：`src/main/java/io/github/atengk/webclient/service/RemoteResponseService.java`

以下服务类演示如何解析异常响应，并在非 2xx 状态码时抛出业务异常。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.exception.RemoteCallException;
import io.github.atengk.webclient.vo.RemoteUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 远程响应处理示例服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteResponseService {

    private final WebClient webClient;

    /**
     * 查询用户并处理异常响应
     *
     * @param userId 用户编号
     * @return 用户信息
     */
    public RemoteUserVO getUserAndHandleError(Long userId) {
        log.info("开始调用远程用户接口并处理响应状态，userId：{}", userId);

        return webClient.get()
                .uri("/users/{userId}", userId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(errorBody -> {
                                    String message = StrUtil.blankToDefault(errorBody, "远程接口调用失败");
                                    log.warn("远程用户接口返回异常，userId：{}，status：{}，body：{}",
                                            userId, response.statusCode().value(), message);
                                    return new RemoteCallException(response.statusCode().value(), message);
                                })
                )
                .bodyToMono(RemoteUserVO.class)
                .block();
    }
}
```

如果需要同时判断状态码、读取响应头，并根据不同状态码返回不同结果，可以使用 `exchangeToMono()`：

```java
Mono<RemoteUserVO> result = webClient.get()
        .uri("/users/{userId}", userId)
        .exchangeToMono(response -> {
            if (response.statusCode().is2xxSuccessful()) {
                return response.bodyToMono(RemoteUserVO.class);
            }
            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(errorBody -> Mono.error(
                            new RemoteCallException(response.statusCode().value(), errorBody)
                    ));
        });
```

一般情况下，业务代码只需要响应体时使用 `retrieve()`；需要精细处理状态码、响应头、异常体时使用 `exchangeToMono()`。

## WebClient 配置封装

本节介绍如何在 Spring Boot 3 项目中封装全局 WebClient 配置。实际项目不建议在每个业务类中重复创建 WebClient，而是通过配置类统一创建 Bean，并集中处理 BaseUrl、默认 Header、超时时间、编码解码、日志过滤器和连接器配置。WebClient 创建后是不可变对象，如果需要基于已有配置创建变体，可以使用 `mutate()` 复制并扩展配置。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-builder.html?utm_source=chatgpt.com))

### 全局 WebClient Bean 配置

全局 Bean 配置的目标是让业务代码只关注接口路径、请求参数和响应类型，不关心底层连接、超时、公共 Header、日志等通用能力。

推荐目录结构如下：

```text
src
└── main
    ├── java
    │   └── io
    │       └── github
    │           └── atengk
    │               └── webclient
    │                   ├── config
    │                   │   ├── WebClientProperties.java
    │                   │   └── WebClientConfig.java
    │                   └── service
    │                       └── RemoteUserService.java
    └── resources
        └── application.yml
```

文件位置：`src/main/resources/application.yml`

以下配置统一维护 WebClient 的基础地址、超时时间、内存缓冲区大小和默认请求头。

```yaml
webclient:
  # 外部接口基础地址
  base-url: https://api.example.com

  # 连接超时时间，单位毫秒
  connect-timeout: 3000

  # 响应超时时间，单位毫秒
  response-timeout: 5000

  # 最大内存缓冲区大小，单位字节；示例为 2MB
  max-in-memory-size: 2097152

  # 默认 User-Agent
  user-agent: springboot3-webclient-demo

  # 默认客户端来源标识
  client-source: springboot3-webclient-demo

  # 是否打印基础请求日志
  enable-log: true
```

文件位置：`src/main/java/io/github/atengk/webclient/config/WebClientProperties.java`

以下配置属性类用于读取 `webclient` 配置项。

```java
package io.github.atengk.webclient.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebClient 配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@ConfigurationProperties(prefix = "webclient")
public class WebClientProperties {

    /**
     * 外部接口基础地址
     */
    private String baseUrl;

    /**
     * 连接超时时间，单位毫秒
     */
    private Integer connectTimeout = 3000;

    /**
     * 响应超时时间，单位毫秒
     */
    private Integer responseTimeout = 5000;

    /**
     * 最大内存缓冲区大小，单位字节
     */
    private Integer maxInMemorySize = 2 * 1024 * 1024;

    /**
     * 默认 User-Agent
     */
    private String userAgent = "springboot3-webclient-demo";

    /**
     * 默认客户端来源标识
     */
    private String clientSource = "springboot3-webclient-demo";

    /**
     * 是否开启请求日志
     */
    private Boolean enableLog = true;
}
```

文件位置：`src/main/java/io/github/atengk/webclient/config/WebClientConfig.java`

以下配置类创建全局 WebClient Bean，并集中配置 BaseUrl、Header、超时、编码解码和日志过滤器。

```java
package io.github.atengk.webclient.config;

import cn.hutool.core.util.StrUtil;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient 全局配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(WebClientProperties.class)
public class WebClientConfig {

    private final WebClientProperties properties;

    /**
     * 创建全局 WebClient
     *
     * @param builder Spring Boot 自动配置的 WebClient.Builder
     * @return WebClient
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(properties.getResponseTimeout()));

        WebClient.Builder webClientBuilder = builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .defaultHeader("X-Client-Source", properties.getClientSource())
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(properties.getMaxInMemorySize()));

        if (StrUtil.isNotBlank(properties.getBaseUrl())) {
            webClientBuilder.baseUrl(properties.getBaseUrl());
        }

        if (Boolean.TRUE.equals(properties.getEnableLog())) {
            webClientBuilder.filter(requestLogFilter());
        }

        log.info("WebClient全局配置初始化完成，baseUrl：{}，connectTimeout：{}ms，responseTimeout：{}ms，maxInMemorySize：{}",
                properties.getBaseUrl(),
                properties.getConnectTimeout(),
                properties.getResponseTimeout(),
                properties.getMaxInMemorySize());

        return webClientBuilder.build();
    }

    /**
     * 请求日志过滤器
     *
     * @return ExchangeFilterFunction
     */
    private ExchangeFilterFunction requestLogFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info("WebClient请求开始，method：{}，url：{}", request.method(), request.url());
            return Mono.just(request);
        });
    }
}
```

该配置完成后，业务类可以直接注入 `WebClient` 使用，不需要重复创建客户端实例。

### BaseUrl 配置

BaseUrl 用于设置请求的基础地址。配置 BaseUrl 后，业务代码中的 `uri()` 可以只写相对路径。例如全局 BaseUrl 为 `https://api.example.com`，业务代码中请求 `/users/1`，最终请求地址就是 `https://api.example.com/users/1`。

```java
WebClient webClient = WebClient.builder()
        .baseUrl("https://api.example.com")
        .build();
```

在项目中建议将 BaseUrl 放入配置文件，而不是写死在代码中。

```yaml
webclient:
  # 开发环境接口地址
  base-url: https://api-dev.example.com
```

多环境配置可以拆成以下形式：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

生产环境配置示例：

```yaml
webclient:
  # 生产环境接口地址
  base-url: https://api.example.com
```

业务调用示例：

```java
RemoteUserVO userVO = webClient.get()
        .uri("/users/{userId}", userId)
        .retrieve()
        .bodyToMono(RemoteUserVO.class)
        .block();
```

如果一个系统需要调用多个不同的外部服务，建议定义多个命名 Bean，例如 `userWebClient`、`orderWebClient`、`paymentWebClient`，避免一个 BaseUrl 混用多个业务域。

### 默认请求头配置

默认请求头适合放置所有请求都需要携带的信息，例如 `User-Agent`、客户端来源、系统标识、固定版本号等。对于动态 Token、租户编号、链路追踪 ID 等运行时变化的信息，可以在单次请求中设置，或者通过过滤器动态追加。

固定默认 Header 示例：

```java
WebClient webClient = WebClient.builder()
        .defaultHeader(HttpHeaders.USER_AGENT, "springboot3-webclient-demo")
        .defaultHeader("X-Client-Source", "springboot3-webclient-demo")
        .build();
```

动态 Header 示例：

```java
webClient.get()
        .uri("/users/{userId}", userId)
        .headers(headers -> {
            headers.setBearerAuth(token);
            headers.add("X-Tenant-Id", tenantId);
            headers.add("X-Trace-Id", traceId);
        })
        .retrieve()
        .bodyToMono(RemoteUserVO.class);
```

如果希望所有请求统一追加 Header，可以使用 `defaultRequest()`：

```java
WebClient webClient = WebClient.builder()
        .defaultRequest(requestHeadersSpec -> requestHeadersSpec
                .header("X-Client-Source", "springboot3-webclient-demo"))
        .build();
```

也可以使用过滤器统一追加 Header。过滤器更适合处理动态逻辑，例如从上下文中读取 Token、追踪 ID 或租户编号。

```java
private ExchangeFilterFunction defaultHeaderFilter() {
    return ExchangeFilterFunction.ofRequestProcessor(request -> {
        ClientRequest newRequest = ClientRequest.from(request)
                .header("X-Client-Source", "springboot3-webclient-demo")
                .build();
        return Mono.just(newRequest);
    });
}
```

如果使用上面代码，需要额外引入：

```java
import org.springframework.web.reactive.function.client.ClientRequest;
```

默认请求头中不要放置长期不变但敏感的信息，例如固定写死的 AccessToken。Token 建议通过配置中心、认证服务或请求上下文动态获取。

### 超时时间配置

WebClient 自身负责构建请求和处理响应，底层实际网络通信由 HTTP 客户端完成。使用默认 Reactor Netty 时，可以通过 `HttpClient` 设置连接超时和响应超时。Spring 官方文档中也给出了通过 `ChannelOption.CONNECT_TIMEOUT_MILLIS` 设置连接超时，以及通过 Netty Handler 设置读写超时的方式。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-builder.html?utm_source=chatgpt.com))

常用超时类型如下：

| 超时类型 | 说明                             |
| -------- | -------------------------------- |
| 连接超时 | 建立 TCP 连接的最大等待时间      |
| 响应超时 | 请求发出后等待响应的最大时间     |
| 读超时   | 连接建立后读取数据的最大等待时间 |
| 写超时   | 向连接写入数据的最大等待时间     |

基础超时配置示例：

```java
HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
        .responseTimeout(Duration.ofMillis(5000));

WebClient webClient = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
```

如果需要配置读写超时，可以使用 Netty 的 `ReadTimeoutHandler` 和 `WriteTimeoutHandler`：

```java
HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
        .responseTimeout(Duration.ofMillis(5000))
        .doOnConnected(connection -> connection
                .addHandlerLast(new ReadTimeoutHandler(5))
                .addHandlerLast(new WriteTimeoutHandler(5)));
```

完整导入如下：

```java
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
```

超时时间不建议设置过长。对于常规第三方接口，连接超时可以设置为 2 到 5 秒，响应超时可以根据接口复杂度设置为 5 到 30 秒。对于文件下载、报表导出等耗时接口，需要单独配置专用 WebClient，避免影响普通接口调用。

### 编码与解码配置

WebClient 使用 Codec 对请求体和响应体进行编码、解码。Spring 官方文档说明，WebClient 依赖与 WebFlux 服务端相同的编解码器处理请求和响应内容；默认编解码器存在内存缓冲限制，默认值为 256KB，如果响应体超过限制，可能出现 `DataBufferLimitException: Exceeded limit on max bytes to buffer`。可以通过 `defaultCodecs().maxInMemorySize(...)` 调整限制。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html?utm_source=chatgpt.com))

配置最大内存缓冲区示例：

```java
WebClient webClient = WebClient.builder()
        .codecs(configurer -> configurer.defaultCodecs()
                .maxInMemorySize(2 * 1024 * 1024))
        .build();
```

在全局配置类中，建议将该值放入配置文件：

```yaml
webclient:
  # 最大内存缓冲区大小，单位字节；2MB = 2 * 1024 * 1024
  max-in-memory-size: 2097152
```

配置类中使用：

```java
WebClient.Builder webClientBuilder = builder
        .codecs(configurer -> configurer.defaultCodecs()
                .maxInMemorySize(properties.getMaxInMemorySize()));
```

需要注意，`maxInMemorySize` 不是越大越好。它控制的是单次响应内容在内存中聚合时允许占用的最大缓冲区。如果接口返回大文件，不建议简单调大该值，而应使用流式下载方式处理，例如使用 `bodyToFlux(DataBuffer.class)`。

普通 JSON 响应可以直接解码为 Java 对象：

```java
Mono<RemoteUserVO> result = webClient.get()
        .uri("/users/{userId}", userId)
        .retrieve()
        .bodyToMono(RemoteUserVO.class);
```

泛型列表响应需要使用 `ParameterizedTypeReference`：

```java
Mono<List<RemoteUserVO>> result = webClient.get()
        .uri("/users")
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<RemoteUserVO>>() {
        });
```

如果响应结果结构是统一包装对象，例如：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "username": "admin"
  }
}
```

可以定义通用响应类。

文件位置：`src/main/java/io/github/atengk/webclient/vo/ApiResult.java`

```java
package io.github.atengk.webclient.vo;

import lombok.Data;

/**
 * 远程接口统一响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class ApiResult<T> {

    /**
     * 响应编码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;
}
```

解析泛型包装响应时，可以这样写：

```java
Mono<ApiResult<RemoteUserVO>> result = webClient.get()
        .uri("/users/{userId}", userId)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<ApiResult<RemoteUserVO>>() {
        });
```

编码与解码配置建议遵循以下原则：

| 建议             | 说明                                             |
| ---------------- | ------------------------------------------------ |
| 普通 JSON 响应   | 使用 `bodyToMono(对象类型.class)`                |
| JSON 数组响应    | 使用 `ParameterizedTypeReference<List<T>>`       |
| 统一包装响应     | 使用 `ParameterizedTypeReference<ApiResult<T>>`  |
| 大响应体         | 不要盲目调大 `maxInMemorySize`，优先考虑流式处理 |
| 文件下载         | 使用 `bodyToFlux(DataBuffer.class)`              |
| 多服务差异化配置 | 为不同外部服务创建不同 WebClient Bean            |

至此，基础使用和配置封装已经覆盖了 WebClient 的主要开发入口。后续章节可以继续展开请求与响应处理，例如 JSON 请求体、表单请求、文件上传、响应状态码处理和异常响应解析。



## 请求与响应处理

本节介绍 WebClient 在实际接口调用中最常见的请求体和响应处理方式，包括 JSON 请求体、表单请求、文件上传、状态码判断和异常响应解析。WebClient 的请求体可以通过 `bodyValue()` 直接传入普通对象，也可以通过 `body(Publisher, Class)` 传入响应式数据源；响应处理则通常通过 `retrieve()`、`onStatus()`、`bodyToMono()`、`bodyToFlux()` 完成。Spring 官方文档也说明，WebClient 支持 JSON、表单、multipart、流式请求体以及基于状态码的响应处理。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-body.html?utm_source=chatgpt.com))

### JSON 请求体处理

JSON 请求体是 WebClient 最常见的使用方式，适合调用新增、修改、提交、查询等以 JSON 作为请求体的接口。实际开发中通常使用 DTO 封装请求参数，然后通过 `bodyValue(request)` 发送请求对象，WebClient 会基于默认 Codec 将对象编码为 JSON。

文件位置：`src/main/java/io/github/atengk/webclient/dto/CreateMessageRequest.java`

以下 DTO 用于封装发送消息接口的 JSON 请求体。

```java
package io.github.atengk.webclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建消息请求参数
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMessageRequest {

    /**
     * 接收人编号
     */
    private Long receiverId;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;
}
```

文件位置：`src/main/java/io/github/atengk/webclient/vo/RemoteMessageVO.java`

以下 VO 用于接收远程消息接口返回结果。

```java
package io.github.atengk.webclient.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 远程消息响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class RemoteMessageVO {

    /**
     * 消息编号
     */
    private Long messageId;

    /**
     * 接收人编号
     */
    private Long receiverId;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/webclient/service/RemoteMessageService.java`

以下服务类演示如何使用 WebClient 发送 JSON 请求体，并将 JSON 响应解析为 Java 对象。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.dto.CreateMessageRequest;
import io.github.atengk.webclient.vo.RemoteMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 远程消息接口调用服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteMessageService {

    private final WebClient webClient;

    /**
     * 创建远程消息
     *
     * @param request 创建消息请求参数
     * @return 远程消息响应结果
     */
    public RemoteMessageVO createMessage(CreateMessageRequest request) {
        if (request == null || request.getReceiverId() == null || StrUtil.isBlank(request.getTitle())) {
            throw new IllegalArgumentException("接收人编号和消息标题不能为空");
        }

        log.info("开始调用远程消息创建接口，receiverId：{}，title：{}", request.getReceiverId(), request.getTitle());

        RemoteMessageVO messageVO = webClient.post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RemoteMessageVO.class)
                .block();

        log.info("远程消息创建接口调用完成，receiverId：{}，messageId：{}",
                request.getReceiverId(), messageVO == null ? null : messageVO.getMessageId());
        return messageVO;
    }
}
```

如果请求体本身来自响应式流程，可以使用 `body(Mono<T>, Class<T>)`。这种方式适合上游数据也是异步结果的场景。

```java
Mono<CreateMessageRequest> requestMono = Mono.just(request);

Mono<RemoteMessageVO> result = webClient.post()
        .uri("/messages")
        .contentType(MediaType.APPLICATION_JSON)
        .body(requestMono, CreateMessageRequest.class)
        .retrieve()
        .bodyToMono(RemoteMessageVO.class);
```

普通 Spring MVC 或同步 Service 中，常见做法是最后调用 `block()` 获取结果；如果是在 WebFlux 非阻塞链路中，则应直接返回 `Mono<RemoteMessageVO>`，不要随意阻塞。

### 表单请求处理

表单请求通常用于调用 `application/x-www-form-urlencoded` 类型的接口，例如 OAuth2 获取 Token、旧系统登录接口、第三方平台签名提交接口等。WebClient 可以通过 `MultiValueMap<String, String>` 传递表单字段，Spring 的表单编码器会将其编码为标准表单请求体。Spring 官方文档说明，发送表单数据时可以提供 `MultiValueMap<String, String>` 作为请求体，内容类型会由表单写入器处理为 `application/x-www-form-urlencoded`。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-body.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/webclient/vo/RemoteTokenVO.java`

以下 VO 用于接收远程认证接口返回的 Token 信息。

```java
package io.github.atengk.webclient.vo;

import lombok.Data;

/**
 * 远程 Token 响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class RemoteTokenVO {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 过期时间，单位秒
     */
    private Long expiresIn;
}
```

文件位置：`src/main/java/io/github/atengk/webclient/service/RemoteFormService.java`

以下服务类演示如何提交表单请求并解析响应结果。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.vo.RemoteTokenVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 远程表单接口调用服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteFormService {

    private final WebClient webClient;

    /**
     * 使用表单方式获取访问令牌
     *
     * @param username 用户名
     * @param password 密码
     * @return Token 响应结果
     */
    public RemoteTokenVO getAccessToken(String username, String password) {
        if (StrUtil.hasBlank(username, password)) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("username", username);
        formData.add("password", password);

        log.info("开始调用远程Token接口，username：{}", username);

        RemoteTokenVO tokenVO = webClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(RemoteTokenVO.class)
                .block();

        log.info("远程Token接口调用完成，username：{}，tokenType：{}",
                username, tokenVO == null ? null : tokenVO.getTokenType());
        return tokenVO;
    }
}
```

表单请求和 JSON 请求的主要区别在于 `contentType` 和请求体结构。JSON 请求一般传 DTO 对象；表单请求一般传 `MultiValueMap<String, String>`。

```java
// JSON 请求体
.contentType(MediaType.APPLICATION_JSON)
.bodyValue(requestDto)

// 表单请求体
.contentType(MediaType.APPLICATION_FORM_URLENCODED)
.bodyValue(formData)
```

对于密码、Token、签名密钥等敏感字段，日志中不要打印原文。示例中只打印用户名，不打印密码。

### 文件上传请求处理

文件上传通常使用 `multipart/form-data` 请求。WebClient 可以配合 `MultipartBodyBuilder` 构建 multipart 请求体，并通过 `BodyInserters.fromMultipartData()` 发送文件和普通表单字段。multipart 上传适合调用文件服务、对象存储服务、图片识别服务、合同解析服务等接口。Spring 官方文档也说明，WebClient 支持 multipart 表单，并可以将普通字段和文件资源组合成请求体发送。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-body.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/webclient/vo/FileUploadVO.java`

以下 VO 用于接收文件上传接口返回结果。

```java
package io.github.atengk.webclient.vo;

import lombok.Data;

/**
 * 文件上传响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class FileUploadVO {

    /**
     * 文件编号
     */
    private String fileId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件访问地址
     */
    private String fileUrl;

    /**
     * 文件大小，单位字节
     */
    private Long fileSize;
}
```

文件位置：`src/main/java/io/github/atengk/webclient/service/RemoteFileService.java`

以下服务类演示如何通过本地文件路径上传文件，并携带业务字段。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;

/**
 * 远程文件接口调用服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteFileService {

    private final WebClient webClient;

    /**
     * 上传本地文件
     *
     * @param filePath 本地文件路径
     * @param bizType 业务类型
     * @return 文件上传结果
     */
    public FileUploadVO uploadFile(String filePath, String bizType) {
        if (StrUtil.isBlank(filePath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        File file = FileUtil.file(filePath);
        if (!FileUtil.exist(file) || !FileUtil.isFile(file)) {
            throw new IllegalArgumentException("文件不存在或不是普通文件：" + filePath);
        }

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(file))
                .filename(file.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM);
        builder.part("bizType", StrUtil.blankToDefault(bizType, "default"));

        log.info("开始调用远程文件上传接口，fileName：{}，fileSize：{}，bizType：{}",
                file.getName(), file.length(), bizType);

        FileUploadVO uploadVO = webClient.post()
                .uri("/files/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(FileUploadVO.class)
                .block();

        log.info("远程文件上传接口调用完成，fileName：{}，fileId：{}",
                file.getName(), uploadVO == null ? null : uploadVO.getFileId());
        return uploadVO;
    }
}
```

如果文件来自接口上传的 `MultipartFile`，也可以先转为字节资源再转发。不过要注意大文件转字节数组会占用内存，生产环境更建议使用临时文件或流式方式处理。

下面示例展示小文件转发方式。

```java
MultipartBodyBuilder builder = new MultipartBodyBuilder();
builder.part("file", multipartFile.getResource())
        .filename(multipartFile.getOriginalFilename())
        .contentType(MediaType.parseMediaType(multipartFile.getContentType()));
builder.part("bizType", "avatar");

Mono<FileUploadVO> result = webClient.post()
        .uri("/files/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(builder.build()))
        .retrieve()
        .bodyToMono(FileUploadVO.class);
```

文件上传时需要重点关注三类配置：单文件大小限制、整体请求大小限制、超时时间。对于大文件上传，不建议使用普通接口共用的短超时时间，可以单独创建文件上传专用 WebClient。

### 响应状态码处理

远程接口不一定总是返回 2xx 状态码。WebClient 使用 `retrieve()` 时，如果响应是 4xx 或 5xx，默认会产生 `WebClientResponseException` 及其子类。Spring 官方文档说明，可以通过 `onStatus()` 自定义错误响应处理逻辑；如果需要更细粒度访问状态码、响应头和响应体，可以使用 `exchangeToMono()`。([Home](https://docs.spring.vmware.com/spring-framework/reference/web/webflux-webclient/client-retrieve.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/webclient/exception/RemoteCallException.java`

以下异常类用于统一封装远程接口调用异常。

```java
package io.github.atengk.webclient.exception;

import lombok.Getter;

/**
 * 远程接口调用异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
public class RemoteCallException extends RuntimeException {

    /**
     * HTTP 状态码
     */
    private final Integer statusCode;

    /**
     * 错误响应体
     */
    private final String responseBody;

    /**
     * 创建远程接口调用异常
     *
     * @param statusCode HTTP 状态码
     * @param message 异常消息
     * @param responseBody 错误响应体
     */
    public RemoteCallException(Integer statusCode, String message, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
```

文件位置：`src/main/java/io/github/atengk/webclient/service/RemoteStatusService.java`

以下服务类演示如何通过 `onStatus()` 处理 4xx 和 5xx 响应。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.exception.RemoteCallException;
import io.github.atengk.webclient.vo.RemoteMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 远程状态码处理服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteStatusService {

    private final WebClient webClient;

    /**
     * 查询消息并处理响应状态码
     *
     * @param messageId 消息编号
     * @return 消息响应结果
     */
    public RemoteMessageVO getMessageById(Long messageId) {
        log.info("开始调用远程消息详情接口，messageId：{}", messageId);

        return webClient.get()
                .uri("/messages/{messageId}", messageId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> {
                                    String message = StrUtil.blankToDefault(body, "远程接口客户端错误");
                                    log.warn("远程消息接口返回4xx，messageId：{}，status：{}，body：{}",
                                            messageId, response.statusCode().value(), message);
                                    return new RemoteCallException(response.statusCode().value(), message, body);
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> {
                                    String message = StrUtil.blankToDefault(body, "远程接口服务端错误");
                                    log.error("远程消息接口返回5xx，messageId：{}，status：{}，body：{}",
                                            messageId, response.statusCode().value(), message);
                                    return new RemoteCallException(response.statusCode().value(), message, body);
                                })
                )
                .bodyToMono(RemoteMessageVO.class)
                .block();
    }
}
```

`onStatus()` 适合大多数异常处理场景。如果需要根据响应头、状态码和响应体进行更灵活的分支处理，可以使用 `exchangeToMono()`。

```java
Mono<RemoteMessageVO> result = webClient.get()
        .uri("/messages/{messageId}", messageId)
        .exchangeToMono(response -> {
            if (response.statusCode().is2xxSuccessful()) {
                return response.bodyToMono(RemoteMessageVO.class);
            }

            return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> Mono.error(new RemoteCallException(
                            response.statusCode().value(),
                            StrUtil.blankToDefault(body, "远程消息接口调用失败"),
                            body
                    )));
        });
```

一般建议：只需要响应体时使用 `retrieve()`；需要精细判断响应状态、响应头、错误响应体时使用 `exchangeToMono()`。

### 异常响应解析

实际项目中，远程接口的错误响应通常不是纯字符串，而是统一 JSON 结构。例如：

```json
{
  "code": 40001,
  "message": "参数校验失败",
  "traceId": "b7f2c7f1c1a94c88"
}
```

为了让业务异常更清晰，可以定义错误响应对象，并在 `onStatus()` 中解析错误 JSON。这里使用 Hutool 的 `JSONUtil` 做简单解析，避免在业务代码中直接散落字符串处理逻辑。

文件位置：`src/main/java/io/github/atengk/webclient/vo/RemoteErrorVO.java`

以下 VO 用于承接远程接口错误响应。

```java
package io.github.atengk.webclient.vo;

import lombok.Data;

/**
 * 远程错误响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class RemoteErrorVO {

    /**
     * 业务错误码
     */
    private Integer code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 链路追踪编号
     */
    private String traceId;
}
```

文件位置：`src/main/java/io/github/atengk/webclient/util/RemoteErrorParser.java`

以下工具类用于将远程错误响应体解析为可读错误信息。

```java
package io.github.atengk.webclient.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.webclient.vo.RemoteErrorVO;

/**
 * 远程错误响应解析工具
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class RemoteErrorParser {

    /**
     * 解析远程错误响应消息
     *
     * @param responseBody 错误响应体
     * @return 错误消息
     */
    public static String parseMessage(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return "远程接口调用失败";
        }

        if (!JSONUtil.isTypeJSON(responseBody)) {
            return responseBody;
        }

        try {
            RemoteErrorVO errorVO = JSONUtil.toBean(responseBody, RemoteErrorVO.class);
            if (errorVO != null && StrUtil.isNotBlank(errorVO.getMessage())) {
                return errorVO.getMessage();
            }
            return responseBody;
        } catch (Exception ex) {
            return responseBody;
        }
    }

    /**
     * 解析远程错误响应对象
     *
     * @param responseBody 错误响应体
     * @return 错误响应对象
     */
    public static RemoteErrorVO parseError(String responseBody) {
        if (StrUtil.isBlank(responseBody) || !JSONUtil.isTypeJSON(responseBody)) {
            return null;
        }

        try {
            return JSONUtil.toBean(responseBody, RemoteErrorVO.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private RemoteErrorParser() {
    }
}
```

文件位置：`src/main/java/io/github/atengk/webclient/service/RemoteErrorHandleService.java`

以下服务类演示如何在异常响应中解析 JSON 错误体，并抛出统一异常。

```java
package io.github.atengk.webclient.service;

import io.github.atengk.webclient.exception.RemoteCallException;
import io.github.atengk.webclient.util.RemoteErrorParser;
import io.github.atengk.webclient.vo.RemoteMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 远程异常响应解析服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteErrorHandleService {

    private final WebClient webClient;

    /**
     * 查询消息并解析异常响应
     *
     * @param messageId 消息编号
     * @return 消息响应结果
     */
    public RemoteMessageVO getMessageWithErrorParse(Long messageId) {
        log.info("开始调用远程消息详情接口并解析异常响应，messageId：{}", messageId);

        return webClient.get()
                .uri("/messages/{messageId}", messageId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> {
                                    String message = RemoteErrorParser.parseMessage(body);
                                    log.warn("远程消息接口调用失败，messageId：{}，status：{}，message：{}",
                                            messageId, response.statusCode().value(), message);
                                    return new RemoteCallException(response.statusCode().value(), message, body);
                                })
                )
                .bodyToMono(RemoteMessageVO.class)
                .block();
    }
}
```

如果多个远程接口都使用相同的异常响应结构，建议将 `onStatus()` 封装为通用方法，避免每个 Service 重复编写异常解析逻辑。

```java
private Function<ClientResponse, Mono<? extends Throwable>> remoteErrorHandler(String apiName) {
    return response -> response.bodyToMono(String.class)
            .defaultIfEmpty("")
            .map(body -> {
                String message = RemoteErrorParser.parseMessage(body);
                log.warn("远程接口调用失败，apiName：{}，status：{}，message：{}",
                        apiName, response.statusCode().value(), message);
                return new RemoteCallException(response.statusCode().value(), message, body);
            });
}
```

使用该方法时，需要引入以下类型：

```java
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;
```

异常响应解析的核心原则是：HTTP 状态码用于判断调用是否成功，业务错误码用于判断远程服务内部的失败原因，错误响应体用于记录排查信息。生产环境日志要避免输出 Token、密码、身份证号、手机号等敏感信息。

## Reactor 基础配合

WebClient 基于 Reactor 编程模型，常见返回值是 `Mono<T>` 和 `Flux<T>`。Reactor 官方文档说明，`Mono<T>` 表示 0 到 1 个异步结果，`Flux<T>` 表示 0 到 N 个异步元素序列，两者都可能正常完成或以错误信号结束。理解 Mono、Flux、`block()` 和非阻塞返回方式，是正确使用 WebClient 的基础。([Project Reactor](https://projectreactor.io/docs/core/release/reference/coreFeatures/mono.html?utm_source=chatgpt.com))

### Mono 的使用

`Mono<T>` 表示最多返回一个元素的异步结果。WebClient 调用单个详情接口、创建接口、修改接口、删除接口时，通常使用 `Mono<T>` 或 `Mono<Void>`。Reactor 官方文档说明，`Mono<T>` 最多发出一个 `onNext` 信号，然后以 `onComplete` 正常结束，或者以 `onError` 失败结束。([Project Reactor](https://projectreactor.io/docs/core/release/reference/coreFeatures/mono.html?utm_source=chatgpt.com))

常见 Mono 使用方式如下：

```java
Mono<RemoteMessageVO> messageMono = webClient.get()
        .uri("/messages/{messageId}", messageId)
        .retrieve()
        .bodyToMono(RemoteMessageVO.class);
```

Mono 可以通过 `map()` 转换数据，通过 `flatMap()` 串联异步调用，通过 `onErrorResume()` 处理异常。

文件位置：`src/main/java/io/github/atengk/webclient/service/ReactorMonoService.java`

以下服务类演示 Mono 在 WebClient 调用中的常见组合方式。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.dto.CreateMessageRequest;
import io.github.atengk.webclient.vo.RemoteMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Reactor Mono 使用示例服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReactorMonoService {

    private final WebClient webClient;

    /**
     * 查询单条消息
     *
     * @param messageId 消息编号
     * @return 消息异步结果
     */
    public Mono<RemoteMessageVO> getMessageMono(Long messageId) {
        log.info("开始构建消息详情查询Mono，messageId：{}", messageId);

        return webClient.get()
                .uri("/messages/{messageId}", messageId)
                .retrieve()
                .bodyToMono(RemoteMessageVO.class)
                .doOnSuccess(message -> log.info("消息详情查询完成，messageId：{}", messageId))
                .doOnError(ex -> log.error("消息详情查询失败，messageId：{}，error：{}", messageId, ex.getMessage()));
    }

    /**
     * 创建消息后返回消息编号
     *
     * @param request 创建消息请求参数
     * @return 消息编号异步结果
     */
    public Mono<Long> createMessageAndReturnId(CreateMessageRequest request) {
        if (request == null || StrUtil.isBlank(request.getTitle())) {
            return Mono.error(new IllegalArgumentException("消息标题不能为空"));
        }

        return webClient.post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RemoteMessageVO.class)
                .map(RemoteMessageVO::getMessageId)
                .doOnSuccess(messageId -> log.info("消息创建完成，messageId：{}", messageId));
    }

    /**
     * 创建消息后继续查询消息详情
     *
     * @param request 创建消息请求参数
     * @return 消息详情异步结果
     */
    public Mono<RemoteMessageVO> createThenGetMessage(CreateMessageRequest request) {
        return createMessageAndReturnId(request)
                .flatMap(this::getMessageMono)
                .onErrorResume(ex -> {
                    log.error("创建并查询消息流程失败，error：{}", ex.getMessage());
                    return Mono.empty();
                });
    }
}
```

`map()` 用于同步转换，例如从 `RemoteMessageVO` 中提取 `messageId`；`flatMap()` 用于继续发起异步调用，例如创建成功后再查询详情；`onErrorResume()` 用于异常降级，例如远程接口失败时返回空结果或默认值。

常见 Mono 操作符如下：

| 操作符             | 说明                           |
| ------------------ | ------------------------------ |
| `map()`            | 同步转换元素                   |
| `flatMap()`        | 异步转换并展开新的 Mono        |
| `defaultIfEmpty()` | 空结果时提供默认值             |
| `switchIfEmpty()`  | 空结果时切换到另一个 Mono      |
| `doOnSuccess()`    | 成功时执行日志等副作用         |
| `doOnError()`      | 失败时执行日志等副作用         |
| `onErrorResume()`  | 异常时降级返回另一个 Mono      |
| `then()`           | 忽略前一个结果，只关心完成信号 |

Mono 不等于线程。创建 Mono 只是描述一段异步执行流程，真正触发执行通常发生在订阅时。WebFlux Controller 返回 Mono 时，框架会负责订阅；普通同步代码中调用 `block()` 时，也会触发执行。

### Flux 的使用

`Flux<T>` 表示 0 到 N 个元素的异步序列。WebClient 调用列表接口、流式接口、SSE 接口、NDJSON 接口时，通常会使用 `Flux<T>`。Reactor 官方文档说明，`Flux<T>` 是通用响应式类型，表示 0 到 N 个元素，并可以正常完成或以错误信号结束。([Home](https://docs.spring.io/projectreactor/reactor-core/docs/current/reference/html/?utm_source=chatgpt.com))

常见 Flux 使用方式如下：

```java
Flux<RemoteMessageVO> messageFlux = webClient.get()
        .uri("/messages/stream")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(RemoteMessageVO.class);
```

文件位置：`src/main/java/io/github/atengk/webclient/service/ReactorFluxService.java`

以下服务类演示如何使用 Flux 处理消息列表和流式响应。

```java
package io.github.atengk.webclient.service;

import io.github.atengk.webclient.vo.RemoteMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Reactor Flux 使用示例服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReactorFluxService {

    private final WebClient webClient;

    /**
     * 查询消息流
     *
     * @param receiverId 接收人编号
     * @return 消息流
     */
    public Flux<RemoteMessageVO> streamMessages(Long receiverId) {
        log.info("开始构建消息流查询Flux，receiverId：{}", receiverId);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages/stream")
                        .queryParam("receiverId", receiverId)
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(RemoteMessageVO.class)
                .doOnNext(message -> log.info("接收到消息流数据，messageId：{}", message.getMessageId()))
                .doOnError(ex -> log.error("消息流查询失败，receiverId：{}，error：{}", receiverId, ex.getMessage()))
                .doOnComplete(() -> log.info("消息流查询完成，receiverId：{}", receiverId));
    }

    /**
     * 查询消息列表并过滤有效数据
     *
     * @param receiverId 接收人编号
     * @return 有效消息流
     */
    public Flux<RemoteMessageVO> listValidMessages(Long receiverId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages")
                        .queryParam("receiverId", receiverId)
                        .build())
                .retrieve()
                .bodyToFlux(RemoteMessageVO.class)
                .filter(message -> message.getMessageId() != null)
                .delayElements(Duration.ofMillis(10))
                .doOnNext(message -> log.info("处理消息数据，messageId：{}", message.getMessageId()));
    }
}
```

Flux 常见操作符如下：

| 操作符            | 说明                           |
| ----------------- | ------------------------------ |
| `map()`           | 转换每个元素                   |
| `flatMap()`       | 将每个元素转换为异步流并合并   |
| `filter()`        | 过滤元素                       |
| `take()`          | 获取前 N 个元素                |
| `collectList()`   | 将 Flux 收集为 `Mono<List<T>>` |
| `doOnNext()`      | 每个元素到达时执行副作用       |
| `doOnComplete()`  | 流正常完成时执行副作用         |
| `doOnError()`     | 流异常时执行副作用             |
| `onErrorResume()` | 异常时切换到降级流             |

如果远程接口返回的是普通 JSON 数组，也可以使用 `bodyToFlux(RemoteMessageVO.class)` 逐个处理元素；如果希望一次性得到列表，可以使用 `collectList()`。

```java
Mono<List<RemoteMessageVO>> messageListMono = webClient.get()
        .uri("/messages")
        .retrieve()
        .bodyToFlux(RemoteMessageVO.class)
        .collectList();
```

对于大数据量或流式接口，Flux 更适合逐步消费数据；对于普通分页列表接口，返回 `Mono<List<T>>` 也可以，具体取决于业务处理方式。

### block 阻塞调用场景

`block()` 会阻塞当前线程，直到 Mono 或 Flux 返回结果、完成或抛出异常。WebClient 虽然是非阻塞客户端，但在传统 Spring MVC 项目、定时任务、命令行任务、同步 Service 中，可以使用 `block()` 将异步结果转为同步结果。Spring 官方文档中也专门说明了 WebClient 的同步使用方式，即在结果末尾调用 `block()` 获取对象。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html?utm_source=chatgpt.com))

适合使用 `block()` 的场景如下：

| 场景                  | 说明                         |
| --------------------- | ---------------------------- |
| Spring MVC Controller | 控制器方法本身是同步返回     |
| 普通 Service 方法     | 方法签名返回普通 Java 对象   |
| 定时任务              | 定时任务内部同步执行远程调用 |
| 启动初始化任务        | 启动时同步加载外部配置或数据 |
| 单元测试临时验证      | 简单验证远程调用结果         |

文件位置：`src/main/java/io/github/atengk/webclient/controller/MessageSyncController.java`

以下 Controller 演示传统同步接口中如何使用 `block()` 返回结果。

```java
package io.github.atengk.webclient.controller;

import io.github.atengk.webclient.service.ReactorMonoService;
import io.github.atengk.webclient.vo.RemoteMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息同步调用接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MessageSyncController {

    private final ReactorMonoService reactorMonoService;

    /**
     * 同步查询消息详情
     *
     * @param messageId 消息编号
     * @return 消息详情
     */
    @GetMapping("/sync/messages/{messageId}")
    public RemoteMessageVO getMessageSync(@PathVariable Long messageId) {
        log.info("同步接口开始查询消息详情，messageId：{}", messageId);

        RemoteMessageVO messageVO = reactorMonoService.getMessageMono(messageId)
                .block();

        log.info("同步接口查询消息详情完成，messageId：{}", messageId);
        return messageVO;
    }
}
```

验证方式如下：

```bash
# 调用同步查询接口
curl -X GET "http://localhost:8080/sync/messages/1001"
```

`block()` 使用注意事项如下：

| 注意事项                   | 说明                                 |
| -------------------------- | ------------------------------------ |
| 不要在响应式链路中随意使用 | WebFlux 事件循环线程中阻塞会影响吞吐 |
| 必须配置超时               | 外部接口卡住时，当前线程会一直等待   |
| 不适合高并发长耗时接口     | 容易造成线程堆积                     |
| 可以用于兼容老代码         | 传统同步业务迁移时比较常见           |
| 测试中可以使用             | 单元测试或临时验证更直接             |

如果确实需要阻塞，建议配合前文的连接超时、响应超时和异常处理，避免外部接口异常导致应用线程长时间等待。

### 非阻塞调用场景

非阻塞调用是 WebClient 的核心优势。非阻塞场景中，Controller、Service 或处理流程直接返回 `Mono<T>` 或 `Flux<T>`，不主动调用 `block()`，由 WebFlux 框架负责订阅和写出响应。WebClient 官方文档也说明，其函数式流畅 API 基于 Reactor，可以声明式组合异步逻辑，无需直接处理线程和并发，并且支持流式传输。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html?utm_source=chatgpt.com))

适合非阻塞调用的场景如下：

| 场景               | 说明                                |
| ------------------ | ----------------------------------- |
| WebFlux Controller | 接口直接返回 `Mono<T>` 或 `Flux<T>` |
| SSE 消息推送       | 持续返回事件流                      |
| 多接口异步编排     | 使用 `Mono.zip()` 聚合多个远程接口  |
| 网关或 BFF 服务    | 聚合后端多个接口返回给前端          |
| 高并发远程调用     | 减少线程阻塞等待                    |
| 流式文件或数据处理 | 按块读取和写出数据                  |

文件位置：`src/main/java/io/github/atengk/webclient/controller/MessageReactiveController.java`

以下 Controller 演示 WebFlux 风格的非阻塞接口，方法直接返回 `Mono` 和 `Flux`。

```java
package io.github.atengk.webclient.controller;

import io.github.atengk.webclient.dto.CreateMessageRequest;
import io.github.atengk.webclient.service.ReactorFluxService;
import io.github.atengk.webclient.service.ReactorMonoService;
import io.github.atengk.webclient.vo.RemoteMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 消息非阻塞调用接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MessageReactiveController {

    private final ReactorMonoService reactorMonoService;

    private final ReactorFluxService reactorFluxService;

    /**
     * 非阻塞查询消息详情
     *
     * @param messageId 消息编号
     * @return 消息详情异步结果
     */
    @GetMapping("/reactive/messages/{messageId}")
    public Mono<RemoteMessageVO> getMessageReactive(@PathVariable Long messageId) {
        log.info("非阻塞接口开始查询消息详情，messageId：{}", messageId);
        return reactorMonoService.getMessageMono(messageId);
    }

    /**
     * 非阻塞创建消息
     *
     * @param request 创建消息请求参数
     * @return 消息编号异步结果
     */
    @PostMapping("/reactive/messages")
    public Mono<Long> createMessageReactive(@RequestBody CreateMessageRequest request) {
        log.info("非阻塞接口开始创建消息，receiverId：{}", request.getReceiverId());
        return reactorMonoService.createMessageAndReturnId(request);
    }

    /**
     * 以事件流方式返回消息数据
     *
     * @param receiverId 接收人编号
     * @return 消息事件流
     */
    @GetMapping(value = "/reactive/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<RemoteMessageVO> streamMessages(@RequestParam Long receiverId) {
        log.info("非阻塞接口开始返回消息流，receiverId：{}", receiverId);
        return reactorFluxService.streamMessages(receiverId);
    }
}
```

验证方式如下：

```bash
# 非阻塞查询消息详情
curl -X GET "http://localhost:8080/reactive/messages/1001"

# 非阻塞创建消息
curl -X POST "http://localhost:8080/reactive/messages" \
  -H "Content-Type: application/json" \
  -d '{"receiverId":10001,"title":"系统通知","content":"这是一条测试消息"}'

# 接收 SSE 消息流
curl -N "http://localhost:8080/reactive/messages/stream?receiverId=10001"
```

多接口异步聚合可以使用 `Mono.zip()`。例如同时查询消息详情和用户信息，两个远程调用可以并发执行，然后合并结果。

```java
Mono<RemoteMessageVO> messageMono = reactorMonoService.getMessageMono(messageId);
Mono<RemoteUserVO> userMono = remoteUserService.getUserMono(userId);

Mono<Tuple2<RemoteMessageVO, RemoteUserVO>> result = Mono.zip(messageMono, userMono);
```

非阻塞调用的关键原则是：返回响应式类型，不主动订阅，不调用 `block()`，将执行权交给框架。只有在明确处于同步边界时，才使用 `block()` 做兼容处理。



## 业务封装实践

本节介绍如何在业务项目中对 WebClient 进行二次封装。实际开发中不建议在每个 Service 中直接重复编写 `retrieve()`、`onStatus()`、`bodyToMono()`、日志打印、Token 设置和异常解析逻辑，而是应封装一个通用 HTTP 客户端组件。WebClient 支持通过 `ExchangeFilterFunction` 注册过滤器，用于处理认证、日志、请求改写等横切逻辑；`retrieve()` 可用于提取响应体，4xx 和 5xx 响应默认会转为 `WebClientResponseException`，也可以通过 `onStatus()` 自定义异常处理。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-filter.html?utm_source=chatgpt.com))

### 通用 HTTP 客户端封装

通用 HTTP 客户端封装的目标是让业务代码只关心接口路径、请求参数和响应类型。公共逻辑，例如请求头、状态码判断、业务错误码判断、异常转换、日志记录、泛型解析，都放在统一组件中处理。

推荐目录结构如下：

```text
src
└── main
    └── java
        └── io
            └── github
                └── atengk
                    └── webclient
                        ├── client
                        │   └── CommonHttpClient.java
                        ├── exception
                        │   └── RemoteCallException.java
                        ├── util
                        │   └── RemoteErrorParser.java
                        └── vo
                            └── ApiResult.java
```

文件位置：`src/main/java/io/github/atengk/webclient/vo/ApiResult.java`

以下通用响应对象用于承接第三方接口或内部远程服务的统一响应结构。

```java
package io.github.atengk.webclient.vo;

import lombok.Data;

/**
 * 通用接口响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class ApiResult<T> {

    /**
     * 响应编码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 判断是否成功
     *
     * @return 是否成功
     */
    public boolean success() {
        return Integer.valueOf(200).equals(code);
    }
}
```

文件位置：`src/main/java/io/github/atengk/webclient/client/CommonHttpClient.java`

以下客户端封装 GET、POST JSON 和统一响应解析，业务 Service 可以直接注入使用。

```java
package io.github.atengk.webclient.client;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.exception.RemoteCallException;
import io.github.atengk.webclient.util.RemoteErrorParser;
import io.github.atengk.webclient.vo.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 通用 HTTP 客户端
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonHttpClient {

    private final WebClient webClient;

    /**
     * GET 请求
     *
     * @param path 请求路径
     * @param queryParams 查询参数
     * @param responseType 响应类型
     * @return 响应数据
     * @param <T> 响应数据类型
     */
    public <T> Mono<T> get(String path,
                           Map<String, Object> queryParams,
                           ParameterizedTypeReference<ApiResult<T>> responseType) {
        log.info("开始执行GET请求，path：{}，queryParams：{}", path, queryParams);

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path);
                    if (MapUtil.isNotEmpty(queryParams)) {
                        queryParams.forEach((key, value) -> {
                            if (StrUtil.isNotBlank(key) && value != null) {
                                uriBuilder.queryParam(key, value);
                            }
                        });
                    }
                    return uriBuilder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> buildRemoteException(response.statusCode(), body))
                )
                .bodyToMono(responseType)
                .map(this::unwrapResult)
                .doOnSuccess(data -> log.info("GET请求执行完成，path：{}", path))
                .doOnError(ex -> log.error("GET请求执行失败，path：{}，error：{}", path, ex.getMessage()));
    }

    /**
     * POST JSON 请求
     *
     * @param path 请求路径
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @return 响应数据
     * @param <T> 响应数据类型
     */
    public <T> Mono<T> postJson(String path,
                                Object requestBody,
                                ParameterizedTypeReference<ApiResult<T>> responseType) {
        log.info("开始执行POST JSON请求，path：{}", path);

        return webClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> buildRemoteException(response.statusCode(), body))
                )
                .bodyToMono(responseType)
                .map(this::unwrapResult)
                .doOnSuccess(data -> log.info("POST JSON请求执行完成，path：{}", path))
                .doOnError(ex -> log.error("POST JSON请求执行失败，path：{}，error：{}", path, ex.getMessage()));
    }

    /**
     * 解析通用响应结果
     *
     * @param result 通用响应结果
     * @return 响应数据
     * @param <T> 响应数据类型
     */
    private <T> T unwrapResult(ApiResult<T> result) {
        if (result == null) {
            throw new RemoteCallException(500, "远程接口响应为空", "");
        }

        if (!result.success()) {
            String message = StrUtil.blankToDefault(result.getMessage(), "远程接口业务处理失败");
            throw new RemoteCallException(result.getCode(), message, "");
        }

        return result.getData();
    }

    /**
     * 构建远程调用异常
     *
     * @param statusCode HTTP 状态码
     * @param responseBody 响应体
     * @return 远程调用异常
     */
    private RemoteCallException buildRemoteException(HttpStatusCode statusCode, String responseBody) {
        String message = RemoteErrorParser.parseMessage(responseBody);
        return new RemoteCallException(statusCode.value(), message, responseBody);
    }
}
```

业务 Service 使用时，只需要传入接口路径、参数和泛型响应类型。

```java
Mono<RemoteUserVO> userMono = commonHttpClient.get(
        "/users/{userId}",
        Map.of("includeRole", true),
        new ParameterizedTypeReference<ApiResult<RemoteUserVO>>() {
        }
);
```

如果路径中存在 `{userId}` 这种路径变量，上面的通用方法还需要继续扩展路径变量支持。简单项目中可以先使用完整路径，例如 `/users/1001`；中大型项目建议再封装 `pathVariables` 参数。

### 请求日志打印

请求日志用于排查远程接口调用问题，建议通过 `ExchangeFilterFunction` 统一处理。过滤器可以在请求发出前读取请求方法、URL、Header，也可以在响应返回后读取状态码。Spring 官方文档说明，`ExchangeFilterFunction` 可以通过 `WebClient.Builder` 注册，用于拦截和修改请求，也常用于认证等横切逻辑。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-filter.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/webclient/config/WebClientLogFilter.java`

以下过滤器用于统一打印请求和响应日志。生产环境中应避免打印敏感 Header 和完整请求体。

```java
package io.github.atengk.webclient.config;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * WebClient 日志过滤器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class WebClientLogFilter {

    /**
     * 创建请求日志过滤器
     *
     * @return 请求日志过滤器
     */
    public ExchangeFilterFunction requestLogFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info("WebClient请求开始，method：{}，url：{}", request.method(), request.url());
            logSafeHeaders(request);
            return Mono.just(request);
        });
    }

    /**
     * 创建响应日志过滤器
     *
     * @return 响应日志过滤器
     */
    public ExchangeFilterFunction responseLogFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("WebClient响应返回，status：{}", response.statusCode().value());
            return Mono.just(response);
        });
    }

    /**
     * 打印安全请求头
     *
     * @param request 请求对象
     */
    private void logSafeHeaders(ClientRequest request) {
        HttpHeaders headers = request.headers();
        List<String> traceIdList = headers.get("X-Trace-Id");
        List<String> tenantIdList = headers.get("X-Tenant-Id");

        if (CollUtil.isNotEmpty(traceIdList)) {
            log.info("WebClient请求头，X-Trace-Id：{}", traceIdList.get(0));
        }

        if (CollUtil.isNotEmpty(tenantIdList)) {
            log.info("WebClient请求头，X-Tenant-Id：{}", tenantIdList.get(0));
        }
    }
}
```

在全局配置中注册日志过滤器。

文件位置：`src/main/java/io/github/atengk/webclient/config/WebClientConfig.java`

```java
@Bean
public WebClient webClient(WebClient.Builder builder, WebClientLogFilter webClientLogFilter) {
    return builder
            .baseUrl(properties.getBaseUrl())
            .filter(webClientLogFilter.requestLogFilter())
            .filter(webClientLogFilter.responseLogFilter())
            .build();
}
```

日志打印建议遵循以下规则：

| 日志内容      | 建议                                       |
| ------------- | ------------------------------------------ |
| 请求方法      | 可以打印                                   |
| 请求 URL      | 可以打印，但查询参数中有敏感信息时需要脱敏 |
| TraceId       | 建议打印                                   |
| TenantId      | 可以打印                                   |
| Authorization | 不要打印完整值                             |
| Cookie        | 不要打印                                   |
| 请求体        | 默认不打印，必要时脱敏后打印               |
| 响应体        | 默认不打印，异常时可打印脱敏后的错误体     |

### 统一异常处理

统一异常处理用于屏蔽 WebClient 底层异常差异，将远程调用失败统一转换为业务可识别的异常。`retrieve()` 默认会将 4xx 和 5xx 响应转换为异常，也可以通过 `onStatus()` 自定义异常转换；如果需要更灵活地访问状态码、响应头和响应体，可以使用 `exchangeToMono()`。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-retrieve.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/webclient/handler/WebClientErrorHandler.java`

以下处理器用于统一构建 `onStatus()` 异常处理逻辑。

```java
package io.github.atengk.webclient.handler;

import io.github.atengk.webclient.exception.RemoteCallException;
import io.github.atengk.webclient.util.RemoteErrorParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * WebClient 异常处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class WebClientErrorHandler {

    /**
     * 构建远程异常处理函数
     *
     * @param apiName 接口名称
     * @return 异常处理函数
     */
    public Function<ClientResponse, Mono<? extends Throwable>> handleError(String apiName) {
        return response -> response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> {
                    String message = RemoteErrorParser.parseMessage(body);
                    log.warn("远程接口调用失败，apiName：{}，status：{}，message：{}",
                            apiName, response.statusCode().value(), message);
                    return new RemoteCallException(response.statusCode().value(), message, body);
                });
    }
}
```

业务调用时使用：

```java
RemoteUserVO userVO = webClient.get()
        .uri("/users/{userId}", userId)
        .retrieve()
        .onStatus(HttpStatusCode::isError, webClientErrorHandler.handleError("查询用户详情"))
        .bodyToMono(RemoteUserVO.class)
        .block();
```

全局异常类可以继续沿用前文定义的 `RemoteCallException`。如果项目中已经有统一业务异常，例如 `BusinessException`，也可以在 `WebClientErrorHandler` 中直接转换为项目内部异常类型。

### 泛型响应结果解析

很多接口返回统一包装结构，例如 `ApiResult<T>`、`Result<T>`、`BaseResponse<T>`。由于 Java 泛型擦除，解析 `ApiResult<RemoteUserVO>`、`ApiResult<List<RemoteUserVO>>` 时不能简单使用 `ApiResult.class`，需要使用 `ParameterizedTypeReference` 保留泛型信息。WebClient 的 `retrieve().bodyToMono(...)` 支持传入 `ParameterizedTypeReference`，适合处理泛型响应体。([Home](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-retrieve.html?utm_source=chatgpt.com))

解析单对象响应：

```java
Mono<ApiResult<RemoteUserVO>> resultMono = webClient.get()
        .uri("/users/{userId}", userId)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<ApiResult<RemoteUserVO>>() {
        });
```

解析列表响应：

```java
Mono<ApiResult<List<RemoteUserVO>>> resultMono = webClient.get()
        .uri("/users")
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<ApiResult<List<RemoteUserVO>>>() {
        });
```

封装为工具方法时，可以将泛型类型由调用方传入。

```java
public <T> Mono<T> getData(String path, ParameterizedTypeReference<ApiResult<T>> responseType) {
    return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(responseType)
            .map(result -> {
                if (result == null || !result.success()) {
                    throw new RemoteCallException(500, "远程接口业务响应失败", "");
                }
                return result.getData();
            });
}
```

调用示例：

```java
Mono<RemoteUserVO> userMono = getData(
        "/users/1001",
        new ParameterizedTypeReference<ApiResult<RemoteUserVO>>() {
        }
);

Mono<List<RemoteUserVO>> userListMono = getData(
        "/users",
        new ParameterizedTypeReference<ApiResult<List<RemoteUserVO>>>() {
        }
);
```

对于统一响应结构，建议在通用 HTTP 客户端中完成以下处理：

| 处理项          | 说明                                           |
| --------------- | ---------------------------------------------- |
| HTTP 状态码判断 | 处理 4xx、5xx                                  |
| 业务状态码判断  | 判断 `code` 是否为成功码                       |
| 错误消息提取    | 优先使用远程接口返回的 `message`               |
| data 提取       | 业务代码只拿到真正的数据                       |
| 泛型类型传入    | 使用 `ParameterizedTypeReference` 保留泛型信息 |

## 常见调用场景

本节汇总 WebClient 在项目中最常见的调用方式，包括第三方 REST 接口调用、Token 请求、动态 Header 设置、文件下载、超时与重试。前文已经完成通用客户端、日志和异常封装，本节重点给出可直接套用的业务场景代码。

### 调用第三方 REST 接口

调用第三方 REST 接口时，通常需要明确请求地址、请求方法、请求参数、请求头、响应类型和异常处理方式。对于稳定接口，可以使用统一的 `CommonHttpClient`；对于特殊接口，可以直接使用 WebClient 编写独立方法。

文件位置：`src/main/java/io/github/atengk/webclient/service/ThirdPartyUserService.java`

以下服务类演示调用第三方用户详情接口。

```java
package io.github.atengk.webclient.service;

import io.github.atengk.webclient.client.CommonHttpClient;
import io.github.atengk.webclient.vo.ApiResult;
import io.github.atengk.webclient.vo.RemoteUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 第三方用户接口服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdPartyUserService {

    private final CommonHttpClient commonHttpClient;

    /**
     * 查询第三方用户详情
     *
     * @param userId 用户编号
     * @return 用户详情
     */
    public RemoteUserVO getUserDetail(Long userId) {
        log.info("开始查询第三方用户详情，userId：{}", userId);

        Mono<RemoteUserVO> userMono = commonHttpClient.get(
                "/third-party/users/" + userId,
                Map.of("includeRole", true),
                new ParameterizedTypeReference<ApiResult<RemoteUserVO>>() {
                }
        );

        RemoteUserVO userVO = userMono.block();

        log.info("第三方用户详情查询完成，userId：{}", userId);
        return userVO;
    }
}
```

如果第三方接口没有统一包装结构，而是直接返回业务对象，可以单独使用 `bodyToMono(RemoteUserVO.class)`。

```java
RemoteUserVO userVO = webClient.get()
        .uri("/third-party/users/{userId}", userId)
        .retrieve()
        .bodyToMono(RemoteUserVO.class)
        .block();
```

### 携带 Token 调用接口

携带 Token 调用接口时，常见做法是在请求头中设置 `Authorization: Bearer xxx`。如果 Token 每次请求不同，可以在单次请求中设置；如果 Token 来源统一，可以通过过滤器自动追加。

文件位置：`src/main/java/io/github/atengk/webclient/service/TokenRemoteService.java`

以下服务类演示单次请求中携带 Token。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.vo.RemoteUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Token 远程调用服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRemoteService {

    private final WebClient webClient;

    /**
     * 携带 Token 查询用户详情
     *
     * @param userId 用户编号
     * @param token 访问令牌
     * @return 用户详情
     */
    public RemoteUserVO getUserWithToken(Long userId, String token) {
        if (StrUtil.isBlank(token)) {
            throw new IllegalArgumentException("访问令牌不能为空");
        }

        log.info("开始携带Token调用用户详情接口，userId：{}", userId);

        return webClient.get()
                .uri("/users/{userId}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(RemoteUserVO.class)
                .block();
    }
}
```

如果 Token 由系统统一维护，可以封装 Token 提供器和过滤器。

文件位置：`src/main/java/io/github/atengk/webclient/support/AccessTokenProvider.java`

```java
package io.github.atengk.webclient.support;

import org.springframework.stereotype.Component;

/**
 * 访问令牌提供器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Component
public class AccessTokenProvider {

    /**
     * 获取访问令牌
     *
     * @return 访问令牌
     */
    public String getToken() {
        // 实际项目中可从 Redis、配置中心、认证服务或本地缓存中读取
        return "mock-access-token";
    }
}
```

文件位置：`src/main/java/io/github/atengk/webclient/config/WebClientTokenFilter.java`

```java
package io.github.atengk.webclient.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.webclient.support.AccessTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient Token 过滤器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Component
@RequiredArgsConstructor
public class WebClientTokenFilter {

    private final AccessTokenProvider accessTokenProvider;

    /**
     * 创建 Token 过滤器
     *
     * @return Token 过滤器
     */
    public ExchangeFilterFunction tokenFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            String token = accessTokenProvider.getToken();
            if (StrUtil.isBlank(token)) {
                return Mono.just(request);
            }

            ClientRequest newRequest = ClientRequest.from(request)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            return Mono.just(newRequest);
        });
    }
}
```

Token 不建议写死在配置文件或代码中。生产环境中应优先使用认证服务获取，并结合缓存、过期时间和刷新机制处理。

### 设置动态 Header

动态 Header 常用于传递租户编号、链路追踪 ID、语言环境、灰度标识、请求来源等运行时上下文。动态 Header 可以在单次请求中设置，也可以通过过滤器从上下文中读取并统一追加。

单次请求设置动态 Header：

```java
RemoteUserVO userVO = webClient.get()
        .uri("/users/{userId}", userId)
        .headers(headers -> {
            headers.add("X-Tenant-Id", tenantId);
            headers.add("X-Trace-Id", traceId);
            headers.add("X-Lang", "zh-CN");
        })
        .retrieve()
        .bodyToMono(RemoteUserVO.class)
        .block();
```

如果是 Servlet Web 项目，可以从 `RequestContextHolder` 中读取当前请求头，然后透传给下游接口。

文件位置：`src/main/java/io/github/atengk/webclient/config/WebClientDynamicHeaderFilter.java`

以下过滤器演示透传当前请求中的租户编号和链路追踪 ID。

```java
package io.github.atengk.webclient.config;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient 动态请求头过滤器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Component
public class WebClientDynamicHeaderFilter {

    /**
     * 创建动态请求头过滤器
     *
     * @return 动态请求头过滤器
     */
    public ExchangeFilterFunction dynamicHeaderFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return Mono.just(request);
            }

            HttpServletRequest servletRequest = attributes.getRequest();
            String tenantId = servletRequest.getHeader("X-Tenant-Id");
            String traceId = servletRequest.getHeader("X-Trace-Id");

            ClientRequest.Builder builder = ClientRequest.from(request);

            if (StrUtil.isNotBlank(tenantId)) {
                builder.header("X-Tenant-Id", tenantId);
            }

            if (StrUtil.isNotBlank(traceId)) {
                builder.header("X-Trace-Id", traceId);
            }

            return Mono.just(builder.build());
        });
    }
}
```

在全局 WebClient 配置中注册：

```java
@Bean
public WebClient webClient(WebClient.Builder builder,
                           WebClientDynamicHeaderFilter dynamicHeaderFilter) {
    return builder
            .baseUrl(properties.getBaseUrl())
            .filter(dynamicHeaderFilter.dynamicHeaderFilter())
            .build();
}
```

如果是纯 WebFlux 项目，不建议使用 `RequestContextHolder`，应改用 Reactor Context 传递上下文。

### 下载文件

文件下载时，如果响应体较小，可以直接读取为字节数组；如果文件较大，应使用 `DataBufferUtils.write()` 以流式方式写入文件，避免一次性把完整文件加载到内存。WebClient 支持将响应体解码为 `Flux<DataBuffer>`，这类方式更适合文件流和大响应体处理。

文件位置：`src/main/java/io/github/atengk/webclient/service/FileDownloadService.java`

以下服务类演示使用 WebClient 下载文件到本地路径。

```java
package io.github.atengk.webclient.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 文件下载服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    private final WebClient webClient;

    /**
     * 下载文件到本地
     *
     * @param fileUrl 文件下载地址
     * @param savePath 本地保存路径
     */
    public void downloadToFile(String fileUrl, String savePath) {
        if (StrUtil.hasBlank(fileUrl, savePath)) {
            throw new IllegalArgumentException("文件下载地址和保存路径不能为空");
        }

        FileUtil.mkParentDirs(savePath);
        Path targetPath = Path.of(savePath);

        log.info("开始下载远程文件，fileUrl：{}，savePath：{}", fileUrl, savePath);

        Flux<DataBuffer> dataBufferFlux = webClient.get()
                .uri(fileUrl)
                .retrieve()
                .bodyToFlux(DataBuffer.class);

        DataBufferUtils.write(dataBufferFlux, targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                .doOnComplete(() -> log.info("远程文件下载完成，savePath：{}", savePath))
                .doOnError(ex -> log.error("远程文件下载失败，fileUrl：{}，error：{}", fileUrl, ex.getMessage()))
                .block();
    }
}
```

如果下载接口需要携带 Token，可以追加请求头：

```java
Flux<DataBuffer> dataBufferFlux = webClient.get()
        .uri(fileUrl)
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .bodyToFlux(DataBuffer.class);
```

文件下载注意事项如下：

| 注意事项                 | 说明                           |
| ------------------------ | ------------------------------ |
| 大文件不要使用 `byte[]`  | 容易造成内存占用过高           |
| 下载接口建议设置更长超时 | 文件下载耗时通常比普通接口更长 |
| 保存路径需要校验         | 避免非法路径或覆盖重要文件     |
| 日志不要打印敏感下载地址 | 带签名的 URL 可能包含访问凭证  |
| 下载失败要清理临时文件   | 避免残留不完整文件             |

### 调用超时与重试

远程调用必须配置超时，避免接口卡住导致线程或连接资源长时间占用。重试适合处理临时网络抖动、短暂 502、503、504 等可恢复错误，但不适合对所有请求无差别重试。Reactor 的 `Retry` 可用于 `Mono.retryWhen(Retry)` 和 `Flux.retryWhen(Retry)`，`Retry.backoff(maxAttempts, minBackoff)` 提供指数退避策略，并支持 jitter、最大退避时间、异常过滤等配置。([Spring Enterprise 文档](https://docs.enterprise.spring.io/reactor-core/docs/3.6.19/javadoc-api/reactor/util/retry/Retry.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/webclient/service/RetryRemoteService.java`

以下服务类演示 WebClient 调用时配置单次请求超时和退避重试。

```java
package io.github.atengk.webclient.service;

import io.github.atengk.webclient.exception.RemoteCallException;
import io.github.atengk.webclient.vo.RemoteUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * 远程调用重试服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryRemoteService {

    private final WebClient webClient;

    /**
     * 查询用户并支持超时与重试
     *
     * @param userId 用户编号
     * @return 用户详情
     */
    public RemoteUserVO getUserWithRetry(Long userId) {
        log.info("开始调用用户详情接口并启用重试，userId：{}", userId);

        return webClient.get()
                .uri("/users/{userId}", userId)
                .retrieve()
                .bodyToMono(RemoteUserVO.class)
                .timeout(Duration.ofSeconds(3))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(300))
                        .maxBackoff(Duration.ofSeconds(3))
                        .jitter(0.3)
                        .filter(this::canRetry)
                        .doBeforeRetry(retrySignal -> log.warn(
                                "远程用户接口准备重试，userId：{}，retryCount：{}，error：{}",
                                userId,
                                retrySignal.totalRetries() + 1,
                                retrySignal.failure().getMessage()
                        ))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new RemoteCallException(500, "远程用户接口重试耗尽", retrySignal.failure().getMessage())
                        ))
                .doOnSuccess(user -> log.info("远程用户接口调用成功，userId：{}", userId))
                .doOnError(ex -> log.error("远程用户接口调用失败，userId：{}，error：{}", userId, ex.getMessage()))
                .block();
    }

    /**
     * 判断异常是否允许重试
     *
     * @param throwable 异常
     * @return 是否允许重试
     */
    private boolean canRetry(Throwable throwable) {
        Throwable actual = Exceptions.unwrap(throwable);

        if (actual instanceof TimeoutException) {
            return true;
        }

        if (actual instanceof RemoteCallException remoteCallException) {
            Integer statusCode = remoteCallException.getStatusCode();
            return statusCode != null && (statusCode == 502 || statusCode == 503 || statusCode == 504);
        }

        return false;
    }
}
```

重试策略建议如下：

| 场景            | 是否建议重试         | 说明                             |
| --------------- | -------------------- | -------------------------------- |
| GET 查询接口    | 可以重试             | 一般幂等，适合处理临时故障       |
| PUT 幂等更新    | 谨慎重试             | 需要确认接口幂等语义             |
| DELETE 幂等删除 | 谨慎重试             | 需要确认重复删除行为             |
| POST 创建订单   | 不建议盲目重试       | 可能重复创建业务数据             |
| 支付扣款接口    | 不建议客户端直接重试 | 应依赖幂等号和服务端状态查询     |
| 文件上传        | 谨慎重试             | 需要确认服务端是否支持断点或幂等 |

如果需要对所有请求统一设置连接超时和响应超时，优先在全局 WebClient 配置中的 Reactor Netty `HttpClient` 上设置；如果某个接口有特殊耗时，例如文件下载、报表导出、AI 生成任务，可以单独在调用链上增加 `timeout(Duration)` 或定义专用 WebClient。



## 测试与验证

本节介绍 WebClient 相关代码的测试方式。WebClient 调用外部接口时，不建议在单元测试中直接访问真实第三方服务，否则测试结果会受到网络、外部服务状态、测试数据和接口限流影响。更推荐通过 Mock 外部接口的方式验证请求路径、请求方法、请求头、请求体、响应解析和异常处理逻辑。

### 单元测试方式

WebClient 单元测试的重点不是测试 WebClient 本身，而是测试业务封装是否正确。例如：请求路径是否正确、查询参数是否正确、请求头是否正确、响应体是否可以正确反序列化、异常状态码是否可以正确转换为业务异常。

测试依赖可以在 `pom.xml` 中补充 MockWebServer。它可以在本地启动一个轻量级 HTTP 服务，用于模拟外部接口。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Test：提供 JUnit 5、AssertJ、Spring Test 等测试能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Reactor Test：用于测试 Mono、Flux 响应式流程 -->
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- MockWebServer：本地模拟外部 HTTP 服务 -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>mockwebserver</artifactId>
        <version>4.12.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

下面的测试类用于验证 WebClient 调用 GET 接口时，是否能够正确解析响应结果。

文件位置：`src/test/java/io/github/atengk/webclient/service/RemoteUserServiceTest.java`

```java
package io.github.atengk.webclient.service;

import io.github.atengk.webclient.vo.RemoteUserVO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 远程用户接口调用服务测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class RemoteUserServiceTest {

    private static MockWebServer mockWebServer;

    private static RemoteUserService remoteUserService;

    /**
     * 初始化 Mock 服务
     *
     * @throws IOException IO异常
     */
    @BeforeAll
    static void beforeAll() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        remoteUserService = new RemoteUserService(webClient);
    }

    /**
     * 关闭 Mock 服务
     *
     * @throws IOException IO异常
     */
    @AfterAll
    static void afterAll() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * 测试根据用户编号查询用户信息
     *
     * @throws InterruptedException 中断异常
     */
    @Test
    void testGetUserById() throws InterruptedException {
        String responseBody = """
                {
                  "userId": 1001,
                  "username": "admin",
                  "nickname": "管理员",
                  "phone": "13800000000",
                  "createTime": "2026-05-07T10:00:00"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        RemoteUserVO userVO = remoteUserService.getUserById(1001L);

        Assertions.assertNotNull(userVO);
        Assertions.assertEquals(1001L, userVO.getUserId());
        Assertions.assertEquals("admin", userVO.getUsername());
        Assertions.assertEquals("管理员", userVO.getNickname());

        RecordedRequest recordedRequest = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
        Assertions.assertNotNull(recordedRequest);
        Assertions.assertEquals("GET", recordedRequest.getMethod());
        Assertions.assertEquals("/users/1001", recordedRequest.getPath());
    }
}
```

如果测试的是通用 HTTP 客户端，需要重点验证统一包装响应 `ApiResult<T>` 是否可以正确解包。

文件位置：`src/test/java/io/github/atengk/webclient/client/CommonHttpClientTest.java`

```java
package io.github.atengk.webclient.client;

import io.github.atengk.webclient.vo.ApiResult;
import io.github.atengk.webclient.vo.RemoteUserVO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 通用 HTTP 客户端测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class CommonHttpClientTest {

    private static MockWebServer mockWebServer;

    private static CommonHttpClient commonHttpClient;

    /**
     * 初始化 Mock 服务
     *
     * @throws IOException IO异常
     */
    @BeforeAll
    static void beforeAll() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        commonHttpClient = new CommonHttpClient(webClient);
    }

    /**
     * 关闭 Mock 服务
     *
     * @throws IOException IO异常
     */
    @AfterAll
    static void afterAll() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * 测试 GET 请求并解析泛型响应
     *
     * @throws InterruptedException 中断异常
     */
    @Test
    void testGetAndParseApiResult() throws InterruptedException {
        String responseBody = """
                {
                  "code": 200,
                  "message": "操作成功",
                  "data": {
                    "userId": 1001,
                    "username": "admin",
                    "nickname": "管理员"
                  }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        StepVerifier.create(commonHttpClient.get(
                        "/users",
                        Map.of("userId", 1001),
                        new ParameterizedTypeReference<ApiResult<RemoteUserVO>>() {
                        }
                ))
                .assertNext(userVO -> {
                    Assertions.assertEquals(1001L, userVO.getUserId());
                    Assertions.assertEquals("admin", userVO.getUsername());
                })
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest(3, TimeUnit.SECONDS);
        Assertions.assertNotNull(recordedRequest);
        Assertions.assertEquals("GET", recordedRequest.getMethod());
        Assertions.assertEquals("/users?userId=1001", recordedRequest.getPath());
    }
}
```

`StepVerifier` 适合测试 `Mono` 和 `Flux`。如果业务方法内部已经调用 `block()` 并返回普通对象，可以直接使用 JUnit 断言；如果业务方法返回 `Mono<T>` 或 `Flux<T>`，建议使用 `StepVerifier` 验证响应式流程。

### Mock 外部接口

Mock 外部接口的核心目标是让测试在本地可重复执行，不依赖真实第三方服务。Mock 时应覆盖成功响应、业务失败响应、HTTP 4xx、HTTP 5xx、超时、异常响应体格式错误等场景。

下面的测试类用于验证 HTTP 500 响应是否可以被转换为 `RemoteCallException`。

文件位置：`src/test/java/io/github/atengk/webclient/service/RemoteStatusServiceTest.java`

```java
package io.github.atengk.webclient.service;

import io.github.atengk.webclient.exception.RemoteCallException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

/**
 * 远程状态码处理服务测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class RemoteStatusServiceTest {

    private static MockWebServer mockWebServer;

    private static RemoteStatusService remoteStatusService;

    /**
     * 初始化 Mock 服务
     *
     * @throws IOException IO异常
     */
    @BeforeAll
    static void beforeAll() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        remoteStatusService = new RemoteStatusService(webClient);
    }

    /**
     * 关闭 Mock 服务
     *
     * @throws IOException IO异常
     */
    @AfterAll
    static void afterAll() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * 测试 5xx 响应转换为远程调用异常
     */
    @Test
    void testServerError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "code": 50001,
                          "message": "远程服务内部错误",
                          "traceId": "trace-1001"
                        }
                        """));

        RemoteCallException exception = Assertions.assertThrows(
                RemoteCallException.class,
                () -> remoteStatusService.getMessageById(1001L)
        );

        Assertions.assertEquals(500, exception.getStatusCode());
        Assertions.assertTrue(exception.getMessage().contains("远程服务内部错误")
                || exception.getMessage().contains("远程接口服务端错误"));
    }
}
```

下面的测试类用于模拟远程接口响应超时。

文件位置：`src/test/java/io/github/atengk/webclient/service/RetryRemoteServiceTest.java`

```java
package io.github.atengk.webclient.service;

import io.github.atengk.webclient.exception.RemoteCallException;
import io.netty.channel.ChannelOption;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 远程重试服务测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
class RetryRemoteServiceTest {

    private static MockWebServer mockWebServer;

    private static RetryRemoteService retryRemoteService;

    /**
     * 初始化 Mock 服务
     *
     * @throws IOException IO异常
     */
    @BeforeAll
    static void beforeAll() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .responseTimeout(Duration.ofMillis(500));

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        retryRemoteService = new RetryRemoteService(webClient);
    }

    /**
     * 关闭 Mock 服务
     *
     * @throws IOException IO异常
     */
    @AfterAll
    static void afterAll() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * 测试远程接口超时
     */
    @Test
    void testTimeout() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "userId": 1001,
                          "username": "admin"
                        }
                        """)
                .setBodyDelay(5, TimeUnit.SECONDS));

        RemoteCallException exception = Assertions.assertThrows(
                RemoteCallException.class,
                () -> retryRemoteService.getUserWithRetry(1001L)
        );

        Assertions.assertNotNull(exception);
    }
}
```

Mock 外部接口时，建议至少覆盖以下场景：

| 场景            | 验证点                                |
| --------------- | ------------------------------------- |
| 2xx 成功响应    | 响应体是否正确反序列化                |
| 4xx 客户端错误  | 是否转换为统一业务异常                |
| 5xx 服务端错误  | 是否记录日志并抛出异常                |
| 响应体为空      | 是否有默认异常信息                    |
| 响应体不是 JSON | 是否可以兜底处理                      |
| 接口超时        | 是否触发超时异常或重试                |
| Header 校验     | Token、TenantId、TraceId 是否正确传递 |
| 请求体校验      | JSON、表单、multipart 是否按预期发送  |

### 接口调用验证

接口调用验证用于确认应用启动后 WebClient 能否按预期访问外部服务。验证方式可以分为本地 Mock 验证、测试环境验证和生产环境灰度验证。开发阶段建议先使用本地 Mock 或测试环境接口，不要直接依赖生产接口。

如果项目中已经提供 Controller 包装 WebClient 调用，可以通过 `curl` 验证接口。

```bash
# 验证同步用户详情接口
curl -X GET "http://localhost:8080/sync/users/1001"

# 验证非阻塞消息详情接口
curl -X GET "http://localhost:8080/reactive/messages/1001"

# 验证 POST JSON 调用
curl -X POST "http://localhost:8080/reactive/messages" \
  -H "Content-Type: application/json" \
  -d '{"receiverId":10001,"title":"系统通知","content":"这是一条测试消息"}'

# 验证携带 Token 的接口
curl -X GET "http://localhost:8080/sync/users/1001/token" \
  -H "Authorization: Bearer mock-access-token" \
  -H "X-Tenant-Id: tenant-001" \
  -H "X-Trace-Id: trace-001"
```

如果需要验证 WebClient 是否真实发出了符合预期的请求，可以观察以下内容：

| 验证项      | 说明                                          |
| ----------- | --------------------------------------------- |
| 请求方法    | 是否为 GET、POST、PUT、DELETE                 |
| 请求 URL    | BaseUrl、路径变量、查询参数是否正确           |
| 请求 Header | Authorization、Content-Type、TraceId 是否正确 |
| 请求体      | JSON、表单、multipart 是否符合接口文档        |
| HTTP 状态码 | 2xx、4xx、5xx 是否按预期处理                  |
| 响应解析    | JSON 字段是否正确映射到 Java 对象             |
| 异常日志    | 是否包含接口名、状态码、TraceId、错误消息     |
| 超时表现    | 外部接口无响应时是否及时失败                  |

接口验证完成后，建议将关键调用纳入自动化测试或集成测试，避免后续改造 WebClient 配置、统一异常处理、Token 过滤器时引入回归问题。

## 开发注意事项

本节总结 WebClient 在 Spring Boot 3 项目中的常见开发问题。WebClient 虽然使用方便，但如果阻塞和非阻塞混用不当、超时缺失、连接池无控制、日志未脱敏，仍然可能引发线程阻塞、连接耗尽、接口雪崩或敏感信息泄露等问题。

### 阻塞与非阻塞混用问题

WebClient 支持非阻塞调用，也支持通过 `block()` 转为同步调用。问题不在于能不能使用 `block()`，而在于是否在正确的位置使用。传统 Spring MVC、定时任务、启动初始化任务中可以使用 `block()`；WebFlux Controller 或响应式链路中不应随意使用 `block()`。

错误示例：

```java
@GetMapping("/reactive/users/{userId}")
public Mono<RemoteUserVO> getUser(@PathVariable Long userId) {
    RemoteUserVO userVO = webClient.get()
            .uri("/users/{userId}", userId)
            .retrieve()
            .bodyToMono(RemoteUserVO.class)
            .block();

    return Mono.just(userVO);
}
```

上面的代码看起来返回了 `Mono<RemoteUserVO>`，但内部已经调用 `block()` 阻塞当前线程，破坏了响应式链路。

正确写法如下：

```java
@GetMapping("/reactive/users/{userId}")
public Mono<RemoteUserVO> getUser(@PathVariable Long userId) {
    return webClient.get()
            .uri("/users/{userId}", userId)
            .retrieve()
            .bodyToMono(RemoteUserVO.class);
}
```

如果是传统同步接口，可以使用 `block()`：

```java
@GetMapping("/sync/users/{userId}")
public RemoteUserVO getUserSync(@PathVariable Long userId) {
    return webClient.get()
            .uri("/users/{userId}", userId)
            .retrieve()
            .bodyToMono(RemoteUserVO.class)
            .block();
}
```

阻塞与非阻塞使用建议如下：

| 场景                       | 建议                          |
| -------------------------- | ----------------------------- |
| Spring MVC Controller      | 可以 `block()`                |
| 普通 Service 同步方法      | 可以 `block()`                |
| 定时任务                   | 可以 `block()`                |
| WebFlux Controller         | 不要 `block()`                |
| `Mono` / `Flux` 链式处理中 | 不要 `block()`                |
| 网关、BFF 高并发聚合接口   | 优先非阻塞                    |
| 老项目渐进迁移             | 可在边界层使用 `block()` 兼容 |

判断原则很简单：方法签名已经返回 `Mono` 或 `Flux` 时，通常不要在内部 `block()`；方法签名返回普通对象时，可以在明确配置超时的前提下使用 `block()`。

### 超时配置注意事项

远程 HTTP 调用必须配置超时。没有超时配置时，外部接口异常、网络抖动、DNS 异常或服务端无响应都可能导致请求长时间等待，最终造成线程堆积或连接资源耗尽。

常见超时类型如下：

| 超时类型     | 说明                         | 建议                |
| ------------ | ---------------------------- | ------------------- |
| 连接超时     | 建立 TCP 连接的最大等待时间  | 普通接口 2 到 5 秒  |
| 响应超时     | 请求发出后等待响应的最大时间 | 普通接口 5 到 30 秒 |
| 读超时       | 连接建立后读取数据的最大间隔 | 按接口类型配置      |
| 写超时       | 写入请求体的最大等待时间     | 文件上传可适当放大  |
| 单次调用超时 | 响应式链路上的 `timeout()`   | 特殊接口可单独设置  |

推荐在全局 WebClient 中设置基础超时。

```java
HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
        .responseTimeout(Duration.ofSeconds(5));

WebClient webClient = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
```

对于特殊接口，可以单独设置调用链超时。

```java
Mono<RemoteUserVO> result = webClient.get()
        .uri("/users/{userId}", userId)
        .retrieve()
        .bodyToMono(RemoteUserVO.class)
        .timeout(Duration.ofSeconds(3));
```

超时配置建议如下：

| 场景           | 建议                                 |
| -------------- | ------------------------------------ |
| 普通查询接口   | 连接 2 到 5 秒，响应 5 到 10 秒      |
| 复杂计算接口   | 响应时间按 SLA 设置                  |
| 文件下载       | 使用专用 WebClient，响应超时适当放大 |
| 文件上传       | 读写超时适当放大                     |
| 支付、订单接口 | 超时后不要盲目重试，应查询最终状态   |
| 内部服务调用   | 应结合网关、服务治理和熔断策略       |

不要只配置连接超时。连接成功但服务端迟迟不返回时，仍然需要响应超时或调用链超时来兜底。

### 连接池配置注意事项

WebClient 默认底层常用 Reactor Netty。高并发场景下，如果不关注连接池配置，可能出现连接数不足、请求排队、连接长时间占用、外部服务被压垮等问题。连接池不是越大越好，应根据业务 QPS、外部接口响应时间、机器资源和下游服务承载能力综合设置。

可以通过 `ConnectionProvider` 自定义连接池。

```java
ConnectionProvider connectionProvider = ConnectionProvider.builder("webclient-pool")
        .maxConnections(200)
        .pendingAcquireMaxCount(500)
        .pendingAcquireTimeout(Duration.ofSeconds(3))
        .maxIdleTime(Duration.ofSeconds(30))
        .maxLifeTime(Duration.ofMinutes(5))
        .build();

HttpClient httpClient = HttpClient.create(connectionProvider)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
        .responseTimeout(Duration.ofSeconds(5));

WebClient webClient = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
```

需要引入以下类型：

```java
import io.netty.channel.ChannelOption;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
```

连接池参数说明如下：

| 参数                     | 说明                     |
| ------------------------ | ------------------------ |
| `maxConnections`         | 最大连接数               |
| `pendingAcquireMaxCount` | 等待获取连接的最大请求数 |
| `pendingAcquireTimeout`  | 等待获取连接的最大时间   |
| `maxIdleTime`            | 连接最大空闲时间         |
| `maxLifeTime`            | 连接最大存活时间         |

连接池配置建议如下：

| 场景             | 建议                              |
| ---------------- | --------------------------------- |
| 低并发管理后台   | 可以使用默认连接池                |
| 中高并发服务调用 | 建议显式配置连接池                |
| 多个第三方服务   | 建议按服务拆分 WebClient 和连接池 |
| 文件上传下载     | 建议使用独立连接池                |
| 慢接口           | 不要和普通接口共用过小连接池      |
| 下游服务能力有限 | 连接池要限制上限，避免压垮下游    |

连接池设置过小会导致请求排队，设置过大可能导致本机资源消耗过高，也可能对下游服务造成压力。生产环境应结合监控指标逐步调整，例如请求耗时、连接池等待时间、连接数、错误率、超时率等。

### 日志脱敏处理

WebClient 请求日志和异常日志中可能包含敏感信息，例如 Authorization、Cookie、Token、手机号、身份证号、银行卡号、密码、签名 URL、访问密钥等。生产环境中不能直接打印完整 Header、完整请求体或完整响应体。

常见敏感字段如下：

| 敏感字段 | 示例                                             |
| -------- | ------------------------------------------------ |
| Token    | `Authorization`、`access_token`、`refresh_token` |
| Cookie   | `Cookie`、`Set-Cookie`                           |
| 密码     | `password`、`oldPassword`、`newPassword`         |
| 手机号   | `phone`、`mobile`                                |
| 身份证号 | `idCard`、`identityNo`                           |
| 银行卡   | `bankCardNo`、`cardNo`                           |
| 密钥     | `secret`、`appSecret`、`privateKey`              |
| 签名     | `sign`、`signature`                              |
| 下载地址 | 带临时签名的 URL                                 |

文件位置：`src/main/java/io/github/atengk/webclient/util/LogDesensitizeUtil.java`

以下工具类用于对日志中的敏感信息做简单脱敏处理。

```java
package io.github.atengk.webclient.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 日志脱敏工具
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class LogDesensitizeUtil {

    /**
     * 脱敏 Token
     *
     * @param token Token
     * @return 脱敏后的 Token
     */
    public static String token(String token) {
        if (StrUtil.isBlank(token)) {
            return "";
        }

        if (token.length() <= 12) {
            return "******";
        }

        return StrUtil.subPre(token, 6) + "******" + StrUtil.subSuf(token, token.length() - 6);
    }

    /**
     * 脱敏手机号
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String phone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return "";
        }
        return DesensitizedUtil.mobilePhone(phone);
    }

    /**
     * 脱敏身份证号
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String idCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return "";
        }
        return DesensitizedUtil.idCardNum(idCard, 6, 4);
    }

    /**
     * 脱敏通用文本
     *
     * @param value 文本
     * @return 脱敏文本
     */
    public static String common(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }

        if (value.length() <= 4) {
            return "****";
        }

        return StrUtil.subPre(value, 2) + "****" + StrUtil.subSuf(value, value.length() - 2);
    }

    private LogDesensitizeUtil() {
    }
}
```

文件位置：`src/main/java/io/github/atengk/webclient/config/SafeWebClientLogFilter.java`

以下过滤器用于安全打印请求日志，不输出完整敏感 Header。

```java
package io.github.atengk.webclient.config;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.webclient.util.LogDesensitizeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 安全 WebClient 日志过滤器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class SafeWebClientLogFilter {

    /**
     * 创建安全请求日志过滤器
     *
     * @return 请求日志过滤器
     */
    public ExchangeFilterFunction safeRequestLogFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info("WebClient请求开始，method：{}，url：{}", request.method(), safeUrl(request.url().toString()));
            logSafeHeader(request, HttpHeaders.AUTHORIZATION);
            logSafeHeader(request, "X-Trace-Id");
            logSafeHeader(request, "X-Tenant-Id");
            return Mono.just(request);
        });
    }

    /**
     * 打印安全请求头
     *
     * @param request 请求对象
     * @param headerName 请求头名称
     */
    private void logSafeHeader(ClientRequest request, String headerName) {
        List<String> headerValueList = request.headers().get(headerName);
        if (CollUtil.isEmpty(headerValueList)) {
            return;
        }

        String headerValue = headerValueList.get(0);
        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)) {
            log.info("WebClient请求头，{}：{}", headerName, LogDesensitizeUtil.token(headerValue));
            return;
        }

        log.info("WebClient请求头，{}：{}", headerName, headerValue);
    }

    /**
     * 处理安全 URL
     *
     * @param url 请求地址
     * @return 安全 URL
     */
    private String safeUrl(String url) {
        if (url == null) {
            return "";
        }

        return url.replaceAll("(?i)(access_token=)[^&]+", "$1******")
                .replaceAll("(?i)(token=)[^&]+", "$1******")
                .replaceAll("(?i)(signature=)[^&]+", "$1******")
                .replaceAll("(?i)(sign=)[^&]+", "$1******");
    }
}
```

日志脱敏建议如下：

| 日志类型       | 建议                                         |
| -------------- | -------------------------------------------- |
| 请求 URL       | 可以打印，但查询参数中的 Token、签名要脱敏   |
| 请求 Header    | 白名单打印，Authorization、Cookie 不打印原文 |
| 请求体         | 默认不打印，必要时只在测试环境打印           |
| 响应体         | 默认不打印完整内容，异常时脱敏后打印摘要     |
| 文件下载 URL   | 带签名参数时必须脱敏                         |
| 第三方错误响应 | 记录状态码、错误码、错误摘要，不记录敏感字段 |

生产环境推荐使用“白名单字段打印”策略，而不是“黑名单字段过滤”策略。也就是说，只打印明确安全的字段，例如请求方法、路径、状态码、耗时、TraceId、业务接口名；对不确定是否安全的内容默认不打印。
