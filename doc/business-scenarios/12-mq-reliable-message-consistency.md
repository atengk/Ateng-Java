# MQ 可靠消息与最终一致性

本文基于“MQ 可靠消息与最终一致性”场景展开，重点实现本地业务成功、本地消息落库、MQ 可靠投递、消费者幂等消费、失败重试、死信补偿、定时扫描未发送消息这一套核心链路。

## 案例目标

本案例实现一个“订单创建后异步发放积分”的最终一致性场景。订单服务在本地事务中完成订单落库和本地消息表写入，事务提交后再投递 RabbitMQ。积分服务消费消息后发放积分，并记录消费结果，保证消息重复投递时不会重复发放积分。

实现目标如下：

```text
1. 订单创建成功后，必须生成一条可靠消息
2. 业务数据和消息数据必须处于同一个本地事务中
3. MQ 投递失败不能导致消息丢失
4. 消费者重复消费不能导致重复发放积分
5. 消费失败可以自动重试
6. 多次重试失败后进入死信队列
7. 定时任务扫描未发送或发送失败的消息并重新投递
8. 消息最终要达到 SEND_SUCCESS / CONSUME_SUCCESS / DEAD 状态之一
```

核心结果是：不强依赖分布式事务，也不使用 Seata 这类强一致方案，而是通过“本地消息表 + MQ + 幂等消费 + 补偿任务”实现业务最终一致。

### 业务场景

本案例选择电商下单后的积分发放场景，便于体现跨服务一致性问题。

正常流程如下：

```text
用户创建订单
-> 订单服务保存订单
-> 订单服务写入本地消息表
-> 本地事务提交
-> 订单服务投递 MQ
-> 积分服务消费 MQ
-> 积分服务校验消费幂等
-> 积分服务发放积分
-> 积分服务记录消费成功
-> 订单消息状态更新为已消费
```

异常流程如下：

```text
订单创建成功，但 MQ 投递失败
-> 本地消息表仍然保留待发送消息
-> 定时任务扫描待发送消息
-> 重新投递 MQ
MQ 投递成功，但消费者处理失败
-> RabbitMQ 自动重试
-> 多次失败后进入死信队列
-> 死信消费者记录异常
-> 后台或定时任务进行补偿处理
MQ 重复投递或消费者重复消费
-> 消费者根据 message_key / biz_id 判断是否已处理
-> 已处理则直接 ACK
-> 未处理才执行积分发放
```

本案例中的业务角色如下：

| 角色     | 职责                                           |
| -------- | ---------------------------------------------- |
| 订单服务 | 创建订单，写入本地消息表，投递 MQ              |
| RabbitMQ | 承载异步消息，支持确认、重试、死信             |
| 积分服务 | 消费订单消息，发放积分                         |
| MySQL    | 保存订单、本地消息、消费记录                   |
| XXL-JOB  | 定时扫描未发送、发送失败、死信待补偿消息       |
| Redis    | 可选，用于短期消费幂等加速，不作为唯一可靠依据 |

### 实现边界

本案例只实现可靠消息最终一致性的核心功能，不展开完整电商系统。

包含内容：

```text
1. 订单创建接口
2. 订单表设计
3. 本地消息表设计
4. 消费记录表设计
5. RabbitMQ 交换机、队列、死信队列配置
6. 订单创建时写入本地消息
7. 事务提交后投递 MQ
8. RabbitMQ Confirm Callback 更新发送状态
9. 消费者幂等消费
10. 消费失败重试
11. 死信队列监听
12. XXL-JOB 定时补偿未发送消息
```

不包含内容：

```text
1. 完整用户系统
2. 完整商品库存系统
3. 完整支付系统
4. 多服务注册发现
5. 分库分表
6. 复杂顺序消息
7. Kafka / RocketMQ 多版本实现
8. 前端页面
```

本案例默认使用单体 Spring Boot 项目模拟订单服务和积分服务两个模块。实际微服务项目中，可以将订单生产者和积分消费者拆分到不同服务，核心代码结构和一致性方案不变。

### 技术选型

本案例采用 Spring Boot 3 + RabbitMQ + MyBatis-Plus + MySQL + XXL-JOB 的组合实现。RabbitMQ 负责消息投递和死信处理，本地消息表负责消息可靠存储，MyBatis-Plus 负责数据库访问，XXL-JOB 负责定时补偿。

| 技术          | 用途                                       |
| ------------- | ------------------------------------------ |
| Spring Boot 3 | 项目基础框架                               |
| Spring AMQP   | RabbitMQ 生产与消费                        |
| RabbitMQ      | 消息投递、重试、死信队列                   |
| MyBatis-Plus  | 订单、消息、消费记录持久化                 |
| MySQL 8       | 业务数据和消息数据存储                     |
| XXL-JOB       | 未发送消息、失败消息补偿任务               |
| Hutool        | JSON 序列化、字符串判断、日期处理、ID 生成 |
| Lombok        | 简化实体类和日志代码                       |
| Redis         | 可选，用于短期幂等缓存                     |

推荐版本如下：

```text
JDK 17
Spring Boot 3.2.x 或 3.3.x
MyBatis-Plus 3.5.x
MySQL 8.x
RabbitMQ 3.12+
XXL-JOB 2.4.x
Hutool 5.8.x
```

本案例选择 RabbitMQ 的原因是它对 Confirm Callback、消费 ACK、重试队列、死信队列支持直接，适合展示“可靠投递 + 失败补偿 + 死信处理”的完整链路。实际生产中，如果业务更强调高吞吐日志流或大规模事件流，可以替换为 Kafka；如果业务更强调事务消息和顺序消息，可以考虑 RocketMQ。

## 核心流程设计

本章节说明可靠消息的核心链路。整体方案采用“本地消息表 + RabbitMQ Confirm Callback + 消费幂等 + 死信队列 + 定时补偿”的组合，覆盖原文中提到的“消息不能丢、消息可能重复、业务和消息一致、消费失败重试、死信队列、消费幂等、补偿任务”等关键点。

### 本地业务与本地消息表

本地业务与本地消息表必须放在同一个数据库事务中处理。也就是说，订单创建成功时，不是直接发送 MQ，而是先把“待发送消息”写入本地消息表。只要事务提交成功，就说明订单数据和消息数据都已经可靠落库。

核心流程如下：

```text
开启本地事务
-> 创建订单
-> 写入本地消息表，状态为 WAIT_SEND
-> 提交事务
-> 事务提交后投递 MQ
-> 投递成功后更新消息状态为 SEND_SUCCESS
-> 投递失败则保留 WAIT_SEND / SEND_FAILED，等待补偿任务重新投递
```

这种方式解决的是“业务成功但消息丢失”的问题。即使应用在事务提交后、MQ 投递前宕机，本地消息表中仍然保留了待发送消息，后续可以由定时任务重新扫描并投递。

消息状态建议设计为：

| 状态            | 含义     | 说明                              |
| --------------- | -------- | --------------------------------- |
| WAIT_SEND       | 待发送   | 业务事务已提交，但尚未成功投递 MQ |
| SEND_SUCCESS    | 发送成功 | RabbitMQ Confirm Callback 已确认  |
| SEND_FAILED     | 发送失败 | 投递异常或 Confirm 返回失败       |
| CONSUME_SUCCESS | 消费成功 | 消费者已成功处理业务              |
| DEAD            | 死信     | 消息多次消费失败，进入死信队列    |
| CLOSED          | 已关闭   | 人工确认无需继续补偿              |

本地消息表不应该只保存消息内容，还应该保存业务类型、业务主键、消息唯一键、重试次数、下次重试时间、异常原因等字段，方便补偿和排查问题。

### MQ 投递确认

MQ 投递确认主要依赖 RabbitMQ 的 Publisher Confirm 机制。生产者发送消息后，RabbitMQ Broker 会异步返回确认结果。应用根据 Confirm Callback 更新本地消息表状态。

核心流程如下：

```text
生产者发送消息
-> RabbitMQ Broker 接收消息
-> Broker 返回 ACK
-> 更新本地消息状态为 SEND_SUCCESS
```

如果 Broker 返回 NACK，或者发送过程中发生异常，则更新为 SEND_FAILED，并等待定时任务补偿。

```text
生产者发送消息
-> RabbitMQ Broker 接收失败
-> Broker 返回 NACK 或抛出异常
-> 更新本地消息状态为 SEND_FAILED
-> 定时任务后续重新投递
```

需要注意的是，Confirm Callback 只能说明消息已经到达 RabbitMQ Broker，不能说明消费者已经处理成功。因此，消息最终一致性不能只依赖 Confirm Callback，还需要消费者处理完成后记录消费结果。

建议生产者发送消息时携带以下关键信息：

| 字段       | 说明                         |
| ---------- | ---------------------------- |
| messageId  | 消息表主键 ID                |
| messageKey | 全局唯一消息 Key             |
| bizType    | 业务类型，例如 ORDER_CREATED |
| bizId      | 业务 ID，例如订单 ID         |
| payload    | 业务消息内容                 |
| createTime | 消息创建时间                 |

### 消费者幂等处理

MQ 消息天然可能重复投递。重复投递的原因可能是消费者处理成功但 ACK 失败、消费者超时、Broker 重投、人工补偿重新发送等。因此消费者必须做幂等处理。

本案例采用“消费记录表唯一索引”作为最终幂等依据。Redis 可以作为短期缓存加速判断，但不能替代数据库唯一索引。

消费流程如下：

```text
消费者收到消息
-> 根据 messageKey 查询消费记录
-> 已消费成功，直接 ACK
-> 未消费，尝试插入消费记录
-> 插入成功，执行业务处理
-> 发放积分
-> 更新消费记录为 SUCCESS
-> 更新本地消息状态为 CONSUME_SUCCESS
-> 手动 ACK
```

重复消费流程如下：

```text
消费者收到重复消息
-> 根据 messageKey 查询消费记录
-> 发现已 SUCCESS
-> 不再执行业务
-> 直接 ACK
```

消费记录表需要对 `message_key` 或 `message_key + consumer_group` 添加唯一索引。这样即使多个消费者并发消费同一条消息，也只有一个消费者能成功插入消费记录，其他消费者会因为唯一索引冲突而识别为重复消费。

### 失败重试与死信补偿

消费者处理失败时，不建议无限重试。无限重试会阻塞正常消息，也会导致系统资源被异常数据拖垮。

本案例采用 RabbitMQ 消费重试 + 死信队列的方式处理失败消息：

```text
消费者处理消息失败
-> 抛出异常
-> Spring AMQP 按配置进行本地重试
-> 重试仍失败
-> 消息被拒绝
-> RabbitMQ 路由到死信交换机
-> 死信队列监听器记录 DEAD 状态
-> 后续人工或任务补偿
```

死信队列主要用于兜底，不应该把死信队列当成正常业务流程的一部分。进入死信队列通常说明业务数据异常、下游服务长期不可用、消息格式错误或代码存在缺陷。

建议死信处理只做以下事情：

```text
1. 记录死信消息
2. 更新本地消息表状态为 DEAD
3. 保存异常原因
4. 发送告警或打印错误日志
5. 提供人工补偿入口
```

不建议在死信消费者中无限递归重发消息，否则会形成“死信 -> 重发 -> 再死信”的异常循环。

### 定时扫描未发送消息

定时扫描任务用于补偿生产者侧的异常，包括应用宕机、MQ 短暂不可用、Confirm Callback 未返回、发送失败等情况。

扫描范围建议包括：

```text
1. WAIT_SEND 且创建时间超过一定时间的消息
2. SEND_FAILED 且未超过最大重试次数的消息
3. SEND_SUCCESS 但长时间未消费成功的消息
```

任务流程如下：

```text
XXL-JOB 定时触发
-> 查询待补偿消息
-> 判断 retry_count 是否超过最大次数
-> 未超过则重新投递 MQ
-> retry_count + 1
-> 更新 next_retry_time
-> 超过最大次数则标记为 DEAD 或人工处理
```

补偿任务需要控制每次扫描数量，避免一次性加载大量消息。推荐使用分页查询，并配合 `next_retry_time` 做延迟重试。

常见重试策略如下：

| 重试次数 | 下次重试间隔 |
| -------- | ------------ |
| 1        | 1 分钟       |
| 2        | 5 分钟       |
| 3        | 10 分钟      |
| 4        | 30 分钟      |
| 5        | 60 分钟      |

超过最大重试次数后，不再自动投递，防止异常消息持续冲击 MQ 和消费者。

## 数据库表设计

本章节给出最小可运行的数据库表结构。表设计围绕订单业务、本地消息、消费幂等三类数据展开，核心是保证业务数据、消息数据、消费结果都有可追踪记录。

### 业务订单表

业务订单表用于模拟订单创建场景。真实项目中订单表会更复杂，这里只保留可靠消息案例需要的核心字段。

```sql
CREATE TABLE `biz_order` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `amount` DECIMAL(18,2) NOT NULL COMMENT '订单金额',
  `order_status` VARCHAR(32) NOT NULL COMMENT '订单状态：CREATED-已创建，CLOSED-已关闭',
  `point_status` VARCHAR(32) NOT NULL DEFAULT 'WAIT_GRANT' COMMENT '积分状态：WAIT_GRANT-待发放，GRANTED-已发放',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务订单表';
```

字段说明：

| 字段         | 说明                      |
| ------------ | ------------------------- |
| id           | 订单主键，建议使用雪花 ID |
| order_no     | 订单号，需要唯一          |
| user_id      | 下单用户                  |
| amount       | 订单金额                  |
| order_status | 订单状态                  |
| point_status | 积分发放状态              |
| create_time  | 创建时间                  |
| update_time  | 更新时间                  |

### 本地消息表

本地消息表是可靠消息方案的核心。订单创建时，业务订单和本地消息必须在同一个事务中写入。

```sql
CREATE TABLE `local_message` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `message_key` VARCHAR(128) NOT NULL COMMENT '消息唯一Key',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型，例如 ORDER_CREATED',
  `biz_id` VARCHAR(64) NOT NULL COMMENT '业务ID，例如订单ID',
  `exchange_name` VARCHAR(128) NOT NULL COMMENT '交换机名称',
  `routing_key` VARCHAR(128) NOT NULL COMMENT '路由Key',
  `message_body` TEXT NOT NULL COMMENT '消息体JSON',
  `message_status` VARCHAR(32) NOT NULL COMMENT '消息状态：WAIT_SEND，SEND_SUCCESS，SEND_FAILED，CONSUME_SUCCESS，DEAD，CLOSED',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `max_retry_count` INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
  `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
  `fail_reason` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
  `confirm_time` DATETIME DEFAULT NULL COMMENT 'MQ确认时间',
  `consume_time` DATETIME DEFAULT NULL COMMENT '消费成功时间',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_key` (`message_key`),
  KEY `idx_biz_type_biz_id` (`biz_type`, `biz_id`),
  KEY `idx_status_retry_time` (`message_status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表';
```

字段说明：

| 字段            | 说明                                     |
| --------------- | ---------------------------------------- |
| message_key     | 全局唯一消息 Key，用于投递和消费幂等     |
| biz_type        | 业务类型，用于区分订单、库存、积分等消息 |
| biz_id          | 业务主键                                 |
| exchange_name   | RabbitMQ 交换机                          |
| routing_key     | RabbitMQ 路由 Key                        |
| message_body    | JSON 消息体                              |
| message_status  | 消息状态                                 |
| retry_count     | 当前重试次数                             |
| max_retry_count | 最大重试次数                             |
| next_retry_time | 下次补偿投递时间                         |
| fail_reason     | 失败原因，便于排查                       |
| confirm_time    | MQ Confirm 成功时间                      |
| consume_time    | 消费成功时间                             |

本地消息表最关键的是 `uk_message_key` 和 `idx_status_retry_time`。前者保证消息唯一，后者提高补偿任务扫描效率。

### 消费记录表

消费记录表用于保证消费者幂等。消费者收到消息后，先写入消费记录，再执行业务处理。重复消息会因为唯一索引被识别出来。

```sql
CREATE TABLE `message_consume_record` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `message_key` VARCHAR(128) NOT NULL COMMENT '消息唯一Key',
  `message_id` BIGINT NOT NULL COMMENT '本地消息ID',
  `consumer_group` VARCHAR(128) NOT NULL COMMENT '消费者分组',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型',
  `biz_id` VARCHAR(64) NOT NULL COMMENT '业务ID',
  `consume_status` VARCHAR(32) NOT NULL COMMENT '消费状态：PROCESSING，SUCCESS，FAILED',
  `consume_count` INT NOT NULL DEFAULT 0 COMMENT '消费次数',
  `fail_reason` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_consumer` (`message_key`, `consumer_group`),
  KEY `idx_biz_type_biz_id` (`biz_type`, `biz_id`),
  KEY `idx_consume_status` (`consume_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息消费记录表';
```

字段说明：

| 字段           | 说明          |
| -------------- | ------------- |
| message_key    | 消息唯一 Key  |
| message_id     | 本地消息表 ID |
| consumer_group | 消费者分组    |
| biz_type       | 业务类型      |
| biz_id         | 业务 ID       |
| consume_status | 消费状态      |
| consume_count  | 消费次数      |
| fail_reason    | 消费失败原因  |

如果同一条消息需要被多个业务方消费，例如积分服务、通知服务、数据同步服务，那么可以通过 `message_key + consumer_group` 实现不同消费者组之间互不影响。

## 项目依赖与基础配置

本章节给出项目需要的 Maven 依赖和核心配置。后续代码默认基于这些依赖和配置实现。

### Maven 依赖

文件位置：`pom.xml`

下面的依赖覆盖 Web 接口、RabbitMQ、MyBatis-Plus、MySQL、XXL-JOB、Hutool、Lombok 等核心能力。

```xml
<dependencies>
    <!-- Web 接口，用于提供订单创建和测试接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- RabbitMQ，用于可靠消息投递、消费、重试和死信队列 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- MyBatis-Plus，简化数据库 CRUD 和分页查询 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动，用于连接 MySQL 8.x -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- XXL-JOB，用于实现可靠消息补偿任务 -->
    <dependency>
        <groupId>com.xuxueli</groupId>
        <artifactId>xxl-job-core</artifactId>
        <version>2.4.1</version>
    </dependency>

    <!-- Hutool 工具类，用于 JSON、日期、字符串、ID 等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>

    <!-- Lombok，减少 Getter、Setter、构造器和日志样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 参数校验，用于 Controller 入参校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot Parent 管理版本，`spring-boot-starter-*` 和 MySQL 驱动可以不显式指定版本。

### RabbitMQ 配置

RabbitMQ 配置需要开启生产者确认、消费者手动 ACK、消费重试和死信队列。这里先给出 `application.yml` 基础配置，交换机和队列的 Java 配置在后续章节实现。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: admin
    password: admin
    virtual-host: /

    # 开启生产者 Confirm Callback，确认消息是否到达 Broker
    publisher-confirm-type: correlated

    # 开启 Return Callback，处理路由不到队列的消息
    publisher-returns: true

    template:
      # 消息路由失败时触发 Return Callback，而不是直接丢弃
      mandatory: true

    listener:
      simple:
        # 消费者手动 ACK，业务处理成功后再确认
        acknowledge-mode: manual

        # 每个消费者每次最多拉取 1 条消息，方便控制消费失败和重试
        prefetch: 1

        retry:
          # 开启 Spring AMQP 消费重试
          enabled: true

          # 首次重试间隔
          initial-interval: 1000ms

          # 最大重试次数，包含首次消费
          max-attempts: 3

          # 最大重试间隔
          max-interval: 10000ms

          # 重试间隔倍数
          multiplier: 2
```

建议使用的交换机和队列命名如下：

| 名称                         | 类型            | 用途           |
| ---------------------------- | --------------- | -------------- |
| reliable.order.exchange      | Direct Exchange | 订单业务交换机 |
| reliable.order.point.queue   | Queue           | 积分发放队列   |
| reliable.order.point.routing | Routing Key     | 积分发放路由   |
| reliable.order.dlx.exchange  | Direct Exchange | 死信交换机     |
| reliable.order.dlx.queue     | Queue           | 死信队列       |
| reliable.order.dlx.routing   | Routing Key     | 死信路由       |

### MyBatis-Plus 配置

MyBatis-Plus 用于处理订单表、本地消息表、消费记录表的 CRUD。这里开启驼峰映射和 SQL 日志，便于开发阶段排查问题。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/reliable_message_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root

mybatis-plus:
  configuration:
    # 数据库下划线字段自动映射为 Java 驼峰字段
    map-underscore-to-camel-case: true

    # 开发阶段打印 SQL，生产环境建议关闭或改为标准日志采集
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

  global-config:
    db-config:
      # 使用 ASSIGN_ID 生成雪花 ID
      id-type: assign_id

  mapper-locations: classpath*:/mapper/**/*.xml
```

后续实体类可以通过 MyBatis-Plus 的 `BaseMapper` 直接完成常规操作。涉及状态更新的地方，建议使用带条件的 UpdateWrapper，避免误更新。

### XXL-JOB 配置

XXL-JOB 用于定时扫描本地消息表中的未发送消息、发送失败消息和长时间未消费成功消息。这里给出执行器侧配置。

文件位置：`src/main/resources/application.yml`

```yaml
xxl:
  job:
    admin:
      # XXL-JOB 调度中心地址
      addresses: http://127.0.0.1:8080/xxl-job-admin

    executor:
      # 当前执行器名称，需要在调度中心中保持一致
      appname: reliable-message-executor

      # 执行器注册地址，为空时自动注册
      address:

      # 执行器 IP，为空时自动获取
      ip:

      # 执行器端口
      port: 9999

      # 执行器日志路径
      logpath: ./logs/xxl-job/jobhandler

      # 日志保留天数
      logretentiondays: 30

    # 执行器通讯 Token，需与调度中心保持一致
    accessToken: default_token
```

建议配置三个补偿任务：

| 任务名称                  | 执行频率   | 作用                                                |
| ------------------------- | ---------- | --------------------------------------------------- |
| scanWaitSendMessageJob    | 每 1 分钟  | 扫描 WAIT_SEND 消息并重新投递                       |
| retryFailedMessageJob     | 每 5 分钟  | 扫描 SEND_FAILED 消息并按重试策略投递               |
| checkUnconsumedMessageJob | 每 10 分钟 | 扫描长时间 SEND_SUCCESS 但未 CONSUME_SUCCESS 的消息 |

任务查询时必须限制单次处理数量，例如每次最多处理 100 条，避免补偿任务对数据库和 MQ 造成突发压力。

## 生产者核心实现

生产者侧的核心目标是：订单创建成功后，消息不能丢。实现方式不是在业务代码里直接发送 MQ，而是先在同一个本地事务中写入订单和本地消息表，事务提交后再投递 RabbitMQ。这个设计对应原场景中的“本地业务执行成功、写入本地消息表、投递 MQ、定时扫描未发送消息”。

### 创建订单并写入本地消息

订单创建时，需要同时写入 `biz_order` 和 `local_message`。这两个操作必须在同一个 `@Transactional` 事务中完成。

先定义 RabbitMQ 常量和订单创建消息体。

文件位置：`src/main/java/io/github/atengk/reliablemessage/common/RabbitMqConstant.java`

```java
package io.github.atengk.reliablemessage.common;

/**
 * RabbitMQ 常量配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class RabbitMqConstant {

    public static final String ORDER_EXCHANGE = "reliable.order.exchange";

    public static final String ORDER_POINT_QUEUE = "reliable.order.point.queue";

    public static final String ORDER_POINT_ROUTING_KEY = "reliable.order.point.routing";

    public static final String ORDER_DLX_EXCHANGE = "reliable.order.dlx.exchange";

    public static final String ORDER_DLX_QUEUE = "reliable.order.dlx.queue";

    public static final String ORDER_DLX_ROUTING_KEY = "reliable.order.dlx.routing";

    private RabbitMqConstant() {
    }
}
```

文件位置：`src/main/java/io/github/atengk/reliablemessage/common/MessageStatus.java`

```java
package io.github.atengk.reliablemessage.common;

/**
 * 本地消息状态
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MessageStatus {

    public static final String WAIT_SEND = "WAIT_SEND";

    public static final String SEND_SUCCESS = "SEND_SUCCESS";

    public static final String SEND_FAILED = "SEND_FAILED";

    public static final String CONSUME_SUCCESS = "CONSUME_SUCCESS";

    public static final String DEAD = "DEAD";

    public static final String CLOSED = "CLOSED";

    private MessageStatus() {
    }
}
```

文件位置：`src/main/java/io/github/atengk/reliablemessage/dto/OrderCreateRequest.java`

```java
package io.github.atengk.reliablemessage.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderCreateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal amount;

    private String remark;
}
```

文件位置：`src/main/java/io/github/atengk/reliablemessage/mq/OrderCreatedMessage.java`

```java
package io.github.atengk.reliablemessage.mq;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建消息体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderCreatedMessage {

    private Long messageId;

    private String messageKey;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;
}
```

创建订单时，同时写入本地消息表，并在事务提交后触发 MQ 投递。

文件位置：`src/main/java/io/github/atengk/reliablemessage/service/impl/OrderServiceImpl.java`

```java
package io.github.atengk.reliablemessage.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.reliablemessage.common.MessageStatus;
import io.github.atengk.reliablemessage.common.RabbitMqConstant;
import io.github.atengk.reliablemessage.dto.OrderCreateRequest;
import io.github.atengk.reliablemessage.entity.BizOrder;
import io.github.atengk.reliablemessage.entity.LocalMessage;
import io.github.atengk.reliablemessage.mapper.BizOrderMapper;
import io.github.atengk.reliablemessage.mapper.LocalMessageMapper;
import io.github.atengk.reliablemessage.mq.OrderCreatedMessage;
import io.github.atengk.reliablemessage.mq.ReliableMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 订单业务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl {

    private final BizOrderMapper bizOrderMapper;

    private final LocalMessageMapper localMessageMapper;

    private final ReliableMessageProducer reliableMessageProducer;

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    /**
     * 创建订单并写入本地消息
     *
     * @param request 创建订单请求
     * @return 订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Long orderId = SNOWFLAKE.nextId();
        String orderNo = "ORD" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmssSSS") + IdUtil.fastSimpleUUID().substring(0, 8);

        BizOrder order = new BizOrder();
        order.setId(orderId);
        order.setOrderNo(orderNo);
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        order.setOrderStatus("CREATED");
        order.setPointStatus("WAIT_GRANT");
        order.setRemark(request.getRemark());
        order.setCreateTime(now);
        order.setUpdateTime(now);
        bizOrderMapper.insert(order);

        Long messageId = SNOWFLAKE.nextId();
        String messageKey = "ORDER_CREATED:" + orderId;

        OrderCreatedMessage messageBody = new OrderCreatedMessage();
        messageBody.setMessageId(messageId);
        messageBody.setMessageKey(messageKey);
        messageBody.setOrderId(orderId);
        messageBody.setOrderNo(orderNo);
        messageBody.setUserId(request.getUserId());
        messageBody.setAmount(request.getAmount());

        LocalMessage localMessage = new LocalMessage();
        localMessage.setId(messageId);
        localMessage.setMessageKey(messageKey);
        localMessage.setBizType("ORDER_CREATED");
        localMessage.setBizId(String.valueOf(orderId));
        localMessage.setExchangeName(RabbitMqConstant.ORDER_EXCHANGE);
        localMessage.setRoutingKey(RabbitMqConstant.ORDER_POINT_ROUTING_KEY);
        localMessage.setMessageBody(JSONUtil.toJsonStr(messageBody));
        localMessage.setMessageStatus(MessageStatus.WAIT_SEND);
        localMessage.setRetryCount(0);
        localMessage.setMaxRetryCount(5);
        localMessage.setNextRetryTime(now);
        localMessage.setCreateTime(now);
        localMessage.setUpdateTime(now);
        localMessageMapper.insert(localMessage);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                reliableMessageProducer.sendLocalMessage(messageId);
            }
        });

        log.info("订单创建成功，已写入本地消息，orderId={}，messageKey={}", orderId, messageKey);
        return orderId;
    }
}
```

这里的关键点是：`bizOrderMapper.insert(order)` 和 `localMessageMapper.insert(localMessage)` 在同一个事务中执行。只要订单创建成功，本地消息一定存在。即使后续 MQ 投递失败，也可以依靠本地消息表补偿。

### 事务提交后投递 MQ

事务提交后投递 MQ，可以避免“事务回滚但消息已经发出”的问题。投递时使用 `CorrelationData` 携带本地消息 ID，后续 Confirm Callback 可以根据这个 ID 更新消息状态。

文件位置：`src/main/java/io/github/atengk/reliablemessage/mq/ReliableMessageProducer.java`

```java
package io.github.atengk.reliablemessage.mq;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.reliablemessage.entity.LocalMessage;
import io.github.atengk.reliablemessage.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 可靠消息生产者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReliableMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    private final LocalMessageService localMessageService;

    /**
     * 根据本地消息ID投递消息
     *
     * @param messageId 本地消息ID
     */
    public void sendLocalMessage(Long messageId) {
        LocalMessage localMessage = localMessageService.getById(messageId);
        if (localMessage == null) {
            log.warn("本地消息不存在，messageId={}", messageId);
            return;
        }

        if (StrUtil.isBlank(localMessage.getExchangeName()) || StrUtil.isBlank(localMessage.getRoutingKey())) {
            localMessageService.markSendFailed(messageId, "交换机或路由Key为空");
            log.warn("本地消息配置不完整，messageId={}", messageId);
            return;
        }

        try {
            CorrelationData correlationData = new CorrelationData(String.valueOf(messageId));
            rabbitTemplate.convertAndSend(
                    localMessage.getExchangeName(),
                    localMessage.getRoutingKey(),
                    localMessage.getMessageBody(),
                    message -> {
                        message.getMessageProperties().setMessageId(localMessage.getMessageKey());
                        message.getMessageProperties().setCorrelationId(localMessage.getMessageKey());
                        message.getMessageProperties().setContentType("application/json");
                        return message;
                    },
                    correlationData
            );

            log.info("本地消息已提交到 RabbitTemplate，messageId={}，messageKey={}", messageId, localMessage.getMessageKey());
        } catch (Exception e) {
            String errorMessage = ExceptionUtil.getRootCauseMessage(e);
            localMessageService.markSendFailed(messageId, errorMessage);
            log.error("本地消息投递异常，messageId={}，原因={}", messageId, errorMessage, e);
        }
    }
}
```

这里的 `convertAndSend` 只是把消息交给 RabbitTemplate，不代表消息已经可靠到达 Broker。是否真正到达 RabbitMQ，需要依靠 Confirm Callback 确认。

### Confirm Callback 更新消息状态

Confirm Callback 用来确认消息是否到达 RabbitMQ Broker。收到 ACK 后，更新本地消息状态为 `SEND_SUCCESS`；收到 NACK 后，更新为 `SEND_FAILED`，等待后续补偿。

文件位置：`src/main/java/io/github/atengk/reliablemessage/mq/RabbitProducerCallback.java`

```java
package io.github.atengk.reliablemessage.mq;

import cn.hutool.core.convert.Convert;
import io.github.atengk.reliablemessage.service.LocalMessageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 生产者回调
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitProducerCallback {

    private final RabbitTemplate rabbitTemplate;

    private final LocalMessageService localMessageService;

    /**
     * 初始化 RabbitTemplate 回调
     */
    @PostConstruct
    public void initCallback() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData == null) {
                log.warn("RabbitMQ Confirm 回调缺少 CorrelationData");
                return;
            }

            Long messageId = Convert.toLong(correlationData.getId());
            if (Boolean.TRUE.equals(ack)) {
                localMessageService.markSendSuccess(messageId);
                log.info("RabbitMQ Confirm 成功，messageId={}", messageId);
            } else {
                localMessageService.markSendFailed(messageId, cause);
                log.warn("RabbitMQ Confirm 失败，messageId={}，原因={}", messageId, cause);
            }
        });

        rabbitTemplate.setReturnsCallback(this::handleReturnedMessage);
    }

    /**
     * 处理路由失败消息
     *
     * @param returnedMessage 路由失败消息
     */
    private void handleReturnedMessage(ReturnedMessage returnedMessage) {
        String messageId = returnedMessage.getMessage().getMessageProperties().getCorrelationId();
        log.warn("RabbitMQ 消息路由失败，correlationId={}，replyCode={}，replyText={}，exchange={}，routingKey={}",
                messageId,
                returnedMessage.getReplyCode(),
                returnedMessage.getReplyText(),
                returnedMessage.getExchange(),
                returnedMessage.getRoutingKey());
    }
}
```

`ReturnsCallback` 处理的是“消息到达交换机，但没有路由到队列”的情况。实际项目中可以根据 `correlationId` 或消息体中的 `messageId` 更新本地消息为 `SEND_FAILED`。如果队列配置固定且启动时声明完整，路由失败一般属于配置错误。

### 投递失败标记待重试

投递失败、Confirm NACK、发送异常都需要写回本地消息表。下面的服务封装了消息状态更新和重试次数递增逻辑，后续消费者和补偿任务也会复用。

文件位置：`src/main/java/io/github/atengk/reliablemessage/service/LocalMessageService.java`

```java
package io.github.atengk.reliablemessage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.reliablemessage.entity.LocalMessage;

import java.util.List;

/**
 * 本地消息服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface LocalMessageService extends IService<LocalMessage> {

    /**
     * 标记发送成功
     *
     * @param messageId 消息ID
     */
    void markSendSuccess(Long messageId);

    /**
     * 标记发送失败
     *
     * @param messageId 消息ID
     * @param reason    失败原因
     */
    void markSendFailed(Long messageId, String reason);

    /**
     * 标记消费成功
     *
     * @param messageId 消息ID
     */
    void markConsumeSuccess(Long messageId);

    /**
     * 标记死信
     *
     * @param messageId 消息ID
     * @param reason    死信原因
     */
    void markDead(Long messageId, String reason);

    /**
     * 增加重试次数
     *
     * @param messageId 消息ID
     */
    void increaseRetryCount(Long messageId);

    /**
     * 查询待补偿消息
     *
     * @param status 消息状态
     * @param limit  查询数量
     * @return 消息列表
     */
    List<LocalMessage> listRetryableMessages(String status, int limit);
}
```

文件位置：`src/main/java/io/github/atengk/reliablemessage/service/impl/LocalMessageServiceImpl.java`

```java
package io.github.atengk.reliablemessage.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.reliablemessage.common.MessageStatus;
import io.github.atengk.reliablemessage.entity.LocalMessage;
import io.github.atengk.reliablemessage.mapper.LocalMessageMapper;
import io.github.atengk.reliablemessage.service.LocalMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class LocalMessageServiceImpl extends ServiceImpl<LocalMessageMapper, LocalMessage> implements LocalMessageService {

    /**
     * 标记发送成功
     *
     * @param messageId 消息ID
     */
    @Override
    public void markSendSuccess(Long messageId) {
        LocalMessage update = new LocalMessage();
        update.setMessageStatus(MessageStatus.SEND_SUCCESS);
        update.setConfirmTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());

        lambdaUpdate()
                .eq(LocalMessage::getId, messageId)
                .in(LocalMessage::getMessageStatus, MessageStatus.WAIT_SEND, MessageStatus.SEND_FAILED)
                .update(update);
    }

    /**
     * 标记发送失败
     *
     * @param messageId 消息ID
     * @param reason    失败原因
     */
    @Override
    public void markSendFailed(Long messageId, String reason) {
        LocalMessage update = new LocalMessage();
        update.setMessageStatus(MessageStatus.SEND_FAILED);
        update.setFailReason(StrUtil.subPre(reason, 1000));
        update.setNextRetryTime(DateUtil.offsetMinute(LocalDateTime.now(), 1).toLocalDateTime());
        update.setUpdateTime(LocalDateTime.now());

        lambdaUpdate()
                .eq(LocalMessage::getId, messageId)
                .ne(LocalMessage::getMessageStatus, MessageStatus.CONSUME_SUCCESS)
                .update(update);
    }

    /**
     * 标记消费成功
     *
     * @param messageId 消息ID
     */
    @Override
    public void markConsumeSuccess(Long messageId) {
        LocalMessage update = new LocalMessage();
        update.setMessageStatus(MessageStatus.CONSUME_SUCCESS);
        update.setConsumeTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());

        lambdaUpdate()
                .eq(LocalMessage::getId, messageId)
                .ne(LocalMessage::getMessageStatus, MessageStatus.CLOSED)
                .update(update);
    }

    /**
     * 标记死信
     *
     * @param messageId 消息ID
     * @param reason    死信原因
     */
    @Override
    public void markDead(Long messageId, String reason) {
        LocalMessage update = new LocalMessage();
        update.setMessageStatus(MessageStatus.DEAD);
        update.setFailReason(StrUtil.subPre(reason, 1000));
        update.setUpdateTime(LocalDateTime.now());

        lambdaUpdate()
                .eq(LocalMessage::getId, messageId)
                .ne(LocalMessage::getMessageStatus, MessageStatus.CONSUME_SUCCESS)
                .update(update);
    }

    /**
     * 增加重试次数
     *
     * @param messageId 消息ID
     */
    @Override
    public void increaseRetryCount(Long messageId) {
        LocalMessage localMessage = getById(messageId);
        if (localMessage == null) {
            return;
        }

        int nextRetryCount = localMessage.getRetryCount() + 1;
        int delayMinutes = switch (nextRetryCount) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 30;
            default -> 60;
        };

        LocalMessage update = new LocalMessage();
        update.setRetryCount(nextRetryCount);
        update.setNextRetryTime(DateUtil.offsetMinute(LocalDateTime.now(), delayMinutes).toLocalDateTime());
        update.setUpdateTime(LocalDateTime.now());

        lambdaUpdate()
                .eq(LocalMessage::getId, messageId)
                .update(update);
    }

    /**
     * 查询待补偿消息
     *
     * @param status 消息状态
     * @param limit  查询数量
     * @return 消息列表
     */
    @Override
    public List<LocalMessage> listRetryableMessages(String status, int limit) {
        return list(new LambdaQueryWrapper<LocalMessage>()
                .eq(LocalMessage::getMessageStatus, status)
                .le(LocalMessage::getNextRetryTime, LocalDateTime.now())
                .apply("retry_count < max_retry_count")
                .orderByAsc(LocalMessage::getNextRetryTime)
                .last("LIMIT " + limit));
    }
}
```

这里使用 `next_retry_time` 控制补偿节奏，避免失败消息被定时任务高频反复投递。

## 消费者核心实现

消费者侧的核心目标是：消息可以重复到达，但业务不能重复执行。因此消费者必须基于数据库唯一索引做幂等控制，不能只依赖 Redis 或内存状态。

### 监听业务消息

先声明业务交换机、业务队列、死信交换机和死信队列。业务队列绑定死信交换机，消费失败且最终拒绝后，消息会进入死信队列。

文件位置：`src/main/java/io/github/atengk/reliablemessage/config/RabbitMqConfig.java`

```java
package io.github.atengk.reliablemessage.config;

import io.github.atengk.reliablemessage.common.RabbitMqConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 队列配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 订单业务交换机
     *
     * @return Direct交换机
     */
    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange(RabbitMqConstant.ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 积分发放队列
     *
     * @return 队列
     */
    @Bean
    public Queue orderPointQueue() {
        return QueueBuilder.durable(RabbitMqConstant.ORDER_POINT_QUEUE)
                .deadLetterExchange(RabbitMqConstant.ORDER_DLX_EXCHANGE)
                .deadLetterRoutingKey(RabbitMqConstant.ORDER_DLX_ROUTING_KEY)
                .build();
    }

    /**
     * 积分发放队列绑定
     *
     * @return 绑定关系
     */
    @Bean
    public Binding orderPointBinding() {
        return BindingBuilder.bind(orderPointQueue())
                .to(orderExchange())
                .with(RabbitMqConstant.ORDER_POINT_ROUTING_KEY);
    }

    /**
     * 死信交换机
     *
     * @return 死信交换机
     */
    @Bean
    public DirectExchange orderDlxExchange() {
        return ExchangeBuilder.directExchange(RabbitMqConstant.ORDER_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 死信队列
     *
     * @return 死信队列
     */
    @Bean
    public Queue orderDlxQueue() {
        return QueueBuilder.durable(RabbitMqConstant.ORDER_DLX_QUEUE).build();
    }

    /**
     * 死信队列绑定
     *
     * @return 绑定关系
     */
    @Bean
    public Binding orderDlxBinding() {
        return BindingBuilder.bind(orderDlxQueue())
                .to(orderDlxExchange())
                .with(RabbitMqConstant.ORDER_DLX_ROUTING_KEY);
    }
}
```

业务消费者监听 `reliable.order.point.queue`，消费成功后手动 ACK；消费失败则抛出异常，由 Spring AMQP 重试机制处理，重试耗尽后进入死信队列。

文件位置：`src/main/java/io/github/atengk/reliablemessage/mq/OrderPointConsumer.java`

```java
package io.github.atengk.reliablemessage.mq;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.reliablemessage.common.RabbitMqConstant;
import io.github.atengk.reliablemessage.service.MessageConsumeRecordService;
import io.github.atengk.reliablemessage.service.OrderPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单积分消息消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPointConsumer {

    private final MessageConsumeRecordService messageConsumeRecordService;

    private final OrderPointService orderPointService;

    /**
     * 监听订单创建消息并发放积分
     *
     * @param message RabbitMQ原始消息
     * @param channel RabbitMQ通道
     * @throws Exception 消费异常
     */
    @RabbitListener(queues = RabbitMqConstant.ORDER_POINT_QUEUE)
    public void consumeOrderCreatedMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageBody = new String(message.getBody());

        OrderCreatedMessage orderMessage = JSONUtil.toBean(messageBody, OrderCreatedMessage.class);
        log.info("收到订单积分消息，messageKey={}，orderId={}", orderMessage.getMessageKey(), orderMessage.getOrderId());

        try {
            boolean alreadySuccess = messageConsumeRecordService.isConsumedSuccess(orderMessage.getMessageKey(), "order-point-consumer");
            if (alreadySuccess) {
                channel.basicAck(deliveryTag, false);
                log.info("订单积分消息已消费成功，本次直接ACK，messageKey={}", orderMessage.getMessageKey());
                return;
            }

            messageConsumeRecordService.initOrUpdateProcessing(orderMessage, "order-point-consumer");
            orderPointService.grantPoint(orderMessage);
            messageConsumeRecordService.markConsumeSuccess(orderMessage, "order-point-consumer");

            channel.basicAck(deliveryTag, false);
            log.info("订单积分消息消费成功，messageKey={}，orderId={}", orderMessage.getMessageKey(), orderMessage.getOrderId());
        } catch (Exception e) {
            String errorMessage = ExceptionUtil.getRootCauseMessage(e);
            messageConsumeRecordService.markConsumeFailed(orderMessage, "order-point-consumer", errorMessage);
            log.error("订单积分消息消费失败，messageKey={}，原因={}", orderMessage.getMessageKey(), errorMessage, e);

            throw e;
        }
    }
}
```

这里不要在 `catch` 中直接 ACK，否则失败消息会被吞掉。抛出异常后，Spring AMQP 会按照上一节配置进行重试。

### 消费幂等校验

消费幂等依赖 `message_consume_record` 表的唯一索引：

```sql
UNIQUE KEY `uk_message_consumer` (`message_key`, `consumer_group`)
```

消费者先判断是否已消费成功。如果已成功，直接 ACK；如果没有成功，则初始化或更新消费记录。

文件位置：`src/main/java/io/github/atengk/reliablemessage/common/ConsumeStatus.java`

```java
package io.github.atengk.reliablemessage.common;

/**
 * 消费状态
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ConsumeStatus {

    public static final String PROCESSING = "PROCESSING";

    public static final String SUCCESS = "SUCCESS";

    public static final String FAILED = "FAILED";

    private ConsumeStatus() {
    }
}
```

文件位置：`src/main/java/io/github/atengk/reliablemessage/service/MessageConsumeRecordService.java`

```java
package io.github.atengk.reliablemessage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.reliablemessage.entity.MessageConsumeRecord;
import io.github.atengk.reliablemessage.mq.OrderCreatedMessage;

/**
 * 消息消费记录服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface MessageConsumeRecordService extends IService<MessageConsumeRecord> {

    /**
     * 判断消息是否已消费成功
     *
     * @param messageKey    消息Key
     * @param consumerGroup 消费者组
     * @return 是否已成功消费
     */
    boolean isConsumedSuccess(String messageKey, String consumerGroup);

    /**
     * 初始化或更新处理中记录
     *
     * @param message       订单消息
     * @param consumerGroup 消费者组
     */
    void initOrUpdateProcessing(OrderCreatedMessage message, String consumerGroup);

    /**
     * 标记消费成功
     *
     * @param message       订单消息
     * @param consumerGroup 消费者组
     */
    void markConsumeSuccess(OrderCreatedMessage message, String consumerGroup);

    /**
     * 标记消费失败
     *
     * @param message       订单消息
     * @param consumerGroup 消费者组
     * @param reason        失败原因
     */
    void markConsumeFailed(OrderCreatedMessage message, String consumerGroup, String reason);
}
```

文件位置：`src/main/java/io/github/atengk/reliablemessage/service/impl/MessageConsumeRecordServiceImpl.java`

```java
package io.github.atengk.reliablemessage.service.impl;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.reliablemessage.common.ConsumeStatus;
import io.github.atengk.reliablemessage.entity.MessageConsumeRecord;
import io.github.atengk.reliablemessage.mapper.MessageConsumeRecordMapper;
import io.github.atengk.reliablemessage.mq.OrderCreatedMessage;
import io.github.atengk.reliablemessage.service.LocalMessageService;
import io.github.atengk.reliablemessage.service.MessageConsumeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 消息消费记录服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageConsumeRecordServiceImpl
        extends ServiceImpl<MessageConsumeRecordMapper, MessageConsumeRecord>
        implements MessageConsumeRecordService {

    private final LocalMessageService localMessageService;

    /**
     * 判断消息是否已消费成功
     *
     * @param messageKey    消息Key
     * @param consumerGroup 消费者组
     * @return 是否已成功消费
     */
    @Override
    public boolean isConsumedSuccess(String messageKey, String consumerGroup) {
        Long count = lambdaQuery()
                .eq(MessageConsumeRecord::getMessageKey, messageKey)
                .eq(MessageConsumeRecord::getConsumerGroup, consumerGroup)
                .eq(MessageConsumeRecord::getConsumeStatus, ConsumeStatus.SUCCESS)
                .count();

        return count != null && count > 0;
    }

    /**
     * 初始化或更新处理中记录
     *
     * @param message       订单消息
     * @param consumerGroup 消费者组
     */
    @Override
    public void initOrUpdateProcessing(OrderCreatedMessage message, String consumerGroup) {
        LocalDateTime now = LocalDateTime.now();

        MessageConsumeRecord record = new MessageConsumeRecord();
        record.setMessageKey(message.getMessageKey());
        record.setMessageId(message.getMessageId());
        record.setConsumerGroup(consumerGroup);
        record.setBizType("ORDER_CREATED");
        record.setBizId(String.valueOf(message.getOrderId()));
        record.setConsumeStatus(ConsumeStatus.PROCESSING);
        record.setConsumeCount(1);
        record.setCreateTime(now);
        record.setUpdateTime(now);

        try {
            save(record);
        } catch (DuplicateKeyException e) {
            lambdaUpdate()
                    .eq(MessageConsumeRecord::getMessageKey, message.getMessageKey())
                    .eq(MessageConsumeRecord::getConsumerGroup, consumerGroup)
                    .ne(MessageConsumeRecord::getConsumeStatus, ConsumeStatus.SUCCESS)
                    .setSql("consume_count = consume_count + 1")
                    .set(MessageConsumeRecord::getConsumeStatus, ConsumeStatus.PROCESSING)
                    .set(MessageConsumeRecord::getUpdateTime, now)
                    .update();

            log.info("消息消费记录已存在，更新为处理中，messageKey={}，原因={}", message.getMessageKey(), ExceptionUtil.getRootCauseMessage(e));
        }
    }

    /**
     * 标记消费成功
     *
     * @param message       订单消息
     * @param consumerGroup 消费者组
     */
    @Override
    public void markConsumeSuccess(OrderCreatedMessage message, String consumerGroup) {
        lambdaUpdate()
                .eq(MessageConsumeRecord::getMessageKey, message.getMessageKey())
                .eq(MessageConsumeRecord::getConsumerGroup, consumerGroup)
                .set(MessageConsumeRecord::getConsumeStatus, ConsumeStatus.SUCCESS)
                .set(MessageConsumeRecord::getFailReason, null)
                .set(MessageConsumeRecord::getUpdateTime, LocalDateTime.now())
                .update();

        localMessageService.markConsumeSuccess(message.getMessageId());
    }

    /**
     * 标记消费失败
     *
     * @param message       订单消息
     * @param consumerGroup 消费者组
     * @param reason        失败原因
     */
    @Override
    public void markConsumeFailed(OrderCreatedMessage message, String consumerGroup, String reason) {
        MessageConsumeRecord record = getOne(new LambdaQueryWrapper<MessageConsumeRecord>()
                .eq(MessageConsumeRecord::getMessageKey, message.getMessageKey())
                .eq(MessageConsumeRecord::getConsumerGroup, consumerGroup)
                .last("LIMIT 1"));

        if (record == null) {
            return;
        }

        lambdaUpdate()
                .eq(MessageConsumeRecord::getId, record.getId())
                .ne(MessageConsumeRecord::getConsumeStatus, ConsumeStatus.SUCCESS)
                .set(MessageConsumeRecord::getConsumeStatus, ConsumeStatus.FAILED)
                .set(MessageConsumeRecord::getFailReason, StrUtil.subPre(reason, 1000))
                .set(MessageConsumeRecord::getUpdateTime, LocalDateTime.now())
                .update();
    }
}
```

这里的幂等不是只查一次状态，而是通过唯一索引兜底。即使并发重复消费，也不会插入多条消费记录。

### 执行业务处理

本案例用“更新订单积分状态”为例模拟积分发放。真实项目中可以替换为新增积分流水、更新积分账户余额等操作。

文件位置：`src/main/java/io/github/atengk/reliablemessage/service/OrderPointService.java`

```java
package io.github.atengk.reliablemessage.service;

import io.github.atengk.reliablemessage.mq.OrderCreatedMessage;

/**
 * 订单积分服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface OrderPointService {

    /**
     * 发放订单积分
     *
     * @param message 订单创建消息
     */
    void grantPoint(OrderCreatedMessage message);
}
```

文件位置：`src/main/java/io/github/atengk/reliablemessage/service/impl/OrderPointServiceImpl.java`

```java
package io.github.atengk.reliablemessage.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.reliablemessage.entity.BizOrder;
import io.github.atengk.reliablemessage.mapper.BizOrderMapper;
import io.github.atengk.reliablemessage.mq.OrderCreatedMessage;
import io.github.atengk.reliablemessage.service.OrderPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单积分服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPointServiceImpl implements OrderPointService {

    private final BizOrderMapper bizOrderMapper;

    /**
     * 发放订单积分
     *
     * @param message 订单创建消息
     */
    @Override
    public void grantPoint(OrderCreatedMessage message) {
        int updated = bizOrderMapper.update(null, new LambdaUpdateWrapper<BizOrder>()
                .eq(BizOrder::getId, message.getOrderId())
                .eq(BizOrder::getPointStatus, "WAIT_GRANT")
                .set(BizOrder::getPointStatus, "GRANTED")
                .set(BizOrder::getUpdateTime, LocalDateTime.now()));

        if (updated > 0) {
            log.info("订单积分发放成功，orderId={}，userId={}", message.getOrderId(), message.getUserId());
            return;
        }

        BizOrder order = bizOrderMapper.selectById(message.getOrderId());
        if (order != null && "GRANTED".equals(order.getPointStatus())) {
            log.info("订单积分已发放，本次跳过，orderId={}", message.getOrderId());
            return;
        }

        throw new IllegalStateException("订单不存在或积分状态异常，orderId=" + message.getOrderId());
    }
}
```

这里通过 `WHERE point_status = 'WAIT_GRANT'` 做业务状态幂等。即使消费记录失效，也不会重复把同一订单的积分状态从已发放再次处理。

### 记录消费结果

消费成功后，需要同时更新消费记录和本地消息状态。这里已经在 `MessageConsumeRecordServiceImpl#markConsumeSuccess` 中完成：

```java
lambdaUpdate()
        .eq(MessageConsumeRecord::getMessageKey, message.getMessageKey())
        .eq(MessageConsumeRecord::getConsumerGroup, consumerGroup)
        .set(MessageConsumeRecord::getConsumeStatus, ConsumeStatus.SUCCESS)
        .set(MessageConsumeRecord::getFailReason, null)
        .set(MessageConsumeRecord::getUpdateTime, LocalDateTime.now())
        .update();

localMessageService.markConsumeSuccess(message.getMessageId());
```

这一步的结果是：

```text
message_consume_record.consume_status = SUCCESS
local_message.message_status = CONSUME_SUCCESS
biz_order.point_status = GRANTED
```

这三个状态都成功后，说明该条消息已经完成最终一致。

### 消费异常处理

消费异常时，不要手动 ACK。应该记录失败原因，然后继续抛出异常，让 RabbitMQ 重试和死信机制接管。

消费者中的异常处理逻辑如下：

```java
catch (Exception e) {
    String errorMessage = ExceptionUtil.getRootCauseMessage(e);
    messageConsumeRecordService.markConsumeFailed(orderMessage, "order-point-consumer", errorMessage);
    log.error("订单积分消息消费失败，messageKey={}，原因={}", orderMessage.getMessageKey(), errorMessage, e);

    throw e;
}
```

异常处理原则如下：

```text
1. 业务成功后再 ACK
2. 业务失败不要 ACK
3. 消费失败要记录失败原因
4. 重试耗尽后进入死信队列
5. 死信队列只做记录和告警，不做无限重发
```

这样可以避免“消费失败但消息被确认”的数据丢失问题。

## 可靠性补偿实现

补偿逻辑用于兜底生产者和消费者两侧异常。生产者侧主要补偿 `WAIT_SEND` 和 `SEND_FAILED` 消息；消费者侧主要通过死信队列和人工补偿处理异常消息。

### 未发送消息扫描任务

未发送消息一般来自以下场景：

```text
1. 订单事务提交成功后，应用在投递 MQ 前宕机
2. RabbitTemplate 发送前发生异常
3. Confirm Callback 长时间未返回
4. 消息仍停留在 WAIT_SEND 状态
```

使用 XXL-JOB 定时扫描 `WAIT_SEND` 消息，并重新投递。

文件位置：`src/main/java/io/github/atengk/reliablemessage/job/ReliableMessageJobHandler.java`

```java
package io.github.atengk.reliablemessage.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.reliablemessage.common.MessageStatus;
import io.github.atengk.reliablemessage.entity.LocalMessage;
import io.github.atengk.reliablemessage.mq.ReliableMessageProducer;
import io.github.atengk.reliablemessage.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 可靠消息补偿任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReliableMessageJobHandler {

    private final LocalMessageService localMessageService;

    private final ReliableMessageProducer reliableMessageProducer;

    /**
     * 扫描待发送消息
     */
    @XxlJob("scanWaitSendMessageJob")
    public void scanWaitSendMessageJob() {
        List<LocalMessage> messages = localMessageService.listRetryableMessages(MessageStatus.WAIT_SEND, 100);
        log.info("开始扫描待发送消息，数量={}", messages.size());

        for (LocalMessage message : messages) {
            localMessageService.increaseRetryCount(message.getId());
            reliableMessageProducer.sendLocalMessage(message.getId());
        }

        log.info("待发送消息扫描完成，数量={}", messages.size());
    }

    /**
     * 扫描发送失败消息
     */
    @XxlJob("retryFailedMessageJob")
    public void retryFailedMessageJob() {
        List<LocalMessage> messages = localMessageService.listRetryableMessages(MessageStatus.SEND_FAILED, 100);
        log.info("开始扫描发送失败消息，数量={}", messages.size());

        for (LocalMessage message : messages) {
            localMessageService.increaseRetryCount(message.getId());
            reliableMessageProducer.sendLocalMessage(message.getId());
        }

        log.info("发送失败消息扫描完成，数量={}", messages.size());
    }
}
```

调度建议：

```text
scanWaitSendMessageJob：每 1 分钟执行一次
单次处理数量：100
最大重试次数：5
超过最大重试次数：标记 DEAD 或进入人工处理
```

### 发送失败消息重试任务

发送失败消息来自以下场景：

```text
1. RabbitMQ 不可用
2. Confirm Callback 返回 NACK
3. 交换机不存在
4. 路由失败
5. 网络抖动
```

上面的 `retryFailedMessageJob` 会扫描 `SEND_FAILED` 状态并重新发送。为了避免异常消息无限重试，`listRetryableMessages` 中已经限制：

```java
.apply("retry_count < max_retry_count")
```

如果需要把超过最大重试次数的消息自动标记为 `DEAD`，可以增加一个任务。

文件位置：`src/main/java/io/github/atengk/reliablemessage/job/MessageDeadCheckJobHandler.java`

```java
package io.github.atengk.reliablemessage.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.reliablemessage.common.MessageStatus;
import io.github.atengk.reliablemessage.entity.LocalMessage;
import io.github.atengk.reliablemessage.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息重试上限检查任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDeadCheckJobHandler {

    private final LocalMessageService localMessageService;

    /**
     * 标记超过最大重试次数的消息为死信
     */
    @XxlJob("markOverRetryMessageDeadJob")
    public void markOverRetryMessageDeadJob() {
        List<LocalMessage> messages = localMessageService.list(new LambdaQueryWrapper<LocalMessage>()
                .in(LocalMessage::getMessageStatus, MessageStatus.WAIT_SEND, MessageStatus.SEND_FAILED)
                .apply("retry_count >= max_retry_count")
                .last("LIMIT 100"));

        log.info("开始检查超过最大重试次数的消息，数量={}", messages.size());

        for (LocalMessage message : messages) {
            localMessageService.markDead(message.getId(), "超过最大投递重试次数");
        }

        log.info("超过最大重试次数的消息检查完成，数量={}", messages.size());
    }
}
```

这个任务建议每 5 分钟执行一次。它不会重新发送消息，只负责把不可自动恢复的消息转为 `DEAD`，等待人工确认。

### 死信队列监听

死信队列监听器用于处理消费端重试耗尽的消息。这里不做无限重发，只更新本地消息状态，记录异常日志。

文件位置：`src/main/java/io/github/atengk/reliablemessage/mq/OrderDeadLetterConsumer.java`

```java
package io.github.atengk.reliablemessage.mq;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.reliablemessage.common.RabbitMqConstant;
import io.github.atengk.reliablemessage.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单死信消息消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeadLetterConsumer {

    private final LocalMessageService localMessageService;

    /**
     * 监听死信队列
     *
     * @param message RabbitMQ消息
     * @param channel RabbitMQ通道
     * @throws Exception ACK异常
     */
    @RabbitListener(queues = RabbitMqConstant.ORDER_DLX_QUEUE)
    public void consumeDeadLetter(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageBody = new String(message.getBody());

        try {
            OrderCreatedMessage orderMessage = JSONUtil.toBean(messageBody, OrderCreatedMessage.class);
            localMessageService.markDead(orderMessage.getMessageId(), "消费重试耗尽，进入死信队列");

            channel.basicAck(deliveryTag, false);
            log.error("订单消息进入死信队列，messageKey={}，orderId={}", orderMessage.getMessageKey(), orderMessage.getOrderId());
        } catch (Exception e) {
            log.error("死信消息处理异常，body={}，原因={}", messageBody, ExceptionUtil.getRootCauseMessage(e), e);

            channel.basicAck(deliveryTag, false);
        }
    }
}
```

这里对解析异常的死信消息也执行 ACK，是为了避免坏消息长期阻塞死信队列。真实项目中建议把原始消息体落库到异常消息表，便于后续排查。

### 人工补偿入口

人工补偿入口用于处理 `DEAD`、`SEND_FAILED` 等状态的消息。常见操作包括重新投递、关闭消息、查询消息详情。

文件位置：`src/main/java/io/github/atengk/reliablemessage/controller/ReliableMessageController.java`

```java
package io.github.atengk.reliablemessage.controller;

import io.github.atengk.reliablemessage.common.MessageStatus;
import io.github.atengk.reliablemessage.entity.LocalMessage;
import io.github.atengk.reliablemessage.mq.ReliableMessageProducer;
import io.github.atengk.reliablemessage.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 可靠消息人工补偿接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/reliable-messages")
public class ReliableMessageController {

    private final LocalMessageService localMessageService;

    private final ReliableMessageProducer reliableMessageProducer;

    /**
     * 查询消息详情
     *
     * @param messageId 消息ID
     * @return 消息详情
     */
    @GetMapping("/{messageId}")
    public LocalMessage getMessage(@PathVariable Long messageId) {
        return localMessageService.getById(messageId);
    }

    /**
     * 人工重新投递消息
     *
     * @param messageId 消息ID
     * @return 处理结果
     */
    @PostMapping("/{messageId}/resend")
    public String resendMessage(@PathVariable Long messageId) {
        LocalMessage message = localMessageService.getById(messageId);
        if (message == null) {
            return "消息不存在";
        }

        if (MessageStatus.CONSUME_SUCCESS.equals(message.getMessageStatus())) {
            return "消息已消费成功，无需重新投递";
        }

        localMessageService.increaseRetryCount(messageId);
        reliableMessageProducer.sendLocalMessage(messageId);

        log.info("人工重新投递消息，messageId={}，messageKey={}", messageId, message.getMessageKey());
        return "已提交重新投递";
    }

    /**
     * 人工关闭消息
     *
     * @param messageId 消息ID
     * @return 处理结果
     */
    @PostMapping("/{messageId}/close")
    public String closeMessage(@PathVariable Long messageId) {
        LocalMessage message = localMessageService.getById(messageId);
        if (message == null) {
            return "消息不存在";
        }

        if (MessageStatus.CONSUME_SUCCESS.equals(message.getMessageStatus())) {
            return "消息已消费成功，不能关闭";
        }

        LocalMessage update = new LocalMessage();
        update.setId(messageId);
        update.setMessageStatus(MessageStatus.CLOSED);
        update.setFailReason("人工关闭");
        update.setUpdateTime(LocalDateTime.now());
        localMessageService.updateById(update);

        log.info("人工关闭消息，messageId={}，messageKey={}", messageId, message.getMessageKey());
        return "消息已关闭";
    }
}
```

接口调用示例：

```bash
# 查询消息详情
curl -X GET "http://localhost:8080/reliable-messages/10001"

# 人工重新投递消息
curl -X POST "http://localhost:8080/reliable-messages/10001/resend"

# 人工关闭无需继续补偿的消息
curl -X POST "http://localhost:8080/reliable-messages/10001/close"
```

人工补偿要有权限控制和操作审计。生产环境中不建议让普通用户直接访问这些接口，通常只开放给内部运营后台、运维后台或管理端。

## 接口与验证

本章节给出可直接调用的测试接口，用于验证“订单创建、本地消息落库、MQ 投递、消费成功、消费失败、死信补偿、人工重投”等核心链路。该部分对应原场景中的“消费者消费消息、记录消费结果、失败重试、死信补偿、定时扫描未发送消息”。

### 创建订单接口

创建订单接口用于触发完整可靠消息链路：保存订单、写入本地消息表、事务提交后投递 MQ、消费者异步发放积分。

文件位置：`src/main/java/io/github/atengk/reliablemessage/controller/OrderController.java`

```java
package io.github.atengk.reliablemessage.controller;

import io.github.atengk.reliablemessage.dto.OrderCreateRequest;
import io.github.atengk.reliablemessage.service.impl.OrderServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单测试接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderServiceImpl orderService;

    /**
     * 创建订单
     *
     * @param request 创建订单请求
     * @return 订单ID
     */
    @PostMapping
    public Long createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(request);
    }
}
```

调用示例：

```bash
curl -X POST "http://localhost:8080/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "amount": 199.90,
    "remark": "可靠消息测试订单"
  }'
```

正常返回示例：

```json
1956638135478620160
```

创建成功后，应该能在数据库中看到：

```text
biz_order.order_status = CREATED
biz_order.point_status = GRANTED
local_message.message_status = CONSUME_SUCCESS
message_consume_record.consume_status = SUCCESS
```

### 查询消息状态接口

查询消息状态接口用于观察本地消息表状态变化。常用来确认消息是否已经发送成功、消费成功、发送失败或进入死信状态。

文件位置：`src/main/java/io/github/atengk/reliablemessage/controller/ReliableMessageQueryController.java`

```java
package io.github.atengk.reliablemessage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.reliablemessage.entity.LocalMessage;
import io.github.atengk.reliablemessage.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 可靠消息查询接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/messages")
public class ReliableMessageQueryController {

    private final LocalMessageService localMessageService;

    /**
     * 根据消息ID查询消息
     *
     * @param messageId 消息ID
     * @return 本地消息
     */
    @GetMapping("/{messageId}")
    public LocalMessage getById(@PathVariable Long messageId) {
        return localMessageService.getById(messageId);
    }

    /**
     * 根据消息Key查询消息
     *
     * @param messageKey 消息Key
     * @return 本地消息
     */
    @GetMapping("/key/{messageKey}")
    public LocalMessage getByMessageKey(@PathVariable String messageKey) {
        return localMessageService.getOne(new LambdaQueryWrapper<LocalMessage>()
                .eq(LocalMessage::getMessageKey, messageKey)
                .last("LIMIT 1"));
    }
}
```

调用示例：

```bash
# 根据消息ID查询
curl -X GET "http://localhost:8080/messages/1956638135478620161"

# 根据消息Key查询
curl -X GET "http://localhost:8080/messages/key/ORDER_CREATED:1956638135478620160"
```

返回示例：

```json
{
  "id": 1956638135478620161,
  "messageKey": "ORDER_CREATED:1956638135478620160",
  "bizType": "ORDER_CREATED",
  "bizId": "1956638135478620160",
  "exchangeName": "reliable.order.exchange",
  "routingKey": "reliable.order.point.routing",
  "messageStatus": "CONSUME_SUCCESS",
  "retryCount": 0,
  "maxRetryCount": 5
}
```

### 模拟消费失败接口

模拟消费失败接口用于验证 RabbitMQ 消费重试和死信队列。这里通过一个开关控制积分消费者是否主动抛出异常。

文件位置：`src/main/java/io/github/atengk/reliablemessage/support/ConsumerFailureSwitch.java`

```java
package io.github.atengk.reliablemessage.support;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 消费失败模拟开关
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
public class ConsumerFailureSwitch {

    private final AtomicBoolean enabled = new AtomicBoolean(false);

    /**
     * 开启失败模拟
     */
    public void enable() {
        enabled.set(true);
    }

    /**
     * 关闭失败模拟
     */
    public void disable() {
        enabled.set(false);
    }

    /**
     * 判断是否开启失败模拟
     *
     * @return 是否开启
     */
    public boolean isEnabled() {
        return enabled.get();
    }
}
```

在积分发放服务中加入失败模拟判断。

文件位置：`src/main/java/io/github/atengk/reliablemessage/service/impl/OrderPointServiceImpl.java`

```java
package io.github.atengk.reliablemessage.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.reliablemessage.entity.BizOrder;
import io.github.atengk.reliablemessage.mapper.BizOrderMapper;
import io.github.atengk.reliablemessage.mq.OrderCreatedMessage;
import io.github.atengk.reliablemessage.service.OrderPointService;
import io.github.atengk.reliablemessage.support.ConsumerFailureSwitch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单积分服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPointServiceImpl implements OrderPointService {

    private final BizOrderMapper bizOrderMapper;

    private final ConsumerFailureSwitch consumerFailureSwitch;

    /**
     * 发放订单积分
     *
     * @param message 订单创建消息
     */
    @Override
    public void grantPoint(OrderCreatedMessage message) {
        if (consumerFailureSwitch.isEnabled()) {
            log.warn("当前已开启消费失败模拟，messageKey={}", message.getMessageKey());
            throw new IllegalStateException("模拟积分服务异常");
        }

        int updated = bizOrderMapper.update(null, new LambdaUpdateWrapper<BizOrder>()
                .eq(BizOrder::getId, message.getOrderId())
                .eq(BizOrder::getPointStatus, "WAIT_GRANT")
                .set(BizOrder::getPointStatus, "GRANTED")
                .set(BizOrder::getUpdateTime, LocalDateTime.now()));

        if (updated > 0) {
            log.info("订单积分发放成功，orderId={}，userId={}", message.getOrderId(), message.getUserId());
            return;
        }

        BizOrder order = bizOrderMapper.selectById(message.getOrderId());
        if (order != null && "GRANTED".equals(order.getPointStatus())) {
            log.info("订单积分已发放，本次跳过，orderId={}", message.getOrderId());
            return;
        }

        throw new IllegalStateException("订单不存在或积分状态异常，orderId=" + message.getOrderId());
    }
}
```

提供测试开关接口。

文件位置：`src/main/java/io/github/atengk/reliablemessage/controller/ConsumerFailureController.java`

```java
package io.github.atengk.reliablemessage.controller;

import io.github.atengk.reliablemessage.support.ConsumerFailureSwitch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 消费失败模拟接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/consumer-failure")
public class ConsumerFailureController {

    private final ConsumerFailureSwitch consumerFailureSwitch;

    /**
     * 开启消费失败模拟
     *
     * @return 操作结果
     */
    @PostMapping("/enable")
    public String enable() {
        consumerFailureSwitch.enable();
        return "已开启消费失败模拟";
    }

    /**
     * 关闭消费失败模拟
     *
     * @return 操作结果
     */
    @PostMapping("/disable")
    public String disable() {
        consumerFailureSwitch.disable();
        return "已关闭消费失败模拟";
    }

    /**
     * 查询消费失败模拟状态
     *
     * @return 是否开启
     */
    @GetMapping
    public Boolean status() {
        return consumerFailureSwitch.isEnabled();
    }
}
```

调用示例：

```bash
# 开启消费失败模拟
curl -X POST "http://localhost:8080/test/consumer-failure/enable"

# 创建订单，触发消费失败
curl -X POST "http://localhost:8080/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "amount": 88.00,
    "remark": "模拟消费失败订单"
  }'

# 关闭消费失败模拟
curl -X POST "http://localhost:8080/test/consumer-failure/disable"
```

开启失败模拟后，消费者会持续抛出异常。超过 RabbitMQ 重试次数后，消息会进入死信队列，本地消息状态会被标记为 `DEAD`。

### 接口测试流程

完整测试顺序建议如下：

```text
1. 启动 MySQL
2. 启动 RabbitMQ
3. 初始化数据库表
4. 启动 Spring Boot 项目
5. 调用创建订单接口
6. 查询 biz_order，确认订单已创建
7. 查询 local_message，确认消息状态为 CONSUME_SUCCESS
8. 查询 message_consume_record，确认消费状态为 SUCCESS
9. 开启消费失败模拟
10. 再次创建订单
11. 等待消费重试耗尽
12. 查询 local_message，确认消息状态为 DEAD
13. 关闭消费失败模拟
14. 调用人工重新投递接口
15. 再次查询消息状态，确认补偿成功
```

常用 SQL 验证：

```sql
-- 查看最近订单
SELECT *
FROM biz_order
ORDER BY create_time DESC
LIMIT 5;

-- 查看最近本地消息
SELECT id, message_key, biz_type, biz_id, message_status, retry_count, fail_reason, create_time, update_time
FROM local_message
ORDER BY create_time DESC
LIMIT 5;

-- 查看最近消费记录
SELECT id, message_key, consumer_group, consume_status, consume_count, fail_reason, create_time, update_time
FROM message_consume_record
ORDER BY create_time DESC
LIMIT 5;
```

## 核心代码清单

本章节整理本案例的核心文件。前文已经给出了生产者、消费者、RabbitMQ 配置和补偿任务的主要代码，这里补齐实体类、Mapper、Service 层接口和控制器清单。

### Entity 实体类

实体类对应三张核心表：业务订单表、本地消息表、消费记录表。

文件位置：`src/main/java/io/github/atengk/reliablemessage/entity/BizOrder.java`

```java
package io.github.atengk.reliablemessage.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务订单实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("biz_order")
public class BizOrder {

    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    private String orderStatus;

    private String pointStatus;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/reliablemessage/entity/LocalMessage.java`

```java
package io.github.atengk.reliablemessage.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("local_message")
public class LocalMessage {

    private Long id;

    private String messageKey;

    private String bizType;

    private String bizId;

    private String exchangeName;

    private String routingKey;

    private String messageBody;

    private String messageStatus;

    private Integer retryCount;

    private Integer maxRetryCount;

    private LocalDateTime nextRetryTime;

    private String failReason;

    private LocalDateTime confirmTime;

    private LocalDateTime consumeTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/reliablemessage/entity/MessageConsumeRecord.java`

```java
package io.github.atengk.reliablemessage.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息消费记录实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("message_consume_record")
public class MessageConsumeRecord {

    private Long id;

    private String messageKey;

    private Long messageId;

    private String consumerGroup;

    private String bizType;

    private String bizId;

    private String consumeStatus;

    private Integer consumeCount;

    private String failReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

### Mapper 持久层

Mapper 使用 MyBatis-Plus `BaseMapper` 即可满足本案例的核心 CRUD 需求。

文件位置：`src/main/java/io/github/atengk/reliablemessage/mapper/BizOrderMapper.java`

```java
package io.github.atengk.reliablemessage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.reliablemessage.entity.BizOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务订单 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface BizOrderMapper extends BaseMapper<BizOrder> {
}
```

文件位置：`src/main/java/io/github/atengk/reliablemessage/mapper/LocalMessageMapper.java`

```java
package io.github.atengk.reliablemessage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.reliablemessage.entity.LocalMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地消息 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface LocalMessageMapper extends BaseMapper<LocalMessage> {
}
```

文件位置：`src/main/java/io/github/atengk/reliablemessage/mapper/MessageConsumeRecordMapper.java`

```java
package io.github.atengk.reliablemessage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.reliablemessage.entity.MessageConsumeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息消费记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface MessageConsumeRecordMapper extends BaseMapper<MessageConsumeRecord> {
}
```

### Service 业务层

Service 层主要包含以下组件：

| 类                                | 作用                                             |
| --------------------------------- | ------------------------------------------------ |
| `OrderServiceImpl`                | 创建订单，写入本地消息，事务提交后投递 MQ        |
| `LocalMessageService`             | 本地消息状态流转                                 |
| `LocalMessageServiceImpl`         | 实现发送成功、发送失败、消费成功、死信、重试查询 |
| `MessageConsumeRecordService`     | 消费幂等记录                                     |
| `MessageConsumeRecordServiceImpl` | 处理消费记录初始化、成功、失败                   |
| `OrderPointService`               | 积分发放业务                                     |
| `OrderPointServiceImpl`           | 模拟积分发放和消费失败                           |

前文已经给出核心完整代码。这里需要注意两点：

```text
1. 创建订单和写入本地消息必须在同一个 @Transactional 中
2. 消费者业务处理必须同时依赖消费记录幂等和业务状态幂等
```

### RabbitMQ 配置类

RabbitMQ 配置类负责声明业务交换机、业务队列、死信交换机、死信队列及绑定关系。

核心文件：

```text
src/main/java/io/github/atengk/reliablemessage/config/RabbitMqConfig.java
```

配置内容包括：

```text
1. reliable.order.exchange
2. reliable.order.point.queue
3. reliable.order.point.routing
4. reliable.order.dlx.exchange
5. reliable.order.dlx.queue
6. reliable.order.dlx.routing
```

业务队列必须绑定死信交换机：

```java
QueueBuilder.durable(RabbitMqConstant.ORDER_POINT_QUEUE)
        .deadLetterExchange(RabbitMqConstant.ORDER_DLX_EXCHANGE)
        .deadLetterRoutingKey(RabbitMqConstant.ORDER_DLX_ROUTING_KEY)
        .build();
```

### Producer 投递组件

Producer 负责读取本地消息表并投递 RabbitMQ。

核心文件：

```text
src/main/java/io/github/atengk/reliablemessage/mq/ReliableMessageProducer.java
src/main/java/io/github/atengk/reliablemessage/mq/RabbitProducerCallback.java
```

核心职责：

```text
1. 根据 messageId 查询 local_message
2. 使用 exchangeName + routingKey 投递消息
3. 使用 CorrelationData 绑定 messageId
4. Confirm ACK 后标记 SEND_SUCCESS
5. Confirm NACK 或发送异常后标记 SEND_FAILED
```

### Consumer 消费组件

Consumer 负责监听业务队列和死信队列。

核心文件：

```text
src/main/java/io/github/atengk/reliablemessage/mq/OrderPointConsumer.java
src/main/java/io/github/atengk/reliablemessage/mq/OrderDeadLetterConsumer.java
```

核心职责：

```text
1. 监听 reliable.order.point.queue
2. 解析订单创建消息
3. 根据 message_key + consumer_group 做幂等判断
4. 发放积分
5. 记录消费成功
6. 手动 ACK
7. 消费失败抛出异常，交给重试和死信机制
8. 监听死信队列并标记 DEAD
```

### XXL-JOB 补偿任务

XXL-JOB 负责扫描未发送、发送失败、超过最大重试次数的消息。

核心文件：

```text
src/main/java/io/github/atengk/reliablemessage/job/ReliableMessageJobHandler.java
src/main/java/io/github/atengk/reliablemessage/job/MessageDeadCheckJobHandler.java
```

推荐任务配置：

| JobHandler                    | Cron            | 作用                            |
| ----------------------------- | --------------- | ------------------------------- |
| `scanWaitSendMessageJob`      | `0 */1 * * * ?` | 每 1 分钟扫描待发送消息         |
| `retryFailedMessageJob`       | `0 */5 * * * ?` | 每 5 分钟扫描发送失败消息       |
| `markOverRetryMessageDeadJob` | `0 */5 * * * ?` | 每 5 分钟标记超过重试上限的消息 |

补偿任务必须限制单次处理数量，建议每次最多处理 100 条。

### Controller 测试接口

本案例测试接口如下：

| 接口                                    | 方法 | 作用                      |
| --------------------------------------- | ---- | ------------------------- |
| `/orders`                               | POST | 创建订单并触发可靠消息    |
| `/messages/{messageId}`                 | GET  | 根据消息 ID 查询本地消息  |
| `/messages/key/{messageKey}`            | GET  | 根据消息 Key 查询本地消息 |
| `/test/consumer-failure/enable`         | POST | 开启消费失败模拟          |
| `/test/consumer-failure/disable`        | POST | 关闭消费失败模拟          |
| `/test/consumer-failure`                | GET  | 查询消费失败模拟状态      |
| `/reliable-messages/{messageId}/resend` | POST | 人工重新投递消息          |
| `/reliable-messages/{messageId}/close`  | POST | 人工关闭消息              |

## 运行与测试

本章节给出从环境启动到功能验证的完整流程。建议先跑通正常链路，再验证失败补偿和死信处理。

### 启动 MySQL 和 RabbitMQ

可以使用 Docker 快速启动 MySQL 和 RabbitMQ。

```bash
docker run -d \
  --name reliable-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=reliable_message_demo \
  mysql:8.0

docker run -d \
  --name reliable-rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin \
  rabbitmq:3.12-management
```

参数说明：

```text
3306：MySQL 数据库端口
5672：RabbitMQ AMQP 通信端口
15672：RabbitMQ 管理后台端口
RABBITMQ_DEFAULT_USER：RabbitMQ 用户名
RABBITMQ_DEFAULT_PASS：RabbitMQ 密码
```

RabbitMQ 管理后台访问：

```text
http://localhost:15672
username: admin
password: admin
```

### 初始化数据库脚本

在 MySQL 中执行以下脚本，创建订单表、本地消息表和消费记录表。

```sql
CREATE DATABASE IF NOT EXISTS reliable_message_demo
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE reliable_message_demo;

DROP TABLE IF EXISTS message_consume_record;
DROP TABLE IF EXISTS local_message;
DROP TABLE IF EXISTS biz_order;

CREATE TABLE `biz_order` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `amount` DECIMAL(18,2) NOT NULL COMMENT '订单金额',
  `order_status` VARCHAR(32) NOT NULL COMMENT '订单状态：CREATED-已创建，CLOSED-已关闭',
  `point_status` VARCHAR(32) NOT NULL DEFAULT 'WAIT_GRANT' COMMENT '积分状态：WAIT_GRANT-待发放，GRANTED-已发放',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务订单表';

CREATE TABLE `local_message` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `message_key` VARCHAR(128) NOT NULL COMMENT '消息唯一Key',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型，例如 ORDER_CREATED',
  `biz_id` VARCHAR(64) NOT NULL COMMENT '业务ID，例如订单ID',
  `exchange_name` VARCHAR(128) NOT NULL COMMENT '交换机名称',
  `routing_key` VARCHAR(128) NOT NULL COMMENT '路由Key',
  `message_body` TEXT NOT NULL COMMENT '消息体JSON',
  `message_status` VARCHAR(32) NOT NULL COMMENT '消息状态：WAIT_SEND，SEND_SUCCESS，SEND_FAILED，CONSUME_SUCCESS，DEAD，CLOSED',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `max_retry_count` INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
  `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
  `fail_reason` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
  `confirm_time` DATETIME DEFAULT NULL COMMENT 'MQ确认时间',
  `consume_time` DATETIME DEFAULT NULL COMMENT '消费成功时间',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_key` (`message_key`),
  KEY `idx_biz_type_biz_id` (`biz_type`, `biz_id`),
  KEY `idx_status_retry_time` (`message_status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表';

CREATE TABLE `message_consume_record` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `message_key` VARCHAR(128) NOT NULL COMMENT '消息唯一Key',
  `message_id` BIGINT NOT NULL COMMENT '本地消息ID',
  `consumer_group` VARCHAR(128) NOT NULL COMMENT '消费者分组',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型',
  `biz_id` VARCHAR(64) NOT NULL COMMENT '业务ID',
  `consume_status` VARCHAR(32) NOT NULL COMMENT '消费状态：PROCESSING，SUCCESS，FAILED',
  `consume_count` INT NOT NULL DEFAULT 0 COMMENT '消费次数',
  `fail_reason` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_consumer` (`message_key`, `consumer_group`),
  KEY `idx_biz_type_biz_id` (`biz_type`, `biz_id`),
  KEY `idx_consume_status` (`consume_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息消费记录表';
```

### 启动 Spring Boot 项目

确认 `application.yml` 中 MySQL 和 RabbitMQ 配置正确。

```yaml
server:
  port: 8080

spring:
  application:
    name: reliable-message-demo

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/reliable_message_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root

  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: admin
    password: admin
    virtual-host: /
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1
        retry:
          enabled: true
          initial-interval: 1000ms
          max-attempts: 3
          max-interval: 10000ms
          multiplier: 2

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: assign_id
```

启动命令：

```bash
mvn clean spring-boot:run
```

启动成功后，需要在 RabbitMQ 管理后台看到以下队列：

```text
reliable.order.point.queue
reliable.order.dlx.queue
```

### 验证正常投递消费

创建一笔订单。

```bash
curl -X POST "http://localhost:8080/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "amount": 199.90,
    "remark": "正常投递消费测试"
  }'
```

查询数据库：

```sql
SELECT id, order_no, user_id, amount, order_status, point_status
FROM biz_order
ORDER BY create_time DESC
LIMIT 1;

SELECT id, message_key, message_status, retry_count, fail_reason
FROM local_message
ORDER BY create_time DESC
LIMIT 1;

SELECT message_key, consumer_group, consume_status, consume_count, fail_reason
FROM message_consume_record
ORDER BY create_time DESC
LIMIT 1;
```

预期结果：

```text
biz_order.point_status = GRANTED
local_message.message_status = CONSUME_SUCCESS
message_consume_record.consume_status = SUCCESS
message_consume_record.consume_count = 1
```

### 验证重复消费幂等

重复消费可以通过人工重新投递同一条已消费成功的消息来验证。

先查询最近一条消息 ID：

```sql
SELECT id, message_key, message_status
FROM local_message
ORDER BY create_time DESC
LIMIT 1;
```

调用人工重投接口：

```bash
curl -X POST "http://localhost:8080/reliable-messages/{messageId}/resend"
```

再次查询消费记录：

```sql
SELECT message_key, consumer_group, consume_status, consume_count
FROM message_consume_record
WHERE message_key = 'ORDER_CREATED:你的订单ID';
```

预期结果：

```text
1. 不会重复发放积分
2. biz_order.point_status 仍然是 GRANTED
3. 消费者日志会输出“消息已消费成功，本次直接ACK”
```

这里验证的是两层幂等：

```text
1. message_consume_record 使用 message_key + consumer_group 防止重复消费
2. biz_order 使用 point_status = WAIT_GRANT 条件防止重复执行业务状态变更
```

### 验证发送失败补偿

发送失败补偿可以通过临时关闭 RabbitMQ 或修改错误交换机来验证。更推荐在本地测试时临时停止 RabbitMQ。

停止 RabbitMQ：

```bash
docker stop reliable-rabbitmq
```

创建订单：

```bash
curl -X POST "http://localhost:8080/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10002,
    "amount": 66.60,
    "remark": "发送失败补偿测试"
  }'
```

此时订单会创建成功，本地消息表会保留消息，状态通常为 `WAIT_SEND` 或 `SEND_FAILED`。

```sql
SELECT id, message_key, message_status, retry_count, next_retry_time, fail_reason
FROM local_message
ORDER BY create_time DESC
LIMIT 1;
```

重新启动 RabbitMQ：

```bash
docker start reliable-rabbitmq
```

等待 XXL-JOB 扫描任务执行，或手动调用人工重投接口：

```bash
curl -X POST "http://localhost:8080/reliable-messages/{messageId}/resend"
```

最终预期结果：

```text
local_message.message_status = CONSUME_SUCCESS
biz_order.point_status = GRANTED
message_consume_record.consume_status = SUCCESS
```

如果没有接入 XXL-JOB 调度中心，也可以先通过人工重投接口验证补偿逻辑。

### 验证死信补偿

死信补偿用于验证消费者连续失败后的兜底处理。

开启消费失败模拟：

```bash
curl -X POST "http://localhost:8080/test/consumer-failure/enable"
```

创建订单：

```bash
curl -X POST "http://localhost:8080/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10003,
    "amount": 88.80,
    "remark": "死信补偿测试"
  }'
```

等待 RabbitMQ 消费重试耗尽后，查询消息状态：

```sql
SELECT id, message_key, message_status, retry_count, fail_reason
FROM local_message
ORDER BY create_time DESC
LIMIT 1;

SELECT message_key, consumer_group, consume_status, consume_count, fail_reason
FROM message_consume_record
ORDER BY create_time DESC
LIMIT 1;
```

预期结果：

```text
local_message.message_status = DEAD
message_consume_record.consume_status = FAILED
fail_reason 包含模拟异常信息
biz_order.point_status = WAIT_GRANT
```

关闭消费失败模拟：

```bash
curl -X POST "http://localhost:8080/test/consumer-failure/disable"
```

人工重新投递死信消息：

```bash
curl -X POST "http://localhost:8080/reliable-messages/{messageId}/resend"
```

再次查询状态：

```sql
SELECT id, message_key, message_status, consume_time
FROM local_message
WHERE id = {messageId};

SELECT id, point_status
FROM biz_order
WHERE id = {orderId};

SELECT message_key, consume_status, consume_count
FROM message_consume_record
WHERE message_key = 'ORDER_CREATED:{orderId}';
```

最终预期结果：

```text
local_message.message_status = CONSUME_SUCCESS
biz_order.point_status = GRANTED
message_consume_record.consume_status = SUCCESS
```

到这里，正常投递、重复消费、发送失败补偿、消费失败死信、死信人工恢复这几条核心链路都已经验证完成。