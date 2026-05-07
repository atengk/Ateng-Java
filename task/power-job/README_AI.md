# Spring Boot 集成 PowerJob 开发

本文用于说明如何在 Spring Boot 3 项目中集成 PowerJob，实现分布式任务调度、任务执行、任务日志查看与任务治理。PowerJob 官方定位为开源的分布式计算与任务调度框架，提供可视化前端页面，支持 CRON、固定频率、固定延迟和 OpenAPI 等调度方式，并支持单机、广播、Map、MapReduce 等执行模式。([GitHub](https://github.com/PowerJob/PowerJob))

## 项目概述

### 功能定位

本项目将 Spring Boot 3 应用作为 PowerJob Worker 接入 PowerJob Server，由 Server 负责任务配置、调度触发、状态管理和日志展示，由 Worker 负责任务处理器加载、业务逻辑执行和执行结果上报。

在整体架构中，PowerJob Server 是调度中心，主要用于创建任务、维护任务配置、触发任务实例、查看执行日志和管理 Worker 节点。Spring Boot 应用集成 `powerjob-worker-spring-boot-starter` 后，会作为 Worker 注册到 Server，并通过应用名称区分不同业务系统。PowerJob 官方说明其前端页面可用于任务管理、状态监控和在线日志查看；同时支持 Java、Shell、Python 等处理器形式，适合将业务系统中的定时逻辑统一接入调度平台。([GitHub](https://github.com/PowerJob/PowerJob))

集成后的核心能力包括：

| 能力            | 说明                                                     |
| --------------- | -------------------------------------------------------- |
| 任务统一管理    | 任务不再分散写死在业务代码中，可通过 PowerJob 控制台维护 |
| 动态调度        | 支持 CRON、固定频率、固定延迟、API 触发等调度方式        |
| 分布式执行      | 支持单机、广播、Map、MapReduce 等执行模式                |
| 执行日志查看    | Worker 执行日志可在控制台侧查看，降低排查成本            |
| 失败重试        | 任务失败后可按配置进行重试，提高任务可靠性               |
| Worker 横向扩展 | 同一应用可部署多个 Worker 节点，提升执行能力和可用性     |

### 适用场景

PowerJob 适合用于对“定时触发、异步执行、分布式处理、可视化治理”有要求的业务场景。官方列出的典型场景包括定时任务、广播任务、MapReduce 任务、延迟任务和通过 OpenAPI 自定义触发的任务。([GitHub](https://github.com/PowerJob/PowerJob))

常见业务落地场景如下：

| 场景           | 示例                                                         |
| -------------- | ------------------------------------------------------------ |
| 数据同步任务   | 定时同步第三方系统数据、同步数据仓库、刷新统计表             |
| 报表生成任务   | 每日生成销售报表、运营报表、对账报表                         |
| 订单超时处理   | 扫描超时未支付订单并关闭订单                                 |
| 异步补偿任务   | 对失败的支付回调、消息投递、接口调用进行补偿                 |
| 缓存刷新任务   | 定时预热热点数据缓存、刷新业务配置缓存                       |
| 广播清理任务   | 在所有 Worker 节点上执行本地缓存清理、临时文件清理           |
| 大批量处理任务 | 使用 Map 或 MapReduce 拆分大数据量处理任务，减少单机执行压力 |

不建议将 PowerJob 用于毫秒级强实时任务、强事务一致性任务或必须在当前请求线程内同步完成的业务逻辑。此类场景更适合使用本地方法调用、消息队列、事务补偿或专用流式处理框架。

### 技术选型

本开发文档基于 Spring Boot 3 技术栈，推荐使用 JDK 17 及以上版本。PowerJob Worker 依赖建议与 Server 版本保持一致，当前 Maven Central 可查询到 `powerjob-worker-spring-boot-starter` 的 `5.1.2` 版本，发布时间为 2025-08-17；GitHub Release 页面也显示 PowerJob-V5.1.2 为较新的发布版本。([Maven Repository](https://repo1.maven.org/maven2/tech/powerjob/powerjob-worker-spring-boot-starter/?utm_source=chatgpt.com))

| 技术组件                | 推荐版本                 | 说明                                             |
| ----------------------- | ------------------------ | ------------------------------------------------ |
| JDK                     | 17+                      | Spring Boot 3 基础运行要求                       |
| Spring Boot             | 3.x                      | 业务应用基础框架                                 |
| Maven                   | 3.8+                     | 项目构建与依赖管理                               |
| PowerJob Server         | v5.1.2                   | 任务调度中心，建议与 Worker 版本一致             |
| PowerJob Worker Starter | 5.1.2                    | Spring Boot 应用接入 PowerJob 的核心依赖         |
| MySQL                   | 8.x                      | PowerJob Server 元数据存储                       |
| Docker / Docker Compose | Docker 20+ / Compose v2+ | 本地或测试环境快速部署 Server                    |
| Lombok                  | 1.18.30+                 | 简化 Java Bean、日志对象等样板代码               |
| Hutool                  | 5.8.x                    | 常用工具类封装，便于参数判断、集合处理和日期处理 |

PowerJob Server 官方 Docker 镜像仓库提供 `powerjob/powerjob-server` 镜像，Docker Hub 当前可见的近期标签包含 `v5.1.2`，可用于本地开发和测试环境部署。([Docker Hub](https://hub.docker.com/r/powerjob/powerjob-server?utm_source=chatgpt.com))

## 环境准备

### 基础环境要求

本节用于说明集成 PowerJob 前需要准备的基础软件、网络端口和配置约束。环境准备完成后，后续章节才能继续进行 Worker 依赖引入、任务处理器开发和调度验证。

基础环境建议如下：

| 环境项         | 要求       | 验证命令                 |
| -------------- | ---------- | ------------------------ |
| JDK            | 17 或以上  | `java -version`          |
| Maven          | 3.8 或以上 | `mvn -version`           |
| Docker         | 20 或以上  | `docker version`         |
| Docker Compose | v2 或以上  | `docker compose version` |
| MySQL          | 8.x        | `mysql --version`        |
| Git            | 2.x        | `git --version`          |

执行以下命令检查本地基础环境：

```bash
# 查看 JDK 版本，Spring Boot 3 建议使用 JDK 17 或以上
java -version

# 查看 Maven 版本，用于构建 Spring Boot 项目
mvn -version

# 查看 Docker 版本，用于部署 PowerJob Server
docker version

# 查看 Docker Compose 版本，用于编排 MySQL 和 PowerJob Server
docker compose version
```

如果是本地开发环境，建议统一使用以下端口规划：

| 服务                    | 端口  | 说明                                                         |
| ----------------------- | ----- | ------------------------------------------------------------ |
| Spring Boot Worker      | 8080  | 业务应用端口，可按项目调整                                   |
| PowerJob Server         | 7700  | Worker 连接 Server 的地址端口，官方示例中 Worker 使用 `powerjob-server:7700` 连接 |
| PowerJob Server Console | 10086 | PowerJob Server 示例部署暴露的控制台相关端口                 |
| PowerJob Worker         | 27777 | Worker 默认通信端口，多实例部署时需要避免端口冲突            |

PowerJob 官方仓库中的 `docker-compose.yml` 示例暴露了 `7700`、`10086`、`10010`、`10077` 等 Server 端口，并在 Worker 示例中通过 `--powerjob.worker.server-address=powerjob-server:7700` 连接 Server。([GitHub](https://github.com/PowerJob/PowerJob/blob/master/docker-compose.yml))

### PowerJob Server 部署

本节用于部署本地开发和测试环境中的 PowerJob Server。生产环境建议按企业基础设施规范部署，并对数据库、日志目录、JVM 参数和网络访问权限进行单独规划。

推荐使用 Docker Compose 部署 MySQL 和 PowerJob Server。示例中固定使用 `powerjob/powerjob-server:v5.1.2`，避免直接使用 `latest` 导致环境不可复现。Docker Hub 当前可查询到该镜像标签。([Docker Hub](https://hub.docker.com/r/powerjob/powerjob-server?utm_source=chatgpt.com))

文件位置：`docker/powerjob/docker-compose.yml`

```yaml
version: "3.8"

services:
  powerjob-mysql:
    image: mysql:8.0
    container_name: powerjob-mysql
    restart: always
    environment:
      # MySQL root 密码，仅用于本地开发示例，生产环境请使用更强密码并接入密钥管理
      MYSQL_ROOT_PASSWORD: root123456
      # 初始化 PowerJob 使用的数据库
      MYSQL_DATABASE: powerjob
      # 设置容器时区
      TZ: Asia/Shanghai
    ports:
      # 本地如已安装 MySQL，可将宿主机端口改为 3307
      - "3307:3306"
    volumes:
      # 持久化 MySQL 数据，避免容器重建后数据丢失
      - ./data/mysql:/var/lib/mysql
    command:
      # PowerJob 表结构与字段建议使用 utf8mb4
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_general_ci
      - --lower_case_table_names=1

  powerjob-server:
    image: powerjob/powerjob-server:v5.1.2
    container_name: powerjob-server
    restart: always
    depends_on:
      - powerjob-mysql
    environment:
      # 控制 Server JVM 内存，本地开发不建议设置过大
      JVMOPTIONS: "-Xms256m -Xmx512m"
      # PowerJob Server 启动参数
      PARAMS: >-
        --oms.mongodb.enable=false
        --spring.datasource.core.jdbc-url=jdbc:mysql://powerjob-mysql:3306/powerjob?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
        --spring.datasource.core.username=root
        --spring.datasource.core.password=root123456
    ports:
      # Worker 连接 Server 使用
      - "7700:7700"
      # PowerJob 控制台相关端口
      - "10086:10086"
      # Server 其他服务端口，按官方示例暴露
      - "10010:10010"
      - "10077:10077"
    volumes:
      # 持久化 PowerJob Server 运行数据和日志
      - ./data/server:/root/powerjob/server
```

在 `docker/powerjob` 目录下执行启动命令：

```bash
# 创建部署目录
mkdir -p docker/powerjob
cd docker/powerjob

# 启动 MySQL 和 PowerJob Server
docker compose up -d

# 查看容器状态
docker compose ps

# 查看 PowerJob Server 日志
docker logs -f powerjob-server
```

命令说明：

`docker compose up -d` 用于后台启动服务；`docker compose ps` 用于确认容器是否正常运行；`docker logs -f powerjob-server` 用于查看 PowerJob Server 启动日志。如果日志中出现数据库连接失败，需要优先检查 MySQL 容器状态、数据库账号密码、JDBC 地址和容器网络。

部署完成后，先通过浏览器访问 PowerJob 控制台。如果无法访问，按以下顺序排查：

```bash
# 检查容器是否运行
docker ps | grep powerjob

# 检查 Server 端口是否监听
docker port powerjob-server

# 检查 Server 日志中是否存在数据库连接异常
docker logs powerjob-server --tail=200

# 检查 MySQL 容器日志
docker logs powerjob-mysql --tail=200
```

PowerJob Server 正常启动后，需要在控制台中创建或确认 Worker 应用名称。后续 Spring Boot Worker 配置中的 `powerjob.worker.app-name` 必须与控制台中的应用名称保持一致，否则 Worker 可能无法正确注册或归属到目标应用。

### Spring Boot 3 项目准备

本节用于准备一个可接入 PowerJob 的 Spring Boot 3 Worker 项目。这里仅放置项目初始化、基础依赖和最小启动配置；更完整的 Worker 参数、任务处理器和调度配置可在后续章节继续展开。

推荐项目结构如下：

```text
springboot-powerjob-demo
├── pom.xml
├── src
│   └── main
│       ├── java
│       │   └── io
│       │       └── github
│       │           └── atengk
│       │               └── powerjob
│       │                   └── PowerJobApplication.java
│       └── resources
│           └── application.yml
└── docker
    └── powerjob
        └── docker-compose.yml
```

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
           http://maven.apache.org/POM/4.0.0
           https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot-powerjob-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot-powerjob-demo</name>
    <description>Spring Boot 3 集成 PowerJob 示例项目</description>

    <properties>
        <!-- Spring Boot 3 建议使用 JDK 17 或以上版本 -->
        <java.version>17</java.version>
        <!-- 示例版本，建议与 PowerJob Server 版本保持一致 -->
        <powerjob.version>5.1.2</powerjob.version>
        <!-- 常用工具类版本 -->
        <hutool.version>5.8.35</hutool.version>
        <!-- Lombok 版本 -->
        <lombok.version>1.18.34</lombok.version>
    </properties>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <dependencies>
        <!-- Spring Boot Web 基础能力，便于后续提供健康检查或业务接口 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- PowerJob Worker Spring Boot Starter，用于将当前应用注册为 Worker -->
        <dependency>
            <groupId>tech.powerjob</groupId>
            <artifactId>powerjob-worker-spring-boot-starter</artifactId>
            <version>${powerjob.version}</version>
        </dependency>

        <!-- Hutool 工具类，便于后续任务参数解析、集合处理、日期处理等 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化日志对象、构造器、Getter、Setter 等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖，用于后续任务处理器单元测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件，用于生成可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

`powerjob-worker-spring-boot-starter` 的 Maven Central 目录中已经包含 `5.1.2` 版本，因此示例中将 Worker 依赖版本固定为 `5.1.2`。实际企业项目中，建议统一在父工程或 BOM 中管理该版本。([Maven Repository](https://repo1.maven.org/maven2/tech/powerjob/powerjob-worker-spring-boot-starter/?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 当前 Spring Boot Worker 应用端口
  port: 8080

spring:
  application:
    # 建议与 PowerJob 控制台中的应用名称保持一致
    name: springboot-powerjob-demo

powerjob:
  worker:
    # 是否启用 PowerJob Worker
    enabled: true

    # Worker 应用名称，需要与 PowerJob Server 控制台中的应用名称一致
    app-name: ${spring.application.name}

    # PowerJob Server 地址；本地 Docker 部署时使用 127.0.0.1:7700
    server-address: 127.0.0.1:7700

    # Worker 本地通信端口；同一机器部署多个 Worker 时必须使用不同端口
    port: 27777

    # 通信协议，常用 akka；具体以项目版本支持为准
    protocol: akka

    # 本地存储策略，disk 表示使用磁盘存储任务执行过程数据
    store-strategy: disk

    # 任务执行结果最大长度，超过后会被截断
    max-result-length: 8192

    # Worker 健康上报间隔，单位秒
    health-report-interval: 10

logging:
  level:
    # 当前项目日志级别
    io.github.atengk: info
    # PowerJob Worker 日志级别
    tech.powerjob: info
```

PowerJob Worker 的关键配置包括 `app-name`、`server-address`、`port`、`protocol`、`store-strategy`、`max-result-length` 和 `health-report-interval` 等。常见配置说明中，`appName` 和 `serverAddress` 属于 Worker 初始化的核心参数，`port` 可用于指定 Worker 绑定端口，`protocol` 默认通常为 AKKA，`storeStrategy` 默认通常为 DISK。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob/3-worker-architecture?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/powerjob/PowerJobApplication.java`

```java
package io.github.atengk.powerjob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 集成 PowerJob 启动类。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@SpringBootApplication
public class PowerJobApplication {

    /**
     * 应用启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PowerJobApplication.class, args);
    }

}
```

完成项目准备后，执行以下命令启动 Worker 应用：

```bash
# 编译项目，检查依赖是否正常
mvn clean package -DskipTests

# 启动 Spring Boot Worker
mvn spring-boot:run
```

启动后重点观察以下日志信息：

```text
PowerJob Worker started
server-address=127.0.0.1:7700
app-name=springboot-powerjob-demo
```

如果 Worker 无法连接 Server，优先检查以下配置：

| 检查项                    | 处理方式                                          |
| ------------------------- | ------------------------------------------------- |
| PowerJob Server 是否启动  | 执行 `docker ps` 和 `docker logs powerjob-server` |
| `server-address` 是否正确 | 本机 Docker 部署通常使用 `127.0.0.1:7700`         |
| `app-name` 是否一致       | 确认 PowerJob 控制台中已创建同名应用              |
| Worker 端口是否冲突       | 多实例启动时调整 `powerjob.worker.port`           |
| 防火墙是否放行端口        | 测试环境需放行 Server 和 Worker 通信端口          |

至此，PowerJob Server 和 Spring Boot 3 Worker 的基础环境已经准备完成。后续可以继续补充 `依赖与配置`、`PowerJob 核心概念` 和 `任务开发` 等章节。



## 依赖与配置

本节用于说明 Spring Boot 3 项目接入 PowerJob Worker 所需的 Maven 依赖、Worker 连接参数和应用基础配置。PowerJob Worker 依赖版本建议与 PowerJob Server 保持一致，示例继续使用 `5.1.2`，该版本已发布在 Maven Central。([Maven Repository](https://repo1.maven.org/maven2/tech/powerjob/powerjob-worker-spring-boot-starter/?utm_source=chatgpt.com))

### Maven 依赖配置

Maven 依赖主要包含 Spring Boot 基础依赖、PowerJob Worker Starter、Hutool 工具类和 Lombok。PowerJob Worker Starter 是接入 PowerJob Server 的核心依赖，用于完成 Worker 初始化、任务处理器扫描、任务执行和执行结果上报。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 3 推荐使用 JDK 17 或以上版本 -->
    <java.version>17</java.version>

    <!-- PowerJob Worker 版本，建议与 PowerJob Server 版本保持一致 -->
    <powerjob.version>5.1.2</powerjob.version>

    <!-- Hutool 工具类版本，用于参数处理、集合处理、日期处理等 -->
    <hutool.version>5.8.35</hutool.version>

    <!-- Lombok 版本，用于简化日志对象和 Java Bean 代码 -->
    <lombok.version>1.18.34</lombok.version>
</properties>

<dependencies>
    <!-- Spring Boot Web 基础依赖，提供 Web 容器、健康检查接口、HTTP 接口等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- PowerJob Worker Spring Boot Starter，用于将当前 Spring Boot 应用注册为 Worker -->
    <dependency>
        <groupId>tech.powerjob</groupId>
        <artifactId>powerjob-worker-spring-boot-starter</artifactId>
        <version>${powerjob.version}</version>
    </dependency>

    <!-- Hutool 工具类，后续任务参数解析、集合判断、日期处理会使用 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok 简化 Getter、Setter、构造器、日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于后续任务处理器单元测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用统一父工程管理版本，建议将 `powerjob.version` 放到父工程的 `dependencyManagement` 中，避免多个 Worker 服务之间出现版本不一致。PowerJob 的调度、任务实例、执行日志和 Worker 通信都依赖 Server 与 Worker 之间的协议兼容性，因此生产环境不建议随意混用不同大版本。

### PowerJob Worker 配置

PowerJob Worker 配置用于指定当前应用如何连接 PowerJob Server，以及 Worker 自身的通信端口、存储策略、执行结果长度和健康上报间隔。Worker 的关键参数包括 `appName`、`serverAddress`、`port`、`protocol`、`storeStrategy`、`maxResultLength` 和 `healthReportInterval` 等，其中 `appName` 和 `serverAddress` 是 Worker 注册和连接 Server 的核心配置。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob/3-worker-architecture?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # Spring Boot 应用端口
  port: 8080

spring:
  application:
    # 应用名称，建议与 PowerJob 控制台中的应用名称保持一致
    name: springboot-powerjob-demo

powerjob:
  worker:
    # 是否启用 PowerJob Worker
    enabled: true

    # Worker 应用名称，用于在 PowerJob Server 中归属到指定应用
    app-name: ${spring.application.name}

    # PowerJob Server 地址，多个 Server 地址可使用英文逗号分隔
    server-address: 127.0.0.1:7700

    # Worker 本地通信端口；同一机器部署多个 Worker 时需要避免端口冲突
    port: 27777

    # 通信协议，常用 akka；具体取值以当前 PowerJob 版本支持为准
    protocol: akka

    # 本地任务数据存储策略，disk 表示使用磁盘存储
    store-strategy: disk

    # 任务返回结果最大长度，超过后会被截断
    max-result-length: 8192

    # Worker 健康上报间隔，单位秒
    health-report-interval: 10

logging:
  level:
    # 当前业务项目日志级别
    io.github.atengk: info
    # PowerJob Worker 日志级别
    tech.powerjob: info
```

多环境项目建议拆分为 `application-dev.yml`、`application-test.yml` 和 `application-prod.yml`，不同环境只调整 Server 地址、Worker 端口、日志级别和运行参数。

文件位置：`src/main/resources/application-dev.yml`

```yaml
powerjob:
  worker:
    # 本地开发环境 PowerJob Server 地址
    server-address: 127.0.0.1:7700
    # 本地开发 Worker 端口
    port: 27777

logging:
  level:
    io.github.atengk: debug
    tech.powerjob: info
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
powerjob:
  worker:
    # 生产环境建议配置为内网域名或多 Server 地址
    server-address: powerjob-server-01:7700,powerjob-server-02:7700

    # 生产环境端口需要结合容器、Pod 或主机端口规划
    port: 27777

    # 生产环境不建议输出过长执行结果，避免日志和数据库膨胀
    max-result-length: 8192

logging:
  level:
    io.github.atengk: info
    tech.powerjob: warn
```

配置完成后，启动应用时指定环境：

```bash
# 使用开发环境配置启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 使用生产环境配置启动 Jar
java -jar springboot-powerjob-demo.jar --spring.profiles.active=prod
```

启动后需要确认 Worker 是否成功注册到 PowerJob Server。可以从三个位置验证：Spring Boot 控制台日志、PowerJob Server 控制台应用列表、PowerJob Server 任务执行节点列表。如果 Worker 无法注册，优先检查 `server-address`、`app-name`、网络连通性和 Worker 本地端口是否冲突。

### 应用基础配置

应用基础配置用于保证 Spring Boot 项目可以稳定启动，并为后续任务处理器开发提供统一的应用入口、健康检查接口和参数读取方式。这里给出最小可运行配置，后续章节的任务处理器可以直接基于该项目继续扩展。

项目启动类放在根包路径下，确保后续 `processor`、`controller`、`service` 等包都能被 Spring Boot 正常扫描。

文件位置：`src/main/java/io/github/atengk/powerjob/PowerJobApplication.java`

```java
package io.github.atengk.powerjob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 集成 PowerJob 启动类。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@SpringBootApplication
public class PowerJobApplication {

    /**
     * 应用启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PowerJobApplication.class, args);
    }

}
```

下面的接口用于本地或部署环境快速确认 Worker 应用是否正常启动，不负责判断 PowerJob Server 注册状态，只用于确认当前 Spring Boot 服务存活。

文件位置：`src/main/java/io/github/atengk/powerjob/controller/HealthController.java`

```java
package io.github.atengk.powerjob.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 应用健康检查接口。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestController
public class HealthController {

    /**
     * 检查当前 Worker 应用是否正常运行。
     *
     * @return 健康检查结果
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        log.info("执行应用健康检查");
        return MapUtil.<String, Object>builder()
                .put("status", "UP")
                .put("time", DateUtil.now())
                .put("message", "Spring Boot PowerJob Worker 运行正常")
                .build();
    }

}
```

启动应用后，可以使用以下命令验证基础接口：

```bash
# 启动应用
mvn spring-boot:run

# 验证健康检查接口
curl http://127.0.0.1:8080/health
```

预期响应示例：

```json
{
  "status": "UP",
  "time": "2026-05-07 10:30:00",
  "message": "Spring Boot PowerJob Worker 运行正常"
}
```

基础配置完成后，当前项目已经具备三项能力：可以作为普通 Spring Boot 应用启动；可以通过 PowerJob Worker 配置连接 PowerJob Server；可以提供基础健康检查接口用于部署验证。

## PowerJob 核心概念

本节用于说明 PowerJob 开发中必须理解的核心对象，包括 Server、Worker、应用、任务、Processor、任务实例和执行日志。理解这些概念后，再开发单机任务、广播任务和 MapReduce 任务会更清晰。

### Server 与 Worker

PowerJob 采用 Server 与 Worker 分离的架构。Server 是调度中心，Worker 是执行节点。Server 负责任务配置、调度触发、任务实例管理、运行状态查看和日志查询；Worker 负责任务处理器加载、任务执行、执行结果返回和执行日志上报。PowerJob 官方介绍中也明确提供了前端页面，用于任务管理、状态监控和在线日志查看。([GitHub](https://github.com/PowerJob/PowerJob?utm_source=chatgpt.com))

两者职责可以拆分如下：

| 角色            | 职责                                                         |
| --------------- | ------------------------------------------------------------ |
| PowerJob Server | 管理应用、管理任务、生成任务实例、分发任务、查看日志、维护执行状态 |
| PowerJob Worker | 注册应用节点、接收任务、执行 Processor、返回执行结果、上报运行日志 |

一次典型任务执行流程如下：

```text
1. 开发人员在 PowerJob Server 控制台创建任务
2. Server 根据任务调度策略生成任务实例
3. Server 根据应用名称找到可用 Worker 节点
4. Server 将任务下发给 Worker
5. Worker 调用对应 Processor 执行业务逻辑
6. Worker 返回执行结果并上报日志
7. Server 更新任务实例状态并展示执行日志
```

在部署层面，一个 PowerJob Server 可以管理多个应用，一个应用可以对应多个 Worker 节点。生产环境中通常会为同一个 Spring Boot Worker 服务部署多个实例，提升任务执行可用性和处理能力。

### 应用与任务

应用是 Worker 在 PowerJob Server 中的逻辑分组，通常对应一个业务系统或一个 Spring Boot 服务。任务是挂载在应用下面的调度配置，用于定义什么时候执行、执行哪个 Processor、使用什么执行模式以及失败后如何处理。

应用与任务的关系如下：

```text
PowerJob Server
└── 应用：springboot-powerjob-demo
    ├── 任务：每日数据同步任务
    ├── 任务：订单超时关闭任务
    ├── 任务：缓存刷新广播任务
    └── 任务：大批量用户统计任务
```

应用名称由 Worker 配置中的 `powerjob.worker.app-name` 指定。该名称需要与 PowerJob Server 控制台中的应用名称保持一致，否则任务无法正确路由到目标 Worker。

任务通常包含以下关键配置：

| 配置项       | 说明                                 |
| ------------ | ------------------------------------ |
| 任务名称     | 用于识别当前任务的业务含义           |
| 任务描述     | 说明任务用途、执行范围和注意事项     |
| 调度方式     | CRON、固定频率、固定延迟、API 触发等 |
| 执行模式     | 单机、广播、Map、MapReduce 等        |
| Processor    | Worker 中实际执行业务逻辑的处理器    |
| 任务参数     | 运行时传递给 Processor 的参数        |
| 最大重试次数 | 任务失败后的重试策略                 |
| 超时时间     | 单次任务执行允许的最大耗时           |
| 告警策略     | 任务失败后的通知方式，按企业配置决定 |

PowerJob 支持 CRON、固定频率、固定延迟和 OpenAPI 等调度策略，也支持单机、广播、Map、MapReduce 等执行模式，适合覆盖普通定时任务和分布式批处理任务。([GitHub](https://github.com/PowerJob/PowerJob?utm_source=chatgpt.com))

### Processor 处理器

Processor 是 Worker 中真正执行业务逻辑的组件。PowerJob Server 只负责调度和分发任务，具体业务逻辑需要由 Worker 侧的 Processor 完成。开发人员通常会在 Spring Boot 项目中创建 Processor 类，并在 PowerJob 控制台任务配置中填写对应的处理器名称或全限定类名。

Processor 开发时需要关注四个问题：

| 问题             | 说明                                            |
| ---------------- | ----------------------------------------------- |
| 任务参数如何获取 | 从任务上下文中读取控制台配置的参数              |
| 业务逻辑如何执行 | 在 Processor 中调用 Service、Mapper、远程接口等 |
| 执行结果如何返回 | 返回成功或失败状态，以及简短执行信息            |
| 异常如何处理     | 捕获业务异常并返回失败结果，必要时记录日志      |

下面给出一个基础 Processor 示例，用于读取任务参数并输出执行结果。该示例适合普通单机任务。

文件位置：`src/main/java/io/github/atengk/powerjob/processor/DemoStandaloneProcessor.java`

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 单机任务示例处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class DemoStandaloneProcessor implements BasicProcessor {

    /**
     * 执行 PowerJob 单机任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String jobParams = context.getJobParams();

        if (StrUtil.isBlank(jobParams)) {
            log.warn("PowerJob 单机任务参数为空，instanceId={}", context.getInstanceId());
            return new ProcessResult(false, "任务参数不能为空");
        }

        log.info("开始执行 PowerJob 单机任务，instanceId={}，jobParams={}", context.getInstanceId(), jobParams);

        try {
            // 这里编写实际业务逻辑，例如数据同步、订单关闭、报表生成等
            log.info("PowerJob 单机任务执行完成，instanceId={}", context.getInstanceId());
            return new ProcessResult(true, "任务执行成功，参数：" + jobParams);
        } catch (Exception e) {
            log.error("PowerJob 单机任务执行失败，instanceId={}", context.getInstanceId(), e);
            return new ProcessResult(false, "任务执行失败：" + e.getMessage());
        }
    }

}
```

如果任务逻辑需要复用业务 Service，推荐将 Processor 作为 Spring Bean 管理，再通过构造器注入业务组件。这样 Processor 只负责任务入口控制，具体业务逻辑仍放在 Service 层。

文件位置：`src/main/java/io/github/atengk/powerjob/service/DataSyncService.java`

```java
package io.github.atengk.powerjob.service;

/**
 * 数据同步业务服务。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface DataSyncService {

    /**
     * 根据任务参数执行数据同步。
     *
     * @param jobParams 任务参数
     * @return 同步结果描述
     */
    String syncData(String jobParams);

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/service/impl/DataSyncServiceImpl.java`

```java
package io.github.atengk.powerjob.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.powerjob.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 数据同步业务服务实现。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
public class DataSyncServiceImpl implements DataSyncService {

    /**
     * 根据任务参数执行数据同步。
     *
     * @param jobParams 任务参数
     * @return 同步结果描述
     */
    @Override
    public String syncData(String jobParams) {
        if (StrUtil.isBlank(jobParams)) {
            log.warn("数据同步任务参数为空");
            return "数据同步失败：任务参数为空";
        }

        log.info("开始执行数据同步，jobParams={}，time={}", jobParams, DateUtil.now());

        // 示例业务逻辑：实际项目中可调用 Mapper、远程接口、消息队列或文件服务
        String result = StrUtil.format("数据同步完成，参数={}，完成时间={}", jobParams, DateUtil.now());

        log.info("数据同步执行完成，result={}", result);
        return result;
    }

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/DataSyncProcessor.java`

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.powerjob.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 数据同步任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncProcessor implements BasicProcessor {

    private final DataSyncService dataSyncService;

    /**
     * 执行数据同步任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String jobParams = context.getJobParams();

        if (StrUtil.isBlank(jobParams)) {
            log.warn("数据同步任务参数为空，instanceId={}", context.getInstanceId());
            return new ProcessResult(false, "任务参数不能为空");
        }

        try {
            log.info("开始执行数据同步任务，instanceId={}，jobParams={}", context.getInstanceId(), jobParams);
            String result = dataSyncService.syncData(jobParams);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("数据同步任务执行异常，instanceId={}，jobParams={}", context.getInstanceId(), jobParams, e);
            return new ProcessResult(false, "数据同步任务执行异常：" + e.getMessage());
        }
    }

}
```

在 PowerJob 控制台创建任务时，Processor 配置需要与当前项目中的 Processor 能够匹配。实际填写方式以当前 PowerJob Server 版本控制台要求为准，常见方式包括填写全限定类名或填写 Spring Bean 处理器名称。

### 任务实例与执行日志

任务是调度配置，任务实例是某一次真实执行记录。一个任务可以被调度多次，每次触发都会生成一个任务实例。任务实例会记录本次执行的触发时间、执行节点、执行状态、耗时、结果和日志信息。

任务与任务实例的关系如下：

```text
任务：每日数据同步任务
├── 实例：2026-05-07 00:00:00 执行，成功
├── 实例：2026-05-08 00:00:00 执行，成功
├── 实例：2026-05-09 00:00:00 执行，失败
└── 实例：2026-05-09 00:05:00 重试，成功
```

任务实例常见状态包括等待调度、运行中、执行成功、执行失败、执行超时和已取消。开发人员排查任务问题时，不应只看业务应用日志，还需要结合 PowerJob 控制台中的任务实例状态和在线执行日志一起判断。

执行日志通常用于定位以下问题：

| 问题类型      | 排查方式                                                    |
| ------------- | ----------------------------------------------------------- |
| 任务未触发    | 检查调度时间、任务状态、Server 时间和 CRON 表达式           |
| Worker 未执行 | 检查 Worker 是否在线、应用名称是否一致、Server 地址是否正确 |
| 任务执行失败  | 查看 Processor 异常日志、任务参数和业务依赖状态             |
| 任务执行超时  | 检查任务超时时间、SQL 慢查询、远程接口耗时和批处理数据量    |
| 执行结果异常  | 检查 `ProcessResult` 返回内容和异常捕获逻辑                 |

任务执行日志建议遵循以下规范：

```text
1. 任务开始时打印 instanceId、jobId、任务参数
2. 核心业务分支打印关键处理数量和条件
3. 外部接口调用失败时打印请求标识和异常摘要
4. 批量处理任务打印总数、成功数、失败数
5. 任务结束时返回简短明确的 ProcessResult
```

Processor 中建议返回简短结果，不建议将大批量明细、完整 SQL 结果、完整接口响应体直接放入执行结果。Worker 配置中的 `max-result-length` 会限制返回结果长度，超过限制后可能被截断。配置项默认值和语义可参考 Worker 配置说明。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob/3-worker-architecture?utm_source=chatgpt.com))



## 任务开发

本节用于说明 PowerJob 常见任务类型的开发方式，包括单机任务、广播任务、MapReduce 任务和秒级任务。PowerJob 官方说明其支持单机、广播、Map、MapReduce 等执行模式，同时支持 CRON、固定频率、固定延迟和 OpenAPI 等调度策略。([GitHub](https://github.com/PowerJob/PowerJob?utm_source=chatgpt.com))

### 单机任务开发

单机任务是最常用的任务类型。每次任务触发时，PowerJob Server 会从可用 Worker 中选择一个节点执行任务。它适合数据同步、订单扫描、报表生成、状态检查等不需要所有节点同时执行的场景。

单机任务通常实现 `BasicProcessor` 接口，通过 `TaskContext` 获取任务参数，并通过 `ProcessResult` 返回执行结果。

文件位置：`src/main/java/io/github/atengk/powerjob/processor/OrderTimeoutCloseProcessor.java`

下面的处理器用于模拟关闭超时未支付订单，适合作为单机任务示例。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 订单超时关闭任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class OrderTimeoutCloseProcessor implements BasicProcessor {

    /**
     * 执行订单超时关闭任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String jobParams = context.getJobParams();
        log.info("开始执行订单超时关闭任务，instanceId={}，jobParams={}", context.getInstanceId(), jobParams);

        try {
            if (StrUtil.isBlank(jobParams)) {
                log.warn("订单超时关闭任务参数为空，instanceId={}", context.getInstanceId());
                return new ProcessResult(false, "任务参数不能为空");
            }

            OrderTimeoutParam param = JSONUtil.toBean(jobParams, OrderTimeoutParam.class);
            if (param.getTimeoutMinutes() == null || param.getTimeoutMinutes() <= 0) {
                log.warn("订单超时时间配置不合法，timeoutMinutes={}", param.getTimeoutMinutes());
                return new ProcessResult(false, "timeoutMinutes 必须大于 0");
            }

            // 示例逻辑：实际项目中应调用订单 Service 查询并关闭超时订单
            int closeCount = closeTimeoutOrders(param);

            String result = StrUtil.format("订单超时关闭任务执行成功，关闭订单数量={}", closeCount);
            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("订单超时关闭任务执行异常，instanceId={}", context.getInstanceId(), e);
            return new ProcessResult(false, "订单超时关闭任务执行异常：" + e.getMessage());
        }
    }

    /**
     * 关闭超时订单。
     *
     * @param param 任务参数
     * @return 关闭数量
     */
    private int closeTimeoutOrders(OrderTimeoutParam param) {
        log.info("查询并关闭超时订单，timeoutMinutes={}，batchSize={}", param.getTimeoutMinutes(), param.getBatchSize());

        // 示例返回值，实际应替换为数据库查询和更新逻辑
        return Math.min(param.getBatchSize() == null ? 100 : param.getBatchSize(), 100);
    }

    /**
     * 订单超时关闭任务参数。
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class OrderTimeoutParam {

        /**
         * 超时时间，单位分钟。
         */
        private Integer timeoutMinutes;

        /**
         * 单次处理数量。
         */
        private Integer batchSize;

    }

}
```

PowerJob 控制台任务配置示例：

```text
任务名称：订单超时关闭任务
执行模式：单机执行
处理器类型：Java / Spring Bean
处理器信息：orderTimeoutCloseProcessor 或 io.github.atengk.powerjob.processor.OrderTimeoutCloseProcessor
调度方式：CRON
CRON 表达式：0 */5 * * * ?
任务参数：{"timeoutMinutes":30,"batchSize":100}
```

验证方式：

```bash
# 启动 Worker 应用
mvn spring-boot:run

# 在 PowerJob 控制台手动触发任务
# 然后查看任务实例状态和 Worker 控制台日志
```

单机任务需要重点关注幂等性。任务可能因为失败重试、手动重复触发、Worker 重启等原因重复执行，因此关闭订单、同步数据、发送通知等业务逻辑必须能安全重复执行。

### 广播任务开发

广播任务会在当前应用的所有可用 Worker 节点上执行一次，适合清理本地缓存、刷新本地配置、删除临时文件、输出节点状态等需要每个节点都执行的场景。PowerJob 的广播执行流程通常包含前置处理、所有 Worker 并行执行、后置汇总三个阶段。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob/3.1-task-tracking-and-execution?utm_source=chatgpt.com))

广播任务通常实现 `BroadcastProcessor` 接口。`preProcess` 只执行一次，用于前置校验；`process` 会在每个 Worker 上执行；`postProcess` 在所有节点执行完成后汇总结果。

文件位置：`src/main/java/io/github/atengk/powerjob/processor/LocalCacheRefreshProcessor.java`

下面的处理器用于在所有 Worker 节点上刷新本地缓存。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.core.processor.sdk.BroadcastProcessor;

import java.net.InetAddress;
import java.util.List;

/**
 * 本地缓存刷新广播任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class LocalCacheRefreshProcessor implements BroadcastProcessor {

    /**
     * 广播任务前置处理。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult preProcess(TaskContext context) {
        String jobParams = context.getJobParams();
        log.info("开始执行缓存刷新广播任务前置检查，instanceId={}，jobParams={}", context.getInstanceId(), jobParams);

        if (StrUtil.isBlank(jobParams)) {
            log.warn("缓存刷新广播任务参数为空");
            return new ProcessResult(false, "缓存名称不能为空");
        }

        return new ProcessResult(true, "前置检查通过");
    }

    /**
     * 每个 Worker 节点执行缓存刷新。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String cacheName = context.getJobParams();

        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            log.info("当前 Worker 开始刷新本地缓存，hostName={}，cacheName={}", hostName, cacheName);

            // 示例逻辑：实际项目中可调用 Caffeine、Guava Cache、本地 Map 或配置中心刷新逻辑
            refreshLocalCache(cacheName);

            String result = StrUtil.format("节点 {} 刷新缓存 {} 成功", hostName, cacheName);
            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("当前 Worker 刷新本地缓存失败，cacheName={}", cacheName, e);
            return new ProcessResult(false, "刷新本地缓存失败：" + e.getMessage());
        }
    }

    /**
     * 广播任务后置汇总。
     *
     * @param context     任务上下文
     * @param taskResults 各 Worker 执行结果
     * @return 汇总结果
     */
    @Override
    public ProcessResult postProcess(TaskContext context, List<TaskResult> taskResults) {
        if (CollUtil.isEmpty(taskResults)) {
            log.warn("缓存刷新广播任务没有收到 Worker 执行结果，instanceId={}", context.getInstanceId());
            return new ProcessResult(false, "未收到 Worker 执行结果");
        }

        long successCount = taskResults.stream().filter(TaskResult::isSuccess).count();
        long failedCount = taskResults.size() - successCount;

        String result = StrUtil.format("缓存刷新广播任务完成，总节点数={}，成功={}，失败={}",
                taskResults.size(), successCount, failedCount);

        log.info(result);
        return new ProcessResult(failedCount == 0, result);
    }

    /**
     * 刷新本地缓存。
     *
     * @param cacheName 缓存名称
     */
    private void refreshLocalCache(String cacheName) {
        log.info("执行本地缓存刷新逻辑，cacheName={}", cacheName);

        // 示例逻辑：实际项目中替换为真实缓存刷新实现
    }

}
```

PowerJob 控制台任务配置示例：

```text
任务名称：刷新本地缓存广播任务
执行模式：广播执行
处理器类型：Java / Spring Bean
处理器信息：localCacheRefreshProcessor 或 io.github.atengk.powerjob.processor.LocalCacheRefreshProcessor
调度方式：手动触发
任务参数：userCache
```

广播任务不适合执行“只能执行一次”的业务逻辑，例如扣款、发券、生成唯一流水等。因为广播模式会在多个 Worker 节点同时执行，业务逻辑如果没有按节点隔离或幂等控制，可能导致重复处理。

### MapReduce 任务开发

MapReduce 任务适合处理大批量数据。它的基本思路是：根任务负责拆分任务分片，多个 Worker 并行处理分片，最后由 Reduce 阶段汇总所有分片结果。PowerJob 的 MapReduce 执行过程通常包括根任务生成子任务、TaskTracker 分发子任务、子任务执行完成后创建最终 Reduce 任务并汇总结果。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob/3.1-task-tracking-and-execution?utm_source=chatgpt.com))

MapReduce 任务适合以下场景：

| 场景           | 说明                                       |
| -------------- | ------------------------------------------ |
| 大批量用户统计 | 将用户 ID 按范围拆分，多个 Worker 并行统计 |
| 大表数据修复   | 按主键区间拆分，分片执行修复逻辑           |
| 批量消息补偿   | 将失败消息分片后并行补偿                   |
| 大文件处理     | 按文件行号、分片编号或对象 Key 拆分处理    |

文件位置：`src/main/java/io/github/atengk/powerjob/processor/UserStatisticMapReduceProcessor.java`

下面的处理器用于模拟批量用户统计任务。根任务拆分用户 ID 区间，子任务处理区间数据，Reduce 阶段汇总分片结果。不同 PowerJob 5.x 小版本的 `map(...)` 方法签名可能存在细微差异，实际项目以当前引入版本的接口为准。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.core.processor.sdk.MapReduceProcessor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户统计 MapReduce 任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class UserStatisticMapReduceProcessor implements MapReduceProcessor {

    /**
     * 执行 MapReduce 任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        try {
            if (isRootTask()) {
                log.info("开始执行用户统计根任务，instanceId={}，jobParams={}",
                        context.getInstanceId(), context.getJobParams());

                List<UserStatisticSubTask> subTasks = buildSubTasks(context.getJobParams());
                if (CollUtil.isEmpty(subTasks)) {
                    return new ProcessResult(false, "未生成有效子任务");
                }

                log.info("用户统计根任务拆分完成，subTaskCount={}", subTasks.size());

                // 将子任务分发到 Worker 集群执行
                return map(subTasks, "USER_STATISTIC_SUB_TASK");
            }

            UserStatisticSubTask subTask = (UserStatisticSubTask) context.getSubTask();
            if (subTask == null) {
                log.warn("用户统计子任务为空，instanceId={}", context.getInstanceId());
                return new ProcessResult(false, "子任务不能为空");
            }

            log.info("开始执行用户统计子任务，startId={}，endId={}", subTask.getStartId(), subTask.getEndId());

            int count = statisticUserCount(subTask);
            String result = StrUtil.format("分片统计完成，startId={}，endId={}，count={}",
                    subTask.getStartId(), subTask.getEndId(), count);

            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("用户统计 MapReduce 任务执行异常，instanceId={}", context.getInstanceId(), e);
            return new ProcessResult(false, "用户统计任务执行异常：" + e.getMessage());
        }
    }

    /**
     * 汇总所有子任务执行结果。
     *
     * @param context     任务上下文
     * @param taskResults 子任务执行结果
     * @return 汇总结果
     */
    @Override
    public ProcessResult reduce(TaskContext context, List<TaskResult> taskResults) {
        if (CollUtil.isEmpty(taskResults)) {
            log.warn("用户统计 Reduce 阶段未收到子任务结果，instanceId={}", context.getInstanceId());
            return new ProcessResult(false, "未收到子任务执行结果");
        }

        long successCount = taskResults.stream().filter(TaskResult::isSuccess).count();
        long failedCount = taskResults.size() - successCount;

        String result = StrUtil.format("用户统计 MapReduce 任务完成，分片总数={}，成功={}，失败={}",
                taskResults.size(), successCount, failedCount);

        log.info(result);
        return new ProcessResult(failedCount == 0, result);
    }

    /**
     * 构建用户统计子任务。
     *
     * @param jobParams 任务参数
     * @return 子任务列表
     */
    private List<UserStatisticSubTask> buildSubTasks(String jobParams) {
        int maxUserId = NumberUtil.isInteger(jobParams) ? Integer.parseInt(jobParams) : 10000;
        int step = 1000;

        List<UserStatisticSubTask> subTasks = new ArrayList<>();
        for (int startId = 1; startId <= maxUserId; startId += step) {
            int endId = Math.min(startId + step - 1, maxUserId);
            subTasks.add(new UserStatisticSubTask(startId, endId));
        }

        return subTasks;
    }

    /**
     * 统计用户数量。
     *
     * @param subTask 子任务
     * @return 统计数量
     */
    private int statisticUserCount(UserStatisticSubTask subTask) {
        // 示例逻辑：实际项目中应按 startId 和 endId 查询数据库
        return subTask.getEndId() - subTask.getStartId() + 1;
    }

    /**
     * 用户统计子任务。
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatisticSubTask implements Serializable {

        /**
         * 开始用户 ID。
         */
        private Integer startId;

        /**
         * 结束用户 ID。
         */
        private Integer endId;

    }

}
```

PowerJob 控制台任务配置示例：

```text
任务名称：用户统计 MapReduce 任务
执行模式：MapReduce
处理器类型：Java / Spring Bean
处理器信息：userStatisticMapReduceProcessor 或 io.github.atengk.powerjob.processor.UserStatisticMapReduceProcessor
调度方式：手动触发
任务参数：10000
```

MapReduce 任务开发注意事项：

| 注意事项               | 说明                                                  |
| ---------------------- | ----------------------------------------------------- |
| 子任务对象必须可序列化 | 子任务需要在 Worker 之间传递，建议实现 `Serializable` |
| 单个子任务不要过大     | 分片过大会降低并行度，也会增加失败重试成本            |
| Reduce 只做汇总        | 不建议在 Reduce 中执行大量业务处理                    |
| 子任务要幂等           | 子任务失败后可能重试，数据库更新需要防重复            |
| 结果不要过长           | 子任务返回结果应简短，避免超过 `max-result-length`    |

### 秒级任务开发

秒级任务一般通过固定频率或固定延迟调度实现，适合高频状态检查、短周期补偿、小批量数据轮询等场景。PowerJob 支持固定频率和固定延迟等调度策略，可用于配置秒级触发任务。([GitHub](https://github.com/PowerJob/PowerJob?utm_source=chatgpt.com))

秒级任务必须控制执行耗时。如果任务每 5 秒触发一次，但单次执行需要 20 秒，就会造成任务堆积、重复执行或资源占用过高。秒级任务建议遵循以下原则：

| 原则             | 说明                                       |
| ---------------- | ------------------------------------------ |
| 单次处理数据量小 | 每次只处理少量数据，例如 50 条或 100 条    |
| 执行时间可控     | 单次执行耗时应小于调度间隔                 |
| 强制幂等         | 高频任务更容易重复触发                     |
| 避免长事务       | 不要在秒级任务中执行大事务                 |
| 避免重 IO        | 远程接口、慢 SQL、大文件处理不适合秒级任务 |

文件位置：`src/main/java/io/github/atengk/powerjob/processor/SecondLevelCompensationProcessor.java`

下面的处理器用于模拟秒级补偿失败记录，每次只处理少量数据。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 秒级补偿任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class SecondLevelCompensationProcessor implements BasicProcessor {

    /**
     * 执行秒级补偿任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        long startTime = System.currentTimeMillis();
        String jobParams = context.getJobParams();

        int batchSize = NumberUtil.isInteger(jobParams) ? Integer.parseInt(jobParams) : 50;
        batchSize = Math.min(batchSize, 200);

        log.info("开始执行秒级补偿任务，instanceId={}，batchSize={}", context.getInstanceId(), batchSize);

        try {
            int successCount = compensateFailedRecords(batchSize);
            long cost = System.currentTimeMillis() - startTime;

            String result = StrUtil.format("秒级补偿任务执行完成，处理数量={}，耗时={}ms", successCount, cost);
            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("秒级补偿任务执行异常，instanceId={}", context.getInstanceId(), e);
            return new ProcessResult(false, "秒级补偿任务执行异常：" + e.getMessage());
        }
    }

    /**
     * 补偿失败记录。
     *
     * @param batchSize 单次处理数量
     * @return 成功数量
     */
    private int compensateFailedRecords(int batchSize) {
        // 示例逻辑：实际项目中应查询待补偿记录，并使用状态机或唯一键保证幂等
        log.info("查询并补偿失败记录，batchSize={}", batchSize);
        return batchSize;
    }

}
```

PowerJob 控制台任务配置示例：

```text
任务名称：秒级失败记录补偿任务
执行模式：单机执行
处理器类型：Java / Spring Bean
处理器信息：secondLevelCompensationProcessor 或 io.github.atengk.powerjob.processor.SecondLevelCompensationProcessor
调度方式：固定频率
固定频率：5 秒
任务参数：50
```

秒级任务建议只处理“轻量、可重复、可中断”的逻辑。大批量数据处理应优先使用普通定时任务或 MapReduce 任务，不建议通过高频单机任务硬扫大表。

## 任务处理器实现

本节用于说明 PowerJob 处理器的几种实现方式，包括内置处理器、Java 处理器、Spring Bean 接入方式，以及任务参数的接收和解析。PowerJob 支持 Java、Shell、Python 等处理器类型，同时在 Spring Boot 场景中可以使用 Spring Bean 或注解方法简化接入。([GitHub](https://github.com/PowerJob/PowerJob?utm_source=chatgpt.com))

### 内置处理器使用

内置处理器适合执行 Shell、Python、HTTP 或简单脚本类任务，具体能力以当前 PowerJob Server 版本控制台支持项为准。PowerJob 官方说明其支持 Java、Shell、Python 等处理器类型；在 Spring Boot 业务项目中，业务强相关任务仍建议优先使用 Java Processor 或 Spring Bean Processor。([GitHub](https://github.com/PowerJob/PowerJob?utm_source=chatgpt.com))

常见处理器选择建议如下：

| 处理器类型            | 适用场景                               | 建议       |
| --------------------- | -------------------------------------- | ---------- |
| Java Processor        | 业务系统内部任务                       | 推荐       |
| Spring Bean Processor | 需要注入 Service、Mapper、Redis 等组件 | 推荐       |
| Shell Processor       | 服务器脚本、文件清理、运维命令         | 谨慎使用   |
| Python Processor      | 简单脚本、数据处理脚本                 | 谨慎使用   |
| HTTP / OpenAPI 触发   | 外部系统触发任务                       | 按场景使用 |

Shell 处理器示例脚本：

```bash
#!/bin/bash

# 输出当前执行节点信息
echo "开始执行 PowerJob Shell 任务"
echo "当前用户：$(whoami)"
echo "当前目录：$(pwd)"
echo "当前时间：$(date '+%Y-%m-%d %H:%M:%S')"

# 示例：清理 7 天前的临时日志文件
find /tmp -name "*.log" -mtime +7 -type f -print
echo "PowerJob Shell 任务执行完成"
```

内置处理器注意事项：

| 注意事项 | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 权限控制 | Shell 或 Python 脚本执行权限较高，生产环境必须限制可执行命令范围 |
| 日志输出 | 脚本应输出关键日志，便于在控制台查看                         |
| 依赖环境 | 脚本依赖的命令、Python 包、文件路径需要在所有 Worker 节点存在 |
| 幂等控制 | 脚本重复执行时不能造成数据损坏                               |
| 资源限制 | 不建议在脚本中执行高 CPU、高内存或长时间阻塞操作             |

### Java 处理器开发

Java 处理器适合承载业务逻辑，是 Spring Boot 集成 PowerJob 时最常用的开发方式。不同任务模式对应不同 Processor 接口，常见关系如下：

| 任务类型       | 推荐接口             | 说明                       |
| -------------- | -------------------- | -------------------------- |
| 单机任务       | `BasicProcessor`     | 一个 Worker 执行一次       |
| 广播任务       | `BroadcastProcessor` | 所有 Worker 各执行一次     |
| Map 任务       | `MapProcessor`       | 根任务拆分子任务并分发执行 |
| MapReduce 任务 | `MapReduceProcessor` | Map 后增加 Reduce 汇总阶段 |

Java 处理器建议遵循以下开发规范：

```text
1. Processor 只作为任务入口，不承载过重业务逻辑
2. 复杂业务逻辑下沉到 Service 层
3. 任务参数必须校验，避免空参数或非法参数直接进入业务逻辑
4. 所有外部调用、数据库写入、状态变更都要考虑幂等
5. 返回 ProcessResult 时保持结果简短、明确
6. 关键流程打印中文业务日志，便于排查任务执行问题
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/SystemHealthCheckProcessor.java`

下面的处理器用于执行系统健康检查，展示 Java 处理器中如何进行多分支校验和结果返回。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 系统健康检查任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class SystemHealthCheckProcessor implements BasicProcessor {

    /**
     * 执行系统健康检查任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String checkType = StrUtil.blankToDefault(context.getJobParams(), "all");
        log.info("开始执行系统健康检查任务，instanceId={}，checkType={}", context.getInstanceId(), checkType);

        boolean databaseHealthy = checkDatabase();
        boolean cacheHealthy = checkCache();
        boolean remoteApiHealthy = checkRemoteApi();

        if ("database".equalsIgnoreCase(checkType)) {
            return buildResult(databaseHealthy, "数据库健康检查");
        }

        if ("cache".equalsIgnoreCase(checkType)) {
            return buildResult(cacheHealthy, "缓存健康检查");
        }

        if ("remoteApi".equalsIgnoreCase(checkType)) {
            return buildResult(remoteApiHealthy, "远程接口健康检查");
        }

        boolean success = databaseHealthy && cacheHealthy && remoteApiHealthy;
        String message = StrUtil.format("系统健康检查完成，database={}，cache={}，remoteApi={}",
                databaseHealthy, cacheHealthy, remoteApiHealthy);

        log.info(message);
        return new ProcessResult(success, message);
    }

    /**
     * 构建检查结果。
     *
     * @param success 是否成功
     * @param name    检查名称
     * @return 执行结果
     */
    private ProcessResult buildResult(boolean success, String name) {
        String message = StrUtil.format("{}{}", name, success ? "通过" : "失败");
        log.info(message);
        return new ProcessResult(success, message);
    }

    /**
     * 检查数据库状态。
     *
     * @return 是否健康
     */
    private boolean checkDatabase() {
        log.info("执行数据库健康检查");
        return true;
    }

    /**
     * 检查缓存状态。
     *
     * @return 是否健康
     */
    private boolean checkCache() {
        log.info("执行缓存健康检查");
        return true;
    }

    /**
     * 检查远程接口状态。
     *
     * @return 是否健康
     */
    private boolean checkRemoteApi() {
        log.info("执行远程接口健康检查");
        return true;
    }

}
```

### Spring Bean 方式接入

Spring Bean 方式是 Spring Boot 项目中推荐的接入方式。处理器由 Spring 容器管理，可以直接注入 Service、Mapper、RedisTemplate、RestTemplate、FeignClient、配置类等业务组件。公开资料也建议在宿主应用支持 Spring 时优先使用 Spring Bean 处理器，只需将 Processor 注册到 Spring IOC 容器即可使用 Spring 能力。([阿里云开发者社区](https://developer.aliyun.com/article/1680726?utm_source=chatgpt.com))

推荐结构如下：

```text
src/main/java/io/github/atengk/powerjob
├── processor
│   └── ReportGenerateProcessor.java
├── service
│   ├── ReportService.java
│   └── impl
│       └── ReportServiceImpl.java
└── dto
    └── ReportGenerateParam.java
```

文件位置：`src/main/java/io/github/atengk/powerjob/dto/ReportGenerateParam.java`

下面的 DTO 用于接收报表生成任务参数。

```java
package io.github.atengk.powerjob.dto;

import lombok.Data;

/**
 * 报表生成任务参数。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class ReportGenerateParam {

    /**
     * 报表日期，格式 yyyy-MM-dd。
     */
    private String reportDate;

    /**
     * 报表类型，例如 daily、weekly、monthly。
     */
    private String reportType;

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/service/ReportService.java`

下面的接口定义报表生成业务能力。

```java
package io.github.atengk.powerjob.service;

import io.github.atengk.powerjob.dto.ReportGenerateParam;

/**
 * 报表业务服务。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface ReportService {

    /**
     * 生成报表。
     *
     * @param param 报表生成参数
     * @return 生成结果
     */
    String generateReport(ReportGenerateParam param);

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/service/impl/ReportServiceImpl.java`

下面的实现类承载真实业务逻辑，Processor 只负责调度入口控制。

```java
package io.github.atengk.powerjob.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.powerjob.dto.ReportGenerateParam;
import io.github.atengk.powerjob.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 报表业务服务实现。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    /**
     * 生成报表。
     *
     * @param param 报表生成参数
     * @return 生成结果
     */
    @Override
    public String generateReport(ReportGenerateParam param) {
        log.info("开始生成业务报表，reportDate={}，reportType={}", param.getReportDate(), param.getReportType());

        // 示例逻辑：实际项目中可查询数据库、生成 Excel、上传 MinIO、写入报表记录
        String fileName = StrUtil.format("{}-report-{}.xlsx", param.getReportType(), param.getReportDate());

        String result = StrUtil.format("报表生成成功，fileName={}，finishTime={}", fileName, DateUtil.now());
        log.info(result);
        return result;
    }

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/ReportGenerateProcessor.java`

下面的 Processor 通过构造器注入 `ReportService`，体现 Spring Bean 接入方式。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.powerjob.dto.ReportGenerateParam;
import io.github.atengk.powerjob.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 报表生成任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGenerateProcessor implements BasicProcessor {

    private final ReportService reportService;

    /**
     * 执行报表生成任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String jobParams = context.getJobParams();
        log.info("开始执行报表生成任务，instanceId={}，jobParams={}", context.getInstanceId(), jobParams);

        try {
            if (StrUtil.isBlank(jobParams)) {
                return new ProcessResult(false, "报表生成任务参数不能为空");
            }

            ReportGenerateParam param = JSONUtil.toBean(jobParams, ReportGenerateParam.class);
            if (StrUtil.isBlank(param.getReportDate()) || StrUtil.isBlank(param.getReportType())) {
                return new ProcessResult(false, "reportDate 和 reportType 不能为空");
            }

            String result = reportService.generateReport(param);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("报表生成任务执行异常，instanceId={}", context.getInstanceId(), e);
            return new ProcessResult(false, "报表生成任务执行异常：" + e.getMessage());
        }
    }

}
```

PowerJob 控制台任务参数示例：

```json
{
  "reportDate": "2026-05-07",
  "reportType": "daily"
}
```

### 参数接收与解析

任务参数通常通过 PowerJob 控制台配置，并在 Worker 执行时通过 `TaskContext` 获取。常见参数形式包括普通字符串、数字、JSON 对象和 JSON 数组。实际项目中推荐使用 JSON 对象作为任务参数，便于扩展字段和校验。

参数格式建议如下：

| 参数类型   | 示例                | 适用场景                    |
| ---------- | ------------------- | --------------------------- |
| 普通字符串 | `userCache`         | 简单缓存名称、业务标识      |
| 数字       | `100`               | 批量大小、最大 ID、间隔数量 |
| JSON 对象  | `{"batchSize":100}` | 推荐，适合多数业务任务      |
| JSON 数组  | `[1,2,3]`           | 指定 ID 列表、批量对象列表  |

文件位置：`src/main/java/io/github/atengk/powerjob/util/PowerJobParamUtils.java`

下面的工具类统一处理任务参数解析，避免每个 Processor 重复编写参数校验逻辑。

```java
package io.github.atengk.powerjob.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

/**
 * PowerJob 任务参数工具类。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class PowerJobParamUtils {

    private PowerJobParamUtils() {
    }

    /**
     * 解析 JSON 任务参数。
     *
     * @param jobParams 任务参数
     * @param clazz     目标类型
     * @param <T>       泛型类型
     * @return 解析结果
     */
    public static <T> T parseJsonObject(String jobParams, Class<T> clazz) {
        if (StrUtil.isBlank(jobParams)) {
            throw new IllegalArgumentException("任务参数不能为空");
        }

        if (!JSONUtil.isTypeJSONObject(jobParams)) {
            throw new IllegalArgumentException("任务参数必须是 JSON 对象");
        }

        return JSONUtil.toBean(jobParams, clazz);
    }

    /**
     * 获取字符串参数。
     *
     * @param jobParams     任务参数
     * @param defaultValue  默认值
     * @return 字符串参数
     */
    public static String getString(String jobParams, String defaultValue) {
        return StrUtil.blankToDefault(jobParams, defaultValue);
    }

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/dto/DataCleanParam.java`

下面的 DTO 用于接收数据清理任务参数。

```java
package io.github.atengk.powerjob.dto;

import lombok.Data;

/**
 * 数据清理任务参数。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class DataCleanParam {

    /**
     * 保留天数。
     */
    private Integer retainDays;

    /**
     * 单次处理数量。
     */
    private Integer batchSize;

    /**
     * 是否试运行。
     */
    private Boolean dryRun;

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/DataCleanProcessor.java`

下面的处理器展示如何使用统一工具类解析 JSON 参数。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.powerjob.dto.DataCleanParam;
import io.github.atengk.powerjob.util.PowerJobParamUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 数据清理任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class DataCleanProcessor implements BasicProcessor {

    /**
     * 执行数据清理任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        try {
            DataCleanParam param = PowerJobParamUtils.parseJsonObject(context.getJobParams(), DataCleanParam.class);

            if (param.getRetainDays() == null || param.getRetainDays() < 1) {
                return new ProcessResult(false, "retainDays 必须大于 0");
            }

            int batchSize = param.getBatchSize() == null ? 100 : Math.min(param.getBatchSize(), 1000);
            boolean dryRun = BooleanUtil.isTrue(param.getDryRun());

            log.info("开始执行数据清理任务，retainDays={}，batchSize={}，dryRun={}",
                    param.getRetainDays(), batchSize, dryRun);

            int cleanCount = cleanHistoryData(param.getRetainDays(), batchSize, dryRun);

            String result = StrUtil.format("数据清理任务执行完成，清理数量={}，dryRun={}", cleanCount, dryRun);
            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("数据清理任务执行异常，instanceId={}", context.getInstanceId(), e);
            return new ProcessResult(false, "数据清理任务执行异常：" + e.getMessage());
        }
    }

    /**
     * 清理历史数据。
     *
     * @param retainDays 保留天数
     * @param batchSize  单次处理数量
     * @param dryRun     是否试运行
     * @return 清理数量
     */
    private int cleanHistoryData(Integer retainDays, int batchSize, boolean dryRun) {
        if (dryRun) {
            log.info("当前为试运行模式，仅统计可清理数据，不执行删除");
            return batchSize;
        }

        log.info("执行历史数据清理，retainDays={}，batchSize={}", retainDays, batchSize);
        return batchSize;
    }

}
```

PowerJob 控制台任务参数示例：

```json
{
  "retainDays": 30,
  "batchSize": 200,
  "dryRun": false
}
```

参数接收与解析建议：

```text
1. 简单任务可以使用字符串参数，但中大型项目建议统一使用 JSON 对象
2. Processor 中必须校验必填参数，不能完全信任控制台输入
3. 参数默认值应在代码中明确处理，避免空指针异常
4. 时间范围、批量大小、开关参数需要设置上限和默认值
5. 参数解析失败时返回失败结果，并记录清晰日志
6. 执行结果只返回摘要，不返回大量明细数据
```

至此，任务开发和处理器实现部分已经覆盖常见任务形态。后续章节可以继续补充 `任务调度配置`，包括固定频率任务、Cron 表达式任务、延迟任务和手动触发任务。



## 任务调度配置

本节用于说明 PowerJob 常见调度方式的配置思路，包括固定频率、Cron 表达式、延迟任务和手动触发任务。PowerJob 支持 CRON、固定频率、固定延迟和 OpenAPI 等调度策略，执行模式支持单机、广播、Map 和 MapReduce，可覆盖常规定时任务、高频轮询任务、人工触发任务和程序化触发任务。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob?utm_source=chatgpt.com))

### 固定频率任务

固定频率任务适合按固定间隔重复执行的场景，例如每 5 秒补偿失败记录、每 1 分钟同步状态、每 10 分钟刷新统计缓存。固定频率更关注“按间隔触发”，需要确保单次执行时间不要明显超过调度间隔，否则容易出现任务堆积或重复处理。

固定频率任务配置示例：

```text
任务名称：失败记录补偿任务
执行模式：单机执行
调度方式：固定频率
固定频率：10 秒
处理器类型：Java / Spring Bean
处理器信息：failedRecordCompensationProcessor
任务参数：{"batchSize":50,"maxRetryCount":3}
```

固定频率任务适合执行轻量任务。任务代码中应控制批量大小，并且业务逻辑必须具备幂等性。

文件位置：`src/main/java/io/github/atengk/powerjob/dto/FailedRecordCompensationParam.java`

下面的 DTO 用于接收固定频率补偿任务参数。

```java
package io.github.atengk.powerjob.dto;

import lombok.Data;

/**
 * 失败记录补偿任务参数。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class FailedRecordCompensationParam {

    /**
     * 单次处理数量。
     */
    private Integer batchSize;

    /**
     * 最大重试次数。
     */
    private Integer maxRetryCount;

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/FailedRecordCompensationProcessor.java`

下面的处理器用于固定频率补偿失败记录，每次只处理小批量数据。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.powerjob.dto.FailedRecordCompensationParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 失败记录补偿任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class FailedRecordCompensationProcessor implements BasicProcessor {

    /**
     * 执行失败记录补偿任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        long startTime = System.currentTimeMillis();
        String jobParams = context.getJobParams();

        try {
            FailedRecordCompensationParam param = buildParam(jobParams);
            int batchSize = Math.min(param.getBatchSize(), 200);

            log.info("开始执行失败记录补偿任务，instanceId={}，batchSize={}，maxRetryCount={}",
                    context.getInstanceId(), batchSize, param.getMaxRetryCount());

            int successCount = compensateFailedRecords(batchSize, param.getMaxRetryCount());
            long cost = System.currentTimeMillis() - startTime;

            String result = StrUtil.format("失败记录补偿完成，成功数量={}，耗时={}ms", successCount, cost);
            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("失败记录补偿任务执行异常，instanceId={}，jobParams={}",
                    context.getInstanceId(), jobParams, e);
            return new ProcessResult(false, "失败记录补偿任务执行异常：" + e.getMessage());
        }
    }

    /**
     * 构建任务参数。
     *
     * @param jobParams 任务参数
     * @return 任务参数对象
     */
    private FailedRecordCompensationParam buildParam(String jobParams) {
        FailedRecordCompensationParam param;

        if (StrUtil.isBlank(jobParams)) {
            param = new FailedRecordCompensationParam();
            param.setBatchSize(50);
            param.setMaxRetryCount(3);
            return param;
        }

        if (JSONUtil.isTypeJSONObject(jobParams)) {
            param = JSONUtil.toBean(jobParams, FailedRecordCompensationParam.class);
        } else if (NumberUtil.isInteger(jobParams)) {
            param = new FailedRecordCompensationParam();
            param.setBatchSize(Integer.parseInt(jobParams));
            param.setMaxRetryCount(3);
        } else {
            throw new IllegalArgumentException("任务参数格式不正确");
        }

        if (param.getBatchSize() == null || param.getBatchSize() <= 0) {
            param.setBatchSize(50);
        }

        if (param.getMaxRetryCount() == null || param.getMaxRetryCount() <= 0) {
            param.setMaxRetryCount(3);
        }

        return param;
    }

    /**
     * 补偿失败记录。
     *
     * @param batchSize     单次处理数量
     * @param maxRetryCount 最大重试次数
     * @return 补偿成功数量
     */
    private int compensateFailedRecords(int batchSize, int maxRetryCount) {
        log.info("查询待补偿失败记录，batchSize={}，maxRetryCount={}", batchSize, maxRetryCount);

        // 示例逻辑：实际项目中应查询 retry_count < maxRetryCount 且状态为失败的数据
        // 更新时建议使用状态机或唯一业务键保证幂等
        return batchSize;
    }

}
```

固定频率任务配置建议如下：

| 配置项       | 建议                                   |
| ------------ | -------------------------------------- |
| 调度间隔     | 应大于任务平均执行耗时                 |
| 单次处理数量 | 建议设置上限，例如 50、100、200        |
| 失败重试     | 不建议无限重试，应结合业务重试次数控制 |
| 幂等控制     | 必须基于业务唯一键、状态机或版本号控制 |
| 执行结果     | 只返回摘要，不返回完整明细             |

### Cron 表达式任务

Cron 表达式任务适合按固定时间点执行的任务，例如每天凌晨生成报表、每小时同步数据、每月初结算账单。PowerJob 支持 CRON 表达式调度，适合对执行时间有明确要求的业务任务。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob?utm_source=chatgpt.com))

Cron 表达式任务配置示例：

```text
任务名称：每日销售报表生成任务
执行模式：单机执行
调度方式：CRON
CRON 表达式：0 0 2 * * ?
处理器类型：Java / Spring Bean
处理器信息：dailySalesReportProcessor
任务参数：{"reportType":"daily","offsetDays":1}
```

常用 Cron 表达式示例：

| 表达式          | 说明                   |
| --------------- | ---------------------- |
| `0 0/5 * * * ?` | 每 5 分钟执行一次      |
| `0 0 2 * * ?`   | 每天凌晨 2 点执行      |
| `0 0 1 * * ?`   | 每天凌晨 1 点执行      |
| `0 0 3 ? * MON` | 每周一凌晨 3 点执行    |
| `0 0 4 1 * ?`   | 每月 1 日凌晨 4 点执行 |

文件位置：`src/main/java/io/github/atengk/powerjob/dto/DailyReportParam.java`

下面的 DTO 用于接收日报生成任务参数。

```java
package io.github.atengk.powerjob.dto;

import lombok.Data;

/**
 * 日报生成任务参数。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class DailyReportParam {

    /**
     * 报表类型。
     */
    private String reportType;

    /**
     * 日期偏移量，例如 1 表示生成昨天的数据。
     */
    private Integer offsetDays;

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/DailySalesReportProcessor.java`

下面的处理器用于根据 Cron 调度生成每日销售报表。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.powerjob.dto.DailyReportParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

import java.util.Date;

/**
 * 每日销售报表任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class DailySalesReportProcessor implements BasicProcessor {

    /**
     * 执行每日销售报表生成任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String jobParams = context.getJobParams();

        try {
            DailyReportParam param = parseParam(jobParams);
            int offsetDays = param.getOffsetDays() == null ? 1 : param.getOffsetDays();

            Date reportDate = DateUtil.offsetDay(new Date(), -offsetDays);
            String reportDateText = DateUtil.formatDate(reportDate);

            log.info("开始生成每日销售报表，instanceId={}，reportType={}，reportDate={}",
                    context.getInstanceId(), param.getReportType(), reportDateText);

            String fileName = generateReport(param.getReportType(), reportDateText);

            String result = StrUtil.format("每日销售报表生成成功，reportDate={}，fileName={}",
                    reportDateText, fileName);
            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("每日销售报表任务执行异常，instanceId={}，jobParams={}",
                    context.getInstanceId(), jobParams, e);
            return new ProcessResult(false, "每日销售报表任务执行异常：" + e.getMessage());
        }
    }

    /**
     * 解析任务参数。
     *
     * @param jobParams 任务参数
     * @return 任务参数对象
     */
    private DailyReportParam parseParam(String jobParams) {
        DailyReportParam param;

        if (StrUtil.isBlank(jobParams)) {
            param = new DailyReportParam();
            param.setReportType("daily");
            param.setOffsetDays(1);
            return param;
        }

        param = JSONUtil.toBean(jobParams, DailyReportParam.class);

        if (StrUtil.isBlank(param.getReportType())) {
            param.setReportType("daily");
        }

        return param;
    }

    /**
     * 生成报表。
     *
     * @param reportType     报表类型
     * @param reportDateText 报表日期
     * @return 报表文件名
     */
    private String generateReport(String reportType, String reportDateText) {
        log.info("执行报表生成逻辑，reportType={}，reportDate={}", reportType, reportDateText);

        // 示例逻辑：实际项目中可查询销售数据、生成 Excel、上传对象存储
        return StrUtil.format("{}-sales-report-{}.xlsx", reportType, reportDateText);
    }

}
```

Cron 表达式任务注意事项：

```text
1. 生产环境需要统一确认 Server 时区和业务时区。
2. 凌晨批处理任务较多时，应错峰配置，避免数据库瞬时压力过高。
3. 每日任务建议支持日期参数，方便手动补跑历史数据。
4. 月结、日结、账单类任务必须支持幂等执行。
5. Cron 表达式变更后，需要在控制台确认下一次触发时间是否符合预期。
```

### 延迟任务

延迟任务适合“当前不立即执行，而是在指定延迟时间后执行”的场景，例如订单创建 30 分钟后检查支付状态、消息发送失败 5 分钟后补偿、用户注册后延迟触达。PowerJob 的 OpenAPI 调度策略可用于自定义调度策略，例如延迟执行；实际项目中也可以通过业务表保存 `next_execute_time`，再由固定频率任务扫描到期数据执行。([libraries.io](https://libraries.io/maven/tech.powerjob%3Apowerjob-remote-framework?utm_source=chatgpt.com))

延迟任务推荐两种落地方式：

| 方式                  | 说明                                              | 适用场景                   |
| --------------------- | ------------------------------------------------- | -------------------------- |
| PowerJob OpenAPI 触发 | 由外部系统通过接口触发任务，并传入延迟执行策略    | 需要平台化触发能力         |
| 业务表 + 固定频率扫描 | 业务系统保存待执行记录，PowerJob 定时扫描到期数据 | 更容易控制幂等、重试和状态 |

在业务系统中，更推荐使用“业务表 + 固定频率扫描”的方式。这样可以清晰记录每条延迟任务的状态、重试次数、下次执行时间和失败原因。

表结构示例：

```sql
CREATE TABLE delay_task_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    biz_id VARCHAR(128) NOT NULL COMMENT '业务ID',
    task_param TEXT NULL COMMENT '任务参数',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待执行，1执行中，2成功，3失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    next_execute_time DATETIME NOT NULL COMMENT '下次执行时间',
    fail_reason VARCHAR(1000) NULL COMMENT '失败原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_biz_type_biz_id (biz_type, biz_id),
    KEY idx_status_next_time (status, next_execute_time)
) COMMENT='延迟任务记录表';
```

文件位置：`src/main/java/io/github/atengk/powerjob/dto/DelayTaskScanParam.java`

下面的 DTO 用于接收延迟任务扫描参数。

```java
package io.github.atengk.powerjob.dto;

import lombok.Data;

/**
 * 延迟任务扫描参数。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class DelayTaskScanParam {

    /**
     * 单次扫描数量。
     */
    private Integer batchSize;

    /**
     * 业务类型。
     */
    private String bizType;

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/DelayTaskScanProcessor.java`

下面的处理器用于扫描到期的延迟任务，并按业务类型执行处理逻辑。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.powerjob.dto.DelayTaskScanParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 延迟任务扫描处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class DelayTaskScanProcessor implements BasicProcessor {

    /**
     * 执行延迟任务扫描。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String jobParams = context.getJobParams();

        try {
            DelayTaskScanParam param = parseParam(jobParams);
            int batchSize = Math.min(param.getBatchSize(), 200);

            log.info("开始扫描延迟任务，instanceId={}，bizType={}，batchSize={}",
                    context.getInstanceId(), param.getBizType(), batchSize);

            int successCount = executeDueDelayTasks(param.getBizType(), batchSize);

            String result = StrUtil.format("延迟任务扫描完成，成功处理数量={}", successCount);
            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("延迟任务扫描执行异常，instanceId={}，jobParams={}",
                    context.getInstanceId(), jobParams, e);
            return new ProcessResult(false, "延迟任务扫描执行异常：" + e.getMessage());
        }
    }

    /**
     * 解析扫描参数。
     *
     * @param jobParams 任务参数
     * @return 扫描参数
     */
    private DelayTaskScanParam parseParam(String jobParams) {
        DelayTaskScanParam param;

        if (StrUtil.isBlank(jobParams)) {
            param = new DelayTaskScanParam();
            param.setBatchSize(100);
            param.setBizType("ALL");
            return param;
        }

        param = JSONUtil.toBean(jobParams, DelayTaskScanParam.class);

        if (param.getBatchSize() == null || param.getBatchSize() <= 0) {
            param.setBatchSize(100);
        }

        if (StrUtil.isBlank(param.getBizType())) {
            param.setBizType("ALL");
        }

        return param;
    }

    /**
     * 执行到期延迟任务。
     *
     * @param bizType   业务类型
     * @param batchSize 单次处理数量
     * @return 成功数量
     */
    private int executeDueDelayTasks(String bizType, int batchSize) {
        log.info("查询并执行到期延迟任务，bizType={}，batchSize={}", bizType, batchSize);

        // 示例逻辑：实际项目中应按 status=0、next_execute_time<=now 查询
        // 执行前建议先将状态从待执行更新为执行中，避免多 Worker 重复处理
        return batchSize;
    }

}
```

PowerJob 控制台配置示例：

```text
任务名称：延迟任务扫描任务
执行模式：单机执行
调度方式：固定频率
固定频率：10 秒
处理器类型：Java / Spring Bean
处理器信息：delayTaskScanProcessor
任务参数：{"batchSize":100,"bizType":"ORDER_TIMEOUT_CHECK"}
```

延迟任务业务状态建议：

| 状态       | 说明                     |
| ---------- | ------------------------ |
| `0 待执行` | 任务已创建，等待到期执行 |
| `1 执行中` | Worker 已领取，正在执行  |
| `2 成功`   | 任务执行成功             |
| `3 失败`   | 达到最大重试次数后仍失败 |

延迟任务的关键不是“延迟触发”本身，而是状态控制。建议通过唯一键避免重复创建，通过状态流转避免重复执行，通过重试次数避免无限补偿。

### 手动触发任务

手动触发任务适合临时补数据、人工重跑失败任务、验证新处理器、执行一次性运维任务等场景。PowerJob 提供可视化前端页面，可用于任务管理、状态监控和在线日志查看，因此手动触发通常直接在控制台完成。([GitHub](https://github.com/PowerJob/PowerJob?utm_source=chatgpt.com))

手动触发任务配置示例：

```text
任务名称：手动补跑销售报表任务
执行模式：单机执行
调度方式：OpenAPI / 手动触发
处理器类型：Java / Spring Bean
处理器信息：manualReportRerunProcessor
任务参数：{"startDate":"2026-05-01","endDate":"2026-05-07","reportType":"daily"}
```

文件位置：`src/main/java/io/github/atengk/powerjob/dto/ManualReportRerunParam.java`

下面的 DTO 用于接收手动补跑报表参数。

```java
package io.github.atengk.powerjob.dto;

import lombok.Data;

/**
 * 手动补跑报表参数。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class ManualReportRerunParam {

    /**
     * 开始日期，格式 yyyy-MM-dd。
     */
    private String startDate;

    /**
     * 结束日期，格式 yyyy-MM-dd。
     */
    private String endDate;

    /**
     * 报表类型。
     */
    private String reportType;

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/ManualReportRerunProcessor.java`

下面的处理器用于按日期范围手动补跑报表。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.powerjob.dto.ManualReportRerunParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

import java.util.Date;

/**
 * 手动补跑报表任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class ManualReportRerunProcessor implements BasicProcessor {

    /**
     * 执行手动补跑报表任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String jobParams = context.getJobParams();

        try {
            if (StrUtil.isBlank(jobParams)) {
                return new ProcessResult(false, "手动补跑参数不能为空");
            }

            ManualReportRerunParam param = JSONUtil.toBean(jobParams, ManualReportRerunParam.class);
            validateParam(param);

            Date startDate = DateUtil.parseDate(param.getStartDate());
            Date endDate = DateUtil.parseDate(param.getEndDate());
            DateRange dateRange = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);

            int successCount = 0;
            for (Date date : dateRange) {
                String reportDate = DateUtil.formatDate(date);
                log.info("开始补跑报表，reportDate={}，reportType={}", reportDate, param.getReportType());

                // 示例逻辑：实际项目中应调用报表 Service，并通过唯一键保证重复补跑可覆盖或跳过
                successCount++;
            }

            String result = StrUtil.format("手动补跑报表完成，日期范围={} 至 {}，成功数量={}",
                    param.getStartDate(), param.getEndDate(), successCount);
            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("手动补跑报表任务执行异常，instanceId={}，jobParams={}",
                    context.getInstanceId(), jobParams, e);
            return new ProcessResult(false, "手动补跑报表任务执行异常：" + e.getMessage());
        }
    }

    /**
     * 校验任务参数。
     *
     * @param param 任务参数
     */
    private void validateParam(ManualReportRerunParam param) {
        if (StrUtil.isBlank(param.getStartDate())) {
            throw new IllegalArgumentException("startDate 不能为空");
        }

        if (StrUtil.isBlank(param.getEndDate())) {
            throw new IllegalArgumentException("endDate 不能为空");
        }

        if (StrUtil.isBlank(param.getReportType())) {
            throw new IllegalArgumentException("reportType 不能为空");
        }

        Date startDate = DateUtil.parseDate(param.getStartDate());
        Date endDate = DateUtil.parseDate(param.getEndDate());

        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("startDate 不能大于 endDate");
        }

        long betweenDay = DateUtil.betweenDay(startDate, endDate, true);
        if (betweenDay > 31) {
            throw new IllegalArgumentException("单次补跑日期范围不能超过 31 天");
        }
    }

}
```

手动触发任务注意事项：

```text
1. 手动任务必须限制参数范围，例如日期范围、批量大小、业务类型。
2. 手动任务要打印清晰日志，便于审计谁触发了什么业务处理。
3. 补跑类任务必须支持幂等，避免重复生成、重复扣款、重复通知。
4. 高风险任务建议先支持 dryRun 参数，先预览影响范围再执行。
5. 生产环境不建议开放无边界的手动任务。
```

## 业务集成实践

本节用于说明 PowerJob 在业务系统中的典型集成方式。实际项目中，Processor 不建议直接堆积复杂业务逻辑，而应作为任务入口，参数校验后调用 Service 层完成业务处理。这样既方便单元测试，也方便任务逻辑被 HTTP 接口、消息消费或其他入口复用。

### 数据同步任务

数据同步任务适合从第三方系统、其他业务库、数据中台或外部接口中拉取数据，再写入当前系统。数据同步任务通常使用 Cron 或固定频率调度，核心要求是分页处理、断点续传、幂等写入和失败重试。

数据同步任务推荐流程：

```text
1. PowerJob 按固定频率或 Cron 触发同步任务
2. Processor 解析同步任务参数
3. Service 查询同步游标或时间窗口
4. 分页拉取外部数据
5. 根据业务唯一键执行新增或更新
6. 更新同步游标
7. 返回同步数量和耗时摘要
```

文件位置：`src/main/java/io/github/atengk/powerjob/dto/DataSyncJobParam.java`

下面的 DTO 用于接收数据同步任务参数。

```java
package io.github.atengk.powerjob.dto;

import lombok.Data;

/**
 * 数据同步任务参数。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class DataSyncJobParam {

    /**
     * 同步类型，例如 order、user、product。
     */
    private String syncType;

    /**
     * 单页数量。
     */
    private Integer pageSize;

    /**
     * 是否全量同步。
     */
    private Boolean fullSync;

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/service/DataSyncJobService.java`

下面的接口定义数据同步业务能力。

```java
package io.github.atengk.powerjob.service;

import io.github.atengk.powerjob.dto.DataSyncJobParam;

/**
 * 数据同步任务业务服务。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface DataSyncJobService {

    /**
     * 执行数据同步。
     *
     * @param param 同步参数
     * @return 同步结果
     */
    String sync(DataSyncJobParam param);

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/service/impl/DataSyncJobServiceImpl.java`

下面的实现类演示分页同步、数量统计和日志输出。

```java
package io.github.atengk.powerjob.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.powerjob.dto.DataSyncJobParam;
import io.github.atengk.powerjob.service.DataSyncJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 数据同步任务业务服务实现。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
public class DataSyncJobServiceImpl implements DataSyncJobService {

    /**
     * 执行数据同步。
     *
     * @param param 同步参数
     * @return 同步结果
     */
    @Override
    public String sync(DataSyncJobParam param) {
        int pageSize = param.getPageSize() == null ? 100 : Math.min(param.getPageSize(), 500);
        boolean fullSync = BooleanUtil.isTrue(param.getFullSync());

        log.info("开始执行数据同步，syncType={}，pageSize={}，fullSync={}",
                param.getSyncType(), pageSize, fullSync);

        int pageNo = 1;
        int totalCount = 0;

        while (pageNo <= 3) {
            log.info("拉取外部数据，syncType={}，pageNo={}，pageSize={}", param.getSyncType(), pageNo, pageSize);

            // 示例逻辑：实际项目中调用外部接口或查询外部数据库
            int currentCount = pageSize;
            totalCount += currentCount;

            // 示例逻辑：实际项目中根据业务唯一键执行新增或更新
            log.info("写入同步数据完成，pageNo={}，currentCount={}", pageNo, currentCount);

            pageNo++;
        }

        String result = StrUtil.format("数据同步完成，syncType={}，totalCount={}", param.getSyncType(), totalCount);
        log.info(result);
        return result;
    }

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/DataSyncJobProcessor.java`

下面的 Processor 负责接收 PowerJob 任务参数并调用数据同步服务。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.powerjob.dto.DataSyncJobParam;
import io.github.atengk.powerjob.service.DataSyncJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 数据同步任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncJobProcessor implements BasicProcessor {

    private final DataSyncJobService dataSyncJobService;

    /**
     * 执行数据同步任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String jobParams = context.getJobParams();

        try {
            if (StrUtil.isBlank(jobParams)) {
                return new ProcessResult(false, "数据同步任务参数不能为空");
            }

            DataSyncJobParam param = JSONUtil.toBean(jobParams, DataSyncJobParam.class);
            if (StrUtil.isBlank(param.getSyncType())) {
                return new ProcessResult(false, "syncType 不能为空");
            }

            String result = dataSyncJobService.sync(param);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("数据同步任务执行异常，instanceId={}，jobParams={}",
                    context.getInstanceId(), jobParams, e);
            return new ProcessResult(false, "数据同步任务执行异常：" + e.getMessage());
        }
    }

}
```

PowerJob 控制台参数示例：

```json
{
  "syncType": "order",
  "pageSize": 100,
  "fullSync": false
}
```

### 定时清理任务

定时清理任务适合清理历史日志、临时文件、过期业务数据和中间表数据。此类任务通常在凌晨低峰期执行，推荐使用 Cron 表达式调度。清理任务必须设置批量上限和保留天数，避免误删和长事务。

定时清理任务推荐流程：

```text
1. PowerJob 在低峰期触发清理任务
2. Processor 校验 retainDays、batchSize、dryRun 参数
3. Service 查询可清理数据数量
4. dryRun=true 时只输出影响范围
5. dryRun=false 时分批删除或归档
6. 返回清理数量摘要
```

文件位置：`src/main/java/io/github/atengk/powerjob/dto/HistoryCleanJobParam.java`

下面的 DTO 用于接收历史数据清理参数。

```java
package io.github.atengk.powerjob.dto;

import lombok.Data;

/**
 * 历史数据清理任务参数。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class HistoryCleanJobParam {

    /**
     * 保留天数。
     */
    private Integer retainDays;

    /**
     * 单次清理数量。
     */
    private Integer batchSize;

    /**
     * 是否试运行。
     */
    private Boolean dryRun;

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/HistoryCleanJobProcessor.java`

下面的处理器用于定时清理历史数据，并支持试运行模式。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.powerjob.dto.HistoryCleanJobParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

import java.util.Date;

/**
 * 历史数据清理任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class HistoryCleanJobProcessor implements BasicProcessor {

    /**
     * 执行历史数据清理任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String jobParams = context.getJobParams();

        try {
            HistoryCleanJobParam param = JSONUtil.toBean(jobParams, HistoryCleanJobParam.class);
            validateParam(param);

            int batchSize = Math.min(param.getBatchSize(), 1000);
            boolean dryRun = BooleanUtil.isTrue(param.getDryRun());
            Date cleanBeforeTime = DateUtil.offsetDay(new Date(), -param.getRetainDays());

            log.info("开始执行历史数据清理，retainDays={}，batchSize={}，dryRun={}，cleanBeforeTime={}",
                    param.getRetainDays(), batchSize, dryRun, DateUtil.formatDateTime(cleanBeforeTime));

            int cleanCount = cleanHistoryData(cleanBeforeTime, batchSize, dryRun);

            String result = StrUtil.format("历史数据清理完成，cleanCount={}，dryRun={}", cleanCount, dryRun);
            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("历史数据清理任务执行异常，instanceId={}，jobParams={}",
                    context.getInstanceId(), jobParams, e);
            return new ProcessResult(false, "历史数据清理任务执行异常：" + e.getMessage());
        }
    }

    /**
     * 校验清理参数。
     *
     * @param param 清理参数
     */
    private void validateParam(HistoryCleanJobParam param) {
        if (param.getRetainDays() == null || param.getRetainDays() < 7) {
            throw new IllegalArgumentException("retainDays 不能小于 7");
        }

        if (param.getBatchSize() == null || param.getBatchSize() <= 0) {
            throw new IllegalArgumentException("batchSize 必须大于 0");
        }
    }

    /**
     * 清理历史数据。
     *
     * @param cleanBeforeTime 清理时间边界
     * @param batchSize       单次清理数量
     * @param dryRun          是否试运行
     * @return 清理数量
     */
    private int cleanHistoryData(Date cleanBeforeTime, int batchSize, boolean dryRun) {
        if (dryRun) {
            log.info("试运行模式，仅统计历史数据，cleanBeforeTime={}，batchSize={}",
                    DateUtil.formatDateTime(cleanBeforeTime), batchSize);
            return batchSize;
        }

        log.info("执行历史数据删除，cleanBeforeTime={}，batchSize={}",
                DateUtil.formatDateTime(cleanBeforeTime), batchSize);

        // 示例逻辑：实际项目中应使用 limit 分批删除，避免一次删除过多数据
        return batchSize;
    }

}
```

PowerJob 控制台参数示例：

```json
{
  "retainDays": 30,
  "batchSize": 500,
  "dryRun": false
}
```

清理任务注意事项：

```text
1. 必须设置 retainDays 下限，避免误删近期数据。
2. 必须设置 batchSize 上限，避免长事务和锁表。
3. 高风险清理任务建议先 dryRun，再正式执行。
4. 删除前可先归档，尤其是审计类、账务类、订单类数据。
5. 清理结果应输出清理数量、时间边界和执行模式。
```

### 异步补偿任务

异步补偿任务适合处理接口调用失败、消息发送失败、回调通知失败、状态同步失败等场景。补偿任务通常按固定频率执行，通过状态、重试次数和下次执行时间筛选待补偿数据。

异步补偿任务推荐流程：

```text
1. 业务失败时写入补偿记录
2. PowerJob 固定频率触发补偿任务
3. Processor 查询待补偿数据
4. Service 按业务类型执行补偿
5. 成功则更新为成功
6. 失败则增加 retry_count 并设置 next_retry_time
7. 超过最大重试次数后标记最终失败
```

补偿记录表结构示例：

```sql
CREATE TABLE async_compensation_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    biz_id VARCHAR(128) NOT NULL COMMENT '业务ID',
    request_body TEXT NULL COMMENT '请求参数',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待补偿，1补偿中，2成功，3最终失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry_count INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    next_retry_time DATETIME NOT NULL COMMENT '下次重试时间',
    fail_reason VARCHAR(1000) NULL COMMENT '失败原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_biz_type_biz_id (biz_type, biz_id),
    KEY idx_status_next_retry_time (status, next_retry_time)
) COMMENT='异步补偿记录表';
```

文件位置：`src/main/java/io/github/atengk/powerjob/dto/AsyncCompensationParam.java`

下面的 DTO 用于接收异步补偿任务参数。

```java
package io.github.atengk.powerjob.dto;

import lombok.Data;

/**
 * 异步补偿任务参数。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class AsyncCompensationParam {

    /**
     * 业务类型。
     */
    private String bizType;

    /**
     * 单次处理数量。
     */
    private Integer batchSize;

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/AsyncCompensationProcessor.java`

下面的处理器用于执行异步补偿任务。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.powerjob.dto.AsyncCompensationParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 异步补偿任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class AsyncCompensationProcessor implements BasicProcessor {

    /**
     * 执行异步补偿任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        String jobParams = context.getJobParams();

        try {
            AsyncCompensationParam param = parseParam(jobParams);
            int batchSize = Math.min(param.getBatchSize(), 200);

            log.info("开始执行异步补偿任务，instanceId={}，bizType={}，batchSize={}",
                    context.getInstanceId(), param.getBizType(), batchSize);

            int successCount = compensate(param.getBizType(), batchSize);

            String result = StrUtil.format("异步补偿任务执行完成，bizType={}，successCount={}",
                    param.getBizType(), successCount);
            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("异步补偿任务执行异常，instanceId={}，jobParams={}",
                    context.getInstanceId(), jobParams, e);
            return new ProcessResult(false, "异步补偿任务执行异常：" + e.getMessage());
        }
    }

    /**
     * 解析任务参数。
     *
     * @param jobParams 任务参数
     * @return 补偿参数
     */
    private AsyncCompensationParam parseParam(String jobParams) {
        AsyncCompensationParam param;

        if (StrUtil.isBlank(jobParams)) {
            param = new AsyncCompensationParam();
            param.setBizType("ALL");
            param.setBatchSize(100);
            return param;
        }

        param = JSONUtil.toBean(jobParams, AsyncCompensationParam.class);

        if (StrUtil.isBlank(param.getBizType())) {
            param.setBizType("ALL");
        }

        if (param.getBatchSize() == null || param.getBatchSize() <= 0) {
            param.setBatchSize(100);
        }

        return param;
    }

    /**
     * 执行业务补偿。
     *
     * @param bizType   业务类型
     * @param batchSize 单次处理数量
     * @return 成功数量
     */
    private int compensate(String bizType, int batchSize) {
        log.info("查询待补偿记录，bizType={}，batchSize={}", bizType, batchSize);

        // 示例逻辑：实际项目中应先将待补偿记录更新为补偿中
        // 调用外部接口成功后更新为成功，失败后增加重试次数并设置下一次重试时间
        return batchSize;
    }

}
```

异步补偿任务的核心是“可恢复”。业务失败时必须有补偿记录，补偿失败时必须有失败原因和下次重试时间，达到最大重试次数后必须进入最终失败状态，不能无限重试。

### 批量处理任务

批量处理任务适合处理数据量较大的业务，例如批量生成用户画像、批量修复历史数据、批量重算统计结果、批量发送通知等。数据量较小时可以使用单机任务分批处理，数据量较大时建议使用 MapReduce 模式拆分执行。PowerJob 支持 Map 和 MapReduce 执行模式，适合将大任务拆分到多个 Worker 并行执行。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob?utm_source=chatgpt.com))

批量处理任务推荐流程：

```text
1. 根任务根据 ID 范围、时间范围或分页信息拆分子任务
2. 子任务在不同 Worker 上并行执行
3. 每个子任务只处理自己的数据范围
4. 子任务返回简短处理结果
5. Reduce 阶段汇总成功数、失败数和异常摘要
```

文件位置：`src/main/java/io/github/atengk/powerjob/dto/BatchRepairParam.java`

下面的 DTO 用于接收批量修复任务参数。

```java
package io.github.atengk.powerjob.dto;

import lombok.Data;

/**
 * 批量修复任务参数。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class BatchRepairParam {

    /**
     * 开始 ID。
     */
    private Long startId;

    /**
     * 结束 ID。
     */
    private Long endId;

    /**
     * 分片大小。
     */
    private Integer shardSize;

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/BatchRepairMapReduceProcessor.java`

下面的处理器用于将批量修复任务拆分为多个分片执行，并在 Reduce 阶段汇总结果。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.powerjob.dto.BatchRepairParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.core.processor.sdk.MapReduceProcessor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量修复 MapReduce 任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class BatchRepairMapReduceProcessor implements MapReduceProcessor {

    /**
     * 执行批量修复任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        try {
            if (isRootTask()) {
                BatchRepairParam param = parseParam(context.getJobParams());
                List<BatchRepairSubTask> subTasks = buildSubTasks(param);

                if (CollUtil.isEmpty(subTasks)) {
                    return new ProcessResult(false, "未生成批量修复子任务");
                }

                log.info("批量修复根任务拆分完成，instanceId={}，subTaskCount={}",
                        context.getInstanceId(), subTasks.size());

                return map(subTasks, "BATCH_REPAIR_SUB_TASK");
            }

            BatchRepairSubTask subTask = (BatchRepairSubTask) context.getSubTask();
            if (subTask == null) {
                return new ProcessResult(false, "批量修复子任务为空");
            }

            int repairCount = repairData(subTask);

            String result = StrUtil.format("批量修复分片完成，startId={}，endId={}，repairCount={}",
                    subTask.getStartId(), subTask.getEndId(), repairCount);
            log.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            log.error("批量修复任务执行异常，instanceId={}", context.getInstanceId(), e);
            return new ProcessResult(false, "批量修复任务执行异常：" + e.getMessage());
        }
    }

    /**
     * 汇总批量修复子任务结果。
     *
     * @param context     任务上下文
     * @param taskResults 子任务执行结果
     * @return 汇总结果
     */
    @Override
    public ProcessResult reduce(TaskContext context, List<TaskResult> taskResults) {
        if (CollUtil.isEmpty(taskResults)) {
            return new ProcessResult(false, "未收到批量修复子任务结果");
        }

        long successCount = taskResults.stream().filter(TaskResult::isSuccess).count();
        long failedCount = taskResults.size() - successCount;

        String result = StrUtil.format("批量修复任务完成，分片总数={}，成功={}，失败={}",
                taskResults.size(), successCount, failedCount);

        log.info(result);
        return new ProcessResult(failedCount == 0, result);
    }

    /**
     * 解析任务参数。
     *
     * @param jobParams 任务参数
     * @return 批量修复参数
     */
    private BatchRepairParam parseParam(String jobParams) {
        if (StrUtil.isBlank(jobParams)) {
            throw new IllegalArgumentException("批量修复任务参数不能为空");
        }

        BatchRepairParam param = JSONUtil.toBean(jobParams, BatchRepairParam.class);

        if (param.getStartId() == null || param.getEndId() == null) {
            throw new IllegalArgumentException("startId 和 endId 不能为空");
        }

        if (param.getStartId() > param.getEndId()) {
            throw new IllegalArgumentException("startId 不能大于 endId");
        }

        if (param.getShardSize() == null || param.getShardSize() <= 0) {
            param.setShardSize(1000);
        }

        return param;
    }

    /**
     * 构建子任务。
     *
     * @param param 批量修复参数
     * @return 子任务列表
     */
    private List<BatchRepairSubTask> buildSubTasks(BatchRepairParam param) {
        List<BatchRepairSubTask> subTasks = new ArrayList<>();

        long startId = param.getStartId();
        while (startId <= param.getEndId()) {
            long endId = Math.min(startId + param.getShardSize() - 1, param.getEndId());
            subTasks.add(new BatchRepairSubTask(startId, endId));
            startId = endId + 1;
        }

        return subTasks;
    }

    /**
     * 修复分片数据。
     *
     * @param subTask 子任务
     * @return 修复数量
     */
    private int repairData(BatchRepairSubTask subTask) {
        log.info("开始修复分片数据，startId={}，endId={}", subTask.getStartId(), subTask.getEndId());

        // 示例逻辑：实际项目中应按 ID 范围查询并修复数据
        return Math.toIntExact(subTask.getEndId() - subTask.getStartId() + 1);
    }

    /**
     * 批量修复子任务。
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchRepairSubTask implements Serializable {

        /**
         * 开始 ID。
         */
        private Long startId;

        /**
         * 结束 ID。
         */
        private Long endId;

    }

}
```

PowerJob 控制台参数示例：

```json
{
  "startId": 1,
  "endId": 100000,
  "shardSize": 5000
}
```

批量处理任务注意事项：

```text
1. 分片字段应选择有索引的字段，例如主键 ID、创建时间、分区字段。
2. 每个子任务的数据范围必须互斥，避免多个 Worker 重复处理同一批数据。
3. 子任务必须幂等，失败重试时不能重复产生副作用。
4. Reduce 阶段只做汇总，不要执行大批量业务逻辑。
5. 批量修复类任务建议支持 dryRun 或灰度范围参数。
```



## 异常处理与日志

本节用于说明 PowerJob 任务执行过程中的日志记录、异常捕获、重试策略、执行结果返回和失败告警处理方式。PowerJob Worker 执行任务时可通过 `TaskContext` 获取任务上下文，并可使用 `OmsLogger` 将日志上报到 PowerJob Server，便于在控制台查看任务在线日志；PowerJob Worker 侧也会进行任务状态上报和失败处理。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob/3.1-task-tracking-and-execution?utm_source=chatgpt.com))

### 任务执行日志

任务执行日志用于定位任务是否触发、参数是否正确、业务分支是否命中、外部依赖是否异常以及任务最终是否成功。建议同时使用业务日志和 PowerJob 在线日志：业务日志写入应用日志文件，PowerJob 在线日志通过 `context.getOmsLogger()` 输出到控制台任务实例日志中。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob/3.1-task-tracking-and-execution?utm_source=chatgpt.com))

任务日志建议包含以下关键信息：

| 日志节点 | 建议内容                                  |
| -------- | ----------------------------------------- |
| 任务开始 | `jobId`、`instanceId`、任务参数、执行节点 |
| 参数解析 | 原始参数、解析结果、默认值处理            |
| 业务处理 | 查询数量、处理数量、成功数量、失败数量    |
| 外部调用 | 接口名称、请求标识、响应状态、耗时        |
| 异常捕获 | 异常类型、异常摘要、关键业务参数          |
| 任务结束 | 执行结果、耗时、成功数、失败数            |

文件位置：`src/main/java/io/github/atengk/powerjob/util/PowerJobLogUtils.java`

下面的工具类用于统一输出普通业务日志和 PowerJob 在线日志，避免每个 Processor 重复编写日志桥接逻辑。

```java
package io.github.atengk.powerjob.util;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.log.OmsLogger;

/**
 * PowerJob 任务日志工具类。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class PowerJobLogUtils {

    private static final Logger log = LoggerFactory.getLogger(PowerJobLogUtils.class);

    private static final ThreadLocal<OmsLogger> OMS_LOGGER_LOCAL = new ThreadLocal<>();

    private PowerJobLogUtils() {
    }

    /**
     * 初始化当前线程的 PowerJob 在线日志。
     *
     * @param context 任务上下文
     */
    public static void init(TaskContext context) {
        if (context != null && context.getOmsLogger() != null) {
            OMS_LOGGER_LOCAL.set(context.getOmsLogger());
        }
    }

    /**
     * 输出 info 日志。
     *
     * @param template 日志模板
     * @param args     日志参数
     */
    public static void info(String template, Object... args) {
        String message = StrUtil.format(template, args);
        OmsLogger omsLogger = OMS_LOGGER_LOCAL.get();

        if (omsLogger != null) {
            omsLogger.info(message);
        }

        log.info(message);
    }

    /**
     * 输出 warn 日志。
     *
     * @param template 日志模板
     * @param args     日志参数
     */
    public static void warn(String template, Object... args) {
        String message = StrUtil.format(template, args);
        OmsLogger omsLogger = OMS_LOGGER_LOCAL.get();

        if (omsLogger != null) {
            omsLogger.warn(message);
        }

        log.warn(message);
    }

    /**
     * 输出 error 日志。
     *
     * @param message 错误信息
     * @param e       异常对象
     */
    public static void error(String message, Throwable e) {
        OmsLogger omsLogger = OMS_LOGGER_LOCAL.get();

        if (omsLogger != null) {
            omsLogger.error(message, e);
        }

        log.error(message, e);
    }

    /**
     * 清理当前线程日志上下文。
     */
    public static void clear() {
        OMS_LOGGER_LOCAL.remove();
    }

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/LogDemoProcessor.java`

下面的处理器演示如何在任务开始、处理过程、任务结束和异常分支中输出日志。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.powerjob.util.PowerJobLogUtils;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 任务日志示例处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Component
public class LogDemoProcessor implements BasicProcessor {

    /**
     * 执行日志示例任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        long startTime = System.currentTimeMillis();
        PowerJobLogUtils.init(context);

        try {
            PowerJobLogUtils.info("开始执行任务，jobId={}，instanceId={}，jobParams={}",
                    context.getJobId(), context.getInstanceId(), context.getJobParams());

            int totalCount = 100;
            int successCount = 0;

            for (int i = 1; i <= totalCount; i++) {
                // 示例逻辑：实际项目中替换为真实业务处理
                successCount++;

                if (i % 20 == 0) {
                    PowerJobLogUtils.info("任务处理中，当前进度={}/{}", i, totalCount);
                }
            }

            long cost = System.currentTimeMillis() - startTime;
            String result = StrUtil.format("任务执行成功，successCount={}，cost={}ms", successCount, cost);

            PowerJobLogUtils.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            PowerJobLogUtils.error("任务执行异常，instanceId=" + context.getInstanceId(), e);
            return new ProcessResult(false, "任务执行异常：" + e.getMessage());
        } finally {
            PowerJobLogUtils.clear();
        }
    }

}
```

日志输出建议：

```text
1. 任务开始和结束必须打印 instanceId，便于串联 PowerJob 控制台日志和应用日志。
2. 批量任务需要打印总数、当前批次、成功数、失败数。
3. 外部接口调用需要打印接口名称、业务请求号和耗时。
4. 异常日志必须打印堆栈，返回结果只保留摘要。
5. 不要在任务结果中返回大对象、大列表或完整接口响应。
```

### 异常捕获与重试

异常捕获用于确保任务失败时能够返回明确的失败状态，而不是让异常直接中断并丢失业务上下文。PowerJob 的任务执行链路包含失败处理、任务重试、Worker 失败恢复和超时处理机制，Worker 侧的任务状态也会向 Server 上报。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob/3.1-task-tracking-and-execution?utm_source=chatgpt.com))

异常可以分为三类处理：

| 异常类型 | 示例                                 | 处理方式                 |
| -------- | ------------------------------------ | ------------------------ |
| 参数异常 | 参数为空、格式错误、日期范围非法     | 直接返回失败，不建议重试 |
| 业务异常 | 状态不允许、数据不存在、幂等冲突     | 按业务语义返回成功或失败 |
| 系统异常 | 数据库异常、网络异常、第三方接口超时 | 返回失败，允许按配置重试 |

文件位置：`src/main/java/io/github/atengk/powerjob/enums/TaskErrorCode.java`

下面的枚举用于定义任务执行中的错误类型，便于统一返回错误信息。

```java
package io.github.atengk.powerjob.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 任务错误码枚举。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
@RequiredArgsConstructor
public enum TaskErrorCode {

    PARAM_ERROR("PARAM_ERROR", "任务参数错误"),

    BIZ_ERROR("BIZ_ERROR", "业务处理失败"),

    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常"),

    RETRYABLE_ERROR("RETRYABLE_ERROR", "可重试异常");

    private final String code;

    private final String message;

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/ReliableTaskProcessor.java`

下面的处理器演示参数异常、业务异常和系统异常的处理方式。参数异常直接失败；业务幂等场景可按实际情况返回成功；系统异常返回失败，交由 PowerJob 任务配置中的重试策略处理。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.powerjob.enums.TaskErrorCode;
import io.github.atengk.powerjob.util.PowerJobLogUtils;
import lombok.Data;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 可靠任务处理器示例。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Component
public class ReliableTaskProcessor implements BasicProcessor {

    /**
     * 执行可靠任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        PowerJobLogUtils.init(context);

        try {
            PowerJobLogUtils.info("开始执行可靠任务，instanceId={}，jobParams={}",
                    context.getInstanceId(), context.getJobParams());

            ReliableTaskParam param = parseParam(context.getJobParams());

            if (isAlreadyProcessed(param.getBizId())) {
                String result = StrUtil.format("业务已处理，bizId={}，本次任务按幂等成功返回", param.getBizId());
                PowerJobLogUtils.warn(result);
                return new ProcessResult(true, result);
            }

            boolean success = executeBusiness(param);
            if (!success) {
                String result = StrUtil.format("{}：bizId={}，业务处理失败",
                        TaskErrorCode.BIZ_ERROR.getCode(), param.getBizId());
                PowerJobLogUtils.warn(result);
                return new ProcessResult(false, result);
            }

            String result = StrUtil.format("可靠任务执行成功，bizId={}", param.getBizId());
            PowerJobLogUtils.info(result);
            return new ProcessResult(true, result);
        } catch (IllegalArgumentException e) {
            PowerJobLogUtils.error("可靠任务参数异常", e);
            return new ProcessResult(false, TaskErrorCode.PARAM_ERROR.getCode() + "：" + e.getMessage());
        } catch (Exception e) {
            PowerJobLogUtils.error("可靠任务系统异常", e);
            return new ProcessResult(false, TaskErrorCode.SYSTEM_ERROR.getCode() + "：" + e.getMessage());
        } finally {
            PowerJobLogUtils.clear();
        }
    }

    /**
     * 解析任务参数。
     *
     * @param jobParams 任务参数
     * @return 参数对象
     */
    private ReliableTaskParam parseParam(String jobParams) {
        if (StrUtil.isBlank(jobParams)) {
            throw new IllegalArgumentException("任务参数不能为空");
        }

        ReliableTaskParam param = JSONUtil.toBean(jobParams, ReliableTaskParam.class);
        if (StrUtil.isBlank(param.getBizId())) {
            throw new IllegalArgumentException("bizId 不能为空");
        }

        return param;
    }

    /**
     * 判断业务是否已处理。
     *
     * @param bizId 业务ID
     * @return 是否已处理
     */
    private boolean isAlreadyProcessed(String bizId) {
        // 示例逻辑：实际项目中应按业务唯一键查询数据库状态
        return false;
    }

    /**
     * 执行业务处理。
     *
     * @param param 任务参数
     * @return 是否成功
     */
    private boolean executeBusiness(ReliableTaskParam param) {
        PowerJobLogUtils.info("执行可靠任务业务逻辑，bizId={}，bizType={}", param.getBizId(), param.getBizType());

        // 示例逻辑：实际项目中替换为真实业务处理
        return true;
    }

    /**
     * 可靠任务参数。
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class ReliableTaskParam {

        /**
         * 业务ID。
         */
        private String bizId;

        /**
         * 业务类型。
         */
        private String bizType;

    }

}
```

PowerJob 控制台重试配置建议：

```text
任务名称：可靠任务示例
执行模式：单机执行
处理器信息：reliableTaskProcessor
最大重试次数：3
任务超时时间：300 秒
任务参数：{"bizId":"ORDER_10001","bizType":"ORDER_SYNC"}
```

重试配置建议如下：

| 场景           | 是否建议重试 | 说明                               |
| -------------- | ------------ | ---------------------------------- |
| 参数错误       | 否           | 重试无法修复参数                   |
| 数据不存在     | 视业务而定   | 如果数据可能延迟写入，可以短期重试 |
| 数据库连接异常 | 是           | 属于临时系统异常                   |
| 第三方接口超时 | 是           | 需要结合接口幂等键                 |
| 业务状态冲突   | 否           | 应按幂等规则返回成功或失败         |
| 批量子任务失败 | 是           | 子任务必须支持幂等重试             |

### 执行结果返回

执行结果用于告诉 PowerJob Server 当前任务是否成功，以及返回一段简短的执行摘要。PowerJob 任务执行状态包含等待调度、已接收、运行中、失败、成功等状态，Worker 会在任务执行过程中上报状态和结果。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob/3.1-task-tracking-and-execution?utm_source=chatgpt.com))

`ProcessResult` 的返回建议如下：

| 返回场景 | `success` | `msg` 建议                       |
| -------- | --------- | -------------------------------- |
| 执行成功 | `true`    | 返回处理数量、耗时、关键业务编号 |
| 参数错误 | `false`   | 返回具体缺失字段或格式错误       |
| 业务失败 | `false`   | 返回业务失败摘要                 |
| 系统异常 | `false`   | 返回异常摘要，不返回完整堆栈     |
| 幂等命中 | `true`    | 返回“已处理，本次跳过”           |

文件位置：`src/main/java/io/github/atengk/powerjob/util/TaskResultUtils.java`

下面的工具类用于统一构建任务执行结果。

```java
package io.github.atengk.powerjob.util;

import cn.hutool.core.util.StrUtil;
import tech.powerjob.worker.core.processor.ProcessResult;

/**
 * 任务执行结果工具类。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class TaskResultUtils {

    private TaskResultUtils() {
    }

    /**
     * 返回成功结果。
     *
     * @param template 结果模板
     * @param args     模板参数
     * @return 执行结果
     */
    public static ProcessResult success(String template, Object... args) {
        return new ProcessResult(true, StrUtil.format(template, args));
    }

    /**
     * 返回失败结果。
     *
     * @param template 结果模板
     * @param args     模板参数
     * @return 执行结果
     */
    public static ProcessResult failure(String template, Object... args) {
        return new ProcessResult(false, StrUtil.format(template, args));
    }

    /**
     * 返回幂等成功结果。
     *
     * @param bizId 业务ID
     * @return 执行结果
     */
    public static ProcessResult idempotentSuccess(String bizId) {
        return new ProcessResult(true, StrUtil.format("业务已处理，本次跳过，bizId={}", bizId));
    }

}
```

使用示例：

```java
return TaskResultUtils.success("任务执行成功，处理数量={}，耗时={}ms", successCount, cost);
return TaskResultUtils.failure("任务参数错误，缺少字段={}", "bizId");
return TaskResultUtils.idempotentSuccess("ORDER_10001");
```

执行结果注意事项：

```text
1. 执行结果只返回摘要，不返回完整对象、完整列表或完整异常堆栈。
2. 失败结果要明确说明失败原因，避免只返回 false 或 unknown error。
3. 幂等跳过通常应返回成功，避免重复触发重试。
4. 大批量任务应返回成功数、失败数、总数和耗时。
5. 敏感信息不能写入执行结果，例如密码、Token、身份证号、银行卡号。
```

### 失败告警处理

失败告警用于在任务失败、超时、连续失败或关键任务未执行时通知开发和运维人员。PowerJob 提供任务管理、状态监控和在线日志查看能力，具体告警方式需要结合当前 Server 版本、企业通知渠道和监控系统配置处理。([GitHub](https://github.com/PowerJob/PowerJob?utm_source=chatgpt.com))

失败告警建议分为两层：

| 层级                | 说明                                                         |
| ------------------- | ------------------------------------------------------------ |
| PowerJob 平台侧告警 | 在 PowerJob 控制台配置任务失败、超时、重试失败等告警策略     |
| 业务系统侧告警      | Processor 捕获关键异常后调用企业微信、钉钉、飞书、短信或监控平台 |

文件位置：`src/main/java/io/github/atengk/powerjob/service/TaskAlertService.java`

下面的接口用于抽象失败告警能力。

```java
package io.github.atengk.powerjob.service;

/**
 * 任务告警服务。
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface TaskAlertService {

    /**
     * 发送任务失败告警。
     *
     * @param taskName   任务名称
     * @param instanceId 任务实例ID
     * @param reason     失败原因
     */
    void sendFailureAlert(String taskName, Long instanceId, String reason);

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/service/impl/TaskAlertServiceImpl.java`

下面的实现类演示告警消息组装。实际项目中可替换为钉钉、企业微信、飞书、邮件或 Prometheus Alertmanager。

```java
package io.github.atengk.powerjob.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.powerjob.service.TaskAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 任务告警服务实现。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
public class TaskAlertServiceImpl implements TaskAlertService {

    /**
     * 发送任务失败告警。
     *
     * @param taskName   任务名称
     * @param instanceId 任务实例ID
     * @param reason     失败原因
     */
    @Override
    public void sendFailureAlert(String taskName, Long instanceId, String reason) {
        String message = StrUtil.format("PowerJob 任务失败告警，taskName={}，instanceId={}，reason={}",
                taskName, instanceId, reason);

        // 示例逻辑：实际项目中可调用企业微信、钉钉、飞书、邮件或监控平台
        log.warn(message);
    }

}
```

文件位置：`src/main/java/io/github/atengk/powerjob/processor/AlertDemoProcessor.java`

下面的处理器演示任务失败时如何发送业务侧告警。

```java
package io.github.atengk.powerjob.processor;

import io.github.atengk.powerjob.service.TaskAlertService;
import io.github.atengk.powerjob.util.PowerJobLogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 失败告警示例处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Component
@RequiredArgsConstructor
public class AlertDemoProcessor implements BasicProcessor {

    private final TaskAlertService taskAlertService;

    /**
     * 执行告警示例任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        PowerJobLogUtils.init(context);

        try {
            PowerJobLogUtils.info("开始执行告警示例任务，instanceId={}", context.getInstanceId());

            // 示例逻辑：实际项目中替换为真实业务处理
            throw new IllegalStateException("模拟关键任务执行失败");
        } catch (Exception e) {
            PowerJobLogUtils.error("告警示例任务执行失败", e);
            taskAlertService.sendFailureAlert("告警示例任务", context.getInstanceId(), e.getMessage());
            return new ProcessResult(false, "任务执行失败，已发送业务告警：" + e.getMessage());
        } finally {
            PowerJobLogUtils.clear();
        }
    }

}
```

告警处理建议：

```text
1. 普通失败优先依赖 PowerJob 控制台任务实例和在线日志。
2. 核心链路任务需要配置业务侧告警，例如支付、结算、对账、数据同步。
3. 告警消息应包含任务名称、实例 ID、环境、失败原因和排查入口。
4. 高频任务需要做告警收敛，避免同一问题短时间重复通知。
5. 告警失败不能影响原任务结果返回，告警异常需要单独捕获。
```

## 本地开发与调试

本节用于说明本地如何启动 PowerJob Server、注册 Spring Boot Worker，并通过控制台手动触发任务进行验证。PowerJob 提供可视化前端页面，可用于任务管理、状态监控和在线日志查看，因此本地调试应优先使用控制台观察任务实例和日志。([GitHub](https://github.com/PowerJob/PowerJob?utm_source=chatgpt.com))

### 本地启动 PowerJob Server

本地开发环境建议使用 Docker Compose 启动 PowerJob Server 和 MySQL。Server 镜像版本应与 Worker 依赖版本保持一致，避免调试环境和项目依赖不一致。

文件位置：`docker/powerjob/docker-compose.yml`

```yaml
version: "3.8"

services:
  powerjob-mysql:
    image: mysql:8.0
    container_name: powerjob-mysql
    restart: always
    environment:
      # MySQL root 密码，仅用于本地开发环境
      MYSQL_ROOT_PASSWORD: root123456
      # 初始化 PowerJob 元数据库
      MYSQL_DATABASE: powerjob
      # 设置容器时区
      TZ: Asia/Shanghai
    ports:
      # 避免与本机 MySQL 3306 冲突
      - "3307:3306"
    volumes:
      # 持久化数据库数据
      - ./data/mysql:/var/lib/mysql
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_general_ci
      - --lower_case_table_names=1

  powerjob-server:
    image: powerjob/powerjob-server:v5.1.2
    container_name: powerjob-server
    restart: always
    depends_on:
      - powerjob-mysql
    environment:
      # 本地开发 JVM 参数
      JVMOPTIONS: "-Xms256m -Xmx512m"
      # PowerJob Server 启动参数
      PARAMS: >-
        --oms.mongodb.enable=false
        --spring.datasource.core.jdbc-url=jdbc:mysql://powerjob-mysql:3306/powerjob?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
        --spring.datasource.core.username=root
        --spring.datasource.core.password=root123456
    ports:
      # Worker 连接 Server 使用
      - "7700:7700"
      # 控制台或 Server 相关端口，按当前镜像实际配置访问
      - "10086:10086"
      - "10010:10010"
      - "10077:10077"
    volumes:
      # 持久化 Server 运行数据
      - ./data/server:/root/powerjob/server
```

在 `docker/powerjob` 目录执行以下命令：

```bash
# 创建目录
mkdir -p docker/powerjob
cd docker/powerjob

# 启动 PowerJob Server 和 MySQL
docker compose up -d

# 查看容器状态
docker compose ps

# 查看 Server 启动日志
docker logs -f powerjob-server
```

命令说明：`docker compose up -d` 用于后台启动服务；`docker compose ps` 用于查看容器状态；`docker logs -f powerjob-server` 用于观察 Server 是否正常完成启动。控制台访问地址以当前容器实际暴露端口为准，如果 `10086` 无法访问，应检查容器日志和端口映射。

常见排查命令：

```bash
# 检查容器是否存在
docker ps | grep powerjob

# 检查 PowerJob Server 端口映射
docker port powerjob-server

# 检查 MySQL 日志
docker logs powerjob-mysql --tail=100

# 检查 PowerJob Server 日志
docker logs powerjob-server --tail=200
```

### 本地注册 Worker

本地注册 Worker 指 Spring Boot 应用启动后，根据 `powerjob.worker.app-name` 和 `powerjob.worker.server-address` 连接 PowerJob Server，并在控制台中显示为可用 Worker 节点。PowerJob Worker Starter 用于简化 Spring Boot 应用集成 Worker 的过程。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-dev.yml`

```yaml
server:
  # 本地 Worker 应用端口
  port: 8080

spring:
  application:
    # 需要与 PowerJob 控制台中的应用名称一致
    name: springboot-powerjob-demo

powerjob:
  worker:
    # 启用 Worker
    enabled: true
    # Worker 所属应用名称
    app-name: ${spring.application.name}
    # 本地 PowerJob Server 地址
    server-address: 127.0.0.1:7700
    # Worker 本地通信端口，同一机器多实例时必须修改
    port: 27777
    # 本地调试建议使用 disk，便于观察任务数据
    store-strategy: disk
    # 任务执行结果最大长度
    max-result-length: 8192
    # 健康上报间隔，单位秒
    health-report-interval: 10

logging:
  level:
    io.github.atengk: debug
    tech.powerjob: info
```

启动 Worker：

```bash
# 使用 dev 环境启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或打包后启动
mvn clean package -DskipTests
java -jar target/springboot-powerjob-demo-1.0.0.jar --spring.profiles.active=dev
```

启动后检查：

```bash
# 检查 Spring Boot 健康检查接口
curl http://127.0.0.1:8080/health

# 检查 Worker 端口是否监听
lsof -i:27777

# 检查本地是否能连通 Server 端口
nc -vz 127.0.0.1 7700
```

本地注册失败时，优先检查以下问题：

| 问题            | 处理方式                                            |
| --------------- | --------------------------------------------------- |
| 应用名称不一致  | 确认 `spring.application.name` 与控制台应用名称一致 |
| Server 地址错误 | 本地 Docker 通常使用 `127.0.0.1:7700`               |
| Worker 端口冲突 | 修改 `powerjob.worker.port`                         |
| Server 未启动   | 查看 `docker ps` 和 `docker logs powerjob-server`   |
| 网络不可达      | 使用 `nc -vz 127.0.0.1 7700` 测试                   |

### 任务触发验证

任务触发验证用于确认从 Server 调度到 Worker 执行的完整链路是否正常。建议先使用简单的单机任务验证，再验证广播任务、固定频率任务和 MapReduce 任务。

推荐验证顺序：

```text
1. 启动 PowerJob Server
2. 启动 Spring Boot Worker
3. 在 PowerJob 控制台确认应用和 Worker 在线
4. 创建单机任务，处理器填写 logDemoProcessor
5. 手动触发任务
6. 查看任务实例状态
7. 查看在线日志和 Spring Boot 控制台日志
```

PowerJob 控制台任务配置示例：

```text
任务名称：本地日志验证任务
执行模式：单机执行
调度方式：手动触发
处理器类型：Java / Spring Bean
处理器信息：logDemoProcessor
任务参数：{"source":"local-debug","batchSize":10}
```

验证结果：

| 检查项      | 预期结果                                            |
| ----------- | --------------------------------------------------- |
| Worker 状态 | 控制台显示 Worker 在线                              |
| 任务实例    | 手动触发后生成一条任务实例                          |
| 执行状态    | 任务实例状态为成功                                  |
| 在线日志    | 可看到 `开始执行任务`、`任务处理中`、`任务执行成功` |
| 应用日志    | Spring Boot 控制台同步输出业务日志                  |

如果任务触发失败，按以下顺序排查：

```text
1. 控制台任务所属应用是否正确。
2. Worker 是否在线。
3. 处理器名称是否填写正确。
4. Processor 类是否被 Spring 扫描到。
5. 任务参数是否符合 Processor 解析逻辑。
6. Worker 日志中是否存在连接失败、反射失败或参数异常。
```

## 部署与验证

本节用于说明 Spring Boot Worker 服务在测试环境或生产环境中的部署方式，以及部署完成后的 Server 连接、任务执行和日志状态检查。生产环境应重点关注版本一致性、网络连通性、Worker 端口规划、任务幂等、日志可观测性和告警配置。

### Worker 服务部署

Worker 服务部署可以采用 Jar、Docker、Kubernetes 等方式。无论采用哪种方式，都需要保证 Worker 可以访问 PowerJob Server 的调度端口，并且多个 Worker 实例的 `app-name` 一致，端口规划不冲突。

文件位置：`Dockerfile`

下面的 Dockerfile 用于构建 Spring Boot Worker 镜像。

```dockerfile
# 使用 JDK 17 运行 Spring Boot 3 应用
FROM eclipse-temurin:17-jre

# 应用工作目录
WORKDIR /app

# 复制构建产物
COPY target/springboot-powerjob-demo-1.0.0.jar /app/app.jar

# Spring Boot 服务端口
EXPOSE 8080

# PowerJob Worker 通信端口
EXPOSE 27777

# 启动应用，环境变量可在容器运行时覆盖
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

构建和运行镜像：

```bash
# 构建 Jar
mvn clean package -DskipTests

# 构建 Worker 镜像
docker build -t springboot-powerjob-demo:1.0.0 .

# 启动 Worker 容器
docker run -d \
  --name springboot-powerjob-demo \
  -p 8080:8080 \
  -p 27777:27777 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e POWERJOB_WORKER_SERVER_ADDRESS=127.0.0.1:7700 \
  springboot-powerjob-demo:1.0.0
```

命令说明：`SPRING_PROFILES_ACTIVE=prod` 用于启用生产配置；`POWERJOB_WORKER_SERVER_ADDRESS` 用于覆盖配置文件中的 Server 地址。容器化部署时，如果 Worker 与 Server 不在同一网络，需要将 `127.0.0.1` 替换为 Server 的内网域名或服务名。

Kubernetes 部署示例：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: springboot-powerjob-demo
  labels:
    app: springboot-powerjob-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: springboot-powerjob-demo
  template:
    metadata:
      labels:
        app: springboot-powerjob-demo
    spec:
      containers:
        - name: springboot-powerjob-demo
          image: springboot-powerjob-demo:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            # Spring Boot 服务端口
            - containerPort: 8080
            # PowerJob Worker 通信端口
            - containerPort: 27777
          env:
            # 启用生产环境配置
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            # PowerJob Server 内网地址
            - name: POWERJOB_WORKER_SERVER_ADDRESS
              value: "powerjob-server:7700"
            # Worker 应用名称
            - name: POWERJOB_WORKER_APP_NAME
              value: "springboot-powerjob-demo"
          readinessProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 20
---
apiVersion: v1
kind: Service
metadata:
  name: springboot-powerjob-demo
spec:
  selector:
    app: springboot-powerjob-demo
  ports:
    - name: http
      port: 8080
      targetPort: 8080
    - name: powerjob-worker
      port: 27777
      targetPort: 27777
```

Kubernetes 部署命令：

```bash
# 应用部署文件
kubectl apply -f k8s/springboot-powerjob-demo.yaml

# 查看 Pod 状态
kubectl get pods -l app=springboot-powerjob-demo

# 查看应用日志
kubectl logs -f deployment/springboot-powerjob-demo
```

### Server 连接验证

Server 连接验证用于确认 Worker 与 PowerJob Server 的网络、应用名和注册状态正常。PowerJob 的任务生命周期包括任务提交、调度、分发到 Worker、Worker 执行、结果上报和失败重试等环节，因此 Worker 成功连接 Server 是任务执行的前提。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob?utm_source=chatgpt.com))

验证命令：

```bash
# 验证 Worker 应用健康状态
curl http://127.0.0.1:8080/health

# 验证 Worker 到 Server 端口连通性
nc -vz powerjob-server 7700

# Docker 部署时查看容器日志
docker logs springboot-powerjob-demo --tail=200

# Kubernetes 部署时查看 Pod 日志
kubectl logs -f deployment/springboot-powerjob-demo
```

Worker 日志中应重点关注以下信息：

```text
1. PowerJob Worker 是否启动成功。
2. server-address 是否为目标环境地址。
3. app-name 是否为当前业务应用名称。
4. Worker 是否完成注册或连接。
5. 是否存在端口冲突、网络超时、认证失败或协议异常。
```

连接失败排查表：

| 现象                | 可能原因           | 处理方式                         |
| ------------------- | ------------------ | -------------------------------- |
| Worker 启动失败     | 端口冲突           | 修改 `powerjob.worker.port`      |
| Server 连接失败     | 地址错误或网络不通 | 检查 `server-address` 和网络策略 |
| 控制台看不到 Worker | 应用名称不一致     | 检查 `app-name`                  |
| 多实例只有一个在线  | 端口或容器网络冲突 | 检查端口暴露和 Pod 网络          |
| 任务无法下发        | Worker 不健康      | 查看 Worker 心跳和日志           |

### 任务执行验证

任务执行验证用于确认部署后的 Worker 能够接收任务、执行 Processor、返回结果并上报日志。PowerJob Worker 执行任务后会进行结果和状态上报，控制台可用于查看任务状态和在线日志。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob/3.1-task-tracking-and-execution?utm_source=chatgpt.com))

建议创建一个专门用于部署验证的轻量任务，不依赖数据库、Redis 或第三方接口，只验证调度链路。

文件位置：`src/main/java/io/github/atengk/powerjob/processor/DeployVerifyProcessor.java`

下面的处理器用于部署后验证任务执行链路。

```java
package io.github.atengk.powerjob.processor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.powerjob.util.PowerJobLogUtils;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 部署验证任务处理器。
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Component
public class DeployVerifyProcessor implements BasicProcessor {

    /**
     * 执行部署验证任务。
     *
     * @param context 任务上下文
     * @return 执行结果
     */
    @Override
    public ProcessResult process(TaskContext context) {
        long startTime = System.currentTimeMillis();
        PowerJobLogUtils.init(context);

        try {
            PowerJobLogUtils.info("开始执行部署验证任务，jobId={}，instanceId={}，jobParams={}",
                    context.getJobId(), context.getInstanceId(), context.getJobParams());

            String result = StrUtil.format("部署验证成功，time={}，cost={}ms",
                    DateUtil.now(), System.currentTimeMillis() - startTime);

            PowerJobLogUtils.info(result);
            return new ProcessResult(true, result);
        } catch (Exception e) {
            PowerJobLogUtils.error("部署验证任务执行异常", e);
            return new ProcessResult(false, "部署验证任务执行异常：" + e.getMessage());
        } finally {
            PowerJobLogUtils.clear();
        }
    }

}
```

PowerJob 控制台任务配置：

```text
任务名称：部署验证任务
执行模式：单机执行
调度方式：手动触发
处理器类型：Java / Spring Bean
处理器信息：deployVerifyProcessor
任务参数：{"env":"prod","purpose":"deploy-check"}
```

验证标准：

| 检查项     | 预期结果                                 |
| ---------- | ---------------------------------------- |
| 任务实例   | 手动触发后生成任务实例                   |
| 执行状态   | 状态为成功                               |
| 执行结果   | 返回 `部署验证成功`                      |
| 在线日志   | 控制台可查看任务开始和结束日志           |
| 应用日志   | Worker 日志中有同样的任务日志            |
| 多实例验证 | 多个 Worker 部署时，控制台可看到多个节点 |

### 日志与状态检查

日志与状态检查用于部署后持续观察 Worker 运行状态、任务实例状态、任务执行耗时和失败原因。PowerJob Worker 包含任务跟踪、处理器执行、任务持久化、日志上报和状态上报等机制，控制台日志与应用日志应结合使用。([DeepWiki](https://deepwiki.com/PowerJob/PowerJob/3.1-task-tracking-and-execution?utm_source=chatgpt.com))

部署完成后建议检查以下内容：

```text
1. Worker 应用是否正常启动。
2. Worker 是否成功连接 PowerJob Server。
3. 控制台是否显示 Worker 在线。
4. 部署验证任务是否能手动触发成功。
5. 在线日志是否能正常展示。
6. 应用日志是否能输出任务开始、结束和异常信息。
7. 失败任务是否能按配置重试。
8. 关键任务失败是否能触发告警。
```

Docker 环境检查命令：

```bash
# 查看 Worker 容器状态
docker ps | grep springboot-powerjob-demo

# 查看 Worker 最近日志
docker logs springboot-powerjob-demo --tail=200

# 持续跟踪 Worker 日志
docker logs -f springboot-powerjob-demo

# 检查健康接口
curl http://127.0.0.1:8080/health
```

Kubernetes 环境检查命令：

```bash
# 查看 Deployment 状态
kubectl get deployment springboot-powerjob-demo

# 查看 Pod 状态
kubectl get pods -l app=springboot-powerjob-demo -o wide

# 查看最近日志
kubectl logs deployment/springboot-powerjob-demo --tail=200

# 持续跟踪日志
kubectl logs -f deployment/springboot-powerjob-demo

# 进入 Pod 检查网络
kubectl exec -it deployment/springboot-powerjob-demo -- sh
```

任务状态检查建议：

| 检查对象 | 检查内容                          |
| -------- | --------------------------------- |
| 应用列表 | Worker 是否在线、实例数量是否正确 |
| 任务列表 | 任务是否启用、调度方式是否正确    |
| 任务实例 | 是否按预期生成实例                |
| 执行日志 | 是否有完整开始、处理中、结束日志  |
| 失败记录 | 失败原因是否明确                  |
| 重试记录 | 是否按配置进行重试                |
| 执行耗时 | 是否超过任务超时时间              |
| 告警记录 | 失败后是否触达负责人              |

常见问题处理：

| 问题               | 处理方式                                              |
| ------------------ | ----------------------------------------------------- |
| 任务一直不触发     | 检查任务是否启用、调度时间、Cron 表达式和 Server 时间 |
| 任务触发但不执行   | 检查 Worker 是否在线、处理器名称是否正确              |
| 任务执行失败       | 查看在线日志和应用日志，定位参数、业务或系统异常      |
| 任务执行超时       | 调整批量大小、优化 SQL、增加超时时间或改为 MapReduce  |
| 日志看不到         | 确认使用 `context.getOmsLogger()` 或日志桥接工具      |
| 重试后重复处理     | 检查业务幂等逻辑、唯一键和状态流转                    |
| 多实例任务重复执行 | 确认执行模式是否误选为广播，业务是否需要分布式锁      |

