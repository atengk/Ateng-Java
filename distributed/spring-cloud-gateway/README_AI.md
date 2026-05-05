# Spring Cloud Gateway



## 网关概述

本章用于说明 Spring Cloud Gateway 在微服务系统中的定位、适用场景，以及它和传统网关方案的区别。理解这些内容后，再进入路由、过滤器、服务发现、鉴权、限流等开发章节会更顺。

### Spring Cloud Gateway 定位

Spring Cloud Gateway 是 Spring Cloud 体系中的 API 网关组件，主要负责统一接入外部请求，并将请求转发到后端微服务。它不是业务服务本身，而是位于客户端和后端服务之间的入口层，承担路由转发、请求过滤、鉴权校验、限流熔断、日志审计、跨域处理、协议适配等横切能力。

在 Spring Boot 3 项目中，推荐使用基于 WebFlux 的 Gateway Server 模式。Spring 官方文档说明，Spring Cloud Gateway 构建在 Spring Boot、Spring WebFlux 和 Project Reactor 之上，并且需要 Spring Boot 与 WebFlux 提供的 Netty 运行时，不适合作为传统 Servlet WAR 包部署。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/starter.html))

从项目架构上看，Gateway 通常放在最外层，典型调用链如下：

```text
客户端
  ↓
Spring Cloud Gateway
  ↓
认证服务 / 用户服务 / 订单服务 / 文件服务 / 其他业务服务
```

在实际项目中，网关一般只处理通用能力，不建议在网关中编写复杂业务逻辑。业务判断应下沉到对应微服务中，网关只负责“入口控制”和“流量治理”。

### 典型使用场景

Spring Cloud Gateway 适合用于微服务系统的统一入口。它可以将不同业务服务的接口统一暴露在同一个域名、端口或入口路径下，客户端不需要直接感知后端服务地址变化。

常见场景包括：

| 场景         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 统一路由转发 | 根据请求路径、请求方法、Header、Query 参数等条件转发到不同服务 |
| 统一鉴权     | 在请求进入后端服务前校验 Token、登录态、签名、权限等信息     |
| 用户信息透传 | 鉴权成功后，将用户 ID、租户 ID、角色等信息写入请求头传递给下游服务 |
| 跨域处理     | 在网关层统一处理 CORS，避免每个后端服务重复配置              |
| 限流控制     | 对接口、用户、IP、租户、服务维度进行访问频率控制             |
| 黑白名单控制 | 对公开接口、内部接口、管理接口做访问范围限制                 |
| 日志审计     | 统一记录请求路径、耗时、状态码、客户端 IP、异常信息          |
| 灰度发布     | 根据 Header、用户标识、版本号等条件转发到不同服务版本        |
| 服务发现路由 | 结合 Nacos、Eureka、Consul 等注册中心，按服务名动态转发      |

Spring Cloud Gateway 官方定位中也强调了智能路由、微代理、服务治理等分布式系统通用能力，Spring Cloud 本身提供配置管理、服务发现、路由、负载均衡、熔断等微服务基础组件。([Home](https://spring.io/projects/spring-cloud/))

### 与传统网关方案对比

传统网关方案通常包括 Nginx、OpenResty、Apache APISIX、Kong、Traefik 等。Spring Cloud Gateway 与这些组件并不是完全替代关系，而是定位不同。

| 对比项     | Spring Cloud Gateway                        | Nginx / OpenResty                        | Kong / APISIX 等 API 网关                  |
| ---------- | ------------------------------------------- | ---------------------------------------- | ------------------------------------------ |
| 技术栈     | Java、Spring Boot、WebFlux、Reactor         | C、Lua、Nginx 配置                       | 多语言生态，通常带插件体系                 |
| 微服务集成 | 与 Spring Cloud、服务发现、配置中心集成自然 | 需要额外脚本或配置同步                   | 支持较强，但需要独立网关平台               |
| 开发方式   | Java 代码 + YAML 配置                       | Nginx 配置 + Lua 扩展                    | 插件配置、控制台、Admin API                |
| 业务扩展   | Java 开发者上手成本低                       | Lua 或 Nginx 配置要求较高                | 插件化能力强，但平台维护成本更高           |
| 路由能力   | 支持 Predicate、Filter、服务发现路由        | 路径、域名、反向代理能力成熟             | 路由、插件、认证、限流能力完整             |
| 运维复杂度 | 适合 Spring Cloud 项目内聚集成              | 运维成熟，性能强                         | 适合企业级 API 管理平台                    |
| 适用重点   | 微服务内部统一入口、Java 体系网关           | 静态资源、反向代理、负载均衡、高性能入口 | API 生命周期管理、插件治理、多租户网关平台 |

在 Spring Boot 3 微服务项目中，如果系统主体技术栈是 Spring Cloud，并且需要使用 Java 编写鉴权、过滤器、路由规则、上下文透传逻辑，Spring Cloud Gateway 更适合放在业务微服务入口层。如果只需要高性能反向代理、TLS 终止、静态资源代理，Nginx 仍然更合适。生产环境中也常见组合方式：外层使用 Nginx 或云负载均衡，内层使用 Spring Cloud Gateway 做微服务路由和业务入口治理。

## 环境准备

本章用于确定开发 Spring Cloud Gateway 前需要准备的 JDK、Spring Boot、Spring Cloud 版本，以及基础 Maven 依赖。版本选择必须以 Spring Boot 与 Spring Cloud Release Train 的兼容关系为准，不能随意混用。

### JDK 与 Spring Boot 版本

Spring Boot 3 最低要求 Java 17，因此开发 Spring Boot 3 + Spring Cloud Gateway 项目时，JDK 至少使用 17。对于新项目，建议直接使用 JDK 17 或 JDK 21，生产环境优先选择 LTS 版本，避免使用过新的非 LTS 版本带来依赖兼容风险。

当前 Spring Boot 3.5.14 已发布，并且 Spring 官方博客说明该版本已发布到 Maven Central。([Home](https://spring.io/blog/2026/04/23/spring-boot-3-5-14-available-now)) 因此新建 Spring Boot 3 Gateway 项目时，可以优先选择 `3.5.x` 的最新维护版本；如果公司已有统一基线，则应以公司基线为准。

推荐版本：

| 环境项       | 推荐值          | 说明                                                 |
| ------------ | --------------- | ---------------------------------------------------- |
| JDK          | 17 或 21        | Spring Boot 3 最低要求 Java 17                       |
| Spring Boot  | 3.5.x           | 新项目优先选择当前维护分支                           |
| 构建工具     | Maven 3.6.3+    | Spring Boot 文档对 Maven 版本有明确要求              |
| 网关运行模型 | WebFlux + Netty | Gateway Server WebFlux 不按传统 Servlet WAR 方式运行 |

需要注意，Spring Cloud Gateway WebFlux 使用的是响应式编程模型。项目中不要直接使用阻塞式写法处理高频请求，例如在过滤器中执行长时间阻塞的数据库查询、远程 HTTP 调用或文件 IO。确实需要调用外部服务时，应控制耗时，并结合缓存、超时、降级等策略处理。

### Spring Cloud 版本选择

Spring Cloud 不是按单个组件独立随意选版本，而是通过 Release Train 管理一组 Spring Cloud 组件的兼容版本。Spring 官方给出了 Spring Cloud Release Train 与 Spring Boot 版本的对应关系：`2025.0.x` 对应 Spring Boot `3.5.x`，`2024.0.x` 对应 Spring Boot `3.4.x`，`2023.0.x` 对应 Spring Boot `3.3.x` 和 `3.2.x`。([Home](https://spring.io/projects/spring-cloud/))

推荐选择如下：

| Spring Boot 版本 | Spring Cloud Release Train | 适用建议                     |
| ---------------- | -------------------------- | ---------------------------- |
| 3.5.x            | 2025.0.x，Northfields      | 新项目优先选择               |
| 3.4.x            | 2024.0.x，Moorgate         | 已有 3.4 项目继续维护可用    |
| 3.3.x / 3.2.x    | 2023.0.x，Leyton           | 老项目维护可用，新项目不优先 |
| 3.0.x / 3.1.x    | 2022.0.x，Kilburn          | 已进入不推荐新项目使用范围   |

如果当前项目明确使用 Spring Boot 3.5.x，建议 Spring Cloud 使用 `2025.0.2` 或该 release train 的最新 service release。Spring Cloud 2025.0.2 发布记录中，Spring Cloud Gateway 对应版本为 `4.3.4`。([GitHub](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2025.0-Release-Notes))

Spring Cloud Gateway 5.x 已经面向 Spring Boot 4 和 Spring Framework 7，而本大纲主题是 Spring Boot 3，因此不要在 Spring Boot 3 项目中直接选择 Gateway 5.x。Spring Cloud Gateway 当前文档中也展示了 Gateway 5.0.1、4.3.4、4.2.7、4.1.9 等稳定版本，其中 4.3.x 才是 Spring Boot 3.5 线更合适的选择。([Home](https://docs.spring.io/spring-cloud-gateway/reference/))

### 项目依赖配置

本节先给出 Gateway 基础工程的 Maven 依赖配置。这里使用 Spring Boot `3.5.14`、Spring Cloud `2025.0.2`，并采用新的 Gateway WebFlux Starter：`spring-cloud-starter-gateway-server-webflux`。Spring Cloud Gateway 官方文档说明，Gateway WebFlux Starter 的 groupId 为 `org.springframework.cloud`，artifactId 为 `spring-cloud-starter-gateway-server-webflux`。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/starter.html))

文件位置：`pom.xml`

下面的配置用于创建一个基础 Gateway 服务，包含 Gateway WebFlux、负载均衡、Actuator、Hutool、Lombok 和测试依赖。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 统一管理插件和常用依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-cloud-gateway-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-cloud-gateway-demo</name>
    <description>Spring Boot 3 Spring Cloud Gateway 示例工程</description>

    <properties>
        <!-- Spring Boot 3 最低要求 JDK 17 -->
        <java.version>17</java.version>

        <!-- Spring Boot 3.5.x 对应 Spring Cloud 2025.0.x -->
        <spring-cloud.version>2025.0.2</spring-cloud.version>

        <!-- Hutool 工具类版本 -->
        <hutool.version>5.8.40</hutool.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud BOM：统一管理 Gateway、LoadBalancer 等 Spring Cloud 组件版本 -->
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
        <!-- Spring Cloud Gateway WebFlux 网关核心依赖，基于 WebFlux + Reactor Netty -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
        </dependency>

        <!-- Spring Cloud LoadBalancer：使用 lb://service-name 路由时需要 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>

        <!-- Actuator：用于健康检查、指标监控、网关端点查看等 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Validation：用于配置属性校验、请求参数校验等场景 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Hutool：常用工具类，后续过滤器、字符串处理、集合处理可使用 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：简化日志对象、构造器、Getter、Setter 等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Test：单元测试和集成测试基础依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Reactor Test：测试 WebFlux、Mono、Flux 等响应式逻辑 -->
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件：用于打包可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <!-- 打包时排除 Lombok，避免无意义依赖进入运行包 -->
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

依赖配置完成后，可以先执行 Maven 依赖检查，确认 Spring Boot、Spring Cloud 和 Gateway 版本能够正常解析。

```bash
mvn -U clean dependency:tree
```

如果项目使用 Spring Boot `3.5.x` 与 Spring Cloud `2025.0.x`，后续配置文件中的 Gateway 配置建议优先使用新前缀：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service
              uri: http://localhost:8081
              predicates:
                - Path=/api/user/**
```

从 Spring Cloud 2025.0 开始，Gateway 新模块和新 Starter 名称已经明确区分 WebFlux、WebMVC、Server、Proxy Exchange 等模式；同时配置前缀也迁移到了更明确的新命名空间，例如 WebFlux Server 推荐使用 `spring.cloud.gateway.server.webflux.*`。([GitHub](https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2025.0-Release-Notes))

## 基础工程搭建

本章用于搭建 Spring Cloud Gateway 的最小可运行工程，包括 Maven 项目结构、启动类和基础配置文件。这里延续大纲中的 `基础工程搭建` 部分，不展开路由断言、过滤器、鉴权、限流等后续内容。

### Maven 项目结构

Gateway 工程建议作为独立微服务存在，例如 `gateway-service`。它只负责统一入口、路由转发、过滤器处理和基础网关能力，不建议把业务 Controller、业务 Service、Mapper 等复杂业务代码放在网关工程中。

推荐项目结构如下：

```text
spring-cloud-gateway-demo
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               └── gateway
│   │   │                   ├── GatewayApplication.java
│   │   │                   └── config
│   │   │                       └── WebFluxConfig.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── gateway
│                           └── GatewayApplicationTests.java
└── target
```

核心目录说明如下：

| 路径                                     | 说明                                                      |
| ---------------------------------------- | --------------------------------------------------------- |
| `pom.xml`                                | Maven 依赖、Spring Boot 版本、Spring Cloud BOM 版本配置   |
| `src/main/java/io/github/atengk/gateway` | 网关服务 Java 代码根包                                    |
| `GatewayApplication.java`                | Gateway 服务启动类                                        |
| `config`                                 | 网关公共配置，例如跨域、JSON、WebFlux 相关配置            |
| `src/main/resources/application.yml`     | 通用配置文件，放置应用名、端口、基础路由、Actuator 等配置 |
| `application-dev.yml`                    | 开发环境配置                                              |
| `application-prod.yml`                   | 生产环境配置                                              |
| `src/test/java`                          | 单元测试和集成测试代码                                    |

需要注意，Spring Cloud Gateway WebFlux 依赖 Spring Boot、Spring WebFlux、Project Reactor 和 Netty 运行时，不适合按传统 Servlet WAR 包方式部署；官方文档也明确说明它不能运行在传统 Servlet 容器中。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/starter.html?utm_source=chatgpt.com)) 因此 Gateway 工程中不要同时引入 `spring-boot-starter-web`，避免 WebMVC 与 WebFlux 栈混用导致运行模型混乱。

### Gateway 启动类

启动类是 Gateway 服务的入口，保持简洁即可。它不需要加 `@EnableDiscoveryClient`，在 Spring Cloud 新版本中，服务发现能力通常由依赖和自动配置完成。后续接入 Nacos 时，只需要补充 Nacos Discovery 依赖和配置。

文件位置：`src/main/java/io/github/atengk/gateway/GatewayApplication.java`

下面的启动类用于启动 Spring Cloud Gateway 服务。

```java
package io.github.atengk.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud Gateway 启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@SpringBootApplication
public class GatewayApplication {

    /**
     * 启动网关服务
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        log.info("Spring Cloud Gateway 网关服务启动成功");
    }

}
```

如果项目后续需要统一处理 WebFlux 相关扩展配置，可以提前保留一个配置类。当前配置类不写复杂逻辑，只作为后续跨域、编码器、响应处理等配置的扩展位置。

文件位置：`src/main/java/io/github/atengk/gateway/config/WebFluxConfig.java`

下面的配置类用于预留 WebFlux 扩展配置入口。

```java
package io.github.atengk.gateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * WebFlux 扩展配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class WebFluxConfig {

}
```

启动类完成后，可以在项目根目录执行以下命令验证工程能否正常编译：

```bash
mvn clean compile
```

如果编译成功，说明 Maven 依赖、Java 版本、包路径和启动类基本正确。后续如果启动失败，优先检查是否误引入了 `spring-boot-starter-web`、Spring Boot 与 Spring Cloud 版本是否匹配，以及 Gateway 配置前缀是否写错。

### 基础配置文件

基础配置文件用于配置服务端口、应用名称、运行环境、基础路由、日志、Actuator 暴露端点等内容。Spring Cloud Gateway 的路由由 `RouteDefinitionLocator` 体系加载，常见方式是通过配置文件中的路由定义加载。([Spring 企业文档](https://docs.enterprise.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/configuration.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面的配置提供一个最小可运行的 Gateway 示例，包含服务端口、应用名称、基础静态路由、Actuator 和日志配置。

```yaml
server:
  # 网关服务端口，客户端统一访问该端口
  port: 9000

spring:
  application:
    # 服务名称，后续接入注册中心时会作为网关服务名
    name: spring-cloud-gateway-demo

  profiles:
    # 默认启用开发环境配置
    active: dev

  cloud:
    gateway:
      server:
        webflux:
          # Gateway 总开关，默认 true；显式声明便于排查配置
          enabled: true

          # 路由配置列表
          routes:
            - id: user-service-route
              # 示例：直接转发到本地用户服务
              uri: http://localhost:8081
              predicates:
                # 匹配 /api/user/** 请求
                - Path=/api/user/**
              filters:
                # 转发到下游服务前，去掉前 1 层路径，即 /api/user/list -> /user/list
                - StripPrefix=1

            - id: order-service-route
              # 示例：直接转发到本地订单服务
              uri: http://localhost:8082
              predicates:
                # 匹配 /api/order/** 请求
                - Path=/api/order/**
              filters:
                # 转发到下游服务前，去掉 /api 前缀
                - StripPrefix=1

          httpclient:
            # 连接下游服务超时时间，单位毫秒
            connect-timeout: 3000

            # Netty HttpClient 响应超时时间
            response-timeout: 10s

management:
  endpoints:
    web:
      exposure:
        # 开发环境暴露常用监控端点，生产环境不建议直接使用 "*"
        include: health,info,metrics,gateway

  endpoint:
    health:
      # 展示详细健康检查信息，生产环境可调整为 when_authorized
      show-details: always

logging:
  level:
    # Gateway 核心日志
    org.springframework.cloud.gateway: info

    # Reactor Netty 访问日志排查时可临时调整为 debug
    reactor.netty: info

    # 当前项目日志
    io.github.atengk.gateway: debug
```

Spring Cloud Gateway 的配置属性中包含 `spring.cloud.gateway.server.webflux.routes`、`spring.cloud.gateway.server.webflux.enabled`、`spring.cloud.gateway.server.webflux.httpclient.connect-timeout` 等配置项，适用于 Gateway Server WebFlux 模式。([Home](https://docs.spring.io/spring-cloud-gateway/reference/configprops.html?utm_source=chatgpt.com)) 如果你使用的是 Spring Cloud 2025.0.x 和 Gateway 4.3.x，建议优先使用 `spring.cloud.gateway.server.webflux.*` 这一类新配置前缀。

开发环境可以单独维护 `application-dev.yml`，用于放置本地服务地址、调试日志、开发环境开关等配置。

文件位置：`src/main/resources/application-dev.yml`

下面的配置用于开发环境覆盖通用配置。

```yaml
server:
  # 开发环境网关端口
  port: 9000

logging:
  level:
    # 开发环境便于观察路由匹配和过滤器执行情况
    org.springframework.cloud.gateway: debug
    io.github.atengk.gateway: debug
```

生产环境建议单独维护 `application-prod.yml`，避免开发环境的调试日志、监控端点暴露策略直接进入生产。

文件位置：`src/main/resources/application-prod.yml`

下面的配置用于生产环境收敛日志和监控端点。

```yaml
server:
  # 生产环境端口可由启动参数或环境变量覆盖
  port: 9000

management:
  endpoints:
    web:
      exposure:
        # 生产环境只暴露必要端点
        include: health,info

  endpoint:
    health:
      # 生产环境不直接展示详细健康信息
      show-details: never

logging:
  level:
    # 生产环境减少 Gateway 调试日志
    org.springframework.cloud.gateway: warn
    reactor.netty: warn
    io.github.atengk.gateway: info
```

配置完成后，可以使用以下命令启动开发环境：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

也可以先打包，再通过 Jar 启动：

```bash
mvn clean package -DskipTests

java -jar target/spring-cloud-gateway-demo-1.0.0.jar --spring.profiles.active=dev
```

启动成功后，可以访问健康检查接口验证服务是否正常：

```bash
curl http://localhost:9000/actuator/health
```

如果下游用户服务运行在 `8081`，可以通过网关测试路由转发：

```bash
curl http://localhost:9000/api/user/test
```

如果下游服务未启动，网关通常会返回连接失败或 `5xx` 相关错误。此时说明网关已匹配路由，但无法连接到目标服务，需要检查 `uri` 地址、端口、下游服务状态和本机网络。

## 路由配置

本章用于说明 Spring Cloud Gateway 中最基础的路由配置方式。路由由 `id`、`uri`、`predicates`、`filters` 等部分组成，其中 `predicates` 用于判断请求是否命中当前路由，多个断言同时配置时通常按逻辑与组合，全部匹配后才会转发。该章节对应你给出的 `路由配置` 部分。 Spring Cloud Gateway 官方文档也说明，内置 Route Predicate Factory 会基于 HTTP 请求的不同属性进行匹配，并且多个 Predicate 可以组合使用。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/request-predicates-factories.html?utm_source=chatgpt.com))

### 静态路由配置

静态路由是最基础的路由方式，直接在 `application.yml` 中写死目标服务地址。它适合本地开发、简单服务代理、固定第三方接口代理，以及暂时没有接入注册中心的项目。

文件位置：`src/main/resources/application.yml`

下面的配置定义了两个静态路由，分别转发用户服务和订单服务请求。

```yaml
server:
  # 网关统一入口端口
  port: 9000

spring:
  application:
    # 网关服务名称
    name: spring-cloud-gateway-demo

  cloud:
    gateway:
      server:
        webflux:
          routes:
            # 用户服务静态路由
            - id: user-service-route
              # 固定转发地址，适合本地开发或固定后端服务
              uri: http://localhost:8081
              predicates:
                # 匹配用户服务接口
                - Path=/api/user/**
              filters:
                # 去掉第 1 层路径：/api/user/list -> /user/list
                - StripPrefix=1

            # 订单服务静态路由
            - id: order-service-route
              # 固定转发地址，适合本地开发或固定后端服务
              uri: http://localhost:8082
              predicates:
                # 匹配订单服务接口
                - Path=/api/order/**
              filters:
                # 去掉第 1 层路径：/api/order/list -> /order/list
                - StripPrefix=1
```

路由字段说明如下：

| 字段         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| `id`         | 路由唯一标识，建议使用清晰的业务名称                         |
| `uri`        | 请求命中路由后的转发地址，可以是 `http://`、`https://`、`lb://` 等形式 |
| `predicates` | 路由断言，决定请求是否匹配当前路由                           |
| `filters`    | 当前路由生效的过滤器，用于改写路径、请求头、响应头等         |

Spring Cloud Gateway 支持简写配置和完整参数配置两种方式。简写方式使用 `PredicateName=参数1,参数2`，完整方式使用 `name` 和 `args` 明确声明参数。官方文档说明，大多数示例都采用简写方式，因为它更适合 YAML 中的常规路由配置。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.2/spring-cloud-gateway/configuring-route-predicate-factories-and-filter-factories.html?utm_source=chatgpt.com))

完整参数写法示例如下：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service-route
              uri: http://localhost:8081
              predicates:
                - name: Path
                  args:
                    patterns: /api/user/**
              filters:
                - name: StripPrefix
                  args:
                    parts: 1
```

开发中优先使用简写形式即可；当参数较多、配置含义不直观，或者需要团队统一规范时，再使用完整参数形式。

### Path 断言配置

`Path` 是最常用的路由断言，用于根据请求路径匹配路由。它适合按照接口前缀拆分服务，例如 `/api/user/**` 转发到用户服务，`/api/order/**` 转发到订单服务。

文件位置：`src/main/resources/application.yml`

下面的配置通过不同路径前缀转发到不同服务。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-path-route
              uri: http://localhost:8081
              predicates:
                # 匹配 /api/user 开头的所有请求
                - Path=/api/user/**
              filters:
                # /api/user/profile -> /user/profile
                - StripPrefix=1

            - id: order-path-route
              uri: http://localhost:8082
              predicates:
                # 匹配 /api/order 开头的所有请求
                - Path=/api/order/**
              filters:
                # /api/order/page -> /order/page
                - StripPrefix=1
```

`Path` 断言支持多个路径模式，也支持路径变量。官方文档说明，`Path` Predicate 接收路径模式列表，以及可选的 `matchTrailingSlash` 参数；路径变量会放入 `ServerWebExchange` 的 attributes 中，后续过滤器可以读取。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/request-predicates-factories.html?utm_source=chatgpt.com))

多个路径模式示例如下：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: multi-path-route
              uri: http://localhost:8081
              predicates:
                # 同一个路由匹配多个路径前缀
                - Path=/api/user/**,/open/user/**
```

路径变量示例如下：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-version-route
              uri: http://localhost:8081
              predicates:
                # 匹配带版本号的路径，例如 /api/v1/user/list
                - Path=/api/{version}/user/**
```

验证命令如下：

```bash
# 命中 user-path-route
curl http://localhost:9000/api/user/profile

# 命中 order-path-route
curl http://localhost:9000/api/order/page

# 命中 multi-path-route
curl http://localhost:9000/open/user/info
```

使用 `Path` 断言时，需要重点关注 `StripPrefix` 的层级。比如 `/api/user/profile` 使用 `StripPrefix=1` 后会变成 `/user/profile`，使用 `StripPrefix=2` 后会变成 `/profile`。如果下游服务接口实际路径是 `/profile`，则应使用 `StripPrefix=2`。

### Method 断言配置

`Method` 断言用于根据 HTTP 请求方法匹配路由，例如只允许 `GET` 请求进入查询服务，只允许 `POST` 请求进入写入服务。官方文档说明，`Method` Predicate 接收一个或多个 HTTP Method 参数，请求方法匹配时才会命中路由。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/request-predicates-factories.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面的配置将用户查询接口和用户写入接口拆成两个路由。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-query-route
              uri: http://localhost:8081
              predicates:
                # 只匹配 GET 请求
                - Path=/api/user/**
                - Method=GET
              filters:
                - StripPrefix=1

            - id: user-write-route
              uri: http://localhost:8081
              predicates:
                # 只匹配 POST、PUT、DELETE 请求
                - Path=/api/user/**
                - Method=POST,PUT,DELETE
              filters:
                - StripPrefix=1
```

验证命令如下：

```bash
# 命中 user-query-route
curl -X GET http://localhost:9000/api/user/list

# 命中 user-write-route
curl -X POST http://localhost:9000/api/user/create \
  -H "Content-Type: application/json" \
  -d '{"username":"ateng","nickname":"阿腾"}'

# 命中 user-write-route
curl -X DELETE http://localhost:9000/api/user/1001
```

`Method` 断言通常和 `Path` 断言一起使用。如果只配置 `Method=GET`，没有配置路径限制，则所有 `GET` 请求都有可能匹配该路由，这在多服务网关中通常不够安全。

### Header 断言配置

`Header` 断言用于根据请求头是否存在，以及请求头值是否符合正则表达式来匹配路由。它适合做版本路由、灰度路由、内部调用标记、客户端类型区分等场景。官方文档说明，`Header` Predicate 接收两个参数：Header 名称和 Java 正则表达式。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/request-predicates-factories.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面的配置通过请求头 `X-Client-Type` 区分来自后台管理端的请求。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: admin-client-route
              uri: http://localhost:8083
              predicates:
                # 匹配后台管理接口路径
                - Path=/api/admin/**
                # 只有 Header 符合要求时才转发
                - Header=X-Client-Type, admin
              filters:
                - StripPrefix=1
```

也可以使用正则表达式做更灵活的匹配，例如匹配版本号请求头。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: api-version-route
              uri: http://localhost:8081
              predicates:
                # 匹配用户服务接口
                - Path=/api/user/**
                # 匹配 X-Api-Version: v1 或 v2
                - Header=X-Api-Version, v[12]
              filters:
                - StripPrefix=1
```

验证命令如下：

```bash
# 命中 admin-client-route
curl http://localhost:9000/api/admin/menu \
  -H "X-Client-Type: admin"

# 不命中 admin-client-route，因为 Header 值不匹配
curl http://localhost:9000/api/admin/menu \
  -H "X-Client-Type: app"

# 命中 api-version-route
curl http://localhost:9000/api/user/list \
  -H "X-Api-Version: v1"
```

`Header` 断言只适合做路由匹配，不等同于完整鉴权。比如 `X-Client-Type: admin` 可以被客户端伪造，不能直接作为管理员身份依据。真正的身份认证应在后续鉴权过滤器中校验 Token、签名或登录态。

### Query 断言配置

`Query` 断言用于根据请求参数匹配路由。它可以只判断参数是否存在，也可以进一步判断参数值是否符合正则表达式。官方文档说明，`Query` Predicate 接收一个必填参数 `param` 和一个可选正则表达式参数 `regexp`。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/request-predicates-factories.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面的配置表示：只有请求中包含 `debug` 参数时，才会转发到调试服务。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: debug-query-route
              uri: http://localhost:8084
              predicates:
                # 匹配调试接口路径
                - Path=/api/debug/**
                # 只要求存在 debug 参数，不限制参数值
                - Query=debug
              filters:
                - StripPrefix=1
```

如果需要限制参数值，可以在第二个参数中写正则表达式。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: channel-query-route
              uri: http://localhost:8081
              predicates:
                # 匹配用户服务接口
                - Path=/api/user/**
                # 只匹配 channel=app 或 channel=admin
                - Query=channel, app|admin
              filters:
                - StripPrefix=1
```

也可以将 `Query` 与 `Method`、`Header` 组合使用，形成更严格的路由条件。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-admin-query-route
              uri: http://localhost:8081
              predicates:
                # 匹配用户接口
                - Path=/api/user/**
                # 只匹配 GET 请求
                - Method=GET
                # 只匹配 channel=admin
                - Query=channel, admin
                # 只匹配后台客户端请求
                - Header=X-Client-Type, admin
              filters:
                - StripPrefix=1
```

验证命令如下：

```bash
# 命中 debug-query-route
curl "http://localhost:9000/api/debug/env?debug=true"

# 不命中 debug-query-route，因为没有 debug 参数
curl "http://localhost:9000/api/debug/env"

# 命中 channel-query-route
curl "http://localhost:9000/api/user/list?channel=app"

# 不命中 channel-query-route，因为 channel 参数值不符合 app|admin
curl "http://localhost:9000/api/user/list?channel=h5"

# 命中 user-admin-query-route
curl "http://localhost:9000/api/user/list?channel=admin" \
  -H "X-Client-Type: admin"
```

`Query` 断言适合处理临时调试、渠道区分、版本灰度、来源标记等场景，但不建议把关键权限控制建立在 Query 参数上。Query 参数容易被篡改，涉及安全的判断应交给鉴权过滤器处理。

## 过滤器开发

本章用于说明 Spring Cloud Gateway 中过滤器的基本开发方式，包括内置 `GatewayFilter`、全局 `GlobalFilter`、自定义 `GatewayFilterFactory`、请求日志过滤器和统一响应处理。过滤器是 Gateway 的核心扩展点，适合处理路径改写、请求头透传、鉴权、日志、限流、异常响应等横切逻辑。该章节对应你给出的 `过滤器开发` 部分。

Spring Cloud Gateway 中的路由过滤器作用于具体路由，可以修改进入下游服务的请求，也可以修改返回客户端的响应；官方文档说明，Gateway 内置了多种 `GatewayFilter Factory`，并且这些过滤器是路由级别的。([Spring 企业文档](https://docs.enterprise.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/gatewayfilter-factories.html?utm_source=chatgpt.com)) 全局过滤器 `GlobalFilter` 会参与所有匹配路由的过滤链，官方文档说明它与路由级 `GatewayFilter` 会合并成同一个过滤链，并按照 `Ordered` 排序执行。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/global-filters.html?utm_source=chatgpt.com))

### 内置 GatewayFilter 使用

内置 `GatewayFilter` 适合处理常见的请求和响应改写逻辑，例如添加请求头、添加响应头、移除敏感请求头、路径前缀处理、路径重写等。它通常直接写在 `application.yml` 的路由配置中，不需要额外编写 Java 代码。

文件位置：`src/main/resources/application.yml`

下面的配置展示了常用内置过滤器的使用方式，包括 `StripPrefix`、`AddRequestHeader`、`SetRequestHeader`、`RemoveRequestHeader`、`AddResponseHeader` 和 `RewritePath`。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-filter-route
              uri: http://localhost:8081
              predicates:
                # 匹配用户服务接口
                - Path=/api/user/**
              filters:
                # 去掉第 1 层路径：/api/user/list -> /user/list
                - StripPrefix=1

                # 给下游服务增加请求头，便于识别请求来源
                - AddRequestHeader=X-Gateway-Source, spring-cloud-gateway

                # 设置请求头；如果已存在同名 Header，则覆盖
                - SetRequestHeader=X-Forwarded-System, ateng-system

                # 移除客户端传入的敏感 Header，避免伪造内部身份
                - RemoveRequestHeader=X-User-Id
                - RemoveRequestHeader=X-Tenant-Id

                # 给客户端响应增加 Header
                - AddResponseHeader=X-Gateway-Response, success

            - id: rewrite-path-route
              uri: http://localhost:8082
              predicates:
                # 匹配订单接口
                - Path=/gateway/order/**
              filters:
                # 使用正则重写路径：/gateway/order/list -> /order/list
                - RewritePath=/gateway/(?<segment>.*), /${segment}
```

`StripPrefix` 的参数 `parts` 表示转发到下游前移除几个路径段；例如配置 `StripPrefix=2` 时，`/name/blue/red` 转发后会变成 `/red`。([Spring 企业文档](https://docs.enterprise.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/gatewayfilter-factories/stripprefix-factory.html?utm_source=chatgpt.com)) `AddRequestHeader` 接收请求头名称和值两个参数，用于给所有匹配请求增加下游请求头；官方文档也说明它可以识别路径或主机匹配中的 URI 变量。([Spring 企业文档](https://docs.enterprise.spring.io/spring-cloud-gateway/reference/4.1-SNAPSHOT/spring-cloud-gateway/gatewayfilter-factories/addrequestheader-factory.html?utm_source=chatgpt.com)) `AddResponseHeader` 用于给匹配路由的响应增加响应头，并支持控制是否覆盖已有响应头。([Spring 企业文档](https://docs.enterprise.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/gatewayfilter-factories/addresponseheader-factory.html?utm_source=chatgpt.com))

常用内置过滤器说明如下：

| 过滤器                 | 作用                 | 示例                                 |
| ---------------------- | -------------------- | ------------------------------------ |
| `StripPrefix`          | 去掉请求路径前缀     | `/api/user/list -> /user/list`       |
| `PrefixPath`           | 给请求路径增加前缀   | `/list -> /api/list`                 |
| `RewritePath`          | 使用正则重写路径     | `/gateway/order/list -> /order/list` |
| `AddRequestHeader`     | 增加下游请求头       | `X-Gateway-Source`                   |
| `SetRequestHeader`     | 设置或覆盖下游请求头 | `X-Forwarded-System`                 |
| `RemoveRequestHeader`  | 移除请求头           | 移除伪造的 `X-User-Id`               |
| `AddResponseHeader`    | 增加响应头           | `X-Gateway-Response`                 |
| `RemoveResponseHeader` | 移除响应头           | 移除下游敏感响应头                   |
| `RequestSize`          | 限制请求体大小       | 上传接口保护                         |
| `Retry`                | 请求失败重试         | 临时网络异常重试                     |

验证命令如下：

```bash
# 验证 StripPrefix、AddRequestHeader、RemoveRequestHeader
curl -i http://localhost:9000/api/user/list \
  -H "X-User-Id: fake-user-id"

# 验证 RewritePath
curl -i http://localhost:9000/gateway/order/list
```

如果需要确认下游服务实际收到的路径和 Header，可以在下游服务临时打印请求路径和请求头。生产环境不建议打印完整 Header，尤其是 `Authorization`、`Cookie`、`Token` 等敏感信息。

### GlobalFilter 全局过滤器

`GlobalFilter` 会对所有匹配到路由的请求生效，适合处理请求链路追踪、统一 Header 注入、请求耗时统计、全局日志、基础鉴权等逻辑。官方文档说明，全局过滤器和路由级过滤器会按 `Ordered` 组合排序；优先级最高的过滤器在前置阶段最先执行，在后置阶段最后执行。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/global-filters.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/gateway/filter/GatewayTraceGlobalFilter.java`

下面的全局过滤器用于生成请求链路 ID，并将链路 ID 透传给下游服务。

```java
package io.github.atengk.gateway.filter;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关链路追踪全局过滤器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class GatewayTraceGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 请求链路 ID Header
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 处理网关请求
     *
     * @param exchange ServerWebExchange
     * @param chain    GatewayFilterChain
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
        }

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(TRACE_ID_HEADER, traceId)
                .build();

        log.debug("网关链路追踪开始，traceId={}", traceId);

        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signalType -> log.debug("网关链路追踪结束，traceId={}, signal={}", traceId, signalType));
    }

    /**
     * 获取过滤器执行顺序
     *
     * @return 顺序值，值越小优先级越高
     */
    @Override
    public int getOrder() {
        return -100;
    }

}
```

该过滤器的执行逻辑如下：

| 步骤              | 说明                                           |
| ----------------- | ---------------------------------------------- |
| 读取 `X-Trace-Id` | 如果客户端或上游已经传入，则继续使用           |
| 生成链路 ID       | 如果没有传入，则使用 Hutool 生成 UUID          |
| 修改请求头        | 将 `X-Trace-Id` 写入下游请求                   |
| 执行过滤链        | 调用 `chain.filter(exchange)` 继续后续路由流程 |
| 输出调试日志      | 在请求完成后记录过滤器结束状态                 |

启动后可以使用以下命令验证：

```bash
# 不传 X-Trace-Id，由网关生成
curl -i http://localhost:9000/api/user/list

# 手动传入 X-Trace-Id，网关继续透传
curl -i http://localhost:9000/api/user/list \
  -H "X-Trace-Id: test-trace-001"
```

全局过滤器不建议滥用。对于只作用于某个路由的逻辑，应优先使用路由级 `GatewayFilter` 或自定义 `GatewayFilterFactory`；对于所有请求都必须执行的逻辑，才放到 `GlobalFilter` 中。

### 自定义 GatewayFilterFactory

自定义 `GatewayFilterFactory` 适合封装可配置、可复用、路由级别的过滤逻辑。例如某些内部接口要求必须携带指定 Header，或者某些路由需要单独校验渠道、版本、签名等信息。它和 `GlobalFilter` 的区别是：`GatewayFilterFactory` 只对配置了该过滤器的路由生效。

文件位置：`src/main/java/io/github/atengk/gateway/filter/factory/CheckHeaderGatewayFilterFactory.java`

下面的过滤器工厂用于校验指定请求头是否存在，并可选校验请求头值是否匹配。

```java
package io.github.atengk.gateway.filter.factory;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.gateway.support.GatewayResponseWriter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 请求头校验过滤器工厂
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class CheckHeaderGatewayFilterFactory extends AbstractGatewayFilterFactory<CheckHeaderGatewayFilterFactory.Config> {

    public CheckHeaderGatewayFilterFactory() {
        super(Config.class);
    }

    /**
     * 创建路由过滤器
     *
     * @param config 过滤器配置
     * @return GatewayFilter
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String headerName = config.getHeaderName();
            String requiredValue = config.getRequiredValue();
            String actualValue = exchange.getRequest().getHeaders().getFirst(headerName);

            if (StrUtil.isBlank(headerName)) {
                log.warn("请求头校验配置错误，headerName 不能为空");
                return GatewayResponseWriter.write(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "网关过滤器配置错误");
            }

            if (StrUtil.isBlank(actualValue)) {
                log.warn("请求头校验失败，缺少请求头，headerName={}", headerName);
                return GatewayResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, "缺少必要请求头");
            }

            if (StrUtil.isNotBlank(requiredValue) && !StrUtil.equals(requiredValue, actualValue)) {
                log.warn("请求头校验失败，请求头值不匹配，headerName={}", headerName);
                return GatewayResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, "请求头校验失败");
            }

            log.debug("请求头校验通过，headerName={}", headerName);
            return chain.filter(exchange);
        };
    }

    /**
     * 配置简写参数顺序
     *
     * @return 参数名称列表
     */
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("headerName", "requiredValue");
    }

    /**
     * 请求头校验配置
     *
     * @author Ateng
     * @since 2026-05-05
     */
    @Data
    public static class Config {

        /**
         * 请求头名称
         */
        private String headerName;

        /**
         * 期望的请求头值，为空时只校验请求头是否存在
         */
        private String requiredValue;

    }

}
```

上面的代码依赖统一响应工具类，后面的 `统一响应处理` 小节会给出完整代码。如果你只想先验证 `CheckHeaderGatewayFilterFactory`，可以先创建该工具类，再配置路由。

文件位置：`src/main/resources/application.yml`

下面的配置将自定义过滤器应用到后台接口路由上。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: admin-check-header-route
              uri: http://localhost:8083
              predicates:
                # 匹配后台管理接口
                - Path=/api/admin/**
              filters:
                # 去掉 /api 前缀
                - StripPrefix=1

                # 自定义过滤器：要求请求头 X-Internal-Token 的值为 ateng-demo-token
                - CheckHeader=X-Internal-Token,ateng-demo-token
```

需要注意，自定义工厂类名称必须符合 Spring Cloud Gateway 的命名约定。类名 `CheckHeaderGatewayFilterFactory` 在 YAML 中使用时，过滤器名称就是 `CheckHeader`。如果类名是 `AuthTokenGatewayFilterFactory`，配置名就是 `AuthToken`。

验证命令如下：

```bash
# 校验通过
curl -i http://localhost:9000/api/admin/menu \
  -H "X-Internal-Token: ateng-demo-token"

# 校验失败：缺少 Header
curl -i http://localhost:9000/api/admin/menu

# 校验失败：Header 值不匹配
curl -i http://localhost:9000/api/admin/menu \
  -H "X-Internal-Token: wrong-token"
```

自定义 `GatewayFilterFactory` 适合做“某些路由需要、某些路由不需要”的逻辑。后续鉴权章节中的白名单、Token 校验、用户信息透传，也可以基于这种模式进一步扩展。

### 请求日志过滤器

请求日志过滤器用于记录网关请求的关键链路信息，例如请求方法、请求路径、路由 ID、响应状态码、耗时、客户端 IP 和链路 ID。日志内容应足够支持排查问题，但不要记录完整 Token、Cookie、请求体和响应体，避免敏感信息泄露。

文件位置：`src/main/java/io/github/atengk/gateway/filter/RequestLogGlobalFilter.java`

下面的全局过滤器用于记录网关请求日志，适合开发和生产环境保留。

```java
package io.github.atengk.gateway.filter;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * 网关请求日志全局过滤器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class RequestLogGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 请求链路 ID Header
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 处理网关请求日志
     *
     * @param exchange ServerWebExchange
     * @param chain    GatewayFilterChain
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        String method = request.getMethod().name();
        String path = request.getURI().getRawPath();
        String clientIp = getClientIp(request);

        return chain.filter(exchange)
                .doOnError(ex -> log.error("网关请求异常，traceId={}, method={}, path={}, clientIp={}, error={}",
                        traceId, method, path, clientIp, ex.getMessage(), ex))
                .doFinally(signalType -> {
                    stopWatch.stop();

                    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                    String routeId = Objects.nonNull(route) ? route.getId() : "-";
                    HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
                    int status = Objects.nonNull(statusCode) ? statusCode.value() : 0;

                    log.info("网关请求完成，traceId={}, routeId={}, method={}, path={}, status={}, cost={}ms, clientIp={}",
                            StrUtil.blankToDefault(traceId, "-"),
                            routeId,
                            method,
                            path,
                            status,
                            stopWatch.getTotalTimeMillis(),
                            clientIp);
                });
    }

    /**
     * 获取过滤器执行顺序
     *
     * @return 顺序值
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 获取客户端 IP
     *
     * @param request 请求对象
     * @return 客户端 IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            return StrUtil.splitTrim(forwardedFor, ",").getFirst();
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (Objects.isNull(remoteAddress) || Objects.isNull(remoteAddress.getAddress())) {
            return "-";
        }

        return remoteAddress.getAddress().getHostAddress();
    }

}
```

日志输出示例：

```text
网关请求完成，traceId=9d7b3c8e7c4f4b8d9bb51a0a2d0a1b2c, routeId=user-service-route, method=GET, path=/api/user/list, status=200, cost=38ms, clientIp=127.0.0.1
```

验证命令如下：

```bash
curl -i http://localhost:9000/api/user/list \
  -H "X-Trace-Id: log-test-001"
```

请求日志过滤器建议注意以下几点：

| 注意项            | 说明                                                     |
| ----------------- | -------------------------------------------------------- |
| 不记录敏感 Header | 不直接打印 `Authorization`、`Cookie`、`Token`            |
| 不记录完整请求体  | 请求体可能很大，也可能包含密码、身份证、手机号等敏感信息 |
| 保留链路 ID       | 便于和下游服务日志串联排查                               |
| 保留路由 ID       | 便于判断请求命中了哪个路由                               |
| 控制日志级别      | 生产环境核心访问日志用 `info`，详细调试日志用 `debug`    |

如果你已经保留了 `GatewayTraceGlobalFilter`，建议让它的 `getOrder()` 值小于请求日志过滤器，保证请求日志中能读取到链路 ID。

### 统一响应处理

统一响应处理主要处理两类场景：第一类是网关过滤器主动拦截后返回错误，例如鉴权失败、请求头缺失、限流拒绝；第二类是网关自身异常，例如路由处理异常、过滤器运行异常等。对于已经由下游服务正常返回的业务响应，不建议在全局过滤器中强行包装响应体，否则可能影响文件下载、流式响应、SSE、图片、网关代理透明性等场景。

如果确实需要修改响应体，Spring Cloud Gateway 官方文档说明 `ModifyResponseBody` 可以在响应返回客户端前修改响应体，但它只能通过 Java DSL 配置。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/gatewayfilter-factories/modifyresponsebody-factory.html?utm_source=chatgpt.com)) 因此本节先给出网关主动响应的统一工具类，再给出全局异常响应处理。

文件位置：`src/main/java/io/github/atengk/gateway/support/GatewayResponseWriter.java`

下面的工具类用于在过滤器或异常处理器中统一返回 JSON 响应。

```java
package io.github.atengk.gateway.support;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 网关响应写入工具类
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class GatewayResponseWriter {

    /**
     * 请求链路 ID Header
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private GatewayResponseWriter() {
    }

    /**
     * 写入统一 JSON 响应
     *
     * @param exchange   ServerWebExchange
     * @param statusCode HTTP 状态码
     * @param message    响应消息
     * @return Mono<Void>
     */
    public static Mono<Void> write(ServerWebExchange exchange, HttpStatusCode statusCode, String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        String path = exchange.getRequest().getURI().getRawPath();

        Map<String, Object> body = MapUtil.newHashMap(true);
        body.put("code", statusCode.value());
        body.put("message", StrUtil.blankToDefault(message, "网关请求处理失败"));
        body.put("path", path);
        body.put("traceId", StrUtil.blankToDefault(traceId, "-"));
        body.put("timestamp", DateUtil.now());

        byte[] bytes = JSONUtil.toJsonStr(body).getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = exchange.getResponse().bufferFactory().wrap(bytes);

        exchange.getResponse().setStatusCode(statusCode);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

}
```

统一响应示例：

```json
{
  "path": "/api/admin/menu",
  "traceId": "log-test-001",
  "code": 401,
  "message": "请求头校验失败",
  "timestamp": "2026-05-05 14:30:00"
}
```

文件位置：`src/main/java/io/github/atengk/gateway/handler/GatewayErrorWebExceptionHandler.java`

下面的异常处理器用于兜底处理网关自身异常，并返回统一 JSON 响应。

```java
package io.github.atengk.gateway.handler;

import io.github.atengk.gateway.support.GatewayResponseWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * 网关全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Order(-2)
@Component
public class GatewayErrorWebExceptionHandler implements WebExceptionHandler {

    /**
     * 处理网关异常
     *
     * @param exchange ServerWebExchange
     * @param ex       异常对象
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatusCode statusCode = getStatusCode(ex);
        String path = exchange.getRequest().getURI().getRawPath();

        log.error("网关统一异常处理，path={}, status={}, error={}",
                path, statusCode.value(), ex.getMessage(), ex);

        return GatewayResponseWriter.write(exchange, statusCode, getMessage(statusCode));
    }

    /**
     * 获取响应状态码
     *
     * @param ex 异常对象
     * @return HTTP 状态码
     */
    private HttpStatusCode getStatusCode(Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 获取响应消息
     *
     * @param statusCode HTTP 状态码
     * @return 响应消息
     */
    private String getMessage(HttpStatusCode statusCode) {
        if (HttpStatus.NOT_FOUND.value() == statusCode.value()) {
            return "网关路由不存在";
        }
        if (HttpStatus.SERVICE_UNAVAILABLE.value() == statusCode.value()) {
            return "下游服务暂不可用";
        }
        if (HttpStatus.GATEWAY_TIMEOUT.value() == statusCode.value()) {
            return "下游服务响应超时";
        }
        return "网关服务异常";
    }

}
```

如果只想对个别路由进行响应体改写，可以使用 Java DSL 配置 `ModifyResponseBody`。该方式适合内部接口统一包装、简单字段脱敏、响应内容标准化等场景，但不建议对所有路由无差别启用。

文件位置：`src/main/java/io/github/atengk/gateway/config/ResponseRouteConfig.java`

下面的配置演示如何对指定路由的响应体做简单包装。

```java
package io.github.atengk.gateway.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 响应处理路由配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class ResponseRouteConfig {

    /**
     * 配置响应体改写路由
     *
     * @param builder RouteLocatorBuilder
     * @return RouteLocator
     */
    @Bean
    public RouteLocator responseBodyRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-response-wrap-route", route -> route
                        .path("/wrap/user/**")
                        .filters(filter -> filter
                                .stripPrefix(1)
                                .modifyResponseBody(String.class, String.class, (exchange, body) -> {
                                    if (body == null) {
                                        return Mono.empty();
                                    }

                                    Map<String, Object> result = MapUtil.newHashMap(true);
                                    result.put("code", 200);
                                    result.put("message", "操作成功");
                                    result.put("data", JSONUtil.parse(body));

                                    return Mono.just(JSONUtil.toJsonStr(result));
                                }))
                        .uri("http://localhost:8081"))
                .build();
    }

}
```

验证命令如下：

```bash
# 验证过滤器主动返回统一响应
curl -i http://localhost:9000/api/admin/menu

# 验证网关异常统一响应，可以访问一个不存在的路由
curl -i http://localhost:9000/not-found-test

# 验证 Java DSL 响应体包装路由
curl -i http://localhost:9000/wrap/user/list
```

统一响应处理建议遵循以下原则：

| 原则                 | 说明                                                   |
| -------------------- | ------------------------------------------------------ |
| 网关拦截响应统一     | 鉴权失败、限流拒绝、请求头缺失等由网关主动返回统一格式 |
| 下游业务响应慎重改写 | 不建议全局包装所有下游响应，避免破坏代理透明性         |
| 异常响应兜底处理     | 网关自身异常应有统一 JSON 响应，避免直接暴露异常堆栈   |
| 文件和流式响应排除   | 文件下载、图片、SSE、WebSocket 不应做 JSON 包装        |
| 保留链路 ID          | 所有错误响应建议带上 `traceId`，便于日志排查           |

## 服务发现集成

本章用于将 Spring Cloud Gateway 接入 Nacos，实现服务注册、服务发现、基于服务名的路由转发和负载均衡。该章节对应你给出的 `服务发现集成` 部分。

前面章节使用的是静态地址，例如 `http://localhost:8081`。这种方式适合本地开发，但不适合微服务生产环境。接入 Nacos 后，网关不再关心具体服务实例 IP 和端口，而是通过服务名转发，例如 `lb://user-service`。

### Nacos 服务注册与发现

Nacos 在这里承担注册中心角色。网关服务启动后会注册到 Nacos，后端业务服务也注册到 Nacos。Gateway 转发请求时，通过服务名从注册中心获取可用实例，再由 Spring Cloud LoadBalancer 完成实例选择。

Spring Cloud Alibaba 官方版本说明中，`2025.0.x` 分支适配 Spring Boot `3.5.x` 与 Spring Cloud `2025.0.x`，其中 `2025.0.0.0` 对应 Nacos `3.0.3`。因此 Spring Boot 3.5.x 项目接入 Nacos 时，建议优先使用 Spring Cloud Alibaba `2025.0.x` 分支；如果项目已经升级到更高的 Spring Cloud service release，应先通过依赖树和集成测试确认兼容性。([Spring Cloud Alibaba](https://sca.aliyun.com/en/docs/2025.x/overview/version-explain/?utm_source=chatgpt.com))

文件位置：`pom.xml`

在前面 Gateway 基础依赖的基础上，增加 Spring Cloud Alibaba BOM 和 Nacos Discovery 依赖。

```xml
<properties>
    <!-- Spring Boot 3 最低要求 JDK 17 -->
    <java.version>17</java.version>

    <!-- Spring Boot 3.5.x 对应 Spring Cloud 2025.0.x -->
    <spring-cloud.version>2025.0.2</spring-cloud.version>

    <!-- Spring Cloud Alibaba 2025.0.x 适配 Spring Boot 3.5.x 与 Spring Cloud 2025.0.x -->
    <spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>

    <!-- Hutool 工具类版本 -->
    <hutool.version>5.8.40</hutool.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Spring Cloud BOM：统一管理 Gateway、LoadBalancer 等组件版本 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Spring Cloud Alibaba BOM：统一管理 Nacos、Sentinel、RocketMQ 等组件版本 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Spring Cloud Gateway WebFlux 网关核心依赖 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
    </dependency>

    <!-- Spring Cloud LoadBalancer：支持 lb://service-name 负载均衡路由 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>

    <!-- Nacos Discovery：服务注册与服务发现 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- Actuator：健康检查、指标监控、Gateway 端点查看 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Validation：配置属性校验、参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：Token、字符串、集合、JSON 等工具能力 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：简化日志对象、Getter、Setter 等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

接入 Nacos 后，启动类可以增加 `@EnableDiscoveryClient`，用于明确启用服务发现能力。Nacos 官方 Spring Cloud 快速开始文档中也使用该注解开启服务发现。([Nacos 官网](https://nacos.io/en/docs/quick-start-spring-cloud?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/gateway/GatewayApplication.java`

下面的启动类用于启动网关服务，并启用服务发现。

```java
package io.github.atengk.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Cloud Gateway 启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    /**
     * 启动网关服务
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        log.info("Spring Cloud Gateway 网关服务启动成功");
    }

}
```

文件位置：`src/main/resources/application.yml`

下面的配置用于将 Gateway 注册到 Nacos，并启用 Nacos 服务发现。

```yaml
server:
  # 网关服务端口
  port: 9000

spring:
  application:
    # 注册到 Nacos 的服务名
    name: gateway-service

  profiles:
    # 默认启用开发环境
    active: dev

  cloud:
    nacos:
      discovery:
        # Nacos 服务端地址
        server-addr: 127.0.0.1:8848

        # 注册到 Nacos 的服务名，默认取 spring.application.name
        service: ${spring.application.name}

        # 命名空间，开发环境可使用 public；生产建议按环境隔离
        namespace: public

        # 分组，建议按系统或环境规划
        group: DEFAULT_GROUP

        # 是否启用服务发现
        enabled: true

        # 是否将当前服务注册到 Nacos
        register-enabled: true

        # Nacos 认证用户名，未开启认证时可以不配置
        username: nacos

        # Nacos 认证密码，生产环境建议使用环境变量注入
        password: nacos

management:
  endpoints:
    web:
      exposure:
        # 开发环境开放必要监控端点
        include: health,info,gateway
```

开发环境可以使用本地 Nacos 单机模式。下面命令用于快速启动一个 Nacos 容器，版本号应与 Spring Cloud Alibaba 组件关系保持一致。

```bash
docker run -d \
  --name nacos-standalone \
  -e MODE=standalone \
  -e NACOS_AUTH_ENABLE=false \
  -p 8848:8848 \
  -p 9848:9848 \
  nacos/nacos-server:v3.0.3
```

启动 Gateway 后，可以在 Nacos 控制台查看 `gateway-service` 是否已经注册。如果业务服务也注册成功，例如 `user-service`、`order-service`，后续就可以使用服务名进行路由转发。

### 基于服务名的动态路由

基于服务名的路由有两种常用方式：一种是手动配置 `lb://service-name`，另一种是开启 DiscoveryClient Route Definition Locator 自动根据注册中心服务生成路由。

第一种方式更常用，也更可控。它仍然在配置文件中明确声明路由规则，只是 `uri` 不再写固定 IP 和端口，而是写服务名。

文件位置：`src/main/resources/application.yml`

下面的配置使用 `lb://user-service` 和 `lb://order-service` 转发请求。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service-route
              # lb:// 表示通过 Spring Cloud LoadBalancer 按服务名选择实例
              uri: lb://user-service
              predicates:
                # 匹配用户服务接口
                - Path=/api/user/**
              filters:
                # /api/user/list -> /user/list
                - StripPrefix=1

            - id: order-service-route
              # 转发到注册中心中的 order-service
              uri: lb://order-service
              predicates:
                # 匹配订单服务接口
                - Path=/api/order/**
              filters:
                # /api/order/page -> /order/page
                - StripPrefix=1
```

Spring Cloud Gateway 官方文档说明，使用 DiscoveryClient 创建的路由默认采用 `lb://service-name` 协议，这表示请求会进行负载均衡；因此需要引入 `org.springframework.cloud:spring-cloud-starter-loadbalancer` 依赖。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.2/spring-cloud-gateway/the-discoveryclient-route-definition-locator.html?utm_source=chatgpt.com)) Gateway 的 LoadBalancer 过滤器也说明，`lb://myservice` 会通过 Spring Cloud `LoadBalancerClient` 将服务名解析为实际主机和端口。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/filters/loadbalancer.html?utm_source=chatgpt.com))

第二种方式是开启注册中心动态路由。开启后，Gateway 会根据注册中心中的服务自动创建路由，适合内部调试或服务数量较多且路由规则统一的场景。

文件位置：`src/main/resources/application.yml`

下面的配置启用 DiscoveryClient 动态路由，并使用 Gateway WebFlux 新配置前缀。Gateway 当前配置属性中，`spring.cloud.gateway.server.webflux.discovery.locator.enabled` 用于开启 DiscoveryClient 网关集成，默认值为 `false`。([Home](https://docs.spring.io/spring-cloud-gateway/reference/configprops.html?utm_source=chatgpt.com))

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          discovery:
            locator:
              # 开启基于注册中心的动态路由
              enabled: true

              # 将服务名转为小写，避免服务名大小写导致路径不匹配
              lower-case-service-id: true
```

默认情况下，动态路由访问格式通常类似下面这样：

```text
/user-service/**
/order-service/**
```

也就是服务名会成为路径前缀。例如：

```bash
# 访问注册中心中的 user-service
curl http://localhost:9000/user-service/user/list

# 访问注册中心中的 order-service
curl http://localhost:9000/order-service/order/page
```

如果希望动态路由统一带 `/api` 前缀，可以自定义 DiscoveryClient 路由的 `predicates` 和 `filters`。Gateway 官方文档说明，DiscoveryClient 路由默认使用 `/serviceId/**` 的 Path 断言，并通过 RewritePath 移除服务 ID 前缀；自定义 predicates 和 filters 时，如果还想保留类似行为，需要显式配置对应规则。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.2/spring-cloud-gateway/the-discoveryclient-route-definition-locator.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面的配置将动态路由格式调整为 `/api/{serviceId}/**`。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          discovery:
            locator:
              # 开启注册中心动态路由
              enabled: true

              # 服务名转小写
              lower-case-service-id: true

              predicates:
                - name: Path
                  args:
                    # 生成路径规则：/api/user-service/**
                    pattern: "'/api/' + serviceId + '/**'"

              filters:
                - name: RewritePath
                  args:
                    # 将 /api/user-service/user/list 重写为 /user/list
                    regexp: "'/api/' + serviceId + '/?(?<remaining>.*)'"
                    replacement: "'/${remaining}'"
```

验证命令如下：

```bash
# 转发到 user-service，并将路径改写为 /user/list
curl http://localhost:9000/api/user-service/user/list

# 转发到 order-service，并将路径改写为 /order/page
curl http://localhost:9000/api/order-service/order/page
```

生产项目中，推荐核心业务接口使用手动 `lb://service-name` 路由，因为它可读性更好，也更容易控制路径、鉴权、限流和灰度规则。动态路由更适合内部环境、快速联调或统一规则明确的服务集群。

### 负载均衡配置

Gateway 使用 `lb://service-name` 时，会通过 Spring Cloud LoadBalancer 从注册中心获取服务实例并选择其中一个实例转发。默认策略通常已经能满足基础轮询需求，但生产环境建议配置超时、健康检查、实例筛选和日志监控。

文件位置：`src/main/resources/application.yml`

下面的配置给出 Gateway 访问下游服务时的常用超时配置。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          httpclient:
            # 连接下游服务超时时间，单位毫秒
            connect-timeout: 3000

            # 下游服务响应超时时间
            response-timeout: 10s

          routes:
            - id: user-service-route
              uri: lb://user-service
              predicates:
                - Path=/api/user/**
              filters:
                - StripPrefix=1

              metadata:
                # 当前路由连接超时时间，单位毫秒
                connect-timeout: 3000

                # 当前路由响应超时时间，单位毫秒
                response-timeout: 10000
```

当服务实例不可用时，Gateway 默认可能返回 `503`。LoadBalancer 文档也说明，当 `ReactorLoadBalancer` 找不到服务实例时，默认返回 `503`。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/filters/loadbalancer.html?utm_source=chatgpt.com)) 因此排查 `503` 时，应优先检查下面几点：

| 检查项                    | 说明                                                         |
| ------------------------- | ------------------------------------------------------------ |
| 服务是否注册              | 在 Nacos 控制台确认 `user-service` 是否存在健康实例          |
| 服务名是否一致            | `lb://user-service` 必须和 Nacos 中的服务名一致              |
| 命名空间是否一致          | Gateway 和业务服务必须处于同一个 Nacos namespace，或明确配置跨命名空间 |
| 分组是否一致              | `group` 不一致时可能发现不到服务                             |
| 实例是否健康              | 下游服务异常、心跳失败、端口不通都会导致不可用               |
| LoadBalancer 依赖是否存在 | 必须引入 `spring-cloud-starter-loadbalancer`                 |

验证命令如下：

```bash
# 查看 Gateway 健康状态
curl http://localhost:9000/actuator/health

# 通过服务名路由访问用户服务
curl -i http://localhost:9000/api/user/list

# 通过服务名路由访问订单服务
curl -i http://localhost:9000/api/order/page
```

如果需要查看 Gateway 当前加载的路由，可以打开 Actuator Gateway 端点：

```bash
curl http://localhost:9000/actuator/gateway/routes
```

该接口适合开发和测试环境排查路由问题。生产环境不建议公开暴露 Gateway 路由详情，应通过内网访问、鉴权或运维平台查看。

## 鉴权与安全

本章用于实现网关层的基础 Token 鉴权、白名单路径放行、登录态校验和用户信息透传。该章节对应你给出的 `鉴权与安全` 部分。

网关鉴权的核心目标是：请求进入业务服务之前，先由 Gateway 判断是否需要登录；如果需要登录，则校验 Token；校验通过后，将用户信息写入请求头传递给下游服务。下游服务只信任网关透传的内部 Header，不直接信任客户端伪造的用户 Header。

### Token 鉴权流程

Token 鉴权通常放在 `GlobalFilter` 中处理，因为它需要覆盖大部分业务接口。基础流程如下：

```text
客户端请求
  ↓
Gateway 接收请求
  ↓
判断是否命中白名单
  ├── 是：直接放行
  └── 否：读取 Authorization Token
        ↓
      校验 Token 签名和有效性
        ↓
      解析用户 ID、用户名、租户 ID
        ↓
      移除客户端伪造的用户 Header
        ↓
      写入可信用户 Header
        ↓
      转发到下游服务
```

建议使用标准 Header 传递 Token：

```text
Authorization: Bearer <token>
```

网关校验通过后，再向下游服务透传内部 Header：

```text
X-User-Id: 10001
X-Username: ateng
X-Tenant-Id: default
X-Trace-Id: 9d7b3c8e7c4f4b8d9bb51a0a2d0a1b2c
```

需要注意，`X-User-Id`、`X-Username`、`X-Tenant-Id` 这类 Header 不应该由客户端直接决定。网关在写入这些 Header 前，应先移除客户端传入的同名 Header，避免用户伪造身份。

### 白名单路径配置

白名单用于放行不需要登录的接口，例如登录接口、验证码接口、健康检查接口、公开文档接口、静态资源接口等。白名单应该配置化，不应硬编码在过滤器里。

文件位置：`src/main/resources/application.yml`

下面的配置增加 `gateway.auth` 配置项，用于管理 Token 密钥、白名单和用户信息透传 Header。

```yaml
gateway:
  auth:
    # 是否启用网关鉴权
    enabled: true

    # Token 请求头前缀
    token-prefix: Bearer

    # JWT 签名密钥，生产环境必须改为环境变量或配置中心注入
    token-secret: ateng-gateway-demo-secret

    # Token 中的用户 ID 字段名
    user-id-payload-key: userId

    # Token 中的用户名字段名
    username-payload-key: username

    # Token 中的租户 ID 字段名
    tenant-id-payload-key: tenantId

    # 下游服务接收的用户 ID Header
    user-id-header: X-User-Id

    # 下游服务接收的用户名 Header
    username-header: X-Username

    # 下游服务接收的租户 ID Header
    tenant-id-header: X-Tenant-Id

    # 白名单路径，不做登录态校验
    white-list:
      - /actuator/health
      - /actuator/info
      - /api/auth/login
      - /api/auth/logout
      - /api/auth/captcha
      - /api/open/**
      - /doc.html
      - /webjars/**
      - /v3/api-docs/**
```

文件位置：`src/main/java/io/github/atengk/gateway/config/AuthProperties.java`

下面的配置类用于承载 `gateway.auth` 配置项。

```java
package io.github.atengk.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权配置属性
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class AuthProperties {

    /**
     * 是否启用鉴权
     */
    private Boolean enabled = true;

    /**
     * Token 请求头前缀
     */
    private String tokenPrefix = "Bearer";

    /**
     * JWT 签名密钥
     */
    private String tokenSecret = "ateng-gateway-demo-secret";

    /**
     * Token 中的用户 ID 字段名
     */
    private String userIdPayloadKey = "userId";

    /**
     * Token 中的用户名字段名
     */
    private String usernamePayloadKey = "username";

    /**
     * Token 中的租户 ID 字段名
     */
    private String tenantIdPayloadKey = "tenantId";

    /**
     * 下游服务接收的用户 ID Header
     */
    private String userIdHeader = "X-User-Id";

    /**
     * 下游服务接收的用户名 Header
     */
    private String usernameHeader = "X-Username";

    /**
     * 下游服务接收的租户 ID Header
     */
    private String tenantIdHeader = "X-Tenant-Id";

    /**
     * 白名单路径
     */
    private List<String> whiteList = new ArrayList<>();

}
```

白名单匹配建议使用路径模式匹配，不建议简单使用 `startsWith`。例如 `/api/open/**` 可以放行所有公开接口，但 `/api/openapi/user` 不应因为字符串前缀相似而误放行。

### 登录态校验过滤器

登录态校验过滤器负责读取 Token、判断白名单、校验签名、解析用户信息和拒绝非法请求。这里使用 Hutool 的 JWT 工具类完成示例校验，适合开发文档和基础项目落地。生产项目如果已经使用 Sa-Token、Spring Security、OAuth2 Resource Server 或统一认证中心，应将 Token 校验逻辑替换为对应体系。

文件位置：`src/main/java/io/github/atengk/gateway/filter/TokenAuthGlobalFilter.java`

下面的全局过滤器用于完成网关层 Token 登录态校验。

```java
package io.github.atengk.gateway.filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import io.github.atengk.gateway.config.AuthProperties;
import io.github.atengk.gateway.support.GatewayResponseWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Token 登录态校验全局过滤器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 执行 Token 登录态校验
     *
     * @param exchange ServerWebExchange
     * @param chain    GatewayFilterChain
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!Boolean.TRUE.equals(authProperties.getEnabled())) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getRawPath();

        if (isWhitePath(path)) {
            log.debug("网关白名单放行，path={}", path);
            return chain.filter(exchange);
        }

        String token = resolveToken(request);
        if (StrUtil.isBlank(token)) {
            log.warn("网关登录态校验失败，Token 为空，path={}", path);
            return GatewayResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, "请先登录");
        }

        Map<String, String> userInfo = parseToken(token);
        if (MapUtil.isEmpty(userInfo)) {
            log.warn("网关登录态校验失败，Token 无效，path={}", path);
            return GatewayResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, "登录状态已失效");
        }

        ServerHttpRequest newRequest = request.mutate()
                .headers(headers -> {
                    // 移除客户端伪造的用户 Header，避免身份被绕过
                    headers.remove(authProperties.getUserIdHeader());
                    headers.remove(authProperties.getUsernameHeader());
                    headers.remove(authProperties.getTenantIdHeader());

                    // 写入网关校验后的可信用户信息
                    headers.add(authProperties.getUserIdHeader(), userInfo.get("userId"));
                    headers.add(authProperties.getUsernameHeader(), userInfo.get("username"));
                    headers.add(authProperties.getTenantIdHeader(), userInfo.get("tenantId"));
                })
                .build();

        log.debug("网关登录态校验通过，path={}, userId={}", path, userInfo.get("userId"));
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    /**
     * 获取过滤器执行顺序
     *
     * @return 顺序值
     */
    @Override
    public int getOrder() {
        return -90;
    }

    /**
     * 判断是否为白名单路径
     *
     * @param path 请求路径
     * @return true 表示白名单路径
     */
    private boolean isWhitePath(String path) {
        return CollUtil.emptyIfNull(authProperties.getWhiteList())
                .stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 解析请求中的 Token
     *
     * @param request 请求对象
     * @return Token
     */
    private String resolveToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isBlank(authorization)) {
            return null;
        }

        String tokenPrefix = StrUtil.blankToDefault(authProperties.getTokenPrefix(), "Bearer");
        if (StrUtil.startWithIgnoreCase(authorization, tokenPrefix)) {
            return StrUtil.trim(StrUtil.removePrefixIgnoreCase(authorization, tokenPrefix));
        }

        return authorization;
    }

    /**
     * 解析并校验 Token
     *
     * @param token Token
     * @return 用户信息
     */
    private Map<String, String> parseToken(String token) {
        try {
            byte[] secretBytes = authProperties.getTokenSecret().getBytes(StandardCharsets.UTF_8);
            boolean verified = JWTUtil.verify(token, secretBytes);
            if (!verified) {
                return MapUtil.empty();
            }

            JWT jwt = JWTUtil.parseToken(token);
            String userId = Convert.toStr(jwt.getPayload(authProperties.getUserIdPayloadKey()));
            String username = Convert.toStr(jwt.getPayload(authProperties.getUsernamePayloadKey()));
            String tenantId = Convert.toStr(jwt.getPayload(authProperties.getTenantIdPayloadKey()), "default");

            if (StrUtil.isBlank(userId)) {
                return MapUtil.empty();
            }

            Map<String, String> userInfo = MapUtil.newHashMap(true);
            userInfo.put("userId", userId);
            userInfo.put("username", StrUtil.blankToDefault(username, userId));
            userInfo.put("tenantId", tenantId);
            return userInfo;
        } catch (Exception ex) {
            log.warn("网关 Token 解析异常，error={}", ex.getMessage());
            return MapUtil.empty();
        }
    }

}
```

过滤器执行顺序建议如下：

| 过滤器                     | 建议顺序            | 说明                   |
| -------------------------- | ------------------- | ---------------------- |
| `GatewayTraceGlobalFilter` | `-100`              | 先生成链路 ID          |
| `TokenAuthGlobalFilter`    | `-90`               | 再执行登录态校验       |
| `RequestLogGlobalFilter`   | `LOWEST_PRECEDENCE` | 最后统计请求状态和耗时 |

验证命令如下：

```bash
# 白名单接口，直接放行
curl -i http://localhost:9000/actuator/health

# 非白名单接口，未带 Token，返回 401
curl -i http://localhost:9000/api/user/list

# 非白名单接口，携带 Token
curl -i http://localhost:9000/api/user/list \
  -H "Authorization: Bearer <token>"
```

如果返回 `401`，优先检查以下内容：

| 检查项               | 说明                                                       |
| -------------------- | ---------------------------------------------------------- |
| Header 是否正确      | 必须是 `Authorization: Bearer <token>`                     |
| 密钥是否一致         | 生成 Token 和 Gateway 校验 Token 使用同一个 `token-secret` |
| Payload 字段是否一致 | Token 中应包含 `userId`，可选包含 `username`、`tenantId`   |
| 白名单是否配置正确   | 路径匹配应使用网关入口路径，而不是下游服务改写后的路径     |
| 时间是否过期         | 如果 Token 中包含过期时间，应在解析逻辑中增加过期校验      |

### 用户信息透传

用户信息透传是指网关校验 Token 后，将可信用户信息写入请求头，再传递给下游服务。下游服务可以从 Header 中读取当前用户 ID、用户名、租户 ID，用于业务数据隔离、审计日志、权限判断等场景。

网关侧在 `TokenAuthGlobalFilter` 中已经完成了用户信息透传：

```text
X-User-Id
X-Username
X-Tenant-Id
```

下游业务服务可以通过 `HttpServletRequest` 或 Controller 参数读取这些 Header。下面给出一个用户服务中的验证接口。

文件位置：`user-service/src/main/java/io/github/atengk/user/controller/UserContextController.java`

下面的 Controller 用于验证下游服务是否能接收到网关透传的用户信息。

```java
package io.github.atengk.user.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户上下文验证接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class UserContextController {

    /**
     * 获取当前登录用户上下文
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param tenantId 租户 ID
     * @param headers  请求头
     * @return 用户上下文
     */
    @GetMapping("/user/context")
    public Map<String, Object> getUserContext(@RequestHeader(value = "X-User-Id", required = false) String userId,
                                              @RequestHeader(value = "X-Username", required = false) String username,
                                              @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
                                              @RequestHeader HttpHeaders headers) {
        log.info("下游服务接收到用户上下文，userId={}, username={}, tenantId={}", userId, username, tenantId);

        Map<String, Object> result = MapUtil.newHashMap(true);
        result.put("userId", StrUtil.blankToDefault(userId, "-"));
        result.put("username", StrUtil.blankToDefault(username, "-"));
        result.put("tenantId", StrUtil.blankToDefault(tenantId, "-"));
        result.put("traceId", StrUtil.blankToDefault(headers.getFirst("X-Trace-Id"), "-"));
        return result;
    }

}
```

通过网关访问验证接口：

```bash
curl -i http://localhost:9000/api/user/context \
  -H "Authorization: Bearer <token>" \
  -H "X-User-Id: fake-user-id"
```

如果 Token 中的 `userId` 是 `10001`，即使客户端伪造了 `X-User-Id: fake-user-id`，下游服务也应该接收到网关重新写入后的真实用户 ID，而不是客户端伪造值。

响应示例：

```json
{
  "userId": "10001",
  "username": "ateng",
  "tenantId": "default",
  "traceId": "9d7b3c8e7c4f4b8d9bb51a0a2d0a1b2c"
}
```

用户信息透传需要注意以下几点：

| 注意项                        | 说明                                                      |
| ----------------------------- | --------------------------------------------------------- |
| 网关必须移除客户端伪造 Header | 写入用户 Header 前先 `remove`，再 `add`                   |
| 下游服务只信任内网网关        | 不允许客户端绕过网关直接访问业务服务                      |
| Header 字段保持统一           | 所有服务统一使用 `X-User-Id`、`X-Username`、`X-Tenant-Id` |
| 敏感信息不要透传              | 不建议透传手机号、身份证、完整权限列表等敏感数据          |
| 租户信息必须校验              | 多租户系统不能只依赖前端传入租户 ID                       |
| 日志避免泄露 Token            | 日志中不要打印完整 `Authorization` 内容                   |

生产环境中，建议在网络层限制业务服务只允许 Gateway、服务网格或内部负载均衡访问。否则客户端绕过网关直连下游服务时，仍然可能伪造用户 Header。

## 跨域与限流

本章用于处理前后端分离项目中常见的 CORS 跨域问题，以及基于 Redis 的网关限流能力。该章节对应你给出的 `跨域与限流` 部分。

在 Spring Cloud Gateway 中，跨域属于入口层通用能力，建议优先在网关统一配置；限流属于流量治理能力，建议优先按用户、IP、接口路径、租户等维度配置在网关层。Spring Cloud Gateway 官方文档说明，CORS 可以全局配置，也可以配置在具体路由的 `metadata.cors` 中；Redis 限流则通过 `RequestRateLimiter` 过滤器实现，不允许使用简写参数形式，需要使用 `name + args` 的完整写法。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/cors-configuration.html?utm_source=chatgpt.com))

### CORS 跨域配置

CORS 跨域配置用于允许浏览器从指定前端域名访问 Gateway 接口。前后端分离项目中，如果前端运行在 `http://localhost:5173`，网关运行在 `http://localhost:9000`，浏览器会触发跨域检查。此时应由 Gateway 返回 `Access-Control-Allow-Origin`、`Access-Control-Allow-Methods`、`Access-Control-Allow-Headers` 等响应头。

Spring Cloud Gateway 4.3 的配置属性中，WebFlux Gateway 的全局 CORS 配置项位于 `spring.cloud.gateway.server.webflux.globalcors.*`；旧文档示例中也常见 `spring.cloud.gateway.globalcors.*`，在 Spring Boot 3 + Spring Cloud Gateway 4.3/5.x 项目中建议优先使用新前缀，避免和 WebMVC Gateway 配置混淆。([Home](https://docs.spring.io/spring-cloud-gateway/reference/configprops.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面的配置用于在网关层统一处理跨域请求。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          globalcors:
            # 让没有匹配到具体路由的 OPTIONS 预检请求也能走 CORS 处理
            add-to-simple-url-handler-mapping: true

            cors-configurations:
              '[/**]':
                # 开发环境允许的前端地址，生产环境必须改为实际域名
                allowed-origins:
                  - http://localhost:5173
                  - http://localhost:3000

                # 允许的请求方法
                allowed-methods:
                  - GET
                  - POST
                  - PUT
                  - DELETE
                  - PATCH
                  - OPTIONS

                # 允许客户端携带的请求头
                allowed-headers:
                  - Authorization
                  - Content-Type
                  - X-Requested-With
                  - X-Trace-Id
                  - X-Client-Type

                # 允许前端读取的响应头
                exposed-headers:
                  - X-Trace-Id
                  - X-RateLimit-Remaining
                  - X-RateLimit-Replenish-Rate
                  - X-RateLimit-Burst-Capacity

                # 是否允许携带 Cookie、Authorization 等凭证信息
                allow-credentials: true

                # 预检请求缓存时间，单位秒
                max-age: 3600
```

如果某个路由需要单独配置跨域规则，可以将 CORS 配置放在路由 `metadata.cors` 下。官方文档说明，路由级 CORS 和全局 CORS 使用的都是 Spring Framework `CorsConfiguration` 属性。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/cors-configuration.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面的配置只对公开接口路由单独开放跨域。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: open-api-route
              uri: lb://open-service
              predicates:
                # 匹配公开接口
                - Path=/api/open/**
              filters:
                # 去掉 /api 前缀
                - StripPrefix=1
              metadata:
                cors:
                  # 公开接口允许指定前端域名访问
                  allowedOrigins:
                    - https://www.example.com
                  allowedMethods:
                    - GET
                    - POST
                  allowedHeaders:
                    - Content-Type
                    - X-Trace-Id
                  maxAge: 1800
```

如果网关和下游服务都配置了 CORS，可能出现重复响应头，例如重复的 `Access-Control-Allow-Origin` 或 `Access-Control-Allow-Credentials`。此时建议只保留网关层 CORS，下游服务删除跨域配置；如果暂时无法删除，可以使用 `DedupeResponseHeader` 去重。

文件位置：`src/main/resources/application.yml`

下面的配置用于去除重复的 CORS 响应头。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          default-filters:
            # 去除重复的跨域响应头，保留唯一值
            - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin, RETAIN_UNIQUE
```

验证跨域预检请求：

```bash
curl -i -X OPTIONS http://localhost:9000/api/user/list \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization,Content-Type"
```

正常情况下，响应头中应包含类似内容：

```text
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
Access-Control-Allow-Credentials: true
```

跨域配置需要注意以下几点：

| 注意项                   | 说明                                                         |
| ------------------------ | ------------------------------------------------------------ |
| 生产环境不要直接使用 `*` | 携带凭证时不能随意开放全部来源                               |
| 优先在网关统一配置       | 避免每个业务服务重复配置 CORS                                |
| 预检请求要能被处理       | `OPTIONS` 请求可能不命中具体路由，需要开启 `add-to-simple-url-handler-mapping` |
| 避免重复响应头           | 网关和下游同时设置 CORS 时容易出现重复 Header                |
| 前端域名精确配置         | 开发、测试、生产环境应使用不同白名单                         |

### Redis 限流配置

Redis 限流用于在多个 Gateway 实例之间共享限流状态，适合生产环境。Spring Cloud Gateway 的 `RequestRateLimiter` 会调用 `RateLimiter` 判断当前请求是否允许通过；Redis 实现基于令牌桶算法，并且需要引入 `spring-boot-starter-data-redis-reactive`。官方文档说明，`replenishRate` 表示每秒补充令牌数，`burstCapacity` 表示桶容量，`requestedTokens` 表示每个请求消耗的令牌数；超过限制时默认返回 `HTTP 429 Too Many Requests`。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

下面的依赖用于启用 Redis 响应式访问能力，供 Gateway Redis 限流器使用。

```xml
<dependencies>
    <!-- Spring Cloud Gateway WebFlux 网关核心依赖 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
    </dependency>

    <!-- Redis Reactive：Gateway RedisRateLimiter 必需依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>

    <!-- Spring Cloud LoadBalancer：支持 lb://service-name 路由 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>

    <!-- Hutool：字符串、集合、JSON 等工具能力 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

下面的配置连接 Redis，并对用户服务接口启用 Redis 限流。

```yaml
spring:
  data:
    redis:
      # Redis 地址
      host: 127.0.0.1

      # Redis 端口
      port: 6379

      # Redis 数据库
      database: 0

      # Redis 密码，未设置密码时可以删除该项
      password:

      lettuce:
        pool:
          # 最大连接数
          max-active: 16

          # 最大空闲连接数
          max-idle: 8

          # 最小空闲连接数
          min-idle: 2

  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-rate-limit-route
              uri: lb://user-service
              predicates:
                # 匹配用户服务接口
                - Path=/api/user/**
              filters:
                # 去掉 /api 前缀
                - StripPrefix=1

                # Redis 限流，必须使用完整写法，不能使用简写写法
                - name: RequestRateLimiter
                  args:
                    # 使用自定义用户限流 KeyResolver
                    key-resolver: "#{@userRateLimitKeyResolver}"

                    # 每秒补充 5 个令牌，稳定速率约为 5 次/秒
                    redis-rate-limiter.replenishRate: 5

                    # 令牌桶最大容量 10，允许短时间突发 10 次
                    redis-rate-limiter.burstCapacity: 10

                    # 每个请求消耗 1 个令牌
                    redis-rate-limiter.requestedTokens: 1
```

文件位置：`src/main/java/io/github/atengk/gateway/config/RateLimitKeyResolverConfig.java`

下面的配置类定义多个限流 KeyResolver，可按用户、IP、路径等维度生成限流 Key。

```java
package io.github.atengk.gateway.config;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * 网关限流 Key 解析配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class RateLimitKeyResolverConfig {

    /**
     * 按用户 ID 限流，适合登录后接口
     *
     * @return KeyResolver
     */
    @Bean
    public KeyResolver userRateLimitKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String userId = request.getHeaders().getFirst("X-User-Id");
            String clientIp = getClientIp(request);

            String key = StrUtil.isNotBlank(userId) ? "user:" + userId : "ip:" + clientIp;
            log.debug("生成用户限流 Key，key={}", key);
            return Mono.just(key);
        };
    }

    /**
     * 按客户端 IP 限流，适合公开接口或登录接口
     *
     * @return KeyResolver
     */
    @Bean
    public KeyResolver ipRateLimitKeyResolver() {
        return exchange -> {
            String clientIp = getClientIp(exchange.getRequest());
            String key = "ip:" + clientIp;
            log.debug("生成 IP 限流 Key，key={}", key);
            return Mono.just(key);
        };
    }

    /**
     * 按用户 ID 和接口路径组合限流，适合核心业务接口
     *
     * @return KeyResolver
     */
    @Bean
    public KeyResolver userPathRateLimitKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String userId = StrUtil.blankToDefault(request.getHeaders().getFirst("X-User-Id"), "anonymous");
            String path = request.getURI().getRawPath();

            String key = StrUtil.format("user-path:{}:{}", userId, path);
            log.debug("生成用户路径限流 Key，key={}", key);
            return Mono.just(key);
        };
    }

    /**
     * 获取客户端 IP
     *
     * @param request 请求对象
     * @return 客户端 IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            return StrUtil.splitTrim(forwardedFor, ",").getFirst();
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (Objects.isNull(remoteAddress) || Objects.isNull(remoteAddress.getAddress())) {
            return "unknown";
        }

        return remoteAddress.getAddress().getHostAddress();
    }

}
```

不同接口可以选择不同限流 Key：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: auth-login-rate-limit-route
              uri: lb://auth-service
              predicates:
                # 登录接口按 IP 限流，防止暴力尝试
                - Path=/api/auth/login
              filters:
                - StripPrefix=1
                - name: RequestRateLimiter
                  args:
                    key-resolver: "#{@ipRateLimitKeyResolver}"
                    redis-rate-limiter.replenishRate: 1
                    redis-rate-limiter.burstCapacity: 3
                    redis-rate-limiter.requestedTokens: 1

            - id: user-query-rate-limit-route
              uri: lb://user-service
              predicates:
                # 用户查询接口按用户 ID 限流
                - Path=/api/user/**
              filters:
                - StripPrefix=1
                - name: RequestRateLimiter
                  args:
                    key-resolver: "#{@userRateLimitKeyResolver}"
                    redis-rate-limiter.replenishRate: 5
                    redis-rate-limiter.burstCapacity: 10
                    redis-rate-limiter.requestedTokens: 1

            - id: order-submit-rate-limit-route
              uri: lb://order-service
              predicates:
                # 订单提交接口按用户和路径组合限流
                - Path=/api/order/submit
              filters:
                - StripPrefix=1
                - name: RequestRateLimiter
                  args:
                    key-resolver: "#{@userPathRateLimitKeyResolver}"
                    redis-rate-limiter.replenishRate: 1
                    redis-rate-limiter.burstCapacity: 2
                    redis-rate-limiter.requestedTokens: 1
```

启动 Redis：

```bash
docker run -d \
  --name redis-gateway \
  -p 6379:6379 \
  redis:7.4
```

验证限流效果：

```bash
# 连续请求，超过阈值后应返回 429
for i in $(seq 1 20); do
  curl -i http://localhost:9000/api/user/list \
    -H "X-User-Id: 10001"
done
```

出现限流时，响应状态码通常为：

```text
HTTP/1.1 429 Too Many Requests
```

如果开启了 Gateway 默认限流响应头，响应中还会出现类似 Header：

```text
X-RateLimit-Remaining: 0
X-RateLimit-Replenish-Rate: 5
X-RateLimit-Burst-Capacity: 10
```

### 自定义限流策略

自定义限流策略通常分为两类：第一类是自定义限流 Key，例如按用户、IP、租户、接口路径组合限流；第二类是完全自定义限流逻辑，例如按接口配置不同窗口、按租户设置不同额度、按黑名单用户直接拒绝。优先推荐第一类，因为它可以复用 Gateway 官方 `RequestRateLimiter` 和 Redis 令牌桶实现，稳定性更好。

如果需要更细粒度的业务限流，可以自定义一个 `GlobalFilter`，使用 Redis 实现固定窗口计数。固定窗口实现简单，适合登录接口、短信验证码接口、公开接口等场景；但它不如令牌桶平滑，生产核心流量限流仍建议优先使用 Gateway 内置 RedisRateLimiter。

文件位置：`src/main/resources/application.yml`

下面的配置定义自定义固定窗口限流参数。

```yaml
gateway:
  custom-rate-limit:
    # 是否启用自定义固定窗口限流
    enabled: true

    # 每个窗口允许的最大请求数
    limit: 10

    # 窗口时间，单位秒
    window-seconds: 60

    # 需要启用自定义限流的路径
    include-paths:
      - /api/auth/login
      - /api/auth/captcha
      - /api/open/**
```

文件位置：`src/main/java/io/github/atengk/gateway/config/CustomRateLimitProperties.java`

下面的配置类用于读取自定义固定窗口限流参数。

```java
package io.github.atengk.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义限流配置属性
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.custom-rate-limit")
public class CustomRateLimitProperties {

    /**
     * 是否启用自定义限流
     */
    private Boolean enabled = false;

    /**
     * 窗口内最大请求数
     */
    private Long limit = 10L;

    /**
     * 窗口时间，单位秒
     */
    private Long windowSeconds = 60L;

    /**
     * 需要限流的路径
     */
    private List<String> includePaths = new ArrayList<>();

}
```

文件位置：`src/main/java/io/github/atengk/gateway/filter/CustomRateLimitGlobalFilter.java`

下面的过滤器基于 Redis 实现简单固定窗口限流。

```java
package io.github.atengk.gateway.filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.gateway.config.CustomRateLimitProperties;
import io.github.atengk.gateway.support.GatewayResponseWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;

/**
 * 自定义固定窗口限流全局过滤器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomRateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final String RATE_LIMIT_KEY_PREFIX = "gateway:custom-rate-limit:";

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    private final CustomRateLimitProperties properties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 执行自定义限流
     *
     * @param exchange ServerWebExchange
     * @param chain    GatewayFilterChain
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getRawPath();

        if (!isMatchPath(path)) {
            return chain.filter(exchange);
        }

        String key = buildRateLimitKey(request);
        Duration expire = Duration.ofSeconds(properties.getWindowSeconds());

        return reactiveStringRedisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    Mono<Boolean> expireMono = count == 1
                            ? reactiveStringRedisTemplate.expire(key, expire)
                            : Mono.just(Boolean.TRUE);

                    return expireMono.flatMap(ignore -> {
                        if (count > properties.getLimit()) {
                            log.warn("自定义限流触发，key={}, count={}, limit={}", key, count, properties.getLimit());
                            return GatewayResponseWriter.write(exchange, HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
                        }

                        log.debug("自定义限流通过，key={}, count={}, limit={}", key, count, properties.getLimit());
                        return chain.filter(exchange);
                    });
                })
                .onErrorResume(ex -> {
                    log.error("自定义限流异常，path={}, error={}", path, ex.getMessage(), ex);
                    return GatewayResponseWriter.write(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "网关限流服务异常");
                });
    }

    /**
     * 获取过滤器顺序
     *
     * @return 顺序值
     */
    @Override
    public int getOrder() {
        return -80;
    }

    /**
     * 判断路径是否需要限流
     *
     * @param path 请求路径
     * @return true 表示需要限流
     */
    private boolean isMatchPath(String path) {
        return CollUtil.emptyIfNull(properties.getIncludePaths())
                .stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 构建限流 Key
     *
     * @param request 请求对象
     * @return 限流 Key
     */
    private String buildRateLimitKey(ServerHttpRequest request) {
        String userId = request.getHeaders().getFirst("X-User-Id");
        String clientIp = getClientIp(request);
        String path = request.getURI().getRawPath();

        String identity = StrUtil.isNotBlank(userId) ? "user:" + userId : "ip:" + clientIp;
        return RATE_LIMIT_KEY_PREFIX + identity + ":" + path;
    }

    /**
     * 获取客户端 IP
     *
     * @param request 请求对象
     * @return 客户端 IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            return StrUtil.splitTrim(forwardedFor, ",").getFirst();
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (Objects.isNull(remoteAddress) || Objects.isNull(remoteAddress.getAddress())) {
            return "unknown";
        }

        return remoteAddress.getAddress().getHostAddress();
    }

}
```

验证自定义固定窗口限流：

```bash
# 连续请求登录接口，超过 gateway.custom-rate-limit.limit 后返回 429
for i in $(seq 1 15); do
  curl -i http://localhost:9000/api/auth/login
done
```

自定义限流策略建议如下：

| 场景         | 推荐策略                          |
| ------------ | --------------------------------- |
| 登录接口     | 按 IP 限流，防暴力破解            |
| 短信验证码   | 按手机号 + IP 限流，防刷短信      |
| 用户查询接口 | 按用户 ID 限流                    |
| 订单提交接口 | 按用户 ID + 路径限流              |
| 开放接口     | 按 AppId、租户 ID 或 API Key 限流 |
| 管理后台接口 | 按用户 ID + 客户端类型限流        |

固定窗口限流实现简单，但在窗口边界可能出现瞬时突刺。例如 60 秒窗口内最后 1 秒请求 10 次，下一窗口第 1 秒又请求 10 次，短时间内实际会出现 20 次请求。因此生产核心接口更推荐使用 Gateway 内置 RedisRateLimiter 的令牌桶算法。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html?utm_source=chatgpt.com))

## 异常处理

本章用于说明网关层常见异常分类、统一异常响应和下游服务异常处理方式。该章节对应你给出的 `异常处理` 部分。

Gateway 异常处理要区分“网关主动拒绝”“网关自身异常”和“下游服务异常”。网关主动拒绝包括鉴权失败、限流拒绝、请求头非法等；网关自身异常包括路由不存在、配置错误、过滤器异常等；下游服务异常包括服务不可用、超时、连接失败、业务服务返回 5xx 等。

### 网关异常分类

网关异常可以按发生位置和处理方式分类。分类清楚后，才能决定是由过滤器主动响应、全局异常处理器兜底，还是通过重试、熔断、降级处理下游异常。

| 异常类型       | 常见状态码 | 触发场景                         | 推荐处理方式                    |
| -------------- | ---------- | -------------------------------- | ------------------------------- |
| 路由不存在     | `404`      | 请求路径没有匹配任何路由         | 返回统一 JSON，提示路由不存在   |
| 鉴权失败       | `401`      | 未登录、Token 无效、Token 过期   | 鉴权过滤器主动返回              |
| 权限不足       | `403`      | 用户已登录但无访问权限           | 权限过滤器主动返回              |
| 限流拒绝       | `429`      | 请求超过限流阈值                 | 限流过滤器返回统一响应          |
| 请求过大       | `413`      | 请求体超过限制                   | 配置 RequestSize 或前置代理限制 |
| 请求头过大     | `431`      | Header 超过限制                  | 配置 RequestHeaderSize          |
| 下游服务不可用 | `503`      | 注册中心无实例、服务不可达       | 返回统一响应或熔断降级          |
| 下游响应超时   | `504`      | 下游服务处理超时                 | 配置超时、重试、熔断            |
| 网关内部异常   | `500`      | 过滤器异常、配置错误、序列化异常 | 全局异常处理器兜底              |

Spring Cloud Gateway 的 `RequestRateLimiter` 默认在请求不允许通过时返回 `HTTP 429 Too Many Requests`；如果服务名路由找不到可用实例，负载均衡场景通常会表现为下游不可用类错误。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html?utm_source=chatgpt.com))

异常处理的基本原则如下：

| 原则                   | 说明                                         |
| ---------------------- | -------------------------------------------- |
| 网关主动拒绝要明确     | 鉴权、限流、请求非法等直接返回清晰错误       |
| 不暴露内部异常堆栈     | 客户端只返回错误码、错误消息、路径、traceId  |
| 保留 traceId           | 便于和网关日志、下游日志串联排查             |
| 区分 4xx 和 5xx        | 4xx 多为客户端问题，5xx 多为服务端或下游问题 |
| 下游业务异常不强行包装 | 下游正常返回的业务响应应尽量保持透明         |

### 统一异常响应

统一异常响应用于保证网关层错误格式一致。前面章节已经给出了 `GatewayResponseWriter`，这里继续复用它，并补充一个更完整的异常处理器。该处理器负责兜底处理 Gateway 抛出的异常，返回统一 JSON。

统一响应格式建议如下：

```json
{
  "code": 503,
  "message": "下游服务暂不可用",
  "path": "/api/user/list",
  "traceId": "9d7b3c8e7c4f4b8d9bb51a0a2d0a1b2c",
  "timestamp": "2026-05-05 15:30:00"
}
```

文件位置：`src/main/java/io/github/atengk/gateway/support/GatewayResponseWriter.java`

下面的工具类用于写入统一 JSON 响应。

```java
package io.github.atengk.gateway.support;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 网关响应写入工具类
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class GatewayResponseWriter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private GatewayResponseWriter() {
    }

    /**
     * 写入统一 JSON 响应
     *
     * @param exchange   ServerWebExchange
     * @param statusCode HTTP 状态码
     * @param message    响应消息
     * @return Mono<Void>
     */
    public static Mono<Void> write(ServerWebExchange exchange, HttpStatusCode statusCode, String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        String path = exchange.getRequest().getURI().getRawPath();

        Map<String, Object> body = MapUtil.newHashMap(true);
        body.put("code", statusCode.value());
        body.put("message", StrUtil.blankToDefault(message, "网关请求处理失败"));
        body.put("path", path);
        body.put("traceId", StrUtil.blankToDefault(traceId, "-"));
        body.put("timestamp", DateUtil.now());

        byte[] bytes = JSONUtil.toJsonStr(body).getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = exchange.getResponse().bufferFactory().wrap(bytes);

        exchange.getResponse().setStatusCode(statusCode);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

}
```

文件位置：`src/main/java/io/github/atengk/gateway/handler/GatewayGlobalExceptionHandler.java`

下面的异常处理器用于统一处理网关运行时异常。

```java
package io.github.atengk.gateway.handler;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.gateway.support.GatewayResponseWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

/**
 * 网关全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Order(-2)
@Component
public class GatewayGlobalExceptionHandler implements WebExceptionHandler {

    /**
     * 处理网关异常
     *
     * @param exchange ServerWebExchange
     * @param ex       异常对象
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatusCode statusCode = resolveStatusCode(ex);
        String message = resolveMessage(statusCode, ex);
        String path = exchange.getRequest().getURI().getRawPath();

        log.error("网关异常处理，path={}, status={}, message={}, error={}",
                path, statusCode.value(), message, ex.getMessage(), ex);

        return GatewayResponseWriter.write(exchange, statusCode, message);
    }

    /**
     * 解析 HTTP 状态码
     *
     * @param ex 异常对象
     * @return HTTP 状态码
     */
    private HttpStatusCode resolveStatusCode(Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode();
        }

        if (ex instanceof NotFoundException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }

        if (ex instanceof TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }

        if (containsCause(ex, ConnectException.class)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 解析响应消息
     *
     * @param statusCode HTTP 状态码
     * @param ex         异常对象
     * @return 响应消息
     */
    private String resolveMessage(HttpStatusCode statusCode, Throwable ex) {
        if (HttpStatus.NOT_FOUND.value() == statusCode.value()) {
            return "网关路由不存在";
        }

        if (HttpStatus.UNAUTHORIZED.value() == statusCode.value()) {
            return "请先登录";
        }

        if (HttpStatus.FORBIDDEN.value() == statusCode.value()) {
            return "没有访问权限";
        }

        if (HttpStatus.TOO_MANY_REQUESTS.value() == statusCode.value()) {
            return "请求过于频繁，请稍后再试";
        }

        if (HttpStatus.SERVICE_UNAVAILABLE.value() == statusCode.value()) {
            return "下游服务暂不可用";
        }

        if (HttpStatus.GATEWAY_TIMEOUT.value() == statusCode.value()) {
            return "下游服务响应超时";
        }

        String errorMessage = ex.getMessage();
        if (StrUtil.containsIgnoreCase(errorMessage, "Connection refused")) {
            return "下游服务连接失败";
        }

        return "网关服务异常";
    }

    /**
     * 判断异常链中是否包含指定异常类型
     *
     * @param throwable 异常对象
     * @param causeType 异常类型
     * @return true 表示包含
     */
    private boolean containsCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isAssignableFrom(current.getClass())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
```

验证网关异常响应：

```bash
# 访问不存在的路由
curl -i http://localhost:9000/not-found-test

# 停止 user-service 后访问用户服务路由，观察下游不可用响应
curl -i http://localhost:9000/api/user/list
```

如果你已经在上一章创建过 `GatewayErrorWebExceptionHandler`，不要同时保留两个 `WebExceptionHandler` 处理器。建议保留这里的 `GatewayGlobalExceptionHandler`，删除旧版本，避免顺序和响应行为不一致。

### 下游服务异常处理

下游服务异常主要包括连接失败、实例不存在、响应超时、返回 5xx、临时网络抖动等。Gateway 层不能替代业务服务修复异常，但可以通过超时、重试、熔断和降级来避免异常扩大。

第一步是配置合理的下游连接超时和响应超时。超时时间不能过长，否则会拖垮 Gateway 的连接资源；也不能过短，否则正常慢接口会被误判失败。

文件位置：`src/main/resources/application.yml`

下面的配置设置全局 HttpClient 超时和单路由超时。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          httpclient:
            # 连接下游服务超时时间，单位毫秒
            connect-timeout: 3000

            # 等待下游响应的最大时间
            response-timeout: 10s

          routes:
            - id: user-service-timeout-route
              uri: lb://user-service
              predicates:
                - Path=/api/user/**
              filters:
                - StripPrefix=1
              metadata:
                # 当前路由连接超时时间，单位毫秒
                connect-timeout: 2000

                # 当前路由响应超时时间，单位毫秒
                response-timeout: 5000
```

第二步是对幂等请求配置重试。Spring Cloud Gateway 的 `Retry` 过滤器支持配置重试次数、状态码、HTTP 方法、异常类型、退避策略等；官方文档也明确列出了 `retries`、`statuses`、`methods`、`series`、`exceptions`、`backoff` 等参数。([spring.pleiades.io](https://spring.pleiades.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/retry-factory.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面的配置只对 `GET` 请求启用重试，避免对创建订单、支付、扣库存等非幂等请求重复提交。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service-retry-route
              uri: lb://user-service
              predicates:
                - Path=/api/user/**
              filters:
                - StripPrefix=1

                # 只对幂等 GET 请求启用重试
                - name: Retry
                  args:
                    # 最多重试 2 次
                    retries: 2

                    # 只重试 GET 请求
                    methods:
                      - GET

                    # 对 5xx 系列状态码重试
                    series:
                      - SERVER_ERROR

                    # 对指定状态码重试
                    statuses:
                      - BAD_GATEWAY
                      - SERVICE_UNAVAILABLE
                      - GATEWAY_TIMEOUT

                    # 指数退避配置
                    backoff:
                      firstBackoff: 100ms
                      maxBackoff: 1s
                      factor: 2
                      basedOnPreviousValue: false
```

第三步是对关键下游服务配置熔断降级。Spring Cloud Gateway 的 `CircuitBreaker` 过滤器基于 Spring Cloud CircuitBreaker API，Spring Cloud 默认支持 Resilience4J；使用该过滤器需要引入 `spring-cloud-starter-circuitbreaker-reactor-resilience4j` 依赖，`fallbackUri` 目前支持 `forward:` 方式转发到网关内部处理器。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/circuitbreaker-filter-factory.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

下面的依赖用于启用响应式熔断能力。

```xml
<dependencies>
    <!-- Spring Cloud CircuitBreaker Resilience4J：Gateway 熔断降级依赖 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

下面的配置给用户服务路由增加熔断降级。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service-circuitbreaker-route
              uri: lb://user-service
              predicates:
                - Path=/api/user/**
              filters:
                - StripPrefix=1

                # 下游服务异常时转发到网关内部 fallback 地址
                - name: CircuitBreaker
                  args:
                    name: userServiceCircuitBreaker
                    fallbackUri: forward:/fallback/user-service

                    # 下游返回这些状态码时触发熔断统计
                    statusCodes:
                      - 500
                      - 502
                      - 503
                      - 504
```

文件位置：`src/main/java/io/github/atengk/gateway/handler/FallbackHandler.java`

下面的处理器用于返回下游服务降级响应。

```java
package io.github.atengk.gateway.handler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 网关降级响应处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class FallbackHandler {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 用户服务降级响应
     *
     * @param request ServerRequest
     * @return ServerResponse
     */
    public Mono<ServerResponse> userServiceFallback(ServerRequest request) {
        ServerWebExchange exchange = request.exchange();
        Throwable throwable = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        String path = exchange.getRequest().getURI().getRawPath();

        if (throwable != null) {
            log.warn("用户服务触发熔断降级，path={}, traceId={}, error={}", path, traceId, throwable.getMessage());
        } else {
            log.warn("用户服务触发熔断降级，path={}, traceId={}", path, traceId);
        }

        Map<String, Object> body = MapUtil.newHashMap(true);
        body.put("code", 503);
        body.put("message", "用户服务暂不可用，请稍后再试");
        body.put("path", path);
        body.put("traceId", StrUtil.blankToDefault(traceId, "-"));
        body.put("timestamp", DateUtil.now());

        return ServerResponse.status(503)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

}
```

文件位置：`src/main/java/io/github/atengk/gateway/config/FallbackRouterConfig.java`

下面的路由配置用于将 `forward:/fallback/user-service` 映射到降级处理器。

```java
package io.github.atengk.gateway.config;

import io.github.atengk.gateway.handler.FallbackHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * 网关降级路由配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class FallbackRouterConfig {

    /**
     * 配置降级处理路由
     *
     * @param fallbackHandler 降级处理器
     * @return RouterFunction<ServerResponse>
     */
    @Bean
    public RouterFunction<ServerResponse> fallbackRouterFunction(FallbackHandler fallbackHandler) {
        return route(path("/fallback/user-service"), fallbackHandler::userServiceFallback);
    }

}
```

验证下游异常处理：

```bash
# 1. 先停止 user-service
# 2. 再访问网关用户接口，观察是否返回降级响应
curl -i http://localhost:9000/api/user/list
```

降级响应示例：

```json
{
  "code": 503,
  "message": "用户服务暂不可用，请稍后再试",
  "path": "/fallback/user-service",
  "traceId": "9d7b3c8e7c4f4b8d9bb51a0a2d0a1b2c",
  "timestamp": "2026-05-05 15:40:00"
}
```

下游服务异常处理建议如下：

| 场景                       | 推荐处理                             |
| -------------------------- | ------------------------------------ |
| 查询接口临时失败           | 可配置少量重试                       |
| 创建、支付、扣库存接口失败 | 不建议网关重试，应由业务层保证幂等   |
| 下游服务长时间不可用       | 使用熔断降级，快速失败               |
| 注册中心无实例             | 返回 `503`，提示服务暂不可用         |
| 下游响应慢                 | 配置合理超时，避免拖垮网关           |
| 文件上传下载               | 谨慎配置重试和响应包装，避免内存压力 |
| WebSocket / SSE            | 不建议做普通 JSON 降级包装           |

生产环境中，网关异常处理应和日志、链路追踪、监控告警配套使用。客户端拿到 `traceId` 后，后端可以通过网关日志和下游服务日志快速定位请求链路。

## 配置管理

本章用于说明 Spring Cloud Gateway 项目的配置管理方式，包括本地配置、Nacos 配置中心集成和多环境配置管理。该章节对应你给出的 `配置管理` 部分。

Gateway 的配置通常包含服务端口、路由规则、跨域、限流、鉴权、Nacos 注册发现、超时、日志、Actuator 等内容。开发初期可以使用本地 `application.yml` 管理；进入联调、测试和生产环境后，建议将可变配置迁移到 Nacos 配置中心，减少重新打包和手动改配置的风险。

### 本地配置方式

本地配置方式适合开发环境、单机调试和基础功能验证。推荐将通用配置放在 `application.yml`，将环境差异配置放在 `application-dev.yml`、`application-test.yml`、`application-prod.yml` 中。

推荐配置文件结构如下：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

文件职责说明如下：

| 文件                   | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| `application.yml`      | 通用配置，例如应用名称、基础路由、通用鉴权配置、Actuator 基础配置 |
| `application-dev.yml`  | 开发环境配置，例如本地 Nacos、Redis、调试日志                |
| `application-test.yml` | 测试环境配置，例如测试 Nacos、测试 Redis、测试服务地址       |
| `application-prod.yml` | 生产环境配置，例如生产 Nacos、生产 Redis、日志级别、监控端点收敛 |

文件位置：`src/main/resources/application.yml`

下面的配置用于管理 Gateway 通用配置，并通过 `spring.profiles.active` 指定默认环境。

```yaml
server:
  # 网关服务端口
  port: 9000

spring:
  application:
    # 网关服务名称
    name: gateway-service

  profiles:
    # 默认启用开发环境，生产环境应通过启动参数覆盖
    active: dev

  cloud:
    gateway:
      server:
        webflux:
          # 网关开关
          enabled: true

          # 默认过滤器，作用于所有路由
          default-filters:
            # 去重跨域响应头，避免网关和下游服务重复设置
            - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin, RETAIN_UNIQUE

          # 路由配置
          routes:
            - id: user-service-route
              uri: lb://user-service
              predicates:
                - Path=/api/user/**
              filters:
                - StripPrefix=1

            - id: order-service-route
              uri: lb://order-service
              predicates:
                - Path=/api/order/**
              filters:
                - StripPrefix=1

          httpclient:
            # 连接下游服务超时时间，单位毫秒
            connect-timeout: 3000

            # 下游服务响应超时时间
            response-timeout: 10s

gateway:
  auth:
    # 是否启用网关鉴权
    enabled: true

    # Token 请求头前缀
    token-prefix: Bearer

    # JWT 签名密钥，生产环境必须通过环境变量或配置中心注入
    token-secret: ateng-gateway-demo-secret

    # 白名单路径
    white-list:
      - /actuator/health
      - /actuator/info
      - /api/auth/login
      - /api/auth/captcha
      - /api/open/**

management:
  endpoint:
    gateway:
      # Gateway Actuator 端点建议只读，避免远程新增、删除、刷新路由带来安全风险
      access: read-only

  endpoints:
    web:
      exposure:
        # 开发和测试环境可打开 gateway，生产环境需结合鉴权和内网访问控制
        include: health,info,gateway

logging:
  level:
    # 网关基础日志
    org.springframework.cloud.gateway: info

    # 当前项目日志
    io.github.atengk.gateway: info
```

Spring Cloud Gateway 的 `/actuator/gateway` 端点默认需要显式开放访问，官方文档建议将 `management.endpoint.gateway.access` 设置为 `read-only`，并通过 `management.endpoints.web.exposure.include=gateway` 暴露端点；如果开放 unrestricted 或允许创建、删除、刷新路由，需要额外做好 Actuator 安全保护。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/actuator-api.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-dev.yml`

下面的配置用于本地开发环境，主要覆盖 Nacos、Redis 和日志级别。

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # 本地 Nacos 注册中心地址
        server-addr: 127.0.0.1:8848

        # 开发环境命名空间
        namespace: public

        # 开发环境服务分组
        group: DEFAULT_GROUP

  data:
    redis:
      # 本地 Redis 地址
      host: 127.0.0.1

      # 本地 Redis 端口
      port: 6379

      # 本地 Redis 数据库
      database: 0

logging:
  level:
    # 开发环境打开 Gateway 调试日志
    org.springframework.cloud.gateway: debug
    io.github.atengk.gateway: debug
```

文件位置：`src/main/resources/application-prod.yml`

下面的配置用于生产环境，重点收敛日志、监控端点和敏感配置。

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # 生产环境 Nacos 地址，建议通过环境变量注入
        server-addr: ${NACOS_SERVER_ADDR}

        # 生产环境命名空间 ID
        namespace: ${NACOS_NAMESPACE}

        # 生产环境分组
        group: PROD_GROUP

        # Nacos 用户名
        username: ${NACOS_USERNAME}

        # Nacos 密码
        password: ${NACOS_PASSWORD}

  data:
    redis:
      # 生产环境 Redis 地址
      host: ${REDIS_HOST}

      # 生产环境 Redis 端口
      port: ${REDIS_PORT:6379}

      # 生产环境 Redis 密码
      password: ${REDIS_PASSWORD}

management:
  endpoints:
    web:
      exposure:
        # 生产环境只暴露必要端点
        include: health,info

logging:
  level:
    # 生产环境减少调试日志
    org.springframework.cloud.gateway: warn
    reactor.netty: warn
    io.github.atengk.gateway: info
```

本地启动命令如下：

```bash
# 使用 dev 环境启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 打包后使用 dev 环境启动
mvn clean package -DskipTests
java -jar target/spring-cloud-gateway-demo-1.0.0.jar --spring.profiles.active=dev

# 使用生产环境配置启动，敏感参数通过环境变量注入
export NACOS_SERVER_ADDR=10.0.0.10:8848
export NACOS_NAMESPACE=prod-namespace-id
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=your-password
export REDIS_HOST=10.0.0.20
export REDIS_PASSWORD=your-redis-password

java -jar target/spring-cloud-gateway-demo-1.0.0.jar --spring.profiles.active=prod
```

本地配置方式的优点是简单直接，缺点是配置变更需要重新发布或重启服务。对于路由、限流阈值、白名单、跨域域名、下游服务超时等经常调整的配置，建议迁移到配置中心。

### Nacos 配置中心集成

Nacos 配置中心用于集中管理 Gateway 配置。接入后，配置可以存放在 Nacos 控制台中，应用启动时拉取配置；部分配置还可以开启动态刷新。Spring Cloud Alibaba 2025.x 官方文档说明，接入 Nacos 配置中心必须使用 `spring.config.import` 方式导入配置；`2025.1.x` 已废弃 `bootstrap.yml` / `bootstrap.properties` 引导方式，`2025.0.x` 虽仍兼容，但也推荐使用 `spring.config.import`。([Spring Cloud Alibaba](https://sca.aliyun.com/docs/2025.x/user-guide/nacos/advanced-guide/?utm_source=chatgpt.com))

文件位置：`pom.xml`

下面的依赖用于启用 Nacos Config 配置中心能力。

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Cloud BOM：统一管理 Gateway、LoadBalancer 等组件版本 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Spring Cloud Alibaba BOM：统一管理 Nacos Config、Nacos Discovery 等组件版本 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Nacos Config：配置中心依赖 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>

    <!-- Nacos Discovery：服务注册与发现依赖 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

下面的配置用于接入 Nacos 配置中心，并从 Nacos 加载公共配置和当前环境配置。

```yaml
spring:
  application:
    # 应用名称，建议和 Nacos Data ID 保持一致
    name: gateway-service

  profiles:
    # 默认开发环境
    active: dev

  cloud:
    nacos:
      config:
        # Nacos 配置中心地址
        server-addr: 127.0.0.1:8848

        # Nacos 命名空间，public 可不填；生产建议使用独立 namespace
        namespace: public

        # Nacos 配置分组
        group: DEFAULT_GROUP

        # Nacos 用户名，未开启认证时可删除
        username: nacos

        # Nacos 密码，生产环境建议使用环境变量注入
        password: nacos

        # 是否开启动态刷新
        refresh-enabled: true

      discovery:
        # Nacos 注册中心地址
        server-addr: 127.0.0.1:8848
        namespace: public
        group: DEFAULT_GROUP

  config:
    import:
      # 公共配置：适合放通用日志、通用超时、公共鉴权开关等
      - optional:nacos:gateway-common.yml?group=DEFAULT_GROUP&refreshEnabled=true

      # 当前环境配置：dev/test/prod 根据 spring.profiles.active 自动选择
      - optional:nacos:gateway-service-${spring.profiles.active}.yml?group=DEFAULT_GROUP&refreshEnabled=true
```

`optional:nacos:` 表示 Nacos 配置不存在时不阻断应用启动；如果去掉 `optional:`，当配置中心不可用或配置不存在时，应用会快速失败。官方文档也说明，引入 `spring-cloud-starter-alibaba-nacos-config` 后，应用会检查是否存在 `spring.config.import` 中的 `nacos:` 条目，如果缺失会提示添加 `spring.config.import=nacos:` 或 `optional:nacos:`。([Spring Cloud Alibaba](https://sca.aliyun.com/docs/2025.x/user-guide/nacos/advanced-guide/?utm_source=chatgpt.com))

Nacos 控制台中建议创建以下配置：

```text
Data ID: gateway-common.yml
Group: DEFAULT_GROUP
Format: YAML
```

`gateway-common.yml` 示例内容如下：

```yaml
server:
  # 网关服务端口
  port: 9000

spring:
  cloud:
    gateway:
      server:
        webflux:
          httpclient:
            # 连接下游服务超时时间
            connect-timeout: 3000

            # 下游服务响应超时时间
            response-timeout: 10s

management:
  endpoint:
    gateway:
      # Gateway Actuator 端点只读
      access: read-only

  endpoints:
    web:
      exposure:
        # 暴露健康检查和网关路由查看端点
        include: health,info,gateway
```

开发环境配置：

```text
Data ID: gateway-service-dev.yml
Group: DEFAULT_GROUP
Format: YAML
```

`gateway-service-dev.yml` 示例内容如下：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service-route
              uri: lb://user-service
              predicates:
                - Path=/api/user/**
              filters:
                - StripPrefix=1

            - id: order-service-route
              uri: lb://order-service
              predicates:
                - Path=/api/order/**
              filters:
                - StripPrefix=1

gateway:
  auth:
    # 开发环境启用鉴权
    enabled: true

    # 开发环境 Token 密钥
    token-secret: ateng-gateway-demo-secret

    # 开发环境白名单
    white-list:
      - /actuator/health
      - /actuator/info
      - /api/auth/login
      - /api/auth/captcha
      - /api/open/**

logging:
  level:
    org.springframework.cloud.gateway: debug
    io.github.atengk.gateway: debug
```

生产环境配置：

```text
Data ID: gateway-service-prod.yml
Group: PROD_GROUP
Format: YAML
```

`gateway-service-prod.yml` 示例内容如下：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service-route
              uri: lb://user-service
              predicates:
                - Path=/api/user/**
              filters:
                - StripPrefix=1

            - id: order-service-route
              uri: lb://order-service
              predicates:
                - Path=/api/order/**
              filters:
                - StripPrefix=1

gateway:
  auth:
    # 生产环境启用鉴权
    enabled: true

    # 生产环境 Token 密钥应使用更安全的配置注入方式
    token-secret: ${GATEWAY_TOKEN_SECRET}

    # 生产环境白名单必须严格控制
    white-list:
      - /actuator/health
      - /api/auth/login
      - /api/auth/captcha

logging:
  level:
    org.springframework.cloud.gateway: warn
    io.github.atengk.gateway: info
```

修改 Nacos 配置后，可以通过 Gateway Actuator 刷新路由缓存：

```bash
# 查看当前 Gateway 路由
curl http://localhost:9000/actuator/gateway/routes

# 刷新 Gateway 路由缓存
curl -X POST http://localhost:9000/actuator/gateway/refresh
```

Spring Cloud Gateway 官方文档说明，`/actuator/gateway/routes` 可以查看已定义路由，`/actuator/gateway/refresh` 可以刷新路由缓存。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/actuator-api.html?utm_source=chatgpt.com))

### 多环境配置管理

多环境配置管理的目标是将开发、测试、预发、生产环境隔离开，避免配置串用。建议从三个维度隔离：`profile`、Nacos `namespace`、Nacos `group`。

推荐环境规划如下：

| 环境     | Spring Profile | Nacos Namespace             | Nacos Group     | 说明           |
| -------- | -------------- | --------------------------- | --------------- | -------------- |
| 开发环境 | `dev`          | `public` 或 `dev` namespace | `DEFAULT_GROUP` | 本地开发、联调 |
| 测试环境 | `test`         | `test` namespace            | `TEST_GROUP`    | 测试人员验证   |
| 预发环境 | `pre`          | `pre` namespace             | `PRE_GROUP`     | 生产前验证     |
| 生产环境 | `prod`         | `prod` namespace            | `PROD_GROUP`    | 正式流量       |

文件位置：`src/main/resources/application.yml`

下面的配置通过环境变量控制 Nacos 地址、命名空间和分组，适合容器化部署。

```yaml
spring:
  application:
    name: gateway-service

  profiles:
    # 默认 dev，部署时通过 SPRING_PROFILES_ACTIVE 覆盖
    active: ${SPRING_PROFILES_ACTIVE:dev}

  cloud:
    nacos:
      config:
        # Nacos 配置中心地址
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}

        # Nacos 配置命名空间
        namespace: ${NACOS_CONFIG_NAMESPACE:public}

        # Nacos 配置分组
        group: ${NACOS_CONFIG_GROUP:DEFAULT_GROUP}

        # Nacos 用户名
        username: ${NACOS_USERNAME:nacos}

        # Nacos 密码
        password: ${NACOS_PASSWORD:nacos}

        # 开启配置刷新
        refresh-enabled: true

      discovery:
        # Nacos 注册中心地址
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}

        # Nacos 服务发现命名空间
        namespace: ${NACOS_DISCOVERY_NAMESPACE:public}

        # Nacos 服务发现分组
        group: ${NACOS_DISCOVERY_GROUP:DEFAULT_GROUP}

  config:
    import:
      # 公共配置
      - optional:nacos:gateway-common.yml?group=${NACOS_CONFIG_GROUP:DEFAULT_GROUP}&refreshEnabled=true

      # 环境配置
      - optional:nacos:gateway-service-${spring.profiles.active}.yml?group=${NACOS_CONFIG_GROUP:DEFAULT_GROUP}&refreshEnabled=true
```

不同环境启动示例：

```bash
# 开发环境
export SPRING_PROFILES_ACTIVE=dev
export NACOS_SERVER_ADDR=127.0.0.1:8848
export NACOS_CONFIG_NAMESPACE=public
export NACOS_CONFIG_GROUP=DEFAULT_GROUP
java -jar target/spring-cloud-gateway-demo-1.0.0.jar

# 测试环境
export SPRING_PROFILES_ACTIVE=test
export NACOS_SERVER_ADDR=10.0.1.10:8848
export NACOS_CONFIG_NAMESPACE=test-namespace-id
export NACOS_CONFIG_GROUP=TEST_GROUP
java -jar target/spring-cloud-gateway-demo-1.0.0.jar

# 生产环境
export SPRING_PROFILES_ACTIVE=prod
export NACOS_SERVER_ADDR=10.0.2.10:8848
export NACOS_CONFIG_NAMESPACE=prod-namespace-id
export NACOS_CONFIG_GROUP=PROD_GROUP
export GATEWAY_TOKEN_SECRET=your-production-token-secret
java -jar target/spring-cloud-gateway-demo-1.0.0.jar
```

多环境配置建议如下：

| 配置项            | 建议                                                      |
| ----------------- | --------------------------------------------------------- |
| 路由配置          | 可放入 Nacos，便于联调和灰度调整                          |
| 鉴权密钥          | 生产环境不要写死在 Git 仓库中                             |
| Redis 密码        | 使用环境变量、密钥管理或配置中心加密能力                  |
| 白名单路径        | 生产环境严格收敛，不保留测试接口                          |
| Actuator 暴露端点 | 生产环境只保留 `health`、`info`，Gateway 端点需内网或鉴权 |
| 日志级别          | 开发环境 `debug`，生产环境 `info` 或 `warn`               |
| Nacos Namespace   | 不同环境必须隔离，避免测试配置影响生产                    |

## 接口测试

本章用于说明 Gateway 常见功能的接口测试方式，包括路由转发测试、过滤器功能测试、鉴权流程测试和限流效果测试。该章节对应你给出的 `接口测试` 部分。

测试前需要确保 Gateway、Nacos、Redis 和至少一个下游服务已经启动。如果使用 `lb://user-service` 路由，还需要确认 `user-service` 已注册到 Nacos，并且实例状态健康。

### 路由转发测试

路由转发测试用于确认请求是否能从 Gateway 正确转发到下游服务。测试重点包括路由是否命中、路径是否正确改写、服务名是否能解析、下游响应是否能正常返回。

测试前建议先查看 Gateway 当前加载的路由：

```bash
# 查看 Gateway 当前已加载路由
curl http://localhost:9000/actuator/gateway/routes

# 查看 Gateway 路由定义
curl http://localhost:9000/actuator/gateway/routedefinitions
```

如果返回结果中包含 `user-service-route`，说明 Gateway 已加载用户服务路由。官方文档说明，`/actuator/gateway/routes` 可以返回已定义路由信息，包括路由 ID、断言、过滤器、URI 和排序信息。([Home](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/actuator-api.html?utm_source=chatgpt.com))

文件位置：`scripts/test-route.sh`

下面的脚本用于测试用户服务和订单服务的基础路由转发。

```bash
#!/usr/bin/env bash

# 网关地址
GATEWAY_URL="http://localhost:9000"

echo "测试用户服务路由..."
curl -i "${GATEWAY_URL}/api/user/context"

echo
echo "测试订单服务路由..."
curl -i "${GATEWAY_URL}/api/order/page"

echo
echo "查看 Gateway 路由列表..."
curl -s "${GATEWAY_URL}/actuator/gateway/routes"
```

执行脚本：

```bash
chmod +x scripts/test-route.sh
./scripts/test-route.sh
```

预期结果如下：

| 测试项                     | 预期结果                    |
| -------------------------- | --------------------------- |
| `/api/user/context`        | 命中 `user-service-route`   |
| `/api/order/page`          | 命中 `order-service-route`  |
| `/actuator/gateway/routes` | 能看到当前网关路由信息      |
| 下游服务未启动             | 返回 `503` 或统一异常响应   |
| 路径写错                   | 返回 `404` 或路由不存在响应 |

如果路由转发失败，按下面顺序排查：

```bash
# 1. 检查 Gateway 是否启动
curl http://localhost:9000/actuator/health

# 2. 检查路由是否加载
curl http://localhost:9000/actuator/gateway/routes

# 3. 检查 Nacos 中是否有 user-service 健康实例
# 需要登录 Nacos 控制台查看服务列表

# 4. 如果使用静态路由，直接测试下游服务
curl http://localhost:8081/user/context
```

### 过滤器功能测试

过滤器功能测试用于确认 `StripPrefix`、请求头注入、链路 ID、请求日志、自定义 Header 校验等过滤器是否生效。测试时建议下游服务提供一个回显接口，用于返回请求头和请求路径。

文件位置：`user-service/src/main/java/io/github/atengk/user/controller/EchoController.java`

下面的 Controller 用于在下游服务中回显请求路径和关键请求头，便于验证网关过滤器行为。

```java
package io.github.atengk.user.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 请求回显测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class EchoController {

    /**
     * 回显请求信息
     *
     * @param request 请求对象
     * @param headers 请求头
     * @return 请求信息
     */
    @GetMapping("/user/echo")
    public Map<String, Object> echo(HttpServletRequest request,
                                    @RequestHeader HttpHeaders headers) {
        String traceId = headers.getFirst("X-Trace-Id");
        String gatewaySource = headers.getFirst("X-Gateway-Source");
        String forwardedSystem = headers.getFirst("X-Forwarded-System");

        log.info("接收到网关转发请求，path={}, traceId={}", request.getRequestURI(), traceId);

        Map<String, Object> result = MapUtil.newHashMap(true);
        result.put("path", request.getRequestURI());
        result.put("traceId", StrUtil.blankToDefault(traceId, "-"));
        result.put("gatewaySource", StrUtil.blankToDefault(gatewaySource, "-"));
        result.put("forwardedSystem", StrUtil.blankToDefault(forwardedSystem, "-"));
        result.put("userId", StrUtil.blankToDefault(headers.getFirst("X-User-Id"), "-"));
        return result;
    }

}
```

文件位置：`scripts/test-filter.sh`

下面的脚本用于测试路径改写、请求头透传和自定义过滤器行为。

```bash
#!/usr/bin/env bash

# 网关地址
GATEWAY_URL="http://localhost:9000"

echo "测试 StripPrefix 和链路 ID 透传..."
curl -i "${GATEWAY_URL}/api/user/echo" \
  -H "X-Trace-Id: filter-test-001"

echo
echo "测试客户端伪造用户 Header 是否被网关覆盖或移除..."
curl -i "${GATEWAY_URL}/api/user/echo" \
  -H "X-Trace-Id: filter-test-002" \
  -H "X-User-Id: fake-user-id"

echo
echo "测试自定义 CheckHeader 过滤器，缺少 Header 应返回 401..."
curl -i "${GATEWAY_URL}/api/admin/menu"

echo
echo "测试自定义 CheckHeader 过滤器，Header 正确应放行..."
curl -i "${GATEWAY_URL}/api/admin/menu" \
  -H "X-Internal-Token: ateng-demo-token"
```

执行脚本：

```bash
chmod +x scripts/test-filter.sh
./scripts/test-filter.sh
```

预期结果如下：

| 测试项                  | 预期结果                                           |
| ----------------------- | -------------------------------------------------- |
| `StripPrefix`           | 下游收到 `/user/echo`，不是 `/api/user/echo`       |
| `X-Trace-Id`            | 下游能收到请求中的链路 ID                          |
| `X-Gateway-Source`      | 如果配置了 `AddRequestHeader`，下游能收到该 Header |
| 伪造 `X-User-Id`        | 网关鉴权启用后，应由网关重新写入可信用户 ID        |
| 缺少 `X-Internal-Token` | 自定义过滤器返回 `401`                             |
| `X-Internal-Token` 正确 | 请求放行到下游服务                                 |

如果日志中没有看到请求日志，检查 `RequestLogGlobalFilter` 是否被 Spring 扫描到，以及日志级别是否允许输出。

### 鉴权流程测试

鉴权流程测试用于确认白名单放行、未登录拦截、Token 校验、用户信息透传等能力是否正常。测试前需要准备一个合法 JWT。下面给出一个简单的 Token 生成工具类，用于本地测试。

文件位置：`src/test/java/io/github/atengk/gateway/JwtTokenGenerator.java`

下面的测试工具类用于生成 Gateway 鉴权测试 Token。

```java
package io.github.atengk.gateway;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWTUtil;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 测试 Token 生成工具
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class JwtTokenGenerator {

    /**
     * 生成测试 Token
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        String secret = "ateng-gateway-demo-secret";

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", "10001");
        payload.put("username", "ateng");
        payload.put("tenantId", "default");
        payload.put("iat", DateUtil.currentSeconds());
        payload.put("exp", DateUtil.offsetHour(DateUtil.date(), 2).getTime() / 1000);

        String token = JWTUtil.createToken(payload, secret.getBytes(StandardCharsets.UTF_8));
        System.out.println(token);
    }

}
```

执行方式：

```bash
# 编译测试类
mvn test-compile

# 执行 Token 生成类
mvn exec:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=io.github.atengk.gateway.JwtTokenGenerator
```

如果项目未引入 `exec-maven-plugin`，也可以在 IDE 中直接运行 `JwtTokenGenerator.main()` 方法。

文件位置：`scripts/test-auth.sh`

下面的脚本用于测试白名单、未登录、非法 Token、合法 Token 和用户信息透传。

```bash
#!/usr/bin/env bash

# 网关地址
GATEWAY_URL="http://localhost:9000"

# 替换为 JwtTokenGenerator 生成的 Token
TOKEN="${1}"

echo "测试白名单接口，应该直接放行..."
curl -i "${GATEWAY_URL}/actuator/health"

echo
echo "测试未携带 Token 的业务接口，应该返回 401..."
curl -i "${GATEWAY_URL}/api/user/context"

echo
echo "测试非法 Token，应该返回 401..."
curl -i "${GATEWAY_URL}/api/user/context" \
  -H "Authorization: Bearer invalid-token"

echo
if [ -z "${TOKEN}" ]; then
  echo "未传入合法 Token，跳过合法 Token 测试"
  echo "用法：./scripts/test-auth.sh <token>"
  exit 0
fi

echo "测试合法 Token，应该放行并透传用户信息..."
curl -i "${GATEWAY_URL}/api/user/context" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "X-User-Id: fake-user-id"
```

执行脚本：

```bash
chmod +x scripts/test-auth.sh

# 不带 Token，验证白名单、未登录和非法 Token
./scripts/test-auth.sh

# 带合法 Token，验证用户信息透传
./scripts/test-auth.sh "替换为生成的JWT"
```

鉴权测试预期结果如下：

| 测试项       | 请求                                  | 预期结果                             |
| ------------ | ------------------------------------- | ------------------------------------ |
| 白名单       | `/actuator/health`                    | 直接返回健康检查结果                 |
| 未登录       | `/api/user/context` 不带 Token        | 返回 `401`                           |
| 非法 Token   | `Authorization: Bearer invalid-token` | 返回 `401`                           |
| 合法 Token   | `Authorization: Bearer <token>`       | 请求放行                             |
| 用户信息透传 | 客户端伪造 `X-User-Id`                | 下游收到 Token 中解析出的真实用户 ID |

合法 Token 响应示例：

```json
{
  "userId": "10001",
  "username": "ateng",
  "tenantId": "default",
  "traceId": "auth-test-001"
}
```

如果合法 Token 仍然返回 `401`，优先检查以下内容：

| 检查项         | 说明                                             |
| -------------- | ------------------------------------------------ |
| `token-secret` | 生成 Token 和校验 Token 的密钥必须一致           |
| Header 格式    | 应使用 `Authorization: Bearer <token>`           |
| Payload 字段   | Token 中必须包含 `userId`                        |
| 白名单路径     | 白名单匹配的是网关入口路径，不是下游改写后的路径 |
| 过滤器顺序     | 链路 ID 过滤器应早于鉴权过滤器，日志过滤器应靠后 |

### 限流效果测试

限流效果测试用于确认 Redis 限流和自定义限流策略是否生效。测试前需要确认 Redis 已启动，Gateway 已引入 `spring-boot-starter-data-redis-reactive`，并且路由中配置了 `RequestRateLimiter`。Spring Cloud Gateway 官方文档说明，Redis 限流实现需要使用 `spring-boot-starter-data-redis-reactive`，算法采用令牌桶；请求超过限流规则时默认返回 `HTTP 429 Too Many Requests`。([Home](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html?utm_source=chatgpt.com))

先启动 Redis：

```bash
docker run -d \
  --name redis-gateway \
  -p 6379:6379 \
  redis:7.4
```

确认 Redis 连接正常：

```bash
docker exec -it redis-gateway redis-cli ping
```

预期返回：

```text
PONG
```

文件位置：`src/main/resources/application.yml`

下面的配置用于给用户服务接口启用 Redis 限流。

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-rate-limit-route
              uri: lb://user-service
              predicates:
                - Path=/api/user/**
              filters:
                - StripPrefix=1

                # Redis 令牌桶限流
                - name: RequestRateLimiter
                  args:
                    # 按用户 ID 限流，未登录时可回退到 IP
                    key-resolver: "#{@userRateLimitKeyResolver}"

                    # 每秒补充 2 个令牌
                    redis-rate-limiter.replenishRate: 2

                    # 令牌桶容量为 4，允许短时间突发 4 次
                    redis-rate-limiter.burstCapacity: 4

                    # 每个请求消耗 1 个令牌
                    redis-rate-limiter.requestedTokens: 1
```

文件位置：`scripts/test-rate-limit.sh`

下面的脚本用于连续请求同一个接口，验证是否触发限流。

```bash
#!/usr/bin/env bash

# 网关地址
GATEWAY_URL="http://localhost:9000"

# 可选 Token。如果接口启用了鉴权，需要传入合法 Token
TOKEN="${1}"

# 测试请求次数
TOTAL=20

echo "开始限流测试，请求次数：${TOTAL}"

for i in $(seq 1 ${TOTAL}); do
  if [ -n "${TOKEN}" ]; then
    STATUS=$(curl -s -o /tmp/gateway-rate-limit-response.txt -w "%{http_code}" \
      "${GATEWAY_URL}/api/user/context" \
      -H "Authorization: Bearer ${TOKEN}" \
      -H "X-Trace-Id: rate-limit-test-${i}")
  else
    STATUS=$(curl -s -o /tmp/gateway-rate-limit-response.txt -w "%{http_code}" \
      "${GATEWAY_URL}/api/user/context" \
      -H "X-User-Id: 10001" \
      -H "X-Trace-Id: rate-limit-test-${i}")
  fi

  echo "第 ${i} 次请求，HTTP 状态码：${STATUS}"

  if [ "${STATUS}" = "429" ]; then
    echo "已触发限流，响应内容："
    cat /tmp/gateway-rate-limit-response.txt
    echo
  fi
done
```

执行脚本：

```bash
chmod +x scripts/test-rate-limit.sh

# 未启用鉴权或白名单测试时
./scripts/test-rate-limit.sh

# 启用鉴权时，传入合法 Token
./scripts/test-rate-limit.sh "替换为生成的JWT"
```

预期结果如下：

| 测试项             | 预期结果                              |
| ------------------ | ------------------------------------- |
| 前几次请求         | 返回 `200` 或下游正常响应             |
| 超过令牌桶阈值     | 返回 `429`                            |
| Redis 未启动       | 限流器可能异常，需查看 Gateway 日志   |
| KeyResolver 未生效 | 所有请求可能使用同一个 Key 或无法限流 |
| 鉴权未通过         | 会先返回 `401`，不会进入业务限流验证  |

查看 Redis 中的限流 Key：

```bash
docker exec -it redis-gateway redis-cli

# 进入 redis-cli 后执行，查看 Gateway 限流相关 Key
keys request_rate_limiter.*
```

限流测试时需要注意过滤器顺序。如果鉴权过滤器在限流过滤器之前，未登录请求会先被 `401` 拦截，不会触发 Redis 限流；如果限流过滤器在鉴权之前，登录接口这类公开接口可以按 IP 限流。实际项目中通常建议：登录、验证码等公开接口按 IP 限流；登录后的业务接口按用户 ID 限流。

## 部署运行

本章用于说明 Gateway 项目的本地启动、Docker 镜像构建和生产环境配置要点。该章节对应你给出的 `部署运行` 部分。

Spring Cloud Gateway 本质是一个 Spring Boot WebFlux 应用，通常以可执行 Jar 或容器镜像方式部署。生产环境中，Gateway 位于外部负载均衡、Nginx、Ingress 或云网关之后，再向内转发到各个微服务。

### 本地启动方式

本地启动适合开发调试、路由验证、过滤器验证、鉴权联调和限流测试。启动前需要确认 JDK、Maven、Nacos、Redis 和下游服务是否准备完成。

本地运行前置条件如下：

| 组件     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| JDK      | 使用 JDK 17 或 21                                            |
| Maven    | 用于编译、打包、运行项目                                     |
| Nacos    | 如果使用 `lb://service-name` 路由，需要启动 Nacos            |
| Redis    | 如果启用 Redis 限流，需要启动 Redis                          |
| 下游服务 | 例如 `user-service`、`order-service`，需要注册到 Nacos 或使用静态地址 |

下面的命令用于启动本地 Nacos 和 Redis。

```bash
# 启动 Nacos 单机模式
docker run -d \
  --name nacos-standalone \
  -e MODE=standalone \
  -e NACOS_AUTH_ENABLE=false \
  -p 8848:8848 \
  -p 9848:9848 \
  nacos/nacos-server:v3.0.3

# 启动 Redis
docker run -d \
  --name redis-gateway \
  -p 6379:6379 \
  redis:7.4
```

进入 Gateway 项目根目录后，可以使用 Maven 直接启动。

```bash
# 使用 dev 环境启动 Gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

也可以先编译打包，再通过 Jar 启动。

```bash
# 编译并打包
mvn clean package -DskipTests

# 使用 dev 环境启动
java -jar target/spring-cloud-gateway-demo-1.0.0.jar --spring.profiles.active=dev
```

启动成功后，先检查健康状态。

```bash
# 检查 Gateway 健康状态
curl http://localhost:9000/actuator/health

# 查看 Gateway 当前加载的路由
curl http://localhost:9000/actuator/gateway/routes
```

Spring Cloud Gateway 的 `/actuator/gateway` 端点默认不是直接开放的，需要显式设置访问级别并暴露端点；官方文档建议将访问级别设置为 `read-only`，如果开放路由刷新、创建、删除等能力，则必须做好 Actuator 安全保护。([docs.spring.io](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/actuator-api.html?utm_source=chatgpt.com))

本地启动常用参数如下：

```bash
# 指定端口启动
java -jar target/spring-cloud-gateway-demo-1.0.0.jar \
  --server.port=9000 \
  --spring.profiles.active=dev

# 指定 Nacos 地址启动
java -jar target/spring-cloud-gateway-demo-1.0.0.jar \
  --spring.profiles.active=dev \
  --spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848 \
  --spring.cloud.nacos.config.server-addr=127.0.0.1:8848

# 指定 Redis 地址启动
java -jar target/spring-cloud-gateway-demo-1.0.0.jar \
  --spring.profiles.active=dev \
  --spring.data.redis.host=127.0.0.1 \
  --spring.data.redis.port=6379
```

如果本地启动失败，优先按以下顺序排查：

| 问题             | 排查方向                                                |
| ---------------- | ------------------------------------------------------- |
| 端口被占用       | 检查 `9000` 是否已被其他进程占用                        |
| Nacos 注册失败   | 检查 Nacos 地址、namespace、group、用户名密码           |
| Redis 连接失败   | 检查 Redis 是否启动、端口是否正确、密码是否一致         |
| 路由无法转发     | 检查下游服务是否注册到 Nacos，服务名是否一致            |
| Gateway 启动异常 | 检查是否误引入 `spring-boot-starter-web`                |
| 配置未生效       | 检查 `spring.profiles.active` 和 Nacos Data ID 是否一致 |

### Docker 镜像构建

Gateway 可以使用两种方式构建 Docker 镜像：第一种是使用 Dockerfile 手动构建，第二种是使用 Spring Boot Maven Plugin 的 `build-image` 目标基于 Cloud Native Buildpacks 构建 OCI 镜像。Spring Boot 官方文档说明，`spring-boot:build-image` 可以基于可执行 Jar 或 War 创建 OCI 镜像，并且需要访问 Docker daemon。([docs.spring.io](https://docs.spring.io/spring-boot/maven-plugin/build-image.html?utm_source=chatgpt.com))

#### 方式一：使用 Dockerfile 构建镜像

这种方式最直观，适合大多数项目和 CI/CD 流水线。

文件位置：`Dockerfile`

下面的 Dockerfile 用于将 Gateway 可执行 Jar 构建为容器镜像。

```dockerfile
# 使用 JRE 运行镜像，Gateway 运行时不需要完整 JDK
FROM eclipse-temurin:17-jre

# 设置工作目录
WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai

# JVM 参数，可在容器启动时覆盖
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC"

# Spring Profile，可在容器启动时覆盖
ENV SPRING_PROFILES_ACTIVE=prod

# 复制构建产物
COPY target/spring-cloud-gateway-demo-1.0.0.jar /app/app.jar

# 暴露网关端口
EXPOSE 9000

# 启动 Gateway
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
```

构建镜像前先打包项目。

```bash
# 编译打包
mvn clean package -DskipTests

# 构建 Docker 镜像
docker build -t gateway-service:1.0.0 .

# 查看镜像
docker images | grep gateway-service
```

本地运行镜像。

```bash
docker run -d \
  --name gateway-service \
  -p 9000:9000 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e NACOS_SERVER_ADDR=host.docker.internal:8848 \
  -e REDIS_HOST=host.docker.internal \
  gateway-service:1.0.0
```

如果在 Linux 环境中运行容器，`host.docker.internal` 不一定可用。可以改为宿主机实际 IP，或者将 Gateway、Nacos、Redis 放在同一个 Docker 网络中。

#### 方式二：使用 Docker Compose 本地编排

本地联调时，可以使用 Docker Compose 同时启动 Gateway、Nacos、Redis。下游服务也可以加入同一个网络，便于模拟测试环境。

文件位置：`docker-compose.yml`

下面的 Compose 文件用于本地启动 Gateway、Nacos 和 Redis。

```yaml
services:
  nacos:
    image: nacos/nacos-server:v3.0.3
    container_name: nacos-standalone
    environment:
      # 单机模式
      MODE: standalone

      # 本地开发关闭鉴权，生产环境必须开启鉴权
      NACOS_AUTH_ENABLE: "false"
    ports:
      - "8848:8848"
      - "9848:9848"
    networks:
      - gateway-net

  redis:
    image: redis:7.4
    container_name: redis-gateway
    ports:
      - "6379:6379"
    networks:
      - gateway-net

  gateway:
    image: gateway-service:1.0.0
    container_name: gateway-service
    depends_on:
      - nacos
      - redis
    ports:
      - "9000:9000"
    environment:
      # 启用开发环境
      SPRING_PROFILES_ACTIVE: dev

      # Nacos 地址，容器内使用服务名访问
      NACOS_SERVER_ADDR: nacos:8848

      # Redis 地址，容器内使用服务名访问
      REDIS_HOST: redis

      # Gateway Token 密钥，本地开发示例
      GATEWAY_TOKEN_SECRET: ateng-gateway-demo-secret
    networks:
      - gateway-net

networks:
  gateway-net:
    driver: bridge
```

执行命令如下：

```bash
# 先构建 Gateway 镜像
mvn clean package -DskipTests
docker build -t gateway-service:1.0.0 .

# 启动本地编排环境
docker compose up -d

# 查看容器状态
docker compose ps

# 查看 Gateway 日志
docker logs -f gateway-service

# 停止环境
docker compose down
```

#### 方式三：使用 Spring Boot build-image

如果团队希望减少手写 Dockerfile，也可以使用 Spring Boot Maven Plugin 构建 OCI 镜像。Spring Boot 官方文档说明，最简单的方式是执行 `mvn spring-boot:build-image`，也可以通过 `-Dspring-boot.build-image.imageName=...` 指定镜像名称。([docs.spring.io](https://docs.spring.io/spring-boot/maven-plugin/build-image.html?utm_source=chatgpt.com))

下面的命令用于直接构建 Gateway 镜像。

```bash
# 使用 Spring Boot Maven Plugin 构建 OCI 镜像
mvn spring-boot:build-image \
  -Dspring-boot.build-image.imageName=gateway-service:1.0.0

# 运行镜像
docker run -d \
  --name gateway-service \
  -p 9000:9000 \
  -e SPRING_PROFILES_ACTIVE=prod \
  gateway-service:1.0.0
```

`build-image` 方式的优点是镜像分层、JVM 运行参数、运行用户等由 Buildpacks 统一处理；官方文档也说明，出于安全原因，Buildpacks 构建出的镜像会以非 root 用户构建和运行。([docs.spring.io](https://docs.spring.io/spring-boot/maven-plugin/build-image.html?utm_source=chatgpt.com))

### 生产环境配置要点

生产环境部署 Gateway 时，重点不是“能启动”，而是要保证安全、稳定、可观测、可扩展。Gateway 是所有请求的入口，配置不当会直接影响全站访问。

生产部署推荐链路如下：

```text
用户 / 前端应用
  ↓
DNS / CDN
  ↓
负载均衡 / Ingress / Nginx
  ↓
Spring Cloud Gateway 集群
  ↓
Nacos / Redis / 业务微服务
```

生产环境建议至少部署两个 Gateway 实例，前面挂负载均衡，避免单点故障。Gateway 实例本身尽量保持无状态，登录态通过 Token 校验，限流状态通过 Redis 共享，路由配置通过 Nacos 配置中心管理。

生产环境核心配置建议如下：

| 配置项     | 建议                                             |
| ---------- | ------------------------------------------------ |
| 实例数量   | 至少 2 个 Gateway 实例                           |
| 注册中心   | Nacos 使用独立生产 namespace                     |
| 配置中心   | 生产配置单独 namespace 和 group                  |
| Redis      | 使用高可用 Redis 或云 Redis                      |
| 日志       | 保留访问日志、异常日志、traceId                  |
| Actuator   | 生产只暴露必要端点，并限制访问来源               |
| CORS       | 只允许正式前端域名                               |
| Token 密钥 | 不写入 Git，通过环境变量、密钥系统或配置中心注入 |
| 白名单     | 只保留登录、健康检查等必要接口                   |
| 超时       | 配置连接超时和响应超时，避免请求堆积             |
| 限流       | 登录、验证码、公开接口、核心接口分别限流         |
| 下游保护   | 配置重试、熔断、降级，但非幂等接口慎用重试       |

生产环境 `application-prod.yml` 示例：

```yaml
server:
  # 生产环境网关端口
  port: ${SERVER_PORT:9000}

spring:
  application:
    name: gateway-service

  cloud:
    nacos:
      discovery:
        # 生产 Nacos 地址
        server-addr: ${NACOS_SERVER_ADDR}

        # 生产命名空间
        namespace: ${NACOS_DISCOVERY_NAMESPACE}

        # 生产分组
        group: PROD_GROUP

        # Nacos 用户名
        username: ${NACOS_USERNAME}

        # Nacos 密码
        password: ${NACOS_PASSWORD}

      config:
        # 生产 Nacos 配置中心地址
        server-addr: ${NACOS_SERVER_ADDR}

        # 生产配置命名空间
        namespace: ${NACOS_CONFIG_NAMESPACE}

        # 生产配置分组
        group: PROD_GROUP

        # 开启配置刷新
        refresh-enabled: true

    gateway:
      server:
        webflux:
          httpclient:
            # 连接下游服务超时时间
            connect-timeout: 3000

            # 下游服务响应超时时间
            response-timeout: 10s

          globalcors:
            cors-configurations:
              '[/**]':
                # 生产环境只允许正式前端域名
                allowed-origins:
                  - https://www.example.com
                allowed-methods:
                  - GET
                  - POST
                  - PUT
                  - DELETE
                  - OPTIONS
                allowed-headers:
                  - Authorization
                  - Content-Type
                  - X-Trace-Id
                allow-credentials: true
                max-age: 3600

  data:
    redis:
      # 生产 Redis 地址
      host: ${REDIS_HOST}

      # 生产 Redis 端口
      port: ${REDIS_PORT:6379}

      # 生产 Redis 密码
      password: ${REDIS_PASSWORD}

gateway:
  auth:
    # 生产环境启用鉴权
    enabled: true

    # 生产 Token 密钥，必须从安全配置中注入
    token-secret: ${GATEWAY_TOKEN_SECRET}

    # 生产白名单严格收敛
    white-list:
      - /actuator/health
      - /api/auth/login
      - /api/auth/captcha

management:
  endpoints:
    web:
      exposure:
        # 生产环境只暴露必要端点
        include: health,info

  endpoint:
    health:
      # 不向外暴露详细健康信息
      show-details: never

logging:
  level:
    org.springframework.cloud.gateway: warn
    reactor.netty: warn
    io.github.atengk.gateway: info
```

容器启动示例：

```bash
docker run -d \
  --name gateway-service-1 \
  --restart=always \
  -p 9000:9000 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SERVER_PORT=9000 \
  -e NACOS_SERVER_ADDR=10.0.0.10:8848 \
  -e NACOS_DISCOVERY_NAMESPACE=prod-discovery-namespace \
  -e NACOS_CONFIG_NAMESPACE=prod-config-namespace \
  -e NACOS_USERNAME=nacos \
  -e NACOS_PASSWORD=your-nacos-password \
  -e REDIS_HOST=10.0.0.20 \
  -e REDIS_PORT=6379 \
  -e REDIS_PASSWORD=your-redis-password \
  -e GATEWAY_TOKEN_SECRET=your-production-token-secret \
  -e JAVA_OPTS="-Xms1024m -Xmx1024m -XX:+UseG1GC" \
  gateway-service:1.0.0
```

生产环境部署后，建议执行以下检查：

```bash
# 检查健康状态
curl http://网关内网地址:9000/actuator/health

# 检查基础路由
curl -i http://网关内网地址:9000/api/user/context

# 检查未登录拦截
curl -i http://网关内网地址:9000/api/user/context

# 检查登录接口限流
for i in $(seq 1 20); do
  curl -i http://网关内网地址:9000/api/auth/login
done
```

生产环境重点风险如下：

| 风险                     | 处理方式                                      |
| ------------------------ | --------------------------------------------- |
| Actuator 暴露过多        | 只暴露 `health`、`info`，敏感端点走内网和鉴权 |
| 业务服务可被绕过网关访问 | 网络层限制业务服务只允许网关或内网访问        |
| Token 密钥泄露           | 不写死在代码仓库，不打印到日志                |
| CORS 配置过宽            | 只允许正式前端域名                            |
| 网关日志过大             | 控制日志级别，不打印请求体和敏感 Header       |
| 非幂等接口重试           | 创建、支付、扣库存接口不要在网关层盲目重试    |
| Redis 单点               | 生产限流 Redis 使用高可用部署                 |
| Nacos 配置串环境         | 使用 namespace 和 group 隔离环境              |

## 项目实践总结

本章用于总结 Spring Boot 3 + Spring Cloud Gateway 项目开发中的常见问题、开发注意事项和后续扩展方向。该章节对应你给出的 `项目实践总结` 部分。

Gateway 项目的核心目标是统一入口、稳定转发、控制风险、便于排查。开发时不要把 Gateway 写成业务服务，也不要把所有能力都堆在网关层。网关适合处理横切逻辑，业务逻辑应留在业务微服务中。

### 常见问题

#### 1. 启动时报 WebFlux 和 WebMVC 冲突

常见原因是项目同时引入了 `spring-boot-starter-web` 和 `spring-cloud-starter-gateway-server-webflux`。Gateway WebFlux 运行在响应式模型上，不建议同时引入传统 Servlet Web 依赖。

处理方式：

```xml
<!-- Gateway 项目保留 WebFlux Gateway 依赖 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
</dependency>

<!-- 不要在 Gateway 项目中引入 spring-boot-starter-web -->
```

#### 2. 配置了路由但请求返回 404

常见原因包括路径没有匹配、配置前缀写错、Nacos 配置未加载、路由缓存未刷新。

排查命令：

```bash
# 查看健康状态
curl http://localhost:9000/actuator/health

# 查看当前路由
curl http://localhost:9000/actuator/gateway/routes

# 刷新路由缓存，仅在允许刷新时使用
curl -X POST http://localhost:9000/actuator/gateway/refresh
```

如果使用 Spring Cloud Gateway Server WebFlux 新配置前缀，应优先检查是否写在：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service-route
              uri: lb://user-service
              predicates:
                - Path=/api/user/**
```

#### 3. 使用 `lb://user-service` 返回 503

常见原因是注册中心没有可用实例，或者服务名、namespace、group 不一致。

排查方向：

| 检查项            | 说明                                         |
| ----------------- | -------------------------------------------- |
| Nacos 服务列表    | 是否存在 `user-service`                      |
| 实例健康状态      | 是否为健康实例                               |
| 服务名大小写      | `lb://user-service` 必须和注册中心服务名一致 |
| namespace         | Gateway 和下游服务是否在同一 namespace       |
| group             | Gateway 和下游服务是否在同一 group           |
| LoadBalancer 依赖 | 是否引入 `spring-cloud-starter-loadbalancer` |

#### 4. 跨域配置后仍然报 CORS 错误

常见原因是前端域名未加入 `allowed-origins`、预检请求没有被处理、网关和下游服务重复设置 CORS Header。

处理建议：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          globalcors:
            # 让 OPTIONS 预检请求也能被处理
            add-to-simple-url-handler-mapping: true

            cors-configurations:
              '[/**]':
                allowed-origins:
                  - http://localhost:5173
                allowed-methods:
                  - GET
                  - POST
                  - PUT
                  - DELETE
                  - OPTIONS
                allowed-headers:
                  - Authorization
                  - Content-Type
                allow-credentials: true

          default-filters:
            # 去重跨域响应头
            - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin, RETAIN_UNIQUE
```

#### 5. Redis 限流不生效

常见原因包括未引入 Redis Reactive 依赖、`RequestRateLimiter` 使用了简写形式、`KeyResolver` Bean 名称写错、Redis 未连接成功。

正确配置示例：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-rate-limit-route
              uri: lb://user-service
              predicates:
                - Path=/api/user/**
              filters:
                - StripPrefix=1
                - name: RequestRateLimiter
                  args:
                    key-resolver: "#{@userRateLimitKeyResolver}"
                    redis-rate-limiter.replenishRate: 5
                    redis-rate-limiter.burstCapacity: 10
                    redis-rate-limiter.requestedTokens: 1
```

`RequestRateLimiter` 官方文档明确说明，该过滤器不支持快捷配置写法，需要使用 `name` 和 `args` 方式配置。([docs.spring.io](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html?utm_source=chatgpt.com))

#### 6. 鉴权过滤器没有生效

常见原因是过滤器没有被 Spring 扫描到、白名单配置过宽、路径匹配写错、过滤器顺序不合理。

排查方向：

| 检查项       | 说明                                           |
| ------------ | ---------------------------------------------- |
| 包扫描路径   | 过滤器类是否位于启动类同级或子包               |
| `@Component` | 过滤器是否注册为 Spring Bean                   |
| 白名单       | 是否误写成 `/api/**` 这类过宽配置              |
| 请求路径     | 白名单匹配的是网关入口路径                     |
| 日志级别     | 开发环境打开 `io.github.atengk.gateway: debug` |
| 过滤器顺序   | 鉴权应在请求日志统计前执行                     |

#### 7. 下游服务拿不到用户信息 Header

常见原因是鉴权过滤器未通过、用户 Header 被后续过滤器移除、下游服务直接访问而不是通过网关访问。

处理建议：

| 检查项              | 说明                                          |
| ------------------- | --------------------------------------------- |
| Token 是否合法      | Gateway 是否解析出 `userId`                   |
| Header 名称是否一致 | 网关和下游统一使用 `X-User-Id`                |
| 是否绕过网关        | 直接访问下游不会有网关透传 Header             |
| 是否被 Nginx 移除   | 外层代理是否过滤了自定义 Header               |
| 下游读取方式        | Controller 中用 `@RequestHeader` 或拦截器读取 |

### 开发注意事项

Gateway 开发时应遵循“入口治理、轻业务、可观测、可回滚”的原则。

#### 1. 不在 Gateway 中写复杂业务逻辑

Gateway 可以做鉴权、限流、日志、跨域、路由、灰度、请求头透传，但不适合写订单计算、库存扣减、复杂权限树查询、报表统计等业务逻辑。

推荐边界如下：

| 能力           | 是否适合放 Gateway |
| -------------- | ------------------ |
| Token 基础校验 | 适合               |
| 用户 ID 透传   | 适合               |
| 接口限流       | 适合               |
| CORS           | 适合               |
| 请求日志       | 适合               |
| 复杂业务权限   | 不建议             |
| 数据库写入业务 | 不建议             |
| 复杂聚合查询   | 不建议             |
| 文件内容处理   | 不建议             |

#### 2. 避免在过滤器中执行阻塞操作

Gateway WebFlux 基于响应式模型，过滤器中不要直接执行长时间阻塞操作，例如阻塞式数据库查询、阻塞式 HTTP 调用、大文件读取、复杂 CPU 计算等。

不推荐写法：

```java
String result = restTemplate.getForObject("http://auth-service/check", String.class);
```

推荐做法是减少远程依赖，将常用鉴权信息放到 Token 中，或者使用缓存、异步响应式客户端、短超时和降级策略。

#### 3. 过滤器顺序要明确

多个过滤器同时存在时，必须明确执行顺序。建议顺序如下：

| 过滤器           | 顺序       | 说明                        |
| ---------------- | ---------- | --------------------------- |
| 链路追踪过滤器   | `-100`     | 先生成 `traceId`            |
| 鉴权过滤器       | `-90`      | 校验 Token，透传用户信息    |
| 自定义限流过滤器 | `-80`      | 按用户、IP、路径限流        |
| 业务路由过滤器   | 默认       | 路由级路径处理、Header 改写 |
| 请求日志过滤器   | 最低优先级 | 最后记录状态码和耗时        |

#### 4. 生产环境谨慎开放 Actuator

Gateway Actuator 可以查看路由，也可能刷新、创建、删除路由。生产环境建议只暴露 `health`、`info`，如果需要查看 Gateway 路由，应限制在内网、运维平台或加鉴权访问。Spring Cloud Gateway 官方文档也建议将 Gateway Actuator 端点访问级别设置为 `read-only`，如果开启 unrestricted 访问，需要确保 Actuator 端点安全。([docs.spring.io](https://docs.spring.io/spring-cloud-gateway/reference/4.3/spring-cloud-gateway-server-webflux/actuator-api.html?utm_source=chatgpt.com))

#### 5. 不打印敏感信息

请求日志中不要打印以下内容：

| 敏感信息        | 风险                       |
| --------------- | -------------------------- |
| `Authorization` | 泄露 Token                 |
| `Cookie`        | 泄露会话信息               |
| 密码字段        | 泄露用户密码               |
| 手机号、身份证  | 泄露隐私数据               |
| 请求体全文      | 可能包含业务敏感数据       |
| 响应体全文      | 可能包含隐私或内部错误信息 |

建议日志保留以下信息：

```text
traceId
routeId
method
path
status
cost
clientIp
userId
```

#### 6. 路由配置要有命名规范

路由 ID 建议使用清晰命名，避免出现 `route1`、`test-route`、`abc` 这类不可维护名称。

推荐命名：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service-route
              uri: lb://user-service
              predicates:
                - Path=/api/user/**

            - id: order-service-route
              uri: lb://order-service
              predicates:
                - Path=/api/order/**

            - id: auth-login-rate-limit-route
              uri: lb://auth-service
              predicates:
                - Path=/api/auth/login
```

#### 7. 重试只用于幂等接口

网关层重试容易导致重复请求。查询接口通常可以重试，但创建订单、支付、扣库存、发券、发短信等接口不建议在网关层盲目重试。

| 接口类型 | 是否建议 Gateway 重试 |
| -------- | --------------------- |
| 查询列表 | 可以少量重试          |
| 查询详情 | 可以少量重试          |
| 创建订单 | 不建议                |
| 支付扣款 | 不建议                |
| 扣库存   | 不建议                |
| 发短信   | 不建议                |
| 文件上传 | 谨慎使用              |

#### 8. 下游服务必须限制直接访问

如果业务服务可以被外部直接访问，攻击者可能绕过 Gateway 鉴权，并伪造 `X-User-Id` 等 Header。因此生产环境必须在网络层限制业务服务访问来源。

常见方式：

| 方式                     | 说明                                |
| ------------------------ | ----------------------------------- |
| 安全组                   | 只允许 Gateway 所在网段访问业务服务 |
| Kubernetes NetworkPolicy | 限制 Pod 间访问                     |
| 内网负载均衡             | 业务服务只暴露内网地址              |
| 服务网格策略             | 使用 mTLS 和访问策略                |
| Nginx 内网代理           | 外部只暴露 Gateway                  |

### 后续扩展方向

当前文档已经覆盖 Gateway 的基础路由、过滤器、服务发现、鉴权、跨域、限流、异常、配置、测试和部署。后续可以继续从安全、流量治理、可观测性和平台化几个方向扩展。

#### 1. 接入统一认证体系

当前示例使用 Hutool JWT 做基础 Token 校验，适合教学和中小项目快速落地。生产环境可以根据团队技术栈扩展为：

| 方案                                   | 说明                                   |
| -------------------------------------- | -------------------------------------- |
| Sa-Token                               | 适合国内常见后台系统、权限系统         |
| Spring Security OAuth2 Resource Server | 适合标准 OAuth2 / OIDC 体系            |
| Keycloak                               | 适合企业统一身份认证                   |
| 自研认证中心                           | 适合已有统一登录、租户、权限体系的公司 |
| API Key + 签名                         | 适合开放平台、第三方接口调用           |

#### 2. 接入 Sentinel 或 Resilience4J

如果需要更完善的流量治理，可以接入 Sentinel 或 Resilience4J，实现熔断、降级、热点参数限流、系统保护等能力。

可扩展方向：

| 能力           | 说明                   |
| -------------- | ---------------------- |
| 熔断降级       | 下游服务异常时快速失败 |
| 慢调用比例熔断 | 下游响应变慢时主动保护 |
| 异常比例熔断   | 下游错误率过高时隔离   |
| 热点接口限流   | 对高频接口做细粒度限流 |
| 服务级限流     | 按服务维度控制总流量   |

#### 3. 动态路由管理

当前路由可以通过本地 YAML 或 Nacos 配置中心管理。后续可以建设动态路由管理能力，将路由配置放入数据库、Nacos、Apollo、Consul 或运维平台中。

可扩展字段：

| 字段       | 说明                            |
| ---------- | ------------------------------- |
| 路由 ID    | 唯一路由标识                    |
| 路由名称   | 运维展示名称                    |
| 目标 URI   | `lb://service-name` 或固定地址  |
| 断言配置   | Path、Method、Header、Query 等  |
| 过滤器配置 | StripPrefix、限流、鉴权、灰度等 |
| 是否启用   | 控制路由上下线                  |
| 排序       | 控制路由匹配优先级              |
| 版本号     | 支持路由配置回滚                |

#### 4. 灰度发布和流量染色

Gateway 非常适合做入口级灰度。可以根据 Header、Cookie、用户 ID、租户 ID、App 版本号等条件，将部分流量转发到新版本服务。

常见灰度策略：

| 策略         | 示例                         |
| ------------ | ---------------------------- |
| Header 灰度  | `X-Version: v2` 转发到新服务 |
| 用户灰度     | 指定用户 ID 命中新版本       |
| 租户灰度     | 指定租户先升级               |
| 比例灰度     | 5% 流量进入新版本            |
| App 版本灰度 | 新版 App 使用新版接口        |

#### 5. 接入链路追踪和指标监控

Gateway 是请求入口，必须具备可观测能力。后续可以接入 Micrometer、Prometheus、Grafana、OpenTelemetry、SkyWalking、Zipkin 等工具。

建议监控指标：

| 指标             | 说明                  |
| ---------------- | --------------------- |
| 请求总量         | 网关整体流量          |
| 路由请求量       | 每个 routeId 的请求量 |
| 响应状态码       | 2xx、4xx、5xx 分布    |
| 请求耗时         | P50、P90、P95、P99    |
| 限流次数         | 429 次数              |
| 鉴权失败次数     | 401、403 次数         |
| 下游超时次数     | 504 次数              |
| 下游不可用次数   | 503 次数              |
| Gateway JVM 指标 | 内存、GC、线程、CPU   |

#### 6. 开放平台网关能力

如果系统需要对外开放 API，可以在当前 Gateway 基础上扩展开放平台能力。

可扩展能力：

| 能力              | 说明                     |
| ----------------- | ------------------------ |
| AppId / AppSecret | 第三方应用身份           |
| 请求签名          | 防篡改、防伪造           |
| 时间戳校验        | 防重放                   |
| Nonce 校验        | 防重复请求               |
| API 级限流        | 每个 App 单独限流        |
| API 权限          | 控制 App 可访问哪些接口  |
| 调用统计          | 统计调用量、失败率、耗时 |
| 账单计费          | 按接口调用量计费         |

#### 7. 多租户网关治理

多租户系统中，Gateway 可以从 Token、域名、Header 或路径中识别租户，并将租户信息透传给下游服务。

可扩展方式：

| 租户识别方式  | 示例                      |
| ------------- | ------------------------- |
| Token Payload | `tenantId`                |
| Header        | `X-Tenant-Id`             |
| 子域名        | `tenant-a.example.com`    |
| 路径前缀      | `/tenant-a/api/user/list` |
| API Key       | 通过 AppId 绑定租户       |

多租户网关注意事项：

| 注意项                     | 说明                                 |
| -------------------------- | ------------------------------------ |
| 租户 ID 不能直接信任客户端 | 必须由 Token 或可信凭证解析          |
| 租户限流独立               | 防止单个租户打爆整体服务             |
| 租户配置隔离               | 不同租户可配置不同白名单、限流、路由 |
| 日志包含租户 ID            | 便于审计和问题排查                   |

#### 8. 网关管理后台

当路由、限流、白名单、灰度规则逐渐增多后，可以建设一个网关管理后台，降低运维配置成本。

管理后台功能可以包括：

| 功能         | 说明                            |
| ------------ | ------------------------------- |
| 路由管理     | 新增、修改、启停路由            |
| 白名单管理   | 管理公开接口                    |
| 限流规则管理 | 按接口、用户、租户配置限流      |
| 灰度规则管理 | 管理灰度条件和目标服务          |
| 配置发布     | 支持审批、发布、回滚            |
| 路由查看     | 查看当前 Gateway 实例加载的路由 |
| 调用统计     | 展示请求量、错误率、耗时        |
| 操作审计     | 记录谁修改了网关配置            |

项目初期可以先用 Nacos 管理配置；当团队规模扩大、路由规则复杂、发布频繁时，再考虑建设独立网关管理平台。