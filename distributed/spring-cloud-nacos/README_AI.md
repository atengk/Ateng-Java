# Spring Cloud Alibaba Nacos

## 技术概述

本章节用于说明 Spring Boot 3、Spring Cloud Alibaba 与 Nacos 在微服务体系中的职责边界。后续项目依赖、注册中心、配置中心、多环境配置、服务调用等内容，都应围绕这三者的协作关系展开。

### Spring Boot 3 简介

Spring Boot 3 是 Spring 生态中用于快速构建独立、可运行、生产级 Spring 应用的基础框架。它通过自动配置、Starter 依赖、内嵌 Web 容器、统一配置体系、Actuator 运维能力等机制，降低了传统 Spring 项目的环境搭建和 XML 配置成本。Spring Boot 官方定位是帮助开发者创建可以直接运行的独立 Spring 应用，并尽量减少手工配置。([Home](https://docs.spring.io/spring-boot/3.5/index.html))

在微服务项目中，Spring Boot 3 通常承担单个服务应用的基础运行职责，例如用户服务、订单服务、网关服务、任务服务等。每个服务都可以通过 `java -jar` 独立启动，并通过配置文件、环境变量或启动参数完成端口、数据源、日志、注册中心、配置中心等运行参数的注入。([Home](https://docs.spring.io/spring-boot/3.5/index.html))

Spring Boot 3 的关键变化是全面进入 Java 17 与 Jakarta EE 体系。以 Spring Boot 3.5.x 为例，官方要求至少 Java 17，并依赖 Spring Framework 6.2.x 及以上版本；Maven 需要 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html)) 因此，基于 Spring Boot 3 开发微服务时，不应再使用 JDK 8、`javax.*` Servlet API 或老版本 Spring Cloud 组件。

核心特点可以概括为：

| 特性         | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 自动配置     | 根据 classpath、配置项和运行环境自动装配常用 Bean      |
| Starter 依赖 | 通过 `spring-boot-starter-*` 简化 Maven 依赖管理       |
| 内嵌容器     | 默认支持内嵌 Tomcat、Jetty、Undertow，不强制部署 WAR   |
| 外部化配置   | 支持 `application.yml`、环境变量、启动参数、配置中心等 |
| 生产可观测   | 通过 Actuator 提供健康检查、指标、环境信息等能力       |
| Java 17 基线 | 适配 Spring Framework 6、Jakarta EE、现代 JVM 特性     |

### Spring Cloud Alibaba 简介

Spring Cloud Alibaba 是面向分布式应用开发的一组 Spring Cloud 扩展组件，主要用于将阿里巴巴中间件能力整合到 Spring Boot / Spring Cloud 项目中。它提供服务注册发现、分布式配置、限流降级、消息驱动、分布式事务等能力，核心组件通常包括 Nacos、Sentinel、RocketMQ、Seata 等。Spring 官方项目页也将 Spring Cloud Alibaba 描述为面向 Alibaba middleware 的分布式应用开发一站式方案。([Home](https://spring.io/projects/spring-cloud-alibaba?utm_source=chatgpt.com))

在 Spring Boot 3 项目中，Spring Cloud Alibaba 主要负责把 Nacos、Sentinel、RocketMQ、Seata 等中间件接入 Spring Cloud 编程模型。例如，服务注册发现可以使用 `spring-cloud-starter-alibaba-nacos-discovery`，配置中心可以使用 `spring-cloud-starter-alibaba-nacos-config`，应用层代码仍然保持 Spring Boot 风格。

当前如果以 Spring Boot 3.5.x 作为新项目基线，建议选择 Spring Cloud 2025.0.x 与 Spring Cloud Alibaba 2025.0.0.0。Spring Cloud 官方兼容矩阵显示，2025.0.x 对应 Spring Boot 3.5.x；Spring Cloud Alibaba 官方 2025.x 说明显示，2025.0.0.0 对应 Spring Cloud 2025.0.0、Spring Boot 3.5.0，并管理 Nacos 3.0.3 客户端。([Home](https://spring.io/projects/spring-cloud/?utm_source=chatgpt.com))

版本选择建议如下：

| 开发路线       | Spring Boot | Spring Cloud | Spring Cloud Alibaba | Nacos Client | 适用场景                         |
| -------------- | ----------- | ------------ | -------------------- | ------------ | -------------------------------- |
| 推荐新项目路线 | 3.5.x       | 2025.0.x     | 2025.0.0.0           | 3.0.3        | 新建 Spring Boot 3 微服务项目    |
| 稳定保守路线   | 3.2.x       | 2023.0.x     | 2023.0.1.0           | 2.3.2        | 已基于 Boot 3.2 的项目或课程环境 |
| 不建议混用     | 3.5.x       | 2023.0.x     | 2023.0.x             | 2.x          | 容易触发 Spring Cloud 兼容性问题 |

### Nacos 功能定位

Nacos 在 Spring Cloud Alibaba 体系中主要承担两个核心职责：服务注册发现和配置中心。服务注册发现用于让微服务实例启动后自动注册到 Nacos，并让调用方可以按服务名发现可用实例；配置中心用于把 `application.yml` 中不适合写死的配置放到 Nacos Server，由服务在启动或运行期间读取。

在注册中心场景中，Nacos 负责维护服务名、实例 IP、端口、健康状态、权重、元数据等信息。服务提供方启动后注册实例，服务消费方通过服务名拉取实例列表，再结合 Spring Cloud LoadBalancer、OpenFeign 或 RestTemplate 完成服务调用。

在配置中心场景中，Nacos 负责集中存储配置内容，并通过 `dataId`、`group`、`namespace` 组织配置。应用启动时会从 Nacos 拉取配置，运行过程中配置变更后可以触发动态刷新，适合管理数据库连接、业务开关、第三方接口地址、限流参数、灰度参数等内容。

需要注意，Nacos 官方明确建议将 Nacos 部署在内部隔离网络中，不建议暴露在公网环境；快速开始中的单机模式主要用于测试，生产环境应使用集群模式并开启鉴权。([Nacos 官网](https://nacos.io/en/docs/next/quickstart/quick-start/))

## 环境准备

本章节用于准备开发 Spring Boot 3 + Spring Cloud Alibaba + Nacos 所需的基础环境。建议先统一 JDK、Maven、Spring Boot、Spring Cloud、Spring Cloud Alibaba 和 Nacos Server 版本，再创建项目，避免后续依赖冲突。

### JDK 版本要求

Spring Boot 3 项目必须使用 JDK 17 或更高版本。以 Spring Boot 3.5.x 为例，官方要求 Java 17+，并兼容到 Java 25；Nacos 3.x 快速开始文档也要求 64 位 JDK 17+。因此，本开发大纲建议统一使用 JDK 17 或 JDK 21，其中 JDK 17 适合作为最低兼容基线，JDK 21 适合作为长期支持版本。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

开发环境建议如下：

| 工具     | 推荐版本                | 说明                                      |
| -------- | ----------------------- | ----------------------------------------- |
| JDK      | 17 或 21                | Spring Boot 3 与 Nacos 3.x 的基础要求     |
| Maven    | 3.9.x                   | 高于 Spring Boot 3.5.x 要求的 Maven 3.6.3 |
| Docker   | 24+                     | 用于本地快速启动 Nacos Server             |
| IDE      | IntelliJ IDEA 2024+     | 推荐启用 Lombok、Maven 自动导入           |
| 操作系统 | Linux / macOS / Windows | Nacos 官方支持多平台，生产优先 Linux      |

检查本地 Java 与 Maven 版本：

```bash
# 查看当前 JDK 版本，确认主版本不低于 17
java -version

# 查看 Maven 版本，确认 Maven 能识别当前 JAVA_HOME
mvn -v

# 查看 JAVA_HOME，确认 IDE、Maven、命令行使用同一套 JDK
echo "$JAVA_HOME"
```

如果 `java -version` 显示的是 1.8、8、11，说明当前终端环境不满足 Spring Boot 3 开发要求，需要先切换到 JDK 17 或 JDK 21。

### Spring Boot 与 Spring Cloud Alibaba 版本选择

Spring Boot、Spring Cloud 与 Spring Cloud Alibaba 必须按兼容矩阵选择，不建议只升级其中一个组件。Spring Cloud 官方兼容矩阵显示，Spring Cloud 2025.0.x 对应 Spring Boot 3.5.x，Spring Cloud 2023.0.x 对应 Spring Boot 3.2.x / 3.3.x。([Home](https://spring.io/projects/spring-cloud/?utm_source=chatgpt.com))

推荐的新项目版本组合如下：

```xml
<!-- pom.xml：统一维护项目版本，后续依赖配置章节可直接复用 -->
<properties>
    <!-- Spring Boot 3.5.x 当前稳定线 -->
    <spring-boot.version>3.5.14</spring-boot.version>

    <!-- Spring Cloud 2025.0.x 对应 Spring Boot 3.5.x -->
    <spring-cloud.version>2025.0.0</spring-cloud.version>

    <!-- Spring Cloud Alibaba 2025.0.0.0 对应 Spring Cloud 2025.0.0、Spring Boot 3.5.0 -->
    <spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>

    <!-- Spring Boot 3 与 Spring Cloud Alibaba 2025.x 均要求 Java 17+ -->
    <java.version>17</java.version>
</properties>
```

如果课程、公司脚手架或已有项目固定在 Spring Boot 3.2.x，则使用以下组合更稳妥：

```xml
<!-- pom.xml：Spring Boot 3.2.x 保守路线 -->
<properties>
    <!-- Spring Boot 3.2.x 路线 -->
    <spring-boot.version>3.2.4</spring-boot.version>

    <!-- Spring Cloud 2023.0.x 对应 Spring Boot 3.2.x / 3.3.x -->
    <spring-cloud.version>2023.0.1</spring-cloud.version>

    <!-- Spring Cloud Alibaba 2023.0.1.0 对应 Spring Boot 3.2.4 -->
    <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>

    <!-- Spring Boot 3 最低要求 -->
    <java.version>17</java.version>
</properties>
```

版本选择原则如下：

| 原则                         | 说明                                               |
| ---------------------------- | -------------------------------------------------- |
| 先定 Spring Boot             | 项目主版本由 Spring Boot 决定                      |
| 再定 Spring Cloud            | 按 Spring Cloud 官方兼容矩阵选择 release train     |
| 最后定 Spring Cloud Alibaba  | 按 Spring Cloud Alibaba 官方版本说明选择对应版本   |
| 不手动指定 Nacos Client      | 优先交给 Spring Cloud Alibaba BOM 管理             |
| 不混用 Boot 2 与 Boot 3 依赖 | Boot 3 使用 Jakarta EE，老依赖可能仍依赖 `javax.*` |

Spring Cloud Alibaba 2025.0.0.0 的官方版本说明中，Nacos 组件版本为 3.0.3；如果使用 Spring Cloud Alibaba 2023.0.1.0，则 Nacos 组件版本为 2.3.2。([Spring Cloud Alibaba](https://sca.aliyun.com/en/docs/2025.x/overview/version-explain/?utm_source=chatgpt.com)) 实际开发中，应用侧 Nacos Client 建议由 BOM 管理，Nacos Server 则可按公司环境或本地开发环境统一部署。

### Nacos Server 安装与启动

本地开发建议优先使用 Docker 启动 Nacos Server，便于快速创建、停止和清理环境。当前 Nacos 最新文档推荐生产稳定版本为 3.2.0，并要求 JDK 17+；Nacos 3.x 控制台默认端口为 `8080`，客户端访问和 OpenAPI 常用端口仍是 `8848`，gRPC 通信端口常用 `9848`。([Nacos 官网](https://nacos.io/en/docs/next/quickstart/quick-start/))

本地 Docker 单机启动示例：

```bash
# 指定 Nacos Server 版本，开发环境建议固定版本，避免 latest 自动变化
export NACOS_VERSION=3.2.0

# 生成 Nacos 鉴权 Token，生产环境必须使用独立安全随机值
export NACOS_AUTH_TOKEN="$(openssl rand -base64 48)"

# 设置 Nacos Server 内部身份标识，生产环境不要使用示例值
export NACOS_AUTH_IDENTITY_KEY="nacos-server-identity"
export NACOS_AUTH_IDENTITY_VALUE="$(openssl rand -hex 16)"

# 启动单机 Derby 模式，仅适合本地开发和功能验证
docker run --name nacos-standalone-derby \
  -e MODE=standalone \
  -e NACOS_AUTH_TOKEN="${NACOS_AUTH_TOKEN}" \
  -e NACOS_AUTH_IDENTITY_KEY="${NACOS_AUTH_IDENTITY_KEY}" \
  -e NACOS_AUTH_IDENTITY_VALUE="${NACOS_AUTH_IDENTITY_VALUE}" \
  -p 8080:8080 \
  -p 8848:8848 \
  -p 9848:9848 \
  -d nacos/nacos-server:${NACOS_VERSION}
```

启动后查看日志：

```bash
# 查看 Nacos 启动日志
docker logs -f nacos-standalone-derby
```

看到类似以下日志时，表示 Nacos Server 已启动完成：

```text
Nacos started successfully in stand alone mode. use embedded storage
```

浏览器访问控制台地址：

```text
http://127.0.0.1:8080
```

Nacos 3.x 首次访问控制台时，会要求初始化 `nacos` 管理员用户的密码。初始化后，应在后续 Spring Boot 应用配置中使用对应的用户名和密码连接 Nacos。

如果不使用 Docker，也可以下载 Nacos Server 二进制包手动启动：

```bash
# 解压 Nacos Server 安装包
unzip nacos-server-${NACOS_VERSION}.zip

# 进入启动脚本目录
cd nacos/bin

# Linux / macOS 单机模式启动
sh startup.sh -m standalone

# 如果 Ubuntu 下 sh 执行脚本出现 [[ 兼容问题，改用 bash
bash startup.sh -m standalone
```

Windows 环境启动命令：

```bash
# Windows CMD 单机模式启动
startup.cmd -m standalone
```

Nacos 3.0.0 起，控制台鉴权默认开启；如果配置文件中未提前设置鉴权相关参数，启动程序会提示输入 `nacos.core.auth.plugin.nacos.token.secret.key`、`nacos.core.auth.server.identity.key`、`nacos.core.auth.server.identity.value` 三项配置。([Nacos 官网](https://nacos.io/en/docs/next/quickstart/quick-start/))

停止 Docker 方式启动的 Nacos：

```bash
# 停止容器
docker stop nacos-standalone-derby

# 删除容器，保留镜像
docker rm nacos-standalone-derby
```

停止二进制包方式启动的 Nacos：

```bash
# Linux / macOS 停止 Nacos
sh shutdown.sh

# Windows 停止 Nacos
shutdown.cmd
```

开发环境可以使用单机 Derby 模式快速验证服务注册和配置中心功能；测试、预发、生产环境不建议使用 Derby 单机模式，应使用集群部署、外部 MySQL 存储、鉴权、内网访问控制和监控告警。Nacos 官方文档也明确提示，快速开始生成的单机服务主要用于测试，生产环境应部署集群并开启鉴权。([Nacos 官网](https://nacos.io/en/docs/next/quickstart/quick-start/))



## 项目依赖配置

本章节用于说明 Spring Boot 3 项目接入 Spring Cloud Alibaba Nacos 时需要添加的 Maven 依赖、BOM 版本管理方式，以及基础配置文件的组织结构。Nacos 相关依赖不建议手动指定单个 Jar 版本，应优先通过 Spring Boot、Spring Cloud、Spring Cloud Alibaba 的 BOM 统一管理。

### Maven 依赖引入

Maven 依赖主要分为三类：Spring Boot 基础依赖、Nacos 注册中心依赖、Nacos 配置中心依赖。Spring Cloud Alibaba 2025.0.0.0 官方适配 Spring Cloud 2025.0.0 与 Spring Boot 3.5.0，Nacos 组件版本为 3.0.3；Spring Cloud 官方兼容矩阵中，Spring Cloud 2025.0.x 对应 Spring Boot 3.5.x。([Spring Cloud Alibaba](https://sca.aliyun.com/en/docs/2025.x/overview/version-explain/?utm_source=chatgpt.com))

文件位置：`pom.xml`

以下配置用于创建一个支持 Nacos 服务注册、服务发现、配置中心和健康检查的 Spring Boot 3 微服务项目。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
            http://maven.apache.org/POM/4.0.0
            https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 父工程：统一管理 Spring Boot 插件和基础依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.0</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>ateng-nacos-demo</artifactId>
    <version>1.0.0</version>
    <name>ateng-nacos-demo</name>
    <description>Spring Boot 3 Spring Cloud Alibaba Nacos Demo</description>

    <properties>
        <!-- Spring Boot 3 要求 JDK 17+ -->
        <java.version>17</java.version>

        <!-- Spring Cloud 2025.0.x 适配 Spring Boot 3.5.x -->
        <spring-cloud.version>2025.0.0</spring-cloud.version>

        <!-- Spring Cloud Alibaba 2025.0.0.0 适配 Spring Cloud 2025.0.0 -->
        <spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>

        <!-- Maven 编译编码 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Web 基础依赖：提供 REST 接口、内嵌 Tomcat、Spring MVC 等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Nacos 注册中心：服务注册、服务发现、实例健康状态维护 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <!-- Nacos 配置中心：从 Nacos Server 读取远程配置并支持动态刷新 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>

        <!-- Actuator：提供健康检查、应用信息、指标端点，便于注册中心和运维系统识别应用状态 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Hutool：常用工具类集合，便于后续示例处理字符串、集合、JSON、日期等场景 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.8.39</version>
        </dependency>

        <!-- Lombok：减少 Getter、Setter、构造器、日志对象等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test：Spring Boot 单元测试和集成测试基础依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud BOM：统一管理 Spring Cloud 组件版本 -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Spring Cloud Alibaba BOM：统一管理 Nacos、Sentinel、RocketMQ、Seata 等组件版本 -->
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件：用于打包可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <!-- 打包时排除 Lombok，避免无意义依赖进入运行包 -->
                    <excludes>
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

如果只使用 Nacos 注册中心，可以只保留 `spring-cloud-starter-alibaba-nacos-discovery`；如果只使用 Nacos 配置中心，可以只保留 `spring-cloud-starter-alibaba-nacos-config`。实际微服务项目通常两者都会引入，因为服务注册和配置管理经常同时使用。

### BOM 版本管理

BOM 的作用是统一依赖版本，避免手动指定多个 Spring Cloud、Spring Cloud Alibaba、Nacos Client 依赖版本导致冲突。Spring Cloud 官方也建议通过 `spring-cloud-dependencies` BOM 管理 Spring Cloud 相关组件版本。([Home](https://spring.io/projects/spring-cloud/?utm_source=chatgpt.com))

在 Spring Boot 3 + Spring Cloud Alibaba 项目中，推荐按照以下顺序管理版本：

```xml
<dependencyManagement>
    <dependencies>
        <!-- 先导入 Spring Cloud BOM -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- 再导入 Spring Cloud Alibaba BOM -->
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

版本组合建议如下：

| Spring Boot   | Spring Cloud | Spring Cloud Alibaba      | 说明                          |
| ------------- | ------------ | ------------------------- | ----------------------------- |
| 3.5.x         | 2025.0.x     | 2025.0.0.0                | 推荐用于 Spring Boot 3 新项目 |
| 3.4.x         | 2024.0.x     | 需按 Alibaba 官方矩阵选择 | 适合已有 Boot 3.4 项目        |
| 3.2.x / 3.3.x | 2023.0.x     | 2023.0.x                  | 适合旧项目维护或课程环境      |

当前文档后续示例默认采用以下版本：

```xml
<properties>
    <!-- JDK 版本 -->
    <java.version>17</java.version>

    <!-- Spring Boot 版本由 parent 管理 -->
    <spring-boot.version>3.5.0</spring-boot.version>

    <!-- Spring Cloud 版本 -->
    <spring-cloud.version>2025.0.0</spring-cloud.version>

    <!-- Spring Cloud Alibaba 版本 -->
    <spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>
</properties>
```

依赖管理注意事项：

| 注意事项                           | 说明                                                     |
| ---------------------------------- | -------------------------------------------------------- |
| 不要手动指定 Nacos Client 版本     | 交给 Spring Cloud Alibaba BOM 管理，减少兼容性问题       |
| 不要混用 Spring Boot 2 依赖        | Spring Boot 3 已切换到 Jakarta EE 体系，旧依赖可能不兼容 |
| 不要随意升级单个 Spring Cloud 组件 | 应按 release train 整体升级                              |
| 不要同时使用多个版本 BOM           | 多套 BOM 可能造成依赖覆盖关系不清晰                      |
| 公司项目应锁定版本                 | 不建议使用 `LATEST`、`RELEASE` 或不固定版本的依赖        |

可以通过以下命令查看最终依赖树：

```bash
# 查看 Nacos 相关依赖版本
mvn dependency:tree -Dincludes=com.alibaba.nacos

# 查看 Spring Cloud Alibaba 相关依赖
mvn dependency:tree -Dincludes=com.alibaba.cloud

# 查看是否存在冲突依赖
mvn dependency:tree -Dverbose
```

其中 `mvn dependency:tree` 用于输出 Maven 最终解析后的依赖树，`-Dincludes` 可以过滤指定 groupId 或 artifactId，便于确认 Nacos Client、Spring Cloud Alibaba Starter 等依赖是否由 BOM 正确管理。

### 基础配置文件结构

Spring Boot 3 接入 Nacos 配置中心时，推荐使用 `spring.config.import` 引入 Nacos 配置。Nacos 官方 Spring Cloud 接入文档也展示了通过 `spring.config.import` 指定一个或多个 Nacos 配置源，并通过 `spring.cloud.nacos.config.server-addr` 指定 Nacos 地址的方式。([Nacos 官网](https://nacos.io/docs/latest/ecology/use-nacos-with-spring-cloud/?utm_source=chatgpt.com))

推荐的基础目录结构如下：

```text
ateng-nacos-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               └── nacos
        │                   └── AtengNacosDemoApplication.java
        └── resources
            ├── application.yml
            ├── application-dev.yml
            ├── application-test.yml
            └── application-prod.yml
```

文件说明：

| 文件                   | 作用                                                       |
| ---------------------- | ---------------------------------------------------------- |
| `application.yml`      | 公共基础配置，放应用名、激活环境、Nacos 地址、配置导入规则 |
| `application-dev.yml`  | 本地开发环境配置                                           |
| `application-test.yml` | 测试环境配置                                               |
| `application-prod.yml` | 生产环境配置                                               |
| Nacos 远程配置         | 存放可动态调整的业务配置、中间件配置、开关配置             |

文件位置：`src/main/resources/application.yml`

以下配置作为本地和多环境的公共入口，负责声明应用名、激活环境、Nacos 注册中心和配置中心地址。

```yaml
server:
  # 当前服务本地端口，不同服务需要使用不同端口
  port: 18080

spring:
  application:
    # 服务名：同时作为 Nacos 注册中心服务名，建议使用小写中划线格式
    name: ateng-nacos-demo

  profiles:
    # 默认激活 dev 环境，可通过启动参数覆盖：--spring.profiles.active=test
    active: dev

  config:
    import:
      # 公共配置：适合存放所有环境共享的配置项
      - optional:nacos:${spring.application.name}.yml?group=DEFAULT_GROUP&refreshEnabled=true

      # 环境配置：适合存放 dev/test/prod 差异化配置
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml?group=DEFAULT_GROUP&refreshEnabled=true

  cloud:
    nacos:
      # Nacos Server 地址；Nacos 3.x 控制台端口通常是 8080，客户端访问仍常用 8848
      server-addr: 127.0.0.1:8848

      # Nacos 用户名，开启鉴权后必须配置
      username: nacos

      # Nacos 密码，实际项目建议通过环境变量或启动参数注入
      password: nacos

      discovery:
        # 注册中心地址，未配置时默认复用 spring.cloud.nacos.server-addr
        server-addr: ${spring.cloud.nacos.server-addr}

        # 命名空间 ID，空值表示 public 命名空间
        namespace:

        # 服务分组，默认 DEFAULT_GROUP
        group: DEFAULT_GROUP

        # 是否启用服务注册
        enabled: true

        # 是否注册临时实例，默认 true
        ephemeral: true

      config:
        # 配置中心地址，未配置时默认复用 spring.cloud.nacos.server-addr
        server-addr: ${spring.cloud.nacos.server-addr}

        # 命名空间 ID，空值表示 public 命名空间
        namespace:

        # 配置分组，默认 DEFAULT_GROUP
        group: DEFAULT_GROUP

        # 配置文件扩展名，常用 yaml 或 yml
        file-extension: yml

        # 是否启用配置中心
        enabled: true

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查和基础信息端点，便于本地调试和服务状态检查
        include: health,info,env

  endpoint:
    health:
      # 显示详细健康信息，生产环境可按安全要求调整
      show-details: always

logging:
  level:
    # 调试 Nacos 注册与配置加载时可临时改为 debug
    com.alibaba.cloud.nacos: info
    com.alibaba.nacos: info
```

文件位置：`src/main/resources/application-dev.yml`

以下配置用于本地开发环境，通常连接本机 Nacos Server。

```yaml
spring:
  cloud:
    nacos:
      # 本地开发环境 Nacos 地址
      server-addr: 127.0.0.1:8848

      discovery:
        # dev 环境服务分组
        group: DEV_GROUP

      config:
        # dev 环境配置分组
        group: DEV_GROUP

logging:
  level:
    # 本地开发可适当提高日志级别，便于排查注册和配置加载问题
    io.github.atengk: debug
```

文件位置：`src/main/resources/application-test.yml`

以下配置用于测试环境，通常连接测试环境 Nacos Server。

```yaml
spring:
  cloud:
    nacos:
      # 测试环境 Nacos 地址，按实际环境替换
      server-addr: 192.168.10.100:8848

      discovery:
        # test 环境服务分组
        group: TEST_GROUP

      config:
        # test 环境配置分组
        group: TEST_GROUP

logging:
  level:
    # 测试环境保留主要业务日志
    io.github.atengk: info
```

文件位置：`src/main/resources/application-prod.yml`

以下配置用于生产环境，敏感信息建议通过环境变量或启动参数注入。

```yaml
spring:
  cloud:
    nacos:
      # 生产环境 Nacos 地址，建议使用内网域名或 VIP
      server-addr: ${NACOS_SERVER_ADDR}

      # 生产环境用户名通过环境变量注入
      username: ${NACOS_USERNAME}

      # 生产环境密码通过环境变量注入
      password: ${NACOS_PASSWORD}

      discovery:
        # prod 环境服务分组
        group: PROD_GROUP

        # 生产环境建议显式开启注册
        enabled: true

      config:
        # prod 环境配置分组
        group: PROD_GROUP

        # 生产环境建议配置不存在时直接启动失败，可去掉 application.yml 中 optional 前缀
        enabled: true

logging:
  level:
    # 生产环境不建议开启 debug 日志
    io.github.atengk: info
    com.alibaba.cloud.nacos: warn
    com.alibaba.nacos: warn
```

Nacos 配置中心中建议创建以下配置：

```text
# 公共配置
Data ID: ateng-nacos-demo.yml
Group: DEFAULT_GROUP 或对应环境分组
Format: YAML

# 开发环境配置
Data ID: ateng-nacos-demo-dev.yml
Group: DEV_GROUP
Format: YAML

# 测试环境配置
Data ID: ateng-nacos-demo-test.yml
Group: TEST_GROUP
Format: YAML

# 生产环境配置
Data ID: ateng-nacos-demo-prod.yml
Group: PROD_GROUP
Format: YAML
```

Nacos 中的示例配置内容：

```yaml
# Data ID: ateng-nacos-demo-dev.yml
# Group: DEV_GROUP

ateng:
  nacos:
    # 示例业务开关，后续可通过动态刷新验证配置中心是否生效
    enable-demo: true

    # 示例提示文本，接口读取后可直接观察配置变化
    message: "这是来自 Nacos dev 环境的配置"
```

启动项目时可以通过参数切换环境：

```bash
# 使用默认 dev 环境启动
mvn spring-boot:run

# 指定 test 环境启动
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test"

# Jar 包方式指定 prod 环境启动
java -jar target/ateng-nacos-demo-1.0.0.jar \
  --spring.profiles.active=prod \
  --NACOS_SERVER_ADDR=127.0.0.1:8848 \
  --NACOS_USERNAME=nacos \
  --NACOS_PASSWORD=nacos
```

其中 `--spring.profiles.active` 用于指定当前激活环境，`NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD` 用于给生产配置中的占位符赋值。生产环境不建议把 Nacos 密码直接写入 Git 仓库，应使用环境变量、Kubernetes Secret、配置管理平台或部署系统注入。



## Nacos 注册中心

Nacos 注册中心用于解决微服务实例动态注册、服务发现、实例上下线感知和服务名调用的问题。应用引入 `spring-cloud-starter-alibaba-nacos-discovery` 后，可以将当前服务注册到 Nacos Server，并通过 Spring Cloud 的服务发现能力获取其他服务实例。Nacos 官方 Spring Cloud 示例也采用 `spring.cloud.nacos.discovery.server-addr` 配置 Nacos 地址，并通过 `@EnableDiscoveryClient` 开启服务注册发现能力。([Nacos 官网](https://nacos.io/docs/latest/ecology/use-nacos-with-spring-cloud/?utm_source=chatgpt.com))

### 服务注册配置

服务注册配置用于让当前 Spring Boot 应用启动后自动注册到 Nacos。注册信息通常包括服务名、IP、端口、命名空间、分组、集群名、权重、元数据和健康状态。Nacos Discovery Starter 支持将服务实例自身的 host、port、健康检查地址、主页和元数据注册到 Nacos。([GitHub](https://github.com/alibaba/spring-cloud-alibaba/wiki/Nacos-discovery?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

以下配置用于将当前服务注册到本地 Nacos Server。

```yaml
server:
  # 当前服务端口
  port: 18081

spring:
  application:
    # 服务名：注册到 Nacos 后的服务标识，服务调用时也使用该名称
    name: ateng-nacos-provider

  cloud:
    nacos:
      # Nacos Server 客户端访问地址
      server-addr: 127.0.0.1:8848

      # Nacos 开启鉴权后需要配置用户名
      username: nacos

      # Nacos 开启鉴权后需要配置密码
      password: nacos

      discovery:
        # 注册中心地址，默认可复用 spring.cloud.nacos.server-addr
        server-addr: ${spring.cloud.nacos.server-addr}

        # 命名空间 ID，空值表示 public 命名空间
        namespace:

        # 服务分组，建议按环境或业务域划分
        group: DEFAULT_GROUP

        # 集群名称，适合按机房、可用区或地域划分
        cluster-name: DEFAULT

        # 当前实例权重，数值越大，被调用概率越高
        weight: 1

        # 是否开启服务注册发现
        enabled: true

        # 是否允许注册当前服务
        register-enabled: true

        # 是否注册为临时实例，默认 true
        ephemeral: true

        metadata:
          # 自定义元数据：可用于灰度、版本、地域、责任人等标识
          version: v1
          zone: local
          owner: ateng

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查端点，便于本地验证
        include: health,info

  endpoint:
    health:
      # 本地调试显示健康详情
      show-details: always

logging:
  level:
    # 查看 Nacos 注册发现相关日志
    com.alibaba.cloud.nacos: info
    com.alibaba.nacos: info
    io.github.atengk: debug
```

文件位置：`src/main/java/io/github/atengk/nacos/AtengNacosProviderApplication.java`

以下启动类用于开启 Spring Boot 应用和 Spring Cloud 服务发现能力。

```java
package io.github.atengk.nacos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Nacos 服务提供者启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@EnableDiscoveryClient
@SpringBootApplication
public class AtengNacosProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtengNacosProviderApplication.class, args);
    }

}
```

文件位置：`src/main/java/io/github/atengk/nacos/controller/ProviderEchoController.java`

以下接口用于验证服务提供者是否正常注册和对外提供服务。

```java
package io.github.atengk.nacos.controller;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务提供者测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class ProviderEchoController {

    /**
     * 返回服务提供者响应内容
     *
     * @param message 请求消息
     * @return 响应内容
     */
    @GetMapping("/provider/echo/{message}")
    public String echo(@PathVariable String message) {
        String safeMessage = StrUtil.blankToDefault(message, "default");
        String result = StrUtil.format("Nacos Provider 响应成功，message={}", safeMessage);
        log.info("服务提供者处理请求，message={}", safeMessage);
        return result;
    }

}
```

启动服务提供者后，可以通过以下方式验证本地接口是否正常：

```bash
# 调用服务提供者本地接口
curl http://127.0.0.1:18081/provider/echo/hello
```

预期返回：

```text
Nacos Provider 响应成功，message=hello
```

然后访问 Nacos 控制台，在“服务管理 / 服务列表”中确认服务名 `ateng-nacos-provider` 是否存在。如果服务存在且实例数为 1，说明当前服务已经完成注册。

### 服务发现配置

服务发现配置用于让服务消费者通过服务名发现服务提供者，而不是在代码中写死 IP 和端口。Spring Cloud LoadBalancer 支持从服务发现组件中获取实例，并为 `RestTemplate`、`RestClient`、`WebClient` 等客户端提供负载均衡能力；官方文档说明，使用负载均衡 `RestTemplate` 时，需要创建带有 `@LoadBalanced` 注解的 Bean，并在 classpath 中提供 Spring Cloud LoadBalancer 实现。([Home](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html?utm_source=chatgpt.com))

如果后续要使用 `RestTemplate` 按服务名调用，建议在消费者项目中显式加入 LoadBalancer 依赖。

文件位置：`pom.xml`

```xml
<!-- Spring Cloud LoadBalancer：支持通过服务名进行客户端负载均衡调用 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

文件位置：`src/main/resources/application.yml`

以下配置用于创建一个服务消费者，使其可以从 Nacos 发现 `ateng-nacos-provider` 服务。

```yaml
server:
  # 当前消费者服务端口
  port: 18082

spring:
  application:
    # 消费者服务名
    name: ateng-nacos-consumer

  cloud:
    nacos:
      # Nacos Server 客户端访问地址
      server-addr: 127.0.0.1:8848

      # Nacos 开启鉴权后需要配置用户名
      username: nacos

      # Nacos 开启鉴权后需要配置密码
      password: nacos

      discovery:
        # 注册中心地址
        server-addr: ${spring.cloud.nacos.server-addr}

        # 与提供者保持同一命名空间，否则无法发现服务
        namespace:

        # 与提供者保持同一分组，否则默认调用场景下可能无法匹配服务
        group: DEFAULT_GROUP

        # 是否开启服务发现
        enabled: true

        # 消费者也可以注册到 Nacos，便于观察调用链路和实例状态
        register-enabled: true

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查和服务发现相关调试端点
        include: health,info,beans

logging:
  level:
    # 本地调试服务发现和负载均衡
    io.github.atengk: debug
    org.springframework.cloud.loadbalancer: info
    com.alibaba.cloud.nacos: info
```

文件位置：`src/main/java/io/github/atengk/nacos/config/RestTemplateConfig.java`

以下配置创建一个支持服务名调用的 `RestTemplate`。

```java
package io.github.atengk.nacos.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 负载均衡配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建支持服务名调用的 RestTemplate
     *
     * @return RestTemplate 实例
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
```

文件位置：`src/main/java/io/github/atengk/nacos/controller/ConsumerCallController.java`

以下接口通过服务名 `ateng-nacos-provider` 调用服务提供者。

```java
package io.github.atengk.nacos.controller;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * 服务消费者调用接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ConsumerCallController {

    private final RestTemplate restTemplate;

    /**
     * 通过服务名调用服务提供者
     *
     * @param message 请求消息
     * @return 服务提供者响应
     */
    @GetMapping("/consumer/call/{message}")
    public String callProvider(@PathVariable String message) {
        String safeMessage = StrUtil.blankToDefault(message, "default");
        String url = StrUtil.format("http://ateng-nacos-provider/provider/echo/{}", safeMessage);
        log.info("准备调用服务提供者，url={}", url);
        return restTemplate.getForObject(url, String.class);
    }

}
```

文件位置：`src/main/java/io/github/atengk/nacos/controller/DiscoveryController.java`

以下接口用于查看消费者从 Nacos 发现到的服务实例列表。

```java
package io.github.atengk.nacos.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 服务发现调试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryClient discoveryClient;

    /**
     * 查询指定服务的实例列表
     *
     * @param serviceId 服务名
     * @return 服务实例列表
     */
    @GetMapping("/discovery/instances/{serviceId}")
    public List<Map<String, Object>> instances(@PathVariable String serviceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        log.info("查询服务实例列表，serviceId={}，实例数量={}", serviceId, instances.size());

        if (CollUtil.isEmpty(instances)) {
            return List.of();
        }

        return instances.stream()
                .map(instance -> MapUtil.<String, Object>builder()
                        .put("serviceId", instance.getServiceId())
                        .put("host", instance.getHost())
                        .put("port", instance.getPort())
                        .put("uri", instance.getUri().toString())
                        .put("metadata", instance.getMetadata())
                        .build())
                .toList();
    }

}
```

验证服务发现和服务调用：

```bash
# 先启动服务提供者
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=18081 --spring.application.name=ateng-nacos-provider"

# 再启动服务消费者
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=18082 --spring.application.name=ateng-nacos-consumer"

# 查看消费者发现到的提供者实例
curl http://127.0.0.1:18082/discovery/instances/ateng-nacos-provider

# 通过消费者按服务名调用提供者
curl http://127.0.0.1:18082/consumer/call/hello
```

预期响应：

```text
Nacos Provider 响应成功，message=hello
```

### 服务调用流程

服务调用流程用于说明一个消费者请求如何通过 Nacos 找到服务提供者。Nacos Spring Cloud 示例中，服务提供者注册到 Nacos，服务消费者从 Nacos 获取服务实例，并通过服务名完成调用。([Nacos 官网](https://nacos.io/docs/latest/ecology/use-nacos-with-spring-cloud/?utm_source=chatgpt.com))

典型调用链路如下：

```text
用户请求
  ↓
ateng-nacos-consumer
  ↓ 通过服务名 ateng-nacos-provider 发起调用
Spring Cloud LoadBalancer
  ↓ 从 DiscoveryClient 获取服务实例
Nacos Discovery Client
  ↓ 拉取或缓存 Nacos Server 中的实例列表
Nacos Server
  ↓ 返回可用实例
ateng-nacos-provider:18081
  ↓
返回业务响应
```

核心流程说明：

| 步骤                  | 说明                                                         |
| --------------------- | ------------------------------------------------------------ |
| 1. 服务提供者启动     | `ateng-nacos-provider` 读取 Nacos 地址、服务名、分组和命名空间配置 |
| 2. 注册实例           | Nacos Client 将当前服务的 IP、端口、权重、元数据注册到 Nacos Server |
| 3. 服务消费者启动     | `ateng-nacos-consumer` 连接同一个 Nacos Server               |
| 4. 发起服务名调用     | 消费者访问 `http://ateng-nacos-provider/provider/echo/hello` |
| 5. 负载均衡选实例     | Spring Cloud LoadBalancer 根据服务名选择一个可用实例         |
| 6. 执行真实 HTTP 请求 | 请求被转换为真实地址，例如 `http://192.168.1.10:18081/provider/echo/hello` |
| 7. 返回调用结果       | 提供者返回响应，消费者将结果返回给调用方                     |

服务调用时需要重点关注以下问题：

| 问题             | 排查方向                                                   |
| ---------------- | ---------------------------------------------------------- |
| 找不到服务实例   | 检查服务名、命名空间、分组、Nacos 地址是否一致             |
| 调用超时         | 检查网络、防火墙、端口、服务健康状态                       |
| 负载不均         | 检查实例权重、实例数量、负载均衡策略                       |
| 本地 IP 注册错误 | 检查网卡、容器网络、`spring.cloud.nacos.discovery.ip` 配置 |
| 服务没有注册     | 检查 `register-enabled`、Nacos 账号密码、启动日志          |

## Nacos 配置中心

Nacos 配置中心用于集中管理应用配置，并支持运行期动态刷新。应用可以将 Nacos 中的配置作为 Spring Environment 的属性源，之后通过 `@Value`、`@ConfigurationProperties` 或 `Environment#getProperty()` 获取配置值；Nacos 官方文档也说明了通过 `spring-cloud-starter-alibaba-nacos-config` 实现配置动态变更的方式。([Nacos 官网](https://nacos.io/docs/latest/ecology/use-nacos-with-spring-cloud/?utm_source=chatgpt.com))

### 配置中心接入

配置中心接入的目标是让应用启动时从 Nacos 加载远程配置，并在配置变更后按需刷新 Bean 中的配置值。Spring Boot 2.4 之后推荐使用 `spring.config.import` 机制导入外部配置；Nacos 官方 Spring Cloud 接入文档也展示了通过 `spring.config.import=nacos:...` 引入 Nacos 配置源的方式。([Nacos 官网](https://nacos.io/docs/latest/ecology/use-nacos-with-spring-cloud/?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

以下配置用于接入 Nacos 配置中心，并加载公共配置和环境配置。

```yaml
server:
  # 当前服务端口
  port: 18080

spring:
  application:
    # 应用名：默认会参与 Nacos 配置 Data ID 命名
    name: ateng-nacos-demo

  profiles:
    # 默认开发环境
    active: dev

  config:
    import:
      # 公共配置：应用启动时加载，optional 表示配置不存在时不阻断启动
      - optional:nacos:${spring.application.name}.yml?group=DEFAULT_GROUP&refreshEnabled=true

      # 环境配置：根据 spring.profiles.active 加载对应环境配置
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml?group=DEFAULT_GROUP&refreshEnabled=true

  cloud:
    nacos:
      # Nacos Server 客户端访问地址
      server-addr: 127.0.0.1:8848

      # Nacos 用户名
      username: nacos

      # Nacos 密码
      password: nacos

      config:
        # 配置中心地址，默认可复用 spring.cloud.nacos.server-addr
        server-addr: ${spring.cloud.nacos.server-addr}

        # 命名空间 ID，空值表示 public 命名空间
        namespace:

        # 默认配置分组
        group: DEFAULT_GROUP

        # 默认配置格式
        file-extension: yml

        # 开启配置中心
        enabled: true

logging:
  level:
    # 查看配置加载和刷新日志
    com.alibaba.cloud.nacos: info
    com.alibaba.nacos: info
    io.github.atengk: debug
```

在 Nacos 控制台中创建公共配置：

```text
Data ID: ateng-nacos-demo.yml
Group: DEFAULT_GROUP
Format: YAML
```

配置内容：

```yaml
ateng:
  nacos:
    # 公共配置，所有环境共享
    title: "Spring Cloud Alibaba Nacos 示例项目"
    owner: "Ateng"
```

在 Nacos 控制台中创建开发环境配置：

```text
Data ID: ateng-nacos-demo-dev.yml
Group: DEFAULT_GROUP
Format: YAML
```

配置内容：

```yaml
ateng:
  nacos:
    # 是否开启演示功能
    enable-demo: true

    # 动态刷新测试内容
    message: "这是来自 Nacos dev 环境的配置"
```

### 配置文件命名规则

配置文件命名规则用于约定 Spring Boot 应用与 Nacos 配置之间的映射关系。Nacos Spring Cloud 文档说明，Data ID 通常可以由 `${prefix}-${spring.profiles.active}.${file-extension}` 组成，其中 `prefix` 默认取 `spring.application.name`。([Nacos 官网](https://nacos.io/en/docs/quick-start-spring-cloud?utm_source=chatgpt.com))

常见命名规则如下：

| 场景         | Data ID 示例                | 说明                                 |
| ------------ | --------------------------- | ------------------------------------ |
| 公共配置     | `ateng-nacos-demo.yml`      | 不区分环境，所有环境共享             |
| 开发环境     | `ateng-nacos-demo-dev.yml`  | `spring.profiles.active=dev` 时加载  |
| 测试环境     | `ateng-nacos-demo-test.yml` | `spring.profiles.active=test` 时加载 |
| 生产环境     | `ateng-nacos-demo-prod.yml` | `spring.profiles.active=prod` 时加载 |
| 独立业务配置 | `ateng-nacos-demo-biz.yml`  | 可通过额外 import 显式加载           |

推荐命名规范：

```text
${spring.application.name}.yml
${spring.application.name}-${spring.profiles.active}.yml
${spring.application.name}-${module}.yml
${spring.application.name}-${module}-${spring.profiles.active}.yml
```

例如：

```text
ateng-nacos-demo.yml
ateng-nacos-demo-dev.yml
ateng-nacos-demo-datasource.yml
ateng-nacos-demo-datasource-dev.yml
```

如果要引入多个独立配置文件，可以在 `spring.config.import` 中追加：

```yaml
spring:
  config:
    import:
      # 主配置
      - optional:nacos:${spring.application.name}.yml?group=DEFAULT_GROUP&refreshEnabled=true

      # 环境配置
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml?group=DEFAULT_GROUP&refreshEnabled=true

      # 业务配置
      - optional:nacos:${spring.application.name}-biz.yml?group=DEFAULT_GROUP&refreshEnabled=true

      # 数据源配置
      - optional:nacos:${spring.application.name}-datasource.yml?group=DEFAULT_GROUP&refreshEnabled=true
```

命名时需要避免以下问题：

| 问题               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| Data ID 后缀不一致 | `file-extension: yml` 时，不要在 Nacos 中创建成 `.yaml` 或 `.properties` |
| 环境名不一致       | `spring.profiles.active=dev` 时，对应环境配置应使用 `-dev`   |
| 分组不一致         | `group=DEV_GROUP` 时，Nacos 配置也必须创建在 `DEV_GROUP`     |
| 命名空间不一致     | 本地配置的 namespace 必须与 Nacos 控制台中的命名空间 ID 一致 |
| 配置格式错误       | Nacos 中 Format 选择 YAML 时，内容必须符合 YAML 缩进规则     |

### 命名空间与分组配置

命名空间和分组用于做配置隔离。一般情况下，Namespace 适合隔离环境或租户，Group 适合隔离业务域、应用组或灰度分组。注册中心和配置中心都支持 namespace 和 group，但二者需要按场景独立规划。

推荐规划如下：

| 隔离维度 | 推荐方式                           | 示例                        |
| -------- | ---------------------------------- | --------------------------- |
| 环境隔离 | Namespace                          | `dev`、`test`、`prod`       |
| 业务隔离 | Group                              | `ORDER_GROUP`、`USER_GROUP` |
| 默认开发 | public Namespace + `DEFAULT_GROUP` | 本地快速验证                |
| 生产环境 | 独立 Namespace + 业务 Group        | `prod` + `ORDER_GROUP`      |

文件位置：`src/main/resources/application-dev.yml`

以下配置用于开发环境命名空间和分组隔离。

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # 开发环境命名空间 ID，按 Nacos 控制台实际 ID 填写
        namespace: dev-namespace-id

        # 开发环境服务分组
        group: DEV_GROUP

      config:
        # 开发环境命名空间 ID，必须与控制台配置所在命名空间一致
        namespace: dev-namespace-id

        # 开发环境配置分组
        group: DEV_GROUP
```

文件位置：`src/main/resources/application-prod.yml`

以下配置用于生产环境命名空间和分组隔离。

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # 生产环境命名空间 ID，建议独立创建
        namespace: ${NACOS_NAMESPACE}

        # 生产服务分组
        group: PROD_GROUP

      config:
        # 生产环境配置命名空间 ID
        namespace: ${NACOS_NAMESPACE}

        # 生产配置分组
        group: PROD_GROUP
```

命名空间和分组使用建议：

| 建议                               | 说明                            |
| ---------------------------------- | ------------------------------- |
| dev、test、prod 使用不同 Namespace | 防止测试服务误调用生产服务      |
| 同一环境内用 Group 区分业务域      | 例如用户域、订单域、支付域      |
| 注册中心和配置中心保持规划一致     | 降低排查复杂度                  |
| 生产 Namespace ID 使用环境变量注入 | 避免生产 ID、账号、密码写入仓库 |
| 不要滥用 Group                     | Group 太多会增加配置维护成本    |

### 动态配置刷新

动态配置刷新用于在不重启应用的情况下读取 Nacos 中变更后的配置。对于普通配置读取，可以使用 `@ConfigurationProperties` 绑定配置对象；对于需要动态刷新的 Bean，可以结合 `@RefreshScope` 使用。Nacos 官方文档说明，Nacos 中的配置可作为 Spring 环境上下文属性源，并通过 `@Value`、`@ConfigurationProperties` 或 `Environment#getProperty()` 获取。([Nacos 官网](https://nacos.io/docs/latest/ecology/use-nacos-with-spring-cloud/?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/nacos/config/DemoNacosProperties.java`

以下配置类用于绑定 `ateng.nacos` 开头的配置项，并支持配置刷新。

```java
package io.github.atengk.nacos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Nacos 示例配置属性
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "ateng.nacos")
public class DemoNacosProperties {

    /**
     * 项目标题
     */
    private String title;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 是否开启演示功能
     */
    private Boolean enableDemo;

    /**
     * 动态消息内容
     */
    private String message;

}
```

文件位置：`src/main/java/io/github/atengk/nacos/controller/ConfigRefreshController.java`

以下接口用于读取 Nacos 配置，并验证配置变更后是否能够动态刷新。

```java
package io.github.atengk.nacos.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.nacos.config.DemoNacosProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Nacos 配置刷新测试接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ConfigRefreshController {

    private final DemoNacosProperties demoNacosProperties;
    private final Environment environment;

    /**
     * 通过配置属性类读取 Nacos 配置
     *
     * @return Nacos 配置信息
     */
    @GetMapping("/config/properties")
    public Map<String, Object> properties() {
        log.info("读取 Nacos 配置属性，title={}，enableDemo={}",
                demoNacosProperties.getTitle(), demoNacosProperties.getEnableDemo());

        return MapUtil.<String, Object>builder()
                .put("title", demoNacosProperties.getTitle())
                .put("owner", demoNacosProperties.getOwner())
                .put("enableDemo", demoNacosProperties.getEnableDemo())
                .put("message", demoNacosProperties.getMessage())
                .build();
    }

    /**
     * 通过 Environment 读取 Nacos 配置
     *
     * @return Nacos 环境配置
     */
    @GetMapping("/config/environment")
    public Map<String, Object> environment() {
        String title = environment.getProperty("ateng.nacos.title");
        String message = environment.getProperty("ateng.nacos.message");

        log.info("通过 Environment 读取 Nacos 配置，title={}，message={}", title, message);

        return MapUtil.<String, Object>builder()
                .put("title", title)
                .put("message", message)
                .build();
    }

}
```

启动后验证配置读取：

```bash
# 查看绑定到配置属性类中的内容
curl http://127.0.0.1:18080/config/properties

# 查看 Environment 中的配置内容
curl http://127.0.0.1:18080/config/environment
```

修改 Nacos 中的配置：

```yaml
ateng:
  nacos:
    title: "Spring Cloud Alibaba Nacos 示例项目"
    owner: "Ateng"
    enable-demo: true
    message: "Nacos 配置已经动态刷新"
```

保存发布后再次调用：

```bash
curl http://127.0.0.1:18080/config/properties
```

如果返回的 `message` 已变为 `Nacos 配置已经动态刷新`，说明动态刷新生效。

动态刷新注意事项：

| 注意事项                   | 说明                                                   |
| -------------------------- | ------------------------------------------------------ |
| `refreshEnabled=true`      | 使用 `spring.config.import` 时建议显式开启刷新         |
| `@RefreshScope`            | 对需要运行期刷新的 Bean 添加该注解                     |
| `@ConfigurationProperties` | 推荐用于结构化配置，优于大量散落的 `@Value`            |
| 配置格式                   | Nacos 中 YAML 缩进错误会导致配置加载失败               |
| 生产环境                   | 动态配置发布前应先在 test 环境验证，避免误改核心参数   |
| 敏感配置                   | 密码、密钥等敏感内容建议结合权限控制和外部密钥系统管理 |

## 多环境配置

多环境配置用于隔离本地开发、测试环境和生产环境的 Nacos 注册中心、配置中心、服务分组、命名空间、日志级别和敏感参数。当前文档默认使用 `dev`、`test`、`prod` 三套环境，通过 `spring.profiles.active` 切换。Spring Boot 的外部化配置支持 profile 机制，Spring Cloud Alibaba Nacos 配置中心可以通过 `spring.config.import` 接入远程配置。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

推荐目录结构如下：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

文件位置：`src/main/resources/application.yml`

该文件作为公共配置入口，主要放应用名、默认环境、Nacos 配置导入规则、Actuator 和通用日志配置。

```yaml
server:
  # 默认服务端口，可被不同环境覆盖
  port: 18080

spring:
  application:
    # 服务名：注册到 Nacos 的服务名称，也是配置文件 Data ID 的基础名称
    name: ateng-nacos-demo

  profiles:
    # 默认启用 dev 环境，可通过启动参数覆盖
    active: dev

  config:
    import:
      # 公共配置：所有环境共享
      - optional:nacos:${spring.application.name}.yml?group=${nacos.config.group:DEFAULT_GROUP}&refreshEnabled=true

      # 环境配置：根据 spring.profiles.active 加载 dev/test/prod 配置
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml?group=${nacos.config.group:DEFAULT_GROUP}&refreshEnabled=true

  cloud:
    nacos:
      # Nacos Server 地址，默认本地开发环境
      server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}

      # Nacos 用户名
      username: ${NACOS_USERNAME:nacos}

      # Nacos 密码
      password: ${NACOS_PASSWORD:nacos}

      discovery:
        # 注册中心地址
        server-addr: ${spring.cloud.nacos.server-addr}

        # 是否启用服务注册发现
        enabled: true

        # 是否注册当前实例
        register-enabled: true

        # 默认注册为临时实例
        ephemeral: true

      config:
        # 配置中心地址
        server-addr: ${spring.cloud.nacos.server-addr}

        # 配置文件格式
        file-extension: yml

        # 是否启用配置中心
        enabled: true

management:
  endpoints:
    web:
      exposure:
        # 暴露基础运维端点，生产环境可收敛
        include: health,info,env

  endpoint:
    health:
      # 本地和测试环境便于查看健康详情
      show-details: always

logging:
  level:
    # 默认日志级别
    root: info
    io.github.atengk: info
    com.alibaba.cloud.nacos: info
    com.alibaba.nacos: info
```

这里使用 `${nacos.config.group:DEFAULT_GROUP}` 是为了让不同环境可以在 profile 文件中覆盖 Nacos 配置分组。生产环境的地址、账号、密码建议通过环境变量注入，而不是写死在 Git 仓库中。

### dev 环境配置

`dev` 环境用于本地开发和功能验证，通常连接本机 Docker 启动的 Nacos Server。该环境可以开启更详细的业务日志，便于排查服务注册、配置加载和接口调用问题。

文件位置：`src/main/resources/application-dev.yml`

```yaml
server:
  # 本地开发端口
  port: 18080

# 当前环境 Nacos 配置分组
nacos:
  config:
    group: DEV_GROUP

spring:
  cloud:
    nacos:
      # 本地 Nacos Server 地址
      server-addr: 127.0.0.1:8848

      discovery:
        # dev 命名空间 ID，空值表示 public 命名空间
        namespace:

        # dev 服务注册分组
        group: DEV_GROUP

        metadata:
          # 当前实例环境标识
          env: dev
          version: v1
          owner: ateng

      config:
        # dev 配置命名空间 ID，空值表示 public 命名空间
        namespace:

        # dev 配置分组
        group: DEV_GROUP

logging:
  level:
    # 本地开发环境建议开启 debug
    io.github.atengk: debug
    org.springframework.cloud.openfeign: debug
    org.springframework.cloud.loadbalancer: debug
```

Nacos 控制台中创建 dev 配置：

```text
Data ID: ateng-nacos-demo.yml
Group: DEV_GROUP
Format: YAML

Data ID: ateng-nacos-demo-dev.yml
Group: DEV_GROUP
Format: YAML
```

`ateng-nacos-demo-dev.yml` 示例内容：

```yaml
ateng:
  nacos:
    # 当前运行环境
    env: dev

    # 示例业务开关
    enable-demo: true

    # 示例动态配置内容
    message: "这是 dev 环境 Nacos 配置"
```

启动 dev 环境：

```bash
# 默认 application.yml 已配置 spring.profiles.active=dev，可直接启动
mvn spring-boot:run

# 显式指定 dev 环境启动
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### test 环境配置

`test` 环境用于联调、测试和预发布前验证。该环境应连接测试 Nacos Server，使用独立命名空间或分组，避免测试服务误注册到开发或生产环境。

文件位置：`src/main/resources/application-test.yml`

```yaml
server:
  # 测试环境端口，可按部署平台要求覆盖
  port: 18080

# 当前环境 Nacos 配置分组
nacos:
  config:
    group: TEST_GROUP

spring:
  cloud:
    nacos:
      # 测试环境 Nacos 地址，按实际环境替换
      server-addr: ${NACOS_SERVER_ADDR:192.168.10.100:8848}

      # 测试环境账号
      username: ${NACOS_USERNAME:nacos}

      # 测试环境密码
      password: ${NACOS_PASSWORD:nacos}

      discovery:
        # 测试环境命名空间 ID，建议在 Nacos 控制台独立创建
        namespace: ${NACOS_NAMESPACE:test-namespace-id}

        # 测试环境服务分组
        group: TEST_GROUP

        metadata:
          env: test
          version: v1
          owner: ateng

      config:
        # 测试环境配置命名空间 ID
        namespace: ${NACOS_NAMESPACE:test-namespace-id}

        # 测试环境配置分组
        group: TEST_GROUP

logging:
  level:
    # 测试环境保留业务调试能力，但不建议过度输出第三方组件 debug
    io.github.atengk: info
    org.springframework.cloud.openfeign: info
    org.springframework.cloud.loadbalancer: info
```

Nacos 控制台中创建 test 配置：

```text
Data ID: ateng-nacos-demo.yml
Group: TEST_GROUP
Format: YAML

Data ID: ateng-nacos-demo-test.yml
Group: TEST_GROUP
Format: YAML
```

`ateng-nacos-demo-test.yml` 示例内容：

```yaml
ateng:
  nacos:
    env: test
    enable-demo: true
    message: "这是 test 环境 Nacos 配置"
```

启动 test 环境：

```bash
java -jar target/ateng-nacos-demo-1.0.0.jar \
  --spring.profiles.active=test \
  --NACOS_SERVER_ADDR=192.168.10.100:8848 \
  --NACOS_USERNAME=nacos \
  --NACOS_PASSWORD=nacos \
  --NACOS_NAMESPACE=test-namespace-id
```

### prod 环境配置

`prod` 环境用于正式运行环境。生产配置应避免硬编码账号、密码、命名空间 ID、Nacos 地址等敏感参数，应通过环境变量、Kubernetes Secret、部署平台变量或配置管理系统注入。生产环境也应收敛日志级别和 Actuator 暴露端点。

文件位置：`src/main/resources/application-prod.yml`

```yaml
server:
  # 生产环境端口，通常由部署平台统一指定
  port: ${SERVER_PORT:18080}

# 当前环境 Nacos 配置分组
nacos:
  config:
    group: PROD_GROUP

spring:
  cloud:
    nacos:
      # 生产环境 Nacos 地址，建议使用内网域名、VIP 或 SLB 地址
      server-addr: ${NACOS_SERVER_ADDR}

      # 生产环境账号，必须通过环境变量注入
      username: ${NACOS_USERNAME}

      # 生产环境密码，必须通过环境变量注入
      password: ${NACOS_PASSWORD}

      discovery:
        # 生产环境命名空间 ID
        namespace: ${NACOS_NAMESPACE}

        # 生产环境服务分组
        group: PROD_GROUP

        # 生产环境开启服务注册
        enabled: true

        # 生产环境开启实例注册
        register-enabled: true

        metadata:
          env: prod
          version: ${APP_VERSION:v1}
          owner: ateng

      config:
        # 生产环境配置命名空间 ID
        namespace: ${NACOS_NAMESPACE}

        # 生产环境配置分组
        group: PROD_GROUP

        # 生产环境开启配置中心
        enabled: true

management:
  endpoints:
    web:
      exposure:
        # 生产环境建议只暴露必要端点
        include: health,info

  endpoint:
    health:
      # 生产环境不建议直接展示完整健康详情
      show-details: never

logging:
  level:
    # 生产环境日志级别保持克制
    root: info
    io.github.atengk: info
    com.alibaba.cloud.nacos: warn
    com.alibaba.nacos: warn
    org.springframework.cloud.openfeign: warn
    org.springframework.cloud.loadbalancer: warn
```

Nacos 控制台中创建 prod 配置：

```text
Data ID: ateng-nacos-demo.yml
Group: PROD_GROUP
Format: YAML

Data ID: ateng-nacos-demo-prod.yml
Group: PROD_GROUP
Format: YAML
```

`ateng-nacos-demo-prod.yml` 示例内容：

```yaml
ateng:
  nacos:
    env: prod
    enable-demo: false
    message: "这是 prod 环境 Nacos 配置"
```

生产环境启动示例：

```bash
export SERVER_PORT=18080
export NACOS_SERVER_ADDR=prod-nacos.example.internal:8848
export NACOS_USERNAME=nacos_prod_user
export NACOS_PASSWORD='replace-with-real-password'
export NACOS_NAMESPACE=prod-namespace-id
export APP_VERSION=v1.0.0

java -jar target/ateng-nacos-demo-1.0.0.jar \
  --spring.profiles.active=prod
```

生产环境需要重点检查三项：服务是否注册到 `PROD_GROUP`，配置是否从 `PROD_GROUP` 加载，命名空间是否为生产命名空间 ID。如果服务和配置不在同一套命名空间规划下，容易出现服务能启动但读取不到配置、服务能注册但消费者发现不到实例的问题。

## 服务调用集成

服务调用集成用于让微服务之间通过服务名完成 HTTP 调用。前文已经介绍过 `RestTemplate` + `@LoadBalanced` 的方式，本章节重点使用 OpenFeign。Spring Cloud OpenFeign 是声明式 REST Client，使用接口和注解定义远程调用；Spring Cloud 会集成 Spring MVC 注解、编码器、解码器，并在 Spring Cloud LoadBalancer 存在时使用负载均衡客户端。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

### OpenFeign 接入

OpenFeign 接入需要在消费者服务中添加 `spring-cloud-starter-openfeign` 依赖，并在启动类上启用 `@EnableFeignClients`。Spring Cloud OpenFeign 官方文档说明，引入 Feign 使用 `org.springframework.cloud:spring-cloud-starter-openfeign`，并通过 `@FeignClient` 声明远程服务接口。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

以下依赖用于让消费者服务支持 OpenFeign 声明式调用和客户端负载均衡。

```xml
<dependencies>
    <!-- Web 基础依赖：提供 REST 接口能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Nacos 注册中心：服务注册和服务发现 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- OpenFeign：声明式 HTTP 客户端 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>

    <!-- LoadBalancer：通过服务名选择 Nacos 中的可用服务实例 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>

    <!-- Actuator：健康检查和基础运维端点 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Hutool：常用工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.39</version>
    </dependency>

    <!-- Lombok：减少样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

文件位置：`src/main/java/io/github/atengk/nacos/AtengNacosConsumerApplication.java`

以下启动类开启服务发现和 Feign Client 扫描。

```java
package io.github.atengk.nacos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Nacos 服务消费者启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "io.github.atengk.nacos.client")
@SpringBootApplication
public class AtengNacosConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtengNacosConsumerApplication.class, args);
    }

}
```

文件位置：`src/main/java/io/github/atengk/nacos/client/ProviderFeignClient.java`

以下 Feign 接口通过服务名 `ateng-nacos-provider` 调用服务提供者。

```java
package io.github.atengk.nacos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 服务提供者 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(
        name = "ateng-nacos-provider",
        contextId = "providerFeignClient"
)
public interface ProviderFeignClient {

    /**
     * 调用服务提供者 echo 接口
     *
     * @param message 请求消息
     * @return 响应内容
     */
    @GetMapping("/provider/echo")
    Map<String, Object> echo(@RequestParam("message") String message);

}
```

文件位置：`src/main/resources/application.yml`

以下配置用于启用 OpenFeign 日志、超时时间和请求压缩。OpenFeign 支持通过 `spring.cloud.openfeign.client.config` 对指定 Feign Client 设置连接超时、读取超时、日志级别等属性。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

```yaml
spring:
  application:
    name: ateng-nacos-consumer

  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      username: nacos
      password: nacos

      discovery:
        namespace:
        group: DEFAULT_GROUP

    openfeign:
      client:
        config:
          # default 表示所有 Feign Client 默认配置
          default:
            # 连接超时时间，单位毫秒
            connectTimeout: 3000

            # 读取超时时间，单位毫秒
            readTimeout: 5000

            # Feign 日志级别：none/basic/headers/full
            loggerLevel: basic

          # 针对 ateng-nacos-provider 单独配置
          ateng-nacos-provider:
            connectTimeout: 3000
            readTimeout: 5000
            loggerLevel: full

      compression:
        request:
          # 开启 Feign 请求压缩
          enabled: true

          # 支持压缩的请求类型
          mime-types: text/xml,application/xml,application/json

          # 超过该大小才压缩，单位字节
          min-request-size: 2048

        response:
          # 开启 Feign 响应压缩
          enabled: true

logging:
  level:
    # Feign 接口日志级别需要配置到接口所在包或具体接口
    io.github.atengk.nacos.client: debug
```

### 负载均衡配置

负载均衡配置用于控制消费者如何从多个服务提供者实例中选择目标实例。Spring Cloud LoadBalancer 是 Spring Cloud 提供的客户端负载均衡实现，可以通过 DiscoveryClient 从注册中心获取服务实例，并支持轮询、随机、缓存、预加载等能力。([Home](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html?utm_source=chatgpt.com))

如果使用 OpenFeign，且 classpath 中存在 Spring Cloud LoadBalancer，OpenFeign 会使用 `FeignBlockingLoadBalancerClient` 进行负载均衡调用。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com)) 因此，在 Nacos + OpenFeign 场景中，调用 `@FeignClient(name = "ateng-nacos-provider")` 时，`name` 会作为服务名去注册中心查找实例。

文件位置：`src/main/resources/application.yml`

以下配置用于开启指定服务的 LoadBalancer 预加载，减少第一次调用时创建子上下文带来的延迟。

```yaml
spring:
  cloud:
    loadbalancer:
      # 是否启用 Spring Cloud LoadBalancer
      enabled: true

      eager-load:
        # 预加载指定服务的负载均衡上下文
        clients:
          - ateng-nacos-provider
```

如果需要使用随机负载均衡策略，可以增加自定义配置类。默认情况下，Spring Cloud LoadBalancer 提供轮询和随机等实现。([Home](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/nacos/config/RandomLoadBalancerConfig.java`

以下配置为指定服务启用随机负载均衡策略。

```java
package io.github.atengk.nacos.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 随机负载均衡配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class RandomLoadBalancerConfig {

    /**
     * 创建随机负载均衡器
     *
     * @param environment 环境配置
     * @param loadBalancerClientFactory 负载均衡客户端工厂
     * @return 随机负载均衡器
     */
    @Bean
    public RandomLoadBalancer randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory
    ) {
        String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class),
                serviceId
        );
    }

}
```

文件位置：`src/main/java/io/github/atengk/nacos/client/ProviderFeignClient.java`

以下配置将随机负载均衡策略应用到 `ateng-nacos-provider` 这个 Feign Client。

```java
package io.github.atengk.nacos.client;

import io.github.atengk.nacos.config.RandomLoadBalancerConfig;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 服务提供者 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@LoadBalancerClient(
        name = "ateng-nacos-provider",
        configuration = RandomLoadBalancerConfig.class
)
@FeignClient(
        name = "ateng-nacos-provider",
        contextId = "providerFeignClient"
)
public interface ProviderFeignClient {

    /**
     * 调用服务提供者 echo 接口
     *
     * @param message 请求消息
     * @return 响应内容
     */
    @GetMapping("/provider/echo")
    Map<String, Object> echo(@RequestParam("message") String message);

}
```

使用自定义负载均衡配置时，建议注意两点。第一，`RandomLoadBalancerConfig` 不要放到主启动类默认扫描路径下的通用配置包中，否则可能被全局扫描并影响其他服务。第二，如果只是普通业务服务调用，默认轮询策略通常已经足够，不需要过早自定义策略。

### 服务间调用示例

服务间调用示例使用两个应用：`ateng-nacos-provider` 作为服务提供者，`ateng-nacos-consumer` 作为服务消费者。提供者注册到 Nacos，消费者通过 OpenFeign 使用服务名调用提供者。

Provider 项目关键结构：

```text
ateng-nacos-provider
├── pom.xml
└── src/main
    ├── java/io/github/atengk/nacos
    │   ├── AtengNacosProviderApplication.java
    │   └── controller/ProviderEchoController.java
    └── resources/application.yml
```

Consumer 项目关键结构：

```text
ateng-nacos-consumer
├── pom.xml
└── src/main
    ├── java/io/github/atengk/nacos
    │   ├── AtengNacosConsumerApplication.java
    │   ├── client/ProviderFeignClient.java
    │   └── controller/ConsumerFeignController.java
    └── resources/application.yml
```

文件位置：`ateng-nacos-provider/src/main/resources/application.yml`

以下配置用于启动服务提供者并注册到 Nacos。

```yaml
server:
  port: 18081

spring:
  application:
    name: ateng-nacos-provider

  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      username: nacos
      password: nacos

      discovery:
        namespace:
        group: DEFAULT_GROUP
        cluster-name: DEFAULT
        metadata:
          env: dev
          version: v1

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

文件位置：`ateng-nacos-provider/src/main/java/io/github/atengk/nacos/controller/ProviderEchoController.java`

以下接口作为服务提供者的远程调用目标。

```java
package io.github.atengk.nacos.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 服务提供者接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class ProviderEchoController {

    @Value("${server.port}")
    private Integer serverPort;

    /**
     * 返回服务提供者响应
     *
     * @param message 请求消息
     * @return 响应结果
     */
    @GetMapping("/provider/echo")
    public Map<String, Object> echo(@RequestParam("message") String message) {
        String safeMessage = StrUtil.blankToDefault(message, "default");
        log.info("服务提供者收到请求，message={}，port={}", safeMessage, serverPort);

        return MapUtil.<String, Object>builder()
                .put("success", true)
                .put("service", "ateng-nacos-provider")
                .put("port", serverPort)
                .put("message", StrUtil.format("Provider 响应成功：{}", safeMessage))
                .build();
    }

}
```

文件位置：`ateng-nacos-consumer/src/main/resources/application.yml`

以下配置用于启动服务消费者，并通过 Nacos 发现服务提供者。

```yaml
server:
  port: 18082

spring:
  application:
    name: ateng-nacos-consumer

  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      username: nacos
      password: nacos

      discovery:
        namespace:
        group: DEFAULT_GROUP

    openfeign:
      client:
        config:
          default:
            connectTimeout: 3000
            readTimeout: 5000
            loggerLevel: basic

          ateng-nacos-provider:
            connectTimeout: 3000
            readTimeout: 5000
            loggerLevel: full

    loadbalancer:
      eager-load:
        clients:
          - ateng-nacos-provider

logging:
  level:
    io.github.atengk.nacos.client: debug
```

文件位置：`ateng-nacos-consumer/src/main/java/io/github/atengk/nacos/controller/ConsumerFeignController.java`

以下接口用于从消费者侧发起服务间调用。

```java
package io.github.atengk.nacos.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.nacos.client.ProviderFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * OpenFeign 服务调用接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ConsumerFeignController {

    private final ProviderFeignClient providerFeignClient;

    /**
     * 通过 OpenFeign 调用服务提供者
     *
     * @param message 请求消息
     * @return 调用结果
     */
    @GetMapping("/consumer/feign/echo")
    public Map<String, Object> echo(@RequestParam(value = "message", required = false) String message) {
        String safeMessage = StrUtil.blankToDefault(message, "default");
        log.info("消费者准备通过 OpenFeign 调用服务提供者，message={}", safeMessage);

        Map<String, Object> providerResult = providerFeignClient.echo(safeMessage);

        return MapUtil.<String, Object>builder()
                .put("success", true)
                .put("consumer", "ateng-nacos-consumer")
                .put("providerResult", providerResult)
                .build();
    }

}
```

启动两个 Provider 实例，用于验证负载均衡：

```bash
# 启动第一个 Provider 实例
java -jar ateng-nacos-provider/target/ateng-nacos-provider-1.0.0.jar \
  --server.port=18081 \
  --spring.application.name=ateng-nacos-provider

# 启动第二个 Provider 实例
java -jar ateng-nacos-provider/target/ateng-nacos-provider-1.0.0.jar \
  --server.port=18083 \
  --spring.application.name=ateng-nacos-provider
```

启动 Consumer：

```bash
java -jar ateng-nacos-consumer/target/ateng-nacos-consumer-1.0.0.jar \
  --server.port=18082 \
  --spring.application.name=ateng-nacos-consumer
```

调用消费者接口：

```bash
curl "http://127.0.0.1:18082/consumer/feign/echo?message=hello"
```

可能返回：

```json
{
  "success": true,
  "consumer": "ateng-nacos-consumer",
  "providerResult": {
    "success": true,
    "service": "ateng-nacos-provider",
    "port": 18081,
    "message": "Provider 响应成功：hello"
  }
}
```

连续调用多次，如果返回中的 `port` 在 `18081` 和 `18083` 之间变化，说明消费者已经通过 Nacos 服务发现和 Spring Cloud LoadBalancer 完成了服务间负载均衡调用。

排查要点如下：

| 问题                                        | 处理方式                                               |
| ------------------------------------------- | ------------------------------------------------------ |
| `No Feign Client for loadBalancing defined` | 检查是否引入 `spring-cloud-starter-loadbalancer`       |
| 找不到 `ateng-nacos-provider`               | 检查 Provider 是否注册到 Nacos，服务名是否一致         |
| 调用超时                                    | 检查 Provider 端口、防火墙、Nacos 实例健康状态         |
| Feign 日志不输出                            | 检查 `loggerLevel` 和接口包日志级别是否为 `debug`      |
| 多环境调用失败                              | 检查 Consumer 和 Provider 的 namespace、group 是否一致 |
| 生产误调用测试服务                          | 使用独立 Namespace 隔离 dev/test/prod                  |



## 配置安全与隔离

配置安全与隔离主要解决三个问题：不同环境之间不能互相读取配置、不同业务域之间不能互相污染服务列表、Nacos 控制台和客户端访问需要鉴权。Nacos 官方文档明确建议 Nacos 作为内部微服务组件运行在可信内网中，不建议暴露到公网；快速开始中的单机模式也主要用于测试，生产环境应使用集群模式并开启鉴权。([Nacos 官网](https://nacos.io/en/docs/next/quickstart/quick-start/?utm_source=chatgpt.com))

### Namespace 隔离

Namespace 适合做环境级或租户级隔离。常见做法是为 `dev`、`test`、`prod` 分别创建独立 Namespace，避免开发服务发现测试服务、测试服务误调用生产服务、生产应用读取测试配置。

推荐隔离方式如下：

| 环境     | Namespace 名称 | Namespace ID 示例   | 说明                   |
| -------- | -------------- | ------------------- | ---------------------- |
| 开发环境 | `dev`          | `dev-namespace-id`  | 本地开发、功能自测     |
| 测试环境 | `test`         | `test-namespace-id` | 联调、测试、预发布验证 |
| 生产环境 | `prod`         | `prod-namespace-id` | 正式生产运行           |

在 Nacos 控制台创建 Namespace 后，需要复制对应的 Namespace ID，并配置到应用中。注意，应用配置中使用的是 Namespace ID，不是控制台显示名称。

文件位置：`src/main/resources/application-dev.yml`

以下配置用于让 dev 环境的注册中心和配置中心都使用 dev Namespace。

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # dev 命名空间 ID，来自 Nacos 控制台“命名空间”页面
        namespace: dev-namespace-id

        # dev 服务分组
        group: DEV_GROUP

      config:
        # dev 配置命名空间 ID，需要与配置所在命名空间一致
        namespace: dev-namespace-id

        # dev 配置分组
        group: DEV_GROUP
```

文件位置：`src/main/resources/application-test.yml`

以下配置用于让 test 环境接入独立测试 Namespace。

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # test 命名空间 ID
        namespace: ${NACOS_NAMESPACE:test-namespace-id}

        # test 服务分组
        group: TEST_GROUP

      config:
        # test 配置命名空间 ID
        namespace: ${NACOS_NAMESPACE:test-namespace-id}

        # test 配置分组
        group: TEST_GROUP
```

文件位置：`src/main/resources/application-prod.yml`

以下配置用于让 prod 环境通过环境变量注入生产 Namespace。

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # 生产环境命名空间 ID，不建议写死在代码仓库
        namespace: ${NACOS_NAMESPACE}

        # 生产服务分组
        group: PROD_GROUP

      config:
        # 生产环境配置命名空间 ID
        namespace: ${NACOS_NAMESPACE}

        # 生产配置分组
        group: PROD_GROUP
```

Namespace 使用建议：

| 建议                                     | 说明                                   |
| ---------------------------------------- | -------------------------------------- |
| `dev/test/prod` 使用不同 Namespace       | 防止环境之间服务发现和配置读取串线     |
| 生产 Namespace ID 使用环境变量注入       | 避免敏感环境标识进入 Git 仓库          |
| 注册中心和配置中心保持同一环境 Namespace | 降低排查成本                           |
| 本地快速验证可以先使用 public            | 但正式联调和生产不建议依赖 public      |
| 不要频繁删除 Namespace                   | 删除后关联配置和服务隔离关系会受到影响 |

### Group 隔离

Group 适合在同一个 Namespace 内继续做业务域、应用组、配置组隔离。Namespace 更偏环境边界，Group 更偏业务边界。实际项目中，推荐使用 Namespace 隔离环境，再使用 Group 区分业务域。

推荐分组方式如下：

| 使用场景 | Group 示例                              | 说明                     |
| -------- | --------------------------------------- | ------------------------ |
| 默认开发 | `DEFAULT_GROUP`                         | 适合学习、演示、简单项目 |
| 用户业务 | `USER_GROUP`                            | 用户中心服务和配置       |
| 订单业务 | `ORDER_GROUP`                           | 订单服务和订单配置       |
| 支付业务 | `PAY_GROUP`                             | 支付服务和支付配置       |
| 环境分组 | `DEV_GROUP`、`TEST_GROUP`、`PROD_GROUP` | 小项目可直接按环境分组   |

如果项目规模较小，可以采用环境 Group：

```text
dev  Namespace  + DEV_GROUP
test Namespace  + TEST_GROUP
prod Namespace  + PROD_GROUP
```

如果项目规模较大，可以采用环境 Namespace + 业务 Group：

```text
dev  Namespace  + USER_GROUP / ORDER_GROUP / PAY_GROUP
test Namespace  + USER_GROUP / ORDER_GROUP / PAY_GROUP
prod Namespace  + USER_GROUP / ORDER_GROUP / PAY_GROUP
```

文件位置：`src/main/resources/application.yml`

以下配置通过变量统一控制 Nacos 配置分组，便于不同环境覆盖。

```yaml
spring:
  config:
    import:
      # 公共配置，group 从 nacos.config.group 读取
      - optional:nacos:${spring.application.name}.yml?group=${nacos.config.group:DEFAULT_GROUP}&refreshEnabled=true

      # 环境配置，group 从 nacos.config.group 读取
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml?group=${nacos.config.group:DEFAULT_GROUP}&refreshEnabled=true

  cloud:
    nacos:
      discovery:
        # 服务注册分组
        group: ${nacos.discovery.group:DEFAULT_GROUP}

      config:
        # 配置中心默认分组
        group: ${nacos.config.group:DEFAULT_GROUP}

nacos:
  discovery:
    # 默认服务注册分组
    group: DEFAULT_GROUP

  config:
    # 默认配置分组
    group: DEFAULT_GROUP
```

文件位置：`src/main/resources/application-prod.yml`

生产环境可以覆盖为生产分组。

```yaml
nacos:
  discovery:
    # 生产服务注册分组
    group: PROD_GROUP

  config:
    # 生产配置分组
    group: PROD_GROUP

spring:
  cloud:
    nacos:
      discovery:
        group: ${nacos.discovery.group}

      config:
        group: ${nacos.config.group}
```

Group 使用注意事项：

| 问题                                                         | 说明                         |
| ------------------------------------------------------------ | ---------------------------- |
| 服务提供者和消费者 Group 不一致                              | 可能导致消费者发现不到提供者 |
| `spring.config.import` 中的 group 与 `spring.cloud.nacos.config.group` 不一致 | 可能导致远程配置加载失败     |
| 同一环境使用太多 Group                                       | 会增加维护和排查成本         |
| 配置 Group 和服务 Group 混乱                                 | 建议在项目规范中明确命名规则 |
| public Namespace + 多 Group                                  | 适合开发阶段，不适合生产隔离 |

### 用户认证配置

用户认证配置用于保护 Nacos 控制台、OpenAPI、配置中心和注册中心访问。Nacos 3.x 快速开始文档说明，Nacos 3.0.0 起控制台鉴权默认开启，首次打开控制台会要求初始化管理员用户 `nacos` 的密码；访问 Admin API 默认需要使用 access token。([Nacos 官网](https://nacos.io/en/docs/next/quickstart/quick-start/?utm_source=chatgpt.com))

本地 Docker 启动 Nacos 时，建议显式设置鉴权 Token 和服务端身份标识。Nacos Docker 快速开始文档要求配置 `NACOS_AUTH_TOKEN`、`NACOS_AUTH_IDENTITY_KEY`、`NACOS_AUTH_IDENTITY_VALUE`，其中 `NACOS_AUTH_TOKEN` 用于生成 JWT Token，要求使用长度大于 32 字符的字符串并进行 Base64 编码。([Nacos 官网](https://nacos.io/docs/v3.1/quickstart/quick-start-docker/?utm_source=chatgpt.com))

以下命令用于本地启动带基础鉴权参数的 Nacos Server。

```bash
# 固定 Nacos Server 版本，避免 latest 自动变化
export NACOS_VERSION=3.2.0

# 生成 Nacos JWT Token 密钥，生产环境应使用统一密钥管理系统生成和保存
export NACOS_AUTH_TOKEN="$(openssl rand -base64 48)"

# 生成 Nacos Server 内部通信身份标识
export NACOS_AUTH_IDENTITY_KEY="nacos-server-identity"
export NACOS_AUTH_IDENTITY_VALUE="$(openssl rand -hex 16)"

# 启动本地单机 Nacos，仅用于开发和测试
docker run --name nacos-standalone-derby \
  -e MODE=standalone \
  -e NACOS_AUTH_TOKEN="${NACOS_AUTH_TOKEN}" \
  -e NACOS_AUTH_IDENTITY_KEY="${NACOS_AUTH_IDENTITY_KEY}" \
  -e NACOS_AUTH_IDENTITY_VALUE="${NACOS_AUTH_IDENTITY_VALUE}" \
  -p 8080:8080 \
  -p 8848:8848 \
  -p 9848:9848 \
  -d nacos/nacos-server:${NACOS_VERSION}
```

命令说明：

| 参数                        | 说明                                       |
| --------------------------- | ------------------------------------------ |
| `MODE=standalone`           | 单机模式，仅适合本地开发和测试             |
| `NACOS_AUTH_TOKEN`          | JWT Token 密钥，生产环境必须使用安全随机值 |
| `NACOS_AUTH_IDENTITY_KEY`   | Nacos Server 内部 API 身份标识 Key         |
| `NACOS_AUTH_IDENTITY_VALUE` | Nacos Server 内部 API 身份标识 Value       |
| `8080`                      | Nacos 3.x 控制台访问端口                   |
| `8848`                      | Nacos 客户端访问和 OpenAPI 常用端口        |
| `9848`                      | Nacos gRPC 通信端口                        |

Spring Boot 应用侧需要配置 Nacos 用户名和密码。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cloud:
    nacos:
      # Nacos Server 地址
      server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}

      # Nacos 用户名，生产环境通过环境变量注入
      username: ${NACOS_USERNAME:nacos}

      # Nacos 密码，生产环境通过环境变量注入
      password: ${NACOS_PASSWORD:nacos}

      discovery:
        # 注册中心地址
        server-addr: ${spring.cloud.nacos.server-addr}

      config:
        # 配置中心地址
        server-addr: ${spring.cloud.nacos.server-addr}
```

生产环境启动时通过环境变量注入认证信息：

```bash
export NACOS_SERVER_ADDR=prod-nacos.example.internal:8848
export NACOS_USERNAME=nacos_prod_user
export NACOS_PASSWORD='replace-with-real-password'
export NACOS_NAMESPACE=prod-namespace-id

java -jar target/ateng-nacos-demo-1.0.0.jar \
  --spring.profiles.active=prod
```

如果需要通过 Nacos 3.x OpenAPI 调用 Admin API，需要先登录获取 access token，再携带 token 调用管理接口。Nacos 3.x 文档示例中，登录接口为 `POST /nacos/v3/auth/user/login`，Admin API 使用 `accessToken` 请求头。([Nacos 官网](https://nacos.io/en/docs/v3.0/quickstart/quick-start/?utm_source=chatgpt.com))

```bash
# 登录 Nacos 获取 access token
curl -X POST 'http://127.0.0.1:8848/nacos/v3/auth/user/login' \
  -d 'username=nacos' \
  -d 'password=your_password'

# 调用 Admin API 时携带 accessToken
curl -X POST 'http://127.0.0.1:8848/nacos/v3/admin/cs/config?dataId=ateng-nacos-demo-dev.yml&groupName=DEV_GROUP&content=hello' \
  -H 'accessToken: your_access_token'
```

生产安全建议：

| 建议               | 说明                                    |
| ------------------ | --------------------------------------- |
| Nacos 只暴露在内网 | 不允许公网直接访问控制台和 OpenAPI      |
| 开启鉴权           | 生产环境必须启用用户认证和权限控制      |
| 禁止使用默认弱密码 | 初始化后立即修改默认管理员密码          |
| Token 和密码不入库 | 不写入 Git 仓库，不写入镜像，不写入日志 |
| 按环境创建独立账号 | dev/test/prod 使用不同账号和权限        |
| 限制控制台访问来源 | 通过 VPN、堡垒机、网关或安全组限制访问  |
| 配置变更保留审计   | 生产配置发布需要评审、记录和回滚方案    |

## 本地开发与调试

本地开发与调试主要用于验证三件事：Nacos Server 是否正常启动、Spring Boot 服务是否成功注册、Nacos 配置是否可以动态刷新。Nacos 快速开始文档提供了控制台访问、服务注册、服务发现、发布配置和获取配置等验证方式，可作为本地排查基础。([Nacos 官网](https://nacos.io/en/docs/next/quickstart/quick-start/?utm_source=chatgpt.com))

### 本地 Nacos 启动

本地开发推荐使用 Docker 启动 Nacos，启动和清理都更简单。Nacos 3.x 控制台默认通过 `http://127.0.0.1:8080` 访问，客户端访问端口通常使用 `8848`；首次打开控制台会要求初始化管理员用户 `nacos` 的密码。([Nacos 官网](https://nacos.io/en/docs/next/quickstart/quick-start/?utm_source=chatgpt.com))

以下命令用于启动本地单机 Nacos。

```bash
# 设置版本
export NACOS_VERSION=3.2.0

# 设置鉴权参数
export NACOS_AUTH_TOKEN="$(openssl rand -base64 48)"
export NACOS_AUTH_IDENTITY_KEY="nacos-server-identity"
export NACOS_AUTH_IDENTITY_VALUE="$(openssl rand -hex 16)"

# 启动 Nacos 单机 Derby 模式
docker run --name nacos-standalone-derby \
  -e MODE=standalone \
  -e NACOS_AUTH_TOKEN="${NACOS_AUTH_TOKEN}" \
  -e NACOS_AUTH_IDENTITY_KEY="${NACOS_AUTH_IDENTITY_KEY}" \
  -e NACOS_AUTH_IDENTITY_VALUE="${NACOS_AUTH_IDENTITY_VALUE}" \
  -p 8080:8080 \
  -p 8848:8848 \
  -p 9848:9848 \
  -d nacos/nacos-server:${NACOS_VERSION}
```

查看启动日志：

```bash
# 查看 Nacos 启动日志
docker logs -f nacos-standalone-derby
```

看到以下日志说明启动成功：

```text
Nacos started successfully in stand alone mode. use embedded storage
```

访问控制台：

```text
http://127.0.0.1:8080
```

停止并删除本地 Nacos 容器：

```bash
# 停止容器
docker stop nacos-standalone-derby

# 删除容器
docker rm nacos-standalone-derby
```

如果本地端口冲突，可以检查端口占用：

```bash
# 检查 8080 端口占用
lsof -i :8080

# 检查 8848 端口占用
lsof -i :8848

# 检查 9848 端口占用
lsof -i :9848
```

### 本地服务注册验证

服务注册验证用于确认 Spring Boot 应用是否成功注册到 Nacos。验证时需要关注服务名、端口、Namespace、Group 和实例健康状态。

启动服务提供者：

```bash
# 启动 Provider 服务
java -jar target/ateng-nacos-provider-1.0.0.jar \
  --server.port=18081 \
  --spring.profiles.active=dev \
  --spring.application.name=ateng-nacos-provider \
  --NACOS_SERVER_ADDR=127.0.0.1:8848 \
  --NACOS_USERNAME=nacos \
  --NACOS_PASSWORD=nacos
```

启动服务消费者：

```bash
# 启动 Consumer 服务
java -jar target/ateng-nacos-consumer-1.0.0.jar \
  --server.port=18082 \
  --spring.profiles.active=dev \
  --spring.application.name=ateng-nacos-consumer \
  --NACOS_SERVER_ADDR=127.0.0.1:8848 \
  --NACOS_USERNAME=nacos \
  --NACOS_PASSWORD=nacos
```

在 Nacos 控制台验证：

```text
服务管理
└── 服务列表
    ├── ateng-nacos-provider
    └── ateng-nacos-consumer
```

也可以通过应用日志确认注册成功。重点查看以下关键词：

```text
Registering service with nacos
nacos registry
ateng-nacos-provider
ateng-nacos-consumer
```

如果服务没有出现在 Nacos 控制台中，按以下顺序排查：

| 排查项     | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| Nacos 地址 | `spring.cloud.nacos.server-addr` 是否为 `127.0.0.1:8848`     |
| 用户名密码 | Nacos 开启鉴权后应用侧是否配置正确                           |
| 服务名     | `spring.application.name` 是否存在                           |
| 注册开关   | `spring.cloud.nacos.discovery.enabled` 和 `register-enabled` 是否为 `true` |
| Namespace  | 应用配置的 Namespace ID 是否与控制台选择一致                 |
| Group      | 服务是否注册到预期 Group                                     |
| 网络端口   | 应用是否能访问 `8848` 和 `9848`                              |

### 配置刷新验证

配置刷新验证用于确认应用是否可以从 Nacos 配置中心读取配置，并在配置发布后动态刷新。前文已给出 `@ConfigurationProperties + @RefreshScope` 的读取方式，这里重点说明本地验证步骤。

在 Nacos 控制台创建配置：

```text
Data ID: ateng-nacos-demo-dev.yml
Group: DEV_GROUP
Format: YAML
```

配置内容：

```yaml
ateng:
  nacos:
    env: dev
    enable-demo: true
    message: "这是第一次发布的 Nacos 配置"
```

应用侧需要确认已经导入该配置。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  config:
    import:
      # 加载 dev 环境远程配置
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml?group=${nacos.config.group:DEV_GROUP}&refreshEnabled=true

  cloud:
    nacos:
      config:
        # Nacos 配置中心地址
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}

        # 配置格式
        file-extension: yml

        # 配置分组
        group: ${nacos.config.group:DEV_GROUP}

nacos:
  config:
    # 本地开发默认配置分组
    group: DEV_GROUP
```

调用配置查看接口：

```bash
# 查看当前配置
curl http://127.0.0.1:18080/config/properties
```

修改 Nacos 配置内容：

```yaml
ateng:
  nacos:
    env: dev
    enable-demo: true
    message: "这是动态刷新后的 Nacos 配置"
```

保存发布后再次调用：

```bash
# 再次查看配置，确认 message 是否变化
curl http://127.0.0.1:18080/config/properties
```

如果返回值中的 `message` 变为 `这是动态刷新后的 Nacos 配置`，说明动态刷新成功。

常见问题排查：

| 问题                 | 处理方式                                      |
| -------------------- | --------------------------------------------- |
| 配置读取不到         | 检查 Data ID、Group、Namespace、文件后缀      |
| 发布后不刷新         | 检查 `refreshEnabled=true` 和 `@RefreshScope` |
| YAML 解析失败        | 检查缩进、冒号、字符串引号                    |
| 本地配置覆盖远程配置 | 检查属性源优先级和是否存在同名本地配置        |
| 生产不建议随意刷新   | 生产配置变更需要发布流程和回滚方案            |

## 部署与验证

部署与验证用于将应用打包成可执行 Jar，并在目标环境中通过启动参数连接对应 Nacos Server，最后通过 Nacos 控制台、健康检查和接口调用确认应用可用。Spring Boot Maven Plugin 可以将应用打包为包含依赖的可执行 Jar，并支持通过 `java -jar` 运行。([Home](https://docs.spring.io/spring-boot/maven-plugin/packaging.html?utm_source=chatgpt.com))

### 应用打包

Spring Boot 项目使用 `spring-boot-maven-plugin` 后，可以通过 Maven `package` 阶段生成可执行 Jar。使用 `spring-boot-starter-parent` 时，插件的 repackage 执行通常已经预配置。([Home](https://docs.spring.io/spring-boot/maven-plugin/packaging.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

以下配置用于生成可执行 Jar，并排除 Lombok。

```xml
<build>
    <plugins>
        <!-- Spring Boot Maven 插件：生成可通过 java -jar 运行的可执行 Jar -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <!-- 打包时排除 Lombok，避免无意义依赖进入运行包 -->
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

执行打包命令：

```bash
# 清理并打包，跳过测试
mvn clean package -DskipTests

# 查看打包结果
ls -lh target/*.jar
```

如果是多模块项目，可以在根目录执行：

```bash
# 多模块统一打包
mvn clean package -DskipTests

# 只打包指定模块，同时构建其依赖模块
mvn clean package -pl ateng-nacos-consumer -am -DskipTests
```

验证 Jar 是否可执行：

```bash
# 查看 Jar 文件清单
jar tf target/ateng-nacos-demo-1.0.0.jar | head

# 本地启动验证
java -jar target/ateng-nacos-demo-1.0.0.jar --spring.profiles.active=dev
```

### 服务启动参数

服务启动参数用于在不同环境中覆盖应用端口、激活环境、Nacos 地址、认证信息、命名空间、服务分组和配置分组。生产环境推荐使用环境变量注入敏感信息，再通过 `java -jar` 启动应用。

开发环境启动：

```bash
java -jar target/ateng-nacos-demo-1.0.0.jar \
  --server.port=18080 \
  --spring.profiles.active=dev \
  --NACOS_SERVER_ADDR=127.0.0.1:8848 \
  --NACOS_USERNAME=nacos \
  --NACOS_PASSWORD=nacos
```

测试环境启动：

```bash
java -jar target/ateng-nacos-demo-1.0.0.jar \
  --server.port=18080 \
  --spring.profiles.active=test \
  --NACOS_SERVER_ADDR=192.168.10.100:8848 \
  --NACOS_USERNAME=nacos \
  --NACOS_PASSWORD=nacos \
  --NACOS_NAMESPACE=test-namespace-id
```

生产环境启动：

```bash
export SERVER_PORT=18080
export NACOS_SERVER_ADDR=prod-nacos.example.internal:8848
export NACOS_USERNAME=nacos_prod_user
export NACOS_PASSWORD='replace-with-real-password'
export NACOS_NAMESPACE=prod-namespace-id
export APP_VERSION=v1.0.0

java -jar target/ateng-nacos-demo-1.0.0.jar \
  --server.port=${SERVER_PORT} \
  --spring.profiles.active=prod
```

如果需要后台运行，可以使用 `nohup`：

```bash
# 创建日志目录
mkdir -p logs

# 后台启动应用
nohup java -jar target/ateng-nacos-demo-1.0.0.jar \
  --server.port=18080 \
  --spring.profiles.active=prod \
  > logs/ateng-nacos-demo.log 2>&1 &

# 查看启动日志
tail -f logs/ateng-nacos-demo.log
```

常用启动参数说明：

| 参数                       | 说明                           |
| -------------------------- | ------------------------------ |
| `--server.port`            | 当前应用 HTTP 端口             |
| `--spring.profiles.active` | 激活环境，例如 `dev/test/prod` |
| `--NACOS_SERVER_ADDR`      | Nacos Server 地址              |
| `--NACOS_USERNAME`         | Nacos 用户名                   |
| `--NACOS_PASSWORD`         | Nacos 密码                     |
| `--NACOS_NAMESPACE`        | Nacos Namespace ID             |
| `--APP_VERSION`            | 应用版本号，可写入注册元数据   |

### Nacos 控制台验证

Nacos 控制台验证用于确认应用是否注册成功、实例是否健康、配置是否存在、分组和命名空间是否正确。Nacos 3.x 快速开始文档中，控制台默认通过 `http://127.0.0.1:8080` 访问。([Nacos 官网](https://nacos.io/en/docs/next/quickstart/quick-start/?utm_source=chatgpt.com))

验证服务注册：

```text
Nacos 控制台
└── 服务管理
    └── 服务列表
        ├── 选择 Namespace
        ├── 选择 Group
        ├── 查看服务名
        └── 查看实例数和健康状态
```

重点检查：

| 检查项    | 期望结果                                         |
| --------- | ------------------------------------------------ |
| Namespace | 与 `spring.cloud.nacos.discovery.namespace` 一致 |
| Group     | 与 `spring.cloud.nacos.discovery.group` 一致     |
| 服务名    | 与 `spring.application.name` 一致                |
| 实例 IP   | 是当前机器或容器可访问 IP                        |
| 实例端口  | 与 `server.port` 一致                            |
| 健康状态  | healthy                                          |
| 元数据    | 包含 `env`、`version`、`owner` 等预期信息        |

验证配置中心：

```text
Nacos 控制台
└── 配置管理
    └── 配置列表
        ├── 选择 Namespace
        ├── 选择 Group
        ├── 查看 Data ID
        └── 查看配置内容和发布记录
```

重点检查：

| 检查项    | 期望结果                                              |
| --------- | ----------------------------------------------------- |
| Data ID   | `ateng-nacos-demo.yml`、`ateng-nacos-demo-dev.yml` 等 |
| Group     | 与 `spring.config.import` 中的 group 一致             |
| Format    | 与配置内容格式一致，例如 YAML                         |
| Namespace | 与 `spring.cloud.nacos.config.namespace` 一致         |
| 发布状态  | 已发布                                                |
| 配置内容  | YAML 缩进正确、键名正确                               |

如果控制台没有服务实例，但应用日志显示启动成功，优先检查 Nacos 账号密码、Namespace、Group 和网络端口。如果控制台有服务实例，但消费者调用失败，优先检查消费者和提供者是否在同一 Namespace 和 Group 下。

### 接口联调验证

接口联调验证用于确认服务注册、服务发现、负载均衡、OpenFeign 调用、配置中心读取和动态刷新是否全部正常。建议先验证单服务健康，再验证服务间调用，最后验证配置刷新。

验证应用健康：

```bash
# Provider 健康检查
curl http://127.0.0.1:18081/actuator/health

# Consumer 健康检查
curl http://127.0.0.1:18082/actuator/health
```

预期返回：

```json
{
  "status": "UP"
}
```

验证 Provider 本地接口：

```bash
curl "http://127.0.0.1:18081/provider/echo?message=hello"
```

预期返回：

```json
{
  "success": true,
  "service": "ateng-nacos-provider",
  "port": 18081,
  "message": "Provider 响应成功：hello"
}
```

验证 Consumer 通过 OpenFeign 调用 Provider：

```bash
curl "http://127.0.0.1:18082/consumer/feign/echo?message=hello"
```

预期返回：

```json
{
  "success": true,
  "consumer": "ateng-nacos-consumer",
  "providerResult": {
    "success": true,
    "service": "ateng-nacos-provider",
    "port": 18081,
    "message": "Provider 响应成功：hello"
  }
}
```

验证负载均衡：

```bash
# 连续调用 10 次，观察 providerResult.port 是否在多个 Provider 端口之间切换
for i in {1..10}; do
  curl -s "http://127.0.0.1:18082/consumer/feign/echo?message=hello-${i}"
  echo
done
```

如果同时启动了 `18081` 和 `18083` 两个 Provider 实例，返回中的 `providerResult.port` 应该能观察到不同端口。默认轮询策略下，端口通常会交替出现；如果配置了随机策略，则出现顺序不固定。

验证配置读取：

```bash
curl http://127.0.0.1:18080/config/properties
```

修改 Nacos 配置后再次验证：

```bash
curl http://127.0.0.1:18080/config/properties
```

联调问题排查表：

| 问题                      | 可能原因                         | 处理方式                                      |
| ------------------------- | -------------------------------- | --------------------------------------------- |
| `/actuator/health` 不通   | 服务未启动或端口错误             | 检查启动日志和 `server.port`                  |
| Provider 本地接口不通     | Controller 路径错误              | 检查接口路径和请求参数                        |
| Consumer 调 Provider 失败 | 服务未注册或服务名不一致         | 检查 Nacos 服务列表和 `@FeignClient(name)`    |
| 发现不到实例              | Namespace 或 Group 不一致        | 确认 Provider 与 Consumer 配置一致            |
| Feign 超时                | 网络不通或 Provider 响应慢       | 检查端口、防火墙、超时配置                    |
| 配置未刷新                | 未开启刷新或 Bean 未加刷新作用域 | 检查 `refreshEnabled=true` 和 `@RefreshScope` |
| 生产读取测试配置          | 环境变量或 Namespace 配错        | 检查启动参数和 Nacos 控制台选择               |
| Nacos 登录失败            | 用户名、密码或初始化状态异常     | 重置账号或检查鉴权配置                        |

完成以上验证后，可以认为项目已经具备基础的 Nacos 注册中心、配置中心、多环境隔离、安全鉴权、服务调用和部署验证能力。后续如果继续扩展，可以增加灰度发布、配置回滚、Sentinel 限流、Seata 分布式事务、网关路由和链路追踪等内容。