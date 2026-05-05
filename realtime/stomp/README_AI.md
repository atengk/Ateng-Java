# Spring WebSocket STOMP 开发文档

## 项目概述

本项目用于在 Spring Boot 3 应用中构建基于 WebSocket 与 STOMP 的实时消息通信能力，覆盖广播通知、用户点对点消息、群组消息、业务事件推送、在线状态同步、管理端实时看板等典型场景。整体设计以“可接入、可鉴权、可扩展、可观测、可集群化”为目标，避免把 WebSocket 仅当作简单长连接使用，而是通过 STOMP 的消息语义建立统一的实时通信模型。

### 技术背景

传统 HTTP 通信以请求响应为主，客户端主动请求服务端，服务端通常不能在业务事件发生时直接向客户端推送数据。轮询、长轮询和 Server-Sent Events 可以解决部分实时性问题，但在双向通信、高频消息、多人协作、聊天、任务进度、实时看板等场景下，WebSocket 更适合作为基础传输协议。

WebSocket 通过一次 HTTP Upgrade 握手升级为长连接，握手成功后底层 TCP 连接保持打开，客户端和服务端可以在同一连接上双向发送消息。Spring 文档说明，WebSocket 提供基于单个 TCP 连接的全双工双向通信能力，并通过 HTTP Upgrade 完成协议切换。([Home](https://docs.spring.io/spring-framework/reference/web/websocket.html))

但是，WebSocket 本身只定义连接和消息传输能力，不规定业务消息格式、路由规则、订阅语义、确认机制和用户消息模型。Spring 文档也指出，WebSocket 是低层传输协议，不规定消息内容语义；如果没有更高层协议，客户端和服务端需要自行约定消息格式和路由规则。([Home](https://docs.spring.io/spring-framework/reference/web/websocket.html))

因此，本项目在 WebSocket 之上引入 STOMP 作为应用层消息协议。STOMP 提供类似消息队列的 `CONNECT`、`SEND`、`SUBSCRIBE`、`MESSAGE`、`ACK`、`ERROR` 等帧命令，使前后端可以围绕“连接、发送、订阅、广播、点对点推送”建立清晰的开发规范。STOMP 是基于帧的文本协议，可运行在 TCP、WebSocket 等可靠双向流协议之上。

### 适用场景

本项目适用于需要服务端主动推送、客户端实时接收、多个客户端共享业务状态的系统。典型场景包括聊天系统、站内通知、任务进度推送、审批待办提醒、订单状态变更通知、设备状态上报、实时大屏、在线用户状态、群组协作、管理端告警、AI 流式响应推送等。

在业务系统中，可以将 WebSocket STOMP 能力定位为“实时消息通道”，而不是替代所有 HTTP API。HTTP API 仍然负责登录、初始化数据、历史记录查询、分页查询、文件上传、复杂条件检索等请求响应型能力；WebSocket STOMP 负责连接保持、实时订阅、业务事件推送、在线状态同步和低延迟消息分发。

建议优先使用 WebSocket STOMP 的场景如下：

| 场景             | 是否适合             | 说明                                               |
| ---------------- | -------------------- | -------------------------------------------------- |
| 系统公告广播     | 适合                 | 所有在线用户订阅同一主题，服务端统一推送           |
| 用户站内通知     | 适合                 | 按用户维度推送，避免全量广播                       |
| 聊天消息         | 适合                 | 需要低延迟双向通信                                 |
| 任务进度         | 适合                 | 后台任务执行中持续向前端推送进度                   |
| 实时看板         | 适合                 | 管理端订阅统计数据或业务事件                       |
| 历史消息分页查询 | 不建议仅用 WebSocket | 更适合 HTTP API 查询                               |
| 大文件传输       | 不建议               | 建议使用 HTTP 上传下载，WebSocket 只推送状态       |
| 强一致事务处理   | 不建议直接依赖推送   | 推送应作为通知层，最终状态以数据库或 HTTP 查询为准 |

### 功能目标

本项目的目标是提供一套完整、规范、可落地的 Spring Boot 3 WebSocket STOMP 实时通信方案。开发完成后，系统应具备稳定的连接接入、消息路由、用户绑定、权限校验、消息推送、异常处理、日志监控和生产部署能力。

核心功能目标如下：

| 目标             | 说明                                                   |
| ---------------- | ------------------------------------------------------ |
| 基础连接能力     | 支持前端通过 WebSocket 或 SockJS 建立 STOMP 连接       |
| 广播推送能力     | 支持服务端向 `/topic/**` 主题广播消息                  |
| 用户点对点能力   | 支持服务端向指定用户的 `/user/queue/**` 队列推送消息   |
| 群组消息能力     | 支持按群组、房间、租户、组织等业务维度推送             |
| 业务事件推送     | 支持订单、审批、任务、告警等业务事件实时通知           |
| 认证鉴权能力     | 支持 Token、JWT、Spring Security 或 Sa-Token 接入      |
| 连接生命周期管理 | 支持连接建立、订阅、取消订阅、断开、异常关闭等事件处理 |
| 消息可靠性设计   | 支持消息 ID、幂等、ACK、回执、补偿和持久化扩展         |
| 集群扩展能力     | 支持 Redis、RabbitMQ、ActiveMQ 等外部组件扩展          |
| 可观测能力       | 支持连接数、消息量、失败率、延迟、线程池等指标监控     |

### 功能边界

本项目主要解决“实时消息通信通道”的工程化问题，不直接承担全部业务系统职责。边界清晰可以避免 WebSocket 模块膨胀为复杂业务中心，也便于后续独立部署、扩容和故障隔离。

本项目负责的内容包括：

| 边界类型 | 包含内容                                             |
| -------- | ---------------------------------------------------- |
| 连接层   | WebSocket Endpoint、SockJS、跨域、心跳、连接生命周期 |
| 协议层   | STOMP 帧、订阅、发送、用户目的地、消息前缀规范       |
| 安全层   | 握手认证、CONNECT 鉴权、SUBSCRIBE 鉴权、SEND 鉴权    |
| 消息层   | 广播、点对点、群组、系统通知、业务事件推送           |
| 会话层   | Session 注册、用户绑定、多端连接、强制下线           |
| 可靠性层 | 消息 ID、ACK、回执、重试、离线消息、失败记录         |
| 观测层   | 日志、指标、审计、异常告警、管理端看板               |

本项目不直接负责的内容包括：

| 非职责范围         | 说明                                                 |
| ------------------ | ---------------------------------------------------- |
| 用户账号体系       | 只对接已有用户体系，不重新实现用户中心               |
| 复杂业务审批流     | 只推送审批事件，不负责审批流编排                     |
| 文件上传下载       | 通过 HTTP 或对象存储完成，WebSocket 只推送进度或结果 |
| 大规模消息队列替代 | STOMP 通道不是 Kafka、RabbitMQ 的完整替代品          |
| 数据最终一致性保证 | 推送不作为唯一事实来源，最终状态以数据库为准         |
| 离线终端强实时送达 | 离线消息需要结合持久化、补偿查询和客户端拉取         |

### 整体技术栈

本项目基于 Spring Boot 3 和 Spring Framework WebSocket Messaging 构建，优先使用 Spring 官方 STOMP 编程模型。Spring STOMP 支持可以将消息路由到 `@Controller` 中的 `@MessageMapping` 方法，也可以将消息交给内置 Simple Broker 或外部 STOMP Broker 进行订阅和广播处理。([Home](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/overview.html))

推荐技术栈如下：

| 分类           | 技术选型                          | 说明                                    |
| -------------- | --------------------------------- | --------------------------------------- |
| 后端框架       | Spring Boot 3.x                   | 项目基础框架                            |
| WebSocket      | Spring WebSocket                  | WebSocket 握手、连接、消息传输          |
| STOMP          | Spring Messaging                  | STOMP 订阅、发送、消息映射              |
| 消息发送       | SimpMessagingTemplate             | 服务端主动推送消息                      |
| 认证授权       | Spring Security / Sa-Token / JWT  | 按项目现有认证体系选择                  |
| JSON 序列化    | Jackson                           | STOMP 消息体 JSON 编解码                |
| 工具类         | Hutool                            | 字符串、集合、JSON、日期、ID 等工具处理 |
| 本地消息代理   | Simple Broker                     | 单体或低并发场景快速接入                |
| 外部 Broker    | RabbitMQ / ActiveMQ               | 生产集群、跨节点消息分发                |
| 缓存与状态同步 | Redis / Redisson                  | 在线状态、会话映射、分布式锁            |
| 持久化         | MySQL / PostgreSQL                | 消息记录、通知记录、离线消息            |
| ORM            | MyBatis-Plus                      | 消息、通知、回执等表的 CRUD             |
| 监控           | Micrometer + Prometheus + Grafana | 指标采集与看板展示                      |
| 日志           | Logback / Logstash                | 连接日志、推送日志、安全审计            |
| 前端客户端     | @stomp/stompjs / sockjs-client    | 浏览器 STOMP 客户端接入                 |
| 部署           | Docker / Kubernetes / Nginx       | 容器化、网关转发、WSS 接入              |

后续基础接入章节可以使用以下 Maven 依赖作为基线。

```xml
<!-- Spring Boot Web：提供 HTTP API、Web 容器和基础 Web 能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring WebSocket：提供 WebSocket、STOMP、消息代理等能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Spring Security：用于连接认证、订阅鉴权、发送鉴权，可按项目实际认证体系替换 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Validation：用于校验 STOMP 消息体字段 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Redis：用于在线状态、Session 映射、分布式消息同步等扩展能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Hutool：常用工具类，处理字符串、集合、JSON、日期、ID 等通用逻辑 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.36</version>
</dependency>

<!-- Lombok：减少 DTO、VO、配置类中的样板代码 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

## WebSocket 与 STOMP 基础

本章节用于说明 WebSocket 与 STOMP 在项目中的职责分工。WebSocket 负责建立浏览器与服务端之间的双向长连接；STOMP 负责在这条连接上定义消息帧格式、发送命令、订阅命令、目的地路由和消息分发模型。

### WebSocket 通信模型

WebSocket 的通信模型可以分为握手阶段和消息通信阶段。握手阶段仍然基于 HTTP，客户端发起带有 `Upgrade: websocket` 的 HTTP 请求，服务端返回 `101 Switching Protocols` 后完成协议升级。连接建立后，客户端和服务端复用同一个 TCP 连接进行双向通信。([Home](https://docs.spring.io/spring-framework/reference/web/websocket.html))

基础流程如下：

```text
浏览器客户端
    |
    | 1. HTTP Upgrade 握手
    |    GET /ws/stomp
    |    Upgrade: websocket
    |    Sec-WebSocket-Protocol: v10.stomp, v11.stomp, v12.stomp
    v
Spring Boot 服务端
    |
    | 2. 返回 101 Switching Protocols
    v
WebSocket 长连接建立
    |
    | 3. 客户端发送 STOMP CONNECT
    | 4. 服务端返回 STOMP CONNECTED
    | 5. 客户端 SUBSCRIBE 订阅主题
    | 6. 客户端 SEND 发送消息
    | 7. 服务端 MESSAGE 推送消息
```

在 Spring Boot 3 中，WebSocket 连接通常不是直接由业务代码逐帧处理，而是交给 Spring WebSocket STOMP 模型管理。客户端连接某个 Endpoint，例如 `/ws/stomp`，随后通过 STOMP 的 `destination` 头完成消息路由。

推荐的连接地址和消息路径规划如下：

| 类型               | 示例            | 说明                         |
| ------------------ | --------------- | ---------------------------- |
| WebSocket Endpoint | `/ws/stomp`     | 客户端建立连接的地址         |
| 应用发送前缀       | `/app`          | 客户端发送到服务端业务处理器 |
| 广播订阅前缀       | `/topic`        | 多个客户端订阅同一主题       |
| 用户队列前缀       | `/user/queue`   | 指定用户接收点对点消息       |
| 群组主题前缀       | `/topic/group`  | 群组、房间、组织等维度广播   |
| 系统通知前缀       | `/topic/system` | 系统公告、维护通知等         |

### STOMP 协议定位

STOMP 的定位是 WebSocket 之上的应用层消息协议。WebSocket 只提供双向通道，不关心消息内容；STOMP 在通道上定义“命令 + Header + Body”的帧结构，让客户端和服务端围绕目的地进行发送、订阅和消息分发。

在 Spring 中，STOMP 的核心价值体现在三个方面：

第一，统一消息路由。客户端发送到 `/app/**` 的消息可以被路由到 `@MessageMapping` 方法，服务端可以像处理 HTTP Controller 一样处理实时消息。Spring 官方示例中，`setApplicationDestinationPrefixes("/app")` 用于把 `/app` 开头的 STOMP 消息路由到 `@MessageMapping` 方法。([Home](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/enable.html))

第二，统一订阅模型。客户端订阅 `/topic/**` 或 `/queue/**` 后，服务端可以通过消息代理把消息推送给匹配订阅者。Spring 内置 Simple Broker 会保存客户端订阅关系，并把消息广播给目的地匹配的连接。([Home](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/handle-simple-broker.html))

第三，统一点对点模型。Spring 支持 `/user/**` 用户目的地，应用可以向指定用户发送消息，Spring 会把通用用户目的地转换为用户 Session 独有的实际目的地，避免不同用户之间的队列冲突。([Home](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/user-destination.html))

常见 STOMP 命令如下：

| 命令          | 方向             | 说明                                  |
| ------------- | ---------------- | ------------------------------------- |
| `CONNECT`     | 客户端 -> 服务端 | 建立 STOMP 会话                       |
| `CONNECTED`   | 服务端 -> 客户端 | STOMP 会话建立成功                    |
| `SEND`        | 客户端 -> 服务端 | 客户端向指定目的地发送消息            |
| `SUBSCRIBE`   | 客户端 -> 服务端 | 客户端订阅指定目的地                  |
| `UNSUBSCRIBE` | 客户端 -> 服务端 | 客户端取消订阅                        |
| `MESSAGE`     | 服务端 -> 客户端 | 服务端向订阅者推送消息                |
| `ACK`         | 客户端 -> 服务端 | 客户端确认消息处理成功                |
| `NACK`        | 客户端 -> 服务端 | 客户端确认消息处理失败                |
| `RECEIPT`     | 服务端 -> 客户端 | 服务端确认已处理带 receipt 的客户端帧 |
| `ERROR`       | 服务端 -> 客户端 | 协议错误或业务异常返回                |

### STOMP 帧结构

STOMP 是基于帧的协议，帧结构由命令行、Header 列表、空行、Body 和 NULL 结束符组成。STOMP 1.2 规范说明，一个帧由 command、可选 headers、可选 body 组成，body 后以 NULL octet 结束。

通用帧结构如下：

```text
COMMAND
header1:value1
header2:value2

Body^@
```

其中：

| 部分      | 说明                                                         |
| --------- | ------------------------------------------------------------ |
| `COMMAND` | STOMP 命令，例如 `CONNECT`、`SEND`、`SUBSCRIBE`、`MESSAGE`   |
| `header`  | 帧头，格式为 `key:value`，用于描述目的地、内容类型、订阅 ID、消息 ID 等 |
| 空行      | Header 与 Body 的分隔符                                      |
| `Body`    | 消息体，可以是 JSON、文本或二进制内容                        |
| `^@`      | NULL 结束符，表示当前帧结束                                  |

客户端连接示例：

```text
CONNECT
accept-version:1.2
heart-beat:10000,10000
Authorization:Bearer xxx.yyy.zzz

^@
```

服务端连接成功响应示例：

```text
CONNECTED
version:1.2
heart-beat:10000,10000

^@
```

客户端订阅广播主题示例：

```text
SUBSCRIBE
id:sub-system-notice
destination:/topic/system/notice

^@
```

客户端发送业务消息示例：

```text
SEND
destination:/app/chat/send
content-type:application/json

{
  "receiverId": "10002",
  "content": "你好，这是一条聊天消息"
}^@
```

服务端推送消息示例：

```text
MESSAGE
subscription:sub-system-notice
message-id:msg-20260505-000001
destination:/topic/system/notice
content-type:application/json

{
  "title": "系统维护通知",
  "content": "系统将在今晚 23:00 进行维护"
}^@
```

STOMP 规范还建议，当 `SEND`、`MESSAGE`、`ERROR` 帧存在消息体时，应设置 `content-type` 帮助接收端解析 body；当 body 中可能包含 NULL 字节时，必须使用 `content-length` 明确字节长度。

### Topic 与 Queue 语义

在本项目中，`Topic` 和 `Queue` 用于区分消息分发语义，而不是表示 Java 类型或数据库表。

`Topic` 表示发布订阅模型，适合一对多广播。多个客户端订阅同一个 `/topic/**` 目的地后，服务端向该目的地发送的消息会被多个订阅者接收。例如系统公告、实时大屏指标、群组消息、订单状态广播等。

`Queue` 表示点对点或用户维度消息模型，适合一对一或有限消费者场景。客户端通常订阅 `/user/queue/**` 接收属于当前用户的私有消息。例如个人通知、审批待办、聊天单聊消息、任务处理结果等。

需要注意，在 Spring 内置 Simple Broker 中，`/topic` 和 `/queue` 前缀本身没有协议级特殊含义，它们主要是一种约定，用于区分发布订阅和点对点消息语义；如果使用外部 Broker，需要以对应 Broker 的 STOMP 目的地规则为准。([Home](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/enable.html))

推荐约定如下：

| 前缀                     | 语义               | 示例                     | 使用场景                   |
| ------------------------ | ------------------ | ------------------------ | -------------------------- |
| `/topic/system/**`       | 系统广播           | `/topic/system/notice`   | 系统公告、维护通知         |
| `/topic/group/{groupId}` | 群组广播           | `/topic/group/10001`     | 群聊、房间消息             |
| `/topic/dashboard/**`    | 看板广播           | `/topic/dashboard/order` | 实时统计、运营大屏         |
| `/user/queue/notice`     | 用户通知队列       | `/user/queue/notice`     | 个人站内通知               |
| `/user/queue/chat`       | 用户聊天队列       | `/user/queue/chat`       | 单聊消息                   |
| `/user/queue/errors`     | 用户错误队列       | `/user/queue/errors`     | 只返回给当前用户的错误消息 |
| `/app/**`                | 客户端发送到服务端 | `/app/chat/send`         | 触发服务端业务处理         |

### 用户点对点消息模型

用户点对点消息用于向指定用户发送私有消息。客户端订阅固定的用户目的地，例如 `/user/queue/notice`，服务端通过用户标识发送消息。Spring 会把该用户目的地转换为具体 Session 对应的内部目的地，从而避免不同用户订阅同一逻辑路径时互相串消息。Spring 官方文档说明，`/user/` 前缀会由 `UserDestinationMessageHandler` 处理，并转换为用户 Session 独有的目的地。([Home](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/user-destination.html))

客户端订阅路径通常写成：

```text
/user/queue/notice
/user/queue/chat
/user/queue/task
/user/queue/errors
```

服务端发送路径通常使用逻辑目标，不需要拼接 `/user/{username}` 的完整客户端订阅路径。典型发送方式如下：

```java
simpMessagingTemplate.convertAndSendToUser(
    userId,
    "/queue/notice",
    messageBody
);
```

用户点对点模型的处理流程如下：

```text
1. 用户登录系统，获取 Token
2. 客户端携带 Token 建立 WebSocket STOMP 连接
3. 服务端在握手或 CONNECT 阶段解析用户身份
4. 服务端将 Principal 与 WebSocket Session 绑定
5. 客户端订阅 /user/queue/notice
6. 业务系统产生用户通知事件
7. 服务端调用 convertAndSendToUser(userId, "/queue/notice", body)
8. Spring 将消息路由到该用户已连接的一个或多个 Session
9. 客户端收到 MESSAGE 帧并更新页面状态
```

用户点对点消息适合以下场景：

| 场景     | 订阅路径              | 说明                     |
| -------- | --------------------- | ------------------------ |
| 个人通知 | `/user/queue/notice`  | 审批、待办、提醒         |
| 单聊消息 | `/user/queue/chat`    | 指定用户接收聊天消息     |
| 任务结果 | `/user/queue/task`    | 后台任务完成后通知发起人 |
| 异常提示 | `/user/queue/errors`  | 只向当前操作用户返回错误 |
| 强制下线 | `/user/queue/session` | 通知用户某端连接被踢出   |

多端登录时，同一用户可能存在多个 WebSocket Session。默认情况下，Spring 用户目的地可以面向该用户的多个 Session 投递；如果只希望返回给当前发起请求的 Session，可以结合 `@SendToUser(broadcast = false)` 处理当前会话级响应。([Home](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/user-destination.html))

### 广播消息模型

广播消息用于向多个订阅者同时推送消息。客户端订阅同一个 `/topic/**` 目的地，服务端向该目的地发送消息后，所有匹配订阅者都会收到消息。Spring 内置 Simple Broker 会保存订阅关系，并向目的地匹配的连接广播消息。([Home](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/handle-simple-broker.html))

广播消息模型的基本流程如下：

```text
1. 客户端 A 订阅 /topic/system/notice
2. 客户端 B 订阅 /topic/system/notice
3. 客户端 C 订阅 /topic/dashboard/order
4. 服务端向 /topic/system/notice 发送系统公告
5. 客户端 A 和客户端 B 收到公告
6. 客户端 C 不会收到公告，因为订阅目的地不匹配
```

推荐广播路径设计如下：

| 广播类型   | 路径                       | 说明               |
| ---------- | -------------------------- | ------------------ |
| 全站公告   | `/topic/system/notice`     | 所有在线用户可订阅 |
| 管理端告警 | `/topic/admin/alarm`       | 管理端用户订阅     |
| 订单看板   | `/topic/dashboard/order`   | 实时订单统计       |
| 群组消息   | `/topic/group/{groupId}`   | 指定群组成员订阅   |
| 房间消息   | `/topic/room/{roomId}`     | 指定房间用户订阅   |
| 租户广播   | `/topic/tenant/{tenantId}` | 指定租户范围内广播 |

广播消息需要重点控制订阅权限和消息范围。不要把敏感业务数据直接广播到公共 Topic；涉及用户隐私、租户隔离、部门隔离、管理端权限的数据，应在订阅阶段校验权限，并按业务维度拆分目的地。

广播模型适合以下类型的业务事件：

```json
{
  "messageId": "msg-20260505-000001",
  "type": "SYSTEM_NOTICE",
  "title": "系统公告",
  "content": "系统将在今晚 23:00 进行例行维护",
  "timestamp": "2026-05-05T10:00:00+09:00"
}
```

### 请求响应与事件推送模型

在 WebSocket STOMP 中，请求响应和事件推送需要明确区分。请求响应通常由客户端主动发送 `SEND` 到 `/app/**`，服务端通过 `@MessageMapping` 处理后返回结果；事件推送通常由服务端业务事件、定时任务、MQ 消费、数据库状态变化等触发，再主动推送到 `/topic/**` 或 `/user/queue/**`。

请求响应模型适合“客户端触发、服务端处理、返回处理结果”的场景。例如发送聊天消息、加入房间、修改在线状态、提交协同编辑操作等。

```text
客户端 SEND /app/chat/send
        |
        v
服务端 @MessageMapping("/chat/send")
        |
        v
业务校验、落库、权限判断
        |
        v
推送给接收人 /user/queue/chat
或广播到群组 /topic/group/{groupId}
```

事件推送模型适合“服务端业务状态变化、主动通知客户端”的场景。例如订单状态变化、审批流转、任务进度、告警产生、设备状态上报、定时刷新看板等。

```text
业务系统产生事件
        |
        v
订单状态变更 / 任务进度变化 / 审批待办生成
        |
        v
服务端组装统一消息体
        |
        v
SimpMessagingTemplate 推送
        |
        v
/topic/** 广播 或 /user/queue/** 点对点
```

两类模型建议采用以下边界：

| 模型     | 触发方      | 典型路径         | 返回对象                | 适用场景                       |
| -------- | ----------- | ---------------- | ----------------------- | ------------------------------ |
| 请求响应 | 客户端      | `/app/**`        | ACK、业务结果、错误消息 | 聊天发送、加入群组、订阅初始化 |
| 用户推送 | 服务端      | `/user/queue/**` | 私有消息                | 通知、待办、任务结果、单聊     |
| 广播推送 | 服务端      | `/topic/**`      | 公共事件                | 系统公告、群组消息、实时看板   |
| 补偿查询 | 客户端 HTTP | `/api/**`        | 历史数据                | 断线重连、消息补偿、分页查询   |

实践中不要把 WebSocket 响应当作数据库事务成功的唯一依据。对于关键业务，建议采用“HTTP 或 STOMP 发起业务操作 + 数据库落库 + 事务提交后推送 + 客户端必要时 HTTP 补偿查询”的方式，避免连接中断、重复推送、客户端重连导致状态不一致。


## 系统架构设计

系统架构设计用于明确 WebSocket STOMP 模块在不同部署形态下的职责边界、调用链路、消息流转方式和扩展方式。实际项目可以从单体架构开始接入，随着连接规模、业务复杂度和部署节点增加，再逐步演进到网关接入、集群部署和外部消息代理架构。

### 单体应用架构

单体应用架构适用于项目早期、内部系统、中小规模通知系统、管理后台实时看板等场景。WebSocket Endpoint、STOMP 消息处理器、业务服务、数据库访问、Redis 缓存都部署在同一个 Spring Boot 应用中，整体结构简单，开发和调试成本较低。

典型链路如下：

```text
浏览器客户端
    |
    | WebSocket / STOMP
    v
Spring Boot 单体应用
    |
    | @MessageMapping 处理客户端消息
    | SimpMessagingTemplate 推送服务端消息
    v
Simple Broker
    |
    v
订阅客户端
```

单体架构中通常使用 Spring 内置 Simple Broker 完成订阅关系维护和消息分发。客户端发送到 `/app/**` 的消息由服务端业务处理器接收，服务端向 `/topic/**` 或 `/user/queue/**` 推送消息。

推荐配置关系如下：

| 模块                   | 职责                                        |
| ---------------------- | ------------------------------------------- |
| WebSocketConfig        | 注册 STOMP Endpoint、配置消息代理、配置前缀 |
| MessageController      | 接收客户端通过 `/app/**` 发送的消息         |
| MessagePushService     | 封装广播、点对点、群组推送能力              |
| WebSocketEventListener | 监听连接、订阅、断开等生命周期事件          |
| SessionRegistry        | 维护用户与 Session 的绑定关系               |
| Redis 缓存             | 可选，用于在线状态、消息去重、未读数缓存    |

单体架构优点是实现直接、部署简单、排查方便；缺点是连接容量、消息吞吐和故障隔离能力受单个应用实例限制。若系统后续需要横向扩容，应提前规范用户标识、消息路由、在线状态和消息持久化设计，避免后期改造成本过高。

### 前后端分离架构

前后端分离架构是 WebSocket STOMP 项目中最常见的接入方式。前端通过 HTTP API 完成登录、初始化数据、历史记录查询等请求响应操作，通过 WebSocket STOMP 完成实时消息订阅、发送和推送接收。

典型链路如下：

```text
Vue / React 前端
    |
    | 1. HTTP 登录，获取 Token
    v
Spring Boot HTTP API
    |
    | 2. 返回 access_token
    v
Vue / React 前端
    |
    | 3. 携带 Token 建立 WebSocket STOMP 连接
    v
Spring Boot WebSocket Endpoint
    |
    | 4. CONNECT 阶段认证用户身份
    v
STOMP 消息通道
```

前后端分离时，需要特别关注 Token 传递方式。浏览器原生 WebSocket API 对自定义 Header 支持有限，实际项目中通常通过以下方式之一传递身份信息：

| 方式                 | 示例                        | 说明                               |
| -------------------- | --------------------------- | ---------------------------------- |
| Query 参数           | `/ws/stomp?token=xxx`       | 接入简单，但需要避免日志泄露 Token |
| STOMP CONNECT Header | `Authorization: Bearer xxx` | 推荐方式，适合 `@stomp/stompjs`    |
| Cookie               | 自动携带登录 Cookie         | 适合传统 Session 或同域系统        |
| SockJS 参数          | SockJS 连接 URL 携带参数    | 兼容部分不支持 WebSocket 的环境    |

前后端分离项目建议统一约定以下路径：

| 类型           | 路径示例                    | 说明              |
| -------------- | --------------------------- | ----------------- |
| HTTP 登录      | `POST /api/auth/login`      | 获取 Token        |
| WebSocket 连接 | `/ws/stomp`                 | 建立 STOMP 连接   |
| 客户端发送     | `/app/chat/send`            | 发送消息到服务端  |
| 广播订阅       | `/topic/system/notice`      | 接收系统公告      |
| 用户订阅       | `/user/queue/notice`        | 接收个人通知      |
| 群组订阅       | `/topic/group/{groupId}`    | 接收群组消息      |
| 历史查询       | `GET /api/messages/history` | HTTP 查询历史消息 |

前端应将连接管理、订阅管理、断线重连、消息解析、消息去重和页面销毁清理封装为统一模块，避免每个页面重复创建连接或重复订阅。

### 微服务架构

微服务架构适用于业务系统已经拆分为多个服务的场景，例如订单服务、审批服务、任务服务、通知服务、用户服务等。此时 WebSocket 模块通常不直接承载复杂业务处理，而是作为实时消息网关或通知中心存在。

推荐职责拆分如下：

```text
订单服务 / 审批服务 / 任务服务 / 告警服务
    |
    | 业务事件
    v
MQ / Redis Stream / Kafka
    |
    v
通知服务 / WebSocket 服务
    |
    | SimpMessagingTemplate
    v
WebSocket STOMP 客户端
```

微服务架构中，业务服务不建议直接依赖 WebSocket Session，也不应该直接管理客户端连接。业务服务只需要发布业务事件，例如“订单状态变更”“审批待办生成”“任务进度更新”“设备告警产生”。WebSocket 服务负责消费这些事件，转换为前端统一消息体，再推送给目标用户、群组或主题。

推荐拆分方式如下：

| 服务           | 职责                                     |
| -------------- | ---------------------------------------- |
| 用户服务       | 提供用户信息、组织关系、权限数据         |
| 业务服务       | 产生业务事件，不直接感知 WebSocket 连接  |
| 通知服务       | 维护通知记录、未读数、消息模板           |
| WebSocket 服务 | 管理连接、订阅、会话、实时推送           |
| MQ 服务        | 解耦业务事件和实时推送                   |
| Redis          | 缓存在线状态、用户 Session、消息去重数据 |

微服务架构的关键点是事件标准化。不同业务服务发布的事件格式应统一，否则 WebSocket 服务会充满大量业务适配逻辑。

推荐事件结构如下：

```json
{
  "eventId": "evt-20260505-000001",
  "eventType": "ORDER_STATUS_CHANGED",
  "receiverType": "USER",
  "receiverIds": ["10001", "10002"],
  "title": "订单状态变更",
  "content": "订单已完成支付",
  "bizId": "order-100001",
  "bizType": "ORDER",
  "timestamp": "2026-05-05T10:00:00+09:00"
}
```

### 网关接入架构

网关接入架构适用于统一入口、统一鉴权、统一限流、统一域名、统一 HTTPS/WSS 证书管理的系统。前端不直接访问 WebSocket 服务实例，而是通过 Nginx、Spring Cloud Gateway、Kong、Traefik 或 Ingress 转发 WebSocket 请求。

典型链路如下：

```text
浏览器客户端
    |
    | wss://api.example.com/ws/stomp
    v
Nginx / Gateway / Ingress
    |
    | WebSocket Upgrade 转发
    v
WebSocket 服务
    |
    v
STOMP 消息处理
```

网关接入时必须正确转发 WebSocket Upgrade 相关 Header，否则连接会在握手阶段失败。网关还需要考虑连接保持时间、读写超时、请求体大小、跨域、认证信息透传和负载均衡策略。

网关层建议承担以下能力：

| 能力         | 说明                                           |
| ------------ | ---------------------------------------------- |
| WSS 终止     | 统一处理 HTTPS/WSS 证书                        |
| 域名路由     | 将 `/ws/**` 转发到 WebSocket 服务              |
| 鉴权前置     | 可校验 Token 是否存在或格式是否合法            |
| 限流防护     | 限制 IP 连接频率、异常握手频率                 |
| 黑名单拦截   | 拦截恶意 IP、异常来源                          |
| Header 透传  | 透传 `Authorization`、`X-Forwarded-For` 等信息 |
| 连接超时控制 | 避免网关过早关闭长连接                         |

需要注意，网关层只能做通用安全控制，不应替代 WebSocket 服务内部的 STOMP 鉴权。连接建立后，客户端仍可能发送非法 `SUBSCRIBE` 或 `SEND` 帧，因此服务端还需要通过 `ChannelInterceptor` 对订阅路径和发送路径做细粒度权限校验。

### 集群部署架构

集群部署架构适用于高可用、高并发连接、多实例横向扩容的场景。WebSocket 连接是有状态连接，客户端连接建立后会固定落到某一个服务实例上，因此集群架构需要解决 Session 分布、用户路由、广播同步、点对点投递和在线状态一致性问题。

典型集群结构如下：

```text
浏览器客户端
    |
    v
负载均衡 / 网关
    |
    +------------------+------------------+
    |                  |                  |
    v                  v                  v
WebSocket 实例 A    WebSocket 实例 B    WebSocket 实例 C
    |                  |                  |
    +------------------+------------------+
                       |
                       v
              Redis / MQ / Broker
```

集群部署下的主要问题如下：

| 问题                     | 说明                           | 推荐方案                                |
| ------------------------ | ------------------------------ | --------------------------------------- |
| 用户连接在哪个实例       | 同一用户可能连接到任意实例     | Redis 维护用户与实例、Session 映射      |
| 点对点消息如何投递       | 业务事件可能被任意实例消费     | 使用 Broker Relay 或 Redis Pub/Sub 转发 |
| 广播消息是否全实例同步   | 只发本实例会导致部分用户收不到 | 使用外部 Broker 或跨实例广播            |
| 在线状态是否一致         | 每个实例只知道本地连接         | Redis 汇总在线状态                      |
| 强制下线如何跨实例生效   | 目标 Session 可能不在当前实例  | 发布踢人事件到对应实例                  |
| 负载均衡是否需要粘性会话 | WebSocket 长连接天然绑定实例   | 可使用 Sticky Session，但不能只依赖它   |

集群架构有两种常见方案。

第一种是“Sticky Session + Redis 状态同步”。客户端连接建立后固定到某个实例，本实例维护连接，Redis 记录用户在线状态和 Session 映射。业务推送时，如果只需要广播，可以通过 Redis Pub/Sub 通知所有实例执行本地推送。这种方案实现相对简单，适合中等规模系统。

第二种是“Broker Relay + 外部消息代理”。所有 WebSocket 实例连接到 RabbitMQ 或 ActiveMQ 的 STOMP Broker，由外部 Broker 统一维护订阅关系和消息分发。该方式更适合生产集群、高可用和大规模消息分发，但引入了外部 Broker 的运维复杂度。

### 消息代理架构

消息代理架构决定消息如何在服务端和订阅客户端之间分发。Spring WebSocket STOMP 常见模式包括内置 Simple Broker、外部 STOMP Broker Relay、业务 MQ 桥接和 Redis Pub/Sub 桥接。

整体关系如下：

```text
客户端 STOMP 连接
    |
    v
Spring WebSocket Message Channel
    |
    +--> @MessageMapping 业务处理
    |
    +--> Simple Broker / Broker Relay
              |
              v
         订阅客户端
```

几种代理模式对比如下：

| 模式                 | 适用场景                     | 优点                          | 注意事项                        |
| -------------------- | ---------------------------- | ----------------------------- | ------------------------------- |
| Simple Broker        | 单体、开发环境、中小规模通知 | 配置简单，无需外部依赖        | 不适合复杂集群和高可靠消息      |
| RabbitMQ STOMP Relay | 生产集群、跨节点分发         | 订阅关系和消息分发交给 Broker | 需要运维 RabbitMQ 和 STOMP 插件 |
| ActiveMQ Relay       | 企业消息系统、传统 MQ 场景   | 支持 JMS 生态和 STOMP         | 需要关注 Broker 性能和高可用    |
| Redis Pub/Sub 桥接   | 简单跨实例广播               | 接入成本低                    | 不提供持久化和严格可靠性        |
| Kafka 桥接           | 高吞吐业务事件流             | 适合事件驱动和审计留存        | 不适合作为浏览器 STOMP Broker   |

推荐演进路径如下：

```text
开发环境 / 初期项目
    -> Simple Broker

单体生产 / 中小规模系统
    -> Simple Broker + Redis 在线状态

多实例集群 / 较高实时消息规模
    -> Redis Pub/Sub 跨实例同步 或 RabbitMQ STOMP Relay

高可靠业务事件 / 多业务服务解耦
    -> Kafka / RabbitMQ 作为业务事件总线
    -> WebSocket 服务消费事件后推送
```

消息代理架构选择时，需要明确两个问题：第一，是否需要跨实例共享订阅和消息分发；第二，是否需要消息持久化、确认、重试和死信处理。Simple Broker 适合实时转发，不适合承担复杂可靠消息系统职责；对于关键业务消息，应结合数据库消息表、通知表、回执表和补偿查询实现可靠性。

## 核心功能规划

核心功能规划用于定义 WebSocket STOMP 模块需要提供的主要业务能力。规划阶段应先明确消息类型、目标用户、订阅路径、权限要求、是否需要持久化、是否需要 ACK 和补偿机制，再进入具体编码实现。

### 实时广播通知

实时广播通知用于向一组订阅者同时推送消息，例如系统维护通知、版本发布提醒、运营公告、管理端实时事件等。广播通知通常使用 `/topic/**` 路径，多个客户端订阅同一个主题即可同时接收消息。

推荐路径如下：

| 类型     | 订阅路径                          | 说明               |
| -------- | --------------------------------- | ------------------ |
| 全站公告 | `/topic/system/notice`            | 所有在线用户可订阅 |
| 租户公告 | `/topic/tenant/{tenantId}/notice` | 指定租户用户订阅   |
| 管理公告 | `/topic/admin/notice`             | 管理端用户订阅     |
| 业务广播 | `/topic/biz/{bizType}`            | 按业务类型广播     |

功能要求如下：

| 能力         | 说明                                         |
| ------------ | -------------------------------------------- |
| 广播范围控制 | 支持全站、租户、部门、角色、群组等范围       |
| 消息类型区分 | 支持普通通知、重要通知、紧急通知             |
| 消息持久化   | 重要公告需要落库，支持历史查询               |
| 订阅鉴权     | 非授权用户不能订阅管理端或租户 Topic         |
| 消息过期     | 支持公告有效期，过期后不再展示               |
| 前端展示     | 支持弹窗、角标、通知列表、声音提醒等展示方式 |

广播通知不适合发送用户隐私数据。如果消息只属于某个用户，应使用用户点对点消息，而不是在广播消息体中携带用户 ID 让前端自行过滤。

### 用户点对点消息

用户点对点消息用于向指定用户推送私有消息，例如个人通知、待办提醒、审批结果、单聊消息、任务处理结果等。客户端订阅 `/user/queue/**`，服务端通过用户 ID 或用户名发送消息。

推荐路径如下：

| 类型     | 订阅路径              | 说明                   |
| -------- | --------------------- | ---------------------- |
| 个人通知 | `/user/queue/notice`  | 站内通知、待办提醒     |
| 单聊消息 | `/user/queue/chat`    | 一对一聊天消息         |
| 任务结果 | `/user/queue/task`    | 异步任务完成通知       |
| 错误消息 | `/user/queue/errors`  | 当前用户操作异常       |
| 会话控制 | `/user/queue/session` | 强制下线、重新登录提醒 |

功能要求如下：

| 能力         | 说明                                     |
| ------------ | ---------------------------------------- |
| 用户身份绑定 | CONNECT 成功后必须绑定 Principal         |
| 多端推送     | 同一用户多个端同时在线时，可推送到全部端 |
| 指定端推送   | 支持只推送到当前 Session 或指定设备      |
| 离线补偿     | 用户不在线时消息落库，登录后补偿         |
| 消息回执     | 客户端收到或阅读后回传 ACK/READ          |
| 未读数维护   | 支持按用户维护未读数量                   |
| 幂等处理     | 使用消息 ID 避免重复展示                 |

点对点消息的关键是用户标识一致性。登录 Token 中的用户 ID、WebSocket Principal 名称、数据库用户主键、前端当前用户 ID 必须统一，否则容易出现消息发送成功但用户收不到的问题。

### 群组消息

群组消息用于聊天室、项目协作空间、组织频道、业务房间等多人通信场景。群组消息通常使用 `/topic/group/{groupId}` 或 `/topic/room/{roomId}` 作为订阅路径，服务端在发送和订阅阶段都需要校验用户是否属于该群组。

推荐路径如下：

| 类型     | 订阅路径                     | 说明               |
| -------- | ---------------------------- | ------------------ |
| 群组消息 | `/topic/group/{groupId}`     | 普通群聊或业务群   |
| 房间消息 | `/topic/room/{roomId}`       | 直播间、协作房间   |
| 项目空间 | `/topic/project/{projectId}` | 项目成员协作       |
| 组织频道 | `/topic/org/{orgId}`         | 部门或组织内部消息 |

功能要求如下：

| 能力         | 说明                             |
| ------------ | -------------------------------- |
| 群组成员校验 | 只有群组成员可以订阅和发送       |
| 群组在线人数 | 统计当前群组在线成员数量         |
| 入群退群事件 | 成员加入、退出时广播状态变更     |
| 群消息落库   | 聊天或重要业务消息需要保存       |
| 禁言控制     | 被禁言用户不能发送群组消息       |
| 踢人控制     | 管理员可移除群组成员并通知客户端 |
| 群消息回执   | 可选支持已读人数、未读人数统计   |

群组消息不建议直接信任前端传入的 `groupId`。服务端必须根据当前用户身份查询群组成员关系，校验通过后才允许订阅或发送。

### 在线状态通知

在线状态通知用于同步用户上线、下线、忙碌、离开、多端登录等状态。该功能通常和连接生命周期事件结合实现，在连接建立、断开、心跳超时、强制下线时更新状态并推送给相关订阅者。

推荐状态类型如下：

| 状态        | 说明         |
| ----------- | ------------ |
| `ONLINE`    | 在线         |
| `OFFLINE`   | 离线         |
| `BUSY`      | 忙碌         |
| `AWAY`      | 离开         |
| `INVISIBLE` | 隐身         |
| `KICKED`    | 被强制下线   |
| `EXPIRED`   | 登录状态过期 |

推荐路径如下：

| 类型           | 订阅路径                        | 说明                 |
| -------------- | ------------------------------- | -------------------- |
| 用户状态       | `/topic/user/status`            | 广播用户状态变更     |
| 群组成员状态   | `/topic/group/{groupId}/status` | 群内成员上下线       |
| 管理端在线状态 | `/topic/admin/online`           | 管理端查看在线用户   |
| 当前用户会话   | `/user/queue/session`           | 当前用户多端状态变化 |

功能要求如下：

| 能力         | 说明                                    |
| ------------ | --------------------------------------- |
| 连接事件监听 | 监听 CONNECT、DISCONNECT 等事件         |
| 多端状态合并 | 一个用户多个端在线时不能误判离线        |
| 心跳检测     | 通过心跳或超时机制识别异常断开          |
| Redis 同步   | 集群部署下同步各实例在线状态            |
| 状态广播     | 状态变化后通知好友、群组或管理端        |
| 状态持久化   | 可选记录最近在线时间、设备信息、IP 信息 |

在线状态不能只依赖客户端主动上报。浏览器关闭、网络中断、移动端切后台等情况可能不会正常发送离线消息，因此必须结合服务端断开事件和心跳超时处理。

### 业务事件推送

业务事件推送用于将后端业务系统中的状态变化实时通知到前端，例如订单支付成功、审批流转、任务完成、库存预警、设备状态变化等。该能力通常由业务服务产生事件，WebSocket 服务消费事件后完成推送。

推荐事件类型如下：

| 事件类型                 | 说明         |
| ------------------------ | ------------ |
| `ORDER_STATUS_CHANGED`   | 订单状态变更 |
| `APPROVAL_TODO_CREATED`  | 审批待办生成 |
| `TASK_PROGRESS_CHANGED`  | 任务进度变化 |
| `ALARM_CREATED`          | 告警产生     |
| `DEVICE_STATUS_CHANGED`  | 设备状态变化 |
| `MESSAGE_UNREAD_CHANGED` | 未读数变化   |

推荐处理流程如下：

```text
业务服务完成数据库事务
    |
    v
发布业务事件
    |
    v
WebSocket 服务消费事件
    |
    v
组装统一消息体
    |
    v
判断推送目标
    |
    +--> /user/queue/notice
    +--> /topic/group/{groupId}
    +--> /topic/dashboard/{type}
```

功能要求如下：

| 能力       | 说明                                     |
| ---------- | ---------------------------------------- |
| 事件标准化 | 所有业务事件使用统一字段结构             |
| 事务后推送 | 数据提交成功后再推送，避免脏通知         |
| 目标解析   | 根据用户、角色、租户、群组解析接收方     |
| 消息模板   | 不同事件使用不同通知标题和内容模板       |
| 失败记录   | 推送失败需要记录，便于补偿和排查         |
| 补偿查询   | 客户端断线后可通过 HTTP 查询最新业务状态 |

业务事件推送应遵循“事件驱动、推送解耦”的原则。业务服务不应直接操作 WebSocket 连接，而应发布标准事件，由实时消息模块统一处理。

### 聊天消息

聊天消息是 WebSocket STOMP 的典型双向通信场景，包括单聊、群聊、系统消息、消息撤回、已读回执、未读数、历史消息和离线消息补偿等能力。

推荐路径如下：

| 类型     | 路径                     | 说明                   |
| -------- | ------------------------ | ---------------------- |
| 发送单聊 | `/app/chat/private/send` | 客户端发送单聊消息     |
| 接收单聊 | `/user/queue/chat`       | 用户接收单聊消息       |
| 发送群聊 | `/app/chat/group/send`   | 客户端发送群聊消息     |
| 接收群聊 | `/topic/group/{groupId}` | 群成员接收群聊消息     |
| 消息回执 | `/app/chat/receipt`      | 客户端上报已送达或已读 |
| 聊天错误 | `/user/queue/errors`     | 返回发送失败原因       |

聊天消息建议落库后再推送。消息表中至少应包含消息 ID、会话 ID、发送人、接收人或群组 ID、消息类型、消息内容、发送时间、状态等字段。客户端收到消息后使用消息 ID 去重，避免断线重连或服务端重试导致重复展示。

功能要求如下：

| 能力       | 说明                                   |
| ---------- | -------------------------------------- |
| 单聊消息   | 指定用户接收，支持多端同步             |
| 群聊消息   | 群组成员接收，非成员不可订阅           |
| 消息持久化 | 支持历史消息查询和离线补偿             |
| 消息撤回   | 限制撤回时间窗口，广播撤回事件         |
| 已读回执   | 支持单聊已读、群聊已读人数             |
| 未读数     | 按会话维度统计未读数量                 |
| 敏感词过滤 | 发送前进行内容过滤                     |
| 消息防刷   | 限制高频发送和重复内容                 |
| 附件消息   | 文件走 HTTP 上传，WebSocket 推送元数据 |

聊天消息属于强业务场景，不能只做实时转发。即使服务端推送成功，也要考虑客户端未收到、用户离线、网络重连、重复消息、顺序错乱和历史补偿。

### 系统公告

系统公告用于向全体用户、指定租户、指定角色或指定组织发布通知。公告通常由管理端创建，可以立即推送，也可以定时发布。重要公告需要持久化并支持用户已读未读状态。

推荐路径如下：

| 类型         | 路径                              | 说明               |
| ------------ | --------------------------------- | ------------------ |
| 全站公告订阅 | `/topic/system/notice`            | 所有用户订阅       |
| 租户公告订阅 | `/topic/tenant/{tenantId}/notice` | 租户用户订阅       |
| 管理端发布   | `POST /api/admin/notices`         | HTTP 创建公告      |
| 公告推送     | 服务端内部调用                    | 创建成功后实时推送 |

功能要求如下：

| 能力     | 说明                             |
| -------- | -------------------------------- |
| 公告范围 | 支持全站、租户、角色、部门、用户 |
| 公告级别 | 普通、重要、紧急                 |
| 定时发布 | 支持指定发布时间                 |
| 已读未读 | 用户点击后记录已读状态           |
| 历史查询 | 支持公告列表和详情查询           |
| 弹窗策略 | 重要公告可前端弹窗展示           |
| 过期策略 | 过期公告不再主动推送             |

系统公告的实时推送只是触达手段，公告本身应保存到数据库。这样用户断线、离线、刷新页面后仍可以通过 HTTP 接口查询公告记录。

### 任务进度推送

任务进度推送用于后台异步任务执行过程中，向任务发起人或管理端实时反馈执行状态。例如文件导入、数据同步、批量处理、报表生成、模型分析、远程部署等场景。

推荐路径如下：

| 类型           | 路径                      | 说明                       |
| -------------- | ------------------------- | -------------------------- |
| 用户任务进度   | `/user/queue/task`        | 当前用户接收自己的任务进度 |
| 管理端任务看板 | `/topic/admin/task`       | 管理端查看全部任务状态     |
| 任务启动接口   | `POST /api/tasks`         | HTTP 创建异步任务          |
| 任务详情接口   | `GET /api/tasks/{taskId}` | HTTP 查询最终任务状态      |

推荐状态如下：

| 状态       | 说明     |
| ---------- | -------- |
| `PENDING`  | 等待执行 |
| `RUNNING`  | 执行中   |
| `SUCCESS`  | 执行成功 |
| `FAILED`   | 执行失败 |
| `CANCELED` | 已取消   |
| `TIMEOUT`  | 执行超时 |

任务进度消息示例：

```json
{
  "taskId": "task-100001",
  "status": "RUNNING",
  "progress": 65,
  "message": "正在处理第 6500 条数据",
  "timestamp": "2026-05-05T10:00:00+09:00"
}
```

功能要求如下：

| 能力         | 说明                               |
| ------------ | ---------------------------------- |
| 进度百分比   | 支持 0 到 100 的进度更新           |
| 阶段描述     | 支持当前执行阶段文本               |
| 失败原因     | 失败时返回明确错误摘要             |
| 最终状态落库 | 任务最终状态必须持久化             |
| 断线补偿     | 客户端重连后通过 HTTP 查询最新状态 |
| 推送节流     | 高频任务进度需要合并或限频推送     |

任务进度推送需要避免过高频率。对于循环处理类任务，不建议每处理一条数据都推送一次，可以按时间间隔或进度变化幅度推送，例如每 1 秒或每变化 5% 推送一次。

### 异常告警推送

异常告警推送用于将系统运行中的异常、业务风险、任务失败、接口错误、设备异常等实时推送给管理端或相关负责人。告警消息通常需要分级、持久化、确认和关闭。

推荐路径如下：

| 类型         | 路径                               | 说明                 |
| ------------ | ---------------------------------- | -------------------- |
| 管理端告警   | `/topic/admin/alarm`               | 管理端实时接收告警   |
| 用户告警     | `/user/queue/alarm`                | 指定负责人接收       |
| 告警创建接口 | 内部事件或 HTTP                    | 创建告警记录         |
| 告警处理接口 | `PUT /api/alarms/{alarmId}/status` | 确认、关闭、忽略告警 |

推荐告警级别如下：

| 级别       | 说明                   |
| ---------- | ---------------------- |
| `INFO`     | 普通提示               |
| `WARN`     | 警告，需要关注         |
| `ERROR`    | 错误，需要处理         |
| `CRITICAL` | 严重告警，需要立即处理 |

功能要求如下：

| 能力     | 说明                           |
| -------- | ------------------------------ |
| 告警分级 | 不同级别使用不同处理策略       |
| 告警聚合 | 相同告警短时间内合并，避免刷屏 |
| 告警确认 | 管理员确认后记录处理人和时间   |
| 告警关闭 | 问题恢复后关闭告警             |
| 定向通知 | 按负责人、角色、部门推送       |
| 审计记录 | 保存告警产生、确认、关闭全过程 |
| 告警降噪 | 对重复异常进行限频和聚合       |

异常告警推送不应直接把完整异常堆栈推给前端。前端消息体只保留摘要、级别、业务 ID、时间和追踪 ID，详细堆栈应进入日志系统，通过 traceId 关联排查。

### 管理端实时看板

管理端实时看板用于向后台管理页面推送连接数、在线用户数、消息发送量、推送失败率、任务执行状态、告警数量、业务统计等实时指标。看板通常面向管理员、运维人员或运营人员。

推荐路径如下：

| 类型     | 路径                                | 说明                 |
| -------- | ----------------------------------- | -------------------- |
| 连接看板 | `/topic/admin/dashboard/connection` | 在线连接、Session 数 |
| 消息看板 | `/topic/admin/dashboard/message`    | 消息发送量、失败量   |
| 告警看板 | `/topic/admin/dashboard/alarm`      | 告警数量、告警级别   |
| 任务看板 | `/topic/admin/dashboard/task`       | 任务状态统计         |
| 业务看板 | `/topic/admin/dashboard/biz`        | 业务指标实时刷新     |

看板数据来源可以包括应用内存统计、Redis 在线状态、数据库聚合、MQ 消费统计、Micrometer 指标和业务服务事件。对于实时性要求较高但精度要求不极端的看板，可以采用定时聚合后推送的方式，例如每 3 秒或 5 秒推送一次。

功能要求如下：

| 能力       | 说明                                    |
| ---------- | --------------------------------------- |
| 指标聚合   | 汇总连接数、在线用户数、消息量等指标    |
| 定时推送   | 固定周期向管理端推送看板数据            |
| 权限隔离   | 只有管理员可以订阅管理端 Topic          |
| 多实例汇总 | 集群下需要汇总所有实例指标              |
| 降频控制   | 避免过高频率刷新造成前端压力            |
| 异常展示   | 失败率、慢消息、Broker 异常需要突出展示 |
| 历史趋势   | 关键指标可落库或进入监控系统做趋势分析  |

管理端实时看板适合展示“当前状态”和“短周期变化”，不适合替代专业监控系统。生产环境建议同时接入 Prometheus、Grafana 和日志平台，看板负责业务可视化，监控系统负责指标留存、告警规则和问题追踪。


## 工程结构设计

工程结构设计用于约定 WebSocket STOMP 模块在 Spring Boot 3 项目中的依赖、包路径、配置类、控制器、消息处理器、服务层、事件监听器、拦截器和数据模型。结构设计的目标是让实时消息能力独立清晰，避免 WebSocket 相关代码散落在业务模块中，后续也便于扩展认证、限流、集群、监控和消息可靠性能力。

### Maven 依赖规划

Maven 依赖规划需要覆盖 WebSocket STOMP 基础能力、JSON 序列化、参数校验、日志、认证、缓存和工具类。基础接入阶段必须引入 `spring-boot-starter-websocket`，如果系统需要 HTTP API、登录接口、历史消息查询等能力，也需要保留 `spring-boot-starter-web`。

推荐依赖如下，适用于 Spring Boot 3.x 项目。

```xml
<dependencies>
    <!-- Spring Web：提供 HTTP 接口、Controller、过滤器、Servlet 容器等基础能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring WebSocket：提供 WebSocket、STOMP、消息代理、SimpMessagingTemplate 等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <!-- Spring Security：用于后续扩展连接认证、订阅鉴权、发送鉴权 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Validation：用于校验客户端发送的 STOMP 消息体 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Redis：用于在线状态、Session 映射、分布式同步、消息去重等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Hutool：用于字符串、集合、JSON、日期、ID、Bean 等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok：减少 DTO、VO、配置类、服务类中的样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：用于单元测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果当前阶段只做基础接入，可以暂时不启用 Redis 和 Security 的完整逻辑，但依赖规划上建议提前预留，避免后续补充在线状态、用户会话、限流和鉴权时大范围调整工程结构。

### 包结构设计

包结构设计建议将 WebSocket 相关能力独立放在 `websocket` 包下，同时保留 `common`、`security`、`system`、`chat` 等业务模块。WebSocket 模块不应直接包含所有业务逻辑，而是提供实时连接、消息接入、消息发送和事件分发能力。

推荐包结构如下：

```text
src/main/java/io/github/atengk
├── AtengApplication.java
├── common
│   ├── constant
│   ├── enums
│   ├── exception
│   ├── result
│   └── util
├── config
│   └── JacksonConfig.java
├── security
│   ├── model
│   └── service
├── websocket
│   ├── config
│   │   ├── WebSocketConfig.java
│   │   └── WebSocketProperties.java
│   ├── constant
│   │   └── WebSocketDestinationConstant.java
│   ├── controller
│   │   ├── ChatMessageController.java
│   │   └── NoticeMessageController.java
│   ├── dto
│   │   ├── ChatSendRequest.java
│   │   ├── NoticeSendRequest.java
│   │   └── TaskProgressMessage.java
│   ├── enums
│   │   ├── WebSocketMessageType.java
│   │   └── WebSocketMessageStatus.java
│   ├── event
│   │   └── StompSessionEventListener.java
│   ├── interceptor
│   │   └── StompAuthChannelInterceptor.java
│   ├── model
│   │   ├── StompUserPrincipal.java
│   │   └── WebSocketMessageEnvelope.java
│   └── service
│       ├── AuthUserService.java
│       ├── WebSocketPushService.java
│       └── impl
│           ├── AuthUserServiceImpl.java
│           └── WebSocketPushServiceImpl.java
└── system
    └── controller
```

各包职责如下：

| 包路径                  | 职责                                                       |
| ----------------------- | ---------------------------------------------------------- |
| `websocket.config`      | WebSocket STOMP Endpoint、Broker、前缀、心跳、线程池等配置 |
| `websocket.constant`    | 路由前缀、订阅路径、发送路径等常量                         |
| `websocket.controller`  | 处理客户端发送到 `/app/**` 的 STOMP 消息                   |
| `websocket.dto`         | 客户端请求体、服务端推送体、业务消息体                     |
| `websocket.enums`       | 消息类型、消息状态、事件类型、客户端类型等枚举             |
| `websocket.event`       | 连接、订阅、断开、异常等生命周期事件监听                   |
| `websocket.interceptor` | CONNECT、SUBSCRIBE、SEND、DISCONNECT 通道拦截              |
| `websocket.model`       | Principal、统一消息包装体、会话上下文等模型                |
| `websocket.service`     | 消息推送、用户认证、会话管理、在线状态等服务接口           |

### 配置类设计

配置类负责注册 STOMP Endpoint、配置消息代理、设置应用消息前缀、设置用户目标前缀、挂载通道拦截器，并提供心跳调度器。配置类应尽量只做框架配置，不放业务判断逻辑，业务鉴权应委托给拦截器或服务层。

推荐配置类包括：

| 配置类                | 职责                                        |
| --------------------- | ------------------------------------------- |
| `WebSocketProperties` | 读取 `application.yml` 中的 WebSocket 配置  |
| `WebSocketConfig`     | 注册 Endpoint、Broker、前缀、心跳、拦截器   |
| `JacksonConfig`       | 可选，统一配置时间格式、枚举序列化等        |
| `SecurityConfig`      | 可选，放行 WebSocket 握手路径或配置安全规则 |

配置属性类用于集中管理 WebSocket 相关配置。

文件位置：`src/main/java/io/github/atengk/websocket/config/WebSocketProperties.java`

```java
package io.github.atengk.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * WebSocket 配置属性
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@ConfigurationProperties(prefix = "app.websocket")
public class WebSocketProperties {

    /**
     * STOMP WebSocket 连接端点
     */
    private String endpoint = "/ws/stomp";

    /**
     * 允许跨域的来源规则
     */
    private List<String> allowedOriginPatterns = List.of("*");

    /**
     * 客户端发送到服务端业务处理器的前缀
     */
    private String applicationDestinationPrefix = "/app";

    /**
     * 用户点对点消息前缀
     */
    private String userDestinationPrefix = "/user";

    /**
     * Simple Broker 代理的目标前缀
     */
    private List<String> simpleBrokerDestinationPrefixes = List.of("/topic", "/queue");

    /**
     * 服务端向客户端发送心跳的间隔，单位毫秒
     */
    private long serverHeartbeat = 10000L;

    /**
     * 服务端期望客户端发送心跳的间隔，单位毫秒
     */
    private long clientHeartbeat = 10000L;

    /**
     * 是否启用 SockJS 兼容模式
     */
    private boolean sockJsEnabled = true;
}
```

### 控制器设计

WebSocket STOMP 控制器用于接收客户端发送到 `/app/**` 的消息。它和普通 HTTP Controller 不同，通常使用 `@Controller`、`@MessageMapping`、`@Payload`、`Principal` 等对象处理消息。

控制器设计原则如下：

| 原则              | 说明                                   |
| ----------------- | -------------------------------------- |
| 只接收 STOMP 消息 | 不负责 HTTP 查询和分页接口             |
| 不直接写复杂业务  | 复杂业务委托给 Service                 |
| 必须校验当前用户  | 从 `Principal` 或上下文中获取用户身份  |
| 必须校验请求体    | 使用 `@Validated` 和参数校验注解       |
| 不信任前端用户 ID | 发送人 ID 以服务端认证结果为准         |
| 统一返回或推送    | 通过服务层推送 ACK、错误消息、业务消息 |

聊天消息控制器示例如下。

文件位置：`src/main/java/io/github/atengk/websocket/controller/ChatMessageController.java`

```java
package io.github.atengk.websocket.controller;

import io.github.atengk.websocket.dto.ChatSendRequest;
import io.github.atengk.websocket.service.WebSocketPushService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 聊天消息控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final WebSocketPushService webSocketPushService;

    /**
     * 发送单聊消息
     *
     * @param request   发送请求
     * @param principal 当前连接用户
     */
    @MessageMapping("/chat/private/send")
    public void sendPrivateMessage(@Valid @Payload ChatSendRequest request, Principal principal) {
        String senderId = principal.getName();
        log.info("接收到单聊消息，发送人：{}，接收人：{}", senderId, request.getReceiverId());

        webSocketPushService.sendToUser(request.getReceiverId(), "/queue/chat", request);
    }

    /**
     * 发送群聊消息
     *
     * @param request   发送请求
     * @param principal 当前连接用户
     */
    @MessageMapping("/chat/group/send")
    public void sendGroupMessage(@Valid @Payload ChatSendRequest request, Principal principal) {
        String senderId = principal.getName();
        log.info("接收到群聊消息，发送人：{}，群组：{}", senderId, request.getGroupId());

        webSocketPushService.sendToGroup(request.getGroupId(), request);
    }
}
```

### 消息处理器设计

消息处理器负责处理客户端发来的具体业务消息。对于简单项目，可以直接使用 `@MessageMapping` 方法作为消息处理器；对于复杂项目，建议将消息处理逻辑从 Controller 下沉到独立 Handler 或 Service 中。

推荐拆分方式如下：

| 类型       | 示例                    | 职责                           |
| ---------- | ----------------------- | ------------------------------ |
| 连接处理器 | `ConnectMessageHandler` | 处理连接初始化上下文           |
| 聊天处理器 | `ChatMessageHandler`    | 处理单聊、群聊、撤回、回执     |
| 通知处理器 | `NoticeMessageHandler`  | 处理通知确认、已读、未读       |
| 任务处理器 | `TaskMessageHandler`    | 处理任务订阅、进度查询         |
| 错误处理器 | `WebSocketErrorHandler` | 处理消息异常并返回用户错误队列 |

消息处理器需要关注以下事项：

| 事项       | 说明                                   |
| ---------- | -------------------------------------- |
| 请求体校验 | 防止空消息、超长消息、非法类型         |
| 权限校验   | 校验当前用户是否可发送到目标用户或群组 |
| 消息 ID    | 为消息生成唯一 ID，便于幂等和追踪      |
| 持久化     | 聊天、通知、告警等重要消息应落库       |
| 推送目标   | 根据业务规则选择用户、群组或广播       |
| 异常处理   | 业务失败应推送到 `/user/queue/errors`  |

### 服务层设计

服务层用于封装服务端消息发送能力，避免业务代码直接散落调用 `SimpMessagingTemplate`。建议提供统一的 `WebSocketPushService`，集中处理广播、点对点、群组、错误消息、任务进度等推送。

推荐服务接口如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/WebSocketPushService.java`

```java
package io.github.atengk.websocket.service;

/**
 * WebSocket 消息推送服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WebSocketPushService {

    /**
     * 广播消息
     *
     * @param destination 目标地址
     * @param payload     消息内容
     */
    void broadcast(String destination, Object payload);

    /**
     * 向指定用户发送消息
     *
     * @param userId      用户 ID
     * @param destination 用户队列地址
     * @param payload     消息内容
     */
    void sendToUser(String userId, String destination, Object payload);

    /**
     * 向指定群组发送消息
     *
     * @param groupId 群组 ID
     * @param payload 消息内容
     */
    void sendToGroup(String groupId, Object payload);
}
```

服务实现类统一封装 `SimpMessagingTemplate` 的调用，并对目标地址和用户 ID 做基础校验。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WebSocketPushServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 消息推送服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushServiceImpl implements WebSocketPushService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 广播消息
     *
     * @param destination 目标地址
     * @param payload     消息内容
     */
    @Override
    public void broadcast(String destination, Object payload) {
        if (StrUtil.isBlank(destination)) {
            log.warn("广播消息失败，目标地址为空");
            return;
        }
        messagingTemplate.convertAndSend(destination, payload);
        log.info("广播消息发送成功，目标地址：{}", destination);
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId      用户 ID
     * @param destination 用户队列地址
     * @param payload     消息内容
     */
    @Override
    public void sendToUser(String userId, String destination, Object payload) {
        if (StrUtil.hasBlank(userId, destination)) {
            log.warn("用户消息发送失败，用户ID或目标地址为空，用户ID：{}，目标地址：{}", userId, destination);
            return;
        }
        messagingTemplate.convertAndSendToUser(userId, destination, payload);
        log.info("用户消息发送成功，用户ID：{}，目标地址：{}", userId, destination);
    }

    /**
     * 向指定群组发送消息
     *
     * @param groupId 群组 ID
     * @param payload 消息内容
     */
    @Override
    public void sendToGroup(String groupId, Object payload) {
        if (StrUtil.isBlank(groupId)) {
            log.warn("群组消息发送失败，群组ID为空");
            return;
        }
        String destination = StrUtil.format("/topic/group/{}", groupId);
        messagingTemplate.convertAndSend(destination, payload);
        log.info("群组消息发送成功，群组ID：{}，目标地址：{}", groupId, destination);
    }
}
```

### 事件监听器设计

事件监听器用于处理 WebSocket STOMP 生命周期事件，例如连接、连接成功、订阅、取消订阅、断开等。它适合做日志记录、在线状态更新、Session 注册、异常排查和管理端连接看板统计。

常用事件如下：

| 事件                      | 说明                     |
| ------------------------- | ------------------------ |
| `SessionConnectEvent`     | 客户端发送 STOMP CONNECT |
| `SessionConnectedEvent`   | STOMP 连接建立成功       |
| `SessionSubscribeEvent`   | 客户端订阅目标地址       |
| `SessionUnsubscribeEvent` | 客户端取消订阅           |
| `SessionDisconnectEvent`  | WebSocket/STOMP 连接断开 |

事件监听器示例如下。

文件位置：`src/main/java/io/github/atengk/websocket/event/StompSessionEventListener.java`

```java
package io.github.atengk.websocket.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

/**
 * STOMP 会话事件监听器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class StompSessionEventListener {

    /**
     * 监听客户端 CONNECT 请求
     *
     * @param event 连接事件
     */
    @EventListener
    public void onSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("收到 STOMP CONNECT 请求，sessionId：{}", accessor.getSessionId());
    }

    /**
     * 监听 STOMP 连接成功事件
     *
     * @param event 连接成功事件
     */
    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        log.info("STOMP 连接成功，sessionId：{}，user：{}", accessor.getSessionId(), user == null ? "anonymous" : user.getName());
    }

    /**
     * 监听订阅事件
     *
     * @param event 订阅事件
     */
    @EventListener
    public void onSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        log.info("STOMP 订阅成功，sessionId：{}，user：{}，destination：{}",
                accessor.getSessionId(),
                user == null ? "anonymous" : user.getName(),
                accessor.getDestination());
    }

    /**
     * 监听连接断开事件
     *
     * @param event 断开事件
     */
    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        log.info("STOMP 连接断开，sessionId：{}，user：{}，closeStatus：{}",
                accessor.getSessionId(),
                user == null ? "anonymous" : user.getName(),
                event.getCloseStatus());
    }
}
```

### 拦截器设计

拦截器用于在 STOMP 入站通道上处理认证、鉴权、目标地址校验、Header 解析、黑名单、限流等逻辑。相比事件监听器，拦截器可以在消息真正进入业务处理前直接拦截非法请求。

常用拦截点如下：

| STOMP 命令   | 典型处理                   |
| ------------ | -------------------------- |
| `CONNECT`    | 解析 Token，绑定 Principal |
| `SUBSCRIBE`  | 校验订阅路径权限           |
| `SEND`       | 校验发送路径权限和消息频率 |
| `DISCONNECT` | 清理上下文或记录日志       |

下面示例展示 CONNECT 认证、SUBSCRIBE 订阅路径校验和 SEND 发送路径校验的基础写法。真实项目中应替换 `AuthUserService` 的 Token 解析逻辑。

文件位置：`src/main/java/io/github/atengk/websocket/interceptor/StompAuthChannelInterceptor.java`

```java
package io.github.atengk.websocket.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.model.StompUserPrincipal;
import io.github.atengk.websocket.service.AuthUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * STOMP 认证与鉴权通道拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final AuthUserService authUserService;

    /**
     * 消息发送前拦截
     *
     * @param message 消息对象
     * @param channel 消息通道
     * @return 处理后的消息
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        }

        return message;
    }

    /**
     * 处理 CONNECT 认证
     *
     * @param accessor STOMP Header 访问器
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String token = getFirstNativeHeader(accessor, "Authorization");
        if (StrUtil.isBlank(token)) {
            token = getFirstNativeHeader(accessor, "token");
        }

        String userId = authUserService.parseUserId(token);
        if (StrUtil.isBlank(userId)) {
            log.warn("STOMP 连接认证失败，sessionId：{}", accessor.getSessionId());
            throw new AccessDeniedException("WebSocket 连接认证失败");
        }

        accessor.setUser(new StompUserPrincipal(userId));
        log.info("STOMP 连接认证成功，sessionId：{}，userId：{}", accessor.getSessionId(), userId);
    }

    /**
     * 处理订阅鉴权
     *
     * @param accessor STOMP Header 访问器
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (StrUtil.isBlank(destination)) {
            throw new AccessDeniedException("订阅地址不能为空");
        }

        if (StrUtil.startWith(destination, "/topic/admin") && !authUserService.isAdmin(accessor.getUser())) {
            log.warn("管理端订阅被拒绝，user：{}，destination：{}", accessor.getUser(), destination);
            throw new AccessDeniedException("无管理端订阅权限");
        }

        log.info("STOMP 订阅校验通过，user：{}，destination：{}", accessor.getUser(), destination);
    }

    /**
     * 处理发送鉴权
     *
     * @param accessor STOMP Header 访问器
     */
    private void handleSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (StrUtil.isBlank(destination)) {
            throw new AccessDeniedException("发送地址不能为空");
        }

        if (!StrUtil.startWith(destination, "/app")) {
            log.warn("非法发送地址，user：{}，destination：{}", accessor.getUser(), destination);
            throw new AccessDeniedException("非法发送地址");
        }
    }

    /**
     * 获取第一个原生 Header
     *
     * @param accessor STOMP Header 访问器
     * @param name     Header 名称
     * @return Header 值
     */
    private String getFirstNativeHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        return values.get(0);
    }
}
```

### 数据模型设计

数据模型设计需要区分“传输消息体”和“数据库实体”。基础接入阶段建议先定义统一消息包装体，后续聊天、通知、任务、告警等业务消息都可以放入 `data` 字段中。

统一消息体建议包含以下字段：

| 字段        | 类型            | 说明                           |
| ----------- | --------------- | ------------------------------ |
| `messageId` | `String`        | 消息唯一 ID，用于幂等和追踪    |
| `type`      | `String`        | 消息类型，例如聊天、通知、告警 |
| `status`    | `String`        | 消息状态                       |
| `data`      | `T`             | 业务数据                       |
| `timestamp` | `LocalDateTime` | 服务端生成时间                 |
| `traceId`   | `String`        | 链路追踪 ID，可选              |
| `version`   | `String`        | 消息协议版本                   |

统一消息包装体示例如下。

文件位置：`src/main/java/io/github/atengk/websocket/model/WebSocketMessageEnvelope.java`

```java
package io.github.atengk.websocket.model;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.websocket.enums.WebSocketMessageStatus;
import io.github.atengk.websocket.enums.WebSocketMessageType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebSocket 统一消息包装体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class WebSocketMessageEnvelope<T> {

    /**
     * 消息唯一 ID
     */
    private String messageId;

    /**
     * 消息类型
     */
    private WebSocketMessageType type;

    /**
     * 消息状态
     */
    private WebSocketMessageStatus status;

    /**
     * 业务数据
     */
    private T data;

    /**
     * 消息时间
     */
    private LocalDateTime timestamp;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 协议版本
     */
    private String version;

    /**
     * 构建成功消息
     *
     * @param type 消息类型
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 消息包装体
     */
    public static <T> WebSocketMessageEnvelope<T> success(WebSocketMessageType type, T data) {
        WebSocketMessageEnvelope<T> envelope = new WebSocketMessageEnvelope<>();
        envelope.setMessageId(IdUtil.fastSimpleUUID());
        envelope.setType(type);
        envelope.setStatus(WebSocketMessageStatus.SUCCESS);
        envelope.setData(data);
        envelope.setTimestamp(LocalDateTime.now());
        envelope.setVersion("1.0");
        return envelope;
    }
}
```

聊天发送请求示例如下。

文件位置：`src/main/java/io/github/atengk/websocket/dto/ChatSendRequest.java`

```java
package io.github.atengk.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天消息发送请求
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class ChatSendRequest {

    /**
     * 接收人 ID，单聊时必填
     */
    private String receiverId;

    /**
     * 群组 ID，群聊时必填
     */
    private String groupId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过 2000 个字符")
    private String content;
}
```

### 常量与枚举设计

常量与枚举用于统一管理路由路径、消息类型、消息状态等固定值，避免代码中大量硬编码字符串。WebSocket 路由尤其需要集中管理，否则前后端联调时容易出现路径不一致、订阅失败或用户消息收不到的问题。

推荐常量类如下。

文件位置：`src/main/java/io/github/atengk/websocket/constant/WebSocketDestinationConstant.java`

```java
package io.github.atengk.websocket.constant;

/**
 * WebSocket 目标地址常量
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class WebSocketDestinationConstant {

    /**
     * 系统公告广播地址
     */
    public static final String TOPIC_SYSTEM_NOTICE = "/topic/system/notice";

    /**
     * 管理端告警广播地址
     */
    public static final String TOPIC_ADMIN_ALARM = "/topic/admin/alarm";

    /**
     * 管理端看板地址
     */
    public static final String TOPIC_ADMIN_DASHBOARD = "/topic/admin/dashboard";

    /**
     * 用户通知队列地址
     */
    public static final String USER_QUEUE_NOTICE = "/queue/notice";

    /**
     * 用户聊天队列地址
     */
    public static final String USER_QUEUE_CHAT = "/queue/chat";

    /**
     * 用户错误队列地址
     */
    public static final String USER_QUEUE_ERRORS = "/queue/errors";

    private WebSocketDestinationConstant() {
    }
}
```

消息类型枚举如下。

文件位置：`src/main/java/io/github/atengk/websocket/enums/WebSocketMessageType.java`

```java
package io.github.atengk.websocket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 消息类型
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@RequiredArgsConstructor
public enum WebSocketMessageType {

    /**
     * 系统公告
     */
    SYSTEM_NOTICE("系统公告"),

    /**
     * 用户通知
     */
    USER_NOTICE("用户通知"),

    /**
     * 单聊消息
     */
    PRIVATE_CHAT("单聊消息"),

    /**
     * 群聊消息
     */
    GROUP_CHAT("群聊消息"),

    /**
     * 任务进度
     */
    TASK_PROGRESS("任务进度"),

    /**
     * 异常告警
     */
    ALARM("异常告警"),

    /**
     * 错误消息
     */
    ERROR("错误消息");

    private final String description;
}
```

消息状态枚举如下。

文件位置：`src/main/java/io/github/atengk/websocket/enums/WebSocketMessageStatus.java`

```java
package io.github.atengk.websocket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 消息状态
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@RequiredArgsConstructor
public enum WebSocketMessageStatus {

    /**
     * 成功
     */
    SUCCESS("成功"),

    /**
     * 失败
     */
    FAILED("失败"),

    /**
     * 待确认
     */
    PENDING_ACK("待确认"),

    /**
     * 已送达
     */
    DELIVERED("已送达"),

    /**
     * 已读
     */
    READ("已读");

    private final String description;
}
```

## WebSocket STOMP 基础接入

基础接入阶段的目标是让前端能够建立 STOMP 连接，完成订阅、发送和接收消息。该阶段不要求一次性实现完整聊天、离线消息、集群同步和消息可靠性，但必须把 Endpoint、Broker、应用前缀、用户前缀、跨域、SockJS、心跳和基础认证扩展点设计好。

### 依赖配置

基础接入至少需要 `spring-boot-starter-websocket`。如果项目使用 Spring Security，需要注意放行 WebSocket 握手地址，STOMP 帧级别的认证通常放在 `ChannelInterceptor` 中处理。

基础依赖如下：

```xml
<dependencies>
    <!-- HTTP API 与基础 Web 容器 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- WebSocket 与 STOMP 支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <!-- 参数校验，用于校验 @Payload 请求体 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool 工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok 简化样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

基础配置文件建议统一放在 `application.yml` 中，后续多环境可以通过 `application-dev.yml`、`application-prod.yml` 覆盖。

```yaml
app:
  websocket:
    # STOMP WebSocket 连接端点，前端连接地址示例：ws://localhost:8080/ws/stomp
    endpoint: /ws/stomp

    # 开发环境可以放开，生产环境建议配置明确域名
    allowed-origin-patterns:
      - "http://localhost:5173"
      - "http://localhost:3000"
      - "https://*.example.com"

    # 客户端发送到服务端 @MessageMapping 的前缀
    application-destination-prefix: /app

    # 用户点对点消息前缀，客户端订阅时使用 /user/queue/**
    user-destination-prefix: /user

    # Simple Broker 管理的订阅前缀
    simple-broker-destination-prefixes:
      - /topic
      - /queue

    # 服务端向客户端发送心跳间隔，单位毫秒
    server-heartbeat: 10000

    # 服务端期望客户端发送心跳间隔，单位毫秒
    client-heartbeat: 10000

    # 是否启用 SockJS 兼容模式
    sock-js-enabled: true
```

### WebSocket 配置类

WebSocket 配置类是基础接入的核心文件，负责启用 STOMP 消息代理、注册连接端点、配置 Broker 前缀、配置用户目的地前缀、配置心跳任务调度器，并挂载入站通道拦截器。

文件位置：`src/main/java/io/github/atengk/websocket/config/WebSocketConfig.java`

```java
package io.github.atengk.websocket.config;

import io.github.atengk.websocket.interceptor.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket STOMP 配置类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@EnableConfigurationProperties(WebSocketProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties properties;

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    /**
     * 注册 STOMP Endpoint
     *
     * @param registry Endpoint 注册器
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = properties.getAllowedOriginPatterns().toArray(new String[0]);

        if (properties.isSockJsEnabled()) {
            registry.addEndpoint(properties.getEndpoint())
                    .setAllowedOriginPatterns(allowedOrigins)
                    .withSockJS();
            return;
        }

        registry.addEndpoint(properties.getEndpoint())
                .setAllowedOriginPatterns(allowedOrigins);
    }

    /**
     * 配置消息代理
     *
     * @param registry 消息代理注册器
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        List<String> brokerPrefixes = properties.getSimpleBrokerDestinationPrefixes();

        registry.enableSimpleBroker(brokerPrefixes.toArray(new String[0]))
                .setHeartbeatValue(new long[]{
                        properties.getServerHeartbeat(),
                        properties.getClientHeartbeat()
                })
                .setTaskScheduler(webSocketHeartbeatTaskScheduler());

        registry.setApplicationDestinationPrefixes(properties.getApplicationDestinationPrefix());
        registry.setUserDestinationPrefix(properties.getUserDestinationPrefix());
    }

    /**
     * 配置客户端入站通道
     *
     * @param registration 通道注册器
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    /**
     * WebSocket 心跳任务调度器
     *
     * @return 任务调度器
     */
    @Bean
    public ThreadPoolTaskScheduler webSocketHeartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        return scheduler;
    }
}
```

该配置完成后，客户端可以连接 `/ws/stomp`，向 `/app/**` 发送消息，订阅 `/topic/**` 广播消息，订阅 `/user/queue/**` 用户私有消息。

### STOMP Endpoint 配置

STOMP Endpoint 是客户端建立 WebSocket 连接的入口，不是业务消息发送地址。前端连接 Endpoint 后，才会继续发送 STOMP `CONNECT`、`SUBSCRIBE`、`SEND` 等帧。

推荐 Endpoint 命名如下：

| Endpoint           | 说明                              |
| ------------------ | --------------------------------- |
| `/ws/stomp`        | 推荐默认地址，语义明确            |
| `/websocket/stomp` | 更长但更容易识别                  |
| `/socket`          | 简短，但语义不如 `/ws/stomp` 明确 |

生产项目不建议把 Endpoint 设计成业务路径，例如 `/chat`、`/notice`。WebSocket 连接端点应保持统一，具体业务通过 STOMP destination 区分。

示例：

```text
WebSocket Endpoint:
ws://localhost:8080/ws/stomp

STOMP 发送地址:
 /app/chat/private/send
 /app/chat/group/send

STOMP 订阅地址:
 /topic/system/notice
 /topic/group/{groupId}
 /user/queue/notice
 /user/queue/chat
```

### 跨域配置

前后端分离项目中，前端页面域名和后端接口域名通常不同，因此 WebSocket Endpoint 需要配置允许的来源。开发环境可以放开到本地端口，生产环境必须配置明确域名或受控的通配规则。

配置示例：

```yaml
app:
  websocket:
    # 生产环境不要直接使用 "*"，应配置可信域名
    allowed-origin-patterns:
      - "https://admin.example.com"
      - "https://app.example.com"
      - "https://*.example.com"
```

跨域配置注意事项如下：

| 注意项      | 说明                                                      |
| ----------- | --------------------------------------------------------- |
| 开发环境    | 可以允许 `http://localhost:5173`、`http://localhost:3000` |
| 测试环境    | 配置测试域名，例如 `https://test.example.com`             |
| 生产环境    | 只允许正式业务域名，不建议使用 `*`                        |
| Cookie 认证 | 需要额外关注 SameSite、Secure、域名和 HTTPS               |
| 网关转发    | 确认网关没有丢失 `Origin` Header                          |

跨域只解决“是否允许来源连接”的问题，不等于认证鉴权。即使来源合法，仍然需要在 `CONNECT`、`SUBSCRIBE`、`SEND` 阶段校验用户身份和权限。

### SockJS 配置

SockJS 用于兼容不支持原生 WebSocket 的浏览器或受限网络环境。现代浏览器通常可以直接使用 WebSocket，但在企业内网、旧浏览器、代理限制等场景下，SockJS 仍然有实用价值。

启用方式已经在 `WebSocketConfig` 中通过 `withSockJS()` 完成。配置项如下：

```yaml
app:
  websocket:
    # true 表示启用 SockJS，前端需要使用 sockjs-client 创建连接
    sock-js-enabled: true
```

启用 SockJS 后，前端连接方式通常不是直接传入 `ws://`，而是使用 HTTP 地址创建 SockJS 对象：

```javascript
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws/stomp'),
  connectHeaders: {
    Authorization: 'Bearer your-token'
  },
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000
})

client.activate()
```

如果项目明确只支持现代浏览器和 App WebView，也可以关闭 SockJS，直接使用原生 WebSocket：

```yaml
app:
  websocket:
    # false 表示不启用 SockJS，前端可直接使用 ws:// 或 wss://
    sock-js-enabled: false
```

### 消息代理配置

消息代理用于处理客户端订阅和服务端推送。基础接入阶段推荐先使用 Spring 内置 Simple Broker，代理 `/topic` 和 `/queue` 两类目的地。

当前配置如下：

```java
registry.enableSimpleBroker("/topic", "/queue");
registry.setApplicationDestinationPrefixes("/app");
registry.setUserDestinationPrefix("/user");
```

含义如下：

| 配置                                        | 说明                                                  |
| ------------------------------------------- | ----------------------------------------------------- |
| `enableSimpleBroker("/topic", "/queue")`    | 启用内置 Simple Broker，处理订阅和推送                |
| `setApplicationDestinationPrefixes("/app")` | 客户端发送到 `/app/**` 的消息路由到 `@MessageMapping` |
| `setUserDestinationPrefix("/user")`         | 启用用户目的地，支持 `/user/queue/**`                 |

基础消息流如下：

```text
客户端 SEND /app/chat/private/send
    |
    v
@MessageMapping("/chat/private/send")
    |
    v
SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/chat", payload)
    |
    v
客户端订阅 /user/queue/chat 后收到消息
```

Simple Broker 适合开发环境、单体应用和中小规模实时推送。如果需要多实例共享订阅关系、跨节点消息分发或更强可靠性，应在后续章节切换到 RabbitMQ 或 ActiveMQ 的 STOMP Broker Relay。

### 应用消息前缀配置

应用消息前缀用于区分“客户端发送给服务端业务处理器”的消息。推荐使用 `/app` 作为统一前缀。

配置如下：

```java
registry.setApplicationDestinationPrefixes("/app");
```

客户端发送路径和服务端处理方法的对应关系如下：

| 客户端发送路径           | 服务端方法                              |
| ------------------------ | --------------------------------------- |
| `/app/chat/private/send` | `@MessageMapping("/chat/private/send")` |
| `/app/chat/group/send`   | `@MessageMapping("/chat/group/send")`   |
| `/app/notice/read`       | `@MessageMapping("/notice/read")`       |
| `/app/task/subscribe`    | `@MessageMapping("/task/subscribe")`    |

需要注意，`@MessageMapping` 中不需要写 `/app` 前缀。`/app` 只存在于客户端发送路径中，Spring 在路由到 Controller 时会自动去掉该前缀。

推荐命名规则如下：

```text
/app/{模块}/{动作}
/app/chat/private/send
/app/chat/group/send
/app/notice/read
/app/task/progress/query
/app/session/kick
```

### 用户目标前缀配置

用户目标前缀用于支持用户点对点消息。推荐使用 `/user` 作为用户目标前缀，客户端订阅 `/user/queue/**`，服务端发送时使用 `convertAndSendToUser(userId, "/queue/**", payload)`。

配置如下：

```java
registry.setUserDestinationPrefix("/user");
```

客户端订阅路径如下：

```text
/user/queue/notice
/user/queue/chat
/user/queue/task
/user/queue/errors
```

服务端发送方式如下：

```java
messagingTemplate.convertAndSendToUser(userId, "/queue/notice", payload);
messagingTemplate.convertAndSendToUser(userId, "/queue/chat", payload);
messagingTemplate.convertAndSendToUser(userId, "/queue/errors", payload);
```

这里容易出现一个常见错误：服务端发送时不要写成 `convertAndSendToUser(userId, "/user/queue/notice", payload)`。第二个参数应是用户目的地下的实际队列地址，例如 `/queue/notice`。客户端订阅时才使用完整路径 `/user/queue/notice`。

用户身份需要通过 `Principal#getName()` 与 `convertAndSendToUser` 的 `userId` 保持一致。下面是基础 Principal 模型。

文件位置：`src/main/java/io/github/atengk/websocket/model/StompUserPrincipal.java`

```java
package io.github.atengk.websocket.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

/**
 * STOMP 用户身份
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@RequiredArgsConstructor
public class StompUserPrincipal implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识
     */
    private final String name;
}
```

### 心跳配置

心跳用于检测连接是否仍然可用，避免浏览器异常关闭、网络中断、代理断开后服务端长时间保留无效连接。STOMP 心跳由客户端和服务端协商完成，服务端配置心跳值后，客户端也需要配置对应的 incoming 和 outgoing 心跳。

服务端配置如下：

```java
registry.enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[]{10000, 10000})
        .setTaskScheduler(webSocketHeartbeatTaskScheduler());
```

含义如下：

| 配置值          | 说明                                     |
| --------------- | ---------------------------------------- |
| 第一个 `10000`  | 服务端向客户端发送心跳的间隔，单位毫秒   |
| 第二个 `10000`  | 服务端期望客户端发送心跳的间隔，单位毫秒 |
| `TaskScheduler` | Simple Broker 发送心跳需要任务调度器     |

前端配置示例：

```javascript
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws/stomp',
  connectHeaders: {
    Authorization: 'Bearer your-token'
  },
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000
})
```

心跳配置建议如下：

| 场景           | 建议值                     |
| -------------- | -------------------------- |
| 本地开发       | 10000 到 30000 毫秒        |
| 普通管理后台   | 10000 到 30000 毫秒        |
| 高频聊天系统   | 10000 到 20000 毫秒        |
| 移动端弱网环境 | 20000 到 60000 毫秒        |
| 大连接数系统   | 不宜过短，需要结合压测调整 |

心跳不是业务在线状态的唯一依据。生产环境中，在线状态应结合连接事件、断开事件、心跳超时、Redis 会话过期和客户端重连补偿共同判断。


## 消息路由设计

消息路由设计用于统一约定客户端发送、服务端接收、广播订阅、用户订阅、群组订阅、系统通知和管理端订阅的路径规则。WebSocket STOMP 项目中，路由命名必须稳定、清晰、可鉴权，否则前后端联调、权限控制、消息排查和集群扩展都会变得困难。

### 客户端发送路径

客户端发送路径是指前端通过 STOMP `SEND` 命令发送到服务端业务处理器的地址。该类路径必须以应用消息前缀 `/app` 开头，随后进入服务端 `@MessageMapping` 方法处理。

推荐格式如下：

```text
/app/{业务模块}/{业务动作}
/app/chat/private/send
/app/chat/group/send
/app/notice/read
/app/task/progress/query
/app/session/kick
/app/online/status/change
```

常用客户端发送路径如下：

| 业务场景     | 客户端发送路径              | 服务端处理说明       |
| ------------ | --------------------------- | -------------------- |
| 发送单聊消息 | `/app/chat/private/send`    | 发送给指定用户       |
| 发送群聊消息 | `/app/chat/group/send`      | 发送到指定群组       |
| 消息已读回执 | `/app/chat/read`            | 上报消息已读状态     |
| 通知已读     | `/app/notice/read`          | 标记通知已读         |
| 查询任务进度 | `/app/task/progress/query`  | 查询或订阅任务进度   |
| 修改在线状态 | `/app/online/status/change` | 用户主动切换在线状态 |
| 管理端踢人   | `/app/admin/session/kick`   | 管理员踢出指定连接   |
| 推送测试     | `/app/admin/push/test`      | 管理端测试消息推送   |

需要注意，客户端发送路径中的 `/app` 不会出现在 `@MessageMapping` 注解中。Spring 会去掉应用前缀后再匹配处理方法。

```java
@MessageMapping("/chat/private/send")
public void sendPrivateMessage(...) {
    // 对应客户端发送路径：/app/chat/private/send
}
```

### 服务端接收路径

服务端接收路径由 `@MessageMapping` 定义，用于接收客户端发送到 `/app/**` 的 STOMP 消息。它只处理“客户端主动发起”的消息，不处理服务端主动推送。

服务端接收路径建议按业务模块拆分，不建议把所有消息都放到一个 `@MessageMapping("/send")` 中。

推荐映射关系如下：

| 客户端路径                  | 服务端 `@MessageMapping` | 方法职责     |
| --------------------------- | ------------------------ | ------------ |
| `/app/chat/private/send`    | `/chat/private/send`     | 处理单聊发送 |
| `/app/chat/group/send`      | `/chat/group/send`       | 处理群聊发送 |
| `/app/chat/ack`             | `/chat/ack`              | 处理聊天 ACK |
| `/app/notice/read`          | `/notice/read`           | 处理通知已读 |
| `/app/task/progress/query`  | `/task/progress/query`   | 查询任务进度 |
| `/app/online/status/change` | `/online/status/change`  | 修改在线状态 |
| `/app/admin/session/kick`   | `/admin/session/kick`    | 管理端踢人   |

服务端接收路径设计原则如下：

| 原则                | 说明                                        |
| ------------------- | ------------------------------------------- |
| 必须以业务模块开头  | 例如 `chat`、`notice`、`task`、`admin`      |
| 必须体现动作        | 例如 `send`、`read`、`ack`、`query`、`kick` |
| 不暴露内部实现      | 路径不应包含数据库表名、类名、MQ Topic 名   |
| 不信任请求体用户 ID | 当前用户以 `Principal#getName()` 为准       |
| 需要配合鉴权        | `SEND` 阶段必须校验路径权限                 |

### 广播订阅路径

广播订阅路径用于多个客户端订阅同一个主题，由服务端向该主题推送消息。广播路径通常使用 `/topic/**`，适用于系统公告、业务广播、实时看板、公共事件等场景。

推荐格式如下：

```text
/topic/{范围}/{业务模块}
/topic/system/notice
/topic/dashboard/order
/topic/biz/order
/topic/tenant/{tenantId}/notice
```

常用广播订阅路径如下：

| 场景     | 订阅路径                          | 说明             |
| -------- | --------------------------------- | ---------------- |
| 系统公告 | `/topic/system/notice`            | 全站或大范围公告 |
| 订单看板 | `/topic/dashboard/order`          | 订单统计实时刷新 |
| 告警广播 | `/topic/system/alarm`             | 系统级告警       |
| 租户通知 | `/topic/tenant/{tenantId}/notice` | 指定租户内广播   |
| 业务事件 | `/topic/biz/{bizType}`            | 指定业务类型事件 |
| 在线状态 | `/topic/online/status`            | 在线状态变化广播 |

广播路径必须关注数据范围。全局广播路径中不要携带用户隐私、租户敏感数据或管理端敏感数据。涉及权限隔离的数据，应拆分为租户、角色、群组或用户路径，并在 `SUBSCRIBE` 阶段做权限校验。

### 用户订阅路径

用户订阅路径用于当前用户接收自己的私有消息。客户端订阅时使用 `/user/queue/**`，服务端发送时使用 `convertAndSendToUser(userId, "/queue/**", payload)`。

推荐客户端订阅路径如下：

```text
/user/queue/notice
/user/queue/chat
/user/queue/task
/user/queue/errors
/user/queue/session
/user/queue/ack
```

用户订阅路径建议如下：

| 场景     | 客户端订阅路径        | 服务端发送目标   |
| -------- | --------------------- | ---------------- |
| 个人通知 | `/user/queue/notice`  | `/queue/notice`  |
| 单聊消息 | `/user/queue/chat`    | `/queue/chat`    |
| 任务进度 | `/user/queue/task`    | `/queue/task`    |
| 错误消息 | `/user/queue/errors`  | `/queue/errors`  |
| 会话控制 | `/user/queue/session` | `/queue/session` |
| ACK 回执 | `/user/queue/ack`     | `/queue/ack`     |

服务端发送示例：

```java
messagingTemplate.convertAndSendToUser(userId, "/queue/notice", payload);
messagingTemplate.convertAndSendToUser(userId, "/queue/chat", payload);
messagingTemplate.convertAndSendToUser(userId, "/queue/errors", payload);
```

用户订阅路径最常见的问题是服务端发送目标写错。客户端订阅完整路径 `/user/queue/notice`，服务端发送时第二个参数应写 `/queue/notice`，不要重复拼接 `/user`。

### 群组订阅路径

群组订阅路径用于聊天室、项目空间、业务房间、组织频道等多人协作场景。推荐使用 `/topic/group/{groupId}` 或按具体业务拆分，例如 `/topic/project/{projectId}`、`/topic/room/{roomId}`。

推荐格式如下：

```text
/topic/group/{groupId}
/topic/room/{roomId}
/topic/project/{projectId}
/topic/org/{orgId}
```

常用群组订阅路径如下：

| 场景     | 订阅路径                        | 说明               |
| -------- | ------------------------------- | ------------------ |
| 普通群聊 | `/topic/group/{groupId}`        | 群成员接收群聊消息 |
| 房间消息 | `/topic/room/{roomId}`          | 房间成员接收消息   |
| 项目空间 | `/topic/project/{projectId}`    | 项目成员协作消息   |
| 组织频道 | `/topic/org/{orgId}`            | 部门或组织内部消息 |
| 群组状态 | `/topic/group/{groupId}/status` | 群成员在线状态     |
| 群组公告 | `/topic/group/{groupId}/notice` | 群内公告           |

群组路径必须在订阅阶段校验权限。不能仅依赖前端控制订阅按钮是否显示，因为任何客户端都可以构造 STOMP `SUBSCRIBE` 帧尝试订阅非法目标。

校验规则建议如下：

| 校验项         | 说明                             |
| -------------- | -------------------------------- |
| 群组是否存在   | 不存在则拒绝订阅                 |
| 用户是否为成员 | 非成员拒绝订阅                   |
| 用户是否被禁用 | 被踢出、被封禁、被禁言时限制操作 |
| 群组是否解散   | 解散后拒绝订阅和发送             |
| 租户是否一致   | 多租户场景必须校验租户隔离       |

### 系统通知路径

系统通知路径用于发布系统公告、版本更新、维护通知、业务提醒、待办消息、审批通知等消息。系统通知可能是广播，也可能是用户点对点消息，需要根据通知范围选择不同路径。

推荐路径如下：

| 通知类型 | 路径                        | 说明               |
| -------- | --------------------------- | ------------------ |
| 全站公告 | `/topic/system/notice`      | 全体在线用户可订阅 |
| 系统告警 | `/topic/system/alarm`       | 系统级别告警       |
| 用户通知 | `/user/queue/notice`        | 指定用户通知       |
| 待办提醒 | `/user/queue/todo`          | 指定用户待办       |
| 审批通知 | `/user/queue/approval`      | 指定审批人         |
| 维护通知 | `/topic/system/maintenance` | 维护窗口公告       |

系统通知建议分为两类：

| 类型     | 推送方式           | 示例                         |
| -------- | ------------------ | ---------------------------- |
| 公共通知 | `/topic/system/**` | 系统维护、版本发布、全站公告 |
| 私有通知 | `/user/queue/**`   | 审批待办、订单提醒、任务结果 |

系统通知通常需要持久化，尤其是待办、审批、业务提醒和重要公告。WebSocket 只负责实时触达，最终通知列表、已读未读状态和历史记录仍应通过数据库和 HTTP API 支撑。

### 管理端订阅路径

管理端订阅路径用于后台管理页面接收实时连接、在线用户、消息量、异常告警、任务状态、业务看板等数据。管理端路径建议统一以 `/topic/admin/**` 开头，并在订阅阶段强制校验管理员权限。

推荐路径如下：

```text
/topic/admin/connection
/topic/admin/online
/topic/admin/session
/topic/admin/alarm
/topic/admin/dashboard/message
/topic/admin/dashboard/task
/topic/admin/dashboard/biz
```

常用管理端订阅路径如下：

| 场景     | 订阅路径                         | 说明                     |
| -------- | -------------------------------- | ------------------------ |
| 在线连接 | `/topic/admin/connection`        | 当前连接数、Session 数   |
| 在线用户 | `/topic/admin/online`            | 在线用户列表变化         |
| 会话监控 | `/topic/admin/session`           | 连接建立、断开、踢人事件 |
| 异常告警 | `/topic/admin/alarm`             | 实时异常或业务告警       |
| 消息看板 | `/topic/admin/dashboard/message` | 发送量、失败量、延迟     |
| 任务看板 | `/topic/admin/dashboard/task`    | 异步任务状态             |
| 业务看板 | `/topic/admin/dashboard/biz`     | 业务统计数据             |

管理端路径必须与普通用户路径隔离。建议在 `ChannelInterceptor` 的 `SUBSCRIBE` 阶段判断目标地址是否以 `/topic/admin` 开头，如果是，则校验当前用户是否具备管理员、运维或对应业务角色。

### 路由命名规范

路由命名规范用于保证前后端、服务端、测试用例和文档中的路径保持一致。路径一旦被前端接入或外部系统使用，应视为接口契约，不能随意修改。

推荐命名规则如下：

| 规则                               | 示例                           | 说明               |
| ---------------------------------- | ------------------------------ | ------------------ |
| 应用发送统一使用 `/app`            | `/app/chat/private/send`       | 客户端发送到服务端 |
| 广播订阅统一使用 `/topic`          | `/topic/system/notice`         | 多客户端订阅       |
| 用户私有订阅统一使用 `/user/queue` | `/user/queue/notice`           | 当前用户私有消息   |
| 管理端统一使用 `/topic/admin`      | `/topic/admin/alarm`           | 管理端权限隔离     |
| 群组统一使用业务 ID                | `/topic/group/{groupId}`       | 便于鉴权和排查     |
| 动词放在末尾                       | `/app/notice/read`             | 模块在前，动作在后 |
| 路径使用小写                       | `/app/chat/private/send`       | 避免大小写不一致   |
| 不使用中文路径                     | 不推荐 `/app/聊天/发送`        | 避免编码和兼容问题 |
| 不暴露内部类名                     | 不推荐 `/app/chatService/send` | 避免和实现耦合     |

建议集中维护路由常量，避免散落硬编码。

文件位置：`src/main/java/io/github/atengk/websocket/constant/WebSocketRouteConstant.java`

```java
package io.github.atengk.websocket.constant;

/**
 * WebSocket 路由常量
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class WebSocketRouteConstant {

    /**
     * 应用消息前缀
     */
    public static final String APP_PREFIX = "/app";

    /**
     * 广播主题前缀
     */
    public static final String TOPIC_PREFIX = "/topic";

    /**
     * 用户目标前缀
     */
    public static final String USER_PREFIX = "/user";

    /**
     * 用户队列前缀
     */
    public static final String QUEUE_PREFIX = "/queue";

    /**
     * 系统公告广播
     */
    public static final String TOPIC_SYSTEM_NOTICE = "/topic/system/notice";

    /**
     * 系统告警广播
     */
    public static final String TOPIC_SYSTEM_ALARM = "/topic/system/alarm";

    /**
     * 管理端告警
     */
    public static final String TOPIC_ADMIN_ALARM = "/topic/admin/alarm";

    /**
     * 管理端消息看板
     */
    public static final String TOPIC_ADMIN_DASHBOARD_MESSAGE = "/topic/admin/dashboard/message";

    /**
     * 用户通知队列，服务端 convertAndSendToUser 使用
     */
    public static final String QUEUE_NOTICE = "/queue/notice";

    /**
     * 用户聊天队列，服务端 convertAndSendToUser 使用
     */
    public static final String QUEUE_CHAT = "/queue/chat";

    /**
     * 用户错误队列，服务端 convertAndSendToUser 使用
     */
    public static final String QUEUE_ERRORS = "/queue/errors";

    /**
     * 用户 ACK 队列，服务端 convertAndSendToUser 使用
     */
    public static final String QUEUE_ACK = "/queue/ack";

    private WebSocketRouteConstant() {
    }
}
```

## 消息模型设计

消息模型设计用于统一 WebSocket STOMP 的消息结构，避免不同业务各自定义完全不同的 JSON 格式。统一消息模型可以提升前端解析、消息去重、日志追踪、协议升级、错误处理和跨端兼容能力。

### 基础消息体

基础消息体是所有 WebSocket 推送消息的外层包装结构。它不直接表达具体业务，而是提供消息 ID、类型、状态、版本、时间、追踪 ID、业务数据等通用字段。

推荐基础结构如下：

| 字段        | 类型     | 必填 | 说明                                  |
| ----------- | -------- | ---- | ------------------------------------- |
| `messageId` | `String` | 是   | 消息唯一 ID，用于幂等、去重和日志追踪 |
| `type`      | `String` | 是   | 消息类型                              |
| `status`    | `String` | 是   | 消息状态                              |
| `version`   | `String` | 是   | 消息协议版本                          |
| `timestamp` | `Long`   | 是   | 服务端生成时间戳，单位毫秒            |
| `traceId`   | `String` | 否   | 链路追踪 ID                           |
| `data`      | `T`      | 否   | 业务消息体                            |

基础消息体示例：

```json
{
  "messageId": "1909550919275921408",
  "type": "SYSTEM_NOTICE",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "traceId": "trace-20260505-000001",
  "data": {
    "title": "系统维护通知",
    "content": "系统将在今晚 23:00 进行维护"
  }
}
```

基础消息体 Java 模型如下，作为所有服务端推送消息的统一外层包装。

文件位置：`src/main/java/io/github/atengk/websocket/model/WsMessage.java`

```java
package io.github.atengk.websocket.model;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.websocket.enums.WsMessageStatus;
import io.github.atengk.websocket.enums.WsMessageType;
import lombok.Data;

/**
 * WebSocket 基础消息体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class WsMessage<T> {

    /**
     * 消息唯一 ID
     */
    private String messageId;

    /**
     * 消息类型
     */
    private WsMessageType type;

    /**
     * 消息状态
     */
    private WsMessageStatus status;

    /**
     * 消息协议版本
     */
    private String version;

    /**
     * 服务端时间戳，单位毫秒
     */
    private Long timestamp;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 业务数据
     */
    private T data;

    public static <T> WsMessage<T> success(WsMessageType type, T data) {
        WsMessage<T> message = new WsMessage<>();
        message.setMessageId(String.valueOf(IdUtil.getSnowflakeNextId()));
        message.setType(type);
        message.setStatus(WsMessageStatus.SUCCESS);
        message.setVersion(WsMessageVersion.V1);
        message.setTimestamp(DateUtil.current());
        message.setData(data);
        return message;
    }

    public static <T> WsMessage<T> failed(WsMessageType type, T data) {
        WsMessage<T> message = new WsMessage<>();
        message.setMessageId(String.valueOf(IdUtil.getSnowflakeNextId()));
        message.setType(type);
        message.setStatus(WsMessageStatus.FAILED);
        message.setVersion(WsMessageVersion.V1);
        message.setTimestamp(DateUtil.current());
        message.setData(data);
        return message;
    }
}
```

### 业务消息体

业务消息体用于承载来自订单、审批、任务、设备、告警等业务模块的事件。它强调业务 ID、业务类型、事件类型、接收方和摘要信息，适合由业务事件转换为 WebSocket 推送消息。

推荐字段如下：

| 字段           | 类型                  | 说明                                      |
| -------------- | --------------------- | ----------------------------------------- |
| `bizId`        | `String`              | 业务主键，例如订单 ID、任务 ID            |
| `bizType`      | `String`              | 业务类型，例如 `ORDER`、`TASK`            |
| `eventType`    | `String`              | 事件类型，例如 `ORDER_PAID`               |
| `receiverType` | `String`              | 接收方类型，例如 `USER`、`GROUP`、`TOPIC` |
| `receiverIds`  | `List<String>`        | 接收方 ID 列表                            |
| `title`        | `String`              | 消息标题                                  |
| `content`      | `String`              | 消息内容                                  |
| `extra`        | `Map<String, Object>` | 扩展字段                                  |

业务消息体模型如下。

文件位置：`src/main/java/io/github/atengk/websocket/dto/BizEventMessage.java`

```java
package io.github.atengk.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 业务事件消息体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class BizEventMessage {

    /**
     * 业务 ID
     */
    @NotBlank(message = "业务ID不能为空")
    private String bizId;

    /**
     * 业务类型
     */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    /**
     * 事件类型
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 接收方类型
     */
    private String receiverType;

    /**
     * 接收方 ID 列表
     */
    private List<String> receiverIds;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 扩展数据
     */
    private Map<String, Object> extra;
}
```

业务消息体示例：

```json
{
  "bizId": "order-100001",
  "bizType": "ORDER",
  "eventType": "ORDER_STATUS_CHANGED",
  "receiverType": "USER",
  "receiverIds": ["10001", "10002"],
  "title": "订单状态变更",
  "content": "订单已完成支付",
  "extra": {
    "orderStatus": "PAID",
    "amount": 199.00
  }
}
```

### 聊天消息体

聊天消息体用于单聊、群聊、系统消息、消息撤回、已读回执等聊天场景。聊天消息必须具备消息 ID、会话 ID、发送人、接收人或群组 ID、消息内容、消息类型和发送时间。

推荐字段如下：

| 字段          | 类型     | 说明                            |
| ------------- | -------- | ------------------------------- |
| `chatId`      | `String` | 会话 ID                         |
| `senderId`    | `String` | 发送人 ID，以服务端认证用户为准 |
| `receiverId`  | `String` | 单聊接收人 ID                   |
| `groupId`     | `String` | 群聊群组 ID                     |
| `content`     | `String` | 消息内容                        |
| `contentType` | `String` | 文本、图片、文件、语音等        |
| `sequence`    | `Long`   | 会话内递增序号，可用于排序      |
| `sentAt`      | `Long`   | 发送时间戳                      |

聊天消息体模型如下。

文件位置：`src/main/java/io/github/atengk/websocket/dto/ChatMessage.java`

```java
package io.github.atengk.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天消息体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class ChatMessage {

    /**
     * 会话 ID
     */
    private String chatId;

    /**
     * 发送人 ID
     */
    private String senderId;

    /**
     * 单聊接收人 ID
     */
    private String receiverId;

    /**
     * 群组 ID
     */
    private String groupId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过 2000 个字符")
    private String content;

    /**
     * 内容类型
     */
    private String contentType;

    /**
     * 会话内消息序号
     */
    private Long sequence;

    /**
     * 发送时间戳
     */
    private Long sentAt;
}
```

单聊消息示例：

```json
{
  "messageId": "1909550919275921408",
  "type": "PRIVATE_CHAT",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "chatId": "private-10001-10002",
    "senderId": "10001",
    "receiverId": "10002",
    "content": "你好，这是一条单聊消息",
    "contentType": "TEXT",
    "sequence": 10001,
    "sentAt": 1777956000000
  }
}
```

群聊消息示例：

```json
{
  "messageId": "1909550919275921409",
  "type": "GROUP_CHAT",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "chatId": "group-90001",
    "senderId": "10001",
    "groupId": "90001",
    "content": "大家好，这是一条群聊消息",
    "contentType": "TEXT",
    "sequence": 30001,
    "sentAt": 1777956000000
  }
}
```

### 通知消息体

通知消息体用于系统公告、待办提醒、审批通知、订单通知、告警通知等场景。通知消息通常需要标题、内容、级别、跳转地址、业务 ID、是否需要确认等字段。

推荐字段如下：

| 字段       | 类型      | 说明         |
| ---------- | --------- | ------------ |
| `noticeId` | `String`  | 通知 ID      |
| `title`    | `String`  | 通知标题     |
| `content`  | `String`  | 通知内容     |
| `level`    | `String`  | 通知级别     |
| `bizId`    | `String`  | 关联业务 ID  |
| `bizType`  | `String`  | 关联业务类型 |
| `url`      | `String`  | 前端跳转地址 |
| `needAck`  | `Boolean` | 是否需要 ACK |
| `expireAt` | `Long`    | 过期时间戳   |

通知消息体模型如下。

文件位置：`src/main/java/io/github/atengk/websocket/dto/NoticeMessage.java`

```java
package io.github.atengk.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通知消息体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class NoticeMessage {

    /**
     * 通知 ID
     */
    private String noticeId;

    /**
     * 通知标题
     */
    @NotBlank(message = "通知标题不能为空")
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知级别
     */
    private String level;

    /**
     * 业务 ID
     */
    private String bizId;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 前端跳转地址
     */
    private String url;

    /**
     * 是否需要 ACK
     */
    private Boolean needAck;

    /**
     * 过期时间戳
     */
    private Long expireAt;
}
```

通知消息示例：

```json
{
  "messageId": "1909550919275921410",
  "type": "USER_NOTICE",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "noticeId": "notice-100001",
    "title": "审批待办提醒",
    "content": "你有一条新的采购审批待处理",
    "level": "IMPORTANT",
    "bizId": "approval-90001",
    "bizType": "APPROVAL",
    "url": "/approval/detail/90001",
    "needAck": true,
    "expireAt": 1778042400000
  }
}
```

### 错误消息体

错误消息体用于向客户端返回认证失败、授权失败、参数错误、业务异常、目标地址非法、消息格式错误等问题。错误消息一般发送到 `/user/queue/errors`，只返回给当前用户，不应广播到公共主题。

推荐字段如下：

| 字段               | 类型      | 说明             |
| ------------------ | --------- | ---------------- |
| `code`             | `String`  | 错误码           |
| `message`          | `String`  | 用户可读错误信息 |
| `destination`      | `String`  | 出错目标地址     |
| `requestMessageId` | `String`  | 原始请求消息 ID  |
| `traceId`          | `String`  | 链路追踪 ID      |
| `retryable`        | `Boolean` | 是否可重试       |

错误消息体模型如下。

文件位置：`src/main/java/io/github/atengk/websocket/dto/WsErrorMessage.java`

```java
package io.github.atengk.websocket.dto;

import lombok.Data;

/**
 * WebSocket 错误消息体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class WsErrorMessage {

    /**
     * 错误码
     */
    private String code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 出错目标地址
     */
    private String destination;

    /**
     * 原始请求消息 ID
     */
    private String requestMessageId;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 是否可重试
     */
    private Boolean retryable;
}
```

错误消息示例：

```json
{
  "messageId": "1909550919275921411",
  "type": "ERROR",
  "status": "FAILED",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "code": "WS_SUBSCRIBE_DENIED",
    "message": "无权限订阅该群组",
    "destination": "/topic/group/90001",
    "requestMessageId": "client-msg-001",
    "traceId": "trace-20260505-000002",
    "retryable": false
  }
}
```

错误消息设计注意事项如下：

| 注意项         | 说明                                        |
| -------------- | ------------------------------------------- |
| 不返回敏感信息 | 不把 Token、SQL、堆栈、服务器路径返回给前端 |
| 错误码稳定     | 前端可以根据错误码做交互处理                |
| 错误信息可读   | `message` 面向用户或前端开发者              |
| 记录 traceId   | 便于后端日志定位                            |
| 区分可重试     | 网络抖动可重试，权限错误不可重试            |

### ACK 消息体

ACK 消息体用于消息确认、送达确认、已读确认和业务处理确认。它可以由客户端发送到服务端，也可以由服务端返回给客户端。

推荐字段如下：

| 字段        | 类型      | 说明                                           |
| ----------- | --------- | ---------------------------------------------- |
| `ackId`     | `String`  | ACK ID                                         |
| `messageId` | `String`  | 被确认的消息 ID                                |
| `ackType`   | `String`  | ACK 类型，例如 `RECEIVED`、`READ`、`PROCESSED` |
| `success`   | `Boolean` | 是否确认成功                                   |
| `reason`    | `String`  | 失败原因                                       |
| `ackAt`     | `Long`    | 确认时间戳                                     |

ACK 消息体模型如下。

文件位置：`src/main/java/io/github/atengk/websocket/dto/AckMessage.java`

```java
package io.github.atengk.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ACK 消息体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class AckMessage {

    /**
     * ACK ID
     */
    private String ackId;

    /**
     * 被确认的消息 ID
     */
    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    /**
     * ACK 类型
     */
    @NotBlank(message = "ACK类型不能为空")
    private String ackType;

    /**
     * 是否确认成功
     */
    private Boolean success;

    /**
     * 失败原因
     */
    private String reason;

    /**
     * 确认时间戳
     */
    private Long ackAt;
}
```

ACK 消息示例：

```json
{
  "messageId": "1909550919275921412",
  "type": "ACK",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "ackId": "ack-100001",
    "messageId": "1909550919275921408",
    "ackType": "READ",
    "success": true,
    "ackAt": 1777956000000
  }
}
```

ACK 类型建议如下：

| ACK 类型    | 说明                 |
| ----------- | -------------------- |
| `RECEIVED`  | 客户端已收到消息     |
| `READ`      | 用户已读消息         |
| `PROCESSED` | 客户端已处理业务动作 |
| `FAILED`    | 客户端处理失败       |
| `TIMEOUT`   | 客户端确认超时       |

### 在线状态消息体

在线状态消息体用于用户上线、下线、忙碌、离开、隐身、多端登录、强制下线等场景。该消息既可以广播给群组成员，也可以推送给管理端或用户本人。

推荐字段如下：

| 字段         | 类型     | 说明                          |
| ------------ | -------- | ----------------------------- |
| `userId`     | `String` | 用户 ID                       |
| `status`     | `String` | 在线状态                      |
| `sessionId`  | `String` | 当前连接 Session ID           |
| `clientType` | `String` | 客户端类型，例如 `WEB`、`APP` |
| `deviceId`   | `String` | 设备 ID                       |
| `ip`         | `String` | 客户端 IP                     |
| `onlineAt`   | `Long`   | 上线时间                      |
| `offlineAt`  | `Long`   | 下线时间                      |
| `reason`     | `String` | 状态变化原因                  |

在线状态消息体模型如下。

文件位置：`src/main/java/io/github/atengk/websocket/dto/OnlineStatusMessage.java`

```java
package io.github.atengk.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 在线状态消息体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class OnlineStatusMessage {

    /**
     * 用户 ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 在线状态
     */
    @NotBlank(message = "在线状态不能为空")
    private String status;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 客户端类型
     */
    private String clientType;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 客户端 IP
     */
    private String ip;

    /**
     * 上线时间
     */
    private Long onlineAt;

    /**
     * 下线时间
     */
    private Long offlineAt;

    /**
     * 状态变化原因
     */
    private String reason;
}
```

在线状态消息示例：

```json
{
  "messageId": "1909550919275921413",
  "type": "ONLINE_STATUS",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "userId": "10001",
    "status": "ONLINE",
    "sessionId": "ws-session-001",
    "clientType": "WEB",
    "deviceId": "browser-chrome-001",
    "ip": "192.168.1.10",
    "onlineAt": 1777956000000,
    "reason": "CONNECT_SUCCESS"
  }
}
```

在线状态在多端登录场景下不能简单地按 Session 断开就判定用户离线。应先判断该用户是否还有其他有效 Session，只有全部 Session 断开或过期后，才将用户状态改为离线。

### 消息类型枚举

消息类型枚举用于前后端识别消息的业务含义。前端可以根据 `type` 决定消息进入聊天窗口、通知中心、告警面板、任务进度条还是在线状态组件。

推荐消息类型如下：

| 类型            | 说明     |
| --------------- | -------- |
| `SYSTEM_NOTICE` | 系统公告 |
| `USER_NOTICE`   | 用户通知 |
| `PRIVATE_CHAT`  | 单聊消息 |
| `GROUP_CHAT`    | 群聊消息 |
| `TASK_PROGRESS` | 任务进度 |
| `ALARM`         | 异常告警 |
| `ACK`           | ACK 回执 |
| `ERROR`         | 错误消息 |
| `ONLINE_STATUS` | 在线状态 |

消息类型枚举如下。

文件位置：`src/main/java/io/github/atengk/websocket/enums/WsMessageType.java`

```java
package io.github.atengk.websocket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 消息类型枚举
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@RequiredArgsConstructor
public enum WsMessageType {

    /**
     * 系统公告
     */
    SYSTEM_NOTICE("系统公告"),

    /**
     * 用户通知
     */
    USER_NOTICE("用户通知"),

    /**
     * 单聊消息
     */
    PRIVATE_CHAT("单聊消息"),

    /**
     * 群聊消息
     */
    GROUP_CHAT("群聊消息"),

    /**
     * 业务事件
     */
    BIZ_EVENT("业务事件"),

    /**
     * 任务进度
     */
    TASK_PROGRESS("任务进度"),

    /**
     * 异常告警
     */
    ALARM("异常告警"),

    /**
     * ACK 回执
     */
    ACK("ACK回执"),

    /**
     * 错误消息
     */
    ERROR("错误消息"),

    /**
     * 在线状态
     */
    ONLINE_STATUS("在线状态");

    private final String description;
}
```

### 消息状态枚举

消息状态枚举用于描述消息在发送、确认、处理、失败、过期等过程中的状态。状态字段不等同于业务状态，它只描述 WebSocket 消息本身的处理状态。

推荐状态如下：

| 状态          | 说明           |
| ------------- | -------------- |
| `SUCCESS`     | 消息处理成功   |
| `FAILED`      | 消息处理失败   |
| `PENDING_ACK` | 等待客户端确认 |
| `DELIVERED`   | 已送达         |
| `READ`        | 已读           |
| `RETRYING`    | 重试中         |
| `EXPIRED`     | 已过期         |
| `CANCELED`    | 已取消         |

消息状态枚举如下。

文件位置：`src/main/java/io/github/atengk/websocket/enums/WsMessageStatus.java`

```java
package io.github.atengk.websocket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 消息状态枚举
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@RequiredArgsConstructor
public enum WsMessageStatus {

    /**
     * 成功
     */
    SUCCESS("成功"),

    /**
     * 失败
     */
    FAILED("失败"),

    /**
     * 等待 ACK
     */
    PENDING_ACK("等待ACK"),

    /**
     * 已送达
     */
    DELIVERED("已送达"),

    /**
     * 已读
     */
    READ("已读"),

    /**
     * 重试中
     */
    RETRYING("重试中"),

    /**
     * 已过期
     */
    EXPIRED("已过期"),

    /**
     * 已取消
     */
    CANCELED("已取消");

    private final String description;
}
```

### 消息版本字段

消息版本字段用于支持协议演进。WebSocket 是长连接通信，前端、后端、移动端、管理端可能不会同时升级，因此消息体中应保留 `version` 字段，便于兼容旧客户端。

版本字段建议放在基础消息体外层：

```json
{
  "messageId": "1909550919275921414",
  "type": "USER_NOTICE",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "title": "通知标题",
    "content": "通知内容"
  }
}
```

版本常量如下。

文件位置：`src/main/java/io/github/atengk/websocket/model/WsMessageVersion.java`

```java
package io.github.atengk.websocket.model;

/**
 * WebSocket 消息协议版本
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class WsMessageVersion {

    /**
     * 初始版本
     */
    public static final String V1 = "1.0";

    /**
     * 扩展版本，预留用于后续协议升级
     */
    public static final String V2 = "2.0";

    private WsMessageVersion() {
    }
}
```

消息版本升级建议如下：

| 场景           | 处理方式                           |
| -------------- | ---------------------------------- |
| 新增可选字段   | 小版本内兼容，不影响旧客户端       |
| 删除字段       | 不建议直接删除，先标记废弃         |
| 修改字段含义   | 应升级大版本                       |
| 修改枚举值     | 需要前后端同步兼容                 |
| 新增消息类型   | 前端默认忽略未知类型               |
| 多端版本不一致 | 服务端可根据客户端版本返回不同结构 |

协议演进时，应保证旧客户端收到未知字段不会异常，新客户端收到旧版本消息也能降级处理。前端解析消息时建议按 `type + version` 做分发，未知类型进入默认日志或忽略逻辑，避免影响主流程。


## 服务端消息发送

服务端消息发送用于将业务系统产生的事件、通知、聊天消息、任务进度、告警信息等推送给在线客户端。Spring WebSocket STOMP 中推荐通过 `SimpMessagingTemplate` 统一发送消息，并在项目中封装业务级推送服务，避免 Controller、Service、Listener、定时任务中直接散落调用底层模板对象。

### 广播消息发送

广播消息发送用于向订阅同一 `/topic/**` 主题的所有客户端推送消息。典型场景包括系统公告、实时看板、公共告警、全站通知、租户通知等。

推荐广播路径如下：

| 场景       | 推送地址                         | 客户端订阅地址                   |
| ---------- | -------------------------------- | -------------------------------- |
| 系统公告   | `/topic/system/notice`           | `/topic/system/notice`           |
| 系统告警   | `/topic/system/alarm`            | `/topic/system/alarm`            |
| 管理端看板 | `/topic/admin/dashboard/message` | `/topic/admin/dashboard/message` |
| 订单看板   | `/topic/dashboard/order`         | `/topic/dashboard/order`         |

广播发送示例：

```java
messagingTemplate.convertAndSend("/topic/system/notice", message);
```

建议将广播能力封装到统一服务中，不要在业务代码中直接写硬编码路径。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsPushService.java`

```java
package io.github.atengk.websocket.service;

import java.time.Duration;
import java.util.Collection;

/**
 * WebSocket 消息推送服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsPushService {

    /**
     * 广播消息
     *
     * @param destination 广播目标地址
     * @param payload     消息内容
     */
    void broadcast(String destination, Object payload);

    /**
     * 向指定用户发送消息
     *
     * @param userId      用户 ID
     * @param destination 用户队列地址
     * @param payload     消息内容
     */
    void sendToUser(String userId, String destination, Object payload);

    /**
     * 向指定会话发送消息
     *
     * @param userId      用户 ID
     * @param sessionId   会话 ID
     * @param destination 用户队列地址
     * @param payload     消息内容
     */
    void sendToSession(String userId, String sessionId, String destination, Object payload);

    /**
     * 向指定群组发送消息
     *
     * @param groupId 群组 ID
     * @param payload 消息内容
     */
    void sendToGroup(String groupId, Object payload);

    /**
     * 延迟发送消息
     *
     * @param destination 目标地址
     * @param payload     消息内容
     * @param delay       延迟时间
     */
    void sendDelay(String destination, Object payload, Duration delay);

    /**
     * 批量向用户发送消息
     *
     * @param userIds     用户 ID 集合
     * @param destination 用户队列地址
     * @param payload     消息内容
     */
    void sendToUsers(Collection<String> userIds, String destination, Object payload);

    /**
     * 当前事务提交后发送消息
     *
     * @param destination 目标地址
     * @param payload     消息内容
     */
    void sendAfterCommit(String destination, Object payload);

    /**
     * 异步发送消息
     *
     * @param destination 目标地址
     * @param payload     消息内容
     */
    void sendAsync(String destination, Object payload);
}
```

该实现类统一封装广播、指定用户、指定会话、群组、延迟、批量、事务后和异步推送逻辑。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsPushServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.WsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

/**
 * WebSocket 消息推送服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsPushServiceImpl implements WsPushService {

    private final SimpMessagingTemplate messagingTemplate;

    private final TaskScheduler webSocketTaskScheduler;

    /**
     * 广播消息
     *
     * @param destination 广播目标地址
     * @param payload     消息内容
     */
    @Override
    public void broadcast(String destination, Object payload) {
        if (StrUtil.isBlank(destination)) {
            log.warn("广播消息发送失败，目标地址为空");
            return;
        }
        messagingTemplate.convertAndSend(destination, payload);
        log.info("广播消息发送成功，目标地址：{}", destination);
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId      用户 ID
     * @param destination 用户队列地址
     * @param payload     消息内容
     */
    @Override
    public void sendToUser(String userId, String destination, Object payload) {
        if (StrUtil.hasBlank(userId, destination)) {
            log.warn("用户消息发送失败，用户ID或目标地址为空，用户ID：{}，目标地址：{}", userId, destination);
            return;
        }
        messagingTemplate.convertAndSendToUser(userId, destination, payload);
        log.info("用户消息发送成功，用户ID：{}，目标地址：{}", userId, destination);
    }

    /**
     * 向指定会话发送消息
     *
     * @param userId      用户 ID
     * @param sessionId   会话 ID
     * @param destination 用户队列地址
     * @param payload     消息内容
     */
    @Override
    public void sendToSession(String userId, String sessionId, String destination, Object payload) {
        if (StrUtil.hasBlank(userId, sessionId, destination)) {
            log.warn("会话消息发送失败，用户ID、会话ID或目标地址为空，用户ID：{}，sessionId：{}，目标地址：{}",
                    userId, sessionId, destination);
            return;
        }

        MessageHeaders headers = createSessionHeaders(sessionId);
        messagingTemplate.convertAndSendToUser(userId, destination, payload, headers);
        log.info("会话消息发送成功，用户ID：{}，sessionId：{}，目标地址：{}", userId, sessionId, destination);
    }

    /**
     * 向指定群组发送消息
     *
     * @param groupId 群组 ID
     * @param payload 消息内容
     */
    @Override
    public void sendToGroup(String groupId, Object payload) {
        if (StrUtil.isBlank(groupId)) {
            log.warn("群组消息发送失败，群组ID为空");
            return;
        }

        String destination = StrUtil.format("/topic/group/{}", groupId);
        messagingTemplate.convertAndSend(destination, payload);
        log.info("群组消息发送成功，群组ID：{}，目标地址：{}", groupId, destination);
    }

    /**
     * 延迟发送消息
     *
     * @param destination 目标地址
     * @param payload     消息内容
     * @param delay       延迟时间
     */
    @Override
    public void sendDelay(String destination, Object payload, Duration delay) {
        if (StrUtil.isBlank(destination) || Objects.isNull(delay) || delay.isNegative()) {
            log.warn("延迟消息发送失败，参数非法，目标地址：{}，延迟时间：{}", destination, delay);
            return;
        }

        webSocketTaskScheduler.schedule(() -> {
            messagingTemplate.convertAndSend(destination, payload);
            log.info("延迟消息发送成功，目标地址：{}，延迟时间：{}ms", destination, delay.toMillis());
        }, Instant.now().plus(delay));
    }

    /**
     * 批量向用户发送消息
     *
     * @param userIds     用户 ID 集合
     * @param destination 用户队列地址
     * @param payload     消息内容
     */
    @Override
    public void sendToUsers(Collection<String> userIds, String destination, Object payload) {
        if (CollUtil.isEmpty(userIds) || StrUtil.isBlank(destination)) {
            log.warn("批量用户消息发送失败，用户集合为空或目标地址为空，目标地址：{}", destination);
            return;
        }

        userIds.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .forEach(userId -> messagingTemplate.convertAndSendToUser(userId, destination, payload));

        log.info("批量用户消息发送完成，用户数量：{}，目标地址：{}", userIds.size(), destination);
    }

    /**
     * 当前事务提交后发送消息
     *
     * @param destination 目标地址
     * @param payload     消息内容
     */
    @Override
    public void sendAfterCommit(String destination, Object payload) {
        if (StrUtil.isBlank(destination)) {
            log.warn("事务后消息发送失败，目标地址为空");
            return;
        }

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            messagingTemplate.convertAndSend(destination, payload);
            log.info("当前无事务，消息已直接发送，目标地址：{}", destination);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend(destination, payload);
                log.info("事务提交后消息发送成功，目标地址：{}", destination);
            }
        });
    }

    /**
     * 异步发送消息
     *
     * @param destination 目标地址
     * @param payload     消息内容
     */
    @Async("webSocketPushExecutor")
    @Override
    public void sendAsync(String destination, Object payload) {
        if (StrUtil.isBlank(destination)) {
            log.warn("异步消息发送失败，目标地址为空");
            return;
        }

        messagingTemplate.convertAndSend(destination, payload);
        log.info("异步消息发送成功，目标地址：{}", destination);
    }

    /**
     * 创建指定 Session 的消息头
     *
     * @param sessionId 会话 ID
     * @return 消息头
     */
    private MessageHeaders createSessionHeaders(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);
        return accessor.getMessageHeaders();
    }
}
```

### 指定用户消息发送

指定用户消息发送用于向某个用户推送私有消息。用户可以有一个或多个在线连接，例如 PC 端、移动端、管理端同时在线。默认情况下，按用户发送的消息会发送给该用户当前匹配的会话。

推荐使用方式如下：

```java
wsPushService.sendToUser("10001", "/queue/notice", noticeMessage);
wsPushService.sendToUser("10001", "/queue/chat", chatMessage);
wsPushService.sendToUser("10001", "/queue/task", taskProgressMessage);
```

客户端订阅地址如下：

```text
/user/queue/notice
/user/queue/chat
/user/queue/task
```

指定用户消息发送注意事项如下：

| 注意项                 | 说明                                                   |
| ---------------------- | ------------------------------------------------------ |
| 用户 ID 必须一致       | `Principal#getName()` 必须和发送时传入的 `userId` 一致 |
| 服务端目标不带 `/user` | `convertAndSendToUser` 第二个参数写 `/queue/notice`    |
| 多端推送需明确         | 默认可推送到同一用户多个会话                           |
| 离线用户需补偿         | 用户不在线时应落库，重连后通过 HTTP 或补偿推送获取     |
| 权限必须服务端校验     | 不能让前端指定发送人身份                               |

### 指定会话消息发送

指定会话消息发送用于只向某个 WebSocket Session 推送消息，而不是推送给该用户的所有在线端。典型场景包括当前页面操作结果、当前连接错误提示、当前设备强制下线、只响应本次请求的 ACK 等。

推荐使用方式如下：

```java
wsPushService.sendToSession("10001", "session-001", "/queue/errors", errorMessage);
wsPushService.sendToSession("10001", "session-001", "/queue/ack", ackMessage);
```

客户端仍然订阅用户队列：

```text
/user/queue/errors
/user/queue/ack
/session scoped by server headers
```

指定会话消息适合以下场景：

| 场景         | 说明                         |
| ------------ | ---------------------------- |
| 当前请求错误 | 只返回给发起该请求的连接     |
| 当前设备下线 | 只踢出某个设备，不影响其他端 |
| 页面级 ACK   | 只确认当前页面发送的消息     |
| 临时订阅结果 | 只向当前连接返回一次性结果   |

如果只是普通用户通知，不要使用指定会话发送，否则用户其他在线端可能收不到消息。

### 指定群组消息发送

指定群组消息发送用于向某个群组、房间、项目空间或组织频道推送消息。服务端通常将群组 ID 拼接到 `/topic/group/{groupId}` 中，前端群成员订阅该地址接收消息。

推荐使用方式如下：

```java
wsPushService.sendToGroup("90001", groupChatMessage);
```

实际推送地址如下：

```text
/topic/group/90001
```

群组消息发送前必须完成以下校验：

| 校验项           | 说明                       |
| ---------------- | -------------------------- |
| 群组是否存在     | 防止向不存在的群组发送     |
| 发送人是否为成员 | 非成员不能发送群消息       |
| 用户是否被禁言   | 禁言用户不能发送           |
| 群组是否解散     | 解散群组不能发送           |
| 租户是否匹配     | 多租户系统必须校验租户隔离 |

群组推送不建议由前端直接决定最终推送地址。前端可以传入 `groupId`，但服务端必须根据当前用户身份和群组关系校验后再推送。

### 延迟消息发送

延迟消息发送用于需要在未来某个时间点推送消息的场景，例如提醒通知、任务超时提示、公告定时发布、订单倒计时通知等。

基础延迟推送可以使用 Spring `TaskScheduler`。如果系统对可靠性要求较高，应使用数据库延迟任务、Redis 延迟队列、RabbitMQ 延迟队列或 XXL-JOB，而不是仅依赖应用内存任务。

配置一个通用任务调度器。

文件位置：`src/main/java/io/github/atengk/websocket/config/WebSocketAsyncConfig.java`

```java
package io.github.atengk.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * WebSocket 异步与调度配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class WebSocketAsyncConfig {

    /**
     * WebSocket 延迟任务调度器
     *
     * @return 任务调度器
     */
    @Bean
    public TaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("ws-task-");
        scheduler.setErrorHandler(throwable -> log.error("WebSocket 调度任务执行异常", throwable));
        scheduler.initialize();
        return scheduler;
    }
}
```

使用示例：

```java
wsPushService.sendDelay(
        "/topic/system/notice",
        noticeMessage,
        Duration.ofMinutes(5)
);
```

延迟消息注意事项如下：

| 场景           | 推荐方案                     |
| -------------- | ---------------------------- |
| 非关键提醒     | 应用内 `TaskScheduler`       |
| 重要通知       | 数据库任务表 + 定时扫描      |
| 高可靠延迟     | RabbitMQ 延迟队列            |
| 分布式任务     | XXL-JOB、Quartz、调度中心    |
| 超大量延迟消息 | Redis ZSet、MQ、专用任务系统 |

### 批量消息发送

批量消息发送用于向多个指定用户推送相同或相似消息，例如审批通知、批量站内信、角色通知、部门通知等。批量推送不应简单无控制地循环大量用户，否则可能造成出站通道拥塞、线程池堆积或 Broker 压力过大。

基础批量推送示例：

```java
List<String> userIds = List.of("10001", "10002", "10003");
wsPushService.sendToUsers(userIds, "/queue/notice", noticeMessage);
```

批量发送建议如下：

| 数量级         | 推荐方式                                      |
| -------------- | --------------------------------------------- |
| 10 到 100 人   | 直接循环发送                                  |
| 100 到 5000 人 | 分批发送，控制批次大小                        |
| 更大范围       | 优先使用 Topic 广播或 MQ 异步分发             |
| 全站通知       | 不要逐个用户发送，使用 `/topic/system/notice` |
| 需要已读状态   | 广播实时触达 + 数据库按用户生成已读记录       |

分批发送示例：

```java
List<List<String>> partitions = CollUtil.split(userIds, 200);
for (List<String> partition : partitions) {
    wsPushService.sendToUsers(partition, "/queue/notice", noticeMessage);
}
```

批量发送时建议记录推送批次 ID、目标用户数、成功数、失败数和耗时，便于后续排查。

### 事务后消息发送

事务后消息发送用于避免“数据库事务回滚但前端已经收到成功通知”的问题。对于订单状态、审批待办、聊天落库、通知生成等关键业务，应在数据库事务提交成功后再推送消息。

错误示例：

```text
1. 更新订单状态为已支付
2. 立即推送订单已支付消息
3. 数据库事务提交失败
4. 前端已收到错误状态
```

正确流程：

```text
1. 开启事务
2. 更新数据库
3. 注册 afterCommit 推送动作
4. 数据库事务提交成功
5. 发送 WebSocket 消息
```

业务使用示例：

```java
@Transactional(rollbackFor = Exception.class)
public void completeOrder(String orderId) {
    // 1. 更新订单状态
    // orderService.updateStatus(orderId, "PAID");

    // 2. 事务提交后再推送
    wsPushService.sendAfterCommit(
            "/topic/dashboard/order",
            WsMessage.success(WsMessageType.BIZ_EVENT, orderId)
    );
}
```

事务后推送适合以下场景：

| 场景         | 说明                   |
| ------------ | ---------------------- |
| 订单状态变更 | 确认事务提交后再通知   |
| 审批待办生成 | 待办落库成功后再推送   |
| 聊天消息落库 | 消息保存成功后再分发   |
| 通知生成     | 通知记录落库后再推送   |
| 任务状态变更 | 任务状态持久化后再推送 |

如果推送失败，不能回滚已经提交的业务事务。应记录失败消息，并通过补偿任务、离线消息或客户端重连查询恢复。

### 异步消息发送

异步消息发送用于将推送动作从主业务线程中解耦，避免 WebSocket 推送阻塞核心业务流程。适用于非强同步通知、管理端看板、日志类消息、批量推送等场景。

启用异步支持：

文件位置：`src/main/java/io/github/atengk/websocket/config/WebSocketExecutorConfig.java`

```java
package io.github.atengk.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * WebSocket 推送线程池配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@EnableAsync
@Configuration
public class WebSocketExecutorConfig {

    /**
     * WebSocket 推送线程池
     *
     * @return 线程池
     */
    @Bean("webSocketPushExecutor")
    public Executor webSocketPushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("ws-push-");
        executor.setRejectedExecutionHandler((runnable, poolExecutor) -> {
            log.warn("WebSocket 推送线程池已满，任务被拒绝，activeCount：{}，queueSize：{}",
                    poolExecutor.getActiveCount(),
                    poolExecutor.getQueue().size());
            new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(runnable, poolExecutor);
        });
        executor.initialize();
        return executor;
    }
}
```

异步发送使用示例：

```java
wsPushService.sendAsync("/topic/admin/dashboard/message", dashboardMessage);
```

异步推送注意事项如下：

| 注意项           | 说明                                     |
| ---------------- | ---------------------------------------- |
| 不适合强一致响应 | 客户端不能把异步推送作为事务成功唯一依据 |
| 需要记录异常     | 异步线程异常不会直接抛给调用方           |
| 需要线程池隔离   | 不要复用业务核心线程池                   |
| 需要监控队列     | 队列堆积说明推送压力过高                 |
| 需要降级策略     | 推送失败时可记录失败消息或降低推送频率   |

### 定时任务消息推送

定时任务消息推送用于周期性向客户端发送统计数据、在线状态、任务进度、系统公告、管理端看板指标等。典型场景是管理端实时看板，每隔数秒推送一次当前连接数、在线用户数、消息量和失败率。

启用定时任务配置：

文件位置：`src/main/java/io/github/atengk/websocket/config/ScheduleConfig.java`

```java
package io.github.atengk.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
```

定时推送任务示例：

文件位置：`src/main/java/io/github/atengk/websocket/task/WebSocketDashboardPushTask.java`

```java
package io.github.atengk.websocket.task;

import cn.hutool.core.date.DateUtil;
import io.github.atengk.websocket.enums.WsMessageType;
import io.github.atengk.websocket.model.WsMessage;
import io.github.atengk.websocket.service.WsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 管理端看板推送任务
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketDashboardPushTask {

    private final WsPushService wsPushService;

    /**
     * 定时推送管理端看板数据
     */
    @Scheduled(fixedDelay = 5000)
    public void pushDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("onlineUserCount", 128);
        metrics.put("sessionCount", 236);
        metrics.put("messageSendCount", 10240);
        metrics.put("pushFailedCount", 3);
        metrics.put("timestamp", DateUtil.current());

        wsPushService.broadcast(
                "/topic/admin/dashboard/message",
                WsMessage.success(WsMessageType.BIZ_EVENT, metrics)
        );

        log.info("管理端看板数据推送完成，指标时间：{}", metrics.get("timestamp"));
    }
}
```

定时推送建议如下：

| 场景         | 推荐频率              |
| ------------ | --------------------- |
| 管理端看板   | 3 到 10 秒            |
| 在线人数统计 | 5 到 30 秒            |
| 高频业务指标 | 1 到 5 秒，但需要压测 |
| 系统公告扫描 | 30 到 60 秒           |
| 任务进度补偿 | 1 到 5 秒             |

不要把定时推送频率设置得过高。对于看板类数据，通常不需要毫秒级刷新，过高频率会增加出站通道、Broker、前端渲染和浏览器内存压力。

## 客户端消息接收

客户端消息接收用于建立 STOMP 连接、订阅主题、解析消息、去重、排序和断线重连补偿。前端不应在每个页面中重复创建连接，而应封装统一的 WebSocket 客户端模块，由业务页面按需订阅和取消订阅。

### 订阅广播主题

订阅广播主题用于接收 `/topic/**` 路径下的公共消息，例如系统公告、订单看板、公共告警等。广播主题通常多个客户端同时订阅，服务端发送一次，所有订阅者都会收到。

前端订阅示例：

```typescript
client.subscribe('/topic/system/notice', (message) => {
  const body = JSON.parse(message.body)
  console.log('收到系统公告：', body)
})
```

推荐封装为统一订阅方法：

```typescript
const subscribeSystemNotice = () => {
  return client.subscribe('/topic/system/notice', (message) => {
    const wsMessage = parseWsMessage(message.body)
    handleSystemNotice(wsMessage)
  })
}
```

广播订阅注意事项如下：

| 注意项               | 说明                                 |
| -------------------- | ------------------------------------ |
| 页面销毁要取消订阅   | 防止重复消费和内存泄漏               |
| 不订阅无权限主题     | 管理端、租户、群组主题必须按权限订阅 |
| 不在前端过滤敏感数据 | 服务端必须控制广播范围               |
| 需要处理未知类型     | 防止后端新增消息类型导致前端异常     |

### 订阅用户队列

订阅用户队列用于接收当前登录用户的私有消息。客户端订阅完整路径 `/user/queue/**`，服务端通过 `convertAndSendToUser(userId, "/queue/**", payload)` 推送。

常见用户队列如下：

```text
/user/queue/notice
/user/queue/chat
/user/queue/task
/user/queue/errors
/user/queue/ack
/user/queue/session
```

前端订阅示例：

```typescript
const subscribeUserQueues = () => {
  const subscriptions = []

  subscriptions.push(client.subscribe('/user/queue/notice', (message) => {
    handleNotice(parseWsMessage(message.body))
  }))

  subscriptions.push(client.subscribe('/user/queue/chat', (message) => {
    handleChatMessage(parseWsMessage(message.body))
  }))

  subscriptions.push(client.subscribe('/user/queue/errors', (message) => {
    handleErrorMessage(parseWsMessage(message.body))
  }))

  return subscriptions
}
```

用户队列订阅注意事项如下：

| 注意项                 | 说明                            |
| ---------------------- | ------------------------------- |
| 必须登录后订阅         | 未认证连接不应订阅用户私有队列  |
| 订阅路径带 `/user`     | 客户端订阅使用 `/user/queue/**` |
| 服务端发送不带 `/user` | 服务端发送使用 `/queue/**`      |
| 重连后重新订阅         | STOMP 重连后原订阅需要重新建立  |
| 页面级订阅要清理       | 避免页面切换后重复收到消息      |

### 订阅群组主题

订阅群组主题用于接收群聊、房间、项目空间、组织频道等多人消息。客户端通常在进入群组页面后订阅，离开页面后取消订阅。

前端订阅示例：

```typescript
const subscribeGroup = (groupId: string) => {
  const destination = `/topic/group/${groupId}`

  return client.subscribe(destination, (message) => {
    const wsMessage = parseWsMessage(message.body)
    handleGroupMessage(groupId, wsMessage)
  })
}
```

群组订阅建议如下：

| 场景         | 建议                                     |
| ------------ | ---------------------------------------- |
| 进入群聊页面 | 订阅 `/topic/group/{groupId}`            |
| 离开群聊页面 | 调用 `subscription.unsubscribe()`        |
| 切换群组     | 先取消旧群组订阅，再订阅新群组           |
| 多群消息列表 | 可只订阅用户队列，由服务端按用户推送摘要 |
| 群成员状态   | 单独订阅 `/topic/group/{groupId}/status` |

群组主题必须由服务端鉴权。前端即使知道某个群组 ID，也不代表有订阅权限。

### 订阅系统通知

订阅系统通知用于接收公告、维护提醒、版本更新、公共告警等消息。系统通知可以是广播，也可以是用户私有通知。

推荐订阅如下：

```typescript
const subscribeSystemMessages = () => {
  return [
    client.subscribe('/topic/system/notice', (message) => {
      handleSystemNotice(parseWsMessage(message.body))
    }),
    client.subscribe('/topic/system/alarm', (message) => {
      handleSystemAlarm(parseWsMessage(message.body))
    }),
    client.subscribe('/user/queue/notice', (message) => {
      handleUserNotice(parseWsMessage(message.body))
    })
  ]
}
```

系统通知接收后，前端通常需要做以下处理：

| 处理         | 说明                         |
| ------------ | ---------------------------- |
| 弹窗提醒     | 重要公告、紧急通知           |
| 通知角标     | 用户通知、待办、审批         |
| 通知列表追加 | 保存到前端状态管理           |
| 声音提醒     | 告警、聊天、待办             |
| 跳转链接     | 点击后进入业务详情           |
| ACK 回传     | 需要确认的通知回传收到或已读 |

### 消息解析

消息解析用于将 STOMP `message.body` 中的 JSON 字符串转换为统一消息对象，并根据 `type` 分发到不同处理器。前端应统一解析，不建议业务页面直接 `JSON.parse` 后各自处理。

推荐 TypeScript 类型如下：

```typescript
export interface WsMessage<T = unknown> {
  messageId: string
  type: string
  status: string
  version: string
  timestamp: number
  traceId?: string
  data?: T
}

export interface NoticeMessage {
  noticeId: string
  title: string
  content?: string
  level?: string
  bizId?: string
  bizType?: string
  url?: string
  needAck?: boolean
  expireAt?: number
}

export interface ChatMessage {
  chatId: string
  senderId: string
  receiverId?: string
  groupId?: string
  content: string
  contentType: string
  sequence?: number
  sentAt: number
}
```

统一解析方法如下：

```typescript
export const parseWsMessage = <T = unknown>(body: string): WsMessage<T> | null => {
  if (!body) {
    console.warn('WebSocket 消息体为空')
    return null
  }

  try {
    const message = JSON.parse(body) as WsMessage<T>

    if (!message.messageId || !message.type || !message.version) {
      console.warn('WebSocket 消息格式不完整：', message)
      return null
    }

    return message
  } catch (error) {
    console.error('WebSocket 消息解析失败：', error)
    return null
  }
}
```

统一消息分发示例：

```typescript
export const dispatchWsMessage = (message: WsMessage | null) => {
  if (!message) {
    return
  }

  switch (message.type) {
    case 'SYSTEM_NOTICE':
      handleSystemNotice(message)
      break
    case 'USER_NOTICE':
      handleUserNotice(message)
      break
    case 'PRIVATE_CHAT':
      handlePrivateChat(message)
      break
    case 'GROUP_CHAT':
      handleGroupChat(message)
      break
    case 'TASK_PROGRESS':
      handleTaskProgress(message)
      break
    case 'ALARM':
      handleAlarm(message)
      break
    case 'ERROR':
      handleWsError(message)
      break
    default:
      console.warn('未知 WebSocket 消息类型：', message.type)
  }
}
```

消息解析注意事项如下：

| 注意项         | 说明                                    |
| -------------- | --------------------------------------- |
| 捕获 JSON 异常 | 防止单条异常消息影响连接                |
| 校验基础字段   | `messageId`、`type`、`version` 必须存在 |
| 兼容未知字段   | 后端新增字段不应导致前端报错            |
| 兼容未知类型   | 未知类型进入默认处理逻辑                |
| 记录 traceId   | 异常排查时可提供给后端定位              |

### 消息去重

消息去重用于避免重复展示同一条消息。重复消息可能来自断线重连补偿、服务端重试、客户端重复订阅、浏览器多标签页、ACK 超时重发等情况。

推荐以 `messageId` 作为去重主键。前端可以维护一个固定容量的已处理消息 ID 缓存。

```typescript
class WsMessageDeduplicator {
  private readonly maxSize: number
  private readonly messageIds: Set<string>

  constructor(maxSize = 1000) {
    this.maxSize = maxSize
    this.messageIds = new Set<string>()
  }

  has(messageId: string): boolean {
    return this.messageIds.has(messageId)
  }

  add(messageId: string): void {
    if (!messageId) {
      return
    }

    if (this.messageIds.size >= this.maxSize) {
      const first = this.messageIds.values().next().value
      this.messageIds.delete(first)
    }

    this.messageIds.add(messageId)
  }

  shouldHandle(messageId: string): boolean {
    if (!messageId || this.has(messageId)) {
      return false
    }

    this.add(messageId)
    return true
  }
}

export const wsMessageDeduplicator = new WsMessageDeduplicator(2000)
```

使用示例：

```typescript
const handleIncomingMessage = (body: string) => {
  const message = parseWsMessage(body)

  if (!message) {
    return
  }

  if (!wsMessageDeduplicator.shouldHandle(message.messageId)) {
    console.warn('WebSocket 重复消息已忽略：', message.messageId)
    return
  }

  dispatchWsMessage(message)
}
```

去重建议如下：

| 场景         | 去重策略                           |
| ------------ | ---------------------------------- |
| 聊天消息     | 按 `messageId` 去重                |
| 会话消息排序 | 按 `chatId + sequence` 辅助去重    |
| 通知消息     | 按 `noticeId` 或 `messageId` 去重  |
| 任务进度     | 按 `taskId + progress/status` 合并 |
| 看板数据     | 不需要保留历史，只覆盖最新值       |
| 告警消息     | 按 `alarmId` 聚合展示              |

### 消息排序

消息排序用于处理消息乱序问题。WebSocket 单连接内通常能保持发送顺序，但在多实例、异步发送、断线补偿、批量推送、历史消息合并等情况下，前端仍可能收到乱序消息。

聊天消息建议使用会话内递增 `sequence` 排序；通知和任务消息可以使用服务端时间戳 `timestamp` 排序。

聊天消息排序示例：

```typescript
interface ChatViewMessage {
  messageId: string
  chatId: string
  content: string
  sequence?: number
  sentAt: number
}

export const sortChatMessages = (messages: ChatViewMessage[]): ChatViewMessage[] => {
  return [...messages].sort((a, b) => {
    if (a.sequence !== undefined && b.sequence !== undefined) {
      return a.sequence - b.sequence
    }
    return a.sentAt - b.sentAt
  })
}
```

通知消息排序示例：

```typescript
export const sortNoticeMessages = <T extends { timestamp: number }>(messages: T[]): T[] => {
  return [...messages].sort((a, b) => b.timestamp - a.timestamp)
}
```

排序规则建议如下：

| 消息类型 | 排序字段                       | 说明                 |
| -------- | ------------------------------ | -------------------- |
| 单聊消息 | `sequence` 优先，`sentAt` 兜底 | 保证会话内顺序       |
| 群聊消息 | `sequence` 优先，`sentAt` 兜底 | 服务端应生成群内序号 |
| 通知消息 | `timestamp` 倒序               | 最新通知在前         |
| 任务进度 | `progress` 或 `timestamp`      | 通常覆盖最新状态     |
| 告警消息 | `level + timestamp`            | 高级别、最新优先     |
| 看板数据 | `timestamp`                    | 只保留最新快照       |

排序不能替代幂等处理。前端应先去重，再按业务规则排序。

### 断线重连后补偿

断线重连后补偿用于处理客户端断线期间漏收的消息。WebSocket 是实时通道，不保证客户端离线期间自动收到所有历史消息，因此关键业务必须提供 HTTP 补偿接口或服务端离线消息机制。

推荐流程如下：

```text
1. 客户端连接成功
2. 订阅用户队列、系统主题、群组主题
3. 记录当前 lastMessageId 或 lastSyncTime
4. 连接断开
5. 客户端自动重连
6. 重连成功后重新订阅所有主题
7. 调用 HTTP 补偿接口查询断线期间消息
8. 前端去重、排序、合并到本地状态
```

前端重连配置示例：

```typescript
import { Client } from '@stomp/stompjs'

export const createStompClient = (token: string) => {
  const client = new Client({
    brokerURL: 'ws://localhost:8080/ws/stomp',
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      console.info('WebSocket 连接成功')
      resubscribeAll()
      compensateOfflineMessages()
    },
    onStompError: (frame) => {
      console.error('STOMP 协议错误：', frame.headers['message'], frame.body)
    },
    onWebSocketClose: () => {
      console.warn('WebSocket 连接已关闭，等待自动重连')
    }
  })

  return client
}
```

补偿接口调用示例：

```typescript
const compensateOfflineMessages = async () => {
  const lastSyncTime = localStorage.getItem('ws:lastSyncTime') || '0'

  try {
    const response = await fetch(`/api/ws/messages/compensate?lastSyncTime=${lastSyncTime}`, {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('access_token')}`
      }
    })

    const result = await response.json()
    const messages = result.data || []

    messages
      .filter((message: WsMessage) => wsMessageDeduplicator.shouldHandle(message.messageId))
      .sort((a: WsMessage, b: WsMessage) => a.timestamp - b.timestamp)
      .forEach(dispatchWsMessage)

    localStorage.setItem('ws:lastSyncTime', String(Date.now()))
    console.info('WebSocket 离线消息补偿完成，消息数量：', messages.length)
  } catch (error) {
    console.error('WebSocket 离线消息补偿失败：', error)
  }
}
```

后端补偿接口建议按用户、时间、消息类型查询。

```text
GET /api/ws/messages/compensate?lastSyncTime=1777956000000

返回：
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "messageId": "1909550919275921410",
      "type": "USER_NOTICE",
      "status": "SUCCESS",
      "version": "1.0",
      "timestamp": 1777956000000,
      "data": {}
    }
  ]
}
```

断线补偿建议如下：

| 场景     | 补偿方式                             |
| -------- | ------------------------------------ |
| 聊天消息 | 按会话查询 lastSequence 之后的消息   |
| 用户通知 | 按 lastSyncTime 查询未读或新增通知   |
| 任务进度 | 查询任务当前最终状态，不回放全部进度 |
| 系统公告 | 查询有效期内未读公告                 |
| 告警消息 | 查询未处理告警                       |
| 看板数据 | 不补偿历史，只刷新最新快照           |

断线重连后必须重新订阅。STOMP 连接断开后，旧订阅已经失效，客户端不能假设原订阅仍然存在。前端应维护订阅注册表，在 `onConnect` 中统一恢复订阅。


## 连接生命周期管理

连接生命周期管理用于描述 WebSocket STOMP 从握手、连接、订阅、消息收发到断开、异常关闭、超时关闭的完整过程。生命周期管理是认证鉴权、在线状态、会话管理、日志审计、资源清理和故障排查的基础。

### 握手阶段

握手阶段发生在 WebSocket 连接真正建立之前。客户端通过 HTTP 请求访问 WebSocket Endpoint，例如 `/ws/stomp`，服务端校验来源、参数、Cookie、Token 或网关透传 Header 后，决定是否允许协议升级。

典型握手流程如下：

```text
1. 客户端请求 ws://localhost:8080/ws/stomp
2. 浏览器发送 HTTP Upgrade 请求
3. 服务端进入 HandshakeInterceptor
4. 校验 Origin、Token、Query 参数、Cookie 等信息
5. 将必要信息写入 attributes
6. 握手通过后升级为 WebSocket 连接
7. 客户端继续发送 STOMP CONNECT 帧
```

握手阶段适合处理以下逻辑：

| 处理项         | 说明                                          |
| -------------- | --------------------------------------------- |
| 来源校验       | 判断 `Origin` 是否允许连接                    |
| Query 参数解析 | 读取 `token`、`clientType`、`deviceId` 等参数 |
| Cookie 解析    | 读取登录态 Cookie 或 Session ID               |
| IP 获取        | 记录客户端 IP，便于审计和限流                 |
| 初步黑名单     | 拦截黑名单 IP 或异常来源                      |
| 属性传递       | 将握手阶段信息放入 WebSocket attributes       |

握手拦截器示例。

文件位置：`src/main/java/io/github/atengk/websocket/interceptor/WsHandshakeInterceptor.java`

```java
package io.github.atengk.websocket.interceptor;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * 握手前处理
     *
     * @param request    请求对象
     * @param response   响应对象
     * @param wsHandler  WebSocket 处理器
     * @param attributes WebSocket 会话属性
     * @return 是否允许握手
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return true;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String token = httpRequest.getParameter("token");
        String clientType = httpRequest.getParameter("clientType");
        String deviceId = httpRequest.getParameter("deviceId");
        String ip = getClientIp(httpRequest);

        attributes.put("token", token);
        attributes.put("clientType", StrUtil.blankToDefault(clientType, "WEB"));
        attributes.put("deviceId", deviceId);
        attributes.put("ip", ip);

        log.info("WebSocket 握手请求，ip：{}，clientType：{}，deviceId：{}", ip, clientType, deviceId);
        return true;
    }

    /**
     * 握手后处理
     *
     * @param request   请求对象
     * @param response  响应对象
     * @param wsHandler WebSocket 处理器
     * @param exception 异常信息
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               org.springframework.http.server.ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            log.warn("WebSocket 握手完成但存在异常：{}", exception.getMessage());
        }
    }

    /**
     * 获取客户端 IP
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            return StrUtil.splitTrim(forwardedFor, ",").get(0);
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }

        return request.getRemoteAddr();
    }
}
```

需要在 `WebSocketConfig` 的 Endpoint 注册阶段挂载握手拦截器。

```java
registry.addEndpoint(properties.getEndpoint())
        .addInterceptors(wsHandshakeInterceptor)
        .setAllowedOriginPatterns(allowedOrigins)
        .withSockJS();
```

### 连接建立

连接建立分为 WebSocket 物理连接建立和 STOMP 逻辑连接建立。WebSocket 握手成功只代表底层连接可用，真正的业务连接通常以 STOMP `CONNECT` 帧认证成功为准。

连接建立阶段建议完成以下事项：

| 事项             | 说明                                                    |
| ---------------- | ------------------------------------------------------- |
| 解析 Token       | 从 CONNECT Header、握手 attributes、Cookie 中读取 Token |
| 绑定用户身份     | 设置 `Principal`，保证用户点对点消息可达                |
| 注册 Session     | 保存 `sessionId`、`userId`、客户端类型、设备 ID、IP     |
| 更新在线状态     | 标记用户或设备在线                                      |
| 记录连接日志     | 记录连接时间、用户、IP、设备信息                        |
| 初始化订阅上下文 | 为后续 SUBSCRIBE 鉴权做准备                             |

连接建立通常在 `ChannelInterceptor` 的 `CONNECT` 阶段完成用户身份绑定，在 `SessionConnectedEvent` 中完成日志和在线状态更新。

### 订阅建立

订阅建立发生在客户端发送 STOMP `SUBSCRIBE` 帧时。订阅阶段是权限控制的关键节点，尤其是群组主题、租户主题和管理端主题，必须在服务端校验订阅权限。

订阅建立流程如下：

```text
1. 客户端发送 SUBSCRIBE
2. 服务端进入 ChannelInterceptor
3. 获取 destination
4. 判断路径类型
5. 校验用户是否有订阅权限
6. 校验通过后建立订阅
7. 触发 SessionSubscribeEvent
8. 记录订阅日志或维护订阅关系
```

订阅权限建议如下：

| 订阅路径                      | 权限要求               |
| ----------------------------- | ---------------------- |
| `/topic/system/notice`        | 登录用户可订阅         |
| `/user/queue/**`              | 当前登录用户可订阅     |
| `/topic/group/{groupId}`      | 群组成员可订阅         |
| `/topic/tenant/{tenantId}/**` | 当前租户用户可订阅     |
| `/topic/admin/**`             | 管理员或运维角色可订阅 |

订阅建立时不建议只记录日志，应同时具备拒绝非法订阅的能力。非法订阅应直接抛出访问拒绝异常，避免客户端悄悄订阅成功。

### 消息发送

消息发送指客户端通过 STOMP `SEND` 帧向服务端发送消息。所有客户端发送的业务消息建议统一进入 `/app/**`，由 `@MessageMapping` 处理。

消息发送阶段建议完成以下处理：

| 处理项       | 说明                                     |
| ------------ | ---------------------------------------- |
| 发送路径校验 | 只允许发送到 `/app/**`                   |
| 用户身份校验 | 未认证用户禁止发送业务消息               |
| 请求体校验   | 校验 JSON 格式、必填字段、长度限制       |
| 业务权限校验 | 校验是否可向目标用户、群组、业务对象发送 |
| 限流校验     | 防止高频发送和恶意刷消息                 |
| 敏感词过滤   | 聊天、评论等内容类消息需要过滤           |
| 消息落库     | 重要消息先持久化，再事务后推送           |

客户端发送路径示例：

```text
/app/chat/private/send
/app/chat/group/send
/app/notice/read
/app/chat/ack
/app/online/status/change
```

### 消息接收

消息接收指客户端订阅目标地址后收到服务端推送的 `MESSAGE` 帧。服务端发送消息前应保证消息体结构统一、消息 ID 唯一、类型明确、版本可识别。

消息接收流程如下：

```text
1. 客户端订阅 /topic/** 或 /user/queue/**
2. 服务端通过 SimpMessagingTemplate 发送消息
3. Broker 匹配订阅关系
4. 客户端收到 MESSAGE 帧
5. 前端解析 JSON 消息体
6. 根据 messageId 去重
7. 根据 type 分发到对应业务处理器
8. 必要时回传 ACK 或 READ
```

客户端接收时应遵循以下规则：

| 规则       | 说明                              |
| ---------- | --------------------------------- |
| 先解析     | 统一解析 `WsMessage<T>` 外层结构  |
| 再去重     | 使用 `messageId` 或业务 ID 去重   |
| 再排序     | 聊天按 sequence，通知按 timestamp |
| 再分发     | 按 `type` 分发到对应模块          |
| 必要时 ACK | 重要消息回传收到或已读状态        |
| 异常不中断 | 单条消息解析失败不能断开连接      |

### 取消订阅

取消订阅发生在客户端发送 STOMP `UNSUBSCRIBE` 帧时。常见场景包括页面销毁、切换群组、关闭弹窗、退出房间、退出项目空间等。

取消订阅建议如下：

| 场景           | 处理方式                         |
| -------------- | -------------------------------- |
| 页面销毁       | 取消当前页面创建的所有订阅       |
| 切换群组       | 先取消旧群组订阅，再订阅新群组   |
| 用户退出群组   | 服务端取消权限，前端取消对应订阅 |
| 强制下线       | 关闭连接并清理订阅               |
| 管理端切换看板 | 取消不再使用的看板订阅           |

前端需要保存订阅对象，页面销毁时调用 `unsubscribe()`。

```typescript
const subscription = client.subscribe('/topic/group/90001', (message) => {
  handleGroupMessage(message)
})

// 页面销毁或切换群组时取消订阅
subscription.unsubscribe()
```

服务端可通过 `SessionUnsubscribeEvent` 记录取消订阅日志，但通常不需要为每个订阅持久化状态，除非业务确实需要统计订阅人数或房间在线人数。

### 连接断开

连接断开发生在客户端主动关闭连接、浏览器关闭、网络中断、服务端关闭连接、Token 失效、强制下线等场景。连接断开后必须清理 Session、更新在线状态、释放资源并记录日志。

断开处理流程如下：

```text
1. 客户端关闭或连接异常
2. 服务端触发 SessionDisconnectEvent
3. 获取 sessionId 和 Principal
4. 清理本地 Session 注册表
5. 清理 Redis Session 映射
6. 判断用户是否还有其他在线端
7. 必要时广播用户离线状态
8. 记录断开日志
```

断开事件监听示例可以在前面 `StompSessionEventListener` 基础上扩展在线状态清理逻辑。

```java
@EventListener
public void onSessionDisconnect(SessionDisconnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    Principal user = accessor.getUser();

    String sessionId = accessor.getSessionId();
    String userId = user == null ? null : user.getName();

    log.info("STOMP 连接断开，sessionId：{}，userId：{}，closeStatus：{}",
            sessionId, userId, event.getCloseStatus());

    // sessionRegistry.remove(sessionId);
    // onlineStatusService.offlineIfNoSession(userId);
}
```

多端在线场景下，不能因为某一个 Session 断开就直接把用户置为离线。必须先判断该用户是否还有其他有效 Session。

### 异常关闭

异常关闭包括协议错误、认证失败、权限拒绝、消息格式错误、服务端异常、网关中断、Broker 异常、客户端非正常断开等情况。异常关闭需要记录足够日志，方便定位问题。

常见异常关闭原因如下：

| 原因         | 说明                             |
| ------------ | -------------------------------- |
| 认证失败     | Token 缺失、过期、伪造           |
| 授权失败     | 订阅非法路径或发送非法路径       |
| 消息格式错误 | JSON 解析失败、字段校验失败      |
| 连接被踢出   | 管理端强制下线或多端策略限制     |
| 心跳超时     | 客户端长时间无心跳               |
| 网关超时     | Nginx、Gateway、Ingress 关闭连接 |
| Broker 异常  | 外部消息代理不可用               |
| 服务重启     | 应用发布或异常退出               |

异常关闭处理建议如下：

| 处理项       | 说明                                       |
| ------------ | ------------------------------------------ |
| 记录错误码   | 便于前端展示和后端排查                     |
| 记录 traceId | 关联日志链路                               |
| 清理 Session | 避免连接泄漏                               |
| 推送错误消息 | 可在关闭前向 `/user/queue/errors` 发送错误 |
| 前端重连策略 | 对网络异常可重连，对认证失败不应无限重连   |
| 管理端告警   | 高频异常关闭应进入告警系统                 |

### 超时关闭

超时关闭通常由心跳超时、网关读写超时、客户端长时间无响应、服务端空闲连接清理等机制触发。超时关闭的目标是避免无效连接占用内存、线程、Session 映射和 Broker 资源。

超时相关配置包括：

| 配置                       | 说明                                       |
| -------------------------- | ------------------------------------------ |
| STOMP 心跳                 | 服务端和客户端互相检测连接活性             |
| Nginx `proxy_read_timeout` | 网关读超时，过短会导致连接被关闭           |
| Gateway 超时               | Spring Cloud Gateway 或 Ingress 长连接超时 |
| Redis Session TTL          | 分布式 Session 映射过期时间                |
| 客户端重连延迟             | 断开后多久尝试重连                         |

心跳建议如下：

```yaml
app:
  websocket:
    # 服务端每 10 秒向客户端发送心跳
    server-heartbeat: 10000

    # 服务端期望客户端每 10 秒发送心跳
    client-heartbeat: 10000
```

超时关闭注意事项如下：

| 注意项                 | 说明                                    |
| ---------------------- | --------------------------------------- |
| 心跳不宜过短           | 大量连接下心跳过短会增加网络和 CPU 压力 |
| 网关超时要大于心跳间隔 | 否则连接可能被网关误关                  |
| 移动端可适当放宽       | 弱网和后台场景下心跳可更长              |
| 断开后需清理状态       | 防止在线状态不准确                      |
| 前端需要重连           | 网络抖动后自动恢复订阅和补偿消息        |

## 认证与授权

认证与授权用于保证只有合法用户可以建立连接、发送消息、订阅主题和接收数据。WebSocket STOMP 安全不能只依赖 HTTP 登录接口，因为连接建立后客户端可以持续发送 STOMP 帧，因此必须同时覆盖握手阶段、CONNECT 阶段、SUBSCRIBE 阶段和 SEND 阶段。

### Token 握手认证

Token 握手认证是在 WebSocket HTTP Upgrade 阶段进行初步认证。客户端可以将 Token 放在 Query 参数中，例如 `/ws/stomp?token=xxx`，服务端通过 `HandshakeInterceptor` 读取并校验。

握手认证优点是可以在 WebSocket 连接建立前拒绝非法请求，减少无效连接。缺点是 Query 参数容易被网关访问日志、浏览器历史或监控系统记录，因此生产环境需要谨慎使用。

示例连接地址：

```text
ws://localhost:8080/ws/stomp?token=eyJhbGciOiJIUzI1NiJ9.xxx
```

握手认证适用场景如下：

| 场景         | 说明                              |
| ------------ | --------------------------------- |
| 内部系统     | 访问链路受控，日志脱敏完善        |
| SockJS 兼容  | 某些场景 Header 传递不方便        |
| 初步拦截     | 在连接建立前过滤无效请求          |
| 设备标识传递 | 可同时传 `clientType`、`deviceId` |

生产环境如果使用 Query Token，必须做到日志脱敏，不要在网关、应用日志、异常日志中打印完整 Token。

### Header 认证

Header 认证通常发生在 STOMP `CONNECT` 阶段。客户端通过 `connectHeaders` 传递 `Authorization`，服务端在 `ChannelInterceptor` 中读取并解析 Token，然后设置当前连接的 `Principal`。

前端示例：

```typescript
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws/stomp',
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`
  },
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000
})
```

服务端读取 Header：

```java
String authorization = accessor.getFirstNativeHeader("Authorization");
```

Header 认证推荐作为主要认证方式，原因如下：

| 优点           | 说明                             |
| -------------- | -------------------------------- |
| 语义清晰       | 与 HTTP `Authorization` 保持一致 |
| 不暴露在 URL   | 相比 Query 参数更安全            |
| 便于刷新 Token | 重连时可更新 Header              |
| 适合 STOMP     | STOMP CONNECT 天然支持 Header    |

需要注意，浏览器原生 WebSocket 构造函数不支持直接设置自定义 HTTP Header，但 STOMP CONNECT Header 是在 WebSocket 建立后由客户端发送的 STOMP 帧 Header，可以通过 `@stomp/stompjs` 设置。

### Query 参数认证

Query 参数认证通过连接地址传递 Token，常用于 SockJS、简单客户端、测试工具或无法设置 STOMP Header 的场景。

前端示例：

```typescript
const socket = new SockJS(`http://localhost:8080/ws/stomp?token=${encodeURIComponent(accessToken)}`)
const client = new Client({
  webSocketFactory: () => socket,
  reconnectDelay: 5000
})
```

服务端可在握手拦截器中读取：

```java
String token = httpRequest.getParameter("token");
attributes.put("token", token);
```

Query 参数认证注意事项如下：

| 注意项             | 说明                                  |
| ------------------ | ------------------------------------- |
| 必须日志脱敏       | 禁止打印完整 URL 和完整 Token         |
| 建议短有效期       | 降低泄露风险                          |
| HTTPS/WSS 必须启用 | 防止明文传输                          |
| 不适合高安全场景   | 优先使用 STOMP CONNECT Header         |
| 可以作为兜底       | Header 缺失时从握手 attributes 中读取 |

### Cookie 认证

Cookie 认证适合传统 Session 登录系统、同域部署系统或使用 HttpOnly Cookie 存储 Token 的系统。浏览器建立 WebSocket 连接时会按同源策略和 Cookie 策略携带 Cookie。

Cookie 认证优点是前端不需要显式处理 Token，缺点是需要额外关注 SameSite、Secure、跨域、CSRF 风险和负载均衡 Session 一致性。

Cookie 认证适用场景如下：

| 场景           | 说明                           |
| -------------- | ------------------------------ |
| 前后端同域     | Cookie 自动携带，接入简单      |
| 传统 Session   | 与已有登录态复用               |
| HttpOnly Token | 前端 JS 无法直接读取 Token     |
| 企业内部系统   | 登录态统一由网关或认证中心管理 |

Cookie 认证注意事项如下：

| 注意项       | 说明                                    |
| ------------ | --------------------------------------- |
| 跨域 Cookie  | 需要配置 `SameSite=None; Secure`        |
| HTTPS 要求   | Secure Cookie 必须通过 HTTPS/WSS        |
| CSRF 风险    | 不能只依赖 Cookie，需要校验 Origin      |
| 集群 Session | 需要 Redis Session 或粘性会话           |
| STOMP 鉴权   | Cookie 认证后仍需校验 SUBSCRIBE 和 SEND |

### Spring Security 集成

Spring Security 集成用于统一 HTTP 接口和 WebSocket 握手路径的安全规则。一般建议 HTTP 层放行 WebSocket Endpoint，让连接能够进入 STOMP CONNECT 阶段，再由 `ChannelInterceptor` 做 Token 解析和用户绑定。

基础配置示例。

文件位置：`src/main/java/io/github/atengk/security/config/SecurityConfig.java`

```java
package io.github.atengk.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 安全过滤链
     *
     * @param http HTTP 安全配置
     * @return 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // WebSocket 通常不使用表单 CSRF，实际项目需结合登录方式评估
                .csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**"))
                .authorizeHttpRequests(authorize -> authorize
                        // 放行 WebSocket 握手地址，STOMP CONNECT 阶段再认证 Token
                        .requestMatchers("/ws/**").permitAll()
                        // 放行登录接口
                        .requestMatchers("/api/auth/login").permitAll()
                        // 其他 HTTP 接口需要认证
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
```

Spring Security 集成注意事项如下：

| 注意项           | 说明                                     |
| ---------------- | ---------------------------------------- |
| 握手路径可放行   | STOMP CONNECT 阶段做真正认证             |
| 不要只保护 HTTP  | STOMP SUBSCRIBE 和 SEND 也要鉴权         |
| CSRF 需评估      | Cookie 认证场景尤其要校验 Origin         |
| Principal 要一致 | WebSocket Principal 名称要和用户 ID 一致 |
| 异常要可读       | 认证失败应返回明确错误或关闭连接         |

### JWT 集成

JWT 集成用于无状态认证。客户端登录后获取 JWT，在 STOMP CONNECT Header 或 Query 参数中传递给服务端。服务端解析 JWT，校验签名、过期时间、用户状态和权限信息，然后绑定 WebSocket Principal。

JWT 解析服务示例。

文件位置：`src/main/java/io/github/atengk/websocket/service/AuthUserService.java`

```java
package io.github.atengk.websocket.service;

import java.security.Principal;

/**
 * WebSocket 用户认证服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface AuthUserService {

    /**
     * 解析用户 ID
     *
     * @param token 访问令牌
     * @return 用户 ID
     */
    String parseUserId(String token);

    /**
     * 判断是否为管理员
     *
     * @param principal 当前用户
     * @return 是否管理员
     */
    boolean isAdmin(Principal principal);

    /**
     * 判断是否允许订阅群组
     *
     * @param userId  用户 ID
     * @param groupId 群组 ID
     * @return 是否允许
     */
    boolean canSubscribeGroup(String userId, String groupId);

    /**
     * 判断是否允许发送到群组
     *
     * @param userId  用户 ID
     * @param groupId 群组 ID
     * @return 是否允许
     */
    boolean canSendGroup(String userId, String groupId);
}
```

下面是示例实现，演示 Token 清洗、用户 ID 解析和基础权限判断。实际项目中应替换为真实 JWT 工具类、用户服务、权限服务或 Sa-Token/Spring Security 上下文。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/AuthUserServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.AuthUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.Principal;

/**
 * WebSocket 用户认证服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class AuthUserServiceImpl implements AuthUserService {

    /**
     * 解析用户 ID
     *
     * @param token 访问令牌
     * @return 用户 ID
     */
    @Override
    public String parseUserId(String token) {
        String cleanToken = cleanToken(token);
        if (StrUtil.isBlank(cleanToken)) {
            return null;
        }

        // 示例逻辑：开发阶段允许 token=userId:10001
        // 生产环境应替换为 JWT 签名校验、过期校验、用户状态校验
        if (StrUtil.startWith(cleanToken, "userId:")) {
            return StrUtil.removePrefix(cleanToken, "userId:");
        }

        log.warn("Token 解析失败，请替换为真实 JWT 解析逻辑");
        return null;
    }

    /**
     * 判断是否为管理员
     *
     * @param principal 当前用户
     * @return 是否管理员
     */
    @Override
    public boolean isAdmin(Principal principal) {
        if (principal == null || StrUtil.isBlank(principal.getName())) {
            return false;
        }

        // 示例逻辑：实际项目应查询角色权限
        return StrUtil.equalsAny(principal.getName(), "1", "admin", "10000");
    }

    /**
     * 判断是否允许订阅群组
     *
     * @param userId  用户 ID
     * @param groupId 群组 ID
     * @return 是否允许
     */
    @Override
    public boolean canSubscribeGroup(String userId, String groupId) {
        if (StrUtil.hasBlank(userId, groupId)) {
            return false;
        }

        // 示例逻辑：实际项目应查询群组成员表
        return true;
    }

    /**
     * 判断是否允许发送到群组
     *
     * @param userId  用户 ID
     * @param groupId 群组 ID
     * @return 是否允许
     */
    @Override
    public boolean canSendGroup(String userId, String groupId) {
        if (StrUtil.hasBlank(userId, groupId)) {
            return false;
        }

        // 示例逻辑：实际项目应校验成员身份、禁言状态、群组状态
        return true;
    }

    /**
     * 清洗 Token
     *
     * @param token 原始 Token
     * @return 清洗后的 Token
     */
    private String cleanToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }

        return StrUtil.removePrefixIgnoreCase(token.trim(), "Bearer ");
    }
}
```

JWT 校验至少应包含以下内容：

| 校验项     | 说明                           |
| ---------- | ------------------------------ |
| 签名校验   | 防止伪造 Token                 |
| 过期时间   | 过期 Token 拒绝连接            |
| 用户状态   | 禁用、删除、冻结用户拒绝连接   |
| Token 版本 | 支持用户修改密码后失效旧 Token |
| 权限信息   | 管理端订阅、群组订阅需要权限   |
| 租户信息   | 多租户系统必须校验租户隔离     |

### 用户身份绑定

用户身份绑定是 WebSocket 用户点对点消息能否正常工作的关键。Spring 的 `convertAndSendToUser(userId, "/queue/notice", payload)` 会根据当前连接的 `Principal#getName()` 匹配用户，因此绑定的名称必须和发送时使用的用户 ID 一致。

Principal 模型如下。

文件位置：`src/main/java/io/github/atengk/websocket/model/StompUserPrincipal.java`

```java
package io.github.atengk.websocket.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

/**
 * STOMP 用户身份
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@RequiredArgsConstructor
public class StompUserPrincipal implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识
     */
    private final String name;
}
```

在 CONNECT 阶段绑定用户：

```java
String userId = authUserService.parseUserId(token);
accessor.setUser(new StompUserPrincipal(userId));
```

用户身份绑定要求如下：

| 要求                       | 说明                                          |
| -------------------------- | --------------------------------------------- |
| `Principal#getName()` 稳定 | 建议使用用户 ID，不建议使用昵称               |
| 与发送 ID 一致             | `convertAndSendToUser` 的 userId 必须匹配     |
| 不信任前端 senderId        | 发送人 ID 由服务端 Principal 决定             |
| 支持多端                   | 同一用户多个 Session 可以共享同一 Principal   |
| 支持踢人                   | Session 注册表应记录 userId 与 sessionId 关系 |

### 会话权限校验

会话权限校验用于控制某个连接是否仍然有效。例如 Token 已过期、用户被禁用、设备被踢出、Session 被强制下线、用户超过最大连接数时，都应拒绝继续发送或订阅。

会话校验建议覆盖以下场景：

| 场景           | 处理方式                                |
| -------------- | --------------------------------------- |
| Token 过期     | 拒绝 SEND/SUBSCRIBE，通知客户端重新登录 |
| 用户被禁用     | 关闭连接或拒绝后续消息                  |
| 设备被踢出     | 向当前 Session 推送下线通知并关闭连接   |
| 多端超限       | 按策略拒绝新连接或踢出旧连接            |
| Session 不存在 | 拒绝发送和订阅                          |
| 租户状态异常   | 拒绝租户相关 Topic 订阅                 |

会话权限可以在 `ChannelInterceptor` 的 `SUBSCRIBE` 和 `SEND` 阶段统一校验。不要只在连接建立时校验一次，因为长连接可能持续数小时，期间用户权限和状态可能发生变化。

### 订阅权限校验

订阅权限校验用于防止用户订阅不属于自己的主题。它是 WebSocket 安全中最容易被忽略但最关键的一层。

推荐订阅校验规则如下：

| 路径                          | 校验规则                           |
| ----------------------------- | ---------------------------------- |
| `/topic/system/**`            | 登录用户可订阅，敏感系统主题需角色 |
| `/user/queue/**`              | 当前用户私有队列，登录用户可订阅   |
| `/topic/group/{groupId}`      | 必须是群组成员                     |
| `/topic/tenant/{tenantId}/**` | 必须属于当前租户                   |
| `/topic/admin/**`             | 必须具备管理员或运维权限           |

订阅鉴权示例。

文件位置：`src/main/java/io/github/atengk/websocket/interceptor/StompPermissionChannelInterceptor.java`

```java
package io.github.atengk.websocket.interceptor;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.AuthUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * STOMP 权限校验组件
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompPermissionChannelInterceptor {

    private final AuthUserService authUserService;

    /**
     * 校验订阅权限
     *
     * @param principal   当前用户
     * @param destination 订阅地址
     */
    public void checkSubscribePermission(Principal principal, String destination) {
        if (principal == null || StrUtil.isBlank(principal.getName())) {
            throw new AccessDeniedException("用户未认证");
        }

        if (StrUtil.isBlank(destination)) {
            throw new AccessDeniedException("订阅地址不能为空");
        }

        String userId = principal.getName();

        if (StrUtil.startWith(destination, "/topic/admin")) {
            checkAdmin(principal, destination);
            return;
        }

        if (StrUtil.startWith(destination, "/topic/group/")) {
            String groupId = ReUtil.getGroup1("^/topic/group/([^/]+).*$", destination);
            if (!authUserService.canSubscribeGroup(userId, groupId)) {
                log.warn("群组订阅被拒绝，userId：{}，groupId：{}，destination：{}", userId, groupId, destination);
                throw new AccessDeniedException("无群组订阅权限");
            }
        }

        log.info("订阅权限校验通过，userId：{}，destination：{}", userId, destination);
    }

    /**
     * 校验管理端权限
     *
     * @param principal   当前用户
     * @param destination 订阅地址
     */
    private void checkAdmin(Principal principal, String destination) {
        if (!authUserService.isAdmin(principal)) {
            log.warn("管理端订阅被拒绝，user：{}，destination：{}", principal.getName(), destination);
            throw new AccessDeniedException("无管理端订阅权限");
        }
    }
}
```

### 发送权限校验

发送权限校验用于防止用户向非法目标发送消息。客户端只能向 `/app/**` 发送业务消息，不能直接向 `/topic/**` 或 `/user/**` 发送消息，否则可能绕过服务端业务校验。

发送权限建议如下：

| 发送路径                 | 校验规则                       |
| ------------------------ | ------------------------------ |
| `/app/chat/private/send` | 校验发送人、接收人关系、黑名单 |
| `/app/chat/group/send`   | 校验群组成员身份和禁言状态     |
| `/app/notice/read`       | 只能操作自己的通知             |
| `/app/admin/**`          | 必须具备管理员权限             |
| `/topic/**`              | 禁止客户端直接发送             |
| `/user/**`               | 禁止客户端直接发送             |

发送鉴权逻辑示例：

```java
private void handleSend(StompHeaderAccessor accessor) {
    Principal principal = accessor.getUser();
    String destination = accessor.getDestination();

    if (principal == null) {
        throw new AccessDeniedException("用户未认证");
    }

    if (StrUtil.isBlank(destination)) {
        throw new AccessDeniedException("发送地址不能为空");
    }

    if (!StrUtil.startWith(destination, "/app")) {
        log.warn("非法发送地址，user：{}，destination：{}", principal.getName(), destination);
        throw new AccessDeniedException("非法发送地址");
    }

    if (StrUtil.startWith(destination, "/app/admin") && !authUserService.isAdmin(principal)) {
        log.warn("管理端发送被拒绝，user：{}，destination：{}", principal.getName(), destination);
        throw new AccessDeniedException("无管理端发送权限");
    }
}
```

发送权限不能只根据路径判断。具体业务中还需要在 `@MessageMapping` 对应方法或 Service 中校验业务对象权限，例如是否能向某用户发送消息、是否能操作某条通知、是否能进入某个群组。

### 群组权限校验

群组权限校验用于控制群组订阅、群组消息发送、群组成员列表查看、群组在线状态查看、群组公告接收等行为。群组权限不能只由前端控制，必须由服务端根据当前用户身份和群组成员关系判断。

群组权限校验点如下：

| 操作         | 校验规则                     |
| ------------ | ---------------------------- |
| 订阅群组     | 用户必须是群成员             |
| 发送群消息   | 用户必须是群成员，且未被禁言 |
| 查看成员状态 | 用户必须是群成员或管理员     |
| 群组公告     | 用户必须属于群组             |
| 踢出成员     | 用户必须是群主或管理员       |
| 解散群组     | 用户必须是群主或系统管理员   |

群组订阅路径解析示例：

```java
String groupId = ReUtil.getGroup1("^/topic/group/([^/]+).*$", destination);
```

群组发送请求中即使包含 `groupId`，服务端也必须二次校验：

```java
@MessageMapping("/chat/group/send")
public void sendGroupMessage(@Valid @Payload ChatMessage request, Principal principal) {
    String userId = principal.getName();

    if (!authUserService.canSendGroup(userId, request.getGroupId())) {
        throw new AccessDeniedException("无群组发送权限");
    }

    request.setSenderId(userId);
    wsPushService.sendToGroup(request.getGroupId(), WsMessage.success(WsMessageType.GROUP_CHAT, request));
}
```

群组权限建议结合缓存优化。群成员关系可以缓存在 Redis 中，但最终变更应以数据库为准。成员退出、被踢出、禁言、群组解散时，需要及时清理缓存并通知在线客户端取消订阅或刷新页面状态。

### 管理端权限隔离

管理端权限隔离用于保护 `/topic/admin/**` 和 `/app/admin/**` 路径，防止普通用户订阅连接看板、在线用户、告警、消息统计、强制下线等敏感能力。

管理端路径建议如下：

| 类型           | 路径                             | 权限         |
| -------------- | -------------------------------- | ------------ |
| 管理端连接看板 | `/topic/admin/connection`        | 管理员、运维 |
| 管理端在线用户 | `/topic/admin/online`            | 管理员、运维 |
| 管理端告警     | `/topic/admin/alarm`             | 管理员、运维 |
| 管理端消息看板 | `/topic/admin/dashboard/message` | 管理员、运维 |
| 管理端踢人     | `/app/admin/session/kick`        | 管理员       |
| 管理端推送测试 | `/app/admin/push/test`           | 管理员       |

管理端隔离建议如下：

| 建议           | 说明                                           |
| -------------- | ---------------------------------------------- |
| 路径统一前缀   | 管理端订阅统一 `/topic/admin/**`               |
| 订阅阶段强校验 | `SUBSCRIBE` 时拒绝普通用户                     |
| 发送阶段强校验 | `SEND /app/admin/**` 时拒绝普通用户            |
| 数据内容脱敏   | 在线用户列表避免展示敏感 Token、完整 IP 等信息 |
| 操作审计       | 踢人、广播、推送测试必须记录操作日志           |
| 多租户隔离     | 租户管理员只能查看本租户数据                   |
| 告警限流       | 防止异常风暴刷屏管理端                         |

管理端权限隔离不能依赖前端路由隐藏。WebSocket 订阅路径可以被手工构造，服务端必须在 STOMP 层进行权限判断。


## 握手与通道拦截

握手与通道拦截是 WebSocket STOMP 安全控制的核心入口。`HandshakeInterceptor` 作用于 WebSocket HTTP Upgrade 握手阶段，适合做来源校验、参数解析、IP 获取和初步拦截；`ChannelInterceptor` 作用于 STOMP 消息通道，适合做 `CONNECT`、`SUBSCRIBE`、`SEND`、`DISCONNECT` 等命令级别的认证、鉴权、限流和非法路径拦截。

### HandshakeInterceptor

`HandshakeInterceptor` 在 WebSocket 握手前后执行。握手阶段还没有进入 STOMP 协议层，因此它拿不到 STOMP `CONNECT` Header，但可以读取 HTTP 请求中的 Query 参数、Cookie、Origin、IP、User-Agent 等信息。

适合在握手阶段处理的内容如下：

| 处理项         | 说明                                                   |
| -------------- | ------------------------------------------------------ |
| Origin 校验    | 拒绝非法来源站点                                       |
| IP 获取        | 记录客户端 IP，支持黑名单和审计                        |
| Query 参数解析 | 获取 `token`、`clientType`、`deviceId` 等参数          |
| Cookie 解析    | 兼容 Cookie 登录态                                     |
| 黑名单初筛     | 在连接升级前拦截明显非法请求                           |
| 属性传递       | 将握手阶段信息写入 `attributes`，供后续 STOMP 阶段使用 |

握手拦截器示例。

文件位置：`src/main/java/io/github/atengk/websocket/interceptor/WsHandshakeInterceptor.java`

```java
package io.github.atengk.websocket.interceptor;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.WsBlacklistService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * WebSocket 握手拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    private final WsBlacklistService wsBlacklistService;

    /**
     * 握手前处理
     *
     * @param request    请求对象
     * @param response   响应对象
     * @param wsHandler  WebSocket 处理器
     * @param attributes WebSocket 会话属性
     * @return 是否允许握手
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return true;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String ip = getClientIp(httpRequest);
        String token = getToken(httpRequest);
        String clientType = StrUtil.blankToDefault(httpRequest.getParameter("clientType"), "WEB");
        String deviceId = httpRequest.getParameter("deviceId");
        String userAgent = httpRequest.getHeader("User-Agent");
        String origin = httpRequest.getHeader("Origin");

        if (wsBlacklistService.isBlockedIp(ip)) {
            log.warn("WebSocket 握手被拒绝，IP 命中黑名单，ip：{}", ip);
            return false;
        }

        attributes.put("ip", ip);
        attributes.put("token", token);
        attributes.put("clientType", clientType);
        attributes.put("deviceId", deviceId);
        attributes.put("userAgent", userAgent);
        attributes.put("origin", origin);

        log.info("WebSocket 握手通过，ip：{}，clientType：{}，deviceId：{}", ip, clientType, deviceId);
        return true;
    }

    /**
     * 握手后处理
     *
     * @param request   请求对象
     * @param response  响应对象
     * @param wsHandler WebSocket 处理器
     * @param exception 异常信息
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               org.springframework.http.server.ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            log.warn("WebSocket 握手后发生异常：{}", exception.getMessage());
        }
    }

    /**
     * 获取 Token
     *
     * @param request HTTP 请求
     * @return Token
     */
    private String getToken(HttpServletRequest request) {
        String token = request.getParameter("token");
        if (StrUtil.isNotBlank(token)) {
            return token;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        Optional<Cookie> accessTokenCookie = Arrays.stream(cookies)
                .filter(cookie -> StrUtil.equalsAnyIgnoreCase(cookie.getName(), "access_token", "token"))
                .findFirst();

        return accessTokenCookie.map(Cookie::getValue).orElse(null);
    }

    /**
     * 获取客户端 IP
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            return StrUtil.splitTrim(forwardedFor, ",").get(0);
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }

        return request.getRemoteAddr();
    }
}
```

在 WebSocket 配置类中挂载握手拦截器。

```java
registry.addEndpoint(properties.getEndpoint())
        .addInterceptors(wsHandshakeInterceptor)
        .setAllowedOriginPatterns(allowedOrigins)
        .withSockJS();
```

### ChannelInterceptor

`ChannelInterceptor` 作用于 STOMP 入站或出站消息通道。对于认证、订阅鉴权、发送鉴权、黑名单、非法路径拦截等场景，通常拦截客户端入站通道，即 `configureClientInboundChannel`。

`ChannelInterceptor` 常见处理点如下：

| STOMP 命令    | 处理内容                           |
| ------------- | ---------------------------------- |
| `CONNECT`     | 解析 Token，绑定用户，注册 Session |
| `SUBSCRIBE`   | 校验订阅权限，拦截非法 Topic       |
| `SEND`        | 校验发送路径、业务权限、限流       |
| `DISCONNECT`  | 清理 Session，记录断开日志         |
| `UNSUBSCRIBE` | 可选记录取消订阅日志               |

入站通道拦截器完整示例。

文件位置：`src/main/java/io/github/atengk/websocket/interceptor/WsInboundChannelInterceptor.java`

```java
package io.github.atengk.websocket.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.model.StompUserPrincipal;
import io.github.atengk.websocket.model.WsSessionInfo;
import io.github.atengk.websocket.service.AuthUserService;
import io.github.atengk.websocket.service.WsBlacklistService;
import io.github.atengk.websocket.service.WsSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * WebSocket STOMP 入站通道拦截器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsInboundChannelInterceptor implements ChannelInterceptor {

    private final AuthUserService authUserService;

    private final WsSessionRegistry wsSessionRegistry;

    private final WsBlacklistService wsBlacklistService;

    /**
     * 消息进入通道前处理
     *
     * @param message 消息对象
     * @param channel 消息通道
     * @return 处理后的消息
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        } else if (StompCommand.DISCONNECT.equals(command)) {
            handleDisconnect(accessor);
        }

        return message;
    }

    /**
     * CONNECT 拦截处理
     *
     * @param accessor Header 访问器
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        Map<String, Object> sessionAttributes = MapUtil.emptyIfNull(accessor.getSessionAttributes());

        String ip = MapUtil.getStr(sessionAttributes, "ip");
        String clientType = MapUtil.getStr(sessionAttributes, "clientType", "WEB");
        String deviceId = MapUtil.getStr(sessionAttributes, "deviceId");
        String token = getToken(accessor, sessionAttributes);

        if (StrUtil.isNotBlank(ip) && wsBlacklistService.isBlockedIp(ip)) {
            log.warn("STOMP CONNECT 被拒绝，IP 命中黑名单，ip：{}，sessionId：{}", ip, sessionId);
            throw new AccessDeniedException("连接来源被拒绝");
        }

        String userId = authUserService.parseUserId(token);
        if (StrUtil.isBlank(userId)) {
            log.warn("STOMP CONNECT 认证失败，sessionId：{}，ip：{}", sessionId, ip);
            throw new AccessDeniedException("WebSocket 连接认证失败");
        }

        if (wsBlacklistService.isBlockedUser(userId)) {
            log.warn("STOMP CONNECT 被拒绝，用户命中黑名单，userId：{}，sessionId：{}", userId, sessionId);
            throw new AccessDeniedException("当前用户已被限制连接");
        }

        accessor.setUser(new StompUserPrincipal(userId));

        WsSessionInfo sessionInfo = WsSessionInfo.builder()
                .sessionId(sessionId)
                .userId(userId)
                .clientType(clientType)
                .deviceId(deviceId)
                .ip(ip)
                .connectTime(LocalDateTime.now())
                .lastActiveTime(LocalDateTime.now())
                .build();

        wsSessionRegistry.register(sessionInfo);
        log.info("STOMP CONNECT 认证成功，userId：{}，sessionId：{}，clientType：{}", userId, sessionId, clientType);
    }

    /**
     * SUBSCRIBE 拦截处理
     *
     * @param accessor Header 访问器
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        String destination = accessor.getDestination();

        checkAuthenticated(principal);
        checkDestination(destination);

        String userId = principal.getName();

        if (StrUtil.startWith(destination, "/topic/admin") && !authUserService.isAdmin(principal)) {
            log.warn("管理端订阅被拒绝，userId：{}，destination：{}", userId, destination);
            throw new AccessDeniedException("无管理端订阅权限");
        }

        if (StrUtil.startWith(destination, "/topic/group/")) {
            String groupId = ReUtil.getGroup1("^/topic/group/([^/]+).*$", destination);
            if (!authUserService.canSubscribeGroup(userId, groupId)) {
                log.warn("群组订阅被拒绝，userId：{}，groupId：{}，destination：{}", userId, groupId, destination);
                throw new AccessDeniedException("无群组订阅权限");
            }
        }

        wsSessionRegistry.refresh(accessor.getSessionId());
        log.info("STOMP SUBSCRIBE 校验通过，userId：{}，sessionId：{}，destination：{}",
                userId, accessor.getSessionId(), destination);
    }

    /**
     * SEND 拦截处理
     *
     * @param accessor Header 访问器
     */
    private void handleSend(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        String destination = accessor.getDestination();

        checkAuthenticated(principal);
        checkDestination(destination);

        String userId = principal.getName();

        if (!StrUtil.startWith(destination, "/app")) {
            log.warn("非法 SEND 地址，userId：{}，destination：{}", userId, destination);
            throw new AccessDeniedException("非法发送地址");
        }

        if (StrUtil.startWith(destination, "/app/admin") && !authUserService.isAdmin(principal)) {
            log.warn("管理端 SEND 被拒绝，userId：{}，destination：{}", userId, destination);
            throw new AccessDeniedException("无管理端发送权限");
        }

        wsSessionRegistry.refresh(accessor.getSessionId());
        log.info("STOMP SEND 校验通过，userId：{}，sessionId：{}，destination：{}",
                userId, accessor.getSessionId(), destination);
    }

    /**
     * DISCONNECT 拦截处理
     *
     * @param accessor Header 访问器
     */
    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();

        wsSessionRegistry.remove(sessionId);
        log.info("STOMP DISCONNECT 已处理，user：{}，sessionId：{}",
                principal == null ? "anonymous" : principal.getName(), sessionId);
    }

    /**
     * 获取 Token
     *
     * @param accessor          Header 访问器
     * @param sessionAttributes 会话属性
     * @return Token
     */
    private String getToken(StompHeaderAccessor accessor, Map<String, Object> sessionAttributes) {
        String authorization = getFirstNativeHeader(accessor, "Authorization");
        if (StrUtil.isNotBlank(authorization)) {
            return authorization;
        }

        String token = getFirstNativeHeader(accessor, "token");
        if (StrUtil.isNotBlank(token)) {
            return token;
        }

        return MapUtil.getStr(sessionAttributes, "token");
    }

    /**
     * 获取第一个原生 Header
     *
     * @param accessor Header 访问器
     * @param name     Header 名称
     * @return Header 值
     */
    private String getFirstNativeHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        return values.get(0);
    }

    /**
     * 校验用户是否已认证
     *
     * @param principal 当前用户
     */
    private void checkAuthenticated(Principal principal) {
        if (principal == null || StrUtil.isBlank(principal.getName())) {
            throw new AccessDeniedException("用户未认证");
        }
    }

    /**
     * 校验目标地址
     *
     * @param destination 目标地址
     */
    private void checkDestination(String destination) {
        if (StrUtil.isBlank(destination)) {
            throw new AccessDeniedException("目标地址不能为空");
        }

        if (StrUtil.containsAny(destination, "..", "\\", "//")) {
            throw new AccessDeniedException("目标地址非法");
        }
    }
}
```

在配置类中注册握手拦截器和入站通道拦截器。

```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    String[] allowedOrigins = properties.getAllowedOriginPatterns().toArray(new String[0]);

    registry.addEndpoint(properties.getEndpoint())
            .addInterceptors(wsHandshakeInterceptor)
            .setAllowedOriginPatterns(allowedOrigins)
            .withSockJS();
}

@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(wsInboundChannelInterceptor);
}
```

### CONNECT 拦截

`CONNECT` 拦截用于建立 STOMP 逻辑连接时认证用户身份。这个阶段应完成 Token 解析、用户状态校验、黑名单校验、Principal 绑定和 Session 注册。

`CONNECT` 阶段处理流程如下：

```text
1. 客户端发送 CONNECT 帧
2. 服务端读取 Authorization、token 或握手 attributes
3. 解析 Token，获取 userId
4. 校验用户是否有效
5. 校验用户或 IP 是否命中黑名单
6. 设置 Principal
7. 注册 Session
8. 允许连接继续建立
```

客户端示例：

```typescript
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws/stomp',
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
    clientType: 'WEB',
    deviceId: 'browser-001'
  },
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000
})
```

`CONNECT` 阶段建议只做连接级认证和轻量校验，不建议执行复杂业务查询。复杂权限应延迟到 `SUBSCRIBE` 和 `SEND` 阶段按目标地址校验。

### SUBSCRIBE 拦截

`SUBSCRIBE` 拦截用于校验客户端是否允许订阅目标地址。该阶段是防止越权读取消息的核心。任何客户端都可以手工构造订阅帧，因此不能依赖前端页面权限控制。

推荐订阅校验规则如下：

| 订阅路径                      | 校验方式             |
| ----------------------------- | -------------------- |
| `/topic/system/notice`        | 登录用户可订阅       |
| `/user/queue/**`              | 当前登录用户可订阅   |
| `/topic/group/{groupId}`      | 校验是否为群组成员   |
| `/topic/tenant/{tenantId}/**` | 校验是否属于租户     |
| `/topic/admin/**`             | 校验管理员或运维权限 |

订阅被拒绝时，应抛出访问拒绝异常，并记录用户、Session、目标地址和拒绝原因。

```java
if (StrUtil.startWith(destination, "/topic/admin") && !authUserService.isAdmin(principal)) {
    log.warn("管理端订阅被拒绝，userId：{}，destination：{}", userId, destination);
    throw new AccessDeniedException("无管理端订阅权限");
}
```

### SEND 拦截

`SEND` 拦截用于校验客户端是否允许向某个目标地址发送消息。客户端发送业务消息时必须进入 `/app/**`，不能直接向 `/topic/**`、`/queue/**` 或 `/user/**` 发送。

推荐发送校验规则如下：

| 发送路径                 | 处理规则                             |
| ------------------------ | ------------------------------------ |
| `/app/chat/private/send` | 登录用户可进入，业务层校验接收人关系 |
| `/app/chat/group/send`   | 登录用户可进入，业务层校验群组权限   |
| `/app/notice/read`       | 登录用户可进入，业务层校验通知归属   |
| `/app/admin/**`          | 管理员可发送                         |
| `/topic/**`              | 拒绝客户端直接发送                   |
| `/user/**`               | 拒绝客户端直接发送                   |
| `/queue/**`              | 拒绝客户端直接发送                   |

`SEND` 拦截负责通用路径权限，业务对象权限仍应在 `@MessageMapping` 或 Service 中二次校验。例如用户是否能给目标用户发消息、是否能向指定群组发言、是否能操作某条通知，这些都不能只依赖路径判断。

### DISCONNECT 拦截

`DISCONNECT` 拦截用于处理客户端主动断开连接时的轻量清理。实际生产中，连接断开不一定总能正常触发客户端主动 `DISCONNECT`，因此还需要结合 `SessionDisconnectEvent` 做兜底清理。

`DISCONNECT` 阶段可处理以下内容：

| 处理项           | 说明                             |
| ---------------- | -------------------------------- |
| 清理本地 Session | 从会话注册表移除当前 Session     |
| 更新最后活跃时间 | 记录连接结束时间                 |
| 记录断开日志     | 记录用户、Session、断开原因      |
| 清理订阅记录     | 可选，业务需要统计订阅时使用     |
| 在线状态判断     | 判断用户是否还有其他在线 Session |

建议将清理逻辑做成幂等操作。因为同一个 Session 的清理可能在 `DISCONNECT` 拦截和 `SessionDisconnectEvent` 中都发生，重复删除不能抛异常。

### Header 参数解析

Header 参数解析用于从 STOMP Native Header、握手 attributes、Query 参数或 Cookie 中获取认证和客户端信息。推荐优先级如下：

```text
1. STOMP CONNECT Header: Authorization
2. STOMP CONNECT Header: token
3. WebSocket 握手 attributes: token
4. Cookie: access_token 或 token
```

常见 Header 参数如下：

| 参数            | 来源                  | 说明                           |
| --------------- | --------------------- | ------------------------------ |
| `Authorization` | STOMP CONNECT Header  | 推荐传递 Token                 |
| `token`         | STOMP Header 或 Query | 兼容简单客户端                 |
| `clientType`    | Header 或 Query       | 客户端类型，如 WEB、APP、ADMIN |
| `deviceId`      | Header 或 Query       | 设备标识                       |
| `traceId`       | Header                | 链路追踪 ID                    |
| `tenantId`      | Header 或 Token       | 多租户标识                     |

Token 清洗示例：

```java
private String cleanToken(String token) {
    if (StrUtil.isBlank(token)) {
        return null;
    }
    return StrUtil.removePrefixIgnoreCase(token.trim(), "Bearer ");
}
```

Header 参数解析注意事项如下：

| 注意项            | 说明                                        |
| ----------------- | ------------------------------------------- |
| Header 名称大小写 | 建议兼容 `Authorization` 和 `authorization` |
| Token 脱敏日志    | 禁止打印完整 Token                          |
| 参数优先级        | Header 优先于 Query 和 Cookie               |
| 参数可信度        | 前端传入的 `userId` 不可信                  |
| 租户标识          | 多租户场景应优先从 Token 中解析             |

### 用户上下文注入

用户上下文注入用于把认证后的用户身份绑定到 WebSocket Session，使后续 `Principal#getName()`、点对点消息发送、权限判断和会话管理都能使用同一个用户标识。

用户上下文包含以下信息：

| 字段             | 说明                 |
| ---------------- | -------------------- |
| `userId`         | 用户唯一 ID          |
| `sessionId`      | WebSocket Session ID |
| `clientType`     | 客户端类型           |
| `deviceId`       | 设备 ID              |
| `ip`             | 客户端 IP            |
| `tenantId`       | 租户 ID，可选        |
| `connectTime`    | 连接时间             |
| `lastActiveTime` | 最后活跃时间         |

`Principal` 只建议保存稳定用户 ID。更复杂的上下文信息放入会话注册表或 Redis，不建议全部塞进 `Principal`。

```java
accessor.setUser(new StompUserPrincipal(userId));
```

后续在 Controller 中获取当前用户：

```java
@MessageMapping("/chat/private/send")
public void sendPrivateMessage(@Valid @Payload ChatMessage request, Principal principal) {
    String senderId = principal.getName();
    request.setSenderId(senderId);
    // 后续业务处理
}
```

### 黑名单拦截

黑名单拦截用于拒绝恶意 IP、风险用户、异常设备或被封禁账号建立连接、订阅主题或发送消息。黑名单可以在握手阶段初筛，也可以在 STOMP 通道阶段精确拦截。

黑名单服务接口如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsBlacklistService.java`

```java
package io.github.atengk.websocket.service;

/**
 * WebSocket 黑名单服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsBlacklistService {

    /**
     * 判断 IP 是否被拦截
     *
     * @param ip 客户端 IP
     * @return 是否拦截
     */
    boolean isBlockedIp(String ip);

    /**
     * 判断用户是否被拦截
     *
     * @param userId 用户 ID
     * @return 是否拦截
     */
    boolean isBlockedUser(String userId);

    /**
     * 判断设备是否被拦截
     *
     * @param deviceId 设备 ID
     * @return 是否拦截
     */
    boolean isBlockedDevice(String deviceId);
}
```

示例实现如下。生产环境建议改为查询 Redis、数据库或风控服务。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsBlacklistServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.WsBlacklistService;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * WebSocket 黑名单服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Service
public class WsBlacklistServiceImpl implements WsBlacklistService {

    private static final Set<String> BLOCKED_IPS = CollUtil.newHashSet("127.0.0.2");

    private static final Set<String> BLOCKED_USERS = CollUtil.newHashSet("blocked-user");

    private static final Set<String> BLOCKED_DEVICES = CollUtil.newHashSet("blocked-device");

    /**
     * 判断 IP 是否被拦截
     *
     * @param ip 客户端 IP
     * @return 是否拦截
     */
    @Override
    public boolean isBlockedIp(String ip) {
        return StrUtil.isNotBlank(ip) && BLOCKED_IPS.contains(ip);
    }

    /**
     * 判断用户是否被拦截
     *
     * @param userId 用户 ID
     * @return 是否拦截
     */
    @Override
    public boolean isBlockedUser(String userId) {
        return StrUtil.isNotBlank(userId) && BLOCKED_USERS.contains(userId);
    }

    /**
     * 判断设备是否被拦截
     *
     * @param deviceId 设备 ID
     * @return 是否拦截
     */
    @Override
    public boolean isBlockedDevice(String deviceId) {
        return StrUtil.isNotBlank(deviceId) && BLOCKED_DEVICES.contains(deviceId);
    }
}
```

黑名单拦截建议如下：

| 拦截对象     | 拦截阶段                 |
| ------------ | ------------------------ |
| IP 黑名单    | 握手阶段优先拦截         |
| 用户黑名单   | CONNECT 阶段拦截         |
| 设备黑名单   | CONNECT 阶段拦截         |
| 高频发送用户 | SEND 阶段限流或封禁      |
| 非法订阅用户 | SUBSCRIBE 阶段记录并拦截 |

### 非法目标地址拦截

非法目标地址拦截用于防止客户端发送或订阅未开放的路径。STOMP 的 destination 本质上是字符串，如果不做限制，客户端可以尝试订阅任意路径。

非法地址示例：

```text
/topic/admin/alarm              普通用户非法订阅管理端主题
/topic/group/other-group        非成员非法订阅群组
/app/admin/session/kick         普通用户非法发送管理端命令
/topic/system/notice            客户端非法 SEND 到广播主题
/user/queue/notice              客户端非法 SEND 到用户队列
/app/../../admin                路径穿越式异常地址
```

推荐拦截规则如下：

| 类型                                  | 规则 |
| ------------------------------------- | ---- |
| 空地址                                | 拒绝 |
| 包含 `..`                             | 拒绝 |
| 包含 `//`                             | 拒绝 |
| 包含反斜杠                            | 拒绝 |
| `SEND` 非 `/app/**`                   | 拒绝 |
| 普通用户订阅 `/topic/admin/**`        | 拒绝 |
| 非群成员订阅 `/topic/group/{groupId}` | 拒绝 |

非法地址拦截应尽早执行，先做格式校验，再做权限校验。格式非法的请求可以直接拒绝，不需要进入业务权限查询。

## 会话管理

会话管理用于维护 WebSocket Session、用户、设备、客户端类型、IP、连接时间、最后活跃时间之间的关系。它是在线状态、点对点推送、强制下线、多端登录控制、会话并发限制和管理端连接看板的基础。

### Session 注册

Session 注册发生在 `CONNECT` 认证成功后。注册时应保存 `sessionId`、`userId`、`clientType`、`deviceId`、`ip`、`connectTime`、`lastActiveTime` 等信息。

会话模型如下。

文件位置：`src/main/java/io/github/atengk/websocket/model/WsSessionInfo.java`

```java
package io.github.atengk.websocket.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebSocket 会话信息
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
public class WsSessionInfo {

    /**
     * WebSocket Session ID
     */
    private String sessionId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 客户端类型
     */
    private String clientType;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 客户端 IP
     */
    private String ip;

    /**
     * 连接时间
     */
    private LocalDateTime connectTime;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;
}
```

会话注册表接口如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsSessionRegistry.java`

```java
package io.github.atengk.websocket.service;

import io.github.atengk.websocket.model.WsSessionInfo;

import java.util.List;
import java.util.Optional;

/**
 * WebSocket 会话注册表
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsSessionRegistry {

    /**
     * 注册会话
     *
     * @param sessionInfo 会话信息
     */
    void register(WsSessionInfo sessionInfo);

    /**
     * 根据 Session ID 查询会话
     *
     * @param sessionId Session ID
     * @return 会话信息
     */
    Optional<WsSessionInfo> getBySessionId(String sessionId);

    /**
     * 根据用户 ID 查询会话列表
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    List<WsSessionInfo> getByUserId(String userId);

    /**
     * 刷新会话活跃时间
     *
     * @param sessionId Session ID
     */
    void refresh(String sessionId);

    /**
     * 移除会话
     *
     * @param sessionId Session ID
     */
    void remove(String sessionId);

    /**
     * 判断用户是否在线
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    boolean isOnline(String userId);

    /**
     * 查询全部会话
     *
     * @return 会话列表
     */
    List<WsSessionInfo> listAll();
}
```

内存版实现如下，适合单实例或开发阶段。集群部署时应扩展为 Redis 实现。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/InMemoryWsSessionRegistry.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.model.WsSessionInfo;
import io.github.atengk.websocket.service.WsSessionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版 WebSocket 会话注册表
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class InMemoryWsSessionRegistry implements WsSessionRegistry {

    private final ConcurrentHashMap<String, WsSessionInfo> sessionMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Set<String>> userSessionIndex = new ConcurrentHashMap<>();

    /**
     * 注册会话
     *
     * @param sessionInfo 会话信息
     */
    @Override
    public void register(WsSessionInfo sessionInfo) {
        if (sessionInfo == null || StrUtil.hasBlank(sessionInfo.getSessionId(), sessionInfo.getUserId())) {
            log.warn("WebSocket Session 注册失败，会话信息不完整");
            return;
        }

        sessionMap.put(sessionInfo.getSessionId(), sessionInfo);
        userSessionIndex.computeIfAbsent(sessionInfo.getUserId(), key -> ConcurrentHashMap.newKeySet())
                .add(sessionInfo.getSessionId());

        log.info("WebSocket Session 注册成功，userId：{}，sessionId：{}",
                sessionInfo.getUserId(), sessionInfo.getSessionId());
    }

    /**
     * 根据 Session ID 查询会话
     *
     * @param sessionId Session ID
     * @return 会话信息
     */
    @Override
    public Optional<WsSessionInfo> getBySessionId(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessionMap.get(sessionId));
    }

    /**
     * 根据用户 ID 查询会话列表
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    @Override
    public List<WsSessionInfo> getByUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return List.of();
        }

        Set<String> sessionIds = userSessionIndex.get(userId);
        if (CollUtil.isEmpty(sessionIds)) {
            return List.of();
        }

        return sessionIds.stream()
                .map(sessionMap::get)
                .filter(sessionInfo -> sessionInfo != null)
                .toList();
    }

    /**
     * 刷新会话活跃时间
     *
     * @param sessionId Session ID
     */
    @Override
    public void refresh(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }

        WsSessionInfo sessionInfo = sessionMap.get(sessionId);
        if (sessionInfo == null) {
            return;
        }

        sessionInfo.setLastActiveTime(LocalDateTime.now());
    }

    /**
     * 移除会话
     *
     * @param sessionId Session ID
     */
    @Override
    public void remove(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }

        WsSessionInfo sessionInfo = sessionMap.remove(sessionId);
        if (sessionInfo == null) {
            return;
        }

        Set<String> sessionIds = userSessionIndex.get(sessionInfo.getUserId());
        if (CollUtil.isNotEmpty(sessionIds)) {
            sessionIds.remove(sessionId);
            if (sessionIds.isEmpty()) {
                userSessionIndex.remove(sessionInfo.getUserId());
            }
        }

        log.info("WebSocket Session 已移除，userId：{}，sessionId：{}", sessionInfo.getUserId(), sessionId);
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    @Override
    public boolean isOnline(String userId) {
        return CollUtil.isNotEmpty(userSessionIndex.get(userId));
    }

    /**
     * 查询全部会话
     *
     * @return 会话列表
     */
    @Override
    public List<WsSessionInfo> listAll() {
        return new ArrayList<>(sessionMap.values());
    }
}
```

### Session 查询

Session 查询用于管理端查看在线连接、业务层判断用户是否在线、推送层判断是否需要离线补偿。常见查询方式包括按 Session ID 查询、按用户 ID 查询、查询全部会话、按客户端类型查询等。

基础查询示例：

```java
Optional<WsSessionInfo> sessionInfo = wsSessionRegistry.getBySessionId("session-001");

List<WsSessionInfo> userSessions = wsSessionRegistry.getByUserId("10001");

boolean online = wsSessionRegistry.isOnline("10001");

List<WsSessionInfo> allSessions = wsSessionRegistry.listAll();
```

Session 查询建议如下：

| 查询类型           | 使用场景                   |
| ------------------ | -------------------------- |
| 按 Session ID 查询 | 指定会话响应、踢出指定连接 |
| 按用户 ID 查询     | 点对点推送、多端登录判断   |
| 查询全部会话       | 管理端在线连接看板         |
| 按客户端类型查询   | 区分 WEB、APP、ADMIN       |
| 按 IP 查询         | 风控、黑名单、异常连接排查 |

### Session 续期

Session 续期用于更新连接的最后活跃时间。每次客户端发送 `SEND`、`SUBSCRIBE`，或者服务端收到心跳、业务消息时，都可以刷新 Session 活跃时间。

当前文档示例中，在 `SUBSCRIBE` 和 `SEND` 拦截阶段调用：

```java
wsSessionRegistry.refresh(accessor.getSessionId());
```

Session 续期建议如下：

| 续期触发点    | 说明                           |
| ------------- | ------------------------------ |
| CONNECT 成功  | 设置初始连接时间和最后活跃时间 |
| SEND          | 用户主动发送业务消息时刷新     |
| SUBSCRIBE     | 用户订阅主题时刷新             |
| 心跳检测      | 可结合心跳刷新活跃时间         |
| HTTP 补偿查询 | 可选，说明用户仍在使用系统     |

如果使用 Redis 保存 Session，应给 Session 设置 TTL，并在刷新时续期。TTL 应大于心跳间隔和网关超时时间，例如 2 到 5 分钟。

### Session 清理

Session 清理用于连接断开、异常关闭、超时过期、服务重启、强制下线等场景。清理逻辑必须幂等，避免同一个 Session 被多次清理时报错。

清理触发点如下：

| 触发点                   | 说明                     |
| ------------------------ | ------------------------ |
| `DISCONNECT` 拦截        | 客户端主动断开           |
| `SessionDisconnectEvent` | 服务端收到断开事件       |
| 超时扫描任务             | 清理长期无活跃的 Session |
| 强制下线                 | 管理端或安全策略触发     |
| 应用关闭                 | 服务停止前清理本实例状态 |

断开事件兜底清理示例。

文件位置：`src/main/java/io/github/atengk/websocket/event/WsSessionEventListener.java`

```java
package io.github.atengk.websocket.event;

import io.github.atengk.websocket.service.WsSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * WebSocket 会话事件监听器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsSessionEventListener {

    private final WsSessionRegistry wsSessionRegistry;

    /**
     * 监听连接断开事件
     *
     * @param event 断开事件
     */
    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();

        wsSessionRegistry.remove(sessionId);

        log.info("WebSocket 断开事件已清理，user：{}，sessionId：{}，closeStatus：{}",
                user == null ? "anonymous" : user.getName(),
                sessionId,
                event.getCloseStatus());
    }
}
```

### 用户与 Session 绑定

用户与 Session 绑定用于支持一个用户一个连接、一个用户多个连接、指定会话推送、强制下线和在线状态统计。绑定关系一般包括两个方向：

```text
sessionId -> WsSessionInfo
userId -> sessionId 集合
```

这种双向索引可以支持以下能力：

| 能力                   | 查询方式                              |
| ---------------------- | ------------------------------------- |
| 根据 sessionId 找用户  | `sessionId -> WsSessionInfo`          |
| 根据 userId 找所有连接 | `userId -> sessionId 集合`            |
| 判断用户是否在线       | 判断 userId 是否存在有效 session      |
| 踢出指定连接           | 按 sessionId 定向推送下线通知         |
| 踢出用户全部连接       | 按 userId 查询全部 session 后逐个处理 |

多端在线时，同一个用户可能有多个 Session：

```text
userId=10001
    ├── sessionId=web-session-001
    ├── sessionId=app-session-002
    └── sessionId=admin-session-003
```

用户离线判断不能只看单个 Session 断开，必须判断该用户是否还有其他 Session。

### 多端登录管理

多端登录管理用于控制同一个用户是否可以同时在多个客户端在线，例如 Web、App、Pad、管理端等。不同系统策略不同，应在设计阶段明确。

常见策略如下：

| 策略         | 说明                         |
| ------------ | ---------------------------- |
| 允许多端在线 | 同一用户多个设备可同时连接   |
| 同端互踢     | 同一客户端类型只保留一个连接 |
| 全端唯一     | 同一用户只允许一个连接       |
| 管理端独立   | 管理端和普通端分开控制       |
| 设备数限制   | 同一用户最多 N 个在线设备    |

多端登录策略建议如下：

| 场景             | 推荐策略                   |
| ---------------- | -------------------------- |
| 普通业务系统     | 允许多端在线               |
| 后台管理系统     | 同端互踢或全端唯一         |
| 金融或高安全系统 | 全端唯一或设备数限制       |
| 聊天系统         | 允许多端在线，消息多端同步 |
| 设备控制系统     | 按设备 ID 控制连接         |

多端登录控制通常在 `CONNECT` 阶段完成。新连接认证成功后，查询该用户已有 Session，根据策略决定允许连接、拒绝连接或踢出旧连接。

### 同用户多连接管理

同用户多连接管理用于处理同一用户多个连接的推送、下线、在线状态和并发限制问题。

同用户多连接下的推送策略如下：

| 推送类型     | 推荐策略                             |
| ------------ | ------------------------------------ |
| 个人通知     | 推送给该用户所有在线端               |
| 单聊消息     | 推送给该用户所有在线端，实现多端同步 |
| 当前操作 ACK | 只推送给当前 Session                 |
| 错误提示     | 通常只推送给当前 Session             |
| 强制下线     | 可指定 Session 或全部 Session        |
| 未读数变化   | 推送给所有在线端                     |

指定 Session 推送需要带上 Session Header。前文 `WsPushService#sendToSession` 已给出实现方式：

```java
wsPushService.sendToSession("10001", "session-001", "/queue/session", kickMessage);
```

需要注意，Spring STOMP 原生更擅长“按用户推送”，不是强会话控制框架。指定 Session 推送适合软控制；如果需要服务端强制关闭底层 WebSocket 连接，需要额外保存 `WebSocketSession` 或使用装饰器扩展。

### 踢出指定连接

踢出指定连接用于只让某个 Session 下线，不影响同一用户其他设备。推荐方式是向该 Session 推送下线消息，由客户端收到后主动断开连接，并跳转登录页或展示下线原因。

下线消息体示例：

```json
{
  "messageId": "1909550919275921415",
  "type": "SESSION_KICKED",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "reason": "当前设备已被管理员强制下线"
  }
}
```

踢出服务示例。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsSessionControlService.java`

```java
package io.github.atengk.websocket.service;

/**
 * WebSocket 会话控制服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsSessionControlService {

    /**
     * 踢出指定连接
     *
     * @param sessionId 会话 ID
     * @param reason    原因
     */
    void kickSession(String sessionId, String reason);

    /**
     * 强制用户全部连接下线
     *
     * @param userId 用户 ID
     * @param reason 原因
     */
    void kickUser(String userId, String reason);
}
```

会话控制实现如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsSessionControlServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.enums.WsMessageType;
import io.github.atengk.websocket.model.WsMessage;
import io.github.atengk.websocket.model.WsSessionInfo;
import io.github.atengk.websocket.service.WsPushService;
import io.github.atengk.websocket.service.WsSessionControlService;
import io.github.atengk.websocket.service.WsSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * WebSocket 会话控制服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsSessionControlServiceImpl implements WsSessionControlService {

    private final WsSessionRegistry wsSessionRegistry;

    private final WsPushService wsPushService;

    /**
     * 踢出指定连接
     *
     * @param sessionId 会话 ID
     * @param reason    原因
     */
    @Override
    public void kickSession(String sessionId, String reason) {
        if (StrUtil.isBlank(sessionId)) {
            log.warn("踢出指定连接失败，sessionId 为空");
            return;
        }

        WsSessionInfo sessionInfo = wsSessionRegistry.getBySessionId(sessionId).orElse(null);
        if (sessionInfo == null) {
            log.warn("踢出指定连接失败，Session 不存在，sessionId：{}", sessionId);
            return;
        }

        Map<String, Object> payload = MapUtil.<String, Object>builder()
                .put("reason", StrUtil.blankToDefault(reason, "当前连接已被强制下线"))
                .put("sessionId", sessionId)
                .build();

        wsPushService.sendToSession(
                sessionInfo.getUserId(),
                sessionInfo.getSessionId(),
                "/queue/session",
                WsMessage.success(WsMessageType.SESSION_KICKED, payload)
        );

        wsSessionRegistry.remove(sessionId);
        log.info("指定连接已触发下线，userId：{}，sessionId：{}", sessionInfo.getUserId(), sessionId);
    }

    /**
     * 强制用户全部连接下线
     *
     * @param userId 用户 ID
     * @param reason 原因
     */
    @Override
    public void kickUser(String userId, String reason) {
        if (StrUtil.isBlank(userId)) {
            log.warn("强制用户下线失败，userId 为空");
            return;
        }

        wsSessionRegistry.getByUserId(userId)
                .forEach(sessionInfo -> kickSession(sessionInfo.getSessionId(), reason));

        log.info("用户全部连接已触发下线，userId：{}", userId);
    }
}
```

为使上述代码可编译，需要在消息类型枚举中补充会话下线类型：

```java
/**
 * 会话被踢出
 */
SESSION_KICKED("会话被踢出");
```

前端收到 `/user/queue/session` 消息后应主动断开连接：

```typescript
client.subscribe('/user/queue/session', (message) => {
  const body = JSON.parse(message.body)

  if (body.type === 'SESSION_KICKED') {
    client.deactivate()
    alert(body.data?.reason || '当前连接已下线')
    window.location.href = '/login'
  }
})
```

### 强制下线

强制下线用于让某个用户的全部在线连接断开，常见于管理员封禁账号、用户修改密码、Token 失效、账户异常风险、单点登录互踢等场景。

强制下线流程如下：

```text
1. 管理员或系统策略触发强制下线
2. 查询 userId 对应的全部 Session
3. 逐个向 /user/queue/session 推送下线消息
4. 客户端收到后主动断开连接
5. 服务端清理 Session 注册表
6. 后续 SEND/SUBSCRIBE 因 Session 无效被拒绝
```

强制下线与踢出指定连接的区别如下：

| 类型         | 范围                 | 场景                   |
| ------------ | -------------------- | ---------------------- |
| 踢出指定连接 | 单个 Session         | 踢出某台设备           |
| 强制用户下线 | 用户全部 Session     | 封禁、改密、Token 失效 |
| 同端互踢     | 某类客户端旧 Session | Web 端只保留一个登录   |
| 全端唯一     | 所有旧 Session       | 单点登录模式           |

严格意义上的“硬关闭底层 WebSocket 连接”需要服务端保存 `WebSocketSession` 并调用 `close()`。Spring STOMP 常规业务更推荐通知式下线：先向客户端发送下线消息，再由客户端主动断开，并由服务端拒绝该 Session 后续消息。

### 会话并发限制

会话并发限制用于控制同一用户、同一 IP、同一设备或同一客户端类型的最大连接数量。它可以防止恶意连接、浏览器重复开页、脚本刷连接和资源耗尽。

常见限制维度如下：

| 限制维度       | 示例                         |
| -------------- | ---------------------------- |
| 用户总连接数   | 每个用户最多 5 个连接        |
| 用户同端连接数 | 每个用户 Web 端最多 1 个连接 |
| IP 连接数      | 每个 IP 最多 100 个连接      |
| 设备连接数     | 每个设备最多 1 个连接        |
| 管理端连接数   | 管理端账号最多 1 个连接      |

并发限制建议在 `CONNECT` 认证成功后执行。此时已经知道用户 ID、客户端类型、设备 ID 和 IP，可以查询已有 Session 并应用策略。

示例策略：

```text
普通用户：
- 总连接数最多 5
- 同一 deviceId 最多 1

管理端用户：
- ADMIN 客户端最多 1
- 新连接成功后踢出旧 ADMIN 连接
```

并发限制伪代码如下：

```java
private void checkSessionLimit(String userId, String clientType) {
    List<WsSessionInfo> sessions = wsSessionRegistry.getByUserId(userId);

    if (sessions.size() >= 5) {
        throw new AccessDeniedException("当前用户连接数已达到上限");
    }

    long sameClientTypeCount = sessions.stream()
            .filter(session -> StrUtil.equalsIgnoreCase(session.getClientType(), clientType))
            .count();

    if (StrUtil.equalsIgnoreCase(clientType, "ADMIN") && sameClientTypeCount >= 1) {
        throw new AccessDeniedException("管理端只允许一个连接在线");
    }
}
```

如果业务希望“新连接踢出旧连接”，则不要直接拒绝新连接，而是在新连接注册前后调用会话控制服务踢出旧 Session。对于集群部署，该能力需要 Redis Session 注册表和跨实例下线事件，否则只能踢出当前实例上的连接。


## 在线状态管理

在线状态管理用于维护用户在 WebSocket STOMP 通道中的实时连接状态，包括上线、下线、多端在线、群组在线成员、心跳保活、超时检测、状态变更广播、状态持久化和 Redis 分布式同步。在线状态不应简单等同于“某个 Session 是否存在”，而应综合用户、设备、客户端类型、连接时间、最后活跃时间和多端连接情况进行判断。

### 用户上线事件

用户上线事件通常发生在 STOMP `CONNECT` 认证成功之后。此时服务端已经解析出用户身份，并注册了当前 WebSocket Session，可以将用户状态更新为在线，并向相关订阅者广播状态变化。

用户上线处理流程如下：

```text
1. 客户端建立 WebSocket 连接
2. 客户端发送 STOMP CONNECT
3. 服务端解析 Token，获取 userId
4. 服务端注册 Session
5. 更新用户在线状态
6. 广播用户上线事件
7. 管理端在线看板刷新
```

上线事件建议包含以下字段：

| 字段         | 说明                           |
| ------------ | ------------------------------ |
| `userId`     | 用户 ID                        |
| `sessionId`  | 当前连接 Session ID            |
| `clientType` | 客户端类型，如 WEB、APP、ADMIN |
| `deviceId`   | 设备 ID                        |
| `ip`         | 客户端 IP                      |
| `onlineAt`   | 上线时间                       |
| `status`     | 在线状态，通常为 `ONLINE`      |

在线状态枚举建议如下。

文件位置：`src/main/java/io/github/atengk/websocket/enums/OnlineStatusEnum.java`

```java
package io.github.atengk.websocket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 在线状态枚举
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@RequiredArgsConstructor
public enum OnlineStatusEnum {

    /**
     * 在线
     */
    ONLINE("在线"),

    /**
     * 离线
     */
    OFFLINE("离线"),

    /**
     * 忙碌
     */
    BUSY("忙碌"),

    /**
     * 离开
     */
    AWAY("离开"),

    /**
     * 隐身
     */
    INVISIBLE("隐身"),

    /**
     * 被踢出
     */
    KICKED("被踢出"),

    /**
     * 连接超时
     */
    TIMEOUT("连接超时");

    private final String description;
}
```

在线状态消息体如下。

文件位置：`src/main/java/io/github/atengk/websocket/dto/OnlineStatusMessage.java`

```java
package io.github.atengk.websocket.dto;

import io.github.atengk.websocket.enums.OnlineStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在线状态消息体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
public class OnlineStatusMessage {

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 在线状态
     */
    private OnlineStatusEnum status;

    /**
     * 客户端类型
     */
    private String clientType;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 客户端 IP
     */
    private String ip;

    /**
     * 在线时间
     */
    private LocalDateTime onlineAt;

    /**
     * 离线时间
     */
    private LocalDateTime offlineAt;

    /**
     * 状态变更原因
     */
    private String reason;
}
```

### 用户下线事件

用户下线事件通常发生在 `SessionDisconnectEvent`、`DISCONNECT` 拦截、心跳超时、强制下线或服务端清理 Session 时。下线处理必须支持幂等，因为同一个连接可能同时触发多个清理入口。

下线处理流程如下：

```text
1. 连接断开或 Session 被清理
2. 从 Session 注册表移除当前 sessionId
3. 查询该用户是否还有其他有效 Session
4. 如果还有其他 Session，用户仍视为在线
5. 如果没有其他 Session，用户状态更新为离线
6. 广播用户下线事件
7. 管理端在线看板刷新
```

用户下线时不能直接根据当前 Session 断开就判定用户离线。多端登录场景下，用户可能仍然在 App 或管理端保持在线。

下线判断建议如下：

| 场景                                     | 判断结果                    |
| ---------------------------------------- | --------------------------- |
| 用户只有一个 Session，断开后无其他连接   | 用户离线                    |
| 用户有 Web 和 App 两个 Session，Web 断开 | 用户仍在线                  |
| 用户所有 Session 被清理                  | 用户离线                    |
| 某个 Session 心跳超时                    | 清理该 Session 后重新判断   |
| 用户被强制全端下线                       | 所有 Session 清理后用户离线 |

在线状态服务接口如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsOnlineStatusService.java`

```java
package io.github.atengk.websocket.service;

import io.github.atengk.websocket.dto.OnlineStatusMessage;
import io.github.atengk.websocket.model.WsSessionInfo;

import java.util.List;

/**
 * WebSocket 在线状态服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsOnlineStatusService {

    /**
     * 处理用户上线
     *
     * @param sessionInfo 会话信息
     */
    void online(WsSessionInfo sessionInfo);

    /**
     * 处理 Session 下线
     *
     * @param sessionId 会话 ID
     * @param reason    下线原因
     */
    void offline(String sessionId, String reason);

    /**
     * 判断用户是否在线
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    boolean isOnline(String userId);

    /**
     * 查询在线用户列表
     *
     * @return 在线用户列表
     */
    List<OnlineStatusMessage> listOnlineUsers();
}
```

在线状态服务实现会根据会话注册表判断用户是否还有其他连接，并在必要时广播状态变化。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsOnlineStatusServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.dto.OnlineStatusMessage;
import io.github.atengk.websocket.enums.OnlineStatusEnum;
import io.github.atengk.websocket.enums.WsMessageType;
import io.github.atengk.websocket.model.WsMessage;
import io.github.atengk.websocket.model.WsSessionInfo;
import io.github.atengk.websocket.service.WsOnlineStatusService;
import io.github.atengk.websocket.service.WsPushService;
import io.github.atengk.websocket.service.WsSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * WebSocket 在线状态服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsOnlineStatusServiceImpl implements WsOnlineStatusService {

    private final WsSessionRegistry wsSessionRegistry;

    private final WsPushService wsPushService;

    /**
     * 处理用户上线
     *
     * @param sessionInfo 会话信息
     */
    @Override
    public void online(WsSessionInfo sessionInfo) {
        if (sessionInfo == null || StrUtil.hasBlank(sessionInfo.getUserId(), sessionInfo.getSessionId())) {
            log.warn("用户上线处理失败，会话信息不完整");
            return;
        }

        OnlineStatusMessage message = OnlineStatusMessage.builder()
                .userId(sessionInfo.getUserId())
                .sessionId(sessionInfo.getSessionId())
                .status(OnlineStatusEnum.ONLINE)
                .clientType(sessionInfo.getClientType())
                .deviceId(sessionInfo.getDeviceId())
                .ip(sessionInfo.getIp())
                .onlineAt(LocalDateTime.now())
                .reason("CONNECT_SUCCESS")
                .build();

        wsPushService.broadcast("/topic/online/status", WsMessage.success(WsMessageType.ONLINE_STATUS, message));
        wsPushService.broadcast("/topic/admin/online", WsMessage.success(WsMessageType.ONLINE_STATUS, message));

        log.info("用户上线状态已广播，userId：{}，sessionId：{}", sessionInfo.getUserId(), sessionInfo.getSessionId());
    }

    /**
     * 处理 Session 下线
     *
     * @param sessionId 会话 ID
     * @param reason    下线原因
     */
    @Override
    public void offline(String sessionId, String reason) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }

        WsSessionInfo sessionInfo = wsSessionRegistry.getBySessionId(sessionId).orElse(null);
        if (sessionInfo == null) {
            log.info("Session 已不存在，无需重复下线，sessionId：{}", sessionId);
            return;
        }

        String userId = sessionInfo.getUserId();
        wsSessionRegistry.remove(sessionId);

        List<WsSessionInfo> remainSessions = wsSessionRegistry.getByUserId(userId);
        if (CollUtil.isNotEmpty(remainSessions)) {
            log.info("用户仍有其他在线连接，不广播离线，userId：{}，剩余连接数：{}", userId, remainSessions.size());
            return;
        }

        OnlineStatusMessage message = OnlineStatusMessage.builder()
                .userId(userId)
                .sessionId(sessionId)
                .status(OnlineStatusEnum.OFFLINE)
                .clientType(sessionInfo.getClientType())
                .deviceId(sessionInfo.getDeviceId())
                .ip(sessionInfo.getIp())
                .offlineAt(LocalDateTime.now())
                .reason(StrUtil.blankToDefault(reason, "DISCONNECT"))
                .build();

        wsPushService.broadcast("/topic/online/status", WsMessage.success(WsMessageType.ONLINE_STATUS, message));
        wsPushService.broadcast("/topic/admin/online", WsMessage.success(WsMessageType.ONLINE_STATUS, message));

        log.info("用户离线状态已广播，userId：{}，sessionId：{}，reason：{}", userId, sessionId, reason);
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    @Override
    public boolean isOnline(String userId) {
        return wsSessionRegistry.isOnline(userId);
    }

    /**
     * 查询在线用户列表
     *
     * @return 在线用户列表
     */
    @Override
    public List<OnlineStatusMessage> listOnlineUsers() {
        return wsSessionRegistry.listAll()
                .stream()
                .sorted(Comparator.comparing(WsSessionInfo::getConnectTime))
                .map(sessionInfo -> OnlineStatusMessage.builder()
                        .userId(sessionInfo.getUserId())
                        .sessionId(sessionInfo.getSessionId())
                        .status(OnlineStatusEnum.ONLINE)
                        .clientType(sessionInfo.getClientType())
                        .deviceId(sessionInfo.getDeviceId())
                        .ip(sessionInfo.getIp())
                        .onlineAt(sessionInfo.getConnectTime())
                        .build())
                .toList();
    }
}
```

### 多端在线状态

多端在线状态用于处理同一用户在多个设备、多个浏览器标签页、多个客户端类型同时连接的场景。在线状态应区分“用户在线”和“连接在线”两个概念。

```text
用户在线：
userId = 10001 至少存在一个有效 Session

连接在线：
sessionId = web-session-001 当前仍然有效
```

多端状态建议设计如下：

| 维度       | 示例                  | 说明                          |
| ---------- | --------------------- | ----------------------------- |
| 用户维度   | `userId=10001`        | 判断用户是否整体在线          |
| 会话维度   | `sessionId=xxx`       | 判断某个连接是否在线          |
| 客户端维度 | `clientType=WEB`      | 判断 Web、APP、ADMIN 是否在线 |
| 设备维度   | `deviceId=iphone-001` | 判断某台设备是否在线          |
| 租户维度   | `tenantId=1`          | 多租户在线统计                |

多端状态处理建议如下：

| 场景               | 处理方式                                     |
| ------------------ | -------------------------------------------- |
| 同一用户多个端在线 | 推送个人通知时默认全端同步                   |
| 当前页面操作错误   | 只推送当前 Session                           |
| 用户离线判断       | 所有 Session 都断开才离线                    |
| 强制下线           | 可按 Session、设备、客户端类型或用户维度下线 |
| 未读数变化         | 建议全端同步                                 |
| 聊天消息           | 建议全端同步并前端去重                       |

### 在线用户列表

在线用户列表用于管理端查看当前在线用户、客服系统查看在线客户、群组系统判断成员在线状态。在线列表可以从内存 Session 注册表、Redis 在线集合或数据库在线状态表中查询。

在线用户接口建议使用 HTTP 查询，不建议完全依赖 WebSocket 推送。WebSocket 用于实时变更通知，HTTP 用于初始化和补偿。

接口设计建议如下：

```text
GET /api/ws/online/users
GET /api/ws/online/users?clientType=WEB
GET /api/ws/online/users?tenantId=1
```

管理端查询控制器示例。

文件位置：`src/main/java/io/github/atengk/websocket/controller/WsOnlineController.java`

```java
package io.github.atengk.websocket.controller;

import io.github.atengk.websocket.dto.OnlineStatusMessage;
import io.github.atengk.websocket.service.WsOnlineStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WebSocket 在线状态查询接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequiredArgsConstructor
public class WsOnlineController {

    private final WsOnlineStatusService wsOnlineStatusService;

    /**
     * 查询在线用户列表
     *
     * @return 在线用户列表
     */
    @GetMapping("/api/ws/online/users")
    public List<OnlineStatusMessage> listOnlineUsers() {
        return wsOnlineStatusService.listOnlineUsers();
    }
}
```

在线用户列表建议返回脱敏数据，不要返回完整 Token、敏感 Cookie、完整设备指纹等信息。

### 群组在线成员

群组在线成员用于展示群聊、房间、项目空间中的在线成员数量和成员状态。该能力通常需要结合群组成员关系和在线状态数据进行交集计算。

群组在线成员计算方式如下：

```text
群组在线成员 = 群组成员列表 ∩ 在线用户集合
```

实现策略如下：

| 策略         | 说明                                  |
| ------------ | ------------------------------------- |
| 小群组       | 直接查询群成员，再逐个判断在线        |
| 大群组       | 使用 Redis Set 求交集                 |
| 房间在线人数 | 维护房间在线 Session Set              |
| 多端在线     | 同一用户多个 Session 只计一个在线成员 |
| 游客房间     | 可以按 Session 统计，不按用户统计     |

Redis 结构建议如下：

```text
ws:online:users                   Set，所有在线用户 ID
ws:user:sessions:{userId}          Set，用户在线 Session
ws:group:members:{groupId}         Set，群组成员 ID
ws:group:online:{groupId}          Set，群组在线用户 ID，可选冗余
ws:room:sessions:{roomId}          Set，房间在线 Session
```

群组在线成员推送路径建议如下：

| 场景             | 路径                            |
| ---------------- | ------------------------------- |
| 群组在线成员变化 | `/topic/group/{groupId}/online` |
| 群组成员状态变化 | `/topic/group/{groupId}/status` |
| 房间人数变化     | `/topic/room/{roomId}/count`    |

### 心跳保活

心跳保活用于检测连接是否仍然有效，防止网络中断、浏览器崩溃、移动端切后台后服务端长时间保留无效连接。STOMP 心跳由客户端和服务端协商，服务端配置心跳间隔，客户端也需要配置 incoming 和 outgoing。

服务端配置示例：

```java
registry.enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[]{10000, 10000})
        .setTaskScheduler(webSocketHeartbeatTaskScheduler());
```

前端配置示例：

```typescript
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws/stomp',
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`
  },
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000
})
```

心跳间隔建议如下：

| 场景         | 建议值                   |
| ------------ | ------------------------ |
| 本地开发     | 10 到 30 秒              |
| 管理后台     | 10 到 30 秒              |
| 聊天系统     | 10 到 20 秒              |
| 移动端弱网   | 30 到 60 秒              |
| 大连接数系统 | 需要压测后确定，不宜过短 |

心跳用于连接活性检测，但不要把心跳当作唯一在线依据。在线状态还应结合 Session 注册表、最后活跃时间、断开事件和超时扫描。

### 心跳超时检测

心跳超时检测用于清理长时间没有活跃的 Session。即使 Spring WebSocket 能处理部分断开事件，业务层仍建议维护 `lastActiveTime`，通过定时任务兜底清理异常连接。

超时扫描任务如下。

文件位置：`src/main/java/io/github/atengk/websocket/task/WsSessionTimeoutScanTask.java`

```java
package io.github.atengk.websocket.task;

import cn.hutool.core.date.LocalDateTimeUtil;
import io.github.atengk.websocket.model.WsSessionInfo;
import io.github.atengk.websocket.service.WsOnlineStatusService;
import io.github.atengk.websocket.service.WsSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebSocket Session 超时扫描任务
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsSessionTimeoutScanTask {

    private final WsSessionRegistry wsSessionRegistry;

    private final WsOnlineStatusService wsOnlineStatusService;

    /**
     * Session 超时时间，单位秒
     */
    private static final long SESSION_TIMEOUT_SECONDS = 120;

    /**
     * 定时扫描超时 Session
     */
    @Scheduled(fixedDelay = 30000)
    public void scanTimeoutSessions() {
        List<WsSessionInfo> sessions = wsSessionRegistry.listAll();
        LocalDateTime now = LocalDateTime.now();

        for (WsSessionInfo session : sessions) {
            long activeSeconds = LocalDateTimeUtil.between(session.getLastActiveTime(), now).getSeconds();
            if (activeSeconds > SESSION_TIMEOUT_SECONDS) {
                log.warn("检测到 WebSocket Session 超时，userId：{}，sessionId：{}，最后活跃间隔：{}秒",
                        session.getUserId(), session.getSessionId(), activeSeconds);
                wsOnlineStatusService.offline(session.getSessionId(), "HEARTBEAT_TIMEOUT");
            }
        }
    }
}
```

超时检测建议如下：

| 配置项   | 建议                                         |
| -------- | -------------------------------------------- |
| 超时时间 | 至少大于心跳间隔的 3 到 5 倍                 |
| 扫描频率 | 30 到 60 秒                                  |
| 清理逻辑 | 必须幂等                                     |
| 集群部署 | 每个实例只清理本实例 Session，Redis 需要 TTL |
| 移动端   | 超时时间适当放宽                             |

### 状态变更广播

状态变更广播用于将用户上线、下线、忙碌、离开、被踢出等事件通知给前端。广播范围应根据业务场景控制，避免全站广播造成压力或泄露用户状态。

推荐广播路径如下：

| 场景           | 路径                            | 说明           |
| -------------- | ------------------------------- | -------------- |
| 全局在线状态   | `/topic/online/status`          | 小系统可使用   |
| 群组在线状态   | `/topic/group/{groupId}/status` | 只通知群成员   |
| 管理端在线状态 | `/topic/admin/online`           | 管理端查看     |
| 用户会话状态   | `/user/queue/session`           | 只通知用户本人 |
| 房间人数变化   | `/topic/room/{roomId}/count`    | 房间内广播     |

状态变更广播建议如下：

| 建议             | 说明                                    |
| ---------------- | --------------------------------------- |
| 控制广播范围     | 优先按群组、房间、租户广播              |
| 避免过高频率     | 用户频繁切换状态时需要防抖              |
| 多端合并         | 单个 Session 断开不一定广播用户离线     |
| 管理端可单独推送 | 管理端看板使用专用 Topic                |
| 数据脱敏         | 不广播 IP、设备详情等敏感字段给普通用户 |

### 在线状态持久化

在线状态持久化用于记录用户最近上线时间、最近离线时间、客户端类型、设备信息和最后活跃时间。持久化后的数据可用于用户列表展示、最近在线时间、审计分析和异常排查。

表结构建议如下：

```sql
CREATE TABLE ws_user_online_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    status VARCHAR(32) NOT NULL COMMENT '在线状态',
    last_session_id VARCHAR(128) DEFAULT NULL COMMENT '最近一次Session ID',
    client_type VARCHAR(32) DEFAULT NULL COMMENT '客户端类型',
    device_id VARCHAR(128) DEFAULT NULL COMMENT '设备ID',
    ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    online_at DATETIME DEFAULT NULL COMMENT '最近上线时间',
    offline_at DATETIME DEFAULT NULL COMMENT '最近离线时间',
    last_active_at DATETIME DEFAULT NULL COMMENT '最后活跃时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_status (status),
    KEY idx_last_active_at (last_active_at)
) COMMENT='WebSocket用户在线状态表';
```

持久化策略建议如下：

| 场景       | 策略                                                   |
| ---------- | ------------------------------------------------------ |
| 上线       | 更新 `status=ONLINE`、`online_at`、`last_active_at`    |
| 下线       | 所有 Session 断开后更新 `status=OFFLINE`、`offline_at` |
| 活跃刷新   | 定期或关键操作时更新 `last_active_at`                  |
| 多端在线   | 可以只保存用户聚合状态，Session 详情放 Redis           |
| 审计要求高 | 额外记录连接日志表，不只覆盖在线状态表                 |

在线状态表不适合存放所有实时 Session 明细。Session 明细建议放内存或 Redis，数据库保存用户级状态和审计记录。

### Redis 在线状态同步

Redis 在线状态同步用于集群部署时共享在线用户、用户 Session、实例归属、群组在线成员等状态。单实例内存注册表只能看到当前实例连接，集群部署必须引入 Redis 或外部 Broker 进行状态同步。

Redis Key 建议如下：

| Key                                 | 类型 | 说明                 |
| ----------------------------------- | ---- | -------------------- |
| `ws:online:users`                   | Set  | 所有在线用户 ID      |
| `ws:user:sessions:{userId}`         | Set  | 某用户所有 Session   |
| `ws:session:{sessionId}`            | Hash | Session 明细         |
| `ws:instance:sessions:{instanceId}` | Set  | 某实例持有的 Session |
| `ws:group:online:{groupId}`         | Set  | 群组在线成员         |
| `ws:room:sessions:{roomId}`         | Set  | 房间在线 Session     |

Redis 同步建议如下：

| 操作           | Redis 处理                                     |
| -------------- | ---------------------------------------------- |
| CONNECT 成功   | 写入 Session Hash，加入用户 Set 和在线用户 Set |
| DISCONNECT     | 删除 Session Hash，从用户 Set 移除 sessionId   |
| 用户无 Session | 从在线用户 Set 移除 userId                     |
| 心跳续期       | 刷新 Session Hash TTL                          |
| 实例宕机       | 根据 instanceId 清理该实例残留 Session         |
| 群组在线       | 根据用户在线状态和群组成员关系维护             |

Redis Session TTL 应大于心跳间隔，并配合续期。例如心跳 10 秒，Session TTL 可设置 120 秒。这样即使实例异常宕机，Redis 中的 Session 也能自动过期，避免在线状态永久残留。

## 群组与房间功能

群组与房间功能用于支持多人通信场景，包括群组创建、成员加入、成员退出、成员列表、群组消息广播、权限控制、禁言、踢人、解散和房间在线人数统计。群组偏向稳定组织关系，房间偏向临时实时会话，二者可以共用部分模型，但生命周期和权限规则通常不同。

### 群组创建

群组创建用于初始化一个多人通信空间。群组可以是聊天群、项目空间、审批协作组、直播房间、设备组或业务讨论区。创建群组时应保存群组基础信息、创建人、租户、群组状态和默认成员。

群组表结构建议如下：

```sql
CREATE TABLE ws_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    group_id VARCHAR(64) NOT NULL COMMENT '群组ID',
    group_name VARCHAR(128) NOT NULL COMMENT '群组名称',
    group_type VARCHAR(32) NOT NULL COMMENT '群组类型',
    owner_id VARCHAR(64) NOT NULL COMMENT '群主用户ID',
    tenant_id VARCHAR(64) DEFAULT NULL COMMENT '租户ID',
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '群组状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_group_id (group_id),
    KEY idx_owner_id (owner_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WebSocket群组表';
```

创建群组时建议同时插入群主成员记录：

```sql
CREATE TABLE ws_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    group_id VARCHAR(64) NOT NULL COMMENT '群组ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT '成员角色',
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '成员状态',
    muted_until DATETIME DEFAULT NULL COMMENT '禁言截止时间',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_group_user (group_id, user_id),
    KEY idx_user_id (user_id),
    KEY idx_group_id (group_id)
) COMMENT='WebSocket群组成员表';
```

群组创建建议通过 HTTP API 完成，创建成功后可以通过 WebSocket 向相关用户推送群组创建通知。

```text
POST /api/ws/groups
```

### 群组成员加入

群组成员加入用于将用户加入指定群组。加入方式可以是群主邀请、用户申请、管理员添加或系统自动加入。加入成功后，应更新群组成员表，并向群组主题广播成员加入事件。

加入流程如下：

```text
1. 校验群组是否存在且未解散
2. 校验操作者是否有添加成员权限
3. 校验目标用户是否已在群组
4. 插入或恢复群组成员记录
5. 刷新群组成员缓存
6. 向 /topic/group/{groupId}/status 广播成员加入事件
```

成员加入事件示例：

```json
{
  "messageId": "1909550919275921501",
  "type": "GROUP_MEMBER_JOINED",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "groupId": "90001",
    "userId": "10002",
    "operatorId": "10001",
    "joinedAt": "2026-05-05T10:00:00"
  }
}
```

成员加入后，前端如果当前在群组页面，应刷新成员列表或将新成员追加到本地状态中。

### 群组成员退出

群组成员退出用于用户主动离开群组，或者管理员移除成员。退出后，该用户不应再能订阅群组主题，也不能继续发送群组消息。

退出流程如下：

```text
1. 校验群组是否存在
2. 校验用户是否为群组成员
3. 如果是群主，判断是否允许退出或需要转让群主
4. 更新成员状态为 EXITED 或删除成员记录
5. 刷新群组成员缓存
6. 向群组广播成员退出事件
7. 通知被移除用户取消订阅或刷新页面
```

退出策略建议如下：

| 场景             | 处理方式                     |
| ---------------- | ---------------------------- |
| 普通成员主动退出 | 更新成员状态，广播退出事件   |
| 管理员踢人       | 更新成员状态，通知被踢用户   |
| 群主退出         | 要求先转让群主或直接解散群组 |
| 用户被封禁       | 移除成员并加入黑名单         |
| 群组解散         | 批量更新所有成员状态         |

成员退出后，服务端的 `SUBSCRIBE` 鉴权必须立即生效。即使前端没有取消订阅，后续重连或重新订阅时也应被拒绝。

### 群组成员列表

群组成员列表用于前端展示群成员、角色、在线状态、禁言状态和加入时间。该列表建议通过 HTTP API 查询，WebSocket 只推送成员变更事件。

接口建议如下：

```text
GET /api/ws/groups/{groupId}/members
```

返回结构示例：

```json
{
  "groupId": "90001",
  "members": [
    {
      "userId": "10001",
      "nickname": "张三",
      "role": "OWNER",
      "status": "NORMAL",
      "online": true,
      "muted": false,
      "joinedAt": "2026-05-05T10:00:00"
    },
    {
      "userId": "10002",
      "nickname": "李四",
      "role": "MEMBER",
      "status": "NORMAL",
      "online": false,
      "muted": false,
      "joinedAt": "2026-05-05T10:05:00"
    }
  ]
}
```

成员列表查询建议如下：

| 数据         | 来源                    |
| ------------ | ----------------------- |
| 成员关系     | 群组成员表              |
| 用户昵称头像 | 用户服务或用户表        |
| 在线状态     | WebSocket 在线状态服务  |
| 禁言状态     | 群组成员表              |
| 角色权限     | 群组成员表              |
| 房间在线人数 | Redis 或 Session 注册表 |

### 群组消息广播

群组消息广播用于将一条群消息推送给订阅 `/topic/group/{groupId}` 的成员。服务端在广播前必须校验发送人是否为群组成员、是否被禁言、群组是否正常。

群组消息发送流程如下：

```text
1. 客户端 SEND /app/chat/group/send
2. 服务端获取当前 Principal userId
3. 校验 groupId、content
4. 校验用户是否为群成员
5. 校验用户是否被禁言
6. 消息落库
7. 事务提交后广播 /topic/group/{groupId}
8. 客户端收到消息并渲染
```

群组消息控制器示例。

文件位置：`src/main/java/io/github/atengk/websocket/controller/WsGroupMessageController.java`

```java
package io.github.atengk.websocket.controller;

import io.github.atengk.websocket.dto.ChatMessage;
import io.github.atengk.websocket.service.WsGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 群组消息控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WsGroupMessageController {

    private final WsGroupService wsGroupService;

    /**
     * 发送群组消息
     *
     * @param message   群组消息
     * @param principal 当前用户
     */
    @MessageMapping("/chat/group/send")
    public void sendGroupMessage(@Valid @Payload ChatMessage message, Principal principal) {
        String senderId = principal.getName();
        message.setSenderId(senderId);

        wsGroupService.sendGroupMessage(message);
        log.info("群组消息发送请求已处理，senderId：{}，groupId：{}", senderId, message.getGroupId());
    }
}
```

群组服务接口如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsGroupService.java`

```java
package io.github.atengk.websocket.service;

import io.github.atengk.websocket.dto.ChatMessage;

import java.util.List;

/**
 * WebSocket 群组服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsGroupService {

    /**
     * 发送群组消息
     *
     * @param message 群组消息
     */
    void sendGroupMessage(ChatMessage message);

    /**
     * 判断用户是否为群组成员
     *
     * @param groupId 群组 ID
     * @param userId  用户 ID
     * @return 是否为成员
     */
    boolean isMember(String groupId, String userId);

    /**
     * 判断用户是否被禁言
     *
     * @param groupId 群组 ID
     * @param userId  用户 ID
     * @return 是否被禁言
     */
    boolean isMuted(String groupId, String userId);

    /**
     * 查询群组成员 ID 列表
     *
     * @param groupId 群组 ID
     * @return 成员 ID 列表
     */
    List<String> listMemberIds(String groupId);
}
```

群组消息广播服务实现如下。实际项目中，成员关系、禁言状态和消息落库应接入数据库或缓存。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsGroupServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.dto.ChatMessage;
import io.github.atengk.websocket.enums.WsMessageType;
import io.github.atengk.websocket.model.WsMessage;
import io.github.atengk.websocket.service.WsGroupService;
import io.github.atengk.websocket.service.WsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WebSocket 群组服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsGroupServiceImpl implements WsGroupService {

    private final WsPushService wsPushService;

    /**
     * 发送群组消息
     *
     * @param message 群组消息
     */
    @Override
    public void sendGroupMessage(ChatMessage message) {
        if (message == null || StrUtil.hasBlank(message.getGroupId(), message.getSenderId(), message.getContent())) {
            throw new IllegalArgumentException("群组消息参数不完整");
        }

        String groupId = message.getGroupId();
        String senderId = message.getSenderId();

        if (!isMember(groupId, senderId)) {
            log.warn("群组消息发送被拒绝，用户不是群成员，userId：{}，groupId：{}", senderId, groupId);
            throw new AccessDeniedException("无群组发送权限");
        }

        if (isMuted(groupId, senderId)) {
            log.warn("群组消息发送被拒绝，用户已被禁言，userId：{}，groupId：{}", senderId, groupId);
            throw new AccessDeniedException("当前用户已被禁言");
        }

        // 生产环境建议先落库，再事务提交后推送
        wsPushService.sendToGroup(groupId, WsMessage.success(WsMessageType.GROUP_CHAT, message));
        log.info("群组消息已广播，userId：{}，groupId：{}", senderId, groupId);
    }

    /**
     * 判断用户是否为群组成员
     *
     * @param groupId 群组 ID
     * @param userId  用户 ID
     * @return 是否为成员
     */
    @Override
    public boolean isMember(String groupId, String userId) {
        if (StrUtil.hasBlank(groupId, userId)) {
            return false;
        }

        // 示例逻辑：实际项目应查询群组成员表或 Redis 成员缓存
        return true;
    }

    /**
     * 判断用户是否被禁言
     *
     * @param groupId 群组 ID
     * @param userId  用户 ID
     * @return 是否被禁言
     */
    @Override
    public boolean isMuted(String groupId, String userId) {
        if (StrUtil.hasBlank(groupId, userId)) {
            return true;
        }

        // 示例逻辑：实际项目应判断 muted_until 是否大于当前时间
        return false;
    }

    /**
     * 查询群组成员 ID 列表
     *
     * @param groupId 群组 ID
     * @return 成员 ID 列表
     */
    @Override
    public List<String> listMemberIds(String groupId) {
        if (StrUtil.isBlank(groupId)) {
            return List.of();
        }

        // 示例逻辑：实际项目应查询数据库或 Redis
        return List.of("10001", "10002", "10003");
    }
}
```

### 群组权限控制

群组权限控制用于限制群组创建、邀请成员、踢人、禁言、解散、发送消息、订阅主题等操作。权限通常由群组成员角色决定。

推荐角色如下：

| 角色     | 权限                         |
| -------- | ---------------------------- |
| `OWNER`  | 群主，拥有全部权限           |
| `ADMIN`  | 管理员，可邀请、踢人、禁言   |
| `MEMBER` | 普通成员，可发送和接收消息   |
| `GUEST`  | 游客，只能接收或临时发言     |
| `BANNED` | 被封禁用户，不允许发送和订阅 |

群组权限矩阵建议如下：

| 操作           | OWNER | ADMIN | MEMBER | GUEST  |
| -------------- | ----- | ----- | ------ | ------ |
| 发送消息       | 是    | 是    | 是     | 可配置 |
| 邀请成员       | 是    | 是    | 否     | 否     |
| 踢出成员       | 是    | 是    | 否     | 否     |
| 禁言成员       | 是    | 是    | 否     | 否     |
| 解散群组       | 是    | 否    | 否     | 否     |
| 查看成员列表   | 是    | 是    | 是     | 可配置 |
| 订阅群组 Topic | 是    | 是    | 是     | 可配置 |

群组订阅权限应在 `SUBSCRIBE` 阶段校验，群组发送权限应在 `SEND` 阶段和业务 Service 中二次校验。

### 群组禁言

群组禁言用于限制某个用户在指定群组中发送消息。禁言不应影响用户接收群组消息，除非业务明确要求封禁。

禁言字段建议保存在群组成员表中：

| 字段          | 说明                                     |
| ------------- | ---------------------------------------- |
| `status`      | 成员状态，如 `NORMAL`、`MUTED`、`KICKED` |
| `muted_until` | 禁言截止时间                             |
| `updated_at`  | 更新时间                                 |
| `operator_id` | 可选，禁言操作人                         |

禁言处理流程如下：

```text
1. 管理员发起禁言
2. 校验管理员权限
3. 更新成员 muted_until
4. 向被禁言用户推送通知
5. 向群组广播成员状态变化
6. 用户后续发送消息时被拒绝
```

禁言消息示例：

```json
{
  "type": "GROUP_MEMBER_MUTED",
  "data": {
    "groupId": "90001",
    "userId": "10002",
    "mutedUntil": "2026-05-05T12:00:00",
    "reason": "发送违规内容"
  }
}
```

禁言校验应在发送群消息前执行。即使前端隐藏输入框，也不能省略服务端校验。

### 群组踢人

群组踢人用于将指定成员移出群组，并阻止其继续订阅群组主题和发送群组消息。踢人后应通知被踢用户取消订阅或刷新页面。

踢人流程如下：

```text
1. 管理员发起踢人操作
2. 校验管理员权限
3. 校验不能踢出群主，或必须由群主操作
4. 更新成员状态为 KICKED 或删除成员记录
5. 清理群组成员缓存
6. 向被踢用户发送 /user/queue/notice 或 /user/queue/session 消息
7. 向群组广播成员退出事件
```

被踢通知路径建议如下：

| 路径                            | 用途                             |
| ------------------------------- | -------------------------------- |
| `/user/queue/notice`            | 通知用户已被移出群组             |
| `/user/queue/session`           | 要求当前页面退出群组或断开某订阅 |
| `/topic/group/{groupId}/status` | 群组内广播成员变更               |

踢人后，服务端必须保证：

| 要求               | 说明                                      |
| ------------------ | ----------------------------------------- |
| 不能继续订阅群组   | `SUBSCRIBE /topic/group/{groupId}` 被拒绝 |
| 不能继续发送消息   | `SEND /app/chat/group/send` 被拒绝        |
| 历史权限按业务处理 | 可配置是否允许查看历史消息                |
| 缓存及时失效       | Redis 成员缓存需要删除或刷新              |

### 群组解散

群组解散用于终止群组生命周期。解散后，群组成员不能继续发送消息、订阅群组主题或加入该群组。历史消息是否保留由业务决定。

解散流程如下：

```text
1. 群主或系统管理员发起解散
2. 校验解散权限
3. 更新群组状态为 DISBANDED
4. 批量更新成员状态
5. 清理群组缓存、在线成员缓存
6. 向 /topic/group/{groupId}/status 广播解散事件
7. 通知所有在线成员退出群组页面
```

群组状态建议如下：

| 状态        | 说明   |
| ----------- | ------ |
| `NORMAL`    | 正常   |
| `DISABLED`  | 禁用   |
| `DISBANDED` | 已解散 |
| `ARCHIVED`  | 已归档 |

群组解散后，服务端应拒绝以下操作：

| 操作         | 处理                 |
| ------------ | -------------------- |
| 订阅群组主题 | 拒绝                 |
| 发送群组消息 | 拒绝                 |
| 邀请成员     | 拒绝                 |
| 成员加入     | 拒绝                 |
| 禁言或踢人   | 通常拒绝             |
| 查询历史消息 | 按业务配置允许或拒绝 |

### 房间在线人数统计

房间在线人数统计用于直播间、协作房间、临时会议、游戏房间、实时大屏等场景。房间与群组不同，房间更强调实时在线 Session 数量，成员关系可能是临时的。

房间统计方式如下：

| 方式            | 说明                         |
| --------------- | ---------------------------- |
| 按用户统计      | 同一用户多个连接只算 1 人    |
| 按 Session 统计 | 每个连接都算 1 个在线        |
| 按设备统计      | 同一设备只算 1 个在线        |
| 按游客统计      | 未登录用户使用临时 visitorId |

Redis 结构建议如下：

```text
ws:room:sessions:{roomId}    Set，房间在线 Session
ws:room:users:{roomId}       Set，房间在线用户
ws:session:room:{sessionId}  String，Session 当前所在房间
```

房间进入流程如下：

```text
1. 用户进入房间页面
2. 客户端 SEND /app/room/join
3. 服务端校验房间权限
4. 将 sessionId 加入房间在线集合
5. 客户端订阅 /topic/room/{roomId}
6. 服务端广播房间在线人数变化
```

房间退出流程如下：

```text
1. 用户离开页面或连接断开
2. 客户端 SEND /app/room/leave 或触发 DISCONNECT
3. 服务端从房间在线集合移除 sessionId
4. 重新计算在线人数
5. 广播 /topic/room/{roomId}/count
```

房间人数消息示例：

```json
{
  "messageId": "1909550919275921502",
  "type": "ROOM_ONLINE_COUNT",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "roomId": "room-10001",
    "onlineUserCount": 128,
    "onlineSessionCount": 236
  }
}
```

房间人数统计建议如下：

| 场景           | 建议                           |
| -------------- | ------------------------------ |
| 聊天房间       | 按用户统计                     |
| 直播间         | 可按用户和 Session 同时统计    |
| 游客房间       | 使用 visitorId 或 Session 统计 |
| 高并发直播     | 使用 Redis Set 或 HyperLogLog  |
| 精确在线列表   | 使用 Redis Set                 |
| 只展示大概人数 | 可使用缓存聚合，降低计算频率   |

房间在线人数不建议每次连接变化都立即高频广播。高并发场景可以做节流，例如每 2 到 5 秒聚合推送一次，避免连接频繁进出导致前端和服务端压力过高。


## 聊天业务功能

聊天业务功能用于支持用户之间的实时通信，覆盖单聊、群聊、系统消息、撤回、已读、未读数、回执、历史消息、离线补偿、敏感词过滤、防刷和附件消息。聊天模块不能只依赖 WebSocket 实时转发，生产环境必须结合消息持久化、消息 ID、会话 ID、序号、状态和补偿查询，保证消息可追踪、可恢复、可审计。

### 单聊消息

单聊消息用于一个用户向另一个用户发送私有消息。客户端发送消息到 `/app/chat/private/send`，服务端校验发送人与接收人关系后，先落库，再向接收人和发送人的其他在线端推送消息。

单聊消息推荐流程如下：

```text
1. 发送方客户端 SEND /app/chat/private/send
2. 服务端从 Principal 获取 senderId
3. 校验 receiverId、content、黑名单、好友关系或会话权限
4. 生成 messageId、chatId、sequence
5. 消息写入数据库
6. 事务提交后推送给接收方 /user/queue/chat
7. 同步推送给发送方其他在线端
8. 客户端收到消息后按 messageId 去重，按 sequence 排序
```

单聊发送请求示例：

```json
{
  "receiverId": "10002",
  "content": "你好，这是一条单聊消息",
  "contentType": "TEXT",
  "clientMessageId": "client-msg-001"
}
```

单聊推送消息示例：

```json
{
  "messageId": "1909550919275921601",
  "type": "PRIVATE_CHAT",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "chatId": "private-10001-10002",
    "senderId": "10001",
    "receiverId": "10002",
    "content": "你好，这是一条单聊消息",
    "contentType": "TEXT",
    "sequence": 10001,
    "sentAt": 1777956000000
  }
}
```

单聊消息表建议如下：

```sql
CREATE TABLE ws_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    chat_id VARCHAR(128) NOT NULL COMMENT '会话ID',
    chat_type VARCHAR(32) NOT NULL COMMENT '会话类型：PRIVATE、GROUP',
    sender_id VARCHAR(64) NOT NULL COMMENT '发送人ID',
    receiver_id VARCHAR(64) DEFAULT NULL COMMENT '单聊接收人ID',
    group_id VARCHAR(64) DEFAULT NULL COMMENT '群组ID',
    content_type VARCHAR(32) NOT NULL COMMENT '内容类型：TEXT、IMAGE、FILE、SYSTEM',
    content TEXT NOT NULL COMMENT '消息内容',
    sequence BIGINT NOT NULL COMMENT '会话内消息序号',
    status VARCHAR(32) NOT NULL DEFAULT 'SENT' COMMENT '消息状态',
    client_message_id VARCHAR(128) DEFAULT NULL COMMENT '客户端消息ID，用于幂等',
    sent_at DATETIME NOT NULL COMMENT '发送时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    UNIQUE KEY uk_client_message (sender_id, client_message_id),
    KEY idx_chat_sequence (chat_id, sequence),
    KEY idx_receiver_status (receiver_id, status),
    KEY idx_sent_at (sent_at)
) COMMENT='WebSocket聊天消息表';
```

单聊消息注意事项如下：

| 事项             | 说明                                      |
| ---------------- | ----------------------------------------- |
| 发送人不信任前端 | `senderId` 必须从 `Principal` 获取        |
| 接收人必须校验   | 判断是否存在、是否允许会话、是否被拉黑    |
| 消息必须有 ID    | 使用服务端生成的 `messageId` 做去重和追踪 |
| 会话内要有序号   | 使用 `sequence` 支持排序和断线补偿        |
| 重要消息先落库   | 落库成功后再推送                          |
| 多端同步         | 发送方其他设备也应收到同步消息            |

### 群聊消息

群聊消息用于群组、房间、项目空间等多人通信场景。客户端发送到 `/app/chat/group/send`，服务端校验发送人是否为群成员、是否被禁言、群组是否正常，然后广播到 `/topic/group/{groupId}`。

群聊流程如下：

```text
1. 客户端 SEND /app/chat/group/send
2. 服务端获取 senderId
3. 校验 groupId、content
4. 校验发送人是否为群成员
5. 校验是否被禁言
6. 生成 messageId 和 group sequence
7. 消息落库
8. 事务提交后广播 /topic/group/{groupId}
9. 群成员客户端接收、去重、排序
```

群聊发送请求示例：

```json
{
  "groupId": "90001",
  "content": "大家好，这是一条群聊消息",
  "contentType": "TEXT",
  "clientMessageId": "client-msg-002"
}
```

群聊推送消息示例：

```json
{
  "messageId": "1909550919275921602",
  "type": "GROUP_CHAT",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "chatId": "group-90001",
    "senderId": "10001",
    "groupId": "90001",
    "content": "大家好，这是一条群聊消息",
    "contentType": "TEXT",
    "sequence": 30001,
    "sentAt": 1777956000000
  }
}
```

群聊消息建议如下：

| 事项         | 说明                                   |
| ------------ | -------------------------------------- |
| 群成员校验   | 非成员不能发送和订阅                   |
| 禁言校验     | 被禁言用户不能发送                     |
| 群组状态校验 | 已解散、禁用群不能发送                 |
| 群内序号     | 群聊消息应有群内递增 `sequence`        |
| 历史查询     | 按 `groupId + sequence` 分页           |
| 离线补偿     | 用户重连后按最后 sequence 拉取缺失消息 |

### 系统消息

系统消息用于在聊天会话中展示非用户直接发送的事件，例如用户加入群组、用户退出群组、群公告、管理员禁言、消息撤回提示、会话创建提示等。系统消息通常由服务端生成，不允许普通客户端伪造。

系统消息类型建议如下：

| 类型               | 说明       |
| ------------------ | ---------- |
| `GROUP_CREATED`    | 群组创建   |
| `MEMBER_JOINED`    | 成员加入   |
| `MEMBER_EXITED`    | 成员退出   |
| `MEMBER_KICKED`    | 成员被踢出 |
| `MEMBER_MUTED`     | 成员被禁言 |
| `MESSAGE_RECALLED` | 消息被撤回 |
| `GROUP_DISBANDED`  | 群组解散   |
| `NOTICE_PINNED`    | 群公告置顶 |

系统消息示例：

```json
{
  "messageId": "1909550919275921603",
  "type": "GROUP_CHAT",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "chatId": "group-90001",
    "senderId": "SYSTEM",
    "groupId": "90001",
    "content": "用户 10002 加入群组",
    "contentType": "SYSTEM",
    "sequence": 30002,
    "sentAt": 1777956000000
  }
}
```

系统消息处理建议如下：

| 事项           | 说明                           |
| -------------- | ------------------------------ |
| 只能服务端生成 | 客户端不能直接发送系统消息     |
| 可以落库       | 需要展示历史事件时应落库       |
| 文案可模板化   | 前端也可根据事件类型渲染文案   |
| 支持多语言     | 国际化项目建议传事件类型和参数 |
| 不参与普通撤回 | 系统消息通常不能被用户撤回     |

### 消息撤回

消息撤回用于发送人在限定时间内撤回已发送消息，或管理员撤回违规消息。撤回不是物理删除消息，而是更新消息状态，并向相关客户端推送撤回事件。

撤回流程如下：

```text
1. 客户端 SEND /app/chat/message/recall
2. 服务端校验消息是否存在
3. 校验操作者是否为发送人或管理员
4. 校验是否超过撤回时间窗口
5. 更新消息状态为 RECALLED
6. 事务提交后推送撤回事件
7. 客户端将原消息替换为撤回提示
```

撤回请求示例：

```json
{
  "messageId": "1909550919275921601",
  "chatId": "private-10001-10002",
  "reason": "用户主动撤回"
}
```

撤回事件示例：

```json
{
  "messageId": "1909550919275921604",
  "type": "MESSAGE_RECALL",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "recalledMessageId": "1909550919275921601",
    "chatId": "private-10001-10002",
    "operatorId": "10001",
    "reason": "用户主动撤回",
    "recalledAt": 1777956000000
  }
}
```

撤回规则建议如下：

| 规则       | 说明                                  |
| ---------- | ------------------------------------- |
| 时间窗口   | 普通用户只能撤回 2 到 5 分钟内消息    |
| 管理员撤回 | 管理员可撤回更长时间范围内的违规消息  |
| 状态更新   | 消息状态改为 `RECALLED`，不建议硬删除 |
| 附件处理   | 附件可标记不可访问或保留审计          |
| 前端展示   | 替换为“该消息已撤回”                  |
| 审计记录   | 管理员撤回必须记录操作日志            |

### 消息已读

消息已读用于标记用户已经查看某条消息或某个会话的消息。单聊已读通常推送给发送方，群聊已读可以按需统计已读人数，不建议在大群中对每条消息实时推送所有已读明细。

已读上报路径建议如下：

```text
/app/chat/read
```

已读请求示例：

```json
{
  "chatId": "private-10001-10002",
  "messageId": "1909550919275921601",
  "readAt": 1777956000000
}
```

已读表建议如下：

```sql
CREATE TABLE ws_chat_message_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    chat_id VARCHAR(128) NOT NULL COMMENT '会话ID',
    user_id VARCHAR(64) NOT NULL COMMENT '已读用户ID',
    read_at DATETIME NOT NULL COMMENT '已读时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_message_user (message_id, user_id),
    KEY idx_chat_user (chat_id, user_id),
    KEY idx_read_at (read_at)
) COMMENT='WebSocket聊天消息已读表';
```

已读处理建议如下：

| 场景     | 建议                           |
| -------- | ------------------------------ |
| 单聊     | 支持精确已读，通知发送方       |
| 小群     | 可支持已读人数和已读成员       |
| 大群     | 只统计已读数量或不启用逐条已读 |
| 会话已读 | 可按会话上报最后已读 sequence  |
| 性能优化 | 批量上报，不要每条消息立即请求 |

### 消息未读数

消息未读数用于展示会话列表角标、通知中心角标、总未读数等。未读数通常不适合每次都实时查询数据库，应结合缓存和事件更新。

未读数维度建议如下：

| 维度       | 说明                               |
| ---------- | ---------------------------------- |
| 用户总未读 | 当前用户所有会话未读总数           |
| 会话未读   | 当前用户在某个 chatId 下的未读数量 |
| 群组未读   | 当前用户在某个群组下的未读数量     |
| 通知未读   | 当前用户通知中心未读数量           |

Redis Key 建议如下：

```text
ws:chat:unread:total:{userId}              String，用户总未读数
ws:chat:unread:chat:{userId}:{chatId}      String，用户某会话未读数
ws:notice:unread:total:{userId}            String，用户通知未读数
```

未读数更新流程如下：

```text
1. 新消息落库成功
2. 判断接收用户是否当前正在查看该会话
3. 如果未查看，则未读数 +1
4. 推送未读数变化到 /user/queue/notice 或 /user/queue/chat
5. 用户进入会话或上报已读后，清零对应会话未读数
```

未读数推送示例：

```json
{
  "messageId": "1909550919275921605",
  "type": "UNREAD_COUNT",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "totalUnread": 12,
    "chatUnread": {
      "private-10001-10002": 3,
      "group-90001": 9
    }
  }
}
```

### 消息回执

消息回执用于确认消息已发送、已送达、已读、处理失败等状态。回执可以提升消息可靠性，但也会增加系统写入量和推送量，应按业务重要程度启用。

回执类型建议如下：

| 回执类型    | 说明               |
| ----------- | ------------------ |
| `SENT`      | 服务端已接收并保存 |
| `DELIVERED` | 客户端已收到       |
| `READ`      | 用户已读           |
| `FAILED`    | 消息处理失败       |
| `RECALLED`  | 消息已撤回         |

回执上报路径：

```text
/app/chat/receipt
```

回执请求示例：

```json
{
  "messageId": "1909550919275921601",
  "chatId": "private-10001-10002",
  "receiptType": "DELIVERED",
  "receiptAt": 1777956000000
}
```

消息回执表建议如下：

```sql
CREATE TABLE ws_chat_message_receipt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    chat_id VARCHAR(128) NOT NULL COMMENT '会话ID',
    user_id VARCHAR(64) NOT NULL COMMENT '回执用户ID',
    receipt_type VARCHAR(32) NOT NULL COMMENT '回执类型',
    receipt_at DATETIME NOT NULL COMMENT '回执时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_message_user_type (message_id, user_id, receipt_type),
    KEY idx_chat_user (chat_id, user_id)
) COMMENT='WebSocket聊天消息回执表';
```

回执处理建议如下：

| 场景     | 建议                     |
| -------- | ------------------------ |
| 普通通知 | 可只做已读，不做送达     |
| 单聊消息 | 可做送达和已读           |
| 群聊消息 | 大群不建议逐人实时回执   |
| 关键指令 | 必须做处理成功或失败回执 |
| 任务消息 | 用任务状态替代逐条回执   |

### 历史消息查询

历史消息查询用于客户端初始化会话、分页加载聊天记录、断线补偿、消息搜索和审计。历史查询建议使用 HTTP API，不建议通过 WebSocket 传输大量历史数据。

接口设计建议如下：

```text
GET /api/chat/messages?chatId=private-10001-10002&beforeSequence=10001&pageSize=20
GET /api/chat/messages?chatId=group-90001&afterSequence=30001&pageSize=100
```

查询参数建议如下：

| 参数             | 说明                               |
| ---------------- | ---------------------------------- |
| `chatId`         | 会话 ID                            |
| `beforeSequence` | 查询该序号之前的消息，用于向上翻页 |
| `afterSequence`  | 查询该序号之后的消息，用于补偿     |
| `pageSize`       | 每页数量，建议限制最大值           |
| `messageType`    | 可选，按内容类型过滤               |
| `keyword`        | 可选，搜索消息内容                 |

返回示例：

```json
{
  "chatId": "group-90001",
  "messages": [
    {
      "messageId": "1909550919275921602",
      "senderId": "10001",
      "content": "大家好，这是一条群聊消息",
      "contentType": "TEXT",
      "sequence": 30001,
      "status": "SENT",
      "sentAt": 1777956000000
    }
  ],
  "hasMore": true
}
```

历史消息查询建议如下：

| 事项          | 说明                             |
| ------------- | -------------------------------- |
| 使用游标分页  | 优先使用 `sequence`，避免深分页  |
| 校验会话权限  | 用户只能查自己有权限的会话       |
| 过滤撤回消息  | 撤回消息返回撤回状态，不返回原文 |
| 附件权限校验  | 附件 URL 需要鉴权或短期签名      |
| 限制 pageSize | 防止一次拉取过多数据             |
| 敏感信息处理  | 按合规要求脱敏或审计             |

### 离线消息补偿

离线消息补偿用于用户断线、网络重连、服务重启、客户端休眠期间恢复漏收消息。补偿通常基于用户最后同步时间、会话最后 sequence 或消息未读状态。

补偿策略如下：

| 类型     | 补偿依据                  | 说明                 |
| -------- | ------------------------- | -------------------- |
| 单聊     | `chatId + lastSequence`   | 查询指定会话缺失消息 |
| 群聊     | `groupId + lastSequence`  | 查询群内缺失消息     |
| 通知     | `lastSyncTime` 或未读状态 | 查询新增通知         |
| 任务进度 | `taskId`                  | 查询任务当前状态     |
| 告警     | 未处理状态                | 查询未处理告警       |

离线补偿接口建议如下：

```text
GET /api/chat/messages/compensate?chatId=group-90001&lastSequence=30001
GET /api/ws/messages/compensate?lastSyncTime=1777956000000
```

补偿流程如下：

```text
1. WebSocket 重连成功
2. 前端恢复订阅
3. 前端读取本地 lastSequence 或 lastSyncTime
4. 调用 HTTP 补偿接口
5. 服务端查询缺失消息
6. 前端按 messageId 去重
7. 按 sequence 或 timestamp 排序合并
```

离线补偿注意事项如下：

| 事项                  | 说明                              |
| --------------------- | --------------------------------- |
| 不依赖 WebSocket 回放 | WebSocket 只负责实时，历史靠 HTTP |
| 必须去重              | 实时消息和补偿消息可能重叠        |
| 必须排序              | 补偿消息可能和实时消息乱序        |
| 控制补偿数量          | 超过上限时提示用户刷新或分页加载  |
| 关键消息落库          | 没有持久化就无法可靠补偿          |

### 敏感词过滤

敏感词过滤用于对聊天文本、评论、群公告等内容进行风险识别和替换。过滤应在消息落库和推送前完成，避免违规内容被持久化或推送给其他用户。

处理流程如下：

```text
1. 客户端发送聊天消息
2. 服务端校验消息长度和内容类型
3. 敏感词检测
4. 按策略拒绝、替换或进入审核
5. 处理后的消息落库
6. 推送给接收方或群组
```

敏感词处理策略如下：

| 策略     | 说明                             |
| -------- | -------------------------------- |
| 拒绝发送 | 命中严重敏感词时直接拒绝         |
| 内容替换 | 将敏感词替换为 `***`             |
| 人工审核 | 可疑内容进入审核后再展示         |
| 用户风控 | 高频命中敏感词时限制发送         |
| 日志审计 | 记录命中类型，不记录过多敏感原文 |

敏感词过滤示例接口如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/ChatContentFilterService.java`

```java
package io.github.atengk.websocket.service;

/**
 * 聊天内容过滤服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface ChatContentFilterService {

    /**
     * 过滤消息内容
     *
     * @param content 原始内容
     * @return 过滤后的内容
     */
    String filter(String content);

    /**
     * 判断是否禁止发送
     *
     * @param content 原始内容
     * @return 是否禁止
     */
    boolean isBlocked(String content);
}
```

基础实现如下，生产环境建议接入敏感词库、风控平台或内容安全服务。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/ChatContentFilterServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.ChatContentFilterService;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 聊天内容过滤服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Service
public class ChatContentFilterServiceImpl implements ChatContentFilterService {

    private static final Set<String> BLOCK_WORDS = CollUtil.newHashSet("违规词1", "违规词2");

    /**
     * 过滤消息内容
     *
     * @param content 原始内容
     * @return 过滤后的内容
     */
    @Override
    public String filter(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }

        String result = content;
        for (String word : BLOCK_WORDS) {
            result = StrUtil.replace(result, word, "***");
        }
        return result;
    }

    /**
     * 判断是否禁止发送
     *
     * @param content 原始内容
     * @return 是否禁止
     */
    @Override
    public boolean isBlocked(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }

        return BLOCK_WORDS.stream().anyMatch(content::contains);
    }
}
```

### 消息防刷

消息防刷用于防止用户高频发送、重复发送、脚本刷屏、恶意攻击和资源耗尽。防刷应结合用户维度、IP 维度、群组维度、目标地址维度和消息内容维度。

防刷策略建议如下：

| 策略         | 示例                     |
| ------------ | ------------------------ |
| 用户发送频率 | 单用户每秒最多 5 条      |
| 群组发送频率 | 单群每秒最多 100 条      |
| 重复内容限制 | 10 秒内相同内容最多 3 次 |
| 消息长度限制 | 文本消息最大 2000 字符   |
| IP 频率限制  | 单 IP 每分钟最多 300 条  |
| 禁言联动     | 高频违规自动禁言         |

Redis Key 示例：

```text
ws:rate:chat:user:{userId}
ws:rate:chat:group:{groupId}
ws:rate:chat:ip:{ip}
ws:rate:chat:dup:{userId}:{contentHash}
```

防刷处理建议如下：

| 结果     | 说明                     |
| -------- | ------------------------ |
| 正常通过 | 继续落库和推送           |
| 轻微超限 | 返回错误消息，不断开连接 |
| 严重超限 | 临时禁言或限制发送       |
| 恶意攻击 | 加入黑名单并强制下线     |
| 管理审计 | 记录异常发送行为         |

### 附件消息

附件消息用于图片、文件、语音、视频等非纯文本内容。附件本体不建议通过 WebSocket 传输，应该通过 HTTP 或对象存储上传，WebSocket 只推送附件元数据。

附件消息流程如下：

```text
1. 客户端通过 HTTP 上传附件
2. 服务端返回 fileId、fileName、url、size、mimeType
3. 客户端 SEND /app/chat/private/send 或 /app/chat/group/send
4. 消息体中 contentType=FILE 或 IMAGE
5. 服务端校验附件归属和权限
6. 消息落库并推送附件元数据
7. 接收方通过 HTTP 下载或预览附件
```

附件消息体示例：

```json
{
  "receiverId": "10002",
  "contentType": "FILE",
  "content": "合同文件.pdf",
  "attachment": {
    "fileId": "file-100001",
    "fileName": "合同文件.pdf",
    "fileSize": 1048576,
    "mimeType": "application/pdf",
    "url": "/api/files/file-100001/download"
  }
}
```

附件消息建议如下：

| 事项                    | 说明                           |
| ----------------------- | ------------------------------ |
| 不用 WebSocket 传大文件 | WebSocket 只传元数据           |
| 文件先上传              | 上传成功后再发送聊天消息       |
| 下载要鉴权              | 防止越权下载附件               |
| URL 建议短期签名        | 对象存储场景建议使用临时 URL   |
| 限制文件大小            | 防止超大附件影响系统           |
| 病毒扫描                | 企业系统建议接入安全扫描       |
| 撤回处理                | 撤回后可禁止附件访问或保留审计 |

## 通知推送功能

通知推送功能用于向用户、群组、租户、角色或管理端推送业务提醒、系统公告、待办、审批、告警、订单状态、任务进度等消息。通知推送与聊天消息不同，通知通常强调业务触达、已读未读、跳转处理和历史列表。

### 业务通知

业务通知用于将业务系统中的状态变化推送给相关用户，例如订单支付成功、审批状态变化、任务完成、库存预警、客户分配等。业务通知通常由业务服务产生事件，通知服务或 WebSocket 服务消费事件后推送。

业务通知流程如下：

```text
1. 业务服务完成数据库事务
2. 发布业务事件
3. 通知服务消费事件
4. 生成通知记录
5. 解析接收人
6. 在线用户实时推送 /user/queue/notice
7. 离线用户保留通知记录
8. 用户登录后查询通知列表
```

业务通知消息体建议如下：

```json
{
  "noticeId": "notice-100001",
  "title": "订单状态变更",
  "content": "订单 order-100001 已完成支付",
  "level": "NORMAL",
  "bizId": "order-100001",
  "bizType": "ORDER",
  "url": "/order/detail/order-100001",
  "needAck": true,
  "expireAt": 1778042400000
}
```

业务通知建议如下：

| 事项             | 说明                             |
| ---------------- | -------------------------------- |
| 事务后生成       | 业务数据提交后再生成通知         |
| 通知要落库       | 支持历史列表和离线查看           |
| 接收人服务端解析 | 不由前端指定接收人               |
| 支持模板         | 标题、内容、跳转地址可模板化     |
| 支持幂等         | 同一业务事件不能重复生成多条通知 |
| 支持过期         | 临时通知可设置有效期             |

### 系统通知

系统通知用于发布平台公告、维护通知、版本升级提醒、安全提醒、全站消息等。系统通知通常面向全体用户、指定租户、指定角色或指定组织。

系统通知路径建议如下：

| 类型         | 推送路径                          | 说明             |
| ------------ | --------------------------------- | ---------------- |
| 全站公告     | `/topic/system/notice`            | 所有在线用户订阅 |
| 租户公告     | `/topic/tenant/{tenantId}/notice` | 租户用户订阅     |
| 用户系统通知 | `/user/queue/notice`              | 指定用户接收     |
| 管理端通知   | `/topic/admin/notice`             | 管理端订阅       |

系统通知字段建议如下：

| 字段         | 说明                                 |
| ------------ | ------------------------------------ |
| `noticeId`   | 通知 ID                              |
| `title`      | 标题                                 |
| `content`    | 内容                                 |
| `level`      | 级别                                 |
| `scopeType`  | 范围类型，如 ALL、TENANT、ROLE、USER |
| `scopeIds`   | 范围 ID                              |
| `publishAt`  | 发布时间                             |
| `expireAt`   | 过期时间                             |
| `forcePopup` | 是否强制弹窗                         |

系统通知注意事项如下：

| 事项                 | 说明                     |
| -------------------- | ------------------------ |
| 重要通知必须落库     | 离线用户也要能看到       |
| 广播只做实时触达     | 列表展示仍通过 HTTP 查询 |
| 敏感通知不要全站广播 | 应使用用户或租户定向     |
| 支持定时发布         | 到达发布时间后再推送     |
| 支持已读状态         | 重要通知需要记录用户已读 |

### 待办通知

待办通知用于提醒用户处理某项任务，例如审批待办、工单待处理、任务待确认、流程待签收等。待办通知通常和业务状态强关联，需要支持跳转、已读、处理完成和撤销。

待办通知示例：

```json
{
  "noticeId": "todo-100001",
  "title": "你有一条新的待办",
  "content": "采购申请 PR-10001 等待你审批",
  "level": "IMPORTANT",
  "bizId": "PR-10001",
  "bizType": "PURCHASE_APPROVAL",
  "url": "/approval/todo/PR-10001",
  "needAck": true
}
```

待办通知状态建议如下：

| 状态       | 说明   |
| ---------- | ------ |
| `UNREAD`   | 未读   |
| `READ`     | 已读   |
| `PENDING`  | 待处理 |
| `DONE`     | 已处理 |
| `CANCELED` | 已取消 |
| `EXPIRED`  | 已过期 |

待办通知处理建议如下：

| 场景     | 处理                           |
| -------- | ------------------------------ |
| 待办生成 | 推送给处理人                   |
| 待办转交 | 通知新处理人，撤销旧处理人待办 |
| 待办完成 | 推送状态变更，更新未读和待办数 |
| 待办撤销 | 推送撤销事件                   |
| 用户离线 | 保留待办记录，登录后查询       |

### 审批通知

审批通知是待办通知的一种典型业务场景，通常包括审批发起、审批待办、审批通过、审批驳回、审批转交、审批撤销等事件。

审批通知类型建议如下：

| 类型                    | 说明           |
| ----------------------- | -------------- |
| `APPROVAL_CREATED`      | 审批已发起     |
| `APPROVAL_TODO_CREATED` | 审批待办已生成 |
| `APPROVAL_APPROVED`     | 审批已通过     |
| `APPROVAL_REJECTED`     | 审批已驳回     |
| `APPROVAL_TRANSFERRED`  | 审批已转交     |
| `APPROVAL_CANCELED`     | 审批已撤销     |

审批通知路径建议使用用户私有队列：

```text
/user/queue/notice
/user/queue/todo
/user/queue/approval
```

审批通知建议如下：

| 事项             | 说明                       |
| ---------------- | -------------------------- |
| 接收人按流程解析 | 根据当前审批节点确定处理人 |
| 事务提交后推送   | 审批状态落库后再推送       |
| 支持撤销和转交   | 旧待办需要同步状态         |
| 支持跳转         | 通知中带审批详情 URL       |
| 支持未读数       | 更新待办角标               |
| 支持审计         | 通知和审批操作都应可追踪   |

### 告警通知

告警通知用于系统异常、业务风险、设备异常、安全事件、任务失败等场景。告警通知通常需要级别、聚合、确认、关闭和审计。

告警通知级别建议如下：

| 级别       | 说明     |
| ---------- | -------- |
| `INFO`     | 普通提示 |
| `WARN`     | 警告     |
| `ERROR`    | 错误     |
| `CRITICAL` | 严重告警 |

告警通知示例：

```json
{
  "noticeId": "alarm-100001",
  "title": "任务执行失败",
  "content": "数据同步任务 sync-10001 执行失败",
  "level": "ERROR",
  "bizId": "sync-10001",
  "bizType": "TASK",
  "url": "/admin/task/sync-10001",
  "needAck": true
}
```

告警通知推送路径建议如下：

| 对象           | 路径                             |
| -------------- | -------------------------------- |
| 管理端实时告警 | `/topic/admin/alarm`             |
| 指定负责人     | `/user/queue/alarm`              |
| 系统级告警     | `/topic/system/alarm`            |
| 租户级告警     | `/topic/tenant/{tenantId}/alarm` |

告警通知注意事项如下：

| 事项         | 说明                     |
| ------------ | ------------------------ |
| 避免刷屏     | 相同告警短时间内应聚合   |
| 支持确认     | 管理员确认后记录处理人   |
| 支持关闭     | 问题恢复后关闭告警       |
| 支持升级     | 长时间未处理可升级级别   |
| 不推完整堆栈 | 前端只展示摘要和 traceId |
| 日志关联     | 告警必须能关联后端日志   |

### 订单状态通知

订单状态通知用于电商、交易、工单、生产任务等业务状态流转场景。订单状态变化通常来源于订单服务，应由订单事务提交后发布事件，再推送给用户或管理端。

订单通知类型建议如下：

| 类型              | 说明       |
| ----------------- | ---------- |
| `ORDER_CREATED`   | 订单已创建 |
| `ORDER_PAID`      | 订单已支付 |
| `ORDER_SHIPPED`   | 订单已发货 |
| `ORDER_COMPLETED` | 订单已完成 |
| `ORDER_CANCELED`  | 订单已取消 |
| `ORDER_REFUNDED`  | 订单已退款 |

订单状态通知示例：

```json
{
  "noticeId": "order-notice-100001",
  "title": "订单状态变更",
  "content": "你的订单 order-100001 已发货",
  "level": "NORMAL",
  "bizId": "order-100001",
  "bizType": "ORDER",
  "url": "/order/detail/order-100001",
  "needAck": false
}
```

订单状态通知建议如下：

| 接收对象   | 推送方式                          |
| ---------- | --------------------------------- |
| 订单用户   | `/user/queue/notice`              |
| 客服人员   | `/user/queue/notice` 或管理端主题 |
| 管理端看板 | `/topic/admin/dashboard/order`    |
| 运营大屏   | `/topic/dashboard/order`          |

订单状态通知应保证幂等。同一个订单事件重复消费时，不能重复生成多条相同通知。可以使用 `bizType + bizId + eventType + receiverId` 作为唯一约束。

### 任务进度通知

任务进度通知用于后台异步任务、文件导入、报表生成、批量处理、模型分析、数据同步等场景。任务进度通常推送给任务发起人，也可以推送给管理端看板。

任务进度消息示例：

```json
{
  "noticeId": "task-progress-100001",
  "title": "文件导入进度",
  "content": "正在处理第 6500 条数据",
  "level": "NORMAL",
  "bizId": "task-100001",
  "bizType": "IMPORT_TASK",
  "url": "/task/detail/task-100001",
  "needAck": false,
  "extra": {
    "status": "RUNNING",
    "progress": 65
  }
}
```

推送路径建议如下：

| 场景           | 路径                                        |
| -------------- | ------------------------------------------- |
| 用户任务进度   | `/user/queue/task`                          |
| 管理端任务看板 | `/topic/admin/dashboard/task`               |
| 任务失败告警   | `/user/queue/alarm` 或 `/topic/admin/alarm` |

任务进度通知建议如下：

| 事项             | 说明                           |
| ---------------- | ------------------------------ |
| 高频进度要节流   | 不要每处理一条数据都推送       |
| 最终状态必须落库 | 成功、失败、取消状态必须持久化 |
| 断线后查最新状态 | 不需要回放每个进度点           |
| 失败原因要清晰   | 返回可读错误摘要和 traceId     |
| 管理端单独推送   | 管理端可聚合全部任务状态       |

### 定向通知

定向通知用于向指定用户、角色、部门、租户、群组或设备推送消息。定向通知的核心是服务端解析接收范围，而不是让前端传入任意接收人。

定向范围建议如下：

| 范围类型 | 说明               |
| -------- | ------------------ |
| `USER`   | 指定用户           |
| `ROLE`   | 指定角色           |
| `DEPT`   | 指定部门           |
| `TENANT` | 指定租户           |
| `GROUP`  | 指定群组           |
| `DEVICE` | 指定设备或 Session |

定向通知请求模型建议如下。

文件位置：`src/main/java/io/github/atengk/websocket/dto/NoticeSendRequest.java`

```java
package io.github.atengk.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 通知发送请求
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class NoticeSendRequest {

    /**
     * 通知标题
     */
    @NotBlank(message = "通知标题不能为空")
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知级别
     */
    private String level;

    /**
     * 范围类型：USER、ROLE、DEPT、TENANT、GROUP、DEVICE
     */
    @NotBlank(message = "范围类型不能为空")
    private String scopeType;

    /**
     * 范围 ID 列表
     */
    @NotEmpty(message = "范围ID不能为空")
    private List<String> scopeIds;

    /**
     * 业务 ID
     */
    private String bizId;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 跳转地址
     */
    private String url;

    /**
     * 扩展字段
     */
    private Map<String, Object> extra;
}
```

定向通知处理流程如下：

```text
1. 接收通知发送请求或业务事件
2. 校验操作人权限
3. 根据 scopeType 和 scopeIds 解析最终用户列表
4. 生成通知记录
5. 在线用户实时推送
6. 离线用户保留记录
7. 更新未读数
```

### 批量通知

批量通知用于一次向大量用户推送消息，例如系统公告、运营活动、租户通知、角色通知等。批量通知需要考虑推送量、数据库写入量、未读数更新和限速。

批量通知策略如下：

| 场景         | 推荐方式                             |
| ------------ | ------------------------------------ |
| 全站公告     | `/topic/system/notice` 广播 + 公告表 |
| 指定角色     | 解析用户后分批生成通知               |
| 指定租户     | 租户 Topic 广播 + 通知记录           |
| 大量用户私信 | MQ 异步分批推送                      |
| 需要已读状态 | 按用户生成通知记录或延迟生成         |

批量通知流程如下：

```text
1. 创建批量通知任务
2. 解析目标用户
3. 分批写入通知记录
4. 在线用户分批推送
5. 更新未读数缓存
6. 记录任务进度和失败用户
7. 支持失败重试
```

批量通知注意事项如下：

| 事项               | 说明                     |
| ------------------ | ------------------------ |
| 不要一次查全量用户 | 使用分页或流式查询       |
| 不要一次推送过多   | 分批发送，控制批次大小   |
| 优先广播公共消息   | 全站公告不必逐个用户推送 |
| 需要任务记录       | 批量通知应有任务状态     |
| 失败可重试         | 记录失败批次和失败原因   |
| 控制前端压力       | 大量通知不要同时弹窗     |

### 通知已读未读

通知已读未读用于通知中心角标、通知列表状态、待办数量和用户行为统计。通知通常需要支持单条已读、批量已读、全部已读和未读数查询。

通知表建议如下：

```sql
CREATE TABLE ws_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    notice_id VARCHAR(64) NOT NULL COMMENT '通知ID',
    receiver_id VARCHAR(64) NOT NULL COMMENT '接收人ID',
    title VARCHAR(128) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    level VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '通知级别',
    biz_id VARCHAR(64) DEFAULT NULL COMMENT '业务ID',
    biz_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    url VARCHAR(512) DEFAULT NULL COMMENT '跳转地址',
    status VARCHAR(32) NOT NULL DEFAULT 'UNREAD' COMMENT '状态：UNREAD、READ、DONE、CANCELED',
    read_at DATETIME DEFAULT NULL COMMENT '已读时间',
    expire_at DATETIME DEFAULT NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_notice_receiver (notice_id, receiver_id),
    KEY idx_receiver_status (receiver_id, status),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_created_at (created_at)
) COMMENT='WebSocket通知表';
```

已读接口建议如下：

```text
PUT /api/notices/{noticeId}/read
PUT /api/notices/read-batch
PUT /api/notices/read-all
GET /api/notices/unread-count
```

WebSocket 已读上报路径也可以提供：

```text
/app/notice/read
/app/notice/read-all
```

通知已读请求示例：

```json
{
  "noticeId": "notice-100001",
  "readAt": 1777956000000
}
```

通知未读数推送示例：

```json
{
  "messageId": "1909550919275921606",
  "type": "NOTICE_UNREAD_COUNT",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "totalUnread": 8,
    "todoUnread": 3,
    "approvalUnread": 2,
    "alarmUnread": 1
  }
}
```

通知已读未读处理建议如下：

| 场景       | 处理                                  |
| ---------- | ------------------------------------- |
| 单条已读   | 更新通知状态和 `read_at`              |
| 批量已读   | 按通知 ID 列表批量更新                |
| 全部已读   | 按用户维度更新未读通知                |
| 未读数变化 | 推送 `/user/queue/notice`             |
| 多端同步   | 同一用户所有在线端同步未读数          |
| 已处理待办 | 状态从 `UNREAD` 或 `READ` 变为 `DONE` |
| 通知过期   | 定时任务更新为 `EXPIRED` 或查询时过滤 |


## 消息可靠性设计

消息可靠性设计用于降低实时通信中消息丢失、重复、乱序、超时、失败不可追踪等问题。WebSocket STOMP 本身更偏向实时通道，不应被直接当作完整可靠消息系统。对于聊天、通知、审批、订单状态、任务结果等关键业务消息，应结合消息唯一 ID、幂等处理、确认机制、回执机制、重试、补偿、持久化、离线存储、失败记录和死信处理共同保证业务可恢复。

### 消息唯一 ID

消息唯一 ID 是消息可靠性的基础。每一条由服务端生成并推送的业务消息都必须具备全局唯一的 `messageId`，用于前端去重、服务端幂等、日志追踪、ACK 回执、失败重试和离线补偿。

推荐 ID 生成策略如下：

| 策略          | 说明                                       | 适用场景             |
| ------------- | ------------------------------------------ | -------------------- |
| 雪花 ID       | 趋势递增，适合数据库索引                   | 聊天、通知、业务事件 |
| UUID          | 全局唯一，长度较长                         | 低频消息、跨系统事件 |
| 业务幂等 ID   | 由 `bizType + bizId + eventType` 生成      | 订单、审批、任务事件 |
| 客户端消息 ID | 前端生成 `clientMessageId`，服务端辅助幂等 | 聊天发送防重复提交   |

建议同时保留两个 ID：

| 字段              | 生成方 | 作用                           |
| ----------------- | ------ | ------------------------------ |
| `messageId`       | 服务端 | 服务端最终消息 ID，可靠性主键  |
| `clientMessageId` | 客户端 | 防止客户端断线重试导致重复提交 |

消息 ID 工具类如下。

文件位置：`src/main/java/io/github/atengk/websocket/util/WsMessageIdUtil.java`

```java
package io.github.atengk.websocket.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

/**
 * WebSocket 消息 ID 工具类
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class WsMessageIdUtil {

    private WsMessageIdUtil() {
    }

    /**
     * 生成服务端消息 ID
     *
     * @return 消息 ID
     */
    public static String nextMessageId() {
        return String.valueOf(IdUtil.getSnowflakeNextId());
    }

    /**
     * 构建业务幂等键
     *
     * @param bizType   业务类型
     * @param bizId     业务 ID
     * @param eventType 事件类型
     * @param receiver  接收方
     * @return 幂等键
     */
    public static String buildBizIdempotentKey(String bizType, String bizId, String eventType, String receiver) {
        return StrUtil.join(":", "ws", "idem", bizType, bizId, eventType, receiver);
    }
}
```

消息唯一 ID 设计建议如下：

| 建议                   | 说明                                     |
| ---------------------- | ---------------------------------------- |
| 服务端生成最终 ID      | 不要完全信任客户端生成的消息 ID          |
| 消息 ID 不复用         | 撤回、ACK、回执等事件也应有自己的消息 ID |
| 业务 ID 与消息 ID 分离 | 一个订单事件可以生成多条用户通知         |
| 前端按 messageId 去重  | 重连补偿和实时推送可能重复               |
| 日志打印 messageId     | 便于定位消息链路                         |

### 消息幂等处理

消息幂等处理用于保证同一条消息或同一个业务事件被重复处理时，不会重复落库、重复推送、重复增加未读数或重复产生通知。重复来源通常包括客户端重试、网络抖动、服务端重试、MQ 重复投递、事务重放和前端重复点击。

幂等处理位置如下：

| 层级         | 幂等对象                                   | 说明                 |
| ------------ | ------------------------------------------ | -------------------- |
| 客户端发送层 | `clientMessageId`                          | 防止重复提交聊天消息 |
| 服务端业务层 | `messageId`                                | 防止重复保存和推送   |
| 业务事件层   | `bizType + bizId + eventType + receiverId` | 防止重复生成通知     |
| ACK 回执层   | `messageId + userId + ackType`             | 防止重复确认         |
| 未读数层     | `messageId + receiverId`                   | 防止重复累加         |

数据库唯一约束示例：

```sql
CREATE TABLE ws_message_idempotent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    idempotent_key VARCHAR(256) NOT NULL COMMENT '幂等键',
    message_id VARCHAR(64) DEFAULT NULL COMMENT '消息ID',
    biz_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    biz_id VARCHAR(64) DEFAULT NULL COMMENT '业务ID',
    event_type VARCHAR(64) DEFAULT NULL COMMENT '事件类型',
    receiver_id VARCHAR(64) DEFAULT NULL COMMENT '接收人ID',
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '处理状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_idempotent_key (idempotent_key),
    KEY idx_message_id (message_id),
    KEY idx_biz (biz_type, biz_id, event_type)
) COMMENT='WebSocket消息幂等表';
```

幂等服务接口如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsIdempotentService.java`

```java
package io.github.atengk.websocket.service;

/**
 * WebSocket 消息幂等服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsIdempotentService {

    /**
     * 尝试占用幂等键
     *
     * @param idempotentKey 幂等键
     * @param messageId     消息 ID
     * @return 是否首次处理
     */
    boolean tryAcquire(String idempotentKey, String messageId);

    /**
     * 判断幂等键是否存在
     *
     * @param idempotentKey 幂等键
     * @return 是否存在
     */
    boolean exists(String idempotentKey);
}
```

基础实现示例如下。生产环境建议使用 Redis `SETNX` 或数据库唯一索引兜底。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/InMemoryWsIdempotentServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.WsIdempotentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版 WebSocket 消息幂等服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class InMemoryWsIdempotentServiceImpl implements WsIdempotentService {

    private final Set<String> idempotentKeys = ConcurrentHashMap.newKeySet();

    /**
     * 尝试占用幂等键
     *
     * @param idempotentKey 幂等键
     * @param messageId     消息 ID
     * @return 是否首次处理
     */
    @Override
    public boolean tryAcquire(String idempotentKey, String messageId) {
        if (StrUtil.isBlank(idempotentKey)) {
            return false;
        }

        boolean acquired = idempotentKeys.add(idempotentKey);
        if (!acquired) {
            log.warn("WebSocket 消息重复处理已拦截，idempotentKey：{}，messageId：{}", idempotentKey, messageId);
        }
        return acquired;
    }

    /**
     * 判断幂等键是否存在
     *
     * @param idempotentKey 幂等键
     * @return 是否存在
     */
    @Override
    public boolean exists(String idempotentKey) {
        return StrUtil.isNotBlank(idempotentKey) && idempotentKeys.contains(idempotentKey);
    }
}
```

幂等处理建议如下：

| 场景       | 幂等键                                        |
| ---------- | --------------------------------------------- |
| 单聊消息   | `senderId + clientMessageId`                  |
| 群聊消息   | `senderId + groupId + clientMessageId`        |
| 订单通知   | `ORDER + orderId + eventType + receiverId`    |
| 审批待办   | `APPROVAL + approvalId + nodeId + receiverId` |
| ACK 回执   | `messageId + userId + ackType`                |
| 未读数累加 | `messageId + receiverId + UNREAD`             |

### 消息确认机制

消息确认机制用于判断消息是否被客户端收到、处理或读取。需要区分三个层面的确认：

| 层面       | 说明                                  |
| ---------- | ------------------------------------- |
| 协议确认   | STOMP ACK、NACK、Receipt 等协议层能力 |
| 服务端确认 | 服务端接收请求、落库成功、推送完成    |
| 业务确认   | 客户端收到、用户已读、业务处理完成    |

常见确认类型如下：

| 类型               | 触发方 | 说明                   |
| ------------------ | ------ | ---------------------- |
| `SERVER_RECEIVED`  | 服务端 | 服务端已收到客户端消息 |
| `SERVER_SAVED`     | 服务端 | 消息已落库             |
| `SERVER_PUSHED`    | 服务端 | 服务端已调用推送       |
| `CLIENT_RECEIVED`  | 客户端 | 客户端已收到消息       |
| `CLIENT_READ`      | 客户端 | 用户已读               |
| `CLIENT_PROCESSED` | 客户端 | 客户端已处理完成       |

确认消息体建议如下：

```json
{
  "ackId": "ack-100001",
  "messageId": "1909550919275921701",
  "ackType": "CLIENT_RECEIVED",
  "success": true,
  "ackAt": 1777956000000,
  "reason": null
}
```

确认机制设计建议如下：

| 建议                           | 说明                                    |
| ------------------------------ | --------------------------------------- |
| 普通广播不强制 ACK             | 全站公告不适合逐人 ACK                  |
| 私有关键消息可 ACK             | 审批、任务结果、重要通知可确认          |
| 聊天可做送达和已读             | 单聊适合，超大群不建议逐人回执          |
| 服务端推送成功不等于客户端收到 | `convertAndSend` 成功只代表进入发送链路 |
| ACK 要幂等                     | 重复 ACK 不应重复更新状态               |
| ACK 可批量                     | 高频消息不要逐条实时 ACK                |

### 消息回执机制

消息回执机制用于记录用户对消息的接收、送达、已读、处理失败等状态。回执通常需要持久化，尤其是聊天、审批、通知和任务类消息。

回执表结构建议如下：

```sql
CREATE TABLE ws_message_receipt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    receipt_id VARCHAR(64) NOT NULL COMMENT '回执ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    user_id VARCHAR(64) NOT NULL COMMENT '回执用户ID',
    receipt_type VARCHAR(32) NOT NULL COMMENT '回执类型',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功',
    reason VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    receipt_at DATETIME NOT NULL COMMENT '回执时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_message_user_type (message_id, user_id, receipt_type),
    KEY idx_message_id (message_id),
    KEY idx_user_id (user_id)
) COMMENT='WebSocket消息回执表';
```

回执类型建议如下：

| 回执类型    | 说明                 |
| ----------- | -------------------- |
| `DELIVERED` | 已送达客户端         |
| `READ`      | 用户已读             |
| `PROCESSED` | 客户端或业务处理完成 |
| `FAILED`    | 客户端处理失败       |
| `TIMEOUT`   | 等待确认超时         |

ACK 请求模型如下。

文件位置：`src/main/java/io/github/atengk/websocket/dto/WsAckRequest.java`

```java
package io.github.atengk.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * WebSocket ACK 请求
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class WsAckRequest {

    /**
     * 被确认的消息 ID
     */
    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    /**
     * ACK 类型
     */
    @NotBlank(message = "ACK类型不能为空")
    private String ackType;

    /**
     * 是否成功
     */
    private Boolean success = true;

    /**
     * 失败原因
     */
    private String reason;

    /**
     * 客户端确认时间
     */
    private Long ackAt;
}
```

### 消息重试机制

消息重试机制用于在消息发送失败、客户端未确认、外部 Broker 异常、网络抖动、事务后推送失败等场景下重新尝试发送。重试必须有次数上限、间隔策略和失败落库，否则容易造成无限重试和消息风暴。

推荐重试策略如下：

| 策略       | 说明                                 |
| ---------- | ------------------------------------ |
| 固定间隔   | 每隔固定时间重试，简单直接           |
| 指数退避   | 每次重试间隔递增，适合外部依赖异常   |
| 最大次数   | 达到上限后转失败或死信               |
| 幂等保护   | 重试前必须确保不会重复落库或重复累加 |
| 可重试标识 | 权限错误、参数错误不应重试           |

失败消息表建议如下：

```sql
CREATE TABLE ws_failed_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    destination VARCHAR(256) NOT NULL COMMENT '目标地址',
    receiver_id VARCHAR(64) DEFAULT NULL COMMENT '接收人ID',
    payload JSON NOT NULL COMMENT '消息内容',
    fail_reason VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    next_retry_at DATETIME DEFAULT NULL COMMENT '下次重试时间',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING、RETRYING、SUCCESS、FAILED、DEAD',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_retry (status, next_retry_at)
) COMMENT='WebSocket失败消息表';
```

重试处理流程如下：

```text
1. 消息发送失败或等待 ACK 超时
2. 写入失败消息表
3. 定时任务扫描 PENDING 或 RETRYING 记录
4. 判断 retry_count 是否超过上限
5. 未超过则重新推送
6. 推送成功更新 SUCCESS
7. 仍失败则更新 retry_count 和 next_retry_at
8. 超过上限转入 DEAD 或死信表
```

重试建议如下：

| 场景              | 是否重试               |
| ----------------- | ---------------------- |
| 网络抖动          | 可以重试               |
| Broker 临时不可用 | 可以重试               |
| 客户端未 ACK      | 可按消息类型重试       |
| 用户离线          | 不直接重试，转离线补偿 |
| 权限不足          | 不重试                 |
| 消息格式错误      | 不重试                 |
| 目标地址非法      | 不重试                 |

### 消息补偿机制

消息补偿机制用于在客户端断线、服务端重启、推送失败、客户端未 ACK、消息乱序等情况下恢复消息状态。补偿一般通过 HTTP 查询接口完成，而不是依赖 WebSocket 自动回放。

补偿策略如下：

| 消息类型 | 补偿方式                                |
| -------- | --------------------------------------- |
| 聊天消息 | 按 `chatId + lastSequence` 拉取缺失消息 |
| 用户通知 | 按 `lastSyncTime` 或未读状态查询        |
| 任务进度 | 查询任务当前最新状态                    |
| 告警消息 | 查询未确认或未关闭告警                  |
| 系统公告 | 查询有效期内未读公告                    |
| 看板数据 | 不补偿历史，只刷新最新快照              |

补偿接口示例：

```text
GET /api/ws/messages/compensate?lastSyncTime=1777956000000
GET /api/chat/messages/compensate?chatId=group-90001&lastSequence=30001
GET /api/notices/unread
GET /api/tasks/{taskId}
```

补偿流程如下：

```text
1. 客户端重连成功
2. 重新订阅 Topic 和用户队列
3. 读取本地 lastSyncTime 或 lastSequence
4. 调用 HTTP 补偿接口
5. 服务端查询持久化消息
6. 客户端去重、排序、合并
7. 更新本地同步游标
```

补偿机制的前提是关键消息已经持久化。如果消息没有落库，断线期间就无法可靠恢复。

### 消息过期策略

消息过期策略用于控制消息在一定时间后不再推送、不再补偿或不再展示。过期策略可以避免历史临时消息长期占用存储，也可以避免用户收到已经失去业务意义的通知。

过期字段建议如下：

| 字段           | 说明         |
| -------------- | ------------ |
| `expireAt`     | 消息过期时间 |
| `ttlSeconds`   | 消息有效秒数 |
| `expired`      | 是否已过期   |
| `expirePolicy` | 过期策略     |

不同消息的过期建议如下：

| 消息类型   | 过期策略                   |
| ---------- | -------------------------- |
| 聊天消息   | 通常不过期，按归档策略处理 |
| 系统公告   | 按公告有效期过期           |
| 待办通知   | 业务完成或撤销后过期       |
| 任务进度   | 任务结束后一段时间过期     |
| 告警消息   | 告警关闭后归档             |
| 看板快照   | 新快照覆盖旧快照           |
| 验证类消息 | 短 TTL，通常几分钟         |

过期扫描任务可以定期将过期消息更新为 `EXPIRED`，或查询时过滤过期记录。对于 Redis 离线消息，可以直接设置 TTL。

### 消息持久化

消息持久化用于保存关键消息，使系统能够支持历史查询、断线补偿、离线消息、失败重试、审计追踪和问题排查。持久化对象包括聊天消息、通知消息、回执、失败消息、离线消息和操作日志。

消息主表建议如下：

```sql
CREATE TABLE ws_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    message_type VARCHAR(64) NOT NULL COMMENT '消息类型',
    destination VARCHAR(256) DEFAULT NULL COMMENT '目标地址',
    sender_id VARCHAR(64) DEFAULT NULL COMMENT '发送人ID',
    receiver_type VARCHAR(32) DEFAULT NULL COMMENT '接收方类型',
    receiver_id VARCHAR(64) DEFAULT NULL COMMENT '接收方ID',
    biz_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    biz_id VARCHAR(64) DEFAULT NULL COMMENT '业务ID',
    payload JSON NOT NULL COMMENT '消息内容',
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '消息状态',
    need_ack TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要ACK',
    expire_at DATETIME DEFAULT NULL COMMENT '过期时间',
    sent_at DATETIME DEFAULT NULL COMMENT '发送时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_receiver (receiver_type, receiver_id),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) COMMENT='WebSocket消息表';
```

持久化策略建议如下：

| 类型         | 是否持久化         | 说明                 |
| ------------ | ------------------ | -------------------- |
| 聊天消息     | 是                 | 支持历史和离线补偿   |
| 用户通知     | 是                 | 支持通知中心和未读数 |
| 系统公告     | 是                 | 支持离线查看         |
| 任务进度     | 最终状态必须持久化 | 中间进度可按需保存   |
| 看板数据     | 否或聚合保存       | 通常只展示实时快照   |
| 在线状态变更 | 可选               | 审计需要时保存       |
| 心跳消息     | 否                 | 不应保存             |

### 离线消息存储

离线消息存储用于保存用户不在线期间需要补偿的消息。离线消息可以直接复用消息表，通过 `receiverId + status` 查询，也可以单独设计离线消息表。

离线消息表建议如下：

```sql
CREATE TABLE ws_offline_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    receiver_id VARCHAR(64) NOT NULL COMMENT '接收人ID',
    message_type VARCHAR(64) NOT NULL COMMENT '消息类型',
    payload JSON NOT NULL COMMENT '消息内容',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING、DELIVERED、EXPIRED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '补偿推送次数',
    expire_at DATETIME DEFAULT NULL COMMENT '过期时间',
    delivered_at DATETIME DEFAULT NULL COMMENT '补偿送达时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_message_receiver (message_id, receiver_id),
    KEY idx_receiver_status (receiver_id, status),
    KEY idx_expire_at (expire_at)
) COMMENT='WebSocket离线消息表';
```

离线消息处理流程如下：

```text
1. 服务端准备向用户推送消息
2. 查询用户是否在线
3. 在线则实时推送
4. 不在线则写入离线消息表
5. 用户重新连接后查询 PENDING 离线消息
6. 推送或通过 HTTP 返回给客户端
7. 客户端 ACK 后更新为 DELIVERED
```

离线消息建议如下：

| 建议             | 说明                                           |
| ---------------- | ---------------------------------------------- |
| 不保存所有消息   | 只保存关键消息                                 |
| 设置过期时间     | 临时提醒不应永久保存                           |
| 支持分页拉取     | 离线期间消息过多时分页补偿                     |
| ACK 后更新状态   | 确认送达后标记                                 |
| 大群消息慎用离线 | 大群建议按 sequence 补偿历史，而不是逐人存离线 |

### 失败消息记录

失败消息记录用于保存推送失败、处理异常、ACK 超时、外部 Broker 异常等消息。失败记录的目标不是让所有错误都立即重试，而是让系统可观测、可排查、可补偿。

失败原因建议分类如下：

| 失败原因                   | 是否可重试     |
| -------------------------- | -------------- |
| `USER_OFFLINE`             | 否，转离线消息 |
| `BROKER_UNAVAILABLE`       | 是             |
| `SEND_TIMEOUT`             | 是             |
| `ACK_TIMEOUT`              | 按消息类型     |
| `ACCESS_DENIED`            | 否             |
| `INVALID_DESTINATION`      | 否             |
| `PAYLOAD_SERIALIZE_FAILED` | 否             |
| `UNKNOWN_ERROR`            | 是，有限重试   |

失败记录建议包含以下字段：

| 字段          | 说明         |
| ------------- | ------------ |
| `messageId`   | 消息 ID      |
| `destination` | 目标地址     |
| `receiverId`  | 接收方       |
| `payload`     | 消息内容     |
| `failReason`  | 失败原因     |
| `retryCount`  | 重试次数     |
| `nextRetryAt` | 下次重试时间 |
| `status`      | 失败状态     |
| `traceId`     | 链路追踪 ID  |

失败消息记录不要保存敏感 Token、Cookie、密码、完整异常堆栈到业务表中。完整堆栈应进入日志系统，业务表中保存摘要和 traceId。

### 死信消息处理

死信消息用于存放达到最大重试次数仍然失败、无法解析、目标非法、权限拒绝或过期的消息。死信不一定要继续自动处理，但必须支持人工排查、重新投递或归档。

死信表建议如下：

```sql
CREATE TABLE ws_dead_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    original_destination VARCHAR(256) DEFAULT NULL COMMENT '原目标地址',
    receiver_id VARCHAR(64) DEFAULT NULL COMMENT '接收人ID',
    message_type VARCHAR(64) NOT NULL COMMENT '消息类型',
    payload JSON NOT NULL COMMENT '消息内容',
    dead_reason VARCHAR(1024) NOT NULL COMMENT '死信原因',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    handled TINYINT NOT NULL DEFAULT 0 COMMENT '是否已人工处理',
    handled_by VARCHAR(64) DEFAULT NULL COMMENT '处理人',
    handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_handled (handled),
    KEY idx_created_at (created_at)
) COMMENT='WebSocket死信消息表';
```

死信处理建议如下：

| 操作       | 说明                   |
| ---------- | ---------------------- |
| 查看死信   | 管理端展示死信列表     |
| 人工重投   | 修复问题后手动重新投递 |
| 标记已处理 | 不再自动处理           |
| 归档清理   | 定期归档历史死信       |
| 告警通知   | 死信数量超过阈值时告警 |
| 分类统计   | 按失败原因统计系统问题 |

## ACK 与事务机制

ACK 与事务机制用于保证消息处理流程在“接收、保存、推送、确认、失败回滚、补偿”之间具备明确边界。对于关键业务，不能在数据库事务未提交时提前通知客户端，也不能把 WebSocket 推送成功当作数据库成功的替代。

### 客户端 ACK

客户端 ACK 用于告诉服务端“客户端已经收到、读取或处理某条消息”。它通常通过业务路径实现，例如 `/app/message/ack`、`/app/chat/receipt`、`/app/notice/read`，而不是完全依赖 STOMP 协议层 ACK。

客户端 ACK 流程如下：

```text
1. 客户端收到 MESSAGE
2. 前端解析消息并完成去重
3. 对需要确认的消息发送 ACK
4. 服务端接收 ACK 请求
5. 校验当前用户是否为消息接收人
6. 幂等保存 ACK 记录
7. 更新消息状态或通知发送人
```

客户端 ACK 请求示例：

```json
{
  "messageId": "1909550919275921701",
  "ackType": "DELIVERED",
  "success": true,
  "ackAt": 1777956000000
}
```

ACK 控制器示例如下。

文件位置：`src/main/java/io/github/atengk/websocket/controller/WsAckController.java`

```java
package io.github.atengk.websocket.controller;

import io.github.atengk.websocket.dto.WsAckRequest;
import io.github.atengk.websocket.service.WsAckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket ACK 控制器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WsAckController {

    private final WsAckService wsAckService;

    /**
     * 处理客户端 ACK
     *
     * @param request   ACK 请求
     * @param principal 当前用户
     */
    @MessageMapping("/message/ack")
    public void ack(@Valid @Payload WsAckRequest request, Principal principal) {
        String userId = principal.getName();
        wsAckService.handleClientAck(userId, request);
        log.info("客户端 ACK 已处理，userId：{}，messageId：{}，ackType：{}",
                userId, request.getMessageId(), request.getAckType());
    }
}
```

ACK 服务接口如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsAckService.java`

```java
package io.github.atengk.websocket.service;

import io.github.atengk.websocket.dto.WsAckRequest;

/**
 * WebSocket ACK 服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsAckService {

    /**
     * 处理客户端 ACK
     *
     * @param userId  用户 ID
     * @param request ACK 请求
     */
    void handleClientAck(String userId, WsAckRequest request);

    /**
     * 处理服务端 ACK
     *
     * @param messageId 消息 ID
     * @param success   是否成功
     * @param reason    原因
     */
    void handleServerAck(String messageId, boolean success, String reason);
}
```

客户端 ACK 建议如下：

| 建议               | 说明                               |
| ------------------ | ---------------------------------- |
| ACK 必须幂等       | 重复 ACK 不应重复写入              |
| ACK 要校验归属     | 用户只能确认发给自己的消息         |
| ACK 可批量         | 高频聊天消息可批量确认             |
| 普通广播不逐人 ACK | 防止全站公告造成 ACK 风暴          |
| 关键消息需要 ACK   | 审批、任务结果、指令类消息建议启用 |

### 服务端 ACK

服务端 ACK 用于确认服务端已经收到客户端发送的业务消息，并完成基本处理。它可以让前端知道本次发送是否成功，例如聊天消息是否已保存、通知已读是否已处理、任务订阅是否成功。

服务端 ACK 与客户端 ACK 的区别如下：

| 类型       | 方向             | 说明                               |
| ---------- | ---------------- | ---------------------------------- |
| 服务端 ACK | 服务端 -> 客户端 | 告诉客户端请求已被服务端处理       |
| 客户端 ACK | 客户端 -> 服务端 | 告诉服务端消息已被客户端收到或处理 |

服务端 ACK 示例：

```json
{
  "messageId": "1909550919275921702",
  "type": "ACK",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "requestMessageId": "client-msg-001",
    "serverMessageId": "1909550919275921701",
    "ackType": "SERVER_SAVED",
    "success": true
  }
}
```

服务端 ACK 推送路径建议：

| 场景         | 路径                                    |
| ------------ | --------------------------------------- |
| 当前请求确认 | `/user/queue/ack`                       |
| 当前请求错误 | `/user/queue/errors`                    |
| 聊天发送成功 | `/user/queue/chat` 或 `/user/queue/ack` |
| 通知已读成功 | `/user/queue/ack`                       |

服务端 ACK 适合当前 Session 响应，不一定要推给用户所有端。如果只是确认当前页面发送的请求，建议使用指定 Session 推送。

### Receipt 机制

Receipt 是 STOMP 协议提供的一种确认机制。客户端可以在 `SEND` 或 `SUBSCRIBE` 帧中携带 `receipt` Header，请求服务端在处理该帧后返回 `RECEIPT` 帧。

示例：

```text
SEND
destination:/app/chat/private/send
receipt:client-receipt-001
content-type:application/json

{"receiverId":"10002","content":"hello"}^@
```

服务端返回：

```text
RECEIPT
receipt-id:client-receipt-001

^@
```

Receipt 的作用是确认协议帧被服务端接收和处理到某个阶段，但它不能替代业务落库成功、消息送达接收方或用户已读。业务关键场景仍应使用业务 ACK 和数据库状态。

Receipt 使用建议如下：

| 场景     | 建议                             |
| -------- | -------------------------------- |
| 订阅确认 | 可用于确认订阅请求已被服务端处理 |
| 发送确认 | 可用于确认客户端帧被接收         |
| 业务成功 | 不建议只依赖 Receipt             |
| 消息送达 | 不代表接收方客户端收到           |
| 调试联调 | 可用于前后端调试协议层问题       |

### 手动确认模式

手动确认模式是指客户端收到消息后，必须主动发送 ACK 或业务回执，服务端才认为消息完成。它适合关键通知、审批待办、任务结果、指令下发等需要明确确认的场景。

手动确认流程如下：

```text
1. 服务端生成需要 ACK 的消息
2. 消息状态为 PENDING_ACK
3. 推送给客户端
4. 客户端收到并处理
5. 客户端 SEND /app/message/ack
6. 服务端保存 ACK 回执
7. 消息状态更新为 DELIVERED 或 READ
8. ACK 超时则进入重试或补偿
```

需要 ACK 的消息体建议包含 `needAck`：

```json
{
  "messageId": "1909550919275921703",
  "type": "USER_NOTICE",
  "status": "PENDING_ACK",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "title": "重要审批通知",
    "content": "你有一条重要审批需要处理",
    "needAck": true
  }
}
```

手动确认建议如下：

| 建议              | 说明                       |
| ----------------- | -------------------------- |
| 只用于关键消息    | 不要对所有消息启用手动 ACK |
| 设置 ACK 超时时间 | 超时后进入重试或补偿       |
| 支持批量 ACK      | 降低客户端和服务端压力     |
| ACK 幂等          | 重复 ACK 不影响状态        |
| 与离线补偿结合    | 未 ACK 消息可在重连后补偿  |

### 自动确认模式

自动确认模式是指服务端只要完成推送调用或客户端只要收到消息就不再额外确认。它适合非关键广播、看板数据、在线状态、临时提醒等场景。

自动确认适用场景如下：

| 场景         | 说明                           |
| ------------ | ------------------------------ |
| 管理端看板   | 只需要最新快照，不需要逐条确认 |
| 在线状态广播 | 用户上下线事件可容忍丢失后刷新 |
| 普通系统公告 | 可通过 HTTP 查询历史补偿       |
| 高频任务进度 | 只关心最新进度                 |
| 非关键提示   | 丢失影响较小                   |

自动确认注意事项如下：

| 注意项               | 说明                             |
| -------------------- | -------------------------------- |
| 不代表可靠送达       | 自动确认只是降低复杂度           |
| 关键消息不适合       | 审批、订单、指令等应持久化并确认 |
| 应有补偿能力         | 断线后可通过 HTTP 查询最新状态   |
| 前端仍需去重         | 自动模式也可能重复推送           |
| 服务端应记录必要日志 | 便于排查推送问题                 |

### 消息处理事务

消息处理事务用于保证消息落库、状态更新、未读数变更、通知生成等数据库操作的一致性。WebSocket 推送本身不能参与数据库事务，因此应先完成数据库事务，再在事务提交后推送。

聊天消息事务流程如下：

```text
1. 开启数据库事务
2. 校验发送权限
3. 保存聊天消息
4. 更新会话最后一条消息
5. 更新接收方未读数
6. 注册事务提交后推送动作
7. 提交事务
8. afterCommit 执行 WebSocket 推送
```

事务处理示例：

```java
@Transactional(rollbackFor = Exception.class)
public void sendPrivateChat(ChatMessage message) {
    // 1. 校验权限
    // 2. 保存消息
    // 3. 更新会话和未读数

    wsPushService.sendAfterCommit(
            "/topic/admin/dashboard/message",
            WsMessage.success(WsMessageType.PRIVATE_CHAT, message)
    );
}
```

需要注意，上述示例中如果是用户点对点消息，应使用事务后执行 `sendToUser`。可以为 `WsPushService` 扩展 `sendToUserAfterCommit` 方法，避免事务内提前推送。

### 数据提交后推送

数据提交后推送用于避免事务回滚但客户端已经收到成功消息的问题。它是 WebSocket 与数据库事务结合时最重要的实践之一。

通用事务后执行工具类如下。

文件位置：`src/main/java/io/github/atengk/websocket/util/TransactionAfterCommitUtil.java`

```java
package io.github.atengk.websocket.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务提交后执行工具类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
public final class TransactionAfterCommitUtil {

    private TransactionAfterCommitUtil() {
    }

    /**
     * 在事务提交后执行任务
     *
     * @param runnable 任务
     */
    public static void runAfterCommit(Runnable runnable) {
        if (runnable == null) {
            return;
        }

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            runnable.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    log.error("事务提交后执行任务失败", e);
                }
            }
        });
    }
}
```

使用示例：

```java
TransactionAfterCommitUtil.runAfterCommit(() -> {
    wsPushService.sendToUser(receiverId, "/queue/chat", WsMessage.success(WsMessageType.PRIVATE_CHAT, message));
});
```

数据提交后推送建议如下：

| 场景               | 是否需要 afterCommit         |
| ------------------ | ---------------------------- |
| 聊天消息落库后推送 | 是                           |
| 通知记录生成后推送 | 是                           |
| 订单状态更新后推送 | 是                           |
| 审批待办生成后推送 | 是                           |
| 管理端临时看板刷新 | 不一定                       |
| 心跳和在线状态     | 不需要数据库事务时可直接推送 |

### 失败回滚处理

失败回滚处理用于定义数据库事务失败、推送失败、ACK 失败、重试失败时系统应该如何恢复。需要明确一点：数据库事务和 WebSocket 推送不在同一个事务资源中，不能指望 WebSocket 推送失败自动回滚数据库。

常见失败场景处理如下：

| 失败场景               | 处理方式                         |
| ---------------------- | -------------------------------- |
| 数据库保存失败         | 事务回滚，不推送消息             |
| 事务提交失败           | `afterCommit` 不执行，不推送消息 |
| 事务提交成功但推送失败 | 记录失败消息，后续重试或补偿     |
| 用户离线               | 写入离线消息或依赖历史查询补偿   |
| 客户端未 ACK           | 标记待确认超时，按策略重试或补偿 |
| 消息格式错误           | 记录失败，不重试                 |
| 权限校验失败           | 直接拒绝，不重试                 |

推荐失败处理流程：

```text
1. 执行业务事务
2. 事务失败：回滚并返回错误
3. 事务成功：afterCommit 执行推送
4. 推送成功：记录发送日志或更新状态
5. 推送失败：写入 ws_failed_message
6. 可重试：定时任务重试
7. 不可重试：转死信或标记失败
8. 客户端重连：通过补偿接口恢复最终状态
```

失败回滚设计建议如下：

| 建议                 | 说明                             |
| -------------------- | -------------------------------- |
| 不在事务内推送       | 避免脏通知                       |
| 推送失败不回滚业务   | 业务已提交后应通过补偿恢复       |
| 失败要落库           | 不能只打印日志                   |
| 区分可重试和不可重试 | 权限、参数错误不应重试           |
| 关键消息有补偿接口   | 用户刷新或重连后能查到最终状态   |
| 告警要及时           | 失败率异常时通知管理端或监控系统 |

对于核心业务，应以数据库最终状态为准，WebSocket 只作为实时触达通道。客户端收到推送后可以更新页面状态，但在关键操作确认、页面刷新、断线重连后，应通过 HTTP 查询或补偿接口重新校准最终状态。


## 异常处理

异常处理用于统一管理 WebSocket STOMP 在连接、认证、授权、消息解析、业务处理、订阅、发送和推送过程中的异常。由于 WebSocket 是长连接，异常处理不能只依赖 HTTP 的全局异常处理器，还需要结合 STOMP 通道拦截器、消息处理异常处理器、错误队列和日志审计共同完成。

### 连接异常

连接异常通常发生在 WebSocket 握手阶段、STOMP `CONNECT` 阶段、网关转发阶段或 Broker 连接阶段。连接异常会导致客户端无法建立长连接，常见表现是前端一直重连、连接立即关闭、控制台出现 `WebSocket connection failed` 或 STOMP `ERROR` 帧。

常见连接异常如下：

| 异常类型      | 常见原因                                          | 处理方式                                      |
| ------------- | ------------------------------------------------- | --------------------------------------------- |
| 握手失败      | Endpoint 地址错误、跨域不允许、网关未转发 Upgrade | 检查连接地址、跨域配置、Nginx 或 Gateway 配置 |
| 认证失败      | Token 缺失、Token 过期、Token 无效                | 返回认证失败错误，客户端跳转登录              |
| 连接被拒绝    | IP 黑名单、用户黑名单、连接数超限                 | 记录安全日志，必要时返回错误原因              |
| 心跳失败      | 客户端未发送心跳、网络中断、代理断开              | 客户端重连，服务端清理 Session                |
| Broker 不可用 | RabbitMQ、ActiveMQ 或 Broker Relay 异常           | 降级、告警、重试连接 Broker                   |

连接异常处理建议如下：

| 建议               | 说明                               |
| ------------------ | ---------------------------------- |
| 握手阶段记录 IP    | 便于排查跨域、黑名单和攻击来源     |
| 不打印完整 Token   | 日志中必须脱敏                     |
| 认证失败不无限重连 | 前端应识别认证错误并停止重连       |
| 网关超时要匹配心跳 | 网关读超时必须大于心跳间隔         |
| 高频连接失败要告警 | 可能是攻击、配置错误或 Broker 异常 |

连接异常日志建议包含 `sessionId`、`userId`、`ip`、`clientType`、`deviceId`、`reason` 和 `traceId`，但不要记录敏感凭证。

### 认证异常

认证异常用于处理用户身份无法识别的情况，通常发生在 STOMP `CONNECT` 阶段。认证异常包括 Token 缺失、Token 过期、Token 伪造、用户不存在、用户被禁用、登录态失效等。

认证异常码建议如下：

| 错误码                    | 说明         |
| ------------------------- | ------------ |
| `WS_AUTH_TOKEN_MISSING`   | Token 缺失   |
| `WS_AUTH_TOKEN_INVALID`   | Token 无效   |
| `WS_AUTH_TOKEN_EXPIRED`   | Token 已过期 |
| `WS_AUTH_USER_DISABLED`   | 用户已被禁用 |
| `WS_AUTH_USER_NOT_FOUND`  | 用户不存在   |
| `WS_AUTH_SESSION_EXPIRED` | 会话已过期   |

认证异常处理流程如下：

```text
1. 客户端发送 STOMP CONNECT
2. 服务端解析 Authorization 或 token
3. Token 校验失败
4. 抛出认证异常
5. 记录认证失败日志
6. 关闭连接或返回 ERROR
7. 客户端停止重连并跳转登录
```

认证异常建议在 `ChannelInterceptor` 中尽早抛出，避免未认证连接继续订阅或发送消息。

```java
if (StrUtil.isBlank(userId)) {
    log.warn("WebSocket 认证失败，sessionId：{}，ip：{}", accessor.getSessionId(), ip);
    throw new AccessDeniedException("WebSocket 认证失败");
}
```

前端收到认证异常后，不应继续无限重连，否则会造成无效连接风暴。应清理本地 Token，跳转登录页或触发刷新 Token 流程。

### 授权异常

授权异常用于处理“用户已认证，但没有权限执行当前操作”的情况。它通常发生在 `SUBSCRIBE` 和 `SEND` 阶段，例如普通用户订阅管理端主题、非群成员订阅群组、被禁言用户发送群消息、用户操作不属于自己的通知等。

授权异常码建议如下：

| 错误码                    | 说明         |
| ------------------------- | ------------ |
| `WS_ACCESS_DENIED`        | 无访问权限   |
| `WS_SUBSCRIBE_DENIED`     | 无订阅权限   |
| `WS_SEND_DENIED`          | 无发送权限   |
| `WS_GROUP_ACCESS_DENIED`  | 无群组权限   |
| `WS_ADMIN_ACCESS_DENIED`  | 无管理端权限 |
| `WS_TENANT_ACCESS_DENIED` | 无租户权限   |

授权异常处理建议如下：

| 场景               | 处理方式               |
| ------------------ | ---------------------- |
| 非法订阅管理端路径 | 拒绝订阅，记录安全日志 |
| 非群成员订阅群组   | 拒绝订阅，返回错误     |
| 被禁言用户发群消息 | 拒绝发送，推送错误消息 |
| 操作他人通知       | 拒绝处理，记录审计     |
| 管理端权限不足     | 拒绝并记录操作人       |

授权异常不建议直接关闭整个连接，除非用户存在明显攻击行为。一般可以拒绝当前帧，并向 `/user/queue/errors` 返回错误消息，让客户端展示友好提示。

### 消息格式异常

消息格式异常用于处理客户端发送的消息体不符合约定的情况，例如 JSON 格式错误、字段缺失、字段类型错误、消息过长、枚举值非法、协议版本不支持等。

消息格式异常码建议如下：

| 错误码                           | 说明           |
| -------------------------------- | -------------- |
| `WS_MESSAGE_EMPTY`               | 消息体为空     |
| `WS_MESSAGE_JSON_INVALID`        | JSON 格式错误  |
| `WS_MESSAGE_FIELD_MISSING`       | 必填字段缺失   |
| `WS_MESSAGE_FIELD_INVALID`       | 字段值非法     |
| `WS_MESSAGE_TOO_LARGE`           | 消息体过大     |
| `WS_MESSAGE_VERSION_UNSUPPORTED` | 消息版本不支持 |

消息格式异常处理建议如下：

| 建议                 | 说明                         |
| -------------------- | ---------------------------- |
| 使用 Bean Validation | 对 `@Payload` DTO 做字段校验 |
| 限制消息长度         | 防止大消息造成内存压力       |
| 枚举值校验           | 不识别的类型直接拒绝         |
| JSON 异常可读        | 返回明确字段错误             |
| 不打印完整大消息     | 避免日志膨胀和敏感信息泄露   |

请求 DTO 示例：

```java
package io.github.atengk.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * WebSocket 文本消息请求
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class WsTextMessageRequest {

    /**
     * 目标用户 ID
     */
    private String receiverId;

    /**
     * 群组 ID
     */
    private String groupId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过 2000 个字符")
    private String content;

    /**
     * 客户端消息 ID
     */
    @Size(max = 128, message = "客户端消息ID不能超过 128 个字符")
    private String clientMessageId;
}
```

### 业务处理异常

业务处理异常用于处理认证、格式校验通过后，业务规则执行失败的情况。例如群组不存在、消息接收人不存在、用户被拉黑、任务不存在、通知已过期、审批已处理等。

业务异常码建议如下：

| 错误码                    | 说明         |
| ------------------------- | ------------ |
| `WS_BIZ_GROUP_NOT_FOUND`  | 群组不存在   |
| `WS_BIZ_USER_NOT_FOUND`   | 用户不存在   |
| `WS_BIZ_MESSAGE_RECALLED` | 消息已撤回   |
| `WS_BIZ_MESSAGE_EXPIRED`  | 消息已过期   |
| `WS_BIZ_USER_MUTED`       | 用户已被禁言 |
| `WS_BIZ_NOTICE_HANDLED`   | 通知已处理   |
| `WS_BIZ_TASK_NOT_FOUND`   | 任务不存在   |

业务处理异常建议返回给当前用户，而不是广播。推荐路径如下：

```text
/user/queue/errors
```

业务异常返回示例：

```json
{
  "messageId": "1909550919275921801",
  "type": "ERROR",
  "status": "FAILED",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "code": "WS_BIZ_USER_MUTED",
    "message": "当前用户已被禁言，无法发送群消息",
    "destination": "/app/chat/group/send",
    "requestMessageId": "client-msg-001",
    "retryable": false
  }
}
```

### 目标地址异常

目标地址异常用于处理 STOMP destination 不合法或不允许访问的情况。目标地址异常是 WebSocket 安全的重要组成部分，因为客户端可以手工构造任意目标地址。

非法目标地址示例：

```text
/topic/admin/alarm          普通用户非法订阅
/topic/group/90001          非群成员非法订阅
/topic/../../admin          异常路径
/app/admin/session/kick     普通用户非法发送
/user/queue/notice          客户端非法 SEND
```

目标地址校验规则如下：

| 校验项                             | 规则       |
| ---------------------------------- | ---------- |
| 空地址                             | 拒绝       |
| 包含 `..`                          | 拒绝       |
| 包含 `//`                          | 拒绝       |
| 包含反斜杠                         | 拒绝       |
| `SEND` 非 `/app/**`                | 拒绝       |
| `SUBSCRIBE /topic/admin/**`        | 管理员校验 |
| `SUBSCRIBE /topic/group/{groupId}` | 群成员校验 |

目标地址校验工具类如下。

文件位置：`src/main/java/io/github/atengk/websocket/util/WsDestinationValidator.java`

```java
package io.github.atengk.websocket.util;

import cn.hutool.core.util.StrUtil;

/**
 * WebSocket 目标地址校验工具类
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class WsDestinationValidator {

    private WsDestinationValidator() {
    }

    /**
     * 判断目标地址格式是否非法
     *
     * @param destination 目标地址
     * @return 是否非法
     */
    public static boolean isInvalid(String destination) {
        return StrUtil.isBlank(destination)
                || StrUtil.contains(destination, "..")
                || StrUtil.contains(destination, "\\")
                || StrUtil.contains(destination, "//");
    }

    /**
     * 判断是否为客户端允许发送的地址
     *
     * @param destination 目标地址
     * @return 是否允许发送
     */
    public static boolean isAllowedSendDestination(String destination) {
        return !isInvalid(destination) && StrUtil.startWith(destination, "/app");
    }

    /**
     * 判断是否为管理端地址
     *
     * @param destination 目标地址
     * @return 是否为管理端地址
     */
    public static boolean isAdminDestination(String destination) {
        return StrUtil.startWith(destination, "/topic/admin") || StrUtil.startWith(destination, "/app/admin");
    }
}
```

### 订阅异常

订阅异常发生在客户端订阅目标地址失败时。常见原因包括未认证、无权限、目标地址非法、群组不存在、租户不匹配、订阅数量超限等。

订阅异常码建议如下：

| 错误码                             | 说明         |
| ---------------------------------- | ------------ |
| `WS_SUBSCRIBE_UNAUTHENTICATED`     | 未认证       |
| `WS_SUBSCRIBE_DENIED`              | 无订阅权限   |
| `WS_SUBSCRIBE_DESTINATION_INVALID` | 订阅地址非法 |
| `WS_SUBSCRIBE_GROUP_NOT_FOUND`     | 群组不存在   |
| `WS_SUBSCRIBE_LIMIT_EXCEEDED`      | 订阅数量超限 |

订阅异常处理建议如下：

| 建议         | 说明                                |
| ------------ | ----------------------------------- |
| 拒绝非法订阅 | 不允许订阅建立                      |
| 记录安全日志 | 记录 userId、sessionId、destination |
| 返回错误消息 | 向当前用户错误队列返回错误          |
| 高频异常限流 | 多次非法订阅可断开连接或加入黑名单  |
| 前端取消重试 | 权限异常不应重复订阅                |

### 发送异常

发送异常发生在客户端向服务端发送消息失败，或服务端向客户端推送消息失败时。需要区分“客户端 SEND 异常”和“服务端推送异常”。

客户端 SEND 异常包括：

| 异常     | 说明                     |
| -------- | ------------------------ |
| 未认证   | 用户未登录或 Token 失效  |
| 无权限   | 不能向目标业务对象发送   |
| 参数错误 | 消息体不合法             |
| 频率过高 | 触发发送限流             |
| 业务失败 | 群组不存在、用户被禁言等 |

服务端推送异常包括：

| 异常         | 说明                         |
| ------------ | ---------------------------- |
| 用户离线     | 目标用户无在线 Session       |
| Broker 异常  | 外部消息代理不可用           |
| 序列化失败   | 消息体无法转换 JSON          |
| 目标地址错误 | destination 非法             |
| 发送超时     | 出站通道阻塞或 Broker 无响应 |

发送异常处理建议如下：

| 场景           | 处理方式                  |
| -------------- | ------------------------- |
| 客户端请求错误 | 返回 `/user/queue/errors` |
| 用户离线       | 写入离线消息或依赖补偿    |
| Broker 异常    | 记录失败消息并告警        |
| 序列化失败     | 记录失败，不重试          |
| 权限错误       | 直接拒绝，不重试          |
| 出站通道阻塞   | 降级、限流、记录慢日志    |

### 全局异常处理

WebSocket STOMP 的全局异常处理可以使用 `@MessageExceptionHandler` 和 `@ControllerAdvice` 处理 `@MessageMapping` 方法内部抛出的异常。它不完全等同于 HTTP 全局异常处理器，但可以统一处理业务异常、参数异常和未捕获异常。

异常响应 DTO 如下。

文件位置：`src/main/java/io/github/atengk/websocket/dto/WsErrorResponse.java`

```java
package io.github.atengk.websocket.dto;

import lombok.Builder;
import lombok.Data;

/**
 * WebSocket 异常响应
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Builder
public class WsErrorResponse {

    /**
     * 错误码
     */
    private String code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 目标地址
     */
    private String destination;

    /**
     * 原请求消息 ID
     */
    private String requestMessageId;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 是否可重试
     */
    private Boolean retryable;
}
```

全局异常处理器如下。

文件位置：`src/main/java/io/github/atengk/websocket/handler/WsGlobalExceptionHandler.java`

```java
package io.github.atengk.websocket.handler;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.dto.WsErrorResponse;
import io.github.atengk.websocket.enums.WsMessageStatus;
import io.github.atengk.websocket.enums.WsMessageType;
import io.github.atengk.websocket.model.WsMessage;
import io.github.atengk.websocket.service.WsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WsGlobalExceptionHandler {

    private final WsPushService wsPushService;

    /**
     * 处理权限异常
     *
     * @param exception 权限异常
     * @return 错误消息
     */
    @MessageExceptionHandler(AccessDeniedException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    public WsMessage<WsErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("WebSocket 权限异常：{}", exception.getMessage());

        WsErrorResponse response = WsErrorResponse.builder()
                .code("WS_ACCESS_DENIED")
                .message(StrUtil.blankToDefault(exception.getMessage(), "无操作权限"))
                .retryable(false)
                .build();

        return buildErrorMessage(response);
    }

    /**
     * 处理参数异常
     *
     * @param exception 参数异常
     * @return 错误消息
     */
    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    public WsMessage<WsErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("WebSocket 参数异常：{}", exception.getMessage());

        WsErrorResponse response = WsErrorResponse.builder()
                .code("WS_PARAM_INVALID")
                .message(StrUtil.blankToDefault(exception.getMessage(), "请求参数非法"))
                .retryable(false)
                .build();

        return buildErrorMessage(response);
    }

    /**
     * 处理未知异常
     *
     * @param exception 未知异常
     * @return 错误消息
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    public WsMessage<WsErrorResponse> handleException(Exception exception) {
        log.error("WebSocket 未知异常", exception);

        WsErrorResponse response = WsErrorResponse.builder()
                .code("WS_INTERNAL_ERROR")
                .message("服务处理异常，请稍后重试")
                .retryable(true)
                .build();

        return buildErrorMessage(response);
    }

    /**
     * 构建错误消息
     *
     * @param response 错误响应
     * @return WebSocket 错误消息
     */
    private WsMessage<WsErrorResponse> buildErrorMessage(WsErrorResponse response) {
        WsMessage<WsErrorResponse> message = WsMessage.failed(WsMessageType.ERROR, response);
        message.setStatus(WsMessageStatus.FAILED);
        return message;
    }
}
```

需要注意，`@MessageExceptionHandler` 主要处理消息处理方法中的异常。`ChannelInterceptor` 中的异常可能在更早阶段发生，需要通过拦截器日志、STOMP ERROR、连接关闭或自定义错误处理机制补充处理。

### 异常消息返回

异常消息返回用于让客户端明确知道当前操作失败原因。异常消息建议统一返回到 `/user/queue/errors`，并使用统一错误结构，便于前端弹窗、提示、重试和日志上报。

异常消息统一结构如下：

```json
{
  "messageId": "1909550919275921802",
  "type": "ERROR",
  "status": "FAILED",
  "version": "1.0",
  "timestamp": 1777956000000,
  "traceId": "trace-20260505-000001",
  "data": {
    "code": "WS_GROUP_ACCESS_DENIED",
    "message": "无群组访问权限",
    "destination": "/topic/group/90001",
    "requestMessageId": "client-msg-001",
    "retryable": false
  }
}
```

错误返回建议如下：

| 字段               | 说明                         |
| ------------------ | ---------------------------- |
| `code`             | 稳定错误码，前端可根据它处理 |
| `message`          | 可读错误信息                 |
| `destination`      | 出错目标地址                 |
| `requestMessageId` | 对应客户端请求 ID            |
| `traceId`          | 后端链路追踪 ID              |
| `retryable`        | 是否建议前端重试             |

前端处理建议如下：

| 错误类型 | 前端行为                 |
| -------- | ------------------------ |
| 认证失败 | 清理登录态，跳转登录     |
| 授权失败 | 提示无权限，停止重复订阅 |
| 参数错误 | 提示用户修正输入         |
| 频率限制 | 提示稍后再试             |
| 服务异常 | 可重试或提示刷新         |
| 被踢下线 | 主动断开连接并跳转登录   |

## 数据持久化设计

数据持久化设计用于支撑消息历史、通知中心、离线补偿、已读未读、回执、在线状态、群组成员、失败消息、操作审计和数据归档。WebSocket STOMP 是实时通道，但关键业务不能只依赖内存状态，必须通过数据库、Redis 和日志系统保留可恢复的数据。

### 消息表设计

消息表用于保存通用 WebSocket 消息记录，适合通知、业务事件、告警、任务结果等非聊天专用消息。聊天消息可以使用独立聊天消息表，也可以复用消息表并增加 chat 字段。生产项目中建议聊天消息独立建表，通用通知和业务推送使用通用消息表。

```sql
CREATE TABLE ws_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    message_type VARCHAR(64) NOT NULL COMMENT '消息类型',
    destination VARCHAR(256) DEFAULT NULL COMMENT 'STOMP目标地址',
    sender_id VARCHAR(64) DEFAULT NULL COMMENT '发送人ID',
    receiver_type VARCHAR(32) DEFAULT NULL COMMENT '接收方类型：USER、GROUP、TOPIC、TENANT、ROLE',
    receiver_id VARCHAR(64) DEFAULT NULL COMMENT '接收方ID',
    biz_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    biz_id VARCHAR(64) DEFAULT NULL COMMENT '业务ID',
    payload JSON NOT NULL COMMENT '消息内容',
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '消息状态',
    need_ack TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要ACK',
    expire_at DATETIME DEFAULT NULL COMMENT '过期时间',
    sent_at DATETIME DEFAULT NULL COMMENT '发送时间',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_receiver (receiver_type, receiver_id),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) COMMENT='WebSocket消息表';
```

字段说明如下：

| 字段                 | 说明                               |
| -------------------- | ---------------------------------- |
| `message_id`         | 全局唯一消息 ID                    |
| `message_type`       | 消息类型，例如通知、告警、任务进度 |
| `destination`        | 推送目标地址                       |
| `receiver_type`      | 接收方类型                         |
| `receiver_id`        | 接收方 ID                          |
| `biz_type`、`biz_id` | 关联业务对象                       |
| `payload`            | 实际消息 JSON                      |
| `need_ack`           | 是否需要客户端确认                 |
| `expire_at`          | 消息过期时间                       |
| `trace_id`           | 链路追踪 ID                        |

### 会话表设计

会话表用于保存 WebSocket 连接记录和会话审计。实时 Session 通常保存在内存或 Redis 中，数据库会话表用于审计、统计和问题排查，不建议高频更新每次心跳。

```sql
CREATE TABLE ws_session_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    session_id VARCHAR(128) NOT NULL COMMENT 'WebSocket Session ID',
    user_id VARCHAR(64) DEFAULT NULL COMMENT '用户ID',
    client_type VARCHAR(32) DEFAULT NULL COMMENT '客户端类型',
    device_id VARCHAR(128) DEFAULT NULL COMMENT '设备ID',
    ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    origin VARCHAR(256) DEFAULT NULL COMMENT '请求来源',
    connect_time DATETIME DEFAULT NULL COMMENT '连接时间',
    disconnect_time DATETIME DEFAULT NULL COMMENT '断开时间',
    close_status VARCHAR(64) DEFAULT NULL COMMENT '关闭状态',
    close_reason VARCHAR(512) DEFAULT NULL COMMENT '关闭原因',
    status VARCHAR(32) NOT NULL DEFAULT 'CONNECTED' COMMENT '会话状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_user_id (user_id),
    KEY idx_connect_time (connect_time),
    KEY idx_status (status)
) COMMENT='WebSocket会话日志表';
```

会话表使用建议如下：

| 场景       | 说明                      |
| ---------- | ------------------------- |
| 连接成功   | 插入一条会话日志          |
| 正常断开   | 更新断开时间和关闭状态    |
| 异常关闭   | 记录异常原因              |
| 管理端查询 | 可查看历史连接记录        |
| 安全审计   | 关联 IP、设备、来源和用户 |

### 用户在线状态表设计

用户在线状态表用于保存用户级别的在线状态，例如当前是否在线、最近上线时间、最近离线时间和最后活跃时间。它不是 Session 明细表，而是用户状态聚合表。

```sql
CREATE TABLE ws_user_online_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    status VARCHAR(32) NOT NULL COMMENT '在线状态：ONLINE、OFFLINE、BUSY、AWAY',
    last_session_id VARCHAR(128) DEFAULT NULL COMMENT '最近一次Session ID',
    client_type VARCHAR(32) DEFAULT NULL COMMENT '最近客户端类型',
    device_id VARCHAR(128) DEFAULT NULL COMMENT '最近设备ID',
    ip VARCHAR(64) DEFAULT NULL COMMENT '最近客户端IP',
    online_at DATETIME DEFAULT NULL COMMENT '最近上线时间',
    offline_at DATETIME DEFAULT NULL COMMENT '最近离线时间',
    last_active_at DATETIME DEFAULT NULL COMMENT '最后活跃时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_status (status),
    KEY idx_last_active_at (last_active_at)
) COMMENT='WebSocket用户在线状态表';
```

在线状态表建议如下：

| 建议                 | 说明                       |
| -------------------- | -------------------------- |
| 用户维度唯一         | `user_id` 使用唯一索引     |
| Session 明细放 Redis | 数据库只保存聚合状态       |
| 下线延迟判断         | 多端全部断开后再更新为离线 |
| 活跃时间低频更新     | 不要每个心跳都写数据库     |
| 最近在线可展示       | 用户列表可展示最近在线时间 |

### 群组表设计

群组表用于保存群组、房间、项目空间等多人通信容器的基础信息。稳定群组建议持久化，临时房间可根据业务选择 Redis 或数据库。

```sql
CREATE TABLE ws_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    group_id VARCHAR(64) NOT NULL COMMENT '群组ID',
    group_name VARCHAR(128) NOT NULL COMMENT '群组名称',
    group_type VARCHAR(32) NOT NULL COMMENT '群组类型：CHAT、ROOM、PROJECT、ORG',
    owner_id VARCHAR(64) NOT NULL COMMENT '群主用户ID',
    tenant_id VARCHAR(64) DEFAULT NULL COMMENT '租户ID',
    description VARCHAR(512) DEFAULT NULL COMMENT '群组描述',
    avatar_url VARCHAR(512) DEFAULT NULL COMMENT '群组头像',
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '群组状态：NORMAL、DISABLED、DISBANDED、ARCHIVED',
    created_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_group_id (group_id),
    KEY idx_owner_id (owner_id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_status (status)
) COMMENT='WebSocket群组表';
```

群组表设计建议如下：

| 字段          | 说明                       |
| ------------- | -------------------------- |
| `group_id`    | 业务层群组唯一标识         |
| `group_type`  | 区分聊天群、房间、项目空间 |
| `owner_id`    | 群主或拥有者               |
| `tenant_id`   | 多租户隔离                 |
| `status`      | 控制是否允许订阅和发送     |
| `description` | 群组说明                   |
| `avatar_url`  | 群头像或图标               |

### 群组成员表设计

群组成员表用于保存用户与群组之间的成员关系、角色、状态、禁言时间和加入时间。订阅群组和发送群组消息都必须基于该表或其缓存进行权限校验。

```sql
CREATE TABLE ws_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    group_id VARCHAR(64) NOT NULL COMMENT '群组ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT '成员角色：OWNER、ADMIN、MEMBER、GUEST',
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '成员状态：NORMAL、MUTED、KICKED、EXITED、BANNED',
    muted_until DATETIME DEFAULT NULL COMMENT '禁言截止时间',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    exited_at DATETIME DEFAULT NULL COMMENT '退出时间',
    operator_id VARCHAR(64) DEFAULT NULL COMMENT '最近操作人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_group_user (group_id, user_id),
    KEY idx_user_id (user_id),
    KEY idx_group_id (group_id),
    KEY idx_status (status)
) COMMENT='WebSocket群组成员表';
```

群组成员表建议如下：

| 设计点         | 说明                                 |
| -------------- | ------------------------------------ |
| 使用唯一索引   | `group_id + user_id` 防止重复加入    |
| 保留退出记录   | 可使用状态标记，便于审计             |
| 禁言用时间字段 | `muted_until` 大于当前时间表示仍禁言 |
| 角色独立字段   | 便于权限矩阵判断                     |
| 缓存成员关系   | Redis 缓存提升订阅鉴权性能           |

### 通知表设计

通知表用于保存用户通知、系统公告、审批待办、告警提醒、订单通知等记录。通知表是通知中心、未读数、离线补偿和历史查询的基础。

```sql
CREATE TABLE ws_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    notice_id VARCHAR(64) NOT NULL COMMENT '通知ID',
    receiver_id VARCHAR(64) NOT NULL COMMENT '接收人ID',
    title VARCHAR(128) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    notice_type VARCHAR(64) NOT NULL COMMENT '通知类型',
    level VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '通知级别：NORMAL、IMPORTANT、WARN、ERROR',
    biz_id VARCHAR(64) DEFAULT NULL COMMENT '业务ID',
    biz_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    url VARCHAR(512) DEFAULT NULL COMMENT '跳转地址',
    status VARCHAR(32) NOT NULL DEFAULT 'UNREAD' COMMENT '状态：UNREAD、READ、DONE、CANCELED、EXPIRED',
    need_ack TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要ACK',
    read_at DATETIME DEFAULT NULL COMMENT '已读时间',
    expire_at DATETIME DEFAULT NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_notice_receiver (notice_id, receiver_id),
    KEY idx_receiver_status (receiver_id, status),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_created_at (created_at),
    KEY idx_expire_at (expire_at)
) COMMENT='WebSocket通知表';
```

通知表设计建议如下：

| 设计点         | 说明                               |
| -------------- | ---------------------------------- |
| 接收人维度存储 | 每个接收人一条记录，方便已读未读   |
| 支持业务关联   | `biz_type + biz_id` 便于跳转和幂等 |
| 支持状态流转   | 未读、已读、已处理、取消、过期     |
| 支持过期时间   | 临时通知自动过期                   |
| 支持唯一约束   | 防止重复生成相同通知               |

### 消息回执表设计

消息回执表用于记录用户对消息的送达、已读、处理成功、处理失败等确认状态。它适合聊天、通知、任务结果和指令类消息。

```sql
CREATE TABLE ws_message_receipt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    receipt_id VARCHAR(64) NOT NULL COMMENT '回执ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    user_id VARCHAR(64) NOT NULL COMMENT '回执用户ID',
    receipt_type VARCHAR(32) NOT NULL COMMENT '回执类型：DELIVERED、READ、PROCESSED、FAILED',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功',
    reason VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    receipt_at DATETIME NOT NULL COMMENT '回执时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_receipt_id (receipt_id),
    UNIQUE KEY uk_message_user_type (message_id, user_id, receipt_type),
    KEY idx_message_id (message_id),
    KEY idx_user_id (user_id),
    KEY idx_receipt_at (receipt_at)
) COMMENT='WebSocket消息回执表';
```

回执表设计建议如下：

| 建议             | 说明                                       |
| ---------------- | ------------------------------------------ |
| 回执幂等         | `message_id + user_id + receipt_type` 唯一 |
| 回执类型明确     | 区分送达、已读、处理                       |
| 失败原因可选     | 客户端处理失败时记录摘要                   |
| 高频场景批量写入 | 大群已读不建议逐条实时写                   |
| 定期归档         | 历史回执数据量可能很大                     |

### 离线消息表设计

离线消息表用于保存用户离线期间仍需补偿的消息。对于聊天大群，通常不建议逐用户保存所有离线消息，而是通过历史消息表和 sequence 补偿。对于私有通知、审批待办、关键提醒，可以使用离线消息表。

```sql
CREATE TABLE ws_offline_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    receiver_id VARCHAR(64) NOT NULL COMMENT '接收人ID',
    message_type VARCHAR(64) NOT NULL COMMENT '消息类型',
    payload JSON NOT NULL COMMENT '消息内容',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING、DELIVERED、EXPIRED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '补偿推送次数',
    expire_at DATETIME DEFAULT NULL COMMENT '过期时间',
    delivered_at DATETIME DEFAULT NULL COMMENT '补偿送达时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_message_receiver (message_id, receiver_id),
    KEY idx_receiver_status (receiver_id, status),
    KEY idx_expire_at (expire_at),
    KEY idx_created_at (created_at)
) COMMENT='WebSocket离线消息表';
```

离线消息使用建议如下：

| 场景         | 建议                                 |
| ------------ | ------------------------------------ |
| 用户通知     | 可写入离线消息                       |
| 单聊消息     | 可通过聊天表补偿，不一定单独写离线表 |
| 群聊消息     | 建议按 sequence 补偿                 |
| 任务最终结果 | 可写入离线消息                       |
| 高频任务进度 | 不建议逐条离线保存，只保存最终状态   |
| 系统公告     | 查询公告表补偿即可                   |

### 操作日志表设计

操作日志表用于记录管理端操作、强制下线、踢人、禁言、解散群组、推送测试、公告发布、黑名单处理等行为。操作日志是安全审计和问题追责的基础。

```sql
CREATE TABLE ws_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    operation_id VARCHAR(64) NOT NULL COMMENT '操作ID',
    operator_id VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(64) DEFAULT NULL COMMENT '目标类型',
    target_id VARCHAR(128) DEFAULT NULL COMMENT '目标ID',
    content VARCHAR(1024) DEFAULT NULL COMMENT '操作内容摘要',
    ip VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    result VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '操作结果：SUCCESS、FAILED',
    fail_reason VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_operation_id (operation_id),
    KEY idx_operator_id (operator_id),
    KEY idx_operation_type (operation_type),
    KEY idx_target (target_type, target_id),
    KEY idx_created_at (created_at)
) COMMENT='WebSocket操作日志表';
```

操作日志类型建议如下：

| 操作类型           | 说明         |
| ------------------ | ------------ |
| `KICK_SESSION`     | 踢出指定连接 |
| `KICK_USER`        | 强制用户下线 |
| `GROUP_MUTE`       | 群组禁言     |
| `GROUP_KICK`       | 群组踢人     |
| `GROUP_DISBAND`    | 群组解散     |
| `NOTICE_PUBLISH`   | 发布通知     |
| `PUSH_TEST`        | 推送测试     |
| `BLACKLIST_ADD`    | 加入黑名单   |
| `BLACKLIST_REMOVE` | 移除黑名单   |

操作日志建议如下：

| 建议               | 说明                             |
| ------------------ | -------------------------------- |
| 管理端操作必须记录 | 尤其是踢人、禁言、公告、黑名单   |
| 内容保存摘要       | 不保存完整敏感数据               |
| 记录操作结果       | 成功和失败都应记录               |
| 保留 traceId       | 便于关联接口日志                 |
| 支持审计查询       | 管理端可按操作人、时间、类型查询 |

### 数据归档策略

数据归档策略用于控制消息表、通知表、回执表、会话日志表和操作日志表的数据增长。WebSocket 系统的数据量可能很大，尤其是聊天消息、回执、连接日志和失败消息，必须提前设计保留周期、冷热分层和清理策略。

推荐保留策略如下：

| 数据类型 | 建议保留周期              | 处理方式           |
| -------- | ------------------------- | ------------------ |
| 聊天消息 | 6 个月到 3 年，按业务要求 | 分表、归档、冷存储 |
| 通知消息 | 3 到 12 个月              | 过期归档或删除     |
| 回执数据 | 1 到 6 个月               | 聚合后归档         |
| 离线消息 | 7 到 30 天                | 过期删除           |
| 失败消息 | 30 到 90 天               | 处理后归档         |
| 死信消息 | 90 到 180 天              | 人工处理后归档     |
| 会话日志 | 30 到 180 天              | 安全审计要求决定   |
| 操作日志 | 1 到 3 年                 | 审计要求决定       |

归档任务建议如下：

```text
1. 定时扫描过期数据
2. 按时间范围分页处理
3. 将历史数据写入归档表或对象存储
4. 校验归档数量
5. 删除主表历史数据
6. 记录归档日志
```

归档表命名建议如下：

```text
ws_message_archive_2026
ws_notice_archive_2026
ws_message_receipt_archive_2026
ws_session_log_archive_2026
ws_operation_log_archive_2026
```

数据归档注意事项如下：

| 注意项           | 说明                             |
| ---------------- | -------------------------------- |
| 避免大事务删除   | 分批删除，每批 500 到 5000 条    |
| 避免影响在线业务 | 归档任务放在低峰期               |
| 建立时间索引     | 所有大表必须有 `created_at` 索引 |
| 归档前校验       | 确认归档成功后再删除主表         |
| 保留审计数据     | 操作日志按合规要求保留           |
| 大表考虑分区     | MySQL 可按月或按年分区           |
| 附件单独归档     | 附件文件和消息记录生命周期要协调 |


## Redis 集成

Redis 集成用于解决 WebSocket STOMP 在生产环境中的分布式状态管理问题，包括在线用户、Session 映射、群组成员、未读数、消息去重、分布式锁、跨实例广播、轻量消息队列和缓存过期。单实例可以使用内存 Map 管理连接状态，但多实例部署后，必须引入 Redis 共享状态，否则不同节点之间无法感知用户在线、群组成员和消息投递状态。

### 在线用户缓存

在线用户缓存用于记录当前在线用户集合。它通常使用 Redis Set 保存用户 ID，支持快速判断用户是否在线、统计在线用户数、查询在线用户列表。

推荐 Redis Key 设计如下：

```text
ws:online:users
```

数据结构建议：

| Key               | 类型 | Value    | 说明                 |
| ----------------- | ---- | -------- | -------------------- |
| `ws:online:users` | Set  | `userId` | 当前在线用户 ID 集合 |

上线时加入集合：

```text
SADD ws:online:users 10001
```

下线时，如果用户没有其他有效 Session，再从集合移除：

```text
SREM ws:online:users 10001
```

在线用户缓存处理规则如下：

| 场景                  | Redis 操作                         |
| --------------------- | ---------------------------------- |
| 用户首个 Session 连接 | `SADD ws:online:users userId`      |
| 用户新增多端连接      | 保持用户在线集合不变               |
| 用户某个 Session 断开 | 判断是否还有其他 Session           |
| 用户全部 Session 断开 | `SREM ws:online:users userId`      |
| 查询用户是否在线      | `SISMEMBER ws:online:users userId` |
| 统计在线人数          | `SCARD ws:online:users`            |

在线用户缓存不应单独作为最终判断依据。正确方式是结合 `ws:user:sessions:{userId}` 判断该用户是否还有有效 Session，防止某个 Session 断开时误判用户离线。

### Session 映射缓存

Session 映射缓存用于维护用户与 WebSocket Session 之间的关系。在集群部署下，某个用户可能连接到任意实例，Redis 需要记录 Session 属于哪个用户、哪个实例、哪个设备和最后活跃时间。

推荐 Redis Key 设计如下：

```text
ws:user:sessions:{userId}
ws:session:{sessionId}
ws:instance:sessions:{instanceId}
```

数据结构建议：

| Key                                 | 类型 | 说明                          |
| ----------------------------------- | ---- | ----------------------------- |
| `ws:user:sessions:{userId}`         | Set  | 某个用户的全部 Session ID     |
| `ws:session:{sessionId}`            | Hash | Session 详细信息              |
| `ws:instance:sessions:{instanceId}` | Set  | 某个应用实例持有的 Session ID |

Session Hash 字段建议如下：

| 字段             | 说明                 |
| ---------------- | -------------------- |
| `sessionId`      | WebSocket Session ID |
| `userId`         | 用户 ID              |
| `instanceId`     | 当前服务实例 ID      |
| `clientType`     | 客户端类型           |
| `deviceId`       | 设备 ID              |
| `ip`             | 客户端 IP            |
| `connectTime`    | 连接时间             |
| `lastActiveTime` | 最后活跃时间         |

推荐操作流程如下：

```text
CONNECT 成功：
1. HSET ws:session:{sessionId} ...
2. EXPIRE ws:session:{sessionId} 180
3. SADD ws:user:sessions:{userId} sessionId
4. SADD ws:instance:sessions:{instanceId} sessionId
5. SADD ws:online:users userId

DISCONNECT：
1. DEL ws:session:{sessionId}
2. SREM ws:user:sessions:{userId} sessionId
3. SREM ws:instance:sessions:{instanceId} sessionId
4. 如果 ws:user:sessions:{userId} 为空，则 SREM ws:online:users userId
```

下面是 Redis Key 常量示例。

文件位置：`src/main/java/io/github/atengk/websocket/constant/WsRedisKeyConstant.java`

```java
package io.github.atengk.websocket.constant;

import cn.hutool.core.util.StrUtil;

/**
 * WebSocket Redis Key 常量
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class WsRedisKeyConstant {

    /**
     * 在线用户集合
     */
    public static final String ONLINE_USERS = "ws:online:users";

    /**
     * 用户 Session 集合 Key 模板
     */
    public static final String USER_SESSIONS = "ws:user:sessions:{}";

    /**
     * Session 信息 Key 模板
     */
    public static final String SESSION_INFO = "ws:session:{}";

    /**
     * 实例 Session 集合 Key 模板
     */
    public static final String INSTANCE_SESSIONS = "ws:instance:sessions:{}";

    private WsRedisKeyConstant() {
    }

    /**
     * 用户 Session 集合 Key
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String userSessions(String userId) {
        return StrUtil.format(USER_SESSIONS, userId);
    }

    /**
     * Session 信息 Key
     *
     * @param sessionId Session ID
     * @return Redis Key
     */
    public static String sessionInfo(String sessionId) {
        return StrUtil.format(SESSION_INFO, sessionId);
    }

    /**
     * 实例 Session 集合 Key
     *
     * @param instanceId 实例 ID
     * @return Redis Key
     */
    public static String instanceSessions(String instanceId) {
        return StrUtil.format(INSTANCE_SESSIONS, instanceId);
    }
}
```

### 群组成员缓存

群组成员缓存用于提升群组订阅鉴权、群组消息发送校验、群组在线成员计算的性能。群成员关系通常来源于数据库，但 WebSocket 的 `SUBSCRIBE` 鉴权属于高频操作，不建议每次都查数据库。

推荐 Redis Key 设计如下：

```text
ws:group:members:{groupId}
ws:user:groups:{userId}
ws:group:online:{groupId}
```

数据结构建议：

| Key                          | 类型 | 说明                   |
| ---------------------------- | ---- | ---------------------- |
| `ws:group:members:{groupId}` | Set  | 群组成员 ID 集合       |
| `ws:user:groups:{userId}`    | Set  | 用户加入的群组 ID 集合 |
| `ws:group:online:{groupId}`  | Set  | 群组在线成员 ID 集合   |

群组鉴权流程如下：

```text
1. 客户端订阅 /topic/group/{groupId}
2. 服务端从 Principal 获取 userId
3. Redis SISMEMBER ws:group:members:{groupId} userId
4. 如果存在，允许订阅
5. 如果不存在，拒绝订阅
6. 如果缓存不存在，回源数据库并重建缓存
```

群组成员缓存更新规则如下：

| 业务动作 | Redis 操作                               |
| -------- | ---------------------------------------- |
| 成员加入 | `SADD ws:group:members:{groupId} userId` |
| 成员退出 | `SREM ws:group:members:{groupId} userId` |
| 群组踢人 | `SREM ws:group:members:{groupId} userId` |
| 群组解散 | `DEL ws:group:members:{groupId}`         |
| 用户上线 | 如果用户属于群组，则更新群组在线集合     |
| 用户下线 | 从相关群组在线集合移除用户               |

群组成员缓存建议设置较长 TTL 或不设置 TTL，由成员变更事件主动更新。若担心缓存不一致，可以设置 1 到 6 小时 TTL，并在查询 miss 时回源数据库。

### 未读数缓存

未读数缓存用于提升通知中心、聊天会话、待办角标等未读数查询性能。未读数属于高频读写数据，不建议每次都直接聚合数据库。

推荐 Redis Key 设计如下：

```text
ws:notice:unread:total:{userId}
ws:chat:unread:total:{userId}
ws:chat:unread:chat:{userId}:{chatId}
ws:todo:unread:total:{userId}
```

数据结构建议：

| Key                                     | 类型   | 说明               |
| --------------------------------------- | ------ | ------------------ |
| `ws:notice:unread:total:{userId}`       | String | 用户通知未读总数   |
| `ws:chat:unread:total:{userId}`         | String | 用户聊天未读总数   |
| `ws:chat:unread:chat:{userId}:{chatId}` | String | 用户某个会话未读数 |
| `ws:todo:unread:total:{userId}`         | String | 用户待办未读数     |

未读数更新策略如下：

| 场景         | 操作                        |
| ------------ | --------------------------- |
| 新通知生成   | 未读数 `INCR`               |
| 新聊天消息   | 会话未读数和总未读数 `INCR` |
| 单条通知已读 | 未读数 `DECR`               |
| 会话全部已读 | 会话未读清零，总未读扣减    |
| 全部通知已读 | 通知未读数清零              |
| 缓存丢失     | 从数据库重新聚合            |

未读数缓存必须考虑数据库一致性。推荐方式是数据库状态更新成功后，再更新 Redis；如果 Redis 更新失败，可以通过定时任务从数据库重建未读数。

未读数推送流程如下：

```text
1. 新通知落库成功
2. 事务提交后更新 Redis 未读数
3. 服务端向 /user/queue/notice 推送未读数变化
4. 用户已读通知
5. 更新数据库状态
6. 更新 Redis 未读数
7. 多端同步未读数变化
```

### 消息去重缓存

消息去重缓存用于防止重复消费、重复推送、重复生成通知、重复 ACK 和重复更新未读数。Redis 的 `SETNX` 非常适合做短期幂等控制。

推荐 Redis Key 设计如下：

```text
ws:dedup:message:{messageId}
ws:dedup:client:{senderId}:{clientMessageId}
ws:dedup:biz:{bizType}:{bizId}:{eventType}:{receiverId}
ws:dedup:ack:{messageId}:{userId}:{ackType}
```

去重缓存 TTL 建议如下：

| 幂等类型      | TTL 建议     |
| ------------- | ------------ |
| 客户端消息 ID | 1 到 24 小时 |
| 普通消息 ID   | 1 到 24 小时 |
| 业务事件通知  | 1 到 7 天    |
| ACK 回执      | 1 到 7 天    |
| 未读数幂等    | 1 到 7 天    |

幂等处理示例：

```text
SET ws:dedup:biz:ORDER:10001:ORDER_PAID:10002 1 NX EX 86400
```

返回成功表示首次处理，返回失败表示重复事件，应跳过生成通知或推送。

消息去重建议如下：

| 场景             | 幂等 Key                                   |
| ---------------- | ------------------------------------------ |
| 聊天重复发送     | `senderId + clientMessageId`               |
| 订单通知重复消费 | `bizType + bizId + eventType + receiverId` |
| ACK 重复上报     | `messageId + userId + ackType`             |
| MQ 重复投递      | MQ messageId 或业务事件 ID                 |
| 任务进度重复推送 | `taskId + status + progress`               |

### 分布式锁

分布式锁用于控制集群下同一资源只被一个实例处理，例如批量通知任务、房间人数刷新、群组解散、离线消息补偿、失败消息重试、缓存重建等。

推荐使用 Redisson 实现分布式锁。基础依赖如下：

```xml
<!-- Redisson：提供 Redis 分布式锁、限流器、延迟队列等高级能力 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.36.0</version>
</dependency>
```

分布式锁 Key 建议如下：

```text
ws:lock:notice:batch:{taskId}
ws:lock:offline:compensate:{userId}
ws:lock:group:disband:{groupId}
ws:lock:failed-message:retry
ws:lock:cache:group-members:{groupId}
```

分布式锁使用示例。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsDistributedLockService.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 分布式锁服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsDistributedLockService {

    private final RedissonClient redissonClient;

    /**
     * 尝试加锁并执行任务
     *
     * @param lockKey   锁 Key
     * @param waitTime  等待时间
     * @param leaseTime 持锁时间
     * @param runnable  任务
     * @return 是否执行成功
     */
    public boolean tryLockAndRun(String lockKey, Duration waitTime, Duration leaseTime, Runnable runnable) {
        if (StrUtil.isBlank(lockKey) || runnable == null) {
            return false;
        }

        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            if (!locked) {
                log.warn("获取 WebSocket 分布式锁失败，lockKey：{}", lockKey);
                return false;
            }

            runnable.run();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取 WebSocket 分布式锁被中断，lockKey：{}", lockKey);
            return false;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

分布式锁注意事项如下：

| 注意项           | 说明                           |
| ---------------- | ------------------------------ |
| 锁粒度要合理     | 不要用全局锁阻塞所有推送       |
| 设置 leaseTime   | 防止实例宕机后锁不释放         |
| 避免锁内耗时过长 | 批量任务应分页处理             |
| 锁失败要可重试   | 定时任务下次继续执行           |
| 不要替代幂等     | 锁只控制并发，幂等仍然必须存在 |

### 发布订阅

Redis Pub/Sub 可用于多实例之间广播事件，例如用户强制下线、在线状态变化、缓存失效、群组成员变更、跨实例推送通知等。它适合实时通知，不适合可靠消息，因为 Redis Pub/Sub 不保存历史消息，订阅者离线期间会丢失消息。

推荐频道设计如下：

```text
ws:pubsub:session:kick
ws:pubsub:online:status
ws:pubsub:group:changed
ws:pubsub:notice:broadcast
ws:pubsub:cache:evict
```

适用场景如下：

| 场景           | 说明                                 |
| -------------- | ------------------------------------ |
| 跨实例强制下线 | A 实例发事件，B 实例关闭本地 Session |
| 在线状态广播   | 多实例同步用户上线下线事件           |
| 群组成员变更   | 通知所有实例刷新成员缓存             |
| 简单广播通知   | 所有实例收到后推送本地连接           |
| 缓存失效       | 通知各实例清理本地缓存               |

Redis Pub/Sub 不适合以下场景：

| 场景           | 原因                         |
| -------------- | ---------------------------- |
| 关键通知持久化 | Pub/Sub 不保存消息           |
| 离线消息补偿   | 订阅者离线会丢消息           |
| 高可靠任务队列 | 无 ACK、无重试、无死信       |
| 大规模事件流   | 更适合 Kafka 或 Redis Stream |

跨实例事件消息建议包含以下字段：

```json
{
  "eventId": "evt-100001",
  "eventType": "KICK_SESSION",
  "targetInstanceId": "ws-node-1",
  "userId": "10001",
  "sessionId": "session-001",
  "timestamp": 1777956000000
}
```

### Stream 消息队列

Redis Stream 可用于轻量级可靠消息队列，支持消息持久化、消费者组、ACK、待处理消息列表和重试。它适合中小规模业务事件、离线消息补偿、失败消息重试、通知分发等场景。

推荐 Stream Key 设计如下：

```text
ws:stream:notice
ws:stream:offline-message
ws:stream:failed-message
ws:stream:online-event
```

Redis Stream 与 Pub/Sub 对比如下：

| 能力         | Pub/Sub | Stream             |
| ------------ | ------- | ------------------ |
| 消息持久化   | 否      | 是                 |
| 消费者组     | 否      | 是                 |
| ACK          | 否      | 是                 |
| 消费失败重试 | 否      | 可实现             |
| 历史消息读取 | 否      | 是                 |
| 实时广播     | 是      | 需要消费者组处理   |
| 适合关键消息 | 不适合  | 可用于中等可靠场景 |

Stream 使用建议如下：

| 场景           | 是否适合            |
| -------------- | ------------------- |
| 通知异步分发   | 适合                |
| 失败消息重试   | 适合                |
| 离线补偿任务   | 适合                |
| 大规模日志流   | 一般，Kafka 更合适  |
| 高可靠金融消息 | 需谨慎，建议专业 MQ |

Stream 消息字段建议：

```text
XADD ws:stream:notice * eventId evt-100001 eventType USER_NOTICE payload {...}
```

消费者组建议：

```text
XGROUP CREATE ws:stream:notice ws-notice-consumer-group 0 MKSTREAM
```

Redis Stream 可作为轻量消息队列，但如果系统已有 RabbitMQ、Kafka 或 RocketMQ，建议优先复用现有消息体系。

### 缓存过期策略

缓存过期策略用于防止 Redis 中的 Session、离线状态、幂等键、未读数、临时房间等数据长期残留。不同类型的缓存需要不同 TTL，不能一刀切。

推荐 TTL 策略如下：

| 缓存类型                    | TTL 建议          | 说明                |
| --------------------------- | ----------------- | ------------------- |
| `ws:session:{sessionId}`    | 2 到 5 分钟       | 需随心跳续期        |
| `ws:user:sessions:{userId}` | 可不设或动态清理  | 依赖 Session 清理   |
| `ws:online:users`           | 不设 TTL          | 由 Session 状态维护 |
| 群组成员缓存                | 1 到 6 小时或不设 | 成员变更主动失效    |
| 群组在线缓存                | 2 到 5 分钟       | 随连接状态更新      |
| 未读数缓存                  | 1 到 30 天        | 可从数据库重建      |
| 幂等缓存                    | 1 小时到 7 天     | 按业务重要性设置    |
| 房间 Session                | 2 到 5 分钟       | 随心跳续期          |
| 临时验证码类消息            | 几分钟            | 业务短有效期        |

Session TTL 必须大于心跳间隔。若心跳间隔为 10 秒，Session TTL 可以设置为 120 秒或 180 秒，并在每次 `SEND`、`SUBSCRIBE`、心跳或活跃事件中刷新。

缓存过期注意事项如下：

| 注意项                  | 说明                                   |
| ----------------------- | -------------------------------------- |
| 不给核心集合随意 TTL    | 在线用户集合应由业务清理，不靠整体过期 |
| Session Hash 必须有 TTL | 防止实例宕机后残留                     |
| 幂等 Key 必须有 TTL     | 防止 Redis 无限制增长                  |
| 未读数可重建            | 缓存丢失时从数据库聚合                 |
| 群组成员变更主动失效    | 避免长 TTL 导致权限滞后                |

### 缓存一致性

缓存一致性用于保证 Redis 中的在线状态、群组成员、未读数和数据库最终状态一致。WebSocket 系统中缓存读写频繁，如果没有一致性策略，容易出现用户明明离线但仍显示在线、用户已退出群但仍能订阅、未读数不准确等问题。

常见一致性问题如下：

| 问题           | 原因                                          |
| -------------- | --------------------------------------------- |
| 用户假在线     | 实例宕机，Session 未清理                      |
| 用户误离线     | 多端场景下某个 Session 断开后直接移除在线用户 |
| 群成员权限滞后 | 群成员缓存未及时失效                          |
| 未读数不准     | 数据库更新成功但 Redis 更新失败               |
| 重复通知       | 幂等缓存失效或业务唯一约束缺失                |
| 房间人数不准   | 连接异常断开未清理房间 Session                |

推荐一致性策略如下：

| 数据类型     | 一致性策略                             |
| ------------ | -------------------------------------- |
| 在线状态     | Redis TTL + 断开事件清理 + 定时扫描    |
| Session 映射 | Hash 设置 TTL，活跃时续期              |
| 群组成员     | 数据库为准，Redis 缓存，变更时主动删除 |
| 未读数       | 数据库为准，Redis 加速，异常时重建     |
| 幂等数据     | Redis 快速拦截 + 数据库唯一约束兜底    |
| 房间人数     | Redis Set 统计 + 定时校正              |

缓存一致性处理建议如下：

```text
写操作：
1. 更新数据库
2. 事务提交成功
3. 删除或更新 Redis 缓存
4. 发布缓存失效事件
5. WebSocket 推送变更消息

读操作：
1. 先查 Redis
2. Redis 命中直接返回
3. Redis 未命中查数据库
4. 回写 Redis 并设置 TTL
```

对于权限相关缓存，例如群组成员、管理端角色、租户权限，宁可短时间多查数据库，也不能长期错误授权。权限缓存变更后应主动失效。

## 外部消息代理集成

外部消息代理集成用于提升 WebSocket STOMP 在集群部署、高并发订阅、跨节点消息分发和可靠事件处理中的能力。Spring 内置 Simple Broker 适合单体和轻量场景；当系统需要多实例统一分发、专业 Broker 管理订阅关系、业务服务解耦或高吞吐事件流时，应引入 RabbitMQ、ActiveMQ、Kafka、Redis Pub/Sub 或 Redis Stream 等外部组件。

### Simple Broker 使用场景

Simple Broker 是 Spring 内置的简单消息代理，适合开发环境、单体部署、中小规模系统和轻量实时通知。它不需要外部依赖，配置简单，可以快速实现 `/topic/**` 广播和 `/queue/**` 用户队列。

Simple Broker 配置如下：

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(new long[]{10000, 10000})
            .setTaskScheduler(webSocketHeartbeatTaskScheduler());

    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
}
```

Simple Broker 适用场景如下：

| 场景           | 是否适合 |
| -------------- | -------- |
| 本地开发       | 适合     |
| 单体应用       | 适合     |
| 内部管理后台   | 适合     |
| 小规模通知     | 适合     |
| 单实例聊天     | 可以     |
| 多实例统一广播 | 不适合   |
| 高可靠消息     | 不适合   |
| 大规模订阅     | 不建议   |

Simple Broker 的边界如下：

| 限制                  | 说明                             |
| --------------------- | -------------------------------- |
| 订阅关系在应用内存中  | 实例间不共享                     |
| 不提供持久化          | 应用重启后订阅关系丢失           |
| 不提供专业 ACK 和死信 | 可靠性需要业务层实现             |
| 集群广播需额外处理    | 需要 Redis Pub/Sub 或外部 Broker |
| 扩展能力有限          | 不适合承担复杂消息系统职责       |

如果项目从单实例开始，建议先使用 Simple Broker，并在架构上预留 Broker Relay 和 Redis 状态同步能力。

### RabbitMQ STOMP Broker Relay

RabbitMQ STOMP Broker Relay 用于让 Spring WebSocket 实例将 STOMP 消息转发给 RabbitMQ，由 RabbitMQ 负责订阅和消息分发。它适合多实例部署、跨节点消息路由、生产集群和较大规模的实时消息系统。

RabbitMQ 需要启用 STOMP 插件：

```bash
rabbitmq-plugins enable rabbitmq_stomp
rabbitmq-plugins enable rabbitmq_web_stomp
```

如果使用 Docker，可以通过以下方式启动 RabbitMQ。

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -p 61613:61613 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin123 \
  rabbitmq:3.13-management
```

命令说明：`5672` 是 AMQP 端口，`15672` 是管理控制台端口，`61613` 是 RabbitMQ STOMP 默认端口。生产环境需要替换默认账号密码，并启用持久化存储和高可用策略。

Spring 配置示例：

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableStompBrokerRelay("/topic", "/queue")
            .setRelayHost("127.0.0.1")
            .setRelayPort(61613)
            .setClientLogin("admin")
            .setClientPasscode("admin123")
            .setSystemLogin("admin")
            .setSystemPasscode("admin123")
            .setSystemHeartbeatSendInterval(10000)
            .setSystemHeartbeatReceiveInterval(10000);

    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
}
```

RabbitMQ Broker Relay 适用场景如下：

| 场景                  | 说明                         |
| --------------------- | ---------------------------- |
| 多实例 WebSocket 服务 | 多个实例共享 Broker 分发能力 |
| 生产集群              | Broker 统一处理订阅和路由    |
| 广播消息较多          | Broker 分担应用分发压力      |
| 点对点消息            | 适合用户私有队列             |
| 与 RabbitMQ 生态集成  | 复用现有 RabbitMQ 运维体系   |

RabbitMQ Relay 注意事项如下：

| 注意项                 | 说明                         |
| ---------------------- | ---------------------------- |
| 需要启用 STOMP 插件    | 默认 RabbitMQ 不一定启用     |
| 端口通常为 61613       | 需开放网络访问               |
| 用户权限要隔离         | 不建议使用默认 guest         |
| Broker 故障会影响推送  | 需要监控和高可用             |
| 消息可靠性仍要业务设计 | Relay 不等于业务消息自动可靠 |

### ActiveMQ Broker Relay

ActiveMQ Broker Relay 与 RabbitMQ 类似，用于让 Spring WebSocket 将 STOMP 消息转发给 ActiveMQ。ActiveMQ 原生支持 STOMP，适合企业已有 ActiveMQ 体系、JMS 生态或传统消息中间件环境。

配置示例：

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableStompBrokerRelay("/topic", "/queue")
            .setRelayHost("127.0.0.1")
            .setRelayPort(61613)
            .setClientLogin("admin")
            .setClientPasscode("admin")
            .setSystemLogin("admin")
            .setSystemPasscode("admin")
            .setSystemHeartbeatSendInterval(10000)
            .setSystemHeartbeatReceiveInterval(10000);

    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
}
```

ActiveMQ Broker Relay 适用场景如下：

| 场景              | 说明                       |
| ----------------- | -------------------------- |
| 企业已有 ActiveMQ | 可复用现有 Broker          |
| JMS 生态系统      | 便于和传统消息系统集成     |
| STOMP 客户端较多  | ActiveMQ 对 STOMP 支持成熟 |
| 多实例 WebSocket  | 支持跨实例分发             |

ActiveMQ 注意事项如下：

| 注意项                    | 说明                     |
| ------------------------- | ------------------------ |
| Broker 性能需压测         | 不同消息模式性能差异明显 |
| Topic 和 Queue 语义要统一 | 避免路径约定混乱         |
| 高可用需单独配置          | 生产需要主从或集群方案   |
| 监控 Broker 积压          | 避免慢消费者导致堆积     |
| 认证权限要配置            | 防止未授权 STOMP 接入    |

### Kafka 事件桥接

Kafka 不适合作为浏览器 STOMP Broker，也不直接处理 WebSocket 客户端订阅关系。Kafka 更适合作为业务事件总线，业务服务将订单、审批、任务、告警等事件写入 Kafka，WebSocket 服务消费事件后再通过 STOMP 推送给客户端。

推荐架构如下：

```text
订单服务 / 审批服务 / 任务服务
    |
    | Kafka Producer
    v
Kafka Topic
    |
    | Kafka Consumer
    v
WebSocket 推送服务
    |
    | SimpMessagingTemplate
    v
WebSocket STOMP 客户端
```

Kafka 事件 Topic 建议如下：

```text
biz-order-event
biz-approval-event
biz-task-event
biz-alarm-event
ws-notice-event
```

Kafka 事件消息示例：

```json
{
  "eventId": "evt-100001",
  "eventType": "ORDER_STATUS_CHANGED",
  "bizType": "ORDER",
  "bizId": "order-100001",
  "receiverType": "USER",
  "receiverIds": ["10001"],
  "title": "订单状态变更",
  "content": "订单已完成支付",
  "timestamp": 1777956000000
}
```

Kafka 桥接适用场景如下：

| 场景               | 说明                         |
| ------------------ | ---------------------------- |
| 微服务业务事件解耦 | 业务服务不直接依赖 WebSocket |
| 高吞吐事件流       | Kafka 适合大量业务事件       |
| 事件可回放         | 消费失败后可以重新消费       |
| 审计和分析         | 事件流可进入数仓或日志平台   |
| 多消费者           | 通知、审计、报表可同时消费   |

Kafka 桥接注意事项如下：

| 注意项           | 说明                                    |
| ---------------- | --------------------------------------- |
| 不直接连接浏览器 | Kafka 不是浏览器 WebSocket Broker       |
| 消费端要幂等     | Kafka 可能重复投递                      |
| 推送失败要记录   | Kafka 消费成功不代表 WebSocket 推送成功 |
| 事件粒度要稳定   | 避免业务事件格式频繁变化                |
| 消费顺序要设计   | 同一业务对象可按 key 分区保证局部有序   |

### Redis Pub/Sub 桥接

Redis Pub/Sub 桥接用于 WebSocket 多实例之间同步轻量事件，例如广播通知、踢人事件、缓存失效、在线状态变化。它实现简单、延迟低，但不提供持久化和 ACK，不适合关键消息。

推荐架构如下：

```text
业务实例 A
    |
    | PUBLISH ws:pubsub:notice:broadcast
    v
Redis Pub/Sub
    |
    +--> WebSocket 实例 A 本地推送
    +--> WebSocket 实例 B 本地推送
    +--> WebSocket 实例 C 本地推送
```

适用场景如下：

| 场景             | 是否适合                      |
| ---------------- | ----------------------------- |
| 跨实例广播通知   | 适合                          |
| 强制下线事件     | 适合，但需结合 Session 注册表 |
| 缓存失效通知     | 适合                          |
| 在线状态同步     | 适合                          |
| 关键通知可靠投递 | 不适合                        |
| 离线消息补偿     | 不适合                        |

Redis Pub/Sub 消息示例：

```json
{
  "eventId": "evt-100001",
  "eventType": "BROADCAST_NOTICE",
  "destination": "/topic/system/notice",
  "payload": {
    "title": "系统维护通知",
    "content": "系统将在今晚 23:00 维护"
  }
}
```

Redis Pub/Sub 桥接建议如下：

| 建议           | 说明                                 |
| -------------- | ------------------------------------ |
| 消息体保持轻量 | 不发送过大 payload                   |
| 关键数据先落库 | Pub/Sub 只做通知                     |
| 接收端要容错   | 解析失败不能影响订阅线程             |
| 避免循环广播   | 消息中带 eventId 或 sourceInstanceId |
| 配合本地推送   | 每个实例只推送自己持有的连接         |

### 消息代理连接配置

消息代理连接配置用于集中管理外部 Broker 的主机、端口、账号、密码、心跳和虚拟主机等参数。生产环境不建议把连接信息硬编码在 Java 配置中，应放入 `application.yml` 或配置中心。

配置示例：

```yaml
app:
  websocket:
    broker:
      # broker 类型：simple、rabbitmq、activemq
      type: rabbitmq

      # STOMP Broker Relay 地址
      relay-host: 127.0.0.1

      # STOMP Broker Relay 端口，RabbitMQ/ActiveMQ 常用 61613
      relay-port: 61613

      # 客户端连接 Broker 使用的账号
      client-login: admin

      # 客户端连接 Broker 使用的密码
      client-passcode: admin123

      # 系统连接 Broker 使用的账号
      system-login: admin

      # 系统连接 Broker 使用的密码
      system-passcode: admin123

      # 服务端向 Broker 发送心跳间隔，单位毫秒
      system-heartbeat-send-interval: 10000

      # 服务端接收 Broker 心跳间隔，单位毫秒
      system-heartbeat-receive-interval: 10000
```

Broker 配置属性类如下。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsBrokerProperties.java`

```java
package io.github.atengk.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 消息代理配置属性
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@ConfigurationProperties(prefix = "app.websocket.broker")
public class WsBrokerProperties {

    /**
     * Broker 类型：simple、rabbitmq、activemq
     */
    private String type = "simple";

    /**
     * Relay 主机
     */
    private String relayHost = "127.0.0.1";

    /**
     * Relay 端口
     */
    private Integer relayPort = 61613;

    /**
     * 客户端登录账号
     */
    private String clientLogin;

    /**
     * 客户端登录密码
     */
    private String clientPasscode;

    /**
     * 系统登录账号
     */
    private String systemLogin;

    /**
     * 系统登录密码
     */
    private String systemPasscode;

    /**
     * 系统心跳发送间隔
     */
    private Long systemHeartbeatSendInterval = 10000L;

    /**
     * 系统心跳接收间隔
     */
    private Long systemHeartbeatReceiveInterval = 10000L;
}
```

配置建议如下：

| 环境       | 建议                                     |
| ---------- | ---------------------------------------- |
| 本地开发   | 使用 Simple Broker                       |
| 测试环境   | 可使用 RabbitMQ Relay 验证集群           |
| 生产单实例 | Simple Broker 或 RabbitMQ Relay          |
| 生产多实例 | RabbitMQ、ActiveMQ 或 Redis/Kafka 桥接   |
| 高可靠事件 | Kafka/RabbitMQ 业务事件 + WebSocket 推送 |

### Broker Relay 心跳配置

Broker Relay 心跳用于保持 Spring WebSocket 服务与外部 STOMP Broker 的连接活性。它不同于客户端与服务端之间的 STOMP 心跳，Broker Relay 心跳发生在服务端应用和 Broker 之间。

配置示例：

```java
registry.enableStompBrokerRelay("/topic", "/queue")
        .setRelayHost(brokerProperties.getRelayHost())
        .setRelayPort(brokerProperties.getRelayPort())
        .setClientLogin(brokerProperties.getClientLogin())
        .setClientPasscode(brokerProperties.getClientPasscode())
        .setSystemLogin(brokerProperties.getSystemLogin())
        .setSystemPasscode(brokerProperties.getSystemPasscode())
        .setSystemHeartbeatSendInterval(brokerProperties.getSystemHeartbeatSendInterval())
        .setSystemHeartbeatReceiveInterval(brokerProperties.getSystemHeartbeatReceiveInterval());
```

心跳配置建议如下：

| 配置项                       | 建议值                 |
| ---------------------------- | ---------------------- |
| 客户端与 WebSocket 服务心跳  | 10 到 30 秒            |
| WebSocket 服务与 Broker 心跳 | 10 到 30 秒            |
| 网关读超时                   | 大于心跳间隔 2 到 3 倍 |
| Broker 连接超时              | 按 Broker 运维规范配置 |

Broker Relay 心跳异常时，通常会导致消息无法分发或订阅异常。生产环境应监控 Broker 连接状态、心跳失败次数、重连次数和消息发送失败率。

### Broker Relay 认证配置

Broker Relay 认证用于控制 Spring WebSocket 服务连接外部 Broker 的账号权限。配置中通常包含 `clientLogin`、`clientPasscode`、`systemLogin`、`systemPasscode`。

含义如下：

| 配置项           | 说明                               |
| ---------------- | ---------------------------------- |
| `clientLogin`    | 代理客户端连接 Broker 时使用的账号 |
| `clientPasscode` | 代理客户端连接 Broker 时使用的密码 |
| `systemLogin`    | Spring 系统连接 Broker 使用的账号  |
| `systemPasscode` | Spring 系统连接 Broker 使用的密码  |

认证配置建议如下：

| 建议           | 说明                                        |
| -------------- | ------------------------------------------- |
| 不使用默认账号 | 生产环境禁止使用 guest/guest 或 admin/admin |
| 最小权限       | Broker 账号只授权需要的 destination         |
| 密码走配置中心 | 不硬编码到代码仓库                          |
| 定期轮换       | 定期更新 Broker 密码                        |
| 区分环境       | dev、test、prod 使用不同账号                |
| 开启 TLS       | 跨网络访问 Broker 时建议启用加密            |

生产环境配置示例：

```yaml
app:
  websocket:
    broker:
      type: rabbitmq
      relay-host: rabbitmq.prod.internal
      relay-port: 61613
      client-login: ${WS_BROKER_CLIENT_LOGIN}
      client-passcode: ${WS_BROKER_CLIENT_PASSCODE}
      system-login: ${WS_BROKER_SYSTEM_LOGIN}
      system-passcode: ${WS_BROKER_SYSTEM_PASSCODE}
```

### Broker 故障处理

Broker 故障处理用于应对 RabbitMQ、ActiveMQ 或其他外部消息代理不可用、连接断开、认证失败、队列积压、慢消费者、磁盘满、内存告警等问题。

常见故障如下：

| 故障            | 表现                                    |
| --------------- | --------------------------------------- |
| Broker 不可连接 | WebSocket 服务启动失败或 Relay 连接失败 |
| Broker 认证失败 | STOMP CONNECT ERROR                     |
| Broker 心跳失败 | 推送中断、连接重连                      |
| 队列积压        | 消息延迟升高                            |
| 慢消费者        | 出站通道阻塞                            |
| Broker 内存不足 | 消息发送失败                            |
| Broker 集群分区 | 部分消息路由异常                        |

故障处理策略如下：

| 策略       | 说明                               |
| ---------- | ---------------------------------- |
| 快速告警   | Broker 连接失败立即告警            |
| 失败记录   | 推送失败写入失败消息表             |
| 降级处理   | 非关键实时推送可临时关闭           |
| 补偿查询   | 客户端重连后通过 HTTP 查询最终状态 |
| 重试机制   | 可重试消息进入重试任务             |
| 限流保护   | Broker 异常时限制批量推送          |
| 管理端提示 | 显示实时推送通道异常               |

Broker 故障时，不能让业务主流程完全依赖 WebSocket 推送。关键业务应先落库，推送失败后通过补偿和重试恢复。

故障处理流程建议：

```text
1. 业务消息落库成功
2. 尝试 WebSocket 推送
3. Broker 异常导致推送失败
4. 写入失败消息表
5. 触发告警
6. 客户端可通过 HTTP 查询最终状态
7. Broker 恢复后重试失败消息
8. 超过最大重试次数后进入死信
```

### Broker 高可用设计

Broker 高可用设计用于保证外部消息代理故障时，WebSocket 推送能力尽量不出现单点问题。不同 Broker 的高可用方案不同，但总体目标是避免单节点 Broker 故障导致全部实时消息中断。

RabbitMQ 高可用建议如下：

| 方案         | 说明                              |
| ------------ | --------------------------------- |
| 集群部署     | 多节点 RabbitMQ 集群              |
| Quorum Queue | 关键队列使用 Quorum Queue         |
| 镜像或策略   | 根据版本使用合适的高可用队列策略  |
| 持久化存储   | 关键消息持久化                    |
| 监控告警     | 监控内存、磁盘、连接、队列积压    |
| 客户端重连   | Spring 服务应支持 Broker 断开重连 |

ActiveMQ 高可用建议如下：

| 方案               | 说明                     |
| ------------------ | ------------------------ |
| 主从部署           | 使用主备 Broker          |
| 共享存储           | 使用共享存储保证切换     |
| Network of Brokers | 适合复杂拓扑             |
| 持久化配置         | 关键消息启用持久化       |
| 连接故障转移       | 客户端配置 failover 地址 |

Redis 高可用建议如下：

| 方案           | 说明                         |
| -------------- | ---------------------------- |
| Redis Sentinel | 主从切换                     |
| Redis Cluster  | 分片与高可用                 |
| 合理 TTL       | 防止状态残留                 |
| 数据持久化     | 根据重要性开启 AOF/RDB       |
| 降级策略       | Redis 异常时本地内存临时兜底 |

Kafka 高可用建议如下：

| 方案           | 说明                     |
| -------------- | ------------------------ |
| 多 Broker 集群 | 避免单 Broker            |
| 多副本         | Topic 设置合理副本数     |
| ISR 监控       | 监控同步副本             |
| 消费者组       | WebSocket 服务多实例消费 |
| 幂等消费       | 防止重复事件导致重复推送 |

Broker 高可用设计建议如下：

| 建议                   | 说明                                       |
| ---------------------- | ------------------------------------------ |
| WebSocket 服务无状态化 | Session 状态放 Redis，便于扩容             |
| Broker 多节点          | 避免单点故障                               |
| 业务消息先落库         | Broker 故障不影响最终状态                  |
| 推送失败可补偿         | 通过失败表、离线表、HTTP 查询恢复          |
| 监控必须完善           | 连接数、心跳、积压、失败率都要监控         |
| 压测后定容量           | 不同 Broker 对长连接和消息分发能力差异明显 |
| 明确降级策略           | Broker 故障时关闭非关键推送，保留核心通知  |


## 集群与分布式支持

集群与分布式支持用于解决 WebSocket STOMP 在多实例部署下的连接状态、消息路由、广播同步、点对点投递、群组消息、在线状态、强制下线、限流、锁、负载均衡和网关转发问题。WebSocket 是有状态长连接，客户端连接建立后会固定挂在某个服务实例上，因此多实例部署不能只把应用复制多份，还必须设计跨节点状态同步和消息分发机制。

### 多实例 Session 问题

多实例 Session 问题是 WebSocket 集群部署的核心问题。普通 HTTP 请求可以通过负载均衡分发到任意实例，但 WebSocket 连接建立后会长期保持在某个实例上，该实例只知道自己持有的连接，其他实例默认无法感知这些 Session。

典型问题如下：

```text
用户 10001 连接到实例 A
用户 10002 连接到实例 B

订单服务调用实例 C 推送消息给用户 10001
实例 C 本地没有用户 10001 的 Session
如果没有分布式路由或外部 Broker，消息无法送达
```

多实例下需要解决以下状态：

| 状态           | 单实例处理 | 多实例问题                    | 推荐方案                           |
| -------------- | ---------- | ----------------------------- | ---------------------------------- |
| Session 信息   | 内存 Map   | 其他实例不可见                | Redis 保存 Session 映射            |
| 在线用户       | 本地集合   | 在线人数不完整                | Redis Set 汇总                     |
| 用户点对点消息 | 本地投递   | 目标用户可能在其他实例        | Broker Relay 或 Redis Pub/Sub 转发 |
| 广播消息       | 本地广播   | 只能覆盖本实例连接            | 外部 Broker 或跨实例广播           |
| 群组在线成员   | 本地统计   | 群成员分布在多个实例          | Redis 统一统计                     |
| 强制下线       | 本地关闭   | 目标 Session 可能不在当前实例 | Redis Pub/Sub 下线事件             |

多实例部署下，内存 Session 注册表只能作为“本实例连接缓存”，不能作为全局在线状态来源。建议采用以下分层：

| 层级     | 存储内容                              | 说明                   |
| -------- | ------------------------------------- | ---------------------- |
| 本地内存 | 当前实例持有的 WebSocket Session      | 用于本地快速推送和清理 |
| Redis    | 全局 Session 映射、在线用户、实例归属 | 用于跨实例查询         |
| 数据库   | 会话日志、用户最近在线状态            | 用于审计和历史查询     |
| Broker   | 订阅关系和消息分发                    | 用于跨节点推送         |

推荐 Redis Key：

```text
ws:online:users
ws:user:sessions:{userId}
ws:session:{sessionId}
ws:instance:sessions:{instanceId}
```

Session 信息中应保存 `instanceId`，用于判断目标连接在哪个应用实例上。

```json
{
  "sessionId": "session-001",
  "userId": "10001",
  "instanceId": "ws-node-01",
  "clientType": "WEB",
  "deviceId": "browser-001",
  "ip": "192.168.1.10",
  "connectTime": "2026-05-05T10:00:00",
  "lastActiveTime": "2026-05-05T10:01:00"
}
```

### 用户路由问题

用户路由问题指服务端需要向指定用户推送消息时，必须知道该用户当前连接在哪些实例、哪些 Session 上。单实例中可以直接使用本地 `SimpMessagingTemplate.convertAndSendToUser`，但集群下目标用户可能不在当前实例。

常见用户路由方案如下：

| 方案                   | 说明                                           | 适用场景                             |
| ---------------------- | ---------------------------------------------- | ------------------------------------ |
| Broker Relay           | 通过 RabbitMQ/ActiveMQ 统一处理用户目的地      | 推荐生产集群                         |
| Redis Pub/Sub 转发     | 当前实例发现目标用户在其他实例，发布跨实例事件 | 中小规模集群                         |
| 业务事件广播到所有实例 | 所有实例收到事件后只推送本地连接               | 简单可靠，消息量可控时适用           |
| Sticky Session         | 同一客户端固定到一个实例                       | 只能解决连接稳定，不能解决跨实例推送 |

推荐用户路由流程：

```text
1. 业务服务产生用户消息
2. 当前 WebSocket 实例查询 Redis：ws:user:sessions:{userId}
3. 获取目标用户所有 sessionId
4. 根据 ws:session:{sessionId} 查询 instanceId
5. 如果 instanceId 是当前实例，直接本地推送
6. 如果 instanceId 是其他实例，发布 Redis Pub/Sub 路由事件
7. 目标实例收到事件后执行本地推送
```

跨实例用户消息事件示例：

```json
{
  "eventId": "evt-100001",
  "eventType": "USER_MESSAGE",
  "targetUserId": "10001",
  "targetSessionId": null,
  "destination": "/queue/notice",
  "payload": {
    "messageId": "1909550919275921901",
    "type": "USER_NOTICE",
    "data": {
      "title": "审批通知",
      "content": "你有一条新的审批待办"
    }
  },
  "sourceInstanceId": "ws-node-02",
  "targetInstanceId": "ws-node-01"
}
```

用户路由注意事项如下：

| 注意项                    | 说明                               |
| ------------------------- | ---------------------------------- |
| 同一用户可能多端在线      | 推送时要遍历用户所有 Session       |
| Session 可能已过期        | 推送前应校验 Session 是否仍有效    |
| 跨实例事件要幂等          | Pub/Sub 可能重复处理或业务重复发布 |
| 不要只依赖 Sticky Session | 后端服务推送不一定进入用户所在实例 |
| 关键消息必须持久化        | 跨实例路由失败后需要补偿           |

### 广播消息同步

广播消息同步用于保证多实例下所有连接都能收到同一条广播消息。单实例中向 `/topic/system/notice` 推送即可，但多实例下如果使用 Simple Broker，每个实例只会推送给本实例的订阅客户端。

广播同步方案如下：

| 方案                           | 说明                               | 特点               |
| ------------------------------ | ---------------------------------- | ------------------ |
| RabbitMQ/ActiveMQ Broker Relay | 由外部 Broker 统一分发广播         | 推荐生产集群       |
| Redis Pub/Sub                  | 各实例订阅广播频道，收到后本地推送 | 实现简单           |
| Kafka 业务事件                 | WebSocket 服务消费业务事件后推送   | 适合事件驱动架构   |
| 定时轮询数据库                 | 各实例扫描公告或通知表             | 实时性较差，可兜底 |

Redis Pub/Sub 广播流程：

```text
1. 管理端发布系统公告
2. 服务端写入公告表
3. 发布 Redis 事件 ws:pubsub:broadcast
4. 所有 WebSocket 实例收到广播事件
5. 每个实例向本地订阅 /topic/system/notice 的客户端推送
```

广播事件结构示例：

```json
{
  "eventId": "broadcast-100001",
  "eventType": "SYSTEM_NOTICE",
  "destination": "/topic/system/notice",
  "payload": {
    "messageId": "1909550919275921902",
    "type": "SYSTEM_NOTICE",
    "data": {
      "title": "系统维护通知",
      "content": "系统将在今晚 23:00 维护"
    }
  },
  "sourceInstanceId": "ws-node-01",
  "timestamp": 1777956000000
}
```

广播同步注意事项如下：

| 注意项         | 说明                                       |
| -------------- | ------------------------------------------ |
| 防止重复广播   | 使用 `eventId` 做幂等                      |
| 本实例也要处理 | 发布事件的实例也应本地推送或消费自己的事件 |
| 控制广播范围   | 全站广播慎用，优先按租户、角色、群组拆分   |
| 关键广播先落库 | Pub/Sub 不可靠，公告等应保存数据库         |
| 大量广播需限流 | 避免瞬时压垮出站通道和前端页面             |

### 点对点消息路由

点对点消息路由用于向指定用户、指定设备或指定 Session 推送消息。它比广播更复杂，因为服务端需要准确定位目标连接。

点对点路由目标可以分为三类：

| 路由目标 | 示例                    | 说明                   |
| -------- | ----------------------- | ---------------------- |
| 用户     | `userId=10001`          | 推送给该用户全部在线端 |
| Session  | `sessionId=session-001` | 只推送给某个连接       |
| 设备     | `deviceId=iphone-001`   | 推送给某台设备对应连接 |

点对点路由流程：

```text
1. 接收点对点推送请求
2. 解析目标 userId、sessionId 或 deviceId
3. 查询 Redis 全局 Session 映射
4. 按 instanceId 分组
5. 当前实例本地推送
6. 其他实例通过 Pub/Sub 或 Broker Relay 转发
7. 推送失败则记录失败消息或离线消息
```

按实例分组后的路由示例：

```text
userId=10001
    session-001 -> ws-node-01
    session-002 -> ws-node-02
    session-003 -> ws-node-02

当前实例 ws-node-01：
    本地推送 session-001
    发布事件给 ws-node-02 推送 session-002、session-003
```

点对点消息建议如下：

| 场景         | 推荐处理                            |
| ------------ | ----------------------------------- |
| 普通个人通知 | 推送用户全部在线端                  |
| 当前操作结果 | 只推送当前 Session                  |
| 单聊消息     | 接收方全部在线端 + 发送方其他端同步 |
| 强制设备下线 | 指定 Session 或 deviceId            |
| 用户离线     | 写入离线消息或依赖历史补偿          |
| 推送失败     | 记录失败表，按策略重试              |

使用 Broker Relay 时，Spring 的用户目的地能力会更适合多实例场景；使用 Simple Broker 时，需要自行维护 Redis 路由和跨实例事件。

### 群组消息同步

群组消息同步用于保证群组、房间、项目空间中的成员即使连接在不同实例上，也能收到同一条群消息。群组消息通常发送到 `/topic/group/{groupId}`，但 Simple Broker 下只能覆盖当前实例订阅者。

群组同步方案如下：

| 方案           | 说明                                             |
| -------------- | ------------------------------------------------ |
| Broker Relay   | 群组 Topic 由 RabbitMQ/ActiveMQ 分发             |
| Redis Pub/Sub  | 发布群组消息事件，各实例本地推送                 |
| Kafka 事件桥接 | 群消息落库后发送 Kafka，各实例消费或指定服务消费 |
| 按成员点对点   | 解析群成员后逐个用户推送，不适合大群             |

Redis Pub/Sub 群组消息流程：

```text
1. 用户发送群消息到 /app/chat/group/send
2. 服务端校验群成员和禁言状态
3. 消息落库
4. 事务提交后发布 ws:pubsub:group-message
5. 所有实例收到事件
6. 每个实例向本地订阅 /topic/group/{groupId} 的客户端推送
```

群组消息事件示例：

```json
{
  "eventId": "group-msg-100001",
  "eventType": "GROUP_MESSAGE",
  "groupId": "90001",
  "destination": "/topic/group/90001",
  "payload": {
    "messageId": "1909550919275921903",
    "type": "GROUP_CHAT",
    "data": {
      "groupId": "90001",
      "senderId": "10001",
      "content": "大家好",
      "sequence": 30001
    }
  },
  "sourceInstanceId": "ws-node-01"
}
```

群组消息同步注意事项如下：

| 注意项                | 说明                       |
| --------------------- | -------------------------- |
| 群消息必须落库        | 支持历史查询和断线补偿     |
| 广播前做权限校验      | 只有群成员可以发送         |
| 订阅阶段也要鉴权      | 防止非成员订阅群 Topic     |
| 大群避免逐人推送      | 优先使用 Topic 或 Broker   |
| 前端按 messageId 去重 | 重连补偿和实时推送可能重复 |
| 按 sequence 排序      | 避免跨实例异步导致乱序     |

### 分布式在线状态

分布式在线状态用于在多实例下准确判断用户是否在线、某个群组有哪些在线成员、管理端在线人数是多少。在线状态不能依赖单个实例内存，需要 Redis 汇总并结合 TTL、心跳、断开事件和实例清理机制。

推荐 Redis 结构：

```text
ws:online:users
ws:user:sessions:{userId}
ws:session:{sessionId}
ws:instance:sessions:{instanceId}
ws:group:online:{groupId}
ws:room:sessions:{roomId}
```

分布式在线状态更新流程：

```text
CONNECT：
1. 写 ws:session:{sessionId}
2. 加入 ws:user:sessions:{userId}
3. 加入 ws:instance:sessions:{instanceId}
4. 加入 ws:online:users
5. 根据用户群组关系更新 ws:group:online:{groupId}

DISCONNECT：
1. 删除 ws:session:{sessionId}
2. 移除 ws:user:sessions:{userId}
3. 移除 ws:instance:sessions:{instanceId}
4. 如果用户无其他 Session，从 ws:online:users 移除
5. 从群组在线集合移除
```

实例异常宕机时，需要清理该实例残留状态。推荐方案如下：

| 方案                | 说明                                          |
| ------------------- | --------------------------------------------- |
| Session TTL         | `ws:session:{sessionId}` 设置过期时间         |
| 实例心跳            | `ws:instance:heartbeat:{instanceId}` 定期续期 |
| 宕机扫描            | 发现实例心跳过期后清理其 Session              |
| Redis Keyspace 事件 | 可选，监听 Session 过期                       |
| 服务启动自清理      | 实例启动时清理自己旧的 instanceId 数据        |

分布式在线状态注意事项如下：

| 注意项                    | 说明                           |
| ------------------------- | ------------------------------ |
| 用户在线要看 Session 集合 | 不要只看单个连接               |
| TTL 大于心跳间隔          | 避免正常连接被误清理           |
| 下线逻辑必须幂等          | 重复清理不能报错               |
| 群组在线按用户去重        | 同一用户多端在线只算一个成员   |
| 管理端看板可定时聚合      | 不必每次连接变化都实时全量刷新 |

### 分布式踢人

分布式踢人用于在任意实例接收到踢人请求时，都能让目标用户或目标 Session 所在实例执行下线动作。由于目标 Session 可能不在当前实例，必须通过 Redis、Broker 或消息队列发送跨实例控制事件。

踢人类型如下：

| 类型             | 说明                           |
| ---------------- | ------------------------------ |
| 踢出指定 Session | 只让某个连接下线               |
| 踢出指定设备     | 根据 deviceId 找到相关 Session |
| 踢出用户全部连接 | 清理该用户所有 Session         |
| 同端互踢         | 新 Web 连接踢出旧 Web 连接     |
| 管理端强制下线   | 管理员操作触发                 |

分布式踢人流程：

```text
1. 管理端请求踢出 userId 或 sessionId
2. 当前实例查询 Redis Session 映射
3. 找到目标 sessionId 所属 instanceId
4. 如果是当前实例，直接发送下线通知
5. 如果是其他实例，发布 Redis Pub/Sub 踢人事件
6. 目标实例收到事件后向 /user/queue/session 推送下线消息
7. 清理本地 Session 和 Redis 映射
```

踢人事件示例：

```json
{
  "eventId": "kick-100001",
  "eventType": "KICK_SESSION",
  "targetUserId": "10001",
  "targetSessionId": "session-001",
  "targetInstanceId": "ws-node-02",
  "reason": "管理员强制下线",
  "operatorId": "admin-001",
  "timestamp": 1777956000000
}
```

分布式踢人建议如下：

| 建议                        | 说明                                        |
| --------------------------- | ------------------------------------------- |
| 使用通知式下线              | 向客户端推送下线消息，客户端主动断开        |
| 目标实例执行本地清理        | 只有持有连接的实例能处理本地 Session        |
| Redis 状态同步清理          | 避免用户仍显示在线                          |
| 记录操作日志                | 管理端踢人必须审计                          |
| 踢人事件要幂等              | 重复事件不应报错                            |
| 集群 Pub/Sub 不可靠时要兜底 | 后续 SEND/SUBSCRIBE 应因 Session 无效被拒绝 |

### 分布式限流

分布式限流用于在多实例下控制连接频率、发送频率、订阅频率、广播频率和管理端操作频率。单实例本地限流只能限制当前实例请求，无法限制用户或 IP 在多个实例之间绕过限制。

限流维度建议如下：

| 维度           | Redis Key 示例                      | 说明              |
| -------------- | ----------------------------------- | ----------------- |
| IP 连接频率    | `ws:rate:connect:ip:{ip}`           | 防止恶意建连      |
| 用户连接频率   | `ws:rate:connect:user:{userId}`     | 防止频繁重连      |
| 用户发送频率   | `ws:rate:send:user:{userId}`        | 防止刷消息        |
| 群组发送频率   | `ws:rate:send:group:{groupId}`      | 防止群刷屏        |
| 目标地址频率   | `ws:rate:destination:{destination}` | 防止某 Topic 被刷 |
| 管理端操作频率 | `ws:rate:admin:{userId}`            | 防止误操作或攻击  |

常见限流算法：

| 算法     | 特点     | 适用场景       |
| -------- | -------- | -------------- |
| 固定窗口 | 实现简单 | 基础连接限制   |
| 滑动窗口 | 更平滑   | 发送频率限制   |
| 令牌桶   | 支持突发 | 聊天和通知发送 |
| 漏桶     | 平滑输出 | 批量推送保护   |

Redisson 限流器可用于分布式限流。示例服务如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsRateLimitService.java`

```java
package io.github.atengk.websocket.service;

/**
 * WebSocket 分布式限流服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsRateLimitService {

    /**
     * 判断是否允许访问
     *
     * @param key      限流 Key
     * @param permits  许可数量
     * @param interval 时间窗口秒数
     * @return 是否允许
     */
    boolean allow(String key, long permits, long interval);
}
```

基础 Redis 计数实现如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/RedisWsRateLimitServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.WsRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis WebSocket 分布式限流服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisWsRateLimitServiceImpl implements WsRateLimitService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 判断是否允许访问
     *
     * @param key      限流 Key
     * @param permits  许可数量
     * @param interval 时间窗口秒数
     * @return 是否允许
     */
    @Override
    public boolean allow(String key, long permits, long interval) {
        if (StrUtil.isBlank(key) || permits <= 0 || interval <= 0) {
            return false;
        }

        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == null) {
            return false;
        }

        if (count == 1) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(interval));
        }

        boolean allowed = count <= permits;
        if (!allowed) {
            log.warn("WebSocket 分布式限流触发，key：{}，count：{}，permits：{}", key, count, permits);
        }
        return allowed;
    }
}
```

分布式限流建议如下：

| 场景           | 建议值               |
| -------------- | -------------------- |
| 单 IP 建连     | 每分钟 30 到 100 次  |
| 单用户建连     | 每分钟 10 到 30 次   |
| 单用户发送消息 | 每秒 3 到 10 条      |
| 单群组消息     | 每秒按群规模设置     |
| 管理端广播     | 每分钟限制次数       |
| 高频非法请求   | 超限后加入临时黑名单 |

### 分布式锁

分布式锁用于保证多实例下同一任务、同一资源或同一业务事件不会被多个实例并发处理。它和分布式限流不同，限流控制频率，锁控制互斥执行。

适合使用分布式锁的场景：

| 场景         | 锁 Key 示例                           |
| ------------ | ------------------------------------- |
| 群组解散     | `ws:lock:group:disband:{groupId}`     |
| 批量通知任务 | `ws:lock:notice:batch:{taskId}`       |
| 离线消息补偿 | `ws:lock:offline:compensate:{userId}` |
| 失败消息重试 | `ws:lock:failed-message:retry`        |
| 缓存重建     | `ws:lock:cache:group:{groupId}`       |
| 房间人数校正 | `ws:lock:room:count:{roomId}`         |

分布式锁使用建议如下：

| 建议               | 说明                       |
| ------------------ | -------------------------- |
| 使用 Redisson      | 支持自动续期和可重入锁     |
| 设置合理 leaseTime | 防止死锁                   |
| 锁粒度尽量小       | 避免全局锁影响吞吐         |
| 锁内逻辑要短       | 不要长时间持锁做大批量处理 |
| 锁不能替代幂等     | 幂等仍要靠唯一键或去重表   |
| 获取失败可跳过     | 定时任务可下轮再执行       |

示例：

```java
boolean success = wsDistributedLockService.tryLockAndRun(
        "ws:lock:offline:compensate:10001",
        Duration.ofSeconds(1),
        Duration.ofSeconds(30),
        () -> {
            // 查询并补偿用户离线消息
        }
);
```

### Sticky Session

Sticky Session 是负载均衡层的一种会话保持策略，用于让同一客户端的连接尽量落到同一个后端实例。对于 WebSocket，连接建立后天然绑定到一个实例，但在重连、握手、多请求场景中，Sticky Session 可以减少用户频繁切换实例带来的状态不一致。

Sticky Session 适用场景：

| 场景                   | 是否建议                 |
| ---------------------- | ------------------------ |
| 单实例状态较多         | 可以启用                 |
| 使用内存 Session       | 建议启用，但不是最终方案 |
| 多实例 + Redis Session | 可选                     |
| Broker Relay 模式      | 不强依赖                 |
| WebSocket 重连频繁     | 可减少跨实例切换         |

需要明确：Sticky Session 不能解决所有分布式问题。它只能让客户端连接尽量固定到同一实例，不能解决以下问题：

| 问题                     | 是否解决 |
| ------------------------ | -------- |
| 业务服务调用任意实例推送 | 不能     |
| 用户点对点跨实例路由     | 不能     |
| 全局在线用户统计         | 不能     |
| 跨实例强制下线           | 不能     |
| 广播消息同步             | 不能     |
| 实例宕机后的状态清理     | 不能     |

Nginx ip_hash 示例：

```nginx
upstream websocket_backend {
    ip_hash;
    server 10.0.0.11:8080;
    server 10.0.0.12:8080;
    server 10.0.0.13:8080;
}
```

Sticky Session 建议作为优化手段，而不是架构依赖。生产集群仍应使用 Redis、Broker Relay 或跨实例事件同步解决全局状态和消息分发问题。

### 负载均衡配置

负载均衡配置用于将 WebSocket 连接分发到多个后端实例。WebSocket 需要支持 HTTP Upgrade，因此负载均衡器必须正确转发 `Upgrade` 和 `Connection` Header，并设置合理的超时时间。

Nginx 配置示例：

```nginx
upstream websocket_backend {
    # 可选：使用 ip_hash 做简单会话保持
    ip_hash;

    server 10.0.0.11:8080 max_fails=3 fail_timeout=30s;
    server 10.0.0.12:8080 max_fails=3 fail_timeout=30s;
    server 10.0.0.13:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 443 ssl;
    server_name api.example.com;

    ssl_certificate /etc/nginx/certs/api.example.com.pem;
    ssl_certificate_key /etc/nginx/certs/api.example.com.key;

    location /ws/ {
        proxy_pass http://websocket_backend;

        # WebSocket Upgrade 必需配置
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";

        # 透传真实客户端信息
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 长连接超时，必须大于心跳间隔
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;

        # 禁用代理缓冲，降低实时消息延迟
        proxy_buffering off;
    }
}
```

配置说明：`proxy_http_version 1.1`、`Upgrade` 和 `Connection` 是 WebSocket 升级的关键配置；`proxy_read_timeout` 和 `proxy_send_timeout` 必须大于服务端和客户端心跳间隔；`proxy_buffering off` 可以减少代理缓冲造成的消息延迟。

负载均衡建议如下：

| 配置项   | 建议                                    |
| -------- | --------------------------------------- |
| 连接超时 | 大于心跳间隔 2 到 3 倍以上              |
| 读写超时 | 长连接场景建议设置较长                  |
| 健康检查 | 通过 HTTP 健康接口检查实例              |
| 会话保持 | 可启用，但不作为唯一依赖                |
| TLS      | 生产环境使用 WSS                        |
| 真实 IP  | 透传 `X-Forwarded-For` 和 `X-Real-IP`   |
| 缓冲     | WebSocket location 禁用 proxy buffering |

### 网关转发配置

网关转发配置用于在 Spring Cloud Gateway、Kubernetes Ingress、API Gateway 或 Nginx 网关中正确转发 WebSocket 请求。WebSocket 请求和普通 HTTP 请求不同，必须支持协议升级和长连接。

Spring Cloud Gateway 路由示例：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: websocket-route
          uri: lb:ws://websocket-service
          predicates:
            - Path=/ws/**
          filters:
            # 保留原始请求路径
            - PreserveHostHeader
```

如果 WebSocket 服务通过 HTTP 负载均衡转发，也可以使用：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: websocket-route
          uri: lb://websocket-service
          predicates:
            - Path=/ws/**
```

Kubernetes Ingress Nginx 示例：

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: websocket-ingress
  namespace: default
  annotations:
    # WebSocket 长连接超时配置
    nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "60"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - api.example.com
      secretName: api-example-com-tls
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /ws
            pathType: Prefix
            backend:
              service:
                name: websocket-service
                port:
                  number: 8080
```

网关转发注意事项如下：

| 注意项             | 说明                                           |
| ------------------ | ---------------------------------------------- |
| 路径不要被错误重写 | `/ws/stomp` 应正确转发到后端                   |
| Header 要透传      | Authorization、Cookie、Origin、X-Forwarded-For |
| 超时要足够长       | 避免网关过早断开连接                           |
| 支持 WSS           | TLS 终止可在网关层完成                         |
| 跨域配置一致       | 网关和后端 Origin 策略不要冲突                 |
| 大规模连接要调优   | 网关 worker、连接数、文件句柄都要配置          |
| 灰度发布要谨慎     | WebSocket 长连接不会像 HTTP 请求一样快速切流   |

网关层只能做通用转发、来源控制、基础限流和证书处理，不能替代服务端 STOMP 层的 `CONNECT`、`SUBSCRIBE`、`SEND` 鉴权。生产环境必须同时保留网关安全和应用层权限校验。


## 安全设计

安全设计用于保证 WebSocket STOMP 通道在连接、认证、订阅、发送、消息内容、频率控制、黑名单、脱敏、XSS、CSRF 和审计等方面具备完整防护。WebSocket 是长连接，一旦连接建立，客户端可以持续发送 STOMP 帧，因此安全控制不能只停留在 HTTP 登录阶段，必须覆盖握手阶段、`CONNECT` 阶段、`SUBSCRIBE` 阶段、`SEND` 阶段和业务处理阶段。

### 连接来源校验

连接来源校验用于限制允许建立 WebSocket 连接的前端域名，防止未知站点直接连接 WebSocket Endpoint。来源校验通常基于 HTTP 握手请求中的 `Origin` Header 完成。

推荐校验范围如下：

| 环境     | 允许来源                                         |
| -------- | ------------------------------------------------ |
| 本地开发 | `http://localhost:5173`、`http://localhost:3000` |
| 测试环境 | 测试域名，例如 `https://test.example.com`        |
| 生产环境 | 正式业务域名，例如 `https://admin.example.com`   |
| 内部系统 | 企业网关域名或统一门户域名                       |

配置示例：

```yaml
app:
  websocket:
    # 生产环境建议明确配置允许来源，不要使用 "*"
    allowed-origin-patterns:
      - "https://admin.example.com"
      - "https://app.example.com"
      - "https://*.example.com"
```

服务端配置示例：

```java
registry.addEndpoint(properties.getEndpoint())
        .setAllowedOriginPatterns(properties.getAllowedOriginPatterns().toArray(new String[0]))
        .withSockJS();
```

连接来源校验注意事项如下：

| 注意项                     | 说明                                   |
| -------------------------- | -------------------------------------- |
| 不要把 `*` 用于生产环境    | 会放大跨站连接风险                     |
| Origin 不等于认证          | 来源合法仍然需要 Token 鉴权            |
| 网关不要丢失 Origin        | Nginx、Gateway 应正确透传 Header       |
| Cookie 认证必须校验 Origin | 防止浏览器自动带 Cookie 被滥用         |
| 记录非法来源               | 高频非法 Origin 连接应进入审计和黑名单 |

### Token 过期处理

Token 过期处理用于在 WebSocket 长连接期间识别登录态失效。WebSocket 连接可能持续数小时，如果只在连接建立时校验 Token，用户被禁用、Token 过期、密码修改、权限回收后，连接仍可能继续发送和订阅消息。

Token 过期处理建议覆盖以下阶段：

| 阶段           | 处理方式                            |
| -------------- | ----------------------------------- |
| 握手阶段       | 可初步校验 Query Token 或 Cookie    |
| CONNECT 阶段   | 必须校验 Token 签名和过期时间       |
| SUBSCRIBE 阶段 | 校验当前会话是否仍有效              |
| SEND 阶段      | 校验 Token 是否过期、用户是否被禁用 |
| 定时扫描       | 主动关闭过期会话或推送重新登录通知  |

Token 过期后建议向当前用户推送会话失效消息：

```json
{
  "messageId": "1909550919275922001",
  "type": "SESSION_EXPIRED",
  "status": "FAILED",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "code": "WS_AUTH_TOKEN_EXPIRED",
    "message": "登录状态已过期，请重新登录",
    "retryable": false
  }
}
```

Token 过期处理建议如下：

| 建议                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| CONNECT 时必须校验     | 防止无效 Token 建立连接              |
| 长连接期间持续校验     | SEND 和 SUBSCRIBE 不应只信任初始连接 |
| 用户改密后失效旧 Token | 可使用 Token 版本号或登录版本号      |
| 过期后停止重连         | 前端应跳转登录，而不是无限重连       |
| 不在日志打印完整 Token | 只打印 Token 摘要或 traceId          |

### Token 刷新机制

Token 刷新机制用于处理访问令牌即将过期但用户仍在活跃使用系统的情况。WebSocket 本身不适合直接承载完整登录刷新流程，推荐通过 HTTP 刷新 Token，然后重建 WebSocket 连接。

推荐流程如下：

```text
1. 前端发现 access_token 即将过期
2. 调用 HTTP 刷新接口获取新 Token
3. 如果刷新成功，关闭旧 WebSocket 连接
4. 使用新 Token 重新建立 STOMP 连接
5. 重连成功后恢复订阅
6. 调用补偿接口同步断开期间消息
```

不建议在同一条 STOMP 连接上直接替换 `Principal`。更清晰的方式是重连并重新认证，避免连接身份和权限状态混乱。

前端处理建议：

```typescript
const refreshTokenAndReconnect = async () => {
  const response = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${localStorage.getItem('refresh_token')}`
    }
  })

  if (!response.ok) {
    client.deactivate()
    window.location.href = '/login'
    return
  }

  const result = await response.json()
  localStorage.setItem('access_token', result.data.accessToken)

  await client.deactivate()
  createAndActivateClient(result.data.accessToken)
}
```

Token 刷新机制注意事项如下：

| 注意项                    | 说明                              |
| ------------------------- | --------------------------------- |
| 刷新走 HTTP               | 不建议通过 WebSocket 完成登录刷新 |
| 刷新失败立即下线          | 清理连接并跳转登录                |
| 重连后恢复订阅            | 旧订阅已失效                      |
| 重连后补偿消息            | 使用 HTTP 查询断线期间消息        |
| 服务端以新 Token 重建身份 | 避免旧权限残留                    |

### 订阅路径鉴权

订阅路径鉴权用于防止用户订阅不属于自己的 Topic 或 Queue。WebSocket STOMP 中，客户端可以手工构造 `SUBSCRIBE` 帧，因此服务端必须在 `SUBSCRIBE` 阶段校验目标地址权限。

订阅路径鉴权规则如下：

| 路径                          | 鉴权规则                     |
| ----------------------------- | ---------------------------- |
| `/topic/system/notice`        | 登录用户可订阅               |
| `/topic/system/alarm`         | 管理员或运维可订阅           |
| `/topic/admin/**`             | 管理员、运维或指定角色可订阅 |
| `/topic/group/{groupId}`      | 群组成员可订阅               |
| `/topic/tenant/{tenantId}/**` | 当前租户用户可订阅           |
| `/user/queue/**`              | 当前登录用户私有队列         |

订阅鉴权示例：

```java
private void handleSubscribe(StompHeaderAccessor accessor) {
    Principal principal = accessor.getUser();
    String destination = accessor.getDestination();

    if (principal == null || StrUtil.isBlank(principal.getName())) {
        throw new AccessDeniedException("用户未认证");
    }

    if (WsDestinationValidator.isInvalid(destination)) {
        throw new AccessDeniedException("订阅地址非法");
    }

    String userId = principal.getName();

    if (StrUtil.startWith(destination, "/topic/admin") && !authUserService.isAdmin(principal)) {
        log.warn("管理端订阅被拒绝，userId：{}，destination：{}", userId, destination);
        throw new AccessDeniedException("无管理端订阅权限");
    }

    if (StrUtil.startWith(destination, "/topic/group/")) {
        String groupId = ReUtil.getGroup1("^/topic/group/([^/]+).*$", destination);
        if (!authUserService.canSubscribeGroup(userId, groupId)) {
            log.warn("群组订阅被拒绝，userId：{}，groupId：{}", userId, groupId);
            throw new AccessDeniedException("无群组订阅权限");
        }
    }
}
```

订阅路径鉴权建议如下：

| 建议                    | 说明                                |
| ----------------------- | ----------------------------------- |
| 不信任前端路由          | 前端隐藏菜单不等于安全              |
| 所有敏感 Topic 必须鉴权 | 尤其是 admin、tenant、group         |
| 鉴权失败记录日志        | 记录 userId、sessionId、destination |
| 高频非法订阅进入风控    | 多次异常可封禁或断开连接            |
| 权限缓存要及时失效      | 群成员、角色变更后要刷新缓存        |

### 消息内容校验

消息内容校验用于保证客户端发送的消息体符合业务规则，防止空消息、非法 JSON、字段缺失、类型错误、恶意脚本、超长内容和非法枚举值进入业务层。

校验内容如下：

| 校验项    | 说明                                    |
| --------- | --------------------------------------- |
| JSON 格式 | 必须能正确反序列化                      |
| 必填字段  | 例如 `receiverId`、`groupId`、`content` |
| 字段长度  | 防止超长字符串                          |
| 枚举值    | `contentType`、`messageType` 必须合法   |
| 业务 ID   | 必须符合格式或存在性校验                |
| 内容安全  | 敏感词、XSS、恶意链接                   |
| 附件 ID   | 校验附件归属和访问权限                  |

请求 DTO 示例：

```java
package io.github.atengk.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天文本消息请求
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class ChatTextSendRequest {

    /**
     * 接收人 ID，单聊时必填
     */
    private String receiverId;

    /**
     * 群组 ID，群聊时必填
     */
    private String groupId;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过 2000 个字符")
    private String content;

    /**
     * 内容类型
     */
    @NotBlank(message = "内容类型不能为空")
    @Pattern(regexp = "TEXT|IMAGE|FILE|SYSTEM", message = "内容类型非法")
    private String contentType;

    /**
     * 客户端消息 ID
     */
    @Size(max = 128, message = "客户端消息ID不能超过 128 个字符")
    private String clientMessageId;
}
```

消息内容校验建议如下：

| 建议                     | 说明                               |
| ------------------------ | ---------------------------------- |
| DTO 使用 Bean Validation | 在 `@MessageMapping` 中加 `@Valid` |
| 服务端二次校验业务权限   | 不能只校验字段格式                 |
| 不信任 senderId          | 发送人必须从 Principal 获取        |
| 附件必须校验归属         | 防止发送他人文件                   |
| 错误返回用户队列         | 参数错误返回 `/user/queue/errors`  |

### 消息长度限制

消息长度限制用于防止超大消息导致内存占用升高、网络拥塞、序列化开销过大、前端卡顿或拒绝服务攻击。WebSocket 不适合传输大文件，文件应走 HTTP 上传，WebSocket 只传元数据。

推荐长度限制如下：

| 类型             | 建议限制                  |
| ---------------- | ------------------------- |
| 文本聊天内容     | 500 到 2000 字符          |
| 通知标题         | 128 字符                  |
| 通知内容         | 1000 到 5000 字符         |
| 自定义 Header    | 128 到 512 字符           |
| 单条 JSON 消息体 | 16KB 到 64KB              |
| 附件消息元数据   | 4KB 到 16KB               |
| 二进制文件       | 不建议通过 WebSocket 传输 |

服务端校验示例：

```java
if (StrUtil.length(request.getContent()) > 2000) {
    log.warn("消息内容过长，userId：{}，长度：{}", userId, StrUtil.length(request.getContent()));
    throw new IllegalArgumentException("消息内容不能超过 2000 个字符");
}
```

消息长度限制建议如下：

| 建议                 | 说明                         |
| -------------------- | ---------------------------- |
| DTO 层限制字段长度   | 使用 `@Size`                 |
| 通道层限制整体大小   | 防止直接绕过 DTO             |
| 附件不走 WebSocket   | 只推送 fileId、fileName、url |
| 超长消息记录安全日志 | 高频超长消息可能是攻击       |
| 前端也做限制         | 提前阻止用户输入过长内容     |

### 发送频率限制

发送频率限制用于防止用户刷消息、脚本攻击、恶意推送和出站通道拥塞。发送限流应在 `SEND` 阶段执行，优先按用户、IP、目标地址、群组等维度控制。

推荐限制值如下：

| 场景           | 建议限制                        |
| -------------- | ------------------------------- |
| 单用户聊天发送 | 每秒 3 到 10 条                 |
| 单用户通知操作 | 每秒 5 到 20 次                 |
| 单群组消息     | 每秒 50 到 500 条，按群规模调整 |
| 管理端广播     | 每分钟 1 到 10 次               |
| 同一目标地址   | 每秒按系统容量限制              |
| 异常错误消息   | 触发后降频返回                  |

发送限流 Key 示例：

```text
ws:rate:send:user:{userId}
ws:rate:send:ip:{ip}
ws:rate:send:group:{groupId}
ws:rate:send:destination:{destinationHash}
```

发送限流处理示例：

```java
String rateKey = StrUtil.format("ws:rate:send:user:{}", userId);
boolean allowed = wsRateLimitService.allow(rateKey, 5, 1);
if (!allowed) {
    log.warn("用户发送频率超限，userId：{}，destination：{}", userId, destination);
    throw new AccessDeniedException("发送过于频繁，请稍后再试");
}
```

发送频率限制建议如下：

| 建议                   | 说明                   |
| ---------------------- | ---------------------- |
| 优先在 SEND 拦截中处理 | 进入业务层前拦截       |
| 用户和 IP 组合限流     | 防止单 IP 多账号刷消息 |
| 群组单独限流           | 防止大群刷屏           |
| 管理端广播限流更严格   | 防止误操作影响全站用户 |
| 超限错误也要限频       | 防止错误消息反向刷屏   |

### 连接频率限制

连接频率限制用于防止客户端频繁建立 WebSocket 连接，造成握手压力、认证压力、Session 膨胀和日志噪声。频繁重连可能来自网络异常、前端 Bug、Token 过期后无限重连或恶意攻击。

推荐限制维度：

| 维度         | Key 示例                              |
| ------------ | ------------------------------------- |
| IP 建连频率  | `ws:rate:connect:ip:{ip}`             |
| 用户建连频率 | `ws:rate:connect:user:{userId}`       |
| 设备建连频率 | `ws:rate:connect:device:{deviceId}`   |
| 来源建连频率 | `ws:rate:connect:origin:{originHash}` |

推荐限制值：

| 场景         | 建议                |
| ------------ | ------------------- |
| 单 IP 建连   | 每分钟 30 到 100 次 |
| 单用户建连   | 每分钟 10 到 30 次  |
| 单设备建连   | 每分钟 10 到 30 次  |
| 认证失败建连 | 每分钟 5 到 10 次   |
| 黑名单 IP    | 直接拒绝            |

连接频率限制建议在握手阶段和 CONNECT 阶段都处理：

| 阶段         | 处理内容                 |
| ------------ | ------------------------ |
| 握手阶段     | 按 IP、Origin、设备限制  |
| CONNECT 阶段 | 解析用户后按 userId 限制 |
| 认证失败     | 单独增加失败计数         |
| 高频失败     | 临时加入黑名单           |
| Token 过期   | 前端应停止自动重连       |

### IP 黑名单

IP 黑名单用于阻止恶意来源建立连接或发送消息。黑名单可以分为永久黑名单和临时黑名单。

IP 黑名单类型如下：

| 类型           | 说明                           |
| -------------- | ------------------------------ |
| 永久黑名单     | 管理员手动封禁                 |
| 临时黑名单     | 高频异常自动封禁，TTL 到期释放 |
| 网段黑名单     | 封禁异常网段                   |
| 地域或来源限制 | 特殊系统可按网络区域限制       |

Redis Key 建议：

```text
ws:blacklist:ip
ws:blacklist:ip:temp:{ip}
```

处理策略如下：

| 阶段           | 操作                         |
| -------------- | ---------------------------- |
| 握手前         | 检查 IP 黑名单，命中直接拒绝 |
| CONNECT        | 记录认证失败次数             |
| SEND/SUBSCRIBE | 记录非法操作次数             |
| 达到阈值       | 加入临时黑名单               |
| 管理端         | 支持查询、添加、解除黑名单   |

IP 黑名单注意事项如下：

| 注意项             | 说明                           |
| ------------------ | ------------------------------ |
| 获取真实 IP        | 网关必须透传 `X-Forwarded-For` |
| 不误封内网网关     | 多用户共享出口 IP 时谨慎处理   |
| 临时封禁优先       | 自动风控建议先临时封禁         |
| 手动封禁需审计     | 记录操作人和原因               |
| 封禁后停止错误回显 | 避免暴露策略细节               |

### 用户黑名单

用户黑名单用于限制风险用户建立连接、订阅主题、发送消息或进入群组。用户黑名单通常来源于管理员封禁、风控策略、违规行为、账号冻结或安全事件。

用户黑名单类型如下：

| 类型       | 说明                       |
| ---------- | -------------------------- |
| 全局封禁   | 禁止建立 WebSocket 连接    |
| 发送封禁   | 允许接收，不允许发送       |
| 群组封禁   | 禁止进入或发送群组消息     |
| 管理端封禁 | 禁止访问管理端订阅         |
| 临时禁言   | 一段时间内不能发送聊天消息 |

Redis Key 建议：

```text
ws:blacklist:user
ws:blacklist:user:temp:{userId}
ws:blacklist:user:send:{userId}
ws:blacklist:group:{groupId}:user:{userId}
```

用户黑名单处理建议如下：

| 场景                       | 处理                       |
| -------------------------- | -------------------------- |
| CONNECT 阶段命中全局封禁   | 拒绝连接                   |
| SEND 阶段命中发送封禁      | 拒绝发送                   |
| SUBSCRIBE 群组命中群组封禁 | 拒绝订阅                   |
| 用户被封禁时在线           | 推送下线通知并清理 Session |
| 黑名单解除                 | 清理缓存并允许后续连接     |

用户黑名单必须与操作日志结合，所有人工封禁和解封都应记录操作人、原因、时间和影响范围。

### 敏感信息脱敏

敏感信息脱敏用于防止 Token、手机号、邮箱、身份证、银行卡、密码、Cookie、IP、设备指纹、内部异常堆栈等敏感信息在 WebSocket 消息、日志、异常响应和管理端页面中泄露。

需要脱敏的数据如下：

| 数据     | 脱敏方式               |
| -------- | ---------------------- |
| Token    | 只显示前 8 位和后 4 位 |
| 手机号   | `138****1234`          |
| 邮箱     | `a***@example.com`     |
| 身份证   | 保留前后少量字符       |
| Cookie   | 不打印完整值           |
| IP       | 普通用户侧可部分脱敏   |
| 设备 ID  | 管理端按权限展示       |
| 异常堆栈 | 前端只返回 traceId     |

脱敏工具类如下。

文件位置：`src/main/java/io/github/atengk/websocket/util/WsSensitiveMaskUtil.java`

```java
package io.github.atengk.websocket.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

/**
 * WebSocket 敏感信息脱敏工具类
 *
 * @author Ateng
 * @since 2026-05-05
 */
public final class WsSensitiveMaskUtil {

    private WsSensitiveMaskUtil() {
    }

    /**
     * 脱敏 Token
     *
     * @param token 原始 Token
     * @return 脱敏 Token
     */
    public static String maskToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }

        String cleanToken = StrUtil.removePrefixIgnoreCase(token.trim(), "Bearer ");
        if (cleanToken.length() <= 12) {
            return "***";
        }

        return StrUtil.subPre(cleanToken, 8) + "***" + StrUtil.subSuf(cleanToken, cleanToken.length() - 4);
    }

    /**
     * 脱敏手机号
     *
     * @param phone 手机号
     * @return 脱敏手机号
     */
    public static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return null;
        }
        return DesensitizedUtil.mobilePhone(phone);
    }

    /**
     * 脱敏邮箱
     *
     * @param email 邮箱
     * @return 脱敏邮箱
     */
    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return null;
        }
        return DesensitizedUtil.email(email);
    }
}
```

脱敏建议如下：

| 场景             | 建议                           |
| ---------------- | ------------------------------ |
| 日志             | Token、Cookie、手机号必须脱敏  |
| 错误响应         | 不返回内部堆栈和 SQL           |
| 管理端           | 按角色展示敏感字段             |
| WebSocket 消息体 | 普通用户不接收敏感字段         |
| 审计日志         | 可保存必要摘要，不保存完整凭证 |

### XSS 防护

XSS 防护用于防止聊天内容、通知内容、群公告、系统消息中携带恶意脚本，导致前端渲染时执行攻击代码。WebSocket 实时推送内容同样可能成为 XSS 入口。

常见风险内容：

```html
<script>alert(1)</script>
<img src=x onerror=alert(1)>
<a href="javascript:alert(1)">点击</a>
```

XSS 防护建议如下：

| 层级           | 处理                                                        |
| -------------- | ----------------------------------------------------------- |
| 服务端输入校验 | 拒绝明显恶意内容                                            |
| 服务端内容过滤 | 对 HTML 标签进行清理或转义                                  |
| 前端渲染       | 不使用 `v-html` 或 `dangerouslySetInnerHTML` 渲染不可信内容 |
| 富文本消息     | 使用白名单 HTML 清洗                                        |
| URL 校验       | 禁止 `javascript:` 协议                                     |
| CSP            | 配置内容安全策略降低风险                                    |

服务端基础转义示例：

```java
String safeContent = HtmlUtil.escape(request.getContent());
```

示例服务：

文件位置：`src/main/java/io/github/atengk/websocket/service/WsContentSecurityService.java`

```java
package io.github.atengk.websocket.service;

/**
 * WebSocket 内容安全服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsContentSecurityService {

    /**
     * 清理消息内容
     *
     * @param content 原始内容
     * @return 安全内容
     */
    String sanitize(String content);

    /**
     * 判断内容是否存在高风险
     *
     * @param content 原始内容
     * @return 是否高风险
     */
    boolean isHighRisk(String content);
}
```

基础实现：

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsContentSecurityServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.text.escape.Html4Escape;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.WsContentSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WebSocket 内容安全服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class WsContentSecurityServiceImpl implements WsContentSecurityService {

    /**
     * 清理消息内容
     *
     * @param content 原始内容
     * @return 安全内容
     */
    @Override
    public String sanitize(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }
        return Html4Escape.escape(content);
    }

    /**
     * 判断内容是否存在高风险
     *
     * @param content 原始内容
     * @return 是否高风险
     */
    @Override
    public boolean isHighRisk(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }

        boolean scriptRisk = ReUtil.contains("(?i)<\\s*script", content);
        boolean eventRisk = ReUtil.contains("(?i)onerror\\s*=|onclick\\s*=|onload\\s*=", content);
        boolean jsUrlRisk = ReUtil.contains("(?i)javascript\\s*:", content);

        if (scriptRisk || eventRisk || jsUrlRisk) {
            log.warn("检测到 WebSocket 高风险内容");
            return true;
        }
        return false;
    }
}
```

### CSRF 风险处理

CSRF 风险在 WebSocket 场景下主要发生于 Cookie 认证模式。浏览器可能自动携带 Cookie 建立连接，如果服务端只依赖 Cookie 而不校验 Origin 和 Token，恶意站点可能诱导用户浏览器发起 WebSocket 连接。

CSRF 风险处理建议如下：

| 措施              | 说明                                   |
| ----------------- | -------------------------------------- |
| 校验 Origin       | 只允许可信站点建立连接                 |
| 使用 Token Header | 优先通过 STOMP CONNECT Header 传 Token |
| SameSite Cookie   | Cookie 设置合适的 SameSite 策略        |
| Secure Cookie     | 生产环境使用 HTTPS/WSS                 |
| 连接阶段认证      | CONNECT 阶段必须校验用户               |
| 订阅和发送鉴权    | 即使连接成功也要校验每个动作           |

Cookie 认证场景建议：

| 场景       | 建议                                   |
| ---------- | -------------------------------------- |
| 前后端同域 | Cookie 可用，但仍校验 Origin           |
| 前后端跨域 | 谨慎使用 Cookie，配置 SameSite 和 CORS |
| 高安全系统 | 优先使用短期 Token + Header            |
| 管理端系统 | 强制 Origin 白名单和二次权限校验       |

Spring Security 中可对 WebSocket 握手路径忽略传统 CSRF，但不能忽略 WebSocket 自身的安全控制：

```java
.csrf(csrf -> csrf.ignoringRequestMatchers("/ws/**"))
```

这不代表 WebSocket 不需要防护，而是防护点转移到 Origin 校验、Token 校验和 STOMP 帧级鉴权。

### WebSocket 安全审计

WebSocket 安全审计用于记录连接、认证、订阅、发送、拒绝、黑名单、限流、踢人、管理端操作等安全相关行为。安全审计可以帮助排查越权访问、攻击行为、异常连接和内部误操作。

审计事件类型建议如下：

| 类型                | 说明                |
| ------------------- | ------------------- |
| `CONNECT_SUCCESS`   | 连接成功            |
| `CONNECT_FAILED`    | 连接失败            |
| `AUTH_FAILED`       | 认证失败            |
| `SUBSCRIBE_DENIED`  | 订阅被拒绝          |
| `SEND_DENIED`       | 发送被拒绝          |
| `RATE_LIMITED`      | 触发限流            |
| `BLACKLIST_HIT`     | 命中黑名单          |
| `KICK_SESSION`      | 踢出连接            |
| `ADMIN_OPERATION`   | 管理端操作          |
| `XSS_RISK_DETECTED` | 检测到 XSS 风险内容 |

审计日志表可复用前文 `ws_operation_log`，也可以单独建立安全审计表。

```sql
CREATE TABLE ws_security_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    audit_id VARCHAR(64) NOT NULL COMMENT '审计ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    user_id VARCHAR(64) DEFAULT NULL COMMENT '用户ID',
    session_id VARCHAR(128) DEFAULT NULL COMMENT 'Session ID',
    destination VARCHAR(256) DEFAULT NULL COMMENT '目标地址',
    ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    client_type VARCHAR(32) DEFAULT NULL COMMENT '客户端类型',
    device_id VARCHAR(128) DEFAULT NULL COMMENT '设备ID',
    risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW' COMMENT '风险级别',
    content VARCHAR(1024) DEFAULT NULL COMMENT '审计内容摘要',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_audit_id (audit_id),
    KEY idx_event_type (event_type),
    KEY idx_user_id (user_id),
    KEY idx_ip (ip),
    KEY idx_created_at (created_at)
) COMMENT='WebSocket安全审计日志表';
```

安全审计建议如下：

| 建议               | 说明                           |
| ------------------ | ------------------------------ |
| 认证失败必须记录   | 高频失败可能是攻击             |
| 授权失败必须记录   | 订阅敏感 Topic 要留痕          |
| 黑名单命中必须记录 | 便于风控分析                   |
| 管理端操作必须记录 | 踢人、禁言、公告、广播都要审计 |
| 敏感字段脱敏       | 不保存完整 Token、Cookie、密码 |
| 支持告警规则       | 高频风险事件自动告警           |

## 限流与风控

限流与风控用于保护 WebSocket STOMP 服务免受高频连接、恶意发送、非法订阅、刷屏、批量广播、超大消息和攻击行为影响。限流关注“频率控制”，风控关注“异常行为识别和处置”。生产环境建议同时使用网关限流、应用层限流、Redis 分布式限流和安全审计。

### 用户级连接限流

用户级连接限流用于限制同一用户在短时间内频繁建立 WebSocket 连接。频繁连接可能来自前端自动重连 Bug、Token 过期后无限重试、用户刷新页面过快或恶意脚本攻击。

推荐限制如下：

| 场景               | 建议限制           |
| ------------------ | ------------------ |
| 普通用户建连       | 每分钟 10 到 30 次 |
| 管理端用户建连     | 每分钟 5 到 10 次  |
| Token 过期建连失败 | 每分钟 3 到 5 次   |
| 同一设备建连       | 每分钟 5 到 10 次  |

Redis Key：

```text
ws:rate:connect:user:{userId}
ws:rate:connect:device:{deviceId}
```

CONNECT 阶段限流示例：

```java
String connectRateKey = StrUtil.format("ws:rate:connect:user:{}", userId);
boolean allowed = wsRateLimitService.allow(connectRateKey, 20, 60);
if (!allowed) {
    log.warn("用户 WebSocket 建连频率超限，userId：{}", userId);
    throw new AccessDeniedException("连接过于频繁，请稍后再试");
}
```

用户级连接限流建议如下：

| 建议                | 说明                         |
| ------------------- | ---------------------------- |
| 在 CONNECT 阶段执行 | 解析出 userId 后限流         |
| 结合设备维度        | 防止同一用户多设备异常刷连接 |
| 认证失败单独计数    | 避免无效 Token 无限重连      |
| 管理端更严格        | 管理端连接频率应更低         |
| 超限后前端停止重连  | 返回明确错误码               |

### IP 级连接限流

IP 级连接限流用于防止同一 IP 高频建立连接，保护握手接口和认证逻辑。它适合在握手阶段执行，因为此时可能还没有解析到用户身份。

推荐限制如下：

| 场景              | 建议限制            |
| ----------------- | ------------------- |
| 单 IP 握手请求    | 每分钟 50 到 200 次 |
| 单 IP 认证失败    | 每分钟 10 到 30 次  |
| 单 IP 非法 Origin | 每分钟 5 到 10 次   |
| 黑名单 IP         | 直接拒绝            |

Redis Key：

```text
ws:rate:connect:ip:{ip}
ws:rate:auth-failed:ip:{ip}
```

握手阶段限流示例：

```java
String rateKey = StrUtil.format("ws:rate:connect:ip:{}", ip);
boolean allowed = wsRateLimitService.allow(rateKey, 100, 60);
if (!allowed) {
    log.warn("IP WebSocket 建连频率超限，ip：{}", ip);
    return false;
}
```

IP 限流注意事项如下：

| 注意项             | 说明                              |
| ------------------ | --------------------------------- |
| 获取真实 IP        | 必须正确处理 `X-Forwarded-For`    |
| 谨慎处理 NAT       | 企业内网或移动网络可能多人共用 IP |
| 与用户限流组合     | IP 限流不能替代用户限流           |
| 高频异常进入黑名单 | 多次超限可临时封禁                |
| 网关层也要限流     | 在 Nginx/Gateway 提前挡住明显攻击 |

### 用户级发送限流

用户级发送限流用于限制用户发送 STOMP `SEND` 消息的频率。它是防止聊天刷屏、通知操作刷接口、恶意脚本发送的重要手段。

推荐限制如下：

| 消息类型   | 建议限制                  |
| ---------- | ------------------------- |
| 聊天文本   | 每秒 3 到 10 条           |
| 通知已读   | 每秒 10 到 30 次          |
| ACK 回执   | 每秒 20 到 100 次，可批量 |
| 管理端操作 | 每分钟 5 到 20 次         |
| 任务订阅   | 每分钟 10 到 30 次        |

用户发送限流 Key：

```text
ws:rate:send:user:{userId}
ws:rate:send:user:{userId}:{messageType}
```

发送限流示例：

```java
String sendRateKey = StrUtil.format("ws:rate:send:user:{}", userId);
if (!wsRateLimitService.allow(sendRateKey, 5, 1)) {
    log.warn("用户发送消息频率超限，userId：{}，destination：{}", userId, destination);
    throw new AccessDeniedException("发送过于频繁，请稍后再试");
}
```

用户级发送限流建议如下：

| 建议                 | 说明                         |
| -------------------- | ---------------------------- |
| 在 SEND 拦截器中处理 | 进入业务层前拦截             |
| 按消息类型区分       | 聊天、ACK、管理操作限制不同  |
| 超限返回错误队列     | 给当前用户明确提示           |
| 高频超限进入风控     | 记录异常行为                 |
| 支持白名单           | 内部系统任务用户可有独立规则 |

### 目标地址级限流

目标地址级限流用于限制某个 STOMP destination 的访问频率，防止某个 Topic、群组、管理端路径或业务路径被大量请求打爆。

适用路径示例：

| 路径                     | 限流目的           |
| ------------------------ | ------------------ |
| `/app/chat/group/send`   | 控制群聊发送入口   |
| `/app/admin/push/test`   | 控制管理端推送测试 |
| `/topic/group/{groupId}` | 控制群组订阅频率   |
| `/topic/admin/**`        | 控制管理端订阅     |
| `/app/notice/read`       | 控制通知已读操作   |

目标地址限流 Key：

```text
ws:rate:destination:{destinationHash}
```

由于 destination 可能包含斜杠和变量，建议先做 Hash：

```java
String destinationHash = SecureUtil.md5(destination);
String rateKey = StrUtil.format("ws:rate:destination:{}", destinationHash);
```

目标地址级限流建议如下：

| 建议                   | 说明                          |
| ---------------------- | ----------------------------- |
| 对热点路径配置独立阈值 | 群聊和 ACK 阈值不同           |
| 管理端路径更严格       | 防止广播误操作                |
| 配合用户限流           | 单路径限流不能替代用户限流    |
| 动态配置               | 生产环境建议通过配置中心调整  |
| 记录热点目标           | 便于发现异常 Topic 或业务入口 |

### 群组消息限流

群组消息限流用于防止单个群组被刷屏，尤其是大群、直播间、房间消息和项目协作空间。群组限流应同时考虑用户维度和群组维度。

群组限流维度如下：

| 维度             | Key 示例                                    |
| ---------------- | ------------------------------------------- |
| 单用户在群内发送 | `ws:rate:group:user:{groupId}:{userId}`     |
| 单群整体发送     | `ws:rate:group:{groupId}`                   |
| 单群重复内容     | `ws:rate:group:dup:{groupId}:{contentHash}` |
| 群组管理操作     | `ws:rate:group:admin:{groupId}:{userId}`    |

推荐限制如下：

| 场景         | 建议限制                 |
| ------------ | ------------------------ |
| 用户群聊发送 | 每秒 3 到 5 条           |
| 小群整体发送 | 每秒 50 条               |
| 大群整体发送 | 按压测结果控制           |
| 重复内容     | 10 秒内相同内容最多 3 次 |
| 被警告用户   | 动态降低阈值             |

群组限流处理建议：

| 超限类型     | 处理                 |
| ------------ | -------------------- |
| 用户轻微超限 | 返回错误提示         |
| 用户持续超限 | 临时禁言             |
| 群组整体超限 | 降频或丢弃非关键消息 |
| 重复内容超限 | 拦截并记录风控       |
| 大规模刷屏   | 管理员告警并自动封禁 |

### 广播消息限流

广播消息限流用于控制系统公告、管理端广播、全站通知、实时看板等大范围推送的频率。广播消息影响范围大，必须比普通用户消息更严格。

广播限流场景如下：

| 场景           | 风险                   |
| -------------- | ---------------------- |
| 全站公告       | 同时推送给所有在线用户 |
| 管理端推送测试 | 误操作可能影响全站     |
| 实时看板       | 高频推送可能压垮前端   |
| 系统告警       | 异常风暴可能刷屏       |
| 租户公告       | 大租户下影响范围较大   |

广播限流 Key：

```text
ws:rate:broadcast:system
ws:rate:broadcast:admin:{userId}
ws:rate:broadcast:destination:{destinationHash}
```

广播限流建议如下：

| 广播类型       | 建议                   |
| -------------- | ---------------------- |
| 全站公告       | 每分钟 1 到 5 次       |
| 管理端推送测试 | 每分钟 5 到 10 次      |
| 看板刷新       | 3 到 10 秒一次         |
| 告警通知       | 按告警类型聚合后推送   |
| 任务进度广播   | 节流推送，只推关键节点 |

广播消息必须支持聚合和降频。例如系统告警短时间内大量出现时，不应每条都广播到管理端，而应聚合为“过去 1 分钟产生 120 条同类告警”。

### 高频订阅限制

高频订阅限制用于防止客户端频繁订阅和取消订阅 Topic，造成 Broker 压力、订阅关系膨胀和安全日志噪声。常见于前端页面重复初始化、路由切换未取消订阅或恶意客户端构造大量订阅。

限制维度如下：

| 维度             | Key 示例                                          |
| ---------------- | ------------------------------------------------- |
| 用户订阅频率     | `ws:rate:subscribe:user:{userId}`                 |
| Session 订阅频率 | `ws:rate:subscribe:session:{sessionId}`           |
| 目标地址订阅频率 | `ws:rate:subscribe:destination:{destinationHash}` |
| 非法订阅次数     | `ws:risk:subscribe:denied:{userId}`               |

推荐限制如下：

| 场景            | 建议                    |
| --------------- | ----------------------- |
| 单用户订阅      | 每分钟 50 到 200 次     |
| 单 Session 订阅 | 每分钟 50 到 100 次     |
| 非法订阅        | 每分钟 5 到 10 次后风控 |
| 管理端订阅      | 按页面固定主题控制      |
| 群组订阅        | 用户必须是群成员        |

高频订阅处理建议如下：

| 处理         | 说明                   |
| ------------ | ---------------------- |
| 正常超限     | 返回错误并拒绝订阅     |
| 非法订阅超限 | 记录安全审计           |
| 持续异常     | 强制断开连接           |
| 恶意目标扫描 | 加入临时黑名单         |
| 前端重复订阅 | 通过订阅注册表避免重复 |

### 消息大小限制

消息大小限制用于从整体 STOMP 帧或 JSON Body 维度限制消息体大小。字段级 `@Size` 只能限制 DTO 字段，不能完全防止异常大 JSON 或恶意负载，因此还需要在通道层或容器层配置整体限制。

建议限制如下：

| 类型           | 建议                      |
| -------------- | ------------------------- |
| 单条聊天消息   | 2KB 到 8KB                |
| 单条通知消息   | 4KB 到 16KB               |
| 单条 ACK 消息  | 1KB 到 4KB                |
| 单条附件元数据 | 4KB 到 16KB               |
| STOMP 帧上限   | 64KB 到 256KB，按业务调整 |
| 文件内容       | 不允许通过 WebSocket 传输 |

消息大小限制处理策略：

| 场景             | 处理                     |
| ---------------- | ------------------------ |
| 内容超长         | 返回参数错误             |
| JSON Body 过大   | 拒绝处理并记录安全日志   |
| 高频大消息       | 触发风控                 |
| 附件内容直接传输 | 拒绝，提示使用 HTTP 上传 |
| 管理端大广播     | 限制内容长度和附件数量   |

消息大小限制建议如下：

| 建议                 | 说明                       |
| -------------------- | -------------------------- |
| 前后端同时限制       | 前端限制输入，后端强制校验 |
| 按类型设置上限       | 聊天、通知、附件元数据不同 |
| 大文件走 HTTP        | WebSocket 不传输文件内容   |
| 记录大消息来源       | 高频大消息可能是攻击       |
| 避免日志打印完整消息 | 大消息日志只记录长度和摘要 |

### 异常行为识别

异常行为识别用于发现可能的攻击、刷屏、越权尝试、异常客户端、脚本行为或前端 Bug。识别结果可以触发告警、限流、临时封禁、强制下线或人工审核。

异常行为类型如下：

| 行为         | 说明                           |
| ------------ | ------------------------------ |
| 高频连接     | 短时间大量 CONNECT             |
| 高频认证失败 | Token 错误或暴力尝试           |
| 高频非法订阅 | 扫描 admin、group、tenant 路径 |
| 高频发送     | 刷消息或压测攻击               |
| 重复内容     | 群聊刷屏                       |
| 大消息攻击   | 发送异常大 Body                |
| XSS 内容     | 消息中包含脚本或危险链接       |
| 越权操作     | 操作他人通知、群组或管理端路径 |
| 高频错误     | 同一 Session 持续触发异常      |

风险计分模型示例：

| 行为           | 分值 |
| -------------- | ---- |
| 认证失败       | +2   |
| 非法订阅       | +5   |
| 非法 SEND 地址 | +5   |
| 发送频率超限   | +3   |
| 消息过大       | +4   |
| XSS 风险内容   | +8   |
| 管理端越权     | +10  |

Redis Key：

```text
ws:risk:score:user:{userId}
ws:risk:score:ip:{ip}
ws:risk:event:user:{userId}
```

异常行为识别服务示例。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsRiskControlService.java`

```java
package io.github.atengk.websocket.service;

/**
 * WebSocket 风控服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsRiskControlService {

    /**
     * 记录用户风险行为
     *
     * @param userId    用户 ID
     * @param eventType 风险事件类型
     * @param score     风险分值
     */
    void recordUserRisk(String userId, String eventType, int score);

    /**
     * 记录 IP 风险行为
     *
     * @param ip        客户端 IP
     * @param eventType 风险事件类型
     * @param score     风险分值
     */
    void recordIpRisk(String ip, String eventType, int score);

    /**
     * 判断用户是否高风险
     *
     * @param userId 用户 ID
     * @return 是否高风险
     */
    boolean isHighRiskUser(String userId);

    /**
     * 判断 IP 是否高风险
     *
     * @param ip 客户端 IP
     * @return 是否高风险
     */
    boolean isHighRiskIp(String ip);
}
```

Redis 实现示例：

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/RedisWsRiskControlServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.WsRiskControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis WebSocket 风控服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisWsRiskControlServiceImpl implements WsRiskControlService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final int HIGH_RISK_THRESHOLD = 30;

    /**
     * 记录用户风险行为
     *
     * @param userId    用户 ID
     * @param eventType 风险事件类型
     * @param score     风险分值
     */
    @Override
    public void recordUserRisk(String userId, String eventType, int score) {
        if (StrUtil.isBlank(userId) || score <= 0) {
            return;
        }

        String key = StrUtil.format("ws:risk:score:user:{}", userId);
        Long total = stringRedisTemplate.opsForValue().increment(key, score);
        stringRedisTemplate.expire(key, Duration.ofMinutes(30));

        log.warn("记录用户 WebSocket 风险行为，userId：{}，eventType：{}，score：{}，total：{}",
                userId, eventType, score, total);
    }

    /**
     * 记录 IP 风险行为
     *
     * @param ip        客户端 IP
     * @param eventType 风险事件类型
     * @param score     风险分值
     */
    @Override
    public void recordIpRisk(String ip, String eventType, int score) {
        if (StrUtil.isBlank(ip) || score <= 0) {
            return;
        }

        String key = StrUtil.format("ws:risk:score:ip:{}", ip);
        Long total = stringRedisTemplate.opsForValue().increment(key, score);
        stringRedisTemplate.expire(key, Duration.ofMinutes(30));

        log.warn("记录 IP WebSocket 风险行为，ip：{}，eventType：{}，score：{}，total：{}",
                ip, eventType, score, total);
    }

    /**
     * 判断用户是否高风险
     *
     * @param userId 用户 ID
     * @return 是否高风险
     */
    @Override
    public boolean isHighRiskUser(String userId) {
        if (StrUtil.isBlank(userId)) {
            return false;
        }

        String value = stringRedisTemplate.opsForValue().get(StrUtil.format("ws:risk:score:user:{}", userId));
        return StrUtil.isNotBlank(value) && Integer.parseInt(value) >= HIGH_RISK_THRESHOLD;
    }

    /**
     * 判断 IP 是否高风险
     *
     * @param ip 客户端 IP
     * @return 是否高风险
     */
    @Override
    public boolean isHighRiskIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }

        String value = stringRedisTemplate.opsForValue().get(StrUtil.format("ws:risk:score:ip:{}", ip));
        return StrUtil.isNotBlank(value) && Integer.parseInt(value) >= HIGH_RISK_THRESHOLD;
    }
}
```

### 自动封禁策略

自动封禁策略用于在用户或 IP 达到风险阈值后，自动执行临时封禁、发送限制、强制下线或加入黑名单。自动封禁必须谨慎，避免误伤正常用户，建议先采用短期封禁和分级处置。

分级处置策略如下：

| 风险等级 | 条件示例                   | 处理方式            |
| -------- | -------------------------- | ------------------- |
| 低风险   | 少量参数错误               | 返回错误提示        |
| 中风险   | 多次频率超限               | 临时限制发送        |
| 高风险   | 多次非法订阅、XSS 内容     | 强制下线，临时封禁  |
| 严重风险 | 攻击管理端路径、爆破 Token | IP 黑名单，安全告警 |

自动封禁 Redis Key：

```text
ws:blacklist:user:temp:{userId}
ws:blacklist:ip:temp:{ip}
ws:blacklist:send:user:{userId}
```

自动封禁服务示例。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsAutoBanService.java`

```java
package io.github.atengk.websocket.service;

import java.time.Duration;

/**
 * WebSocket 自动封禁服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsAutoBanService {

    /**
     * 临时封禁用户连接
     *
     * @param userId   用户 ID
     * @param duration 封禁时长
     * @param reason   原因
     */
    void banUser(String userId, Duration duration, String reason);

    /**
     * 临时封禁 IP
     *
     * @param ip       客户端 IP
     * @param duration 封禁时长
     * @param reason   原因
     */
    void banIp(String ip, Duration duration, String reason);

    /**
     * 限制用户发送消息
     *
     * @param userId   用户 ID
     * @param duration 限制时长
     * @param reason   原因
     */
    void banUserSend(String userId, Duration duration, String reason);
}
```

实现示例：

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/RedisWsAutoBanServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.WsAutoBanService;
import io.github.atengk.websocket.service.WsSessionControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis WebSocket 自动封禁服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisWsAutoBanServiceImpl implements WsAutoBanService {

    private final StringRedisTemplate stringRedisTemplate;

    private final WsSessionControlService wsSessionControlService;

    /**
     * 临时封禁用户连接
     *
     * @param userId   用户 ID
     * @param duration 封禁时长
     * @param reason   原因
     */
    @Override
    public void banUser(String userId, Duration duration, String reason) {
        if (StrUtil.isBlank(userId) || duration == null || duration.isNegative()) {
            return;
        }

        String key = StrUtil.format("ws:blacklist:user:temp:{}", userId);
        stringRedisTemplate.opsForValue().set(key, StrUtil.blankToDefault(reason, "风险行为触发临时封禁"), duration);
        wsSessionControlService.kickUser(userId, "账号存在异常行为，已临时下线");

        log.warn("用户已被自动临时封禁，userId：{}，duration：{}秒，reason：{}", userId, duration.toSeconds(), reason);
    }

    /**
     * 临时封禁 IP
     *
     * @param ip       客户端 IP
     * @param duration 封禁时长
     * @param reason   原因
     */
    @Override
    public void banIp(String ip, Duration duration, String reason) {
        if (StrUtil.isBlank(ip) || duration == null || duration.isNegative()) {
            return;
        }

        String key = StrUtil.format("ws:blacklist:ip:temp:{}", ip);
        stringRedisTemplate.opsForValue().set(key, StrUtil.blankToDefault(reason, "风险行为触发临时封禁"), duration);

        log.warn("IP 已被自动临时封禁，ip：{}，duration：{}秒，reason：{}", ip, duration.toSeconds(), reason);
    }

    /**
     * 限制用户发送消息
     *
     * @param userId   用户 ID
     * @param duration 限制时长
     * @param reason   原因
     */
    @Override
    public void banUserSend(String userId, Duration duration, String reason) {
        if (StrUtil.isBlank(userId) || duration == null || duration.isNegative()) {
            return;
        }

        String key = StrUtil.format("ws:blacklist:send:user:{}", userId);
        stringRedisTemplate.opsForValue().set(key, StrUtil.blankToDefault(reason, "发送频率异常"), duration);

        log.warn("用户发送能力已被临时限制，userId：{}，duration：{}秒，reason：{}", userId, duration.toSeconds(), reason);
    }
}
```

自动封禁建议如下：

| 建议             | 说明                                |
| ---------------- | ----------------------------------- |
| 优先临时封禁     | 自动策略避免永久封禁                |
| 分级处理         | 先限流，再禁发，再踢下线，再封 IP   |
| 所有封禁记录审计 | 记录触发规则、用户、IP、时间        |
| 支持人工解除     | 管理端可解除误封                    |
| 封禁时长递增     | 多次违规逐步延长                    |
| 关键规则可配置   | 阈值、分值、时长应支持配置          |
| 高风险立即告警   | 管理端越权、XSS、爆破应触发安全告警 |

## 性能优化

性能优化用于提升 WebSocket STOMP 在高连接数、高消息量、多主题订阅、群组广播和长连接场景下的稳定性。优化重点不是单纯提高 QPS，而是控制连接资源、线程资源、内存资源、消息大小、推送频率、慢消费者和异常流量，避免实时推送能力影响核心业务链路。

### 连接数优化

连接数优化的目标是让服务端能够稳定承载预期数量的 WebSocket 长连接。WebSocket 连接会长期占用内存、Session 状态、心跳资源和文件描述符，因此需要从应用、容器、网关、操作系统和部署架构多个层面共同优化。

连接数优化建议如下：

| 优化项                  | 说明                                   |
| ----------------------- | -------------------------------------- |
| 控制单用户连接数        | 避免同一用户多标签页、多设备无限连接   |
| 启用连接频率限制        | 防止客户端 Bug 或攻击导致频繁重连      |
| 使用 Redis 管理在线状态 | 集群下不要依赖单实例内存统计           |
| 网关超时大于心跳间隔    | 避免网关误断开正常连接                 |
| 合理设置心跳            | 心跳过短会放大网络和 CPU 压力          |
| 容器资源隔离            | WebSocket 服务最好与重业务服务分开部署 |
| 操作系统调优            | 提高文件描述符、连接队列和端口范围     |

连接数容量需要压测确认，不能只按理论值估算。影响连接数的主要因素包括每连接内存占用、心跳频率、订阅数量、消息频率、客户端网络质量和出站缓冲大小。

建议基准目标如下：

| 场景           | 单实例连接数建议                 |
| -------------- | -------------------------------- |
| 开发环境       | 100 到 1000                      |
| 普通管理后台   | 1000 到 5000                     |
| 内部通知系统   | 5000 到 20000                    |
| 聊天或高频推送 | 必须压测后确定                   |
| 大规模系统     | 使用集群、Broker、Redis 状态同步 |

连接数优化不能只提高 `maxThreads`。WebSocket 长连接的核心瓶颈通常是内存、出站消息堆积、心跳开销、网关连接数和 Broker 吞吐。

### 线程池配置

线程池配置用于隔离入站消息处理、出站消息推送、业务异步处理、定时任务和后台补偿任务。不要让 WebSocket 推送直接占用核心业务线程池，否则在推送堆积时会影响 HTTP 接口、数据库事务和业务主流程。

建议线程池拆分如下：

| 线程池         | 职责                                                 |
| -------------- | ---------------------------------------------------- |
| 入站通道线程池 | 处理客户端 `SEND`、`SUBSCRIBE`、`CONNECT` 等入站消息 |
| 出站通道线程池 | 处理服务端向客户端推送的消息                         |
| 业务处理线程池 | 处理异步业务事件、通知生成、消息转换                 |
| 推送线程池     | 执行异步推送、批量推送、跨实例推送                   |
| 定时任务线程池 | 执行心跳扫描、失败重试、离线补偿、看板推送           |

配置建议如下：

| 参数                       | 建议                                     |
| -------------------------- | ---------------------------------------- |
| `corePoolSize`             | 根据 CPU、业务复杂度和消息频率设置       |
| `maxPoolSize`              | 不要无限放大，避免线程过多导致上下文切换 |
| `queueCapacity`            | 设置有界队列，避免内存被撑爆             |
| `threadNamePrefix`         | 必须可识别，方便日志和监控               |
| `rejectedExecutionHandler` | 明确拒绝策略，不能使用默认静默失败       |
| 异常处理                   | 异步异常必须记录日志和指标               |

线程池不是越大越好。对于 WebSocket 服务，过大的线程池会导致上下文切换增加、GC 压力变大、消息乱序风险增加和故障恢复时间变长。

### 消息序列化优化

消息序列化优化用于降低 JSON 编码、解码、对象转换和网络传输带来的开销。WebSocket 高频推送场景中，消息结构设计不合理会直接增加 CPU、内存和网络带宽压力。

序列化优化建议如下：

| 优化项                 | 说明                           |
| ---------------------- | ------------------------------ |
| 使用统一消息外壳       | 前端可以统一解析，减少兼容逻辑 |
| 字段保持精简           | 不推送前端不需要的字段         |
| 避免深层嵌套           | 降低 JSON 解析成本             |
| 避免大对象直接推送     | 大对象通过 HTTP 查询详情       |
| 时间字段用时间戳       | 比复杂日期字符串更轻量         |
| 枚举使用稳定字符串     | 兼顾可读性和兼容性             |
| 空字段按需忽略         | 减少消息体大小                 |
| 不在消息体携带敏感数据 | 避免泄露和脱敏成本             |

推荐消息结构保持稳定：

```json
{
  "messageId": "1909550919275922101",
  "type": "USER_NOTICE",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "noticeId": "notice-100001",
    "title": "审批通知",
    "url": "/approval/detail/100001"
  }
}
```

不建议推送如下内容：

| 内容            | 原因                         |
| --------------- | ---------------------------- |
| 完整用户对象    | 字段多，容易泄露敏感信息     |
| 完整订单详情    | 应通过 HTTP 查询             |
| 文件内容 Base64 | 消息体过大                   |
| 完整异常堆栈    | 泄露内部实现                 |
| 大量列表数据    | WebSocket 不适合传输分页数据 |

### 大消息处理

大消息处理用于避免单条 STOMP 消息过大导致内存占用、网络阻塞、前端卡顿、Broker 压力和慢消费者问题。WebSocket 适合实时小消息，不适合传输大文件、大列表、大对象和长文本。

大消息处理原则如下：

| 类型       | 推荐处理                                         |
| ---------- | ------------------------------------------------ |
| 大文件     | 走 HTTP 或对象存储上传，WebSocket 只推送 fileId  |
| 大图片     | 走文件服务，WebSocket 推送缩略图地址和文件元数据 |
| 大列表     | 走 HTTP 分页查询，WebSocket 只推送变更事件       |
| 大文本     | 限制长度或保存后推送摘要                         |
| 大对象详情 | WebSocket 推送 bizId，前端再查详情               |
| 大批量通知 | 分批推送或广播摘要                               |

附件消息示例：

```json
{
  "messageId": "1909550919275922102",
  "type": "PRIVATE_CHAT",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "chatId": "private-10001-10002",
    "contentType": "FILE",
    "content": "合同文件.pdf",
    "attachment": {
      "fileId": "file-100001",
      "fileName": "合同文件.pdf",
      "fileSize": 1048576,
      "url": "/api/files/file-100001/download"
    }
  }
}
```

大消息限制建议如下：

| 消息类型      | 建议上限                |
| ------------- | ----------------------- |
| ACK 消息      | 1KB 到 4KB              |
| 聊天文本      | 2KB 到 8KB              |
| 通知消息      | 4KB 到 16KB             |
| 附件元数据    | 4KB 到 16KB             |
| 普通 STOMP 帧 | 64KB 到 256KB           |
| 文件内容      | 禁止通过 WebSocket 传输 |

### 批量推送优化

批量推送优化用于处理一次向大量用户发送通知、待办、公告、告警或任务状态的场景。批量推送如果直接循环同步发送，容易造成出站通道阻塞、线程池堆积、Redis 压力和 Broker 压力。

批量推送优化建议如下：

| 优化项           | 说明                           |
| ---------------- | ------------------------------ |
| 分批处理         | 每批 100 到 500 人，按压测调整 |
| 异步执行         | 批量推送不阻塞业务主线程       |
| 优先使用广播     | 全站公告不应逐用户推送         |
| 在线用户实时推送 | 离线用户只保存通知记录         |
| 推送失败记录     | 批量失败需要可重试             |
| 推送节流         | 防止短时间大量消息打爆客户端   |
| 合并未读数       | 不要每条通知都单独推未读数     |

批量推送流程建议如下：

```text
1. 创建批量通知任务
2. 分页查询目标用户
3. 批量写入通知记录
4. 过滤在线用户
5. 分批推送在线用户
6. 更新未读数缓存
7. 记录成功数、失败数、耗时
8. 失败批次进入重试任务
```

批量推送不要在数据库事务内同步执行。正确方式是事务提交后投递任务，由异步线程池、MQ 或调度任务执行实际推送。

### 广播推送优化

广播推送优化用于控制全站公告、系统通知、管理端看板、公共告警等大范围推送的性能影响。广播影响范围大，必须控制频率、内容大小和目标范围。

广播优化建议如下：

| 优化项            | 说明                         |
| ----------------- | ---------------------------- |
| 控制广播范围      | 优先按租户、角色、群组拆分   |
| 减少全站广播      | 全站广播只用于真正公共消息   |
| 推送摘要而非详情  | 前端点击后再查详情           |
| 看板数据节流      | 3 到 10 秒推送一次即可       |
| 告警聚合          | 同类告警短时间内合并         |
| 避免重复广播      | 使用 eventId 做幂等          |
| 使用 Broker Relay | 多实例下交给外部 Broker 分发 |

广播推送路径建议如下：

| 场景       | 路径                              |
| ---------- | --------------------------------- |
| 全站公告   | `/topic/system/notice`            |
| 租户公告   | `/topic/tenant/{tenantId}/notice` |
| 管理端告警 | `/topic/admin/alarm`              |
| 管理端看板 | `/topic/admin/dashboard/message`  |
| 业务看板   | `/topic/dashboard/{bizType}`      |

广播推送不适合携带用户私有数据。包含用户隐私、审批详情、订单详情的数据，应使用 `/user/queue/**` 点对点推送或 HTTP 查询。

### 群组推送优化

群组推送优化用于提升群聊、房间、项目空间、组织频道等多人场景的消息分发效率。群组消息的复杂度取决于群规模、消息频率、在线成员数和多实例部署方式。

群组推送方案对比如下：

| 方案                 | 说明                             | 适用场景               |
| -------------------- | -------------------------------- | ---------------------- |
| Topic 广播           | 向 `/topic/group/{groupId}` 推送 | 群成员订阅清晰，推荐   |
| 按用户逐个推送       | 遍历群成员发送用户队列           | 小群或需要用户级差异化 |
| Broker Relay         | 由外部 Broker 处理群组订阅分发   | 多实例生产推荐         |
| Redis Pub/Sub 跨实例 | 所有实例收到后本地推送           | 中小集群               |
| Kafka 事件桥接       | 群消息事件进入 Kafka 后消费推送  | 事件驱动架构           |

群组推送优化建议如下：

| 优化项               | 说明                       |
| -------------------- | -------------------------- |
| 大群优先 Topic       | 不要逐个用户推送           |
| 群消息先落库         | 支持历史查询和断线补偿     |
| 前端按 sequence 排序 | 防止异步导致乱序           |
| 高频群做限流         | 防止刷屏和出站堆积         |
| 群成员缓存           | 订阅和发送鉴权走 Redis     |
| 在线人数节流推送     | 房间人数不要每次变化都广播 |
| 大群已读弱化         | 不建议逐人实时已读         |

### 心跳间隔优化

心跳间隔优化用于在连接存活检测和系统资源消耗之间取得平衡。心跳过短会增加网络包、CPU 唤醒、网关压力和移动端耗电；心跳过长会导致异常断开感知不及时。

建议心跳配置如下：

| 场景           | 建议心跳                       |
| -------------- | ------------------------------ |
| 本地开发       | 10 到 30 秒                    |
| 管理后台       | 15 到 30 秒                    |
| 聊天系统       | 10 到 20 秒                    |
| 移动端弱网     | 30 到 60 秒                    |
| 大连接数系统   | 压测后确定，通常不宜低于 20 秒 |
| 内网低延迟系统 | 可适当缩短                     |

服务端配置示例：

```java
registry.enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[]{10000, 10000})
        .setTaskScheduler(webSocketHeartbeatTaskScheduler());
```

心跳优化注意事项如下：

| 注意项                 | 说明                                |
| ---------------------- | ----------------------------------- |
| 网关超时必须更长       | `proxy_read_timeout` 应大于心跳间隔 |
| Redis Session TTL 更长 | TTL 至少是心跳间隔 3 到 5 倍        |
| 移动端放宽心跳         | 避免后台或弱网频繁断开              |
| 心跳不是业务消息       | 不要记录心跳日志到数据库            |
| 心跳异常要清理 Session | 避免假在线                          |

### 内存占用优化

内存占用优化用于控制长连接、Session 注册表、订阅关系、消息缓存、出站队列、离线队列和序列化对象带来的 JVM 内存压力。

内存占用来源如下：

| 来源              | 说明                        |
| ----------------- | --------------------------- |
| WebSocket Session | 每个连接都有对象和缓冲区    |
| 订阅关系          | 每个连接可能订阅多个 Topic  |
| 出站消息队列      | 慢消费者会导致消息堆积      |
| 本地缓存          | 在线状态、幂等、群组关系    |
| 大消息对象        | 大 JSON、大 DTO、附件元数据 |
| 日志对象          | 高频日志和异常堆栈          |
| 线程池队列        | 无界队列会吞掉大量内存      |

优化建议如下：

| 优化项             | 说明                         |
| ------------------ | ---------------------------- |
| 使用有界队列       | 所有线程池队列必须有容量上限 |
| 限制单连接订阅数   | 防止一个连接订阅过多 Topic   |
| 限制消息大小       | 防止大对象进入内存           |
| 避免本地无限缓存   | 幂等和在线状态优先放 Redis   |
| 慢消费者断开或降级 | 防止出站队列持续堆积         |
| 日志降频           | 高频消息不要每条都 info      |
| 定期清理 Session   | 断开、超时、异常都要清理     |

### GC 风险控制

GC 风险控制用于避免高频消息、大量短生命周期对象、出站队列堆积和大对象分配引发频繁 GC 或 Full GC。WebSocket 系统一旦出现 GC 停顿，可能导致心跳超时、连接批量断开和消息延迟升高。

GC 风险来源如下：

| 风险             | 说明                           |
| ---------------- | ------------------------------ |
| 高频 JSON 序列化 | 大量临时对象                   |
| 大消息体         | 进入老年代或大对象区           |
| 无界队列         | 消息堆积导致堆内存膨胀         |
| 日志过多         | 字符串对象和异步日志队列堆积   |
| 批量推送瞬时峰值 | 短时间创建大量消息对象         |
| 连接泄漏         | Session 未清理导致对象长期存活 |

GC 控制建议如下：

| 建议              | 说明                     |
| ----------------- | ------------------------ |
| 限制消息大小      | 从源头减少大对象         |
| 控制队列容量      | 防止堆积进入老年代       |
| 复用消息模板      | 通知模板化，减少重复构造 |
| 批量任务分片      | 避免一次构造超大集合     |
| 使用 G1 或 ZGC    | 按 JDK 和延迟要求选择    |
| 开启 GC 日志      | 生产环境必须可观测       |
| 压测观察 P99 延迟 | 不只看平均吞吐           |

JVM 参数需要结合实际压测调整，不建议直接复制固定值。对于 Spring Boot 3 和 JDK 17 以上，普通中低延迟服务可以优先使用 G1；连接数极大且延迟敏感时再评估 ZGC。

### 背压处理

背压处理用于在消息生产速度大于消费速度时保护系统，避免队列无限增长、内存耗尽、Broker 积压和客户端卡死。WebSocket 推送系统必须有明确的“满了怎么办”策略。

背压触发场景如下：

| 场景         | 说明                     |
| ------------ | ------------------------ |
| 客户端网络慢 | 出站消息发送不出去       |
| 前端处理慢   | 消息堆积在浏览器或代理   |
| Broker 积压  | 外部消息代理处理不过来   |
| 推送线程池满 | 后端推送任务堆积         |
| 批量广播过大 | 瞬时消息量超过容量       |
| 下游服务慢   | 查询、序列化、持久化变慢 |

背压策略如下：

| 策略       | 说明                           |
| ---------- | ------------------------------ |
| 限流       | 控制消息进入速度               |
| 降级       | 暂停非关键推送                 |
| 丢弃旧快照 | 看板类数据只保留最新           |
| 合并消息   | 未读数、任务进度、告警聚合     |
| 拒绝任务   | 线程池队列满时拒绝或调用方执行 |
| 断开慢连接 | 慢消费者持续堆积时断开         |
| 失败入库   | 关键消息进入失败表或离线表     |

背压处理不能依赖单点策略。对于关键消息，不能简单丢弃；对于看板快照、在线人数、任务中间进度，可以覆盖旧值或降频。

### 慢消费者处理

慢消费者是指客户端接收或处理消息速度明显低于服务端发送速度，导致出站缓冲区、Broker 队列或前端消息队列堆积。慢消费者会拖慢整体推送能力，严重时会影响同实例其他连接。

慢消费者识别指标如下：

| 指标                | 说明                     |
| ------------------- | ------------------------ |
| 出站队列长度        | 某连接或某通道消息堆积   |
| 发送延迟            | 从生成到发送耗时升高     |
| ACK 超时            | 客户端长时间不确认       |
| 心跳异常            | 客户端响应变慢或断开     |
| 浏览器处理慢        | 前端消息队列持续增加     |
| Broker 慢消费者告警 | 外部 Broker 识别慢消费者 |

处理策略如下：

| 策略           | 说明                         |
| -------------- | ---------------------------- |
| 降频推送       | 对看板、进度类消息降低频率   |
| 合并消息       | 多条未读数变更合并为一条     |
| 丢弃旧快照     | 只保留最新看板数据           |
| 限制订阅数量   | 防止单连接订阅过多 Topic     |
| 断开连接       | 长期慢消费者主动断开         |
| 提醒客户端补偿 | 重连后通过 HTTP 查询最新状态 |
| 记录审计       | 管理端查看慢连接和异常设备   |

慢消费者处理要区分消息类型。聊天、通知、审批等关键消息不能随意丢弃；看板快照、在线人数、任务中间进度可以丢弃旧值，只推最新状态。

## 线程池与异步处理

线程池与异步处理用于将 WebSocket 入站、出站、业务计算、消息推送、定时任务和失败补偿隔离开，避免某类任务阻塞整个服务。生产环境必须使用有界队列、明确拒绝策略、异常处理和监控指标。

### 入站通道线程池

入站通道线程池用于处理客户端发往服务端的 STOMP 消息，包括 `CONNECT`、`SUBSCRIBE`、`SEND`、`DISCONNECT`。入站线程池中的逻辑应尽量轻量，复杂业务应转交业务线程池或消息队列。

入站通道适合处理：

| 操作         | 说明                     |
| ------------ | ------------------------ |
| Token 解析   | `CONNECT` 阶段认证       |
| 路径鉴权     | `SUBSCRIBE`、`SEND` 阶段 |
| 参数初步校验 | 目标地址、Header、限流   |
| Session 刷新 | 更新最后活跃时间         |
| 风控记录     | 记录异常行为             |

不适合在入站线程池中直接执行：

| 操作           | 原因                       |
| -------------- | -------------------------- |
| 大量数据库查询 | 阻塞入站消息               |
| 大批量推送     | 占用入站线程               |
| 长耗时业务处理 | 增加消息延迟               |
| 文件处理       | WebSocket 不应处理文件内容 |
| 批量通知生成   | 应进入异步任务             |

### 出站通道线程池

出站通道线程池用于处理服务端向客户端推送的消息。出站线程池容易受到慢消费者、批量推送和广播消息影响，因此必须设置有界队列和监控指标。

出站通道优化建议如下：

| 配置项     | 建议                             |
| ---------- | -------------------------------- |
| 核心线程数 | 根据 CPU 和消息频率设置          |
| 最大线程数 | 不宜过大，避免上下文切换         |
| 队列容量   | 必须有界                         |
| 慢消费者   | 结合队列长度和 ACK 超时处理      |
| 日志       | 不要每条消息都打印 info          |
| 监控       | 关注活跃线程、队列长度、拒绝次数 |

下面的配置类用于配置入站和出站通道线程池，适合补充到 WebSocket 配置中。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsChannelThreadPoolConfig.java`

```java
package io.github.atengk.websocket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 入站与出站通道线程池配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
@RequiredArgsConstructor
public class WsChannelThreadPoolConfig implements WebSocketMessageBrokerConfigurer {

    private final WsThreadPoolProperties threadPoolProperties;

    /**
     * 配置客户端入站通道线程池
     *
     * @param registration 通道注册器
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        WsThreadPoolProperties.Channel inbound = threadPoolProperties.getInbound();
        registration.taskExecutor()
                .corePoolSize(inbound.getCorePoolSize())
                .maxPoolSize(inbound.getMaxPoolSize())
                .queueCapacity(inbound.getQueueCapacity())
                .keepAliveSeconds(inbound.getKeepAliveSeconds());
    }

    /**
     * 配置客户端出站通道线程池
     *
     * @param registration 通道注册器
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        WsThreadPoolProperties.Channel outbound = threadPoolProperties.getOutbound();
        registration.taskExecutor()
                .corePoolSize(outbound.getCorePoolSize())
                .maxPoolSize(outbound.getMaxPoolSize())
                .queueCapacity(outbound.getQueueCapacity())
                .keepAliveSeconds(outbound.getKeepAliveSeconds());
    }
}
```

对应的线程池配置属性如下。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsThreadPoolProperties.java`

```java
package io.github.atengk.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 线程池配置属性
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@ConfigurationProperties(prefix = "app.websocket.thread-pool")
public class WsThreadPoolProperties {

    /**
     * 入站通道线程池
     */
    private Channel inbound = new Channel();

    /**
     * 出站通道线程池
     */
    private Channel outbound = new Channel();

    /**
     * 业务处理线程池
     */
    private Executor business = new Executor();

    /**
     * 推送线程池
     */
    private Executor push = new Executor();

    /**
     * 线程池通道配置
     */
    @Data
    public static class Channel {

        /**
         * 核心线程数
         */
        private int corePoolSize = 4;

        /**
         * 最大线程数
         */
        private int maxPoolSize = 16;

        /**
         * 队列容量
         */
        private int queueCapacity = 1000;

        /**
         * 空闲线程存活时间，单位秒
         */
        private int keepAliveSeconds = 60;
    }

    /**
     * 普通线程池配置
     */
    @Data
    public static class Executor {

        /**
         * 核心线程数
         */
        private int corePoolSize = 4;

        /**
         * 最大线程数
         */
        private int maxPoolSize = 16;

        /**
         * 队列容量
         */
        private int queueCapacity = 1000;

        /**
         * 空闲线程存活时间，单位秒
         */
        private int keepAliveSeconds = 60;

        /**
         * 线程名前缀
         */
        private String threadNamePrefix = "ws-exec-";
    }
}
```

配置文件示例如下。

```yaml
app:
  websocket:
    thread-pool:
      # 客户端入站消息处理线程池
      inbound:
        core-pool-size: 4
        max-pool-size: 16
        queue-capacity: 1000
        keep-alive-seconds: 60

      # 服务端出站消息推送线程池
      outbound:
        core-pool-size: 4
        max-pool-size: 32
        queue-capacity: 2000
        keep-alive-seconds: 60

      # 业务异步处理线程池
      business:
        core-pool-size: 4
        max-pool-size: 16
        queue-capacity: 1000
        keep-alive-seconds: 60
        thread-name-prefix: ws-business-

      # WebSocket 推送线程池
      push:
        core-pool-size: 4
        max-pool-size: 32
        queue-capacity: 3000
        keep-alive-seconds: 60
        thread-name-prefix: ws-push-
```

### 业务处理线程池

业务处理线程池用于执行通知生成、业务事件转换、消息持久化后的异步处理、失败补偿、离线消息处理等任务。业务处理线程池应与入站通道线程池分离，避免复杂业务阻塞 STOMP 通道。

适合放入业务线程池的任务如下：

| 任务         | 说明                             |
| ------------ | -------------------------------- |
| 业务事件转换 | 将订单、审批、任务事件转换为通知 |
| 批量通知生成 | 分批写入通知记录                 |
| 离线消息补偿 | 用户重连后异步补偿               |
| 失败消息重试 | 定时扫描失败表并重试             |
| 告警聚合     | 多条告警合并后推送               |
| 未读数重建   | Redis 异常后从数据库重建         |

不建议把数据库事务内的核心写操作盲目异步化。异步处理适合非核心链路，核心状态仍应先可靠提交。

### 推送线程池

推送线程池用于执行 `SimpMessagingTemplate` 推送、批量用户推送、跨实例推送、Redis Pub/Sub 事件转发等操作。推送线程池应设置比业务线程池更大的队列容量，但不能无界。

下面的配置类用于定义业务处理线程池和推送线程池。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsExecutorConfig.java`

```java
package io.github.atengk.websocket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * WebSocket 异步线程池配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@EnableAsync
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(WsThreadPoolProperties.class)
public class WsExecutorConfig {

    private final WsThreadPoolProperties threadPoolProperties;

    /**
     * WebSocket 业务处理线程池
     *
     * @return 业务处理线程池
     */
    @Bean("wsBusinessExecutor")
    public Executor wsBusinessExecutor() {
        WsThreadPoolProperties.Executor config = threadPoolProperties.getBusiness();
        return buildExecutor(config, "ws-business-");
    }

    /**
     * WebSocket 推送线程池
     *
     * @return 推送线程池
     */
    @Bean("wsPushExecutor")
    public Executor wsPushExecutor() {
        WsThreadPoolProperties.Executor config = threadPoolProperties.getPush();
        return buildExecutor(config, "ws-push-");
    }

    /**
     * 构建线程池
     *
     * @param config              线程池配置
     * @param defaultThreadPrefix 默认线程名前缀
     * @return 线程池
     */
    private ThreadPoolTaskExecutor buildExecutor(WsThreadPoolProperties.Executor config, String defaultThreadPrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.setThreadNamePrefix(config.getThreadNamePrefix() == null ? defaultThreadPrefix : config.getThreadNamePrefix());
        executor.setRejectedExecutionHandler((runnable, poolExecutor) -> {
            log.warn("WebSocket 线程池队列已满，threadPrefix：{}，activeCount：{}，queueSize：{}",
                    executor.getThreadNamePrefix(),
                    poolExecutor.getActiveCount(),
                    poolExecutor.getQueue().size());
            new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(runnable, poolExecutor);
        });
        executor.initialize();
        return executor;
    }
}
```

使用示例：

```java
@Async("wsPushExecutor")
public void pushNoticeAsync(String userId, Object payload) {
    wsPushService.sendToUser(userId, "/queue/notice", payload);
}
```

### 定时任务线程池

定时任务线程池用于执行心跳超时扫描、失败消息重试、离线消息补偿、过期消息清理、管理端看板推送和归档任务。默认单线程定时任务在任务较多时容易互相阻塞，生产环境建议显式配置。

定时任务线程池示例如下。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsScheduleConfig.java`

```java
package io.github.atengk.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * WebSocket 定时任务线程池配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
@EnableScheduling
public class WsScheduleConfig implements SchedulingConfigurer {

    /**
     * 配置定时任务调度器
     *
     * @param taskRegistrar 定时任务注册器
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("ws-schedule-");
        scheduler.setErrorHandler(throwable -> log.error("WebSocket 定时任务执行异常", throwable));
        scheduler.initialize();

        taskRegistrar.setTaskScheduler(scheduler);
    }
}
```

定时任务拆分建议如下：

| 任务             | 建议频率     |
| ---------------- | ------------ |
| Session 超时扫描 | 30 到 60 秒  |
| 失败消息重试     | 10 到 60 秒  |
| 离线消息补偿扫描 | 10 到 60 秒  |
| 管理端看板推送   | 3 到 10 秒   |
| 过期消息清理     | 5 到 30 分钟 |
| 数据归档         | 每天低峰期   |

定时任务应避免大事务和全表扫描。大数据量任务必须分页处理，并使用分布式锁避免多实例重复执行。

### 异步异常处理

异步异常处理用于捕获 `@Async` 方法中的异常。异步方法抛出的异常不会直接返回给调用方，如果不配置处理器，容易出现异常被日志遗漏、任务失败不可见的问题。

下面的配置用于统一处理 WebSocket 异步任务异常。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsAsyncExceptionConfig.java`

```java
package io.github.atengk.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.lang.reflect.Method;

/**
 * WebSocket 异步异常处理配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class WsAsyncExceptionConfig implements AsyncConfigurer {

    /**
     * 获取异步异常处理器
     *
     * @return 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::handleAsyncException;
    }

    /**
     * 处理异步异常
     *
     * @param throwable 异常
     * @param method    方法
     * @param objects   参数
     */
    private void handleAsyncException(Throwable throwable, Method method, Object... objects) {
        log.error("WebSocket 异步任务执行异常，method：{}，paramCount：{}",
                method.getName(),
                objects == null ? 0 : objects.length,
                throwable);
    }
}
```

异步异常处理建议如下：

| 建议                 | 说明                     |
| -------------------- | ------------------------ |
| 异常必须记录日志     | 包含方法名和 traceId     |
| 推送失败要入库       | 关键消息不能只打印异常   |
| 批量任务要记录失败项 | 便于重试                 |
| 异步任务不要吞异常   | 至少记录并上报指标       |
| 关键任务加告警       | 失败率超过阈值通知管理端 |

### 队列容量配置

队列容量配置用于控制线程池任务堆积规模。队列过小会导致频繁拒绝，队列过大会掩盖问题并引发内存膨胀。WebSocket 系统必须使用有界队列。

队列容量建议如下：

| 线程池         | 建议队列容量       |
| -------------- | ------------------ |
| 入站通道线程池 | 500 到 2000        |
| 出站通道线程池 | 1000 到 5000       |
| 业务处理线程池 | 500 到 2000        |
| 推送线程池     | 1000 到 10000      |
| 定时任务线程池 | 通常不使用大队列   |
| 批量通知线程池 | 根据任务量单独配置 |

队列容量设置原则如下：

| 原则                 | 说明                   |
| -------------------- | ---------------------- |
| 不能无界             | 防止 OOM               |
| 不能过大             | 避免延迟持续扩大       |
| 结合拒绝策略         | 队列满时要有降级动作   |
| 结合监控告警         | 队列使用率超过阈值告警 |
| 区分关键和非关键任务 | 非关键推送可丢弃或降频 |

队列容量过大时，系统可能表面没有报错，但消息延迟会持续升高。对于实时系统，延迟过高本身就是故障。

### 拒绝策略配置

拒绝策略配置用于定义线程池队列已满时如何处理新任务。默认拒绝策略可能直接抛异常，如果没有捕获，会导致推送失败不可控。推荐根据任务类型选择不同策略。

常见拒绝策略如下：

| 策略                  | 说明                       | 适用场景               |
| --------------------- | -------------------------- | ---------------------- |
| `CallerRunsPolicy`    | 调用线程执行任务，形成反压 | 关键但不能丢的轻量任务 |
| `AbortPolicy`         | 直接抛异常                 | 需要快速失败并记录     |
| `DiscardPolicy`       | 静默丢弃                   | 不建议使用             |
| `DiscardOldestPolicy` | 丢弃队列最旧任务           | 看板快照类可考虑       |
| 自定义策略            | 记录失败、入库、降级       | 生产推荐               |

拒绝策略建议如下：

| 任务类型   | 推荐策略                   |
| ---------- | -------------------------- |
| 聊天消息   | 失败入库或补偿，不静默丢弃 |
| 用户通知   | 失败记录，后续补偿         |
| 看板快照   | 可丢弃旧任务，只保留最新   |
| 在线状态   | 可合并或降频               |
| 管理端告警 | 记录失败并触发监控         |
| 批量通知   | 任务暂停或分批重试         |

自定义拒绝策略可以记录失败消息：

```java
executor.setRejectedExecutionHandler((runnable, poolExecutor) -> {
    log.warn("WebSocket 推送任务被拒绝，activeCount：{}，queueSize：{}",
            poolExecutor.getActiveCount(),
            poolExecutor.getQueue().size());

    // 关键消息场景可以在这里写入失败消息表或触发降级
    new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(runnable, poolExecutor);
});
```

### 线程池监控

线程池监控用于观察 WebSocket 线程池的活跃线程数、队列长度、完成任务数、拒绝次数和执行耗时。没有线程池监控时，系统往往只能在消息严重延迟或 OOM 后才发现问题。

建议监控指标如下：

| 指标                     | 说明                       |
| ------------------------ | -------------------------- |
| `corePoolSize`           | 核心线程数                 |
| `maxPoolSize`            | 最大线程数                 |
| `activeCount`            | 当前活跃线程数             |
| `poolSize`               | 当前线程数                 |
| `queueSize`              | 当前队列长度               |
| `queueRemainingCapacity` | 队列剩余容量               |
| `completedTaskCount`     | 已完成任务数               |
| `rejectedCount`          | 拒绝任务数，需要自定义统计 |
| `taskExecutionTime`      | 任务执行耗时               |

下面的控制器用于开发和管理端查看线程池状态，生产环境建议接入 Micrometer、Prometheus 和 Grafana。

文件位置：`src/main/java/io/github/atengk/websocket/controller/WsThreadPoolMonitorController.java`

```java
package io.github.atengk.websocket.controller;

import cn.hutool.core.map.MapUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * WebSocket 线程池监控接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequiredArgsConstructor
public class WsThreadPoolMonitorController {

    private final ThreadPoolTaskExecutor wsPushExecutor;

    private final ThreadPoolTaskExecutor wsBusinessExecutor;

    /**
     * 查询 WebSocket 线程池状态
     *
     * @return 线程池状态
     */
    @GetMapping("/api/ws/thread-pools")
    public Map<String, Object> threadPools() {
        return MapUtil.<String, Object>builder()
                .put("push", buildExecutorInfo(wsPushExecutor))
                .put("business", buildExecutorInfo(wsBusinessExecutor))
                .build();
    }

    /**
     * 构建线程池状态
     *
     * @param executor 线程池
     * @return 状态信息
     */
    private Map<String, Object> buildExecutorInfo(ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();

        return MapUtil.<String, Object>builder()
                .put("threadNamePrefix", executor.getThreadNamePrefix())
                .put("corePoolSize", threadPoolExecutor.getCorePoolSize())
                .put("maximumPoolSize", threadPoolExecutor.getMaximumPoolSize())
                .put("poolSize", threadPoolExecutor.getPoolSize())
                .put("activeCount", threadPoolExecutor.getActiveCount())
                .put("queueSize", threadPoolExecutor.getQueue().size())
                .put("queueRemainingCapacity", threadPoolExecutor.getQueue().remainingCapacity())
                .put("completedTaskCount", threadPoolExecutor.getCompletedTaskCount())
                .build();
    }
}
```

验证方式：

```bash
curl -X GET http://localhost:8080/api/ws/thread-pools
```

返回示例：

```json
{
  "push": {
    "threadNamePrefix": "ws-push-",
    "corePoolSize": 4,
    "maximumPoolSize": 32,
    "poolSize": 8,
    "activeCount": 2,
    "queueSize": 15,
    "queueRemainingCapacity": 2985,
    "completedTaskCount": 10240
  },
  "business": {
    "threadNamePrefix": "ws-business-",
    "corePoolSize": 4,
    "maximumPoolSize": 16,
    "poolSize": 4,
    "activeCount": 1,
    "queueSize": 3,
    "queueRemainingCapacity": 997,
    "completedTaskCount": 2048
  }
}
```

线程池告警建议如下：

| 告警项                   | 建议阈值                           |
| ------------------------ | ---------------------------------- |
| 活跃线程数接近最大线程数 | 超过 80%                           |
| 队列使用率               | 超过 70% 告警，超过 90% 严重       |
| 拒绝任务数               | 大于 0 需要关注                    |
| 任务执行耗时             | P95、P99 持续升高                  |
| 出站延迟                 | 持续升高说明慢消费者或 Broker 压力 |
| 定时任务延迟             | 说明任务堆积或线程不足             |

线程池监控应和连接数、消息发送量、失败率、Broker 状态、Redis 延迟、JVM GC 指标一起观察。单独看线程池状态不足以判断 WebSocket 系统健康度。

## 序列化与协议扩展

序列化与协议扩展用于统一 WebSocket STOMP 消息在服务端和客户端之间的传输格式。协议设计要兼顾可读性、兼容性、扩展性和性能，避免后续新增消息类型、字段、Header、版本时破坏已有客户端。

### JSON 消息格式

JSON 是 WebSocket STOMP 项目中最常用的消息格式，适合浏览器、移动端、管理端和后端服务统一解析。建议所有业务消息都使用统一外层结构，业务数据放入 `data` 字段中。

推荐统一消息格式如下：

```json
{
  "messageId": "1909550919275922201",
  "type": "USER_NOTICE",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "traceId": "trace-20260505-000001",
  "data": {
    "noticeId": "notice-100001",
    "title": "审批通知",
    "content": "你有一条新的审批待办",
    "url": "/approval/detail/100001"
  }
}
```

字段建议如下：

| 字段        | 类型     | 说明                                         |
| ----------- | -------- | -------------------------------------------- |
| `messageId` | `String` | 服务端生成的全局唯一消息 ID                  |
| `type`      | `String` | 消息类型，例如 `PRIVATE_CHAT`、`USER_NOTICE` |
| `status`    | `String` | 消息状态，例如 `SUCCESS`、`FAILED`           |
| `version`   | `String` | 协议版本                                     |
| `timestamp` | `Long`   | 服务端时间戳，毫秒                           |
| `traceId`   | `String` | 链路追踪 ID                                  |
| `data`      | `Object` | 业务数据                                     |

JSON 消息设计建议如下：

| 建议                 | 说明                                        |
| -------------------- | ------------------------------------------- |
| 外层字段稳定         | 前端统一解析和分发                          |
| `data` 按业务扩展    | 不同消息类型使用不同业务结构                |
| 时间统一用毫秒时间戳 | 跨端处理更稳定                              |
| 枚举统一用字符串     | 可读性好，便于调试                          |
| 新增字段保持可选     | 避免旧客户端解析失败                        |
| 不返回敏感字段       | Token、Cookie、密码、内部堆栈不能进入消息体 |

### Jackson 配置

Jackson 配置用于控制 WebSocket 消息的 JSON 序列化和反序列化行为。Spring WebSocket 默认会使用消息转换器处理 `@Payload` 和 `SimpMessagingTemplate` 的消息体，但生产项目建议显式配置 ObjectMapper，保证时间、枚举、空字段和未知字段处理方式一致。

推荐配置目标如下：

| 配置项         | 说明                                 |
| -------------- | ------------------------------------ |
| 忽略未知字段   | 前端或旧版本字段不影响反序列化       |
| 时间统一格式   | `LocalDateTime` 输出固定格式或时间戳 |
| 枚举输出字符串 | 避免输出 ordinal                     |
| 空字段处理     | 可按项目需要保留或忽略               |
| JavaTimeModule | 支持 Java 8 时间类型                 |
| 禁止时间戳数组 | 避免 `LocalDateTime` 输出为数组      |

下面的配置用于统一 Spring Boot 和 WebSocket 消息的 Jackson 序列化行为。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsJacksonConfig.java`

```java
package io.github.atengk.websocket.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket Jackson 配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
public class WsJacksonConfig {

    /**
     * WebSocket 专用 JsonMapper
     *
     * @return JsonMapper
     */
    @Bean("wsJsonMapper")
    public JsonMapper wsJsonMapper() {
        return JsonMapper.builder()
                // 支持 LocalDateTime、LocalDate、Instant 等 Java 时间类型
                .addModule(new JavaTimeModule())
                // 反序列化时忽略未知字段，便于协议平滑升级
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 避免 LocalDateTime 输出为数组
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 空 Bean 不抛异常，避免部分动态消息体序列化失败
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
    }
}
```

WebSocket 消息转换器配置如下，用于让 STOMP 消息使用统一的 JSON 转换规则。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsMessageConverterConfig.java`

```java
package io.github.atengk.websocket.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket 消息转换器配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Configuration
@RequiredArgsConstructor
public class WsMessageConverterConfig implements WebSocketMessageBrokerConfigurer {

    @Qualifier("wsJsonMapper")
    private final JsonMapper wsJsonMapper;

    /**
     * 配置 STOMP 消息转换器
     *
     * @param messageConverters 消息转换器列表
     * @return 是否完全自定义转换器
     */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(wsJsonMapper);
        messageConverters.add(converter);

        // 返回 false 表示保留 Spring 默认转换器，避免影响 String、byte[] 等消息处理
        return false;
    }
}
```

Jackson 配置注意事项如下：

| 注意项               | 说明                         |
| -------------------- | ---------------------------- |
| 不要输出枚举 ordinal | 枚举顺序变化会导致协议不兼容 |
| 未知字段建议忽略     | 有利于前后端灰度升级         |
| 时间格式必须统一     | 避免前端解析差异             |
| 大对象不要直接序列化 | 只推送必要字段               |
| 异常对象不要直接返回 | 防止序列化堆栈和敏感信息     |

### 时间字段序列化

时间字段序列化用于统一 `timestamp`、`sentAt`、`readAt`、`expireAt`、`onlineAt` 等字段格式。WebSocket 实时消息建议优先使用毫秒时间戳，原因是跨端处理简单、排序方便、时区歧义少。

推荐时间字段如下：

| 字段        | 类型   | 说明               |
| ----------- | ------ | ------------------ |
| `timestamp` | `Long` | 服务端生成消息时间 |
| `sentAt`    | `Long` | 聊天消息发送时间   |
| `readAt`    | `Long` | 已读时间           |
| `expireAt`  | `Long` | 消息过期时间       |
| `onlineAt`  | `Long` | 用户上线时间       |
| `offlineAt` | `Long` | 用户下线时间       |

推荐示例：

```json
{
  "timestamp": 1777956000000,
  "data": {
    "sentAt": 1777956000000,
    "expireAt": 1778042400000
  }
}
```

如果业务需要展示标准日期字符串，可以由前端格式化，不建议后端同时返回多种时间格式。

前端格式化示例：

```typescript
export const formatTimestamp = (timestamp?: number): string => {
  if (!timestamp) {
    return ''
  }

  const date = new Date(timestamp)
  return date.toLocaleString()
}
```

时间字段设计建议如下：

| 建议                          | 说明                     |
| ----------------------------- | ------------------------ |
| WebSocket 消息用毫秒时间戳    | 排序和兼容性更好         |
| 数据库仍使用 DATETIME         | 持久化层保持可读和可查询 |
| 不信任客户端时间              | 服务端生成关键时间       |
| 排序使用服务端时间或 sequence | 不使用客户端本地时间排序 |
| 跨时区展示交给前端            | 前端根据用户时区格式化   |

### 枚举字段序列化

枚举字段序列化用于保证消息类型、状态、内容类型、ACK 类型、通知级别等字段在前后端之间稳定传输。推荐使用枚举名称字符串，而不是数字 ordinal。

推荐枚举字段如下：

| 字段          | 示例                           |
| ------------- | ------------------------------ |
| `type`        | `PRIVATE_CHAT`、`USER_NOTICE`  |
| `status`      | `SUCCESS`、`FAILED`            |
| `contentType` | `TEXT`、`IMAGE`、`FILE`        |
| `ackType`     | `DELIVERED`、`READ`            |
| `noticeLevel` | `NORMAL`、`IMPORTANT`、`ERROR` |

Java 枚举示例：

文件位置：`src/main/java/io/github/atengk/websocket/enums/WsContentType.java`

```java
package io.github.atengk.websocket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 内容类型枚举
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Getter
@RequiredArgsConstructor
public enum WsContentType {

    /**
     * 文本
     */
    TEXT("文本"),

    /**
     * 图片
     */
    IMAGE("图片"),

    /**
     * 文件
     */
    FILE("文件"),

    /**
     * 语音
     */
    AUDIO("语音"),

    /**
     * 视频
     */
    VIDEO("视频"),

    /**
     * 系统消息
     */
    SYSTEM("系统消息");

    private final String description;
}
```

前端 TypeScript 枚举建议保持同名：

```typescript
export enum WsContentType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  FILE = 'FILE',
  AUDIO = 'AUDIO',
  VIDEO = 'VIDEO',
  SYSTEM = 'SYSTEM'
}
```

枚举兼容建议如下：

| 建议                     | 说明                             |
| ------------------------ | -------------------------------- |
| 新增枚举值要兼容旧客户端 | 前端未知类型进入默认处理         |
| 不删除旧枚举             | 先标记废弃，再逐步迁移           |
| 不使用数字 ordinal       | 后端枚举顺序变化会破坏协议       |
| 枚举文案前端维护         | 后端传稳定 code，前端负责显示    |
| 错误枚举要拒绝           | 客户端发送非法枚举时返回参数异常 |

### 消息压缩

消息压缩用于减少网络传输体积，适合大规模连接、跨公网传输、通知内容较长或消息频率较高的场景。WebSocket 压缩一般依赖 `permessage-deflate` 或网关/容器支持，但压缩会增加 CPU 消耗，不应盲目开启。

压缩适用场景：

| 场景              | 是否建议           |
| ----------------- | ------------------ |
| 小消息高频推送    | 不一定，压缩收益低 |
| 大文本通知        | 可以               |
| 公网弱网环境      | 可以               |
| 大量重复字段 JSON | 可以               |
| 内网低延迟系统    | 不一定             |
| CPU 已经紧张      | 谨慎               |

压缩优化建议如下：

| 建议                 | 说明                              |
| -------------------- | --------------------------------- |
| 优先减少字段         | 比压缩更直接                      |
| 大文件不走 WebSocket | 压缩也不适合传文件                |
| 压测 CPU 成本        | 压缩会增加服务端和客户端 CPU      |
| 网关支持要验证       | 不同网关对 WebSocket 压缩支持不同 |
| 小消息不强制压缩     | 可能收益小于成本                  |

实际项目中，优先通过精简消息体、按需查询详情、限制大消息来降低流量。压缩应作为网络优化手段，而不是协议设计的补救措施。

### 二进制消息处理

二进制消息处理用于支持非 JSON 的消息载荷，例如 Protobuf、MessagePack、音视频片段、二进制控制指令等。对于普通 Spring WebSocket STOMP 管理后台、聊天通知系统，不建议优先使用二进制协议，因为调试和兼容成本更高。

二进制消息适用场景如下：

| 场景           | 说明                        |
| -------------- | --------------------------- |
| 高频低延迟通信 | 需要极致减少序列化体积      |
| IoT 设备消息   | 设备协议可能是二进制        |
| 游戏或实时协作 | 对带宽和延迟敏感            |
| 内部服务通信   | 前后端可控，协议稳定        |
| 文件传输       | 仍不建议通过 STOMP 传大文件 |

对于浏览器业务系统，推荐策略如下：

| 数据类型     | 推荐传输方式      |
| ------------ | ----------------- |
| 业务消息     | JSON              |
| 附件文件     | HTTP 或对象存储   |
| 图片预览     | HTTP URL          |
| 大批量数据   | HTTP 分页         |
| 实时控制指令 | JSON 或轻量二进制 |
| IoT 消息     | 可考虑二进制协议  |

如果使用二进制协议，需要额外定义：

| 内容     | 说明                   |
| -------- | ---------------------- |
| 协议版本 | 二进制结构必须可升级   |
| 消息类型 | 标识 payload 类型      |
| 编解码器 | 前后端都要实现         |
| 错误处理 | 无法解析时返回统一错误 |
| 调试工具 | 提供编码和解码工具     |

### 协议版本兼容

协议版本兼容用于支持后端、Web 前端、移动端、管理端分阶段升级。WebSocket 是长连接，客户端可能长时间停留在旧页面，因此消息结构演进必须保持兼容。

版本字段建议放在外层：

```json
{
  "messageId": "1909550919275922203",
  "type": "USER_NOTICE",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {}
}
```

版本兼容策略如下：

| 变更类型      | 兼容策略                  |
| ------------- | ------------------------- |
| 新增可选字段  | 兼容，不需要升级大版本    |
| 删除字段      | 不建议直接删除，先废弃    |
| 字段重命名    | 新旧字段并行一段时间      |
| 字段类型变化  | 升级大版本                |
| 枚举新增      | 前端默认忽略未知枚举      |
| 消息类型新增  | 前端进入默认处理逻辑      |
| data 结构大改 | 升级版本并按 version 分发 |

前端分发建议：

```typescript
export const dispatchByVersion = (message: WsMessage) => {
  const version = message.version || '1.0'

  if (version.startsWith('1.')) {
    dispatchV1Message(message)
    return
  }

  if (version.startsWith('2.')) {
    dispatchV2Message(message)
    return
  }

  console.warn('未知 WebSocket 协议版本：', version)
}
```

协议兼容建议如下：

| 建议                     | 说明                                      |
| ------------------------ | ----------------------------------------- |
| 外层字段长期稳定         | `messageId`、`type`、`version` 不轻易修改 |
| 新字段默认可选           | 旧客户端忽略即可                          |
| 前端解析要容错           | 未知字段、未知类型不能导致连接崩溃        |
| 服务端可按客户端版本返回 | CONNECT Header 中传 `clientVersion`       |
| 保留废弃周期             | 至少跨一个版本周期再删除字段              |

### 自定义 Header

自定义 Header 用于在 STOMP 帧中传递认证信息、客户端类型、设备 ID、协议版本、租户 ID、traceId、语言环境等元数据。Header 适合传递控制信息，不适合传递大量业务数据。

常见自定义 Header 如下：

| Header          | 说明                           |
| --------------- | ------------------------------ |
| `Authorization` | Bearer Token                   |
| `clientType`    | 客户端类型，如 WEB、APP、ADMIN |
| `deviceId`      | 设备 ID                        |
| `clientVersion` | 客户端版本                     |
| `tenantId`      | 租户 ID                        |
| `traceId`       | 链路追踪 ID                    |
| `lang`          | 语言，如 `zh-CN`               |

客户端 CONNECT Header 示例：

```typescript
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws/stomp',
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
    clientType: 'WEB',
    deviceId: 'browser-001',
    clientVersion: '1.0.0',
    lang: 'zh-CN'
  }
})
```

服务端解析示例：

```java
String clientType = accessor.getFirstNativeHeader("clientType");
String deviceId = accessor.getFirstNativeHeader("deviceId");
String clientVersion = accessor.getFirstNativeHeader("clientVersion");
```

自定义 Header 建议如下：

| 建议                      | 说明                          |
| ------------------------- | ----------------------------- |
| Header 保持简短           | 不放大对象或长文本            |
| 认证 Header 优先          | Token 优先放 `Authorization`  |
| 不信任 userId Header      | 用户身份必须从 Token 解析     |
| Header 需要脱敏日志       | Authorization 不打印完整值    |
| 租户信息优先从 Token 获取 | Header 中 tenantId 可作为辅助 |

### 自定义消息类型

自定义消息类型用于扩展业务消息，例如聊天、通知、任务、告警、在线状态、会话控制、管理端看板等。消息类型要稳定、清晰、可枚举，前端根据 `type` 分发处理。

推荐消息类型分组如下：

| 分组     | 类型示例                                              |
| -------- | ----------------------------------------------------- |
| 聊天消息 | `PRIVATE_CHAT`、`GROUP_CHAT`、`MESSAGE_RECALL`        |
| 通知消息 | `USER_NOTICE`、`SYSTEM_NOTICE`、`NOTICE_UNREAD_COUNT` |
| 任务消息 | `TASK_PROGRESS`、`TASK_FINISHED`、`TASK_FAILED`       |
| 在线状态 | `ONLINE_STATUS`、`ROOM_ONLINE_COUNT`                  |
| 会话控制 | `SESSION_KICKED`、`SESSION_EXPIRED`                   |
| 错误消息 | `ERROR`、`ACK`                                        |
| 管理端   | `ADMIN_DASHBOARD`、`ALARM`                            |

扩展枚举示例：

```java
/**
 * 任务完成
 */
TASK_FINISHED("任务完成"),

/**
 * 任务失败
 */
TASK_FAILED("任务失败"),

/**
 * 通知未读数
 */
NOTICE_UNREAD_COUNT("通知未读数"),

/**
 * 房间在线人数
 */
ROOM_ONLINE_COUNT("房间在线人数"),

/**
 * 会话过期
 */
SESSION_EXPIRED("会话过期");
```

自定义消息类型设计建议如下：

| 建议                   | 说明                              |
| ---------------------- | --------------------------------- |
| 类型名称使用大写下划线 | 前后端统一                        |
| 不复用语义不同的类型   | 避免前端分发混乱                  |
| 新类型要有默认处理     | 前端未知类型不应报错              |
| 类型文档化             | 每个类型说明路径、data 结构和用途 |
| 配合 version 使用      | 复杂结构变更时按版本处理          |

## 前端接入设计

前端接入设计用于统一浏览器端 WebSocket STOMP 连接、认证、自动重连、订阅管理、消息发送、消息接收、页面销毁清理和多标签页处理。前端不建议在每个页面重复创建连接，应封装统一客户端模块，再由业务页面注册订阅和处理器。

### STOMP 客户端选型

STOMP 客户端建议优先选择 `@stomp/stompjs`。它支持 STOMP 协议、自动重连、心跳、订阅管理、Header 传递和 SockJS 适配，适合 Vue、React、原生 TypeScript 项目。

常见选型如下：

| 客户端           | 说明                             | 推荐程度         |
| ---------------- | -------------------------------- | ---------------- |
| `@stomp/stompjs` | 现代 STOMP 客户端，维护较好      | 推荐             |
| `sockjs-client`  | SockJS 兼容库，配合 STOMP 使用   | 按需使用         |
| 原生 `WebSocket` | 只支持裸 WebSocket，不支持 STOMP | 仅简单场景       |
| 旧版 `stompjs`   | 老版本库                         | 不推荐新项目使用 |

安装依赖：

```bash
npm install @stomp/stompjs sockjs-client
npm install -D @types/sockjs-client
```

命令说明：`@stomp/stompjs` 用于 STOMP 协议客户端；`sockjs-client` 用于兼容不支持原生 WebSocket 的环境或需要 SockJS fallback 的场景。

### SockJS 客户端接入

SockJS 客户端接入用于兼容某些代理、旧浏览器或特殊网络环境。后端 Endpoint 使用 `.withSockJS()` 时，前端可以通过 `SockJS` 创建连接，再交给 STOMP Client 使用。

SockJS 接入示例：

```typescript
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export const createSockJsStompClient = (accessToken: string) => {
  return new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws/stomp'),
    connectHeaders: {
      Authorization: `Bearer ${accessToken}`,
      clientType: 'WEB',
      deviceId: 'browser-001'
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000
  })
}
```

SockJS 注意事项如下：

| 注意项                           | 说明                                         |
| -------------------------------- | -------------------------------------------- |
| URL 使用 HTTP/HTTPS              | SockJS 通常传入 `http://` 或 `https://` 地址 |
| 后端需要 `.withSockJS()`         | 前后端要匹配                                 |
| Header 仍通过 STOMP CONNECT 传递 | 浏览器 WebSocket 握手不能自定义 Header       |
| Query Token 谨慎使用             | 可能进入网关日志                             |
| 生产优先 WSS                     | HTTPS 页面必须使用安全连接                   |

### 原生 WebSocket 接入

原生 WebSocket 接入适合不使用 STOMP 协议的简单场景，例如服务端只处理裸 WebSocket 文本消息。但当前文档基于 Spring WebSocket STOMP，原生 WebSocket 不能直接订阅 `/topic/**`、`/user/queue/**`，也不能使用 STOMP `SEND`、`SUBSCRIBE`、`ACK` 语义。

原生 WebSocket 示例：

```typescript
const socket = new WebSocket('ws://localhost:8080/ws/native')

socket.onopen = () => {
  console.info('WebSocket 连接成功')
  socket.send(JSON.stringify({
    type: 'PING',
    data: {}
  }))
}

socket.onmessage = (event) => {
  const message = JSON.parse(event.data)
  console.info('收到消息：', message)
}

socket.onclose = () => {
  console.warn('WebSocket 连接关闭')
}

socket.onerror = (event) => {
  console.error('WebSocket 连接异常：', event)
}
```

原生 WebSocket 与 STOMP 对比如下：

| 能力        | 原生 WebSocket     | STOMP                     |
| ----------- | ------------------ | ------------------------- |
| 订阅 Topic  | 需要自定义实现     | 支持                      |
| 用户队列    | 需要自定义实现     | 支持                      |
| Header      | 握手 Header 受限   | STOMP Header 支持         |
| ACK         | 需要自定义实现     | 协议和业务都可支持        |
| 路由        | 自定义             | `/app`、`/topic`、`/user` |
| Spring 集成 | 需要自定义 Handler | 原生支持                  |

如果项目已经采用 STOMP，不建议前端再混用裸 WebSocket，除非是单独的二进制通道或特殊设备协议。

### Vue 接入

Vue 接入建议封装一个独立的 STOMP 客户端模块，并通过 Pinia 或组合式函数向页面暴露连接、订阅、发送、断开和状态监听能力。不要在每个页面中重复创建 `Client` 实例。

推荐目录结构：

```text
src/
  api/
    websocket/
      stomp-client.ts
      ws-types.ts
  stores/
    websocket.ts
  views/
    demo/
      WebSocketDemo.vue
```

下面定义前端通用消息类型。

文件位置：`src/api/websocket/ws-types.ts`

```typescript
export interface WsMessage<T = unknown> {
  messageId: string
  type: string
  status: string
  version: string
  timestamp: number
  traceId?: string
  data?: T
}

export interface WsSubscriptionOptions {
  destination: string
  callback: (message: WsMessage) => void
}

export interface WsSendOptions<T = unknown> {
  destination: string
  body?: T
  headers?: Record<string, string>
}
```

下面封装 Vue 项目可复用的 STOMP 客户端。

文件位置：`src/api/websocket/stomp-client.ts`

```typescript
import { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { WsMessage, WsSendOptions } from './ws-types'

class StompClientManager {
  private client: Client | null = null
  private subscriptions = new Map<string, StompSubscription>()
  private connected = false

  connect(accessToken: string): void {
    if (this.client?.active) {
      return
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws/stomp'),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
        clientType: 'WEB',
        deviceId: this.getDeviceId(),
        clientVersion: '1.0.0'
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connected = true
        console.info('WebSocket STOMP 连接成功')
        this.restoreSubscriptions()
      },
      onDisconnect: () => {
        this.connected = false
        console.warn('WebSocket STOMP 已断开')
      },
      onStompError: (frame) => {
        console.error('STOMP 协议异常：', frame.headers['message'], frame.body)
      },
      onWebSocketClose: () => {
        this.connected = false
        console.warn('WebSocket 连接关闭')
      }
    })

    this.client.activate()
  }

  disconnect(): void {
    this.subscriptions.forEach((subscription) => subscription.unsubscribe())
    this.subscriptions.clear()
    this.client?.deactivate()
    this.connected = false
  }

  subscribe(destination: string, callback: (message: WsMessage) => void): void {
    if (!this.client || !this.connected) {
      this.subscriptions.set(destination, {
        unsubscribe: () => {}
      } as StompSubscription)
      return
    }

    if (this.subscriptions.has(destination)) {
      return
    }

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      const wsMessage = this.parseMessage(message.body)
      if (wsMessage) {
        callback(wsMessage)
      }
    })

    this.subscriptions.set(destination, subscription)
  }

  unsubscribe(destination: string): void {
    const subscription = this.subscriptions.get(destination)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(destination)
    }
  }

  send<T = unknown>(options: WsSendOptions<T>): void {
    if (!this.client || !this.connected) {
      console.warn('WebSocket 未连接，消息发送失败：', options.destination)
      return
    }

    this.client.publish({
      destination: options.destination,
      headers: options.headers || {},
      body: JSON.stringify(options.body || {})
    })
  }

  isConnected(): boolean {
    return this.connected
  }

  private restoreSubscriptions(): void {
    const destinations = Array.from(this.subscriptions.keys())
    this.subscriptions.clear()

    destinations.forEach((destination) => {
      console.info('WebSocket 订阅需要业务层重新注册：', destination)
    })
  }

  private parseMessage(body: string): WsMessage | null {
    if (!body) {
      return null
    }

    try {
      return JSON.parse(body) as WsMessage
    } catch (error) {
      console.error('WebSocket 消息解析失败：', error)
      return null
    }
  }

  private getDeviceId(): string {
    const key = 'ws:deviceId'
    const exists = localStorage.getItem(key)
    if (exists) {
      return exists
    }

    const deviceId = `web-${Date.now()}-${Math.random().toString(16).slice(2)}`
    localStorage.setItem(key, deviceId)
    return deviceId
  }
}

export const stompClientManager = new StompClientManager()
```

Vue 页面使用示例：

文件位置：`src/views/demo/WebSocketDemo.vue`

```vue
<template>
  <div class="p-4">
    <h2>WebSocket STOMP 示例</h2>

    <div class="mt-3">
      <button @click="connect">连接</button>
      <button @click="subscribeNotice">订阅通知</button>
      <button @click="sendTestMessage">发送测试消息</button>
      <button @click="disconnect">断开</button>
    </div>

    <pre class="mt-3">{{ messages }}</pre>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount } from 'vue'
import { stompClientManager } from '@/api/websocket/stomp-client'
import type { WsMessage } from '@/api/websocket/ws-types'

const messages = ref<WsMessage[]>([])

const connect = () => {
  const token = localStorage.getItem('access_token') || ''
  stompClientManager.connect(token)
}

const subscribeNotice = () => {
  stompClientManager.subscribe('/user/queue/notice', (message) => {
    messages.value.unshift(message)
  })

  stompClientManager.subscribe('/topic/system/notice', (message) => {
    messages.value.unshift(message)
  })
}

const sendTestMessage = () => {
  stompClientManager.send({
    destination: '/app/admin/push/test',
    body: {
      title: '测试通知',
      content: '这是一条前端发送的测试消息'
    }
  })
}

const disconnect = () => {
  stompClientManager.disconnect()
}

onBeforeUnmount(() => {
  stompClientManager.unsubscribe('/user/queue/notice')
  stompClientManager.unsubscribe('/topic/system/notice')
})
</script>
```

### React 接入

React 接入同样建议封装独立 Hook，统一处理连接、订阅、发送和销毁。组件只负责注册订阅和处理业务消息，不直接管理底层 STOMP 细节。

React Hook 示例：

文件位置：`src/websocket/useStompClient.ts`

```typescript
import { useCallback, useEffect, useRef, useState } from 'react'
import { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export interface WsMessage<T = unknown> {
  messageId: string
  type: string
  status: string
  version: string
  timestamp: number
  traceId?: string
  data?: T
}

export const useStompClient = (accessToken: string) => {
  const clientRef = useRef<Client | null>(null)
  const subscriptionsRef = useRef<Map<string, StompSubscription>>(new Map())
  const [connected, setConnected] = useState(false)

  const connect = useCallback(() => {
    if (clientRef.current?.active) {
      return
    }

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws/stomp'),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
        clientType: 'WEB',
        deviceId: localStorage.getItem('ws:deviceId') || 'react-web'
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true)
        console.info('WebSocket STOMP 连接成功')
      },
      onWebSocketClose: () => {
        setConnected(false)
        console.warn('WebSocket 连接关闭')
      },
      onStompError: (frame) => {
        console.error('STOMP 协议异常：', frame.headers['message'], frame.body)
      }
    })

    clientRef.current = client
    client.activate()
  }, [accessToken])

  const subscribe = useCallback((destination: string, callback: (message: WsMessage) => void) => {
    const client = clientRef.current
    if (!client || !connected || subscriptionsRef.current.has(destination)) {
      return
    }

    const subscription = client.subscribe(destination, (message: IMessage) => {
      try {
        callback(JSON.parse(message.body) as WsMessage)
      } catch (error) {
        console.error('WebSocket 消息解析失败：', error)
      }
    })

    subscriptionsRef.current.set(destination, subscription)
  }, [connected])

  const unsubscribe = useCallback((destination: string) => {
    const subscription = subscriptionsRef.current.get(destination)
    if (subscription) {
      subscription.unsubscribe()
      subscriptionsRef.current.delete(destination)
    }
  }, [])

  const send = useCallback((destination: string, body?: unknown, headers?: Record<string, string>) => {
    const client = clientRef.current
    if (!client || !connected) {
      console.warn('WebSocket 未连接，消息发送失败：', destination)
      return
    }

    client.publish({
      destination,
      headers: headers || {},
      body: JSON.stringify(body || {})
    })
  }, [connected])

  const disconnect = useCallback(() => {
    subscriptionsRef.current.forEach((subscription) => subscription.unsubscribe())
    subscriptionsRef.current.clear()
    clientRef.current?.deactivate()
    setConnected(false)
  }, [])

  useEffect(() => {
    return () => {
      disconnect()
    }
  }, [disconnect])

  return {
    connected,
    connect,
    subscribe,
    unsubscribe,
    send,
    disconnect
  }
}
```

React 页面示例：

文件位置：`src/pages/WebSocketDemo.tsx`

```tsx
import React, { useEffect, useState } from 'react'
import { useStompClient, WsMessage } from '../websocket/useStompClient'

export const WebSocketDemo: React.FC = () => {
  const token = localStorage.getItem('access_token') || ''
  const { connected, connect, subscribe, unsubscribe, send } = useStompClient(token)
  const [messages, setMessages] = useState<WsMessage[]>([])

  useEffect(() => {
    connect()
  }, [connect])

  useEffect(() => {
    if (!connected) {
      return
    }

    subscribe('/user/queue/notice', (message) => {
      setMessages((prev) => [message, ...prev])
    })

    subscribe('/topic/system/notice', (message) => {
      setMessages((prev) => [message, ...prev])
    })

    return () => {
      unsubscribe('/user/queue/notice')
      unsubscribe('/topic/system/notice')
    }
  }, [connected, subscribe, unsubscribe])

  const sendTest = () => {
    send('/app/admin/push/test', {
      title: '测试通知',
      content: 'React 页面发送的测试消息'
    })
  }

  return (
    <div>
      <h2>WebSocket STOMP 示例</h2>
      <button onClick={sendTest}>发送测试消息</button>
      <pre>{JSON.stringify(messages, null, 2)}</pre>
    </div>
  )
}
```

### Token 传递

Token 传递用于在 STOMP 连接建立时完成用户认证。推荐通过 STOMP `CONNECT` Header 传递 `Authorization`，避免将 Token 放在 URL Query 中。

推荐方式：

```typescript
connectHeaders: {
  Authorization: `Bearer ${accessToken}`,
  clientType: 'WEB',
  deviceId: 'browser-001'
}
```

不同传递方式对比如下：

| 方式         | 安全性     | 说明                            |
| ------------ | ---------- | ------------------------------- |
| STOMP Header | 较好       | 推荐方式                        |
| Query 参数   | 一般       | 可能进入网关日志                |
| Cookie       | 视配置而定 | 需要处理 SameSite、CSRF、Origin |
| 消息体传递   | 不推荐     | 认证语义不清晰                  |

Token 传递建议如下：

| 建议                     | 说明                  |
| ------------------------ | --------------------- |
| 使用短期 Access Token    | 降低泄露影响          |
| 生产环境使用 WSS         | 防止明文传输          |
| Token 过期后重连         | 刷新 Token 后重建连接 |
| 不在日志输出完整 Token   | 前后端都要避免        |
| 服务端不信任 userId 参数 | 用户身份从 Token 解析 |

### 自动重连

自动重连用于处理网络抖动、网关断开、服务重启、浏览器休眠恢复等情况。STOMP 客户端可以通过 `reconnectDelay` 启用自动重连。

配置示例：

```typescript
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws/stomp',
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000
})
```

自动重连处理流程：

```text
1. WebSocket 连接断开
2. 客户端等待 reconnectDelay
3. 使用当前 Token 重新 CONNECT
4. 连接成功后重新订阅
5. 调用补偿接口拉取断线期间消息
6. 前端去重、排序、合并
```

自动重连注意事项如下：

| 场景       | 处理                                  |
| ---------- | ------------------------------------- |
| 网络异常   | 可以自动重连                          |
| 服务重启   | 可以自动重连                          |
| Token 过期 | 不应无限重连，应刷新 Token 或跳转登录 |
| 权限被拒绝 | 停止订阅对应主题                      |
| 被踢下线   | 主动断开并跳转登录                    |
| 高频重连   | 增加退避或停止重试                    |

推荐增加重连后的补偿逻辑：

```typescript
onConnect: () => {
  resubscribeAll()
  compensateOfflineMessages()
}
```

### 订阅管理

订阅管理用于统一维护当前页面或当前应用注册的 Topic、Queue 和业务处理器。STOMP 连接断开后，旧订阅会失效；重连成功后必须重新订阅。

订阅管理建议如下：

| 建议             | 说明                                     |
| ---------------- | ---------------------------------------- |
| 保存订阅注册表   | 记录 destination 和 callback             |
| 防止重复订阅     | 同一 destination 不重复注册              |
| 页面销毁取消订阅 | 避免内存泄漏和重复消费                   |
| 重连后恢复订阅   | 旧订阅不会自动恢复                       |
| 按页面隔离订阅   | 页面级订阅只在页面生命周期内存在         |
| 全局订阅集中管理 | 用户通知、错误队列、会话控制属于全局订阅 |

订阅注册结构示例：

```typescript
interface SubscriptionRegister {
  destination: string
  callback: (message: WsMessage) => void
  global?: boolean
}
```

全局订阅建议：

| 订阅路径               | 用途       |
| ---------------------- | ---------- |
| `/user/queue/notice`   | 用户通知   |
| `/user/queue/errors`   | 错误消息   |
| `/user/queue/session`  | 会话控制   |
| `/user/queue/ack`      | 服务端 ACK |
| `/topic/system/notice` | 系统公告   |

页面级订阅建议在页面卸载时取消，例如群聊页面的 `/topic/group/{groupId}`。

### 消息发送封装

消息发送封装用于统一处理 STOMP `publish` 调用、目标地址、请求体序列化、Header、客户端消息 ID、错误日志和连接状态校验。业务页面不应直接调用底层 `client.publish`。

推荐发送封装如下：

```typescript
export interface SendMessageOptions<T = unknown> {
  destination: string
  body?: T
  headers?: Record<string, string>
  receipt?: string
}

export const sendWsMessage = <T = unknown>(
  client: Client,
  options: SendMessageOptions<T>
): boolean => {
  if (!client.connected) {
    console.warn('WebSocket 未连接，无法发送消息：', options.destination)
    return false
  }

  client.publish({
    destination: options.destination,
    headers: {
      ...(options.headers || {}),
      ...(options.receipt ? { receipt: options.receipt } : {})
    },
    body: JSON.stringify(options.body || {})
  })

  return true
}
```

业务发送示例：

```typescript
sendWsMessage(client, {
  destination: '/app/chat/private/send',
  body: {
    receiverId: '10002',
    content: '你好',
    contentType: 'TEXT',
    clientMessageId: crypto.randomUUID()
  }
})
```

消息发送封装建议如下：

| 建议                     | 说明                 |
| ------------------------ | -------------------- |
| 发送前检查连接状态       | 未连接时不要静默失败 |
| 自动生成 clientMessageId | 支持服务端幂等       |
| 统一 JSON.stringify      | 避免业务页面重复处理 |
| 支持 receipt             | 需要协议层确认时传递 |
| 错误统一上报             | 发送失败进入前端日志 |
| 大消息提前拦截           | 前端先限制内容长度   |

### 消息接收封装

消息接收封装用于统一解析服务端消息、校验基础字段、去重、排序、错误处理和消息分发。业务页面不应重复写 `JSON.parse` 和 switch 分发逻辑。

推荐接收流程：

```text
1. 收到 STOMP MESSAGE
2. JSON.parse 解析
3. 校验 messageId、type、version
4. 按 messageId 去重
5. 按 type 分发
6. 必要时发送 ACK
7. 更新页面状态
```

接收封装示例：

```typescript
const handledMessageIds = new Set<string>()

export const handleIncomingWsMessage = (
  body: string,
  dispatch: (message: WsMessage) => void
): void => {
  if (!body) {
    return
  }

  let message: WsMessage

  try {
    message = JSON.parse(body) as WsMessage
  } catch (error) {
    console.error('WebSocket 消息解析失败：', error)
    return
  }

  if (!message.messageId || !message.type || !message.version) {
    console.warn('WebSocket 消息基础字段缺失：', message)
    return
  }

  if (handledMessageIds.has(message.messageId)) {
    console.warn('WebSocket 重复消息已忽略：', message.messageId)
    return
  }

  if (handledMessageIds.size > 2000) {
    const first = handledMessageIds.values().next().value
    handledMessageIds.delete(first)
  }

  handledMessageIds.add(message.messageId)
  dispatch(message)
}
```

消息分发示例：

```typescript
export const dispatchWsMessage = (message: WsMessage): void => {
  switch (message.type) {
    case 'USER_NOTICE':
      handleUserNotice(message)
      break
    case 'SYSTEM_NOTICE':
      handleSystemNotice(message)
      break
    case 'PRIVATE_CHAT':
      handlePrivateChat(message)
      break
    case 'GROUP_CHAT':
      handleGroupChat(message)
      break
    case 'ERROR':
      handleWsError(message)
      break
    case 'SESSION_KICKED':
      handleSessionKicked(message)
      break
    default:
      console.warn('未知 WebSocket 消息类型：', message.type)
  }
}
```

消息接收封装建议如下：

| 建议               | 说明                                  |
| ------------------ | ------------------------------------- |
| 单条异常不影响连接 | JSON 解析失败只丢弃当前消息           |
| 基础字段必须校验   | 防止脏消息进入业务层                  |
| 先去重再分发       | 补偿消息和实时消息可能重复            |
| 未知类型默认忽略   | 保证协议可扩展                        |
| 重要消息发送 ACK   | 需要确认的消息处理后回执              |
| 错误消息统一处理   | `/user/queue/errors` 进入统一错误提示 |

### 页面销毁清理

页面销毁清理用于避免页面切换后订阅仍然存在，导致重复消费、内存泄漏、重复弹窗和旧页面状态被更新。前端必须在页面生命周期结束时取消页面级订阅。

Vue 清理示例：

```typescript
import { onBeforeUnmount } from 'vue'

const groupDestination = `/topic/group/${groupId}`

stompClientManager.subscribe(groupDestination, handleGroupMessage)

onBeforeUnmount(() => {
  stompClientManager.unsubscribe(groupDestination)
})
```

React 清理示例：

```typescript
useEffect(() => {
  if (!connected) {
    return
  }

  const destination = `/topic/group/${groupId}`
  subscribe(destination, handleGroupMessage)

  return () => {
    unsubscribe(destination)
  }
}, [connected, groupId, subscribe, unsubscribe])
```

清理规则建议如下：

| 类型         | 是否页面销毁清理   |
| ------------ | ------------------ |
| 群组主题     | 是                 |
| 房间主题     | 是                 |
| 页面看板主题 | 是                 |
| 临时任务进度 | 是                 |
| 用户通知队列 | 否，通常是全局订阅 |
| 错误队列     | 否，通常是全局订阅 |
| 会话控制队列 | 否，通常是全局订阅 |

页面销毁时不一定要断开整个 WebSocket 连接。通常只取消页面级订阅，全局连接保持在线，避免频繁重连。

### 多标签页处理

多标签页处理用于解决同一用户在同一浏览器打开多个页面时产生多个 WebSocket 连接、重复通知、重复订阅和未读数多次处理的问题。多标签页策略需要根据业务选择。

常见策略如下：

| 策略                  | 说明                                                         |
| --------------------- | ------------------------------------------------------------ |
| 每个标签页独立连接    | 实现简单，但连接数增加                                       |
| 单主标签页连接        | 只有一个标签页保持 WebSocket，其他标签页通过 BroadcastChannel 接收 |
| SharedWorker 共享连接 | 多标签页共享一个 Worker 连接                                 |
| 服务端允许多连接      | 后端按用户多 Session 管理                                    |
| 服务端限制同端连接数  | 新标签页踢出旧连接或拒绝新连接                               |

推荐普通后台系统使用“单主标签页连接 + BroadcastChannel”策略，降低连接数和重复消息处理。

BroadcastChannel 示例：

```typescript
const channel = new BroadcastChannel('ws-message-channel')

export const publishToTabs = (message: WsMessage) => {
  channel.postMessage(message)
}

export const listenFromTabs = (callback: (message: WsMessage) => void) => {
  channel.onmessage = (event) => {
    callback(event.data as WsMessage)
  }
}
```

主标签页选举可以使用 localStorage 简化实现：

```typescript
const MASTER_KEY = 'ws:master-tab'
const TAB_ID = `${Date.now()}-${Math.random().toString(16).slice(2)}`

export const tryBecomeMasterTab = (): boolean => {
  const current = localStorage.getItem(MASTER_KEY)

  if (!current) {
    localStorage.setItem(MASTER_KEY, TAB_ID)
    return true
  }

  return current === TAB_ID
}

export const releaseMasterTab = (): void => {
  if (localStorage.getItem(MASTER_KEY) === TAB_ID) {
    localStorage.removeItem(MASTER_KEY)
  }
}
```

多标签页处理建议如下：

| 场景         | 建议                               |
| ------------ | ---------------------------------- |
| 普通管理后台 | 单主标签页连接，其他标签页共享消息 |
| 聊天系统     | 可允许多标签页，但前端必须去重     |
| 高安全系统   | 服务端限制同端连接数               |
| 通知中心     | 多标签页必须按 messageId 去重      |
| 未读数       | 以服务端最终未读数为准             |
| 页面关闭     | 主标签页关闭后重新选举             |

多标签页处理不能只依赖前端。服务端仍需要支持同用户多连接管理、消息幂等、未读数幂等和指定 Session 推送，否则不同浏览器、不同设备仍然可能产生重复消息和状态竞争。

## API 与消息接口设计

API 与消息接口设计用于统一 HTTP 初始化接口、WebSocket 连接地址、STOMP 发送地址、STOMP 订阅地址、历史消息、未读数、在线用户、群组管理、通知管理和管理端监控接口。WebSocket 负责实时推送，HTTP API 负责初始化、查询、分页、补偿和管理操作，两者需要明确边界。

### HTTP 初始化接口

HTTP 初始化接口用于客户端在建立 WebSocket 连接前获取必要配置，例如连接地址、订阅路径、当前用户信息、未读数、最近同步时间、服务端时间、协议版本等。

推荐接口：

```text
GET /api/ws/init
```

返回示例：

```json
{
  "serverTime": 1777956000000,
  "protocolVersion": "1.0",
  "endpoint": "/ws/stomp",
  "heartbeatIncoming": 10000,
  "heartbeatOutgoing": 10000,
  "user": {
    "userId": "10001",
    "nickname": "张三",
    "clientType": "WEB"
  },
  "subscriptions": {
    "userNotice": "/user/queue/notice",
    "userChat": "/user/queue/chat",
    "userError": "/user/queue/errors",
    "userSession": "/user/queue/session",
    "systemNotice": "/topic/system/notice"
  },
  "unread": {
    "noticeUnread": 8,
    "chatUnread": 12,
    "todoUnread": 3
  }
}
```

HTTP 初始化接口建议返回以下内容：

| 字段                | 说明                           |
| ------------------- | ------------------------------ |
| `serverTime`        | 服务端当前时间，便于客户端校准 |
| `protocolVersion`   | WebSocket 消息协议版本         |
| `endpoint`          | WebSocket STOMP 连接地址       |
| `heartbeatIncoming` | 客户端接收心跳配置             |
| `heartbeatOutgoing` | 客户端发送心跳配置             |
| `subscriptions`     | 推荐订阅地址                   |
| `unread`            | 当前未读数                     |
| `user`              | 当前用户基础信息               |

初始化接口控制器示例。

文件位置：`src/main/java/io/github/atengk/websocket/controller/WsInitController.java`

```java
package io.github.atengk.websocket.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket 初始化接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class WsInitController {

    /**
     * 获取 WebSocket 初始化配置
     *
     * @param principal 当前用户
     * @return 初始化配置
     */
    @GetMapping("/api/ws/init")
    public Map<String, Object> init(Principal principal) {
        String userId = principal == null ? null : principal.getName();

        Map<String, Object> subscriptions = MapUtil.<String, Object>builder()
                .put("userNotice", "/user/queue/notice")
                .put("userChat", "/user/queue/chat")
                .put("userError", "/user/queue/errors")
                .put("userSession", "/user/queue/session")
                .put("systemNotice", "/topic/system/notice")
                .build();

        Map<String, Object> unread = MapUtil.<String, Object>builder()
                .put("noticeUnread", 0)
                .put("chatUnread", 0)
                .put("todoUnread", 0)
                .build();

        Map<String, Object> user = MapUtil.<String, Object>builder()
                .put("userId", userId)
                .put("clientType", "WEB")
                .build();

        log.info("WebSocket 初始化配置已返回，userId：{}", userId);

        return MapUtil.<String, Object>builder()
                .put("serverTime", DateUtil.current())
                .put("protocolVersion", "1.0")
                .put("endpoint", "/ws/stomp")
                .put("heartbeatIncoming", 10000)
                .put("heartbeatOutgoing", 10000)
                .put("user", user)
                .put("subscriptions", subscriptions)
                .put("unread", unread)
                .build();
    }
}
```

初始化接口注意事项如下：

| 注意项         | 说明                               |
| -------------- | ---------------------------------- |
| 登录后调用     | 未登录用户不应获取用户私有订阅路径 |
| 不返回敏感信息 | 不返回 Token、Cookie、完整权限明细 |
| 返回服务端时间 | 便于客户端处理补偿时间             |
| 返回协议版本   | 支持前后端版本兼容                 |
| 返回推荐订阅   | 降低前端硬编码路径                 |

### WebSocket 连接地址

WebSocket 连接地址用于客户端建立 STOMP 连接。后端配置 Endpoint 后，前端使用该地址连接。

推荐地址如下：

| 环境        | 地址示例                          |
| ----------- | --------------------------------- |
| 本地开发    | `ws://localhost:8080/ws/stomp`    |
| 本地 SockJS | `http://localhost:8080/ws/stomp`  |
| 测试环境    | `wss://test.example.com/ws/stomp` |
| 生产环境    | `wss://api.example.com/ws/stomp`  |

后端配置示例：

```java
registry.addEndpoint("/ws/stomp")
        .addInterceptors(wsHandshakeInterceptor)
        .setAllowedOriginPatterns("https://*.example.com")
        .withSockJS();
```

前端 STOMP 连接示例：

```typescript
const client = new Client({
  brokerURL: 'wss://api.example.com/ws/stomp',
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
    clientType: 'WEB',
    deviceId: 'browser-001'
  },
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000
})
```

连接地址设计建议如下：

| 建议              | 说明                              |
| ----------------- | --------------------------------- |
| 统一前缀          | 建议统一使用 `/ws/**`             |
| 生产使用 WSS      | HTTPS 页面必须使用安全 WebSocket  |
| 不把 Token 放 URL | 优先使用 STOMP CONNECT Header     |
| 网关正确转发      | 必须支持 Upgrade                  |
| SockJS 地址不同   | SockJS 通常使用 `http/https` 地址 |

### STOMP 发送地址

STOMP 发送地址是客户端向服务端发送消息的 destination。推荐所有客户端发送地址统一使用 `/app/**` 前缀，由服务端 `@MessageMapping` 接收。

推荐发送地址如下：

| 功能           | 客户端发送地址            | 服务端处理                               |
| -------------- | ------------------------- | ---------------------------------------- |
| 单聊消息       | `/app/chat/private/send`  | `@MessageMapping("/chat/private/send")`  |
| 群聊消息       | `/app/chat/group/send`    | `@MessageMapping("/chat/group/send")`    |
| 消息 ACK       | `/app/message/ack`        | `@MessageMapping("/message/ack")`        |
| 通知已读       | `/app/notice/read`        | `@MessageMapping("/notice/read")`        |
| 全部通知已读   | `/app/notice/read-all`    | `@MessageMapping("/notice/read-all")`    |
| 房间加入       | `/app/room/join`          | `@MessageMapping("/room/join")`          |
| 房间退出       | `/app/room/leave`         | `@MessageMapping("/room/leave")`         |
| 管理端推送测试 | `/app/admin/push/test`    | `@MessageMapping("/admin/push/test")`    |
| 管理端踢人     | `/app/admin/session/kick` | `@MessageMapping("/admin/session/kick")` |

发送路径规范如下：

| 规则                       | 说明                                                |
| -------------------------- | --------------------------------------------------- |
| 客户端只能发送到 `/app/**` | 禁止直接发送到 `/topic/**`、`/queue/**`、`/user/**` |
| 业务路径清晰               | 使用模块名和动作名                                  |
| 管理端路径独立             | 管理端发送统一 `/app/admin/**`                      |
| 服务端必须鉴权             | SEND 阶段做通用鉴权，业务层做对象权限校验           |
| 发送体必须校验             | DTO 使用 `@Valid` 校验                              |

前端发送示例：

```typescript
client.publish({
  destination: '/app/chat/private/send',
  headers: {
    receipt: 'receipt-client-msg-001'
  },
  body: JSON.stringify({
    receiverId: '10002',
    content: '你好',
    contentType: 'TEXT',
    clientMessageId: crypto.randomUUID()
  })
})
```

### STOMP 订阅地址

STOMP 订阅地址是客户端接收服务端推送的 destination。订阅地址一般分为广播主题、用户私有队列、群组主题、房间主题、系统主题和管理端主题。

推荐订阅地址如下：

| 功能       | 客户端订阅地址                   | 服务端推送方式                          |
| ---------- | -------------------------------- | --------------------------------------- |
| 系统公告   | `/topic/system/notice`           | `convertAndSend`                        |
| 系统告警   | `/topic/system/alarm`            | `convertAndSend`                        |
| 用户通知   | `/user/queue/notice`             | `convertAndSendToUser`                  |
| 用户聊天   | `/user/queue/chat`               | `convertAndSendToUser`                  |
| 用户错误   | `/user/queue/errors`             | `@SendToUser` 或 `convertAndSendToUser` |
| 用户 ACK   | `/user/queue/ack`                | `convertAndSendToUser`                  |
| 会话控制   | `/user/queue/session`            | `convertAndSendToUser`                  |
| 群组消息   | `/topic/group/{groupId}`         | `convertAndSend`                        |
| 群组状态   | `/topic/group/{groupId}/status`  | `convertAndSend`                        |
| 房间人数   | `/topic/room/{roomId}/count`     | `convertAndSend`                        |
| 管理端在线 | `/topic/admin/online`            | `convertAndSend`                        |
| 管理端看板 | `/topic/admin/dashboard/message` | `convertAndSend`                        |

订阅权限建议如下：

| 订阅类型                      | 权限要求                       |
| ----------------------------- | ------------------------------ |
| `/user/queue/**`              | 登录用户                       |
| `/topic/group/{groupId}`      | 群组成员                       |
| `/topic/room/{roomId}`        | 房间访问权限                   |
| `/topic/admin/**`             | 管理员、运维或授权角色         |
| `/topic/tenant/{tenantId}/**` | 当前租户用户                   |
| `/topic/system/notice`        | 登录用户或公开用户，按业务决定 |

订阅示例：

```typescript
client.subscribe('/user/queue/notice', (message) => {
  const body = JSON.parse(message.body)
  console.info('收到用户通知：', body)
})

client.subscribe('/topic/group/90001', (message) => {
  const body = JSON.parse(message.body)
  console.info('收到群组消息：', body)
})
```

### 历史消息接口

历史消息接口用于查询聊天记录、群组消息、系统消息和断线补偿消息。历史数据量通常较大，必须使用 HTTP 分页查询，不建议通过 WebSocket 一次性推送历史列表。

推荐接口：

```text
GET /api/chat/messages?chatId=private-10001-10002&beforeSequence=10001&pageSize=20
GET /api/chat/messages?chatId=group-90001&afterSequence=30001&pageSize=100
GET /api/chat/messages/compensate?chatId=group-90001&lastSequence=30001
```

请求参数说明：

| 参数             | 说明                             |
| ---------------- | -------------------------------- |
| `chatId`         | 会话 ID                          |
| `beforeSequence` | 查询指定序号之前的消息           |
| `afterSequence`  | 查询指定序号之后的消息，适合补偿 |
| `pageSize`       | 每页数量                         |
| `keyword`        | 可选，消息内容搜索               |
| `contentType`    | 可选，按消息类型过滤             |

返回示例：

```json
{
  "chatId": "group-90001",
  "messages": [
    {
      "messageId": "1909550919275922301",
      "senderId": "10001",
      "groupId": "90001",
      "content": "大家好",
      "contentType": "TEXT",
      "sequence": 30001,
      "status": "SENT",
      "sentAt": 1777956000000
    }
  ],
  "hasMore": true,
  "nextCursor": "30001"
}
```

历史消息接口建议如下：

| 建议                     | 说明                            |
| ------------------------ | ------------------------------- |
| 使用游标分页             | 优先使用 `sequence`，避免深分页 |
| 必须鉴权                 | 用户只能查询有权限的会话        |
| 撤回消息不返回原文       | 返回撤回状态                    |
| 附件 URL 要鉴权          | 不返回永久公开下载地址          |
| 限制 pageSize            | 防止一次拉取过多                |
| 补偿接口按 sequence 查询 | 避免按时间导致漏消息            |

历史消息查询接口示例。

文件位置：`src/main/java/io/github/atengk/websocket/controller/ChatMessageQueryController.java`

```java
package io.github.atengk.websocket.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 聊天历史消息查询接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
public class ChatMessageQueryController {

    /**
     * 查询历史消息
     *
     * @param chatId         会话 ID
     * @param beforeSequence 查询该序号之前的消息
     * @param afterSequence  查询该序号之后的消息
     * @param pageSize       每页数量
     * @param principal      当前用户
     * @return 历史消息
     */
    @GetMapping("/api/chat/messages")
    public Map<String, Object> listMessages(String chatId,
                                            Long beforeSequence,
                                            Long afterSequence,
                                            Integer pageSize,
                                            Principal principal) {
        String userId = principal == null ? null : principal.getName();
        int size = pageSize == null ? 20 : Math.min(pageSize, 100);

        if (StrUtil.hasBlank(userId, chatId)) {
            throw new IllegalArgumentException("会话ID不能为空");
        }

        log.info("查询聊天历史消息，userId：{}，chatId：{}，beforeSequence：{}，afterSequence：{}，pageSize：{}",
                userId, chatId, beforeSequence, afterSequence, size);

        return MapUtil.<String, Object>builder()
                .put("chatId", chatId)
                .put("messages", List.of())
                .put("hasMore", false)
                .put("nextCursor", null)
                .build();
    }
}
```

### 未读数接口

未读数接口用于查询用户当前聊天、通知、待办、审批和告警未读数量。未读数可以通过 Redis 缓存读取，缓存缺失时从数据库聚合重建。

推荐接口：

```text
GET /api/ws/unread-count
GET /api/chat/unread-count
GET /api/notices/unread-count
```

返回示例：

```json
{
  "totalUnread": 23,
  "chatUnread": 12,
  "noticeUnread": 8,
  "todoUnread": 3,
  "approvalUnread": 2,
  "alarmUnread": 1,
  "chatUnreadMap": {
    "private-10001-10002": 3,
    "group-90001": 9
  }
}
```

未读数接口建议如下：

| 建议               | 说明                       |
| ------------------ | -------------------------- |
| 初始化时调用       | 页面加载后获取初始角标     |
| WebSocket 推送变更 | 后续实时同步未读变化       |
| 支持全量校准       | 前端角标异常时重新查询     |
| Redis 加速         | 避免频繁聚合数据库         |
| 以服务端为准       | 前端本地未读数只做临时展示 |

### 在线用户接口

在线用户接口用于管理端、客服系统、协作系统查询在线用户、在线连接和用户在线状态。普通用户不应获取全站在线用户列表，除非业务允许。

推荐接口：

```text
GET /api/ws/online/users
GET /api/ws/online/users?clientType=WEB
GET /api/ws/online/users?tenantId=1
GET /api/ws/online/users/{userId}
```

返回示例：

```json
{
  "total": 2,
  "users": [
    {
      "userId": "10001",
      "status": "ONLINE",
      "sessionCount": 2,
      "clientTypes": ["WEB", "APP"],
      "onlineAt": 1777956000000,
      "lastActiveAt": 1777956060000
    }
  ]
}
```

在线用户接口建议如下：

| 建议           | 说明                                |
| -------------- | ----------------------------------- |
| 管理端权限控制 | 全站在线列表属于敏感数据            |
| 返回脱敏字段   | 普通管理角色不展示完整 IP、设备指纹 |
| 支持分页       | 在线用户很多时必须分页              |
| 支持筛选       | 按租户、客户端类型、状态筛选        |
| 数据来自 Redis | 实时状态优先从 Redis 查询           |
| 数据库用于审计 | 历史连接从会话日志表查询            |

### 群组管理接口

群组管理接口用于创建群组、查询群组、加入成员、退出成员、禁言、踢人、解散、查询群组在线人数和成员列表。群组管理通常使用 HTTP API，群组消息实时通信使用 STOMP。

推荐接口：

| 功能         | 方法     | 地址                                             |
| ------------ | -------- | ------------------------------------------------ |
| 创建群组     | `POST`   | `/api/ws/groups`                                 |
| 查询群组详情 | `GET`    | `/api/ws/groups/{groupId}`                       |
| 查询群成员   | `GET`    | `/api/ws/groups/{groupId}/members`               |
| 添加成员     | `POST`   | `/api/ws/groups/{groupId}/members`               |
| 退出群组     | `DELETE` | `/api/ws/groups/{groupId}/members/me`            |
| 踢出成员     | `DELETE` | `/api/ws/groups/{groupId}/members/{userId}`      |
| 禁言成员     | `PUT`    | `/api/ws/groups/{groupId}/members/{userId}/mute` |
| 解除禁言     | `DELETE` | `/api/ws/groups/{groupId}/members/{userId}/mute` |
| 解散群组     | `DELETE` | `/api/ws/groups/{groupId}`                       |
| 查询在线人数 | `GET`    | `/api/ws/groups/{groupId}/online-count`          |

创建群组请求示例：

```json
{
  "groupName": "项目协作群",
  "groupType": "PROJECT",
  "memberIds": ["10001", "10002", "10003"]
}
```

禁言请求示例：

```json
{
  "mutedUntil": 1777963200000,
  "reason": "发送违规内容"
}
```

群组管理接口建议如下：

| 建议               | 说明                                            |
| ------------------ | ----------------------------------------------- |
| HTTP 管理群组关系  | 不建议通过 WebSocket 修改群成员                 |
| 成员变更后推送事件 | 通过 `/topic/group/{groupId}/status` 通知客户端 |
| 缓存及时失效       | 群成员缓存必须更新或删除                        |
| 权限严格校验       | 群主、管理员、普通成员权限不同                  |
| 操作写日志         | 踢人、禁言、解散必须审计                        |

### 通知管理接口

通知管理接口用于通知列表查询、详情查看、已读、批量已读、全部已读、删除、撤销和管理端发布通知。通知实时到达使用 WebSocket，历史列表和状态修改使用 HTTP API。

推荐接口：

| 功能         | 方法     | 地址                                   |
| ------------ | -------- | -------------------------------------- |
| 查询通知列表 | `GET`    | `/api/notices`                         |
| 查询通知详情 | `GET`    | `/api/notices/{noticeId}`              |
| 单条已读     | `PUT`    | `/api/notices/{noticeId}/read`         |
| 批量已读     | `PUT`    | `/api/notices/read-batch`              |
| 全部已读     | `PUT`    | `/api/notices/read-all`                |
| 未读数       | `GET`    | `/api/notices/unread-count`            |
| 删除通知     | `DELETE` | `/api/notices/{noticeId}`              |
| 发布通知     | `POST`   | `/api/admin/notices`                   |
| 撤销通知     | `PUT`    | `/api/admin/notices/{noticeId}/cancel` |

通知列表查询参数：

| 参数         | 说明                     |
| ------------ | ------------------------ |
| `status`     | `UNREAD`、`READ`、`DONE` |
| `noticeType` | 通知类型                 |
| `level`      | 通知级别                 |
| `bizType`    | 业务类型                 |
| `pageNum`    | 页码                     |
| `pageSize`   | 每页数量                 |

返回示例：

```json
{
  "total": 1,
  "records": [
    {
      "noticeId": "notice-100001",
      "title": "审批通知",
      "content": "你有一条新的审批待办",
      "noticeType": "APPROVAL",
      "level": "IMPORTANT",
      "status": "UNREAD",
      "url": "/approval/detail/100001",
      "createdAt": 1777956000000
    }
  ]
}
```

通知管理建议如下：

| 建议                 | 说明                          |
| -------------------- | ----------------------------- |
| 列表走 HTTP          | 不通过 WebSocket 拉取通知列表 |
| 实时变更走 WebSocket | 新通知、未读数变化实时推送    |
| 已读状态幂等         | 重复已读请求不应报错          |
| 管理端发布要限流     | 防止误发全站广播              |
| 通知内容要脱敏       | 不返回无权限业务详情          |
| 批量操作分页处理     | 大量通知已读不要一次性大事务  |

### 管理端监控接口

管理端监控接口用于查看 WebSocket 服务运行状态，包括连接数、在线用户、Session、线程池、Broker、Redis、消息发送量、失败数、异常连接和慢消费者。

推荐接口：

| 功能         | 方法   | 地址                                      |
| ------------ | ------ | ----------------------------------------- |
| 监控总览     | `GET`  | `/api/admin/ws/metrics/summary`           |
| 在线连接     | `GET`  | `/api/admin/ws/sessions`                  |
| 在线用户     | `GET`  | `/api/admin/ws/online-users`              |
| 用户会话     | `GET`  | `/api/admin/ws/users/{userId}/sessions`   |
| 强制下线     | `POST` | `/api/admin/ws/sessions/{sessionId}/kick` |
| 用户全端下线 | `POST` | `/api/admin/ws/users/{userId}/kick`       |
| 推送测试     | `POST` | `/api/admin/ws/push/test`                 |
| 线程池状态   | `GET`  | `/api/admin/ws/thread-pools`              |
| 失败消息     | `GET`  | `/api/admin/ws/failed-messages`           |
| 异常连接     | `GET`  | `/api/admin/ws/abnormal-sessions`         |
| Broker 状态  | `GET`  | `/api/admin/ws/broker/status`             |

监控总览返回示例：

```json
{
  "onlineUserCount": 128,
  "sessionCount": 236,
  "todayMessageSendCount": 10240,
  "todayPushFailedCount": 3,
  "brokerStatus": "UP",
  "redisStatus": "UP",
  "inboundQueueSize": 12,
  "outboundQueueSize": 35,
  "slowConsumerCount": 2,
  "timestamp": 1777956000000
}
```

管理端监控接口建议如下：

| 建议           | 说明                                    |
| -------------- | --------------------------------------- |
| 必须管理端鉴权 | 所有 `/api/admin/ws/**` 接口都需要权限  |
| 敏感字段脱敏   | IP、设备 ID、用户信息按权限展示         |
| 支持分页和筛选 | Session、失败消息、日志可能很多         |
| 操作必须审计   | 踢人、广播、推送测试要写操作日志        |
| 监控数据可缓存 | 总览数据可以 3 到 5 秒缓存              |
| 异常指标要告警 | Broker 断开、失败率升高、队列堆积要告警 |

## 管理端功能

管理端功能用于支撑 WebSocket 系统的运维、监控、排障和运营能力，包括在线连接查看、在线用户查看、用户会话查看、强制下线、消息推送测试、广播公告、群组状态查看、消息发送记录、异常连接记录和实时监控面板。

### 在线连接查看

在线连接查看用于展示当前 WebSocket 集群中的所有连接，包括 Session ID、用户 ID、客户端类型、设备 ID、IP、连接时间、最后活跃时间和所属实例。

展示字段建议如下：

| 字段             | 说明                 |
| ---------------- | -------------------- |
| `sessionId`      | WebSocket Session ID |
| `userId`         | 用户 ID              |
| `clientType`     | 客户端类型           |
| `deviceId`       | 设备 ID              |
| `ip`             | 客户端 IP            |
| `instanceId`     | 所属服务实例         |
| `connectTime`    | 连接时间             |
| `lastActiveTime` | 最后活跃时间         |
| `status`         | 连接状态             |

查询接口：

```text
GET /api/admin/ws/sessions?pageNum=1&pageSize=20&userId=10001&clientType=WEB
```

返回示例：

```json
{
  "total": 1,
  "records": [
    {
      "sessionId": "session-001",
      "userId": "10001",
      "clientType": "WEB",
      "deviceId": "browser-001",
      "ip": "192.168.1.10",
      "instanceId": "ws-node-01",
      "connectTime": 1777956000000,
      "lastActiveTime": 1777956060000,
      "status": "CONNECTED"
    }
  ]
}
```

在线连接查看建议如下：

| 建议             | 说明                            |
| ---------------- | ------------------------------- |
| 支持按用户筛选   | 排查某个用户连接问题            |
| 支持按实例筛选   | 判断负载是否均衡                |
| 支持按客户端筛选 | 区分 WEB、APP、ADMIN            |
| IP 可脱敏展示    | 普通管理员不展示完整 IP         |
| 支持踢出操作     | 每条连接可触发指定 Session 下线 |

### 在线用户查看

在线用户查看用于从用户维度展示当前在线状态。一个用户可能存在多个 Session，因此在线用户列表应展示用户聚合状态，而不是简单等同于连接列表。

展示字段建议如下：

| 字段           | 说明           |
| -------------- | -------------- |
| `userId`       | 用户 ID        |
| `nickname`     | 用户昵称       |
| `status`       | 在线状态       |
| `sessionCount` | 当前连接数     |
| `clientTypes`  | 在线客户端类型 |
| `onlineAt`     | 最近上线时间   |
| `lastActiveAt` | 最后活跃时间   |
| `tenantId`     | 租户 ID，可选  |

接口：

```text
GET /api/admin/ws/online-users?pageNum=1&pageSize=20&keyword=张三
```

在线用户查看建议如下：

| 建议             | 说明                       |
| ---------------- | -------------------------- |
| 按用户聚合       | 同一用户多端在线只显示一条 |
| 可展开连接列表   | 点击用户后查看 Session     |
| 支持强制全端下线 | 从用户维度踢出所有连接     |
| 支持租户筛选     | 多租户系统必须隔离         |
| 支持在线时长排序 | 便于排查长期连接           |

### 用户会话查看

用户会话查看用于查看某个用户当前所有在线连接，以及历史连接记录。该功能常用于排查用户收不到消息、重复收到消息、多端登录异常和强制下线问题。

推荐接口：

```text
GET /api/admin/ws/users/{userId}/sessions
GET /api/admin/ws/users/{userId}/session-logs?pageNum=1&pageSize=20
```

返回示例：

```json
{
  "userId": "10001",
  "online": true,
  "sessionCount": 2,
  "sessions": [
    {
      "sessionId": "session-web-001",
      "clientType": "WEB",
      "deviceId": "browser-001",
      "ip": "192.168.1.10",
      "instanceId": "ws-node-01",
      "connectTime": 1777956000000
    },
    {
      "sessionId": "session-app-002",
      "clientType": "APP",
      "deviceId": "iphone-001",
      "ip": "192.168.1.11",
      "instanceId": "ws-node-02",
      "connectTime": 1777956100000
    }
  ]
}
```

用户会话查看建议如下：

| 场景             | 用途                           |
| ---------------- | ------------------------------ |
| 用户重复收到消息 | 查看是否多端或多标签页连接     |
| 用户收不到消息   | 查看是否在线、连接在哪个实例   |
| 强制下线         | 按 Session、设备或全部连接下线 |
| 安全排查         | 查看异常 IP 或设备             |
| 多端策略排查     | 判断同端互踢是否生效           |

### 强制下线

强制下线用于管理端踢出某个 Session、某台设备或某个用户的全部连接。强制下线必须写操作日志，并向客户端发送明确下线原因。

推荐接口：

```text
POST /api/admin/ws/sessions/{sessionId}/kick
POST /api/admin/ws/users/{userId}/kick
POST /api/admin/ws/devices/{deviceId}/kick
```

请求示例：

```json
{
  "reason": "管理员强制下线",
  "notifyUser": true
}
```

强制下线返回示例：

```json
{
  "success": true,
  "kickedSessionCount": 2,
  "message": "强制下线已执行"
}
```

强制下线控制器示例。

文件位置：`src/main/java/io/github/atengk/websocket/controller/admin/AdminWsSessionController.java`

```java
package io.github.atengk.websocket.controller.admin;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.websocket.service.WsSessionControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/**
 * 管理端 WebSocket 会话控制接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AdminWsSessionController {

    private final WsSessionControlService wsSessionControlService;

    /**
     * 踢出指定连接
     *
     * @param sessionId 会话 ID
     * @param reason    原因
     * @param principal 当前管理员
     * @return 处理结果
     */
    @PostMapping("/api/admin/ws/sessions/{sessionId}/kick")
    public Map<String, Object> kickSession(@PathVariable String sessionId,
                                           String reason,
                                           Principal principal) {
        String operatorId = principal == null ? "unknown" : principal.getName();
        String kickReason = StrUtil.blankToDefault(reason, "管理员强制下线");

        wsSessionControlService.kickSession(sessionId, kickReason);

        log.info("管理端踢出指定连接，operatorId：{}，sessionId：{}，reason：{}",
                operatorId, sessionId, kickReason);

        return MapUtil.<String, Object>builder()
                .put("success", true)
                .put("message", "强制下线已执行")
                .build();
    }

    /**
     * 踢出用户全部连接
     *
     * @param userId    用户 ID
     * @param reason    原因
     * @param principal 当前管理员
     * @return 处理结果
     */
    @PostMapping("/api/admin/ws/users/{userId}/kick")
    public Map<String, Object> kickUser(@PathVariable String userId,
                                        String reason,
                                        Principal principal) {
        String operatorId = principal == null ? "unknown" : principal.getName();
        String kickReason = StrUtil.blankToDefault(reason, "管理员强制下线");

        wsSessionControlService.kickUser(userId, kickReason);

        log.info("管理端踢出用户全部连接，operatorId：{}，userId：{}，reason：{}",
                operatorId, userId, kickReason);

        return MapUtil.<String, Object>builder()
                .put("success", true)
                .put("message", "用户全部连接已触发下线")
                .build();
    }
}
```

强制下线建议如下：

| 建议                     | 说明                            |
| ------------------------ | ------------------------------- |
| 管理端权限校验           | 只有管理员或运维可操作          |
| 操作必须审计             | 记录 operatorId、target、reason |
| 支持指定 Session         | 不影响同用户其他设备            |
| 支持用户全端下线         | 用于封禁、改密、风险处理        |
| 集群下通过 Pub/Sub 转发  | 目标 Session 可能在其他实例     |
| 客户端收到通知后主动断开 | 推荐通知式下线                  |

### 消息推送测试

消息推送测试用于管理端验证 WebSocket 连接、用户队列、广播主题、群组主题、消息格式、Broker 和前端处理逻辑是否正常。

推荐测试类型：

| 类型         | 推送目标                         |
| ------------ | -------------------------------- |
| 指定用户通知 | `/user/queue/notice`             |
| 指定用户错误 | `/user/queue/errors`             |
| 系统公告     | `/topic/system/notice`           |
| 管理端看板   | `/topic/admin/dashboard/message` |
| 群组消息     | `/topic/group/{groupId}`         |
| 房间人数     | `/topic/room/{roomId}/count`     |

接口：

```text
POST /api/admin/ws/push/test
```

请求示例：

```json
{
  "targetType": "USER",
  "targetId": "10001",
  "destination": "/queue/notice",
  "title": "测试通知",
  "content": "这是一条管理端推送测试消息"
}
```

推送测试建议如下：

| 建议           | 说明                         |
| -------------- | ---------------------------- |
| 仅管理端可用   | 防止普通用户滥用             |
| 必须限流       | 防止误操作大量推送           |
| 标记测试消息   | `type` 或 `data.test=true`   |
| 不携带敏感数据 | 测试消息内容可控             |
| 写操作日志     | 记录推送目标和操作人         |
| 支持 dryRun    | 只校验目标和连接，不真正推送 |

### 广播公告

广播公告用于管理端发布系统公告、维护通知、版本升级提醒、紧急通知等消息。公告通常需要落库，WebSocket 广播只负责实时触达。

推荐接口：

```text
POST /api/admin/ws/announcements
PUT /api/admin/ws/announcements/{noticeId}/cancel
GET /api/admin/ws/announcements
```

发布请求示例：

```json
{
  "title": "系统维护通知",
  "content": "系统将在今晚 23:00 至 23:30 进行维护",
  "level": "IMPORTANT",
  "scopeType": "ALL",
  "scopeIds": [],
  "forcePopup": true,
  "publishAt": 1777956000000,
  "expireAt": 1778042400000
}
```

广播公告流程：

```text
1. 管理端提交公告
2. 服务端校验管理员权限
3. 公告内容安全校验和长度校验
4. 公告记录落库
5. 如果立即发布，事务提交后广播 /topic/system/notice
6. 如果定时发布，由定时任务到期推送
7. 用户已读状态通过通知接口维护
```

广播公告建议如下：

| 建议               | 说明                       |
| ------------------ | -------------------------- |
| 公告必须落库       | 离线用户也能查看           |
| 重要公告支持强弹窗 | 前端根据 `forcePopup` 处理 |
| 支持范围控制       | ALL、TENANT、ROLE、USER    |
| 支持定时发布       | publishAt 到期后推送       |
| 支持撤销           | 撤销后推送取消事件         |
| 发布操作审计       | 记录发布人、范围、内容摘要 |

### 群组状态查看

群组状态查看用于管理端查看群组基础信息、成员数量、在线人数、消息量、禁言成员、异常状态和最近活跃时间。

推荐接口：

```text
GET /api/admin/ws/groups
GET /api/admin/ws/groups/{groupId}
GET /api/admin/ws/groups/{groupId}/members
GET /api/admin/ws/groups/{groupId}/online-members
GET /api/admin/ws/groups/{groupId}/messages
```

群组状态字段建议如下：

| 字段                | 说明         |
| ------------------- | ------------ |
| `groupId`           | 群组 ID      |
| `groupName`         | 群组名称     |
| `groupType`         | 群组类型     |
| `ownerId`           | 群主         |
| `memberCount`       | 成员数       |
| `onlineMemberCount` | 在线成员数   |
| `mutedMemberCount`  | 禁言成员数   |
| `todayMessageCount` | 今日消息数   |
| `status`            | 群组状态     |
| `lastMessageAt`     | 最近消息时间 |

返回示例：

```json
{
  "groupId": "90001",
  "groupName": "项目协作群",
  "groupType": "PROJECT",
  "ownerId": "10001",
  "memberCount": 128,
  "onlineMemberCount": 36,
  "mutedMemberCount": 2,
  "todayMessageCount": 320,
  "status": "NORMAL",
  "lastMessageAt": 1777956000000
}
```

群组状态查看建议如下：

| 建议               | 说明                       |
| ------------------ | -------------------------- |
| 在线人数来自 Redis | 实时数据不直接查数据库     |
| 成员关系来自数据库 | Redis 缓存只做加速         |
| 支持管理操作入口   | 禁言、踢人、解散           |
| 大群成员分页       | 不一次返回全部成员         |
| 敏感群组权限隔离   | 租户管理员只能看本租户群组 |

### 消息发送记录

消息发送记录用于管理端查询 WebSocket 消息发送历史、状态、目标、失败原因、重试次数和链路追踪 ID。它是排查“用户没收到消息”“消息重复”“推送失败”的关键功能。

推荐接口：

```text
GET /api/admin/ws/messages
GET /api/admin/ws/messages/{messageId}
GET /api/admin/ws/messages/{messageId}/receipts
GET /api/admin/ws/failed-messages
GET /api/admin/ws/dead-messages
```

查询参数建议：

| 参数          | 说明           |
| ------------- | -------------- |
| `messageId`   | 消息 ID        |
| `messageType` | 消息类型       |
| `receiverId`  | 接收人 ID      |
| `destination` | STOMP 目标地址 |
| `status`      | 消息状态       |
| `bizType`     | 业务类型       |
| `bizId`       | 业务 ID        |
| `startTime`   | 开始时间       |
| `endTime`     | 结束时间       |

返回示例：

```json
{
  "messageId": "1909550919275922401",
  "messageType": "USER_NOTICE",
  "destination": "/user/queue/notice",
  "receiverType": "USER",
  "receiverId": "10001",
  "bizType": "APPROVAL",
  "bizId": "approval-100001",
  "status": "SENT",
  "needAck": true,
  "sentAt": 1777956000000,
  "traceId": "trace-20260505-000001"
}
```

消息发送记录建议如下：

| 建议               | 说明                       |
| ------------------ | -------------------------- |
| 关键消息必须记录   | 审批、订单、告警、任务结果 |
| 支持按业务 ID 查询 | 便于从业务单据定位推送     |
| 支持查看回执       | 判断客户端是否收到或已读   |
| 失败消息单独展示   | 支持重试和转死信           |
| payload 脱敏展示   | 不展示敏感数据             |
| 历史数据归档       | 大表按时间归档             |

### 异常连接记录

异常连接记录用于记录认证失败、授权失败、频率超限、心跳超时、异常断开、黑名单命中、非法订阅、非法发送等行为。它既服务于排障，也服务于安全风控。

推荐接口：

```text
GET /api/admin/ws/abnormal-sessions
GET /api/admin/ws/security-audit-logs
GET /api/admin/ws/security-audit-logs/{auditId}
```

异常连接字段建议：

| 字段          | 说明         |
| ------------- | ------------ |
| `auditId`     | 审计 ID      |
| `eventType`   | 异常事件类型 |
| `userId`      | 用户 ID      |
| `sessionId`   | Session ID   |
| `ip`          | 客户端 IP    |
| `clientType`  | 客户端类型   |
| `deviceId`    | 设备 ID      |
| `destination` | 目标地址     |
| `riskLevel`   | 风险级别     |
| `content`     | 内容摘要     |
| `traceId`     | 链路追踪 ID  |
| `createdAt`   | 发生时间     |

异常类型建议：

| 类型                  | 说明       |
| --------------------- | ---------- |
| `AUTH_FAILED`         | 认证失败   |
| `SUBSCRIBE_DENIED`    | 订阅被拒绝 |
| `SEND_DENIED`         | 发送被拒绝 |
| `RATE_LIMITED`        | 触发限流   |
| `BLACKLIST_HIT`       | 命中黑名单 |
| `HEARTBEAT_TIMEOUT`   | 心跳超时   |
| `MESSAGE_TOO_LARGE`   | 消息过大   |
| `XSS_RISK_DETECTED`   | XSS 风险   |
| `ABNORMAL_DISCONNECT` | 异常断开   |

异常连接记录建议如下：

| 建议             | 说明                        |
| ---------------- | --------------------------- |
| 高频异常聚合     | 防止日志和数据库刷爆        |
| 支持按 IP 查询   | 排查攻击来源                |
| 支持按用户查询   | 排查账号风险                |
| 支持自动封禁联动 | 风险分高时封禁              |
| 敏感字段脱敏     | 不存完整 Token、Cookie      |
| 高风险事件告警   | 管理端越权、XSS、爆破要告警 |

### 实时监控面板

实时监控面板用于管理端展示 WebSocket 系统健康状态，包括连接数、在线用户数、消息发送量、失败率、线程池队列、Broker 状态、Redis 状态、异常连接和慢消费者。

推荐面板模块如下：

| 模块     | 指标                                     |
| -------- | ---------------------------------------- |
| 连接概览 | 当前连接数、在线用户数、今日连接峰值     |
| 消息概览 | 今日发送量、发送成功数、失败数、失败率   |
| 线程池   | 入站队列、出站队列、推送线程池、拒绝次数 |
| Broker   | 连接状态、队列积压、心跳状态             |
| Redis    | 延迟、在线用户 Key、Session Key 数量     |
| 异常行为 | 认证失败、非法订阅、限流次数             |
| 慢消费者 | 慢连接数量、出站延迟                     |
| 群组监控 | 活跃群组数、群消息量、房间人数           |
| 告警列表 | 最新告警、未处理死信、失败消息           |

实时监控面板推送路径：

```text
/topic/admin/dashboard/message
/topic/admin/online
/topic/admin/alarm
```

监控推送消息示例：

```json
{
  "messageId": "1909550919275922402",
  "type": "ADMIN_DASHBOARD",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "onlineUserCount": 128,
    "sessionCount": 236,
    "messageSendCount": 10240,
    "pushFailedCount": 3,
    "inboundQueueSize": 12,
    "outboundQueueSize": 35,
    "slowConsumerCount": 2,
    "brokerStatus": "UP",
    "redisStatus": "UP"
  }
}
```

实时监控面板建议如下：

| 建议             | 说明                                  |
| ---------------- | ------------------------------------- |
| 管理端专用 Topic | 使用 `/topic/admin/**`                |
| 严格订阅鉴权     | 普通用户不能订阅监控数据              |
| 推送频率控制     | 3 到 10 秒一次即可                    |
| 指标可聚合       | 不需要每条事件都实时展示              |
| 支持手动刷新     | WebSocket 异常时可 HTTP 拉取          |
| 异常指标高亮     | 失败率、队列积压、Broker 异常需要突出 |
| 支持下钻         | 从总览跳转到连接、消息、异常详情      |

## 日志设计

日志设计用于记录 WebSocket STOMP 在连接、认证、订阅、发送、接收、推送、异常、安全审计和性能分析过程中的关键行为。日志既要能支持问题排查，也要避免高频实时消息导致日志量爆炸。生产环境建议采用“关键事件必记、高频消息采样、敏感字段脱敏、异常链路带 traceId”的原则。

### 连接日志

连接日志用于记录客户端建立 WebSocket 和 STOMP 连接的过程。连接日志应覆盖握手阶段和 `CONNECT` 阶段，便于排查连接失败、来源异常、Token 问题和客户端版本问题。

连接日志建议字段如下：

| 字段          | 说明                 |
| ------------- | -------------------- |
| `traceId`     | 链路追踪 ID          |
| `sessionId`   | WebSocket Session ID |
| `userId`      | 用户 ID              |
| `ip`          | 客户端 IP            |
| `origin`      | 请求来源             |
| `clientType`  | 客户端类型           |
| `deviceId`    | 设备 ID              |
| `userAgent`   | 浏览器或客户端标识   |
| `connectTime` | 连接时间             |
| `instanceId`  | 当前服务实例         |

推荐日志格式：

```text
WebSocket 连接成功，traceId：{}，userId：{}，sessionId：{}，clientType：{}，deviceId：{}，ip：{}，instanceId：{}
```

连接日志示例：

```java
log.info("WebSocket 连接成功，traceId：{}，userId：{}，sessionId：{}，clientType：{}，deviceId：{}，ip：{}，instanceId：{}",
        traceId, userId, sessionId, clientType, deviceId, ip, instanceId);
```

连接日志建议如下：

| 建议              | 说明                           |
| ----------------- | ------------------------------ |
| 连接成功记录 info | 便于审计和排查                 |
| 连接失败记录 warn | 包含失败原因                   |
| Token 必须脱敏    | 不允许打印完整 Token           |
| 高频连接失败聚合  | 防止攻击时日志爆炸             |
| 记录 instanceId   | 集群排查必须知道连接在哪个实例 |

### 断开日志

断开日志用于记录客户端主动断开、网络异常断开、心跳超时、服务端强制下线、网关关闭连接等场景。断开日志是排查“用户为什么掉线”的主要依据。

断开日志建议字段如下：

| 字段              | 说明         |
| ----------------- | ------------ |
| `sessionId`       | Session ID   |
| `userId`          | 用户 ID      |
| `closeStatus`     | 关闭状态     |
| `reason`          | 断开原因     |
| `connectTime`     | 连接时间     |
| `disconnectTime`  | 断开时间     |
| `durationSeconds` | 连接持续时间 |
| `lastActiveTime`  | 最后活跃时间 |
| `instanceId`      | 实例 ID      |

推荐日志格式：

```text
WebSocket 连接断开，userId：{}，sessionId：{}，closeStatus：{}，reason：{}，durationSeconds：{}
```

断开日志示例：

```java
log.info("WebSocket 连接断开，userId：{}，sessionId：{}，closeStatus：{}，reason：{}，durationSeconds：{}",
        userId, sessionId, closeStatus, reason, durationSeconds);
```

断开原因建议标准化：

| 原因                | 说明           |
| ------------------- | -------------- |
| `CLIENT_DISCONNECT` | 客户端主动断开 |
| `HEARTBEAT_TIMEOUT` | 心跳超时       |
| `TOKEN_EXPIRED`     | Token 过期     |
| `SESSION_KICKED`    | 被强制下线     |
| `NETWORK_ERROR`     | 网络异常       |
| `SERVER_SHUTDOWN`   | 服务端关闭     |
| `BROKER_ERROR`      | Broker 异常    |

### 认证日志

认证日志用于记录 STOMP `CONNECT` 阶段的身份认证结果。认证日志属于安全日志的一部分，必须记录失败原因，但不能泄露 Token、密码、Cookie 等敏感信息。

认证日志建议字段如下：

| 字段          | 说明                       |
| ------------- | -------------------------- |
| `traceId`     | 链路追踪 ID                |
| `sessionId`   | Session ID                 |
| `userId`      | 成功时记录用户 ID          |
| `ip`          | 客户端 IP                  |
| `clientType`  | 客户端类型                 |
| `deviceId`    | 设备 ID                    |
| `authResult`  | 成功或失败                 |
| `failCode`    | 失败错误码                 |
| `tokenDigest` | Token 摘要，不是完整 Token |

认证成功日志：

```java
log.info("WebSocket 认证成功，traceId：{}，userId：{}，sessionId：{}，clientType：{}，ip：{}",
        traceId, userId, sessionId, clientType, ip);
```

认证失败日志：

```java
log.warn("WebSocket 认证失败，traceId：{}，sessionId：{}，ip：{}，clientType：{}，failCode：{}，token：{}",
        traceId, sessionId, ip, clientType, failCode, WsSensitiveMaskUtil.maskToken(token));
```

认证日志建议如下：

| 建议             | 说明                         |
| ---------------- | ---------------------------- |
| 成功记录 info    | 用于连接审计                 |
| 失败记录 warn    | 高频失败可能是攻击           |
| Token 脱敏       | 只记录摘要                   |
| 失败码标准化     | 便于统计和告警               |
| 高频失败进入风控 | 认证失败次数超过阈值触发封禁 |

### 订阅日志

订阅日志用于记录客户端 `SUBSCRIBE` 行为，包括订阅成功、订阅失败、非法订阅、权限拒绝和高频订阅。订阅日志对排查“用户为什么收不到某类消息”非常关键。

订阅日志建议字段如下：

| 字段             | 说明          |
| ---------------- | ------------- |
| `userId`         | 用户 ID       |
| `sessionId`      | Session ID    |
| `destination`    | 订阅地址      |
| `subscriptionId` | STOMP 订阅 ID |
| `result`         | 成功或失败    |
| `reason`         | 失败原因      |
| `clientType`     | 客户端类型    |
| `traceId`        | 链路追踪 ID   |

订阅成功日志：

```java
log.info("WebSocket 订阅成功，userId：{}，sessionId：{}，destination：{}，subscriptionId：{}",
        userId, sessionId, destination, subscriptionId);
```

订阅失败日志：

```java
log.warn("WebSocket 订阅被拒绝，userId：{}，sessionId：{}，destination：{}，reason：{}",
        userId, sessionId, destination, reason);
```

订阅日志建议如下：

| 场景           | 日志级别          |
| -------------- | ----------------- |
| 正常订阅       | `info` 或采样记录 |
| 页面高频订阅   | `warn`            |
| 非法目标地址   | `warn`            |
| 无权限订阅     | `warn`            |
| 管理端越权订阅 | `warn` 或安全审计 |
| 系统异常       | `error`           |

对于大规模系统，正常订阅日志可以降级为 debug 或采样记录，否则用户频繁切换页面会产生大量日志。

### 发送日志

发送日志用于记录客户端通过 `SEND` 向服务端发送业务消息的行为。发送日志不等同于推送日志，它关注“客户端向服务端发送了什么请求”。

发送日志建议字段如下：

| 字段              | 说明                      |
| ----------------- | ------------------------- |
| `messageId`       | 服务端消息 ID，生成后记录 |
| `clientMessageId` | 客户端消息 ID             |
| `userId`          | 发送人                    |
| `sessionId`       | 当前连接                  |
| `destination`     | 发送地址                  |
| `messageType`     | 消息类型                  |
| `payloadSize`     | 消息体大小                |
| `result`          | 处理结果                  |
| `costMillis`      | 处理耗时                  |

发送成功日志：

```java
log.info("WebSocket 消息发送处理成功，userId：{}，sessionId：{}，destination：{}，messageId：{}，costMillis：{}",
        userId, sessionId, destination, messageId, costMillis);
```

发送失败日志：

```java
log.warn("WebSocket 消息发送处理失败，userId：{}，sessionId：{}，destination：{}，reason：{}",
        userId, sessionId, destination, reason);
```

发送日志建议如下：

| 建议              | 说明                         |
| ----------------- | ---------------------------- |
| 不打印完整消息体  | 只记录摘要、大小和 messageId |
| 聊天高频日志采样  | 避免日志过量                 |
| 关键业务必记      | 审批、订单、任务结果需要记录 |
| 失败必须记录      | 包含失败原因和 traceId       |
| 超大消息记录 warn | 可能是攻击或前端异常         |

### 接收日志

接收日志用于记录服务端接收到客户端 STOMP 消息的情况。它比发送日志更底层，通常用于通道级排查，包括 `CONNECT`、`SUBSCRIBE`、`SEND`、`DISCONNECT` 等命令。

接收日志建议字段如下：

| 字段          | 说明        |
| ------------- | ----------- |
| `command`     | STOMP 命令  |
| `sessionId`   | Session ID  |
| `userId`      | 用户 ID     |
| `destination` | 目标地址    |
| `payloadSize` | 消息体大小  |
| `headers`     | Header 摘要 |
| `traceId`     | 链路追踪 ID |

通道接收日志建议不要默认全部 info 打印。高并发系统中，每个 STOMP 帧都打印日志会明显影响性能。

建议策略：

| 场景         | 日志策略                          |
| ------------ | --------------------------------- |
| `CONNECT`    | info                              |
| `DISCONNECT` | info                              |
| `SUBSCRIBE`  | info 或采样                       |
| `SEND`       | 业务关键消息 info，普通消息 debug |
| 心跳         | 不打印                            |
| 异常帧       | warn                              |
| 解析失败     | warn 或 error                     |

### 异常日志

异常日志用于记录 WebSocket 生命周期、通道拦截、业务处理、推送、序列化、Broker、Redis、线程池等异常。异常日志必须包含足够上下文，否则难以定位问题。

异常日志建议字段如下：

| 字段           | 说明        |
| -------------- | ----------- |
| `traceId`      | 链路追踪 ID |
| `messageId`    | 消息 ID     |
| `userId`       | 用户 ID     |
| `sessionId`    | Session ID  |
| `destination`  | 目标地址    |
| `errorCode`    | 错误码      |
| `errorMessage` | 错误摘要    |
| `retryable`    | 是否可重试  |

异常日志示例：

```java
log.error("WebSocket 消息处理异常，traceId：{}，userId：{}，sessionId：{}，destination：{}，messageId：{}",
        traceId, userId, sessionId, destination, messageId, exception);
```

异常日志建议如下：

| 建议                  | 说明                   |
| --------------------- | ---------------------- |
| 未知异常打 error      | 需要完整堆栈           |
| 业务可预期异常打 warn | 例如权限不足、参数错误 |
| 高频异常要聚合        | 防止同类异常刷屏       |
| 前端只返回 traceId    | 不返回内部堆栈         |
| 推送失败要入库        | 关键消息不能只记录日志 |

### 推送日志

推送日志用于记录服务端向客户端、用户、群组、广播主题、管理端主题推送消息的行为。推送日志关注“服务端向哪里推送了什么”。

推送日志建议字段如下：

| 字段           | 说明                |
| -------------- | ------------------- |
| `messageId`    | 消息 ID             |
| `messageType`  | 消息类型            |
| `destination`  | 推送目标地址        |
| `receiverType` | 接收方类型          |
| `receiverId`   | 接收方 ID           |
| `sessionId`    | 指定 Session 时记录 |
| `payloadSize`  | 消息体大小          |
| `result`       | 成功或失败          |
| `costMillis`   | 推送耗时            |

推送成功日志：

```java
log.info("WebSocket 推送成功，messageId：{}，messageType：{}，receiverType：{}，receiverId：{}，destination：{}，costMillis：{}",
        messageId, messageType, receiverType, receiverId, destination, costMillis);
```

推送失败日志：

```java
log.error("WebSocket 推送失败，messageId：{}，receiverType：{}，receiverId：{}，destination：{}，reason：{}",
        messageId, receiverType, receiverId, destination, reason, exception);
```

推送日志建议如下：

| 场景         | 策略                      |
| ------------ | ------------------------- |
| 普通聊天推送 | debug 或采样              |
| 关键通知推送 | info                      |
| 推送失败     | error，并写失败消息表     |
| 批量推送     | 记录批次统计，不逐条 info |
| 广播推送     | 记录广播范围和目标地址    |
| 看板推送     | 采样或 debug              |

### 安全审计日志

安全审计日志用于记录认证失败、授权失败、非法订阅、非法发送、黑名单命中、限流、强制下线、管理端操作、敏感内容命中等安全相关事件。

安全审计事件建议如下：

| 事件                | 说明            |
| ------------------- | --------------- |
| `AUTH_FAILED`       | 认证失败        |
| `SUBSCRIBE_DENIED`  | 订阅被拒绝      |
| `SEND_DENIED`       | 发送被拒绝      |
| `BLACKLIST_HIT`     | 命中黑名单      |
| `RATE_LIMITED`      | 触发限流        |
| `XSS_RISK_DETECTED` | 检测到 XSS 风险 |
| `USER_BANNED`       | 用户被封禁      |
| `KICK_SESSION`      | 踢出连接        |
| `ADMIN_BROADCAST`   | 管理端广播      |
| `ADMIN_PUSH_TEST`   | 管理端推送测试  |

安全审计日志建议字段：

| 字段          | 说明       |
| ------------- | ---------- |
| `auditId`     | 审计 ID    |
| `eventType`   | 事件类型   |
| `riskLevel`   | 风险级别   |
| `userId`      | 用户 ID    |
| `sessionId`   | Session ID |
| `ip`          | IP         |
| `destination` | 目标地址   |
| `operatorId`  | 操作人     |
| `content`     | 内容摘要   |
| `traceId`     | 链路 ID    |

安全审计记录服务接口如下。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsSecurityAuditLogService.java`

```java
package io.github.atengk.websocket.service;

import java.util.Map;

/**
 * WebSocket 安全审计日志服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface WsSecurityAuditLogService {

    /**
     * 记录安全审计事件
     *
     * @param eventType 事件类型
     * @param riskLevel 风险级别
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param content   内容摘要
     * @param extra     扩展信息
     */
    void record(String eventType,
                String riskLevel,
                String userId,
                String sessionId,
                String content,
                Map<String, Object> extra);
}
```

基础实现示例，实际项目中可以扩展为写数据库、Kafka 或日志平台。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsSecurityAuditLogServiceImpl.java`

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.websocket.service.WsSecurityAuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * WebSocket 安全审计日志服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class WsSecurityAuditLogServiceImpl implements WsSecurityAuditLogService {

    /**
     * 记录安全审计事件
     *
     * @param eventType 事件类型
     * @param riskLevel 风险级别
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param content   内容摘要
     * @param extra     扩展信息
     */
    @Override
    public void record(String eventType,
                       String riskLevel,
                       String userId,
                       String sessionId,
                       String content,
                       Map<String, Object> extra) {
        String auditId = String.valueOf(IdUtil.getSnowflakeNextId());
        Map<String, Object> extraMap = MapUtil.emptyIfNull(extra);

        log.warn("WebSocket 安全审计，auditId：{}，eventType：{}，riskLevel：{}，userId：{}，sessionId：{}，content：{}，extra：{}",
                auditId, eventType, riskLevel, userId, sessionId, content, extraMap);
    }
}
```

### 性能慢日志

性能慢日志用于记录 WebSocket 消息处理耗时、推送耗时、订阅鉴权耗时、Redis 查询耗时、Broker 推送耗时、数据库查询耗时等超过阈值的场景。

慢日志建议阈值如下：

| 场景           | 建议阈值 |
| -------------- | -------- |
| CONNECT 认证   | 200ms    |
| SUBSCRIBE 鉴权 | 100ms    |
| SEND 业务处理  | 300ms    |
| 单条推送       | 100ms    |
| 批量推送       | 1000ms   |
| Redis 查询     | 50ms     |
| 数据库查询     | 200ms    |
| 补偿接口       | 500ms    |

慢日志示例：

```java
if (costMillis > 300) {
    log.warn("WebSocket 慢处理，userId：{}，sessionId：{}，destination：{}，messageId：{}，costMillis：{}",
            userId, sessionId, destination, messageId, costMillis);
}
```

可以封装一个慢日志工具类。

文件位置：`src/main/java/io/github/atengk/websocket/util/WsSlowLogUtil.java`

```java
package io.github.atengk.websocket.util;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 慢日志工具类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
public final class WsSlowLogUtil {

    private WsSlowLogUtil() {
    }

    /**
     * 记录慢处理日志
     *
     * @param scene       场景
     * @param thresholdMs 阈值毫秒
     * @param costMillis  实际耗时毫秒
     * @param detail      详情
     */
    public static void logIfSlow(String scene, long thresholdMs, long costMillis, String detail) {
        if (costMillis >= thresholdMs) {
            log.warn("WebSocket 慢日志，scene：{}，thresholdMs：{}，costMillis：{}，detail：{}",
                    scene, thresholdMs, costMillis, detail);
        }
    }
}
```

## 监控与可观测性

监控与可观测性用于持续观察 WebSocket STOMP 服务的运行状态，包括连接数、在线用户数、消息吞吐、推送失败率、消息延迟、线程池、Broker、Redis、JVM、Prometheus 指标、Grafana 看板和告警规则。没有监控的 WebSocket 系统很难定位连接假在线、推送延迟、Broker 堆积、慢消费者和线程池拥塞问题。

### 连接数指标

连接数指标用于观察当前 WebSocket 服务承载的长连接数量，包括当前连接数、新增连接数、断开连接数、连接峰值和异常连接数。

推荐指标如下：

| 指标                             | 类型    | 说明             |
| -------------------------------- | ------- | ---------------- |
| `ws_sessions_current`            | Gauge   | 当前连接数       |
| `ws_sessions_connected_total`    | Counter | 累计连接成功次数 |
| `ws_sessions_disconnected_total` | Counter | 累计断开次数     |
| `ws_sessions_abnormal_total`     | Counter | 异常断开次数     |
| `ws_sessions_kicked_total`       | Counter | 被踢下线次数     |
| `ws_sessions_timeout_total`      | Counter | 心跳超时次数     |

连接数指标维度建议：

| 标签         | 说明            |
| ------------ | --------------- |
| `instance`   | 实例 ID         |
| `clientType` | WEB、APP、ADMIN |
| `tenantId`   | 租户 ID，可选   |
| `reason`     | 断开原因        |
| `result`     | success、failed |

Micrometer 指标服务示例。

文件位置：`src/main/java/io/github/atengk/websocket/monitor/WsMetricsService.java`

```java
package io.github.atengk.websocket.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket 指标服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Component
@RequiredArgsConstructor
public class WsMetricsService {

    private final MeterRegistry meterRegistry;

    private final AtomicInteger currentSessions = new AtomicInteger(0);

    /**
     * 初始化连接数 Gauge
     */
    public void initSessionGauge() {
        meterRegistry.gauge("ws_sessions_current", currentSessions);
    }

    /**
     * 连接成功
     *
     * @param clientType 客户端类型
     */
    public void connect(String clientType) {
        currentSessions.incrementAndGet();
        Counter.builder("ws_sessions_connected_total")
                .tag("clientType", clientType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 连接断开
     *
     * @param clientType 客户端类型
     * @param reason     断开原因
     */
    public void disconnect(String clientType, String reason) {
        currentSessions.updateAndGet(value -> Math.max(0, value - 1));
        Counter.builder("ws_sessions_disconnected_total")
                .tag("clientType", clientType)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }
}
```

实际项目中，`currentSessions` 更建议从 SessionRegistry 或 Redis 定期同步，避免计数异常。

### 在线用户指标

在线用户指标用于观察当前在线用户数、多端连接数、在线设备数、群组在线人数、管理端在线人数等。

推荐指标如下：

| 指标                              | 类型  | 说明                 |
| --------------------------------- | ----- | -------------------- |
| `ws_online_users_current`         | Gauge | 当前在线用户数       |
| `ws_online_admin_users_current`   | Gauge | 当前管理端在线用户数 |
| `ws_online_devices_current`       | Gauge | 当前在线设备数       |
| `ws_group_online_users_current`   | Gauge | 群组在线用户数       |
| `ws_room_online_sessions_current` | Gauge | 房间在线连接数       |

在线用户指标建议从 Redis 聚合：

```text
SCARD ws:online:users
SCARD ws:group:online:{groupId}
SCARD ws:room:sessions:{roomId}
```

在线用户指标建议如下：

| 建议                 | 说明                               |
| -------------------- | ---------------------------------- |
| 从 Redis 获取全局值  | 多实例下不能只看本地内存           |
| 区分连接数和用户数   | 一个用户可能多个连接               |
| 管理端单独统计       | 管理端连接通常更敏感               |
| 群组指标只监控热点群 | 不建议为所有群创建高基数指标       |
| 避免高基数标签       | 不要把 userId 作为 Prometheus 标签 |

### 消息发送量指标

消息发送量指标用于统计服务端向客户端推送消息的数量，包括广播、点对点、群组、通知、聊天、告警、任务进度等。

推荐指标如下：

| 指标                           | 类型    | 说明               |
| ------------------------------ | ------- | ------------------ |
| `ws_messages_sent_total`       | Counter | 服务端推送消息总数 |
| `ws_messages_sent_bytes_total` | Counter | 推送消息总字节数   |
| `ws_messages_broadcast_total`  | Counter | 广播消息数         |
| `ws_messages_user_total`       | Counter | 用户点对点消息数   |
| `ws_messages_group_total`      | Counter | 群组消息数         |

推荐标签：

| 标签              | 说明               |
| ----------------- | ------------------ |
| `messageType`     | 消息类型           |
| `receiverType`    | USER、GROUP、TOPIC |
| `destinationType` | topic、queue、user |
| `result`          | success、failed    |

推送指标记录示例：

```java
Counter.builder("ws_messages_sent_total")
        .tag("messageType", messageType)
        .tag("receiverType", receiverType)
        .tag("result", "success")
        .register(meterRegistry)
        .increment();
```

消息发送量指标建议如下：

| 建议                              | 说明                   |
| --------------------------------- | ---------------------- |
| messageType 控制枚举数量          | 避免标签高基数         |
| 不使用 destination 全路径作为标签 | 群组 ID 会造成高基数   |
| 失败和成功分开统计                | 便于计算失败率         |
| 大批量推送记录批次数              | 不一定逐条打指标       |
| 统计字节数                        | 便于观察消息体过大问题 |

### 消息接收量指标

消息接收量指标用于统计客户端发送到服务端的 STOMP 消息数量，包括 `SEND`、`SUBSCRIBE`、`CONNECT`、`DISCONNECT`、ACK、已读、聊天发送等。

推荐指标如下：

| 指标                         | 类型    | 说明           |
| ---------------------------- | ------- | -------------- |
| `ws_messages_received_total` | Counter | 入站消息总数   |
| `ws_connect_total`           | Counter | CONNECT 数量   |
| `ws_subscribe_total`         | Counter | SUBSCRIBE 数量 |
| `ws_send_total`              | Counter | SEND 数量      |
| `ws_ack_total`               | Counter | ACK 数量       |
| `ws_invalid_messages_total`  | Counter | 非法消息数量   |

推荐标签：

| 标签              | 说明                    |
| ----------------- | ----------------------- |
| `command`         | STOMP 命令              |
| `destinationType` | app、topic、user、queue |
| `clientType`      | 客户端类型              |
| `result`          | success、failed         |
| `reason`          | 失败原因                |

消息接收量指标建议如下：

| 建议                     | 说明                    |
| ------------------------ | ----------------------- |
| CONNECT 和 SEND 分开统计 | 二者含义不同            |
| 非法消息单独统计         | 便于安全监控            |
| ACK 高频时可采样         | 防止指标压力过大        |
| 不使用 sessionId 标签    | 高基数会压垮 Prometheus |
| 订阅失败按原因统计       | 可发现权限配置问题      |

### 推送失败率

推送失败率用于衡量服务端向客户端推送消息失败的比例。它是 WebSocket 系统最重要的可靠性指标之一。

推荐指标如下：

| 指标                    | 类型    | 说明             |
| ----------------------- | ------- | ---------------- |
| `ws_push_success_total` | Counter | 推送成功次数     |
| `ws_push_failed_total`  | Counter | 推送失败次数     |
| `ws_push_retry_total`   | Counter | 推送重试次数     |
| `ws_push_dead_total`    | Counter | 死信消息次数     |
| `ws_push_offline_total` | Counter | 目标用户离线次数 |

失败原因标签建议：

| reason                | 说明          |
| --------------------- | ------------- |
| `USER_OFFLINE`        | 用户离线      |
| `BROKER_UNAVAILABLE`  | Broker 不可用 |
| `SERIALIZE_FAILED`    | 序列化失败    |
| `DESTINATION_INVALID` | 目标地址非法  |
| `SEND_TIMEOUT`        | 发送超时      |
| `UNKNOWN_ERROR`       | 未知错误      |

失败率计算示例：

```text
推送失败率 = ws_push_failed_total / (ws_push_success_total + ws_push_failed_total)
```

PromQL 示例：

```promql
sum(rate(ws_push_failed_total[5m]))
/
sum(rate(ws_push_success_total[5m]) + rate(ws_push_failed_total[5m]))
```

推送失败率告警建议：

| 等级        | 条件                          |
| ----------- | ----------------------------- |
| 提醒        | 5 分钟失败率 > 1%             |
| 警告        | 5 分钟失败率 > 5%             |
| 严重        | 5 分钟失败率 > 10%            |
| Broker 故障 | `BROKER_UNAVAILABLE` 持续增长 |

### 消息延迟

消息延迟用于衡量消息从生成、入库、推送到客户端 ACK 的耗时。对于聊天、审批、告警和任务进度，延迟指标比平均吞吐更重要。

推荐延迟指标如下：

| 指标                                  | 类型  | 说明              |
| ------------------------------------- | ----- | ----------------- |
| `ws_message_process_duration_seconds` | Timer | 服务端处理耗时    |
| `ws_push_duration_seconds`            | Timer | 推送耗时          |
| `ws_ack_duration_seconds`             | Timer | 从推送到 ACK 耗时 |
| `ws_broker_publish_duration_seconds`  | Timer | Broker 发布耗时   |
| `ws_compensation_duration_seconds`    | Timer | 补偿查询耗时      |

推送耗时记录示例：

```java
long start = System.nanoTime();
try {
    wsPushService.sendToUser(userId, destination, payload);
} finally {
    meterRegistry.timer("ws_push_duration_seconds", "messageType", messageType)
            .record(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS);
}
```

延迟指标建议关注：

| 分位数 | 说明                         |
| ------ | ---------------------------- |
| P50    | 普通用户平均体验             |
| P90    | 多数用户体验                 |
| P95    | 常规告警参考                 |
| P99    | 尾部延迟和慢消费者问题       |
| Max    | 极端异常参考，但不宜单独告警 |

消息延迟告警建议：

| 场景       | 条件        |
| ---------- | ----------- |
| 普通通知   | P95 > 1s    |
| 聊天消息   | P95 > 500ms |
| 管理端看板 | P95 > 2s    |
| 告警消息   | P95 > 1s    |
| ACK 延迟   | P95 > 5s    |

### 线程池指标

线程池指标用于观察入站、出站、业务、推送和定时任务线程池的健康状态。线程池堆积通常是系统延迟升高和故障扩大的早期信号。

推荐指标如下：

| 指标                             | 类型    | 说明         |
| -------------------------------- | ------- | ------------ |
| `ws_thread_pool_active`          | Gauge   | 活跃线程数   |
| `ws_thread_pool_size`            | Gauge   | 当前线程数   |
| `ws_thread_pool_queue_size`      | Gauge   | 队列长度     |
| `ws_thread_pool_queue_remaining` | Gauge   | 队列剩余容量 |
| `ws_thread_pool_completed_total` | Counter | 完成任务数   |
| `ws_thread_pool_rejected_total`  | Counter | 拒绝任务数   |

标签建议：

| 标签       | 说明                                        |
| ---------- | ------------------------------------------- |
| `pool`     | inbound、outbound、business、push、schedule |
| `instance` | 实例 ID                                     |

线程池监控建议如下：

| 告警项           | 建议阈值                     |
| ---------------- | ---------------------------- |
| 活跃线程数       | 超过最大线程数 80%           |
| 队列使用率       | 超过 70% 提醒，超过 90% 严重 |
| 拒绝任务数       | 任何增长都需要关注           |
| 出站队列持续增长 | 可能存在慢消费者             |
| 入站队列持续增长 | 可能业务处理阻塞             |

### Broker 指标

Broker 指标用于观察 RabbitMQ、ActiveMQ 或其他外部消息代理的运行状态。使用 Broker Relay 时，Broker 是 WebSocket 推送能力的核心依赖。

推荐 RabbitMQ 指标如下：

| 指标            | 说明                           |
| --------------- | ------------------------------ |
| Broker 可用状态 | RabbitMQ 是否可连接            |
| 连接数          | WebSocket 服务与 Broker 连接数 |
| Channel 数      | RabbitMQ Channel 数量          |
| 队列积压        | Ready 和 Unacked 消息数        |
| 发布速率        | publish rate                   |
| 消费速率        | deliver/get rate               |
| 内存使用率      | 内存水位                       |
| 磁盘剩余        | 磁盘水位                       |
| 节点状态        | 集群节点是否正常               |

Broker Relay 关注指标：

| 指标            | 说明                      |
| --------------- | ------------------------- |
| relay 连接状态  | Spring 到 Broker 是否连接 |
| relay 心跳失败  | 心跳异常次数              |
| broker 重连次数 | Broker 断开后重连次数     |
| 发送失败次数    | Relay 推送失败            |
| 订阅异常次数    | SUBSCRIBE 处理异常        |

告警建议：

| 场景          | 条件                |
| ------------- | ------------------- |
| Broker 不可用 | 持续 1 分钟不可连接 |
| 队列积压      | 积压持续增长 5 分钟 |
| 内存高水位    | 超过 80%            |
| 磁盘不足      | 低于安全阈值        |
| 心跳失败      | 连续失败超过阈值    |

### Redis 指标

Redis 指标用于观察在线状态、Session 映射、未读数、限流、幂等、Pub/Sub、Stream 等功能的基础依赖状态。

推荐指标如下：

| 指标              | 说明                    |
| ----------------- | ----------------------- |
| Redis 可用状态    | 是否可连接              |
| Redis 命令延迟    | P95/P99 延迟            |
| 在线用户 Key 数量 | `SCARD ws:online:users` |
| Session Key 数量  | `ws:session:*` 数量     |
| Pub/Sub 消息量    | 跨实例事件数量          |
| Stream 积压       | Pending 消息数量        |
| 缓存命中率        | 群成员、未读数缓存命中  |
| Redis 内存使用率  | used_memory             |
| Key 过期数量      | expired_keys            |
| 连接数            | connected_clients       |

Redis 指标建议如下：

| 建议                | 说明                                 |
| ------------------- | ------------------------------------ |
| 监控 Redis 延迟     | Redis 慢会直接影响连接鉴权和在线状态 |
| 监控 Session 残留   | Session Key 异常增长说明清理异常     |
| 监控 Stream Pending | 消费者失败会导致积压                 |
| 避免 KEYS 扫描      | 生产统计 Key 数量要谨慎              |
| Redis 异常要降级    | 至少保留本地连接处理能力             |

### JVM 指标

JVM 指标用于观察 WebSocket 服务的内存、GC、线程、类加载、CPU 等运行状态。WebSocket 长连接系统对内存和 GC 更敏感，GC 停顿会导致心跳超时、消息延迟和连接批量断开。

推荐 JVM 指标如下：

| 指标             | 说明                      |
| ---------------- | ------------------------- |
| JVM 堆内存使用率 | Heap 使用情况             |
| JVM 非堆内存     | Metaspace 等              |
| GC 次数          | Young GC、Full GC         |
| GC 耗时          | GC pause                  |
| 线程数           | 当前线程、峰值线程        |
| CPU 使用率       | 进程 CPU                  |
| 类加载数量       | 类加载指标                |
| 直接内存         | Netty/Tomcat 相关直接内存 |
| 文件描述符       | 长连接系统必须关注        |

JVM 告警建议：

| 指标         | 条件                   |
| ------------ | ---------------------- |
| 堆内存使用率 | 持续超过 80%           |
| Full GC      | 短时间频繁出现         |
| GC 暂停      | P99 超过业务可接受范围 |
| 线程数       | 持续增长不下降         |
| CPU          | 持续超过 80%           |
| 文件描述符   | 使用率超过 80%         |

JVM 监控建议如下：

| 建议               | 说明                       |
| ------------------ | -------------------------- |
| 开启 GC 日志       | 生产排查必须               |
| 监控堆外内存       | WebSocket 可能涉及网络缓冲 |
| 压测观察 P99       | 平均值不能代表实时体验     |
| 慢消费者结合内存看 | 出站堆积会推高内存         |
| 线程数异常要排查   | 可能是线程池配置错误或泄漏 |

### Prometheus 集成

Prometheus 集成用于采集 Spring Boot Actuator、Micrometer、自定义 WebSocket 指标、JVM 指标、线程池指标等数据。Spring Boot 3 推荐使用 Micrometer + Actuator + Prometheus Registry。

依赖配置如下：

```xml
<!-- Spring Boot Actuator：暴露健康检查和指标端点 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus：将指标暴露为 Prometheus 格式 -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Actuator 配置如下：

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
          - metrics
          - prometheus
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    tags:
      application: spring-websocket-stomp
```

Prometheus 抓取配置示例：

```yaml
scrape_configs:
  - job_name: 'spring-websocket-stomp'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets:
          - 'websocket-service-1:8080'
          - 'websocket-service-2:8080'
```

自定义指标注册器可以在应用启动后初始化。

文件位置：`src/main/java/io/github/atengk/websocket/monitor/WsMetricsInitializer.java`

```java
package io.github.atengk.websocket.monitor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * WebSocket 指标初始化器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Component
@RequiredArgsConstructor
public class WsMetricsInitializer {

    private final WsMetricsService wsMetricsService;

    /**
     * 初始化指标
     */
    @PostConstruct
    public void init() {
        wsMetricsService.initSessionGauge();
    }
}
```

Prometheus 集成建议如下：

| 建议                     | 说明                                         |
| ------------------------ | -------------------------------------------- |
| 不使用高基数标签         | 不要把 userId、sessionId、messageId 作为标签 |
| 标签枚举要稳定           | messageType、clientType、result 可以         |
| 指标名统一前缀           | 建议使用 `ws_`                               |
| 采集间隔合理             | 10 到 30 秒                                  |
| 管理端和 Prometheus 分离 | 管理端展示聚合，Prometheus 做时序监控        |

### Grafana 看板

Grafana 看板用于可视化 WebSocket 服务的连接、消息、延迟、失败、线程池、Redis、Broker、JVM 和安全事件。看板应按“总览、连接、消息、性能、依赖、安全”分组。

推荐看板模块如下：

| 模块   | 图表                                         |
| ------ | -------------------------------------------- |
| 总览   | 当前连接数、在线用户数、推送失败率、P95 延迟 |
| 连接   | 连接成功率、断开原因、客户端类型分布         |
| 消息   | 入站消息量、出站消息量、广播量、群组消息量   |
| 可靠性 | 推送失败、重试、死信、离线消息               |
| 延迟   | 处理耗时、推送耗时、ACK 耗时                 |
| 线程池 | 活跃线程、队列长度、拒绝次数                 |
| Broker | Broker 状态、积压、心跳、重连                |
| Redis  | 延迟、连接数、Session Key、Stream Pending    |
| JVM    | 堆内存、GC、线程、CPU                        |
| 安全   | 认证失败、非法订阅、限流、黑名单命中         |

常用 PromQL 示例：

当前连接数：

```promql
sum(ws_sessions_current)
```

5 分钟连接成功速率：

```promql
sum(rate(ws_sessions_connected_total[5m]))
```

5 分钟推送失败率：

```promql
sum(rate(ws_push_failed_total[5m]))
/
sum(rate(ws_push_success_total[5m]) + rate(ws_push_failed_total[5m]))
```

P95 推送延迟：

```promql
histogram_quantile(0.95, sum(rate(ws_push_duration_seconds_bucket[5m])) by (le))
```

线程池队列长度：

```promql
sum(ws_thread_pool_queue_size) by (pool)
```

Grafana 看板建议如下：

| 建议                   | 说明                   |
| ---------------------- | ---------------------- |
| 总览页少而关键         | 不堆过多图表           |
| 支持按实例筛选         | 排查单实例异常         |
| 支持按 clientType 筛选 | WEB、APP、ADMIN 分开看 |
| 延迟看 P95/P99         | 平均值意义有限         |
| 失败率和流量一起看     | 低流量时失败率可能误导 |
| 安全事件单独成页       | 便于风控排查           |

### 告警规则

告警规则用于在 WebSocket 服务出现连接异常、推送失败、消息延迟、线程池堆积、Broker 故障、Redis 异常、JVM 压力和安全攻击时及时通知运维或研发。

推荐告警规则如下：

| 告警项             | 条件                             | 等级     |
| ------------------ | -------------------------------- | -------- |
| 推送失败率高       | 5 分钟失败率 > 5%                | 警告     |
| 推送失败率严重     | 5 分钟失败率 > 10%               | 严重     |
| 当前连接数异常下降 | 5 分钟内下降超过 50%             | 严重     |
| 连接失败数升高     | 5 分钟认证失败或连接失败持续增长 | 警告     |
| P95 推送延迟高     | P95 > 1s 持续 5 分钟             | 警告     |
| 出站队列堆积       | 队列使用率 > 80% 持续 3 分钟     | 警告     |
| 线程池拒绝任务     | 拒绝次数 > 0                     | 严重     |
| Broker 不可用      | Broker 状态 DOWN 超过 1 分钟     | 严重     |
| Redis 不可用       | Redis 连接失败超过 1 分钟        | 严重     |
| Full GC 频繁       | 5 分钟内 Full GC 多次            | 严重     |
| 非法订阅激增       | 5 分钟非法订阅超过阈值           | 安全告警 |
| 黑名单命中激增     | 5 分钟命中次数超过阈值           | 安全告警 |

Prometheus 告警规则示例：

```yaml
groups:
  - name: websocket-stomp-alerts
    rules:
      - alert: WebSocketPushFailureRateHigh
        expr: |
          sum(rate(ws_push_failed_total[5m]))
          /
          sum(rate(ws_push_success_total[5m]) + rate(ws_push_failed_total[5m])) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "WebSocket 推送失败率过高"
          description: "过去 5 分钟 WebSocket 推送失败率超过 5%"

      - alert: WebSocketOutboundQueueHigh
        expr: |
          sum(ws_thread_pool_queue_size{pool="outbound"})
          /
          sum(ws_thread_pool_queue_size{pool="outbound"} + ws_thread_pool_queue_remaining{pool="outbound"}) > 0.8
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "WebSocket 出站线程池队列堆积"
          description: "WebSocket 出站线程池队列使用率超过 80%"

      - alert: WebSocketPushRejected
        expr: |
          increase(ws_thread_pool_rejected_total[5m]) > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "WebSocket 线程池出现拒绝任务"
          description: "WebSocket 线程池发生任务拒绝，请检查队列堆积和慢消费者"

      - alert: WebSocketPushLatencyHigh
        expr: |
          histogram_quantile(0.95, sum(rate(ws_push_duration_seconds_bucket[5m])) by (le)) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "WebSocket 推送延迟过高"
          description: "过去 5 分钟 WebSocket 推送 P95 延迟超过 1 秒"

      - alert: WebSocketAuthFailedHigh
        expr: |
          sum(rate(ws_invalid_messages_total{reason="AUTH_FAILED"}[5m])) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "WebSocket 认证失败次数异常"
          description: "WebSocket 认证失败次数持续升高，可能存在 Token 失效或攻击行为"
```

告警设计建议如下：

| 建议             | 说明                                         |
| ---------------- | -------------------------------------------- |
| 告警要分级       | warning、critical 分开处理                   |
| 避免高基数标签   | 告警不要按 userId、sessionId 分组            |
| 告警要有恢复条件 | 防止一直处于异常状态                         |
| 结合业务时间段   | 夜间低流量失败率可能误报                     |
| 安全告警单独通知 | 非法订阅、认证失败、黑名单命中应进入安全通道 |
| 告警必须可定位   | annotation 中包含排查方向                    |
| 告警后有降级策略 | Broker、Redis、线程池异常要有应急预案        |

## 测试方案

测试方案用于验证 Spring Boot 3 WebSocket STOMP 模块在连接、认证、订阅、发送、推送、可靠性、离线补偿、集群同步、限流和高并发场景下是否符合预期。测试需要覆盖单元测试、集成测试、协议测试、业务测试、异常测试和压测，不能只验证“能连上”。

### 单元测试

单元测试用于验证工具类、消息模型、路由规则、权限判断、幂等逻辑、限流逻辑、敏感词过滤、消息构造等不依赖真实 WebSocket 连接的代码。

适合单元测试的对象如下：

| 测试对象     | 测试重点                           |
| ------------ | ---------------------------------- |
| 消息 ID 工具 | ID 唯一性、非空、格式              |
| 消息模型     | 成功消息、失败消息、时间戳、版本号 |
| 目标地址校验 | 非法路径、管理端路径、发送路径     |
| 敏感信息脱敏 | Token、手机号、邮箱脱敏            |
| 敏感词过滤   | 命中、替换、拒绝                   |
| 幂等服务     | 首次处理、重复处理                 |
| 限流服务     | 阈值内通过、超过阈值拒绝           |
| 权限服务     | 管理员、群成员、普通用户权限       |

测试依赖建议：

```xml
<!-- Spring Boot 测试依赖：包含 JUnit Jupiter、AssertJ、Mockito 等 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Awaitility：用于异步测试等待，例如 WebSocket 消息接收、异步推送完成 -->
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

下面示例用于测试目标地址校验工具。

文件位置：`src/test/java/io/github/atengk/websocket/util/WsDestinationValidatorTest.java`

```java
package io.github.atengk.websocket.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket 目标地址校验测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
class WsDestinationValidatorTest {

    @Test
    void shouldRejectInvalidDestination() {
        assertTrue(WsDestinationValidator.isInvalid(null));
        assertTrue(WsDestinationValidator.isInvalid(""));
        assertTrue(WsDestinationValidator.isInvalid("/topic/../../admin"));
        assertTrue(WsDestinationValidator.isInvalid("/topic//admin"));
        assertTrue(WsDestinationValidator.isInvalid("/topic\\admin"));
    }

    @Test
    void shouldAllowAppSendDestination() {
        assertTrue(WsDestinationValidator.isAllowedSendDestination("/app/chat/private/send"));
        assertFalse(WsDestinationValidator.isAllowedSendDestination("/topic/system/notice"));
        assertFalse(WsDestinationValidator.isAllowedSendDestination("/user/queue/notice"));
    }

    @Test
    void shouldIdentifyAdminDestination() {
        assertTrue(WsDestinationValidator.isAdminDestination("/topic/admin/online"));
        assertTrue(WsDestinationValidator.isAdminDestination("/app/admin/push/test"));
        assertFalse(WsDestinationValidator.isAdminDestination("/topic/group/90001"));
    }
}
```

单元测试建议如下：

| 建议                 | 说明                           |
| -------------------- | ------------------------------ |
| 测试粒度小           | 不启动完整 Spring 容器         |
| 覆盖异常分支         | 空值、非法路径、超长内容都要测 |
| 权限判断要表格化     | 不同角色、不同目标路径都覆盖   |
| 幂等和限流重点测边界 | 阈值等于、超过、重复调用       |
| 不依赖真实 Redis     | 单元测试可用 Mock 或内存实现   |

### 集成测试

集成测试用于验证 Spring 容器、WebSocket 配置、STOMP Endpoint、拦截器、消息转换器、认证鉴权、消息处理器和推送服务是否能协同工作。集成测试通常需要启动 Spring Boot 测试环境。

适合集成测试的内容如下：

| 测试对象             | 测试重点                          |
| -------------------- | --------------------------------- |
| WebSocket 配置       | Endpoint、跨域、SockJS、Broker    |
| ChannelInterceptor   | CONNECT、SUBSCRIBE、SEND 拦截     |
| HandshakeInterceptor | Query、Cookie、IP、Origin         |
| 消息处理器           | `@MessageMapping` 是否正确处理    |
| 推送服务             | 广播、用户、群组、指定 Session    |
| 全局异常处理         | 错误是否返回 `/user/queue/errors` |
| Jackson 转换器       | 时间、枚举、未知字段处理          |
| Redis 集成           | Session、在线状态、限流、幂等     |

Spring Boot 集成测试基础配置如下。

文件位置：`src/test/java/io/github/atengk/websocket/WsIntegrationTest.java`

```java
package io.github.atengk.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * WebSocket 集成测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WsIntegrationTest {

    @Test
    void contextLoads() {
    }
}
```

如果项目依赖 Redis，建议使用 Testcontainers 启动真实 Redis，避免测试环境和本机 Redis 状态互相影响。

测试依赖：

```xml
<!-- Testcontainers：集成测试中启动 Redis、RabbitMQ 等外部依赖 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

集成测试建议如下：

| 建议                | 说明                                |
| ------------------- | ----------------------------------- |
| 使用随机端口        | 避免端口冲突                        |
| 外部依赖容器化      | Redis、RabbitMQ 使用 Testcontainers |
| 测试数据隔离        | 每个测试清理 Redis 和数据库         |
| 重点验证链路        | 连接、订阅、发送、推送、异常返回    |
| 不用 sleep 等待异步 | 使用 Awaitility                     |

### WebSocket 连接测试

WebSocket 连接测试用于验证客户端能否建立 WebSocket/STOMP 连接，并完成握手、CONNECT、心跳协商和断开。

测试场景如下：

| 场景            | 预期                     |
| --------------- | ------------------------ |
| 合法 Token 连接 | 连接成功                 |
| 缺少 Token      | 连接失败或收到认证错误   |
| 过期 Token      | 连接失败                 |
| 非法 Origin     | 握手失败                 |
| 黑名单 IP       | 握手或 CONNECT 被拒绝    |
| 连接数超限      | 新连接被拒绝或旧连接被踢 |
| 断开连接        | Session 被清理           |

下面示例使用 `WebSocketStompClient` 连接测试环境。

文件位置：`src/test/java/io/github/atengk/websocket/stomp/WsConnectTest.java`

```java
package io.github.atengk.websocket.stomp;

import cn.hutool.core.map.MapUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * WebSocket STOMP 连接测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WsConnectTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldConnectWithValidToken() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        CompletableFuture<StompSession> future = new CompletableFuture<>();

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer test-token");
        connectHeaders.add("clientType", "WEB");
        connectHeaders.add("deviceId", "test-device");

        stompClient.connectAsync(
                "ws://localhost:" + port + "/ws/stomp",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new TestSessionHandler(future)
        );

        StompSession session = future.get(5, TimeUnit.SECONDS);
        assertNotNull(session);
        session.disconnect();
    }

    /**
     * 测试 STOMP Session 处理器
     *
     * @author Ateng
     * @since 2026-05-05
     */
    static class TestSessionHandler extends StompSessionHandlerAdapter {

        private final CompletableFuture<StompSession> future;

        TestSessionHandler(CompletableFuture<StompSession> future) {
            this.future = future;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            future.complete(session);
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Map.class;
        }

        @Override
        public void handleException(StompSession session,
                                    StompCommand command,
                                    StompHeaders headers,
                                    byte[] payload,
                                    Throwable exception) {
            future.completeExceptionally(exception);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            future.completeExceptionally(exception);
        }
    }
}
```

连接测试注意事项如下：

| 注意项               | 说明                           |
| -------------------- | ------------------------------ |
| 测试合法和非法 Token | 不能只测成功连接               |
| 验证 Session 注册    | CONNECT 成功后应能查到 Session |
| 验证断开清理         | DISCONNECT 后 Session 应移除   |
| 验证重连             | 客户端重连后能重新订阅         |
| 验证心跳             | 长连接保持期间不应误断开       |

### STOMP 消息测试

STOMP 消息测试用于验证客户端发送到 `/app/**` 后，服务端能正确接收、处理并推送到目标订阅地址。

测试场景如下：

| 场景           | 预期                                |
| -------------- | ----------------------------------- |
| 发送单聊消息   | 接收方 `/user/queue/chat` 收到消息  |
| 发送群聊消息   | `/topic/group/{groupId}` 收到消息   |
| 发送通知已读   | `/user/queue/notice` 收到未读数变化 |
| 发送 ACK       | 回执记录被保存                      |
| 发送非法消息体 | `/user/queue/errors` 收到错误       |
| 发送到非法地址 | 请求被拒绝                          |

STOMP 消息测试流程：

```text
1. 建立 STOMP 连接
2. 订阅目标地址
3. 发送消息到 /app/**
4. 等待目标订阅收到消息
5. 校验 messageId、type、data
6. 断开连接并清理数据
```

消息接收可以使用 `BlockingQueue` 存储回调结果：

```java
BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();

session.subscribe("/user/queue/notice", new StompFrameHandler() {
    @Override
    public Type getPayloadType(StompHeaders headers) {
        return Map.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        queue.add((Map<String, Object>) payload);
    }
});
```

断言示例：

```java
Map<String, Object> message = queue.poll(5, TimeUnit.SECONDS);
assertNotNull(message);
assertEquals("USER_NOTICE", message.get("type"));
```

### 认证鉴权测试

认证鉴权测试用于验证 WebSocket 连接和 STOMP 消息处理过程中的身份认证和权限控制。

认证测试场景如下：

| 场景             | 预期                   |
| ---------------- | ---------------------- |
| 合法 Token       | CONNECT 成功           |
| 缺少 Token       | CONNECT 失败           |
| Token 过期       | CONNECT 失败           |
| Token 签名错误   | CONNECT 失败           |
| 用户被禁用       | CONNECT 失败           |
| 用户黑名单       | CONNECT 失败或被踢下线 |
| Token 刷新后重连 | 新连接成功             |

鉴权测试场景如下：

| 场景                     | 预期 |
| ------------------------ | ---- |
| 普通用户订阅系统公告     | 允许 |
| 普通用户订阅管理端 Topic | 拒绝 |
| 群成员订阅群组 Topic     | 允许 |
| 非群成员订阅群组 Topic   | 拒绝 |
| 普通用户发送管理端消息   | 拒绝 |
| 被禁言用户发送群消息     | 拒绝 |
| 用户操作他人通知         | 拒绝 |

鉴权测试建议如下：

| 建议                        | 说明                            |
| --------------------------- | ------------------------------- |
| 覆盖 CONNECT/SUBSCRIBE/SEND | 三个阶段权限不同                |
| 构造恶意 destination        | 测试 `..`、`//`、`\` 等非法路径 |
| 使用不同角色账号            | 普通用户、群管理员、系统管理员  |
| 验证错误返回                | 拒绝后应返回明确错误码          |
| 验证审计日志                | 重要越权行为应记录审计          |

### 订阅权限测试

订阅权限测试用于重点验证客户端不能越权订阅敏感 Topic。订阅权限测试比发送权限更容易被忽视，但实际风险更高，因为一旦非法订阅成功，用户可能持续接收不该看到的数据。

订阅权限测试矩阵如下：

| 订阅路径                 | 普通用户   | 群成员   | 管理员   |
| ------------------------ | ---------- | -------- | -------- |
| `/topic/system/notice`   | 允许       | 允许     | 允许     |
| `/topic/admin/online`    | 拒绝       | 拒绝     | 允许     |
| `/topic/admin/alarm`     | 拒绝       | 拒绝     | 允许     |
| `/topic/group/90001`     | 按成员关系 | 允许     | 按业务   |
| `/topic/tenant/1/notice` | 按租户     | 按租户   | 按租户   |
| `/user/queue/notice`     | 允许本人   | 允许本人 | 允许本人 |

测试重点如下：

| 测试点         | 说明                               |
| -------------- | ---------------------------------- |
| 非法路径拒绝   | `/topic/../../admin` 必须拒绝      |
| 管理端路径隔离 | 普通用户不能订阅 `/topic/admin/**` |
| 群组成员校验   | 退出群后不能再订阅                 |
| 租户隔离       | 不能订阅其他租户 Topic             |
| 高频非法订阅   | 达阈值后触发风控                   |

### 消息可靠性测试

消息可靠性测试用于验证消息 ID、幂等、ACK、回执、重试、补偿、持久化、失败记录和死信处理是否符合设计。

可靠性测试场景如下：

| 场景                          | 预期                       |
| ----------------------------- | -------------------------- |
| 相同 clientMessageId 重复发送 | 只生成一条消息             |
| 相同业务事件重复消费          | 只生成一条通知             |
| 客户端重复 ACK                | 只保存一条回执             |
| 推送失败                      | 写入失败消息表             |
| 重试成功                      | 失败记录状态变为 SUCCESS   |
| 重试超过上限                  | 转入死信                   |
| 消息落库后推送                | 数据提交成功后才推送       |
| 事务回滚                      | 不推送消息                 |
| 客户端断线后重连              | 可通过补偿接口拉取缺失消息 |

可靠性测试建议如下：

| 建议             | 说明                             |
| ---------------- | -------------------------------- |
| 测幂等边界       | 重复请求、并发请求都要测         |
| 模拟推送失败     | Mock Broker 或关闭目标连接       |
| 验证 afterCommit | 事务回滚时不能推送               |
| 验证 ACK 超时    | 超时后进入重试或补偿             |
| 验证补偿去重     | 实时消息和补偿消息重复时前端去重 |

### 离线消息测试

离线消息测试用于验证用户离线期间产生的消息是否能在用户重连后补偿到客户端。

测试场景如下：

| 场景                   | 预期                         |
| ---------------------- | ---------------------------- |
| 用户离线时收到通知     | 写入通知表或离线消息表       |
| 用户重连后查询未读     | 返回离线期间通知             |
| 用户重连后补偿聊天     | 按 lastSequence 返回缺失消息 |
| 离线消息过期           | 不再补偿                     |
| 离线消息已送达         | 状态更新为 DELIVERED         |
| 大量离线消息           | 分页补偿                     |
| 补偿消息与实时消息重叠 | 客户端按 messageId 去重      |

离线消息测试流程：

```text
1. 用户 A 建立连接后断开
2. 用户 B 给用户 A 发送消息
3. 服务端检测用户 A 离线
4. 消息写入持久化表或离线表
5. 用户 A 重连
6. 用户 A 调用补偿接口
7. 校验返回缺失消息
8. 客户端 ACK 后服务端更新状态
```

离线消息测试建议如下：

| 建议               | 说明                                     |
| ------------------ | ---------------------------------------- |
| 区分聊天和通知     | 聊天优先按 sequence 补偿，通知按未读查询 |
| 测试过期策略       | 过期离线消息不应返回                     |
| 测试分页           | 离线消息过多时不能一次返回全部           |
| 测试多端在线       | 一个端离线不代表用户离线                 |
| 测试重连后订阅恢复 | 补偿前先恢复必要订阅                     |

### 集群消息测试

集群消息测试用于验证多实例部署下的点对点、广播、群组、在线状态、强制下线、限流和 Redis/Broker 同步是否正常。

测试环境建议至少启动两个 WebSocket 实例：

```text
ws-node-01:8081
ws-node-02:8082
Redis
RabbitMQ 或 Redis Pub/Sub
Nginx/Gateway
```

集群测试场景如下：

| 场景                                 | 预期                      |
| ------------------------------------ | ------------------------- |
| 用户 A 连接实例 1，用户 B 连接实例 2 | 两个用户都在线            |
| 实例 1 推送给用户 B                  | 用户 B 能收到             |
| 实例 2 发布广播                      | 两个实例上的客户端都收到  |
| 群成员分布在不同实例                 | 群消息全部送达            |
| 管理端在实例 1 踢用户 B              | 实例 2 上的用户 B 下线    |
| 实例 2 宕机                          | Redis 残留 Session 被清理 |
| 用户在线状态                         | Redis 全局状态正确        |
| 分布式限流                           | 用户跨实例发送仍被限制    |

集群测试建议如下：

| 建议             | 说明                                   |
| ---------------- | -------------------------------------- |
| 不只测单实例     | 多实例问题单机测不出来                 |
| 验证 instanceId  | Session 映射必须记录所属实例           |
| 验证跨实例事件   | Redis Pub/Sub 或 Broker Relay 必须生效 |
| 验证实例宕机清理 | 防止用户假在线                         |
| 验证消息幂等     | 跨实例广播可能重复消费                 |

### 限流测试

限流测试用于验证连接频率、发送频率、订阅频率、目标地址、群组、广播和消息大小限制是否生效。

限流测试场景如下：

| 场景                   | 预期                       |
| ---------------------- | -------------------------- |
| 单用户每秒发送超过阈值 | 超出部分被拒绝             |
| 单 IP 高频建连         | 后续连接被拒绝             |
| 单 Session 高频订阅    | 订阅被拒绝                 |
| 普通用户高频非法订阅   | 触发风控                   |
| 群组高频刷屏           | 用户被临时禁言或消息被拒绝 |
| 管理端高频广播         | 广播被限流                 |
| 超大消息体             | 被拒绝并记录安全日志       |
| 分布式跨实例刷请求     | Redis 限流仍生效           |

限流测试示例思路：

```text
1. 构造同一个 userId
2. 1 秒内连续发送 20 条消息
3. 服务端阈值为每秒 5 条
4. 断言前 5 条成功
5. 后续消息收到限流错误
6. Redis 中限流 Key 有 TTL
7. TTL 过后再次发送成功
```

限流测试建议如下：

| 建议           | 说明                     |
| -------------- | ------------------------ |
| 测试阈值边界   | 等于阈值和超过阈值都要测 |
| 测试 TTL 释放  | 过期后应恢复正常         |
| 测试分布式限流 | 多实例下不能绕过         |
| 测试错误返回   | 前端应收到明确错误码     |
| 测试风控联动   | 多次超限后是否封禁       |

### 压力测试

压力测试用于评估 WebSocket 服务在目标连接数、目标消息量、目标广播频率和目标业务模型下的性能表现。压测目标应包括连接建立能力、稳定连接能力、消息吞吐、延迟、失败率、线程池队列、内存、GC、Redis 和 Broker 状态。

压测指标如下：

| 指标         | 说明                        |
| ------------ | --------------------------- |
| 最大连接数   | 单实例或集群可承载连接数    |
| 建连速率     | 每秒建立连接数              |
| 消息吞吐     | 每秒 SEND 和推送数量        |
| P95/P99 延迟 | 消息处理和推送延迟          |
| 推送失败率   | 失败消息比例                |
| 断线率       | 压测期间连接异常断开比例    |
| CPU          | 应用和 Broker CPU           |
| 内存         | JVM 堆、Redis、Broker 内存  |
| GC           | GC 次数和停顿               |
| 队列长度     | 入站、出站、推送队列        |
| Redis 延迟   | 在线状态和限流访问延迟      |
| Broker 积压  | Broker Relay 队列和连接状态 |

压力测试建议如下：

| 建议               | 说明                                |
| ------------------ | ----------------------------------- |
| 先单场景再混合场景 | 先测连接，再测发送，再测广播        |
| 压测环境接近生产   | 网关、Redis、Broker 都要包含        |
| 逐步升压           | 不要一开始直接打满                  |
| 记录资源曲线       | CPU、内存、GC、队列和延迟一起看     |
| 明确停止条件       | 错误率、延迟、CPU、内存超过阈值停止 |
| 压测后清理数据     | Session、离线消息、测试通知清理     |

### 稳定性测试

稳定性测试用于验证系统在长时间运行、网络抖动、实例重启、Broker 抖动、Redis 抖动、慢消费者、客户端重连、消息持续推送等情况下是否能保持稳定。

稳定性测试场景如下：

| 场景               | 持续时间      | 验证点                     |
| ------------------ | ------------- | -------------------------- |
| 长连接保持         | 8 到 24 小时  | 连接是否稳定、内存是否增长 |
| 持续低频推送       | 8 到 24 小时  | 消息延迟是否稳定           |
| 心跳维持           | 8 到 24 小时  | 是否出现误断开             |
| 客户端随机断开重连 | 2 到 8 小时   | Session 是否正确清理       |
| Broker 短暂不可用  | 10 到 30 分钟 | 是否失败记录和恢复         |
| Redis 短暂不可用   | 10 到 30 分钟 | 降级和恢复                 |
| 慢消费者           | 1 到 2 小时   | 背压是否生效               |
| 服务滚动发布       | 多轮          | 长连接迁移和重连是否正常   |

稳定性测试关注的不是峰值，而是资源是否持续增长、延迟是否逐步变大、Session 是否残留、线程池队列是否堆积、Redis Key 是否泄漏。

## 压测方案

压测方案用于系统性评估 Spring WebSocket STOMP 服务在不同负载模型下的容量边界和瓶颈。压测应分阶段执行：先测单实例，再测集群；先测空连接，再测业务消息；先测 Simple Broker，再测 Broker Relay；先测短压，再测长稳。

### 连接数压测

连接数压测用于评估服务端可以稳定保持多少 WebSocket/STOMP 长连接。该测试重点是连接建立、心跳维持、Session 注册、内存占用和网关长连接能力。

压测目标示例：

| 阶段   | 目标连接数         |
| ------ | ------------------ |
| 阶段 1 | 1000               |
| 阶段 2 | 5000               |
| 阶段 3 | 10000              |
| 阶段 4 | 20000              |
| 阶段 5 | 按业务目标继续增加 |

测试步骤：

```text
1. 启动 WebSocket 服务和监控
2. 按固定速率逐步建立连接
3. 每个连接完成 STOMP CONNECT
4. 保持心跳
5. 观察 10 到 30 分钟
6. 记录连接成功率、断线率、CPU、内存、GC
7. 提升连接数进入下一阶段
```

重点观察指标：

| 指标              | 说明                   |
| ----------------- | ---------------------- |
| 当前连接数        | 是否达到目标           |
| 建连失败率        | 是否随连接数升高       |
| Session 注册数    | 是否与连接数一致       |
| JVM 内存          | 是否线性增长           |
| GC 停顿           | 是否影响心跳           |
| 网关连接数        | 是否达到文件描述符限制 |
| Redis Session Key | 是否正确写入           |
| 心跳超时数        | 是否异常增长           |

连接数压测建议如下：

| 建议                   | 说明                         |
| ---------------------- | ---------------------------- |
| 客户端压测机要足够     | 单台压测机也有连接上限       |
| 调整文件描述符         | 服务端、网关、压测机都要调整 |
| 连接速率逐步增加       | 避免瞬时建连打爆认证逻辑     |
| 空连接和订阅连接分开测 | 订阅会增加 Broker 和内存压力 |
| 长时间保持             | 连接建立成功不等于能长期稳定 |

### 并发发送压测

并发发送压测用于评估大量客户端同时向 `/app/**` 发送消息时，服务端入站通道、业务处理、数据库、Redis、限流和出站推送的能力。

压测模型示例：

| 场景           | 参数        |
| -------------- | ----------- |
| 在线连接数     | 5000        |
| 活跃发送用户   | 1000        |
| 每用户发送频率 | 1 条/秒     |
| 总发送速率     | 1000 条/秒  |
| 消息类型       | 单聊或群聊  |
| 消息大小       | 200B 到 2KB |

测试步骤：

```text
1. 建立指定数量连接
2. 每个连接完成认证
3. 部分连接订阅接收队列
4. 按目标速率发送 /app/chat/private/send
5. 服务端处理、落库、推送
6. 客户端统计收到消息数和延迟
7. 观察失败率、线程池、数据库和 Redis
```

重点观察：

| 指标            | 说明                         |
| --------------- | ---------------------------- |
| 入站消息量      | `ws_messages_received_total` |
| 业务处理耗时    | SEND 处理 P95/P99            |
| 推送成功率      | 接收方是否收到               |
| 数据库写入 TPS  | 聊天消息落库能力             |
| Redis 延迟      | 幂等、限流、在线查询         |
| 入站队列        | 是否堆积                     |
| 出站队列        | 是否堆积                     |
| 客户端 ACK 延迟 | 客户端处理能力               |

并发发送压测建议如下：

| 建议               | 说明                                |
| ------------------ | ----------------------------------- |
| 先不落库测通道     | 分离 WebSocket 通道能力和数据库瓶颈 |
| 再开启完整业务链路 | 验证真实场景                        |
| 控制消息大小       | 分别测试小消息和较大 JSON           |
| 限流规则要明确     | 压测账号可配置白名单或独立阈值      |
| 观察尾延迟         | P99 比平均值更重要                  |

### 广播推送压测

广播推送压测用于评估服务端向大量订阅者同时推送系统公告、看板数据、告警等广播消息时的能力。

压测模型示例：

| 参数                  | 示例                       |
| --------------------- | -------------------------- |
| 总连接数              | 10000                      |
| 订阅广播 Topic 连接数 | 10000                      |
| 广播频率              | 1 条/秒、5 条/秒、10 条/秒 |
| 消息大小              | 500B、2KB、10KB            |
| Topic                 | `/topic/system/notice`     |

测试步骤：

```text
1. 所有客户端连接并订阅 /topic/system/notice
2. 服务端按固定频率广播消息
3. 客户端统计接收数量和延迟
4. 逐步提升广播频率和消息大小
5. 观察出站队列、Broker、CPU、网络带宽
```

重点指标：

| 指标             | 说明                        |
| ---------------- | --------------------------- |
| 广播发送速率     | 服务端每秒广播条数          |
| 实际客户端接收量 | 是否全部收到                |
| 推送延迟         | P95/P99                     |
| 出站队列长度     | 是否持续增长                |
| 网络带宽         | 是否成为瓶颈                |
| Broker 积压      | Broker Relay 模式下重点观察 |
| 慢消费者数量     | 是否出现大量慢连接          |

广播压测建议如下：

| 建议                         | 说明                           |
| ---------------------------- | ------------------------------ |
| 按消息大小分组测试           | 小消息和大消息差异明显         |
| 不要只测单客户端             | 广播成本取决于订阅者数量       |
| 开启和关闭 Broker Relay 对比 | 找到 Simple Broker 边界        |
| 看板类消息测试覆盖旧值策略   | 高频时是否合并或降频           |
| 记录客户端接收率             | 服务端发送成功不代表客户端收到 |

### 点对点推送压测

点对点推送压测用于评估向指定用户或指定 Session 推送消息的能力，常用于用户通知、单聊、审批待办、任务结果等场景。

压测模型示例：

| 参数         | 示例                 |
| ------------ | -------------------- |
| 在线用户数   | 10000                |
| 每用户连接数 | 1 到 3               |
| 推送目标     | 随机在线用户         |
| 推送速率     | 500 到 5000 条/秒    |
| 推送路径     | `/user/queue/notice` |
| 消息大小     | 500B 到 2KB          |

测试步骤：

```text
1. 建立大量用户连接
2. 每个用户订阅 /user/queue/notice
3. 服务端随机选择在线用户推送
4. 客户端统计收到消息和延迟
5. 多实例下验证跨实例路由
6. 观察 Redis、Broker、出站线程池
```

重点指标：

| 指标         | 说明                         |
| ------------ | ---------------------------- |
| 用户路由耗时 | 查询 Session 映射耗时        |
| 推送成功率   | 点对点消息是否送达           |
| 跨实例转发量 | Pub/Sub 或 Broker Relay 指标 |
| Redis 延迟   | 用户 Session 查询            |
| ACK 延迟     | 客户端确认速度               |
| 失败消息数   | 离线或推送失败               |

点对点压测建议如下：

| 建议                       | 说明                            |
| -------------------------- | ------------------------------- |
| 测单连接和多端连接         | 同一用户多 Session 会放大推送量 |
| 测在线和离线混合           | 离线用户应进入补偿逻辑          |
| 多实例必须测试             | 目标用户可能不在当前实例        |
| 不使用 userId 作为指标标签 | 避免高基数                      |
| 观察 Redis 热点            | 大量用户路由会依赖 Redis        |

### 群组推送压测

群组推送压测用于评估群聊、房间、项目空间等多人 Topic 的推送能力。群组场景需要关注群规模、群数量、群消息频率和多实例同步。

压测模型示例：

| 场景       | 示例                  |
| ---------- | --------------------- |
| 小群多数量 | 1000 个群，每群 10 人 |
| 中群       | 100 个群，每群 500 人 |
| 大群       | 10 个群，每群 5000 人 |
| 超大房间   | 1 个房间，10000 人    |
| 群消息频率 | 每群 1 到 10 条/秒    |

测试步骤：

```text
1. 创建压测群组和成员关系
2. 客户端按群组订阅 /topic/group/{groupId}
3. 服务端按压测模型发送群消息
4. 客户端统计接收率和延迟
5. 观察 Broker、Redis、出站队列和网络
6. 测试群成员鉴权和缓存命中率
```

重点指标：

| 指标                 | 说明                        |
| -------------------- | --------------------------- |
| 群消息吞吐           | 每秒群消息数                |
| 实际投递量           | 群人数 × 消息数             |
| 群成员缓存命中率     | 订阅鉴权性能                |
| 群消息延迟           | P95/P99                     |
| Broker Topic 压力    | Broker Relay 模式下重点关注 |
| Redis Pub/Sub 事件量 | 跨实例同步模式下关注        |
| 客户端接收率         | 是否有丢失或延迟            |

群组压测建议如下：

| 建议                     | 说明                           |
| ------------------------ | ------------------------------ |
| 区分群消息数和实际投递数 | 1 条大群消息可能对应上万人投递 |
| 大群不要逐人推送         | 优先 Topic 或 Broker Relay     |
| 测试订阅鉴权缓存         | 群成员校验不能每次查库         |
| 测试离线补偿             | 群消息按 sequence 补偿         |
| 在线人数推送要节流       | 大房间人数变化不能每次广播     |

### 心跳压测

心跳压测用于评估大量连接下心跳带来的网络、CPU、调度线程和网关压力。心跳频率过高会显著增加系统负载。

压测变量如下：

| 变量     | 示例               |
| -------- | ------------------ |
| 连接数   | 5000、10000、20000 |
| 心跳间隔 | 10s、20s、30s、60s |
| 网络环境 | 内网、公网、弱网   |
| 网关超时 | 60s、300s、3600s   |

测试步骤：

```text
1. 建立目标数量连接
2. 配置指定心跳间隔
3. 保持连接 30 到 120 分钟
4. 不发送业务消息，只保留心跳
5. 观察 CPU、网络包、心跳超时、断线数
6. 调整心跳间隔重复测试
```

重点指标：

| 指标               | 说明             |
| ------------------ | ---------------- |
| 心跳消息量         | 每秒心跳帧数量   |
| 心跳超时数         | 是否误判断开     |
| CPU 使用率         | 心跳处理成本     |
| 网络流量           | 心跳包带宽       |
| 网关断开数         | 超时配置是否合理 |
| Session TTL 过期数 | Redis 是否误清理 |

心跳压测建议如下：

| 建议                   | 说明                           |
| ---------------------- | ------------------------------ |
| 大连接数不要心跳过短   | 10 秒以下要谨慎                |
| 网关超时必须匹配       | 超时小于心跳会误断开           |
| Redis TTL 大于心跳多倍 | 推荐 3 到 5 倍以上             |
| 移动端单独测试         | 弱网和后台休眠影响明显         |
| 观察误断开率           | 心跳压测核心不是吞吐，而是稳定 |

### 断线重连压测

断线重连压测用于评估大量客户端同时断开、重连、恢复订阅和补偿消息时，服务端能否稳定处理。

压测场景如下：

| 场景            | 说明                    |
| --------------- | ----------------------- |
| 随机断开        | 每分钟随机断开部分连接  |
| 集体断开        | 模拟网关重启或网络抖动  |
| 服务重启        | WebSocket 实例滚动发布  |
| Token 过期      | 客户端刷新 Token 后重连 |
| Broker 短暂中断 | Relay 恢复后客户端重连  |
| Redis 短暂异常  | 状态恢复后重连          |

测试步骤：

```text
1. 建立大量连接并订阅主题
2. 随机或批量断开连接
3. 客户端按重连策略自动重连
4. 重连后恢复订阅
5. 调用补偿接口
6. 校验消息无重复、无明显丢失
7. 观察建连峰值、认证压力、Redis 压力
```

重点指标：

| 指标           | 说明                 |
| -------------- | -------------------- |
| 重连成功率     | 客户端是否恢复连接   |
| 重连耗时       | 从断开到恢复时间     |
| 认证失败数     | Token 是否失效       |
| 订阅恢复数     | 主题是否重新订阅     |
| 补偿接口耗时   | 离线消息补偿压力     |
| Session 残留数 | 断线后是否清理       |
| 连接风暴       | 是否出现瞬时建连过载 |

断线重连压测建议如下：

| 建议                  | 说明                     |
| --------------------- | ------------------------ |
| 使用指数退避          | 防止所有客户端同时重连   |
| 服务端建连限流        | 防止重连风暴             |
| 重连后先订阅再补偿    | 避免补偿期间实时消息漏收 |
| 前端按 messageId 去重 | 补偿和实时可能重复       |
| 滚动发布必须测试      | 生产发布最常见断线来源   |

### Broker Relay 压测

Broker Relay 压测用于评估 RabbitMQ、ActiveMQ 等外部 STOMP Broker 在 WebSocket 多实例场景下的消息分发能力和稳定性。

压测内容如下：

| 场景              | 说明                              |
| ----------------- | --------------------------------- |
| 多实例连接 Broker | 多个 WebSocket 实例同时连接 Relay |
| 大量 Topic 订阅   | 客户端订阅大量 Topic              |
| 广播消息          | Broker 分发给所有订阅者           |
| 用户队列          | 点对点消息分发                    |
| Broker 故障恢复   | Broker 重启、网络抖动             |
| Broker 积压       | 慢消费者导致队列堆积              |

重点指标：

| 指标          | 说明                  |
| ------------- | --------------------- |
| Broker 连接数 | STOMP 连接是否稳定    |
| Topic 数量    | 订阅规模              |
| 消息发布速率  | publish rate          |
| 消息分发速率  | deliver rate          |
| 队列积压      | ready、unacked        |
| Broker CPU    | 消息分发压力          |
| Broker 内存   | 队列和连接占用        |
| 心跳失败      | Relay 心跳状态        |
| 重连次数      | Broker 抖动后恢复情况 |

Broker Relay 压测建议如下：

| 建议               | 说明                              |
| ------------------ | --------------------------------- |
| 单独压 Broker      | 先确认 Broker 容量                |
| 再压完整链路       | WebSocket + Broker + Redis 一起测 |
| 监控 Broker 积压   | 积压增长说明消费或分发不足        |
| 测 Broker 重启     | 验证服务恢复能力                  |
| 配置专用账号       | 压测不要使用生产账号              |
| 消息持久化按需开启 | 持久化会影响吞吐                  |

### Redis 缓存压测

Redis 缓存压测用于评估在线状态、Session 映射、群组成员、未读数、幂等、限流、Pub/Sub 和 Stream 等 Redis 功能在高并发下的性能。

压测场景如下：

| 场景         | Redis 操作                   |
| ------------ | ---------------------------- |
| 用户连接     | `HSET`、`SADD`、`EXPIRE`     |
| 用户断开     | `DEL`、`SREM`                |
| 在线判断     | `SISMEMBER`                  |
| Session 查询 | `HGETALL`                    |
| 限流         | `INCR`、`EXPIRE`             |
| 幂等         | `SET NX EX`                  |
| 未读数       | `INCR`、`DECR`、`GET`        |
| 群成员校验   | `SISMEMBER`                  |
| Pub/Sub      | `PUBLISH`、订阅消费          |
| Stream       | `XADD`、`XREADGROUP`、`XACK` |

重点指标：

| 指标           | 说明               |
| -------------- | ------------------ |
| Redis QPS      | 每秒命令数         |
| P95/P99 延迟   | Redis 响应时间     |
| CPU            | Redis 单线程压力   |
| 内存           | Key 和数据结构占用 |
| 连接数         | 应用 Redis 连接池  |
| 慢查询         | slowlog            |
| Pub/Sub 延迟   | 跨实例事件延迟     |
| Stream Pending | 消费积压           |

Redis 压测建议如下：

| 建议                | 说明                            |
| ------------------- | ------------------------------- |
| 按真实 Key 模型压测 | 不要只用简单 GET/SET            |
| 控制大 Key          | 群成员 Set、在线集合要观察大小  |
| 避免 KEYS 命令      | 生产禁止全量扫描                |
| 连接池要调优        | Lettuce/Jedis 连接数要匹配      |
| 设置合理 TTL        | Session 和幂等 Key 防止无限增长 |
| 监控慢查询          | 超时会直接影响连接和发送        |

### 长时间稳定性压测

长时间稳定性压测用于验证 WebSocket 服务在持续连接、持续心跳、持续消息推送、周期性重连、Redis/Broker 正常运行的情况下是否存在内存泄漏、Session 泄漏、线程池堆积、GC 恶化和性能衰减。

建议测试时长：

| 环境           | 时长          |
| -------------- | ------------- |
| 开发验证       | 1 到 2 小时   |
| 测试环境       | 8 到 24 小时  |
| 预生产         | 24 到 72 小时 |
| 上线前容量验证 | 至少 24 小时  |

稳定性压测模型示例：

```text
连接数：10000
在线用户：8000
每秒入站消息：500
每秒点对点推送：1000
广播频率：每 10 秒 1 条
群组消息：每秒 200 条
随机断线比例：每分钟 1%
持续时间：24 小时
```

观察指标：

| 指标        | 期望                 |
| ----------- | -------------------- |
| 当前连接数  | 稳定在目标范围       |
| JVM 内存    | 不持续单向增长       |
| Full GC     | 不频繁出现           |
| Session Key | 与实际连接数基本一致 |
| 失败消息    | 不持续增长           |
| 死信消息    | 不异常增长           |
| 线程池队列  | 不长期堆积           |
| 推送延迟    | P95/P99 稳定         |
| Redis 延迟  | 不持续升高           |
| Broker 积压 | 不持续增长           |

长时间稳定性压测建议如下：

| 建议             | 说明                                |
| ---------------- | ----------------------------------- |
| 记录完整监控曲线 | 不能只看结束时状态                  |
| 定期采集堆内存   | 排查泄漏                            |
| 随机断线重连     | 模拟真实网络                        |
| 模拟慢消费者     | 验证背压和断开策略                  |
| 压测后校验残留   | Redis Session、数据库失败表、离线表 |
| 保留压测报告     | 记录容量边界和配置参数              |

压测报告建议包含以下内容：

```text
1. 压测目标
2. 压测环境
3. 应用配置
4. Redis/Broker 配置
5. 压测工具和脚本
6. 场景模型
7. 指标结果
8. 瓶颈分析
9. 异常记录
10. 优化建议
11. 最终容量结论
```

容量结论建议写成可执行格式：

```text
在 4C8G × 3 个 WebSocket 实例、Redis 单主从、RabbitMQ 3 节点配置下：
- 可稳定保持 30000 个 WebSocket 连接
- P95 点对点推送延迟小于 500ms
- P95 广播推送延迟小于 1000ms
- 建议单实例连接数控制在 10000 以内
- 建议出站线程池队列告警阈值设置为 70%
- 建议生产心跳间隔为 20 秒
```

压测结论必须绑定环境、配置和业务模型，不能脱离压测条件直接推广到生产。

## 部署方案

部署方案用于说明 Spring Boot 3 WebSocket STOMP 服务在单实例、多实例、Docker、Kubernetes、Nginx、网关、HTTPS/WSS、跨域、负载均衡、灰度发布和滚动升级场景下的部署方式。WebSocket 是有状态长连接，部署时必须重点关注连接保持、网关超时、负载均衡、Session 状态同步、Broker Relay、Redis 在线状态和发布期间的连接迁移。

### 单实例部署

单实例部署适合本地开发、测试环境、小型内部系统或连接量较低的管理后台。单实例部署通常使用 Spring 内置 Simple Broker，不需要外部 STOMP Broker，但仍建议引入 Redis 用于未读数、幂等、限流和后续平滑扩展。

单实例部署结构如下：

```text
Browser / Admin Web
        |
        | ws:// 或 wss://
        v
Nginx / Gateway
        |
        v
Spring Boot WebSocket STOMP
        |
        +-- Simple Broker
        +-- Redis
        +-- MySQL
```

单实例适用场景如下：

| 场景     | 说明                            |
| -------- | ------------------------------- |
| 本地开发 | 快速验证 STOMP 连接、订阅、发送 |
| 测试环境 | 联调前后端消息协议              |
| 小型后台 | 在线连接数较少，推送频率不高    |
| 内部系统 | 用户规模可控                    |
| 原型验证 | 不引入 RabbitMQ 或 ActiveMQ     |

单实例部署建议如下：

| 建议                 | 说明                       |
| -------------------- | -------------------------- |
| 使用 Simple Broker   | 配置简单，便于快速启动     |
| 仍然配置心跳         | 防止异常连接长期残留       |
| 仍然限制连接数       | 防止前端 Bug 频繁建连      |
| Redis 可选但建议保留 | 为限流、未读数、幂等做准备 |
| 不适合高可用         | 单实例宕机会断开全部连接   |
| 不适合大规模广播     | 出站推送压力集中在单实例   |

单实例启动命令示例：

```bash
java -jar spring-websocket-stomp.jar \
  --spring.profiles.active=dev \
  --server.port=8080
```

生产环境即使从单实例开始，也建议配置好 `/actuator/health`、`/actuator/prometheus`、Nginx WebSocket 转发、日志归档和连接数监控。

### 多实例部署

多实例部署适合生产环境或连接数较高的系统。多实例部署的核心问题是 WebSocket Session 分布在不同实例中，必须通过 Redis、Broker Relay 或跨实例事件机制实现在线状态共享和消息同步。

多实例部署结构如下：

```text
Browser / APP / Admin
        |
        v
Nginx / Gateway / Load Balancer
        |
        +-------------------+-------------------+
        |                   |                   |
        v                   v                   v
 WebSocket Node 1     WebSocket Node 2     WebSocket Node 3
        |                   |                   |
        +-------------------+-------------------+
                            |
                            v
                  Redis / RabbitMQ / MySQL
```

多实例部署必须解决以下问题：

| 问题              | 推荐方案                      |
| ----------------- | ----------------------------- |
| 全局在线用户      | Redis Set                     |
| 用户 Session 映射 | Redis Hash + Set              |
| 跨实例点对点推送  | Broker Relay 或 Redis Pub/Sub |
| 广播消息同步      | Broker Relay 或 Redis Pub/Sub |
| 群组消息同步      | Broker Relay 或 Redis Pub/Sub |
| 分布式限流        | Redis                         |
| 分布式踢人        | Redis Pub/Sub                 |
| 实例宕机清理      | Session TTL + 实例心跳        |

多实例部署建议如下：

| 建议                     | 说明                                  |
| ------------------------ | ------------------------------------- |
| WebSocket 服务无状态化   | 本地只保存当前实例连接                |
| Session 全局状态放 Redis | 支持跨实例查询                        |
| 生产优先 Broker Relay    | RabbitMQ 或 ActiveMQ 统一分发订阅消息 |
| 网关支持长连接           | 超时时间必须大于心跳                  |
| 发布时使用滚动升级       | 避免全部连接同时断开                  |
| 配置重连退避             | 防止发布后重连风暴                    |

多实例场景下，不建议仅依赖 Sticky Session。Sticky Session 只能让客户端连接尽量固定到某个实例，不能解决业务服务调用任意实例推送、跨实例踢人、全局在线统计和广播同步问题。

### Docker 部署

Docker 部署用于将 Spring Boot WebSocket 服务打包为容器镜像，便于在测试、生产和 Kubernetes 环境中统一运行。容器化部署时要注意 JVM 内存、时区、日志输出、健康检查和优雅停机。

Dockerfile 示例：

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/spring-websocket-stomp.jar /app/app.jar

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
```

构建镜像：

```bash
docker build -t spring-websocket-stomp:1.0.0 .
```

启动容器：

```bash
docker run -d \
  --name spring-websocket-stomp \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0" \
  spring-websocket-stomp:1.0.0
```

Docker Compose 示例：

```yaml
services:
  websocket-app:
    image: spring-websocket-stomp:1.0.0
    container_name: websocket-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      JAVA_OPTS: "-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
      SPRING_DATA_REDIS_HOST: redis
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      - redis
      - rabbitmq
    restart: always

  redis:
    image: redis:7.2
    container_name: websocket-redis
    ports:
      - "6379:6379"
    restart: always

  rabbitmq:
    image: rabbitmq:3.13-management
    container_name: websocket-rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
      - "61613:61613"
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin123
    restart: always
```

Docker 部署建议如下：

| 建议                 | 说明                    |
| -------------------- | ----------------------- |
| 使用 JRE 镜像        | 减小镜像体积            |
| 设置容器内存感知参数 | 使用 `MaxRAMPercentage` |
| 日志输出到 stdout    | 交给容器平台采集        |
| 配置健康检查         | 便于自动恢复            |
| 不在镜像写死配置     | 使用环境变量或配置中心  |
| 优雅停机             | 配合滚动发布和连接关闭  |

### Kubernetes 部署

Kubernetes 部署用于生产环境弹性扩缩容、滚动升级、健康检查、配置管理和服务发现。WebSocket 长连接在 Kubernetes 中需要关注 Ingress 超时、Pod 优雅终止、滚动升级、连接重建和 Redis/Broker 外部依赖。

Deployment 示例：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: websocket-service
  namespace: default
spec:
  replicas: 3
  selector:
    matchLabels:
      app: websocket-service
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: websocket-service
    spec:
      terminationGracePeriodSeconds: 60
      containers:
        - name: websocket-service
          image: spring-websocket-stomp:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: JAVA_OPTS
              value: "-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 10
            timeoutSeconds: 3
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 15
            timeoutSeconds: 3
          lifecycle:
            preStop:
              exec:
                command:
                  - sh
                  - -c
                  - "sleep 20"
          resources:
            requests:
              cpu: "500m"
              memory: "1Gi"
            limits:
              cpu: "2"
              memory: "2Gi"
```

Service 示例：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: websocket-service
  namespace: default
spec:
  selector:
    app: websocket-service
  ports:
    - name: http
      port: 8080
      targetPort: 8080
  type: ClusterIP
```

Kubernetes 部署建议如下：

| 建议                  | 说明                                |
| --------------------- | ----------------------------------- |
| `maxUnavailable=0`    | 滚动升级时避免可用实例减少过多      |
| 配置 `preStop`        | 给连接迁移和网关摘流留时间          |
| 配置 readinessProbe   | 未就绪 Pod 不接收新连接             |
| 配置 livenessProbe    | 异常实例自动重启                    |
| 配置资源限制          | 防止单 Pod 占用过多资源             |
| 避免频繁扩缩容        | WebSocket 长连接不适合频繁 HPA 抖动 |
| 使用外部 Redis/Broker | Pod 重启不影响全局状态              |

### Nginx WebSocket 配置

Nginx WebSocket 配置用于支持 HTTP Upgrade、长连接、真实 IP 透传、TLS 终止、负载均衡和超时控制。错误的 Nginx 配置会导致 WebSocket 握手失败、连接频繁断开或消息延迟。

Nginx 配置示例：

```nginx
upstream websocket_backend {
    # 可选：会话保持，不能替代 Redis 和 Broker
    ip_hash;

    server 10.0.0.11:8080 max_fails=3 fail_timeout=30s;
    server 10.0.0.12:8080 max_fails=3 fail_timeout=30s;
    server 10.0.0.13:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 443 ssl;
    server_name api.example.com;

    ssl_certificate /etc/nginx/certs/api.example.com.pem;
    ssl_certificate_key /etc/nginx/certs/api.example.com.key;

    location /ws/ {
        proxy_pass http://websocket_backend;

        # WebSocket 协议升级必需配置
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";

        # 透传真实请求信息
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 长连接超时，必须大于心跳间隔
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
        proxy_connect_timeout 60s;

        # WebSocket 不需要代理缓冲
        proxy_buffering off;
    }
}
```

Nginx 配置建议如下：

| 配置项               | 建议                            |
| -------------------- | ------------------------------- |
| `proxy_http_version` | 必须为 `1.1`                    |
| `Upgrade`            | 必须透传                        |
| `Connection`         | 必须设置为 `Upgrade`            |
| `proxy_read_timeout` | 大于心跳间隔，生产可配置 1 小时 |
| `proxy_buffering`    | 关闭                            |
| `X-Forwarded-For`    | 必须透传，用于真实 IP           |
| TLS                  | 生产使用 HTTPS/WSS              |

### 网关 WebSocket 配置

网关 WebSocket 配置用于在 Spring Cloud Gateway、Kubernetes Ingress 或 API Gateway 中转发 WebSocket 流量。网关必须支持协议升级和长连接，且不能错误重写 WebSocket 路径。

Spring Cloud Gateway 配置示例：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: websocket-route
          uri: lb:ws://websocket-service
          predicates:
            - Path=/ws/**
          filters:
            - PreserveHostHeader
```

如果使用 HTTP 服务发现，也可以配置：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: websocket-route
          uri: lb://websocket-service
          predicates:
            - Path=/ws/**
```

Kubernetes Ingress Nginx 示例：

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: websocket-ingress
  namespace: default
  annotations:
    nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "60"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - api.example.com
      secretName: api-example-com-tls
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /ws
            pathType: Prefix
            backend:
              service:
                name: websocket-service
                port:
                  number: 8080
```

网关配置建议如下：

| 建议                   | 说明                                           |
| ---------------------- | ---------------------------------------------- |
| 保持 `/ws/**` 路径一致 | 避免 Endpoint 匹配失败                         |
| Header 透传            | Authorization、Origin、Cookie、X-Forwarded-For |
| 超时足够长             | 避免网关主动断开                               |
| HTTPS 终止清晰         | 明确 TLS 在网关还是应用层                      |
| 不在网关替代鉴权       | 应用层仍要校验 CONNECT、SUBSCRIBE、SEND        |
| 限流前置               | IP 级连接限流可在网关层先做                    |

### HTTPS 与 WSS 配置

HTTPS 与 WSS 配置用于保证 WebSocket 连接在生产环境中加密传输。HTTPS 页面不能连接 `ws://`，必须连接 `wss://`，否则浏览器会阻止混合内容。

连接地址对应关系如下：

| 页面协议   | WebSocket 协议 |
| ---------- | -------------- |
| `http://`  | `ws://`        |
| `https://` | `wss://`       |

生产推荐：

```text
https://admin.example.com
        |
        | wss://api.example.com/ws/stomp
        v
Nginx / Gateway TLS 终止
        |
        | http://websocket-service:8080/ws/stomp
        v
Spring Boot WebSocket 服务
```

Spring Boot 内置 HTTPS 配置示例：

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:certs/server.p12
    key-store-password: changeit
    key-store-type: PKCS12
```

更推荐在生产中由 Nginx、Ingress 或网关统一终止 TLS，后端服务使用内网 HTTP 通信。

HTTPS/WSS 建议如下：

| 建议               | 说明                        |
| ------------------ | --------------------------- |
| 生产必须使用 WSS   | 防止 Token 和消息明文传输   |
| 证书在网关统一管理 | 便于续期和统一入口          |
| 后端可用内网 HTTP  | 降低应用复杂度              |
| Secure Cookie      | Cookie 认证时必须配合 HTTPS |
| HSTS 按需开启      | 强制浏览器使用 HTTPS        |
| Token 不放 URL     | URL 可能进入访问日志        |

### 跨域配置

跨域配置用于限制哪些前端来源可以建立 WebSocket 连接。WebSocket 的跨域控制主要依赖 Origin 校验，而不是普通 HTTP CORS 的全部机制。

配置示例：

```yaml
app:
  websocket:
    endpoint: /ws/stomp
    allowed-origin-patterns:
      - "https://admin.example.com"
      - "https://app.example.com"
      - "https://*.example.com"
```

服务端 Endpoint 配置：

```java
registry.addEndpoint(webSocketProperties.getEndpoint())
        .setAllowedOriginPatterns(webSocketProperties.getAllowedOriginPatterns().toArray(new String[0]))
        .withSockJS();
```

跨域配置建议如下：

| 建议                   | 说明                           |
| ---------------------- | ------------------------------ |
| 生产不要使用 `*`       | 应明确允许业务域名             |
| 区分环境配置           | dev、test、prod 使用不同白名单 |
| SockJS 也要配置 Origin | 前后端配置保持一致             |
| Cookie 认证更严格      | 必须校验 Origin                |
| 非法 Origin 记录审计   | 高频非法来源可能是攻击         |
| 网关不要移除 Origin    | 服务端需要读取 Origin          |

### 负载均衡配置

负载均衡配置用于将 WebSocket 连接分发到多个后端实例。负载均衡对 WebSocket 的影响比普通 HTTP 更大，因为每条连接建立后会长期占用某个实例。

负载均衡策略如下：

| 策略        | 说明                         |
| ----------- | ---------------------------- |
| 轮询        | 新连接平均分配               |
| IP Hash     | 同一 IP 尽量固定到同一实例   |
| Cookie 粘性 | 通过网关 Cookie 实现会话保持 |
| 最少连接    | 优先分配到连接数较少的实例   |
| 权重        | 按实例规格分配连接           |

推荐策略：

| 场景         | 推荐                          |
| ------------ | ----------------------------- |
| 简单部署     | IP Hash                       |
| 多实例生产   | 最少连接或轮询 + Redis/Broker |
| 实例规格不同 | 权重                          |
| 高可用集群   | 不依赖粘性，依赖 Redis/Broker |
| 灰度发布     | 按权重引流                    |

负载均衡建议如下：

| 建议                    | 说明                         |
| ----------------------- | ---------------------------- |
| 不只依赖 Sticky Session | 必须有跨实例消息路由         |
| 关注实例连接分布        | 防止某实例连接过多           |
| 新版本灰度控制连接比例  | 避免一次接入过多长连接       |
| 实例下线前摘流          | 不再接收新连接               |
| 配合客户端自动重连      | 老连接断开后自动接入可用实例 |

### 灰度发布

灰度发布用于将少量 WebSocket 连接或指定用户流量引导到新版本，验证连接、认证、订阅、推送、Broker、Redis、消息协议和前端兼容性后再逐步扩大范围。

灰度策略如下：

| 策略             | 说明                             |
| ---------------- | -------------------------------- |
| 按用户灰度       | 指定用户或用户组进入新版本       |
| 按租户灰度       | 指定租户进入新版本               |
| 按客户端版本灰度 | 新前端版本连接新后端             |
| 按权重灰度       | 网关按比例分配流量               |
| 按路径灰度       | 新 Endpoint，例如 `/ws/stomp-v2` |

灰度发布流程：

```text
1. 部署新版本 WebSocket 服务
2. 接入少量用户或测试租户
3. 观察连接成功率、推送失败率、异常日志
4. 验证协议兼容和消息格式
5. 逐步扩大流量
6. 出现异常时切回旧版本
```

灰度发布建议如下：

| 建议                          | 说明                                |
| ----------------------------- | ----------------------------------- |
| 消息协议向后兼容              | 旧前端不能因新字段崩溃              |
| 支持双版本 Endpoint           | 大版本升级时可 `/ws/stomp-v2`       |
| 新旧版本共用 Redis Key 要谨慎 | Key 格式变更需兼容                  |
| Broker destination 保持兼容   | 不要随意改 Topic 路径               |
| 监控按版本拆分                | 指标加 `version` 标签，但避免高基数 |
| 支持快速回滚                  | 镜像和配置都可回退                  |

### 滚动升级

滚动升级用于在不中断整体服务的情况下逐个替换 WebSocket 服务实例。WebSocket 长连接不会像 HTTP 请求一样快速完成，因此滚动升级必须考虑连接摘流、优雅停机和客户端重连。

滚动升级流程建议：

```text
1. 将目标实例从负载均衡摘流
2. readiness 设置为不可用，不再接收新连接
3. 向当前连接推送 SERVER_RESTART 或 SESSION_RECONNECT 消息
4. 等待客户端主动重连到其他实例
5. 达到宽限时间后关闭剩余连接
6. 停止旧实例
7. 启动新实例
8. 新实例健康检查通过后加入负载均衡
```

优雅停机配置：

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

Kubernetes 中配合：

```yaml
spec:
  terminationGracePeriodSeconds: 60
  containers:
    - name: websocket-service
      lifecycle:
        preStop:
          exec:
            command:
              - sh
              - -c
              - "sleep 20"
```

滚动升级建议如下：

| 建议                 | 说明                         |
| -------------------- | ---------------------------- |
| 配置客户端自动重连   | 发布期间连接断开后自动恢复   |
| 重连使用退避         | 防止重连风暴                 |
| 推送服务重启通知     | 前端可展示“正在重连”         |
| 升级前停止接收新连接 | readiness 摘流               |
| 升级后补偿消息       | 重连后通过 HTTP 查询缺失消息 |
| 避免所有实例同时重启 | 使用滚动发布                 |
| 关注连接数曲线       | 发布时连接应平滑迁移         |

## 配置管理

配置管理用于统一管理 WebSocket 的连接、Endpoint、Broker、心跳、线程池、安全、限流、Redis、RabbitMQ、监控和多环境配置。配置应集中、可审计、可覆盖，避免硬编码在 Java 代码中。

### 基础连接配置

基础连接配置用于定义协议版本、服务实例 ID、连接数限制、是否启用 SockJS、是否启用管理端能力等通用参数。

配置示例：

```yaml
app:
  websocket:
    # 当前 WebSocket 协议版本
    protocol-version: "1.0"

    # 当前服务实例 ID，生产环境可通过环境变量注入
    instance-id: ${HOSTNAME:local-node}

    # 是否启用 WebSocket 功能
    enabled: true

    # 单用户最大连接数
    max-session-per-user: 5

    # 单 Session 最大订阅数
    max-subscription-per-session: 50

    # 是否启用管理端接口
    admin-enabled: true
```

配置属性类示例。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsBaseProperties.java`

```java
package io.github.atengk.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 基础配置属性
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@ConfigurationProperties(prefix = "app.websocket")
public class WsBaseProperties {

    /**
     * 是否启用 WebSocket
     */
    private Boolean enabled = true;

    /**
     * 协议版本
     */
    private String protocolVersion = "1.0";

    /**
     * 服务实例 ID
     */
    private String instanceId = "local-node";

    /**
     * 单用户最大连接数
     */
    private Integer maxSessionPerUser = 5;

    /**
     * 单 Session 最大订阅数
     */
    private Integer maxSubscriptionPerSession = 50;

    /**
     * 是否启用管理端功能
     */
    private Boolean adminEnabled = true;
}
```

基础配置建议如下：

| 配置项                         | 建议                    |
| ------------------------------ | ----------------------- |
| `instance-id`                  | 生产使用 Pod 名或主机名 |
| `protocol-version`             | 与前端协议版本保持一致  |
| `max-session-per-user`         | 防止多标签页过多连接    |
| `max-subscription-per-session` | 防止恶意订阅            |
| `admin-enabled`                | 生产按权限开放          |

### Endpoint 配置

Endpoint 配置用于定义 WebSocket 连接地址、允许来源、是否启用 SockJS 和握手相关参数。

配置示例：

```yaml
app:
  websocket:
    endpoint:
      # STOMP WebSocket 连接地址
      path: /ws/stomp

      # 是否启用 SockJS
      sockjs-enabled: true

      # 允许的 Origin Pattern
      allowed-origin-patterns:
        - "https://admin.example.com"
        - "https://app.example.com"
        - "https://*.example.com"
```

属性类示例。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsEndpointProperties.java`

```java
package io.github.atengk.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket Endpoint 配置属性
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@ConfigurationProperties(prefix = "app.websocket.endpoint")
public class WsEndpointProperties {

    /**
     * WebSocket Endpoint 路径
     */
    private String path = "/ws/stomp";

    /**
     * 是否启用 SockJS
     */
    private Boolean sockjsEnabled = true;

    /**
     * 允许的来源 Pattern
     */
    private List<String> allowedOriginPatterns = new ArrayList<>();
}
```

Endpoint 配置建议如下：

| 建议              | 说明                           |
| ----------------- | ------------------------------ |
| 路径统一 `/ws/**` | 方便网关转发                   |
| 生产明确 Origin   | 不使用 `*`                     |
| SockJS 按需开启   | 现代浏览器可优先原生 WebSocket |
| 配置与前端同步    | 初始化接口返回 Endpoint        |
| 多版本可分路径    | 例如 `/ws/stomp-v2`            |

### Broker 配置

Broker 配置用于选择 Simple Broker、RabbitMQ STOMP Broker Relay 或 ActiveMQ Broker Relay，并统一管理 destination 前缀、Relay 地址、账号和心跳。

配置示例：

```yaml
app:
  websocket:
    broker:
      # broker 类型：simple、rabbitmq、activemq
      type: simple

      # 广播和队列前缀
      simple-destination-prefixes:
        - /topic
        - /queue

      # 应用消息前缀
      application-destination-prefixes:
        - /app

      # 用户目标前缀
      user-destination-prefix: /user

      # Broker Relay 配置
      relay-host: 127.0.0.1
      relay-port: 61613
      client-login: admin
      client-passcode: admin123
      system-login: admin
      system-passcode: admin123
```

Broker 配置建议如下：

| 环境           | 建议                            |
| -------------- | ------------------------------- |
| 本地开发       | Simple Broker                   |
| 测试多实例     | RabbitMQ Relay 或 Redis Pub/Sub |
| 生产单实例     | Simple Broker 或 RabbitMQ Relay |
| 生产多实例     | RabbitMQ/ActiveMQ Broker Relay  |
| 微服务事件桥接 | Kafka + WebSocket 推送服务      |

### 心跳配置

心跳配置用于定义客户端与服务端、服务端与 Broker Relay 之间的心跳间隔。心跳配置需要与网关超时、Redis Session TTL 和客户端重连策略协调。

配置示例：

```yaml
app:
  websocket:
    heartbeat:
      # 服务端向客户端发送心跳间隔，单位毫秒
      server-to-client: 10000

      # 服务端期望接收客户端心跳间隔，单位毫秒
      client-to-server: 10000

      # Redis Session TTL，单位秒
      session-ttl-seconds: 180

      # 心跳超时扫描间隔，单位毫秒
      timeout-scan-interval: 30000
```

属性类示例。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsHeartbeatProperties.java`

```java
package io.github.atengk.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 心跳配置属性
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@ConfigurationProperties(prefix = "app.websocket.heartbeat")
public class WsHeartbeatProperties {

    /**
     * 服务端向客户端发送心跳间隔，单位毫秒
     */
    private Long serverToClient = 10000L;

    /**
     * 服务端期望接收客户端心跳间隔，单位毫秒
     */
    private Long clientToServer = 10000L;

    /**
     * Session TTL，单位秒
     */
    private Long sessionTtlSeconds = 180L;

    /**
     * 超时扫描间隔，单位毫秒
     */
    private Long timeoutScanInterval = 30000L;
}
```

心跳配置建议如下：

| 配置项       | 建议                      |
| ------------ | ------------------------- |
| 心跳间隔     | 10 到 30 秒，移动端可更长 |
| Session TTL  | 心跳间隔 3 到 5 倍以上    |
| 网关超时     | 大于心跳间隔多倍          |
| 扫描间隔     | 30 到 60 秒               |
| 大连接数系统 | 压测后调整，不宜过短      |

### 线程池配置

线程池配置用于控制入站、出站、业务处理、推送和定时任务线程池。线程池配置必须支持多环境覆盖，避免生产环境使用默认小线程池。

配置示例：

```yaml
app:
  websocket:
    thread-pool:
      inbound:
        core-pool-size: 4
        max-pool-size: 16
        queue-capacity: 1000
        keep-alive-seconds: 60

      outbound:
        core-pool-size: 4
        max-pool-size: 32
        queue-capacity: 3000
        keep-alive-seconds: 60

      business:
        core-pool-size: 4
        max-pool-size: 16
        queue-capacity: 1000
        keep-alive-seconds: 60
        thread-name-prefix: ws-business-

      push:
        core-pool-size: 4
        max-pool-size: 32
        queue-capacity: 5000
        keep-alive-seconds: 60
        thread-name-prefix: ws-push-
```

线程池配置建议如下：

| 建议               | 说明                             |
| ------------------ | -------------------------------- |
| 入站和出站分离     | 避免互相影响                     |
| 队列必须有界       | 防止 OOM                         |
| 出站队列重点监控   | 慢消费者会导致堆积               |
| 业务处理独立线程池 | 不阻塞 STOMP 通道                |
| 拒绝策略明确       | 关键消息失败入库，非关键消息降级 |
| 配置可动态调整     | 推荐接入配置中心                 |

### 安全配置

安全配置用于管理 Origin 白名单、Token 校验、黑名单、XSS 检测、敏感词过滤、管理端权限和安全审计开关。

配置示例：

```yaml
app:
  websocket:
    security:
      # 是否启用 Token 认证
      token-auth-enabled: true

      # 是否启用 Origin 校验
      origin-check-enabled: true

      # 是否启用订阅鉴权
      subscribe-auth-enabled: true

      # 是否启用发送鉴权
      send-auth-enabled: true

      # 是否启用 XSS 内容检查
      xss-check-enabled: true

      # 是否启用安全审计日志
      audit-log-enabled: true

      # 管理端 Topic 前缀
      admin-destination-prefix: /topic/admin

      # 黑名单检查开关
      blacklist-enabled: true
```

安全配置建议如下：

| 配置项      | 生产建议 |
| ----------- | -------- |
| Token 认证  | 开启     |
| Origin 校验 | 开启     |
| 订阅鉴权    | 开启     |
| 发送鉴权    | 开启     |
| XSS 检测    | 开启     |
| 审计日志    | 开启     |
| 黑名单      | 开启     |
| 管理端隔离  | 开启     |

### 限流配置

限流配置用于统一管理连接、发送、订阅、广播、群组消息、消息大小和风险行为阈值。

配置示例：

```yaml
app:
  websocket:
    rate-limit:
      # 是否启用限流
      enabled: true

      # 单 IP 每分钟最大连接次数
      ip-connect-per-minute: 100

      # 单用户每分钟最大连接次数
      user-connect-per-minute: 20

      # 单用户每秒最大发送次数
      user-send-per-second: 5

      # 单 Session 每分钟最大订阅次数
      session-subscribe-per-minute: 100

      # 单群组每秒最大消息数
      group-send-per-second: 100

      # 管理端广播每分钟最大次数
      admin-broadcast-per-minute: 5

      # 单条消息最大字节数
      max-message-bytes: 65536

      # 风险分阈值
      risk-score-threshold: 30
```

限流配置属性类示例。

文件位置：`src/main/java/io/github/atengk/websocket/config/WsRateLimitProperties.java`

```java
package io.github.atengk.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 限流配置属性
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@ConfigurationProperties(prefix = "app.websocket.rate-limit")
public class WsRateLimitProperties {

    /**
     * 是否启用限流
     */
    private Boolean enabled = true;

    /**
     * 单 IP 每分钟最大连接次数
     */
    private Long ipConnectPerMinute = 100L;

    /**
     * 单用户每分钟最大连接次数
     */
    private Long userConnectPerMinute = 20L;

    /**
     * 单用户每秒最大发送次数
     */
    private Long userSendPerSecond = 5L;

    /**
     * 单 Session 每分钟最大订阅次数
     */
    private Long sessionSubscribePerMinute = 100L;

    /**
     * 单群组每秒最大消息数
     */
    private Long groupSendPerSecond = 100L;

    /**
     * 管理端广播每分钟最大次数
     */
    private Long adminBroadcastPerMinute = 5L;

    /**
     * 单条消息最大字节数
     */
    private Long maxMessageBytes = 65536L;

    /**
     * 风险分阈值
     */
    private Integer riskScoreThreshold = 30;
}
```

限流配置建议如下：

| 建议             | 说明                         |
| ---------------- | ---------------------------- |
| 配置可按环境调整 | 测试、生产阈值不同           |
| 管理端广播更严格 | 防止误操作                   |
| 支持白名单       | 压测账号、系统账号可独立配置 |
| 风险阈值可配置   | 自动封禁策略不能写死         |
| 限流命中要监控   | 限流过多可能是攻击或配置过低 |

### Redis 配置

Redis 配置用于在线状态、Session 映射、未读数、限流、幂等、分布式锁、Pub/Sub 和 Stream。生产环境建议使用 Redis Sentinel 或 Redis Cluster。

Spring Boot Redis 配置示例：

```yaml
spring:
  data:
    redis:
      host: redis.example.internal
      port: 6379
      password: ${REDIS_PASSWORD}
      database: 0
      timeout: 3s
      lettuce:
        pool:
          max-active: 32
          max-idle: 16
          min-idle: 4
          max-wait: 2s
```

Redis Sentinel 示例：

```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD}
      sentinel:
        master: redis-master
        nodes:
          - redis-sentinel-1:26379
          - redis-sentinel-2:26379
          - redis-sentinel-3:26379
```

Redis Cluster 示例：

```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD}
      cluster:
        nodes:
          - redis-cluster-1:6379
          - redis-cluster-2:6379
          - redis-cluster-3:6379
          - redis-cluster-4:6379
          - redis-cluster-5:6379
          - redis-cluster-6:6379
```

Redis 配置建议如下：

| 建议                 | 说明                       |
| -------------------- | -------------------------- |
| 生产配置密码         | 禁止无密码暴露             |
| 配置连接池           | 避免高并发时连接不足       |
| 设置超时             | 防止 Redis 卡住业务线程    |
| Key 使用统一前缀     | 便于排查和清理             |
| Session Key 设置 TTL | 防止实例宕机残留           |
| 监控慢查询           | Redis 延迟会影响连接和推送 |
| 避免高危命令         | 生产禁止频繁 `KEYS`        |

### RabbitMQ 配置

RabbitMQ 配置用于 STOMP Broker Relay、多实例消息分发和业务事件桥接。使用 RabbitMQ STOMP Broker Relay 前，需要启用 RabbitMQ STOMP 插件。

RabbitMQ 插件启用命令：

```bash
rabbitmq-plugins enable rabbitmq_stomp
rabbitmq-plugins enable rabbitmq_web_stomp
```

Spring Boot RabbitMQ 基础配置：

```yaml
spring:
  rabbitmq:
    host: rabbitmq.example.internal
    port: 5672
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    virtual-host: /
    connection-timeout: 5s
```

WebSocket Broker Relay 配置：

```yaml
app:
  websocket:
    broker:
      type: rabbitmq
      relay-host: rabbitmq.example.internal
      relay-port: 61613
      client-login: ${WS_BROKER_CLIENT_LOGIN}
      client-passcode: ${WS_BROKER_CLIENT_PASSCODE}
      system-login: ${WS_BROKER_SYSTEM_LOGIN}
      system-passcode: ${WS_BROKER_SYSTEM_PASSCODE}
      system-heartbeat-send-interval: 10000
      system-heartbeat-receive-interval: 10000
```

RabbitMQ 配置建议如下：

| 建议               | 说明                          |
| ------------------ | ----------------------------- |
| 不使用默认账号     | 禁止 guest/guest 暴露         |
| STOMP 账号单独创建 | 与业务 AMQP 账号隔离          |
| 配置最小权限       | 只允许需要的 destination      |
| 监控连接和积压     | Broker 是推送核心依赖         |
| 高可用部署         | 生产使用集群和持久化          |
| 心跳配置合理       | Broker Relay 心跳 10 到 30 秒 |
| 故障有降级         | Broker 异常时关键消息可补偿   |

### 监控配置

监控配置用于开启 Actuator、Prometheus、健康检查、指标标签和管理端监控接口。

依赖：

```xml
<!-- Spring Boot Actuator：健康检查和指标端点 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Prometheus 指标导出 -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

配置示例：

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
          - metrics
          - prometheus
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when_authorized
  metrics:
    tags:
      application: spring-websocket-stomp
      env: ${spring.profiles.active}
```

监控配置建议如下：

| 建议                    | 说明                            |
| ----------------------- | ------------------------------- |
| 开启 readiness/liveness | Kubernetes 需要                 |
| Prometheus 暴露受控     | 不要公网暴露                    |
| 指标增加应用标签        | 便于多服务区分                  |
| 避免高基数标签          | 不把 userId、sessionId 作为标签 |
| 自定义 WebSocket 指标   | 连接数、推送失败、队列长度      |
| 管理端接口鉴权          | `/api/admin/ws/**` 必须加权限   |

### 多环境配置

多环境配置用于区分 local、dev、test、pre、prod 中的 Endpoint、Origin、Broker、Redis、线程池、限流和监控配置。WebSocket 配置强依赖部署环境，不能一套配置覆盖所有环境。

推荐文件结构：

```text
src/main/resources/
  application.yml
  application-local.yml
  application-dev.yml
  application-test.yml
  application-pre.yml
  application-prod.yml
```

基础配置 `application.yml`：

```yaml
spring:
  application:
    name: spring-websocket-stomp

server:
  port: 8080

app:
  websocket:
    enabled: true
    protocol-version: "1.0"
    endpoint:
      path: /ws/stomp
    broker:
      application-destination-prefixes:
        - /app
      simple-destination-prefixes:
        - /topic
        - /queue
      user-destination-prefix: /user
```

本地配置 `application-local.yml`：

```yaml
app:
  websocket:
    instance-id: local-node
    endpoint:
      allowed-origin-patterns:
        - "http://localhost:5173"
        - "http://localhost:3000"
    broker:
      type: simple

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

生产配置 `application-prod.yml`：

```yaml
app:
  websocket:
    instance-id: ${HOSTNAME}
    endpoint:
      allowed-origin-patterns:
        - "https://admin.example.com"
        - "https://app.example.com"
    broker:
      type: rabbitmq
      relay-host: rabbitmq.prod.internal
      relay-port: 61613
      client-login: ${WS_BROKER_CLIENT_LOGIN}
      client-passcode: ${WS_BROKER_CLIENT_PASSCODE}
      system-login: ${WS_BROKER_SYSTEM_LOGIN}
      system-passcode: ${WS_BROKER_SYSTEM_PASSCODE}
    heartbeat:
      server-to-client: 20000
      client-to-server: 20000
      session-ttl-seconds: 180
    rate-limit:
      enabled: true
      ip-connect-per-minute: 100
      user-connect-per-minute: 20
      user-send-per-second: 5

spring:
  data:
    redis:
      host: redis.prod.internal
      port: 6379
      password: ${REDIS_PASSWORD}
      timeout: 3s
```

多环境配置建议如下：

| 环境  | Broker                    | Origin    | 限流     | 监控     |
| ----- | ------------------------- | --------- | -------- | -------- |
| local | Simple Broker             | localhost | 宽松     | 可选     |
| dev   | Simple Broker 或 RabbitMQ | 开发域名  | 宽松     | 开启     |
| test  | RabbitMQ                  | 测试域名  | 接近生产 | 开启     |
| pre   | RabbitMQ 集群             | 预发域名  | 生产同等 | 开启     |
| prod  | RabbitMQ/ActiveMQ 高可用  | 正式域名  | 严格     | 必须开启 |

多环境配置注意事项如下：

| 注意项            | 说明                         |
| ----------------- | ---------------------------- |
| 密码使用环境变量  | 不写入配置文件               |
| Origin 按环境隔离 | 测试域名不能进入生产白名单   |
| 生产禁止 `*`      | 防止任意来源连接             |
| 生产启用 WSS      | HTTPS 页面必须使用 WSS       |
| 限流阈值生产严格  | 防止攻击和误操作             |
| 配置变更要审计    | WebSocket 配置影响连接和安全 |
| 灰度时版本兼容    | 新旧版本配置要共存一段时间   |

## 运维与故障处理

运维与故障处理用于定位 WebSocket STOMP 在连接、订阅、发送、接收、集群同步、Broker、Redis、内存和连接生命周期中的问题。排查时应优先按链路拆分：客户端、网关、WebSocket 服务、Redis、Broker、数据库、前端订阅处理。不要只看浏览器报错，也不要只看服务端日志。

### 连接失败排查

连接失败通常发生在 WebSocket 握手阶段或 STOMP `CONNECT` 阶段。握手失败说明 WebSocket 协议升级没有成功；`CONNECT` 失败说明连接已建立，但 STOMP 认证、限流、黑名单或拦截器处理失败。

排查路径如下：

```text
1. 检查前端连接地址是否正确
2. 检查协议是否匹配：http -> ws，https -> wss
3. 检查 Nginx 或网关是否支持 WebSocket Upgrade
4. 检查 Origin 是否在白名单中
5. 检查 Token 是否正确传递
6. 检查 CONNECT 拦截器是否拒绝连接
7. 检查连接频率是否触发限流
8. 检查 IP 或用户是否命中黑名单
9. 检查服务端日志和浏览器控制台
```

连接失败常见原因如下：

| 现象                                   | 可能原因                              | 处理方式                        |
| -------------------------------------- | ------------------------------------- | ------------------------------- |
| 浏览器报 `WebSocket connection failed` | 地址错误、网关未转发 Upgrade          | 检查 URL、Nginx、Gateway        |
| HTTPS 页面连接 `ws://`                 | 浏览器阻止混合内容                    | 改为 `wss://`                   |
| 连接立即关闭                           | Token 无效、Origin 非法、黑名单       | 查看 CONNECT 和安全审计日志     |
| 本地能连，生产不能连                   | Nginx/Ingress 超时或 Upgrade 配置缺失 | 检查代理配置                    |
| 一直自动重连                           | Token 过期后前端未停止重连            | 前端识别认证失败并跳转登录      |
| SockJS 404                             | 后端未启用 SockJS 或路径不匹配        | 检查 `.withSockJS()` 与前端 URL |

使用 curl 检查健康接口，确认服务本身可访问。

```bash
curl -i http://localhost:8080/actuator/health
```

使用浏览器控制台或网络面板检查 WebSocket 请求，重点关注：

| 检查项               | 说明                             |
| -------------------- | -------------------------------- |
| Request URL          | 是否为 `/ws/stomp`               |
| Status Code          | 是否为 `101 Switching Protocols` |
| Origin               | 是否在后端白名单                 |
| Connection           | 是否为 `Upgrade`                 |
| Upgrade              | 是否为 `websocket`               |
| Sec-WebSocket-Accept | 是否存在                         |
| Response             | 是否有 403、401、404、502、504   |

连接失败日志建议重点搜索以下关键字：

```text
WebSocket 认证失败
WebSocket 连接失败
WebSocket 握手失败
WebSocket 连接频率超限
WebSocket 黑名单命中
Invalid Origin
AccessDeniedException
```

### 订阅失败排查

订阅失败发生在客户端发送 STOMP `SUBSCRIBE` 帧之后。连接成功不代表订阅成功，服务端仍会在 `SUBSCRIBE` 阶段校验目标地址、用户权限、群组成员关系、租户权限、管理端权限和订阅频率。

排查路径如下：

```text
1. 确认 WebSocket 已连接成功
2. 确认订阅地址是否正确
3. 确认订阅地址是否以 /topic 或 /user/queue 开头
4. 检查 ChannelInterceptor 中 SUBSCRIBE 拦截日志
5. 检查用户是否具备目标地址权限
6. 群组订阅检查用户是否为群成员
7. 管理端订阅检查用户是否为管理员
8. 检查是否触发订阅频率限制
9. 检查前端是否重复订阅或页面销毁后未清理
```

订阅失败常见原因如下：

| 订阅地址                      | 常见失败原因                     |
| ----------------------------- | -------------------------------- |
| `/topic/admin/**`             | 普通用户无管理端权限             |
| `/topic/group/{groupId}`      | 用户不是群成员，或群组已解散     |
| `/topic/tenant/{tenantId}/**` | 租户不匹配                       |
| `/user/queue/notice`          | 用户未认证或连接未绑定 Principal |
| `/topic/../../admin`          | 目标地址非法                     |
| 高频订阅                      | 触发订阅限流                     |

前端订阅示例应保证在连接成功后执行。

```typescript
client.onConnect = () => {
  client.subscribe('/user/queue/notice', (message) => {
    console.info('收到通知：', JSON.parse(message.body))
  })
}
```

如果订阅写在连接成功之前，部分客户端库不会立即生效，或者需要在重连后重新订阅。

订阅失败排查重点如下：

| 检查项         | 说明                                  |
| -------------- | ------------------------------------- |
| destination    | 是否与后端路由规范一致                |
| Principal      | 当前连接是否绑定用户                  |
| subscriptionId | 是否重复或异常                        |
| 群组缓存       | Redis 群成员缓存是否正确              |
| 订阅日志       | 是否有 `SUBSCRIBE_DENIED`             |
| 错误队列       | `/user/queue/errors` 是否收到错误消息 |

### 消息发送失败排查

消息发送失败包括客户端发送到服务端失败，以及服务端向客户端推送失败。两类问题要分开排查。

客户端发送失败排查路径：

```text
1. 检查客户端连接状态
2. 检查 SEND destination 是否以 /app 开头
3. 检查消息体是否为合法 JSON
4. 检查必填字段和字段长度
5. 检查发送用户是否有权限
6. 检查是否被禁言、限流、黑名单拦截
7. 检查 @MessageMapping 是否匹配
8. 检查业务异常和错误队列返回
```

服务端推送失败排查路径：

```text
1. 检查接收用户是否在线
2. 检查用户是否订阅了目标队列或 Topic
3. 检查 destination 是否正确
4. 检查 convertAndSendToUser 的 user 参数是否与 Principal.getName 一致
5. 检查 Broker 是否可用
6. 检查出站线程池是否堆积
7. 检查消息序列化是否异常
8. 检查失败消息表或推送失败日志
```

常见发送失败原因如下：

| 现象               | 可能原因                     | 处理方式                               |
| ------------------ | ---------------------------- | -------------------------------------- |
| 前端发送无响应     | 未连接或发送地址错误         | 检查 `client.connected` 和 destination |
| 服务端没有进入方法 | `@MessageMapping` 路径不匹配 | 去掉 `/app` 后匹配方法路径             |
| 返回参数错误       | JSON 字段缺失、类型错误      | 检查 DTO 校验                          |
| 返回无权限         | SEND 鉴权失败                | 检查用户角色、群权限                   |
| 被限流             | 发送频率过高                 | 降低频率或调整阈值                     |
| 接收方收不到       | 未订阅、离线、用户路由错误   | 检查 Session 和订阅                    |
| 推送失败           | Broker、序列化、出站通道异常 | 检查 Broker 和失败表                   |

发送路径和服务端方法匹配关系如下：

| 客户端发送地址           | 服务端方法                              |
| ------------------------ | --------------------------------------- |
| `/app/chat/private/send` | `@MessageMapping("/chat/private/send")` |
| `/app/chat/group/send`   | `@MessageMapping("/chat/group/send")`   |
| `/app/message/ack`       | `@MessageMapping("/message/ack")`       |
| `/app/notice/read`       | `@MessageMapping("/notice/read")`       |

### 消息丢失排查

消息丢失指客户端认为应该收到消息，但实际没有收到。排查时要明确消息是否已经生成、是否已经落库、是否已经推送、目标用户是否在线、前端是否订阅、是否被前端过滤或去重。

排查链路如下：

```text
业务事件产生
    |
消息生成
    |
消息落库
    |
事务提交
    |
WebSocket 推送
    |
Broker / 本地出站通道
    |
客户端订阅队列
    |
前端解析和展示
```

消息丢失排查步骤：

| 步骤 | 检查内容                               |
| ---- | -------------------------------------- |
| 1    | 根据 `bizId` 或 `messageId` 查询消息表 |
| 2    | 检查消息是否事务提交成功               |
| 3    | 检查是否有事务后推送日志               |
| 4    | 检查推送失败表是否有记录               |
| 5    | 检查用户在线状态和 Session 映射        |
| 6    | 检查客户端是否订阅正确路径             |
| 7    | 检查前端是否解析异常                   |
| 8    | 检查前端是否按 messageId 误去重        |
| 9    | 集群下检查跨实例同步或 Broker          |
| 10   | 离线场景检查补偿接口                   |

常见丢失原因如下：

| 原因             | 说明                                         |
| ---------------- | -------------------------------------------- |
| 事务未提交就推送 | 客户端收到后查不到数据，或事务回滚导致脏通知 |
| 用户离线         | 没有写离线消息或补偿数据                     |
| 订阅地址错误     | 客户端订阅的路径与服务端推送路径不一致       |
| 用户目的地错误   | `convertAndSendToUser` 的 userId 不匹配      |
| 集群不同步       | Simple Broker 多实例下只推送到本实例         |
| Broker 故障      | Relay 不可用导致推送失败                     |
| 前端解析失败     | JSON 结构不符合预期                          |
| 前端重复过滤     | 去重缓存误判                                 |
| 消息过期         | 过期策略导致不补偿                           |

消息丢失排查建议如下：

| 建议                    | 说明                     |
| ----------------------- | ------------------------ |
| 关键消息必须落库        | 没有持久化无法可靠排查   |
| 推送必须带 messageId    | 便于全链路追踪           |
| 客户端记录最后 sequence | 支持断线后补偿           |
| 服务端记录失败消息      | 推送失败不能只打日志     |
| 前端记录接收日志        | 便于判断是否前端处理丢失 |

### 重复消息排查

重复消息指客户端收到两条或多条语义相同的消息。重复消息可能来自客户端重复发送、服务端重复消费、MQ 重复投递、重试机制、实时推送与补偿重叠、前端重复订阅或多标签页重复连接。

排查路径如下：

```text
1. 检查重复消息的 messageId 是否相同
2. 如果 messageId 相同，说明是重复投递或重复订阅
3. 如果 messageId 不同但业务内容相同，说明幂等失效
4. 检查 clientMessageId 是否重复
5. 检查业务幂等键是否生效
6. 检查前端是否重复订阅同一 destination
7. 检查多标签页是否都建立连接
8. 检查补偿接口是否与实时消息重叠
9. 检查 MQ 是否重复投递
10. 检查重试任务是否重复执行
```

重复消息分类如下：

| 类型                      | 判断方式                   | 常见原因                 |
| ------------------------- | -------------------------- | ------------------------ |
| 同 messageId 重复         | messageId 一样             | 重复订阅、重试、补偿重叠 |
| 不同 messageId 但业务相同 | bizId、eventType 相同      | 幂等键缺失               |
| 同一客户端发送多次        | clientMessageId 不同或缺失 | 前端重复点击             |
| 同用户多端收到            | userId 相同、session 不同  | 正常多端同步             |
| 多标签页重复              | 同浏览器多个连接           | 多标签页策略缺失         |

重复消息处理建议如下：

| 层级       | 处理                                               |
| ---------- | -------------------------------------------------- |
| 前端       | 按 `messageId` 去重                                |
| 服务端接收 | 按 `senderId + clientMessageId` 幂等               |
| 业务事件   | 按 `bizType + bizId + eventType + receiverId` 幂等 |
| ACK 回执   | 按 `messageId + userId + ackType` 幂等             |
| 补偿接口   | 返回消息仍需前端去重                               |
| MQ 消费    | 消费端必须幂等                                     |

前端去重缓存不能无限增长，应设置容量或 TTL：

```typescript
const handledMessageIds = new Set<string>()

export const shouldHandleMessage = (messageId: string): boolean => {
  if (handledMessageIds.has(messageId)) {
    return false
  }

  if (handledMessageIds.size > 2000) {
    const first = handledMessageIds.values().next().value
    handledMessageIds.delete(first)
  }

  handledMessageIds.add(messageId)
  return true
}
```

### 用户收不到消息排查

用户收不到消息是最常见的运维问题。排查时不要直接判断“WebSocket 坏了”，应按用户、连接、订阅、消息、路由、Broker、前端处理逐层定位。

排查清单如下：

| 检查项                | 说明                                                        |
| --------------------- | ----------------------------------------------------------- |
| 用户是否在线          | Redis `ws:online:users` 是否包含 userId                     |
| 用户 Session 是否存在 | `ws:user:sessions:{userId}` 是否有值                        |
| Session 是否有效      | `ws:session:{sessionId}` 是否存在且未过期                   |
| 用户连接在哪个实例    | 查看 `instanceId`                                           |
| 是否订阅目标地址      | 服务端订阅日志或前端订阅表                                  |
| 推送 userId 是否正确  | 必须与 `Principal.getName()` 一致                           |
| destination 是否正确  | 用户队列一般为 `/queue/notice`，订阅为 `/user/queue/notice` |
| 推送是否失败          | 查失败消息表和日志                                          |
| 是否集群路由失败      | 当前实例是否能推到目标实例                                  |
| 前端是否处理失败      | 浏览器控制台是否 JSON 解析异常                              |

用户点对点消息路径要特别注意：

```text
客户端订阅：
/user/queue/notice

服务端推送：
convertAndSendToUser(userId, "/queue/notice", payload)
```

不要在 `convertAndSendToUser` 中传 `/user/queue/notice`，通常应传用户目标前缀之后的部分，即 `/queue/notice`。

错误示例：

```java
simpMessagingTemplate.convertAndSendToUser(userId, "/user/queue/notice", payload);
```

正确示例：

```java
simpMessagingTemplate.convertAndSendToUser(userId, "/queue/notice", payload);
```

用户收不到消息常见原因如下：

| 原因                 | 处理                                        |
| -------------------- | ------------------------------------------- |
| 用户未在线           | 写离线消息或等待重连补偿                    |
| Principal 名称不一致 | 统一使用 userId 作为 Principal name         |
| 前端订阅路径错误     | 订阅 `/user/queue/**`                       |
| 服务端推送路径错误   | `convertAndSendToUser(userId, "/queue/**")` |
| 集群路由失败         | 使用 Broker Relay 或 Redis Pub/Sub          |
| 被权限拦截           | 检查订阅和发送权限                          |
| 前端重复断线         | 检查重连和 Token 过期                       |
| 前端解析失败         | 检查消息格式和控制台                        |

### 集群广播异常排查

集群广播异常通常表现为部分用户能收到广播，部分用户收不到。根因通常是 Simple Broker 多实例下每个实例只知道本地订阅，广播只发生在当前实例。

排查步骤如下：

```text
1. 确认当前是否多实例部署
2. 确认使用 Simple Broker 还是 Broker Relay
3. 确认客户端分别连接在哪些实例
4. 在每个实例查看订阅数量
5. 检查广播消息由哪个实例发出
6. 如果是 Simple Broker，确认是否有 Redis Pub/Sub 跨实例广播
7. 如果是 Broker Relay，检查 Broker 连接和 Topic 分发
8. 检查跨实例事件是否被重复过滤
9. 检查广播 destination 是否一致
```

异常场景示例：

```text
用户 A 连接 ws-node-01
用户 B 连接 ws-node-02
管理端请求打到 ws-node-01
ws-node-01 使用 Simple Broker 向 /topic/system/notice 广播
只有 ws-node-01 上的用户收到，ws-node-02 上的用户收不到
```

解决方案如下：

| 方案                            | 说明                              |
| ------------------------------- | --------------------------------- |
| 使用 RabbitMQ Broker Relay      | 推荐生产多实例                    |
| 使用 ActiveMQ Broker Relay      | 适合已有 ActiveMQ 环境            |
| 使用 Redis Pub/Sub 同步广播事件 | 中小规模集群可用                  |
| 业务服务只发事件                | 所有 WebSocket 实例消费后本地推送 |
| 广播公告落库                    | 客户端可通过 HTTP 查询补偿        |

集群广播排查重点指标：

| 指标                 | 说明                     |
| -------------------- | ------------------------ |
| 各实例连接数         | 是否分布不均             |
| 各实例订阅数         | 是否只有某实例有订阅     |
| Redis Pub/Sub 消息量 | 跨实例事件是否发布       |
| Broker Topic 指标    | Relay 是否正常分发       |
| 推送失败率           | 是否某实例异常           |
| 客户端接收率         | 是否只有部分实例用户收到 |

### Broker 异常排查

Broker 异常主要发生在使用 RabbitMQ 或 ActiveMQ Broker Relay 时。Broker 异常会导致订阅异常、推送失败、消息延迟升高、队列积压和连接断开。

排查步骤如下：

```text
1. 检查 WebSocket 服务是否能连接 Broker
2. 检查 Broker STOMP 端口是否开放
3. 检查 Broker 用户名和密码是否正确
4. 检查 STOMP 插件是否启用
5. 检查 Broker 心跳是否正常
6. 检查 Broker 队列或 Topic 是否积压
7. 检查 Broker 内存和磁盘水位
8. 检查 WebSocket 服务 Relay 重连日志
9. 检查推送失败表和死信表
```

RabbitMQ 常用检查命令如下。

```bash
rabbitmq-diagnostics status
rabbitmq-diagnostics listeners
rabbitmqctl list_connections
rabbitmqctl list_queues name messages_ready messages_unacknowledged
```

确认 RabbitMQ STOMP 插件是否启用。

```bash
rabbitmq-plugins list | grep stomp
```

Broker 异常常见原因如下：

| 现象            | 可能原因                           | 处理                 |
| --------------- | ---------------------------------- | -------------------- |
| Relay 连接失败  | STOMP 端口未开放、插件未启用       | 开启插件和端口       |
| 认证失败        | 账号密码错误或权限不足             | 检查 Broker 用户权限 |
| 推送延迟高      | Broker 积压或慢消费者              | 查看队列和消费者     |
| Broker 频繁断开 | 心跳配置不匹配、网络不稳定         | 调整心跳和网络       |
| 消息失败率升高  | Broker 不可用或内存水位高          | 告警并降级           |
| 订阅异常        | destination 权限或 Broker 配置问题 | 检查权限策略         |

Broker 异常处理建议如下：

| 建议            | 说明                            |
| --------------- | ------------------------------- |
| 关键业务先落库  | Broker 异常不应影响最终业务状态 |
| 推送失败入库    | 等 Broker 恢复后重试            |
| Broker 异常告警 | Relay 断开必须告警              |
| 非关键推送降级  | 看板、在线人数可以暂停          |
| 客户端支持补偿  | 重连后 HTTP 查询最终状态        |

### Redis 异常排查

Redis 异常会影响在线状态、Session 映射、未读数、限流、幂等、分布式锁、Pub/Sub 和 Stream。Redis 异常不一定导致 WebSocket 连接立即断开，但可能导致用户路由错误、假在线、限流失效和重复消息。

排查步骤如下：

```text
1. 检查 Redis 是否可连接
2. 检查 Redis 延迟和慢查询
3. 检查连接池是否耗尽
4. 检查在线用户和 Session Key 是否异常增长
5. 检查 Key 是否设置 TTL
6. 检查 Pub/Sub 是否正常接收
7. 检查 Stream Pending 是否积压
8. 检查 Redis 内存是否达到上限
9. 检查应用日志中 Redis 超时异常
```

Redis 常用检查命令如下。

```bash
redis-cli INFO clients
redis-cli INFO memory
redis-cli INFO stats
redis-cli INFO commandstats
redis-cli SLOWLOG GET 10
```

检查在线用户数量。

```bash
redis-cli SCARD ws:online:users
```

检查某个用户 Session。

```bash
redis-cli SMEMBERS ws:user:sessions:10001
```

检查某个 Session 信息。

```bash
redis-cli HGETALL ws:session:session-001
```

Redis 异常常见原因如下：

| 现象           | 可能原因                       | 处理                         |
| -------------- | ------------------------------ | ---------------------------- |
| 用户显示假在线 | 实例宕机后 Session 未清理      | TTL + 实例心跳清理           |
| 用户路由失败   | Session 映射缺失或过期         | 检查 TTL 和心跳续期          |
| Redis 超时     | Redis 慢查询、网络、连接池不足 | 调整连接池和 Redis 性能      |
| 限流失效       | Redis 不可用                   | 本地降级限流或拒绝高风险请求 |
| 重复消息增加   | 幂等 Key 写入失败              | 数据库唯一约束兜底           |
| Pub/Sub 不同步 | 订阅线程异常或 Redis 断连      | 重连并记录告警               |
| Stream 积压    | 消费者失败                     | 处理 Pending 和重试          |

Redis 异常处理建议如下：

| 建议                 | 说明                                 |
| -------------------- | ------------------------------------ |
| Redis 失败要有降级   | 至少保证本地连接不崩溃               |
| Session TTL 必须存在 | 防止假在线                           |
| 幂等要有数据库兜底   | Redis 不是唯一防线                   |
| 连接池设置合理       | 高并发 CONNECT/SEND 会大量访问 Redis |
| 监控 Redis 延迟      | 延迟升高会影响整个 WebSocket 链路    |

### 内存升高排查

内存升高可能来自连接数增加、Session 未清理、出站队列堆积、线程池队列过大、消息体过大、本地缓存无限增长、日志异步队列堆积或慢消费者。

排查路径如下：

```text
1. 查看当前连接数是否增长
2. 查看 Session 数是否与连接数匹配
3. 查看出站线程池队列是否堆积
4. 查看推送线程池队列是否堆积
5. 查看是否存在慢消费者
6. 查看最近是否有大广播或大消息
7. 查看本地缓存大小
8. 查看 GC 日志和堆内存曲线
9. 生成 heap dump 分析对象占用
```

常见内存升高原因如下：

| 原因           | 表现                  | 处理                     |
| -------------- | --------------------- | ------------------------ |
| 连接数正常增长 | 内存随连接数线性增长  | 扩容或限制连接数         |
| Session 泄漏   | 断开后 Session 不减少 | 修复 DISCONNECT 清理     |
| 出站队列堆积   | 队列长度持续增长      | 背压、限流、断开慢消费者 |
| 大消息推送     | 瞬时内存升高          | 限制消息大小             |
| 本地缓存无界   | 内存持续增长          | 增加容量和 TTL           |
| 日志过多       | 异步日志队列堆积      | 降低日志级别或采样       |
| GC 不及时      | 老年代持续升高        | 分析堆对象和 GC 参数     |

JVM 诊断命令示例。

```bash
jcmd <pid> GC.heap_info
jcmd <pid> Thread.print
jcmd <pid> GC.class_histogram
```

生成堆转储文件用于离线分析。

```bash
jcmd <pid> GC.heap_dump /tmp/websocket-heap.hprof
```

内存升高处理建议如下：

| 建议               | 说明                           |
| ------------------ | ------------------------------ |
| 首先看连接数       | 长连接系统内存与连接数强相关   |
| 关注队列长度       | 队列堆积是 OOM 前兆            |
| 限制消息大小       | 防止大对象进入堆               |
| 慢消费者要处理     | 不处理会拖垮出站通道           |
| 本地缓存必须有上限 | 去重缓存、订阅缓存不能无限增长 |
| 生产保留 GC 日志   | 没有 GC 日志很难定位           |

### 连接泄漏排查

连接泄漏指客户端断开后，服务端或 Redis 中仍保留 Session、订阅、在线状态或本地对象，导致在线人数虚高、内存增长、用户假在线和推送路由错误。

排查步骤如下：

```text
1. 对比实际连接数和 Redis Session 数
2. 对比本地 SessionRegistry 和 Redis Session 映射
3. 检查 DISCONNECT 事件是否触发
4. 检查异常断开时是否清理 Session
5. 检查 Session TTL 是否设置和续期
6. 检查实例宕机后是否清理 instance sessions
7. 检查用户多端断开后是否仍在 online users
8. 检查订阅对象是否取消
```

泄漏常见原因如下：

| 原因                 | 说明                               |
| -------------------- | ---------------------------------- |
| DISCONNECT 未处理    | 断开事件没有清理本地和 Redis       |
| 异常关闭未覆盖       | 网络断开、网关超时没有进入正常逻辑 |
| Redis Session 无 TTL | 实例宕机后 Key 永久残留            |
| 用户多端逻辑错误     | 一个 Session 断开就误清理或不清理  |
| 页面销毁未取消订阅   | 前端重复订阅导致重复处理           |
| 本地缓存无清理       | SessionRegistry 没有 remove        |
| 滚动升级未优雅停机   | 大量连接异常断开后状态残留         |

连接泄漏检查命令示例。

```bash
redis-cli SCARD ws:online:users
redis-cli SCARD ws:instance:sessions:ws-node-01
```

检查 Session Key 是否有 TTL。

```bash
redis-cli TTL ws:session:session-001
```

如果返回 `-1`，说明 Key 没有过期时间，存在残留风险。

连接泄漏处理建议如下：

| 建议                            | 说明                     |
| ------------------------------- | ------------------------ |
| 所有 Session Key 设置 TTL       | 防止实例宕机残留         |
| 心跳或活跃事件续期              | 正常连接保持 TTL         |
| DISCONNECT 幂等清理             | 重复清理不能报错         |
| 实例心跳清理                    | 实例失联后清理其 Session |
| 用户在线状态按 Session 集合判断 | 不要简单 SREM 用户       |
| 定时校正在线状态                | 定期扫描和修复假在线     |

## 常见问题

### WebSocket 与 HTTP 的区别

HTTP 是请求响应模型，客户端发起请求，服务端返回响应后连接通常结束或复用；WebSocket 是长连接双向通信模型，连接建立后客户端和服务端都可以主动发送消息。

核心区别如下：

| 对比项         | HTTP                     | WebSocket                |
| -------------- | ------------------------ | ------------------------ |
| 通信模型       | 请求响应                 | 双向长连接               |
| 服务端主动推送 | 不天然支持               | 原生支持                 |
| 连接生命周期   | 短连接或连接复用         | 长连接                   |
| 适合场景       | 查询、提交、分页、初始化 | 实时通知、聊天、状态推送 |
| 负载均衡       | 相对简单                 | 需要考虑长连接和 Session |
| 可靠性         | 请求结果明确             | 需要 ACK、补偿、持久化   |
| 网关要求       | 普通 HTTP 转发           | 必须支持 Upgrade         |

推荐边界如下：

| 功能             | 推荐方式  |
| ---------------- | --------- |
| 初始化配置       | HTTP      |
| 历史消息查询     | HTTP      |
| 分页列表         | HTTP      |
| 文件上传下载     | HTTP      |
| 实时通知         | WebSocket |
| 聊天消息实时到达 | WebSocket |
| 在线状态推送     | WebSocket |
| 任务进度推送     | WebSocket |

### STOMP 与原生 WebSocket 的区别

原生 WebSocket 只提供双向传输通道，不定义消息路由、订阅、队列、ACK、Header 等语义。STOMP 是运行在 WebSocket 之上的简单文本消息协议，提供类似消息队列的发送和订阅模型。

对比如下：

| 能力         | 原生 WebSocket     | STOMP                               |
| ------------ | ------------------ | ----------------------------------- |
| 双向通信     | 支持               | 支持                                |
| Topic 订阅   | 需要自定义         | 支持                                |
| 用户队列     | 需要自定义         | 支持                                |
| Header       | 需要自定义消息格式 | STOMP Header                        |
| ACK          | 需要自定义         | 协议层和业务层都可支持              |
| 路由         | 需要自定义         | `/app`、`/topic`、`/queue`、`/user` |
| Spring 集成  | 自定义 Handler     | 原生支持                            |
| Broker Relay | 需要自定义         | 支持 RabbitMQ/ActiveMQ              |

选择建议如下：

| 场景                 | 推荐                        |
| -------------------- | --------------------------- |
| 简单点对点裸消息     | 原生 WebSocket 可用         |
| 聊天、通知、订阅模型 | STOMP                       |
| 多 Topic、多 Queue   | STOMP                       |
| Spring Boot 快速集成 | STOMP                       |
| 二进制协议或 IoT     | 原生 WebSocket 或自定义协议 |
| 大规模 Broker 分发   | STOMP Broker Relay          |

### Simple Broker 是否适合生产环境

Simple Broker 可以用于生产，但只适合规模较小、单实例或低复杂度场景。它不适合作为大规模、多实例、高可靠实时消息系统的核心 Broker。

Simple Broker 适合：

| 场景         | 说明                   |
| ------------ | ---------------------- |
| 单实例部署   | 内置 Broker 足够       |
| 内部管理后台 | 在线人数少、推送频率低 |
| 测试环境     | 快速联调               |
| 低频通知     | 消息量较小             |
| 原型系统     | 快速验证方案           |

Simple Broker 不适合：

| 场景           | 原因                           |
| -------------- | ------------------------------ |
| 多实例广播     | 订阅关系在各实例内存中，不共享 |
| 高可靠消息     | 不提供持久化、重试、死信       |
| 大规模订阅     | 内存和出站通道压力大           |
| Broker 高可用  | 应用重启后订阅关系丢失         |
| 跨节点统一分发 | 需要额外 Redis/Broker 机制     |

生产建议如下：

| 规模           | 建议                                 |
| -------------- | ------------------------------------ |
| 小型单实例     | Simple Broker 可以接受               |
| 中型多实例     | Redis Pub/Sub 或 Broker Relay        |
| 大型生产       | RabbitMQ/ActiveMQ Broker Relay       |
| 微服务事件驱动 | Kafka/RabbitMQ 事件 + WebSocket 推送 |

### 为什么需要 Broker Relay

Broker Relay 用于让 Spring WebSocket 服务把 STOMP 消息转发给外部 Broker，例如 RabbitMQ 或 ActiveMQ，由外部 Broker 负责订阅关系和消息分发。

需要 Broker Relay 的原因如下：

| 原因                   | 说明                                    |
| ---------------------- | --------------------------------------- |
| 支持多实例             | 多个 WebSocket 实例共享 Broker 分发能力 |
| 支持跨节点广播         | 连接在不同实例的用户都能收到            |
| 降低应用分发压力       | Broker 负责 Topic/Queue 分发            |
| 复用消息中间件运维能力 | 监控、连接、权限、集群                  |
| 提升扩展能力           | WebSocket 服务可水平扩展                |
| 支持更复杂路由         | Topic、Queue 语义更清晰                 |

没有 Broker Relay 时，多实例 Simple Broker 会出现：

```text
用户 A 连接实例 1
用户 B 连接实例 2
实例 1 广播 /topic/system/notice
只有实例 1 的订阅者收到
实例 2 的订阅者收不到
```

使用 Broker Relay 后：

```text
实例 1 广播消息
    |
    v
RabbitMQ / ActiveMQ
    |
    +--> 实例 1 连接的客户端
    +--> 实例 2 连接的客户端
    +--> 实例 3 连接的客户端
```

### 为什么用户点对点消息收不到

用户点对点消息收不到通常与用户目的地配置、Principal 绑定、订阅路径、推送路径和集群路由有关。

常见原因如下：

| 原因                          | 说明                                       |
| ----------------------------- | ------------------------------------------ |
| 前端没有订阅 `/user/queue/**` | 用户队列必须订阅用户目的地                 |
| 服务端推送路径写错            | `convertAndSendToUser` 应传 `/queue/**`    |
| Principal 名称不一致          | 推送 userId 必须等于 `Principal.getName()` |
| 用户未在线                    | 没有可投递的 Session                       |
| 用户连接在其他实例            | Simple Broker 无跨实例路由                 |
| 用户目标前缀配置错误          | `setUserDestinationPrefix("/user")`        |
| Token 未绑定用户              | CONNECT 后 Principal 为空                  |
| 前端解析失败                  | 收到消息但没有展示                         |

正确配置：

```java
registry.setUserDestinationPrefix("/user");
```

前端订阅：

```typescript
client.subscribe('/user/queue/notice', (message) => {
  console.info('收到用户通知：', JSON.parse(message.body))
})
```

服务端推送：

```java
simpMessagingTemplate.convertAndSendToUser(userId, "/queue/notice", payload);
```

排查口诀：

```text
订阅看 /user/queue/notice
推送看 /queue/notice
用户看 Principal.getName()
集群看 Broker Relay 或 Redis 路由
```

### 为什么断线后无法自动恢复

断线后无法自动恢复通常不是后端单方面问题，而是客户端重连、Token 刷新、订阅恢复和消息补偿没有完整实现。

常见原因如下：

| 原因                        | 说明                             |
| --------------------------- | -------------------------------- |
| 前端未配置 `reconnectDelay` | STOMP 客户端不会自动重连         |
| Token 过期                  | 重连仍使用旧 Token，被服务端拒绝 |
| 重连后未重新订阅            | 原订阅在旧连接上，已失效         |
| 补偿接口未调用              | 断线期间消息没有恢复             |
| 网关关闭连接                | 超时小于心跳间隔                 |
| 服务端清理状态异常          | Redis Session 残留或误清理       |
| 前端无限快速重连            | 触发连接限流                     |

前端重连配置：

```typescript
const client = new Client({
  brokerURL: 'wss://api.example.com/ws/stomp',
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  onConnect: () => {
    restoreSubscriptions()
    compensateMessages()
  }
})
```

断线恢复正确流程：

```text
1. WebSocket 断开
2. 客户端等待重连间隔
3. 检查 Token 是否即将过期
4. 必要时先刷新 Token
5. 使用新 Token 重新 CONNECT
6. 重连成功后恢复订阅
7. 调用 HTTP 补偿接口
8. 前端按 messageId 去重并排序
```

### 为什么集群下消息不同步

集群下消息不同步通常是因为使用了 Simple Broker，但没有 Broker Relay 或 Redis Pub/Sub 跨实例同步。每个 WebSocket 实例只知道本实例连接和订阅，无法自动把消息推送到其他实例的客户端。

典型表现如下：

| 现象                       | 说明                           |
| -------------------------- | ------------------------------ |
| A 用户能收到，B 用户收不到 | 两人连接在不同实例             |
| 广播只部分用户收到         | 广播只发生在当前实例           |
| 点对点偶发失败             | 请求打到没有目标用户连接的实例 |
| 群消息只部分成员收到       | 群成员连接分散在多个实例       |
| 管理端踢人无效             | 目标 Session 在其他实例        |

解决方案如下：

| 方案                  | 说明                   |
| --------------------- | ---------------------- |
| RabbitMQ Broker Relay | 推荐生产多实例方案     |
| ActiveMQ Broker Relay | 适合已有 ActiveMQ 体系 |
| Redis Pub/Sub         | 中小规模跨实例同步     |
| Kafka 事件桥接        | 微服务事件驱动场景     |
| Redis Session 映射    | 支持用户路由和在线状态 |
| Sticky Session        | 只能辅助，不能根治     |

结论：

```text
多实例 WebSocket 不能只复制应用实例。
必须引入 Redis 共享状态和 Broker/PubSub 跨实例消息同步。
```

### 为什么 Nginx 转发失败

Nginx 转发失败通常是因为缺少 WebSocket Upgrade 配置、超时时间过短、路径重写错误、HTTPS/WSS 不匹配或请求头未透传。

常见错误配置如下：

| 问题                            | 说明                            |
| ------------------------------- | ------------------------------- |
| 未设置 `proxy_http_version 1.1` | WebSocket Upgrade 需要 HTTP/1.1 |
| 未设置 `Upgrade` Header         | 后端无法升级协议                |
| 未设置 `Connection Upgrade`     | 协议升级失败                    |
| `proxy_read_timeout` 太短       | 长连接被 Nginx 断开             |
| 路径 rewrite 错误               | 后端 Endpoint 匹配不到          |
| HTTPS 页面使用 `ws://`          | 浏览器阻止混合内容              |
| Origin 被覆盖或丢失             | 后端来源校验失败                |

正确 Nginx 配置核心片段如下：

```nginx
location /ws/ {
    proxy_pass http://websocket_backend;

    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "Upgrade";

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
    proxy_buffering off;
}
```

排查 Nginx 转发问题时，重点看：

| 检查项            | 正常结果                        |
| ----------------- | ------------------------------- |
| HTTP 状态码       | `101 Switching Protocols`       |
| Upgrade Header    | `websocket`                     |
| Connection Header | `Upgrade`                       |
| 后端日志          | 有握手请求                      |
| Nginx error.log   | 无 upstream prematurely closed  |
| 浏览器控制台      | 无 mixed content、403、502、504 |

### 为什么 Token 无法在 Header 中传递

浏览器原生 WebSocket 构造函数不支持自定义 HTTP 握手 Header，因此不能像普通 HTTP 请求一样在握手阶段设置 `Authorization` Header。

错误认知：

```typescript
new WebSocket('wss://api.example.com/ws/stomp', {
  headers: {
    Authorization: 'Bearer token'
  }
})
```

浏览器不支持这种写法。

推荐做法是通过 STOMP `CONNECT` Header 传递 Token：

```typescript
const client = new Client({
  brokerURL: 'wss://api.example.com/ws/stomp',
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
    clientType: 'WEB',
    deviceId: 'browser-001'
  }
})
```

服务端在 `ChannelInterceptor` 的 `CONNECT` 阶段解析：

```java
String authorization = accessor.getFirstNativeHeader("Authorization");
```

其他方案对比：

| 方案                 | 建议                           |
| -------------------- | ------------------------------ |
| STOMP CONNECT Header | 推荐                           |
| Cookie               | 可用，但要处理 CSRF 和 Origin  |
| Query 参数           | 不推荐，可能进入日志           |
| 首条消息传 Token     | 不推荐，认证语义混乱           |
| SockJS Header        | 仍以 STOMP CONNECT Header 为主 |

如果使用 SockJS，也建议通过 STOMP `connectHeaders` 传递 Token，而不是依赖握手 Header。

### 为什么心跳不生效

心跳不生效可能是客户端未启用心跳、服务端未配置 TaskScheduler、Broker Relay 心跳配置错误、网关超时过短、客户端库不支持或浏览器后台休眠导致。

常见原因如下：

| 原因                            | 说明                                              |
| ------------------------------- | ------------------------------------------------- |
| 服务端 Simple Broker 未配置心跳 | 没有 `setHeartbeatValue`                          |
| 未配置 TaskScheduler            | Simple Broker 心跳需要调度器                      |
| 客户端心跳为 0                  | `heartbeatIncoming` 或 `heartbeatOutgoing` 未设置 |
| 网关超时小于心跳间隔            | 还没心跳就被断开                                  |
| Broker Relay 心跳未配置         | 服务端到 Broker 心跳异常                          |
| 浏览器后台休眠                  | 定时器被降频，心跳延迟                            |
| 移动端弱网                      | 心跳包丢失                                        |
| SockJS fallback 模式差异        | 不同传输模式表现不同                              |

服务端心跳配置示例：

```java
registry.enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[]{10000, 10000})
        .setTaskScheduler(webSocketHeartbeatTaskScheduler());
```

TaskScheduler 示例：

```java
@Bean
public TaskScheduler webSocketHeartbeatTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(4);
    scheduler.setThreadNamePrefix("ws-heartbeat-");
    scheduler.initialize();
    return scheduler;
}
```

前端心跳配置：

```typescript
const client = new Client({
  brokerURL: 'wss://api.example.com/ws/stomp',
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  reconnectDelay: 5000
})
```

心跳配置关系建议：

| 配置               | 建议                             |
| ------------------ | -------------------------------- |
| 客户端心跳         | 10 到 30 秒                      |
| 服务端心跳         | 10 到 30 秒                      |
| Redis Session TTL  | 心跳间隔 3 到 5 倍以上           |
| Nginx read timeout | 大于心跳间隔多倍，生产可 3600 秒 |
| Gateway timeout    | 大于心跳间隔                     |
| 移动端心跳         | 可放宽到 30 到 60 秒             |

心跳不生效排查路径：

```text
1. 检查前端是否设置 heartbeatIncoming 和 heartbeatOutgoing
2. 检查服务端是否 setHeartbeatValue
3. 检查 Simple Broker 是否配置 TaskScheduler
4. 检查 Nginx/Gateway 超时时间
5. 检查浏览器 Network 中是否有心跳帧
6. 检查服务端 Session TTL 是否续期
7. 检查是否是浏览器后台休眠导致
8. 检查 Broker Relay 心跳和连接状态
```

心跳的目标是发现异常连接并维持链路活性，但不能替代业务 ACK。关键消息仍应通过 ACK、回执、持久化和补偿保证可靠性。

## 最佳实践

最佳实践用于沉淀 Spring Boot 3 WebSocket STOMP 在生产项目中的推荐做法，覆盖路由命名、消息体设计、认证鉴权、会话管理、离线消息、集群部署、性能优化、日志监控、前端接入和生产配置。WebSocket STOMP 的核心原则是：实时通道只负责实时触达，关键业务状态必须以数据库、缓存、ACK、补偿和监控为支撑。

### 路由命名最佳实践

路由命名应保持稳定、清晰、分层明确。客户端发送、广播订阅、用户队列、群组主题、管理端主题必须区分，不能混用。

推荐路由前缀如下：

| 路由类型     | 前缀                     | 说明                        |
| ------------ | ------------------------ | --------------------------- |
| 客户端发送   | `/app/**`                | 客户端向服务端发送业务请求  |
| 广播主题     | `/topic/**`              | 服务端向多个订阅者广播      |
| 用户私有队列 | `/user/queue/**`         | 服务端向指定用户推送        |
| 普通队列     | `/queue/**`              | Broker 或服务端内部队列语义 |
| 管理端主题   | `/topic/admin/**`        | 管理端专用实时数据          |
| 群组主题     | `/topic/group/{groupId}` | 群聊、房间、项目空间        |
| 系统主题     | `/topic/system/**`       | 系统公告、系统状态          |

推荐命名规范：

```text
客户端发送：
/app/chat/private/send
/app/chat/group/send
/app/message/ack
/app/notice/read
/app/room/join
/app/admin/push/test

客户端订阅：
/user/queue/notice
/user/queue/chat
/user/queue/errors
/user/queue/session
/topic/system/notice
/topic/group/{groupId}
/topic/admin/dashboard/message
```

路由命名建议如下：

| 建议                         | 说明                                              |
| ---------------------------- | ------------------------------------------------- |
| 客户端只能 SEND 到 `/app/**` | 禁止客户端直接向 `/topic/**` 或 `/user/**` 发送   |
| 订阅路径按业务域划分         | chat、notice、admin、group、room 分层清晰         |
| 管理端路径单独隔离           | 所有管理端订阅统一 `/topic/admin/**`              |
| 群组路径带 groupId           | 便于订阅鉴权和消息分发                            |
| 路由不要带敏感信息           | 不在路径中放 Token、手机号、身份证号              |
| 不频繁变更路径               | 路由是前后端协议的一部分                          |
| 版本升级谨慎改路由           | 大版本可使用 `/ws/stomp-v2` 或消息 `version` 字段 |

错误示例：

```text
/topic/user/10001/notice
/app/topic/chat
/sendMessage
/admin
/ws/chat/send
```

正确示例：

```text
/user/queue/notice
/app/chat/private/send
/topic/group/90001
/topic/admin/dashboard/message
```

### 消息体设计最佳实践

消息体设计应统一外层结构，业务字段放入 `data`，保证前端可以统一解析、去重、分发、排序和异常处理。

推荐统一消息体：

```json
{
  "messageId": "1909550919275922501",
  "type": "USER_NOTICE",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "traceId": "trace-20260505-000001",
  "data": {
    "noticeId": "notice-100001",
    "title": "审批通知",
    "content": "你有一条新的审批待办",
    "url": "/approval/detail/100001"
  }
}
```

消息体字段建议：

| 字段        | 是否必填 | 说明                            |
| ----------- | -------- | ------------------------------- |
| `messageId` | 是       | 全局唯一消息 ID，用于去重和追踪 |
| `type`      | 是       | 消息类型，用于前端分发          |
| `status`    | 是       | 消息状态                        |
| `version`   | 是       | 协议版本                        |
| `timestamp` | 是       | 服务端时间戳                    |
| `traceId`   | 建议     | 链路追踪 ID                     |
| `data`      | 是       | 业务数据                        |

消息体设计建议如下：

| 建议                     | 说明                                  |
| ------------------------ | ------------------------------------- |
| 外层结构保持稳定         | 前端统一处理所有消息                  |
| `messageId` 必须全局唯一 | 去重、ACK、补偿都依赖它               |
| 时间统一使用毫秒时间戳   | 避免时区和格式差异                    |
| 枚举使用字符串           | 不使用数字 ordinal                    |
| 新增字段保持可选         | 兼容旧客户端                          |
| 大对象不直接推送         | 推送摘要或业务 ID，详情走 HTTP        |
| 敏感字段不进入消息体     | Token、Cookie、密码、内部堆栈禁止推送 |
| 错误消息统一格式         | 错误码、错误信息、是否可重试要明确    |

错误消息推荐结构：

```json
{
  "messageId": "1909550919275922502",
  "type": "ERROR",
  "status": "FAILED",
  "version": "1.0",
  "timestamp": 1777956000000,
  "traceId": "trace-20260505-000002",
  "data": {
    "code": "WS_ACCESS_DENIED",
    "message": "无操作权限",
    "destination": "/topic/admin/online",
    "requestMessageId": "client-msg-001",
    "retryable": false
  }
}
```

### 认证鉴权最佳实践

认证鉴权必须覆盖握手、`CONNECT`、`SUBSCRIBE`、`SEND` 和业务处理。不能只在 HTTP 登录阶段认证，也不能只在 WebSocket 连接建立时校验一次。

认证鉴权分层如下：

| 阶段           | 处理内容                           |
| -------------- | ---------------------------------- |
| 握手阶段       | Origin、IP、基础限流、黑名单       |
| CONNECT 阶段   | Token 校验、用户绑定、设备识别     |
| SUBSCRIBE 阶段 | 订阅路径权限、群组权限、管理端权限 |
| SEND 阶段      | 发送路径权限、限流、黑名单、禁言   |
| 业务处理阶段   | 业务对象权限、租户权限、数据归属   |

认证最佳实践：

| 建议                                | 说明                                       |
| ----------------------------------- | ------------------------------------------ |
| Token 放 STOMP CONNECT Header       | 浏览器原生 WebSocket 不能自定义握手 Header |
| 使用 `Authorization: Bearer xxx`    | 与 HTTP 认证风格保持一致                   |
| `Principal.getName()` 统一为 userId | 点对点推送依赖它                           |
| Token 过期后重建连接                | 不建议在同一连接上替换身份                 |
| 用户禁用后主动踢下线                | 防止旧连接继续发送消息                     |
| 日志中 Token 必须脱敏               | 不记录完整凭证                             |

鉴权最佳实践：

| 场景                                  | 推荐处理                |
| ------------------------------------- | ----------------------- |
| 普通用户订阅 `/topic/admin/**`        | 拒绝并记录安全审计      |
| 非群成员订阅 `/topic/group/{groupId}` | 拒绝订阅                |
| 被禁言用户发送群消息                  | 拒绝发送并返回错误      |
| 用户操作他人通知                      | 拒绝处理                |
| 租户不匹配                            | 拒绝订阅和发送          |
| 黑名单用户连接                        | 拒绝 CONNECT 或强制下线 |

前端 Token 传递示例：

```typescript
const client = new Client({
  brokerURL: 'wss://api.example.com/ws/stomp',
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`,
    clientType: 'WEB',
    deviceId: 'browser-001',
    clientVersion: '1.0.0'
  }
})
```

服务端推送用户消息时，必须确保 `userId` 与连接绑定的 Principal 一致：

```java
simpMessagingTemplate.convertAndSendToUser(userId, "/queue/notice", payload);
```

### 会话管理最佳实践

会话管理应同时维护本地 Session、Redis 全局 Session 映射、用户在线状态和数据库会话日志。本地内存只能代表当前实例，不能代表全局在线状态。

推荐 Session 结构：

```text
本地内存：
sessionId -> WebSocketSession

Redis：
ws:online:users
ws:user:sessions:{userId}
ws:session:{sessionId}
ws:instance:sessions:{instanceId}

数据库：
ws_session_log
ws_user_online_status
```

会话管理建议如下：

| 建议                                | 说明                               |
| ----------------------------------- | ---------------------------------- |
| Session 注册和清理必须幂等          | CONNECT、DISCONNECT 可能重复触发   |
| Redis Session Key 必须设置 TTL      | 防止实例宕机后残留                 |
| 心跳或活跃事件续期                  | 保持正常连接不过期                 |
| 用户在线按 Session 集合判断         | 同用户多端在线不能误判             |
| 断开一个 Session 不等于用户离线     | 需要判断是否还有其他 Session       |
| 会话日志写数据库                    | 便于审计和排障                     |
| 管理端支持按用户查看 Session        | 排查收不到消息、重复消息、多端登录 |
| 强制下线支持指定 Session 和用户全端 | 满足运维和安全需求                 |

多端登录建议：

| 场景       | 推荐策略                         |
| ---------- | -------------------------------- |
| 普通系统   | 允许同用户多端在线               |
| 管理端系统 | 可限制同端只允许一个连接         |
| 高安全系统 | 新连接踢旧连接                   |
| 聊天系统   | 多端同步消息                     |
| 多标签页   | 前端单主标签页或服务端限制连接数 |

强制下线流程：

```text
1. 管理端发起踢人请求
2. 查询 Redis 中目标 Session 所属 instanceId
3. 当前实例则本地处理
4. 其他实例通过 Redis Pub/Sub 发送踢人事件
5. 目标实例推送 SESSION_KICKED
6. 客户端主动断开
7. 服务端清理本地和 Redis Session
8. 记录操作日志
```

### 离线消息最佳实践

离线消息用于解决用户不在线、断线、重连、网络抖动时的消息补偿问题。WebSocket 不应承担完整历史消息存储能力，关键消息必须先持久化，再通过实时推送触达。

离线消息设计原则：

| 原则                   | 说明                                    |
| ---------------------- | --------------------------------------- |
| 关键消息先落库         | 不能只依赖实时推送                      |
| 离线补偿走 HTTP        | 不建议通过 WebSocket 一次性回放大量历史 |
| 前端按 messageId 去重  | 实时消息和补偿消息可能重复              |
| 聊天按 sequence 补偿   | 比按时间更可靠                          |
| 通知按未读状态补偿     | 通知中心以数据库状态为准                |
| 临时消息设置过期时间   | 避免长期堆积                            |
| 大群不逐人保存离线消息 | 使用历史消息 + sequence 补偿            |

不同业务的离线策略如下：

| 消息类型 | 推荐策略                                 |
| -------- | ---------------------------------------- |
| 单聊消息 | 持久化聊天表，按会话 sequence 补偿       |
| 群聊消息 | 持久化群消息，按 groupId + sequence 补偿 |
| 用户通知 | 写通知表，按未读和时间补偿               |
| 审批待办 | 写业务表和通知表，重连后查询未处理待办   |
| 任务进度 | 中间状态可丢，最终状态必须可查询         |
| 看板数据 | 不补偿历史，只拉取最新快照               |
| 系统公告 | 查询有效期内公告和未读状态               |

推荐断线补偿流程：

```text
1. 客户端连接成功
2. 恢复必要订阅
3. 读取本地 lastSequence 或 lastSyncTime
4. 调用 HTTP 补偿接口
5. 服务端返回缺失消息
6. 前端按 messageId 去重
7. 按 sequence 或 timestamp 排序
8. 更新本地同步游标
```

补偿接口示例：

```text
GET /api/chat/messages/compensate?chatId=group-90001&lastSequence=30001
GET /api/notices/unread
GET /api/tasks/{taskId}
```

### 集群部署最佳实践

集群部署必须解决 Session 分布、跨实例推送、广播同步、群组同步、分布式在线状态、分布式限流和实例宕机清理问题。WebSocket 多实例部署不是简单增加 Pod 数量。

推荐集群架构：

```text
Client
  |
Nginx / Gateway / Ingress
  |
WebSocket Node 1 / Node 2 / Node 3
  |
  +-- Redis：Session、在线状态、限流、幂等
  +-- RabbitMQ/ActiveMQ：STOMP Broker Relay
  +-- MySQL：消息、通知、回执、日志
```

集群部署建议如下：

| 建议                            | 说明                                  |
| ------------------------------- | ------------------------------------- |
| 生产多实例优先使用 Broker Relay | RabbitMQ 或 ActiveMQ 统一处理订阅分发 |
| Redis 保存全局在线状态          | 不依赖单实例内存                      |
| Session 信息保存 instanceId     | 支持跨实例路由和踢人                  |
| 广播通过 Broker 或 Pub/Sub 同步 | Simple Broker 不能自动跨实例广播      |
| 点对点推送必须考虑用户所在实例  | 不保证请求一定打到目标用户所在节点    |
| 分布式限流用 Redis              | 本地限流无法覆盖多实例                |
| 实例心跳和 Session TTL          | 解决实例宕机后的假在线                |
| Sticky Session 只能辅助         | 不能替代 Redis 和 Broker              |

集群下常见错误：

| 错误                          | 后果                       |
| ----------------------------- | -------------------------- |
| 只部署多个 Simple Broker 实例 | 广播和群消息只覆盖本实例   |
| 不保存 Session instanceId     | 无法跨实例踢人和路由       |
| 只用 Sticky Session           | 后端推送仍可能打到错误实例 |
| Redis Session 无 TTL          | 实例宕机后用户假在线       |
| Pub/Sub 消息无 eventId        | 容易重复处理               |
| 群组订阅不鉴权                | 非群成员可接收群消息       |

### 性能优化最佳实践

性能优化应围绕连接数、消息大小、线程池、心跳、广播、群组、慢消费者、背压和 GC 展开。WebSocket 系统的瓶颈通常不是单点 QPS，而是长连接资源、出站队列、慢消费者和消息 fan-out。

性能优化建议如下：

| 优化方向 | 推荐做法                            |
| -------- | ----------------------------------- |
| 连接数   | 控制单用户连接数，分实例承载        |
| 消息大小 | 限制单条消息大小，大文件走 HTTP     |
| 序列化   | 消息体字段精简，避免大对象          |
| 广播     | 控制范围、频率和消息大小            |
| 群组     | 大群使用 Topic，不逐用户推送        |
| 批量推送 | 分批、异步、失败可重试              |
| 心跳     | 10 到 30 秒，按压测结果调整         |
| 线程池   | 入站、出站、业务、推送分离          |
| 队列     | 必须有界，队列堆积要告警            |
| 慢消费者 | 降频、合并、丢弃旧快照或断开        |
| GC       | 限制大对象和无界队列，监控 P99 延迟 |
| Redis    | 避免大 Key 和高延迟操作             |
| Broker   | 监控积压、心跳、连接和慢消费者      |

不同消息类型的优化策略：

| 消息类型 | 优化策略                      |
| -------- | ----------------------------- |
| 聊天消息 | 小消息、持久化、sequence 排序 |
| 通知消息 | 合并未读数，离线补偿          |
| 系统公告 | 广播前落库，控制频率          |
| 任务进度 | 节流推送，只保留最新进度      |
| 看板数据 | 固定频率推送快照              |
| 在线人数 | 聚合后推送，不每次变化都广播  |
| 告警消息 | 聚合同类告警，避免告警风暴    |

性能压测建议：

| 建议           | 说明                           |
| -------------- | ------------------------------ |
| 先测空连接     | 得到连接容量基线               |
| 再测订阅       | 观察订阅关系和 Broker 压力     |
| 再测消息吞吐   | 观察入站和出站线程池           |
| 再测广播和群组 | 观察 fan-out 能力              |
| 最后做长稳     | 观察内存、GC、Session 泄漏     |
| 结论绑定配置   | 容量结论必须说明机器规格和参数 |

### 日志监控最佳实践

日志监控应覆盖连接、认证、订阅、发送、推送、异常、安全审计、慢日志和运行指标。日志用于定位单个问题，指标用于发现系统趋势，告警用于及时响应故障。

日志最佳实践：

| 日志类型 | 建议                               |
| -------- | ---------------------------------- |
| 连接日志 | CONNECT 和 DISCONNECT 记录 info    |
| 认证日志 | 成功 info，失败 warn，Token 脱敏   |
| 订阅日志 | 正常可采样，失败必须记录           |
| 发送日志 | 高频消息采样，关键消息必记         |
| 推送日志 | 关键消息记录，失败必须记录         |
| 异常日志 | 未知异常 error，可预期异常 warn    |
| 安全审计 | 越权、黑名单、限流、管理端操作必记 |
| 慢日志   | 超过阈值记录 warn                  |
| 心跳日志 | 不逐条打印                         |

日志字段建议统一包含：

| 字段          | 说明           |
| ------------- | -------------- |
| `traceId`     | 链路追踪       |
| `messageId`   | 消息 ID        |
| `userId`      | 用户 ID        |
| `sessionId`   | Session ID     |
| `destination` | STOMP 目标地址 |
| `clientType`  | 客户端类型     |
| `instanceId`  | 服务实例       |
| `costMillis`  | 处理耗时       |
| `result`      | 成功或失败     |
| `reason`      | 失败原因       |

监控指标最佳实践：

| 指标         | 说明                |
| ------------ | ------------------- |
| 当前连接数   | 判断连接规模        |
| 在线用户数   | 判断真实在线规模    |
| 入站消息量   | 客户端发送压力      |
| 出站消息量   | 服务端推送压力      |
| 推送失败率   | 可靠性核心指标      |
| P95/P99 延迟 | 用户体验指标        |
| 线程池队列   | 系统拥塞前兆        |
| Broker 积压  | Broker Relay 健康度 |
| Redis 延迟   | 在线状态和限流性能  |
| JVM GC       | 长连接稳定性        |
| 安全事件     | 风控和攻击识别      |

告警最佳实践：

| 告警项         | 建议                         |
| -------------- | ---------------------------- |
| 推送失败率     | 超过阈值立即告警             |
| 连接数异常下降 | 可能是网关或服务故障         |
| 出站队列堆积   | 可能是慢消费者或 Broker 异常 |
| 线程池拒绝任务 | 严重告警                     |
| Broker 不可用  | 严重告警                     |
| Redis 不可用   | 严重告警                     |
| Full GC 频繁   | 严重告警                     |
| 非法订阅激增   | 安全告警                     |
| 认证失败激增   | 安全告警                     |

### 前端接入最佳实践

前端接入应统一封装 STOMP 客户端，不要在每个页面重复创建连接。连接、订阅、发送、重连、Token 刷新、消息解析、去重和页面销毁都应由统一模块处理。

前端接入建议如下：

| 建议                      | 说明                                   |
| ------------------------- | -------------------------------------- |
| 使用 `@stomp/stompjs`     | 现代 STOMP 客户端                      |
| 统一 WebSocket 客户端实例 | 避免每个页面重复连接                   |
| Token 放 `connectHeaders` | 使用 STOMP CONNECT Header              |
| 配置自动重连              | 网络异常后自动恢复                     |
| Token 过期停止无限重连    | 先刷新 Token 或跳转登录                |
| 重连后恢复订阅            | 旧连接订阅会失效                       |
| 重连后调用补偿接口        | 恢复断线期间消息                       |
| 消息按 messageId 去重     | 防止重复推送和补偿重叠                 |
| 页面销毁取消页面级订阅    | 防止内存泄漏和重复消费                 |
| 全局订阅统一管理          | 用户通知、错误队列、会话控制           |
| 多标签页控制连接数        | 使用 BroadcastChannel 或服务端连接限制 |

推荐前端连接流程：

```text
1. 用户登录成功
2. 调用 /api/ws/init 获取初始化配置
3. 创建 STOMP Client
4. CONNECT Header 携带 Token、clientType、deviceId
5. 连接成功后注册全局订阅
6. 页面进入后注册页面级订阅
7. 收到消息后解析、去重、分发
8. 需要 ACK 的消息发送 ACK
9. 断线后自动重连
10. 重连成功后恢复订阅并补偿消息
```

前端订阅分类：

| 类型                             | 生命周期     |
| -------------------------------- | ------------ |
| `/user/queue/notice`             | 应用全局     |
| `/user/queue/errors`             | 应用全局     |
| `/user/queue/session`            | 应用全局     |
| `/topic/system/notice`           | 应用全局     |
| `/topic/group/{groupId}`         | 页面级       |
| `/topic/room/{roomId}/count`     | 页面级       |
| `/topic/admin/dashboard/message` | 管理端页面级 |

前端错误处理建议：

| 错误类型                | 处理                   |
| ----------------------- | ---------------------- |
| `WS_AUTH_TOKEN_EXPIRED` | 刷新 Token 或跳转登录  |
| `WS_ACCESS_DENIED`      | 提示无权限，停止订阅   |
| `WS_RATE_LIMITED`       | 提示操作过快           |
| `SESSION_KICKED`        | 主动断开并跳转登录     |
| `WS_INTERNAL_ERROR`     | 可提示稍后重试         |
| JSON 解析失败           | 丢弃当前消息并记录日志 |

### 生产环境配置最佳实践

生产环境配置必须以安全、稳定、可观测、可扩展为目标。不要直接使用开发环境配置上线，尤其不要使用 `*` 跨域、默认密码、无监控、无心跳、无 Redis TTL、无 Broker 高可用和无连接限制。

生产配置清单：

| 配置项      | 生产建议                                         |
| ----------- | ------------------------------------------------ |
| 连接协议    | 使用 WSS                                         |
| Origin      | 明确白名单，禁止 `*`                             |
| Token       | 短期 Access Token + 刷新机制                     |
| Endpoint    | 统一 `/ws/stomp`                                 |
| Broker      | 多实例使用 RabbitMQ/ActiveMQ Broker Relay        |
| Redis       | Sentinel 或 Cluster，高可用部署                  |
| 心跳        | 10 到 30 秒，压测后确定                          |
| Session TTL | 心跳间隔 3 到 5 倍以上                           |
| 线程池      | 入站、出站、业务、推送分离                       |
| 队列容量    | 有界队列                                         |
| 限流        | 连接、发送、订阅、广播全部开启                   |
| 黑名单      | IP 和用户黑名单开启                              |
| 审计        | 认证失败、越权、管理端操作必记                   |
| 日志        | 敏感信息脱敏                                     |
| 监控        | Actuator + Prometheus + Grafana                  |
| 告警        | 推送失败率、连接异常、线程池、Broker、Redis、JVM |
| 滚动升级    | 开启优雅停机和客户端重连                         |
| 补偿接口    | 关键消息支持断线补偿                             |

生产推荐配置示例：

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

app:
  websocket:
    enabled: true
    protocol-version: "1.0"
    instance-id: ${HOSTNAME}

    endpoint:
      path: /ws/stomp
      sockjs-enabled: true
      allowed-origin-patterns:
        - "https://admin.example.com"
        - "https://app.example.com"

    broker:
      type: rabbitmq
      relay-host: rabbitmq.prod.internal
      relay-port: 61613
      client-login: ${WS_BROKER_CLIENT_LOGIN}
      client-passcode: ${WS_BROKER_CLIENT_PASSCODE}
      system-login: ${WS_BROKER_SYSTEM_LOGIN}
      system-passcode: ${WS_BROKER_SYSTEM_PASSCODE}
      system-heartbeat-send-interval: 10000
      system-heartbeat-receive-interval: 10000

    heartbeat:
      server-to-client: 20000
      client-to-server: 20000
      session-ttl-seconds: 180
      timeout-scan-interval: 30000

    rate-limit:
      enabled: true
      ip-connect-per-minute: 100
      user-connect-per-minute: 20
      user-send-per-second: 5
      session-subscribe-per-minute: 100
      group-send-per-second: 100
      admin-broadcast-per-minute: 5
      max-message-bytes: 65536
      risk-score-threshold: 30

    security:
      token-auth-enabled: true
      origin-check-enabled: true
      subscribe-auth-enabled: true
      send-auth-enabled: true
      xss-check-enabled: true
      audit-log-enabled: true
      blacklist-enabled: true
```

生产上线前检查清单：

| 检查项                         | 是否必须     |
| ------------------------------ | ------------ |
| WSS 可用                       | 是           |
| Nginx/Gateway Upgrade 配置正确 | 是           |
| Origin 白名单正确              | 是           |
| Token CONNECT 鉴权正常         | 是           |
| 普通用户不能订阅管理端 Topic   | 是           |
| 非群成员不能订阅群组 Topic     | 是           |
| 用户点对点消息可达             | 是           |
| 集群广播可达所有实例用户       | 是           |
| Redis Session 有 TTL           | 是           |
| Broker Relay 连接正常          | 多实例必须   |
| 推送失败有记录                 | 是           |
| 离线补偿可用                   | 关键业务必须 |
| 限流生效                       | 是           |
| 线程池有界队列                 | 是           |
| Prometheus 指标可采集          | 是           |
| Grafana 看板可查看             | 是           |
| 告警规则已配置                 | 是           |
| 滚动升级验证通过               | 是           |
| 压测报告已完成                 | 是           |

生产环境的核心原则是：连接可控、消息可追踪、失败可恢复、状态可补偿、异常可观测、权限不可绕过。WebSocket 只负责实时触达，不能替代数据库最终状态和 HTTP 补偿查询。

## 项目落地清单

项目落地清单用于将 WebSocket STOMP 从技术方案转化为可实施、可验收、可上线、可运维的工程任务。清单应覆盖基础能力、安全能力、可靠性能力、集群能力、监控能力、运维能力、测试验收和上线检查，避免只完成“能连接、能推送”的最小功能后直接上线。

### 基础能力清单

基础能力是 WebSocket STOMP 项目能够正常运行的最低要求，主要包括连接、订阅、发送、广播、用户消息、群组消息、消息模型和前端接入。

| 能力项             | 是否必须 | 验收标准                                                     |
| ------------------ | -------- | ------------------------------------------------------------ |
| WebSocket Endpoint | 是       | 客户端可以连接 `/ws/stomp`                                   |
| STOMP CONNECT      | 是       | 连接成功后完成身份绑定                                       |
| 应用发送前缀       | 是       | 客户端可向 `/app/**` 发送消息                                |
| 广播订阅           | 是       | 客户端可订阅 `/topic/**`                                     |
| 用户队列订阅       | 是       | 客户端可订阅 `/user/queue/**`                                |
| 点对点推送         | 是       | 服务端可向指定用户推送                                       |
| 群组订阅           | 视业务   | 群成员可订阅 `/topic/group/{groupId}`                        |
| 统一消息体         | 是       | 消息包含 `messageId`、`type`、`version`、`timestamp`、`data` |
| 错误消息返回       | 是       | 异常统一返回 `/user/queue/errors`                            |
| 前端自动重连       | 是       | 网络断开后可自动恢复连接                                     |
| 页面订阅清理       | 是       | 页面销毁后取消页面级订阅                                     |
| HTTP 初始化接口    | 建议     | 前端可获取 Endpoint、订阅路径、未读数和服务时间              |

基础能力落地顺序建议：

```text
1. Maven 依赖和 WebSocket 配置
2. Endpoint、Broker、前缀配置
3. CONNECT 认证和 Principal 绑定
4. 统一消息模型
5. 广播推送
6. 用户点对点推送
7. 群组消息
8. 错误队列
9. 前端客户端封装
10. 初始化接口和补偿接口
```

基础能力完成后，应至少能够验证以下链路：

```text
连接成功 -> 订阅用户队列 -> 服务端向用户推送消息 -> 客户端收到消息
连接成功 -> 订阅广播主题 -> 服务端广播消息 -> 多客户端收到消息
连接成功 -> 客户端 SEND 到 /app/** -> 服务端处理并返回 ACK 或业务消息
```

### 安全能力清单

安全能力用于防止未认证连接、越权订阅、非法发送、恶意刷消息、跨站连接、敏感信息泄露和管理端数据暴露。

| 能力项             | 是否必须   | 验收标准                                  |
| ------------------ | ---------- | ----------------------------------------- |
| Origin 白名单      | 是         | 非法来源不能建立连接                      |
| CONNECT Token 认证 | 是         | 无 Token、过期 Token、非法 Token 均被拒绝 |
| Principal 绑定     | 是         | `Principal.getName()` 返回当前用户 ID     |
| SUBSCRIBE 鉴权     | 是         | 普通用户不能订阅管理端 Topic              |
| SEND 鉴权          | 是         | 普通用户不能发送管理端消息                |
| 群组权限校验       | 视业务     | 非群成员不能订阅或发送群消息              |
| 租户隔离           | 多租户必须 | 用户不能访问其他租户 Topic                |
| 消息内容校验       | 是         | 空消息、超长消息、非法枚举被拒绝          |
| 消息大小限制       | 是         | 超出大小限制的消息被拒绝                  |
| 发送频率限制       | 是         | 高频发送触发限流                          |
| 连接频率限制       | 是         | 高频建连触发限流                          |
| IP 黑名单          | 建议       | 命中黑名单的 IP 被拒绝                    |
| 用户黑名单         | 建议       | 封禁用户无法连接或发送                    |
| 敏感字段脱敏       | 是         | 日志不打印完整 Token、Cookie、密码        |
| XSS 内容处理       | 建议       | 高风险内容被拦截或转义                    |
| 安全审计日志       | 是         | 认证失败、越权、黑名单、限流均有记录      |

安全验收重点：

```text
1. 未登录用户不能 CONNECT
2. 普通用户不能订阅 /topic/admin/**
3. 非群成员不能订阅 /topic/group/{groupId}
4. 客户端不能 SEND 到 /topic/** 或 /user/**
5. 超大消息不能进入业务层
6. Token 过期后不能继续使用旧连接发送消息
7. 被封禁用户在线时会被强制下线
8. 所有安全拒绝都有审计日志
```

安全能力不要只依赖前端控制。前端隐藏按钮、隐藏菜单、隐藏路由都不能替代服务端鉴权。

### 可靠性能力清单

可靠性能力用于保证关键消息可追踪、可去重、可确认、可补偿、可重试、可排查。WebSocket 本身不保证完整可靠投递，因此业务层必须补齐可靠性设计。

| 能力项        | 是否必须     | 验收标准                               |
| ------------- | ------------ | -------------------------------------- |
| 消息唯一 ID   | 是           | 每条业务消息都有全局唯一 `messageId`   |
| 客户端消息 ID | 聊天建议     | 重复提交不会重复落库                   |
| 服务端幂等    | 是           | 相同业务事件不会重复生成通知           |
| 前端去重      | 是           | 相同 `messageId` 不重复展示            |
| 消息持久化    | 关键消息必须 | 聊天、通知、审批、订单状态可查询       |
| ACK 机制      | 关键消息建议 | 客户端收到后可回执                     |
| 回执记录      | 视业务       | 可查询已送达、已读、处理失败           |
| 离线消息      | 是           | 离线用户重连后可补偿                   |
| 补偿接口      | 是           | 断线后可按 sequence 或时间拉取缺失消息 |
| 失败消息记录  | 是           | 推送失败有失败表或失败日志             |
| 重试机制      | 建议         | 可重试失败消息自动重试                 |
| 死信处理      | 建议         | 超过重试上限进入死信                   |
| 事务后推送    | 是           | 数据提交成功后再推送                   |
| 消息过期策略  | 建议         | 临时消息过期后不再补偿                 |

可靠性验收重点：

```text
1. 数据库事务回滚时不会推送成功消息
2. 事务提交后推送失败会写失败消息表
3. 客户端重复发送同一 clientMessageId 不会产生重复聊天消息
4. MQ 重复投递同一业务事件不会重复生成通知
5. 用户离线期间产生的关键消息可在重连后查询
6. 前端实时消息和补偿消息重复时只展示一次
7. ACK 重复上报不会重复写回执
8. 失败消息超过重试次数后进入死信或人工处理队列
```

可靠性设计的最终原则是：数据库是最终状态来源，WebSocket 是实时触达通道。关键业务不能只依赖 WebSocket 推送结果。

### 集群能力清单

集群能力用于支持 WebSocket 服务多实例部署。多实例部署时，Session、订阅、在线状态、点对点消息和广播消息都不能只依赖本地内存。

| 能力项              | 是否必须       | 验收标准                             |
| ------------------- | -------------- | ------------------------------------ |
| 实例 ID             | 是             | 每个实例有唯一 `instanceId`          |
| Redis Session 映射  | 多实例必须     | 可查询用户连接在哪个实例             |
| 全局在线用户        | 多实例必须     | Redis 中可统计全局在线用户           |
| 用户多 Session 管理 | 是             | 同用户多端连接可正确维护             |
| 跨实例点对点推送    | 多实例必须     | 用户连接在其他实例也能收到消息       |
| 跨实例广播同步      | 多实例必须     | 所有实例上的订阅者都能收到广播       |
| 群组消息同步        | 群组业务必须   | 群成员分布在不同实例仍能收到群消息   |
| 分布式踢人          | 管理端建议     | 任意实例可踢出任意连接               |
| 分布式限流          | 建议           | 跨实例请求不能绕过限流               |
| 分布式锁            | 视业务         | 批量任务、失败重试不会多实例重复执行 |
| Session TTL         | 是             | 实例宕机后 Session 可自动过期        |
| 实例心跳            | 建议           | 可清理宕机实例残留 Session           |
| Broker Relay        | 生产多实例建议 | RabbitMQ/ActiveMQ 统一分发订阅消息   |

集群能力验收场景：

```text
1. 用户 A 连接实例 1，用户 B 连接实例 2
2. 实例 1 向用户 B 推送点对点消息，用户 B 能收到
3. 实例 2 发布系统公告，实例 1 和实例 2 上的客户端都能收到
4. 群成员分布在多个实例时，群消息全部送达
5. 管理端请求打到实例 1，可以踢出实例 2 上的连接
6. 实例 2 异常宕机后，其 Redis Session 最终被清理
7. 同一用户跨多个实例高频发送仍会触发限流
```

集群部署最容易遗漏的是“广播同步”和“点对点路由”。如果使用 Simple Broker 且没有 Redis Pub/Sub 或 Broker Relay，多实例下必然出现部分用户收不到消息的问题。

### 监控能力清单

监控能力用于持续观察连接规模、在线用户、消息吞吐、推送失败、延迟、线程池、Broker、Redis、JVM 和安全事件。没有监控的 WebSocket 系统很难稳定运行。

| 能力项       | 是否必须        | 验收标准                           |
| ------------ | --------------- | ---------------------------------- |
| 当前连接数   | 是              | 可查看当前 WebSocket Session 数    |
| 在线用户数   | 是              | 可查看全局在线用户数               |
| 连接成功数   | 建议            | 可统计连接趋势                     |
| 断开原因统计 | 建议            | 可区分正常断开、超时、踢下线       |
| 入站消息量   | 是              | 可统计客户端发送量                 |
| 出站消息量   | 是              | 可统计服务端推送量                 |
| 推送失败率   | 是              | 可计算失败比例                     |
| 消息延迟     | 是              | 可查看 P95、P99                    |
| 线程池指标   | 是              | 可查看活跃线程、队列长度、拒绝次数 |
| Redis 指标   | 是              | 可查看延迟、连接数、内存、慢查询   |
| Broker 指标  | Broker 模式必须 | 可查看连接、积压、心跳、重连       |
| JVM 指标     | 是              | 可查看内存、GC、线程、CPU          |
| 安全事件指标 | 建议            | 可统计认证失败、非法订阅、限流     |
| Prometheus   | 建议            | 指标可被采集                       |
| Grafana 看板 | 建议            | 可视化系统状态                     |
| 告警规则     | 是              | 故障可主动通知                     |

核心告警项：

| 告警项         | 建议阈值                |
| -------------- | ----------------------- |
| 推送失败率     | 5 分钟超过 5% 告警      |
| 连接数异常下降 | 5 分钟下降超过 50% 告警 |
| 出站队列堆积   | 队列使用率超过 80% 告警 |
| 线程池拒绝任务 | 大于 0 严重告警         |
| Broker 不可用  | 持续 1 分钟严重告警     |
| Redis 不可用   | 持续 1 分钟严重告警     |
| P95 推送延迟   | 超过业务阈值告警        |
| Full GC 频繁   | 持续出现严重告警        |
| 非法订阅激增   | 安全告警                |
| 认证失败激增   | 安全告警                |

监控能力验收时，必须通过模拟异常验证告警是否真的触发，而不是只确认配置文件存在。

### 运维能力清单

运维能力用于支持管理端查看连接、在线用户、会话、推送记录、异常连接、失败消息、死信消息，并支持强制下线、推送测试、广播公告等操作。

| 能力项          | 是否必须        | 验收标准                                         |
| --------------- | --------------- | ------------------------------------------------ |
| 在线连接查看    | 是              | 可查看 sessionId、userId、instanceId、clientType |
| 在线用户查看    | 是              | 可按用户维度查看在线状态                         |
| 用户会话查看    | 是              | 可查看用户全部在线连接                           |
| 强制下线        | 建议            | 可踢出指定 Session 或用户全部连接                |
| 消息推送测试    | 建议            | 管理端可向用户或 Topic 推送测试消息              |
| 广播公告        | 视业务          | 可发布系统公告                                   |
| 群组状态查看    | 群组业务建议    | 可查看群成员、在线人数、禁言状态                 |
| 消息发送记录    | 关键消息必须    | 可按 messageId、bizId 查询                       |
| 失败消息查看    | 是              | 可查看失败原因、重试次数                         |
| 死信消息处理    | 建议            | 可人工处理或重投                                 |
| 异常连接记录    | 是              | 可查询认证失败、非法订阅、限流                   |
| 安全审计查询    | 是              | 可查询管理端操作和高风险事件                     |
| 线程池状态查看  | 建议            | 可查看队列和活跃线程                             |
| Broker 状态查看 | Broker 模式建议 | 可查看 Broker 连接状态                           |
| Redis 状态查看  | 建议            | 可查看在线状态和 Session 残留                    |

运维能力验收重点：

```text
1. 管理端可以定位某个用户当前连接在哪个实例
2. 管理端可以踢出某个 Session
3. 被踢客户端能收到 SESSION_KICKED 消息并主动断开
4. 管理端可以查询某条消息是否发送、是否失败、是否 ACK
5. 管理端可以看到异常连接和非法订阅记录
6. 推送失败后可以在失败消息列表看到记录
7. 死信消息可以人工标记已处理或重新投递
```

所有管理端操作都必须写操作日志，尤其是踢人、封禁、禁言、广播公告、推送测试、死信重投。

### 测试验收清单

测试验收清单用于上线前验证功能、安全、可靠性、集群、性能和异常处理是否符合预期。

| 测试项         | 是否必须     | 验收标准                             |
| -------------- | ------------ | ------------------------------------ |
| 单元测试       | 是           | 工具类、权限、幂等、限流测试通过     |
| 集成测试       | 是           | WebSocket 配置、拦截器、消息处理通过 |
| 连接测试       | 是           | 合法连接成功，非法连接失败           |
| STOMP 消息测试 | 是           | SEND、SUBSCRIBE、推送链路正常        |
| 认证测试       | 是           | Token 缺失、过期、非法均拒绝         |
| 订阅权限测试   | 是           | 越权订阅被拒绝                       |
| 发送权限测试   | 是           | 越权发送被拒绝                       |
| 消息可靠性测试 | 是           | 幂等、ACK、补偿、重试通过            |
| 离线消息测试   | 关键业务必须 | 离线后重连可补偿                     |
| 集群消息测试   | 多实例必须   | 跨实例点对点、广播、群组通过         |
| 限流测试       | 是           | 连接、发送、订阅、广播限流生效       |
| 压力测试       | 生产必须     | 达到目标连接数和消息吞吐             |
| 稳定性测试     | 生产必须     | 长时间运行无泄漏、无异常堆积         |
| 滚动升级测试   | 生产必须     | 发布期间客户端可重连恢复             |

推荐验收用例：

```text
1. 普通用户连接成功
2. 普通用户订阅 /topic/admin/online 被拒绝
3. 群成员订阅 /topic/group/{groupId} 成功
4. 非群成员订阅 /topic/group/{groupId} 被拒绝
5. 用户 A 给用户 B 发送单聊消息，B 收到
6. 用户 B 离线时产生消息，重连后可补偿
7. 重复 clientMessageId 发送不会重复落库
8. 事务回滚不会推送消息
9. Broker 暂时不可用时失败消息被记录
10. 多实例下广播所有节点客户端都能收到
11. 管理端踢人后客户端断开
12. 高频发送触发限流
13. 超大消息被拒绝
14. Token 过期后连接被拒绝
15. 滚动升级期间客户端能自动重连并恢复订阅
```

### 上线检查清单

上线检查清单用于生产发布前最终确认配置、安全、依赖、监控、压测和回滚方案是否准备完整。

| 检查项                     | 是否必须   | 结果                                              |
| -------------------------- | ---------- | ------------------------------------------------- |
| WSS 可用                   | 是         | 生产连接使用 `wss://`                             |
| Nginx/Gateway Upgrade 配置 | 是         | 返回 `101 Switching Protocols`                    |
| Origin 白名单              | 是         | 只允许正式域名                                    |
| Token CONNECT 认证         | 是         | 无效 Token 被拒绝                                 |
| SUBSCRIBE 鉴权             | 是         | 敏感 Topic 不可越权订阅                           |
| SEND 鉴权                  | 是         | 非法发送被拒绝                                    |
| Redis 高可用               | 是         | Sentinel 或 Cluster 可用                          |
| Redis Session TTL          | 是         | Session Key 有过期时间                            |
| Broker Relay               | 多实例必须 | RabbitMQ/ActiveMQ 连接正常                        |
| Broker 高可用              | 生产建议   | Broker 集群或主备可用                             |
| 数据库表结构               | 是         | 消息、通知、回执、日志、失败表已建                |
| 索引检查                   | 是         | `messageId`、`receiverId`、`createdAt` 等索引存在 |
| 线程池有界队列             | 是         | 不存在无界队列                                    |
| 限流配置                   | 是         | 连接、发送、订阅、广播限流开启                    |
| 黑名单能力                 | 建议       | IP 和用户黑名单可用                               |
| 日志脱敏                   | 是         | Token、Cookie、手机号脱敏                         |
| Prometheus 指标            | 是         | `/actuator/prometheus` 可采集                     |
| Grafana 看板               | 建议       | 连接、消息、失败、延迟可视化                      |
| 告警规则                   | 是         | Broker、Redis、失败率、线程池告警可触发           |
| 压测报告                   | 生产必须   | 有容量结论和配置基线                              |
| 滚动升级验证               | 是         | 客户端可自动重连                                  |
| 回滚方案                   | 是         | 镜像、配置、数据库兼容可回滚                      |
| 前端补偿逻辑               | 是         | 重连后恢复订阅并补偿消息                          |
| 管理端运维功能             | 建议       | 可查看连接、踢人、查失败消息                      |

上线前最终确认：

```text
1. 生产配置与测试配置已区分
2. 生产 Origin 未配置 *
3. Broker、Redis、数据库密码未写死在代码仓库
4. WebSocket Endpoint 已通过网关连通性测试
5. 多实例跨节点推送已验证
6. 关键消息已支持持久化和补偿
7. 监控和告警已经接入
8. 压测结果满足业务目标
9. 发布失败时可以快速回滚
10. 运维团队知道故障排查入口
```

## 扩展能力

扩展能力用于说明 WebSocket STOMP 在基础聊天、通知、看板之外的更多实时业务场景。扩展能力落地时，应基于前文的认证、订阅鉴权、消息模型、可靠性、集群、限流和监控体系复用，而不是为每个场景重新设计一套实时通道。

### WebRTC 信令服务

WebRTC 信令服务用于在浏览器之间建立音视频或实时通信连接时交换 SDP、ICE Candidate、呼叫状态、挂断状态等信令。WebSocket STOMP 不传输音视频媒体流，只负责信令交换；真正的音视频数据通过 WebRTC 点对点或 SFU/MCU 传输。

典型信令类型：

| 类型                   | 说明          |
| ---------------------- | ------------- |
| `WEBRTC_CALL_INVITE`   | 发起呼叫      |
| `WEBRTC_CALL_ACCEPT`   | 接受呼叫      |
| `WEBRTC_CALL_REJECT`   | 拒绝呼叫      |
| `WEBRTC_CALL_CANCEL`   | 取消呼叫      |
| `WEBRTC_CALL_HANGUP`   | 挂断          |
| `WEBRTC_SDP_OFFER`     | SDP Offer     |
| `WEBRTC_SDP_ANSWER`    | SDP Answer    |
| `WEBRTC_ICE_CANDIDATE` | ICE Candidate |

推荐路由：

| 场景         | 路由                          |
| ------------ | ----------------------------- |
| 发起信令     | `/app/webrtc/signal/send`     |
| 用户接收信令 | `/user/queue/webrtc`          |
| 房间信令     | `/topic/room/{roomId}/webrtc` |

信令消息示例：

```json
{
  "messageId": "1909550919275922601",
  "type": "WEBRTC_SDP_OFFER",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "callId": "call-100001",
    "fromUserId": "10001",
    "toUserId": "10002",
    "sdp": "v=0...",
    "mediaType": "VIDEO"
  }
}
```

WebRTC 信令设计建议：

| 建议               | 说明                             |
| ------------------ | -------------------------------- |
| WebSocket 只传信令 | 不传音视频媒体数据               |
| callId 全程唯一    | 用于关联一次通话                 |
| 信令要有时效       | 过期呼叫不再处理                 |
| 用户在线校验       | 对方离线时返回不可达             |
| 权限校验           | 只有房间成员或会话双方可交换信令 |
| 限流               | 防止频繁呼叫骚扰                 |
| 日志脱敏           | SDP 可较大，不建议完整打印       |

### 实时协同编辑

实时协同编辑用于文档、表格、白板、代码、设计稿等多人同时编辑场景。WebSocket 可以推送操作事件、光标位置、用户在线状态和版本变更，但需要额外设计冲突处理算法。

常见协同模型：

| 模型         | 说明                                               |
| ------------ | -------------------------------------------------- |
| OT           | Operational Transformation，传统协同编辑算法       |
| CRDT         | Conflict-free Replicated Data Type，适合分布式协同 |
| 锁定编辑     | 简单场景下按区域或字段加锁                         |
| 服务端顺序化 | 所有操作进入服务端排序后广播                       |

推荐路由：

| 场景         | 路由                                           |
| ------------ | ---------------------------------------------- |
| 发送编辑操作 | `/app/collab/document/edit`                    |
| 订阅文档操作 | `/topic/collab/document/{documentId}`          |
| 光标位置     | `/topic/collab/document/{documentId}/cursor`   |
| 在线成员     | `/topic/collab/document/{documentId}/presence` |

协同编辑消息示例：

```json
{
  "messageId": "1909550919275922602",
  "type": "COLLAB_OPERATION",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "documentId": "doc-100001",
    "operationId": "op-100001",
    "userId": "10001",
    "baseVersion": 32,
    "operationType": "INSERT_TEXT",
    "position": 120,
    "content": "新增文本"
  }
}
```

协同编辑设计建议：

| 建议               | 说明                          |
| ------------------ | ----------------------------- |
| 操作必须有版本号   | 防止乱序和冲突                |
| 服务端统一排序     | 不信任客户端操作顺序          |
| 快照定期保存       | 避免从头回放全部操作          |
| 光标状态可不持久化 | 属于临时状态                  |
| 编辑操作要鉴权     | 只有文档协作者可订阅和发送    |
| 断线后拉取快照     | 不建议通过 WebSocket 全量补偿 |
| 高频操作要合并     | 光标、输入中状态需要节流      |

### 实时地图位置推送

实时地图位置推送用于车辆定位、人员定位、配送轨迹、巡检轨迹、设备位置、订单配送等场景。位置消息通常频率较高，必须做节流、压缩、聚合和权限控制。

推荐路由：

| 场景         | 路由                                  |
| ------------ | ------------------------------------- |
| 设备上报位置 | `/app/location/report`                |
| 订阅订单位置 | `/topic/location/order/{orderId}`     |
| 订阅车辆位置 | `/topic/location/vehicle/{vehicleId}` |
| 订阅区域位置 | `/topic/location/area/{areaId}`       |

位置消息示例：

```json
{
  "messageId": "1909550919275922603",
  "type": "LOCATION_UPDATE",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "targetId": "vehicle-100001",
    "targetType": "VEHICLE",
    "longitude": 116.397128,
    "latitude": 39.916527,
    "speed": 42.5,
    "direction": 90,
    "reportedAt": 1777956000000
  }
}
```

实时位置设计建议：

| 建议                 | 说明                               |
| -------------------- | ---------------------------------- |
| 高频位置要节流       | 例如 1 到 5 秒推送一次             |
| 地图只需要最新位置   | 可丢弃旧位置快照                   |
| 轨迹查询走 HTTP      | 历史轨迹不通过 WebSocket 全量推送  |
| 订阅必须鉴权         | 用户只能看有权限的车辆、订单或区域 |
| 位置数据属于敏感数据 | 日志和管理端展示要控制权限         |
| 弱网下允许延迟       | 前端根据时间戳处理旧位置           |
| 大屏场景做聚合       | 大量点位不应逐点高频推送           |

### 实时大屏数据推送

实时大屏数据推送用于运营大屏、监控大屏、业务指标大屏、告警大屏、生产看板等场景。大屏通常只关心最新聚合数据，不关心每一条原始事件。

推荐路由：

| 场景         | 路由                             |
| ------------ | -------------------------------- |
| 订阅业务大屏 | `/topic/dashboard/business`      |
| 订阅运维大屏 | `/topic/dashboard/ops`           |
| 订阅告警大屏 | `/topic/dashboard/alarm`         |
| 管理端看板   | `/topic/admin/dashboard/message` |

大屏消息示例：

```json
{
  "messageId": "1909550919275922604",
  "type": "DASHBOARD_METRIC",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "onlineUserCount": 1280,
    "todayOrderCount": 35620,
    "todayPayAmount": 9823450.25,
    "alarmCount": 12,
    "refreshInterval": 5000
  }
}
```

实时大屏设计建议：

| 建议                | 说明                          |
| ------------------- | ----------------------------- |
| 推送聚合结果        | 不推送大量明细                |
| 固定频率刷新        | 3 到 10 秒一次即可            |
| 支持 HTTP 全量刷新  | WebSocket 异常时可手动刷新    |
| 指标计算异步化      | 不在推送线程中做重计算        |
| 大屏 Topic 严格鉴权 | 业务指标可能敏感              |
| 数据可覆盖          | 旧快照可丢弃，只展示最新      |
| 异常状态可视化      | Redis、Broker、任务失败要高亮 |

### 实时订单状态推送

实时订单状态推送用于电商、物流、支付、配送、售后等订单状态变化场景。订单状态属于关键业务状态，必须先以数据库为准，WebSocket 只做实时通知。

推荐路由：

| 场景           | 路由                            |
| -------------- | ------------------------------- |
| 用户订单通知   | `/user/queue/order`             |
| 商家订单通知   | `/user/queue/merchant/order`    |
| 管理端订单监控 | `/topic/admin/order/status`     |
| 订单详情页状态 | `/topic/order/{orderId}/status` |

订单状态消息示例：

```json
{
  "messageId": "1909550919275922605",
  "type": "ORDER_STATUS_CHANGED",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "orderId": "order-100001",
    "oldStatus": "WAIT_PAY",
    "newStatus": "PAID",
    "title": "订单已支付",
    "url": "/orders/order-100001"
  }
}
```

订单状态推送建议：

| 建议                           | 说明                                    |
| ------------------------------ | --------------------------------------- |
| 订单状态先落库                 | 推送不能早于事务提交                    |
| 使用业务幂等键                 | `ORDER + orderId + status + receiverId` |
| 用户只接收自己的订单           | 订阅和推送都要鉴权                      |
| 订单详情页可订阅订单 Topic     | 但必须校验订单访问权限                  |
| 离线用户通过通知中心补偿       | 不在线时仍可查看状态                    |
| 支付、退款等关键事件必须持久化 | 不能只推送                              |
| 前端刷新以 HTTP 为准           | 收到推送后可重新查询订单详情            |

### 实时审批流通知

实时审批流通知用于 OA、工作流、流程中心、待办中心等业务。审批通知通常涉及待办、已办、抄送、催办、撤回、转交、审批完成等状态。

推荐路由：

| 场景           | 路由                                         |
| -------------- | -------------------------------------------- |
| 用户审批待办   | `/user/queue/approval`                       |
| 用户通知中心   | `/user/queue/notice`                         |
| 管理端流程监控 | `/topic/admin/approval`                      |
| 流程实例状态   | `/topic/approval/{processInstanceId}/status` |

审批消息类型：

| 类型                     | 说明     |
| ------------------------ | -------- |
| `APPROVAL_TODO_CREATED`  | 新待办   |
| `APPROVAL_TODO_CANCELED` | 待办取消 |
| `APPROVAL_DONE`          | 审批完成 |
| `APPROVAL_REJECTED`      | 审批驳回 |
| `APPROVAL_TRANSFERRED`   | 审批转交 |
| `APPROVAL_URGE`          | 催办通知 |
| `APPROVAL_CC`            | 抄送通知 |

审批通知示例：

```json
{
  "messageId": "1909550919275922606",
  "type": "APPROVAL_TODO_CREATED",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "processInstanceId": "process-100001",
    "taskId": "task-100001",
    "title": "你有一条新的审批待办",
    "initiatorId": "10002",
    "url": "/approval/todo/task-100001"
  }
}
```

审批流通知建议：

| 建议                   | 说明                     |
| ---------------------- | ------------------------ |
| 待办必须落库           | 通知丢失不能导致待办消失 |
| 推送事务后执行         | 流程任务生成成功后再推送 |
| 审批状态以流程引擎为准 | WebSocket 只是提醒       |
| 用户只能接收自己的待办 | 严格校验接收人           |
| 支持未读数             | 待办数量实时更新         |
| 支持撤销通知           | 流程撤回后推送取消事件   |
| 支持离线补偿           | 用户上线后查询未处理待办 |

### 实时设备状态上报

实时设备状态上报用于设备管理、监控平台、工业系统、IoT 网关、边缘设备等场景。设备可以通过 WebSocket、MQTT、HTTP 或 TCP 上报状态，WebSocket STOMP 可用于浏览器端实时展示设备状态。

推荐路由：

| 场景             | 路由                                       |
| ---------------- | ------------------------------------------ |
| 设备状态上报     | `/app/device/status/report`                |
| 设备告警上报     | `/app/device/alarm/report`                 |
| 订阅设备状态     | `/topic/device/{deviceId}/status`          |
| 订阅产品设备状态 | `/topic/device/product/{productId}/status` |
| 管理端设备监控   | `/topic/admin/device/status`               |

设备状态消息示例：

```json
{
  "messageId": "1909550919275922607",
  "type": "DEVICE_STATUS_CHANGED",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "deviceId": "device-100001",
    "productId": "product-001",
    "onlineStatus": "ONLINE",
    "battery": 86,
    "signal": 72,
    "reportedAt": 1777956000000
  }
}
```

设备状态设计建议：

| 建议                  | 说明                                    |
| --------------------- | --------------------------------------- |
| 设备认证独立设计      | 设备 Token、证书或密钥与用户 Token 区分 |
| 高频状态要聚合        | 不要每秒推送所有字段                    |
| 最新状态可覆盖        | 状态类消息通常只关心最新                |
| 告警必须持久化        | 设备告警需要可追踪                      |
| 管理端按权限订阅      | 只能看授权设备                          |
| 离线状态要延迟判断    | 避免网络抖动导致频繁上下线              |
| IoT 场景优先考虑 MQTT | WebSocket 更适合管理端展示              |

### 物联网消息推送

物联网消息推送用于将设备事件、遥测数据、告警、指令结果推送到管理端、用户端或第三方系统。IoT 场景通常设备数量大、消息频率高、数据类型多，需要更强的消息队列和流处理能力。

推荐架构：

```text
设备 / 网关
  |
MQTT / TCP / HTTP
  |
IoT 接入服务
  |
Kafka / RabbitMQ / Redis Stream
  |
WebSocket 推送服务
  |
管理端 / 用户端
```

推荐消息类型：

| 类型                 | 说明         |
| -------------------- | ------------ |
| `IOT_TELEMETRY`      | 遥测数据     |
| `IOT_EVENT`          | 设备事件     |
| `IOT_ALARM`          | 设备告警     |
| `IOT_COMMAND_RESULT` | 指令执行结果 |
| `IOT_DEVICE_ONLINE`  | 设备上线     |
| `IOT_DEVICE_OFFLINE` | 设备离线     |

遥测消息示例：

```json
{
  "messageId": "1909550919275922608",
  "type": "IOT_TELEMETRY",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "deviceId": "device-100001",
    "metrics": {
      "temperature": 26.5,
      "humidity": 60.2,
      "voltage": 220.1
    },
    "reportedAt": 1777956000000
  }
}
```

物联网推送建议：

| 建议                             | 说明                       |
| -------------------------------- | -------------------------- |
| 设备接入与 WebSocket 展示解耦    | 设备不一定直接连 WebSocket |
| 高频遥测数据不要全部推前端       | 聚合、采样、降频           |
| 告警和指令结果必须可靠           | 需要持久化和 ACK           |
| 大规模事件使用 Kafka             | 便于削峰、回放、分析       |
| 管理端订阅按产品、设备、区域授权 | 避免越权查看               |
| 大屏展示推聚合指标               | 不推全部设备明细           |
| 历史数据走时序数据库查询         | WebSocket 只推实时变化     |

### 游戏房间消息

游戏房间消息用于轻量实时游戏、房间聊天、准备状态、匹配状态、位置同步、操作指令、倒计时等场景。WebSocket STOMP 可用于轻量房间类业务，但对低延迟、高帧率、强实时竞技类游戏并不一定适合。

推荐路由：

| 场景         | 路由                              |
| ------------ | --------------------------------- |
| 加入房间     | `/app/game/room/join`             |
| 离开房间     | `/app/game/room/leave`            |
| 发送房间消息 | `/app/game/room/message`          |
| 发送操作指令 | `/app/game/room/action`           |
| 订阅房间事件 | `/topic/game/room/{roomId}`       |
| 订阅房间状态 | `/topic/game/room/{roomId}/state` |

游戏房间消息示例：

```json
{
  "messageId": "1909550919275922609",
  "type": "GAME_ROOM_ACTION",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "roomId": "room-100001",
    "userId": "10001",
    "actionType": "READY",
    "sequence": 120,
    "payload": {}
  }
}
```

游戏房间设计建议：

| 建议                  | 说明                           |
| --------------------- | ------------------------------ |
| 房间成员必须鉴权      | 非房间成员不能订阅房间 Topic   |
| 操作事件要有 sequence | 保证前端按序处理               |
| 高频状态要节流        | 位置或状态同步不宜过高频       |
| 关键状态服务端判定    | 不信任客户端胜负、分数、道具   |
| 房间状态可快照化      | 重连后直接拉取最新状态         |
| 轻量游戏可用 STOMP    | 高频竞技类游戏建议专用实时协议 |
| 防刷和风控必须开启    | 防止脚本快速发送操作           |

### AI 流式响应推送

AI 流式响应推送用于将大模型生成内容实时推送到前端，例如聊天机器人、智能问答、报告生成、代码生成、摘要生成等。WebSocket 可以将模型输出的 token、段落、状态和最终结果逐步推送给客户端。

推荐路由：

| 场景             | 路由                        |
| ---------------- | --------------------------- |
| 发起 AI 请求     | `/app/ai/chat/send`         |
| 用户接收流式响应 | `/user/queue/ai/stream`     |
| 任务进度         | `/user/queue/task/progress` |
| 取消生成         | `/app/ai/chat/cancel`       |

AI 流式消息类型：

| 类型                  | 说明         |
| --------------------- | ------------ |
| `AI_STREAM_START`     | 流式响应开始 |
| `AI_STREAM_DELTA`     | 增量文本     |
| `AI_STREAM_DONE`      | 响应完成     |
| `AI_STREAM_ERROR`     | 响应失败     |
| `AI_STREAM_CANCELLED` | 用户取消     |
| `AI_TASK_PROGRESS`    | 任务进度     |

流式响应示例：

```json
{
  "messageId": "1909550919275922610",
  "type": "AI_STREAM_DELTA",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "conversationId": "conv-100001",
    "requestId": "req-100001",
    "sequence": 18,
    "delta": "这是模型生成的一段增量文本",
    "finished": false
  }
}
```

完成消息示例：

```json
{
  "messageId": "1909550919275922611",
  "type": "AI_STREAM_DONE",
  "status": "SUCCESS",
  "version": "1.0",
  "timestamp": 1777956000000,
  "data": {
    "conversationId": "conv-100001",
    "requestId": "req-100001",
    "totalTokens": 1024,
    "finished": true
  }
}
```

AI 流式推送建议：

| 建议                  | 说明                                  |
| --------------------- | ------------------------------------- |
| 增量消息要有 sequence | 防止乱序展示                          |
| 支持取消生成          | 用户关闭页面或点击停止时取消后端任务  |
| 控制推送频率          | 不一定每个 token 都推送，可合并小片段 |
| 最终结果必须持久化    | 页面刷新后可查询完整回答              |
| 失败消息要明确        | 返回错误码和可读提示                  |
| 用户隔离              | 只向当前请求用户推送                  |
| 限流和配额            | 防止用户频繁发起大模型请求            |
| 敏感内容处理          | 输出前可做安全检查或后处理            |
| 断线后补偿            | 重连后查询任务状态和已生成内容        |

AI 流式响应推荐流程：

```text
1. 用户通过 /app/ai/chat/send 发起请求
2. 服务端创建 requestId 和任务记录
3. AI 服务开始生成
4. 服务端分批推送 AI_STREAM_DELTA
5. 前端按 sequence 追加文本
6. 用户可通过 /app/ai/chat/cancel 取消
7. 生成完成后推送 AI_STREAM_DONE
8. 最终回答落库，页面刷新后通过 HTTP 查询
```

AI 流式响应不建议依赖单条超长 WebSocket 消息。应使用小片段增量推送、最终结果持久化、断线后 HTTP 查询补偿的组合方案。

## 附录

附录用于集中整理 STOMP 命令、Header、WebSocket 状态码、常用配置、异常说明、依赖版本、目录结构和消息路由规范，方便开发、联调、排查和上线检查时快速查阅。Spring 官方文档说明，WebSocket 本身只定义文本和二进制消息，消息内容语义需要由双方约定；STOMP 正是运行在 WebSocket 之上的子协议，用于定义消息命令、Header、destination、订阅和发送语义。([Home](https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html?utm_source=chatgpt.com))

### STOMP 常用命令

STOMP 是基于帧的文本协议，常见帧结构由命令、Header、空行和 Body 组成。Spring 文档中给出的 STOMP 帧结构为 `COMMAND`、若干 Header、空行、Body 和结束符，客户端可使用 `SEND` 或 `SUBSCRIBE` 携带 `destination` Header 发送或订阅消息。([NPM](https://www.npmjs.com/package/@stomp/stompjs?activeTab=versions&utm_source=chatgpt.com))

常用命令如下：

| 命令          | 方向           | 说明                                                 |
| ------------- | -------------- | ---------------------------------------------------- |
| `CONNECT`     | 客户端到服务端 | 建立 STOMP 会话，通常携带 Token、客户端类型、设备 ID |
| `CONNECTED`   | 服务端到客户端 | 服务端确认 STOMP 连接成功                            |
| `SEND`        | 客户端到服务端 | 客户端向服务端发送消息                               |
| `SUBSCRIBE`   | 客户端到服务端 | 客户端订阅某个 destination                           |
| `UNSUBSCRIBE` | 客户端到服务端 | 客户端取消订阅                                       |
| `MESSAGE`     | 服务端到客户端 | 服务端向订阅者推送消息                               |
| `ACK`         | 客户端到服务端 | 客户端确认消息已处理                                 |
| `NACK`        | 客户端到服务端 | 客户端确认消息处理失败                               |
| `BEGIN`       | 客户端到服务端 | 开启 STOMP 事务                                      |
| `COMMIT`      | 客户端到服务端 | 提交 STOMP 事务                                      |
| `ABORT`       | 客户端到服务端 | 回滚 STOMP 事务                                      |
| `DISCONNECT`  | 客户端到服务端 | 客户端主动断开 STOMP 会话                            |
| `RECEIPT`     | 服务端到客户端 | 服务端确认客户端请求已收到或处理                     |
| `ERROR`       | 服务端到客户端 | 服务端返回协议级或处理异常                           |

CONNECT 示例：

```text
CONNECT
accept-version:1.2
heart-beat:10000,10000
Authorization:Bearer xxx
clientType:WEB
deviceId:browser-001

^@
```

SUBSCRIBE 示例：

```text
SUBSCRIBE
id:sub-notice-001
destination:/user/queue/notice
ack:auto

^@
```

SEND 示例：

```text
SEND
destination:/app/chat/private/send
content-type:application/json
receipt:receipt-client-msg-001

{"receiverId":"10002","content":"你好","contentType":"TEXT","clientMessageId":"client-msg-001"}^@
```

MESSAGE 示例：

```text
MESSAGE
subscription:sub-notice-001
message-id:1909550919275922701
destination:/user/queue/notice
content-type:application/json

{"messageId":"1909550919275922701","type":"USER_NOTICE","status":"SUCCESS","version":"1.0","timestamp":1777956000000,"data":{"title":"通知"}}^@
```

命令使用建议如下：

| 建议                         | 说明                                           |
| ---------------------------- | ---------------------------------------------- |
| `CONNECT` 只做认证和会话建立 | 不在 CONNECT 中传复杂业务数据                  |
| `SEND` 只允许发往 `/app/**`  | 禁止客户端直接 SEND 到 `/topic/**`、`/user/**` |
| `SUBSCRIBE` 必须做鉴权       | 管理端、群组、租户 Topic 都需要校验            |
| 关键请求可使用 `receipt`     | 用于确认服务端收到请求                         |
| 业务 ACK 与 STOMP ACK 区分   | STOMP ACK 是协议确认，业务 ACK 是应用确认      |
| 断开前可发送 `DISCONNECT`    | 便于服务端清理 Session                         |

### STOMP Header 说明

STOMP Header 用于传递命令元数据，例如目标地址、订阅 ID、消息 ID、ACK 模式、事务 ID、Receipt、内容类型、心跳、认证信息等。Header 应保持简短，不适合传递大对象或复杂业务数据。

常用 Header 如下：

| Header           | 常见命令                          | 说明                                                 |
| ---------------- | --------------------------------- | ---------------------------------------------------- |
| `accept-version` | `CONNECT`                         | 客户端支持的 STOMP 协议版本                          |
| `heart-beat`     | `CONNECT`、`CONNECTED`            | 心跳协商配置                                         |
| `host`           | `CONNECT`                         | 虚拟主机，Broker Relay 场景可用                      |
| `login`          | `CONNECT`                         | Broker 登录账号，浏览器业务通常不用                  |
| `passcode`       | `CONNECT`                         | Broker 登录密码，浏览器业务通常不用                  |
| `Authorization`  | `CONNECT`                         | 应用层 Token，推荐使用 `Bearer xxx`                  |
| `clientType`     | `CONNECT`                         | 客户端类型，例如 `WEB`、`APP`、`ADMIN`               |
| `deviceId`       | `CONNECT`                         | 设备 ID                                              |
| `clientVersion`  | `CONNECT`                         | 客户端版本                                           |
| `destination`    | `SEND`、`SUBSCRIBE`、`MESSAGE`    | 消息目标地址                                         |
| `id`             | `SUBSCRIBE`、`UNSUBSCRIBE`        | 订阅 ID                                              |
| `subscription`   | `MESSAGE`                         | 服务端返回的订阅 ID                                  |
| `message-id`     | `MESSAGE`                         | STOMP 消息 ID                                        |
| `ack`            | `SUBSCRIBE`                       | ACK 模式，例如 `auto`、`client`、`client-individual` |
| `transaction`    | `SEND`、`ACK`、`NACK`             | 事务 ID                                              |
| `receipt`        | `SEND`、`SUBSCRIBE`、`DISCONNECT` | 请求服务端返回 RECEIPT                               |
| `receipt-id`     | `RECEIPT`                         | 与请求中的 receipt 对应                              |
| `content-type`   | `SEND`、`MESSAGE`                 | 消息体类型，常用 `application/json`                  |
| `content-length` | `SEND`、`MESSAGE`                 | 消息体长度                                           |
| `version`        | `CONNECTED`                       | 服务端协商后的 STOMP 版本                            |

项目自定义 Header 建议如下：

| Header          | 说明           | 是否建议                |
| --------------- | -------------- | ----------------------- |
| `Authorization` | 用户认证 Token | 是                      |
| `clientType`    | 客户端类型     | 是                      |
| `deviceId`      | 设备 ID        | 是                      |
| `clientVersion` | 客户端版本     | 建议                    |
| `tenantId`      | 租户 ID        | 可选，优先从 Token 解析 |
| `traceId`       | 链路追踪 ID    | 建议                    |
| `lang`          | 语言环境       | 可选                    |

服务端解析 Header 示例：

```java
String authorization = accessor.getFirstNativeHeader("Authorization");
String clientType = accessor.getFirstNativeHeader("clientType");
String deviceId = accessor.getFirstNativeHeader("deviceId");
String clientVersion = accessor.getFirstNativeHeader("clientVersion");
```

Header 使用注意事项如下：

| 注意项                    | 说明                       |
| ------------------------- | -------------------------- |
| 不信任 Header 中的 userId | 用户身份必须从 Token 解析  |
| 不打印完整 Authorization  | 日志中必须脱敏             |
| Header 不放大对象         | 大对象放 JSON Body         |
| 租户信息以服务端解析为准  | Header 只能作为辅助信息    |
| `receipt` 只代表协议确认  | 不等于业务处理最终成功     |
| `ack` 不等于已读          | 聊天已读应使用业务回执模型 |

### WebSocket 状态码

WebSocket 状态码用于表示连接关闭原因。浏览器、网关、服务端或客户端主动关闭连接时，可能会带上 Close Code。排查断线问题时，应结合关闭码、服务端断开日志、网关日志和前端控制台一起判断。

常见状态码如下：

| 状态码 | 名称                | 说明                               |
| ------ | ------------------- | ---------------------------------- |
| `1000` | Normal Closure      | 正常关闭                           |
| `1001` | Going Away          | 端点离开，例如页面关闭、服务重启   |
| `1002` | Protocol Error      | 协议错误                           |
| `1003` | Unsupported Data    | 接收了不支持的数据类型             |
| `1005` | No Status Received  | 未收到关闭状态，通常仅本地使用     |
| `1006` | Abnormal Closure    | 异常关闭，常见于网络断开、网关中断 |
| `1007` | Invalid Payload     | 消息体格式不合法                   |
| `1008` | Policy Violation    | 策略违规，例如认证、权限、黑名单   |
| `1009` | Message Too Big     | 消息过大                           |
| `1010` | Mandatory Extension | 客户端期望扩展但服务端未协商成功   |
| `1011` | Internal Error      | 服务端内部错误                     |
| `1012` | Service Restart     | 服务重启                           |
| `1013` | Try Again Later     | 服务暂不可用，建议稍后重试         |
| `1015` | TLS Handshake       | TLS 握手失败，通常仅本地使用       |

项目中建议自定义业务关闭原因，但不建议滥用 Close Code。业务错误应优先通过 `/user/queue/errors` 返回，连接关闭只用于会话失效、强制下线、协议异常、黑名单、严重限流等场景。

推荐关闭原因映射如下：

| 业务场景     | 推荐状态码 | 说明       |
| ------------ | ---------- | ---------- |
| 用户主动退出 | `1000`     | 正常断开   |
| 页面关闭     | `1001`     | 客户端离开 |
| Token 过期   | `1008`     | 策略违规   |
| 用户被封禁   | `1008`     | 策略违规   |
| 消息体过大   | `1009`     | 消息过大   |
| 服务重启     | `1012`     | 服务重启   |
| 服务过载     | `1013`     | 稍后重试   |
| 服务端异常   | `1011`     | 内部错误   |

### 常用配置参数

常用配置参数用于统一管理 WebSocket Endpoint、Broker、心跳、线程池、安全、限流、Redis、RabbitMQ 和监控。

推荐配置总览：

| 配置项                                                  | 示例值                  | 说明                     |
| ------------------------------------------------------- | ----------------------- | ------------------------ |
| `app.websocket.enabled`                                 | `true`                  | 是否启用 WebSocket       |
| `app.websocket.protocol-version`                        | `1.0`                   | 协议版本                 |
| `app.websocket.instance-id`                             | `${HOSTNAME}`           | 当前实例 ID              |
| `app.websocket.endpoint.path`                           | `/ws/stomp`             | STOMP Endpoint           |
| `app.websocket.endpoint.sockjs-enabled`                 | `true`                  | 是否启用 SockJS          |
| `app.websocket.endpoint.allowed-origin-patterns`        | `https://*.example.com` | Origin 白名单            |
| `app.websocket.broker.type`                             | `simple`、`rabbitmq`    | Broker 类型              |
| `app.websocket.broker.application-destination-prefixes` | `/app`                  | 应用发送前缀             |
| `app.websocket.broker.simple-destination-prefixes`      | `/topic,/queue`         | Simple Broker 前缀       |
| `app.websocket.broker.user-destination-prefix`          | `/user`                 | 用户目标前缀             |
| `app.websocket.heartbeat.server-to-client`              | `20000`                 | 服务端到客户端心跳，毫秒 |
| `app.websocket.heartbeat.client-to-server`              | `20000`                 | 客户端到服务端心跳，毫秒 |
| `app.websocket.heartbeat.session-ttl-seconds`           | `180`                   | Redis Session TTL        |
| `app.websocket.rate-limit.user-send-per-second`         | `5`                     | 用户发送限流             |
| `app.websocket.rate-limit.ip-connect-per-minute`        | `100`                   | IP 建连限流              |
| `app.websocket.rate-limit.max-message-bytes`            | `65536`                 | 单条消息最大字节         |
| `app.websocket.thread-pool.inbound.core-pool-size`      | `4`                     | 入站核心线程数           |
| `app.websocket.thread-pool.outbound.max-pool-size`      | `32`                    | 出站最大线程数           |
| `app.websocket.security.origin-check-enabled`           | `true`                  | 是否启用 Origin 校验     |
| `app.websocket.security.subscribe-auth-enabled`         | `true`                  | 是否启用订阅鉴权         |
| `app.websocket.security.audit-log-enabled`              | `true`                  | 是否启用安全审计         |

完整配置示例：

```yaml
app:
  websocket:
    enabled: true
    protocol-version: "1.0"
    instance-id: ${HOSTNAME:local-node}

    endpoint:
      path: /ws/stomp
      sockjs-enabled: true
      allowed-origin-patterns:
        - "https://admin.example.com"
        - "https://app.example.com"

    broker:
      type: rabbitmq
      application-destination-prefixes:
        - /app
      simple-destination-prefixes:
        - /topic
        - /queue
      user-destination-prefix: /user
      relay-host: rabbitmq.prod.internal
      relay-port: 61613
      client-login: ${WS_BROKER_CLIENT_LOGIN}
      client-passcode: ${WS_BROKER_CLIENT_PASSCODE}
      system-login: ${WS_BROKER_SYSTEM_LOGIN}
      system-passcode: ${WS_BROKER_SYSTEM_PASSCODE}
      system-heartbeat-send-interval: 10000
      system-heartbeat-receive-interval: 10000

    heartbeat:
      server-to-client: 20000
      client-to-server: 20000
      session-ttl-seconds: 180
      timeout-scan-interval: 30000

    rate-limit:
      enabled: true
      ip-connect-per-minute: 100
      user-connect-per-minute: 20
      user-send-per-second: 5
      session-subscribe-per-minute: 100
      group-send-per-second: 100
      admin-broadcast-per-minute: 5
      max-message-bytes: 65536
      risk-score-threshold: 30
```

配置建议如下：

| 建议                      | 说明                                   |
| ------------------------- | -------------------------------------- |
| 生产 Origin 不使用 `*`    | 明确允许域名                           |
| 生产使用 WSS              | 避免明文传输                           |
| 多实例使用 Redis + Broker | 不只依赖本地内存                       |
| Session TTL 大于心跳间隔  | 推荐 3 到 5 倍以上                     |
| 线程池队列有界            | 防止 OOM                               |
| 限流默认开启              | 连接、发送、订阅、广播都要覆盖         |
| 密码走环境变量            | 不写死在配置文件                       |
| 配置纳入审计              | WebSocket 配置变更可能影响安全和稳定性 |

### 常用异常说明

常用异常说明用于统一错误码、异常原因、处理方式和前端行为。错误码应保持稳定，前端可以基于错误码处理跳转、提示、重连、停止订阅等动作。

连接与认证异常：

| 错误码                    | 说明               | 前端处理               |
| ------------------------- | ------------------ | ---------------------- |
| `WS_CONNECT_FAILED`       | WebSocket 连接失败 | 展示连接失败，等待重连 |
| `WS_AUTH_TOKEN_MISSING`   | Token 缺失         | 跳转登录               |
| `WS_AUTH_TOKEN_INVALID`   | Token 无效         | 清理登录态，跳转登录   |
| `WS_AUTH_TOKEN_EXPIRED`   | Token 过期         | 刷新 Token 或跳转登录  |
| `WS_AUTH_USER_DISABLED`   | 用户被禁用         | 断开连接，提示账号异常 |
| `WS_BLACKLIST_HIT`        | 命中黑名单         | 停止重连，提示禁止访问 |
| `WS_CONNECT_RATE_LIMITED` | 连接频率超限       | 延迟重试               |

订阅异常：

| 错误码                             | 说明             | 前端处理               |
| ---------------------------------- | ---------------- | ---------------------- |
| `WS_SUBSCRIBE_DESTINATION_INVALID` | 订阅地址非法     | 停止订阅               |
| `WS_SUBSCRIBE_DENIED`              | 无订阅权限       | 提示无权限             |
| `WS_SUBSCRIBE_GROUP_DENIED`        | 无群组订阅权限   | 离开群组页面或刷新权限 |
| `WS_SUBSCRIBE_ADMIN_DENIED`        | 无管理端订阅权限 | 停止订阅管理端 Topic   |
| `WS_SUBSCRIBE_RATE_LIMITED`        | 订阅频率超限     | 降低订阅频率           |
| `WS_SUBSCRIBE_LIMIT_EXCEEDED`      | 单连接订阅数超限 | 清理无效订阅           |

发送异常：

| 错误码                        | 说明          | 前端处理               |
| ----------------------------- | ------------- | ---------------------- |
| `WS_SEND_DESTINATION_INVALID` | 发送地址非法  | 修正 destination       |
| `WS_SEND_DENIED`              | 无发送权限    | 提示无权限             |
| `WS_SEND_RATE_LIMITED`        | 发送频率超限  | 提示操作过快           |
| `WS_MESSAGE_EMPTY`            | 消息体为空    | 提示用户输入内容       |
| `WS_MESSAGE_TOO_LARGE`        | 消息过大      | 限制输入或改用文件上传 |
| `WS_MESSAGE_JSON_INVALID`     | JSON 格式错误 | 前端修正请求体         |
| `WS_MESSAGE_FIELD_INVALID`    | 字段值非法    | 显示字段错误           |
| `WS_USER_MUTED`               | 用户被禁言    | 提示禁言原因和时间     |

系统与推送异常：

| 错误码                  | 说明           | 处理方式                   |
| ----------------------- | -------------- | -------------------------- |
| `WS_PUSH_FAILED`        | 推送失败       | 记录失败消息，按策略重试   |
| `WS_BROKER_UNAVAILABLE` | Broker 不可用  | 告警、降级、失败入库       |
| `WS_REDIS_UNAVAILABLE`  | Redis 不可用   | 降级处理，告警             |
| `WS_SERIALIZE_FAILED`   | 消息序列化失败 | 记录异常，不重试或修复数据 |
| `WS_INTERNAL_ERROR`     | 服务内部异常   | 返回通用错误，记录 traceId |
| `WS_SESSION_EXPIRED`    | 会话过期       | 客户端重新连接             |
| `WS_SESSION_KICKED`     | 被强制下线     | 断开连接，提示原因         |

异常返回格式建议如下：

```json
{
  "messageId": "1909550919275922702",
  "type": "ERROR",
  "status": "FAILED",
  "version": "1.0",
  "timestamp": 1777956000000,
  "traceId": "trace-20260505-000001",
  "data": {
    "code": "WS_SEND_RATE_LIMITED",
    "message": "发送过于频繁，请稍后再试",
    "destination": "/app/chat/group/send",
    "requestMessageId": "client-msg-001",
    "retryable": true
  }
}
```

### 推荐依赖版本

推荐依赖版本应以 Spring Boot BOM 为主，不建议手动指定 Spring Framework、Jackson、Micrometer、Spring Messaging、Spring WebSocket 等由 Boot 管理的版本。对于 Spring Boot 3 项目，截至 2026-05-05，Spring 官方已发布 Spring Boot 3.5.14，并说明该版本包含 bug 修复、文档改进、依赖升级和若干 CVE 修复；若项目仍要求 Spring Boot 3 而不是 Spring Boot 4，建议优先选择当前维护线的 3.5.x，并在上线时再次确认最新补丁版本。([Home](https://spring.io/blog/2026/04/23/spring-boot-3-5-14-available-now?utm_source=chatgpt.com))

后端推荐依赖：

```xml
<dependencies>
    <!-- Spring Web：提供 REST API、管理端接口、初始化接口等 HTTP 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring WebSocket：提供 WebSocket、STOMP、消息代理配置能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <!-- Spring Security：用于连接认证、订阅鉴权、管理端权限隔离 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Validation：用于 WebSocket 消息 DTO 参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Redis：用于在线状态、Session 映射、限流、幂等、Pub/Sub -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- RabbitMQ：用于业务事件桥接或 Broker 相关能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- Actuator：用于健康检查、指标、Prometheus 暴露 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Prometheus 指标导出 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- Hutool：常用工具类，简化字符串、集合、日期、脱敏、ID 等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.38</version>
    </dependency>

    <!-- Lombok：减少 DTO、配置类、日志样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

Redisson 用于分布式锁、分布式限流、延迟队列等能力时，需要单独声明版本。截至检索结果，`redisson-spring-boot-starter` 在 Maven Repository 上显示 4.3.1 为 2026-04-06 发布的较新版本；生产项目应结合 Spring Boot 版本兼容性、企业安全扫描结果和压测结果选型。([Maven Repository](https://mvnrepository.com/artifact/org.redisson/redisson-spring-boot-starter?utm_source=chatgpt.com))

```xml
<!-- Redisson：用于 Redis 分布式锁、限流器、延迟队列等高级能力 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>4.3.1</version>
</dependency>
```

前端推荐依赖：

```bash
npm install @stomp/stompjs sockjs-client
npm install -D @types/sockjs-client
```

`@stomp/stompjs` 官方 npm 页面显示 7.1.1 版本，并说明其支持浏览器和 Node.js 的 STOMP over WebSocket 客户端能力；`sockjs-client` npm 页面显示 latest 为 1.6.1，可作为 WebSocket fallback 方案。([NPM](https://www.npmjs.com/package/@stomp/stompjs?activeTab=versions&utm_source=chatgpt.com))

外部组件推荐：

| 组件          | 推荐策略                             | 说明                                                         |
| ------------- | ------------------------------------ | ------------------------------------------------------------ |
| JDK           | 17 或更高                            | Spring Boot 3 基线通常使用 Java 17+                          |
| Spring Boot   | 3.5.x 当前补丁版                     | 使用 Boot BOM 管理依赖                                       |
| Redis         | 7.x                                  | 生产使用 Sentinel 或 Cluster                                 |
| RabbitMQ      | 4.2.x 稳定支持线，或验证后采用 4.3.x | RabbitMQ 官方 2026-04-23 发布了 4.3 版本；Release Information 页面仍列出 4.2.x 的支持信息，生产升级需结合官方支持策略验证。([RabbitMQ](https://www.rabbitmq.com/blog/2026/04/23/rabbitmq-4.3-release?utm_source=chatgpt.com)) |
| Nginx Ingress | 与集群版本匹配                       | 重点验证 WebSocket Upgrade 和超时                            |
| Prometheus    | 2.x/3.x 按企业标准                   | 采集 Actuator 指标                                           |
| Grafana       | 按企业标准                           | 展示连接、消息、失败、延迟、线程池                           |

依赖版本实践建议如下：

| 建议                           | 说明                                                   |
| ------------------------------ | ------------------------------------------------------ |
| Spring 相关依赖交给 Boot BOM   | 不手动指定 `spring-websocket`、`spring-messaging` 版本 |
| 第三方依赖上线前安全扫描       | Redisson、Hutool、SockJS、STOMP 客户端都要扫描         |
| 不追最新大版本                 | 生产优先补丁版本和长期维护线                           |
| 前后端协议版本单独管理         | 不等同于依赖版本                                       |
| 升级先压测                     | 尤其是 Spring Boot、RabbitMQ、Redis、Redisson          |
| 文档中写推荐线，不写死永久版本 | 上线时以官方最新补丁和企业基线为准                     |

### 推荐目录结构

推荐目录结构用于保持 WebSocket 模块清晰、可维护、可扩展。建议按配置、常量、枚举、DTO、模型、控制器、消息处理器、服务、事件、拦截器、监听器、监控、工具类拆分。

后端目录结构建议：

```text
src/main/java/io/github/atengk/websocket/
  config/
    WebSocketConfig.java
    WsBrokerProperties.java
    WsEndpointProperties.java
    WsHeartbeatProperties.java
    WsThreadPoolProperties.java
    WsExecutorConfig.java
    WsSecurityConfig.java
    WsMessageConverterConfig.java

  constant/
    WsDestinationConstant.java
    WsRedisKeyConstant.java
    WsHeaderConstant.java
    WsErrorCodeConstant.java

  enums/
    WsMessageType.java
    WsMessageStatus.java
    WsContentType.java
    WsAckType.java
    WsClientType.java
    WsSessionStatus.java
    WsRiskLevel.java

  model/
    WsMessage.java
    WsSessionInfo.java
    WsOnlineStatus.java
    WsPushResult.java

  dto/
    ChatTextSendRequest.java
    GroupChatSendRequest.java
    NoticeReadRequest.java
    MessageAckRequest.java
    WsErrorResponse.java
    WsInitResponse.java

  handler/
    ChatMessageHandler.java
    NoticeMessageHandler.java
    AckMessageHandler.java
    AdminWsMessageHandler.java
    WsGlobalExceptionHandler.java

  controller/
    WsInitController.java
    ChatMessageQueryController.java
    NoticeController.java

  controller/admin/
    AdminWsSessionController.java
    AdminWsMonitorController.java
    AdminWsPushTestController.java
    AdminWsMessageRecordController.java

  service/
    WsPushService.java
    WsSessionRegistryService.java
    WsOnlineStatusService.java
    WsGroupMemberService.java
    WsRateLimitService.java
    WsRiskControlService.java
    WsSecurityAuditLogService.java
    WsOfflineMessageService.java
    WsMessageReceiptService.java

  service/impl/
    WsPushServiceImpl.java
    RedisWsSessionRegistryServiceImpl.java
    RedisWsOnlineStatusServiceImpl.java
    RedisWsRateLimitServiceImpl.java
    RedisWsRiskControlServiceImpl.java
    WsSecurityAuditLogServiceImpl.java

  interceptor/
    WsHandshakeInterceptor.java
    WsChannelInterceptor.java
    WsSubscribeAuthInterceptor.java
    WsSendAuthInterceptor.java

  listener/
    WsConnectEventListener.java
    WsDisconnectEventListener.java
    WsSubscribeEventListener.java
    WsSessionExpireListener.java

  event/
    WsUserOnlineEvent.java
    WsUserOfflineEvent.java
    WsMessagePushFailedEvent.java
    WsGroupChangedEvent.java

  monitor/
    WsMetricsService.java
    WsMetricsInitializer.java
    WsThreadPoolMonitor.java

  task/
    WsSessionCleanTask.java
    WsOfflineMessageCompensateTask.java
    WsFailedMessageRetryTask.java
    WsDashboardPushTask.java

  util/
    WsDestinationValidator.java
    WsSensitiveMaskUtil.java
    WsTraceIdUtil.java
    WsMessageIdUtil.java
    WsSlowLogUtil.java
```

前端目录结构建议：

```text
src/
  api/
    websocket/
      stomp-client.ts
      ws-types.ts
      ws-routes.ts
      ws-message-dispatcher.ts
      ws-message-dedup.ts

  stores/
    websocket.ts
    notice.ts
    chat.ts

  hooks/
    useStompClient.ts
    useWsSubscription.ts

  views/
    chat/
      ChatRoom.vue
    notice/
      NoticeCenter.vue
    admin/
      WebSocketMonitor.vue
      OnlineSessionList.vue

  utils/
    ws-format.ts
    ws-device.ts
    ws-broadcast-channel.ts
```

目录结构建议如下：

| 建议                          | 说明                                     |
| ----------------------------- | ---------------------------------------- |
| WebSocket 配置集中放 `config` | Endpoint、Broker、线程池、转换器统一管理 |
| 消息处理器与 HTTP 控制器分离  | `@MessageMapping` 不与 REST 混在一起     |
| Redis Key 统一常量            | 防止 Key 拼写分散                        |
| 路由常量统一维护              | 前后端都应有路由规范                     |
| 管理端接口单独包              | `/controller/admin` 权限隔离             |
| 监控和任务独立包              | 避免与业务服务混杂                       |
| 前端客户端统一封装            | 页面不直接创建 STOMP Client              |
| 页面级订阅生命周期明确        | 进入页面订阅，离开页面取消               |

### 推荐消息路由规范

推荐消息路由规范用于统一客户端发送、用户队列、广播主题、群组主题、房间主题、系统通知、管理端看板等路径，避免不同模块各自定义导致权限混乱和维护困难。

路由前缀规范：

| 前缀               | 用途                       | 示例                              |
| ------------------ | -------------------------- | --------------------------------- |
| `/app/**`          | 客户端发送到服务端         | `/app/chat/private/send`          |
| `/user/queue/**`   | 用户私有订阅               | `/user/queue/notice`              |
| `/topic/system/**` | 系统广播                   | `/topic/system/notice`            |
| `/topic/group/**`  | 群组主题                   | `/topic/group/90001`              |
| `/topic/room/**`   | 房间主题                   | `/topic/room/room-001/count`      |
| `/topic/admin/**`  | 管理端主题                 | `/topic/admin/dashboard/message`  |
| `/topic/tenant/**` | 租户主题                   | `/topic/tenant/tenant-001/notice` |
| `/queue/**`        | 服务端发送用户目的地时使用 | `/queue/notice`                   |

客户端发送路由建议：

| 功能           | 路由                      |
| -------------- | ------------------------- |
| 单聊发送       | `/app/chat/private/send`  |
| 群聊发送       | `/app/chat/group/send`    |
| 消息 ACK       | `/app/message/ack`        |
| 通知已读       | `/app/notice/read`        |
| 全部通知已读   | `/app/notice/read-all`    |
| 加入房间       | `/app/room/join`          |
| 离开房间       | `/app/room/leave`         |
| 任务订阅       | `/app/task/subscribe`     |
| AI 对话发送    | `/app/ai/chat/send`       |
| AI 取消生成    | `/app/ai/chat/cancel`     |
| 管理端推送测试 | `/app/admin/push/test`    |
| 管理端踢人     | `/app/admin/session/kick` |

客户端订阅路由建议：

| 功能           | 路由                             |
| -------------- | -------------------------------- |
| 用户通知       | `/user/queue/notice`             |
| 用户聊天       | `/user/queue/chat`               |
| 用户错误       | `/user/queue/errors`             |
| 用户 ACK       | `/user/queue/ack`                |
| 会话控制       | `/user/queue/session`            |
| 用户订单状态   | `/user/queue/order`              |
| 用户审批通知   | `/user/queue/approval`           |
| AI 流式响应    | `/user/queue/ai/stream`          |
| 系统公告       | `/topic/system/notice`           |
| 系统告警       | `/topic/system/alarm`            |
| 群组消息       | `/topic/group/{groupId}`         |
| 群组状态       | `/topic/group/{groupId}/status`  |
| 房间人数       | `/topic/room/{roomId}/count`     |
| 管理端在线连接 | `/topic/admin/online`            |
| 管理端看板     | `/topic/admin/dashboard/message` |
| 管理端告警     | `/topic/admin/alarm`             |

服务端推送方法对应关系：

| 推送类型   | 服务端调用                                                  | 客户端订阅                       |
| ---------- | ----------------------------------------------------------- | -------------------------------- |
| 广播       | `convertAndSend("/topic/system/notice", payload)`           | `/topic/system/notice`           |
| 群组       | `convertAndSend("/topic/group/" + groupId, payload)`        | `/topic/group/{groupId}`         |
| 用户通知   | `convertAndSendToUser(userId, "/queue/notice", payload)`    | `/user/queue/notice`             |
| 用户聊天   | `convertAndSendToUser(userId, "/queue/chat", payload)`      | `/user/queue/chat`               |
| 错误消息   | `convertAndSendToUser(userId, "/queue/errors", payload)`    | `/user/queue/errors`             |
| 会话控制   | `convertAndSendToUser(userId, "/queue/session", payload)`   | `/user/queue/session`            |
| 管理端看板 | `convertAndSend("/topic/admin/dashboard/message", payload)` | `/topic/admin/dashboard/message` |

路由命名硬性规则：

| 规则                                                | 说明                                     |
| --------------------------------------------------- | ---------------------------------------- |
| 客户端 `SEND` 只能发往 `/app/**`                    | 其他前缀直接拒绝                         |
| 客户端不能 `SEND` 到 `/topic/**`                    | Topic 只能由服务端推送                   |
| 用户私有消息订阅必须使用 `/user/queue/**`           | 不把 userId 放入 Topic 路径              |
| 管理端路由统一 `/topic/admin/**` 和 `/app/admin/**` | 方便统一鉴权                             |
| 群组路由必须带 groupId                              | 便于群成员校验                           |
| 租户路由必须带 tenantId                             | 多租户隔离                               |
| 路由不包含敏感信息                                  | 不放 Token、手机号、邮箱等               |
| 路由不频繁变更                                      | 属于前后端协议                           |
| 服务端统一校验非法字符                              | 拒绝 `..`、`//`、`\` 等路径              |
| 路由文档化                                          | 每个路由说明发送方、订阅方、权限和消息体 |

最终推荐路由清单：

```text
# 连接地址
/ws/stomp

# 客户端发送
/app/chat/private/send
/app/chat/group/send
/app/message/ack
/app/notice/read
/app/notice/read-all
/app/room/join
/app/room/leave
/app/task/subscribe
/app/ai/chat/send
/app/ai/chat/cancel
/app/admin/push/test
/app/admin/session/kick

# 用户订阅
/user/queue/notice
/user/queue/chat
/user/queue/errors
/user/queue/ack
/user/queue/session
/user/queue/order
/user/queue/approval
/user/queue/ai/stream

# 广播订阅
/topic/system/notice
/topic/system/alarm
/topic/group/{groupId}
/topic/group/{groupId}/status
/topic/room/{roomId}/count
/topic/tenant/{tenantId}/notice
/topic/admin/online
/topic/admin/alarm
/topic/admin/dashboard/message
```

这份路由规范建议作为前后端联调基线，并在项目中以常量形式维护，避免页面、后端处理器、文档三处不一致。