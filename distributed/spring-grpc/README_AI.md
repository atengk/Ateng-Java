# Spring gRPC

Spring gRPC 用于在 Spring Boot 应用中开发基于 gRPC 的服务端与客户端。它适合服务间高性能通信、强接口契约、跨语言调用和流式数据传输。当前建议将文档版本基线写清楚：生产环境优先采用 `Spring Boot 4.0.x + Spring gRPC 1.0.x`；如果使用 Spring Boot 4.1 的官方 gRPC starter，需要注意 4.1 文档当前仍属于预发布线。Spring gRPC 1.0.x 官方标注支持 Spring Boot 4.0.x，当前稳定版本为 1.0.3。([Home](https://docs.spring.io/spring-grpc/reference/getting-started.html))

## 技术概述

本章节用于说明 Spring gRPC 的定位、适用边界，以及它与 REST、WebSocket 的差异。gRPC 本质上是一套高性能 RPC 框架，底层通常基于 HTTP/2，消息格式使用 Protocol Buffers，适合将接口定义、数据结构和代码生成流程统一起来。Spring Boot 4 的 gRPC 文档也明确说明，gRPC 通过二进制消息实现客户端与服务端通信，底层消息格式使用 Protobuf。([Home](https://docs.spring.io/spring-boot/4.1/reference/io/grpc.html))

### Spring gRPC 定位

Spring gRPC 的核心定位是将 gRPC 的服务注册、客户端 Channel、Stub 注入、拦截器、异常处理、健康检查、可观测性等能力接入 Spring Boot 体系。开发者只需要关注 `.proto` 接口定义和业务实现类，服务发布、Bean 扫描、客户端创建等基础设施由 Spring 自动配置完成。

在服务端，Spring gRPC 通常通过 Protobuf 生成的 `BindableService` 实现类暴露服务。只要应用上下文中存在一个或多个 `BindableService` Bean，并且 classpath 中存在 gRPC Server 实现，Spring gRPC 就可以创建 gRPC Server 并绑定服务。官方服务端文档也说明，常见做法是继承 Protobuf 生成的服务基类，再使用 Spring 注解纳入组件扫描。([Home](https://docs.spring.io/spring-grpc/reference/server.html))

在客户端，Spring gRPC 的重点是管理 `GrpcChannelFactory` 和 Protobuf 生成的 Stub。客户端可以通过命名 Channel 读取配置，也可以手动创建 Stub Bean；Spring gRPC 还支持通过 `@ImportGrpcClients` 自动导入生成的 Stub。([Home](https://docs.spring.io/spring-grpc/reference/client.html))

可以将 Spring gRPC 理解为：“gRPC 协议与 Protobuf 代码生成负责通信契约，Spring Boot 负责自动配置、Bean 管理、运行时集成和工程化治理。”

### 适用场景

Spring gRPC 适合以下场景。

| 场景                 | 说明                                                     |
| -------------------- | -------------------------------------------------------- |
| 微服务内部调用       | 服务间调用频繁、接口契约稳定、对性能和序列化体积敏感     |
| 跨语言服务调用       | Java、Go、Python、Node.js 等服务需要共享统一接口定义     |
| 高并发低延迟通信     | 请求体较小、调用频繁、希望降低 JSON 序列化成本           |
| 强契约 API           | 希望通过 `.proto` 统一定义请求、响应、枚举和服务接口     |
| 流式数据传输         | 需要 Server Streaming、Client Streaming 或双向流式通信   |
| 移动端或边缘服务调用 | 希望降低网络传输体积并保持明确接口定义                   |
| 内部平台能力开放     | 例如用户中心、权限中心、订单中心、文件服务等内部基础服务 |

不建议优先使用 Spring gRPC 的场景主要包括：需要直接面向浏览器调用的开放接口、强依赖 HTTP 缓存或普通网关能力的接口、以简单 CRUD 为主且调用频率不高的后台管理接口、需要人类直接调试和阅读请求体的轻量接口。

### 与 REST、WebSocket 的对比

REST、WebSocket 和 gRPC 解决的问题不同，不应简单替代。REST 更适合资源模型和开放 HTTP API；WebSocket 更适合浏览器与服务端之间的长连接实时推送；gRPC 更适合服务间强契约、高性能、跨语言 RPC 调用。

| 对比项       | Spring gRPC                                 | REST                         | WebSocket                    |
| ------------ | ------------------------------------------- | ---------------------------- | ---------------------------- |
| 通信模型     | RPC 方法调用                                | 资源导向 HTTP API            | 长连接双向消息               |
| 常见协议     | HTTP/2                                      | HTTP/1.1 或 HTTP/2           | WebSocket                    |
| 数据格式     | Protobuf 二进制                             | JSON 为主                    | JSON、文本、二进制均可       |
| 接口契约     | `.proto` 强契约                             | OpenAPI 或文档约束           | 通常依赖自定义消息协议       |
| 浏览器友好度 | 原生浏览器调用不如 REST 直接                | 非常友好                     | 非常适合浏览器实时通信       |
| 性能         | 高，序列化体积小                            | 中等，JSON 可读但体积较大    | 适合实时推送，不等同于 RPC   |
| 流式能力     | 原生支持多种流式 RPC                        | 通常需要 SSE、分页或轮询     | 原生双向实时通信             |
| 调试成本     | 需要 grpcurl、BloomRPC、Postman gRPC 等工具 | curl、浏览器、Postman 都方便 | 需要 WebSocket 客户端工具    |
| 典型场景     | 微服务内部调用、跨语言 RPC、高性能调用      | 对外 API、管理后台、开放平台 | IM、通知、实时看板、协同编辑 |

选择建议如下：对外开放 API 优先 REST；浏览器实时推送优先 WebSocket 或 SSE；服务内部高频调用、跨语言调用、强契约调用优先 gRPC。一个系统中三者可以共存，例如：前端调用 REST，服务间调用 gRPC，实时通知使用 WebSocket。

## 环境准备

本章节用于明确开发 Spring Boot 4 + Spring gRPC 项目前需要准备的 JDK、构建工具、项目结构和 Protobuf 编译链路。Spring Boot 4.0.6 要求至少 Java 17，并兼容到 Java 26；构建工具方面，Spring Boot 4 明确支持 Maven 3.6.3+，以及 Gradle 8.x（8.14+）和 9.x。([Home](https://docs.spring.io/spring-boot/system-requirements.html))

### JDK 版本要求

Spring Boot 4 的最低 JDK 要求是 Java 17，因此 Spring gRPC 项目也建议统一使用 JDK 17 作为最低开发与运行版本。生产环境可以选择 JDK 21 LTS，但需要确保 CI、镜像、IDE、构建工具和运行环境版本一致。

推荐版本如下。

| 组件                   | 推荐版本     | 说明                                                         |
| ---------------------- | ------------ | ------------------------------------------------------------ |
| JDK                    | 17 或 21 LTS | Spring Boot 4 最低要求 Java 17                               |
| Spring Boot            | 4.0.6        | 当前稳定线示例                                               |
| Spring gRPC            | 1.0.3        | 当前稳定线示例                                               |
| Maven                  | 3.9.x        | 高于 Spring Boot 最低要求，兼容插件更稳妥                    |
| Gradle                 | 8.14+ 或 9.x | 符合 Spring Boot 4 构建工具要求                              |
| Protobuf Gradle Plugin | 0.9.6        | 当前 Gradle Plugin Portal 最新版本为 0.9.6 ([plugins.gradle.org](https://plugins.gradle.org/plugin/com.google.protobuf?utm_source=chatgpt.com)) |

本地建议先确认版本。

```bash
java -version
mvn -version
gradle -version
```

这些命令分别用于确认 JDK、Maven 和 Gradle 是否满足项目要求。如果团队使用 Maven Wrapper 或 Gradle Wrapper，应以 `./mvnw -version`、`./gradlew -version` 的输出为准。

### Spring Boot 4 项目结构

Spring gRPC 项目建议将普通 Java 代码、配置文件和 `.proto` 文件分开管理。官方文档也建议将 `.proto` 文件放在 `src/main/proto` 目录下，构建时会生成 Java 源码和 gRPC Stub。([Home](https://docs.spring.io/spring-grpc/reference/getting-started.html))

推荐项目结构如下。

```text
spring-grpc-demo
├── pom.xml 或 build.gradle
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               └── grpc
│   │   │                   ├── GrpcDemoApplication.java
│   │   │                   ├── server
│   │   │                   │   └── UserGrpcService.java
│   │   │                   ├── client
│   │   │                   │   └── UserGrpcClient.java
│   │   │                   └── config
│   │   │                       └── GrpcClientConfig.java
│   │   ├── proto
│   │   │   └── user.proto
│   │   └── resources
│   │       └── application.yml
│   └── test
│       ├── java
│       │   └── io
│       │       └── github
│       │           └── atengk
│       │               └── grpc
│       │                   └── UserGrpcServiceTest.java
│       └── resources
│           └── application-test.yml
```

目录说明如下。

| 路径                                 | 作用                                                    |
| ------------------------------------ | ------------------------------------------------------- |
| `src/main/proto`                     | 存放 Protobuf 接口定义文件                              |
| `src/main/java`                      | 存放 Spring Boot 启动类、服务端实现、客户端封装、配置类 |
| `src/main/resources/application.yml` | 配置 gRPC 服务端端口、客户端 Channel、Profile 参数      |
| `target/generated-sources`           | Maven 编译后生成的 Protobuf Java 代码                   |
| `build/generated/source/proto`       | Gradle 编译后生成的 Protobuf Java 代码                  |

Maven 构建后常见生成目录是 `target/generated-sources/protobuf/grpc-java` 和 `target/generated-sources/protobuf/java`；Gradle 构建后常见生成目录是 `build/generated/source/proto/main/grpc` 和 `build/generated/source/proto/main/java`。这些目录用于存放 Protobuf Message 类、Service 基类和 Stub 类。([Home](https://docs.spring.io/spring-grpc/reference/getting-started.html))

### Gradle 或 Maven 构建配置

本节给出 Maven 和 Gradle 两种基础构建配置。实际项目二选一即可。生产环境建议固定版本，避免因为预发布依赖或插件升级导致构建结果不一致。

Maven 示例适用于 Spring Boot 4.0.x + Spring gRPC 1.0.x 稳定基线。

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.6</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-grpc-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-grpc-demo</name>
    <description>Spring Boot 4 Spring gRPC 示例项目</description>

    <properties>
        <!-- Spring Boot 4 最低要求 Java 17 -->
        <java.version>17</java.version>

        <!-- Spring gRPC 1.0.x 适配 Spring Boot 4.0.x -->
        <spring-grpc.version>1.0.3</spring-grpc.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring gRPC 推荐 BOM，统一管理 gRPC、Protobuf 等相关依赖版本 -->
            <dependency>
                <groupId>org.springframework.grpc</groupId>
                <artifactId>spring-grpc-dependencies</artifactId>
                <version>${spring-grpc.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring gRPC Boot Starter，提供服务端、客户端、Channel、Stub 等自动配置能力 -->
        <dependency>
            <groupId>org.springframework.grpc</groupId>
            <artifactId>spring-grpc-spring-boot-starter</artifactId>
        </dependency>

        <!-- 可选：提供 gRPC Reflection 与 Health 标准服务，便于 grpcurl 调试和健康检查 -->
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-services</artifactId>
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
            <!-- Spring Boot 打包插件，用于生成可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- Protobuf 编译插件，用于根据 src/main/proto 生成 Java 类和 gRPC Stub -->
            <plugin>
                <groupId>io.github.ascopes</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <!-- 执行 Protobuf 代码生成 -->
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
```

Spring gRPC 官方建议在 Spring Boot 应用中使用 `spring-grpc-dependencies` BOM，以统一管理 Spring gRPC 推荐的依赖版本；服务端使用 `spring-grpc-spring-boot-starter` 时，默认会使用 Netty Server，并且默认端口为 `9090`。([Home](https://docs.spring.io/spring-grpc/reference/getting-started.html))

Gradle 示例适用于同一稳定基线。

文件位置：`build.gradle`

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.6'
    id 'io.spring.dependency-management' version '1.1.7'

    // Protobuf 编译插件，用于生成 Message 类、Service 基类和 Stub
    id 'com.google.protobuf' version '0.9.6'
}

group = 'io.github.atengk'
version = '1.0.0'

java {
    // Spring Boot 4 最低要求 Java 17
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring gRPC 推荐 BOM，统一管理相关依赖版本
    implementation platform('org.springframework.grpc:spring-grpc-dependencies:1.0.3')

    // Spring gRPC Boot Starter，提供服务端与客户端自动配置
    implementation 'org.springframework.grpc:spring-grpc-spring-boot-starter'

    // 可选：用于 gRPC Reflection 与 Health，便于调试和健康检查
    implementation 'io.grpc:grpc-services'

    // Spring Boot 测试依赖
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

如果项目已经明确采用 Spring Boot 4.1 的 gRPC 官方 starter，可以使用 `org.springframework.boot:spring-boot-starter-grpc-server` 和 `org.springframework.boot:spring-boot-starter-grpc-client`。不过 Spring Boot 4.1 文档当前标注为开发中版本，最新稳定线仍是 Spring Boot 4.0.6，因此正式文档中应将这类配置标注为 Boot 4.1+ 方案，避免和 Spring Boot 4.0.x + Spring gRPC 1.0.x 的依赖坐标混用。([Home](https://docs.spring.io/spring-boot/4.1/reference/io/grpc.html))

### Protobuf 编译插件配置

Protobuf 编译插件用于把 `src/main/proto/*.proto` 文件转换成 Java 代码。生成结果主要包括两类：一类是请求、响应、枚举等 Message Java 类；另一类是 gRPC Service 基类和客户端 Stub。Spring Boot 文档说明，`.proto` 文件是语言无关的，需要通过构建流程转换成可用的 Java 代码，然后用于服务实现或远程调用。([Home](https://docs.spring.io/spring-boot/4.1/reference/io/grpc.html))

Maven 项目中，推荐将 `.proto` 文件放在 `src/main/proto`，然后执行：

```bash
./mvnw clean package
```

执行完成后，重点检查以下目录。

```text
target/generated-sources/protobuf/java
target/generated-sources/protobuf/grpc-java
```

Gradle 项目中，同样将 `.proto` 文件放在 `src/main/proto`，然后执行：

```bash
./gradlew clean build
```

执行完成后，重点检查以下目录。

```text
build/generated/source/proto/main/java
build/generated/source/proto/main/grpc
```

官方 Getting Started 文档也给出了上述 Maven 和 Gradle 生成目录，IDE 中需要将这些目录识别为 Generated Source Root；IntelliJ IDEA 可以右键目录选择 `Mark Directory As` → `Generated Source Root`。([Home](https://docs.spring.io/spring-grpc/reference/getting-started.html))

建议先放入一个最小可编译的 proto 文件验证插件链路。

文件位置：`src/main/proto/user.proto`

```proto
syntax = "proto3";

// 生成多个 Java 文件，便于在业务代码中直接引用 UserRequest、UserResponse 等类型
option java_multiple_files = true;

// 生成代码所在包名，建议与 Java 主包名保持一致并追加 proto 分层
option java_package = "io.github.atengk.grpc.proto";

// 生成外层类名称，主要用于未拆分类型或静态描述信息
option java_outer_classname = "UserProto";

// 用户服务定义
service UserService {
  // 根据用户 ID 查询用户信息
  rpc GetUser (GetUserRequest) returns (GetUserResponse) {}
}

// 查询用户请求
message GetUserRequest {
  int64 id = 1;
}

// 查询用户响应
message GetUserResponse {
  int64 id = 1;
  string username = 2;
  string nickname = 3;
}
```

编译成功后，业务代码中通常会使用以下生成类。

```text
io.github.atengk.grpc.proto.GetUserRequest
io.github.atengk.grpc.proto.GetUserResponse
io.github.atengk.grpc.proto.UserServiceGrpc
io.github.atengk.grpc.proto.UserServiceGrpc.UserServiceImplBase
io.github.atengk.grpc.proto.UserServiceGrpc.UserServiceBlockingStub
io.github.atengk.grpc.proto.UserServiceGrpc.UserServiceStub
io.github.atengk.grpc.proto.UserServiceGrpc.UserServiceFutureStub
```

验证方式如下。

```bash
# Maven 项目
./mvnw clean package

# Gradle 项目
./gradlew clean build
```

如果构建失败，优先检查四类问题：第一，`src/main/proto` 路径是否正确；第二，`syntax = "proto3";` 是否写在文件第一行附近；第三，`java_package` 是否与项目包名规范一致；第四，Maven 或 Gradle 是否成功下载 Protobuf、gRPC 和插件依赖。



## 依赖配置

本章节用于说明 Spring Boot 4 项目中接入 Spring gRPC 时需要配置的核心依赖、Protobuf 与 gRPC 依赖、服务端依赖和客户端依赖。Spring gRPC 官方建议在 Spring Boot 应用中使用 `spring-grpc-dependencies` 进行依赖版本管理，这样可以避免手动维护 gRPC、Protobuf 以及 Spring gRPC 相关依赖版本。([Home](https://docs.spring.io/spring-grpc/reference/getting-started.html?utm_source=chatgpt.com))

### Spring gRPC 核心依赖

Spring gRPC 核心依赖主要由 BOM 和 Starter 两部分组成。BOM 用于统一版本，Starter 用于启用 Spring Boot 自动配置能力。对于 Spring Boot 应用，应优先使用 `spring-grpc-dependencies`；如果不是 Spring Boot 应用，才考虑使用 `spring-grpc-build-dependencies`。([Home](https://docs.spring.io/spring-grpc/reference/getting-started.html?utm_source=chatgpt.com))

Maven 项目建议先配置依赖版本管理。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 4 最低要求 Java 17 -->
    <java.version>17</java.version>

    <!-- Spring gRPC 稳定线版本，适合 Spring Boot 4.0.x 项目 -->
    <spring-grpc.version>1.0.3</spring-grpc.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Spring gRPC BOM：统一管理 Spring gRPC、gRPC、Protobuf 等推荐依赖版本 -->
        <dependency>
            <groupId>org.springframework.grpc</groupId>
            <artifactId>spring-grpc-dependencies</artifactId>
            <version>${spring-grpc.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

然后添加 Spring gRPC Starter。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring gRPC 核心 Starter：启用服务端、客户端、Channel、Stub 等自动配置能力 -->
    <dependency>
        <groupId>org.springframework.grpc</groupId>
        <artifactId>spring-grpc-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

Gradle 项目可以使用 `platform` 方式导入 BOM。

文件位置：`build.gradle`

```groovy
dependencies {
    // Spring gRPC BOM：统一管理推荐依赖版本
    implementation platform('org.springframework.grpc:spring-grpc-dependencies:1.0.3')

    // Spring gRPC 核心 Starter：启用服务端与客户端自动配置
    implementation 'org.springframework.grpc:spring-grpc-spring-boot-starter'
}
```

如果项目后续升级到 Spring Boot 4.1 线，可以关注 `spring-boot-starter-grpc-server` 和 `spring-boot-starter-grpc-client`。Spring Boot 4.1 文档说明，Spring Boot 提供了 gRPC server/client starter，但该文档当前对应 4.1.0-RC1，并提示最新稳定版本应使用 Spring Boot 4.0.6，因此不要在稳定项目中盲目混用两套依赖坐标。([Home](https://docs.spring.io/spring-boot/4.1/reference/io/grpc.html?utm_source=chatgpt.com))

### Protobuf 与 gRPC 依赖

Protobuf 与 gRPC 依赖主要用于 `.proto` 文件编译、Message 类生成、Service 基类生成和 Stub 生成。Spring Boot 4.1 文档说明，`.proto` 文件是语言无关的，需要通过构建流程转换成 Java 代码，随后才能用于服务实现或客户端调用。([Home](https://docs.spring.io/spring-boot/4.1/reference/io/grpc.html?utm_source=chatgpt.com))

在使用 Spring Boot 父工程和 Spring gRPC BOM 的情况下，通常不需要显式声明 `protobuf-java`、`grpc-protobuf`、`grpc-stub` 等版本。只有在需要精细控制生成代码、替换传输实现或排查依赖冲突时，才建议显式声明。

Maven 中常见的显式依赖如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Protobuf Java 运行时：生成的 Message 类依赖该运行时 -->
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
    </dependency>

    <!-- gRPC Protobuf 支持：用于 gRPC 与 Protobuf 消息编解码 -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-protobuf</artifactId>
    </dependency>

    <!-- gRPC Stub 支持：生成的 BlockingStub、AsyncStub、FutureStub 依赖该模块 -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-stub</artifactId>
    </dependency>
</dependencies>
```

Gradle 中对应配置如下。

文件位置：`build.gradle`

```groovy
dependencies {
    // Protobuf Java 运行时：生成的 Message 类依赖该运行时
    implementation 'com.google.protobuf:protobuf-java'

    // gRPC Protobuf 支持：用于 gRPC 与 Protobuf 消息编解码
    implementation 'io.grpc:grpc-protobuf'

    // gRPC Stub 支持：生成的客户端 Stub 和服务端基类依赖该模块
    implementation 'io.grpc:grpc-stub'
}
```

实际项目中，如果 `spring-grpc-spring-boot-starter` 已经传递引入了这些依赖，并且项目可以正常编译，则不建议重复声明。依赖声明越少，版本冲突风险越低；需要定制时再显式添加更合适。

### 服务端依赖

服务端依赖用于启动 gRPC Server，并将 Spring 容器中的 `BindableService` Bean 绑定到服务端。Spring gRPC 服务端文档说明，只要应用上下文中提供一个或多个 `BindableService` Bean，并且 classpath 中存在 gRPC Server 实现，Spring gRPC 就可以创建服务端；通常做法是继承 `.proto` 生成的服务基类，并使用 `@Service` 纳入 Spring 扫描。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

基础服务端依赖如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring gRPC Starter：默认启用基于 Netty 的 gRPC Server -->
    <dependency>
        <groupId>org.springframework.grpc</groupId>
        <artifactId>spring-grpc-spring-boot-starter</artifactId>
    </dependency>

    <!-- gRPC 标准服务：提供 Reflection 与 Health，便于 grpcurl 调试和健康检查 -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-services</artifactId>
    </dependency>
</dependencies>
```

Gradle 配置如下。

文件位置：`build.gradle`

```groovy
dependencies {
    // Spring gRPC Starter：默认启用基于 Netty 的 gRPC Server
    implementation 'org.springframework.grpc:spring-grpc-spring-boot-starter'

    // gRPC 标准服务：提供 Reflection 与 Health
    implementation 'io.grpc:grpc-services'
}
```

`grpc-services` 是可选但非常建议在开发、测试和内部服务环境中启用的依赖。Spring gRPC 文档说明，gRPC Reflection 和 Health 服务位于 `io.grpc:grpc-services` 中；添加该依赖后，Spring gRPC 可以自动配置标准 Reflection 服务和 Health 服务。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

服务端常用配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  grpc:
    server:
      # gRPC 服务端监听端口，默认通常为 9090
      port: 9090

      reflection:
        # 开发和测试环境建议开启，生产环境需结合安全策略评估
        enabled: true

      health:
        # 启用 gRPC 标准健康检查服务
        enabled: true
```

如果需要使用 gRPC 官方的 Shaded Netty，可以排除默认 Netty 并添加 `grpc-netty-shaded`。Spring gRPC 服务端文档说明，可以通过添加 `grpc-netty-shaded` 并替换 `grpc-netty` 来切换传输实现。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring gRPC Starter：排除默认 grpc-netty -->
    <dependency>
        <groupId>org.springframework.grpc</groupId>
        <artifactId>spring-grpc-spring-boot-starter</artifactId>
        <exclusions>
            <exclusion>
                <groupId>io.grpc</groupId>
                <artifactId>grpc-netty</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- 使用 gRPC 官方 Shaded Netty，减少 Netty 版本冲突风险 -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
    </dependency>
</dependencies>
```

### 客户端依赖

客户端依赖用于创建 Channel、注入 Stub，并调用远程 gRPC 服务。Spring gRPC 客户端文档说明，Protobuf 生成的 Stub 需要绑定到 Channel；可以通过 `GrpcChannelFactory` 手动创建 Stub，也可以使用 `@ImportGrpcClients` 自动导入 Stub Bean。([Home](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

基础客户端依赖如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring gRPC Starter：启用客户端 Channel 与 Stub 自动配置 -->
    <dependency>
        <groupId>org.springframework.grpc</groupId>
        <artifactId>spring-grpc-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

客户端 Channel 配置示例如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  grpc:
    client:
      channels:
        user-service:
          # 命名 Channel 地址，业务代码中通过 user-service 创建 Stub
          address: 127.0.0.1:9090
```

手动创建客户端 Stub 时，可以注入 `GrpcChannelFactory`。Spring gRPC API 文档说明，`GrpcChannelFactory` 用于根据目标字符串创建 `ManagedChannel`，目标可以是命名 Channel，也可以是符合 gRPC NameResolver 规范的地址。([Home](https://docs.spring.io/spring-grpc/reference/api/java/org/springframework/grpc/client/GrpcChannelFactory.html?utm_source=chatgpt.com))

这个配置类用于把命名 Channel 转换成业务可直接注入的 Blocking Stub。

文件位置：`src/main/java/io/github/atengk/grpc/config/UserGrpcClientConfig.java`

```java
package io.github.atengk.grpc.config;

import io.github.atengk.grpc.proto.UserServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * 用户 gRPC 客户端配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class UserGrpcClientConfig {

    /**
     * 创建用户服务 Blocking Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 用户服务阻塞式 Stub
     */
    @Bean
    public UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub(GrpcChannelFactory channelFactory) {
        return UserServiceGrpc.newBlockingStub(channelFactory.createChannel("user-service"));
    }
}
```

如果客户端需要调用多个远程服务，建议每个服务使用独立命名 Channel，例如 `user-service`、`order-service`、`file-service`，避免在代码中硬编码 IP 和端口。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  grpc:
    client:
      channels:
        user-service:
          # 用户服务 gRPC 地址
          address: 127.0.0.1:9090

        order-service:
          # 订单服务 gRPC 地址
          address: 127.0.0.1:9091

        file-service:
          # 文件服务 gRPC 地址
          address: 127.0.0.1:9092
```

## Protobuf 接口定义

本章节用于说明 `.proto` 文件的目录规范、Message 定义、Service 定义，以及 Java 代码生成相关配置。Protobuf 是 gRPC 的接口契约来源，定义质量会直接影响服务兼容性、代码可读性、跨语言调用体验和后续版本演进成本。

### proto 文件目录规范

Spring gRPC 官方 Getting Started 文档建议将 `.proto` 文件放在 `src/main/proto` 目录下，构建插件会从该目录读取 Protobuf 文件并生成 Java 源码。Maven 和 Gradle 生成目录不同，但源文件目录都建议统一使用 `src/main/proto`。([Home](https://docs.spring.io/spring-grpc/reference/getting-started.html?utm_source=chatgpt.com))

推荐目录结构如下。

```text
src
├── main
│   ├── java
│   │   └── io/github/atengk/grpc
│   ├── proto
│   │   ├── common
│   │   │   └── common.proto
│   │   ├── user
│   │   │   └── user.proto
│   │   └── order
│   │       └── order.proto
│   └── resources
│       └── application.yml
```

目录设计建议如下。

| 目录                     | 说明                                         |
| ------------------------ | -------------------------------------------- |
| `src/main/proto/common`  | 存放通用响应、分页、错误信息、公共枚举等定义 |
| `src/main/proto/user`    | 存放用户服务相关 Message 和 Service          |
| `src/main/proto/order`   | 存放订单服务相关 Message 和 Service          |
| `src/main/proto/payment` | 存放支付服务相关 Message 和 Service          |
| `src/main/proto/file`    | 存放文件服务相关 Message 和 Service          |

命名建议如下。

| 类型         | 推荐命名           | 示例                                   |
| ------------ | ------------------ | -------------------------------------- |
| proto 文件   | 小写下划线或业务名 | `user.proto`、`order_query.proto`      |
| package      | 业务域分层         | `ateng.user.v1`                        |
| Java package | Java 包名规范      | `io.github.atengk.grpc.proto.user.v1`  |
| Message      | 大驼峰             | `GetUserRequest`、`UserDetailResponse` |
| Service      | 大驼峰 + `Service` | `UserService`、`OrderService`          |
| RPC 方法     | 大驼峰动词短语     | `GetUser`、`ListUsers`、`CreateUser`   |

### Message 定义

Message 用于定义请求体、响应体、嵌套对象、枚举和集合字段。字段编号一旦发布后不应随意修改或复用，因为字段编号是 Protobuf 二进制编码的重要组成部分。新增字段应使用新的字段编号，废弃字段建议使用 `reserved` 保留。

下面是用户服务的 Message 定义示例。

文件位置：`src/main/proto/user/user.proto`

```proto
syntax = "proto3";

package ateng.user.v1;

option java_multiple_files = true;
option java_package = "io.github.atengk.grpc.proto.user.v1";
option java_outer_classname = "UserProto";

// 用户性别
enum Gender {
  // 未指定，避免默认值被误判为有效业务值
  GENDER_UNSPECIFIED = 0;

  // 男
  GENDER_MALE = 1;

  // 女
  GENDER_FEMALE = 2;
}

// 查询用户请求
message GetUserRequest {
  // 用户 ID
  int64 id = 1;
}

// 批量查询用户请求
message BatchGetUserRequest {
  // 用户 ID 集合
  repeated int64 ids = 1;
}

// 用户详情
message UserDetail {
  // 用户 ID
  int64 id = 1;

  // 用户名
  string username = 2;

  // 昵称
  string nickname = 3;

  // 性别
  Gender gender = 4;

  // 手机号
  string mobile = 5;

  // 邮箱
  string email = 6;

  // 是否启用
  bool enabled = 7;

  // 创建时间，建议使用毫秒时间戳或 google.protobuf.Timestamp
  int64 created_at = 8;
}

// 查询用户响应
message GetUserResponse {
  // 用户详情
  UserDetail user = 1;
}

// 批量查询用户响应
message BatchGetUserResponse {
  // 用户列表
  repeated UserDetail users = 1;
}
```

Message 设计建议如下。

| 规则                           | 说明                                                   |
| ------------------------------ | ------------------------------------------------------ |
| 字段编号不要复用               | 删除字段后也不要把原编号分配给新字段                   |
| 枚举第一个值使用 `UNSPECIFIED` | proto3 枚举默认值为 0，业务枚举要避免误判              |
| 请求对象和响应对象分开定义     | 不建议多个接口共享一个过大的通用 Request               |
| 金额字段避免直接使用浮点数     | 建议使用 `int64 amount_cent` 或字符串小数              |
| 时间字段统一规范               | 可使用毫秒时间戳，也可使用 `google.protobuf.Timestamp` |
| 集合字段使用 `repeated`        | 适合列表、批量 ID、明细集合                            |
| 不确定字段谨慎使用 `map`       | `map` 灵活但契约弱，不适合作为主要业务模型             |

字段废弃时建议使用 `reserved`。

```proto
message UserDetail {
  reserved 9;
  reserved "old_field_name";

  int64 id = 1;
  string username = 2;
  string nickname = 3;
}
```

这种写法可以防止后续误用已废弃字段编号或字段名，有利于接口长期兼容。

### Service 定义

Service 用于定义 gRPC 方法。一个 Service 通常对应一个业务领域，例如 `UserService`、`OrderService`、`FileService`。gRPC 支持四种 RPC 形式：Unary RPC、Server Streaming RPC、Client Streaming RPC 和 Bidirectional Streaming RPC。

下面是用户服务的 Service 定义示例。

文件位置：`src/main/proto/user/user.proto`

```proto
syntax = "proto3";

package ateng.user.v1;

option java_multiple_files = true;
option java_package = "io.github.atengk.grpc.proto.user.v1";
option java_outer_classname = "UserProto";

// 用户服务
service UserService {
  // 根据用户 ID 查询用户
  rpc GetUser (GetUserRequest) returns (GetUserResponse);

  // 批量查询用户
  rpc BatchGetUser (BatchGetUserRequest) returns (BatchGetUserResponse);

  // 服务端流式返回用户列表
  rpc StreamUsers (StreamUsersRequest) returns (stream UserDetail);

  // 客户端流式导入用户
  rpc ImportUsers (stream ImportUserRequest) returns (ImportUserResponse);

  // 双向流式同步用户状态
  rpc SyncUserStatus (stream SyncUserStatusRequest) returns (stream SyncUserStatusResponse);
}

// 查询用户请求
message GetUserRequest {
  int64 id = 1;
}

// 查询用户响应
message GetUserResponse {
  UserDetail user = 1;
}

// 批量查询用户请求
message BatchGetUserRequest {
  repeated int64 ids = 1;
}

// 批量查询用户响应
message BatchGetUserResponse {
  repeated UserDetail users = 1;
}

// 流式查询用户请求
message StreamUsersRequest {
  bool enabled_only = 1;
  int32 page_size = 2;
}

// 导入用户请求
message ImportUserRequest {
  string username = 1;
  string nickname = 2;
  string mobile = 3;
}

// 导入用户响应
message ImportUserResponse {
  int32 success_count = 1;
  int32 failure_count = 2;
}

// 同步用户状态请求
message SyncUserStatusRequest {
  int64 id = 1;
  bool enabled = 2;
}

// 同步用户状态响应
message SyncUserStatusResponse {
  int64 id = 1;
  bool success = 2;
  string message = 3;
}

// 用户详情
message UserDetail {
  int64 id = 1;
  string username = 2;
  string nickname = 3;
  string mobile = 4;
  bool enabled = 5;
}
```

RPC 方法设计建议如下。

| RPC 类型                | 定义形式                                           | 适用场景                                 |
| ----------------------- | -------------------------------------------------- | ---------------------------------------- |
| Unary                   | `rpc A (Request) returns (Response)`               | 普通查询、创建、修改、删除               |
| Server Streaming        | `rpc A (Request) returns (stream Response)`        | 服务端连续返回数据，例如大列表、日志推送 |
| Client Streaming        | `rpc A (stream Request) returns (Response)`        | 客户端批量上传或导入                     |
| Bidirectional Streaming | `rpc A (stream Request) returns (stream Response)` | 双向实时通信、状态同步、长任务交互       |

对于大多数业务系统，优先使用 Unary RPC。只有在确实存在大量数据分批返回、持续推送、批量上传或双向交互场景时，再使用流式 RPC。流式接口会增加客户端处理、超时控制、异常恢复和可观测性复杂度。

### 包名与 Java 代码生成配置

包名与 Java 代码生成配置决定生成类的包路径、文件拆分方式和外层类名称。Spring Boot 文档中的 `.proto` 示例也使用了 `option java_package` 和 `option java_multiple_files`，这两个配置是 Java 项目中最常用的生成选项。([Home](https://docs.spring.io/spring-boot/4.1/reference/io/grpc.html?utm_source=chatgpt.com))

推荐配置如下。

```proto
syntax = "proto3";

package ateng.user.v1;

option java_multiple_files = true;
option java_package = "io.github.atengk.grpc.proto.user.v1";
option java_outer_classname = "UserProto";
```

配置说明如下。

| 配置项                  | 说明                   | 建议                         |
| ----------------------- | ---------------------- | ---------------------------- |
| `syntax`                | 指定 Protobuf 语法版本 | 新项目统一使用 `proto3`      |
| `package`               | Protobuf 层面的包名    | 建议包含业务域和版本号       |
| `java_multiple_files`   | 是否生成多个 Java 文件 | 建议设置为 `true`            |
| `java_package`          | 生成 Java 类的包名     | 建议与项目主包名保持一致     |
| `java_outer_classname`  | 外层类名称             | 建议使用业务名 + `Proto`     |
| `java_generic_services` | 是否生成通用服务接口   | gRPC Java 项目通常不需要配置 |

推荐包名规范如下。

```proto
package ateng.user.v1;

option java_package = "io.github.atengk.grpc.proto.user.v1";
```

其中：

`ateng.user.v1` 是 Protobuf 包名，主要用于跨语言识别和避免 proto 类型冲突。

`io.github.atengk.grpc.proto.user.v1` 是 Java 生成代码包名，主要用于 Java 代码导入和 IDE 管理。

`v1` 是接口版本号。后续如果接口发生不兼容变化，应新增 `v2` 包，而不是直接破坏 `v1` 的字段编号、字段类型或 RPC 方法语义。

构建完成后，Maven 项目通常会生成类似目录。

```text
target/generated-sources/protobuf/java/io/github/atengk/grpc/proto/user/v1
target/generated-sources/protobuf/grpc-java/io/github/atengk/grpc/proto/user/v1
```

Gradle 项目通常会生成类似目录。

```text
build/generated/source/proto/main/java/io/github/atengk/grpc/proto/user/v1
build/generated/source/proto/main/grpc/io/github/atengk/grpc/proto/user/v1
```

最终业务代码中常用的生成类包括：

```text
io.github.atengk.grpc.proto.user.v1.GetUserRequest
io.github.atengk.grpc.proto.user.v1.GetUserResponse
io.github.atengk.grpc.proto.user.v1.UserDetail
io.github.atengk.grpc.proto.user.v1.UserServiceGrpc
io.github.atengk.grpc.proto.user.v1.UserServiceGrpc.UserServiceImplBase
io.github.atengk.grpc.proto.user.v1.UserServiceGrpc.UserServiceBlockingStub
io.github.atengk.grpc.proto.user.v1.UserServiceGrpc.UserServiceStub
io.github.atengk.grpc.proto.user.v1.UserServiceGrpc.UserServiceFutureStub
```

验证命令如下。

```bash
# Maven 项目：编译 proto 并打包
./mvnw clean package

# Gradle 项目：编译 proto 并执行构建
./gradlew clean build
```

如果构建成功但 IDE 仍然无法识别生成类，优先刷新 Maven/Gradle 项目，并确认生成源码目录是否已被 IDE 标记为 Generated Sources Root。Spring gRPC 官方文档也明确说明，生成代码目录可以被 IDE 标记为 Generated Source Root，以便业务代码直接引用生成类。([Home](https://docs.spring.io/spring-grpc/reference/getting-started.html?utm_source=chatgpt.com))



## gRPC 服务端开发

本章节用于说明 Spring gRPC 服务端的核心开发方式。Spring gRPC 服务端的基本模型是：开发者提供一个或多个 `BindableService` Bean，Spring gRPC 自动创建 gRPC Server 并将这些服务绑定到服务端。最常见的方式是继承 Protobuf 生成的 `xxxGrpc.xxxImplBase`，然后使用 Spring `@Service` 注解交给容器管理。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

以下代码基于前文 `src/main/proto/user/user.proto` 生成的 Java 类，默认生成包名为：

```text
io.github.atengk.grpc.proto.user.v1
```

示例中使用四种 RPC 类型：

| RPC 类型                    | 服务端方法特征     | 典型用途                           |
| --------------------------- | ------------------ | ---------------------------------- |
| Unary RPC                   | 一个请求，一个响应 | 查询详情、创建、修改、删除         |
| Server Streaming RPC        | 一个请求，多个响应 | 分批返回列表、日志推送、进度推送   |
| Client Streaming RPC        | 多个请求，一个响应 | 批量导入、批量上传、客户端持续上报 |
| Bidirectional Streaming RPC | 多个请求，多个响应 | 实时同步、双向通信、长任务交互     |

gRPC Java 生成代码中，服务端实现类通常是 `ServiceNameGrpc.ServiceNameImplBase`；客户端会生成异步、阻塞式和 Future 三类 Stub，例如 `ServiceNameStub`、`ServiceNameBlockingStub` 和 `ServiceNameFutureStub`。Blocking Stub 只支持 Unary 和 Server Streaming；Future Stub 只支持 Unary；Async Stub 支持四种 RPC 类型。([gRPC](https://grpc.io/docs/languages/java/generated-code/?utm_source=chatgpt.com))

### 服务实现类编写

服务实现类用于承接 Protobuf 中定义的 RPC 方法，并把请求转换为具体业务处理。Spring gRPC 推荐将服务实现类声明为 Spring Bean，最直接的方式是加上 `@Service`，让 Spring Boot 组件扫描自动发现。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

示例文件结构如下。

```text
src/main/java/io/github/atengk/grpc
├── server
│   └── UserGrpcService.java
└── support
    └── UserMockRepository.java
```

下面的仓储类用于模拟用户数据，便于服务端 RPC 示例可以直接运行。

文件位置：`src/main/java/io/github/atengk/grpc/support/UserMockRepository.java`

```java
package io.github.atengk.grpc.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.proto.user.v1.UserDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 用户模拟数据仓储
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Repository
public class UserMockRepository {

    private final List<UserDetail> users = new ArrayList<>();

    public UserMockRepository() {
        users.add(UserDetail.newBuilder()
                .setId(1L)
                .setUsername("ateng")
                .setNickname("阿腾")
                .setMobile("13800000001")
                .setEnabled(true)
                .build());

        users.add(UserDetail.newBuilder()
                .setId(2L)
                .setUsername("blair")
                .setNickname("Blair")
                .setMobile("13800000002")
                .setEnabled(true)
                .build());

        users.add(UserDetail.newBuilder()
                .setId(3L)
                .setUsername("disabled_user")
                .setNickname("停用用户")
                .setMobile("13800000003")
                .setEnabled(false)
                .build());
    }

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    public Optional<UserDetail> findById(Long id) {
        if (Objects.isNull(id) || id <= 0) {
            log.warn("用户查询参数非法，id={}", id);
            return Optional.empty();
        }

        return users.stream()
                .filter(user -> Objects.equals(user.getId(), id))
                .findFirst();
    }

    /**
     * 查询用户列表
     *
     * @param enabledOnly 是否只查询启用用户
     * @param pageSize    返回数量
     * @return 用户列表
     */
    public List<UserDetail> listUsers(boolean enabledOnly, int pageSize) {
        int safePageSize = pageSize <= 0 ? 10 : pageSize;

        List<UserDetail> result = users.stream()
                .filter(user -> !enabledOnly || user.getEnabled())
                .limit(safePageSize)
                .toList();

        if (CollUtil.isEmpty(result)) {
            log.info("未查询到用户数据，enabledOnly={}, pageSize={}", enabledOnly, pageSize);
        }

        return result;
    }

    /**
     * 校验导入用户名
     *
     * @param username 用户名
     * @return 是否有效
     */
    public boolean validUsername(String username) {
        return StrUtil.isNotBlank(username) && username.length() >= 3;
    }
}
```

下面是完整的 gRPC 服务实现类，包含 Unary、Server Streaming、Client Streaming 和 Bidirectional Streaming 四种接口实现。

文件位置：`src/main/java/io/github/atengk/grpc/server/UserGrpcService.java`

```java
package io.github.atengk.grpc.server;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.grpc.proto.user.v1.BatchGetUserRequest;
import io.github.atengk.grpc.proto.user.v1.BatchGetUserResponse;
import io.github.atengk.grpc.proto.user.v1.GetUserRequest;
import io.github.atengk.grpc.proto.user.v1.GetUserResponse;
import io.github.atengk.grpc.proto.user.v1.ImportUserRequest;
import io.github.atengk.grpc.proto.user.v1.ImportUserResponse;
import io.github.atengk.grpc.proto.user.v1.StreamUsersRequest;
import io.github.atengk.grpc.proto.user.v1.SyncUserStatusRequest;
import io.github.atengk.grpc.proto.user.v1.SyncUserStatusResponse;
import io.github.atengk.grpc.proto.user.v1.UserDetail;
import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import io.github.atengk.grpc.support.UserMockRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户 gRPC 服务端实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserMockRepository userMockRepository;

    /**
     * Unary RPC：根据用户 ID 查询用户
     *
     * @param request          查询请求
     * @param responseObserver 响应观察者
     */
    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        long userId = request.getId();
        log.info("收到 gRPC 用户查询请求，userId={}", userId);

        Optional<UserDetail> optionalUser = userMockRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("用户不存在，userId=" + userId)
                    .asRuntimeException());
            return;
        }

        GetUserResponse response = GetUserResponse.newBuilder()
                .setUser(optionalUser.get())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Unary RPC：批量查询用户
     *
     * @param request          批量查询请求
     * @param responseObserver 响应观察者
     */
    @Override
    public void batchGetUser(BatchGetUserRequest request, StreamObserver<BatchGetUserResponse> responseObserver) {
        List<Long> ids = request.getIdsList();

        if (CollUtil.isEmpty(ids)) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("用户 ID 集合不能为空")
                    .asRuntimeException());
            return;
        }

        log.info("收到 gRPC 批量用户查询请求，数量={}", ids.size());

        BatchGetUserResponse.Builder responseBuilder = BatchGetUserResponse.newBuilder();
        ids.forEach(id -> userMockRepository.findById(id).ifPresent(responseBuilder::addUsers));

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    /**
     * Server Streaming RPC：服务端流式返回用户列表
     *
     * @param request          流式查询请求
     * @param responseObserver 响应观察者
     */
    @Override
    public void streamUsers(StreamUsersRequest request, StreamObserver<UserDetail> responseObserver) {
        log.info("收到 gRPC 用户流式查询请求，enabledOnly={}, pageSize={}",
                request.getEnabledOnly(), request.getPageSize());

        List<UserDetail> users = userMockRepository.listUsers(request.getEnabledOnly(), request.getPageSize());
        for (UserDetail user : users) {
            responseObserver.onNext(user);
        }

        responseObserver.onCompleted();
    }

    /**
     * Client Streaming RPC：客户端流式导入用户
     *
     * @param responseObserver 响应观察者
     * @return 请求观察者
     */
    @Override
    public StreamObserver<ImportUserRequest> importUsers(StreamObserver<ImportUserResponse> responseObserver) {
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        return new StreamObserver<>() {

            @Override
            public void onNext(ImportUserRequest request) {
                if (userMockRepository.validUsername(request.getUsername())) {
                    successCount.incrementAndGet();
                    log.info("接收用户导入数据，username={}", request.getUsername());
                } else {
                    failureCount.incrementAndGet();
                    log.warn("用户导入数据校验失败，username={}", request.getUsername());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("客户端流式导入用户异常", throwable);
            }

            @Override
            public void onCompleted() {
                ImportUserResponse response = ImportUserResponse.newBuilder()
                        .setSuccessCount(successCount.get())
                        .setFailureCount(failureCount.get())
                        .build();

                log.info("客户端流式导入用户完成，successCount={}, failureCount={}",
                        successCount.get(), failureCount.get());

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    /**
     * Bidirectional Streaming RPC：双向流式同步用户状态
     *
     * @param responseObserver 响应观察者
     * @return 请求观察者
     */
    @Override
    public StreamObserver<SyncUserStatusRequest> syncUserStatus(StreamObserver<SyncUserStatusResponse> responseObserver) {
        return new StreamObserver<>() {

            @Override
            public void onNext(SyncUserStatusRequest request) {
                long userId = request.getId();
                boolean enabled = request.getEnabled();

                log.info("收到用户状态同步请求，userId={}, enabled={}", userId, enabled);

                SyncUserStatusResponse response = SyncUserStatusResponse.newBuilder()
                        .setId(userId)
                        .setSuccess(userId > 0)
                        .setMessage(userId > 0 ? "同步成功" : "用户 ID 非法")
                        .build();

                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("双向流式同步用户状态异常", throwable);
            }

            @Override
            public void onCompleted() {
                log.info("双向流式同步用户状态完成");
                responseObserver.onCompleted();
            }
        };
    }
}
```

服务端实现的关键点如下。

| 方法             | 类型                    | 说明                                                      |
| ---------------- | ----------------------- | --------------------------------------------------------- |
| `getUser`        | Unary                   | 调用一次 `onNext` 返回结果，再调用 `onCompleted` 完成响应 |
| `batchGetUser`   | Unary                   | 普通批量查询，适合请求量可控的批量接口                    |
| `streamUsers`    | Server Streaming        | 服务端多次调用 `onNext`，最后调用 `onCompleted`           |
| `importUsers`    | Client Streaming        | 返回 `StreamObserver<Request>` 接收客户端多次请求         |
| `syncUserStatus` | Bidirectional Streaming | 服务端接收一个请求后可以立即返回一个响应                  |

服务端异常建议使用 `Status` 转换为 gRPC 标准异常。常用状态码包括 `INVALID_ARGUMENT`、`NOT_FOUND`、`PERMISSION_DENIED`、`UNAUTHENTICATED`、`INTERNAL` 等。业务异常不要直接抛出普通 RuntimeException，否则客户端收到的错误信息不够稳定，后续应在“异常处理”章节统一封装。

### Unary RPC 接口实现

Unary RPC 是最常用的 gRPC 调用方式，一个请求对应一个响应。查询详情、创建数据、修改数据、删除数据、批量查询等普通业务接口都可以优先使用 Unary RPC。

典型实现流程如下：

```text
接收请求 -> 参数校验 -> 调用业务逻辑 -> 构造响应 -> onNext -> onCompleted
```

最小实现示例如下。

```java
@Override
public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
    long userId = request.getId();

    if (userId <= 0) {
        responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("用户 ID 必须大于 0")
                .asRuntimeException());
        return;
    }

    UserDetail user = UserDetail.newBuilder()
            .setId(userId)
            .setUsername("ateng")
            .setNickname("阿腾")
            .setEnabled(true)
            .build();

    GetUserResponse response = GetUserResponse.newBuilder()
            .setUser(user)
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
}
```

Unary RPC 的注意事项如下。

| 注意点         | 说明                                           |
| -------------- | ---------------------------------------------- |
| 必须结束响应   | 成功时调用 `onCompleted`，失败时调用 `onError` |
| 不要多次响应   | Unary RPC 正常情况下只调用一次 `onNext`        |
| 参数校验要前置 | 非法参数建议返回 `INVALID_ARGUMENT`            |
| 查询不到数据   | 建议返回 `NOT_FOUND`，不要返回空对象掩盖问题   |
| 业务异常       | 建议统一转换为 gRPC `Status`                   |

### Server Streaming 接口实现

Server Streaming RPC 是一个请求对应多个响应。客户端发起一次请求后，服务端可以持续推送多条结果。它适合返回大列表、任务进度、日志片段、实时状态等场景。

典型实现流程如下：

```text
接收请求 -> 查询或分批生成数据 -> 多次 onNext -> onCompleted
```

示例实现如下。

```java
@Override
public void streamUsers(StreamUsersRequest request, StreamObserver<UserDetail> responseObserver) {
    int pageSize = request.getPageSize() <= 0 ? 10 : request.getPageSize();

    List<UserDetail> users = userMockRepository.listUsers(request.getEnabledOnly(), pageSize);
    for (UserDetail user : users) {
        responseObserver.onNext(user);
    }

    responseObserver.onCompleted();
}
```

Server Streaming 的注意事项如下。

| 注意点       | 说明                                         |
| ------------ | -------------------------------------------- |
| 控制返回数量 | 必须限制单次流式返回规模，避免长时间占用连接 |
| 处理中断     | 客户端取消时服务端应尽快停止推送             |
| 逐条发送     | 每条数据调用一次 `onNext`                    |
| 正常结束     | 全部发送完成后调用 `onCompleted`             |
| 异常结束     | 出现异常时调用 `onError`                     |

如果流式返回的数据来自数据库分页查询，不建议一次性查出全部数据后再推送。更合理的做法是分页查询、分批发送，并结合超时、取消信号和最大数量限制。

### Client Streaming 接口实现

Client Streaming RPC 是多个请求对应一个响应。服务端返回一个 `StreamObserver<Request>`，客户端可以多次调用 `onNext` 发送数据，最后调用 `onCompleted` 表示发送完成。gRPC Java 官方文档也说明，客户端流式和双向流式方法在服务端签名上都会返回 `StreamObserver<RequestType>`。([gRPC](https://grpc.io/docs/languages/java/generated-code/?utm_source=chatgpt.com))

典型实现流程如下：

```text
返回请求观察者 -> 多次接收 onNext -> 统计或聚合数据 -> onCompleted 时返回最终响应
```

示例实现如下。

```java
@Override
public StreamObserver<ImportUserRequest> importUsers(StreamObserver<ImportUserResponse> responseObserver) {
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger failureCount = new AtomicInteger();

    return new StreamObserver<>() {

        @Override
        public void onNext(ImportUserRequest request) {
            if (userMockRepository.validUsername(request.getUsername())) {
                successCount.incrementAndGet();
                log.info("导入用户数据校验成功，username={}", request.getUsername());
            } else {
                failureCount.incrementAndGet();
                log.warn("导入用户数据校验失败，username={}", request.getUsername());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("导入用户流异常", throwable);
        }

        @Override
        public void onCompleted() {
            ImportUserResponse response = ImportUserResponse.newBuilder()
                    .setSuccessCount(successCount.get())
                    .setFailureCount(failureCount.get())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    };
}
```

Client Streaming 的注意事项如下。

| 注意点                       | 说明                                           |
| ---------------------------- | ---------------------------------------------- |
| 聚合状态要线程安全           | 示例使用 `AtomicInteger` 统计成功和失败数量    |
| 不要在 `onNext` 中长时间阻塞 | 大批量导入建议异步落库或分批缓冲               |
| `onCompleted` 返回最终结果   | 客户端发送结束后，服务端再返回汇总响应         |
| `onError` 只记录或释放资源   | 客户端异常后通常不再继续返回正常响应           |
| 需要限制流大小               | 通过业务规则限制最大条数、最大消息体、超时时间 |

### Bidirectional Streaming 接口实现

Bidirectional Streaming RPC 是多个请求对应多个响应。客户端和服务端都可以持续发送消息，适合实时同步、长连接任务交互、协同状态更新等场景。

典型实现流程如下：

```text
服务端返回请求观察者 -> 每次收到客户端消息 -> 可立即返回响应 -> 客户端完成后服务端完成响应流
```

示例实现如下。

```java
@Override
public StreamObserver<SyncUserStatusRequest> syncUserStatus(StreamObserver<SyncUserStatusResponse> responseObserver) {
    return new StreamObserver<>() {

        @Override
        public void onNext(SyncUserStatusRequest request) {
            long userId = request.getId();

            SyncUserStatusResponse response = SyncUserStatusResponse.newBuilder()
                    .setId(userId)
                    .setSuccess(userId > 0)
                    .setMessage(userId > 0 ? "同步成功" : "用户 ID 非法")
                    .build();

            responseObserver.onNext(response);
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("用户状态双向同步异常", throwable);
        }

        @Override
        public void onCompleted() {
            responseObserver.onCompleted();
        }
    };
}
```

Bidirectional Streaming 的注意事项如下。

| 注意点                   | 说明                                               |
| ------------------------ | -------------------------------------------------- |
| 请求和响应不要求一一对应 | 可以一个请求返回一个响应，也可以多个请求聚合后返回 |
| 必须设计结束条件         | 否则长连接可能长期占用服务端资源                   |
| 需要处理客户端取消       | 客户端断开后服务端应释放资源                       |
| 适合状态同步             | 不建议把普通 CRUD 强行设计成双向流式               |
| 可观测性更重要           | 需要记录连接建立、消息数量、异常、耗时和关闭原因   |

## gRPC 客户端开发

本章节用于说明 Spring gRPC 客户端的开发方式。客户端核心是把 Protobuf 生成的 Stub 绑定到一个 gRPC Channel。Spring gRPC 提供 `GrpcChannelFactory` 创建命名 Channel，也支持通过 `@ImportGrpcClients` 自动导入 Stub Bean。([Home](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

客户端常用文件结构如下。

```text
src/main/java/io/github/atengk/grpc
├── client
│   └── UserGrpcClient.java
└── config
    └── UserGrpcClientConfig.java
```

客户端配置文件如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  grpc:
    client:
      channels:
        user-service:
          # 用户服务 gRPC 地址，生产环境建议使用服务发现地址或域名
          address: 127.0.0.1:9090
```

### Blocking Stub 使用

Blocking Stub 是阻塞式客户端调用方式。调用线程会等待服务端返回结果，适合后台管理、同步业务流程、简单服务间调用等场景。gRPC Java 官方文档说明，Blocking Stub 支持 Unary 和 Server Streaming，不支持 Client Streaming 和 Bidirectional Streaming。([gRPC](https://grpc.io/docs/languages/java/generated-code/?utm_source=chatgpt.com))

下面的客户端类演示 Blocking Stub 调用 Unary RPC 和 Server Streaming RPC。

文件位置：`src/main/java/io/github/atengk/grpc/client/UserGrpcClient.java`

```java
package io.github.atengk.grpc.client;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.grpc.proto.user.v1.GetUserRequest;
import io.github.atengk.grpc.proto.user.v1.GetUserResponse;
import io.github.atengk.grpc.proto.user.v1.StreamUsersRequest;
import io.github.atengk.grpc.proto.user.v1.UserDetail;
import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 用户 gRPC 客户端封装
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserGrpcClient {

    private final UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    /**
     * 阻塞式查询用户
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    public UserDetail getUser(Long userId) {
        try {
            GetUserRequest request = GetUserRequest.newBuilder()
                    .setId(userId)
                    .build();

            GetUserResponse response = userServiceBlockingStub.getUser(request);
            log.info("gRPC 用户查询成功，userId={}", userId);
            return response.getUser();
        } catch (StatusRuntimeException e) {
            log.error("gRPC 用户查询失败，userId={}, status={}", userId, e.getStatus(), e);
            throw e;
        }
    }

    /**
     * 阻塞式流式查询用户列表
     *
     * @param enabledOnly 是否只查询启用用户
     * @param pageSize    返回数量
     * @return 用户列表
     */
    public List<UserDetail> streamUsers(boolean enabledOnly, int pageSize) {
        StreamUsersRequest request = StreamUsersRequest.newBuilder()
                .setEnabledOnly(enabledOnly)
                .setPageSize(pageSize)
                .build();

        Iterator<UserDetail> iterator = userServiceBlockingStub.streamUsers(request);
        List<UserDetail> users = new ArrayList<>();

        while (iterator.hasNext()) {
            users.add(iterator.next());
        }

        if (CollUtil.isEmpty(users)) {
            log.info("gRPC 流式查询用户结果为空，enabledOnly={}, pageSize={}", enabledOnly, pageSize);
        } else {
            log.info("gRPC 流式查询用户成功，数量={}", users.size());
        }

        return users;
    }
}
```

Blocking Stub 的典型使用方式如下。

```java
UserDetail user = userGrpcClient.getUser(1L);
List<UserDetail> users = userGrpcClient.streamUsers(true, 10);
```

Blocking Stub 的注意事项如下。

| 注意点                 | 说明                                     |
| ---------------------- | ---------------------------------------- |
| 调用线程会阻塞         | 不建议在高并发事件循环线程中直接使用     |
| 适合简单同步调用       | 服务间同步查询、后台任务、管理接口较合适 |
| 需要设置超时           | 生产环境建议使用 `withDeadlineAfter`     |
| 不支持客户端流和双向流 | 流式写入需要使用 Async Stub              |
| 异常类型               | 通常捕获 `StatusRuntimeException`        |

为防止调用长时间阻塞，可以在调用前设置 Deadline。

```java
GetUserResponse response = userServiceBlockingStub
        .withDeadlineAfter(3, java.util.concurrent.TimeUnit.SECONDS)
        .getUser(request);
```

### Async Stub 使用

Async Stub 是异步客户端调用方式，通过 `StreamObserver` 接收响应。它支持 Unary、Server Streaming、Client Streaming 和 Bidirectional Streaming 四种 RPC 类型，适合异步处理、流式上传、双向通信等场景。([gRPC](https://grpc.io/docs/languages/java/generated-code/?utm_source=chatgpt.com))

下面的客户端类演示 Async Stub 的三种典型用法：异步 Unary、客户端流式导入、双向流式同步。

文件位置：`src/main/java/io/github/atengk/grpc/client/UserGrpcAsyncClient.java`

```java
package io.github.atengk.grpc.client;

import io.github.atengk.grpc.proto.user.v1.GetUserRequest;
import io.github.atengk.grpc.proto.user.v1.GetUserResponse;
import io.github.atengk.grpc.proto.user.v1.ImportUserRequest;
import io.github.atengk.grpc.proto.user.v1.ImportUserResponse;
import io.github.atengk.grpc.proto.user.v1.SyncUserStatusRequest;
import io.github.atengk.grpc.proto.user.v1.SyncUserStatusResponse;
import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 用户 gRPC 异步客户端封装
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserGrpcAsyncClient {

    private final UserServiceGrpc.UserServiceStub userServiceStub;

    /**
     * 异步查询用户
     *
     * @param userId 用户 ID
     */
    public void getUserAsync(Long userId) {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId(userId)
                .build();

        userServiceStub.getUser(request, new StreamObserver<>() {

            @Override
            public void onNext(GetUserResponse response) {
                log.info("异步 gRPC 用户查询成功，userId={}, username={}",
                        response.getUser().getId(), response.getUser().getUsername());
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("异步 gRPC 用户查询失败，userId={}", userId, throwable);
            }

            @Override
            public void onCompleted() {
                log.info("异步 gRPC 用户查询完成，userId={}", userId);
            }
        });
    }

    /**
     * 客户端流式导入用户
     *
     * @param requests 导入请求列表
     * @throws InterruptedException 等待响应被中断
     */
    public void importUsers(List<ImportUserRequest> requests) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<ImportUserResponse> responseObserver = new StreamObserver<>() {

            @Override
            public void onNext(ImportUserResponse response) {
                log.info("用户导入完成，successCount={}, failureCount={}",
                        response.getSuccessCount(), response.getFailureCount());
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("客户端流式导入用户失败", throwable);
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("客户端流式导入用户响应完成");
                latch.countDown();
            }
        };

        StreamObserver<ImportUserRequest> requestObserver = userServiceStub.importUsers(responseObserver);

        for (ImportUserRequest request : requests) {
            requestObserver.onNext(request);
        }

        requestObserver.onCompleted();

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        if (!completed) {
            log.warn("客户端流式导入用户等待响应超时");
        }
    }

    /**
     * 双向流式同步用户状态
     *
     * @param requests 用户状态同步请求列表
     * @throws InterruptedException 等待响应被中断
     */
    public void syncUserStatus(List<SyncUserStatusRequest> requests) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<SyncUserStatusResponse> responseObserver = new StreamObserver<>() {

            @Override
            public void onNext(SyncUserStatusResponse response) {
                log.info("用户状态同步响应，userId={}, success={}, message={}",
                        response.getId(), response.getSuccess(), response.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("双向流式同步用户状态失败", throwable);
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("双向流式同步用户状态响应完成");
                latch.countDown();
            }
        };

        StreamObserver<SyncUserStatusRequest> requestObserver = userServiceStub.syncUserStatus(responseObserver);

        for (SyncUserStatusRequest request : requests) {
            requestObserver.onNext(request);
        }

        requestObserver.onCompleted();

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        if (!completed) {
            log.warn("双向流式同步用户状态等待响应超时");
        }
    }
}
```

Async Stub 的注意事项如下。

| 注意点             | 说明                                                     |
| ------------------ | -------------------------------------------------------- |
| 响应通过回调处理   | 业务结果在 `onNext`、`onError`、`onCompleted` 中处理     |
| 支持所有 RPC 类型  | 包括客户端流和双向流                                     |
| 流式调用要主动完成 | 客户端发送完成后必须调用 `requestObserver.onCompleted()` |
| 注意线程等待       | 示例用 `CountDownLatch` 仅用于演示或测试                 |
| 生产环境需封装回调 | 建议转换为业务事件、CompletableFuture 或响应式流         |

### Future Stub 使用

Future Stub 用于返回 `ListenableFuture`，适合 Unary RPC 的异步调用。gRPC Java 官方文档说明，Future Stub 只支持 Unary，不支持 Streaming RPC。([gRPC](https://grpc.io/docs/languages/java/generated-code/?utm_source=chatgpt.com))

下面示例将 `ListenableFuture` 转换为 `CompletableFuture`，便于在现代 Java 代码中组合异步流程。

文件位置：`src/main/java/io/github/atengk/grpc/client/UserGrpcFutureClient.java`

```java
package io.github.atengk.grpc.client;

import com.google.common.util.concurrent.ListenableFuture;
import io.github.atengk.grpc.proto.user.v1.GetUserRequest;
import io.github.atengk.grpc.proto.user.v1.GetUserResponse;
import io.github.atengk.grpc.proto.user.v1.UserDetail;
import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * 用户 gRPC Future 客户端封装
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserGrpcFutureClient {

    private final UserServiceGrpc.UserServiceFutureStub userServiceFutureStub;

    /**
     * Future 方式查询用户
     *
     * @param userId 用户 ID
     * @return 用户详情 Future
     */
    public CompletableFuture<UserDetail> getUserFuture(Long userId) {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId(userId)
                .build();

        ListenableFuture<GetUserResponse> listenableFuture = userServiceFutureStub.getUser(request);
        CompletableFuture<UserDetail> completableFuture = new CompletableFuture<>();

        listenableFuture.addListener(() -> {
            try {
                GetUserResponse response = listenableFuture.get();
                log.info("Future gRPC 用户查询成功，userId={}", userId);
                completableFuture.complete(response.getUser());
            } catch (Exception e) {
                log.error("Future gRPC 用户查询失败，userId={}", userId, e);
                completableFuture.completeExceptionally(e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());

        return completableFuture;
    }
}
```

如果项目暂不使用虚拟线程，可以将 `Executors.newVirtualThreadPerTaskExecutor()` 替换为业务线程池 Bean，不建议在高频调用中反复创建临时线程池。更规范的写法是在配置类中声明统一的 `Executor`。

### 客户端 Bean 配置

客户端 Bean 配置用于统一创建 Blocking Stub、Async Stub 和 Future Stub。Spring gRPC 客户端文档说明，可以注入 `GrpcChannelFactory`，再通过命名 Channel 创建 `ManagedChannel`，最后将 Channel 绑定到 Protobuf 生成的 Stub。([Home](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

下面是手动配置三类 Stub 的完整示例。

文件位置：`src/main/java/io/github/atengk/grpc/config/UserGrpcClientConfig.java`

```java
package io.github.atengk.grpc.config;

import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * 用户 gRPC 客户端配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class UserGrpcClientConfig {

    /**
     * 用户服务 Blocking Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 用户服务 Blocking Stub
     */
    @Bean
    public UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub(GrpcChannelFactory channelFactory) {
        return UserServiceGrpc.newBlockingStub(channelFactory.createChannel("user-service"));
    }

    /**
     * 用户服务 Async Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 用户服务 Async Stub
     */
    @Bean
    public UserServiceGrpc.UserServiceStub userServiceStub(GrpcChannelFactory channelFactory) {
        return UserServiceGrpc.newStub(channelFactory.createChannel("user-service"));
    }

    /**
     * 用户服务 Future Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 用户服务 Future Stub
     */
    @Bean
    public UserServiceGrpc.UserServiceFutureStub userServiceFutureStub(GrpcChannelFactory channelFactory) {
        return UserServiceGrpc.newFutureStub(channelFactory.createChannel("user-service"));
    }
}
```

对应的配置文件如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  grpc:
    client:
      channels:
        user-service:
          # 命名 Channel 地址，UserGrpcClientConfig 中通过 user-service 引用
          address: 127.0.0.1:9090
```

如果希望使用自动 Stub 注册，可以使用 `@ImportGrpcClients`。Spring gRPC 文档说明，`@ImportGrpcClients` 可以扫描 Protobuf 生成的 Stub，并自动创建 Bean；默认通常创建 Blocking Stub，也可以通过属性调整扫描包、目标 Channel、Stub 类型和工厂。([Home](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/grpc/GrpcDemoApplication.java`

```java
package io.github.atengk.grpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;

/**
 * Spring gRPC 示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@ImportGrpcClients(basePackages = "io.github.atengk.grpc.proto.user.v1")
@SpringBootApplication
public class GrpcDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcDemoApplication.class, args);
    }
}
```

手动配置和自动配置的选择建议如下。

| 方式                          | 适用场景                                              |
| ----------------------------- | ----------------------------------------------------- |
| 手动 `@Bean` 创建 Stub        | 需要明确控制每个 Stub、每个 Channel、每个超时和拦截器 |
| `@ImportGrpcClients` 自动导入 | Stub 较多，希望减少配置代码                           |
| 多 Channel 手动配置           | 同一个服务有多个环境、多个集群或不同安全策略          |
| 默认 Channel 自动配置         | 小型项目或测试项目，只有一个目标服务                  |

客户端最终调用示例如下。

```java
UserDetail user = userGrpcClient.getUser(1L);

List<UserDetail> users = userGrpcClient.streamUsers(true, 20);

userGrpcAsyncClient.getUserAsync(1L);

CompletableFuture<UserDetail> future = userGrpcFutureClient.getUserFuture(1L);
```

验证服务端和客户端是否连通，可以先启动服务端，然后在客户端应用中调用 `UserGrpcClient#getUser`。如果启用了 Reflection，也可以使用 `grpcurl` 查看服务列表。

```bash
# 查看服务列表
grpcurl -plaintext 127.0.0.1:9090 list

# 查看用户服务方法
grpcurl -plaintext 127.0.0.1:9090 list ateng.user.v1.UserService
```

`grpcurl -plaintext` 表示使用明文连接，适合本地开发环境；生产环境如果启用了 TLS，需要去掉 `-plaintext` 并配置证书相关参数。



## Spring Boot 集成配置

本章节用于说明 Spring Boot 4 项目中如何配置 Spring gRPC 服务端、客户端 Channel、多服务调用和多环境隔离。Spring gRPC 的服务端配置通常通过 `spring.grpc.server.*` 完成，客户端配置通常通过 `spring.grpc.client.channels.<name>.*` 或 `spring.grpc.client.default-channel.*` 完成；客户端代码可以通过 `GrpcChannelFactory` 创建命名 Channel，再绑定 Protobuf 生成的 Stub。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

### 服务端端口配置

服务端端口用于指定 gRPC Server 的监听地址。Spring gRPC 服务端默认基于 Netty 启动 gRPC 服务，常见端口是 `9090`。如果一个 Spring Boot 应用同时提供 REST 和 gRPC，REST 使用 `server.port`，gRPC 使用 `spring.grpc.server.port`，两者不要混淆。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # Spring MVC / WebFlux HTTP 服务端口
  port: 8080

spring:
  grpc:
    server:
      # gRPC 服务端监听端口
      port: 9090

      reflection:
        # 开发、测试环境建议开启，便于 grpcurl 查看服务和方法
        enabled: true

      health:
        # 启用 gRPC 标准健康检查
        enabled: true
```

如果只提供 gRPC 服务，不提供 REST 接口，可以不引入 Web Starter，此时 `server.port` 不一定会真正启动 HTTP 服务；但 `spring.grpc.server.port` 仍然用于 gRPC 服务监听。

本地验证端口是否监听。

```bash
# 启动应用后查看 9090 端口
lsof -i:9090

# 如果启用了 Reflection，可以查看服务列表
grpcurl -plaintext 127.0.0.1:9090 list
```

`grpcurl -plaintext` 表示使用明文连接，适合本地开发环境。生产环境如果启用 TLS，需要使用证书参数，不应继续使用明文连接。

测试环境中可以将 gRPC 端口设置为 `0`，由系统自动分配可用端口。Spring gRPC 文档说明，测试中可以使用 `spring.grpc.server.port=0` 启用随机端口，并通过 `@LocalGrpcPort` 获取实际端口。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  grpc:
    server:
      # 测试环境使用随机端口，避免本地并发测试端口冲突
      port: 0
```

### 客户端 Channel 配置

客户端 Channel 用于描述远程 gRPC 服务的连接目标。Spring gRPC 客户端文档说明，`GrpcChannelFactory` 可以根据命名 Channel 创建 `ManagedChannel`；命名 Channel 的地址可以通过 `spring.grpc.client.channels.<channel-name>.address` 配置。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  grpc:
    client:
      channels:
        user-service:
          # 用户服务 gRPC 地址
          address: 127.0.0.1:9090
```

客户端 Stub 配置如下。

文件位置：`src/main/java/io/github/atengk/grpc/config/UserGrpcClientConfig.java`

```java
package io.github.atengk.grpc.config;

import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * 用户 gRPC 客户端配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class UserGrpcClientConfig {

    /**
     * 创建用户服务阻塞式 Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 用户服务阻塞式 Stub
     */
    @Bean
    public UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub(GrpcChannelFactory channelFactory) {
        return UserServiceGrpc.newBlockingStub(channelFactory.createChannel("user-service"));
    }

    /**
     * 创建用户服务异步 Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 用户服务异步 Stub
     */
    @Bean
    public UserServiceGrpc.UserServiceStub userServiceStub(GrpcChannelFactory channelFactory) {
        return UserServiceGrpc.newStub(channelFactory.createChannel("user-service"));
    }

    /**
     * 创建用户服务 Future Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 用户服务 Future Stub
     */
    @Bean
    public UserServiceGrpc.UserServiceFutureStub userServiceFutureStub(GrpcChannelFactory channelFactory) {
        return UserServiceGrpc.newFutureStub(channelFactory.createChannel("user-service"));
    }
}
```

`GrpcChannelFactory#createChannel` 的参数既可以是用户配置的命名 Channel，也可以是符合 gRPC NameResolver 规则的目标字符串；返回的 `ManagedChannel` 会应用全局注册的客户端拦截器。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/api/java/org/springframework/grpc/client/GrpcChannelFactory.html?utm_source=chatgpt.com))

如果希望配置默认 Channel，可以使用 `spring.grpc.client.default-channel.*`。当创建 Stub 时未显式命中某个命名 Channel，默认 Channel 可以作为兜底配置。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  grpc:
    client:
      default-channel:
        # 默认 gRPC 目标地址，适合单服务客户端或测试场景
        address: 127.0.0.1:9090
```

### 多服务客户端配置

多服务客户端配置适合一个应用同时调用多个 gRPC 服务。例如订单服务需要调用用户服务、库存服务、支付服务和文件服务。推荐每个远程服务使用独立命名 Channel，避免在 Java 代码中硬编码 IP、端口和服务名。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  grpc:
    client:
      channels:
        user-service:
          # 用户服务地址
          address: 127.0.0.1:9090

        order-service:
          # 订单服务地址
          address: 127.0.0.1:9091

        file-service:
          # 文件服务地址
          address: 127.0.0.1:9092

        payment-service:
          # 支付服务地址
          address: 127.0.0.1:9093
```

多服务 Stub 配置如下。

文件位置：`src/main/java/io/github/atengk/grpc/config/MultiGrpcClientConfig.java`

```java
package io.github.atengk.grpc.config;

import io.github.atengk.grpc.proto.file.v1.FileServiceGrpc;
import io.github.atengk.grpc.proto.order.v1.OrderServiceGrpc;
import io.github.atengk.grpc.proto.payment.v1.PaymentServiceGrpc;
import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * 多服务 gRPC 客户端配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class MultiGrpcClientConfig {

    /**
     * 用户服务阻塞式 Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 用户服务 Stub
     */
    @Bean
    public UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub(GrpcChannelFactory channelFactory) {
        return UserServiceGrpc.newBlockingStub(channelFactory.createChannel("user-service"));
    }

    /**
     * 订单服务阻塞式 Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 订单服务 Stub
     */
    @Bean
    public OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub(GrpcChannelFactory channelFactory) {
        return OrderServiceGrpc.newBlockingStub(channelFactory.createChannel("order-service"));
    }

    /**
     * 文件服务阻塞式 Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 文件服务 Stub
     */
    @Bean
    public FileServiceGrpc.FileServiceBlockingStub fileServiceBlockingStub(GrpcChannelFactory channelFactory) {
        return FileServiceGrpc.newBlockingStub(channelFactory.createChannel("file-service"));
    }

    /**
     * 支付服务阻塞式 Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 支付服务 Stub
     */
    @Bean
    public PaymentServiceGrpc.PaymentServiceBlockingStub paymentServiceBlockingStub(GrpcChannelFactory channelFactory) {
        return PaymentServiceGrpc.newBlockingStub(channelFactory.createChannel("payment-service"));
    }
}
```

如果不同服务需要不同超时策略，可以在业务调用处使用 Stub 的不可变派生能力配置 Deadline。

```java
GetUserResponse response = userServiceBlockingStub
        .withDeadlineAfter(3, java.util.concurrent.TimeUnit.SECONDS)
        .getUser(request);
```

如果需要对所有 Channel 或某个 Channel 做更底层的 Netty 参数、重试、负载均衡或拦截器配置，可以使用 `GrpcChannelBuilderCustomizer`。Spring gRPC 客户端文档说明，`GrpcChannelBuilderCustomizer` 可以全局应用到自动配置的 `GrpcChannelFactory`，也可以在单个 Channel 创建时通过选项应用。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

### Profile 环境隔离

Profile 环境隔离用于区分开发、测试、预发和生产环境下的 gRPC 地址、端口、Reflection、TLS、日志和超时策略。推荐将公共配置放在 `application.yml`，环境差异配置放在 `application-dev.yml`、`application-test.yml`、`application-prod.yml`。

公共配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    name: spring-grpc-demo

  profiles:
    # 默认启用 dev 环境
    active: dev

  grpc:
    server:
      # 默认 gRPC 端口
      port: 9090

    client:
      channels:
        user-service:
          # 默认用户服务地址，具体环境可覆盖
          address: 127.0.0.1:9090
```

开发环境配置如下。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  grpc:
    server:
      # 开发环境使用固定端口，方便 grpcurl 调试
      port: 9090

      reflection:
        # 开发环境开启服务反射
        enabled: true

      health:
        # 开发环境开启健康检查
        enabled: true

    client:
      channels:
        user-service:
          # 本地用户服务地址
          address: 127.0.0.1:9090
```

测试环境配置如下。

文件位置：`src/main/resources/application-test.yml`

```yaml
spring:
  grpc:
    server:
      # 测试环境使用随机端口，避免并发测试冲突
      port: 0

      reflection:
        # 测试环境可按需开启
        enabled: true

    client:
      channels:
        user-service:
          # 集成测试中可以使用 ${local.grpc.port} 连接随机端口
          address: 127.0.0.1:${local.grpc.port}
```

生产环境配置如下。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  grpc:
    server:
      # 生产 gRPC 端口
      port: 9090

      reflection:
        # 生产环境通常关闭 Reflection，避免暴露服务结构
        enabled: false

      health:
        # 生产环境建议开启健康检查，供探针或服务治理使用
        enabled: true

    client:
      channels:
        user-service:
          # 生产环境建议使用服务发现地址、内网域名或网关地址
          address: user-service.grpc.svc.cluster.local:9090

        order-service:
          # 订单服务生产地址
          address: order-service.grpc.svc.cluster.local:9090
```

启动时指定环境。

```bash
# 开发环境
java -jar spring-grpc-demo.jar --spring.profiles.active=dev

# 测试环境
java -jar spring-grpc-demo.jar --spring.profiles.active=test

# 生产环境
java -jar spring-grpc-demo.jar --spring.profiles.active=prod
```

Profile 隔离建议如下。

| 配置项                    | dev        | test               | prod               |
| ------------------------- | ---------- | ------------------ | ------------------ |
| `spring.grpc.server.port` | 固定端口   | 随机端口或测试端口 | 固定端口           |
| `reflection.enabled`      | 开启       | 按需开启           | 通常关闭           |
| `health.enabled`          | 开启       | 开启               | 开启               |
| 客户端地址                | 本地地址   | 测试服务地址       | 服务发现或内网域名 |
| TLS                       | 可关闭     | 按需开启           | 建议开启           |
| 日志级别                  | DEBUG/INFO | INFO               | INFO/WARN          |

## 数据转换与业务封装

本章节用于说明业务 DTO、Java 枚举、Java 时间类型与 Protobuf 对象之间的转换方式。gRPC 的生成类是通信契约对象，不建议在业务层、数据库层和 Controller 层直接大面积使用；更合理的方式是：外部通信使用 Protobuf，内部业务使用 DTO、Entity、VO，通过专门的 Converter 做边界转换。

### DTO 与 Protobuf 对象转换

DTO 与 Protobuf 对象转换用于隔离通信模型和业务模型。Protobuf 生成类通常是不可变对象，需要通过 `newBuilder()` 构造；业务 DTO 则更适合在 Service、Controller、Mapper 等内部层流转。

示例文件结构如下。

```text
src/main/java/io/github/atengk/grpc
├── dto
│   └── UserDTO.java
├── enums
│   └── UserGenderEnum.java
└── converter
    └── UserProtoConverter.java
```

业务 DTO 示例。

文件位置：`src/main/java/io/github/atengk/grpc/dto/UserDTO.java`

```java
package io.github.atengk.grpc.dto;

import io.github.atengk.grpc.enums.UserGenderEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户业务 DTO
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 性别
     */
    private UserGenderEnum gender;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
```

性别枚举示例。

文件位置：`src/main/java/io/github/atengk/grpc/enums/UserGenderEnum.java`

```java
package io.github.atengk.grpc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户性别枚举
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@AllArgsConstructor
public enum UserGenderEnum {

    /**
     * 未知
     */
    UNKNOWN(0, "未知"),

    /**
     * 男
     */
    MALE(1, "男"),

    /**
     * 女
     */
    FEMALE(2, "女");

    private final Integer code;

    private final String description;
}
```

下面的转换器用于在业务 DTO 和 Protobuf `UserDetail` 之间进行双向转换。

文件位置：`src/main/java/io/github/atengk/grpc/converter/UserProtoConverter.java`

```java
package io.github.atengk.grpc.converter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.dto.UserDTO;
import io.github.atengk.grpc.enums.UserGenderEnum;
import io.github.atengk.grpc.proto.user.v1.Gender;
import io.github.atengk.grpc.proto.user.v1.UserDetail;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;

/**
 * 用户 Protobuf 转换器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@UtilityClass
public class UserProtoConverter {

    /**
     * DTO 转 Protobuf 对象
     *
     * @param userDTO 用户 DTO
     * @return 用户 Protobuf 对象
     */
    public UserDetail toProto(UserDTO userDTO) {
        if (ObjectUtil.isNull(userDTO)) {
            return UserDetail.getDefaultInstance();
        }

        return UserDetail.newBuilder()
                .setId(ObjectUtil.defaultIfNull(userDTO.getId(), 0L))
                .setUsername(StrUtil.blankToDefault(userDTO.getUsername(), ""))
                .setNickname(StrUtil.blankToDefault(userDTO.getNickname(), ""))
                .setGender(toProtoGender(userDTO.getGender()))
                .setMobile(StrUtil.blankToDefault(userDTO.getMobile(), ""))
                .setEmail(StrUtil.blankToDefault(userDTO.getEmail(), ""))
                .setEnabled(ObjectUtil.defaultIfNull(userDTO.getEnabled(), false))
                .build();
    }

    /**
     * Protobuf 对象转 DTO
     *
     * @param userDetail 用户 Protobuf 对象
     * @return 用户 DTO
     */
    public UserDTO toDTO(UserDetail userDetail) {
        if (ObjectUtil.isNull(userDetail) || UserDetail.getDefaultInstance().equals(userDetail)) {
            return null;
        }

        return UserDTO.builder()
                .id(userDetail.getId())
                .username(userDetail.getUsername())
                .nickname(userDetail.getNickname())
                .gender(toDTOGender(userDetail.getGender()))
                .mobile(userDetail.getMobile())
                .email(userDetail.getEmail())
                .enabled(userDetail.getEnabled())
                .build();
    }

    /**
     * DTO 集合转 Protobuf 集合
     *
     * @param userDTOList 用户 DTO 集合
     * @return 用户 Protobuf 集合
     */
    public List<UserDetail> toProtoList(List<UserDTO> userDTOList) {
        if (CollUtil.isEmpty(userDTOList)) {
            return Collections.emptyList();
        }

        return userDTOList.stream()
                .map(UserProtoConverter::toProto)
                .toList();
    }

    /**
     * Protobuf 集合转 DTO 集合
     *
     * @param userDetailList 用户 Protobuf 集合
     * @return 用户 DTO 集合
     */
    public List<UserDTO> toDTOList(List<UserDetail> userDetailList) {
        if (CollUtil.isEmpty(userDetailList)) {
            return Collections.emptyList();
        }

        return userDetailList.stream()
                .map(UserProtoConverter::toDTO)
                .toList();
    }

    /**
     * 业务枚举转 Protobuf 枚举
     *
     * @param genderEnum 业务性别枚举
     * @return Protobuf 性别枚举
     */
    public Gender toProtoGender(UserGenderEnum genderEnum) {
        if (ObjectUtil.isNull(genderEnum)) {
            return Gender.GENDER_UNSPECIFIED;
        }

        return switch (genderEnum) {
            case MALE -> Gender.GENDER_MALE;
            case FEMALE -> Gender.GENDER_FEMALE;
            default -> Gender.GENDER_UNSPECIFIED;
        };
    }

    /**
     * Protobuf 枚举转业务枚举
     *
     * @param gender Protobuf 性别枚举
     * @return 业务性别枚举
     */
    public UserGenderEnum toDTOGender(Gender gender) {
        if (ObjectUtil.isNull(gender)) {
            return UserGenderEnum.UNKNOWN;
        }

        return switch (gender) {
            case GENDER_MALE -> UserGenderEnum.MALE;
            case GENDER_FEMALE -> UserGenderEnum.FEMALE;
            default -> UserGenderEnum.UNKNOWN;
        };
    }
}
```

转换器使用建议如下。

| 位置            | 建议                                                   |
| --------------- | ------------------------------------------------------ |
| gRPC 服务实现类 | 接收 Protobuf Request 后转换为业务 DTO                 |
| 业务 Service    | 尽量不要直接依赖 Protobuf 生成类                       |
| 客户端封装类    | 将 Protobuf Response 转换为业务 DTO 后返回             |
| Controller 层   | 不建议直接暴露 Protobuf 对象给 REST API                |
| 数据库层        | Entity 与 Protobuf 之间不要直接互转，建议通过 DTO 中转 |

服务端中使用转换器的示例。

```java
@Override
public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
    UserDTO userDTO = userService.getUserById(request.getId());
    UserDetail userDetail = UserProtoConverter.toProto(userDTO);

    GetUserResponse response = GetUserResponse.newBuilder()
            .setUser(userDetail)
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
}
```

### 枚举映射

枚举映射用于处理业务枚举和 Protobuf 枚举之间的差异。Protobuf `proto3` 枚举的第一个值通常使用 `0`，业务上建议定义为 `UNSPECIFIED` 或 `UNKNOWN`，避免默认值被误判为真实业务值。

proto 枚举定义示例。

文件位置：`src/main/proto/user/user.proto`

```proto
syntax = "proto3";

package ateng.user.v1;

option java_multiple_files = true;
option java_package = "io.github.atengk.grpc.proto.user.v1";
option java_outer_classname = "UserProto";

// 用户性别
enum Gender {
  // 未指定，作为默认值使用
  GENDER_UNSPECIFIED = 0;

  // 男
  GENDER_MALE = 1;

  // 女
  GENDER_FEMALE = 2;
}
```

业务枚举和 Protobuf 枚举不建议共用同一个枚举类。业务枚举可以包含数据库编码、展示文案、业务状态说明；Protobuf 枚举主要用于接口契约和跨语言通信。

如果枚举值较多，可以使用 Hutool 的 `EnumUtil` 或静态 Map 做映射。下面是根据业务 code 转换枚举的示例。

文件位置：`src/main/java/io/github/atengk/grpc/converter/UserGenderConverter.java`

```java
package io.github.atengk.grpc.converter;

import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.grpc.enums.UserGenderEnum;
import io.github.atengk.grpc.proto.user.v1.Gender;
import lombok.experimental.UtilityClass;

/**
 * 用户性别枚举转换器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@UtilityClass
public class UserGenderConverter {

    /**
     * 根据业务编码转换业务枚举
     *
     * @param code 业务编码
     * @return 业务枚举
     */
    public UserGenderEnum fromCode(Integer code) {
        if (ObjectUtil.isNull(code)) {
            return UserGenderEnum.UNKNOWN;
        }

        return EnumUtil.getBy(UserGenderEnum.class, item -> ObjectUtil.equals(item.getCode(), code));
    }

    /**
     * 业务枚举转 Protobuf 枚举
     *
     * @param genderEnum 业务枚举
     * @return Protobuf 枚举
     */
    public Gender toProto(UserGenderEnum genderEnum) {
        if (ObjectUtil.isNull(genderEnum)) {
            return Gender.GENDER_UNSPECIFIED;
        }

        return switch (genderEnum) {
            case MALE -> Gender.GENDER_MALE;
            case FEMALE -> Gender.GENDER_FEMALE;
            default -> Gender.GENDER_UNSPECIFIED;
        };
    }

    /**
     * Protobuf 枚举转业务枚举
     *
     * @param gender Protobuf 枚举
     * @return 业务枚举
     */
    public UserGenderEnum toDTO(Gender gender) {
        if (ObjectUtil.isNull(gender)) {
            return UserGenderEnum.UNKNOWN;
        }

        return switch (gender) {
            case GENDER_MALE -> UserGenderEnum.MALE;
            case GENDER_FEMALE -> UserGenderEnum.FEMALE;
            default -> UserGenderEnum.UNKNOWN;
        };
    }
}
```

枚举映射注意事项如下。

| 注意点                 | 说明                                        |
| ---------------------- | ------------------------------------------- |
| 保留默认值             | Protobuf 枚举第一个值建议使用 `UNSPECIFIED` |
| 不要删除已发布枚举值   | 废弃后可保留名称，业务上不再使用            |
| 不要复用旧枚举编号     | 避免老客户端解析成错误含义                  |
| 业务枚举和接口枚举分层 | 防止数据库编码变化影响接口契约              |
| 未识别枚举要兜底       | 客户端可能收到新服务端返回的新枚举          |

### 时间类型处理

时间类型处理用于统一 Java 时间对象和 Protobuf 时间字段之间的转换方式。Protobuf 常见时间表达有两种：一种是使用 `int64` 表示毫秒时间戳；另一种是使用标准类型 `google.protobuf.Timestamp`。`Timestamp` 表示与时区无关的时间点，基于 Unix epoch，并具备纳秒精度；Protobuf Java 工具类 `Timestamps` 提供 `fromMillis`、`toMillis`、`parse`、`toString` 等方法。([protobuf.dev](https://protobuf.dev/reference/java/api-docs/com/google/protobuf/Timestamp?utm_source=chatgpt.com), [protobuf.dev](https://protobuf.dev/reference/java/api-docs/com/google/protobuf/util/Timestamps?utm_source=chatgpt.com))

推荐在跨语言、跨时区系统中使用 `google.protobuf.Timestamp`。如果系统内部只在 Java 服务之间调用，并且历史接口已经大量使用毫秒时间戳，也可以继续使用 `int64`，但需要统一命名，例如 `created_at_millis`。

proto 中使用 `Timestamp` 的示例。

文件位置：`src/main/proto/user/user.proto`

```proto
syntax = "proto3";

package ateng.user.v1;

import "google/protobuf/timestamp.proto";

option java_multiple_files = true;
option java_package = "io.github.atengk.grpc.proto.user.v1";
option java_outer_classname = "UserProto";

// 用户详情
message UserDetail {
  int64 id = 1;
  string username = 2;
  string nickname = 3;

  // 创建时间
  google.protobuf.Timestamp created_time = 4;

  // 更新时间
  google.protobuf.Timestamp updated_time = 5;
}
```

下面的工具类用于在 `LocalDateTime`、`Instant`、毫秒时间戳和 Protobuf `Timestamp` 之间转换。

文件位置：`src/main/java/io/github/atengk/grpc/converter/GrpcTimeConverter.java`

```java
package io.github.atengk.grpc.converter;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * gRPC 时间类型转换器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@UtilityClass
public class GrpcTimeConverter {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    /**
     * LocalDateTime 转 Protobuf Timestamp
     *
     * @param localDateTime 本地时间
     * @return Protobuf Timestamp
     */
    public Timestamp toTimestamp(LocalDateTime localDateTime) {
        if (ObjectUtil.isNull(localDateTime)) {
            return Timestamp.getDefaultInstance();
        }

        Instant instant = localDateTime.atZone(DEFAULT_ZONE_ID).toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    /**
     * Instant 转 Protobuf Timestamp
     *
     * @param instant 时间点
     * @return Protobuf Timestamp
     */
    public Timestamp toTimestamp(Instant instant) {
        if (ObjectUtil.isNull(instant)) {
            return Timestamp.getDefaultInstance();
        }

        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    /**
     * 毫秒时间戳转 Protobuf Timestamp
     *
     * @param millis 毫秒时间戳
     * @return Protobuf Timestamp
     */
    public Timestamp toTimestamp(Long millis) {
        if (ObjectUtil.isNull(millis) || millis <= 0) {
            return Timestamp.getDefaultInstance();
        }

        return Timestamps.fromMillis(millis);
    }

    /**
     * Protobuf Timestamp 转 LocalDateTime
     *
     * @param timestamp Protobuf Timestamp
     * @return 本地时间
     */
    public LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (ObjectUtil.isNull(timestamp) || Timestamp.getDefaultInstance().equals(timestamp)) {
            return null;
        }

        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return LocalDateTimeUtil.of(instant.atZone(DEFAULT_ZONE_ID));
    }

    /**
     * Protobuf Timestamp 转 Instant
     *
     * @param timestamp Protobuf Timestamp
     * @return 时间点
     */
    public Instant toInstant(Timestamp timestamp) {
        if (ObjectUtil.isNull(timestamp) || Timestamp.getDefaultInstance().equals(timestamp)) {
            return null;
        }

        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    /**
     * Protobuf Timestamp 转毫秒时间戳
     *
     * @param timestamp Protobuf Timestamp
     * @return 毫秒时间戳
     */
    public Long toMillis(Timestamp timestamp) {
        if (ObjectUtil.isNull(timestamp) || Timestamp.getDefaultInstance().equals(timestamp)) {
            return null;
        }

        return Timestamps.toMillis(timestamp);
    }
}
```

转换器在用户对象中的使用示例。

```java
UserDetail userDetail = UserDetail.newBuilder()
        .setId(userDTO.getId())
        .setUsername(userDTO.getUsername())
        .setCreatedTime(GrpcTimeConverter.toTimestamp(userDTO.getCreatedTime()))
        .build();

LocalDateTime createdTime = GrpcTimeConverter.toLocalDateTime(userDetail.getCreatedTime());
```

时间类型处理建议如下。

| 场景                   | 建议                                           |
| ---------------------- | ---------------------------------------------- |
| 跨语言服务             | 优先使用 `google.protobuf.Timestamp`           |
| 只在 Java 服务内部通信 | 可以使用 `Timestamp`，也可以统一使用毫秒时间戳 |
| 与数据库交互           | 业务层使用 `LocalDateTime` 或 `Instant`        |
| 与前端 REST 交互       | 根据接口规范返回字符串时间或毫秒时间戳         |
| 日志和链路追踪         | 推荐使用 ISO-8601 或毫秒时间戳                 |
| 时区处理               | 服务间传输使用时间点，展示层再处理时区         |

不要在 Protobuf 中使用字符串随意表示时间，例如 `string created_time = 1`，除非接口明确要求 RFC 3339 字符串。字符串时间不利于排序、比较、跨语言精度控制和统一校验。

### 统一响应结构设计

统一响应结构用于在业务层表达成功、失败、错误码、错误消息和实际数据。gRPC 本身已经有标准 `Status` 状态码，因此不建议把所有异常都包装成 `code/message/data` 后仍返回 OK。更合理的做法是：参数错误、鉴权失败、资源不存在、系统异常等使用 gRPC `Status`；业务可预期结果可以使用统一响应结构表达。

通用响应 proto 示例。

文件位置：`src/main/proto/common/common.proto`

```proto
syntax = "proto3";

package ateng.common.v1;

option java_multiple_files = true;
option java_package = "io.github.atengk.grpc.proto.common.v1";
option java_outer_classname = "CommonProto";

// 通用响应头
message ResponseHeader {
  // 业务是否成功
  bool success = 1;

  // 业务编码，成功一般为 0
  int32 code = 2;

  // 响应消息
  string message = 3;

  // 请求追踪 ID
  string trace_id = 4;

  // 服务器响应时间戳，毫秒
  int64 timestamp = 5;
}

// 分页信息
message PageInfo {
  // 当前页码
  int32 page_no = 1;

  // 每页大小
  int32 page_size = 2;

  // 总数量
  int64 total = 3;
}
```

业务响应 proto 示例。

文件位置：`src/main/proto/user/user.proto`

```proto
syntax = "proto3";

package ateng.user.v1;

import "common/common.proto";

option java_multiple_files = true;
option java_package = "io.github.atengk.grpc.proto.user.v1";
option java_outer_classname = "UserProto";

// 查询用户响应
message GetUserResponse {
  // 通用响应头
  ateng.common.v1.ResponseHeader header = 1;

  // 用户详情
  UserDetail user = 2;
}

// 查询用户列表响应
message ListUserResponse {
  // 通用响应头
  ateng.common.v1.ResponseHeader header = 1;

  // 分页信息
  ateng.common.v1.PageInfo page = 2;

  // 用户列表
  repeated UserDetail users = 3;
}

// 用户详情
message UserDetail {
  int64 id = 1;
  string username = 2;
  string nickname = 3;
}
```

下面的工厂类用于统一创建响应头。

文件位置：`src/main/java/io/github/atengk/grpc/support/GrpcResponseFactory.java`

```java
package io.github.atengk.grpc.support;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.proto.common.v1.PageInfo;
import io.github.atengk.grpc.proto.common.v1.ResponseHeader;
import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

/**
 * gRPC 统一响应工厂
 *
 * @author Ateng
 * @since 2026-05-05
 */
@UtilityClass
public class GrpcResponseFactory {

    private static final int SUCCESS_CODE = 0;

    private static final int FAILURE_CODE = 500;

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 创建成功响应头
     *
     * @return 成功响应头
     */
    public ResponseHeader successHeader() {
        return ResponseHeader.newBuilder()
                .setSuccess(true)
                .setCode(SUCCESS_CODE)
                .setMessage("操作成功")
                .setTraceId(getTraceId())
                .setTimestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建失败响应头
     *
     * @param code    业务编码
     * @param message 业务消息
     * @return 失败响应头
     */
    public ResponseHeader failureHeader(Integer code, String message) {
        return ResponseHeader.newBuilder()
                .setSuccess(false)
                .setCode(code == null ? FAILURE_CODE : code)
                .setMessage(StrUtil.blankToDefault(message, "操作失败"))
                .setTraceId(getTraceId())
                .setTimestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建分页信息
     *
     * @param pageNo   当前页码
     * @param pageSize 每页大小
     * @param total    总数量
     * @return 分页信息
     */
    public PageInfo pageInfo(Integer pageNo, Integer pageSize, Long total) {
        return PageInfo.newBuilder()
                .setPageNo(pageNo == null ? 1 : pageNo)
                .setPageSize(pageSize == null ? 10 : pageSize)
                .setTotal(total == null ? 0L : total)
                .build();
    }

    /**
     * 获取链路追踪 ID
     *
     * @return 链路追踪 ID
     */
    private String getTraceId() {
        return StrUtil.blankToDefault(MDC.get(TRACE_ID_KEY), "");
    }
}
```

服务端使用统一响应结构的示例。

```java
@Override
public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
    UserDTO userDTO = userService.getUserById(request.getId());

    if (userDTO == null) {
        GetUserResponse response = GetUserResponse.newBuilder()
                .setHeader(GrpcResponseFactory.failureHeader(404, "用户不存在"))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
        return;
    }

    GetUserResponse response = GetUserResponse.newBuilder()
            .setHeader(GrpcResponseFactory.successHeader())
            .setUser(UserProtoConverter.toProto(userDTO))
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
}
```

统一响应结构和 gRPC `Status` 的使用边界如下。

| 类型           | 建议处理方式             | 示例                                               |
| -------------- | ------------------------ | -------------------------------------------------- |
| 参数非法       | gRPC `INVALID_ARGUMENT`  | ID 为空、分页参数非法                              |
| 未登录         | gRPC `UNAUTHENTICATED`   | Token 缺失或无效                                   |
| 无权限         | gRPC `PERMISSION_DENIED` | 无业务操作权限                                     |
| 资源不存在     | 视业务而定               | 查询详情可用 `NOT_FOUND`，列表为空可返回成功空列表 |
| 系统异常       | gRPC `INTERNAL`          | 数据库异常、未知异常                               |
| 可预期业务失败 | 统一响应结构             | 库存不足、余额不足、审批拒绝                       |
| 成功结果       | 统一响应结构             | `success=true, code=0`                             |

统一响应结构设计建议如下。

| 建议             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 不要滥用统一响应 | gRPC 已有标准 Status，异常场景应优先使用 Status              |
| 成功响应要稳定   | 成功编码建议固定为 `0`                                       |
| 错误码要分层     | 可以按模块划分，例如用户 `10000`、订单 `20000`               |
| 保留 traceId     | 方便客户端问题反馈和服务端日志检索                           |
| 分页信息独立定义 | 不要把分页字段散落到每个业务响应里                           |
| 流式响应谨慎包装 | Server Streaming 每条消息都带 header 会增加冗余，需要按场景评估 |

对于内部微服务调用，推荐采用“gRPC Status 处理技术错误和协议级错误，业务响应结构处理可预期业务结果”的组合方式。这样既能利用 gRPC 标准语义，也能保留业务错误码体系。



## 异常处理

本章节用于说明 Spring gRPC 服务端和客户端的异常处理方式。gRPC 的异常不应简单类比 HTTP 状态码，它有自己的 `Status.Code` 体系；每次 RPC 调用最终都会产生一个 status，成功时为 `OK`，失败时由 gRPC 框架或业务服务端返回具体错误码。gRPC 官方状态码文档说明，`INVALID_ARGUMENT`、`NOT_FOUND`、`ALREADY_EXISTS`、`FAILED_PRECONDITION`、`ABORTED`、`OUT_OF_RANGE`、`DATA_LOSS` 等状态码不会由 gRPC 库自动生成，通常表示应用层主动返回的业务语义。([gRPC](https://grpc.io/docs/guides/status-codes/?utm_source=chatgpt.com))

Spring gRPC 提供了自动配置的异常处理机制。服务端只需要在 Spring 容器中提供 `GrpcExceptionHandler` 类型的 Bean，就可以统一处理服务方法抛出的异常；一个处理器可以处理特定异常，不支持时返回 `null`，也可以作为兜底处理器处理所有异常。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

### gRPC Status 状态码

`Status` 是 gRPC 的标准调用结果表达，包含标准错误码和可选描述信息。gRPC Java 的 `Status` API 支持通过 `withDescription` 添加错误描述，通过 `asException` 或 `asRuntimeException` 转换为异常；客户端可以从异常中提取 `Status`。需要注意的是，`withCause` 中的 Java 异常原因不会从服务端传输到客户端，因此跨服务错误信息必须通过 `Status` 描述或 Metadata 显式传递。([grpc.github.io](https://grpc.github.io/grpc-java/javadoc/io/grpc/Status.html?utm_source=chatgpt.com))

常用状态码建议如下。

| 状态码                | 典型含义       | 建议使用场景                           |
| --------------------- | -------------- | -------------------------------------- |
| `OK`                  | 成功           | RPC 正常完成                           |
| `INVALID_ARGUMENT`    | 参数非法       | ID 为空、分页参数非法、字段格式错误    |
| `NOT_FOUND`           | 资源不存在     | 根据 ID 查询不到用户、订单、文件       |
| `ALREADY_EXISTS`      | 资源已存在     | 用户名重复、订单号重复                 |
| `FAILED_PRECONDITION` | 前置条件不满足 | 状态不允许操作、订单已关闭但仍提交支付 |
| `PERMISSION_DENIED`   | 已认证但无权限 | 用户无角色、无资源访问权限             |
| `UNAUTHENTICATED`     | 未认证         | Token 缺失、Token 无效                 |
| `DEADLINE_EXCEEDED`   | 调用超时       | 客户端设置 deadline 后服务端未及时返回 |
| `UNAVAILABLE`         | 服务不可用     | 服务下线、连接失败、临时网络问题       |
| `INTERNAL`            | 服务端内部错误 | 未预期异常、数据库异常、系统异常       |
| `UNKNOWN`             | 未知错误       | 未被正确转换的异常                     |

推荐按以下规则使用状态码。

| 异常类型       | 推荐 Status                   | 说明                       |
| -------------- | ----------------------------- | -------------------------- |
| 参数校验异常   | `INVALID_ARGUMENT`            | 请求本身不合法             |
| 鉴权失败       | `UNAUTHENTICATED`             | 没有有效身份               |
| 授权失败       | `PERMISSION_DENIED`           | 有身份但没有权限           |
| 资源不存在     | `NOT_FOUND`                   | 查询指定资源不存在         |
| 幂等冲突       | `ALREADY_EXISTS` 或 `ABORTED` | 重复创建或并发冲突         |
| 状态不允许     | `FAILED_PRECONDITION`         | 当前业务状态不满足操作条件 |
| 下游服务不可用 | `UNAVAILABLE`                 | 可重试的临时失败           |
| 未预期异常     | `INTERNAL`                    | 服务端内部错误             |

不建议把所有异常都转换成 `UNKNOWN` 或 `INTERNAL`。这会导致客户端无法区分参数错误、鉴权失败、资源不存在和服务端异常，也不利于调用方做重试、降级或用户提示。

### 业务异常转换

业务异常转换用于将 Java 业务异常统一映射为 gRPC `StatusException`。推荐业务层只抛出领域异常，不直接依赖 gRPC API；在传输层由 `GrpcExceptionHandler` 统一转换。这样可以避免业务 Service 被 gRPC 细节污染。

示例文件结构如下。

```text
src/main/java/io/github/atengk/grpc
├── common
│   ├── enums
│   │   └── GrpcErrorCodeEnum.java
│   └── exception
│       ├── BizException.java
│       ├── ResourceNotFoundException.java
│       └── UnauthorizedException.java
└── exception
    └── GlobalGrpcExceptionHandler.java
```

下面定义业务错误码枚举，用于统一维护业务异常和 gRPC Status 的映射关系。

文件位置：`src/main/java/io/github/atengk/grpc/common/enums/GrpcErrorCodeEnum.java`

```java
package io.github.atengk.grpc.common.enums;

import io.grpc.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * gRPC 业务错误码枚举
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@AllArgsConstructor
public enum GrpcErrorCodeEnum {

    /**
     * 操作成功
     */
    SUCCESS(0, "操作成功", Status.OK),

    /**
     * 参数非法
     */
    INVALID_ARGUMENT(40001, "参数非法", Status.INVALID_ARGUMENT),

    /**
     * 未认证
     */
    UNAUTHENTICATED(40101, "认证信息无效", Status.UNAUTHENTICATED),

    /**
     * 无权限
     */
    PERMISSION_DENIED(40301, "无访问权限", Status.PERMISSION_DENIED),

    /**
     * 资源不存在
     */
    NOT_FOUND(40401, "资源不存在", Status.NOT_FOUND),

    /**
     * 业务状态不满足
     */
    FAILED_PRECONDITION(40901, "业务状态不满足", Status.FAILED_PRECONDITION),

    /**
     * 服务不可用
     */
    UNAVAILABLE(50301, "服务暂不可用", Status.UNAVAILABLE),

    /**
     * 系统异常
     */
    INTERNAL_ERROR(50001, "系统异常", Status.INTERNAL);

    private final Integer code;

    private final String message;

    private final Status status;
}
```

下面定义基础业务异常。

文件位置：`src/main/java/io/github/atengk/grpc/common/exception/BizException.java`

```java
package io.github.atengk.grpc.common.exception;

import io.github.atengk.grpc.common.enums.GrpcErrorCodeEnum;
import lombok.Getter;

/**
 * gRPC 业务异常
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
public class BizException extends RuntimeException {

    private final GrpcErrorCodeEnum errorCode;

    /**
     * 创建业务异常
     *
     * @param errorCode 错误码
     */
    public BizException(GrpcErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 创建业务异常
     *
     * @param errorCode 错误码
     * @param message   错误消息
     */
    public BizException(GrpcErrorCodeEnum errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

下面定义资源不存在异常。

文件位置：`src/main/java/io/github/atengk/grpc/common/exception/ResourceNotFoundException.java`

```java
package io.github.atengk.grpc.common.exception;

import io.github.atengk.grpc.common.enums.GrpcErrorCodeEnum;

/**
 * 资源不存在异常
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class ResourceNotFoundException extends BizException {

    /**
     * 创建资源不存在异常
     *
     * @param message 错误消息
     */
    public ResourceNotFoundException(String message) {
        super(GrpcErrorCodeEnum.NOT_FOUND, message);
    }
}
```

下面定义认证异常。

文件位置：`src/main/java/io/github/atengk/grpc/common/exception/UnauthorizedException.java`

```java
package io.github.atengk.grpc.common.exception;

import io.github.atengk.grpc.common.enums.GrpcErrorCodeEnum;

/**
 * gRPC 认证异常
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class UnauthorizedException extends BizException {

    /**
     * 创建认证异常
     *
     * @param message 错误消息
     */
    public UnauthorizedException(String message) {
        super(GrpcErrorCodeEnum.UNAUTHENTICATED, message);
    }
}
```

服务端业务代码中只抛出业务异常，不直接构造 `StatusRuntimeException`。

```java
public UserDTO getUserById(Long userId) {
    if (userId == null || userId <= 0) {
        throw new BizException(GrpcErrorCodeEnum.INVALID_ARGUMENT, "用户 ID 必须大于 0");
    }

    return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在，userId=" + userId));
}
```

这种写法的核心收益是分层清晰：业务层只表达业务失败原因，gRPC 层负责把异常转换成协议可识别的 `Status`。

### 全局异常处理

全局异常处理用于集中捕获服务端 RPC 方法抛出的异常，并转换成稳定的 `StatusException`。Spring gRPC 的服务端异常处理机制基于 `GrpcExceptionHandler` Bean，多个处理器可以同时存在，不支持的异常返回 `null` 后交给后续处理器。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

下面先定义 Metadata Key，后续异常处理器、拦截器、客户端解析器都会复用。

文件位置：`src/main/java/io/github/atengk/grpc/common/constant/GrpcMetadataKeys.java`

```java
package io.github.atengk.grpc.common.constant;

import io.grpc.Metadata;

/**
 * gRPC Metadata Key 常量
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class GrpcMetadataKeys {

    /**
     * 认证信息 Key
     */
    public static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * 链路追踪 ID Key
     */
    public static final Metadata.Key<String> TRACE_ID =
            Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * 业务错误码 Key
     */
    public static final Metadata.Key<String> BIZ_CODE =
            Metadata.Key.of("x-biz-code", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * 业务错误消息 Key
     */
    public static final Metadata.Key<String> BIZ_MESSAGE =
            Metadata.Key.of("x-biz-message", Metadata.ASCII_STRING_MARSHALLER);

    private GrpcMetadataKeys() {
    }
}
```

下面的全局异常处理器会把业务异常、参数异常、认证异常和未知异常转换为 gRPC Status，并在 Trailer Metadata 中附加业务错误码和 traceId。gRPC Java 的 `StatusRuntimeException` 支持携带 `Metadata trailers`，客户端可以通过异常对象读取这些 trailers。([grpc.github.io](https://grpc.github.io/grpc-java/javadoc/io/grpc/StatusRuntimeException.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/grpc/exception/GlobalGrpcExceptionHandler.java`

```java
package io.github.atengk.grpc.exception;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.common.constant.GrpcMetadataKeys;
import io.github.atengk.grpc.common.enums.GrpcErrorCodeEnum;
import io.github.atengk.grpc.common.exception.BizException;
import io.github.atengk.grpc.common.exception.UnauthorizedException;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.stereotype.Component;

/**
 * gRPC 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@Order(100)
public class GlobalGrpcExceptionHandler implements GrpcExceptionHandler {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 处理 gRPC 服务端异常
     *
     * @param exception 异常对象
     * @return gRPC 状态异常
     */
    @Override
    public StatusException handleException(Throwable exception) {
        if (exception instanceof UnauthorizedException unauthorizedException) {
            return buildException(unauthorizedException.getErrorCode(), unauthorizedException.getMessage());
        }

        if (exception instanceof BizException bizException) {
            return buildException(bizException.getErrorCode(), bizException.getMessage());
        }

        if (exception instanceof IllegalArgumentException illegalArgumentException) {
            return buildException(GrpcErrorCodeEnum.INVALID_ARGUMENT, illegalArgumentException.getMessage());
        }

        log.error("gRPC 服务端发生未处理异常", exception);
        return buildException(GrpcErrorCodeEnum.INTERNAL_ERROR, "服务端内部异常");
    }

    /**
     * 构建 gRPC 状态异常
     *
     * @param errorCode 错误码枚举
     * @param message   错误消息
     * @return gRPC 状态异常
     */
    private StatusException buildException(GrpcErrorCodeEnum errorCode, String message) {
        GrpcErrorCodeEnum safeErrorCode = ObjectUtil.defaultIfNull(errorCode, GrpcErrorCodeEnum.INTERNAL_ERROR);
        String safeMessage = StrUtil.blankToDefault(message, safeErrorCode.getMessage());

        Metadata trailers = new Metadata();
        trailers.put(GrpcMetadataKeys.BIZ_CODE, String.valueOf(safeErrorCode.getCode()));
        trailers.put(GrpcMetadataKeys.BIZ_MESSAGE, safeMessage);
        trailers.put(GrpcMetadataKeys.TRACE_ID, StrUtil.blankToDefault(MDC.get(TRACE_ID_KEY), ""));

        Status status = safeErrorCode.getStatus().withDescription(safeMessage);

        if (Status.Code.INTERNAL.equals(status.getCode())) {
            log.error("gRPC 异常转换完成，status={}, bizCode={}, message={}",
                    status.getCode(), safeErrorCode.getCode(), safeMessage);
        } else {
            log.warn("gRPC 业务异常转换完成，status={}, bizCode={}, message={}",
                    status.getCode(), safeErrorCode.getCode(), safeMessage);
        }

        return status.asException(trailers);
    }
}
```

服务实现类中不需要再大量写 `try-catch`。

```java
@Override
public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
    UserDTO userDTO = userService.getUserById(request.getId());

    GetUserResponse response = GetUserResponse.newBuilder()
            .setUser(UserProtoConverter.toProto(userDTO))
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
}
```

全局异常处理建议如下。

| 建议                  | 说明                                                         |
| --------------------- | ------------------------------------------------------------ |
| 业务异常集中转换      | 不要在每个 RPC 方法中重复转换 Status                         |
| 未知异常不要透出堆栈  | 客户端只返回“服务端内部异常”，堆栈留在服务端日志             |
| Metadata 只放必要信息 | 不要放 SQL、堆栈、内部类名、敏感数据                         |
| 日志区分级别          | 参数错误用 WARN，系统异常用 ERROR                            |
| traceId 必须返回      | 方便客户端携带 traceId 反馈问题                              |
| 认证授权异常单独处理  | 认证失败用 `UNAUTHENTICATED`，授权失败用 `PERMISSION_DENIED` |

### 客户端异常解析

客户端异常解析用于把服务端返回的 `StatusRuntimeException` 转换为客户端内部可识别的业务异常。阻塞式 Stub 调用失败时通常会抛出 `StatusRuntimeException`；该异常可以读取 `getStatus()` 和 `getTrailers()`。([grpc.github.io](https://grpc.github.io/grpc-java/javadoc/io/grpc/StatusRuntimeException.html?utm_source=chatgpt.com))

下面定义客户端侧异常对象。

文件位置：`src/main/java/io/github/atengk/grpc/client/exception/GrpcRemoteCallException.java`

```java
package io.github.atengk.grpc.client.exception;

import io.grpc.Status;
import lombok.Getter;

/**
 * gRPC 远程调用异常
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
public class GrpcRemoteCallException extends RuntimeException {

    private final Status.Code statusCode;

    private final String bizCode;

    private final String traceId;

    /**
     * 创建远程调用异常
     *
     * @param statusCode gRPC 状态码
     * @param bizCode    业务错误码
     * @param message    错误消息
     * @param traceId    链路追踪 ID
     * @param cause      原始异常
     */
    public GrpcRemoteCallException(Status.Code statusCode, String bizCode, String message, String traceId, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.bizCode = bizCode;
        this.traceId = traceId;
    }
}
```

下面的解析器用于统一解析客户端异常。

文件位置：`src/main/java/io/github/atengk/grpc/client/exception/GrpcClientExceptionParser.java`

```java
package io.github.atengk.grpc.client.exception;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.common.constant.GrpcMetadataKeys;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC 客户端异常解析器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@UtilityClass
public class GrpcClientExceptionParser {

    /**
     * 解析 gRPC 调用异常
     *
     * @param exception gRPC 状态异常
     * @return 远程调用异常
     */
    public GrpcRemoteCallException parse(StatusRuntimeException exception) {
        Status status = exception.getStatus();
        Metadata trailers = exception.getTrailers();

        String bizCode = "";
        String bizMessage = "";
        String traceId = "";

        if (trailers != null) {
            bizCode = StrUtil.blankToDefault(trailers.get(GrpcMetadataKeys.BIZ_CODE), "");
            bizMessage = StrUtil.blankToDefault(trailers.get(GrpcMetadataKeys.BIZ_MESSAGE), "");
            traceId = StrUtil.blankToDefault(trailers.get(GrpcMetadataKeys.TRACE_ID), "");
        }

        String message = StrUtil.blankToDefault(bizMessage, status.getDescription());
        message = StrUtil.blankToDefault(message, "gRPC 远程调用失败");

        log.warn("gRPC 远程调用异常，status={}, bizCode={}, traceId={}, message={}",
                status.getCode(), bizCode, traceId, message);

        return new GrpcRemoteCallException(status.getCode(), bizCode, message, traceId, exception);
    }
}
```

客户端业务封装中统一捕获并转换异常。

```java
public UserDTO getUser(Long userId) {
    try {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId(userId)
                .build();

        GetUserResponse response = userServiceBlockingStub.getUser(request);
        return UserProtoConverter.toDTO(response.getUser());
    } catch (StatusRuntimeException exception) {
        throw GrpcClientExceptionParser.parse(exception);
    }
}
```

客户端处理建议如下。

| Status              | 客户端建议                                 |
| ------------------- | ------------------------------------------ |
| `INVALID_ARGUMENT`  | 不重试，提示参数错误或记录调用方 Bug       |
| `UNAUTHENTICATED`   | 刷新 Token 或重新认证                      |
| `PERMISSION_DENIED` | 不重试，提示无权限                         |
| `NOT_FOUND`         | 视业务返回空结果或提示资源不存在           |
| `DEADLINE_EXCEEDED` | 可按幂等性判断是否重试                     |
| `UNAVAILABLE`       | 可进行短暂重试或降级                       |
| `INTERNAL`          | 不盲目重试，记录 traceId 并告警            |
| `UNKNOWN`           | 记录完整上下文，优先排查服务端异常转换缺失 |

## 拦截器开发

本章节用于说明 Spring gRPC 中服务端拦截器、客户端拦截器、日志链路追踪和认证信息透传。gRPC 官方文档说明，拦截器是按调用生效的横切机制，适合日志、指标、认证、授权、缓存、策略控制等场景；客户端拦截器和服务端拦截器 API 不同，多个拦截器的顺序会影响执行结果。([gRPC](https://grpc.io/docs/guides/interceptors/?utm_source=chatgpt.com))

Spring gRPC 支持全局服务端拦截器和全局客户端拦截器。服务端可以注册 `ServerInterceptor` Bean 并使用 `@GlobalServerInterceptor` 使其应用到所有服务；客户端可以注册 `ClientInterceptor` Bean 并使用 `@GlobalClientInterceptor` 应用到创建的 Channel，多个拦截器按 `@Order` 排序。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

### 服务端拦截器

服务端拦截器用于在 RPC 请求进入具体服务方法前后处理横切逻辑，例如 traceId 初始化、认证校验、请求日志、耗时统计、异常观察等。gRPC Java 的 `ServerInterceptor` 会在请求分发到具体 `ServerCallHandler` 前被调用，可以读取请求 Metadata，也可以拦截监听器事件。([grpc.github.io](https://grpc.github.io/grpc-java/javadoc/io/grpc/ServerInterceptor.html?utm_source=chatgpt.com))

下面实现一个服务端日志与链路追踪拦截器。

文件位置：`src/main/java/io/github/atengk/grpc/interceptor/server/GrpcServerTraceInterceptor.java`

```java
package io.github.atengk.grpc.interceptor.server;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.common.constant.GrpcMetadataKeys;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * gRPC 服务端链路追踪拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
public class GrpcServerTraceInterceptor implements ServerInterceptor {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 拦截服务端 RPC 调用
     *
     * @param call    服务端调用对象
     * @param headers 请求 Metadata
     * @param next    下一个调用处理器
     * @param <ReqT>  请求类型
     * @param <RespT> 响应类型
     * @return 请求监听器
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String traceId = StrUtil.blankToDefault(headers.get(GrpcMetadataKeys.TRACE_ID), IdUtil.fastSimpleUUID());
        String methodName = call.getMethodDescriptor().getFullMethodName();
        long startTime = SystemClock.now();

        MDC.put(TRACE_ID_KEY, traceId);
        log.info("gRPC 服务端开始处理请求，method={}, traceId={}", methodName, traceId);

        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {

            @Override
            public void onHalfClose() {
                try {
                    MDC.put(TRACE_ID_KEY, traceId);
                    super.onHalfClose();
                } finally {
                    MDC.remove(TRACE_ID_KEY);
                }
            }

            @Override
            public void onCancel() {
                try {
                    MDC.put(TRACE_ID_KEY, traceId);
                    log.warn("gRPC 服务端请求被取消，method={}, traceId={}", methodName, traceId);
                    super.onCancel();
                } finally {
                    MDC.remove(TRACE_ID_KEY);
                }
            }

            @Override
            public void onComplete() {
                try {
                    MDC.put(TRACE_ID_KEY, traceId);
                    long cost = SystemClock.now() - startTime;
                    log.info("gRPC 服务端请求处理完成，method={}, traceId={}, cost={}ms",
                            methodName, traceId, cost);
                    super.onComplete();
                } finally {
                    MDC.remove(TRACE_ID_KEY);
                }
            }
        };
    }
}
```

下面实现服务端认证拦截器。示例中从 Metadata 的 `authorization` 中读取 Bearer Token，并通过 `GrpcTokenValidator` 校验。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcTokenValidator.java`

```java
package io.github.atengk.grpc.security;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * gRPC Token 校验器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class GrpcTokenValidator {

    /**
     * 校验 Token
     *
     * @param token Token
     * @return 是否有效
     */
    public boolean valid(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }

        // 示例代码：生产环境应替换为 JWT、OAuth2、Sa-Token 或内部认证服务校验
        boolean valid = StrUtil.equals(token, "dev-token");
        if (!valid) {
            log.warn("gRPC Token 校验失败");
        }

        return valid;
    }
}
```

文件位置：`src/main/java/io/github/atengk/grpc/interceptor/server/GrpcServerAuthInterceptor.java`

```java
package io.github.atengk.grpc.interceptor.server;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.common.constant.GrpcMetadataKeys;
import io.github.atengk.grpc.security.GrpcTokenValidator;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC 服务端认证拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RequiredArgsConstructor
public class GrpcServerAuthInterceptor implements ServerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GrpcTokenValidator tokenValidator;

    /**
     * 拦截服务端 RPC 调用
     *
     * @param call    服务端调用对象
     * @param headers 请求 Metadata
     * @param next    下一个调用处理器
     * @param <ReqT>  请求类型
     * @param <RespT> 响应类型
     * @return 请求监听器
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        if (isPublicMethod(methodName)) {
            return next.startCall(call, headers);
        }

        String authorization = headers.get(GrpcMetadataKeys.AUTHORIZATION);
        String token = StrUtil.removePrefix(StrUtil.blankToDefault(authorization, ""), BEARER_PREFIX);

        if (!tokenValidator.valid(token)) {
            log.warn("gRPC 请求认证失败，method={}", methodName);
            call.close(Status.UNAUTHENTICATED.withDescription("认证信息缺失或无效"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        return next.startCall(call, headers);
    }

    /**
     * 判断是否为公开方法
     *
     * @param methodName 方法名
     * @return 是否公开
     */
    private boolean isPublicMethod(String methodName) {
        return StrUtil.startWith(methodName, "grpc.health.v1.Health/")
                || StrUtil.startWith(methodName, "grpc.reflection.v1alpha.ServerReflection/")
                || StrUtil.startWith(methodName, "grpc.reflection.v1.ServerReflection/");
    }
}
```

全局注册服务端拦截器。

文件位置：`src/main/java/io/github/atengk/grpc/config/GrpcServerInterceptorConfig.java`

```java
package io.github.atengk.grpc.config;

import io.github.atengk.grpc.interceptor.server.GrpcServerAuthInterceptor;
import io.github.atengk.grpc.interceptor.server.GrpcServerTraceInterceptor;
import io.github.atengk.grpc.security.GrpcTokenValidator;
import io.grpc.ServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.service.GlobalServerInterceptor;

/**
 * gRPC 服务端拦截器配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class GrpcServerInterceptorConfig {

    /**
     * 服务端链路追踪拦截器
     *
     * @return 服务端拦截器
     */
    @Bean
    @Order(100)
    @GlobalServerInterceptor
    public ServerInterceptor grpcServerTraceInterceptor() {
        return new GrpcServerTraceInterceptor();
    }

    /**
     * 服务端认证拦截器
     *
     * @param tokenValidator Token 校验器
     * @return 服务端拦截器
     */
    @Bean
    @Order(200)
    @GlobalServerInterceptor
    public ServerInterceptor grpcServerAuthInterceptor(GrpcTokenValidator tokenValidator) {
        return new GrpcServerAuthInterceptor(tokenValidator);
    }
}
```

服务端拦截器建议如下。

| 拦截器         | 建议顺序     | 说明                         |
| -------------- | ------------ | ---------------------------- |
| traceId 拦截器 | 最前         | 确保后续日志都能拿到 traceId |
| 认证拦截器     | 靠前         | 尽早拒绝非法请求             |
| 权限拦截器     | 认证之后     | 基于认证身份判断方法权限     |
| 日志拦截器     | traceId 之后 | 记录方法、耗时、状态         |
| 指标拦截器     | 业务前后     | 统计调用次数、耗时、错误码   |

### 客户端拦截器

客户端拦截器用于在每次 RPC 调用发出前后处理横切逻辑，例如写入 traceId、写入 Token、记录调用耗时、解析调用结果等。Spring gRPC 客户端文档说明，全局客户端拦截器可以通过 `@GlobalClientInterceptor` 注册，并按 `@Order` 排序；也可以通过 `ChannelBuilderOptions` 对单个 Channel 设置拦截器。([Home](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

下面实现客户端 traceId 拦截器。

文件位置：`src/main/java/io/github/atengk/grpc/interceptor/client/GrpcClientTraceInterceptor.java`

```java
package io.github.atengk.grpc.interceptor.client;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.common.constant.GrpcMetadataKeys;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * gRPC 客户端链路追踪拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
public class GrpcClientTraceInterceptor implements ClientInterceptor {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 拦截客户端 RPC 调用
     *
     * @param method      方法描述
     * @param callOptions 调用配置
     * @param next        下一个 Channel
     * @param <ReqT>      请求类型
     * @param <RespT>     响应类型
     * @return 客户端调用对象
     */
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        String methodName = method.getFullMethodName();
        String traceId = StrUtil.blankToDefault(MDC.get(TRACE_ID_KEY), IdUtil.fastSimpleUUID());
        long startTime = SystemClock.now();

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(GrpcMetadataKeys.TRACE_ID, traceId);
                MDC.put(TRACE_ID_KEY, traceId);
                log.info("gRPC 客户端开始调用，method={}, traceId={}", methodName, traceId);

                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        try {
                            MDC.put(TRACE_ID_KEY, traceId);
                            long cost = SystemClock.now() - startTime;

                            if (status.isOk()) {
                                log.info("gRPC 客户端调用成功，method={}, traceId={}, cost={}ms",
                                        methodName, traceId, cost);
                            } else {
                                log.warn("gRPC 客户端调用失败，method={}, traceId={}, status={}, cost={}ms",
                                        methodName, traceId, status.getCode(), cost);
                            }

                            super.onClose(status, trailers);
                        } finally {
                            MDC.remove(TRACE_ID_KEY);
                        }
                    }
                }, headers);
            }
        };
    }
}
```

下面实现客户端 Token 透传拦截器。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcClientTokenProvider.java`

```java
package io.github.atengk.grpc.security;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * gRPC 客户端 Token 提供器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Component
public class GrpcClientTokenProvider {

    @Value("${app.grpc.token:dev-token}")
    private String token;

    /**
     * 获取客户端调用 Token
     *
     * @return Token
     */
    public String getToken() {
        return StrUtil.blankToDefault(token, "");
    }
}
```

文件位置：`src/main/java/io/github/atengk/grpc/interceptor/client/GrpcClientAuthInterceptor.java`

```java
package io.github.atengk.grpc.interceptor.client;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.common.constant.GrpcMetadataKeys;
import io.github.atengk.grpc.security.GrpcClientTokenProvider;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC 客户端认证拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RequiredArgsConstructor
public class GrpcClientAuthInterceptor implements ClientInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GrpcClientTokenProvider tokenProvider;

    /**
     * 拦截客户端 RPC 调用
     *
     * @param method      方法描述
     * @param callOptions 调用配置
     * @param next        下一个 Channel
     * @param <ReqT>      请求类型
     * @param <RespT>     响应类型
     * @return 客户端调用对象
     */
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                String token = tokenProvider.getToken();

                if (StrUtil.isBlank(token)) {
                    log.warn("gRPC 客户端 Token 为空，method={}", method.getFullMethodName());
                } else {
                    headers.put(GrpcMetadataKeys.AUTHORIZATION, BEARER_PREFIX + token);
                }

                super.start(responseListener, headers);
            }
        };
    }
}
```

全局注册客户端拦截器。

文件位置：`src/main/java/io/github/atengk/grpc/config/GrpcClientInterceptorConfig.java`

```java
package io.github.atengk.grpc.config;

import io.github.atengk.grpc.interceptor.client.GrpcClientAuthInterceptor;
import io.github.atengk.grpc.interceptor.client.GrpcClientTraceInterceptor;
import io.github.atengk.grpc.security.GrpcClientTokenProvider;
import io.grpc.ClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.interceptor.GlobalClientInterceptor;

/**
 * gRPC 客户端拦截器配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class GrpcClientInterceptorConfig {

    /**
     * 客户端链路追踪拦截器
     *
     * @return 客户端拦截器
     */
    @Bean
    @Order(100)
    @GlobalClientInterceptor
    public ClientInterceptor grpcClientTraceInterceptor() {
        return new GrpcClientTraceInterceptor();
    }

    /**
     * 客户端认证拦截器
     *
     * @param tokenProvider Token 提供器
     * @return 客户端拦截器
     */
    @Bean
    @Order(200)
    @GlobalClientInterceptor
    public ClientInterceptor grpcClientAuthInterceptor(GrpcClientTokenProvider tokenProvider) {
        return new GrpcClientAuthInterceptor(tokenProvider);
    }
}
```

客户端配置示例。

文件位置：`src/main/resources/application.yml`

```yaml
app:
  grpc:
    # 示例 Token，生产环境建议来自配置中心、密钥系统、OAuth2 客户端或 Sa-Token
    token: dev-token

spring:
  grpc:
    client:
      channels:
        user-service:
          # 用户服务地址
          address: 127.0.0.1:9090
```

如果只希望某个 Channel 使用特定拦截器，可以通过 `ChannelBuilderOptions` 对单个 Channel 配置。Spring gRPC 文档说明，单 Channel 拦截器可以通过创建 Channel 时传入 options 设置；当同时存在全局拦截器和单 Channel 拦截器时，默认先执行全局拦截器，再执行单 Channel 拦截器。([Home](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

### 日志链路追踪

日志链路追踪用于将一次客户端请求、多个服务间 gRPC 调用、服务端处理日志串联起来。最简单的方式是通过 Metadata 传递 `x-trace-id`，服务端和客户端拦截器将其写入 MDC，然后日志模板中输出 traceId。

日志配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  pattern:
    # 在日志级别后输出 traceId，便于跨服务检索
    level: "%5p [traceId:%X{traceId}]"

  level:
    root: info
    io.github.atengk.grpc: debug
    org.springframework.grpc: info
    io.grpc: info
```

调用链示例。

```text
REST Controller / Job / MQ Consumer
        |
        | 生成或读取 traceId
        v
GrpcClientTraceInterceptor 写入 Metadata: x-trace-id
        |
        v
远程 gRPC 服务
        |
        v
GrpcServerTraceInterceptor 读取 Metadata 并写入 MDC
        |
        v
业务 Service 日志自动带 traceId
```

如果入口是 REST 接口，可以在 HTTP Filter 中生成 traceId 并写入 MDC；如果入口是 MQ 消费、定时任务或批处理任务，也应在任务开始时生成 traceId。gRPC 客户端拦截器会优先读取 MDC 中已有的 `traceId`，没有时再自动生成。

REST 入口 traceId Filter 示例。

文件位置：`src/main/java/io/github/atengk/grpc/web/TraceIdFilter.java`

```java
package io.github.atengk.grpc.web;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 链路追踪过滤器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";

    private static final String TRACE_ID_HEADER = "x-trace-id";

    /**
     * 处理 HTTP 请求链路追踪
     *
     * @param request     请求
     * @param response    响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = StrUtil.blankToDefault(request.getHeader(TRACE_ID_HEADER), IdUtil.fastSimpleUUID());

        try {
            MDC.put(TRACE_ID_KEY, traceId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
```

日志链路追踪建议如下。

| 建议                 | 说明                                                 |
| -------------------- | ---------------------------------------------------- |
| traceId 统一命名     | HTTP Header 和 gRPC Metadata 都建议使用 `x-trace-id` |
| 客户端优先透传       | 有上游 traceId 时不要重新生成                        |
| 服务端写入 MDC       | 确保业务日志自动带 traceId                           |
| 异常响应返回 traceId | 客户端报错时可以直接提供 traceId 给服务端排查        |
| 不要只依赖日志       | 生产环境建议结合 Micrometer、Tracing、OpenTelemetry  |

Spring gRPC 服务端和客户端都提供了可观测性相关自动配置；服务端文档说明，添加 Spring Boot Actuator 以及相应观测平台桥接后，可以看到 gRPC trace logging 和 metrics，客户端文档也说明 Spring gRPC 提供自动配置的客户端观测拦截器。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

### 认证信息透传

认证信息透传用于在服务间调用时携带用户身份、服务身份或系统访问令牌。gRPC Metadata 可以承载认证信息，例如 `authorization: Bearer <token>`；客户端拦截器负责写入 Metadata，服务端拦截器负责读取并校验。

推荐的透传模型如下。

```text
入口服务校验用户身份
        |
        | 生成服务间调用 Token / 透传用户 Token
        v
客户端拦截器写入 authorization
        |
        v
服务端认证拦截器读取 authorization
        |
        v
Token 校验通过后进入业务方法
```

简单配置示例。

文件位置：`src/main/resources/application.yml`

```yaml
app:
  grpc:
    # 本地开发示例 Token
    token: dev-token
```

客户端发送 Metadata。

```java
headers.put(GrpcMetadataKeys.AUTHORIZATION, "Bearer " + token);
```

服务端读取 Metadata。

```java
String authorization = headers.get(GrpcMetadataKeys.AUTHORIZATION);
String token = StrUtil.removePrefix(StrUtil.blankToDefault(authorization, ""), "Bearer ");
```

认证信息透传建议如下。

| 场景         | 建议                                                     |
| ------------ | -------------------------------------------------------- |
| 本地开发     | 可以使用固定 dev token                                   |
| 内部微服务   | 使用服务间 Token、JWT、OAuth2 Client Credentials 或 mTLS |
| 用户身份透传 | 可透传用户 Token，但要控制信任边界                       |
| 高安全服务   | 使用 mTLS + Token 双重校验                               |
| Token 过期   | 客户端统一刷新，不要在业务代码中散落刷新逻辑             |
| 日志输出     | 禁止打印完整 Token，只允许打印脱敏摘要                   |

Spring gRPC 客户端安全文档说明，客户端认证可以通过拦截器实现；框架也提供 Basic 认证和 Bearer Token 相关拦截器，并支持通过 SSL Bundles 配置 mTLS。官方 gRPC 拦截器指南也提醒，客户端认证虽然可以通过拦截器实现，但 gRPC 也提供专门的 call credentials API，适合更标准化的认证场景。([Home](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

生产环境建议至少满足以下要求。

| 要求                 | 说明                                             |
| -------------------- | ------------------------------------------------ |
| 不使用明文固定 Token | 固定 Token 只适合本地开发                        |
| Token 来源安全       | 从配置中心、密钥系统或认证服务获取               |
| 启用超时             | 防止认证服务或下游调用拖垮线程                   |
| 关闭生产 Reflection  | 避免暴露服务结构                                 |
| 开启 TLS 或 mTLS     | 防止内部链路被旁路嗅探                           |
| 记录认证失败日志     | 记录 method、traceId、来源服务，不记录敏感 Token |

对于中小型内部系统，可以先采用“Bearer Token + Metadata 拦截器 + traceId”的方案；对于核心链路、跨团队服务或高安全等级服务，建议升级为“mTLS + OAuth2/JWT + 服务级授权策略”。



## 安全认证

本章节用于说明 Spring gRPC 中的 Metadata 认证、Token 鉴权、TLS 配置和服务间调用安全。gRPC Metadata 是 RPC 调用的边车信息，底层基于 HTTP/2 Header，可以用于传递认证凭证、traceId、自定义上下文等信息；Metadata 的 key 是 ASCII 字符串，且不能以 `grpc-` 开头，因为该前缀被 gRPC 保留。([grpc.io](https://grpc.io/docs/guides/metadata/?utm_source=chatgpt.com))

gRPC 官方认证文档说明，gRPC 支持 SSL/TLS、Token 类认证机制，也允许接入自定义认证系统。Spring gRPC 在此基础上提供了 Spring Boot 风格的 SSL Bundle、服务端安全拦截器、客户端认证拦截器等集成方式。([grpc.io](https://grpc.io/docs/guides/auth/?utm_source=chatgpt.com), [docs.spring.io](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

### Metadata 认证

Metadata 认证适合在客户端调用服务端时传递认证信息，例如 `authorization`、`x-trace-id`、`x-service-name`、`x-tenant-id` 等。认证类 Metadata 一般由客户端拦截器统一写入，由服务端拦截器统一读取，不建议在每个业务 RPC 方法中手动处理。

推荐先统一定义 Metadata Key。

文件位置：`src/main/java/io/github/atengk/grpc/common/constant/GrpcMetadataKeys.java`

```java
package io.github.atengk.grpc.common.constant;

import io.grpc.Metadata;

/**
 * gRPC Metadata Key 常量
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class GrpcMetadataKeys {

    /**
     * 认证信息
     */
    public static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * 链路追踪 ID
     */
    public static final Metadata.Key<String> TRACE_ID =
            Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * 调用方服务名称
     */
    public static final Metadata.Key<String> SERVICE_NAME =
            Metadata.Key.of("x-service-name", Metadata.ASCII_STRING_MARSHALLER);

    /**
     * 租户 ID
     */
    public static final Metadata.Key<String> TENANT_ID =
            Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);

    private GrpcMetadataKeys() {
    }
}
```

客户端通过拦截器写入 Metadata，服务端通过拦截器读取 Metadata。下面是客户端上下文提供器，用于统一获取 token、traceId、服务名和租户信息。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcClientContextProvider.java`

```java
package io.github.atengk.grpc.security;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * gRPC 客户端上下文提供器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@Component
public class GrpcClientContextProvider {

    private static final String TRACE_ID_KEY = "traceId";

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Value("${app.grpc.auth.token:dev-token}")
    private String token;

    @Value("${app.grpc.auth.tenant-id:default}")
    private String tenantId;

    /**
     * 获取链路追踪 ID
     *
     * @return 链路追踪 ID
     */
    public String getTraceId() {
        return StrUtil.blankToDefault(MDC.get(TRACE_ID_KEY), IdUtil.fastSimpleUUID());
    }

    /**
     * 获取认证 Header 值
     *
     * @return Bearer Token
     */
    public String getAuthorization() {
        if (StrUtil.isBlank(token)) {
            return "";
        }
        return "Bearer " + token;
    }
}
```

下面的客户端拦截器会在每次 RPC 请求发出前写入认证和上下文 Metadata。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcClientMetadataInterceptor.java`

```java
package io.github.atengk.grpc.security;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.common.constant.GrpcMetadataKeys;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC 客户端 Metadata 拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RequiredArgsConstructor
public class GrpcClientMetadataInterceptor implements ClientInterceptor {

    private final GrpcClientContextProvider contextProvider;

    /**
     * 拦截客户端 RPC 调用
     *
     * @param method      方法描述
     * @param callOptions 调用配置
     * @param next        下一个 Channel
     * @param <ReqT>      请求类型
     * @param <RespT>     响应类型
     * @return 客户端调用对象
     */
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                putIfNotBlank(headers, GrpcMetadataKeys.AUTHORIZATION, contextProvider.getAuthorization());
                putIfNotBlank(headers, GrpcMetadataKeys.TRACE_ID, contextProvider.getTraceId());
                putIfNotBlank(headers, GrpcMetadataKeys.SERVICE_NAME, contextProvider.getServiceName());
                putIfNotBlank(headers, GrpcMetadataKeys.TENANT_ID, contextProvider.getTenantId());

                log.debug("gRPC 客户端写入 Metadata，method={}, serviceName={}",
                        method.getFullMethodName(), contextProvider.getServiceName());

                super.start(responseListener, headers);
            }
        };
    }

    /**
     * 写入非空 Metadata
     *
     * @param headers Metadata
     * @param key     Metadata Key
     * @param value   Metadata Value
     */
    private void putIfNotBlank(Metadata headers, Metadata.Key<String> key, String value) {
        if (StrUtil.isNotBlank(value)) {
            headers.put(key, value);
        }
    }
}
```

注册客户端全局拦截器。

文件位置：`src/main/java/io/github/atengk/grpc/config/GrpcSecurityClientConfig.java`

```java
package io.github.atengk.grpc.config;

import io.github.atengk.grpc.security.GrpcClientContextProvider;
import io.github.atengk.grpc.security.GrpcClientMetadataInterceptor;
import io.grpc.ClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.interceptor.GlobalClientInterceptor;

/**
 * gRPC 客户端安全配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class GrpcSecurityClientConfig {

    /**
     * 客户端 Metadata 全局拦截器
     *
     * @param contextProvider 客户端上下文提供器
     * @return 客户端拦截器
     */
    @Bean
    @Order(100)
    @GlobalClientInterceptor
    public ClientInterceptor grpcClientMetadataInterceptor(GrpcClientContextProvider contextProvider) {
        return new GrpcClientMetadataInterceptor(contextProvider);
    }
}
```

服务端读取 Metadata 后，可以放入请求上下文，供业务层或后续拦截器使用。下面定义服务端请求上下文对象。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcRequestContext.java`

```java
package io.github.atengk.grpc.security;

import lombok.Builder;
import lombok.Getter;

/**
 * gRPC 请求上下文
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@Builder
public class GrpcRequestContext {

    /**
     * 认证信息
     */
    private String authorization;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 调用方服务名称
     */
    private String serviceName;

    /**
     * 租户 ID
     */
    private String tenantId;
}
```

使用 ThreadLocal 保存当前请求上下文。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcRequestContextHolder.java`

```java
package io.github.atengk.grpc.security;

import lombok.experimental.UtilityClass;

/**
 * gRPC 请求上下文持有器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@UtilityClass
public class GrpcRequestContextHolder {

    private static final ThreadLocal<GrpcRequestContext> CONTEXT = new ThreadLocal<>();

    /**
     * 设置请求上下文
     *
     * @param context 请求上下文
     */
    public void set(GrpcRequestContext context) {
        CONTEXT.set(context);
    }

    /**
     * 获取请求上下文
     *
     * @return 请求上下文
     */
    public GrpcRequestContext get() {
        return CONTEXT.get();
    }

    /**
     * 清理请求上下文
     */
    public void clear() {
        CONTEXT.remove();
    }
}
```

服务端 Metadata 解析拦截器如下。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcServerMetadataInterceptor.java`

```java
package io.github.atengk.grpc.security;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.common.constant.GrpcMetadataKeys;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * gRPC 服务端 Metadata 解析拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
public class GrpcServerMetadataInterceptor implements ServerInterceptor {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 拦截服务端 RPC 调用
     *
     * @param call    服务端调用对象
     * @param headers 请求 Metadata
     * @param next    下一个调用处理器
     * @param <ReqT>  请求类型
     * @param <RespT> 响应类型
     * @return 请求监听器
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String traceId = StrUtil.blankToDefault(headers.get(GrpcMetadataKeys.TRACE_ID), IdUtil.fastSimpleUUID());

        GrpcRequestContext context = GrpcRequestContext.builder()
                .authorization(StrUtil.blankToDefault(headers.get(GrpcMetadataKeys.AUTHORIZATION), ""))
                .traceId(traceId)
                .serviceName(StrUtil.blankToDefault(headers.get(GrpcMetadataKeys.SERVICE_NAME), "unknown-service"))
                .tenantId(StrUtil.blankToDefault(headers.get(GrpcMetadataKeys.TENANT_ID), "default"))
                .build();

        MDC.put(TRACE_ID_KEY, traceId);
        GrpcRequestContextHolder.set(context);

        log.info("gRPC 服务端接收请求，method={}, traceId={}, serviceName={}, tenantId={}",
                call.getMethodDescriptor().getFullMethodName(), traceId, context.getServiceName(), context.getTenantId());

        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {

            @Override
            public void onComplete() {
                try {
                    super.onComplete();
                } finally {
                    clearContext();
                }
            }

            @Override
            public void onCancel() {
                try {
                    log.warn("gRPC 服务端请求取消，traceId={}", traceId);
                    super.onCancel();
                } finally {
                    clearContext();
                }
            }

            /**
             * 清理请求上下文
             */
            private void clearContext() {
                GrpcRequestContextHolder.clear();
                MDC.remove(TRACE_ID_KEY);
            }
        };
    }
}
```

注册服务端全局拦截器。

文件位置：`src/main/java/io/github/atengk/grpc/config/GrpcSecurityServerConfig.java`

```java
package io.github.atengk.grpc.config;

import io.github.atengk.grpc.security.GrpcServerMetadataInterceptor;
import io.grpc.ServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.service.GlobalServerInterceptor;

/**
 * gRPC 服务端安全配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class GrpcSecurityServerConfig {

    /**
     * 服务端 Metadata 全局拦截器
     *
     * @return 服务端拦截器
     */
    @Bean
    @Order(100)
    @GlobalServerInterceptor
    public ServerInterceptor grpcServerMetadataInterceptor() {
        return new GrpcServerMetadataInterceptor();
    }
}
```

Metadata 认证适合传递轻量认证信息和调用上下文。不要在 Metadata 中放入大对象、完整用户资料、敏感明文字段或业务请求体；这些内容应放在 Protobuf Message 或服务端安全上下文中。

### Token 鉴权

Token 鉴权用于判断调用方是否具备访问当前 gRPC 服务或方法的权限。常见方案包括固定内部 Token、JWT、OAuth2 Resource Server、Sa-Token、自研认证中心和 mTLS 证书身份。Spring gRPC 服务端安全文档说明，在 Spring Security 在 classpath 中时，可以通过 gRPC 安全拦截器接入认证授权，也支持 HTTP Basic、mTLS 预认证、OAuth2 Resource Server 等机制。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

下面先给出轻量级 Token 鉴权实现，适合内部服务或开发环境。生产环境建议替换为 JWT、OAuth2 或统一认证服务。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcTokenProperties.java`

```java
package io.github.atengk.grpc.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * gRPC Token 鉴权配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@ConfigurationProperties(prefix = "app.grpc.auth")
public class GrpcTokenProperties {

    /**
     * 是否启用 Token 鉴权
     */
    private Boolean enabled = true;

    /**
     * 服务端允许的 Token 列表
     */
    private List<String> allowedTokens = new ArrayList<>();

    /**
     * 不需要鉴权的方法前缀
     */
    private List<String> permitMethodPrefixes = new ArrayList<>(List.of(
            "grpc.health.v1.Health/",
            "grpc.reflection.v1alpha.ServerReflection/",
            "grpc.reflection.v1.ServerReflection/"
    ));
}
```

Token 校验器如下。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcTokenValidator.java`

```java
package io.github.atengk.grpc.security;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * gRPC Token 校验器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrpcTokenValidator {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GrpcTokenProperties tokenProperties;

    /**
     * 判断方法是否放行
     *
     * @param methodName gRPC 完整方法名
     * @return 是否放行
     */
    public boolean permit(String methodName) {
        return tokenProperties.getPermitMethodPrefixes()
                .stream()
                .anyMatch(prefix -> StrUtil.startWith(methodName, prefix));
    }

    /**
     * 校验 Authorization
     *
     * @param authorization Authorization Header
     * @return 是否有效
     */
    public boolean validAuthorization(String authorization) {
        if (!Boolean.TRUE.equals(tokenProperties.getEnabled())) {
            return true;
        }

        String token = StrUtil.removePrefix(StrUtil.blankToDefault(authorization, ""), BEARER_PREFIX);
        if (StrUtil.isBlank(token)) {
            log.warn("gRPC Token 为空");
            return false;
        }

        if (CollUtil.isEmpty(tokenProperties.getAllowedTokens())) {
            log.warn("gRPC 服务端未配置允许的 Token 列表");
            return false;
        }

        return tokenProperties.getAllowedTokens().contains(token);
    }
}
```

服务端 Token 鉴权拦截器如下。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcServerTokenAuthInterceptor.java`

```java
package io.github.atengk.grpc.security;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.common.constant.GrpcMetadataKeys;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC 服务端 Token 鉴权拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RequiredArgsConstructor
public class GrpcServerTokenAuthInterceptor implements ServerInterceptor {

    private final GrpcTokenValidator tokenValidator;

    /**
     * 拦截服务端 RPC 调用
     *
     * @param call    服务端调用对象
     * @param headers 请求 Metadata
     * @param next    下一个调用处理器
     * @param <ReqT>  请求类型
     * @param <RespT> 响应类型
     * @return 请求监听器
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        if (tokenValidator.permit(methodName)) {
            return next.startCall(call, headers);
        }

        String authorization = headers.get(GrpcMetadataKeys.AUTHORIZATION);
        if (!tokenValidator.validAuthorization(authorization)) {
            log.warn("gRPC Token 鉴权失败，method={}, authorizationPresent={}",
                    methodName, StrUtil.isNotBlank(authorization));

            call.close(Status.UNAUTHENTICATED.withDescription("认证信息缺失或无效"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        return next.startCall(call, headers);
    }
}
```

注册配置如下。

文件位置：`src/main/java/io/github/atengk/grpc/config/GrpcTokenAuthConfig.java`

```java
package io.github.atengk.grpc.config;

import io.github.atengk.grpc.security.GrpcServerTokenAuthInterceptor;
import io.github.atengk.grpc.security.GrpcTokenProperties;
import io.github.atengk.grpc.security.GrpcTokenValidator;
import io.grpc.ServerInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.service.GlobalServerInterceptor;

/**
 * gRPC Token 鉴权配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
@EnableConfigurationProperties(GrpcTokenProperties.class)
public class GrpcTokenAuthConfig {

    /**
     * 服务端 Token 鉴权拦截器
     *
     * @param tokenValidator Token 校验器
     * @return 服务端拦截器
     */
    @Bean
    @Order(200)
    @GlobalServerInterceptor
    public ServerInterceptor grpcServerTokenAuthInterceptor(GrpcTokenValidator tokenValidator) {
        return new GrpcServerTokenAuthInterceptor(tokenValidator);
    }
}
```

配置文件示例。

文件位置：`src/main/resources/application.yml`

```yaml
app:
  grpc:
    auth:
      # 是否启用 Token 鉴权
      enabled: true

      # 客户端发出的 Token，客户端拦截器会转换为 Authorization: Bearer <token>
      token: dev-token

      # 服务端允许的 Token 列表，生产环境建议来自配置中心或密钥系统
      allowed-tokens:
        - dev-token
        - internal-service-token

      # 允许匿名访问的方法前缀，通常只放行健康检查和反射服务
      permit-method-prefixes:
        - grpc.health.v1.Health/
        - grpc.reflection.v1alpha.ServerReflection/
        - grpc.reflection.v1.ServerReflection/
```

Token 鉴权建议如下。

| 场景       | 建议                                                         |
| ---------- | ------------------------------------------------------------ |
| 本地开发   | 可以使用固定 `dev-token`                                     |
| 内部微服务 | 使用服务间 Token、JWT、OAuth2 Client Credentials 或 Sa-Token |
| 外部调用   | 不建议直接暴露原生 gRPC，建议经过网关、mTLS、OAuth2          |
| 核心服务   | Token + mTLS 双重校验                                        |
| Token 日志 | 禁止打印完整 Token                                           |
| Token 存储 | 使用配置中心、密钥系统、Kubernetes Secret 或 Vault           |
| 失败状态码 | 未认证返回 `UNAUTHENTICATED`，无权限返回 `PERMISSION_DENIED` |

### TLS 配置

TLS 用于加密客户端与服务端之间的通信，mTLS 则要求客户端和服务端互相出示证书。Spring gRPC 的 Netty 服务端支持 TLS 和 mTLS，可以通过 Spring Boot SSL Bundle 配置；客户端也可以在命名 Channel 上配置 SSL Bundle 并设置 TLS negotiation。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com), [docs.spring.io](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

服务端单向 TLS 配置示例。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  grpc:
    server:
      # gRPC 服务端端口
      port: 9090

      ssl:
        # 应用服务端 SSL Bundle
        bundle: grpc-server

  ssl:
    bundle:
      jks:
        grpc-server:
          keystore:
            # 服务端证书库
            location: classpath:certs/grpc-server.jks
            password: ${GRPC_SERVER_KEYSTORE_PASSWORD}
            type: JKS
          key:
            # 服务端私钥密码
            password: ${GRPC_SERVER_KEY_PASSWORD}
```

客户端单向 TLS 配置示例。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  grpc:
    client:
      channels:
        user-service:
          # 生产环境建议使用域名，证书 CN/SAN 应覆盖该域名
          address: user-service.grpc.svc.cluster.local:9090

          # 使用 TLS 建立 HTTP/2 连接
          negotiation-type: TLS
```

mTLS 场景下，服务端要求客户端证书，客户端需要配置自己的证书 Bundle。Spring Boot 4.1 gRPC 文档也说明，服务端可以通过 `spring.grpc.server.ssl.client-auth` 设置客户端认证为 `optional` 或 `require`。([docs.spring.io](https://docs.spring.io/spring-boot/4.1/reference/io/grpc.html?utm_source=chatgpt.com))

服务端 mTLS 配置示例。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  grpc:
    server:
      port: 9090
      ssl:
        # 服务端证书 Bundle
        bundle: grpc-server

        # 要求客户端提供证书
        client-auth: require

  ssl:
    bundle:
      jks:
        grpc-server:
          keystore:
            location: classpath:certs/grpc-server.jks
            password: ${GRPC_SERVER_KEYSTORE_PASSWORD}
            type: JKS
          key:
            password: ${GRPC_SERVER_KEY_PASSWORD}
          truststore:
            # 用于验证客户端证书的信任库
            location: classpath:certs/grpc-server-truststore.jks
            password: ${GRPC_SERVER_TRUSTSTORE_PASSWORD}
            type: JKS
```

客户端 mTLS 配置示例。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  grpc:
    client:
      channels:
        user-service:
          address: user-service.grpc.svc.cluster.local:9090

          # mTLS 连接必须使用 TLS
          negotiation-type: TLS

          ssl:
            # 客户端证书 Bundle
            bundle: grpc-client

  ssl:
    bundle:
      jks:
        grpc-client:
          keystore:
            # 客户端证书库
            location: classpath:certs/grpc-client.jks
            password: ${GRPC_CLIENT_KEYSTORE_PASSWORD}
            type: JKS
          key:
            # 客户端私钥密码
            password: ${GRPC_CLIENT_KEY_PASSWORD}
          truststore:
            # 用于验证服务端证书的信任库
            location: classpath:certs/grpc-client-truststore.jks
            password: ${GRPC_CLIENT_TRUSTSTORE_PASSWORD}
            type: JKS
```

本地开发如使用自签名证书，可以临时允许不安全证书，但只能用于测试。Spring gRPC 服务端文档明确说明，自签名证书测试场景可以设置 `spring.grpc.server.ssl.secure=false`。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  grpc:
    server:
      ssl:
        # 仅测试环境使用，生产环境禁止关闭安全校验
        secure: false
```

TLS 配置建议如下。

| 场景       | 建议                                               |
| ---------- | -------------------------------------------------- |
| 本地开发   | 可使用明文或自签名证书                             |
| 测试环境   | 尽量模拟生产 TLS，避免上线后才暴露证书问题         |
| 生产内网   | 建议启用 TLS，核心链路启用 mTLS                    |
| 跨集群调用 | 建议启用 mTLS                                      |
| 证书存储   | 使用 Secret、Vault、KMS 或配置中心，不要提交到 Git |
| 证书轮换   | 设计证书过期监控和轮换流程                         |
| 域名匹配   | 客户端访问域名应被服务端证书 CN/SAN 覆盖           |

### 服务间调用安全

服务间调用安全不只包括 Token 或 TLS，还包括调用方身份、权限范围、超时、重试、幂等、审计和最小权限。对于内部微服务，推荐组合使用“Metadata 身份透传 + Token/JWT 鉴权 + TLS/mTLS + 方法级授权 + 调用审计”。

推荐安全模型如下。

```text
调用方服务
  ├── 生成 traceId
  ├── 获取服务间 Token
  ├── 客户端拦截器写入 Metadata
  └── TLS/mTLS 加密传输
        ↓
被调用方服务
  ├── 服务端拦截器读取 Metadata
  ├── 校验 Token 或客户端证书
  ├── 判断服务名、租户、方法权限
  ├── 写入 MDC 和请求上下文
  └── 执行业务逻辑
```

服务间调用安全配置示例。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  application:
    name: order-service

  grpc:
    client:
      channels:
        user-service:
          # 使用服务发现域名或内网域名
          address: user-service.grpc.svc.cluster.local:9090
          negotiation-type: TLS
          ssl:
            bundle: grpc-client

app:
  grpc:
    auth:
      # 服务间调用 Token，生产环境应来自密钥系统
      token: ${ORDER_SERVICE_GRPC_TOKEN}

      # 服务端允许调用方 Token
      allowed-tokens:
        - ${USER_SERVICE_ALLOWED_TOKEN}

      # 当前租户，单租户系统可固定，SaaS 系统建议从入口上下文透传
      tenant-id: ${TENANT_ID:default}
```

如果要做方法级权限控制，可以定义允许的服务和方法映射。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcMethodAccessProperties.java`

```java
package io.github.atengk.grpc.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * gRPC 方法访问控制配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@ConfigurationProperties(prefix = "app.grpc.access")
public class GrpcMethodAccessProperties {

    /**
     * key 为服务名，value 为允许访问的方法集合
     */
    private Map<String, Set<String>> serviceMethods = new HashMap<>();

    /**
     * 判断服务是否允许访问方法
     *
     * @param serviceName 服务名
     * @param methodName  方法名
     * @return 是否允许
     */
    public boolean allow(String serviceName, String methodName) {
        Set<String> methods = serviceMethods.getOrDefault(serviceName, new HashSet<>());
        return methods.contains(methodName) || methods.contains("*");
    }
}
```

方法级授权拦截器如下。

文件位置：`src/main/java/io/github/atengk/grpc/security/GrpcMethodAccessInterceptor.java`

```java
package io.github.atengk.grpc.security;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.common.constant.GrpcMetadataKeys;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC 方法级访问控制拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RequiredArgsConstructor
public class GrpcMethodAccessInterceptor implements ServerInterceptor {

    private final GrpcMethodAccessProperties accessProperties;

    /**
     * 拦截服务端 RPC 调用
     *
     * @param call    服务端调用对象
     * @param headers 请求 Metadata
     * @param next    下一个调用处理器
     * @param <ReqT>  请求类型
     * @param <RespT> 响应类型
     * @return 请求监听器
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String serviceName = StrUtil.blankToDefault(headers.get(GrpcMetadataKeys.SERVICE_NAME), "unknown-service");
        String methodName = call.getMethodDescriptor().getFullMethodName();

        if (!accessProperties.allow(serviceName, methodName)) {
            log.warn("gRPC 方法访问被拒绝，serviceName={}, method={}", serviceName, methodName);
            call.close(Status.PERMISSION_DENIED.withDescription("无方法访问权限"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }

        return next.startCall(call, headers);
    }
}
```

配置示例。

文件位置：`src/main/resources/application-prod.yml`

```yaml
app:
  grpc:
    access:
      service-methods:
        order-service:
          # 允许订单服务调用用户查询接口
          - ateng.user.v1.UserService/GetUser
          - ateng.user.v1.UserService/BatchGetUser

        admin-service:
          # 管理服务允许调用全部用户服务方法
          - "*"
```

注册方法级授权拦截器。

文件位置：`src/main/java/io/github/atengk/grpc/config/GrpcMethodAccessConfig.java`

```java
package io.github.atengk.grpc.config;

import io.github.atengk.grpc.security.GrpcMethodAccessInterceptor;
import io.github.atengk.grpc.security.GrpcMethodAccessProperties;
import io.grpc.ServerInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.service.GlobalServerInterceptor;

/**
 * gRPC 方法级访问控制配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
@EnableConfigurationProperties(GrpcMethodAccessProperties.class)
public class GrpcMethodAccessConfig {

    /**
     * 方法级访问控制拦截器
     *
     * @param accessProperties 访问控制配置
     * @return 服务端拦截器
     */
    @Bean
    @Order(300)
    @GlobalServerInterceptor
    public ServerInterceptor grpcMethodAccessInterceptor(GrpcMethodAccessProperties accessProperties) {
        return new GrpcMethodAccessInterceptor(accessProperties);
    }
}
```

服务间调用安全建议如下。

| 安全项   | 建议                                         |
| -------- | -------------------------------------------- |
| 身份认证 | Token、JWT、OAuth2、mTLS 至少选择一种        |
| 链路加密 | 生产环境建议 TLS，核心系统建议 mTLS          |
| 调用授权 | 按服务名和方法名做白名单                     |
| 超时控制 | 每个客户端 Stub 设置 Deadline                |
| 重试控制 | 只对幂等方法开启重试                         |
| 敏感日志 | 不打印 Token、证书、手机号、身份证等敏感信息 |
| 反射服务 | 生产环境通常关闭 Reflection                  |
| 审计日志 | 记录调用方、方法名、traceId、状态码、耗时    |

## 测试与验证

本章节用于说明 Spring gRPC 项目中的 proto 编译验证、服务端单元测试、客户端调用测试和集成测试。Spring gRPC 提供 `spring-grpc-test`，在 `@SpringBootTest` 中可以启用 in-process transport，不监听真实网络端口也能测试 gRPC 服务；可以通过 `spring.grpc.test.inprocess.enabled=true` 或 `@AutoConfigureInProcessTransport` 开启。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

### proto 编译验证

proto 编译验证用于确认 `.proto` 文件可以正确生成 Java Message 类、Service 基类和客户端 Stub。Spring gRPC Getting Started 文档说明，Maven 构建后会生成 `target/generated-sources/protobuf/grpc-java` 和 `target/generated-sources/protobuf/java`，Gradle 构建后会生成 `build/generated/source/proto/main/grpc` 和 `build/generated/source/proto/main/java`。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/getting-started.html?utm_source=chatgpt.com))

Maven 验证命令如下。

```bash
# 清理并编译 proto，验证生成代码是否正常
./mvnw clean compile

# 执行完整打包，验证测试和打包流程
./mvnw clean package

# 查看生成的 Protobuf Java 类
find target/generated-sources/protobuf/java -type f | head

# 查看生成的 gRPC Stub 类
find target/generated-sources/protobuf/grpc-java -type f | head
```

Gradle 验证命令如下。

```bash
# 清理并构建项目，验证 proto 生成和测试流程
./gradlew clean build

# 查看生成的 Protobuf Java 类
find build/generated/source/proto/main/java -type f | head

# 查看生成的 gRPC Stub 类
find build/generated/source/proto/main/grpc -type f | head
```

建议增加一个基础 proto 文件作为编译验证入口。

文件位置：`src/main/proto/user/user.proto`

```proto
syntax = "proto3";

package ateng.user.v1;

option java_multiple_files = true;
option java_package = "io.github.atengk.grpc.proto.user.v1";
option java_outer_classname = "UserProto";

// 用户服务
service UserService {
  // 根据用户 ID 查询用户
  rpc GetUser (GetUserRequest) returns (GetUserResponse);
}

// 查询用户请求
message GetUserRequest {
  int64 id = 1;
}

// 查询用户响应
message GetUserResponse {
  UserDetail user = 1;
}

// 用户详情
message UserDetail {
  int64 id = 1;
  string username = 2;
  string nickname = 3;
  bool enabled = 4;
}
```

编译后应能看到类似生成类。

```text
io.github.atengk.grpc.proto.user.v1.GetUserRequest
io.github.atengk.grpc.proto.user.v1.GetUserResponse
io.github.atengk.grpc.proto.user.v1.UserDetail
io.github.atengk.grpc.proto.user.v1.UserServiceGrpc
io.github.atengk.grpc.proto.user.v1.UserServiceGrpc.UserServiceImplBase
io.github.atengk.grpc.proto.user.v1.UserServiceGrpc.UserServiceBlockingStub
```

proto 编译失败时，优先检查以下问题。

| 问题           | 处理方式                                                     |
| -------------- | ------------------------------------------------------------ |
| 找不到生成类   | 刷新 Maven/Gradle，确认 generated-sources 是否被 IDE 标记为源码目录 |
| package 冲突   | 检查 `package`、`java_package`、`java_outer_classname` 是否重复 |
| import 失败    | 检查被 import 的 proto 文件路径是否相对 `src/main/proto`     |
| 字段编号重复   | 同一个 message 中字段编号必须唯一                            |
| 枚举默认值错误 | proto3 枚举第一个值必须是 0                                  |
| 插件下载失败   | 检查 Maven/Gradle 仓库、代理和网络配置                       |

### 服务端单元测试

服务端单元测试用于直接测试 gRPC 服务实现类，不启动真实 gRPC Server。此类测试速度快，适合验证参数校验、业务异常、响应构造和边界逻辑。可以使用 `grpc-testing` 提供的 `StreamRecorder` 接收服务端通过 `StreamObserver` 返回的数据。

测试依赖如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot 测试依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- gRPC 测试工具，提供 StreamRecorder 等测试组件 -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-testing</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Gradle 配置如下。

文件位置：`build.gradle`

```groovy
dependencies {
    // Spring Boot 测试依赖
    testImplementation 'org.springframework.boot:spring-boot-starter-test'

    // gRPC 测试工具，提供 StreamRecorder 等测试组件
    testImplementation 'io.grpc:grpc-testing'
}
```

下面测试服务端 Unary RPC 的成功和失败场景。

文件位置：`src/test/java/io/github/atengk/grpc/server/UserGrpcServiceTest.java`

```java
package io.github.atengk.grpc.server;

import io.github.atengk.grpc.proto.user.v1.GetUserRequest;
import io.github.atengk.grpc.proto.user.v1.GetUserResponse;
import io.github.atengk.grpc.support.UserMockRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.testing.StreamRecorder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户 gRPC 服务端单元测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
class UserGrpcServiceTest {

    private final UserGrpcService userGrpcService = new UserGrpcService(new UserMockRepository());

    /**
     * 测试根据用户 ID 查询成功
     *
     * @throws Exception 等待响应异常
     */
    @Test
    void shouldGetUserSuccess() throws Exception {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId(1L)
                .build();

        StreamRecorder<GetUserResponse> responseObserver = StreamRecorder.create();

        userGrpcService.getUser(request, responseObserver);

        boolean completed = responseObserver.awaitCompletion(3, TimeUnit.SECONDS);
        Assertions.assertTrue(completed, "gRPC 服务端响应未按时完成");

        List<GetUserResponse> responses = responseObserver.getValues();
        Assertions.assertEquals(1, responses.size());
        Assertions.assertEquals(1L, responses.getFirst().getUser().getId());
        Assertions.assertEquals("ateng", responses.getFirst().getUser().getUsername());

        Assertions.assertNull(responseObserver.getError());
    }

    /**
     * 测试根据用户 ID 查询不存在
     *
     * @throws Exception 等待响应异常
     */
    @Test
    void shouldGetUserNotFound() throws Exception {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId(999L)
                .build();

        StreamRecorder<GetUserResponse> responseObserver = StreamRecorder.create();

        userGrpcService.getUser(request, responseObserver);

        boolean completed = responseObserver.awaitCompletion(3, TimeUnit.SECONDS);
        Assertions.assertTrue(completed, "gRPC 服务端响应未按时完成");

        Throwable error = responseObserver.getError();
        Assertions.assertNotNull(error);
        Assertions.assertInstanceOf(StatusRuntimeException.class, error);

        StatusRuntimeException statusRuntimeException = (StatusRuntimeException) error;
        Assertions.assertEquals(Status.Code.NOT_FOUND, statusRuntimeException.getStatus().getCode());
    }
}
```

如果测试代码运行在 Java 17，`responses.getFirst()` 需要替换为 `responses.get(0)`，因为 `List#getFirst()` 是 Java 21 引入的接口方法。为了兼容前文 JDK 17 最低要求，可以统一使用 `responses.get(0)`。

下面是 JDK 17 兼容版本的断言片段。

```java
List<GetUserResponse> responses = responseObserver.getValues();
Assertions.assertEquals(1, responses.size());
Assertions.assertEquals(1L, responses.get(0).getUser().getId());
Assertions.assertEquals("ateng", responses.get(0).getUser().getUsername());
```

服务端单元测试建议如下。

| 测试项     | 建议                                       |
| ---------- | ------------------------------------------ |
| 成功响应   | 校验 `onNext` 返回内容和 `onCompleted`     |
| 参数错误   | 校验 `INVALID_ARGUMENT`                    |
| 资源不存在 | 校验 `NOT_FOUND`                           |
| 业务异常   | 校验全局异常转换结果                       |
| 流式接口   | 校验多次 `onNext` 的顺序和数量             |
| 异常日志   | 不直接断言日志，优先断言 Status 和响应内容 |

### 客户端调用测试

客户端调用测试用于验证客户端封装类是否正确构造请求、调用 Stub、解析响应和处理异常。测试方式可以分为两类：第一类是 mock Stub 或 mock 客户端封装依赖；第二类是启动 in-process gRPC Server，用真实 Stub 调用服务端。对于 gRPC 客户端，后者更接近真实调用链，且不会占用网络端口。

Spring gRPC 客户端文档说明，测试本地 gRPC Server 时可以使用随机端口 `spring.grpc.server.port=0`，并通过 `@LocalGrpcPort` 或 `${local.grpc.port}` 获取实际端口；创建 Stub 的 Bean 需要懒加载，因为端口只有服务启动后才可用。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/client.html?utm_source=chatgpt.com))

下面使用随机端口做客户端调用测试。

文件位置：`src/test/resources/application-client-test.yml`

```yaml
spring:
  grpc:
    server:
      # 测试环境使用随机端口
      port: 0

    client:
      channels:
        local-user-service:
          # 通过本地随机端口连接测试服务端
          address: 127.0.0.1:${local.grpc.port}
```

测试专用 Stub 配置如下。

文件位置：`src/test/java/io/github/atengk/grpc/client/UserGrpcClientTestConfig.java`

```java
package io.github.atengk.grpc.client;

import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * 用户 gRPC 客户端测试配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class UserGrpcClientTestConfig {

    /**
     * 创建测试用户服务 Blocking Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 用户服务 Blocking Stub
     */
    @Bean
    @Lazy
    public UserServiceGrpc.UserServiceBlockingStub testUserServiceBlockingStub(GrpcChannelFactory channelFactory) {
        return UserServiceGrpc.newBlockingStub(channelFactory.createChannel("local-user-service"));
    }
}
```

客户端测试类如下。

文件位置：`src/test/java/io/github/atengk/grpc/client/UserGrpcClientCallTest.java`

```java
package io.github.atengk.grpc.client;

import io.github.atengk.grpc.proto.user.v1.GetUserRequest;
import io.github.atengk.grpc.proto.user.v1.GetUserResponse;
import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import io.github.atengk.grpc.server.UserGrpcService;
import io.github.atengk.grpc.support.UserMockRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 用户 gRPC 客户端调用测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@ActiveProfiles("client-test")
@SpringBootTest(classes = UserGrpcClientCallTest.TestConfig.class)
class UserGrpcClientCallTest {

    private final UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    UserGrpcClientCallTest(UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub) {
        this.userServiceBlockingStub = userServiceBlockingStub;
    }

    /**
     * 测试客户端调用用户查询接口
     */
    @Test
    void shouldCallGetUserSuccess() {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId(1L)
                .build();

        GetUserResponse response = userServiceBlockingStub.getUser(request);

        Assertions.assertEquals(1L, response.getUser().getId());
        Assertions.assertEquals("ateng", response.getUser().getUsername());
    }

    /**
     * 测试配置
     *
     * @author Ateng
     * @since 2026-05-05
     */
    @EnableAutoConfiguration
    @Import({
            UserGrpcClientTestConfig.class,
            UserGrpcService.class,
            UserMockRepository.class
    })
    static class TestConfig {
    }
}
```

如果使用 Spring gRPC 的 in-process 测试能力，可以不监听真实网络端口。Spring gRPC 服务端测试文档说明，添加 `spring-grpc-test` 后，可以通过 `spring.grpc.test.inprocess.enabled=true` 或 `@AutoConfigureInProcessTransport` 启用 in-process transport；启用后，测试中的常规 server/channel factory 会被测试用 in-process factory 替换，客户端可以通过自动配置的 `GrpcChannelFactory` 连接。([docs.spring.io](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

测试依赖如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring gRPC 测试支持，提供 in-process transport 等测试能力 -->
    <dependency>
        <groupId>org.springframework.grpc</groupId>
        <artifactId>spring-grpc-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Gradle 配置如下。

文件位置：`build.gradle`

```groovy
dependencies {
    // Spring gRPC 测试支持，提供 in-process transport 等测试能力
    testImplementation 'org.springframework.grpc:spring-grpc-test'
}
```

使用 in-process transport 的测试配置如下。

文件位置：`src/test/java/io/github/atengk/grpc/client/UserGrpcInProcessClientTest.java`

```java
package io.github.atengk.grpc.client;

import io.github.atengk.grpc.proto.user.v1.GetUserRequest;
import io.github.atengk.grpc.proto.user.v1.GetUserResponse;
import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import io.github.atengk.grpc.server.UserGrpcService;
import io.github.atengk.grpc.support.UserMockRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureInProcessTransport;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * 用户 gRPC In-Process 客户端测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@TestPropertySource(properties = {
        "spring.grpc.client.default-channel.address=localhost:9090"
})
@SpringJUnitConfig(UserGrpcInProcessClientTest.TestConfig.class)
@AutoConfigureInProcessTransport
class UserGrpcInProcessClientTest {

    private final GrpcChannelFactory channelFactory;

    UserGrpcInProcessClientTest(GrpcChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
    }

    /**
     * 测试 In-Process 调用用户查询接口
     */
    @Test
    void shouldCallGetUserByInProcessTransport() {
        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(
                channelFactory.createChannel("default")
        );

        GetUserResponse response = stub.getUser(GetUserRequest.newBuilder()
                .setId(1L)
                .build());

        Assertions.assertEquals(1L, response.getUser().getId());
        Assertions.assertEquals("ateng", response.getUser().getUsername());
    }

    /**
     * 测试配置
     *
     * @author Ateng
     * @since 2026-05-05
     */
    @EnableAutoConfiguration
    @Import({
            UserGrpcService.class,
            UserMockRepository.class
    })
    static class TestConfig {
    }
}
```

说明：如果你切换到 Spring Boot 4.1 原生 gRPC starter 体系，测试注解名称可能对应 `@AutoConfigureTestGrpcTransport`，而 Spring gRPC 1.0.x 文档中的测试注解为 `@AutoConfigureInProcessTransport`。Spring Boot 4.1 文档也提供了新的 `spring-boot-grpc-test` 模块和 `@AutoConfigureTestGrpcTransport`。([docs.spring.io](https://docs.spring.io/spring-boot/4.1/reference/io/grpc.html?utm_source=chatgpt.com))

### 集成测试

集成测试用于验证 Spring Boot 容器、gRPC Server、客户端 Stub、拦截器、异常处理、Metadata 透传和安全认证是否能协同工作。推荐至少覆盖成功调用、认证失败、资源不存在、Metadata 透传、TLS 配置加载等场景。

下面给出一个包含 Token 鉴权和 Metadata 透传的集成测试配置。

文件位置：`src/test/resources/application-integration-test.yml`

```yaml
spring:
  application:
    name: grpc-integration-test

  grpc:
    server:
      # 集成测试使用随机端口
      port: 0

    client:
      channels:
        user-service:
          # 连接当前测试启动的本地 gRPC 服务端
          address: 127.0.0.1:${local.grpc.port}

app:
  grpc:
    auth:
      # 客户端发出的 Token
      token: integration-test-token

      # 服务端允许的 Token
      allowed-tokens:
        - integration-test-token

      tenant-id: test-tenant

      permit-method-prefixes:
        - grpc.health.v1.Health/
```

集成测试配置类如下。

文件位置：`src/test/java/io/github/atengk/grpc/integration/GrpcIntegrationTestConfig.java`

```java
package io.github.atengk.grpc.integration;

import io.github.atengk.grpc.config.GrpcSecurityClientConfig;
import io.github.atengk.grpc.config.GrpcSecurityServerConfig;
import io.github.atengk.grpc.config.GrpcTokenAuthConfig;
import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import io.github.atengk.grpc.security.GrpcClientContextProvider;
import io.github.atengk.grpc.server.UserGrpcService;
import io.github.atengk.grpc.support.UserMockRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * gRPC 集成测试配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@EnableAutoConfiguration
@Import({
        UserGrpcService.class,
        UserMockRepository.class,
        GrpcClientContextProvider.class,
        GrpcSecurityClientConfig.class,
        GrpcSecurityServerConfig.class,
        GrpcTokenAuthConfig.class
})
public class GrpcIntegrationTestConfig {

    /**
     * 创建用户服务测试 Stub
     *
     * @param channelFactory gRPC Channel 工厂
     * @return 用户服务 Blocking Stub
     */
    @Bean
    @Lazy
    public UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub(GrpcChannelFactory channelFactory) {
        return UserServiceGrpc.newBlockingStub(channelFactory.createChannel("user-service"));
    }
}
```

集成测试类如下。

文件位置：`src/test/java/io/github/atengk/grpc/integration/UserGrpcIntegrationTest.java`

```java
package io.github.atengk.grpc.integration;

import io.github.atengk.grpc.proto.user.v1.GetUserRequest;
import io.github.atengk.grpc.proto.user.v1.GetUserResponse;
import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 用户 gRPC 集成测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@ActiveProfiles("integration-test")
@SpringBootTest(classes = GrpcIntegrationTestConfig.class)
class UserGrpcIntegrationTest {

    private final UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    UserGrpcIntegrationTest(UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub) {
        this.userServiceBlockingStub = userServiceBlockingStub;
    }

    /**
     * 测试带认证信息的用户查询成功
     */
    @Test
    void shouldGetUserSuccessWithAuth() {
        GetUserResponse response = userServiceBlockingStub.getUser(GetUserRequest.newBuilder()
                .setId(1L)
                .build());

        Assertions.assertEquals(1L, response.getUser().getId());
        Assertions.assertEquals("ateng", response.getUser().getUsername());
    }

    /**
     * 测试用户不存在时返回 NOT_FOUND
     */
    @Test
    void shouldReturnNotFoundWhenUserMissing() {
        StatusRuntimeException exception = Assertions.assertThrows(
                StatusRuntimeException.class,
                () -> userServiceBlockingStub.getUser(GetUserRequest.newBuilder()
                        .setId(999L)
                        .build())
        );

        Assertions.assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }
}
```

如果需要测试认证失败，可以在测试中创建一个不带客户端认证拦截器的 Stub，或使用错误 Token 覆盖配置。

文件位置：`src/test/resources/application-auth-fail-test.yml`

```yaml
spring:
  application:
    name: grpc-auth-fail-test

  grpc:
    server:
      port: 0

    client:
      channels:
        user-service:
          address: 127.0.0.1:${local.grpc.port}

app:
  grpc:
    auth:
      # 客户端错误 Token
      token: wrong-token

      # 服务端只允许正确 Token
      allowed-tokens:
        - right-token
```

认证失败测试如下。

文件位置：`src/test/java/io/github/atengk/grpc/integration/UserGrpcAuthFailIntegrationTest.java`

```java
package io.github.atengk.grpc.integration;

import io.github.atengk.grpc.proto.user.v1.GetUserRequest;
import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 用户 gRPC 认证失败集成测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@ActiveProfiles("auth-fail-test")
@SpringBootTest(classes = GrpcIntegrationTestConfig.class)
class UserGrpcAuthFailIntegrationTest {

    private final UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub;

    UserGrpcAuthFailIntegrationTest(UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub) {
        this.userServiceBlockingStub = userServiceBlockingStub;
    }

    /**
     * 测试 Token 错误时返回未认证
     */
    @Test
    void shouldReturnUnauthenticatedWhenTokenInvalid() {
        StatusRuntimeException exception = Assertions.assertThrows(
                StatusRuntimeException.class,
                () -> userServiceBlockingStub.getUser(GetUserRequest.newBuilder()
                        .setId(1L)
                        .build())
        );

        Assertions.assertEquals(Status.Code.UNAUTHENTICATED, exception.getStatus().getCode());
    }
}
```

集成测试建议覆盖以下内容。

| 测试项           | 建议断言                                     |
| ---------------- | -------------------------------------------- |
| 正常调用         | 响应数据正确                                 |
| 参数非法         | `INVALID_ARGUMENT`                           |
| 资源不存在       | `NOT_FOUND`                                  |
| Token 缺失或错误 | `UNAUTHENTICATED`                            |
| 无方法权限       | `PERMISSION_DENIED`                          |
| traceId 透传     | 响应 trailers 或日志上下文存在 traceId       |
| 多 Channel       | 不同服务 Stub 能连接正确地址                 |
| 流式接口         | 校验响应数量、顺序、完成状态                 |
| TLS 配置         | 测试环境至少验证配置能加载                   |
| 超时             | 客户端 Deadline 超时返回 `DEADLINE_EXCEEDED` |

完整验证命令如下。

```bash
# 只执行 proto 编译和单元测试
./mvnw clean test

# 执行完整打包流程
./mvnw clean package

# 指定运行集成测试 Profile
./mvnw test -Dspring.profiles.active=integration-test

# Gradle 执行测试
./gradlew clean test

# Gradle 执行完整构建
./gradlew clean build
```

如果启用了 Reflection，可以在应用启动后使用 `grpcurl` 做手工验证。

```bash
# 查看服务列表
grpcurl -plaintext 127.0.0.1:9090 list

# 查看用户服务方法
grpcurl -plaintext 127.0.0.1:9090 list ateng.user.v1.UserService

# 调用用户查询接口，带 Authorization Metadata
grpcurl \
  -plaintext \
  -H 'authorization: Bearer dev-token' \
  -H 'x-service-name: grpcurl-test' \
  -H 'x-trace-id: manual-test-trace-id' \
  -d '{"id": 1}' \
  127.0.0.1:9090 \
  ateng.user.v1.UserService/GetUser
```

`grpcurl -plaintext` 只适用于明文测试环境。生产环境启用 TLS 后，应使用证书参数调用，不应继续使用明文连接。



## 部署与运行

本章节用于说明 Spring gRPC 应用从本地启动到容器化、Kubernetes 部署和健康检查的完整落地方式。Spring gRPC 服务端可以自动注册标准 gRPC Health 服务，但需要引入 `io.grpc:grpc-services`；当应用中存在 `BindableService` 时，服务端 Health 默认可用，也可以通过 `spring.grpc.server.health.*` 进一步配置。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

### 本地启动验证

本地启动验证主要确认四件事：项目能正常编译 proto、gRPC Server 能监听端口、服务反射或健康检查可用、客户端能够调用服务端接口。

建议先确认配置文件。

文件位置：`src/main/resources/application-dev.yml`

```yaml
server:
  # 如果应用同时提供 REST 接口，HTTP 端口使用 server.port
  port: 8080

spring:
  application:
    name: spring-grpc-demo

  grpc:
    server:
      # gRPC 服务端端口
      port: 9090

      reflection:
        # 开发环境开启 Reflection，方便 grpcurl 查看服务和方法
        enabled: true

      health:
        # 开发环境开启 gRPC 标准健康检查
        enabled: true

    client:
      channels:
        user-service:
          # 本地用户服务地址
          address: 127.0.0.1:9090

app:
  grpc:
    auth:
      # 本地开发 Token
      token: dev-token

      # 服务端允许的 Token
      allowed-tokens:
        - dev-token
```

启动前先编译 proto 并打包。

```bash
# Maven 项目
./mvnw clean package -DskipTests

# Gradle 项目
./gradlew clean build -x test
```

启动应用。

```bash
java -jar target/spring-grpc-demo-1.0.0.jar --spring.profiles.active=dev
```

本地验证端口。

```bash
# 查看 gRPC 端口是否监听
lsof -i:9090

# 查看服务列表，要求已开启 Reflection
grpcurl -plaintext 127.0.0.1:9090 list

# 查看用户服务方法
grpcurl -plaintext 127.0.0.1:9090 list ateng.user.v1.UserService

# 调用用户查询接口
grpcurl \
  -plaintext \
  -H 'authorization: Bearer dev-token' \
  -H 'x-service-name: grpcurl-local' \
  -H 'x-trace-id: local-test-trace-id' \
  -d '{"id": 1}' \
  127.0.0.1:9090 \
  ateng.user.v1.UserService/GetUser
```

如果没有开启 Reflection，`grpcurl list` 无法直接列出服务。此时可以通过 `-proto` 指定本地 proto 文件。

```bash
grpcurl \
  -plaintext \
  -proto src/main/proto/user/user.proto \
  -H 'authorization: Bearer dev-token' \
  -d '{"id": 1}' \
  127.0.0.1:9090 \
  ateng.user.v1.UserService/GetUser
```

本地验证建议如下。

| 验证项     | 命令或方式                                                   |
| ---------- | ------------------------------------------------------------ |
| proto 编译 | `./mvnw clean compile` 或 `./gradlew clean build`            |
| 端口监听   | `lsof -i:9090`                                               |
| 服务列表   | `grpcurl -plaintext 127.0.0.1:9090 list`                     |
| 方法列表   | `grpcurl -plaintext 127.0.0.1:9090 list ateng.user.v1.UserService` |
| 接口调用   | `grpcurl -d '{"id":1}' ... UserService/GetUser`              |
| 日志链路   | 检查日志中是否包含 `traceId`                                 |
| 异常返回   | 使用非法参数验证 `INVALID_ARGUMENT` 或 `NOT_FOUND`           |

### Docker 镜像构建

Docker 镜像构建建议采用多阶段构建：第一阶段使用 Maven 或 Gradle 构建 Jar，第二阶段使用 JRE 基础镜像运行应用。这样可以减少运行镜像体积，避免把 Maven 缓存、源码和构建工具带入生产镜像。

Maven 多阶段 Dockerfile 如下。

文件位置：`Dockerfile`

```dockerfile
# 第一阶段：构建 Spring Boot 可执行 Jar
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# 复制 Maven Wrapper 与配置文件
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 预下载依赖，提升后续构建缓存命中率
RUN chmod +x ./mvnw && ./mvnw -q -DskipTests dependency:go-offline

# 复制源码并执行打包
COPY src src
RUN ./mvnw -q clean package -DskipTests

# 第二阶段：运行应用
FROM eclipse-temurin:21-jre

WORKDIR /app

# 设置容器时区
ENV TZ=Asia/Shanghai

# JVM 参数可在部署时通过环境变量覆盖
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# 复制构建产物
COPY --from=builder /workspace/target/*.jar /app/app.jar

# 暴露 HTTP 与 gRPC 端口
EXPOSE 8080 9090

# 启动应用，默认使用 prod 环境
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
```

构建和运行镜像。

```bash
# 构建镜像
docker build -t spring-grpc-demo:1.0.0 .

# 本地运行容器
docker run -d \
  --name spring-grpc-demo \
  -p 8080:8080 \
  -p 9090:9090 \
  -e SPRING_PROFILES_ACTIVE=dev \
  spring-grpc-demo:1.0.0

# 查看日志
docker logs -f spring-grpc-demo

# 停止并删除容器
docker rm -f spring-grpc-demo
```

如果使用 Gradle，可以改成以下 Dockerfile。

文件位置：`Dockerfile.gradle`

```dockerfile
# 第一阶段：构建 Spring Boot 可执行 Jar
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

# 复制 Gradle Wrapper 与构建文件
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 预下载依赖
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

# 复制源码并构建
COPY src src
RUN ./gradlew clean bootJar -x test --no-daemon

# 第二阶段：运行应用
FROM eclipse-temurin:21-jre

WORKDIR /app

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080 9090

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
```

镜像构建建议如下。

| 项目     | 建议                                                      |
| -------- | --------------------------------------------------------- |
| 基础镜像 | 使用 JRE 运行镜像，构建阶段使用 JDK                       |
| JVM 参数 | 使用 `MaxRAMPercentage` 适配容器内存                      |
| 端口     | 同时暴露 HTTP 管理端口和 gRPC 业务端口                    |
| 配置     | 使用环境变量和 ConfigMap 注入                             |
| 密钥     | 使用 Secret 注入，不要写进镜像                            |
| 证书     | 使用 Secret 挂载，不要提交到 Git                          |
| 健康检查 | 优先交给 Kubernetes Probe，不在镜像里强依赖额外二进制工具 |

### Kubernetes 部署配置

Kubernetes 部署配置需要同时暴露 gRPC 端口和可选的 HTTP Actuator 端口。如果服务只提供 gRPC，也仍然可以通过 Kubernetes 原生 gRPC probe 做健康检查。Kubernetes 的 gRPC probe 在 v1.27 进入稳定状态，要求目标应用实现 gRPC Health Checking Protocol，并且 probe 中必须指定数值端口；gRPC probe 不能使用命名端口，也不能配置自定义 host。([Kubernetes](https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/?utm_source=chatgpt.com))

推荐 Kubernetes 资源如下。

```text
k8s
├── configmap.yaml
├── secret.yaml
├── deployment.yaml
└── service.yaml
```

ConfigMap 用于保存非敏感配置。

文件位置：`k8s/configmap.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: spring-grpc-demo-config
  namespace: default
data:
  SPRING_PROFILES_ACTIVE: "prod"
  JAVA_OPTS: "-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

  # gRPC 客户端远程服务地址
  USER_SERVICE_GRPC_ADDRESS: "user-service.default.svc.cluster.local:9090"
```

Secret 用于保存 Token、证书密码等敏感配置。

文件位置：`k8s/secret.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: spring-grpc-demo-secret
  namespace: default
type: Opaque
stringData:
  # 服务间调用 Token，生产环境建议由密钥系统统一管理
  GRPC_AUTH_TOKEN: "replace-with-secure-token"
```

Deployment 配置如下。

文件位置：`k8s/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-grpc-demo
  namespace: default
  labels:
    app: spring-grpc-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: spring-grpc-demo
  template:
    metadata:
      labels:
        app: spring-grpc-demo
    spec:
      terminationGracePeriodSeconds: 30
      containers:
        - name: spring-grpc-demo
          image: spring-grpc-demo:1.0.0
          imagePullPolicy: IfNotPresent

          ports:
            # HTTP 管理端口，暴露 Actuator 或 REST 接口
            - name: http
              containerPort: 8080

            # gRPC 业务端口
            - name: grpc
              containerPort: 9090

          envFrom:
            - configMapRef:
                name: spring-grpc-demo-config
            - secretRef:
                name: spring-grpc-demo-secret

          env:
            - name: APP_GRPC_AUTH_TOKEN
              valueFrom:
                secretKeyRef:
                  name: spring-grpc-demo-secret
                  key: GRPC_AUTH_TOKEN

          resources:
            requests:
              cpu: "500m"
              memory: "512Mi"
            limits:
              cpu: "1000m"
              memory: "1024Mi"

          # 启动探针：应用启动慢时，避免 liveness 过早重启容器
          startupProbe:
            grpc:
              port: 9090
            periodSeconds: 10
            failureThreshold: 30
            timeoutSeconds: 3

          # 存活探针：判断进程是否仍能提供 gRPC 健康响应
          livenessProbe:
            grpc:
              port: 9090
            periodSeconds: 10
            failureThreshold: 3
            timeoutSeconds: 3

          # 就绪探针：判断当前 Pod 是否可以接收流量
          readinessProbe:
            grpc:
              port: 9090
            periodSeconds: 5
            failureThreshold: 3
            timeoutSeconds: 3
```

Service 配置如下。

文件位置：`k8s/service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: spring-grpc-demo
  namespace: default
  labels:
    app: spring-grpc-demo
spec:
  type: ClusterIP
  selector:
    app: spring-grpc-demo
  ports:
    # HTTP 管理端口
    - name: http
      port: 8080
      targetPort: http

    # gRPC 业务端口
    - name: grpc
      port: 9090
      targetPort: grpc
```

部署命令如下。

```bash
# 应用配置
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# 部署应用和服务
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# 查看 Pod
kubectl get pod -l app=spring-grpc-demo

# 查看服务
kubectl get svc spring-grpc-demo

# 查看日志
kubectl logs -f deployment/spring-grpc-demo
```

如果集群中的 gRPC 流量需要通过 Ingress 或 Gateway 暴露，需要确认网关支持 HTTP/2 和 gRPC 转发。常见做法是内部服务使用 `ClusterIP`，跨命名空间或跨集群调用使用 Service DNS、Service Mesh 或专门的 API Gateway。

### 健康检查

健康检查分为服务端健康状态、客户端健康检查和 Kubernetes 探针三层。gRPC 官方定义了标准 Health Checking Protocol；服务端可以暴露 `grpc.health.v1.Health` 服务，客户端也可以启用健康检查，让客户端在后端不健康时避免继续发送请求。([gRPC](https://grpc.io/docs/guides/health-checking/?utm_source=chatgpt.com))

Spring gRPC 服务端健康检查配置如下。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  grpc:
    server:
      health:
        # 启用 gRPC 标准 Health 服务
        enabled: true

        actuator:
          # 将 Spring Boot Actuator 的健康状态同步到 gRPC Health
          health-indicator-paths:
            - db
            - redis
```

Spring gRPC 文档说明，当引入 Spring Boot Actuator 且 Health endpoint 可用时，框架可以周期性更新 gRPC Health 状态，并可配置要纳入的 health indicator，例如 `db`、`redis`。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

客户端健康检查配置如下。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  grpc:
    client:
      channels:
        user-service:
          address: user-service.default.svc.cluster.local:9090

          health:
            # 启用客户端侧健康检查
            enabled: true

            # 默认可留空，表示检查整体服务状态；也可以指定具体服务名
            service-name: ""
```

Spring gRPC 支持在命名 Channel 上启用客户端健康检查；如果启用客户端健康检查，负载均衡策略需要支持该能力，Spring gRPC 默认使用的 `round_robin` 可以参与健康检查。([Home](https://docs.spring.io/spring-grpc/reference/server.html?utm_source=chatgpt.com))

Kubernetes 原生 gRPC 探针示例。

```yaml
startupProbe:
  grpc:
    # gRPC probe 必须使用数字端口
    port: 9090
  periodSeconds: 10
  failureThreshold: 30
  timeoutSeconds: 3

livenessProbe:
  grpc:
    port: 9090
  periodSeconds: 10
  failureThreshold: 3
  timeoutSeconds: 3

readinessProbe:
  grpc:
    port: 9090
  periodSeconds: 5
  failureThreshold: 3
  timeoutSeconds: 3
```

健康检查建议如下。

| 检查类型        | 目标                     | 建议                                  |
| --------------- | ------------------------ | ------------------------------------- |
| Startup Probe   | 判断应用是否启动完成     | 启动慢的 Spring Boot 应用建议配置     |
| Liveness Probe  | 判断应用是否需要重启     | 不要把下游依赖短暂失败作为重启条件    |
| Readiness Probe | 判断是否接收流量         | 可以关联数据库、Redis、配置加载等状态 |
| gRPC Health     | 判断 gRPC 服务状态       | 推荐启用 `grpc-services`              |
| Actuator Health | 判断 Spring 生态组件状态 | 与 gRPC Health 联动                   |
| 客户端 Health   | 避免调用不健康后端       | 多实例负载均衡时更有价值              |

## 可观测性

本章节用于说明 Spring gRPC 应用的请求日志、指标监控、链路追踪和调用耗时分析。Spring Boot 的可观测性包含日志、指标和追踪三类能力，指标和追踪基于 Micrometer Observation；Spring gRPC 也提供自动配置的拦截器用于服务端和客户端观测，添加 Actuator 以及观测平台桥接后，可以看到 gRPC 的 trace logging 和 metrics。([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-boot/reference/actuator/observability.html?utm_source=chatgpt.com))

### 请求日志

请求日志用于记录 gRPC 方法名、调用方、traceId、业务状态码、耗时和异常信息。建议用拦截器统一记录，业务代码只记录关键业务分支，不要在每个 RPC 方法里重复打印请求入口日志。

日志配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
logging:
  pattern:
    # 输出 traceId 和 spanId，便于跨服务检索
    correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "

  include-application-name: false

  level:
    root: info
    io.github.atengk.grpc: info
    org.springframework.grpc: info
    io.grpc: warn
```

Spring Boot Tracing 文档说明，如果使用 Micrometer Tracing，Spring Boot 默认会把 `traceId` 和 `spanId` 作为 correlation ID 写入日志；也可以通过 `logging.pattern.correlation` 自定义格式。([Home](https://docs.spring.io/spring-boot/reference/actuator/tracing.html?utm_source=chatgpt.com))

下面给出一个请求耗时日志拦截器，适合在没有完整接入 tracing 平台前使用。

文件位置：`src/main/java/io/github/atengk/grpc/observability/GrpcServerAccessLogInterceptor.java`

```java
package io.github.atengk.grpc.observability;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.grpc.common.constant.GrpcMetadataKeys;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC 服务端访问日志拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
public class GrpcServerAccessLogInterceptor implements ServerInterceptor {

    /**
     * 拦截服务端 RPC 调用
     *
     * @param call    服务端调用对象
     * @param headers 请求 Metadata
     * @param next    下一个调用处理器
     * @param <ReqT>  请求类型
     * @param <RespT> 响应类型
     * @return 请求监听器
     */
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        long startTime = SystemClock.now();
        String methodName = call.getMethodDescriptor().getFullMethodName();
        String traceId = StrUtil.blankToDefault(headers.get(GrpcMetadataKeys.TRACE_ID), "");
        String serviceName = StrUtil.blankToDefault(headers.get(GrpcMetadataKeys.SERVICE_NAME), "unknown-service");

        ServerCall<ReqT, RespT> loggingCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {

            @Override
            public void close(Status status, Metadata trailers) {
                long cost = SystemClock.now() - startTime;
                if (status.isOk()) {
                    log.info("gRPC 请求完成，method={}, caller={}, traceId={}, status={}, cost={}ms",
                            methodName, serviceName, traceId, status.getCode(), cost);
                } else {
                    log.warn("gRPC 请求失败，method={}, caller={}, traceId={}, status={}, description={}, cost={}ms",
                            methodName, serviceName, traceId, status.getCode(), status.getDescription(), cost);
                }
                super.close(status, trailers);
            }
        };

        return next.startCall(loggingCall, headers);
    }
}
```

注册访问日志拦截器。

文件位置：`src/main/java/io/github/atengk/grpc/config/GrpcObservabilityConfig.java`

```java
package io.github.atengk.grpc.config;

import io.github.atengk.grpc.observability.GrpcServerAccessLogInterceptor;
import io.grpc.ServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.service.GlobalServerInterceptor;

/**
 * gRPC 可观测性配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class GrpcObservabilityConfig {

    /**
     * gRPC 服务端访问日志拦截器
     *
     * @return 服务端拦截器
     */
    @Bean
    @Order(900)
    @GlobalServerInterceptor
    public ServerInterceptor grpcServerAccessLogInterceptor() {
        return new GrpcServerAccessLogInterceptor();
    }
}
```

请求日志建议如下。

| 字段         | 建议                          |
| ------------ | ----------------------------- |
| method       | 必须记录完整 gRPC 方法名      |
| traceId      | 必须记录，方便跨服务检索      |
| caller       | 建议记录调用方服务名          |
| status       | 必须记录 gRPC Status          |
| cost         | 必须记录耗时                  |
| request body | 默认不记录，敏感场景禁止记录  |
| token        | 禁止记录完整 Token            |
| error        | 业务异常 WARN，系统异常 ERROR |

### 指标监控

指标监控用于统计 gRPC 调用次数、成功率、错误率、耗时分布、消息大小、JVM、线程池、连接池等数据。Spring Boot Actuator 自动配置 Micrometer，并支持 Prometheus、OTLP、Datadog、Dynatrace 等多种监控系统；Prometheus 场景下，Spring Boot 可以通过 `/actuator/prometheus` 暴露 scrape 格式的指标，但该 endpoint 默认不暴露，需要显式开放。([Home](https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com))

Maven 依赖如下。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Actuator：提供 health、metrics、prometheus 等运维端点基础能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Prometheus 指标导出：暴露 /actuator/prometheus -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

Gradle 依赖如下。

文件位置：`build.gradle`

```groovy
dependencies {
    // Spring Boot Actuator：提供健康检查、指标和运维端点
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Prometheus 指标导出
    implementation 'io.micrometer:micrometer-registry-prometheus'
}
```

Actuator 配置如下。

文件位置：`src/main/resources/application-prod.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        # 生产环境按需开放，prometheus 用于指标抓取
        include: health,info,metrics,prometheus

  endpoint:
    health:
      # 按需显示详细健康信息，生产环境通常限制为 when_authorized
      show-details: when_authorized

  metrics:
    tags:
      # 为所有指标增加应用名标签
      application: ${spring.application.name}
```

Prometheus 抓取示例。

文件位置：`prometheus.yml`

```yaml
scrape_configs:
  - job_name: "spring-grpc-demo"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets:
          - "spring-grpc-demo.default.svc.cluster.local:8080"
```

自定义 gRPC 业务指标示例。

文件位置：`src/main/java/io/github/atengk/grpc/observability/GrpcBusinessMetrics.java`

```java
package io.github.atengk.grpc.observability;

import cn.hutool.core.util.StrUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * gRPC 业务指标组件
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Component
@RequiredArgsConstructor
public class GrpcBusinessMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * 记录用户查询次数
     *
     * @param result 调用结果
     */
    public void recordUserQuery(String result) {
        Counter.builder("grpc.user.query.total")
                .description("用户 gRPC 查询次数")
                .tag("result", StrUtil.blankToDefault(result, "unknown"))
                .register(meterRegistry)
                .increment();
    }
}
```

在服务端业务方法中记录指标。

```java
try {
    UserDTO userDTO = userService.getUserById(request.getId());
    grpcBusinessMetrics.recordUserQuery("success");
    responseObserver.onNext(buildResponse(userDTO));
    responseObserver.onCompleted();
} catch (Exception e) {
    grpcBusinessMetrics.recordUserQuery("failure");
    throw e;
}
```

指标监控建议如下。

| 指标类型 | 建议                                   |
| -------- | -------------------------------------- |
| QPS      | 按 method、caller、status 聚合         |
| 错误率   | 按 gRPC Status 聚合                    |
| 延迟     | 使用 Timer 或框架自动指标统计 P95、P99 |
| JVM      | 堆内存、GC、线程数必须监控             |
| 连接池   | 数据库、Redis、HTTP/gRPC 客户端连接池  |
| 流式接口 | 记录消息条数、连接时长、异常关闭数量   |
| 重试     | 记录重试次数和最终状态                 |
| 饱和度   | CPU、内存、线程池队列、限流拒绝数      |

### 链路追踪

链路追踪用于把一次入口请求、多个服务间 gRPC 调用、数据库访问、缓存访问、消息队列调用串联成完整调用链。Spring Boot 使用 Micrometer Observation 支撑指标和追踪；如果使用 Micrometer Tracing，日志中可以自动包含 traceId 和 spanId。([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-boot/reference/actuator/observability.html?utm_source=chatgpt.com))

使用 OTLP 导出到 OpenTelemetry Collector 的依赖示例。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Actuator：可观测性基础能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Micrometer Tracing Bridge：接入 OpenTelemetry -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-otel</artifactId>
    </dependency>

    <!-- OTLP 导出器：将 trace 导出到 OpenTelemetry Collector -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
</dependencies>
```

OTLP 配置示例。

文件位置：`src/main/resources/application-prod.yml`

```yaml
management:
  tracing:
    # 采样比例，生产环境按流量和成本调整
    sampling:
      probability: 0.1

  otlp:
    tracing:
      endpoint: http://otel-collector.default.svc.cluster.local:4318/v1/traces

logging:
  pattern:
    correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
  include-application-name: false
```

如果暂时没有接入完整 tracing 平台，也应至少通过 Metadata 透传 `x-trace-id`，并在服务端、客户端日志中输出同一个 traceId。这样虽然无法看到完整调用拓扑，但仍可以通过日志检索定位一次调用链。

链路追踪建议如下。

| 场景          | 建议                                                   |
| ------------- | ------------------------------------------------------ |
| 单服务调试    | MDC + traceId 足够                                     |
| 多服务调用    | 接入 Micrometer Tracing 或 OpenTelemetry               |
| 高 QPS 服务   | 降低采样率，保留错误调用全量采样                       |
| 异步任务      | 手动传递 trace 上下文                                  |
| MQ 消费       | 从消息 Header 读取 traceId                             |
| gRPC Metadata | 透传 `x-trace-id`，不要透传过多业务字段                |
| 日志关联      | 日志、指标、trace 使用同一 application name 和 traceId |

### 调用耗时分析

调用耗时分析用于定位慢调用来源。gRPC 调用耗时通常由客户端排队、连接建立、TLS 握手、网络传输、服务端排队、业务处理、数据库访问、序列化和客户端反序列化等部分组成。不要只看客户端总耗时，应该结合服务端耗时、下游依赖耗时和 trace span 一起分析。

可以通过 Micrometer `Timer` 对关键业务调用做补充统计。

文件位置：`src/main/java/io/github/atengk/grpc/observability/GrpcCallTimer.java`

```java
package io.github.atengk.grpc.observability;

import cn.hutool.core.util.StrUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * gRPC 调用耗时统计组件
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Component
@RequiredArgsConstructor
public class GrpcCallTimer {

    private final MeterRegistry meterRegistry;

    /**
     * 记录 gRPC 服务端调用耗时
     *
     * @param method 方法名
     * @param status 状态
     * @param costMs 耗时毫秒
     */
    public void recordServerCost(String method, String status, long costMs) {
        Timer.builder("grpc.server.business.duration")
                .description("gRPC 服务端业务调用耗时")
                .tag("method", StrUtil.blankToDefault(method, "unknown"))
                .tag("status", StrUtil.blankToDefault(status, "unknown"))
                .register(meterRegistry)
                .record(costMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录 gRPC 客户端调用耗时
     *
     * @param target 目标服务
     * @param method 方法名
     * @param status 状态
     * @param costMs 耗时毫秒
     */
    public void recordClientCost(String target, String method, String status, long costMs) {
        Timer.builder("grpc.client.business.duration")
                .description("gRPC 客户端业务调用耗时")
                .tag("target", StrUtil.blankToDefault(target, "unknown"))
                .tag("method", StrUtil.blankToDefault(method, "unknown"))
                .tag("status", StrUtil.blankToDefault(status, "unknown"))
                .register(meterRegistry)
                .record(costMs, TimeUnit.MILLISECONDS);
    }
}
```

客户端调用中记录耗时。

```java
long startTime = System.currentTimeMillis();
try {
    GetUserResponse response = userServiceBlockingStub
            .withDeadlineAfter(3, java.util.concurrent.TimeUnit.SECONDS)
            .getUser(request);

    grpcCallTimer.recordClientCost("user-service", "UserService/GetUser", "OK",
            System.currentTimeMillis() - startTime);
    return response;
} catch (Exception e) {
    grpcCallTimer.recordClientCost("user-service", "UserService/GetUser", "ERROR",
            System.currentTimeMillis() - startTime);
    throw e;
}
```

调用耗时分析建议如下。

| 维度           | 分析方式                                 |
| -------------- | ---------------------------------------- |
| 客户端总耗时   | Stub 调用前后计时                        |
| 服务端业务耗时 | 服务端拦截器或业务 Timer                 |
| 下游依赖耗时   | 数据库、Redis、HTTP、MQ 分别监控         |
| P95/P99        | 用 Prometheus、Grafana 或 APM 查看分位数 |
| 超时调用       | 统计 `DEADLINE_EXCEEDED`                 |
| 重试调用       | 统计重试次数和最终状态                   |
| 流式调用       | 统计连接时长、消息间隔、消息数量         |

## 最佳实践

本章节用于总结 Spring gRPC 项目中的 proto 版本管理、接口兼容性、超时重试和流式接口边界。gRPC 项目的长期维护成本主要来自接口演进、调用治理和流式接口复杂度，因此需要在项目初期明确规范。

### proto 版本管理

proto 文件是 gRPC 服务的接口契约，应该像 Java API、数据库表结构和消息事件一样进行版本管理。Protocol Buffers 官方最佳实践明确建议不要复用字段编号，删除字段后应使用 `reserved` 保留字段编号和字段名；客户端和服务端也不能假设总是同时升级或同时回滚。([protobuf.dev](https://protobuf.dev/best-practices/dos-donts/?utm_source=chatgpt.com))

推荐目录结构如下。

```text
src/main/proto
├── common
│   └── v1
│       └── common.proto
├── user
│   ├── v1
│   │   └── user.proto
│   └── v2
│       └── user.proto
└── order
    └── v1
        └── order.proto
```

proto 包名建议包含业务域和版本号。

文件位置：`src/main/proto/user/v1/user.proto`

```proto
syntax = "proto3";

package ateng.user.v1;

option java_multiple_files = true;
option java_package = "io.github.atengk.grpc.proto.user.v1";
option java_outer_classname = "UserProto";

// 用户服务 v1
service UserService {
  // 根据用户 ID 查询用户
  rpc GetUser (GetUserRequest) returns (GetUserResponse);
}

// 查询用户请求
message GetUserRequest {
  int64 id = 1;
}

// 查询用户响应
message GetUserResponse {
  UserDetail user = 1;
}

// 用户详情
message UserDetail {
  int64 id = 1;
  string username = 2;
  string nickname = 3;

  // 删除字段后必须保留字段编号和字段名，避免后续误复用
  reserved 4;
  reserved "old_mobile";
}
```

proto 版本管理建议如下。

| 项目       | 建议                                           |
| ---------- | ---------------------------------------------- |
| 包名       | 使用 `业务域.v1`、`业务域.v2`                  |
| Java 包    | 使用 `io.github.atengk.grpc.proto.<domain>.v1` |
| 文件目录   | 与版本号保持一致                               |
| 字段编号   | 发布后不修改、不复用                           |
| 删除字段   | 使用 `reserved` 保留编号和名称                 |
| 新增字段   | 只追加字段，不破坏旧字段                       |
| 大版本升级 | 新增 v2 包和 v2 service                        |
| 文档       | 每个 RPC 方法说明用途、幂等性、超时建议        |

如果 proto 是多个服务共享的契约，建议单独维护 `proto-contract` 模块，由服务端和客户端共同依赖生成的 Java 代码。

```text
spring-grpc-platform
├── proto-contract
│   └── src/main/proto
├── user-service
│   └── depends on proto-contract
└── order-service
    └── depends on proto-contract
```

### 接口兼容性设计

接口兼容性设计的目标是让新服务端兼容旧客户端，新客户端也能在一定范围内兼容旧服务端。Protobuf 的字段编号是 wire format 的关键，字段编号一旦使用就不能更改；删除字段时应 reserve 旧编号，避免未来误复用导致反序列化错误。([protobuf.dev](https://protobuf.dev/programming-guides/editions/?utm_source=chatgpt.com))

兼容性规则如下。

| 变更类型             | 是否兼容 | 建议                     |
| -------------------- | -------- | ------------------------ |
| 新增可选字段         | 通常兼容 | 推荐                     |
| 删除字段但不复用编号 | 通常兼容 | 使用 `reserved`          |
| 修改字段编号         | 不兼容   | 禁止                     |
| 修改字段类型         | 高风险   | 原则上禁止               |
| 复用字段编号         | 不兼容   | 禁止                     |
| 修改枚举编号         | 不兼容   | 禁止                     |
| 新增枚举值           | 通常兼容 | 客户端要有 unknown 兜底  |
| 修改 RPC 方法语义    | 不兼容   | 新增方法或新版本 service |
| 删除 RPC 方法        | 不兼容   | 先废弃，再迁移，再删除   |
| Unary 改 Streaming   | 不兼容   | 新增 RPC 方法            |

推荐废弃字段时使用 `deprecated` 标记，再配合 `reserved` 完成最终删除。

```proto
message UserDetail {
  int64 id = 1;
  string username = 2;

  // 已废弃：不再使用手机号字段，新接口使用 user_contact
  string mobile = 3 [deprecated = true];

  UserContact user_contact = 4;
}

message UserContact {
  string mobile = 1;
  string email = 2;
}
```

最终删除时保留编号和字段名。

```proto
message UserDetail {
  int64 id = 1;
  string username = 2;

  reserved 3;
  reserved "mobile";

  UserContact user_contact = 4;
}
```

接口兼容性建议如下。

| 设计点   | 建议                                                     |
| -------- | -------------------------------------------------------- |
| 请求对象 | 每个 RPC 使用独立 Request，避免多个接口共享大 Request    |
| 响应对象 | 每个 RPC 使用独立 Response，避免响应语义互相污染         |
| 枚举     | 第一个值使用 `UNSPECIFIED = 0`                           |
| 分页     | 统一 `page_no`、`page_size`、`total`                     |
| 金额     | 使用 `int64 amount_cent` 或 decimal string               |
| 时间     | 跨语言优先使用 `google.protobuf.Timestamp`               |
| 空值     | proto3 默认值要谨慎处理，必要时使用 wrapper 或显式标志位 |
| 错误     | 协议错误用 gRPC Status，业务结果用响应结构               |
| 版本     | 不兼容变更新增 v2，不直接破坏 v1                         |

### 超时与重试配置

超时与重试是 gRPC 调用治理的核心。gRPC 官方 Deadlines 文档说明，默认情况下 gRPC 不设置 deadline，这意味着客户端可能一直等待响应；因此客户端应显式设置合理 deadline。超过 deadline 后，客户端调用会以 `DEADLINE_EXCEEDED` 失败，服务端也会在 deadline 过期后自动取消该调用，但业务代码仍需要主动停止自己派生出的工作。([gRPC](https://grpc.io/docs/guides/deadlines/?utm_source=chatgpt.com))

客户端调用建议始终设置 deadline。

```java
GetUserResponse response = userServiceBlockingStub
        .withDeadlineAfter(3, java.util.concurrent.TimeUnit.SECONDS)
        .getUser(request);
```

可以封装统一的 Stub 工厂，避免业务代码随意设置超时。

文件位置：`src/main/java/io/github/atengk/grpc/client/GrpcStubCustomizer.java`

```java
package io.github.atengk.grpc.client;

import io.github.atengk.grpc.proto.user.v1.UserServiceGrpc;
import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;

/**
 * gRPC Stub 定制工具
 *
 * @author Ateng
 * @since 2026-05-05
 */
@UtilityClass
public class GrpcStubCustomizer {

    /**
     * 设置用户服务默认超时
     *
     * @param stub 原始 Stub
     * @return 设置超时后的 Stub
     */
    public UserServiceGrpc.UserServiceBlockingStub withDefaultDeadline(UserServiceGrpc.UserServiceBlockingStub stub) {
        return stub.withDeadlineAfter(3, TimeUnit.SECONDS);
    }

    /**
     * 设置用户服务长查询超时
     *
     * @param stub 原始 Stub
     * @return 设置超时后的 Stub
     */
    public UserServiceGrpc.UserServiceBlockingStub withLongQueryDeadline(UserServiceGrpc.UserServiceBlockingStub stub) {
        return stub.withDeadlineAfter(10, TimeUnit.SECONDS);
    }
}
```

重试需要谨慎。gRPC 官方 Retry 文档说明，gRPC 支持基于 Service Config 的重试策略，重试策略可以设置最大尝试次数、初始退避、最大退避、退避倍数和可重试状态码；没有显式 retry policy 时，gRPC 只会在非常有限的场景进行透明重试。([gRPC](https://grpc.io/docs/guides/retry/?utm_source=chatgpt.com))

推荐重试规则如下。

| 接口类型   | 是否建议重试   | 说明                         |
| ---------- | -------------- | ---------------------------- |
| 查询接口   | 可以重试       | 必须幂等                     |
| 创建接口   | 谨慎重试       | 需要幂等键                   |
| 修改接口   | 谨慎重试       | 需要版本号或幂等控制         |
| 删除接口   | 谨慎重试       | 需要幂等语义                 |
| 支付扣款   | 默认不自动重试 | 必须通过业务幂等单号控制     |
| 文件上传流 | 不建议透明重试 | 需要断点续传或业务补偿       |
| 双向流式   | 不建议自动重试 | 连接状态复杂，需业务恢复机制 |

如果使用 gRPC Service Config 配置重试，建议只对 `UNAVAILABLE`、短暂网络错误等可恢复状态做有限重试，并配合业务幂等。

```json
{
  "methodConfig": [
    {
      "name": [
        {
          "service": "ateng.user.v1.UserService",
          "method": "GetUser"
        }
      ],
      "timeout": "3s",
      "retryPolicy": {
        "maxAttempts": 3,
        "initialBackoff": "0.1s",
        "maxBackoff": "1s",
        "backoffMultiplier": 2,
        "retryableStatusCodes": [
          "UNAVAILABLE"
        ]
      }
    }
  ]
}
```

超时与重试建议如下。

| 项目       | 建议                               |
| ---------- | ---------------------------------- |
| 默认超时   | 所有客户端调用必须设置 deadline    |
| 查询接口   | 1 到 3 秒起步，根据 P99 调整       |
| 写接口     | 3 到 10 秒，视事务复杂度调整       |
| 流式接口   | 使用更长 deadline 或业务心跳       |
| 重试次数   | 通常不超过 2 到 3 次               |
| 重试状态码 | 优先只重试 `UNAVAILABLE`           |
| 幂等控制   | 写接口重试前必须有幂等键           |
| 降级       | 下游不可用时返回兜底结果或失败状态 |
| 监控       | 统计重试次数、最终失败率和超时率   |

### 流式接口使用边界

流式接口包括 Server Streaming、Client Streaming 和 Bidirectional Streaming。它们能力强，但治理成本也更高。gRPC 官方 Flow Control 文档说明，流控用于避免接收方被快速发送方压垮，主要作用于 streaming RPC；gRPC Java 的 `StreamObserver` 文档也提醒，单个 `StreamObserver` 实现不要求线程安全，如果多个线程并发写同一个 `StreamObserver`，应用侧必须自行同步。([gRPC](https://grpc.io/docs/guides/flow-control/?utm_source=chatgpt.com))

流式接口适用边界如下。

| 流式类型                | 适合场景                           | 不适合场景                       |
| ----------------------- | ---------------------------------- | -------------------------------- |
| Server Streaming        | 大列表分批返回、日志推送、进度推送 | 普通分页查询、小结果集查询       |
| Client Streaming        | 批量导入、批量上报、文件分片上传   | 单条创建、普通表单提交           |
| Bidirectional Streaming | 实时同步、交互式任务、长连接协作   | 普通 CRUD、简单状态修改          |
| Unary                   | 默认首选                           | 超大数据、持续推送、双向实时交互 |

Server Streaming 示例中，应限制最大返回条数。

```java
@Override
public void streamUsers(StreamUsersRequest request, StreamObserver<UserDetail> responseObserver) {
    int pageSize = Math.min(Math.max(request.getPageSize(), 1), 1000);

    List<UserDetail> users = userMockRepository.listUsers(request.getEnabledOnly(), pageSize);
    for (UserDetail user : users) {
        responseObserver.onNext(user);
    }

    responseObserver.onCompleted();
}
```

Client Streaming 示例中，应限制最大接收条数。

```java
@Override
public StreamObserver<ImportUserRequest> importUsers(StreamObserver<ImportUserResponse> responseObserver) {
    AtomicInteger totalCount = new AtomicInteger();
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger failureCount = new AtomicInteger();

    return new StreamObserver<>() {

        @Override
        public void onNext(ImportUserRequest request) {
            if (totalCount.incrementAndGet() > 10000) {
                responseObserver.onError(io.grpc.Status.RESOURCE_EXHAUSTED
                        .withDescription("单次导入用户数量超过限制")
                        .asRuntimeException());
                return;
            }

            if (userMockRepository.validUsername(request.getUsername())) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("客户端流式导入用户异常", throwable);
        }

        @Override
        public void onCompleted() {
            ImportUserResponse response = ImportUserResponse.newBuilder()
                    .setSuccessCount(successCount.get())
                    .setFailureCount(failureCount.get())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    };
}
```

双向流式接口建议增加业务心跳、最大连接时长、最大消息数量和异常关闭策略。

```java
@Override
public StreamObserver<SyncUserStatusRequest> syncUserStatus(StreamObserver<SyncUserStatusResponse> responseObserver) {
    AtomicInteger messageCount = new AtomicInteger();

    return new StreamObserver<>() {

        @Override
        public void onNext(SyncUserStatusRequest request) {
            if (messageCount.incrementAndGet() > 5000) {
                responseObserver.onError(io.grpc.Status.RESOURCE_EXHAUSTED
                        .withDescription("双向流式消息数量超过限制")
                        .asRuntimeException());
                return;
            }

            SyncUserStatusResponse response = SyncUserStatusResponse.newBuilder()
                    .setId(request.getId())
                    .setSuccess(request.getId() > 0)
                    .setMessage(request.getId() > 0 ? "同步成功" : "用户 ID 非法")
                    .build();

            responseObserver.onNext(response);
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("双向流式同步异常", throwable);
        }

        @Override
        public void onCompleted() {
            log.info("双向流式同步完成，messageCount={}", messageCount.get());
            responseObserver.onCompleted();
        }
    };
}
```

流式接口设计建议如下。

| 项目       | 建议                                         |
| ---------- | -------------------------------------------- |
| 默认选择   | 普通业务优先 Unary                           |
| 数量限制   | 每个流必须有最大消息数或最大持续时间         |
| 超时控制   | 客户端和服务端都要有 deadline 或业务超时     |
| 流控       | 大量数据传输时关注背压和缓冲                 |
| 并发写     | 不要多个线程直接写同一个 `StreamObserver`    |
| 错误处理   | 明确 `onError` 后资源释放逻辑                |
| 客户端取消 | 服务端应尽快停止后台任务                     |
| 幂等恢复   | 流式中断后要能重新同步或补偿                 |
| 监控       | 记录流数量、消息数、持续时长、异常关闭次数   |
| 文档       | 明确客户端发送顺序、结束条件和服务端响应语义 |

最终建议是：除非确实需要持续数据传输或双向交互，否则不要为了“高级”而使用流式 RPC。绝大多数内部服务调用使用 Unary RPC 更容易测试、治理、监控和降级。