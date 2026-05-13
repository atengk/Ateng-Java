# 消息与事件模型

消息与事件模型用于承载系统内部消息、业务事件、异步通知、状态流转记录和跨模块协作数据。该模型通常用于解决业务操作完成后需要异步处理、延迟处理、通知下游系统、记录事件轨迹或支撑消息重试的问题。

## 消息事件模型

消息事件模型用于统一保存业务系统中产生的消息或事件数据。它既可以作为内部异步任务的持久化载体，也可以作为业务事件追踪、消息补偿、消费状态管理和故障排查的数据基础。

### 适用场景

消息事件模型适合用于业务动作完成后，需要将后续处理从主流程中解耦出来的场景。它强调事件可记录、状态可追踪、失败可重试、结果可回溯。

常见适用场景包括：

| 场景         | 说明                                                 |
| ------------ | ---------------------------------------------------- |
| 业务事件记录 | 记录订单创建、支付成功、退款完成、库存扣减等业务事件 |
| 异步消息处理 | 主业务完成后，将后续通知、同步、计算等操作异步化     |
| 消息补偿重试 | 对发送失败、消费失败、回调失败的消息进行重试         |
| 状态流转追踪 | 保存消息从待处理、处理中、成功、失败到取消的完整状态 |
| 下游系统通知 | 将业务事件通知给搜索、统计、风控、CRM、营销等系统    |
| 定时扫描任务 | 通过调度任务扫描待处理或失败消息，进行补偿处理       |
| 审计与排查   | 通过事件数据定位业务链路中的异常点                   |

该模型更适合系统内部消息或业务事件的持久化管理。如果需要保证本地事务和消息投递强一致，通常应进一步使用 Outbox 事件表模型。

### 建模结构

消息事件模型通常以一张消息事件表作为核心结构，用于保存事件类型、业务标识、消息内容、处理状态、重试次数、处理时间和异常信息。

推荐表名为 `biz_message_event`，表示业务消息事件表。该表不直接表达某个具体业务，而是通过 `event_type`、`biz_type`、`biz_id` 等字段关联具体业务对象。

```sql
CREATE TABLE biz_message_event (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    event_no VARCHAR(64) NOT NULL COMMENT '事件编号',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    biz_id VARCHAR(64) NOT NULL COMMENT '业务ID',
    event_source VARCHAR(64) NOT NULL COMMENT '事件来源',
    event_payload JSON NOT NULL COMMENT '事件内容',
    event_status TINYINT NOT NULL DEFAULT 0 COMMENT '事件状态：0待处理，1处理中，2处理成功，3处理失败，4已取消',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    last_handle_time DATETIME DEFAULT NULL COMMENT '最后处理时间',
    success_time DATETIME DEFAULT NULL COMMENT '处理成功时间',
    fail_reason VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务消息事件表';
```

建模时需要注意，消息事件表不是普通日志表。它不仅保存事件内容，还要承担状态机、重试控制和幂等识别能力。因此表结构中应明确保留事件唯一标识、业务唯一标识、处理状态、重试次数和下一次重试时间。

### 字段设计

字段设计应围绕“事件是什么、属于哪个业务、当前处理到哪里、失败后如何补偿、后续如何排查”这几个问题展开。

| 字段               | 类型              | 必填 | 说明                                            |
| ------------------ | ----------------- | ---- | ----------------------------------------------- |
| `id`               | `BIGINT UNSIGNED` | 是   | 主键ID，建议使用雪花ID或分布式ID                |
| `event_no`         | `VARCHAR(64)`     | 是   | 事件编号，全局唯一，用于幂等和排查              |
| `event_type`       | `VARCHAR(64)`     | 是   | 事件类型，如 `ORDER_CREATED`、`PAY_SUCCESS`     |
| `biz_type`         | `VARCHAR(64)`     | 是   | 业务类型，如 `ORDER`、`PAYMENT`、`REFUND`       |
| `biz_id`           | `VARCHAR(64)`     | 是   | 业务ID，可以是订单ID、支付单ID、退款单ID        |
| `event_source`     | `VARCHAR(64)`     | 是   | 事件来源，如 `order-service`、`payment-service` |
| `event_payload`    | `JSON`            | 是   | 事件内容，保存必要的业务快照                    |
| `event_status`     | `TINYINT`         | 是   | 事件状态                                        |
| `retry_count`      | `INT`             | 是   | 已重试次数                                      |
| `max_retry_count`  | `INT`             | 是   | 最大重试次数                                    |
| `next_retry_time`  | `DATETIME`        | 否   | 下一次允许重试的时间                            |
| `last_handle_time` | `DATETIME`        | 否   | 最后一次处理时间                                |
| `success_time`     | `DATETIME`        | 否   | 成功处理时间                                    |
| `fail_reason`      | `VARCHAR(1000)`   | 否   | 最近一次失败原因                                |
| `trace_id`         | `VARCHAR(128)`    | 否   | 链路追踪ID                                      |
| `remark`           | `VARCHAR(500)`    | 否   | 备注                                            |
| `version`          | `INT`             | 是   | 乐观锁版本号                                    |
| `deleted`          | `TINYINT`         | 是   | 软删除标识                                      |
| `create_time`      | `DATETIME`        | 是   | 创建时间                                        |
| `update_time`      | `DATETIME`        | 是   | 更新时间                                        |

`event_no` 应作为消息事件的全局唯一编号，通常由业务前缀、时间戳、随机序列或雪花ID组成。它用于避免重复写入同一条事件，也方便排查消息链路。

`event_type` 用于区分具体事件语义，不建议使用中文或随意字符串。推荐使用稳定的枚举编码，例如 `ORDER_CREATED`、`ORDER_PAID`、`PAY_SUCCESS`、`REFUND_FINISHED`。

`event_payload` 使用 MySQL 8 的 `JSON` 类型保存事件内容。该字段应保存处理消息所需的最小业务快照，不建议直接保存过大的完整对象。对于查询频繁的属性，应冗余成普通字段，不应依赖 JSON 深层查询作为主要查询方式。

`event_status` 建议采用有限状态流转：

| 状态值 | 状态含义 | 说明                                 |
| ------ | -------- | ------------------------------------ |
| `0`    | 待处理   | 消息已创建，等待处理                 |
| `1`    | 处理中   | 消息已被任务或消费者锁定处理         |
| `2`    | 处理成功 | 消息已完成处理                       |
| `3`    | 处理失败 | 消息处理失败，可根据重试次数继续补偿 |
| `4`    | 已取消   | 消息不再处理                         |

状态流转通常为：

```text
待处理 -> 处理中 -> 处理成功
待处理 -> 处理中 -> 处理失败 -> 待处理
待处理 -> 已取消
处理失败 -> 已取消
```

### 索引设计

索引设计应围绕幂等写入、按状态扫描、按业务查询、按事件类型统计和问题排查展开。消息事件表通常会被定时任务高频扫描，因此状态和重试时间相关索引非常关键。

推荐索引如下：

```sql
ALTER TABLE biz_message_event
    ADD UNIQUE KEY uk_event_no (event_no),
    ADD KEY idx_status_retry_time (event_status, next_retry_time, retry_count),
    ADD KEY idx_biz_type_biz_id (biz_type, biz_id),
    ADD KEY idx_event_type_create_time (event_type, create_time),
    ADD KEY idx_trace_id (trace_id),
    ADD KEY idx_create_time (create_time);
```

`uk_event_no` 用于保证同一事件不会重复写入，是消息事件模型的核心幂等约束。

`idx_status_retry_time` 用于定时任务扫描待处理或到期重试的消息。字段顺序建议以 `event_status` 开头，因为大多数补偿任务都会先按状态过滤，再按 `next_retry_time` 判断是否到达重试时间。

`idx_biz_type_biz_id` 用于根据业务对象查询相关事件，例如查询某个订单关联的所有消息事件。

`idx_event_type_create_time` 用于按事件类型查看一段时间内的消息数量、失败数量和处理情况。

`idx_trace_id` 用于链路追踪和问题排查。如果系统没有统一链路追踪ID，可以不建该索引。

`idx_create_time` 用于归档、清理和按时间范围分页查询。

### 常用查询

常用查询应围绕业务排查、任务扫描、状态统计和失败补偿展开。消息事件表的数据量通常增长较快，查询时应尽量带上状态、时间范围或业务标识，避免无条件全表扫描。

#### 查询待处理消息

该查询用于调度任务拉取待处理消息。为了避免一次性拉取过多数据，应使用 `LIMIT` 控制批量大小。

```sql
SELECT
    id,
    event_no,
    event_type,
    biz_type,
    biz_id,
    event_source,
    event_payload,
    retry_count,
    max_retry_count,
    trace_id,
    create_time
FROM biz_message_event
WHERE deleted = 0
  AND event_status = 0
ORDER BY create_time ASC
LIMIT 100;
```

该查询适合处理新产生的待处理消息。如果系统中失败重试和首次处理使用同一套任务，也可以合并下一节的重试条件。

#### 查询到期可重试消息

该查询用于扫描处理失败但仍未超过最大重试次数的消息。`next_retry_time` 用于控制退避重试时间。

```sql
SELECT
    id,
    event_no,
    event_type,
    biz_type,
    biz_id,
    event_payload,
    retry_count,
    max_retry_count,
    next_retry_time,
    fail_reason
FROM biz_message_event
WHERE deleted = 0
  AND event_status = 3
  AND retry_count < max_retry_count
  AND next_retry_time <= NOW()
ORDER BY next_retry_time ASC
LIMIT 100;
```

该查询应优先命中 `idx_status_retry_time` 索引，适合补偿任务周期性执行。

#### 根据业务对象查询事件

该查询用于查看某个业务对象关联的全部事件，例如查询某个订单的创建、支付、发货、取消等事件轨迹。

```sql
SELECT
    id,
    event_no,
    event_type,
    event_status,
    retry_count,
    max_retry_count,
    last_handle_time,
    success_time,
    fail_reason,
    create_time,
    update_time
FROM biz_message_event
WHERE deleted = 0
  AND biz_type = 'ORDER'
  AND biz_id = 'ORDER202605130001'
ORDER BY create_time ASC;
```

该查询适合后台管理、客服排查和业务链路追踪。

#### 根据事件编号查询详情

该查询用于通过事件编号精确定位一条消息事件，通常用于日志排查、接口幂等校验或人工补偿。

```sql
SELECT
    id,
    event_no,
    event_type,
    biz_type,
    biz_id,
    event_source,
    event_payload,
    event_status,
    retry_count,
    max_retry_count,
    next_retry_time,
    last_handle_time,
    success_time,
    fail_reason,
    trace_id,
    create_time,
    update_time
FROM biz_message_event
WHERE event_no = 'EVT202605130001';
```

该查询应命中 `uk_event_no` 唯一索引。

#### 统计事件处理状态

该查询用于统计某类事件在指定时间范围内的处理情况，适合监控看板和运维排查。

```sql
SELECT
    event_status,
    COUNT(*) AS total_count
FROM biz_message_event
WHERE deleted = 0
  AND event_type = 'ORDER_CREATED'
  AND create_time >= '2026-05-01 00:00:00'
  AND create_time < '2026-06-01 00:00:00'
GROUP BY event_status
ORDER BY event_status ASC;
```

如果该查询频率较高，可以结合统计汇总模型，将每日事件状态统计结果写入汇总表，避免频繁扫描明细数据。

#### 查询失败次数较多的消息

该查询用于定位多次处理失败的消息，方便人工介入或专项修复。

```sql
SELECT
    id,
    event_no,
    event_type,
    biz_type,
    biz_id,
    retry_count,
    max_retry_count,
    next_retry_time,
    fail_reason,
    update_time
FROM biz_message_event
WHERE deleted = 0
  AND event_status = 3
  AND retry_count >= 3
ORDER BY update_time DESC
LIMIT 100;
```

该查询适合后台管理页面或告警巡检任务使用。

### 常用写入

常用写入主要包括创建消息事件、抢占待处理消息、标记处理成功、标记处理失败和取消消息。写入时应重点保证幂等、状态流转正确以及并发处理安全。

#### 创建消息事件

创建消息事件时，应通过 `event_no` 唯一约束保证幂等。业务代码可以先生成稳定的事件编号，再执行插入。

```sql
INSERT INTO biz_message_event (
    id,
    event_no,
    event_type,
    biz_type,
    biz_id,
    event_source,
    event_payload,
    event_status,
    retry_count,
    max_retry_count,
    trace_id,
    create_time,
    update_time
) VALUES (
    100000000000000001,
    'EVT202605130001',
    'ORDER_CREATED',
    'ORDER',
    'ORDER202605130001',
    'order-service',
    JSON_OBJECT(
        'orderId', 'ORDER202605130001',
        'userId', 10001,
        'orderAmount', 199.00,
        'createTime', '2026-05-13 10:00:00'
    ),
    0,
    0,
    3,
    'trace-202605130001',
    NOW(),
    NOW()
);
```

如果业务允许重复调用创建事件接口，可以使用 `INSERT ... ON DUPLICATE KEY UPDATE` 实现幂等写入。

```sql
INSERT INTO biz_message_event (
    id,
    event_no,
    event_type,
    biz_type,
    biz_id,
    event_source,
    event_payload,
    event_status,
    retry_count,
    max_retry_count,
    trace_id,
    create_time,
    update_time
) VALUES (
    100000000000000001,
    'EVT202605130001',
    'ORDER_CREATED',
    'ORDER',
    'ORDER202605130001',
    'order-service',
    JSON_OBJECT('orderId', 'ORDER202605130001'),
    0,
    0,
    3,
    'trace-202605130001',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    update_time = VALUES(update_time);
```

这里不建议在重复写入时覆盖 `event_payload`、`event_status` 或 `retry_count`，避免已经进入处理流程的消息被意外重置。

#### 抢占待处理消息

多实例任务并发处理消息时，需要先将消息从待处理状态更新为处理中状态。可以结合 `event_status` 和 `version` 控制并发抢占。

```sql
UPDATE biz_message_event
SET
    event_status = 1,
    last_handle_time = NOW(),
    version = version + 1,
    update_time = NOW()
WHERE id = 100000000000000001
  AND event_status IN (0, 3)
  AND deleted = 0;
```

业务代码执行该 SQL 后，应检查影响行数。只有影响行数为 `1` 时，才表示当前实例成功抢占该消息。

#### 标记处理成功

消息处理成功后，应将状态更新为成功，并记录成功时间。

```sql
UPDATE biz_message_event
SET
    event_status = 2,
    success_time = NOW(),
    fail_reason = NULL,
    update_time = NOW()
WHERE id = 100000000000000001
  AND event_status = 1
  AND deleted = 0;
```

建议只允许 `处理中` 状态更新为 `处理成功`，避免待处理或已失败消息被错误标记成功。

#### 标记处理失败

消息处理失败后，应增加重试次数，记录失败原因，并计算下一次重试时间。

```sql
UPDATE biz_message_event
SET
    event_status = 3,
    retry_count = retry_count + 1,
    next_retry_time = DATE_ADD(NOW(), INTERVAL 5 MINUTE),
    fail_reason = '调用下游库存服务超时',
    update_time = NOW()
WHERE id = 100000000000000001
  AND event_status = 1
  AND retry_count < max_retry_count
  AND deleted = 0;
```

重试间隔可以根据 `retry_count` 做退避策略，例如第一次 1 分钟、第二次 5 分钟、第三次 30 分钟。复杂退避策略建议在应用层计算后写入 `next_retry_time`。

#### 超过最大重试次数后停止重试

当消息已经达到最大重试次数时，可以保留失败状态并不再进入自动补偿任务，也可以根据业务规则转为取消状态。

```sql
UPDATE biz_message_event
SET
    event_status = 4,
    fail_reason = '超过最大重试次数，自动取消处理',
    update_time = NOW()
WHERE id = 100000000000000001
  AND event_status = 3
  AND retry_count >= max_retry_count
  AND deleted = 0;
```

是否自动取消应根据业务重要性决定。对于支付、退款、库存等关键事件，通常不建议自动取消，而应进入人工处理列表。

### 常见问题

消息事件模型的常见问题主要集中在幂等、并发、数据量、重试策略和事件内容设计上。建模时应提前约束这些问题，否则后期容易出现重复消费、消息堆积和排查困难。

| 问题             | 说明                                  | 建议                                          |
| ---------------- | ------------------------------------- | --------------------------------------------- |
| 消息重复写入     | 同一业务动作重复生成多条事件          | 使用 `event_no` 唯一约束                      |
| 消息重复处理     | 多个任务实例同时处理同一条消息        | 抢占时通过状态条件或版本号控制                |
| 失败消息无限重试 | 下游长期异常导致消息反复处理          | 增加 `retry_count` 和 `max_retry_count`       |
| 查询性能下降     | 消息表数据持续增长                    | 按时间归档，查询必须带状态或时间范围          |
| JSON 内容过大    | 事件内容保存完整业务对象              | 只保存必要快照，大字段放业务表或对象存储      |
| JSON 查询过多    | 频繁从 `event_payload` 中提取字段过滤 | 高频查询字段应冗余为普通列                    |
| 状态流转混乱     | 任意状态都可以互相更新                | 在 SQL 和应用层同时限制状态流转               |
| 人工排查困难     | 缺少链路ID和失败原因                  | 保留 `trace_id`、`fail_reason` 和处理时间字段 |

对于关键业务消息，消费者必须具备幂等处理能力。即使消息事件表已经通过状态控制减少重复处理，也不能假设消息一定只会被处理一次。下游业务表仍应使用业务唯一键、状态判断或幂等记录表进行保护。

对于高并发消息处理，不建议先 `SELECT` 再无条件 `UPDATE`。正确方式是先查询候选消息，再通过带状态条件的 `UPDATE` 抢占，最后根据影响行数判断是否获得处理权。

对于数据量较大的系统，应配合归档数据模型或冷热数据模型，将已成功且超过保留周期的历史消息迁移到归档表中。在线表只保留近期需要查询、补偿和排查的数据。

### 总结

消息事件模型适合用于保存系统内部业务消息和事件数据，核心目标是让事件可追踪、消息可补偿、失败可重试、处理结果可回溯。

建模时应重点关注以下原则：

| 原则     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 事件唯一 | 使用 `event_no` 保证消息事件幂等                             |
| 状态清晰 | 使用有限状态表达处理生命周期                                 |
| 可重试   | 使用 `retry_count`、`max_retry_count` 和 `next_retry_time` 控制补偿 |
| 可排查   | 保存业务标识、事件类型、失败原因和链路追踪ID                 |
| 可扩展   | 使用 `event_payload` 保存事件快照，但避免滥用 JSON 查询      |
| 可归档   | 对历史成功消息按时间归档，避免在线表持续膨胀                 |

该模型可以作为大多数业务系统的通用消息事件基础表。对于需要保证“业务数据写入”和“事件消息写入”处于同一事务边界的场景，应继续使用 Outbox 事件表模型。



## Outbox事件表模型

Outbox 事件表模型用于解决“业务数据写入成功，但消息发送失败”这一类分布式一致性问题。它的核心思想是：业务数据和待发送事件写入同一个本地数据库事务中，事务提交后，再由后台任务或消息发布器异步扫描 Outbox 表并投递到 MQ、事件总线或下游系统。

### 适用场景

Outbox 事件表模型适合用于需要保证本地业务变更和事件记录同时成功的场景。它不直接保证消息一定已经发送到 MQ，但可以保证只要业务事务提交成功，待发送事件就一定被可靠记录下来，后续可以通过扫描、重试和补偿完成投递。

常见适用场景包括：

| 场景               | 说明                                                    |
| ------------------ | ------------------------------------------------------- |
| 订单创建后发送事件 | 创建订单和写入 `ORDER_CREATED` 事件在同一个事务中完成   |
| 支付成功后通知下游 | 支付单状态更新和写入 `PAY_SUCCESS` 事件保持本地事务一致 |
| 库存扣减后同步消息 | 库存流水、库存余额和库存变更事件一起提交                |
| 退款完成后异步通知 | 退款单更新和退款完成事件同时落库                        |
| 领域事件发布       | 在领域模型状态变化后，将领域事件写入 Outbox 表          |
| MQ 临时不可用      | MQ 故障时事件仍然保存在数据库中，恢复后继续投递         |
| 跨系统最终一致性   | 通过事件投递让搜索、统计、风控、营销等下游系统最终同步  |

该模型适用于对可靠性要求高于实时性的场景。如果业务要求事件必须在主流程中同步发送并立即得到下游确认，Outbox 模型并不能直接满足，需要结合同步调用、事务消息或强一致事务方案。

### 建模结构

Outbox 事件表通常围绕“聚合对象、事件类型、事件内容、投递状态、锁定信息、重试控制和失败原因”进行建模。它与普通消息事件表的区别在于，Outbox 表更强调和业务表处于同一个本地事务边界，并服务于后续可靠投递。

推荐表名为 `biz_outbox_event`，表示业务 Outbox 事件表。

```sql
CREATE TABLE biz_outbox_event (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    event_no VARCHAR(64) NOT NULL COMMENT '事件编号',
    aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合类型',
    aggregate_id VARCHAR(64) NOT NULL COMMENT '聚合ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    event_version INT NOT NULL DEFAULT 1 COMMENT '事件版本',
    event_source VARCHAR(64) NOT NULL COMMENT '事件来源',
    publish_channel VARCHAR(32) NOT NULL COMMENT '发布通道：MQ，HTTP，EVENT_BUS',
    topic VARCHAR(128) DEFAULT NULL COMMENT '消息主题',
    routing_key VARCHAR(128) DEFAULT NULL COMMENT '路由键',
    event_payload JSON NOT NULL COMMENT '事件内容',
    event_headers JSON DEFAULT NULL COMMENT '事件头信息',
    publish_status TINYINT NOT NULL DEFAULT 0 COMMENT '发布状态：0待发布，1发布中，2发布成功，3发布失败，4已取消',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry_count INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    locked_by VARCHAR(128) DEFAULT NULL COMMENT '锁定实例',
    lock_until DATETIME DEFAULT NULL COMMENT '锁定过期时间',
    last_publish_time DATETIME DEFAULT NULL COMMENT '最后发布时间',
    published_time DATETIME DEFAULT NULL COMMENT '发布成功时间',
    fail_reason VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务Outbox事件表';
```

该结构中，`aggregate_type` 和 `aggregate_id` 用于描述事件所属的业务聚合对象，例如订单、支付单、退款单、库存单等。`publish_status` 用于描述事件投递生命周期。`locked_by` 和 `lock_until` 用于控制多实例发布器并发扫描时的事件抢占，避免同一条事件被多个实例同时发布。

### 字段设计

字段设计应围绕“事件由哪个业务产生、准备投递到哪里、当前是否已经发布、失败后如何重试、并发扫描如何避免重复发布”展开。

| 字段                | 类型              | 必填 | 说明                                        |
| ------------------- | ----------------- | ---- | ------------------------------------------- |
| `id`                | `BIGINT UNSIGNED` | 是   | 主键ID，建议使用雪花ID或分布式ID            |
| `event_no`          | `VARCHAR(64)`     | 是   | 事件编号，全局唯一，用于幂等和排查          |
| `aggregate_type`    | `VARCHAR(64)`     | 是   | 聚合类型，如 `ORDER`、`PAYMENT`、`REFUND`   |
| `aggregate_id`      | `VARCHAR(64)`     | 是   | 聚合ID，如订单ID、支付单ID                  |
| `event_type`        | `VARCHAR(64)`     | 是   | 事件类型，如 `ORDER_CREATED`、`PAY_SUCCESS` |
| `event_version`     | `INT`             | 是   | 事件版本，用于事件结构升级                  |
| `event_source`      | `VARCHAR(64)`     | 是   | 事件来源服务或模块                          |
| `publish_channel`   | `VARCHAR(32)`     | 是   | 发布通道，如 `MQ`、`HTTP`、`EVENT_BUS`      |
| `topic`             | `VARCHAR(128)`    | 否   | 消息主题、交换机或事件主题                  |
| `routing_key`       | `VARCHAR(128)`    | 否   | 路由键或消息标签                            |
| `event_payload`     | `JSON`            | 是   | 事件内容，保存下游消费所需的业务快照        |
| `event_headers`     | `JSON`            | 否   | 事件头信息，如租户ID、用户ID、请求来源      |
| `publish_status`    | `TINYINT`         | 是   | 发布状态                                    |
| `retry_count`       | `INT`             | 是   | 已重试次数                                  |
| `max_retry_count`   | `INT`             | 是   | 最大重试次数                                |
| `next_retry_time`   | `DATETIME`        | 否   | 下一次允许重试的时间                        |
| `locked_by`         | `VARCHAR(128)`    | 否   | 当前锁定该事件的发布器实例                  |
| `lock_until`        | `DATETIME`        | 否   | 锁定过期时间                                |
| `last_publish_time` | `DATETIME`        | 否   | 最后一次尝试发布时间                        |
| `published_time`    | `DATETIME`        | 否   | 发布成功时间                                |
| `fail_reason`       | `VARCHAR(1000)`   | 否   | 最近一次失败原因                            |
| `trace_id`          | `VARCHAR(128)`    | 否   | 链路追踪ID                                  |
| `remark`            | `VARCHAR(500)`    | 否   | 备注                                        |
| `version`           | `INT`             | 是   | 乐观锁版本号                                |
| `deleted`           | `TINYINT`         | 是   | 软删除标识                                  |
| `create_time`       | `DATETIME`        | 是   | 创建时间                                    |
| `update_time`       | `DATETIME`        | 是   | 更新时间                                    |

`event_no` 是 Outbox 事件的唯一业务编号。它应由业务操作稳定生成，例如 `ORDER_CREATED:{orderId}` 或基于业务单号、事件类型和版本组合生成。这样即使接口重复调用，也不会重复生成多条同义事件。

`aggregate_type` 和 `aggregate_id` 用于把事件和业务对象关联起来。与普通 `biz_type`、`biz_id` 相比，Outbox 模型更强调聚合根的概念，即事件通常由一个明确的业务聚合状态变化产生。

`event_version` 用于兼容事件结构演进。当 `event_payload` 的字段发生变化时，不应直接破坏旧消费者。可以通过事件版本让消费者按版本解析事件内容。

`publish_channel`、`topic` 和 `routing_key` 用于描述事件投递目标。不同系统可以将其映射为 Kafka Topic、RabbitMQ Exchange 与 Routing Key、RocketMQ Topic 与 Tag，或者内部事件总线的事件名称。

`publish_status` 建议采用以下状态：

| 状态值 | 状态含义 | 说明                             |
| ------ | -------- | -------------------------------- |
| `0`    | 待发布   | 事件已随业务事务提交，等待发布   |
| `1`    | 发布中   | 事件已被发布器实例锁定并正在投递 |
| `2`    | 发布成功 | 事件已经成功投递到目标通道       |
| `3`    | 发布失败 | 事件发布失败，可继续重试         |
| `4`    | 已取消   | 事件不再发布                     |

典型状态流转如下：

```text
待发布 -> 发布中 -> 发布成功
待发布 -> 发布中 -> 发布失败 -> 待发布
发布失败 -> 发布中 -> 发布成功
待发布 -> 已取消
发布失败 -> 已取消
发布中 -> 发布失败
```

`locked_by` 和 `lock_until` 用于解决发布器多实例并发扫描问题。发布器抢占事件时写入当前实例标识和锁定过期时间，只有抢占成功的实例才能发布该事件。如果实例宕机，锁定到期后其他实例可以重新抢占。

### 索引设计

索引设计应围绕事件唯一性、待发布扫描、失败重试扫描、锁超时恢复、聚合对象查询和按事件类型排查展开。Outbox 表通常会被后台发布器高频访问，因此状态、重试时间和锁定过期时间相关索引非常关键。

推荐索引如下：

```sql
ALTER TABLE biz_outbox_event
    ADD UNIQUE KEY uk_event_no (event_no),
    ADD KEY idx_status_retry_time (publish_status, next_retry_time, retry_count),
    ADD KEY idx_status_lock_until (publish_status, lock_until),
    ADD KEY idx_aggregate (aggregate_type, aggregate_id),
    ADD KEY idx_event_type_create_time (event_type, create_time),
    ADD KEY idx_topic_create_time (topic, create_time),
    ADD KEY idx_trace_id (trace_id),
    ADD KEY idx_create_time (create_time);
```

`uk_event_no` 用于保证同一业务事件不会重复写入，是 Outbox 模型的核心幂等约束。

`idx_status_retry_time` 用于扫描待发布或到期重试的事件。发布器通常会按 `publish_status` 过滤，再根据 `next_retry_time` 判断是否可以重试。

`idx_status_lock_until` 用于查找锁定超时的事件，适合处理发布器宕机、进程中断或发布过程异常退出后的恢复场景。

`idx_aggregate` 用于根据聚合对象查看事件轨迹，例如查看某个订单生成过哪些事件。

`idx_event_type_create_time` 用于按事件类型和时间范围统计事件数量、失败数量和投递情况。

`idx_topic_create_time` 用于按投递目标排查消息堆积或发布失败问题。

`idx_trace_id` 用于链路追踪。如果系统没有统一链路追踪ID，可以不创建该索引。

`idx_create_time` 用于归档、清理和按创建时间分页查询。

### 常用查询

常用查询主要服务于发布器扫描、失败补偿、锁定恢复、业务排查和运维统计。Outbox 表可能持续增长，查询时应优先带上状态、时间范围、事件类型、聚合对象或投递目标。

#### 查询待发布事件

该查询用于发布器扫描还没有发布的事件。为了避免一次扫描过多数据，应使用 `LIMIT` 控制批量大小。

```sql
SELECT
    id,
    event_no,
    aggregate_type,
    aggregate_id,
    event_type,
    event_version,
    event_source,
    publish_channel,
    topic,
    routing_key,
    event_payload,
    event_headers,
    retry_count,
    max_retry_count,
    trace_id,
    create_time
FROM biz_outbox_event
WHERE deleted = 0
  AND publish_status = 0
ORDER BY create_time ASC
LIMIT 100;
```

该查询适合处理首次发布的事件。如果发布器同时负责失败重试，可以将待发布和失败重试逻辑分开，避免首次事件被大量失败事件阻塞。

#### 查询到期可重试事件

该查询用于扫描发布失败但仍未超过最大重试次数的事件。`next_retry_time` 用于控制退避重试，避免频繁请求 MQ 或下游系统。

```sql
SELECT
    id,
    event_no,
    aggregate_type,
    aggregate_id,
    event_type,
    publish_channel,
    topic,
    routing_key,
    event_payload,
    retry_count,
    max_retry_count,
    next_retry_time,
    fail_reason
FROM biz_outbox_event
WHERE deleted = 0
  AND publish_status = 3
  AND retry_count < max_retry_count
  AND next_retry_time <= NOW()
ORDER BY next_retry_time ASC
LIMIT 100;
```

该查询应命中 `idx_status_retry_time` 索引。对于高并发系统，发布器应分批扫描，并通过抢占更新控制并发处理权。

#### 查询锁定超时事件

该查询用于查找长时间处于发布中状态的事件。通常这类事件是因为发布器进程异常退出、服务重启、网络阻塞或 MQ 客户端超时导致。

```sql
SELECT
    id,
    event_no,
    aggregate_type,
    aggregate_id,
    event_type,
    publish_status,
    locked_by,
    lock_until,
    last_publish_time,
    retry_count,
    fail_reason
FROM biz_outbox_event
WHERE deleted = 0
  AND publish_status = 1
  AND lock_until <= NOW()
ORDER BY lock_until ASC
LIMIT 100;
```

查询到锁定超时事件后，可以将其恢复为失败状态或待发布状态，再由发布器重新处理。

#### 根据聚合对象查询事件

该查询用于查看某个业务对象产生的全部 Outbox 事件，例如查看订单创建、支付成功、取消订单等事件是否已经发布。

```sql
SELECT
    id,
    event_no,
    event_type,
    event_version,
    publish_status,
    retry_count,
    max_retry_count,
    topic,
    routing_key,
    last_publish_time,
    published_time,
    fail_reason,
    create_time,
    update_time
FROM biz_outbox_event
WHERE deleted = 0
  AND aggregate_type = 'ORDER'
  AND aggregate_id = 'ORDER202605130001'
ORDER BY create_time ASC;
```

该查询适合后台管理、业务排查、客服查询和链路追踪。

#### 根据事件编号查询详情

该查询用于通过事件编号精确定位一条 Outbox 事件，通常用于日志排查、幂等检查和人工补偿。

```sql
SELECT
    id,
    event_no,
    aggregate_type,
    aggregate_id,
    event_type,
    event_version,
    event_source,
    publish_channel,
    topic,
    routing_key,
    event_payload,
    event_headers,
    publish_status,
    retry_count,
    max_retry_count,
    next_retry_time,
    locked_by,
    lock_until,
    last_publish_time,
    published_time,
    fail_reason,
    trace_id,
    create_time,
    update_time
FROM biz_outbox_event
WHERE event_no = 'OUTBOX202605130001';
```

该查询应命中 `uk_event_no` 唯一索引，适合在日志中通过事件编号快速定位问题。

#### 统计事件发布状态

该查询用于统计某类事件在指定时间范围内的发布情况，适合运维看板、告警统计和稳定性分析。

```sql
SELECT
    publish_status,
    COUNT(*) AS total_count
FROM biz_outbox_event
WHERE deleted = 0
  AND event_type = 'ORDER_CREATED'
  AND create_time >= '2026-05-01 00:00:00'
  AND create_time < '2026-06-01 00:00:00'
GROUP BY publish_status
ORDER BY publish_status ASC;
```

如果该统计查询访问频率较高，不建议直接长期扫描 Outbox 明细表。可以将统计结果写入统计汇总表，按日或按小时聚合。

#### 查询某个主题的失败事件

该查询用于定位某个 MQ Topic、事件主题或下游通道的失败事件，适合排查某类消息集中发布失败的问题。

```sql
SELECT
    id,
    event_no,
    event_type,
    aggregate_type,
    aggregate_id,
    topic,
    routing_key,
    retry_count,
    max_retry_count,
    next_retry_time,
    fail_reason,
    update_time
FROM biz_outbox_event
WHERE deleted = 0
  AND topic = 'order.event'
  AND publish_status = 3
ORDER BY update_time DESC
LIMIT 100;
```

该查询适合运维巡检、告警联动和人工补偿入口。

### 常用写入

常用写入主要包括在业务事务中写入 Outbox 事件、发布器抢占事件、标记发布成功、标记发布失败、恢复锁定超时事件和取消事件。写入时应重点保证本地事务一致、事件唯一、抢占安全和状态流转可控。

#### 在业务事务中写入 Outbox 事件

业务表更新和 Outbox 事件写入必须放在同一个本地事务中。以下示例表示创建订单后，同事务写入订单创建事件。

```sql
START TRANSACTION;

INSERT INTO biz_order (
    id,
    order_no,
    user_id,
    order_amount,
    order_status,
    create_time,
    update_time
) VALUES (
    100000000000000001,
    'ORDER202605130001',
    10001,
    199.00,
    0,
    NOW(),
    NOW()
);

INSERT INTO biz_outbox_event (
    id,
    event_no,
    aggregate_type,
    aggregate_id,
    event_type,
    event_version,
    event_source,
    publish_channel,
    topic,
    routing_key,
    event_payload,
    event_headers,
    publish_status,
    retry_count,
    max_retry_count,
    trace_id,
    create_time,
    update_time
) VALUES (
    200000000000000001,
    'OUTBOX_ORDER_CREATED_100000000000000001',
    'ORDER',
    '100000000000000001',
    'ORDER_CREATED',
    1,
    'order-service',
    'MQ',
    'order.event',
    'order.created',
    JSON_OBJECT(
        'orderId', 100000000000000001,
        'orderNo', 'ORDER202605130001',
        'userId', 10001,
        'orderAmount', 199.00,
        'orderStatus', 0,
        'createTime', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
    ),
    JSON_OBJECT(
        'tenantId', 'default',
        'source', 'order-service'
    ),
    0,
    0,
    5,
    'trace-202605130001',
    NOW(),
    NOW()
);

COMMIT;
```

该写法的关键点是：只要订单创建事务提交成功，Outbox 事件也一定已经落库。即使此时 MQ 不可用，也不会导致事件丢失。

#### 幂等写入 Outbox 事件

如果业务接口可能被重复调用，可以通过 `event_no` 唯一约束实现幂等写入。

```sql
INSERT INTO biz_outbox_event (
    id,
    event_no,
    aggregate_type,
    aggregate_id,
    event_type,
    event_version,
    event_source,
    publish_channel,
    topic,
    routing_key,
    event_payload,
    publish_status,
    retry_count,
    max_retry_count,
    trace_id,
    create_time,
    update_time
) VALUES (
    200000000000000001,
    'OUTBOX_ORDER_CREATED_100000000000000001',
    'ORDER',
    '100000000000000001',
    'ORDER_CREATED',
    1,
    'order-service',
    'MQ',
    'order.event',
    'order.created',
    JSON_OBJECT('orderId', 100000000000000001),
    0,
    0,
    5,
    'trace-202605130001',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    update_time = VALUES(update_time);
```

重复写入时不建议覆盖 `event_payload`、`publish_status`、`retry_count`、`locked_by` 等字段，避免已经发布或正在发布的事件被错误重置。

#### 抢占待发布事件

发布器在真正投递 MQ 前，应先抢占事件。抢占成功后，当前实例才允许发布该事件。

```sql
UPDATE biz_outbox_event
SET
    publish_status = 1,
    locked_by = 'publisher-01',
    lock_until = DATE_ADD(NOW(), INTERVAL 2 MINUTE),
    last_publish_time = NOW(),
    version = version + 1,
    update_time = NOW()
WHERE id = 200000000000000001
  AND deleted = 0
  AND publish_status IN (0, 3)
  AND (next_retry_time IS NULL OR next_retry_time <= NOW());
```

执行该 SQL 后，应检查影响行数。只有影响行数为 `1` 时，说明当前发布器抢占成功。影响行数为 `0` 时，说明事件可能已经被其他实例抢占、发布成功、取消或尚未到达重试时间。

#### 标记发布成功

事件成功投递到 MQ 或事件总线后，应更新为发布成功状态，并清理锁定信息。

```sql
UPDATE biz_outbox_event
SET
    publish_status = 2,
    published_time = NOW(),
    fail_reason = NULL,
    locked_by = NULL,
    lock_until = NULL,
    update_time = NOW()
WHERE id = 200000000000000001
  AND deleted = 0
  AND publish_status = 1
  AND locked_by = 'publisher-01';
```

建议只允许持有锁的发布器将事件标记为成功，避免其他实例误更新。

#### 标记发布失败

事件投递失败后，应记录失败原因、增加重试次数，并设置下一次重试时间。

```sql
UPDATE biz_outbox_event
SET
    publish_status = 3,
    retry_count = retry_count + 1,
    next_retry_time = DATE_ADD(NOW(), INTERVAL 5 MINUTE),
    fail_reason = '发送MQ消息超时',
    locked_by = NULL,
    lock_until = NULL,
    update_time = NOW()
WHERE id = 200000000000000001
  AND deleted = 0
  AND publish_status = 1
  AND locked_by = 'publisher-01'
  AND retry_count < max_retry_count;
```

重试间隔可以在应用层根据 `retry_count` 计算，例如 1 分钟、5 分钟、30 分钟、2 小时递增。数据库中只保存最终计算后的 `next_retry_time`。

#### 恢复锁定超时事件

当发布器异常退出时，事件可能一直停留在发布中状态。可以通过定时任务将锁定超时的事件恢复为失败状态，等待后续重试。

```sql
UPDATE biz_outbox_event
SET
    publish_status = 3,
    retry_count = retry_count + 1,
    next_retry_time = DATE_ADD(NOW(), INTERVAL 1 MINUTE),
    fail_reason = '发布器锁定超时，等待重新发布',
    locked_by = NULL,
    lock_until = NULL,
    update_time = NOW()
WHERE deleted = 0
  AND publish_status = 1
  AND lock_until <= NOW();
```

该操作应谨慎控制执行频率，避免与正常发布器处理过程冲突。`lock_until` 的设置时间应大于一次正常发布的最长预期耗时。

#### 取消不再需要发布的事件

当业务已经回滚、事件语义失效或人工确认不需要继续发布时，可以将事件设置为已取消。

```sql
UPDATE biz_outbox_event
SET
    publish_status = 4,
    fail_reason = '业务已关闭，取消事件发布',
    locked_by = NULL,
    lock_until = NULL,
    update_time = NOW()
WHERE id = 200000000000000001
  AND deleted = 0
  AND publish_status IN (0, 3);
```

一般不建议取消支付成功、退款成功、库存扣减等关键事件。关键事件应优先通过人工补偿或专项修复完成发布。

### 常见问题

Outbox 事件表模型的常见问题主要集中在重复发布、消费者幂等、扫描性能、事件堆积、锁定超时和事务边界使用不当上。

| 问题                 | 说明                                             | 建议                                         |
| -------------------- | ------------------------------------------------ | -------------------------------------------- |
| 事件重复发布         | 发布器发送成功后更新数据库失败，后续可能再次发送 | 下游消费者必须实现幂等                       |
| 事件重复写入         | 接口重试导致同一业务动作写入多条事件             | 使用 `event_no` 唯一约束                     |
| 事件长期发布中       | 发布器宕机或超时导致状态未恢复                   | 使用 `locked_by` 和 `lock_until`             |
| 失败事件堆积         | MQ 或下游系统长时间不可用                        | 设置重试次数、告警和人工处理入口             |
| 扫描性能下降         | Outbox 表数据持续增长                            | 对成功历史事件归档，扫描查询必须带状态和时间 |
| 事务边界错误         | 业务数据提交后才写 Outbox 事件                   | 业务写入和事件写入必须在同一本地事务内       |
| JSON 过大            | 事件内容保存完整业务对象                         | 只保存必要快照，大对象通过业务ID回查         |
| 事件结构不兼容       | 下游消费者无法解析新字段或缺失字段               | 使用 `event_version` 做兼容处理              |
| 消息顺序不稳定       | 多个事件并发发布导致顺序变化                     | 按聚合对象控制顺序，或在消费者侧按版本处理   |
| 删除历史数据影响排查 | 成功事件过早清理                                 | 设置合理保留周期，并归档后再清理             |

Outbox 模型解决的是“业务数据与事件记录的一致性”，不是“事件只发布一次”。在实际系统中，发布器可能因为网络超时、MQ 返回结果不确定、数据库更新失败等原因导致同一事件被投递多次。因此，下游消费者必须基于 `event_no`、业务唯一键或消费记录表实现幂等。

如果业务要求同一个聚合对象的事件严格按顺序发布，需要额外设计顺序控制。常见方式包括按 `aggregate_type + aggregate_id` 分组串行发布、在事件中增加业务版本号、消费者按版本号处理，或者通过 MQ 的分区键保证同一聚合进入同一队列分区。

对于高频业务事件，不建议让 Outbox 在线表无限增长。发布成功并超过保留周期的事件应迁移到归档表，在线表只保留近期需要发布、重试、排查和补偿的数据。

### 总结

Outbox 事件表模型适合用于解决业务系统中的可靠事件发布问题。它通过“业务数据写入”和“事件数据写入”处于同一个本地事务，避免业务成功但事件丢失的问题。

建模时应重点关注以下原则：

| 原则         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 本地事务一致 | 业务表和 Outbox 事件表必须在同一数据库事务中提交             |
| 事件唯一     | 使用 `event_no` 保证同一业务事件不会重复落库                 |
| 状态清晰     | 使用待发布、发布中、发布成功、发布失败、已取消表达投递生命周期 |
| 并发安全     | 使用 `locked_by` 和 `lock_until` 控制多实例发布器并发抢占    |
| 可重试       | 使用 `retry_count`、`max_retry_count` 和 `next_retry_time` 控制补偿 |
| 可追踪       | 保存聚合对象、事件类型、投递目标、失败原因和链路追踪ID       |
| 消费幂等     | 发布端不能保证绝对只发送一次，消费者必须做幂等               |
| 可归档       | 发布成功的历史事件应按保留周期归档，避免在线表持续膨胀       |

该模型是业务系统实现最终一致性的常用基础模型，尤其适合订单、支付、库存、退款、营销、统计等需要可靠异步事件驱动的业务场景。