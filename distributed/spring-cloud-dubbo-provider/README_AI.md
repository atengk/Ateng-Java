# Apache Dubbo

Apache Dubbo 是一套面向微服务架构的高性能 RPC 与服务治理框架。在 Spring Boot 3 项目中，Dubbo 通常用于拆分内部业务服务，例如用户服务、订单服务、支付服务、库存服务之间的远程调用。Spring Boot 负责应用启动、配置管理、Web 接口、监控与依赖装配，Dubbo 负责服务暴露、服务引用、注册发现、负载均衡、超时、重试、分组、版本和治理能力。

## 技术概述

本节用于说明 Spring Boot 3 与 Apache Dubbo 在微服务项目中的定位。学习重点不是单纯理解 RPC 概念，而是掌握 Dubbo 在 Spring Boot 3 工程中的依赖配置、接口拆分、服务暴露、远程调用和注册中心集成方式。

### Spring Boot 3 特性

Spring Boot 3 的核心变化是技术栈整体升级到 Java 17、Spring Framework 6 和 Jakarta EE 命名空间。以 Spring Boot 3.2.6 官方文档为例，它要求 Java 17，并明确支持 Maven 3.6.3 及以上版本；Servlet 容器方面使用 Tomcat 10.1、Jetty 12、Undertow 2.3 这一代运行环境。([Home](https://docs.spring.io/spring-boot/docs/3.2.6/reference/htmlsingle/))

在 Dubbo 项目中，Spring Boot 3 主要承担以下职责。

第一，提供统一的应用启动模型。每个 Provider 和 Consumer 都是独立的 Spring Boot 应用，可以通过 `java -jar`、Docker、Kubernetes 或传统进程方式启动。

第二，提供配置管理能力。Dubbo 的应用名、协议端口、注册中心地址、超时时间、重试次数、服务版本等配置，都可以放在 `application.yml` 中统一维护。Dubbo Spring Boot Starter 会识别 `dubbo.` 开头的配置项，并扫描 `@DubboService` 等注解。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/tasks/develop/springboot/))

第三，提供工程生态能力。Spring Boot 3 可以继续集成 Web、Validation、Actuator、日志、配置中心、监控、链路追踪等能力。Dubbo 3.3.x 也提供了 observability、tracing、sentinel、seata、nacos、zookeeper 等 Starter，便于按需引入服务治理组件。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/config/spring/spring-boot/))

第四，推动代码从传统 Java EE 迁移到 Jakarta EE。Spring Boot 3 生态中，`javax.*` 相关 API 大量迁移到 `jakarta.*`，因此在老项目升级到 Spring Boot 3 + Dubbo 3 时，需要重点检查 Servlet、Validation、JPA、JAXB 等依赖是否仍引用旧包名。

### Apache Dubbo 核心概念

Apache Dubbo 的核心是面向接口的远程服务调用。业务方先定义公共 API 接口，Provider 实现接口并暴露服务，Consumer 引用接口并像调用本地对象一样调用远程服务。Dubbo 负责网络通信、序列化、路由、负载均衡、超时控制、失败重试和服务治理。

Dubbo 在 Spring Boot 项目中常见的核心概念如下。

| 概念              | 说明                                           | 开发中对应内容                          |
| ----------------- | ---------------------------------------------- | --------------------------------------- |
| Service Interface | 服务接口，定义 Provider 和 Consumer 之间的契约 | `dubbo-api` 模块中的 Java 接口          |
| Provider          | 服务提供者，实现接口并对外暴露 RPC 服务        | `@DubboService` 标注的实现类            |
| Consumer          | 服务消费者，引用远程服务并发起调用             | `@DubboReference` 注入的远程代理        |
| Registry          | 注册中心，维护服务地址和元数据                 | Nacos、ZooKeeper、Kubernetes 等         |
| Protocol          | 通信协议，决定远程调用方式                     | `dubbo`、`tri` 等                       |
| Serialization     | 序列化方式，决定请求与响应对象如何编码         | Hessian、Fastjson2、Triple 相关序列化等 |
| Cluster           | 集群容错策略，处理多 Provider 场景下的失败行为 | failover、failfast 等                   |
| LoadBalance       | 负载均衡策略，决定请求打到哪个 Provider        | random、roundrobin、leastactive 等      |
| Version / Group   | 服务版本与分组，用于灰度、隔离和多实现共存     | `version`、`group` 配置                 |

注册中心是 Dubbo 服务治理的核心组件，Dubbo 依赖注册中心完成服务地址发现；自动服务发现是动态扩缩容、负载均衡、流量管理等能力的基础。Dubbo 3 的服务发现机制也从 Dubbo 2 时代的接口级服务发现演进到应用级服务发现。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/registry/overview/))

在 Spring Boot 3 项目中，Dubbo Starter 的职责是降低接入成本。`dubbo-spring-boot-starter` 会引入 Dubbo 核心依赖，并自动扫描 Dubbo 配置和注解；Dubbo 3.3.x 还提供了 `dubbo-nacos-spring-boot-starter`、`dubbo-zookeeper-spring-boot-starter`、`dubbo-zookeeper-curator5-spring-boot-starter` 等组件 Starter。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/tasks/develop/springboot/))

### 适用场景

Dubbo 更适合后端服务之间的高性能内部调用，尤其适合 Java 技术栈较重、服务拆分明确、调用频率较高、对服务治理能力有要求的业务系统。

典型适用场景包括以下几类。

第一，单体系统拆分为微服务。比如将用户、订单、商品、库存、支付、营销等模块拆成独立服务，各服务之间通过 Dubbo 接口调用，避免所有模块耦合在一个应用中。

第二，高频内部 RPC 调用。Dubbo 比普通 HTTP JSON 调用更强调 RPC 性能、接口契约和服务治理，适合服务间频繁调用、对延迟和吞吐有要求的业务链路。

第三，需要服务治理能力的系统。例如需要注册发现、负载均衡、超时控制、重试、分组、版本、灰度、路由、限流、熔断、链路追踪、指标监控等能力时，Dubbo 比简单的 HTTP Client 更完整。

第四，Provider 与 Consumer 需要明确接口契约。Dubbo 推荐将公共接口、DTO、枚举、异常定义放在独立 API 模块中，Provider 和 Consumer 都依赖该模块，从工程结构上保证调用契约一致。

不建议使用 Dubbo 的场景也需要明确。对外开放 API、前端直接调用、跨语言公共开放平台、简单低频接口调用，通常优先使用 HTTP REST、OpenAPI 或网关方案。Dubbo 更适合作为后端内部服务之间的 RPC 通信框架。

## 环境准备

本节用于确定开发前需要安装的 JDK、Maven、Spring Boot、Dubbo 和注册中心版本。版本选择建议以稳定、兼容、可维护为原则，不建议在核心业务系统中随意使用未验证的最新版本。

### JDK 与 Maven 版本

Spring Boot 3 项目建议统一使用 JDK 17 或 JDK 21。JDK 17 是 Spring Boot 3 的最低要求，也是当前企业项目中更稳妥的基线；JDK 21 可以作为长期演进版本，但需要确认 Dubbo、注册中心客户端、监控组件、构建插件和运行环境全部兼容。Spring Boot 3.2.6 官方文档要求 Java 17，并要求 Maven 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/docs/3.2.6/reference/htmlsingle/))

推荐本地开发环境如下。

| 工具   | 推荐版本            | 说明                                                         |
| ------ | ------------------- | ------------------------------------------------------------ |
| JDK    | 17                  | Spring Boot 3 最低要求，生产项目优先选用                     |
| Maven  | 3.9.x               | 满足 Spring Boot 3 的 Maven 3.6.3+ 要求，插件兼容性更好      |
| IDE    | IntelliJ IDEA 2024+ | 对 Spring Boot 3、JDK 17、Maven 多模块支持较好               |
| Lombok | 最新稳定版          | 简化 DTO、VO、配置类代码                                     |
| Hutool | 5.8.x               | 常用工具类库，用于字符串、集合、日期、JSON、Bean 拷贝等辅助处理 |

开发前可以执行以下命令确认本机环境。

```bash
# 查看 JDK 版本，建议输出 17.x 或 21.x
java -version

# 查看 Maven 版本，建议使用 3.9.x
mvn -version

# 查看 Maven 当前使用的 JDK，避免 IDE 和命令行 JDK 不一致
mvn -v
```

Maven 编译版本建议在父工程中统一声明，避免各模块 JDK 配置不一致。

```xml
<properties>
    <!-- Spring Boot 3 推荐使用 Java 17 作为最低基线 -->
    <java.version>17</java.version>

    <!-- 统一 Maven 编译源码版本 -->
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>

    <!-- 项目源码编码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

### Spring Boot 与 Dubbo 版本选择

Spring Boot 3 与 Dubbo 3 的版本需要成组选择。Dubbo 官方版本说明中，3.3.x 分支标注为生产可用、推荐、长期维护，并支持 JDK 8、17、21 以及 Spring Boot 2.x、3.x。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/versions/))

推荐组合如下。

| 场景                      | Spring Boot   | Dubbo          | JDK  | 说明                                                         |
| ------------------------- | ------------- | -------------- | ---- | ------------------------------------------------------------ |
| 新项目稳妥选择            | 3.2.x / 3.3.x | 3.3.x          | 17   | 兼容性和资料完整度较好                                       |
| 已有 Spring Boot 3.2 项目 | 3.2.x         | 3.3.x          | 17   | Dubbo 官方 Starter 文档明确覆盖 Spring Boot 3.2 相关 Starter |
| 需要 JDK 21               | 3.2.x+        | 3.3.x          | 21   | 需要对注册中心客户端、构建插件、部署镜像做完整回归           |
| 老项目升级                | 2.7.x / 3.0.x | 不建议继续新建 | 8    | Dubbo 2.7.x 和 3.0.x 已不适合作为新项目基线                  |

截至 Maven Central 当前可见版本，`dubbo-spring-boot-starter` 已发布到 3.3.6；文档编写时可以将 `dubbo.version` 写成一个统一属性，后续升级只需要调整父工程版本。([repo.maven.apache.org](https://repo.maven.apache.org/maven2/org/apache/dubbo/dubbo-spring-boot-starter/?utm_source=chatgpt.com))

父工程建议使用 `spring-boot-starter-parent` 管理 Spring Boot 依赖，再通过 `dubbo-bom` 管理 Dubbo 相关依赖版本。

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.6</version>
    <relativePath/>
</parent>

<properties>
    <!-- Spring Boot 3 最低要求是 Java 17 -->
    <java.version>17</java.version>

    <!-- Dubbo 版本集中管理，实际项目可根据公司基线锁定 -->
    <dubbo.version>3.3.6</dubbo.version>

    <!-- 常用工具类库版本 -->
    <hutool.version>5.8.36</hutool.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Dubbo BOM：统一管理 Dubbo Starter、协议、注册中心等依赖版本 -->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-bom</artifactId>
            <version>${dubbo.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Provider 和 Consumer 模块中引入 Dubbo Starter。

```xml
<dependencies>
    <!-- Spring Boot Web：用于 Consumer 对外提供 REST 接口或健康检查接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Dubbo Spring Boot Starter：启用 Dubbo 注解扫描、配置绑定和 RPC 能力 -->
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-spring-boot-starter</artifactId>
    </dependency>

    <!-- Hutool：常用工具类库，处理字符串、集合、日期、Bean、JSON 等辅助逻辑 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：减少 DTO、VO、配置类样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 注册中心选择

注册中心用于存储和发现 Provider 的服务地址。Provider 启动后将服务实例注册到注册中心，Consumer 启动或调用时从注册中心订阅服务地址，再根据负载均衡策略选择具体 Provider 发起调用。Dubbo 官方文档说明，Spring Boot 应用中只需要配置 `dubbo.registry.address`，例如 `nacos://localhost:8848`；Dubbo 3.3.0 及以后注册中心可选，但生产微服务场景仍建议配置注册中心。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/registry/overview/))

常见选择如下。

| 注册中心              | 推荐程度 | 适用场景                                         | 说明                                                         |
| --------------------- | -------- | ------------------------------------------------ | ------------------------------------------------------------ |
| Nacos                 | 推荐     | Spring Cloud Alibaba、配置中心、服务发现统一治理 | 同时可承担注册中心、配置中心、元数据中心，国内 Java 微服务项目使用较多 |
| ZooKeeper             | 可选     | 已有 ZooKeeper 基础设施、传统 Dubbo 项目         | Dubbo 经典注册中心，稳定成熟，但配置管理能力不如 Nacos 一体化 |
| Kubernetes            | 可选     | 服务部署完全基于 K8s                             | 适合云原生部署，服务发现更多依赖 K8s Service 与 Dubbo 适配能力 |
| Redis / Consul / Etcd | 特定场景 | 公司已有统一基础设施                             | 需要结合团队运维能力和 Dubbo 扩展支持情况评估                |

新项目如果没有历史包袱，建议优先选择 Nacos。原因是 Nacos 可以同时承载服务注册、配置管理和元数据管理，开发环境搭建简单，也便于后续扩展动态配置、命名空间隔离、多环境管理等能力。Dubbo 官方 Starter 列表中也提供了 `dubbo-nacos-spring-boot-starter`，用于引入 Nacos 注册中心和配置中心相关依赖。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/config/spring/spring-boot/))

使用 Nacos 时，Provider 和 Consumer 可以配置相同的注册中心地址。

```yaml
dubbo:
  application:
    # 当前 Dubbo 应用名称，注册中心中用于区分服务应用
    name: order-provider

  protocol:
    # Dubbo 协议名称，常见为 dubbo 或 tri
    name: dubbo
    # Provider 暴露 RPC 服务端口，避免与 server.port 冲突
    port: 20880

  registry:
    # Nacos 注册中心地址，生产环境建议使用域名或集群地址
    address: nacos://127.0.0.1:8848
```

如果项目已有 ZooKeeper 基础设施，可以选择 ZooKeeper 注册中心。Dubbo 官方 Starter 中提供了 ZooKeeper 相关 Starter，其中 `dubbo-zookeeper-curator5-spring-boot-starter` 用于引入 ZooKeeper 与 Curator5 相关依赖。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/config/spring/spring-boot/))

```yaml
dubbo:
  application:
    # 当前 Dubbo 应用名称
    name: order-provider

  protocol:
    # 使用 Dubbo 协议暴露服务
    name: dubbo
    port: 20880

  registry:
    # ZooKeeper 注册中心地址
    address: zookeeper://127.0.0.1:2181
```

注册中心选择建议遵循三个原则。

第一，优先选择团队已经具备运维经验的组件。注册中心属于核心基础设施，一旦不可用会影响服务发布、服务发现和调用链路稳定性。

第二，开发、测试、生产环境保持同一类注册中心。不要本地使用直连、测试使用 ZooKeeper、生产使用 Nacos，否则联调问题和环境差异会明显增加。

第三，注册中心地址、命名空间、分组、用户名、密码等信息应通过多环境配置或环境变量管理，不要硬编码在业务代码中。



## 项目结构设计

本节用于确定 Spring Boot 3 + Apache Dubbo 项目的工程拆分方式。Dubbo 项目建议使用 Maven 多模块结构，将公共接口、服务提供者、服务消费者分离，避免 Provider 与 Consumer 直接依赖彼此的业务实现。

推荐使用如下结构：

```text
apache-dubbo-demo
├── pom.xml
├── dubbo-api
│   ├── pom.xml
│   └── src
│       └── main
│           └── java
│               └── io
│                   └── github
│                       └── atengk
│                           └── dubbo
│                               └── api
│                                   ├── dto
│                                   │   └── UserDTO.java
│                                   └── service
│                                       └── UserDubboService.java
├── dubbo-provider
│   ├── pom.xml
│   └── src
│       └── main
│           ├── java
│           │   └── io
│           │       └── github
│           │           └── atengk
│           │               └── dubbo
│           │                   └── provider
│           │                       ├── ProviderApplication.java
│           │                       └── service
│           │                           └── UserDubboServiceImpl.java
│           └── resources
│               └── application.yml
└── dubbo-consumer
    ├── pom.xml
    └── src
        └── main
            ├── java
            │   └── io
            │       └── github
            │           └── atengk
            │               └── dubbo
            │                   └── consumer
            │                       ├── ConsumerApplication.java
            │                       └── controller
            │                           └── UserController.java
            └── resources
                └── application.yml
```

这个结构中，`dubbo-api` 是服务契约模块，`dubbo-provider` 是服务提供者模块，`dubbo-consumer` 是服务消费者模块。Provider 和 Consumer 都依赖 API 模块，但 Provider 与 Consumer 之间不能互相依赖。

### 父工程结构

父工程用于统一管理模块、依赖版本、插件版本和公共构建参数。父工程不写业务代码，也不需要 `src/main/java` 目录。

父工程主要承担以下职责：

| 职责          | 说明                                                         |
| ------------- | ------------------------------------------------------------ |
| 模块聚合      | 通过 `<modules>` 管理 `dubbo-api`、`dubbo-provider`、`dubbo-consumer` |
| 版本统一      | 统一 Spring Boot、Dubbo、Hutool、Lombok 等版本               |
| 依赖管理      | 通过 `<dependencyManagement>` 控制子模块依赖版本             |
| 插件管理      | 统一 Maven 编译插件、打包插件、测试插件                      |
| Java 版本统一 | 所有模块使用相同 JDK 编译版本                                |

父工程 `pom.xml` 推荐写法如下。

文件位置：`apache-dubbo-demo/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3 父工程，统一管理 Spring 生态依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.6</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>apache-dubbo-demo</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <name>apache-dubbo-demo</name>
    <description>Spring Boot 3 Apache Dubbo 多模块示例工程</description>

    <modules>
        <!-- 公共接口模块 -->
        <module>dubbo-api</module>

        <!-- 服务提供者模块 -->
        <module>dubbo-provider</module>

        <!-- 服务消费者模块 -->
        <module>dubbo-consumer</module>
    </modules>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17 -->
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>

        <!-- 项目源码编码 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Dubbo 版本集中管理 -->
        <dubbo.version>3.3.6</dubbo.version>

        <!-- 常用工具类库 -->
        <hutool.version>5.8.36</hutool.version>

        <!-- Lombok 版本由 Spring Boot 父工程托管时可不单独指定 -->
        <lombok.version>1.18.32</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Dubbo BOM：统一管理 Dubbo 核心、Starter、注册中心相关依赖版本 -->
            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo-bom</artifactId>
                <version>${dubbo.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Hutool：统一管理工具包版本 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>

            <!-- Lombok：统一管理注解工具版本 -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <!-- Maven 编译插件：统一 Java 编译版本 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <source>${maven.compiler.source}</source>
                        <target>${maven.compiler.target}</target>
                        <encoding>${project.build.sourceEncoding}</encoding>
                    </configuration>
                </plugin>

                <!-- Spring Boot 打包插件：Provider 和 Consumer 模块使用 -->
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

</project>
```

父工程只负责聚合和版本控制，不建议在父工程中直接引入业务依赖。公共依赖应优先放到具体模块中，只有确实需要统一管理版本的依赖才放入 `dependencyManagement`。

### API 模块

API 模块用于存放 Provider 和 Consumer 共同依赖的服务契约。这个模块只定义接口、DTO、VO、枚举、常量和必要的公共异常，不应该包含服务实现、数据库访问、Controller、Spring Boot 启动类或业务配置。

API 模块的核心原则是“稳定、轻量、可复用”。Consumer 调用 Dubbo 服务时，实际依赖的是 API 模块中的接口，而不是 Provider 的实现类。

API 模块建议包含以下内容：

| 内容           | 示例                            | 说明                         |
| -------------- | ------------------------------- | ---------------------------- |
| Dubbo 服务接口 | `UserDubboService`              | Provider 实现，Consumer 引用 |
| 请求 DTO       | `UserQueryDTO`、`UserCreateDTO` | 远程调用入参                 |
| 响应 DTO       | `UserDTO`                       | 远程调用返回值               |
| 枚举           | `UserStatusEnum`                | 统一业务状态                 |
| 常量           | `DubboVersionConstant`          | 服务版本、分组等公共配置     |
| 公共异常       | `RemoteServiceException`        | 可选，封装业务异常           |

API 模块 `pom.xml` 推荐保持轻量。

文件位置：`apache-dubbo-demo/dubbo-api/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>apache-dubbo-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>dubbo-api</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Lombok：简化 DTO 对象 getter、setter、builder 等代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Hutool：API 模块中如需常量校验、字符串处理时可使用 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
    </dependencies>

</project>
```

API 模块中可以先定义一个最小服务接口，后续 Provider 和 Consumer 都基于这个接口开发。

文件位置：`apache-dubbo-demo/dubbo-api/src/main/java/io/github/atengk/dubbo/api/dto/UserDTO.java`

```java
package io.github.atengk.dubbo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户传输对象
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

}
```

文件位置：`apache-dubbo-demo/dubbo-api/src/main/java/io/github/atengk/dubbo/api/service/UserDubboService.java`

```java
package io.github.atengk.dubbo.api.service;

import io.github.atengk.dubbo.api.dto.UserDTO;

/**
 * 用户 Dubbo 服务接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserDTO getUserById(Long id);

}
```

这里的 `UserDTO` 必须实现 `Serializable`，因为 Dubbo 远程调用需要对入参和返回值进行序列化。实际业务中，DTO 字段应尽量使用稳定的数据类型，例如 `String`、`Long`、`Integer`、`BigDecimal`、`LocalDateTime` 等，避免直接暴露数据库实体对象。

### Provider 服务提供者模块

Provider 模块用于实现 API 模块中的 Dubbo 接口，并将服务注册到注册中心。Provider 只关心服务实现、业务处理、数据库访问和服务暴露，不应该依赖 Consumer 模块。

Provider 模块通常包含以下内容：

| 内容                | 说明                                 |
| ------------------- | ------------------------------------ |
| 启动类              | Spring Boot 启动入口                 |
| Dubbo 服务实现      | 使用 `@DubboService` 暴露远程服务    |
| 业务 Service        | 承载具体业务逻辑                     |
| Mapper / Repository | 数据库访问，可选                     |
| 配置文件            | 配置应用名、Dubbo 协议、注册中心地址 |
| 健康检查接口        | 可选，便于本地或容器探活             |

Provider 模块依赖 `dubbo-api`，并引入 Dubbo Starter 与注册中心依赖。

文件位置：`apache-dubbo-demo/dubbo-provider/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>apache-dubbo-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>dubbo-provider</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- 公共 API 模块：包含 Dubbo 服务接口和 DTO -->
        <dependency>
            <groupId>io.github.atengk</groupId>
            <artifactId>dubbo-api</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Boot 基础启动依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Spring Boot Web：可选，用于健康检查、Actuator 或管理接口 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Dubbo Spring Boot Starter：支持 @DubboService 服务暴露 -->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
        </dependency>

        <!-- Nacos 注册中心 Starter：使用 Nacos 作为服务注册中心 -->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-nacos-spring-boot-starter</artifactId>
        </dependency>

        <!-- Hutool：业务开发常用工具类 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- Lombok：简化日志、构造器、Getter、Setter 等代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件：生成可执行 jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

Provider 的启动类保持简洁即可。

文件位置：`apache-dubbo-demo/dubbo-provider/src/main/java/io/github/atengk/dubbo/provider/ProviderApplication.java`

```java
package io.github.atengk.dubbo.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dubbo 服务提供者启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@SpringBootApplication
public class ProviderApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }

}
```

Provider 服务实现类使用 `@DubboService` 暴露服务，Consumer 通过 API 接口进行远程调用。

文件位置：`apache-dubbo-demo/dubbo-provider/src/main/java/io/github/atengk/dubbo/provider/service/UserDubboServiceImpl.java`

```java
package io.github.atengk.dubbo.provider.service;

import cn.hutool.core.lang.Assert;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 用户 Dubbo 服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@DubboService(version = "1.0.0", group = "user")
public class UserDubboServiceImpl implements UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UserDTO getUserById(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        log.info("收到用户查询请求，用户ID：{}", id);

        return UserDTO.builder()
                .id(id)
                .username("ateng")
                .phone("13800000000")
                .build();
    }

}
```

### Consumer 服务消费者模块

Consumer 模块用于引用 Dubbo 远程服务，并对外提供 REST 接口、定时任务、消息消费逻辑或其他业务入口。Consumer 依赖 `dubbo-api`，通过 `@DubboReference` 注入远程服务代理。

Consumer 模块通常包含以下内容：

| 内容            | 说明                                   |
| --------------- | -------------------------------------- |
| 启动类          | Spring Boot 启动入口                   |
| Controller      | 对外提供 HTTP 接口                     |
| Dubbo Reference | 使用 `@DubboReference` 引用远程服务    |
| 业务 Service    | 编排多个远程服务调用，可选             |
| 配置文件        | 配置应用名、注册中心地址、消费端超时等 |
| 异常处理        | 封装远程调用异常，可选                 |

Consumer 模块依赖 `dubbo-api`，不依赖 `dubbo-provider`。

文件位置：`apache-dubbo-demo/dubbo-consumer/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.atengk</groupId>
        <artifactId>apache-dubbo-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>dubbo-consumer</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- 公共 API 模块：Consumer 通过接口引用远程 Dubbo 服务 -->
        <dependency>
            <groupId>io.github.atengk</groupId>
            <artifactId>dubbo-api</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Boot Web：提供 REST API，便于浏览器、Postman、curl 调用 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Dubbo Spring Boot Starter：支持 @DubboReference 服务引用 -->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
        </dependency>

        <!-- Nacos 注册中心 Starter：从 Nacos 发现 Provider 服务地址 -->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-nacos-spring-boot-starter</artifactId>
        </dependency>

        <!-- Hutool：常用参数校验、字符串处理、集合处理工具 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- Lombok：简化日志和样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件：生成可执行 jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

Consumer 启动类如下。

文件位置：`apache-dubbo-demo/dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/ConsumerApplication.java`

```java
package io.github.atengk.dubbo.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dubbo 服务消费者启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@SpringBootApplication
public class ConsumerApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

}
```

Consumer 可以通过 REST 接口调用 Dubbo 远程服务，便于本地联调和前后端接口验证。

文件位置：`apache-dubbo-demo/dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/controller/UserController.java`

```java
package io.github.atengk.dubbo.consumer.controller;

import cn.hutool.core.lang.Assert;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    @DubboReference(version = "1.0.0", group = "user", timeout = 3000)
    private UserDubboService userDubboService;

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/users/{id}")
    public UserDTO getUserById(@PathVariable Long id) {
        Assert.notNull(id, "用户ID不能为空");

        log.info("开始调用用户 Dubbo 服务，用户ID：{}", id);
        UserDTO user = userDubboService.getUserById(id);
        log.info("用户 Dubbo 服务调用完成，用户ID：{}", id);

        return user;
    }

}
```

启动顺序建议先启动注册中心，再启动 Provider，最后启动 Consumer。Consumer 成功启动后，可以通过 HTTP 接口触发远程调用。

```bash
# 在父工程目录执行编译
mvn clean package -DskipTests

# 启动 Provider
java -jar dubbo-provider/target/dubbo-provider-1.0.0.jar

# 启动 Consumer
java -jar dubbo-consumer/target/dubbo-consumer-1.0.0.jar

# 调用 Consumer REST 接口，内部会触发 Dubbo 远程调用
curl http://127.0.0.1:8081/users/1
```

其中 `mvn clean package -DskipTests` 用于编译并打包所有子模块；两个 `java -jar` 命令分别启动 Provider 和 Consumer；`curl` 请求访问 Consumer 暴露的 HTTP 接口，由 Consumer 内部调用 Provider 暴露的 Dubbo 服务。

## 依赖配置

本节用于统一说明 Spring Boot 3 + Apache Dubbo 项目中常用依赖的选择方式。实际项目中建议由父工程统一管理版本，子模块只声明自己需要的依赖，避免版本散落在多个 `pom.xml` 中。

依赖配置可以分为三类：Spring Boot 基础依赖、Dubbo Starter 依赖、注册中心依赖。

### Spring Boot 基础依赖

Spring Boot 基础依赖用于提供应用启动、Web 接口、参数校验、日志、健康检查和配置管理能力。Dubbo Provider 不一定必须引入 Web，如果 Provider 只暴露 RPC 服务，可以只引入 `spring-boot-starter`；如果需要健康检查、Actuator 或本地测试接口，则可以引入 `spring-boot-starter-web`。

常用基础依赖如下：

| 依赖                             | 适用模块                       | 作用                         |
| -------------------------------- | ------------------------------ | ---------------------------- |
| `spring-boot-starter`            | Provider / Consumer            | 基础启动、日志、自动配置     |
| `spring-boot-starter-web`        | Consumer 常用，Provider 可选   | 提供 REST API、内置 Web 容器 |
| `spring-boot-starter-validation` | API / Consumer / Provider 可选 | 参数校验                     |
| `spring-boot-starter-actuator`   | Provider / Consumer 可选       | 健康检查、指标暴露           |
| `lombok`                         | 所有模块可选                   | 简化实体类和日志代码         |
| `hutool-all`                     | 所有模块可选                   | 常用工具类                   |

推荐在 Provider 和 Consumer 中按需引入基础依赖。

```xml
<dependencies>
    <!-- Spring Boot 基础启动依赖，提供自动配置、日志等基础能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>

    <!-- Spring Boot Web，Consumer 对外提供 REST 接口时需要 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验依赖，用于 Controller、DTO、配置属性校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Actuator，用于健康检查、运行状态、指标暴露 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Hutool，提供常用工具类，减少重复工具代码 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>

    <!-- Lombok，简化 DTO、日志、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

如果 Provider 不需要 HTTP 接口，可以去掉 `spring-boot-starter-web`，只保留 `spring-boot-starter`。如果 Consumer 需要对外提供 HTTP API，则必须保留 `spring-boot-starter-web`。

### Dubbo Starter 依赖

Dubbo Starter 是 Spring Boot 项目接入 Dubbo 的核心依赖。它负责 Dubbo 配置绑定、注解扫描、服务暴露、服务引用和 Spring 容器集成。

Provider 和 Consumer 都需要引入：

```xml
<!-- Dubbo Spring Boot Starter：启用 Dubbo 与 Spring Boot 集成能力 -->
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
</dependency>
```

Provider 使用 `@DubboService` 暴露服务。

```java
package io.github.atengk.dubbo.provider.service;

import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 用户服务提供者示例
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@DubboService(version = "1.0.0", group = "user")
public class UserProviderDemoService implements UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UserDTO getUserById(Long id) {
        log.info("Provider 接收到远程调用，用户ID：{}", id);

        return UserDTO.builder()
                .id(id)
                .username("provider-user")
                .phone("13800000001")
                .build();
    }

}
```

Consumer 使用 `@DubboReference` 引用服务。

```java
package io.github.atengk.dubbo.consumer.controller;

import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户消费端示例接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class UserConsumerDemoController {

    @DubboReference(version = "1.0.0", group = "user", timeout = 3000, retries = 0)
    private UserDubboService userDubboService;

    /**
     * 测试 Dubbo 远程调用
     *
     * @return 用户信息
     */
    @GetMapping("/demo/user")
    public UserDTO getDemoUser() {
        log.info("Consumer 开始调用 Provider 用户服务");
        return userDubboService.getUserById(1L);
    }

}
```

`@DubboService` 和 `@DubboReference` 中的 `version`、`group` 必须保持一致，否则 Consumer 可能无法找到对应 Provider。`timeout` 用于控制远程调用超时时间，`retries = 0` 表示失败后不自动重试，适合写操作或不希望重复提交的接口。

### 注册中心依赖

注册中心依赖用于让 Provider 注册服务、Consumer 发现服务。实际项目中常用 Nacos 或 ZooKeeper。新项目优先推荐 Nacos；如果公司已有 ZooKeeper 基础设施，也可以继续使用 ZooKeeper。

使用 Nacos 注册中心时，引入以下依赖：

```xml
<!-- Nacos 注册中心 Starter：用于服务注册、服务发现，可配合 Nacos 配置中心使用 -->
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-nacos-spring-boot-starter</artifactId>
</dependency>
```

Provider 配置示例：

文件位置：`apache-dubbo-demo/dubbo-provider/src/main/resources/application.yml`

```yaml
server:
  # Provider HTTP 端口，仅用于健康检查或管理接口，不是 Dubbo RPC 端口
  port: 8080

spring:
  application:
    # Spring Boot 应用名称
    name: dubbo-provider

dubbo:
  application:
    # Dubbo 应用名称，注册中心中用于识别当前应用
    name: dubbo-provider

  protocol:
    # Dubbo RPC 协议名称
    name: dubbo
    # Dubbo RPC 暴露端口，不能与 server.port 冲突
    port: 20880

  registry:
    # Nacos 注册中心地址
    address: nacos://127.0.0.1:8848

  provider:
    # Provider 默认超时时间，单位毫秒
    timeout: 3000
```

Consumer 配置示例：

文件位置：`apache-dubbo-demo/dubbo-consumer/src/main/resources/application.yml`

```yaml
server:
  # 文件位置 Consumer 对外提供 REST 接口的端口
  port: 8081

spring:
  application:
    # Spring Boot 应用名称
    name: dubbo-consumer

dubbo:
  application:
    # Dubbo 应用名称，注册中心中用于识别当前应用
    name: dubbo-consumer

  registry:
    # Consumer 通过同一个注册中心发现 Provider
    address: nacos://127.0.0.1:8848

  consumer:
    # 启动时是否检查 Provider 是否存在；本地开发建议 false，生产可按需调整
    check: false
    # Consumer 默认调用超时时间，单位毫秒
    timeout: 3000
    # Consumer 默认重试次数，写操作建议为 0
    retries: 0
```

使用 ZooKeeper 注册中心时，可以替换为 ZooKeeper 依赖：

```xml
<!-- ZooKeeper 注册中心 Starter：使用 ZooKeeper 作为服务注册中心 -->
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-zookeeper-curator5-spring-boot-starter</artifactId>
</dependency>
```

对应配置如下：

```yaml
dubbo:
  registry:
    # ZooKeeper 注册中心地址
    address: zookeeper://127.0.0.1:2181
```

注册中心依赖只需要选择一种即可。不要在没有明确隔离设计的情况下同时引入 Nacos 和 ZooKeeper 注册中心依赖，否则容易造成配置混乱、服务注册到错误注册中心或 Consumer 订阅不到 Provider。

实际项目中建议将注册中心地址改为环境变量形式：

```yaml
dubbo:
  registry:
    # 通过环境变量注入注册中心地址，默认使用本地 Nacos
    address: ${DUBBO_REGISTRY_ADDRESS:nacos://127.0.0.1:8848}
```

启动时可以通过命令覆盖注册中心地址：

```bash
# 使用指定 Nacos 注册中心启动 Provider
java -jar dubbo-provider/target/dubbo-provider-1.0.0.jar \
  --DUBBO_REGISTRY_ADDRESS=nacos://192.168.1.100:8848

# 使用指定 Nacos 注册中心启动 Consumer
java -jar dubbo-consumer/target/dubbo-consumer-1.0.0.jar \
  --DUBBO_REGISTRY_ADDRESS=nacos://192.168.1.100:8848
```

这里通过 `--DUBBO_REGISTRY_ADDRESS=...` 覆盖 `application.yml` 中的默认值，适合本地、测试、预发、生产环境共用同一份配置文件。



## 公共接口定义

公共接口定义是 Dubbo 项目的契约层。Provider 和 Consumer 通过同一套 API 模块完成远程调用约束，因此 API 模块必须保持稳定、轻量、清晰，不能混入 Provider 的业务实现、数据库实体、Controller、Mapper 或 Spring Boot 启动类。

在多模块项目中，公共接口通常放在 `dubbo-api` 模块中，Provider 和 Consumer 都依赖该模块。

```text
dubbo-api
└── src
    └── main
        └── java
            └── io
                └── github
                    └── atengk
                        └── dubbo
                            └── api
                                ├── constant
                                │   └── DubboVersionConstant.java
                                ├── dto
                                │   ├── PageResultDTO.java
                                │   ├── UserCreateDTO.java
                                │   ├── UserDTO.java
                                │   └── UserQueryDTO.java
                                └── service
                                    └── UserDubboService.java
```

API 模块建议只放以下内容：

| 类型     | 说明                                        |
| -------- | ------------------------------------------- |
| 服务接口 | Dubbo 远程服务接口，例如 `UserDubboService` |
| 请求 DTO | Consumer 调用 Provider 时传入的参数对象     |
| 响应 DTO | Provider 返回给 Consumer 的数据对象         |
| 分页 DTO | 跨服务分页查询结果对象                      |
| 枚举     | 稳定的业务枚举，例如状态、类型、来源        |
| 常量     | 服务分组、版本号、接口标识等公共常量        |
| 公共异常 | 可选，通常用于业务异常码或跨服务异常封装    |

### DTO 对象设计

DTO 是 Provider 和 Consumer 之间传输数据的对象。Dubbo 远程调用会对入参和返回值进行序列化，因此 DTO 必须具备稳定的字段结构，并实现 `Serializable`。

DTO 设计需要遵循以下原则：

| 设计原则                   | 说明                                                         |
| -------------------------- | ------------------------------------------------------------ |
| 必须实现 `Serializable`    | 保证对象可以被 Dubbo 序列化和反序列化                        |
| 不直接暴露数据库实体       | 避免数据库字段变化影响远程接口契约                           |
| 不使用 Web 对象            | 不要在 DTO 中使用 `HttpServletRequest`、`MultipartFile`、`ResponseEntity` 等对象 |
| 字段类型保持稳定           | 优先使用 `String`、`Long`、`Integer`、`BigDecimal`、`LocalDateTime` 等通用类型 |
| 避免复杂泛型嵌套           | 降低序列化兼容风险                                           |
| 参数超过两个时使用请求 DTO | 避免接口参数不断膨胀                                         |
| 保持向后兼容               | 新增字段通常可以，删除字段和修改字段类型需要谨慎             |

下面给出一组常用 DTO 示例，包括用户响应对象、用户查询对象、用户创建对象和分页结果对象。

这段代码定义用户响应对象，Provider 返回用户信息时使用。

文件位置：`dubbo-api/src/main/java/io/github/atengk/dubbo/api/dto/UserDTO.java`

```java
package io.github.atengk.dubbo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息传输对象
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
```

这段代码定义用户查询请求对象，适合用于列表查询、分页查询和条件查询。

文件位置：`dubbo-api/src/main/java/io/github/atengk/dubbo/api/dto/UserQueryDTO.java`

```java
package io.github.atengk.dubbo.api.dto;

import cn.hutool.core.lang.Assert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户查询请求对象
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名称，支持模糊查询
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户状态
     */
    private Integer status;

    /**
     * 当前页码，从 1 开始
     */
    private Integer pageNum;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 校验分页参数
     */
    public void validatePageParam() {
        Assert.notNull(pageNum, "当前页码不能为空");
        Assert.notNull(pageSize, "每页条数不能为空");
        Assert.isTrue(pageNum > 0, "当前页码必须大于0");
        Assert.isTrue(pageSize > 0 && pageSize <= 100, "每页条数必须在1到100之间");
    }

}
```

这段代码定义用户创建请求对象，适合用于新增用户远程调用。

文件位置：`dubbo-api/src/main/java/io/github/atengk/dubbo/api/dto/UserCreateDTO.java`

```java
package io.github.atengk.dubbo.api.dto;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户创建请求对象
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户状态
     */
    private Integer status;

    /**
     * 校验创建参数
     */
    public void validateParam() {
        Assert.isTrue(StrUtil.isNotBlank(username), "用户名称不能为空");
        Assert.isTrue(StrUtil.length(username) <= 50, "用户名称长度不能超过50");
        Assert.isTrue(StrUtil.isNotBlank(phone), "手机号不能为空");
        Assert.isTrue(ReUtil.isMatch("^1[3-9]\\d{9}$", phone), "手机号格式不正确");
        Assert.notNull(status, "用户状态不能为空");
    }

}
```

这段代码定义通用分页结果对象，适合 Dubbo 服务返回分页数据。

文件位置：`dubbo-api/src/main/java/io/github/atengk/dubbo/api/dto/PageResultDTO.java`

```java
package io.github.atengk.dubbo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果传输对象
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResultDTO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总条数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 创建空分页结果
     *
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @param <T> 数据类型
     * @return 空分页结果
     */
    public static <T> PageResultDTO<T> empty(Integer pageNum, Integer pageSize) {
        return PageResultDTO.<T>builder()
                .records(Collections.emptyList())
                .total(0L)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }

}
```

DTO 不建议直接复用数据库 Entity。例如 `UserEntity` 可能包含密码、删除标识、租户字段、内部备注等字段，这些字段不应该暴露给 Consumer。正确做法是 Provider 内部将 Entity 转换为 DTO 后再返回。

### Dubbo 服务接口定义

Dubbo 服务接口是 Provider 和 Consumer 的调用契约。Provider 实现接口，Consumer 引用接口。接口方法需要表达清楚业务语义，参数和返回值要尽量稳定。

服务接口设计建议遵循以下规则：

| 规则                                | 说明                                                   |
| ----------------------------------- | ------------------------------------------------------ |
| 接口名体现业务领域                  | 例如 `UserDubboService`、`OrderDubboService`           |
| 方法名体现业务动作                  | 例如 `getUserById`、`createUser`、`pageUsers`          |
| 查询单个对象返回 DTO                | 查不到数据时可以返回 `null` 或抛业务异常，需要团队统一 |
| 查询列表返回 `List<DTO>` 或分页对象 | 分页建议使用自定义 `PageResultDTO`                     |
| 写操作返回主键或操作结果            | 例如返回 `Long`、`Boolean` 或业务结果对象              |
| 不在接口中使用 Controller 层对象    | Dubbo 接口不应该感知 HTTP                              |
| 不直接抛底层异常                    | 不要将数据库异常、中间件异常直接作为接口契约的一部分   |

这段代码定义用户 Dubbo 服务接口，包含查询、分页、新增和状态变更等常见操作。

文件位置：`dubbo-api/src/main/java/io/github/atengk/dubbo/api/service/UserDubboService.java`

```java
package io.github.atengk.dubbo.api.service;

import io.github.atengk.dubbo.api.dto.PageResultDTO;
import io.github.atengk.dubbo.api.dto.UserCreateDTO;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.dto.UserQueryDTO;

import java.util.List;

/**
 * 用户 Dubbo 服务接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息，不存在时返回 null
     */
    UserDTO getUserById(Long id);

    /**
     * 根据用户ID列表批量查询用户信息
     *
     * @param ids 用户ID列表
     * @return 用户信息列表
     */
    List<UserDTO> listUsersByIds(List<Long> ids);

    /**
     * 分页查询用户信息
     *
     * @param query 查询条件
     * @return 分页用户信息
     */
    PageResultDTO<UserDTO> pageUsers(UserQueryDTO query);

    /**
     * 创建用户
     *
     * @param createDTO 用户创建参数
     * @return 新增用户ID
     */
    Long createUser(UserCreateDTO createDTO);

    /**
     * 修改用户状态
     *
     * @param id 用户ID
     * @param status 用户状态
     * @return 是否修改成功
     */
    Boolean updateUserStatus(Long id, Integer status);

}
```

Provider 实现该接口时，负责业务校验、数据库操作和异常转换。Consumer 调用该接口时，只依赖接口和 DTO，不关心 Provider 内部实现。

Provider 侧实现类示例：

文件位置：`dubbo-provider/src/main/java/io/github/atengk/dubbo/provider/service/UserDubboServiceImpl.java`

```java
package io.github.atengk.dubbo.provider.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import io.github.atengk.dubbo.api.dto.PageResultDTO;
import io.github.atengk.dubbo.api.dto.UserCreateDTO;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.dto.UserQueryDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 Dubbo 服务实现类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@DubboService(version = "1.0.0", group = "user")
public class UserDubboServiceImpl implements UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息，不存在时返回 null
     */
    @Override
    public UserDTO getUserById(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        log.info("查询用户信息，用户ID：{}", id);

        return UserDTO.builder()
                .id(id)
                .username("ateng")
                .phone("13800000000")
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 根据用户ID列表批量查询用户信息
     *
     * @param ids 用户ID列表
     * @return 用户信息列表
     */
    @Override
    public List<UserDTO> listUsersByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            log.info("批量查询用户信息，用户ID列表为空");
            return List.of();
        }

        log.info("批量查询用户信息，用户数量：{}", ids.size());

        return ids.stream()
                .map(id -> UserDTO.builder()
                        .id(id)
                        .username("user-" + id)
                        .phone("13800000000")
                        .status(1)
                        .createTime(LocalDateTime.now())
                        .build())
                .toList();
    }

    /**
     * 分页查询用户信息
     *
     * @param query 查询条件
     * @return 分页用户信息
     */
    @Override
    public PageResultDTO<UserDTO> pageUsers(UserQueryDTO query) {
        Assert.notNull(query, "查询条件不能为空");
        query.validatePageParam();

        log.info("分页查询用户信息，当前页：{}，每页条数：{}", query.getPageNum(), query.getPageSize());

        List<UserDTO> records = List.of(
                UserDTO.builder()
                        .id(1L)
                        .username("ateng")
                        .phone("13800000000")
                        .status(1)
                        .createTime(LocalDateTime.now())
                        .build()
        );

        return PageResultDTO.<UserDTO>builder()
                .records(records)
                .total(1L)
                .pageNum(query.getPageNum())
                .pageSize(query.getPageSize())
                .build();
    }

    /**
     * 创建用户
     *
     * @param createDTO 用户创建参数
     * @return 新增用户ID
     */
    @Override
    public Long createUser(UserCreateDTO createDTO) {
        Assert.notNull(createDTO, "用户创建参数不能为空");
        createDTO.validateParam();

        log.info("创建用户，用户名：{}，手机号：{}", createDTO.getUsername(), createDTO.getPhone());

        return 10001L;
    }

    /**
     * 修改用户状态
     *
     * @param id 用户ID
     * @param status 用户状态
     * @return 是否修改成功
     */
    @Override
    public Boolean updateUserStatus(Long id, Integer status) {
        Assert.notNull(id, "用户ID不能为空");
        Assert.notNull(status, "用户状态不能为空");

        log.info("修改用户状态，用户ID：{}，状态：{}", id, status);

        return Boolean.TRUE;
    }

}
```

Consumer 侧引用接口时，必须保证 `version` 和 `group` 与 Provider 暴露配置一致。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/controller/UserController.java`

```java
package io.github.atengk.dubbo.consumer.controller;

import cn.hutool.core.lang.Assert;
import io.github.atengk.dubbo.api.dto.PageResultDTO;
import io.github.atengk.dubbo.api.dto.UserCreateDTO;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.dto.UserQueryDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    @DubboReference(version = "1.0.0", group = "user", timeout = 3000, retries = 0)
    private UserDubboService userDubboService;

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public UserDTO getUserById(@PathVariable Long id) {
        Assert.notNull(id, "用户ID不能为空");

        log.info("Consumer 查询用户信息，用户ID：{}", id);
        return userDubboService.getUserById(id);
    }

    /**
     * 批量查询用户信息
     *
     * @param ids 用户ID列表
     * @return 用户信息列表
     */
    @GetMapping
    public List<UserDTO> listUsersByIds(@RequestParam List<Long> ids) {
        log.info("Consumer 批量查询用户信息，用户数量：{}", ids.size());
        return userDubboService.listUsersByIds(ids);
    }

    /**
     * 分页查询用户信息
     *
     * @param query 查询条件
     * @return 分页用户信息
     */
    @PostMapping("/page")
    public PageResultDTO<UserDTO> pageUsers(@RequestBody UserQueryDTO query) {
        log.info("Consumer 分页查询用户信息");
        return userDubboService.pageUsers(query);
    }

    /**
     * 创建用户
     *
     * @param createDTO 用户创建参数
     * @return 新增用户ID
     */
    @PostMapping
    public Long createUser(@RequestBody UserCreateDTO createDTO) {
        log.info("Consumer 创建用户，用户名：{}", createDTO.getUsername());
        return userDubboService.createUser(createDTO);
    }

}
```

### 接口版本规划

Dubbo 接口一旦被多个 Consumer 使用，就需要进行版本规划。版本规划的目标是保证接口升级时不影响旧 Consumer，同时让新 Consumer 可以逐步迁移到新版本。

Dubbo 常用的隔离维度有两个：`version` 和 `group`。

| 配置      | 作用                                             | 示例                      |
| --------- | ------------------------------------------------ | ------------------------- |
| `version` | 区分接口版本，适合接口升级和兼容演进             | `1.0.0`、`1.1.0`、`2.0.0` |
| `group`   | 区分服务分组，适合业务域、租户、环境或多实现隔离 | `user`、`order`、`gray`   |

建议将版本和分组定义为公共常量，避免字符串散落在 Provider 和 Consumer 代码中。

文件位置：`dubbo-api/src/main/java/io/github/atengk/dubbo/api/constant/DubboVersionConstant.java`

```java
package io.github.atengk.dubbo.api.constant;

/**
 * Dubbo 服务版本常量
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class DubboVersionConstant {

    /**
     * 用户服务分组
     */
    public static final String USER_GROUP = "user";

    /**
     * 用户服务 1.0.0 版本
     */
    public static final String USER_VERSION_V1 = "1.0.0";

    /**
     * 用户服务 1.1.0 版本
     */
    public static final String USER_VERSION_V1_1 = "1.1.0";

    /**
     * 用户服务 2.0.0 版本
     */
    public static final String USER_VERSION_V2 = "2.0.0";

    private DubboVersionConstant() {
    }

}
```

Provider 暴露服务时使用公共常量。

```java
package io.github.atengk.dubbo.provider.service;

import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 用户 Dubbo V1 服务实现类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@DubboService(
        version = DubboVersionConstant.USER_VERSION_V1,
        group = DubboVersionConstant.USER_GROUP
)
public class UserDubboV1ServiceImpl implements UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UserDTO getUserById(Long id) {
        log.info("调用用户服务 V1，用户ID：{}", id);
        return UserDTO.builder().id(id).username("v1-user").build();
    }

}
```

Consumer 引用服务时也使用相同常量。

```java
package io.github.atengk.dubbo.consumer.controller;

import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户版本调用示例控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
public class UserVersionController {

    @DubboReference(
            version = DubboVersionConstant.USER_VERSION_V1,
            group = DubboVersionConstant.USER_GROUP,
            timeout = 3000,
            retries = 0
    )
    private UserDubboService userDubboService;

    /**
     * 查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/version/users/{id}")
    public UserDTO getUserById(@PathVariable Long id) {
        return userDubboService.getUserById(id);
    }

}
```

接口版本演进建议如下：

| 变更类型           | 是否兼容     | 建议处理方式                                   |
| ------------------ | ------------ | ---------------------------------------------- |
| DTO 新增非必填字段 | 通常兼容     | 可保持原版本                                   |
| DTO 删除字段       | 不兼容风险高 | 新增版本                                       |
| DTO 修改字段类型   | 不兼容       | 新增版本                                       |
| 接口新增方法       | 通常兼容     | 可保持原版本，但需要确认 Consumer 依赖更新策略 |
| 修改方法参数       | 不兼容       | 新增方法或新增版本                             |
| 修改返回值结构     | 不兼容       | 新增版本                                       |
| 修改业务语义       | 不兼容风险高 | 新增版本并保留旧版本过渡                       |
| 删除方法           | 不兼容       | 先废弃，再下线                                 |

推荐版本规划方式：

```text
1.0.0：初始稳定版本
1.1.0：兼容性新增字段或新增方法
1.2.0：兼容性增强，例如新增查询条件
2.0.0：不兼容升级，例如修改参数结构、返回结构或核心业务语义
```

接口升级时不建议直接修改旧接口行为。更稳妥的方式是保留旧版本服务，同时新增新版本服务，让 Consumer 分批迁移。

```text
Provider 同时暴露：
- UserDubboService version = 1.0.0
- UserDubboService version = 2.0.0

Consumer 分批迁移：
- 老 Consumer 继续引用 version = 1.0.0
- 新 Consumer 引用 version = 2.0.0
- 所有 Consumer 迁移完成后，再下线 1.0.0
```

对于接口废弃，建议先使用 `@Deprecated` 标记，并在注释中说明替代方法。

```java
package io.github.atengk.dubbo.api.service;

import io.github.atengk.dubbo.api.dto.UserDTO;

/**
 * 用户 Dubbo 服务接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface UserDeprecatedDemoService {

    /**
     * 根据手机号查询用户信息
     *
     * @param phone 手机号
     * @return 用户信息
     * @deprecated 建议使用 {@link #getUserById(Long)}，手机号查询后续将下线
     */
    @Deprecated(since = "1.1.0", forRemoval = false)
    UserDTO getUserByPhone(String phone);

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserDTO getUserById(Long id);

}
```

公共接口定义阶段需要重点控制接口稳定性。DTO、接口方法、版本号和分组一旦被多个服务使用，就会变成跨服务契约，后续修改必须考虑兼容性、灰度迁移和旧 Consumer 下线周期。



## 服务提供者开发

服务提供者负责实现 `dubbo-api` 模块中定义的公共接口，并将接口实现暴露为 Dubbo 远程服务。Provider 启动后会连接注册中心，将服务名称、接口、分组、版本、协议地址、端口等信息注册进去，供 Consumer 发现和调用。

Provider 开发重点包括四部分：配置文件、服务实现类、服务暴露配置、启动与注册验证。

### Provider 配置文件

Provider 配置文件用于声明 Spring Boot 应用端口、Dubbo 应用名称、协议端口、注册中心地址、服务暴露参数和扫描路径。需要注意，`server.port` 是 HTTP 端口，`dubbo.protocol.port` 是 Dubbo RPC 端口，两者不能混淆。

这段配置用于启动 Provider，并将用户服务注册到 Nacos 注册中心。

文件位置：`dubbo-provider/src/main/resources/application.yml`

```yaml
server:
  # Provider 的 HTTP 端口，主要用于健康检查、Actuator 或临时管理接口
  port: 8080

spring:
  application:
    # Spring Boot 应用名称
    name: dubbo-provider

dubbo:
  application:
    # Dubbo 应用名称，注册中心中展示的应用标识
    name: dubbo-provider
    # Dubbo QOS 管理端口，多个 Dubbo 应用本机启动时不能重复
    qos-port: 22222
    # 本地开发可开启 QOS，生产环境按安全规范决定是否开启
    qos-enable: true

  scan:
    # 扫描 @DubboService 注解所在包
    base-packages: io.github.atengk.dubbo.provider.service

  protocol:
    # Dubbo RPC 协议名称
    name: dubbo
    # Dubbo RPC 服务端口，Consumer 实际远程调用该端口
    port: 20880

  registry:
    # 注册中心地址，默认使用本地 Nacos，可通过环境变量覆盖
    address: ${DUBBO_REGISTRY_ADDRESS:nacos://127.0.0.1:8848}

  provider:
    # Provider 默认超时时间，单位毫秒
    timeout: 3000
    # Provider 默认重试次数，Provider 侧通常不配置重试，重试更多由 Consumer 控制
    retries: 0
    # 服务负载均衡策略，多个 Provider 实例时生效
    loadbalance: random
```

如果使用 ZooKeeper 注册中心，只需要替换注册中心依赖和地址。

```yaml
dubbo:
  registry:
    # ZooKeeper 注册中心地址
    address: ${DUBBO_REGISTRY_ADDRESS:zookeeper://127.0.0.1:2181}
```

Provider 启动类需要启用 Spring Boot 应用，并可以显式启用 Dubbo 注解扫描。虽然配置文件中已经指定了 `dubbo.scan.base-packages`，但在示例项目中保留 `@EnableDubbo` 更直观，便于初学者理解 Dubbo 是被显式启用的。

这段代码是 Provider 模块启动入口。

文件位置：`dubbo-provider/src/main/java/io/github/atengk/dubbo/provider/ProviderApplication.java`

```java
package io.github.atengk.dubbo.provider;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dubbo 服务提供者启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@EnableDubbo
@SpringBootApplication
public class ProviderApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }

}
```

### 服务实现类

服务实现类用于实现 `dubbo-api` 模块中的远程接口。Provider 只实现接口，不应该让 Consumer 依赖 Provider 模块的实现类。

实现类上使用 `@DubboService` 注解后，Dubbo 会将该类注册为远程服务。`version` 和 `group` 必须与 Consumer 侧的 `@DubboReference` 保持一致，否则 Consumer 无法正确匹配服务。

这段代码实现用户 Dubbo 服务，使用 Hutool 完成参数校验和集合判断。

文件位置：`dubbo-provider/src/main/java/io/github/atengk/dubbo/provider/service/UserDubboServiceImpl.java`

```java
package io.github.atengk.dubbo.provider.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.dto.PageResultDTO;
import io.github.atengk.dubbo.api.dto.UserCreateDTO;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.dto.UserQueryDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 用户 Dubbo 服务实现类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@DubboService(
        version = DubboVersionConstant.USER_VERSION_V1,
        group = DubboVersionConstant.USER_GROUP,
        timeout = 3000,
        retries = 0
)
public class UserDubboServiceImpl implements UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息，不存在时返回 null
     */
    @Override
    public UserDTO getUserById(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        log.info("Provider 查询用户信息，用户ID：{}", id);

        if (Objects.equals(id, 0L)) {
            log.info("Provider 未查询到用户信息，用户ID：{}", id);
            return null;
        }

        return buildUserDTO(id, "ateng", "13800000000");
    }

    /**
     * 根据用户ID列表批量查询用户信息
     *
     * @param ids 用户ID列表
     * @return 用户信息列表
     */
    @Override
    public List<UserDTO> listUsersByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            log.info("Provider 批量查询用户信息，用户ID列表为空");
            return List.of();
        }

        log.info("Provider 批量查询用户信息，用户数量：{}", ids.size());

        return ids.stream()
                .filter(Objects::nonNull)
                .map(id -> buildUserDTO(id, "user-" + id, "13800000000"))
                .toList();
    }

    /**
     * 分页查询用户信息
     *
     * @param query 查询条件
     * @return 分页用户信息
     */
    @Override
    public PageResultDTO<UserDTO> pageUsers(UserQueryDTO query) {
        Assert.notNull(query, "查询条件不能为空");
        query.validatePageParam();

        log.info("Provider 分页查询用户信息，用户名：{}，页码：{}，每页条数：{}",
                query.getUsername(), query.getPageNum(), query.getPageSize());

        List<UserDTO> records = List.of(
                buildUserDTO(1L, StrUtil.blankToDefault(query.getUsername(), "ateng"), "13800000000"),
                buildUserDTO(2L, "demo-user", "13800000001")
        );

        return PageResultDTO.<UserDTO>builder()
                .records(records)
                .total((long) records.size())
                .pageNum(query.getPageNum())
                .pageSize(query.getPageSize())
                .build();
    }

    /**
     * 创建用户
     *
     * @param createDTO 用户创建参数
     * @return 新增用户ID
     */
    @Override
    public Long createUser(UserCreateDTO createDTO) {
        Assert.notNull(createDTO, "用户创建参数不能为空");
        createDTO.validateParam();

        long userId = IdUtil.getSnowflakeNextId();

        log.info("Provider 创建用户成功，用户ID：{}，用户名：{}", userId, createDTO.getUsername());

        return userId;
    }

    /**
     * 修改用户状态
     *
     * @param id 用户ID
     * @param status 用户状态
     * @return 是否修改成功
     */
    @Override
    public Boolean updateUserStatus(Long id, Integer status) {
        Assert.notNull(id, "用户ID不能为空");
        Assert.notNull(status, "用户状态不能为空");

        log.info("Provider 修改用户状态，用户ID：{}，状态：{}", id, status);

        return Boolean.TRUE;
    }

    /**
     * 构建用户传输对象
     *
     * @param id 用户ID
     * @param username 用户名称
     * @param phone 手机号
     * @return 用户传输对象
     */
    private UserDTO buildUserDTO(Long id, String username, String phone) {
        return UserDTO.builder()
                .id(id)
                .username(username)
                .phone(phone)
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
    }

}
```

实际项目中，`UserDubboServiceImpl` 不建议直接写数据库逻辑。更合理的分层是：Dubbo 服务实现类负责远程接口适配、参数校验、异常转换和日志记录；业务逻辑交给本地 `UserService`；数据库访问交给 Mapper 或 Repository。

推荐结构如下：

```text
dubbo-provider
└── src/main/java/io/github/atengk/dubbo/provider
    ├── ProviderApplication.java
    ├── dubbo
    │   └── UserDubboServiceImpl.java
    ├── service
    │   ├── UserService.java
    │   └── impl
    │       └── UserServiceImpl.java
    └── mapper
        └── UserMapper.java
```

### 服务暴露配置

服务暴露配置可以写在两个地方：注解和配置文件。注解适合声明某个服务自己的版本、分组、超时、重试等参数；配置文件适合声明 Provider 全局默认参数。

常用的 `@DubboService` 配置如下：

| 配置项           | 说明             | 示例                     |
| ---------------- | ---------------- | ------------------------ |
| `version`        | 服务版本         | `1.0.0`                  |
| `group`          | 服务分组         | `user`                   |
| `timeout`        | 服务超时时间     | `3000`                   |
| `retries`        | 重试次数         | `0`                      |
| `loadbalance`    | 负载均衡策略     | `random`                 |
| `interfaceClass` | 明确服务接口类型 | `UserDubboService.class` |

如果一个实现类只实现一个 Dubbo 接口，通常不需要显式指定 `interfaceClass`。如果实现类实现多个接口，建议明确指定接口类型。

这段代码展示显式指定服务接口的暴露方式。

文件位置：`dubbo-provider/src/main/java/io/github/atengk/dubbo/provider/service/UserDubboServiceImpl.java`

```java
package io.github.atengk.dubbo.provider.service;

import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 用户 Dubbo 服务暴露示例
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@DubboService(
        interfaceClass = UserDubboService.class,
        version = DubboVersionConstant.USER_VERSION_V1,
        group = DubboVersionConstant.USER_GROUP,
        timeout = 3000,
        retries = 0,
        loadbalance = "random"
)
public class UserDubboExposeDemoServiceImpl implements UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UserDTO getUserById(Long id) {
        log.info("Provider 暴露服务测试，用户ID：{}", id);
        return UserDTO.builder().id(id).username("expose-demo").build();
    }

}
```

如果多个服务都使用相同的超时、重试和负载均衡策略，可以放在配置文件中统一配置。

```yaml
dubbo:
  provider:
    # Provider 全局默认超时时间
    timeout: 3000
    # Provider 全局默认重试次数
    retries: 0
    # Provider 全局默认负载均衡策略
    loadbalance: random
```

实际开发中建议遵循以下约定：

| 场景             | 建议                              |
| ---------------- | --------------------------------- |
| 所有服务统一配置 | 写到 `application.yml`            |
| 单个接口特殊配置 | 写到 `@DubboService`              |
| 读接口可重试     | 可配置 `retries = 1` 或按业务评估 |
| 写接口不建议重试 | 配置 `retries = 0`                |
| 核心链路接口     | 单独设置更严格的 `timeout`        |
| 灰度或多版本服务 | 明确配置 `version` 和 `group`     |

### 启动与注册验证

启动 Provider 前，需要先保证注册中心可用。以 Nacos 为例，启动后需要确认 `127.0.0.1:8848` 可以访问，并且 Provider 配置中的 `dubbo.registry.address` 指向正确地址。

在父工程目录执行打包命令。

```bash
# 编译并打包所有 Maven 子模块
mvn clean package -DskipTests
```

该命令会先构建 `dubbo-api`，再构建 `dubbo-provider` 和 `dubbo-consumer`。如果 Provider 提示找不到 API 模块，通常是父工程模块顺序、子模块版本或本地 Maven 仓库安装存在问题。

启动 Provider。

```bash
# 启动 Provider，使用默认本地 Nacos 注册中心
java -jar dubbo-provider/target/dubbo-provider-1.0.0.jar
```

如果注册中心地址需要通过环境变量指定，可以使用下面的方式启动。

```bash
# 指定 Nacos 注册中心地址启动 Provider
java -jar dubbo-provider/target/dubbo-provider-1.0.0.jar \
  --DUBBO_REGISTRY_ADDRESS=nacos://127.0.0.1:8848
```

Provider 启动成功后，日志中通常可以看到服务暴露、协议端口绑定、注册中心连接、服务注册等信息。重点关注以下内容：

```text
Dubbo service has been exported
Register dubbo service
dubbo://
nacos://127.0.0.1:8848
```

也可以通过端口检查确认 Dubbo RPC 端口是否监听。

```bash
# 查看 Provider HTTP 端口
curl http://127.0.0.1:8080/actuator/health

# 查看 Dubbo RPC 端口是否监听
lsof -i:20880
```

如果没有引入 `spring-boot-starter-actuator`，`/actuator/health` 不会生效。此时主要通过启动日志、注册中心控制台和端口监听判断 Provider 是否正常启动。

在 Nacos 控制台中验证时，重点检查以下信息：

| 检查项     | 期望结果                                  |
| ---------- | ----------------------------------------- |
| 服务列表   | 能看到 `dubbo-provider` 或对应 Dubbo 服务 |
| 实例数量   | Provider 实例数量大于 0                   |
| 实例状态   | 实例健康                                  |
| IP 与端口  | IP 正确，Dubbo 端口为 `20880`             |
| 分组与版本 | 与 `@DubboService` 配置一致               |

## 服务消费者开发

服务消费者负责从注册中心发现 Provider 服务，并通过 Dubbo 代理对象发起远程调用。Consumer 可以是 Web 应用、定时任务、消息消费者、网关适配层或另一个业务服务。

Consumer 开发重点包括四部分：配置文件、远程服务引用、REST 接口调用 Dubbo 服务、启动与调用验证。

### Consumer 配置文件

Consumer 配置文件用于声明 HTTP 端口、Dubbo 应用名称、注册中心地址、消费端默认超时、重试次数和启动检查策略。

这段配置用于启动 Consumer，并从 Nacos 注册中心发现 Provider。

文件位置：`dubbo-consumer/src/main/resources/application.yml`

```yaml
server:
  # Consumer 对外提供 REST 接口的 HTTP 端口
  port: 8081

spring:
  application:
    # Spring Boot 应用名称
    name: dubbo-consumer

dubbo:
  application:
    # Dubbo 应用名称，注册中心中展示的消费端应用标识
    name: dubbo-consumer
    # Consumer 的 QOS 端口，本机与 Provider 同时启动时不能重复
    qos-port: 22223
    qos-enable: true

  registry:
    # 注册中心地址，需要与 Provider 使用同一个注册中心
    address: ${DUBBO_REGISTRY_ADDRESS:nacos://127.0.0.1:8848}

  consumer:
    # 启动时是否检查 Provider 存在；本地开发建议 false，避免 Provider 未启动导致 Consumer 启动失败
    check: false
    # Consumer 默认远程调用超时时间，单位毫秒
    timeout: 3000
    # 默认重试次数，写操作建议为 0，避免重复提交
    retries: 0
```

Consumer 启动类同样可以显式启用 Dubbo。

这段代码是 Consumer 模块启动入口。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/ConsumerApplication.java`

```java
package io.github.atengk.dubbo.consumer;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Dubbo 服务消费者启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@EnableDubbo
@SpringBootApplication
public class ConsumerApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

}
```

`dubbo.consumer.check=false` 适合本地开发和联调环境。如果设置为 `true`，Consumer 启动时会检查对应 Provider 是否存在；Provider 不存在时，Consumer 可能启动失败。生产环境是否开启检查，需要结合发布顺序、容器编排和服务可用性策略决定。

### 远程服务引用

Consumer 使用 `@DubboReference` 引用远程服务。引用时需要指定与 Provider 一致的 `version` 和 `group`。如果 Provider 使用了 `version = "1.0.0"`、`group = "user"`，Consumer 必须使用相同配置。

常见引用方式如下：

```java
@DubboReference(
        version = DubboVersionConstant.USER_VERSION_V1,
        group = DubboVersionConstant.USER_GROUP,
        timeout = 3000,
        retries = 0
)
private UserDubboService userDubboService;
```

常用参数说明：

| 参数          | 说明                   | 建议                 |
| ------------- | ---------------------- | -------------------- |
| `version`     | 服务版本               | 必须与 Provider 一致 |
| `group`       | 服务分组               | 必须与 Provider 一致 |
| `timeout`     | 调用超时时间           | 核心接口单独配置     |
| `retries`     | 失败重试次数           | 写操作设置为 0       |
| `check`       | 启动时是否检查服务存在 | 本地开发可设为 false |
| `loadbalance` | 负载均衡策略           | 多实例时按业务选择   |

这段代码封装了 Consumer 侧的远程调用逻辑，Controller 不直接处理过多 Dubbo 调用细节。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/service/UserRemoteService.java`

```java
package io.github.atengk.dubbo.consumer.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.dto.PageResultDTO;
import io.github.atengk.dubbo.api.dto.UserCreateDTO;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.dto.UserQueryDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户远程服务调用封装
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class UserRemoteService {

    @DubboReference(
            version = DubboVersionConstant.USER_VERSION_V1,
            group = DubboVersionConstant.USER_GROUP,
            timeout = 3000,
            retries = 0,
            check = false
    )
    private UserDubboService userDubboService;

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public UserDTO getUserById(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        log.info("Consumer 发起远程查询用户信息，用户ID：{}", id);
        return userDubboService.getUserById(id);
    }

    /**
     * 批量查询用户信息
     *
     * @param ids 用户ID列表
     * @return 用户信息列表
     */
    public List<UserDTO> listUsersByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            log.info("Consumer 批量查询用户信息，用户ID列表为空");
            return List.of();
        }

        log.info("Consumer 发起远程批量查询用户信息，用户数量：{}", ids.size());
        return userDubboService.listUsersByIds(ids);
    }

    /**
     * 分页查询用户信息
     *
     * @param query 查询条件
     * @return 分页用户信息
     */
    public PageResultDTO<UserDTO> pageUsers(UserQueryDTO query) {
        Assert.notNull(query, "查询条件不能为空");
        query.validatePageParam();

        log.info("Consumer 发起远程分页查询用户信息");
        return userDubboService.pageUsers(query);
    }

    /**
     * 创建用户
     *
     * @param createDTO 用户创建参数
     * @return 新增用户ID
     */
    public Long createUser(UserCreateDTO createDTO) {
        Assert.notNull(createDTO, "用户创建参数不能为空");
        createDTO.validateParam();

        log.info("Consumer 发起远程创建用户，用户名：{}", createDTO.getUsername());
        return userDubboService.createUser(createDTO);
    }

}
```

这种封装方式比直接在 Controller 中引用 `@DubboReference` 更利于后续扩展。比如可以在 `UserRemoteService` 中统一处理超时、异常、日志、降级、指标埋点和链路追踪。

### REST 接口调用 Dubbo 服务

Consumer 通常会对外提供 REST 接口，前端、Postman 或其他 HTTP 调用方访问 Consumer，Consumer 内部再调用 Dubbo Provider。这种结构常见于“HTTP 接入层 + Dubbo 内部服务”的系统。

调用链路如下：

```text
浏览器 / Postman / curl
        ↓ HTTP
dubbo-consumer UserController
        ↓ 本地 Spring Bean 调用
dubbo-consumer UserRemoteService
        ↓ Dubbo RPC
dubbo-provider UserDubboServiceImpl
```

这段代码提供 REST 接口，并通过 `UserRemoteService` 调用 Dubbo 服务。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/controller/UserController.java`

```java
package io.github.atengk.dubbo.consumer.controller;

import cn.hutool.core.lang.Assert;
import io.github.atengk.dubbo.api.dto.PageResultDTO;
import io.github.atengk.dubbo.api.dto.UserCreateDTO;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.dto.UserQueryDTO;
import io.github.atengk.dubbo.consumer.service.UserRemoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 REST 接口控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserRemoteService userRemoteService;

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public UserDTO getUserById(@PathVariable Long id) {
        Assert.notNull(id, "用户ID不能为空");

        log.info("REST 查询用户信息，用户ID：{}", id);
        return userRemoteService.getUserById(id);
    }

    /**
     * 批量查询用户信息
     *
     * @param ids 用户ID列表
     * @return 用户信息列表
     */
    @GetMapping
    public List<UserDTO> listUsersByIds(@RequestParam List<Long> ids) {
        log.info("REST 批量查询用户信息，用户数量：{}", ids.size());
        return userRemoteService.listUsersByIds(ids);
    }

    /**
     * 分页查询用户信息
     *
     * @param query 查询条件
     * @return 分页用户信息
     */
    @PostMapping("/page")
    public PageResultDTO<UserDTO> pageUsers(@RequestBody UserQueryDTO query) {
        log.info("REST 分页查询用户信息");
        return userRemoteService.pageUsers(query);
    }

    /**
     * 创建用户
     *
     * @param createDTO 用户创建参数
     * @return 新增用户ID
     */
    @PostMapping
    public Long createUser(@RequestBody UserCreateDTO createDTO) {
        log.info("REST 创建用户，用户名：{}", createDTO.getUsername());
        return userRemoteService.createUser(createDTO);
    }

}
```

为了让 REST 调用结果更清晰，实际项目中可以统一返回包装对象，例如 `Result<T>`。本章节为了突出 Dubbo 调用链路，直接返回 DTO 或基础类型。

如果需要补充统一返回对象，可以在 Consumer 模块中定义：

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/common/Result.java`

```java
package io.github.atengk.dubbo.consumer.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST 接口统一返回对象
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 响应码
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
     * 成功响应
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 统一响应对象
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败响应
     *
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 统一响应对象
     */
    public static <T> Result<T> fail(String message) {
        return Result.<T>builder()
                .code(500)
                .message(message)
                .build();
    }

}
```

### 启动与调用验证

启动验证建议按照“注册中心 → Provider → Consumer → REST 调用”的顺序执行。这样可以快速定位问题发生在哪一层。

先在父工程目录执行编译。

```bash
# 编译并打包所有模块
mvn clean package -DskipTests
```

启动 Provider。

```bash
# 启动服务提供者
java -jar dubbo-provider/target/dubbo-provider-1.0.0.jar
```

Provider 正常启动后，再启动 Consumer。

```bash
# 启动服务消费者
java -jar dubbo-consumer/target/dubbo-consumer-1.0.0.jar
```

如果需要指定注册中心地址，Provider 和 Consumer 必须使用同一个地址。

```bash
# 启动 Provider，指定注册中心
java -jar dubbo-provider/target/dubbo-provider-1.0.0.jar \
  --DUBBO_REGISTRY_ADDRESS=nacos://127.0.0.1:8848

# 启动 Consumer，指定同一个注册中心
java -jar dubbo-consumer/target/dubbo-consumer-1.0.0.jar \
  --DUBBO_REGISTRY_ADDRESS=nacos://127.0.0.1:8848
```

调用单个用户查询接口。

```bash
# 查询单个用户
curl http://127.0.0.1:8081/users/1
```

预期返回示例：

```json
{
  "id": 1,
  "username": "ateng",
  "phone": "13800000000",
  "status": 1,
  "createTime": "2026-05-05T10:30:00"
}
```

调用批量查询接口。

```bash
# 批量查询用户
curl "http://127.0.0.1:8081/users?ids=1,2,3"
```

调用分页查询接口。

```bash
# 分页查询用户
curl -X POST http://127.0.0.1:8081/users/page \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "pageNum": 1,
    "pageSize": 10
  }'
```

调用创建用户接口。

```bash
# 创建用户
curl -X POST http://127.0.0.1:8081/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "phone": "13800000000",
    "status": 1
  }'
```

调用成功时，日志链路大致如下：

```text
Consumer 日志：
REST 查询用户信息，用户ID：1
Consumer 发起远程查询用户信息，用户ID：1

Provider 日志：
Provider 查询用户信息，用户ID：1
```

如果调用失败，可以按以下顺序排查：

| 问题现象                | 常见原因                                             | 处理方式                                  |
| ----------------------- | ---------------------------------------------------- | ----------------------------------------- |
| Consumer 启动失败       | `check=true` 且 Provider 未启动                      | 本地开发设置 `dubbo.consumer.check=false` |
| 找不到服务              | `version` 或 `group` 不一致                          | 检查 `@DubboService` 与 `@DubboReference` |
| 连接注册中心失败        | 注册中心地址错误或注册中心未启动                     | 检查 `dubbo.registry.address`             |
| 调用超时                | Provider 处理慢、网络不通、端口未开放                | 调整 `timeout`，检查 Provider 日志和端口  |
| 序列化失败              | DTO 未实现 `Serializable` 或字段类型不兼容           | 检查 API 模块 DTO                         |
| 端口冲突                | `server.port`、`dubbo.protocol.port` 或 QOS 端口重复 | 修改对应端口配置                          |
| Consumer 调不到最新接口 | API 模块版本未更新                                   | 重新构建并确认依赖版本                    |

最小验证标准是：Provider 能注册到注册中心，Consumer 能从注册中心发现 Provider，REST 接口能返回 Provider 构造的数据，Provider 和 Consumer 日志中都能看到同一次调用记录。



## 注册中心集成

注册中心用于解决 Provider 地址动态发现问题。Provider 启动后将服务实例、应用名、接口、分组、版本、协议地址等信息注册到注册中心；Consumer 通过订阅注册中心获取 Provider 地址列表，并在调用时根据路由、负载均衡和容错策略选择具体实例。Dubbo 官方文档也将注册中心定位为服务治理的核心组件，自动服务发现是动态扩缩容、负载均衡和流量治理的基础。([Apache Dubbo](https://dubbo.apache.org/zh-cn/overview/mannual/java-sdk/reference-manual/registry/overview/?utm_source=chatgpt.com))

Spring Boot 3 + Dubbo 3 项目中，常用注册中心主要是 Nacos 和 ZooKeeper。新项目通常优先选择 Nacos；如果公司已有成熟 ZooKeeper 基础设施，也可以继续使用 ZooKeeper。

### Nacos 注册中心

Nacos 可以作为 Dubbo 的注册中心、配置中心和元数据中心使用。Dubbo 官方文档中，Spring Boot 应用接入 Nacos 时需要引入 `dubbo-spring-boot-starter` 和 `dubbo-nacos-spring-boot-starter`，并通过 `dubbo.registry.address=nacos://localhost:8848` 启用注册中心。Dubbo 3.3.0 对应推荐 Nacos 2.3.0，兼容范围为 Nacos 2.x。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/registry/nacos/?utm_source=chatgpt.com))

Provider 和 Consumer 都需要引入 Nacos 注册中心依赖。

文件位置：`dubbo-provider/pom.xml`、`dubbo-consumer/pom.xml`

```xml
<dependencies>
    <!-- Dubbo Spring Boot Starter：启用 Dubbo 与 Spring Boot 集成能力 -->
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-spring-boot-starter</artifactId>
    </dependency>

    <!-- Nacos 注册中心 Starter：使用 Nacos 作为 Dubbo 注册中心、配置中心或元数据中心 -->
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-nacos-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

Provider 使用 Nacos 的配置如下。

文件位置：`dubbo-provider/src/main/resources/application.yml`

```yaml
server:
  # Provider HTTP 端口，主要用于健康检查或管理接口
  port: 8080

spring:
  application:
    # Spring Boot 应用名称
    name: dubbo-provider

dubbo:
  application:
    # Dubbo 应用名称，Nacos 控制台中可看到该应用
    name: dubbo-provider

  protocol:
    # Dubbo RPC 协议
    name: dubbo
    # Dubbo RPC 端口，Consumer 实际调用该端口
    port: 20880

  registry:
    # Nacos 注册中心地址，生产环境建议改为集群地址或域名
    address: ${DUBBO_REGISTRY_ADDRESS:nacos://127.0.0.1:8848}
    # Nacos 命名空间，常用于环境隔离，例如 dev、test、prod
    parameters.namespace: ${NACOS_NAMESPACE:}
    # Nacos 分组，常用于同一环境下的数据分类
    group: ${NACOS_GROUP:DEFAULT_GROUP}

  provider:
    # Provider 默认超时时间，单位毫秒
    timeout: 3000
    # 写操作不建议重试，避免重复提交
    retries: 0
```

Consumer 使用 Nacos 的配置如下。

文件位置：`dubbo-consumer/src/main/resources/application.yml`

```yaml
server:
  # Consumer 对外提供 REST 接口的 HTTP 端口
  port: 8081

spring:
  application:
    # Spring Boot 应用名称
    name: dubbo-consumer

dubbo:
  application:
    # Dubbo 消费端应用名称
    name: dubbo-consumer

  registry:
    # Consumer 必须与 Provider 使用同一个注册中心地址
    address: ${DUBBO_REGISTRY_ADDRESS:nacos://127.0.0.1:8848}
    # 命名空间需要与 Provider 保持一致，否则无法发现服务
    parameters.namespace: ${NACOS_NAMESPACE:}
    # 分组需要与 Provider 保持一致
    group: ${NACOS_GROUP:DEFAULT_GROUP}

  consumer:
    # 本地开发建议 false，避免 Provider 未启动导致 Consumer 启动失败
    check: false
    # Consumer 默认调用超时时间
    timeout: 3000
    # 默认不重试
    retries: 0
```

如果 Nacos 开启了鉴权，可以将用户名和密码拼接到注册中心地址中。Dubbo 官方文档给出的形式包括 `nacos://localhost:8848?username=nacos&password=nacos`，也可以使用环境变量进行替换。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/registry/nacos/?utm_source=chatgpt.com))

```yaml
dubbo:
  registry:
    # Nacos 鉴权配置，生产环境不要将明文账号密码直接写死在代码仓库
    address: nacos://${NACOS_HOST:127.0.0.1}:${NACOS_PORT:8848}?username=${NACOS_USERNAME:nacos}&password=${NACOS_PASSWORD:nacos}
```

如果需要配置 Nacos 命名空间，推荐使用 `parameters.namespace`，这样便于通过环境变量覆盖。

```yaml
dubbo:
  registry:
    # Nacos 注册中心地址
    address: nacos://127.0.0.1:8848
    # Nacos 命名空间 ID，用于隔离 dev、test、prod 等环境
    parameters.namespace: ${NACOS_NAMESPACE:5cbb70a5-xxxx-xxxx-xxxx-d43479ae0932}
    # Nacos 分组
    group: ${NACOS_GROUP:DUBBO_GROUP}
```

Dubbo 3 默认服务发现模型与 Dubbo 2 有差异。Dubbo 官方文档说明，Dubbo 3 的服务发现从 Dubbo 2 时代的接口级服务发现演进到应用级服务发现；Nacos 文档也提到 Dubbo 3 默认存在应用级服务发现和接口级服务发现的注册行为，并可通过 `dubbo.registry.register-mode` 调整注册模式。([Apache Dubbo](https://dubbo.apache.org/zh-cn/overview/mannual/java-sdk/reference-manual/registry/overview/?utm_source=chatgpt.com))

```yaml
dubbo:
  registry:
    # 可选值通常包括 instance、interface、all
    # instance：应用级注册
    # interface：接口级注册
    # all：同时注册应用级和接口级信息
    register-mode: all
```

实际项目中建议先保持默认行为，等服务治理、监控、注册数据展示规则明确后，再统一调整 `register-mode`。

### ZooKeeper 注册中心

ZooKeeper 是 Dubbo 早期项目中非常常见的注册中心，适合已有 ZooKeeper 运维基础设施的团队。Dubbo 官方文档说明，Dubbo 3.3 及以上版本使用 ZooKeeper Server 3.5 及以上时，Spring Boot 项目应使用 `dubbo-zookeeper-curator5-spring-boot-starter`；如果使用 JDK 17，官方也建议选择 Curator5 相关依赖，并推荐 ZooKeeper Server 3.8.0 及以上版本。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/registry/zookeeper/?utm_source=chatgpt.com))

Provider 和 Consumer 使用 ZooKeeper 时，可以引入以下依赖。

文件位置：`dubbo-provider/pom.xml`、`dubbo-consumer/pom.xml`

```xml
<dependencies>
    <!-- Dubbo Spring Boot Starter：启用 Dubbo 与 Spring Boot 集成能力 -->
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-spring-boot-starter</artifactId>
    </dependency>

    <!-- ZooKeeper Curator5 Starter：适合 ZooKeeper Server 3.5+，Spring Boot 3 + JDK 17 项目优先使用 -->
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-zookeeper-curator5-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

Provider 使用 ZooKeeper 的配置如下。

文件位置：`dubbo-provider/src/main/resources/application.yml`

```yaml
server:
  # Provider HTTP 端口
  port: 8080

spring:
  application:
    # Spring Boot 应用名称
    name: dubbo-provider

dubbo:
  application:
    # Dubbo 应用名称
    name: dubbo-provider

  protocol:
    # Dubbo RPC 协议
    name: dubbo
    # Dubbo RPC 端口
    port: 20880

  registry:
    # ZooKeeper 注册中心地址
    address: ${DUBBO_REGISTRY_ADDRESS:zookeeper://127.0.0.1:2181}

  provider:
    # Provider 默认超时时间
    timeout: 3000
    # Provider 默认重试次数
    retries: 0
```

Consumer 使用 ZooKeeper 的配置如下。

文件位置：`dubbo-consumer/src/main/resources/application.yml`

```yaml
server:
  # Consumer REST 接口端口
  port: 8081

spring:
  application:
    # Spring Boot 应用名称
    name: dubbo-consumer

dubbo:
  application:
    # Dubbo 消费端应用名称
    name: dubbo-consumer

  registry:
    # Consumer 与 Provider 使用同一个 ZooKeeper 地址
    address: ${DUBBO_REGISTRY_ADDRESS:zookeeper://127.0.0.1:2181}

  consumer:
    # 本地开发可关闭启动检查
    check: false
    # Consumer 默认调用超时时间
    timeout: 3000
    # Consumer 默认重试次数
    retries: 0
```

生产环境中 ZooKeeper 通常使用集群地址。Dubbo 官方文档给出的集群写法可以通过 `backup` 参数声明多个备用节点。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/registry/zookeeper/?utm_source=chatgpt.com))

```yaml
dubbo:
  registry:
    # ZooKeeper 集群地址，主地址不可用时使用 backup 节点
    address: zookeeper://10.20.153.10:2181?backup=10.20.153.11:2181,10.20.153.12:2181
```

ZooKeeper 注册中心的优点是成熟稳定、Dubbo 生态历史支持充分；不足是配置中心、元数据管理、控制台展示和多环境隔离能力通常不如 Nacos 一体化。新项目如果没有强制基础设施约束，建议优先使用 Nacos；存量项目如果已经稳定运行在 ZooKeeper 上，可以先保持 ZooKeeper，再评估迁移成本。

### 服务注册与发现流程

Dubbo 的服务注册与发现可以理解为 Provider 注册、Consumer 订阅、注册中心推送、Consumer 发起 RPC 调用四个阶段。Dubbo 官方文档说明，Provider 会将实例地址注册到注册中心，Consumer 通过订阅注册中心变更事件自动获取实例变化，从而让流量转发到正确节点。([Apache Dubbo](https://dubbo.apache.org/zh-cn/overview/mannual/java-sdk/tasks/service-discovery/registry/?utm_source=chatgpt.com))

典型流程如下：

```text
1. 注册中心启动
   Nacos / ZooKeeper 先启动，并暴露服务注册发现能力。

2. Provider 启动
   Provider 加载 application.yml 中的 dubbo.registry.address、dubbo.protocol、dubbo.application 等配置。

3. Provider 暴露服务
   Provider 扫描 @DubboService，将 UserDubboServiceImpl 暴露为 Dubbo 服务。

4. Provider 注册服务
   Provider 将应用名、接口名、版本、分组、IP、端口、协议等信息写入注册中心。

5. Consumer 启动
   Consumer 加载 @DubboReference，并连接同一个注册中心。

6. Consumer 订阅服务
   Consumer 根据接口、版本、分组订阅 Provider 地址列表。

7. 注册中心推送地址
   注册中心将可用 Provider 实例推送给 Consumer。

8. Consumer 发起调用
   Consumer 根据负载均衡策略选择一个 Provider，通过 Dubbo 协议发起 RPC 调用。

9. Provider 返回结果
   Provider 执行业务逻辑，序列化返回结果给 Consumer。
```

调用链路可以简化为：

```text
Provider
  ├── @DubboService 暴露服务
  ├── 注册服务地址到 Nacos / ZooKeeper
  ↓

Registry
  ├── 保存 Provider 实例信息
  ├── 推送服务变更事件
  ↓

Consumer
  ├── @DubboReference 引用远程服务
  ├── 订阅 Provider 地址
  ├── 根据负载均衡策略选择实例
  └── 发起 RPC 调用
```

注册与发现排查时，可以按下面顺序定位：

| 排查项         | Provider                          | Consumer                           |
| -------------- | --------------------------------- | ---------------------------------- |
| 注册中心地址   | `dubbo.registry.address` 是否正确 | 是否与 Provider 完全一致           |
| 应用名         | `dubbo.application.name` 是否正确 | `dubbo.application.name` 是否唯一  |
| 协议端口       | `dubbo.protocol.port` 是否监听    | Consumer 是否能访问该端口          |
| 服务分组       | `@DubboService(group = "...")`    | `@DubboReference(group = "...")`   |
| 服务版本       | `@DubboService(version = "...")`  | `@DubboReference(version = "...")` |
| API 依赖       | Provider 实现的接口版本           | Consumer 依赖的 API jar 版本       |
| 注册中心控制台 | 是否能看到 Provider 实例          | 是否能看到订阅或调用信息           |

最常见的问题是 Provider 和 Consumer 使用了不同的注册中心地址、不同的 namespace、不同的 group、不同的 version，或者 Consumer 依赖的 `dubbo-api` 版本不是最新。

## Dubbo 常用配置

Dubbo 常用配置主要包括协议、超时、重试、负载均衡、分组和版本。建议将全局默认值写在 `application.yml` 中，将单个接口的特殊配置写在 `@DubboService` 或 `@DubboReference` 上。

Dubbo Spring Boot 配置可以直接写在 `application.yml` 中，官方文档列出的常用配置组件包括 `dubbo.application`、`dubbo.protocol`、`dubbo.registry`、`dubbo.consumer`、`dubbo.provider` 等；`dubbo-spring-boot-starter` 会识别 `dubbo.` 开头的配置项并扫描 Dubbo 注解。([Apache Dubbo](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/config/spring/spring-boot/?utm_source=chatgpt.com))

### 协议配置

协议配置决定 Provider 使用什么协议暴露远程服务，以及监听哪个端口。常见配置是 `dubbo.protocol.name=dubbo`，并指定一个独立的 RPC 端口，例如 `20880`。

Provider 协议配置如下。

文件位置：`dubbo-provider/src/main/resources/application.yml`

```yaml
dubbo:
  protocol:
    # Dubbo 默认 RPC 协议，适合 Java 内部服务调用
    name: dubbo
    # Dubbo RPC 端口，不要与 server.port 冲突
    port: 20880
    # 线程池类型，默认配置即可；高并发场景再按压测结果调整
    threadpool: fixed
    # Dubbo 服务端业务线程数，需结合 CPU、接口耗时和并发量压测
    threads: 200
```

如果同一台机器启动多个 Provider 实例，`dubbo.protocol.port` 不能重复。可以通过环境变量注入端口。

```yaml
dubbo:
  protocol:
    # 支持通过环境变量覆盖 RPC 端口
    port: ${DUBBO_PROTOCOL_PORT:20880}
```

启动时指定端口：

```bash
# 指定 Dubbo RPC 端口启动 Provider
java -jar dubbo-provider/target/dubbo-provider-1.0.0.jar \
  --DUBBO_PROTOCOL_PORT=20881
```

Dubbo 3 也支持 Triple 协议，适合需要 HTTP/2、跨语言或云原生网关场景的项目。普通 Java 内部 RPC 项目可以先使用 `dubbo` 协议，后续再根据网关、跨语言、观测体系需求评估是否引入 `tri`。

```yaml
dubbo:
  protocol:
    # Triple 协议，适合 HTTP/2 和跨语言场景
    name: tri
    # Triple 服务端口
    port: 50051
```

### 超时配置

超时配置用于控制 Consumer 等待 Provider 响应的最长时间。Dubbo 配置参考手册中，`timeout` 表示远程服务调用超时时间，单位为毫秒。([Apache Dubbo](https://dubbo.apache.org/en/docs3-v2/java-sdk/reference-manual/config/properties/?utm_source=chatgpt.com))

超时可以配置在多个层级：

| 配置位置                          | 作用范围              | 示例                 |
| --------------------------------- | --------------------- | -------------------- |
| `dubbo.consumer.timeout`          | Consumer 全局默认超时 | 所有引用默认生效     |
| `dubbo.provider.timeout`          | Provider 全局默认超时 | 所有暴露服务默认生效 |
| `@DubboReference(timeout = 3000)` | 单个消费引用          | 当前引用生效         |
| `@DubboService(timeout = 3000)`   | 单个服务暴露          | 当前服务生效         |

Consumer 全局超时配置：

```yaml
dubbo:
  consumer:
    # Consumer 默认调用超时时间，单位毫秒
    timeout: 3000
```

Provider 全局超时配置：

```yaml
dubbo:
  provider:
    # Provider 默认服务超时时间，单位毫秒
    timeout: 3000
```

单个接口引用超时配置：

```java
package io.github.atengk.dubbo.consumer.service;

import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.service.UserDubboService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 用户远程调用超时配置示例
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Service
public class UserTimeoutDemoService {

    @DubboReference(
            version = DubboVersionConstant.USER_VERSION_V1,
            group = DubboVersionConstant.USER_GROUP,
            timeout = 3000
    )
    private UserDubboService userDubboService;

}
```

超时配置建议：

| 接口类型           | 建议超时                               |
| ------------------ | -------------------------------------- |
| 本地机房普通查询   | 1000ms 到 3000ms                       |
| 复杂查询或聚合接口 | 3000ms 到 5000ms                       |
| 涉及第三方接口     | 按第三方 SLA 单独评估                  |
| 核心写接口         | 不宜设置过长，避免线程堆积             |
| 批量接口           | 结合批量大小、数据库耗时和压测结果设置 |

超时时间不是越长越好。过长的超时会导致 Consumer 线程长时间阻塞；过短的超时会导致正常慢查询被误判为失败。生产环境应结合接口 P95、P99 耗时和容量压测结果设置。

### 重试配置

重试配置用于控制远程调用失败后的自动重试次数。Dubbo 配置参考手册中，`retries` 表示远程服务调用重试次数，不包含第一次调用；不需要重试时应设置为 `0`。([Apache Dubbo](https://dubbo.apache.org/en/docs3-v2/java-sdk/reference-manual/config/properties/?utm_source=chatgpt.com))

Consumer 全局重试配置如下。

```yaml
dubbo:
  consumer:
    # 默认不重试，避免写操作重复提交
    retries: 0
```

单个接口引用可以单独配置重试。

```java
package io.github.atengk.dubbo.consumer.service;

import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.service.UserDubboService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 用户远程调用重试配置示例
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Service
public class UserRetryDemoService {

    @DubboReference(
            version = DubboVersionConstant.USER_VERSION_V1,
            group = DubboVersionConstant.USER_GROUP,
            timeout = 3000,
            retries = 0
    )
    private UserDubboService userDubboService;

}
```

重试配置建议：

| 接口类型           | 是否建议重试    | 原因                       |
| ------------------ | --------------- | -------------------------- |
| 查询接口           | 可以谨慎重试    | 查询通常具备幂等性         |
| 新增接口           | 不建议自动重试  | 可能导致重复创建           |
| 修改接口           | 不建议自动重试  | 可能导致重复更新或状态错乱 |
| 删除接口           | 不建议自动重试  | 可能导致幂等性问题         |
| 支付、扣库存、发券 | 禁止无控制重试  | 必须通过业务幂等控制       |
| 只读配置查询       | 可配置 1 次重试 | 失败影响较小               |

如果确实需要对写操作重试，必须先设计业务幂等。例如使用业务唯一号、请求流水号、数据库唯一索引、防重表、状态机或分布式锁，确保重复请求不会造成重复数据。

### 负载均衡配置

负载均衡配置用于多个 Provider 实例同时存在时决定 Consumer 调用哪个实例。Dubbo 配置参考手册中，`loadbalance` 可配置为 `random`、`roundrobin`、`leastactive`、`consistenthash`、`shortestresponse` 等策略。([Apache Dubbo](https://dubbo.apache.org/en/docs3-v2/java-sdk/reference-manual/config/properties/?utm_source=chatgpt.com))

Consumer 全局负载均衡配置：

```yaml
dubbo:
  consumer:
    # 默认随机负载均衡，适合大多数普通接口
    loadbalance: random
```

单个服务引用配置负载均衡：

```java
package io.github.atengk.dubbo.consumer.service;

import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.service.UserDubboService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 用户远程调用负载均衡配置示例
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Service
public class UserLoadBalanceDemoService {

    @DubboReference(
            version = DubboVersionConstant.USER_VERSION_V1,
            group = DubboVersionConstant.USER_GROUP,
            loadbalance = "random"
    )
    private UserDubboService userDubboService;

}
```

常用负载均衡策略说明：

| 策略               | 说明                            | 适用场景                        |
| ------------------ | ------------------------------- | ------------------------------- |
| `random`           | 随机选择 Provider               | 默认场景，简单稳定              |
| `roundrobin`       | 轮询选择 Provider               | Provider 性能接近，调用耗时稳定 |
| `leastactive`      | 优先选择活跃调用数少的 Provider | 接口耗时差异较大的场景          |
| `consistenthash`   | 相同参数尽量落到同一 Provider   | 需要调用粘性或本地缓存命中      |
| `shortestresponse` | 优先选择响应时间短的 Provider   | 对延迟敏感的接口                |

实际项目中，不建议一开始就过度调整负载均衡策略。普通服务先使用默认 `random`，等出现热点、长尾延迟、缓存命中或节点性能差异问题后，再根据监控数据调整。

### 分组与版本配置

分组和版本用于服务隔离和接口演进。`version` 主要用于接口版本管理，`group` 主要用于服务分组、业务隔离、多实现共存或灰度场景。

建议将分组和版本定义为公共常量，Provider 和 Consumer 都引用同一份常量，避免字符串写错。

文件位置：`dubbo-api/src/main/java/io/github/atengk/dubbo/api/constant/DubboVersionConstant.java`

```java
package io.github.atengk.dubbo.api.constant;

/**
 * Dubbo 服务分组与版本常量
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class DubboVersionConstant {

    /**
     * 用户服务分组
     */
    public static final String USER_GROUP = "user";

    /**
     * 订单服务分组
     */
    public static final String ORDER_GROUP = "order";

    /**
     * 用户服务 V1 版本
     */
    public static final String USER_VERSION_V1 = "1.0.0";

    /**
     * 用户服务 V2 版本
     */
    public static final String USER_VERSION_V2 = "2.0.0";

    private DubboVersionConstant() {
    }

}
```

Provider 暴露 V1 服务：

```java
package io.github.atengk.dubbo.provider.service;

import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 用户 V1 Dubbo 服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@DubboService(
        version = DubboVersionConstant.USER_VERSION_V1,
        group = DubboVersionConstant.USER_GROUP
)
public class UserV1DubboServiceImpl implements UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UserDTO getUserById(Long id) {
        log.info("调用用户 V1 服务，用户ID：{}", id);
        return UserDTO.builder().id(id).username("user-v1").build();
    }

}
```

Consumer 引用 V1 服务：

```java
package io.github.atengk.dubbo.consumer.service;

import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.service.UserDubboService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 用户 V1 远程服务引用
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Service
public class UserV1RemoteService {

    @DubboReference(
            version = DubboVersionConstant.USER_VERSION_V1,
            group = DubboVersionConstant.USER_GROUP,
            timeout = 3000,
            retries = 0
    )
    private UserDubboService userDubboService;

}
```

如果需要灰度升级，可以同时暴露 V1 和 V2 两个版本。

```text
Provider：
- UserDubboService group = user, version = 1.0.0
- UserDubboService group = user, version = 2.0.0

Consumer：
- 老系统继续引用 version = 1.0.0
- 新系统或灰度流量引用 version = 2.0.0
- 全部迁移完成后下线 version = 1.0.0
```

分组和版本使用建议：

| 场景           | 建议配置                                           |
| -------------- | -------------------------------------------------- |
| 正常业务接口   | 固定 `group` 和 `version`                          |
| 多业务域隔离   | 使用不同 `group`                                   |
| 接口兼容升级   | 保持原 `version`                                   |
| 接口不兼容升级 | 新增 `version`                                     |
| 灰度发布       | 新旧版本同时暴露                                   |
| 多实现共存     | 使用不同 `group` 或不同接口                        |
| 环境隔离       | 优先使用注册中心 namespace，不建议只靠 Dubbo group |

最终建议形成统一约定：`group` 表示业务分组，`version` 表示接口版本，Nacos namespace 表示环境隔离。不要把环境、业务域和接口版本全部混在一个字段里，否则后期治理和排查会非常困难。



## 异常处理

Dubbo 远程调用跨越 Consumer、网络、注册中心和 Provider，异常处理不能只依赖本地 `try-catch`。实际项目中需要区分参数异常、业务异常、远程调用异常、超时异常、服务不可用异常和系统未知异常，并在 Provider 和 Consumer 两侧分别处理。

推荐的基本原则是：Provider 不直接暴露数据库异常、中间件异常、空指针异常等底层异常；Consumer 不直接把 Dubbo 原始异常返回给前端；跨服务传输的异常要使用 API 模块中定义的公共异常或统一结果对象。

### Provider 异常处理

Provider 侧异常处理的目标是保护服务边界。Provider 内部可以发生数据库异常、参数异常、业务异常、缓存异常、第三方接口异常，但暴露给 Consumer 时应该统一转换为可识别、可记录、可排查的远程异常。

推荐处理方式如下：

| 异常类型   | Provider 处理方式                             |
| ---------- | --------------------------------------------- |
| 参数异常   | 使用 Hutool `Assert` 或业务校验后抛出统一异常 |
| 业务异常   | 抛出 API 模块中的公共业务异常                 |
| 数据库异常 | 捕获后记录日志，转换为远程服务异常            |
| 第三方异常 | 捕获后记录上下文，转换为远程服务异常          |
| 未知异常   | 统一兜底捕获，避免直接泄露内部堆栈            |

先在 `dubbo-api` 模块中定义远程错误码常量。该类会被 Provider 和 Consumer 同时依赖。

文件位置：`dubbo-api/src/main/java/io/github/atengk/dubbo/api/constant/RemoteErrorCode.java`

```java
package io.github.atengk.dubbo.api.constant;

/**
 * 远程服务错误码常量
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class RemoteErrorCode {

    /**
     * 参数错误
     */
    public static final String PARAM_ERROR = "PARAM_ERROR";

    /**
     * 业务错误
     */
    public static final String BUSINESS_ERROR = "BUSINESS_ERROR";

    /**
     * 数据不存在
     */
    public static final String DATA_NOT_FOUND = "DATA_NOT_FOUND";

    /**
     * Provider 服务异常
     */
    public static final String PROVIDER_ERROR = "PROVIDER_ERROR";

    /**
     * Consumer 调用异常
     */
    public static final String CONSUMER_ERROR = "CONSUMER_ERROR";

    /**
     * 远程调用超时
     */
    public static final String REMOTE_TIMEOUT = "REMOTE_TIMEOUT";

    /**
     * 远程服务不可用
     */
    public static final String REMOTE_UNAVAILABLE = "REMOTE_UNAVAILABLE";

    private RemoteErrorCode() {
    }

}
```

再定义跨服务可识别的统一远程异常。该异常放在 API 模块中，Provider 抛出后 Consumer 才能识别异常类型。

文件位置：`dubbo-api/src/main/java/io/github/atengk/dubbo/api/exception/RemoteServiceException.java`

```java
package io.github.atengk.dubbo.api.exception;

import java.io.Serial;
import java.io.Serializable;

/**
 * 远程服务异常
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class RemoteServiceException extends RuntimeException implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误详情
     */
    private final String detail;

    /**
     * 创建远程服务异常
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public RemoteServiceException(String code, String message) {
        super(message);
        this.code = code;
        this.detail = null;
    }

    /**
     * 创建远程服务异常
     *
     * @param code 错误码
     * @param message 错误消息
     * @param detail 错误详情
     */
    public RemoteServiceException(String code, String message, String detail) {
        super(message);
        this.code = code;
        this.detail = detail;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取错误详情
     *
     * @return 错误详情
     */
    public String getDetail() {
        return detail;
    }

}
```

Provider 模块建议引入 AOP 依赖，用于统一拦截 `@DubboService` 服务方法。

文件位置：`dubbo-provider/pom.xml`

```xml
<dependencies>
    <!-- Spring AOP：用于统一拦截 Dubbo Provider 服务异常 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
</dependencies>
```

Provider 侧通过切面统一记录异常并转换异常类型。这样可以避免每个 Dubbo 方法都写重复的 `try-catch`。

文件位置：`dubbo-provider/src/main/java/io/github/atengk/dubbo/provider/aspect/DubboProviderExceptionAspect.java`

```java
package io.github.atengk.dubbo.provider.aspect;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.dubbo.api.constant.RemoteErrorCode;
import io.github.atengk.dubbo.api.exception.RemoteServiceException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Dubbo Provider 异常处理切面
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Aspect
@Component
public class DubboProviderExceptionAspect implements Ordered {

    /**
     * 拦截 Dubbo 服务方法并统一处理异常
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 远程服务异常
     */
    @Around("@within(org.apache.dubbo.config.annotation.DubboService)")
    public Object aroundDubboService(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        try {
            return joinPoint.proceed();
        } catch (RemoteServiceException ex) {
            log.warn("Provider 业务异常，方法：{}，错误码：{}，错误信息：{}",
                    methodName, ex.getCode(), ex.getMessage());
            throw ex;
        } catch (IllegalArgumentException ex) {
            String message = StrUtil.blankToDefault(ex.getMessage(), "请求参数不合法");
            log.warn("Provider 参数异常，方法：{}，错误信息：{}", methodName, message);
            throw new RemoteServiceException(RemoteErrorCode.PARAM_ERROR, message);
        } catch (Exception ex) {
            String rootMessage = ExceptionUtil.getRootCauseMessage(ex);
            log.error("Provider 系统异常，方法：{}，异常信息：{}", methodName, rootMessage, ex);
            throw new RemoteServiceException(
                    RemoteErrorCode.PROVIDER_ERROR,
                    "远程服务处理失败",
                    rootMessage
            );
        }
    }

    /**
     * 设置切面优先级
     *
     * @return 优先级
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
```

Provider 服务实现中只保留必要的业务校验和业务异常，不需要在每个方法里捕获未知异常。

文件位置：`dubbo-provider/src/main/java/io/github/atengk/dubbo/provider/service/UserDubboServiceImpl.java`

```java
package io.github.atengk.dubbo.provider.service;

import cn.hutool.core.lang.Assert;
import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.constant.RemoteErrorCode;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.exception.RemoteServiceException;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户 Dubbo 服务实现类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@DubboService(
        version = DubboVersionConstant.USER_VERSION_V1,
        group = DubboVersionConstant.USER_GROUP,
        timeout = 3000,
        retries = 0
)
public class UserDubboServiceImpl implements UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UserDTO getUserById(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        log.info("Provider 查询用户信息，用户ID：{}", id);

        if (Objects.equals(id, -1L)) {
            throw new RemoteServiceException(RemoteErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        return UserDTO.builder()
                .id(id)
                .username("ateng")
                .phone("13800000000")
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
    }

}
```

Provider 侧不建议直接抛出 `SQLException`、`DataAccessException`、`NullPointerException`、`RuntimeException` 等底层异常给 Consumer。底层异常应该在 Provider 日志中完整记录，对 Consumer 返回明确的业务错误码和简洁错误信息。

### Consumer 异常处理

Consumer 侧异常处理的目标是保护调用入口。Consumer 不应该把 Dubbo 的 `RpcException`、超时堆栈、注册中心异常、序列化异常直接返回给前端或上游系统，而应该转换成当前应用自己的异常或统一响应对象。

Consumer 常见异常类型如下：

| 异常类型                 | 常见原因                                    | 处理方式           |
| ------------------------ | ------------------------------------------- | ------------------ |
| `RemoteServiceException` | Provider 主动抛出的业务异常                 | 直接转换为业务响应 |
| `RpcException`           | Dubbo 调用失败、超时、无 Provider、网络异常 | 转换为远程调用异常 |
| 参数异常                 | Consumer 本地参数校验失败                   | 直接返回参数错误   |
| 未知异常                 | Consumer 本地代码异常                       | 统一兜底处理       |

Consumer 模块可以定义自己的调用异常，用于屏蔽 Dubbo 原始异常。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/exception/RemoteCallException.java`

```java
package io.github.atengk.dubbo.consumer.exception;

/**
 * 远程调用异常
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class RemoteCallException extends RuntimeException {

    /**
     * 错误码
     */
    private final String code;

    /**
     * 远程服务名称
     */
    private final String serviceName;

    /**
     * 远程方法名称
     */
    private final String methodName;

    /**
     * 创建远程调用异常
     *
     * @param code 错误码
     * @param message 错误消息
     * @param serviceName 远程服务名称
     * @param methodName 远程方法名称
     */
    public RemoteCallException(String code, String message, String serviceName, String methodName) {
        super(message);
        this.code = code;
        this.serviceName = serviceName;
        this.methodName = methodName;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取远程服务名称
     *
     * @return 远程服务名称
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * 获取远程方法名称
     *
     * @return 远程方法名称
     */
    public String getMethodName() {
        return methodName;
    }

}
```

Consumer 远程服务封装类中统一捕获远程异常，并转换为 `RemoteCallException` 或直接抛出 Provider 传来的 `RemoteServiceException`。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/service/UserRemoteService.java`

```java
package io.github.atengk.dubbo.consumer.service;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.lang.Assert;
import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.constant.RemoteErrorCode;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.exception.RemoteServiceException;
import io.github.atengk.dubbo.api.service.UserDubboService;
import io.github.atengk.dubbo.consumer.exception.RemoteCallException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Service;

/**
 * 用户远程服务调用封装
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class UserRemoteService {

    @DubboReference(
            version = DubboVersionConstant.USER_VERSION_V1,
            group = DubboVersionConstant.USER_GROUP,
            timeout = 3000,
            retries = 0,
            check = false
    )
    private UserDubboService userDubboService;

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public UserDTO getUserById(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        String serviceName = "UserDubboService";
        String methodName = "getUserById";

        try {
            log.info("Consumer 开始调用远程服务，服务：{}，方法：{}，用户ID：{}", serviceName, methodName, id);
            UserDTO user = userDubboService.getUserById(id);
            log.info("Consumer 远程服务调用成功，服务：{}，方法：{}，用户ID：{}", serviceName, methodName, id);
            return user;
        } catch (RemoteServiceException ex) {
            log.warn("Consumer 接收到 Provider 业务异常，服务：{}，方法：{}，错误码：{}，错误信息：{}",
                    serviceName, methodName, ex.getCode(), ex.getMessage());
            throw ex;
        } catch (RpcException ex) {
            String rootMessage = ExceptionUtil.getRootCauseMessage(ex);
            log.error("Consumer 远程调用失败，服务：{}，方法：{}，异常信息：{}",
                    serviceName, methodName, rootMessage, ex);

            String errorCode = ex.isTimeout()
                    ? RemoteErrorCode.REMOTE_TIMEOUT
                    : RemoteErrorCode.REMOTE_UNAVAILABLE;

            throw new RemoteCallException(errorCode, "远程服务暂时不可用", serviceName, methodName);
        } catch (Exception ex) {
            String rootMessage = ExceptionUtil.getRootCauseMessage(ex);
            log.error("Consumer 本地处理异常，服务：{}，方法：{}，异常信息：{}",
                    serviceName, methodName, rootMessage, ex);
            throw new RemoteCallException(RemoteErrorCode.CONSUMER_ERROR, "远程调用处理失败", serviceName, methodName);
        }
    }

}
```

Consumer 对外提供 REST 接口时，建议使用全局异常处理器统一返回错误响应。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/common/Result.java`

```java
package io.github.atengk.dubbo.consumer.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST 接口统一返回对象
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 业务错误码
     */
    private String errorCode;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 统一响应对象
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败响应
     *
     * @param code HTTP 风格响应码
     * @param errorCode 业务错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 统一响应对象
     */
    public static <T> Result<T> fail(Integer code, String errorCode, String message) {
        return Result.<T>builder()
                .code(code)
                .errorCode(errorCode)
                .message(message)
                .build();
    }

}
```

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.dubbo.consumer.handler;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.dubbo.api.constant.RemoteErrorCode;
import io.github.atengk.dubbo.api.exception.RemoteServiceException;
import io.github.atengk.dubbo.consumer.common.Result;
import io.github.atengk.dubbo.consumer.exception.RemoteCallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 Provider 返回的远程业务异常
     *
     * @param ex 远程服务异常
     * @return 统一响应对象
     */
    @ExceptionHandler(RemoteServiceException.class)
    public Result<Void> handleRemoteServiceException(RemoteServiceException ex) {
        log.warn("处理远程业务异常，错误码：{}，错误信息：{}", ex.getCode(), ex.getMessage());
        return Result.fail(500, ex.getCode(), ex.getMessage());
    }

    /**
     * 处理 Consumer 侧远程调用异常
     *
     * @param ex 远程调用异常
     * @return 统一响应对象
     */
    @ExceptionHandler(RemoteCallException.class)
    public Result<Void> handleRemoteCallException(RemoteCallException ex) {
        log.warn("处理远程调用异常，服务：{}，方法：{}，错误码：{}，错误信息：{}",
                ex.getServiceName(), ex.getMethodName(), ex.getCode(), ex.getMessage());
        return Result.fail(503, ex.getCode(), ex.getMessage());
    }

    /**
     * 处理参数异常
     *
     * @param ex 参数异常
     * @return 统一响应对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = StrUtil.blankToDefault(ex.getMessage(), "请求参数不合法");
        log.warn("处理参数异常，错误信息：{}", message);
        return Result.fail(400, RemoteErrorCode.PARAM_ERROR, message);
    }

    /**
     * 处理未知异常
     *
     * @param ex 未知异常
     * @return 统一响应对象
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        String rootMessage = ExceptionUtil.getRootCauseMessage(ex);
        log.error("处理未知异常，异常信息：{}", rootMessage, ex);
        return Result.fail(500, RemoteErrorCode.CONSUMER_ERROR, "系统繁忙，请稍后重试");
    }

}
```

Controller 层返回统一对象时，可以这样写：

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/controller/UserController.java`

```java
package io.github.atengk.dubbo.consumer.controller;

import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.consumer.common.Result;
import io.github.atengk.dubbo.consumer.service.UserRemoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 REST 接口控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserRemoteService userRemoteService;

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        log.info("REST 查询用户信息，用户ID：{}", id);
        return Result.success(userRemoteService.getUserById(id));
    }

}
```

### 远程调用异常封装

远程调用异常封装的核心目标是让上游调用方知道“发生了什么”，但不暴露“底层怎么坏的”。例如，前端或上游服务只需要知道远程服务暂时不可用、调用超时、业务参数错误、数据不存在，不需要看到 Provider 的 SQL、Nacos 连接堆栈或 Dubbo 内部类名。

推荐将异常封装分为三层：

```text
Provider 内部异常
    ↓
RemoteServiceException
    ↓
Consumer RemoteCallException / Result
```

常见映射关系如下：

| 原始异常           | 转换后错误码         | 返回消息             |
| ------------------ | -------------------- | -------------------- |
| 参数为空、格式错误 | `PARAM_ERROR`        | 请求参数不合法       |
| 业务规则不满足     | `BUSINESS_ERROR`     | 具体业务提示         |
| 数据不存在         | `DATA_NOT_FOUND`     | 数据不存在           |
| Provider 未知异常  | `PROVIDER_ERROR`     | 远程服务处理失败     |
| Dubbo 调用超时     | `REMOTE_TIMEOUT`     | 远程服务调用超时     |
| Provider 不可用    | `REMOTE_UNAVAILABLE` | 远程服务暂时不可用   |
| Consumer 本地异常  | `CONSUMER_ERROR`     | 系统繁忙，请稍后重试 |

如果远程服务调用较多，可以封装一个通用执行器，统一记录服务名、方法名、耗时和异常。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/support/RemoteCallExecutor.java`

```java
package io.github.atengk.dubbo.consumer.support;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.exceptions.ExceptionUtil;
import io.github.atengk.dubbo.api.constant.RemoteErrorCode;
import io.github.atengk.dubbo.api.exception.RemoteServiceException;
import io.github.atengk.dubbo.consumer.exception.RemoteCallException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 远程调用执行器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class RemoteCallExecutor {

    /**
     * 执行远程调用
     *
     * @param serviceName 服务名称
     * @param methodName 方法名称
     * @param supplier 调用逻辑
     * @param <T> 返回类型
     * @return 调用结果
     */
    public <T> T execute(String serviceName, String methodName, Supplier<T> supplier) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            log.info("开始远程调用，服务：{}，方法：{}", serviceName, methodName);
            T result = supplier.get();
            stopWatch.stop();
            log.info("远程调用成功，服务：{}，方法：{}，耗时：{}ms",
                    serviceName, methodName, stopWatch.getTotalTimeMillis());
            return result;
        } catch (RemoteServiceException ex) {
            stopWatch.stop();
            log.warn("远程业务异常，服务：{}，方法：{}，耗时：{}ms，错误码：{}，错误信息：{}",
                    serviceName, methodName, stopWatch.getTotalTimeMillis(), ex.getCode(), ex.getMessage());
            throw ex;
        } catch (RpcException ex) {
            stopWatch.stop();
            String rootMessage = ExceptionUtil.getRootCauseMessage(ex);
            String errorCode = ex.isTimeout()
                    ? RemoteErrorCode.REMOTE_TIMEOUT
                    : RemoteErrorCode.REMOTE_UNAVAILABLE;

            log.error("远程调用失败，服务：{}，方法：{}，耗时：{}ms，异常信息：{}",
                    serviceName, methodName, stopWatch.getTotalTimeMillis(), rootMessage, ex);

            throw new RemoteCallException(errorCode, "远程服务暂时不可用", serviceName, methodName);
        } catch (Exception ex) {
            stopWatch.stop();
            String rootMessage = ExceptionUtil.getRootCauseMessage(ex);

            log.error("远程调用处理异常，服务：{}，方法：{}，耗时：{}ms，异常信息：{}",
                    serviceName, methodName, stopWatch.getTotalTimeMillis(), rootMessage, ex);

            throw new RemoteCallException(RemoteErrorCode.CONSUMER_ERROR, "远程调用处理失败", serviceName, methodName);
        }
    }

}
```

`UserRemoteService` 使用执行器后，远程调用代码会更简洁。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/service/UserRemoteService.java`

```java
package io.github.atengk.dubbo.consumer.service;

import cn.hutool.core.lang.Assert;
import io.github.atengk.dubbo.api.constant.DubboVersionConstant;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import io.github.atengk.dubbo.consumer.support.RemoteCallExecutor;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 用户远程服务调用封装
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Service
@RequiredArgsConstructor
public class UserRemoteService {

    private final RemoteCallExecutor remoteCallExecutor;

    @DubboReference(
            version = DubboVersionConstant.USER_VERSION_V1,
            group = DubboVersionConstant.USER_GROUP,
            timeout = 3000,
            retries = 0,
            check = false
    )
    private UserDubboService userDubboService;

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public UserDTO getUserById(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        return remoteCallExecutor.execute(
                "UserDubboService",
                "getUserById",
                () -> userDubboService.getUserById(id)
        );
    }

}
```

这种方式适合 Consumer 中存在大量 Dubbo 调用的项目。Controller 不关心 Dubbo 异常，业务 Service 不重复写异常转换逻辑，异常处理集中在 `RemoteCallExecutor` 和 `GlobalExceptionHandler` 中。

## 日志与链路排查

Dubbo 调用链路通常包含 REST 入口、Consumer 本地服务、Dubbo 代理、注册中心、Provider 服务实现、数据库或其他中间件。日志与链路排查的目标是快速定位问题发生在哪个环节，而不是只看到“调用失败”。

建议至少记录以下信息：

| 日志字段      | 说明               |
| ------------- | ------------------ |
| `traceId`     | 一次请求的链路标识 |
| `serviceName` | Dubbo 服务名称     |
| `methodName`  | Dubbo 方法名称     |
| `version`     | 服务版本           |
| `group`       | 服务分组           |
| `provider`    | Provider 地址      |
| `elapsedMs`   | 调用耗时           |
| `errorCode`   | 业务或系统错误码   |
| `message`     | 简洁错误信息       |

### 调用日志记录

调用日志建议分为三层记录：REST 入口日志、Consumer 远程调用日志、Provider 服务执行日志。三层日志都带上同一个 `traceId`，排查时可以直接通过 `traceId` 检索完整链路。

先配置日志格式，将 `traceId` 输出到日志中。

文件位置：`dubbo-consumer/src/main/resources/application.yml`、`dubbo-provider/src/main/resources/application.yml`

```yaml
logging:
  pattern:
    # 输出 traceId，便于串联 Consumer 和 Provider 日志
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{traceId}] %logger{36} - %msg%n"
  level:
    # 项目代码日志级别
    io.github.atengk: info
    # Dubbo 核心日志，排查时可临时改为 debug
    org.apache.dubbo: info
```

Consumer 的 REST 入口可以通过 Web 拦截器生成 `traceId`。如果上游已经传入 `X-Trace-Id`，则复用上游链路 ID；如果没有，则使用 Hutool 生成简单 UUID。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/interceptor/TraceIdInterceptor.java`

```java
package io.github.atengk.dubbo.consumer.interceptor;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 链路追踪拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    /**
     * 链路ID请求头
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * MDC 链路ID键
     */
    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 请求进入前写入 traceId
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @return 是否继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
        }

        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        return true;
    }

    /**
     * 请求完成后清理 MDC
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @param ex 异常对象
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(TRACE_ID_KEY);
    }

}
```

注册 Web 拦截器。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/config/WebMvcConfig.java`

```java
package io.github.atengk.dubbo.consumer.config;

import io.github.atengk.dubbo.consumer.interceptor.TraceIdInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TraceIdInterceptor traceIdInterceptor;

    /**
     * 注册请求拦截器
     *
     * @param registry 拦截器注册对象
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/**");
    }

}
```

如果希望 `traceId` 从 Consumer 传递到 Provider，可以增加 Dubbo Filter。Consumer 发送请求时将 `traceId` 放入 Dubbo Attachment，Provider 接收请求时从 Attachment 中取出并写入 MDC。

文件位置：`dubbo-common/src/main/java/io/github/atengk/dubbo/common/filter/TraceIdDubboFilter.java`

如果没有单独的 `dubbo-common` 模块，也可以分别放在 `dubbo-consumer` 和 `dubbo-provider` 中，保持包名和 SPI 配置一致即可。

```java
package io.github.atengk.dubbo.common.filter;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;

/**
 * Dubbo 链路追踪过滤器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER})
public class TraceIdDubboFilter implements Filter {

    /**
     * 链路ID键
     */
    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 执行 Dubbo 过滤逻辑
     *
     * @param invoker 调用器
     * @param invocation 调用信息
     * @return 调用结果
     * @throws RpcException RPC 异常
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        boolean providerSide = RpcContext.getServiceContext().isProviderSide();

        if (providerSide) {
            String traceId = RpcContext.getServerAttachment().getAttachment(TRACE_ID_KEY);
            MDC.put(TRACE_ID_KEY, StrUtil.blankToDefault(traceId, IdUtil.fastSimpleUUID()));
        } else {
            String traceId = MDC.get(TRACE_ID_KEY);
            if (StrUtil.isBlank(traceId)) {
                traceId = IdUtil.fastSimpleUUID();
                MDC.put(TRACE_ID_KEY, traceId);
            }
            RpcContext.getClientAttachment().setAttachment(TRACE_ID_KEY, traceId);
        }

        try {
            return invoker.invoke(invocation);
        } finally {
            if (providerSide) {
                MDC.remove(TRACE_ID_KEY);
            }
        }
    }

}
```

Dubbo Filter 需要通过 SPI 文件注册。

文件位置：`src/main/resources/META-INF/dubbo/org.apache.dubbo.rpc.Filter`

```properties
# traceId 过滤器，用于 Consumer 与 Provider 之间传递链路ID
traceIdDubboFilter=io.github.atengk.dubbo.common.filter.TraceIdDubboFilter
```

在配置文件中启用 Filter。

```yaml
dubbo:
  consumer:
    # Consumer 侧启用 traceId 过滤器
    filter: traceIdDubboFilter
  provider:
    # Provider 侧启用 traceId 过滤器
    filter: traceIdDubboFilter
```

完成后，一次请求的 Consumer 和 Provider 日志会出现相同的 `traceId`，例如：

```text
2026-05-05 10:30:01.123 INFO  [http-nio-8081-exec-1] [9f8c1d2a3b4c4d6e9a1b2c3d4e5f6789] UserController - REST 查询用户信息，用户ID：1
2026-05-05 10:30:01.130 INFO  [DubboServerHandler-20880-thread-1] [9f8c1d2a3b4c4d6e9a1b2c3d4e5f6789] UserDubboServiceImpl - Provider 查询用户信息，用户ID：1
```

### 注册中心排查

注册中心排查主要用于解决 Consumer 找不到 Provider、Provider 注册失败、服务实例不健康、服务版本不匹配等问题。

Nacos 排查重点如下：

| 检查项               | 说明                                                      |
| -------------------- | --------------------------------------------------------- |
| Nacos 是否启动       | 浏览器访问 `http://127.0.0.1:8848/nacos`                  |
| 注册中心地址是否一致 | Provider 和 Consumer 的 `dubbo.registry.address` 必须一致 |
| namespace 是否一致   | Provider 和 Consumer 必须在同一个 namespace               |
| group 是否一致       | Nacos 分组需要一致                                        |
| Provider 是否注册    | 服务列表中应能看到 Provider 实例                          |
| 实例是否健康         | 实例数大于 0，健康状态正常                                |
| 端口是否正确         | 实例端口应是 Dubbo RPC 端口，不是 HTTP 端口               |

常用检查命令如下：

```bash
# 检查 Nacos 端口是否可访问
curl http://127.0.0.1:8848/nacos

# 检查 Provider HTTP 健康接口，需要引入 spring-boot-starter-actuator
curl http://127.0.0.1:8080/actuator/health

# 检查 Consumer HTTP 健康接口
curl http://127.0.0.1:8081/actuator/health

# 检查 Provider Dubbo RPC 端口是否监听
lsof -i:20880
```

ZooKeeper 排查重点如下：

| 检查项                | 说明                                        |
| --------------------- | ------------------------------------------- |
| ZooKeeper 是否启动    | `2181` 端口是否监听                         |
| 注册中心地址是否一致  | Provider 和 Consumer 都指向同一组 ZooKeeper |
| Provider 节点是否存在 | ZooKeeper 中是否有对应 Dubbo 节点           |
| Consumer 是否订阅     | Consumer 是否能读取 Provider 地址           |
| 网络是否连通          | Consumer 到 Provider RPC 端口是否可达       |

常用检查命令如下：

```bash
# 检查 ZooKeeper 端口是否监听
lsof -i:2181

# 测试 ZooKeeper 是否响应，需要本机有 nc 命令
echo ruok | nc 127.0.0.1 2181

# 检查 Provider Dubbo RPC 端口
lsof -i:20880
```

如果 Consumer 报找不到服务，优先检查以下配置是否完全一致：

```text
1. dubbo.registry.address
2. Nacos namespace
3. Nacos group
4. @DubboService group
5. @DubboReference group
6. @DubboService version
7. @DubboReference version
8. dubbo-api 依赖版本
```

常见错误示例：

```yaml
# Provider 使用 dev namespace
dubbo:
  registry:
    address: nacos://127.0.0.1:8848
    parameters.namespace: dev-namespace-id

# Consumer 使用 test namespace
dubbo:
  registry:
    address: nacos://127.0.0.1:8848
    parameters.namespace: test-namespace-id
```

这种情况下，即使 Provider 已经启动，Consumer 也无法发现服务，因为两者不在同一个命名空间中。

### Dubbo 调用排查

Dubbo 调用排查需要从 Consumer 到 Provider 逐层定位。不要只看 Consumer 报错，也要同时看 Provider 是否收到请求、注册中心是否有实例、网络是否可达、接口版本是否一致。

常见问题和处理方式如下：

| 问题现象                 | 可能原因                                | 排查方式                                         |
| ------------------------ | --------------------------------------- | ------------------------------------------------ |
| `No provider available`  | Provider 未启动、未注册、版本分组不一致 | 查注册中心、查 Provider 日志、查 `version/group` |
| `TimeoutException`       | Provider 响应慢、网络慢、超时时间过短   | 查 Provider 耗时日志、数据库慢 SQL、调整 timeout |
| `Connection refused`     | Provider 端口未监听或网络不通           | `lsof -i:20880`、检查防火墙                      |
| `SerializationException` | DTO 不可序列化或字段类型不兼容          | 检查 DTO 是否实现 `Serializable`                 |
| Consumer 启动失败        | `check=true` 且 Provider 不存在         | 本地开发设置 `dubbo.consumer.check=false`        |
| 调到旧逻辑               | Provider 多版本共存或 API jar 未更新    | 检查注册中心实例和依赖版本                       |
| 重复写入数据             | 写接口配置了重试                        | 设置 `retries=0`，增加业务幂等                   |

Consumer 调用超时时，排查顺序建议如下：

```text
1. Consumer 日志是否出现 timeout
2. Provider 日志是否收到同一个 traceId
3. Provider 方法内部耗时是否过长
4. 数据库、Redis、第三方接口是否慢
5. dubbo.consumer.timeout 是否过短
6. Provider 线程池是否打满
7. 网络是否抖动或丢包
```

可以临时打开 Dubbo debug 日志观察调用细节。

文件位置：`dubbo-consumer/src/main/resources/application.yml`、`dubbo-provider/src/main/resources/application.yml`

```yaml
logging:
  level:
    # 排查 Dubbo 调用问题时临时打开，生产环境不建议长期 debug
    org.apache.dubbo: debug
    io.github.atengk: debug
```

也可以在 Consumer 侧调用前后记录耗时，快速判断问题是否发生在远程调用阶段。

```java
package io.github.atengk.dubbo.consumer.service;

import cn.hutool.core.date.StopWatch;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * Dubbo 调用耗时排查示例
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class DubboCallDebugService {

    @DubboReference(timeout = 3000, retries = 0, check = false)
    private UserDubboService userDubboService;

    /**
     * 调试用户查询调用耗时
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public UserDTO debugGetUserById(Long id) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            return userDubboService.getUserById(id);
        } finally {
            stopWatch.stop();
            log.info("Dubbo 调用耗时统计，服务：UserDubboService，方法：getUserById，用户ID：{}，耗时：{}ms",
                    id, stopWatch.getTotalTimeMillis());
        }
    }

}
```

Provider 侧也应记录核心方法耗时，避免只知道 Consumer 超时，却不知道 Provider 内部慢在哪里。

```java
package io.github.atengk.dubbo.provider.service;

import cn.hutool.core.date.StopWatch;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * Provider 调用耗时排查示例
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@DubboService(timeout = 3000, retries = 0)
public class ProviderCallDebugService implements UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UserDTO getUserById(Long id) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            return UserDTO.builder()
                    .id(id)
                    .username("debug-user")
                    .build();
        } finally {
            stopWatch.stop();
            log.info("Provider 方法耗时统计，方法：getUserById，用户ID：{}，耗时：{}ms",
                    id, stopWatch.getTotalTimeMillis());
        }
    }

}
```

最终排查标准是：Consumer 能看到请求入口日志，Consumer 远程调用日志有服务名、方法名、耗时和异常；Provider 能看到相同 `traceId` 的服务执行日志；注册中心能看到健康 Provider 实例；Provider 的 Dubbo RPC 端口可访问。这样才能快速判断问题是在入口层、消费端、注册中心、网络层、服务提供者还是底层数据库。



## 本地开发与测试

本地开发与测试的目标是让开发人员可以在单机环境中完成注册中心启动、Provider 注册、Consumer 调用、接口验证和常见问题排查。推荐先使用 Nacos 作为本地注册中心，再分别启动 Provider 和 Consumer。

本地最小启动链路如下：

```text
Nacos 注册中心
    ↓
dubbo-provider
    ↓
dubbo-consumer
    ↓
curl / Postman 调用 REST 接口
```

### 单机启动流程

单机启动需要先启动注册中心，再启动 Provider，最后启动 Consumer。Provider 启动后会向注册中心注册服务，Consumer 启动后会从注册中心订阅服务地址。

如果使用 Docker 启动 Nacos，可以先准备本地开发用的 `docker-compose.yml`。

文件位置：`docker/docker-compose-nacos.yml`

```yaml
services:
  nacos:
    # Nacos 单机开发环境镜像
    image: nacos/nacos-server:v2.3.0
    container_name: nacos-standalone
    environment:
      # 单机模式，适合本地开发
      MODE: standalone
      # 本地开发关闭鉴权，生产环境必须按安全规范开启
      NACOS_AUTH_ENABLE: "false"
      # JVM 内存参数，避免本机资源占用过高
      JVM_XMS: 256m
      JVM_XMX: 512m
    ports:
      # Nacos 控制台与客户端访问端口
      - "8848:8848"
      # Nacos gRPC 端口
      - "9848:9848"
    restart: unless-stopped
```

启动 Nacos：

```bash
# 进入项目根目录
cd apache-dubbo-demo

# 启动本地 Nacos
docker compose -f docker/docker-compose-nacos.yml up -d

# 查看 Nacos 容器状态
docker ps | grep nacos

# 访问 Nacos 控制台
curl http://127.0.0.1:8848/nacos
```

`docker compose up -d` 表示后台启动 Nacos；`docker ps` 用于确认容器是否正常运行；`curl` 用于验证 Nacos 控制台地址是否可以访问。

然后在父工程目录编译所有模块：

```bash
# 清理并打包所有 Maven 模块，跳过测试
mvn clean package -DskipTests
```

正常情况下，Maven 会按模块依赖顺序构建：

```text
dubbo-api
dubbo-provider
dubbo-consumer
```

启动 Provider：

```bash
# 启动 Provider，使用本地 Nacos 注册中心
java -jar dubbo-provider/target/dubbo-provider-1.0.0.jar \
  --DUBBO_REGISTRY_ADDRESS=nacos://127.0.0.1:8848 \
  --DUBBO_PROTOCOL_PORT=20880 \
  --server.port=8080
```

启动 Consumer：

```bash
# 启动 Consumer，使用同一个 Nacos 注册中心
java -jar dubbo-consumer/target/dubbo-consumer-1.0.0.jar \
  --DUBBO_REGISTRY_ADDRESS=nacos://127.0.0.1:8848 \
  --server.port=8081
```

启动成功后，需要确认三个端口：

| 端口    | 服务     | 说明                           |
| ------- | -------- | ------------------------------ |
| `8848`  | Nacos    | 注册中心控制台与客户端访问端口 |
| `20880` | Provider | Dubbo RPC 服务端口             |
| `8080`  | Provider | Provider HTTP 端口             |
| `8081`  | Consumer | Consumer REST 接口端口         |

本地端口检查命令如下：

```bash
# 检查 Nacos 端口
lsof -i:8848

# 检查 Provider Dubbo RPC 端口
lsof -i:20880

# 检查 Provider HTTP 端口
lsof -i:8080

# 检查 Consumer HTTP 端口
lsof -i:8081
```

如果项目引入了 `spring-boot-starter-actuator`，可以进一步检查健康状态：

```bash
# Provider 健康检查
curl http://127.0.0.1:8080/actuator/health

# Consumer 健康检查
curl http://127.0.0.1:8081/actuator/health
```

### 接口联调方式

接口联调建议通过 Consumer 的 REST 接口触发 Dubbo 调用。调用方只访问 Consumer，Consumer 内部通过 `@DubboReference` 调用 Provider。

联调链路如下：

```text
curl / Postman
    ↓ HTTP
dubbo-consumer:8081
    ↓ Dubbo RPC
dubbo-provider:20880
    ↓ 返回 DTO
dubbo-consumer:8081
    ↓ HTTP JSON
curl / Postman
```

查询单个用户：

```bash
# 查询用户ID为 1 的用户信息
curl http://127.0.0.1:8081/users/1
```

预期返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "ateng",
    "phone": "13800000000",
    "status": 1,
    "createTime": "2026-05-05T10:30:00"
  }
}
```

批量查询用户：

```bash
# 批量查询用户信息
curl "http://127.0.0.1:8081/users?ids=1,2,3"
```

分页查询用户：

```bash
# 分页查询用户信息
curl -X POST http://127.0.0.1:8081/users/page \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "pageNum": 1,
    "pageSize": 10
  }'
```

创建用户：

```bash
# 创建用户
curl -X POST http://127.0.0.1:8081/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "phone": "13800000000",
    "status": 1
  }'
```

修改用户状态：

```bash
# 修改用户状态
curl -X PUT "http://127.0.0.1:8081/users/1/status?status=1"
```

如果需要在本地快速确认当前应用加载的是哪个环境，可以在 Provider 和 Consumer 中增加一个简单的环境检查接口。

文件位置：`dubbo-consumer/src/main/java/io/github/atengk/dubbo/consumer/controller/ProfileController.java`

```java
package io.github.atengk.dubbo.consumer.controller;

import cn.hutool.core.map.MapUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 环境信息检查接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final Environment environment;

    /**
     * 查看当前环境信息
     *
     * @return 环境信息
     */
    @GetMapping("/profiles")
    public Map<String, Object> getProfiles() {
        return MapUtil.<String, Object>builder()
                .put("activeProfiles", environment.getActiveProfiles())
                .put("applicationName", environment.getProperty("spring.application.name"))
                .put("registryAddress", environment.getProperty("dubbo.registry.address"))
                .build();
    }

}
```

调用环境检查接口：

```bash
# 查看 Consumer 当前环境和注册中心配置
curl http://127.0.0.1:8081/profiles
```

### 常见问题验证

本地开发中，建议主动验证几个常见异常场景，避免部署后才发现配置问题。

验证 Provider 未启动时 Consumer 的表现：

```bash
# 停止 Provider 后调用 Consumer
curl http://127.0.0.1:8081/users/1
```

预期现象是 Consumer 返回远程服务不可用或调用失败，而不是直接输出 Dubbo 原始堆栈。若 Consumer 启动失败，检查是否配置了：

```yaml
dubbo:
  consumer:
    # 本地开发建议关闭启动检查
    check: false
```

验证版本不一致问题。Provider 暴露版本：

```java
@DubboService(version = "1.0.0", group = "user")
```

Consumer 引用版本：

```java
@DubboReference(version = "2.0.0", group = "user")
```

此时 Consumer 通常无法找到对应服务。处理方式是保证 Provider 和 Consumer 的 `version`、`group` 完全一致，并确认双方依赖的是同一个 `dubbo-api` 版本。

验证超时问题。可以临时在 Provider 中模拟慢调用：

文件位置：`dubbo-provider/src/main/java/io/github/atengk/dubbo/provider/service/UserDubboServiceImpl.java`

```java
package io.github.atengk.dubbo.provider.service;

import cn.hutool.core.thread.ThreadUtil;
import io.github.atengk.dubbo.api.dto.UserDTO;
import io.github.atengk.dubbo.api.service.UserDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 用户 Dubbo 超时验证服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@DubboService(version = "1.0.0", group = "user", timeout = 1000, retries = 0)
public class UserTimeoutVerifyServiceImpl implements UserDubboService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public UserDTO getUserById(Long id) {
        log.info("开始模拟慢调用，用户ID：{}", id);

        // 模拟 Provider 处理时间超过 Consumer 超时时间
        ThreadUtil.sleep(3000);

        return UserDTO.builder()
                .id(id)
                .username("timeout-user")
                .build();
    }

}
```

如果 Consumer 超时时间小于 Provider 实际耗时，Consumer 应返回远程调用超时。验证后需要删除或恢复该模拟代码。

常见问题对照表如下：

| 问题                | 验证方式                           | 处理方式                                    |
| ------------------- | ---------------------------------- | ------------------------------------------- |
| Nacos 未启动        | `curl http://127.0.0.1:8848/nacos` | 启动 Nacos                                  |
| Provider 未注册     | 查看 Nacos 服务列表                | 检查 Provider 日志和注册中心地址            |
| Consumer 找不到服务 | 调用 REST 接口观察错误             | 检查 `group`、`version`、namespace          |
| 调用超时            | 模拟 Provider 慢调用               | 调整超时时间或优化 Provider                 |
| DTO 序列化失败      | 传输复杂对象                       | DTO 实现 `Serializable`，减少复杂字段       |
| 端口冲突            | `lsof -i:端口`                     | 修改 `server.port` 或 `dubbo.protocol.port` |
| 写接口重复执行      | 开启重试后模拟异常                 | 写接口配置 `retries=0`，增加幂等控制        |

## 部署说明

部署阶段需要解决三个问题：如何打包、如何注入环境变量、如何管理多环境配置。Spring Boot 3 + Dubbo 项目建议打包为可执行 Jar，再通过 Docker、Kubernetes 或传统服务器进程方式部署。

部署时需要重点区分两个端口：`server.port` 是 HTTP 端口，`dubbo.protocol.port` 是 Dubbo RPC 端口。Provider 必须暴露 Dubbo RPC 端口给 Consumer；Consumer 通常只需要暴露 HTTP 端口给上游调用方。

### 打包方式

项目使用 Maven 多模块结构时，推荐在父工程执行统一打包。

```bash
# 在父工程目录执行
mvn clean package -DskipTests
```

打包完成后，关键产物如下：

```text
dubbo-provider/target/dubbo-provider-1.0.0.jar
dubbo-consumer/target/dubbo-consumer-1.0.0.jar
```

如果只打包 Provider：

```bash
# 只打包 Provider，并自动构建它依赖的 dubbo-api
mvn clean package -pl dubbo-provider -am -DskipTests
```

如果只打包 Consumer：

```bash
# 只打包 Consumer，并自动构建它依赖的 dubbo-api
mvn clean package -pl dubbo-consumer -am -DskipTests
```

`-pl` 表示指定构建模块；`-am` 表示同时构建该模块依赖的其他模块，例如 `dubbo-api`。

Provider 的 Dockerfile 示例：

文件位置：`dubbo-provider/Dockerfile`

```dockerfile
# 使用 JDK 17 运行 Spring Boot 3 应用
FROM eclipse-temurin:17-jre

# 应用工作目录
WORKDIR /app

# 复制 Provider 可执行 Jar
COPY target/dubbo-provider-1.0.0.jar app.jar

# Provider HTTP 端口
EXPOSE 8080

# Provider Dubbo RPC 端口
EXPOSE 20880

# 启动参数通过环境变量注入，便于多环境部署
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar ${APP_OPTS}"]
```

Consumer 的 Dockerfile 示例：

文件位置：`dubbo-consumer/Dockerfile`

```dockerfile
# 使用 JDK 17 运行 Spring Boot 3 应用
FROM eclipse-temurin:17-jre

# 应用工作目录
WORKDIR /app

# 复制 Consumer 可执行 Jar
COPY target/dubbo-consumer-1.0.0.jar app.jar

# Consumer REST 接口端口
EXPOSE 8081

# 启动参数通过环境变量注入，便于多环境部署
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar ${APP_OPTS}"]
```

构建镜像：

```bash
# 构建 Provider 镜像
docker build -t apache-dubbo-demo/dubbo-provider:1.0.0 dubbo-provider

# 构建 Consumer 镜像
docker build -t apache-dubbo-demo/dubbo-consumer:1.0.0 dubbo-consumer
```

本地运行 Provider 容器：

```bash
# 启动 Provider 容器
docker run -d \
  --name dubbo-provider \
  -p 8080:8080 \
  -p 20880:20880 \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  -e APP_OPTS="--spring.profiles.active=dev --DUBBO_REGISTRY_ADDRESS=nacos://host.docker.internal:8848" \
  apache-dubbo-demo/dubbo-provider:1.0.0
```

本地运行 Consumer 容器：

```bash
# 启动 Consumer 容器
docker run -d \
  --name dubbo-consumer \
  -p 8081:8081 \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  -e APP_OPTS="--spring.profiles.active=dev --DUBBO_REGISTRY_ADDRESS=nacos://host.docker.internal:8848" \
  apache-dubbo-demo/dubbo-consumer:1.0.0
```

如果容器和 Nacos 在同一个 Docker Compose 网络中，注册中心地址应使用服务名，例如 `nacos://nacos:8848`，而不是 `127.0.0.1`。容器内的 `127.0.0.1` 指的是容器自身，不是宿主机。

### 环境变量配置

部署时不建议把注册中心地址、端口、环境名称、命名空间、账号密码等配置写死在 `application.yml` 中。推荐在配置文件中使用占位符，并通过环境变量注入。

Provider 配置示例：

文件位置：`dubbo-provider/src/main/resources/application.yml`

```yaml
server:
  # Provider HTTP 端口
  port: ${SERVER_PORT:8080}

spring:
  application:
    # Spring Boot 应用名称
    name: ${SPRING_APPLICATION_NAME:dubbo-provider}
  profiles:
    # 当前激活环境
    active: ${SPRING_PROFILES_ACTIVE:dev}

dubbo:
  application:
    # Dubbo 应用名称
    name: ${DUBBO_APPLICATION_NAME:dubbo-provider}

  protocol:
    # Dubbo RPC 协议
    name: ${DUBBO_PROTOCOL_NAME:dubbo}
    # Dubbo RPC 端口
    port: ${DUBBO_PROTOCOL_PORT:20880}

  registry:
    # 注册中心地址
    address: ${DUBBO_REGISTRY_ADDRESS:nacos://127.0.0.1:8848}
    # Nacos 命名空间
    parameters.namespace: ${NACOS_NAMESPACE:}
    # Nacos 分组
    group: ${NACOS_GROUP:DEFAULT_GROUP}

  provider:
    # Provider 默认超时时间
    timeout: ${DUBBO_PROVIDER_TIMEOUT:3000}
    # Provider 默认重试次数
    retries: ${DUBBO_PROVIDER_RETRIES:0}
```

Consumer 配置示例：

文件位置：`dubbo-consumer/src/main/resources/application.yml`

```yaml
server:
  # Consumer REST 端口
  port: ${SERVER_PORT:8081}

spring:
  application:
    # Spring Boot 应用名称
    name: ${SPRING_APPLICATION_NAME:dubbo-consumer}
  profiles:
    # 当前激活环境
    active: ${SPRING_PROFILES_ACTIVE:dev}

dubbo:
  application:
    # Dubbo 应用名称
    name: ${DUBBO_APPLICATION_NAME:dubbo-consumer}

  registry:
    # 注册中心地址
    address: ${DUBBO_REGISTRY_ADDRESS:nacos://127.0.0.1:8848}
    # Nacos 命名空间，必须与 Provider 一致
    parameters.namespace: ${NACOS_NAMESPACE:}
    # Nacos 分组，必须与 Provider 一致
    group: ${NACOS_GROUP:DEFAULT_GROUP}

  consumer:
    # 启动时是否检查 Provider 存在
    check: ${DUBBO_CONSUMER_CHECK:false}
    # Consumer 默认调用超时时间
    timeout: ${DUBBO_CONSUMER_TIMEOUT:3000}
    # Consumer 默认重试次数
    retries: ${DUBBO_CONSUMER_RETRIES:0}
```

常用环境变量说明：

| 环境变量                 | 说明                  | 示例                  |
| ------------------------ | --------------------- | --------------------- |
| `SERVER_PORT`            | Spring Boot HTTP 端口 | `8080`、`8081`        |
| `SPRING_PROFILES_ACTIVE` | 当前环境              | `dev`、`test`、`prod` |
| `DUBBO_APPLICATION_NAME` | Dubbo 应用名          | `dubbo-provider`      |
| `DUBBO_REGISTRY_ADDRESS` | 注册中心地址          | `nacos://nacos:8848`  |
| `NACOS_NAMESPACE`        | Nacos 命名空间        | `dev-namespace-id`    |
| `NACOS_GROUP`            | Nacos 分组            | `DEFAULT_GROUP`       |
| `DUBBO_PROTOCOL_PORT`    | Provider RPC 端口     | `20880`               |
| `DUBBO_CONSUMER_TIMEOUT` | Consumer 调用超时     | `3000`                |
| `DUBBO_CONSUMER_RETRIES` | Consumer 重试次数     | `0`                   |

传统服务器启动脚本示例：

文件位置：`scripts/start-provider.sh`

```bash
#!/usr/bin/env bash

# Provider 启动脚本，适合传统服务器部署

export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=prod
export DUBBO_APPLICATION_NAME=dubbo-provider
export DUBBO_PROTOCOL_PORT=20880
export DUBBO_REGISTRY_ADDRESS=nacos://nacos.example.com:8848
export NACOS_NAMESPACE=prod-namespace-id
export NACOS_GROUP=DEFAULT_GROUP
export DUBBO_PROVIDER_TIMEOUT=3000
export DUBBO_PROVIDER_RETRIES=0

java -Xms512m -Xmx1024m \
  -jar dubbo-provider/target/dubbo-provider-1.0.0.jar
```

文件位置：`scripts/start-consumer.sh`

```bash
#!/usr/bin/env bash

# Consumer 启动脚本，适合传统服务器部署

export SERVER_PORT=8081
export SPRING_PROFILES_ACTIVE=prod
export DUBBO_APPLICATION_NAME=dubbo-consumer
export DUBBO_REGISTRY_ADDRESS=nacos://nacos.example.com:8848
export NACOS_NAMESPACE=prod-namespace-id
export NACOS_GROUP=DEFAULT_GROUP
export DUBBO_CONSUMER_CHECK=false
export DUBBO_CONSUMER_TIMEOUT=3000
export DUBBO_CONSUMER_RETRIES=0

java -Xms512m -Xmx1024m \
  -jar dubbo-consumer/target/dubbo-consumer-1.0.0.jar
```

给脚本增加执行权限并启动：

```bash
# 增加执行权限
chmod +x scripts/start-provider.sh scripts/start-consumer.sh

# 启动 Provider
./scripts/start-provider.sh

# 启动 Consumer
./scripts/start-consumer.sh
```

### 多环境配置管理

多环境配置管理建议结合 Spring Profile、环境变量和注册中心 namespace 使用。不要为每个环境维护大量重复配置，也不要把生产环境敏感信息提交到代码仓库。

推荐配置文件结构如下：

```text
dubbo-provider
└── src/main/resources
    ├── application.yml
    ├── application-dev.yml
    ├── application-test.yml
    └── application-prod.yml

dubbo-consumer
└── src/main/resources
    ├── application.yml
    ├── application-dev.yml
    ├── application-test.yml
    └── application-prod.yml
```

`application.yml` 放公共配置：

```yaml
spring:
  profiles:
    # 默认开发环境，可通过环境变量覆盖
    active: ${SPRING_PROFILES_ACTIVE:dev}

dubbo:
  consumer:
    # 写操作默认不重试
    retries: 0
  provider:
    # Provider 默认不重试
    retries: 0
```

`application-dev.yml` 放本地开发配置：

```yaml
server:
  # 本地开发端口
  port: 8081

dubbo:
  registry:
    # 本地 Nacos
    address: nacos://127.0.0.1:8848
    # 本地开发可不配置 namespace
    parameters.namespace:
    group: DEFAULT_GROUP
  consumer:
    # 本地开发关闭启动检查
    check: false
    timeout: 3000
```

`application-test.yml` 放测试环境配置：

```yaml
server:
  # 测试环境端口
  port: ${SERVER_PORT:8081}

dubbo:
  registry:
    # 测试环境 Nacos 地址
    address: ${DUBBO_REGISTRY_ADDRESS:nacos://nacos-test.example.com:8848}
    # 测试环境 namespace
    parameters.namespace: ${NACOS_NAMESPACE:test-namespace-id}
    group: ${NACOS_GROUP:DEFAULT_GROUP}
  consumer:
    # 测试环境通常也可关闭启动检查，便于独立部署
    check: false
    timeout: 3000
```

`application-prod.yml` 放生产环境配置：

```yaml
server:
  # 生产环境端口由部署平台注入
  port: ${SERVER_PORT:8081}

dubbo:
  registry:
    # 生产环境 Nacos 地址必须通过环境变量或配置中心注入
    address: ${DUBBO_REGISTRY_ADDRESS}
    # 生产环境 namespace 必须显式指定
    parameters.namespace: ${NACOS_NAMESPACE}
    group: ${NACOS_GROUP:DEFAULT_GROUP}
  consumer:
    # 生产是否开启启动检查，需要结合发布策略决定
    check: ${DUBBO_CONSUMER_CHECK:false}
    timeout: ${DUBBO_CONSUMER_TIMEOUT:3000}
    retries: ${DUBBO_CONSUMER_RETRIES:0}
```

多环境管理建议如下：

| 配置项          | 推荐管理方式                            |
| --------------- | --------------------------------------- |
| 应用名          | 固定写在配置文件或通过环境变量覆盖      |
| HTTP 端口       | 环境变量注入                            |
| Dubbo RPC 端口  | Provider 通过环境变量注入               |
| 注册中心地址    | 环境变量或配置中心注入                  |
| Nacos namespace | 每个环境单独配置                        |
| Nacos group     | 默认统一，特殊场景再拆分                |
| 超时和重试      | 公共默认值 + 核心接口单独覆盖           |
| 账号密码        | 环境变量、密钥管理系统或部署平台 Secret |
| 日志级别        | 默认 info，排查时临时 debug             |

环境隔离建议采用以下规则：

```text
dev  环境：Nacos dev namespace
test 环境：Nacos test namespace
prod 环境：Nacos prod namespace

同一个环境内：
- Provider 和 Consumer 使用相同 namespace
- Provider 和 Consumer 使用相同 registry group
- Dubbo 服务通过 group 区分业务域
- Dubbo 服务通过 version 区分接口版本
```

不要只依赖 Dubbo `group` 做环境隔离。更清晰的方式是：Nacos namespace 隔离环境，Dubbo group 隔离业务分组，Dubbo version 管理接口版本。

## 总结

本文档围绕 Spring Boot 3 + Apache Dubbo 3 的开发流程，完成了从技术概述、环境准备、项目结构、依赖配置、公共接口、Provider 开发、Consumer 开发、注册中心集成、常用配置、异常处理、日志排查、本地测试到部署说明的完整梳理。

### 开发流程回顾

一个标准的 Dubbo 微服务开发流程可以概括为以下步骤：

```text
1. 创建 Maven 父工程
   统一管理 Spring Boot、Dubbo、Hutool、Lombok 等版本。

2. 创建 dubbo-api 模块
   定义 DTO、常量、异常和 Dubbo 服务接口。

3. 创建 dubbo-provider 模块
   依赖 dubbo-api，实现接口，使用 @DubboService 暴露服务。

4. 创建 dubbo-consumer 模块
   依赖 dubbo-api，使用 @DubboReference 引用远程服务。

5. 集成注册中心
   本地优先使用 Nacos，Provider 注册服务，Consumer 发现服务。

6. 配置超时、重试、负载均衡、分组和版本
   写接口关闭自动重试，核心接口单独设置 timeout。

7. 封装异常处理
   Provider 转换内部异常，Consumer 转换远程调用异常。

8. 补充日志与链路排查
   记录 traceId、服务名、方法名、耗时和错误码。

9. 本地启动验证
   按 Nacos、Provider、Consumer、REST 调用顺序验证。

10. 打包部署
    使用 Jar 或 Docker 镜像部署，通过环境变量管理多环境配置。
```

核心开发约定如下：

| 约定                | 说明                              |
| ------------------- | --------------------------------- |
| API 模块只放契约    | 不放实现类、Controller、Mapper    |
| Provider 只实现服务 | 不依赖 Consumer                   |
| Consumer 只引用接口 | 不依赖 Provider                   |
| DTO 必须可序列化    | 避免直接暴露数据库实体            |
| 写接口不自动重试    | 使用 `retries = 0`                |
| 分组和版本必须一致  | Provider 与 Consumer 配置保持一致 |
| 注册中心环境隔离    | 优先使用 Nacos namespace          |
| 异常统一封装        | 不直接暴露底层异常                |
| 日志带 traceId      | 便于跨服务排查                    |

最小可用调用链路如下：

```text
UserController
    ↓
UserRemoteService
    ↓
@DubboReference UserDubboService
    ↓
Nacos / ZooKeeper 服务发现
    ↓
@DubboService UserDubboServiceImpl
    ↓
返回 UserDTO
```

### 后续扩展方向

完成基础 Dubbo 调用后，可以继续从服务治理、可观测性、可靠性和工程规范四个方向扩展。

服务治理方向：

| 扩展项             | 说明                                        |
| ------------------ | ------------------------------------------- |
| 服务分组与版本治理 | 建立统一的 `group`、`version` 命名规范      |
| 灰度发布           | 新旧版本同时暴露，Consumer 分批迁移         |
| 路由规则           | 按环境、标签、实例、机房进行流量路由        |
| 权重配置           | 不同 Provider 实例分配不同流量比例          |
| 参数校验           | 统一校验 DTO 入参，减少 Provider 防御性代码 |

可观测性方向：

| 扩展项   | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 链路追踪 | 接入 Micrometer Tracing、SkyWalking、Zipkin 或 OpenTelemetry |
| 指标监控 | 统计 QPS、P95、P99、错误率、超时率                           |
| 日志聚合 | 接入 ELK、Loki 或云日志平台                                  |
| 调用审计 | 记录核心接口调用方、参数摘要和结果状态                       |
| 告警规则 | 对超时、错误率、Provider 下线建立告警                        |

可靠性方向：

| 扩展项   | 说明                                           |
| -------- | ---------------------------------------------- |
| 业务幂等 | 对写接口使用请求流水号、唯一索引或状态机       |
| 熔断降级 | 接入 Sentinel、Resilience4j 或网关治理能力     |
| 限流保护 | 对高频接口、核心资源接口配置限流               |
| 超时治理 | 根据接口耗时分级设置 timeout                   |
| 容量压测 | 在多 Provider 实例下验证线程池、连接数和吞吐量 |

工程规范方向：

| 扩展项           | 说明                                      |
| ---------------- | ----------------------------------------- |
| API 版本发布规范 | API 变更需要兼容性评审                    |
| DTO 字段规范     | 禁止直接暴露 Entity，字段变更记录版本     |
| 统一异常码       | Provider 和 Consumer 共用错误码体系       |
| 统一返回对象     | REST 层统一 `Result<T>`                   |
| 自动化测试       | 增加 Provider 单元测试、Consumer 集成测试 |
| CI/CD            | 构建、测试、镜像、部署流水线自动化        |

最终建议将 Spring Boot 3 + Dubbo 项目沉淀成团队脚手架。脚手架中预置父工程、API 模块、Provider 模块、Consumer 模块、Nacos 配置、统一异常、统一日志、Dockerfile、启动脚本和基础测试用例。这样后续新建微服务时，只需要关注业务接口和业务实现，不需要重复搭建底层工程结构。