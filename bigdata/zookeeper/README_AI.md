# Spring Boot 集成 Zookeeper 开发

## 文档概述

本文档用于说明如何在 Spring Boot 3 项目中集成 Zookeeper，并基于 Apache Curator 完成 Zookeeper 客户端连接、节点操作、监听机制、分布式锁、服务注册与发现等常见功能开发。

文档面向 Spring Boot 后端开发人员，重点关注项目搭建、依赖配置、客户端初始化、核心 API 使用和功能验证。通过本文档，可以快速完成一个可运行、可测试、可扩展的 Spring Boot 3 + Zookeeper 示例工程。

### 技术背景

Zookeeper 是一个分布式协调服务，常用于解决分布式系统中的状态同步、节点感知、服务注册、配置管理和分布式锁等问题。它通过树形节点结构存储少量协调数据，并提供临时节点、顺序节点、Watcher 监听等机制。

在实际 Java 项目中，通常不会直接使用 Zookeeper 原生客户端进行业务开发，而是使用 Apache Curator。Curator 对 Zookeeper 原生 API 做了高级封装，提供了更友好的客户端连接管理、重试机制、节点操作、监听器和分布式锁组件。

Spring Boot 3 负责提供 Web 服务、配置管理、Bean 生命周期管理和日志能力；Zookeeper 负责分布式协调；Curator 负责简化 Zookeeper 客户端开发。三者组合后，可以较低成本地在业务系统中实现分布式协调类功能。

### 适用场景

Spring Boot 集成 Zookeeper 适用于需要在多个服务实例之间共享状态、感知节点变化或控制并发执行的场景。

| 场景           | 说明                                               | 常用实现方式                 |
| -------------- | -------------------------------------------------- | ---------------------------- |
| 分布式锁       | 多实例部署时，保证同一资源同一时间只被一个实例处理 | Curator `InterProcessMutex`  |
| 服务注册与发现 | 服务启动时注册节点，服务下线时自动删除节点         | 临时节点、子节点监听         |
| 配置管理       | 保存少量动态配置，并在配置变更时通知业务服务       | 持久节点、节点数据监听       |
| 节点状态监听   | 监听指定节点或子节点的创建、更新、删除事件         | Curator Cache 监听           |
| 主从选举       | 多个实例中选举一个主节点执行调度任务               | 临时顺序节点、Leader 选举    |
| 集群协调       | 多个服务实例之间共享任务状态或执行状态             | 节点数据、版本控制、监听机制 |

Zookeeper 不适合存储大体量业务数据，也不适合作为高频读写缓存使用。它更适合存储体积小、变化频率较低、对一致性和监听能力有要求的协调类数据。

### 技术选型说明

本项目采用 Spring Boot 3、Apache Curator 和 Zookeeper 作为核心技术组合。

| 技术组件       | 作用                                                       |
| -------------- | ---------------------------------------------------------- |
| Spring Boot 3  | 项目基础框架，负责 Web 接口、配置管理、Bean 管理和日志体系 |
| Zookeeper      | 分布式协调服务，负责节点存储、监听通知和集群状态协调       |
| Apache Curator | Zookeeper 高级客户端，简化连接、重试、监听和分布式锁开发   |
| JDK 17+        | Spring Boot 3 基础运行环境                                 |
| Maven          | 项目构建和依赖管理                                         |
| Docker Compose | 本地快速部署 Zookeeper 服务                                |
| Hutool         | 常用工具类封装，减少重复工具代码                           |
| Lombok         | 简化日志对象、Getter、Setter 和构造方法等代码              |

选型说明如下：

1. Spring Boot 3 要求使用 Java 17 或更高版本，因此项目统一使用 JDK 17+。
2. Curator 对 Zookeeper 原生 API 做了封装，能够减少连接管理、异常重试和监听处理的重复代码。
3. 本地开发建议使用 Docker Compose 启动单节点 Zookeeper，便于快速验证功能。
4. 生产环境建议使用三节点或五节点 Zookeeper 集群，避免单节点故障影响业务。
5. Zookeeper 主要用于协调类场景，不建议替代数据库、缓存或专业配置中心。

## 环境准备

本章节用于准备 Spring Boot 3 集成 Zookeeper 所需的基础开发环境，包括 JDK、Maven、Docker、Zookeeper 服务和 Spring Boot 基础项目。后续依赖配置、客户端初始化、节点管理和监听机制均基于本章节环境展开。

### 基础环境要求

开发前需要确认本机已经安装 JDK、Maven 和 Docker。建议本地使用 Docker Compose 启动 Zookeeper，避免手动安装服务端组件。

| 环境项         | 建议版本            | 说明                            |
| -------------- | ------------------- | ------------------------------- |
| JDK            | 17 或以上           | Spring Boot 3 必须使用 Java 17+ |
| Maven          | 3.6.3 或以上        | 用于项目构建和依赖管理          |
| Docker         | 20.x 或以上         | 用于本地启动 Zookeeper          |
| Docker Compose | v2.x                | 用于编排 Zookeeper 容器         |
| Zookeeper      | 3.8.x / 3.9.x       | 本地开发和功能验证              |
| IDE            | IntelliJ IDEA 2023+ | 推荐用于 Spring Boot 3 开发     |

检查基础环境：

```bash
# 查看 Java 版本
java -version

# 查看 Maven 版本
mvn -version

# 查看 Docker 版本
docker version

# 查看 Docker Compose 版本
docker compose version
```

命令说明：

`java -version` 用于确认当前 JDK 是否为 17 或以上版本。`mvn -version` 用于确认 Maven 是否可用，并检查 Maven 当前使用的 JDK。`docker version` 需要同时显示 Client 和 Server 信息，如果只显示 Client，通常表示 Docker 服务未启动。`docker compose version` 用于确认当前环境是否支持新版 Docker Compose 命令。

### Zookeeper 服务部署

本地开发环境推荐使用 Docker Compose 启动单节点 Zookeeper。单节点部署适合开发、调试和接口验证，不建议直接用于生产环境。

文件位置：`docker-compose.yml`

```yaml
services:
  zookeeper:
    image: zookeeper:3.9
    container_name: zookeeper
    restart: unless-stopped
    ports:
      # Zookeeper 客户端连接端口
      - "2181:2181"
    environment:
      # Zookeeper 基础心跳时间，单位毫秒
      ZOO_TICK_TIME: 2000
      # 开启常用四字命令，便于本地检查服务状态
      ZOO_4LW_COMMANDS_WHITELIST: "ruok,stat,conf,cons"
    volumes:
      # 数据目录持久化
      - ./data/zookeeper/data:/data
      # 事务日志目录持久化
      - ./data/zookeeper/datalog:/datalog
```

启动 Zookeeper 服务：

```bash
# 在 docker-compose.yml 所在目录执行
docker compose up -d

# 查看容器运行状态
docker ps

# 查看 Zookeeper 容器日志
docker logs -f zookeeper
```

验证 Zookeeper 服务：

```bash
# 检查 Zookeeper 是否正常响应
echo ruok | nc 127.0.0.1 2181
```

如果返回如下内容，说明 Zookeeper 服务可用：

```text
imok
```

也可以进入容器内部使用 Zookeeper CLI 验证节点操作：

```bash
# 进入 Zookeeper 容器
docker exec -it zookeeper bash

# 连接 Zookeeper 服务
zkCli.sh -server 127.0.0.1:2181

# 查看根节点
ls /

# 创建测试节点
create /demo "hello-zookeeper"

# 查询测试节点
get /demo

# 删除测试节点
delete /demo
```

命令说明：

`docker compose up -d` 用于后台启动 Zookeeper 容器。`docker logs -f zookeeper` 用于观察服务启动日志。`zkCli.sh` 是 Zookeeper 自带的命令行客户端，可以直接验证节点创建、查询和删除功能。

### Spring Boot 3 项目准备

Spring Boot 3 项目可以通过 Spring Initializr、IDEA 或 Maven 手动创建。本文示例使用 Maven 项目结构，基础包名统一使用 `io.github.atengk.zookeeper`。

推荐项目信息如下：

| 配置项       | 示例值                       |
| ------------ | ---------------------------- |
| Group        | `io.github.atengk`           |
| Artifact     | `spring-boot-zookeeper`      |
| Name         | `spring-boot-zookeeper`      |
| Package Name | `io.github.atengk.zookeeper` |
| Java Version | `17`                         |
| Packaging    | `jar`                        |
| Build Tool   | `Maven`                      |

推荐项目结构：

```text
spring-boot-zookeeper
├── pom.xml
├── docker-compose.yml
└── src
    ├── main
    │   ├── java
    │   │   └── io
    │   │       └── github
    │   │           └── atengk
    │   │               └── zookeeper
    │   │                   ├── ZookeeperApplication.java
    │   │                   ├── config
    │   │                   ├── controller
    │   │                   ├── service
    │   │                   └── listener
    │   └── resources
    │       └── application.yml
    └── test
        └── java
```

基础 Maven 配置如下，Curator 依赖可以在后续“依赖与配置”章节中继续补充。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <!-- Spring Boot 3 父工程，统一管理 Spring 生态依赖版本 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-boot-zookeeper</artifactId>
    <version>1.0.0</version>
    <name>spring-boot-zookeeper</name>
    <description>Spring Boot 3 集成 Zookeeper 示例项目</description>

    <properties>
        <!-- Spring Boot 3 要求 Java 17 或以上版本 -->
        <java.version>17</java.version>
        <!-- Hutool 工具包版本 -->
        <hutool.version>5.8.38</hutool.version>
    </properties>

    <dependencies>
        <!-- Spring MVC Web 能力，用于提供接口测试入口 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验能力，用于 Controller 请求参数校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Actuator 健康检查能力，便于后续扩展 Zookeeper 状态检查 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Hutool 常用工具类，减少字符串、集合、JSON 等重复工具代码 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化实体类、日志对象和构造方法代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <!-- Spring Boot Maven 插件，用于打包可执行 Jar -->
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

基础配置文件如下：

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 应用服务端口
  port: 8080

spring:
  application:
    # 应用名称，后续可作为服务注册标识
    name: spring-boot-zookeeper

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查接口
        include: health,info

logging:
  level:
    # 项目业务日志级别
    io.github.atengk.zookeeper: info
    # Curator 客户端日志级别，调试时可临时调整为 debug
    org.apache.curator: info
```

启动类如下：

文件位置：`src/main/java/io/github/atengk/zookeeper/ZookeeperApplication.java`

```java
package io.github.atengk.zookeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Zookeeper 示例项目启动类
 *
 * @author Ateng
 * @since 2026-05-07
 */
@SpringBootApplication
public class ZookeeperApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZookeeperApplication.class, args);
    }

}
```

项目基础结构创建完成后，执行以下命令验证工程是否可正常启动：

```bash
# 编译项目
mvn clean compile

# 启动项目
mvn spring-boot:run

# 验证健康检查接口
curl http://127.0.0.1:8080/actuator/health
```

如果接口返回如下内容，说明 Spring Boot 3 基础项目启动成功：

```json
{
  "status": "UP"
}
```

至此，Spring Boot 3 项目和本地 Zookeeper 服务已经准备完成。后续可以继续补充 Curator 依赖、Zookeeper 连接配置、客户端初始化、节点 CRUD、监听器和分布式锁相关代码。



## 依赖与配置

本章节用于完成 Spring Boot 3 项目接入 Zookeeper 所需的 Maven 依赖、连接参数和 Curator 客户端配置。后续节点创建、查询、更新、删除等功能都基于本章节提供的 `CuratorFramework` 客户端完成。

### Maven 依赖配置

Spring Boot 项目推荐使用 Apache Curator 作为 Zookeeper 客户端。Curator 对 Zookeeper 原生 API 进行了封装，提供了更简洁的节点操作、重试策略、监听器和分布式锁能力。

在前面基础 `pom.xml` 的基础上，补充 Curator 相关依赖。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 3 要求 Java 17 或以上版本 -->
    <java.version>17</java.version>
    <!-- Curator 版本，提供 Zookeeper 高级客户端能力 -->
    <curator.version>5.9.0</curator.version>
    <!-- Hutool 工具包版本 -->
    <hutool.version>5.8.38</hutool.version>
</properties>

<dependencies>
    <!-- Spring MVC Web 能力，用于提供接口测试入口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验能力，用于 Controller 请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Actuator 健康检查能力，便于扩展 Zookeeper 连接状态检查 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Curator Framework，封装 Zookeeper 连接、重试、节点操作等基础能力 -->
    <dependency>
        <groupId>org.apache.curator</groupId>
        <artifactId>curator-framework</artifactId>
        <version>${curator.version}</version>
    </dependency>

    <!-- Curator Recipes，提供分布式锁、Leader 选举、缓存监听等高级能力 -->
    <dependency>
        <groupId>org.apache.curator</groupId>
        <artifactId>curator-recipes</artifactId>
        <version>${curator.version}</version>
    </dependency>

    <!-- Hutool 常用工具类，减少字符串、集合、JSON 等重复工具代码 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok 简化日志对象、构造方法、Getter、Setter 等代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

依赖说明：

| 依赖                             | 说明                                                         |
| -------------------------------- | ------------------------------------------------------------ |
| `curator-framework`              | Curator 基础客户端，提供 Zookeeper 连接、节点 CRUD、重试机制等能力 |
| `curator-recipes`                | Curator 高级组件，提供分布式锁、Leader 选举、监听缓存等能力  |
| `hutool-all`                     | 提供字符串、集合、JSON、字符集等工具类                       |
| `spring-boot-starter-validation` | 用于后续接口参数校验                                         |
| `spring-boot-starter-actuator`   | 用于服务健康检查和运行状态扩展                               |

添加依赖后执行编译，确认依赖可以正常下载：

```bash
mvn clean compile
```

如果 Maven 编译失败，优先检查 Curator 版本、Maven 仓库网络、JDK 版本和 Spring Boot 版本是否匹配。

### Zookeeper 连接配置

Zookeeper 连接配置建议统一放在 `application.yml` 中，避免在代码中硬编码连接地址、超时时间、重试次数等参数。这样可以在本地、测试、生产环境中通过配置文件或环境变量灵活调整。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 应用服务端口
  port: 8080

spring:
  application:
    # 应用名称，后续可作为服务注册标识
    name: spring-boot-zookeeper

zookeeper:
  # Zookeeper 连接地址，多个节点使用英文逗号分隔
  connect-string: 127.0.0.1:2181
  # 会话超时时间，单位毫秒
  session-timeout-ms: 60000
  # 连接超时时间，单位毫秒
  connection-timeout-ms: 15000
  # 命名空间，所有业务节点都会创建在该命名空间下
  namespace: spring-boot-demo
  # 最大重试次数
  max-retries: 3
  # 重试间隔时间，单位毫秒
  base-sleep-time-ms: 1000

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查接口
        include: health,info

logging:
  level:
    # 项目业务日志级别
    io.github.atengk.zookeeper: info
    # Curator 客户端日志级别，排查连接问题时可调整为 debug
    org.apache.curator: info
    org.apache.zookeeper: warn
```

配置项说明：

| 配置项                            | 说明                                   | 示例               |
| --------------------------------- | -------------------------------------- | ------------------ |
| `zookeeper.connect-string`        | Zookeeper 服务地址，集群使用逗号分隔   | `127.0.0.1:2181`   |
| `zookeeper.session-timeout-ms`    | Session 超时时间，超时后临时节点会失效 | `60000`            |
| `zookeeper.connection-timeout-ms` | 客户端连接超时时间                     | `15000`            |
| `zookeeper.namespace`             | Curator 命名空间，用于隔离不同应用节点 | `spring-boot-demo` |
| `zookeeper.max-retries`           | 操作失败后的最大重试次数               | `3`                |
| `zookeeper.base-sleep-time-ms`    | 重试基础间隔时间                       | `1000`             |

生产环境中，`connect-string` 应配置为 Zookeeper 集群地址：

```yaml
zookeeper:
  # 生产环境建议使用集群地址
  connect-string: zk-01:2181,zk-02:2181,zk-03:2181
```

使用命名空间后，业务代码中访问 `/config/app-name`，实际会映射到 Zookeeper 中的 `/spring-boot-demo/config/app-name`。这样可以避免不同应用之间节点路径冲突。

### Curator 客户端配置

Curator 客户端建议交给 Spring 容器管理。项目启动时创建 `CuratorFramework` Bean，项目关闭时由 Spring 自动释放连接资源。

推荐新增以下配置类和属性类：

```text
src/main/java/io/github/atengk/zookeeper
├── config
│   ├── ZookeeperProperties.java
│   └── CuratorConfig.java
```

`ZookeeperProperties` 用于承接 `application.yml` 中的 Zookeeper 配置。

文件位置：`src/main/java/io/github/atengk/zookeeper/config/ZookeeperProperties.java`

```java
package io.github.atengk.zookeeper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zookeeper 配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@ConfigurationProperties(prefix = "zookeeper")
public class ZookeeperProperties {

    /**
     * Zookeeper 连接地址
     */
    private String connectString;

    /**
     * 会话超时时间，单位毫秒
     */
    private Integer sessionTimeoutMs = 60000;

    /**
     * 连接超时时间，单位毫秒
     */
    private Integer connectionTimeoutMs = 15000;

    /**
     * Curator 命名空间
     */
    private String namespace = "spring-boot-demo";

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;

    /**
     * 重试基础间隔时间，单位毫秒
     */
    private Integer baseSleepTimeMs = 1000;

}
```

`CuratorConfig` 用于创建并启动 Curator 客户端。

文件位置：`src/main/java/io/github/atengk/zookeeper/config/CuratorConfig.java`

```java
package io.github.atengk.zookeeper.config;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Curator 客户端配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ZookeeperProperties.class)
public class CuratorConfig {

    private final ZookeeperProperties zookeeperProperties;

    /**
     * 创建 Curator 客户端
     *
     * @return Curator 客户端
     */
    @Bean(initMethod = "start", destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        if (StrUtil.isBlank(zookeeperProperties.getConnectString())) {
            throw new IllegalArgumentException("Zookeeper 连接地址不能为空");
        }

        RetryPolicy retryPolicy = new ExponentialBackoffRetry(
                zookeeperProperties.getBaseSleepTimeMs(),
                zookeeperProperties.getMaxRetries()
        );

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(zookeeperProperties.getConnectString())
                .sessionTimeoutMs(zookeeperProperties.getSessionTimeoutMs())
                .connectionTimeoutMs(zookeeperProperties.getConnectionTimeoutMs())
                .namespace(zookeeperProperties.getNamespace())
                .retryPolicy(retryPolicy)
                .build();

        log.info("初始化 Curator 客户端，connectString={}, namespace={}",
                zookeeperProperties.getConnectString(),
                zookeeperProperties.getNamespace());

        return client;
    }

    /**
     * 校验 Curator 客户端连接状态
     *
     * @param curatorFramework Curator 客户端
     * @return 是否连接成功
     */
    @Bean
    public Boolean curatorConnectionChecker(CuratorFramework curatorFramework) {
        try {
            boolean connected = curatorFramework.blockUntilConnected(10, TimeUnit.SECONDS);
            log.info("Curator 客户端连接状态：{}", connected ? "已连接" : "未连接");
            return connected;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Curator 客户端连接检查被中断");
            return false;
        }
    }

}
```

配置完成后启动项目，如果日志中出现类似以下内容，说明 Curator 客户端已经完成初始化：

```text
初始化 Curator 客户端，connectString=127.0.0.1:2181, namespace=spring-boot-demo
Curator 客户端连接状态：已连接
```

## 核心功能开发

本章节基于前面配置好的 `CuratorFramework` 客户端，封装 Zookeeper 节点的创建、查询、更新和删除功能。为了便于后续 Controller 调用，这里采用 Service 接口和 Service 实现类的方式组织代码。

推荐新增以下文件：

```text
src/main/java/io/github/atengk/zookeeper
├── service
│   ├── ZookeeperNodeService.java
│   └── impl
│       └── ZookeeperNodeServiceImpl.java
└── vo
    └── ZookeeperNodeVO.java
```

### 客户端初始化

客户端初始化已经在 `CuratorConfig` 中完成。业务代码中不需要手动创建 Zookeeper 客户端，只需要通过构造方法注入 `CuratorFramework` 即可。

在 Service 中注入客户端的方式如下：

```java
private final CuratorFramework curatorFramework;
```

完整业务代码会在后续 `ZookeeperNodeServiceImpl` 中给出。该方式可以保证：

1. 客户端连接由 Spring 容器统一管理。
2. 项目启动时自动初始化连接。
3. 项目关闭时自动释放连接。
4. 所有业务类复用同一个 Curator 客户端实例。
5. 重试策略、命名空间、超时时间等配置集中管理。

如果需要在业务方法中检查客户端状态，可以使用以下方式：

```java
boolean started = curatorFramework.getState().name().equals("STARTED");
```

实际开发中通常不建议每次节点操作前都检查连接状态。Curator 内部已经封装了连接状态维护和重试机制，业务代码重点处理具体操作异常即可。

### 节点创建

节点创建用于向 Zookeeper 写入业务节点。常见节点类型包括持久节点、临时节点、持久顺序节点和临时顺序节点。本文先封装最常用的持久节点和临时节点创建方法。

节点创建方法需要关注以下问题：

| 问题         | 处理方式                                        |
| ------------ | ----------------------------------------------- |
| 父节点不存在 | 使用 `creatingParentsIfNeeded()` 自动创建父节点 |
| 节点已存在   | 根据业务需要返回提示或覆盖更新                  |
| 节点数据为空 | 使用空字符串或 JSON 字符串写入                  |
| 中文乱码     | 统一使用 UTF-8 编码                             |
| 路径不规范   | 统一校验必须以 `/` 开头                         |

先定义节点返回对象，用于封装节点路径和节点数据。

文件位置：`src/main/java/io/github/atengk/zookeeper/vo/ZookeeperNodeVO.java`

```java
package io.github.atengk.zookeeper.vo;

import lombok.Builder;
import lombok.Data;

/**
 * Zookeeper 节点信息返回对象
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class ZookeeperNodeVO {

    /**
     * 节点路径
     */
    private String path;

    /**
     * 节点数据
     */
    private String data;

    /**
     * 节点版本号
     */
    private Integer version;

    /**
     * 是否存在
     */
    private Boolean exists;

}
```

定义节点操作 Service 接口。

文件位置：`src/main/java/io/github/atengk/zookeeper/service/ZookeeperNodeService.java`

```java
package io.github.atengk.zookeeper.service;

import io.github.atengk.zookeeper.vo.ZookeeperNodeVO;

import java.util.List;

/**
 * Zookeeper 节点操作服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface ZookeeperNodeService {

    /**
     * 创建持久节点
     *
     * @param path 节点路径
     * @param data 节点数据
     * @return 节点路径
     */
    String createPersistentNode(String path, String data);

    /**
     * 创建临时节点
     *
     * @param path 节点路径
     * @param data 节点数据
     * @return 节点路径
     */
    String createEphemeralNode(String path, String data);

    /**
     * 查询节点数据
     *
     * @param path 节点路径
     * @return 节点信息
     */
    ZookeeperNodeVO getNode(String path);

    /**
     * 查询子节点列表
     *
     * @param path 节点路径
     * @return 子节点列表
     */
    List<String> listChildren(String path);

    /**
     * 更新节点数据
     *
     * @param path 节点路径
     * @param data 节点数据
     * @return 节点信息
     */
    ZookeeperNodeVO updateNode(String path, String data);

    /**
     * 删除节点
     *
     * @param path 节点路径
     * @return 是否删除成功
     */
    Boolean deleteNode(String path);

}
```

以下是节点操作核心实现类，包含创建、查询、更新和删除的完整实现。

文件位置：`src/main/java/io/github/atengk/zookeeper/service/impl/ZookeeperNodeServiceImpl.java`

```java
package io.github.atengk.zookeeper.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.zookeeper.service.ZookeeperNodeService;
import io.github.atengk.zookeeper.vo.ZookeeperNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Zookeeper 节点操作服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZookeeperNodeServiceImpl implements ZookeeperNodeService {

    private final CuratorFramework curatorFramework;

    /**
     * 创建持久节点
     *
     * @param path 节点路径
     * @param data 节点数据
     * @return 节点路径
     */
    @Override
    public String createPersistentNode(String path, String data) {
        return createNode(path, data, CreateMode.PERSISTENT);
    }

    /**
     * 创建临时节点
     *
     * @param path 节点路径
     * @param data 节点数据
     * @return 节点路径
     */
    @Override
    public String createEphemeralNode(String path, String data) {
        return createNode(path, data, CreateMode.EPHEMERAL);
    }

    /**
     * 查询节点数据
     *
     * @param path 节点路径
     * @return 节点信息
     */
    @Override
    public ZookeeperNodeVO getNode(String path) {
        String nodePath = normalizePath(path);

        try {
            Stat stat = curatorFramework.checkExists().forPath(nodePath);
            if (stat == null) {
                log.info("Zookeeper 节点不存在，path={}", nodePath);
                return ZookeeperNodeVO.builder()
                        .path(nodePath)
                        .exists(false)
                        .build();
            }

            byte[] bytes = curatorFramework.getData().forPath(nodePath);
            String data = StrUtil.str(bytes, CharsetUtil.CHARSET_UTF_8);

            log.info("查询 Zookeeper 节点成功，path={}, version={}", nodePath, stat.getVersion());
            return ZookeeperNodeVO.builder()
                    .path(nodePath)
                    .data(data)
                    .version(stat.getVersion())
                    .exists(true)
                    .build();
        } catch (Exception e) {
            log.error("查询 Zookeeper 节点失败，path={}", nodePath, e);
            throw new IllegalStateException("查询 Zookeeper 节点失败：" + nodePath, e);
        }
    }

    /**
     * 查询子节点列表
     *
     * @param path 节点路径
     * @return 子节点列表
     */
    @Override
    public List<String> listChildren(String path) {
        String nodePath = normalizePath(path);

        try {
            Stat stat = curatorFramework.checkExists().forPath(nodePath);
            if (stat == null) {
                log.info("查询子节点时发现父节点不存在，path={}", nodePath);
                return CollUtil.newArrayList();
            }

            List<String> children = curatorFramework.getChildren().forPath(nodePath);
            log.info("查询 Zookeeper 子节点成功，path={}, size={}", nodePath, children.size());
            return children;
        } catch (Exception e) {
            log.error("查询 Zookeeper 子节点失败，path={}", nodePath, e);
            throw new IllegalStateException("查询 Zookeeper 子节点失败：" + nodePath, e);
        }
    }

    /**
     * 更新节点数据
     *
     * @param path 节点路径
     * @param data 节点数据
     * @return 节点信息
     */
    @Override
    public ZookeeperNodeVO updateNode(String path, String data) {
        String nodePath = normalizePath(path);

        try {
            Stat stat = curatorFramework.checkExists().forPath(nodePath);
            if (stat == null) {
                throw new KeeperException.NoNodeException(nodePath);
            }

            byte[] bytes = StrUtil.bytes(StrUtil.nullToEmpty(data), CharsetUtil.CHARSET_UTF_8);
            Stat newStat = curatorFramework.setData().forPath(nodePath, bytes);

            log.info("更新 Zookeeper 节点成功，path={}, version={}", nodePath, newStat.getVersion());
            return ZookeeperNodeVO.builder()
                    .path(nodePath)
                    .data(data)
                    .version(newStat.getVersion())
                    .exists(true)
                    .build();
        } catch (KeeperException.NoNodeException e) {
            log.warn("更新 Zookeeper 节点失败，节点不存在，path={}", nodePath);
            throw new IllegalArgumentException("Zookeeper 节点不存在：" + nodePath, e);
        } catch (Exception e) {
            log.error("更新 Zookeeper 节点失败，path={}", nodePath, e);
            throw new IllegalStateException("更新 Zookeeper 节点失败：" + nodePath, e);
        }
    }

    /**
     * 删除节点
     *
     * @param path 节点路径
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteNode(String path) {
        String nodePath = normalizePath(path);

        try {
            Stat stat = curatorFramework.checkExists().forPath(nodePath);
            if (stat == null) {
                log.info("删除 Zookeeper 节点时节点不存在，path={}", nodePath);
                return false;
            }

            curatorFramework.delete()
                    .deletingChildrenIfNeeded()
                    .forPath(nodePath);

            log.info("删除 Zookeeper 节点成功，path={}", nodePath);
            return true;
        } catch (Exception e) {
            log.error("删除 Zookeeper 节点失败，path={}", nodePath, e);
            throw new IllegalStateException("删除 Zookeeper 节点失败：" + nodePath, e);
        }
    }

    /**
     * 创建节点
     *
     * @param path 节点路径
     * @param data 节点数据
     * @param createMode 节点类型
     * @return 节点路径
     */
    private String createNode(String path, String data, CreateMode createMode) {
        String nodePath = normalizePath(path);

        try {
            Stat stat = curatorFramework.checkExists().forPath(nodePath);
            if (stat != null) {
                log.warn("创建 Zookeeper 节点失败，节点已存在，path={}", nodePath);
                throw new IllegalArgumentException("Zookeeper 节点已存在：" + nodePath);
            }

            byte[] bytes = StrUtil.bytes(StrUtil.nullToEmpty(data), CharsetUtil.CHARSET_UTF_8);
            String createdPath = curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .withMode(createMode)
                    .forPath(nodePath, bytes);

            log.info("创建 Zookeeper 节点成功，path={}, mode={}", createdPath, createMode);
            return createdPath;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建 Zookeeper 节点失败，path={}, mode={}", nodePath, createMode, e);
            throw new IllegalStateException("创建 Zookeeper 节点失败：" + nodePath, e);
        }
    }

    /**
     * 规范化节点路径
     *
     * @param path 原始节点路径
     * @return 规范化后的节点路径
     */
    private String normalizePath(String path) {
        if (StrUtil.isBlank(path)) {
            throw new IllegalArgumentException("Zookeeper 节点路径不能为空");
        }

        String nodePath = StrUtil.trim(path);
        if (!StrUtil.startWith(nodePath, "/")) {
            nodePath = StrUtil.prependIfMissing(nodePath, "/");
        }

        if (StrUtil.equals(nodePath, "/")) {
            throw new IllegalArgumentException("不允许直接操作 Zookeeper 根节点");
        }

        return nodePath;
    }

}
```

### 节点查询

节点查询主要包括两类操作：查询指定节点的数据，以及查询指定节点下的子节点列表。

查询指定节点数据时，先使用 `checkExists()` 判断节点是否存在。如果节点不存在，返回 `exists=false`，而不是直接抛出异常。这样更适合接口层向前端返回明确状态。

核心代码如下：

```java
Stat stat = curatorFramework.checkExists().forPath(nodePath);
if (stat == null) {
    return ZookeeperNodeVO.builder()
            .path(nodePath)
            .exists(false)
            .build();
}

byte[] bytes = curatorFramework.getData().forPath(nodePath);
String data = StrUtil.str(bytes, CharsetUtil.CHARSET_UTF_8);
```

查询子节点时，使用 `getChildren()`：

```java
List<String> children = curatorFramework.getChildren().forPath(nodePath);
```

使用示例：

```java
ZookeeperNodeVO node = zookeeperNodeService.getNode("/config/app-name");
List<String> children = zookeeperNodeService.listChildren("/config");
```

如果前面配置了 `namespace: spring-boot-demo`，这里的 `/config/app-name` 实际对应 Zookeeper 服务端中的 `/spring-boot-demo/config/app-name`。

### 节点更新

节点更新用于修改已有节点的数据。更新前建议先判断节点是否存在。如果节点不存在，直接抛出业务异常，避免误以为更新成功。

核心代码如下：

```java
Stat stat = curatorFramework.checkExists().forPath(nodePath);
if (stat == null) {
    throw new KeeperException.NoNodeException(nodePath);
}

byte[] bytes = StrUtil.bytes(StrUtil.nullToEmpty(data), CharsetUtil.CHARSET_UTF_8);
Stat newStat = curatorFramework.setData().forPath(nodePath, bytes);
```

Zookeeper 每次更新节点数据后，节点版本号都会递增。版本号可以用于后续实现乐观锁更新，避免多个实例同时覆盖同一个节点数据。

普通更新示例：

```java
ZookeeperNodeVO node = zookeeperNodeService.updateNode("/config/app-name", "new-value");
```

如果需要保存 JSON 配置，可以配合 Hutool 的 `JSONUtil` 转换对象：

```java
String json = cn.hutool.json.JSONUtil.createObj()
        .set("enabled", true)
        .set("timeout", 3000)
        .toString();

ZookeeperNodeVO node = zookeeperNodeService.updateNode("/config/system", json);
```

### 节点删除

节点删除用于移除指定 Zookeeper 节点。实际业务中需要谨慎处理删除操作，因为删除节点可能会触发监听器回调，也可能影响服务发现、配置管理或分布式锁等功能。

当前实现中使用了 `deletingChildrenIfNeeded()`，表示如果当前节点存在子节点，会一并删除子节点。

核心代码如下：

```java
curatorFramework.delete()
        .deletingChildrenIfNeeded()
        .forPath(nodePath);
```

删除示例：

```java
Boolean deleted = zookeeperNodeService.deleteNode("/config/app-name");
```

删除逻辑说明：

| 情况           | 处理结果                 |
| -------------- | ------------------------ |
| 节点存在       | 删除节点并返回 `true`    |
| 节点不存在     | 不抛异常，返回 `false`   |
| 节点存在子节点 | 递归删除当前节点及子节点 |
| 删除失败       | 记录错误日志并抛出异常   |

节点删除在生产环境中建议增加权限控制和操作审计。如果节点用于配置管理或服务注册，不建议暴露无保护的删除接口。



## 监听机制

监听机制用于感知 Zookeeper 节点变化、子节点变化和客户端连接状态变化。Spring Boot 项目中推荐使用 Curator 提供的 `CuratorCache` 和 `ConnectionStateListener` 实现监听能力，避免直接使用 Zookeeper 原生 Watcher 带来的重复注册和状态处理问题。

### 节点监听

节点监听用于监听指定节点的数据变化、节点创建和节点删除事件。适用于配置变更通知、开关状态变更、业务节点状态感知等场景。

Curator 5.x 推荐使用 `CuratorCache` 实现节点监听。`CuratorCache` 可以持续监听指定路径下的节点变化，并在节点创建、更新、删除时触发回调。

推荐新增监听服务文件：

```text
src/main/java/io/github/atengk/zookeeper
└── listener
    └── ZookeeperWatchService.java
```

以下代码封装了节点监听和子节点监听能力，可被业务服务或测试接口直接调用。

文件位置：`src/main/java/io/github/atengk/zookeeper/listener/ZookeeperWatchService.java`

```java
package io.github.atengk.zookeeper.listener;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zookeeper 节点监听服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZookeeperWatchService {

    private final CuratorFramework curatorFramework;

    private final Map<String, CuratorCache> cacheMap = new ConcurrentHashMap<>();

    public void watchNode(String path) {
        String nodePath = normalizePath(path);
        String cacheKey = "node:" + nodePath;

        if (cacheMap.containsKey(cacheKey)) {
            log.info("Zookeeper 节点监听已存在，path={}", nodePath);
            return;
        }

        CuratorCache cache = CuratorCache.builder(curatorFramework, nodePath).build();

        CuratorCacheListener listener = CuratorCacheListener.builder()
                .forAll((type, oldData, data) -> {
                    String eventPath = getEventPath(oldData, data);
                    if (!StrUtil.equals(eventPath, nodePath)) {
                        return;
                    }

                    String oldValue = getDataValue(oldData);
                    String newValue = getDataValue(data);

                    switch (type) {
                        case NODE_CREATED -> log.info("监听到 Zookeeper 节点创建，path={}, data={}", eventPath, newValue);
                        case NODE_CHANGED -> log.info("监听到 Zookeeper 节点更新，path={}, oldData={}, newData={}", eventPath, oldValue, newValue);
                        case NODE_DELETED -> log.info("监听到 Zookeeper 节点删除，path={}, oldData={}", eventPath, oldValue);
                        default -> log.info("监听到 Zookeeper 节点事件，type={}, path={}", type, eventPath);
                    }
                })
                .build();

        cache.listenable().addListener(listener);
        cache.start();

        cacheMap.put(cacheKey, cache);
        log.info("启动 Zookeeper 节点监听成功，path={}", nodePath);
    }

    public void watchChildren(String path) {
        String parentPath = normalizePath(path);
        String cacheKey = "children:" + parentPath;

        if (cacheMap.containsKey(cacheKey)) {
            log.info("Zookeeper 子节点监听已存在，path={}", parentPath);
            return;
        }

        CuratorCache cache = CuratorCache.builder(curatorFramework, parentPath).build();

        CuratorCacheListener listener = CuratorCacheListener.builder()
                .forAll((type, oldData, data) -> {
                    String eventPath = getEventPath(oldData, data);
                    if (!isDirectChild(parentPath, eventPath)) {
                        return;
                    }

                    String oldValue = getDataValue(oldData);
                    String newValue = getDataValue(data);

                    switch (type) {
                        case NODE_CREATED -> log.info("监听到 Zookeeper 子节点创建，parentPath={}, childPath={}, data={}", parentPath, eventPath, newValue);
                        case NODE_CHANGED -> log.info("监听到 Zookeeper 子节点更新，parentPath={}, childPath={}, oldData={}, newData={}", parentPath, eventPath, oldValue, newValue);
                        case NODE_DELETED -> log.info("监听到 Zookeeper 子节点删除，parentPath={}, childPath={}, oldData={}", parentPath, eventPath, oldValue);
                        default -> log.info("监听到 Zookeeper 子节点事件，type={}, parentPath={}, childPath={}", type, parentPath, eventPath);
                    }
                })
                .build();

        cache.listenable().addListener(listener);
        cache.start();

        cacheMap.put(cacheKey, cache);
        log.info("启动 Zookeeper 子节点监听成功，path={}", parentPath);
    }

    public void closeWatch(String path) {
        String nodePath = normalizePath(path);
        closeCache("node:" + nodePath);
        closeCache("children:" + nodePath);
    }

    @PreDestroy
    public void destroy() {
        cacheMap.forEach((key, cache) -> {
            try {
                cache.close();
                log.info("关闭 Zookeeper 监听成功，key={}", key);
            } catch (Exception e) {
                log.warn("关闭 Zookeeper 监听失败，key={}", key, e);
            }
        });
        cacheMap.clear();
    }

    private void closeCache(String cacheKey) {
        CuratorCache cache = cacheMap.remove(cacheKey);
        if (cache == null) {
            return;
        }

        cache.close();
        log.info("关闭 Zookeeper 监听成功，key={}", cacheKey);
    }

    private String getEventPath(ChildData oldData, ChildData data) {
        if (data != null) {
            return data.getPath();
        }
        if (oldData != null) {
            return oldData.getPath();
        }
        return StrUtil.EMPTY;
    }

    private String getDataValue(ChildData childData) {
        if (childData == null || childData.getData() == null) {
            return StrUtil.EMPTY;
        }
        return StrUtil.str(childData.getData(), CharsetUtil.CHARSET_UTF_8);
    }

    private boolean isDirectChild(String parentPath, String childPath) {
        if (StrUtil.isBlank(childPath) || StrUtil.equals(parentPath, childPath)) {
            return false;
        }

        String prefix = StrUtil.addSuffixIfNot(parentPath, "/");
        if (!StrUtil.startWith(childPath, prefix)) {
            return false;
        }

        String remainPath = StrUtil.removePrefix(childPath, prefix);
        return StrUtil.isNotBlank(remainPath) && !StrUtil.contains(remainPath, "/");
    }

    private String normalizePath(String path) {
        if (StrUtil.isBlank(path)) {
            throw new IllegalArgumentException("Zookeeper 节点路径不能为空");
        }

        String nodePath = StrUtil.trim(path);
        return StrUtil.prependIfMissing(nodePath, "/");
    }

}
```

节点监听使用示例：

```java
zookeeperWatchService.watchNode("/config/system");
```

当 `/config/system` 节点被创建、更新或删除时，控制台会输出对应日志。该方式适合监听单个配置节点、业务开关节点或服务状态节点。

### 子节点监听

子节点监听用于监听某个父节点下一级子节点的变化，适用于服务注册发现、任务实例变化、在线节点变化等场景。

使用示例：

```java
zookeeperWatchService.watchChildren("/services/order-service");
```

当 `/services/order-service` 下新增、更新或删除服务实例节点时，会输出子节点变化日志。

常见路径设计如下：

| 场景     | 父节点路径                | 子节点示例                                  |
| -------- | ------------------------- | ------------------------------------------- |
| 服务发现 | `/services/order-service` | `/services/order-service/192.168.1.10:8080` |
| 配置分组 | `/config`                 | `/config/system`                            |
| 任务实例 | `/tasks/export`           | `/tasks/export/instance-01`                 |
| 在线用户 | `/online/users`           | `/online/users/user-10001`                  |

子节点监听需要注意以下事项：

1. `CuratorCache` 会监听指定路径下的节点变化。
2. 示例代码中通过 `isDirectChild` 过滤了非直属子节点。
3. 如果需要监听所有层级子节点，可以去掉直属子节点过滤逻辑。
4. 监听器应避免执行耗时业务，复杂逻辑建议投递到线程池或消息队列处理。
5. 项目关闭时需要释放监听资源，避免客户端连接残留。

### 连接状态监听

连接状态监听用于感知 Curator 客户端与 Zookeeper 服务端之间的连接变化。常见状态包括连接成功、连接断开、连接挂起、Session 丢失、重新连接等。

连接状态监听适合用于记录连接状态、触发服务重新注册、清理本地缓存或发送告警。

推荐新增连接状态监听组件：

```text
src/main/java/io/github/atengk/zookeeper
└── listener
    └── ZookeeperConnectionStateListener.java
```

以下代码在项目启动后注册 Curator 连接状态监听器。

文件位置：`src/main/java/io/github/atengk/zookeeper/listener/ZookeeperConnectionStateListener.java`

```java
package io.github.atengk.zookeeper.listener;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.springframework.stereotype.Component;

/**
 * Zookeeper 连接状态监听器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZookeeperConnectionStateListener {

    private final CuratorFramework curatorFramework;

    @PostConstruct
    public void registerListener() {
        curatorFramework.getConnectionStateListenable().addListener((client, newState) -> {
            if (ConnectionState.CONNECTED == newState) {
                log.info("Zookeeper 连接成功");
                return;
            }

            if (ConnectionState.RECONNECTED == newState) {
                log.info("Zookeeper 重新连接成功");
                return;
            }

            if (ConnectionState.SUSPENDED == newState) {
                log.warn("Zookeeper 连接暂时中断，等待自动重连");
                return;
            }

            if (ConnectionState.LOST == newState) {
                log.error("Zookeeper 会话丢失，临时节点和分布式锁可能已经失效");
                return;
            }

            if (ConnectionState.READ_ONLY == newState) {
                log.warn("Zookeeper 当前处于只读连接状态");
                return;
            }

            log.info("Zookeeper 连接状态变化，state={}", newState);
        });

        log.info("注册 Zookeeper 连接状态监听器成功");
    }

}
```

连接状态说明：

| 状态          | 说明         | 处理建议                                       |
| ------------- | ------------ | ---------------------------------------------- |
| `CONNECTED`   | 首次连接成功 | 正常记录日志                                   |
| `SUSPENDED`   | 连接暂时中断 | 暂停依赖强一致性的业务操作                     |
| `RECONNECTED` | 重新连接成功 | 可重新注册临时节点或刷新本地缓存               |
| `LOST`        | Session 丢失 | 临时节点、锁、选举状态可能失效，需要重新初始化 |
| `READ_ONLY`   | 只读连接     | 不建议执行写操作                               |

在生产环境中，`LOST` 状态需要重点关注。因为 Session 丢失后，当前客户端创建的临时节点会被 Zookeeper 删除，当前客户端持有的分布式锁也不再可靠。

## 分布式场景实现

本章节基于前面封装的 Curator 客户端和节点操作能力，实现几个常见分布式场景，包括分布式锁、服务注册与发现和配置管理。示例代码以可直接集成为目标，重点展示核心实现路径。

### 分布式锁

分布式锁用于解决多实例部署场景下的并发互斥问题。例如多个服务实例同时执行定时任务、同时处理同一订单、同时生成同一类报表时，可以使用 Zookeeper 分布式锁保证同一时间只有一个实例进入临界区。

Curator 提供了 `InterProcessMutex` 可重入分布式锁，底层基于 Zookeeper 临时顺序节点实现。业务代码只需要关注加锁、执行业务、释放锁即可。

推荐新增分布式锁服务：

```text
src/main/java/io/github/atengk/zookeeper
└── service
    ├── ZookeeperLockService.java
    └── impl
        └── ZookeeperLockServiceImpl.java
```

分布式锁接口定义如下。

文件位置：`src/main/java/io/github/atengk/zookeeper/service/ZookeeperLockService.java`

```java
package io.github.atengk.zookeeper.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Zookeeper 分布式锁服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface ZookeeperLockService {

    <T> T executeWithLock(String lockPath, long waitTime, TimeUnit timeUnit, Supplier<T> supplier);

}
```

以下代码封装了加锁、执行业务、释放锁的完整流程。

文件位置：`src/main/java/io/github/atengk/zookeeper/service/impl/ZookeeperLockServiceImpl.java`

```java
package io.github.atengk.zookeeper.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.zookeeper.service.ZookeeperLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Zookeeper 分布式锁服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZookeeperLockServiceImpl implements ZookeeperLockService {

    private final CuratorFramework curatorFramework;

    @Override
    public <T> T executeWithLock(String lockPath, long waitTime, TimeUnit timeUnit, Supplier<T> supplier) {
        String realLockPath = normalizeLockPath(lockPath);
        InterProcessMutex lock = new InterProcessMutex(curatorFramework, realLockPath);
        boolean acquired = false;

        try {
            log.info("尝试获取 Zookeeper 分布式锁，path={}, waitTime={}, timeUnit={}", realLockPath, waitTime, timeUnit);
            acquired = lock.acquire(waitTime, timeUnit);

            if (!acquired) {
                log.warn("获取 Zookeeper 分布式锁超时，path={}", realLockPath);
                throw new IllegalStateException("获取 Zookeeper 分布式锁超时：" + realLockPath);
            }

            log.info("获取 Zookeeper 分布式锁成功，path={}", realLockPath);
            return supplier.get();
        } catch (Exception e) {
            log.error("执行 Zookeeper 分布式锁业务失败，path={}", realLockPath, e);
            throw new IllegalStateException("执行 Zookeeper 分布式锁业务失败：" + realLockPath, e);
        } finally {
            if (acquired && lock.isAcquiredInThisProcess()) {
                try {
                    lock.release();
                    log.info("释放 Zookeeper 分布式锁成功，path={}", realLockPath);
                } catch (Exception e) {
                    log.error("释放 Zookeeper 分布式锁失败，path={}", realLockPath, e);
                }
            }
        }
    }

    private String normalizeLockPath(String lockPath) {
        if (StrUtil.isBlank(lockPath)) {
            throw new IllegalArgumentException("Zookeeper 锁路径不能为空");
        }

        String path = StrUtil.trim(lockPath);
        path = StrUtil.prependIfMissing(path, "/");
        return StrUtil.addPrefixIfNot(path, "/locks");
    }

}
```

分布式锁调用示例：

```java
String result = zookeeperLockService.executeWithLock(
        "/order/submit/10001",
        5,
        TimeUnit.SECONDS,
        () -> {
            log.info("执行业务逻辑，订单编号={}", "10001");
            return "success";
        }
);
```

锁路径设计建议：

| 业务场景 | 锁路径示例                            |
| -------- | ------------------------------------- |
| 订单提交 | `/locks/order/submit/{orderId}`       |
| 报表生成 | `/locks/report/generate/{reportType}` |
| 定时任务 | `/locks/job/{jobName}`                |
| 库存扣减 | `/locks/stock/deduct/{skuId}`         |

使用分布式锁时需要注意：

1. 锁路径粒度不能过粗，否则会降低并发能力。
2. 锁路径粒度不能过细，否则会增加节点数量和管理复杂度。
3. 加锁后业务逻辑应尽量短，避免长时间占用锁。
4. 必须在 `finally` 中释放锁。
5. 发生 `LOST` 状态后，当前锁不再可靠，应中断或补偿相关业务。

### 服务注册与发现

服务注册与发现用于维护服务实例在线状态。服务启动时，将当前实例信息注册到 Zookeeper 临时节点；服务下线、进程退出或 Session 失效后，临时节点会自动删除。其他服务可以监听服务路径下的子节点变化，从而感知服务实例上线和下线。

推荐节点结构如下：

```text
/services
└── order-service
    ├── 192.168.1.10:8080
    └── 192.168.1.11:8080
```

推荐新增服务注册组件：

```text
src/main/java/io/github/atengk/zookeeper
└── service
    └── ZookeeperServiceRegistry.java
```

以下代码封装服务注册、服务注销和服务实例查询能力。

文件位置：`src/main/java/io/github/atengk/zookeeper/service/ZookeeperServiceRegistry.java`

```java
package io.github.atengk.zookeeper.service;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Zookeeper 服务注册组件
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZookeeperServiceRegistry {

    private final CuratorFramework curatorFramework;

    public void register(String serviceName, String host, Integer port) {
        String instanceId = host + ":" + port;
        String nodePath = buildInstancePath(serviceName, instanceId);

        String data = JSONUtil.createObj()
                .set("serviceName", serviceName)
                .set("host", host)
                .set("port", port)
                .set("instanceId", instanceId)
                .set("registerTime", System.currentTimeMillis())
                .toString();

        try {
            if (curatorFramework.checkExists().forPath(nodePath) != null) {
                curatorFramework.delete().forPath(nodePath);
                log.info("删除旧的服务实例节点，path={}", nodePath);
            }

            curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(nodePath, StrUtil.bytes(data, CharsetUtil.CHARSET_UTF_8));

            log.info("注册服务实例成功，serviceName={}, instanceId={}, path={}", serviceName, instanceId, nodePath);
        } catch (Exception e) {
            log.error("注册服务实例失败，serviceName={}, instanceId={}", serviceName, instanceId, e);
            throw new IllegalStateException("注册服务实例失败：" + serviceName + "/" + instanceId, e);
        }
    }

    public List<String> listInstances(String serviceName) {
        String servicePath = buildServicePath(serviceName);

        try {
            if (curatorFramework.checkExists().forPath(servicePath) == null) {
                log.info("服务路径不存在，serviceName={}, path={}", serviceName, servicePath);
                return List.of();
            }

            List<String> instances = curatorFramework.getChildren().forPath(servicePath);
            log.info("查询服务实例成功，serviceName={}, size={}", serviceName, instances.size());
            return instances;
        } catch (Exception e) {
            log.error("查询服务实例失败，serviceName={}", serviceName, e);
            throw new IllegalStateException("查询服务实例失败：" + serviceName, e);
        }
    }

    public void unregister(String serviceName, String host, Integer port) {
        String instanceId = host + ":" + port;
        String nodePath = buildInstancePath(serviceName, instanceId);

        try {
            if (curatorFramework.checkExists().forPath(nodePath) == null) {
                log.info("服务实例节点不存在，无需注销，path={}", nodePath);
                return;
            }

            curatorFramework.delete().forPath(nodePath);
            log.info("注销服务实例成功，serviceName={}, instanceId={}", serviceName, instanceId);
        } catch (Exception e) {
            log.error("注销服务实例失败，serviceName={}, instanceId={}", serviceName, instanceId, e);
            throw new IllegalStateException("注销服务实例失败：" + serviceName + "/" + instanceId, e);
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("应用关闭，Zookeeper 临时节点将由 Session 生命周期自动清理");
    }

    private String buildServicePath(String serviceName) {
        if (StrUtil.isBlank(serviceName)) {
            throw new IllegalArgumentException("服务名称不能为空");
        }
        return "/services/" + StrUtil.trim(serviceName);
    }

    private String buildInstancePath(String serviceName, String instanceId) {
        return buildServicePath(serviceName) + "/" + instanceId;
    }

}
```

注册服务实例示例：

```java
zookeeperServiceRegistry.register("order-service", "127.0.0.1", 8080);
```

查询服务实例示例：

```java
List<String> instances = zookeeperServiceRegistry.listInstances("order-service");
```

监听服务实例变化示例：

```java
zookeeperWatchService.watchChildren("/services/order-service");
```

服务注册与发现注意事项：

1. 服务实例节点建议使用临时节点。
2. 服务节点数据可以保存实例地址、端口、权重、版本、启动时间等信息。
3. 服务下线后临时节点会随 Session 失效自动删除。
4. 服务发现方应监听 `/services/{serviceName}` 下的子节点变化。
5. 生产环境建议配合负载均衡策略、健康检查和实例元数据使用。

### 配置管理

配置管理用于在 Zookeeper 中保存少量动态配置，并在配置变更时通知应用。该方式适合保存开关类、小型 JSON 配置、限流参数、任务参数等轻量级配置。

推荐节点结构如下：

```text
/config
├── system
├── order
└── job
```

配置节点示例：

```json
{
  "enabled": true,
  "timeout": 3000,
  "maxRetry": 3
}
```

推荐新增配置管理服务：

```text
src/main/java/io/github/atengk/zookeeper
└── service
    └── ZookeeperConfigService.java
```

以下代码封装配置读取、配置保存和配置监听能力。

文件位置：`src/main/java/io/github/atengk/zookeeper/service/ZookeeperConfigService.java`

```java
package io.github.atengk.zookeeper.service;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.zookeeper.CreateMode;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Zookeeper 配置管理服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZookeeperConfigService {

    private final CuratorFramework curatorFramework;

    private final Map<String, CuratorCache> configCacheMap = new ConcurrentHashMap<>();

    public String getConfig(String configKey) {
        String configPath = buildConfigPath(configKey);

        try {
            if (curatorFramework.checkExists().forPath(configPath) == null) {
                log.info("Zookeeper 配置不存在，key={}, path={}", configKey, configPath);
                return StrUtil.EMPTY;
            }

            byte[] bytes = curatorFramework.getData().forPath(configPath);
            String value = StrUtil.str(bytes, CharsetUtil.CHARSET_UTF_8);

            log.info("读取 Zookeeper 配置成功，key={}, path={}", configKey, configPath);
            return value;
        } catch (Exception e) {
            log.error("读取 Zookeeper 配置失败，key={}, path={}", configKey, configPath, e);
            throw new IllegalStateException("读取 Zookeeper 配置失败：" + configKey, e);
        }
    }

    public void putConfig(String configKey, String configValue) {
        String configPath = buildConfigPath(configKey);
        String value = StrUtil.nullToEmpty(configValue);

        try {
            byte[] bytes = StrUtil.bytes(value, CharsetUtil.CHARSET_UTF_8);

            if (curatorFramework.checkExists().forPath(configPath) == null) {
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(configPath, bytes);

                log.info("创建 Zookeeper 配置成功，key={}, path={}", configKey, configPath);
                return;
            }

            curatorFramework.setData().forPath(configPath, bytes);
            log.info("更新 Zookeeper 配置成功，key={}, path={}", configKey, configPath);
        } catch (Exception e) {
            log.error("保存 Zookeeper 配置失败，key={}, path={}", configKey, configPath, e);
            throw new IllegalStateException("保存 Zookeeper 配置失败：" + configKey, e);
        }
    }

    public void watchConfig(String configKey, Consumer<String> consumer) {
        String configPath = buildConfigPath(configKey);

        if (configCacheMap.containsKey(configPath)) {
            log.info("Zookeeper 配置监听已存在，key={}, path={}", configKey, configPath);
            return;
        }

        CuratorCache cache = CuratorCache.builder(curatorFramework, configPath).build();

        CuratorCacheListener listener = CuratorCacheListener.builder()
                .forAll((type, oldData, data) -> {
                    if (data == null || !StrUtil.equals(data.getPath(), configPath)) {
                        return;
                    }

                    String value = StrUtil.str(data.getData(), CharsetUtil.CHARSET_UTF_8);
                    log.info("监听到 Zookeeper 配置变化，key={}, type={}, value={}", configKey, type, value);
                    consumer.accept(value);
                })
                .build();

        cache.listenable().addListener(listener);
        cache.start();

        configCacheMap.put(configPath, cache);
        log.info("启动 Zookeeper 配置监听成功，key={}, path={}", configKey, configPath);
    }

    @PreDestroy
    public void destroy() {
        configCacheMap.forEach((path, cache) -> {
            try {
                cache.close();
                log.info("关闭 Zookeeper 配置监听成功，path={}", path);
            } catch (Exception e) {
                log.warn("关闭 Zookeeper 配置监听失败，path={}", path, e);
            }
        });
        configCacheMap.clear();
    }

    private String buildConfigPath(String configKey) {
        if (StrUtil.isBlank(configKey)) {
            throw new IllegalArgumentException("配置 Key 不能为空");
        }

        String key = StrUtil.trim(configKey);
        key = StrUtil.removePrefix(key, "/");
        return "/config/" + key;
    }

}
```

保存配置示例：

```java
String configValue = cn.hutool.json.JSONUtil.createObj()
        .set("enabled", true)
        .set("timeout", 3000)
        .set("maxRetry", 3)
        .toString();

zookeeperConfigService.putConfig("system", configValue);
```

读取配置示例：

```java
String configValue = zookeeperConfigService.getConfig("system");
```

监听配置变化示例：

```java
zookeeperConfigService.watchConfig("system", value -> {
    log.info("接收到最新系统配置，value={}", value);
});
```

配置管理注意事项：

1. Zookeeper 适合保存小体积配置，不适合保存大 JSON、大文本或频繁变更的数据。
2. 配置节点建议使用持久节点。
3. 配置值建议统一使用 JSON 字符串，便于扩展字段。
4. 配置监听回调中不建议直接执行耗时任务。
5. 配置变更应保留操作日志，生产环境建议增加权限控制和审计记录。
6. 如果配置强依赖一致性，服务启动时应先读取当前配置，再注册监听器。



## 接口设计

本章节用于提供 Zookeeper 功能的 HTTP 测试接口，包括节点管理接口、监听测试接口和分布式锁测试接口。接口层主要用于开发验证和调试，生产环境中应结合权限控制、操作审计、参数白名单和路径限制使用。

建议新增以下文件：

```text
src/main/java/io/github/atengk/zookeeper
├── common
│   └── ApiResult.java
├── controller
│   ├── ZookeeperNodeController.java
│   ├── ZookeeperWatchController.java
│   └── ZookeeperLockController.java
└── dto
    ├── NodeCreateRequest.java
    ├── NodeUpdateRequest.java
    └── LockTestRequest.java
```

先定义统一接口返回对象，后续所有 Controller 都使用该对象返回数据。

文件位置：`src/main/java/io/github/atengk/zookeeper/common/ApiResult.java`

```java
package io.github.atengk.zookeeper.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一返回对象
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功返回
     *
     * @param data 返回数据
     * @param <T> 数据类型
     * @return 统一返回对象
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 成功返回
     *
     * @return 统一返回对象
     */
    public static ApiResult<Void> success() {
        return new ApiResult<>(200, "操作成功", null);
    }

    /**
     * 失败返回
     *
     * @param code 状态码
     * @param message 错误消息
     * @return 统一返回对象
     */
    public static ApiResult<Void> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null);
    }

}
```

### 节点管理接口

节点管理接口用于验证 Zookeeper 节点的创建、查询、更新和删除功能。由于 Zookeeper 节点路径通常包含 `/`，接口设计时不建议将节点路径放在 PathVariable 中，推荐通过 Query 参数或 RequestBody 传递。

节点创建请求对象如下。

文件位置：`src/main/java/io/github/atengk/zookeeper/dto/NodeCreateRequest.java`

```java
package io.github.atengk.zookeeper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Zookeeper 节点创建请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class NodeCreateRequest {

    /**
     * 节点路径
     */
    @NotBlank(message = "节点路径不能为空")
    private String path;

    /**
     * 节点数据
     */
    private String data;

    /**
     * 是否为临时节点
     */
    private Boolean ephemeral = false;

}
```

节点更新请求对象如下。

文件位置：`src/main/java/io/github/atengk/zookeeper/dto/NodeUpdateRequest.java`

```java
package io.github.atengk.zookeeper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Zookeeper 节点更新请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class NodeUpdateRequest {

    /**
     * 节点路径
     */
    @NotBlank(message = "节点路径不能为空")
    private String path;

    /**
     * 节点数据
     */
    private String data;

}
```

节点管理 Controller 如下。

文件位置：`src/main/java/io/github/atengk/zookeeper/controller/ZookeeperNodeController.java`

```java
package io.github.atengk.zookeeper.controller;

import cn.hutool.core.util.BooleanUtil;
import io.github.atengk.zookeeper.common.ApiResult;
import io.github.atengk.zookeeper.dto.NodeCreateRequest;
import io.github.atengk.zookeeper.dto.NodeUpdateRequest;
import io.github.atengk.zookeeper.service.ZookeeperNodeService;
import io.github.atengk.zookeeper.vo.ZookeeperNodeVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Zookeeper 节点管理接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/zookeeper/nodes")
public class ZookeeperNodeController {

    private final ZookeeperNodeService zookeeperNodeService;

    /**
     * 创建节点
     *
     * @param request 创建请求
     * @return 节点路径
     */
    @PostMapping
    public ApiResult<String> createNode(@Valid @RequestBody NodeCreateRequest request) {
        String path;
        if (BooleanUtil.isTrue(request.getEphemeral())) {
            path = zookeeperNodeService.createEphemeralNode(request.getPath(), request.getData());
        } else {
            path = zookeeperNodeService.createPersistentNode(request.getPath(), request.getData());
        }

        log.info("接口创建 Zookeeper 节点成功，path={}, ephemeral={}", path, request.getEphemeral());
        return ApiResult.success(path);
    }

    /**
     * 查询节点数据
     *
     * @param path 节点路径
     * @return 节点信息
     */
    @GetMapping
    public ApiResult<ZookeeperNodeVO> getNode(@RequestParam @NotBlank(message = "节点路径不能为空") String path) {
        ZookeeperNodeVO node = zookeeperNodeService.getNode(path);
        return ApiResult.success(node);
    }

    /**
     * 查询子节点列表
     *
     * @param path 节点路径
     * @return 子节点列表
     */
    @GetMapping("/children")
    public ApiResult<List<String>> listChildren(@RequestParam @NotBlank(message = "节点路径不能为空") String path) {
        List<String> children = zookeeperNodeService.listChildren(path);
        return ApiResult.success(children);
    }

    /**
     * 更新节点数据
     *
     * @param request 更新请求
     * @return 节点信息
     */
    @PutMapping
    public ApiResult<ZookeeperNodeVO> updateNode(@Valid @RequestBody NodeUpdateRequest request) {
        ZookeeperNodeVO node = zookeeperNodeService.updateNode(request.getPath(), request.getData());
        log.info("接口更新 Zookeeper 节点成功，path={}", request.getPath());
        return ApiResult.success(node);
    }

    /**
     * 删除节点
     *
     * @param path 节点路径
     * @return 是否删除成功
     */
    @DeleteMapping
    public ApiResult<Boolean> deleteNode(@RequestParam @NotBlank(message = "节点路径不能为空") String path) {
        Boolean deleted = zookeeperNodeService.deleteNode(path);
        log.info("接口删除 Zookeeper 节点完成，path={}, deleted={}", path, deleted);
        return ApiResult.success(deleted);
    }

}
```

节点管理接口说明：

| 功能       | 请求方法 | 接口路径                                   | 说明                   |
| ---------- | -------- | ------------------------------------------ | ---------------------- |
| 创建节点   | `POST`   | `/api/zookeeper/nodes`                     | 创建持久节点或临时节点 |
| 查询节点   | `GET`    | `/api/zookeeper/nodes?path=/demo`          | 查询指定节点数据       |
| 查询子节点 | `GET`    | `/api/zookeeper/nodes/children?path=/demo` | 查询指定节点下的子节点 |
| 更新节点   | `PUT`    | `/api/zookeeper/nodes`                     | 更新指定节点数据       |
| 删除节点   | `DELETE` | `/api/zookeeper/nodes?path=/demo`          | 删除指定节点及其子节点 |

创建持久节点：

```bash
curl -X POST "http://127.0.0.1:8080/api/zookeeper/nodes" \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/config/system",
    "data": "{\"enabled\":true,\"timeout\":3000}",
    "ephemeral": false
  }'
```

查询节点数据：

```bash
curl "http://127.0.0.1:8080/api/zookeeper/nodes?path=/config/system"
```

更新节点数据：

```bash
curl -X PUT "http://127.0.0.1:8080/api/zookeeper/nodes" \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/config/system",
    "data": "{\"enabled\":false,\"timeout\":5000}"
  }'
```

删除节点：

```bash
curl -X DELETE "http://127.0.0.1:8080/api/zookeeper/nodes?path=/config/system"
```

### 监听测试接口

监听测试接口用于手动启动节点监听和子节点监听。监听接口一般只用于开发和测试环境，生产环境中更推荐在应用启动时按业务配置自动注册监听器。

监听测试 Controller 如下。

文件位置：`src/main/java/io/github/atengk/zookeeper/controller/ZookeeperWatchController.java`

```java
package io.github.atengk.zookeeper.controller;

import io.github.atengk.zookeeper.common.ApiResult;
import io.github.atengk.zookeeper.listener.ZookeeperWatchService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Zookeeper 监听测试接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/zookeeper/watch")
public class ZookeeperWatchController {

    private final ZookeeperWatchService zookeeperWatchService;

    /**
     * 启动节点监听
     *
     * @param path 节点路径
     * @return 操作结果
     */
    @PostMapping("/node")
    public ApiResult<Void> watchNode(@RequestParam @NotBlank(message = "节点路径不能为空") String path) {
        zookeeperWatchService.watchNode(path);
        log.info("接口启动 Zookeeper 节点监听成功，path={}", path);
        return ApiResult.success();
    }

    /**
     * 启动子节点监听
     *
     * @param path 父节点路径
     * @return 操作结果
     */
    @PostMapping("/children")
    public ApiResult<Void> watchChildren(@RequestParam @NotBlank(message = "节点路径不能为空") String path) {
        zookeeperWatchService.watchChildren(path);
        log.info("接口启动 Zookeeper 子节点监听成功，path={}", path);
        return ApiResult.success();
    }

    /**
     * 关闭节点监听
     *
     * @param path 节点路径
     * @return 操作结果
     */
    @DeleteMapping
    public ApiResult<Void> closeWatch(@RequestParam @NotBlank(message = "节点路径不能为空") String path) {
        zookeeperWatchService.closeWatch(path);
        log.info("接口关闭 Zookeeper 监听成功，path={}", path);
        return ApiResult.success();
    }

}
```

监听测试接口说明：

| 功能           | 请求方法 | 接口路径                                                     | 说明                           |
| -------------- | -------- | ------------------------------------------------------------ | ------------------------------ |
| 启动节点监听   | `POST`   | `/api/zookeeper/watch/node?path=/config/system`              | 监听指定节点创建、更新、删除   |
| 启动子节点监听 | `POST`   | `/api/zookeeper/watch/children?path=/services/order-service` | 监听指定父节点下一级子节点变化 |
| 关闭监听       | `DELETE` | `/api/zookeeper/watch?path=/config/system`                   | 关闭指定路径的监听             |

启动节点监听：

```bash
curl -X POST "http://127.0.0.1:8080/api/zookeeper/watch/node?path=/config/system"
```

更新节点触发监听：

```bash
curl -X PUT "http://127.0.0.1:8080/api/zookeeper/nodes" \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/config/system",
    "data": "{\"enabled\":true,\"timeout\":8000}"
  }'
```

启动子节点监听：

```bash
curl -X POST "http://127.0.0.1:8080/api/zookeeper/watch/children?path=/services/order-service"
```

创建子节点触发监听：

```bash
curl -X POST "http://127.0.0.1:8080/api/zookeeper/nodes" \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/services/order-service/127.0.0.1:8080",
    "data": "{\"host\":\"127.0.0.1\",\"port\":8080}",
    "ephemeral": true
  }'
```

### 分布式锁测试接口

分布式锁测试接口用于验证多个请求同时进入时，是否只有一个请求能够在指定锁路径下执行业务逻辑。测试时可以通过并发调用接口观察日志。

分布式锁测试请求对象如下。

文件位置：`src/main/java/io/github/atengk/zookeeper/dto/LockTestRequest.java`

```java
package io.github.atengk.zookeeper.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Zookeeper 分布式锁测试请求
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class LockTestRequest {

    /**
     * 锁路径
     */
    @NotBlank(message = "锁路径不能为空")
    private String lockPath;

    /**
     * 业务标识
     */
    @NotBlank(message = "业务标识不能为空")
    private String businessKey;

    /**
     * 等待锁时间，单位秒
     */
    @Min(value = 1, message = "等待锁时间不能小于 1 秒")
    private Long waitSeconds = 5L;

    /**
     * 模拟业务执行耗时，单位毫秒
     */
    @Min(value = 0, message = "模拟执行耗时不能小于 0 毫秒")
    private Long sleepMillis = 3000L;

}
```

分布式锁测试 Controller 如下。

文件位置：`src/main/java/io/github/atengk/zookeeper/controller/ZookeeperLockController.java`

```java
package io.github.atengk.zookeeper.controller;

import cn.hutool.core.thread.ThreadUtil;
import io.github.atengk.zookeeper.common.ApiResult;
import io.github.atengk.zookeeper.dto.LockTestRequest;
import io.github.atengk.zookeeper.service.ZookeeperLockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Zookeeper 分布式锁测试接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/zookeeper/locks")
public class ZookeeperLockController {

    private final ZookeeperLockService zookeeperLockService;

    /**
     * 测试分布式锁
     *
     * @param request 锁测试请求
     * @return 执行结果
     */
    @PostMapping("/test")
    public ApiResult<String> testLock(@Valid @RequestBody LockTestRequest request) {
        String result = zookeeperLockService.executeWithLock(
                request.getLockPath(),
                request.getWaitSeconds(),
                TimeUnit.SECONDS,
                () -> {
                    log.info("开始执行加锁业务，businessKey={}, sleepMillis={}",
                            request.getBusinessKey(), request.getSleepMillis());

                    ThreadUtil.sleep(request.getSleepMillis());

                    log.info("加锁业务执行完成，businessKey={}", request.getBusinessKey());
                    return "分布式锁业务执行成功：" + request.getBusinessKey();
                }
        );

        return ApiResult.success(result);
    }

}
```

分布式锁测试接口说明：

| 功能         | 请求方法 | 接口路径                    | 说明                       |
| ------------ | -------- | --------------------------- | -------------------------- |
| 测试分布式锁 | `POST`   | `/api/zookeeper/locks/test` | 在指定锁路径下模拟执行业务 |

调用示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/zookeeper/locks/test" \
  -H "Content-Type: application/json" \
  -d '{
    "lockPath": "/order/submit/10001",
    "businessKey": "order-10001",
    "waitSeconds": 5,
    "sleepMillis": 3000
  }'
```

并发测试可以在两个终端中同时执行上述命令。正常情况下，第一个请求获取锁并执行业务，第二个请求会等待锁释放后继续执行，或者在等待超时后返回异常。

## 异常处理

本章节用于统一处理 Zookeeper 连接异常、节点操作异常、参数校验异常和业务异常。接口层不应直接暴露 Curator 或 Zookeeper 原始异常堆栈，而应返回清晰、稳定、可排查的错误信息。

### 连接异常处理

连接异常主要包括 Zookeeper 服务不可用、网络中断、Session 超时、Session 丢失等情况。Curator 内部会根据配置的重试策略自动重试，但业务层仍需要识别关键连接状态。

前面已经通过 `ZookeeperConnectionStateListener` 监听连接状态。这里建议再增加健康检查组件，用于通过 Actuator 查看 Zookeeper 连接是否正常。

文件位置：`src/main/java/io/github/atengk/zookeeper/config/ZookeeperHealthIndicator.java`

```java
package io.github.atengk.zookeeper.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Zookeeper 健康检查
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZookeeperHealthIndicator implements HealthIndicator {

    private final CuratorFramework curatorFramework;

    /**
     * 检查 Zookeeper 健康状态
     *
     * @return 健康检查结果
     */
    @Override
    public Health health() {
        try {
            boolean started = CuratorFrameworkState.STARTED == curatorFramework.getState();
            boolean connected = curatorFramework.getZookeeperClient().isConnected();

            if (started && connected) {
                return Health.up()
                        .withDetail("state", curatorFramework.getState().name())
                        .withDetail("connected", true)
                        .build();
            }

            log.warn("Zookeeper 健康检查未通过，state={}, connected={}",
                    curatorFramework.getState().name(), connected);

            return Health.down()
                    .withDetail("state", curatorFramework.getState().name())
                    .withDetail("connected", connected)
                    .build();
        } catch (Exception e) {
            log.error("Zookeeper 健康检查异常", e);
            return Health.down(e).build();
        }
    }

}
```

访问健康检查接口：

```bash
curl http://127.0.0.1:8080/actuator/health
```

返回示例：

```json
{
  "status": "UP"
}
```

如果需要查看详细健康信息，可以在 `application.yml` 中开启详情展示。

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoint:
    health:
      # 开发环境可展示健康检查详情，生产环境建议按权限控制
      show-details: always
  endpoints:
    web:
      exposure:
        # 暴露健康检查接口
        include: health,info
```

连接异常处理建议：

| 异常场景         | 表现                           | 处理建议                                  |
| ---------------- | ------------------------------ | ----------------------------------------- |
| Zookeeper 未启动 | 应用启动连接失败或健康检查失败 | 检查服务端口、容器日志和连接地址          |
| 网络短暂中断     | 状态变为 `SUSPENDED`           | 暂停强一致写操作，等待 Curator 重连       |
| Session 丢失     | 状态变为 `LOST`                | 重新注册临时节点，重新初始化锁和选举状态  |
| 只读连接         | 状态变为 `READ_ONLY`           | 避免执行写操作                            |
| 长时间不可用     | 接口持续异常                   | 触发告警，必要时降级依赖 Zookeeper 的业务 |

### 节点操作异常处理

节点操作异常包括节点不存在、节点已存在、路径非法、无权限、连接中断、版本冲突等。建议通过全局异常处理器统一返回错误响应，并记录必要日志。

新增全局异常处理器。

文件位置：`src/main/java/io/github/atengk/zookeeper/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.zookeeper.common;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求体参数校验异常
     *
     * @param e 参数校验异常
     * @return 统一返回对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .orElse("请求参数不合法");

        log.warn("请求体参数校验失败，message={}", message);
        return ApiResult.fail(400, message);
    }

    /**
     * 处理表单参数绑定异常
     *
     * @param e 参数绑定异常
     * @return 统一返回对象
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .orElse("请求参数绑定失败");

        log.warn("请求参数绑定失败，message={}", message);
        return ApiResult.fail(400, message);
    }

    /**
     * 处理 Query 参数校验异常
     *
     * @param e 参数校验异常
     * @return 统一返回对象
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(item -> item.getPropertyPath() + "：" + item.getMessage())
                .orElse("请求参数不合法");

        log.warn("请求参数校验失败，message={}", message);
        return ApiResult.fail(400, message);
    }

    /**
     * 处理请求体不可读异常
     *
     * @param e 请求体异常
     * @return 统一返回对象
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败，message={}", e.getMessage());
        return ApiResult.fail(400, "请求体格式不正确");
    }

    /**
     * 处理业务参数异常
     *
     * @param e 参数异常
     * @return 统一返回对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("业务参数异常，message={}", e.getMessage());
        return ApiResult.fail(400, e.getMessage());
    }

    /**
     * 处理业务状态异常
     *
     * @param e 状态异常
     * @return 统一返回对象
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResult<Void> handleIllegalStateException(IllegalStateException e) {
        log.error("业务状态异常，message={}", e.getMessage(), e);
        return ApiResult.fail(500, e.getMessage());
    }

    /**
     * 处理 Zookeeper 异常
     *
     * @param e Zookeeper 异常
     * @return 统一返回对象
     */
    @ExceptionHandler(KeeperException.class)
    public ApiResult<Void> handleKeeperException(KeeperException e) {
        String message = StrUtil.format("Zookeeper 操作失败，code={}", e.code());
        log.error("Zookeeper 原生异常，code={}, message={}", e.code(), e.getMessage(), e);
        return ApiResult.fail(500, message);
    }

    /**
     * 处理未知异常
     *
     * @param e 未知异常
     * @return 统一返回对象
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统未知异常", e);
        return ApiResult.fail(500, "系统异常，请查看服务日志");
    }

}
```

常见节点操作异常说明：

| 异常                      | 常见原因     | 处理建议                                 |
| ------------------------- | ------------ | ---------------------------------------- |
| `NoNodeException`         | 节点不存在   | 更新、删除前先检查节点是否存在           |
| `NodeExistsException`     | 节点已存在   | 创建前先检查节点，或改为更新             |
| `BadVersionException`     | 版本不一致   | 使用版本号实现乐观锁时需要重试或提示冲突 |
| `NoAuthException`         | 没有权限     | 检查 ACL 配置和认证信息                  |
| `ConnectionLossException` | 连接中断     | 依赖 Curator 重试，必要时由业务补偿      |
| `SessionExpiredException` | Session 过期 | 重新初始化临时节点、锁、监听器           |

接口错误返回示例：

```json
{
  "code": 400,
  "message": "path：节点路径不能为空",
  "data": null
}
{
  "code": 500,
  "message": "查询 Zookeeper 节点失败：/config/system",
  "data": null
}
```

### 重试机制设计

Zookeeper 操作可能因为网络抖动、服务端短暂不可用、连接重建等原因失败。Curator 推荐通过 `RetryPolicy` 配置统一重试策略，避免在业务代码中大量编写重复重试逻辑。

前文 `CuratorConfig` 中使用的是指数退避重试策略：

```java
RetryPolicy retryPolicy = new ExponentialBackoffRetry(
        zookeeperProperties.getBaseSleepTimeMs(),
        zookeeperProperties.getMaxRetries()
);
```

配置含义如下：

| 参数                      | 说明                             |
| ------------------------- | -------------------------------- |
| `baseSleepTimeMs`         | 第一次重试的基础等待时间         |
| `maxRetries`              | 最大重试次数                     |
| `ExponentialBackoffRetry` | 指数退避策略，重试间隔会逐步增加 |

推荐配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
zookeeper:
  # Zookeeper 连接地址
  connect-string: 127.0.0.1:2181
  # 会话超时时间，单位毫秒
  session-timeout-ms: 60000
  # 连接超时时间，单位毫秒
  connection-timeout-ms: 15000
  # Curator 命名空间
  namespace: spring-boot-demo
  # 最大重试次数
  max-retries: 3
  # 重试基础间隔时间，单位毫秒
  base-sleep-time-ms: 1000
```

重试策略选择建议：

| 策略                             | 说明                     | 适用场景                             |
| -------------------------------- | ------------------------ | ------------------------------------ |
| `ExponentialBackoffRetry`        | 指数退避重试             | 通用推荐，适合大多数业务场景         |
| `RetryNTimes`                    | 固定次数重试             | 简单测试或明确次数限制的场景         |
| `RetryOneTime`                   | 只重试一次               | 对延迟敏感的轻量操作                 |
| `RetryForever`                   | 无限重试                 | 不建议普通业务使用，容易掩盖故障     |
| `BoundedExponentialBackoffRetry` | 有最大间隔限制的指数退避 | 生产环境可选，更容易控制重试间隔上限 |

重试机制设计原则：

1. 连接级重试交给 Curator 统一处理。
2. 业务代码不应无脑循环重试，避免放大故障。
3. 非幂等操作需要谨慎重试，例如创建节点、删除节点、扣减库存等。
4. 查询类操作通常可以安全重试。
5. 节点创建如果返回连接异常，需要再次查询节点是否已经创建成功，避免重复创建导致误判。
6. 分布式锁获取失败不建议无限重试，应设置明确等待时间。
7. Session 丢失不是普通重试可以解决的问题，需要重新注册临时节点、重新参与选举或重新获取锁。

对于关键业务，可以在服务层增加一次结果确认。例如创建节点时，如果出现连接中断，可以再次查询节点是否存在：

```java
try {
    curatorFramework.create()
            .creatingParentsIfNeeded()
            .forPath(nodePath, bytes);
} catch (Exception e) {
    if (curatorFramework.checkExists().forPath(nodePath) != null) {
        log.warn("创建节点出现异常但节点已存在，按创建成功处理，path={}", nodePath);
        return nodePath;
    }
    throw e;
}
```

该模式适合“创建后可通过路径确认结果”的场景。对于需要严格一致性的业务，应结合业务流水号、幂等 Key 和补偿机制设计，不建议只依赖客户端重试。



## 功能验证

本章节用于验证 Spring Boot 3 集成 Zookeeper 后的整体功能是否正常，包括本地 Zookeeper 服务状态、Spring Boot 应用启动状态、节点接口调用、监听事件触发、分布式锁互斥效果和日志输出结果。

### 本地环境验证

本地环境验证主要确认 Zookeeper 服务、Spring Boot 应用和 Curator 客户端连接是否正常。建议按照“先启动 Zookeeper，再启动 Spring Boot 应用，最后调用接口”的顺序进行验证。

启动 Zookeeper 服务：

```bash
# 在 docker-compose.yml 所在目录执行
docker compose up -d

# 查看容器状态
docker ps

# 查看 Zookeeper 日志
docker logs -f zookeeper
```

验证 Zookeeper 是否正常响应：

```bash
echo ruok | nc 127.0.0.1 2181
```

正常返回如下：

```text
imok
```

如果本机没有 `nc` 命令，可以进入容器内部使用 `zkCli.sh` 验证：

```bash
# 进入 Zookeeper 容器
docker exec -it zookeeper bash

# 连接 Zookeeper
zkCli.sh -server 127.0.0.1:2181

# 查看根节点
ls /
```

启动 Spring Boot 应用：

```bash
# 编译项目
mvn clean compile

# 启动项目
mvn spring-boot:run
```

验证 Spring Boot 健康检查接口：

```bash
curl http://127.0.0.1:8080/actuator/health
```

正常返回示例：

```json
{
  "status": "UP"
}
```

如果开启了健康检查详情，返回中应能看到 Zookeeper 相关状态信息。正常情况下，Curator 客户端状态应为 `STARTED`，连接状态应为 `connected=true`。

本地环境验证清单如下：

| 验证项             | 验证方式                        | 预期结果           |
| ------------------ | ------------------------------- | ------------------ |
| Zookeeper 容器状态 | `docker ps`                     | 容器处于 `Up` 状态 |
| Zookeeper 服务响应 | `echo ruok | nc 127.0.0.1 2181` | 返回 `imok`        |
| Spring Boot 编译   | `mvn clean compile`             | 编译成功           |
| Spring Boot 启动   | `mvn spring-boot:run`           | 应用正常启动       |
| 健康检查           | `/actuator/health`              | 返回 `UP`          |
| Curator 连接       | 查看启动日志                    | 输出连接成功日志   |

### 接口调用验证

接口调用验证用于确认节点管理、监听测试和分布式锁测试接口是否能够正常工作。建议先验证节点 CRUD，再验证监听，最后验证分布式锁。

先创建一个持久节点：

```bash
curl -X POST "http://127.0.0.1:8080/api/zookeeper/nodes" \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/config/system",
    "data": "{\"enabled\":true,\"timeout\":3000}",
    "ephemeral": false
  }'
```

正常返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "/config/system"
}
```

查询节点数据：

```bash
curl "http://127.0.0.1:8080/api/zookeeper/nodes?path=/config/system"
```

正常返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "path": "/config/system",
    "data": "{\"enabled\":true,\"timeout\":3000}",
    "version": 0,
    "exists": true
  }
}
```

更新节点数据：

```bash
curl -X PUT "http://127.0.0.1:8080/api/zookeeper/nodes" \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/config/system",
    "data": "{\"enabled\":false,\"timeout\":5000}"
  }'
```

再次查询节点数据：

```bash
curl "http://127.0.0.1:8080/api/zookeeper/nodes?path=/config/system"
```

此时返回结果中的 `data` 应变为更新后的内容，`version` 应递增。

查询子节点列表：

```bash
curl "http://127.0.0.1:8080/api/zookeeper/nodes/children?path=/config"
```

正常返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    "system"
  ]
}
```

启动节点监听：

```bash
curl -X POST "http://127.0.0.1:8080/api/zookeeper/watch/node?path=/config/system"
```

然后再次更新节点，触发监听日志：

```bash
curl -X PUT "http://127.0.0.1:8080/api/zookeeper/nodes" \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/config/system",
    "data": "{\"enabled\":true,\"timeout\":8000}"
  }'
```

启动子节点监听：

```bash
curl -X POST "http://127.0.0.1:8080/api/zookeeper/watch/children?path=/services/order-service"
```

创建一个临时服务实例节点：

```bash
curl -X POST "http://127.0.0.1:8080/api/zookeeper/nodes" \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/services/order-service/127.0.0.1:8080",
    "data": "{\"host\":\"127.0.0.1\",\"port\":8080}",
    "ephemeral": true
  }'
```

调用分布式锁测试接口：

```bash
curl -X POST "http://127.0.0.1:8080/api/zookeeper/locks/test" \
  -H "Content-Type: application/json" \
  -d '{
    "lockPath": "/order/submit/10001",
    "businessKey": "order-10001",
    "waitSeconds": 5,
    "sleepMillis": 3000
  }'
```

并发验证分布式锁时，可以打开两个终端，同时执行上述分布式锁测试命令。预期结果是同一个锁路径下的请求不会同时进入业务临界区。

删除节点：

```bash
curl -X DELETE "http://127.0.0.1:8080/api/zookeeper/nodes?path=/config/system"
```

接口验证清单如下：

| 功能       | 接口                                 | 预期结果                 |
| ---------- | ------------------------------------ | ------------------------ |
| 创建节点   | `POST /api/zookeeper/nodes`          | 返回创建后的节点路径     |
| 查询节点   | `GET /api/zookeeper/nodes`           | 返回节点数据和版本号     |
| 查询子节点 | `GET /api/zookeeper/nodes/children`  | 返回子节点名称列表       |
| 更新节点   | `PUT /api/zookeeper/nodes`           | 返回更新后的节点数据     |
| 删除节点   | `DELETE /api/zookeeper/nodes`        | 返回 `true` 或 `false`   |
| 节点监听   | `POST /api/zookeeper/watch/node`     | 节点变化时输出监听日志   |
| 子节点监听 | `POST /api/zookeeper/watch/children` | 子节点变化时输出监听日志 |
| 分布式锁   | `POST /api/zookeeper/locks/test`     | 同一锁路径下串行执行业务 |

### 日志结果验证

日志结果验证用于确认 Curator 客户端初始化、节点操作、监听事件、连接状态和分布式锁流程是否符合预期。开发阶段建议重点观察应用启动日志、接口调用日志和监听回调日志。

应用启动后，预期可以看到 Curator 初始化日志：

```text
初始化 Curator 客户端，connectString=127.0.0.1:2181, namespace=spring-boot-demo
Curator 客户端连接状态：已连接
注册 Zookeeper 连接状态监听器成功
```

创建节点时，预期日志如下：

```text
创建 Zookeeper 节点成功，path=/config/system, mode=PERSISTENT
接口创建 Zookeeper 节点成功，path=/config/system, ephemeral=false
```

查询节点时，预期日志如下：

```text
查询 Zookeeper 节点成功，path=/config/system, version=0
```

更新节点时，预期日志如下：

```text
更新 Zookeeper 节点成功，path=/config/system, version=1
接口更新 Zookeeper 节点成功，path=/config/system
```

节点监听被触发时，预期日志如下：

```text
监听到 Zookeeper 节点更新，path=/config/system, oldData={"enabled":false,"timeout":5000}, newData={"enabled":true,"timeout":8000}
```

子节点监听被触发时，预期日志如下：

```text
监听到 Zookeeper 子节点创建，parentPath=/services/order-service, childPath=/services/order-service/127.0.0.1:8080, data={"host":"127.0.0.1","port":8080}
```

分布式锁测试时，预期日志如下：

```text
尝试获取 Zookeeper 分布式锁，path=/locks/order/submit/10001, waitTime=5, timeUnit=SECONDS
获取 Zookeeper 分布式锁成功，path=/locks/order/submit/10001
开始执行加锁业务，businessKey=order-10001, sleepMillis=3000
加锁业务执行完成，businessKey=order-10001
释放 Zookeeper 分布式锁成功，path=/locks/order/submit/10001
```

连接状态变化时，预期日志如下：

```text
Zookeeper 连接成功
Zookeeper 连接暂时中断，等待自动重连
Zookeeper 重新连接成功
Zookeeper 会话丢失，临时节点和分布式锁可能已经失效
```

常见异常日志排查建议：

| 日志现象                       | 可能原因                         | 处理方式                         |
| ------------------------------ | -------------------------------- | -------------------------------- |
| `Connection refused`           | Zookeeper 未启动或端口不通       | 检查容器状态、端口映射和连接地址 |
| `KeeperErrorCode = NoNode`     | 操作的节点不存在                 | 创建节点后再更新或查询           |
| `KeeperErrorCode = NodeExists` | 创建的节点已存在                 | 改用更新接口或先删除旧节点       |
| `Session expired`              | Session 超时或服务端重启         | 重新注册临时节点、重新获取锁     |
| 监听没有触发                   | 监听路径错误或节点不在命名空间下 | 检查 `namespace` 和实际节点路径  |
| 锁长时间等待                   | 有其他请求持有同一路径锁         | 检查业务执行耗时和锁路径粒度     |

## 总结与扩展

本文档完成了 Spring Boot 3 集成 Zookeeper 的基础开发流程，包括环境准备、依赖配置、Curator 客户端初始化、节点管理、监听机制、分布式锁、服务注册发现、配置管理、接口设计、异常处理和功能验证。通过这些内容，可以构建一个具备基础分布式协调能力的 Spring Boot 示例项目。

### 开发注意事项

Zookeeper 更适合做分布式协调，不适合承担业务数据库、缓存系统或大规模配置中心的职责。开发时应明确它的使用边界，避免把大量业务数据、高频变更数据或复杂配置全部写入 Zookeeper。

节点路径设计需要提前规划。建议按业务域划分路径，例如 `/config`、`/services`、`/locks`、`/tasks`。路径命名应保持稳定、简洁、可读，避免在路径中放入特殊字符、中文或过长业务描述。

推荐路径设计如下：

| 类型     | 推荐路径                               | 说明                     |
| -------- | -------------------------------------- | ------------------------ |
| 配置节点 | `/config/{configKey}`                  | 保存轻量级 JSON 配置     |
| 服务节点 | `/services/{serviceName}/{instanceId}` | 保存服务实例信息         |
| 锁节点   | `/locks/{businessType}/{businessKey}`  | 保存分布式锁临时顺序节点 |
| 任务节点 | `/tasks/{taskName}/{instanceId}`       | 保存任务实例状态         |
| 选举节点 | `/leaders/{bizName}`                   | 用于主从选举             |

临时节点需要重点关注 Session 生命周期。服务注册、分布式锁和主从选举通常依赖临时节点，一旦 Session 丢失，临时节点会被 Zookeeper 自动删除。因此在监听到 `LOST` 状态后，需要根据业务场景重新注册服务节点、重新参与选举或重新获取分布式锁。

监听器中不要直接执行耗时任务。监听回调属于客户端事件处理链路，如果在回调中执行复杂业务、远程调用或长时间阻塞，可能影响后续事件处理。建议监听器只做轻量判断和日志记录，复杂处理交给线程池、事件总线或消息队列。

分布式锁使用时应控制锁粒度和持锁时间。锁粒度过粗会降低并发能力，锁粒度过细会增加节点数量和维护复杂度。持锁期间只应执行必要的临界区逻辑，不建议在持锁状态下执行长时间外部接口调用。

配置管理只适合轻量级场景。Zookeeper 单节点数据不宜过大，配置内容建议保持简洁。配置值可以使用 JSON 字符串，但不建议存储大 JSON、大文本、文件内容或频繁变化的业务数据。

生产环境需要补充权限控制。本文示例主要面向开发验证，没有展开 ACL 权限控制。生产环境应根据实际安全要求启用认证和 ACL，避免未授权客户端读取、修改或删除关键节点。

接口层需要限制操作范围。节点管理、监听测试、删除节点等接口如果直接暴露到生产环境，存在误删节点和越权操作风险。建议增加登录认证、角色权限、路径白名单、操作审计和环境隔离。

建议增加统一监控。生产环境应监控 Zookeeper 连接状态、Session 状态、请求失败次数、节点数量、监听器数量、分布式锁等待耗时、锁获取失败次数等指标。

开发注意事项汇总如下：

| 注意事项          | 说明                                             |
| ----------------- | ------------------------------------------------ |
| 明确使用边界      | Zookeeper 只用于协调类数据，不用于大规模业务存储 |
| 规范节点路径      | 路径应稳定、简洁、分层清晰                       |
| 避免大数据节点    | 节点数据应保持轻量                               |
| 关注 Session 丢失 | Session 丢失后临时节点和锁状态会失效             |
| 控制监听逻辑      | 监听回调中不要执行耗时任务                       |
| 控制锁粒度        | 分布式锁路径应按业务资源精确设计                 |
| 增加权限控制      | 生产环境应启用认证、ACL 和接口权限               |
| 增加操作审计      | 删除、更新、配置变更等操作应记录审计日志         |
| 做好监控告警      | 关注连接状态、失败次数和锁等待耗时               |
| 区分环境配置      | 本地、测试、生产环境应使用不同命名空间或集群     |

### 后续扩展方向

在完成基础集成后，可以根据实际业务场景继续扩展 Zookeeper 的使用能力。常见扩展方向包括 ACL 权限控制、服务注册自动化、配置热更新、分布式任务协调、Leader 选举、Actuator 监控指标和生产级高可用部署。

可以扩展 ACL 权限控制。当前示例默认使用无认证连接，适合本地开发和功能验证。生产环境可以增加 Digest 认证，并为关键路径设置只读、读写或管理权限，限制不同应用对节点的访问范围。

可以扩展自动服务注册。当前服务注册示例需要手动调用注册方法，后续可以基于 `ApplicationReadyEvent` 在应用启动完成后自动注册服务实例，并在 `RECONNECTED` 状态下自动重新注册临时节点。

可以扩展配置热更新。当前配置管理示例已经提供配置监听能力，后续可以将监听到的 JSON 配置反序列化为 Java 配置对象，并刷新本地缓存，使业务代码可以直接读取最新配置。

可以扩展分布式任务协调。对于多实例部署的定时任务，可以通过分布式锁或 Leader 选举保证同一时间只有一个实例执行任务，避免重复调度、重复导出或重复发送。

可以扩展 Leader 选举。Curator 提供 `LeaderLatch` 和 `LeaderSelector`，适合实现主从节点选举。主节点可以负责调度任务、聚合数据或执行单点业务逻辑，从节点作为备用实例等待接管。

可以扩展服务发现负载均衡。当前服务发现示例只返回实例列表，后续可以增加随机、轮询、权重、版本优先、同机房优先等负载均衡策略。

可以扩展统一监控指标。可以结合 Micrometer 和 Actuator 暴露 Zookeeper 连接状态、节点操作次数、失败次数、锁等待耗时、锁获取失败次数等指标，方便接入 Prometheus 和 Grafana。

可以扩展生产级部署文档。生产环境应补充 Zookeeper 集群部署、数据目录规划、事务日志目录规划、JVM 参数、快照清理、四字命令限制、监控告警、备份恢复等内容。

后续扩展方向汇总如下：

| 扩展方向         | 说明                                       |
| ---------------- | ------------------------------------------ |
| ACL 权限控制     | 增加 Digest 认证和节点级权限管理           |
| 自动服务注册     | 应用启动后自动注册实例，重连后自动补偿注册 |
| 配置热更新       | 监听配置节点变化并刷新本地缓存             |
| Leader 选举      | 使用 Curator 实现主从选举                  |
| 分布式任务协调   | 通过锁或选举避免多实例重复执行任务         |
| 服务发现负载均衡 | 在实例列表基础上实现选择策略               |
| Actuator 扩展    | 暴露 Zookeeper 连接状态和自定义健康检查    |
| Micrometer 指标  | 统计节点操作、锁等待、异常次数等指标       |
| 生产集群部署     | 补充三节点或五节点集群部署方案             |
| 运维与告警       | 增加日志、指标、告警和故障恢复流程         |

至此，Spring Boot 3 集成 Zookeeper 的基础开发文档已经形成完整闭环。文档覆盖了从环境准备、编码实现到接口验证和后续扩展的主要流程，可作为项目开发、团队培训和后续生产化改造的基础文档。
