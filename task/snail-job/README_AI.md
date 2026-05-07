# Spring Boot 集成 SnailJob 开发

## 项目概述

本章节用于说明项目为什么引入 SnailJob、适合解决哪些业务问题，以及 Spring Boot 业务系统与 SnailJob 服务端之间的整体集成关系。SnailJob 官方定位为“灵活、可靠、高效的分布式任务调度与重试平台”，核心能力覆盖分布式重试、任务调度、工作流编排、监控告警和多语言客户端接入。([Snail Job](https://snailjob.opensnail.com/docs/introduce/preface.html))

### 功能定位

在 Spring Boot 业务系统中，SnailJob 主要承担任务调度、失败重试、执行日志采集、任务状态管理和可视化运维的职责。业务服务作为 SnailJob 客户端运行，负责注册执行器、接收服务端调度指令、执行业务逻辑，并将执行结果、异常信息和日志上报到 SnailJob 服务端。

整体职责可以拆分为三层：

| 层级                 | 职责         | 说明                                         |
| -------------------- | ------------ | -------------------------------------------- |
| Spring Boot 业务服务 | 执行业务任务 | 编写任务执行器、处理业务参数、保证业务幂等   |
| SnailJob 客户端      | 通信与上报   | 连接服务端，注册任务节点，上报执行状态和日志 |
| SnailJob 服务端      | 调度与管理   | 配置任务、分发调度、记录日志、管理重试和告警 |

集成后，业务系统不再需要只依赖 `@Scheduled`、Quartz 或自研轮询表来处理复杂任务，而是将任务的触发、分发、失败重试、可视化日志和运维操作交给 SnailJob 管理。SnailJob 的能力包含分布式调度、分布式重试、任务编排、失败处理、实时日志和多渠道告警等，适合用于需要可靠执行和可观测运维的后台任务。([Snail Job](https://snailjob.opensnail.com/docs/introduce/project_features.html))

### 应用场景

SnailJob 适合用于对可靠性、可追踪性、可恢复性要求较高的后台任务场景。典型场景如下：

| 场景       | 业务示例                                          | 使用方式                                        |
| ---------- | ------------------------------------------------- | ----------------------------------------------- |
| 定时任务   | 每日统计报表、订单超时关闭、会员权益刷新          | 在管理端配置 CRON、固定频率或秒级调度           |
| 分布式任务 | 多实例部署下避免重复执行、任务按节点分发          | 使用集群、广播、分片、Map、MapReduce 等执行模式 |
| 失败重试   | MQ 消息发送失败、第三方接口调用失败、支付回调失败 | 使用本地、远程或混合重试策略                    |
| 最终一致性 | 订单状态同步、库存补偿、支付结果补偿              | 失败后自动重试，超过阈值进入告警或人工处理      |
| 运维可视化 | 查看任务执行日志、失败原因、重试次数              | 在 SnailJob 管理端查看执行记录和失败明细        |
| 工作流编排 | 多步骤任务串联、审批后触发后续任务                | 通过工作流节点管理任务依赖关系                  |

例如，在订单服务中，创建订单后需要发送 MQ 消息。如果消息中间件临时不可用，直接抛出异常会导致下游系统无法感知订单状态变化。接入 SnailJob 后，可以将消息发送逻辑包装为可重试任务，由 SnailJob 记录失败数据并按配置策略重新执行。官方文档也将强通知、消息队列发送、业务回调等列为典型重试场景。([Snail Job](https://snailjob.opensnail.com/docs/introduce/use_case_scenarios.html))

### 技术选型

本项目以 Spring Boot 3 为业务应用基础框架，以 SnailJob 作为分布式任务调度与重试平台。Spring Boot 3.5.x 要求至少 Java 17，并支持 Maven 3.6.3 或更高版本；因此业务项目建议统一使用 JDK 17 或 JDK 21，避免使用 JDK 8 启动 Spring Boot 3 项目。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

| 技术                    | 推荐版本               | 用途                                      |
| ----------------------- | ---------------------- | ----------------------------------------- |
| JDK                     | 17 或 21               | Spring Boot 3 运行环境                    |
| Spring Boot             | 3.x                    | 业务服务基础框架                          |
| SnailJob Server         | 与客户端版本保持一致   | 任务调度、重试、日志、管理端              |
| SnailJob Client         | `${snail-job.version}` | Spring Boot 业务服务接入 SnailJob         |
| MySQL                   | 8.x                    | SnailJob 服务端数据库，开发与生产优先推荐 |
| Docker / Docker Compose | Docker Compose v2      | 本地或测试环境快速部署                    |
| Maven                   | 3.6.3+                 | 项目构建与依赖管理                        |
| Lombok                  | 1.18.x                 | 简化实体类和配置类代码                    |
| Hutool                  | 5.8.x                  | 字符串、集合、JSON、日期等工具能力        |
| MyBatis-Plus            | 3.5.x                  | 业务表 CRUD 与任务关联表管理，可选        |

SnailJob Java 客户端官方 Maven 依赖主要包括 `snail-job-client-starter`、`snail-job-client-retry-core` 和 `snail-job-client-job-core`，后续依赖配置章节中应统一通过 `${snail-job.version}` 管理版本，避免客户端与服务端版本不一致。([Snail Job](https://snailjob.opensnail.com/docs/guide/client/java/client.html))

## 环境准备

本章节用于准备 Spring Boot 业务服务和 SnailJob 服务端运行所需的基础环境，包括 JDK、构建工具、SnailJob 服务端、数据库以及业务中可能依赖的中间件。

### JDK 与 Spring Boot 版本

Spring Boot 3 项目最低要求 Java 17。开发环境、测试环境和生产环境应尽量保持 JDK 主版本一致，推荐使用 JDK 17 作为默认基线；如果团队已经统一升级到 JDK 21，也可以使用 JDK 21 作为长期运行版本。Spring Boot 3.5.x 官方文档显示其最低要求为 Java 17，并要求 Maven 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

开发机需要确认以下工具已经安装：

```bash
# 查看 JDK 版本，Spring Boot 3 要求至少 Java 17
java -version

# 查看 Maven 版本，建议使用 Maven 3.6.3 或更高版本
mvn -version

# 查看 Docker 版本，用于本地启动 SnailJob Server
docker version

# 查看 Docker Compose 版本，SnailJob 官方数据库构建示例要求 Docker Compose v2
docker compose version
```

推荐版本约束如下：

| 环境项         | 开发环境 | 测试/生产环境 | 说明                               |
| -------------- | -------- | ------------- | ---------------------------------- |
| JDK            | 17 或 21 | 17 或 21      | 所有环境保持一致                   |
| Spring Boot    | 3.x      | 3.x           | 与业务项目统一                     |
| Maven          | 3.6.3+   | 3.6.3+        | CI/CD 构建环境同样需要配置         |
| Docker         | 20.x+    | 按运维规范    | 本地、测试环境部署服务端           |
| Docker Compose | v2       | 按运维规范    | 快速启动数据库或 SnailJob 相关服务 |

### SnailJob 服务端部署

SnailJob 服务端负责提供管理端、任务调度、任务分发、日志记录和重试数据管理能力。Spring Boot 业务应用只需要以客户端形式接入服务端，因此在开发业务代码之前，应先准备可访问的 SnailJob Server。

本地开发环境推荐使用 Docker 方式启动 SnailJob Server，便于快速验证客户端接入。官方 Docker 示例使用 `8080` 作为 Web 端口，使用 `17888` 作为通信端口。([Snail Job](https://snailjob.opensnail.com/docs/guide/server/service_deployment.html))

```bash
# 创建 SnailJob 服务端容器，示例以 MySQL 作为服务端数据库
# 注意：将 ip:port 替换为实际 MySQL 地址，例如 192.168.1.10:3306
docker run \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e SPRING_DATASOURCE_URL='jdbc:mysql://ip:port/snail_job?useSSL=false&characterEncoding=utf8&useUnicode=true' \
  -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver \
  -p 8080:8080 \
  -p 17888:17888 \
  --name snail-job-server-mysql \
  -d opensnail/snail-job:{Latest Version}
```

命令参数说明：

| 参数                                   | 说明                                          |
| -------------------------------------- | --------------------------------------------- |
| `SPRING_DATASOURCE_USERNAME`           | SnailJob 服务端数据库用户名                   |
| `SPRING_DATASOURCE_PASSWORD`           | SnailJob 服务端数据库密码                     |
| `SPRING_DATASOURCE_URL`                | SnailJob 服务端数据库 JDBC 地址               |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME`  | 数据库驱动类                                  |
| `8080:8080`                            | 管理端 Web 访问端口                           |
| `17888:17888`                          | 客户端与服务端通信端口                        |
| `opensnail/snail-job:{Latest Version}` | SnailJob 服务端镜像版本，生产环境应固定版本号 |

服务端启动后，可以通过浏览器访问管理端。官方部署文档给出的默认访问路径和账号为 `http://localhost:8000/snail-job`、用户名 `admin`、密码 `admin`；如果实际 Docker 映射为 `8080:8080`，则本地访问地址通常为 `http://localhost:8080/snail-job`，具体以实际 `server.port` 和网关配置为准。([Snail Job](https://snailjob.opensnail.com/docs/guide/server/service_deployment.html))

生产环境部署时不建议继续使用默认密码和默认 Token。应在服务端初始化后立即修改管理员密码，并将客户端 Token、命名空间、分组信息按环境隔离，例如 `dev`、`test`、`prod` 分别使用不同命名空间和分组。

### 数据库与中间件准备

SnailJob 服务端需要数据库保存任务、执行记录、重试记录、日志和系统配置。官方服务端部署文档说明，SnailJob 已适配 MySQL、达梦 V8、Oracle、PostgreSQL、SQL Server，并在源码 `/doc/sql/` 目录下提供对应初始化脚本。([Snail Job](https://snailjob.opensnail.com/docs/guide/server/service_deployment.html))

开发和生产环境推荐优先使用 MySQL 8.x。数据库初始化建议单独创建 `snail_job` 库，并使用独立账号授权，避免和业务库共用高权限账号。

```sql
-- 创建 SnailJob 服务端数据库
CREATE DATABASE IF NOT EXISTS snail_job
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

-- 创建 SnailJob 独立账号，生产环境请替换为强密码
CREATE USER IF NOT EXISTS 'snail_job'@'%' IDENTIFIED BY 'SnailJob@123456';

-- 授权 SnailJob 账号访问 snail_job 数据库
GRANT ALL PRIVILEGES ON snail_job.* TO 'snail_job'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

对应 Docker 启动命令中的数据库连接配置可以调整为：

```bash
# 使用独立数据库账号启动 SnailJob Server
docker run \
  -e SPRING_DATASOURCE_USERNAME=snail_job \
  -e SPRING_DATASOURCE_PASSWORD='SnailJob@123456' \
  -e SPRING_DATASOURCE_URL='jdbc:mysql://192.168.1.10:3306/snail_job?useSSL=false&characterEncoding=utf8&useUnicode=true&serverTimezone=Asia/Shanghai' \
  -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver \
  -p 8080:8080 \
  -p 17888:17888 \
  --name snail-job-server \
  -d opensnail/snail-job:{Latest Version}
```

SnailJob 自身接入成本较低，官方说明其系统特性包含“低接入成本：仅依赖数据库”。因此，Redis、RabbitMQ、Kafka 等并不是 SnailJob 服务端的必选依赖；它们通常属于业务系统自己的中间件。如果任务内容涉及缓存刷新、消息补偿、异步通知或第三方接口调用，则需要按业务系统实际情况准备对应中间件。([Snail Job](https://snailjob.opensnail.com/docs/introduce/preface.html))

建议环境准备清单如下：

| 类型                        | 是否必选 | 用途                                              |
| --------------------------- | -------- | ------------------------------------------------- |
| MySQL / PostgreSQL 等数据库 | 必选     | SnailJob 服务端持久化任务、日志、重试数据         |
| Redis                       | 可选     | 业务任务缓存、分布式锁、幂等控制                  |
| RabbitMQ / Kafka            | 可选     | 业务消息发送、消息补偿、失败重试                  |
| Nginx                       | 可选     | SnailJob 服务端高可用代理、统一入口               |
| Prometheus / Grafana        | 可选     | 生产环境监控指标采集和可视化                      |
| 日志平台                    | 可选     | 聚合 Spring Boot 客户端日志，便于排查任务执行异常 |

生产环境建议将 SnailJob 服务端数据库与业务数据库分库管理。业务任务表中只保存业务状态和 SnailJob 关联字段，例如任务编号、重试场景、执行状态、失败原因、最后执行时间等；SnailJob 内部调度表和日志表交由 SnailJob 服务端维护，避免业务服务直接修改 SnailJob 内部表。

## 项目依赖配置

本章节用于说明 Spring Boot 业务服务接入 SnailJob 客户端时需要引入哪些依赖、配置哪些连接参数，以及应用启动时如何启用 SnailJob 客户端能力。当前大纲中的后续“任务开发实现”章节会基于这里的依赖和配置继续展开。

### Maven 依赖配置

SnailJob Java 客户端在 Spring 项目中主要使用 `snail-job-client-starter`、`snail-job-client-retry-core` 和 `snail-job-client-job-core` 三个依赖，分别负责客户端自动装配、重试能力和任务执行能力。官方 Java 客户端文档也给出了这三个 Maven 依赖作为 Spring 接入配置。([Snail Job](https://snailjob.opensnail.com/docs/guide/client/java/client.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 3 建议使用 JDK 17 或更高版本 -->
    <java.version>17</java.version>

    <!-- SnailJob 服务端与客户端版本建议保持一致 -->
    <snail-job.version>请替换为实际版本</snail-job.version>

    <!-- 常用工具类，业务参数处理和 JSON 转换时使用 -->
    <hutool.version>5.8.35</hutool.version>
</properties>

<dependencies>
    <!-- Web 基础能力，用于提供任务触发、测试、管理接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- SnailJob 客户端启动器，提供 Spring Boot 自动装配能力 -->
    <dependency>
        <groupId>com.aizuda</groupId>
        <artifactId>snail-job-client-starter</artifactId>
        <version>${snail-job.version}</version>
    </dependency>

    <!-- SnailJob 重试核心依赖，提供 @Retryable 等失败重试能力 -->
    <dependency>
        <groupId>com.aizuda</groupId>
        <artifactId>snail-job-client-retry-core</artifactId>
        <version>${snail-job.version}</version>
    </dependency>

    <!-- SnailJob 任务核心依赖，提供 @JobExecutor、JobArgs、ExecuteResult 等任务执行能力 -->
    <dependency>
        <groupId>com.aizuda</groupId>
        <artifactId>snail-job-client-job-core</artifactId>
        <version>${snail-job.version}</version>
    </dependency>

    <!-- Hutool 工具类，用于字符串、JSON、集合、日期等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，减少 DTO、配置类、日志对象的样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于本地任务执行和重试逻辑验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

依赖配置应遵循两个原则。第一，SnailJob 服务端和客户端版本尽量保持一致，避免通信协议、参数结构或管理端功能不兼容。第二，如果当前业务模块只使用调度任务，可以保留 `starter` 和 `job-core`；如果需要失败重试能力，则必须引入 `retry-core`。

### SnailJob 客户端配置

SnailJob 客户端配置主要包括服务端通信地址、命名空间、分组、Token、客户端绑定 IP 和客户端通信端口。快速开始文档中的客户端配置示例包含 `server.host`、`server.port`、`namespace`、`group`、`token`、`host` 和 `port` 等关键项，其中 `server.port` 是客户端连接服务端的通信端口，不是管理后台 Web 端口。([Snail Job](https://snailjob.opensnail.com/docs/quickstart/quick_start.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 当前业务服务端口
  port: 8081

spring:
  application:
    # 当前 Spring Boot 应用名称，建议与 SnailJob 分组保持业务含义一致
    name: order-service
  profiles:
    # 默认使用 dev 环境，生产环境通过启动参数覆盖
    active: dev

snail-job:
  # SnailJob 服务端通信配置
  server:
    # SnailJob 服务端地址；生产环境建议配置为内网域名或 Nginx 地址
    host: 127.0.0.1
    # SnailJob 服务端通信端口，不是管理端 Web 端口
    port: 17888

  # 命名空间，用于环境隔离，例如 dev、test、prod 使用不同 namespace
  namespace: a1NSize1D5jP83Wj0B6Hre094f7kFeHB

  # 接入组名，建议按业务服务划分，例如 order-service、pay-service、member-service
  group: order-service

  # 接入组 Token，需要与 SnailJob 管理端组配置保持一致
  token: SJ_t3GFbbDCpmznt9M9Pp62GYJecE6S9q9H

  # 客户端绑定 IP；服务端无法回调客户端时需要显式配置为本机内网 IP
  host: 127.0.0.1

  # 客户端通信端口；多实例本地调试时不能冲突
  port: 17899

logging:
  # 如需实时日志上报到 SnailJob 管理端，可配置 logback-boot.xml
  config: classpath:logback-boot.xml
```

多环境配置建议将 `namespace`、`group` 和 `token` 放到不同 profile 文件中，避免开发环境误接入生产调度中心。

文件位置：`src/main/resources/application-prod.yml`

```yaml
snail-job:
  server:
    # 生产环境建议使用内网负载均衡地址
    host: snail-job.internal.example.com
    port: 17888

  # 生产环境独立命名空间
  namespace: prod_namespace_id

  # 当前服务在 SnailJob 中的生产分组
  group: order-service

  # 生产 Token 不允许提交到公开仓库，建议通过环境变量或配置中心注入
  token: ${SNAIL_JOB_TOKEN}

  # 生产环境建议显式绑定可被服务端访问的内网 IP
  host: ${POD_IP:127.0.0.1}

  # Kubernetes 或多实例部署时，需确认端口和网络策略可访问
  port: ${SNAIL_JOB_CLIENT_PORT:17899}
```

### 应用基础配置

Spring Boot 应用需要在启动类上开启 SnailJob 客户端能力。官方快速开始文档要求在启动类上添加 `@EnableSnailJob`，并说明也可以通过注解指定分组，但注解配置优先级高于配置文件；为了环境隔离更清晰，实际项目建议优先使用配置文件维护分组信息。([Snail Job](https://snailjob.opensnail.com/docs/quickstart/quick_start.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/SnailJobApplication.java`

```java
package io.github.atengk;

import com.aizuda.snailjob.client.starter.EnableSnailJob;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SnailJob 示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-07
 */
@EnableSnailJob
@SpringBootApplication
public class SnailJobApplication {

    /**
     * 启动 Spring Boot 应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SnailJobApplication.class, args);
    }

}
```

如果需要在 SnailJob 管理端查看任务实时日志，需要额外配置 Logback Appender。官方快速开始文档给出了 `SnailLogbackAppender` 的配置方式，用于将客户端日志输出到 SnailJob。([Snail Job](https://snailjob.opensnail.com/docs/quickstart/quick_start.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/logback-boot.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true">

    <!-- 控制台日志输出，便于本地调试 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- SnailJob 实时日志追加器，用于管理端查看任务执行日志 -->
    <appender name="SNAIL_LOG" class="com.aizuda.snailjob.client.common.appender.SnailLogbackAppender"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="SNAIL_LOG"/>
    </root>

</configuration>
```

基础配置完成后，启动业务服务并进入 SnailJob 管理端的在线机器列表。如果能看到当前客户端节点，说明服务端地址、命名空间、分组、Token 和客户端端口配置基本正确。

## 核心功能设计

本章节用于定义业务系统中任务、分组、执行器和重试策略的设计规范。这里不直接展开完整业务实现，而是明确后续开发普通任务、定时任务、重试任务和回调任务时应遵循的模型和命名规则。

### 任务模型设计

任务模型用于描述业务任务和 SnailJob 调度任务之间的关系。SnailJob 管理端负责任务配置、调度批次和执行日志；业务系统应保存自身业务状态、业务单号、执行结果和幂等标识，不建议直接依赖或修改 SnailJob 内部表。

建议将任务模型拆分为两类：

| 模型         | 维护位置           | 说明                                                         |
| ------------ | ------------------ | ------------------------------------------------------------ |
| 调度任务模型 | SnailJob 服务端    | 任务名称、执行器名称、触发类型、路由策略、阻塞策略、超时时间、重试次数 |
| 业务任务模型 | Spring Boot 业务库 | 业务单号、业务类型、任务状态、执行结果、失败原因、幂等键     |

业务任务状态建议使用有限状态机设计，避免直接用字符串散落在业务代码中。

文件位置：`src/main/java/io/github/atengk/snailjob/model/enums/BizTaskStatusEnum.java`

```java
package io.github.atengk.snailjob.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务任务状态枚举
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
@AllArgsConstructor
public enum BizTaskStatusEnum {

    /**
     * 待执行
     */
    PENDING("PENDING", "待执行"),

    /**
     * 执行中
     */
    RUNNING("RUNNING", "执行中"),

    /**
     * 执行成功
     */
    SUCCESS("SUCCESS", "执行成功"),

    /**
     * 执行失败
     */
    FAILED("FAILED", "执行失败"),

    /**
     * 已取消
     */
    CANCELED("CANCELED", "已取消");

    private final String code;

    private final String description;

}
```

业务任务参数建议使用统一 DTO 承载，后续可以通过 SnailJob 管理端的任务参数传入 JSON 字符串，再在执行器中反序列化为该 DTO。

文件位置：`src/main/java/io/github/atengk/snailjob/model/dto/OrderJobParamDTO.java`

```java
package io.github.atengk.snailjob.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 订单任务参数
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class OrderJobParamDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 操作来源，例如 SYSTEM、ADMIN、CALLBACK
     */
    private String source;

}
```

### 任务分组设计

任务分组用于隔离不同业务服务的任务执行范围。SnailJob 文档在重试场景说明中将“组”解释为业务服务维度，例如商品服务、用户服务、订单服务分别可以对应不同组；“场景”则是服务内更细粒度的功能接口或重试入口。([Snail Job](https://snailjob.opensnail.com/docs/guide/retry/scene.html?utm_source=chatgpt.com))

建议按服务维度设计分组，而不是按单个任务设计分组。

| 业务服务 | SnailJob 分组     | 适合放入的任务                       |
| -------- | ----------------- | ------------------------------------ |
| 订单服务 | `order-service`   | 订单关闭、订单统计、订单状态同步     |
| 支付服务 | `pay-service`     | 支付回调补偿、退款状态同步、对账任务 |
| 会员服务 | `member-service`  | 会员等级刷新、积分结算、权益发放     |
| 消息服务 | `message-service` | 短信补发、站内信推送、MQ 消息补偿    |

分组命名建议遵循以下规则：

| 规则                    | 示例                              | 说明                                   |
| ----------------------- | --------------------------------- | -------------------------------------- |
| 使用服务名              | `order-service`                   | 与 `spring.application.name` 保持一致  |
| 环境通过 namespace 隔离 | `dev_namespace`、`prod_namespace` | 不建议在 group 中拼接环境              |
| 不频繁变更              | `pay-service`                     | group 变更会影响客户端注册和管理端配置 |
| Token 独立              | 每组单独 Token                    | 避免多个服务共用同一 Token             |

可以在业务代码中维护任务名称、执行器名称和重试场景常量，避免字符串写散。

文件位置：`src/main/java/io/github/atengk/snailjob/constants/SnailJobBizConstant.java`

```java
package io.github.atengk.snailjob.constants;

/**
 * SnailJob 业务常量
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class SnailJobBizConstant {

    private SnailJobBizConstant() {
    }

    /**
     * 订单服务分组
     */
    public static final String GROUP_ORDER_SERVICE = "order-service";

    /**
     * 订单超时关闭执行器
     */
    public static final String EXECUTOR_ORDER_TIMEOUT_CLOSE = "orderTimeoutCloseJobExecutor";

    /**
     * 订单统计执行器
     */
    public static final String EXECUTOR_ORDER_STATISTICS = "orderStatisticsJobExecutor";

    /**
     * 支付回调重试场景
     */
    public static final String SCENE_PAY_CALLBACK_RETRY = "payCallbackRetry";

    /**
     * MQ 消息发送重试场景
     */
    public static final String SCENE_MQ_SEND_RETRY = "mqSendRetry";

}
```

### 执行器设计

执行器是 SnailJob 调度任务真正进入业务代码的入口。官方 `@JobExecutor` 文档说明，执行器名称 `name` 是任务标识，同一个客户端不能存在相同的执行器名称；注解方式支持作用在类或方法上，方法参数只支持 `JobArgs` 或无参模式。([Snail Job](https://snailjob.opensnail.com/docs/guide/job/job_executor.html?utm_source=chatgpt.com))

执行器设计建议遵循三个原则。第一，执行器只做参数解析、日志记录、异常转换和调用业务 Service，不直接堆积复杂业务逻辑。第二，执行器名称稳定，不随任务标题或页面描述变化。第三，所有可能重复执行的任务都要做业务幂等判断。

下面给出一个订单超时关闭执行器示例。

文件位置：`src/main/java/io/github/atengk/snailjob/executor/OrderTimeoutCloseJobExecutor.java`

```java
package io.github.atengk.snailjob.executor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import io.github.atengk.snailjob.constants.SnailJobBizConstant;
import io.github.atengk.snailjob.model.dto.OrderJobParamDTO;
import io.github.atengk.snailjob.service.OrderTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单超时关闭任务执行器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutCloseJobExecutor {

    private final OrderTaskService orderTaskService;

    /**
     * 执行订单超时关闭任务
     *
     * @param jobArgs SnailJob 任务参数
     * @return 执行结果
     */
    @JobExecutor(name = SnailJobBizConstant.EXECUTOR_ORDER_TIMEOUT_CLOSE)
    public ExecuteResult closeTimeoutOrder(JobArgs jobArgs) {
        String jobParams = jobArgs.getJobParams();
        if (StrUtil.isBlank(jobParams)) {
            log.warn("订单超时关闭任务参数为空");
            return ExecuteResult.failure("任务参数不能为空");
        }

        try {
            OrderJobParamDTO param = JSONUtil.toBean(jobParams, OrderJobParamDTO.class);
            log.info("开始执行订单超时关闭任务，orderNo={}", param.getOrderNo());

            boolean closed = orderTaskService.closeTimeoutOrder(param);
            if (closed) {
                log.info("订单超时关闭任务执行成功，orderNo={}", param.getOrderNo());
                return ExecuteResult.success("订单关闭成功");
            }

            log.info("订单无需关闭，可能已支付或已取消，orderNo={}", param.getOrderNo());
            return ExecuteResult.success("订单无需关闭");
        } catch (Exception e) {
            log.error("订单超时关闭任务执行失败，params={}", jobParams, e);
            return ExecuteResult.failure("订单超时关闭任务执行失败：" + e.getMessage());
        }
    }

}
```

对应的业务 Service 只保留核心接口，具体订单状态判断、幂等更新和数据库操作在实现类中完成。

文件位置：`src/main/java/io/github/atengk/snailjob/service/OrderTaskService.java`

```java
package io.github.atengk.snailjob.service;

import io.github.atengk.snailjob.model.dto.OrderJobParamDTO;

/**
 * 订单任务服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface OrderTaskService {

    /**
     * 关闭超时未支付订单
     *
     * @param param 订单任务参数
     * @return 是否执行了关闭操作
     */
    boolean closeTimeoutOrder(OrderJobParamDTO param);

}
```

### 重试策略设计

SnailJob 重试能力用于处理短时间失败但后续可能恢复的业务操作，例如第三方接口调用、MQ 消息发送、支付回调通知和远程服务调用。官方 `@Retryable` 文档说明，`scene` 是必填参数，`retryStrategy` 默认是 `LOCAL_REMOTE`，还支持包含异常、排除异常、幂等 ID、业务编号和重试完成回调等配置项。([Snail Job](https://snailjob.opensnail.com/docs/guide/retry/retryable_annotation.html?utm_source=chatgpt.com))

重试策略建议按业务影响范围拆分：

| 场景                   | 推荐策略        | 说明                             |
| ---------------------- | --------------- | -------------------------------- |
| 瞬时网络抖动           | 本地重试优先    | 减少服务端重试数据量             |
| 第三方接口失败         | 本地 + 远程重试 | 本地快速重试失败后交给服务端兜底 |
| MQ 发送失败            | 远程重试        | 避免请求线程长时间阻塞           |
| 支付回调失败           | 本地 + 远程重试 | 需要保证最终一致性               |
| 参数错误或业务校验失败 | 不重试          | 应直接失败并记录原因             |

官方基础参数文档说明，`scene` 用于区分不同重试场景，`localTimes` 表示本地重试次数，`localInterval` 表示本地重试间隔，且建议根据业务特点合理设置重试次数和间隔。([Snail Job](https://snailjob.opensnail.com/docs/guide/retry/basic_setting_params.html?utm_source=chatgpt.com))

下面给出一个 MQ 消息发送失败重试的设计示例。该方法适合用于订单创建后通知下游系统，如果发送失败，则由 SnailJob 记录重试现场并继续补偿。

文件位置：`src/main/java/io/github/atengk/snailjob/service/OrderMessageService.java`

```java
package io.github.atengk.snailjob.service;

/**
 * 订单消息服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface OrderMessageService {

    /**
     * 发送订单创建消息
     *
     * @param orderNo 订单编号
     */
    void sendOrderCreatedMessage(String orderNo);

}
```

文件位置：`src/main/java/io/github/atengk/snailjob/service/impl/OrderMessageServiceImpl.java`

```java
package io.github.atengk.snailjob.service.impl;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snailjob.client.core.annotation.Retryable;
import com.aizuda.snailjob.client.core.retryer.RetryType;
import io.github.atengk.snailjob.constants.SnailJobBizConstant;
import io.github.atengk.snailjob.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单消息服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
public class OrderMessageServiceImpl implements OrderMessageService {

    /**
     * 发送订单创建消息
     *
     * @param orderNo 订单编号
     */
    @Override
    @Retryable(
            scene = SnailJobBizConstant.SCENE_MQ_SEND_RETRY,
            retryStrategy = RetryType.LOCAL_REMOTE,
            localTimes = 3,
            localInterval = 2,
            bizNo = "#orderNo"
    )
    public void sendOrderCreatedMessage(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单编号不能为空");
        }

        log.info("开始发送订单创建消息，orderNo={}", orderNo);

        // 这里替换为 RabbitMQ、Kafka 或其他消息组件发送逻辑
        boolean sendSuccess = mockSendMessage(orderNo);
        if (!sendSuccess) {
            log.warn("订单创建消息发送失败，等待 SnailJob 重试，orderNo={}", orderNo);
            throw new IllegalStateException("订单创建消息发送失败");
        }

        log.info("订单创建消息发送成功，orderNo={}", orderNo);
    }

    /**
     * 模拟发送消息
     *
     * @param orderNo 订单编号
     * @return 是否发送成功
     */
    private boolean mockSendMessage(String orderNo) {
        return false;
    }

}
```

重试场景名称一旦确定后不应随意修改。官方文档明确提醒，`scene` 值指定后不要修改，否则会导致历史重试数据失效。([Snail Job](https://snailjob.opensnail.com/docs/guide/retry/scene.html?utm_source=chatgpt.com))

重试策略落地时需要特别注意幂等性。对于 MQ 发送、支付回调、订单状态同步等任务，应使用业务编号、唯一请求号或幂等表防止重复处理。推荐将 `bizNo` 设置为订单号、支付单号、消息编号等具备业务意义的值，便于在 SnailJob 管理端检索失败记录和定位问题。



## 任务开发实现

本章节用于说明 Spring Boot 业务系统中如何开发 SnailJob 任务代码，包括普通任务、定时任务、重试任务和回调任务。SnailJob 的 Java 客户端通过 `@JobExecutor` 标识任务执行器，执行方法参数支持 `JobArgs` 或无参模式，执行结果通过 `ExecuteResult.success()` 或 `ExecuteResult.failure()` 返回；官方文档也说明同一个客户端中执行器名称不能重复。([Snail Job](https://snailjob.opensnail.com/docs/guide/job/job_executor.html?utm_source=chatgpt.com))

建议在业务工程中按任务类型拆分目录，避免调度任务、重试任务、回调处理逻辑散落在 Controller 或普通 Service 中。

```text
src/main/java/io/github/atengk/snailjob/
├── constants/
│   └── SnailJobBizConstant.java
├── model/
│   ├── dto/
│   │   ├── OrderJobParamDTO.java
│   │   └── PayCallbackRetryDTO.java
│   └── enums/
│       └── BizTaskStatusEnum.java
├── executor/
│   ├── OrderCleanupJobExecutor.java
│   └── DailyOrderStatisticsJobExecutor.java
├── retry/
│   ├── PayCallbackRetryService.java
│   └── impl/
│       └── PayCallbackRetryServiceImpl.java
├── callback/
│   ├── PayRetryCompleteCallback.java
│   └── WorkflowCallbackController.java
└── service/
    ├── OrderTaskService.java
    └── OrderStatisticsService.java
```

### 普通任务开发

普通任务适合处理手动触发、固定业务批处理、临时数据修复、后台补偿等场景。普通任务本质上也是一个 SnailJob 执行器，由服务端根据管理端配置将任务下发到客户端执行，客户端执行完成后返回成功或失败结果。

下面以“清理订单临时数据”为例。该任务适合由运维人员在管理端手动触发，也可以在后续改造成定时任务。执行器只负责参数解析、日志记录和调用业务 Service，具体清理逻辑放到 Service 层。

文件位置：`src/main/java/io/github/atengk/snailjob/model/dto/OrderCleanupParamDTO.java`

```java
package io.github.atengk.snailjob.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 订单清理任务参数
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class OrderCleanupParamDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 清理多少天之前的数据
     */
    private Integer beforeDays;

    /**
     * 是否试运行，true 表示只统计不删除
     */
    private Boolean dryRun;

}
```

文件位置：`src/main/java/io/github/atengk/snailjob/service/OrderTaskService.java`

```java
package io.github.atengk.snailjob.service;

import io.github.atengk.snailjob.model.dto.OrderCleanupParamDTO;
import io.github.atengk.snailjob.model.dto.OrderJobParamDTO;

/**
 * 订单任务服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface OrderTaskService {

    /**
     * 关闭超时未支付订单
     *
     * @param param 订单任务参数
     * @return 是否执行了关闭操作
     */
    boolean closeTimeoutOrder(OrderJobParamDTO param);

    /**
     * 清理订单临时数据
     *
     * @param param 清理参数
     * @return 清理数量
     */
    int cleanupTempOrders(OrderCleanupParamDTO param);

}
```

下面的执行器用于接收 SnailJob 服务端下发的任务参数，并调用订单任务服务完成清理操作。

文件位置：`src/main/java/io/github/atengk/snailjob/executor/OrderCleanupJobExecutor.java`

```java
package io.github.atengk.snailjob.executor;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import io.github.atengk.snailjob.model.dto.OrderCleanupParamDTO;
import io.github.atengk.snailjob.service.OrderTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单临时数据清理任务执行器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCleanupJobExecutor {

    private static final int DEFAULT_BEFORE_DAYS = 30;

    private final OrderTaskService orderTaskService;

    /**
     * 执行订单临时数据清理任务
     *
     * @param jobArgs SnailJob 任务参数
     * @return 执行结果
     */
    @JobExecutor(name = "orderCleanupJobExecutor")
    public ExecuteResult cleanup(JobArgs jobArgs) {
        String jobParams = ObjectUtil.defaultIfNull(jobArgs.getJobParams(), StrUtil.EMPTY);
        log.info("开始执行订单临时数据清理任务，params={}", jobParams);

        try {
            OrderCleanupParamDTO param = buildParam(jobParams);
            int count = orderTaskService.cleanupTempOrders(param);

            String message = StrUtil.format("订单临时数据清理完成，dryRun={}，count={}", param.getDryRun(), count);
            log.info(message);
            return ExecuteResult.success(message);
        } catch (Exception e) {
            log.error("订单临时数据清理任务执行失败，params={}", jobParams, e);
            return ExecuteResult.failure("订单临时数据清理任务执行失败：" + e.getMessage());
        }
    }

    /**
     * 构建任务参数
     *
     * @param jobParams 原始任务参数
     * @return 清理任务参数
     */
    private OrderCleanupParamDTO buildParam(String jobParams) {
        OrderCleanupParamDTO param;
        if (StrUtil.isBlank(jobParams)) {
            param = new OrderCleanupParamDTO();
        } else {
            param = JSONUtil.toBean(jobParams, OrderCleanupParamDTO.class);
        }

        if (ObjectUtil.isNull(param.getBeforeDays()) || param.getBeforeDays() <= 0) {
            param.setBeforeDays(DEFAULT_BEFORE_DAYS);
        }
        if (ObjectUtil.isNull(param.getDryRun())) {
            param.setDryRun(Boolean.TRUE);
        }
        return param;
    }

}
```

在 SnailJob 管理端配置普通任务时，需要保证“执行器名称”与 `@JobExecutor(name = "orderCleanupJobExecutor")` 完全一致。官方快速开始示例也是先编写 `@JobExecutor` 执行器，再到管理端配置任务并触发执行。([Snail Job](https://snailjob.opensnail.com/docs/quickstart/quick_start.html?utm_source=chatgpt.com))

任务参数示例：

```json
{
  "beforeDays": 30,
  "dryRun": true
}
```

配置建议如下：

| 配置项       | 示例值                    | 说明                                |
| ------------ | ------------------------- | ----------------------------------- |
| 任务名称     | 订单临时数据清理          | 管理端展示名称                      |
| 组名称       | `order-service`           | 需要与客户端 `snail-job.group` 一致 |
| 执行器类型   | Java                      | Spring Boot Java 客户端             |
| 执行器名称   | `orderCleanupJobExecutor` | 对应 `@JobExecutor` 的 `name`       |
| 任务参数     | JSON 字符串               | 对应 `OrderCleanupParamDTO`         |
| 超时时间     | 300 秒                    | 根据清理数据量设置                  |
| 最大重试次数 | 0 或 1                    | 数据清理任务不建议盲目多次重试      |

### 定时任务开发

定时任务适合处理周期性业务，例如订单超时关闭、日报统计、对账任务、会员权益刷新等。SnailJob 的定时任务触发规则在服务端管理端配置，业务客户端仍然只需要提供稳定的 `@JobExecutor` 执行器。官方任务示例中，客户端定义执行器后，通过管理端配置任务名称、组、执行器名称、触发类型、间隔、超时时间和重试次数等参数。([Snail Job](https://snailjob.opensnail.com/docs/guide/job/task_type/cluster.html?utm_source=chatgpt.com))

下面以“每日订单统计”为例。该任务每天凌晨执行，统计前一天订单数据。

文件位置：`src/main/java/io/github/atengk/snailjob/model/dto/OrderStatisticsParamDTO.java`

```java
package io.github.atengk.snailjob.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 订单统计任务参数
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class OrderStatisticsParamDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 统计日期，格式 yyyy-MM-dd；为空时默认统计昨天
     */
    private String statisticsDate;

    /**
     * 是否覆盖已有统计结果
     */
    private Boolean overwrite;

}
```

文件位置：`src/main/java/io/github/atengk/snailjob/service/OrderStatisticsService.java`

```java
package io.github.atengk.snailjob.service;

import io.github.atengk.snailjob.model.dto.OrderStatisticsParamDTO;

/**
 * 订单统计服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface OrderStatisticsService {

    /**
     * 统计订单数据
     *
     * @param param 统计参数
     * @return 统计结果描述
     */
    String statistics(OrderStatisticsParamDTO param);

}
```

下面的执行器用于执行每日订单统计。参数为空时默认统计昨天，便于管理端配置为固定周期任务。

文件位置：`src/main/java/io/github/atengk/snailjob/executor/DailyOrderStatisticsJobExecutor.java`

```java
package io.github.atengk.snailjob.executor;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import io.github.atengk.snailjob.model.dto.OrderStatisticsParamDTO;
import io.github.atengk.snailjob.service.OrderStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 每日订单统计任务执行器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyOrderStatisticsJobExecutor {

    private final OrderStatisticsService orderStatisticsService;

    /**
     * 执行每日订单统计任务
     *
     * @param jobArgs SnailJob 任务参数
     * @return 执行结果
     */
    @JobExecutor(name = "dailyOrderStatisticsJobExecutor")
    public ExecuteResult statistics(JobArgs jobArgs) {
        String jobParams = ObjectUtil.defaultIfNull(jobArgs.getJobParams(), StrUtil.EMPTY);
        log.info("开始执行每日订单统计任务，params={}", jobParams);

        try {
            OrderStatisticsParamDTO param = buildParam(jobParams);
            String result = orderStatisticsService.statistics(param);

            log.info("每日订单统计任务执行成功，statisticsDate={}，result={}", param.getStatisticsDate(), result);
            return ExecuteResult.success(result);
        } catch (Exception e) {
            log.error("每日订单统计任务执行失败，params={}", jobParams, e);
            return ExecuteResult.failure("每日订单统计任务执行失败：" + e.getMessage());
        }
    }

    /**
     * 构建统计参数
     *
     * @param jobParams 原始任务参数
     * @return 统计任务参数
     */
    private OrderStatisticsParamDTO buildParam(String jobParams) {
        OrderStatisticsParamDTO param;
        if (StrUtil.isBlank(jobParams)) {
            param = new OrderStatisticsParamDTO();
        } else {
            param = JSONUtil.toBean(jobParams, OrderStatisticsParamDTO.class);
        }

        if (StrUtil.isBlank(param.getStatisticsDate())) {
            param.setStatisticsDate(DateUtil.format(DateUtil.yesterday(), DatePattern.NORM_DATE_PATTERN));
        }
        if (ObjectUtil.isNull(param.getOverwrite())) {
            param.setOverwrite(Boolean.FALSE);
        }
        return param;
    }

}
```

管理端配置建议如下：

| 配置项       | 示例值                            | 说明                           |
| ------------ | --------------------------------- | ------------------------------ |
| 任务名称     | 每日订单统计                      | 管理端展示名称                 |
| 组名称       | `order-service`                   | 与客户端分组一致               |
| 执行器名称   | `dailyOrderStatisticsJobExecutor` | 与 `@JobExecutor` 保持一致     |
| 触发类型     | CRON 或固定时间                   | 按管理端实际支持项选择         |
| CRON         | `0 0 1 * * ?`                     | 每天凌晨 1 点执行              |
| 路由策略     | 轮询 / 第一个 / 最后一个          | 单实例任务建议选择稳定路由     |
| 阻塞策略     | 丢弃或覆盖                        | 统计任务未完成时不建议并发堆积 |
| 最大重试次数 | 1 到 3                            | 取决于任务重要性               |
| 超时时间     | 600 秒                            | 根据统计数据量设置             |

定时任务必须具备幂等性。比如同一天统计任务重复执行时，应通过 `statisticsDate` 做唯一键，优先执行“存在则跳过”或“存在且 overwrite=true 才覆盖”的逻辑，避免重复写入统计结果。

### 重试任务开发

重试任务用于处理短时失败但后续可能恢复的操作，例如第三方支付回调、MQ 消息发送、短信通知、远程接口调用等。SnailJob 的快速开始示例使用 `@Retryable(scene = "...", retryStrategy = RetryType.LOCAL_REMOTE)` 标记需要重试的方法；官方文档也说明 `scene` 是重试场景标识，`retryStrategy` 用于指定本地重试、远程重试或本地加远程组合策略。([Snail Job](https://snailjob.opensnail.com/docs/quickstart/quick_start.html?utm_source=chatgpt.com))

下面以“支付回调通知商户失败后重试”为例。该任务由业务接口主动触发；如果调用商户通知地址失败，则交给 SnailJob 进行本地和远程重试。

文件位置：`src/main/java/io/github/atengk/snailjob/model/dto/PayCallbackRetryDTO.java`

```java
package io.github.atengk.snailjob.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付回调重试参数
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class PayCallbackRetryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 支付单号
     */
    private String payNo;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 商户编号
     */
    private String merchantNo;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 回调地址
     */
    private String callbackUrl;

}
```

文件位置：`src/main/java/io/github/atengk/snailjob/retry/PayCallbackRetryService.java`

```java
package io.github.atengk.snailjob.retry;

import io.github.atengk.snailjob.model.dto.PayCallbackRetryDTO;

/**
 * 支付回调重试服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface PayCallbackRetryService {

    /**
     * 通知商户支付结果
     *
     * @param param 支付回调参数
     */
    void notifyMerchant(PayCallbackRetryDTO param);

}
```

下面的实现类通过 `@Retryable` 标记重试入口。`bizNo` 建议设置为支付单号或订单号，便于在 SnailJob 管理端按业务编号检索失败数据；公开文档和示例也说明 `bizNo` 可用于以订单号、物流编号等业务值进行检索。([CSDN](https://blog.csdn.net/xiyang_1990/article/details/144950151?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/snailjob/retry/impl/PayCallbackRetryServiceImpl.java`

```java
package io.github.atengk.snailjob.retry.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.aizuda.snailjob.client.core.annotation.Retryable;
import com.aizuda.snailjob.client.core.retryer.RetryType;
import io.github.atengk.snailjob.model.dto.PayCallbackRetryDTO;
import io.github.atengk.snailjob.retry.PayCallbackRetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付回调重试服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
public class PayCallbackRetryServiceImpl implements PayCallbackRetryService {

    /**
     * 通知商户支付结果
     *
     * @param param 支付回调参数
     */
    @Override
    @Retryable(
            scene = "payCallbackRetry",
            retryStrategy = RetryType.LOCAL_REMOTE,
            localTimes = 3,
            localInterval = 2,
            bizNo = "param.payNo",
            retryCompleteCallback = io.github.atengk.snailjob.callback.PayRetryCompleteCallback.class
    )
    public void notifyMerchant(PayCallbackRetryDTO param) {
        validateParam(param);

        String body = JSONUtil.toJsonStr(param);
        log.info("开始通知商户支付结果，payNo={}，callbackUrl={}", param.getPayNo(), param.getCallbackUrl());

        try (HttpResponse response = HttpRequest.post(param.getCallbackUrl())
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(body)
                .timeout(5000)
                .execute()) {

            if (!response.isOk()) {
                log.warn("通知商户支付结果失败，payNo={}，httpStatus={}，body={}",
                        param.getPayNo(), response.getStatus(), response.body());
                throw new IllegalStateException("商户回调接口响应异常：" + response.getStatus());
            }

            log.info("通知商户支付结果成功，payNo={}，response={}", param.getPayNo(), response.body());
        }
    }

    /**
     * 校验支付回调参数
     *
     * @param param 支付回调参数
     */
    private void validateParam(PayCallbackRetryDTO param) {
        if (param == null) {
            throw new IllegalArgumentException("支付回调参数不能为空");
        }
        if (StrUtil.isBlank(param.getPayNo())) {
            throw new IllegalArgumentException("支付单号不能为空");
        }
        if (StrUtil.isBlank(param.getCallbackUrl())) {
            throw new IllegalArgumentException("商户回调地址不能为空");
        }
    }

}
```

对外提供一个测试接口，用于模拟业务触发重试任务。生产代码中通常由支付成功事件、MQ 消费逻辑或订单状态变更逻辑调用 `PayCallbackRetryService`，不一定需要暴露该接口。

文件位置：`src/main/java/io/github/atengk/snailjob/controller/PayCallbackTestController.java`

```java
package io.github.atengk.snailjob.controller;

import io.github.atengk.snailjob.model.dto.PayCallbackRetryDTO;
import io.github.atengk.snailjob.retry.PayCallbackRetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付回调重试测试接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/snail-job/pay-callback")
public class PayCallbackTestController {

    private final PayCallbackRetryService payCallbackRetryService;

    /**
     * 触发支付回调重试测试
     *
     * @param param 支付回调参数
     * @return 处理结果
     */
    @PostMapping("/notify")
    public String notifyMerchant(@RequestBody PayCallbackRetryDTO param) {
        payCallbackRetryService.notifyMerchant(param);
        return "已触发支付回调通知";
    }

}
```

接口测试命令如下：

```bash
curl -X POST 'http://localhost:8081/snail-job/pay-callback/notify' \
  -H 'Content-Type: application/json' \
  -d '{
    "payNo": "PAY202605070001",
    "orderNo": "ORDER202605070001",
    "merchantNo": "M10001",
    "amount": 99.90,
    "callbackUrl": "http://127.0.0.1:9999/mock/pay/callback"
  }'
```

执行后，如果回调地址不可用或返回非 2xx 状态，方法会抛出异常并触发 SnailJob 重试。可以在 SnailJob 管理端的重试任务列表中按 `payNo` 或重试场景 `payCallbackRetry` 检索记录。

### 回调任务开发

回调任务主要分为两类。第一类是重试完成回调，例如重试成功或达到最大重试次数后更新业务表状态；第二类是工作流回调节点，例如工作流执行到指定节点后调用业务系统的 HTTP 接口。SnailJob 文档说明可通过 `retryCompleteCallback` 自定义重试完成后的回调逻辑，工作流回调节点也支持通过 HTTP 接口接收上下文信息。([Snail Job](https://snailjob.opensnail.com/docs/guide/retry/business_params_retryCompleteCallback.html?utm_source=chatgpt.com))

下面先给出重试完成回调示例。该回调用于在支付回调重试成功时标记业务状态为成功，在达到最大重试次数时标记为失败并等待人工处理。

文件位置：`src/main/java/io/github/atengk/snailjob/service/PayCallbackRecordService.java`

```java
package io.github.atengk.snailjob.service;

import io.github.atengk.snailjob.model.dto.PayCallbackRetryDTO;

/**
 * 支付回调记录服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface PayCallbackRecordService {

    /**
     * 标记回调成功
     *
     * @param param 支付回调参数
     */
    void markSuccess(PayCallbackRetryDTO param);

    /**
     * 标记回调失败
     *
     * @param param 支付回调参数
     * @param reason 失败原因
     */
    void markFailed(PayCallbackRetryDTO param, String reason);

}
```

下面的回调类实现 SnailJob 的重试完成回调接口。`params` 是原始业务方法参数数组，因此需要按业务方法签名进行类型转换。

文件位置：`src/main/java/io/github/atengk/snailjob/callback/PayRetryCompleteCallback.java`

```java
package io.github.atengk.snailjob.callback;

import cn.hutool.core.util.ArrayUtil;
import com.aizuda.snailjob.client.core.callback.complete.RetryCompleteCallback;
import io.github.atengk.snailjob.model.dto.PayCallbackRetryDTO;
import io.github.atengk.snailjob.service.PayCallbackRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付回调重试完成回调
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayRetryCompleteCallback implements RetryCompleteCallback {

    private final PayCallbackRecordService payCallbackRecordService;

    /**
     * 重试成功回调
     *
     * @param sceneName 场景名称
     * @param executorName 执行器名称
     * @param params 原始方法参数
     */
    @Override
    public void doSuccessCallback(String sceneName, String executorName, Object[] params) {
        PayCallbackRetryDTO param = parseParam(params);
        if (param == null) {
            log.warn("支付回调重试成功，但回调参数为空，sceneName={}，executorName={}", sceneName, executorName);
            return;
        }

        payCallbackRecordService.markSuccess(param);
        log.info("支付回调重试成功，已更新业务状态，sceneName={}，payNo={}", sceneName, param.getPayNo());
    }

    /**
     * 达到最大重试次数回调
     *
     * @param sceneName 场景名称
     * @param executorName 执行器名称
     * @param params 原始方法参数
     */
    @Override
    public void doMaxRetryCallback(String sceneName, String executorName, Object[] params) {
        PayCallbackRetryDTO param = parseParam(params);
        if (param == null) {
            log.warn("支付回调达到最大重试次数，但回调参数为空，sceneName={}，executorName={}", sceneName, executorName);
            return;
        }

        payCallbackRecordService.markFailed(param, "支付回调达到最大重试次数");
        log.error("支付回调达到最大重试次数，已标记失败，sceneName={}，payNo={}", sceneName, param.getPayNo());
    }

    /**
     * 解析原始方法参数
     *
     * @param params 原始方法参数
     * @return 支付回调参数
     */
    private PayCallbackRetryDTO parseParam(Object[] params) {
        if (ArrayUtil.isEmpty(params) || !(params[0] instanceof PayCallbackRetryDTO)) {
            return null;
        }
        return (PayCallbackRetryDTO) params[0];
    }

}
```

下面给出工作流 HTTP 回调节点示例。该接口用于接收 SnailJob 工作流回调节点发送的上下文参数，业务系统可以在接口中更新审批状态、发送通知或触发后续业务处理。

文件位置：`src/main/java/io/github/atengk/snailjob/callback/WorkflowCallbackController.java`

```java
package io.github.atengk.snailjob.callback;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SnailJob 工作流回调接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestController
@RequestMapping("/snail-job/workflow/callback")
public class WorkflowCallbackController {

    /**
     * 接收工作流回调
     *
     * @param headers 请求头
     * @param body 回调请求体
     * @return 处理结果
     */
    @PostMapping("/order")
    public String orderWorkflowCallback(@RequestHeader HttpHeaders headers,
                                        @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> callbackBody = MapUtil.emptyIfNull(body);

        log.info("收到 SnailJob 工作流回调，请求头={}，请求体={}",
                JSONUtil.toJsonStr(headers), JSONUtil.toJsonStr(callbackBody));

        String orderNo = MapUtil.getStr(callbackBody, "orderNo");
        String workflowStatus = MapUtil.getStr(callbackBody, "workflowStatus");

        if (MapUtil.isEmpty(callbackBody)) {
            log.warn("SnailJob 工作流回调请求体为空");
            return "EMPTY_BODY";
        }

        log.info("处理订单工作流回调，orderNo={}，workflowStatus={}", orderNo, workflowStatus);

        // 这里可以调用业务 Service 更新订单流程状态、发送通知或记录审计日志
        return "SUCCESS";
    }

}
```

工作流回调节点配置建议如下：

| 配置项   | 示例值                                                       | 说明                           |
| -------- | ------------------------------------------------------------ | ------------------------------ |
| 回调地址 | `http://order-service:8081/snail-job/workflow/callback/order` | 服务端可访问的业务接口地址     |
| 请求方式 | POST                                                         | 建议统一使用 POST              |
| 参数格式 | JSON                                                         | 便于扩展上下文字段             |
| 超时时间 | 10 秒                                                        | 避免回调节点长期阻塞           |
| 失败策略 | 阻塞或跳过                                                   | 关键业务回调建议阻塞并人工处理 |

## 业务集成方案

本章节用于说明 SnailJob 与业务系统如何衔接，包括任务触发方式、参数传递、状态处理、异常处理和日志处理。业务集成的核心原则是：SnailJob 负责任务调度和重试控制，业务系统负责业务状态、幂等控制和数据一致性。

### 任务触发方式

任务触发方式需要根据业务场景选择。周期性任务适合由 SnailJob 管理端调度，失败补偿适合通过业务代码触发 `@Retryable` 方法，临时处理或运维操作适合在管理端手动触发。官方快速开始示例中，定时任务由管理端配置和触发，重试任务则通过业务接口调用带 `@Retryable` 的方法触发。([Snail Job](https://snailjob.opensnail.com/docs/quickstart/quick_start.html?utm_source=chatgpt.com))

| 触发方式       | 适用场景                       | 示例                       |
| -------------- | ------------------------------ | -------------------------- |
| 管理端手动触发 | 临时补偿、数据修复、一次性任务 | 手动执行订单数据清理       |
| 管理端定时触发 | 周期任务、批处理任务           | 每日订单统计               |
| 业务代码触发   | 失败重试、接口补偿             | 支付回调失败重试           |
| MQ 消费触发    | 消息补偿、异步任务             | 消费订单创建消息后触发通知 |
| 工作流触发     | 多步骤编排任务                 | 审批完成后执行发放权益任务 |

业务代码触发示例：

文件位置：`src/main/java/io/github/atengk/snailjob/service/impl/PayOrderServiceImpl.java`

```java
package io.github.atengk.snailjob.service.impl;

import io.github.atengk.snailjob.model.dto.PayCallbackRetryDTO;
import io.github.atengk.snailjob.retry.PayCallbackRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付订单服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderServiceImpl {

    private final PayCallbackRetryService payCallbackRetryService;

    /**
     * 支付成功后处理业务逻辑
     *
     * @param param 支付回调参数
     */
    public void handlePaySuccess(PayCallbackRetryDTO param) {
        log.info("开始处理支付成功逻辑，payNo={}，orderNo={}", param.getPayNo(), param.getOrderNo());

        // 1. 更新支付单状态
        // 2. 更新订单状态
        // 3. 触发商户回调，失败时进入 SnailJob 重试
        payCallbackRetryService.notifyMerchant(param);

        log.info("支付成功逻辑处理完成，payNo={}，orderNo={}", param.getPayNo(), param.getOrderNo());
    }

}
```

使用建议如下：

| 业务类型           | 推荐触发方式   | 说明                     |
| ------------------ | -------------- | ------------------------ |
| 每天固定执行       | 管理端定时触发 | 由 SnailJob 统一调度     |
| 用户操作后异步补偿 | 业务代码触发   | 方法失败后进入重试       |
| 大批量数据修复     | 管理端手动触发 | 参数控制范围，避免误处理 |
| 多步骤依赖任务     | 工作流触发     | 节点之间传递上下文       |
| 外部系统通知       | 回调接口触发   | 需要鉴权和幂等校验       |

### 任务参数传递

任务参数建议统一使用 JSON 字符串传递，并映射到明确的 DTO。这样可以避免参数含义不清、字段顺序依赖和后续扩展困难。SnailJob 的 `JobArgs` 中包含任务参数，工作流任务还可以通过上下文传递数据，官方工作流示例中也展示了 `jobArgs.getJobParams()`、`jobArgs.appendContext()` 和 `jobArgs.getWfContext()` 的用法。([Snail Job](https://snailjob.opensnail.com/docs/guide/workflow/wf_callback_node.html?utm_source=chatgpt.com))

参数设计建议如下：

| 参数类型     | 传递方式           | 示例                            |
| ------------ | ------------------ | ------------------------------- |
| 简单任务参数 | JSON DTO           | `{"beforeDays":30}`             |
| 业务唯一标识 | 单独字段           | `orderNo`、`payNo`、`messageId` |
| 时间范围     | 明确开始和结束时间 | `startTime`、`endTime`          |
| 幂等标识     | 业务唯一键         | `idempotentKey`                 |
| 工作流上下文 | `appendContext`    | 上一节点处理结果传给下一节点    |

下面给出一个统一参数解析工具，减少每个执行器中重复编写 JSON 解析和默认值处理逻辑。

文件位置：`src/main/java/io/github/atengk/snailjob/support/SnailJobParamParser.java`

```java
package io.github.atengk.snailjob.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * SnailJob 任务参数解析工具
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
public final class SnailJobParamParser {

    private SnailJobParamParser() {
    }

    /**
     * 解析任务参数
     *
     * @param jobParams 原始任务参数
     * @param targetType 目标类型
     * @param <T> 参数类型
     * @return 参数对象
     */
    public static <T> T parse(String jobParams, Class<T> targetType) {
        if (StrUtil.isBlank(jobParams)) {
            log.warn("SnailJob 任务参数为空，targetType={}", targetType.getSimpleName());
            return null;
        }

        try {
            return JSONUtil.toBean(jobParams, targetType);
        } catch (Exception e) {
            log.error("SnailJob 任务参数解析失败，targetType={}，jobParams={}", targetType.getSimpleName(), jobParams, e);
            throw new IllegalArgumentException("任务参数解析失败：" + e.getMessage(), e);
        }
    }

}
```

任务参数示例：

```json
{
  "orderNo": "ORDER202605070001",
  "tenantId": "T10001",
  "source": "SYSTEM"
}
```

使用约定如下：

| 约定                   | 说明                              |
| ---------------------- | --------------------------------- |
| 字段名稳定             | 已上线任务参数字段不要随意改名    |
| 参数可为空时给默认值   | 如统计日期为空时默认昨天          |
| 关键字段必须校验       | 如订单号、支付单号、回调地址      |
| 参数变化需要兼容旧任务 | 新增字段优先使用非必填            |
| 不传敏感信息           | Token、密码、密钥不应放入任务参数 |

### 任务状态处理

任务状态处理应区分 SnailJob 调度状态和业务状态。SnailJob 管理端关注任务是否调度成功、是否执行失败、是否重试完成；业务系统关注订单、支付、消息、报表等业务对象是否处理完成。两类状态不能混用，也不建议业务系统直接修改 SnailJob 内部表。

建议业务侧维护任务记录表，用于关联业务数据与任务执行结果。

```sql
CREATE TABLE biz_task_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型，例如 ORDER_CLOSE、PAY_CALLBACK',
    biz_no VARCHAR(128) NOT NULL COMMENT '业务编号，例如订单号、支付单号',
    task_status VARCHAR(32) NOT NULL COMMENT '业务任务状态：PENDING、RUNNING、SUCCESS、FAILED、CANCELED',
    retry_scene VARCHAR(128) DEFAULT NULL COMMENT 'SnailJob 重试场景',
    executor_name VARCHAR(128) DEFAULT NULL COMMENT 'SnailJob 执行器名称',
    last_error_message VARCHAR(1000) DEFAULT NULL COMMENT '最后一次失败原因',
    last_execute_time DATETIME DEFAULT NULL COMMENT '最后执行时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_biz_type_biz_no (biz_type, biz_no),
    KEY idx_task_status (task_status),
    KEY idx_retry_scene (retry_scene),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务任务记录表';
```

状态流转建议如下：

| 当前状态  | 触发动作         | 目标状态                   |
| --------- | ---------------- | -------------------------- |
| `PENDING` | 开始执行         | `RUNNING`                  |
| `RUNNING` | 执行成功         | `SUCCESS`                  |
| `RUNNING` | 执行失败且可重试 | `PENDING` 或保持 `RUNNING` |
| `RUNNING` | 达到最大重试次数 | `FAILED`                   |
| `PENDING` | 人工取消         | `CANCELED`                 |
| `FAILED`  | 人工重新触发     | `PENDING`                  |

业务状态更新应使用幂等更新条件。例如关闭订单时，只允许从 `WAIT_PAY` 更新为 `CLOSED`；支付回调时，只允许未通知成功的数据再次通知；报表统计时，使用统计日期作为唯一键，避免重复插入。

下面给出状态更新接口示例。

文件位置：`src/main/java/io/github/atengk/snailjob/service/BizTaskRecordService.java`

```java
package io.github.atengk.snailjob.service;

/**
 * 业务任务记录服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface BizTaskRecordService {

    /**
     * 标记任务执行中
     *
     * @param bizType 业务类型
     * @param bizNo 业务编号
     */
    void markRunning(String bizType, String bizNo);

    /**
     * 标记任务成功
     *
     * @param bizType 业务类型
     * @param bizNo 业务编号
     */
    void markSuccess(String bizType, String bizNo);

    /**
     * 标记任务失败
     *
     * @param bizType 业务类型
     * @param bizNo 业务编号
     * @param errorMessage 错误信息
     */
    void markFailed(String bizType, String bizNo, String errorMessage);

}
```

在执行器中使用状态服务时，应保证异常路径也能更新业务状态。

```java
try {
    bizTaskRecordService.markRunning("ORDER_CLOSE", param.getOrderNo());
    orderTaskService.closeTimeoutOrder(param);
    bizTaskRecordService.markSuccess("ORDER_CLOSE", param.getOrderNo());
    return ExecuteResult.success("订单关闭成功");
} catch (Exception e) {
    bizTaskRecordService.markFailed("ORDER_CLOSE", param.getOrderNo(), e.getMessage());
    log.error("订单关闭任务执行失败，orderNo={}", param.getOrderNo(), e);
    return ExecuteResult.failure("订单关闭失败：" + e.getMessage());
}
```

### 异常与日志处理

异常处理的核心目标是让 SnailJob 能准确感知任务是否成功，同时让业务系统能定位失败原因。对于 `@JobExecutor` 任务，业务异常应转换为 `ExecuteResult.failure()`；对于 `@Retryable` 方法，需要抛出异常才能触发重试。SnailJob 的任务重试文档说明，Job 任务重试触发时机包括网络超时、客户端明确返回错误和客户端出现异常。([Snail Job](https://snailjob.opensnail.com/docs/guide/job/job_retry.html?utm_source=chatgpt.com))

异常处理建议如下：

| 场景           | 处理方式                 | 说明                             |
| -------------- | ------------------------ | -------------------------------- |
| 参数错误       | 直接失败，不重试         | 返回明确错误信息                 |
| 业务状态不满足 | 返回成功或失败取决于语义 | 如订单已支付，关闭任务可返回成功 |
| 第三方接口失败 | 抛出异常或返回失败       | 适合进入重试                     |
| 数据库异常     | 抛出异常或返回失败       | 需要保留堆栈日志                 |
| 重复执行       | 幂等跳过并返回成功       | 避免重复副作用                   |

日志处理建议同时满足本地排查和管理端排查。前面已经配置 `logback-boot.xml` 时，普通 `log.info`、`log.warn`、`log.error` 可以通过 SnailJob Logback Appender 上报到管理端；官方快速开始示例也展示了任务中记录日志并在管理端查看执行结果的流程。([Snail Job](https://snailjob.opensnail.com/docs/quickstart/quick_start.html?utm_source=chatgpt.com))

推荐日志字段如下：

| 字段       | 示例                      | 说明               |
| ---------- | ------------------------- | ------------------ |
| 业务编号   | `orderNo`、`payNo`        | 定位单笔业务       |
| 场景名称   | `payCallbackRetry`        | 定位重试场景       |
| 执行器名称 | `orderCleanupJobExecutor` | 定位任务代码       |
| 参数摘要   | JSON 参数                 | 不打印敏感字段     |
| 结果状态   | 成功、失败、跳过          | 便于检索           |
| 异常堆栈   | `log.error(..., e)`       | 保留完整错误上下文 |

统一异常处理示例：

文件位置：`src/main/java/io/github/atengk/snailjob/support/SnailJobExecuteTemplate.java`

```java
package io.github.atengk.snailjob.support;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * SnailJob 执行模板
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
public final class SnailJobExecuteTemplate {

    private SnailJobExecuteTemplate() {
    }

    /**
     * 执行任务并统一转换结果
     *
     * @param executorName 执行器名称
     * @param bizNo 业务编号
     * @param supplier 任务逻辑
     * @return 执行结果
     */
    public static ExecuteResult execute(String executorName, String bizNo, Supplier<String> supplier) {
        try {
            log.info("开始执行 SnailJob 任务，executorName={}，bizNo={}", executorName, bizNo);

            String message = supplier.get();
            String resultMessage = StrUtil.blankToDefault(message, "执行成功");

            log.info("SnailJob 任务执行成功，executorName={}，bizNo={}，message={}",
                    executorName, bizNo, resultMessage);
            return ExecuteResult.success(resultMessage);
        } catch (Exception e) {
            log.error("SnailJob 任务执行失败，executorName={}，bizNo={}", executorName, bizNo, e);
            return ExecuteResult.failure("任务执行失败：" + e.getMessage());
        }
    }

}
```

执行器中使用模板：

```java
@JobExecutor(name = "dailyOrderStatisticsJobExecutor")
public ExecuteResult statistics(JobArgs jobArgs) {
    return SnailJobExecuteTemplate.execute(
            "dailyOrderStatisticsJobExecutor",
            "ORDER_STATISTICS",
            () -> {
                OrderStatisticsParamDTO param = buildParam(jobArgs.getJobParams());
                return orderStatisticsService.statistics(param);
            }
    );
}
```

异常与日志处理的最终要求是：任务失败不能静默吞掉，参数错误不能无限重试，重复执行不能造成重复扣款、重复发券、重复发消息等副作用。生产环境应结合业务任务表、SnailJob 管理端执行日志和应用日志平台共同排查问题。

## 管理端使用

本章节用于说明 SnailJob 管理端中与 Spring Boot 客户端集成相关的核心配置，包括命名空间、组、任务和执行日志。管理端配置应与 `application.yml` 中的 `snail-job.namespace`、`snail-job.group`、`snail-job.token`、执行器名称保持一致，否则客户端虽然可以启动，但任务无法被正确识别、调度或回调。

### 命名空间配置

命名空间用于隔离不同环境、不同业务域或不同租户下的任务数据。开发、测试、生产环境应使用不同命名空间，避免测试任务误调度生产客户端。SnailJob 快速开始文档中也说明，项目启动后可以创建自己的命名空间，如果不创建也可以使用默认命名空间；创建后需要切换到对应命名空间视角查看数据。([Snail Job](https://snailjob.opensnail.com/docs/quickstart/quick_start.html?utm_source=chatgpt.com))

推荐命名空间设计如下：

| 环境     | 命名空间名称      | namespace 示例       | 说明               |
| -------- | ----------------- | -------------------- | ------------------ |
| 开发环境 | `dev_snail_job`   | `dev_namespace_id`   | 开发联调、个人测试 |
| 测试环境 | `test_snail_job`  | `test_namespace_id`  | 测试环境回归验证   |
| 预发环境 | `stage_snail_job` | `stage_namespace_id` | 上线前验证         |
| 生产环境 | `prod_snail_job`  | `prod_namespace_id`  | 生产任务调度       |

配置步骤建议如下：

| 步骤 | 操作                 | 注意事项                           |
| ---- | -------------------- | ---------------------------------- |
| 1    | 登录 SnailJob 管理端 | 默认账号密码上线后必须修改         |
| 2    | 进入命名空间管理     | 按环境创建命名空间                 |
| 3    | 新增命名空间         | 名称建议包含环境含义               |
| 4    | 复制 namespace       | 写入 Spring Boot 客户端配置        |
| 5    | 切换命名空间         | 后续组、任务、日志都在该视角下管理 |

Spring Boot 客户端配置示例：

```yaml
snail-job:
  # 当前环境对应的命名空间 ID，需要与管理端命名空间保持一致
  namespace: prod_namespace_id

  # 当前业务服务分组
  group: order-service

  # 当前分组 Token
  token: ${SNAIL_JOB_TOKEN}
```

命名空间使用建议如下：

| 建议                     | 说明                                               |
| ------------------------ | -------------------------------------------------- |
| 环境隔离优先用 namespace | 不建议通过 group 名称拼接 `dev`、`prod` 来隔离环境 |
| 生产 namespace 单独授权  | 避免开发人员误操作生产任务                         |
| 任务迁移要同步 namespace | 从测试迁移到生产时，需要重新确认 namespace         |
| 命名空间不要频繁变更     | 变更后客户端配置、组配置、任务配置都需要同步调整   |

### 组配置

组用于管理某个业务服务下的任务，相当于业务应用的 AppId。SnailJob 官方组管理文档说明，可以针对不同模块创建多个分组；组名称必须与客户端配置中的 `snail-job.group` 一致，Token 也必须与客户端配置中的 `snail-job.token` 一致。文档同时提醒，删除分组是物理删除，操作后无法恢复；如果组状态关闭，该组下面的定时任务和重试任务都不会执行。([Snail Job](https://snailjob.opensnail.com/docs/guide/common/group_manage.html?utm_source=chatgpt.com))

推荐按 Spring Boot 服务维度创建分组：

| Spring Boot 服务 | SnailJob 组名     | 说明                             |
| ---------------- | ----------------- | -------------------------------- |
| 订单服务         | `order-service`   | 订单关闭、订单统计、订单状态同步 |
| 支付服务         | `pay-service`     | 支付回调、退款同步、对账任务     |
| 消息服务         | `message-service` | 短信补发、站内信推送、MQ 补偿    |
| 会员服务         | `member-service`  | 会员等级刷新、积分结算、权益发放 |

组配置字段建议如下：

| 配置项     | 示例值          | 说明                                |
| ---------- | --------------- | ----------------------------------- |
| 组名称     | `order-service` | 必须与客户端 `snail-job.group` 一致 |
| Token      | `SJ_xxxxxxxxxx` | 必须与客户端 `snail-job.token` 一致 |
| 状态       | 启用            | 停用后该组任务不会执行              |
| 描述       | 订单服务任务组  | 说明该组负责的业务范围              |
| 初始化场景 | 按需开启        | 用于快速初始化重试场景              |

客户端对应配置：

```yaml
snail-job:
  # 当前服务对应的 SnailJob 分组
  group: order-service

  # 当前分组 Token，生产环境建议通过环境变量或配置中心注入
  token: ${SNAIL_JOB_TOKEN}
```

组配置完成后，需要启动 Spring Boot 客户端，在管理端查看在线节点。如果节点未出现，应优先检查服务端地址、通信端口、namespace、group、token 和客户端网络是否一致。

### 任务配置

任务配置用于定义任务如何触发、调度到哪个执行器、失败后如何处理以及执行超时时间。SnailJob 定时任务文档中列出了任务类型、阻塞策略、执行器类型、执行器名称、触发类型、触发间隔、路由策略、超时时间、重试相关、标签等配置项；任务调度能力也支持集群、广播、静态分片、Map、MapReduce 等执行模式。([Snail Job](https://snailjob.opensnail.com/docs/guide/job/job_task.html?utm_source=chatgpt.com))

常用任务配置项如下：

| 配置项       | 示例值                            | 说明                               |
| ------------ | --------------------------------- | ---------------------------------- |
| 任务名称     | 每日订单统计                      | 管理端展示名称                     |
| 组名称       | `order-service`                   | 选择当前业务服务分组               |
| 任务类型     | 集群                              | 多实例部署时只选择一个节点执行     |
| 执行器类型   | Java                              | Spring Boot Java 客户端任务        |
| 执行器名称   | `dailyOrderStatisticsJobExecutor` | 对应 `@JobExecutor(name = "...")`  |
| 方法参数     | JSON 字符串                       | 通过 `JobArgs#getJobParams()` 获取 |
| 路由策略     | 轮询 / 第一个 / 一致性哈希        | 控制任务分发到哪个客户端节点       |
| 阻塞策略     | 丢弃 / 覆盖 / 并行 / 恢复         | 控制上次任务未完成时如何处理       |
| 触发类型     | CRON / 固定时间 / 固定频率        | 根据任务周期选择                   |
| 超时时间     | 600 秒                            | 避免长时间占用执行线程             |
| 最大重试次数 | 1 到 3                            | 根据任务重要性配置                 |
| 重试间隔     | 10 秒                             | 根据业务恢复时间配置               |

执行器名称是任务配置中最容易出错的字段。SnailJob 执行器名称文档说明，使用注解方式时，管理端配置的执行器名称与 `@JobExecutor` 注解中的 `name` 值保持一致即可；方法参数可以通过 `JobArgs` 对象获取。([Snail Job](https://snailjob.opensnail.com/docs/guide/job/job_execute_name.html?utm_source=chatgpt.com))

例如代码中定义：

```java
@JobExecutor(name = "dailyOrderStatisticsJobExecutor")
public ExecuteResult statistics(JobArgs jobArgs) {
    // 任务处理逻辑
}
```

管理端任务配置中应填写：

```text
执行器名称：dailyOrderStatisticsJobExecutor
```

不同任务类型的选择建议如下：

| 任务类型  | 适用场景               | 示例                       |
| --------- | ---------------------- | -------------------------- |
| 集群      | 多实例只需一个节点执行 | 每日订单统计、订单超时关闭 |
| 广播      | 所有在线节点都要执行   | 本地缓存刷新、本地文件清理 |
| 静态分片  | 固定分片并行处理       | 大批量订单扫描             |
| Map       | 动态拆分子任务         | 按门店、租户、日期拆分任务 |
| MapReduce | 拆分后汇总结果         | 多分片统计后汇总报表       |

路由策略应根据业务特性选择。SnailJob 路由策略文档列出了最后一个、第一个、轮询、LRU、随机、一致性哈希等策略；例如轮询会在多个客户端之间轮流命中，一致性哈希会相对稳定地调度到某个客户端节点。([Snail Job](https://snailjob.opensnail.com/docs/guide/job/job_router_strategy.html?utm_source=chatgpt.com))

阻塞策略需要谨慎配置。对于统计类、清理类任务，不建议并行执行；对于可重复消费且幂等性强的任务，可以按业务承受能力选择并行。SnailJob 的阻塞策略文档以“丢弃”策略为例说明，当上一个任务未结束时，服务端会触发阻塞策略；该策略在一定程度上可以减少重复执行，但不能替代业务幂等。([Snail Job](https://snailjob.opensnail.com/docs/guide/job/block_strategy/discard.html?utm_source=chatgpt.com))

推荐配置示例：

| 任务           | 任务类型   | 路由策略      | 阻塞策略   | 最大重试次数 | 说明                   |
| -------------- | ---------- | ------------- | ---------- | ------------ | ---------------------- |
| 每日订单统计   | 集群       | 第一个 / 轮询 | 丢弃       | 1            | 避免重复统计           |
| 订单超时关闭   | 集群       | 轮询          | 丢弃       | 3            | 需要幂等关闭           |
| 本地缓存刷新   | 广播       | 无需关注      | 并行或丢弃 | 0            | 每个节点都刷新         |
| 支付回调补偿   | 重试任务   | 按重试场景    | 不适用     | 按场景配置   | 通过 `@Retryable` 触发 |
| 大批量数据处理 | 分片 / Map | 一致性哈希    | 丢弃       | 1            | 避免全量堆积           |

### 执行日志查看

执行日志用于排查任务是否被调度、客户端是否收到请求、业务代码是否执行成功以及失败原因是什么。SnailJob 集群任务文档中的验证结果说明，任务调度后客户端本地日志会正常打印，服务端线上日志也会正常记录；前提是客户端日志配置和任务执行器日志输出已经正确接入。([Snail Job](https://snailjob.opensnail.com/docs/guide/job/task_type/cluster.html?utm_source=chatgpt.com))

执行日志排查建议按以下顺序进行：

| 排查项   | 查看位置              | 说明                             |
| -------- | --------------------- | -------------------------------- |
| 任务批次 | 管理端任务批次列表    | 确认任务是否被触发               |
| 执行节点 | 批次详情 / 客户端节点 | 确认任务调度到哪个实例           |
| 执行状态 | 执行记录              | 查看成功、失败、超时、取消等状态 |
| 入参     | 执行详情              | 确认任务参数是否正确             |
| 业务日志 | 在线日志 / 应用日志   | 查看业务执行过程                 |
| 异常堆栈 | 应用日志平台          | 定位具体代码异常                 |
| 重试记录 | 重试任务列表          | 查看失败后是否进入重试           |

日志查看时应重点关注以下字段：

| 字段                | 说明                           |
| ------------------- | ------------------------------ |
| 任务名称            | 管理端配置的任务名称           |
| 执行器名称          | 对应 `@JobExecutor` 的名称     |
| 组名称              | 对应客户端 `snail-job.group`   |
| 任务参数            | 任务执行时传入的 JSON 参数     |
| 执行节点            | 实际执行任务的客户端实例       |
| 开始时间 / 结束时间 | 判断是否超时或长时间运行       |
| 执行结果            | 成功、失败、超时、取消         |
| 失败原因            | 业务异常、网络异常、参数异常等 |

如果日志无法在管理端查看，应优先确认 `logback-boot.xml` 中是否配置了 SnailJob 的 Logback Appender，以及任务中是否输出了有效日志。生产环境还需要配置日志保留策略。SnailJob 日志清理文档说明，开源版本支持通过全局 `log-storage` 参数统一配置日志保留天数，默认保留 7 天；PRO 版本支持按任务维度配置日志清理策略。([Snail Job](https://snailjob.opensnail.com/docs/guide/common/log_clear.html?utm_source=chatgpt.com))

服务端日志保留配置示例：

```yaml
snail-job:
  # 服务端日志保留天数，开源版本为全局配置
  log-storage: 30
```

管理端操作日志也可以用于审计任务配置变更。SnailJob 系统操作日志文档说明，操作日志记录用户操作、系统自动操作等行为，并包含操作人、操作时间、操作 IP、操作结果等信息；其模块范围包括定时任务、客户端节点、命名空间、组管理、任务批次、任务执行器、重试任务、场景配置、工作流等。([Snail Job](https://snailjob.opensnail.com/docs/guide/common/system_operation_log.html?utm_source=chatgpt.com))

## 接口与数据设计

本章节用于说明业务系统侧如何设计任务相关数据表、关联字段和管理接口。SnailJob 负责调度、重试、执行日志和管理端配置；业务系统负责保存业务单据、业务状态、幂等键、人工处理状态和对外管理接口。业务系统不应直接修改 SnailJob 内部表，避免破坏调度状态和升级兼容性。

### 业务表设计

业务表设计的核心是将“业务对象”和“任务执行记录”解耦。订单、支付、消息等业务表只保存本身状态；统一任务记录表保存任务执行过程、重试场景、执行器名称、失败原因和人工处理状态。这样可以避免在每张业务表中堆积大量任务字段，也便于统一查询和运维。

推荐业务任务记录表如下：

```sql
CREATE TABLE biz_task_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型，例如 ORDER_CLOSE、PAY_CALLBACK、MESSAGE_SEND',
    biz_no VARCHAR(128) NOT NULL COMMENT '业务编号，例如订单号、支付单号、消息编号',
    task_name VARCHAR(128) NOT NULL COMMENT '业务任务名称',
    task_status VARCHAR(32) NOT NULL COMMENT '任务状态：PENDING、RUNNING、SUCCESS、FAILED、CANCELED',
    namespace_id VARCHAR(128) DEFAULT NULL COMMENT 'SnailJob 命名空间 ID',
    group_name VARCHAR(128) DEFAULT NULL COMMENT 'SnailJob 组名称',
    executor_name VARCHAR(128) DEFAULT NULL COMMENT 'SnailJob 执行器名称',
    retry_scene VARCHAR(128) DEFAULT NULL COMMENT 'SnailJob 重试场景',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '业务侧记录的重试次数',
    max_retry_count INT NOT NULL DEFAULT 0 COMMENT '业务侧允许的最大重试次数',
    idempotent_key VARCHAR(256) DEFAULT NULL COMMENT '幂等键',
    task_param JSON DEFAULT NULL COMMENT '任务参数 JSON',
    last_error_message VARCHAR(1000) DEFAULT NULL COMMENT '最后一次失败原因',
    last_execute_time DATETIME DEFAULT NULL COMMENT '最后执行时间',
    next_execute_time DATETIME DEFAULT NULL COMMENT '下次计划执行时间，业务侧可选',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_biz_type_biz_no (biz_type, biz_no),
    UNIQUE KEY uk_idempotent_key (idempotent_key),
    KEY idx_task_status (task_status),
    KEY idx_group_executor (group_name, executor_name),
    KEY idx_retry_scene (retry_scene),
    KEY idx_last_execute_time (last_execute_time),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务任务记录表';
```

如果支付回调、消息发送等场景需要保留每次执行明细，可以增加任务执行明细表：

```sql
CREATE TABLE biz_task_execute_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    task_record_id BIGINT NOT NULL COMMENT '业务任务记录 ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    biz_no VARCHAR(128) NOT NULL COMMENT '业务编号',
    execute_status VARCHAR(32) NOT NULL COMMENT '执行状态：SUCCESS、FAILED',
    execute_message VARCHAR(1000) DEFAULT NULL COMMENT '执行结果描述',
    error_stack TEXT DEFAULT NULL COMMENT '异常堆栈摘要',
    execute_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    cost_millis BIGINT DEFAULT NULL COMMENT '执行耗时，单位毫秒',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_task_record_id (task_record_id),
    KEY idx_biz_type_biz_no (biz_type, biz_no),
    KEY idx_execute_status (execute_status),
    KEY idx_execute_time (execute_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务任务执行日志表';
```

任务状态建议统一使用枚举值：

| 状态       | 说明     | 典型触发点                     |
| ---------- | -------- | ------------------------------ |
| `PENDING`  | 待执行   | 创建任务记录、人工重新触发     |
| `RUNNING`  | 执行中   | 执行器开始处理                 |
| `SUCCESS`  | 执行成功 | 业务处理完成                   |
| `FAILED`   | 执行失败 | 达到最大重试次数或不可恢复失败 |
| `CANCELED` | 已取消   | 人工取消或业务状态已终止       |

如果项目已经接入 MyBatis-Plus，可以使用以下实体类映射 `biz_task_record` 表。

文件位置：`src/main/java/io/github/atengk/snailjob/entity/BizTaskRecord.java`

```java
package io.github.atengk.snailjob.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务任务记录实体
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@TableName("biz_task_record")
public class BizTaskRecord {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务编号
     */
    private String bizNo;

    /**
     * 业务任务名称
     */
    private String taskName;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * SnailJob 命名空间 ID
     */
    private String namespaceId;

    /**
     * SnailJob 组名称
     */
    private String groupName;

    /**
     * SnailJob 执行器名称
     */
    private String executorName;

    /**
     * SnailJob 重试场景
     */
    private String retryScene;

    /**
     * 业务侧记录的重试次数
     */
    private Integer retryCount;

    /**
     * 业务侧允许的最大重试次数
     */
    private Integer maxRetryCount;

    /**
     * 幂等键
     */
    private String idempotentKey;

    /**
     * 任务参数 JSON
     */
    private String taskParam;

    /**
     * 最后一次失败原因
     */
    private String lastErrorMessage;

    /**
     * 最后执行时间
     */
    private LocalDateTime lastExecuteTime;

    /**
     * 下次计划执行时间
     */
    private LocalDateTime nextExecuteTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
```

### 任务关联字段设计

任务关联字段用于把业务数据和 SnailJob 配置关联起来，但不直接依赖 SnailJob 内部主键。建议以业务编号、执行器名称、重试场景、幂等键作为核心关联字段。SnailJob 重试能力支持业务编号、幂等 ID 等配置；任务调度侧也支持任务状态、执行日志、失败告警和生命周期管理，因此业务侧只需要保存可用于检索和幂等控制的字段。([Snail Job](https://snailjob.opensnail.com/docs/introduce/preface.html?utm_source=chatgpt.com))

推荐字段设计如下：

| 字段                 | 是否必填 | 说明                                         |
| -------------------- | -------- | -------------------------------------------- |
| `biz_type`           | 是       | 业务类型，例如 `ORDER_CLOSE`、`PAY_CALLBACK` |
| `biz_no`             | 是       | 业务编号，例如订单号、支付单号               |
| `task_status`        | 是       | 业务任务状态                                 |
| `namespace_id`       | 否       | 对应 SnailJob 命名空间                       |
| `group_name`         | 否       | 对应 SnailJob 组名                           |
| `executor_name`      | 否       | 对应 `@JobExecutor` 名称                     |
| `retry_scene`        | 否       | 对应 `@Retryable(scene = "...")`             |
| `idempotent_key`     | 是       | 防止重复执行                                 |
| `task_param`         | 否       | 保存业务任务参数                             |
| `last_error_message` | 否       | 保存最近一次失败原因                         |
| `last_execute_time`  | 否       | 最近执行时间                                 |

不同任务类型建议使用不同关联策略：

| 任务类型 | 关联方式                              | 示例                                                         |
| -------- | ------------------------------------- | ------------------------------------------------------------ |
| 普通任务 | `biz_type + biz_no + executor_name`   | `ORDER_CLEANUP + 批次号 + orderCleanupJobExecutor`           |
| 定时任务 | `biz_type + 统计日期 + executor_name` | `ORDER_STATISTICS + 2026-05-06 + dailyOrderStatisticsJobExecutor` |
| 重试任务 | `biz_type + biz_no + retry_scene`     | `PAY_CALLBACK + PAY202605070001 + payCallbackRetry`          |
| 回调任务 | `biz_type + biz_no + idempotent_key`  | `WORKFLOW_CALLBACK + ORDER202605070001 + 回调请求唯一键`     |

幂等键生成建议统一封装，避免不同业务模块拼接规则不一致。

文件位置：`src/main/java/io/github/atengk/snailjob/support/BizTaskIdempotentKeyBuilder.java`

```java
package io.github.atengk.snailjob.support;

import cn.hutool.core.util.StrUtil;

/**
 * 业务任务幂等键构建器
 *
 * @author Ateng
 * @since 2026-05-07
 */
public final class BizTaskIdempotentKeyBuilder {

    private BizTaskIdempotentKeyBuilder() {
    }

    /**
     * 构建幂等键
     *
     * @param bizType 业务类型
     * @param bizNo 业务编号
     * @param scene 场景或执行器名称
     * @return 幂等键
     */
    public static String build(String bizType, String bizNo, String scene) {
        if (StrUtil.hasBlank(bizType, bizNo, scene)) {
            throw new IllegalArgumentException("业务类型、业务编号和场景不能为空");
        }
        return StrUtil.format("{}:{}:{}", bizType, bizNo, scene);
    }

}
```

调用示例：

```java
String idempotentKey = BizTaskIdempotentKeyBuilder.build(
        "PAY_CALLBACK",
        "PAY202605070001",
        "payCallbackRetry"
);
```

任务关联字段使用建议如下：

| 建议                                       | 说明                                                   |
| ------------------------------------------ | ------------------------------------------------------ |
| 不直接保存 SnailJob 内部表主键作为唯一依据 | 避免服务端升级或数据迁移导致业务依赖失效               |
| `biz_type + biz_no` 建唯一索引             | 防止同一业务重复创建任务                               |
| `idempotent_key` 建唯一索引                | 防止重复执行造成副作用                                 |
| `task_param` 保存任务快照                  | 便于失败后人工排查                                     |
| 错误信息只保存摘要                         | 完整堆栈放应用日志或执行明细表                         |
| 状态变更必须带条件                         | 例如只允许 `RUNNING -> SUCCESS` 或 `RUNNING -> FAILED` |

### 管理接口设计

管理接口用于业务系统侧查询任务状态、人工触发任务、取消业务任务、查看执行记录和重新处理失败任务。SnailJob 自身也提供 OpenAPI 能力，文档目录包含新增任务、修改任务、查询任务、调度任务、任务状态更新、任务删除等 API；如果需要由业务系统动态创建或调度 SnailJob 任务，应优先通过 SnailJob OpenAPI 对接，而不是直接写 SnailJob 内部表。([Snail Job](https://snailjob.opensnail.com/docs/guide/job/job_task.html?utm_source=chatgpt.com))

业务系统侧建议提供以下管理接口：

| 接口         | 方法 | 路径                                    | 说明                           |
| ------------ | ---- | --------------------------------------- | ------------------------------ |
| 查询任务列表 | GET  | `/api/snail-job/tasks`                  | 按状态、业务类型、业务编号查询 |
| 查询任务详情 | GET  | `/api/snail-job/tasks/{id}`             | 查看单个业务任务记录           |
| 人工标记失败 | POST | `/api/snail-job/tasks/{id}/mark-failed` | 人工终止不可恢复任务           |
| 人工重新触发 | POST | `/api/snail-job/tasks/{id}/retry`       | 将失败任务重新置为待执行       |
| 取消业务任务 | POST | `/api/snail-job/tasks/{id}/cancel`      | 取消未执行或不再需要执行的任务 |
| 查询执行日志 | GET  | `/api/snail-job/tasks/{id}/logs`        | 查询业务侧执行明细             |

请求 DTO 示例：

文件位置：`src/main/java/io/github/atengk/snailjob/model/dto/BizTaskQueryDTO.java`

```java
package io.github.atengk.snailjob.model.dto;

import lombok.Data;

/**
 * 业务任务查询参数
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class BizTaskQueryDTO {

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务编号
     */
    private String bizNo;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 页码
     */
    private Long pageNum = 1L;

    /**
     * 每页数量
     */
    private Long pageSize = 20L;

}
```

文件位置：`src/main/java/io/github/atengk/snailjob/model/dto/BizTaskManualRequestDTO.java`

```java
package io.github.atengk.snailjob.model.dto;

import lombok.Data;

/**
 * 业务任务人工处理请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class BizTaskManualRequestDTO {

    /**
     * 处理原因
     */
    private String reason;

}
```

Mapper 示例：

文件位置：`src/main/java/io/github/atengk/snailjob/mapper/BizTaskRecordMapper.java`

```java
package io.github.atengk.snailjob.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.snailjob.entity.BizTaskRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务任务记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Mapper
public interface BizTaskRecordMapper extends BaseMapper<BizTaskRecord> {
}
```

Service 接口示例：

文件位置：`src/main/java/io/github/atengk/snailjob/service/BizTaskManageService.java`

```java
package io.github.atengk.snailjob.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.atengk.snailjob.entity.BizTaskRecord;
import io.github.atengk.snailjob.model.dto.BizTaskManualRequestDTO;
import io.github.atengk.snailjob.model.dto.BizTaskQueryDTO;

/**
 * 业务任务管理服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface BizTaskManageService {

    /**
     * 分页查询业务任务
     *
     * @param query 查询参数
     * @return 任务分页结果
     */
    IPage<BizTaskRecord> pageTasks(BizTaskQueryDTO query);

    /**
     * 查询任务详情
     *
     * @param id 任务 ID
     * @return 任务详情
     */
    BizTaskRecord getTask(Long id);

    /**
     * 重新触发失败任务
     *
     * @param id 任务 ID
     * @param request 人工处理请求
     */
    void retryTask(Long id, BizTaskManualRequestDTO request);

    /**
     * 取消任务
     *
     * @param id 任务 ID
     * @param request 人工处理请求
     */
    void cancelTask(Long id, BizTaskManualRequestDTO request);

    /**
     * 标记任务失败
     *
     * @param id 任务 ID
     * @param request 人工处理请求
     */
    void markFailed(Long id, BizTaskManualRequestDTO request);

}
```

Service 实现示例：

文件位置：`src/main/java/io/github/atengk/snailjob/service/impl/BizTaskManageServiceImpl.java`

```java
package io.github.atengk.snailjob.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.snailjob.entity.BizTaskRecord;
import io.github.atengk.snailjob.mapper.BizTaskRecordMapper;
import io.github.atengk.snailjob.model.dto.BizTaskManualRequestDTO;
import io.github.atengk.snailjob.model.dto.BizTaskQueryDTO;
import io.github.atengk.snailjob.service.BizTaskManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 业务任务管理服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
public class BizTaskManageServiceImpl extends ServiceImpl<BizTaskRecordMapper, BizTaskRecord>
        implements BizTaskManageService {

    /**
     * 分页查询业务任务
     *
     * @param query 查询参数
     * @return 任务分页结果
     */
    @Override
    public IPage<BizTaskRecord> pageTasks(BizTaskQueryDTO query) {
        LambdaQueryWrapper<BizTaskRecord> wrapper = new LambdaQueryWrapper<BizTaskRecord>()
                .eq(StrUtil.isNotBlank(query.getBizType()), BizTaskRecord::getBizType, query.getBizType())
                .eq(StrUtil.isNotBlank(query.getBizNo()), BizTaskRecord::getBizNo, query.getBizNo())
                .eq(StrUtil.isNotBlank(query.getTaskStatus()), BizTaskRecord::getTaskStatus, query.getTaskStatus())
                .orderByDesc(BizTaskRecord::getCreateTime);

        return page(Page.of(query.getPageNum(), query.getPageSize()), wrapper);
    }

    /**
     * 查询任务详情
     *
     * @param id 任务 ID
     * @return 任务详情
     */
    @Override
    public BizTaskRecord getTask(Long id) {
        BizTaskRecord record = getById(id);
        if (record == null) {
            throw new IllegalArgumentException("业务任务不存在");
        }
        return record;
    }

    /**
     * 重新触发失败任务
     *
     * @param id 任务 ID
     * @param request 人工处理请求
     */
    @Override
    public void retryTask(Long id, BizTaskManualRequestDTO request) {
        BizTaskRecord record = getTask(id);
        if (!StrUtil.equals(record.getTaskStatus(), "FAILED")) {
            throw new IllegalStateException("只有失败任务允许重新触发");
        }

        BizTaskRecord update = new BizTaskRecord();
        update.setId(id);
        update.setTaskStatus("PENDING");
        update.setLastErrorMessage(null);
        update.setRemark(buildRemark(request, "人工重新触发"));
        update.setUpdateTime(LocalDateTime.now());

        updateById(update);
        log.info("人工重新触发业务任务，id={}，bizType={}，bizNo={}", id, record.getBizType(), record.getBizNo());
    }

    /**
     * 取消任务
     *
     * @param id 任务 ID
     * @param request 人工处理请求
     */
    @Override
    public void cancelTask(Long id, BizTaskManualRequestDTO request) {
        BizTaskRecord record = getTask(id);
        if (StrUtil.equalsAny(record.getTaskStatus(), "SUCCESS", "CANCELED")) {
            throw new IllegalStateException("成功或已取消任务不允许重复取消");
        }

        BizTaskRecord update = new BizTaskRecord();
        update.setId(id);
        update.setTaskStatus("CANCELED");
        update.setRemark(buildRemark(request, "人工取消"));
        update.setUpdateTime(LocalDateTime.now());

        updateById(update);
        log.info("人工取消业务任务，id={}，bizType={}，bizNo={}", id, record.getBizType(), record.getBizNo());
    }

    /**
     * 标记任务失败
     *
     * @param id 任务 ID
     * @param request 人工处理请求
     */
    @Override
    public void markFailed(Long id, BizTaskManualRequestDTO request) {
        BizTaskRecord record = getTask(id);

        BizTaskRecord update = new BizTaskRecord();
        update.setId(id);
        update.setTaskStatus("FAILED");
        update.setLastErrorMessage(buildRemark(request, "人工标记失败"));
        update.setRemark(buildRemark(request, "人工标记失败"));
        update.setUpdateTime(LocalDateTime.now());

        updateById(update);
        log.warn("人工标记业务任务失败，id={}，bizType={}，bizNo={}", id, record.getBizType(), record.getBizNo());
    }

    /**
     * 构建备注
     *
     * @param request 人工处理请求
     * @param defaultReason 默认原因
     * @return 备注
     */
    private String buildRemark(BizTaskManualRequestDTO request, String defaultReason) {
        if (request == null || StrUtil.isBlank(request.getReason())) {
            return defaultReason;
        }
        return request.getReason();
    }

}
```

Controller 示例：

文件位置：`src/main/java/io/github/atengk/snailjob/controller/BizTaskManageController.java`

```java
package io.github.atengk.snailjob.controller;

import cn.hutool.core.lang.Dict;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.atengk.snailjob.entity.BizTaskRecord;
import io.github.atengk.snailjob.model.dto.BizTaskManualRequestDTO;
import io.github.atengk.snailjob.model.dto.BizTaskQueryDTO;
import io.github.atengk.snailjob.service.BizTaskManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 业务任务管理接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/snail-job/tasks")
public class BizTaskManageController {

    private final BizTaskManageService bizTaskManageService;

    /**
     * 分页查询业务任务
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping
    public Dict pageTasks(BizTaskQueryDTO query) {
        IPage<BizTaskRecord> page = bizTaskManageService.pageTasks(query);
        return Dict.create()
                .set("code", 0)
                .set("message", "查询成功")
                .set("data", page);
    }

    /**
     * 查询任务详情
     *
     * @param id 任务 ID
     * @return 任务详情
     */
    @GetMapping("/{id}")
    public Dict getTask(@PathVariable Long id) {
        return Dict.create()
                .set("code", 0)
                .set("message", "查询成功")
                .set("data", bizTaskManageService.getTask(id));
    }

    /**
     * 重新触发失败任务
     *
     * @param id 任务 ID
     * @param request 人工处理请求
     * @return 处理结果
     */
    @PostMapping("/{id}/retry")
    public Dict retryTask(@PathVariable Long id, @RequestBody(required = false) BizTaskManualRequestDTO request) {
        bizTaskManageService.retryTask(id, request);
        return Dict.create()
                .set("code", 0)
                .set("message", "重新触发成功");
    }

    /**
     * 取消任务
     *
     * @param id 任务 ID
     * @param request 人工处理请求
     * @return 处理结果
     */
    @PostMapping("/{id}/cancel")
    public Dict cancelTask(@PathVariable Long id, @RequestBody(required = false) BizTaskManualRequestDTO request) {
        bizTaskManageService.cancelTask(id, request);
        return Dict.create()
                .set("code", 0)
                .set("message", "取消成功");
    }

    /**
     * 标记任务失败
     *
     * @param id 任务 ID
     * @param request 人工处理请求
     * @return 处理结果
     */
    @PostMapping("/{id}/mark-failed")
    public Dict markFailed(@PathVariable Long id, @RequestBody(required = false) BizTaskManualRequestDTO request) {
        bizTaskManageService.markFailed(id, request);
        return Dict.create()
                .set("code", 0)
                .set("message", "标记失败成功");
    }

}
```

接口调用示例：

```bash
# 查询失败任务
curl 'http://localhost:8081/api/snail-job/tasks?taskStatus=FAILED&pageNum=1&pageSize=20'

# 查询任务详情
curl 'http://localhost:8081/api/snail-job/tasks/1'

# 人工重新触发失败任务
curl -X POST 'http://localhost:8081/api/snail-job/tasks/1/retry' \
  -H 'Content-Type: application/json' \
  -d '{"reason":"第三方接口已恢复，人工重新触发"}'

# 取消任务
curl -X POST 'http://localhost:8081/api/snail-job/tasks/1/cancel' \
  -H 'Content-Type: application/json' \
  -d '{"reason":"业务订单已关闭，无需继续执行"}'
```

管理接口设计注意事项如下：

| 注意事项                           | 说明                                       |
| ---------------------------------- | ------------------------------------------ |
| 管理接口必须做权限控制             | 只允许运维、管理员或具备任务权限的用户操作 |
| 人工操作必须记录原因               | 便于审计和问题追溯                         |
| 重新触发前校验业务状态             | 避免对已完成业务重复执行                   |
| 取消任务不等于停止 SnailJob 批次   | 业务侧取消后，执行器中也要判断状态并跳过   |
| 查询接口不要返回敏感参数           | `task_param` 中如有敏感字段需要脱敏        |
| 动态管理 SnailJob 任务使用 OpenAPI | 不直接操作 SnailJob 服务端内部表           |



## 测试验证

本章节用于验证 Spring Boot 客户端是否正确接入 SnailJob 服务端、任务是否可以正常调度、失败是否能触发重试、日志是否能在本地和管理端定位。SnailJob 快速开始文档中给出的基础流程包括：初始化数据库、启动服务端、访问 `http://localhost:8080/snail-job`、创建命名空间、创建执行器组、配置客户端并触发任务。 ([Snail Job](https://snailjob.opensnail.com/docs/quickstart/quick_start.html?utm_source=chatgpt.com))

### 本地启动验证

本地启动验证用于确认 SnailJob 服务端、Spring Boot 客户端、数据库和网络端口均处于可用状态。验证重点不是业务逻辑，而是基础连接链路是否打通。

本地验证前先确认服务端和客户端配置一致：

| 验证项         | 服务端 / 管理端  | Spring Boot 客户端      |
| -------------- | ---------------- | ----------------------- |
| 命名空间       | 管理端 namespace | `snail-job.namespace`   |
| 组名称         | 管理端 groupName | `snail-job.group`       |
| Token          | 管理端组 Token   | `snail-job.token`       |
| 服务端通信端口 | 默认 `17888`     | `snail-job.server.port` |
| 客户端通信端口 | 客户端上报       | `snail-job.port`        |
| Web 管理端口   | 默认 `8080`      | 仅访问管理端时使用      |

SnailJob Java 客户端配置项包括服务端地址、服务端 gRPC 端口、客户端 IP、客户端端口、namespace、token、rpc-type、OpenAPI 端口以及网络优先规则等；如果服务端集群部署，客户端 `snail-job.server.host` 应配置为服务端域名或负载均衡地址。([Snail Job](https://snailjob.opensnail.com/docs/guide/client/java/client.html?utm_source=chatgpt.com))

本地启动检查命令如下：

```bash
# 检查 SnailJob Server 容器是否运行
docker ps | grep snail-job

# 查看 SnailJob Server 日志
docker logs -f snail-job-server

# 检查服务端 Web 端口
curl -I http://127.0.0.1:8080/snail-job

# 检查服务端通信端口是否监听
netstat -tunlp | grep 17888

# 启动 Spring Boot 客户端
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

命令说明：

| 命令                  | 说明                                             |
| --------------------- | ------------------------------------------------ |
| `docker ps`           | 确认 SnailJob 服务端容器是否启动                 |
| `docker logs -f`      | 查看服务端启动异常、数据库连接异常、端口占用异常 |
| `curl -I`             | 验证管理端 Web 地址是否可访问                    |
| `netstat`             | 验证 `17888` 通信端口是否监听                    |
| `mvn spring-boot:run` | 使用 dev 配置启动 Spring Boot 客户端             |

客户端启动成功后，进入 SnailJob 管理端，在对应命名空间和组下查看在线机器。如果在线机器列表中能看到当前 Spring Boot 应用实例，说明 namespace、group、token、服务端地址和通信端口基本配置正确。

### 任务执行验证

任务执行验证用于确认管理端配置的任务可以调度到 Spring Boot 客户端，并且执行器名称、任务参数、返回结果和业务日志都符合预期。SnailJob 的 `JobExecutor` 文档要求执行器名称与任务配置保持一致，同一个客户端内执行器名称不能重复。([Snail Job](https://snailjob.opensnail.com/docs/guide/alarm/retry_notice.html?utm_source=chatgpt.com))

建议先创建一个最小化测试执行器，用于验证调度链路。

文件位置：`src/main/java/io/github/atengk/snailjob/executor/HealthCheckJobExecutor.java`

```java
package io.github.atengk.snailjob.executor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SnailJob 健康检查任务执行器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class HealthCheckJobExecutor {

    /**
     * 执行健康检查任务
     *
     * @param jobArgs 任务参数
     * @return 执行结果
     */
    @JobExecutor(name = "healthCheckJobExecutor")
    public ExecuteResult execute(JobArgs jobArgs) {
        String jobParams = jobArgs.getJobParams();

        log.info("SnailJob 健康检查任务开始执行，params={}", jobParams);

        String message = StrUtil.format("客户端执行正常，执行时间={}", DateUtil.now());

        log.info("SnailJob 健康检查任务执行完成，message={}", message);
        return ExecuteResult.success(message);
    }

}
```

管理端任务配置如下：

| 配置项       | 示例值                     |
| ------------ | -------------------------- |
| 任务名称     | SnailJob 客户端健康检查    |
| 组名称       | `order-service`            |
| 执行器类型   | Java                       |
| 执行器名称   | `healthCheckJobExecutor`   |
| 任务参数     | `{"source":"manual-test"}` |
| 触发类型     | 手动触发或 CRON            |
| 最大重试次数 | 0                          |
| 超时时间     | 30 秒                      |

验证步骤如下：

```bash
# 1. 启动 Spring Boot 客户端
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 2. 登录 SnailJob 管理端
# 浏览器访问：http://127.0.0.1:8080/snail-job

# 3. 切换到当前 namespace

# 4. 进入任务管理，新增或选择 healthCheckJobExecutor 任务

# 5. 手动执行任务

# 6. 查看客户端控制台日志
```

预期结果：

| 验证点         | 预期结果                               |
| -------------- | -------------------------------------- |
| 管理端任务批次 | 生成新的执行批次                       |
| 执行状态       | 成功                                   |
| 客户端日志     | 输出“健康检查任务开始执行”和“执行完成” |
| 执行结果       | 返回 `客户端执行正常`                  |
| 在线日志       | 可以查看任务日志                       |

如果任务一直未执行，应优先检查执行器名称是否完全一致、客户端是否在线、任务组是否启用、任务所在 namespace 是否正确。

### 失败重试验证

失败重试验证用于确认任务失败后是否能按配置进行重试。SnailJob Job 任务重试触发时机包括网络超时、客户端明确返回错误和客户端出现异常；最大重试次数和重试间隔在管理端任务配置中控制。([Snail Job](https://snailjob.opensnail.com/docs/guide/job/job_retry.html?utm_source=chatgpt.com))

先创建一个故意失败的任务执行器。

文件位置：`src/main/java/io/github/atengk/snailjob/executor/FailureRetryTestJobExecutor.java`

```java
package io.github.atengk.snailjob.executor;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 失败重试测试任务执行器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class FailureRetryTestJobExecutor {

    /**
     * 执行失败重试测试任务
     *
     * @param jobArgs 任务参数
     * @return 执行结果
     */
    @JobExecutor(name = "failureRetryTestJobExecutor")
    public ExecuteResult execute(JobArgs jobArgs) {
        String jobParams = jobArgs.getJobParams();

        log.info("开始执行失败重试测试任务，params={}", jobParams);

        if (StrUtil.contains(jobParams, "success")) {
            log.info("失败重试测试任务执行成功，params={}", jobParams);
            return ExecuteResult.success("执行成功");
        }

        log.warn("失败重试测试任务返回失败，等待 SnailJob 重试，params={}", jobParams);
        return ExecuteResult.failure("模拟业务失败");
    }

}
```

管理端任务配置如下：

| 配置项       | 示例值                        | 说明              |
| ------------ | ----------------------------- | ----------------- |
| 任务名称     | 失败重试测试任务              |                   |
| 执行器名称   | `failureRetryTestJobExecutor` | 与注解名称一致    |
| 任务参数     | `{"mode":"fail"}`             | 故意触发失败      |
| 最大重试次数 | 3                             | 验证是否重试 3 次 |
| 重试间隔     | 10 秒                         | 便于观察          |
| 超时时间     | 30 秒                         | 避免长时间阻塞    |

验证结果应包含以下现象：

| 验证点           | 预期结果                       |
| ---------------- | ------------------------------ |
| 第一次执行       | 返回失败                       |
| 后续重试         | 按配置间隔继续调度             |
| 客户端日志       | 多次输出同一任务的失败日志     |
| 管理端批次       | 可以看到失败和重试记录         |
| 达到最大重试次数 | 任务最终失败或进入对应失败状态 |

对于 `@Retryable` 方法的重试，需要通过业务接口触发异常。SnailJob 重试策略包含 `ONLY_LOCAL`、`ONLY_REMOTE` 和 `LOCAL_REMOTE`：本地重试仅在客户端内存中执行，远程重试会上报服务端持久化，`LOCAL_REMOTE` 则先本地重试，仍失败后再上报服务端。([Snail Job](https://snailjob.opensnail.com/docs/guide/retry/server_report_retryStrategy.html?utm_source=chatgpt.com))

### 日志排查验证

日志排查验证用于确认本地控制台、应用日志文件和 SnailJob 管理端日志都能用于定位问题。SnailJob 服务端支持日志保留配置，开源版本通过全局 `log-storage` 配置日志保留天数，默认保留 7 天，PRO 版本支持按任务维度配置。([Snail Job](https://snailjob.opensnail.com/docs/guide/common/log_clear.html?utm_source=chatgpt.com))

建议测试以下日志链路：

| 日志类型         | 查看位置             | 验证方式                                 |
| ---------------- | -------------------- | ---------------------------------------- |
| 客户端控制台日志 | Spring Boot 控制台   | 查看 `log.info`、`log.warn`、`log.error` |
| 客户端文件日志   | 应用日志目录         | 查看任务执行日志是否落盘                 |
| 管理端执行日志   | 任务批次 / 日志按钮  | 查看任务执行过程                         |
| 服务端日志       | SnailJob Server 日志 | 查看调度、通信、数据库异常               |
| 业务日志平台     | ELK / Loki / 云日志  | 按 `bizNo`、`executorName` 检索          |

推荐在任务日志中固定输出以下字段：

```text
executorName=任务执行器名称
bizNo=业务编号
scene=重试场景
jobParams=任务参数摘要
status=执行状态
errorMessage=失败原因
costMillis=执行耗时
```

日志排查命令示例：

```bash
# 查看 Spring Boot 客户端最近 200 行日志
tail -n 200 logs/order-service.log

# 按执行器名称检索
grep 'dailyOrderStatisticsJobExecutor' logs/order-service.log

# 按业务编号检索
grep 'ORDER202605070001' logs/order-service.log

# 查看 SnailJob Server 容器日志
docker logs --tail 200 snail-job-server

# 持续跟踪 SnailJob Server 日志
docker logs -f snail-job-server
```

验证通过标准如下：

| 验证项     | 通过标准                         |
| ---------- | -------------------------------- |
| 成功任务   | 能看到开始、成功、耗时日志       |
| 失败任务   | 能看到失败原因和异常堆栈         |
| 重试任务   | 能看到每次重试记录               |
| 管理端日志 | 能看到任务批次、任务项和执行日志 |
| 日志检索   | 能按业务编号快速定位一次任务     |

## 部署上线

本章节用于说明 SnailJob 服务端和 Spring Boot 客户端上线时的部署方式、配置隔离方案和上线检查清单。SnailJob 服务端部署文档说明，服务端需要初始化数据库表，官方已经适配 MySQL、达梦 V8、Oracle、PostgreSQL、SQL Server，并提供 Docker 和源码启动方式；Docker 部署示例暴露 Web 端口 `8080` 和通信端口 `17888`。([Snail Job](https://snailjob.opensnail.com/docs/guide/server/service_deployment.html?utm_source=chatgpt.com))

### 服务端部署

服务端部署建议优先采用 Docker 或 Kubernetes。单机部署适合开发和测试环境；生产环境建议至少部署两个 SnailJob Server 节点，并通过同一个数据库共享调度数据。

生产环境 Docker Compose 示例：

文件位置：`deploy/snail-job/docker-compose.yml`

```yaml
version: "3.8"

services:
  snail-job-server:
    image: opensnail/snail-job:${SNAIL_JOB_VERSION}
    container_name: snail-job-server
    restart: always
    environment:
      # 服务端数据库连接配置
      SPRING_DATASOURCE_USERNAME: ${SNAIL_JOB_DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${SNAIL_JOB_DB_PASSWORD}
      SPRING_DATASOURCE_URL: jdbc:mysql://${SNAIL_JOB_DB_HOST}:${SNAIL_JOB_DB_PORT}/snail_job?useSSL=false&characterEncoding=utf8&useUnicode=true&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_DRIVER_CLASS_NAME: com.mysql.cj.jdbc.Driver

      # 管理端 Web 端口
      SERVER_PORT: 8080

      # 日志保留天数，生产环境建议 15 到 30 天
      SNAIL_JOB_LOG_STORAGE: 30
    ports:
      # 管理端 Web 端口
      - "8080:8080"
      # 服务端通信端口
      - "17888:17888"
    volumes:
      # 挂载服务端日志目录
      - ./logs:/logs
    networks:
      - snail-job-net

networks:
  snail-job-net:
    driver: bridge
```

环境变量文件示例：

文件位置：`deploy/snail-job/.env`

```properties
# SnailJob 服务端镜像版本，生产环境必须固定版本号
SNAIL_JOB_VERSION=请替换为实际版本号

# SnailJob 服务端数据库配置
SNAIL_JOB_DB_HOST=192.168.1.10
SNAIL_JOB_DB_PORT=3306
SNAIL_JOB_DB_USERNAME=snail_job
SNAIL_JOB_DB_PASSWORD=SnailJob@123456
```

启动命令如下：

```bash
# 进入部署目录
cd deploy/snail-job

# 启动 SnailJob Server
docker compose up -d

# 查看容器状态
docker compose ps

# 查看启动日志
docker compose logs -f snail-job-server
```

命令说明：

| 命令                     | 说明                                     |
| ------------------------ | ---------------------------------------- |
| `docker compose up -d`   | 后台启动服务端                           |
| `docker compose ps`      | 查看容器运行状态                         |
| `docker compose logs -f` | 实时查看服务端日志                       |
| `.env`                   | 管理镜像版本、数据库地址和密码等环境变量 |

高可用部署时，SnailJob 文档要求配置业务侧 Web 代理和通信端口代理，并保证集群节点共用同一个数据库且节点之间网络互通。([Snail Job](https://snailjob.opensnail.com/docs/guide/server/service_deployment.html?utm_source=chatgpt.com))

Nginx 代理配置示例：

文件位置：`/etc/nginx/conf.d/snail-job.conf`

```nginx
# SnailJob 管理端 Web 代理
upstream snail_job_web {
    server 10.0.0.11:8080 weight=5 max_fails=2 fail_timeout=3s;
    server 10.0.0.12:8080 weight=5 max_fails=2 fail_timeout=3s;
}

server {
    listen 8080;
    server_name snail-job.example.com;

    location /snail-job/ {
        proxy_ignore_client_abort on;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_pass http://snail_job_web/snail-job/;
    }
}

# SnailJob 服务端通信代理
upstream snail_job_rpc {
    server 10.0.0.11:17888 weight=5 max_fails=2 fail_timeout=3s;
    server 10.0.0.12:17888 weight=5 max_fails=2 fail_timeout=3s;
}

server {
    listen 17888;
    server_name snail-job-rpc.example.com;

    location / {
        proxy_ignore_client_abort on;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_pass http://snail_job_rpc/;
    }
}
```

### 客户端部署

客户端部署指 Spring Boot 业务服务上线时携带 SnailJob 客户端配置。客户端配置中的 `snail-job.server.host`、`snail-job.server.port`、`snail-job.namespace`、`snail-job.group`、`snail-job.token`、`snail-job.host` 和 `snail-job.port` 需要与生产环境管理端配置一致。([Snail Job](https://snailjob.opensnail.com/docs/guide/client/java/client.html?utm_source=chatgpt.com))

生产配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  application:
    name: order-service

snail-job:
  server:
    # 生产环境配置为 SnailJob 服务端通信代理地址
    host: snail-job-rpc.example.com
    port: 17888

  # 生产命名空间 ID
  namespace: ${SNAIL_JOB_NAMESPACE}

  # 当前业务服务分组
  group: order-service

  # 生产 Token 从环境变量或配置中心注入
  token: ${SNAIL_JOB_TOKEN}

  # Kubernetes 场景下建议注入 Pod IP
  host: ${POD_IP}

  # 客户端通信端口；多副本部署时每个 Pod 内可相同，宿主机部署时不能冲突
  port: ${SNAIL_JOB_CLIENT_PORT:17889}

  # 通信方式，官方 1.6.0 后默认使用 grpc
  rpc-type: grpc

logging:
  config: classpath:logback-boot.xml
```

Kubernetes 部署片段示例：

文件位置：`deploy/k8s/order-service-deployment.yml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: business-prod
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: registry.example.com/order-service:${APP_VERSION}
          imagePullPolicy: IfNotPresent
          ports:
            # Spring Boot Web 端口
            - containerPort: 8081
            # SnailJob 客户端通信端口
            - containerPort: 17889
          env:
            # Spring Boot 环境
            - name: SPRING_PROFILES_ACTIVE
              value: prod

            # 将 Pod IP 注入 SnailJob 客户端 host
            - name: POD_IP
              valueFrom:
                fieldRef:
                  fieldPath: status.podIP

            # SnailJob 命名空间
            - name: SNAIL_JOB_NAMESPACE
              valueFrom:
                secretKeyRef:
                  name: order-service-secret
                  key: snail-job-namespace

            # SnailJob Token
            - name: SNAIL_JOB_TOKEN
              valueFrom:
                secretKeyRef:
                  name: order-service-secret
                  key: snail-job-token

            # SnailJob 客户端通信端口
            - name: SNAIL_JOB_CLIENT_PORT
              value: "17889"
```

客户端部署注意事项：

| 项目              | 说明                                         |
| ----------------- | -------------------------------------------- |
| `snail-job.host`  | 必须是 SnailJob 服务端可以访问到的客户端地址 |
| `snail-job.port`  | 客户端通信端口需要放通                       |
| `snail-job.group` | 必须提前在管理端创建                         |
| `snail-job.token` | 不允许硬编码在代码仓库                       |
| 多副本部署        | 每个副本都会作为在线机器注册到管理端         |
| 灰度发布          | 新旧版本执行器名称要兼容                     |

### 配置隔离

配置隔离用于避免不同环境之间互相调度任务。SnailJob 的 namespace、group 和 token 都应按环境区分；Spring Boot 的 profile、配置中心、Kubernetes Secret 或环境变量也应同步隔离。

推荐隔离方式如下：

| 配置项      | 开发环境           | 测试环境            | 生产环境            |
| ----------- | ------------------ | ------------------- | ------------------- |
| namespace   | `dev_namespace_id` | `test_namespace_id` | `prod_namespace_id` |
| group       | `order-service`    | `order-service`     | `order-service`     |
| token       | dev token          | test token          | prod token          |
| server.host | 本地或测试地址     | 测试服务端地址      | 生产通信代理地址    |
| log-storage | 3 天               | 7 天                | 15 到 30 天         |
| 告警        | 可关闭             | 测试群              | 生产告警群          |

建议使用环境变量覆盖敏感配置：

```bash
# 生产环境启动示例
java -jar order-service.jar \
  --spring.profiles.active=prod \
  --snail-job.server.host=snail-job-rpc.example.com \
  --snail-job.namespace="${SNAIL_JOB_NAMESPACE}" \
  --snail-job.token="${SNAIL_JOB_TOKEN}" \
  --snail-job.host="${POD_IP}" \
  --snail-job.port=17889
```

配置隔离检查清单：

| 检查项       | 要求                            |
| ------------ | ------------------------------- |
| namespace    | 不同环境不能共用                |
| token        | 不同环境不能共用                |
| 数据库       | SnailJob 服务端数据库按环境隔离 |
| 管理端账号   | 生产账号权限收敛                |
| 任务默认状态 | 新上线任务默认关闭或先手动验证  |
| 告警接收人   | 生产环境必须配置有效接收人      |
| 日志保留     | 按环境配置合理保留天数          |

### 上线检查

上线检查用于在发布前确认服务端、客户端、管理端配置和业务任务都处于可控状态。SnailJob 服务端配置中包含服务端通信端口、日志保留天数、重试拉取大小、bucket 数量、Dashboard 容错天数、负载均衡周期、rpc-type、重试拉取并行度等参数，生产环境应在上线前确认这些配置符合容量和运维要求。([Snail Job](https://snailjob.opensnail.com/docs/guide/server/server.html?utm_source=chatgpt.com))

上线检查清单如下：

| 类别   | 检查项     | 通过标准                           |
| ------ | ---------- | ---------------------------------- |
| 服务端 | 数据库连接 | 服务端启动无数据库异常             |
| 服务端 | Web 管理端 | 可以访问 `/snail-job`              |
| 服务端 | 通信端口   | `17888` 可被客户端访问             |
| 服务端 | 日志保留   | 配置符合环境要求                   |
| 客户端 | namespace  | 与生产命名空间一致                 |
| 客户端 | group      | 与管理端组名称一致                 |
| 客户端 | token      | 与管理端组 Token 一致              |
| 客户端 | host       | 服务端可回调访问                   |
| 客户端 | port       | 客户端端口放通                     |
| 任务   | 执行器名称 | 与 `@JobExecutor` 完全一致         |
| 任务   | 任务参数   | JSON 格式正确，关键字段完整        |
| 任务   | 阻塞策略   | 统计、清理类任务禁止盲目并行       |
| 任务   | 重试次数   | 避免无限重试或过低重试             |
| 业务   | 幂等控制   | 重复执行不会重复扣款、发券、发消息 |
| 日志   | 本地日志   | 可以按业务编号检索                 |
| 告警   | 接收人     | 告警渠道有效                       |

上线后建议先执行健康检查任务，再执行低风险业务任务，最后再开启高影响定时任务。对于订单关闭、支付回调、消息补偿等任务，应先在管理端手动触发单笔业务参数验证，再开放周期调度。

## 运维与问题排查

本章节用于说明生产环境中常见异常、任务积压、执行失败和日志定位的处理方式。排查原则是先确认基础链路，再确认任务配置，最后定位业务代码；不要直接修改 SnailJob 内部表绕过问题。

### 常见异常排查

常见异常主要集中在客户端无法注册、任务无法执行、任务执行失败、日志看不到和重试不生效。SnailJob 服务部署文档也将数据库连接错误、服务启动失败、集群网络互通列为部署验证中的典型问题。([Snail Job](https://snailjob.opensnail.com/docs/guide/server/service_deployment.html?utm_source=chatgpt.com))

常见问题排查表：

| 问题               | 可能原因                                 | 排查方式                             | 处理建议                 |
| ------------------ | ---------------------------------------- | ------------------------------------ | ------------------------ |
| 管理端无法访问     | 服务端未启动、端口未开放、Nginx 配置错误 | `docker logs`、`curl -I`、Nginx 日志 | 修复服务端或代理配置     |
| 客户端不在线       | namespace、group、token 不一致           | 查看客户端启动日志和管理端组配置     | 同步配置后重启客户端     |
| 任务不执行         | 任务未启用、组停用、执行器名称错误       | 查看任务状态、组状态、执行器名称     | 启用任务并修正执行器名称 |
| 执行器找不到       | `@JobExecutor` 名称与管理端不一致        | 搜索代码注解名称                     | 保持完全一致             |
| 任务超时           | 业务处理慢、超时时间过短                 | 查看执行耗时和数据库慢 SQL           | 优化业务或调整超时时间   |
| 重试不生效         | 重试次数为 0、异常被吞掉、返回成功       | 查看代码异常处理和任务配置           | 抛出异常或返回 failure   |
| 日志看不到         | 未配置 Appender、日志级别过高            | 查看 `logback-boot.xml`              | 修复日志配置             |
| 重复执行产生副作用 | 业务无幂等                               | 查业务表状态变更记录                 | 增加幂等键和状态条件更新 |

客户端连接排查命令：

```bash
# 查看客户端是否能解析服务端地址
ping snail-job-rpc.example.com

# 检查客户端到服务端通信端口
telnet snail-job-rpc.example.com 17888

# 查看客户端启动日志中的 SnailJob 配置
grep -i 'snail' logs/order-service.log

# 查看客户端端口是否监听
netstat -tunlp | grep 17889
```

服务端排查命令：

```bash
# 查看服务端容器状态
docker ps | grep snail-job-server

# 查看服务端最近日志
docker logs --tail 300 snail-job-server

# 查看数据库连接是否正常
docker logs snail-job-server | grep -i 'datasource\|mysql\|error\|exception'

# 检查服务端通信端口
netstat -tunlp | grep 17888
```

### 任务积压处理

任务积压通常出现在任务执行速度低于任务产生速度时，例如定时任务触发频率过高、失败重试量过大、业务接口长时间不可用、数据库慢查询或线程资源不足。处理任务积压时，应先止血，再扩容，再优化业务。

任务积压判断方式：

| 现象                        | 说明                         |
| --------------------------- | ---------------------------- |
| 待执行批次持续增长          | 任务生产速度大于消费速度     |
| 失败重试记录持续增长        | 下游系统或业务逻辑持续异常   |
| 客户端 CPU 或数据库负载升高 | 执行任务占用资源过高         |
| 执行耗时越来越长            | 数据量变大或 SQL 性能下降    |
| 相同任务重复堆积            | 阻塞策略或触发频率配置不合理 |

处理步骤如下：

| 步骤 | 操作             | 说明                   |
| ---- | ---------------- | ---------------------- |
| 1    | 暂停低优先级任务 | 先减少新增积压         |
| 2    | 降低触发频率     | 避免继续放大问题       |
| 3    | 检查失败原因     | 判断是否下游系统不可用 |
| 4    | 扩容客户端实例   | 提升执行能力           |
| 5    | 调整并行数或分片 | 大批量任务拆分执行     |
| 6    | 优化业务 SQL     | 降低单次任务耗时       |
| 7    | 分批恢复任务     | 避免恢复后再次打满资源 |

积压期间可先暂停非关键任务：

```bash
# 通过管理端操作：
# 1. 进入任务管理
# 2. 筛选非核心任务
# 3. 暂停高频任务或失败任务
# 4. 观察待执行数量是否下降
```

对于大批量数据处理任务，建议改造成分片、Map 或 MapReduce 模式。SnailJob 定时任务能力支持集群、广播、静态分片、Map、MapReduce 等任务类型，适合按数据范围、租户、门店、日期等维度拆分处理。([Snail Job](https://snailjob.opensnail.com/docs/guide/alarm/retry_notice.html?utm_source=chatgpt.com))

任务积压治理建议：

| 场景             | 建议                           |
| ---------------- | ------------------------------ |
| 高频定时任务积压 | 调大触发间隔，增加阻塞策略     |
| 单次执行慢       | 优化 SQL、拆分任务、增加分页   |
| 失败重试积压     | 暂停重试场景，修复下游后再恢复 |
| 客户端资源不足   | 增加客户端实例或独立任务节点   |
| 数据量过大       | 按日期、租户、业务编号分片     |
| 日志过多         | 调整日志级别和日志保留天数     |

### 执行失败处理

执行失败处理要区分可恢复失败和不可恢复失败。网络抖动、下游服务短暂不可用、数据库临时连接失败属于可恢复失败，可以通过重试处理；参数错误、业务状态不满足、数据不存在、签名错误等通常属于不可恢复失败，不应无限重试。

失败分类如下：

| 失败类型     | 示例                      | 处理方式                     |
| ------------ | ------------------------- | ---------------------------- |
| 参数错误     | JSON 格式错误、订单号为空 | 直接失败，修正参数后手动重跑 |
| 业务状态错误 | 订单已支付但执行关闭任务  | 幂等跳过或返回业务失败       |
| 下游服务异常 | 支付网关超时、MQ 不可用   | 进入重试                     |
| 数据库异常   | 死锁、连接超时            | 进入重试并排查数据库         |
| 代码异常     | 空指针、类型转换错误      | 修复代码后重新发布           |
| 超时异常     | 数据量过大、接口响应慢    | 优化性能或调整超时时间       |

推荐执行失败处理流程：

```text
发现任务失败
  ↓
查看管理端执行日志
  ↓
按 bizNo / executorName 查询客户端日志
  ↓
判断失败类型
  ├─ 参数错误：修正参数后手动重跑
  ├─ 可恢复异常：等待自动重试或人工触发
  ├─ 代码异常：修复发布后重新执行
  ├─ 下游异常：恢复下游后重新执行
  └─ 幂等冲突：确认业务状态后标记完成或取消
```

对于 `@JobExecutor` 任务，失败时应返回 `ExecuteResult.failure()`，让 SnailJob 感知失败并按任务配置处理。对于 `@Retryable` 方法，业务异常应抛出，不能在方法内部吞掉异常后返回成功，否则不会触发重试。

示例处理方式：

```java
try {
    orderTaskService.closeTimeoutOrder(param);
    return ExecuteResult.success("订单关闭成功");
} catch (IllegalArgumentException e) {
    log.warn("订单关闭任务参数错误，orderNo={}，message={}", param.getOrderNo(), e.getMessage());
    return ExecuteResult.failure("参数错误：" + e.getMessage());
} catch (Exception e) {
    log.error("订单关闭任务执行异常，orderNo={}", param.getOrderNo(), e);
    return ExecuteResult.failure("执行异常：" + e.getMessage());
}
```

人工处理建议如下：

| 处理方式 | 适用场景                           |
| -------- | ---------------------------------- |
| 手动重跑 | 下游系统恢复、参数已修正           |
| 标记失败 | 不可恢复错误、业务确认无需继续     |
| 标记成功 | 业务已经通过其他方式完成           |
| 取消任务 | 业务对象已终止                     |
| 调整配置 | 重试次数、超时时间、阻塞策略不合理 |
| 修复代码 | 空指针、SQL 错误、字段映射错误     |

### 日志定位方式

日志定位方式应以业务编号为主线，将 SnailJob 管理端日志、Spring Boot 应用日志、业务任务表和下游系统日志串联起来。生产环境中，每个任务执行器都应输出 `executorName`、`bizNo`、`scene`、`jobParams`、`status` 和 `costMillis` 等字段。

推荐日志格式：

```java
long startTime = System.currentTimeMillis();
String executorName = "dailyOrderStatisticsJobExecutor";
String bizNo = "ORDER_STATISTICS_2026-05-06";

try {
    log.info("任务开始执行，executorName={}，bizNo={}", executorName, bizNo);

    String result = orderStatisticsService.statistics(param);

    log.info("任务执行成功，executorName={}，bizNo={}，costMillis={}，result={}",
            executorName, bizNo, System.currentTimeMillis() - startTime, result);
    return ExecuteResult.success(result);
} catch (Exception e) {
    log.error("任务执行失败，executorName={}，bizNo={}，costMillis={}",
            executorName, bizNo, System.currentTimeMillis() - startTime, e);
    return ExecuteResult.failure("任务执行失败：" + e.getMessage());
}
```

日志定位路径如下：

| 步骤 | 操作                             | 目标                           |
| ---- | -------------------------------- | ------------------------------ |
| 1    | 在管理端找到失败批次             | 获取任务名称、执行器、执行时间 |
| 2    | 查看任务详情                     | 确认参数和执行节点             |
| 3    | 到应用日志按 `executorName` 检索 | 找到任务入口日志               |
| 4    | 按 `bizNo` 检索                  | 串联同一业务操作               |
| 5    | 查看异常堆栈                     | 定位代码、SQL、远程调用异常    |
| 6    | 查询业务任务表                   | 确认业务状态和幂等结果         |
| 7    | 查询下游日志                     | 判断是否下游系统失败           |

常用检索命令：

```bash
# 按执行器名称定位
grep 'executorName=dailyOrderStatisticsJobExecutor' logs/order-service.log

# 按业务编号定位
grep 'ORDER202605070001' logs/order-service.log

# 查看异常上下文
grep -n 'ORDER202605070001' logs/order-service.log

# 查看某个时间段日志
awk '/2026-05-07 01:00:00/,/2026-05-07 01:10:00/' logs/order-service.log

# 查看最近错误日志
grep -i 'error\|exception\|failed\|失败' logs/order-service.log | tail -n 100
```

日志定位注意事项：

| 注意事项             | 说明                                              |
| -------------------- | ------------------------------------------------- |
| 不打印敏感字段       | Token、密码、密钥、身份证号需要脱敏               |
| 业务编号必须稳定     | 订单号、支付单号、消息 ID 不应变化                |
| 异常日志保留堆栈     | `log.error("...", e)` 不要只打印 `e.getMessage()` |
| 成功日志也要记录     | 便于确认任务是否执行过                            |
| 管理端日志有保留期限 | 重要问题需及时导出或同步日志平台                  |
| 高频任务控制日志量   | 避免日志量过大影响磁盘和检索性能                  |

生产环境建议设置合理的日志保留策略。SnailJob 开源版本通过 `snail-job.log-storage` 控制全局日志保留天数，默认 7 天，配置范围为 1 到 365 天；生产环境通常建议保留 7 到 30 天，测试环境 3 到 7 天，开发环境 1 到 3 天。([Snail Job](https://snailjob.opensnail.com/docs/guide/common/log_clear.html?utm_source=chatgpt.com))
