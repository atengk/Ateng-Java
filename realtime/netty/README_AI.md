# Netty

Netty 是一个基于 Java 的异步事件驱动网络应用框架，适合开发高性能、高并发、可维护的 TCP、UDP、HTTP、WebSocket 等网络通信服务。Spring Boot 3 项目中集成 Netty，通常用于构建自定义协议服务、长连接网关、物联网接入服务、消息推送服务或高性能内部通信服务。Netty 官方将其定位为用于快速开发高性能协议服务端与客户端的异步事件驱动网络框架。

## 项目概述

本项目基于 Spring Boot 3 与 Netty 实现一个可配置、可扩展、可维护的网络通信服务。Spring Boot 负责应用启动、配置管理、Bean 生命周期管理、日志监控与业务组件装配，Netty 负责底层网络连接、事件分发、编解码、粘包拆包处理和高并发 I/O 通信。

项目整体设计目标是将 Netty 的网络通信能力与 Spring Boot 的工程化能力结合起来，使网络服务既具备较高性能，又能够按照标准 Spring Boot 项目方式进行配置、启动、测试、部署和运维。

### 技术选型

本项目采用 Spring Boot 3 作为基础应用框架，使用 Netty 作为网络通信框架，使用 Maven 进行依赖管理与项目构建。Spring Boot 3 官方要求 Java 17 作为最低运行基线，因此项目建议使用 JDK 17 或 JDK 21 LTS。

核心技术选型如下：

| 技术        | 推荐版本                  | 作用                                                 |
| ----------- | ------------------------- | ---------------------------------------------------- |
| JDK         | 17 或 21 LTS              | Spring Boot 3 运行基础，提供现代 Java 语法与长期支持 |
| Spring Boot | 3.x                       | 项目启动、配置管理、Bean 管理、日志、监控、测试      |
| Netty       | 4.1.x 或 4.2.x            | 异步网络通信、连接管理、编解码、事件处理             |
| Maven       | 3.6.3+                    | 项目构建、依赖管理、插件管理                         |
| Lombok      | 当前稳定版                | 简化实体类、配置类、日志对象编写                     |
| Hutool      | 当前稳定版                | 字符串、集合、编码、校验、类型转换等通用工具         |
| Logback     | Spring Boot 默认集成      | 应用日志、连接日志、异常日志输出                     |
| JUnit 5     | Spring Boot Test 默认集成 | 单元测试与集成测试                                   |

Netty 依赖建议通过 `netty-bom` 统一管理版本，避免多个 Netty 模块版本不一致。Maven Central 中 Netty 仍保持 4.1.x 与 4.2.x 版本线持续发布，生产项目应固定明确版本，并在升级前完成协议兼容性、压测和回归验证。

### 应用场景

Spring Boot 3 集成 Netty 主要适用于对连接数量、通信延迟、协议灵活性或 I/O 吞吐量有较高要求的场景。相比普通 HTTP 接口服务，Netty 更适合处理长连接、自定义二进制协议、设备接入、消息推送等网络通信问题。

常见应用场景包括：

| 场景           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| TCP 长连接服务 | 维护客户端与服务端之间的长连接，适合网关、推送、设备通信     |
| 自定义协议服务 | 基于二进制或文本协议设计私有通信协议，支持协议头、消息体、校验位等 |
| 物联网设备接入 | 接入大量设备终端，处理设备注册、心跳、上报数据、指令下发     |
| 即时通信服务   | 处理用户连接、消息转发、在线状态、消息确认等实时通信逻辑     |
| 内部高性能通信 | 用于服务间高吞吐、低延迟的数据传输                           |
| 网关代理服务   | 接收客户端连接后转发到后端服务，支持协议转换、鉴权、限流     |
| WebSocket 服务 | 处理浏览器或客户端的双向通信场景                             |
| UDP 通信服务   | 处理无连接、轻量级、低延迟的数据包通信场景                   |

### 功能目标

本项目的功能目标是构建一个具备基础生产能力的 Netty 通信服务，而不是只完成一个简单的 Echo 示例。项目应覆盖服务启动、连接管理、协议编解码、业务分发、异常处理、日志监控和优雅停机等核心能力。

主要功能目标如下：

| 功能            | 目标说明                                                   |
| --------------- | ---------------------------------------------------------- |
| Netty 服务启动  | 应用启动后自动启动 Netty 服务端，监听指定端口              |
| 配置化管理      | 端口、线程数、心跳时间、消息长度等参数通过配置文件管理     |
| Handler Bean 化 | Netty Handler 支持由 Spring 容器管理，便于注入业务 Service |
| 连接管理        | 维护客户端连接信息，支持连接注册、移除、查询和消息发送     |
| 协议处理        | 支持自定义消息协议、编码、解码、粘包拆包处理               |
| 心跳机制        | 支持客户端心跳检测，及时清理失效连接                       |
| 业务分发        | 根据消息类型分发到不同业务处理器                           |
| 异常处理        | 统一处理编解码异常、连接异常、业务异常                     |
| 日志记录        | 记录服务启动、客户端连接、消息收发、异常断开等关键日志     |
| 优雅停机        | 应用关闭时释放端口、关闭连接、停止 EventLoopGroup          |
| 测试验证        | 提供客户端通信测试、单元测试和压测验证方式                 |

## 环境准备

环境准备用于统一项目的 JDK、Spring Boot、Maven、Netty 依赖和目录结构。该部分是后续服务端开发、客户端开发、协议设计、业务处理和部署运行的基础，建议在项目初始化时优先完成。

### JDK 与 Spring Boot 版本

Spring Boot 3 项目最低要求 Java 17，因此本项目建议使用 JDK 17 作为基础版本；如果项目周期较长或希望获得更长的运行时支持，也可以使用 JDK 21 LTS。Spring Boot 3.0.x 官方要求 Java 17，后续 3.3.x 版本仍要求至少 Java 17，并对 Maven、Gradle 等构建工具版本有明确要求。

推荐环境如下：

| 环境        | 推荐配置          | 说明                                              |
| ----------- | ----------------- | ------------------------------------------------- |
| JDK         | 17 或 21 LTS      | Spring Boot 3 最低要求 Java 17                    |
| Spring Boot | 3.x               | 示例基于 Spring Boot 3 工程结构                   |
| Maven       | 3.6.3+，推荐 3.9+ | Spring Boot 3.3.x 明确支持 Maven 3.6.3 或更高版本 |
| Netty       | 4.1.x 或 4.2.x    | 生产环境固定版本，避免动态升级                    |
| 编码        | UTF-8             | 统一源码、配置文件和日志编码                      |
| 构建方式    | jar               | 使用 Spring Boot 可执行 Jar 部署                  |

建议先检查本地环境版本：

```bash
# 查看 JDK 版本
java -version

# 查看 Maven 版本
mvn -version
```

项目开发建议统一配置 `maven-compiler-plugin` 或 Spring Boot Maven 插件中的 Java 版本，避免本地、测试环境、生产环境使用不同 JDK 导致编译或运行行为不一致。

### Maven 依赖配置

Maven 依赖用于引入 Spring Boot、Netty、Lombok、Hutool、测试组件等基础能力。Netty 依赖建议使用 `netty-bom` 做版本统一管理，然后按需引入 `netty-transport`、`netty-codec`、`netty-handler` 等模块。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-netty-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot3-netty-demo</name>
    <description>Spring Boot 3 集成 Netty 示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17 -->
        <java.version>17</java.version>

        <!-- Netty 版本统一管理，生产环境升级前需要完成回归测试 -->
        <netty.version>4.1.132.Final</netty.version>

        <!-- Hutool 工具类库版本 -->
        <hutool.version>5.8.38</hutool.version>

        <!-- Lombok 版本 -->
        <lombok.version>1.18.38</lombok.version>

        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot 依赖版本管理 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>3.5.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Netty 依赖版本统一管理，避免不同 Netty 模块版本冲突 -->
            <dependency>
                <groupId>io.netty</groupId>
                <artifactId>netty-bom</artifactId>
                <version>${netty.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Boot 基础启动依赖，提供配置、日志、生命周期管理等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Spring Boot 配置属性提示，便于 application.yml 自动补全 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Netty 核心传输模块，用于 ServerBootstrap、Bootstrap、Channel、EventLoopGroup 等 -->
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-transport</artifactId>
        </dependency>

        <!-- Netty 编解码模块，用于处理粘包拆包、长度字段、字符串、二进制协议等 -->
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-codec</artifactId>
        </dependency>

        <!-- Netty Handler 模块，用于 IdleStateHandler、SSL、日志处理器等扩展能力 -->
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-handler</artifactId>
        </dependency>

        <!-- Netty Buffer 模块，用于 ByteBuf 数据读写 -->
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-buffer</artifactId>
        </dependency>

        <!-- Hutool 工具类库，用于字符串、集合、编码、校验等通用处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化实体类、配置类和日志对象代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖，包含 JUnit 5、AssertJ、Mockito 等 -->
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

            <!-- Java 编译插件，统一源码和目标字节码版本 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

如果项目部署在 Linux 环境，并且希望使用 Epoll 提升网络 I/O 性能，可以按运行环境额外引入 Netty Native Transport。该依赖不是所有环境都必须配置，建议在 Linux 生产环境经过压测后再启用。

```xml
<!-- Linux 环境可选：Netty Epoll 原生传输，适用于高并发网络通信场景 -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-transport-native-epoll</artifactId>
    <classifier>linux-x86_64</classifier>
</dependency>
```

基础依赖验证命令如下：

```bash
# 查看依赖树，确认 Netty 模块版本是否一致
mvn dependency:tree -Dincludes=io.netty

# 编译项目，确认 JDK、Maven、依赖版本是否正常
mvn clean compile
```

如果依赖树中出现多个不同版本的 `io.netty` 模块，需要优先检查是否有第三方组件间接引入了旧版本 Netty，并通过 `dependencyManagement` 统一版本。

### 项目目录结构

项目目录结构按照 Spring Boot 标准工程组织，同时将 Netty 相关代码独立放在 `netty` 包下，避免与普通 Web、Service、配置类混杂。建议将启动、配置、协议、编解码、Handler、连接管理、客户端测试等模块分层管理。

推荐目录结构如下：

```text
springboot3-netty-demo
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               └── netty
│   │   │                   ├── NettyApplication.java
│   │   │                   ├── config
│   │   │                   │   ├── NettyProperties.java
│   │   │                   │   └── NettyServerConfig.java
│   │   │                   ├── server
│   │   │                   │   ├── NettyServer.java
│   │   │                   │   └── NettyServerLifecycle.java
│   │   │                   ├── client
│   │   │                   │   ├── NettyClient.java
│   │   │                   │   └── NettyClientTestRunner.java
│   │   │                   ├── initializer
│   │   │                   │   ├── NettyServerChannelInitializer.java
│   │   │                   │   └── NettyClientChannelInitializer.java
│   │   │                   ├── handler
│   │   │                   │   ├── NettyServerHandler.java
│   │   │                   │   ├── NettyClientHandler.java
│   │   │                   │   ├── HeartbeatHandler.java
│   │   │                   │   └── NettyExceptionHandler.java
│   │   │                   ├── codec
│   │   │                   │   ├── MessageDecoder.java
│   │   │                   │   └── MessageEncoder.java
│   │   │                   ├── protocol
│   │   │                   │   ├── MessagePacket.java
│   │   │                   │   ├── MessageType.java
│   │   │                   │   └── ProtocolConstant.java
│   │   │                   ├── dispatcher
│   │   │                   │   ├── MessageDispatcher.java
│   │   │                   │   └── MessageProcessor.java
│   │   │                   ├── session
│   │   │                   │   ├── ChannelSession.java
│   │   │                   │   └── ChannelSessionManager.java
│   │   │                   └── service
│   │   │                       ├── DeviceMessageService.java
│   │   │                       └── HeartbeatService.java
│   │   └── resources
│   │       ├── application.yml
│   │       └── logback-spring.xml
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── netty
│                           ├── NettyServerTests.java
│                           └── NettyClientTests.java
└── docs
    ├── protocol.md
    ├── deploy.md
    └── pressure-test.md
```

主要目录说明如下：

| 目录          | 说明                                                   |
| ------------- | ------------------------------------------------------ |
| `config`      | Netty 配置属性、线程池参数、端口、心跳、消息长度等配置 |
| `server`      | Netty 服务端启动、关闭、生命周期控制                   |
| `client`      | Netty 客户端连接、发送消息、测试验证                   |
| `initializer` | Channel 初始化器，负责装配 Pipeline                    |
| `handler`     | 入站、出站、心跳、异常等 Handler                       |
| `codec`       | 自定义协议编码器和解码器                               |
| `protocol`    | 消息包、消息类型、协议常量                             |
| `dispatcher`  | 消息分发器，根据消息类型路由到业务处理器               |
| `session`     | 连接会话管理，维护 Channel 与客户端标识关系            |
| `service`     | 业务处理服务，可被 Handler 或 Dispatcher 调用          |
| `resources`   | Spring Boot 配置文件和日志配置                         |
| `test`        | 服务端、客户端、编解码、协议处理相关测试               |
| `docs`        | 协议说明、部署说明、压测说明等开发文档                 |

`application.yml` 中建议预留 Netty 基础配置，后续章节可以围绕这些配置展开服务启动、线程模型、心跳检测和协议限制。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot3-netty-demo

netty:
  server:
    # Netty 服务监听端口，避免与 Spring Boot Web 端口冲突
    port: 9000

    # Boss 线程数，主要负责接收客户端连接
    boss-threads: 1

    # Worker 线程数，0 表示根据 CPU 核心数自动计算
    worker-threads: 0

    # 是否开启 TCP keepalive
    keep-alive: true

    # 是否开启 TCP_NODELAY，降低小包延迟
    tcp-no-delay: true

    # 服务端连接队列大小
    backlog: 1024

    # 最大帧长度，防止异常大包占用内存
    max-frame-length: 1048576

    # 读空闲时间，超过该时间未收到客户端数据则触发心跳检测
    reader-idle-seconds: 60

    # 写空闲时间，超过该时间未向客户端写数据可触发写空闲事件
    writer-idle-seconds: 30

    # 读写空闲时间，适合统一心跳检测
    all-idle-seconds: 90
```

完成目录结构和基础配置后，后续章节可以按以下顺序继续开发：先实现 `NettyProperties` 读取配置，再实现 `NettyServer` 启动服务，随后通过 `ChannelInitializer` 装配编解码器、心跳 Handler、业务 Handler 和异常 Handler，最后补充客户端测试、协议文档和压测脚本。



## Netty 基础设计

Netty 基础设计部分用于说明服务端网络通信的核心结构，包括线程模型、连接抽象、Pipeline 编排、ByteBuf 数据处理和 Handler 处理链。理解这些内容后，后续 Spring Boot 集成、服务端开发、协议设计和业务分发会更容易落地。

### Reactor 线程模型

Netty 基于 Reactor 事件驱动模型处理网络 I/O。服务端通常使用主从 Reactor 模型，也就是一个 `bossGroup` 负责接收客户端连接，一个 `workerGroup` 负责处理连接上的读写事件、编解码和业务 Handler 调度。

在实际项目中，线程模型建议按照以下方式设计：

| 组件                 | 作用                                 | 建议配置                                        |
| -------------------- | ------------------------------------ | ----------------------------------------------- |
| `bossGroup`          | 接收客户端连接，完成 Accept 事件处理 | 通常设置为 `1`                                  |
| `workerGroup`        | 处理客户端 Channel 的读写事件        | 可设置为 CPU 核心数的 2 倍，或由 Netty 默认计算 |
| `ServerBootstrap`    | 服务端启动引导类                     | 绑定端口、配置线程组、设置 Channel 类型         |
| `ChannelInitializer` | 初始化每个客户端连接的 Pipeline      | 添加编解码器、心跳 Handler、业务 Handler        |
| `ChannelHandler`     | 处理具体 I/O 事件                    | 按职责拆分，避免单个 Handler 过重               |

典型处理流程如下：

```text
客户端连接
   │
   ▼
bossGroup 接收连接
   │
   ▼
注册到 workerGroup
   │
   ▼
ChannelInitializer 初始化 Pipeline
   │
   ▼
Decoder 解码请求数据
   │
   ▼
业务 Handler 处理消息
   │
   ▼
Encoder 编码响应数据
   │
   ▼
写回客户端
```

线程模型设计时需要注意，Netty 的 I/O 线程不适合执行耗时业务。如果业务逻辑包含数据库访问、远程接口调用、文件处理、大量计算等操作，应将任务提交到业务线程池中执行，避免阻塞 `EventLoop`，导致同一个线程管理的多个 Channel 都受到影响。

推荐配置示例：

```yaml
netty:
  server:
    # Netty 服务监听端口
    port: 9000

    # Boss 线程数，主要处理客户端连接接入
    boss-threads: 1

    # Worker 线程数，0 表示使用 Netty 默认策略
    worker-threads: 0

    # 业务线程池核心线程数，处理耗时业务
    business-core-threads: 8

    # 业务线程池最大线程数
    business-max-threads: 32

    # 业务队列容量
    business-queue-capacity: 1000
```

### Channel 与 Pipeline

`Channel` 是 Netty 对网络连接的抽象，每一个客户端连接都会对应一个 Channel。服务端可以通过 Channel 获取连接状态、远程地址、写出数据、关闭连接，也可以通过 Channel 的属性绑定客户端标识、设备编号、用户 ID 等业务信息。

`Pipeline` 是绑定在 Channel 上的 Handler 处理链。每个 Channel 都有自己的 Pipeline，数据读取、编码、解码、心跳检测、业务处理、异常处理都会按照 Pipeline 中 Handler 的顺序执行。

常见 Pipeline 设计如下：

```text
ChannelPipeline
├── IdleStateHandler              心跳空闲检测
├── LengthFieldBasedFrameDecoder  粘包拆包处理
├── MessageDecoder                二进制数据解码为消息对象
├── MessageEncoder                消息对象编码为二进制数据
├── NettyServerHandler            业务消息处理
└── NettyExceptionHandler         异常统一处理
```

文件位置：`src/main/java/io/github/atengk/netty/initializer/NettyServerChannelInitializer.java`

该初始化器负责为每个新连接创建 Pipeline，并按顺序添加心跳、拆包、编解码、业务和异常处理器。

```java
package io.github.atengk.netty.initializer;

import io.github.atengk.netty.codec.MessageDecoder;
import io.github.atengk.netty.codec.MessageEncoder;
import io.github.atengk.netty.config.NettyProperties;
import io.github.atengk.netty.handler.NettyExceptionHandler;
import io.github.atengk.netty.handler.NettyServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Netty 服务端 Channel 初始化器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final NettyProperties nettyProperties;
    private final ObjectProvider<NettyServerHandler> nettyServerHandlerProvider;
    private final ObjectProvider<NettyExceptionHandler> nettyExceptionHandlerProvider;

    /**
     * 初始化客户端 Channel 的 Pipeline
     *
     * @param channel 客户端连接
     */
    @Override
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast("idleStateHandler", new IdleStateHandler(
                nettyProperties.getServer().getReaderIdleSeconds(),
                nettyProperties.getServer().getWriterIdleSeconds(),
                nettyProperties.getServer().getAllIdleSeconds(),
                TimeUnit.SECONDS
        ));

        pipeline.addLast("messageDecoder", new MessageDecoder(nettyProperties.getServer().getMaxFrameLength()));
        pipeline.addLast("messageEncoder", new MessageEncoder());

        // 每个 Channel 使用独立的业务 Handler，避免共享状态导致并发问题
        pipeline.addLast("nettyServerHandler", nettyServerHandlerProvider.getObject());
        pipeline.addLast("nettyExceptionHandler", nettyExceptionHandlerProvider.getObject());

        log.info("初始化客户端连接Pipeline，remoteAddress={}", channel.remoteAddress());
    }
}
```

这里使用 `ObjectProvider` 获取 Handler，主要是为了支持原型作用域 Handler。对于有状态的 Handler，不应直接作为单例复用到多个 Channel；对于无状态且明确标注 `@ChannelHandler.Sharable` 的 Handler，才适合单例复用。

### ByteBuf 数据处理

`ByteBuf` 是 Netty 提供的高性能字节容器，用于替代传统 Java NIO 中的 `ByteBuffer`。它通过读指针 `readerIndex` 和写指针 `writerIndex` 管理数据读写，不需要频繁执行 `flip()` 操作，更适合网络通信场景。

ByteBuf 常用操作如下：

| 操作                 | 说明                        |
| -------------------- | --------------------------- |
| `readableBytes()`    | 获取可读字节数              |
| `readByte()`         | 读取 1 个字节，读指针后移   |
| `readInt()`          | 读取 4 字节整数，读指针后移 |
| `readBytes(byte[])`  | 读取指定长度数据到字节数组  |
| `writeByte()`        | 写入 1 个字节，写指针后移   |
| `writeInt()`         | 写入 4 字节整数，写指针后移 |
| `writeBytes(byte[])` | 写入字节数组                |
| `markReaderIndex()`  | 标记当前读指针              |
| `resetReaderIndex()` | 重置到标记的读指针位置      |

在自定义协议中，ByteBuf 通常用于读取协议头、消息类型、消息长度和消息体。例如一个简单协议可以设计为：

```text
魔数 4 字节 + 版本 1 字节 + 消息类型 1 字节 + 消息长度 4 字节 + 消息体 N 字节
```

协议常量示例：

文件位置：`src/main/java/io/github/atengk/netty/protocol/ProtocolConstant.java`

该常量类定义自定义协议的基础字段，供编码器和解码器统一使用。

```java
package io.github.atengk.netty.protocol;

/**
 * Netty 自定义协议常量
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class ProtocolConstant {

    /**
     * 魔数，用于快速识别非法数据包
     */
    public static final int MAGIC_NUMBER = 0xA7B9C3D5;

    /**
     * 当前协议版本
     */
    public static final byte VERSION = 1;

    /**
     * 协议头长度：魔数 4 字节 + 版本 1 字节 + 消息类型 1 字节 + 消息长度 4 字节
     */
    public static final int HEADER_LENGTH = 10;

    private ProtocolConstant() {
    }
}
```

解码器示例：

文件位置：`src/main/java/io/github/atengk/netty/codec/MessageDecoder.java`

该解码器从 ByteBuf 中读取自定义协议数据，并将二进制数据转换为消息对象。

```java
package io.github.atengk.netty.codec;

import cn.hutool.core.util.ArrayUtil;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Netty 消息解码器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RequiredArgsConstructor
public class MessageDecoder extends ByteToMessageDecoder {

    private final int maxFrameLength;

    /**
     * 将 ByteBuf 解码为消息对象
     *
     * @param ctx 上下文
     * @param in 输入缓冲区
     * @param out 解码结果
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < ProtocolConstant.HEADER_LENGTH) {
            return;
        }

        in.markReaderIndex();

        int magicNumber = in.readInt();
        if (magicNumber != ProtocolConstant.MAGIC_NUMBER) {
            log.warn("非法数据包，关闭连接，remoteAddress={}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        byte version = in.readByte();
        byte messageType = in.readByte();
        int bodyLength = in.readInt();

        if (bodyLength < 0 || bodyLength > maxFrameLength) {
            log.warn("消息体长度非法，bodyLength={}，maxFrameLength={}", bodyLength, maxFrameLength);
            ctx.close();
            return;
        }

        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] body = new byte[bodyLength];
        in.readBytes(body);

        MessagePacket packet = new MessagePacket();
        packet.setVersion(version);
        packet.setMessageType(messageType);
        packet.setBody(ArrayUtil.isEmpty(body) ? new byte[0] : body);

        out.add(packet);
    }
}
```

编码器示例：

文件位置：`src/main/java/io/github/atengk/netty/codec/MessageEncoder.java`

该编码器将消息对象写入 ByteBuf，按照协议格式输出给客户端。

```java
package io.github.atengk.netty.codec;

import cn.hutool.core.util.ArrayUtil;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Netty 消息编码器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class MessageEncoder extends MessageToByteEncoder<MessagePacket> {

    /**
     * 将消息对象编码为 ByteBuf
     *
     * @param ctx 上下文
     * @param msg 消息对象
     * @param out 输出缓冲区
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, MessagePacket msg, ByteBuf out) {
        byte[] body = ArrayUtil.isEmpty(msg.getBody()) ? new byte[0] : msg.getBody();

        out.writeInt(ProtocolConstant.MAGIC_NUMBER);
        out.writeByte(msg.getVersion());
        out.writeByte(msg.getMessageType());
        out.writeInt(body.length);
        out.writeBytes(body);
    }
}
```

消息对象示例：

文件位置：`src/main/java/io/github/atengk/netty/protocol/MessagePacket.java`

该消息对象用于承载解码后的业务数据，也作为编码器输出响应数据的统一模型。

```java
package io.github.atengk.netty.protocol;

import lombok.Data;

/**
 * Netty 消息数据包
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class MessagePacket {

    /**
     * 协议版本
     */
    private byte version;

    /**
     * 消息类型
     */
    private byte messageType;

    /**
     * 消息体
     */
    private byte[] body;
}
```

ByteBuf 使用时需要注意以下几点：

| 注意事项         | 说明                                        |
| ---------------- | ------------------------------------------- |
| 不要越界读取     | 读取前先判断 `readableBytes()`              |
| 半包要重置读指针 | 数据不足时使用 `resetReaderIndex()` 回退    |
| 控制最大包长度   | 防止异常客户端发送超大数据包占用内存        |
| 避免手动释放错误 | 在解码器中不要随意 `release()` 入站 ByteBuf |
| 编码解码保持一致 | 协议字段顺序、长度、类型必须完全一致        |

### Handler 处理链

Handler 是 Netty 中处理网络事件的核心组件。每个 Handler 只负责一个明确职责，例如心跳检测、协议解码、业务处理、响应编码、异常处理等。合理拆分 Handler 可以提升代码可维护性，也便于单独测试和替换。

推荐 Handler 顺序如下：

| 顺序 | Handler                 | 类型 | 作用                      |
| ---- | ----------------------- | ---- | ------------------------- |
| 1    | `IdleStateHandler`      | 入站 | 检测连接读写空闲          |
| 2    | `MessageDecoder`        | 入站 | 将 ByteBuf 解码为消息对象 |
| 3    | `MessageEncoder`        | 出站 | 将消息对象编码为 ByteBuf  |
| 4    | `NettyServerHandler`    | 入站 | 处理业务消息              |
| 5    | `NettyExceptionHandler` | 入站 | 统一处理异常和连接关闭    |

业务 Handler 示例：

文件位置：`src/main/java/io/github/atengk/netty/handler/NettyServerHandler.java`

该 Handler 负责接收客户端消息、处理心跳事件，并将业务消息交给分发器处理。

```java
package io.github.atengk.netty.handler;

import cn.hutool.core.util.ArrayUtil;
import io.github.atengk.netty.dispatcher.MessageDispatcher;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Netty 服务端业务处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class NettyServerHandler extends SimpleChannelInboundHandler<MessagePacket> {

    private final MessageDispatcher messageDispatcher;

    /**
     * 客户端连接建立
     *
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("客户端连接成功，remoteAddress={}", ctx.channel().remoteAddress());
    }

    /**
     * 接收客户端消息
     *
     * @param ctx 上下文
     * @param packet 消息包
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessagePacket packet) {
        if (packet == null || ArrayUtil.isEmpty(packet.getBody())) {
            log.warn("收到空消息，remoteAddress={}", ctx.channel().remoteAddress());
            return;
        }

        if (packet.getMessageType() == MessageType.HEARTBEAT) {
            MessagePacket response = new MessagePacket();
            response.setVersion(ProtocolConstant.VERSION);
            response.setMessageType(MessageType.HEARTBEAT_ACK);
            response.setBody("pong".getBytes());
            ctx.writeAndFlush(response);
            log.debug("处理客户端心跳，remoteAddress={}", ctx.channel().remoteAddress());
            return;
        }

        messageDispatcher.dispatch(ctx, packet);
    }

    /**
     * 处理用户事件
     *
     * @param ctx 上下文
     * @param evt 事件对象
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.warn("客户端连接空闲超时，关闭连接，remoteAddress={}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * 客户端连接断开
     *
     * @param ctx 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("客户端连接断开，remoteAddress={}", ctx.channel().remoteAddress());
    }
}
```

消息类型示例：

文件位置：`src/main/java/io/github/atengk/netty/protocol/MessageType.java`

该类定义协议中的消息类型，便于 Handler 和业务分发器根据类型处理不同业务。

```java
package io.github.atengk.netty.protocol;

/**
 * Netty 消息类型
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class MessageType {

    /**
     * 心跳请求
     */
    public static final byte HEARTBEAT = 1;

    /**
     * 心跳响应
     */
    public static final byte HEARTBEAT_ACK = 2;

    /**
     * 业务请求
     */
    public static final byte BUSINESS_REQUEST = 10;

    /**
     * 业务响应
     */
    public static final byte BUSINESS_RESPONSE = 11;

    private MessageType() {
    }
}
```

异常 Handler 示例：

文件位置：`src/main/java/io/github/atengk/netty/handler/NettyExceptionHandler.java`

该 Handler 负责统一记录 Pipeline 中未处理的异常，并关闭异常连接。

```java
package io.github.atengk.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Netty 异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
public class NettyExceptionHandler extends ChannelInboundHandlerAdapter {

    /**
     * 处理异常事件
     *
     * @param ctx 上下文
     * @param cause 异常信息
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Netty连接发生异常，remoteAddress={}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
```

Handler 设计时建议遵循以下规则：

| 规则                          | 说明                                                      |
| ----------------------------- | --------------------------------------------------------- |
| Handler 职责单一              | 不要把编解码、业务、异常、连接管理全部写在一个 Handler 中 |
| 有状态 Handler 使用原型作用域 | 每个 Channel 创建独立实例，避免并发数据污染               |
| 无状态 Handler 可共享         | 必须确认线程安全，并标记 `@ChannelHandler.Sharable`       |
| 耗时业务不要阻塞 I/O 线程     | 使用业务线程池处理数据库、远程调用等操作                  |
| 异常要统一收口                | 避免异常在 Pipeline 中无人处理导致连接异常残留            |

## Spring Boot 集成 Netty

Spring Boot 集成 Netty 的核心目标是让 Netty 服务跟随 Spring Boot 应用一起启动、一起关闭，并让 Netty 中的 Handler、配置类、业务 Service 都能被 Spring 容器统一管理。这样可以避免手动创建对象造成依赖注入失效，也便于后续扩展监控、配置、测试和运维能力。

### Netty 服务启动方式

Netty 服务可以通过多种方式在 Spring Boot 中启动，常见方式包括 `ApplicationRunner`、`CommandLineRunner`、`ApplicationListener<ApplicationReadyEvent>` 和 `SmartLifecycle`。生产项目更推荐使用 `SmartLifecycle`，因为它同时支持启动、停止、运行状态判断和生命周期阶段控制。

推荐方式如下：

| 启动方式                | 特点                         | 适用场景                 |
| ----------------------- | ---------------------------- | ------------------------ |
| `ApplicationRunner`     | Spring Boot 启动后执行一次   | 简单 Demo 或临时测试     |
| `CommandLineRunner`     | 与 `ApplicationRunner` 类似  | 简单启动逻辑             |
| `ApplicationReadyEvent` | 应用准备完成后启动           | 依赖应用完全初始化的场景 |
| `SmartLifecycle`        | 支持自动启动、停止、状态判断 | 推荐用于生产项目         |

文件位置：`src/main/java/io/github/atengk/netty/server/NettyServer.java`

该服务类负责创建 ServerBootstrap、绑定端口、启动 Netty 服务和释放线程资源。

```java
package io.github.atengk.netty.server;

import io.github.atengk.netty.config.NettyProperties;
import io.github.atengk.netty.initializer.NettyServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Netty 服务端
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyServer {

    private final NettyProperties nettyProperties;
    private final NettyServerChannelInitializer channelInitializer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * 启动 Netty 服务
     */
    public synchronized void start() {
        if (serverChannel != null && serverChannel.isActive()) {
            log.warn("Netty服务已启动，无需重复启动");
            return;
        }

        NettyProperties.Server server = nettyProperties.getServer();
        bossGroup = new MultiThreadIoEventLoopGroup(server.getBossThreads(), NioIoHandler.newFactory());
        workerGroup = server.getWorkerThreads() > 0
                ? new MultiThreadIoEventLoopGroup(server.getWorkerThreads(), NioIoHandler.newFactory())
                : new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(channelInitializer)
                    .option(ChannelOption.SO_BACKLOG, server.getBacklog())
                    .childOption(ChannelOption.SO_KEEPALIVE, server.isKeepAlive())
                    .childOption(ChannelOption.TCP_NODELAY, server.isTcpNoDelay());

            serverChannel = bootstrap.bind(server.getPort()).sync().channel();
            log.info("Netty服务启动成功，port={}", server.getPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Netty服务启动被中断，port={}", server.getPort(), e);
            stop();
        } catch (Exception e) {
            log.error("Netty服务启动失败，port={}", server.getPort(), e);
            stop();
            throw e;
        }
    }

    /**
     * 停止 Netty 服务
     */
    public synchronized void stop() {
        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
            log.info("Netty服务端口已关闭");
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
            log.info("Netty workerGroup 已释放");
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
            log.info("Netty bossGroup 已释放");
        }
    }

    /**
     * 判断 Netty 服务是否运行
     *
     * @return true 表示运行中
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
}
```

上面示例使用 Netty 4.2 风格的 `MultiThreadIoEventLoopGroup` 和 `NioIoHandler`。如果项目使用 Netty 4.1.x，可以改为以下传统写法：

```java
bossGroup = new NioEventLoopGroup(server.getBossThreads());
workerGroup = server.getWorkerThreads() > 0
        ? new NioEventLoopGroup(server.getWorkerThreads())
        : new NioEventLoopGroup();
```

### Spring Bean 管理 Handler

Netty Handler 中经常需要调用 Spring Service，例如设备认证、消息入库、业务分发、连接会话管理等。如果 Handler 通过 `new` 创建，就无法正常注入 Spring Bean。因此，业务 Handler 建议交给 Spring 容器管理。

Handler 管理方式建议如下：

| Handler 类型           | 是否可单例     | 建议                                     |
| ---------------------- | -------------- | ---------------------------------------- |
| `ByteToMessageDecoder` | 不建议         | 每个 Channel 创建新实例                  |
| `MessageToByteEncoder` | 视情况         | 无状态时可复用，但建议每个 Pipeline 新建 |
| 业务 Handler           | 不建议默认单例 | 有状态时使用 `@Scope("prototype")`       |
| 异常 Handler           | 可单例或原型   | 简单项目可原型，避免共享风险             |
| 连接管理器             | 推荐单例       | 使用线程安全集合维护连接                 |
| 消息分发器             | 推荐单例       | 内部不保存 Channel 私有状态              |

业务分发器示例：

文件位置：`src/main/java/io/github/atengk/netty/dispatcher/MessageDispatcher.java`

该分发器由 Spring 管理，用于根据消息类型调用不同业务处理逻辑。

```java
package io.github.atengk.netty.dispatcher;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Netty 消息分发器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class MessageDispatcher {

    /**
     * 分发消息
     *
     * @param ctx 上下文
     * @param packet 消息包
     */
    public void dispatch(ChannelHandlerContext ctx, MessagePacket packet) {
        if (packet.getMessageType() != MessageType.BUSINESS_REQUEST) {
            log.warn("不支持的消息类型，messageType={}，remoteAddress={}",
                    packet.getMessageType(), ctx.channel().remoteAddress());
            return;
        }

        String requestBody = new String(packet.getBody(), StandardCharsets.UTF_8);
        if (StrUtil.isBlank(requestBody)) {
            log.warn("业务消息内容为空，remoteAddress={}", ctx.channel().remoteAddress());
            return;
        }

        log.info("收到业务消息，remoteAddress={}，body={}", ctx.channel().remoteAddress(), requestBody);

        MessagePacket response = new MessagePacket();
        response.setVersion(ProtocolConstant.VERSION);
        response.setMessageType(MessageType.BUSINESS_RESPONSE);
        response.setBody(("服务端已处理：" + requestBody).getBytes(StandardCharsets.UTF_8));

        ctx.writeAndFlush(response);
    }
}
```

连接会话管理器示例：

文件位置：`src/main/java/io/github/atengk/netty/session/ChannelSessionManager.java`

该管理器作为 Spring 单例 Bean，用于维护客户端连接，后续可扩展为按设备编号、用户 ID 或租户 ID 管理 Channel。

```java
package io.github.atengk.netty.session;

import cn.hutool.core.util.StrUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty 连接会话管理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class ChannelSessionManager {

    private final Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    /**
     * 添加连接
     *
     * @param clientId 客户端标识
     * @param channel 客户端连接
     */
    public void add(String clientId, Channel channel) {
        if (StrUtil.isBlank(clientId) || channel == null) {
            log.warn("添加连接失败，客户端标识或Channel为空");
            return;
        }

        channelMap.put(clientId, channel);
        log.info("添加客户端连接，clientId={}，当前连接数={}", clientId, channelMap.size());
    }

    /**
     * 移除连接
     *
     * @param clientId 客户端标识
     */
    public void remove(String clientId) {
        if (StrUtil.isBlank(clientId)) {
            return;
        }

        channelMap.remove(clientId);
        log.info("移除客户端连接，clientId={}，当前连接数={}", clientId, channelMap.size());
    }

    /**
     * 发送消息
     *
     * @param clientId 客户端标识
     * @param message 消息对象
     * @return true 表示发送成功
     */
    public boolean send(String clientId, Object message) {
        Channel channel = channelMap.get(clientId);
        if (channel == null || !channel.isActive()) {
            log.warn("客户端连接不存在或不可用，clientId={}", clientId);
            return false;
        }

        channel.writeAndFlush(message);
        log.info("向客户端发送消息，clientId={}", clientId);
        return true;
    }

    /**
     * 获取当前连接数量
     *
     * @return 连接数量
     */
    public int count() {
        return channelMap.size();
    }
}
```

这种设计可以让 Handler 只负责接收 Netty 事件，真正的业务逻辑放在 Spring Bean 中处理，便于单元测试和后续扩展。

### 服务生命周期控制

Netty 服务需要跟随 Spring Boot 应用生命周期启动和关闭。推荐通过 `SmartLifecycle` 管理，保证 Spring 容器初始化完成后启动 Netty，并在应用关闭时优雅释放端口、Channel 和线程组。

文件位置：`src/main/java/io/github/atengk/netty/server/NettyServerLifecycle.java`

该生命周期组件负责让 Netty 服务随 Spring Boot 应用自动启动和优雅关闭。

```java
package io.github.atengk.netty.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Netty 服务生命周期控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyServerLifecycle implements SmartLifecycle {

    private final NettyServer nettyServer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 启动服务
     */
    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("开始启动Netty服务");
            nettyServer.start();
        }
    }

    /**
     * 停止服务
     */
    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("开始停止Netty服务");
            nettyServer.stop();
        }
    }

    /**
     * 停止服务并执行回调
     *
     * @param callback 停止完成回调
     */
    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    /**
     * 判断是否运行
     *
     * @return true 表示运行中
     */
    @Override
    public boolean isRunning() {
        return running.get() && nettyServer.isRunning();
    }

    /**
     * 是否自动启动
     *
     * @return true 表示 Spring Boot 启动后自动启动
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 启动阶段，数值越大越晚启动、越早关闭
     *
     * @return 生命周期阶段
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
```

生命周期控制需要注意以下几点：

| 事项             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 避免重复启动     | 使用运行状态标识控制幂等                                     |
| 关闭顺序清晰     | 先关闭服务端 Channel，再释放 workerGroup，最后释放 bossGroup |
| 处理中断状态     | 捕获 `InterruptedException` 后需要恢复线程中断标记           |
| 不要吞掉启动异常 | 启动失败应记录日志并抛出异常，避免应用假启动                 |
| 优雅停机         | 使用 `shutdownGracefully()` 释放 EventLoopGroup              |

如果需要在关闭前清理所有客户端连接，可以在 `NettyServer.stop()` 中增加连接管理器清理逻辑，例如关闭所有 Channel、清空会话 Map、记录剩余连接数等。

### 配置参数管理

Netty 的端口、线程数、连接队列、心跳时间、最大包长度等参数不建议写死在代码中，应统一放入 `application.yml`，再通过 `@ConfigurationProperties` 绑定到配置类。这样可以在不同环境中通过配置文件或启动参数调整服务行为。

文件位置：`src/main/java/io/github/atengk/netty/config/NettyProperties.java`

该配置类用于读取 `application.yml` 中的 `netty` 配置项，并提供默认值。

```java
package io.github.atengk.netty.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Netty 配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "netty")
public class NettyProperties {

    /**
     * 服务端配置
     */
    private Server server = new Server();

    /**
     * Netty 服务端配置
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Server {

        /**
         * 服务监听端口
         */
        private int port = 9000;

        /**
         * Boss 线程数
         */
        private int bossThreads = 1;

        /**
         * Worker 线程数，0 表示使用默认值
         */
        private int workerThreads = 0;

        /**
         * 服务端连接队列大小
         */
        private int backlog = 1024;

        /**
         * 是否开启 TCP keepalive
         */
        private boolean keepAlive = true;

        /**
         * 是否开启 TCP_NODELAY
         */
        private boolean tcpNoDelay = true;

        /**
         * 最大帧长度
         */
        private int maxFrameLength = 1024 * 1024;

        /**
         * 读空闲时间，单位秒
         */
        private int readerIdleSeconds = 60;

        /**
         * 写空闲时间，单位秒
         */
        private int writerIdleSeconds = 30;

        /**
         * 读写空闲时间，单位秒
         */
        private int allIdleSeconds = 90;

        /**
         * 业务线程池核心线程数
         */
        private int businessCoreThreads = 8;

        /**
         * 业务线程池最大线程数
         */
        private int businessMaxThreads = 32;

        /**
         * 业务线程池队列容量
         */
        private int businessQueueCapacity = 1000;
    }
}
```

配置文件示例：

文件位置：`src/main/resources/application.yml`

该配置文件用于统一管理 Netty 服务端口、线程数、连接参数、心跳时间和消息大小限制。

```yaml
netty:
  server:
    # Netty 服务监听端口
    port: 9000

    # Boss 线程数，负责接收连接
    boss-threads: 1

    # Worker 线程数，0 表示使用 Netty 默认值
    worker-threads: 0

    # TCP 连接队列长度
    backlog: 1024

    # 开启 TCP keepalive
    keep-alive: true

    # 开启 TCP_NODELAY，降低小数据包延迟
    tcp-no-delay: true

    # 最大帧长度，超过该长度直接关闭连接
    max-frame-length: 1048576

    # 读空闲时间，单位秒
    reader-idle-seconds: 60

    # 写空闲时间，单位秒
    writer-idle-seconds: 30

    # 读写空闲时间，单位秒
    all-idle-seconds: 90

    # 业务线程池核心线程数
    business-core-threads: 8

    # 业务线程池最大线程数
    business-max-threads: 32

    # 业务线程池队列容量
    business-queue-capacity: 1000
```

如果需要在启动时覆盖端口，可以使用命令行参数：

```bash
java -jar springboot3-netty-demo.jar --netty.server.port=9100
```

配置参数建议按环境拆分，例如：

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

生产环境建议重点调整以下参数：

| 参数                               | 建议                                     |
| ---------------------------------- | ---------------------------------------- |
| `netty.server.port`                | 与 Spring Boot Web 端口分离，避免冲突    |
| `netty.server.worker-threads`      | 结合 CPU、连接数、压测结果调整           |
| `netty.server.max-frame-length`    | 根据协议最大消息体设置，避免过大         |
| `netty.server.reader-idle-seconds` | 根据客户端心跳周期设置，一般大于心跳间隔 |
| `netty.server.backlog`             | 高并发连接场景适当调大                   |
| `netty.server.tcp-no-delay`        | 实时通信场景通常开启                     |
| `netty.server.business-*`          | 根据业务耗时和吞吐量压测调整             |

完成配置参数管理后，后续服务端开发只需要依赖 `NettyProperties`，不要在 `ServerBootstrap`、`ChannelInitializer` 或 Handler 中硬编码端口、线程数、心跳时间和最大包长度。



## 服务端开发

服务端开发部分用于实现 Netty 服务端的核心通信能力，包括启动引导、线程组配置、Pipeline 初始化和业务 Handler 编写。该部分代码建立在前面已经定义的 `NettyProperties`、`MessagePacket`、`MessageEncoder`、`MessageDecoder`、`MessageType` 等基础类之上。

### ServerBootstrap 初始化

`ServerBootstrap` 是 Netty 服务端启动的核心入口，负责绑定端口、配置线程组、指定服务端 Channel 类型、设置连接参数并装配客户端 Channel 的初始化逻辑。Spring Boot 集成 Netty 时，建议将 `ServerBootstrap` 初始化逻辑封装到独立的 `NettyServer` 组件中，由 Spring 容器统一管理。

服务端启动流程如下：

```text
Spring Boot 启动
   │
   ▼
创建 bossGroup 和 workerGroup
   │
   ▼
初始化 ServerBootstrap
   │
   ▼
配置 ChannelOption
   │
   ▼
绑定 Netty 监听端口
   │
   ▼
等待客户端连接
```

文件位置：`src/main/java/io/github/atengk/netty/server/NettyServer.java`

该服务类负责初始化 `ServerBootstrap`、绑定端口、启动 Netty 服务，并在应用关闭时释放线程组资源。

```java
package io.github.atengk.netty.server;

import io.github.atengk.netty.config.NettyProperties;
import io.github.atengk.netty.initializer.NettyServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Netty 服务端
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyServer {

    private final NettyProperties nettyProperties;
    private final NettyServerChannelInitializer channelInitializer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * 启动 Netty 服务
     */
    public synchronized void start() {
        if (isRunning()) {
            log.warn("Netty服务已处于运行状态，无需重复启动");
            return;
        }

        NettyProperties.Server server = nettyProperties.getServer();

        bossGroup = new NioEventLoopGroup(server.getBossThreads());
        workerGroup = server.getWorkerThreads() > 0
                ? new NioEventLoopGroup(server.getWorkerThreads())
                : new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(channelInitializer)
                    // 服务端连接队列长度
                    .option(ChannelOption.SO_BACKLOG, server.getBacklog())
                    // 允许地址端口复用，方便服务快速重启
                    .option(ChannelOption.SO_REUSEADDR, true)
                    // 开启 TCP keepalive
                    .childOption(ChannelOption.SO_KEEPALIVE, server.isKeepAlive())
                    // 开启 TCP_NODELAY，降低小数据包延迟
                    .childOption(ChannelOption.TCP_NODELAY, server.isTcpNoDelay())
                    // 设置接收缓冲区
                    .childOption(ChannelOption.SO_RCVBUF, 1024 * 64)
                    // 设置发送缓冲区
                    .childOption(ChannelOption.SO_SNDBUF, 1024 * 64)
                    // 设置写缓冲水位，避免写入过快导致内存压力
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(
                            1024 * 512,
                            1024 * 1024
                    ));

            ChannelFuture channelFuture = bootstrap.bind(server.getPort()).sync();
            serverChannel = channelFuture.channel();

            log.info("Netty服务启动成功，port={}", server.getPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Netty服务启动被中断，port={}", server.getPort(), e);
            stop();
        } catch (Exception e) {
            log.error("Netty服务启动失败，port={}", server.getPort(), e);
            stop();
            throw e;
        }
    }

    /**
     * 停止 Netty 服务
     */
    public synchronized void stop() {
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
            serverChannel = null;
            log.info("Netty服务端Channel已关闭");
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup = null;
            log.info("Netty workerGroup 已释放");
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup = null;
            log.info("Netty bossGroup 已释放");
        }
    }

    /**
     * 判断服务是否运行
     *
     * @return true 表示运行中
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
}
```

`ServerBootstrap` 中常用参数说明如下：

| 参数                      | 说明                                         |
| ------------------------- | -------------------------------------------- |
| `SO_BACKLOG`              | 服务端连接队列大小，高并发连接场景可适当调大 |
| `SO_REUSEADDR`            | 允许地址复用，减少服务重启时端口占用问题     |
| `SO_KEEPALIVE`            | 开启 TCP keepalive，辅助检测失效连接         |
| `TCP_NODELAY`             | 禁用 Nagle 算法，降低小包发送延迟            |
| `SO_RCVBUF`               | TCP 接收缓冲区大小                           |
| `SO_SNDBUF`               | TCP 发送缓冲区大小                           |
| `WRITE_BUFFER_WATER_MARK` | 写缓冲区高低水位，避免写入过快造成内存压力   |

如果服务启动失败，优先检查端口是否被占用：

```bash
# 查看 9000 端口占用情况
lsof -i :9000

# 或使用 ss 查看监听状态
ss -lntp | grep 9000
```

`lsof -i :9000` 用于查看指定端口被哪个进程占用，`ss -lntp` 用于查看当前 TCP 监听端口和进程信息。

### EventLoopGroup 配置

`EventLoopGroup` 是 Netty 的线程模型实现，负责处理网络事件。服务端通常使用两个线程组：`bossGroup` 接收连接，`workerGroup` 处理读写事件。线程数应通过配置文件控制，避免在代码中写死。

线程配置建议如下：

| 线程组        | 职责                           | 建议                          |
| ------------- | ------------------------------ | ----------------------------- |
| `bossGroup`   | 接收客户端连接                 | 一般配置为 `1`                |
| `workerGroup` | 处理读写事件、Pipeline 调度    | 默认即可，或按 CPU 核心数调整 |
| 业务线程池    | 处理数据库、远程调用、耗时计算 | 独立配置，避免阻塞 I/O 线程   |

文件位置：`src/main/java/io/github/atengk/netty/config/NettyBusinessExecutorConfig.java`

该配置类用于创建独立业务线程池，避免耗时业务直接阻塞 Netty 的 I/O 线程。

```java
package io.github.atengk.netty.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Netty 业务线程池配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
@RequiredArgsConstructor
public class NettyBusinessExecutorConfig {

    private final NettyProperties nettyProperties;

    /**
     * 创建 Netty 业务线程池
     *
     * @return 业务线程池
     */
    @Bean("nettyBusinessExecutor")
    public ThreadPoolExecutor nettyBusinessExecutor() {
        NettyProperties.Server server = nettyProperties.getServer();

        return new ThreadPoolExecutor(
                server.getBusinessCoreThreads(),
                server.getBusinessMaxThreads(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(server.getBusinessQueueCapacity()),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("netty-business-" + thread.threadId());
                    thread.setDaemon(false);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

`CallerRunsPolicy` 表示当线程池和队列都满时，由提交任务的线程执行该任务。该策略可以在高峰期形成一定反压，避免任务无限堆积，但也可能影响 I/O 线程，因此生产环境需要结合压测结果调整线程数和队列容量。

在 `application.yml` 中补充业务线程池配置：

```yaml
netty:
  server:
    # Boss 线程数，负责接收连接
    boss-threads: 1

    # Worker 线程数，0 表示使用 Netty 默认值
    worker-threads: 0

    # 业务线程池核心线程数
    business-core-threads: 8

    # 业务线程池最大线程数
    business-max-threads: 32

    # 业务线程池队列容量
    business-queue-capacity: 1000
```

线程配置需要根据业务类型调整。如果 Handler 中只做轻量级内存操作，`workerGroup` 默认配置通常可以满足需求；如果业务逻辑包含数据库写入、HTTP 调用、文件处理或复杂计算，应将业务逻辑提交到 `nettyBusinessExecutor` 中执行。

### ChannelInitializer 实现

`ChannelInitializer` 用于初始化每个客户端连接对应的 `ChannelPipeline`。服务端每接入一个新的客户端连接，Netty 都会调用 `initChannel` 方法，将心跳检测、粘包拆包、编解码器、业务 Handler 和异常 Handler 添加到 Pipeline 中。

推荐 Pipeline 顺序如下：

```text
IdleStateHandler
   │
   ▼
LengthFieldBasedFrameDecoder
   │
   ▼
MessageDecoder
   │
   ▼
MessageEncoder
   │
   ▼
NettyServerHandler
   │
   ▼
NettyExceptionHandler
```

文件位置：`src/main/java/io/github/atengk/netty/initializer/NettyServerChannelInitializer.java`

该初始化器负责为服务端每个客户端连接装配完整的 Pipeline。

```java
package io.github.atengk.netty.initializer;

import io.github.atengk.netty.codec.MessageDecoder;
import io.github.atengk.netty.codec.MessageEncoder;
import io.github.atengk.netty.config.NettyProperties;
import io.github.atengk.netty.handler.NettyExceptionHandler;
import io.github.atengk.netty.handler.NettyServerHandler;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Netty 服务端 Channel 初始化器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final NettyProperties nettyProperties;
    private final ObjectProvider<NettyServerHandler> nettyServerHandlerProvider;
    private final ObjectProvider<NettyExceptionHandler> nettyExceptionHandlerProvider;

    /**
     * 初始化客户端连接 Pipeline
     *
     * @param channel 客户端连接
     */
    @Override
    protected void initChannel(SocketChannel channel) {
        NettyProperties.Server server = nettyProperties.getServer();
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast("idleStateHandler", new IdleStateHandler(
                server.getReaderIdleSeconds(),
                server.getWriterIdleSeconds(),
                server.getAllIdleSeconds(),
                TimeUnit.SECONDS
        ));

        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                server.getMaxFrameLength() + ProtocolConstant.HEADER_LENGTH,
                6,
                4,
                0,
                0
        ));

        pipeline.addLast("messageDecoder", new MessageDecoder(server.getMaxFrameLength()));
        pipeline.addLast("messageEncoder", new MessageEncoder());

        // 业务 Handler 使用原型 Bean，避免多连接共享有状态对象
        pipeline.addLast("nettyServerHandler", nettyServerHandlerProvider.getObject());

        // 异常 Handler 放在业务 Handler 后面，统一收口异常
        pipeline.addLast("nettyExceptionHandler", nettyExceptionHandlerProvider.getObject());

        log.info("服务端Pipeline初始化完成，remoteAddress={}", channel.remoteAddress());
    }
}
```

`LengthFieldBasedFrameDecoder` 参数说明如下：

| 参数                  | 当前值                           | 说明                                                    |
| --------------------- | -------------------------------- | ------------------------------------------------------- |
| `maxFrameLength`      | `maxFrameLength + HEADER_LENGTH` | 单个完整数据包最大长度                                  |
| `lengthFieldOffset`   | `6`                              | 长度字段偏移量，魔数 4 字节 + 版本 1 字节 + 类型 1 字节 |
| `lengthFieldLength`   | `4`                              | 消息体长度字段占 4 字节                                 |
| `lengthAdjustment`    | `0`                              | 长度字段只表示消息体长度，不需要修正                    |
| `initialBytesToStrip` | `0`                              | 不剥离协议头，后续解码器继续读取完整协议                |

该实现要求客户端和服务端使用一致的数据包格式：

```text
魔数 4 字节 + 版本 1 字节 + 消息类型 1 字节 + 消息长度 4 字节 + 消息体 N 字节
```

### 业务 Handler 开发

业务 Handler 负责处理客户端连接事件、消息读取事件、心跳空闲事件和连接断开事件。为了避免 Handler 过重，推荐 Handler 只做网络事件处理，具体业务逻辑交给 `MessageDispatcher` 或业务 Service 完成。

本章节示例使用 JSON 作为消息体内容，协议层仍然使用前面定义的二进制数据包。请求体中包含 `requestId`，客户端可以通过该字段完成响应回调匹配。

业务请求对象如下：

文件位置：`src/main/java/io/github/atengk/netty/protocol/BusinessRequest.java`

该对象表示客户端发送到服务端的业务请求内容。

```java
package io.github.atengk.netty.protocol;

import lombok.Data;

/**
 * Netty 业务请求
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class BusinessRequest {

    /**
     * 请求ID，用于客户端响应回调匹配
     */
    private String requestId;

    /**
     * 请求内容
     */
    private String content;
}
```

业务响应对象如下：

文件位置：`src/main/java/io/github/atengk/netty/protocol/BusinessResponse.java`

该对象表示服务端返回给客户端的业务响应内容。

```java
package io.github.atengk.netty.protocol;

import lombok.Data;

/**
 * Netty 业务响应
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class BusinessResponse {

    /**
     * 请求ID，与客户端请求保持一致
     */
    private String requestId;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private Object data;
}
```

业务 Service 示例：

文件位置：`src/main/java/io/github/atengk/netty/service/DeviceMessageService.java`

该业务服务用于处理客户端上报的业务消息，实际项目中可以替换为设备认证、数据入库、指令处理等逻辑。

```java
package io.github.atengk.netty.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 设备消息业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class DeviceMessageService {

    /**
     * 处理设备消息
     *
     * @param content 消息内容
     * @return 处理结果
     */
    public String handleMessage(String content) {
        if (StrUtil.isBlank(content)) {
            log.warn("设备消息内容为空");
            return "消息内容为空";
        }

        log.info("处理设备消息，content={}", content);
        return "服务端已处理：" + content;
    }
}
```

消息分发器示例：

文件位置：`src/main/java/io/github/atengk/netty/dispatcher/MessageDispatcher.java`

该分发器负责解析业务请求、调用业务 Service，并向客户端写回统一响应。

```java
package io.github.atengk.netty.dispatcher;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import io.github.atengk.netty.protocol.BusinessRequest;
import io.github.atengk.netty.protocol.BusinessResponse;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.github.atengk.netty.service.DeviceMessageService;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Netty 消息分发器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDispatcher {

    private final DeviceMessageService deviceMessageService;

    /**
     * 分发消息
     *
     * @param ctx 上下文
     * @param packet 消息包
     */
    public void dispatch(ChannelHandlerContext ctx, MessagePacket packet) {
        if (packet.getMessageType() != MessageType.BUSINESS_REQUEST) {
            log.warn("不支持的消息类型，messageType={}，remoteAddress={}",
                    packet.getMessageType(), ctx.channel().remoteAddress());
            return;
        }

        BusinessResponse response = new BusinessResponse();

        try {
            String body = StrUtil.str(packet.getBody(), StandardCharsets.UTF_8);
            BusinessRequest request = JSONUtil.toBean(body, BusinessRequest.class);

            if (StrUtil.isBlank(request.getRequestId())) {
                log.warn("请求ID为空，remoteAddress={}", ctx.channel().remoteAddress());
                response.setSuccess(false);
                response.setMessage("请求ID不能为空");
                writeResponse(ctx, response);
                return;
            }

            response.setRequestId(request.getRequestId());

            if (StrUtil.isBlank(request.getContent())) {
                log.warn("业务请求内容为空，requestId={}", request.getRequestId());
                response.setSuccess(false);
                response.setMessage("请求内容不能为空");
                writeResponse(ctx, response);
                return;
            }

            String result = deviceMessageService.handleMessage(request.getContent());

            response.setSuccess(true);
            response.setMessage("处理成功");
            response.setData(result);

            writeResponse(ctx, response);
            log.info("业务消息处理完成，requestId={}，remoteAddress={}",
                    request.getRequestId(), ctx.channel().remoteAddress());
        } catch (JSONException e) {
            log.error("业务消息JSON解析失败，remoteAddress={}", ctx.channel().remoteAddress(), e);
            response.setSuccess(false);
            response.setMessage("请求格式错误");
            writeResponse(ctx, response);
        } catch (Exception e) {
            log.error("业务消息处理异常，remoteAddress={}", ctx.channel().remoteAddress(), e);
            response.setSuccess(false);
            response.setMessage("服务端处理异常");
            writeResponse(ctx, response);
        }
    }

    /**
     * 写出响应
     *
     * @param ctx 上下文
     * @param response 响应对象
     */
    private void writeResponse(ChannelHandlerContext ctx, BusinessResponse response) {
        MessagePacket responsePacket = new MessagePacket();
        responsePacket.setVersion(ProtocolConstant.VERSION);
        responsePacket.setMessageType(MessageType.BUSINESS_RESPONSE);
        responsePacket.setBody(JSONUtil.toJsonStr(response).getBytes(StandardCharsets.UTF_8));

        ctx.writeAndFlush(responsePacket);
    }
}
```

服务端业务 Handler 示例：

文件位置：`src/main/java/io/github/atengk/netty/handler/NettyServerHandler.java`

该 Handler 负责接收消息、处理心跳、提交业务分发任务，并在连接断开时记录日志。

```java
package io.github.atengk.netty.handler;

import cn.hutool.core.util.ArrayUtil;
import io.github.atengk.netty.dispatcher.MessageDispatcher;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadPoolExecutor;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Netty 服务端业务处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class NettyServerHandler extends SimpleChannelInboundHandler<MessagePacket> {

    private final MessageDispatcher messageDispatcher;

    @Qualifier("nettyBusinessExecutor")
    private final ThreadPoolExecutor nettyBusinessExecutor;

    /**
     * 客户端连接建立
     *
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("客户端连接成功，remoteAddress={}", ctx.channel().remoteAddress());
    }

    /**
     * 读取客户端消息
     *
     * @param ctx 上下文
     * @param packet 消息包
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessagePacket packet) {
        if (packet == null || ArrayUtil.isEmpty(packet.getBody())) {
            log.warn("收到空消息，remoteAddress={}", ctx.channel().remoteAddress());
            return;
        }

        if (packet.getMessageType() == MessageType.HEARTBEAT) {
            writeHeartbeatAck(ctx);
            return;
        }

        nettyBusinessExecutor.execute(() -> messageDispatcher.dispatch(ctx, packet));
    }

    /**
     * 处理空闲事件
     *
     * @param ctx 上下文
     * @param evt 事件对象
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.warn("客户端连接空闲超时，关闭连接，remoteAddress={}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        super.userEventTriggered(ctx, evt);
    }

    /**
     * 客户端连接断开
     *
     * @param ctx 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("客户端连接断开，remoteAddress={}", ctx.channel().remoteAddress());
    }

    /**
     * 写出心跳响应
     *
     * @param ctx 上下文
     */
    private void writeHeartbeatAck(ChannelHandlerContext ctx) {
        MessagePacket response = new MessagePacket();
        response.setVersion(ProtocolConstant.VERSION);
        response.setMessageType(MessageType.HEARTBEAT_ACK);
        response.setBody("pong".getBytes(StandardCharsets.UTF_8));

        ctx.writeAndFlush(response);
        log.debug("服务端响应客户端心跳，remoteAddress={}", ctx.channel().remoteAddress());
    }
}
```

服务端 Handler 开发需要注意以下几点：

| 注意事项             | 说明                                                  |
| -------------------- | ----------------------------------------------------- |
| 不要阻塞 I/O 线程    | 耗时业务提交到业务线程池                              |
| Handler 尽量职责单一 | 网络事件处理与业务处理分离                            |
| 异常统一处理         | 业务异常在分发器内处理，Pipeline 异常交给异常 Handler |
| 心跳及时响应         | 收到心跳请求后快速返回心跳响应                        |
| 响应带请求 ID        | 客户端才能准确完成响应回调匹配                        |

## 客户端开发

客户端开发部分用于实现 Netty 客户端连接、重连、请求发送和响应回调处理。客户端既可以用于本地通信测试，也可以作为实际业务中的设备模拟器、内部 TCP 调用客户端或压测客户端基础实现。

### Bootstrap 初始化

`Bootstrap` 是 Netty 客户端启动入口，负责配置客户端线程组、Channel 类型、连接参数和客户端 Pipeline。客户端通常只需要一个 `EventLoopGroup`，用于处理连接、读写和重连事件。

在配置客户端之前，需要在 `NettyProperties` 中补充客户端配置项。

文件位置：`src/main/java/io/github/atengk/netty/config/NettyProperties.java`

如果前面配置类中只有 `server` 配置，需要增加以下 `client` 字段和内部类。

```java
/**
 * 客户端配置
 */
private Client client = new Client();

/**
 * Netty 客户端配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public static class Client {

    /**
     * 服务端地址
     */
    private String host = "127.0.0.1";

    /**
     * 服务端端口
     */
    private int port = 9000;

    /**
     * 连接超时时间，单位毫秒
     */
    private int connectTimeoutMillis = 3000;

    /**
     * 是否开启自动重连
     */
    private boolean reconnectEnabled = true;

    /**
     * 重连间隔，单位秒
     */
    private int reconnectDelaySeconds = 3;

    /**
     * 最大重连次数，0 表示不限制
     */
    private int maxReconnectTimes = 0;

    /**
     * 响应超时时间，单位秒
     */
    private int responseTimeoutSeconds = 10;

    /**
     * 最大帧长度
     */
    private int maxFrameLength = 1024 * 1024;

    /**
     * 读空闲时间，单位秒
     */
    private int readerIdleSeconds = 60;

    /**
     * 写空闲时间，单位秒
     */
    private int writerIdleSeconds = 30;

    /**
     * 读写空闲时间，单位秒
     */
    private int allIdleSeconds = 90;
}
```

客户端配置文件如下：

文件位置：`src/main/resources/application.yml`

该配置用于指定客户端要连接的服务端地址、端口、连接超时时间、重连策略和响应超时时间。

```yaml
netty:
  client:
    # 服务端地址
    host: 127.0.0.1

    # 服务端端口
    port: 9000

    # 连接超时时间，单位毫秒
    connect-timeout-millis: 3000

    # 是否开启自动重连
    reconnect-enabled: true

    # 重连间隔，单位秒
    reconnect-delay-seconds: 3

    # 最大重连次数，0 表示不限制
    max-reconnect-times: 0

    # 响应超时时间，单位秒
    response-timeout-seconds: 10

    # 最大帧长度
    max-frame-length: 1048576

    # 读空闲时间，单位秒
    reader-idle-seconds: 60

    # 写空闲时间，单位秒
    writer-idle-seconds: 30

    # 读写空闲时间，单位秒
    all-idle-seconds: 90
```

文件位置：`src/main/java/io/github/atengk/netty/initializer/NettyClientChannelInitializer.java`

该初始化器用于为客户端 Channel 装配心跳检测、拆包、编解码和响应 Handler。

```java
package io.github.atengk.netty.initializer;

import io.github.atengk.netty.codec.MessageDecoder;
import io.github.atengk.netty.codec.MessageEncoder;
import io.github.atengk.netty.config.NettyProperties;
import io.github.atengk.netty.handler.NettyClientHandler;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Netty 客户端 Channel 初始化器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final NettyProperties nettyProperties;
    private final ObjectProvider<NettyClientHandler> nettyClientHandlerProvider;

    /**
     * 初始化客户端 Pipeline
     *
     * @param channel 客户端连接
     */
    @Override
    protected void initChannel(SocketChannel channel) {
        NettyProperties.Client client = nettyProperties.getClient();
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast("idleStateHandler", new IdleStateHandler(
                client.getReaderIdleSeconds(),
                client.getWriterIdleSeconds(),
                client.getAllIdleSeconds(),
                TimeUnit.SECONDS
        ));

        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                client.getMaxFrameLength() + ProtocolConstant.HEADER_LENGTH,
                6,
                4,
                0,
                0
        ));

        pipeline.addLast("messageDecoder", new MessageDecoder(client.getMaxFrameLength()));
        pipeline.addLast("messageEncoder", new MessageEncoder());
        pipeline.addLast("nettyClientHandler", nettyClientHandlerProvider.getObject());

        log.info("客户端Pipeline初始化完成");
    }
}
```

文件位置：`src/main/java/io/github/atengk/netty/client/NettyClient.java`

该客户端类负责初始化 `Bootstrap`、连接服务端、发送请求和关闭连接。

```java
package io.github.atengk.netty.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.netty.callback.ClientResponseCallbackManager;
import io.github.atengk.netty.config.NettyProperties;
import io.github.atengk.netty.initializer.NettyClientChannelInitializer;
import io.github.atengk.netty.protocol.BusinessRequest;
import io.github.atengk.netty.protocol.BusinessResponse;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Netty 客户端
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyClient {

    private final NettyProperties nettyProperties;
    private final NettyClientChannelInitializer channelInitializer;
    private final ClientResponseCallbackManager callbackManager;

    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectTimes = new AtomicInteger(0);

    private EventLoopGroup group;
    private Channel channel;

    /**
     * 连接服务端
     */
    public synchronized void connect() {
        if (isActive()) {
            log.warn("Netty客户端已连接，无需重复连接");
            return;
        }

        if (!connecting.compareAndSet(false, true)) {
            log.warn("Netty客户端正在连接中，请勿重复触发");
            return;
        }

        NettyProperties.Client client = nettyProperties.getClient();
        group = group == null ? new NioEventLoopGroup() : group;

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(channelInitializer)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, client.getConnectTimeoutMillis())
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture channelFuture = bootstrap.connect(client.getHost(), client.getPort()).sync();
            channel = channelFuture.channel();
            reconnectTimes.set(0);

            log.info("Netty客户端连接成功，server={}:{}", client.getHost(), client.getPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Netty客户端连接被中断，server={}:{}", client.getHost(), client.getPort(), e);
            scheduleReconnect();
        } catch (Exception e) {
            log.error("Netty客户端连接失败，server={}:{}", client.getHost(), client.getPort(), e);
            scheduleReconnect();
        } finally {
            connecting.set(false);
        }
    }

    /**
     * 发送业务请求
     *
     * @param content 请求内容
     * @return 响应结果
     */
    public CompletableFuture<BusinessResponse> sendAsync(String content) {
        if (!isActive()) {
            log.warn("Netty客户端未连接，无法发送请求");
            return CompletableFuture.failedFuture(new IllegalStateException("Netty客户端未连接"));
        }

        NettyProperties.Client client = nettyProperties.getClient();
        String requestId = IdUtil.fastSimpleUUID();

        BusinessRequest request = new BusinessRequest();
        request.setRequestId(requestId);
        request.setContent(content);

        MessagePacket packet = new MessagePacket();
        packet.setVersion(ProtocolConstant.VERSION);
        packet.setMessageType(MessageType.BUSINESS_REQUEST);
        packet.setBody(JSONUtil.toJsonStr(request).getBytes(StandardCharsets.UTF_8));

        CompletableFuture<BusinessResponse> responseFuture = callbackManager.register(
                requestId,
                Duration.ofSeconds(client.getResponseTimeoutSeconds())
        );

        channel.writeAndFlush(packet).addListener(future -> {
            if (future.isSuccess()) {
                log.info("客户端请求发送成功，requestId={}", requestId);
                return;
            }

            callbackManager.remove(requestId);
            responseFuture.completeExceptionally(future.cause());
            log.error("客户端请求发送失败，requestId={}", requestId, future.cause());
        });

        return responseFuture;
    }

    /**
     * 安排重连
     */
    public void scheduleReconnect() {
        NettyProperties.Client client = nettyProperties.getClient();
        if (!client.isReconnectEnabled()) {
            log.info("客户端自动重连未开启");
            return;
        }

        if (client.getMaxReconnectTimes() > 0 && reconnectTimes.incrementAndGet() > client.getMaxReconnectTimes()) {
            log.warn("客户端重连次数已达到上限，maxReconnectTimes={}", client.getMaxReconnectTimes());
            return;
        }

        if (group == null || group.isShuttingDown()) {
            group = new NioEventLoopGroup();
        }

        log.info("客户端将在{}秒后发起重连，当前重连次数={}",
                client.getReconnectDelaySeconds(), reconnectTimes.get());

        group.schedule(this::connect, client.getReconnectDelaySeconds(), TimeUnit.SECONDS);
    }

    /**
     * 关闭客户端
     */
    public synchronized void close() {
        if (channel != null) {
            channel.close().syncUninterruptibly();
            channel = null;
            log.info("Netty客户端Channel已关闭");
        }

        if (group != null) {
            group.shutdownGracefully().syncUninterruptibly();
            group = null;
            log.info("Netty客户端线程组已释放");
        }

        callbackManager.failAll(new IllegalStateException("Netty客户端已关闭"));
    }

    /**
     * 判断连接是否可用
     *
     * @return true 表示连接可用
     */
    public boolean isActive() {
        return channel != null && channel.isActive();
    }
}
```

### 连接与重连机制

客户端连接可能因为服务端重启、网络抖动、防火墙断开、心跳超时等原因失效。为了提高可用性，客户端需要在连接断开后自动触发重连，并限制重连频率，避免异常情况下频繁创建连接。

重连机制建议满足以下要求：

| 要求               | 说明                              |
| ------------------ | --------------------------------- |
| 连接断开后自动重连 | 在 `channelInactive` 中触发重连   |
| 避免重复重连       | 使用 `AtomicBoolean` 控制连接状态 |
| 支持最大重连次数   | 防止无限重连影响系统资源          |
| 支持重连间隔       | 避免服务端不可用时频繁连接        |
| 关闭时清理回调     | 客户端关闭后应释放未完成请求      |

文件位置：`src/main/java/io/github/atengk/netty/handler/NettyClientHandler.java`

该 Handler 负责处理客户端连接状态、心跳事件和服务端响应，并在连接断开时触发重连。

```java
package io.github.atengk.netty.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.netty.callback.ClientResponseCallbackManager;
import io.github.atengk.netty.client.NettyClient;
import io.github.atengk.netty.protocol.BusinessResponse;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Netty 客户端处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class NettyClientHandler extends SimpleChannelInboundHandler<MessagePacket> {

    private final ClientResponseCallbackManager callbackManager;
    private final ObjectProvider<NettyClient> nettyClientProvider;

    /**
     * 客户端连接成功
     *
     * @param ctx 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("客户端连接服务端成功，remoteAddress={}", ctx.channel().remoteAddress());
    }

    /**
     * 读取服务端响应
     *
     * @param ctx 上下文
     * @param packet 消息包
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessagePacket packet) {
        if (packet.getMessageType() == MessageType.HEARTBEAT_ACK) {
            log.debug("客户端收到服务端心跳响应，remoteAddress={}", ctx.channel().remoteAddress());
            return;
        }

        if (packet.getMessageType() != MessageType.BUSINESS_RESPONSE) {
            log.warn("客户端收到未知消息类型，messageType={}", packet.getMessageType());
            return;
        }

        String body = StrUtil.str(packet.getBody(), StandardCharsets.UTF_8);
        BusinessResponse response = JSONUtil.toBean(body, BusinessResponse.class);
        callbackManager.complete(response);
    }

    /**
     * 处理空闲事件
     *
     * @param ctx 上下文
     * @param evt 事件对象
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            MessagePacket heartbeat = new MessagePacket();
            heartbeat.setVersion(ProtocolConstant.VERSION);
            heartbeat.setMessageType(MessageType.HEARTBEAT);
            heartbeat.setBody("ping".getBytes(StandardCharsets.UTF_8));

            ctx.writeAndFlush(heartbeat);
            log.debug("客户端发送心跳，remoteAddress={}", ctx.channel().remoteAddress());
            return;
        }

        super.userEventTriggered(ctx, evt);
    }

    /**
     * 客户端连接断开
     *
     * @param ctx 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("客户端连接已断开，remoteAddress={}", ctx.channel().remoteAddress());
        callbackManager.failAll(new IllegalStateException("客户端连接已断开"));
        nettyClientProvider.getObject().scheduleReconnect();
    }

    /**
     * 处理异常
     *
     * @param ctx 上下文
     * @param cause 异常信息
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("客户端连接异常，remoteAddress={}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
```

客户端启动可以通过 `ApplicationRunner` 在应用启动后执行，也可以在测试类中手动调用。生产环境如果客户端是常驻连接，建议通过生命周期组件统一管理。

文件位置：`src/main/java/io/github/atengk/netty/client/NettyClientRunner.java`

该启动器用于在 Spring Boot 启动完成后自动连接 Netty 服务端。

```java
package io.github.atengk.netty.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Netty 客户端启动器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyClientRunner implements ApplicationRunner {

    private final NettyClient nettyClient;

    /**
     * 应用启动后连接服务端
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("开始启动Netty客户端连接");
        nettyClient.connect();
    }
}
```

如果当前项目既作为服务端又作为客户端，是否启用客户端建议通过配置项控制，例如 `netty.client.enabled=true`，避免服务端项目启动时默认创建客户端连接。

### 请求发送处理

请求发送处理用于将业务数据封装为协议消息，通过客户端 Channel 写出到服务端。为了支持并发请求和异步响应，客户端每次发送请求时都生成一个 `requestId`，并在回调管理器中注册一个 `CompletableFuture`。

请求发送流程如下：

```text
调用 sendAsync
   │
   ▼
生成 requestId
   │
   ▼
构造 BusinessRequest
   │
   ▼
序列化为 JSON 字节数组
   │
   ▼
封装 MessagePacket
   │
   ▼
注册响应回调
   │
   ▼
writeAndFlush 发送请求
   │
   ▼
等待服务端响应
```

发送请求示例：

```java
CompletableFuture<BusinessResponse> responseFuture = nettyClient.sendAsync("hello netty");

responseFuture.whenComplete((response, throwable) -> {
    if (throwable != null) {
        log.error("请求处理失败", throwable);
        return;
    }

    log.info("收到服务端响应，success={}，message={}，data={}",
            response.getSuccess(), response.getMessage(), response.getData());
});
```

如果需要在启动后发送测试请求，可以添加一个测试 Runner。

文件位置：`src/main/java/io/github/atengk/netty/client/NettyClientTestRunner.java`

该测试启动器用于在客户端连接成功后发送一条测试业务消息。

```java
package io.github.atengk.netty.client;

import io.github.atengk.netty.protocol.BusinessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Netty 客户端测试请求启动器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class NettyClientTestRunner implements ApplicationRunner {

    private final NettyClient nettyClient;

    /**
     * 应用启动后发送测试请求
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            TimeUnit.SECONDS.sleep(2);

            CompletableFuture<BusinessResponse> future = nettyClient.sendAsync("hello springboot3 netty");

            future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    log.error("测试请求发送失败", throwable);
                    return;
                }

                log.info("测试请求收到响应，success={}，message={}，data={}",
                        response.getSuccess(), response.getMessage(), response.getData());
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("测试请求发送被中断", e);
        }
    }
}
```

如果业务需要同步等待结果，可以在调用方使用 `get` 或 `join`，但不建议在 Netty I/O 线程中同步等待，避免阻塞事件循环。

```java
BusinessResponse response = nettyClient.sendAsync("sync request").get(10, TimeUnit.SECONDS);
```

该写法适合普通业务线程或测试代码，不适合在 `channelRead0`、`userEventTriggered` 等 Netty I/O 回调方法中使用。

### 响应回调处理

响应回调处理用于将服务端返回的响应与客户端发出的请求对应起来。客户端发送请求时注册 `requestId -> CompletableFuture`，服务端响应回来后，客户端 Handler 解析响应体中的 `requestId`，并完成对应的 `CompletableFuture`。

文件位置：`src/main/java/io/github/atengk/netty/callback/ClientResponseCallbackManager.java`

该回调管理器负责注册请求回调、完成响应回调、移除超时请求和在连接断开时失败所有未完成请求。

```java
package io.github.atengk.netty.callback;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.netty.protocol.BusinessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 客户端响应回调管理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class ClientResponseCallbackManager {

    private final Map<String, CompletableFuture<BusinessResponse>> callbackMap = new ConcurrentHashMap<>();

    /**
     * 注册响应回调
     *
     * @param requestId 请求ID
     * @param timeout 超时时间
     * @return 响应 Future
     */
    public CompletableFuture<BusinessResponse> register(String requestId, Duration timeout) {
        CompletableFuture<BusinessResponse> future = new CompletableFuture<>();

        callbackMap.put(requestId, future);

        future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((response, throwable) -> callbackMap.remove(requestId));

        log.debug("注册客户端响应回调，requestId={}", requestId);
        return future;
    }

    /**
     * 完成响应回调
     *
     * @param response 响应对象
     */
    public void complete(BusinessResponse response) {
        if (response == null || StrUtil.isBlank(response.getRequestId())) {
            log.warn("响应对象或请求ID为空，无法完成回调");
            return;
        }

        CompletableFuture<BusinessResponse> future = callbackMap.remove(response.getRequestId());
        if (future == null) {
            log.warn("未找到响应回调，requestId={}", response.getRequestId());
            return;
        }

        future.complete(response);
        log.debug("完成客户端响应回调，requestId={}", response.getRequestId());
    }

    /**
     * 移除响应回调
     *
     * @param requestId 请求ID
     */
    public void remove(String requestId) {
        if (StrUtil.isBlank(requestId)) {
            return;
        }

        callbackMap.remove(requestId);
        log.debug("移除客户端响应回调，requestId={}", requestId);
    }

    /**
     * 失败所有未完成请求
     *
     * @param throwable 异常信息
     */
    public void failAll(Throwable throwable) {
        callbackMap.forEach((requestId, future) -> {
            if (!future.isDone()) {
                future.completeExceptionally(throwable);
            }
        });

        callbackMap.clear();
        log.warn("已清理所有客户端响应回调");
    }

    /**
     * 获取待响应请求数量
     *
     * @return 待响应数量
     */
    public int size() {
        return callbackMap.size();
    }
}
```

响应回调处理需要注意以下几点：

| 注意事项                     | 说明                                 |
| ---------------------------- | ------------------------------------ |
| 请求必须有 `requestId`       | 否则无法匹配响应                     |
| 响应必须原样返回 `requestId` | 服务端响应体需要携带同一个请求 ID    |
| 请求需要设置超时             | 防止服务端未响应导致 Future 长期堆积 |
| 连接断开要失败未完成请求     | 避免调用方一直等待                   |
| 回调 Map 使用并发集合        | 客户端可能同时发送多个请求           |

完成客户端开发后，可以按以下顺序验证：

```bash
# 编译项目
mvn clean compile

# 启动服务端
mvn spring-boot:run

# 查看 Netty 端口是否监听
ss -lntp | grep 9000
```

验证时应重点观察以下日志：

```text
Netty服务启动成功，port=9000
Netty客户端连接成功，server=127.0.0.1:9000
客户端请求发送成功，requestId=xxx
收到业务消息，remoteAddress=xxx
业务消息处理完成，requestId=xxx
完成客户端响应回调，requestId=xxx
测试请求收到响应，success=true
```

如果没有看到客户端响应日志，优先检查以下问题：服务端和客户端协议格式是否一致、`MessageType` 是否一致、服务端是否返回了相同的 `requestId`、Pipeline 中编解码器顺序是否正确、端口是否被防火墙或本地安全策略拦截。

## 协议设计

协议设计用于定义客户端与服务端之间的数据交换规则，包括数据包结构、粘包拆包策略、编码器和解码器实现。Netty 本身只负责字节流传输，业务系统必须定义明确的协议格式，才能稳定地区分消息边界、识别消息类型并解析业务内容。

### 数据包格式

本项目采用固定协议头 + 可变消息体的数据包格式。固定协议头用于识别协议合法性、版本、消息类型和消息体长度，可变消息体用于承载 JSON、二进制数据或业务自定义内容。

数据包格式如下：

```text
+------------+----------+--------------+--------------+----------------+
| 魔数 4字节 | 版本 1字节 | 消息类型 1字节 | 消息长度 4字节 | 消息体 N字节     |
+------------+----------+--------------+--------------+----------------+
```

字段说明如下：

| 字段     | 长度   | 类型     | 说明                                   |
| -------- | ------ | -------- | -------------------------------------- |
| 魔数     | 4 字节 | `int`    | 用于快速识别非法数据包                 |
| 版本     | 1 字节 | `byte`   | 协议版本，便于后续协议升级             |
| 消息类型 | 1 字节 | `byte`   | 标识心跳、业务请求、业务响应等类型     |
| 消息长度 | 4 字节 | `int`    | 表示消息体字节长度                     |
| 消息体   | N 字节 | `byte[]` | 实际业务数据，示例中使用 JSON 字节数组 |

协议常量定义如下。

文件位置：`src/main/java/io/github/atengk/netty/protocol/ProtocolConstant.java`

该常量类用于统一维护协议魔数、版本号、协议头长度和默认字符集，编码器与解码器必须使用同一套常量。

```java
package io.github.atengk.netty.protocol;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Netty 自定义协议常量
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class ProtocolConstant {

    /**
     * 魔数，用于识别当前系统协议数据包
     */
    public static final int MAGIC_NUMBER = 0xA7B9C3D5;

    /**
     * 当前协议版本
     */
    public static final byte VERSION = 1;

    /**
     * 魔数字段长度
     */
    public static final int MAGIC_LENGTH = 4;

    /**
     * 协议版本字段长度
     */
    public static final int VERSION_LENGTH = 1;

    /**
     * 消息类型字段长度
     */
    public static final int MESSAGE_TYPE_LENGTH = 1;

    /**
     * 消息体长度字段长度
     */
    public static final int BODY_LENGTH_FIELD_LENGTH = 4;

    /**
     * 协议头长度：魔数 4 字节 + 版本 1 字节 + 消息类型 1 字节 + 消息长度 4 字节
     */
    public static final int HEADER_LENGTH = MAGIC_LENGTH + VERSION_LENGTH + MESSAGE_TYPE_LENGTH + BODY_LENGTH_FIELD_LENGTH;

    /**
     * 消息体长度字段偏移量：魔数 4 字节 + 版本 1 字节 + 消息类型 1 字节
     */
    public static final int BODY_LENGTH_FIELD_OFFSET = MAGIC_LENGTH + VERSION_LENGTH + MESSAGE_TYPE_LENGTH;

    /**
     * 默认字符集
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private ProtocolConstant() {
    }
}
```

消息数据包定义如下。

文件位置：`src/main/java/io/github/atengk/netty/protocol/MessagePacket.java`

该对象是协议层统一消息模型，入站解码后得到该对象，出站编码时也使用该对象。

```java
package io.github.atengk.netty.protocol;

import lombok.Data;

/**
 * Netty 消息数据包
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class MessagePacket {

    /**
     * 协议版本
     */
    private byte version = ProtocolConstant.VERSION;

    /**
     * 消息类型
     */
    private byte messageType;

    /**
     * 消息体
     */
    private byte[] body;
}
```

业务请求和响应建议作为消息体内容存在，不直接参与协议头解析。示例中使用 JSON 序列化为 `byte[]`。

文件位置：`src/main/java/io/github/atengk/netty/protocol/BusinessRequest.java`

该对象表示客户端发送到服务端的业务请求内容。

```java
package io.github.atengk.netty.protocol;

import lombok.Data;

/**
 * Netty 业务请求
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class BusinessRequest {

    /**
     * 请求ID，用于响应回调匹配
     */
    private String requestId;

    /**
     * 客户端标识
     */
    private String clientId;

    /**
     * 请求内容
     */
    private String content;
}
```

文件位置：`src/main/java/io/github/atengk/netty/protocol/BusinessResponse.java`

该对象表示服务端返回给客户端的业务响应内容。

```java
package io.github.atengk.netty.protocol;

import lombok.Data;

/**
 * Netty 业务响应
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class BusinessResponse {

    /**
     * 请求ID，与请求保持一致
     */
    private String requestId;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private Object data;
}
```

协议设计需要保证客户端和服务端完全一致。字段顺序、字段长度、字符集、消息类型、消息体序列化方式只要有一处不一致，就会导致解码失败或业务数据异常。

### 粘包与拆包处理

TCP 是面向字节流的协议，不保证一次发送对应一次接收。客户端连续发送多个消息时，服务端可能一次读取到多个完整消息，这就是粘包；客户端发送一个较大消息时，服务端可能多次读取后才能得到完整消息，这就是拆包。

本项目使用 `LengthFieldBasedFrameDecoder` 处理粘包与拆包。由于协议中已经包含 `消息长度` 字段，因此可以让 Netty 根据长度字段自动切分完整数据包，再交给业务解码器解析协议内容。

协议格式对应的拆包参数如下：

| 参数                  | 值                               | 说明                                                         |
| --------------------- | -------------------------------- | ------------------------------------------------------------ |
| `maxFrameLength`      | `maxFrameLength + HEADER_LENGTH` | 完整数据包最大长度                                           |
| `lengthFieldOffset`   | `6`                              | 消息长度字段偏移量，魔数 4 字节 + 版本 1 字节 + 消息类型 1 字节 |
| `lengthFieldLength`   | `4`                              | 消息长度字段占 4 字节                                        |
| `lengthAdjustment`    | `0`                              | 消息长度字段只表示消息体长度，不需要修正                     |
| `initialBytesToStrip` | `0`                              | 不剥离协议头，后续解码器继续解析完整协议包                   |

服务端 Pipeline 中配置粘包拆包处理器如下。

文件位置：`src/main/java/io/github/atengk/netty/initializer/NettyServerChannelInitializer.java`

以下代码片段展示粘包拆包处理器在 Pipeline 中的位置，应放在自定义 `MessageDecoder` 之前。

```java
pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
        server.getMaxFrameLength() + ProtocolConstant.HEADER_LENGTH,
        ProtocolConstant.BODY_LENGTH_FIELD_OFFSET,
        ProtocolConstant.BODY_LENGTH_FIELD_LENGTH,
        0,
        0
));

pipeline.addLast("messageDecoder", new MessageDecoder(server.getMaxFrameLength()));
pipeline.addLast("messageEncoder", new MessageEncoder());
```

客户端 Pipeline 也需要使用相同的拆包规则，否则客户端无法正确解析服务端响应。

文件位置：`src/main/java/io/github/atengk/netty/initializer/NettyClientChannelInitializer.java`

```java
pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
        client.getMaxFrameLength() + ProtocolConstant.HEADER_LENGTH,
        ProtocolConstant.BODY_LENGTH_FIELD_OFFSET,
        ProtocolConstant.BODY_LENGTH_FIELD_LENGTH,
        0,
        0
));

pipeline.addLast("messageDecoder", new MessageDecoder(client.getMaxFrameLength()));
pipeline.addLast("messageEncoder", new MessageEncoder());
```

如果协议格式发生变化，例如在协议头中增加序列号、时间戳、压缩标识、加密标识，必须同步调整 `BODY_LENGTH_FIELD_OFFSET` 和解码器读取顺序。

### 编码器实现

编码器用于将业务对象 `MessagePacket` 转换为网络传输所需的 `ByteBuf`。编码器必须严格按照协议字段顺序写入数据，否则接收方无法正确拆包和解码。

编码流程如下：

```text
MessagePacket
   │
   ▼
获取消息体 byte[]
   │
   ▼
写入魔数
   │
   ▼
写入协议版本
   │
   ▼
写入消息类型
   │
   ▼
写入消息体长度
   │
   ▼
写入消息体
   │
   ▼
输出 ByteBuf
```

文件位置：`src/main/java/io/github/atengk/netty/codec/MessageEncoder.java`

该编码器用于将 `MessagePacket` 按自定义协议写入 `ByteBuf`，服务端和客户端可以复用同一个编码器。

```java
package io.github.atengk.netty.codec;

import cn.hutool.core.util.ArrayUtil;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty 消息编码器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class MessageEncoder extends MessageToByteEncoder<MessagePacket> {

    /**
     * 将消息对象编码为 ByteBuf
     *
     * @param ctx 上下文
     * @param msg 消息对象
     * @param out 输出缓冲区
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, MessagePacket msg, ByteBuf out) {
        byte[] body = ArrayUtil.isEmpty(msg.getBody()) ? new byte[0] : msg.getBody();

        out.writeInt(ProtocolConstant.MAGIC_NUMBER);
        out.writeByte(msg.getVersion());
        out.writeByte(msg.getMessageType());
        out.writeInt(body.length);
        out.writeBytes(body);

        log.debug("消息编码完成，messageType={}，bodyLength={}，remoteAddress={}",
                msg.getMessageType(), body.length, ctx.channel().remoteAddress());
    }
}
```

使用编码器时需要注意，`MessageEncoder` 是出站处理器，只有执行 `ctx.writeAndFlush(messagePacket)` 或 `channel.writeAndFlush(messagePacket)` 时才会触发。如果写出的对象不是 `MessagePacket`，该编码器不会处理。

业务响应封装示例如下：

```java
BusinessResponse response = new BusinessResponse();
response.setRequestId("10001");
response.setSuccess(true);
response.setMessage("处理成功");
response.setData("ok");

MessagePacket packet = new MessagePacket();
packet.setVersion(ProtocolConstant.VERSION);
packet.setMessageType(MessageType.BUSINESS_RESPONSE);
packet.setBody(JSONUtil.toJsonStr(response).getBytes(ProtocolConstant.DEFAULT_CHARSET));

ctx.writeAndFlush(packet);
```

### 解码器实现

解码器用于将网络传输过来的 `ByteBuf` 转换为业务可处理的 `MessagePacket`。由于前面已经通过 `LengthFieldBasedFrameDecoder` 完成粘包拆包处理，因此该解码器接收到的通常是一个完整数据包。

解码流程如下：

```text
ByteBuf
   │
   ▼
校验协议头长度
   │
   ▼
读取魔数
   │
   ▼
校验协议版本
   │
   ▼
读取消息类型
   │
   ▼
读取消息体长度
   │
   ▼
读取消息体
   │
   ▼
输出 MessagePacket
```

文件位置：`src/main/java/io/github/atengk/netty/codec/MessageDecoder.java`

该解码器用于校验协议合法性，并将完整数据包转换为 `MessagePacket`。

```java
package io.github.atengk.netty.codec;

import cn.hutool.core.util.ArrayUtil;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Netty 消息解码器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RequiredArgsConstructor
public class MessageDecoder extends ByteToMessageDecoder {

    private final int maxFrameLength;

    /**
     * 将 ByteBuf 解码为消息对象
     *
     * @param ctx 上下文
     * @param in 输入缓冲区
     * @param out 解码结果
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < ProtocolConstant.HEADER_LENGTH) {
            return;
        }

        in.markReaderIndex();

        int magicNumber = in.readInt();
        if (magicNumber != ProtocolConstant.MAGIC_NUMBER) {
            log.warn("非法协议魔数，关闭连接，magicNumber={}，remoteAddress={}",
                    magicNumber, ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        byte version = in.readByte();
        if (version != ProtocolConstant.VERSION) {
            log.warn("协议版本不匹配，version={}，currentVersion={}，remoteAddress={}",
                    version, ProtocolConstant.VERSION, ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        byte messageType = in.readByte();
        int bodyLength = in.readInt();

        if (bodyLength < 0 || bodyLength > maxFrameLength) {
            log.warn("消息体长度非法，bodyLength={}，maxFrameLength={}，remoteAddress={}",
                    bodyLength, maxFrameLength, ctx.channel().remoteAddress());
            ctx.close();
            return;
        }

        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] body = new byte[bodyLength];
        in.readBytes(body);

        MessagePacket packet = new MessagePacket();
        packet.setVersion(version);
        packet.setMessageType(messageType);
        packet.setBody(ArrayUtil.isEmpty(body) ? new byte[0] : body);

        out.add(packet);

        log.debug("消息解码完成，messageType={}，bodyLength={}，remoteAddress={}",
                messageType, bodyLength, ctx.channel().remoteAddress());
    }
}
```

解码器中的关键校验包括：

| 校验项     | 说明                           |
| ---------- | ------------------------------ |
| 协议头长度 | 防止半包场景下提前读取         |
| 魔数       | 快速识别非法协议或误连接       |
| 协议版本   | 防止不同版本客户端和服务端混用 |
| 消息体长度 | 防止异常大包导致内存占用过高   |
| 可读字节数 | 防止消息体未完整到达时越界读取 |

如果前面已经使用 `LengthFieldBasedFrameDecoder`，理论上 `MessageDecoder` 接收到的是完整帧，但仍建议保留长度校验和可读字节校验，提升协议安全性和容错能力。

## 业务处理

业务处理部分用于定义消息类型、请求分发、异常处理和连接管理。协议层只负责把字节流转换为消息对象，业务层负责根据消息类型执行具体处理逻辑，例如心跳响应、客户端注册、业务请求处理和服务端主动推送。

### 消息类型定义

消息类型用于区分不同业务语义。建议使用固定 `byte` 常量定义消息类型，避免使用字符串作为协议层类型字段，减少网络传输体积并提升解析效率。

消息类型规划如下：

| 消息类型            | 值   | 方向             | 说明           |
| ------------------- | ---- | ---------------- | -------------- |
| `HEARTBEAT`         | `1`  | 客户端 -> 服务端 | 心跳请求       |
| `HEARTBEAT_ACK`     | `2`  | 服务端 -> 客户端 | 心跳响应       |
| `REGISTER`          | `3`  | 客户端 -> 服务端 | 客户端注册     |
| `REGISTER_ACK`      | `4`  | 服务端 -> 客户端 | 注册响应       |
| `BUSINESS_REQUEST`  | `10` | 客户端 -> 服务端 | 业务请求       |
| `BUSINESS_RESPONSE` | `11` | 服务端 -> 客户端 | 业务响应       |
| `SERVER_PUSH`       | `20` | 服务端 -> 客户端 | 服务端主动推送 |
| `ERROR`             | `99` | 双向             | 错误消息       |

文件位置：`src/main/java/io/github/atengk/netty/protocol/MessageType.java`

该类统一定义协议消息类型，服务端和客户端必须保持一致。

```java
package io.github.atengk.netty.protocol;

/**
 * Netty 消息类型
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class MessageType {

    /**
     * 心跳请求
     */
    public static final byte HEARTBEAT = 1;

    /**
     * 心跳响应
     */
    public static final byte HEARTBEAT_ACK = 2;

    /**
     * 客户端注册请求
     */
    public static final byte REGISTER = 3;

    /**
     * 客户端注册响应
     */
    public static final byte REGISTER_ACK = 4;

    /**
     * 业务请求
     */
    public static final byte BUSINESS_REQUEST = 10;

    /**
     * 业务响应
     */
    public static final byte BUSINESS_RESPONSE = 11;

    /**
     * 服务端主动推送
     */
    public static final byte SERVER_PUSH = 20;

    /**
     * 错误消息
     */
    public static final byte ERROR = 99;

    private MessageType() {
    }
}
```

错误响应对象如下。

文件位置：`src/main/java/io/github/atengk/netty/protocol/ErrorResponse.java`

该对象用于承载统一错误信息，便于客户端识别异常原因。

```java
package io.github.atengk.netty.protocol;

import lombok.Data;

/**
 * Netty 错误响应
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class ErrorResponse {

    /**
     * 请求ID，可为空
     */
    private String requestId;

    /**
     * 错误编码
     */
    private String code;

    /**
     * 错误消息
     */
    private String message;
}
```

错误编码可以按业务需要扩展，例如：

| 错误编码                   | 说明                 |
| -------------------------- | -------------------- |
| `INVALID_PROTOCOL`         | 协议格式错误         |
| `UNSUPPORTED_MESSAGE_TYPE` | 不支持的消息类型     |
| `INVALID_REQUEST`          | 请求参数错误         |
| `UNAUTHORIZED_CLIENT`      | 客户端未注册或无权限 |
| `SERVER_ERROR`             | 服务端处理异常       |

### 请求分发机制

请求分发机制用于将不同消息类型路由到不同的处理器。推荐使用 `MessageProcessor` 接口定义统一处理规范，再由 `MessageDispatcher` 根据消息类型选择对应处理器。这样可以避免在 Handler 中写大量 `if-else`，也便于新增消息类型。

请求分发流程如下：

```text
NettyServerHandler 收到 MessagePacket
   │
   ▼
调用 MessageDispatcher
   │
   ▼
根据 messageType 查找 MessageProcessor
   │
   ▼
执行具体业务处理
   │
   ▼
返回响应或写出错误消息
```

文件位置：`src/main/java/io/github/atengk/netty/dispatcher/MessageProcessor.java`

该接口定义消息处理器规范，每种消息类型可以对应一个处理器。

```java
package io.github.atengk.netty.dispatcher;

import io.github.atengk.netty.protocol.MessagePacket;
import io.netty.channel.ChannelHandlerContext;

/**
 * Netty 消息处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface MessageProcessor {

    /**
     * 获取支持的消息类型
     *
     * @return 消息类型
     */
    byte messageType();

    /**
     * 处理消息
     *
     * @param ctx 上下文
     * @param packet 消息包
     */
    void process(ChannelHandlerContext ctx, MessagePacket packet);
}
```

心跳处理器如下。

文件位置：`src/main/java/io/github/atengk/netty/dispatcher/HeartbeatMessageProcessor.java`

该处理器负责处理客户端心跳请求，并返回心跳响应。

```java
package io.github.atengk.netty.dispatcher;

import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Netty 心跳消息处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class HeartbeatMessageProcessor implements MessageProcessor {

    /**
     * 获取支持的消息类型
     *
     * @return 消息类型
     */
    @Override
    public byte messageType() {
        return MessageType.HEARTBEAT;
    }

    /**
     * 处理心跳消息
     *
     * @param ctx 上下文
     * @param packet 消息包
     */
    @Override
    public void process(ChannelHandlerContext ctx, MessagePacket packet) {
        MessagePacket response = new MessagePacket();
        response.setVersion(ProtocolConstant.VERSION);
        response.setMessageType(MessageType.HEARTBEAT_ACK);
        response.setBody("pong".getBytes(ProtocolConstant.DEFAULT_CHARSET));

        ctx.writeAndFlush(response);
        log.debug("服务端响应心跳，remoteAddress={}", ctx.channel().remoteAddress());
    }
}
```

业务请求处理器如下。

文件位置：`src/main/java/io/github/atengk/netty/dispatcher/BusinessMessageProcessor.java`

该处理器负责解析业务请求、校验参数、调用业务服务并写回业务响应。

```java
package io.github.atengk.netty.dispatcher;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import io.github.atengk.netty.protocol.BusinessRequest;
import io.github.atengk.netty.protocol.BusinessResponse;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.github.atengk.netty.service.DeviceMessageService;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Netty 业务消息处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessMessageProcessor implements MessageProcessor {

    private final DeviceMessageService deviceMessageService;

    /**
     * 获取支持的消息类型
     *
     * @return 消息类型
     */
    @Override
    public byte messageType() {
        return MessageType.BUSINESS_REQUEST;
    }

    /**
     * 处理业务消息
     *
     * @param ctx 上下文
     * @param packet 消息包
     */
    @Override
    public void process(ChannelHandlerContext ctx, MessagePacket packet) {
        BusinessResponse response = new BusinessResponse();

        try {
            String body = StrUtil.str(packet.getBody(), ProtocolConstant.DEFAULT_CHARSET);
            BusinessRequest request = JSONUtil.toBean(body, BusinessRequest.class);

            response.setRequestId(request.getRequestId());

            if (StrUtil.isBlank(request.getRequestId())) {
                writeBusinessResponse(ctx, response, false, "请求ID不能为空", null);
                return;
            }

            if (StrUtil.isBlank(request.getContent())) {
                writeBusinessResponse(ctx, response, false, "请求内容不能为空", null);
                return;
            }

            String result = deviceMessageService.handleMessage(request.getContent());
            writeBusinessResponse(ctx, response, true, "处理成功", result);

            log.info("业务请求处理完成，requestId={}，clientId={}，remoteAddress={}",
                    request.getRequestId(), request.getClientId(), ctx.channel().remoteAddress());
        } catch (JSONException e) {
            log.error("业务请求JSON解析失败，remoteAddress={}", ctx.channel().remoteAddress(), e);
            writeBusinessResponse(ctx, response, false, "请求格式错误", null);
        } catch (Exception e) {
            log.error("业务请求处理异常，remoteAddress={}", ctx.channel().remoteAddress(), e);
            writeBusinessResponse(ctx, response, false, "服务端处理异常", null);
        }
    }

    /**
     * 写出业务响应
     *
     * @param ctx 上下文
     * @param response 响应对象
     * @param success 是否成功
     * @param message 响应消息
     * @param data 响应数据
     */
    private void writeBusinessResponse(ChannelHandlerContext ctx,
                                       BusinessResponse response,
                                       boolean success,
                                       String message,
                                       Object data) {
        response.setSuccess(success);
        response.setMessage(message);
        response.setData(data);

        MessagePacket responsePacket = new MessagePacket();
        responsePacket.setVersion(ProtocolConstant.VERSION);
        responsePacket.setMessageType(MessageType.BUSINESS_RESPONSE);
        responsePacket.setBody(JSONUtil.toJsonStr(response).getBytes(ProtocolConstant.DEFAULT_CHARSET));

        ctx.writeAndFlush(responsePacket);
    }
}
```

消息分发器如下。

文件位置：`src/main/java/io/github/atengk/netty/dispatcher/MessageDispatcher.java`

该分发器在 Spring 启动时收集所有 `MessageProcessor`，并根据消息类型完成路由。

```java
package io.github.atengk.netty.dispatcher;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.netty.protocol.ErrorResponse;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty 消息分发器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class MessageDispatcher {

    private final Map<Byte, MessageProcessor> processorMap = new ConcurrentHashMap<>();

    /**
     * 初始化消息分发器
     *
     * @param processors 消息处理器列表
     */
    public MessageDispatcher(List<MessageProcessor> processors) {
        if (CollUtil.isEmpty(processors)) {
            log.warn("未加载到任何Netty消息处理器");
            return;
        }

        for (MessageProcessor processor : processors) {
            MessageProcessor oldProcessor = processorMap.put(processor.messageType(), processor);
            if (oldProcessor != null) {
                log.warn("消息类型处理器重复注册，messageType={}", processor.messageType());
            }
        }

        log.info("Netty消息分发器初始化完成，processorCount={}", processorMap.size());
    }

    /**
     * 分发消息
     *
     * @param ctx 上下文
     * @param packet 消息包
     */
    public void dispatch(ChannelHandlerContext ctx, MessagePacket packet) {
        if (packet == null) {
            log.warn("消息包为空，remoteAddress={}", ctx.channel().remoteAddress());
            writeError(ctx, null, "INVALID_REQUEST", "消息包不能为空");
            return;
        }

        MessageProcessor processor = processorMap.get(packet.getMessageType());
        if (processor == null) {
            log.warn("不支持的消息类型，messageType={}，remoteAddress={}",
                    packet.getMessageType(), ctx.channel().remoteAddress());
            writeError(ctx, null, "UNSUPPORTED_MESSAGE_TYPE", "不支持的消息类型");
            return;
        }

        processor.process(ctx, packet);
    }

    /**
     * 写出错误消息
     *
     * @param ctx 上下文
     * @param requestId 请求ID
     * @param code 错误编码
     * @param message 错误消息
     */
    private void writeError(ChannelHandlerContext ctx, String requestId, String code, String message) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setRequestId(requestId);
        errorResponse.setCode(code);
        errorResponse.setMessage(message);

        MessagePacket responsePacket = new MessagePacket();
        responsePacket.setVersion(ProtocolConstant.VERSION);
        responsePacket.setMessageType(MessageType.ERROR);
        responsePacket.setBody(JSONUtil.toJsonStr(errorResponse).getBytes(ProtocolConstant.DEFAULT_CHARSET));

        ctx.writeAndFlush(responsePacket);
    }
}
```

如果前面章节中的 `NettyServerHandler` 已经直接处理了心跳消息，可以保留其中一种方式即可。更推荐本节这种处理器分发方式，因为新增消息类型时只需要新增一个 `MessageProcessor` 实现类，不需要持续修改主 Handler。

### 异常处理机制

异常处理机制用于统一处理协议异常、业务异常、连接异常和未捕获异常。Netty 中异常如果没有被处理，可能导致连接状态异常、资源未释放或日志不完整，因此建议在 Pipeline 末尾添加统一异常 Handler。

异常分类如下：

| 异常类型         | 处理方式                         |
| ---------------- | -------------------------------- |
| 魔数错误         | 记录非法协议日志并关闭连接       |
| 协议版本错误     | 记录版本不兼容日志并关闭连接     |
| 消息长度非法     | 记录异常长度并关闭连接           |
| JSON 解析失败    | 返回业务错误响应，不一定关闭连接 |
| 不支持的消息类型 | 返回错误响应                     |
| 业务处理异常     | 返回业务失败响应并记录异常日志   |
| 网络连接异常     | 关闭 Channel 并清理会话          |
| 空闲超时         | 关闭连接并清理会话               |

文件位置：`src/main/java/io/github/atengk/netty/handler/NettyExceptionHandler.java`

该 Handler 放在 Pipeline 后部，用于兜底处理未被前置 Handler 捕获的异常。

```java
package io.github.atengk.netty.handler;

import io.github.atengk.netty.session.ChannelSessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Netty 异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class NettyExceptionHandler extends ChannelInboundHandlerAdapter {

    private final ChannelSessionManager channelSessionManager;

    /**
     * 捕获异常
     *
     * @param ctx 上下文
     * @param cause 异常信息
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof DecoderException) {
            log.error("Netty协议解码异常，remoteAddress={}", ctx.channel().remoteAddress(), cause);
            closeChannel(ctx);
            return;
        }

        log.error("Netty连接发生未处理异常，remoteAddress={}", ctx.channel().remoteAddress(), cause);
        closeChannel(ctx);
    }

    /**
     * 连接断开时清理资源
     *
     * @param ctx 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelSessionManager.removeByChannel(ctx.channel());
        log.info("连接断开，已清理会话，remoteAddress={}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    /**
     * 关闭连接
     *
     * @param ctx 上下文
     */
    private void closeChannel(ChannelHandlerContext ctx) {
        channelSessionManager.removeByChannel(ctx.channel());
        ctx.close();
    }
}
```

服务端业务 Handler 中也需要处理空闲超时事件。

文件位置：`src/main/java/io/github/atengk/netty/handler/NettyServerHandler.java`

以下代码片段展示空闲超时处理逻辑，超过配置时间未收到客户端数据时关闭连接。

```java
@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
        log.warn("客户端连接空闲超时，关闭连接，remoteAddress={}", ctx.channel().remoteAddress());
        ctx.close();
        return;
    }

    super.userEventTriggered(ctx, evt);
}
```

业务异常建议在具体 `MessageProcessor` 内部捕获并返回明确错误响应，不建议直接抛到 Netty Pipeline。Pipeline 异常处理器更适合处理协议级、连接级和未预期异常。

### 连接管理

连接管理用于维护客户端标识与 Netty Channel 之间的关系。服务端需要知道某个客户端是否在线，并能够根据客户端标识主动发送消息、断开连接或清理异常连接。

连接管理通常包含以下能力：

| 能力       | 说明                                               |
| ---------- | -------------------------------------------------- |
| 添加连接   | 客户端注册或鉴权成功后绑定 `clientId` 和 `Channel` |
| 移除连接   | 客户端断开、异常关闭或心跳超时时清理连接           |
| 查询连接   | 判断客户端是否在线                                 |
| 主动发送   | 服务端根据 `clientId` 向客户端推送消息             |
| 关闭连接   | 服务端主动断开指定客户端                           |
| 统计连接数 | 用于监控当前在线客户端数量                         |

连接会话对象如下。

文件位置：`src/main/java/io/github/atengk/netty/session/ChannelSession.java`

该对象用于保存客户端连接的基础信息。

```java
package io.github.atengk.netty.session;

import io.netty.channel.Channel;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Netty 连接会话
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class ChannelSession {

    /**
     * 客户端标识
     */
    private String clientId;

    /**
     * 客户端连接
     */
    private Channel channel;

    /**
     * 远程地址
     */
    private String remoteAddress;

    /**
     * 连接时间
     */
    private LocalDateTime connectTime;

    /**
     * 最近活跃时间
     */
    private LocalDateTime lastActiveTime;
}
```

连接会话管理器如下。

文件位置：`src/main/java/io/github/atengk/netty/session/ChannelSessionManager.java`

该管理器使用线程安全集合维护客户端连接，支持添加、移除、查询、发送和关闭连接。

```java
package io.github.atengk.netty.session;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.netty.protocol.MessagePacket;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty 连接会话管理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class ChannelSessionManager {

    private final Map<String, ChannelSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, String> channelIdClientIdMap = new ConcurrentHashMap<>();

    /**
     * 添加连接会话
     *
     * @param clientId 客户端标识
     * @param channel 客户端连接
     */
    public void add(String clientId, Channel channel) {
        if (StrUtil.isBlank(clientId) || channel == null) {
            log.warn("添加连接会话失败，clientId或channel为空");
            return;
        }

        ChannelSession oldSession = sessionMap.remove(clientId);
        if (oldSession != null && oldSession.getChannel() != null && oldSession.getChannel().isActive()) {
            oldSession.getChannel().close();
            log.warn("客户端重复连接，已关闭旧连接，clientId={}", clientId);
        }

        ChannelSession session = new ChannelSession();
        session.setClientId(clientId);
        session.setChannel(channel);
        session.setRemoteAddress(String.valueOf(channel.remoteAddress()));
        session.setConnectTime(LocalDateTime.now());
        session.setLastActiveTime(LocalDateTime.now());

        sessionMap.put(clientId, session);
        channelIdClientIdMap.put(channel.id().asLongText(), clientId);

        log.info("添加客户端连接会话，clientId={}，remoteAddress={}，onlineCount={}",
                clientId, channel.remoteAddress(), sessionMap.size());
    }

    /**
     * 更新活跃时间
     *
     * @param clientId 客户端标识
     */
    public void refreshActiveTime(String clientId) {
        if (StrUtil.isBlank(clientId)) {
            return;
        }

        ChannelSession session = sessionMap.get(clientId);
        if (session != null) {
            session.setLastActiveTime(LocalDateTime.now());
        }
    }

    /**
     * 根据客户端标识移除连接
     *
     * @param clientId 客户端标识
     */
    public void remove(String clientId) {
        if (StrUtil.isBlank(clientId)) {
            return;
        }

        ChannelSession session = sessionMap.remove(clientId);
        if (session != null && session.getChannel() != null) {
            channelIdClientIdMap.remove(session.getChannel().id().asLongText());
        }

        log.info("移除客户端连接会话，clientId={}，onlineCount={}", clientId, sessionMap.size());
    }

    /**
     * 根据 Channel 移除连接
     *
     * @param channel 客户端连接
     */
    public void removeByChannel(Channel channel) {
        if (channel == null) {
            return;
        }

        String clientId = channelIdClientIdMap.remove(channel.id().asLongText());
        if (StrUtil.isNotBlank(clientId)) {
            sessionMap.remove(clientId);
            log.info("根据Channel移除客户端连接会话，clientId={}，onlineCount={}", clientId, sessionMap.size());
        }
    }

    /**
     * 判断客户端是否在线
     *
     * @param clientId 客户端标识
     * @return true 表示在线
     */
    public boolean isOnline(String clientId) {
        if (StrUtil.isBlank(clientId)) {
            return false;
        }

        ChannelSession session = sessionMap.get(clientId);
        return session != null && session.getChannel() != null && session.getChannel().isActive();
    }

    /**
     * 获取连接会话
     *
     * @param clientId 客户端标识
     * @return 连接会话
     */
    public Optional<ChannelSession> getSession(String clientId) {
        if (StrUtil.isBlank(clientId)) {
            return Optional.empty();
        }

        return Optional.ofNullable(sessionMap.get(clientId));
    }

    /**
     * 向客户端发送消息
     *
     * @param clientId 客户端标识
     * @param packet 消息包
     * @return true 表示发送成功
     */
    public boolean send(String clientId, MessagePacket packet) {
        ChannelSession session = sessionMap.get(clientId);
        if (session == null || session.getChannel() == null || !session.getChannel().isActive()) {
            log.warn("客户端不在线，无法发送消息，clientId={}", clientId);
            return false;
        }

        session.getChannel().writeAndFlush(packet);
        log.info("向客户端发送消息，clientId={}，messageType={}", clientId, packet.getMessageType());
        return true;
    }

    /**
     * 关闭客户端连接
     *
     * @param clientId 客户端标识
     */
    public void close(String clientId) {
        ChannelSession session = sessionMap.get(clientId);
        if (session == null || session.getChannel() == null) {
            log.warn("客户端连接不存在，clientId={}", clientId);
            return;
        }

        session.getChannel().close();
        remove(clientId);
        log.info("关闭客户端连接，clientId={}", clientId);
    }

    /**
     * 获取在线连接数量
     *
     * @return 在线连接数量
     */
    public int onlineCount() {
        return sessionMap.size();
    }
}
```

客户端注册处理器如下。

文件位置：`src/main/java/io/github/atengk/netty/dispatcher/RegisterMessageProcessor.java`

该处理器用于处理客户端注册请求，将 `clientId` 与当前 Channel 绑定到连接管理器。

```java
package io.github.atengk.netty.dispatcher;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.netty.protocol.BusinessRequest;
import io.github.atengk.netty.protocol.BusinessResponse;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.github.atengk.netty.session.ChannelSessionManager;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Netty 客户端注册消息处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterMessageProcessor implements MessageProcessor {

    private final ChannelSessionManager channelSessionManager;

    /**
     * 获取支持的消息类型
     *
     * @return 消息类型
     */
    @Override
    public byte messageType() {
        return MessageType.REGISTER;
    }

    /**
     * 处理客户端注册
     *
     * @param ctx 上下文
     * @param packet 消息包
     */
    @Override
    public void process(ChannelHandlerContext ctx, MessagePacket packet) {
        String body = StrUtil.str(packet.getBody(), ProtocolConstant.DEFAULT_CHARSET);
        BusinessRequest request = JSONUtil.toBean(body, BusinessRequest.class);

        BusinessResponse response = new BusinessResponse();
        response.setRequestId(request.getRequestId());

        if (StrUtil.isBlank(request.getClientId())) {
            response.setSuccess(false);
            response.setMessage("客户端标识不能为空");
            writeRegisterResponse(ctx, response);
            return;
        }

        channelSessionManager.add(request.getClientId(), ctx.channel());

        response.setSuccess(true);
        response.setMessage("注册成功");
        response.setData(request.getClientId());

        writeRegisterResponse(ctx, response);
        log.info("客户端注册成功，clientId={}，remoteAddress={}", request.getClientId(), ctx.channel().remoteAddress());
    }

    /**
     * 写出注册响应
     *
     * @param ctx 上下文
     * @param response 响应对象
     */
    private void writeRegisterResponse(ChannelHandlerContext ctx, BusinessResponse response) {
        MessagePacket responsePacket = new MessagePacket();
        responsePacket.setVersion(ProtocolConstant.VERSION);
        responsePacket.setMessageType(MessageType.REGISTER_ACK);
        responsePacket.setBody(JSONUtil.toJsonStr(response).getBytes(ProtocolConstant.DEFAULT_CHARSET));

        ctx.writeAndFlush(responsePacket);
    }
}
```

服务端主动推送示例如下。

```java
BusinessResponse pushBody = new BusinessResponse();
pushBody.setSuccess(true);
pushBody.setMessage("服务端推送消息");
pushBody.setData("设备指令内容");

MessagePacket pushPacket = new MessagePacket();
pushPacket.setVersion(ProtocolConstant.VERSION);
pushPacket.setMessageType(MessageType.SERVER_PUSH);
pushPacket.setBody(JSONUtil.toJsonStr(pushBody).getBytes(ProtocolConstant.DEFAULT_CHARSET));

boolean success = channelSessionManager.send("device-001", pushPacket);
```

连接管理需要注意以下几点：

| 注意事项                 | 说明                                                      |
| ------------------------ | --------------------------------------------------------- |
| 注册后再绑定连接         | 不建议在 `channelActive` 阶段直接绑定业务客户端标识       |
| 重复连接要处理旧连接     | 同一个 `clientId` 重复连接时应关闭旧 Channel 或拒绝新连接 |
| 连接断开必须清理会话     | 在 `channelInactive` 或异常处理中调用 `removeByChannel`   |
| 推送前检查连接状态       | 使用 `isActive()` 判断 Channel 是否可用                   |
| 会话集合必须线程安全     | 使用 `ConcurrentHashMap` 管理连接                         |
| 不要长期保存无效 Channel | 心跳超时、异常关闭、客户端断开时及时移除                  |

完成协议设计和业务处理后，服务端已经具备自定义协议解析、粘包拆包、消息分发、业务响应、异常兜底和连接管理能力。后续可以继续补充接口测试、客户端通信测试、单元测试、压测和日志运维内容。

## 接口与测试

接口与测试部分用于验证 Netty 服务是否能够正常启动、接收客户端连接、完成协议编解码、处理业务请求并返回响应。由于本项目使用 TCP 自定义协议，不适合直接使用普通 HTTP 工具测试，应优先使用 Netty 客户端、单元测试和压测客户端进行验证。

### 启动验证

启动验证用于确认 Spring Boot 应用和 Netty 服务端是否正常启动，并检查 Netty 端口是否已经监听。完成前面服务端生命周期配置后，Spring Boot 启动时会自动启动 Netty 服务。

启动前需要确认配置文件中 Netty 服务端口。

文件位置：`src/main/resources/application.yml`

```yaml
netty:
  server:
    # Netty 服务端监听端口
    port: 9000

    # Boss 线程数，负责接收连接
    boss-threads: 1

    # Worker 线程数，0 表示使用 Netty 默认线程数
    worker-threads: 0

    # 最大帧长度，防止异常大包占用内存
    max-frame-length: 1048576

    # 读空闲时间，单位秒
    reader-idle-seconds: 60

    # 写空闲时间，单位秒
    writer-idle-seconds: 30

    # 读写空闲时间，单位秒
    all-idle-seconds: 90
```

本地启动命令如下：

```bash
# 编译项目
mvn clean compile

# 启动 Spring Boot 应用
mvn spring-boot:run
```

如果项目已经打包为 Jar，可以使用以下命令启动：

```bash
# 启动可执行 Jar
java -jar target/springboot3-netty-demo-1.0.0.jar
```

启动成功后，控制台应出现类似日志：

```text
Netty服务启动成功，port=9000
Started NettyApplication in 2.315 seconds
```

检查端口监听状态：

```bash
# 查看 Netty 端口是否监听
ss -lntp | grep 9000

# 如果系统支持 lsof，也可以查看端口占用进程
lsof -i :9000
```

`ss -lntp` 用于查看当前 TCP 监听端口，`grep 9000` 用于过滤 Netty 服务端口。`lsof -i :9000` 可以查看端口被哪个进程占用，适合排查端口冲突问题。

如果启动失败，应优先检查以下内容：

| 检查项               | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| 端口是否被占用       | 使用 `ss` 或 `lsof` 检查 `9000` 端口                         |
| JDK 版本是否正确     | Spring Boot 3 要求 Java 17 或更高版本                        |
| Netty 依赖是否冲突   | 使用 `mvn dependency:tree -Dincludes=io.netty` 查看          |
| 配置文件是否正确     | 检查 `application.yml` 缩进和配置路径                        |
| 生命周期是否重复启动 | 检查是否同时使用了 `ApplicationRunner` 和 `SmartLifecycle` 启动服务 |

### 客户端通信测试

客户端通信测试用于验证客户端能否连接服务端、发送自定义协议消息、接收服务端响应并完成回调处理。由于本项目使用二进制协议，不建议使用 `curl`、浏览器或普通 HTTP 客户端测试。

测试流程如下：

```text
启动服务端
   │
   ▼
启动 Netty 客户端
   │
   ▼
客户端注册连接
   │
   ▼
客户端发送业务请求
   │
   ▼
服务端处理消息
   │
   ▼
服务端返回业务响应
   │
   ▼
客户端根据 requestId 完成响应回调
```

文件位置：`src/test/java/io/github/atengk/netty/NettyClientCommunicationTests.java`

该测试类用于启动 Spring Boot 测试上下文，通过客户端发送业务消息并断言服务端响应。

```java
package io.github.atengk.netty;

import io.github.atengk.netty.client.NettyClient;
import io.github.atengk.netty.protocol.BusinessResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

/**
 * Netty 客户端通信测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@SpringBootTest
class NettyClientCommunicationTests {

    @Autowired
    private NettyClient nettyClient;

    /**
     * 测试客户端发送业务请求
     *
     * @throws Exception 测试异常
     */
    @Test
    void testSendBusinessMessage() throws Exception {
        if (!nettyClient.isActive()) {
            nettyClient.connect();
        }

        BusinessResponse response = nettyClient.sendAsync("hello springboot3 netty")
                .get(10, TimeUnit.SECONDS);

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.getSuccess());
        Assertions.assertEquals("处理成功", response.getMessage());

        log.info("客户端通信测试完成，response={}", response);
    }
}
```

如果测试环境不希望客户端自动启动，可以增加测试配置文件。

文件位置：`src/test/resources/application-test.yml`

```yaml
netty:
  client:
    # 测试环境连接本地服务端
    host: 127.0.0.1
    port: 9000

    # 响应超时时间，单位秒
    response-timeout-seconds: 10

    # 是否开启重连
    reconnect-enabled: false
```

执行测试命令如下：

```bash
# 执行指定测试类
mvn test -Dtest=NettyClientCommunicationTests
```

如果客户端通信测试失败，重点检查以下问题：

| 问题         | 排查方向                                               |
| ------------ | ------------------------------------------------------ |
| 连接被拒绝   | 服务端未启动或端口配置错误                             |
| 请求超时     | 服务端未返回响应、`requestId` 未匹配或客户端回调未完成 |
| 解码失败     | 客户端和服务端协议字段顺序不一致                       |
| 响应为空     | 服务端业务处理器未写出 `MessagePacket`                 |
| 连接立即断开 | 魔数、版本号、消息长度校验失败                         |

### 单元测试

单元测试用于验证协议编解码、消息分发和连接管理等核心逻辑。Netty 提供 `EmbeddedChannel`，可以在不启动真实端口的情况下测试 Pipeline 中的编码器、解码器和 Handler。

建议优先覆盖以下内容：

| 测试对象                        | 测试内容                                    |
| ------------------------------- | ------------------------------------------- |
| `MessageEncoder`                | 验证消息对象是否按协议格式写入 ByteBuf      |
| `MessageDecoder`                | 验证 ByteBuf 是否能正确解码为 MessagePacket |
| `MessageDispatcher`             | 验证不同消息类型是否路由到正确处理器        |
| `ChannelSessionManager`         | 验证连接添加、移除、查询、发送逻辑          |
| `ClientResponseCallbackManager` | 验证响应回调完成和超时清理                  |

编解码单元测试如下。

文件位置：`src/test/java/io/github/atengk/netty/codec/MessageCodecTests.java`

该测试类使用 `EmbeddedChannel` 验证自定义协议编码器和解码器是否符合数据包格式。

```java
package io.github.atengk.netty.codec;

import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Netty 消息编解码测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class MessageCodecTests {

    /**
     * 测试消息编码器
     */
    @Test
    void testEncode() {
        EmbeddedChannel channel = new EmbeddedChannel(new MessageEncoder());

        MessagePacket packet = new MessagePacket();
        packet.setVersion(ProtocolConstant.VERSION);
        packet.setMessageType(MessageType.BUSINESS_REQUEST);
        packet.setBody("hello".getBytes(ProtocolConstant.DEFAULT_CHARSET));

        Assertions.assertTrue(channel.writeOutbound(packet));

        ByteBuf byteBuf = channel.readOutbound();
        Assertions.assertNotNull(byteBuf);
        Assertions.assertEquals(ProtocolConstant.MAGIC_NUMBER, byteBuf.readInt());
        Assertions.assertEquals(ProtocolConstant.VERSION, byteBuf.readByte());
        Assertions.assertEquals(MessageType.BUSINESS_REQUEST, byteBuf.readByte());
        Assertions.assertEquals(5, byteBuf.readInt());

        byte[] body = new byte[5];
        byteBuf.readBytes(body);
        Assertions.assertEquals("hello", new String(body, ProtocolConstant.DEFAULT_CHARSET));

        byteBuf.release();
        channel.finishAndReleaseAll();
    }

    /**
     * 测试消息解码器
     */
    @Test
    void testDecode() {
        EmbeddedChannel channel = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(
                        1024 + ProtocolConstant.HEADER_LENGTH,
                        ProtocolConstant.BODY_LENGTH_FIELD_OFFSET,
                        ProtocolConstant.BODY_LENGTH_FIELD_LENGTH,
                        0,
                        0
                ),
                new MessageDecoder(1024)
        );

        byte[] body = "hello".getBytes(ProtocolConstant.DEFAULT_CHARSET);
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt(ProtocolConstant.MAGIC_NUMBER);
        byteBuf.writeByte(ProtocolConstant.VERSION);
        byteBuf.writeByte(MessageType.BUSINESS_REQUEST);
        byteBuf.writeInt(body.length);
        byteBuf.writeBytes(body);

        Assertions.assertTrue(channel.writeInbound(byteBuf));

        MessagePacket packet = channel.readInbound();
        Assertions.assertNotNull(packet);
        Assertions.assertEquals(ProtocolConstant.VERSION, packet.getVersion());
        Assertions.assertEquals(MessageType.BUSINESS_REQUEST, packet.getMessageType());
        Assertions.assertEquals("hello", new String(packet.getBody(), ProtocolConstant.DEFAULT_CHARSET));

        channel.finishAndReleaseAll();
    }
}
```

连接管理单元测试如下。

文件位置：`src/test/java/io/github/atengk/netty/session/ChannelSessionManagerTests.java`

该测试类用于验证客户端连接会话的添加、查询和移除逻辑。

```java
package io.github.atengk.netty.session;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Netty 连接会话管理测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class ChannelSessionManagerTests {

    /**
     * 测试添加和移除连接会话
     */
    @Test
    void testAddAndRemoveSession() {
        ChannelSessionManager manager = new ChannelSessionManager();
        EmbeddedChannel channel = new EmbeddedChannel();

        manager.add("client-001", channel);

        Assertions.assertTrue(manager.isOnline("client-001"));
        Assertions.assertEquals(1, manager.onlineCount());

        manager.remove("client-001");

        Assertions.assertFalse(manager.isOnline("client-001"));
        Assertions.assertEquals(0, manager.onlineCount());

        channel.finishAndReleaseAll();
    }

    /**
     * 测试根据 Channel 移除连接会话
     */
    @Test
    void testRemoveByChannel() {
        ChannelSessionManager manager = new ChannelSessionManager();
        EmbeddedChannel channel = new EmbeddedChannel();

        manager.add("client-002", channel);
        manager.removeByChannel(channel);

        Assertions.assertFalse(manager.isOnline("client-002"));
        Assertions.assertEquals(0, manager.onlineCount());

        channel.finishAndReleaseAll();
    }
}
```

执行全部测试：

```bash
# 执行全部单元测试
mvn test
```

只执行编解码测试：

```bash
# 执行指定测试类
mvn test -Dtest=MessageCodecTests
```

单元测试应尽量避免依赖真实网络端口。协议、编解码、连接管理、回调管理优先使用 `EmbeddedChannel` 和普通对象测试；完整通信流程再使用集成测试验证。

### 压力测试

压力测试用于评估 Netty 服务在高并发连接、大量请求、长连接心跳和大包数据场景下的稳定性。由于本项目是 TCP 自定义协议，不能直接使用 HTTP 压测工具，需要编写专用压测客户端。

压测重点指标如下：

| 指标             | 说明                               |
| ---------------- | ---------------------------------- |
| 并发连接数       | 同时在线客户端数量                 |
| 请求吞吐量       | 每秒处理请求数                     |
| 平均响应时间     | 请求发出到收到响应的平均耗时       |
| P95/P99 响应时间 | 高延迟请求分布                     |
| 失败请求数       | 连接失败、响应超时、解码失败等数量 |
| JVM 内存         | 堆内存、直接内存、GC 情况          |
| CPU 使用率       | EventLoop 和业务线程池是否过载     |
| 连接稳定性       | 长连接是否频繁断开                 |

文件位置：`src/test/java/io/github/atengk/netty/pressure/NettyPressureClient.java`

该压测客户端用于创建多个并发任务，持续向 Netty 服务发送业务请求，并统计成功数、失败数和耗时。

```java
package io.github.atengk.netty.pressure;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.netty.codec.MessageDecoder;
import io.github.atengk.netty.codec.MessageEncoder;
import io.github.atengk.netty.protocol.BusinessRequest;
import io.github.atengk.netty.protocol.MessagePacket;
import io.github.atengk.netty.protocol.MessageType;
import io.github.atengk.netty.protocol.ProtocolConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LongAdder;

/**
 * Netty TCP 压测客户端
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class NettyPressureClient {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9000;
    private static final int CLIENT_COUNT = 100;
    private static final int REQUEST_PER_CLIENT = 100;

    private static final LongAdder SUCCESS_COUNT = new LongAdder();
    private static final LongAdder FAIL_COUNT = new LongAdder();

    /**
     * 启动压测客户端
     *
     * @param args 启动参数
     * @throws Exception 压测异常
     */
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        CountDownLatch latch = new CountDownLatch(CLIENT_COUNT * REQUEST_PER_CLIENT);
        StopWatch stopWatch = new StopWatch("netty-pressure-test");

        stopWatch.start("pressure-test");

        try {
            for (int i = 0; i < CLIENT_COUNT; i++) {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .handler(new ChannelInitializer<SocketChannel>() {

                            @Override
                            protected void initChannel(SocketChannel channel) {
                                channel.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                                        1024 * 1024 + ProtocolConstant.HEADER_LENGTH,
                                        ProtocolConstant.BODY_LENGTH_FIELD_OFFSET,
                                        ProtocolConstant.BODY_LENGTH_FIELD_LENGTH,
                                        0,
                                        0
                                ));
                                channel.pipeline().addLast("messageDecoder", new MessageDecoder(1024 * 1024));
                                channel.pipeline().addLast("messageEncoder", new MessageEncoder());
                                channel.pipeline().addLast("pressureHandler", new PressureClientHandler(latch));
                            }
                        });

                Channel channel = bootstrap.connect(HOST, PORT).sync().channel();

                for (int j = 0; j < REQUEST_PER_CLIENT; j++) {
                    BusinessRequest request = new BusinessRequest();
                    request.setRequestId(IdUtil.fastSimpleUUID());
                    request.setClientId("pressure-client-" + i);
                    request.setContent("pressure-message-" + j);

                    MessagePacket packet = new MessagePacket();
                    packet.setVersion(ProtocolConstant.VERSION);
                    packet.setMessageType(MessageType.BUSINESS_REQUEST);
                    packet.setBody(JSONUtil.toJsonStr(request).getBytes(ProtocolConstant.DEFAULT_CHARSET));

                    channel.writeAndFlush(packet).addListener(future -> {
                        if (!future.isSuccess()) {
                            FAIL_COUNT.increment();
                            latch.countDown();
                        }
                    });
                }
            }

            latch.await();
            stopWatch.stop();

            log.info("压测完成，clientCount={}，requestPerClient={}，successCount={}，failCount={}，耗时={}ms",
                    CLIENT_COUNT,
                    REQUEST_PER_CLIENT,
                    SUCCESS_COUNT.sum(),
                    FAIL_COUNT.sum(),
                    stopWatch.getTotalTimeMillis());
        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * 压测客户端响应处理器
     *
     * @author Ateng
     * @since 2026-05-06
     */
    private static class PressureClientHandler extends SimpleChannelInboundHandler<MessagePacket> {

        private final CountDownLatch latch;

        private PressureClientHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        /**
         * 读取服务端响应
         *
         * @param ctx 上下文
         * @param packet 消息包
         */
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MessagePacket packet) {
            if (packet.getMessageType() == MessageType.BUSINESS_RESPONSE) {
                SUCCESS_COUNT.increment();
            } else {
                FAIL_COUNT.increment();
            }

            latch.countDown();
        }

        /**
         * 处理异常
         *
         * @param ctx 上下文
         * @param cause 异常信息
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            FAIL_COUNT.increment();
            latch.countDown();
            ctx.close();
        }
    }
}
```

压测执行方式如下：

```bash
# 先启动服务端
mvn spring-boot:run

# 单独运行压测客户端 main 方法
mvn test-compile exec:java \
  -Dexec.mainClass=io.github.atengk.netty.pressure.NettyPressureClient \
  -Dexec.classpathScope=test
```

如果没有配置 `exec-maven-plugin`，可以在 IDE 中直接运行 `NettyPressureClient.main()`。

压测时建议同步观察系统指标：

```bash
# 查看 Java 进程
jps -l

# 查看 JVM GC 情况，将 <pid> 替换为实际进程ID
jstat -gcutil <pid> 1000

# 查看进程线程情况
top -Hp <pid>

# 查看 TCP 连接数量
ss -ant | grep 9000 | wc -l
```

压测结果需要结合连接数、吞吐量、响应时间、失败数、CPU、内存和 GC 一起判断。不要只看单次成功日志，应至少进行多轮压测，并分别覆盖短连接、高并发长连接、小包高频、大包低频、服务端重启后客户端重连等场景。

## 日志与运维

日志与运维部分用于保障 Netty 服务在生产环境中可观察、可排查、可关闭。Netty 服务通常处理大量长连接，如果缺少连接日志、异常日志、运行状态监控和优雅停机能力，问题发生时很难定位。

### 连接日志

连接日志用于记录客户端接入、注册、断开、心跳和连接数量变化。建议连接建立和断开使用 `info` 级别，心跳使用 `debug` 级别，异常连接和非法协议使用 `warn` 或 `error` 级别。

推荐日志事件如下：

| 事件       | 日志级别 | 说明                           |
| ---------- | -------- | ------------------------------ |
| 服务启动   | `info`   | 记录监听端口和线程配置         |
| 客户端连接 | `info`   | 记录远程地址                   |
| 客户端注册 | `info`   | 记录 `clientId` 和连接数       |
| 心跳请求   | `debug`  | 高频日志，避免默认输出过多     |
| 连接断开   | `info`   | 记录远程地址和当前连接数       |
| 重复连接   | `warn`   | 同一客户端重复注册或挤掉旧连接 |
| 空闲超时   | `warn`   | 记录超时关闭连接               |
| 非法协议   | `warn`   | 记录魔数、版本或长度异常       |

文件位置：`src/main/resources/logback-spring.xml`

该日志配置按控制台和文件输出日志，并单独保留 Netty 相关日志。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 日志目录，可通过启动参数覆盖：-DLOG_PATH=/data/logs/springboot3-netty-demo -->
    <property name="LOG_PATH" value="${LOG_PATH:-logs}"/>
    <property name="APP_NAME" value="springboot3-netty-demo"/>

    <!-- 控制台日志 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 应用滚动日志 -->
    <appender name="APP_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>200MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
    </appender>

    <!-- Netty 专用日志 -->
    <appender name="NETTY_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/netty.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/netty.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>200MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
    </appender>

    <!-- Netty 业务包日志 -->
    <logger name="io.github.atengk.netty" level="INFO" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="NETTY_FILE"/>
    </logger>

    <!-- 根日志 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="APP_FILE"/>
    </root>
</configuration>
```

生产环境不建议默认开启心跳 `debug` 日志，否则长连接数量较大时日志量会快速增长。需要排查心跳问题时，可以临时将 `io.github.atengk.netty` 日志级别调整为 `DEBUG`。

### 异常日志

异常日志用于记录协议异常、连接异常、业务异常和系统异常。日志中应包含远程地址、客户端标识、消息类型、请求 ID、异常堆栈等关键信息，便于快速定位问题。

建议异常日志字段如下：

| 字段            | 说明                        |
| --------------- | --------------------------- |
| `remoteAddress` | 客户端远程地址              |
| `clientId`      | 已注册客户端标识            |
| `requestId`     | 请求 ID，便于关联客户端日志 |
| `messageType`   | 消息类型                    |
| `bodyLength`    | 消息体长度                  |
| `exception`     | 异常堆栈                    |
| `onlineCount`   | 当前在线连接数              |

异常处理示例：

```java
log.error("业务请求处理异常，requestId={}，messageType={}，remoteAddress={}",
        request.getRequestId(), packet.getMessageType(), ctx.channel().remoteAddress(), e);
```

协议异常示例：

```java
log.warn("非法协议魔数，关闭连接，magicNumber={}，remoteAddress={}",
        magicNumber, ctx.channel().remoteAddress());
```

连接异常示例：

```java
log.error("Netty连接发生未处理异常，remoteAddress={}",
        ctx.channel().remoteAddress(), cause);
```

异常日志需要注意以下几点：

| 注意事项             | 说明                           |
| -------------------- | ------------------------------ |
| 不要吞异常           | 至少记录错误级别日志           |
| 不要打印过大的消息体 | 大包场景只记录长度和摘要       |
| 高频异常要限流       | 非法连接过多时避免日志刷爆磁盘 |
| 业务异常返回明确错误 | 客户端需要知道失败原因         |
| 协议异常及时关闭连接 | 防止非法客户端持续占用连接     |

如果需要记录消息体摘要，可以使用 Hutool 计算摘要，避免直接输出完整大包内容。

```java
String bodyText = StrUtil.str(packet.getBody(), ProtocolConstant.DEFAULT_CHARSET);
String bodyDigest = cn.hutool.crypto.SecureUtil.md5(bodyText);
log.warn("业务消息处理失败，requestId={}，bodyDigest={}", requestId, bodyDigest);
```

### 运行状态监控

运行状态监控用于观察 Netty 服务是否正常运行，包括在线连接数、服务端端口、业务线程池状态、回调积压数量等。可以通过 Spring Boot Actuator、定时日志或自定义接口暴露监控信息。

如果项目需要 HTTP 监控接口，可以增加以下依赖。

文件位置：`pom.xml`

```xml
<!-- Spring Boot Web，用于暴露监控接口 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Boot Actuator，用于应用健康检查和指标监控 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

监控配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 根据实际安全策略开放端点
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

自定义运行状态对象如下。

文件位置：`src/main/java/io/github/atengk/netty/monitor/NettyStatusVO.java`

该对象用于返回 Netty 服务运行状态。

```java
package io.github.atengk.netty.monitor;

import lombok.Builder;
import lombok.Data;

/**
 * Netty 运行状态响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
public class NettyStatusVO {

    /**
     * 服务端是否运行
     */
    private Boolean running;

    /**
     * 在线连接数量
     */
    private Integer onlineCount;

    /**
     * 业务线程池活跃线程数
     */
    private Integer businessActiveThreads;

    /**
     * 业务线程池队列积压数量
     */
    private Integer businessQueueSize;

    /**
     * 业务线程池已完成任务数
     */
    private Long businessCompletedTaskCount;
}
```

自定义监控接口如下。

文件位置：`src/main/java/io/github/atengk/netty/monitor/NettyMonitorController.java`

该接口用于查看 Netty 服务运行状态，适合本地运维、健康巡检或接入监控系统。

```java
package io.github.atengk.netty.monitor;

import io.github.atengk.netty.server.NettyServer;
import io.github.atengk.netty.session.ChannelSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Netty 运行状态监控接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
public class NettyMonitorController {

    private final NettyServer nettyServer;
    private final ChannelSessionManager channelSessionManager;

    @Qualifier("nettyBusinessExecutor")
    private final ThreadPoolExecutor nettyBusinessExecutor;

    /**
     * 查看 Netty 运行状态
     *
     * @return 运行状态
     */
    @GetMapping("/netty/status")
    public NettyStatusVO status() {
        return NettyStatusVO.builder()
                .running(nettyServer.isRunning())
                .onlineCount(channelSessionManager.onlineCount())
                .businessActiveThreads(nettyBusinessExecutor.getActiveCount())
                .businessQueueSize(nettyBusinessExecutor.getQueue().size())
                .businessCompletedTaskCount(nettyBusinessExecutor.getCompletedTaskCount())
                .build();
    }
}
```

接口验证命令如下：

```bash
# 查看自定义 Netty 运行状态
curl http://127.0.0.1:8080/netty/status

# 查看 Spring Boot 健康状态
curl http://127.0.0.1:8080/actuator/health
```

响应示例：

```json
{
  "running": true,
  "onlineCount": 128,
  "businessActiveThreads": 4,
  "businessQueueSize": 0,
  "businessCompletedTaskCount": 10240
}
```

如果项目不希望引入 Web 依赖，也可以使用定时任务输出状态日志。

文件位置：`src/main/java/io/github/atengk/netty/monitor/NettyStatusLogger.java`

该组件用于周期性输出 Netty 运行状态日志。

```java
package io.github.atengk.netty.monitor;

import io.github.atengk.netty.server.NettyServer;
import io.github.atengk.netty.session.ChannelSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Netty 运行状态日志输出器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyStatusLogger {

    private final NettyServer nettyServer;
    private final ChannelSessionManager channelSessionManager;

    @Qualifier("nettyBusinessExecutor")
    private final ThreadPoolExecutor nettyBusinessExecutor;

    /**
     * 定时输出 Netty 运行状态
     */
    @Scheduled(fixedDelay = 60000)
    public void logStatus() {
        log.info("Netty运行状态，running={}，onlineCount={}，activeThreads={}，queueSize={}，completedTaskCount={}",
                nettyServer.isRunning(),
                channelSessionManager.onlineCount(),
                nettyBusinessExecutor.getActiveCount(),
                nettyBusinessExecutor.getQueue().size(),
                nettyBusinessExecutor.getCompletedTaskCount());
    }
}
```

如果使用定时任务，需要在启动类上启用调度。

文件位置：`src/main/java/io/github/atengk/netty/NettyApplication.java`

```java
package io.github.atengk.netty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Netty 应用启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@EnableScheduling
@SpringBootApplication
public class NettyApplication {

    /**
     * 应用入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(NettyApplication.class, args);
    }
}
```

### 优雅停机

优雅停机用于在应用关闭时停止接收新连接、关闭已有 Channel、失败未完成请求、释放业务线程池和 Netty EventLoopGroup。Spring Boot 中可以通过 `SmartLifecycle`、`DisposableBean` 或 `@PreDestroy` 实现资源释放，推荐前面章节中的 `SmartLifecycle` 管理 Netty 服务。

优雅停机顺序如下：

```text
收到关闭信号
   │
   ▼
停止接收新连接
   │
   ▼
关闭服务端 Channel
   │
   ▼
关闭客户端连接
   │
   ▼
清理连接会话
   │
   ▼
释放 workerGroup
   │
   ▼
释放 bossGroup
   │
   ▼
关闭业务线程池
```

Spring Boot 优雅停机配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 开启 Spring Boot Web 优雅停机；如果未引入 Web，该配置不会影响 Netty 关闭逻辑
  shutdown: graceful

spring:
  lifecycle:
    # 等待生命周期组件停止的最长时间
    timeout-per-shutdown-phase: 30s
```

业务线程池关闭配置如下。

文件位置：`src/main/java/io/github/atengk/netty/config/NettyBusinessExecutorDestroy.java`

该组件用于在 Spring 容器关闭时释放业务线程池，避免任务线程残留。

```java
package io.github.atengk.netty.config;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Netty 业务线程池销毁器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyBusinessExecutorDestroy {

    @Qualifier("nettyBusinessExecutor")
    private final ThreadPoolExecutor nettyBusinessExecutor;

    /**
     * 关闭业务线程池
     */
    @PreDestroy
    public void destroy() {
        nettyBusinessExecutor.shutdown();
        try {
            if (!nettyBusinessExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                nettyBusinessExecutor.shutdownNow();
                log.warn("Netty业务线程池未及时结束，已强制关闭");
            } else {
                log.info("Netty业务线程池已优雅关闭");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            nettyBusinessExecutor.shutdownNow();
            log.error("Netty业务线程池关闭被中断", e);
        }
    }
}
```

Linux 下停止服务建议使用 `SIGTERM`，不要直接使用 `kill -9`。

```bash
# 查看 Java 进程
jps -l

# 优雅停止，将 <pid> 替换为实际进程ID
kill -15 <pid>

# 不建议使用，除非进程无法正常退出
kill -9 <pid>
```

`kill -15` 会向进程发送终止信号，Spring Boot 可以接收到关闭事件并执行生命周期停止逻辑。`kill -9` 会强制杀死进程，应用没有机会释放 Netty 端口、关闭连接和输出停机日志。

## 部署运行

部署运行部分用于说明生产环境配置、打包方式、启动命令和常见问题处理。Netty 服务部署时需要同时关注 Spring Boot 应用端口和 Netty TCP 端口，并确保防火墙、安全组、系统连接数限制和 JVM 参数满足长连接场景要求。

### 配置文件说明

生产环境建议使用独立配置文件，例如 `application-prod.yml`，并通过启动参数指定环境。Netty 服务端口、线程数、心跳时间、最大帧长度、日志路径等参数应按环境配置，不建议写死在代码中。

文件位置：`src/main/resources/application-prod.yml`

```yaml
server:
  # Spring Boot HTTP 端口，用于监控接口或 Actuator
  port: 8080

  # Web 优雅停机
  shutdown: graceful

spring:
  application:
    name: springboot3-netty-demo

  lifecycle:
    # 应用关闭时等待生命周期组件停止
    timeout-per-shutdown-phase: 30s

netty:
  server:
    # Netty TCP 服务端口
    port: 9000

    # Boss 线程数，负责接收连接
    boss-threads: 1

    # Worker 线程数，生产环境可结合 CPU 和压测结果设置
    worker-threads: 0

    # 服务端连接队列
    backlog: 2048

    # 开启 TCP keepalive
    keep-alive: true

    # 开启 TCP_NODELAY，降低小包延迟
    tcp-no-delay: true

    # 最大帧长度，按协议最大消息大小设置
    max-frame-length: 1048576

    # 读空闲时间，单位秒，通常大于客户端心跳间隔
    reader-idle-seconds: 60

    # 写空闲时间，单位秒
    writer-idle-seconds: 30

    # 读写空闲时间，单位秒
    all-idle-seconds: 90

    # 业务线程池核心线程数
    business-core-threads: 16

    # 业务线程池最大线程数
    business-max-threads: 64

    # 业务线程池队列容量
    business-queue-capacity: 5000

logging:
  file:
    # 日志目录，也可使用 -DLOG_PATH 覆盖
    path: /data/logs/springboot3-netty-demo

management:
  endpoints:
    web:
      exposure:
        # 生产环境按安全策略开放
        include: health,info,metrics
```

生产环境建议重点检查以下配置：

| 配置                               | 说明                                 |
| ---------------------------------- | ------------------------------------ |
| `server.port`                      | Spring Boot HTTP 端口，通常用于监控  |
| `netty.server.port`                | Netty TCP 端口，业务客户端连接该端口 |
| `netty.server.backlog`             | 高并发连接场景适当调大               |
| `netty.server.max-frame-length`    | 必须限制最大包大小                   |
| `netty.server.reader-idle-seconds` | 应大于客户端心跳发送间隔             |
| `business-*`                       | 根据业务耗时和压测结果调整           |
| `logging.file.path`                | 日志目录需要有写入权限               |

系统参数也需要关注文件句柄数量：

```bash
# 查看当前文件句柄限制
ulimit -n

# 临时调整当前会话文件句柄限制
ulimit -n 65535
```

如果是长连接服务，生产环境文件句柄限制过低会导致连接数无法提升。

### 打包方式

项目推荐打包为 Spring Boot 可执行 Jar。打包前需要先执行测试，确保协议编解码、连接管理和客户端通信测试通过。

打包命令如下：

```bash
# 清理、测试并打包
mvn clean package

# 如果需要跳过测试
mvn clean package -DskipTests
```

打包完成后，目标文件位于：

```text
target/springboot3-netty-demo-1.0.0.jar
```

验证 Jar 是否生成：

```bash
# 查看 target 目录
ls -lh target/

# 查看 Jar 包内容
jar tf target/springboot3-netty-demo-1.0.0.jar | head
```

生产环境建议保留以下目录结构：

```text
/opt/springboot3-netty-demo
├── app
│   └── springboot3-netty-demo-1.0.0.jar
├── config
│   └── application-prod.yml
├── logs
│   ├── springboot3-netty-demo.log
│   └── netty.log
└── scripts
    ├── start.sh
    ├── stop.sh
    └── status.sh
```

目录说明如下：

| 目录      | 说明                         |
| --------- | ---------------------------- |
| `app`     | 存放应用 Jar                 |
| `config`  | 存放外部化配置文件           |
| `logs`    | 存放运行日志                 |
| `scripts` | 存放启动、停止、状态检查脚本 |

### 启动命令

生产环境启动时建议明确指定运行环境、配置文件路径、日志路径、JVM 内存和直接内存参数。Netty 使用直接内存较多，应根据业务消息大小、连接数量和压测结果设置 `MaxDirectMemorySize`。

基础启动命令如下：

```bash
java \
  -Xms512m \
  -Xmx512m \
  -XX:MaxDirectMemorySize=512m \
  -DLOG_PATH=/opt/springboot3-netty-demo/logs \
  -jar /opt/springboot3-netty-demo/app/springboot3-netty-demo-1.0.0.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=/opt/springboot3-netty-demo/config/
```

后台启动命令如下：

```bash
nohup java \
  -Xms512m \
  -Xmx512m \
  -XX:MaxDirectMemorySize=512m \
  -DLOG_PATH=/opt/springboot3-netty-demo/logs \
  -jar /opt/springboot3-netty-demo/app/springboot3-netty-demo-1.0.0.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=/opt/springboot3-netty-demo/config/ \
  > /opt/springboot3-netty-demo/logs/startup.log 2>&1 &
```

参数说明如下：

| 参数                                  | 说明                                 |
| ------------------------------------- | ------------------------------------ |
| `-Xms`                                | JVM 初始堆内存                       |
| `-Xmx`                                | JVM 最大堆内存                       |
| `-XX:MaxDirectMemorySize`             | JVM 最大直接内存，Netty ByteBuf 常用 |
| `-DLOG_PATH`                          | 日志输出目录                         |
| `--spring.profiles.active`            | 指定运行环境                         |
| `--spring.config.additional-location` | 指定外部配置目录                     |

启动脚本示例：

文件位置：`scripts/start.sh`

该脚本用于后台启动 Netty 服务，并将启动日志写入指定目录。

```bash
#!/usr/bin/env bash

APP_NAME="springboot3-netty-demo"
BASE_DIR="/opt/${APP_NAME}"
JAR_FILE="${BASE_DIR}/app/${APP_NAME}-1.0.0.jar"
LOG_DIR="${BASE_DIR}/logs"
CONFIG_DIR="${BASE_DIR}/config"

mkdir -p "${LOG_DIR}"

PID=$(pgrep -f "${JAR_FILE}")
if [ -n "${PID}" ]; then
  echo "${APP_NAME} is already running, pid=${PID}"
  exit 0
fi

nohup java \
  -Xms512m \
  -Xmx512m \
  -XX:MaxDirectMemorySize=512m \
  -DLOG_PATH="${LOG_DIR}" \
  -jar "${JAR_FILE}" \
  --spring.profiles.active=prod \
  --spring.config.additional-location="${CONFIG_DIR}/" \
  > "${LOG_DIR}/startup.log" 2>&1 &

echo "${APP_NAME} start command submitted"
```

停止脚本示例：

文件位置：`scripts/stop.sh`

该脚本使用 `SIGTERM` 优雅停止服务。

```bash
#!/usr/bin/env bash

APP_NAME="springboot3-netty-demo"
BASE_DIR="/opt/${APP_NAME}"
JAR_FILE="${BASE_DIR}/app/${APP_NAME}-1.0.0.jar"

PID=$(pgrep -f "${JAR_FILE}")
if [ -z "${PID}" ]; then
  echo "${APP_NAME} is not running"
  exit 0
fi

kill -15 "${PID}"
echo "sent SIGTERM to ${APP_NAME}, pid=${PID}"
```

状态检查脚本示例：

文件位置：`scripts/status.sh`

该脚本用于检查进程、HTTP 监控端口和 Netty TCP 端口状态。

```bash
#!/usr/bin/env bash

APP_NAME="springboot3-netty-demo"
HTTP_PORT=8080
NETTY_PORT=9000

PID=$(pgrep -f "${APP_NAME}")

if [ -z "${PID}" ]; then
  echo "${APP_NAME} is not running"
  exit 1
fi

echo "${APP_NAME} is running, pid=${PID}"

echo "HTTP port status:"
ss -lntp | grep ":${HTTP_PORT}" || true

echo "Netty port status:"
ss -lntp | grep ":${NETTY_PORT}" || true
```

脚本授权并执行：

```bash
# 添加执行权限
chmod +x scripts/start.sh scripts/stop.sh scripts/status.sh

# 启动服务
scripts/start.sh

# 查看状态
scripts/status.sh

# 停止服务
scripts/stop.sh
```

### 常见问题处理

常见问题处理用于快速定位启动失败、连接失败、编解码失败、请求超时、内存异常和端口占用等问题。排查时建议先看启动日志，再看端口状态，最后结合客户端日志和协议字段排查。

常见问题如下：

| 问题                 | 可能原因                              | 处理方式                                  |
| -------------------- | ------------------------------------- | ----------------------------------------- |
| Netty 服务启动失败   | 端口被占用                            | 使用 `ss -lntp` 或 `lsof -i` 找到占用进程 |
| 客户端连接失败       | 服务端未启动、端口错误、防火墙拦截    | 检查服务端日志、端口监听和网络策略        |
| 客户端连接后立即断开 | 魔数错误、版本错误、协议格式不一致    | 对比客户端和服务端协议常量                |
| 请求一直超时         | 服务端未返回响应或 `requestId` 不一致 | 检查业务处理器和响应回调                  |
| 解码异常             | 消息长度字段错误或 Pipeline 顺序错误  | 检查 `LengthFieldBasedFrameDecoder` 参数  |
| 内存升高             | 大包过多、回调未清理、连接未释放      | 检查最大帧长度、回调 Map、会话 Map        |
| 日志量过大           | 心跳日志开启 DEBUG                    | 调整日志级别或关闭心跳详细日志            |
| 连接数上不去         | 文件句柄限制过低                      | 调整 `ulimit -n`                          |
| 服务无法优雅停止     | 使用了 `kill -9` 或线程池未释放       | 使用 `kill -15`，检查线程池关闭逻辑       |

端口占用排查：

```bash
# 查看 Netty 端口
ss -lntp | grep 9000

# 查看占用进程
lsof -i :9000
```

依赖冲突排查：

```bash
# 查看 Netty 依赖树
mvn dependency:tree -Dincludes=io.netty

# 查看 Spring Boot 依赖树
mvn dependency:tree -Dincludes=org.springframework.boot
```

JVM 内存排查：

```bash
# 查看 Java 进程
jps -l

# 查看堆和 GC 情况
jstat -gcutil <pid> 1000

# 查看 JVM 参数
jcmd <pid> VM.flags
```

连接数量排查：

```bash
# 查看 9000 端口连接数量
ss -ant | grep ':9000' | wc -l

# 查看已建立连接数量
ss -ant state established | grep ':9000' | wc -l
```

日志排查：

```bash
# 查看启动日志
tail -f /opt/springboot3-netty-demo/logs/startup.log

# 查看应用日志
tail -f /opt/springboot3-netty-demo/logs/springboot3-netty-demo.log

# 查看 Netty 专用日志
tail -f /opt/springboot3-netty-demo/logs/netty.log
```

协议问题排查建议按以下顺序进行：

```text
确认魔数一致
   │
   ▼
确认协议版本一致
   │
   ▼
确认消息类型一致
   │
   ▼
确认消息长度字段偏移量一致
   │
   ▼
确认消息体字符集一致
   │
   ▼
确认 JSON 字段和 requestId 一致
```

如果出现客户端请求发送成功但一直没有响应，优先检查服务端是否执行了 `ctx.writeAndFlush(responsePacket)`，其次检查响应体中是否带回了客户端请求时生成的 `requestId`。如果 `requestId` 丢失或不一致，客户端回调管理器无法完成对应的 `CompletableFuture`，最终会触发响应超时。

