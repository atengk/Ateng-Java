# Spring Boot Actuator

Spring Boot Actuator 是 Spring Boot 面向生产环境提供的监控与管理模块，用于在应用运行时暴露健康检查、指标、日志、环境信息、线程信息、Bean 信息等运维数据。官方文档将 Actuator 端点定义为用于监控和交互应用的入口，Spring Boot 内置了多个端点，同时也支持开发者自定义端点。默认情况下，Web 端点通常以 `/actuator/{id}` 的形式访问，例如健康检查端点为 `/actuator/health`。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html))

## Actuator 概述

本节用于说明 Actuator 在 Spring Boot 项目中的定位、典型使用场景，以及 Spring Boot 3 相比 Spring Boot 2 在运行环境、依赖体系和监控能力上的主要变化。

### 功能定位

Actuator 的核心定位是为 Spring Boot 应用提供生产级可观测性和运维管理能力。它不是业务功能模块，而是应用运行状态的外部观察窗口，主要用于回答以下问题：

| 问题                          | Actuator 对应能力             |
| ----------------------------- | ----------------------------- |
| 应用是否存活                  | `health` 健康检查端点         |
| 当前暴露了哪些接口            | `mappings` 映射信息端点       |
| 当前有哪些 Spring Bean        | `beans` Bean 信息端点         |
| JVM、HTTP、线程等指标是否正常 | `metrics` 指标端点            |
| 日志级别是否需要临时调整      | `loggers` 日志端点            |
| 配置属性是否加载正确          | `env`、`configprops` 配置端点 |
| 是否需要接入 Prometheus       | `prometheus` 指标抓取端点     |

Actuator 内置的常见端点包括 `health`、`info`、`metrics`、`loggers`、`mappings`、`beans`、`env`、`configprops`、`threaddump`、`heapdump`、`prometheus` 等。其中 `prometheus` 端点需要额外引入 `micrometer-registry-prometheus` 依赖后才会暴露 Prometheus 可抓取的指标格式。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html))

在实际项目中，Actuator 通常与以下组件配合使用：

| 组件            | 作用                                   |
| --------------- | -------------------------------------- |
| Spring Security | 控制 Actuator 端点访问权限             |
| Micrometer      | 采集应用指标                           |
| Prometheus      | 定时抓取指标数据                       |
| Grafana         | 可视化监控指标                         |
| Kubernetes      | 使用健康检查端点作为存活探针和就绪探针 |
| 日志平台        | 配合日志端点和日志文件定位运行问题     |

### 适用场景

Actuator 适用于需要对 Spring Boot 应用进行运行时观测、健康检查、问题排查和自动化运维的场景。它通常不是只在生产环境使用，在开发、测试、预发和生产环境中都可以启用，但不同环境应暴露不同端点。

常见适用场景如下：

| 场景         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 本地开发调试 | 查看 Bean、配置属性、接口映射、健康状态，快速定位自动配置是否生效 |
| 测试环境联调 | 检查数据库、Redis、消息队列等依赖组件是否可用                |
| 生产健康检查 | 通过 `/actuator/health` 判断应用是否正常运行                 |
| 容器化部署   | Kubernetes 可使用 `/actuator/health/liveness` 和 `/actuator/health/readiness` 作为探针 |
| 性能监控     | 通过 `metrics` 或 `prometheus` 查看 JVM、HTTP 请求、线程、内存、GC 等指标 |
| 线上问题排查 | 通过 `loggers` 动态调整日志级别，通过 `threaddump` 分析线程状态 |
| 监控平台集成 | 将 Micrometer 指标暴露给 Prometheus，再通过 Grafana 展示     |

需要注意的是，Actuator 端点可能包含敏感信息。Spring Boot 官方文档明确建议，公开暴露端点前应确认端点内容不包含敏感数据，或者通过防火墙、Spring Security 等方式进行保护。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html))

### Spring Boot 3 中的变化

Spring Boot 3 基于 Spring Framework 6，要求 Java 17 或更高版本，并迁移到 Jakarta EE 体系。也就是说，原来 Java EE 时代常见的 `javax.*` 包在相关规范中需要迁移到 `jakarta.*` 包，例如 Servlet、JPA 等依赖和导入路径都需要调整。([GitHub](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide))

与 Actuator 相关的主要变化包括：

| 变化项                   | 说明                                                         |
| ------------------------ | ------------------------------------------------------------ |
| Java 版本要求提升        | Spring Boot 3 要求 Java 17+，不再支持 Java 8                 |
| Jakarta EE 迁移          | 相关规范从 `javax.*` 迁移到 `jakarta.*`                      |
| JMX 暴露策略变化         | Spring Boot 3 中默认只有 `health` 端点通过 JMX 暴露，用于与 Web 端点默认暴露策略保持一致 |
| `httptrace` 更名         | 原 `httptrace` 端点更名为 `httpexchanges`，相关类也迁移到新的包路径 |
| Actuator JSON 序列化变化 | Actuator 端点响应默认使用隔离的 `ObjectMapper`，避免被应用全局 Jackson 配置影响 |
| 敏感值脱敏更严格         | `/env`、`/configprops` 等端点默认更严格脱敏，可通过 `show-values` 控制显示策略 |
| 指标体系升级             | Spring Boot 3 基于 Micrometer 1.10，引入 Observation 概念，将 Metrics 和 Tracing 统一到可观测性体系中 |

这些变化对新项目影响较小，但对从 Spring Boot 2.x 升级的项目影响较明显。尤其是自定义指标、HTTP Trace、Spring Security 配置、Actuator 暴露策略和配置属性迁移，需要在升级时重点检查。([GitHub](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide))

## 环境与依赖配置

本节用于说明在 Spring Boot 3 项目中启用 Actuator 所需的 Maven 依赖、基础配置项和 Web 访问路径。Actuator 推荐通过 `spring-boot-starter-actuator` 启用，该 Starter 会引入生产级监控和管理所需的基础能力。([Home](https://docs.spring.io/spring-boot/reference/actuator/enabling.html?utm_source=chatgpt.com))

### Maven 依赖配置

在 Maven 项目中，通常只需要引入 `spring-boot-starter-actuator` 即可启用 Actuator。如果项目需要通过 HTTP 访问端点，需要同时存在 Web 运行环境，例如 `spring-boot-starter-web` 或 `spring-boot-starter-webflux`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Web：提供 HTTP 服务能力，Actuator Web 端点通常依赖 Web 环境访问 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Actuator：提供健康检查、指标、日志、环境信息等生产级监控能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Spring Security：可选，用于保护 Actuator 端点，生产环境建议启用 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Prometheus 指标导出：可选，启用 /actuator/prometheus 端点 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

如果项目已经继承 `spring-boot-starter-parent`，通常不需要手动声明以上依赖版本，版本由 Spring Boot 依赖管理统一控制。生产环境是否引入 `spring-boot-starter-security` 取决于系统已有的安全体系；如果 Actuator 端点会暴露到内网网关、办公网络或公网入口，建议必须加访问控制。

### 基础配置项

Actuator 的核心配置集中在 `management.*` 前缀下。生产环境中不建议一次性暴露所有端点，应按需暴露 `health`、`info`、`metrics`、`prometheus` 等必要端点，避免暴露 `env`、`configprops`、`beans`、`heapdump` 等敏感端点。Spring Boot 默认只通过 HTTP 和 JMX 暴露 `health` 端点，其他端点需要通过 `management.endpoints.web.exposure.include` 显式配置。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 应用业务端口
  port: 8080

spring:
  application:
    # 应用名称，会影响部分监控指标和日志展示
    name: springboot3-actuator-demo

management:
  endpoints:
    web:
      exposure:
        # 按需暴露端点；生产环境不建议直接配置为 "*"
        include: health,info,metrics,prometheus,loggers
        # 明确排除敏感端点，避免误暴露环境变量、配置属性、堆转储等信息
        exclude: env,configprops,beans,heapdump
      discovery:
        # 是否启用 /actuator 端点发现页
        enabled: true

  endpoint:
    health:
      # never：不显示健康详情；when-authorized：认证后显示；always：始终显示
      show-details: when-authorized
      # 显示各组件健康状态，例如 db、redis、diskSpace
      show-components: when-authorized

    info:
      # 启用 info 端点
      enabled: true

    shutdown:
      # 关闭远程停机端点，生产环境不建议启用
      enabled: false

    env:
      # never：始终脱敏；when-authorized：授权后显示；always：始终显示
      show-values: never

    configprops:
      # 配置属性端点默认应保持脱敏
      show-values: never

  info:
    env:
      # 允许从 info.* 配置中读取应用信息并展示到 /actuator/info
      enabled: true

info:
  app:
    # 自定义应用信息，可通过 /actuator/info 查看
    name: ${spring.application.name}
    description: Spring Boot 3 Actuator 示例应用
    version: 1.0.0
```

常用基础配置说明如下：

| 配置项                                        | 作用                       | 建议                           |
| --------------------------------------------- | -------------------------- | ------------------------------ |
| `management.endpoints.web.exposure.include`   | 指定通过 HTTP 暴露的端点   | 生产环境按需配置               |
| `management.endpoints.web.exposure.exclude`   | 指定排除暴露的端点         | 用于兜底排除敏感端点           |
| `management.endpoint.health.show-details`     | 控制健康详情展示方式       | 生产环境建议 `when-authorized` |
| `management.endpoint.env.show-values`         | 控制环境变量值是否脱敏     | 生产环境建议 `never`           |
| `management.endpoint.configprops.show-values` | 控制配置属性值是否脱敏     | 生产环境建议 `never`           |
| `management.info.env.enabled`                 | 是否启用 `info.*` 信息展示 | 可开启                         |
| `management.endpoint.shutdown.enabled`        | 是否启用远程关闭应用端点   | 生产环境建议关闭               |

如果需要启用所有端点，可以使用 `include: "*"`，但 YAML 中 `*` 有特殊含义，必须加引号。官方文档也强调，公开暴露 Actuator 端点时应额外加安全控制。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html))

### Web 访问路径配置

Actuator Web 端点的默认访问格式为 `/actuator/{id}`，例如：

```text
GET http://localhost:8080/actuator
GET http://localhost:8080/actuator/health
GET http://localhost:8080/actuator/info
GET http://localhost:8080/actuator/metrics
GET http://localhost:8080/actuator/loggers
```

如果需要修改 Actuator 的基础路径，可以配置 `management.endpoints.web.base-path`。例如将默认 `/actuator` 改为 `/manage` 后，端点路径会从 `/actuator/{id}` 变为 `/manage/{id}`。([Home](https://docs.spring.io/spring-boot/3.4/api/rest/actuator/index.html))

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      # 修改 Actuator Web 端点基础路径
      base-path: /manage
      exposure:
        # 暴露常用运维端点
        include: health,info,metrics,prometheus
```

配置后访问路径如下：

```text
GET http://localhost:8080/manage
GET http://localhost:8080/manage/health
GET http://localhost:8080/manage/info
GET http://localhost:8080/manage/metrics
GET http://localhost:8080/manage/prometheus
```

如果希望 Actuator 使用独立端口，可以配置 `management.server.port`。这种方式适合生产环境将业务流量和运维流量隔离，例如业务端口走外部网关，管理端口只允许内网或监控系统访问。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 业务接口端口
  port: 8080

management:
  server:
    # Actuator 独立管理端口
    port: 9090

  endpoints:
    web:
      # Actuator 独立路径
      base-path: /actuator
      exposure:
        # 独立端口下仍然建议按需暴露
        include: health,info,metrics,prometheus
```

配置后，业务接口仍然访问 `8080` 端口，Actuator 端点访问 `9090` 端口：

```text
GET http://localhost:8080/api/users
GET http://localhost:9090/actuator/health
GET http://localhost:9090/actuator/info
GET http://localhost:9090/actuator/metrics
GET http://localhost:9090/actuator/prometheus
```

本地验证可以使用以下命令：

```bash
# 查看 Actuator 端点发现页
curl http://localhost:8080/actuator

# 查看应用健康状态
curl http://localhost:8080/actuator/health

# 使用独立管理端口访问健康状态
curl http://localhost:9090/actuator/health

# 查看应用基础信息
curl http://localhost:8080/actuator/info
```

以上命令中，`8080` 是业务服务端口，`9090` 是独立管理端口，`/actuator` 或 `/manage` 是 Actuator Web 端点基础路径。实际项目中应根据部署环境、网关规则、容器端口映射和安全策略统一规划。



## 端点管理

Actuator 端点是 Spring Boot 应用对外暴露运行状态、配置、指标和运维操作的入口。一个端点能否被访问，通常取决于两个条件：端点本身是否允许访问，以及端点是否通过 HTTP 或 JMX 暴露。Spring Boot 官方文档中也明确说明，端点只有在“访问被允许并且已经暴露”时才是可用状态。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

### 常用内置端点

Spring Boot Actuator 内置了多个端点，不同端点面向不同的运维场景。默认情况下，Web 端点通常以 `/actuator/{id}` 的形式访问，例如 `/actuator/health`、`/actuator/info`、`/actuator/metrics`。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

常用内置端点如下：

| 端点 ID          | 访问路径                   | 主要作用                                 | 生产环境建议           |
| ---------------- | -------------------------- | ---------------------------------------- | ---------------------- |
| `health`         | `/actuator/health`         | 查看应用健康状态                         | 建议暴露               |
| `info`           | `/actuator/info`           | 查看应用基础信息、构建信息、Git 信息     | 可暴露                 |
| `metrics`        | `/actuator/metrics`        | 查看应用指标名称和指标详情               | 建议内网暴露           |
| `prometheus`     | `/actuator/prometheus`     | 以 Prometheus 格式暴露指标               | 接入 Prometheus 时暴露 |
| `loggers`        | `/actuator/loggers`        | 查看和动态调整日志级别                   | 建议鉴权后暴露         |
| `mappings`       | `/actuator/mappings`       | 查看 Spring MVC/WebFlux 接口映射         | 仅开发或测试环境暴露   |
| `beans`          | `/actuator/beans`          | 查看 Spring 容器 Bean 信息               | 不建议生产暴露         |
| `env`            | `/actuator/env`            | 查看环境变量和配置属性                   | 不建议生产暴露         |
| `configprops`    | `/actuator/configprops`    | 查看 `@ConfigurationProperties` 配置绑定 | 不建议生产暴露         |
| `threaddump`     | `/actuator/threaddump`     | 查看线程快照                             | 问题排查时临时开放     |
| `heapdump`       | `/actuator/heapdump`       | 下载 JVM 堆转储文件                      | 高风险，不建议开放     |
| `scheduledtasks` | `/actuator/scheduledtasks` | 查看定时任务信息                         | 内网排查使用           |
| `caches`         | `/actuator/caches`         | 查看缓存信息                             | 按需开放               |
| `shutdown`       | `/actuator/shutdown`       | 远程关闭应用                             | 生产环境禁止开放       |

其中 `prometheus` 端点需要引入 `micrometer-registry-prometheus` 依赖后才会生效；`shutdown` 属于高风险操作端点，默认不应开放。Spring Boot 官方文档也提示，公开暴露 Actuator 端点前需要确认端点内容不包含敏感信息，或者使用防火墙、Spring Security 等方式进行保护。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

### 端点启用与禁用

端点启用与禁用用于控制端点是否可以被应用使用。需要区分两个概念：启用或禁用决定端点是否参与应用上下文；暴露配置决定端点是否可以通过 HTTP 或 JMX 访问。当前 Spring Boot 3 文档中，可以通过 `management.endpoint.<id>.access`、`management.endpoints.access.default` 和 `management.endpoints.access.max-permitted` 控制端点访问级别；不可访问的端点会从应用上下文中移除。([Home](https://docs.spring.io/spring-boot/3.4/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    access:
      # 默认只允许只读访问，适合大多数监控类端点
      default: read-only

  endpoint:
    health:
      # health 端点保持只读访问
      access: read-only

    info:
      # info 端点保持只读访问
      access: read-only

    loggers:
      # loggers 如果需要动态修改日志级别，需要允许读写访问
      access: unrestricted

    shutdown:
      # 禁止远程关闭应用
      access: none
```

如果希望所有端点默认不可访问，再逐个放开指定端点，可以使用白名单模式。

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    access:
      # 默认所有端点不可访问
      default: none

  endpoint:
    health:
      # 仅开放健康检查
      access: read-only

    info:
      # 仅开放应用信息
      access: read-only

    metrics:
      # 仅开放指标读取
      access: read-only

    prometheus:
      # 仅开放 Prometheus 指标抓取
      access: read-only
```

这种方式更适合生产环境。它的优点是默认收敛权限，后续新增 Actuator 端点时不会被自动开放。

### 端点暴露配置

端点暴露配置用于控制端点是否能通过某种技术方式访问，常见方式包括 HTTP 和 JMX。Spring Boot 出于安全考虑，默认只通过 HTTP 暴露 `health` 端点，其他端点需要通过 `management.endpoints.web.exposure.include` 显式配置。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 生产环境推荐按需暴露，不建议直接暴露 "*"
        include: health,info,metrics,prometheus
        # 明确排除高风险端点
        exclude: env,configprops,beans,heapdump,shutdown

  endpoint:
    health:
      # 健康详情仅授权用户可见
      show-details: when-authorized
      show-components: when-authorized
```

开发环境中，为了排查自动配置、Bean 注册、接口映射等问题，可以临时暴露更多端点。

文件位置：`src/main/resources/application-dev.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 开发环境可以暴露更多端点，便于排查问题
        include: "*"
        # 即使是开发环境，也建议排除 shutdown
        exclude: shutdown

  endpoint:
    health:
      # 开发环境直接展示健康详情
      show-details: always
      show-components: always

    env:
      # 开发环境如需查看配置值，可临时打开；生产环境不要使用 always
      show-values: always

    configprops:
      # 开发环境如需查看配置绑定值，可临时打开
      show-values: always
```

如果 YAML 中配置 `include: "*"`，必须加双引号。因为 `*` 在 YAML 中有特殊语义，不加引号可能导致配置解析异常。

### 端点访问方式

Actuator 端点最常见的访问方式是 HTTP。默认基础路径为 `/actuator`，完整路径格式为 `/actuator/{id}`。该基础路径可以通过 `management.endpoints.web.base-path` 修改，例如改为 `/manage` 后，`info` 端点路径会从 `/actuator/info` 变为 `/manage/info`。([Home](https://docs.spring.io/spring-boot/3.4/api/rest/actuator/index.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      # 修改 Actuator 统一访问前缀
      base-path: /actuator
      exposure:
        # 暴露常用运维端点
        include: health,info,metrics,prometheus,loggers
```

常用访问命令如下：

```bash
# 查看 Actuator 端点发现页
curl http://localhost:8080/actuator

# 查看健康状态
curl http://localhost:8080/actuator/health

# 查看应用信息
curl http://localhost:8080/actuator/info

# 查看所有指标名称
curl http://localhost:8080/actuator/metrics

# 查看 JVM 内存指标
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 查看日志级别
curl http://localhost:8080/actuator/loggers

# 查看指定包日志级别
curl http://localhost:8080/actuator/loggers/io.github.atengk
```

如果 `loggers` 端点允许写访问，可以通过 POST 请求动态调整日志级别。

```bash
# 动态调整指定包的日志级别为 DEBUG
curl -X POST "http://localhost:8080/actuator/loggers/io.github.atengk" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'

# 恢复默认日志级别
curl -X POST "http://localhost:8080/actuator/loggers/io.github.atengk" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":null}'
```

以上命令中，`configuredLevel` 表示显式配置的日志级别。设置为 `null` 表示移除当前显式配置，让日志级别回退到父级 Logger 或日志框架默认配置。

## 健康检查

健康检查用于判断应用当前是否处于可用状态。它通常被负载均衡、监控平台、容器编排平台调用，用来判断应用是否可以接收流量，或者是否需要重启。Spring Boot 的健康信息来自 `HealthContributorRegistry` 中的 `HealthContributor`，常见实现包括 `HealthIndicator` 和 `CompositeHealthContributor`。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

### Health 端点说明

`health` 是 Actuator 中最重要的端点之一，用于返回应用整体健康状态。默认访问路径如下：

```text
GET /actuator/health
```

典型返回结果如下：

```json
{
  "status": "UP"
}
```

当配置允许展示组件详情时，返回结果会包含数据库、Redis、磁盘空间等组件状态。

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoint:
    health:
      # never：不展示详情；when-authorized：授权后展示；always：始终展示
      show-details: when-authorized

      # 是否展示组件级健康状态
      show-components: when-authorized
```

常见健康状态如下：

| 状态             | 说明                 | 默认 HTTP 状态码 |
| ---------------- | -------------------- | ---------------- |
| `UP`             | 应用或组件正常       | `200`            |
| `DOWN`           | 应用或组件异常       | `503`            |
| `OUT_OF_SERVICE` | 应用不可对外提供服务 | `503`            |
| `UNKNOWN`        | 状态未知             | `200`            |

Spring Boot 默认将 `DOWN` 和 `OUT_OF_SERVICE` 映射为 HTTP `503`，`UP` 和 `UNKNOWN` 默认返回 HTTP `200`。如果自定义了新的健康状态，需要同时考虑状态排序和 HTTP 状态码映射。([Home](https://docs.spring.io/spring-boot/3.4/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoint:
    health:
      status:
        # 健康状态严重程度排序，越靠前优先级越高
        order: fatal,down,out-of-service,unknown,up

        http-mapping:
          # 保留默认异常状态映射
          down: 503
          out-of-service: 503
          # 自定义 fatal 状态也映射为 503
          fatal: 503
```

验证命令如下：

```bash
# 查看健康状态
curl http://localhost:8080/actuator/health

# 查看完整响应头和状态码
curl -i http://localhost:8080/actuator/health
```

### 自定义健康检查

自定义健康检查适合用于判断业务依赖是否可用，例如第三方接口、核心配置、业务开关、文件目录、许可证、内部服务等。实现方式是注册一个 `HealthIndicator` Bean，并实现 `health()` 方法。Spring Boot 会自动收集该 Bean，并将其合并到 `/actuator/health` 响应中。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

下面示例用于检查一个业务必需的外部服务地址是否已经配置。示例使用 Hutool 的 `StrUtil` 处理字符串判断。

文件位置：`src/main/java/io/github/atengk/actuator/health/BusinessServiceHealthIndicator.java`

```java
package io.github.atengk.actuator.health;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 业务服务健康检查
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class BusinessServiceHealthIndicator implements HealthIndicator {

    @Value("${business.service.url:}")
    private String businessServiceUrl;

    /**
     * 执行业务服务健康检查
     *
     * @return 健康检查结果
     */
    @Override
    public Health health() {
        if (StrUtil.isBlank(businessServiceUrl)) {
            log.warn("业务服务健康检查失败，business.service.url 未配置");
            return Health.down()
                    .withDetail("reason", "business.service.url 未配置")
                    .withDetail("service", "business-service")
                    .build();
        }

        log.debug("业务服务健康检查通过，服务地址：{}", businessServiceUrl);
        return Health.up()
                .withDetail("service", "business-service")
                .withDetail("url", businessServiceUrl)
                .build();
    }
}
```

配套配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
business:
  service:
    # 业务依赖服务地址，自定义健康检查会读取该配置
    url: https://api.example.com

management:
  endpoint:
    health:
      # 开发环境可设置为 always，生产环境建议 when-authorized
      show-details: always
      show-components: always
```

启动应用后访问：

```bash
# 查看整体健康状态
curl http://localhost:8080/actuator/health
```

可能返回如下结果：

```json
{
  "status": "UP",
  "components": {
    "businessService": {
      "status": "UP",
      "details": {
        "service": "business-service",
        "url": "https://api.example.com"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

需要注意，`BusinessServiceHealthIndicator` 在健康检查结果中的组件名称通常会去掉 `HealthIndicator` 后缀，因此显示为 `businessService`。官方文档也说明，某个 `HealthIndicator` 的标识符通常来自 Bean 名称，并会去掉 `HealthIndicator` 后缀。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

如果需要关闭某个健康检查组件，可以使用如下配置：

文件位置：`src/main/resources/application.yml`

```yaml
management:
  health:
    # 关闭自定义 businessService 健康检查
    businessService:
      enabled: false

    # 关闭默认磁盘空间健康检查
    diskspace:
      enabled: false
```

### 健康状态分组

健康状态分组用于将不同的健康检查组件组织成不同的访问入口。典型场景是区分“应用是否存活”和“应用是否可以接收业务流量”。Spring Boot 支持通过 `management.endpoint.health.group.<name>` 创建健康分组，并通过 `/actuator/health/{group}` 访问。([Home](https://docs.spring.io/spring-boot/3.4/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoint:
    health:
      show-details: when-authorized
      show-components: when-authorized

      group:
        # 存活检查：只检查应用自身是否还活着，不依赖数据库、Redis、外部接口
        liveness:
          include: ping
          show-details: never

        # 就绪检查：检查应用是否可以接收业务流量
        readiness:
          include: ping,diskSpace,businessService
          show-details: when-authorized

        # 自定义数据库分组：用于单独检查数据库相关状态
        database:
          include: db
          show-details: when-authorized
```

访问方式如下：

```bash
# 应用整体健康状态
curl http://localhost:8080/actuator/health

# 存活检查
curl http://localhost:8080/actuator/health/liveness

# 就绪检查
curl http://localhost:8080/actuator/health/readiness

# 数据库健康检查
curl http://localhost:8080/actuator/health/database
```

在 Kubernetes 场景中，`liveness` 不建议依赖数据库、Redis、外部 HTTP 服务等外部系统。因为如果外部系统故障导致 `liveness` 失败，Kubernetes 可能会不断重启应用实例，进一步放大故障范围。Spring Boot 官方文档也强调，liveness 探针不应依赖外部系统；外部系统更适合放到 readiness 检查中。([Home](https://docs.spring.io/spring-boot/3.4/reference/actuator/endpoints.html?utm_source=chatgpt.com))

Kubernetes 探针示例：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: springboot3-actuator-demo
spec:
  template:
    spec:
      containers:
        - name: springboot3-actuator-demo
          image: springboot3-actuator-demo:1.0.0
          ports:
            - containerPort: 8080
          livenessProbe:
            # 存活探针：只判断应用是否需要重启
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            # 就绪探针：判断应用是否可以接收流量
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 10
```

## 应用信息管理

应用信息管理主要通过 `info` 端点实现，用于展示应用名称、版本、构建时间、Git 分支、提交 ID、Java 运行时、操作系统等信息。`info` 端点的数据来源是 Spring 容器中的 `InfoContributor`，Spring Boot 内置了 `build`、`env`、`git`、`java`、`os`、`process`、`ssl` 等贡献器。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

### Info 端点配置

`info` 端点默认路径为：

```text
GET /actuator/info
```

如果需要通过 `application.yml` 中的 `info.*` 配置展示应用信息，需要启用 `management.info.env.enabled`。

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露 info 端点
        include: health,info

  info:
    env:
      # 启用从 info.* 配置读取应用信息
      enabled: true

    java:
      # 启用 Java 运行时信息
      enabled: true

    os:
      # 启用操作系统信息
      enabled: true

    process:
      # 启用进程信息
      enabled: true

info:
  app:
    # 应用名称
    name: ${spring.application.name}

    # 应用描述
    description: Spring Boot 3 Actuator 示例应用

    # 应用版本
    version: 1.0.0

    # 维护团队
    owner: ateng-team
```

访问验证：

```bash
curl http://localhost:8080/actuator/info
```

返回示例：

```json
{
  "app": {
    "name": "springboot3-actuator-demo",
    "description": "Spring Boot 3 Actuator 示例应用",
    "version": "1.0.0",
    "owner": "ateng-team"
  },
  "java": {
    "version": "17.0.12"
  },
  "os": {
    "name": "Linux",
    "version": "5.15.0"
  }
}
```

Spring Boot 官方文档说明，`env`、`java`、`os`、`process` 等 `InfoContributor` 是否启用由 `management.info.<id>.enabled` 控制；`build` 和 `git` 在满足资源文件存在的前提下通常会自动贡献信息。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

### 构建信息展示

构建信息用于在运行时确认当前应用的构建版本、构建时间、构件名称等信息。Spring Boot 会在 classpath 下存在 `META-INF/build-info.properties` 时，通过 `BuildInfoContributor` 将构建信息暴露到 `/actuator/info`。该文件通常由 Spring Boot Maven 插件生成。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<build>
    <plugins>
        <!-- Spring Boot Maven 插件：用于打包应用，并生成 build-info.properties -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <!-- 生成 META-INF/build-info.properties -->
                        <goal>build-info</goal>
                    </goals>
                    <configuration>
                        <additionalProperties>
                            <!-- 自定义构建环境 -->
                            <build.environment>dev</build.environment>
                            <!-- 自定义构建负责人 -->
                            <build.owner>ateng</build.owner>
                        </additionalProperties>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

构建项目：

```bash
# 清理并打包项目，同时生成 build-info.properties
mvn clean package
```

验证构建产物中是否已经生成构建信息文件：

```bash
# 查看 jar 包中的 build-info.properties
jar tf target/*.jar | grep "META-INF/build-info.properties"
```

启动应用后访问：

```bash
curl http://localhost:8080/actuator/info
```

返回示例：

```json
{
  "build": {
    "artifact": "springboot3-actuator-demo",
    "name": "springboot3-actuator-demo",
    "time": "2026-05-06T09:30:00Z",
    "version": "1.0.0",
    "group": "io.github.atengk",
    "environment": "dev",
    "owner": "ateng"
  }
}
```

如果 `/actuator/info` 中没有 `build` 节点，通常检查以下事项：

| 检查项                                                     | 说明                                       |
| ---------------------------------------------------------- | ------------------------------------------ |
| 是否执行了 `mvn clean package`                             | `build-info.properties` 通常在构建阶段生成 |
| 是否配置了 `spring-boot-maven-plugin` 的 `build-info` goal | 未配置时不会生成构建信息                   |
| jar 中是否存在 `META-INF/build-info.properties`            | 不存在则 Actuator 无法读取构建信息         |
| 是否关闭了 `management.info.build.enabled`                 | 关闭后不会显示构建信息                     |

### Git 信息展示

Git 信息用于在运行时确认当前应用来自哪个分支、哪个提交、什么时间构建。Spring Boot 会在 classpath 根路径存在 `git.properties` 时自动配置 `GitProperties`，并通过 `GitInfoContributor` 暴露到 `/actuator/info`。默认情况下，`info` 端点通常展示 `git.branch`、`git.commit.id` 和 `git.commit.time` 等关键信息；如果需要展示完整 Git 信息，可以配置 `management.info.git.mode=full`。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<build>
    <plugins>
        <!-- Git Commit Id 插件：用于生成 git.properties，供 Actuator info 端点读取 -->
        <plugin>
            <groupId>io.github.git-commit-id</groupId>
            <artifactId>git-commit-id-maven-plugin</artifactId>
            <version>9.0.1</version>
            <executions>
                <execution>
                    <goals>
                        <!-- 生成 git.properties -->
                        <goal>revision</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <!-- 生成文件路径，Spring Boot 会从 classpath 根路径读取 git.properties -->
                <generateGitPropertiesFile>true</generateGitPropertiesFile>
                <generateGitPropertiesFilename>${project.build.outputDirectory}/git.properties</generateGitPropertiesFilename>

                <!-- 日期格式 -->
                <dateFormat>yyyy-MM-dd'T'HH:mm:ssZ</dateFormat>

                <!-- 只保留常用字段，避免暴露过多仓库信息 -->
                <includeOnlyProperties>
                    <includeOnlyProperty>^git.branch$</includeOnlyProperty>
                    <includeOnlyProperty>^git.commit.id.abbrev$</includeOnlyProperty>
                    <includeOnlyProperty>^git.commit.id.full$</includeOnlyProperty>
                    <includeOnlyProperty>^git.commit.time$</includeOnlyProperty>
                    <includeOnlyProperty>^git.build.time$</includeOnlyProperty>
                    <includeOnlyProperty>^git.build.version$</includeOnlyProperty>
                </includeOnlyProperties>
            </configuration>
        </plugin>
    </plugins>
</build>
```

配套 Actuator 配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
management:
  info:
    git:
      # simple：展示关键信息；full：展示 git.properties 中的完整信息
      mode: simple

      # 是否启用 Git 信息展示
      enabled: true
```

构建并验证：

```bash
# 打包项目，生成 git.properties
mvn clean package

# 检查 jar 包中是否存在 git.properties
jar tf target/*.jar | grep "git.properties"

# 启动后查看 info 端点
curl http://localhost:8080/actuator/info
```

返回示例：

```json
{
  "git": {
    "branch": "main",
    "commit": {
      "id": "8f3a21c",
      "time": "2026-05-06T09:20:00Z"
    }
  }
}
```

如果需要展示完整 Git 信息，可以改为：

文件位置：`src/main/resources/application.yml`

```yaml
management:
  info:
    git:
      # 展示 git.properties 中的完整 Git 信息
      mode: full
```

生产环境中不建议无差别展示完整 Git 信息，尤其是包含构建用户、构建主机、远程仓库地址等字段时。更稳妥的做法是通过 Maven 插件的 `includeOnlyProperties` 控制生成字段，只保留分支、提交 ID、提交时间和构建版本等必要信息。



## 指标监控

指标监控用于观察应用运行过程中的关键数值，例如 JVM 内存、GC、线程、HTTP 请求耗时、数据库连接池、缓存、业务调用次数等。Spring Boot Actuator 基于 Micrometer 自动配置指标采集能力，Micrometer 本身是一个指标门面，支持 Prometheus、JMX、OTLP、Datadog、Influx、StatsD 等多种监控系统。Spring Boot 会自动配置 Spring 管理的 `MeterRegistry`，项目中可以通过注入 `MeterRegistry` 注册自定义业务指标。([docs.spring.io](https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com))

### Metrics 端点说明

`metrics` 端点用于查看当前应用已经注册的指标名称，以及指定指标的当前测量值、单位、统计类型和可用标签。该端点主要用于诊断应用当前采集了哪些指标，不建议把 `/actuator/metrics` 当作生产环境指标采集后端；生产监控通常使用 Prometheus、OTLP 或其他指标系统。([docs.spring.io](https://docs.spring.io/spring-boot/api/rest/actuator/metrics.html?utm_source=chatgpt.com))

默认访问路径如下：

```text
GET /actuator/metrics
GET /actuator/metrics/{metric.name}
```

启用配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露指标端点和 Prometheus 抓取端点
        include: health,info,metrics,prometheus

  endpoint:
    metrics:
      # 允许读取 metrics 端点
      access: read-only

  metrics:
    tags:
      # 全局公共标签，便于在监控系统中按环境、应用、区域过滤
      application: ${spring.application.name}
      env: dev
      region: local
```

常用访问命令如下：

```bash
# 查看当前应用已注册的所有指标名称
curl http://localhost:8080/actuator/metrics

# 查看 JVM 已使用内存
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 按标签过滤非堆内存指标
curl "http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:nonheap"

# 查看 HTTP 服务端请求指标
curl http://localhost:8080/actuator/metrics/http.server.requests

# 查看 Prometheus 格式指标
curl http://localhost:8080/actuator/prometheus
```

返回示例：

```json
{
  "name": "jvm.memory.used",
  "description": "The amount of used memory",
  "baseUnit": "bytes",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 123456789
    }
  ],
  "availableTags": [
    {
      "tag": "area",
      "values": [
        "heap",
        "nonheap"
      ]
    },
    {
      "tag": "id",
      "values": [
        "G1 Eden Space",
        "G1 Old Gen",
        "Metaspace"
      ]
    }
  ]
}
```

需要注意，`/actuator/metrics/{metric.name}` 中使用的指标名称应使用代码中的原始名称，而不是 Prometheus 等监控系统转换后的名称。例如 Prometheus 中可能显示为 `jvm_memory_used`，但通过 Actuator `metrics` 端点查询时仍然应使用 `jvm.memory.used`。([docs.spring.io](https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com))

### JVM 指标

JVM 指标用于观察 Java 进程内部运行状态，常用于排查内存泄漏、GC 频繁、线程堆积、类加载异常等问题。Spring Boot 会通过 Micrometer 自动注册 JVM 相关指标，指标名称通常以 `jvm.` 开头。官方文档说明，Spring Boot 自动提供内存、缓冲池、GC、线程、类加载、JVM 版本和 JIT 编译时间等 JVM 指标。([docs.spring.io](https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com))

常用 JVM 指标如下：

| 指标名称                 | 说明              | 常见用途                      |
| ------------------------ | ----------------- | ----------------------------- |
| `jvm.memory.used`        | JVM 已使用内存    | 判断堆内存和非堆内存占用      |
| `jvm.memory.max`         | JVM 最大可用内存  | 判断内存上限                  |
| `jvm.memory.committed`   | JVM 已提交内存    | 观察 JVM 向操作系统申请的内存 |
| `jvm.gc.pause`           | GC 暂停耗时       | 排查 GC 停顿                  |
| `jvm.threads.live`       | 当前存活线程数    | 排查线程数量异常              |
| `jvm.threads.daemon`     | 守护线程数        | 观察后台线程                  |
| `jvm.threads.peak`       | 峰值线程数        | 判断历史线程峰值              |
| `jvm.classes.loaded`     | 当前已加载类数量  | 排查类加载异常                |
| `jvm.buffer.memory.used` | Buffer 已使用内存 | 观察 Direct Buffer 等内存占用 |

查询示例：

```bash
# 查看 JVM 堆内存使用情况
curl "http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap"

# 查看 JVM 非堆内存使用情况
curl "http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:nonheap"

# 查看 GC 暂停指标
curl http://localhost:8080/actuator/metrics/jvm.gc.pause

# 查看当前存活线程数
curl http://localhost:8080/actuator/metrics/jvm.threads.live

# 查看当前已加载类数量
curl http://localhost:8080/actuator/metrics/jvm.classes.loaded
```

在生产环境中，JVM 指标通常不直接由人工频繁访问，而是由 Prometheus 等系统定时抓取 `/actuator/prometheus`，再通过 Grafana 配置 JVM Dashboard 进行展示。

### HTTP 请求指标

HTTP 请求指标用于观察接口调用次数、请求耗时、响应状态码、异常情况和 URI 维度分布。Spring MVC、Spring WebFlux 和 Jersey 请求都会被 Spring Boot 自动接入观测体系，其中 Spring MVC 和 WebFlux 默认生成的服务端请求指标名称为 `http.server.requests`，也可以通过 `management.observations.http.server.requests.name` 修改名称。([docs.spring.io](https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com))

常见 HTTP 指标标签如下：

| 标签        | 说明          | 示例                                      |
| ----------- | ------------- | ----------------------------------------- |
| `method`    | HTTP 请求方法 | `GET`、`POST`                             |
| `uri`       | URI 模板      | `/api/users/{id}`                         |
| `status`    | HTTP 状态码   | `200`、`404`、`500`                       |
| `outcome`   | 响应结果分类  | `SUCCESS`、`CLIENT_ERROR`、`SERVER_ERROR` |
| `exception` | 异常类型      | `None`、`IllegalArgumentException`        |

查询示例：

```bash
# 查看所有 HTTP 请求指标
curl http://localhost:8080/actuator/metrics/http.server.requests

# 查看 GET 请求指标
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=method:GET"

# 查看 500 响应指标
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=status:500"

# 查看指定接口模板的指标
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/api/orders/mock"
```

如果需要修改 HTTP 服务端请求指标名称，可以配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
management:
  observations:
    http:
      server:
        requests:
          # 自定义 HTTP 服务端请求指标名称
          name: app.http.server.requests
```

配置后，查询路径也需要同步调整：

```bash
curl http://localhost:8080/actuator/metrics/app.http.server.requests
```

接口路径标签应尽量使用 URI 模板，例如 `/api/orders/{id}`，避免把订单号、用户 ID、手机号等高基数字段作为标签值。高基数标签会造成监控系统存储压力过大，也会影响查询性能。

### 自定义业务指标

自定义业务指标用于记录业务层面的关键数据，例如订单创建次数、支付成功次数、任务队列积压数量、外部接口调用耗时等。Spring Boot 官方文档建议通过注入 Spring 管理的 `MeterRegistry` 注册自定义指标；如果指标依赖其他 Bean，也可以使用 `MeterBinder` 统一注册。([docs.spring.io](https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com))

下面示例包含三类常见业务指标：

| 指标类型 | Micrometer 类型 | 示例指标                          |
| -------- | --------------- | --------------------------------- |
| 累加计数 | `Counter`       | `business.order.created.total`    |
| 耗时统计 | `Timer`         | `business.order.process.duration` |
| 当前值   | `Gauge`         | `business.order.pending`          |

示例代码使用 Hutool 生成业务编号、校验字符串，并使用日志记录关键业务节点。需要补充 Hutool 和 Lombok 依赖。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Hutool：提供常用工具类，例如字符串、ID、集合等工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.38</version>
    </dependency>

    <!-- Lombok：简化日志对象、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

文件位置：`src/main/java/io/github/atengk/actuator/metrics/OrderMetricsService.java`

```java
package io.github.atengk.actuator.metrics;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单业务指标服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class OrderMetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter orderCreatedCounter;
    private final Counter orderFailedCounter;
    private final Timer orderProcessTimer;
    private final AtomicInteger pendingOrderGauge = new AtomicInteger(0);

    public OrderMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.orderCreatedCounter = Counter.builder("business.order.created.total")
                .description("订单创建总次数")
                .tag("module", "order")
                .register(meterRegistry);
        this.orderFailedCounter = Counter.builder("business.order.failed.total")
                .description("订单创建失败总次数")
                .tag("module", "order")
                .register(meterRegistry);
        this.orderProcessTimer = Timer.builder("business.order.process.duration")
                .description("订单创建处理耗时")
                .tag("module", "order")
                .publishPercentileHistogram()
                .register(meterRegistry);

        Gauge.builder("business.order.pending", pendingOrderGauge, AtomicInteger::get)
                .description("当前处理中订单数量")
                .tag("module", "order")
                .register(meterRegistry);
    }

    /**
     * 模拟创建订单并记录业务指标
     *
     * @param userId 用户ID
     * @param amount 订单金额
     * @return 订单创建结果
     */
    public Dict createOrder(String userId, BigDecimal amount) {
        Timer.Sample sample = Timer.start(meterRegistry);
        pendingOrderGauge.incrementAndGet();

        try {
            if (StrUtil.isBlank(userId)) {
                throw new IllegalArgumentException("用户ID不能为空");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("订单金额必须大于0");
            }

            String orderNo = IdUtil.fastSimpleUUID();
            orderCreatedCounter.increment();

            log.info("订单创建成功，订单号：{}，用户ID：{}，金额：{}", orderNo, userId, amount);
            return Dict.create()
                    .set("orderNo", orderNo)
                    .set("userId", userId)
                    .set("amount", amount)
                    .set("status", "CREATED");
        } catch (RuntimeException e) {
            orderFailedCounter.increment();
            log.warn("订单创建失败，用户ID：{}，原因：{}", userId, e.getMessage());
            throw e;
        } finally {
            sample.stop(orderProcessTimer);
            pendingOrderGauge.decrementAndGet();
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/actuator/metrics/OrderMockController.java`

```java
package io.github.atengk.actuator.metrics;

import cn.hutool.core.lang.Dict;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 订单指标模拟接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
public class OrderMockController {

    private final OrderMetricsService orderMetricsService;

    /**
     * 模拟创建订单
     *
     * @param userId 用户ID
     * @param amount 订单金额
     * @return 订单创建结果
     */
    @GetMapping("/api/orders/mock")
    public Dict mockCreateOrder(@RequestParam String userId, @RequestParam BigDecimal amount) {
        return orderMetricsService.createOrder(userId, amount);
    }
}
```

启动应用后，先调用业务接口产生指标数据：

```bash
# 模拟成功订单
curl "http://localhost:8080/api/orders/mock?userId=10001&amount=99.90"

# 模拟失败订单
curl "http://localhost:8080/api/orders/mock?userId=&amount=99.90"
```

再查看自定义指标：

```bash
# 查看订单创建总次数
curl http://localhost:8080/actuator/metrics/business.order.created.total

# 查看订单创建失败总次数
curl http://localhost:8080/actuator/metrics/business.order.failed.total

# 查看订单处理耗时
curl http://localhost:8080/actuator/metrics/business.order.process.duration

# 查看当前处理中订单数量
curl http://localhost:8080/actuator/metrics/business.order.pending
```

生产环境中设计业务指标时应避免把用户 ID、订单号、手机号、请求流水号等高基数字段作为标签。标签更适合使用低基数字段，例如 `module=order`、`type=create`、`channel=web`、`result=success`。

## 日志管理

日志管理用于在应用运行时查看和调整日志级别。Actuator 的 `loggers` 端点可以查看所有 Logger，也可以查看单个 Logger 或 Logger Group 的配置，并支持在运行时动态设置日志级别。官方文档说明，`loggers` 端点可查看显式配置的日志级别和日志框架计算出的有效日志级别，支持的级别包括 `TRACE`、`DEBUG`、`INFO`、`WARN`、`ERROR`、`FATAL`、`OFF` 和 `null`。([docs.spring.io](https://docs.spring.io/spring-boot/reference/actuator/loggers.html?utm_source=chatgpt.com))

### Loggers 端点说明

`loggers` 端点默认访问路径如下：

```text
GET /actuator/loggers
GET /actuator/loggers/{logger.name}
POST /actuator/loggers/{logger.name}
```

启用配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露 loggers 端点
        include: health,info,metrics,loggers

  endpoint:
    loggers:
      # 允许读取和修改日志级别
      access: unrestricted
```

常用查询命令如下：

```bash
# 查看所有日志级别
curl http://localhost:8080/actuator/loggers

# 查看根 Logger
curl http://localhost:8080/actuator/loggers/ROOT

# 查看指定业务包 Logger
curl http://localhost:8080/actuator/loggers/io.github.atengk
```

单个 Logger 返回示例：

```json
{
  "configuredLevel": "INFO",
  "effectiveLevel": "INFO"
}
```

其中 `configuredLevel` 表示显式配置的日志级别，`effectiveLevel` 表示日志框架最终生效的日志级别。当 `configuredLevel` 为 `null` 时，表示当前 Logger 没有单独配置日志级别，会继承父级 Logger 的配置。([docs.spring.io](https://docs.spring.io/spring-boot/reference/actuator/loggers.html?utm_source=chatgpt.com))

### 动态调整日志级别

动态调整日志级别适合用于生产问题排查。例如某个业务模块出现异常，但默认 `INFO` 日志不足以定位问题，可以临时将指定包调整为 `DEBUG`，排查完成后再恢复默认级别。官方文档说明，设置日志级别时可以向对应 Logger URI 发送 `POST` 请求，并在 JSON 中传入 `configuredLevel`；如果需要清除显式级别，可以传入 `null` 或空对象。([docs.spring.io](https://docs.spring.io/spring-boot/reference/actuator/loggers.html?utm_source=chatgpt.com))

将业务包日志级别调整为 `DEBUG`：

```bash
curl -X POST "http://localhost:8080/actuator/loggers/io.github.atengk" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'
```

查看是否生效：

```bash
curl http://localhost:8080/actuator/loggers/io.github.atengk
```

返回示例：

```json
{
  "configuredLevel": "DEBUG",
  "effectiveLevel": "DEBUG"
}
```

恢复默认日志级别：

```bash
# 方式一：configuredLevel 设置为 null
curl -X POST "http://localhost:8080/actuator/loggers/io.github.atengk" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":null}'

# 方式二：提交空对象，清除显式日志级别
curl -X POST "http://localhost:8080/actuator/loggers/io.github.atengk" \
  -H "Content-Type: application/json" \
  -d '{}'
```

如果需要按日志组统一调整，可以在配置中定义 `logging.group.*`。

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  group:
    # 自定义业务日志组
    business: io.github.atengk

    # 自定义框架日志组
    framework: org.springframework,org.hibernate

  level:
    # 默认业务日志级别
    business: INFO
    framework: WARN
```

动态调整日志组：

```bash
# 将业务日志组调整为 DEBUG
curl -X POST "http://localhost:8080/actuator/loggers/business" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'

# 恢复业务日志组默认级别
curl -X POST "http://localhost:8080/actuator/loggers/business" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### 日志配置建议

日志配置应同时考虑可排查性、性能和安全性。生产环境中建议默认使用 `INFO` 级别，框架日志使用 `WARN` 级别，只有在问题排查时临时开启指定包的 `DEBUG` 或 `TRACE`。

推荐配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  level:
    # 根日志级别
    root: INFO

    # 项目业务包日志级别
    io.github.atengk: INFO

    # Spring 框架日志级别
    org.springframework: WARN

    # Hibernate 日志级别
    org.hibernate: WARN

  file:
    # 输出日志文件，配合 /actuator/logfile 端点可查看日志内容
    name: logs/springboot3-actuator-demo.log

  logback:
    rollingpolicy:
      # 单个日志文件最大大小
      max-file-size: 100MB

      # 日志保留天数
      max-history: 30

      # 日志总大小上限
      total-size-cap: 10GB
```

生产环境建议遵守以下规则：

| 建议               | 说明                                    |
| ------------------ | --------------------------------------- |
| 默认不使用 `DEBUG` | 避免日志量过大影响性能和磁盘            |
| 不记录敏感字段     | 避免输出密码、Token、身份证号、银行卡号 |
| 动态日志及时恢复   | 线上临时开启 `DEBUG` 后应及时恢复       |
| 日志按模块分组     | 使用 `logging.group.*` 管理业务模块日志 |
| 保留滚动策略       | 防止日志文件无限增长                    |
| 日志端点加权限控制 | `loggers` 和 `logfile` 都不应公网裸露   |

如果需要查看日志文件内容，可以暴露 `logfile` 端点，但它要求配置 `logging.file.name` 或 `logging.file.path`。Spring Boot 文档说明，`logfile` 端点在配置日志文件后可以返回日志文件内容，并支持 HTTP `Range` 头读取部分内容。([docs.spring.io](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

## 自定义 Actuator 端点

自定义 Actuator 端点用于将项目内部的运维能力封装为标准管理接口，例如查看业务缓存状态、刷新本地规则、查看内部开关、触发轻量级维护操作等。Spring Boot 支持通过 `@Endpoint` 定义技术无关端点，并使用 `@ReadOperation`、`@WriteOperation`、`@DeleteOperation` 暴露读、写、删操作。官方文档说明，添加带有 `@Endpoint` 的 Bean 后，标注了这些操作注解的方法会自动通过 JMX 暴露，在 Web 应用中也会通过 HTTP 暴露。([docs.spring.io](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

### 自定义端点设计

自定义端点设计时应优先面向运维场景，而不是替代业务接口。端点返回内容应简洁、稳定、低敏感，写操作应谨慎开放，并配合 Spring Security、内网访问控制或管理端口隔离使用。

推荐设计原则如下：

| 原则             | 说明                                                  |
| ---------------- | ----------------------------------------------------- |
| 端点 ID 简短明确 | 例如 `bizcache`、`bizswitch`、`runtime`               |
| 读写分离         | 查询使用 `@ReadOperation`，变更使用 `@WriteOperation` |
| 避免敏感数据     | 不返回密钥、Token、密码、完整配置                     |
| 写操作必须鉴权   | 刷新缓存、修改开关等操作不应匿名访问                  |
| 返回结构固定     | 便于脚本、监控系统或运维平台解析                      |
| 避免重业务逻辑   | 不在 Actuator 端点中执行复杂业务交易                  |

下面设计一个 `bizcache` 自定义端点，用于查看和修改本地业务缓存开关状态。

| 操作             | 注解                         | HTTP 方法 | 访问路径                         |
| ---------------- | ---------------------------- | --------- | -------------------------------- |
| 查看整体缓存状态 | `@ReadOperation`             | `GET`     | `/actuator/bizcache`             |
| 查看指定缓存状态 | `@ReadOperation + @Selector` | `GET`     | `/actuator/bizcache/{cacheName}` |
| 修改缓存状态     | `@WriteOperation`            | `POST`    | `/actuator/bizcache`             |

端点暴露配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露自定义 bizcache 端点
        include: health,info,metrics,loggers,bizcache

  endpoint:
    bizcache:
      # 自定义端点允许读写，生产环境必须配合权限控制
      access: unrestricted
```

在 Web 环境中，Actuator 会根据操作类型自动生成 HTTP 谓词：`@ReadOperation` 对应 `GET`，`@WriteOperation` 对应 `POST`，`@DeleteOperation` 对应 `DELETE`；如果方法参数标注 `@Selector`，该参数会成为路径变量。([docs.spring.io](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

### 读取型端点实现

读取型端点用于返回运行时状态，不修改应用内部数据。下面示例提供两个读取操作：一个查看所有缓存状态，一个按缓存名称查看单个缓存状态。

文件位置：`src/main/java/io/github/atengk/actuator/endpoint/BizCacheEndpoint.java`

```java
package io.github.atengk.actuator.endpoint;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务缓存 Actuator 端点
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@Endpoint(id = "bizcache")
public class BizCacheEndpoint {

    private final Map<String, Boolean> cacheStatusMap = new ConcurrentHashMap<>();
    private volatile LocalDateTime lastRefreshTime = LocalDateTime.now();

    public BizCacheEndpoint() {
        cacheStatusMap.put("userCache", true);
        cacheStatusMap.put("orderCache", true);
        cacheStatusMap.put("productCache", true);
    }

    /**
     * 查看全部业务缓存状态
     *
     * @return 缓存状态
     */
    @ReadOperation
    public Dict all() {
        return Dict.create()
                .set("enabled", true)
                .set("lastRefreshTime", lastRefreshTime)
                .set("caches", cacheStatusMap);
    }

    /**
     * 查看指定业务缓存状态
     *
     * @param cacheName 缓存名称
     * @return 缓存状态
     */
    @ReadOperation
    public Dict one(@Selector String cacheName) {
        Boolean enabled = cacheStatusMap.get(cacheName);
        if (enabled == null) {
            return Dict.create()
                    .set("cacheName", cacheName)
                    .set("exists", false)
                    .set("message", "缓存不存在");
        }

        return Dict.create()
                .set("cacheName", cacheName)
                .set("exists", true)
                .set("enabled", enabled)
                .set("lastRefreshTime", lastRefreshTime);
    }

    /**
     * 修改业务缓存状态
     *
     * @param cacheName 缓存名称
     * @param enabled 是否启用
     * @param reason 操作原因
     * @return 修改结果
     */
    @WriteOperation
    public Dict update(@Nullable String cacheName, @Nullable Boolean enabled, @Nullable String reason) {
        if (StrUtil.isBlank(cacheName)) {
            return Dict.create()
                    .set("success", false)
                    .set("message", "cacheName 不能为空");
        }
        if (enabled == null) {
            return Dict.create()
                    .set("success", false)
                    .set("message", "enabled 不能为空");
        }

        cacheStatusMap.put(cacheName, enabled);
        lastRefreshTime = LocalDateTime.now();

        log.info("业务缓存状态已修改，缓存名称：{}，启用状态：{}，原因：{}", cacheName, enabled, StrUtil.blankToDefault(reason, "未填写"));
        return Dict.create()
                .set("success", true)
                .set("cacheName", cacheName)
                .set("enabled", enabled)
                .set("reason", StrUtil.blankToDefault(reason, "未填写"))
                .set("lastRefreshTime", lastRefreshTime);
    }
}
```

读取操作验证如下：

```bash
# 查看全部业务缓存状态
curl http://localhost:8080/actuator/bizcache

# 查看指定业务缓存状态
curl http://localhost:8080/actuator/bizcache/userCache

# 查看不存在的缓存
curl http://localhost:8080/actuator/bizcache/notExistsCache
```

返回示例：

```json
{
  "enabled": true,
  "lastRefreshTime": "2026-05-06T10:30:00",
  "caches": {
    "userCache": true,
    "orderCache": true,
    "productCache": true
  }
}
```

### 写入型端点实现

写入型端点用于执行运行时变更操作，例如刷新缓存、调整开关、触发轻量级维护动作。Actuator 写操作通过 `@WriteOperation` 暴露为 HTTP `POST` 请求，请求体中的 JSON 根属性会映射到方法参数。官方文档说明，Web 暴露时，端点方法参数可以从 URL 查询参数和 JSON 请求体中获取；参数默认必填，如需可选参数可使用 `@Nullable` 标注。([docs.spring.io](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

上面的 `BizCacheEndpoint#update` 已经提供写操作，调用方式如下：

```bash
# 禁用 userCache
curl -X POST "http://localhost:8080/actuator/bizcache" \
  -H "Content-Type: application/json" \
  -d '{
    "cacheName": "userCache",
    "enabled": false,
    "reason": "排查用户缓存命中异常"
  }'

# 启用 userCache
curl -X POST "http://localhost:8080/actuator/bizcache" \
  -H "Content-Type: application/json" \
  -d '{
    "cacheName": "userCache",
    "enabled": true,
    "reason": "问题排查完成，恢复缓存"
  }'
```

返回示例：

```json
{
  "success": true,
  "cacheName": "userCache",
  "enabled": false,
  "reason": "排查用户缓存命中异常",
  "lastRefreshTime": "2026-05-06T10:35:00"
}
```

再次查询验证：

```bash
curl http://localhost:8080/actuator/bizcache/userCache
```

返回示例：

```json
{
  "cacheName": "userCache",
  "exists": true,
  "enabled": false,
  "lastRefreshTime": "2026-05-06T10:35:00"
}
```

写入型端点必须谨慎开放。生产环境中建议至少满足以下条件：端点只暴露在管理端口或内网；接入 Spring Security；只允许运维或管理员角色访问；操作写入审计日志；请求参数做严格校验；禁止在端点中执行大批量数据变更、删除数据、清空核心缓存等高风险操作。

## 安全控制

Actuator 端点可能暴露应用运行时的内部信息，例如环境变量、配置属性、Bean 信息、日志级别、线程栈、堆转储文件等。Spring Boot 官方文档明确说明，Actuator 端点只有在访问被允许并且已经暴露时才可用；同时，公开暴露端点前应确认端点不包含敏感信息，或者通过防火墙、Spring Security 等方式保护。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

### Actuator 安全风险

Actuator 的安全风险主要来自“暴露范围过大”和“权限控制不足”。在生产环境中，不应简单使用 `management.endpoints.web.exposure.include: "*"` 暴露全部端点，更不应将管理端口直接暴露到公网。

常见风险如下：

| 风险类型       | 典型端点                 | 风险说明                                      | 建议                      |
| -------------- | ------------------------ | --------------------------------------------- | ------------------------- |
| 应用结构泄露   | `beans`、`mappings`      | 暴露 Bean、Controller、路由、过滤器等内部结构 | 生产环境不暴露            |
| 配置信息泄露   | `env`、`configprops`     | 可能暴露配置项、环境变量、连接信息            | 不暴露或强制脱敏          |
| 运行时信息泄露 | `threaddump`、`heapdump` | 线程栈和堆文件可能包含业务数据                | 仅排障时临时开放          |
| 操作型风险     | `loggers`、`shutdown`    | 可能修改日志级别或关闭应用                    | 必须鉴权，`shutdown` 禁用 |
| 监控数据滥用   | `metrics`、`prometheus`  | 可能暴露接口路径、异常比例、资源状态          | 仅内网或监控系统访问      |
| 健康详情泄露   | `health`                 | 可能暴露数据库、Redis、MQ 等依赖状态          | 对匿名用户只显示整体状态  |

生产环境推荐采用以下策略：

| 策略         | 说明                                                        |
| ------------ | ----------------------------------------------------------- |
| 最小暴露     | 只暴露 `health`、`info`、`metrics`、`prometheus` 等必要端点 |
| 独立管理端口 | 使用 `management.server.port` 将运维流量和业务流量隔离      |
| 网络隔离     | 管理端口只允许内网、Prometheus、运维平台访问                |
| 强制鉴权     | 除健康检查外，其余端点都应要求认证                          |
| 权限分级     | 监控用户只读，运维管理员才可访问 `loggers` 等操作端点       |
| 敏感脱敏     | `env`、`configprops` 保持 `show-values: never`              |
| 禁用高危端点 | `shutdown`、`heapdump` 默认不暴露                           |

推荐生产配置如下：

文件位置：`src/main/resources/application-prod.yml`

```yaml
server:
  # 业务服务端口
  port: 8080

management:
  server:
    # 独立 Actuator 管理端口，仅允许内网访问
    port: 9090

  endpoints:
    web:
      # Actuator 统一访问前缀
      base-path: /actuator
      exposure:
        # 生产环境按需暴露端点
        include: health,info,metrics,prometheus,loggers
        # 明确排除高风险端点
        exclude: env,configprops,beans,heapdump,shutdown

  endpoint:
    health:
      # 未授权用户只看整体状态，授权用户可看详情
      show-details: when-authorized
      show-components: when-authorized
      roles: ACTUATOR_ADMIN,MONITOR

    env:
      # 环境变量值始终脱敏
      show-values: never

    configprops:
      # 配置属性值始终脱敏
      show-values: never

    shutdown:
      # 禁止远程关闭应用
      access: none

  endpoints:
    access:
      # 默认只允许读取端点
      default: read-only

  endpoint:
    loggers:
      # 日志端点允许读写，但必须由 Spring Security 控制权限
      access: unrestricted
```

如果当前版本使用的是较早的 Spring Boot 3.x 配置模型，部分项目仍可能使用 `management.endpoint.<id>.enabled` 控制端点启用状态。新文档中更推荐使用 `management.endpoint.<id>.access` 和 `management.endpoints.access.default` 控制端点访问级别。不可访问的端点会从应用上下文中移除；如果只是控制 HTTP 或 JMX 暴露方式，应使用 `include` 和 `exclude`。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

### Spring Security 集成

当项目引入 Spring Security 且没有自定义 `SecurityFilterChain` 时，Spring Boot 会自动保护除 `/health` 以外的 Actuator 端点。如果项目定义了自己的 `SecurityFilterChain`，Spring Boot 的 Actuator 安全自动配置会退让，端点访问规则需要由开发者显式配置。Spring Boot 提供了 `EndpointRequest`，用于基于 Actuator 端点创建 `RequestMatcher`。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

依赖配置如下：

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Security：用于保护 Actuator 端点和业务接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Spring Boot Actuator：提供监控和管理端点 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

下面配置提供两个用户：`monitor` 只能查看监控类端点，`actuator_admin` 可以访问日志调整等运维端点。实际生产环境建议对接公司统一认证系统，例如 OAuth2、LDAP、OIDC、网关鉴权或内部权限平台，不建议长期使用内存用户。

文件位置：`src/main/java/io/github/atengk/actuator/config/ActuatorSecurityConfig.java`

```java
package io.github.atengk.actuator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Actuator 安全配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class ActuatorSecurityConfig {

    /**
     * 配置 Actuator 端点安全过滤链
     *
     * @param http HTTP 安全配置
     * @return 安全过滤链
     * @throws Exception 安全配置异常
     */
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("初始化 Actuator 安全过滤链");

        return http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(authorize -> authorize
                        // 健康检查和基础信息允许匿名访问，便于负载均衡和探针检查
                        .requestMatchers(EndpointRequest.to("health", "info")).permitAll()

                        // Prometheus 端点只允许监控角色访问
                        .requestMatchers(EndpointRequest.to("prometheus", "metrics")).hasAnyRole("MONITOR", "ACTUATOR_ADMIN")

                        // 日志、线程、堆等运维端点只允许管理员访问
                        .requestMatchers(EndpointRequest.to("loggers", "threaddump", "heapdump")).hasRole("ACTUATOR_ADMIN")

                        // 其他 Actuator 端点默认要求管理员权限
                        .anyRequest().hasRole("ACTUATOR_ADMIN")
                )
                // 使用 HTTP Basic 便于 Prometheus、脚本、运维平台集成
                .httpBasic(Customizer.withDefaults())
                // Actuator 写操作一般由脚本调用，忽略 CSRF；业务接口不要照搬该策略
                .csrf(csrf -> csrf.ignoringRequestMatchers(EndpointRequest.toAnyEndpoint()))
                .build();
    }

    /**
     * 配置业务接口安全过滤链
     *
     * @param http HTTP 安全配置
     * @return 安全过滤链
     * @throws Exception 安全配置异常
     */
    @Bean
    @Order(2)
    public SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        // 示例中放行业务接口，实际项目应替换为真实业务权限规则
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .build();
    }

    /**
     * 配置 Actuator 示例用户
     *
     * @param passwordEncoder 密码编码器
     * @return 用户详情服务
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();

        manager.createUser(User.withUsername("monitor")
                .password(passwordEncoder.encode("monitor123"))
                .roles("MONITOR")
                .build());

        manager.createUser(User.withUsername("actuator_admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ACTUATOR_ADMIN")
                .build());

        return manager;
    }

    /**
     * 配置密码编码器
     *
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

验证命令如下：

```bash
# 匿名访问健康检查
curl http://localhost:9090/actuator/health

# 监控用户访问 Prometheus 端点
curl -u monitor:monitor123 http://localhost:9090/actuator/prometheus

# 监控用户访问 loggers 端点，预期无权限
curl -i -u monitor:monitor123 http://localhost:9090/actuator/loggers

# 管理员访问 loggers 端点
curl -u actuator_admin:admin123 http://localhost:9090/actuator/loggers
```

这里使用 `@Order(1)` 将 Actuator 安全过滤链放在业务过滤链之前，确保 Actuator 路径优先匹配。如果项目已经存在统一安全配置，需要把 Actuator 规则合并进现有安全体系，避免定义自定义 `SecurityFilterChain` 后误放行业务接口。

### 端点权限控制

端点权限控制建议按照“匿名、监控、运维管理员”三类访问者划分。匿名用户只能访问最小健康状态；监控系统可以抓取指标；运维管理员可以查看日志、线程、必要的排障端点。

推荐权限矩阵如下：

| 端点          | 匿名     | 监控用户 | 运维管理员 |
| ------------- | -------- | -------- | ---------- |
| `health`      | 允许     | 允许     | 允许       |
| `info`        | 可选允许 | 允许     | 允许       |
| `metrics`     | 禁止     | 允许     | 允许       |
| `prometheus`  | 禁止     | 允许     | 允许       |
| `loggers`     | 禁止     | 禁止     | 允许       |
| `threaddump`  | 禁止     | 禁止     | 临时允许   |
| `heapdump`    | 禁止     | 禁止     | 默认禁止   |
| `env`         | 禁止     | 禁止     | 默认禁止   |
| `configprops` | 禁止     | 禁止     | 默认禁止   |
| `shutdown`    | 禁止     | 禁止     | 禁止       |

配套配置如下：

文件位置：`src/main/resources/application-prod.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 暴露范围只控制可通过 Web 访问的端点，不等同于权限控制
        include: health,info,metrics,prometheus,loggers,threaddump
        exclude: env,configprops,beans,heapdump,shutdown

  endpoint:
    health:
      # 匿名用户看到 UP/DOWN，认证用户按角色查看详情
      show-details: when-authorized
      show-components: when-authorized
      roles: MONITOR,ACTUATOR_ADMIN

    loggers:
      # 允许动态调整日志级别，但必须由 Spring Security 控制角色
      access: unrestricted

    shutdown:
      # 生产环境禁止关闭应用
      access: none
```

如果 Prometheus 需要 Basic 认证抓取 `/actuator/prometheus`，需要在 Prometheus 配置中提供认证信息。也可以在网关层、Service Mesh 或内网白名单层面进行访问控制。

## 运维集成

Actuator 的核心价值在于与外部运维系统集成。常见组合是 Spring Boot Actuator 暴露指标，Prometheus 定时抓取，Grafana 负责展示和告警，Kubernetes 使用健康端点判断容器存活和就绪状态。Spring Boot 提供 `/actuator/prometheus` 端点用于 Prometheus 抓取，但该端点默认不可用，需要引入 Prometheus Registry 并显式暴露。([Home](https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com))

### Prometheus 监控集成

Prometheus 通过定时拉取应用实例的指标数据完成采集。Spring Boot 在引入 Prometheus Registry 后，可以通过 `/actuator/prometheus` 输出 Prometheus 可识别的指标格式。Spring Boot 文档说明，Prometheus 端点默认不可用，必须显式暴露。([Home](https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com))

Maven 依赖如下：

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Actuator：提供管理端点 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Prometheus Registry：启用 /actuator/prometheus 指标输出 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

Spring Boot 配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    name: springboot3-actuator-demo

management:
  endpoints:
    web:
      exposure:
        # 暴露 Prometheus 抓取端点
        include: health,info,metrics,prometheus

  metrics:
    tags:
      # 公共标签，便于 Prometheus 和 Grafana 按应用、环境聚合
      application: ${spring.application.name}
      env: dev
      region: local
```

验证 Prometheus 端点：

```bash
# 查看 Prometheus 格式指标
curl http://localhost:8080/actuator/prometheus

# 查看包含应用标签的指标
curl http://localhost:8080/actuator/prometheus | grep "application"
```

Prometheus 抓取配置如下。Prometheus 的 `scrape_config` 用于定义抓取目标、抓取间隔、超时时间、指标路径等参数；`metrics_path` 表示目标应用暴露指标的 HTTP 路径。([Prometheus](https://prometheus.io/docs/prometheus/latest/configuration/configuration/?utm_source=chatgpt.com))

文件位置：`prometheus/prometheus.yml`

```yaml
global:
  # 全局抓取间隔
  scrape_interval: 15s
  # 全局抓取超时时间
  scrape_timeout: 10s

scrape_configs:
  - job_name: "springboot3-actuator-demo"
    # Spring Boot Actuator Prometheus 端点路径
    metrics_path: "/actuator/prometheus"
    # 抓取间隔
    scrape_interval: 15s
    static_configs:
      # 目标地址，Docker Compose 内可使用服务名，宿主机可使用 IP:PORT
      - targets: ["host.docker.internal:8080"]
        labels:
          application: "springboot3-actuator-demo"
          env: "dev"
```

如果 Actuator 端点启用了 Basic 认证，Prometheus 配置需要增加认证信息：

文件位置：`prometheus/prometheus.yml`

```yaml
scrape_configs:
  - job_name: "springboot3-actuator-demo"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets: ["host.docker.internal:9090"]
    basic_auth:
      # Prometheus 监控用户
      username: "monitor"
      password: "monitor123"
```

本地 Docker Compose 示例：

文件位置：`docker-compose.yml`

```yaml
services:
  prometheus:
    image: prom/prometheus:v3.3.0
    container_name: prometheus
    restart: unless-stopped
    ports:
      # Prometheus Web 控制台
      - "9090:9090"
    volumes:
      # 挂载 Prometheus 配置文件
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      # 指定配置文件路径
      - "--config.file=/etc/prometheus/prometheus.yml"

  grafana:
    image: grafana/grafana:12.0.0
    container_name: grafana
    restart: unless-stopped
    ports:
      # Grafana Web 控制台
      - "3000:3000"
    environment:
      # Grafana 管理员账号
      - GF_SECURITY_ADMIN_USER=admin
      # Grafana 管理员密码
      - GF_SECURITY_ADMIN_PASSWORD=admin
    depends_on:
      - prometheus
```

启动命令：

```bash
# 启动 Prometheus 和 Grafana
docker compose up -d

# 查看容器状态
docker compose ps

# 查看 Prometheus 日志
docker logs -f prometheus
```

访问地址：

```text
Prometheus 控制台：http://localhost:9090
Grafana 控制台：http://localhost:3000
Spring Boot 指标：http://localhost:8080/actuator/prometheus
```

在 Prometheus 页面中进入 `Status -> Targets`，确认 `springboot3-actuator-demo` 状态为 `UP`。如果状态为 `DOWN`，优先检查应用端口、`metrics_path`、Basic 认证、Docker 网络和 Actuator 暴露配置。

### Grafana 可视化

Grafana 用于连接 Prometheus 数据源，并将 JVM、HTTP、线程、GC、业务指标等数据转换为图表和告警。Grafana 官方文档说明，Grafana 内置支持 Prometheus 数据源，不需要额外安装插件；配置数据源时需要提供 Prometheus Server URL。([Grafana Labs](https://grafana.com/docs/grafana/latest/datasources/prometheus/?utm_source=chatgpt.com))

Grafana 添加 Prometheus 数据源的基本步骤如下：

| 步骤 | 操作                                                 |
| ---- | ---------------------------------------------------- |
| 1    | 登录 Grafana，默认本地地址为 `http://localhost:3000` |
| 2    | 进入 `Connections -> Data sources`                   |
| 3    | 选择 `Prometheus`                                    |
| 4    | 配置 Prometheus Server URL                           |
| 5    | 点击 `Save & test` 验证连接                          |
| 6    | 新建 Dashboard 或导入 JVM/Spring Boot 模板           |

如果使用上面的 Docker Compose，Grafana 与 Prometheus 在同一个 Compose 网络中，数据源地址应配置为：

```text
http://prometheus:9090
```

不要在 Grafana 容器中配置 `http://localhost:9090`。Grafana 文档也说明，当 Grafana 和 Prometheus 运行在不同容器中时，`localhost` 指的是 Grafana 容器自身，而不是 Prometheus 容器或宿主机。([Grafana Labs](https://grafana.com/docs/grafana/latest/datasources/prometheus/configure/?src=grafana_plugin_list&utm_source=chatgpt.com))

也可以使用 Grafana Provisioning 自动配置数据源。

文件位置：`grafana/provisioning/datasources/prometheus.yml`

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    # 数据源类型
    type: prometheus
    # 设置为默认数据源
    isDefault: true
    # Grafana 后端代理访问 Prometheus
    access: proxy
    # Docker Compose 服务名
    url: http://prometheus:9090
    jsonData:
      # Prometheus 查询建议使用 POST，适合较长 PromQL
      httpMethod: POST
      # 与 Prometheus 抓取间隔保持一致
      timeInterval: 15s
```

Docker Compose 中挂载 Grafana 配置：

文件位置：`docker-compose.yml`

```yaml
services:
  prometheus:
    image: prom/prometheus:v3.3.0
    container_name: prometheus
    restart: unless-stopped
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - "--config.file=/etc/prometheus/prometheus.yml"

  grafana:
    image: grafana/grafana:12.0.0
    container_name: grafana
    restart: unless-stopped
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      # 自动加载 Prometheus 数据源
      - ./grafana/provisioning:/etc/grafana/provisioning
    depends_on:
      - prometheus
```

常用 PromQL 示例：

```promql
# HTTP 请求总量
sum(rate(http_server_requests_seconds_count[1m]))

# 按 URI 统计 HTTP 请求耗时 P95
histogram_quantile(
  0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
)

# JVM 堆内存使用量
sum(jvm_memory_used_bytes{area="heap"})

# JVM 堆内存使用率
sum(jvm_memory_used_bytes{area="heap"}) / sum(jvm_memory_max_bytes{area="heap"})

# GC 暂停耗时 P95
histogram_quantile(
  0.95,
  sum(rate(jvm_gc_pause_seconds_bucket[5m])) by (le)
)

# 业务订单创建速率
sum(rate(business_order_created_total[1m]))
```

推荐看板分组如下：

| 面板分组 | 关键指标                                   |
| -------- | ------------------------------------------ |
| 应用概览 | 实例状态、请求量、错误率、P95 响应时间     |
| JVM 内存 | 堆内存、非堆内存、Metaspace、Direct Buffer |
| GC       | GC 次数、GC 暂停耗时、不同 GC 类型耗时     |
| 线程     | 存活线程、峰值线程、守护线程               |
| HTTP     | 按 URI、状态码、方法统计 QPS 和耗时        |
| 业务指标 | 订单数、支付数、失败数、队列积压量         |

### Kubernetes 探针集成

Kubernetes 探针用于判断容器是否存活、是否启动完成、是否可以接收流量。Spring Boot 在 Kubernetes 环境中可以基于 `ApplicationAvailability` 自动提供 liveness 和 readiness 健康分组，对应路径为 `/actuator/health/liveness` 和 `/actuator/health/readiness`。这些探针组也可以通过 `management.endpoint.health.probes.enabled=true` 在非 Kubernetes 环境中显式启用。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

Spring Boot 配置如下：

文件位置：`src/main/resources/application-k8s.yml`

```yaml
management:
  endpoint:
    health:
      probes:
        # 在任意环境中启用 liveness/readiness 探针端点
        enabled: true

      group:
        readiness:
          # 就绪检查可按需加入外部依赖，但要谨慎
          include: readinessState,db,redis
          show-details: never

        liveness:
          # 存活检查不应依赖数据库、Redis、外部接口
          include: livenessState
          show-details: never

  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

Kubernetes Deployment 示例：

文件位置：`k8s/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: springboot3-actuator-demo
  labels:
    app: springboot3-actuator-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: springboot3-actuator-demo
  template:
    metadata:
      labels:
        app: springboot3-actuator-demo
    spec:
      containers:
        - name: springboot3-actuator-demo
          image: springboot3-actuator-demo:1.0.0
          ports:
            - name: http
              containerPort: 8080
            - name: management
              containerPort: 9090
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "k8s"

          startupProbe:
            # 启动探针：应用启动较慢时，避免被过早杀死
            httpGet:
              path: /actuator/health/readiness
              port: management
            failureThreshold: 30
            periodSeconds: 10

          livenessProbe:
            # 存活探针：判断应用是否需要重启
            httpGet:
              path: /actuator/health/liveness
              port: management
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3

          readinessProbe:
            # 就绪探针：判断应用是否可以接收流量
            httpGet:
              path: /actuator/health/readiness
              port: management
            initialDelaySeconds: 10
            periodSeconds: 10
            failureThreshold: 3
```

Spring Boot 文档特别强调，liveness 探针不应依赖外部系统，例如数据库、外部 API、外部缓存。因为外部系统故障时，如果 liveness 失败，Kubernetes 会不断重启所有应用实例，可能造成级联故障；外部依赖更适合放入 readiness，或者由业务熔断、降级机制处理。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

如果使用独立管理端口，探针只检查管理端口可能无法完全反映业务端口是否可用。Spring Boot 支持通过 `management.endpoint.health.probes.add-additional-paths=true` 将 `/livez` 和 `/readyz` 额外暴露到主服务端口。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-k8s.yml`

```yaml
management:
  endpoint:
    health:
      probes:
        # 将 /livez 和 /readyz 额外暴露到主服务端口
        add-additional-paths: true
```

此时也可以将探针改为主业务端口：

```yaml
livenessProbe:
  httpGet:
    path: /livez
    port: http

readinessProbe:
  httpGet:
    path: /readyz
    port: http
```

## 开发与验证

开发与验证用于确认 Actuator 的依赖、配置、端点暴露、安全控制、Prometheus 抓取和 Kubernetes 探针是否符合预期。建议在本地开发、测试环境、预发环境分别验证，不要等到生产发布后再排查端点不可访问、权限错误或指标缺失问题。

### 本地访问验证

本地验证首先确认应用是否正常启动，其次确认 Actuator 端点是否暴露，最后确认安全策略是否符合预期。默认情况下，Web 端点使用 `/actuator/{id}` 形式访问，例如 `/actuator/health`。([Home](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html?utm_source=chatgpt.com))

基础启动命令：

```bash
# 使用 dev 环境启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或者打包后启动
mvn clean package
java -jar target/*.jar --spring.profiles.active=dev
```

本地端点验证：

```bash
# 查看端点发现页
curl http://localhost:8080/actuator

# 查看健康检查
curl http://localhost:8080/actuator/health

# 查看应用信息
curl http://localhost:8080/actuator/info

# 查看指标列表
curl http://localhost:8080/actuator/metrics

# 查看 JVM 内存指标
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 查看 Prometheus 指标
curl http://localhost:8080/actuator/prometheus
```

如果使用独立管理端口：

```bash
# 业务接口端口
curl http://localhost:8080/api/orders/mock?userId=10001\&amount=99.90

# Actuator 管理端口
curl http://localhost:9090/actuator/health
curl http://localhost:9090/actuator/info
curl http://localhost:9090/actuator/prometheus
```

安全验证：

```bash
# 匿名访问 health，预期 200
curl -i http://localhost:9090/actuator/health

# 匿名访问 metrics，预期 401 或 403
curl -i http://localhost:9090/actuator/metrics

# 监控用户访问 prometheus，预期 200
curl -i -u monitor:monitor123 http://localhost:9090/actuator/prometheus

# 监控用户访问 loggers，预期 403
curl -i -u monitor:monitor123 http://localhost:9090/actuator/loggers

# 管理员访问 loggers，预期 200
curl -i -u actuator_admin:admin123 http://localhost:9090/actuator/loggers
```

### 接口测试方式

接口测试可以使用 curl、Postman、Apifox、HTTP Client 或自动化测试脚本完成。测试时建议覆盖“端点存在性、权限控制、响应结构、指标产生、动态日志修改、健康分组”等场景。

推荐测试清单如下：

| 测试项     | 请求                                    | 预期                                   |
| ---------- | --------------------------------------- | -------------------------------------- |
| 健康检查   | `GET /actuator/health`                  | 返回 `UP` 或明确失败原因               |
| 应用信息   | `GET /actuator/info`                    | 返回应用版本、构建、Git 信息           |
| 指标列表   | `GET /actuator/metrics`                 | 返回指标名称列表                       |
| 指标详情   | `GET /actuator/metrics/jvm.memory.used` | 返回 measurements 和 availableTags     |
| Prometheus | `GET /actuator/prometheus`              | 返回 Prometheus 文本格式指标           |
| 日志查询   | `GET /actuator/loggers/{name}`          | 返回 configuredLevel 和 effectiveLevel |
| 日志修改   | `POST /actuator/loggers/{name}`         | 修改后再次查询生效                     |
| 存活探针   | `GET /actuator/health/liveness`         | 返回 `UP`                              |
| 就绪探针   | `GET /actuator/health/readiness`        | 返回 `UP` 或 `OUT_OF_SERVICE`          |
| 权限验证   | 匿名访问敏感端点                        | 返回 `401` 或 `403`                    |

HTTP Client 示例：

文件位置：`src/test/http/actuator.http`

```http
### 查看健康状态
GET http://localhost:9090/actuator/health

### 查看应用信息
GET http://localhost:9090/actuator/info

### 查看指标列表
GET http://localhost:9090/actuator/metrics
Authorization: Basic monitor monitor123

### 查看 Prometheus 指标
GET http://localhost:9090/actuator/prometheus
Authorization: Basic monitor monitor123

### 查看业务包日志级别
GET http://localhost:9090/actuator/loggers/io.github.atengk
Authorization: Basic actuator_admin admin123

### 调整业务包日志级别为 DEBUG
POST http://localhost:9090/actuator/loggers/io.github.atengk
Authorization: Basic actuator_admin admin123
Content-Type: application/json

{
  "configuredLevel": "DEBUG"
}

### 恢复业务包日志级别
POST http://localhost:9090/actuator/loggers/io.github.atengk
Authorization: Basic actuator_admin admin123
Content-Type: application/json

{
  "configuredLevel": null
}
```

Spring Boot 集成测试示例：

文件位置：`src/test/java/io/github/atengk/actuator/ActuatorEndpointTest.java`

```java
package io.github.atengk.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Actuator 端点测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ActuatorEndpointTest {

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private TestRestTemplate testRestTemplate;

    /**
     * 验证健康检查端点可访问
     */
    @Test
    void shouldAccessHealthEndpoint() {
        ResponseEntity<String> response = testRestTemplate.getForEntity(
                "http://localhost:" + managementPort + "/actuator/health",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    /**
     * 验证未认证用户不能访问指标端点
     */
    @Test
    void shouldRejectMetricsEndpointWithoutAuthentication() {
        ResponseEntity<String> response = testRestTemplate.getForEntity(
                "http://localhost:" + managementPort + "/actuator/metrics",
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    /**
     * 验证监控用户可以访问 Prometheus 端点
     */
    @Test
    void shouldAccessPrometheusEndpointWithMonitorUser() {
        ResponseEntity<String> response = testRestTemplate
                .withBasicAuth("monitor", "monitor123")
                .getForEntity(
                        "http://localhost:" + managementPort + "/actuator/prometheus",
                        String.class
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jvm");
    }
}
```

测试依赖如下：

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot 测试：包含 JUnit 5、AssertJ、MockMvc 等测试能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 常见问题排查

Actuator 常见问题通常集中在端点不可访问、认证失败、Prometheus 抓取失败、健康检查详情不显示、Kubernetes 探针异常等方面。排查时应先区分“端点是否启用”“端点是否暴露”“端点是否被安全规则拦截”“端点路径是否正确”。

常见问题和处理方式如下：

| 问题                            | 可能原因                                                     | 处理方式                                                  |
| ------------------------------- | ------------------------------------------------------------ | --------------------------------------------------------- |
| `/actuator` 返回 404            | 未引入 Actuator 依赖或路径被修改                             | 检查 `spring-boot-starter-actuator` 和 `base-path`        |
| `/actuator/metrics` 返回 404    | 端点未暴露                                                   | 配置 `management.endpoints.web.exposure.include`          |
| `/actuator/prometheus` 返回 404 | 缺少 Prometheus Registry 或未暴露端点                        | 引入 `micrometer-registry-prometheus` 并暴露 `prometheus` |
| 返回 401                        | 未认证                                                       | 增加 Basic Auth、Token 或登录态                           |
| 返回 403                        | 已认证但角色不足                                             | 检查 Spring Security 角色和 `EndpointRequest` 规则        |
| `health` 无详情                 | `show-details` 为 `never`                                    | 调整为 `when-authorized` 或 `always`                      |
| `info` 为空                     | 未启用 `management.info.env.enabled` 或未生成 build/git 信息 | 检查 `info.*`、`build-info`、`git.properties`             |
| Prometheus Target DOWN          | 地址、端口、路径、认证或网络不通                             | 检查 `targets`、`metrics_path`、容器网络                  |
| Grafana 无数据                  | 数据源地址错误或 PromQL 不匹配                               | 检查 Prometheus 数据源和指标名称                          |
| Kubernetes 反复重启             | liveness 配置不合理或启动时间过长                            | 增加 `startupProbe`，避免 liveness 检查外部依赖           |
| `loggers` POST 失败             | CSRF、权限、端点访问级别问题                                 | 对 Actuator 忽略 CSRF，检查 `access` 和角色               |

排查命令如下：

```bash
# 检查 Actuator 依赖是否存在
mvn dependency:tree | grep "spring-boot-starter-actuator"

# 检查 Prometheus Registry 是否存在
mvn dependency:tree | grep "micrometer-registry-prometheus"

# 查看端点发现页
curl -i http://localhost:8080/actuator

# 查看管理端口健康检查
curl -i http://localhost:9090/actuator/health

# 检查 Prometheus 指标是否输出
curl -i http://localhost:9090/actuator/prometheus

# 查看 Prometheus Target 状态接口
curl http://localhost:9090/api/v1/targets

# 查看 Kubernetes Pod 事件
kubectl describe pod <pod-name> -n <namespace>

# 查看 Kubernetes 容器日志
kubectl logs -f <pod-name> -n <namespace>
```

端点不可访问时，建议按照以下顺序排查：

```text
1. 是否引入 spring-boot-starter-actuator
2. 是否配置了正确的 management.server.port
3. 是否修改了 management.endpoints.web.base-path
4. 是否通过 management.endpoints.web.exposure.include 暴露端点
5. 是否通过 management.endpoint.<id>.access 禁用了端点
6. 是否被 Spring Security 拦截
7. 是否被网关、Nginx、防火墙、Kubernetes NetworkPolicy 拦截
8. 是否访问了错误端口、错误路径或错误容器网络地址
```

生产环境最终建议如下：

| 类别     | 建议                                                         |
| -------- | ------------------------------------------------------------ |
| 暴露端点 | 只开放 `health`、`info`、`metrics`、`prometheus`，按需开放 `loggers` |
| 安全控制 | 除 `health` 外全部鉴权，写操作只允许管理员                   |
| 网络边界 | Actuator 管理端口只允许内网或监控系统访问                    |
| 指标采集 | 使用 Prometheus 抓取 `/actuator/prometheus`                  |
| 可视化   | 使用 Grafana 连接 Prometheus                                 |
| 探针设计 | liveness 不依赖外部系统，readiness 谨慎加入外部依赖          |
| 故障排查 | 临时开放排障端点，排查完成后恢复最小暴露策略                 |