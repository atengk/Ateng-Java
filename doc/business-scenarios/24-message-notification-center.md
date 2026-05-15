# 站内信 / 消息通知中心

本文档基于「Java 后端经典高含金量业务场景功能 30 个」中的第 24 个场景：站内信 / 消息通知中心，围绕消息模板、异步投递、站内信落库、WebSocket 实时推送、已读未读、失败重试等核心能力展开实现。

## 功能目标

站内信 / 消息通知中心用于统一承接系统内部的消息通知需求，例如审批结果通知、工单流转通知、订单状态通知、系统公告、营销消息、告警消息等。

本案例的目标不是实现一个完整的企业级消息平台，而是实现一套可直接落地到 Java 后端项目中的核心消息中心能力：

```text
业务系统触发通知
-> 根据消息模板生成通知内容
-> 创建消息主记录
-> 创建用户消息记录
-> 发送 MQ 异步通知事件
-> 消费者执行站内信落库和 WebSocket 推送
-> 用户查询消息列表
-> 用户标记消息已读
-> 系统定时重试失败消息
-> 系统定时清理过期消息
```

本案例重点解决以下问题：

```text
消息内容如何模板化
通知如何异步发送
站内信如何保存
WebSocket 如何实时推送
用户如何查询未读消息
消息如何标记已读
发送失败如何重试
过期消息如何清理
```

技术实现上以 Spring Boot 3、MyBatis-Plus、MySQL、Redis、RabbitMQ、WebSocket、XXL-JOB、Hutool 为主，适合中后台系统、SaaS 系统、OA 系统、工单系统、订单系统直接复用。

### 核心业务流程

本案例采用“业务触发 + MQ 异步解耦 + 站内信落库 + WebSocket 实时推送”的方式实现消息通知。

整体流程如下：

```text
业务系统调用通知发送接口
-> NotificationService 校验请求参数
-> 查询消息模板
-> 使用模板变量渲染消息标题和内容
-> 创建消息主表记录 notify_message
-> 批量创建用户消息表记录 notify_user_message
-> 发送 RabbitMQ 通知事件
-> MQ 消费者接收通知事件
-> 判断用户通知偏好
-> 执行 WebSocket 在线推送
-> 记录发送日志 notify_send_log
-> 用户在消息中心分页查询消息
-> 用户将消息标记为已读
```

在实际业务中，通知触发方不应该直接关心消息是通过站内信、WebSocket、短信还是邮件发送。业务系统只需要提交“通知对象、模板编码、模板变量、业务来源”即可。

例如，审批通过后只需要调用：

```text
发送对象：userId = 10001
模板编码：APPROVAL_PASS
模板变量：{"approvalName":"采购申请单","operatorName":"张三"}
业务来源：APPROVAL
业务主键：AP202605150001
```

消息中心负责将模板渲染成用户可读内容：

```text
标题：审批通过通知
内容：你的采购申请单已由张三审批通过。
```

然后完成站内信保存、实时推送、失败记录和后续重试。

### 本案例实现范围

本案例实现消息通知中心的核心闭环，重点放在后端实操代码，不扩展过多平台化能力。

实现范围如下：

| 功能               | 是否实现 | 说明                                   |
| ------------------ | -------- | -------------------------------------- |
| 消息模板管理       | 实现     | 支持按模板编码查询模板                 |
| 模板变量渲染       | 实现     | 使用 Hutool + 简单占位符替换           |
| 创建通知消息       | 实现     | 支持单用户和多用户通知                 |
| 站内信落库         | 实现     | 保存消息主记录和用户消息记录           |
| RabbitMQ 异步投递  | 实现     | 发送通知事件，消费者异步处理           |
| WebSocket 实时推送 | 实现     | 用户在线时实时推送                     |
| 已读 / 未读        | 实现     | 支持查询未读数、单条已读、批量已读     |
| 用户通知偏好       | 实现     | 支持站内信和 WebSocket 开关            |
| 发送日志           | 实现     | 记录发送成功、失败原因                 |
| 失败重试           | 实现     | 通过 XXL-JOB 扫描失败记录重试          |
| 过期清理           | 实现     | 定时清理过期消息                       |
| 短信 / 邮件        | 预留扩展 | 本案例只给出渠道扩展点，不接入真实 SDK |
| 多租户隔离         | 预留扩展 | 表字段预留 tenant_id                   |
| 管理后台页面       | 不实现   | 仅提供后端接口和核心代码               |

本案例最终会提供以下接口能力：

```text
POST   /api/notify/send              创建通知消息
GET    /api/notify/message/page      分页查询我的站内信
GET    /api/notify/message/unread    查询我的未读数量
PUT    /api/notify/message/read/{id} 标记单条消息已读
PUT    /api/notify/message/read-all  批量标记全部已读
DELETE /api/notify/message/{id}      删除我的消息
GET    /api/notify/preference        查询我的通知偏好
PUT    /api/notify/preference        修改我的通知偏好
```

核心实现原则如下：

```text
业务发送只负责创建通知请求
消息发送通过 MQ 异步削峰
站内信数据必须先落库
WebSocket 推送失败不影响站内信查询
消费端必须具备幂等能力
用户消息和消息主表分离
已读状态只维护在用户消息表
失败发送记录进入重试流程
```

## 技术栈与模块结构

本案例按单体 Spring Boot 模块实现，保留后续拆分为消息服务的边界。技术栈延续原文档中推荐的 Spring Boot、RabbitMQ、Redis、WebSocket、MyBatis-Plus、XXL-JOB、MySQL 等组合，并补充 Hutool 做模板变量处理、JSON 转换和基础工具封装。

### 技术选型

| 技术             | 用途                                           |
| ---------------- | ---------------------------------------------- |
| Spring Boot 3    | 后端基础框架                                   |
| MyBatis-Plus     | 消息模板、消息记录、用户消息等表的 CRUD        |
| MySQL            | 持久化站内信、模板、偏好、发送日志             |
| RabbitMQ         | 异步通知投递、削峰解耦                         |
| Redis            | 消费幂等、未读数量缓存、WebSocket 在线状态扩展 |
| Spring WebSocket | 在线用户实时推送                               |
| XXL-JOB          | 失败消息重试、过期消息清理                     |
| Hutool           | JSON 处理、字符串处理、集合处理、日期处理      |
| Lombok           | 简化实体、DTO、VO 代码                         |
| Sa-Token         | 登录鉴权，本文档先预留用户上下文获取方式       |

本案例采用如下设计取舍：

```text
消息模板：数据库维护，按 template_code 查询
消息内容：创建消息时完成模板渲染
消息投递：先落库，再投递 MQ
实时推送：MQ 消费端通过 WebSocket 推送
离线处理：离线用户不丢消息，登录后从站内信列表查询
已读状态：维护在用户消息表，不污染消息主表
失败重试：发送日志记录失败状态，定时任务补偿
```

### 项目目录结构

下面是本案例建议的核心目录结构，后续代码会按这个结构展开。

```text
src/main/java/io/github/atengk/notify
├── NotifyApplication.java
├── common
│   ├── constant
│   │   └── NotifyMqConstant.java
│   ├── enums
│   │   ├── NotifyChannelEnum.java
│   │   ├── NotifyMessageStatusEnum.java
│   │   ├── NotifyReadStatusEnum.java
│   │   └── NotifySendStatusEnum.java
│   └── result
│       └── Result.java
├── config
│   ├── RabbitMqConfig.java
│   └── WebSocketConfig.java
├── controller
│   ├── NotifyMessageController.java
│   └── NotifyPreferenceController.java
├── dto
│   ├── NotifySendDTO.java
│   ├── NotifyPreferenceUpdateDTO.java
│   └── NotifyMessagePageDTO.java
├── entity
│   ├── NotifyTemplate.java
│   ├── NotifyMessage.java
│   ├── NotifyUserMessage.java
│   ├── NotifyUserPreference.java
│   └── NotifySendLog.java
├── mapper
│   ├── NotifyTemplateMapper.java
│   ├── NotifyMessageMapper.java
│   ├── NotifyUserMessageMapper.java
│   ├── NotifyUserPreferenceMapper.java
│   └── NotifySendLogMapper.java
├── mq
│   ├── NotifyMessageEvent.java
│   ├── NotifyProducer.java
│   └── NotifyConsumer.java
├── service
│   ├── NotifySendService.java
│   ├── NotifyMessageService.java
│   ├── NotifyPreferenceService.java
│   └── impl
│       ├── NotifySendServiceImpl.java
│       ├── NotifyMessageServiceImpl.java
│       └── NotifyPreferenceServiceImpl.java
├── websocket
│   ├── NotifyWebSocketHandler.java
│   └── NotifyWebSocketSessionManager.java
└── job
    └── NotifyRetryJob.java
```

## 数据库表设计

本案例将消息拆成“消息主记录”和“用户消息记录”两层。消息主记录保存标题、内容、来源、过期时间等公共信息；用户消息记录保存接收人、已读状态、删除状态等用户维度信息。

### 消息模板表

消息模板表用于维护不同业务场景的标题和内容模板。例如审批通过、工单分派、订单发货等。

```sql
CREATE TABLE notify_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_code VARCHAR(64) NOT NULL COMMENT '模板编码',
    template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
    title_template VARCHAR(255) NOT NULL COMMENT '标题模板',
    content_template TEXT NOT NULL COMMENT '内容模板',
    channel VARCHAR(32) NOT NULL DEFAULT 'INBOX' COMMENT '默认发送渠道：INBOX/WEBSOCKET/SMS/EMAIL',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否 1是',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID，单租户可固定为0',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_template_code_tenant (template_code, tenant_id),
    KEY idx_enabled (enabled)
) COMMENT='消息模板表';
```

初始化几条业务模板，后续发送通知时直接按 `template_code` 查询。

```sql
INSERT INTO notify_template
(template_code, template_name, title_template, content_template, channel, enabled, remark)
VALUES
('APPROVAL_PASS', '审批通过通知', '审批通过通知', '你的${approvalName}已由${operatorName}审批通过。', 'INBOX', 1, '审批业务通知'),
('WORK_ORDER_ASSIGN', '工单分派通知', '工单分派通知', '你有一个新的工单【${ticketTitle}】待处理。', 'INBOX', 1, '工单业务通知'),
('ORDER_SHIPPED', '订单发货通知', '订单发货通知', '你的订单${orderNo}已发货，物流单号：${expressNo}。', 'INBOX', 1, '订单业务通知');
```

### 消息记录表

消息记录表保存一条通知的公共内容。无论接收人是一个还是多个，消息正文只保存一份，避免批量消息重复存储大文本内容。

```sql
CREATE TABLE notify_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_code VARCHAR(64) DEFAULT NULL COMMENT '模板编码',
    title VARCHAR(255) NOT NULL COMMENT '消息标题',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type VARCHAR(32) NOT NULL DEFAULT 'BUSINESS' COMMENT '消息类型：BUSINESS/SYSTEM/MARKETING/ALARM',
    source_type VARCHAR(64) DEFAULT NULL COMMENT '业务来源类型，如 APPROVAL/ORDER/TICKET',
    source_id VARCHAR(128) DEFAULT NULL COMMENT '业务来源主键',
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '消息状态：NORMAL/EXPIRED/CANCELLED',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_template_code (template_code),
    KEY idx_source (source_type, source_id),
    KEY idx_expire_time (expire_time),
    KEY idx_tenant_create_time (tenant_id, create_time)
) COMMENT='消息记录表';
```

### 用户消息表

用户消息表保存用户维度状态。已读、删除、接收人、读取时间都放在这里。

```sql
CREATE TABLE notify_user_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态：0未读 1已读',
    read_time DATETIME DEFAULT NULL COMMENT '阅读时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_message_user (message_id, user_id),
    KEY idx_user_read (user_id, read_status, deleted),
    KEY idx_user_create_time (user_id, create_time),
    KEY idx_message_id (message_id)
) COMMENT='用户消息表';
```

这里增加 `uk_message_user` 唯一索引，避免同一条消息重复发送给同一个用户。后续批量插入时，即使 MQ 重复消费，也可以结合唯一索引和 Redis 幂等保证不重复产生用户消息。

### 用户通知偏好表

用户通知偏好表用于控制用户是否接收站内信、WebSocket、短信、邮件等渠道通知。本案例实现站内信和 WebSocket 两个核心开关。

```sql
CREATE TABLE notify_user_preference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    inbox_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否开启站内信：0否 1是',
    websocket_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否开启WebSocket推送：0否 1是',
    sms_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否开启短信：0否 1是',
    email_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否开启邮件：0否 1是',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_tenant (user_id, tenant_id)
) COMMENT='用户通知偏好表';
```

### 消息发送日志表

消息发送日志表用于记录每个渠道的发送结果，失败后由定时任务扫描重试。

```sql
CREATE TABLE notify_send_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    channel VARCHAR(32) NOT NULL COMMENT '发送渠道：INBOX/WEBSOCKET/SMS/EMAIL',
    send_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '发送状态：PENDING/SUCCESS/FAIL',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    fail_reason VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_message_user (message_id, user_id),
    KEY idx_retry (send_status, next_retry_time, retry_count),
    KEY idx_channel_status (channel, send_status)
) COMMENT='消息发送日志表';
```

## 基础工程配置

这一部分给出项目启动所需的核心依赖和配置。后续代码默认已经完成 MySQL、RabbitMQ、Redis、WebSocket 的基础配置。

### Maven 依赖配置

文件位置：`pom.xml`

下面配置 Spring Boot 3、MyBatis-Plus、RabbitMQ、Redis、WebSocket、Hutool、XXL-JOB 等核心依赖。

```xml
<dependencies>
    <!-- Spring Web：提供 REST 接口能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于 DTO 入参校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- WebSocket：用于在线消息实时推送 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <!-- RabbitMQ：用于消息异步投递和消费 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- Redis：用于消费幂等、缓存和在线状态扩展 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- MyBatis-Plus：简化 MySQL CRUD -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 8.x -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool：JSON、字符串、集合、日期等工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- XXL-JOB：用于失败消息重试、过期消息清理 -->
    <dependency>
        <groupId>com.xuxueli</groupId>
        <artifactId>xxl-job-core</artifactId>
        <version>2.4.1</version>
    </dependency>

    <!-- Lombok：减少实体类和 DTO 样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Sa-Token：用于登录鉴权和用户上下文，本文档后续只预留使用方式 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>1.39.0</version>
    </dependency>
</dependencies>
```

### application.yml 配置

文件位置：`src/main/resources/application.yml`

下面配置数据库、Redis、RabbitMQ、MyBatis-Plus 和通知中心自定义参数。

```yaml
server:
  port: 8080

spring:
  application:
    name: notify-center-demo

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/notify_center?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3s

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual # 手动确认，避免消费失败时消息直接丢失
        prefetch: 20            # 单消费者预取数量，避免一次拉取过多消息
        retry:
          enabled: false        # 本案例自己记录失败日志，不使用容器自动重试
    publisher-confirm-type: correlated # 开启发布确认
    publisher-returns: true            # 开启不可路由消息返回

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: assign_id # 使用雪花ID；如果表是 AUTO_INCREMENT，也可以改为 auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

notify:
  websocket:
    path: /ws/notify
  retry:
    max-count: 3
    interval-minutes: 5
  message:
    default-expire-days: 30
```

本地启动依赖中间件时，可以使用下面的 Docker 命令快速准备 MySQL、Redis、RabbitMQ。

```bash
docker run -d --name notify-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=notify_center \
  mysql:8.0

docker run -d --name notify-redis \
  -p 6379:6379 \
  redis:7.2

docker run -d --name notify-rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3.13-management
```

`15672` 是 RabbitMQ 管理后台端口，默认账号密码是 `guest/guest`。MySQL 启动后执行上面的建表 SQL 即可。

### RabbitMQ 队列配置

RabbitMQ 用于解耦业务请求和通知发送。业务接口只负责创建通知记录和投递事件，真正的推送逻辑由消费者异步完成。

文件位置：`src/main/java/io/github/atengk/notify/common/constant/NotifyMqConstant.java`

下面定义通知中心使用的交换机、队列和路由键常量。

```java
package io.github.atengk.notify.common.constant;

/**
 * 通知中心 MQ 常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface NotifyMqConstant {

    /**
     * 通知交换机
     */
    String NOTIFY_EXCHANGE = "notify.exchange";

    /**
     * 通知发送队列
     */
    String NOTIFY_SEND_QUEUE = "notify.send.queue";

    /**
     * 通知发送路由键
     */
    String NOTIFY_SEND_ROUTING_KEY = "notify.send";
}
```

文件位置：`src/main/java/io/github/atengk/notify/config/RabbitMqConfig.java`

下面创建 DirectExchange、Queue 和 Binding，并开启 JSON 消息转换。

```java
package io.github.atengk.notify.config;

import io.github.atengk.notify.common.constant.NotifyMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 创建通知交换机
     *
     * @return DirectExchange
     */
    @Bean
    public DirectExchange notifyExchange() {
        return new DirectExchange(NotifyMqConstant.NOTIFY_EXCHANGE, true, false);
    }

    /**
     * 创建通知发送队列
     *
     * @return Queue
     */
    @Bean
    public Queue notifySendQueue() {
        Map<String, Object> args = new HashMap<>();
        // 可按需配置死信交换机，本案例先通过发送日志和定时任务补偿失败消息
        return new Queue(NotifyMqConstant.NOTIFY_SEND_QUEUE, true, false, false, args);
    }

    /**
     * 绑定交换机和队列
     *
     * @return Binding
     */
    @Bean
    public Binding notifySendBinding() {
        return BindingBuilder
                .bind(notifySendQueue())
                .to(notifyExchange())
                .with(NotifyMqConstant.NOTIFY_SEND_ROUTING_KEY);
    }

    /**
     * 使用 JSON 序列化 MQ 消息
     *
     * @return Jackson2JsonMessageConverter
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

### WebSocket 配置

WebSocket 用于在线用户实时接收通知。为了让案例更容易跑通，这里先用 `userId` 查询参数识别连接用户，例如：

```text
ws://localhost:8080/ws/notify?userId=10001
```

生产环境建议改成 Sa-Token 或网关鉴权后透传用户 ID，不建议直接相信前端传入的 `userId`。

文件位置：`src/main/java/io/github/atengk/notify/config/WebSocketConfig.java`

下面注册通知中心 WebSocket 端点。

```java
package io.github.atengk.notify.config;

import io.github.atengk.notify.websocket.NotifyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotifyWebSocketHandler notifyWebSocketHandler;

    /**
     * 注册 WebSocket 处理器
     *
     * @param registry WebSocket 注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notifyWebSocketHandler, "/ws/notify")
                .setAllowedOriginPatterns("*");
    }
}
```

文件位置：`src/main/java/io/github/atengk/notify/websocket/NotifyWebSocketSessionManager.java`

下面维护用户和 WebSocket Session 的映射关系，后续 MQ 消费端会通过它向在线用户推送消息。

```java
package io.github.atengk.notify.websocket;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 通知 WebSocket 会话管理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class NotifyWebSocketSessionManager {

    private final ConcurrentMap<Long, Set<WebSocketSession>> userSessionMap = new ConcurrentHashMap<>();

    /**
     * 添加用户连接
     *
     * @param userId 用户ID
     * @param session WebSocket 会话
     */
    public void addSession(Long userId, WebSocketSession session) {
        userSessionMap.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("用户建立 WebSocket 连接，userId={}，sessionId={}", userId, session.getId());
    }

    /**
     * 移除用户连接
     *
     * @param userId 用户ID
     * @param session WebSocket 会话
     */
    public void removeSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = userSessionMap.get(userId);
        if (CollUtil.isEmpty(sessions)) {
            return;
        }
        sessions.remove(session);
        if (CollUtil.isEmpty(sessions)) {
            userSessionMap.remove(userId);
        }
        log.info("用户断开 WebSocket 连接，userId={}，sessionId={}", userId, session.getId());
    }

    /**
     * 获取用户连接
     *
     * @param userId 用户ID
     * @return WebSocket 会话集合
     */
    public Set<WebSocketSession> getSessions(Long userId) {
        return userSessionMap.getOrDefault(userId, Set.of());
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isOnline(Long userId) {
        return CollUtil.isNotEmpty(userSessionMap.get(userId));
    }
}
```

文件位置：`src/main/java/io/github/atengk/notify/websocket/NotifyWebSocketHandler.java`

下面处理 WebSocket 建连、断连和异常。真正的业务推送方法会在后续章节补充。

```java
package io.github.atengk.notify.websocket;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

/**
 * 通知 WebSocket 处理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID_KEY = "userId";

    private final NotifyWebSocketSessionManager sessionManager;

    /**
     * 建立连接后保存用户会话
     *
     * @param session WebSocket 会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = parseUserId(session);
        if (userId == null) {
            log.warn("WebSocket 连接缺少 userId，sessionId={}", session.getId());
            return;
        }
        session.getAttributes().put(USER_ID_KEY, userId);
        sessionManager.addSession(userId, session);
    }

    /**
     * 接收客户端消息
     *
     * @param session WebSocket 会话
     * @param message 文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("收到 WebSocket 客户端消息，sessionId={}，payload={}", session.getId(), message.getPayload());
    }

    /**
     * 断开连接后移除用户会话
     *
     * @param session WebSocket 会话
     * @param status 关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = Convert.toLong(session.getAttributes().get(USER_ID_KEY), null);
        if (userId != null) {
            sessionManager.removeSession(userId, session);
        }
    }

    /**
     * 连接异常处理
     *
     * @param session WebSocket 会话
     * @param exception 异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 连接异常，sessionId={}，原因={}", session.getId(), exception.getMessage());
    }

    /**
     * 从连接地址中解析用户ID
     *
     * @param session WebSocket 会话
     * @return 用户ID
     */
    private Long parseUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || StrUtil.isBlank(uri.getQuery())) {
            return null;
        }

        String query = uri.getQuery();
        for (String item : query.split("&")) {
            String[] pair = item.split("=");
            if (pair.length == 2 && StrUtil.equals(pair[0], USER_ID_KEY)) {
                return Convert.toLong(pair[1], null);
            }
        }
        return null;
    }
}
```

## 枚举与基础模型

这一部分先定义消息中心中最基础的枚举、统一返回对象、消息模板实体和模板相关 DTO。后续创建消息、MQ 投递、WebSocket 推送都会复用这些基础模型。

### 消息类型枚举

消息类型用于区分业务通知、系统公告、营销消息和告警消息。它主要存储在 `notify_message.message_type` 字段中，便于前端分类展示和后端筛选。

文件位置：`src/main/java/io/github/atengk/notify/common/enums/NotifyMessageTypeEnum.java`

```java
package io.github.atengk.notify.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum NotifyMessageTypeEnum {

    /**
     * 业务消息
     */
    BUSINESS("BUSINESS", "业务消息"),

    /**
     * 系统消息
     */
    SYSTEM("SYSTEM", "系统消息"),

    /**
     * 营销消息
     */
    MARKETING("MARKETING", "营销消息"),

    /**
     * 告警消息
     */
    ALARM("ALARM", "告警消息");

    private final String code;

    private final String desc;
}
```

### 消息渠道枚举

消息渠道用于描述通知发送方式。本案例核心实现站内信和 WebSocket，短信、邮件只保留扩展入口。

文件位置：`src/main/java/io/github/atengk/notify/common/enums/NotifyChannelEnum.java`

```java
package io.github.atengk.notify.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息渠道枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum NotifyChannelEnum {

    /**
     * 站内信
     */
    INBOX("INBOX", "站内信"),

    /**
     * WebSocket 实时推送
     */
    WEBSOCKET("WEBSOCKET", "WebSocket 推送"),

    /**
     * 短信
     */
    SMS("SMS", "短信"),

    /**
     * 邮件
     */
    EMAIL("EMAIL", "邮件");

    private final String code;

    private final String desc;
}
```

### 消息状态枚举

消息状态用于控制消息是否正常可见。正常消息可以被用户查询；过期消息由定时任务标记；取消消息适合公告撤回、运营消息撤销等场景。

文件位置：`src/main/java/io/github/atengk/notify/common/enums/NotifyMessageStatusEnum.java`

```java
package io.github.atengk.notify.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum NotifyMessageStatusEnum {

    /**
     * 正常
     */
    NORMAL("NORMAL", "正常"),

    /**
     * 已过期
     */
    EXPIRED("EXPIRED", "已过期"),

    /**
     * 已取消
     */
    CANCELLED("CANCELLED", "已取消");

    private final String code;

    private final String desc;
}
```

### 已读状态枚举

已读状态只维护在用户消息表中，因为同一条消息对不同用户的阅读状态不同。

文件位置：`src/main/java/io/github/atengk/notify/common/enums/NotifyReadStatusEnum.java`

```java
package io.github.atengk.notify.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息已读状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum NotifyReadStatusEnum {

    /**
     * 未读
     */
    UNREAD(0, "未读"),

    /**
     * 已读
     */
    READ(1, "已读");

    private final Integer code;

    private final String desc;
}
```

为了后续接口统一返回，这里补充一个简单的 `Result` 返回模型。真实项目中可以替换为你现有项目的统一响应对象。

文件位置：`src/main/java/io/github/atengk/notify/common/result/Result.java`

```java
package io.github.atengk.notify.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 成功返回
     *
     * @param data 返回数据
     * @return 统一结果
     * @param <T> 数据类型
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功返回
     *
     * @return 统一结果
     * @param <T> 数据类型
     */
    public static <T> Result<T> ok() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 失败返回
     *
     * @param message 错误消息
     * @return 统一结果
     * @param <T> 数据类型
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
```

消息模板实体类对应前面创建的 `notify_template` 表，用于模板新增、查询和渲染。

文件位置：`src/main/java/io/github/atengk/notify/entity/NotifyTemplate.java`

```java
package io.github.atengk.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息模板实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("notify_template")
public class NotifyTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateCode;

    private String templateName;

    private String titleTemplate;

    private String contentTemplate;

    private String channel;

    private Integer enabled;

    private String remark;

    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/notify/mapper/NotifyTemplateMapper.java`

```java
package io.github.atengk.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.notify.entity.NotifyTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息模板 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface NotifyTemplateMapper extends BaseMapper<NotifyTemplate> {
}
```

## 消息模板管理

消息模板管理负责维护通知标题和内容模板。业务系统发送通知时只传模板编码和变量，不直接拼接消息内容，这样可以降低业务代码和通知文案之间的耦合。

模板内容采用 `${变量名}` 格式，例如：

```text
你的${approvalName}已由${operatorName}审批通过。
```

发送时传入：

```json
{
  "approvalName": "采购申请单",
  "operatorName": "张三"
}
```

最终渲染结果为：

```text
你的采购申请单已由张三审批通过。
```

### 新增消息模板

新增模板用于后台维护消息文案。本案例先提供一个后端接口，真实项目中可以由管理后台调用。

文件位置：`src/main/java/io/github/atengk/notify/dto/NotifyTemplateCreateDTO.java`

```java
package io.github.atengk.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增消息模板请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class NotifyTemplateCreateDTO {

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "标题模板不能为空")
    private String titleTemplate;

    @NotBlank(message = "内容模板不能为空")
    private String contentTemplate;

    @NotBlank(message = "消息渠道不能为空")
    private String channel;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private String remark;
}
```

文件位置：`src/main/java/io/github/atengk/notify/vo/NotifyTemplateVO.java`

```java
package io.github.atengk.notify.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息模板响应对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class NotifyTemplateVO {

    private Long id;

    private String templateCode;

    private String templateName;

    private String titleTemplate;

    private String contentTemplate;

    private String channel;

    private Integer enabled;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/notify/service/NotifyTemplateService.java`

```java
package io.github.atengk.notify.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.notify.dto.NotifyTemplateCreateDTO;
import io.github.atengk.notify.entity.NotifyTemplate;
import io.github.atengk.notify.vo.NotifyTemplateVO;

import java.util.Map;

/**
 * 消息模板服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface NotifyTemplateService extends IService<NotifyTemplate> {

    /**
     * 新增消息模板
     *
     * @param dto 新增模板请求
     * @return 模板ID
     */
    Long createTemplate(NotifyTemplateCreateDTO dto);

    /**
     * 根据模板编码查询启用模板
     *
     * @param templateCode 模板编码
     * @return 模板信息
     */
    NotifyTemplate getEnabledTemplate(String templateCode);

    /**
     * 查询模板详情
     *
     * @param templateCode 模板编码
     * @return 模板响应对象
     */
    NotifyTemplateVO getTemplateByCode(String templateCode);

    /**
     * 渲染模板内容
     *
     * @param template 模板内容
     * @param variables 模板变量
     * @return 渲染后的内容
     */
    String renderTemplate(String template, Map<String, Object> variables);
}
```

下面实现模板新增、模板查询和变量渲染。新增时会校验模板编码是否重复，避免业务侧发送消息时出现模板冲突。

文件位置：`src/main/java/io/github/atengk/notify/service/impl/NotifyTemplateServiceImpl.java`

```java
package io.github.atengk.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.notify.dto.NotifyTemplateCreateDTO;
import io.github.atengk.notify.entity.NotifyTemplate;
import io.github.atengk.notify.mapper.NotifyTemplateMapper;
import io.github.atengk.notify.service.NotifyTemplateService;
import io.github.atengk.notify.vo.NotifyTemplateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 消息模板服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class NotifyTemplateServiceImpl extends ServiceImpl<NotifyTemplateMapper, NotifyTemplate>
        implements NotifyTemplateService {

    /**
     * 新增消息模板
     *
     * @param dto 新增模板请求
     * @return 模板ID
     */
    @Override
    public Long createTemplate(NotifyTemplateCreateDTO dto) {
        long existsCount = this.count(Wrappers.<NotifyTemplate>lambdaQuery()
                .eq(NotifyTemplate::getTemplateCode, dto.getTemplateCode())
                .eq(NotifyTemplate::getTenantId, 0L));

        if (existsCount > 0) {
            throw new IllegalArgumentException("模板编码已存在：" + dto.getTemplateCode());
        }

        NotifyTemplate template = BeanUtil.copyProperties(dto, NotifyTemplate.class);
        template.setTenantId(0L);
        this.save(template);

        log.info("新增消息模板成功，templateCode={}，templateId={}", template.getTemplateCode(), template.getId());
        return template.getId();
    }

    /**
     * 根据模板编码查询启用模板
     *
     * @param templateCode 模板编码
     * @return 模板信息
     */
    @Override
    public NotifyTemplate getEnabledTemplate(String templateCode) {
        NotifyTemplate template = this.getOne(Wrappers.<NotifyTemplate>lambdaQuery()
                .eq(NotifyTemplate::getTemplateCode, templateCode)
                .eq(NotifyTemplate::getEnabled, 1)
                .eq(NotifyTemplate::getTenantId, 0L)
                .last("LIMIT 1"));

        if (template == null) {
            throw new IllegalArgumentException("消息模板不存在或未启用：" + templateCode);
        }
        return template;
    }

    /**
     * 查询模板详情
     *
     * @param templateCode 模板编码
     * @return 模板响应对象
     */
    @Override
    public NotifyTemplateVO getTemplateByCode(String templateCode) {
        NotifyTemplate template = this.getEnabledTemplate(templateCode);
        return BeanUtil.copyProperties(template, NotifyTemplateVO.class);
    }

    /**
     * 渲染模板内容
     *
     * @param template 模板内容
     * @param variables 模板变量
     * @return 渲染后的内容
     */
    @Override
    public String renderTemplate(String template, Map<String, Object> variables) {
        if (StrUtil.isBlank(template)) {
            return StrUtil.EMPTY;
        }
        if (MapUtil.isEmpty(variables)) {
            return template;
        }

        String rendered = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = StrUtil.format("${{{}}}", entry.getKey());
            String value = ObjectUtil.toString(entry.getValue(), StrUtil.EMPTY);
            rendered = StrUtil.replace(rendered, placeholder, value);
        }
        return rendered;
    }
}
```

文件位置：`src/main/java/io/github/atengk/notify/controller/NotifyTemplateController.java`

```java
package io.github.atengk.notify.controller;

import io.github.atengk.notify.common.result.Result;
import io.github.atengk.notify.dto.NotifyTemplateCreateDTO;
import io.github.atengk.notify.service.NotifyTemplateService;
import io.github.atengk.notify.vo.NotifyTemplateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 消息模板接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notify/template")
public class NotifyTemplateController {

    private final NotifyTemplateService notifyTemplateService;

    /**
     * 新增消息模板
     *
     * @param dto 新增模板请求
     * @return 模板ID
     */
    @PostMapping
    public Result<Long> createTemplate(@Valid @RequestBody NotifyTemplateCreateDTO dto) {
        return Result.ok(notifyTemplateService.createTemplate(dto));
    }

    /**
     * 根据模板编码查询模板
     *
     * @param templateCode 模板编码
     * @return 模板详情
     */
    @GetMapping("/{templateCode}")
    public Result<NotifyTemplateVO> getTemplate(@PathVariable String templateCode) {
        return Result.ok(notifyTemplateService.getTemplateByCode(templateCode));
    }
}
```

新增模板接口示例：

```bash
curl -X POST "http://localhost:8080/api/notify/template" \
  -H "Content-Type: application/json" \
  -d '{
    "templateCode": "CONTRACT_APPROVED",
    "templateName": "合同审批通过通知",
    "titleTemplate": "合同审批通过",
    "contentTemplate": "你的合同【${contractName}】已由${operatorName}审批通过。",
    "channel": "INBOX",
    "enabled": 1,
    "remark": "合同业务通知"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1
}
```

### 查询消息模板

查询模板接口主要用于管理后台回显，也可以用于发送通知前确认模板是否存在。

请求示例：

```bash
curl -X GET "http://localhost:8080/api/notify/template/CONTRACT_APPROVED"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "templateCode": "CONTRACT_APPROVED",
    "templateName": "合同审批通过通知",
    "titleTemplate": "合同审批通过",
    "contentTemplate": "你的合同【${contractName}】已由${operatorName}审批通过。",
    "channel": "INBOX",
    "enabled": 1,
    "remark": "合同业务通知",
    "createTime": "2026-05-15T10:20:00",
    "updateTime": "2026-05-15T10:20:00"
  }
}
```

### 模板变量渲染

模板变量渲染是消息中心的关键能力。业务侧只传变量，不拼接完整文案。这样文案修改只需要调整模板表，不需要发布业务代码。

为了方便单独验证，可以增加一个简单的测试接口。实际生产项目可以不暴露该接口，只在服务内部使用。

文件位置：`src/main/java/io/github/atengk/notify/dto/NotifyTemplateRenderDTO.java`

```java
package io.github.atengk.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 消息模板渲染请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class NotifyTemplateRenderDTO {

    @NotBlank(message = "模板内容不能为空")
    private String template;

    @NotNull(message = "模板变量不能为空")
    private Map<String, Object> variables;
}
```

在 `NotifyTemplateController` 中追加一个模板渲染接口：

```java
/**
 * 渲染模板内容
 *
 * @param dto 模板渲染请求
 * @return 渲染后的内容
 */
@PostMapping("/render")
public Result<String> renderTemplate(@Valid @RequestBody NotifyTemplateRenderDTO dto) {
    return Result.ok(notifyTemplateService.renderTemplate(dto.getTemplate(), dto.getVariables()));
}
```

测试模板渲染：

```bash
curl -X POST "http://localhost:8080/api/notify/template/render" \
  -H "Content-Type: application/json" \
  -d '{
    "template": "你的合同【${contractName}】已由${operatorName}审批通过。",
    "variables": {
      "contractName": "年度采购合同",
      "operatorName": "张三"
    }
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "你的合同【年度采购合同】已由张三审批通过。"
}
```

这里的渲染逻辑保持简单直接，适合站内信、工单通知、审批通知这类变量较少的场景。如果模板中存在条件判断、循环、格式化日期等复杂需求，可以将 `renderTemplate` 方法替换为 Freemarker 或 Thymeleaf 实现。

## 消息创建与投递

消息创建与投递是消息中心的主链路。业务系统调用发送接口后，服务端先查询模板、渲染内容、保存消息主记录和用户消息记录，然后发送 MQ 事件，由消费者完成实时推送和发送日志处理。

本案例采用“先落库，后投递 MQ”的方式，保证即使 RabbitMQ 或 WebSocket 临时异常，用户也能在站内信列表中查询到消息。

### 创建站内信消息

消息主记录只保存一份公共消息内容，例如标题、正文、来源业务、过期时间等。多个接收人共享同一条 `notify_message` 记录。

文件位置：`src/main/java/io/github/atengk/notify/entity/NotifyMessage.java`

```java
package io.github.atengk.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息主记录实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("notify_message")
public class NotifyMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateCode;

    private String title;

    private String content;

    private String messageType;

    private String sourceType;

    private String sourceId;

    private String status;

    private LocalDateTime expireTime;

    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/notify/mapper/NotifyMessageMapper.java`

```java
package io.github.atengk.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.notify.entity.NotifyMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息主记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface NotifyMessageMapper extends BaseMapper<NotifyMessage> {
}
```

### 批量创建用户消息

用户消息记录用于保存每个接收人的已读状态、删除状态和阅读时间。同一条消息发给多个用户时，需要批量写入 `notify_user_message`。

文件位置：`src/main/java/io/github/atengk/notify/entity/NotifyUserMessage.java`

```java
package io.github.atengk.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户消息实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("notify_user_message")
public class NotifyUserMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;

    private Long userId;

    private Integer readStatus;

    private LocalDateTime readTime;

    private Integer deleted;

    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/notify/mapper/NotifyUserMessageMapper.java`

```java
package io.github.atengk.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.notify.entity.NotifyUserMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户消息 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface NotifyUserMessageMapper extends BaseMapper<NotifyUserMessage> {
}
```

发送日志用于记录每个用户、每个渠道的发送结果，后续失败重试会扫描这张表。

文件位置：`src/main/java/io/github/atengk/notify/entity/NotifySendLog.java`

```java
package io.github.atengk.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息发送日志实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("notify_send_log")
public class NotifySendLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;

    private Long userId;

    private String channel;

    private String sendStatus;

    private Integer retryCount;

    private Integer maxRetryCount;

    private String failReason;

    private LocalDateTime nextRetryTime;

    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/notify/mapper/NotifySendLogMapper.java`

```java
package io.github.atengk.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.notify.entity.NotifySendLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息发送日志 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface NotifySendLogMapper extends BaseMapper<NotifySendLog> {
}
```

### 发送 MQ 异步通知事件

MQ 事件只传递必要信息，不直接传递完整的大对象。消费者根据 `messageId` 和 `receiverUserIds` 查询消息并执行推送。

文件位置：`src/main/java/io/github/atengk/notify/mq/NotifyMessageEvent.java`

```java
package io.github.atengk.notify.mq;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通知消息事件
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class NotifyMessageEvent implements Serializable {

    private Long messageId;

    private List<Long> receiverUserIds;

    private String traceId;
}
```

文件位置：`src/main/java/io/github/atengk/notify/common/enums/NotifySendStatusEnum.java`

```java
package io.github.atengk.notify.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息发送状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum NotifySendStatusEnum {

    /**
     * 待发送
     */
    PENDING("PENDING", "待发送"),

    /**
     * 发送成功
     */
    SUCCESS("SUCCESS", "发送成功"),

    /**
     * 发送失败
     */
    FAIL("FAIL", "发送失败");

    private final String code;

    private final String desc;
}
```

### 消息创建接口实现

发送请求 DTO 负责接收业务方传入的模板编码、接收人、模板变量、业务来源等信息。

文件位置：`src/main/java/io/github/atengk/notify/dto/NotifySendDTO.java`

```java
package io.github.atengk.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 通知发送请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class NotifySendDTO {

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotEmpty(message = "接收用户不能为空")
    private List<Long> receiverUserIds;

    private Map<String, Object> variables;

    private String messageType;

    private String sourceType;

    private String sourceId;

    private LocalDateTime expireTime;
}
```

文件位置：`src/main/java/io/github/atengk/notify/service/NotifySendService.java`

```java
package io.github.atengk.notify.service;

import io.github.atengk.notify.dto.NotifySendDTO;

/**
 * 通知发送服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface NotifySendService {

    /**
     * 创建通知并投递 MQ
     *
     * @param dto 通知发送请求
     * @return 消息ID
     */
    Long sendNotify(NotifySendDTO dto);
}
```

下面的实现完成四件事：查询模板、渲染消息、保存站内信、投递 MQ。这里使用事务包裹数据库写入，避免消息主表和用户消息表不一致。

文件位置：`src/main/java/io/github/atengk/notify/service/impl/NotifySendServiceImpl.java`

```java
package io.github.atengk.notify.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.notify.common.enums.NotifyMessageStatusEnum;
import io.github.atengk.notify.common.enums.NotifyMessageTypeEnum;
import io.github.atengk.notify.common.enums.NotifyReadStatusEnum;
import io.github.atengk.notify.dto.NotifySendDTO;
import io.github.atengk.notify.entity.NotifyMessage;
import io.github.atengk.notify.entity.NotifyTemplate;
import io.github.atengk.notify.entity.NotifyUserMessage;
import io.github.atengk.notify.mapper.NotifyMessageMapper;
import io.github.atengk.notify.mapper.NotifyUserMessageMapper;
import io.github.atengk.notify.mq.NotifyMessageEvent;
import io.github.atengk.notify.mq.NotifyProducer;
import io.github.atengk.notify.service.NotifySendService;
import io.github.atengk.notify.service.NotifyTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知发送服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifySendServiceImpl extends ServiceImpl<NotifyMessageMapper, NotifyMessage>
        implements NotifySendService {

    private final NotifyTemplateService notifyTemplateService;

    private final NotifyUserMessageMapper notifyUserMessageMapper;

    private final NotifyProducer notifyProducer;

    /**
     * 创建通知并投递 MQ
     *
     * @param dto 通知发送请求
     * @return 消息ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendNotify(NotifySendDTO dto) {
        if (CollUtil.isEmpty(dto.getReceiverUserIds())) {
            throw new IllegalArgumentException("接收用户不能为空");
        }

        NotifyTemplate template = notifyTemplateService.getEnabledTemplate(dto.getTemplateCode());
        String title = notifyTemplateService.renderTemplate(template.getTitleTemplate(), dto.getVariables());
        String content = notifyTemplateService.renderTemplate(template.getContentTemplate(), dto.getVariables());

        NotifyMessage message = new NotifyMessage();
        message.setTemplateCode(template.getTemplateCode());
        message.setTitle(title);
        message.setContent(content);
        message.setMessageType(StrUtil.blankToDefault(dto.getMessageType(), NotifyMessageTypeEnum.BUSINESS.getCode()));
        message.setSourceType(dto.getSourceType());
        message.setSourceId(dto.getSourceId());
        message.setStatus(NotifyMessageStatusEnum.NORMAL.getCode());
        message.setExpireTime(dto.getExpireTime());
        message.setTenantId(0L);
        this.save(message);

        List<NotifyUserMessage> userMessages = dto.getReceiverUserIds()
                .stream()
                .distinct()
                .map(userId -> buildUserMessage(message.getId(), userId))
                .toList();

        for (NotifyUserMessage userMessage : userMessages) {
            notifyUserMessageMapper.insert(userMessage);
        }

        NotifyMessageEvent event = new NotifyMessageEvent();
        event.setMessageId(message.getId());
        event.setReceiverUserIds(userMessages.stream().map(NotifyUserMessage::getUserId).toList());
        event.setTraceId(UUID.fastUUID().toString(true));

        notifyProducer.sendNotifyEvent(event);

        log.info("创建通知消息成功，messageId={}，receiverCount={}，templateCode={}",
                message.getId(), userMessages.size(), dto.getTemplateCode());
        return message.getId();
    }

    /**
     * 构建用户消息
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 用户消息
     */
    private NotifyUserMessage buildUserMessage(Long messageId, Long userId) {
        NotifyUserMessage userMessage = new NotifyUserMessage();
        userMessage.setMessageId(messageId);
        userMessage.setUserId(userId);
        userMessage.setReadStatus(NotifyReadStatusEnum.UNREAD.getCode());
        userMessage.setDeleted(0);
        userMessage.setTenantId(0L);
        return userMessage;
    }
}
```

文件位置：`src/main/java/io/github/atengk/notify/controller/NotifySendController.java`

```java
package io.github.atengk.notify.controller;

import io.github.atengk.notify.common.result.Result;
import io.github.atengk.notify.dto.NotifySendDTO;
import io.github.atengk.notify.service.NotifySendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知发送接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notify")
public class NotifySendController {

    private final NotifySendService notifySendService;

    /**
     * 创建通知消息
     *
     * @param dto 通知发送请求
     * @return 消息ID
     */
    @PostMapping("/send")
    public Result<Long> sendNotify(@Valid @RequestBody NotifySendDTO dto) {
        return Result.ok(notifySendService.sendNotify(dto));
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/api/notify/send" \
  -H "Content-Type: application/json" \
  -d '{
    "templateCode": "APPROVAL_PASS",
    "receiverUserIds": [10001, 10002],
    "variables": {
      "approvalName": "采购申请单",
      "operatorName": "张三"
    },
    "messageType": "BUSINESS",
    "sourceType": "APPROVAL",
    "sourceId": "AP202605150001"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1
}
```

## MQ 异步消费发送

MQ 消费端负责处理通知事件。这里主要完成 WebSocket 推送和发送日志记录。站内信数据在创建阶段已经落库，所以即使 MQ 消费失败，也不会影响用户后续在消息中心查看消息。

### 通知消息生产者

生产者封装 RabbitTemplate，业务服务只调用 `sendNotifyEvent`，不直接接触交换机、路由键等 MQ 细节。

文件位置：`src/main/java/io/github/atengk/notify/mq/NotifyProducer.java`

```java
package io.github.atengk.notify.mq;

import cn.hutool.json.JSONUtil;
import io.github.atengk.notify.common.constant.NotifyMqConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 通知消息生产者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送通知事件
     *
     * @param event 通知消息事件
     */
    public void sendNotifyEvent(NotifyMessageEvent event) {
        rabbitTemplate.convertAndSend(
                NotifyMqConstant.NOTIFY_EXCHANGE,
                NotifyMqConstant.NOTIFY_SEND_ROUTING_KEY,
                event
        );
        log.info("通知事件投递成功，messageId={}，traceId={}，payload={}",
                event.getMessageId(), event.getTraceId(), JSONUtil.toJsonStr(event));
    }
}
```

### 通知消息消费者

消费者收到 MQ 事件后，先做幂等判断，再查询消息正文，并按用户执行 WebSocket 推送。在线用户推送成功会记录成功日志；离线或推送异常会记录失败日志，后续由定时任务重试。

为了支持服务端主动推送，需要先给 `NotifyWebSocketSessionManager` 增加发送方法。

文件位置：`src/main/java/io/github/atengk/notify/websocket/NotifyWebSocketSessionManager.java`

在原有类中追加下面方法：

```java
/**
 * 向指定用户发送文本消息
 *
 * @param userId 用户ID
 * @param payload 消息内容
 * @return 是否发送成功
 */
public boolean sendText(Long userId, String payload) {
    Set<WebSocketSession> sessions = this.getSessions(userId);
    if (CollUtil.isEmpty(sessions)) {
        log.info("用户当前不在线，跳过 WebSocket 推送，userId={}", userId);
        return false;
    }

    boolean success = false;
    for (WebSocketSession session : sessions) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new org.springframework.web.socket.TextMessage(payload));
                success = true;
            }
        } catch (Exception ex) {
            log.warn("WebSocket 推送失败，userId={}，sessionId={}，原因={}",
                    userId, session.getId(), ex.getMessage());
        }
    }
    return success;
}
```

文件位置：`src/main/java/io/github/atengk/notify/vo/NotifyPushVO.java`

```java
package io.github.atengk.notify.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebSocket 推送消息对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class NotifyPushVO {

    private Long messageId;

    private String title;

    private String content;

    private String messageType;

    private String sourceType;

    private String sourceId;

    private LocalDateTime pushTime;
}
```

下面是 MQ 消费者完整实现。`basicAck` 只在业务处理完成后确认；如果处理过程中出现异常，则 `basicNack` 并设置不重新入队，避免异常消息无限循环。失败详情已经写入发送日志，后续由重试任务补偿。

文件位置：`src/main/java/io/github/atengk/notify/mq/NotifyConsumer.java`

```java
package io.github.atengk.notify.mq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;
import io.github.atengk.notify.common.constant.NotifyMqConstant;
import io.github.atengk.notify.common.enums.NotifyChannelEnum;
import io.github.atengk.notify.common.enums.NotifySendStatusEnum;
import io.github.atengk.notify.entity.NotifyMessage;
import io.github.atengk.notify.mapper.NotifyMessageMapper;
import io.github.atengk.notify.service.NotifySendLogService;
import io.github.atengk.notify.vo.NotifyPushVO;
import io.github.atengk.notify.websocket.NotifyWebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 通知消息消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyConsumer {

    private final NotifyMessageMapper notifyMessageMapper;

    private final NotifySendLogService notifySendLogService;

    private final NotifyWebSocketSessionManager sessionManager;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 消费通知发送事件
     *
     * @param event 通知事件
     * @param message 原始消息
     * @param channel RabbitMQ 信道
     * @throws IOException IO 异常
     */
    @RabbitListener(queues = NotifyMqConstant.NOTIFY_SEND_QUEUE)
    public void consumeNotifyEvent(NotifyMessageEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            if (event == null || event.getMessageId() == null || CollUtil.isEmpty(event.getReceiverUserIds())) {
                log.warn("通知事件参数不完整，payload={}", JSONUtil.toJsonStr(event));
                channel.basicAck(deliveryTag, false);
                return;
            }

            String idempotentKey = "notify:mq:consume:" + event.getMessageId() + ":" + event.getTraceId();
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 10, TimeUnit.MINUTES);

            if (Boolean.FALSE.equals(locked)) {
                log.info("通知事件重复消费，messageId={}，traceId={}", event.getMessageId(), event.getTraceId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            NotifyMessage notifyMessage = notifyMessageMapper.selectOne(Wrappers.<NotifyMessage>lambdaQuery()
                    .eq(NotifyMessage::getId, event.getMessageId())
                    .last("LIMIT 1"));

            if (notifyMessage == null) {
                log.warn("通知消息不存在，messageId={}", event.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            for (Long userId : event.getReceiverUserIds()) {
                pushWebSocketMessage(notifyMessage, userId);
            }

            channel.basicAck(deliveryTag, false);
            log.info("通知事件消费完成，messageId={}，receiverCount={}",
                    event.getMessageId(), event.getReceiverUserIds().size());
        } catch (Exception ex) {
            log.error("通知事件消费异常，payload={}，原因={}", JSONUtil.toJsonStr(event), ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 推送 WebSocket 消息
     *
     * @param notifyMessage 通知消息
     * @param userId 用户ID
     */
    private void pushWebSocketMessage(NotifyMessage notifyMessage, Long userId) {
        NotifyPushVO pushVO = new NotifyPushVO();
        pushVO.setMessageId(notifyMessage.getId());
        pushVO.setTitle(notifyMessage.getTitle());
        pushVO.setContent(notifyMessage.getContent());
        pushVO.setMessageType(notifyMessage.getMessageType());
        pushVO.setSourceType(notifyMessage.getSourceType());
        pushVO.setSourceId(notifyMessage.getSourceId());
        pushVO.setPushTime(LocalDateTime.now());

        boolean pushed = sessionManager.sendText(userId, JSONUtil.toJsonStr(pushVO));
        if (pushed) {
            notifySendLogService.recordSuccess(
                    notifyMessage.getId(),
                    userId,
                    NotifyChannelEnum.WEBSOCKET.getCode()
            );
            log.info("WebSocket 通知推送成功，messageId={}，userId={}", notifyMessage.getId(), userId);
            return;
        }

        notifySendLogService.recordFail(
                notifyMessage.getId(),
                userId,
                NotifyChannelEnum.WEBSOCKET.getCode(),
                "用户不在线或连接不可用"
        );
    }
}
```

### 消费幂等处理

MQ 天然存在重复投递的可能，所以消费者必须具备幂等能力。本案例使用 Redis `SETNX` 做消费幂等，幂等 Key 由 `messageId + traceId` 组成。

关键逻辑如下：

```java
String idempotentKey = "notify:mq:consume:" + event.getMessageId() + ":" + event.getTraceId();
Boolean locked = stringRedisTemplate.opsForValue()
        .setIfAbsent(idempotentKey, "1", 10, TimeUnit.MINUTES);

if (Boolean.FALSE.equals(locked)) {
    log.info("通知事件重复消费，messageId={}，traceId={}", event.getMessageId(), event.getTraceId());
    channel.basicAck(deliveryTag, false);
    return;
}
```

这里的处理规则是：

```text
第一次消费：SETNX 成功，继续执行业务
重复消费：SETNX 失败，直接 ack
消费异常：记录错误并 nack，不重新入队
失败补偿：依赖 notify_send_log + 定时任务重试
```

如果业务要求更严格，可以再增加数据库消费记录表，例如 `notify_consume_log`，通过唯一索引保证幂等。对于本案例这种通知推送场景，Redis 幂等已经足够覆盖核心需求。

### 发送失败记录

发送失败记录由 `NotifySendLogService` 统一处理，避免消费者里直接拼装日志对象。后续失败重试任务也会复用这个服务。

文件位置：`src/main/java/io/github/atengk/notify/service/NotifySendLogService.java`

```java
package io.github.atengk.notify.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.notify.entity.NotifySendLog;

/**
 * 通知发送日志服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface NotifySendLogService extends IService<NotifySendLog> {

    /**
     * 记录发送成功
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @param channel 渠道
     */
    void recordSuccess(Long messageId, Long userId, String channel);

    /**
     * 记录发送失败
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @param channel 渠道
     * @param failReason 失败原因
     */
    void recordFail(Long messageId, Long userId, String channel, String failReason);
}
```

文件位置：`src/main/java/io/github/atengk/notify/service/impl/NotifySendLogServiceImpl.java`

```java
package io.github.atengk.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.notify.common.enums.NotifySendStatusEnum;
import io.github.atengk.notify.entity.NotifySendLog;
import io.github.atengk.notify.mapper.NotifySendLogMapper;
import io.github.atengk.notify.service.NotifySendLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 通知发送日志服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class NotifySendLogServiceImpl extends ServiceImpl<NotifySendLogMapper, NotifySendLog>
        implements NotifySendLogService {

    /**
     * 记录发送成功
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @param channel 渠道
     */
    @Override
    public void recordSuccess(Long messageId, Long userId, String channel) {
        NotifySendLog sendLog = new NotifySendLog();
        sendLog.setMessageId(messageId);
        sendLog.setUserId(userId);
        sendLog.setChannel(channel);
        sendLog.setSendStatus(NotifySendStatusEnum.SUCCESS.getCode());
        sendLog.setRetryCount(0);
        sendLog.setMaxRetryCount(3);
        sendLog.setTenantId(0L);
        this.save(sendLog);

        log.info("记录通知发送成功，messageId={}，userId={}，channel={}", messageId, userId, channel);
    }

    /**
     * 记录发送失败
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @param channel 渠道
     * @param failReason 失败原因
     */
    @Override
    public void recordFail(Long messageId, Long userId, String channel, String failReason) {
        NotifySendLog sendLog = new NotifySendLog();
        sendLog.setMessageId(messageId);
        sendLog.setUserId(userId);
        sendLog.setChannel(channel);
        sendLog.setSendStatus(NotifySendStatusEnum.FAIL.getCode());
        sendLog.setRetryCount(0);
        sendLog.setMaxRetryCount(3);
        sendLog.setFailReason(StrUtil.maxLength(failReason, 512));
        sendLog.setNextRetryTime(LocalDateTime.now().plusMinutes(5));
        sendLog.setTenantId(0L);
        this.save(sendLog);

        log.warn("记录通知发送失败，messageId={}，userId={}，channel={}，原因={}",
                messageId, userId, channel, failReason);
    }
}
```

到这里，消息创建和异步推送主链路已经完整：

```text
POST /api/notify/send
-> 查询模板
-> 渲染标题和内容
-> 写入 notify_message
-> 写入 notify_user_message
-> 投递 RabbitMQ
-> 消费者推送 WebSocket
-> 写入 notify_send_log
```

最小验证方式如下：

```bash
# 1. 先建立 WebSocket 连接
# ws://localhost:8080/ws/notify?userId=10001

# 2. 调用通知发送接口
curl -X POST "http://localhost:8080/api/notify/send" \
  -H "Content-Type: application/json" \
  -d '{
    "templateCode": "APPROVAL_PASS",
    "receiverUserIds": [10001],
    "variables": {
      "approvalName": "采购申请单",
      "operatorName": "张三"
    },
    "sourceType": "APPROVAL",
    "sourceId": "AP202605150001"
  }'

# 3. 查询数据库确认消息落库
SELECT * FROM notify_message ORDER BY id DESC LIMIT 1;
SELECT * FROM notify_user_message ORDER BY id DESC LIMIT 1;
SELECT * FROM notify_send_log ORDER BY id DESC LIMIT 1;
```

## WebSocket 实时推送

WebSocket 负责把消息实时推送给在线用户。站内信是否能查询，不依赖 WebSocket 是否成功；WebSocket 只是实时提醒能力，真正的消息可靠性由 `notify_message` 和 `notify_user_message` 两张表保证。

### 用户连接管理

用户连接管理的核心是维护 `userId -> WebSocketSession` 的映射关系。一个用户可能在多个浏览器、多个标签页、多个设备同时在线，所以这里使用 `Set<WebSocketSession>` 保存多个连接。

前面已经给出 `NotifyWebSocketSessionManager` 的基础版本，这里补齐一个更适合后续使用的完整版本。

文件位置：`src/main/java/io/github/atengk/notify/websocket/NotifyWebSocketSessionManager.java`

下面的代码用于管理用户 WebSocket 会话，并支持向指定用户主动推送文本消息。

```java
package io.github.atengk.notify.websocket;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 通知 WebSocket 会话管理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class NotifyWebSocketSessionManager {

    private final ConcurrentMap<Long, Set<WebSocketSession>> userSessionMap = new ConcurrentHashMap<>();

    /**
     * 添加用户连接
     *
     * @param userId 用户ID
     * @param session WebSocket 会话
     */
    public void addSession(Long userId, WebSocketSession session) {
        userSessionMap.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("用户建立 WebSocket 连接，userId={}，sessionId={}", userId, session.getId());
    }

    /**
     * 移除用户连接
     *
     * @param userId 用户ID
     * @param session WebSocket 会话
     */
    public void removeSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = userSessionMap.get(userId);
        if (CollUtil.isEmpty(sessions)) {
            return;
        }

        sessions.remove(session);
        if (CollUtil.isEmpty(sessions)) {
            userSessionMap.remove(userId);
        }

        log.info("用户断开 WebSocket 连接，userId={}，sessionId={}", userId, session.getId());
    }

    /**
     * 获取用户连接
     *
     * @param userId 用户ID
     * @return WebSocket 会话集合
     */
    public Set<WebSocketSession> getSessions(Long userId) {
        return userSessionMap.getOrDefault(userId, Set.of());
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isOnline(Long userId) {
        return CollUtil.isNotEmpty(userSessionMap.get(userId));
    }

    /**
     * 向指定用户发送文本消息
     *
     * @param userId 用户ID
     * @param payload 消息内容
     * @return 是否至少推送成功一个连接
     */
    public boolean sendText(Long userId, String payload) {
        Set<WebSocketSession> sessions = this.getSessions(userId);
        if (CollUtil.isEmpty(sessions)) {
            log.info("用户当前不在线，跳过 WebSocket 推送，userId={}", userId);
            return false;
        }

        boolean success = false;
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                    success = true;
                }
            } catch (Exception ex) {
                log.warn("WebSocket 推送失败，userId={}，sessionId={}，原因={}",
                        userId, session.getId(), ex.getMessage());
            }
        }

        return success;
    }
}
```

用户连接时，示例地址如下：

```text
ws://localhost:8080/ws/notify?userId=10001
```

生产环境建议不要直接使用 URL 参数中的 `userId`，而是使用 Sa-Token、JWT 或网关鉴权后解析当前登录用户。

### 单用户消息推送

单用户消息推送一般发生在 MQ 消费端。消费者取出消息正文后，构建 WebSocket 推送对象，再调用 `NotifyWebSocketSessionManager#sendText` 推送给用户。

文件位置：`src/main/java/io/github/atengk/notify/service/NotifyPushService.java`

下面定义一个独立的推送服务，避免把推送逻辑堆在 MQ Consumer 中。

```java
package io.github.atengk.notify.service;

import io.github.atengk.notify.entity.NotifyMessage;

/**
 * 通知推送服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface NotifyPushService {

    /**
     * 推送单个用户消息
     *
     * @param notifyMessage 消息记录
     * @param userId 用户ID
     * @return 是否推送成功
     */
    boolean pushToUser(NotifyMessage notifyMessage, Long userId);
}
```

文件位置：`src/main/java/io/github/atengk/notify/service/impl/NotifyPushServiceImpl.java`

下面实现单用户 WebSocket 推送，并把消息结构转换成前端更容易处理的 JSON。

```java
package io.github.atengk.notify.service.impl;

import cn.hutool.json.JSONUtil;
import io.github.atengk.notify.entity.NotifyMessage;
import io.github.atengk.notify.service.NotifyPushService;
import io.github.atengk.notify.vo.NotifyPushVO;
import io.github.atengk.notify.websocket.NotifyWebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 通知推送服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyPushServiceImpl implements NotifyPushService {

    private final NotifyWebSocketSessionManager sessionManager;

    /**
     * 推送单个用户消息
     *
     * @param notifyMessage 消息记录
     * @param userId 用户ID
     * @return 是否推送成功
     */
    @Override
    public boolean pushToUser(NotifyMessage notifyMessage, Long userId) {
        NotifyPushVO pushVO = new NotifyPushVO();
        pushVO.setMessageId(notifyMessage.getId());
        pushVO.setTitle(notifyMessage.getTitle());
        pushVO.setContent(notifyMessage.getContent());
        pushVO.setMessageType(notifyMessage.getMessageType());
        pushVO.setSourceType(notifyMessage.getSourceType());
        pushVO.setSourceId(notifyMessage.getSourceId());
        pushVO.setPushTime(LocalDateTime.now());

        boolean pushed = sessionManager.sendText(userId, JSONUtil.toJsonStr(pushVO));
        if (pushed) {
            log.info("单用户 WebSocket 推送成功，messageId={}，userId={}", notifyMessage.getId(), userId);
        } else {
            log.info("单用户 WebSocket 推送未送达，messageId={}，userId={}", notifyMessage.getId(), userId);
        }

        return pushed;
    }
}
```

对应的推送消息结构如下：

```json
{
  "messageId": 1,
  "title": "审批通过通知",
  "content": "你的采购申请单已由张三审批通过。",
  "messageType": "BUSINESS",
  "sourceType": "APPROVAL",
  "sourceId": "AP202605150001",
  "pushTime": "2026-05-15T10:30:00"
}
```

MQ 消费者中可以将原来的 `sessionManager.sendText(...)` 替换为：

```java
boolean pushed = notifyPushService.pushToUser(notifyMessage, userId);
```

这样后续扩展短信、邮件、App Push 时，推送逻辑也更容易拆分。

### 离线用户消息落库

本案例的离线消息策略很简单：消息创建阶段已经写入 `notify_user_message`，所以用户离线时不需要额外写一张离线消息表。用户下次打开消息中心时，直接分页查询自己的未读消息即可。

也就是说，消息可靠性链路是：

```text
创建 notify_message
-> 创建 notify_user_message
-> 尝试 WebSocket 推送
-> 推送失败只影响实时提醒
-> 用户仍然可以在站内信列表查询消息
```

离线用户的 WebSocket 推送失败会记录到 `notify_send_log`：

```text
channel = WEBSOCKET
send_status = FAIL
fail_reason = 用户不在线或连接不可用
next_retry_time = 当前时间 + 5 分钟
```

如果你希望用户上线后立即补推未读消息，可以在 `afterConnectionEstablished` 中增加“查询最近未读消息并推送”的逻辑。但普通业务系统通常不建议一上线就推送大量历史消息，前端进入消息中心后分页查询更稳妥。

## 用户消息中心接口

用户消息中心接口用于前端展示站内信列表、未读数量、已读操作和删除操作。这里的所有接口都以当前登录用户为范围，不能允许用户操作别人的消息。

为了让代码独立可运行，下面先用请求头 `X-User-Id` 获取当前用户。生产环境应替换为 Sa-Token：

```java
StpUtil.getLoginIdAsLong();
```

### 分页查询站内信

分页查询需要关联 `notify_user_message` 和 `notify_message` 两张表。用户消息表负责筛选用户、已读状态和删除状态；消息主表负责返回标题、正文、来源信息和创建时间。

文件位置：`src/main/java/io/github/atengk/notify/dto/NotifyMessagePageDTO.java`

```java
package io.github.atengk.notify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户消息分页查询请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class NotifyMessagePageDTO {

    @Min(value = 1, message = "页码不能小于1")
    private Long pageNum = 1L;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能大于100")
    private Long pageSize = 10L;

    /**
     * 已读状态：0未读 1已读；为空查询全部
     */
    private Integer readStatus;
}
```

文件位置：`src/main/java/io/github/atengk/notify/vo/NotifyMessageVO.java`

```java
package io.github.atengk.notify.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户消息响应对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class NotifyMessageVO {

    private Long userMessageId;

    private Long messageId;

    private String title;

    private String content;

    private String messageType;

    private String sourceType;

    private String sourceId;

    private Integer readStatus;

    private LocalDateTime readTime;

    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/notify/mapper/NotifyUserMessageMapper.java`

下面在原有 `BaseMapper` 基础上增加分页查询方法。

```java
package io.github.atengk.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.notify.entity.NotifyUserMessage;
import io.github.atengk.notify.vo.NotifyMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户消息 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface NotifyUserMessageMapper extends BaseMapper<NotifyUserMessage> {

    /**
     * 分页查询用户消息
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param readStatus 已读状态
     * @return 用户消息分页
     */
    @Select("""
            <script>
            SELECT
                um.id AS userMessageId,
                m.id AS messageId,
                m.title AS title,
                m.content AS content,
                m.message_type AS messageType,
                m.source_type AS sourceType,
                m.source_id AS sourceId,
                um.read_status AS readStatus,
                um.read_time AS readTime,
                um.create_time AS createTime
            FROM notify_user_message um
            INNER JOIN notify_message m ON um.message_id = m.id
            WHERE um.user_id = #{userId}
              AND um.deleted = 0
              AND m.status = 'NORMAL'
              AND (m.expire_time IS NULL OR m.expire_time &gt; NOW())
            <if test="readStatus != null">
              AND um.read_status = #{readStatus}
            </if>
            ORDER BY um.create_time DESC
            </script>
            """)
    Page<NotifyMessageVO> selectUserMessagePage(Page<NotifyMessageVO> page,
                                                @Param("userId") Long userId,
                                                @Param("readStatus") Integer readStatus);
}
```

如果项目未启用 MyBatis-Plus 分页插件，需要添加配置。

文件位置：`src/main/java/io/github/atengk/notify/config/MybatisPlusConfig.java`

下面启用 MyBatis-Plus 分页插件，用于消息列表分页查询。

```java
package io.github.atengk.notify.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册分页插件
     *
     * @return MybatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

### 查询未读消息数量

未读数量可以直接查库，也可以使用 Redis 缓存。本案例先使用数据库查询，保证实现简单可靠。后续消息量大时，可以在创建用户消息和标记已读时同步维护 Redis 计数。

文件位置：`src/main/java/io/github/atengk/notify/service/NotifyMessageService.java`

```java
package io.github.atengk.notify.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.notify.dto.NotifyMessagePageDTO;
import io.github.atengk.notify.vo.NotifyMessageVO;

/**
 * 用户消息服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface NotifyMessageService {

    /**
     * 分页查询我的站内信
     *
     * @param userId 用户ID
     * @param dto 查询参数
     * @return 消息分页
     */
    Page<NotifyMessageVO> pageMyMessages(Long userId, NotifyMessagePageDTO dto);

    /**
     * 查询我的未读消息数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    Long countUnread(Long userId);

    /**
     * 标记单条消息已读
     *
     * @param userId 用户ID
     * @param userMessageId 用户消息ID
     */
    void readOne(Long userId, Long userMessageId);

    /**
     * 批量标记全部消息已读
     *
     * @param userId 用户ID
     */
    void readAll(Long userId);

    /**
     * 删除我的消息
     *
     * @param userId 用户ID
     * @param userMessageId 用户消息ID
     */
    void deleteOne(Long userId, Long userMessageId);
}
```

文件位置：`src/main/java/io/github/atengk/notify/service/impl/NotifyMessageServiceImpl.java`

下面实现用户消息分页、未读数量、已读和删除操作。所有更新都带上 `userId` 条件，避免越权操作。

```java
package io.github.atengk.notify.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.notify.common.enums.NotifyReadStatusEnum;
import io.github.atengk.notify.dto.NotifyMessagePageDTO;
import io.github.atengk.notify.entity.NotifyUserMessage;
import io.github.atengk.notify.mapper.NotifyUserMessageMapper;
import io.github.atengk.notify.service.NotifyMessageService;
import io.github.atengk.notify.vo.NotifyMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户消息服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyMessageServiceImpl implements NotifyMessageService {

    private final NotifyUserMessageMapper notifyUserMessageMapper;

    /**
     * 分页查询我的站内信
     *
     * @param userId 用户ID
     * @param dto 查询参数
     * @return 消息分页
     */
    @Override
    public Page<NotifyMessageVO> pageMyMessages(Long userId, NotifyMessagePageDTO dto) {
        Page<NotifyMessageVO> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        return notifyUserMessageMapper.selectUserMessagePage(page, userId, dto.getReadStatus());
    }

    /**
     * 查询我的未读消息数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    @Override
    public Long countUnread(Long userId) {
        return notifyUserMessageMapper.selectCount(Wrappers.<NotifyUserMessage>lambdaQuery()
                .eq(NotifyUserMessage::getUserId, userId)
                .eq(NotifyUserMessage::getReadStatus, NotifyReadStatusEnum.UNREAD.getCode())
                .eq(NotifyUserMessage::getDeleted, 0));
    }

    /**
     * 标记单条消息已读
     *
     * @param userId 用户ID
     * @param userMessageId 用户消息ID
     */
    @Override
    public void readOne(Long userId, Long userMessageId) {
        NotifyUserMessage userMessage = notifyUserMessageMapper.selectOne(Wrappers.<NotifyUserMessage>lambdaQuery()
                .eq(NotifyUserMessage::getId, userMessageId)
                .eq(NotifyUserMessage::getUserId, userId)
                .eq(NotifyUserMessage::getDeleted, 0)
                .last("LIMIT 1"));

        if (ObjectUtil.isNull(userMessage)) {
            throw new IllegalArgumentException("消息不存在或无权操作");
        }

        if (NotifyReadStatusEnum.READ.getCode().equals(userMessage.getReadStatus())) {
            log.info("消息已是已读状态，无需重复处理，userId={}，userMessageId={}", userId, userMessageId);
            return;
        }

        NotifyUserMessage update = new NotifyUserMessage();
        update.setId(userMessageId);
        update.setReadStatus(NotifyReadStatusEnum.READ.getCode());
        update.setReadTime(LocalDateTime.now());
        notifyUserMessageMapper.updateById(update);

        log.info("标记单条消息已读成功，userId={}，userMessageId={}", userId, userMessageId);
    }

    /**
     * 批量标记全部消息已读
     *
     * @param userId 用户ID
     */
    @Override
    public void readAll(Long userId) {
        NotifyUserMessage update = new NotifyUserMessage();
        update.setReadStatus(NotifyReadStatusEnum.READ.getCode());
        update.setReadTime(LocalDateTime.now());

        int updated = notifyUserMessageMapper.update(update, Wrappers.<NotifyUserMessage>lambdaUpdate()
                .eq(NotifyUserMessage::getUserId, userId)
                .eq(NotifyUserMessage::getReadStatus, NotifyReadStatusEnum.UNREAD.getCode())
                .eq(NotifyUserMessage::getDeleted, 0));

        log.info("批量标记消息已读完成，userId={}，updated={}", userId, updated);
    }

    /**
     * 删除我的消息
     *
     * @param userId 用户ID
     * @param userMessageId 用户消息ID
     */
    @Override
    public void deleteOne(Long userId, Long userMessageId) {
        NotifyUserMessage update = new NotifyUserMessage();
        update.setDeleted(1);

        int updated = notifyUserMessageMapper.update(update, Wrappers.<NotifyUserMessage>lambdaUpdate()
                .eq(NotifyUserMessage::getId, userMessageId)
                .eq(NotifyUserMessage::getUserId, userId)
                .eq(NotifyUserMessage::getDeleted, 0));

        if (updated <= 0) {
            throw new IllegalArgumentException("消息不存在或无权操作");
        }

        log.info("删除用户消息成功，userId={}，userMessageId={}", userId, userMessageId);
    }
}
```

### 标记单条消息已读

单条已读接口用于用户点击某条消息详情后更新阅读状态。这里更新的是 `notify_user_message.id`，不是 `notify_message.id`。

文件位置：`src/main/java/io/github/atengk/notify/controller/NotifyMessageController.java`

下面提供用户消息中心的完整接口，包括分页查询、未读数量、单条已读、全部已读和删除。

```java
package io.github.atengk.notify.controller;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.notify.common.result.Result;
import io.github.atengk.notify.dto.NotifyMessagePageDTO;
import io.github.atengk.notify.service.NotifyMessageService;
import io.github.atengk.notify.vo.NotifyMessageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户消息中心接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notify/message")
public class NotifyMessageController {

    private final NotifyMessageService notifyMessageService;

    /**
     * 分页查询我的站内信
     *
     * @param dto 查询参数
     * @param request HTTP 请求
     * @return 消息分页
     */
    @GetMapping("/page")
    public Result<Page<NotifyMessageVO>> pageMessages(@Valid NotifyMessagePageDTO dto,
                                                      HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return Result.ok(notifyMessageService.pageMyMessages(userId, dto));
    }

    /**
     * 查询我的未读消息数量
     *
     * @param request HTTP 请求
     * @return 未读数量
     */
    @GetMapping("/unread")
    public Result<Long> countUnread(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return Result.ok(notifyMessageService.countUnread(userId));
    }

    /**
     * 标记单条消息已读
     *
     * @param userMessageId 用户消息ID
     * @param request HTTP 请求
     * @return 操作结果
     */
    @PutMapping("/read/{userMessageId}")
    public Result<Void> readOne(@PathVariable Long userMessageId,
                                HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notifyMessageService.readOne(userId, userMessageId);
        return Result.ok();
    }

    /**
     * 批量标记全部消息已读
     *
     * @param request HTTP 请求
     * @return 操作结果
     */
    @PutMapping("/read-all")
    public Result<Void> readAll(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notifyMessageService.readAll(userId);
        return Result.ok();
    }

    /**
     * 删除我的消息
     *
     * @param userMessageId 用户消息ID
     * @param request HTTP 请求
     * @return 操作结果
     */
    @DeleteMapping("/{userMessageId}")
    public Result<Void> deleteOne(@PathVariable Long userMessageId,
                                  HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notifyMessageService.deleteOne(userId, userMessageId);
        return Result.ok();
    }

    /**
     * 获取当前用户ID
     *
     * @param request HTTP 请求
     * @return 用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Long userId = Convert.toLong(request.getHeader("X-User-Id"), null);
        if (userId == null) {
            throw new IllegalArgumentException("请求头 X-User-Id 不能为空");
        }
        return userId;
    }
}
```

分页查询接口示例：

```bash
curl -X GET "http://localhost:8080/api/notify/message/page?pageNum=1&pageSize=10" \
  -H "X-User-Id: 10001"
```

只查询未读消息：

```bash
curl -X GET "http://localhost:8080/api/notify/message/page?pageNum=1&pageSize=10&readStatus=0" \
  -H "X-User-Id: 10001"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "userMessageId": 1,
        "messageId": 1,
        "title": "审批通过通知",
        "content": "你的采购申请单已由张三审批通过。",
        "messageType": "BUSINESS",
        "sourceType": "APPROVAL",
        "sourceId": "AP202605150001",
        "readStatus": 0,
        "readTime": null,
        "createTime": "2026-05-15T10:30:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 批量标记已读

批量标记已读适合用户点击“全部已读”。这里只更新当前用户自己的未读消息。

调用示例：

```bash
curl -X PUT "http://localhost:8080/api/notify/message/read-all" \
  -H "X-User-Id: 10001"
```

单条标记已读调用示例：

```bash
curl -X PUT "http://localhost:8080/api/notify/message/read/1" \
  -H "X-User-Id: 10001"
```

查询未读数量：

```bash
curl -X GET "http://localhost:8080/api/notify/message/unread" \
  -H "X-User-Id: 10001"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 0
}
```

### 删除用户消息

删除用户消息使用逻辑删除，只更新 `notify_user_message.deleted = 1`，不删除 `notify_message` 主记录。这样同一条消息发给多个用户时，某个用户删除消息不会影响其他接收人。

调用示例：

```bash
curl -X DELETE "http://localhost:8080/api/notify/message/1" \
  -H "X-User-Id: 10001"
```

执行后，该用户再次查询消息列表时不会看到这条消息：

```sql
SELECT *
FROM notify_user_message
WHERE id = 1
  AND user_id = 10001;
```

预期结果中 `deleted = 1`。

这一部分完成后，用户侧消息中心已经具备完整闭环：

```text
收到通知
-> WebSocket 在线提醒
-> 消息中心分页查询
-> 查看未读数量
-> 单条标记已读
-> 全部标记已读
-> 删除个人消息
```

当前接口已经能满足审批通知、工单通知、订单通知、系统公告等常见业务场景。

## 用户通知偏好

用户通知偏好用于控制用户是否接收不同渠道的通知。本案例重点实现站内信和 WebSocket 两个开关，短信和邮件字段先预留，便于后续接入短信 SDK、邮件服务等渠道。该模块对应原场景中的“用户通知偏好”和“多渠道适配”能力。

### 查询通知偏好

通知偏好按用户维度保存。如果用户第一次查询时没有偏好记录，系统会自动创建默认偏好：开启站内信、开启 WebSocket，关闭短信和邮件。

文件位置：`src/main/java/io/github/atengk/notify/entity/NotifyUserPreference.java`

```java
package io.github.atengk.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户通知偏好实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("notify_user_preference")
public class NotifyUserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer inboxEnabled;

    private Integer websocketEnabled;

    private Integer smsEnabled;

    private Integer emailEnabled;

    private Long tenantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/notify/mapper/NotifyUserPreferenceMapper.java`

```java
package io.github.atengk.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.notify.entity.NotifyUserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知偏好 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface NotifyUserPreferenceMapper extends BaseMapper<NotifyUserPreference> {
}
```

文件位置：`src/main/java/io/github/atengk/notify/vo/NotifyPreferenceVO.java`

```java
package io.github.atengk.notify.vo;

import lombok.Data;

/**
 * 用户通知偏好响应对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class NotifyPreferenceVO {

    private Long userId;

    private Integer inboxEnabled;

    private Integer websocketEnabled;

    private Integer smsEnabled;

    private Integer emailEnabled;
}
```

文件位置：`src/main/java/io/github/atengk/notify/service/NotifyPreferenceService.java`

```java
package io.github.atengk.notify.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.notify.dto.NotifyPreferenceUpdateDTO;
import io.github.atengk.notify.entity.NotifyUserPreference;
import io.github.atengk.notify.vo.NotifyPreferenceVO;

/**
 * 用户通知偏好服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface NotifyPreferenceService extends IService<NotifyUserPreference> {

    /**
     * 查询用户通知偏好，不存在则创建默认偏好
     *
     * @param userId 用户ID
     * @return 通知偏好
     */
    NotifyPreferenceVO getPreference(Long userId);

    /**
     * 修改用户通知偏好
     *
     * @param userId 用户ID
     * @param dto 修改请求
     */
    void updatePreference(Long userId, NotifyPreferenceUpdateDTO dto);

    /**
     * 判断是否开启站内信
     *
     * @param userId 用户ID
     * @return 是否开启
     */
    boolean isInboxEnabled(Long userId);

    /**
     * 判断是否开启 WebSocket 推送
     *
     * @param userId 用户ID
     * @return 是否开启
     */
    boolean isWebSocketEnabled(Long userId);
}
```

下面实现查询偏好、默认偏好创建和渠道开关判断。这里使用 Hutool 的 `BeanUtil` 简化对象转换。

文件位置：`src/main/java/io/github/atengk/notify/service/impl/NotifyPreferenceServiceImpl.java`

```java
package io.github.atengk.notify.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.notify.dto.NotifyPreferenceUpdateDTO;
import io.github.atengk.notify.entity.NotifyUserPreference;
import io.github.atengk.notify.mapper.NotifyUserPreferenceMapper;
import io.github.atengk.notify.service.NotifyPreferenceService;
import io.github.atengk.notify.vo.NotifyPreferenceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户通知偏好服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class NotifyPreferenceServiceImpl extends ServiceImpl<NotifyUserPreferenceMapper, NotifyUserPreference>
        implements NotifyPreferenceService {

    /**
     * 查询用户通知偏好，不存在则创建默认偏好
     *
     * @param userId 用户ID
     * @return 通知偏好
     */
    @Override
    public NotifyPreferenceVO getPreference(Long userId) {
        NotifyUserPreference preference = this.getOrCreatePreference(userId);
        return BeanUtil.copyProperties(preference, NotifyPreferenceVO.class);
    }

    /**
     * 修改用户通知偏好
     *
     * @param userId 用户ID
     * @param dto 修改请求
     */
    @Override
    public void updatePreference(Long userId, NotifyPreferenceUpdateDTO dto) {
        NotifyUserPreference preference = this.getOrCreatePreference(userId);

        NotifyUserPreference update = new NotifyUserPreference();
        update.setId(preference.getId());
        update.setInboxEnabled(dto.getInboxEnabled());
        update.setWebsocketEnabled(dto.getWebsocketEnabled());
        update.setSmsEnabled(dto.getSmsEnabled());
        update.setEmailEnabled(dto.getEmailEnabled());
        this.updateById(update);

        log.info("修改用户通知偏好成功，userId={}，inboxEnabled={}，websocketEnabled={}",
                userId, dto.getInboxEnabled(), dto.getWebsocketEnabled());
    }

    /**
     * 判断是否开启站内信
     *
     * @param userId 用户ID
     * @return 是否开启
     */
    @Override
    public boolean isInboxEnabled(Long userId) {
        NotifyUserPreference preference = this.getOrCreatePreference(userId);
        return Integer.valueOf(1).equals(preference.getInboxEnabled());
    }

    /**
     * 判断是否开启 WebSocket 推送
     *
     * @param userId 用户ID
     * @return 是否开启
     */
    @Override
    public boolean isWebSocketEnabled(Long userId) {
        NotifyUserPreference preference = this.getOrCreatePreference(userId);
        return Integer.valueOf(1).equals(preference.getWebsocketEnabled());
    }

    /**
     * 获取或创建默认偏好
     *
     * @param userId 用户ID
     * @return 用户通知偏好
     */
    private NotifyUserPreference getOrCreatePreference(Long userId) {
        NotifyUserPreference preference = this.getOne(Wrappers.<NotifyUserPreference>lambdaQuery()
                .eq(NotifyUserPreference::getUserId, userId)
                .eq(NotifyUserPreference::getTenantId, 0L)
                .last("LIMIT 1"));

        if (ObjectUtil.isNotNull(preference)) {
            return preference;
        }

        NotifyUserPreference defaultPreference = new NotifyUserPreference();
        defaultPreference.setUserId(userId);
        defaultPreference.setInboxEnabled(1);
        defaultPreference.setWebsocketEnabled(1);
        defaultPreference.setSmsEnabled(0);
        defaultPreference.setEmailEnabled(0);
        defaultPreference.setTenantId(0L);
        this.save(defaultPreference);

        log.info("创建用户默认通知偏好，userId={}", userId);
        return defaultPreference;
    }
}
```

### 修改通知偏好

修改通知偏好时，前端直接传四个渠道开关。这里为了保持接口简单，使用 `0/1` 表示关闭和开启。

文件位置：`src/main/java/io/github/atengk/notify/dto/NotifyPreferenceUpdateDTO.java`

```java
package io.github.atengk.notify.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改通知偏好请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class NotifyPreferenceUpdateDTO {

    @NotNull(message = "站内信开关不能为空")
    private Integer inboxEnabled;

    @NotNull(message = "WebSocket 开关不能为空")
    private Integer websocketEnabled;

    @NotNull(message = "短信开关不能为空")
    private Integer smsEnabled;

    @NotNull(message = "邮件开关不能为空")
    private Integer emailEnabled;
}
```

文件位置：`src/main/java/io/github/atengk/notify/controller/NotifyPreferenceController.java`

下面提供查询和修改通知偏好的接口。这里继续使用请求头 `X-User-Id` 模拟当前登录用户，真实项目中建议替换为 Sa-Token。

```java
package io.github.atengk.notify.controller;

import cn.hutool.core.convert.Convert;
import io.github.atengk.notify.common.result.Result;
import io.github.atengk.notify.dto.NotifyPreferenceUpdateDTO;
import io.github.atengk.notify.service.NotifyPreferenceService;
import io.github.atengk.notify.vo.NotifyPreferenceVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户通知偏好接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notify/preference")
public class NotifyPreferenceController {

    private final NotifyPreferenceService notifyPreferenceService;

    /**
     * 查询我的通知偏好
     *
     * @param request HTTP 请求
     * @return 通知偏好
     */
    @GetMapping
    public Result<NotifyPreferenceVO> getPreference(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        return Result.ok(notifyPreferenceService.getPreference(userId));
    }

    /**
     * 修改我的通知偏好
     *
     * @param dto 修改请求
     * @param request HTTP 请求
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> updatePreference(@Valid @RequestBody NotifyPreferenceUpdateDTO dto,
                                         HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notifyPreferenceService.updatePreference(userId, dto);
        return Result.ok();
    }

    /**
     * 获取当前用户ID
     *
     * @param request HTTP 请求
     * @return 用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Long userId = Convert.toLong(request.getHeader("X-User-Id"), null);
        if (userId == null) {
            throw new IllegalArgumentException("请求头 X-User-Id 不能为空");
        }
        return userId;
    }
}
```

查询通知偏好：

```bash
curl -X GET "http://localhost:8080/api/notify/preference" \
  -H "X-User-Id: 10001"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 10001,
    "inboxEnabled": 1,
    "websocketEnabled": 1,
    "smsEnabled": 0,
    "emailEnabled": 0
  }
}
```

修改通知偏好：

```bash
curl -X PUT "http://localhost:8080/api/notify/preference" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 10001" \
  -d '{
    "inboxEnabled": 1,
    "websocketEnabled": 0,
    "smsEnabled": 0,
    "emailEnabled": 0
  }'
```

### 发送前偏好校验

本案例建议采用两个层面的偏好校验：

```text
创建用户消息前：判断 inboxEnabled，关闭站内信则不创建 notify_user_message
执行实时推送前：判断 websocketEnabled，关闭 WebSocket 则不推送
```

如果你的业务要求“所有系统通知都必须保留站内信”，可以忽略 `inboxEnabled`，只用 `websocketEnabled` 控制实时推送。本案例给出更完整的实现方式。

在 `NotifySendServiceImpl` 中，注入 `NotifyPreferenceService`：

```java
private final NotifyPreferenceService notifyPreferenceService;
```

然后将原来的用户消息构建逻辑：

```java
List<NotifyUserMessage> userMessages = dto.getReceiverUserIds()
        .stream()
        .distinct()
        .map(userId -> buildUserMessage(message.getId(), userId))
        .toList();
```

替换为下面的偏好过滤逻辑：

```java
List<NotifyUserMessage> userMessages = dto.getReceiverUserIds()
        .stream()
        .distinct()
        .filter(userId -> {
            boolean enabled = notifyPreferenceService.isInboxEnabled(userId);
            if (!enabled) {
                log.info("用户关闭站内信，跳过消息创建，userId={}，templateCode={}",
                        userId, dto.getTemplateCode());
            }
            return enabled;
        })
        .map(userId -> buildUserMessage(message.getId(), userId))
        .toList();

if (CollUtil.isEmpty(userMessages)) {
    log.info("所有接收人均关闭站内信，本次通知不创建用户消息，messageId={}", message.getId());
    return message.getId();
}
```

在 `NotifyConsumer` 中，注入 `NotifyPreferenceService`：

```java
private final NotifyPreferenceService notifyPreferenceService;
```

然后在推送前增加 WebSocket 偏好判断：

```java
private void pushWebSocketMessage(NotifyMessage notifyMessage, Long userId) {
    if (!notifyPreferenceService.isWebSocketEnabled(userId)) {
        notifySendLogService.recordFail(
                notifyMessage.getId(),
                userId,
                NotifyChannelEnum.WEBSOCKET.getCode(),
                "用户已关闭 WebSocket 推送"
        );
        log.info("用户关闭 WebSocket 推送，跳过实时通知，messageId={}，userId={}",
                notifyMessage.getId(), userId);
        return;
    }

    boolean pushed = notifyPushService.pushToUser(notifyMessage, userId);
    if (pushed) {
        notifySendLogService.recordSuccess(
                notifyMessage.getId(),
                userId,
                NotifyChannelEnum.WEBSOCKET.getCode()
        );
        return;
    }

    notifySendLogService.recordFail(
            notifyMessage.getId(),
            userId,
            NotifyChannelEnum.WEBSOCKET.getCode(),
            "用户不在线或连接不可用"
    );
}
```

这里的处理结果是：

```text
关闭站内信：不产生用户消息记录
关闭 WebSocket：不实时推送，但可保留站内信
用户离线：站内信保留，WebSocket 日志记录失败
用户在线且开启 WebSocket：实时推送成功并记录成功日志
```

## 失败重试与过期清理

失败重试用于处理 WebSocket 短暂不可用、用户短暂离线、外部渠道调用失败等情况。过期清理用于将超过有效期的消息标记为过期，避免长期展示无效通知。

### 失败消息重试任务

失败消息重试任务扫描 `notify_send_log` 中满足条件的数据：

```text
send_status = FAIL
retry_count < max_retry_count
next_retry_time <= 当前时间
```

如果重试成功，则更新为 `SUCCESS`；如果重试失败，则增加 `retry_count` 并设置下一次重试时间。

先扩展发送日志服务。

文件位置：`src/main/java/io/github/atengk/notify/service/NotifySendLogService.java`

在原接口中追加下面方法：

```java
/**
 * 重试失败发送日志
 *
 * @param limit 每次处理数量
 */
void retryFailLogs(int limit);
```

文件位置：`src/main/java/io/github/atengk/notify/service/impl/NotifySendLogServiceImpl.java`

下面实现失败日志扫描和 WebSocket 重试。短信、邮件可以后续在 `retryOneLog` 中按 `channel` 扩展。

```java
package io.github.atengk.notify.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.notify.common.enums.NotifyChannelEnum;
import io.github.atengk.notify.common.enums.NotifySendStatusEnum;
import io.github.atengk.notify.entity.NotifyMessage;
import io.github.atengk.notify.entity.NotifySendLog;
import io.github.atengk.notify.mapper.NotifyMessageMapper;
import io.github.atengk.notify.mapper.NotifySendLogMapper;
import io.github.atengk.notify.service.NotifyPushService;
import io.github.atengk.notify.service.NotifySendLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知发送日志服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifySendLogServiceImpl extends ServiceImpl<NotifySendLogMapper, NotifySendLog>
        implements NotifySendLogService {

    private final NotifyMessageMapper notifyMessageMapper;

    private final NotifyPushService notifyPushService;

    /**
     * 记录发送成功
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @param channel 渠道
     */
    @Override
    public void recordSuccess(Long messageId, Long userId, String channel) {
        NotifySendLog sendLog = new NotifySendLog();
        sendLog.setMessageId(messageId);
        sendLog.setUserId(userId);
        sendLog.setChannel(channel);
        sendLog.setSendStatus(NotifySendStatusEnum.SUCCESS.getCode());
        sendLog.setRetryCount(0);
        sendLog.setMaxRetryCount(3);
        sendLog.setTenantId(0L);
        this.save(sendLog);

        log.info("记录通知发送成功，messageId={}，userId={}，channel={}", messageId, userId, channel);
    }

    /**
     * 记录发送失败
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     * @param channel 渠道
     * @param failReason 失败原因
     */
    @Override
    public void recordFail(Long messageId, Long userId, String channel, String failReason) {
        NotifySendLog sendLog = new NotifySendLog();
        sendLog.setMessageId(messageId);
        sendLog.setUserId(userId);
        sendLog.setChannel(channel);
        sendLog.setSendStatus(NotifySendStatusEnum.FAIL.getCode());
        sendLog.setRetryCount(0);
        sendLog.setMaxRetryCount(3);
        sendLog.setFailReason(StrUtil.maxLength(failReason, 512));
        sendLog.setNextRetryTime(LocalDateTime.now().plusMinutes(5));
        sendLog.setTenantId(0L);
        this.save(sendLog);

        log.warn("记录通知发送失败，messageId={}，userId={}，channel={}，原因={}",
                messageId, userId, channel, failReason);
    }

    /**
     * 重试失败发送日志
     *
     * @param limit 每次处理数量
     */
    @Override
    public void retryFailLogs(int limit) {
        List<NotifySendLog> failLogs = this.list(Wrappers.<NotifySendLog>lambdaQuery()
                .eq(NotifySendLog::getSendStatus, NotifySendStatusEnum.FAIL.getCode())
                .le(NotifySendLog::getNextRetryTime, LocalDateTime.now())
                .apply("retry_count < max_retry_count")
                .orderByAsc(NotifySendLog::getNextRetryTime)
                .last("LIMIT " + limit));

        if (CollUtil.isEmpty(failLogs)) {
            log.info("暂无需要重试的通知发送日志");
            return;
        }

        for (NotifySendLog sendLog : failLogs) {
            retryOneLog(sendLog);
        }

        log.info("失败通知重试任务执行完成，处理数量={}", failLogs.size());
    }

    /**
     * 重试单条发送日志
     *
     * @param sendLog 发送日志
     */
    private void retryOneLog(NotifySendLog sendLog) {
        try {
            NotifyMessage notifyMessage = notifyMessageMapper.selectById(sendLog.getMessageId());
            if (notifyMessage == null) {
                updateRetryFail(sendLog, "消息记录不存在");
                return;
            }

            if (NotifyChannelEnum.WEBSOCKET.getCode().equals(sendLog.getChannel())) {
                boolean pushed = notifyPushService.pushToUser(notifyMessage, sendLog.getUserId());
                if (pushed) {
                    updateRetrySuccess(sendLog);
                    return;
                }
                updateRetryFail(sendLog, "WebSocket 重试推送失败，用户可能仍不在线");
                return;
            }

            updateRetryFail(sendLog, "暂不支持该渠道重试：" + sendLog.getChannel());
        } catch (Exception ex) {
            log.warn("通知发送日志重试异常，sendLogId={}，原因={}", sendLog.getId(), ex.getMessage());
            updateRetryFail(sendLog, ex.getMessage());
        }
    }

    /**
     * 更新重试成功
     *
     * @param sendLog 发送日志
     */
    private void updateRetrySuccess(NotifySendLog sendLog) {
        NotifySendLog update = new NotifySendLog();
        update.setId(sendLog.getId());
        update.setSendStatus(NotifySendStatusEnum.SUCCESS.getCode());
        update.setFailReason(null);
        update.setNextRetryTime(null);
        this.updateById(update);

        log.info("通知发送重试成功，sendLogId={}，messageId={}，userId={}",
                sendLog.getId(), sendLog.getMessageId(), sendLog.getUserId());
    }

    /**
     * 更新重试失败
     *
     * @param sendLog 发送日志
     * @param reason 失败原因
     */
    private void updateRetryFail(NotifySendLog sendLog, String reason) {
        int nextRetryCount = sendLog.getRetryCount() + 1;

        NotifySendLog update = new NotifySendLog();
        update.setId(sendLog.getId());
        update.setRetryCount(nextRetryCount);
        update.setFailReason(StrUtil.maxLength(reason, 512));

        if (nextRetryCount >= sendLog.getMaxRetryCount()) {
            update.setNextRetryTime(null);
            log.warn("通知发送重试达到上限，sendLogId={}，retryCount={}",
                    sendLog.getId(), nextRetryCount);
        } else {
            update.setNextRetryTime(LocalDateTime.now().plusMinutes(5));
        }

        this.updateById(update);
    }
}
```

### 过期消息清理任务

过期消息清理不建议直接物理删除消息。更稳妥的方式是将 `notify_message.status` 更新为 `EXPIRED`，这样历史数据仍可追溯，用户消息列表查询时排除过期消息即可。

文件位置：`src/main/java/io/github/atengk/notify/service/NotifyMessageExpireService.java`

```java
package io.github.atengk.notify.service;

/**
 * 消息过期处理服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface NotifyMessageExpireService {

    /**
     * 标记过期消息
     *
     * @param limit 每次处理数量
     * @return 处理数量
     */
    int expireMessages(int limit);
}
```

文件位置：`src/main/java/io/github/atengk/notify/service/impl/NotifyMessageExpireServiceImpl.java`

下面扫描已到期的正常消息，并批量标记为过期。

```java
package io.github.atengk.notify.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.notify.common.enums.NotifyMessageStatusEnum;
import io.github.atengk.notify.entity.NotifyMessage;
import io.github.atengk.notify.mapper.NotifyMessageMapper;
import io.github.atengk.notify.service.NotifyMessageExpireService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 消息过期处理服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyMessageExpireServiceImpl implements NotifyMessageExpireService {

    private final NotifyMessageMapper notifyMessageMapper;

    /**
     * 标记过期消息
     *
     * @param limit 每次处理数量
     * @return 处理数量
     */
    @Override
    public int expireMessages(int limit) {
        NotifyMessage update = new NotifyMessage();
        update.setStatus(NotifyMessageStatusEnum.EXPIRED.getCode());

        int updated = notifyMessageMapper.update(update, Wrappers.<NotifyMessage>lambdaUpdate()
                .eq(NotifyMessage::getStatus, NotifyMessageStatusEnum.NORMAL.getCode())
                .isNotNull(NotifyMessage::getExpireTime)
                .le(NotifyMessage::getExpireTime, LocalDateTime.now())
                .last("LIMIT " + limit));

        log.info("过期消息清理完成，updated={}", updated);
        return updated;
    }
}
```

注意：`UPDATE ... LIMIT` 是 MySQL 支持的写法。如果你使用 PostgreSQL，需要改成“先查 ID，再按 ID 批量更新”。

### XXL-JOB 任务实现

XXL-JOB 用于周期性执行失败重试和过期清理。这里给出执行器代码，调度中心里配置两个任务即可。

文件位置：`src/main/java/io/github/atengk/notify/job/NotifyJobHandler.java`

下面定义两个 XXL-JOB 任务：`notifyFailRetryJob` 和 `notifyExpireCleanJob`。

```java
package io.github.atengk.notify.job;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.notify.service.NotifyMessageExpireService;
import io.github.atengk.notify.service.NotifySendLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 通知中心定时任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyJobHandler {

    private final NotifySendLogService notifySendLogService;

    private final NotifyMessageExpireService notifyMessageExpireService;

    /**
     * 失败消息重试任务
     */
    @XxlJob("notifyFailRetryJob")
    public void notifyFailRetryJob() {
        int limit = parseLimit(100);
        log.info("开始执行通知失败重试任务，limit={}", limit);
        notifySendLogService.retryFailLogs(limit);
        log.info("通知失败重试任务执行结束");
    }

    /**
     * 过期消息清理任务
     */
    @XxlJob("notifyExpireCleanJob")
    public void notifyExpireCleanJob() {
        int limit = parseLimit(500);
        log.info("开始执行过期消息清理任务，limit={}", limit);
        int updated = notifyMessageExpireService.expireMessages(limit);
        log.info("过期消息清理任务执行结束，updated={}", updated);
    }

    /**
     * 解析任务参数
     *
     * @param defaultLimit 默认数量
     * @return 每次处理数量
     */
    private int parseLimit(int defaultLimit) {
        String jobParam = com.xxl.job.core.context.XxlJobHelper.getJobParam();
        if (StrUtil.isBlank(jobParam)) {
            return defaultLimit;
        }
        return Convert.toInt(jobParam, defaultLimit);
    }
}
```

如果项目还没有配置 XXL-JOB 执行器，需要补充配置。

文件位置：`src/main/resources/application.yml`

```yaml
xxl:
  job:
    admin:
      # XXL-JOB 调度中心地址，按实际环境修改
      addresses: http://localhost:8088/xxl-job-admin
    executor:
      # 当前应用在 XXL-JOB 中注册的执行器名称
      appname: notify-center-demo
      # 执行器注册地址，为空时自动获取
      address:
      # 执行器 IP，为空时自动获取
      ip:
      # 执行器端口，需保证不与业务端口冲突
      port: 9999
      # 日志文件路径
      logpath: ./logs/xxl-job
      # 日志保留天数
      logretentiondays: 30
    # 执行器通讯令牌，需要和调度中心保持一致
    accessToken: default_token
```

文件位置：`src/main/java/io/github/atengk/notify/config/XxlJobConfig.java`

下面注册 XXL-JOB 执行器。

```java
package io.github.atengk.notify.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-JOB 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobConfig {

    private Admin admin;

    private Executor executor;

    private String accessToken;

    /**
     * 创建 XXL-JOB 执行器
     *
     * @return XxlJobSpringExecutor
     */
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executorBean = new XxlJobSpringExecutor();
        executorBean.setAdminAddresses(admin.getAddresses());
        executorBean.setAppname(executor.getAppname());
        executorBean.setAddress(executor.getAddress());
        executorBean.setIp(executor.getIp());
        executorBean.setPort(executor.getPort());
        executorBean.setAccessToken(accessToken);
        executorBean.setLogPath(executor.getLogpath());
        executorBean.setLogRetentionDays(executor.getLogretentiondays());

        log.info("初始化 XXL-JOB 执行器成功，appname={}，port={}",
                executor.getAppname(), executor.getPort());
        return executorBean;
    }

    /**
     * 调度中心配置
     *
     * @author Ateng
     * @since 2026-05-15
     */
    @Data
    public static class Admin {
        private String addresses;
    }

    /**
     * 执行器配置
     *
     * @author Ateng
     * @since 2026-05-15
     */
    @Data
    public static class Executor {
        private String appname;
        private String address;
        private String ip;
        private Integer port;
        private String logpath;
        private Integer logretentiondays;
    }
}
```

在 XXL-JOB 调度中心中创建两个任务：

| 任务描述     | JobHandler             | 建议调度周期   | 任务参数 |
| ------------ | ---------------------- | -------------- | -------- |
| 通知失败重试 | `notifyFailRetryJob`   | 每 5 分钟一次  | `100`    |
| 过期消息清理 | `notifyExpireCleanJob` | 每 30 分钟一次 | `500`    |

也可以直接通过数据库验证任务效果。

准备一条失败日志：

```sql
UPDATE notify_send_log
SET send_status = 'FAIL',
    retry_count = 0,
    max_retry_count = 3,
    next_retry_time = NOW()
WHERE id = 1;
```

准备一条过期消息：

```sql
UPDATE notify_message
SET status = 'NORMAL',
    expire_time = DATE_SUB(NOW(), INTERVAL 1 MINUTE)
WHERE id = 1;
```

执行任务后验证：

```sql
SELECT id, send_status, retry_count, fail_reason, next_retry_time
FROM notify_send_log
WHERE id = 1;

SELECT id, status, expire_time
FROM notify_message
WHERE id = 1;
```

到这里，用户通知偏好、失败重试和过期清理已经形成闭环：

```text
用户维护通知偏好
-> 创建消息时判断站内信开关
-> MQ 消费时判断 WebSocket 开关
-> 推送失败写入发送日志
-> XXL-JOB 定时重试失败日志
-> XXL-JOB 定时标记过期消息
```

## 接口测试与验证

本章节用于把前面实现的消息模板、消息创建、MQ 消费、WebSocket 推送、已读未读、失败重试串起来验证。测试顺序建议按照“模板准备 -> 建立 WebSocket -> 创建通知 -> 查询消息 -> 已读操作 -> 失败重试”的顺序执行。该场景对应原 README 中第 24 项“站内信 / 消息通知中心”的核心流程。

### 创建通知消息测试

创建通知前，先确认数据库中已经存在可用模板。如果没有模板，先执行模板初始化 SQL。

```sql
-- 查询模板是否存在
SELECT id, template_code, template_name, enabled
FROM notify_template
WHERE template_code = 'APPROVAL_PASS';

-- 不存在时初始化审批通过模板
INSERT INTO notify_template
(template_code, template_name, title_template, content_template, channel, enabled, remark, tenant_id)
VALUES
('APPROVAL_PASS', '审批通过通知', '审批通过通知', '你的${approvalName}已由${operatorName}审批通过。', 'INBOX', 1, '审批业务通知', 0);
```

启动 MySQL、Redis、RabbitMQ 和后端服务后，调用创建通知接口。

```bash
curl -X POST "http://localhost:8080/api/notify/send" \
  -H "Content-Type: application/json" \
  -d '{
    "templateCode": "APPROVAL_PASS",
    "receiverUserIds": [10001, 10002],
    "variables": {
      "approvalName": "采购申请单",
      "operatorName": "张三"
    },
    "messageType": "BUSINESS",
    "sourceType": "APPROVAL",
    "sourceId": "AP202605150001"
  }'
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1
}
```

调用成功后，验证消息主表和用户消息表。

```sql
-- 验证消息主记录
SELECT id, template_code, title, content, message_type, source_type, source_id, status, create_time
FROM notify_message
ORDER BY id DESC
LIMIT 1;

-- 验证用户消息记录
SELECT id, message_id, user_id, read_status, deleted, create_time
FROM notify_user_message
ORDER BY id DESC
LIMIT 10;
```

预期结果：

```text
notify_message 中生成 1 条消息主记录
notify_user_message 中为 10001、10002 各生成 1 条用户消息记录
read_status = 0
deleted = 0
```

### WebSocket 推送测试

WebSocket 推送测试需要先建立用户连接，再发送通知。这里使用 `websocat` 举例，也可以使用 Postman、Apifox 或浏览器控制台测试。

安装 `websocat` 后，先建立用户 `10001` 的连接。

```bash
websocat "ws://localhost:8080/ws/notify?userId=10001"
```

保持连接不关闭，然后另开一个终端发送通知。

```bash
curl -X POST "http://localhost:8080/api/notify/send" \
  -H "Content-Type: application/json" \
  -d '{
    "templateCode": "APPROVAL_PASS",
    "receiverUserIds": [10001],
    "variables": {
      "approvalName": "报销申请单",
      "operatorName": "李四"
    },
    "messageType": "BUSINESS",
    "sourceType": "APPROVAL",
    "sourceId": "AP202605150002"
  }'
```

WebSocket 客户端预期收到类似消息：

```json
{
  "messageId": 2,
  "title": "审批通过通知",
  "content": "你的报销申请单已由李四审批通过。",
  "messageType": "BUSINESS",
  "sourceType": "APPROVAL",
  "sourceId": "AP202605150002",
  "pushTime": "2026-05-15T11:20:00"
}
```

也可以用浏览器控制台测试。

```javascript
const socket = new WebSocket("ws://localhost:8080/ws/notify?userId=10001");

socket.onopen = () => {
  console.log("WebSocket 已连接");
};

socket.onmessage = (event) => {
  console.log("收到通知：", JSON.parse(event.data));
};

socket.onclose = () => {
  console.log("WebSocket 已关闭");
};

socket.onerror = (error) => {
  console.error("WebSocket 异常：", error);
};
```

验证发送日志。

```sql
SELECT id, message_id, user_id, channel, send_status, retry_count, fail_reason, next_retry_time
FROM notify_send_log
ORDER BY id DESC
LIMIT 10;
```

如果用户在线且开启 WebSocket，预期结果：

```text
channel = WEBSOCKET
send_status = SUCCESS
fail_reason = NULL
```

如果用户不在线，预期结果：

```text
channel = WEBSOCKET
send_status = FAIL
fail_reason = 用户不在线或连接不可用
next_retry_time 不为空
```

### 已读未读测试

先查询用户 `10001` 的站内信列表。

```bash
curl -X GET "http://localhost:8080/api/notify/message/page?pageNum=1&pageSize=10" \
  -H "X-User-Id: 10001"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "userMessageId": 1,
        "messageId": 1,
        "title": "审批通过通知",
        "content": "你的采购申请单已由张三审批通过。",
        "messageType": "BUSINESS",
        "sourceType": "APPROVAL",
        "sourceId": "AP202605150001",
        "readStatus": 0,
        "readTime": null,
        "createTime": "2026-05-15T11:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

查询未读数量。

```bash
curl -X GET "http://localhost:8080/api/notify/message/unread" \
  -H "X-User-Id: 10001"
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1
}
```

标记单条消息已读。这里传的是 `userMessageId`，不是 `messageId`。

```bash
curl -X PUT "http://localhost:8080/api/notify/message/read/1" \
  -H "X-User-Id: 10001"
```

再次查询未读数量。

```bash
curl -X GET "http://localhost:8080/api/notify/message/unread" \
  -H "X-User-Id: 10001"
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 0
}
```

验证数据库状态。

```sql
SELECT id, user_id, message_id, read_status, read_time, deleted
FROM notify_user_message
WHERE id = 1;
```

预期结果：

```text
read_status = 1
read_time 不为空
deleted = 0
```

批量标记全部已读。

```bash
curl -X PUT "http://localhost:8080/api/notify/message/read-all" \
  -H "X-User-Id: 10001"
```

删除用户消息。

```bash
curl -X DELETE "http://localhost:8080/api/notify/message/1" \
  -H "X-User-Id: 10001"
```

验证删除结果。

```sql
SELECT id, user_id, message_id, read_status, deleted
FROM notify_user_message
WHERE id = 1;
```

预期结果：

```text
deleted = 1
```

### 失败重试测试

失败重试主要验证 `notify_send_log` 中失败的 WebSocket 推送记录能否被 XXL-JOB 重新推送。

先模拟用户不在线。不要建立 WebSocket 连接，直接发送消息。

```bash
curl -X POST "http://localhost:8080/api/notify/send" \
  -H "Content-Type: application/json" \
  -d '{
    "templateCode": "APPROVAL_PASS",
    "receiverUserIds": [10003],
    "variables": {
      "approvalName": "请假申请单",
      "operatorName": "王五"
    },
    "messageType": "BUSINESS",
    "sourceType": "APPROVAL",
    "sourceId": "AP202605150003"
  }'
```

查询失败日志。

```sql
SELECT id, message_id, user_id, channel, send_status, retry_count, max_retry_count, fail_reason, next_retry_time
FROM notify_send_log
WHERE user_id = 10003
ORDER BY id DESC
LIMIT 1;
```

预期结果：

```text
send_status = FAIL
retry_count = 0
next_retry_time 不为空
```

为了立即测试重试，可以手动把 `next_retry_time` 改成当前时间之前。

```sql
UPDATE notify_send_log
SET next_retry_time = NOW()
WHERE user_id = 10003
  AND send_status = 'FAIL';
```

然后建立用户 `10003` 的 WebSocket 连接。

```bash
websocat "ws://localhost:8080/ws/notify?userId=10003"
```

在 XXL-JOB 调度中心手动执行任务：

```text
JobHandler：notifyFailRetryJob
任务参数：100
```

执行后，WebSocket 客户端应该收到补推消息。再次查询发送日志。

```sql
SELECT id, message_id, user_id, channel, send_status, retry_count, fail_reason, next_retry_time
FROM notify_send_log
WHERE user_id = 10003
ORDER BY id DESC
LIMIT 1;
```

预期结果：

```text
send_status = SUCCESS
fail_reason = NULL
next_retry_time = NULL
```

过期消息清理测试可以手动制造一条已过期消息。

```sql
UPDATE notify_message
SET expire_time = DATE_SUB(NOW(), INTERVAL 1 MINUTE),
    status = 'NORMAL'
WHERE id = 1;
```

在 XXL-JOB 调度中心手动执行任务：

```text
JobHandler：notifyExpireCleanJob
任务参数：500
```

验证消息状态。

```sql
SELECT id, status, expire_time
FROM notify_message
WHERE id = 1;
```

预期结果：

```text
status = EXPIRED
```

## 总结与扩展方向

本案例实现的是消息通知中心的核心闭环：业务系统创建通知，消息中心完成模板渲染、站内信落库、MQ 异步投递、WebSocket 实时推送、用户消息查询、已读未读处理、失败重试和过期清理。

### 当前案例覆盖能力

当前实现已经覆盖常见中后台系统的核心通知能力。

```text
消息模板管理
消息变量渲染
单用户通知
批量用户通知
站内信落库
RabbitMQ 异步解耦
WebSocket 在线推送
用户离线消息保留
用户消息分页查询
未读数量查询
单条标记已读
批量标记已读
用户消息逻辑删除
用户通知偏好
失败发送日志
XXL-JOB 失败重试
XXL-JOB 过期清理
```

适合直接落地到以下业务场景：

```text
审批通过 / 驳回通知
工单分派 / 转交通知
订单发货 / 售后状态通知
系统公告
平台运营消息
告警提醒
待办提醒
```

当前实现的关键设计点如下：

```text
消息主表和用户消息表分离，避免批量通知重复保存正文
先落库再投递 MQ，保证 WebSocket 失败不影响站内信可靠性
已读状态放在用户消息表，支持同一消息多用户独立状态
发送日志独立保存，便于失败重试和问题排查
通知偏好独立维护，便于扩展多渠道开关
```

### 可扩展渠道

当前案例只真正实现了站内信和 WebSocket。如果需要扩展为统一通知平台，可以继续增加短信、邮件、企业微信、钉钉、App Push 等渠道。

推荐扩展方式是抽象统一发送接口。

文件位置：`src/main/java/io/github/atengk/notify/channel/NotifyChannelSender.java`

下面定义渠道发送器接口，不同渠道实现自己的发送逻辑。

```java
package io.github.atengk.notify.channel;

import io.github.atengk.notify.entity.NotifyMessage;

/**
 * 通知渠道发送器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface NotifyChannelSender {

    /**
     * 获取渠道编码
     *
     * @return 渠道编码
     */
    String channel();

    /**
     * 发送通知
     *
     * @param notifyMessage 通知消息
     * @param userId 用户ID
     * @return 是否发送成功
     */
    boolean send(NotifyMessage notifyMessage, Long userId);
}
```

后续可以按渠道增加实现类。

```text
WebSocketNotifyChannelSender
SmsNotifyChannelSender
EmailNotifyChannelSender
DingTalkNotifyChannelSender
WechatWorkNotifyChannelSender
AppPushNotifyChannelSender
```

短信渠道一般需要补充以下字段或配置：

```text
手机号
短信签名
短信模板 ID
短信平台 accessKey
发送频率限制
短信发送回执
短信费用统计
```

邮件渠道一般需要补充以下能力：

```text
邮箱地址
HTML 邮件模板
附件支持
SMTP 配置
邮件发送失败重试
退信处理
```

企业微信、钉钉渠道一般需要补充：

```text
企业用户 ID 映射
机器人 webhook
access_token 缓存
接口限流
签名加密
失败重试
```

### 可优化点

当前实现以“核心功能可跑通”为目标，适合教学、专项案例和中小规模业务直接改造使用。如果要用于高并发、多租户、企业级通知平台，可以继续优化以下方向。

```text
发送可靠性：
引入本地消息表，保证数据库事务和 MQ 投递最终一致
增加 MQ 死信队列，集中处理异常事件
增加 RabbitMQ ConfirmCallback 和 ReturnCallback

消费幂等：
使用数据库消费日志表替代单纯 Redis 幂等
对 message_id、user_id、channel 建立唯一发送约束
避免重复消费产生重复发送日志

查询性能：
未读数量使用 Redis 计数缓存
用户消息列表按 user_id + create_time 建联合索引
历史消息按月份分表或归档
系统公告可以使用广播消息模型减少写扩散

WebSocket 多节点：
使用 Redis Pub/Sub 或 RabbitMQ 做多节点推送广播
用户在线状态写入 Redis
网关层支持 WebSocket 负载均衡和会话保持

模板能力：
接入 Freemarker 或 Thymeleaf
支持模板版本管理
支持模板变量校验
支持多语言模板

通知偏好：
按消息类型维护偏好，例如审批必须通知、营销可关闭
支持免打扰时间段
支持渠道优先级
支持用户级、角色级、租户级配置合并

安全与权限：
接口接入 Sa-Token 获取当前用户
查询消息时校验租户隔离
管理端模板维护增加操作审计
敏感内容脱敏展示

运维治理：
增加通知发送成功率统计
增加失败原因聚合报表
增加积压消息监控
增加 WebSocket 在线人数监控
增加慢查询和大批量发送告警
```

如果只是在普通后台系统中使用，优先建议做这几项增强：

```text
接入 Sa-Token 替换 X-User-Id
未读数量接入 Redis 缓存
RabbitMQ 增加发布确认
WebSocket 在线状态接入 Redis
发送日志增加唯一索引防重复
失败重试增加最大重试后的人工处理状态
```

最终，一个可维护的消息通知中心应该遵循这条主线：

```text
业务系统只负责触发通知
消息中心负责模板、投递、推送、查询、重试
实时推送不影响站内信可靠性
失败记录必须可追踪、可重试、可排查
```