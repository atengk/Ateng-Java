# SSE

SSE（Server-Sent Events）是一种基于 HTTP 的服务端单向推送技术，适合在 Spring Boot 3 项目中实现消息通知、任务进度、实时日志、状态刷新等功能。Spring MVC 提供了 `SseEmitter`，它是 `ResponseBodyEmitter` 的 SSE 专用实现，可用于向客户端发送标准 SSE 事件流。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html?utm_source=chatgpt.com))

## SSE 概述

本节主要说明 SSE 的功能定位、典型适用场景，以及它和 WebSocket 的区别。SSE 的核心特点是服务端主动推送、客户端被动接收、基于 HTTP 长连接，适合实现轻量级实时通知类功能。

### 功能定位

SSE 的功能定位是建立一条从服务端到客户端的持续推送通道。客户端通过浏览器原生 `EventSource` API 发起连接，服务端保持 HTTP 连接不断开，并以 `text/event-stream` 格式持续向客户端写入事件数据。

在 Spring Boot 3 中，服务端通常通过 `SseEmitter` 创建 SSE 连接。Controller 方法返回 `SseEmitter` 后，HTTP 响应不会立即结束，后端可以在异步任务、定时任务、消息队列消费线程或业务处理线程中继续向客户端发送消息。

SSE 主要提供以下能力：

| 能力           | 说明                                           |
| -------------- | ---------------------------------------------- |
| 服务端主动推送 | 服务端可以在业务状态变化时主动向客户端发送消息 |
| 长连接保持     | 客户端建立连接后，服务端可以持续写入事件流     |
| 事件类型区分   | 可通过事件名称区分通知、进度、日志、心跳等消息 |
| 自动重连       | 浏览器 `EventSource` 默认具备断线重连能力      |
| 文本数据传输   | 适合传输字符串、JSON 文本、状态信息等内容      |
| 实现成本较低   | 基于 HTTP，不需要额外引入 WebSocket 协议栈     |

需要注意的是，SSE 是单向推送机制。它适合服务端向客户端持续发送数据，不适合客户端和服务端通过同一条连接进行高频双向通信。

### 适用场景

SSE 适合“前端订阅，后端推送”的业务模型。常见模式是前端先通过普通 HTTP 接口发起任务，然后通过 SSE 连接接收后端的进度、状态或结果通知。

典型适用场景如下：

| 场景         | 说明                                          |
| ------------ | --------------------------------------------- |
| 系统消息通知 | 例如站内信、审批提醒、告警通知、待办数量变化  |
| 任务进度推送 | 例如文件导入、批量处理、报表生成、AI 生成进度 |
| 实时日志输出 | 例如后台任务日志、部署日志、数据同步日志      |
| 订单状态刷新 | 例如支付状态、配送状态、审核状态变更          |
| 设备状态监控 | 例如设备上线、离线、异常状态推送              |
| 数据看板刷新 | 例如运营大屏、实时指标、统计数据变化          |
| 异步结果通知 | 例如异步计算完成后通知前端刷新页面            |

不建议使用 SSE 的场景如下：

| 场景           | 原因                                         |
| -------------- | -------------------------------------------- |
| 聊天系统       | 需要客户端和服务端高频双向通信               |
| 多人协同编辑   | 需要复杂的双向同步机制                       |
| 实时游戏       | 对延迟、双向交互和消息频率要求较高           |
| 二进制数据传输 | SSE 主要面向文本事件流                       |
| 极高并发长连接 | 需要额外评估服务端连接数、线程模型和网关限制 |

对于大多数后台管理系统，SSE 非常适合用于“通知类”“状态类”“进度类”功能，开发和维护成本通常低于 WebSocket。

### 与 WebSocket 的区别

SSE 和 WebSocket 都可以用于实时通信，但两者的通信模型不同。SSE 是基于 HTTP 的服务端单向推送，WebSocket 是独立的全双工通信协议。

| 对比项     | SSE                                | WebSocket                                  |
| ---------- | ---------------------------------- | ------------------------------------------ |
| 通信方向   | 服务端到客户端单向推送             | 客户端和服务端双向通信                     |
| 协议基础   | HTTP                               | WebSocket 协议，通常通过 HTTP Upgrade 建立 |
| 浏览器 API | `EventSource`                      | `WebSocket`                                |
| 数据格式   | 文本事件流，常用 JSON 字符串       | 文本或二进制                               |
| 自动重连   | 浏览器原生支持                     | 通常需要业务自行实现                       |
| 实现复杂度 | 较低                               | 较高                                       |
| 适用场景   | 通知、进度、日志、状态刷新         | 聊天、协作、游戏、实时控制                 |
| 服务端实现 | Spring MVC 可直接使用 `SseEmitter` | 需要 WebSocket 端点和会话管理              |
| 代理兼容性 | 复用 HTTP 链路，接入相对简单       | 需要代理和网关支持 WebSocket               |

技术选型建议如下：

| 需求                             | 推荐方案  |
| -------------------------------- | --------- |
| 只需要服务端推送数据到浏览器     | SSE       |
| 需要客户端和服务端频繁双向通信   | WebSocket |
| 需要推送任务进度、日志、通知     | SSE       |
| 需要实现聊天、协同编辑、实时控制 | WebSocket |
| 希望快速接入已有 Spring MVC 项目 | SSE       |

在 Spring Boot 3 后台系统中，如果业务只是推送通知、进度或状态，优先选择 SSE。如果业务需要稳定的双向实时通信，再选择 WebSocket。

## 环境准备

本节说明 Spring Boot 3 开发 SSE 功能需要准备的基础环境、Maven 依赖和推荐项目目录结构。SSE 本身不需要引入专门的第三方组件，使用 `spring-boot-starter-web` 即可获得 Spring MVC 和 `SseEmitter` 支持。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html?utm_source=chatgpt.com))

### Spring Boot 版本要求

Spring Boot 3 要求使用 Java 17 或更高版本。以 Spring Boot 3.3.x 维护线为例，官方系统要求中说明 Spring Boot 3.3.16 至少需要 Java 17，构建工具 Maven 需要 3.6.3 或更高版本，默认嵌入式 Tomcat 版本线为 Tomcat 10.1。([Spring Enterprise 文档](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

推荐环境如下：

| 环境             | 推荐版本     | 说明                                            |
| ---------------- | ------------ | ----------------------------------------------- |
| JDK              | 17+          | Spring Boot 3 的最低 Java 要求                  |
| Spring Boot      | 3.x          | 建议使用项目统一版本                            |
| Maven            | 3.6.3+       | 用于依赖管理和项目构建                          |
| Servlet 容器     | Tomcat 10.1+ | `spring-boot-starter-web` 默认使用嵌入式 Tomcat |
| Spring Framework | 6.x          | Spring Boot 3 对应 Spring Framework 6 体系      |

开发 SSE 功能时，需要注意以下几点：

1. 项目必须是 Web 项目，需要引入 `spring-boot-starter-web`。
2. SSE 接口响应类型应声明为 `text/event-stream`。
3. SSE 连接接口应直接返回 `SseEmitter`。
4. 不要将 SSE 长连接接口包装成普通 JSON 响应。
5. 生产环境需要关注网关、Nginx、超时、心跳和连接数限制。

### Maven 依赖配置

下面是 Spring Boot 3 SSE 项目的基础 Maven 依赖配置。核心依赖是 `spring-boot-starter-web`，其中已经包含 Spring MVC 相关能力；`hutool-core` 用于后续处理字符串、集合、ID、时间等通用逻辑；`lombok` 用于简化日志和对象代码。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring MVC Web 支持，包含 Controller、Tomcat、JSON 序列化和 SseEmitter 所需能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验支持，用于接口参数、DTO 字段校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于 ID、时间、字符串、集合等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-core</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok 简化实体、DTO、日志对象代码，编译期生效 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于接口、服务和上下文测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot 官方父工程管理依赖版本，可以按下面方式配置：

文件位置：`pom.xml`

```xml
<parent>
    <!-- Spring Boot 父工程，统一管理插件和常用依赖版本 -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.6</version>
    <relativePath/>
</parent>

<properties>
    <!-- Spring Boot 3 最低要求 Java 17 -->
    <java.version>17</java.version>

    <!-- 项目源码编码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

SSE 不需要引入 `spring-boot-starter-websocket`。只有在同一个项目中同时需要 WebSocket 双向通信能力时，才需要额外引入 WebSocket 相关依赖。

### 项目目录结构

下面是推荐的 Spring Boot 3 SSE 示例项目目录结构。包路径使用 `io.github.atengk`，整体按照 Controller、Service、Manager、DTO、VO、Config 分层组织，便于后续扩展连接管理、指定用户推送、广播推送、心跳保活和异常处理。

```text
springboot3-sse-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               └── sse
│   │   │                   ├── SseApplication.java
│   │   │                   ├── config
│   │   │                   │   └── WebMvcConfig.java
│   │   │                   ├── controller
│   │   │                   │   └── SseController.java
│   │   │                   ├── service
│   │   │                   │   ├── SseService.java
│   │   │                   │   └── impl
│   │   │                   │       └── SseServiceImpl.java
│   │   │                   ├── manager
│   │   │                   │   └── SseEmitterManager.java
│   │   │                   ├── model
│   │   │                   │   ├── dto
│   │   │                   │   │   └── SseMessageDTO.java
│   │   │                   │   └── vo
│   │   │                   │       └── SseConnectVO.java
│   │   │                   └── common
│   │   │                       └── result
│   │   │                           └── Result.java
│   │   └── resources
│   │       ├── application.yml
│   │       └── static
│   │           └── sse-demo.html
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── sse
│                           └── SseApplicationTests.java
```

各目录职责如下：

| 路径              | 职责                                                |
| ----------------- | --------------------------------------------------- |
| `controller`      | 提供 SSE 连接接口、指定用户推送接口、广播推送接口   |
| `service`         | 定义 SSE 业务操作，例如创建连接、发送消息、关闭连接 |
| `service/impl`    | 实现 SSE 业务逻辑                                   |
| `manager`         | 统一管理 `SseEmitter` 连接对象                      |
| `model/dto`       | 定义请求参数对象，例如推送消息内容、目标用户 ID     |
| `model/vo`        | 定义返回给前端的连接状态、连接 ID、用户标识等信息   |
| `config`          | 配置跨域、异步线程池、MVC 行为等                    |
| `common/result`   | 普通业务接口统一响应结构                            |
| `static`          | 放置本地测试页面，例如 `sse-demo.html`              |
| `application.yml` | 配置服务端口、SSE 超时时间、心跳间隔、日志级别等    |

建议提前在 `application.yml` 中预留 SSE 配置，后续实现连接超时、心跳保活和连接数量限制时可以直接使用。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 服务启动端口
  port: 8080

spring:
  application:
    # 应用名称
    name: springboot3-sse-demo

sse:
  # SSE 连接超时时间，单位毫秒；0 表示不主动超时
  timeout: 0

  # 心跳间隔，单位秒，用于保持连接活跃
  heartbeat-interval: 30

  # 单个用户允许保留的最大连接数
  max-connection-per-user: 3

logging:
  level:
    # 项目包日志级别
    io.github.atengk.sse: info
```

目录结构设计时需要特别注意：普通业务接口可以返回统一响应对象，例如 `Result<T>`；但 SSE 连接接口应直接返回 `SseEmitter`，并通过 `produces = MediaType.TEXT_EVENT_STREAM_VALUE` 声明响应类型，避免被统一响应增强器转换为普通 JSON 响应。

## 核心实现

本节给出 Spring Boot 3 中 SSE 的核心后端实现，包括连接接口、`SseEmitter` 创建与管理、消息推送和资源释放。Spring MVC 的 `SseEmitter` 是 `ResponseBodyEmitter` 的 SSE 专用实现，支持通过 `send(Object)` 或 `send(SseEmitter.SseEventBuilder)` 向客户端发送事件数据。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html?utm_source=chatgpt.com)) 本节内容延续你提供的大纲结构。

### SSE 连接接口

SSE 连接接口用于让客户端建立长连接。接口需要返回 `SseEmitter`，并声明响应类型为 `text/event-stream`。普通业务接口可以返回 JSON，但 SSE 连接接口不要返回统一响应对象，否则可能导致响应格式被包装，前端 `EventSource` 无法正确识别事件流。

本示例提供三个接口：

| 接口                        | 方法     | 说明                        |
| --------------------------- | -------- | --------------------------- |
| `/api/sse/connect/{userId}` | `GET`    | 建立指定用户的 SSE 连接     |
| `/api/sse/push`             | `POST`   | 向指定用户推送消息          |
| `/api/sse/close/{userId}`   | `DELETE` | 主动关闭指定用户的 SSE 连接 |

下面的 Controller 提供 SSE 连接、指定用户推送和主动关闭连接接口。

文件位置：`src/main/java/io/github/atengk/sse/controller/SseController.java`

```java
package io.github.atengk.sse.controller;

import io.github.atengk.sse.model.dto.SseMessageDTO;
import io.github.atengk.sse.service.SseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SSE 推送控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    /**
     * 建立 SSE 连接
     *
     * @param userId 用户 ID
     * @return SSE 连接对象
     */
    @GetMapping(value = "/connect/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String userId) {
        return sseService.connect(userId);
    }

    /**
     * 向指定用户推送消息
     *
     * @param messageDTO 消息请求参数
     * @return 推送结果
     */
    @PostMapping("/push")
    public Map<String, Object> push(@Valid @RequestBody SseMessageDTO messageDTO) {
        boolean success = sseService.sendToUser(messageDTO);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("message", success ? "消息推送成功" : "用户连接不存在或推送失败");
        return result;
    }

    /**
     * 主动关闭指定用户的 SSE 连接
     *
     * @param userId 用户 ID
     * @return 关闭结果
     */
    @DeleteMapping("/close/{userId}")
    public Map<String, Object> close(@PathVariable String userId) {
        sseService.close(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "连接关闭成功");
        return result;
    }
}
```

连接接口使用方式如下：

```bash
curl -N http://localhost:8080/api/sse/connect/user-1001
```

这里的 `-N` 表示关闭 curl 的输出缓冲，使服务端推送的数据能够实时显示。`user-1001` 是示例用户 ID，实际项目中可以替换为登录用户 ID、租户用户 ID 或业务客户端 ID。

### SseEmitter 创建与管理

`SseEmitter` 的管理是 SSE 功能的核心。服务端需要保存用户和连接之间的映射关系，后续才能根据用户 ID 主动推送消息。`SseEmitter` 支持自定义超时时间，超时时间单位为毫秒；如果不设置，则使用 MVC 配置或底层服务容器的默认超时策略。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html?utm_source=chatgpt.com))

本示例使用 `ConcurrentHashMap` 保存连接，使用 `SseEmitter` 的 `onCompletion`、`onTimeout`、`onError` 回调处理资源释放。

下面的配置类用于读取 `application.yml` 中的 SSE 配置。

文件位置：`src/main/java/io/github/atengk/sse/config/SseProperties.java`

```java
package io.github.atengk.sse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SSE 配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "sse")
public class SseProperties {

    /**
     * SSE 连接超时时间，单位毫秒
     */
    private Long timeout = 0L;

    /**
     * 心跳间隔，单位秒
     */
    private Integer heartbeatInterval = 30;

    /**
     * 单个用户最大连接数
     */
    private Integer maxConnectionPerUser = 3;
}
```

下面的 Manager 负责统一创建、保存、获取、删除和关闭 `SseEmitter` 连接。

文件位置：`src/main/java/io/github/atengk/sse/manager/SseEmitterManager.java`

```java
package io.github.atengk.sse.manager;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.sse.config.SseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterManager {

    private final SseProperties sseProperties;

    /**
     * 用户连接缓存，key 为用户 ID，value 为 SseEmitter
     */
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 创建 SSE 连接
     *
     * @param userId 用户 ID
     * @return SSE 连接对象
     */
    public SseEmitter create(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        close(userId);

        SseEmitter emitter = new SseEmitter(sseProperties.getTimeout());
        emitterMap.put(userId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE 连接完成，用户ID：{}", userId);
            remove(userId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时，用户ID：{}", userId);
            remove(userId);
        });

        emitter.onError(throwable -> {
            log.warn("SSE 连接异常，用户ID：{}，异常信息：{}", userId, throwable.getMessage());
            remove(userId);
        });

        log.info("SSE 连接创建成功，用户ID：{}，连接ID：{}", userId, IdUtil.fastSimpleUUID());
        return emitter;
    }

    /**
     * 获取用户连接
     *
     * @param userId 用户 ID
     * @return SSE 连接对象
     */
    public SseEmitter get(String userId) {
        if (StrUtil.isBlank(userId)) {
            return null;
        }
        return emitterMap.get(userId);
    }

    /**
     * 移除用户连接
     *
     * @param userId 用户 ID
     */
    public void remove(String userId) {
        if (StrUtil.isBlank(userId)) {
            return;
        }
        emitterMap.remove(userId);
        log.info("SSE 连接已移除，用户ID：{}", userId);
    }

    /**
     * 关闭用户连接
     *
     * @param userId 用户 ID
     */
    public void close(String userId) {
        SseEmitter emitter = get(userId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.complete();
            log.info("SSE 连接已主动关闭，用户ID：{}", userId);
        } catch (Exception e) {
            log.warn("SSE 连接关闭异常，用户ID：{}，异常信息：{}", userId, e.getMessage());
        } finally {
            remove(userId);
        }
    }

    /**
     * 获取当前连接数
     *
     * @return 当前连接数
     */
    public int count() {
        return emitterMap.size();
    }
}
```

这段实现采用“同一用户只保留一个连接”的策略。新连接建立时会先关闭旧连接，避免同一用户刷新页面后残留多个旧连接。如果业务需要支持多端登录，可以将缓存结构改为 `Map<String, Map<String, SseEmitter>>`，其中第一层 key 是用户 ID，第二层 key 是连接 ID。

### 消息推送实现

消息推送一般通过业务接口、定时任务、MQ 消费者或异步任务触发。服务端根据用户 ID 找到对应的 `SseEmitter`，然后通过 `send` 方法向客户端发送事件。

`SseEmitter.event()` 可以构造标准 SSE 事件，常用字段包括：

| 字段            | 说明                                         |
| --------------- | -------------------------------------------- |
| `id`            | 事件 ID，便于客户端记录消息位置              |
| `name`          | 事件名称，前端可通过 `addEventListener` 监听 |
| `data`          | 事件数据，通常是字符串或 JSON 对象           |
| `reconnectTime` | 建议客户端断线后的重连时间                   |

下面先定义消息请求 DTO。

文件位置：`src/main/java/io/github/atengk/sse/model/dto/SseMessageDTO.java`

```java
package io.github.atengk.sse.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSE 消息请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class SseMessageDTO {

    /**
     * 用户 ID
     */
    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;
}
```

下面定义 SSE 服务接口。

文件位置：`src/main/java/io/github/atengk/sse/service/SseService.java`

```java
package io.github.atengk.sse.service;

import io.github.atengk.sse.model.dto.SseMessageDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 服务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface SseService {

    /**
     * 建立 SSE 连接
     *
     * @param userId 用户 ID
     * @return SSE 连接对象
     */
    SseEmitter connect(String userId);

    /**
     * 向指定用户推送消息
     *
     * @param messageDTO 消息参数
     * @return 是否推送成功
     */
    boolean sendToUser(SseMessageDTO messageDTO);

    /**
     * 关闭指定用户连接
     *
     * @param userId 用户 ID
     */
    void close(String userId);
}
```

下面是 SSE 服务实现类，包含连接建立、首次连接成功事件、指定用户消息推送和异常清理逻辑。

文件位置：`src/main/java/io/github/atengk/sse/service/impl/SseServiceImpl.java`

```java
package io.github.atengk.sse.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.sse.manager.SseEmitterManager;
import io.github.atengk.sse.model.dto.SseMessageDTO;
import io.github.atengk.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * SSE 服务实现类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseServiceImpl implements SseService {

    private static final String DEFAULT_EVENT_NAME = "message";

    private final SseEmitterManager sseEmitterManager;

    /**
     * 建立 SSE 连接
     *
     * @param userId 用户 ID
     * @return SSE 连接对象
     */
    @Override
    public SseEmitter connect(String userId) {
        SseEmitter emitter = sseEmitterManager.create(userId);

        try {
            emitter.send(SseEmitter.event()
                    .id(IdUtil.fastSimpleUUID())
                    .name("connect")
                    .data("SSE 连接成功")
                    .reconnectTime(3000L));

            log.info("SSE 首次连接消息发送成功，用户ID：{}", userId);
        } catch (IOException e) {
            log.warn("SSE 首次连接消息发送失败，用户ID：{}，异常信息：{}", userId, e.getMessage());
            sseEmitterManager.remove(userId);
        }

        return emitter;
    }

    /**
     * 向指定用户推送消息
     *
     * @param messageDTO 消息参数
     * @return 是否推送成功
     */
    @Override
    public boolean sendToUser(SseMessageDTO messageDTO) {
        String userId = messageDTO.getUserId();
        SseEmitter emitter = sseEmitterManager.get(userId);

        if (emitter == null) {
            log.warn("SSE 消息推送失败，用户连接不存在，用户ID：{}", userId);
            return false;
        }

        String eventName = StrUtil.blankToDefault(messageDTO.getEventName(), DEFAULT_EVENT_NAME);

        try {
            emitter.send(SseEmitter.event()
                    .id(IdUtil.fastSimpleUUID())
                    .name(eventName)
                    .data(messageDTO.getContent())
                    .reconnectTime(3000L));

            log.info("SSE 消息推送成功，用户ID：{}，事件名称：{}", userId, eventName);
            return true;
        } catch (IOException e) {
            log.warn("SSE 消息推送异常，用户ID：{}，异常信息：{}", userId, e.getMessage());
            sseEmitterManager.remove(userId);
            return false;
        }
    }

    /**
     * 关闭指定用户连接
     *
     * @param userId 用户 ID
     */
    @Override
    public void close(String userId) {
        sseEmitterManager.close(userId);
    }
}
```

推送消息的调用示例如下：

```bash
curl -X POST http://localhost:8080/api/sse/push \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-1001",
    "eventName": "notice",
    "content": "你有一条新的系统通知"
  }'
```

参数说明如下：

| 参数        | 是否必填 | 说明                                       |
| ----------- | -------- | ------------------------------------------ |
| `userId`    | 是       | 接收消息的用户 ID                          |
| `eventName` | 否       | SSE 事件名称，不传时默认为 `message`       |
| `content`   | 是       | 推送消息内容，可以是普通文本或 JSON 字符串 |

如果 `eventName` 为 `notice`，前端需要使用 `addEventListener("notice", callback)` 监听。如果不指定事件名称，前端可以使用 `onmessage` 或 `addEventListener("message", callback)` 监听。

### 连接关闭与资源释放

SSE 是长连接，连接关闭和资源释放必须明确处理。常见关闭场景包括：客户端主动关闭页面、浏览器刷新、网络断开、服务端超时、服务端主动关闭连接、服务端发送消息异常。

后端需要重点处理以下回调：

| 回调           | 触发场景       | 处理方式           |
| -------------- | -------------- | ------------------ |
| `onCompletion` | 连接正常结束   | 从连接缓存中移除   |
| `onTimeout`    | 连接超时       | 从连接缓存中移除   |
| `onError`      | 连接异常       | 从连接缓存中移除   |
| `complete()`   | 服务端主动关闭 | 完成响应并移除连接 |

在上面的 `SseEmitterManager` 中，连接创建时已经注册了 `onCompletion`、`onTimeout` 和 `onError` 回调。主动关闭连接可以通过以下接口触发：

```bash
curl -X DELETE http://localhost:8080/api/sse/close/user-1001
```

返回示例：

```json
{
  "success": true,
  "message": "连接关闭成功"
}
```

生产环境中建议额外增加心跳机制。心跳并不是业务消息，而是用于防止代理、网关或浏览器长时间无数据时关闭连接。心跳可以通过定时任务向客户端发送 `heartbeat` 事件实现，后续“生产注意事项”章节可以展开说明。

## 客户端接入

本节说明浏览器如何通过 `EventSource` 接入 SSE 服务端接口。`EventSource` 会打开一条到服务端的持久连接，服务端以 `text/event-stream` 格式发送事件数据，连接会保持打开状态，直到调用 `EventSource.close()` 或连接被服务端关闭。([MDN Web Docs](https://developer.mozilla.org/en-US/docs/Web/API/EventSource?utm_source=chatgpt.com))

### EventSource 基础使用

浏览器原生支持 `EventSource`，不需要安装额外依赖。前端只需要传入 SSE 接口地址即可建立连接。

下面是一个基础 HTML 测试页面，用于连接后端 `/api/sse/connect/{userId}` 接口，并接收服务端推送的消息。

文件位置：`src/main/resources/static/sse-demo.html`

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>Spring Boot 3 SSE 测试页面</title>
    <style>
        body {
            font-family: Arial, "Microsoft YaHei", sans-serif;
            padding: 24px;
            background: #f7f8fa;
        }

        .container {
            max-width: 900px;
            margin: 0 auto;
            background: #ffffff;
            padding: 24px;
            border-radius: 8px;
            box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
        }

        .row {
            margin-bottom: 16px;
        }

        input {
            width: 240px;
            padding: 8px;
        }

        button {
            padding: 8px 14px;
            margin-right: 8px;
            cursor: pointer;
        }

        #status {
            font-weight: bold;
        }

        #messageList {
            padding-left: 20px;
        }

        .log {
            margin-bottom: 8px;
        }
    </style>
</head>
<body>
<div class="container">
    <h2>Spring Boot 3 SSE 测试页面</h2>

    <div class="row">
        <label for="userId">用户 ID：</label>
        <input id="userId" value="user-1001">
        <button onclick="connectSse()">建立连接</button>
        <button onclick="closeSse()">关闭连接</button>
    </div>

    <div class="row">
        当前状态：<span id="status">未连接</span>
    </div>

    <h3>消息列表</h3>
    <ul id="messageList"></ul>
</div>

<script>
    let eventSource = null;

    function connectSse() {
        const userId = document.getElementById("userId").value;
        if (!userId) {
            alert("请输入用户 ID");
            return;
        }

        closeSse();

        const url = `/api/sse/connect/${encodeURIComponent(userId)}`;
        eventSource = new EventSource(url);

        eventSource.onopen = function () {
            setStatus("已连接");
            appendMessage("系统", "SSE 连接已打开");
        };

        eventSource.onmessage = function (event) {
            appendMessage("message", event.data);
        };

        eventSource.addEventListener("connect", function (event) {
            appendMessage("connect", event.data);
        });

        eventSource.addEventListener("notice", function (event) {
            appendMessage("notice", event.data);
        });

        eventSource.addEventListener("heartbeat", function (event) {
            appendMessage("heartbeat", event.data);
        });

        eventSource.onerror = function () {
            setStatus("连接异常，浏览器将尝试自动重连");
            appendMessage("error", "连接异常或服务端已关闭连接");
        };
    }

    function closeSse() {
        if (eventSource !== null) {
            eventSource.close();
            eventSource = null;
            setStatus("已关闭");
            appendMessage("系统", "SSE 连接已关闭");
        }
    }

    function setStatus(status) {
        document.getElementById("status").innerText = status;
    }

    function appendMessage(type, data) {
        const messageList = document.getElementById("messageList");
        const item = document.createElement("li");
        item.className = "log";
        item.innerText = `[${new Date().toLocaleTimeString()}] ${type}：${data}`;
        messageList.appendChild(item);
    }

    window.addEventListener("beforeunload", function () {
        closeSse();
    });
</script>
</body>
</html>
```

启动 Spring Boot 项目后，可以在浏览器中访问：

```text
http://localhost:8080/sse-demo.html
```

页面打开后，输入 `user-1001` 并点击“建立连接”，再通过后端推送接口发送消息即可看到实时输出。

### 消息监听方式

`EventSource` 支持两类消息监听方式：默认消息监听和命名事件监听。MDN 对 `EventSource` 的说明中提到，如果服务端消息带有 `event` 字段，会触发同名事件；如果没有 `event` 字段，则触发通用的 `message` 事件。([MDN Web Docs](https://developer.mozilla.org/en-US/docs/Web/API/EventSource?utm_source=chatgpt.com))

默认消息监听适合接收没有指定事件名称的消息：

```javascript
eventSource.onmessage = function (event) {
    console.log("收到默认消息：", event.data);
};
```

命名事件监听适合区分不同业务消息类型：

```javascript
eventSource.addEventListener("notice", function (event) {
    console.log("收到通知消息：", event.data);
});

eventSource.addEventListener("progress", function (event) {
    console.log("收到进度消息：", event.data);
});

eventSource.addEventListener("heartbeat", function (event) {
    console.log("收到心跳消息：", event.data);
});
```

服务端发送命名事件时，需要设置 `name`：

```java
emitter.send(SseEmitter.event()
        .id(IdUtil.fastSimpleUUID())
        .name("notice")
        .data("你有一条新的系统通知")
        .reconnectTime(3000L));
```

事件名称建议按业务类型设计，例如：

| 事件名称    | 说明         |
| ----------- | ------------ |
| `connect`   | 连接建立成功 |
| `message`   | 默认业务消息 |
| `notice`    | 系统通知     |
| `progress`  | 任务进度     |
| `log`       | 实时日志     |
| `heartbeat` | 心跳消息     |
| `error`     | 业务异常提示 |

如果前端使用 `addEventListener("notice", callback)`，服务端就需要发送 `.name("notice")`。如果服务端不设置事件名称，前端应使用 `onmessage` 或监听 `message` 事件。

### 异常重连处理

`EventSource` 默认具备自动重连能力。当前连接发生错误时，会触发 `onerror` 回调；如果连接没有被主动 `close()`，浏览器会尝试重新建立连接。`EventSource.readyState` 可用于判断当前连接状态，常见状态包括 `CONNECTING`、`OPEN` 和 `CLOSED`。([MDN Web Docs](https://developer.mozilla.org/en-US/docs/Web/API/EventSource?utm_source=chatgpt.com))

前端可以通过 `readyState` 做更明确的状态提示：

```javascript
eventSource.onerror = function () {
    if (eventSource.readyState === EventSource.CONNECTING) {
        setStatus("连接中断，正在自动重连");
        appendMessage("error", "连接中断，浏览器正在尝试自动重连");
        return;
    }

    if (eventSource.readyState === EventSource.CLOSED) {
        setStatus("连接已关闭");
        appendMessage("error", "连接已关闭，不再自动重连");
        return;
    }

    setStatus("连接异常");
    appendMessage("error", "SSE 连接发生异常");
};
```

服务端可以通过 `reconnectTime` 给浏览器建议重连间隔：

```java
emitter.send(SseEmitter.event()
        .id(IdUtil.fastSimpleUUID())
        .name("notice")
        .data("服务端推送消息")
        .reconnectTime(3000L));
```

异常重连处理建议如下：

| 场景           | 建议处理                                       |
| -------------- | ---------------------------------------------- |
| 网络短暂中断   | 依赖浏览器自动重连                             |
| 服务端重启     | 前端提示“正在重连”，等待连接恢复               |
| 用户主动退出   | 调用 `eventSource.close()`，不再重连           |
| 页面刷新或关闭 | 在 `beforeunload` 中关闭连接                   |
| 服务端连接超时 | 前端自动重连，后端重新注册连接                 |
| 权限失效       | 服务端返回错误状态，前端跳转登录或提示重新认证 |

如果接口需要登录态，并且前后端同域部署，`EventSource` 会自动携带同域 Cookie。如果是跨域请求，并且需要携带 Cookie，可以创建连接时设置 `withCredentials: true`；MDN 的 `EventSource` 文档也说明其构造函数支持带凭据模式。([MDN Web Docs](https://developer.mozilla.org/en-US/docs/Web/API/EventSource?utm_source=chatgpt.com))

```javascript
const eventSource = new EventSource("https://api.example.com/api/sse/connect/user-1001", {
    withCredentials: true
});
```

跨域携带 Cookie 时，后端还需要正确配置 CORS，允许指定来源、允许凭据，并避免使用 `*` 作为允许来源。生产环境中通常建议通过同域网关转发 SSE 接口，减少跨域和 Cookie 策略带来的复杂度。

## 多用户连接管理

本节在前面单用户连接示例的基础上，将 SSE 连接管理升级为“多用户、多连接”模型。实际项目中，同一个用户可能会同时打开多个浏览器标签页，或者在 PC、移动端、管理后台等多个客户端同时在线，因此服务端不能简单地只保存一个 `userId -> SseEmitter` 映射。

Spring MVC 的 `SseEmitter` 支持创建带超时时间的 SSE 连接，也支持 `send(SseEmitter.SseEventBuilder)` 发送带事件名、事件 ID 和数据体的事件；连接完成、超时、异常等生命周期可以通过继承自 `ResponseBodyEmitter` 的 `onCompletion`、`onTimeout`、`onError` 等回调处理。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html?utm_source=chatgpt.com)) 本节内容延续你上传的大纲结构。

### 用户连接注册

用户连接注册用于在客户端建立 SSE 长连接后，将当前连接保存到服务端内存中。推荐使用“用户 ID + 连接 ID”的二级结构管理连接，这样既可以向指定用户推送，也可以支持同一用户多个页面同时接收消息。

推荐连接缓存结构如下：

| 层级   | Key            | Value                 | 说明                         |
| ------ | -------------- | --------------------- | ---------------------------- |
| 第一层 | `userId`       | 用户连接集合          | 标识某个用户                 |
| 第二层 | `connectionId` | `SseEmitter` 包装对象 | 标识该用户的某一个客户端连接 |

下面代码替换前面单连接版本的 `SseEmitterManager`，支持多用户、多连接、最大连接数限制和连接生命周期清理。

文件位置：`src/main/java/io/github/atengk/sse/manager/SseEmitterManager.java`

```java
package io.github.atengk.sse.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.sse.config.SseProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterManager {

    private final SseProperties sseProperties;

    /**
     * 用户连接缓存，第一层 key 为用户 ID，第二层 key 为连接 ID
     */
    private final Map<String, Map<String, SseConnection>> userConnectionMap = new ConcurrentHashMap<>();

    /**
     * 创建并注册 SSE 连接
     *
     * @param userId 用户 ID
     * @param clientId 客户端 ID
     * @return SSE 连接对象
     */
    public SseEmitter register(String userId, String clientId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        String connectionId = StrUtil.blankToDefault(clientId, IdUtil.fastSimpleUUID());
        clearExceededConnection(userId);

        SseEmitter emitter = new SseEmitter(sseProperties.getTimeout());
        SseConnection connection = new SseConnection(connectionId, userId, emitter, System.currentTimeMillis());

        userConnectionMap
                .computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
                .put(connectionId, connection);

        emitter.onCompletion(() -> {
            log.info("SSE 连接完成，用户ID：{}，连接ID：{}", userId, connectionId);
            remove(userId, connectionId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时，用户ID：{}，连接ID：{}", userId, connectionId);
            remove(userId, connectionId);
        });

        emitter.onError(throwable -> {
            log.warn("SSE 连接异常，用户ID：{}，连接ID：{}，异常信息：{}",
                    userId, connectionId, throwable.getMessage());
            remove(userId, connectionId);
        });

        log.info("SSE 连接注册成功，用户ID：{}，连接ID：{}，连接时间：{}",
                userId, connectionId, DateUtil.now());
        return emitter;
    }

    /**
     * 获取指定用户的全部连接
     *
     * @param userId 用户 ID
     * @return 用户连接集合
     */
    public Map<String, SseConnection> getUserConnections(String userId) {
        if (StrUtil.isBlank(userId)) {
            return new ConcurrentHashMap<>();
        }
        return userConnectionMap.getOrDefault(userId, new ConcurrentHashMap<>());
    }

    /**
     * 获取全部用户连接
     *
     * @return 全部用户连接
     */
    public Map<String, Map<String, SseConnection>> getAllConnections() {
        return userConnectionMap;
    }

    /**
     * 移除指定连接
     *
     * @param userId 用户 ID
     * @param connectionId 连接 ID
     */
    public void remove(String userId, String connectionId) {
        if (StrUtil.hasBlank(userId, connectionId)) {
            return;
        }

        Map<String, SseConnection> connectionMap = userConnectionMap.get(userId);
        if (CollUtil.isEmpty(connectionMap)) {
            return;
        }

        connectionMap.remove(connectionId);

        if (CollUtil.isEmpty(connectionMap)) {
            userConnectionMap.remove(userId);
        }

        log.info("SSE 连接已移除，用户ID：{}，连接ID：{}", userId, connectionId);
    }

    /**
     * 关闭指定用户的全部连接
     *
     * @param userId 用户 ID
     */
    public void closeUser(String userId) {
        Map<String, SseConnection> connectionMap = getUserConnections(userId);
        if (CollUtil.isEmpty(connectionMap)) {
            return;
        }

        connectionMap.forEach((connectionId, connection) -> closeConnection(userId, connectionId, connection));
        userConnectionMap.remove(userId);

        log.info("SSE 用户全部连接已关闭，用户ID：{}", userId);
    }

    /**
     * 获取当前总连接数
     *
     * @return 当前总连接数
     */
    public int countTotalConnection() {
        return userConnectionMap.values()
                .stream()
                .mapToInt(Map::size)
                .sum();
    }

    /**
     * 获取指定用户连接数
     *
     * @param userId 用户 ID
     * @return 用户连接数
     */
    public int countUserConnection(String userId) {
        return getUserConnections(userId).size();
    }

    /**
     * 清理超过限制的用户连接
     *
     * @param userId 用户 ID
     */
    private void clearExceededConnection(String userId) {
        Map<String, SseConnection> connectionMap = getUserConnections(userId);
        if (CollUtil.isEmpty(connectionMap)) {
            return;
        }

        Integer maxConnectionPerUser = sseProperties.getMaxConnectionPerUser();
        if (maxConnectionPerUser == null || maxConnectionPerUser <= 0) {
            return;
        }

        if (connectionMap.size() < maxConnectionPerUser) {
            return;
        }

        Optional<SseConnection> oldestConnection = connectionMap.values()
                .stream()
                .min(Comparator.comparingLong(SseConnection::getCreateTime));

        oldestConnection.ifPresent(connection -> {
            log.warn("SSE 用户连接数超过限制，关闭最早连接，用户ID：{}，连接ID：{}",
                    userId, connection.getConnectionId());
            closeConnection(userId, connection.getConnectionId(), connection);
        });
    }

    /**
     * 关闭指定连接
     *
     * @param userId 用户 ID
     * @param connectionId 连接 ID
     * @param connection 连接对象
     */
    private void closeConnection(String userId, String connectionId, SseConnection connection) {
        try {
            connection.getEmitter().complete();
            log.info("SSE 连接已关闭，用户ID：{}，连接ID：{}", userId, connectionId);
        } catch (Exception e) {
            log.warn("SSE 连接关闭异常，用户ID：{}，连接ID：{}，异常信息：{}",
                    userId, connectionId, e.getMessage());
        } finally {
            remove(userId, connectionId);
        }
    }

    /**
     * SSE 连接对象
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    @AllArgsConstructor
    public static class SseConnection {

        /**
         * 连接 ID
         */
        private String connectionId;

        /**
         * 用户 ID
         */
        private String userId;

        /**
         * SSE 连接对象
         */
        private SseEmitter emitter;

        /**
         * 创建时间
         */
        private Long createTime;
    }
}
```

这版实现有几个关键点：

| 设计点                 | 说明                                               |
| ---------------------- | -------------------------------------------------- |
| `ConcurrentHashMap`    | 支持并发注册、删除、推送连接                       |
| `connectionId`         | 区分同一个用户的不同页面或不同客户端               |
| `maxConnectionPerUser` | 限制单用户最大连接数，避免页面反复刷新导致连接堆积 |
| `onCompletion`         | 客户端正常断开或响应完成后释放资源                 |
| `onTimeout`            | 连接超时后释放资源                                 |
| `onError`              | 网络异常、写入失败等场景释放资源                   |

连接接口也需要调整为支持客户端 ID。客户端 ID 可以由前端生成，也可以不传；不传时后端自动生成。

文件位置：`src/main/java/io/github/atengk/sse/controller/SseController.java`

```java
@GetMapping(value = "/connect/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter connect(@PathVariable String userId,
                          @RequestParam(required = false) String clientId) {
    return sseService.connect(userId, clientId);
}
```

对应的 Service 接口也需要调整。

文件位置：`src/main/java/io/github/atengk/sse/service/SseService.java`

```java
/**
 * 建立 SSE 连接
 *
 * @param userId 用户 ID
 * @param clientId 客户端 ID
 * @return SSE 连接对象
 */
SseEmitter connect(String userId, String clientId);
```

Service 实现类中建立连接的方法修改如下：

文件位置：`src/main/java/io/github/atengk/sse/service/impl/SseServiceImpl.java`

```java
@Override
public SseEmitter connect(String userId, String clientId) {
    SseEmitter emitter = sseEmitterManager.register(userId, clientId);

    try {
        emitter.send(SseEmitter.event()
                .id(IdUtil.fastSimpleUUID())
                .name("connect")
                .data("SSE 连接成功")
                .reconnectTime(3000L));

        log.info("SSE 首次连接消息发送成功，用户ID：{}，客户端ID：{}", userId, clientId);
    } catch (IOException e) {
        log.warn("SSE 首次连接消息发送失败，用户ID：{}，客户端ID：{}，异常信息：{}",
                userId, clientId, e.getMessage());
    }

    return emitter;
}
```

浏览器连接示例：

```text
http://localhost:8080/api/sse/connect/user-1001?clientId=browser-tab-001
```

如果一个用户打开多个页面，可以分别传入不同 `clientId`：

```text
http://localhost:8080/api/sse/connect/user-1001?clientId=browser-tab-001
http://localhost:8080/api/sse/connect/user-1001?clientId=browser-tab-002
```

### 指定用户推送

指定用户推送用于向某个用户的全部在线连接发送消息。例如用户同时打开了两个页面，后端向该用户推送一条通知时，两个页面都应该收到消息。

下面的实现会获取指定用户下的全部连接，并逐个发送事件。如果某个连接发送失败，则移除该连接，不影响其他连接。

文件位置：`src/main/java/io/github/atengk/sse/service/impl/SseServiceImpl.java`

```java
@Override
public boolean sendToUser(SseMessageDTO messageDTO) {
    String userId = messageDTO.getUserId();
    Map<String, SseEmitterManager.SseConnection> connectionMap = sseEmitterManager.getUserConnections(userId);

    if (CollUtil.isEmpty(connectionMap)) {
        log.warn("SSE 指定用户推送失败，用户连接不存在，用户ID：{}", userId);
        return false;
    }

    String eventName = StrUtil.blankToDefault(messageDTO.getEventName(), DEFAULT_EVENT_NAME);
    int successCount = 0;

    for (Map.Entry<String, SseEmitterManager.SseConnection> entry : connectionMap.entrySet()) {
        String connectionId = entry.getKey();
        SseEmitter emitter = entry.getValue().getEmitter();

        try {
            emitter.send(SseEmitter.event()
                    .id(IdUtil.fastSimpleUUID())
                    .name(eventName)
                    .data(messageDTO.getContent())
                    .reconnectTime(3000L));

            successCount++;
            log.info("SSE 指定用户推送成功，用户ID：{}，连接ID：{}，事件名称：{}",
                    userId, connectionId, eventName);
        } catch (IOException e) {
            log.warn("SSE 指定用户推送失败，用户ID：{}，连接ID：{}，异常信息：{}",
                    userId, connectionId, e.getMessage());
            sseEmitterManager.remove(userId, connectionId);
        }
    }

    return successCount > 0;
}
```

该方法需要补充以下 imports：

```java
import cn.hutool.core.collection.CollUtil;
import java.util.Map;
```

指定用户推送接口保持不变：

```java
@PostMapping("/push")
public Map<String, Object> push(@Valid @RequestBody SseMessageDTO messageDTO) {
    boolean success = sseService.sendToUser(messageDTO);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("success", success);
    result.put("message", success ? "消息推送成功" : "用户连接不存在或推送失败");
    return result;
}
```

调用示例：

```bash
curl -X POST http://localhost:8080/api/sse/push \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-1001",
    "eventName": "notice",
    "content": "你有一条新的系统通知"
  }'
```

如果用户 `user-1001` 当前有多个在线连接，所有连接都会收到 `notice` 事件。前端可以这样监听：

```javascript
eventSource.addEventListener("notice", function (event) {
    console.log("收到通知消息：", event.data);
});
```

### 广播消息推送

广播消息推送用于向当前所有在线用户的所有连接发送消息。常见场景包括系统维护通知、全局公告、服务状态提醒、数据看板刷新通知等。

先新增广播消息 DTO。

文件位置：`src/main/java/io/github/atengk/sse/model/dto/SseBroadcastMessageDTO.java`

```java
package io.github.atengk.sse.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSE 广播消息请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class SseBroadcastMessageDTO {

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;
}
```

Service 接口新增广播方法。

文件位置：`src/main/java/io/github/atengk/sse/service/SseService.java`

```java
/**
 * 广播消息
 *
 * @param messageDTO 广播消息参数
 * @return 推送成功连接数
 */
int broadcast(SseBroadcastMessageDTO messageDTO);
```

Service 实现类新增广播逻辑。

文件位置：`src/main/java/io/github/atengk/sse/service/impl/SseServiceImpl.java`

```java
@Override
public int broadcast(SseBroadcastMessageDTO messageDTO) {
    Map<String, Map<String, SseEmitterManager.SseConnection>> allConnections =
            sseEmitterManager.getAllConnections();

    if (CollUtil.isEmpty(allConnections)) {
        log.warn("SSE 广播消息推送跳过，当前没有在线连接");
        return 0;
    }

    String eventName = StrUtil.blankToDefault(messageDTO.getEventName(), "broadcast");
    int successCount = 0;

    for (Map.Entry<String, Map<String, SseEmitterManager.SseConnection>> userEntry : allConnections.entrySet()) {
        String userId = userEntry.getKey();
        Map<String, SseEmitterManager.SseConnection> connectionMap = userEntry.getValue();

        if (CollUtil.isEmpty(connectionMap)) {
            continue;
        }

        for (Map.Entry<String, SseEmitterManager.SseConnection> connectionEntry : connectionMap.entrySet()) {
            String connectionId = connectionEntry.getKey();
            SseEmitter emitter = connectionEntry.getValue().getEmitter();

            try {
                emitter.send(SseEmitter.event()
                        .id(IdUtil.fastSimpleUUID())
                        .name(eventName)
                        .data(messageDTO.getContent())
                        .reconnectTime(3000L));

                successCount++;
                log.info("SSE 广播消息推送成功，用户ID：{}，连接ID：{}，事件名称：{}",
                        userId, connectionId, eventName);
            } catch (IOException e) {
                log.warn("SSE 广播消息推送失败，用户ID：{}，连接ID：{}，异常信息：{}",
                        userId, connectionId, e.getMessage());
                sseEmitterManager.remove(userId, connectionId);
            }
        }
    }

    log.info("SSE 广播消息推送完成，成功连接数：{}", successCount);
    return successCount;
}
```

该方法需要补充以下 import：

```java
import io.github.atengk.sse.model.dto.SseBroadcastMessageDTO;
```

Controller 新增广播接口。

文件位置：`src/main/java/io/github/atengk/sse/controller/SseController.java`

```java
@PostMapping("/broadcast")
public Map<String, Object> broadcast(@Valid @RequestBody SseBroadcastMessageDTO messageDTO) {
    int successCount = sseService.broadcast(messageDTO);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("success", successCount > 0);
    result.put("successCount", successCount);
    result.put("message", successCount > 0 ? "广播消息推送成功" : "当前没有可推送连接");
    return result;
}
```

该接口需要补充以下 import：

```java
import io.github.atengk.sse.model.dto.SseBroadcastMessageDTO;
```

广播调用示例：

```bash
curl -X POST http://localhost:8080/api/sse/broadcast \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "broadcast",
    "content": "系统将在 23:00 进行维护，请提前保存数据"
  }'
```

前端监听广播事件：

```javascript
eventSource.addEventListener("broadcast", function (event) {
    console.log("收到广播消息：", event.data);
});
```

广播推送需要注意控制频率。如果广播消息非常频繁，或者在线连接数很多，应考虑异步线程池、消息队列、分批推送、限流和背压策略，避免单次广播阻塞业务线程。

## 异常与超时处理

SSE 的稳定性主要取决于连接生命周期管理。服务端必须正确处理连接超时、客户端断开和推送失败，否则容易出现无效连接残留、内存占用增长、重复推送失败等问题。

`SseEmitter` 支持构造时指定超时时间；如果不指定，则使用 MVC 配置或底层容器默认策略。它还继承了 `complete`、`completeWithError`、`onCompletion`、`onError`、`onTimeout` 等生命周期相关方法。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html?utm_source=chatgpt.com))

### 连接超时处理

连接超时是指 SSE 长连接超过服务端设定的超时时间后被关闭。开发环境可以将超时时间设置得较长，便于调试；生产环境则应结合网关、Nginx、浏览器和服务端资源情况综合设置。

配置示例：

文件位置：`src/main/resources/application.yml`

```yaml
sse:
  # SSE 连接超时时间，单位毫秒；0 表示不主动超时
  timeout: 0

  # 心跳间隔，单位秒
  heartbeat-interval: 30

  # 单个用户允许保留的最大连接数
  max-connection-per-user: 3
```

如果希望连接 30 分钟后超时，可以设置为：

```yaml
sse:
  # 30 分钟，单位毫秒
  timeout: 1800000
```

超时处理逻辑已经在 `SseEmitterManager.register` 中注册：

```java
emitter.onTimeout(() -> {
    log.warn("SSE 连接超时，用户ID：{}，连接ID：{}", userId, connectionId);
    remove(userId, connectionId);
});
```

超时发生后，服务端应将连接从缓存中移除。客户端如果没有主动调用 `eventSource.close()`，浏览器通常会尝试重新连接；服务端会重新执行连接注册逻辑，生成新的 `SseEmitter`。

建议增加心跳推送，降低中间代理因为连接长时间没有数据而关闭连接的概率。下面是一个简单的心跳任务示例。

文件位置：`src/main/java/io/github/atengk/sse/config/ScheduleConfig.java`

```java
package io.github.atengk.sse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
```

文件位置：`src/main/java/io/github/atengk/sse/service/SseHeartbeatService.java`

```java
package io.github.atengk.sse.service;

/**
 * SSE 心跳服务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface SseHeartbeatService {

    /**
     * 发送心跳消息
     */
    void sendHeartbeat();
}
```

文件位置：`src/main/java/io/github/atengk/sse/service/impl/SseHeartbeatServiceImpl.java`

```java
package io.github.atengk.sse.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.sse.manager.SseEmitterManager;
import io.github.atengk.sse.service.SseHeartbeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * SSE 心跳服务实现类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseHeartbeatServiceImpl implements SseHeartbeatService {

    private final SseEmitterManager sseEmitterManager;

    /**
     * 定时发送心跳消息
     */
    @Override
    @Scheduled(fixedDelayString = "${sse.heartbeat-interval:30}000")
    public void sendHeartbeat() {
        Map<String, Map<String, SseEmitterManager.SseConnection>> allConnections =
                sseEmitterManager.getAllConnections();

        if (CollUtil.isEmpty(allConnections)) {
            return;
        }

        for (Map.Entry<String, Map<String, SseEmitterManager.SseConnection>> userEntry : allConnections.entrySet()) {
            String userId = userEntry.getKey();
            Map<String, SseEmitterManager.SseConnection> connectionMap = userEntry.getValue();

            if (CollUtil.isEmpty(connectionMap)) {
                continue;
            }

            for (Map.Entry<String, SseEmitterManager.SseConnection> connectionEntry : connectionMap.entrySet()) {
                String connectionId = connectionEntry.getKey();
                SseEmitter emitter = connectionEntry.getValue().getEmitter();

                try {
                    emitter.send(SseEmitter.event()
                            .id(IdUtil.fastSimpleUUID())
                            .name("heartbeat")
                            .data(DateUtil.now())
                            .reconnectTime(3000L));

                    log.debug("SSE 心跳发送成功，用户ID：{}，连接ID：{}", userId, connectionId);
                } catch (IOException e) {
                    log.warn("SSE 心跳发送失败，用户ID：{}，连接ID：{}，异常信息：{}",
                            userId, connectionId, e.getMessage());
                    sseEmitterManager.remove(userId, connectionId);
                }
            }
        }
    }
}
```

这段代码通过定时任务向全部在线连接发送 `heartbeat` 事件。如果心跳发送失败，说明连接大概率已经不可用，此时直接移除该连接。

### 客户端断开处理

客户端断开通常发生在以下场景：

| 场景           | 说明                           |
| -------------- | ------------------------------ |
| 浏览器页面关闭 | 用户关闭当前页面或标签页       |
| 浏览器刷新     | 旧连接断开，新页面重新建立连接 |
| 网络中断       | 客户端网络不稳定               |
| 前端主动关闭   | 调用 `eventSource.close()`     |
| 登录失效或退出 | 前端主动关闭连接并清理状态     |

前端应在页面关闭前主动关闭 SSE 连接：

```javascript
window.addEventListener("beforeunload", function () {
    if (eventSource !== null) {
        eventSource.close();
        eventSource = null;
    }
});
```

如果是 Vue 3 页面，建议在组件卸载时关闭连接：

```javascript
import { onBeforeUnmount } from "vue";

let eventSource = null;

onBeforeUnmount(() => {
    if (eventSource !== null) {
        eventSource.close();
        eventSource = null;
    }
});
```

服务端不应该依赖客户端一定会主动通知断开。即使客户端没有发送任何关闭请求，服务端仍然可以通过 `onCompletion`、`onError` 和下一次推送失败来清理无效连接。

服务端生命周期回调如下：

```java
emitter.onCompletion(() -> {
    log.info("SSE 连接完成，用户ID：{}，连接ID：{}", userId, connectionId);
    remove(userId, connectionId);
});

emitter.onError(throwable -> {
    log.warn("SSE 连接异常，用户ID：{}，连接ID：{}，异常信息：{}",
            userId, connectionId, throwable.getMessage());
    remove(userId, connectionId);
});
```

如果业务需要用户退出登录时主动关闭全部 SSE 连接，可以调用：

```java
sseEmitterManager.closeUser(userId);
```

也可以暴露一个关闭接口：

```java
@DeleteMapping("/close/{userId}")
public Map<String, Object> close(@PathVariable String userId) {
    sseService.close(userId);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("success", true);
    result.put("message", "连接关闭成功");
    return result;
}
```

客户端退出登录时调用：

```bash
curl -X DELETE http://localhost:8080/api/sse/close/user-1001
```

客户端断开处理的关键原则是：前端主动关闭是优化手段，后端回调清理和失败清理才是可靠兜底。

### 推送失败处理

推送失败通常发生在连接已经断开但服务端缓存还未及时清理的场景。此时调用 `emitter.send(...)` 会抛出异常，服务端应该捕获异常并移除对应连接。

推送失败处理示例：

```java
try {
    emitter.send(SseEmitter.event()
            .id(IdUtil.fastSimpleUUID())
            .name(eventName)
            .data(content)
            .reconnectTime(3000L));

    log.info("SSE 消息推送成功，用户ID：{}，连接ID：{}，事件名称：{}",
            userId, connectionId, eventName);
} catch (IOException e) {
    log.warn("SSE 消息推送失败，用户ID：{}，连接ID：{}，异常信息：{}",
            userId, connectionId, e.getMessage());
    sseEmitterManager.remove(userId, connectionId);
}
```

推送失败处理建议如下：

| 问题                   | 处理方式                                       |
| ---------------------- | ---------------------------------------------- |
| 单个连接发送失败       | 移除该连接，不影响其他连接                     |
| 指定用户所有连接都失败 | 返回推送失败，业务可记录日志                   |
| 广播时部分连接失败     | 只移除失败连接，继续推送其他连接               |
| 频繁失败               | 检查网关、Nginx、服务端超时和前端重连逻辑      |
| 业务消息不能丢         | 不要只依赖 SSE，应将消息持久化到数据库或消息表 |
| 集群部署               | 需要 Redis、MQ 或网关粘性会话支持跨节点推送    |

如果消息非常重要，例如审批通知、支付结果、系统告警，不建议只依赖 SSE。SSE 应作为实时到达通道，消息本身应落库，前端收到事件后再通过普通 HTTP 接口拉取最新数据。这样即使 SSE 连接中断，也不会造成业务消息丢失。

完整的异常处理流程如下：

```text
服务端准备推送消息
        ↓
根据 userId 查询连接集合
        ↓
逐个连接发送 SSE 事件
        ↓
发送成功：记录成功日志
        ↓
发送失败：捕获 IOException
        ↓
移除失败连接
        ↓
继续处理其他连接
        ↓
返回最终推送结果
```

最终建议保留三层清理机制：

| 清理机制         | 作用                     |
| ---------------- | ------------------------ |
| 生命周期回调清理 | 处理正常完成、超时、异常 |
| 推送失败清理     | 处理缓存中残留的失效连接 |
| 心跳失败清理     | 定期发现不可用连接并移除 |

通过这三层机制，可以避免大多数 SSE 无效连接残留问题，使多用户推送、指定用户推送和广播推送在生产环境中更稳定。

## 接口测试

本节用于验证 SSE 服务端连接、消息推送、异常断开和前端接收是否正常。SSE 接口和普通 JSON 接口不同，它会保持 HTTP 连接持续打开，因此测试时需要关注响应头、连接状态、消息是否实时输出，以及连接断开后的资源清理情况。本节继续补齐你提供的大纲中的“接口测试”和“生产注意事项”部分。

### 浏览器测试

浏览器测试适合验证前端 `EventSource` 是否能正常建立连接、监听事件、接收消息和处理异常。`EventSource` 提供 `onopen`、`onmessage`、`onerror` 等事件处理能力，并且可以通过 `close()` 主动关闭连接。([devdoc.net](https://devdoc.net/web/developer.mozilla.org/en-US/docs/Web/API/EventSource/index.html?utm_source=chatgpt.com))

测试前需要先启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

命令说明：

| 命令                  | 说明                                                         |
| --------------------- | ------------------------------------------------------------ |
| `mvn spring-boot:run` | 使用 Maven 启动当前 Spring Boot 项目                         |
| 默认端口              | 根据 `application.yml` 中的 `server.port` 决定，本文示例为 `8080` |

浏览器访问测试页面：

```text
http://localhost:8080/sse-demo.html
```

测试步骤如下：

| 步骤 | 操作                          | 预期结果                            |
| ---- | ----------------------------- | ----------------------------------- |
| 1    | 打开 `sse-demo.html`          | 页面正常加载                        |
| 2    | 输入用户 ID，例如 `user-1001` | 用户 ID 输入框有值                  |
| 3    | 点击“建立连接”                | 页面状态变为“已连接”                |
| 4    | 后端发送 `connect` 事件       | 页面消息列表显示连接成功            |
| 5    | 调用指定用户推送接口          | 页面收到 `notice` 或 `message` 事件 |
| 6    | 调用广播接口                  | 所有在线页面收到 `broadcast` 事件   |
| 7    | 点击“关闭连接”                | 页面状态变为“已关闭”                |
| 8    | 刷新页面或关闭标签页          | 后端日志输出连接完成或连接移除日志  |

浏览器开发者工具中可以重点检查以下内容：

| 检查项       | 说明                                                      |
| ------------ | --------------------------------------------------------- |
| Network 请求 | `/api/sse/connect/{userId}` 请求应保持 Pending 或持续打开 |
| 响应类型     | 响应头应包含 `Content-Type: text/event-stream`            |
| 消息输出     | 后端推送后，Response 或 EventStream 中应出现事件内容      |
| Console 日志 | 前端应输出连接成功、消息接收、异常重连等日志              |
| 服务端日志   | 应输出连接注册、推送成功、连接移除等日志                  |

SSE 响应内容示例：

```text
id: 4d84dc9c8c2246c68f28d9f3d51e6611
event: notice
retry: 3000
data: 你有一条新的系统通知
```

字段说明如下：

| 字段    | 说明                                 |
| ------- | ------------------------------------ |
| `id`    | 当前事件 ID                          |
| `event` | 当前事件名称                         |
| `retry` | 浏览器断线后的建议重连间隔，单位毫秒 |
| `data`  | 服务端推送的数据内容                 |

如果页面无法收到消息，优先排查以下问题：

| 问题                 | 排查方式                                                     |
| -------------------- | ------------------------------------------------------------ |
| 请求立即结束         | 检查 Controller 是否直接返回 `SseEmitter`                    |
| 响应不是事件流       | 检查接口是否声明 `produces = MediaType.TEXT_EVENT_STREAM_VALUE` |
| 前端监听不到命名事件 | 检查服务端 `.name("notice")` 和前端 `addEventListener("notice", ...)` 是否一致 |
| 页面一直重连         | 检查服务端日志是否有异常、超时或网关断开                     |
| 消息被统一响应包装   | 检查全局 `ResponseBodyAdvice` 是否排除了 `SseEmitter`        |

如果项目存在统一响应包装器，建议对 `SseEmitter` 做排除处理，避免 SSE 长连接被包装为普通 JSON。

下面是一个统一响应包装排除 SSE 的示例。

文件位置：`src/main/java/io/github/atengk/sse/common/result/GlobalResponseAdvice.java`

```java
package io.github.atengk.sse.common.result;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 全局响应增强处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 判断是否需要增强响应
     *
     * @param returnType 返回类型
     * @param converterType 消息转换器类型
     * @return 是否支持增强
     */
    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> parameterType = returnType.getParameterType();
        boolean support = !SseEmitter.class.isAssignableFrom(parameterType);

        if (!support) {
            log.info("SSE 响应跳过统一包装，返回类型：{}", parameterType.getName());
        }

        return support;
    }

    /**
     * 响应写出前处理
     *
     * @param body 响应体
     * @param returnType 返回类型
     * @param selectedContentType 响应类型
     * @param selectedConverterType 消息转换器类型
     * @param request 请求对象
     * @param response 响应对象
     * @return 处理后的响应体
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        return body;
    }
}
```

这段代码用于在统一响应处理时识别 `SseEmitter`，并跳过 JSON 包装。实际项目中的 `beforeBodyWrite` 可以继续保留原有统一响应逻辑。

### curl 测试

`curl` 测试适合验证服务端接口是否可用，不依赖前端页面。SSE 是持续事件流，测试连接接口时应使用 `-N` 参数关闭 curl 的缓冲，否则消息可能不会实时显示。

先打开一个终端，建立 SSE 连接：

```bash
curl -N http://localhost:8080/api/sse/connect/user-1001
```

命令说明：

| 参数                         | 说明                                |
| ---------------------------- | ----------------------------------- |
| `curl`                       | 发起 HTTP 请求                      |
| `-N`                         | 禁用输出缓冲，便于实时查看 SSE 消息 |
| `/api/sse/connect/user-1001` | 建立用户 `user-1001` 的 SSE 连接    |

建立连接后，终端中通常会看到类似输出：

```text
id: 6c6a89fd3d24445a8b76d2d0d69c1e3f
event: connect
retry: 3000
data: SSE 连接成功
```

再打开第二个终端，调用指定用户推送接口：

```bash
curl -X POST http://localhost:8080/api/sse/push \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-1001",
    "eventName": "notice",
    "content": "你有一条新的系统通知"
  }'
```

推送成功后，第一个终端应实时输出：

```text
id: ebc1620ac9e743d5a19a13af55d76185
event: notice
retry: 3000
data: 你有一条新的系统通知
```

测试广播接口：

```bash
curl -X POST http://localhost:8080/api/sse/broadcast \
  -H "Content-Type: application/json" \
  -d '{
    "eventName": "broadcast",
    "content": "系统将在 23:00 进行维护，请提前保存数据"
  }'
```

测试关闭用户连接：

```bash
curl -X DELETE http://localhost:8080/api/sse/close/user-1001
```

返回示例：

```json
{
  "success": true,
  "message": "连接关闭成功"
}
```

可以使用多个终端模拟多连接：

```bash
curl -N "http://localhost:8080/api/sse/connect/user-1001?clientId=terminal-001"
curl -N "http://localhost:8080/api/sse/connect/user-1001?clientId=terminal-002"
```

然后调用指定用户推送接口，两个终端都应收到消息。这个测试用于验证“同一用户多连接推送”是否正常。

也可以模拟多个用户：

```bash
curl -N "http://localhost:8080/api/sse/connect/user-1001?clientId=terminal-001"
curl -N "http://localhost:8080/api/sse/connect/user-1002?clientId=terminal-001"
```

调用广播接口后，两个用户的连接都应收到 `broadcast` 事件。

常用测试命令汇总如下：

| 测试目标     | 命令                                                         |
| ------------ | ------------------------------------------------------------ |
| 建立连接     | `curl -N http://localhost:8080/api/sse/connect/user-1001`    |
| 指定用户推送 | `curl -X POST http://localhost:8080/api/sse/push ...`        |
| 广播推送     | `curl -X POST http://localhost:8080/api/sse/broadcast ...`   |
| 关闭连接     | `curl -X DELETE http://localhost:8080/api/sse/close/user-1001` |
| 多连接测试   | 多个终端分别执行 `curl -N`                                   |
| 多用户测试   | 使用不同 `userId` 建立连接                                   |

### 前后端联调验证

前后端联调需要验证“连接建立、事件监听、消息推送、异常重连、连接释放”完整链路。不要只验证接口能连通，还需要确认后端是否正确清理连接、前端是否正确处理重连、消息事件名称是否一致。

推荐联调流程如下：

```text
前端页面初始化
        ↓
生成 clientId
        ↓
通过 EventSource 建立 SSE 连接
        ↓
后端注册 userId + clientId 连接
        ↓
后端发送 connect 事件
        ↓
前端监听 connect 事件并更新连接状态
        ↓
业务接口触发消息推送
        ↓
前端监听 notice / progress / broadcast 事件
        ↓
页面关闭或用户退出
        ↓
前端 close 连接，后端释放资源
```

前端建议生成一个稳定的 `clientId`，用于区分同一用户的不同浏览器标签页。

```javascript
function getClientId() {
    const storageKey = "sse_client_id";
    let clientId = sessionStorage.getItem(storageKey);

    if (!clientId) {
        clientId = `browser-${Date.now()}-${Math.random().toString(16).slice(2)}`;
        sessionStorage.setItem(storageKey, clientId);
    }

    return clientId;
}
```

建立连接示例：

```javascript
let eventSource = null;

function connectSse(userId) {
    const clientId = getClientId();
    const url = `/api/sse/connect/${encodeURIComponent(userId)}?clientId=${encodeURIComponent(clientId)}`;

    if (eventSource !== null) {
        eventSource.close();
    }

    eventSource = new EventSource(url);

    eventSource.onopen = function () {
        console.log("SSE 连接已打开");
    };

    eventSource.addEventListener("connect", function (event) {
        console.log("连接事件：", event.data);
    });

    eventSource.addEventListener("notice", function (event) {
        console.log("通知事件：", event.data);
    });

    eventSource.addEventListener("broadcast", function (event) {
        console.log("广播事件：", event.data);
    });

    eventSource.addEventListener("heartbeat", function (event) {
        console.debug("心跳事件：", event.data);
    });

    eventSource.onerror = function () {
        console.warn("SSE 连接异常，当前状态：", eventSource.readyState);
    };
}
```

关闭连接示例：

```javascript
function closeSse() {
    if (eventSource !== null) {
        eventSource.close();
        eventSource = null;
        console.log("SSE 连接已关闭");
    }
}

window.addEventListener("beforeunload", closeSse);
```

前后端联调检查表如下：

| 检查项             | 前端验证                     | 后端验证                 |
| ------------------ | ---------------------------- | ------------------------ |
| 用户 ID 是否一致   | 请求路径中的 `userId` 正确   | 日志打印正确用户 ID      |
| 客户端 ID 是否唯一 | 不同标签页 `clientId` 不同   | 后端注册多个连接         |
| 事件名称是否一致   | `addEventListener("notice")` | 服务端 `.name("notice")` |
| 连接是否保持       | Network 中请求持续打开       | 连接缓存数量增加         |
| 页面关闭是否清理   | 关闭页面后不再接收消息       | 后端连接移除日志         |
| 推送失败是否清理   | 无效页面不再接收消息         | 后端捕获异常并移除连接   |
| 心跳是否正常       | 前端周期性收到 `heartbeat`   | 后端定时发送心跳         |
| 跨域是否正常       | Console 无 CORS 错误         | 后端 CORS 配置正确       |

联调过程中，如果前端使用 Nginx 或网关代理后端接口，需要重点验证代理是否关闭响应缓冲。Nginx 默认会对代理响应进行缓冲，响应内容可能不会立即转发给客户端，这会影响 SSE 的实时推送效果。([docs.nginx.com](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy?utm_source=chatgpt.com))

## 生产注意事项

生产环境中的 SSE 重点不是接口能否运行，而是长连接能否稳定保持、代理是否及时转发事件、连接数量是否可控、服务重启或集群部署时消息是否可靠。SSE 是基于 HTTP 的长连接能力，后端、代理、网关、浏览器和业务消息可靠性都需要一起考虑。

### 网关与代理配置

SSE 在生产环境中经常经过 Nginx、Ingress、API Gateway 或 Spring Cloud Gateway。代理层如果启用了响应缓冲，或者读超时时间过短，可能导致客户端长时间收不到消息，或者连接被代理提前关闭。

Nginx 官方文档说明，Nginx 默认会缓冲被代理服务器的响应，`proxy_buffering` 控制是否启用代理响应缓冲，默认是开启状态。对于 SSE 这类事件流接口，应在对应 location 中关闭缓冲。([docs.nginx.com](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy?utm_source=chatgpt.com))

Nginx 推荐配置如下：

文件位置：`/etc/nginx/conf.d/sse.conf`

```nginx
server {
    listen 80;
    server_name example.com;

    # SSE 接口代理配置
    location /api/sse/ {
        proxy_pass http://springboot_sse_backend;

        # SSE 需要禁用代理缓冲，避免事件被缓存后批量返回
        proxy_buffering off;

        # 禁用 Nginx 代理缓存
        proxy_cache off;

        # 保持 HTTP 连接，适合长连接场景
        proxy_http_version 1.1;
        proxy_set_header Connection "";

        # 传递客户端真实信息
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # 连接后端超时时间
        proxy_connect_timeout 60s;

        # 读取后端响应超时时间，SSE 场景需要设置得比心跳间隔更长
        proxy_read_timeout 3600s;

        # 向后端发送请求超时时间
        proxy_send_timeout 3600s;
    }
}

upstream springboot_sse_backend {
    server 127.0.0.1:8080;
}
```

配置说明如下：

| 配置项                           | 说明                                     |
| -------------------------------- | ---------------------------------------- |
| `proxy_buffering off`            | 关闭代理响应缓冲，确保事件实时返回       |
| `proxy_cache off`                | 关闭代理缓存，避免事件流被缓存           |
| `proxy_http_version 1.1`         | 使用 HTTP/1.1 代理后端请求               |
| `proxy_set_header Connection ""` | 避免错误设置连接头                       |
| `proxy_read_timeout 3600s`       | 后端长时间没有数据时，代理等待响应的时间 |
| `proxy_send_timeout 3600s`       | 代理向后端发送请求的超时时间             |
| `proxy_connect_timeout 60s`      | 代理连接后端的超时时间                   |

应用配置后重新加载 Nginx：

```bash
nginx -t
nginx -s reload
```

命令说明：

| 命令              | 说明                            |
| ----------------- | ------------------------------- |
| `nginx -t`        | 检查 Nginx 配置文件语法是否正确 |
| `nginx -s reload` | 平滑重新加载 Nginx 配置         |

如果使用 NGINX Ingress Controller，也需要关注代理读写超时和缓冲配置。NGINX Ingress 的配置项中，`proxy-read-timeout`、`proxy-send-timeout`、`proxy-connect-timeout` 分别对应代理读取、发送和连接后端的超时配置，且文档中列出的默认值通常为 `60s`；`proxy-buffering` 用于控制响应缓冲。([docs.nginx.com](https://docs.nginx.com/nginx-ingress-controller/configuration/global-configuration/configmap-resource/?utm_source=chatgpt.com))

Kubernetes Ingress 示例：

文件位置：`k8s/sse-ingress.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: springboot-sse-ingress
  namespace: default
  annotations:
    # 关闭响应缓冲，避免 SSE 消息被代理层缓存
    nginx.org/proxy-buffering: "False"

    # 后端连接超时时间
    nginx.org/proxy-connect-timeout: "60s"

    # 读取后端响应超时时间，必须大于心跳间隔
    nginx.org/proxy-read-timeout: "3600s"

    # 向后端发送请求超时时间
    nginx.org/proxy-send-timeout: "3600s"
spec:
  rules:
    - host: sse.example.com
      http:
        paths:
          - path: /api/sse/
            pathType: Prefix
            backend:
              service:
                name: springboot-sse-service
                port:
                  number: 8080
```

不同 Ingress 控制器的注解名称可能不同。使用前需要确认当前集群实际安装的是 NGINX Inc Ingress、ingress-nginx，还是云厂商自带网关，并以对应产品文档为准。

Spring Boot 侧也可以为 SSE 响应增加关键响应头。下面示例通过过滤器只处理 `/api/sse/` 路径。

文件位置：`src/main/java/io/github/atengk/sse/config/SseHeaderFilter.java`

```java
package io.github.atengk.sse.config;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * SSE 响应头过滤器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class SseHeaderFilter extends OncePerRequestFilter {

    /**
     * 设置 SSE 响应头
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();

        if (StrUtil.startWith(requestUri, "/api/sse/")) {
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            response.setHeader("X-Accel-Buffering", "no");
            log.debug("SSE 响应头设置完成，请求路径：{}", requestUri);
        }

        filterChain.doFilter(request, response);
    }
}
```

这段代码用于给 SSE 接口补充响应头，辅助代理层识别事件流响应。关键配置仍应以网关或 Nginx 的专用配置为准，尤其是缓冲和超时。

### 心跳保活机制

心跳保活用于防止连接长时间没有数据时被浏览器、网关、Nginx 或中间网络设备关闭。生产环境中建议固定周期发送 `heartbeat` 事件，心跳间隔要小于代理层的读超时时间。

推荐关系如下：

```text
心跳间隔 < Nginx proxy_read_timeout < 服务端连接超时时间或业务允许连接时间
```

示例配置：

文件位置：`src/main/resources/application.yml`

```yaml
sse:
  # SSE 连接超时时间，0 表示服务端不主动超时
  timeout: 0

  # 心跳间隔，单位秒
  heartbeat-interval: 30

  # 单个用户最大连接数
  max-connection-per-user: 3
```

心跳事件格式示例：

```text
id: 91e9ef0f4f47437d88aeec35c0f80bd7
event: heartbeat
retry: 3000
data: 2026-05-06 14:30:00
```

前端监听心跳：

```javascript
eventSource.addEventListener("heartbeat", function (event) {
    console.debug("收到 SSE 心跳：", event.data);
});
```

心跳发送失败时，后端应立即移除连接。`SseEmitter` 的 `send` 方法会在发送失败时抛出 `IOException`，因此发送心跳和发送业务消息一样，都需要捕获异常并清理连接。`SseEmitter` 本身支持发送普通对象，也支持发送 `SseEmitter.event()` 构造出的标准 SSE 事件。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/SseEmitter.html?utm_source=chatgpt.com))

心跳配置建议如下：

| 场景              | 建议心跳间隔                                  |
| ----------------- | --------------------------------------------- |
| 本地开发          | 30 秒                                         |
| 普通后台管理系统  | 20 到 30 秒                                   |
| 经过 Nginx 或网关 | 小于代理读超时时间                            |
| 移动端弱网环境    | 15 到 30 秒，结合业务评估                     |
| 高频实时场景      | 不建议只依赖 SSE，应评估 WebSocket 或消息网关 |

心跳不应承载业务数据。它只用于确认连接仍然可用，前端一般只记录调试日志，不需要展示给用户。

心跳机制的生产建议如下：

| 建议                 | 说明                               |
| -------------------- | ---------------------------------- |
| 固定周期发送         | 避免连接长时间无数据               |
| 失败立即清理         | 发现无效连接后及时移除             |
| 不记录 info 级别日志 | 心跳频率高，建议使用 debug 日志    |
| 不携带敏感数据       | 心跳只需要时间戳或简单文本         |
| 间隔小于代理超时     | 例如心跳 30 秒，代理读超时 3600 秒 |
| 配合前端状态显示     | 前端可根据 `onerror` 提示重连状态  |

如果系统已经有频繁业务消息，例如每几秒推送一次进度，可以适当降低心跳频率；如果业务消息不稳定，则仍建议保留独立心跳。

### 并发连接控制

SSE 每个客户端连接都会占用服务端、代理和浏览器的一定资源。生产环境必须限制单用户连接数、总连接数和推送频率，避免页面反复刷新、异常重连或恶意请求导致连接堆积。

并发连接控制通常包含以下几个层面：

| 控制层面         | 说明                                     |
| ---------------- | ---------------------------------------- |
| 单用户连接数限制 | 限制同一用户最多保留多少个连接           |
| 全局连接数限制   | 限制当前服务实例最多维护多少个 SSE 连接  |
| 推送频率限制     | 防止高频推送压垮连接和网络               |
| 无效连接清理     | 通过回调、异常和心跳失败清理连接         |
| 权限控制         | SSE 连接必须绑定真实登录用户             |
| 集群路由策略     | 多实例部署时需要解决连接在哪个节点的问题 |

建议在 `application.yml` 中增加全局连接数配置：

```yaml
sse:
  # 单个用户允许保留的最大连接数
  max-connection-per-user: 3

  # 当前服务实例允许保留的最大连接数
  max-total-connection: 10000
```

配置类增加字段。

文件位置：`src/main/java/io/github/atengk/sse/config/SseProperties.java`

```java
/**
 * 当前服务实例最大连接数
 */
private Integer maxTotalConnection = 10000;
```

在连接注册前增加总连接数校验。

文件位置：`src/main/java/io/github/atengk/sse/manager/SseEmitterManager.java`

```java
/**
 * 检查是否允许创建新连接
 */
private void checkTotalConnectionLimit() {
    Integer maxTotalConnection = sseProperties.getMaxTotalConnection();
    if (maxTotalConnection == null || maxTotalConnection <= 0) {
        return;
    }

    int currentCount = countTotalConnection();
    if (currentCount >= maxTotalConnection) {
        log.warn("SSE 当前连接数已达到上限，当前连接数：{}，最大连接数：{}", currentCount, maxTotalConnection);
        throw new IllegalStateException("SSE 当前连接数已达到上限");
    }
}
```

在 `register` 方法中调用：

```java
public SseEmitter register(String userId, String clientId) {
    if (StrUtil.isBlank(userId)) {
        throw new IllegalArgumentException("用户 ID 不能为空");
    }

    checkTotalConnectionLimit();

    String connectionId = StrUtil.blankToDefault(clientId, IdUtil.fastSimpleUUID());
    clearExceededConnection(userId);

    SseEmitter emitter = new SseEmitter(sseProperties.getTimeout());

    // 后续逻辑保持不变
    return emitter;
}
```

如果达到连接上限，Controller 可以返回明确错误，避免前端不断重试。下面是一个基础异常处理示例。

文件位置：`src/main/java/io/github/atengk/sse/common/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.sse.common.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理非法状态异常
     *
     * @param e 异常对象
     * @return 错误响应
     */
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(IllegalStateException.class)
    public Map<String, Object> handleIllegalStateException(IllegalStateException e) {
        log.warn("请求被拒绝，异常信息：{}", e.getMessage());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", e.getMessage());
        return result;
    }

    /**
     * 处理参数异常
     *
     * @param e 异常对象
     * @return 错误响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, Object> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("请求参数异常，异常信息：{}", e.getMessage());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", e.getMessage());
        return result;
    }
}
```

前端收到 `429 Too Many Requests` 后，不应继续高频重连，应提示用户稍后重试：

```javascript
eventSource.onerror = function () {
    if (eventSource.readyState === EventSource.CONNECTING) {
        console.warn("SSE 连接中断，浏览器正在尝试重连");
        return;
    }

    if (eventSource.readyState === EventSource.CLOSED) {
        console.warn("SSE 连接已关闭");
    }
};
```

生产环境并发控制建议如下：

| 项目           | 建议                                              |
| -------------- | ------------------------------------------------- |
| 单用户连接数   | 通常设置为 1 到 3                                 |
| 单实例总连接数 | 根据机器规格、Tomcat 配置、网关限制和压测结果确定 |
| 心跳频率       | 20 到 30 秒较常见                                 |
| 推送频率       | 高频推送需要合并、节流或批量发送                  |
| 消息可靠性     | 重要消息必须落库，SSE 只做实时通知                |
| 集群部署       | 使用 MQ、Redis Pub/Sub 或消息中心转发事件         |
| 负载均衡       | 可以使用粘性会话，或设计跨节点推送能力            |
| 服务重启       | 前端自动重连，后端重新注册连接                    |

集群部署时需要特别注意：`SseEmitter` 连接保存在当前 JVM 内存中，如果用户连接到了 A 节点，而业务消息在 B 节点产生，B 节点无法直接拿到 A 节点内存中的连接。常见解决方案如下：

| 方案                | 说明                                                   |
| ------------------- | ------------------------------------------------------ |
| 粘性会话            | 同一用户尽量路由到同一服务实例                         |
| Redis Pub/Sub       | 各节点订阅消息，连接所在节点负责实际推送               |
| MQ 广播             | 消息进入 MQ 后由各节点消费并判断本节点是否存在目标连接 |
| 独立推送服务        | 将 SSE 连接集中到专门的推送服务                        |
| 消息落库 + 前端拉取 | SSE 只通知“有新消息”，前端再调用接口拉取详情           |

最终生产建议是：SSE 只负责实时触达，不负责业务消息的唯一可靠投递。重要消息必须持久化，前端收到 SSE 事件后再通过普通 HTTP 接口拉取最新业务数据，这样即使 SSE 连接断开、服务重启或节点切换，也不会造成业务数据丢失。
