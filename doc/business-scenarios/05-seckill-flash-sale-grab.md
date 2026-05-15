# 秒杀 / 抢购 / 抢号核心

本文档基于“秒杀 / 抢购 / 抢号”业务场景展开，核心链路包括活动预热、Redis 库存加载、用户抢购、资格校验、一人一次限制、Redis 原子扣减、MQ 异步创建业务单、抢购结果查询等内容。

## 案例目标

本案例目标是实现一个可落地的高并发抢购核心链路，用最小但完整的工程结构覆盖秒杀场景中的关键能力：防超卖、防重复、异步削峰、消费幂等、失败回补和结果查询。

系统不追求完整商城能力，只聚焦秒杀核心流程：

```text
后台创建秒杀活动
-> 活动开始前预热库存到 Redis
-> 用户提交抢购请求
-> 校验活动时间、用户资格、一人一单
-> Redis Lua 原子扣减库存
-> 写入抢购结果为排队中
-> 发送 MQ 消息异步创建订单
-> 消费端创建秒杀订单
-> 用户查询抢购结果
```

### 实现功能范围

本案例只实现核心功能，不扩展复杂运营后台、支付履约、商品详情、风控画像等外围能力。

| 功能模块       | 实现内容                                               |
| -------------- | ------------------------------------------------------ |
| 活动管理       | 创建秒杀活动、配置开始时间、结束时间、活动库存         |
| 活动预热       | 将 MySQL 中的活动库存加载到 Redis                      |
| 抢购接口       | 用户发起抢购请求，执行时间校验、库存校验、一人一单校验 |
| Redis 原子扣减 | 使用 Lua 脚本保证库存扣减和用户限购标记原子执行        |
| 异步下单       | 抢购成功后投递 RabbitMQ，由消费者异步创建订单          |
| 结果查询       | 用户查询抢购结果：排队中、成功、失败                   |
| 幂等控制       | Redis 限购 Key、数据库唯一索引、MQ 消费记录共同兜底    |
| 库存回补       | 异步下单失败时回补 Redis 库存并更新抢购结果            |

本案例暂不实现以下内容：

| 暂不实现       | 原因                                                    |
| -------------- | ------------------------------------------------------- |
| 支付流程       | 秒杀订单创建后可对接订单支付模块，不属于本案例核心      |
| 完整后台管理   | 本案例以接口和核心服务代码为主                          |
| 多级风控       | 只保留基础限购和限流设计                                |
| 分布式事务组件 | 秒杀链路优先采用 Redis + MQ + 幂等 + 补偿实现最终一致性 |
| 多服务拆分     | 采用单体 Spring Boot 示例，方便直接运行和理解           |

### 核心技术栈

本案例采用 Spring Boot 3 单体工程实现核心链路，技术选型兼顾实战性和可读性。

| 技术              | 用途                                         |
| ----------------- | -------------------------------------------- |
| Spring Boot 3     | 后端基础框架                                 |
| MyBatis-Plus      | 数据库 CRUD、分页、条件构造                  |
| MySQL             | 存储活动、订单、消费记录等持久化数据         |
| Redis             | 缓存活动库存、用户限购标记、抢购结果         |
| Lua               | 实现库存扣减和一人一单的原子操作             |
| Redisson          | 可用于活动预热锁、后台补偿锁、分布式互斥控制 |
| RabbitMQ          | 抢购成功后异步创建订单，削峰填谷             |
| Hutool            | 日期、字符串、JSON、唯一 ID 等工具处理       |
| Lombok            | 简化实体类、DTO、VO、构造器代码              |
| Spring Validation | 请求参数校验                                 |
| Maven             | 项目依赖管理                                 |

推荐的核心设计如下：

```text
用户请求
  |
  v
SeckillController
  |
  v
SeckillService
  |
  v
Redis Lua 原子校验并扣减
  |
  +--> 扣减失败：直接返回失败原因
  |
  +--> 扣减成功：写入抢购结果为处理中
                 |
                 v
              RabbitMQ
                 |
                 v
        SeckillOrderConsumer
                 |
                 v
        创建秒杀订单 + 更新抢购结果
```

Redis 中保存三类关键数据：

```text
seckill:stock:{activityId}
用于保存活动剩余库存

seckill:user:{activityId}:{userId}
用于控制一人一单

seckill:result:{activityId}:{userId}
用于保存用户抢购结果
```

MySQL 中使用唯一索引作为最终兜底：

```text
unique(activity_id, user_id)
```

即使 Redis 或 MQ 出现重复请求、重复投递、重复消费，数据库唯一索引仍能保证同一个用户在同一个活动下只能生成一条秒杀订单。

## 业务流程

本流程基于活动预热、Redis 原子扣减、MQ 异步下单、结果查询四个核心步骤展开，重点解决高并发下的库存扣减、一人一单、异步削峰和结果可查询问题。原 README 中该场景的核心链路也是“活动预热 -> Redis 加载库存 -> 用户请求抢购 -> Redis 原子扣减库存 -> MQ 异步创建业务单 -> 用户查询抢购结果”。

### 活动预热

活动预热用于在秒杀开始前，将数据库中的活动库存提前加载到 Redis，避免秒杀开始后大量请求直接打到 MySQL。

推荐在以下场景触发预热：

| 触发方式     | 适用场景                       |
| ------------ | ------------------------------ |
| 后台手动预热 | 运营配置活动后手动发布         |
| 定时任务预热 | 活动开始前几分钟自动加载       |
| 应用启动预热 | 测试环境或小规模活动           |
| MQ 事件预热  | 活动发布后异步通知秒杀服务预热 |

预热流程如下：

```text
查询活动信息
-> 校验活动状态是否为待开始
-> 校验活动库存是否大于 0
-> 将库存写入 Redis
-> 初始化活动状态缓存
-> 记录预热时间
```

Redis 预热后的核心数据如下：

```text
seckill:stock:{activityId} = 活动库存数量
```

预热时建议加分布式锁，避免多节点重复预热导致库存覆盖异常。

```text
seckill:lock:warmup:{activityId}
```

预热规则：

| 规则                       | 说明                       |
| -------------------------- | -------------------------- |
| 只预热未开始或进行中的活动 | 已结束活动不允许预热       |
| Redis 已存在库存时谨慎覆盖 | 避免活动进行中覆盖真实库存 |
| 预热操作需要记录日志       | 便于排查库存初始化问题     |
| 多节点预热需要加锁         | 防止重复执行               |

### 用户抢购

用户抢购是秒杀系统的核心入口，需要尽量少访问数据库，优先通过 Redis 完成高频判断。

用户请求流程如下：

```text
接收抢购请求
-> 校验活动是否存在
-> 校验当前时间是否在活动时间范围内
-> 校验用户是否已抢购
-> 执行 Lua 脚本原子扣减库存
-> 扣减成功后写入抢购结果为处理中
-> 发送 MQ 下单消息
-> 返回排队中
```

抢购接口不直接创建订单，而是返回排队结果：

```json
{
  "code": 0,
  "message": "抢购请求已受理，请稍后查询结果",
  "data": {
    "activityId": 10001,
    "userId": 20001,
    "result": "PROCESSING"
  }
}
```

Lua 脚本需要同时完成三件事：

```text
校验库存是否充足
校验用户是否已经参与
扣减库存并标记用户已参与
```

这样可以避免以下问题：

| 问题           | 处理方式                 |
| -------------- | ------------------------ |
| 超卖           | Redis Lua 原子扣减库存   |
| 重复抢购       | 用户限购 Key             |
| 高并发写库压力 | 抢购成功后走 MQ 异步下单 |
| 结果不明确     | Redis 保存抢购结果       |

### 异步下单

异步下单用于削峰填谷。抢购接口只负责“资格校验 + Redis 扣库存 + 发送消息”，真正的订单创建由 MQ 消费者完成。

异步下单流程如下：

```text
消费者接收抢购消息
-> 校验消息是否已消费
-> 查询活动信息
-> 查询是否已存在秒杀订单
-> 创建秒杀订单
-> 写入消费记录
-> 更新抢购结果为成功
```

如果订单创建失败，需要回补 Redis 库存，并将用户抢购结果更新为失败。

```text
订单创建失败
-> Redis 库存 + 1
-> 删除用户限购 Key 或保留失败状态
-> 更新抢购结果为失败
-> 记录异常日志
```

是否删除用户限购 Key 需要根据业务决定：

| 策略         | 说明                                         |
| ------------ | -------------------------------------------- |
| 删除限购 Key | 用户可以重新抢购，适合技术失败场景           |
| 保留限购 Key | 用户不能重复提交，适合资格失败、风控失败场景 |
| 写入失败原因 | 推荐做法，便于用户查询和后台排查             |

MQ 消费端必须做幂等，至少需要三层兜底：

```text
MQ 消息唯一 ID
订单唯一索引 activity_id + user_id
消费记录表唯一索引 message_id
```

### 结果查询

抢购接口通常不会同步返回最终订单，而是返回“处理中”。用户通过结果查询接口获取最终状态。

结果查询流程如下：

```text
用户查询抢购结果
-> 查询 Redis 结果 Key
-> 如果 Redis 有结果，直接返回
-> 如果 Redis 无结果，查询 MySQL 秒杀订单
-> 如果订单存在，返回成功
-> 如果订单不存在，返回失败或处理中
```

推荐结果状态如下：

| 状态        | 说明                     |
| ----------- | ------------------------ |
| PROCESSING  | 已进入队列，正在创建订单 |
| SUCCESS     | 抢购成功，订单已创建     |
| FAIL        | 抢购失败                 |
| SOLD_OUT    | 库存不足                 |
| REPEAT      | 用户重复抢购             |
| NOT_STARTED | 活动未开始               |
| ENDED       | 活动已结束               |

抢购结果 Redis Key 建议设置过期时间，例如 24 小时，避免长期占用内存。

```text
seckill:result:{activityId}:{userId} = PROCESSING / SUCCESS / FAIL / SOLD_OUT / REPEAT
```

## 数据库设计

数据库只承担活动配置、订单结果、消费幂等记录等持久化职责。高并发扣库存不直接依赖数据库，而是由 Redis 承担实时扣减，MySQL 作为最终结果存储和一致性兜底。

### 秒杀活动表

秒杀活动表用于保存活动基本信息，包括活动名称、时间范围、活动状态等。

```sql
CREATE TABLE `seckill_activity` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `activity_name` VARCHAR(100) NOT NULL COMMENT '活动名称',
  `start_time` DATETIME NOT NULL COMMENT '活动开始时间',
  `end_time` DATETIME NOT NULL COMMENT '活动结束时间',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '活动状态：0待开始，1进行中，2已结束，3已关闭',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_start_end_time` (`start_time`, `end_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';
```

状态建议：

| 状态值 | 状态名 | 说明                 |
| ------ | ------ | -------------------- |
| 0      | 待开始 | 已创建但未到开始时间 |
| 1      | 进行中 | 当前可抢购           |
| 2      | 已结束 | 活动自然结束         |
| 3      | 已关闭 | 后台手动关闭         |

### 秒杀商品库存表

秒杀商品库存表用于保存活动库存信息。即使 Redis 承担实时扣减，MySQL 仍需要保存活动初始化库存和最终扣减结果。

```sql
CREATE TABLE `seckill_stock` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `sku_id` BIGINT NOT NULL COMMENT '商品或资源ID',
  `total_stock` INT NOT NULL COMMENT '活动总库存',
  `sold_stock` INT NOT NULL DEFAULT 0 COMMENT '已售库存',
  `available_stock` INT NOT NULL COMMENT '可用库存',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_sku` (`activity_id`, `sku_id`),
  KEY `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品库存表';
```

库存字段说明：

| 字段            | 说明                 |
| --------------- | -------------------- |
| total_stock     | 活动初始库存         |
| sold_stock      | 已成功创建订单的库存 |
| available_stock | 数据库侧剩余库存     |
| version         | 数据库乐观锁兜底字段 |

秒杀高峰期不建议每个请求实时更新这张表。推荐由 MQ 消费创建订单成功后更新，或者通过定时任务做库存结果同步。

### 秒杀订单表

秒杀订单表用于保存用户抢购成功后的订单结果。一人一单通过唯一索引兜底。

```sql
CREATE TABLE `seckill_order` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID',
  `sku_id` BIGINT NOT NULL COMMENT '商品或资源ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已关闭',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';
```

关键索引：

| 索引             | 作用                   |
| ---------------- | ---------------------- |
| uk_order_no      | 保证订单号唯一         |
| uk_activity_user | 保证同一活动下一人一单 |
| idx_user_id      | 支持用户订单查询       |
| idx_activity_id  | 支持活动订单统计       |

即使 Redis 已经做了一人一单控制，也必须保留 `uk_activity_user`，防止 MQ 重复消费、缓存异常、接口绕过等情况造成重复订单。

### MQ 消息消费记录表

MQ 消息消费记录表用于记录消费者是否已经处理过某条消息，防止重复消费导致重复创建订单。

```sql
CREATE TABLE `mq_consume_record` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `message_id` VARCHAR(100) NOT NULL COMMENT '消息唯一ID',
  `business_type` VARCHAR(50) NOT NULL COMMENT '业务类型',
  `business_key` VARCHAR(100) NOT NULL COMMENT '业务唯一标识',
  `consume_status` TINYINT NOT NULL DEFAULT 0 COMMENT '消费状态：0处理中，1成功，2失败',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `error_message` VARCHAR(500) DEFAULT NULL COMMENT '异常信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_business_key` (`business_key`),
  KEY `idx_consume_status` (`consume_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消息消费记录表';
```

字段设计建议：

| 字段           | 说明                     |
| -------------- | ------------------------ |
| message_id     | MQ 消息唯一 ID           |
| business_type  | 例如 SECKILL_ORDER       |
| business_key   | 可使用 activityId:userId |
| consume_status | 记录消费状态             |
| retry_count    | 记录重试次数             |
| error_message  | 保存失败原因，方便排查   |

消费逻辑建议：

```text
先插入消费记录
-> 插入成功，说明首次消费
-> 插入失败，说明消息已处理或正在处理
-> 成功创建订单后更新为成功
-> 失败时更新为失败并记录异常
```

## Redis 设计

Redis 是秒杀链路的高并发核心，主要保存库存、用户限购标记和抢购结果。Redis Key 必须具备清晰的业务语义，并设置合理过期时间。

### 活动库存 Key

活动库存 Key 用于保存秒杀活动的实时剩余库存。

```text
seckill:stock:{activityId}
```

示例：

```text
seckill:stock:10001 = 500
```

建议配置：

| 配置项   | 建议值                      |
| -------- | --------------------------- |
| 数据类型 | String                      |
| Value    | 剩余库存数量                |
| 过期时间 | 活动结束后 1 到 24 小时     |
| 写入时机 | 活动预热时                  |
| 修改方式 | 只能通过 Lua 脚本扣减或回补 |

库存 Key 操作规则：

```text
活动预热：SET seckill:stock:{activityId} totalStock
抢购扣减：Lua DECR
失败回补：INCR seckill:stock:{activityId}
活动结束：保留一段时间后自动过期
```

### 用户限购 Key

用户限购 Key 用于控制同一用户在同一活动中只能抢购一次。

```text
seckill:user:{activityId}:{userId}
```

示例：

```text
seckill:user:10001:20001 = 1
```

建议配置：

| 配置项   | 建议值                   |
| -------- | ------------------------ |
| 数据类型 | String                   |
| Value    | 1                        |
| 过期时间 | 活动结束后 1 到 24 小时  |
| 写入时机 | Lua 扣减库存成功时       |
| 删除时机 | 技术失败且允许用户重试时 |

限购 Key 必须和库存扣减放在同一个 Lua 脚本中执行，否则会出现以下风险：

```text
先扣库存，后写限购失败 -> 用户可能重复抢购
先写限购，后扣库存失败 -> 用户未抢到但被限制
```

### 抢购结果 Key

抢购结果 Key 用于保存用户抢购状态，支持前端轮询查询。

```text
seckill:result:{activityId}:{userId}
```

示例：

```text
seckill:result:10001:20001 = PROCESSING
```

建议状态值：

| 状态        | 说明                        |
| ----------- | --------------------------- |
| PROCESSING  | 排队中，MQ 正在异步创建订单 |
| SUCCESS     | 抢购成功                    |
| FAIL        | 抢购失败                    |
| SOLD_OUT    | 库存不足                    |
| REPEAT      | 重复抢购                    |
| NOT_STARTED | 活动未开始                  |
| ENDED       | 活动已结束                  |

结果 Key 建议保存 JSON，便于携带订单号和失败原因。

```json
{
  "status": "SUCCESS",
  "orderNo": "SK20260101000001",
  "message": "抢购成功"
}
```

失败示例：

```json
{
  "status": "SOLD_OUT",
  "orderNo": null,
  "message": "库存不足"
}
```

### Lua 原子扣减脚本

Lua 脚本用于保证“判断库存、判断用户是否已参与、扣减库存、写入用户限购标记”在 Redis 中原子执行。

文件位置：`src/main/resources/lua/seckill_stock.lua`

```lua
-- 秒杀库存扣减脚本
-- KEYS[1]：库存 Key，例如 seckill:stock:10001
-- KEYS[2]：用户限购 Key，例如 seckill:user:10001:20001
-- ARGV[1]：用户限购 Key 过期秒数

local stockKey = KEYS[1]
local userKey = KEYS[2]
local userExpireSeconds = tonumber(ARGV[1])

-- 用户已经抢购过，返回 2
if redis.call('exists', userKey) == 1 then
    return 2
end

-- 库存不存在，返回 3
local stock = redis.call('get', stockKey)
if not stock then
    return 3
end

-- 库存不足，返回 1
if tonumber(stock) <= 0 then
    return 1
end

-- 扣减库存
redis.call('decr', stockKey)

-- 标记用户已参与
redis.call('set', userKey, '1', 'EX', userExpireSeconds)

-- 扣减成功，返回 0
return 0
```

返回码约定：

| 返回码 | 含义         | 接口处理                 |
| ------ | ------------ | ------------------------ |
| 0      | 扣减成功     | 发送 MQ，返回处理中      |
| 1      | 库存不足     | 返回 SOLD_OUT            |
| 2      | 重复抢购     | 返回 REPEAT              |
| 3      | 库存未初始化 | 返回活动未预热或活动异常 |

Lua 脚本只做最高频、最核心的原子判断，不建议在脚本中放复杂业务逻辑，例如用户等级、黑名单、活动渠道等。这些逻辑可以在进入 Lua 前完成，或者通过本地缓存、Redis Set、布隆过滤器等方式提前判断。

## 核心接口设计

本案例只暴露三个核心接口：活动预热、抢购下单、结果查询。接口职责保持单一，秒杀请求不直接同步创建订单，而是通过 Redis 扣减成功后投递 MQ，由消费者异步创建订单。这个设计对应原场景中“Redis 原子扣减库存 -> MQ 异步创建业务单 -> 用户查询抢购结果”的核心链路。

### 活动预热接口

活动预热接口用于在秒杀开始前，将 MySQL 中的活动库存加载到 Redis。该接口通常由后台管理端、定时任务或活动发布流程触发。

| 项目             | 内容                           |
| ---------------- | ------------------------------ |
| 请求地址         | `/api/seckill/warmup`          |
| 请求方式         | `POST`                         |
| 接口作用         | 将活动库存预热到 Redis         |
| 是否需要登录     | 后台接口建议需要管理员权限     |
| 是否允许重复调用 | 允许，但不建议覆盖进行中的库存 |

请求参数：

```json
{
  "activityId": 10001
}
```

响应示例：

```json
{
  "code": 0,
  "message": "活动预热成功",
  "data": true
}
```

调用示例：

```bash
curl -X POST 'http://localhost:8080/api/seckill/warmup' \
  -H 'Content-Type: application/json' \
  -d '{
    "activityId": 10001
  }'
```

### 抢购下单接口

抢购下单接口是用户参与秒杀的核心入口。该接口只负责完成资格校验、Redis 原子扣减、写入抢购结果、发送 MQ 消息，不在接口线程中直接创建订单。

| 项目             | 内容                                 |
| ---------------- | ------------------------------------ |
| 请求地址         | `/api/seckill/purchase`              |
| 请求方式         | `POST`                               |
| 接口作用         | 用户提交抢购请求                     |
| 是否同步创建订单 | 否                                   |
| 成功返回         | `PROCESSING`，表示已进入异步下单队列 |

请求参数：

```json
{
  "activityId": 10001,
  "skuId": 90001,
  "userId": 20001
}
```

生产环境中 `userId` 应该从登录态中获取，例如 Sa-Token、Spring Security Context 或网关注入的用户上下文。这里为了便于压测和接口演示，将 `userId` 放在请求体中。

抢购成功响应：

```json
{
  "code": 0,
  "message": "抢购请求已受理，请稍后查询结果",
  "data": {
    "status": "PROCESSING",
    "orderNo": null,
    "message": "抢购请求已进入队列"
  }
}
```

库存不足响应：

```json
{
  "code": 0,
  "message": "库存不足",
  "data": {
    "status": "SOLD_OUT",
    "orderNo": null,
    "message": "库存不足"
  }
}
```

重复抢购响应：

```json
{
  "code": 0,
  "message": "请勿重复抢购",
  "data": {
    "status": "REPEAT",
    "orderNo": null,
    "message": "请勿重复抢购"
  }
}
```

调用示例：

```bash
curl -X POST 'http://localhost:8080/api/seckill/purchase' \
  -H 'Content-Type: application/json' \
  -d '{
    "activityId": 10001,
    "skuId": 90001,
    "userId": 20001
  }'
```

### 抢购结果查询接口

抢购结果查询接口用于查询用户当前抢购状态。前端可以在提交抢购请求后每隔 1 到 2 秒轮询一次，不建议高频轮询。

| 项目     | 内容                  |
| -------- | --------------------- |
| 请求地址 | `/api/seckill/result` |
| 请求方式 | `GET`                 |
| 接口作用 | 查询用户抢购结果      |
| 优先查询 | Redis 抢购结果 Key    |
| 兜底查询 | MySQL 秒杀订单表      |

请求参数：

| 参数       | 类型 | 必填 | 说明    |
| ---------- | ---- | ---- | ------- |
| activityId | Long | 是   | 活动 ID |
| userId     | Long | 是   | 用户 ID |

成功响应：

```json
{
  "code": 0,
  "message": "查询成功",
  "data": {
    "status": "SUCCESS",
    "orderNo": "SK202605151234560001",
    "message": "抢购成功"
  }
}
```

处理中响应：

```json
{
  "code": 0,
  "message": "查询成功",
  "data": {
    "status": "PROCESSING",
    "orderNo": null,
    "message": "订单创建中"
  }
}
```

调用示例：

```bash
curl 'http://localhost:8080/api/seckill/result?activityId=10001&userId=20001'
```

## 核心代码实现

下面给出核心链路代码。为控制文档篇幅，默认你已经根据前面数据库表创建了 `Entity` 和 `Mapper`，例如 `SeckillActivityMapper`、`SeckillStockMapper`、`SeckillOrderMapper`、`MqConsumeRecordMapper`。这些 Mapper 都可以直接继承 MyBatis-Plus 的 `BaseMapper<T>`。

建议目录结构如下：

```text
src/main/java/io/github/atengk/seckill
├── common
│   └── ApiResult.java
├── config
│   ├── RabbitMqConfig.java
│   └── RedisScriptConfig.java
├── constant
│   └── SeckillConstants.java
├── controller
│   └── SeckillController.java
├── dto
│   ├── SeckillPurchaseRequest.java
│   └── SeckillWarmupRequest.java
├── mq
│   ├── SeckillOrderConsumer.java
│   └── SeckillOrderMessage.java
├── service
│   └── SeckillService.java
├── service.impl
│   └── SeckillServiceImpl.java
└── vo
    └── SeckillResultVO.java

src/main/resources
├── application.yml
└── lua
    └── seckill_stock.lua
```

### Maven 依赖配置

这里配置 Spring Boot Web、Redis、RabbitMQ、MyBatis-Plus、MySQL、Hutool、Validation 和 Lombok。

文件位置：`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <!-- Spring Boot 3 父工程，统一管理 Spring 相关依赖版本 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>seckill-demo</artifactId>
    <version>1.0.0</version>
    <name>seckill-demo</name>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <hutool.version>5.8.29</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 接口能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验，例如 @NotNull -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- RedisTemplate / StringRedisTemplate -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- RabbitMQ 消息队列 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- MyBatis-Plus Spring Boot 3 启动器 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL 驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Hutool 工具类：ID、JSON、字符串、日期等 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化实体、DTO、VO 代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 单元测试，可按需使用 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

### Redis 配置

这里配置 Redis 连接信息，并预留连接池参数。实际生产环境建议使用 Redis Cluster 或哨兵模式，避免单点故障。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: seckill-demo

  datasource:
    # MySQL 连接地址，根据本地环境修改库名、地址和参数
    url: jdbc:mysql://127.0.0.1:3306/seckill_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      # Redis 地址
      host: 127.0.0.1
      port: 6379
      # 如无密码可留空
      password:
      database: 0
      timeout: 3s
      lettuce:
        pool:
          # 最大连接数
          max-active: 32
          # 最大空闲连接
          max-idle: 16
          # 最小空闲连接
          min-idle: 4
          # 获取连接最大等待时间
          max-wait: 3s

mybatis-plus:
  configuration:
    # 开发环境输出 SQL，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 雪花 ID，由代码侧或 MP 自动生成均可
      id-type: assign_id
```

Redis Key 统一放在常量类中，避免各业务类手写字符串。

文件位置：`src/main/java/io/github/atengk/seckill/constant/SeckillConstants.java`

```java
package io.github.atengk.seckill.constant;

import cn.hutool.core.util.StrUtil;

/**
 * 秒杀常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class SeckillConstants {

    private SeckillConstants() {
    }

    public static final String STOCK_KEY = "seckill:stock:{}";
    public static final String USER_KEY = "seckill:user:{}:{}";
    public static final String RESULT_KEY = "seckill:result:{}:{}";
    public static final String WARMUP_LOCK_KEY = "seckill:lock:warmup:{}";

    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order.create";

    /**
     * 结果缓存时间，默认 24 小时
     */
    public static final long RESULT_EXPIRE_SECONDS = 24 * 60 * 60L;

    /**
     * 用户限购标记缓存时间，默认 24 小时
     */
    public static final long USER_LIMIT_EXPIRE_SECONDS = 24 * 60 * 60L;

    /**
     * 构建活动库存 Key
     *
     * @param activityId 活动ID
     * @return Redis Key
     */
    public static String stockKey(Long activityId) {
        return StrUtil.format(STOCK_KEY, activityId);
    }

    /**
     * 构建用户限购 Key
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return Redis Key
     */
    public static String userKey(Long activityId, Long userId) {
        return StrUtil.format(USER_KEY, activityId, userId);
    }

    /**
     * 构建抢购结果 Key
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return Redis Key
     */
    public static String resultKey(Long activityId, Long userId) {
        return StrUtil.format(RESULT_KEY, activityId, userId);
    }

    /**
     * 构建活动预热锁 Key
     *
     * @param activityId 活动ID
     * @return Redis Key
     */
    public static String warmupLockKey(Long activityId) {
        return StrUtil.format(WARMUP_LOCK_KEY, activityId);
    }
}
```

### RabbitMQ 配置

这里定义秒杀订单交换机、队列、路由键，并使用 JSON 消息转换器传输对象消息。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        # 消费端手动确认，便于消费成功后再 ack
        acknowledge-mode: manual
        # 单个消费者一次最多拉取 10 条，避免瞬间打爆数据库
        prefetch: 10
        retry:
          # 业务中建议配合死信队列；这里先关闭自动重试，交给代码和后续补偿处理
          enabled: false
    template:
      # 发送超时时间
      reply-timeout: 5s
```

RabbitMQ 队列和交换机配置如下。

文件位置：`src/main/java/io/github/atengk/seckill/config/RabbitMqConfig.java`

```java
package io.github.atengk.seckill.config;

import io.github.atengk.seckill.constant.SeckillConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.support.converter.Jackson2JsonMessageConverter;
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
     * 秒杀订单交换机
     *
     * @return 直连交换机
     */
    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SeckillConstants.SECKILL_ORDER_EXCHANGE, true, false);
    }

    /**
     * 秒杀订单队列
     *
     * @return 队列
     */
    @Bean
    public Queue seckillOrderQueue() {
        return new Queue(SeckillConstants.SECKILL_ORDER_QUEUE, true);
    }

    /**
     * 秒杀订单队列绑定
     *
     * @return 绑定关系
     */
    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillOrderExchange())
                .with(SeckillConstants.SECKILL_ORDER_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器
     *
     * @return 消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

### Lua 脚本加载

Lua 脚本放在 `resources/lua` 目录下，由 Spring 启动时加载成 `DefaultRedisScript<Long>`，后续通过 `StringRedisTemplate.execute()` 执行。

文件位置：`src/main/resources/lua/seckill_stock.lua`

```lua
-- 秒杀库存扣减脚本
-- KEYS[1]：库存 Key，例如 seckill:stock:10001
-- KEYS[2]：用户限购 Key，例如 seckill:user:10001:20001
-- ARGV[1]：用户限购 Key 过期秒数

local stockKey = KEYS[1]
local userKey = KEYS[2]
local userExpireSeconds = tonumber(ARGV[1])

-- 用户已经抢购过，返回 2
if redis.call('exists', userKey) == 1 then
    return 2
end

-- 库存不存在，返回 3
local stock = redis.call('get', stockKey)
if not stock then
    return 3
end

-- 库存不足，返回 1
if tonumber(stock) <= 0 then
    return 1
end

-- 扣减库存
redis.call('decr', stockKey)

-- 标记用户已参与
redis.call('set', userKey, '1', 'EX', userExpireSeconds)

-- 扣减成功，返回 0
return 0
```

Lua 脚本加载配置如下。

文件位置：`src/main/java/io/github/atengk/seckill/config/RedisScriptConfig.java`

```java
package io.github.atengk.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Redis Lua 脚本配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class RedisScriptConfig {

    /**
     * 秒杀库存扣减脚本
     *
     * @return Redis Lua 脚本
     */
    @Bean
    public DefaultRedisScript<Long> seckillStockScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill_stock.lua")));
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}
```

### 活动预热实现

活动预热从数据库读取活动库存，将库存写入 Redis。这里使用 Redis `SETNX` 做一个轻量预热锁，避免多节点重复预热。

先给出接口入参、统一响应和 Controller。

文件位置：`src/main/java/io/github/atengk/seckill/common/ApiResult.java`

```java
package io.github.atengk.seckill.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 成功响应
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 响应结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(0, "操作成功", data);
    }

    /**
     * 成功响应
     *
     * @param message 提示信息
     * @param data    数据
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(0, message, data);
    }

    /**
     * 失败响应
     *
     * @param message 提示信息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(-1, message, null);
    }
}
```

文件位置：`src/main/java/io/github/atengk/seckill/dto/SeckillWarmupRequest.java`

```java
package io.github.atengk.seckill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒杀活动预热请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class SeckillWarmupRequest {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;
}
```

文件位置：`src/main/java/io/github/atengk/seckill/dto/SeckillPurchaseRequest.java`

```java
package io.github.atengk.seckill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒杀抢购请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class SeckillPurchaseRequest {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "商品ID不能为空")
    private Long skuId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
```

文件位置：`src/main/java/io/github/atengk/seckill/vo/SeckillResultVO.java`

```java
package io.github.atengk.seckill.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀结果响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResultVO {

    private String status;

    private String orderNo;

    private String message;

    /**
     * 构建结果对象
     *
     * @param status  状态
     * @param orderNo 订单号
     * @param message 提示信息
     * @return 秒杀结果
     */
    public static SeckillResultVO of(String status, String orderNo, String message) {
        return new SeckillResultVO(status, orderNo, message);
    }
}
```

Controller 提供活动预热、抢购和结果查询三个接口。

文件位置：`src/main/java/io/github/atengk/seckill/controller/SeckillController.java`

```java
package io.github.atengk.seckill.controller;

import io.github.atengk.seckill.common.ApiResult;
import io.github.atengk.seckill.dto.SeckillPurchaseRequest;
import io.github.atengk.seckill.dto.SeckillWarmupRequest;
import io.github.atengk.seckill.service.SeckillService;
import io.github.atengk.seckill.vo.SeckillResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 活动预热
     *
     * @param request 预热请求
     * @return 预热结果
     */
    @PostMapping("/warmup")
    public ApiResult<Boolean> warmup(@Valid @RequestBody SeckillWarmupRequest request) {
        return seckillService.warmup(request.getActivityId());
    }

    /**
     * 抢购下单
     *
     * @param request 抢购请求
     * @return 抢购受理结果
     */
    @PostMapping("/purchase")
    public ApiResult<SeckillResultVO> purchase(@Valid @RequestBody SeckillPurchaseRequest request) {
        return seckillService.purchase(request);
    }

    /**
     * 查询抢购结果
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 抢购结果
     */
    @GetMapping("/result")
    public ApiResult<SeckillResultVO> queryResult(@RequestParam Long activityId, @RequestParam Long userId) {
        return seckillService.queryResult(activityId, userId);
    }
}
```

Service 接口定义如下。

文件位置：`src/main/java/io/github/atengk/seckill/service/SeckillService.java`

```java
package io.github.atengk.seckill.service;

import io.github.atengk.seckill.common.ApiResult;
import io.github.atengk.seckill.dto.SeckillPurchaseRequest;
import io.github.atengk.seckill.vo.SeckillResultVO;

/**
 * 秒杀服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SeckillService {

    /**
     * 活动预热
     *
     * @param activityId 活动ID
     * @return 预热结果
     */
    ApiResult<Boolean> warmup(Long activityId);

    /**
     * 抢购下单
     *
     * @param request 抢购请求
     * @return 抢购结果
     */
    ApiResult<SeckillResultVO> purchase(SeckillPurchaseRequest request);

    /**
     * 查询抢购结果
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 抢购结果
     */
    ApiResult<SeckillResultVO> queryResult(Long activityId, Long userId);
}
```

### 抢购请求校验实现

抢购请求校验主要检查活动是否存在、活动是否开始、活动是否结束。资格校验、黑名单、会员等级等复杂规则可以在这里扩展。

下面的 Service 实现包含活动预热、抢购校验、Redis Lua 扣减、MQ 发送和结果查询。

文件位置：`src/main/java/io/github/atengk/seckill/service/impl/SeckillServiceImpl.java`

```java
package io.github.atengk.seckill.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.seckill.common.ApiResult;
import io.github.atengk.seckill.constant.SeckillConstants;
import io.github.atengk.seckill.dto.SeckillPurchaseRequest;
import io.github.atengk.seckill.entity.SeckillActivity;
import io.github.atengk.seckill.entity.SeckillOrder;
import io.github.atengk.seckill.entity.SeckillStock;
import io.github.atengk.seckill.mapper.SeckillActivityMapper;
import io.github.atengk.seckill.mapper.SeckillOrderMapper;
import io.github.atengk.seckill.mapper.SeckillStockMapper;
import io.github.atengk.seckill.mq.SeckillOrderMessage;
import io.github.atengk.seckill.service.SeckillService;
import io.github.atengk.seckill.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private static final long LUA_SUCCESS = 0L;
    private static final long LUA_SOLD_OUT = 1L;
    private static final long LUA_REPEAT = 2L;
    private static final long LUA_STOCK_NOT_INIT = 3L;

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> seckillStockScript;
    private final RabbitTemplate rabbitTemplate;

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillStockMapper seckillStockMapper;
    private final SeckillOrderMapper seckillOrderMapper;

    /**
     * 活动预热
     *
     * @param activityId 活动ID
     * @return 预热结果
     */
    @Override
    public ApiResult<Boolean> warmup(Long activityId) {
        String lockKey = SeckillConstants.warmupLockKey(activityId);
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(30));

        if (!Boolean.TRUE.equals(locked)) {
            log.warn("秒杀活动正在预热中，activityId={}", activityId);
            return ApiResult.fail("活动正在预热中，请勿重复操作");
        }

        try {
            SeckillActivity activity = seckillActivityMapper.selectById(activityId);
            if (ObjectUtil.isNull(activity)) {
                return ApiResult.fail("活动不存在");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(activity.getEndTime())) {
                return ApiResult.fail("活动已结束，不允许预热");
            }

            SeckillStock stock = seckillStockMapper.selectOne(
                    Wrappers.<SeckillStock>lambdaQuery()
                            .eq(SeckillStock::getActivityId, activityId)
                            .last("limit 1")
            );

            if (ObjectUtil.isNull(stock) || stock.getAvailableStock() <= 0) {
                return ApiResult.fail("活动库存不足，无法预热");
            }

            String stockKey = SeckillConstants.stockKey(activityId);
            Boolean stockExists = stringRedisTemplate.hasKey(stockKey);
            if (Boolean.TRUE.equals(stockExists)) {
                log.info("秒杀库存已存在，跳过重复预热，activityId={}, stockKey={}", activityId, stockKey);
                return ApiResult.success("活动已预热，无需重复操作", true);
            }

            long expireSeconds = calcExpireSeconds(activity.getEndTime());
            stringRedisTemplate.opsForValue()
                    .set(stockKey, String.valueOf(stock.getAvailableStock()), expireSeconds, TimeUnit.SECONDS);

            log.info("秒杀活动预热成功，activityId={}, stock={}, expireSeconds={}",
                    activityId, stock.getAvailableStock(), expireSeconds);

            return ApiResult.success("活动预热成功", true);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 抢购下单
     *
     * @param request 抢购请求
     * @return 抢购结果
     */
    @Override
    public ApiResult<SeckillResultVO> purchase(SeckillPurchaseRequest request) {
        SeckillActivity activity = seckillActivityMapper.selectById(request.getActivityId());
        if (ObjectUtil.isNull(activity)) {
            return ApiResult.fail("活动不存在");
        }

        ApiResult<SeckillResultVO> timeCheckResult = checkActivityTime(activity, request.getActivityId(), request.getUserId());
        if (ObjectUtil.isNotNull(timeCheckResult)) {
            return timeCheckResult;
        }

        String stockKey = SeckillConstants.stockKey(request.getActivityId());
        String userKey = SeckillConstants.userKey(request.getActivityId(), request.getUserId());
        String resultKey = SeckillConstants.resultKey(request.getActivityId(), request.getUserId());

        Long luaResult = stringRedisTemplate.execute(
                seckillStockScript,
                List.of(stockKey, userKey),
                String.valueOf(SeckillConstants.USER_LIMIT_EXPIRE_SECONDS)
        );

        if (ObjectUtil.isNull(luaResult)) {
            log.error("Redis Lua 脚本执行结果为空，activityId={}, userId={}", request.getActivityId(), request.getUserId());
            return ApiResult.fail("系统繁忙，请稍后重试");
        }

        if (LUA_SOLD_OUT == luaResult) {
            SeckillResultVO resultVO = SeckillResultVO.of("SOLD_OUT", null, "库存不足");
            setResult(resultKey, resultVO);
            return ApiResult.success("库存不足", resultVO);
        }

        if (LUA_REPEAT == luaResult) {
            SeckillResultVO resultVO = SeckillResultVO.of("REPEAT", null, "请勿重复抢购");
            setResult(resultKey, resultVO);
            return ApiResult.success("请勿重复抢购", resultVO);
        }

        if (LUA_STOCK_NOT_INIT == luaResult) {
            SeckillResultVO resultVO = SeckillResultVO.of("FAIL", null, "活动库存未初始化");
            setResult(resultKey, resultVO);
            return ApiResult.success("活动库存未初始化", resultVO);
        }

        if (LUA_SUCCESS != luaResult) {
            log.error("未知 Lua 返回码，activityId={}, userId={}, luaResult={}",
                    request.getActivityId(), request.getUserId(), luaResult);
            return ApiResult.fail("系统繁忙，请稍后重试");
        }

        SeckillResultVO processingResult = SeckillResultVO.of("PROCESSING", null, "抢购请求已进入队列");
        setResult(resultKey, processingResult);

        try {
            sendOrderMessage(request);
            log.info("秒杀抢购请求受理成功，activityId={}, skuId={}, userId={}",
                    request.getActivityId(), request.getSkuId(), request.getUserId());
            return ApiResult.success("抢购请求已受理，请稍后查询结果", processingResult);
        } catch (Exception exception) {
            log.error("秒杀 MQ 消息发送失败，开始回补 Redis 库存，activityId={}, userId={}",
                    request.getActivityId(), request.getUserId(), exception);

            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(userKey);

            SeckillResultVO failResult = SeckillResultVO.of("FAIL", null, "请求进入队列失败，请重试");
            setResult(resultKey, failResult);
            return ApiResult.fail("请求进入队列失败，请重试");
        }
    }

    /**
     * 查询抢购结果
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 抢购结果
     */
    @Override
    public ApiResult<SeckillResultVO> queryResult(Long activityId, Long userId) {
        String resultKey = SeckillConstants.resultKey(activityId, userId);
        String resultJson = stringRedisTemplate.opsForValue().get(resultKey);

        if (JSONUtil.isTypeJSON(resultJson)) {
            SeckillResultVO resultVO = JSONUtil.toBean(resultJson, SeckillResultVO.class);
            return ApiResult.success("查询成功", resultVO);
        }

        SeckillOrder order = seckillOrderMapper.selectOne(
                Wrappers.<SeckillOrder>lambdaQuery()
                        .eq(SeckillOrder::getActivityId, activityId)
                        .eq(SeckillOrder::getUserId, userId)
                        .last("limit 1")
        );

        if (ObjectUtil.isNotNull(order)) {
            SeckillResultVO resultVO = SeckillResultVO.of("SUCCESS", order.getOrderNo(), "抢购成功");
            setResult(resultKey, resultVO);
            return ApiResult.success("查询成功", resultVO);
        }

        return ApiResult.success("查询成功", SeckillResultVO.of("PROCESSING", null, "订单创建中"));
    }

    /**
     * 校验活动时间
     *
     * @param activity   活动信息
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 校验失败时返回响应，校验通过返回 null
     */
    private ApiResult<SeckillResultVO> checkActivityTime(SeckillActivity activity, Long activityId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        String resultKey = SeckillConstants.resultKey(activityId, userId);

        if (now.isBefore(activity.getStartTime())) {
            SeckillResultVO resultVO = SeckillResultVO.of("NOT_STARTED", null, "活动未开始");
            setResult(resultKey, resultVO);
            return ApiResult.success("活动未开始", resultVO);
        }

        if (now.isAfter(activity.getEndTime())) {
            SeckillResultVO resultVO = SeckillResultVO.of("ENDED", null, "活动已结束");
            setResult(resultKey, resultVO);
            return ApiResult.success("活动已结束", resultVO);
        }

        return null;
    }

    /**
     * 发送异步下单消息
     *
     * @param request 抢购请求
     */
    private void sendOrderMessage(SeckillPurchaseRequest request) {
        String messageId = IdUtil.fastSimpleUUID();

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setMessageId(messageId);
        message.setActivityId(request.getActivityId());
        message.setSkuId(request.getSkuId());
        message.setUserId(request.getUserId());
        message.setRequestTime(DateUtil.now());

        MessagePostProcessor postProcessor = amqpMessage -> {
            amqpMessage.getMessageProperties().setMessageId(messageId);
            amqpMessage.getMessageProperties().setContentType("application/json");
            return amqpMessage;
        };

        rabbitTemplate.convertAndSend(
                SeckillConstants.SECKILL_ORDER_EXCHANGE,
                SeckillConstants.SECKILL_ORDER_ROUTING_KEY,
                message,
                postProcessor
        );
    }

    /**
     * 写入抢购结果
     *
     * @param resultKey Redis 结果 Key
     * @param resultVO  结果对象
     */
    private void setResult(String resultKey, SeckillResultVO resultVO) {
        stringRedisTemplate.opsForValue().set(
                resultKey,
                JSONUtil.toJsonStr(resultVO),
                SeckillConstants.RESULT_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * 计算 Redis Key 过期时间
     *
     * @param endTime 活动结束时间
     * @return 过期秒数
     */
    private long calcExpireSeconds(LocalDateTime endTime) {
        long seconds = Duration.between(LocalDateTime.now(), endTime.plusHours(24)).getSeconds();
        return Math.max(seconds, SeckillConstants.RESULT_EXPIRE_SECONDS);
    }
}
```

### Redis 原子扣减实现

Redis 原子扣减已经集成在 `purchase()` 方法中，核心调用如下：

```java
Long luaResult = stringRedisTemplate.execute(
        seckillStockScript,
        List.of(stockKey, userKey),
        String.valueOf(SeckillConstants.USER_LIMIT_EXPIRE_SECONDS)
);
```

返回码处理建议保持简单：

| 返回码 | 状态       | 说明                   |
| ------ | ---------- | ---------------------- |
| `0`    | PROCESSING | 扣减成功，进入 MQ 队列 |
| `1`    | SOLD_OUT   | 库存不足               |
| `2`    | REPEAT     | 用户重复抢购           |
| `3`    | FAIL       | Redis 库存未初始化     |

这一步是防超卖和一人一单的核心。不要拆成 `GET -> 判断 -> DECR -> SET 用户标记` 多条 Redis 命令，否则高并发下会出现竞态问题。

### MQ 异步发送实现

MQ 消息对象只保留创建订单所需的最小字段，避免消息体过大。

文件位置：`src/main/java/io/github/atengk/seckill/mq/SeckillOrderMessage.java`

```java
package io.github.atengk.seckill.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * 秒杀下单消息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class SeckillOrderMessage implements Serializable {

    private String messageId;

    private Long activityId;

    private Long skuId;

    private Long userId;

    private String requestTime;
}
```

消息发送在 `SeckillServiceImpl#sendOrderMessage()` 中完成，发送成功后接口返回 `PROCESSING`，由前端通过结果查询接口轮询最终结果。

如果 MQ 发送失败，当前示例会立即执行 Redis 库存回补：

```text
Redis 库存 + 1
删除用户限购 Key
写入抢购结果 FAIL
返回接口失败
```

### MQ 消费创建订单实现

MQ 消费者负责真正创建秒杀订单。核心逻辑包括消息幂等、订单唯一校验、创建订单、更新数据库库存、更新抢购结果。

这里假设 `MqConsumeRecord`、`SeckillOrder`、`SeckillStock` 等实体已经按前面的表结构创建。

文件位置：`src/main/java/io/github/atengk/seckill/mq/SeckillOrderConsumer.java`

```java
package io.github.atengk.seckill.mq;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;
import io.github.atengk.seckill.constant.SeckillConstants;
import io.github.atengk.seckill.entity.MqConsumeRecord;
import io.github.atengk.seckill.entity.SeckillOrder;
import io.github.atengk.seckill.entity.SeckillStock;
import io.github.atengk.seckill.mapper.MqConsumeRecordMapper;
import io.github.atengk.seckill.mapper.SeckillOrderMapper;
import io.github.atengk.seckill.mapper.SeckillStockMapper;
import io.github.atengk.seckill.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀订单消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillStockMapper seckillStockMapper;
    private final MqConsumeRecordMapper mqConsumeRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 消费秒杀下单消息
     *
     * @param orderMessage 秒杀下单消息
     * @param message      RabbitMQ 原始消息
     * @param channel      RabbitMQ 通道
     * @throws IOException ack 异常
     */
    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = SeckillConstants.SECKILL_ORDER_QUEUE)
    public void consume(SeckillOrderMessage orderMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            createOrder(orderMessage);
            channel.basicAck(deliveryTag, false);
            log.info("秒杀订单消息消费成功，messageId={}, activityId={}, userId={}",
                    orderMessage.getMessageId(), orderMessage.getActivityId(), orderMessage.getUserId());
        } catch (DuplicateKeyException exception) {
            channel.basicAck(deliveryTag, false);
            log.warn("秒杀订单消息重复消费，已直接确认，messageId={}, activityId={}, userId={}",
                    orderMessage.getMessageId(), orderMessage.getActivityId(), orderMessage.getUserId());
        } catch (Exception exception) {
            log.error("秒杀订单消息消费失败，messageId={}, activityId={}, userId={}",
                    orderMessage.getMessageId(), orderMessage.getActivityId(), orderMessage.getUserId(), exception);

            // 简化处理：核心案例中直接回补并确认消息。
            // 生产环境建议结合重试队列、死信队列、补偿任务，避免临时异常导致直接失败。
            compensateRedis(orderMessage);
            updateResult(orderMessage, SeckillResultVO.of("FAIL", null, "订单创建失败，请稍后重试"));
            channel.basicAck(deliveryTag, false);
        }
    }

    /**
     * 创建秒杀订单
     *
     * @param message 下单消息
     */
    private void createOrder(SeckillOrderMessage message) {
        insertConsumeRecord(message);

        SeckillOrder existedOrder = seckillOrderMapper.selectOne(
                Wrappers.<SeckillOrder>lambdaQuery()
                        .eq(SeckillOrder::getActivityId, message.getActivityId())
                        .eq(SeckillOrder::getUserId, message.getUserId())
                        .last("limit 1")
        );

        if (existedOrder != null) {
            updateResult(message, SeckillResultVO.of("SUCCESS", existedOrder.getOrderNo(), "抢购成功"));
            markConsumeSuccess(message.getMessageId());
            log.info("秒杀订单已存在，跳过重复创建，activityId={}, userId={}",
                    message.getActivityId(), message.getUserId());
            return;
        }

        String orderNo = buildOrderNo();

        SeckillOrder order = new SeckillOrder();
        order.setId(IdUtil.getSnowflakeNextId());
        order.setOrderNo(orderNo);
        order.setActivityId(message.getActivityId());
        order.setSkuId(message.getSkuId());
        order.setUserId(message.getUserId());
        order.setStatus(0);

        seckillOrderMapper.insert(order);

        int updated = seckillStockMapper.update(
                null,
                Wrappers.<SeckillStock>update()
                        .setSql("sold_stock = sold_stock + 1")
                        .setSql("available_stock = available_stock - 1")
                        .eq("activity_id", message.getActivityId())
                        .eq("sku_id", message.getSkuId())
                        .gt("available_stock", 0)
        );

        if (updated <= 0) {
            throw new IllegalStateException("数据库库存扣减失败");
        }

        updateResult(message, SeckillResultVO.of("SUCCESS", orderNo, "抢购成功"));
        markConsumeSuccess(message.getMessageId());
    }

    /**
     * 插入消费记录
     *
     * @param message 下单消息
     */
    private void insertConsumeRecord(SeckillOrderMessage message) {
        MqConsumeRecord record = new MqConsumeRecord();
        record.setId(IdUtil.getSnowflakeNextId());
        record.setMessageId(message.getMessageId());
        record.setBusinessType("SECKILL_ORDER");
        record.setBusinessKey(message.getActivityId() + ":" + message.getUserId());
        record.setConsumeStatus(0);
        record.setRetryCount(0);
        mqConsumeRecordMapper.insert(record);
    }

    /**
     * 标记消费成功
     *
     * @param messageId 消息ID
     */
    private void markConsumeSuccess(String messageId) {
        mqConsumeRecordMapper.update(
                null,
                Wrappers.<MqConsumeRecord>update()
                        .set("consume_status", 1)
                        .set("error_message", null)
                        .eq("message_id", messageId)
        );
    }

    /**
     * 回补 Redis 库存
     *
     * @param message 下单消息
     */
    private void compensateRedis(SeckillOrderMessage message) {
        String stockKey = SeckillConstants.stockKey(message.getActivityId());
        String userKey = SeckillConstants.userKey(message.getActivityId(), message.getUserId());

        stringRedisTemplate.opsForValue().increment(stockKey);
        stringRedisTemplate.delete(userKey);

        log.warn("秒杀 Redis 库存已回补，activityId={}, userId={}",
                message.getActivityId(), message.getUserId());
    }

    /**
     * 更新抢购结果
     *
     * @param message  下单消息
     * @param resultVO 结果
     */
    private void updateResult(SeckillOrderMessage message, SeckillResultVO resultVO) {
        String resultKey = SeckillConstants.resultKey(message.getActivityId(), message.getUserId());
        stringRedisTemplate.opsForValue().set(
                resultKey,
                JSONUtil.toJsonStr(resultVO),
                SeckillConstants.RESULT_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * 生成订单号
     *
     * @return 订单号
     */
    private String buildOrderNo() {
        return "SK" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + IdUtil.getSnowflakeNextIdStr();
    }
}
```

这段代码的核心幂等点：

| 幂等点                                       | 作用                             |
| -------------------------------------------- | -------------------------------- |
| `mq_consume_record.uk_message_id`            | 防止同一 MQ 消息重复处理         |
| `seckill_order.uk_activity_user`             | 防止同一用户同一活动重复生成订单 |
| Redis `seckill:user:{activityId}:{userId}`   | 在入口处拦截重复抢购             |
| Redis `seckill:result:{activityId}:{userId}` | 支持前端查询最终处理结果         |

### 抢购结果查询实现

抢购结果查询已经在 `SeckillServiceImpl#queryResult()` 中实现，查询顺序如下：

```text
先查 Redis 抢购结果
-> Redis 有结果，直接返回
-> Redis 无结果，查 MySQL 订单表
-> MySQL 有订单，回写 Redis 并返回 SUCCESS
-> MySQL 无订单，返回 PROCESSING
```

核心代码如下：

```java
String resultKey = SeckillConstants.resultKey(activityId, userId);
String resultJson = stringRedisTemplate.opsForValue().get(resultKey);

if (JSONUtil.isTypeJSON(resultJson)) {
    SeckillResultVO resultVO = JSONUtil.toBean(resultJson, SeckillResultVO.class);
    return ApiResult.success("查询成功", resultVO);
}

SeckillOrder order = seckillOrderMapper.selectOne(
        Wrappers.<SeckillOrder>lambdaQuery()
                .eq(SeckillOrder::getActivityId, activityId)
                .eq(SeckillOrder::getUserId, userId)
                .last("limit 1")
);

if (ObjectUtil.isNotNull(order)) {
    SeckillResultVO resultVO = SeckillResultVO.of("SUCCESS", order.getOrderNo(), "抢购成功");
    setResult(resultKey, resultVO);
    return ApiResult.success("查询成功", resultVO);
}

return ApiResult.success("查询成功", SeckillResultVO.of("PROCESSING", null, "订单创建中"));
```

查询接口不建议直接返回“失败”，除非 Redis 中已经明确写入 `FAIL`、`SOLD_OUT`、`REPEAT` 等状态。因为 MQ 消费存在异步延迟，直接查不到订单并不一定代表失败。

## 并发与幂等控制

秒杀链路的并发控制不能只依赖单点方案，需要使用 Redis、MQ、数据库唯一索引多层兜底。核心目标是：库存不能超卖、用户不能重复下单、MQ 重复消费不能产生重复订单、失败后库存能够正确回补。该场景本身的核心难点包括高并发限流、热点 Key、Redis Lua 原子扣减、一人一单、异步削峰、重复消费幂等和库存回补。

### 一人一单控制

一人一单需要在三个位置同时控制：Redis 入口拦截、MQ 消费端查询、数据库唯一索引兜底。不能只依赖前端按钮置灰，也不能只依赖接口层查询数据库。

推荐控制链路如下：

```text
用户提交抢购请求
-> Redis Lua 判断 seckill:user:{activityId}:{userId} 是否存在
-> 不存在则扣库存并写入用户限购 Key
-> MQ 消费端创建订单前查询订单是否已存在
-> 数据库 uk_activity_user 唯一索引最终兜底
```

Redis 用户限购 Key：

```text
seckill:user:{activityId}:{userId}
```

示例：

```text
seckill:user:10001:20001 = 1
```

一人一单的关键点是“判断用户是否参与”和“扣减库存”必须放在同一个 Lua 脚本中，不能拆成多条 Redis 命令。

错误写法：

```text
exists 用户限购 Key
-> get 库存
-> decr 库存
-> set 用户限购 Key
```

高并发下，上面这种写法可能在多个请求同时通过判断后重复扣库存。

正确写法：

```text
Lua 脚本内完成：
exists 用户限购 Key
get 库存
decr 库存
set 用户限购 Key
```

数据库层必须保留唯一索引：

```sql
ALTER TABLE `seckill_order`
ADD UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`);
```

这个唯一索引用于防止以下异常场景：

| 异常场景                   | 唯一索引作用     |
| -------------------------- | ---------------- |
| MQ 重复投递                | 防止重复创建订单 |
| 消费者重复消费             | 防止重复创建订单 |
| Redis Key 异常丢失         | 防止重复创建订单 |
| 接口被绕过直接调用内部逻辑 | 防止重复创建订单 |
| 服务重启导致中间状态丢失   | 防止重复创建订单 |

### Redis 扣减幂等

Redis 扣减幂等的核心是：同一个用户对同一个活动只能成功扣减一次库存。重复请求不能再次扣库存，只能返回重复抢购结果。

Lua 返回码建议固定，不要使用模糊的布尔值：

| 返回码 | 含义         | 是否扣库存 |
| ------ | ------------ | ---------- |
| `0`    | 扣减成功     | 是         |
| `1`    | 库存不足     | 否         |
| `2`    | 重复抢购     | 否         |
| `3`    | 库存未初始化 | 否         |

扣减脚本的核心逻辑如下：

```lua
-- 用户已经抢购过，直接返回重复
if redis.call('exists', userKey) == 1 then
    return 2
end

-- 库存不存在，说明活动未预热或缓存异常
local stock = redis.call('get', stockKey)
if not stock then
    return 3
end

-- 库存不足
if tonumber(stock) <= 0 then
    return 1
end

-- 只有首次参与且库存充足时才扣减库存
redis.call('decr', stockKey)

-- 写入用户限购标记
redis.call('set', userKey, '1', 'EX', userExpireSeconds)

return 0
```

接口层处理时，只有 Lua 返回 `0` 才允许发送 MQ：

```java
if (LUA_SUCCESS != luaResult) {
    return handleLuaFailResult(luaResult, resultKey);
}

setResult(resultKey, SeckillResultVO.of("PROCESSING", null, "抢购请求已进入队列"));
sendOrderMessage(request);
```

推荐将 Lua 失败返回封装成独立方法，避免抢购主流程堆太多分支。

文件位置：`src/main/java/io/github/atengk/seckill/service/impl/SeckillServiceImpl.java`

```java
    /**
     * 处理 Lua 扣减失败结果
     *
     * @param luaResult Lua 返回码
     * @param resultKey 抢购结果 Key
     * @return 接口响应
     */
    private ApiResult<SeckillResultVO> handleLuaFailResult(Long luaResult, String resultKey) {
        SeckillResultVO resultVO;

        if (LUA_SOLD_OUT == luaResult) {
            resultVO = SeckillResultVO.of("SOLD_OUT", null, "库存不足");
            setResult(resultKey, resultVO);
            return ApiResult.success("库存不足", resultVO);
        }

        if (LUA_REPEAT == luaResult) {
            resultVO = SeckillResultVO.of("REPEAT", null, "请勿重复抢购");
            setResult(resultKey, resultVO);
            return ApiResult.success("请勿重复抢购", resultVO);
        }

        if (LUA_STOCK_NOT_INIT == luaResult) {
            resultVO = SeckillResultVO.of("FAIL", null, "活动库存未初始化");
            setResult(resultKey, resultVO);
            return ApiResult.success("活动库存未初始化", resultVO);
        }

        log.error("未知 Lua 返回码，luaResult={}", luaResult);
        return ApiResult.fail("系统繁忙，请稍后重试");
    }
```

### MQ 消费幂等

MQ 消费幂等要解决两个问题：同一条消息可能被重复投递，同一个业务请求也可能产生多条消息。因此消费端不能只根据 RabbitMQ 的投递次数判断是否重复，必须基于业务唯一键做幂等。

推荐幂等键设计：

```text
message_id：消息唯一 ID，用于判断同一条 MQ 消息是否重复消费
business_key：activityId:userId，用于判断同一用户同一活动是否已经处理
```

消费记录表唯一索引：

```sql
ALTER TABLE `mq_consume_record`
ADD UNIQUE KEY `uk_message_id` (`message_id`);
```

订单表唯一索引：

```sql
ALTER TABLE `seckill_order`
ADD UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`);
```

消费端处理顺序建议如下：

```text
插入消费记录 consume_status = 0
-> 插入失败，说明消息已处理或处理中，直接 ack
-> 查询秒杀订单是否已存在
-> 不存在则创建订单
-> 更新消费记录为成功
-> 更新抢购结果为 SUCCESS
-> ack 消息
```

下面给出一个可复用的 MQ 幂等服务，用于消费端判断消息是否可以处理。

文件位置：`src/main/java/io/github/atengk/seckill/service/MqConsumeIdempotentService.java`

```java
package io.github.atengk.seckill.service;

/**
 * MQ 消费幂等服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface MqConsumeIdempotentService {

    /**
     * 标记消息开始消费
     *
     * @param messageId    消息ID
     * @param businessType 业务类型
     * @param businessKey  业务唯一键
     * @return true 表示允许继续消费，false 表示重复消息
     */
    boolean markProcessing(String messageId, String businessType, String businessKey);

    /**
     * 标记消息消费成功
     *
     * @param messageId 消息ID
     */
    void markSuccess(String messageId);

    /**
     * 标记消息消费失败
     *
     * @param messageId    消息ID
     * @param errorMessage 失败原因
     */
    void markFail(String messageId, String errorMessage);
}
```

文件位置：`src/main/java/io/github/atengk/seckill/service/impl/MqConsumeIdempotentServiceImpl.java`

```java
package io.github.atengk.seckill.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.seckill.entity.MqConsumeRecord;
import io.github.atengk.seckill.mapper.MqConsumeRecordMapper;
import io.github.atengk.seckill.service.MqConsumeIdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * MQ 消费幂等服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqConsumeIdempotentServiceImpl implements MqConsumeIdempotentService {

    private final MqConsumeRecordMapper mqConsumeRecordMapper;

    /**
     * 标记消息开始消费
     *
     * @param messageId    消息ID
     * @param businessType 业务类型
     * @param businessKey  业务唯一键
     * @return true 表示允许继续消费，false 表示重复消息
     */
    @Override
    public boolean markProcessing(String messageId, String businessType, String businessKey) {
        try {
            MqConsumeRecord record = new MqConsumeRecord();
            record.setId(IdUtil.getSnowflakeNextId());
            record.setMessageId(messageId);
            record.setBusinessType(businessType);
            record.setBusinessKey(businessKey);
            record.setConsumeStatus(0);
            record.setRetryCount(0);
            mqConsumeRecordMapper.insert(record);
            return true;
        } catch (DuplicateKeyException exception) {
            log.warn("MQ 消息已存在，跳过重复消费，messageId={}, businessKey={}", messageId, businessKey);
            return false;
        }
    }

    /**
     * 标记消息消费成功
     *
     * @param messageId 消息ID
     */
    @Override
    public void markSuccess(String messageId) {
        mqConsumeRecordMapper.update(
                null,
                Wrappers.<MqConsumeRecord>update()
                        .set("consume_status", 1)
                        .set("error_message", null)
                        .eq("message_id", messageId)
        );
    }

    /**
     * 标记消息消费失败
     *
     * @param messageId    消息ID
     * @param errorMessage 失败原因
     */
    @Override
    public void markFail(String messageId, String errorMessage) {
        String safeErrorMessage = StrUtil.maxLength(errorMessage, 500);
        mqConsumeRecordMapper.update(
                null,
                Wrappers.<MqConsumeRecord>update()
                        .setSql("retry_count = retry_count + 1")
                        .set("consume_status", 2)
                        .set("error_message", safeErrorMessage)
                        .eq("message_id", messageId)
        );
    }
}
```

消费端使用方式：

```java
String businessKey = message.getActivityId() + ":" + message.getUserId();
boolean canConsume = mqConsumeIdempotentService.markProcessing(
        message.getMessageId(),
        "SECKILL_ORDER",
        businessKey
);

if (!canConsume) {
    channel.basicAck(deliveryTag, false);
    return;
}
```

### 数据库唯一索引兜底

数据库唯一索引是秒杀系统最后一道防线。即使 Redis、MQ、应用层都做了幂等，也必须保留数据库唯一索引。

推荐索引如下：

```sql
-- 订单号唯一，防止订单编号重复
ALTER TABLE `seckill_order`
ADD UNIQUE KEY `uk_order_no` (`order_no`);

-- 同一活动下同一用户只能有一条秒杀订单
ALTER TABLE `seckill_order`
ADD UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`);

-- 同一条 MQ 消息只能有一条消费记录
ALTER TABLE `mq_consume_record`
ADD UNIQUE KEY `uk_message_id` (`message_id`);

-- 同一活动同一商品只能有一条库存记录
ALTER TABLE `seckill_stock`
ADD UNIQUE KEY `uk_activity_sku` (`activity_id`, `sku_id`);
```

消费端捕获唯一索引冲突时，不应该直接认为系统异常。对于秒杀订单唯一索引冲突，通常说明订单已经创建成功，可以查询已有订单并回写抢购结果。

文件位置：`src/main/java/io/github/atengk/seckill/mq/SeckillOrderConsumer.java`

```java
    /**
     * 处理重复订单
     *
     * @param message 下单消息
     */
    private void handleDuplicateOrder(SeckillOrderMessage message) {
        SeckillOrder existedOrder = seckillOrderMapper.selectOne(
                Wrappers.<SeckillOrder>lambdaQuery()
                        .eq(SeckillOrder::getActivityId, message.getActivityId())
                        .eq(SeckillOrder::getUserId, message.getUserId())
                        .last("limit 1")
        );

        if (existedOrder != null) {
            updateResult(message, SeckillResultVO.of("SUCCESS", existedOrder.getOrderNo(), "抢购成功"));
            log.info("重复下单命中唯一索引，已返回已有订单，activityId={}, userId={}, orderNo={}",
                    message.getActivityId(), message.getUserId(), existedOrder.getOrderNo());
            return;
        }

        updateResult(message, SeckillResultVO.of("FAIL", null, "重复下单校验失败"));
    }
```

## 库存回补处理

库存回补用于处理“Redis 已扣库存，但后续订单没有成功创建”的场景。只要在 Redis 扣减成功之后、订单创建成功之前发生异常，就需要考虑库存回补。

典型异常包括：

| 异常点             | 是否需要回补         |
| ------------------ | -------------------- |
| Redis Lua 扣减失败 | 不需要，因为库存未扣 |
| MQ 发送失败        | 需要回补             |
| MQ 消费参数异常    | 需要回补             |
| 创建订单失败       | 需要回补             |
| 数据库库存扣减失败 | 需要回补             |
| 消息重复消费       | 不需要，直接 ack     |
| 用户重复抢购       | 不需要，因为库存未扣 |

### 下单失败回补库存

下单失败回补库存需要同时处理三个数据：

```text
Redis 库存 + 1
删除用户限购 Key
更新抢购结果为 FAIL
```

推荐使用 Lua 脚本保证回补操作原子执行。

文件位置：`src/main/resources/lua/seckill_stock_rollback.lua`

```lua
-- 秒杀库存回补脚本
-- KEYS[1]：库存 Key，例如 seckill:stock:10001
-- KEYS[2]：用户限购 Key，例如 seckill:user:10001:20001
-- KEYS[3]：抢购结果 Key，例如 seckill:result:10001:20001
-- ARGV[1]：抢购结果 JSON
-- ARGV[2]：结果 Key 过期秒数

local stockKey = KEYS[1]
local userKey = KEYS[2]
local resultKey = KEYS[3]
local resultJson = ARGV[1]
local resultExpireSeconds = tonumber(ARGV[2])

-- 只有用户限购 Key 存在时才回补库存，避免重复回补
if redis.call('exists', userKey) == 1 then
    redis.call('incr', stockKey)
    redis.call('del', userKey)
end

redis.call('set', resultKey, resultJson, 'EX', resultExpireSeconds)

return 0
```

加载回补脚本。

文件位置：`src/main/java/io/github/atengk/seckill/config/RedisScriptConfig.java`

```java
    /**
     * 秒杀库存回补脚本
     *
     * @return Redis Lua 脚本
     */
    @Bean
    public DefaultRedisScript<Long> seckillStockRollbackScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill_stock_rollback.lua")));
        redisScript.setResultType(Long.class);
        return redisScript;
    }
```

封装回补服务，消费端和 MQ 发送失败时都可以复用。

文件位置：`src/main/java/io/github/atengk/seckill/service/SeckillStockRollbackService.java`

```java
package io.github.atengk.seckill.service;

/**
 * 秒杀库存回补服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SeckillStockRollbackService {

    /**
     * 回补秒杀库存
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param reason     失败原因
     */
    void rollback(Long activityId, Long userId, String reason);
}
```

文件位置：`src/main/java/io/github/atengk/seckill/service/impl/SeckillStockRollbackServiceImpl.java`

```java
package io.github.atengk.seckill.service.impl;

import cn.hutool.json.JSONUtil;
import io.github.atengk.seckill.constant.SeckillConstants;
import io.github.atengk.seckill.service.SeckillStockRollbackService;
import io.github.atengk.seckill.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 秒杀库存回补服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillStockRollbackServiceImpl implements SeckillStockRollbackService {

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> seckillStockRollbackScript;

    /**
     * 回补秒杀库存
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param reason     失败原因
     */
    @Override
    public void rollback(Long activityId, Long userId, String reason) {
        String stockKey = SeckillConstants.stockKey(activityId);
        String userKey = SeckillConstants.userKey(activityId, userId);
        String resultKey = SeckillConstants.resultKey(activityId, userId);

        SeckillResultVO resultVO = SeckillResultVO.of("FAIL", null, reason);

        stringRedisTemplate.execute(
                seckillStockRollbackScript,
                List.of(stockKey, userKey, resultKey),
                JSONUtil.toJsonStr(resultVO),
                String.valueOf(SeckillConstants.RESULT_EXPIRE_SECONDS)
        );

        log.warn("秒杀库存回补完成，activityId={}, userId={}, reason={}", activityId, userId, reason);
    }
}
```

在 MQ 发送失败时调用：

```java
try {
    sendOrderMessage(request);
    return ApiResult.success("抢购请求已受理，请稍后查询结果", processingResult);
} catch (Exception exception) {
    log.error("秒杀 MQ 消息发送失败，activityId={}, userId={}",
            request.getActivityId(), request.getUserId(), exception);
    seckillStockRollbackService.rollback(request.getActivityId(), request.getUserId(), "请求进入队列失败，请重试");
    return ApiResult.fail("请求进入队列失败，请重试");
}
```

### MQ 消费失败重试

MQ 消费失败不能一律立即回补。需要先区分异常类型：

| 异常类型       | 处理策略                       |
| -------------- | ------------------------------ |
| 参数错误       | 不重试，直接失败并回补         |
| 活动不存在     | 不重试，直接失败并回补         |
| 用户重复订单   | 不重试，查询已有订单并返回成功 |
| 数据库临时异常 | 可以重试                       |
| 网络抖动       | 可以重试                       |
| 死锁异常       | 可以重试                       |
| 库存数据异常   | 不重试或进入人工补偿           |

推荐在消息中增加重试次数，由代码判断是否重新入队。简单实现可以使用 RabbitMQ 消息头记录 `x-retry-count`。

下面给出消费者失败处理的核心代码。

文件位置：`src/main/java/io/github/atengk/seckill/mq/SeckillOrderConsumer.java`

```java
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 处理消费异常
     *
     * @param orderMessage 下单消息
     * @param message      原始消息
     * @param channel      RabbitMQ 通道
     * @param deliveryTag  投递标签
     * @param exception    异常
     * @throws IOException ack 或 reject 异常
     */
    private void handleConsumeException(SeckillOrderMessage orderMessage,
                                        Message message,
                                        Channel channel,
                                        long deliveryTag,
                                        Exception exception) throws IOException {
        Integer retryCount = getRetryCount(message);

        if (retryCount < MAX_RETRY_COUNT) {
            log.warn("秒杀订单消费失败，准备重试，messageId={}, retryCount={}",
                    orderMessage.getMessageId(), retryCount + 1, exception);

            mqConsumeIdempotentService.markFail(orderMessage.getMessageId(), exception.getMessage());

            // requeue=true 表示重新入队。生产环境更推荐延迟重试队列，避免立即重试打爆数据库。
            channel.basicNack(deliveryTag, false, true);
            return;
        }

        log.error("秒杀订单消费超过最大重试次数，开始回补库存，messageId={}, activityId={}, userId={}",
                orderMessage.getMessageId(), orderMessage.getActivityId(), orderMessage.getUserId(), exception);

        mqConsumeIdempotentService.markFail(orderMessage.getMessageId(), exception.getMessage());
        seckillStockRollbackService.rollback(orderMessage.getActivityId(), orderMessage.getUserId(), "订单创建失败，请稍后重试");

        // 已完成失败处理和库存回补，确认消息，避免无限重试
        channel.basicAck(deliveryTag, false);
    }

    /**
     * 获取消息重试次数
     *
     * @param message RabbitMQ 消息
     * @return 重试次数
     */
    private Integer getRetryCount(Message message) {
        Object retryCount = message.getMessageProperties().getHeaders().get("x-retry-count");
        if (retryCount instanceof Integer count) {
            return count;
        }
        return 0;
    }
```

上面这种 `basicNack(..., true)` 是最简单的重试方式，但不适合生产高峰期，因为失败消息会立即重新入队。更推荐使用延迟重试队列或死信队列。

### 死信队列补偿

死信队列用于承接多次消费失败的消息，再由补偿消费者做最终处理。它适合处理以下场景：

```text
数据库临时异常重试多次仍失败
-> 消息进入死信队列
-> 死信消费者读取消息
-> 查询订单是否已经创建
-> 已创建则更新结果为 SUCCESS
-> 未创建则回补库存并更新结果为 FAIL
-> 记录补偿日志
```

推荐 RabbitMQ 队列设计：

| 队列                        | 作用         |
| --------------------------- | ------------ |
| `seckill.order.queue`       | 正常下单队列 |
| `seckill.order.retry.queue` | 延迟重试队列 |
| `seckill.order.dead.queue`  | 死信补偿队列 |

RabbitMQ 死信配置如下。

文件位置：`src/main/java/io/github/atengk/seckill/config/RabbitMqConfig.java`

```java
    public static final String SECKILL_ORDER_DEAD_EXCHANGE = "seckill.order.dead.exchange";
    public static final String SECKILL_ORDER_DEAD_QUEUE = "seckill.order.dead.queue";
    public static final String SECKILL_ORDER_DEAD_ROUTING_KEY = "seckill.order.dead";

    /**
     * 秒杀订单死信交换机
     *
     * @return 死信交换机
     */
    @Bean
    public DirectExchange seckillOrderDeadExchange() {
        return new DirectExchange(SECKILL_ORDER_DEAD_EXCHANGE, true, false);
    }

    /**
     * 秒杀订单死信队列
     *
     * @return 死信队列
     */
    @Bean
    public Queue seckillOrderDeadQueue() {
        return new Queue(SECKILL_ORDER_DEAD_QUEUE, true);
    }

    /**
     * 秒杀订单死信绑定
     *
     * @return 绑定关系
     */
    @Bean
    public Binding seckillOrderDeadBinding() {
        return BindingBuilder.bind(seckillOrderDeadQueue())
                .to(seckillOrderDeadExchange())
                .with(SECKILL_ORDER_DEAD_ROUTING_KEY);
    }

    /**
     * 带死信配置的秒杀订单队列
     *
     * @return 正常订单队列
     */
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SeckillConstants.SECKILL_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", SECKILL_ORDER_DEAD_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SECKILL_ORDER_DEAD_ROUTING_KEY)
                .build();
    }
```

注意：如果你已经在前面定义过 `seckillOrderQueue()`，需要替换为上面这个带死信参数的版本，不能重复定义同名 Bean。

死信补偿消费者如下。

文件位置：`src/main/java/io/github/atengk/seckill/mq/SeckillOrderDeadConsumer.java`

```java
package io.github.atengk.seckill.mq;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.seckill.config.RabbitMqConfig;
import io.github.atengk.seckill.entity.SeckillOrder;
import io.github.atengk.seckill.mapper.SeckillOrderMapper;
import io.github.atengk.seckill.service.SeckillStockRollbackService;
import io.github.atengk.seckill.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单死信补偿消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderDeadConsumer {

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillStockRollbackService seckillStockRollbackService;
    private final SeckillResultWriter seckillResultWriter;

    /**
     * 处理死信消息
     *
     * @param message 秒杀订单消息
     */
    @RabbitListener(queues = RabbitMqConfig.SECKILL_ORDER_DEAD_QUEUE)
    public void consumeDeadMessage(SeckillOrderMessage message) {
        log.warn("收到秒杀订单死信消息，messageId={}, activityId={}, userId={}",
                message.getMessageId(), message.getActivityId(), message.getUserId());

        SeckillOrder order = seckillOrderMapper.selectOne(
                Wrappers.<SeckillOrder>lambdaQuery()
                        .eq(SeckillOrder::getActivityId, message.getActivityId())
                        .eq(SeckillOrder::getUserId, message.getUserId())
                        .last("limit 1")
        );

        if (order != null) {
            seckillResultWriter.success(message.getActivityId(), message.getUserId(), order.getOrderNo());
            log.info("死信补偿发现订单已存在，已更新抢购结果，activityId={}, userId={}, orderNo={}",
                    message.getActivityId(), message.getUserId(), order.getOrderNo());
            return;
        }

        seckillStockRollbackService.rollback(message.getActivityId(), message.getUserId(), "订单创建失败，库存已回补");
    }
}
```

为了避免多个消费者重复写 Redis 结果，可以把结果写入封装成独立组件。

文件位置：`src/main/java/io/github/atengk/seckill/mq/SeckillResultWriter.java`

```java
package io.github.atengk.seckill.mq;

import cn.hutool.json.JSONUtil;
import io.github.atengk.seckill.constant.SeckillConstants;
import io.github.atengk.seckill.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀结果写入器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
@RequiredArgsConstructor
public class SeckillResultWriter {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 写入成功结果
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param orderNo    订单号
     */
    public void success(Long activityId, Long userId, String orderNo) {
        write(activityId, userId, SeckillResultVO.of("SUCCESS", orderNo, "抢购成功"));
    }

    /**
     * 写入失败结果
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param reason     失败原因
     */
    public void fail(Long activityId, Long userId, String reason) {
        write(activityId, userId, SeckillResultVO.of("FAIL", null, reason));
    }

    /**
     * 写入抢购结果
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @param resultVO   抢购结果
     */
    public void write(Long activityId, Long userId, SeckillResultVO resultVO) {
        String resultKey = SeckillConstants.resultKey(activityId, userId);
        stringRedisTemplate.opsForValue().set(
                resultKey,
                JSONUtil.toJsonStr(resultVO),
                SeckillConstants.RESULT_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );
    }
}
```

死信队列补偿的最终原则：

```text
先查订单
-> 订单存在：说明业务已成功，只补写结果
-> 订单不存在：说明业务未完成，回补 Redis 库存并写失败结果
```

这样可以避免误回补导致库存变多。对于秒杀场景，库存回补必须谨慎：只要订单已经创建成功，就不能再回补 Redis 库存。

## 接口测试

接口测试用于验证完整链路是否可用：初始化活动数据、预热库存、发起抢购、异步创建订单、查询最终结果。该测试覆盖秒杀场景中的核心流程：活动预热、Redis 加载库存、用户抢购、Redis 原子扣减、MQ 异步下单、结果查询。

### 初始化活动数据

先准备一条秒杀活动和一条库存记录。这里假设活动 ID 为 `10001`，商品 ID 为 `90001`，活动库存为 `10`。

执行 SQL 前，需要保证前面设计的 `seckill_activity`、`seckill_stock`、`seckill_order`、`mq_consume_record` 表已经创建。

```sql
-- 清理测试数据，避免重复测试时受历史数据影响
DELETE FROM seckill_order WHERE activity_id = 10001;
DELETE FROM mq_consume_record WHERE business_key LIKE '10001:%';
DELETE FROM seckill_stock WHERE activity_id = 10001;
DELETE FROM seckill_activity WHERE id = 10001;

-- 初始化秒杀活动
INSERT INTO seckill_activity (
    id,
    activity_name,
    start_time,
    end_time,
    status,
    remark
) VALUES (
    10001,
    '测试秒杀活动',
    DATE_SUB(NOW(), INTERVAL 10 MINUTE),
    DATE_ADD(NOW(), INTERVAL 2 HOUR),
    1,
    '接口测试活动'
);

-- 初始化秒杀库存
INSERT INTO seckill_stock (
    id,
    activity_id,
    sku_id,
    total_stock,
    sold_stock,
    available_stock,
    version
) VALUES (
    20001,
    10001,
    90001,
    10,
    0,
    10,
    0
);
```

同时清理 Redis 中的测试 Key，避免旧缓存影响测试结果。

```bash
# 清理活动库存 Key
redis-cli DEL seckill:stock:10001

# 清理指定用户的测试结果和限购标记
redis-cli DEL seckill:user:10001:20001
redis-cli DEL seckill:result:10001:20001

# 批量清理测试用户，按需执行
for i in $(seq 20001 20100); do
  redis-cli DEL seckill:user:10001:$i
  redis-cli DEL seckill:result:10001:$i
done
```

### 执行活动预热

活动预热接口会读取 MySQL 中的 `available_stock`，并写入 Redis 库存 Key。

```bash
curl -X POST 'http://localhost:8080/api/seckill/warmup' \
  -H 'Content-Type: application/json' \
  -d '{
    "activityId": 10001
  }'
```

预期响应：

```json
{
  "code": 0,
  "message": "活动预热成功",
  "data": true
}
```

验证 Redis 库存是否写入成功：

```bash
redis-cli GET seckill:stock:10001
```

预期输出：

```text
"10"
```

如果返回空值，优先检查以下内容：

| 检查项             | 说明                                 |
| ------------------ | ------------------------------------ |
| 活动是否存在       | `seckill_activity.id = 10001`        |
| 活动是否已结束     | `end_time` 必须大于当前时间          |
| 库存是否存在       | `seckill_stock.activity_id = 10001`  |
| 可用库存是否大于 0 | `available_stock > 0`                |
| Redis 是否连接正确 | 检查 `application.yml` 中 Redis 配置 |

### 模拟并发抢购

先测试单个用户抢购。

```bash
curl -X POST 'http://localhost:8080/api/seckill/purchase' \
  -H 'Content-Type: application/json' \
  -d '{
    "activityId": 10001,
    "skuId": 90001,
    "userId": 20001
  }'
```

预期响应：

```json
{
  "code": 0,
  "message": "抢购请求已受理，请稍后查询结果",
  "data": {
    "status": "PROCESSING",
    "orderNo": null,
    "message": "抢购请求已进入队列"
  }
}
```

测试同一个用户重复抢购。

```bash
curl -X POST 'http://localhost:8080/api/seckill/purchase' \
  -H 'Content-Type: application/json' \
  -d '{
    "activityId": 10001,
    "skuId": 90001,
    "userId": 20001
  }'
```

预期响应：

```json
{
  "code": 0,
  "message": "请勿重复抢购",
  "data": {
    "status": "REPEAT",
    "orderNo": null,
    "message": "请勿重复抢购"
  }
}
```

使用 Shell 模拟 100 个不同用户同时抢购 10 个库存。

```bash
seq 20001 20100 | xargs -n 1 -P 50 -I {} curl -s -X POST 'http://localhost:8080/api/seckill/purchase' \
  -H 'Content-Type: application/json' \
  -d "{\"activityId\":10001,\"skuId\":90001,\"userId\":{}}" | jq .
```

参数说明：

| 参数              | 说明                       |
| ----------------- | -------------------------- |
| `seq 20001 20100` | 生成 100 个用户 ID         |
| `xargs -P 50`     | 并发数为 50                |
| `curl -s`         | 静默请求，只输出响应       |
| `jq .`            | 格式化 JSON 响应，可不安装 |

执行后，理论上最多只有 10 个用户进入 `PROCESSING`，其他用户会返回 `SOLD_OUT` 或后续查询失败状态。

### 查询抢购结果

抢购接口返回 `PROCESSING` 后，需要等待 MQ 消费者异步创建订单。通常等待 1 到 3 秒后查询结果。

```bash
curl 'http://localhost:8080/api/seckill/result?activityId=10001&userId=20001'
```

抢购成功响应：

```json
{
  "code": 0,
  "message": "查询成功",
  "data": {
    "status": "SUCCESS",
    "orderNo": "SK202605151430001234567890",
    "message": "抢购成功"
  }
}
```

抢购失败响应：

```json
{
  "code": 0,
  "message": "查询成功",
  "data": {
    "status": "SOLD_OUT",
    "orderNo": null,
    "message": "库存不足"
  }
}
```

批量查询多个用户的抢购结果：

```bash
for i in $(seq 20001 20100); do
  echo "userId=$i"
  curl -s "http://localhost:8080/api/seckill/result?activityId=10001&userId=$i"
  echo
done
```

数据库验证订单数量：

```sql
-- 查询秒杀订单数量，不能超过活动库存 10
SELECT COUNT(*) AS order_count
FROM seckill_order
WHERE activity_id = 10001;

-- 查询库存扣减结果
SELECT activity_id, sku_id, total_stock, sold_stock, available_stock
FROM seckill_stock
WHERE activity_id = 10001;
```

预期结果：

```text
order_count <= 10
sold_stock <= 10
available_stock >= 0
sold_stock + available_stock = total_stock
```

## 压测验证

压测验证的重点不是追求极限 QPS，而是验证在并发请求下系统是否满足三个基本条件：不超卖、不重复下单、异步结果最终一致。

### JMeter 压测脚本配置

推荐使用 JMeter 创建一个简单压测脚本，模拟多个用户同时请求抢购接口。

测试计划结构：

```text
Test Plan
└── Thread Group
    ├── CSV Data Set Config
    ├── HTTP Header Manager
    ├── HTTP Request - 秒杀抢购
    ├── Constant Throughput Timer
    ├── View Results Tree
    └── Summary Report
```

线程组建议配置：

| 配置项                      | 示例值                             |
| --------------------------- | ---------------------------------- |
| Number of Threads           | 1000                               |
| Ramp-up Period              | 5                                  |
| Loop Count                  | 1                                  |
| Same user on next iteration | 勾选或不勾选均可，本案例 Loop 为 1 |

CSV 用户数据文件：`users.csv`

```csv
userId
20001
20002
20003
20004
20005
20006
20007
20008
20009
20010
20011
20012
20013
20014
20015
```

实际压测时可以生成更多用户：

```bash
echo "userId" > users.csv
seq 20001 30000 >> users.csv
```

`CSV Data Set Config` 配置：

| 配置项             | 示例值               |
| ------------------ | -------------------- |
| Filename           | `/path/to/users.csv` |
| Variable Names     | `userId`             |
| Ignore first line  | `true`               |
| Recycle on EOF     | `false`              |
| Stop thread on EOF | `true`               |
| Sharing mode       | `All threads`        |

`HTTP Header Manager` 配置：

```text
Content-Type: application/json
```

`HTTP Request - 秒杀抢购` 配置：

| 配置项            | 示例值                  |
| ----------------- | ----------------------- |
| Protocol          | `http`                  |
| Server Name or IP | `127.0.0.1`             |
| Port              | `8080`                  |
| Method            | `POST`                  |
| Path              | `/api/seckill/purchase` |

请求体：

```json
{
  "activityId": 10001,
  "skuId": 90001,
  "userId": ${userId}
}
```

如果需要控制吞吐量，可以增加 `Constant Throughput Timer`：

| 配置项                        | 示例值               |
| ----------------------------- | -------------------- |
| Target throughput             | `6000`               |
| Calculate Throughput based on | `All active threads` |

压测前建议重新初始化活动数据和 Redis Key，保证每轮压测条件一致。

```bash
# 清理 Redis 库存
redis-cli DEL seckill:stock:10001

# 清理用户限购和结果 Key
for i in $(seq 20001 30000); do
  redis-cli DEL seckill:user:10001:$i
  redis-cli DEL seckill:result:10001:$i
done
```

然后重新执行活动预热接口：

```bash
curl -X POST 'http://localhost:8080/api/seckill/warmup' \
  -H 'Content-Type: application/json' \
  -d '{"activityId":10001}'
```

### 库存不超卖验证

压测完成后，需要从 Redis、订单表、库存表三个角度验证库存是否正确。

查看 Redis 剩余库存：

```bash
redis-cli GET seckill:stock:10001
```

查询订单数量：

```sql
-- 订单数量不能超过活动总库存
SELECT COUNT(*) AS order_count
FROM seckill_order
WHERE activity_id = 10001;
```

查询数据库库存：

```sql
-- 数据库库存不能出现负数
SELECT
    activity_id,
    sku_id,
    total_stock,
    sold_stock,
    available_stock,
    total_stock - sold_stock AS expected_available_stock
FROM seckill_stock
WHERE activity_id = 10001;
```

验证 SQL：

```sql
-- 如果查出数据，说明出现超卖或库存异常
SELECT *
FROM seckill_stock
WHERE activity_id = 10001
  AND (
      sold_stock > total_stock
      OR available_stock < 0
      OR sold_stock + available_stock <> total_stock
  );
```

预期结果为空。

订单数量也不能超过总库存：

```sql
-- 如果 order_count 大于 total_stock，说明发生超卖
SELECT
    s.activity_id,
    s.total_stock,
    COUNT(o.id) AS order_count
FROM seckill_stock s
LEFT JOIN seckill_order o ON s.activity_id = o.activity_id
WHERE s.activity_id = 10001
GROUP BY s.activity_id, s.total_stock
HAVING COUNT(o.id) > s.total_stock;
```

预期结果为空。

如果出现超卖，优先检查以下点：

| 检查项                      | 说明                           |
| --------------------------- | ------------------------------ |
| 是否所有请求都经过 Lua 脚本 | 不能绕过 Redis 原子扣减        |
| Lua 是否先判断库存再扣减    | `stock <= 0` 必须直接返回      |
| 订单表是否有唯一索引        | `uk_activity_user` 必须存在    |
| 消费端是否重复创建订单      | MQ 消费必须做幂等              |
| 数据库库存更新是否有条件    | `available_stock > 0` 必须保留 |

### 一人一单验证

一人一单需要验证同一个用户不会在同一个活动下生成多条订单。

压测后执行：

```sql
-- 如果查出数据，说明一人一单失效
SELECT
    activity_id,
    user_id,
    COUNT(*) AS order_count
FROM seckill_order
WHERE activity_id = 10001
GROUP BY activity_id, user_id
HAVING COUNT(*) > 1;
```

预期结果为空。

验证唯一索引是否存在：

```sql
SHOW INDEX FROM seckill_order WHERE Key_name = 'uk_activity_user';
```

预期可以看到：

```text
Key_name: uk_activity_user
Column_name: activity_id
Column_name: user_id
```

模拟同一个用户高并发重复提交：

```bash
seq 1 100 | xargs -n 1 -P 50 -I {} curl -s -X POST 'http://localhost:8080/api/seckill/purchase' \
  -H 'Content-Type: application/json' \
  -d '{"activityId":10001,"skuId":90001,"userId":20001}'
```

验证该用户订单数量：

```sql
SELECT COUNT(*) AS order_count
FROM seckill_order
WHERE activity_id = 10001
  AND user_id = 20001;
```

预期结果：

```text
order_count <= 1
```

同时检查 Redis 用户限购 Key：

```bash
redis-cli GET seckill:user:10001:20001
```

预期输出：

```text
"1"
```

如果该用户已经成功抢购或进入处理中状态，再次请求应该返回 `REPEAT`。

## 小结

本案例用最小核心链路实现了秒杀、抢购、抢号类业务的关键能力。实现上没有把订单、支付、风控、商品系统全部展开，而是聚焦高并发抢购最关键的几个点：Redis 原子扣减、一人一单、MQ 异步削峰、消费幂等、库存回补和结果查询。

### 本案例覆盖的核心能力

| 能力       | 实现方式                           |
| ---------- | ---------------------------------- |
| 活动预热   | MySQL 库存提前加载到 Redis         |
| 高并发扣减 | Redis Lua 原子判断和扣减库存       |
| 防超卖     | Lua 库存校验 + 数据库库存条件更新  |
| 一人一单   | Redis 用户限购 Key + 订单唯一索引  |
| 异步削峰   | 抢购成功后发送 RabbitMQ 消息       |
| 消费幂等   | MQ 消费记录表 + 订单唯一索引       |
| 结果查询   | Redis 结果 Key + MySQL 订单兜底    |
| 库存回补   | MQ 发送失败、消费失败时回补库存    |
| 压测验证   | JMeter / Shell 并发请求 + SQL 校验 |
| 最终一致性 | Redis、MQ、MySQL 多层协作          |

核心链路可以概括为：

```text
活动预热
-> Redis 写入活动库存
-> 用户抢购
-> Lua 原子扣减库存并写入用户限购标记
-> 写入抢购结果 PROCESSING
-> 发送 MQ
-> 消费者创建订单
-> 更新抢购结果 SUCCESS
-> 用户查询结果
```

失败链路可以概括为：

```text
Redis 扣减成功
-> MQ 发送失败或消费失败
-> 回补 Redis 库存
-> 删除用户限购标记
-> 更新抢购结果 FAIL
```

### 可扩展优化方向

当前案例已经能支撑核心秒杀链路，但生产环境还可以继续增强以下能力：

| 优化方向           | 说明                                                      |
| ------------------ | --------------------------------------------------------- |
| 网关限流           | 在 Spring Cloud Gateway、Nginx 或 Sentinel 层限制入口流量 |
| 用户风控           | 增加黑名单、设备指纹、IP 频率、账号等级校验               |
| 热点 Key 拆分      | 大流量活动可将库存拆成多个库存桶，降低单 Key 压力         |
| 本地缓存活动信息   | 活动时间、状态等低频变化信息可用 Caffeine 本地缓存        |
| 令牌桶发放         | 活动开始前按库存生成令牌，请求先抢令牌再进入下单链路      |
| 延迟重试队列       | MQ 消费失败后进入延迟队列，避免立即重试打爆数据库         |
| 死信人工补偿       | 死信消息落库后提供后台补偿入口                            |
| 库存对账任务       | 定时对比 Redis 库存、MySQL 库存、订单数量                 |
| WebSocket 推送结果 | 抢购结果生成后主动推送给用户，减少前端轮询                |
| 分库分表           | 大规模订单数据可按活动 ID 或用户 ID 分片                  |
| MQ 可靠消息表      | 抢购成功后先写本地消息表，再异步投递 MQ，提升消息可靠性   |
| 多活动隔离         | 不同活动使用独立队列、独立限流策略，避免互相影响          |

生产环境推荐最终形态：

```text
Nginx / Gateway 限流
-> 用户资格和风控校验
-> Redis Lua 原子扣减
-> 本地消息表
-> MQ 异步下单
-> 消费幂等
-> 库存回补
-> 定时对账
-> 死信补偿
-> 结果推送
```

这个案例的核心价值在于：用较少的代码覆盖了 Java 后端高并发业务中最常见的几个面试和实战考点，包括 Redis 原子操作、MQ 削峰、幂等控制、唯一索引兜底、最终一致性和异常补偿。