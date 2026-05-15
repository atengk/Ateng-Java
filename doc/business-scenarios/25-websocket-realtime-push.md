# 实时推送与 WebSocket 在线状态

本案例对应「实时推送与 WebSocket 在线状态」业务场景，核心关注连接管理、在线状态、多节点推送、离线消息、消息确认、心跳检测和权限隔离等问题。

## 场景说明

实时推送主要用于服务端主动向客户端发送业务消息，解决传统 HTTP 轮询实时性差、请求量大、服务端压力高的问题。

本案例以 Spring Boot WebSocket 为核心实现实时连接，以 Redis 维护在线状态和跨节点消息分发，以 MySQL 保存离线消息和消息确认状态。最终实现用户上线、用户下线、在线推送、离线补发、消息确认等核心能力。

### 适用业务

该方案适合需要服务端主动通知客户端的业务系统，例如：

```text
聊天消息
工单通知
审批通知
订单状态变更
排队叫号
系统公告
告警推送
站内信实时提醒
后台任务进度通知
```

典型业务流程如下：

```text
用户打开页面
-> 建立 WebSocket 连接
-> 服务端校验 Token
-> 记录用户在线状态
-> 业务系统产生消息
-> 判断用户是否在线
-> 在线则实时推送
-> 离线则保存离线消息
-> 用户再次上线后补发未确认消息
```

### 核心功能

本案例重点实现以下核心功能：

| 功能               | 说明                                         |
| ------------------ | -------------------------------------------- |
| WebSocket 连接鉴权 | 建立连接时校验用户 Token，拒绝非法连接       |
| 在线状态维护       | 用户连接成功后记录在线状态，断开后清理状态   |
| 单用户推送         | 根据用户 ID 向指定用户发送消息               |
| 多用户推送         | 支持批量用户消息推送                         |
| 租户隔离推送       | 按租户维度限制消息投递范围                   |
| 离线消息保存       | 用户不在线时将消息保存到数据库               |
| 上线补发消息       | 用户重新上线后补发未读或未确认消息           |
| 消息 ACK 确认      | 客户端收到消息后主动确认，服务端更新消息状态 |
| 多节点推送         | 通过 Redis Pub/Sub 实现跨节点消息转发        |
| 心跳检测           | 客户端定时发送心跳，服务端更新连接活跃时间   |

### 实现目标

本案例不追求完整 IM 系统，而是实现企业后台系统中最常见、最实用的实时推送基础能力。

最终目标如下：

```text
1. 用户携带 Token 建立 WebSocket 连接
2. 服务端解析用户身份并保存连接会话
3. Redis 记录用户在线状态，支持多节点共享
4. 业务接口可以按用户 ID 推送消息
5. 在线用户立即收到消息
6. 离线用户的消息落库保存
7. 用户上线后自动补发离线消息
8. 客户端收到消息后调用 ACK 接口确认
9. 服务端根据 ACK 更新消息确认状态
10. 多个服务实例之间可以通过 Redis 转发消息
```

本案例的实现边界如下：

```text
包含：
- WebSocket 服务端连接管理
- Token 鉴权
- 在线状态管理
- 单用户消息推送
- Redis 跨节点转发
- 离线消息保存和补发
- 消息确认接口

不包含：
- 完整聊天会话体系
- 群聊消息扩散
- 消息全文检索
- 大规模 IM 分布式网关
- Netty 自研协议栈
- WebRTC 音视频通信
```

本案例更适合作为后台管理系统、SaaS 系统、工单系统、审批系统、订单系统中的实时通知基础模块。

## 技术方案

本案例基于原 README 中推荐的 `Spring Boot WebSocket + Redis + RabbitMQ / Kafka + MyBatis-Plus + Sa-Token + Redisson` 技术栈展开，核心目标是实现「在线直接推送，离线消息落库，多节点通过 Redis 转发」这一套最小可用方案。

### 技术栈选型

| 技术             | 用途                               |
| ---------------- | ---------------------------------- |
| Spring Boot 3    | 项目基础框架                       |
| Spring WebSocket | 建立 WebSocket 长连接              |
| Sa-Token         | WebSocket 握手阶段解析用户身份     |
| Redis            | 保存在线状态、连接路由、发布订阅   |
| Redisson         | 分布式锁、部分并发控制             |
| MyBatis-Plus     | 离线消息、消息确认记录的数据库操作 |
| MySQL            | 持久化消息记录                     |
| Hutool           | JSON、集合、字符串、日期等工具处理 |
| Lombok           | 简化实体类、DTO、VO 代码           |

本案例暂不引入 Netty 自研协议栈，优先使用 Spring Boot 原生 WebSocket，原因是集成成本低，适合后台系统、工单系统、审批系统、通知中心这类中等并发场景。

### 整体流程

整体流程分为连接建立、在线状态维护、业务推送、离线存储、上线补发、消息确认六个部分。

```text
客户端携带 Token 建立 WebSocket 连接
-> 服务端通过 Sa-Token 校验 Token
-> 解析 userId、tenantId
-> 保存本机 WebSocketSession
-> Redis 记录用户在线状态和所在服务节点
-> 业务接口触发消息推送
-> 判断用户是否在线
-> 在线：通过 Redis Pub/Sub 广播到对应节点
-> 对应节点找到本地连接并发送消息
-> 离线：消息保存到 MySQL
-> 用户上线后查询未确认消息并补发
-> 客户端收到消息后调用 ACK 接口确认
-> 服务端更新消息状态
```

核心交互可以简化为：

```text
业务系统
  |
  | 调用消息发送接口
  v
消息服务
  |
  | 判断在线状态
  |-------------------- 在线 --------------------|
  |                                            |
  v                                            v
Redis Pub/Sub                              当前节点 / 其他节点
  |                                            |
  v                                            v
WebSocketSession.sendMessage()          客户端接收消息

  |-------------------- 离线 --------------------|
  v
MySQL 保存离线消息
  |
  v
用户下次上线后补发
```

### 核心表设计

本案例只保留核心表，主要包括消息表和消息确认表。在线状态不落库，直接存 Redis，避免频繁上下线导致数据库写入压力过高。

消息主表用于保存所有需要追踪的推送消息，包括在线消息和离线消息。

```sql
-- 推送消息主表
CREATE TABLE ws_message (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    receiver_id BIGINT NOT NULL COMMENT '接收用户ID',
    message_type VARCHAR(64) NOT NULL COMMENT '消息类型，如 ORDER_STATUS、WORK_ORDER、SYSTEM_NOTICE',
    title VARCHAR(128) NOT NULL COMMENT '消息标题',
    content TEXT NOT NULL COMMENT '消息内容',
    biz_id VARCHAR(128) DEFAULT NULL COMMENT '业务ID，如订单ID、工单ID、审批单ID',
    biz_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    push_status TINYINT NOT NULL DEFAULT 0 COMMENT '推送状态：0待推送，1已推送，2推送失败',
    ack_status TINYINT NOT NULL DEFAULT 0 COMMENT '确认状态：0未确认，1已确认',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    last_push_time DATETIME DEFAULT NULL COMMENT '最近一次推送时间',
    ack_time DATETIME DEFAULT NULL COMMENT '确认时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',

    KEY idx_receiver_ack (receiver_id, ack_status),
    KEY idx_tenant_receiver (tenant_id, receiver_id),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WebSocket推送消息表';
```

如果消息确认逻辑比较简单，可以只使用 `ws_message.ack_status`。如果后续要支持一条消息发给多人，可以拆出确认明细表。

```sql
-- 消息确认明细表，适合批量消息、角色消息、租户消息场景
CREATE TABLE ws_message_ack (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    receiver_id BIGINT NOT NULL COMMENT '接收用户ID',
    ack_status TINYINT NOT NULL DEFAULT 0 COMMENT '确认状态：0未确认，1已确认',
    ack_time DATETIME DEFAULT NULL COMMENT '确认时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',

    UNIQUE KEY uk_message_receiver (message_id, receiver_id),
    KEY idx_receiver_ack (receiver_id, ack_status),
    KEY idx_tenant_receiver (tenant_id, receiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WebSocket消息确认表';
```

状态值建议在代码中使用枚举维护：

```text
push_status:
0 待推送
1 已推送
2 推送失败

ack_status:
0 未确认
1 已确认
```

### Redis Key 设计

Redis 主要负责在线状态、连接路由、节点信息和跨节点发布订阅。

| Key                                | 类型            | 示例                        | 说明                      |
| ---------------------------------- | --------------- | --------------------------- | ------------------------- |
| `ws:online:user:{userId}`          | String          | `ws:online:user:10001`      | 用户在线状态，值为节点 ID |
| `ws:online:tenant:{tenantId}`      | Set             | `ws:online:tenant:1`        | 租户下在线用户集合        |
| `ws:node:{nodeId}`                 | Hash            | `ws:node:node-001`          | 服务节点信息              |
| `ws:user:last-active:{userId}`     | String          | `ws:user:last-active:10001` | 用户最近活跃时间          |
| `ws:pubsub:push`                   | Pub/Sub Channel | `ws:pubsub:push`            | 跨节点推送频道            |
| `ws:lock:connect:{userId}`         | Lock            | `ws:lock:connect:10001`     | 用户连接注册锁            |
| `ws:lock:ack:{messageId}:{userId}` | Lock            | `ws:lock:ack:10001:20001`   | 消息确认锁                |

推荐过期时间：

| Key                            | 过期时间   | 说明                     |
| ------------------------------ | ---------- | ------------------------ |
| `ws:online:user:{userId}`      | 90 秒      | 依赖心跳续期             |
| `ws:user:last-active:{userId}` | 7 天       | 便于排查用户最近在线时间 |
| `ws:node:{nodeId}`             | 90 秒      | 服务节点定时续期         |
| `ws:online:tenant:{tenantId}`  | 不固定过期 | 连接断开时主动移除用户   |

Redis 值示例：

```json
{
  "userId": 10001,
  "tenantId": 1,
  "nodeId": "node-001",
  "sessionId": "f8b8d7c1",
  "connectTime": "2026-05-15 10:30:00",
  "lastActiveTime": "2026-05-15 10:30:30"
}
```

## 项目准备

本节先准备依赖、配置和目录结构。后续代码会基于这些基础文件继续展开。

### Maven 依赖

文件位置：`pom.xml`

下面依赖覆盖 WebSocket、Redis、Redisson、Sa-Token、MyBatis-Plus、MySQL、Hutool 和 Lombok。

```xml
<dependencies>
    <!-- Spring Boot Web：提供基础 Web 能力和 REST 接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot WebSocket：提供 WebSocket 服务端支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <!-- Spring Data Redis：操作 Redis 在线状态、发布订阅、缓存数据 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Redisson：分布式锁、连接注册并发控制 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.31.0</version>
    </dependency>

    <!-- MyBatis-Plus：简化消息表 CRUD 操作 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Sa-Token：WebSocket 握手阶段解析和校验登录 Token -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>1.39.0</version>
    </dependency>

    <!-- Hutool：JSON、日期、字符串、集合等常用工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.29</version>
    </dependency>

    <!-- Lombok：减少实体类、DTO、日志对象样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：单元测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目已经统一管理版本，可以把 `version` 移到父工程的 `dependencyManagement` 中。

### 配置文件

文件位置：`src/main/resources/application.yml`

下面配置包含服务端口、数据库、Redis、Sa-Token 和 WebSocket 自定义参数。

```yaml
server:
  port: 8080

spring:
  application:
    name: websocket-push-demo

  datasource:
    # MySQL 连接地址
    url: jdbc:mysql://127.0.0.1:3306/websocket_push_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    # 数据库账号
    username: root
    # 数据库密码
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      # Redis 地址
      host: 127.0.0.1
      # Redis 端口
      port: 6379
      # Redis 密码，没有密码可以删除该配置
      password:
      # Redis 数据库
      database: 0
      # 连接超时时间
      timeout: 3s

mybatis-plus:
  configuration:
    # 控制台打印 SQL，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 逻辑删除字段
      logic-delete-field: deleted
      # 删除值
      logic-delete-value: 1
      # 未删除值
      logic-not-delete-value: 0

sa-token:
  # Token 名称，客户端连接 WebSocket 时需要携带该参数
  token-name: satoken
  # Token 有效期，单位秒
  timeout: 2592000
  # 是否允许同一账号多端登录
  is-concurrent: true
  # 是否共享 Token
  is-share: true
  # 是否输出操作日志
  is-log: false

websocket:
  push:
    # 当前服务节点 ID，多实例部署时必须唯一
    node-id: node-001
    # WebSocket 连接路径
    endpoint: /ws/push
    # Redis 发布订阅频道
    pubsub-channel: ws:pubsub:push
    # 在线状态过期时间，单位秒
    online-expire-seconds: 90
    # 心跳续期时间，单位秒
    heartbeat-expire-seconds: 90
    # 用户上线后补发的最大离线消息数量
    offline-limit: 100
```

本地启动 Redis 和 MySQL 可以使用 Docker：

```bash
# 启动 MySQL
docker run -d \
  --name mysql-websocket \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=websocket_push_demo \
  mysql:8.0

# 启动 Redis
docker run -d \
  --name redis-websocket \
  -p 6379:6379 \
  redis:7.2
```

上面命令会分别启动 MySQL 8.0 和 Redis 7.2。MySQL 会自动创建 `websocket_push_demo` 数据库，后续只需要执行前面的建表 SQL。

### 项目结构

本案例使用标准 Spring Boot 分层结构，基础包名为 `io.github.atengk.websocket`。

```text
websocket-push-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               └── websocket
        │                   ├── WebSocketPushApplication.java
        │                   ├── config
        │                   │   ├── WebSocketConfig.java
        │                   │   ├── WebSocketPushProperties.java
        │                   │   └── RedisSubscriberConfig.java
        │                   ├── constant
        │                   │   └── WsRedisKeyConstant.java
        │                   ├── controller
        │                   │   ├── WsMessageController.java
        │                   │   └── WsOnlineController.java
        │                   ├── dto
        │                   │   ├── WsMessageSendDTO.java
        │                   │   └── WsMessageAckDTO.java
        │                   ├── entity
        │                   │   ├── WsMessage.java
        │                   │   └── WsMessageAck.java
        │                   ├── enums
        │                   │   ├── WsAckStatusEnum.java
        │                   │   └── WsPushStatusEnum.java
        │                   ├── handler
        │                   │   └── PushWebSocketHandler.java
        │                   ├── mapper
        │                   │   ├── WsMessageMapper.java
        │                   │   └── WsMessageAckMapper.java
        │                   ├── model
        │                   │   ├── WsSessionUser.java
        │                   │   ├── WsPushMessage.java
        │                   │   └── WsPubSubMessage.java
        │                   ├── service
        │                   │   ├── WsConnectionService.java
        │                   │   ├── WsMessageService.java
        │                   │   └── WsOnlineService.java
        │                   └── service
        │                       └── impl
        │                           ├── WsConnectionServiceImpl.java
        │                           ├── WsMessageServiceImpl.java
        │                           └── WsOnlineServiceImpl.java
        └── resources
            ├── application.yml
            └── mapper
                ├── WsMessageMapper.xml
                └── WsMessageAckMapper.xml
```

各目录职责如下：

| 目录         | 说明                                         |
| ------------ | -------------------------------------------- |
| `config`     | WebSocket、Redis 订阅、自定义配置            |
| `constant`   | Redis Key 常量                               |
| `controller` | 消息发送、在线状态查询、ACK 确认接口         |
| `dto`        | 接口请求参数                                 |
| `entity`     | 数据库实体                                   |
| `enums`      | 推送状态、确认状态枚举                       |
| `handler`    | WebSocket 连接、消息、关闭、异常处理         |
| `mapper`     | MyBatis-Plus Mapper                          |
| `model`      | WebSocket 会话用户、推送消息、Redis 广播消息 |
| `service`    | 在线状态、连接管理、消息推送核心逻辑         |

## WebSocket 连接管理

本节实现 WebSocket 的核心连接生命周期，包括连接配置、握手鉴权、会话注册和断开清理。原场景中的核心难点包括连接管理、用户在线状态、心跳检测和连接清理，本节先把连接层打通。

### WebSocket 配置

WebSocket 配置负责注册连接地址、绑定处理器、绑定握手拦截器。客户端后续通过 `/ws/push?satoken=xxx&tenantId=1` 建立连接。

文件位置：`src/main/java/io/github/atengk/websocket/config/WebSocketPushProperties.java`

下面配置类用于读取 `application.yml` 中的 `websocket.push` 配置。

```java
package io.github.atengk.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebSocket 推送配置属性
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@ConfigurationProperties(prefix = "websocket.push")
public class WebSocketPushProperties {

    /**
     * 当前服务节点 ID，多节点部署时必须唯一
     */
    private String nodeId = "node-001";

    /**
     * WebSocket 连接端点
     */
    private String endpoint = "/ws/push";

    /**
     * Redis 发布订阅频道
     */
    private String pubsubChannel = "ws:pubsub:push";

    /**
     * 在线状态过期时间，单位秒
     */
    private Long onlineExpireSeconds = 90L;

    /**
     * 心跳续期时间，单位秒
     */
    private Long heartbeatExpireSeconds = 90L;

    /**
     * 上线后补发离线消息数量
     */
    private Integer offlineLimit = 100;
}
```

文件位置：`src/main/java/io/github/atengk/websocket/config/WebSocketConfig.java`

下面配置类注册 WebSocket 服务端处理器和握手鉴权拦截器。

```java
package io.github.atengk.websocket.config;

import io.github.atengk.websocket.handler.PushWebSocketHandler;
import io.github.atengk.websocket.handler.WebSocketAuthHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 服务端配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@EnableConfigurationProperties(WebSocketPushProperties.class)
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketPushProperties properties;
    private final PushWebSocketHandler pushWebSocketHandler;
    private final WebSocketAuthHandshakeInterceptor authHandshakeInterceptor;

    /**
     * 注册 WebSocket 处理器
     *
     * @param registry WebSocket 处理器注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pushWebSocketHandler, properties.getEndpoint())
                .addInterceptors(authHandshakeInterceptor)
                // 生产环境建议替换成具体域名
                .setAllowedOriginPatterns("*");
    }
}
```

### 连接鉴权

连接鉴权在 WebSocket 握手阶段完成。服务端从请求参数或请求头中读取 `satoken`，通过 Sa-Token 解析登录用户 ID。如果 Token 无效，直接拒绝连接。

生产环境中，`tenantId` 不建议完全信任前端传参，最好从登录会话、Token 扩展字段或用户租户关系表中读取。这里为了突出核心流程，示例中从请求参数读取。

文件位置：`src/main/java/io/github/atengk/websocket/handler/WebSocketAuthHandshakeInterceptor.java`

下面拦截器用于在 WebSocket 建连前完成 Token 校验和用户信息写入。

```java
package io.github.atengk.websocket.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * WebSocket 握手前鉴权
     *
     * @param request    请求对象
     * @param response   响应对象
     * @param wsHandler  WebSocket 处理器
     * @param attributes WebSocket 会话属性
     * @return 是否允许建立连接
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("WebSocket握手失败：非法请求类型");
            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();

        String tokenName = StpUtil.getTokenName();
        String token = httpRequest.getParameter(tokenName);
        if (StrUtil.isBlank(token)) {
            token = httpRequest.getHeader(tokenName);
        }

        if (StrUtil.isBlank(token)) {
            log.warn("WebSocket握手失败：Token为空");
            return false;
        }

        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                log.warn("WebSocket握手失败：Token无效");
                return false;
            }

            Long userId = Convert.toLong(loginId);
            Long tenantId = Convert.toLong(httpRequest.getParameter("tenantId"), 1L);

            attributes.put("userId", userId);
            attributes.put("tenantId", tenantId);
            attributes.put("token", token);

            log.info("WebSocket握手鉴权成功，用户ID：{}，租户ID：{}", userId, tenantId);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket握手失败：Token校验异常，原因：{}", e.getMessage());
            return false;
        }
    }

    /**
     * WebSocket 握手后处理
     *
     * @param request   请求对象
     * @param response  响应对象
     * @param wsHandler WebSocket 处理器
     * @param exception 异常信息
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手后暂无额外处理
    }
}
```

客户端连接示例：

```text
ws://127.0.0.1:8080/ws/push?satoken=你的Token&tenantId=1
```

### 会话注册

会话注册的目标是把当前连接保存到服务端内存，同时把用户在线状态写入 Redis。服务端内存保存的是当前节点上的真实 `WebSocketSession`，Redis 保存的是跨节点共享的在线状态和连接路由。

文件位置：`src/main/java/io/github/atengk/websocket/model/WsSessionUser.java`

下面模型用于保存 WebSocket 连接关联的用户信息。

```java
package io.github.atengk.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 会话用户信息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsSessionUser {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * WebSocket 会话 ID
     */
    private String sessionId;

    /**
     * 当前服务节点 ID
     */
    private String nodeId;

    /**
     * 登录 Token
     */
    private String token;

    /**
     * 连接时间
     */
    private String connectTime;

    /**
     * 最近活跃时间
     */
    private String lastActiveTime;
}
```

文件位置：`src/main/java/io/github/atengk/websocket/constant/WsRedisKeyConstant.java`

下面常量类统一维护 WebSocket 模块使用的 Redis Key。

```java
package io.github.atengk.websocket.constant;

/**
 * WebSocket Redis Key 常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class WsRedisKeyConstant {

    private WsRedisKeyConstant() {
    }

    /**
     * 用户在线状态 Key
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String onlineUser(Long userId) {
        return "ws:online:user:" + userId;
    }

    /**
     * 用户多端会话 Hash Key
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String onlineUserSessions(Long userId) {
        return "ws:online:user:sessions:" + userId;
    }

    /**
     * WebSocket 会话 Key
     *
     * @param sessionId 会话 ID
     * @return Redis Key
     */
    public static String session(String sessionId) {
        return "ws:session:" + sessionId;
    }

    /**
     * 租户在线用户集合 Key
     *
     * @param tenantId 租户 ID
     * @return Redis Key
     */
    public static String tenantOnline(Long tenantId) {
        return "ws:online:tenant:" + tenantId;
    }

    /**
     * 用户最近活跃时间 Key
     *
     * @param userId 用户 ID
     * @return Redis Key
     */
    public static String lastActive(Long userId) {
        return "ws:user:last-active:" + userId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/websocket/service/WsConnectionService.java`

下面接口定义本机 WebSocket 会话的注册、清理、心跳刷新和本地推送能力。

```java
package io.github.atengk.websocket.service;

import io.github.atengk.websocket.model.WsSessionUser;
import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket 本地连接服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface WsConnectionService {

    /**
     * 注册 WebSocket 会话
     *
     * @param user    会话用户
     * @param session WebSocket 会话
     */
    void registerSession(WsSessionUser user, WebSocketSession session);

    /**
     * 注销 WebSocket 会话
     *
     * @param session WebSocket 会话
     */
    void unregisterSession(WebSocketSession session);

    /**
     * 刷新心跳
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     */
    void refreshHeartbeat(Long userId, String sessionId);

    /**
     * 判断用户在当前节点是否在线
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    boolean isLocalOnline(Long userId);

    /**
     * 向当前节点的指定用户发送消息
     *
     * @param userId  用户 ID
     * @param message 消息内容
     */
    void sendToLocalUser(Long userId, String message);
}
```

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsConnectionServiceImpl.java`

下面实现类维护当前 JVM 内的 WebSocketSession，并支持同一用户多端在线。

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.websocket.model.WsSessionUser;
import io.github.atengk.websocket.service.WsConnectionService;
import io.github.atengk.websocket.service.WsOnlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 本地连接服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsConnectionServiceImpl implements WsConnectionService {

    private static final String SESSION_USER_KEY = "wsUser";

    /**
     * sessionId -> WebSocketSession
     */
    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * userId -> sessionId 集合
     */
    private final ConcurrentHashMap<Long, Set<String>> userSessionMap = new ConcurrentHashMap<>();

    private final WsOnlineService wsOnlineService;

    /**
     * 注册 WebSocket 会话
     *
     * @param user    会话用户
     * @param session WebSocket 会话
     */
    @Override
    public void registerSession(WsSessionUser user, WebSocketSession session) {
        session.getAttributes().put(SESSION_USER_KEY, user);

        sessionMap.put(user.getSessionId(), session);
        userSessionMap.computeIfAbsent(user.getUserId(), key -> ConcurrentHashMap.newKeySet())
                .add(user.getSessionId());

        wsOnlineService.online(user);

        log.info("WebSocket会话注册成功，用户ID：{}，会话ID：{}，当前节点会话数：{}",
                user.getUserId(), user.getSessionId(), sessionMap.size());
    }

    /**
     * 注销 WebSocket 会话
     *
     * @param session WebSocket 会话
     */
    @Override
    public void unregisterSession(WebSocketSession session) {
        Object userObject = session.getAttributes().get(SESSION_USER_KEY);
        if (!(userObject instanceof WsSessionUser user)) {
            sessionMap.remove(session.getId());
            log.warn("WebSocket会话清理：未找到用户信息，会话ID：{}", session.getId());
            return;
        }

        sessionMap.remove(user.getSessionId());

        Set<String> sessionIds = userSessionMap.get(user.getUserId());
        if (CollUtil.isNotEmpty(sessionIds)) {
            sessionIds.remove(user.getSessionId());
            if (CollUtil.isEmpty(sessionIds)) {
                userSessionMap.remove(user.getUserId());
            }
        }

        wsOnlineService.offline(user);

        log.info("WebSocket会话注销成功，用户ID：{}，会话ID：{}，剩余节点会话数：{}",
                user.getUserId(), user.getSessionId(), sessionMap.size());
    }

    /**
     * 刷新心跳
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     */
    @Override
    public void refreshHeartbeat(Long userId, String sessionId) {
        wsOnlineService.refreshHeartbeat(userId, sessionId);
    }

    /**
     * 判断用户在当前节点是否在线
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    @Override
    public boolean isLocalOnline(Long userId) {
        Set<String> sessionIds = userSessionMap.get(userId);
        return CollUtil.isNotEmpty(sessionIds);
    }

    /**
     * 向当前节点的指定用户发送消息
     *
     * @param userId  用户 ID
     * @param message 消息内容
     */
    @Override
    public void sendToLocalUser(Long userId, String message) {
        Set<String> sessionIds = userSessionMap.get(userId);
        if (CollUtil.isEmpty(sessionIds)) {
            log.info("本节点用户不在线，用户ID：{}", userId);
            return;
        }

        for (String sessionId : sessionIds) {
            WebSocketSession session = sessionMap.get(sessionId);
            if (session == null || !session.isOpen()) {
                log.warn("WebSocket会话不可用，用户ID：{}，会话ID：{}", userId, sessionId);
                continue;
            }

            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
                log.info("WebSocket本地消息发送成功，用户ID：{}，会话ID：{}", userId, sessionId);
            } catch (Exception e) {
                log.error("WebSocket本地消息发送失败，用户ID：{}，会话ID：{}，原因：{}",
                        userId, sessionId, e.getMessage(), e);
            }
        }
    }
}
```

### 连接断开清理

连接断开时必须同时清理本地会话和 Redis 在线状态。这里通过 `afterConnectionClosed` 和 `handleTransportError` 两个入口兜底处理，避免异常断开后连接残留。

文件位置：`src/main/java/io/github/atengk/websocket/handler/PushWebSocketHandler.java`

下面处理器负责 WebSocket 建立连接、心跳消息、断开连接和异常清理。

```java
package io.github.atengk.websocket.handler;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.websocket.config.WebSocketPushProperties;
import io.github.atengk.websocket.model.WsSessionUser;
import io.github.atengk.websocket.service.WsConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 推送处理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketPushProperties properties;
    private final WsConnectionService wsConnectionService;

    /**
     * WebSocket 连接建立后处理
     *
     * @param session WebSocket 会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = Convert.toLong(session.getAttributes().get("userId"));
        Long tenantId = Convert.toLong(session.getAttributes().get("tenantId"));
        String token = Convert.toStr(session.getAttributes().get("token"));

        if (userId == null || tenantId == null || StrUtil.isBlank(token)) {
            log.warn("WebSocket连接建立失败：用户信息不完整，会话ID：{}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("用户信息不完整"));
            return;
        }

        WsSessionUser sessionUser = WsSessionUser.builder()
                .userId(userId)
                .tenantId(tenantId)
                .sessionId(session.getId())
                .nodeId(properties.getNodeId())
                .token(token)
                .connectTime(DateUtil.now())
                .lastActiveTime(DateUtil.now())
                .build();

        wsConnectionService.registerSession(sessionUser, session);

        Dict response = Dict.create()
                .set("type", "CONNECTED")
                .set("sessionId", session.getId())
                .set("serverTime", DateUtil.now());

        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(response)));
    }

    /**
     * 处理客户端文本消息
     *
     * @param session WebSocket 会话
     * @param message 文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if (!JSONUtil.isTypeJSON(payload)) {
            log.warn("WebSocket消息格式错误，会话ID：{}，消息内容：{}", session.getId(), payload);
            return;
        }

        JSONObject jsonObject = JSONUtil.parseObj(payload);
        String type = jsonObject.getStr("type");

        if (StrUtil.equalsIgnoreCase("PING", type)) {
            Long userId = Convert.toLong(session.getAttributes().get("userId"));
            wsConnectionService.refreshHeartbeat(userId, session.getId());

            Dict response = Dict.create()
                    .set("type", "PONG")
                    .set("serverTime", DateUtil.now());

            synchronized (session) {
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(response)));
            }
            return;
        }

        log.info("收到WebSocket客户端消息，会话ID：{}，消息类型：{}", session.getId(), type);
    }

    /**
     * WebSocket 连接关闭后处理
     *
     * @param session WebSocket 会话
     * @param status  关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        wsConnectionService.unregisterSession(session);
        log.info("WebSocket连接关闭，会话ID：{}，状态：{}", session.getId(), status);
    }

    /**
     * WebSocket 传输异常处理
     *
     * @param session   WebSocket 会话
     * @param exception 异常信息
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("WebSocket连接异常，会话ID：{}，原因：{}", session.getId(), exception.getMessage());
        wsConnectionService.unregisterSession(session);

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
```

客户端心跳消息格式：

```json
{
  "type": "PING"
}
```

服务端心跳响应格式：

```json
{
  "type": "PONG",
  "serverTime": "2026-05-15 10:30:30"
}
```

## 在线状态管理

在线状态管理不建议直接落库。WebSocket 连接上下线很频繁，如果每次连接变化都写 MySQL，会带来不必要的写入压力。本案例使用 Redis 保存在线状态，并通过过期时间和心跳续期解决异常断开后的状态残留问题。

### 用户上线处理

用户上线时需要写入四类信息：用户在线标记、用户多端会话、会话详情、租户在线集合。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsOnlineService.java`

下面接口定义在线、离线、心跳刷新和在线查询能力。

```java
package io.github.atengk.websocket.service;

import io.github.atengk.websocket.model.WsSessionUser;

import java.util.List;

/**
 * WebSocket 在线状态服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface WsOnlineService {

    /**
     * 用户上线
     *
     * @param user 会话用户
     */
    void online(WsSessionUser user);

    /**
     * 用户下线
     *
     * @param user 会话用户
     */
    void offline(WsSessionUser user);

    /**
     * 刷新心跳
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     */
    void refreshHeartbeat(Long userId, String sessionId);

    /**
     * 判断用户是否在线
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    boolean isOnline(Long userId);

    /**
     * 查询租户在线用户
     *
     * @param tenantId 租户 ID
     * @return 在线用户 ID 集合
     */
    List<Long> listOnlineUsersByTenant(Long tenantId);
}
```

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsOnlineServiceImpl.java`

下面实现类使用 Redis 维护用户在线状态，并通过心跳续期减少异常断开带来的脏在线状态。

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.websocket.config.WebSocketPushProperties;
import io.github.atengk.websocket.constant.WsRedisKeyConstant;
import io.github.atengk.websocket.model.WsSessionUser;
import io.github.atengk.websocket.service.WsOnlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * WebSocket 在线状态服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsOnlineServiceImpl implements WsOnlineService {

    private final StringRedisTemplate stringRedisTemplate;
    private final WebSocketPushProperties properties;

    /**
     * 用户上线
     *
     * @param user 会话用户
     */
    @Override
    public void online(WsSessionUser user) {
        String onlineUserKey = WsRedisKeyConstant.onlineUser(user.getUserId());
        String userSessionsKey = WsRedisKeyConstant.onlineUserSessions(user.getUserId());
        String sessionKey = WsRedisKeyConstant.session(user.getSessionId());
        String tenantOnlineKey = WsRedisKeyConstant.tenantOnline(user.getTenantId());
        String lastActiveKey = WsRedisKeyConstant.lastActive(user.getUserId());

        Duration onlineExpire = Duration.ofSeconds(properties.getOnlineExpireSeconds());

        stringRedisTemplate.opsForValue().set(onlineUserKey, user.getNodeId(), onlineExpire);
        stringRedisTemplate.opsForHash().put(userSessionsKey, user.getSessionId(), user.getNodeId());
        stringRedisTemplate.expire(userSessionsKey, onlineExpire);

        stringRedisTemplate.opsForValue().set(sessionKey, JSONUtil.toJsonStr(user), onlineExpire);
        stringRedisTemplate.opsForSet().add(tenantOnlineKey, Convert.toStr(user.getUserId()));

        stringRedisTemplate.opsForValue().set(lastActiveKey, DateUtil.now(), Duration.ofDays(7));

        log.info("用户上线状态写入Redis成功，用户ID：{}，租户ID：{}，会话ID：{}，节点：{}",
                user.getUserId(), user.getTenantId(), user.getSessionId(), user.getNodeId());
    }

    /**
     * 用户下线
     *
     * @param user 会话用户
     */
    @Override
    public void offline(WsSessionUser user) {
        String onlineUserKey = WsRedisKeyConstant.onlineUser(user.getUserId());
        String userSessionsKey = WsRedisKeyConstant.onlineUserSessions(user.getUserId());
        String sessionKey = WsRedisKeyConstant.session(user.getSessionId());
        String tenantOnlineKey = WsRedisKeyConstant.tenantOnline(user.getTenantId());

        stringRedisTemplate.delete(sessionKey);
        stringRedisTemplate.opsForHash().delete(userSessionsKey, user.getSessionId());

        Long remainSessionCount = stringRedisTemplate.opsForHash().size(userSessionsKey);
        if (remainSessionCount == null || remainSessionCount <= 0) {
            stringRedisTemplate.delete(onlineUserKey);
            stringRedisTemplate.delete(userSessionsKey);
            stringRedisTemplate.opsForSet().remove(tenantOnlineKey, Convert.toStr(user.getUserId()));

            log.info("用户全部会话已下线，用户ID：{}，租户ID：{}", user.getUserId(), user.getTenantId());
            return;
        }

        stringRedisTemplate.expire(onlineUserKey, Duration.ofSeconds(properties.getOnlineExpireSeconds()));
        stringRedisTemplate.expire(userSessionsKey, Duration.ofSeconds(properties.getOnlineExpireSeconds()));

        log.info("用户单个会话下线，用户ID：{}，会话ID：{}，剩余会话数：{}",
                user.getUserId(), user.getSessionId(), remainSessionCount);
    }

    /**
     * 刷新心跳
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     */
    @Override
    public void refreshHeartbeat(Long userId, String sessionId) {
        String sessionKey = WsRedisKeyConstant.session(sessionId);
        String sessionJson = stringRedisTemplate.opsForValue().get(sessionKey);

        if (!JSONUtil.isTypeJSON(sessionJson)) {
            log.warn("WebSocket心跳刷新失败：会话不存在，用户ID：{}，会话ID：{}", userId, sessionId);
            return;
        }

        WsSessionUser user = JSONUtil.toBean(sessionJson, WsSessionUser.class);
        user.setLastActiveTime(DateUtil.now());

        Duration heartbeatExpire = Duration.ofSeconds(properties.getHeartbeatExpireSeconds());

        stringRedisTemplate.opsForValue().set(sessionKey, JSONUtil.toJsonStr(user), heartbeatExpire);
        stringRedisTemplate.opsForValue().set(WsRedisKeyConstant.onlineUser(userId), user.getNodeId(), heartbeatExpire);
        stringRedisTemplate.expire(WsRedisKeyConstant.onlineUserSessions(userId), heartbeatExpire);
        stringRedisTemplate.opsForValue().set(WsRedisKeyConstant.lastActive(userId), DateUtil.now(), Duration.ofDays(7));

        log.debug("WebSocket心跳刷新成功，用户ID：{}，会话ID：{}", userId, sessionId);
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    @Override
    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(WsRedisKeyConstant.onlineUser(userId)));
    }

    /**
     * 查询租户在线用户
     *
     * @param tenantId 租户 ID
     * @return 在线用户 ID 集合
     */
    @Override
    public List<Long> listOnlineUsersByTenant(Long tenantId) {
        Set<String> members = stringRedisTemplate.opsForSet().members(WsRedisKeyConstant.tenantOnline(tenantId));
        if (CollUtil.isEmpty(members)) {
            return Collections.emptyList();
        }

        return members.stream()
                .map(Convert::toLong)
                .filter(Objects::nonNull)
                .filter(this::isOnline)
                .toList();
    }
}
```

### 用户下线处理

用户下线处理已经在 `WsOnlineServiceImpl#offline` 中完成。这里需要注意两个关键点：

第一，同一用户可能有多个浏览器页签、多个设备、多个客户端连接，所以不能只要一个连接断开就把用户整体标记为离线。

第二，只有当用户所有会话都断开时，才删除 `ws:online:user:{userId}`，并从租户在线集合中移除。

下线判断逻辑如下：

```text
当前 session 断开
-> 删除 ws:session:{sessionId}
-> 从 ws:online:user:sessions:{userId} 删除当前 sessionId
-> 判断用户是否还有其他 session
-> 有其他 session：保留用户在线状态
-> 没有其他 session：删除用户在线状态，移出租户在线集合
```

### 在线状态查询

在线状态查询通常用于后台页面展示、推送前预判断、客服坐席在线判断等场景。这里提供两个接口：查询单个用户是否在线，查询某个租户下的在线用户。

文件位置：`src/main/java/io/github/atengk/websocket/controller/WsOnlineController.java`

下面控制器提供在线状态查询接口。

```java
package io.github.atengk.websocket.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.websocket.service.WsOnlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WebSocket 在线状态接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ws/online")
public class WsOnlineController {

    private final WsOnlineService wsOnlineService;

    /**
     * 查询用户是否在线
     *
     * @param userId 用户 ID
     * @return 在线状态
     */
    @GetMapping("/{userId}")
    public Dict isOnline(@PathVariable Long userId) {
        return Dict.create()
                .set("userId", userId)
                .set("online", wsOnlineService.isOnline(userId));
    }

    /**
     * 查询租户在线用户
     *
     * @param tenantId 租户 ID
     * @return 在线用户集合
     */
    @GetMapping("/tenant/{tenantId}")
    public Dict listOnlineUsersByTenant(@PathVariable Long tenantId) {
        List<Long> userIds = wsOnlineService.listOnlineUsersByTenant(tenantId);
        return Dict.create()
                .set("tenantId", tenantId)
                .set("onlineCount", userIds.size())
                .set("userIds", userIds);
    }
}
```

查询单个用户在线状态：

```bash
curl -X GET "http://127.0.0.1:8080/api/ws/online/10001"
```

返回示例：

```json
{
  "userId": 10001,
  "online": true
}
```

查询租户在线用户：

```bash
curl -X GET "http://127.0.0.1:8080/api/ws/online/tenant/1"
```

返回示例：

```json
{
  "tenantId": 1,
  "onlineCount": 2,
  "userIds": [10001, 10002]
}
```

### 多端在线处理

本案例默认支持同一用户多端在线。多端包括多个浏览器页签、PC 端、App 端、小程序端等。

多端在线的核心数据结构如下：

```text
本地内存：
sessionMap:
  sessionId -> WebSocketSession

userSessionMap:
  userId -> Set<sessionId>

Redis：
ws:online:user:{userId}
  value: nodeId

ws:online:user:sessions:{userId}
  hash:
    sessionId1 -> node-001
    sessionId2 -> node-001
    sessionId3 -> node-002
```

多端推送策略有两种：

| 策略     | 说明                                                   |
| -------- | ------------------------------------------------------ |
| 全端推送 | 用户所有在线端都收到消息，适合通知、公告、订单状态     |
| 单端推送 | 只推送到指定端，适合客服会话、设备控制、强业务状态同步 |

当前代码中的 `sendToLocalUser` 使用的是全端推送策略，会遍历当前节点中该用户的所有 `sessionId` 并逐个发送。

如果要实现单端推送，可以在 `WsSessionUser` 中增加 `clientType` 字段：

```java
/**
 * 客户端类型：WEB、APP、MINI_PROGRAM
 */
private String clientType;
```

连接时传入客户端类型：

```text
ws://127.0.0.1:8080/ws/push?satoken=你的Token&tenantId=1&clientType=WEB
```

然后在本地会话映射中增加一层维度：

```text
userId -> clientType -> Set<sessionId>
```

普通后台通知场景下，建议默认使用全端推送；如果是强交互业务，再按客户端类型或设备 ID 精细化推送。

## 实时消息推送

实时推送的核心是把业务系统产生的事件转换成统一消息模型，然后根据接收范围选择推送策略。该场景对应原 README 中「按用户 / 角色 / 租户推送消息、离线后保存离线消息、多节点推送」这些核心要求。

### 消息模型设计

消息模型分三类：接口入参 DTO、数据库消息实体、Redis 跨节点广播消息。接口层负责接收业务请求，数据库实体负责保存可追踪消息，Redis 广播模型负责在多个服务节点之间转发。

文件位置：`src/main/java/io/github/atengk/websocket/enums/WsPushStatusEnum.java`

下面枚举用于维护消息推送状态。

```java
package io.github.atengk.websocket.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * WebSocket 推送状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum WsPushStatusEnum {

    WAITING(0, "待推送"),
    PUSHED(1, "已推送"),
    FAILED(2, "推送失败");

    private final Integer code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/websocket/enums/WsAckStatusEnum.java`

下面枚举用于维护客户端 ACK 确认状态。

```java
package io.github.atengk.websocket.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * WebSocket 消息确认状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum WsAckStatusEnum {

    UN_CONFIRMED(0, "未确认"),
    CONFIRMED(1, "已确认");

    private final Integer code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/websocket/entity/WsMessage.java`

下面实体类对应前面创建的 `ws_message` 表，用于保存在线消息、离线消息和待确认消息。

```java
package io.github.atengk.websocket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * WebSocket 推送消息实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("ws_message")
public class WsMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long receiverId;

    private String messageType;

    private String title;

    private String content;

    private String bizId;

    private String bizType;

    private Integer pushStatus;

    private Integer ackStatus;

    private Integer retryCount;

    private Date lastPushTime;

    private Date ackTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/websocket/dto/WsMessageSendDTO.java`

下面 DTO 用于接收业务系统的推送请求，支持单用户、批量用户、角色、租户四类推送。

```java
package io.github.atengk.websocket.dto;

import lombok.Data;

import java.util.List;

/**
 * WebSocket 消息发送请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class WsMessageSendDTO {

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 接收用户 ID，单用户推送使用
     */
    private Long receiverId;

    /**
     * 接收用户 ID 集合，批量推送使用
     */
    private List<Long> receiverIds;

    /**
     * 角色 ID，按角色推送使用
     */
    private Long roleId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 业务 ID
     */
    private String bizId;

    /**
     * 业务类型
     */
    private String bizType;
}
```

文件位置：`src/main/java/io/github/atengk/websocket/model/WsPushMessage.java`

下面模型是真正发送给客户端的消息体。

```java
package io.github.atengk.websocket.model;

import lombok.Builder;
import lombok.Data;

/**
 * WebSocket 客户端推送消息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class WsPushMessage {

    /**
     * 消息 ID
     */
    private Long messageId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 业务 ID
     */
    private String bizId;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 服务端发送时间
     */
    private String sendTime;
}
```

文件位置：`src/main/java/io/github/atengk/websocket/model/WsPubSubMessage.java`

下面模型用于 Redis 发布订阅，多个应用节点收到后再判断是否由自己负责发送。

```java
package io.github.atengk.websocket.model;

import lombok.Builder;
import lombok.Data;

/**
 * WebSocket Redis 跨节点广播消息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class WsPubSubMessage {

    /**
     * 目标节点 ID
     */
    private String targetNodeId;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 接收用户 ID
     */
    private Long receiverId;

    /**
     * 推送消息内容
     */
    private WsPushMessage message;
}
```

文件位置：`src/main/java/io/github/atengk/websocket/mapper/WsMessageMapper.java`

下面 Mapper 用于消息表的基础 CRUD。

```java
package io.github.atengk.websocket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.websocket.entity.WsMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * WebSocket 推送消息 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface WsMessageMapper extends BaseMapper<WsMessage> {
}
```

### 单用户推送

单用户推送是最基础的推送能力。流程是先落库生成消息 ID，再判断用户是否在线；如果在线，则通过 Redis 发布订阅广播到目标节点；如果离线，则保留为待推送状态，等用户下次上线补发。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsMessageService.java`

下面接口定义单用户、批量用户、角色和租户推送方法。

```java
package io.github.atengk.websocket.service;

import io.github.atengk.websocket.dto.WsMessageSendDTO;

/**
 * WebSocket 消息推送服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface WsMessageService {

    Long sendToUser(WsMessageSendDTO dto);

    Integer sendToUsers(WsMessageSendDTO dto);

    Integer sendToRole(WsMessageSendDTO dto);

    Integer sendToTenant(WsMessageSendDTO dto);
}
```

文件位置：`src/main/java/io/github/atengk/websocket/service/WsReceiverQueryService.java`

角色和租户推送依赖用户体系，这里先定义一个接收人查询接口。实际项目中可以对接用户中心、权限系统或组织架构服务。

```java
package io.github.atengk.websocket.service;

import java.util.List;

/**
 * WebSocket 接收人查询服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface WsReceiverQueryService {

    List<Long> listUserIdsByRole(Long tenantId, Long roleId);

    List<Long> listUserIdsByTenant(Long tenantId);
}
```

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsMessageServiceImpl.java`

下面实现类完成消息落库、在线判断、Redis 广播和离线保留。

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.websocket.config.WebSocketPushProperties;
import io.github.atengk.websocket.dto.WsMessageSendDTO;
import io.github.atengk.websocket.entity.WsMessage;
import io.github.atengk.websocket.enums.WsAckStatusEnum;
import io.github.atengk.websocket.enums.WsPushStatusEnum;
import io.github.atengk.websocket.mapper.WsMessageMapper;
import io.github.atengk.websocket.model.WsPubSubMessage;
import io.github.atengk.websocket.model.WsPushMessage;
import io.github.atengk.websocket.service.WsMessageService;
import io.github.atengk.websocket.service.WsOnlineService;
import io.github.atengk.websocket.service.WsReceiverQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * WebSocket 消息推送服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsMessageServiceImpl extends ServiceImpl<WsMessageMapper, WsMessage> implements WsMessageService {

    private final WsOnlineService wsOnlineService;
    private final WsReceiverQueryService wsReceiverQueryService;
    private final StringRedisTemplate stringRedisTemplate;
    private final WebSocketPushProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendToUser(WsMessageSendDTO dto) {
        checkBaseParam(dto);
        Assert.notNull(dto.getReceiverId(), "接收用户不能为空");

        WsMessage message = createMessage(dto, dto.getReceiverId());
        save(message);

        publishIfOnline(message);

        log.info("单用户消息推送完成，消息ID：{}，接收用户：{}", message.getId(), dto.getReceiverId());
        return message.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer sendToUsers(WsMessageSendDTO dto) {
        checkBaseParam(dto);
        Assert.notEmpty(dto.getReceiverIds(), "接收用户集合不能为空");

        List<Long> receiverIds = dto.getReceiverIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (CollUtil.isEmpty(receiverIds)) {
            log.warn("批量推送终止：接收用户集合为空");
            return 0;
        }

        receiverIds.forEach(receiverId -> {
            WsMessage message = createMessage(dto, receiverId);
            save(message);
            publishIfOnline(message);
        });

        log.info("批量用户消息推送完成，租户ID：{}，用户数量：{}", dto.getTenantId(), receiverIds.size());
        return receiverIds.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer sendToRole(WsMessageSendDTO dto) {
        checkBaseParam(dto);
        Assert.notNull(dto.getRoleId(), "角色ID不能为空");

        List<Long> receiverIds = wsReceiverQueryService.listUserIdsByRole(dto.getTenantId(), dto.getRoleId());
        if (CollUtil.isEmpty(receiverIds)) {
            log.info("角色推送无接收用户，租户ID：{}，角色ID：{}", dto.getTenantId(), dto.getRoleId());
            return 0;
        }

        dto.setReceiverIds(receiverIds);
        Integer count = sendToUsers(dto);

        log.info("角色消息推送完成，租户ID：{}，角色ID：{}，用户数量：{}", dto.getTenantId(), dto.getRoleId(), count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer sendToTenant(WsMessageSendDTO dto) {
        checkBaseParam(dto);

        List<Long> receiverIds = wsReceiverQueryService.listUserIdsByTenant(dto.getTenantId());
        if (CollUtil.isEmpty(receiverIds)) {
            log.info("租户推送无接收用户，租户ID：{}", dto.getTenantId());
            return 0;
        }

        dto.setReceiverIds(receiverIds);
        Integer count = sendToUsers(dto);

        log.info("租户消息推送完成，租户ID：{}，用户数量：{}", dto.getTenantId(), count);
        return count;
    }

    private void publishIfOnline(WsMessage message) {
        if (!wsOnlineService.isOnline(message.getReceiverId())) {
            log.info("用户不在线，消息保留为离线消息，消息ID：{}，用户ID：{}", message.getId(), message.getReceiverId());
            return;
        }

        List<String> nodeIds = wsOnlineService.listUserOnlineNodes(message.getReceiverId());
        if (CollUtil.isEmpty(nodeIds)) {
            log.info("用户在线状态存在但节点路由为空，消息ID：{}，用户ID：{}", message.getId(), message.getReceiverId());
            return;
        }

        WsPushMessage pushMessage = WsPushMessage.builder()
                .messageId(message.getId())
                .messageType(message.getMessageType())
                .title(message.getTitle())
                .content(message.getContent())
                .bizId(message.getBizId())
                .bizType(message.getBizType())
                .sendTime(DateUtil.now())
                .build();

        for (String nodeId : nodeIds) {
            WsPubSubMessage pubSubMessage = WsPubSubMessage.builder()
                    .targetNodeId(nodeId)
                    .tenantId(message.getTenantId())
                    .receiverId(message.getReceiverId())
                    .message(pushMessage)
                    .build();

            stringRedisTemplate.convertAndSend(properties.getPubsubChannel(), JSONUtil.toJsonStr(pubSubMessage));
        }

        message.setPushStatus(WsPushStatusEnum.PUSHED.getCode());
        message.setLastPushTime(new Date());
        updateById(message);

        log.info("在线消息已发布到Redis频道，消息ID：{}，用户ID：{}，目标节点：{}",
                message.getId(), message.getReceiverId(), nodeIds);
    }

    private WsMessage createMessage(WsMessageSendDTO dto, Long receiverId) {
        Date now = new Date();

        WsMessage message = new WsMessage();
        message.setTenantId(dto.getTenantId());
        message.setReceiverId(receiverId);
        message.setMessageType(dto.getMessageType());
        message.setTitle(dto.getTitle());
        message.setContent(dto.getContent());
        message.setBizId(dto.getBizId());
        message.setBizType(dto.getBizType());
        message.setPushStatus(WsPushStatusEnum.WAITING.getCode());
        message.setAckStatus(WsAckStatusEnum.UN_CONFIRMED.getCode());
        message.setRetryCount(0);
        message.setCreateTime(now);
        message.setUpdateTime(now);
        message.setDeleted(0);
        return message;
    }

    private void checkBaseParam(WsMessageSendDTO dto) {
        Assert.notNull(dto, "请求参数不能为空");
        Assert.notNull(dto.getTenantId(), "租户ID不能为空");
        Assert.isTrue(StrUtil.isNotBlank(dto.getMessageType()), "消息类型不能为空");
        Assert.isTrue(StrUtil.isNotBlank(dto.getTitle()), "消息标题不能为空");
        Assert.isTrue(StrUtil.isNotBlank(dto.getContent()), "消息内容不能为空");
    }
}
```

上面代码里调用了 `wsOnlineService.listUserOnlineNodes`。需要在上一节的在线状态服务中补充这个方法。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsOnlineService.java`

在接口中增加方法：

```java
List<String> listUserOnlineNodes(Long userId);
```

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsOnlineServiceImpl.java`

在实现类中增加方法：

```java
@Override
public List<String> listUserOnlineNodes(Long userId) {
    String userSessionsKey = WsRedisKeyConstant.onlineUserSessions(userId);
    List<Object> values = stringRedisTemplate.opsForHash().values(userSessionsKey);
    if (CollUtil.isEmpty(values)) {
        return Collections.emptyList();
    }

    return values.stream()
            .map(Convert::toStr)
            .filter(StrUtil::isNotBlank)
            .distinct()
            .toList();
}
```

### 批量用户推送

批量用户推送本质是循环创建多条用户消息，每个用户一条消息记录。这样做的好处是每个用户都有独立的 ACK 状态、推送状态和重试次数。

文件位置：`src/main/java/io/github/atengk/websocket/controller/WsMessageController.java`

下面控制器提供四类推送入口，业务系统可以直接调用。

```java
package io.github.atengk.websocket.controller;

import cn.hutool.core.lang.Dict;
import io.github.atengk.websocket.dto.WsMessageSendDTO;
import io.github.atengk.websocket.service.WsMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * WebSocket 消息推送接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ws/message")
public class WsMessageController {

    private final WsMessageService wsMessageService;

    @PostMapping("/send/user")
    public Dict sendToUser(@RequestBody WsMessageSendDTO dto) {
        Long messageId = wsMessageService.sendToUser(dto);
        return Dict.create()
                .set("messageId", messageId)
                .set("success", true);
    }

    @PostMapping("/send/users")
    public Dict sendToUsers(@RequestBody WsMessageSendDTO dto) {
        Integer count = wsMessageService.sendToUsers(dto);
        return Dict.create()
                .set("count", count)
                .set("success", true);
    }

    @PostMapping("/send/role")
    public Dict sendToRole(@RequestBody WsMessageSendDTO dto) {
        Integer count = wsMessageService.sendToRole(dto);
        return Dict.create()
                .set("count", count)
                .set("success", true);
    }

    @PostMapping("/send/tenant")
    public Dict sendToTenant(@RequestBody WsMessageSendDTO dto) {
        Integer count = wsMessageService.sendToTenant(dto);
        return Dict.create()
                .set("count", count)
                .set("success", true);
    }
}
```

批量推送请求示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/ws/message/send/users" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "receiverIds": [10001, 10002, 10003],
    "messageType": "WORK_ORDER",
    "title": "工单状态变更",
    "content": "你有一个工单状态已更新，请及时处理",
    "bizId": "WO202605150001",
    "bizType": "WORK_ORDER"
  }'
```

返回示例：

```json
{
  "count": 3,
  "success": true
}
```

### 按角色推送

按角色推送适合审批通知、工单分派、系统告警等场景，例如给所有管理员、客服、财务人员推送消息。

角色推送不应该在 WebSocket 模块里直接写死用户表查询逻辑。更好的做法是让 WebSocket 模块依赖一个 `WsReceiverQueryService` 接口，具体用户和角色关系由业务系统实现。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/DemoWsReceiverQueryServiceImpl.java`

下面示例实现仅用于演示，实际项目中需要替换成真实用户中心查询。

```java
package io.github.atengk.websocket.service.impl;

import cn.hutool.core.collection.ListUtil;
import io.github.atengk.websocket.service.WsReceiverQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WebSocket 接收人查询演示实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class DemoWsReceiverQueryServiceImpl implements WsReceiverQueryService {

    @Override
    public List<Long> listUserIdsByRole(Long tenantId, Long roleId) {
        log.info("查询角色下用户列表，租户ID：{}，角色ID：{}", tenantId, roleId);

        // 演示数据：实际项目中应查询用户角色关系表或调用用户中心
        if (roleId == 1L) {
            return ListUtil.of(10001L, 10002L);
        }
        if (roleId == 2L) {
            return ListUtil.of(10003L, 10004L);
        }
        return ListUtil.empty();
    }

    @Override
    public List<Long> listUserIdsByTenant(Long tenantId) {
        log.info("查询租户下用户列表，租户ID：{}", tenantId);

        // 演示数据：实际项目中应查询租户用户关系表或组织架构服务
        if (tenantId == 1L) {
            return ListUtil.of(10001L, 10002L, 10003L, 10004L);
        }
        return ListUtil.empty();
    }
}
```

按角色推送请求示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/ws/message/send/role" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "roleId": 1,
    "messageType": "APPROVAL_NOTICE",
    "title": "审批待处理",
    "content": "你有新的审批任务需要处理",
    "bizId": "AP202605150001",
    "bizType": "APPROVAL"
  }'
```

返回示例：

```json
{
  "count": 2,
  "success": true
}
```

### 按租户推送

按租户推送适合系统公告、租户级告警、租户配置变更通知等场景。它的关键点是必须先限定租户，再查询该租户下的用户，避免跨租户误推。

按租户推送请求示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/ws/message/send/tenant" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "messageType": "SYSTEM_NOTICE",
    "title": "系统维护通知",
    "content": "系统将在今晚 23:00 进行维护，请提前保存数据",
    "bizId": "NOTICE202605150001",
    "bizType": "SYSTEM_NOTICE"
  }'
```

返回示例：

```json
{
  "count": 4,
  "success": true
}
```

租户推送的实现逻辑如下：

```text
传入 tenantId
-> 查询当前租户下所有用户
-> 为每个用户创建独立消息记录
-> 判断每个用户是否在线
-> 在线则实时推送
-> 离线则保留为离线消息
```

这种方式虽然会产生多条消息记录，但后续查询未读、ACK 确认、失败补偿都会更简单。

## 多节点推送

单机 WebSocket 只需要从内存中取 `WebSocketSession` 发送消息。但生产环境通常会部署多个应用实例，用户 A 可能连接在 `node-001`，业务请求可能打到 `node-002`。因此需要通过 Redis 发布订阅把消息转发到用户所在节点。

### Redis 发布订阅

Redis 发布订阅用于解决跨节点消息转发问题。发送消息的节点只负责发布消息，所有节点都监听同一个频道，收到消息后判断 `targetNodeId` 是否等于当前节点 ID，匹配才执行本地发送。

文件位置：`src/main/java/io/github/atengk/websocket/config/RedisSubscriberConfig.java`

下面配置类注册 Redis 消息监听容器。

```java
package io.github.atengk.websocket.config;

import io.github.atengk.websocket.listener.WsRedisMessageSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis 发布订阅配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@RequiredArgsConstructor
public class RedisSubscriberConfig {

    private final WebSocketPushProperties properties;
    private final WsRedisMessageSubscriber wsRedisMessageSubscriber;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        MessageListener listener = wsRedisMessageSubscriber;
        container.addMessageListener(listener, new ChannelTopic(properties.getPubsubChannel()));

        return container;
    }
}
```

文件位置：`src/main/java/io/github/atengk/websocket/listener/WsRedisMessageSubscriber.java`

下面监听器消费 Redis 频道消息，并将目标节点匹配的消息转交给本地连接发送。

```java
package io.github.atengk.websocket.listener;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.websocket.config.WebSocketPushProperties;
import io.github.atengk.websocket.model.WsPubSubMessage;
import io.github.atengk.websocket.service.WsConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * WebSocket Redis 消息订阅器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsRedisMessageSubscriber implements MessageListener {

    private final WebSocketPushProperties properties;
    private final WsConnectionService wsConnectionService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        if (!JSONUtil.isTypeJSON(body)) {
            log.warn("Redis推送消息格式错误，内容：{}", body);
            return;
        }

        WsPubSubMessage pubSubMessage = JSONUtil.toBean(body, WsPubSubMessage.class);
        if (!StrUtil.equals(pubSubMessage.getTargetNodeId(), properties.getNodeId())) {
            return;
        }

        String pushContent = JSONUtil.toJsonStr(pubSubMessage.getMessage());
        wsConnectionService.sendToLocalUser(pubSubMessage.getReceiverId(), pushContent);

        log.info("Redis订阅消息已转发到本地WebSocket，节点：{}，用户ID：{}，消息ID：{}",
                properties.getNodeId(),
                pubSubMessage.getReceiverId(),
                pubSubMessage.getMessage().getMessageId());
    }
}
```

### 本地连接路由

本地连接路由由 `WsConnectionServiceImpl` 维护。核心结构是两个 Map：

```text
sessionMap:
  sessionId -> WebSocketSession

userSessionMap:
  userId -> Set<sessionId>
```

当 Redis 订阅器收到属于当前节点的消息时，调用：

```java
wsConnectionService.sendToLocalUser(pubSubMessage.getReceiverId(), pushContent);
```

然后 `sendToLocalUser` 会完成以下动作：

```text
根据 userId 查找当前节点 sessionId 集合
-> 遍历所有 sessionId
-> 判断 WebSocketSession 是否存在
-> 判断连接是否打开
-> session.sendMessage()
-> 发送失败则记录日志
```

这里保持全端推送策略，也就是同一个用户在当前节点打开多个页面时，所有页面都会收到消息。后台通知、订单状态、审批提醒这类消息通常应该使用全端推送。

### 跨节点消息转发

跨节点推送依赖前面 Redis 中保存的用户会话路由：

```text
ws:online:user:sessions:{userId}
  sessionId1 -> node-001
  sessionId2 -> node-002
```

发送消息时，服务端先查出用户所在节点，再向 Redis 频道发布一条或多条消息：

```text
业务请求到达 node-002
-> 查询用户 10001 的在线节点
-> 发现用户连接在 node-001
-> node-002 发布 Redis 消息，targetNodeId=node-001
-> node-001 订阅到消息
-> node-001 从本地 userSessionMap 找到用户连接
-> node-001 调用 WebSocketSession.sendMessage()
-> 客户端收到消息
```

多节点验证可以启动两个实例：

```bash
# 启动第一个节点
java -jar websocket-push-demo.jar \
  --server.port=8080 \
  --websocket.push.node-id=node-001

# 启动第二个节点
java -jar websocket-push-demo.jar \
  --server.port=8081 \
  --websocket.push.node-id=node-002
```

验证步骤如下：

```text
1. 客户端连接 node-001：
   ws://127.0.0.1:8080/ws/push?satoken=用户Token&tenantId=1

2. 业务接口请求 node-002：
   POST http://127.0.0.1:8081/api/ws/message/send/user

3. node-002 查询 Redis，发现用户连接在 node-001

4. node-002 发布 Redis 消息到 ws:pubsub:push

5. node-001 订阅到消息并完成 WebSocket 推送

6. 客户端收到消息
```

单用户跨节点推送请求示例：

```bash
curl -X POST "http://127.0.0.1:8081/api/ws/message/send/user" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "receiverId": 10001,
    "messageType": "ORDER_STATUS",
    "title": "订单状态更新",
    "content": "你的订单已发货",
    "bizId": "ORDER202605150001",
    "bizType": "ORDER"
  }'
```

客户端收到的 WebSocket 消息示例：

```json
{
  "messageId": 1912746000000000001,
  "messageType": "ORDER_STATUS",
  "title": "订单状态更新",
  "content": "你的订单已发货",
  "bizId": "ORDER202605150001",
  "bizType": "ORDER",
  "sendTime": "2026-05-15 10:30:30"
}
```

当前方案的关键点是：应用节点之间不直接通信，所有跨节点推送都通过 Redis Pub/Sub 完成；本地节点只负责维护自己进程内的真实 WebSocket 连接。

## 离线消息处理

离线消息处理对应原场景中的「用户离线后保存离线消息、用户上线后补发消息、消息确认」要求。核心思路是：消息先入库，再判断用户是否在线；在线则实时推送，离线则保留为未确认消息，用户下次上线后补发。

### 离线消息入库

前面 `sendToUser` 已经采用了「先入库，后推送」的方式，所以离线消息不需要额外单独建表。

当前逻辑如下：

```text
业务系统调用发送接口
-> 创建 ws_message 记录
-> push_status = 0 待推送
-> ack_status = 0 未确认
-> 判断用户是否在线
-> 在线：发布 Redis 推送消息，并更新 push_status = 1
-> 离线：不推送，保留数据库记录
-> 用户上线后补发 ack_status = 0 的消息
```

也就是说，满足下面条件的消息都可以认为是待补发消息：

```text
receiver_id = 当前用户
tenant_id = 当前租户
ack_status = 0
deleted = 0
```

其中 `push_status` 不作为补发的唯一判断条件，因为有些消息可能已经推送过，但客户端没有 ACK；这类消息仍然需要补发。

### 用户上线补发

用户上线补发应该在 WebSocket 连接注册成功后执行。服务端查询当前用户未确认消息，然后通过当前 WebSocket 连接重新推送给客户端。

先扩展消息服务接口。

文件位置：`src/main/java/io/github/atengk/websocket/service/WsMessageService.java`

在原接口中增加下面方法：

```java
package io.github.atengk.websocket.service;

import io.github.atengk.websocket.dto.WsMessageAckDTO;
import io.github.atengk.websocket.dto.WsMessageSendDTO;
import io.github.atengk.websocket.model.WsPushMessage;

import java.util.List;

/**
 * WebSocket 消息推送服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface WsMessageService {

    Long sendToUser(WsMessageSendDTO dto);

    Integer sendToUsers(WsMessageSendDTO dto);

    Integer sendToRole(WsMessageSendDTO dto);

    Integer sendToTenant(WsMessageSendDTO dto);

    /**
     * 查询用户未确认消息
     *
     * @param tenantId   租户 ID
     * @param receiverId 接收用户 ID
     * @param limit      查询数量
     * @return 未确认消息集合
     */
    List<WsPushMessage> listUnconfirmedMessages(Long tenantId, Long receiverId, Integer limit);

    /**
     * 用户上线后补发未确认消息
     *
     * @param tenantId   租户 ID
     * @param receiverId 接收用户 ID
     */
    void resendUnconfirmedMessages(Long tenantId, Long receiverId);

    /**
     * 确认消息已读或已收到
     *
     * @param dto 确认请求
     * @return 是否确认成功
     */
    Boolean ackMessage(WsMessageAckDTO dto);
}
```

文件位置：`src/main/java/io/github/atengk/websocket/dto/WsMessageAckDTO.java`

下面 DTO 用于客户端提交 ACK 确认。

```java
package io.github.atengk.websocket.dto;

import lombok.Data;

/**
 * WebSocket 消息确认请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class WsMessageAckDTO {

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 消息 ID
     */
    private Long messageId;

    /**
     * 接收用户 ID
     */
    private Long receiverId;
}
```

文件位置：`src/main/java/io/github/atengk/websocket/constant/WsRedisKeyConstant.java`

在 Redis Key 常量类中增加 ACK 锁 Key。

```java
/**
 * 消息确认锁 Key
 *
 * @param messageId 消息 ID
 * @param userId    用户 ID
 * @return Redis Key
 */
public static String ackLock(Long messageId, Long userId) {
    return "ws:lock:ack:" + messageId + ":" + userId;
}
```

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsMessageServiceImpl.java`

在 `WsMessageServiceImpl` 中增加 `RedissonClient` 依赖。

```java
private final org.redisson.api.RedissonClient redissonClient;
```

同时把上节 `publishIfOnline` 中组装 `WsPushMessage` 的逻辑抽成公共私有方法，后续补发和接口查询都会复用。

```java
private WsPushMessage toPushMessage(WsMessage message) {
    return WsPushMessage.builder()
            .messageId(message.getId())
            .messageType(message.getMessageType())
            .title(message.getTitle())
            .content(message.getContent())
            .bizId(message.getBizId())
            .bizType(message.getBizType())
            .sendTime(DateUtil.now())
            .build();
}
```

然后把 `publishIfOnline` 中原来的 `WsPushMessage.builder()` 替换成：

```java
WsPushMessage pushMessage = toPushMessage(message);
```

继续在 `WsMessageServiceImpl` 中增加下面方法，实现未确认消息查询和上线补发。

```java
@Override
public List<WsPushMessage> listUnconfirmedMessages(Long tenantId, Long receiverId, Integer limit) {
    Assert.notNull(tenantId, "租户ID不能为空");
    Assert.notNull(receiverId, "接收用户不能为空");

    int queryLimit = limit == null || limit <= 0 ? properties.getOfflineLimit() : limit;

    List<WsMessage> messages = lambdaQuery()
            .eq(WsMessage::getTenantId, tenantId)
            .eq(WsMessage::getReceiverId, receiverId)
            .eq(WsMessage::getAckStatus, WsAckStatusEnum.UN_CONFIRMED.getCode())
            .orderByAsc(WsMessage::getCreateTime)
            .last("LIMIT " + queryLimit)
            .list();

    if (CollUtil.isEmpty(messages)) {
        return List.of();
    }

    return messages.stream()
            .map(this::toPushMessage)
            .toList();
}

@Override
@Transactional(rollbackFor = Exception.class)
public void resendUnconfirmedMessages(Long tenantId, Long receiverId) {
    Assert.notNull(tenantId, "租户ID不能为空");
    Assert.notNull(receiverId, "接收用户不能为空");

    List<WsMessage> messages = lambdaQuery()
            .eq(WsMessage::getTenantId, tenantId)
            .eq(WsMessage::getReceiverId, receiverId)
            .eq(WsMessage::getAckStatus, WsAckStatusEnum.UN_CONFIRMED.getCode())
            .orderByAsc(WsMessage::getCreateTime)
            .last("LIMIT " + properties.getOfflineLimit())
            .list();

    if (CollUtil.isEmpty(messages)) {
        log.info("用户上线无待补发消息，租户ID：{}，用户ID：{}", tenantId, receiverId);
        return;
    }

    for (WsMessage message : messages) {
        publishIfOnline(message);
    }

    log.info("用户上线补发消息完成，租户ID：{}，用户ID：{}，补发数量：{}",
            tenantId, receiverId, messages.size());
}
```

最后在 WebSocket 连接建立成功后触发补发。

文件位置：`src/main/java/io/github/atengk/websocket/handler/PushWebSocketHandler.java`

在 `PushWebSocketHandler` 中增加依赖：

```java
private final WsMessageService wsMessageService;
```

并在 `afterConnectionEstablished` 中注册会话后增加下面代码：

```java
wsConnectionService.registerSession(sessionUser, session);

// 用户上线后补发未确认消息
wsMessageService.resendUnconfirmedMessages(tenantId, userId);
```

这样用户重新建立 WebSocket 连接后，会自动收到历史未确认消息。

### 离线消息已读确认

离线消息补发后，客户端需要调用 ACK 接口确认。确认成功后，服务端把 `ack_status` 更新为 `1`，同时记录 `ack_time`。

确认流程如下：

```text
客户端收到 WebSocket 消息
-> 读取 messageId
-> 调用 ACK 接口
-> 服务端校验消息归属
-> 服务端判断是否已确认
-> 未确认则更新 ack_status = 1
-> 已确认则直接返回成功
```

ACK 必须做幂等处理。因为客户端可能重复提交 ACK，请求重试也可能导致重复确认。

## 消息确认机制

消息确认机制用于解决「服务端已经发送，但无法确定客户端是否真正收到」的问题。WebSocket 的 `sendMessage` 成功只能说明服务端写入连接成功，不代表用户页面已经处理完成，所以需要客户端主动 ACK。

### 客户端 ACK

客户端收到消息后，立即调用确认接口。前端可以在消息进入本地通知列表后提交 ACK。

客户端收到的 WebSocket 消息示例：

```json
{
  "messageId": 1912746000000000001,
  "messageType": "ORDER_STATUS",
  "title": "订单状态更新",
  "content": "你的订单已发货",
  "bizId": "ORDER202605150001",
  "bizType": "ORDER",
  "sendTime": "2026-05-15 10:30:30"
}
```

客户端 ACK 请求示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/ws/message/ack" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "messageId": 1912746000000000001,
    "receiverId": 10001
  }'
```

ACK 返回示例：

```json
{
  "success": true
}
```

### 服务端确认更新

服务端确认更新要校验三件事：消息是否存在、消息是否属于当前租户、消息接收人是否匹配。校验通过后再更新确认状态。

文件位置：`src/main/java/io/github/atengk/websocket/service/impl/WsMessageServiceImpl.java`

在 `WsMessageServiceImpl` 中增加 ACK 实现。

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Boolean ackMessage(WsMessageAckDTO dto) {
    Assert.notNull(dto, "确认请求不能为空");
    Assert.notNull(dto.getTenantId(), "租户ID不能为空");
    Assert.notNull(dto.getMessageId(), "消息ID不能为空");
    Assert.notNull(dto.getReceiverId(), "接收用户不能为空");

    String lockKey = WsRedisKeyConstant.ackLock(dto.getMessageId(), dto.getReceiverId());
    org.redisson.api.RLock lock = redissonClient.getLock(lockKey);

    boolean locked = false;
    try {
        locked = lock.tryLock(3, 10, java.util.concurrent.TimeUnit.SECONDS);
        if (!locked) {
            log.warn("消息确认失败：获取确认锁超时，消息ID：{}，用户ID：{}",
                    dto.getMessageId(), dto.getReceiverId());
            return false;
        }

        WsMessage message = getById(dto.getMessageId());
        if (message == null) {
            log.warn("消息确认失败：消息不存在，消息ID：{}", dto.getMessageId());
            return false;
        }

        if (!dto.getTenantId().equals(message.getTenantId())) {
            log.warn("消息确认失败：租户不匹配，消息ID：{}，请求租户：{}，消息租户：{}",
                    dto.getMessageId(), dto.getTenantId(), message.getTenantId());
            return false;
        }

        if (!dto.getReceiverId().equals(message.getReceiverId())) {
            log.warn("消息确认失败：接收人不匹配，消息ID：{}，请求用户：{}，消息用户：{}",
                    dto.getMessageId(), dto.getReceiverId(), message.getReceiverId());
            return false;
        }

        if (WsAckStatusEnum.CONFIRMED.getCode().equals(message.getAckStatus())) {
            log.info("消息已确认，重复ACK直接返回成功，消息ID：{}，用户ID：{}",
                    dto.getMessageId(), dto.getReceiverId());
            return true;
        }

        boolean updated = lambdaUpdate()
                .eq(WsMessage::getId, dto.getMessageId())
                .eq(WsMessage::getTenantId, dto.getTenantId())
                .eq(WsMessage::getReceiverId, dto.getReceiverId())
                .eq(WsMessage::getAckStatus, WsAckStatusEnum.UN_CONFIRMED.getCode())
                .set(WsMessage::getAckStatus, WsAckStatusEnum.CONFIRMED.getCode())
                .set(WsMessage::getAckTime, new Date())
                .update();

        log.info("消息确认完成，消息ID：{}，用户ID：{}，结果：{}",
                dto.getMessageId(), dto.getReceiverId(), updated);
        return updated;
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("消息确认异常：线程被中断，消息ID：{}，用户ID：{}",
                dto.getMessageId(), dto.getReceiverId(), e);
        return false;
    } finally {
        if (locked && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

这个 ACK 更新是幂等的，重复调用不会造成状态异常。

### 未确认消息补偿

未确认消息补偿用于处理客户端未 ACK、服务端发送失败、用户短暂断线等情况。简单方案是定时扫描一段时间内未确认且用户在线的消息，再次推送。

需要先开启定时任务。

文件位置：`src/main/java/io/github/atengk/websocket/WebSocketPushApplication.java`

在启动类增加 `@EnableScheduling`。

```java
package io.github.atengk.websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WebSocket 推送服务启动类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@EnableScheduling
@SpringBootApplication
public class WebSocketPushApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebSocketPushApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/websocket/task/WsMessageCompensationTask.java`

下面定时任务每 30 秒扫描一次未确认消息，只补偿当前在线用户的消息，避免对离线用户反复无效推送。

```java
package io.github.atengk.websocket.task;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.websocket.entity.WsMessage;
import io.github.atengk.websocket.enums.WsAckStatusEnum;
import io.github.atengk.websocket.service.WsMessageService;
import io.github.atengk.websocket.service.WsOnlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * WebSocket 未确认消息补偿任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsMessageCompensationTask {

    private final WsMessageService wsMessageService;
    private final WsOnlineService wsOnlineService;

    /**
     * 扫描未确认消息并补偿推送
     */
    @Scheduled(fixedDelay = 30000)
    public void compensateUnconfirmedMessages() {
        if (!(wsMessageService instanceof com.baomidou.mybatisplus.extension.service.IService<?>)) {
            return;
        }

        com.baomidou.mybatisplus.extension.service.IService<WsMessage> service =
                (com.baomidou.mybatisplus.extension.service.IService<WsMessage>) wsMessageService;

        Date beforeTime = new Date(System.currentTimeMillis() - 30000);

        List<WsMessage> messages = service.list(new LambdaQueryWrapper<WsMessage>()
                .eq(WsMessage::getAckStatus, WsAckStatusEnum.UN_CONFIRMED.getCode())
                .lt(WsMessage::getCreateTime, beforeTime)
                .orderByAsc(WsMessage::getCreateTime)
                .last("LIMIT 100"));

        if (CollUtil.isEmpty(messages)) {
            log.debug("未确认消息补偿任务完成：无待补偿消息");
            return;
        }

        int count = 0;
        for (WsMessage message : messages) {
            if (!wsOnlineService.isOnline(message.getReceiverId())) {
                continue;
            }

            wsMessageService.resendUnconfirmedMessages(message.getTenantId(), message.getReceiverId());
            count++;
        }

        log.info("未确认消息补偿任务完成，扫描数量：{}，触发补偿用户次数：{}", messages.size(), count);
    }
}
```

如果项目中已经接入 XXL-JOB，建议把这个补偿逻辑改造成 XXL-JOB 任务，由调度中心控制执行频率和分片参数。

## 接口设计

本节汇总当前案例中的核心接口，包括在线状态、消息发送、离线消息查询和消息确认。

### 在线状态接口

在线状态接口用于查询单个用户是否在线，以及查询租户下当前在线用户。

| 接口                               | 方法 | 说明                 |
| ---------------------------------- | ---- | -------------------- |
| `/api/ws/online/{userId}`          | GET  | 查询单个用户是否在线 |
| `/api/ws/online/tenant/{tenantId}` | GET  | 查询租户在线用户     |

查询用户是否在线：

```bash
curl -X GET "http://127.0.0.1:8080/api/ws/online/10001"
```

返回示例：

```json
{
  "userId": 10001,
  "online": true
}
```

### 消息发送接口

消息发送接口用于业务系统主动触发推送，支持单用户、批量用户、角色和租户四种范围。

| 接口                          | 方法 | 说明         |
| ----------------------------- | ---- | ------------ |
| `/api/ws/message/send/user`   | POST | 单用户推送   |
| `/api/ws/message/send/users`  | POST | 批量用户推送 |
| `/api/ws/message/send/role`   | POST | 按角色推送   |
| `/api/ws/message/send/tenant` | POST | 按租户推送   |

单用户推送：

```bash
curl -X POST "http://127.0.0.1:8080/api/ws/message/send/user" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "receiverId": 10001,
    "messageType": "ORDER_STATUS",
    "title": "订单状态更新",
    "content": "你的订单已发货",
    "bizId": "ORDER202605150001",
    "bizType": "ORDER"
  }'
```

返回示例：

```json
{
  "messageId": 1912746000000000001,
  "success": true
}
```

### 离线消息查询接口

离线消息查询接口用于客户端主动拉取未确认消息。虽然用户上线后服务端会自动补发，但前端页面刷新、网络闪断、WebSocket 重连时，主动拉取接口仍然很有必要。

文件位置：`src/main/java/io/github/atengk/websocket/controller/WsMessageController.java`

在原 `WsMessageController` 中增加下面两个接口。

```java
@GetMapping("/offline")
public Dict listOfflineMessages(@RequestParam Long tenantId,
                                @RequestParam Long receiverId,
                                @RequestParam(required = false) Integer limit) {
    return Dict.create()
            .set("messages", wsMessageService.listUnconfirmedMessages(tenantId, receiverId, limit))
            .set("success", true);
}

@PostMapping("/ack")
public Dict ackMessage(@RequestBody WsMessageAckDTO dto) {
    Boolean success = wsMessageService.ackMessage(dto);
    return Dict.create()
            .set("success", success);
}
```

需要在 `WsMessageController` 顶部补充导入：

```java
import io.github.atengk.websocket.dto.WsMessageAckDTO;
```

查询离线消息：

```bash
curl -X GET "http://127.0.0.1:8080/api/ws/message/offline?tenantId=1&receiverId=10001&limit=20"
```

返回示例：

```json
{
  "messages": [
    {
      "messageId": 1912746000000000001,
      "messageType": "ORDER_STATUS",
      "title": "订单状态更新",
      "content": "你的订单已发货",
      "bizId": "ORDER202605150001",
      "bizType": "ORDER",
      "sendTime": "2026-05-15 10:30:30"
    }
  ],
  "success": true
}
```

### 消息确认接口

消息确认接口用于客户端 ACK。客户端可以在收到 WebSocket 消息后立即确认，也可以在用户打开通知详情后再确认。

| 接口                  | 方法 | 说明                 |
| --------------------- | ---- | -------------------- |
| `/api/ws/message/ack` | POST | 确认消息已收到或已读 |

确认消息：

```bash
curl -X POST "http://127.0.0.1:8080/api/ws/message/ack" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "messageId": 1912746000000000001,
    "receiverId": 10001
  }'
```

返回示例：

```json
{
  "success": true
}
```

接口调用建议：

```text
1. 页面加载后先建立 WebSocket 连接
2. WebSocket 连接成功后，服务端自动补发未确认消息
3. 前端收到消息后写入本地消息列表
4. 前端调用 /api/ws/message/ack 确认
5. 页面刷新或重连后，前端可主动调用 /api/ws/message/offline 兜底拉取
```

至此，离线消息、上线补发、客户端 ACK、服务端确认更新和未确认消息补偿已经形成闭环。

## 功能测试

本节用于验证 WebSocket 连接、在线推送、离线消息、多节点转发和 ACK 确认是否完整闭环。测试内容对应原场景中的连接管理、在线状态、多节点推送、离线消息和消息确认等核心难点。

### 建立 WebSocket 连接

测试前需要先准备一个有效的 Sa-Token 登录 Token。下面示例假设用户 ID 为 `10001`，租户 ID 为 `1`。

推荐先启动基础服务：

```bash
# 启动 Redis
docker start redis-websocket

# 启动 MySQL
docker start mysql-websocket

# 启动后端服务
java -jar websocket-push-demo.jar \
  --server.port=8080 \
  --websocket.push.node-id=node-001
```

可以直接在浏览器控制台中建立 WebSocket 连接。

```javascript
const token = "替换成有效SaToken";
const tenantId = 1;

const ws = new WebSocket(`ws://127.0.0.1:8080/ws/push?satoken=${token}&tenantId=${tenantId}`);

ws.onopen = () => {
  console.log("WebSocket连接成功");

  // 每 30 秒发送一次心跳
  setInterval(() => {
    ws.send(JSON.stringify({ type: "PING" }));
  }, 30000);
};

ws.onmessage = (event) => {
  console.log("收到服务端消息：", JSON.parse(event.data));
};

ws.onclose = (event) => {
  console.log("WebSocket连接关闭：", event);
};

ws.onerror = (event) => {
  console.error("WebSocket连接异常：", event);
};
```

连接成功后，客户端会收到类似响应：

```json
{
  "type": "CONNECTED",
  "sessionId": "1f1c0f53-7f7b-4a0c-8f7a-9c62f0a21e91",
  "serverTime": "2026-05-15 10:30:00"
}
```

检查 Redis 在线状态：

```bash
# 查看用户在线节点
redis-cli GET ws:online:user:10001

# 查看用户多端会话
redis-cli HGETALL ws:online:user:sessions:10001

# 查看租户在线用户集合
redis-cli SMEMBERS ws:online:tenant:1
```

预期结果：

```text
ws:online:user:10001 = node-001
ws:online:user:sessions:10001 中存在当前 sessionId
ws:online:tenant:1 中存在 10001
```

### 推送在线消息

用户保持 WebSocket 在线状态，然后调用单用户推送接口。

```bash
curl -X POST "http://127.0.0.1:8080/api/ws/message/send/user" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "receiverId": 10001,
    "messageType": "ORDER_STATUS",
    "title": "订单状态更新",
    "content": "你的订单已发货",
    "bizId": "ORDER202605150001",
    "bizType": "ORDER"
  }'
```

接口返回示例：

```json
{
  "messageId": 1912746000000000001,
  "success": true
}
```

浏览器控制台应收到消息：

```json
{
  "messageId": 1912746000000000001,
  "messageType": "ORDER_STATUS",
  "title": "订单状态更新",
  "content": "你的订单已发货",
  "bizId": "ORDER202605150001",
  "bizType": "ORDER",
  "sendTime": "2026-05-15 10:30:30"
}
```

检查数据库消息状态：

```sql
SELECT
    id,
    tenant_id,
    receiver_id,
    message_type,
    push_status,
    ack_status,
    last_push_time
FROM ws_message
WHERE receiver_id = 10001
ORDER BY create_time DESC
LIMIT 5;
```

预期结果：

```text
push_status = 1
ack_status = 0
last_push_time 不为空
```

这里 `ack_status = 0` 是正常的，因为客户端收到消息后还没有调用 ACK 接口。

### 推送离线消息

先关闭浏览器中的 WebSocket 连接。

```javascript
ws.close();
```

确认用户已经离线：

```bash
curl -X GET "http://127.0.0.1:8080/api/ws/online/10001"
```

返回示例：

```json
{
  "userId": 10001,
  "online": false
}
```

用户离线后继续发送消息：

```bash
curl -X POST "http://127.0.0.1:8080/api/ws/message/send/user" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "receiverId": 10001,
    "messageType": "WORK_ORDER",
    "title": "工单待处理",
    "content": "你有一个新的工单需要处理",
    "bizId": "WO202605150001",
    "bizType": "WORK_ORDER"
  }'
```

检查数据库：

```sql
SELECT
    id,
    receiver_id,
    title,
    push_status,
    ack_status,
    last_push_time
FROM ws_message
WHERE receiver_id = 10001
ORDER BY create_time DESC
LIMIT 5;
```

预期结果：

```text
push_status = 0
ack_status = 0
last_push_time 为空
```

然后重新建立 WebSocket 连接。连接成功后，服务端会自动查询未确认消息并补发。浏览器控制台应收到刚才离线期间产生的消息。

也可以主动调用离线消息查询接口兜底验证：

```bash
curl -X GET "http://127.0.0.1:8080/api/ws/message/offline?tenantId=1&receiverId=10001&limit=20"
```

返回示例：

```json
{
  "messages": [
    {
      "messageId": 1912746000000000002,
      "messageType": "WORK_ORDER",
      "title": "工单待处理",
      "content": "你有一个新的工单需要处理",
      "bizId": "WO202605150001",
      "bizType": "WORK_ORDER",
      "sendTime": "2026-05-15 10:35:00"
    }
  ],
  "success": true
}
```

### 多节点推送验证

多节点推送用于验证「用户连接在一个节点，业务接口请求打到另一个节点」的情况。

启动两个服务实例：

```bash
# 节点 1
java -jar websocket-push-demo.jar \
  --server.port=8080 \
  --websocket.push.node-id=node-001

# 节点 2
java -jar websocket-push-demo.jar \
  --server.port=8081 \
  --websocket.push.node-id=node-002
```

客户端连接到 `node-001`：

```javascript
const token = "替换成有效SaToken";

const ws = new WebSocket(`ws://127.0.0.1:8080/ws/push?satoken=${token}&tenantId=1`);

ws.onmessage = (event) => {
  console.log("node-001客户端收到消息：", JSON.parse(event.data));
};
```

确认 Redis 中用户连接节点为 `node-001`：

```bash
redis-cli HGETALL ws:online:user:sessions:10001
```

预期类似：

```text
1) "1f1c0f53-7f7b-4a0c-8f7a-9c62f0a21e91"
2) "node-001"
```

然后请求 `node-002` 的推送接口：

```bash
curl -X POST "http://127.0.0.1:8081/api/ws/message/send/user" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "receiverId": 10001,
    "messageType": "SYSTEM_NOTICE",
    "title": "跨节点推送测试",
    "content": "这条消息从 node-002 发起，由 node-001 WebSocket 连接接收",
    "bizId": "NOTICE202605150001",
    "bizType": "SYSTEM_NOTICE"
  }'
```

预期结果：

```text
1. node-002 接收 HTTP 推送请求
2. node-002 查询 Redis，发现用户连接在 node-001
3. node-002 发布消息到 Redis 频道 ws:pubsub:push
4. node-001 订阅到消息
5. node-001 找到本地 WebSocketSession
6. 客户端收到消息
```

如果客户端收不到消息，优先检查：

```bash
# 检查用户是否在线
redis-cli GET ws:online:user:10001

# 检查用户会话所在节点
redis-cli HGETALL ws:online:user:sessions:10001

# 检查两个服务实例是否使用同一个 Redis
redis-cli CLIENT LIST
```

### 消息确认验证

客户端收到消息后，需要调用 ACK 接口确认。

```bash
curl -X POST "http://127.0.0.1:8080/api/ws/message/ack" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "messageId": 1912746000000000001,
    "receiverId": 10001
  }'
```

返回示例：

```json
{
  "success": true
}
```

检查数据库确认状态：

```sql
SELECT
    id,
    receiver_id,
    push_status,
    ack_status,
    ack_time
FROM ws_message
WHERE id = 1912746000000000001;
```

预期结果：

```text
ack_status = 1
ack_time 不为空
```

重复调用 ACK 接口，仍然应该返回成功，且数据库状态不应异常变化：

```bash
curl -X POST "http://127.0.0.1:8080/api/ws/message/ack" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "messageId": 1912746000000000001,
    "receiverId": 10001
  }'
```

预期结果：

```json
{
  "success": true
}
```

再次查询离线消息：

```bash
curl -X GET "http://127.0.0.1:8080/api/ws/message/offline?tenantId=1&receiverId=10001&limit=20"
```

如果该用户所有消息都已确认，预期返回：

```json
{
  "messages": [],
  "success": true
}
```

## 核心问题总结

本案例的重点不是单纯建立一个 WebSocket 连接，而是围绕生产环境常见问题形成完整链路：连接怎么管、在线状态怎么维护、多节点怎么推、离线消息怎么补、权限边界怎么控制。

### 连接管理

WebSocket 连接不能只保存一个简单的 `userId -> session` 映射。实际项目中，一个用户可能同时打开多个浏览器页签，也可能同时登录 PC、App、小程序。

本案例使用两层本地缓存：

```text
sessionMap:
  sessionId -> WebSocketSession

userSessionMap:
  userId -> Set<sessionId>
```

这样可以支持同一用户多端在线，也可以在连接关闭时精确删除某一个 `sessionId`，避免误踢其他在线端。

连接管理需要注意：

```text
1. WebSocketSession 只能保存在本机内存中，不能序列化到 Redis
2. Redis 只保存在线状态和节点路由，不保存真实连接对象
3. 连接关闭和传输异常都要执行清理逻辑
4. 发送消息时需要判断 session 是否存在、是否打开
5. 多线程发送同一个 session 时建议加 synchronized，避免并发写异常
```

### 在线状态

在线状态不建议落 MySQL。用户刷新页面、网络抖动、移动端切后台都会导致频繁上下线，如果全部写数据库，会带来大量无效写入。

本案例使用 Redis 维护在线状态：

```text
ws:online:user:{userId}
  判断用户是否在线

ws:online:user:sessions:{userId}
  保存用户所有在线会话和所在节点

ws:online:tenant:{tenantId}
  保存租户下在线用户集合

ws:session:{sessionId}
  保存单个连接的用户信息
```

在线状态需要注意：

```text
1. 在线 Key 必须设置过期时间
2. 客户端需要定时发送心跳
3. 服务端收到心跳后续期在线状态
4. 异常断开时，即使没有触发关闭事件，Redis Key 也会自动过期
5. 多端在线时，只有所有 session 都断开，用户才算真正离线
```

### 多节点推送

多节点部署时，HTTP 请求到达的节点不一定是用户 WebSocket 连接所在节点。因此不能直接在当前节点内存里找连接，必须通过共享路由定位目标节点。

本案例使用 Redis Pub/Sub 实现跨节点转发：

```text
发送节点
-> 查询 ws:online:user:sessions:{userId}
-> 获取用户所在 nodeId
-> 发布 Redis 消息
-> 所有节点订阅频道
-> 目标节点匹配 targetNodeId
-> 目标节点查找本地 WebSocketSession
-> 发送消息
```

多节点推送需要注意：

```text
1. 每个服务实例必须配置唯一 node-id
2. 所有节点必须连接同一个 Redis
3. Redis 中保存的是 sessionId -> nodeId
4. Redis Pub/Sub 不保证消息持久化，所以消息必须先入库再发布
5. 服务重启后，本机 WebSocket 连接会断开，Redis 在线状态依赖过期时间兜底清理
```

如果消息可靠性要求更高，可以把 Redis Pub/Sub 替换为 RabbitMQ、Kafka 或 RocketMQ。Redis Pub/Sub 更轻量，适合中小规模后台通知；MQ 更适合强可靠、大规模、可重试的推送链路。

### 离线消息

离线消息的关键不是单独设计一个「离线表」，而是让所有需要追踪的消息先进入统一消息表。用户在线就立即推送，用户离线就保留为未确认消息。

本案例使用：

```text
ack_status = 0
```

作为是否需要补发的主要判断条件。

这样可以同时覆盖：

```text
1. 用户离线时产生的消息
2. 服务端已推送但客户端未 ACK 的消息
3. WebSocket 发送成功但页面未处理的消息
4. 网络闪断期间漏收的消息
```

离线消息需要注意：

```text
1. 不要只用 push_status 判断是否需要补发
2. 已推送但未确认的消息仍然可能需要补发
3. 用户上线后补发数量要限制，避免一次推送过多
4. ACK 接口必须幂等
5. 离线消息需要定期清理或归档，避免消息表无限增长
```

推荐清理策略：

```text
1. 已确认消息保留 30 天或 90 天
2. 系统通知类消息可按业务要求长期保留
3. 超过有效期的未确认消息可以标记过期
4. 大表场景下按 create_time 做分区或归档
```

### 权限隔离

WebSocket 推送很容易出现越权问题，尤其是在多租户系统中。不能只根据前端传入的 `receiverId`、`tenantId` 就直接推送。

权限隔离建议：

```text
1. WebSocket 建连时必须校验 Token
2. tenantId 应优先从登录态、Token 扩展字段或用户租户关系中获取
3. ACK 时必须校验 messageId、tenantId、receiverId 是否匹配
4. 查询离线消息时必须限制当前登录用户，只能查询自己的消息
5. 按角色推送和按租户推送时，接收人必须来自服务端权限系统查询
6. 跨租户推送必须禁止
```

当前示例为了方便测试，部分接口允许传入 `receiverId` 和 `tenantId`。生产环境中建议改成：

```text
1. receiverId 从当前登录用户中获取
2. tenantId 从当前登录租户上下文中获取
3. 管理端推送接口增加权限校验
4. 普通用户不能直接调用任意用户推送接口
5. 所有推送操作写入审计日志
```

最终实现后，完整链路如下：

```text
用户连接 WebSocket
-> Token 鉴权
-> 注册本地连接
-> Redis 写入在线状态
-> 业务系统创建消息
-> 消息先入库
-> 在线用户实时推送
-> 离线用户保留未确认消息
-> 用户上线自动补发
-> 客户端 ACK
-> 服务端幂等确认
-> 定时任务补偿未确认消息
```

这套方案适合作为后台系统、SaaS 系统、工单系统、审批系统、订单系统中的实时通知基础能力。