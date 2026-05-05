# Spring Cloud OpenFeign

## 技术概述

本节用于说明 OpenFeign 在 Spring Cloud 微服务体系中的定位、适合解决的问题，以及在 Spring Boot 3 项目中使用时需要注意的版本兼容关系。OpenFeign 的核心价值不是替代 HTTP 协议，而是把远程 HTTP 调用封装成 Java 接口调用，降低微服务之间调用代码的重复度和维护成本。

### OpenFeign 功能定位

OpenFeign 是 Spring Cloud 提供的声明式 HTTP 客户端组件，主要用于服务之间的 REST 接口调用。开发者只需要定义一个 Java 接口，并在接口上使用 `@FeignClient` 声明远程服务名称或远程地址，再通过 Spring MVC 注解描述 HTTP 方法、路径、参数和请求体，Spring Cloud OpenFeign 会在运行时为该接口生成代理对象，完成 HTTP 请求发送、响应反序列化、异常转换等工作。

官方文档明确说明，Feign 是声明式 Web Service 客户端，Spring Cloud OpenFeign 在此基础上增加了 Spring MVC 注解支持，并复用 Spring Web 默认使用的 `HttpMessageConverters` 进行编解码；同时可以与 Eureka、Spring Cloud CircuitBreaker、Spring Cloud LoadBalancer 集成，实现服务发现、负载均衡和熔断能力。([Home](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/))

OpenFeign 在项目中的常见定位如下：

| 定位                   | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 服务间 HTTP 调用客户端 | 将 `RestTemplate`、`WebClient` 中手写 URL、参数、Header、响应解析等代码封装为接口 |
| 微服务调用抽象层       | 调用方只依赖 Java 接口，不直接感知远程服务的具体 IP、端口和实例数量 |
| 统一调用配置入口       | 可集中配置超时时间、日志级别、编解码器、拦截器、错误解析和降级逻辑 |
| 服务发现与负载均衡入口 | 结合注册中心和 Spring Cloud LoadBalancer，通过服务名调用多个服务实例 |
| 可观测性接入点         | 可结合日志、Micrometer、链路追踪和网关请求头进行远程调用观测 |

典型调用方式如下：

```java
@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @GetMapping("/api/orders/{id}")
    OrderDetailVO getOrderDetail(@PathVariable("id") Long id);
}
```

这段接口的含义是：调用注册中心中服务名为 `order-service` 的服务，并请求其 `/api/orders/{id}` 接口。业务代码中可以像调用本地 Bean 一样调用 `OrderFeignClient#getOrderDetail`，OpenFeign 负责完成底层 HTTP 请求。

### 适用场景

OpenFeign 适用于微服务系统中“同步、短耗时、接口语义清晰”的远程 HTTP 调用场景。它不是消息队列，也不是 RPC 框架，更适合处理 REST 风格接口调用。

适合使用 OpenFeign 的场景包括：

| 场景                     | 说明                                                         |
| ------------------------ | ------------------------------------------------------------ |
| 微服务内部同步调用       | 例如订单服务调用用户服务查询用户信息，支付服务调用订单服务更新支付状态 |
| 后端服务聚合数据         | 例如 BFF 或聚合服务调用多个内部服务，组装前端页面需要的数据  |
| 统一封装第三方 HTTP 接口 | 例如对接短信、文件、支付、地图等外部 HTTP API                |
| 需要服务发现和负载均衡   | 调用方通过服务名访问多个提供方实例，而不是硬编码 IP 和端口   |
| 需要统一传递请求头       | 例如传递 Token、租户 ID、链路 ID、灰度标识、语言标识等公共 Header |
| 需要统一异常解析         | 将远程 HTTP 错误码、业务错误码转换为当前服务的统一异常模型   |

不建议使用 OpenFeign 的场景包括：

| 场景                 | 原因                                                     |
| -------------------- | -------------------------------------------------------- |
| 高吞吐异步解耦       | 更适合使用 Kafka、RabbitMQ、RocketMQ 等消息中间件        |
| 长耗时任务调用       | Feign 调用本质上是同步阻塞 HTTP 调用，长耗时容易占用线程 |
| 强实时双向通信       | 更适合使用 WebSocket、gRPC streaming 等方式              |
| 大文件上传下载主链路 | 可使用对象存储、预签名 URL 或专门的文件服务方案          |
| 频繁调用且强性能敏感 | 需要评估 HTTP 开销、连接池、序列化成本和调用链稳定性     |

在工程设计中，OpenFeign 应优先用于服务间的 API 调用，不应把它当作跨服务直接访问数据库的替代方案。远程服务接口应保持稳定、语义清晰，并通过 DTO/VO 屏蔽服务内部实体结构。

### Spring Boot 3 版本适配

Spring Boot 3 项目使用 OpenFeign 时，重点关注三类兼容关系：JDK 版本、Spring Boot 与 Spring Cloud 版本、OpenFeign 与底层 HTTP 客户端版本。

Spring Boot 3 最低要求 JDK 17，因此微服务项目、构建环境、Docker 镜像、CI/CD 环境都应统一使用 JDK 17 或更高版本。Spring Cloud 的版本必须与 Spring Boot 主版本严格匹配，不能随意混用。Spring Cloud 官方版本矩阵显示：Spring Cloud 2025.0.x 适配 Spring Boot 3.5.x，2024.0.x 适配 Spring Boot 3.4.x，2023.0.x 适配 Spring Boot 3.3.x/3.2.x，2022.0.x 适配 Spring Boot 3.0.x/3.1.x。([Home](https://spring.io/projects/spring-cloud/?utm_source=chatgpt.com))

推荐按下面的版本关系选择：

| Spring Boot 版本          | 推荐 Spring Cloud 版本 | 说明                                  |
| ------------------------- | ---------------------- | ------------------------------------- |
| Spring Boot 3.5.x         | Spring Cloud 2025.0.x  | 当前 Spring Boot 3 系列较新的常用组合 |
| Spring Boot 3.4.x         | Spring Cloud 2024.0.x  | 适合仍在 3.4 系列的项目               |
| Spring Boot 3.3.x / 3.2.x | Spring Cloud 2023.0.x  | 较多企业项目仍在使用的稳定组合        |
| Spring Boot 3.1.x / 3.0.x | Spring Cloud 2022.0.x  | 老项目维护可用，新项目不建议优先选择  |

Spring Cloud OpenFeign 4.x 对 Spring Boot 3 项目更友好，支持 AOT 和 Native Image 相关能力。官方文档说明，从 OpenFeign 4.x 开始，`@FeignClient` 注解属性默认会提前解析，这有利于 AOT 支持；如果在 Spring Cloud Contract 等测试场景中需要延迟解析，可以配置 `spring.cloud.openfeign.lazy-attributes-resolution=true`。([Home](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/))

还需要注意底层 HTTP 客户端变化：Spring Cloud OpenFeign 4 开始不再支持 Apache HttpClient 4，建议使用 Apache HttpClient 5；如果使用 HC5，只需要引入 HttpClient 5 相关依赖，必要时可通过 `spring.cloud.openfeign.httpclient.hc5.enabled=false` 禁用。([Home](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/))

## 环境准备

本节给出一个可直接落地的 Spring Boot 3 + Spring Cloud OpenFeign 项目基础配置。示例默认使用 Maven 构建，JDK 使用 17，服务注册发现以 Spring Cloud LoadBalancer 为基础，同时给出 Nacos 和 Eureka 两种常见注册中心配置方式。

### 项目基础版本

建议项目基础版本统一由父工程或依赖管理模块控制，避免多个微服务各自声明不同的 Spring Boot、Spring Cloud、OpenFeign、注册中心客户端版本。

推荐基础环境如下：

| 项目         | 推荐值                    | 说明                                        |
| ------------ | ------------------------- | ------------------------------------------- |
| JDK          | 17 或 21                  | Spring Boot 3 最低要求 JDK 17               |
| Maven        | 3.9.x                     | 推荐使用较新的 Maven 版本，减少依赖解析问题 |
| Spring Boot  | 3.5.x / 3.4.x / 3.3.x     | 根据项目稳定性和组织标准选择                |
| Spring Cloud | 与 Spring Boot 匹配       | 必须使用官方兼容矩阵中的版本                |
| OpenFeign    | 由 Spring Cloud BOM 管理  | 不建议手动指定 OpenFeign starter 版本       |
| 注册中心     | Nacos / Eureka / Consul   | 根据公司微服务基础设施选择                  |
| 负载均衡     | Spring Cloud LoadBalancer | Spring Cloud 新版本默认推荐方案             |

如果是新项目，建议优先选择如下组合：

```text
JDK 17
Spring Boot 3.5.x
Spring Cloud 2025.0.x
Maven 3.9.x
Spring Cloud OpenFeign 由 spring-cloud-dependencies BOM 管理
```

如果项目使用 Nacos，需要额外关注 Spring Cloud Alibaba 与 Spring Boot、Spring Cloud 的版本关系。Spring Cloud Alibaba 官方版本说明中，2023.x 分支用于适配 Spring Boot 3.2、Spring Cloud 2023.x 及以上版本，具体项目应以团队统一版本矩阵为准。([Spring Cloud Alibaba](https://sca.aliyun.com/docs/2023/overview/version-explain/?utm_source=chatgpt.com))

### Maven 依赖配置

Maven 依赖建议统一使用 `spring-boot-starter-parent` 管理 Spring Boot 依赖版本，再通过 `spring-cloud-dependencies` BOM 管理 Spring Cloud 组件版本。OpenFeign starter 不需要单独写版本号，由 Spring Cloud BOM 统一控制。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3 父工程，统一管理 Spring Boot 官方依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.0</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-cloud-openfeign-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-cloud-openfeign-demo</name>
    <description>Spring Boot 3 Spring Cloud OpenFeign 示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 JDK 17 -->
        <java.version>17</java.version>

        <!-- Spring Cloud 版本需要与 Spring Boot 版本匹配 -->
        <spring-cloud.version>2025.0.0</spring-cloud.version>

        <!-- Hutool 工具类，业务参数处理、字符串判断、集合处理时可使用 -->
        <hutool.version>5.8.36</hutool.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud BOM，统一管理 OpenFeign、LoadBalancer、Eureka 等组件版本 -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Web 基础依赖，提供 Spring MVC、JSON 序列化、内嵌 Servlet 容器 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- OpenFeign 声明式 HTTP 客户端 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Spring Cloud 负载均衡组件，基于服务名调用时建议显式引入 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>

        <!-- Actuator 监控端点，便于健康检查、服务治理和可观测性接入 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Hutool 工具类库，便于处理字符串、集合、JSON、日期等通用逻辑 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok，减少 DTO、VO、配置类中的样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 单元测试与接口测试基础依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件，用于打包可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

如果使用 Eureka 作为注册中心，额外加入：

```xml
<!-- Eureka 客户端，用于将当前服务注册到 Eureka，并从 Eureka 发现其他服务 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

如果使用 Nacos 作为注册中心，额外加入 Spring Cloud Alibaba Nacos Discovery 依赖。注意：Nacos 相关依赖必须与当前 Spring Boot、Spring Cloud 版本匹配，不建议只复制依赖而不校验版本矩阵。

```xml
<!-- Nacos 服务发现客户端，版本由 Spring Cloud Alibaba BOM 管理时不需要单独指定 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

Nacos 项目通常还需要引入 Spring Cloud Alibaba BOM，版本需要按团队统一规范选择：

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Cloud BOM -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Spring Cloud Alibaba BOM，管理 Nacos、Sentinel、Seata 等 Alibaba 生态组件版本 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 服务注册与发现配置

OpenFeign 支持两种常见调用模式：一种是通过固定 URL 调用，另一种是通过服务名调用。微服务内部调用通常使用服务名调用，即 `@FeignClient(name = "order-service")`，由注册中心提供服务实例列表，再由 Spring Cloud LoadBalancer 选择具体实例。

官方文档说明，当 Spring Cloud LoadBalancer 在 classpath 中时，Feign 会使用 `FeignBlockingLoadBalancerClient`；`spring-cloud-starter-openfeign` 支持 `spring-cloud-starter-loadbalancer`，但 LoadBalancer 是可选依赖，因此基于服务名调用时建议显式引入。([Home](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/))

#### 方式一：使用 Nacos 注册中心

Nacos 适合国内 Spring Cloud 微服务项目中作为注册中心和配置中心。使用 Nacos 时，需要启动 Nacos Server，并在每个微服务中配置服务名和 Nacos 地址。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 当前服务端口
  port: 8081

spring:
  application:
    # 当前服务注册到 Nacos 的服务名，Feign 调用时会使用该名称
    name: user-service

  cloud:
    nacos:
      discovery:
        # Nacos 服务端地址，生产环境建议配置为内网域名或集群地址
        server-addr: 127.0.0.1:8848

        # 命名空间，用于区分开发、测试、生产等环境
        namespace: public

        # 分组，用于进一步隔离不同业务线或项目
        group: DEFAULT_GROUP

        # 是否启用服务注册
        enabled: true

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查端点，便于注册中心或监控系统探活
        include: health,info
```

消费方通过服务名声明 Feign 客户端：

```java
@FeignClient(name = "user-service", contextId = "userFeignClient")
public interface UserFeignClient {

    @GetMapping("/api/users/{id}")
    UserDetailVO getUserDetail(@PathVariable("id") Long id);
}
```

这里的 `name = "user-service"` 必须与服务提供方的 `spring.application.name` 保持一致。`contextId` 用于区分同一个服务下的多个 Feign Client，避免 Bean 名称冲突。

#### 方式二：使用 Eureka 注册中心

Eureka 是 Spring Cloud Netflix 体系中常见的注册中心，适合已有 Eureka 基础设施的项目。使用 Eureka 时，服务提供方和调用方都需要注册到 Eureka Server。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 当前服务端口
  port: 8081

spring:
  application:
    # 当前服务注册到 Eureka 的服务名
    name: user-service

eureka:
  client:
    # 是否将当前服务注册到 Eureka
    register-with-eureka: true

    # 是否从 Eureka 拉取服务注册表
    fetch-registry: true

    service-url:
      # Eureka Server 地址，生产环境通常配置为高可用集群地址
      defaultZone: http://127.0.0.1:8761/eureka/

  instance:
    # 优先使用 IP 地址进行服务注册，便于容器或内网环境调用
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        # 暴露基础监控端点
        include: health,info
```

消费方 Feign Client 写法与 Nacos 场景一致：

```java
@FeignClient(name = "user-service", contextId = "userFeignClient")
public interface UserFeignClient {

    @GetMapping("/api/users/{id}")
    UserDetailVO getUserDetail(@PathVariable("id") Long id);
}
```

#### 方式三：不接注册中心，使用固定 URL

如果只是本地调试、调用第三方 HTTP API，或者当前项目还没有接入注册中心，可以先使用固定 URL。此方式不会走服务发现，适合外部接口或简单验证。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

remote:
  user-service:
    # 固定远程地址，本地联调时可以指向另一个 Spring Boot 服务
    url: http://127.0.0.1:8081
```

Feign Client 示例：

```java
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        url = "${remote.user-service.url}"
)
public interface UserFeignClient {

    @GetMapping("/api/users/{id}")
    UserDetailVO getUserDetail(@PathVariable("id") Long id);
}
```

固定 URL 模式下，`name` 仍然建议保留。Spring Cloud OpenFeign 新版本中 `name` 是必要属性，同时它也会作为配置项中的客户端名称使用。

#### 基础验证方式

完成依赖和配置后，可以通过以下步骤验证服务注册与 Feign 调用链路是否正常：

```bash
# 1. 启动注册中心，例如 Nacos 或 Eureka
# 2. 启动服务提供方 user-service
# 3. 启动服务消费方 order-service 或 gateway-service

# 查看当前服务健康状态
curl http://127.0.0.1:8081/actuator/health

# 调用消费方接口，由消费方内部通过 Feign 调用 user-service
curl http://127.0.0.1:8080/api/orders/1001
```

验证时重点检查三项：第一，服务提供方是否成功注册到注册中心；第二，消费方是否能通过服务名找到服务实例；第三，Feign 调用失败时日志中是否包含明确的连接失败、超时、404、反序列化失败或注册中心无实例等错误信息。



## OpenFeign 基础接入

本节给出 OpenFeign 的最小可用接入方式，包括启动类开启 Feign 扫描、定义远程服务接口，以及通过服务名或固定 URL 配置服务调用地址。Spring Cloud OpenFeign 官方示例中，启用 Feign 需要在 Spring Boot 启动类上使用 `@EnableFeignClients`，远程接口通过 `@FeignClient` 声明，接口方法可以使用 Spring MVC 注解描述请求方法、路径和参数。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

### 启用 Feign 客户端

启用 Feign 客户端需要在消费方服务的启动类上添加 `@EnableFeignClients`。如果 Feign 接口与启动类不在同一个基础包下，建议通过 `basePackages` 明确指定扫描路径，避免启动后找不到 Feign Client Bean。

示例项目中假设存在两个服务：

| 服务            | 端口 | 说明                                           |
| --------------- | ---- | ---------------------------------------------- |
| `user-service`  | 8081 | 服务提供方，提供用户查询和创建接口             |
| `order-service` | 8080 | 服务消费方，通过 OpenFeign 调用 `user-service` |

推荐目录结构如下：

```text
spring-cloud-openfeign-demo
├── user-service
│   └── src/main/java/io/github/atengk/user
│       ├── UserServiceApplication.java
│       ├── controller/UserController.java
│       ├── dto/UserCreateDTO.java
│       ├── dto/UserQueryDTO.java
│       └── vo/UserDetailVO.java
└── order-service
    └── src/main/java/io/github/atengk/order
        ├── OrderServiceApplication.java
        ├── client/UserFeignClient.java
        ├── controller/OrderRemoteTestController.java
        ├── dto/UserCreateDTO.java
        ├── dto/UserQueryDTO.java
        └── vo/UserDetailVO.java
```

生产项目中，`DTO`、`VO` 和 Feign 接口可以放在独立的 `api` 模块中，例如 `user-api`。这样消费方不需要重复定义请求和响应对象，也能减少接口字段不一致的问题。

文件位置：`order-service/src/main/java/io/github/atengk/order/OrderServiceApplication.java`

下面的启动类用于启动消费方服务，并开启 Feign Client 扫描。

```java
package io.github.atengk.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@EnableFeignClients(basePackages = "io.github.atengk.order.client")
@SpringBootApplication
public class OrderServiceApplication {

    /**
     * 启动订单服务
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
```

`@EnableFeignClients` 常用配置如下：

| 配置                   | 说明                                                |
| ---------------------- | --------------------------------------------------- |
| `basePackages`         | 指定 Feign 接口所在包，适合多模块项目               |
| `clients`              | 精确指定需要注册的 Feign Client 接口                |
| `defaultConfiguration` | 指定全局 Feign 配置类，例如日志、拦截器、编解码器等 |

如果 Feign 接口放在启动类同级包或子包中，可以直接写：

```java
@EnableFeignClients
@SpringBootApplication
public class OrderServiceApplication {
}
```

如果是多模块项目，建议显式指定扫描包。官方文档也说明，在配置类上使用 `@EnableFeignClients` 时，应指定客户端所在包或显式列出客户端类。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

### 定义远程服务接口

定义 Feign 接口时，需要使用 `@FeignClient` 声明远程服务。接口方法上继续使用 `@GetMapping`、`@PostMapping`、`@PathVariable`、`@RequestParam`、`@RequestBody`、`@RequestHeader` 等 Spring MVC 注解描述 HTTP 请求。

`@FeignClient` 中常用属性如下：

| 属性              | 说明                                                         |
| ----------------- | ------------------------------------------------------------ |
| `name`            | Feign 客户端名称；服务发现模式下通常等于服务提供方的 `spring.application.name` |
| `contextId`       | 当前 Feign Client 在 Spring 容器中的唯一上下文标识，建议显式配置 |
| `url`             | 固定远程地址；配置后不走服务发现和负载均衡                   |
| `path`            | 当前 Feign Client 的统一接口前缀                             |
| `configuration`   | 指定当前 Feign Client 的独立配置类                           |
| `fallback`        | 指定降级实现类                                               |
| `fallbackFactory` | 指定可获取异常原因的降级工厂                                 |

Spring Cloud OpenFeign 文档说明，`@FeignClient` 的 `name` 会用于创建 Spring Cloud LoadBalancer 客户端；也可以通过 `url` 属性指定固定地址。新版本中即使使用 `url`，也应保留 `name` 属性。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

文件位置：`order-service/src/main/java/io/github/atengk/order/client/UserFeignClient.java`

下面的 Feign 接口用于在 `order-service` 中远程调用 `user-service` 的用户接口。

```java
package io.github.atengk.order.client;

import io.github.atengk.order.dto.UserCreateDTO;
import io.github.atengk.order.dto.UserQueryDTO;
import io.github.atengk.order.vo.UserDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 用户服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        path = "/api/users"
)
public interface UserFeignClient {

    /**
     * 根据用户 ID 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    UserDetailVO getUserById(@PathVariable("id") Long id);

    /**
     * 根据查询条件查询用户详情
     *
     * @param queryDTO 用户查询参数
     * @return 用户详情
     */
    @GetMapping("/detail")
    UserDetailVO getUserDetail(@SpringQueryMap UserQueryDTO queryDTO);

    /**
     * 查询当前登录用户信息
     *
     * @param authorization 认证请求头
     * @param traceId       链路追踪 ID
     * @return 用户详情
     */
    @GetMapping("/current")
    UserDetailVO getCurrentUser(@RequestHeader("Authorization") String authorization,
                                @RequestHeader("X-Trace-Id") String traceId);

    /**
     * 创建用户
     *
     * @param createDTO 用户创建参数
     * @return 用户详情
     */
    @PostMapping
    UserDetailVO createUser(@RequestBody UserCreateDTO createDTO);

}
```

这里使用了 `@SpringQueryMap` 接收对象形式的查询参数。它适合 GET 请求参数较多的场景，可以避免在方法参数中写大量 `@RequestParam`。对于简单参数，也可以继续使用 `@RequestParam`。

示例中 `path = "/api/users"` 表示当前 Feign Client 下所有方法都使用该接口前缀。例如：

```java
@GetMapping("/{id}")
UserDetailVO getUserById(@PathVariable("id") Long id);
```

最终请求路径为：

```text
GET /api/users/{id}
```

### 配置服务调用地址

OpenFeign 支持两种常见调用地址配置方式：基于服务名调用和基于固定 URL 调用。基于服务名调用适合微服务内部调用，固定 URL 适合本地联调、测试环境直连或第三方 HTTP 接口调用。

#### 基于服务名调用

基于服务名调用时，`@FeignClient(name = "user-service")` 中的 `name` 必须与服务提供方的 `spring.application.name` 保持一致。此模式通常配合 Nacos、Eureka、Consul 等注册中心使用，并由 Spring Cloud LoadBalancer 选择具体服务实例。官方文档说明，当 Spring Cloud LoadBalancer 在 classpath 中时，OpenFeign 会使用负载均衡客户端进行调用。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

文件位置：`user-service/src/main/resources/application.yml`

```yaml
server:
  # 用户服务端口
  port: 8081

spring:
  application:
    # 服务提供方名称，Feign 调用时使用该服务名
    name: user-service

  cloud:
    nacos:
      discovery:
        # Nacos 注册中心地址
        server-addr: 127.0.0.1:8848
```

文件位置：`order-service/src/main/resources/application.yml`

```yaml
server:
  # 订单服务端口
  port: 8080

spring:
  application:
    # 服务消费方名称
    name: order-service

  cloud:
    nacos:
      discovery:
        # Nacos 注册中心地址
        server-addr: 127.0.0.1:8848
```

对应 Feign Client：

```java
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        path = "/api/users"
)
public interface UserFeignClient {
}
```

这种方式不会在代码中写死 IP 和端口。服务扩容、缩容、迁移后，只要注册中心中的实例信息正确，消费方通常不需要修改代码。

#### 基于固定 URL 调用

固定 URL 调用适合不接注册中心的场景，例如本地调试或调用第三方接口。OpenFeign 支持在 `@FeignClient` 的 `url` 属性中直接写地址，也支持通过配置文件占位符注入地址。官方文档说明，`name` 和 `url` 属性支持占位符配置。([Home](https://docs.spring.io/spring-cloud-openfeign/docs/4.0.6/reference/html/?utm_source=chatgpt.com))

文件位置：`order-service/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: order-service

remote:
  user-service:
    # 本地联调时直接指定用户服务地址
    url: http://127.0.0.1:8081
```

文件位置：`order-service/src/main/java/io/github/atengk/order/client/UserFeignClient.java`

```java
package io.github.atengk.order.client;

import io.github.atengk.order.dto.UserCreateDTO;
import io.github.atengk.order.dto.UserQueryDTO;
import io.github.atengk.order.vo.UserDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 用户服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        url = "${remote.user-service.url}",
        path = "/api/users"
)
public interface UserFeignClient {

    /**
     * 根据用户 ID 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    UserDetailVO getUserById(@PathVariable("id") Long id);

    /**
     * 根据查询条件查询用户详情
     *
     * @param queryDTO 用户查询参数
     * @return 用户详情
     */
    @GetMapping("/detail")
    UserDetailVO getUserDetail(@SpringQueryMap UserQueryDTO queryDTO);

    /**
     * 创建用户
     *
     * @param createDTO 用户创建参数
     * @return 用户详情
     */
    @PostMapping
    UserDetailVO createUser(@RequestBody UserCreateDTO createDTO);

}
```

固定 URL 模式下不会走注册中心和负载均衡。如果同一个接口在不同环境中地址不同，推荐将 `remote.user-service.url` 放到不同环境的配置文件中，例如 `application-dev.yml`、`application-test.yml`、`application-prod.yml`。

## 接口调用开发

本节给出 OpenFeign 常见接口调用写法，包括 GET 请求、POST 请求、普通参数、对象参数、路径参数和请求头传递。示例中会同时给出服务提供方接口和服务消费方调用代码，便于本地联调验证。

### GET 请求调用

GET 请求常用于查询数据。OpenFeign 中的 GET 请求写法与 Spring MVC Controller 基本一致，可以使用 `@GetMapping`、`@PathVariable`、`@RequestParam` 和 `@SpringQueryMap` 描述参数。

#### 服务提供方接口

文件位置：`user-service/src/main/java/io/github/atengk/user/controller/UserController.java`

下面的 Controller 提供用户查询接口，供消费方通过 OpenFeign 调用。

```java
package io.github.atengk.user.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.user.dto.UserCreateDTO;
import io.github.atengk.user.dto.UserQueryDTO;
import io.github.atengk.user.vo.UserDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 用户接口控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * 根据用户 ID 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public UserDetailVO getUserById(@PathVariable("id") Long id) {
        log.info("查询用户详情，用户ID：{}", id);

        return UserDetailVO.builder()
                .id(id)
                .username("ateng")
                .nickname("阿腾")
                .mobile("13800000000")
                .status(1)
                .source("path-variable")
                .build();
    }

    /**
     * 根据查询条件查询用户详情
     *
     * @param queryDTO 查询参数
     * @return 用户详情
     */
    @GetMapping("/detail")
    public UserDetailVO getUserDetail(UserQueryDTO queryDTO) {
        log.info("根据条件查询用户详情，查询参数：{}", queryDTO);

        String username = StrUtil.blankToDefault(queryDTO.getUsername(), "ateng");

        return UserDetailVO.builder()
                .id(queryDTO.getId())
                .username(username)
                .nickname("阿腾")
                .mobile(queryDTO.getMobile())
                .status(1)
                .source("query-object")
                .build();
    }

    /**
     * 查询当前登录用户
     *
     * @param authorization 认证请求头
     * @param traceId       链路追踪 ID
     * @return 用户详情
     */
    @GetMapping("/current")
    public UserDetailVO getCurrentUser(@RequestHeader("Authorization") String authorization,
                                       @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        log.info("查询当前用户，traceId：{}，authorization是否为空：{}", traceId, StrUtil.isBlank(authorization));

        return UserDetailVO.builder()
                .id(10001L)
                .username("current-user")
                .nickname("当前用户")
                .mobile("13900000000")
                .status(1)
                .source("request-header")
                .build();
    }

    /**
     * 创建用户
     *
     * @param createDTO 用户创建参数
     * @return 用户详情
     */
    @PostMapping
    public UserDetailVO createUser(@RequestBody UserCreateDTO createDTO) {
        log.info("创建用户，用户参数：{}", createDTO);

        return UserDetailVO.builder()
                .id(20001L)
                .username(createDTO.getUsername())
                .nickname(createDTO.getNickname())
                .mobile(createDTO.getMobile())
                .status(1)
                .source("request-body")
                .build();
    }

}
```

#### Feign 接口定义

文件位置：`order-service/src/main/java/io/github/atengk/order/client/UserFeignClient.java`

下面的接口中，`getUserById` 是典型路径参数 GET 请求，`getUserDetail` 是对象查询参数 GET 请求。

```java
package io.github.atengk.order.client;

import io.github.atengk.order.dto.UserCreateDTO;
import io.github.atengk.order.dto.UserQueryDTO;
import io.github.atengk.order.vo.UserDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 用户服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        path = "/api/users"
)
public interface UserFeignClient {

    /**
     * 根据用户 ID 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    UserDetailVO getUserById(@PathVariable("id") Long id);

    /**
     * 根据查询条件查询用户详情
     *
     * @param queryDTO 用户查询参数
     * @return 用户详情
     */
    @GetMapping("/detail")
    UserDetailVO getUserDetail(@SpringQueryMap UserQueryDTO queryDTO);

    /**
     * 查询当前登录用户信息
     *
     * @param authorization 认证请求头
     * @param traceId       链路追踪 ID
     * @return 用户详情
     */
    @GetMapping("/current")
    UserDetailVO getCurrentUser(@RequestHeader("Authorization") String authorization,
                                @RequestHeader("X-Trace-Id") String traceId);

    /**
     * 创建用户
     *
     * @param createDTO 用户创建参数
     * @return 用户详情
     */
    @PostMapping
    UserDetailVO createUser(@RequestBody UserCreateDTO createDTO);

}
```

### POST 请求调用

POST 请求常用于新增、修改、复杂查询等需要请求体的接口。OpenFeign 中使用 `@RequestBody` 传递 JSON 请求体，底层由 Spring Web 的 `HttpMessageConverters` 进行序列化和反序列化。Spring Cloud OpenFeign 默认集成 Spring MVC 注解，并复用 Spring Web 默认的消息转换器。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

#### DTO 定义

文件位置：`order-service/src/main/java/io/github/atengk/order/dto/UserCreateDTO.java`

下面的 DTO 用于创建用户请求体，消费方和提供方字段需要保持一致。

```java
package io.github.atengk.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户创建参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class UserCreateDTO implements Serializable {

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String mobile;

}
```

文件位置：`order-service/src/main/java/io/github/atengk/order/vo/UserDetailVO.java`

下面的 VO 用于接收远程服务返回的用户详情。

```java
package io.github.atengk.order.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户详情返回对象
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
public class UserDetailVO implements Serializable {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 用户状态
     */
    private Integer status;

    /**
     * 数据来源
     */
    private String source;

}
```

#### 消费方调用接口

文件位置：`order-service/src/main/java/io/github/atengk/order/controller/OrderRemoteTestController.java`

下面的 Controller 用于在订单服务中测试 Feign 远程调用。

```java
package io.github.atengk.order.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.order.client.UserFeignClient;
import io.github.atengk.order.dto.UserCreateDTO;
import io.github.atengk.order.dto.UserQueryDTO;
import io.github.atengk.order.vo.UserDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单服务远程调用测试控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order/remote/users")
public class OrderRemoteTestController {

    private final UserFeignClient userFeignClient;

    /**
     * 根据用户 ID 远程查询用户
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public UserDetailVO getUserById(@PathVariable("id") Long id) {
        log.info("订单服务开始远程查询用户，用户ID：{}", id);
        UserDetailVO userDetailVO = userFeignClient.getUserById(id);
        log.info("订单服务远程查询用户完成，用户ID：{}，用户名：{}", id, userDetailVO.getUsername());
        return userDetailVO;
    }

    /**
     * 根据查询条件远程查询用户
     *
     * @param queryDTO 查询参数
     * @return 用户详情
     */
    @GetMapping("/detail")
    public UserDetailVO getUserDetail(UserQueryDTO queryDTO) {
        log.info("订单服务开始按条件远程查询用户，查询参数：{}", queryDTO);
        return userFeignClient.getUserDetail(queryDTO);
    }

    /**
     * 远程查询当前登录用户
     *
     * @param authorization 认证请求头
     * @return 用户详情
     */
    @GetMapping("/current")
    public UserDetailVO getCurrentUser(@RequestHeader("Authorization") String authorization) {
        String traceId = IdUtil.fastSimpleUUID();
        log.info("订单服务开始远程查询当前用户，traceId：{}", traceId);

        if (StrUtil.isBlank(authorization)) {
            log.warn("认证请求头为空，traceId：{}", traceId);
        }

        return userFeignClient.getCurrentUser(authorization, traceId);
    }

    /**
     * 远程创建用户
     *
     * @param createDTO 用户创建参数
     * @return 用户详情
     */
    @PostMapping
    public UserDetailVO createUser(@RequestBody UserCreateDTO createDTO) {
        log.info("订单服务开始远程创建用户，用户名：{}", createDTO.getUsername());
        return userFeignClient.createUser(createDTO);
    }

}
```

这里使用 Hutool 的 `IdUtil.fastSimpleUUID()` 生成链路 ID，使用 `StrUtil.isBlank()` 判断请求头是否为空。实际生产项目中，链路 ID 通常由网关、链路追踪组件或统一拦截器生成，不建议每个业务接口手动生成。

### 请求参数传递

OpenFeign 中的请求参数主要有三类：路径参数、查询参数和请求体参数。参数注解应与服务提供方 Controller 保持一致，否则容易出现 404、参数为空、请求体无法解析等问题。

#### 路径参数

路径参数用于 URL 路径中的变量，例如根据用户 ID 查询用户。

服务提供方：

```java
@GetMapping("/{id}")
public UserDetailVO getUserById(@PathVariable("id") Long id) {
    return userService.getUserById(id);
}
```

Feign 客户端：

```java
@GetMapping("/{id}")
UserDetailVO getUserById(@PathVariable("id") Long id);
```

调用示例：

```bash
curl http://127.0.0.1:8080/api/order/remote/users/10001
```

最终请求链路如下：

```text
客户端
  -> order-service:8080/api/order/remote/users/10001
  -> user-service:8081/api/users/10001
```

#### 查询参数

查询参数用于 GET 请求中的 URL 参数，例如：

```text
GET /api/users/detail?id=10001&username=ateng&mobile=13800000000
```

文件位置：`order-service/src/main/java/io/github/atengk/order/dto/UserQueryDTO.java`

下面的 DTO 用于承载 GET 查询参数。

```java
package io.github.atengk.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户查询参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class UserQueryDTO implements Serializable {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String mobile;

}
```

Feign 客户端使用 `@SpringQueryMap`：

```java
@GetMapping("/detail")
UserDetailVO getUserDetail(@SpringQueryMap UserQueryDTO queryDTO);
```

消费方测试接口：

```java
@GetMapping("/detail")
public UserDetailVO getUserDetail(UserQueryDTO queryDTO) {
    log.info("订单服务开始按条件远程查询用户，查询参数：{}", queryDTO);
    return userFeignClient.getUserDetail(queryDTO);
}
```

调用示例：

```bash
curl "http://127.0.0.1:8080/api/order/remote/users/detail?id=10001&username=ateng&mobile=13800000000"
```

如果参数较少，也可以直接使用 `@RequestParam`：

```java
@GetMapping("/detail")
UserDetailVO getUserDetail(@RequestParam("id") Long id,
                           @RequestParam("username") String username,
                           @RequestParam("mobile") String mobile);
```

但当查询参数超过 3 个时，建议使用对象承载参数，便于后续扩展和维护。

#### 请求体参数

请求体参数常用于 POST、PUT、PATCH 请求，通常使用 JSON 格式传递。

Feign 客户端：

```java
@PostMapping
UserDetailVO createUser(@RequestBody UserCreateDTO createDTO);
```

服务提供方：

```java
@PostMapping
public UserDetailVO createUser(@RequestBody UserCreateDTO createDTO) {
    log.info("创建用户，用户参数：{}", createDTO);
    return userService.createUser(createDTO);
}
```

调用示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/order/remote/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "mobile": "13800000000"
  }'
```

POST 请求常见注意事项：

| 问题                       | 处理方式                                             |
| -------------------------- | ---------------------------------------------------- |
| 服务端参数为 `null`        | 检查 Feign 方法参数是否添加 `@RequestBody`           |
| 返回反序列化失败           | 检查返回 JSON 字段类型是否与 VO 字段类型一致         |
| 415 Unsupported Media Type | 检查请求是否为 JSON，接口是否正确声明 `@RequestBody` |
| 400 Bad Request            | 检查必填字段、JSON 格式和字段类型                    |

### 请求头传递

请求头传递常用于认证、租户隔离、链路追踪、灰度发布和语言环境传递。OpenFeign 支持通过 `@RequestHeader` 在接口方法参数中显式传递请求头，也可以通过 `RequestInterceptor` 做全局自动传递。

本节先给出显式传递方式，拦截器方式可以放到后续“拦截器使用”章节中展开。

#### 单个请求头传递

Feign 客户端：

```java
@GetMapping("/current")
UserDetailVO getCurrentUser(@RequestHeader("Authorization") String authorization,
                            @RequestHeader("X-Trace-Id") String traceId);
```

服务提供方：

```java
@GetMapping("/current")
public UserDetailVO getCurrentUser(@RequestHeader("Authorization") String authorization,
                                   @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
    log.info("查询当前用户，traceId：{}，authorization是否为空：{}", traceId, StrUtil.isBlank(authorization));
    return userService.getCurrentUser(authorization);
}
```

消费方调用：

```java
@GetMapping("/current")
public UserDetailVO getCurrentUser(@RequestHeader("Authorization") String authorization) {
    String traceId = IdUtil.fastSimpleUUID();
    log.info("订单服务开始远程查询当前用户，traceId：{}", traceId);
    return userFeignClient.getCurrentUser(authorization, traceId);
}
```

curl 验证：

```bash
curl "http://127.0.0.1:8080/api/order/remote/users/current" \
  -H "Authorization: Bearer test-token" \
  -H "X-Trace-Id: trace-10001"
```

#### 多个请求头传递

如果请求头数量较少，可以继续使用多个 `@RequestHeader` 参数：

```java
@GetMapping("/current")
UserDetailVO getCurrentUser(@RequestHeader("Authorization") String authorization,
                            @RequestHeader("X-Trace-Id") String traceId,
                            @RequestHeader("X-Tenant-Id") String tenantId,
                            @RequestHeader("Accept-Language") String language);
```

如果请求头数量较多，或者每个 Feign 接口都需要传递相同的认证和链路头，不建议在每个方法上重复声明。此时应使用 `RequestInterceptor` 统一处理，例如统一传递：

```text
Authorization
X-Trace-Id
X-Tenant-Id
X-User-Id
Accept-Language
```

#### 请求头传递注意事项

请求头传递时应重点关注以下问题：

| 问题               | 建议                                                         |
| ------------------ | ------------------------------------------------------------ |
| Token 丢失         | 检查消费方 Controller 是否接收到请求头，Feign 方法是否声明 `@RequestHeader` |
| 链路 ID 不一致     | 建议由网关或统一拦截器生成并透传，不要每层服务重复生成       |
| 多租户 ID 丢失     | 将租户 ID 纳入统一请求头规范，并在 Feign 拦截器中自动传递    |
| 请求头为空导致 400 | 服务提供方可将非核心 Header 设置为 `required = false`        |
| 日志泄露敏感信息   | 不要完整打印 Token、Cookie、密钥等敏感请求头                 |



## 配置管理

本节用于集中管理 OpenFeign 的运行参数，包括连接超时、读取超时、日志级别、编码器和解码器。Spring Cloud OpenFeign 支持通过 `spring.cloud.openfeign.client.config` 为默认客户端或指定 Feign Client 配置参数，例如 `connectTimeout`、`readTimeout`、`loggerLevel`、`errorDecoder`、`encoder`、`decoder` 等；如果同时存在 Java 配置类和配置文件，默认配置文件优先级更高。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

### 超时时间配置

OpenFeign 的超时时间主要分为连接超时和读取超时。连接超时用于限制建立 TCP 连接的等待时间，读取超时用于限制连接建立后等待服务端响应数据的时间。Spring Cloud OpenFeign 官方文档说明，`connectTimeout` 和 `readTimeout` 可以分别配置在默认客户端或指定客户端上。([Home](https://docs.spring.io/spring-cloud-openfeign/docs/3.1.3/reference/html/?utm_source=chatgpt.com))

文件位置：`order-service/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: order-service

  cloud:
    openfeign:
      client:
        config:
          # default 表示所有 Feign Client 的默认配置
          default:
            # 连接超时时间，单位毫秒
            connectTimeout: 3000

            # 读取超时时间，单位毫秒
            readTimeout: 5000

          # user-service 表示只对 name/contextId 为 user-service 的 Feign Client 生效
          user-service:
            # 用户服务连接超时时间
            connectTimeout: 2000

            # 用户服务读取超时时间
            readTimeout: 4000
```

如果项目中某个远程服务响应较慢，可以只为该服务单独配置超时时间，避免放大全局超时时间。例如报表服务、文件服务、第三方接口服务通常可以设置更长的 `readTimeout`，而用户、订单、权限等核心链路服务应保持较短超时，避免线程长时间阻塞。

也可以通过 Java 配置方式定义超时时间，但一般不建议这样做，因为配置文件更适合按环境调整。

文件位置：`order-service/src/main/java/io/github/atengk/order/config/UserFeignTimeoutConfiguration.java`

下面的配置类为指定 Feign Client 配置请求超时时间。

```java
package io.github.atengk.order.config;

import feign.Request;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * 用户服务 Feign 超时配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class UserFeignTimeoutConfiguration {

    /**
     * 配置用户服务 Feign 请求超时时间
     *
     * @return Feign 请求配置
     */
    @Bean
    public Request.Options userFeignRequestOptions() {
        return new Request.Options(
                2, TimeUnit.SECONDS,
                4, TimeUnit.SECONDS,
                true
        );
    }

}
```

在 Feign Client 上指定该配置：

```java
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        path = "/api/users",
        configuration = UserFeignTimeoutConfiguration.class
)
public interface UserFeignClient {
}
```

需要注意，Feign 专用配置类不建议被主启动类的 `@ComponentScan` 扫描到。如果该配置类被全局扫描，它可能变成所有 Feign Client 的默认配置。更稳妥的做法是把它放到启动类扫描路径之外，或者不要添加 `@Configuration` 注解，只通过 `@FeignClient(configuration = xxx.class)` 显式引用。Spring Cloud OpenFeign 官方文档也提醒，Feign 配置类如果被组件扫描到，可能会成为全局默认配置。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

### 日志级别配置

OpenFeign 日志分为两层配置：第一层是 Spring Boot 日志系统中 Feign 接口包或类的日志级别，必须设置为 `DEBUG`；第二层是 Feign 自身的 `Logger.Level`，用于控制输出内容范围。官方文档说明，Feign 为每个客户端创建日志器，默认日志器名称是 Feign 接口完整类名，并且 Feign 日志只响应 `DEBUG` 级别。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

Feign 日志级别说明如下：

| 级别      | 说明                                       |
| --------- | ------------------------------------------ |
| `NONE`    | 不输出 Feign 日志，默认值                  |
| `BASIC`   | 只输出请求方法、URL、响应状态码和执行时间  |
| `HEADERS` | 在 `BASIC` 基础上输出请求头和响应头        |
| `FULL`    | 输出请求头、请求体、响应头、响应体和元数据 |

文件位置：`order-service/src/main/resources/application.yml`

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            # 默认日志级别，生产环境建议使用 basic 或 none
            loggerLevel: basic

          user-service:
            # 单独为用户服务开启完整日志，适合本地调试和联调排查
            loggerLevel: full

logging:
  level:
    # Feign 日志必须将对应接口或包设置为 DEBUG，否则 loggerLevel 不会输出
    io.github.atengk.order.client.UserFeignClient: DEBUG

    # 也可以按包开启，适合本地联调；生产环境慎用
    io.github.atengk.order.client: DEBUG
```

如果希望通过 Java 配置方式设置日志级别，可以提供 `Logger.Level` Bean。

文件位置：`order-service/src/main/java/io/github/atengk/order/config/UserFeignLogConfiguration.java`

下面的配置类为指定 Feign Client 开启完整日志。

```java
package io.github.atengk.order.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;

/**
 * 用户服务 Feign 日志配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class UserFeignLogConfiguration {

    /**
     * 设置 Feign 日志输出级别
     *
     * @return Feign 日志级别
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

}
```

生产环境不建议长期使用 `FULL`，因为它可能输出请求体、响应体和请求头。如果请求中包含 `Authorization`、`Cookie`、手机号、身份证号、银行卡号等敏感信息，完整日志会带来安全风险。生产环境通常使用 `NONE` 或 `BASIC`，出现问题时再临时调整指定客户端的日志级别。

### 编码器与解码器配置

编码器用于将 Java 对象转换成 HTTP 请求体，解码器用于将 HTTP 响应体转换成 Java 对象。Spring Cloud OpenFeign 默认提供 `SpringEncoder` 和 `ResponseEntityDecoder`，后者内部包装 `SpringDecoder`；它会复用 Spring Web 的 `HttpMessageConverters`，因此默认即可处理 JSON 请求体和响应体。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

大多数 Spring Boot 3 项目不需要自定义编码器和解码器。只有在以下场景中才建议自定义：

| 场景                       | 说明                                                   |
| -------------------------- | ------------------------------------------------------ |
| 响应结构特殊               | 远程服务返回非标准 JSON，需要自定义解析                |
| 接口使用特殊媒体类型       | 例如 `application/octet-stream`、自定义 `Content-Type` |
| 需要统一包装响应           | 例如统一把远程响应解析成 `Result<T>` 后再处理          |
| 需要扩展消息转换器         | 例如定制 Jackson、日期格式、枚举序列化策略             |
| 需要处理第三方接口兼容问题 | 第三方接口返回字段类型不稳定或空字符串代替对象         |

推荐先使用默认编解码能力，只在确有需要时扩展。

文件位置：`order-service/src/main/java/io/github/atengk/order/config/FeignCodecConfiguration.java`

下面的配置类复用 Spring Boot 的 `HttpMessageConverters`，显式声明 Feign 编码器和解码器。

```java
package io.github.atengk.order.config;

import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;

/**
 * Feign 编码器与解码器配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class FeignCodecConfiguration {

    /**
     * 配置 Feign 编码器
     *
     * @param messageConverters Spring Boot 消息转换器
     * @return Feign 编码器
     */
    @Bean
    public Encoder feignEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringEncoder(messageConverters);
    }

    /**
     * 配置 Feign 解码器
     *
     * @param messageConverters Spring Boot 消息转换器
     * @return Feign 解码器
     */
    @Bean
    public Decoder feignDecoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new ResponseEntityDecoder(new SpringDecoder(messageConverters));
    }

}
```

在 Feign Client 中引用：

```java
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        path = "/api/users",
        configuration = FeignCodecConfiguration.class
)
public interface UserFeignClient {
}
```

如果项目只需要统一 JSON 日期格式、空值处理或枚举序列化，一般不建议直接替换 Feign 编解码器，而是优先配置 Jackson。

文件位置：`order-service/src/main/resources/application.yml`

```yaml
spring:
  jackson:
    # 统一 JSON 日期时间格式
    date-format: yyyy-MM-dd HH:mm:ss

    # 统一时区
    time-zone: Asia/Shanghai

    serialization:
      # 日期不输出为时间戳
      write-dates-as-timestamps: false

    default-property-inclusion: non_null
```

这种方式对 Controller 和 Feign 默认 JSON 编解码都更自然，维护成本也更低。

## 异常处理

本节用于处理 OpenFeign 调用失败后的异常识别、降级返回和错误响应解析。Feign 调用失败可能来自网络连接失败、读取超时、服务无实例、远程服务 4xx/5xx、JSON 反序列化失败，也可能来自业务错误码。实际项目中建议把这些异常统一转换成当前服务的业务异常，避免让 Controller 直接暴露 Feign 底层异常。

### Feign 调用异常

OpenFeign 常见异常可以分为调用前、调用中和调用后三类。

| 异常类型                             | 常见原因                       | 处理建议                                     |
| ------------------------------------ | ------------------------------ | -------------------------------------------- |
| `RetryableException`                 | 连接失败、读取超时、服务不可达 | 检查网络、服务实例、超时时间和重试策略       |
| `FeignException.NotFound`            | 远程接口返回 404               | 检查路径、HTTP 方法、服务版本                |
| `FeignException.BadRequest`          | 远程接口返回 400               | 检查参数、请求体、字段类型                   |
| `FeignException.InternalServerError` | 远程接口返回 500               | 查看服务提供方日志                           |
| `DecodeException`                    | 响应体无法反序列化             | 检查返回 JSON 与 VO 字段类型                 |
| `NoSuchBeanDefinitionException`      | Feign Client 未注册            | 检查 `@EnableFeignClients` 扫描路径          |
| `IllegalStateException`              | Feign 注解或参数配置错误       | 检查 `@PathVariable`、`@RequestParam` 参数名 |

调用方可以在业务层捕获 Feign 异常，并转换为当前服务统一异常。

文件位置：`order-service/src/main/java/io/github/atengk/order/exception/RemoteServiceException.java`

下面的异常类用于表示远程服务调用失败。

```java
package io.github.atengk.order.exception;

import lombok.Getter;

/**
 * 远程服务调用异常
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
public class RemoteServiceException extends RuntimeException {

    /**
     * 错误码
     */
    private final String code;

    /**
     * 创建远程服务调用异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public RemoteServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 创建远程服务调用异常
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     */
    public RemoteServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

}
```

文件位置：`order-service/src/main/java/io/github/atengk/order/service/OrderUserRemoteService.java`

下面的业务服务封装 Feign 调用，并对常见 Feign 异常进行转换。

```java
package io.github.atengk.order.service;

import feign.FeignException;
import feign.RetryableException;
import io.github.atengk.order.client.UserFeignClient;
import io.github.atengk.order.exception.RemoteServiceException;
import io.github.atengk.order.vo.UserDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单用户远程服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderUserRemoteService {

    private final UserFeignClient userFeignClient;

    /**
     * 根据用户 ID 查询用户详情
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    public UserDetailVO getUserDetail(Long userId) {
        try {
            log.info("开始调用用户服务查询用户详情，用户ID：{}", userId);
            return userFeignClient.getUserById(userId);
        } catch (RetryableException e) {
            log.error("调用用户服务超时或连接失败，用户ID：{}，异常信息：{}", userId, e.getMessage(), e);
            throw new RemoteServiceException("USER_SERVICE_UNAVAILABLE", "用户服务暂时不可用，请稍后重试", e);
        } catch (FeignException.NotFound e) {
            log.warn("调用用户服务未找到用户接口或资源，用户ID：{}，状态码：{}", userId, e.status());
            throw new RemoteServiceException("USER_NOT_FOUND", "用户不存在或用户服务接口不存在", e);
        } catch (FeignException e) {
            log.error("调用用户服务失败，用户ID：{}，状态码：{}，响应内容：{}", userId, e.status(), e.contentUTF8(), e);
            throw new RemoteServiceException("USER_SERVICE_ERROR", "用户服务调用失败", e);
        }
    }

}
```

这种方式适合需要在业务层感知异常并做不同处理的场景。如果项目希望对所有 Feign 错误响应做统一解析，建议使用 `ErrorDecoder`。

### 统一降级处理

统一降级处理用于在远程服务不可用、超时、熔断或异常时返回兜底结果，避免异常继续向上扩散。Spring Cloud OpenFeign 当前推荐结合 Spring Cloud CircuitBreaker 使用降级能力。官方文档说明，当 classpath 中存在 Spring Cloud CircuitBreaker 且配置 `spring.cloud.openfeign.circuitbreaker.enabled=true` 时，Feign 方法会被 CircuitBreaker 包装；要启用指定客户端的 fallback，需要在 `@FeignClient` 中配置 `fallback` 或 `fallbackFactory`，并将实现类声明为 Spring Bean。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/configprops.html?utm_source=chatgpt.com))

先加入熔断依赖。

文件位置：`order-service/pom.xml`

```xml
<!-- Spring Cloud CircuitBreaker + Resilience4j，用于 Feign 熔断和降级 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

开启 OpenFeign CircuitBreaker。

文件位置：`order-service/src/main/resources/application.yml`

```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        # 开启 Feign 熔断能力，fallback/fallbackFactory 才会生效
        enabled: true
```

#### fallback 方式

`fallback` 适合不关心具体异常原因，只需要返回固定兜底结果的场景。

文件位置：`order-service/src/main/java/io/github/atengk/order/client/UserFeignClient.java`

```java
package io.github.atengk.order.client;

import io.github.atengk.order.client.fallback.UserFeignFallback;
import io.github.atengk.order.dto.UserCreateDTO;
import io.github.atengk.order.dto.UserQueryDTO;
import io.github.atengk.order.vo.UserDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 用户服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        path = "/api/users",
        fallback = UserFeignFallback.class
)
public interface UserFeignClient {

    /**
     * 根据用户 ID 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    UserDetailVO getUserById(@PathVariable("id") Long id);

    /**
     * 根据查询条件查询用户详情
     *
     * @param queryDTO 查询参数
     * @return 用户详情
     */
    @GetMapping("/detail")
    UserDetailVO getUserDetail(@SpringQueryMap UserQueryDTO queryDTO);

    /**
     * 创建用户
     *
     * @param createDTO 创建参数
     * @return 用户详情
     */
    @PostMapping
    UserDetailVO createUser(@RequestBody UserCreateDTO createDTO);

}
```

文件位置：`order-service/src/main/java/io/github/atengk/order/client/fallback/UserFeignFallback.java`

下面的降级类用于在用户服务不可用时返回兜底用户信息。

```java
package io.github.atengk.order.client.fallback;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.order.client.UserFeignClient;
import io.github.atengk.order.dto.UserCreateDTO;
import io.github.atengk.order.dto.UserQueryDTO;
import io.github.atengk.order.vo.UserDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户服务 Feign 降级处理
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class UserFeignFallback implements UserFeignClient {

    /**
     * 用户详情查询降级
     *
     * @param id 用户 ID
     * @return 兜底用户详情
     */
    @Override
    public UserDetailVO getUserById(Long id) {
        log.warn("用户服务查询详情触发降级，用户ID：{}", id);
        return fallbackUser(id, "用户服务暂时不可用");
    }

    /**
     * 用户条件查询降级
     *
     * @param queryDTO 查询参数
     * @return 兜底用户详情
     */
    @Override
    public UserDetailVO getUserDetail(UserQueryDTO queryDTO) {
        Long userId = queryDTO == null ? null : queryDTO.getId();
        log.warn("用户服务条件查询触发降级，用户ID：{}", userId);
        return fallbackUser(userId, "用户服务查询失败");
    }

    /**
     * 用户创建降级
     *
     * @param createDTO 创建参数
     * @return 兜底用户详情
     */
    @Override
    public UserDetailVO createUser(UserCreateDTO createDTO) {
        String username = createDTO == null ? "unknown" : createDTO.getUsername();
        log.warn("用户服务创建用户触发降级，用户名：{}", username);

        return UserDetailVO.builder()
                .id(-1L)
                .username(StrUtil.blankToDefault(username, "unknown"))
                .nickname("降级用户")
                .status(0)
                .source("fallback")
                .build();
    }

    /**
     * 构建用户兜底结果
     *
     * @param id      用户 ID
     * @param message 降级消息
     * @return 兜底用户详情
     */
    private UserDetailVO fallbackUser(Long id, String message) {
        return UserDetailVO.builder()
                .id(id == null ? -1L : id)
                .username("fallback-user")
                .nickname(message)
                .status(0)
                .source("fallback")
                .build();
    }

}
```

#### fallbackFactory 方式

`fallbackFactory` 可以拿到触发降级的原始异常，更适合生产项目。推荐优先使用 `fallbackFactory`，因为它能根据超时、404、500、服务不可用等不同原因记录不同日志或返回不同提示。OpenFeign 官方文档也说明，如果需要访问触发 fallback 的原因，可以使用 `fallbackFactory`。([Home](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/?utm_source=chatgpt.com))

文件位置：`order-service/src/main/java/io/github/atengk/order/client/UserFeignClient.java`

```java
package io.github.atengk.order.client;

import io.github.atengk.order.client.fallback.UserFeignFallbackFactory;
import io.github.atengk.order.dto.UserCreateDTO;
import io.github.atengk.order.dto.UserQueryDTO;
import io.github.atengk.order.vo.UserDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 用户服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        path = "/api/users",
        fallbackFactory = UserFeignFallbackFactory.class
)
public interface UserFeignClient {

    /**
     * 根据用户 ID 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    UserDetailVO getUserById(@PathVariable("id") Long id);

    /**
     * 根据查询条件查询用户详情
     *
     * @param queryDTO 查询参数
     * @return 用户详情
     */
    @GetMapping("/detail")
    UserDetailVO getUserDetail(@SpringQueryMap UserQueryDTO queryDTO);

    /**
     * 创建用户
     *
     * @param createDTO 创建参数
     * @return 用户详情
     */
    @PostMapping
    UserDetailVO createUser(@RequestBody UserCreateDTO createDTO);

}
```

文件位置：`order-service/src/main/java/io/github/atengk/order/client/fallback/UserFeignFallbackFactory.java`

下面的降级工厂可以获取原始异常，并按异常原因记录日志。

```java
package io.github.atengk.order.client.fallback;

import feign.FeignException;
import io.github.atengk.order.client.UserFeignClient;
import io.github.atengk.order.dto.UserCreateDTO;
import io.github.atengk.order.dto.UserQueryDTO;
import io.github.atengk.order.vo.UserDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务 Feign 降级工厂
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {

    /**
     * 创建用户服务降级实例
     *
     * @param cause 降级原因
     * @return 用户服务 Feign 降级实例
     */
    @Override
    public UserFeignClient create(Throwable cause) {
        return new UserFeignClient() {

            /**
             * 根据用户 ID 查询用户详情降级
             *
             * @param id 用户 ID
             * @return 兜底用户详情
             */
            @Override
            public UserDetailVO getUserById(Long id) {
                logFallback("查询用户详情", cause);
                return buildFallbackUser(id, "用户服务暂时不可用");
            }

            /**
             * 根据查询条件查询用户详情降级
             *
             * @param queryDTO 查询参数
             * @return 兜底用户详情
             */
            @Override
            public UserDetailVO getUserDetail(UserQueryDTO queryDTO) {
                logFallback("条件查询用户", cause);
                Long userId = queryDTO == null ? -1L : queryDTO.getId();
                return buildFallbackUser(userId, "用户服务查询失败");
            }

            /**
             * 创建用户降级
             *
             * @param createDTO 创建参数
             * @return 兜底用户详情
             */
            @Override
            public UserDetailVO createUser(UserCreateDTO createDTO) {
                logFallback("创建用户", cause);
                String username = createDTO == null ? "fallback-user" : createDTO.getUsername();

                return UserDetailVO.builder()
                        .id(-1L)
                        .username(username)
                        .nickname("用户创建已降级")
                        .status(0)
                        .source("fallback-factory")
                        .build();
            }
        };
    }

    /**
     * 记录降级原因
     *
     * @param scene 降级场景
     * @param cause 异常原因
     */
    private void logFallback(String scene, Throwable cause) {
        if (cause instanceof FeignException feignException) {
            log.warn("用户服务调用触发降级，场景：{}，状态码：{}，响应内容：{}",
                    scene, feignException.status(), feignException.contentUTF8());
            return;
        }

        log.warn("用户服务调用触发降级，场景：{}，异常类型：{}，异常信息：{}",
                scene, cause.getClass().getSimpleName(), cause.getMessage());
    }

    /**
     * 构建兜底用户信息
     *
     * @param id      用户 ID
     * @param message 兜底消息
     * @return 用户详情
     */
    private UserDetailVO buildFallbackUser(Long id, String message) {
        return UserDetailVO.builder()
                .id(id == null ? -1L : id)
                .username("fallback-user")
                .nickname(message)
                .status(0)
                .source("fallback-factory")
                .build();
    }

}
```

降级不是异常吞噬机制。核心交易、支付、库存扣减、权限校验等强一致场景不应简单返回成功兜底结果，否则会掩盖真实失败。此类场景更适合抛出明确业务异常，并由上层接口返回失败响应。

### 错误信息解析

`ErrorDecoder` 用于处理远程服务返回的非 2xx HTTP 响应。默认情况下，Feign 会将 4xx、5xx 响应转换为 `FeignException`。如果远程服务有统一错误响应格式，可以通过自定义 `ErrorDecoder` 解析响应体，并转换为当前服务的统一异常。Spring Cloud OpenFeign 支持通过配置项指定 `errorDecoder`，也可以通过 Java Bean 方式提供自定义解码器。([Home](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/index.html?utm_source=chatgpt.com))

假设远程服务错误响应格式如下：

```json
{
  "code": "USER_NOT_FOUND",
  "message": "用户不存在",
  "data": null
}
```

先定义错误响应对象。

文件位置：`order-service/src/main/java/io/github/atengk/order/vo/RemoteErrorVO.java`

下面的对象用于承载远程服务返回的错误信息。

```java
package io.github.atengk.order.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 远程服务错误返回对象
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class RemoteErrorVO implements Serializable {

    /**
     * 错误码
     */
    private String code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 错误数据
     */
    private Object data;

}
```

文件位置：`order-service/src/main/java/io/github/atengk/order/config/FeignErrorDecoder.java`

下面的错误解码器用于读取远程响应体，并转换为 `RemoteServiceException`。

```java
package io.github.atengk.order.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import io.github.atengk.order.exception.RemoteServiceException;
import io.github.atengk.order.vo.RemoteErrorVO;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Feign 错误响应解码器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    /**
     * 解码 Feign 错误响应
     *
     * @param methodKey Feign 方法标识
     * @param response  HTTP 响应
     * @return 异常对象
     */
    @Override
    public Exception decode(String methodKey, Response response) {
        String responseBody = readResponseBody(response);

        log.warn("Feign 调用返回错误响应，methodKey：{}，status：{}，body：{}",
                methodKey, response.status(), responseBody);

        if (StrUtil.isBlank(responseBody)) {
            return defaultErrorDecoder.decode(methodKey, response);
        }

        try {
            RemoteErrorVO remoteErrorVO = JSONUtil.toBean(responseBody, RemoteErrorVO.class);
            String code = StrUtil.blankToDefault(remoteErrorVO.getCode(), "REMOTE_SERVICE_ERROR");
            String message = StrUtil.blankToDefault(remoteErrorVO.getMessage(), "远程服务调用失败");

            return new RemoteServiceException(code, message);
        } catch (Exception e) {
            log.warn("解析远程服务错误响应失败，methodKey：{}，status：{}，body：{}",
                    methodKey, response.status(), responseBody, e);
            return defaultErrorDecoder.decode(methodKey, response);
        }
    }

    /**
     * 读取响应体
     *
     * @param response HTTP 响应
     * @return 响应体字符串
     */
    private String readResponseBody(Response response) {
        if (response.body() == null) {
            return StrUtil.EMPTY;
        }

        try {
            return Util.toString(response.body().asReader(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("读取 Feign 错误响应体失败，状态码：{}", response.status(), e);
            return StrUtil.EMPTY;
        }
    }

}
```

文件位置：`order-service/src/main/java/io/github/atengk/order/config/FeignErrorConfiguration.java`

下面的配置类将自定义 `ErrorDecoder` 注册到 Feign。

```java
package io.github.atengk.order.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

/**
 * Feign 错误处理配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class FeignErrorConfiguration {

    /**
     * 配置 Feign 错误解码器
     *
     * @return Feign 错误解码器
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

}
```

在 Feign Client 中引用错误处理配置：

```java
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        path = "/api/users",
        configuration = FeignErrorConfiguration.class,
        fallbackFactory = UserFeignFallbackFactory.class
)
public interface UserFeignClient {
}
```

也可以通过配置文件指定错误解码器，但该类需要是 Spring Bean 或具备默认构造方法：

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          user-service:
            # 指定用户服务 Feign 错误解码器
            errorDecoder: io.github.atengk.order.config.FeignErrorDecoder
```

推荐在生产项目中优先使用 Java Bean 方式注册 `ErrorDecoder`，因为它更容易注入日志、JSON 工具、配置属性和统一异常组件。

#### 统一异常返回

为了让 Controller 返回统一结构，可以增加全局异常处理。

文件位置：`order-service/src/main/java/io/github/atengk/order/vo/ApiResult.java`

下面的对象用于统一接口响应结构。

```java
package io.github.atengk.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一接口响应对象
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> implements Serializable {

    /**
     * 响应码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 返回成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>("SUCCESS", "操作成功", data);
    }

    /**
     * 返回失败响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 统一响应
     */
    public static <T> ApiResult<T> fail(String code, String message) {
        return new ApiResult<>(code, message, null);
    }

}
```

文件位置：`order-service/src/main/java/io/github/atengk/order/handler/GlobalExceptionHandler.java`

下面的全局异常处理器用于将远程服务异常转换为统一响应。

```java
package io.github.atengk.order.handler;

import io.github.atengk.order.exception.RemoteServiceException;
import io.github.atengk.order.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理远程服务调用异常
     *
     * @param e 远程服务调用异常
     * @return 统一错误响应
     */
    @ExceptionHandler(RemoteServiceException.class)
    public ApiResult<Void> handleRemoteServiceException(RemoteServiceException e) {
        log.warn("远程服务调用异常，错误码：{}，错误信息：{}", e.getCode(), e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理未知异常
     *
     * @param e 未知异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail("SYSTEM_ERROR", "系统繁忙，请稍后重试");
    }

}
```

#### 验证方式

可以通过停止服务提供方、制造 404、制造 500 三种方式验证异常处理和降级逻辑。

```bash
# 1. 正常调用
curl http://127.0.0.1:8080/api/order/remote/users/10001

# 2. 停止 user-service 后再次调用，验证 fallbackFactory 是否生效
curl http://127.0.0.1:8080/api/order/remote/users/10001

# 3. 修改 Feign 路径或访问不存在资源，验证 404 错误解析
curl http://127.0.0.1:8080/api/order/remote/users/99999

# 4. 在 user-service 中主动抛出异常，验证 500 错误解析
curl http://127.0.0.1:8080/api/order/remote/users/500
```

实际排查时重点看三类日志：消费方 Feign 调用日志、消费方降级日志、服务提供方业务日志。Feign 日志能确认请求是否真正发出，降级日志能确认是否进入 fallback，提供方日志能确认远程接口是否执行。



## 拦截器使用

OpenFeign 拦截器主要用于在请求发送前统一处理请求头、认证信息、租户信息、链路追踪 ID、灰度标识等公共数据。Spring Cloud OpenFeign 会从 Spring 容器中查找 `Collection<RequestInterceptor>` 并应用到 Feign Client 中，因此可以通过声明 `RequestInterceptor` Bean 实现全局请求增强；也可以通过配置文件的 `requestInterceptors` 为指定 Feign Client 配置拦截器。([Spring 企业文档](https://docs.enterprise.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

### 请求拦截器配置

请求拦截器适合处理“所有 Feign 调用都需要携带”的公共逻辑，例如：

| 拦截内容           | 说明                      |
| ------------------ | ------------------------- |
| `Authorization`    | 透传用户登录 Token        |
| `X-Trace-Id`       | 透传或生成链路追踪 ID     |
| `X-Tenant-Id`      | 多租户系统中透传租户标识  |
| `X-User-Id`        | 内部服务间透传当前用户 ID |
| `Accept-Language`  | 国际化场景中透传语言标识  |
| `X-Source-Service` | 标识当前调用来源服务      |

推荐先定义统一的请求头常量，避免 Header 名称在多个类中硬编码。

文件位置：`order-service/src/main/java/io/github/atengk/order/constant/FeignHeaderConstants.java`

下面的常量类用于统一管理 Feign 调用中需要透传或补充的请求头名称。

```java
package io.github.atengk.order.constant;

/**
 * Feign 请求头常量
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class FeignHeaderConstants {

    /**
     * 认证请求头
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * 链路追踪 ID
     */
    public static final String TRACE_ID = "X-Trace-Id";

    /**
     * 租户 ID
     */
    public static final String TENANT_ID = "X-Tenant-Id";

    /**
     * 用户 ID
     */
    public static final String USER_ID = "X-User-Id";

    /**
     * 语言标识
     */
    public static final String ACCEPT_LANGUAGE = "Accept-Language";

    /**
     * 来源服务
     */
    public static final String SOURCE_SERVICE = "X-Source-Service";

    private FeignHeaderConstants() {
    }

}
```

文件位置：`order-service/src/main/java/io/github/atengk/order/config/FeignRequestInterceptor.java`

下面的拦截器会在 Feign 请求发送前读取当前 HTTP 请求中的公共 Header，并自动透传到远程服务；如果没有链路 ID，则自动生成一个。

```java
package io.github.atengk.order.config;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.github.atengk.order.constant.FeignHeaderConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求头拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    /**
     * 处理 Feign 请求模板
     *
     * @param template Feign 请求模板
     */
    @Override
    public void apply(RequestTemplate template) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        putHeader(template, FeignHeaderConstants.SOURCE_SERVICE, applicationName);

        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            String traceId = getOrCreateTraceId(null);
            putHeader(template, FeignHeaderConstants.TRACE_ID, traceId);
            log.debug("非 Web 请求触发 Feign 调用，已生成 traceId：{}", traceId);
            return;
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();

        copyHeader(request, template, FeignHeaderConstants.AUTHORIZATION);
        copyHeader(request, template, FeignHeaderConstants.TENANT_ID);
        copyHeader(request, template, FeignHeaderConstants.USER_ID);
        copyHeader(request, template, FeignHeaderConstants.ACCEPT_LANGUAGE);

        String traceId = getOrCreateTraceId(request.getHeader(FeignHeaderConstants.TRACE_ID));
        putHeader(template, FeignHeaderConstants.TRACE_ID, traceId);

        log.debug("Feign 请求头处理完成，url：{}，traceId：{}，sourceService：{}",
                template.url(), traceId, applicationName);
    }

    /**
     * 从原始请求复制请求头
     *
     * @param request  原始 HTTP 请求
     * @param template Feign 请求模板
     * @param name     请求头名称
     */
    private void copyHeader(HttpServletRequest request, RequestTemplate template, String name) {
        String value = request.getHeader(name);
        if (StrUtil.isNotBlank(value)) {
            putHeader(template, name, value);
        }
    }

    /**
     * 写入请求头
     *
     * @param template Feign 请求模板
     * @param name     请求头名称
     * @param value    请求头值
     */
    private void putHeader(RequestTemplate template, String name, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }

        template.removeHeader(name);
        template.header(name, value);
    }

    /**
     * 获取或生成链路追踪 ID
     *
     * @param traceId 原始链路追踪 ID
     * @return 链路追踪 ID
     */
    private String getOrCreateTraceId(String traceId) {
        String currentTraceId = StrUtil.blankToDefault(traceId, MDC.get(FeignHeaderConstants.TRACE_ID));
        if (StrUtil.isNotBlank(currentTraceId)) {
            return currentTraceId;
        }

        String newTraceId = IdUtil.fastSimpleUUID();
        MDC.put(FeignHeaderConstants.TRACE_ID, newTraceId);
        return newTraceId;
    }

}
```

如果希望只对某个 Feign Client 生效，不要使用 `@Component` 注册为全局拦截器。可以把拦截器配置成独立配置类，并通过 `@FeignClient(configuration = xxx.class)` 引用。

文件位置：`order-service/src/main/java/io/github/atengk/order/config/UserFeignInterceptorConfiguration.java`

下面的配置类只会在指定 Feign Client 引用后生效。

```java
package io.github.atengk.order.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * 用户服务 Feign 拦截器配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class UserFeignInterceptorConfiguration {

    /**
     * 配置用户服务 Feign 请求拦截器
     *
     * @return Feign 请求拦截器
     */
    @Bean
    public RequestInterceptor userFeignRequestInterceptor() {
        return new FeignRequestInterceptor();
    }

}
```

对应 Feign Client：

```java
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        path = "/api/users",
        configuration = UserFeignInterceptorConfiguration.class
)
public interface UserFeignClient {
}
```

需要注意：如果 `UserFeignInterceptorConfiguration` 被主启动类扫描到，它可能被应用到所有 Feign Client。生产项目中建议把 Feign 专用配置类放到启动类扫描包之外，或者使用全局拦截器并在拦截器内部按服务名、URL、Header 进行条件处理。

也可以通过配置文件声明拦截器。Spring Cloud OpenFeign 的配置属性支持 `requestInterceptors`、`defaultRequestHeaders`、`defaultQueryParameters` 等配置项。([Spring 企业文档](https://docs.enterprise.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

文件位置：`order-service/src/main/resources/application.yml`

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          user-service:
            # 为 user-service 配置指定请求拦截器
            requestInterceptors:
              - io.github.atengk.order.config.FeignRequestInterceptor

            # 为 user-service 增加默认请求头
            defaultRequestHeaders:
              X-Client-Type: spring-cloud-openfeign
              X-Source-Service: order-service
```

配置文件方式更适合固定 Header，Java 拦截器方式更适合动态 Header，例如 Token、租户 ID、用户 ID、链路 ID。

### Token 传递

Token 传递通常用于用户认证和权限校验。请求进入 `order-service` 后，`order-service` 调用 `user-service` 时需要把原始请求中的 `Authorization` 请求头继续传递下去，否则下游服务无法识别当前用户身份。

推荐做法是使用 Feign 拦截器统一透传 Token，而不是在每个 Feign 方法中重复写 `@RequestHeader("Authorization")`。

#### 服务消费方 Controller

文件位置：`order-service/src/main/java/io/github/atengk/order/controller/OrderRemoteTestController.java`

下面的接口不需要手动从 Controller 方法参数中取 Token，Feign 拦截器会从当前请求上下文中读取并透传。

```java
package io.github.atengk.order.controller;

import io.github.atengk.order.client.UserFeignClient;
import io.github.atengk.order.vo.UserDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单服务远程调用测试控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order/remote/users")
public class OrderRemoteTestController {

    private final UserFeignClient userFeignClient;

    /**
     * 远程查询当前登录用户
     *
     * @return 用户详情
     */
    @GetMapping("/current")
    public UserDetailVO getCurrentUser() {
        log.info("订单服务开始远程查询当前登录用户");
        return userFeignClient.getCurrentUser();
    }

}
```

#### Feign Client

文件位置：`order-service/src/main/java/io/github/atengk/order/client/UserFeignClient.java`

下面的 Feign 方法中不再声明 `Authorization` 参数，Token 由 `FeignRequestInterceptor` 自动处理。

```java
package io.github.atengk.order.client;

import io.github.atengk.order.vo.UserDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 用户服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        path = "/api/users"
)
public interface UserFeignClient {

    /**
     * 查询当前登录用户
     *
     * @return 用户详情
     */
    @GetMapping("/current")
    UserDetailVO getCurrentUser();

}
```

#### 服务提供方接口

文件位置：`user-service/src/main/java/io/github/atengk/user/controller/UserController.java`

下面的接口用于验证 Token 是否成功传递到下游服务。

```java
package io.github.atengk.user.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.user.vo.UserDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * 查询当前登录用户
     *
     * @param authorization 认证请求头
     * @param traceId       链路追踪 ID
     * @param tenantId      租户 ID
     * @param sourceService 来源服务
     * @return 用户详情
     */
    @GetMapping("/current")
    public UserDetailVO getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                       @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
                                       @RequestHeader(value = "X-Source-Service", required = false) String sourceService) {
        log.info("用户服务收到当前用户查询，traceId：{}，tenantId：{}，sourceService：{}，token是否为空：{}",
                traceId, tenantId, sourceService, StrUtil.isBlank(authorization));

        return UserDetailVO.builder()
                .id(10001L)
                .username("ateng")
                .nickname("阿腾")
                .mobile("13800000000")
                .status(1)
                .source("token-transfer")
                .build();
    }

}
```

#### 验证 Token 传递

启动 `user-service` 和 `order-service` 后，通过 `order-service` 发起请求。

```bash
curl "http://127.0.0.1:8080/api/order/remote/users/current" \
  -H "Authorization: Bearer test-token" \
  -H "X-Tenant-Id: tenant-10001" \
  -H "X-Trace-Id: trace-10001"
```

预期结果是 `user-service` 日志中能看到 `traceId`、`tenantId`、`sourceService`，并且 `token是否为空：false`。

Token 传递注意事项如下：

| 注意事项                                 | 说明                                                   |
| ---------------------------------------- | ------------------------------------------------------ |
| 不要完整打印 Token                       | 日志中只打印是否为空，必要时只打印前后几位             |
| 不要在 Feign Client 方法中重复声明 Token | 公共 Header 建议由拦截器统一处理                       |
| 异步线程中无法直接获取请求上下文         | `RequestContextHolder` 默认只在当前 Web 请求线程中可用 |
| 内部定时任务调用没有用户 Token           | 应使用服务身份 Token 或内部签名机制                    |
| 下游服务仍需独立鉴权                     | 不能因为来源是内部服务就跳过权限校验                   |

### 公共请求头处理

公共请求头处理建议形成统一规范，避免每个服务自行定义 Header 名称。对于微服务链路来说，请求头不仅用于认证，也用于观测、灰度、租户隔离和问题排查。

推荐公共请求头如下：

| 请求头             | 是否建议透传 | 说明                                  |
| ------------------ | ------------ | ------------------------------------- |
| `Authorization`    | 是           | 用户认证 Token                        |
| `X-Trace-Id`       | 是           | 链路追踪 ID                           |
| `X-Tenant-Id`      | 是           | 租户 ID                               |
| `X-User-Id`        | 视情况       | 当前用户 ID，建议由认证组件解析后设置 |
| `Accept-Language`  | 是           | 国际化语言标识                        |
| `X-Gray-Version`   | 视情况       | 灰度版本标识                          |
| `X-Source-Service` | 建议追加     | 当前调用来源服务                      |
| `Cookie`           | 通常不建议   | 内部服务调用优先使用 Token 或服务身份 |

如果系统中已经接入网关，推荐由网关负责生成或校验以下内容：

```text
X-Trace-Id
X-Tenant-Id
X-User-Id
X-Gray-Version
Authorization
```

后端服务只负责继续透传和必要校验。这样可以降低每个微服务重复处理公共 Header 的成本。

如果需要同时支持 Web 请求和非 Web 请求，例如定时任务、MQ 消费、异步任务调用 Feign，可以对请求上下文做兼容处理。

文件位置：`order-service/src/main/java/io/github/atengk/order/util/FeignHeaderContext.java`

下面的上下文工具类用于在非 Web 场景中手动设置 Feign 公共请求头。

```java
package io.github.atengk.order.util;

import cn.hutool.core.map.MapUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Feign 请求头上下文
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class FeignHeaderContext {

    private static final ThreadLocal<Map<String, String>> HEADER_CONTEXT = ThreadLocal.withInitial(HashMap::new);

    private FeignHeaderContext() {
    }

    /**
     * 设置请求头
     *
     * @param name  请求头名称
     * @param value 请求头值
     */
    public static void put(String name, String value) {
        HEADER_CONTEXT.get().put(name, value);
    }

    /**
     * 获取请求头
     *
     * @param name 请求头名称
     * @return 请求头值
     */
    public static String get(String name) {
        return HEADER_CONTEXT.get().get(name);
    }

    /**
     * 获取全部请求头
     *
     * @return 请求头 Map
     */
    public static Map<String, String> getAll() {
        return MapUtil.newHashMap(HEADER_CONTEXT.get());
    }

    /**
     * 清理当前线程请求头
     */
    public static void clear() {
        HEADER_CONTEXT.remove();
    }

}
```

然后在拦截器中补充读取 `FeignHeaderContext` 的逻辑。

文件位置：`order-service/src/main/java/io/github/atengk/order/config/FeignRequestInterceptor.java`

下面只展示需要追加到 `apply` 方法中的非 Web 场景处理逻辑。

```java
Map<String, String> contextHeaders = FeignHeaderContext.getAll();
contextHeaders.forEach((name, value) -> putHeader(template, name, value));
```

定时任务或 MQ 消费中调用 Feign 时，可以这样使用：

```java
try {
    FeignHeaderContext.put(FeignHeaderConstants.TRACE_ID, IdUtil.fastSimpleUUID());
    FeignHeaderContext.put(FeignHeaderConstants.SOURCE_SERVICE, "order-service");
    userFeignClient.getCurrentUser();
} finally {
    FeignHeaderContext.clear();
}
```

这种方式可以避免非 Web 线程中 `RequestContextHolder` 为空导致公共 Header 丢失。需要注意，`ThreadLocal` 必须在 `finally` 中清理，否则在线程池场景中可能造成上下文污染。

## 负载均衡与服务发现

OpenFeign 通过 `@FeignClient(name = "服务名")` 与服务发现组件集成。调用方不需要写死服务提供方 IP 和端口，而是通过服务名从注册中心获取实例列表，再由 Spring Cloud LoadBalancer 选择具体实例。Spring Cloud OpenFeign 文档说明，`@FeignClient` 的名称会用于创建 Spring Cloud LoadBalancer 客户端；当 Spring Cloud LoadBalancer 在 classpath 中时，Feign 会使用 `FeignBlockingLoadBalancerClient`，否则使用默认 Feign Client。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

### 基于服务名调用

基于服务名调用是微服务内部调用的标准方式。服务提供方注册到注册中心，服务消费方通过相同的服务名发起调用。

整体调用关系如下：

```text
order-service
  -> @FeignClient(name = "user-service")
  -> 注册中心查询 user-service 实例列表
  -> Spring Cloud LoadBalancer 选择其中一个实例
  -> 调用 user-service 的具体 IP:PORT
```

服务提供方配置如下。

文件位置：`user-service/src/main/resources/application.yml`

```yaml
server:
  # 通过启动参数覆盖端口，便于本地启动多个实例
  port: ${SERVER_PORT:8081}

spring:
  application:
    # 服务提供方名称，Feign 通过该名称调用
    name: user-service

  cloud:
    nacos:
      discovery:
        # Nacos 注册中心地址
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查端点
        include: health,info
```

服务消费方配置如下。

文件位置：`order-service/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    # 服务消费方名称
    name: order-service

  cloud:
    nacos:
      discovery:
        # 与服务提供方使用同一个注册中心
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
```

Feign Client 通过服务名调用。

文件位置：`order-service/src/main/java/io/github/atengk/order/client/UserFeignClient.java`

```java
package io.github.atengk.order.client;

import io.github.atengk.order.vo.UserDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 用户服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(
        name = "user-service",
        contextId = "userFeignClient",
        path = "/api/users"
)
public interface UserFeignClient {

    /**
     * 查询服务实例信息
     *
     * @return 用户详情
     */
    @GetMapping("/instance")
    UserDetailVO getInstance();

}
```

服务提供方返回当前实例端口，便于验证负载均衡效果。

文件位置：`user-service/src/main/java/io/github/atengk/user/controller/UserController.java`

```java
package io.github.atengk.user.controller;

import io.github.atengk.user.vo.UserDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Value("${server.port}")
    private Integer serverPort;

    /**
     * 查询当前服务实例信息
     *
     * @return 当前服务实例信息
     */
    @GetMapping("/instance")
    public UserDetailVO getInstance() {
        log.info("用户服务实例被调用，端口：{}", serverPort);

        return UserDetailVO.builder()
                .id(Long.valueOf(serverPort))
                .username("user-service")
                .nickname("用户服务实例：" + serverPort)
                .status(1)
                .source("port-" + serverPort)
                .build();
    }

}
```

消费方测试接口如下。

文件位置：`order-service/src/main/java/io/github/atengk/order/controller/OrderRemoteTestController.java`

```java
package io.github.atengk.order.controller;

import io.github.atengk.order.client.UserFeignClient;
import io.github.atengk.order.vo.UserDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单服务远程调用测试控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order/remote/users")
public class OrderRemoteTestController {

    private final UserFeignClient userFeignClient;

    /**
     * 查询用户服务实例
     *
     * @return 用户服务实例信息
     */
    @GetMapping("/instance")
    public UserDetailVO getUserServiceInstance() {
        log.info("订单服务开始调用用户服务实例查询接口");
        return userFeignClient.getInstance();
    }

}
```

服务名调用模式下，不要在 `@FeignClient` 中配置 `url`。一旦配置 `url`，Feign 会使用固定地址，不再通过服务发现和负载均衡选择实例。Spring Cloud OpenFeign 文档中也说明，`url` 属性会直接指定目标地址，使用该方式不会进行负载均衡。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

### Spring Cloud LoadBalancer 集成

Spring Cloud LoadBalancer 是 Spring Cloud 当前推荐的客户端负载均衡组件。OpenFeign 与它集成后，消费方通过服务名调用时，会由 LoadBalancer 从服务实例列表中选择一个实例。Spring Cloud OpenFeign 默认会在 classpath 中存在 Spring Cloud LoadBalancer 时使用 `FeignBlockingLoadBalancerClient`。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

Maven 依赖建议显式引入 `spring-cloud-starter-loadbalancer`，避免依赖缺失导致服务名无法解析。

文件位置：`order-service/pom.xml`

```xml
<!-- OpenFeign 声明式 HTTP 客户端 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- Spring Cloud LoadBalancer，用于基于服务名的客户端负载均衡 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>

<!-- Nacos 服务发现客户端，根据实际注册中心选择；如果使用 Eureka，则替换为 Eureka Client -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

如果使用 Eureka，依赖替换为：

```xml
<!-- Eureka 客户端，用于服务注册与服务发现 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

LoadBalancer 默认已经可以完成基础负载均衡。多数项目不需要自定义负载均衡策略。如果需要调整指定服务的实例选择逻辑，可以通过 `@LoadBalancerClient` 配置指定服务的负载均衡策略。

文件位置：`order-service/src/main/java/io/github/atengk/order/config/UserServiceLoadBalancerConfiguration.java`

下面的配置类将 `user-service` 的实例选择策略设置为随机策略。

```java
package io.github.atengk.order.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 用户服务负载均衡配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class UserServiceLoadBalancerConfiguration {

    /**
     * 配置随机负载均衡策略
     *
     * @param environment                 环境变量
     * @param serviceInstanceListSupplier 服务实例列表提供器
     * @return 负载均衡器
     */
    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            ServiceInstanceListSupplier serviceInstanceListSupplier) {
        String serviceId = environment.getProperty("spring.cloud.loadbalancer.client.name");
        return new RandomLoadBalancer(serviceInstanceListSupplier, serviceId);
    }

}
```

文件位置：`order-service/src/main/java/io/github/atengk/order/config/LoadBalancerConfiguration.java`

下面的配置类将随机负载均衡策略绑定到 `user-service`。

```java
package io.github.atengk.order.config;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Configuration;

/**
 * LoadBalancer 配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
@LoadBalancerClient(
        name = "user-service",
        configuration = UserServiceLoadBalancerConfiguration.class
)
public class LoadBalancerConfiguration {
}
```

需要注意，`UserServiceLoadBalancerConfiguration` 不建议被主启动类扫描成普通全局配置，否则可能影响其他服务的负载均衡策略。建议将该类放在启动类扫描路径之外，或只通过 `@LoadBalancerClient(configuration = xxx.class)` 引用。

如果只需要默认负载均衡，不需要增加上述自定义配置。默认策略已经足够满足多数服务调用场景。自定义策略通常用于灰度发布、同机房优先、指定版本优先、按权重选择等场景。

### 多实例调用验证

多实例验证用于确认服务名调用和客户端负载均衡是否真正生效。验证思路是启动多个 `user-service` 实例，让它们注册到同一个注册中心，然后通过 `order-service` 多次调用 `user-service`，观察返回的端口或实例标识是否发生变化。

#### 启动多个服务提供方实例

使用不同端口启动两个 `user-service` 实例。

```bash
# 启动第一个 user-service 实例
java -jar user-service/target/user-service.jar \
  --server.port=8081 \
  --spring.application.name=user-service \
  --spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848

# 启动第二个 user-service 实例
java -jar user-service/target/user-service.jar \
  --server.port=8082 \
  --spring.application.name=user-service \
  --spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
```

这两个实例的 `spring.application.name` 必须相同，都为 `user-service`。端口不同，但服务名一致，注册中心才会把它们视为同一个服务下的多个实例。

然后启动消费方服务：

```bash
java -jar order-service/target/order-service.jar \
  --server.port=8080 \
  --spring.application.name=order-service \
  --spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
```

#### 单次调用验证

调用消费方接口：

```bash
curl http://127.0.0.1:8080/api/order/remote/users/instance
```

可能返回：

```json
{
  "id": 8081,
  "username": "user-service",
  "nickname": "用户服务实例：8081",
  "status": 1,
  "source": "port-8081"
}
```

再次调用可能返回：

```json
{
  "id": 8082,
  "username": "user-service",
  "nickname": "用户服务实例：8082",
  "status": 1,
  "source": "port-8082"
}
```

如果多次调用一直只返回同一个端口，不能立即判断负载均衡失效。可能原因包括实例列表未刷新、只有一个实例注册成功、负载均衡策略选择结果集中、注册中心健康检查剔除了其中一个实例，或自定义策略固定选择了某个实例。

#### 批量调用验证

可以使用循环调用观察实例分布。

```bash
for i in $(seq 1 10); do
  curl -s http://127.0.0.1:8080/api/order/remote/users/instance
  echo
done
```

如果希望只观察端口，可以配合 `jq`：

```bash
for i in $(seq 1 10); do
  curl -s http://127.0.0.1:8080/api/order/remote/users/instance | jq '.source'
done
```

命令说明：`seq 1 10` 用于执行 10 次请求，`curl -s` 用于静默输出响应内容，`jq '.source'` 用于从 JSON 响应中提取当前服务实例标识。没有安装 `jq` 时，可以直接查看完整 JSON 响应。

#### 通过日志验证

同时观察两个 `user-service` 实例的日志：

```text
用户服务实例被调用，端口：8081
用户服务实例被调用，端口：8082
```

消费方日志中应看到多次 Feign 调用记录，服务提供方日志中应看到不同实例被调用。

#### 常见问题排查

多实例调用不生效时，按下面顺序排查：

| 问题                   | 排查方式                                                     |
| ---------------------- | ------------------------------------------------------------ |
| 只注册了一个实例       | 查看 Nacos/Eureka 控制台，确认 `user-service` 下是否有多个健康实例 |
| Feign 配置了 `url`     | 检查 `@FeignClient` 是否配置 `url`，固定 URL 不走服务发现    |
| 缺少 LoadBalancer 依赖 | 检查是否引入 `spring-cloud-starter-loadbalancer`             |
| 服务名不一致           | 检查 `@FeignClient(name = "user-service")` 与提供方 `spring.application.name` 是否一致 |
| 注册中心地址不一致     | 检查消费方和提供方是否连接同一个注册中心                     |
| 实例不健康             | 查看注册中心实例状态和 `/actuator/health`                    |
| 网络不可达             | 在消费方机器上直接访问提供方实例 IP 和端口                   |
| 日志看不到 Feign 请求  | 检查 Feign 日志级别和调用入口是否真正执行                    |

#### 验证结论

当满足以下条件时，可以认为 OpenFeign 基于服务名调用和负载均衡已经生效：

```text
1. Nacos/Eureka 中 user-service 存在多个健康实例
2. order-service 中 @FeignClient(name = "user-service") 没有配置 url
3. order-service 已引入 spring-cloud-starter-loadbalancer
4. 多次调用 order-service 接口后，user-service 多个实例均出现调用日志
5. 返回结果中的端口或实例标识能证明请求被分发到不同实例
```

完成这部分后，OpenFeign 已具备企业项目中常见的公共 Header 透传、Token 传递、服务发现、客户端负载均衡和多实例验证能力。后续可以继续补充测试联调、日志查看、异常场景验证以及生产环境注意事项。



## 测试与验证

本节用于验证 OpenFeign 接入是否可用，重点检查服务注册、服务发现、接口调用、请求参数、请求头透传、日志输出、异常处理和降级逻辑。建议按照“正常调用 → 日志确认 → 异常验证”的顺序执行，避免一开始就从复杂异常场景排查问题。

### 接口联调

接口联调主要验证服务提供方接口是否可直接访问，以及服务消费方是否能通过 OpenFeign 正确调用服务提供方。联调时建议先验证 `user-service`，再验证 `order-service`，最后验证完整 Feign 调用链路。

#### 启动顺序

本地联调建议按以下顺序启动：

```text
1. 启动注册中心，例如 Nacos 或 Eureka
2. 启动 user-service 服务提供方
3. 启动 order-service 服务消费方
4. 通过 curl、Postman 或 Apifox 调用 order-service 接口
5. 查看 order-service 和 user-service 日志
```

如果使用 Nacos，确认 Nacos 控制台中存在以下服务：

```text
user-service
order-service
```

如果使用 Eureka，确认 Eureka 控制台中存在以下应用：

```text
USER-SERVICE
ORDER-SERVICE
```

#### 服务健康检查

先检查服务是否正常启动。

```bash
# 检查 user-service 健康状态
curl http://127.0.0.1:8081/actuator/health

# 检查 order-service 健康状态
curl http://127.0.0.1:8080/actuator/health
```

正常情况下返回：

```json
{
  "status": "UP"
}
```

如果返回连接失败，说明服务未启动、端口不正确或网络不可达。如果返回 `DOWN`，需要检查数据库、Redis、注册中心或其他健康检查依赖是否异常。

#### 直接调用服务提供方

先绕过 OpenFeign，直接调用 `user-service`，确认服务提供方接口本身可用。

```bash
# 根据用户 ID 查询用户详情
curl http://127.0.0.1:8081/api/users/10001

# 根据查询条件查询用户详情
curl "http://127.0.0.1:8081/api/users/detail?id=10001&username=ateng&mobile=13800000000"

# 查询当前登录用户，验证请求头接收
curl "http://127.0.0.1:8081/api/users/current" \
  -H "Authorization: Bearer test-token" \
  -H "X-Trace-Id: trace-direct-10001" \
  -H "X-Tenant-Id: tenant-10001"

# 创建用户
curl -X POST "http://127.0.0.1:8081/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "mobile": "13800000000"
  }'
```

如果服务提供方直接调用失败，不要继续排查 Feign。应先修复 `user-service` 的 Controller、请求路径、参数绑定、JSON 序列化或业务异常。

#### 通过 OpenFeign 调用服务提供方

确认 `user-service` 正常后，再调用 `order-service`，由 `order-service` 内部通过 OpenFeign 调用 `user-service`。

```bash
# 通过 order-service 远程查询用户详情
curl http://127.0.0.1:8080/api/order/remote/users/10001

# 通过 order-service 远程条件查询用户
curl "http://127.0.0.1:8080/api/order/remote/users/detail?id=10001&username=ateng&mobile=13800000000"

# 通过 order-service 远程查询当前登录用户，并验证 Token 透传
curl "http://127.0.0.1:8080/api/order/remote/users/current" \
  -H "Authorization: Bearer test-token" \
  -H "X-Trace-Id: trace-feign-10001" \
  -H "X-Tenant-Id: tenant-10001"

# 通过 order-service 远程创建用户
curl -X POST "http://127.0.0.1:8080/api/order/remote/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -H "X-Tenant-Id: tenant-10001" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "mobile": "13800000000"
  }'
```

联调时重点确认以下内容：

| 检查项                                  | 预期结果                              |
| --------------------------------------- | ------------------------------------- |
| `order-service` 能正常返回数据          | Feign Client 已成功注册为 Spring Bean |
| `user-service` 日志中能看到调用记录     | Feign 请求已经真正到达服务提供方      |
| `Authorization` 能透传到 `user-service` | Feign 请求拦截器生效                  |
| `X-Trace-Id` 在两个服务日志中一致       | 链路 ID 透传正常                      |
| POST 请求体字段完整                     | JSON 编码器正常                       |
| 返回对象字段完整                        | JSON 解码器正常                       |

#### 使用 Spring Boot Test 验证 Feign Client

除了手动 curl，也可以使用集成测试验证 Feign Client 是否能正常调用。该方式适合本地回归和 CI 环境中的基础连通性检查。

文件位置：`order-service/src/test/java/io/github/atengk/order/client/UserFeignClientTest.java`

下面的测试类用于验证用户服务 Feign Client 的基础调用能力。

```java
package io.github.atengk.order.client;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.order.dto.UserCreateDTO;
import io.github.atengk.order.dto.UserQueryDTO;
import io.github.atengk.order.vo.UserDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 用户服务 Feign 客户端测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@SpringBootTest
class UserFeignClientTest {

    @Autowired
    private UserFeignClient userFeignClient;

    /**
     * 测试根据用户 ID 查询用户详情
     */
    @Test
    void testGetUserById() {
        UserDetailVO userDetailVO = userFeignClient.getUserById(10001L);

        log.info("Feign 查询用户详情结果：{}", userDetailVO);

        Assertions.assertNotNull(userDetailVO);
        Assertions.assertEquals(10001L, userDetailVO.getId());
        Assertions.assertTrue(StrUtil.isNotBlank(userDetailVO.getUsername()));
    }

    /**
     * 测试根据查询条件查询用户详情
     */
    @Test
    void testGetUserDetail() {
        UserQueryDTO queryDTO = new UserQueryDTO();
        queryDTO.setId(10001L);
        queryDTO.setUsername("ateng");
        queryDTO.setMobile("13800000000");

        UserDetailVO userDetailVO = userFeignClient.getUserDetail(queryDTO);

        log.info("Feign 条件查询用户结果：{}", userDetailVO);

        Assertions.assertNotNull(userDetailVO);
        Assertions.assertEquals("ateng", userDetailVO.getUsername());
    }

    /**
     * 测试创建用户
     */
    @Test
    void testCreateUser() {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername("ateng");
        createDTO.setNickname("阿腾");
        createDTO.setMobile("13800000000");

        UserDetailVO userDetailVO = userFeignClient.createUser(createDTO);

        log.info("Feign 创建用户结果：{}", userDetailVO);

        Assertions.assertNotNull(userDetailVO);
        Assertions.assertEquals("ateng", userDetailVO.getUsername());
    }

}
```

执行测试：

```bash
# 在 order-service 模块目录下执行
mvn test -Dtest=UserFeignClientTest
```

该命令只执行 `UserFeignClientTest` 测试类。执行前需要确保注册中心和 `user-service` 已经启动，否则测试会因为服务不可用、连接失败或无可用实例而失败。

### 日志查看

日志查看用于确认 Feign 请求是否发出、请求地址是否正确、请求参数是否完整、响应状态码是否正常，以及异常是否被降级或统一解析。排查 Feign 问题时，不建议只看接口响应，应同时查看消费方和提供方日志。

#### 开启 Feign 调试日志

Feign 日志需要同时配置 Spring Boot 日志级别和 Feign 自身日志级别。只有将 Feign 接口或包的日志级别设置为 `DEBUG`，`loggerLevel` 配置才会真正输出。

文件位置：`order-service/src/main/resources/application.yml`

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          user-service:
            # 本地联调建议使用 full；生产环境建议使用 basic 或 none
            loggerLevel: full

logging:
  level:
    # 指定单个 Feign Client 输出 DEBUG 日志
    io.github.atengk.order.client.UserFeignClient: DEBUG

    # 输出 OpenFeign 相关调试日志
    org.springframework.cloud.openfeign: DEBUG

    # 输出 LoadBalancer 相关调试日志
    org.springframework.cloud.loadbalancer: DEBUG
```

本地联调时可以使用 `FULL`，生产环境不建议长期使用。`FULL` 会输出请求体、响应体和请求头，存在泄露 Token、手机号、身份证号等敏感数据的风险。

#### 推荐日志格式

为了方便排查链路问题，建议在日志格式中加入 `traceId`。如果项目使用 Logback，可以配置如下日志格式。

文件位置：`order-service/src/main/resources/application.yml`

```yaml
logging:
  pattern:
    # 在控制台日志中输出 traceId，便于跨服务排查
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{X-Trace-Id}] %logger{36} - %msg%n"
```

如果请求拦截器中已经将 `X-Trace-Id` 写入 MDC，则日志中可以看到类似内容：

```text
2026-05-05 10:30:12.125 INFO  [http-nio-8080-exec-1] [trace-feign-10001] i.g.a.order.controller.OrderRemoteTestController - 订单服务开始远程查询当前登录用户
2026-05-05 10:30:12.241 INFO  [http-nio-8081-exec-3] [trace-feign-10001] i.g.a.user.controller.UserController - 用户服务收到当前用户查询，traceId：trace-feign-10001，tenantId：tenant-10001，sourceService：order-service，token是否为空：false
```

如果两个服务日志中的 `traceId` 一致，说明链路 ID 透传正常。

#### Feign 正常调用日志

开启 `loggerLevel: full` 后，正常调用一般可以看到类似日志：

```text
[UserFeignClient#getUserById] ---> GET http://user-service/api/users/10001 HTTP/1.1
[UserFeignClient#getUserById] X-Trace-Id: trace-feign-10001
[UserFeignClient#getUserById] X-Tenant-Id: tenant-10001
[UserFeignClient#getUserById] ---> END HTTP (0-byte body)
[UserFeignClient#getUserById] <--- HTTP/1.1 200 (38ms)
[UserFeignClient#getUserById] content-type: application/json
[UserFeignClient#getUserById] {"id":10001,"username":"ateng","nickname":"阿腾","status":1}
[UserFeignClient#getUserById] <--- END HTTP
```

需要重点看以下字段：

| 日志内容                  | 检查目的                             |
| ------------------------- | ------------------------------------ |
| `GET/POST`                | HTTP 方法是否正确                    |
| `http://user-service/...` | 是否使用服务名调用                   |
| 请求路径                  | 是否与服务提供方 Controller 路径一致 |
| 请求头                    | Token、TraceId、TenantId 是否透传    |
| 响应状态码                | 是否为 2xx                           |
| 响应体                    | JSON 字段是否符合 VO 定义            |
| 执行耗时                  | 是否接近或超过读取超时时间           |

#### 服务发现日志

如果 Feign 基于服务名调用失败，重点查看 LoadBalancer 和注册中心日志。

常见错误日志如下：

```text
No servers available for service: user-service
```

说明消费方没有从注册中心拿到 `user-service` 的可用实例。需要检查：

```text
1. user-service 是否已启动
2. user-service 是否成功注册到 Nacos/Eureka
3. order-service 和 user-service 是否连接同一个注册中心
4. @FeignClient(name = "user-service") 是否与 spring.application.name 一致
5. user-service 实例是否处于健康状态
```

如果看到类似错误：

```text
Connection refused: /127.0.0.1:8081
```

说明已经选中了服务实例，但目标地址无法连接。常见原因是服务实例下线、端口不通、防火墙限制、容器端口映射错误或注册中心中残留了旧实例。

#### 请求头日志脱敏

如果需要打印请求头，必须对敏感字段脱敏。不要完整输出 `Authorization`、`Cookie`、`Set-Cookie`、`X-Api-Key` 等字段。

文件位置：`order-service/src/main/java/io/github/atengk/order/util/SensitiveLogUtil.java`

下面的工具类用于对敏感内容进行脱敏。

```java
package io.github.atengk.order.util;

import cn.hutool.core.util.StrUtil;

/**
 * 敏感日志工具类
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class SensitiveLogUtil {

    private SensitiveLogUtil() {
    }

    /**
     * 脱敏 Token
     *
     * @param token 原始 Token
     * @return 脱敏后的 Token
     */
    public static String maskToken(String token) {
        if (StrUtil.isBlank(token)) {
            return StrUtil.EMPTY;
        }

        if (token.length() <= 12) {
            return "******";
        }

        return StrUtil.subPre(token, 8) + "******" + StrUtil.subSuf(token, token.length() - 4);
    }

}
```

使用示例：

```java
log.info("准备透传认证请求头，authorization：{}", SensitiveLogUtil.maskToken(authorization));
```

这样既能确认 Token 是否存在，也能避免敏感信息泄露。

### 异常场景验证

异常场景验证用于确认 OpenFeign 在超时、服务不可用、接口不存在、远程服务异常、请求参数错误、响应解析失败等情况下是否符合预期。建议每类异常都至少验证一次，并记录标准处理方式。

#### 服务不可用验证

服务不可用是最常见的异常场景。可以停止 `user-service`，然后继续调用 `order-service`。

```bash
# 停止 user-service 后调用
curl http://127.0.0.1:8080/api/order/remote/users/10001
```

如果配置了 `fallbackFactory`，预期返回兜底结果，例如：

```json
{
  "id": 10001,
  "username": "fallback-user",
  "nickname": "用户服务暂时不可用",
  "status": 0,
  "source": "fallback-factory"
}
```

如果没有配置降级，可能返回统一异常响应：

```json
{
  "code": "USER_SERVICE_UNAVAILABLE",
  "message": "用户服务暂时不可用，请稍后重试",
  "data": null
}
```

需要同时检查 `order-service` 日志中是否出现以下信息：

```text
调用用户服务超时或连接失败
用户服务调用触发降级
No servers available for service: user-service
Connection refused
```

如果希望业务接口失败时直接返回错误，不要配置 fallback；如果希望非核心接口有兜底数据，可以配置 fallback 或 fallbackFactory。

#### 超时验证

超时验证用于确认 `connectTimeout` 和 `readTimeout` 是否生效。可以在 `user-service` 中增加一个慢接口。

文件位置：`user-service/src/main/java/io/github/atengk/user/controller/UserMockController.java`

下面的接口用于模拟远程服务响应超时。

```java
package io.github.atengk.user.controller;

import cn.hutool.core.thread.ThreadUtil;
import io.github.atengk.user.vo.UserDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户服务异常模拟控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/api/users/mock")
public class UserMockController {

    /**
     * 模拟慢接口
     *
     * @param seconds 睡眠秒数
     * @return 用户详情
     */
    @GetMapping("/slow")
    public UserDetailVO slow(@RequestParam(value = "seconds", defaultValue = "10") Integer seconds) {
        log.info("开始模拟慢接口，睡眠秒数：{}", seconds);
        ThreadUtil.sleep(seconds * 1000L);

        return UserDetailVO.builder()
                .id(10001L)
                .username("slow-user")
                .nickname("慢接口用户")
                .status(1)
                .source("slow-api")
                .build();
    }

}
```

Feign Client 增加方法：

```java
@GetMapping("/mock/slow")
UserDetailVO slow(@RequestParam("seconds") Integer seconds);
```

调用测试：

```bash
# 假设 readTimeout 配置为 5000ms，这里睡眠 10 秒会触发读取超时
curl "http://127.0.0.1:8080/api/order/remote/users/slow?seconds=10"
```

预期日志中出现类似信息：

```text
Read timed out
RetryableException
用户服务调用触发降级
```

如果没有触发超时，检查 `spring.cloud.openfeign.client.config` 配置是否写在正确服务名下，或者是否被其他 Java 配置覆盖。

#### 404 接口不存在验证

404 常见于 Feign Client 路径、Controller 路径、HTTP 方法不一致。可以临时将 Feign Client 路径写错，或者访问不存在的资源。

示例错误写法：

```java
@GetMapping("/not-exists/{id}")
UserDetailVO getUserById(@PathVariable("id") Long id);
```

调用：

```bash
curl http://127.0.0.1:8080/api/order/remote/users/10001
```

预期日志：

```text
FeignException$NotFound
status 404
```

如果配置了 `ErrorDecoder`，则应该转换为统一异常，例如：

```json
{
  "code": "REMOTE_SERVICE_ERROR",
  "message": "远程服务调用失败",
  "data": null
}
```

404 排查顺序：

```text
1. 检查 @FeignClient(path = "/api/users")
2. 检查 Feign 方法上的 @GetMapping、@PostMapping
3. 检查服务提供方 @RequestMapping 路径
4. 检查 PathVariable 参数名是否一致
5. 检查是否调用了错误的服务名或错误的服务版本
```

#### 400 参数错误验证

400 通常由请求参数缺失、类型不匹配或请求体格式错误导致。

可以在服务提供方增加参数校验，例如要求用户 ID 不能为空：

```java
@GetMapping("/required")
public UserDetailVO required(@RequestParam("id") Long id) {
    log.info("查询必填用户，用户ID：{}", id);
    return userService.getUserById(id);
}
```

然后在 Feign Client 或调用方中不传 `id`：

```bash
curl "http://127.0.0.1:8080/api/order/remote/users/required"
```

预期响应可能是：

```json
{
  "code": "REMOTE_SERVICE_ERROR",
  "message": "远程服务调用失败",
  "data": null
}
```

如果远程服务返回统一错误结构，并且消费方配置了 `ErrorDecoder`，则可以解析出更明确的错误码和错误信息。

400 排查重点如下：

| 问题                         | 检查方式                              |
| ---------------------------- | ------------------------------------- |
| `@RequestParam` 缺失         | 检查 Feign 方法参数是否传入           |
| `@PathVariable` 参数名不一致 | 检查注解中的变量名是否与路径一致      |
| `@RequestBody` 为空          | 检查 POST 方法是否添加 `@RequestBody` |
| JSON 格式错误                | 检查请求体是否是合法 JSON             |
| 字段类型不匹配               | 检查 DTO 字段类型与请求值是否一致     |

#### 500 远程服务异常验证

500 用于验证远程服务内部异常时，消费方是否能正确解析错误响应、记录日志并返回统一结果。

文件位置：`user-service/src/main/java/io/github/atengk/user/controller/UserMockController.java`

在异常模拟控制器中增加一个主动抛出异常的接口。

```java
@GetMapping("/error")
public UserDetailVO error() {
    log.error("开始模拟用户服务内部异常");
    throw new IllegalStateException("模拟用户服务内部异常");
}
```

Feign Client 增加方法：

```java
@GetMapping("/mock/error")
UserDetailVO error();
```

调用测试：

```bash
curl http://127.0.0.1:8080/api/order/remote/users/error
```

预期日志：

```text
FeignException$InternalServerError
status 500
Feign 调用返回错误响应
用户服务调用触发降级
```

如果已经配置全局异常处理和 `ErrorDecoder`，接口最终应返回统一结构：

```json
{
  "code": "REMOTE_SERVICE_ERROR",
  "message": "远程服务调用失败",
  "data": null
}
```

生产项目中，服务提供方应尽量返回统一错误响应结构，例如：

```json
{
  "code": "USER_QUERY_FAILED",
  "message": "用户查询失败",
  "data": null
}
```

这样消费方的 `ErrorDecoder` 才能准确解析业务错误码，而不是只能依赖 HTTP 状态码。

#### 响应解析失败验证

响应解析失败通常出现在远程服务返回 JSON 字段类型与 Feign Client 接收对象不一致时。例如远程服务返回：

```json
{
  "id": "abc",
  "username": "ateng"
}
```

但消费方 VO 中定义：

```java
private Long id;
```

此时可能出现反序列化异常。排查重点如下：

| 问题               | 处理方式                                   |
| ------------------ | ------------------------------------------ |
| 数字字段返回字符串 | 统一远程接口字段类型                       |
| 日期格式不一致     | 统一 Jackson 日期格式配置                  |
| 枚举值不一致       | 统一枚举编码和反序列化策略                 |
| 返回结构多包了一层 | 检查是否应该接收 `ApiResult<UserDetailVO>` |
| 字段类型变更未同步 | DTO/VO 建议抽取到公共 api 模块             |

建议服务间接口对象放到独立 API 模块中，例如：

```text
user-api
├── dto
│   ├── UserCreateDTO.java
│   └── UserQueryDTO.java
├── vo
│   └── UserDetailVO.java
└── client
    └── UserFeignClient.java
```

这样 `user-service` 和 `order-service` 可以复用同一套 DTO/VO，减少字段不一致导致的解析失败。

#### 多实例异常验证

多实例场景下，还需要验证某个实例异常时负载均衡和健康检查是否正常。

启动两个 `user-service` 实例：

```bash
# 正常实例
java -jar user-service/target/user-service.jar --server.port=8081

# 另一个实例
java -jar user-service/target/user-service.jar --server.port=8082
```

然后停止其中一个实例，例如停止 `8082`，继续多次调用：

```bash
for i in $(seq 1 10); do
  curl -s http://127.0.0.1:8080/api/order/remote/users/instance
  echo
done
```

预期结果是注册中心最终剔除不健康实例，请求逐步只打到健康实例。如果短时间内仍然调用到已停止实例，可能是注册中心实例剔除存在延迟，这是正常现象。可以通过调整注册中心健康检查和客户端缓存刷新配置优化。

#### 验证结论清单

完成以下检查后，可以认为 OpenFeign 基础接入、配置管理、拦截器、负载均衡和异常处理已经验证通过：

```text
1. user-service 可直接访问
2. order-service 可通过 Feign 调用 user-service
3. GET 路径参数、查询参数传递正常
4. POST 请求体传递正常
5. Authorization、X-Trace-Id、X-Tenant-Id 等请求头透传正常
6. Feign 日志可以看到请求方法、路径、状态码和耗时
7. 服务不可用时能进入降级或返回统一异常
8. 超时时间配置生效
9. 404、400、500 能被识别和处理
10. 多实例调用时请求能分发到不同 user-service 实例
```

如果以上验证均通过，说明当前 Spring Boot 3 + Spring Cloud OpenFeign 的核心开发链路已经具备可用性。后续生产落地时，应继续补充接口版本管理、统一响应规范、调用链追踪、指标监控、灰度发布、限流熔断和依赖治理。