# 支付回调与对账补偿

本文档基于“支付回调与对账补偿”业务场景展开，重点实现支付单创建、第三方支付回调、回调验签、金额校验、幂等更新、订单状态推进、主动查询补偿和对账差异处理等核心能力。该场景的关键难点包括重复回调、通知丢失、状态不可逆、金额精度、支付单与业务单一致性、主动查询补偿和对账差异处理。

## 实现目标

本案例目标是实现一套可落地的支付回调与对账补偿核心链路。整体设计不追求完整支付平台能力，而是聚焦 Java 后端项目中最常见、最能体现工程能力的支付核心流程。

最终实现后，系统应具备以下能力：

| 能力         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 创建支付单   | 根据业务订单创建平台支付单，生成唯一支付单号                 |
| 调用支付渠道 | 模拟调用第三方支付渠道，返回支付参数或支付链接               |
| 接收支付回调 | 提供支付渠道回调接口，接收支付成功、失败、关闭等通知         |
| 回调验签     | 使用 Hutool 工具类完成签名校验，防止伪造回调                 |
| 金额校验     | 使用 BigDecimal 严格校验支付金额，避免金额精度问题           |
| 幂等处理     | 重复回调不会重复更新支付单、重复推进订单、重复发送 MQ        |
| 状态不可逆   | 支付成功后不能被回退为未支付、支付失败或已关闭               |
| 订单状态推进 | 支付成功后通过 MQ 通知订单系统更新业务订单状态               |
| 主动查询补偿 | 定时扫描长时间未完成的支付单，主动查询第三方状态             |
| 对账差异处理 | 对平台支付单和渠道账单进行匹配，识别金额差异、状态差异和缺失数据 |

本案例会重点实现核心链路，不引入过多外围能力。比如真实支付 SDK、复杂清结算、退款、分账、支付路由、商户结算等内容不在本章节展开。

### 业务场景

该功能适用于订单支付、会员充值、钱包充值、课程购买、SaaS 套餐订阅等业务。只要系统中存在“业务单据需要通过第三方支付完成付款”的场景，就需要支付回调与对账补偿能力。

以课程购买为例，用户提交课程订单后，系统会创建一笔支付单，并调用第三方支付渠道生成支付参数。用户完成付款后，支付渠道会异步通知本系统。本系统收到回调后，需要验签、校验金额、更新支付单状态，并通知订单系统将订单变更为已支付。

但支付系统不能只依赖回调，因为第三方支付回调可能出现以下问题：

| 问题         | 示例                                       |
| ------------ | ------------------------------------------ |
| 重复回调     | 支付渠道连续多次通知同一笔支付成功         |
| 回调丢失     | 网络异常导致本系统没有收到支付成功通知     |
| 回调乱序     | 先收到关闭通知，后收到成功通知             |
| 回调伪造     | 非支付渠道请求回调接口                     |
| 金额异常     | 回调金额与平台支付单金额不一致             |
| 业务推进失败 | 支付单已成功，但订单状态没有更新           |
| 对账不一致   | 渠道账单有成功记录，但平台支付单仍是未支付 |

因此，本案例采用“回调优先 + 主动查询补偿 + 对账兜底”的处理方式。

### 核心流程

核心流程分为三条链路：支付创建链路、支付回调链路、补偿对账链路。

支付创建链路负责生成支付单并调用第三方支付渠道：

```text
用户提交业务订单
-> 订单系统请求创建支付单
-> 支付系统校验业务单和金额
-> 生成平台支付单号
-> 保存支付单，状态为待支付
-> 调用第三方支付渠道
-> 返回支付参数或支付链接
```

支付回调链路负责处理第三方异步通知：

```text
第三方支付渠道回调
-> 接收回调参数
-> 记录回调日志
-> 验证回调签名
-> 查询平台支付单
-> 校验支付金额
-> 判断支付状态是否允许变更
-> 幂等更新支付单状态
-> 发送支付成功 MQ 消息
-> 订单系统消费消息并更新订单状态
```

主动查询补偿链路负责处理回调丢失或回调失败的数据：

```text
XXL-JOB 定时扫描待支付支付单
-> 筛选超过一定时间仍未终态的支付单
-> 调用第三方支付查询接口
-> 根据渠道返回状态更新平台支付单
-> 支付成功则补发支付成功 MQ 消息
-> 支付关闭则更新支付单为已关闭
```

对账补偿链路负责在日终或定时任务中发现账单差异：

```text
下载或导入渠道账单
-> 读取平台支付单
-> 按渠道流水号或支付单号匹配
-> 校验支付状态
-> 校验支付金额
-> 识别平台缺失、渠道缺失、金额不一致、状态不一致
-> 保存对账差异
-> 对可自动修复的数据执行补偿
-> 对高风险差异进入人工处理
```

本案例中支付状态建议采用不可逆状态机：

```text
待支付
-> 支付成功

待支付
-> 支付失败

待支付
-> 已关闭

支付失败
-> 支付成功

已关闭
-> 支付成功
```

其中，`支付成功` 是最终成功状态，一旦进入成功状态，不允许被任何回调或补偿任务回退。`支付失败` 和 `已关闭` 在部分支付渠道中仍可能被后续成功通知覆盖，因此本案例允许它们被修正为 `支付成功`，但不允许 `支付成功` 回退。

### 技术选型

本案例使用 Spring Boot 3 作为基础框架，使用 MyBatis-Plus 操作 MySQL，使用 Redis 和 Redisson 实现幂等控制与分布式锁，使用 RabbitMQ 解耦支付成功后的订单状态推进，使用 XXL-JOB 实现主动查询和对账补偿任务。

| 技术                         | 用途           | 选型说明                                     |
| ---------------------------- | -------------- | -------------------------------------------- |
| Spring Boot 3                | 后端基础框架   | 提供 Web、事务、配置、定时任务集成能力       |
| MyBatis-Plus                 | 数据访问       | 简化支付单、回调日志、对账差异等表的 CRUD    |
| MySQL                        | 业务数据存储   | 保存支付单、回调日志、对账差异、本地消息     |
| Redis                        | 缓存与幂等标记 | 保存短期幂等 Key、任务执行标记               |
| Redisson                     | 分布式锁       | 控制同一支付单并发回调、补偿任务并发执行     |
| RabbitMQ                     | 异步消息       | 支付成功后通知订单系统，保证最终一致性       |
| XXL-JOB                      | 定时补偿       | 定时扫描未支付单、执行对账任务、重试异常数据 |
| BigDecimal                   | 金额处理       | 严格处理支付金额，避免浮点精度问题           |
| Hutool CryptoUtil / SignUtil | 签名验签       | 简化第三方请求签名和回调验签逻辑             |
| Lombok                       | 简化实体代码   | 减少 Getter、Setter、构造方法等样板代码      |
| Sa-Token                     | 接口鉴权       | 管理端查询支付单、触发补偿、查看差异时使用   |

核心依赖关系如下：

```text
Spring Boot 3
├── MyBatis-Plus
├── MySQL
├── Redis
├── Redisson
├── RabbitMQ
├── XXL-JOB
├── Hutool
├── Lombok
└── Sa-Token
```

本案例不强制引入 Seata。支付回调场景更推荐使用“本地事务 + 本地消息表 + MQ 最终一致性”的方案，因为支付单状态更新和消息记录可以放在同一个本地事务中完成，订单系统通过 MQ 异步消费支付成功事件即可。这样可以降低分布式事务复杂度，也更符合支付链路的高可用实践。

本案例后续实现会默认采用以下工程约定：

| 约定项   | 内容                                               |
| -------- | -------------------------------------------------- |
| 基础包名 | `io.github.atengk.payment`                         |
| 数据库   | MySQL 8.x                                          |
| ORM      | MyBatis-Plus                                       |
| 金额单位 | 数据库保存 `decimal(18,2)`，Java 使用 `BigDecimal` |
| 支付单号 | 使用业务前缀 + 时间 + 雪花 ID                      |
| 回调幂等 | 支付单号维度加 Redisson 锁，数据库状态二次校验     |
| MQ 消息  | 支付成功事件只在支付单首次成功时发送               |
| 定时补偿 | XXL-JOB 扫描非终态支付单并查询渠道状态             |
| 对账方式 | 先使用模拟渠道账单表，后续可替换为真实账单下载     |

## 项目结构

本案例按单体 Spring Boot 项目组织代码，但包结构保留支付服务独立拆分的边界。后续如果演进为微服务，可以直接将 `payment` 模块拆出为独立服务。该实现围绕 README 中“支付回调与对账补偿”的核心链路展开：创建支付单、接收回调、验签、金额校验、幂等更新、订单状态推进、主动查询补偿和对账差异处理。

### 模块分层

推荐项目结构如下：

```text
payment-demo
├── pom.xml
├── src/main/java/io/github/atengk/payment
│   ├── PaymentApplication.java
│   ├── common
│   │   ├── constant
│   │   │   ├── PaymentMqConstant.java
│   │   │   └── PaymentRedisKeyConstant.java
│   │   ├── enums
│   │   │   ├── PayCallbackResultEnum.java
│   │   │   ├── PayChannelEnum.java
│   │   │   ├── PayStatusEnum.java
│   │   │   └── ReconcileDiffTypeEnum.java
│   │   ├── exception
│   │   │   └── BizException.java
│   │   └── result
│   │       └── Result.java
│   ├── config
│   │   ├── RabbitMqConfig.java
│   │   └── RedissonConfig.java
│   ├── controller
│   │   ├── PayCallbackController.java
│   │   ├── PayOrderController.java
│   │   └── PayReconcileController.java
│   ├── domain
│   │   ├── dto
│   │   │   ├── PayCreateDTO.java
│   │   │   ├── PayNotifyDTO.java
│   │   │   └── ReconcileRequestDTO.java
│   │   ├── entity
│   │   │   ├── PayCallbackLog.java
│   │   │   ├── PayOrder.java
│   │   │   ├── PayReconcileDiff.java
│   │   │   └── PayLocalMessage.java
│   │   └── vo
│   │       ├── PayCreateVO.java
│   │       └── PayOrderVO.java
│   ├── mapper
│   │   ├── PayCallbackLogMapper.java
│   │   ├── PayOrderMapper.java
│   │   ├── PayReconcileDiffMapper.java
│   │   └── PayLocalMessageMapper.java
│   ├── mq
│   │   ├── consumer
│   │   │   └── PaySuccessConsumer.java
│   │   ├── event
│   │   │   └── PaySuccessEvent.java
│   │   └── producer
│   │       └── PayMessageProducer.java
│   ├── service
│   │   ├── PayCallbackService.java
│   │   ├── PayCompensateService.java
│   │   ├── PayOrderService.java
│   │   ├── PayReconcileService.java
│   │   └── impl
│   │       ├── PayCallbackServiceImpl.java
│   │       ├── PayCompensateServiceImpl.java
│   │       ├── PayOrderServiceImpl.java
│   │       └── PayReconcileServiceImpl.java
│   ├── third
│   │   ├── client
│   │   │   ├── MockPayClient.java
│   │   │   └── PayChannelClient.java
│   │   └── model
│   │       ├── PayChannelQueryResult.java
│   │       └── PayChannelRequest.java
│   └── job
│       ├── PayCompensateJobHandler.java
│       └── PayReconcileJobHandler.java
└── src/main/resources
    ├── application.yml
    └── mapper
        ├── PayCallbackLogMapper.xml
        ├── PayOrderMapper.xml
        ├── PayReconcileDiffMapper.xml
        └── PayLocalMessageMapper.xml
```

### 核心包说明

| 包路径            | 作用                                           |
| ----------------- | ---------------------------------------------- |
| `common.constant` | 保存 Redis Key、MQ 交换机、队列、路由键等常量  |
| `common.enums`    | 支付状态、支付渠道、回调结果、对账差异类型枚举 |
| `controller`      | 提供支付单创建、支付回调、对账触发和查询接口   |
| `domain.dto`      | 接收前端、订单系统、支付渠道传入的请求参数     |
| `domain.entity`   | 对应数据库表结构，配合 MyBatis-Plus 使用       |
| `domain.vo`       | 返回给前端或调用方的响应对象                   |
| `mapper`          | MyBatis-Plus 数据访问层                        |
| `mq.event`        | MQ 事件对象，例如支付成功事件                  |
| `mq.producer`     | 支付成功、本地消息补偿等消息发送逻辑           |
| `mq.consumer`     | 订单状态推进、消息消费幂等等处理逻辑           |
| `service`         | 支付创建、回调处理、补偿查询、对账处理核心业务 |
| `third.client`    | 第三方支付渠道适配层，本案例先使用模拟渠道     |
| `job`             | XXL-JOB 任务入口，用于支付状态补偿和对账补偿   |

这里的分层重点是把“支付渠道调用”和“支付业务状态处理”隔离开。后续接入支付宝、微信、银联或其他支付渠道时，只需要扩展 `third.client`，不要把渠道 SDK 代码写进回调 Service 里。

## 数据库设计

数据库设计围绕四张核心表展开：支付单表、支付回调日志表、对账差异表、本地消息表。其中支付单表是主表，其他三张表分别解决回调留痕、对账补偿和 MQ 最终一致性问题。

### 支付单表

支付单表用于保存平台侧支付记录。业务订单和支付单建议一对一，也可以根据业务扩展为一对多，例如订单重新支付时生成新的支付单。

```sql
-- 支付单表：保存平台侧支付记录，所有支付状态变更都以该表为准
CREATE TABLE pay_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    pay_no VARCHAR(64) NOT NULL COMMENT '平台支付单号',
    business_order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
    channel_code VARCHAR(32) NOT NULL COMMENT '支付渠道编码：MOCK、ALIPAY、WECHAT',
    channel_trade_no VARCHAR(128) DEFAULT NULL COMMENT '第三方渠道交易号',
    pay_amount DECIMAL(18,2) NOT NULL COMMENT '支付金额',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    pay_status VARCHAR(32) NOT NULL COMMENT '支付状态：WAITING、SUCCESS、FAILED、CLOSED',
    subject VARCHAR(128) DEFAULT NULL COMMENT '支付标题',
    description VARCHAR(512) DEFAULT NULL COMMENT '支付描述',
    expire_time DATETIME DEFAULT NULL COMMENT '支付过期时间',
    success_time DATETIME DEFAULT NULL COMMENT '支付成功时间',
    close_time DATETIME DEFAULT NULL COMMENT '支付关闭时间',
    notify_time DATETIME DEFAULT NULL COMMENT '最近一次回调时间',
    notify_count INT NOT NULL DEFAULT 0 COMMENT '回调次数',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_no (pay_no),
    UNIQUE KEY uk_business_order_no (business_order_no),
    KEY idx_channel_trade_no (channel_trade_no),
    KEY idx_pay_status_expire_time (pay_status, expire_time),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单表';
```

关键设计点：

| 字段                | 说明                                                   |
| ------------------- | ------------------------------------------------------ |
| `pay_no`            | 平台支付单号，系统内部强唯一                           |
| `business_order_no` | 业务订单号，本案例默认一个业务订单只创建一笔有效支付单 |
| `channel_trade_no`  | 第三方支付渠道流水号，支付成功后通常会返回             |
| `pay_amount`        | 支付金额，必须使用 `DECIMAL`，Java 中使用 `BigDecimal` |
| `pay_status`        | 支付状态，禁止成功状态回退                             |
| `notify_count`      | 回调次数，用于排查重复回调                             |
| `version`           | 乐观锁字段，防止并发状态覆盖                           |

支付单表负责表达平台支付状态，但不建议把完整回调报文直接塞进支付单表，否则表会变得臃肿，且不利于排查多次回调记录。

### 支付回调日志表

支付回调日志表用于保存第三方支付渠道每一次回调请求。即使验签失败、金额不一致或重复回调，也应该记录下来，方便排查问题。

```sql
-- 支付回调日志表：保存每一次第三方支付回调请求，便于审计和问题排查
CREATE TABLE pay_callback_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    pay_no VARCHAR(64) DEFAULT NULL COMMENT '平台支付单号',
    business_order_no VARCHAR(64) DEFAULT NULL COMMENT '业务订单号',
    channel_code VARCHAR(32) DEFAULT NULL COMMENT '支付渠道编码',
    channel_trade_no VARCHAR(128) DEFAULT NULL COMMENT '第三方渠道交易号',
    callback_status VARCHAR(32) NOT NULL COMMENT '回调处理结果：SUCCESS、DUPLICATE、SIGN_INVALID、AMOUNT_INVALID、ORDER_NOT_FOUND、FAILED',
    request_body TEXT NOT NULL COMMENT '回调原始报文',
    request_sign VARCHAR(512) DEFAULT NULL COMMENT '回调签名',
    error_msg VARCHAR(1024) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_pay_no (pay_no),
    KEY idx_channel_trade_no (channel_trade_no),
    KEY idx_callback_status (callback_status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调日志表';
```

关键设计点：

| 字段              | 说明                                                 |
| ----------------- | ---------------------------------------------------- |
| `request_body`    | 保存原始回调报文，排查签名、金额、状态问题时非常重要 |
| `request_sign`    | 保存渠道传入的签名                                   |
| `callback_status` | 标记本次回调处理结果，不等同于支付单状态             |
| `error_msg`       | 保存验签失败、金额不一致、状态异常等原因             |

这张表建议只追加，不更新。它是回调审计日志，不是业务状态表。

### 对账差异表

对账差异表用于保存平台支付单和渠道账单之间的差异。常见差异包括金额不一致、状态不一致、平台缺单、渠道缺单等。

```sql
-- 对账差异表：保存平台支付单和渠道账单之间的差异数据
CREATE TABLE pay_reconcile_diff (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    reconcile_batch_no VARCHAR(64) NOT NULL COMMENT '对账批次号',
    pay_no VARCHAR(64) DEFAULT NULL COMMENT '平台支付单号',
    business_order_no VARCHAR(64) DEFAULT NULL COMMENT '业务订单号',
    channel_code VARCHAR(32) NOT NULL COMMENT '支付渠道编码',
    channel_trade_no VARCHAR(128) DEFAULT NULL COMMENT '第三方渠道交易号',
    diff_type VARCHAR(32) NOT NULL COMMENT '差异类型：PLATFORM_MISSING、CHANNEL_MISSING、AMOUNT_NOT_MATCH、STATUS_NOT_MATCH',
    platform_amount DECIMAL(18,2) DEFAULT NULL COMMENT '平台支付金额',
    channel_amount DECIMAL(18,2) DEFAULT NULL COMMENT '渠道账单金额',
    platform_status VARCHAR(32) DEFAULT NULL COMMENT '平台支付状态',
    channel_status VARCHAR(32) DEFAULT NULL COMMENT '渠道支付状态',
    handle_status VARCHAR(32) NOT NULL DEFAULT 'WAITING' COMMENT '处理状态：WAITING、AUTO_FIXED、MANUAL_HANDLED、IGNORED',
    handle_remark VARCHAR(1024) DEFAULT NULL COMMENT '处理备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_channel_trade_no_diff (
        reconcile_batch_no,
        channel_code,
        channel_trade_no,
        diff_type
    ),
    KEY idx_pay_no (pay_no),
    KEY idx_reconcile_batch_no (reconcile_batch_no),
    KEY idx_diff_type (diff_type),
    KEY idx_handle_status (handle_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付对账差异表';
```

关键设计点：

| 字段                 | 说明                               |
| -------------------- | ---------------------------------- |
| `reconcile_batch_no` | 一次对账任务的批次号               |
| `diff_type`          | 差异类型，决定后续是否可以自动补偿 |
| `platform_amount`    | 平台侧金额                         |
| `channel_amount`     | 渠道账单金额                       |
| `handle_status`      | 差异处理状态                       |
| `handle_remark`      | 自动处理或人工处理说明             |

本案例中建议只有“平台未成功但渠道成功”的状态差异允许自动补偿。金额不一致、平台缺单、渠道缺单建议先进入人工处理。

### 本地消息表

本地消息表用于保证支付单状态更新和 MQ 消息发送的一致性。支付成功时，先在同一个数据库事务中更新支付单并写入本地消息表，再异步投递 MQ。

```sql
-- 本地消息表：保证本地事务和 MQ 投递的最终一致性
CREATE TABLE pay_local_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    message_no VARCHAR(64) NOT NULL COMMENT '消息编号',
    business_key VARCHAR(128) NOT NULL COMMENT '业务唯一键，例如支付单号',
    exchange_name VARCHAR(128) NOT NULL COMMENT '交换机名称',
    routing_key VARCHAR(128) NOT NULL COMMENT '路由键',
    message_body TEXT NOT NULL COMMENT '消息内容JSON',
    send_status VARCHAR(32) NOT NULL DEFAULT 'WAITING' COMMENT '发送状态：WAITING、SUCCESS、FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    error_msg VARCHAR(1024) DEFAULT NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_no (message_no),
    UNIQUE KEY uk_business_key_routing_key (business_key, routing_key),
    KEY idx_send_status_next_retry_time (send_status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付本地消息表';
```

关键设计点：

| 字段            | 说明                           |
| --------------- | ------------------------------ |
| `message_no`    | 消息唯一编号                   |
| `business_key`  | 业务唯一键，本案例使用支付单号 |
| `exchange_name` | RabbitMQ 交换机名称            |
| `routing_key`   | RabbitMQ 路由键                |
| `message_body`  | 消息 JSON 内容                 |
| `send_status`   | 消息投递状态                   |
| `retry_count`   | 补偿重试次数                   |

`uk_business_key_routing_key` 可以防止同一支付单重复生成同一种业务消息。例如支付成功消息只允许生成一次。

## 枚举与常量设计

枚举用于约束支付状态、渠道编码、回调处理结果和对账差异类型。不要在代码中散落字符串常量，否则后续状态机判断、对账规则和回调处理会很难维护。

### 支付状态枚举

支付状态枚举控制支付单的核心状态流转。这里提供 `canChangeTo` 方法，用来判断当前状态能否变更为目标状态。

文件位置：`src/main/java/io/github/atengk/payment/common/enums/PayStatusEnum.java`

```java
package io.github.atengk.payment.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 支付状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
public enum PayStatusEnum {

    WAITING("WAITING", "待支付"),
    SUCCESS("SUCCESS", "支付成功"),
    FAILED("FAILED", "支付失败"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String desc;

    PayStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取支付状态
     *
     * @param code 状态编码
     * @return 支付状态枚举
     */
    public static PayStatusEnum of(String code) {
        for (PayStatusEnum statusEnum : values()) {
            if (StrUtil.equals(statusEnum.getCode(), code)) {
                return statusEnum;
            }
        }
        return null;
    }

    /**
     * 判断是否为终态
     *
     * @param code 状态编码
     * @return 是否为终态
     */
    public static boolean isFinalStatus(String code) {
        return StrUtil.equals(SUCCESS.code, code) || StrUtil.equals(CLOSED.code, code);
    }

    /**
     * 判断当前状态能否变更为目标状态
     *
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否允许变更
     */
    public static boolean canChangeTo(String currentStatus, String targetStatus) {
        if (StrUtil.equals(currentStatus, targetStatus)) {
            return false;
        }

        // 支付成功是强终态，禁止回退
        if (StrUtil.equals(SUCCESS.code, currentStatus)) {
            return false;
        }

        // 待支付可以流转为成功、失败、关闭
        if (StrUtil.equals(WAITING.code, currentStatus)) {
            return StrUtil.equals(SUCCESS.code, targetStatus)
                    || StrUtil.equals(FAILED.code, targetStatus)
                    || StrUtil.equals(CLOSED.code, targetStatus);
        }

        // 某些渠道可能先返回失败或关闭，后续又返回成功，以成功结果为准
        if (StrUtil.equals(FAILED.code, currentStatus) || StrUtil.equals(CLOSED.code, currentStatus)) {
            return StrUtil.equals(SUCCESS.code, targetStatus);
        }

        return false;
    }
}
```

状态流转规则如下：

| 当前状态  | 目标状态  | 是否允许 | 说明                 |
| --------- | --------- | -------- | -------------------- |
| `WAITING` | `SUCCESS` | 允许     | 正常支付成功         |
| `WAITING` | `FAILED`  | 允许     | 支付失败             |
| `WAITING` | `CLOSED`  | 允许     | 超时关闭             |
| `FAILED`  | `SUCCESS` | 允许     | 渠道后续成功通知修正 |
| `CLOSED`  | `SUCCESS` | 允许     | 渠道后续成功通知修正 |
| `SUCCESS` | 任意状态  | 不允许   | 成功状态不可逆       |

### 支付渠道枚举

支付渠道枚举用于统一渠道编码。本案例先实现 `MOCK` 模拟渠道，后续可以扩展支付宝、微信支付等真实渠道。

文件位置：`src/main/java/io/github/atengk/payment/common/enums/PayChannelEnum.java`

```java
package io.github.atengk.payment.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 支付渠道枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
public enum PayChannelEnum {

    MOCK("MOCK", "模拟支付"),
    ALIPAY("ALIPAY", "支付宝"),
    WECHAT("WECHAT", "微信支付");

    private final String code;
    private final String desc;

    PayChannelEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取支付渠道
     *
     * @param code 渠道编码
     * @return 支付渠道枚举
     */
    public static PayChannelEnum of(String code) {
        for (PayChannelEnum channelEnum : values()) {
            if (StrUtil.equals(channelEnum.getCode(), code)) {
                return channelEnum;
            }
        }
        return null;
    }

    /**
     * 判断渠道是否支持
     *
     * @param code 渠道编码
     * @return 是否支持
     */
    public static boolean isSupported(String code) {
        return of(code) != null;
    }
}
```

渠道编码不要直接使用中文名称。数据库、MQ 消息、回调参数、对账文件中都应该使用稳定的渠道编码，例如 `MOCK`、`ALIPAY`、`WECHAT`。

### 回调处理结果枚举

回调处理结果枚举用于记录每一次回调请求的处理结果。它不是支付状态，而是本次回调请求的处理状态。

文件位置：`src/main/java/io/github/atengk/payment/common/enums/PayCallbackResultEnum.java`

```java
package io.github.atengk.payment.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 支付回调处理结果枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
public enum PayCallbackResultEnum {

    SUCCESS("SUCCESS", "处理成功"),
    DUPLICATE("DUPLICATE", "重复回调"),
    SIGN_INVALID("SIGN_INVALID", "签名无效"),
    AMOUNT_INVALID("AMOUNT_INVALID", "金额不一致"),
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "支付单不存在"),
    STATUS_INVALID("STATUS_INVALID", "状态不允许变更"),
    FAILED("FAILED", "处理失败");

    private final String code;
    private final String desc;

    PayCallbackResultEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取回调处理结果
     *
     * @param code 处理结果编码
     * @return 回调处理结果枚举
     */
    public static PayCallbackResultEnum of(String code) {
        for (PayCallbackResultEnum resultEnum : values()) {
            if (StrUtil.equals(resultEnum.getCode(), code)) {
                return resultEnum;
            }
        }
        return null;
    }
}
```

建议无论回调是否处理成功，都写入回调日志表。比如验签失败时，支付单不更新，但回调日志要记录 `SIGN_INVALID`。

### 对账差异类型枚举

对账差异类型用于描述平台支付单和渠道账单之间的差异原因。不同差异类型对应不同处理策略。

文件位置：`src/main/java/io/github/atengk/payment/common/enums/ReconcileDiffTypeEnum.java`

```java
package io.github.atengk.payment.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 对账差异类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
public enum ReconcileDiffTypeEnum {

    PLATFORM_MISSING("PLATFORM_MISSING", "平台缺单"),
    CHANNEL_MISSING("CHANNEL_MISSING", "渠道缺单"),
    AMOUNT_NOT_MATCH("AMOUNT_NOT_MATCH", "金额不一致"),
    STATUS_NOT_MATCH("STATUS_NOT_MATCH", "状态不一致");

    private final String code;
    private final String desc;

    ReconcileDiffTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取对账差异类型
     *
     * @param code 差异类型编码
     * @return 对账差异类型枚举
     */
    public static ReconcileDiffTypeEnum of(String code) {
        for (ReconcileDiffTypeEnum diffTypeEnum : values()) {
            if (StrUtil.equals(diffTypeEnum.getCode(), code)) {
                return diffTypeEnum;
            }
        }
        return null;
    }

    /**
     * 判断是否允许自动补偿
     *
     * @param code 差异类型编码
     * @return 是否允许自动补偿
     */
    public static boolean canAutoFix(String code) {
        // 本案例仅允许状态不一致进入自动补偿，金额不一致和缺单默认人工处理
        return StrUtil.equals(STATUS_NOT_MATCH.code, code);
    }
}
```

差异处理建议保持保守策略：

| 差异类型           | 说明                           | 建议处理方式       |
| ------------------ | ------------------------------ | ------------------ |
| `PLATFORM_MISSING` | 渠道有账单，平台没有支付单     | 人工处理           |
| `CHANNEL_MISSING`  | 平台有成功支付单，渠道账单没有 | 人工处理           |
| `AMOUNT_NOT_MATCH` | 平台金额和渠道金额不一致       | 人工处理           |
| `STATUS_NOT_MATCH` | 平台状态和渠道状态不一致       | 符合条件时自动补偿 |

### 常量设计

常量类主要维护 Redis Key 和 RabbitMQ 配置。Redis Key 用于支付回调加锁、补偿任务加锁；RabbitMQ 常量用于支付成功事件投递。

文件位置：`src/main/java/io/github/atengk/payment/common/constant/PaymentRedisKeyConstant.java`

```java
package io.github.atengk.payment.common.constant;

import cn.hutool.core.util.StrUtil;

/**
 * 支付 Redis Key 常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class PaymentRedisKeyConstant {

    public static final String PAY_CALLBACK_LOCK = "payment:callback:lock:{}";
    public static final String PAY_COMPENSATE_LOCK = "payment:compensate:lock";
    public static final String PAY_RECONCILE_LOCK = "payment:reconcile:lock:{}";

    private PaymentRedisKeyConstant() {
    }

    /**
     * 构建支付回调锁 Key
     *
     * @param payNo 支付单号
     * @return Redis Key
     */
    public static String callbackLockKey(String payNo) {
        return StrUtil.format(PAY_CALLBACK_LOCK, payNo);
    }

    /**
     * 构建对账任务锁 Key
     *
     * @param reconcileDate 对账日期
     * @return Redis Key
     */
    public static String reconcileLockKey(String reconcileDate) {
        return StrUtil.format(PAY_RECONCILE_LOCK, reconcileDate);
    }
}
```

文件位置：`src/main/java/io/github/atengk/payment/common/constant/PaymentMqConstant.java`

```java
package io.github.atengk.payment.common.constant;

/**
 * 支付 MQ 常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class PaymentMqConstant {

    public static final String PAY_EXCHANGE = "payment.exchange";

    public static final String PAY_SUCCESS_QUEUE = "payment.success.queue";

    public static final String PAY_SUCCESS_ROUTING_KEY = "payment.success";

    private PaymentMqConstant() {
    }
}
```

这部分完成后，支付模块的基础骨架已经确定。后续实现支付单创建、回调处理和对账补偿时，代码会直接复用这些表结构、枚举和常量。

## 支付单创建

支付单创建是支付链路的入口。它负责根据业务订单生成平台支付单，控制重复创建，校验金额精度，并调用第三方支付渠道获取支付参数。本案例中的支付单创建对应 README 中“创建支付单 -> 调用第三方支付渠道”的前半段流程。

本节先实现核心代码，默认前置条件如下：

| 前置项   | 说明                                                     |
| -------- | -------------------------------------------------------- |
| 支付单表 | 已创建 `pay_order` 表                                    |
| 唯一约束 | `business_order_no` 唯一，避免一个业务订单重复创建支付单 |
| ORM      | 使用 MyBatis-Plus                                        |
| 金额类型 | Java 使用 `BigDecimal`，数据库使用 `DECIMAL(18,2)`       |
| 支付渠道 | 先使用 `MOCK` 模拟渠道                                   |
| 基础包名 | `io.github.atengk.payment`                               |

### 创建支付单接口

创建支付单接口由订单系统或前端调用。接口接收业务订单号、支付金额、支付渠道、支付标题等参数，返回平台支付单号和模拟支付链接。

文件位置：`src/main/java/io/github/atengk/payment/controller/PayOrderController.java`

```java
package io.github.atengk.payment.controller;

import io.github.atengk.payment.common.result.Result;
import io.github.atengk.payment.domain.dto.PayCreateDTO;
import io.github.atengk.payment.domain.vo.PayCreateVO;
import io.github.atengk.payment.service.PayOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付单控制器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pay/orders")
public class PayOrderController {

    private final PayOrderService payOrderService;

    /**
     * 创建支付单
     *
     * @param dto 创建支付单参数
     * @return 支付单创建结果
     */
    @PostMapping
    public Result<PayCreateVO> createPayOrder(@Valid @RequestBody PayCreateDTO dto) {
        return Result.ok(payOrderService.createPayOrder(dto));
    }
}
```

创建支付单请求对象如下，核心字段是业务订单号、支付金额和支付渠道。

文件位置：`src/main/java/io/github/atengk/payment/domain/dto/PayCreateDTO.java`

```java
package io.github.atengk.payment.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付单请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class PayCreateDTO {

    @NotBlank(message = "业务订单号不能为空")
    private String businessOrderNo;

    @NotBlank(message = "支付渠道不能为空")
    private String channelCode;

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    private BigDecimal payAmount;

    @NotBlank(message = "支付标题不能为空")
    private String subject;

    private String description;

    /**
     * 支付过期分钟数，默认30分钟
     */
    private Integer expireMinutes;
}
```

创建支付单响应对象如下，返回给调用方用于拉起支付。

文件位置：`src/main/java/io/github/atengk/payment/domain/vo/PayCreateVO.java`

```java
package io.github.atengk.payment.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 创建支付单响应结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class PayCreateVO {

    private String payNo;

    private String businessOrderNo;

    private String channelCode;

    private BigDecimal payAmount;

    private String payStatus;

    private String payUrl;

    private Map<String, Object> payParams;

    private LocalDateTime expireTime;
}
```

Service 接口只暴露创建支付单方法，后续回调、补偿和对账逻辑不要混到这里。

文件位置：`src/main/java/io/github/atengk/payment/service/PayOrderService.java`

```java
package io.github.atengk.payment.service;

import io.github.atengk.payment.domain.dto.PayCreateDTO;
import io.github.atengk.payment.domain.vo.PayCreateVO;

/**
 * 支付单服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayOrderService {

    /**
     * 创建支付单
     *
     * @param dto 创建支付单参数
     * @return 创建支付单结果
     */
    PayCreateVO createPayOrder(PayCreateDTO dto);
}
```

### 支付单号生成

支付单号需要满足全局唯一、趋势递增、便于排查。这里使用业务前缀 + 日期 + Hutool 雪花 ID 的方式生成。

文件位置：`src/main/java/io/github/atengk/payment/common/util/PayNoGenerator.java`

```java
package io.github.atengk.payment.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;

/**
 * 支付单号生成器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class PayNoGenerator {

    private static final String PAY_NO_PREFIX = "PAY";

    private PayNoGenerator() {
    }

    /**
     * 生成支付单号
     *
     * @return 支付单号
     */
    public static String generatePayNo() {
        String datePart = DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss");
        String snowflakeId = IdUtil.getSnowflakeNextIdStr();
        return PAY_NO_PREFIX + datePart + snowflakeId;
    }
}
```

生成结果示例：

```text
PAY202605151030321831202844829540352
```

该方案适合大多数业务项目。若后续需要更短的支付单号，可以改成 Redis 自增序列或号段服务，但支付场景不建议使用随机短码作为主支付单号。

### 金额精度处理

支付金额必须严格处理，不能使用 `double` 或 `float`。创建支付单时统一将金额规范化为两位小数，如果传入金额超过两位小数，直接拒绝。

文件位置：`src/main/java/io/github/atengk/payment/common/util/PayAmountUtil.java`

```java
package io.github.atengk.payment.common.util;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.payment.common.exception.BizException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 支付金额工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class PayAmountUtil {

    private PayAmountUtil() {
    }

    /**
     * 规范化支付金额
     *
     * @param amount 原始金额
     * @return 两位小数金额
     */
    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            throw new BizException("支付金额不能为空");
        }
        if (NumberUtil.isLessOrEqual(amount, BigDecimal.ZERO)) {
            throw new BizException("支付金额必须大于0");
        }

        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new BizException("支付金额最多只能保留两位小数");
        }
    }

    /**
     * 判断两个金额是否一致
     *
     * @param source 源金额
     * @param target 目标金额
     * @return 是否一致
     */
    public static boolean equals(BigDecimal source, BigDecimal target) {
        if (source == null || target == null) {
            return false;
        }
        return source.compareTo(target) == 0;
    }
}
```

金额处理规则如下：

| 输入金额 | 处理结果     |
| -------- | ------------ |
| `99`     | 转为 `99.00` |
| `99.9`   | 转为 `99.90` |
| `99.99`  | 正常保存     |
| `99.999` | 拒绝         |
| `0`      | 拒绝         |
| `-1`     | 拒绝         |

### 重复创建控制

重复创建控制需要两层保障：业务层先查一次，数据库层使用唯一索引兜底。这样即使并发请求同时进入，也不会产生两笔有效支付单。

核心策略如下：

| 控制点   | 说明                                        |
| -------- | ------------------------------------------- |
| 业务查询 | 根据 `business_order_no` 查询是否已有支付单 |
| 唯一索引 | `uk_business_order_no` 防止并发插入重复数据 |
| 幂等返回 | 已存在待支付支付单时，返回原支付单          |
| 状态判断 | 已支付成功的订单不再创建新支付单            |
| 异常兜底 | 捕获唯一索引冲突后重新查询原支付单          |

下面是支付单创建核心实现，包含金额校验、重复创建控制、支付单号生成和第三方支付调用。

文件位置：`src/main/java/io/github/atengk/payment/service/impl/PayOrderServiceImpl.java`

```java
package io.github.atengk.payment.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.payment.common.enums.PayChannelEnum;
import io.github.atengk.payment.common.enums.PayStatusEnum;
import io.github.atengk.payment.common.exception.BizException;
import io.github.atengk.payment.common.util.PayAmountUtil;
import io.github.atengk.payment.common.util.PayNoGenerator;
import io.github.atengk.payment.domain.dto.PayCreateDTO;
import io.github.atengk.payment.domain.entity.PayOrder;
import io.github.atengk.payment.domain.vo.PayCreateVO;
import io.github.atengk.payment.mapper.PayOrderMapper;
import io.github.atengk.payment.service.PayOrderService;
import io.github.atengk.payment.third.client.PayChannelClient;
import io.github.atengk.payment.third.model.PayChannelRequest;
import io.github.atengk.payment.third.model.PayChannelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付单服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderServiceImpl implements PayOrderService {

    private final PayOrderMapper payOrderMapper;
    private final List<PayChannelClient> payChannelClients;

    /**
     * 创建支付单
     *
     * @param dto 创建支付单参数
     * @return 创建支付单结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayCreateVO createPayOrder(PayCreateDTO dto) {
        BigDecimal payAmount = PayAmountUtil.normalize(dto.getPayAmount());
        PayChannelEnum channelEnum = checkPayChannel(dto.getChannelCode());

        PayOrder existPayOrder = getByBusinessOrderNo(dto.getBusinessOrderNo());
        if (ObjectUtil.isNotNull(existPayOrder)) {
            log.info("业务订单已存在支付单，直接返回原支付单，businessOrderNo={}，payNo={}，payStatus={}",
                    dto.getBusinessOrderNo(), existPayOrder.getPayNo(), existPayOrder.getPayStatus());
            return handleExistingPayOrder(existPayOrder);
        }

        String payNo = PayNoGenerator.generatePayNo();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(
                ObjectUtil.defaultIfNull(dto.getExpireMinutes(), 30)
        );

        PayOrder payOrder = new PayOrder();
        payOrder.setPayNo(payNo);
        payOrder.setBusinessOrderNo(dto.getBusinessOrderNo());
        payOrder.setChannelCode(channelEnum.getCode());
        payOrder.setPayAmount(payAmount);
        payOrder.setCurrency("CNY");
        payOrder.setPayStatus(PayStatusEnum.WAITING.getCode());
        payOrder.setSubject(dto.getSubject());
        payOrder.setDescription(dto.getDescription());
        payOrder.setExpireTime(expireTime);
        payOrder.setNotifyCount(0);
        payOrder.setVersion(0);

        try {
            payOrderMapper.insert(payOrder);
        } catch (DuplicateKeyException e) {
            log.warn("并发创建支付单命中唯一索引，重新查询原支付单，businessOrderNo={}", dto.getBusinessOrderNo());
            PayOrder duplicatePayOrder = getByBusinessOrderNo(dto.getBusinessOrderNo());
            if (ObjectUtil.isNotNull(duplicatePayOrder)) {
                return handleExistingPayOrder(duplicatePayOrder);
            }
            throw e;
        }

        PayChannelResponse channelResponse = invokePayChannel(payOrder);
        log.info("支付单创建成功，businessOrderNo={}，payNo={}，channelCode={}，payAmount={}",
                payOrder.getBusinessOrderNo(), payOrder.getPayNo(), payOrder.getChannelCode(), payOrder.getPayAmount());

        return buildCreateVO(payOrder, channelResponse);
    }

    /**
     * 校验支付渠道
     *
     * @param channelCode 支付渠道编码
     * @return 支付渠道枚举
     */
    private PayChannelEnum checkPayChannel(String channelCode) {
        PayChannelEnum channelEnum = PayChannelEnum.of(channelCode);
        if (ObjectUtil.isNull(channelEnum)) {
            throw new BizException("不支持的支付渠道：" + channelCode);
        }
        return channelEnum;
    }

    /**
     * 根据业务订单号查询支付单
     *
     * @param businessOrderNo 业务订单号
     * @return 支付单
     */
    private PayOrder getByBusinessOrderNo(String businessOrderNo) {
        return payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getBusinessOrderNo, businessOrderNo)
                .last("LIMIT 1"));
    }

    /**
     * 处理已存在支付单
     *
     * @param payOrder 支付单
     * @return 创建支付单结果
     */
    private PayCreateVO handleExistingPayOrder(PayOrder payOrder) {
        if (PayStatusEnum.SUCCESS.getCode().equals(payOrder.getPayStatus())) {
            throw new BizException("业务订单已支付成功，不能重复创建支付单");
        }

        if (PayStatusEnum.CLOSED.getCode().equals(payOrder.getPayStatus())) {
            throw new BizException("支付单已关闭，请重新创建业务订单或走重新支付流程");
        }

        PayChannelResponse channelResponse = invokePayChannel(payOrder);
        return buildCreateVO(payOrder, channelResponse);
    }

    /**
     * 调用支付渠道
     *
     * @param payOrder 支付单
     * @return 渠道响应
     */
    private PayChannelResponse invokePayChannel(PayOrder payOrder) {
        PayChannelClient client = payChannelClients.stream()
                .filter(item -> item.supports(payOrder.getChannelCode()))
                .findFirst()
                .orElseThrow(() -> new BizException("未找到支付渠道客户端：" + payOrder.getChannelCode()));

        PayChannelRequest request = PayChannelRequest.builder()
                .payNo(payOrder.getPayNo())
                .businessOrderNo(payOrder.getBusinessOrderNo())
                .channelCode(payOrder.getChannelCode())
                .payAmount(payOrder.getPayAmount())
                .currency(payOrder.getCurrency())
                .subject(payOrder.getSubject())
                .description(payOrder.getDescription())
                .expireTime(payOrder.getExpireTime())
                .build();

        return client.createPay(request);
    }

    /**
     * 构建支付单创建响应
     *
     * @param payOrder 支付单
     * @param channelResponse 渠道响应
     * @return 支付单创建响应
     */
    private PayCreateVO buildCreateVO(PayOrder payOrder, PayChannelResponse channelResponse) {
        return PayCreateVO.builder()
                .payNo(payOrder.getPayNo())
                .businessOrderNo(payOrder.getBusinessOrderNo())
                .channelCode(payOrder.getChannelCode())
                .payAmount(payOrder.getPayAmount())
                .payStatus(payOrder.getPayStatus())
                .payUrl(channelResponse.getPayUrl())
                .payParams(CollUtil.emptyIfNull(channelResponse.getPayParams()))
                .expireTime(payOrder.getExpireTime())
                .build();
    }
}
```

这里的重复创建并不是简单返回“请求重复”，而是返回原支付单信息。支付接口通常需要具备这种业务幂等能力，否则前端刷新、App 重试、订单系统超时重试都可能造成重复支付单。

## 第三方支付调用

第三方支付调用建议通过适配层隔离，不要在 `PayOrderServiceImpl` 中直接写支付宝、微信支付或其他渠道 SDK 的代码。本案例先实现一个 `MOCK` 支付渠道，后续接入真实渠道时只需要新增对应的 `PayChannelClient` 实现类。

### 支付请求参数组装

支付渠道请求模型用于承载平台支付单转换后的渠道请求参数。

文件位置：`src/main/java/io/github/atengk/payment/third/model/PayChannelRequest.java`

```java
package io.github.atengk.payment.third.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付渠道请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class PayChannelRequest {

    private String payNo;

    private String businessOrderNo;

    private String channelCode;

    private BigDecimal payAmount;

    private String currency;

    private String subject;

    private String description;

    private LocalDateTime expireTime;
}
```

支付渠道响应模型用于统一不同支付渠道的返回结果。

文件位置：`src/main/java/io/github/atengk/payment/third/model/PayChannelResponse.java`

```java
package io.github.atengk.payment.third.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 支付渠道响应结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class PayChannelResponse {

    private String channelCode;

    private String channelRequestNo;

    private String payUrl;

    private Map<String, Object> payParams;

    private String rawResponse;
}
```

支付渠道客户端接口如下。后续支付宝、微信支付、银联支付都实现这个接口。

文件位置：`src/main/java/io/github/atengk/payment/third/client/PayChannelClient.java`

```java
package io.github.atengk.payment.third.client;

import io.github.atengk.payment.third.model.PayChannelRequest;
import io.github.atengk.payment.third.model.PayChannelResponse;

/**
 * 支付渠道客户端
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayChannelClient {

    /**
     * 判断是否支持当前渠道
     *
     * @param channelCode 渠道编码
     * @return 是否支持
     */
    boolean supports(String channelCode);

    /**
     * 创建支付
     *
     * @param request 支付渠道请求
     * @return 支付渠道响应
     */
    PayChannelResponse createPay(PayChannelRequest request);
}
```

### 请求签名生成

签名生成用于模拟真实第三方支付接口的请求签名逻辑。本案例使用 Hutool 的 `SecureUtil.hmacSha256` 对参数进行 HMAC-SHA256 签名。

签名规则如下：

```text
1. 移除 sign 字段
2. 移除 null 值字段
3. 按参数名升序排序
4. 拼接为 key=value&key=value
5. 使用商户密钥做 HMAC-SHA256
```

文件位置：`src/main/java/io/github/atengk/payment/common/util/PaySignUtil.java`

```java
package io.github.atengk.payment.common.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 支付签名工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class PaySignUtil {

    private static final String SIGN_FIELD = "sign";

    private PaySignUtil() {
    }

    /**
     * 生成 HMAC-SHA256 签名
     *
     * @param params 请求参数
     * @param secret 签名密钥
     * @return 签名字符串
     */
    public static String sign(Map<String, Object> params, String secret) {
        String content = buildSignContent(params);
        return SecureUtil.hmacSha256(secret.getBytes(StandardCharsets.UTF_8)).digestHex(content);
    }

    /**
     * 校验签名
     *
     * @param params 请求参数
     * @param secret 签名密钥
     * @param sign 待校验签名
     * @return 是否通过
     */
    public static boolean verify(Map<String, Object> params, String secret, String sign) {
        if (StrUtil.isBlank(sign)) {
            return false;
        }
        String localSign = sign(params, secret);
        return StrUtil.equalsIgnoreCase(localSign, sign);
    }

    /**
     * 构建签名原文
     *
     * @param params 请求参数
     * @return 签名原文
     */
    private static String buildSignContent(Map<String, Object> params) {
        if (MapUtil.isEmpty(params)) {
            return StrUtil.EMPTY;
        }

        TreeMap<String, Object> sortedMap = new TreeMap<>(params);
        return sortedMap.entrySet()
                .stream()
                .filter(entry -> !StrUtil.equals(entry.getKey(), SIGN_FIELD))
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> StrUtil.isNotBlank(String.valueOf(entry.getValue())))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }
}
```

生成签名示例：

```text
businessOrderNo=ORDER202605150001&currency=CNY&payAmount=99.00&payNo=PAY202605151030321831202844829540352&subject=Java课程购买
```

### 支付渠道响应处理

模拟支付渠道会根据平台支付单生成支付链接、支付参数和渠道请求流水号。真实场景中，这里可以替换成支付宝、微信支付 SDK 的统一下单接口。

文件位置：`src/main/java/io/github/atengk/payment/third/client/MockPayClient.java`

```java
package io.github.atengk.payment.third.client;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.payment.common.enums.PayChannelEnum;
import io.github.atengk.payment.common.exception.BizException;
import io.github.atengk.payment.common.util.PaySignUtil;
import io.github.atengk.payment.third.model.PayChannelRequest;
import io.github.atengk.payment.third.model.PayChannelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模拟支付渠道客户端
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class MockPayClient implements PayChannelClient {

    private static final String MOCK_PAY_GATEWAY = "https://mock-pay.example.com/pay";

    /**
     * 示例密钥，真实项目应放在配置中心或密钥管理系统
     */
    private static final String MOCK_MERCHANT_SECRET = "mock-pay-secret-2026";

    /**
     * 判断是否支持当前渠道
     *
     * @param channelCode 渠道编码
     * @return 是否支持
     */
    @Override
    public boolean supports(String channelCode) {
        return StrUtil.equals(PayChannelEnum.MOCK.getCode(), channelCode);
    }

    /**
     * 创建模拟支付
     *
     * @param request 支付渠道请求
     * @return 支付渠道响应
     */
    @Override
    public PayChannelResponse createPay(PayChannelRequest request) {
        checkRequest(request);

        Map<String, Object> payParams = new LinkedHashMap<>();
        payParams.put("payNo", request.getPayNo());
        payParams.put("businessOrderNo", request.getBusinessOrderNo());
        payParams.put("payAmount", request.getPayAmount().toPlainString());
        payParams.put("currency", request.getCurrency());
        payParams.put("subject", request.getSubject());
        payParams.put("expireTime", DateUtil.formatLocalDateTime(request.getExpireTime()));
        payParams.put("nonce", IdUtil.fastSimpleUUID());

        String sign = PaySignUtil.sign(payParams, MOCK_MERCHANT_SECRET);
        payParams.put("sign", sign);

        String payUrl = MOCK_PAY_GATEWAY + "?payNo=" + request.getPayNo();
        String rawResponse = JSONUtil.toJsonStr(MapUtil.builder()
                .put("code", "SUCCESS")
                .put("message", "模拟支付创建成功")
                .put("payUrl", payUrl)
                .put("payParams", payParams)
                .build());

        log.info("模拟支付渠道创建支付成功，payNo={}，businessOrderNo={}，payAmount={}",
                request.getPayNo(), request.getBusinessOrderNo(), request.getPayAmount());

        return PayChannelResponse.builder()
                .channelCode(PayChannelEnum.MOCK.getCode())
                .channelRequestNo("MOCK_REQ_" + IdUtil.getSnowflakeNextIdStr())
                .payUrl(payUrl)
                .payParams(payParams)
                .rawResponse(rawResponse)
                .build();
    }

    /**
     * 校验支付渠道请求
     *
     * @param request 支付渠道请求
     */
    private void checkRequest(PayChannelRequest request) {
        if (request == null) {
            throw new BizException("支付渠道请求不能为空");
        }
        if (StrUtil.isBlank(request.getPayNo())) {
            throw new BizException("支付单号不能为空");
        }
        if (StrUtil.isBlank(request.getBusinessOrderNo())) {
            throw new BizException("业务订单号不能为空");
        }
        if (request.getPayAmount() == null) {
            throw new BizException("支付金额不能为空");
        }
    }
}
```

该模拟渠道返回的 `payParams` 后续也可以用于模拟支付回调。回调处理时，系统会使用相同的签名规则验证回调参数是否合法。

### 支付请求日志记录

支付请求日志至少要记录平台支付单号、业务订单号、渠道编码、支付金额、渠道响应结果和异常原因。这里先使用应用日志记录，后续如果需要更完整的审计能力，可以新增 `pay_request_log` 表。

在当前实现中，日志记录点分布在两个位置：

| 位置                                 | 记录内容                                   |
| ------------------------------------ | ------------------------------------------ |
| `PayOrderServiceImpl#createPayOrder` | 支付单创建成功、重复创建、并发唯一索引冲突 |
| `MockPayClient#createPay`            | 渠道请求参数、渠道创建结果                 |

核心日志示例：

```text
业务订单已存在支付单，直接返回原支付单，businessOrderNo=ORDER202605150001，payNo=PAY202605151030321831202844829540352，payStatus=WAITING

支付单创建成功，businessOrderNo=ORDER202605150001，payNo=PAY202605151030321831202844829540352，channelCode=MOCK，payAmount=99.00

模拟支付渠道创建支付成功，payNo=PAY202605151030321831202844829540352，businessOrderNo=ORDER202605150001，payAmount=99.00
```

接口调用示例：

```bash
curl -X POST 'http://localhost:8080/api/pay/orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "businessOrderNo": "ORDER202605150001",
    "channelCode": "MOCK",
    "payAmount": 99.90,
    "subject": "Java 后端课程购买",
    "description": "支付回调与对账补偿专项案例",
    "expireMinutes": 30
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "payNo": "PAY202605151030321831202844829540352",
    "businessOrderNo": "ORDER202605150001",
    "channelCode": "MOCK",
    "payAmount": 99.90,
    "payStatus": "WAITING",
    "payUrl": "https://mock-pay.example.com/pay?payNo=PAY202605151030321831202844829540352",
    "payParams": {
      "payNo": "PAY202605151030321831202844829540352",
      "businessOrderNo": "ORDER202605150001",
      "payAmount": "99.90",
      "currency": "CNY",
      "subject": "Java 后端课程购买",
      "expireTime": "2026-05-15 11:00:32",
      "nonce": "f8d5a7d7a9de49b9a870f5aa6cc6b9f3",
      "sign": "24ec9d9af3e9eaa4c7f2f6a41b6d9c19e4f8d7cbd0e4b1f6c19efdd3d2e1f221"
    },
    "expireTime": "2026-05-15T11:00:32"
  }
}
```

验证重复创建：

```bash
curl -X POST 'http://localhost:8080/api/pay/orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "businessOrderNo": "ORDER202605150001",
    "channelCode": "MOCK",
    "payAmount": 99.90,
    "subject": "Java 后端课程购买",
    "description": "重复创建测试",
    "expireMinutes": 30
  }'
```

再次请求时，不会插入新的 `pay_order` 记录，而是返回原支付单。可以通过 SQL 验证：

```sql
-- 验证同一个业务订单号只生成一条支付单
SELECT id, pay_no, business_order_no, pay_amount, pay_status, create_time
FROM pay_order
WHERE business_order_no = 'ORDER202605150001';
```

本节完成后，支付创建链路已经具备了创建支付单、支付单号生成、金额精度控制、重复创建防护和模拟第三方支付调用能力。下一步可以继续实现支付回调处理，包括回调参数接收、验签、金额校验、状态不可逆更新和回调日志落库。

## 支付回调处理

支付回调处理是支付链路中最关键的一段。它需要解决重复回调、回调验签、金额校验、状态不可逆、支付单和业务订单一致性等问题，这些也是该业务场景的核心难点。

本案例采用以下处理策略：

| 处理点   | 策略                                             |
| -------- | ------------------------------------------------ |
| 回调验签 | 使用 Hutool 生成本地签名并比对渠道签名           |
| 金额校验 | 使用 `BigDecimal.compareTo` 判断金额是否一致     |
| 幂等控制 | 使用 Redisson 按支付单号加锁，数据库状态二次校验 |
| 状态更新 | 使用状态机限制支付成功不可回退                   |
| 消息发送 | 支付单首次变更为成功时，写入本地消息并投递 MQ    |
| 回调日志 | 每次回调都落库，便于排查问题                     |

### 回调接口设计

支付回调接口通常由第三方支付渠道调用，不建议要求登录态，也不应该走普通用户鉴权。接口需要重点依赖签名验签来识别请求是否合法。

文件位置：`src/main/java/io/github/atengk/payment/controller/PayCallbackController.java`

```java
package io.github.atengk.payment.controller;

import io.github.atengk.payment.service.PayCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付回调控制器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pay/callback")
public class PayCallbackController {

    private final PayCallbackService payCallbackService;

    /**
     * 接收支付渠道回调
     *
     * @param channelCode 支付渠道编码
     * @param params 回调参数
     * @return 渠道响应
     */
    @PostMapping("/{channelCode}/notify")
    public String notify(@PathVariable String channelCode, @RequestBody Map<String, Object> params) {
        try {
            params.put("channelCode", channelCode);
            boolean success = payCallbackService.handleCallback(params);
            return success ? "SUCCESS" : "FAIL";
        } catch (Exception e) {
            log.error("支付回调处理异常，channelCode={}，params={}", channelCode, params, e);
            return "FAIL";
        }
    }
}
```

接口路径示例：

```text
POST /api/pay/callback/MOCK/notify
```

模拟渠道回调参数示例：

```json
{
  "payNo": "PAY202605151030321831202844829540352",
  "businessOrderNo": "ORDER202605150001",
  "channelTradeNo": "MOCK_TRADE_1831202888888888888",
  "payAmount": "99.90",
  "payStatus": "SUCCESS",
  "notifyTime": "2026-05-15 11:05:30",
  "nonce": "a8d7c9e1b2f34e9a80c1",
  "sign": "24ec9d9af3e9eaa4c7f2f6a41b6d9c19e4f8d7cbd0e4b1f6c19efdd3d2e1f221"
}
```

### 回调参数接收

回调参数不建议直接绑定复杂对象，因为不同支付渠道字段不完全一致。更通用的方式是先用 `Map<String, Object>` 接收原始参数，再转换为内部统一 DTO。

文件位置：`src/main/java/io/github/atengk/payment/domain/dto/PayNotifyDTO.java`

```java
package io.github.atengk.payment.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付回调通知参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class PayNotifyDTO {

    private String payNo;

    private String businessOrderNo;

    private String channelCode;

    private String channelTradeNo;

    private BigDecimal payAmount;

    private String payStatus;

    private String notifyTime;

    private String nonce;

    private String sign;
}
```

文件位置：`src/main/java/io/github/atengk/payment/service/PayCallbackService.java`

```java
package io.github.atengk.payment.service;

import java.util.Map;

/**
 * 支付回调服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayCallbackService {

    /**
     * 处理支付回调
     *
     * @param params 回调参数
     * @return 是否处理成功
     */
    boolean handleCallback(Map<String, Object> params);
}
```

### Hutool 验签实现

验签逻辑复用前面定义的 `PaySignUtil`。实际项目中，渠道密钥应来自配置中心、数据库或密钥管理系统。本案例为了简化实现，先用一个 `SecretProvider` 模拟。

文件位置：`src/main/java/io/github/atengk/payment/third/client/PayChannelSecretProvider.java`

```java
package io.github.atengk.payment.third.client;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.payment.common.enums.PayChannelEnum;
import io.github.atengk.payment.common.exception.BizException;
import org.springframework.stereotype.Component;

/**
 * 支付渠道密钥提供器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
public class PayChannelSecretProvider {

    /**
     * 获取支付渠道密钥
     *
     * @param channelCode 支付渠道编码
     * @return 渠道密钥
     */
    public String getSecret(String channelCode) {
        if (StrUtil.equals(PayChannelEnum.MOCK.getCode(), channelCode)) {
            return "mock-pay-secret-2026";
        }
        throw new BizException("未配置支付渠道密钥：" + channelCode);
    }
}
```

验签规则保持和请求签名一致：移除 `sign` 字段，移除空值字段，按参数名排序，拼接后使用 HMAC-SHA256 生成签名。

### 支付金额校验

支付金额必须以平台支付单金额为准。回调金额和平台金额不一致时，不能更新支付状态，也不能推进业务订单。

金额判断继续使用前面定义的 `PayAmountUtil.equals`：

```java
PayAmountUtil.equals(payOrder.getPayAmount(), notifyDTO.getPayAmount())
```

这里不能使用 `equals` 直接比较 `BigDecimal`，因为 `99.90` 和 `99.9` 的数值相等，但 `BigDecimal.equals` 会比较精度。支付金额校验应该使用 `compareTo`。

### 回调幂等控制

回调幂等使用两层控制：

| 层级              | 作用                                           |
| ----------------- | ---------------------------------------------- |
| Redisson 分布式锁 | 防止同一支付单并发回调同时处理                 |
| 数据库状态判断    | 防止锁失效、服务重启、消息重放等情况下重复推进 |

核心处理类如下。

文件位置：`src/main/java/io/github/atengk/payment/service/impl/PayCallbackServiceImpl.java`

```java
package io.github.atengk.payment.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.payment.common.constant.PaymentRedisKeyConstant;
import io.github.atengk.payment.common.enums.PayCallbackResultEnum;
import io.github.atengk.payment.common.enums.PayStatusEnum;
import io.github.atengk.payment.common.exception.BizException;
import io.github.atengk.payment.common.util.PayAmountUtil;
import io.github.atengk.payment.common.util.PaySignUtil;
import io.github.atengk.payment.domain.dto.PayNotifyDTO;
import io.github.atengk.payment.domain.entity.PayCallbackLog;
import io.github.atengk.payment.domain.entity.PayOrder;
import io.github.atengk.payment.mapper.PayCallbackLogMapper;
import io.github.atengk.payment.mapper.PayOrderMapper;
import io.github.atengk.payment.mq.event.PaySuccessEvent;
import io.github.atengk.payment.mq.producer.PayMessageProducer;
import io.github.atengk.payment.service.PayCallbackService;
import io.github.atengk.payment.third.client.PayChannelSecretProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付回调服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayCallbackServiceImpl implements PayCallbackService {

    private final PayOrderMapper payOrderMapper;
    private final PayCallbackLogMapper payCallbackLogMapper;
    private final RedissonClient redissonClient;
    private final PayMessageProducer payMessageProducer;
    private final PayChannelSecretProvider payChannelSecretProvider;

    /**
     * 处理支付回调
     *
     * @param params 回调参数
     * @return 是否处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleCallback(Map<String, Object> params) {
        PayNotifyDTO notifyDTO = convertNotifyDTO(params);

        if (!verifySign(params, notifyDTO)) {
            saveCallbackLog(notifyDTO, params, PayCallbackResultEnum.SIGN_INVALID, "支付回调验签失败");
            log.warn("支付回调验签失败，payNo={}，channelCode={}", notifyDTO.getPayNo(), notifyDTO.getChannelCode());
            return false;
        }

        String lockKey = PaymentRedisKeyConstant.callbackLockKey(notifyDTO.getPayNo());
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                saveCallbackLog(notifyDTO, params, PayCallbackResultEnum.DUPLICATE, "支付回调处理中，请勿重复处理");
                log.warn("支付回调获取锁失败，按重复回调处理，payNo={}", notifyDTO.getPayNo());
                return true;
            }

            return doHandleCallback(notifyDTO, params);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("支付回调获取锁被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行支付回调核心处理
     *
     * @param notifyDTO 回调参数
     * @param rawParams 原始参数
     * @return 是否处理成功
     */
    private boolean doHandleCallback(PayNotifyDTO notifyDTO, Map<String, Object> rawParams) {
        PayOrder payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getPayNo, notifyDTO.getPayNo())
                .last("LIMIT 1"));

        if (payOrder == null) {
            saveCallbackLog(notifyDTO, rawParams, PayCallbackResultEnum.ORDER_NOT_FOUND, "支付单不存在");
            log.warn("支付回调支付单不存在，payNo={}", notifyDTO.getPayNo());
            return false;
        }

        if (!PayAmountUtil.equals(payOrder.getPayAmount(), notifyDTO.getPayAmount())) {
            saveCallbackLog(notifyDTO, rawParams, PayCallbackResultEnum.AMOUNT_INVALID, "支付金额不一致");
            log.warn("支付回调金额不一致，payNo={}，platformAmount={}，notifyAmount={}",
                    notifyDTO.getPayNo(), payOrder.getPayAmount(), notifyDTO.getPayAmount());
            return false;
        }

        String targetStatus = convertChannelStatus(notifyDTO.getPayStatus());

        if (StrUtil.equals(payOrder.getPayStatus(), targetStatus)) {
            saveCallbackLog(notifyDTO, rawParams, PayCallbackResultEnum.DUPLICATE, "支付状态已处理");
            log.info("支付回调重复通知，payNo={}，payStatus={}", notifyDTO.getPayNo(), targetStatus);
            return true;
        }

        if (!PayStatusEnum.canChangeTo(payOrder.getPayStatus(), targetStatus)) {
            saveCallbackLog(notifyDTO, rawParams, PayCallbackResultEnum.STATUS_INVALID, "支付状态不允许变更");
            log.warn("支付回调状态不允许变更，payNo={}，currentStatus={}，targetStatus={}",
                    notifyDTO.getPayNo(), payOrder.getPayStatus(), targetStatus);
            return true;
        }

        int updateRows = updatePayOrderStatus(payOrder, notifyDTO, targetStatus);
        if (updateRows <= 0) {
            saveCallbackLog(notifyDTO, rawParams, PayCallbackResultEnum.DUPLICATE, "支付单状态已被其他请求处理");
            log.info("支付回调更新支付单失败，可能已被并发请求处理，payNo={}", notifyDTO.getPayNo());
            return true;
        }

        if (StrUtil.equals(PayStatusEnum.SUCCESS.getCode(), targetStatus)) {
            PaySuccessEvent event = buildPaySuccessEvent(payOrder, notifyDTO);
            payMessageProducer.saveAndSendPaySuccessMessage(event);
        }

        saveCallbackLog(notifyDTO, rawParams, PayCallbackResultEnum.SUCCESS, "支付回调处理成功");
        log.info("支付回调处理成功，payNo={}，businessOrderNo={}，targetStatus={}",
                notifyDTO.getPayNo(), notifyDTO.getBusinessOrderNo(), targetStatus);
        return true;
    }

    /**
     * 转换回调参数
     *
     * @param params 原始回调参数
     * @return 支付回调通知参数
     */
    private PayNotifyDTO convertNotifyDTO(Map<String, Object> params) {
        PayNotifyDTO notifyDTO = new PayNotifyDTO();
        notifyDTO.setPayNo(Convert.toStr(params.get("payNo")));
        notifyDTO.setBusinessOrderNo(Convert.toStr(params.get("businessOrderNo")));
        notifyDTO.setChannelCode(Convert.toStr(params.get("channelCode")));
        notifyDTO.setChannelTradeNo(Convert.toStr(params.get("channelTradeNo")));
        notifyDTO.setPayAmount(Convert.toBigDecimal(params.get("payAmount")));
        notifyDTO.setPayStatus(Convert.toStr(params.get("payStatus")));
        notifyDTO.setNotifyTime(Convert.toStr(params.get("notifyTime")));
        notifyDTO.setNonce(Convert.toStr(params.get("nonce")));
        notifyDTO.setSign(Convert.toStr(params.get("sign")));

        if (StrUtil.isBlank(notifyDTO.getPayNo())) {
            throw new BizException("回调参数 payNo 不能为空");
        }
        if (StrUtil.isBlank(notifyDTO.getChannelCode())) {
            throw new BizException("回调参数 channelCode 不能为空");
        }
        if (notifyDTO.getPayAmount() == null) {
            throw new BizException("回调参数 payAmount 不能为空");
        }
        return notifyDTO;
    }

    /**
     * 验证支付回调签名
     *
     * @param params 原始回调参数
     * @param notifyDTO 回调通知参数
     * @return 是否通过
     */
    private boolean verifySign(Map<String, Object> params, PayNotifyDTO notifyDTO) {
        String secret = payChannelSecretProvider.getSecret(notifyDTO.getChannelCode());
        return PaySignUtil.verify(params, secret, notifyDTO.getSign());
    }

    /**
     * 转换渠道支付状态
     *
     * @param channelStatus 渠道状态
     * @return 平台支付状态
     */
    private String convertChannelStatus(String channelStatus) {
        PayStatusEnum statusEnum = PayStatusEnum.of(channelStatus);
        if (statusEnum == null) {
            throw new BizException("未知支付状态：" + channelStatus);
        }
        return statusEnum.getCode();
    }

    /**
     * 更新支付单状态
     *
     * @param payOrder 支付单
     * @param notifyDTO 回调通知参数
     * @param targetStatus 目标状态
     * @return 更新行数
     */
    private int updatePayOrderStatus(PayOrder payOrder, PayNotifyDTO notifyDTO, String targetStatus) {
        LambdaUpdateWrapper<PayOrder> updateWrapper = new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getPayNo, payOrder.getPayNo())
                .eq(PayOrder::getPayStatus, payOrder.getPayStatus())
                .set(PayOrder::getPayStatus, targetStatus)
                .set(PayOrder::getChannelTradeNo, notifyDTO.getChannelTradeNo())
                .set(PayOrder::getNotifyTime, LocalDateTime.now())
                .setSql("notify_count = notify_count + 1")
                .setSql("version = version + 1");

        if (StrUtil.equals(PayStatusEnum.SUCCESS.getCode(), targetStatus)) {
            updateWrapper.set(PayOrder::getSuccessTime, parseNotifyTime(notifyDTO.getNotifyTime()));
        }

        if (StrUtil.equals(PayStatusEnum.CLOSED.getCode(), targetStatus)) {
            updateWrapper.set(PayOrder::getCloseTime, LocalDateTime.now());
        }

        return payOrderMapper.update(null, updateWrapper);
    }

    /**
     * 解析回调时间
     *
     * @param notifyTime 回调时间
     * @return 本地时间
     */
    private LocalDateTime parseNotifyTime(String notifyTime) {
        if (StrUtil.isBlank(notifyTime)) {
            return LocalDateTime.now();
        }
        return LocalDateTimeUtil.parse(notifyTime, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 构建支付成功事件
     *
     * @param payOrder 支付单
     * @param notifyDTO 回调通知参数
     * @return 支付成功事件
     */
    private PaySuccessEvent buildPaySuccessEvent(PayOrder payOrder, PayNotifyDTO notifyDTO) {
        PaySuccessEvent event = new PaySuccessEvent();
        event.setPayNo(payOrder.getPayNo());
        event.setBusinessOrderNo(payOrder.getBusinessOrderNo());
        event.setChannelCode(payOrder.getChannelCode());
        event.setChannelTradeNo(notifyDTO.getChannelTradeNo());
        event.setPayAmount(payOrder.getPayAmount());
        event.setPayTime(parseNotifyTime(notifyDTO.getNotifyTime()));
        return event;
    }

    /**
     * 保存支付回调日志
     *
     * @param notifyDTO 回调参数
     * @param rawParams 原始参数
     * @param resultEnum 回调处理结果
     * @param message 处理说明
     */
    private void saveCallbackLog(PayNotifyDTO notifyDTO,
                                 Map<String, Object> rawParams,
                                 PayCallbackResultEnum resultEnum,
                                 String message) {
        PayCallbackLog callbackLog = new PayCallbackLog();
        callbackLog.setPayNo(notifyDTO.getPayNo());
        callbackLog.setBusinessOrderNo(notifyDTO.getBusinessOrderNo());
        callbackLog.setChannelCode(notifyDTO.getChannelCode());
        callbackLog.setChannelTradeNo(notifyDTO.getChannelTradeNo());
        callbackLog.setCallbackStatus(resultEnum.getCode());
        callbackLog.setRequestBody(JSONUtil.toJsonStr(rawParams));
        callbackLog.setRequestSign(notifyDTO.getSign());
        callbackLog.setErrorMsg(message);
        payCallbackLogMapper.insert(callbackLog);
    }
}
```

### 支付状态不可逆更新

上面的 `updatePayOrderStatus` 使用了两个关键条件：

```java
.eq(PayOrder::getPayNo, payOrder.getPayNo())
.eq(PayOrder::getPayStatus, payOrder.getPayStatus())
```

这表示只有数据库中的支付状态仍然等于当前查询出来的状态时，才允许更新。这样可以避免并发回调覆盖状态。

状态是否允许变化由 `PayStatusEnum.canChangeTo` 控制：

```java
if (!PayStatusEnum.canChangeTo(payOrder.getPayStatus(), targetStatus)) {
    return true;
}
```

核心规则是：

| 当前状态  | 回调状态  | 处理方式                         |
| --------- | --------- | -------------------------------- |
| `WAITING` | `SUCCESS` | 更新为支付成功，发送支付成功事件 |
| `WAITING` | `FAILED`  | 更新为支付失败                   |
| `WAITING` | `CLOSED`  | 更新为已关闭                     |
| `FAILED`  | `SUCCESS` | 允许修正为支付成功               |
| `CLOSED`  | `SUCCESS` | 允许修正为支付成功               |
| `SUCCESS` | `FAILED`  | 拒绝变更                         |
| `SUCCESS` | `CLOSED`  | 拒绝变更                         |
| `SUCCESS` | `SUCCESS` | 重复回调，直接返回成功           |

### 回调日志落库

回调日志表记录每一次支付渠道通知。即使回调失败，也应该落库。

建议记录以下场景：

| 场景           | `callback_status` |
| -------------- | ----------------- |
| 处理成功       | `SUCCESS`         |
| 重复回调       | `DUPLICATE`       |
| 验签失败       | `SIGN_INVALID`    |
| 金额不一致     | `AMOUNT_INVALID`  |
| 支付单不存在   | `ORDER_NOT_FOUND` |
| 状态不允许变更 | `STATUS_INVALID`  |
| 系统异常       | `FAILED`          |

回调日志是排查支付问题的第一现场，不建议只写应用日志。应用日志可能滚动清理，而回调日志应该保留更长时间。

## 业务订单状态推进

支付单成功后，不建议在支付系统里直接修改订单表。更合理的做法是发送“支付成功事件”，由订单系统消费后推进业务订单状态。这样可以降低系统耦合，并通过 MQ 实现最终一致性。

本案例采用：

```text
支付回调成功
-> 更新支付单为 SUCCESS
-> 写入本地消息表
-> 事务提交后投递 RabbitMQ
-> 订单服务消费支付成功事件
-> 幂等更新订单状态
```

### 支付成功事件发送

支付成功事件只允许在支付单首次进入 `SUCCESS` 状态时发送。如果是重复成功回调，不再重复发送事件。

文件位置：`src/main/java/io/github/atengk/payment/mq/event/PaySuccessEvent.java`

```java
package io.github.atengk.payment.mq.event;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付成功事件
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class PaySuccessEvent {

    private String payNo;

    private String businessOrderNo;

    private String channelCode;

    private String channelTradeNo;

    private BigDecimal payAmount;

    private LocalDateTime payTime;
}
```

事件字段说明：

| 字段              | 说明             |
| ----------------- | ---------------- |
| `payNo`           | 平台支付单号     |
| `businessOrderNo` | 业务订单号       |
| `channelCode`     | 支付渠道         |
| `channelTradeNo`  | 第三方支付流水号 |
| `payAmount`       | 支付金额         |
| `payTime`         | 支付成功时间     |

### RabbitMQ 消息投递

RabbitMQ 配置包括交换机、队列和绑定关系。

文件位置：`src/main/java/io/github/atengk/payment/config/RabbitMqConfig.java`

```java
package io.github.atengk.payment.config;

import io.github.atengk.payment.common.constant.PaymentMqConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 支付交换机
     *
     * @return Direct交换机
     */
    @Bean
    public DirectExchange paymentExchange() {
        return ExchangeBuilder.directExchange(PaymentMqConstant.PAY_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 支付成功队列
     *
     * @return 支付成功队列
     */
    @Bean
    public Queue paySuccessQueue() {
        return QueueBuilder.durable(PaymentMqConstant.PAY_SUCCESS_QUEUE)
                .build();
    }

    /**
     * 支付成功绑定关系
     *
     * @return 绑定关系
     */
    @Bean
    public Binding paySuccessBinding() {
        return BindingBuilder.bind(paySuccessQueue())
                .to(paymentExchange())
                .with(PaymentMqConstant.PAY_SUCCESS_ROUTING_KEY);
    }
}
```

消息生产者负责两件事：

1. 先写入本地消息表；
2. 数据库事务提交后再投递 RabbitMQ。

文件位置：`src/main/java/io/github/atengk/payment/mq/producer/PayMessageProducer.java`

```java
package io.github.atengk.payment.mq.producer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.payment.common.constant.PaymentMqConstant;
import io.github.atengk.payment.domain.entity.PayLocalMessage;
import io.github.atengk.payment.mapper.PayLocalMessageMapper;
import io.github.atengk.payment.mq.event.PaySuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 支付消息生产者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final PayLocalMessageMapper payLocalMessageMapper;

    /**
     * 保存并发送支付成功消息
     *
     * @param event 支付成功事件
     */
    public void saveAndSendPaySuccessMessage(PaySuccessEvent event) {
        String messageNo = "MSG" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + IdUtil.getSnowflakeNextIdStr();
        String messageBody = JSONUtil.toJsonStr(event);

        PayLocalMessage localMessage = new PayLocalMessage();
        localMessage.setMessageNo(messageNo);
        localMessage.setBusinessKey(event.getPayNo());
        localMessage.setExchangeName(PaymentMqConstant.PAY_EXCHANGE);
        localMessage.setRoutingKey(PaymentMqConstant.PAY_SUCCESS_ROUTING_KEY);
        localMessage.setMessageBody(messageBody);
        localMessage.setSendStatus("WAITING");
        localMessage.setRetryCount(0);
        localMessage.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
        payLocalMessageMapper.insert(localMessage);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendLocalMessage(localMessage);
            }
        });

        log.info("支付成功本地消息已保存，payNo={}，messageNo={}", event.getPayNo(), messageNo);
    }

    /**
     * 发送本地消息
     *
     * @param localMessage 本地消息
     */
    public void sendLocalMessage(PayLocalMessage localMessage) {
        try {
            rabbitTemplate.convertAndSend(
                    localMessage.getExchangeName(),
                    localMessage.getRoutingKey(),
                    localMessage.getMessageBody()
            );

            payLocalMessageMapper.update(null, new LambdaUpdateWrapper<PayLocalMessage>()
                    .eq(PayLocalMessage::getMessageNo, localMessage.getMessageNo())
                    .set(PayLocalMessage::getSendStatus, "SUCCESS")
                    .set(PayLocalMessage::getErrorMsg, null));

            log.info("支付成功消息投递成功，messageNo={}，businessKey={}",
                    localMessage.getMessageNo(), localMessage.getBusinessKey());
        } catch (Exception e) {
            payLocalMessageMapper.update(null, new LambdaUpdateWrapper<PayLocalMessage>()
                    .eq(PayLocalMessage::getMessageNo, localMessage.getMessageNo())
                    .set(PayLocalMessage::getSendStatus, "FAILED")
                    .set(PayLocalMessage::getNextRetryTime, LocalDateTime.now().plusMinutes(5))
                    .setSql("retry_count = retry_count + 1")
                    .set(PayLocalMessage::getErrorMsg, e.getMessage()));

            log.error("支付成功消息投递失败，messageNo={}，businessKey={}",
                    localMessage.getMessageNo(), localMessage.getBusinessKey(), e);
        }
    }
}
```

这里使用 `TransactionSynchronizationManager.afterCommit` 是为了避免事务还没提交，MQ 消费者就消费到消息。如果消息先被消费，而支付单事务最终回滚，就会造成订单状态错误推进。

### 订单服务消费处理

订单服务消费支付成功事件后，需要校验业务订单是否存在、金额是否一致、状态是否允许推进，然后更新订单状态。

本案例用一个模拟消费者表示订单系统处理逻辑。真实项目中，这个消费者应该放在订单服务中。

文件位置：`src/main/java/io/github/atengk/payment/mq/consumer/PaySuccessConsumer.java`

```java
package io.github.atengk.payment.mq.consumer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.payment.common.constant.PaymentMqConstant;
import io.github.atengk.payment.mq.event.PaySuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 支付成功消息消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaySuccessConsumer {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 消费支付成功消息
     *
     * @param messageBody 消息内容
     */
    @RabbitListener(queues = PaymentMqConstant.PAY_SUCCESS_QUEUE)
    public void consumePaySuccess(String messageBody) {
        PaySuccessEvent event = JSONUtil.toBean(messageBody, PaySuccessEvent.class);
        String consumeKey = buildConsumeKey(event.getPayNo());

        Boolean firstConsume = stringRedisTemplate.opsForValue()
                .setIfAbsent(consumeKey, "1", Duration.ofDays(30));

        if (!Boolean.TRUE.equals(firstConsume)) {
            log.info("支付成功消息重复消费，payNo={}，businessOrderNo={}",
                    event.getPayNo(), event.getBusinessOrderNo());
            return;
        }

        // 示例：真实项目中这里应调用订单服务或更新订单表
        log.info("开始推进业务订单状态，businessOrderNo={}，payNo={}，payAmount={}",
                event.getBusinessOrderNo(), event.getPayNo(), event.getPayAmount());

        // 伪代码：
        // 1. 查询业务订单
        // 2. 校验订单金额和支付金额
        // 3. 判断订单状态是否为待支付
        // 4. 更新订单状态为已支付
        // 5. 记录订单支付流水
        // 6. 触发后续履约流程

        log.info("业务订单状态推进完成，businessOrderNo={}，payNo={}",
                event.getBusinessOrderNo(), event.getPayNo());
    }

    /**
     * 构建消费幂等 Key
     *
     * @param payNo 支付单号
     * @return Redis Key
     */
    private String buildConsumeKey(String payNo) {
        return StrUtil.format("payment:mq:consume:pay-success:{}", payNo);
    }
}
```

订单服务真实处理时，建议使用类似下面的 SQL 条件更新：

```sql
-- 只有待支付订单才允许更新为已支付，避免重复消费导致状态重复推进
UPDATE biz_order
SET order_status = 'PAID',
    pay_no = #{payNo},
    pay_time = #{payTime},
    update_time = NOW()
WHERE order_no = #{businessOrderNo}
  AND order_status = 'WAIT_PAY';
```

如果返回更新行数为 `0`，说明订单可能已经处理过，消费者应按幂等成功处理，不要无限重试。

### 消费幂等控制

支付成功消息可能重复投递，也可能被 RabbitMQ 重新投递，因此消费者必须幂等。

推荐使用三层幂等：

| 层级           | 说明                                 |
| -------------- | ------------------------------------ |
| Redis 幂等 Key | 快速拦截短期重复消费                 |
| 订单状态判断   | 只有待支付订单允许推进为已支付       |
| 数据库唯一约束 | 订单支付流水表对 `pay_no` 建唯一索引 |

消费者处理建议：

```text
收到支付成功消息
-> 解析消息体
-> Redis setIfAbsent 做消费幂等
-> 查询业务订单
-> 校验金额
-> 判断订单状态
-> 条件更新订单为已支付
-> 写入订单支付流水
-> 返回消费成功
```

不要只依赖 Redis 幂等。Redis Key 可能过期或被清理，数据库状态判断才是最终兜底。

### 回调测试示例

先构造回调参数，再按同样签名规则生成 `sign`。这里展示最终请求格式：

```bash
curl -X POST 'http://localhost:8080/api/pay/callback/MOCK/notify' \
  -H 'Content-Type: application/json' \
  -d '{
    "payNo": "PAY202605151030321831202844829540352",
    "businessOrderNo": "ORDER202605150001",
    "channelTradeNo": "MOCK_TRADE_1831202888888888888",
    "payAmount": "99.90",
    "payStatus": "SUCCESS",
    "notifyTime": "2026-05-15 11:05:30",
    "nonce": "a8d7c9e1b2f34e9a80c1",
    "sign": "这里替换为PaySignUtil生成的签名"
  }'
```

验证支付单状态：

```sql
SELECT pay_no,
       business_order_no,
       channel_trade_no,
       pay_amount,
       pay_status,
       success_time,
       notify_count
FROM pay_order
WHERE pay_no = 'PAY202605151030321831202844829540352';
```

验证回调日志：

```sql
SELECT pay_no,
       channel_trade_no,
       callback_status,
       error_msg,
       create_time
FROM pay_callback_log
WHERE pay_no = 'PAY202605151030321831202844829540352'
ORDER BY id DESC;
```

验证本地消息：

```sql
SELECT message_no,
       business_key,
       exchange_name,
       routing_key,
       send_status,
       retry_count,
       error_msg
FROM pay_local_message
WHERE business_key = 'PAY202605151030321831202844829540352';
```

本节完成后，支付系统已经具备支付回调验签、金额校验、幂等处理、状态不可逆更新、回调日志留痕、支付成功事件发送和订单状态异步推进能力。

## 主动查询补偿

主动查询补偿用于处理支付回调丢失、回调失败、MQ 投递失败前的状态悬挂等问题。支付系统不能只依赖第三方异步回调，因为 README 中也明确提到了“支付通知丢失、主动查询补偿、对账差异处理”等核心难点。

本案例的补偿策略是：

| 场景                       | 处理方式                                 |
| -------------------------- | ---------------------------------------- |
| 支付单长时间处于 `WAITING` | 定时查询第三方支付状态                   |
| 第三方返回 `SUCCESS`       | 更新平台支付单为成功，并发送支付成功事件 |
| 第三方返回 `CLOSED`        | 更新平台支付单为已关闭                   |
| 第三方仍返回 `WAITING`     | 暂不处理，等待下次补偿                   |
| 平台已是 `SUCCESS`         | 直接跳过，避免重复推进订单               |
| 状态不允许变更             | 记录日志，不强行覆盖                     |

### 未支付订单扫描

未支付订单扫描由 XXL-JOB 或 Spring 定时任务触发。核心逻辑是扫描超过一定时间仍未进入终态的支付单，然后逐笔调用第三方支付查询接口。

本案例先提供 Service 层实现，后续可以直接被 XXL-JOB 调用。

文件位置：`src/main/java/io/github/atengk/payment/service/PayCompensateService.java`

```java
package io.github.atengk.payment.service;

/**
 * 支付补偿服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayCompensateService {

    /**
     * 扫描并补偿未完成支付单
     *
     * @param limit 单次扫描数量
     * @return 补偿处理数量
     */
    int compensateWaitingPayOrders(Integer limit);

    /**
     * 补偿指定支付单
     *
     * @param payNo 支付单号
     * @return 是否处理成功
     */
    boolean compensateByPayNo(String payNo);
}
```

扫描规则建议控制单次数量，避免一次任务扫太多数据压垮支付渠道或数据库。

```sql
-- 扫描超过5分钟仍未完成的待支付单
SELECT *
FROM pay_order
WHERE pay_status = 'WAITING'
  AND create_time <= DATE_SUB(NOW(), INTERVAL 5 MINUTE)
ORDER BY create_time ASC
LIMIT 100;
```

### 第三方支付状态查询

先给支付渠道客户端增加查单能力。真实项目中，这里会调用支付宝、微信支付或银行支付渠道的订单查询接口。

文件位置：`src/main/java/io/github/atengk/payment/third/model/PayChannelQueryResult.java`

```java
package io.github.atengk.payment.third.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付渠道查单结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class PayChannelQueryResult {

    private String payNo;

    private String channelCode;

    private String channelTradeNo;

    private BigDecimal payAmount;

    private String payStatus;

    private LocalDateTime payTime;

    private String rawResponse;
}
```

给支付渠道接口增加 `queryPay` 方法。

文件位置：`src/main/java/io/github/atengk/payment/third/client/PayChannelClient.java`

```java
package io.github.atengk.payment.third.client;

import io.github.atengk.payment.third.model.PayChannelQueryResult;
import io.github.atengk.payment.third.model.PayChannelRequest;
import io.github.atengk.payment.third.model.PayChannelResponse;

/**
 * 支付渠道客户端
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayChannelClient {

    /**
     * 判断是否支持当前渠道
     *
     * @param channelCode 渠道编码
     * @return 是否支持
     */
    boolean supports(String channelCode);

    /**
     * 创建支付
     *
     * @param request 支付渠道请求
     * @return 支付渠道响应
     */
    PayChannelResponse createPay(PayChannelRequest request);

    /**
     * 查询支付状态
     *
     * @param payNo 平台支付单号
     * @return 渠道查单结果
     */
    PayChannelQueryResult queryPay(String payNo);
}
```

模拟支付渠道查询实现如下。为了便于测试，这里使用支付单号后缀模拟不同状态：以 `8` 结尾返回成功，以 `9` 结尾返回关闭，其他默认待支付。真实项目中替换为渠道查单接口即可。

文件位置：`src/main/java/io/github/atengk/payment/third/client/MockPayClient.java`

```java
package io.github.atengk.payment.third.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.payment.common.enums.PayChannelEnum;
import io.github.atengk.payment.common.enums.PayStatusEnum;
import io.github.atengk.payment.third.model.PayChannelQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 模拟支付渠道客户端
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class MockPayClient implements PayChannelClient {

    /**
     * 判断是否支持当前渠道
     *
     * @param channelCode 渠道编码
     * @return 是否支持
     */
    @Override
    public boolean supports(String channelCode) {
        return StrUtil.equals(PayChannelEnum.MOCK.getCode(), channelCode);
    }

    /**
     * 创建支付
     *
     * @param request 支付渠道请求
     * @return 支付渠道响应
     */
    @Override
    public PayChannelResponse createPay(PayChannelRequest request) {
        // 这里沿用上一节 createPay 实现，避免重复展开
        throw new UnsupportedOperationException("请保留上一节中的 createPay 实现");
    }

    /**
     * 查询模拟支付状态
     *
     * @param payNo 平台支付单号
     * @return 渠道查单结果
     */
    @Override
    public PayChannelQueryResult queryPay(String payNo) {
        String status = PayStatusEnum.WAITING.getCode();

        if (StrUtil.endWith(payNo, "8")) {
            status = PayStatusEnum.SUCCESS.getCode();
        } else if (StrUtil.endWith(payNo, "9")) {
            status = PayStatusEnum.CLOSED.getCode();
        }

        String channelTradeNo = StrUtil.equals(status, PayStatusEnum.SUCCESS.getCode())
                ? "MOCK_TRADE_" + IdUtil.getSnowflakeNextIdStr()
                : null;

        Map<String, Object> raw = Map.of(
                "payNo", payNo,
                "channelCode", PayChannelEnum.MOCK.getCode(),
                "payStatus", status,
                "channelTradeNo", StrUtil.blankToDefault(channelTradeNo, "")
        );

        log.info("模拟支付渠道查单完成，payNo={}，payStatus={}", payNo, status);

        return PayChannelQueryResult.builder()
                .payNo(payNo)
                .channelCode(PayChannelEnum.MOCK.getCode())
                .channelTradeNo(channelTradeNo)
                .payAmount(new BigDecimal("99.90"))
                .payStatus(status)
                .payTime(StrUtil.equals(status, PayStatusEnum.SUCCESS.getCode()) ? LocalDateTime.now() : null)
                .rawResponse(JSONUtil.toJsonStr(raw))
                .build();
    }
}
```

如果你保留了上一节完整 `MockPayClient#createPay`，这里只需要把 `queryPay` 方法追加进去，不要覆盖原文件中的创建支付逻辑。

### 支付成功补偿处理

支付成功补偿本质上和支付回调成功处理一样：更新支付单为 `SUCCESS`，然后发送支付成功事件。关键点是不能绕过状态机，也不能重复发送 MQ。

文件位置：`src/main/java/io/github/atengk/payment/service/impl/PayCompensateServiceImpl.java`

```java
package io.github.atengk.payment.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.payment.common.constant.PaymentRedisKeyConstant;
import io.github.atengk.payment.common.enums.PayStatusEnum;
import io.github.atengk.payment.common.exception.BizException;
import io.github.atengk.payment.common.util.PayAmountUtil;
import io.github.atengk.payment.domain.entity.PayOrder;
import io.github.atengk.payment.mapper.PayOrderMapper;
import io.github.atengk.payment.mq.event.PaySuccessEvent;
import io.github.atengk.payment.mq.producer.PayMessageProducer;
import io.github.atengk.payment.service.PayCompensateService;
import io.github.atengk.payment.third.client.PayChannelClient;
import io.github.atengk.payment.third.model.PayChannelQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 支付补偿服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayCompensateServiceImpl implements PayCompensateService {

    private final PayOrderMapper payOrderMapper;
    private final List<PayChannelClient> payChannelClients;
    private final PayMessageProducer payMessageProducer;
    private final RedissonClient redissonClient;

    /**
     * 扫描并补偿未完成支付单
     *
     * @param limit 单次扫描数量
     * @return 补偿处理数量
     */
    @Override
    public int compensateWaitingPayOrders(Integer limit) {
        int queryLimit = ObjectUtil.defaultIfNull(limit, 100);
        LocalDateTime beforeTime = LocalDateTime.now().minusMinutes(5);

        List<PayOrder> payOrders = payOrderMapper.selectList(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getPayStatus, PayStatusEnum.WAITING.getCode())
                .le(PayOrder::getCreateTime, beforeTime)
                .orderByAsc(PayOrder::getCreateTime)
                .last("LIMIT " + queryLimit));

        if (CollUtil.isEmpty(payOrders)) {
            log.info("暂无需要补偿的待支付单");
            return 0;
        }

        int count = 0;
        for (PayOrder payOrder : payOrders) {
            if (compensateByPayNo(payOrder.getPayNo())) {
                count++;
            }
        }

        log.info("待支付单补偿扫描完成，scanCount={}，handleCount={}", payOrders.size(), count);
        return count;
    }

    /**
     * 补偿指定支付单
     *
     * @param payNo 支付单号
     * @return 是否处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean compensateByPayNo(String payNo) {
        String lockKey = PaymentRedisKeyConstant.callbackLockKey(payNo);
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("支付补偿获取锁失败，payNo={}", payNo);
                return false;
            }

            PayOrder payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrder>()
                    .eq(PayOrder::getPayNo, payNo)
                    .last("LIMIT 1"));

            if (payOrder == null) {
                log.warn("支付补偿失败，支付单不存在，payNo={}", payNo);
                return false;
            }

            if (StrUtil.equals(PayStatusEnum.SUCCESS.getCode(), payOrder.getPayStatus())) {
                log.info("支付单已成功，无需补偿，payNo={}", payNo);
                return true;
            }

            PayChannelClient client = getPayChannelClient(payOrder.getChannelCode());
            PayChannelQueryResult queryResult = client.queryPay(payNo);

            if (StrUtil.equals(PayStatusEnum.SUCCESS.getCode(), queryResult.getPayStatus())) {
                return compensateSuccess(payOrder, queryResult);
            }

            if (StrUtil.equals(PayStatusEnum.CLOSED.getCode(), queryResult.getPayStatus())) {
                return compensateClosed(payOrder, queryResult);
            }

            log.info("渠道支付状态仍未完成，暂不补偿，payNo={}，channelStatus={}",
                    payNo, queryResult.getPayStatus());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("支付补偿获取锁被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 支付成功补偿处理
     *
     * @param payOrder 平台支付单
     * @param queryResult 渠道查单结果
     * @return 是否处理成功
     */
    private boolean compensateSuccess(PayOrder payOrder, PayChannelQueryResult queryResult) {
        if (!PayAmountUtil.equals(payOrder.getPayAmount(), queryResult.getPayAmount())) {
            log.warn("支付成功补偿金额不一致，payNo={}，platformAmount={}，channelAmount={}",
                    payOrder.getPayNo(), payOrder.getPayAmount(), queryResult.getPayAmount());
            return false;
        }

        if (!PayStatusEnum.canChangeTo(payOrder.getPayStatus(), PayStatusEnum.SUCCESS.getCode())) {
            log.warn("支付成功补偿状态不允许变更，payNo={}，currentStatus={}",
                    payOrder.getPayNo(), payOrder.getPayStatus());
            return true;
        }

        int rows = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getPayNo, payOrder.getPayNo())
                .eq(PayOrder::getPayStatus, payOrder.getPayStatus())
                .set(PayOrder::getPayStatus, PayStatusEnum.SUCCESS.getCode())
                .set(PayOrder::getChannelTradeNo, queryResult.getChannelTradeNo())
                .set(PayOrder::getSuccessTime, ObjectUtil.defaultIfNull(queryResult.getPayTime(), LocalDateTime.now()))
                .setSql("version = version + 1"));

        if (rows <= 0) {
            log.info("支付成功补偿未更新数据，可能已被其他线程处理，payNo={}", payOrder.getPayNo());
            return true;
        }

        PaySuccessEvent event = new PaySuccessEvent();
        event.setPayNo(payOrder.getPayNo());
        event.setBusinessOrderNo(payOrder.getBusinessOrderNo());
        event.setChannelCode(payOrder.getChannelCode());
        event.setChannelTradeNo(queryResult.getChannelTradeNo());
        event.setPayAmount(payOrder.getPayAmount());
        event.setPayTime(ObjectUtil.defaultIfNull(queryResult.getPayTime(), LocalDateTime.now()));
        payMessageProducer.saveAndSendPaySuccessMessage(event);

        log.info("支付成功补偿完成，payNo={}，businessOrderNo={}",
                payOrder.getPayNo(), payOrder.getBusinessOrderNo());
        return true;
    }

    /**
     * 支付关闭补偿处理
     *
     * @param payOrder 平台支付单
     * @param queryResult 渠道查单结果
     * @return 是否处理成功
     */
    private boolean compensateClosed(PayOrder payOrder, PayChannelQueryResult queryResult) {
        if (!PayStatusEnum.canChangeTo(payOrder.getPayStatus(), PayStatusEnum.CLOSED.getCode())) {
            log.warn("支付关闭补偿状态不允许变更，payNo={}，currentStatus={}",
                    payOrder.getPayNo(), payOrder.getPayStatus());
            return true;
        }

        int rows = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getPayNo, payOrder.getPayNo())
                .eq(PayOrder::getPayStatus, payOrder.getPayStatus())
                .set(PayOrder::getPayStatus, PayStatusEnum.CLOSED.getCode())
                .set(PayOrder::getCloseTime, LocalDateTime.now())
                .setSql("version = version + 1"));

        log.info("支付关闭补偿完成，payNo={}，updateRows={}，rawResponse={}",
                payOrder.getPayNo(), rows, queryResult.getRawResponse());
        return true;
    }

    /**
     * 获取支付渠道客户端
     *
     * @param channelCode 渠道编码
     * @return 支付渠道客户端
     */
    private PayChannelClient getPayChannelClient(String channelCode) {
        return payChannelClients.stream()
                .filter(client -> client.supports(channelCode))
                .findFirst()
                .orElseThrow(() -> new BizException("未找到支付渠道客户端：" + channelCode));
    }
}
```

### 支付关闭补偿处理

支付关闭补偿只更新支付单，不发送支付成功事件。需要注意的是，支付关闭不是强终态，在部分支付渠道中，后续仍可能查到成功状态，所以本案例允许 `CLOSED -> SUCCESS` 的状态修正。

关闭补偿触发条件建议满足以下任意一种：

| 条件                                       | 说明                |
| ------------------------------------------ | ------------------- |
| 渠道明确返回关闭                           | 可以更新为 `CLOSED` |
| 平台支付单已过期，并且渠道返回不存在或关闭 | 可以更新为 `CLOSED` |
| 平台支付单已过期，但渠道仍返回支付中       | 暂不关闭，继续观察  |
| 平台支付单已成功                           | 不允许关闭          |

不要只根据平台过期时间直接关闭支付单。更稳妥的方式是先查渠道状态，确认渠道未成功后再关闭。

## 对账补偿处理

主动查询补偿解决的是短周期状态悬挂问题，对账补偿解决的是日终账务一致性问题。对账应以渠道账单为重要参考，但不能无脑覆盖平台数据。特别是金额不一致、平台缺单、渠道缺单这类差异，需要保守处理。

本案例采用模拟渠道账单导入方式实现核心逻辑：

```text
渠道账单列表
-> 查询平台支付单
-> 按 payNo 或 channelTradeNo 匹配
-> 校验金额
-> 校验状态
-> 生成对账差异
-> 对可自动修复的状态差异执行补偿
```

### 渠道账单下载或模拟导入

真实项目通常通过支付渠道提供的账单下载接口获取 CSV、TXT 或 ZIP 文件。本案例先使用 DTO 模拟渠道账单行数据。

文件位置：`src/main/java/io/github/atengk/payment/domain/dto/ChannelBillItemDTO.java`

```java
package io.github.atengk.payment.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 渠道账单明细
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ChannelBillItemDTO {

    private LocalDate billDate;

    private String channelCode;

    private String payNo;

    private String businessOrderNo;

    private String channelTradeNo;

    private BigDecimal payAmount;

    private String payStatus;
}
```

模拟导入请求对象如下。

文件位置：`src/main/java/io/github/atengk/payment/domain/dto/ReconcileRequestDTO.java`

```java
package io.github.atengk.payment.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 对账请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ReconcileRequestDTO {

    @NotBlank(message = "支付渠道不能为空")
    private String channelCode;

    private LocalDate billDate;

    @Valid
    @NotEmpty(message = "渠道账单不能为空")
    private List<ChannelBillItemDTO> billItems;
}
```

对账接口用于触发模拟对账和处理差异。

文件位置：`src/main/java/io/github/atengk/payment/controller/PayReconcileController.java`

```java
package io.github.atengk.payment.controller;

import io.github.atengk.payment.common.result.Result;
import io.github.atengk.payment.domain.dto.ReconcileRequestDTO;
import io.github.atengk.payment.service.PayReconcileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付对账控制器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pay/reconcile")
public class PayReconcileController {

    private final PayReconcileService payReconcileService;

    /**
     * 执行模拟对账
     *
     * @param dto 对账请求
     * @return 对账批次号
     */
    @PostMapping("/mock")
    public Result<String> mockReconcile(@Valid @RequestBody ReconcileRequestDTO dto) {
        return Result.ok(payReconcileService.reconcile(dto));
    }

    /**
     * 自动补偿指定对账差异
     *
     * @param diffId 差异ID
     * @return 是否处理成功
     */
    @PostMapping("/diff/{diffId}/fix")
    public Result<Boolean> fixDiff(@PathVariable Long diffId) {
        return Result.ok(payReconcileService.fixDiff(diffId));
    }
}
```

### 平台支付单匹配

匹配优先级建议如下：

| 优先级 | 匹配字段          | 说明                             |
| ------ | ----------------- | -------------------------------- |
| 1      | `payNo`           | 平台支付单号最可靠               |
| 2      | `channelTradeNo`  | 渠道流水号适合支付成功后的账单   |
| 3      | `businessOrderNo` | 可作为兜底，但要注意重新支付场景 |

Service 接口如下。

文件位置：`src/main/java/io/github/atengk/payment/service/PayReconcileService.java`

```java
package io.github.atengk.payment.service;

import io.github.atengk.payment.domain.dto.ReconcileRequestDTO;

/**
 * 支付对账服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayReconcileService {

    /**
     * 执行对账
     *
     * @param dto 对账请求
     * @return 对账批次号
     */
    String reconcile(ReconcileRequestDTO dto);

    /**
     * 修复指定对账差异
     *
     * @param diffId 差异ID
     * @return 是否修复成功
     */
    Boolean fixDiff(Long diffId);
}
```

### 金额差异识别

金额差异以平台支付单金额和渠道账单金额对比为准。仍然使用 `BigDecimal.compareTo`，不要使用 `equals`。

```java
if (!PayAmountUtil.equals(payOrder.getPayAmount(), billItem.getPayAmount())) {
    saveDiff(..., ReconcileDiffTypeEnum.AMOUNT_NOT_MATCH, ...);
}
```

金额不一致一般不建议自动补偿，因为它可能涉及优惠、手续费、渠道扣款异常、订单金额被篡改等问题。

### 状态差异识别

状态差异识别需要先做状态映射。为了简化案例，模拟渠道账单直接使用平台状态编码：`WAITING`、`SUCCESS`、`FAILED`、`CLOSED`。

真实项目中需要维护状态映射，例如：

| 渠道状态        | 平台状态  |
| --------------- | --------- |
| `TRADE_SUCCESS` | `SUCCESS` |
| `SUCCESS`       | `SUCCESS` |
| `NOTPAY`        | `WAITING` |
| `TRADE_CLOSED`  | `CLOSED`  |
| `PAYERROR`      | `FAILED`  |

当渠道账单显示成功，而平台仍是待支付、失败或关闭时，可以进入自动补偿候选。

### 差异数据落库

下面是对账核心实现，包含平台支付单匹配、金额差异、状态差异、渠道缺单和差异落库。

文件位置：`src/main/java/io/github/atengk/payment/service/impl/PayReconcileServiceImpl.java`

```java
package io.github.atengk.payment.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.payment.common.enums.PayStatusEnum;
import io.github.atengk.payment.common.enums.ReconcileDiffTypeEnum;
import io.github.atengk.payment.common.exception.BizException;
import io.github.atengk.payment.common.util.PayAmountUtil;
import io.github.atengk.payment.domain.dto.ChannelBillItemDTO;
import io.github.atengk.payment.domain.dto.ReconcileRequestDTO;
import io.github.atengk.payment.domain.entity.PayOrder;
import io.github.atengk.payment.domain.entity.PayReconcileDiff;
import io.github.atengk.payment.mapper.PayOrderMapper;
import io.github.atengk.payment.mapper.PayReconcileDiffMapper;
import io.github.atengk.payment.service.PayCompensateService;
import io.github.atengk.payment.service.PayReconcileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 支付对账服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayReconcileServiceImpl implements PayReconcileService {

    private final PayOrderMapper payOrderMapper;
    private final PayReconcileDiffMapper payReconcileDiffMapper;
    private final PayCompensateService payCompensateService;

    /**
     * 执行对账
     *
     * @param dto 对账请求
     * @return 对账批次号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String reconcile(ReconcileRequestDTO dto) {
        if (CollUtil.isEmpty(dto.getBillItems())) {
            throw new BizException("渠道账单不能为空");
        }

        LocalDate billDate = ObjectUtil.defaultIfNull(dto.getBillDate(), LocalDate.now().minusDays(1));
        String batchNo = buildBatchNo(dto.getChannelCode(), billDate);

        int diffCount = 0;
        for (ChannelBillItemDTO billItem : dto.getBillItems()) {
            PayOrder payOrder = matchPayOrder(billItem);

            if (payOrder == null) {
                saveDiff(batchNo, null, billItem, ReconcileDiffTypeEnum.PLATFORM_MISSING, "渠道有账单，平台无支付单");
                diffCount++;
                continue;
            }

            if (!PayAmountUtil.equals(payOrder.getPayAmount(), billItem.getPayAmount())) {
                saveDiff(batchNo, payOrder, billItem, ReconcileDiffTypeEnum.AMOUNT_NOT_MATCH, "平台金额和渠道金额不一致");
                diffCount++;
                continue;
            }

            if (!StrUtil.equals(payOrder.getPayStatus(), billItem.getPayStatus())) {
                saveDiff(batchNo, payOrder, billItem, ReconcileDiffTypeEnum.STATUS_NOT_MATCH, "平台状态和渠道状态不一致");
                diffCount++;
            }
        }

        diffCount += checkChannelMissing(batchNo, dto.getChannelCode(), billDate, dto);

        log.info("支付对账完成，batchNo={}，channelCode={}，billDate={}，diffCount={}",
                batchNo, dto.getChannelCode(), billDate, diffCount);
        return batchNo;
    }

    /**
     * 修复指定对账差异
     *
     * @param diffId 差异ID
     * @return 是否修复成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean fixDiff(Long diffId) {
        PayReconcileDiff diff = payReconcileDiffMapper.selectById(diffId);
        if (diff == null) {
            throw new BizException("对账差异不存在");
        }

        if (!ReconcileDiffTypeEnum.canAutoFix(diff.getDiffType())) {
            throw new BizException("当前差异类型不允许自动补偿：" + diff.getDiffType());
        }

        if (!StrUtil.equals(PayStatusEnum.SUCCESS.getCode(), diff.getChannelStatus())) {
            throw new BizException("只有渠道成功状态才允许自动补偿");
        }

        boolean success = payCompensateService.compensateByPayNo(diff.getPayNo());
        if (success) {
            diff.setHandleStatus("AUTO_FIXED");
            diff.setHandleRemark("已通过支付查单自动补偿");
            payReconcileDiffMapper.updateById(diff);
            log.info("对账差异自动补偿完成，diffId={}，payNo={}", diffId, diff.getPayNo());
        }

        return success;
    }

    /**
     * 匹配平台支付单
     *
     * @param billItem 渠道账单明细
     * @return 平台支付单
     */
    private PayOrder matchPayOrder(ChannelBillItemDTO billItem) {
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(billItem.getPayNo())) {
            wrapper.eq(PayOrder::getPayNo, billItem.getPayNo());
        } else if (StrUtil.isNotBlank(billItem.getChannelTradeNo())) {
            wrapper.eq(PayOrder::getChannelTradeNo, billItem.getChannelTradeNo());
        } else if (StrUtil.isNotBlank(billItem.getBusinessOrderNo())) {
            wrapper.eq(PayOrder::getBusinessOrderNo, billItem.getBusinessOrderNo());
        } else {
            return null;
        }

        return payOrderMapper.selectOne(wrapper.last("LIMIT 1"));
    }

    /**
     * 检查渠道缺单
     *
     * @param batchNo 对账批次号
     * @param channelCode 渠道编码
     * @param billDate 账单日期
     * @param dto 对账请求
     * @return 差异数量
     */
    private int checkChannelMissing(String batchNo,
                                    String channelCode,
                                    LocalDate billDate,
                                    ReconcileRequestDTO dto) {
        Set<String> billPayNoSet = dto.getBillItems()
                .stream()
                .map(ChannelBillItemDTO::getPayNo)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());

        LocalDateTime startTime = billDate.atStartOfDay();
        LocalDateTime endTime = billDate.plusDays(1).atStartOfDay();

        java.util.List<PayOrder> successOrders = payOrderMapper.selectList(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getChannelCode, channelCode)
                .eq(PayOrder::getPayStatus, PayStatusEnum.SUCCESS.getCode())
                .ge(PayOrder::getSuccessTime, startTime)
                .lt(PayOrder::getSuccessTime, endTime));

        int count = 0;
        for (PayOrder payOrder : successOrders) {
            if (!billPayNoSet.contains(payOrder.getPayNo())) {
                saveDiff(batchNo, payOrder, null, ReconcileDiffTypeEnum.CHANNEL_MISSING, "平台成功支付单未出现在渠道账单");
                count++;
            }
        }
        return count;
    }

    /**
     * 保存对账差异
     *
     * @param batchNo 对账批次号
     * @param payOrder 平台支付单
     * @param billItem 渠道账单明细
     * @param diffTypeEnum 差异类型
     * @param remark 备注
     */
    private void saveDiff(String batchNo,
                          PayOrder payOrder,
                          ChannelBillItemDTO billItem,
                          ReconcileDiffTypeEnum diffTypeEnum,
                          String remark) {
        PayReconcileDiff diff = new PayReconcileDiff();
        diff.setReconcileBatchNo(batchNo);
        diff.setPayNo(payOrder != null ? payOrder.getPayNo() : billItem.getPayNo());
        diff.setBusinessOrderNo(payOrder != null ? payOrder.getBusinessOrderNo() : billItem.getBusinessOrderNo());
        diff.setChannelCode(payOrder != null ? payOrder.getChannelCode() : billItem.getChannelCode());
        diff.setChannelTradeNo(payOrder != null ? payOrder.getChannelTradeNo() : billItem.getChannelTradeNo());
        diff.setDiffType(diffTypeEnum.getCode());
        diff.setPlatformAmount(payOrder != null ? payOrder.getPayAmount() : null);
        diff.setChannelAmount(billItem != null ? billItem.getPayAmount() : null);
        diff.setPlatformStatus(payOrder != null ? payOrder.getPayStatus() : null);
        diff.setChannelStatus(billItem != null ? billItem.getPayStatus() : null);
        diff.setHandleStatus("WAITING");
        diff.setHandleRemark(remark);

        try {
            payReconcileDiffMapper.insert(diff);
        } catch (DuplicateKeyException e) {
            log.warn("对账差异已存在，跳过重复落库，batchNo={}，payNo={}，diffType={}",
                    batchNo, diff.getPayNo(), diffTypeEnum.getCode());
        }
    }

    /**
     * 构建对账批次号
     *
     * @param channelCode 渠道编码
     * @param billDate 账单日期
     * @return 对账批次号
     */
    private String buildBatchNo(String channelCode, LocalDate billDate) {
        return "REC"
                + channelCode
                + DateUtil.format(DateUtil.date(billDate), "yyyyMMdd")
                + IdUtil.getSnowflakeNextIdStr();
    }
}
```

### 差异补偿入口

差异补偿不能对所有差异自动处理。建议先只开放 `STATUS_NOT_MATCH` 的自动修复，并且要求渠道状态为 `SUCCESS`。

自动补偿流程如下：

```text
查询对账差异
-> 判断差异类型是否为 STATUS_NOT_MATCH
-> 判断渠道状态是否为 SUCCESS
-> 调用支付主动查询补偿
-> 补偿成功后更新差异状态为 AUTO_FIXED
```

模拟对账请求示例：

```bash
curl -X POST 'http://localhost:8080/api/pay/reconcile/mock' \
  -H 'Content-Type: application/json' \
  -d '{
    "channelCode": "MOCK",
    "billDate": "2026-05-15",
    "billItems": [
      {
        "billDate": "2026-05-15",
        "channelCode": "MOCK",
        "payNo": "PAY202605151030321831202844829540358",
        "businessOrderNo": "ORDER202605150001",
        "channelTradeNo": "MOCK_TRADE_1831202888888888888",
        "payAmount": 99.90,
        "payStatus": "SUCCESS"
      },
      {
        "billDate": "2026-05-15",
        "channelCode": "MOCK",
        "payNo": "PAY202605151030321831202844829999",
        "businessOrderNo": "ORDER202605150999",
        "channelTradeNo": "MOCK_TRADE_1831202999999999999",
        "payAmount": 199.90,
        "payStatus": "SUCCESS"
      }
    ]
  }'
```

查询差异数据：

```sql
SELECT id,
       reconcile_batch_no,
       pay_no,
       business_order_no,
       diff_type,
       platform_amount,
       channel_amount,
       platform_status,
       channel_status,
       handle_status,
       handle_remark
FROM pay_reconcile_diff
ORDER BY id DESC;
```

自动修复差异：

```bash
curl -X POST 'http://localhost:8080/api/pay/reconcile/diff/1/fix'
```

对账差异处理建议如下：

| 差异类型                      | 是否自动处理 | 处理建议                                       |
| ----------------------------- | ------------ | ---------------------------------------------- |
| `STATUS_NOT_MATCH` 且渠道成功 | 可以         | 调用主动查单补偿，更新支付单并补发支付成功事件 |
| `AMOUNT_NOT_MATCH`            | 不建议       | 进入人工核查，避免错误入账                     |
| `PLATFORM_MISSING`            | 不建议       | 核查是否漏单、串单或非法支付                   |
| `CHANNEL_MISSING`             | 不建议       | 核查渠道账单下载范围、渠道延迟或平台误更新     |

这一节完成后，支付系统已经具备两类关键兜底能力：短周期的主动查单补偿，以及日终级别的对账差异识别和自动补偿入口。

## 定时任务设计

支付定时任务主要用于兜底处理。回调链路解决实时性，定时任务解决可靠性。根据 README 中“定时查询未支付订单、对账补偿异常数据、支付通知丢失、对账差异处理”等难点，本案例设计三类 XXL-JOB 任务：未支付单主动查单、对账任务、异常回调重试。

建议任务拆分如下：

| 任务             | Handler 名称                 | 建议频率     | 作用                               |
| ---------------- | ---------------------------- | ------------ | ---------------------------------- |
| 扫描未支付支付单 | `payCompensateJobHandler`    | 每 1~5 分钟  | 查询长时间待支付的支付单并主动补偿 |
| 执行对账任务     | `payReconcileJobHandler`     | 每日凌晨     | 下载或模拟导入渠道账单并生成差异   |
| 重试异常回调     | `payCallbackRetryJobHandler` | 每 5~10 分钟 | 重试因系统异常失败的回调           |

先引入 XXL-JOB 依赖。

文件位置：`pom.xml`

```xml
<!-- XXL-JOB 执行器依赖，用于注册支付补偿和对账任务 -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.1</version>
</dependency>
```

添加执行器配置。

文件位置：`src/main/resources/application.yml`

```yaml
xxl:
  job:
    admin:
      # XXL-JOB 调度中心地址，多个地址用英文逗号分隔
      addresses: http://127.0.0.1:8088/xxl-job-admin
    accessToken: default_token
    executor:
      # 执行器名称，需要和 XXL-JOB 控制台配置一致
      appname: payment-demo
      # 执行器注册地址，通常为空表示自动注册
      address:
      # 当前服务 IP，容器部署时建议显式配置
      ip:
      # 执行器端口，避免和 Web 端口冲突
      port: 9999
      # 执行日志路径
      logpath: ./logs/xxl-job/payment-demo
      # 执行日志保留天数
      logretentiondays: 30
```

### XXL-JOB 扫描未支付支付单

扫描未支付支付单任务用于定时补偿 `WAITING` 状态支付单。任务只负责调度入口，核心逻辑复用上一节的 `PayCompensateService`，不要在 Job Handler 里直接写复杂业务逻辑。

XXL-JOB 任务参数建议传入单次扫描数量，例如：

```text
100
```

如果不传参数，默认扫描 100 条。

文件位置：`src/main/java/io/github/atengk/payment/job/PayCompensateJobHandler.java`

```java
package io.github.atengk.payment.job;

import cn.hutool.core.convert.Convert;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.context.XxlJobHelper;
import io.github.atengk.payment.service.PayCompensateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付主动查询补偿任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayCompensateJobHandler {

    private final PayCompensateService payCompensateService;

    /**
     * 扫描并补偿未支付支付单
     */
    @XxlJob("payCompensateJobHandler")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();
        Integer limit = Convert.toInt(jobParam, 100);

        log.info("开始执行支付主动查询补偿任务，limit={}", limit);
        int handleCount = payCompensateService.compensateWaitingPayOrders(limit);
        log.info("支付主动查询补偿任务执行完成，handleCount={}", handleCount);

        XxlJobHelper.handleSuccess("支付主动查询补偿完成，处理数量：" + handleCount);
    }
}
```

XXL-JOB 控制台配置建议：

| 配置项       | 建议值                    |
| ------------ | ------------------------- |
| JobHandler   | `payCompensateJobHandler` |
| 调度类型     | CRON                      |
| CRON         | `0 */2 * * * ?`           |
| 阻塞处理策略 | 单机串行                  |
| 路由策略     | 第一个                    |
| 失败重试次数 | 1                         |
| 任务参数     | `100`                     |

该任务需要配合分布式锁使用，避免多节点或任务重入导致同一支付单被重复查单。上一节的 `compensateByPayNo` 已经按支付单号加锁。

### XXL-JOB 执行对账任务

对账任务通常每日执行一次，处理前一天的渠道账单。真实项目中这里会调用渠道账单下载接口；本案例为了保持可运行性，使用模拟账单服务生成账单明细，然后复用 `PayReconcileService#reconcile` 生成差异。

先定义一个模拟渠道账单服务。

文件位置：`src/main/java/io/github/atengk/payment/service/MockChannelBillService.java`

```java
package io.github.atengk.payment.service;

import io.github.atengk.payment.domain.dto.ChannelBillItemDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 模拟渠道账单服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface MockChannelBillService {

    /**
     * 下载模拟渠道账单
     *
     * @param channelCode 渠道编码
     * @param billDate 账单日期
     * @return 渠道账单明细
     */
    List<ChannelBillItemDTO> downloadBill(String channelCode, LocalDate billDate);
}
```

模拟账单实现从平台成功支付单中生成账单数据，便于本地验证对账流程。

文件位置：`src/main/java/io/github/atengk/payment/service/impl/MockChannelBillServiceImpl.java`

```java
package io.github.atengk.payment.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.payment.common.enums.PayStatusEnum;
import io.github.atengk.payment.domain.dto.ChannelBillItemDTO;
import io.github.atengk.payment.domain.entity.PayOrder;
import io.github.atengk.payment.mapper.PayOrderMapper;
import io.github.atengk.payment.service.MockChannelBillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 模拟渠道账单服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockChannelBillServiceImpl implements MockChannelBillService {

    private final PayOrderMapper payOrderMapper;

    /**
     * 下载模拟渠道账单
     *
     * @param channelCode 渠道编码
     * @param billDate 账单日期
     * @return 渠道账单明细
     */
    @Override
    public List<ChannelBillItemDTO> downloadBill(String channelCode, LocalDate billDate) {
        LocalDateTime startTime = billDate.atStartOfDay();
        LocalDateTime endTime = billDate.plusDays(1).atStartOfDay();

        List<PayOrder> payOrders = payOrderMapper.selectList(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getChannelCode, channelCode)
                .eq(PayOrder::getPayStatus, PayStatusEnum.SUCCESS.getCode())
                .ge(PayOrder::getSuccessTime, startTime)
                .lt(PayOrder::getSuccessTime, endTime));

        if (CollUtil.isEmpty(payOrders)) {
            log.info("模拟渠道账单为空，channelCode={}，billDate={}", channelCode, billDate);
            return List.of();
        }

        List<ChannelBillItemDTO> billItems = payOrders.stream().map(payOrder -> {
            ChannelBillItemDTO item = new ChannelBillItemDTO();
            item.setBillDate(billDate);
            item.setChannelCode(payOrder.getChannelCode());
            item.setPayNo(payOrder.getPayNo());
            item.setBusinessOrderNo(payOrder.getBusinessOrderNo());
            item.setChannelTradeNo(payOrder.getChannelTradeNo());
            item.setPayAmount(payOrder.getPayAmount());
            item.setPayStatus(PayStatusEnum.SUCCESS.getCode());
            return item;
        }).toList();

        log.info("模拟渠道账单下载完成，channelCode={}，billDate={}，count={}",
                channelCode, billDate, billItems.size());
        return billItems;
    }
}
```

对账任务 Handler 支持传入任务参数。参数格式建议为：

```text
MOCK,2026-05-15
```

只传渠道时，默认对昨天账单进行对账：

```text
MOCK
```

文件位置：`src/main/java/io/github/atengk/payment/job/PayReconcileJobHandler.java`

```java
package io.github.atengk.payment.job;

import cn.hutool.core.util.StrUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.payment.domain.dto.ChannelBillItemDTO;
import io.github.atengk.payment.domain.dto.ReconcileRequestDTO;
import io.github.atengk.payment.service.MockChannelBillService;
import io.github.atengk.payment.service.PayReconcileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 支付对账任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayReconcileJobHandler {

    private final MockChannelBillService mockChannelBillService;
    private final PayReconcileService payReconcileService;

    /**
     * 执行支付对账任务
     */
    @XxlJob("payReconcileJobHandler")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();

        String channelCode = "MOCK";
        LocalDate billDate = LocalDate.now().minusDays(1);

        if (StrUtil.isNotBlank(jobParam)) {
            List<String> params = StrUtil.splitTrim(jobParam, ",");
            if (!params.isEmpty()) {
                channelCode = params.get(0);
            }
            if (params.size() > 1 && StrUtil.isNotBlank(params.get(1))) {
                billDate = LocalDate.parse(params.get(1));
            }
        }

        log.info("开始执行支付对账任务，channelCode={}，billDate={}", channelCode, billDate);

        List<ChannelBillItemDTO> billItems = mockChannelBillService.downloadBill(channelCode, billDate);

        ReconcileRequestDTO requestDTO = new ReconcileRequestDTO();
        requestDTO.setChannelCode(channelCode);
        requestDTO.setBillDate(billDate);
        requestDTO.setBillItems(billItems);

        String batchNo = payReconcileService.reconcile(requestDTO);

        log.info("支付对账任务执行完成，channelCode={}，billDate={}，batchNo={}",
                channelCode, billDate, batchNo);

        XxlJobHelper.handleSuccess("支付对账完成，批次号：" + batchNo);
    }
}
```

XXL-JOB 控制台配置建议：

| 配置项       | 建议值                   |
| ------------ | ------------------------ |
| JobHandler   | `payReconcileJobHandler` |
| 调度类型     | CRON                     |
| CRON         | `0 10 1 * * ?`           |
| 阻塞处理策略 | 单机串行                 |
| 路由策略     | 第一个                   |
| 失败重试次数 | 0                        |
| 任务参数     | `MOCK`                   |

对账任务不建议高频执行。正常场景下按日执行即可。如果渠道提供小时账单，也可以按小时执行，但需要通过批次号和唯一索引避免重复生成差异。

### XXL-JOB 重试异常回调

异常回调重试用于处理“系统异常导致回调未完成”的场景。它不应该重试所有失败回调，例如验签失败、金额不一致、支付单不存在等问题不应该自动重试。

建议只重试这类数据：

| 回调结果          | 是否重试 | 原因                                    |
| ----------------- | -------- | --------------------------------------- |
| `FAILED`          | 是       | 系统异常、数据库异常、MQ 异常等可能恢复 |
| `SIGN_INVALID`    | 否       | 签名错误，重试无意义                    |
| `AMOUNT_INVALID`  | 否       | 金额不一致，需要人工核查                |
| `ORDER_NOT_FOUND` | 通常否   | 可能是非法回调或平台缺单                |
| `DUPLICATE`       | 否       | 已经处理过                              |
| `SUCCESS`         | 否       | 已成功                                  |

为了支持异常回调重试，建议给 `pay_callback_log` 增加重试字段。

文件位置：数据库变更 SQL

```sql
-- 为异常回调重试增加控制字段
ALTER TABLE pay_callback_log
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数' AFTER error_msg,
    ADD COLUMN next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间' AFTER retry_count;

-- 提高异常回调扫描效率
CREATE INDEX idx_callback_retry
ON pay_callback_log (callback_status, next_retry_time, retry_count);
```

定义回调重试服务。

文件位置：`src/main/java/io/github/atengk/payment/service/PayCallbackRetryService.java`

```java
package io.github.atengk.payment.service;

/**
 * 支付回调重试服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayCallbackRetryService {

    /**
     * 重试异常支付回调
     *
     * @param limit 单次重试数量
     * @return 重试数量
     */
    int retryFailedCallbacks(Integer limit);
}
```

异常回调重试实现会读取原始回调报文，再调用 `PayCallbackService#handleCallback` 重新处理。

文件位置：`src/main/java/io/github/atengk/payment/service/impl/PayCallbackRetryServiceImpl.java`

```java
package io.github.atengk.payment.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.payment.common.enums.PayCallbackResultEnum;
import io.github.atengk.payment.domain.entity.PayCallbackLog;
import io.github.atengk.payment.mapper.PayCallbackLogMapper;
import io.github.atengk.payment.service.PayCallbackRetryService;
import io.github.atengk.payment.service.PayCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 支付回调重试服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayCallbackRetryServiceImpl implements PayCallbackRetryService {

    private static final int MAX_RETRY_COUNT = 5;

    private final PayCallbackLogMapper payCallbackLogMapper;
    private final PayCallbackService payCallbackService;

    /**
     * 重试异常支付回调
     *
     * @param limit 单次重试数量
     * @return 重试数量
     */
    @Override
    public int retryFailedCallbacks(Integer limit) {
        int queryLimit = ObjectUtil.defaultIfNull(limit, 50);

        List<PayCallbackLog> callbackLogs = payCallbackLogMapper.selectList(new LambdaQueryWrapper<PayCallbackLog>()
                .eq(PayCallbackLog::getCallbackStatus, PayCallbackResultEnum.FAILED.getCode())
                .lt(PayCallbackLog::getRetryCount, MAX_RETRY_COUNT)
                .and(wrapper -> wrapper.isNull(PayCallbackLog::getNextRetryTime)
                        .or()
                        .le(PayCallbackLog::getNextRetryTime, LocalDateTime.now()))
                .orderByAsc(PayCallbackLog::getCreateTime)
                .last("LIMIT " + queryLimit));

        int retryCount = 0;
        for (PayCallbackLog callbackLog : callbackLogs) {
            retryOne(callbackLog);
            retryCount++;
        }

        log.info("异常支付回调重试完成，scanCount={}，retryCount={}", callbackLogs.size(), retryCount);
        return retryCount;
    }

    /**
     * 重试单条异常回调
     *
     * @param callbackLog 回调日志
     */
    private void retryOne(PayCallbackLog callbackLog) {
        try {
            Map<String, Object> params = JSONUtil.toBean(callbackLog.getRequestBody(), Map.class);
            boolean success = payCallbackService.handleCallback(params);

            if (success) {
                payCallbackLogMapper.update(null, new LambdaUpdateWrapper<PayCallbackLog>()
                        .eq(PayCallbackLog::getId, callbackLog.getId())
                        .set(PayCallbackLog::getRetryCount, Convert.toInt(callbackLog.getRetryCount(), 0) + 1)
                        .set(PayCallbackLog::getNextRetryTime, null)
                        .set(PayCallbackLog::getErrorMsg, "异常回调重试成功"));
                log.info("异常支付回调重试成功，callbackLogId={}，payNo={}",
                        callbackLog.getId(), callbackLog.getPayNo());
                return;
            }

            markRetryFailed(callbackLog, "异常回调重试未成功");
        } catch (Exception e) {
            markRetryFailed(callbackLog, e.getMessage());
            log.error("异常支付回调重试失败，callbackLogId={}，payNo={}",
                    callbackLog.getId(), callbackLog.getPayNo(), e);
        }
    }

    /**
     * 标记重试失败
     *
     * @param callbackLog 回调日志
     * @param errorMsg 异常信息
     */
    private void markRetryFailed(PayCallbackLog callbackLog, String errorMsg) {
        int retryCount = Convert.toInt(callbackLog.getRetryCount(), 0) + 1;
        LocalDateTime nextRetryTime = retryCount >= MAX_RETRY_COUNT
                ? null
                : LocalDateTime.now().plusMinutes(5L * retryCount);

        payCallbackLogMapper.update(null, new LambdaUpdateWrapper<PayCallbackLog>()
                .eq(PayCallbackLog::getId, callbackLog.getId())
                .set(PayCallbackLog::getRetryCount, retryCount)
                .set(PayCallbackLog::getNextRetryTime, nextRetryTime)
                .set(PayCallbackLog::getErrorMsg, errorMsg));
    }
}
```

XXL-JOB Handler 如下。

文件位置：`src/main/java/io/github/atengk/payment/job/PayCallbackRetryJobHandler.java`

```java
package io.github.atengk.payment.job;

import cn.hutool.core.convert.Convert;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.payment.service.PayCallbackRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付异常回调重试任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayCallbackRetryJobHandler {

    private final PayCallbackRetryService payCallbackRetryService;

    /**
     * 重试异常支付回调
     */
    @XxlJob("payCallbackRetryJobHandler")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();
        Integer limit = Convert.toInt(jobParam, 50);

        log.info("开始执行异常支付回调重试任务，limit={}", limit);
        int retryCount = payCallbackRetryService.retryFailedCallbacks(limit);
        log.info("异常支付回调重试任务执行完成，retryCount={}", retryCount);

        XxlJobHelper.handleSuccess("异常支付回调重试完成，重试数量：" + retryCount);
    }
}
```

XXL-JOB 控制台配置建议：

| 配置项       | 建议值                       |
| ------------ | ---------------------------- |
| JobHandler   | `payCallbackRetryJobHandler` |
| 调度类型     | CRON                         |
| CRON         | `0 */5 * * * ?`              |
| 阻塞处理策略 | 单机串行                     |
| 路由策略     | 第一个                       |
| 失败重试次数 | 0                            |
| 任务参数     | `50`                         |

异常回调重试要谨慎，不要把验签失败、金额不一致这类高风险异常自动重放，否则会放大异常数据。

## Redis 与分布式锁

Redis 在本案例中主要用于两类场景：短期幂等标记和分布式锁。分布式锁由 Redisson 实现，避免多节点同时处理同一支付单或同一批对账任务。

### 回调幂等 Key 设计

支付回调最重要的幂等维度是支付单号。第三方支付渠道可能对同一支付单连续发送多次通知，因此回调锁应该以 `payNo` 为粒度。

推荐 Key 设计如下：

| 场景         | Redis Key                                         | 说明                     |
| ------------ | ------------------------------------------------- | ------------------------ |
| 支付回调锁   | `payment:callback:lock:{payNo}`                   | 控制同一支付单回调并发   |
| 订单消费幂等 | `payment:mq:consume:pay-success:{payNo}`          | 控制支付成功消息重复消费 |
| 补偿任务锁   | `payment:compensate:lock`                         | 控制补偿任务全局并发     |
| 对账任务锁   | `payment:reconcile:lock:{channelCode}:{billDate}` | 控制同渠道同日期对账并发 |

补齐 Redis Key 常量。

文件位置：`src/main/java/io/github/atengk/payment/common/constant/PaymentRedisKeyConstant.java`

```java
package io.github.atengk.payment.common.constant;

import cn.hutool.core.util.StrUtil;

/**
 * 支付 Redis Key 常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class PaymentRedisKeyConstant {

    public static final String PAY_CALLBACK_LOCK = "payment:callback:lock:{}";

    public static final String PAY_SUCCESS_CONSUME_KEY = "payment:mq:consume:pay-success:{}";

    public static final String PAY_COMPENSATE_LOCK = "payment:compensate:lock";

    public static final String PAY_RECONCILE_LOCK = "payment:reconcile:lock:{}:{}";

    private PaymentRedisKeyConstant() {
    }

    /**
     * 构建支付回调锁 Key
     *
     * @param payNo 支付单号
     * @return Redis Key
     */
    public static String callbackLockKey(String payNo) {
        return StrUtil.format(PAY_CALLBACK_LOCK, payNo);
    }

    /**
     * 构建支付成功消费幂等 Key
     *
     * @param payNo 支付单号
     * @return Redis Key
     */
    public static String paySuccessConsumeKey(String payNo) {
        return StrUtil.format(PAY_SUCCESS_CONSUME_KEY, payNo);
    }

    /**
     * 构建对账任务锁 Key
     *
     * @param channelCode 渠道编码
     * @param billDate 账单日期
     * @return Redis Key
     */
    public static String reconcileLockKey(String channelCode, String billDate) {
        return StrUtil.format(PAY_RECONCILE_LOCK, channelCode, billDate);
    }
}
```

Key 设计原则：

| 原则         | 说明                                     |
| ------------ | ---------------------------------------- |
| 业务前缀清晰 | 使用 `payment` 作为统一前缀              |
| 粒度不要过粗 | 回调锁按支付单号加锁，不要全局加锁       |
| 粒度不要过细 | 对账锁按渠道和日期加锁，不要按账单行加锁 |
| Key 可读     | 方便线上排查                             |
| 过期可控     | 锁由 Redisson 看门狗或 leaseTime 控制    |

### 支付单处理锁

支付单处理锁用于支付回调和主动查询补偿。两者操作的是同一张支付单，所以应该使用同一把锁：

```text
payment:callback:lock:{payNo}
```

这样可以避免以下并发问题：

```text
支付回调线程正在处理 payNo=PAY001
-> 主动补偿任务同时查到 PAY001 成功
-> 两个线程都尝试更新支付单
-> 两个线程都可能发送支付成功 MQ
```

统一锁后，处理顺序会变成：

```text
线程 A 获取 payment:callback:lock:PAY001
-> 更新支付单
-> 发送支付成功事件
-> 释放锁

线程 B 获取锁后再次查询数据库
-> 发现支付单已成功
-> 不再重复处理
```

推荐封装一个锁执行工具，避免业务代码里到处写 `tryLock` 和 `unlock`。

文件位置：`src/main/java/io/github/atengk/payment/common/util/RedissonLockExecutor.java`

```java
package io.github.atengk.payment.common.util;

import io.github.atengk.payment.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁执行器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockExecutor {

    private final RedissonClient redissonClient;

    /**
     * 加锁并执行任务
     *
     * @param lockKey 锁 Key
     * @param waitSeconds 等待锁时间，单位秒
     * @param leaseSeconds 持锁时间，单位秒
     * @param callable 任务
     * @return 执行结果
     */
    public <T> T execute(String lockKey,
                         long waitSeconds,
                         long leaseSeconds,
                         Callable<T> callable) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取分布式锁失败，lockKey={}", lockKey);
                throw new BizException("当前数据正在处理中，请稍后重试");
            }

            return callable.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("获取分布式锁被中断");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("分布式锁内任务执行异常，lockKey={}", lockKey, e);
            throw new BizException("任务执行失败：" + e.getMessage());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

在回调处理中可以这样使用：

```java
String lockKey = PaymentRedisKeyConstant.callbackLockKey(notifyDTO.getPayNo());

return redissonLockExecutor.execute(lockKey, 3, 10, () -> {
    // 这里执行验签后的支付单查询、金额校验、状态更新、MQ 发送
    return doHandleCallback(notifyDTO, params);
});
```

支付单锁的建议参数：

| 参数           | 建议值   | 说明                           |
| -------------- | -------- | ------------------------------ |
| `waitSeconds`  | 2~3 秒   | 回调接口不能长时间阻塞         |
| `leaseSeconds` | 10~30 秒 | 覆盖一次支付单处理时间         |
| 锁粒度         | `payNo`  | 同一支付单串行，不同支付单并发 |

如果支付回调处理逻辑较复杂，例如还要调用多个外部系统，建议不要在锁内执行远程调用。锁内只做核心状态更新和消息落库，远程业务通过 MQ 异步完成。

### 对账任务锁

对账任务锁用于防止同一个渠道、同一个账单日期被重复对账。对账任务通常数据量较大，如果重复执行，可能产生重复差异数据，也可能给渠道接口或数据库造成压力。

对账锁 Key：

```text
payment:reconcile:lock:{channelCode}:{billDate}
```

将对账任务 Handler 加上对账锁。

文件位置：`src/main/java/io/github/atengk/payment/job/PayReconcileJobHandler.java`

```java
package io.github.atengk.payment.job;

import cn.hutool.core.util.StrUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.payment.common.constant.PaymentRedisKeyConstant;
import io.github.atengk.payment.common.util.RedissonLockExecutor;
import io.github.atengk.payment.domain.dto.ChannelBillItemDTO;
import io.github.atengk.payment.domain.dto.ReconcileRequestDTO;
import io.github.atengk.payment.service.MockChannelBillService;
import io.github.atengk.payment.service.PayReconcileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 支付对账任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayReconcileJobHandler {

    private final MockChannelBillService mockChannelBillService;
    private final PayReconcileService payReconcileService;
    private final RedissonLockExecutor redissonLockExecutor;

    /**
     * 执行支付对账任务
     */
    @XxlJob("payReconcileJobHandler")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();

        String channelCode = "MOCK";
        LocalDate billDate = LocalDate.now().minusDays(1);

        if (StrUtil.isNotBlank(jobParam)) {
            List<String> params = StrUtil.splitTrim(jobParam, ",");
            if (!params.isEmpty()) {
                channelCode = params.get(0);
            }
            if (params.size() > 1 && StrUtil.isNotBlank(params.get(1))) {
                billDate = LocalDate.parse(params.get(1));
            }
        }

        String lockKey = PaymentRedisKeyConstant.reconcileLockKey(channelCode, billDate.toString());
        String finalChannelCode = channelCode;
        LocalDate finalBillDate = billDate;

        String batchNo = redissonLockExecutor.execute(lockKey, 1, 1800, () -> {
            log.info("开始执行支付对账任务，channelCode={}，billDate={}", finalChannelCode, finalBillDate);

            List<ChannelBillItemDTO> billItems = mockChannelBillService.downloadBill(finalChannelCode, finalBillDate);

            ReconcileRequestDTO requestDTO = new ReconcileRequestDTO();
            requestDTO.setChannelCode(finalChannelCode);
            requestDTO.setBillDate(finalBillDate);
            requestDTO.setBillItems(billItems);

            return payReconcileService.reconcile(requestDTO);
        });

        log.info("支付对账任务执行完成，channelCode={}，billDate={}，batchNo={}",
                finalChannelCode, finalBillDate, batchNo);

        XxlJobHelper.handleSuccess("支付对账完成，批次号：" + batchNo);
    }
}
```

对账锁建议：

| 参数           | 建议值      | 说明                                        |
| -------------- | ----------- | ------------------------------------------- |
| `waitSeconds`  | 1 秒        | 对账任务重复触发时直接失败即可              |
| `leaseSeconds` | 1800 秒     | 根据账单量设置，避免任务长时间执行时锁过期  |
| 锁粒度         | 渠道 + 日期 | 同一渠道同一天只允许一个任务执行            |
| 数据库兜底     | 唯一索引    | `pay_reconcile_diff` 已有批次和差异唯一约束 |

Redis 锁只能减少重复执行，不能替代数据库唯一约束。对账差异表仍然需要唯一索引兜底，避免任务重试或服务重启造成重复差异。

## 核心接口清单

本案例的接口只保留支付链路最核心的入口，覆盖创建支付单、接收回调、查询支付状态、触发对账和查询对账差异。这些接口对应 README 中“创建支付单、接收支付回调、定时查询未支付订单、对账补偿异常数据”的核心流程。

### 创建支付单接口

创建支付单接口用于业务订单发起支付。调用方一般是订单系统、充值系统、会员系统或前端应用。

| 项目         | 内容                                                     |
| ------------ | -------------------------------------------------------- |
| 接口地址     | `POST /api/pay/orders`                                   |
| 接口作用     | 根据业务订单创建支付单，并返回支付参数                   |
| 是否需要登录 | 业务系统内部调用可使用服务鉴权；前端调用建议校验用户登录 |
| 幂等维度     | `businessOrderNo`                                        |
| 核心表       | `pay_order`                                              |

请求示例：

```json
{
  "businessOrderNo": "ORDER202605150001",
  "channelCode": "MOCK",
  "payAmount": 99.90,
  "subject": "Java 后端课程购买",
  "description": "支付回调与对账补偿专项案例",
  "expireMinutes": 30
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "payNo": "PAY202605151030321831202844829540352",
    "businessOrderNo": "ORDER202605150001",
    "channelCode": "MOCK",
    "payAmount": 99.90,
    "payStatus": "WAITING",
    "payUrl": "https://mock-pay.example.com/pay?payNo=PAY202605151030321831202844829540352",
    "payParams": {
      "payNo": "PAY202605151030321831202844829540352",
      "businessOrderNo": "ORDER202605150001",
      "payAmount": "99.90",
      "currency": "CNY",
      "subject": "Java 后端课程购买",
      "sign": "mock-sign"
    },
    "expireTime": "2026-05-15T11:00:32"
  }
}
```

核心校验规则：

| 校验项            | 规则                                 |
| ----------------- | ------------------------------------ |
| `businessOrderNo` | 不能为空，并且不能重复生成有效支付单 |
| `channelCode`     | 必须是系统支持的支付渠道             |
| `payAmount`       | 必须大于 `0`，最多保留两位小数       |
| `subject`         | 不能为空                             |
| 重复请求          | 已存在待支付支付单时直接返回原支付单 |

### 支付回调接口

支付回调接口由第三方支付渠道调用。该接口不依赖用户登录，而是依赖渠道签名验签。

| 项目         | 内容                                                 |
| ------------ | ---------------------------------------------------- |
| 接口地址     | `POST /api/pay/callback/{channelCode}/notify`        |
| 示例地址     | `POST /api/pay/callback/MOCK/notify`                 |
| 接口作用     | 接收支付渠道异步通知                                 |
| 是否需要登录 | 不需要                                               |
| 安全控制     | 签名验签、金额校验、状态机控制                       |
| 幂等维度     | `payNo`                                              |
| 核心表       | `pay_order`、`pay_callback_log`、`pay_local_message` |

请求示例：

```json
{
  "payNo": "PAY202605151030321831202844829540352",
  "businessOrderNo": "ORDER202605150001",
  "channelTradeNo": "MOCK_TRADE_1831202888888888888",
  "payAmount": "99.90",
  "payStatus": "SUCCESS",
  "notifyTime": "2026-05-15 11:05:30",
  "nonce": "a8d7c9e1b2f34e9a80c1",
  "sign": "使用PaySignUtil生成的签名"
}
```

响应示例：

```text
SUCCESS
```

处理规则：

| 场景               | 处理结果                          |
| ------------------ | --------------------------------- |
| 验签失败           | 写入回调日志，返回 `FAIL`         |
| 支付单不存在       | 写入回调日志，返回 `FAIL`         |
| 金额不一致         | 写入回调日志，返回 `FAIL`         |
| 重复成功回调       | 写入重复回调日志，返回 `SUCCESS`  |
| 首次成功回调       | 更新支付单，写入本地消息，投递 MQ |
| 成功状态被失败覆盖 | 拒绝状态回退，返回 `SUCCESS`      |

### 支付状态查询接口

支付状态查询接口用于前端轮询支付结果，也可以用于订单系统确认支付单当前状态。

| 项目         | 内容                          |
| ------------ | ----------------------------- |
| 接口地址     | `GET /api/pay/orders/{payNo}` |
| 接口作用     | 查询支付单状态                |
| 是否需要登录 | 建议需要                      |
| 查询维度     | `payNo`                       |
| 核心表       | `pay_order`                   |

请求示例：

```bash
curl 'http://localhost:8080/api/pay/orders/PAY202605151030321831202844829540352'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "payNo": "PAY202605151030321831202844829540352",
    "businessOrderNo": "ORDER202605150001",
    "channelCode": "MOCK",
    "channelTradeNo": "MOCK_TRADE_1831202888888888888",
    "payAmount": 99.90,
    "payStatus": "SUCCESS",
    "successTime": "2026-05-15T11:05:30",
    "expireTime": "2026-05-15T11:00:32"
  }
}
```

### 对账任务触发接口

对账任务触发接口用于手动触发模拟对账。正式环境中通常由 XXL-JOB 调用，不建议暴露给普通用户。

| 项目         | 内容                           |
| ------------ | ------------------------------ |
| 接口地址     | `POST /api/pay/reconcile/mock` |
| 接口作用     | 模拟渠道账单导入并执行对账     |
| 是否需要登录 | 需要管理端权限                 |
| 幂等控制     | 对账批次号 + 差异唯一索引      |
| 核心表       | `pay_reconcile_diff`           |

请求示例：

```json
{
  "channelCode": "MOCK",
  "billDate": "2026-05-15",
  "billItems": [
    {
      "billDate": "2026-05-15",
      "channelCode": "MOCK",
      "payNo": "PAY202605151030321831202844829540352",
      "businessOrderNo": "ORDER202605150001",
      "channelTradeNo": "MOCK_TRADE_1831202888888888888",
      "payAmount": 99.90,
      "payStatus": "SUCCESS"
    }
  ]
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "RECMOCK202605151831203333333333333333"
}
```

### 对账差异查询接口

对账差异查询接口用于财务、运营或技术人员查看账单差异，并辅助人工处理。

| 项目         | 内容                                 |
| ------------ | ------------------------------------ |
| 接口地址     | `GET /api/pay/reconcile/diffs`       |
| 接口作用     | 分页查询对账差异                     |
| 是否需要登录 | 需要管理端权限                       |
| 查询条件     | 批次号、支付单号、差异类型、处理状态 |
| 核心表       | `pay_reconcile_diff`                 |

请求示例：

```bash
curl 'http://localhost:8080/api/pay/reconcile/diffs?batchNo=RECMOCK202605151831203333333333333333&handleStatus=WAITING&pageNum=1&pageSize=10'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "reconcileBatchNo": "RECMOCK202605151831203333333333333333",
        "payNo": "PAY202605151030321831202844829540352",
        "businessOrderNo": "ORDER202605150001",
        "channelCode": "MOCK",
        "diffType": "STATUS_NOT_MATCH",
        "platformAmount": 99.90,
        "channelAmount": 99.90,
        "platformStatus": "WAITING",
        "channelStatus": "SUCCESS",
        "handleStatus": "WAITING",
        "handleRemark": "平台状态和渠道状态不一致"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

补偿入口：

| 接口                                        | 作用                 |
| ------------------------------------------- | -------------------- |
| `POST /api/pay/reconcile/diff/{diffId}/fix` | 自动修复指定对账差异 |

该接口只建议自动处理 `STATUS_NOT_MATCH` 且渠道状态为 `SUCCESS` 的差异。

## 核心代码实现

本节补齐核心实体类、Mapper、查询接口和关键文件归档。前面章节已经展开过支付创建、回调处理、主动补偿、对账补偿、MQ 和 XXL-JOB 的主要业务代码，这里重点补齐可运行项目中缺失的实体层、Mapper 层和接口查询能力。

### 实体类与 Mapper

实体类与数据库表一一对应，使用 MyBatis-Plus 注解声明表名、主键、乐观锁和逻辑删除字段。

支付单实体对应 `pay_order` 表。

文件位置：`src/main/java/io/github/atengk/payment/domain/entity/PayOrder.java`

```java
package io.github.atengk.payment.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("pay_order")
public class PayOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payNo;

    private String businessOrderNo;

    private String channelCode;

    private String channelTradeNo;

    private BigDecimal payAmount;

    private String currency;

    private String payStatus;

    private String subject;

    private String description;

    private LocalDateTime expireTime;

    private LocalDateTime successTime;

    private LocalDateTime closeTime;

    private LocalDateTime notifyTime;

    private Integer notifyCount;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

支付回调日志实体对应 `pay_callback_log` 表。

文件位置：`src/main/java/io/github/atengk/payment/domain/entity/PayCallbackLog.java`

```java
package io.github.atengk.payment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付回调日志实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("pay_callback_log")
public class PayCallbackLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payNo;

    private String businessOrderNo;

    private String channelCode;

    private String channelTradeNo;

    private String callbackStatus;

    private String requestBody;

    private String requestSign;

    private String errorMsg;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime createTime;
}
```

对账差异实体对应 `pay_reconcile_diff` 表。

文件位置：`src/main/java/io/github/atengk/payment/domain/entity/PayReconcileDiff.java`

```java
package io.github.atengk.payment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付对账差异实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("pay_reconcile_diff")
public class PayReconcileDiff {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reconcileBatchNo;

    private String payNo;

    private String businessOrderNo;

    private String channelCode;

    private String channelTradeNo;

    private String diffType;

    private BigDecimal platformAmount;

    private BigDecimal channelAmount;

    private String platformStatus;

    private String channelStatus;

    private String handleStatus;

    private String handleRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

本地消息实体对应 `pay_local_message` 表。

文件位置：`src/main/java/io/github/atengk/payment/domain/entity/PayLocalMessage.java`

```java
package io.github.atengk.payment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付本地消息实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("pay_local_message")
public class PayLocalMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageNo;

    private String businessKey;

    private String exchangeName;

    private String routingKey;

    private String messageBody;

    private String sendStatus;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

Mapper 接口保持简洁，复杂查询优先使用 MyBatis-Plus Wrapper，确实复杂时再写 XML。

文件位置：`src/main/java/io/github/atengk/payment/mapper/PayOrderMapper.java`

```java
package io.github.atengk.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.payment.domain.entity.PayOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付单 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface PayOrderMapper extends BaseMapper<PayOrder> {
}
```

文件位置：`src/main/java/io/github/atengk/payment/mapper/PayCallbackLogMapper.java`

```java
package io.github.atengk.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.payment.domain.entity.PayCallbackLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付回调日志 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface PayCallbackLogMapper extends BaseMapper<PayCallbackLog> {
}
```

文件位置：`src/main/java/io/github/atengk/payment/mapper/PayReconcileDiffMapper.java`

```java
package io.github.atengk.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.payment.domain.entity.PayReconcileDiff;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付对账差异 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface PayReconcileDiffMapper extends BaseMapper<PayReconcileDiff> {
}
```

文件位置：`src/main/java/io/github/atengk/payment/mapper/PayLocalMessageMapper.java`

```java
package io.github.atengk.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.payment.domain.entity.PayLocalMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付本地消息 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface PayLocalMessageMapper extends BaseMapper<PayLocalMessage> {
}
```

如果项目已经开启 MyBatis-Plus Mapper 扫描，可以在启动类添加：

文件位置：`src/main/java/io/github/atengk/payment/PaymentApplication.java`

```java
package io.github.atengk.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 支付案例启动类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@MapperScan("io.github.atengk.payment.mapper")
@SpringBootApplication
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
```

### DTO 与 VO

前面章节已经给出了 `PayCreateDTO`、`PayNotifyDTO`、`ReconcileRequestDTO`、`ChannelBillItemDTO` 和 `PayCreateVO`。这里补齐支付状态查询响应和对账差异查询参数。

支付单查询响应对象如下。

文件位置：`src/main/java/io/github/atengk/payment/domain/vo/PayOrderVO.java`

```java
package io.github.atengk.payment.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单查询响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class PayOrderVO {

    private String payNo;

    private String businessOrderNo;

    private String channelCode;

    private String channelTradeNo;

    private BigDecimal payAmount;

    private String currency;

    private String payStatus;

    private String subject;

    private LocalDateTime expireTime;

    private LocalDateTime successTime;

    private LocalDateTime closeTime;

    private LocalDateTime notifyTime;
}
```

对账差异查询参数如下。

文件位置：`src/main/java/io/github/atengk/payment/domain/dto/ReconcileDiffQueryDTO.java`

```java
package io.github.atengk.payment.domain.dto;

import lombok.Data;

/**
 * 对账差异查询参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ReconcileDiffQueryDTO {

    private String reconcileBatchNo;

    private String payNo;

    private String businessOrderNo;

    private String channelCode;

    private String diffType;

    private String handleStatus;

    private Long pageNum = 1L;

    private Long pageSize = 10L;
}
```

对账差异响应对象如下。

文件位置：`src/main/java/io/github/atengk/payment/domain/vo/ReconcileDiffVO.java`

```java
package io.github.atengk.payment.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账差异响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class ReconcileDiffVO {

    private Long id;

    private String reconcileBatchNo;

    private String payNo;

    private String businessOrderNo;

    private String channelCode;

    private String channelTradeNo;

    private String diffType;

    private BigDecimal platformAmount;

    private BigDecimal channelAmount;

    private String platformStatus;

    private String channelStatus;

    private String handleStatus;

    private String handleRemark;

    private LocalDateTime createTime;
}
```

### 支付服务实现

支付服务主要包含创建支付单和查询支付单两个能力。创建支付单逻辑前面已经完整展开，这里补齐查询接口实现。

文件位置：`src/main/java/io/github/atengk/payment/service/PayOrderService.java`

```java
package io.github.atengk.payment.service;

import io.github.atengk.payment.domain.dto.PayCreateDTO;
import io.github.atengk.payment.domain.vo.PayCreateVO;
import io.github.atengk.payment.domain.vo.PayOrderVO;

/**
 * 支付单服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayOrderService {

    /**
     * 创建支付单
     *
     * @param dto 创建支付单参数
     * @return 创建支付单结果
     */
    PayCreateVO createPayOrder(PayCreateDTO dto);

    /**
     * 查询支付单
     *
     * @param payNo 支付单号
     * @return 支付单信息
     */
    PayOrderVO getPayOrder(String payNo);
}
```

在 `PayOrderServiceImpl` 中追加查询方法即可。

文件位置：`src/main/java/io/github/atengk/payment/service/impl/PayOrderServiceImpl.java`

```java
/**
 * 查询支付单
 *
 * @param payNo 支付单号
 * @return 支付单信息
 */
@Override
public PayOrderVO getPayOrder(String payNo) {
    PayOrder payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrder>()
            .eq(PayOrder::getPayNo, payNo)
            .last("LIMIT 1"));

    if (payOrder == null) {
        throw new BizException("支付单不存在：" + payNo);
    }

    return PayOrderVO.builder()
            .payNo(payOrder.getPayNo())
            .businessOrderNo(payOrder.getBusinessOrderNo())
            .channelCode(payOrder.getChannelCode())
            .channelTradeNo(payOrder.getChannelTradeNo())
            .payAmount(payOrder.getPayAmount())
            .currency(payOrder.getCurrency())
            .payStatus(payOrder.getPayStatus())
            .subject(payOrder.getSubject())
            .expireTime(payOrder.getExpireTime())
            .successTime(payOrder.getSuccessTime())
            .closeTime(payOrder.getCloseTime())
            .notifyTime(payOrder.getNotifyTime())
            .build();
}
```

在 Controller 中补齐查询接口。

文件位置：`src/main/java/io/github/atengk/payment/controller/PayOrderController.java`

```java
/**
 * 查询支付单
 *
 * @param payNo 支付单号
 * @return 支付单信息
 */
@GetMapping("/{payNo}")
public Result<PayOrderVO> getPayOrder(@PathVariable String payNo) {
    return Result.ok(payOrderService.getPayOrder(payNo));
}
```

### 回调服务实现

回调服务实现已在“支付回调处理”章节展开，核心文件如下：

| 文件                            | 作用                                             |
| ------------------------------- | ------------------------------------------------ |
| `PayCallbackController.java`    | 接收第三方支付回调                               |
| `PayCallbackService.java`       | 回调处理服务接口                                 |
| `PayCallbackServiceImpl.java`   | 验签、金额校验、状态更新、回调日志、支付成功事件 |
| `PaySignUtil.java`              | 使用 Hutool 实现签名和验签                       |
| `PayChannelSecretProvider.java` | 获取渠道密钥                                     |

回调服务的核心执行顺序如下：

```text
接收回调参数
-> 转换 PayNotifyDTO
-> Hutool 验签
-> 按 payNo 获取 Redisson 锁
-> 查询支付单
-> 校验金额
-> 校验状态流转
-> 条件更新支付单
-> 首次成功时写入本地消息并发送 MQ
-> 写入回调日志
```

回调服务必须保证两点：

| 要点             | 说明                              |
| ---------------- | --------------------------------- |
| 成功状态不可逆   | `SUCCESS` 不能被失败、关闭覆盖    |
| 成功事件只发一次 | 只有首次成功更新支付单时才发送 MQ |

### 补偿服务实现

补偿服务实现已在“主动查询补偿”章节展开，核心文件如下：

| 文件                            | 作用                                       |
| ------------------------------- | ------------------------------------------ |
| `PayCompensateService.java`     | 主动查询补偿接口                           |
| `PayCompensateServiceImpl.java` | 扫描待支付单、主动查单、成功补偿、关闭补偿 |
| `PayChannelClient.java`         | 支付渠道统一接口                           |
| `PayChannelQueryResult.java`    | 渠道查单响应                               |
| `MockPayClient.java`            | 模拟支付渠道查单实现                       |

补偿服务的核心执行顺序如下：

```text
扫描 WAITING 支付单
-> 按 payNo 加锁
-> 查询平台支付单
-> 调用渠道查单
-> 渠道成功：校验金额并更新 SUCCESS
-> 渠道关闭：更新 CLOSED
-> 渠道待支付：跳过等待下次扫描
```

补偿服务不能绕过回调服务的状态规则。即使是主动查单，也必须遵守 `PayStatusEnum.canChangeTo`。

### 对账服务实现

对账服务实现已在“对账补偿处理”章节展开，这里补齐差异分页查询能力。

文件位置：`src/main/java/io/github/atengk/payment/service/PayReconcileService.java`

```java
package io.github.atengk.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.payment.domain.dto.ReconcileDiffQueryDTO;
import io.github.atengk.payment.domain.dto.ReconcileRequestDTO;
import io.github.atengk.payment.domain.vo.ReconcileDiffVO;

/**
 * 支付对账服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayReconcileService {

    /**
     * 执行对账
     *
     * @param dto 对账请求
     * @return 对账批次号
     */
    String reconcile(ReconcileRequestDTO dto);

    /**
     * 查询对账差异
     *
     * @param dto 查询参数
     * @return 差异分页数据
     */
    Page<ReconcileDiffVO> pageDiffs(ReconcileDiffQueryDTO dto);

    /**
     * 修复指定对账差异
     *
     * @param diffId 差异ID
     * @return 是否修复成功
     */
    Boolean fixDiff(Long diffId);
}
```

在 `PayReconcileServiceImpl` 中追加分页查询实现。

文件位置：`src/main/java/io/github/atengk/payment/service/impl/PayReconcileServiceImpl.java`

```java
/**
 * 查询对账差异
 *
 * @param dto 查询参数
 * @return 差异分页数据
 */
@Override
public Page<ReconcileDiffVO> pageDiffs(ReconcileDiffQueryDTO dto) {
    LambdaQueryWrapper<PayReconcileDiff> wrapper = new LambdaQueryWrapper<PayReconcileDiff>()
            .eq(StrUtil.isNotBlank(dto.getReconcileBatchNo()), PayReconcileDiff::getReconcileBatchNo, dto.getReconcileBatchNo())
            .eq(StrUtil.isNotBlank(dto.getPayNo()), PayReconcileDiff::getPayNo, dto.getPayNo())
            .eq(StrUtil.isNotBlank(dto.getBusinessOrderNo()), PayReconcileDiff::getBusinessOrderNo, dto.getBusinessOrderNo())
            .eq(StrUtil.isNotBlank(dto.getChannelCode()), PayReconcileDiff::getChannelCode, dto.getChannelCode())
            .eq(StrUtil.isNotBlank(dto.getDiffType()), PayReconcileDiff::getDiffType, dto.getDiffType())
            .eq(StrUtil.isNotBlank(dto.getHandleStatus()), PayReconcileDiff::getHandleStatus, dto.getHandleStatus())
            .orderByDesc(PayReconcileDiff::getId);

    Page<PayReconcileDiff> entityPage = payReconcileDiffMapper.selectPage(
            Page.of(dto.getPageNum(), dto.getPageSize()),
            wrapper
    );

    Page<ReconcileDiffVO> voPage = Page.of(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
    voPage.setRecords(entityPage.getRecords()
            .stream()
            .map(this::convertDiffVO)
            .toList());

    return voPage;
}

/**
 * 转换对账差异响应对象
 *
 * @param diff 对账差异实体
 * @return 对账差异响应
 */
private ReconcileDiffVO convertDiffVO(PayReconcileDiff diff) {
    return ReconcileDiffVO.builder()
            .id(diff.getId())
            .reconcileBatchNo(diff.getReconcileBatchNo())
            .payNo(diff.getPayNo())
            .businessOrderNo(diff.getBusinessOrderNo())
            .channelCode(diff.getChannelCode())
            .channelTradeNo(diff.getChannelTradeNo())
            .diffType(diff.getDiffType())
            .platformAmount(diff.getPlatformAmount())
            .channelAmount(diff.getChannelAmount())
            .platformStatus(diff.getPlatformStatus())
            .channelStatus(diff.getChannelStatus())
            .handleStatus(diff.getHandleStatus())
            .handleRemark(diff.getHandleRemark())
            .createTime(diff.getCreateTime())
            .build();
}
```

Controller 中补齐对账差异查询接口。

文件位置：`src/main/java/io/github/atengk/payment/controller/PayReconcileController.java`

```java
/**
 * 分页查询对账差异
 *
 * @param dto 查询参数
 * @return 差异分页数据
 */
@GetMapping("/diffs")
public Result<Page<ReconcileDiffVO>> pageDiffs(ReconcileDiffQueryDTO dto) {
    return Result.ok(payReconcileService.pageDiffs(dto));
}
```

### MQ 生产者与消费者

MQ 部分已经在“业务订单状态推进”章节展开，核心文件如下：

| 文件                      | 作用                           |
| ------------------------- | ------------------------------ |
| `RabbitMqConfig.java`     | 声明交换机、队列和绑定关系     |
| `PaymentMqConstant.java`  | 定义交换机、队列和路由键       |
| `PaySuccessEvent.java`    | 支付成功事件体                 |
| `PayMessageProducer.java` | 写入本地消息并投递 RabbitMQ    |
| `PaySuccessConsumer.java` | 消费支付成功事件并推进订单状态 |

消息发送采用“本地事务 + 本地消息表 + MQ”的方式：

```text
支付单更新 SUCCESS
-> 插入 pay_local_message
-> 本地事务提交
-> afterCommit 投递 RabbitMQ
-> 投递成功后更新消息状态为 SUCCESS
-> 投递失败后等待定时任务重试
```

如果要补齐本地消息重试任务，可以增加如下接口：

文件位置：`src/main/java/io/github/atengk/payment/service/PayLocalMessageService.java`

```java
package io.github.atengk.payment.service;

/**
 * 支付本地消息服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayLocalMessageService {

    /**
     * 重试发送失败的本地消息
     *
     * @param limit 单次数量
     * @return 重试数量
     */
    int retryFailedMessages(Integer limit);
}
```

本地消息重试实现如下，用于兜底 MQ 投递失败的数据。

文件位置：`src/main/java/io/github/atengk/payment/service/impl/PayLocalMessageServiceImpl.java`

```java
package io.github.atengk.payment.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.payment.domain.entity.PayLocalMessage;
import io.github.atengk.payment.mapper.PayLocalMessageMapper;
import io.github.atengk.payment.mq.producer.PayMessageProducer;
import io.github.atengk.payment.service.PayLocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付本地消息服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayLocalMessageServiceImpl implements PayLocalMessageService {

    private final PayLocalMessageMapper payLocalMessageMapper;
    private final PayMessageProducer payMessageProducer;

    /**
     * 重试发送失败的本地消息
     *
     * @param limit 单次数量
     * @return 重试数量
     */
    @Override
    public int retryFailedMessages(Integer limit) {
        int queryLimit = ObjectUtil.defaultIfNull(limit, 100);

        List<PayLocalMessage> messages = payLocalMessageMapper.selectList(new LambdaQueryWrapper<PayLocalMessage>()
                .in(PayLocalMessage::getSendStatus, List.of("WAITING", "FAILED"))
                .and(wrapper -> wrapper.isNull(PayLocalMessage::getNextRetryTime)
                        .or()
                        .le(PayLocalMessage::getNextRetryTime, LocalDateTime.now()))
                .orderByAsc(PayLocalMessage::getCreateTime)
                .last("LIMIT " + queryLimit));

        for (PayLocalMessage message : messages) {
            payMessageProducer.sendLocalMessage(message);
        }

        log.info("支付本地消息重试完成，count={}", messages.size());
        return messages.size();
    }
}
```

### XXL-JOB 任务处理器

XXL-JOB 任务处理器已在“定时任务设计”章节展开，核心文件如下：

| 文件                              | Handler                      | 作用                       |
| --------------------------------- | ---------------------------- | -------------------------- |
| `PayCompensateJobHandler.java`    | `payCompensateJobHandler`    | 扫描待支付支付单并主动查单 |
| `PayReconcileJobHandler.java`     | `payReconcileJobHandler`     | 执行渠道账单对账           |
| `PayCallbackRetryJobHandler.java` | `payCallbackRetryJobHandler` | 重试系统异常回调           |

补充一个本地消息重试任务，用于重试 MQ 投递失败的数据。

文件位置：`src/main/java/io/github/atengk/payment/job/PayLocalMessageRetryJobHandler.java`

```java
package io.github.atengk.payment.job;

import cn.hutool.core.convert.Convert;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.payment.service.PayLocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付本地消息重试任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayLocalMessageRetryJobHandler {

    private final PayLocalMessageService payLocalMessageService;

    /**
     * 重试支付本地消息
     */
    @XxlJob("payLocalMessageRetryJobHandler")
    public void execute() {
        String jobParam = XxlJobHelper.getJobParam();
        Integer limit = Convert.toInt(jobParam, 100);

        log.info("开始执行支付本地消息重试任务，limit={}", limit);
        int retryCount = payLocalMessageService.retryFailedMessages(limit);
        log.info("支付本地消息重试任务执行完成，retryCount={}", retryCount);

        XxlJobHelper.handleSuccess("支付本地消息重试完成，重试数量：" + retryCount);
    }
}
```

建议 XXL-JOB 控制台新增任务：

| 配置项       | 建议值                           |
| ------------ | -------------------------------- |
| JobHandler   | `payLocalMessageRetryJobHandler` |
| 调度类型     | CRON                             |
| CRON         | `0 */1 * * * ?`                  |
| 阻塞处理策略 | 单机串行                         |
| 路由策略     | 第一个                           |
| 失败重试次数 | 0                                |
| 任务参数     | `100`                            |

核心代码最终归档如下：

```text
controller
├── PayOrderController.java
├── PayCallbackController.java
└── PayReconcileController.java

service
├── PayOrderService.java
├── PayCallbackService.java
├── PayCompensateService.java
├── PayReconcileService.java
├── PayLocalMessageService.java
└── impl
    ├── PayOrderServiceImpl.java
    ├── PayCallbackServiceImpl.java
    ├── PayCompensateServiceImpl.java
    ├── PayReconcileServiceImpl.java
    └── PayLocalMessageServiceImpl.java

domain
├── entity
│   ├── PayOrder.java
│   ├── PayCallbackLog.java
│   ├── PayReconcileDiff.java
│   └── PayLocalMessage.java
├── dto
│   ├── PayCreateDTO.java
│   ├── PayNotifyDTO.java
│   ├── ChannelBillItemDTO.java
│   ├── ReconcileRequestDTO.java
│   └── ReconcileDiffQueryDTO.java
└── vo
    ├── PayCreateVO.java
    ├── PayOrderVO.java
    └── ReconcileDiffVO.java

mapper
├── PayOrderMapper.java
├── PayCallbackLogMapper.java
├── PayReconcileDiffMapper.java
└── PayLocalMessageMapper.java

mq
├── event
│   └── PaySuccessEvent.java
├── producer
│   └── PayMessageProducer.java
└── consumer
    └── PaySuccessConsumer.java

job
├── PayCompensateJobHandler.java
├── PayReconcileJobHandler.java
├── PayCallbackRetryJobHandler.java
└── PayLocalMessageRetryJobHandler.java
```

这一部分完成后，支付回调与对账补偿案例的核心接口、实体、Mapper、服务、MQ 和定时任务入口已经形成闭环。

## 异常处理

支付异常处理不能只返回错误信息，更重要的是保证支付单状态不被错误覆盖、回调请求可追踪、MQ 可补偿、对账差异可人工介入。这对应支付场景中的回调重复、通知丢失、状态不可逆、金额精度和对账差异等核心问题。

建议统一异常返回结构，同时支付回调接口仍按渠道要求返回 `SUCCESS` 或 `FAIL`，不要把内部异常对象直接返回给支付渠道。

先补齐业务异常和全局异常处理。

文件位置：`src/main/java/io/github/atengk/payment/common/exception/BizException.java`

```java
package io.github.atengk.payment.common.exception;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

文件位置：`src/main/java/io/github/atengk/payment/common/result/Result.java`

```java
package io.github.atengk.payment.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用接口返回结果
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
     * @param data 响应数据
     * @return 返回结果
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 失败返回
     *
     * @param message 错误信息
     * @return 返回结果
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
```

文件位置：`src/main/java/io/github/atengk/payment/common/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.payment.common.handler;

import io.github.atengk.payment.common.exception.BizException;
import io.github.atengk.payment.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 返回结果
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务处理异常：{}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    /**
     * 处理请求参数异常
     *
     * @param e 参数异常
     * @return 返回结果
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidException(Exception e) {
        log.warn("请求参数校验失败：{}", e.getMessage());
        return Result.fail("请求参数不合法");
    }

    /**
     * 处理系统异常
     *
     * @param e 系统异常
     * @return 返回结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
```

### 回调验签失败

验签失败说明回调请求可能被篡改、密钥不匹配、参数排序不一致或渠道配置错误。处理策略是：不更新支付单、不发送 MQ、记录回调日志、返回 `FAIL`。

处理逻辑：

```text
接收回调
-> 提取 sign
-> 根据 channelCode 获取密钥
-> 使用 PaySignUtil.verify 验签
-> 验签失败
-> 写入 pay_callback_log，callback_status = SIGN_INVALID
-> 返回 FAIL
```

建议日志：

```text
支付回调验签失败，payNo=PAY202605151030321831202844829540352，channelCode=MOCK
```

数据库验证：

```sql
SELECT pay_no,
       channel_code,
       callback_status,
       request_sign,
       error_msg,
       create_time
FROM pay_callback_log
WHERE callback_status = 'SIGN_INVALID'
ORDER BY id DESC;
```

### 金额不一致

金额不一致属于高风险异常。即使渠道状态是成功，也不能更新平台支付单为成功。

处理策略：

| 动作            | 是否执行 |
| --------------- | -------- |
| 更新支付单状态  | 否       |
| 发送支付成功 MQ | 否       |
| 写入回调日志    | 是       |
| 自动补偿        | 否       |
| 人工核查        | 是       |

处理逻辑：

```text
查询平台支付单
-> 比较 pay_order.pay_amount 和 callback.payAmount
-> 金额不一致
-> 写入 pay_callback_log，callback_status = AMOUNT_INVALID
-> 返回 FAIL
```

金额比较必须使用：

```java
payOrder.getPayAmount().compareTo(notifyDTO.getPayAmount()) == 0
```

不要使用：

```java
payOrder.getPayAmount().equals(notifyDTO.getPayAmount())
```

因为 `99.90` 和 `99.9` 在数值上相等，但 `BigDecimal.equals` 会比较精度。

### 重复回调

第三方支付渠道重复回调是正常现象，不应该按异常处理。只要平台支付单已经处理成功，再次收到同一支付成功通知时，应直接返回 `SUCCESS`。

处理策略：

| 场景                                     | 处理方式                                     |
| ---------------------------------------- | -------------------------------------------- |
| 支付单已是 `SUCCESS`，再次收到 `SUCCESS` | 记录重复回调，返回 `SUCCESS`                 |
| 支付单正在被其他线程处理                 | 获取锁失败时按重复处理中处理，返回 `SUCCESS` |
| MQ 已发送过                              | 不重复发送 MQ                                |
| 订单已推进                               | 消费端按幂等成功处理                         |

重复回调验证 SQL：

```sql
SELECT pay_no,
       callback_status,
       COUNT(*) AS count
FROM pay_callback_log
WHERE pay_no = 'PAY202605151030321831202844829540352'
GROUP BY pay_no, callback_status;
```

预期结果中可能出现：

```text
SUCCESS    1
DUPLICATE  2
```

### 状态回退拦截

支付状态必须遵守不可逆原则。尤其是 `SUCCESS` 不能被后续失败、关闭、超时任务覆盖。

状态回退拦截规则：

| 当前状态  | 新状态    | 是否允许 |
| --------- | --------- | -------- |
| `SUCCESS` | `FAILED`  | 不允许   |
| `SUCCESS` | `CLOSED`  | 不允许   |
| `SUCCESS` | `WAITING` | 不允许   |
| `WAITING` | `SUCCESS` | 允许     |
| `FAILED`  | `SUCCESS` | 允许     |
| `CLOSED`  | `SUCCESS` | 允许     |

推荐日志：

```text
支付回调状态不允许变更，payNo=PAY202605151030321831202844829540352，currentStatus=SUCCESS，targetStatus=CLOSED
```

验证支付单没有被回退：

```sql
SELECT pay_no,
       pay_status,
       success_time,
       close_time,
       update_time
FROM pay_order
WHERE pay_no = 'PAY202605151030321831202844829540352';
```

### MQ 投递失败

MQ 投递失败不能影响支付单成功状态。支付单已经成功时，不能因为 MQ 暂时不可用而回滚支付状态。正确做法是使用本地消息表记录待发送消息，并由定时任务重试。

处理策略：

```text
支付单更新 SUCCESS
-> 写入 pay_local_message，send_status = WAITING
-> 事务提交
-> 投递 RabbitMQ
-> 投递成功：send_status = SUCCESS
-> 投递失败：send_status = FAILED，等待定时任务重试
```

查询失败消息：

```sql
SELECT message_no,
       business_key,
       exchange_name,
       routing_key,
       send_status,
       retry_count,
       next_retry_time,
       error_msg
FROM pay_local_message
WHERE send_status IN ('WAITING', 'FAILED')
ORDER BY create_time ASC;
```

重试任务：

```text
payLocalMessageRetryJobHandler
```

处理原则：

| 问题                           | 处理方式                     |
| ------------------------------ | ---------------------------- |
| RabbitMQ 短暂不可用            | 本地消息表重试               |
| 消息重复投递                   | 消费端幂等                   |
| 消息投递成功但更新本地消息失败 | 后续可能重复投递，消费端兜底 |
| 消费失败                       | RabbitMQ 重试或死信队列处理  |

### 对账差异异常

对账差异不能简单自动修正。支付对账属于资金一致性场景，处理策略必须保守。

差异处理建议：

| 差异类型                      | 自动处理 | 说明                                     |
| ----------------------------- | -------- | ---------------------------------------- |
| `STATUS_NOT_MATCH` 且渠道成功 | 可以     | 调用主动查单确认后补偿                   |
| `AMOUNT_NOT_MATCH`            | 不可以   | 金额异常必须人工核查                     |
| `PLATFORM_MISSING`            | 不可以   | 可能是平台漏单、非法支付或匹配字段异常   |
| `CHANNEL_MISSING`             | 不可以   | 可能是账单延迟、下载范围错误或平台误更新 |

查询待处理差异：

```sql
SELECT id,
       reconcile_batch_no,
       pay_no,
       business_order_no,
       diff_type,
       platform_amount,
       channel_amount,
       platform_status,
       channel_status,
       handle_status,
       handle_remark
FROM pay_reconcile_diff
WHERE handle_status = 'WAITING'
ORDER BY id DESC;
```

## 接口测试与验证

接口测试建议按支付链路顺序执行：先创建支付单，再模拟回调，然后验证重复回调、金额异常、主动补偿和对账补偿。

### 创建支付单测试

请求：

```bash
curl -X POST 'http://localhost:8080/api/pay/orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "businessOrderNo": "ORDER202605150001",
    "channelCode": "MOCK",
    "payAmount": 99.90,
    "subject": "Java 后端课程购买",
    "description": "支付回调与对账补偿专项案例",
    "expireMinutes": 30
  }'
```

预期结果：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "payNo": "PAY202605151030321831202844829540352",
    "businessOrderNo": "ORDER202605150001",
    "channelCode": "MOCK",
    "payAmount": 99.90,
    "payStatus": "WAITING"
  }
}
```

验证数据库：

```sql
SELECT pay_no,
       business_order_no,
       channel_code,
       pay_amount,
       pay_status,
       expire_time,
       create_time
FROM pay_order
WHERE business_order_no = 'ORDER202605150001';
```

预期：

```text
pay_status = WAITING
```

### 模拟支付回调测试

先构造回调参数。签名需要使用 `PaySignUtil.sign` 按相同规则生成。

请求：

```bash
curl -X POST 'http://localhost:8080/api/pay/callback/MOCK/notify' \
  -H 'Content-Type: application/json' \
  -d '{
    "payNo": "PAY202605151030321831202844829540352",
    "businessOrderNo": "ORDER202605150001",
    "channelTradeNo": "MOCK_TRADE_1831202888888888888",
    "payAmount": "99.90",
    "payStatus": "SUCCESS",
    "notifyTime": "2026-05-15 11:05:30",
    "nonce": "a8d7c9e1b2f34e9a80c1",
    "sign": "替换为PaySignUtil生成的签名"
  }'
```

预期响应：

```text
SUCCESS
```

验证支付单：

```sql
SELECT pay_no,
       business_order_no,
       channel_trade_no,
       pay_amount,
       pay_status,
       success_time,
       notify_count
FROM pay_order
WHERE pay_no = 'PAY202605151030321831202844829540352';
```

预期：

```text
pay_status = SUCCESS
notify_count >= 1
channel_trade_no 不为空
```

验证回调日志：

```sql
SELECT pay_no,
       callback_status,
       error_msg,
       create_time
FROM pay_callback_log
WHERE pay_no = 'PAY202605151030321831202844829540352'
ORDER BY id DESC;
```

预期：

```text
callback_status = SUCCESS
```

验证本地消息：

```sql
SELECT message_no,
       business_key,
       routing_key,
       send_status,
       retry_count
FROM pay_local_message
WHERE business_key = 'PAY202605151030321831202844829540352';
```

预期：

```text
send_status = SUCCESS 或 WAITING
```

如果 RabbitMQ 未启动，可能是 `FAILED`，这是正常的，可通过本地消息重试任务补偿。

### 重复回调幂等测试

重复执行相同回调请求：

```bash
curl -X POST 'http://localhost:8080/api/pay/callback/MOCK/notify' \
  -H 'Content-Type: application/json' \
  -d '{
    "payNo": "PAY202605151030321831202844829540352",
    "businessOrderNo": "ORDER202605150001",
    "channelTradeNo": "MOCK_TRADE_1831202888888888888",
    "payAmount": "99.90",
    "payStatus": "SUCCESS",
    "notifyTime": "2026-05-15 11:05:30",
    "nonce": "a8d7c9e1b2f34e9a80c1",
    "sign": "替换为PaySignUtil生成的签名"
  }'
```

预期响应：

```text
SUCCESS
```

验证支付成功消息没有重复生成：

```sql
SELECT business_key,
       routing_key,
       COUNT(*) AS count
FROM pay_local_message
WHERE business_key = 'PAY202605151030321831202844829540352'
GROUP BY business_key, routing_key;
```

预期：

```text
count = 1
```

验证回调日志存在重复记录：

```sql
SELECT callback_status,
       COUNT(*) AS count
FROM pay_callback_log
WHERE pay_no = 'PAY202605151030321831202844829540352'
GROUP BY callback_status;
```

预期：

```text
SUCCESS    1
DUPLICATE  >= 1
```

### 金额不一致测试

构造金额错误的回调：

```bash
curl -X POST 'http://localhost:8080/api/pay/callback/MOCK/notify' \
  -H 'Content-Type: application/json' \
  -d '{
    "payNo": "PAY202605151030321831202844829540352",
    "businessOrderNo": "ORDER202605150001",
    "channelTradeNo": "MOCK_TRADE_AMOUNT_ERROR",
    "payAmount": "100.90",
    "payStatus": "SUCCESS",
    "notifyTime": "2026-05-15 11:10:30",
    "nonce": "amount-error-nonce",
    "sign": "替换为PaySignUtil生成的签名"
  }'
```

预期响应：

```text
FAIL
```

验证回调日志：

```sql
SELECT pay_no,
       callback_status,
       error_msg
FROM pay_callback_log
WHERE pay_no = 'PAY202605151030321831202844829540352'
ORDER BY id DESC
LIMIT 1;
```

预期：

```text
callback_status = AMOUNT_INVALID
```

验证支付单没有被错误金额覆盖：

```sql
SELECT pay_no,
       pay_amount,
       pay_status,
       channel_trade_no
FROM pay_order
WHERE pay_no = 'PAY202605151030321831202844829540352';
```

预期：

```text
pay_amount 仍为 99.90
```

### 主动查询补偿测试

先准备一个长时间处于 `WAITING` 的支付单。可以通过创建支付单后不发回调，或手动调整创建时间：

```sql
UPDATE pay_order
SET create_time = DATE_SUB(NOW(), INTERVAL 10 MINUTE),
    pay_status = 'WAITING'
WHERE pay_no = 'PAY202605151030321831202844829540358';
```

执行补偿接口或 XXL-JOB：

```text
payCompensateJobHandler
```

如果项目提供临时测试接口，也可以调用：

```bash
curl -X POST 'http://localhost:8080/api/pay/compensate/PAY202605151030321831202844829540358'
```

补偿后验证：

```sql
SELECT pay_no,
       pay_status,
       channel_trade_no,
       success_time,
       close_time
FROM pay_order
WHERE pay_no = 'PAY202605151030321831202844829540358';
```

如果模拟渠道返回成功，预期：

```text
pay_status = SUCCESS
channel_trade_no 不为空
success_time 不为空
```

如果模拟渠道返回关闭，预期：

```text
pay_status = CLOSED
close_time 不为空
```

### 对账差异补偿测试

构造渠道账单显示成功，但平台支付单仍是待支付的数据：

```bash
curl -X POST 'http://localhost:8080/api/pay/reconcile/mock' \
  -H 'Content-Type: application/json' \
  -d '{
    "channelCode": "MOCK",
    "billDate": "2026-05-15",
    "billItems": [
      {
        "billDate": "2026-05-15",
        "channelCode": "MOCK",
        "payNo": "PAY202605151030321831202844829540358",
        "businessOrderNo": "ORDER202605150002",
        "channelTradeNo": "MOCK_TRADE_RECONCILE_SUCCESS",
        "payAmount": 99.90,
        "payStatus": "SUCCESS"
      }
    ]
  }'
```

查询差异：

```sql
SELECT id,
       reconcile_batch_no,
       pay_no,
       diff_type,
       platform_status,
       channel_status,
       handle_status
FROM pay_reconcile_diff
WHERE pay_no = 'PAY202605151030321831202844829540358'
ORDER BY id DESC;
```

预期：

```text
diff_type = STATUS_NOT_MATCH
platform_status = WAITING
channel_status = SUCCESS
handle_status = WAITING
```

执行差异修复：

```bash
curl -X POST 'http://localhost:8080/api/pay/reconcile/diff/1/fix'
```

验证差异状态：

```sql
SELECT id,
       pay_no,
       diff_type,
       handle_status,
       handle_remark
FROM pay_reconcile_diff
WHERE id = 1;
```

预期：

```text
handle_status = AUTO_FIXED
```

验证支付单状态：

```sql
SELECT pay_no,
       pay_status,
       success_time
FROM pay_order
WHERE pay_no = 'PAY202605151030321831202844829540358';
```

预期：

```text
pay_status = SUCCESS
```

## 总结

本案例完成了支付回调与对账补偿的核心闭环，重点不是接入某一个真实支付 SDK，而是实现支付类系统最关键的工程能力：幂等、验签、金额校验、状态不可逆、异步消息、主动补偿和对账兜底。

### 核心能力点

本案例覆盖的核心能力如下：

| 能力点       | 实现方式                                   |
| ------------ | ------------------------------------------ |
| 支付单创建   | `businessOrderNo` 唯一约束 + 支付单号生成  |
| 金额精度控制 | `BigDecimal` + 两位小数校验                |
| 回调安全     | Hutool HMAC-SHA256 签名验签                |
| 回调幂等     | Redisson 支付单锁 + 数据库状态判断         |
| 状态不可逆   | `PayStatusEnum.canChangeTo` 控制状态流转   |
| 回调留痕     | `pay_callback_log` 记录每次回调            |
| 订单推进     | 支付成功事件 + RabbitMQ                    |
| MQ 可靠性    | 本地消息表 + 定时重试                      |
| 主动补偿     | 定时扫描 `WAITING` 支付单并查渠道状态      |
| 对账补偿     | 渠道账单和平台支付单匹配，识别差异         |
| 差异处理     | 状态差异可自动补偿，金额和缺单差异人工处理 |

整体链路如下：

```text
创建支付单
-> 调用支付渠道
-> 接收支付回调
-> 验签
-> 校验金额
-> 加锁处理
-> 状态机更新支付单
-> 写入回调日志
-> 写入本地消息
-> 投递 RabbitMQ
-> 订单系统消费
-> 主动查询补偿
-> 日终对账补偿
```

### 可扩展方向

后续可以从以下方向继续增强：

| 方向         | 扩展内容                                    |
| ------------ | ------------------------------------------- |
| 接入真实渠道 | 接入支付宝、微信支付、银联、银行支付        |
| 支付渠道路由 | 根据支付方式、金额、商户配置动态选择渠道    |
| 退款能力     | 增加退款单、退款回调、退款对账              |
| 本地消息增强 | 增加消息 Confirm Callback、死信队列、告警   |
| 对账文件解析 | 支持 CSV、TXT、ZIP 账单下载和解析           |
| 人工差异处理 | 增加差异审核、备注、附件、处理记录          |
| 监控告警     | 回调失败率、金额差异、MQ 堆积、补偿失败告警 |
| 多商户支持   | 增加商户号、应用号、渠道密钥隔离            |
| 安全增强     | 回调 IP 白名单、请求限流、密钥轮换          |
| 交易审计     | 增加支付状态变更流水和操作审计日志          |

如果要继续做成更完整的专项案例，建议下一步优先补充两块内容：退款回调与退款对账、支付状态变更流水。这样支付模块会更接近真实生产系统。