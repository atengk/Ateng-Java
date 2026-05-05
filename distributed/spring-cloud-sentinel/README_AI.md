# Spring Cloud Alibaba Sentinel

Spring Cloud Alibaba Sentinel 用于在 Spring Boot 3 微服务项目中接入 Sentinel 的流量治理能力，重点解决高并发访问、下游服务不稳定、接口响应变慢、突发流量冲击、热点参数访问集中等问题。它不是业务功能组件，而是服务稳定性保护组件，通常与 Nacos、OpenFeign、Spring Cloud Gateway、Prometheus、日志平台等组件配合使用。

## 技术概述

本节说明 Sentinel 在 Spring Cloud Alibaba 体系中的定位、接入方式和适用场景，帮助在项目设计阶段明确它解决什么问题、放在哪一层、如何与现有微服务架构协作。

### Sentinel 功能定位

Sentinel 是面向微服务的高可用流量治理组件，核心定位是围绕“流量”对系统进行保护。它主要覆盖流量控制、熔断降级、系统自适应保护、热点参数限流、授权控制和实时监控等能力。官方文档也将 Sentinel 定义为从流量切入，在流控、熔断降级、系统负载保护等维度保障服务稳定性的组件。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/wiki/sentinel?utm_source=chatgpt.com))

在 Spring Boot 3 项目中，Sentinel 通常承担以下职责：

| 功能         | 说明                                                        | 常见使用位置                               |
| ------------ | ----------------------------------------------------------- | ------------------------------------------ |
| 流量控制     | 按 QPS、并发线程数、调用关系等维度限制访问量                | Controller 接口、Service 方法、网关路由    |
| 熔断降级     | 当接口慢调用、异常比例或异常数达到阈值时，临时切断调用      | OpenFeign 调用、外部接口调用、核心业务接口 |
| 热点参数限流 | 针对某个参数值进行精细化限流，例如商品 ID、用户 ID、租户 ID | 秒杀、查询详情、热门资源访问               |
| 系统保护     | 根据 Load、CPU、RT、入口 QPS、线程数等指标保护整个应用      | 高负载服务、核心交易服务                   |
| 授权控制     | 根据调用来源进行黑白名单控制                                | 内部接口、灰度接口、管理接口               |
| 实时监控     | 在 Dashboard 查看资源调用、通过规则管理页面动态调整策略     | 开发、测试、预发、生产观测                 |

Sentinel 的核心概念是“资源”。一个 URL、一个方法、一个 Feign 调用、一个 Gateway 路由都可以被视为资源。规则不是直接绑定“接口代码”，而是绑定资源名称。资源被访问时，Sentinel 会根据已加载的规则判断是否放行、限流、熔断或降级。

在项目中需要明确两类资源：

| 资源类型   | 示例                                                     | 说明                                                         |
| ---------- | -------------------------------------------------------- | ------------------------------------------------------------ |
| 自动资源   | `/api/order/list`、`GET:http://order-service/order/{id}` | 由 Sentinel 适配器自动识别，适合 Web、Feign、Gateway 场景    |
| 自定义资源 | `orderCreate`、`productDetailQuery`                      | 使用 `@SentinelResource` 或 `SphU.entry()` 主动定义，适合核心业务方法 |

实践中建议不要只依赖 URL 自动资源。对于核心业务逻辑，应使用明确的业务资源名，例如 `orderCreate`、`paySubmit`、`couponReceive`，这样规则更稳定，不会因为 URL 调整导致治理规则失效。

### Spring Cloud Alibaba 集成方式

Spring Cloud Alibaba 通过 `spring-cloud-starter-alibaba-sentinel` starter 对 Sentinel 进行封装，使 Spring Boot 应用可以通过依赖和少量配置快速接入 Sentinel。官方文档中也明确说明，引入 Sentinel 时使用 `com.alibaba.cloud` 分组下的 `spring-cloud-starter-alibaba-sentinel` 依赖。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/wiki/sentinel?utm_source=chatgpt.com))

典型集成方式如下：

| 集成点               | 接入方式                                  | 作用                           |
| -------------------- | ----------------------------------------- | ------------------------------ |
| Spring MVC           | 引入 Sentinel Starter 后自动适配 Web 请求 | 对 Controller URL 自动生成资源 |
| `@SentinelResource`  | 在业务方法上声明资源名                    | 对 Service 方法进行精细化治理  |
| OpenFeign            | 开启 Feign Sentinel 支持                  | 对远程调用进行熔断降级         |
| Spring Cloud Gateway | 引入 Gateway 相关适配                     | 对网关路由、API 分组进行限流   |
| Nacos DataSource     | 引入 Sentinel Nacos 数据源依赖            | 实现规则持久化和动态刷新       |
| Sentinel Dashboard   | 应用配置 Dashboard 地址                   | 进行实时监控和规则管理         |

基础接入的 Maven 依赖通常包含 Sentinel Starter：

```xml
<!-- Spring Cloud Alibaba Sentinel：接入流控、熔断降级、系统保护等能力 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

如果需要将规则持久化到 Nacos，还需要额外引入 Sentinel Nacos 数据源依赖：

```xml
<!-- Sentinel Nacos 数据源：用于从 Nacos 动态加载流控、熔断、热点参数等规则 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
</dependency>
```

最小化配置一般放在 `application.yml` 中：

```yaml
spring:
  application:
    # Sentinel Dashboard 中展示的应用名称，建议与注册中心服务名保持一致
    name: order-service
  cloud:
    sentinel:
      transport:
        # Sentinel Dashboard 地址
        dashboard: 127.0.0.1:8080
        # 当前应用与 Dashboard 通信使用的本地端口，默认 8719，冲突时会自动递增
        port: 8719
```

集成后的基本流程是：应用启动后，Sentinel 客户端随应用一起运行；当接口第一次被访问时，资源信息会被采集；客户端通过心跳上报到 Sentinel Dashboard；Dashboard 可以展示机器列表、资源监控和规则配置。Sentinel Dashboard 官方说明也包含机器发现、单机或集群资源监控、规则管理等能力。([Sentinel Guard](https://sentinelguard.io/en-us/docs/dashboard.html?utm_source=chatgpt.com))

### 适用业务场景

Sentinel 适合用于对稳定性、吞吐量、响应时间和依赖隔离有要求的微服务系统。它尤其适合存在突发流量、核心链路保护、下游依赖不稳定、租户或用户访问不均衡的系统。

常见业务场景如下：

| 场景               | 问题                             | Sentinel 处理方式                      |
| ------------------ | -------------------------------- | -------------------------------------- |
| 秒杀、抢购、活动页 | 瞬时流量远超系统承载能力         | 对活动接口配置 QPS 限流、热点参数限流  |
| 商品详情、文章详情 | 少量热点 ID 被高频访问           | 对商品 ID、文章 ID 配置热点参数限流    |
| 订单提交、支付提交 | 核心接口不能被非核心流量拖垮     | 对核心接口设置并发线程数和系统保护规则 |
| 调用第三方接口     | 第三方响应慢或异常率高           | 使用熔断降级，快速失败并返回兜底结果   |
| OpenFeign 服务调用 | 下游服务不可用导致调用方线程堆积 | 开启 Feign Sentinel，配置 fallback     |
| 网关入口流量治理   | 某些路由或 API 分组流量过大      | 在 Spring Cloud Gateway 层配置网关限流 |
| 多租户系统         | 某个租户流量异常影响其他租户     | 按租户 ID 配置热点参数或授权规则       |
| 后台管理接口       | 只允许指定来源访问               | 使用授权规则配置黑白名单               |

不建议在以下场景中滥用 Sentinel：

| 不建议场景     | 原因                                          | 替代方案                           |
| -------------- | --------------------------------------------- | ---------------------------------- |
| 单体低流量系统 | 接入成本可能高于收益                          | 使用普通异常处理、线程池、缓存即可 |
| 纯权限控制     | Sentinel 授权规则不是完整权限系统             | 使用 Spring Security、Sa-Token 等  |
| 精准业务校验   | Sentinel 只做流量治理，不做业务规则校验       | 在业务代码中校验                   |
| 强一致限流计费 | Sentinel 默认更偏运行时保护，不适合做计费依据 | 使用 Redis、Lua、网关计费系统等    |

实际项目中建议按照“入口层限流、服务层熔断、核心方法自定义资源、规则持久化到 Nacos”的方式落地。入口层控制总流量，服务层隔离不稳定依赖，核心方法使用稳定资源名，Nacos 负责规则动态管理。

## 环境准备

本节用于明确 Spring Boot 3、Spring Cloud、Spring Cloud Alibaba、Sentinel Dashboard 的版本关系和运行要求。Sentinel 接入失败的问题很多不是代码问题，而是版本组合不匹配、Dashboard 未启动、应用未产生请求、规则未持久化导致的。

### Spring Boot 3 版本要求

Spring Boot 3 的基础要求与 Spring Boot 2.x 有明显差异。官方迁移指南说明，Spring Boot 3.0 要求 Java 17 或更高版本，并基于 Spring Framework 6.0。([GitHub](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide?utm_source=chatgpt.com))

开发环境建议如下：

| 项目             | 建议版本                   | 说明                                                    |
| ---------------- | -------------------------- | ------------------------------------------------------- |
| JDK              | 17 或 21                   | Spring Boot 3 最低要求 Java 17；生产可优先选择 LTS 版本 |
| Spring Boot      | 3.2.x、3.3.x、3.4.x、3.5.x | 需要与 Spring Cloud Release Train 对齐                  |
| Spring Framework | 6.x                        | 由 Spring Boot 3 自动管理                               |
| Maven            | 3.8+                       | 推荐使用较新 Maven，减少插件兼容问题                    |
| Gradle           | 8.x                        | 如果使用 Gradle，建议与 Spring Boot 插件版本匹配        |
| Servlet API      | Jakarta Servlet            | Spring Boot 3 已从 `javax.*` 迁移到 `jakarta.*`         |

Spring Boot 3 项目需要特别注意 `javax` 到 `jakarta` 的迁移。官方迁移指南说明，Spring Boot 3 使用 Jakarta EE 版本的规范，代码中的 `javax.*` 导入通常需要改为 `jakarta.*`。([GitHub](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide?utm_source=chatgpt.com))

典型影响包括：

```java
// Spring Boot 2.x 常见写法
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

// Spring Boot 3.x 应使用 Jakarta 包
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
```

如果项目是从 Spring Boot 2.x 升级到 Spring Boot 3，需要重点检查以下内容：

| 检查项                    | 处理建议                                                     |
| ------------------------- | ------------------------------------------------------------ |
| JDK 版本                  | 升级到 JDK 17 或 JDK 21                                      |
| `javax.*` 包              | 替换为 `jakarta.*`                                           |
| Spring Cloud 版本         | 选择与 Spring Boot 3 匹配的 Release Train                    |
| Spring Cloud Alibaba 版本 | 使用 2023.x、2025.0.x 等支持 Spring Boot 3 的版本线          |
| Sentinel 版本             | 避免使用过旧版本，Spring Boot 3 项目建议使用支持 WebMVC 6.x 的 Sentinel 版本 |
| 老旧组件                  | 检查 Knife4j、Swagger、MyBatis、Redis、Security 等依赖是否支持 Spring Boot 3 |

### Spring Cloud Alibaba 版本选择

Spring Cloud Alibaba 必须和 Spring Boot、Spring Cloud 版本配套选择。不要只看某个 starter 的最新版本，也不要混用多个大版本线。Spring Cloud Alibaba 的 GitHub Release 显示，`2025.1.0.0` 已将 Spring Boot 依赖提升到 4.0.0，并支持 Spring Cloud 2025.1.x；因此它不适合作为普通 Spring Boot 3 项目的默认选择。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/releases))

对于 Spring Boot 3 项目，可以按以下策略选择：

| Spring Boot 目标版本 | Spring Cloud 版本线 | Spring Cloud Alibaba 建议版本 | 适用建议                                       |
| -------------------- | ------------------- | ----------------------------- | ---------------------------------------------- |
| 3.2.x                | 2023.0.x            | 2023.0.3.4                    | 稳定优先，适合大多数 Spring Boot 3 微服务项目  |
| 3.5.x                | 2025.0.x            | 2025.0.0.0                    | 新项目可选，但要整体升级 Spring Cloud 依赖     |
| 4.0.x                | 2025.1.x            | 2025.1.0.0                    | Spring Boot 4 项目使用，不建议 Boot 3 项目使用 |

Spring Cloud 官方发布说明显示，Spring Cloud 2023.0.3 基于 Spring Boot 3.2.7，适合与 Spring Boot 3.2.x 体系配合使用。([Home](https://spring.io/blog/2024/07/11/spring-cloud-2023-0-3-aka-leyton-has-been-released/?utm_source=chatgpt.com)) Spring Cloud Alibaba Release 中 `2023.0.3.4` 是 2023.0.x 版本线的较新版本，适合作为 Spring Boot 3.2.x 项目的保守选择。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/releases))

推荐的 Maven BOM 管理方式如下：

```xml
<properties>
    <!-- JDK 版本：Spring Boot 3 最低要求 Java 17 -->
    <java.version>17</java.version>

    <!-- Spring Boot 3.2.x 项目可选版本，需与 Spring Cloud 2023.0.x 配套 -->
    <spring-boot.version>3.2.7</spring-boot.version>

    <!-- Spring Cloud 2023.0.3 基于 Spring Boot 3.2.7 -->
    <spring-cloud.version>2023.0.3</spring-cloud.version>

    <!-- Spring Cloud Alibaba 2023.0.x 版本线，适合 Spring Boot 3 稳定项目 -->
    <spring-cloud-alibaba.version>2023.0.3.4</spring-cloud-alibaba.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Spring Boot 依赖版本管理 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Spring Cloud 依赖版本管理 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Spring Cloud Alibaba 依赖版本管理 -->
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

如果项目已经使用 Spring Boot 3.5.x，则可以考虑 Spring Cloud Alibaba `2025.0.0.0`。该版本的 Release 信息显示其依赖 Spring Boot 3.5.0、Spring Cloud 2025.0.0，并包含 Sentinel 对 Spring Cloud Gateway 2025 版本的支持。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/releases))

版本选择原则如下：

| 原则                       | 说明                                                         |
| -------------------------- | ------------------------------------------------------------ |
| 不跨大版本混用             | 不要用 Spring Boot 3.2 搭配 Spring Cloud 2025.0.x，除非官方兼容矩阵明确支持 |
| 优先使用 BOM               | 不建议每个 starter 单独写版本号                              |
| 生产环境保守选择           | 业务系统优先选稳定维护版本，不追最新大版本                   |
| Dashboard 与客户端版本接近 | Sentinel Dashboard 和 Sentinel 客户端版本尽量保持一致或接近  |
| 升级前做压测验证           | Sentinel 规则、Dashboard 通信、Feign fallback、Gateway 规则都需要验证 |

### Sentinel Dashboard 部署

Sentinel Dashboard 是一个独立的 Spring Boot Web 应用，用于查看资源监控、机器列表和配置规则。官方 Dashboard 文档说明，它提供机器发现、单机或集群资源监控、规则管理等功能，并可以通过 jar 包方式启动。([Sentinel Guard](https://sentinelguard.io/en-us/docs/dashboard.html?utm_source=chatgpt.com))

生产或测试环境中，Dashboard 一般单独部署，不建议和业务服务部署在同一个进程中。Dashboard 主要用于规则配置和监控查看，但规则持久化仍建议放到 Nacos、Apollo、ZooKeeper 等外部配置中心，否则 Dashboard 修改的规则在应用重启后可能丢失。

下载方式可以选择 GitHub Release 或镜像站。Sentinel Release 显示，`v1.8.9` 是较新的稳定版本；`v1.8.8` 增加了 Spring Boot 3.x 相关的 WebMVC 6.x adapter 支持。([GitHub](https://github.com/alibaba/sentinel/releases?utm_source=chatgpt.com)) SourceForge 镜像也显示最新版本包含 `sentinel-dashboard-1.8.9.jar`。([SourceForge](https://sourceforge.net/projects/sentinel.mirror/files/?utm_source=chatgpt.com))

本地开发环境可以使用以下命令启动 Dashboard：

```bash
# 启动 Sentinel Dashboard
# -Dserver.port：Dashboard Web 访问端口
# -Dcsp.sentinel.dashboard.server：Dashboard 自身地址
# -Dproject.name：Dashboard 应用名称
java \
  -Dserver.port=8080 \
  -Dcsp.sentinel.dashboard.server=127.0.0.1:8080 \
  -Dproject.name=sentinel-dashboard \
  -jar sentinel-dashboard-1.8.9.jar
```

启动成功后，访问地址通常为：

```text
http://127.0.0.1:8080
```

默认账号密码通常为：

```text
账号：sentinel
密码：sentinel
```

业务应用需要配置 Dashboard 地址：

```yaml
spring:
  application:
    # Dashboard 中展示的应用名称
    name: order-service
  cloud:
    sentinel:
      transport:
        # Dashboard 服务地址
        dashboard: 127.0.0.1:8080
        # Sentinel 客户端本地通信端口，端口冲突时会自动递增
        port: 8719
```

需要注意的是，应用启动后 Dashboard 不一定立即显示服务。通常要满足以下条件：

| 条件                                                   | 说明                                                         |
| ------------------------------------------------------ | ------------------------------------------------------------ |
| Dashboard 已启动                                       | 先启动 Dashboard，再启动业务应用                             |
| 应用配置了 `spring.application.name`                   | Dashboard 需要展示应用名                                     |
| 应用配置了 `spring.cloud.sentinel.transport.dashboard` | 客户端需要知道 Dashboard 地址                                |
| 接口至少被访问一次                                     | Sentinel 资源通常在首次访问后才会上报                        |
| 网络端口可通                                           | 应用到 Dashboard、Dashboard 到应用客户端端口都不能被防火墙阻断 |

本地验证命令示例：

```bash
# 访问业务接口，触发 Sentinel 资源初始化和上报
curl http://127.0.0.1:8081/test/hello

# 检查 Dashboard 端口是否监听
netstat -tunlp | grep 8080

# 检查业务服务 Sentinel 客户端端口是否监听
netstat -tunlp | grep 8719
```

常见问题处理：

| 问题                  | 可能原因                                   | 处理方式                                                |
| --------------------- | ------------------------------------------ | ------------------------------------------------------- |
| Dashboard 看不到应用  | 应用未访问接口、Dashboard 地址配置错误     | 访问一次接口，检查 `dashboard` 配置                     |
| 规则配置后重启丢失    | 只使用 Dashboard 内存规则                  | 接入 Nacos 等动态数据源                                 |
| 应用端口 8719 冲突    | 多个应用在同一机器启动                     | 为不同服务配置不同 `transport.port`                     |
| Feign fallback 不生效 | 未开启 Feign Sentinel 或 fallback 配置错误 | 配置 `feign.sentinel.enabled=true` 并检查 fallback Bean |
| Gateway 规则不生效    | 未引入 Gateway 适配或资源名不匹配          | 检查路由 ID、API 分组和 Gateway Sentinel 依赖           |

开发阶段可以先通过 Dashboard 手动配置规则，验证限流和熔断效果；测试和生产环境建议将规则迁移到 Nacos，并由配置中心统一管理。这样可以避免应用重启后规则丢失，也便于多实例规则同步。

## 项目依赖配置

本节用于统一项目中的 Maven BOM、Sentinel Starter、Nacos 配置中心和 Sentinel 规则数据源依赖。你给出的原始大纲中，这一部分位于「环境准备」之后，用于承接后续的「基础接入、流控、熔断、规则持久化」章节。

Spring Cloud Alibaba 官方项目页建议通过 `spring-cloud-alibaba-dependencies` BOM 管理版本，再按需引入具体 starter；Spring Cloud 官方兼容表显示，Spring Cloud `2023.0.x` 对应 Spring Boot `3.2.x/3.3.x`，Spring Cloud `2025.0.x` 对应 Spring Boot `3.5.x`。因此 Spring Boot 3 项目不要混用不匹配的大版本线。([Home](https://spring.io/projects/spring-cloud-alibaba?utm_source=chatgpt.com))

### Maven 依赖引入

Maven 依赖建议统一通过 `dependencyManagement` 管理版本，业务模块中只声明依赖坐标，不单独写版本号。这样可以避免 Sentinel、Nacos、Spring Cloud、Spring Boot 之间出现传递依赖冲突。

以下示例以 Spring Boot `3.2.7`、Spring Cloud `2023.0.3`、Spring Cloud Alibaba `2023.0.3.4` 为基准，适合保守型 Spring Boot 3 微服务项目。Maven Central 已提供 `spring-cloud-starter-alibaba-sentinel` 的 `2023.0.3.4` 版本。([Maven Repository](https://repo1.maven.org/maven2/com/alibaba/cloud/spring-cloud-starter-alibaba-sentinel/2023.0.3.4/?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 3 最低要求 JDK 17，生产环境可使用 JDK 17 或 JDK 21 -->
    <java.version>17</java.version>

    <!-- Spring Boot 版本：示例采用 3.2.x 稳定版本线 -->
    <spring-boot.version>3.2.7</spring-boot.version>

    <!-- Spring Cloud 版本：2023.0.x 可用于 Spring Boot 3.2.x / 3.3.x -->
    <spring-cloud.version>2023.0.3</spring-cloud.version>

    <!-- Spring Cloud Alibaba 版本：与 Spring Cloud 2023.0.x 配套 -->
    <spring-cloud-alibaba.version>2023.0.3.4</spring-cloud-alibaba.version>

    <!-- Hutool 工具类，示例接口中用于构建返回数据 -->
    <hutool.version>5.8.35</hutool.version>

    <!-- Lombok 简化日志对象、构造器和实体类代码 -->
    <lombok.version>1.18.34</lombok.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Spring Boot 官方 BOM：统一管理 Spring Boot 生态依赖版本 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Spring Cloud 官方 BOM：统一管理 OpenFeign、Gateway、LoadBalancer 等依赖版本 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Spring Cloud Alibaba BOM：统一管理 Sentinel、Nacos、Seata 等依赖版本 -->
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

业务模块中引入基础 Web、Sentinel、Nacos、Hutool 和 Lombok 依赖：

```xml
<dependencies>
    <!-- Spring MVC Web：用于提供 Controller 接口，Sentinel 会自动对 Web URL 资源埋点 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Cloud Alibaba Sentinel：接入限流、熔断降级、热点参数、系统保护等能力 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
    </dependency>

    <!-- Spring Cloud Alibaba Nacos Discovery：服务注册发现，微服务项目通常会同时使用 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- Spring Cloud Alibaba Nacos Config：配置中心，用于读取应用配置 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>

    <!-- Sentinel Nacos 数据源：用于从 Nacos 动态读取 Sentinel 规则 -->
    <dependency>
        <groupId>com.alibaba.csp</groupId>
        <artifactId>sentinel-datasource-nacos</artifactId>
    </dependency>

    <!-- Hutool：常用工具类库，用于字符串、集合、日期、JSON 等工具操作 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：简化日志对象、Getter、Setter、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：用于后续接口测试和规则验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot `3.5.x`，可以改为 Spring Cloud `2025.0.x` 与 Spring Cloud Alibaba `2025.0.0.0` 版本线。Spring Cloud `2025.0.0` 官方说明其兼容 Spring Boot `3.5.0`，Spring Cloud Alibaba `2025.0.0.0` Release 中也说明其 Spring Boot 依赖提升到 `3.5.0`，并包含 Sentinel 对 Spring Cloud Gateway 2025 版本的支持。([Home](https://spring.io/blog/2025/05/29/spring-cloud-2025-0-0-is-abvailable/?utm_source=chatgpt.com))

### Sentinel Starter 配置

`spring-cloud-starter-alibaba-sentinel` 是 Spring Cloud Alibaba 对 Sentinel 的 Spring Boot 自动装配入口。引入该 starter 后，应用会自动装配 Sentinel 相关组件，并对 Spring MVC Web 请求进行资源识别。Spring Cloud Alibaba Sentinel 文档也说明，接入 Sentinel 时使用 `com.alibaba.cloud:spring-cloud-starter-alibaba-sentinel` 依赖。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/wiki/sentinel?utm_source=chatgpt.com))

Sentinel Starter 主要提供以下能力：

| 能力             | 说明                                                        |
| ---------------- | ----------------------------------------------------------- |
| Web 资源自动埋点 | 自动将 Controller URL 注册为 Sentinel 资源                  |
| Dashboard 通信   | 通过 `spring.cloud.sentinel.transport.dashboard` 连接控制台 |
| 规则加载         | 支持本地、Nacos、ZooKeeper、Apollo 等数据源                 |
| 注解资源         | 支持通过 `@SentinelResource` 定义业务资源                   |
| 异常处理扩展     | 支持自定义 `BlockException` 返回结果                        |
| Feign 集成       | 配合 `feign.sentinel.enabled=true` 支持 Feign 熔断降级      |

基础配置文件如下：

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 当前业务服务端口
  port: 8081

spring:
  application:
    # 应用名称：会显示在 Sentinel Dashboard 机器列表中
    name: sentinel-demo-service

  cloud:
    sentinel:
      # Sentinel 控制台通信配置
      transport:
        # Sentinel Dashboard 地址
        dashboard: 127.0.0.1:8080
        # 当前应用暴露给 Dashboard 通信的本地端口
        port: 8719

      # 是否立即初始化 Sentinel 客户端
      # true：应用启动时初始化；false：首次资源访问时初始化
      eager: true

      # Web 场景下的 URL 资源配置
      web-context-unify: false

# OpenFeign 接入 Sentinel 时开启，当前章节只做预留
feign:
  sentinel:
    # 开启 Feign Sentinel 支持，后续 OpenFeign 降级章节会使用
    enabled: true

logging:
  level:
    # Sentinel 相关日志级别，开发阶段可设置为 INFO 便于排查
    com.alibaba.csp.sentinel: info
    com.alibaba.cloud.sentinel: info
```

`spring.cloud.sentinel.eager=true` 建议在开发和测试阶段开启。默认情况下，Sentinel 资源通常在首次访问后才初始化和上报，容易导致“应用已启动但 Dashboard 看不到服务”的误判。开启 eager 后，Sentinel 客户端会尽早初始化，但资源监控数据仍然需要实际请求触发。

`spring.cloud.sentinel.web-context-unify=false` 用于关闭 Web Context 统一入口。实际业务中如果不关闭，多个 URL 可能在调用链上被归并到同一个入口上下文，观察和规则配置时不够直观。对于需要按 URL 精细限流的 Spring MVC 项目，通常建议设置为 `false`。

### Nacos 配置依赖

Nacos 在 Sentinel 项目中通常有两种用途：第一是作为 Spring Cloud 的配置中心和注册中心；第二是作为 Sentinel 规则的动态数据源。前者由 `spring-cloud-starter-alibaba-nacos-config` 和 `spring-cloud-starter-alibaba-nacos-discovery` 提供，后者由 `sentinel-datasource-nacos` 提供。

Spring Cloud Alibaba Sentinel 文档说明，Sentinel 数据源依赖需要单独引入，例如使用 Nacos 数据源时需要引入 `sentinel-datasource-nacos`；数据源通用配置包含 `data-type`、`rule-type` 等配置项。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/wiki/sentinel?utm_source=chatgpt.com))

Nacos 基础配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # Nacos 注册中心地址
        server-addr: 127.0.0.1:8848
        # 命名空间 ID，开发环境可为空，生产建议按环境隔离
        namespace:
        # 服务分组
        group: DEFAULT_GROUP

      config:
        # Nacos 配置中心地址
        server-addr: 127.0.0.1:8848
        # 配置文件格式
        file-extension: yaml
        # 命名空间 ID，需与 Nacos 控制台中配置保持一致
        namespace:
        # 配置分组
        group: DEFAULT_GROUP
```

如果需要从 Nacos 加载 Sentinel 流控规则，可以在 `application.yml` 中添加数据源配置：

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        flow-rule:
          nacos:
            # Nacos 地址
            server-addr: 127.0.0.1:8848
            # Nacos 中保存 Sentinel 流控规则的 Data ID
            data-id: sentinel-demo-service-flow-rules
            # Nacos 配置分组
            group-id: DEFAULT_GROUP
            # 规则数据格式，常用 json
            data-type: json
            # 规则类型：flow 表示流控规则
            rule-type: flow
```

Nacos 中对应的流控规则示例：

```json
[
  {
    "resource": "/sentinel/hello",
    "limitApp": "default",
    "grade": 1,
    "count": 5,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

字段说明：

| 字段              | 说明                                       |
| ----------------- | ------------------------------------------ |
| `resource`        | 资源名称，Web 自动埋点时通常是 URL 路径    |
| `limitApp`        | 调用来源，`default` 表示不区分来源         |
| `grade`           | 阈值类型，`1` 表示 QPS，`0` 表示并发线程数 |
| `count`           | 限流阈值                                   |
| `strategy`        | 流控模式，`0` 表示直接模式                 |
| `controlBehavior` | 流控效果，`0` 表示快速失败                 |
| `clusterMode`     | 是否为集群限流                             |

后续如果需要配置熔断规则、热点参数规则、授权规则，只需要继续在 `spring.cloud.sentinel.datasource` 下增加不同的数据源节点，例如 `degrade-rule`、`param-flow-rule`、`authority-rule`，并配置对应的 `rule-type`。

## Sentinel 基础接入

本节用于完成 Sentinel 的最小可运行接入。完成后，应用启动并访问接口，应该可以在 Sentinel Dashboard 中看到应用机器、URL 资源和实时请求数据。

### 应用名称配置

应用名称是 Sentinel Dashboard 识别服务的核心字段。一般建议使用 `spring.application.name` 作为应用名，并与 Nacos 注册中心服务名保持一致，这样便于在注册发现、日志、监控和 Sentinel Dashboard 中统一排查问题。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称：建议与 Nacos 注册服务名、日志服务名保持一致
    name: sentinel-demo-service
```

命名建议如下：

| 类型     | 示例              | 说明                 |
| -------- | ----------------- | -------------------- |
| 订单服务 | `order-service`   | 按业务域命名         |
| 支付服务 | `pay-service`     | 简洁明确             |
| 商品服务 | `product-service` | 与注册中心服务名一致 |
| 网关服务 | `gateway-service` | 便于配置网关限流规则 |

不建议使用过于宽泛的名称，例如 `demo`、`app`、`service`。在多服务环境中，这类名称无法快速定位业务归属。

### Dashboard 地址配置

Dashboard 地址用于让当前应用连接 Sentinel 控制台。应用启动后，Sentinel 客户端会向 Dashboard 上报心跳、机器信息和资源监控数据。Dashboard 官方文档说明，它提供机器发现、资源监控和规则管理能力。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/wiki/sentinel?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    sentinel:
      transport:
        # Sentinel Dashboard 访问地址，格式为 host:port
        dashboard: 127.0.0.1:8080

        # 当前应用与 Dashboard 通信的客户端端口
        # 如果本机启动多个服务，建议为每个服务配置不同端口
        port: 8719
```

如果在 Docker、Kubernetes 或远程测试环境部署，需要注意 `dashboard` 不能写成业务容器内部无法访问的地址。常见配置如下：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        # Docker Compose 中可使用 Dashboard 服务名
        dashboard: sentinel-dashboard:8080
        # 当前应用 Sentinel 通信端口
        port: 8719
```

Kubernetes 环境中可以配置为 Service 地址：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        # Kubernetes Service DNS 地址示例
        dashboard: sentinel-dashboard.sentinel.svc.cluster.local:8080
        port: 8719
```

Dashboard 地址配置后，仍然需要访问业务接口才会产生资源数据。仅启动应用但没有任何请求时，Dashboard 中可能看不到资源监控曲线。

### Web 资源自动埋点

引入 Sentinel Starter 后，Spring MVC 的 URL 请求会被自动识别为 Sentinel 资源。也就是说，不需要在每个 Controller 方法上手动添加 `@SentinelResource`，普通 Web 接口就可以在 Dashboard 中看到 URL 资源。

下面提供一个用于验证 Web 自动埋点的接口示例。

文件位置：`src/main/java/io/github/atengk/sentinel/controller/SentinelTestController.java`

```java
package io.github.atengk.sentinel.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sentinel 基础接入测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class SentinelTestController {

    /**
     * 测试普通 Web URL 自动埋点
     *
     * @return 接口响应数据
     */
    @GetMapping("/sentinel/hello")
    public Map<String, Object> hello() {
        log.info("访问 Sentinel Web 自动埋点测试接口");
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "Sentinel Web 资源访问成功")
                .put("resource", "/sentinel/hello")
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 测试慢接口资源，后续熔断降级章节可复用
     *
     * @return 接口响应数据
     * @throws InterruptedException 线程休眠异常
     */
    @GetMapping("/sentinel/slow")
    public Map<String, Object> slow() throws InterruptedException {
        log.info("访问 Sentinel 慢调用测试接口");
        Thread.sleep(1200L);
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "Sentinel 慢调用测试接口访问成功")
                .put("resource", "/sentinel/slow")
                .put("time", DateUtil.now())
                .build();
    }
}
```

启动应用并访问接口后，Dashboard 的簇点链路中通常可以看到以下资源：

```text
/sentinel/hello
/sentinel/slow
```

如果没有看到资源，需要先确认接口是否已经被访问。Sentinel 的资源统计通常基于实际调用产生，不是应用启动时扫描出所有 Controller 接口。

### 启动验证方式

启动验证分为四步：启动 Sentinel Dashboard、启动 Nacos、启动业务应用、访问测试接口并查看 Dashboard。开发阶段建议先完成本地单机验证，再接入 Nacos 规则持久化。

启动 Sentinel Dashboard：

```bash
# 启动 Sentinel Dashboard，默认 Web 端口为 8080
java \
  -Dserver.port=8080 \
  -Dcsp.sentinel.dashboard.server=127.0.0.1:8080 \
  -Dproject.name=sentinel-dashboard \
  -jar sentinel-dashboard-1.8.9.jar
```

启动业务服务：

```bash
# 在项目根目录执行 Maven 启动命令
mvn spring-boot:run
```

访问测试接口：

```bash
# 触发 Web URL 资源自动埋点
curl http://127.0.0.1:8081/sentinel/hello

# 触发慢调用测试接口
curl http://127.0.0.1:8081/sentinel/slow
```

期望响应示例：

```json
{
  "code": 200,
  "message": "Sentinel Web 资源访问成功",
  "resource": "/sentinel/hello",
  "time": "2026-05-05 10:30:00"
}
```

Dashboard 验证步骤如下：

| 步骤           | 验证内容                                               |
| -------------- | ------------------------------------------------------ |
| 登录 Dashboard | 访问 `http://127.0.0.1:8080`                           |
| 查看机器列表   | 左侧应出现 `sentinel-demo-service`                     |
| 查看实时监控   | 访问接口后应出现 QPS、RT 等数据                        |
| 查看簇点链路   | 应出现 `/sentinel/hello`、`/sentinel/slow` 等 URL 资源 |
| 添加流控规则   | 对 `/sentinel/hello` 设置 QPS 阈值，例如 `1`           |
| 压测验证       | 快速多次请求接口，应出现 Sentinel 限流返回             |

可以使用循环请求快速验证限流效果：

```bash
# 连续请求 10 次，用于触发 QPS 限流
for i in $(seq 1 10); do
  curl -s http://127.0.0.1:8081/sentinel/hello
  echo
done
```

如果 Dashboard 看不到应用，按以下顺序排查：

| 问题               | 排查方式                                         |
| ------------------ | ------------------------------------------------ |
| Dashboard 未启动   | 浏览器访问 `http://127.0.0.1:8080`               |
| 应用未配置名称     | 检查 `spring.application.name`                   |
| Dashboard 地址错误 | 检查 `spring.cloud.sentinel.transport.dashboard` |
| 业务接口未访问     | 至少访问一次 `/sentinel/hello`                   |
| 客户端端口冲突     | 修改 `spring.cloud.sentinel.transport.port`      |
| Docker 网络不通    | 确认业务容器能访问 Dashboard 容器                |
| 规则不生效         | 确认资源名与规则中的 `resource` 完全一致         |

完成以上验证后，说明 Sentinel Starter、Dashboard 通信、Web 自动埋点和基础资源上报已经接入成功。后续可以继续基于 `/sentinel/hello` 配置 URL 资源限流，基于 `/sentinel/slow` 配置慢调用比例熔断，并将规则迁移到 Nacos 实现动态刷新和持久化。

## 流量控制开发

本节用于补齐大纲中的流量控制部分，覆盖 URL 自动资源限流、自定义业务资源限流、QPS 限流和并发线程数限流。Sentinel 的流控规则主要作用于资源名，规则字段包括 `resource`、`count`、`grade`、`strategy`、`controlBehavior` 等；其中 `grade` 用于区分 QPS 和并发线程数限流。([Sentinel Guard](https://sentinelguard.io/en-us/docs/flow-control.html?utm_source=chatgpt.com)) 原始大纲中该章节位于 Sentinel 基础接入之后，适合直接基于前面 `/sentinel/hello`、Dashboard 和 Nacos 配置继续展开。

### URL 资源限流

URL 资源限流适合对 Controller 接口进行入口级保护。引入 `spring-cloud-starter-alibaba-sentinel` 后，Spring MVC 请求会被 Sentinel 自动识别为 Web 资源，一般可以直接在 Dashboard 的“簇点链路”中看到 URL 资源，例如 `/sentinel/flow/url`。

文件位置：`src/main/java/io/github/atengk/sentinel/controller/SentinelFlowController.java`

下面的 Controller 提供 URL 限流、自定义资源限流和并发线程数限流测试接口。

```java
package io.github.atengk.sentinel.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import io.github.atengk.sentinel.service.SentinelBusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sentinel 流量控制测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SentinelFlowController {

    private final SentinelBusinessService sentinelBusinessService;

    /**
     * URL 资源限流测试接口
     *
     * @return 响应数据
     */
    @GetMapping("/sentinel/flow/url")
    public Map<String, Object> urlFlow() {
        log.info("访问 URL 资源限流测试接口");
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "URL 资源访问成功")
                .put("resource", "/sentinel/flow/url")
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 自定义资源限流测试接口
     *
     * @param orderNo 订单号
     * @param mockError 是否模拟异常
     * @return 响应数据
     */
    @GetMapping("/sentinel/flow/custom")
    public Map<String, Object> customFlow(String orderNo, Boolean mockError) {
        log.info("访问自定义资源限流测试接口，订单号：{}", orderNo);
        return sentinelBusinessService.queryOrder(orderNo, Boolean.TRUE.equals(mockError));
    }

    /**
     * 并发线程数限流测试接口
     *
     * @return 响应数据
     * @throws InterruptedException 线程休眠异常
     */
    @GetMapping("/sentinel/flow/thread")
    public Map<String, Object> threadFlow() throws InterruptedException {
        log.info("访问并发线程数限流测试接口");
        Thread.sleep(3000L);
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "并发线程数限流测试接口访问成功")
                .put("resource", "/sentinel/flow/thread")
                .put("time", DateUtil.now())
                .build();
    }
}
```

在 Dashboard 中可以对 `/sentinel/flow/url` 添加流控规则：

| 配置项   | 示例值               | 说明                                     |
| -------- | -------------------- | ---------------------------------------- |
| 资源名   | `/sentinel/flow/url` | 与 Dashboard 簇点链路中的 URL 资源名一致 |
| 阈值类型 | QPS                  | 按每秒请求数限流                         |
| 单机阈值 | `2`                  | 单实例每秒最多通过 2 个请求              |
| 流控模式 | 直接                 | 当前资源达到阈值后直接限流               |
| 流控效果 | 快速失败             | 超过阈值后立即拒绝                       |

如果使用 Nacos 持久化 URL 流控规则，可以添加以下配置。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        url-flow-rule:
          nacos:
            # Nacos 服务地址
            server-addr: 127.0.0.1:8848
            # URL 流控规则 Data ID
            data-id: sentinel-demo-service-url-flow-rules
            # Nacos 分组
            group-id: DEFAULT_GROUP
            # 规则格式
            data-type: json
            # 规则类型：flow 表示流控规则
            rule-type: flow
```

Nacos 配置内容如下：

```json
[
  {
    "resource": "/sentinel/flow/url",
    "limitApp": "default",
    "grade": 1,
    "count": 2,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

验证命令如下：

```bash
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/sentinel/flow/url"
  echo
done
```

正常情况下，每秒超过阈值的请求会被 Sentinel 拦截。默认返回内容不一定符合业务接口规范，后续可以在“异常与兜底处理”章节中通过统一 `BlockException` 处理器改造成统一 JSON 返回。

### 自定义资源限流

自定义资源限流适合保护核心业务方法。相比 URL 资源，自定义资源名不依赖接口路径，更适合用于订单创建、支付提交、优惠券领取、库存扣减、第三方接口调用等关键逻辑。

Sentinel 支持通过 `@SentinelResource` 注解定义资源，并可以配置 `blockHandler` 和 `fallback`；注解方式不支持 `private` 方法，`blockHandler` 方法需要是 `public`，返回类型和原方法一致，参数也需要与原方法一致并在最后追加 `BlockException` 参数。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/annotation-support.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/sentinel/service/SentinelBusinessService.java`

```java
package io.github.atengk.sentinel.service;

import java.util.Map;

/**
 * Sentinel 业务测试服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface SentinelBusinessService {

    /**
     * 查询订单信息
     *
     * @param orderNo 订单号
     * @param mockError 是否模拟异常
     * @return 订单响应数据
     */
    Map<String, Object> queryOrder(String orderNo, Boolean mockError);

    /**
     * 查询库存信息
     *
     * @param productId 商品 ID
     * @param mockSlow 是否模拟慢调用
     * @param mockError 是否模拟异常
     * @return 库存响应数据
     */
    Map<String, Object> queryStock(Long productId, Boolean mockSlow, Boolean mockError);
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/service/impl/SentinelBusinessServiceImpl.java`

下面的 Service 实现类定义了两个自定义 Sentinel 资源：`orderQueryResource` 用于流控测试，`stockQueryResource` 用于后续熔断降级测试。

```java
package io.github.atengk.sentinel.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.github.atengk.sentinel.service.SentinelBusinessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sentinel 业务测试服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class SentinelBusinessServiceImpl implements SentinelBusinessService {

    /**
     * 查询订单信息
     *
     * @param orderNo 订单号
     * @param mockError 是否模拟异常
     * @return 订单响应数据
     */
    @Override
    @SentinelResource(
            value = "orderQueryResource",
            entryType = EntryType.IN,
            blockHandler = "queryOrderBlockHandler",
            fallback = "queryOrderFallback"
    )
    public Map<String, Object> queryOrder(String orderNo, Boolean mockError) {
        if (StrUtil.isBlank(orderNo)) {
            log.warn("查询订单失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (Boolean.TRUE.equals(mockError)) {
            log.warn("查询订单模拟业务异常，订单号：{}", orderNo);
            throw new IllegalStateException("模拟订单查询异常");
        }

        log.info("查询订单成功，订单号：{}", orderNo);
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "订单查询成功")
                .put("resource", "orderQueryResource")
                .put("orderNo", orderNo)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 查询订单被 Sentinel 限流、熔断或系统保护时的处理逻辑
     *
     * @param orderNo 订单号
     * @param mockError 是否模拟异常
     * @param ex Sentinel 阻塞异常
     * @return 限流响应数据
     */
    public Map<String, Object> queryOrderBlockHandler(String orderNo, Boolean mockError, BlockException ex) {
        log.warn("订单查询触发 Sentinel 保护，订单号：{}，异常类型：{}", orderNo, ex.getClass().getSimpleName());
        return MapUtil.<String, Object>builder()
                .put("code", 429)
                .put("message", "订单查询过于频繁，请稍后重试")
                .put("resource", "orderQueryResource")
                .put("orderNo", orderNo)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 查询订单发生业务异常时的降级处理逻辑
     *
     * @param orderNo 订单号
     * @param mockError 是否模拟异常
     * @param ex 业务异常
     * @return 降级响应数据
     */
    public Map<String, Object> queryOrderFallback(String orderNo, Boolean mockError, Throwable ex) {
        log.error("订单查询发生业务异常，订单号：{}，异常信息：{}", orderNo, ex.getMessage());
        return MapUtil.<String, Object>builder()
                .put("code", 500)
                .put("message", "订单查询失败，已返回降级结果")
                .put("resource", "orderQueryResource")
                .put("orderNo", orderNo)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 查询库存信息
     *
     * @param productId 商品 ID
     * @param mockSlow 是否模拟慢调用
     * @param mockError 是否模拟异常
     * @return 库存响应数据
     */
    @Override
    @SentinelResource(
            value = "stockQueryResource",
            entryType = EntryType.IN,
            blockHandler = "queryStockBlockHandler",
            fallback = "queryStockFallback"
    )
    public Map<String, Object> queryStock(Long productId, Boolean mockSlow, Boolean mockError) {
        if (productId == null || productId <= 0) {
            log.warn("查询库存失败，商品 ID 不合法：{}", productId);
            throw new IllegalArgumentException("商品 ID 不合法");
        }

        if (Boolean.TRUE.equals(mockSlow)) {
            sleepQuietly(1200L);
        }

        if (Boolean.TRUE.equals(mockError)) {
            log.warn("查询库存模拟业务异常，商品 ID：{}", productId);
            throw new IllegalStateException("模拟库存查询异常");
        }

        log.info("查询库存成功，商品 ID：{}", productId);
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "库存查询成功")
                .put("resource", "stockQueryResource")
                .put("productId", productId)
                .put("stock", 100)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 查询库存被 Sentinel 保护时的处理逻辑
     *
     * @param productId 商品 ID
     * @param mockSlow 是否模拟慢调用
     * @param mockError 是否模拟异常
     * @param ex Sentinel 阻塞异常
     * @return 限流或熔断响应数据
     */
    public Map<String, Object> queryStockBlockHandler(Long productId, Boolean mockSlow, Boolean mockError, BlockException ex) {
        log.warn("库存查询触发 Sentinel 保护，商品 ID：{}，异常类型：{}", productId, ex.getClass().getSimpleName());
        return MapUtil.<String, Object>builder()
                .put("code", 429)
                .put("message", "库存查询暂不可用，请稍后重试")
                .put("resource", "stockQueryResource")
                .put("productId", productId)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 查询库存发生业务异常时的降级处理逻辑
     *
     * @param productId 商品 ID
     * @param mockSlow 是否模拟慢调用
     * @param mockError 是否模拟异常
     * @param ex 业务异常
     * @return 降级响应数据
     */
    public Map<String, Object> queryStockFallback(Long productId, Boolean mockSlow, Boolean mockError, Throwable ex) {
        log.error("库存查询发生业务异常，商品 ID：{}，异常信息：{}", productId, ex.getMessage());
        return MapUtil.<String, Object>builder()
                .put("code", 500)
                .put("message", "库存查询失败，已返回默认库存")
                .put("resource", "stockQueryResource")
                .put("productId", productId)
                .put("stock", 0)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 安静休眠
     *
     * @param millis 休眠毫秒数
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("线程休眠被中断");
        }
    }
}
```

自定义资源限流规则示例：

```json
[
  {
    "resource": "orderQueryResource",
    "limitApp": "default",
    "grade": 1,
    "count": 3,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

验证命令：

```bash
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/sentinel/flow/custom?orderNo=ORDER1001"
  echo
done
```

如果 QPS 超过阈值，返回结果会进入 `queryOrderBlockHandler`；如果传入 `mockError=true` 触发业务异常，则进入 `queryOrderFallback`。

```bash
curl "http://127.0.0.1:8081/sentinel/flow/custom?orderNo=ORDER1001&mockError=true"
```

### QPS 限流规则

QPS 限流用于控制单位时间内允许通过的请求数量。Sentinel 官方文档说明，QPS 超过阈值后会根据 `controlBehavior` 指定的流控效果处理请求，默认行为是快速失败并抛出 `FlowException`。([Sentinel Guard](https://sentinelguard.io/en-us/docs/flow-control.html?utm_source=chatgpt.com))

常用规则如下：

| 字段              | 示例值               | 说明                  |
| ----------------- | -------------------- | --------------------- |
| `resource`        | `/sentinel/flow/url` | 资源名                |
| `grade`           | `1`                  | QPS 限流              |
| `count`           | `2`                  | 每秒允许通过 2 个请求 |
| `strategy`        | `0`                  | 直接限流              |
| `controlBehavior` | `0`                  | 快速失败              |
| `clusterMode`     | `false`              | 单机限流              |

Nacos 中配置 QPS 限流规则：

```json
[
  {
    "resource": "/sentinel/flow/url",
    "limitApp": "default",
    "grade": 1,
    "count": 2,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  },
  {
    "resource": "orderQueryResource",
    "limitApp": "default",
    "grade": 1,
    "count": 3,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

验证 URL 资源：

```bash
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/sentinel/flow/url"
  echo
done
```

验证自定义资源：

```bash
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/sentinel/flow/custom?orderNo=ORDER1001"
  echo
done
```

QPS 限流适合接口调用频率保护，例如查询接口、提交接口、短信发送接口、验证码接口、活动接口。对于耗时较长的接口，单纯控制 QPS 不一定能保护线程资源，此时更适合使用并发线程数限流。

### 并发线程数限流规则

并发线程数限流用于控制某个资源正在处理的线程数量。Sentinel 官方文档说明，线程数限流通常用于保护线程资源，避免慢接口占满业务线程；相比额外线程池隔离，它是一种较轻量的信号量隔离方式。([Sentinel Guard](https://sentinelguard.io/en-us/docs/flow-control.html?utm_source=chatgpt.com))

并发线程数规则示例：

```json
[
  {
    "resource": "/sentinel/flow/thread",
    "limitApp": "default",
    "grade": 0,
    "count": 2,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

字段说明：

| 字段              | 示例值                  | 说明                            |
| ----------------- | ----------------------- | ------------------------------- |
| `resource`        | `/sentinel/flow/thread` | 资源名                          |
| `grade`           | `0`                     | 并发线程数限流                  |
| `count`           | `2`                     | 同时最多允许 2 个线程处理该资源 |
| `controlBehavior` | `0`                     | 快速失败                        |

验证时需要制造并发请求。可以使用 `xargs` 并发访问：

```bash
seq 1 10 | xargs -I {} -P 10 curl -s "http://127.0.0.1:8081/sentinel/flow/thread"
```

也可以使用 Apache Bench：

```bash
ab -n 20 -c 10 "http://127.0.0.1:8081/sentinel/flow/thread"
```

预期效果是同一时间只有少量请求进入业务方法，其余请求会被 Sentinel 快速拒绝。并发线程数限流更适合慢接口、外部接口调用、文件处理、报表导出、复杂聚合查询等场景。

## 熔断降级开发

本节用于补齐熔断降级开发内容，覆盖慢调用比例、异常比例、异常数和降级返回处理。Sentinel 从 1.8.0 开始支持基于慢调用比例、异常比例和异常数的熔断策略，用于在下游资源不稳定时快速失败，避免级联故障。([Sentinel Guard](https://sentinelguard.io/en-us/docs/circuit-breaking.html?utm_source=chatgpt.com))

熔断降级和流量控制的区别如下：

| 类型     | 触发依据                   | 主要目的                     |
| -------- | -------------------------- | ---------------------------- |
| 流量控制 | 请求量、并发线程数         | 控制入口流量，防止服务被打爆 |
| 熔断降级 | 慢调用、异常比例、异常数量 | 隔离不稳定资源，防止故障扩散 |

### 慢调用比例熔断

慢调用比例熔断适合处理下游接口响应变慢的问题。Sentinel 会根据最大允许响应时间判断某次调用是否为慢调用，当统计周期内请求数达到最小请求数，且慢调用比例超过阈值时，资源会进入熔断状态。官方文档说明，慢调用比例策略需要设置响应时间上限，超过该 RT 的请求会被记录为慢调用。([Sentinel Guard](https://sentinelguard.io/en-us/docs/circuit-breaking.html?utm_source=chatgpt.com))

先补充熔断测试 Controller。

文件位置：`src/main/java/io/github/atengk/sentinel/controller/SentinelDegradeController.java`

```java
package io.github.atengk.sentinel.controller;

import io.github.atengk.sentinel.service.SentinelBusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sentinel 熔断降级测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SentinelDegradeController {

    private final SentinelBusinessService sentinelBusinessService;

    /**
     * 慢调用比例熔断测试接口
     *
     * @param productId 商品 ID
     * @param mockSlow 是否模拟慢调用
     * @return 响应数据
     */
    @GetMapping("/sentinel/degrade/slow")
    public Map<String, Object> slowRatio(Long productId, Boolean mockSlow) {
        log.info("访问慢调用比例熔断测试接口，商品 ID：{}", productId);
        return sentinelBusinessService.queryStock(productId, Boolean.TRUE.equals(mockSlow), false);
    }

    /**
     * 异常比例熔断测试接口
     *
     * @param productId 商品 ID
     * @param mockError 是否模拟异常
     * @return 响应数据
     */
    @GetMapping("/sentinel/degrade/error-ratio")
    public Map<String, Object> errorRatio(Long productId, Boolean mockError) {
        log.info("访问异常比例熔断测试接口，商品 ID：{}", productId);
        return sentinelBusinessService.queryStock(productId, false, Boolean.TRUE.equals(mockError));
    }

    /**
     * 异常数熔断测试接口
     *
     * @param productId 商品 ID
     * @param mockError 是否模拟异常
     * @return 响应数据
     */
    @GetMapping("/sentinel/degrade/error-count")
    public Map<String, Object> errorCount(Long productId, Boolean mockError) {
        log.info("访问异常数熔断测试接口，商品 ID：{}", productId);
        return sentinelBusinessService.queryStock(productId, false, Boolean.TRUE.equals(mockError));
    }
}
```

慢调用比例熔断规则示例：

```json
[
  {
    "resource": "stockQueryResource",
    "grade": 0,
    "count": 500,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 10000,
    "slowRatioThreshold": 0.6
  }
]
```

字段说明：

| 字段                 | 示例值               | 说明                  |
| -------------------- | -------------------- | --------------------- |
| `resource`           | `stockQueryResource` | 自定义资源名          |
| `grade`              | `0`                  | 慢调用比例熔断        |
| `count`              | `500`                | 最大允许 RT，单位毫秒 |
| `timeWindow`         | `10`                 | 熔断时长，单位秒      |
| `minRequestAmount`   | `5`                  | 触发熔断的最小请求数  |
| `statIntervalMs`     | `10000`              | 统计窗口，单位毫秒    |
| `slowRatioThreshold` | `0.6`                | 慢调用比例阈值，60%   |

验证命令：

```bash
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/sentinel/degrade/slow?productId=1001&mockSlow=true"
  echo
done
```

当慢调用比例达到规则阈值后，`stockQueryResource` 会进入熔断状态，后续请求不会进入真实业务方法，而是直接进入 `queryStockBlockHandler`。

### 异常比例熔断

异常比例熔断适合处理接口一段时间内错误率升高的问题。例如下游服务抖动、数据库连接异常、第三方接口不可用时，可以通过异常比例熔断减少无效调用。Sentinel 官方文档说明，异常比例策略会根据异常数与总完成数计算比例，超过阈值后触发熔断。([Sentinel Guard](https://sentinelguard.io/en-us/docs/circuit-breaking.html?utm_source=chatgpt.com))

异常比例熔断规则示例：

```json
[
  {
    "resource": "stockQueryResource",
    "grade": 1,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 10000
  }
]
```

字段说明：

| 字段               | 示例值               | 说明               |
| ------------------ | -------------------- | ------------------ |
| `resource`         | `stockQueryResource` | 自定义资源名       |
| `grade`            | `1`                  | 异常比例熔断       |
| `count`            | `0.5`                | 异常比例阈值，50%  |
| `timeWindow`       | `10`                 | 熔断时长，单位秒   |
| `minRequestAmount` | `5`                  | 最小请求数         |
| `statIntervalMs`   | `10000`              | 统计窗口，单位毫秒 |

验证命令：

```bash
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/sentinel/degrade/error-ratio?productId=1001&mockError=true"
  echo
done
```

需要注意，熔断统计的是资源调用过程中的业务异常。对于自定义埋点场景，如果异常被业务代码吞掉，Sentinel 可能无法统计到异常，导致异常比例熔断不触发。因此实际开发中不要在被保护方法内部无条件捕获并消化异常，除非你已经明确记录异常或返回了可被规则感知的结果。

### 异常数熔断

异常数熔断适合处理短时间内连续失败的问题。与异常比例相比，异常数更关注绝对异常次数，例如 10 秒内出现 5 次以上异常就触发熔断。

异常数熔断规则示例：

```json
[
  {
    "resource": "stockQueryResource",
    "grade": 2,
    "count": 5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 10000
  }
]
```

字段说明：

| 字段               | 示例值               | 说明               |
| ------------------ | -------------------- | ------------------ |
| `resource`         | `stockQueryResource` | 自定义资源名       |
| `grade`            | `2`                  | 异常数熔断         |
| `count`            | `5`                  | 异常数量阈值       |
| `timeWindow`       | `10`                 | 熔断时长，单位秒   |
| `minRequestAmount` | `5`                  | 最小请求数         |
| `statIntervalMs`   | `10000`              | 统计窗口，单位毫秒 |

验证命令：

```bash
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/sentinel/degrade/error-count?productId=1001&mockError=true"
  echo
done
```

异常数熔断适合失败次数少但影响严重的接口，例如支付渠道、库存扣减、账户扣款、第三方签约接口。异常比例熔断更适合访问量较大、错误率异常升高的接口。

### 降级返回处理

降级返回处理用于将 Sentinel 的限流、熔断和业务异常转换为前端或调用方可理解的业务响应。对于 `@SentinelResource` 自定义资源，常用处理方式是同时配置 `blockHandler` 和 `fallback`。其中 `blockHandler` 处理 Sentinel 的限流、熔断、系统保护等 `BlockException`；`fallback` 处理业务方法抛出的普通异常。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/annotation-support.html?utm_source=chatgpt.com))

建议区分两类返回：

| 类型          | 处理方法       | 建议状态码     | 说明                                 |
| ------------- | -------------- | -------------- | ------------------------------------ |
| Sentinel 阻塞 | `blockHandler` | `429`          | 请求过多、服务被保护、资源暂不可用   |
| 业务异常      | `fallback`     | `500` 或业务码 | 业务逻辑失败、下游调用失败、参数异常 |

如果多个资源需要复用同一套降级返回，可以将处理方法放到独立类中。

文件位置：`src/main/java/io/github/atengk/sentinel/handler/SentinelResourceHandler.java`

```java
package io.github.atengk.sentinel.handler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Sentinel 自定义资源统一处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
public final class SentinelResourceHandler {

    private SentinelResourceHandler() {
    }

    /**
     * 通用 Sentinel 阻塞处理
     *
     * @param resourceName 资源名称
     * @param ex Sentinel 阻塞异常
     * @return 阻塞响应数据
     */
    public static Map<String, Object> blockHandler(String resourceName, BlockException ex) {
        log.warn("资源触发 Sentinel 保护，资源名：{}，异常类型：{}", resourceName, ex.getClass().getSimpleName());
        return MapUtil.<String, Object>builder()
                .put("code", 429)
                .put("message", "请求过于频繁，请稍后重试")
                .put("resource", resourceName)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 通用业务异常降级处理
     *
     * @param resourceName 资源名称
     * @param ex 业务异常
     * @return 降级响应数据
     */
    public static Map<String, Object> fallbackHandler(String resourceName, Throwable ex) {
        log.error("资源发生业务异常，资源名：{}，异常信息：{}", resourceName, ex.getMessage());
        return MapUtil.<String, Object>builder()
                .put("code", 500)
                .put("message", "服务暂不可用，已返回降级结果")
                .put("resource", resourceName)
                .put("time", DateUtil.now())
                .build();
    }
}
```

不过要注意：`@SentinelResource` 的 `blockHandler` 和 `fallback` 方法签名必须与原方法匹配。因此通用处理器更适合参数固定或通过包装对象统一参数的场景。对于参数不一致的业务方法，建议在当前业务类中分别编写处理方法，避免签名不匹配导致处理方法不生效。

实际项目建议遵守以下规则：

| 建议                              | 说明                                                 |
| --------------------------------- | ---------------------------------------------------- |
| URL 资源用于入口限流              | 适合对接口整体流量做保护                             |
| 自定义资源用于核心方法            | 资源名稳定，不依赖 URL                               |
| `blockHandler` 处理 Sentinel 保护 | 限流、熔断、系统保护都属于阻塞异常                   |
| `fallback` 处理业务异常           | 下游异常、参数异常、运行时异常进入 fallback          |
| 降级返回要可观测                  | 日志中记录资源名、异常类型、关键业务参数             |
| 不要返回假成功                    | 降级结果应明确标识为兜底结果，避免调用方误判业务成功 |

完成本章后，项目已经具备入口限流、自定义资源限流、QPS 限流、并发线程数限流、慢调用熔断、异常比例熔断、异常数熔断和基础降级返回能力。后续可以继续补充热点参数限流、授权规则、系统保护规则和 Nacos 规则持久化。

## 热点参数限流

本节用于补齐热点参数限流开发内容。热点参数限流适合针对高频访问的参数值进行精细化保护，例如商品 ID、用户 ID、租户 ID、优惠券 ID、活动 ID 等。Sentinel 官方文档说明，热点参数限流会统计传入参数中的热点参数，并根据配置的阈值和模式对包含热点参数的资源调用进行限流；它可以看作一种特殊的流量控制，只对携带指定热点参数的资源调用生效。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/parameter-flow-control.html?utm_source=chatgpt.com)) 原始大纲中该章节位于「熔断降级开发」之后，用于继续完善接口级和参数级的流量治理能力。

### 热点参数资源定义

热点参数限流的关键不是只定义资源名，而是在进入 Sentinel 资源时把需要统计的参数传进去。Sentinel 官方文档中说明，使用热点参数限流时，需要通过 `SphU.entry(resourceName, EntryType.IN, count, args...)` 这类重载方法传入参数，后续规则中的 `paramIdx` 才能根据参数索引生效。([Sentinel Guard](https://sentinelguard.io/en-us/docs/parameter-flow-control.html?utm_source=chatgpt.com))

热点参数限流需要引入参数限流模块。部分 Spring Cloud Alibaba 版本可能已经传递引入，但建议显式声明，避免版本升级后依赖缺失。

文件位置：`pom.xml`

```xml
<!-- Sentinel 热点参数限流：用于按方法入参、业务参数值进行精细化限流 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-parameter-flow-control</artifactId>
</dependency>
```

下面定义一个商品详情查询接口，使用商品 ID 作为热点参数。资源名固定为 `productDetailHotspotResource`，后续规则会对该资源的第 0 个参数 `productId` 生效。

文件位置：`src/main/java/io/github/atengk/sentinel/controller/SentinelHotspotController.java`

```java
package io.github.atengk.sentinel.controller;

import io.github.atengk.sentinel.service.SentinelHotspotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sentinel 热点参数限流测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SentinelHotspotController {

    private final SentinelHotspotService sentinelHotspotService;

    /**
     * 商品详情热点参数限流测试接口
     *
     * @param productId 商品 ID
     * @param userId 用户 ID
     * @return 商品详情响应数据
     */
    @GetMapping("/sentinel/hotspot/product")
    public Map<String, Object> productDetail(Long productId, Long userId) {
        log.info("访问商品详情热点参数限流接口，商品ID：{}，用户ID：{}", productId, userId);
        return sentinelHotspotService.queryProductDetail(productId, userId);
    }
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/service/SentinelHotspotService.java`

```java
package io.github.atengk.sentinel.service;

import java.util.Map;

/**
 * Sentinel 热点参数限流业务服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface SentinelHotspotService {

    /**
     * 查询商品详情
     *
     * @param productId 商品 ID
     * @param userId 用户 ID
     * @return 商品详情响应数据
     */
    Map<String, Object> queryProductDetail(Long productId, Long userId);
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/service/impl/SentinelHotspotServiceImpl.java`

下面的实现类通过 `SphU.entry()` 显式传入 `productId` 和 `userId`，其中 `productId` 是第 0 个参数，`userId` 是第 1 个参数。

```java
package io.github.atengk.sentinel.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.github.atengk.sentinel.service.SentinelHotspotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sentinel 热点参数限流业务服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class SentinelHotspotServiceImpl implements SentinelHotspotService {

    private static final String PRODUCT_DETAIL_HOTSPOT_RESOURCE = "productDetailHotspotResource";

    /**
     * 查询商品详情
     *
     * @param productId 商品 ID
     * @param userId 用户 ID
     * @return 商品详情响应数据
     */
    @Override
    public Map<String, Object> queryProductDetail(Long productId, Long userId) {
        if (productId == null || NumberUtil.compare(productId, 0L) <= 0) {
            log.warn("查询商品详情失败，商品ID不合法：{}", productId);
            return buildErrorResult(400, "商品ID不合法", productId, userId);
        }

        Entry entry = null;
        try {
            // 显式传入热点参数：args[0] = productId，args[1] = userId
            entry = SphU.entry(PRODUCT_DETAIL_HOTSPOT_RESOURCE, EntryType.IN, 1, productId, userId);
            log.info("查询商品详情成功，商品ID：{}，用户ID：{}", productId, userId);

            return MapUtil.<String, Object>builder()
                    .put("code", 200)
                    .put("message", "商品详情查询成功")
                    .put("resource", PRODUCT_DETAIL_HOTSPOT_RESOURCE)
                    .put("productId", productId)
                    .put("userId", userId)
                    .put("productName", "Sentinel 实战课程")
                    .put("time", DateUtil.now())
                    .build();
        } catch (BlockException ex) {
            log.warn("商品详情触发热点参数限流，商品ID：{}，用户ID：{}，异常类型：{}",
                    productId, userId, ex.getClass().getSimpleName());
            return MapUtil.<String, Object>builder()
                    .put("code", 429)
                    .put("message", "当前商品访问过于频繁，请稍后重试")
                    .put("resource", PRODUCT_DETAIL_HOTSPOT_RESOURCE)
                    .put("productId", productId)
                    .put("userId", userId)
                    .put("time", DateUtil.now())
                    .build();
        } finally {
            if (entry != null) {
                entry.exit(1, productId, userId);
            }
        }
    }

    /**
     * 构建错误响应
     *
     * @param code 响应码
     * @param message 响应信息
     * @param productId 商品 ID
     * @param userId 用户 ID
     * @return 错误响应数据
     */
    private Map<String, Object> buildErrorResult(Integer code, String message, Long productId, Long userId) {
        return MapUtil.<String, Object>builder()
                .put("code", code)
                .put("message", message)
                .put("resource", PRODUCT_DETAIL_HOTSPOT_RESOURCE)
                .put("productId", productId)
                .put("userId", userId)
                .put("time", DateUtil.now())
                .build();
    }
}
```

这里使用手动 `SphU.entry()`，是为了明确传入热点参数列表。如果使用 `@SentinelResource` 注解方式，也可以基于方法参数进行热点参数限流，但在开发文档和排查问题时，手动传参更容易看清 `paramIdx` 与业务参数的对应关系。

### 参数索引配置

参数索引配置对应 Sentinel 热点参数规则中的 `paramIdx` 字段。官方文档说明，`paramIdx` 是热点参数索引，对应传入 `SphU.entry(xxx, args...)` 中的参数索引位置，并且是必填项。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/parameter-flow-control.html?utm_source=chatgpt.com))

在上面的代码中：

```java
SphU.entry(PRODUCT_DETAIL_HOTSPOT_RESOURCE, EntryType.IN, 1, productId, userId);
```

参数索引关系如下：

| 参数索引 | 参数名      | 业务含义 | 适合限流场景       |
| -------- | ----------- | -------- | ------------------ |
| `0`      | `productId` | 商品 ID  | 热门商品详情页限流 |
| `1`      | `userId`    | 用户 ID  | 高频用户访问限流   |

如果要限制同一个商品 ID 每秒最多访问 3 次，可以配置 `paramIdx=0`。

Nacos 数据源配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        hotspot-param-rule:
          nacos:
            # Nacos 服务地址
            server-addr: 127.0.0.1:8848
            # 热点参数规则 Data ID
            data-id: sentinel-demo-service-param-flow-rules
            # Nacos 配置分组
            group-id: DEFAULT_GROUP
            # 规则数据格式
            data-type: json
            # 规则类型：param-flow 表示热点参数限流规则
            rule-type: param-flow
```

Nacos 中配置热点参数规则：

```json
[
  {
    "resource": "productDetailHotspotResource",
    "count": 3,
    "grade": 1,
    "durationInSec": 1,
    "controlBehavior": 0,
    "maxQueueingTimeMs": 0,
    "paramIdx": 0,
    "clusterMode": false
  }
]
```

字段说明：

| 字段              | 示例值                         | 说明                           |
| ----------------- | ------------------------------ | ------------------------------ |
| `resource`        | `productDetailHotspotResource` | 热点参数资源名                 |
| `count`           | `3`                            | 每个热点参数值的限流阈值       |
| `grade`           | `1`                            | QPS 模式                       |
| `durationInSec`   | `1`                            | 统计窗口，单位秒               |
| `controlBehavior` | `0`                            | 快速失败                       |
| `paramIdx`        | `0`                            | 对第 0 个参数 `productId` 生效 |
| `clusterMode`     | `false`                        | 单机热点参数限流               |

验证命令：

```bash
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/sentinel/hotspot/product?productId=1001&userId=9001"
  echo
done
```

如果同一个 `productId=1001` 在 1 秒内访问超过 3 次，就会被热点参数规则拦截。

再访问另一个商品 ID：

```bash
for i in $(seq 1 5); do
  curl -s "http://127.0.0.1:8081/sentinel/hotspot/product?productId=2001&userId=9001"
  echo
done
```

热点参数限流是按参数值分别统计的。`productId=1001` 被限流，不代表 `productId=2001` 一定被限流。

### 参数例外项配置

参数例外项用于对指定参数值单独配置阈值。例如普通商品每秒最多访问 3 次，但某个首页推荐商品、活动商品或重点商品需要提高阈值到 20。Sentinel 官方文档说明，`paramFlowItemList` 可以针对指定参数值单独设置限流阈值，且仅支持基本类型和字符串类型。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/parameter-flow-control.html?utm_source=chatgpt.com))

参数例外项规则如下：

```json
[
  {
    "resource": "productDetailHotspotResource",
    "count": 3,
    "grade": 1,
    "durationInSec": 1,
    "controlBehavior": 0,
    "maxQueueingTimeMs": 0,
    "paramIdx": 0,
    "paramFlowItemList": [
      {
        "object": "1001",
        "classType": "long",
        "count": 20
      },
      {
        "object": "2001",
        "classType": "long",
        "count": 10
      }
    ],
    "clusterMode": false
  }
]
```

配置含义如下：

| 商品 ID  | 阈值 | 说明                       |
| -------- | ---- | -------------------------- |
| 其他商品 | `3`  | 使用全局热点参数阈值       |
| `1001`   | `20` | 热门推荐商品，提高访问阈值 |
| `2001`   | `10` | 活动商品，提高访问阈值     |

常见 `classType` 写法如下：

| Java 类型           | `classType` 示例               |
| ------------------- | ------------------------------ |
| `String`            | `java.lang.String`             |
| `Integer` / `int`   | `int` 或 `java.lang.Integer`   |
| `Long` / `long`     | `long` 或 `java.lang.Long`     |
| `Double` / `double` | `double` 或 `java.lang.Double` |

开发中需要注意：参数例外项的 `object` 是字符串形式，但 `classType` 必须和实际传入参数类型匹配。比如代码传入的是 `Long productId`，规则中写成 `java.lang.String`，例外项就可能无法命中。

验证普通商品：

```bash
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/sentinel/hotspot/product?productId=3001&userId=9001"
  echo
done
```

验证例外商品：

```bash
for i in $(seq 1 15); do
  curl -s "http://127.0.0.1:8081/sentinel/hotspot/product?productId=1001&userId=9001"
  echo
done
```

预期效果是 `productId=3001` 很快触发限流，而 `productId=1001` 因为配置了更高阈值，可以承载更多访问。

## 授权规则开发

本节用于补齐 Sentinel 授权规则开发内容。授权规则也称为来源访问控制或黑白名单控制，适合根据调用来源限制某个资源是否允许通过。Sentinel 官方文档说明，黑白名单根据资源的请求来源 `origin` 限制资源是否通过；白名单模式下只有指定来源可以通过，黑名单模式下指定来源不能通过。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/origin-authority-control.html?utm_source=chatgpt.com))

授权规则不是完整的权限系统。它适合做调用来源级别的流量治理，例如只允许内部系统调用某个接口，或阻止某个异常调用方继续访问。用户登录鉴权、菜单权限、数据权限仍应交给 Spring Security、Sa-Token、OAuth2 等组件处理。

### 黑白名单规则

Sentinel 授权规则的核心字段包括 `resource`、`limitApp` 和 `strategy`。官方文档说明，`resource` 是规则作用的资源名，`limitApp` 是黑名单或白名单来源列表，多个 origin 使用英文逗号分隔，`strategy` 用于指定白名单或黑名单模式。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/origin-authority-control.html?utm_source=chatgpt.com))

先定义一个需要授权控制的接口。

文件位置：`src/main/java/io/github/atengk/sentinel/controller/SentinelAuthorityController.java`

```java
package io.github.atengk.sentinel.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sentinel 授权规则测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class SentinelAuthorityController {

    /**
     * 内部报表接口，测试 Sentinel 来源访问控制
     *
     * @return 响应数据
     */
    @GetMapping("/sentinel/authority/report")
    public Map<String, Object> report() {
        log.info("访问 Sentinel 授权规则测试接口");
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "内部报表接口访问成功")
                .put("resource", "/sentinel/authority/report")
                .put("time", DateUtil.now())
                .build();
    }
}
```

白名单规则示例：

```json
[
  {
    "resource": "/sentinel/authority/report",
    "limitApp": "order-service,pay-service",
    "strategy": 0
  }
]
```

黑名单规则示例：

```json
[
  {
    "resource": "/sentinel/authority/report",
    "limitApp": "risk-service,test-client",
    "strategy": 1
  }
]
```

策略含义如下：

| `strategy` | 模式   | 说明                             |
| ---------- | ------ | -------------------------------- |
| `0`        | 白名单 | 只有 `limitApp` 中的来源允许访问 |
| `1`        | 黑名单 | `limitApp` 中的来源禁止访问      |

Nacos 授权规则数据源配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        authority-rule:
          nacos:
            # Nacos 服务地址
            server-addr: 127.0.0.1:8848
            # 授权规则 Data ID
            data-id: sentinel-demo-service-authority-rules
            # Nacos 配置分组
            group-id: DEFAULT_GROUP
            # 规则数据格式
            data-type: json
            # 规则类型：authority 表示授权规则
            rule-type: authority
```

白名单适合内部接口保护，例如只允许 `order-service` 和 `pay-service` 调用报表接口。黑名单适合临时阻断异常来源，例如某个客户端产生异常流量时快速阻断。

### 调用来源识别

授权规则能否生效，取决于 Sentinel 能否识别请求来源 `origin`。在 Web 场景中，通常通过实现 `RequestOriginParser` 从请求头、参数或网关转发信息中解析调用来源。

下面示例从请求头 `X-Client-Origin` 中读取来源。如果没有传递该请求头，则返回 `unknown`。这样可以避免来源为空导致规则行为不可控。

文件位置：`src/main/java/io/github/atengk/sentinel/config/SentinelOriginParserConfig.java`

```java
package io.github.atengk.sentinel.config;

import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.RequestOriginParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentinel 调用来源解析配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class SentinelOriginParserConfig {

    private static final String ORIGIN_HEADER = "X-Client-Origin";
    private static final String DEFAULT_ORIGIN = "unknown";

    /**
     * 解析 Sentinel 授权规则使用的调用来源
     *
     * @return 请求来源解析器
     */
    @Bean
    public RequestOriginParser requestOriginParser() {
        return this::parseOrigin;
    }

    /**
     * 从请求中解析调用来源
     *
     * @param request HTTP 请求
     * @return 调用来源
     */
    private String parseOrigin(HttpServletRequest request) {
        String origin = request.getHeader(ORIGIN_HEADER);
        if (StrUtil.isBlank(origin)) {
            log.warn("请求未携带调用来源请求头：{}", ORIGIN_HEADER);
            return DEFAULT_ORIGIN;
        }

        String cleanOrigin = StrUtil.trim(origin);
        log.info("解析 Sentinel 调用来源成功，origin：{}", cleanOrigin);
        return cleanOrigin;
    }
}
```

请求示例：

```bash
# 白名单来源，允许访问
curl -H "X-Client-Origin: order-service" \
  "http://127.0.0.1:8081/sentinel/authority/report"

# 非白名单来源，触发 Sentinel 授权拦截
curl -H "X-Client-Origin: test-client" \
  "http://127.0.0.1:8081/sentinel/authority/report"

# 未传来源，origin 会被解析为 unknown
curl "http://127.0.0.1:8081/sentinel/authority/report"
```

在微服务项目中，来源字段建议由网关统一写入，而不是完全信任客户端传入。例如 Spring Cloud Gateway 可以在认证后添加内部请求头：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: sentinel-demo-service
          uri: lb://sentinel-demo-service
          predicates:
            - Path=/sentinel/**
          filters:
            # 网关认证通过后写入调用来源，后端服务通过该请求头识别 origin
            - AddRequestHeader=X-Client-Origin,gateway-service
```

如果是服务间调用，可以由 OpenFeign 拦截器统一透传调用来源。下面示例为所有 Feign 请求添加 `X-Client-Origin`。

文件位置：`src/main/java/io/github/atengk/sentinel/config/FeignOriginHeaderConfig.java`

```java
package io.github.atengk.sentinel.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 调用来源请求头配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class FeignOriginHeaderConfig {

    private static final String ORIGIN_HEADER = "X-Client-Origin";
    private static final String CURRENT_SERVICE_NAME = "sentinel-demo-service";

    /**
     * 为 Feign 请求添加调用来源
     *
     * @return Feign 请求拦截器
     */
    @Bean
    public RequestInterceptor originRequestInterceptor() {
        return template -> {
            template.header(ORIGIN_HEADER, CURRENT_SERVICE_NAME);
            log.info("Feign 请求添加调用来源请求头，origin：{}", CURRENT_SERVICE_NAME);
        };
    }
}
```

使用请求头传递调用来源时，需要注意两个问题：第一，公网请求不能直接信任客户端伪造的 `X-Client-Origin`；第二，网关到后端之间应在可信网络内传递，必要时可以先移除外部请求中的同名请求头，再由网关重新写入。

### 授权异常处理

当请求来源不符合授权规则时，Sentinel 会抛出授权相关的 `BlockException`，常见类型是 `AuthorityException`。如果不做统一处理，默认返回可能不符合业务系统的 JSON 响应规范。实际项目中建议统一处理 Sentinel Web 层的限流、熔断、授权异常。

Spring MVC 场景中可以注册 `BlockExceptionHandler`，对 Sentinel 拦截异常进行统一返回。

文件位置：`src/main/java/io/github/atengk/sentinel/handler/SentinelBlockExceptionHandler.java`

```java
package io.github.atengk.sentinel.handler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Sentinel Web 层阻塞异常统一处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class SentinelBlockExceptionHandler implements BlockExceptionHandler {

    /**
     * 处理 Sentinel Web 层阻塞异常
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param resourceName 资源名称
     * @param ex Sentinel 阻塞异常
     * @throws Exception 响应写出异常
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       String resourceName,
                       BlockException ex) throws Exception {
        String message = resolveMessage(ex);
        int code = resolveCode(ex);

        log.warn("Sentinel Web 请求被拦截，资源：{}，路径：{}，异常类型：{}，提示：{}",
                resourceName, request.getRequestURI(), ex.getClass().getSimpleName(), message);

        Map<String, Object> result = MapUtil.<String, Object>builder()
                .put("code", code)
                .put("message", message)
                .put("path", request.getRequestURI())
                .put("resource", resourceName)
                .put("time", DateUtil.now())
                .build();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

    /**
     * 解析 Sentinel 异常对应业务码
     *
     * @param ex Sentinel 阻塞异常
     * @return 业务响应码
     */
    private int resolveCode(BlockException ex) {
        if (ex instanceof AuthorityException) {
            return 403;
        }
        if (ex instanceof FlowException || ex instanceof ParamFlowException) {
            return 429;
        }
        if (ex instanceof DegradeException) {
            return 503;
        }
        if (ex instanceof SystemBlockException) {
            return 503;
        }
        return 429;
    }

    /**
     * 解析 Sentinel 异常提示信息
     *
     * @param ex Sentinel 阻塞异常
     * @return 提示信息
     */
    private String resolveMessage(BlockException ex) {
        if (ex instanceof AuthorityException) {
            return "当前调用来源无权访问该资源";
        }
        if (ex instanceof ParamFlowException) {
            return "当前热点参数访问过于频繁，请稍后重试";
        }
        if (ex instanceof FlowException) {
            return "请求过于频繁，请稍后重试";
        }
        if (ex instanceof DegradeException) {
            return "服务暂不可用，已触发熔断降级";
        }
        if (ex instanceof SystemBlockException) {
            return "系统负载较高，已触发系统保护";
        }
        return "请求被 Sentinel 保护规则拦截";
    }
}
```

授权规则验证流程如下：

```bash
# 允许访问：order-service 在白名单中
curl -H "X-Client-Origin: order-service" \
  "http://127.0.0.1:8081/sentinel/authority/report"

# 禁止访问：test-client 不在白名单中
curl -H "X-Client-Origin: test-client" \
  "http://127.0.0.1:8081/sentinel/authority/report"

# 禁止访问：未传来源，解析为 unknown
curl "http://127.0.0.1:8081/sentinel/authority/report"
```

禁止访问时的响应示例：

```json
{
  "code": 403,
  "message": "当前调用来源无权访问该资源",
  "path": "/sentinel/authority/report",
  "resource": "/sentinel/authority/report",
  "time": "2026-05-05 10:30:00"
}
```

开发中建议将授权规则用于“服务来源治理”，不要替代用户认证与权限系统。比较稳妥的落地方式是：外部请求先经过网关认证，网关清洗并写入可信 `X-Client-Origin`，后端服务通过 `RequestOriginParser` 解析来源，Sentinel 授权规则再按来源进行黑白名单控制。

## 系统保护规则

本节用于补齐系统保护规则内容。系统保护规则不是针对单个 URL 或单个业务方法，而是从应用整体维度保护入口流量。Sentinel 官方文档说明，系统自适应保护会结合 Load、CPU 使用率、入口 QPS、平均 RT、并发线程数等指标控制入口流量，并且系统规则只对入口流量 `EntryType.IN` 生效。([Sentinel Guard](https://sentinelguard.io/en-us/docs/system-adaptive-protection.html?utm_source=chatgpt.com)) 原始大纲中该章节位于「授权规则开发」之后，用于继续完善应用整体稳定性保护能力。

系统保护规则适合放在应用入口侧，例如 Spring MVC 接口、Spring Cloud Gateway 入口、RPC Provider 入口。它不适合作为单个业务接口的精细限流手段；如果只需要限制某个接口，应优先使用流控规则、热点参数规则或熔断规则。

### 系统负载保护

系统负载保护基于系统 `load1` 指标进行判断。Sentinel 官方文档说明，Load 保护在 Linux/Unix-like 系统上生效，当系统 `load1` 超过阈值，并且当前并发线程数超过系统容量估算值时，才会触发系统保护；系统容量通常由 `maxQps * minRt` 估算，阈值参考值一般可从 `CPU cores * 2.5` 开始评估。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/system-adaptive-protection.html?utm_source=chatgpt.com))

Nacos 系统规则数据源配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        system-rule:
          nacos:
            # Nacos 服务地址
            server-addr: 127.0.0.1:8848
            # 系统保护规则 Data ID
            data-id: sentinel-demo-service-system-rules
            # Nacos 配置分组
            group-id: DEFAULT_GROUP
            # 规则数据格式
            data-type: json
            # 规则类型：system 表示系统保护规则
            rule-type: system
```

Nacos 中配置系统负载保护规则：

```json
[
  {
    "highestSystemLoad": 8.0
  }
]
```

配置说明：

| 字段                | 示例值          | 说明                                   |
| ------------------- | --------------- | -------------------------------------- |
| `highestSystemLoad` | `8.0`           | 系统 `load1` 触发阈值                  |
| 生效范围            | 入口流量        | 只对 `EntryType.IN` 入口资源生效       |
| 适用环境            | Linux/Unix-like | Windows 本地开发环境不适合作为验证依据 |

负载保护建议在生产环境谨慎启用。它适合用于高并发服务、核心交易服务、网关服务等入口压力较大的应用，但不建议在不了解机器规格、线程模型和真实吞吐能力的情况下直接配置过低阈值。

### RT 保护

RT 保护根据单机所有入口流量的平均响应时间进行系统级保护。Sentinel 官方文档说明，系统规则中的 RT 指标是针对单台机器所有入口流量的平均 RT，单位为毫秒。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/system-adaptive-protection.html?utm_source=chatgpt.com))

先提供一个用于测试系统 RT 保护的接口。

文件位置：`src/main/java/io/github/atengk/sentinel/controller/SentinelSystemController.java`

```java
package io.github.atengk.sentinel.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sentinel 系统保护规则测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class SentinelSystemController {

    /**
     * 系统 RT 保护测试接口
     *
     * @return 响应数据
     * @throws InterruptedException 线程休眠异常
     */
    @GetMapping("/sentinel/system/rt")
    public Map<String, Object> rt() throws InterruptedException {
        log.info("访问系统 RT 保护测试接口");
        Thread.sleep(1200L);
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "系统 RT 保护测试接口访问成功")
                .put("resource", "/sentinel/system/rt")
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 系统入口 QPS 保护测试接口
     *
     * @return 响应数据
     */
    @GetMapping("/sentinel/system/qps")
    public Map<String, Object> qps() {
        log.info("访问系统入口 QPS 保护测试接口");
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "系统入口 QPS 保护测试接口访问成功")
                .put("resource", "/sentinel/system/qps")
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 系统 CPU 使用率保护测试接口
     *
     * @return 响应数据
     */
    @GetMapping("/sentinel/system/cpu")
    public Map<String, Object> cpu() {
        log.info("访问系统 CPU 使用率保护测试接口");
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "系统 CPU 使用率保护测试接口访问成功")
                .put("resource", "/sentinel/system/cpu")
                .put("time", DateUtil.now())
                .build();
    }
}
```

RT 保护规则示例：

```json
[
  {
    "avgRt": 800
  }
]
```

字段说明：

| 字段     | 示例值           | 说明                                   |
| -------- | ---------------- | -------------------------------------- |
| `avgRt`  | `800`            | 单机所有入口流量平均 RT 阈值，单位毫秒 |
| 生效范围 | 全局入口流量     | 不是只对 `/sentinel/system/rt` 生效    |
| 适用场景 | 系统整体响应变慢 | 例如数据库变慢、线程堆积、下游服务变慢 |

验证命令：

```bash
seq 1 20 | xargs -I {} -P 10 curl -s "http://127.0.0.1:8081/sentinel/system/rt"
```

需要注意，系统 RT 保护是全局入口维度。测试 `/sentinel/system/rt` 只是为了制造慢请求，当全局平均 RT 达到阈值后，其他入口接口也可能受到影响。

### 入口 QPS 保护

入口 QPS 保护用于控制单台机器所有入口流量的总 QPS。Sentinel 官方文档说明，系统规则可以基于全局入口 QPS 进行保护，当单台机器所有入口流量 QPS 达到阈值后触发系统保护。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/system-adaptive-protection.html?utm_source=chatgpt.com))

入口 QPS 规则示例：

```json
[
  {
    "qps": 100
  }
]
```

配置说明：

| 字段     | 示例值             | 说明                                 |
| -------- | ------------------ | ------------------------------------ |
| `qps`    | `100`              | 单机入口总 QPS 阈值                  |
| 统计范围 | 所有入口资源       | 包含当前应用所有 `EntryType.IN` 流量 |
| 适用场景 | 应用总入口流量保护 | 适合网关、BFF、核心服务入口          |

本地验证可以将阈值临时调低，例如 `qps=5`：

```json
[
  {
    "qps": 5
  }
]
```

验证命令：

```bash
seq 1 50 | xargs -I {} -P 20 curl -s "http://127.0.0.1:8081/sentinel/system/qps"
```

入口 QPS 保护和普通流控规则的区别如下：

| 类型          | 控制范围     | 使用建议                        |
| ------------- | ------------ | ------------------------------- |
| 普通 QPS 限流 | 单个资源     | 控制某个 URL、方法或 Feign 资源 |
| 系统入口 QPS  | 整个应用入口 | 控制当前服务整体入口流量        |

如果只是某个接口访问量过高，应使用普通流控规则；如果整个应用入口流量超过机器承载能力，才适合使用系统入口 QPS 保护。

### CPU 使用率保护

CPU 使用率保护用于在系统 CPU 使用率过高时限制入口流量。Sentinel 官方文档说明，CPU usage 规则从 Sentinel 1.5.0 开始支持，阈值取值范围是 `0.0` 到 `1.0`，当系统 CPU 使用率超过阈值时触发系统保护。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/system-adaptive-protection.html?utm_source=chatgpt.com))

CPU 使用率保护规则示例：

```json
[
  {
    "highestCpuUsage": 0.85
  }
]
```

字段说明：

| 字段              | 示例值        | 说明                     |
| ----------------- | ------------- | ------------------------ |
| `highestCpuUsage` | `0.85`        | CPU 使用率阈值，表示 85% |
| 取值范围          | `0.0` - `1.0` | `0.9` 表示 90%           |
| 生效范围          | 应用入口流量  | 对入口流量进行系统保护   |

CPU 使用率保护适合 CPU 密集型服务，例如报表计算、复杂规则计算、图片处理、数据转换、加解密服务等。对于 IO 密集型服务，CPU 使用率可能不高，但线程、RT 或入口 QPS 已经出现问题，这时应结合 RT、线程数或 QPS 规则判断。

系统保护规则建议如下：

| 规则       | 适合场景                   | 注意事项               |
| ---------- | -------------------------- | ---------------------- |
| Load       | Linux 生产环境整机负载保护 | 本地开发环境不适合验证 |
| RT         | 全局响应时间升高           | 可能影响所有入口接口   |
| 入口 QPS   | 总入口流量过高             | 不适合替代单接口限流   |
| CPU 使用率 | CPU 密集型服务             | 阈值不要设置过低       |
| 线程数     | 慢接口导致线程堆积         | 可与 RT 规则配合使用   |

生产环境不要一次性启用多个过低阈值的系统规则。建议先观测 Dashboard、Prometheus 或日志平台中的真实峰值，再逐步启用规则。

## 异常与兜底处理

本节用于补齐 Sentinel 异常与兜底处理内容，重点解决四个问题：限流或熔断后如何返回统一 JSON、Controller 层如何统一处理 Web 资源限流、OpenFeign 调用失败如何降级、业务异常和 Sentinel 保护异常如何区分。原始大纲中该章节位于「系统保护规则」之后，是把前面所有规则落到业务可用形态的关键章节。

Sentinel 中常见的保护异常都继承自 `BlockException`，例如流控异常、热点参数限流异常、熔断异常、授权异常、系统保护异常。业务代码抛出的普通异常不属于 `BlockException`，应由业务异常处理器或 `@SentinelResource` 的 `fallback` 处理。

### BlockException 处理

`BlockException` 表示请求被 Sentinel 保护规则拦截。它不是业务失败，而是运行时保护动作。常见类型如下：

| 异常类型               | 触发场景         | 建议业务码 |
| ---------------------- | ---------------- | ---------- |
| `FlowException`        | 普通流控规则触发 | `429`      |
| `ParamFlowException`   | 热点参数限流触发 | `429`      |
| `DegradeException`     | 熔断降级触发     | `503`      |
| `AuthorityException`   | 授权规则拦截     | `403`      |
| `SystemBlockException` | 系统保护规则触发 | `503`      |

对于 `@SentinelResource` 自定义资源，建议使用 `blockHandler` 处理 `BlockException`；对于 Spring MVC URL 自动资源，建议使用 `BlockExceptionHandler` 统一处理。

下面是 `@SentinelResource` 的处理方式示例。

文件位置：`src/main/java/io/github/atengk/sentinel/service/impl/SentinelProtectServiceImpl.java`

```java
package io.github.atengk.sentinel.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sentinel 自定义资源保护示例服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class SentinelProtectServiceImpl {

    /**
     * 查询账户余额
     *
     * @param accountNo 账户号
     * @param mockError 是否模拟业务异常
     * @return 账户余额响应数据
     */
    @SentinelResource(
            value = "accountBalanceResource",
            blockHandler = "queryBalanceBlockHandler",
            fallback = "queryBalanceFallback"
    )
    public Map<String, Object> queryBalance(String accountNo, Boolean mockError) {
        if (StrUtil.isBlank(accountNo)) {
            log.warn("查询账户余额失败，账户号为空");
            throw new IllegalArgumentException("账户号不能为空");
        }

        if (Boolean.TRUE.equals(mockError)) {
            log.warn("查询账户余额模拟业务异常，账户号：{}", accountNo);
            throw new IllegalStateException("模拟账户余额查询异常");
        }

        log.info("查询账户余额成功，账户号：{}", accountNo);
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "账户余额查询成功")
                .put("resource", "accountBalanceResource")
                .put("accountNo", accountNo)
                .put("balance", "1000.00")
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * Sentinel 保护异常处理
     *
     * @param accountNo 账户号
     * @param mockError 是否模拟业务异常
     * @param ex Sentinel 阻塞异常
     * @return 保护响应数据
     */
    public Map<String, Object> queryBalanceBlockHandler(String accountNo, Boolean mockError, BlockException ex) {
        log.warn("账户余额查询触发 Sentinel 保护，账户号：{}，异常类型：{}",
                accountNo, ex.getClass().getSimpleName());
        return MapUtil.<String, Object>builder()
                .put("code", 429)
                .put("message", "账户余额查询过于频繁，请稍后重试")
                .put("resource", "accountBalanceResource")
                .put("accountNo", accountNo)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 业务异常降级处理
     *
     * @param accountNo 账户号
     * @param mockError 是否模拟业务异常
     * @param ex 业务异常
     * @return 降级响应数据
     */
    public Map<String, Object> queryBalanceFallback(String accountNo, Boolean mockError, Throwable ex) {
        log.error("账户余额查询发生业务异常，账户号：{}，异常信息：{}", accountNo, ex.getMessage());
        return MapUtil.<String, Object>builder()
                .put("code", 500)
                .put("message", "账户余额查询失败，已返回降级结果")
                .put("resource", "accountBalanceResource")
                .put("accountNo", accountNo)
                .put("balance", "0.00")
                .put("time", DateUtil.now())
                .build();
    }
}
```

需要注意：`blockHandler` 只处理 Sentinel 保护异常；`fallback` 处理业务异常。二者不要混用，否则日志、监控和接口语义会变得混乱。

### Controller 层统一限流返回

Controller 层统一限流返回主要处理 Spring MVC 自动埋点的 URL 资源，例如 `/sentinel/flow/url`、`/sentinel/system/qps`、`/sentinel/authority/report`。这类资源通常没有手动写 `@SentinelResource`，因此需要通过 `BlockExceptionHandler` 统一处理。

Spring Cloud Alibaba Sentinel 基于 Spring WebMVC 适配器提供 Web 层回调能力，Dashboard 和 Web 自动资源场景中常用该方式处理 URL 资源拦截返回。Sentinel 官方系统保护文档也说明系统规则只对入口流量生效，而 Spring MVC URL 请求正是典型入口流量。([Sentinel Guard](https://sentinelguard.io/en-us/docs/system-adaptive-protection.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/sentinel/handler/SentinelBlockExceptionHandler.java`

下面的处理器将不同 Sentinel 异常统一转换成 JSON 响应。

```java
package io.github.atengk.sentinel.handler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Sentinel Controller 层阻塞异常统一处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class SentinelBlockExceptionHandler implements BlockExceptionHandler {

    /**
     * 处理 Sentinel Web 层阻塞异常
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param resourceName Sentinel 资源名称
     * @param ex Sentinel 阻塞异常
     * @throws Exception 响应写出异常
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       String resourceName,
                       BlockException ex) throws Exception {
        Integer code = resolveCode(ex);
        String message = resolveMessage(ex);

        log.warn("Sentinel 拦截 Controller 请求，路径：{}，资源：{}，异常类型：{}，提示：{}",
                request.getRequestURI(), resourceName, ex.getClass().getSimpleName(), message);

        Map<String, Object> result = MapUtil.<String, Object>builder()
                .put("code", code)
                .put("message", message)
                .put("path", request.getRequestURI())
                .put("resource", resourceName)
                .put("exception", ex.getClass().getSimpleName())
                .put("time", DateUtil.now())
                .build();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }

    /**
     * 解析 Sentinel 异常对应业务码
     *
     * @param ex Sentinel 阻塞异常
     * @return 业务响应码
     */
    private Integer resolveCode(BlockException ex) {
        if (ex instanceof AuthorityException) {
            return 403;
        }
        if (ex instanceof DegradeException || ex instanceof SystemBlockException) {
            return 503;
        }
        if (ex instanceof FlowException || ex instanceof ParamFlowException) {
            return 429;
        }
        return 429;
    }

    /**
     * 解析 Sentinel 异常提示信息
     *
     * @param ex Sentinel 阻塞异常
     * @return 提示信息
     */
    private String resolveMessage(BlockException ex) {
        if (ex instanceof AuthorityException) {
            return "当前调用来源无权访问该资源";
        }
        if (ex instanceof ParamFlowException) {
            return "当前热点参数访问过于频繁，请稍后重试";
        }
        if (ex instanceof FlowException) {
            return "请求过于频繁，请稍后重试";
        }
        if (ex instanceof DegradeException) {
            return "服务暂不可用，已触发熔断降级";
        }
        if (ex instanceof SystemBlockException) {
            return "系统负载较高，已触发系统保护";
        }
        return "请求被 Sentinel 保护规则拦截";
    }
}
```

验证方式：

```bash
# 触发普通 URL 限流
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/sentinel/flow/url"
  echo
done

# 触发系统入口 QPS 保护
seq 1 50 | xargs -I {} -P 20 curl -s "http://127.0.0.1:8081/sentinel/system/qps"
```

返回示例：

```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后重试",
  "path": "/sentinel/flow/url",
  "resource": "/sentinel/flow/url",
  "exception": "FlowException",
  "time": "2026-05-05 10:30:00"
}
```

这里的 HTTP 状态码示例统一返回 `200`，业务码放在 `code` 字段中。如果公司 API 规范要求 HTTP 状态码与业务语义一致，也可以将限流返回改为 `429`、授权返回改为 `403`、熔断或系统保护返回改为 `503`。

### OpenFeign 降级处理

OpenFeign 降级用于处理服务间调用失败、超时、熔断和限流。Spring Cloud OpenFeign 官方文档说明，Feign 是声明式 REST 客户端，启用 fallback 时可以在 `@FeignClient` 上配置 `fallback`；如果需要获取触发 fallback 的异常原因，可以使用 `fallbackFactory`。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com)) Spring Cloud Alibaba Sentinel 文档说明，Feign 接入 Sentinel 需要开启 `feign.sentinel.enabled=true`，并引入 `spring-cloud-starter-openfeign`。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/wiki/Sentinel-en?utm_source=chatgpt.com))

先引入 OpenFeign 依赖。

文件位置：`pom.xml`

```xml
<!-- Spring Cloud OpenFeign：用于声明式服务间 HTTP 调用 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

开启 Feign Sentinel 支持：

文件位置：`src/main/resources/application.yml`

```yaml
feign:
  sentinel:
    # 开启 Sentinel 对 OpenFeign 的支持
    enabled: true

spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            # Feign 连接超时时间，单位毫秒
            connectTimeout: 3000
            # Feign 读取超时时间，单位毫秒
            readTimeout: 5000
            # Feign 日志级别，开发阶段可使用 basic
            loggerLevel: basic
```

启动类开启 Feign 扫描。

文件位置：`src/main/java/io/github/atengk/sentinel/SentinelDemoApplication.java`

```java
package io.github.atengk.sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Sentinel 示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@EnableFeignClients
@SpringBootApplication
public class SentinelDemoApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SentinelDemoApplication.class, args);
    }
}
```

定义 Feign 客户端。这里使用 `fallbackFactory`，方便在降级时记录真实异常原因。

文件位置：`src/main/java/io/github/atengk/sentinel/feign/ProductRemoteClient.java`

```java
package io.github.atengk.sentinel.feign;

import io.github.atengk.sentinel.feign.fallback.ProductRemoteFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 商品服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(
        name = "product-service",
        path = "/product",
        fallbackFactory = ProductRemoteFallbackFactory.class
)
public interface ProductRemoteClient {

    /**
     * 查询商品详情
     *
     * @param productId 商品 ID
     * @return 商品详情响应数据
     */
    @GetMapping("/detail")
    Map<String, Object> detail(@RequestParam("productId") Long productId);
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/feign/fallback/ProductRemoteFallbackFactory.java`

下面的 fallback 工厂用于处理商品服务调用失败、超时、熔断或限流后的降级返回。

```java
package io.github.atengk.sentinel.feign.fallback;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import io.github.atengk.sentinel.feign.ProductRemoteClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 商品服务 Feign 降级工厂
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class ProductRemoteFallbackFactory implements FallbackFactory<ProductRemoteClient> {

    /**
     * 创建商品服务降级客户端
     *
     * @param cause 降级原因
     * @return 商品服务降级客户端
     */
    @Override
    public ProductRemoteClient create(Throwable cause) {
        return new ProductRemoteClient() {

            /**
             * 商品详情降级返回
             *
             * @param productId 商品 ID
             * @return 降级响应数据
             */
            @Override
            public Map<String, Object> detail(Long productId) {
                log.error("调用商品服务失败，商品ID：{}，异常类型：{}，异常信息：{}",
                        productId,
                        cause.getClass().getSimpleName(),
                        cause.getMessage());

                return MapUtil.<String, Object>builder()
                        .put("code", 503)
                        .put("message", "商品服务暂不可用，已返回降级结果")
                        .put("productId", productId)
                        .put("productName", "默认商品")
                        .put("fallback", true)
                        .put("time", DateUtil.now())
                        .build();
            }
        };
    }
}
```

提供一个调用 Feign 的接口，便于验证降级效果。

文件位置：`src/main/java/io/github/atengk/sentinel/controller/SentinelFeignController.java`

```java
package io.github.atengk.sentinel.controller;

import io.github.atengk.sentinel.feign.ProductRemoteClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sentinel OpenFeign 降级测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SentinelFeignController {

    private final ProductRemoteClient productRemoteClient;

    /**
     * 测试 Feign 调用商品服务
     *
     * @param productId 商品 ID
     * @return 商品服务响应数据
     */
    @GetMapping("/sentinel/feign/product")
    public Map<String, Object> product(Long productId) {
        log.info("开始通过 Feign 调用商品服务，商品ID：{}", productId);
        return productRemoteClient.detail(productId);
    }
}
```

验证方式：

```bash
# 如果 product-service 未启动，应该进入 Feign fallbackFactory
curl "http://127.0.0.1:8081/sentinel/feign/product?productId=1001"
```

返回示例：

```json
{
  "code": 503,
  "message": "商品服务暂不可用，已返回降级结果",
  "productId": 1001,
  "productName": "默认商品",
  "fallback": true,
  "time": "2026-05-05 10:30:00"
}
```

OpenFeign 降级建议使用 `fallbackFactory` 而不是简单 `fallback`，因为 `fallbackFactory` 可以获取异常原因，便于区分超时、连接失败、熔断、限流等不同问题。

### 业务异常与限流异常区分

业务异常和 Sentinel 限流异常必须明确区分。业务异常通常代表参数错误、数据不存在、业务状态不允许、数据库异常或下游服务异常；限流异常代表请求被 Sentinel 规则保护拦截。二者的日志级别、响应码、监控指标和告警策略都不同。

建议定义业务异常类，并使用 Spring MVC 全局异常处理器处理普通业务异常。这样可以避免把业务异常误认为 Sentinel 限流。

文件位置：`src/main/java/io/github/atengk/sentinel/exception/BusinessException.java`

```java
package io.github.atengk.sentinel.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    /**
     * 创建业务异常
     *
     * @param code 业务码
     * @param message 异常信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/handler/GlobalExceptionHandler.java`

下面的全局异常处理器只处理业务异常和未知异常，不处理 Sentinel 的 `BlockException`。Web 层 Sentinel 拦截由前面的 `SentinelBlockExceptionHandler` 负责。

```java
package io.github.atengk.sentinel.handler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import io.github.atengk.sentinel.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局业务异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param request HTTP 请求
     * @param ex 业务异常
     * @return 业务异常响应
     */
    @ExceptionHandler(BusinessException.class)
    public Map<String, Object> handleBusinessException(HttpServletRequest request, BusinessException ex) {
        log.warn("业务异常，路径：{}，业务码：{}，异常信息：{}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());

        return MapUtil.<String, Object>builder()
                .put("code", ex.getCode())
                .put("message", ex.getMessage())
                .put("path", request.getRequestURI())
                .put("type", "BUSINESS_ERROR")
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 处理未知系统异常
     *
     * @param request HTTP 请求
     * @param ex 系统异常
     * @return 系统异常响应
     */
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(HttpServletRequest request, Exception ex) {
        log.error("系统异常，路径：{}，异常类型：{}，异常信息：{}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage());

        return MapUtil.<String, Object>builder()
                .put("code", 500)
                .put("message", "系统异常，请稍后重试")
                .put("path", request.getRequestURI())
                .put("type", "SYSTEM_ERROR")
                .put("time", DateUtil.now())
                .build();
    }
}
```

提供一个业务异常测试接口。

文件位置：`src/main/java/io/github/atengk/sentinel/controller/SentinelExceptionController.java`

```java
package io.github.atengk.sentinel.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.sentinel.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sentinel 异常区分测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class SentinelExceptionController {

    /**
     * 测试业务异常
     *
     * @param orderNo 订单号
     * @return 响应数据
     */
    @GetMapping("/sentinel/exception/business")
    public Map<String, Object> business(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            log.warn("订单号为空，触发业务异常");
            throw new BusinessException(400, "订单号不能为空");
        }

        log.info("业务异常区分测试接口访问成功，订单号：{}", orderNo);
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "访问成功")
                .put("orderNo", orderNo)
                .put("time", DateUtil.now())
                .build();
    }
}
```

验证业务异常：

```bash
curl "http://127.0.0.1:8081/sentinel/exception/business"
```

返回示例：

```json
{
  "code": 400,
  "message": "订单号不能为空",
  "path": "/sentinel/exception/business",
  "type": "BUSINESS_ERROR",
  "time": "2026-05-05 10:30:00"
}
```

业务异常与限流异常的处理边界如下：

| 类型          | 示例                           | 处理器                                                | 返回类型                    |
| ------------- | ------------------------------ | ----------------------------------------------------- | --------------------------- |
| 普通 URL 限流 | `/sentinel/flow/url` 超过 QPS  | `SentinelBlockExceptionHandler`                       | `type` 可省略，`code=429`   |
| 热点参数限流  | 商品 ID 访问过于频繁           | `BlockExceptionHandler` 或手动 `catch BlockException` | `code=429`                  |
| 熔断降级      | 下游服务慢调用或异常比例过高   | `blockHandler` 或 `BlockExceptionHandler`             | `code=503`                  |
| 授权拦截      | 来源不在白名单                 | `SentinelBlockExceptionHandler`                       | `code=403`                  |
| 业务异常      | 订单号为空、库存不足、状态错误 | `GlobalExceptionHandler`                              | 业务自定义码                |
| Feign 降级    | 远程服务不可用或超时           | `fallbackFactory`                                     | `code=503`，`fallback=true` |

最终建议是：Sentinel 保护异常统一走 Sentinel 处理链，业务异常统一走业务异常处理链，远程调用失败统一走 Feign fallback。三者不要互相吞异常，也不要全部返回同一个“系统繁忙”，否则后续排查限流、熔断、业务失败和远程调用失败时会缺少明确证据。

## 规则持久化

本节用于补齐 Sentinel 规则持久化内容。Sentinel Dashboard 手动配置的规则默认偏运行时管理，应用重启后容易丢失；生产环境通常需要将规则存储到 Nacos、Apollo、ZooKeeper、文件或自定义配置中心中。Sentinel 官方动态规则文档也说明，生产环境一般不建议只通过 API 硬编码加载规则，而应通过动态规则源管理规则；Spring Cloud Alibaba Sentinel 数据源支持 `flow`、`degrade`、`authority`、`system`、`param-flow`、`gw-flow`、`gw-api-group` 等规则类型。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/dynamic-rule-configuration.html?utm_source=chatgpt.com)) 原始大纲中该章节位于「异常与兜底处理」之后，用于将前面已开发的流控、熔断、热点参数等规则落到可动态维护的配置中心。

### Nacos 持久化方案

Nacos 持久化方案的核心思路是：业务应用启动时从 Nacos 读取 Sentinel 规则，Nacos 配置变更后客户端自动监听并刷新本地规则。需要注意，这种方式解决的是“客户端从 Nacos 动态读取规则”，并不等于原版 Sentinel Dashboard 修改规则后会自动写回 Nacos。若要做到 Dashboard 修改规则并持久化到 Nacos，需要使用改造版 Dashboard 或自行扩展 Dashboard 的规则发布逻辑。Sentinel 官方动态规则文档推荐的生产形态是控制台将规则推送到统一规则中心，客户端监听规则中心实时获取变更。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/dynamic-rule-configuration.html?utm_source=chatgpt.com))

规则持久化建议按规则类型拆分 Data ID，避免所有规则混在一个配置文件中难以维护。

| 规则类型     | Nacos Data ID 示例                       | `rule-type`  |
| ------------ | ---------------------------------------- | ------------ |
| 流控规则     | `sentinel-demo-service-flow-rules`       | `flow`       |
| 熔断规则     | `sentinel-demo-service-degrade-rules`    | `degrade`    |
| 热点参数规则 | `sentinel-demo-service-param-flow-rules` | `param-flow` |
| 授权规则     | `sentinel-demo-service-authority-rules`  | `authority`  |
| 系统规则     | `sentinel-demo-service-system-rules`     | `system`     |

先引入 Nacos 配置中心和 Sentinel Nacos 数据源依赖。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Nacos Config：用于读取应用配置和环境配置 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>

    <!-- Nacos Discovery：用于服务注册发现，微服务项目通常需要 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- Sentinel Nacos 数据源：用于从 Nacos 动态读取 Sentinel 规则 -->
    <dependency>
        <groupId>com.alibaba.csp</groupId>
        <artifactId>sentinel-datasource-nacos</artifactId>
    </dependency>
</dependencies>
```

统一配置 Nacos 和 Sentinel 数据源。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 当前服务端口
  port: 8081

spring:
  application:
    # 当前服务名称，建议与 Nacos 服务名、Sentinel Dashboard 应用名保持一致
    name: sentinel-demo-service

  cloud:
    nacos:
      discovery:
        # Nacos 注册中心地址
        server-addr: 127.0.0.1:8848
        # 命名空间 ID，开发环境可为空，生产建议按环境隔离
        namespace:
        # 服务分组
        group: DEFAULT_GROUP
      config:
        # Nacos 配置中心地址
        server-addr: 127.0.0.1:8848
        # Nacos 配置文件格式
        file-extension: yaml
        # 命名空间 ID，需要与 Nacos 控制台保持一致
        namespace:
        # 配置分组
        group: DEFAULT_GROUP

    sentinel:
      transport:
        # Sentinel Dashboard 地址
        dashboard: 127.0.0.1:8080
        # 当前应用暴露给 Dashboard 通信的端口
        port: 8719

      # 应用启动时立即初始化 Sentinel 客户端
      eager: true

      # 关闭 Web Context 统一入口，便于 URL 资源精细化统计
      web-context-unify: false

      datasource:
        flow-rule:
          nacos:
            # Nacos 地址
            server-addr: 127.0.0.1:8848
            # 流控规则 Data ID
            data-id: sentinel-demo-service-flow-rules
            # Nacos 分组
            group-id: DEFAULT_GROUP
            # 规则格式，常用 json
            data-type: json
            # 规则类型：流控规则
            rule-type: flow

        degrade-rule:
          nacos:
            # Nacos 地址
            server-addr: 127.0.0.1:8848
            # 熔断降级规则 Data ID
            data-id: sentinel-demo-service-degrade-rules
            # Nacos 分组
            group-id: DEFAULT_GROUP
            # 规则格式
            data-type: json
            # 规则类型：熔断降级规则
            rule-type: degrade

        param-flow-rule:
          nacos:
            # Nacos 地址
            server-addr: 127.0.0.1:8848
            # 热点参数规则 Data ID
            data-id: sentinel-demo-service-param-flow-rules
            # Nacos 分组
            group-id: DEFAULT_GROUP
            # 规则格式
            data-type: json
            # 规则类型：热点参数规则
            rule-type: param-flow
```

Spring Cloud Alibaba Sentinel 数据源配置中，`data-type` 表示规则内容格式，内置支持 `json` 和 `xml`；`rule-type` 表示数据源中的规则类型，例如 `flow`、`degrade`、`authority`、`system`、`param-flow`。如果某个数据源规则加载失败，通常不会阻断应用启动，而是在日志中输出错误信息，因此生产环境需要配合启动日志检查和规则加载验证。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/wiki/sentinel?utm_source=chatgpt.com))

### 流控规则配置

流控规则用于控制资源的 QPS 或并发线程数。常用字段包括 `resource`、`limitApp`、`grade`、`count`、`strategy`、`controlBehavior`、`clusterMode`。其中 `grade=1` 表示 QPS 限流，`grade=0` 表示并发线程数限流。

Nacos 配置项：

```text
Data ID：sentinel-demo-service-flow-rules
Group：DEFAULT_GROUP
配置格式：JSON
```

流控规则示例：

```json
[
  {
    "resource": "/example/flow/qps",
    "limitApp": "default",
    "grade": 1,
    "count": 2,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  },
  {
    "resource": "/example/flow/thread",
    "limitApp": "default",
    "grade": 0,
    "count": 2,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  },
  {
    "resource": "orderSubmitResource",
    "limitApp": "default",
    "grade": 1,
    "count": 5,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

字段说明：

| 字段              | 说明                                              |
| ----------------- | ------------------------------------------------- |
| `resource`        | Sentinel 资源名，可以是 URL，也可以是自定义资源名 |
| `limitApp`        | 调用来源，`default` 表示不区分来源                |
| `grade`           | 阈值类型，`1` 表示 QPS，`0` 表示并发线程数        |
| `count`           | 单机阈值                                          |
| `strategy`        | 流控模式，`0` 表示直接模式                        |
| `controlBehavior` | 流控效果，`0` 表示快速失败                        |
| `clusterMode`     | 是否启用集群限流                                  |

验证流控规则是否加载，可以启动应用后访问接口，并在 Dashboard 的“流控规则”或“簇点链路”中查看资源和规则。

```bash
# 验证 URL QPS 限流
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/flow/qps"
  echo
done

# 验证并发线程数限流
seq 1 10 | xargs -I {} -P 10 curl -s "http://127.0.0.1:8081/example/flow/thread"
```

### 熔断规则配置

熔断规则用于在资源出现慢调用、异常比例升高或异常数达到阈值时，临时切断请求并进入降级逻辑。Sentinel 1.8 之后的熔断策略主要包括慢调用比例、异常比例和异常数。([Sentinel Guard](https://sentinelguard.io/zh-cn/docs/dynamic-rule-configuration.html?utm_source=chatgpt.com))

Nacos 配置项：

```text
Data ID：sentinel-demo-service-degrade-rules
Group：DEFAULT_GROUP
配置格式：JSON
```

熔断规则示例：

```json
[
  {
    "resource": "stockQueryResource",
    "grade": 0,
    "count": 500,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 10000,
    "slowRatioThreshold": 0.6
  },
  {
    "resource": "remoteProductResource",
    "grade": 1,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 10000
  },
  {
    "resource": "remotePayResource",
    "grade": 2,
    "count": 5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 10000
  }
]
```

字段说明：

| 字段                 | 说明                                               |
| -------------------- | -------------------------------------------------- |
| `resource`           | 熔断资源名                                         |
| `grade`              | 熔断策略，`0` 慢调用比例，`1` 异常比例，`2` 异常数 |
| `count`              | 慢调用 RT 阈值、异常比例阈值或异常数阈值           |
| `timeWindow`         | 熔断时长，单位秒                                   |
| `minRequestAmount`   | 触发熔断的最小请求数                               |
| `statIntervalMs`     | 统计窗口，单位毫秒                                 |
| `slowRatioThreshold` | 慢调用比例阈值，仅慢调用比例策略使用               |

验证熔断规则：

```bash
# 验证慢调用比例熔断
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/degrade/stock?productId=1001&mockSlow=true"
  echo
done

# 验证异常比例熔断
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/degrade/stock?productId=1001&mockError=true"
  echo
done
```

慢调用比例规则适合下游响应变慢的场景；异常比例适合错误率突然升高的场景；异常数适合短时间连续失败且失败次数本身就需要快速阻断的场景。

### 热点参数规则配置

热点参数规则用于针对指定参数位置的具体参数值进行限流。常见场景包括商品详情、活动详情、用户维度查询、租户维度请求、优惠券领取等。热点参数规则的数据源类型为 `param-flow`，对应 Spring Cloud Alibaba Sentinel 数据源中的规则类型。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/wiki/Sentinel-en?utm_source=chatgpt.com))

Nacos 配置项：

```text
Data ID：sentinel-demo-service-param-flow-rules
Group：DEFAULT_GROUP
配置格式：JSON
```

热点参数规则示例：

```json
[
  {
    "resource": "productDetailHotspotResource",
    "count": 3,
    "grade": 1,
    "durationInSec": 1,
    "controlBehavior": 0,
    "maxQueueingTimeMs": 0,
    "paramIdx": 0,
    "paramFlowItemList": [
      {
        "object": "1001",
        "classType": "long",
        "count": 20
      },
      {
        "object": "2001",
        "classType": "long",
        "count": 10
      }
    ],
    "clusterMode": false
  }
]
```

字段说明：

| 字段                | 说明                                   |
| ------------------- | -------------------------------------- |
| `resource`          | 热点参数资源名                         |
| `count`             | 默认限流阈值                           |
| `grade`             | 限流模式，通常为 QPS                   |
| `durationInSec`     | 统计周期，单位秒                       |
| `paramIdx`          | 参数索引，从 `0` 开始                  |
| `paramFlowItemList` | 参数例外项配置                         |
| `object`            | 特定参数值                             |
| `classType`         | 参数类型，需要与 Java 实际参数类型一致 |
| `clusterMode`       | 是否启用集群模式                       |

验证热点参数规则：

```bash
# 普通商品，默认阈值为 3
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/hotspot/product?productId=3001&userId=9001"
  echo
done

# 例外商品 1001，阈值为 20
for i in $(seq 1 15); do
  curl -s "http://127.0.0.1:8081/example/hotspot/product?productId=1001&userId=9001"
  echo
done
```

如果参数例外项没有生效，优先检查 `paramIdx` 是否正确、`classType` 是否与 Java 参数类型一致、资源名是否完全匹配。

### 规则动态刷新

规则动态刷新指 Nacos 中规则内容变更后，业务应用无需重启即可加载新规则。Spring Cloud Alibaba Sentinel 数据源通过配置中心监听规则变化，客户端根据 `rule-type` 将配置内容转换成对应规则对象并加载到 Sentinel 运行时中。数据源通用配置项包括 `data-type`、`converter-class` 和 `rule-type`，默认常用 `json` 格式。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/wiki/Sentinel-en?utm_source=chatgpt.com))

动态刷新验证流程如下：

```bash
# 第一次验证：将 /example/flow/qps 的 count 设置为 2
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/flow/qps"
  echo
done

# 第二次验证：在 Nacos 中将 count 改为 5，发布配置后再次请求
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/flow/qps"
  echo
done
```

可以通过日志观察规则刷新情况。开发环境建议打开 Sentinel 和 Nacos 相关日志：

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  level:
    # Sentinel 客户端日志
    com.alibaba.csp.sentinel: info
    # Spring Cloud Alibaba Sentinel 自动配置日志
    com.alibaba.cloud.sentinel: info
    # Nacos 客户端日志
    com.alibaba.nacos: info
```

规则动态刷新排查顺序：

| 问题                   | 排查方式                                                 |
| ---------------------- | -------------------------------------------------------- |
| Nacos 配置未发布       | 检查 Nacos 控制台配置是否点击发布                        |
| Data ID 不一致         | 检查 `data-id` 与 Nacos 配置名称是否完全一致             |
| Group 不一致           | 检查 `group-id` 与 Nacos Group 是否一致                  |
| JSON 格式错误          | 检查配置是否为合法 JSON 数组                             |
| `rule-type` 错误       | 流控用 `flow`，熔断用 `degrade`，热点参数用 `param-flow` |
| 资源名不匹配           | 检查 Dashboard 簇点链路中的资源名                        |
| 应用未访问资源         | 先访问接口，让资源产生统计数据                           |
| Dashboard 修改未持久化 | 原版 Dashboard 修改规则不会自动写回 Nacos                |

生产环境建议将 Sentinel 规则作为配置资产管理，至少区分 `dev`、`test`、`prod` 命名空间，并对生产规则变更保留审批、审计和回滚记录。

## 接口开发示例

本节给出一组可以直接用于验证的接口示例，覆盖限流接口、熔断接口和 Feign 调用接口。示例保持资源名稳定，方便与前面的 Nacos 规则直接对应。

### 测试接口设计

接口设计建议覆盖 URL 自动资源、自定义资源、热点参数资源、熔断资源和 Feign 调用资源。这样可以在同一个应用中完成 Sentinel 的主要能力验证。

| 接口                       | 方法 | 资源名                         | 用途                         |
| -------------------------- | ---- | ------------------------------ | ---------------------------- |
| `/example/flow/qps`        | GET  | `/example/flow/qps`            | URL QPS 限流                 |
| `/example/flow/thread`     | GET  | `/example/flow/thread`         | URL 并发线程数限流           |
| `/example/flow/order`      | GET  | `orderSubmitResource`          | 自定义资源限流               |
| `/example/degrade/stock`   | GET  | `stockQueryResource`           | 慢调用、异常比例、异常数熔断 |
| `/example/hotspot/product` | GET  | `productDetailHotspotResource` | 热点参数限流                 |
| `/example/feign/product`   | GET  | `remoteProductResource`        | Feign 调用降级               |

建议目录结构如下：

```text
src/main/java/io/github/atengk/sentinel
├── controller
│   ├── ExampleFlowController.java
│   ├── ExampleDegradeController.java
│   ├── ExampleHotspotController.java
│   └── ExampleFeignController.java
├── feign
│   ├── ProductRemoteClient.java
│   └── fallback
│       └── ProductRemoteFallbackFactory.java
├── service
│   ├── ExampleFlowService.java
│   ├── ExampleDegradeService.java
│   └── ExampleHotspotService.java
└── service/impl
    ├── ExampleFlowServiceImpl.java
    ├── ExampleDegradeServiceImpl.java
    └── ExampleHotspotServiceImpl.java
```

以下示例假设已经引入 `spring-cloud-starter-web`、`spring-cloud-starter-alibaba-sentinel`、`sentinel-datasource-nacos`、`spring-cloud-starter-openfeign`、Hutool 和 Lombok。

### 限流接口实现

限流接口分为两类：第一类是 URL 自动资源限流，直接对 Controller 路径配置规则；第二类是自定义资源限流，通过 `@SentinelResource` 声明稳定业务资源名。

文件位置：`src/main/java/io/github/atengk/sentinel/controller/ExampleFlowController.java`

```java
package io.github.atengk.sentinel.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import io.github.atengk.sentinel.service.ExampleFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sentinel 限流示例接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ExampleFlowController {

    private final ExampleFlowService exampleFlowService;

    /**
     * URL QPS 限流接口
     *
     * @return 响应数据
     */
    @GetMapping("/example/flow/qps")
    public Map<String, Object> qps() {
        log.info("访问 URL QPS 限流示例接口");
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "URL QPS 限流接口访问成功")
                .put("resource", "/example/flow/qps")
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * URL 并发线程数限流接口
     *
     * @return 响应数据
     * @throws InterruptedException 线程休眠异常
     */
    @GetMapping("/example/flow/thread")
    public Map<String, Object> thread() throws InterruptedException {
        log.info("访问 URL 并发线程数限流示例接口");
        Thread.sleep(3000L);
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "URL 并发线程数限流接口访问成功")
                .put("resource", "/example/flow/thread")
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 自定义资源限流接口
     *
     * @param orderNo 订单号
     * @return 响应数据
     */
    @GetMapping("/example/flow/order")
    public Map<String, Object> order(String orderNo) {
        log.info("访问自定义资源限流示例接口，订单号：{}", orderNo);
        return exampleFlowService.submitOrder(orderNo);
    }
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/service/ExampleFlowService.java`

```java
package io.github.atengk.sentinel.service;

import java.util.Map;

/**
 * Sentinel 限流示例服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface ExampleFlowService {

    /**
     * 提交订单
     *
     * @param orderNo 订单号
     * @return 响应数据
     */
    Map<String, Object> submitOrder(String orderNo);
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/service/impl/ExampleFlowServiceImpl.java`

下面的服务通过 `@SentinelResource` 声明 `orderSubmitResource`，用于自定义资源限流。

```java
package io.github.atengk.sentinel.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.github.atengk.sentinel.service.ExampleFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sentinel 限流示例服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class ExampleFlowServiceImpl implements ExampleFlowService {

    /**
     * 提交订单
     *
     * @param orderNo 订单号
     * @return 响应数据
     */
    @Override
    @SentinelResource(
            value = "orderSubmitResource",
            blockHandler = "submitOrderBlockHandler",
            fallback = "submitOrderFallback"
    )
    public Map<String, Object> submitOrder(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            log.warn("提交订单失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        log.info("提交订单成功，订单号：{}", orderNo);
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "订单提交成功")
                .put("resource", "orderSubmitResource")
                .put("orderNo", orderNo)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 订单提交触发 Sentinel 保护时的处理逻辑
     *
     * @param orderNo 订单号
     * @param ex Sentinel 阻塞异常
     * @return 限流响应数据
     */
    public Map<String, Object> submitOrderBlockHandler(String orderNo, BlockException ex) {
        log.warn("订单提交触发 Sentinel 限流，订单号：{}，异常类型：{}", orderNo, ex.getClass().getSimpleName());
        return MapUtil.<String, Object>builder()
                .put("code", 429)
                .put("message", "订单提交过于频繁，请稍后重试")
                .put("resource", "orderSubmitResource")
                .put("orderNo", orderNo)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 订单提交业务异常降级处理
     *
     * @param orderNo 订单号
     * @param ex 业务异常
     * @return 降级响应数据
     */
    public Map<String, Object> submitOrderFallback(String orderNo, Throwable ex) {
        log.error("订单提交发生业务异常，订单号：{}，异常信息：{}", orderNo, ex.getMessage());
        return MapUtil.<String, Object>builder()
                .put("code", 500)
                .put("message", "订单提交失败，已返回降级结果")
                .put("resource", "orderSubmitResource")
                .put("orderNo", orderNo)
                .put("time", DateUtil.now())
                .build();
    }
}
```

验证命令：

```bash
# URL QPS 限流验证
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/flow/qps"
  echo
done

# 自定义资源限流验证
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/flow/order?orderNo=ORDER1001"
  echo
done
```

### 熔断接口实现

熔断接口用于模拟慢调用和业务异常。下面的示例使用 `stockQueryResource` 作为资源名，前面的 Nacos 熔断规则可以直接作用于该资源。

文件位置：`src/main/java/io/github/atengk/sentinel/controller/ExampleDegradeController.java`

```java
package io.github.atengk.sentinel.controller;

import io.github.atengk.sentinel.service.ExampleDegradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sentinel 熔断降级示例接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ExampleDegradeController {

    private final ExampleDegradeService exampleDegradeService;

    /**
     * 库存查询熔断测试接口
     *
     * @param productId 商品 ID
     * @param mockSlow 是否模拟慢调用
     * @param mockError 是否模拟异常
     * @return 响应数据
     */
    @GetMapping("/example/degrade/stock")
    public Map<String, Object> stock(Long productId, Boolean mockSlow, Boolean mockError) {
        log.info("访问库存查询熔断示例接口，商品ID：{}", productId);
        return exampleDegradeService.queryStock(productId, Boolean.TRUE.equals(mockSlow), Boolean.TRUE.equals(mockError));
    }
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/service/ExampleDegradeService.java`

```java
package io.github.atengk.sentinel.service;

import java.util.Map;

/**
 * Sentinel 熔断降级示例服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface ExampleDegradeService {

    /**
     * 查询库存
     *
     * @param productId 商品 ID
     * @param mockSlow 是否模拟慢调用
     * @param mockError 是否模拟异常
     * @return 响应数据
     */
    Map<String, Object> queryStock(Long productId, Boolean mockSlow, Boolean mockError);
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/service/impl/ExampleDegradeServiceImpl.java`

下面的服务通过慢调用和异常模拟触发 Sentinel 熔断规则。

```java
package io.github.atengk.sentinel.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.github.atengk.sentinel.service.ExampleDegradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sentinel 熔断降级示例服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class ExampleDegradeServiceImpl implements ExampleDegradeService {

    /**
     * 查询库存
     *
     * @param productId 商品 ID
     * @param mockSlow 是否模拟慢调用
     * @param mockError 是否模拟异常
     * @return 响应数据
     */
    @Override
    @SentinelResource(
            value = "stockQueryResource",
            blockHandler = "queryStockBlockHandler",
            fallback = "queryStockFallback"
    )
    public Map<String, Object> queryStock(Long productId, Boolean mockSlow, Boolean mockError) {
        if (productId == null || NumberUtil.compare(productId, 0L) <= 0) {
            log.warn("查询库存失败，商品ID不合法：{}", productId);
            throw new IllegalArgumentException("商品ID不合法");
        }

        if (Boolean.TRUE.equals(mockSlow)) {
            sleepQuietly(1200L);
        }

        if (Boolean.TRUE.equals(mockError)) {
            log.warn("查询库存模拟业务异常，商品ID：{}", productId);
            throw new IllegalStateException("模拟库存查询异常");
        }

        log.info("查询库存成功，商品ID：{}", productId);
        return MapUtil.<String, Object>builder()
                .put("code", 200)
                .put("message", "库存查询成功")
                .put("resource", "stockQueryResource")
                .put("productId", productId)
                .put("stock", 100)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 库存查询触发 Sentinel 熔断或限流时的处理逻辑
     *
     * @param productId 商品 ID
     * @param mockSlow 是否模拟慢调用
     * @param mockError 是否模拟异常
     * @param ex Sentinel 阻塞异常
     * @return 保护响应数据
     */
    public Map<String, Object> queryStockBlockHandler(Long productId, Boolean mockSlow, Boolean mockError, BlockException ex) {
        log.warn("库存查询触发 Sentinel 保护，商品ID：{}，异常类型：{}", productId, ex.getClass().getSimpleName());
        return MapUtil.<String, Object>builder()
                .put("code", 503)
                .put("message", "库存服务暂不可用，请稍后重试")
                .put("resource", "stockQueryResource")
                .put("productId", productId)
                .put("stock", 0)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 库存查询业务异常降级处理
     *
     * @param productId 商品 ID
     * @param mockSlow 是否模拟慢调用
     * @param mockError 是否模拟异常
     * @param ex 业务异常
     * @return 降级响应数据
     */
    public Map<String, Object> queryStockFallback(Long productId, Boolean mockSlow, Boolean mockError, Throwable ex) {
        log.error("库存查询发生业务异常，商品ID：{}，异常信息：{}", productId, ex.getMessage());
        return MapUtil.<String, Object>builder()
                .put("code", 500)
                .put("message", "库存查询失败，已返回默认库存")
                .put("resource", "stockQueryResource")
                .put("productId", productId)
                .put("stock", 0)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * 安静休眠
     *
     * @param millis 休眠毫秒数
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("库存查询休眠被中断");
        }
    }
}
```

验证命令：

```bash
# 慢调用比例熔断验证
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/degrade/stock?productId=1001&mockSlow=true"
  echo
done

# 异常比例或异常数熔断验证
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/degrade/stock?productId=1001&mockError=true"
  echo
done
```

### Feign 调用接口实现

Feign 调用接口用于验证远程服务不可用、调用超时或 Sentinel 熔断后的降级返回。Spring Cloud OpenFeign 支持在 `@FeignClient` 上配置 `fallback` 或 `fallbackFactory`；`fallbackFactory` 可以获取触发降级的异常原因，更适合生产排查。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/wiki/Sentinel-en?utm_source=chatgpt.com))

先开启 Feign Sentinel 支持。

文件位置：`src/main/resources/application.yml`

```yaml
feign:
  sentinel:
    # 开启 Sentinel 对 OpenFeign 的支持
    enabled: true

spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            # Feign 连接超时时间
            connectTimeout: 3000
            # Feign 读取超时时间
            readTimeout: 5000
            # 开发阶段可使用 basic，生产环境按日志规范调整
            loggerLevel: basic
```

启动类开启 Feign 客户端扫描。

文件位置：`src/main/java/io/github/atengk/sentinel/SentinelDemoApplication.java`

```java
package io.github.atengk.sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Sentinel 示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@EnableFeignClients
@SpringBootApplication
public class SentinelDemoApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SentinelDemoApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/feign/ProductRemoteClient.java`

```java
package io.github.atengk.sentinel.feign;

import io.github.atengk.sentinel.feign.fallback.ProductRemoteFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 商品服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(
        name = "product-service",
        path = "/product",
        fallbackFactory = ProductRemoteFallbackFactory.class
)
public interface ProductRemoteClient {

    /**
     * 查询商品详情
     *
     * @param productId 商品 ID
     * @return 商品详情响应数据
     */
    @GetMapping("/detail")
    Map<String, Object> detail(@RequestParam("productId") Long productId);
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/feign/fallback/ProductRemoteFallbackFactory.java`

下面的 fallback 工厂用于返回商品服务调用失败后的兜底数据。

```java
package io.github.atengk.sentinel.feign.fallback;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import io.github.atengk.sentinel.feign.ProductRemoteClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 商品服务 Feign 降级工厂
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class ProductRemoteFallbackFactory implements FallbackFactory<ProductRemoteClient> {

    /**
     * 创建商品服务降级客户端
     *
     * @param cause 降级原因
     * @return 商品服务降级客户端
     */
    @Override
    public ProductRemoteClient create(Throwable cause) {
        return new ProductRemoteClient() {

            /**
             * 商品详情降级返回
             *
             * @param productId 商品 ID
             * @return 降级响应数据
             */
            @Override
            public Map<String, Object> detail(Long productId) {
                log.error("调用商品服务失败，商品ID：{}，异常类型：{}，异常信息：{}",
                        productId,
                        cause.getClass().getSimpleName(),
                        cause.getMessage());

                return MapUtil.<String, Object>builder()
                        .put("code", 503)
                        .put("message", "商品服务暂不可用，已返回降级结果")
                        .put("resource", "remoteProductResource")
                        .put("productId", productId)
                        .put("productName", "默认商品")
                        .put("fallback", true)
                        .put("time", DateUtil.now())
                        .build();
            }
        };
    }
}
```

文件位置：`src/main/java/io/github/atengk/sentinel/controller/ExampleFeignController.java`

```java
package io.github.atengk.sentinel.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.github.atengk.sentinel.feign.ProductRemoteClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sentinel Feign 调用示例接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ExampleFeignController {

    private final ProductRemoteClient productRemoteClient;

    /**
     * Feign 调用商品服务
     *
     * @param productId 商品 ID
     * @return 商品详情响应数据
     */
    @GetMapping("/example/feign/product")
    @SentinelResource(
            value = "remoteProductResource",
            blockHandler = "productBlockHandler",
            fallback = "productFallback"
    )
    public Map<String, Object> product(Long productId) {
        log.info("开始调用商品服务，商品ID：{}", productId);
        return productRemoteClient.detail(productId);
    }

    /**
     * Feign 调用触发 Sentinel 保护时的处理逻辑
     *
     * @param productId 商品 ID
     * @param ex Sentinel 阻塞异常
     * @return 保护响应数据
     */
    public Map<String, Object> productBlockHandler(Long productId, BlockException ex) {
        log.warn("Feign 商品调用触发 Sentinel 保护，商品ID：{}，异常类型：{}", productId, ex.getClass().getSimpleName());
        return MapUtil.<String, Object>builder()
                .put("code", 503)
                .put("message", "商品调用过于频繁或已熔断，请稍后重试")
                .put("resource", "remoteProductResource")
                .put("productId", productId)
                .put("fallback", true)
                .put("time", DateUtil.now())
                .build();
    }

    /**
     * Feign 调用业务异常降级处理
     *
     * @param productId 商品 ID
     * @param ex 业务异常
     * @return 降级响应数据
     */
    public Map<String, Object> productFallback(Long productId, Throwable ex) {
        log.error("Feign 商品调用发生业务异常，商品ID：{}，异常信息：{}", productId, ex.getMessage());
        return MapUtil.<String, Object>builder()
                .put("code", 500)
                .put("message", "商品调用失败，已返回接口层降级结果")
                .put("resource", "remoteProductResource")
                .put("productId", productId)
                .put("fallback", true)
                .put("time", DateUtil.now())
                .build();
    }
}
```

验证命令：

```bash
# product-service 未启动时，会进入 Feign fallbackFactory
curl "http://127.0.0.1:8081/example/feign/product?productId=1001"

# 如果对 remoteProductResource 配置熔断规则，连续异常后会触发 Sentinel 熔断
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/feign/product?productId=1001"
  echo
done
```

Feign 调用降级建议分两层处理：远程客户端层使用 `fallbackFactory` 兜底远程服务不可用；接口或业务方法层使用 `@SentinelResource` 保护本地资源，便于对 `remoteProductResource` 配置熔断规则。这样既能拿到远程调用异常原因，也能在 Sentinel 维度观测和控制该调用资源。

## 功能验证

本节用于验证前面章节中 Sentinel Dashboard、限流、熔断、Nacos 动态规则是否已经正确接入。原始大纲中该部分位于「接口开发示例」之后，适合作为开发完成后的自测流程和测试环境验收清单。

功能验证建议按顺序执行：先验证 Dashboard 能看到应用，再验证 URL 限流和自定义资源限流，然后验证熔断降级，最后验证 Nacos 动态规则刷新。不要一开始就同时启用多种规则，否则排查时难以判断是流控、熔断、热点参数还是系统保护触发了拦截。

### Dashboard 监控验证

Dashboard 监控验证用于确认业务应用已经成功接入 Sentinel，并且资源调用数据可以正常上报。应用启动后，只有接口被实际访问过，Dashboard 才更容易显示资源和监控数据。

验证前需要确认三个服务已经启动：

| 服务               | 默认地址         | 用途                                    |
| ------------------ | ---------------- | --------------------------------------- |
| Nacos              | `127.0.0.1:8848` | 注册中心、配置中心、Sentinel 规则持久化 |
| Sentinel Dashboard | `127.0.0.1:8080` | 监控资源、查看规则、调试限流熔断        |
| 业务应用           | `127.0.0.1:8081` | Sentinel 接入测试服务                   |

启动业务应用后，先访问几个测试接口，让 Sentinel 产生资源统计数据：

```bash
# 触发 URL 自动资源上报
curl "http://127.0.0.1:8081/example/flow/qps"

# 触发自定义资源上报
curl "http://127.0.0.1:8081/example/flow/order?orderNo=ORDER1001"

# 触发熔断资源上报
curl "http://127.0.0.1:8081/example/degrade/stock?productId=1001"

# 触发热点参数资源上报
curl "http://127.0.0.1:8081/example/hotspot/product?productId=1001&userId=9001"
```

Dashboard 中需要重点检查以下位置：

| Dashboard 菜单 | 验证内容                                                     |
| -------------- | ------------------------------------------------------------ |
| 机器列表       | 是否出现 `sentinel-demo-service`                             |
| 实时监控       | 是否出现请求 QPS、RT、通过数、拒绝数                         |
| 簇点链路       | 是否出现 `/example/flow/qps`、`orderSubmitResource`、`stockQueryResource` 等资源 |
| 流控规则       | Nacos 或 Dashboard 配置的流控规则是否可见                    |
| 熔断规则       | 熔断规则是否加载                                             |
| 热点规则       | 热点参数规则是否加载                                         |

如果 Dashboard 能看到应用，但看不到资源，通常是接口没有被访问过，或者资源名不是预期名称。先访问接口，再刷新 Dashboard 的簇点链路页面。

### 限流效果验证

限流效果验证用于确认 QPS 限流、并发线程数限流、自定义资源限流是否符合预期。建议先使用较低阈值进行本地验证，例如 QPS 阈值设置为 `2`，并发线程数阈值设置为 `2`。

Nacos 中流控规则示例：

```json
[
  {
    "resource": "/example/flow/qps",
    "limitApp": "default",
    "grade": 1,
    "count": 2,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  },
  {
    "resource": "/example/flow/thread",
    "limitApp": "default",
    "grade": 0,
    "count": 2,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  },
  {
    "resource": "orderSubmitResource",
    "limitApp": "default",
    "grade": 1,
    "count": 3,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

验证 URL QPS 限流：

```bash
# 连续请求 URL 资源，超过 QPS 阈值后应触发限流
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/flow/qps"
  echo
done
```

预期结果：部分请求返回正常业务数据，超过阈值的请求返回统一限流响应，例如：

```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后重试",
  "path": "/example/flow/qps",
  "resource": "/example/flow/qps",
  "exception": "FlowException",
  "time": "2026-05-05 10:30:00"
}
```

验证并发线程数限流：

```bash
# 使用 10 个并发请求访问慢接口，同时只允许 2 个线程进入资源
seq 1 10 | xargs -I {} -P 10 curl -s "http://127.0.0.1:8081/example/flow/thread"
```

预期结果：少量请求进入业务方法并等待返回，其余请求被快速拒绝。Dashboard 的实时监控中，拒绝数会增加。

验证自定义资源限流：

```bash
# orderSubmitResource 配置 QPS 阈值后，连续请求会触发 blockHandler
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/flow/order?orderNo=ORDER1001"
  echo
done
```

预期结果：触发限流时进入 `submitOrderBlockHandler`，返回 `订单提交过于频繁，请稍后重试`。如果没有触发，需要检查 Nacos 规则中的 `resource` 是否严格等于 `orderSubmitResource`。

### 熔断效果验证

熔断效果验证用于确认慢调用比例、异常比例和异常数规则是否生效。熔断不是单次请求失败就一定触发，它依赖统计窗口、最小请求数、比例或数量阈值等条件。

Nacos 中熔断规则示例：

```json
[
  {
    "resource": "stockQueryResource",
    "grade": 0,
    "count": 500,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 10000,
    "slowRatioThreshold": 0.6
  }
]
```

验证慢调用比例熔断：

```bash
# mockSlow=true 会让接口休眠，超过慢调用 RT 阈值
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/degrade/stock?productId=1001&mockSlow=true"
  echo
done
```

预期结果：前几次请求进入真实业务方法，当统计窗口内慢调用比例达到阈值后，后续请求会被快速熔断，进入 `queryStockBlockHandler`。

异常比例熔断规则示例：

```json
[
  {
    "resource": "stockQueryResource",
    "grade": 1,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 10000
  }
]
```

验证异常比例熔断：

```bash
# mockError=true 会抛出业务异常，达到异常比例阈值后触发熔断
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/degrade/stock?productId=1001&mockError=true"
  echo
done
```

异常数熔断规则示例：

```json
[
  {
    "resource": "stockQueryResource",
    "grade": 2,
    "count": 5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 10000
  }
]
```

验证异常数熔断：

```bash
# 连续制造异常，异常数达到阈值后资源会进入熔断状态
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/degrade/stock?productId=1001&mockError=true"
  echo
done
```

熔断验证时需要关注三类现象：

| 现象                         | 说明                               |
| ---------------------------- | ---------------------------------- |
| 前几次请求进入真实业务       | 熔断需要先积累统计数据             |
| 达到阈值后快速返回           | 资源进入熔断状态，不再执行业务方法 |
| 等待 `timeWindow` 后恢复尝试 | 熔断窗口结束后进入探测恢复阶段     |

如果熔断一直不触发，优先检查 `minRequestAmount` 是否过高、统计窗口内请求数是否不足、业务异常是否被代码内部吞掉、资源名是否匹配。

### Nacos 动态规则验证

Nacos 动态规则验证用于确认 Sentinel 客户端是否能监听 Nacos 配置变更，并在不重启应用的情况下刷新规则。该验证是规则持久化是否可用于测试环境和生产环境的关键。

先在 Nacos 中配置低阈值流控规则：

```json
[
  {
    "resource": "/example/flow/qps",
    "limitApp": "default",
    "grade": 1,
    "count": 2,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

第一次验证：

```bash
# 阈值为 2，连续请求应较容易触发限流
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/flow/qps"
  echo
done
```

然后在 Nacos 中将 `count` 从 `2` 修改为 `5` 并发布：

```json
[
  {
    "resource": "/example/flow/qps",
    "limitApp": "default",
    "grade": 1,
    "count": 5,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

第二次验证：

```bash
# 不重启业务应用，再次请求，限流触发频率应降低
for i in $(seq 1 10); do
  curl -s "http://127.0.0.1:8081/example/flow/qps"
  echo
done
```

动态刷新验证通过的标准：

| 验证项                    | 预期结果                                 |
| ------------------------- | ---------------------------------------- |
| 不重启应用修改 Nacos 规则 | 规则生效                                 |
| Dashboard 中规则同步变化  | 能看到新的阈值或运行效果变化             |
| 应用日志无规则解析错误    | 没有 JSON 解析失败、Data ID 找不到等错误 |
| 请求结果随阈值变化        | 限流频率明显变化                         |

如果规则没有动态刷新，按以下顺序排查：

| 排查项             | 处理方式                                                     |
| ------------------ | ------------------------------------------------------------ |
| Data ID            | 必须与 `spring.cloud.sentinel.datasource.*.nacos.data-id` 完全一致 |
| Group              | 必须与 `group-id` 完全一致                                   |
| Namespace          | 应用配置的 namespace 必须和 Nacos 控制台一致                 |
| JSON 格式          | 必须是合法 JSON 数组                                         |
| `rule-type`        | 流控用 `flow`，熔断用 `degrade`，热点参数用 `param-flow`     |
| 资源名             | 必须与 Dashboard 簇点链路中的资源名一致                      |
| Dashboard 修改规则 | 原版 Dashboard 修改后通常不会自动写回 Nacos                  |

## 部署与运行

本节用于说明 Sentinel 示例项目在本地开发环境和测试环境中的部署方式，包括 Nacos、Sentinel Dashboard、业务应用启动、Dashboard 访问配置和常见启动问题处理。

### 本地开发运行

本地开发运行建议使用三个独立进程：Nacos、Sentinel Dashboard、业务应用。先启动 Nacos 和 Dashboard，再启动业务应用，最后访问接口触发资源上报。

推荐启动顺序如下：

```text
1. 启动 Nacos
2. 创建 Sentinel 规则配置
3. 启动 Sentinel Dashboard
4. 启动业务应用 sentinel-demo-service
5. 访问测试接口
6. 打开 Dashboard 查看资源和规则
```

本地启动 Sentinel Dashboard：

```bash
# 启动 Sentinel Dashboard
java \
  -Dserver.port=8080 \
  -Dcsp.sentinel.dashboard.server=127.0.0.1:8080 \
  -Dproject.name=sentinel-dashboard \
  -jar sentinel-dashboard-1.8.9.jar
```

本地启动业务应用：

```bash
# 在项目根目录启动 Spring Boot 应用
mvn spring-boot:run
```

也可以先打包再启动：

```bash
# 打包应用，跳过测试
mvn clean package -DskipTests

# 启动业务应用
java \
  -Dspring.profiles.active=dev \
  -jar target/sentinel-demo-service.jar
```

本地开发配置示例：

文件位置：`src/main/resources/application-dev.yml`

```yaml
server:
  # 本地业务服务端口
  port: 8081

spring:
  application:
    # 应用名称，Dashboard 和 Nacos 中都会使用
    name: sentinel-demo-service

  cloud:
    nacos:
      discovery:
        # 本地 Nacos 注册中心
        server-addr: 127.0.0.1:8848
        group: DEFAULT_GROUP
      config:
        # 本地 Nacos 配置中心
        server-addr: 127.0.0.1:8848
        file-extension: yaml
        group: DEFAULT_GROUP

    sentinel:
      transport:
        # 本地 Sentinel Dashboard
        dashboard: 127.0.0.1:8080
        # Sentinel 客户端通信端口
        port: 8719
      # 开发环境建议立即初始化，减少 Dashboard 看不到应用的误判
      eager: true
      # 关闭 Web Context 统一入口，便于 URL 资源统计
      web-context-unify: false
```

本地验证命令：

```bash
# 验证应用是否启动
curl "http://127.0.0.1:8081/example/flow/qps"

# 验证 Dashboard 是否可访问
curl -I "http://127.0.0.1:8080"

# 验证 Nacos 是否可访问
curl -I "http://127.0.0.1:8848/nacos"
```

本地开发环境中，如果只是验证 Sentinel 规则，可以先不接入完整网关链路，直接访问业务服务接口即可。

### 测试环境部署

测试环境部署建议将 Nacos、Sentinel Dashboard、业务应用拆成独立服务，并通过内网访问。Dashboard 不建议直接暴露到公网；如必须开放，应至少增加网关认证、访问白名单或 VPN 限制。

测试环境推荐配置如下：

| 项目               | 建议                                         |
| ------------------ | -------------------------------------------- |
| Nacos              | 独立部署，按环境划分 namespace               |
| Sentinel Dashboard | 独立部署，限制内网访问                       |
| 业务应用           | 多实例部署，配置相同的 Nacos 规则            |
| 规则配置           | 使用 Nacos 持久化，不依赖 Dashboard 内存规则 |
| 日志               | 保留 Sentinel、Nacos、Feign 降级相关日志     |
| 端口               | 业务端口和 Sentinel transport 端口不能冲突   |

测试环境配置示例：

文件位置：`src/main/resources/application-test.yml`

```yaml
server:
  # 测试环境业务端口
  port: 8081

spring:
  application:
    # 测试环境服务名称
    name: sentinel-demo-service

  cloud:
    nacos:
      discovery:
        # 测试环境 Nacos 地址
        server-addr: nacos-test.example.internal:8848
        # 测试环境命名空间 ID
        namespace: test-namespace-id
        group: TEST_GROUP
      config:
        # 测试环境 Nacos 配置中心地址
        server-addr: nacos-test.example.internal:8848
        file-extension: yaml
        namespace: test-namespace-id
        group: TEST_GROUP

    sentinel:
      transport:
        # 测试环境 Sentinel Dashboard 地址
        dashboard: sentinel-dashboard-test.example.internal:8080
        # 当前实例 Sentinel 通信端口，多实例同机部署时需要避免冲突
        port: 8719
      eager: true
      web-context-unify: false
      datasource:
        flow-rule:
          nacos:
            server-addr: nacos-test.example.internal:8848
            namespace: test-namespace-id
            group-id: TEST_GROUP
            data-id: sentinel-demo-service-flow-rules
            data-type: json
            rule-type: flow
        degrade-rule:
          nacos:
            server-addr: nacos-test.example.internal:8848
            namespace: test-namespace-id
            group-id: TEST_GROUP
            data-id: sentinel-demo-service-degrade-rules
            data-type: json
            rule-type: degrade
        param-flow-rule:
          nacos:
            server-addr: nacos-test.example.internal:8848
            namespace: test-namespace-id
            group-id: TEST_GROUP
            data-id: sentinel-demo-service-param-flow-rules
            data-type: json
            rule-type: param-flow

feign:
  sentinel:
    # 测试环境开启 Feign Sentinel 降级
    enabled: true

logging:
  level:
    # 测试环境保留 Sentinel 关键信息日志
    com.alibaba.csp.sentinel: info
    com.alibaba.cloud.sentinel: info
    com.alibaba.nacos: info
```

测试环境启动命令：

```bash
# 使用 test 配置启动业务应用
java \
  -Xms512m \
  -Xmx512m \
  -Dspring.profiles.active=test \
  -jar sentinel-demo-service.jar
```

如果同一台机器部署多个业务实例，需要显式指定不同的 Sentinel transport 端口：

```bash
# 实例 1
java \
  -Dspring.profiles.active=test \
  -Dspring.cloud.sentinel.transport.port=8719 \
  -jar sentinel-demo-service.jar

# 实例 2
java \
  -Dspring.profiles.active=test \
  -Dspring.cloud.sentinel.transport.port=8720 \
  -jar sentinel-demo-service.jar
```

测试环境验收清单：

| 检查项         | 标准                                       |
| -------------- | ------------------------------------------ |
| 应用注册       | Nacos 服务列表能看到业务服务实例           |
| Dashboard 上报 | Sentinel Dashboard 能看到所有业务实例      |
| 流控规则       | 从 Nacos 加载并能动态调整                  |
| 熔断规则       | 慢调用或异常触发后进入降级                 |
| 热点参数规则   | 指定参数值触发限流                         |
| Feign 降级     | 下游服务不可用时返回兜底数据               |
| 日志           | 能定位资源名、异常类型、请求路径和关键参数 |

### Dashboard 访问配置

Dashboard 访问配置用于控制 Sentinel Dashboard 的访问方式。开发环境可以直接访问 `http://127.0.0.1:8080`，测试环境和生产环境应放在内网，避免未授权用户修改规则。

常见访问方式如下：

| 环境     | 访问方式                | 建议               |
| -------- | ----------------------- | ------------------ |
| 本地开发 | `http://127.0.0.1:8080` | 直接访问           |
| 测试环境 | 内网域名                | 配置网关认证或 VPN |
| 生产环境 | 内网域名 + 权限控制     | 不建议直接暴露公网 |

Nginx 反向代理示例：

文件位置：`/etc/nginx/conf.d/sentinel-dashboard.conf`

```nginx
server {
    # Dashboard 代理端口
    listen 80;

    # Dashboard 内网访问域名
    server_name sentinel-dashboard-test.example.internal;

    location / {
        # 代理到 Sentinel Dashboard 服务
        proxy_pass http://127.0.0.1:8080;

        # 保留原始 Host
        proxy_set_header Host $host;

        # 传递真实客户端 IP
        proxy_set_header X-Real-IP $remote_addr;

        # 传递代理链路 IP
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # 传递协议
        proxy_set_header X-Forwarded-Proto $scheme;

        # Dashboard 页面和接口超时时间
        proxy_connect_timeout 5s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }
}
```

加载 Nginx 配置：

```bash
# 检查 Nginx 配置是否正确
nginx -t

# 重新加载 Nginx 配置
nginx -s reload
```

Dashboard 访问安全建议：

| 建议             | 说明                                   |
| ---------------- | -------------------------------------- |
| 不暴露公网       | Dashboard 可修改规则，暴露公网风险较高 |
| 使用内网域名     | 仅允许办公网、测试网或 VPN 访问        |
| 限制访问 IP      | 通过 Nginx、网关或安全组限制来源       |
| 修改默认账号密码 | 避免使用默认账号长期运行               |
| 生产规则走 Nacos | 不依赖 Dashboard 内存规则              |
| 记录规则变更     | 生产环境规则变更应有审计和回滚方案     |

业务应用访问 Dashboard 时，配置的是 `spring.cloud.sentinel.transport.dashboard`，该地址必须是业务应用所在机器或容器能够访问的地址。浏览器能打开 Dashboard，不代表业务应用也能连通 Dashboard，尤其是在 Docker、Kubernetes 或跨网段部署时需要重点检查网络。

### 常见启动问题处理

常见启动问题通常集中在版本不兼容、Dashboard 看不到应用、Nacos 规则未加载、端口冲突、资源名不匹配、Feign 降级不生效几个方面。排查时建议先看应用启动日志，再看 Dashboard 机器列表，最后用接口请求验证规则效果。

常见问题和处理方式如下：

| 问题                     | 可能原因                                                   | 处理方式                                                     |
| ------------------------ | ---------------------------------------------------------- | ------------------------------------------------------------ |
| Dashboard 看不到应用     | 应用未访问接口、Dashboard 地址配置错误、网络不通           | 访问一次接口，检查 `spring.cloud.sentinel.transport.dashboard` |
| Dashboard 有应用但无资源 | 资源未被访问、资源名不符合预期                             | 访问接口后刷新簇点链路                                       |
| 规则不生效               | 资源名不匹配、Nacos Data ID 错误、JSON 格式错误            | 以 Dashboard 簇点链路资源名为准                              |
| 应用启动报依赖错误       | Spring Boot、Spring Cloud、Spring Cloud Alibaba 版本不匹配 | 统一使用 BOM 管理版本                                        |
| Nacos 规则未加载         | namespace、group、data-id 配置不一致                       | 对照 Nacos 控制台逐项检查                                    |
| 动态修改规则无效         | Nacos 配置未发布、客户端未监听到变更                       | 发布配置并观察应用日志                                       |
| 端口 8719 冲突           | 同机启动多个应用实例                                       | 为每个实例配置不同 `transport.port`                          |
| Feign fallback 不生效    | 未开启 `feign.sentinel.enabled`、fallback 未注册为 Bean    | 检查配置和 `@Component`                                      |
| 热点参数规则不生效       | 未通过 `SphU.entry` 传参、`paramIdx` 错误、类型不匹配      | 检查参数索引和 `classType`                                   |
| 授权规则不生效           | 未实现 `RequestOriginParser` 或来源为空                    | 检查 `X-Client-Origin` 解析逻辑                              |
| 系统规则误拦截           | 阈值设置过低                                               | 结合监控数据调高阈值                                         |

查看端口占用：

```bash
# 查看 Dashboard 端口
netstat -tunlp | grep 8080

# 查看业务应用端口
netstat -tunlp | grep 8081

# 查看 Sentinel 客户端通信端口
netstat -tunlp | grep 8719

# 查看 Nacos 端口
netstat -tunlp | grep 8848
```

查看应用日志中的 Sentinel 和 Nacos 关键字：

```bash
# 查看 Sentinel 相关日志
grep -i "sentinel" logs/sentinel-demo-service.log

# 查看 Nacos 相关日志
grep -i "nacos" logs/sentinel-demo-service.log

# 查看规则加载异常
grep -i "rule" logs/sentinel-demo-service.log
```

检查资源名是否匹配：

```bash
# 访问 URL 资源
curl "http://127.0.0.1:8081/example/flow/qps"

# 访问自定义资源
curl "http://127.0.0.1:8081/example/flow/order?orderNo=ORDER1001"
```

资源名匹配原则如下：

| 资源类型       | 规则中的 `resource`                                          |
| -------------- | ------------------------------------------------------------ |
| URL 自动资源   | 一般使用接口路径，例如 `/example/flow/qps`                   |
| 自定义资源     | 使用 `@SentinelResource(value = "...")` 中的值               |
| 热点参数资源   | 使用 `SphU.entry(resourceName, ...)` 中的资源名              |
| Feign 保护资源 | 使用自定义包装资源名或 Feign 自动资源名，按 Dashboard 实际显示为准 |

最终验收标准是：应用启动后能在 Dashboard 看到实例和资源；Nacos 中配置的流控、熔断、热点参数规则能够自动加载；修改 Nacos 规则后无需重启应用即可生效；限流、熔断、热点参数、Feign 降级都能返回统一且可观测的兜底响应。