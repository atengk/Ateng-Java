# 库存 / 资源锁定与释放

本文基于“库存 / 资源锁定与释放”场景展开，重点覆盖查询可用资源、锁定库存、确认扣减、取消释放、超时释放、库存流水等核心能力，适用于商品库存、课程名额、票务、优惠券库存、会议室资源等业务。原始 README 中该场景的核心难点包括防超卖、并发扣减、资源锁定、重复释放幂等、库存流水、热点资源竞争和最终一致性。

## 场景说明

本案例以“课程名额库存”为例实现资源锁定与释放。用户下单时不直接扣减最终库存，而是先锁定名额；支付成功后再确认扣减；用户取消或订单超时未支付时释放锁定名额。

库存模型采用三段式设计：

```text
总库存 total_stock
锁定库存 locked_stock
已售库存 sold_stock

可用库存 available_stock = total_stock - locked_stock - sold_stock
```

这种模型比直接扣减库存更适合订单、支付、预约类业务，因为资源占用和最终成交之间通常存在时间差。

### 业务目标

本功能的目标是实现一套可落地的库存锁定与释放机制，保证在高并发下库存不超卖，且订单取消、支付超时、重复请求等场景能够安全处理。

核心目标如下：

| 目标         | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 防止超卖     | 并发锁定时，确保可用库存不足时不能继续占用             |
| 支持资源锁定 | 用户下单后先锁定库存，不立即确认售出                   |
| 支持确认扣减 | 支付成功后，将锁定库存转为已售库存                     |
| 支持主动释放 | 用户取消订单时，释放之前锁定的库存                     |
| 支持超时释放 | 订单长时间未支付时，自动释放锁定库存                   |
| 支持幂等处理 | 重复锁定、重复确认、重复释放不会造成库存异常           |
| 支持库存流水 | 每一次锁定、扣减、释放都记录库存变更流水，方便排查问题 |

实际业务中，库存异常通常不是由单次请求造成的，而是由并发、重试、回调重复、定时任务补偿、MQ 重复消费等问题共同导致。因此本案例会把“库存数值变更”和“业务操作记录”一起设计，而不是只写简单的 `update stock = stock - 1`。

### 核心流程

整体流程围绕“锁定记录”推进。每一次用户占用资源，都会生成一条锁定记录，后续确认扣减、取消释放、超时释放都基于这条记录进行状态流转。

核心流程如下：

```text
查询可用资源
-> 用户发起占用
-> 校验库存是否充足
-> 锁定库存
-> 创建库存锁定记录
-> 记录库存锁定流水
-> 等待订单支付

支付成功：
-> 查询锁定记录
-> 校验记录状态为已锁定
-> 锁定库存减少
-> 已售库存增加
-> 锁定记录变更为已确认
-> 记录确认扣减流水

用户取消：
-> 查询锁定记录
-> 校验记录状态为已锁定
-> 锁定库存减少
-> 锁定记录变更为已释放
-> 记录释放流水

支付超时：
-> 定时任务扫描过期锁定记录
-> 执行释放逻辑
-> 锁定库存减少
-> 锁定记录变更为已超时释放
-> 记录超时释放流水
```

对应的状态流转如下：

```text
LOCKED 已锁定
  -> CONFIRMED 已确认扣减
  -> RELEASED 已主动释放
  -> EXPIRED_RELEASED 已超时释放
```

状态约束：

| 当前状态         | 允许操作       | 目标状态         |
| ---------------- | -------------- | ---------------- |
| LOCKED           | 支付成功确认   | CONFIRMED        |
| LOCKED           | 用户取消释放   | RELEASED         |
| LOCKED           | 超时任务释放   | EXPIRED_RELEASED |
| CONFIRMED        | 不允许释放     | 保持不变         |
| RELEASED         | 不允许重复释放 | 保持不变         |
| EXPIRED_RELEASED | 不允许重复释放 | 保持不变         |

实现时会以数据库状态作为最终依据，Redis 主要用于高并发场景下的快速库存判断和原子锁定。数据库通过乐观锁和条件更新兜底，防止 Redis 异常、重复请求、补偿任务并发执行导致库存错误。

### 实现边界

本案例只实现“库存 / 资源锁定与释放”的核心能力，不扩展成完整订单系统或支付系统。订单、支付、用户权限、分布式事务等内容只保留必要的业务字段和调用入口。

本案例包含的内容：

| 模块          | 是否实现 | 说明                                     |
| ------------- | -------- | ---------------------------------------- |
| 库存查询      | 实现     | 查询总库存、锁定库存、已售库存、可用库存 |
| 库存锁定      | 实现     | 用户提交占用请求后锁定库存               |
| 支付确认扣减  | 实现     | 模拟支付成功后确认扣减锁定库存           |
| 用户取消释放  | 实现     | 用户主动取消后释放锁定库存               |
| 超时释放      | 实现     | 使用 XXL-JOB 或 Spring 定时任务扫描释放  |
| 库存流水      | 实现     | 记录锁定、确认、释放、超时释放流水       |
| 幂等控制      | 实现     | 通过业务单号唯一索引和状态判断处理       |
| 并发防超卖    | 实现     | Redis Lua + 数据库条件更新兜底           |
| MQ 最终一致性 | 可选     | 本案例先以同步实现为主，后续可扩展 MQ    |
| 完整订单系统  | 不实现   | 只使用 `bizNo` 模拟订单号                |
| 真实支付回调  | 不实现   | 只提供确认扣减接口模拟支付成功           |

推荐技术栈如下：

```text
Spring Boot 3
MyBatis-Plus
MySQL
Redis
Redisson
Lua 脚本
Hutool
Lombok
XXL-JOB / Spring Scheduler
```

为方便实操，后续代码会采用单体 Spring Boot 工程实现。核心重点放在库存数据结构、状态流转、并发扣减、释放幂等和流水记录上，不引入过多微服务组件。

## 技术方案

本节承接 README 中“库存 / 资源锁定与释放”的功能描述，重点围绕防超卖、资源锁定、重复释放幂等、库存流水和最终一致性展开。

### 技术栈选型

本案例采用单体 Spring Boot 工程实现核心逻辑，重点放在库存锁定与释放本身，不引入完整订单系统、支付系统和微服务治理组件。

| 技术                       | 用途                                   |
| -------------------------- | -------------------------------------- |
| Spring Boot 3              | 后端基础框架                           |
| MyBatis-Plus               | 数据库 CRUD、乐观锁支持                |
| MySQL                      | 持久化库存、锁定记录、库存流水         |
| Redis                      | 缓存可用库存，提升热点资源并发处理能力 |
| Lua 脚本                   | 保证 Redis 中库存判断和扣减的原子性    |
| Redisson                   | 分布式锁、Redis 客户端能力补充         |
| Hutool                     | 字符串、时间、JSON、集合等工具类       |
| Lombok                     | 简化实体、DTO、VO 编写                 |
| Spring Scheduler / XXL-JOB | 扫描超时未支付的锁定记录并释放资源     |

本案例的核心方案是：

```text
Redis Lua 原子锁定库存
+ MySQL 状态机记录锁定单
+ MySQL 条件更新兜底防超卖
+ 库存流水完整留痕
+ 定时任务释放超时锁定资源
```

### 核心设计思路

库存不要只设计一个 `stock` 字段，否则很难表达“已被占用但还未成交”的中间态。推荐拆分为总库存、锁定库存、已售库存。

```text
total_stock   总库存
locked_stock  已锁定库存
sold_stock    已售库存

available_stock = total_stock - locked_stock - sold_stock
```

用户下单时只增加 `locked_stock`，不增加 `sold_stock`。支付成功后，将 `locked_stock` 转移到 `sold_stock`。取消订单或支付超时时，减少 `locked_stock`。

核心原则如下：

| 原则           | 说明                                           |
| -------------- | ---------------------------------------------- |
| 先锁定，后确认 | 用户创建订单时只锁定库存，支付成功后才确认扣减 |
| 状态驱动       | 所有确认、释放、超时释放都基于锁定记录状态判断 |
| 幂等优先       | 重复请求不能导致库存重复扣减或重复释放         |
| Redis 提速     | 高并发锁定时先走 Redis Lua 原子扣减            |
| 数据库兜底     | Redis 成功后，数据库仍然使用条件更新防止脏数据 |
| 流水留痕       | 每次库存变化都记录流水，便于排查和补偿         |
| 定时补偿       | 超时未确认的锁定记录由任务自动释放             |

实际链路建议如下：

```text
锁定库存：
请求进入
-> 校验业务单号是否已存在
-> Redis Lua 判断可用库存并扣减
-> MySQL 条件更新 locked_stock
-> 创建锁定记录
-> 创建库存流水

确认扣减：
支付成功
-> 查询锁定记录
-> 判断状态是否 LOCKED
-> MySQL 减 locked_stock，加 sold_stock
-> 更新锁定记录为 CONFIRMED
-> 创建库存流水

释放库存：
用户取消 / 超时任务
-> 查询锁定记录
-> 判断状态是否 LOCKED
-> Redis 回补可用库存
-> MySQL 减 locked_stock
-> 更新锁定记录为 RELEASED / EXPIRED_RELEASED
-> 创建库存流水
```

### 状态流转设计

锁定记录是整个库存生命周期的核心，不能只依赖库存数值判断。推荐使用锁定记录状态控制业务流转。

| 状态             | 含义                     | 是否终态 |
| ---------------- | ------------------------ | -------- |
| LOCKED           | 已锁定，等待支付或确认   | 否       |
| CONFIRMED        | 已确认扣减，资源最终成交 | 是       |
| RELEASED         | 用户主动取消后释放       | 是       |
| EXPIRED_RELEASED | 支付超时后自动释放       | 是       |

状态流转如下：

```text
LOCKED
  -> CONFIRMED
  -> RELEASED
  -> EXPIRED_RELEASED
```

不允许的流转：

```text
CONFIRMED -> RELEASED
RELEASED -> CONFIRMED
EXPIRED_RELEASED -> CONFIRMED
RELEASED -> RELEASED
EXPIRED_RELEASED -> EXPIRED_RELEASED
```

实现时应使用 `where status = 'LOCKED'` 做状态条件更新，而不是先查询再无条件更新。这样可以天然保证并发场景下只有一个线程能成功推动状态。

示例：

```sql
-- 只有 LOCKED 状态才能释放，重复释放时 affected rows = 0
update resource_lock_record
set status = 'RELEASED',
    release_time = now(),
    update_time = now()
where id = #{id}
  and status = 'LOCKED';
```

## 表结构设计

本案例使用三张核心表：资源库存表、资源锁定记录表、资源变更流水表。库存表保存当前库存快照，锁定记录表保存每一次业务占用，流水表记录每一次库存变化。

### 资源库存表

资源库存表用于保存某个资源的库存快照，例如某个课程、某张票、某个优惠券批次、某个会议室时段。

这段 SQL 用于创建资源库存表，重点字段是总库存、锁定库存、已售库存和乐观锁版本号。

```sql
create table resource_stock (
    id bigint primary key auto_increment comment '主键ID',
    resource_type varchar(64) not null comment '资源类型：COURSE-课程，TICKET-票务，COUPON-优惠券',
    resource_id bigint not null comment '资源ID',
    resource_name varchar(128) not null comment '资源名称',
    total_stock int not null default 0 comment '总库存',
    locked_stock int not null default 0 comment '锁定库存',
    sold_stock int not null default 0 comment '已售库存',
    status tinyint not null default 1 comment '状态：1-启用，0-禁用',
    version int not null default 0 comment '乐观锁版本号',
    create_time datetime not null default current_timestamp comment '创建时间',
    update_time datetime not null default current_timestamp on update current_timestamp comment '更新时间',
    unique key uk_resource (resource_type, resource_id),
    key idx_status (status)
) engine = InnoDB default charset = utf8mb4 comment = '资源库存表';
```

字段说明：

| 字段          | 说明                                   |
| ------------- | -------------------------------------- |
| resource_type | 资源类型，用于区分课程、票务、优惠券等 |
| resource_id   | 具体资源 ID                            |
| total_stock   | 总库存                                 |
| locked_stock  | 已锁定但未最终成交的库存               |
| sold_stock    | 已确认成交的库存                       |
| version       | 乐观锁字段，用于并发更新兜底           |

可用库存不建议单独落库，直接通过公式计算：

```sql
total_stock - locked_stock - sold_stock
```

### 资源锁定记录表

资源锁定记录表用于保存每一次业务占用记录。后续确认扣减、用户取消、超时释放，都以这张表作为业务依据。

这段 SQL 用于创建资源锁定记录表，通过 `biz_no` 唯一索引保证同一个业务单号不会重复锁定。

```sql
create table resource_lock_record (
    id bigint primary key auto_increment comment '主键ID',
    lock_no varchar(64) not null comment '锁定单号',
    biz_no varchar(64) not null comment '业务单号，例如订单号',
    user_id bigint not null comment '用户ID',
    resource_type varchar(64) not null comment '资源类型',
    resource_id bigint not null comment '资源ID',
    lock_quantity int not null comment '锁定数量',
    status varchar(32) not null comment '状态：LOCKED，CONFIRMED，RELEASED，EXPIRED_RELEASED',
    expire_time datetime not null comment '锁定过期时间',
    confirm_time datetime null comment '确认扣减时间',
    release_time datetime null comment '释放时间',
    remark varchar(255) null comment '备注',
    create_time datetime not null default current_timestamp comment '创建时间',
    update_time datetime not null default current_timestamp on update current_timestamp comment '更新时间',
    unique key uk_lock_no (lock_no),
    unique key uk_biz_no (biz_no),
    key idx_resource (resource_type, resource_id),
    key idx_status_expire_time (status, expire_time),
    key idx_user_id (user_id)
) engine = InnoDB default charset = utf8mb4 comment = '资源锁定记录表';
```

关键约束：

| 约束                   | 作用                         |
| ---------------------- | ---------------------------- |
| uk_lock_no             | 保证锁定单号唯一             |
| uk_biz_no              | 保证一个业务单号只能锁定一次 |
| idx_status_expire_time | 支持定时任务扫描超时锁定记录 |
| idx_resource           | 支持按资源查询锁定情况       |

`biz_no` 通常来自订单号、预约单号、领取单号等业务编号。它是业务幂等的关键字段。

### 资源变更流水表

资源变更流水表用于记录每一次库存变化。出现库存不一致时，可以通过流水反查是哪一次锁定、确认或释放导致的。

这段 SQL 用于创建资源变更流水表，记录库存变化前后的关键数值和业务来源。

```sql
create table resource_stock_flow (
    id bigint primary key auto_increment comment '主键ID',
    flow_no varchar(64) not null comment '流水号',
    biz_no varchar(64) not null comment '业务单号',
    lock_no varchar(64) null comment '锁定单号',
    resource_type varchar(64) not null comment '资源类型',
    resource_id bigint not null comment '资源ID',
    change_type varchar(32) not null comment '变更类型：LOCK，CONFIRM，RELEASE，EXPIRE_RELEASE',
    change_quantity int not null comment '变更数量',
    before_total_stock int not null comment '变更前总库存',
    before_locked_stock int not null comment '变更前锁定库存',
    before_sold_stock int not null comment '变更前已售库存',
    after_total_stock int not null comment '变更后总库存',
    after_locked_stock int not null comment '变更后锁定库存',
    after_sold_stock int not null comment '变更后已售库存',
    remark varchar(255) null comment '备注',
    create_time datetime not null default current_timestamp comment '创建时间',
    unique key uk_flow_no (flow_no),
    key idx_biz_no (biz_no),
    key idx_lock_no (lock_no),
    key idx_resource (resource_type, resource_id),
    key idx_change_type (change_type)
) engine = InnoDB default charset = utf8mb4 comment = '资源库存变更流水表';
```

流水类型建议固定为枚举：

| 类型           | 说明             |
| -------------- | ---------------- |
| LOCK           | 锁定库存         |
| CONFIRM        | 支付成功确认扣减 |
| RELEASE        | 用户主动取消释放 |
| EXPIRE_RELEASE | 超时自动释放     |

流水表只追加，不修改，不删除。这样便于排查历史库存变化。

## Redis 设计

Redis 用于提升热点资源的并发锁定能力。锁定库存时，先通过 Lua 脚本在 Redis 中原子判断和扣减可用库存，再落 MySQL。MySQL 仍然是最终数据源。

### Redis Key 规划

Redis Key 需要具备明确的业务前缀、资源维度和过期策略。建议统一封装 Key 生成工具，不要在业务代码中散落字符串拼接。

| Key                                           | 类型        | 示例                               | 说明                              |
| --------------------------------------------- | ----------- | ---------------------------------- | --------------------------------- |
| `stock:available:{resourceType}:{resourceId}` | String      | `stock:available:COURSE:1001`      | Redis 中的可用库存                |
| `stock:lock:biz:{bizNo}`                      | String      | `stock:lock:biz:ORDER202605150001` | 业务锁定幂等标记                  |
| `stock:lock:record:{lockNo}`                  | Hash/String | `stock:lock:record:LK202605150001` | 可选，缓存锁定记录                |
| `lock:stock:{resourceType}:{resourceId}`      | Lock        | `lock:stock:COURSE:1001`           | Redisson 分布式锁，可用于补偿场景 |

推荐过期时间：

| Key              | 过期时间                     | 说明                       |
| ---------------- | ---------------------------- | -------------------------- |
| 可用库存 Key     | 1 天或不过期                 | 热点资源建议活动结束后清理 |
| 业务锁定幂等 Key | 与订单支付超时时间一致或略长 | 防止短时间重复提交         |
| 锁定记录缓存 Key | 与锁定过期时间一致           | 可选，不作为最终依据       |
| 分布式锁 Key     | 自动续期或短 TTL             | 只用于临界区保护           |

Redis 中的可用库存初始化方式：

```text
系统启动 / 活动预热 / 后台发布资源
-> 从 MySQL 查询 total_stock、locked_stock、sold_stock
-> 计算 available_stock
-> 写入 Redis
```

### Lua 原子锁定库存

普通 Redis 命令如果拆成 `get` 和 `decrby` 两步，在并发下仍然可能出现判断和扣减不一致。Lua 脚本可以保证判断库存、扣减库存、写入幂等标记在 Redis 单线程中原子执行。

这段 Lua 脚本用于锁定库存：校验业务单号是否重复，校验可用库存是否充足，充足时扣减 Redis 可用库存并写入幂等标记。

```lua
-- KEYS[1] 可用库存 Key，例如 stock:available:COURSE:1001
-- KEYS[2] 业务幂等 Key，例如 stock:lock:biz:ORDER202605150001
-- ARGV[1] 本次锁定数量
-- ARGV[2] 幂等 Key 过期秒数

local availableKey = KEYS[1]
local bizKey = KEYS[2]

local quantity = tonumber(ARGV[1])
local expireSeconds = tonumber(ARGV[2])

if redis.call('exists', bizKey) == 1 then
    return -2
end

local availableStock = tonumber(redis.call('get', availableKey))
if availableStock == nil then
    return -3
end

if availableStock < quantity then
    return -1
end

redis.call('decrby', availableKey, quantity)
redis.call('set', bizKey, 'LOCKED', 'EX', expireSeconds)

return availableStock - quantity
```

返回值建议统一定义：

| 返回值 | 说明                                  |
| ------ | ------------------------------------- |
| `>= 0` | 锁定成功，返回扣减后的 Redis 可用库存 |
| `-1`   | 库存不足                              |
| `-2`   | 业务单号重复提交                      |
| `-3`   | Redis 库存未初始化                    |

释放库存时，可以使用简单 Lua 脚本回补 Redis 可用库存，并删除或更新业务幂等标记。

这段 Lua 脚本用于释放库存：回补 Redis 可用库存，并将业务幂等 Key 标记为已释放。

```lua
-- KEYS[1] 可用库存 Key，例如 stock:available:COURSE:1001
-- KEYS[2] 业务幂等 Key，例如 stock:lock:biz:ORDER202605150001
-- ARGV[1] 本次释放数量
-- ARGV[2] 幂等 Key 过期秒数

local availableKey = KEYS[1]
local bizKey = KEYS[2]

local quantity = tonumber(ARGV[1])
local expireSeconds = tonumber(ARGV[2])

if redis.call('exists', availableKey) == 0 then
    return -1
end

redis.call('incrby', availableKey, quantity)
redis.call('set', bizKey, 'RELEASED', 'EX', expireSeconds)

return tonumber(redis.call('get', availableKey))
```

确认扣减时不需要回补 Redis，因为用户锁定时已经从 Redis 可用库存中扣减。支付成功只是把 MySQL 中的 `locked_stock` 转为 `sold_stock`。

### Redis 与数据库一致性策略

Redis 只作为高并发入口的快速库存层，MySQL 仍然是最终可信数据源。不能只扣 Redis 不写数据库，也不能只依赖 Redis 判断最终库存。

推荐一致性策略如下：

| 场景                           | 处理方式                                   |
| ------------------------------ | ------------------------------------------ |
| Redis 锁定成功，MySQL 更新成功 | 正常完成                                   |
| Redis 锁定成功，MySQL 更新失败 | 回补 Redis，并返回失败                     |
| Redis 库存未初始化             | 从 MySQL 重建 Redis 库存后重试，或直接失败 |
| MySQL 已确认扣减，Redis 不变   | 正常，确认扣减不回补 Redis                 |
| MySQL 释放成功，Redis 回补失败 | 记录告警，由补偿任务重建 Redis 库存        |
| Redis 与 MySQL 不一致          | 以 MySQL 为准，重新计算可用库存刷新 Redis  |

数据库锁定库存时必须使用条件更新：

```sql
update resource_stock
set locked_stock = locked_stock + #{quantity},
    version = version + 1,
    update_time = now()
where resource_type = #{resourceType}
  and resource_id = #{resourceId}
  and status = 1
  and total_stock - locked_stock - sold_stock >= #{quantity};
```

释放库存时也要做条件保护，避免锁定库存被扣成负数：

```sql
update resource_stock
set locked_stock = locked_stock - #{quantity},
    version = version + 1,
    update_time = now()
where resource_type = #{resourceType}
  and resource_id = #{resourceId}
  and locked_stock >= #{quantity};
```

确认扣减时将锁定库存转为已售库存：

```sql
update resource_stock
set locked_stock = locked_stock - #{quantity},
    sold_stock = sold_stock + #{quantity},
    version = version + 1,
    update_time = now()
where resource_type = #{resourceType}
  and resource_id = #{resourceId}
  and locked_stock >= #{quantity};
```

这种设计的关键点是：Redis 负责抗并发，MySQL 负责最终一致和可追溯。

## 后端工程搭建

本案例默认使用 Spring Boot 3、JDK 17、MySQL 8、Redis 7。包路径使用 `io.github.atengk.stock`，后续代码会围绕 Controller、Service、Mapper、Entity、DTO、VO 分层实现。

### Maven 依赖配置

`pom.xml` 中加入 Spring Boot、MyBatis-Plus、Redis、Redisson、Hutool、Lombok、MySQL 驱动等依赖。

这段 Maven 配置提供库存锁定案例所需的核心依赖。

```xml
<dependencies>
    <!-- Web 接口能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验，用于 DTO 字段校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- RedisTemplate 支持，用于执行 Lua 脚本和操作 Redis Key -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Redisson 分布式锁和高级 Redis 能力 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.27.2</version>
    </dependency>

    <!-- MyBatis-Plus，简化 CRUD 和分页操作 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool 工具类，用于日期、字符串、JSON、ID 等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>

    <!-- Lombok，减少 Getter、Setter、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 单元测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

推荐插件配置：

```xml
<build>
    <plugins>
        <!-- Spring Boot 打包插件 -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

### application 配置

`application.yml` 中配置 MySQL、Redis、MyBatis-Plus、日志等基础参数。库存锁定超时时间也建议配置化，避免写死在代码里。

文件位置：`src/main/resources/application.yml`

这段配置用于连接 MySQL、Redis，并定义库存锁定业务参数。

```yaml
server:
  port: 8080

spring:
  application:
    name: stock-lock-demo

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/stock_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3s
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: io.github.atengk.stock.entity
  configuration:
    # SQL 下划线字段自动映射 Java 驼峰属性
    map-underscore-to-camel-case: true
    # 开发环境可开启 SQL 日志，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 雪花算法生成主键
      id-type: assign_id
      # 逻辑删除字段可按需扩展，本案例暂不启用
      logic-delete-value: 1
      logic-not-delete-value: 0

stock:
  lock:
    # 默认锁定 15 分钟，超过时间未确认则自动释放
    expire-minutes: 15
    # Redis 业务幂等 Key 过期时间，建议略大于订单支付超时时间
    biz-key-expire-seconds: 1800
    # 超时释放任务每次处理数量
    expire-scan-limit: 100

logging:
  level:
    io.github.atengk.stock: info
    com.baomidou.mybatisplus: warn
```

### MyBatis-Plus 基础配置

MyBatis-Plus 需要开启 Mapper 扫描。如果使用乐观锁插件，也可以在配置类中加入拦截器。本案例的主要并发控制依靠条件更新，乐观锁作为补充能力保留。

文件位置：`src/main/java/io/github/atengk/stock/config/MybatisPlusConfig.java`

这个配置类用于开启 MyBatis-Plus 分页插件和乐观锁插件。

```java
package io.github.atengk.stock.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 基础配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@MapperScan("io.github.atengk.stock.mapper")
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 拦截器
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 乐观锁插件，用于带 @Version 字段的更新场景
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页插件，后续库存流水分页查询可以复用
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
```

如果后续实体类中使用 `@Version`，字段应与表中的 `version` 对应：

```java
@Version
private Integer version;
```

不过对于本案例的核心库存扣减，仍然建议优先使用 SQL 条件更新：

```sql
and total_stock - locked_stock - sold_stock >= #{quantity}
```

原因是库存锁定更关注“库存是否足够”这个业务条件，而不仅是版本号是否匹配。

## 核心代码实现

本节实现库存锁定、确认扣减、释放、超时释放和库存流水记录。代码基于前文三张表：`resource_stock`、`resource_lock_record`、`resource_stock_flow`，核心目标对应 README 中的防超卖、资源锁定、资源释放、重复释放幂等和库存流水。

### 库存实体与枚举

先定义实体类、枚举、返回对象和业务异常。实体字段与前面的 SQL 表结构保持一致。

文件位置：`src/main/java/io/github/atengk/stock/entity/ResourceStock.java`

```java
package io.github.atengk.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源库存实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("resource_stock")
public class ResourceStock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String resourceType;

    private Long resourceId;

    private String resourceName;

    private Integer totalStock;

    private Integer lockedStock;

    private Integer soldStock;

    private Integer status;

    @Version
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/stock/entity/ResourceLockRecord.java`

```java
package io.github.atengk.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源锁定记录实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("resource_lock_record")
public class ResourceLockRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String lockNo;

    private String bizNo;

    private Long userId;

    private String resourceType;

    private Long resourceId;

    private Integer lockQuantity;

    private String status;

    private LocalDateTime expireTime;

    private LocalDateTime confirmTime;

    private LocalDateTime releaseTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/stock/entity/ResourceStockFlow.java`

```java
package io.github.atengk.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源库存变更流水实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("resource_stock_flow")
public class ResourceStockFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String flowNo;

    private String bizNo;

    private String lockNo;

    private String resourceType;

    private Long resourceId;

    private String changeType;

    private Integer changeQuantity;

    private Integer beforeTotalStock;

    private Integer beforeLockedStock;

    private Integer beforeSoldStock;

    private Integer afterTotalStock;

    private Integer afterLockedStock;

    private Integer afterSoldStock;

    private String remark;

    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/stock/enums/ResourceLockStatusEnum.java`

```java
package io.github.atengk.stock.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资源锁定状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum ResourceLockStatusEnum {

    LOCKED("LOCKED", "已锁定"),

    CONFIRMED("CONFIRMED", "已确认扣减"),

    RELEASED("RELEASED", "已主动释放"),

    EXPIRED_RELEASED("EXPIRED_RELEASED", "已超时释放");

    private final String code;

    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/stock/enums/StockFlowTypeEnum.java`

```java
package io.github.atengk.stock.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存流水类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum StockFlowTypeEnum {

    LOCK("LOCK", "锁定库存"),

    CONFIRM("CONFIRM", "确认扣减"),

    RELEASE("RELEASE", "主动释放"),

    EXPIRE_RELEASE("EXPIRE_RELEASE", "超时释放");

    private final String code;

    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/stock/vo/AvailableStockVO.java`

```java
package io.github.atengk.stock.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 可用库存返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class AvailableStockVO {

    private String resourceType;

    private Long resourceId;

    private String resourceName;

    private Integer totalStock;

    private Integer lockedStock;

    private Integer soldStock;

    private Integer availableStock;
}
```

文件位置：`src/main/java/io/github/atengk/stock/exception/StockException.java`

```java
package io.github.atengk.stock.exception;

/**
 * 库存业务异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class StockException extends RuntimeException {

    public StockException(String message) {
        super(message);
    }
}
```

文件位置：`src/main/java/io/github/atengk/stock/config/StockLockProperties.java`

```java
package io.github.atengk.stock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 库存锁定配置属性
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "stock.lock")
public class StockLockProperties {

    /**
     * 默认锁定分钟数
     */
    private Integer expireMinutes = 15;

    /**
     * 业务幂等 Key 过期秒数
     */
    private Long bizKeyExpireSeconds = 1800L;

    /**
     * 超时释放任务每次扫描数量
     */
    private Integer expireScanLimit = 100;
}
```

### Mapper 与 Service 接口

Mapper 中保留常规 CRUD，同时补充几个关键 SQL：库存行锁查询、库存锁定、确认扣减、释放锁定库存、锁定记录行锁查询、扫描超时锁定记录。

文件位置：`src/main/java/io/github/atengk/stock/mapper/ResourceStockMapper.java`

```java
package io.github.atengk.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.stock.entity.ResourceStock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 资源库存 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ResourceStockMapper extends BaseMapper<ResourceStock> {

    /**
     * 根据资源查询库存并加行锁
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @return 资源库存
     */
    @Select("""
            select *
            from resource_stock
            where resource_type = #{resourceType}
              and resource_id = #{resourceId}
              and status = 1
            for update
            """)
    ResourceStock selectByResourceForUpdate(@Param("resourceType") String resourceType,
                                            @Param("resourceId") Long resourceId);

    /**
     * 增加锁定库存
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param quantity     数量
     * @return 影响行数
     */
    @Update("""
            update resource_stock
            set locked_stock = locked_stock + #{quantity},
                version = version + 1,
                update_time = now()
            where resource_type = #{resourceType}
              and resource_id = #{resourceId}
              and status = 1
              and total_stock - locked_stock - sold_stock >= #{quantity}
            """)
    int increaseLockedStock(@Param("resourceType") String resourceType,
                            @Param("resourceId") Long resourceId,
                            @Param("quantity") Integer quantity);

    /**
     * 确认扣减锁定库存
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param quantity     数量
     * @return 影响行数
     */
    @Update("""
            update resource_stock
            set locked_stock = locked_stock - #{quantity},
                sold_stock = sold_stock + #{quantity},
                version = version + 1,
                update_time = now()
            where resource_type = #{resourceType}
              and resource_id = #{resourceId}
              and locked_stock >= #{quantity}
            """)
    int confirmLockedStock(@Param("resourceType") String resourceType,
                           @Param("resourceId") Long resourceId,
                           @Param("quantity") Integer quantity);

    /**
     * 释放锁定库存
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param quantity     数量
     * @return 影响行数
     */
    @Update("""
            update resource_stock
            set locked_stock = locked_stock - #{quantity},
                version = version + 1,
                update_time = now()
            where resource_type = #{resourceType}
              and resource_id = #{resourceId}
              and locked_stock >= #{quantity}
            """)
    int releaseLockedStock(@Param("resourceType") String resourceType,
                           @Param("resourceId") Long resourceId,
                           @Param("quantity") Integer quantity);
}
```

文件位置：`src/main/java/io/github/atengk/stock/mapper/ResourceLockRecordMapper.java`

```java
package io.github.atengk.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.stock.entity.ResourceLockRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资源锁定记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ResourceLockRecordMapper extends BaseMapper<ResourceLockRecord> {

    /**
     * 根据业务单号查询锁定记录并加行锁
     *
     * @param bizNo 业务单号
     * @return 锁定记录
     */
    @Select("""
            select *
            from resource_lock_record
            where biz_no = #{bizNo}
            for update
            """)
    ResourceLockRecord selectByBizNoForUpdate(@Param("bizNo") String bizNo);

    /**
     * 扫描超时未释放的锁定记录
     *
     * @param now   当前时间
     * @param limit 查询数量
     * @return 锁定记录列表
     */
    @Select("""
            select *
            from resource_lock_record
            where status = 'LOCKED'
              and expire_time <= #{now}
            order by expire_time asc
            limit #{limit}
            """)
    List<ResourceLockRecord> selectExpiredLockedList(@Param("now") LocalDateTime now,
                                                     @Param("limit") Integer limit);
}
```

文件位置：`src/main/java/io/github/atengk/stock/mapper/ResourceStockFlowMapper.java`

```java
package io.github.atengk.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.stock.entity.ResourceStockFlow;

/**
 * 资源库存流水 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ResourceStockFlowMapper extends BaseMapper<ResourceStockFlow> {
}
```

文件位置：`src/main/java/io/github/atengk/stock/service/StockLockService.java`

```java
package io.github.atengk.stock.service;

import io.github.atengk.stock.vo.AvailableStockVO;

/**
 * 库存锁定服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface StockLockService {

    /**
     * 查询可用库存
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @return 可用库存信息
     */
    AvailableStockVO queryAvailableStock(String resourceType, Long resourceId);

    /**
     * 锁定库存
     *
     * @param bizNo        业务单号
     * @param userId       用户ID
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param quantity     锁定数量
     * @return 锁定单号
     */
    String lockStock(String bizNo, Long userId, String resourceType, Long resourceId, Integer quantity);

    /**
     * 支付成功确认扣减
     *
     * @param bizNo 业务单号
     */
    void confirmStock(String bizNo);

    /**
     * 用户取消释放库存
     *
     * @param bizNo 业务单号
     */
    void releaseStock(String bizNo);

    /**
     * 释放超时未支付库存
     *
     * @return 释放数量
     */
    int releaseExpiredStock();
}
```

### 查询可用库存

查询可用库存只读取 MySQL 快照并计算 `totalStock - lockedStock - soldStock`。Redis 用于高并发锁定，不作为库存查询的唯一依据。

### 锁定库存

锁定库存流程为：校验参数、判断业务单是否已处理、Redis Lua 原子扣减、MySQL 增加锁定库存、写入锁定记录、写入库存流水。如果 MySQL 失败，需要回滚 Redis。

### 支付成功确认扣减

确认扣减流程为：根据业务单号查询锁定记录，加行锁，确认状态为 `LOCKED`，将 MySQL 中的 `locked_stock` 转为 `sold_stock`，更新锁定记录为 `CONFIRMED`，写入流水。

### 用户取消释放库存

释放库存流程为：查询锁定记录，加行锁，只允许 `LOCKED` 状态释放，MySQL 减少锁定库存，更新锁定记录为 `RELEASED`，回补 Redis 可用库存，写入流水。重复释放时直接幂等返回。

### 超时未支付自动释放

超时释放通过定时任务扫描 `status = LOCKED and expire_time <= now()` 的记录，然后复用释放逻辑，只是目标状态和流水类型不同。

### 资源变更流水记录

下面是核心 Service 实现，包含查询、锁定、确认、释放、超时释放和流水记录。

文件位置：`src/main/java/io/github/atengk/stock/service/impl/StockLockServiceImpl.java`

```java
package io.github.atengk.stock.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.stock.config.StockLockProperties;
import io.github.atengk.stock.entity.ResourceLockRecord;
import io.github.atengk.stock.entity.ResourceStock;
import io.github.atengk.stock.entity.ResourceStockFlow;
import io.github.atengk.stock.enums.ResourceLockStatusEnum;
import io.github.atengk.stock.enums.StockFlowTypeEnum;
import io.github.atengk.stock.exception.StockException;
import io.github.atengk.stock.mapper.ResourceLockRecordMapper;
import io.github.atengk.stock.mapper.ResourceStockFlowMapper;
import io.github.atengk.stock.mapper.ResourceStockMapper;
import io.github.atengk.stock.service.StockLockService;
import io.github.atengk.stock.vo.AvailableStockVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 库存锁定服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockLockServiceImpl implements StockLockService {

    private static final String STOCK_AVAILABLE_KEY = "stock:available:{}:{}";

    private static final String STOCK_LOCK_BIZ_KEY = "stock:lock:biz:{}";

    private static final String LOCK_STOCK_LUA = """
            local availableKey = KEYS[1]
            local bizKey = KEYS[2]
            
            local quantity = tonumber(ARGV[1])
            local expireSeconds = tonumber(ARGV[2])
            
            if redis.call('exists', bizKey) == 1 then
                return -2
            end
            
            local availableStock = tonumber(redis.call('get', availableKey))
            if availableStock == nil then
                return -3
            end
            
            if availableStock < quantity then
                return -1
            end
            
            redis.call('decrby', availableKey, quantity)
            redis.call('set', bizKey, 'LOCKED', 'EX', expireSeconds)
            
            return availableStock - quantity
            """;

    private static final String ROLLBACK_LOCK_LUA = """
            local availableKey = KEYS[1]
            local bizKey = KEYS[2]
            
            local quantity = tonumber(ARGV[1])
            
            if redis.call('exists', availableKey) == 1 then
                redis.call('incrby', availableKey, quantity)
            end
            
            redis.call('del', bizKey)
            
            return 1
            """;

    private static final String RELEASE_STOCK_LUA = """
            local availableKey = KEYS[1]
            local bizKey = KEYS[2]
            
            local quantity = tonumber(ARGV[1])
            local status = ARGV[2]
            local expireSeconds = tonumber(ARGV[3])
            
            if redis.call('exists', availableKey) == 0 then
                return -1
            end
            
            redis.call('incrby', availableKey, quantity)
            redis.call('set', bizKey, status, 'EX', expireSeconds)
            
            return tonumber(redis.call('get', availableKey))
            """;

    private final ResourceStockMapper resourceStockMapper;

    private final ResourceLockRecordMapper resourceLockRecordMapper;

    private final ResourceStockFlowMapper resourceStockFlowMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final StockLockProperties stockLockProperties;

    /**
     * 查询可用库存
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @return 可用库存信息
     */
    @Override
    public AvailableStockVO queryAvailableStock(String resourceType, Long resourceId) {
        ResourceStock stock = resourceStockMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResourceStock>()
                        .eq(ResourceStock::getResourceType, resourceType)
                        .eq(ResourceStock::getResourceId, resourceId)
                        .eq(ResourceStock::getStatus, 1)
        );

        if (ObjectUtil.isNull(stock)) {
            throw new StockException("资源库存不存在");
        }

        Integer availableStock = calculateAvailableStock(stock);

        return AvailableStockVO.builder()
                .resourceType(stock.getResourceType())
                .resourceId(stock.getResourceId())
                .resourceName(stock.getResourceName())
                .totalStock(stock.getTotalStock())
                .lockedStock(stock.getLockedStock())
                .soldStock(stock.getSoldStock())
                .availableStock(availableStock)
                .build();
    }

    /**
     * 锁定库存
     *
     * @param bizNo        业务单号
     * @param userId       用户ID
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param quantity     锁定数量
     * @return 锁定单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String lockStock(String bizNo, Long userId, String resourceType, Long resourceId, Integer quantity) {
        checkLockParam(bizNo, userId, resourceType, resourceId, quantity);

        ResourceLockRecord existRecord = resourceLockRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResourceLockRecord>()
                        .eq(ResourceLockRecord::getBizNo, bizNo)
        );
        if (ObjectUtil.isNotNull(existRecord)) {
            log.info("业务单已存在锁定记录，直接返回锁定单号，bizNo={}，lockNo={}", bizNo, existRecord.getLockNo());
            return existRecord.getLockNo();
        }

        String availableKey = buildAvailableKey(resourceType, resourceId);
        String bizKey = buildBizKey(bizNo);
        boolean redisLocked = false;

        try {
            Long redisResult = executeLockLua(availableKey, bizKey, quantity);
            handleLockLuaResult(redisResult);
            redisLocked = true;

            ResourceStock beforeStock = resourceStockMapper.selectByResourceForUpdate(resourceType, resourceId);
            if (ObjectUtil.isNull(beforeStock)) {
                throw new StockException("资源库存不存在或已禁用");
            }

            int updateRows = resourceStockMapper.increaseLockedStock(resourceType, resourceId, quantity);
            if (updateRows != 1) {
                throw new StockException("库存不足，锁定失败");
            }

            String lockNo = "LK" + IdUtil.getSnowflakeNextIdStr();
            LocalDateTime now = now();
            LocalDateTime expireTime = DateUtil.offsetMinute(DateUtil.date(), stockLockProperties.getExpireMinutes())
                    .toLocalDateTime();

            ResourceLockRecord record = new ResourceLockRecord();
            record.setLockNo(lockNo);
            record.setBizNo(bizNo);
            record.setUserId(userId);
            record.setResourceType(resourceType);
            record.setResourceId(resourceId);
            record.setLockQuantity(quantity);
            record.setStatus(ResourceLockStatusEnum.LOCKED.getCode());
            record.setExpireTime(expireTime);
            record.setRemark("用户发起资源锁定");
            record.setCreateTime(now);
            record.setUpdateTime(now);
            resourceLockRecordMapper.insert(record);

            ResourceStock afterStock = copyAfterLock(beforeStock, quantity);
            saveStockFlow(beforeStock, afterStock, bizNo, lockNo, StockFlowTypeEnum.LOCK, quantity, "锁定库存");

            log.info("库存锁定成功，bizNo={}，lockNo={}，resourceType={}，resourceId={}，quantity={}",
                    bizNo, lockNo, resourceType, resourceId, quantity);
            return lockNo;
        } catch (RuntimeException ex) {
            if (redisLocked) {
                rollbackRedisLock(availableKey, bizKey, quantity);
            }
            throw ex;
        }
    }

    /**
     * 支付成功确认扣减
     *
     * @param bizNo 业务单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmStock(String bizNo) {
        ResourceLockRecord record = resourceLockRecordMapper.selectByBizNoForUpdate(bizNo);
        if (ObjectUtil.isNull(record)) {
            throw new StockException("锁定记录不存在");
        }

        if (Objects.equals(record.getStatus(), ResourceLockStatusEnum.CONFIRMED.getCode())) {
            log.info("库存已确认扣减，幂等返回，bizNo={}", bizNo);
            return;
        }

        if (!Objects.equals(record.getStatus(), ResourceLockStatusEnum.LOCKED.getCode())) {
            throw new StockException("当前锁定记录状态不允许确认扣减");
        }

        ResourceStock beforeStock = resourceStockMapper.selectByResourceForUpdate(record.getResourceType(), record.getResourceId());
        if (ObjectUtil.isNull(beforeStock)) {
            throw new StockException("资源库存不存在或已禁用");
        }

        int updateRows = resourceStockMapper.confirmLockedStock(
                record.getResourceType(),
                record.getResourceId(),
                record.getLockQuantity()
        );
        if (updateRows != 1) {
            throw new StockException("确认扣减失败，锁定库存不足");
        }

        LocalDateTime now = now();
        record.setStatus(ResourceLockStatusEnum.CONFIRMED.getCode());
        record.setConfirmTime(now);
        record.setUpdateTime(now);
        resourceLockRecordMapper.updateById(record);

        ResourceStock afterStock = copyAfterConfirm(beforeStock, record.getLockQuantity());
        saveStockFlow(beforeStock, afterStock, record.getBizNo(), record.getLockNo(),
                StockFlowTypeEnum.CONFIRM, record.getLockQuantity(), "支付成功确认扣减");

        stringRedisTemplate.opsForValue().set(
                buildBizKey(record.getBizNo()),
                ResourceLockStatusEnum.CONFIRMED.getCode(),
                stockLockProperties.getBizKeyExpireSeconds(),
                TimeUnit.SECONDS
        );

        log.info("库存确认扣减成功，bizNo={}，lockNo={}，quantity={}",
                record.getBizNo(), record.getLockNo(), record.getLockQuantity());
    }

    /**
     * 用户取消释放库存
     *
     * @param bizNo 业务单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseStock(String bizNo) {
        releaseStockInternal(bizNo, ResourceLockStatusEnum.RELEASED, StockFlowTypeEnum.RELEASE, "用户取消释放库存");
    }

    /**
     * 释放超时未支付库存
     *
     * @return 释放数量
     */
    @Override
    public int releaseExpiredStock() {
        List<ResourceLockRecord> expiredRecords = resourceLockRecordMapper.selectExpiredLockedList(
                now(),
                stockLockProperties.getExpireScanLimit()
        );

        int successCount = 0;
        for (ResourceLockRecord record : expiredRecords) {
            try {
                releaseStockInternal(record.getBizNo(), ResourceLockStatusEnum.EXPIRED_RELEASED,
                        StockFlowTypeEnum.EXPIRE_RELEASE, "支付超时自动释放库存");
                successCount++;
            } catch (RuntimeException ex) {
                log.warn("超时释放库存失败，bizNo={}，原因={}", record.getBizNo(), ex.getMessage());
            }
        }

        log.info("超时库存释放完成，本次扫描={}，成功释放={}", expiredRecords.size(), successCount);
        return successCount;
    }

    /**
     * 内部释放库存
     *
     * @param bizNo        业务单号
     * @param targetStatus 目标状态
     * @param flowType     流水类型
     * @param remark       备注
     */
    private void releaseStockInternal(String bizNo,
                                      ResourceLockStatusEnum targetStatus,
                                      StockFlowTypeEnum flowType,
                                      String remark) {
        ResourceLockRecord record = resourceLockRecordMapper.selectByBizNoForUpdate(bizNo);
        if (ObjectUtil.isNull(record)) {
            throw new StockException("锁定记录不存在");
        }

        if (Objects.equals(record.getStatus(), targetStatus.getCode())) {
            log.info("库存已释放，幂等返回，bizNo={}，status={}", bizNo, targetStatus.getCode());
            return;
        }

        if (!Objects.equals(record.getStatus(), ResourceLockStatusEnum.LOCKED.getCode())) {
            log.info("当前状态无需释放，幂等返回，bizNo={}，status={}", bizNo, record.getStatus());
            return;
        }

        ResourceStock beforeStock = resourceStockMapper.selectByResourceForUpdate(record.getResourceType(), record.getResourceId());
        if (ObjectUtil.isNull(beforeStock)) {
            throw new StockException("资源库存不存在或已禁用");
        }

        int updateRows = resourceStockMapper.releaseLockedStock(
                record.getResourceType(),
                record.getResourceId(),
                record.getLockQuantity()
        );
        if (updateRows != 1) {
            throw new StockException("释放库存失败，锁定库存不足");
        }

        LocalDateTime now = now();
        record.setStatus(targetStatus.getCode());
        record.setReleaseTime(now);
        record.setUpdateTime(now);
        resourceLockRecordMapper.updateById(record);

        ResourceStock afterStock = copyAfterRelease(beforeStock, record.getLockQuantity());
        saveStockFlow(beforeStock, afterStock, record.getBizNo(), record.getLockNo(),
                flowType, record.getLockQuantity(), remark);

        Long redisResult = executeReleaseLua(
                buildAvailableKey(record.getResourceType(), record.getResourceId()),
                buildBizKey(record.getBizNo()),
                record.getLockQuantity(),
                targetStatus.getCode()
        );

        if (Objects.equals(redisResult, -1L)) {
            log.warn("Redis 可用库存 Key 不存在，建议触发库存缓存重建，bizNo={}，resourceType={}，resourceId={}",
                    record.getBizNo(), record.getResourceType(), record.getResourceId());
        }

        log.info("库存释放成功，bizNo={}，lockNo={}，status={}，quantity={}",
                record.getBizNo(), record.getLockNo(), targetStatus.getCode(), record.getLockQuantity());
    }

    /**
     * 保存库存流水
     *
     * @param beforeStock    变更前库存
     * @param afterStock     变更后库存
     * @param bizNo          业务单号
     * @param lockNo         锁定单号
     * @param flowType       流水类型
     * @param changeQuantity 变更数量
     * @param remark         备注
     */
    private void saveStockFlow(ResourceStock beforeStock,
                               ResourceStock afterStock,
                               String bizNo,
                               String lockNo,
                               StockFlowTypeEnum flowType,
                               Integer changeQuantity,
                               String remark) {
        ResourceStockFlow flow = new ResourceStockFlow();
        flow.setFlowNo("SF" + IdUtil.getSnowflakeNextIdStr());
        flow.setBizNo(bizNo);
        flow.setLockNo(lockNo);
        flow.setResourceType(beforeStock.getResourceType());
        flow.setResourceId(beforeStock.getResourceId());
        flow.setChangeType(flowType.getCode());
        flow.setChangeQuantity(changeQuantity);
        flow.setBeforeTotalStock(beforeStock.getTotalStock());
        flow.setBeforeLockedStock(beforeStock.getLockedStock());
        flow.setBeforeSoldStock(beforeStock.getSoldStock());
        flow.setAfterTotalStock(afterStock.getTotalStock());
        flow.setAfterLockedStock(afterStock.getLockedStock());
        flow.setAfterSoldStock(afterStock.getSoldStock());
        flow.setRemark(remark);
        flow.setCreateTime(now());
        resourceStockFlowMapper.insert(flow);
    }

    /**
     * 执行锁定库存 Lua
     *
     * @param availableKey 可用库存 Key
     * @param bizKey       业务幂等 Key
     * @param quantity     数量
     * @return Lua 返回值
     */
    private Long executeLockLua(String availableKey, String bizKey, Integer quantity) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LOCK_STOCK_LUA, Long.class);
        return stringRedisTemplate.execute(
                script,
                List.of(availableKey, bizKey),
                String.valueOf(quantity),
                String.valueOf(stockLockProperties.getBizKeyExpireSeconds())
        );
    }

    /**
     * 执行释放库存 Lua
     *
     * @param availableKey 可用库存 Key
     * @param bizKey       业务幂等 Key
     * @param quantity     数量
     * @param status       状态
     * @return Lua 返回值
     */
    private Long executeReleaseLua(String availableKey, String bizKey, Integer quantity, String status) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_STOCK_LUA, Long.class);
        return stringRedisTemplate.execute(
                script,
                List.of(availableKey, bizKey),
                String.valueOf(quantity),
                status,
                String.valueOf(stockLockProperties.getBizKeyExpireSeconds())
        );
    }

    /**
     * 回滚 Redis 锁定结果
     *
     * @param availableKey 可用库存 Key
     * @param bizKey       业务幂等 Key
     * @param quantity     数量
     */
    private void rollbackRedisLock(String availableKey, String bizKey, Integer quantity) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(ROLLBACK_LOCK_LUA, Long.class);
        stringRedisTemplate.execute(script, List.of(availableKey, bizKey), String.valueOf(quantity));
        log.warn("数据库锁定失败，已回滚 Redis 库存，availableKey={}，bizKey={}，quantity={}",
                availableKey, bizKey, quantity);
    }

    /**
     * 处理 Redis 锁定返回值
     *
     * @param redisResult Redis 返回值
     */
    private void handleLockLuaResult(Long redisResult) {
        if (ObjectUtil.isNull(redisResult)) {
            throw new StockException("Redis 库存锁定失败");
        }
        if (Objects.equals(redisResult, -1L)) {
            throw new StockException("库存不足");
        }
        if (Objects.equals(redisResult, -2L)) {
            throw new StockException("业务单号重复提交");
        }
        if (Objects.equals(redisResult, -3L)) {
            throw new StockException("Redis 库存未初始化");
        }
    }

    /**
     * 校验锁定参数
     *
     * @param bizNo        业务单号
     * @param userId       用户ID
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param quantity     数量
     */
    private void checkLockParam(String bizNo, Long userId, String resourceType, Long resourceId, Integer quantity) {
        if (StrUtil.isBlank(bizNo)) {
            throw new StockException("业务单号不能为空");
        }
        if (ObjectUtil.isNull(userId)) {
            throw new StockException("用户ID不能为空");
        }
        if (StrUtil.isBlank(resourceType)) {
            throw new StockException("资源类型不能为空");
        }
        if (ObjectUtil.isNull(resourceId)) {
            throw new StockException("资源ID不能为空");
        }
        if (ObjectUtil.isNull(quantity) || quantity <= 0) {
            throw new StockException("锁定数量必须大于0");
        }
    }

    /**
     * 计算可用库存
     *
     * @param stock 库存
     * @return 可用库存
     */
    private Integer calculateAvailableStock(ResourceStock stock) {
        return stock.getTotalStock() - stock.getLockedStock() - stock.getSoldStock();
    }

    /**
     * 构建可用库存 Key
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @return Redis Key
     */
    private String buildAvailableKey(String resourceType, Long resourceId) {
        return StrUtil.format(STOCK_AVAILABLE_KEY, resourceType, resourceId);
    }

    /**
     * 构建业务幂等 Key
     *
     * @param bizNo 业务单号
     * @return Redis Key
     */
    private String buildBizKey(String bizNo) {
        return StrUtil.format(STOCK_LOCK_BIZ_KEY, bizNo);
    }

    /**
     * 获取当前时间
     *
     * @return 当前时间
     */
    private LocalDateTime now() {
        return DateUtil.date().toLocalDateTime();
    }

    /**
     * 复制锁定后的库存快照
     *
     * @param beforeStock 变更前库存
     * @param quantity    数量
     * @return 变更后库存
     */
    private ResourceStock copyAfterLock(ResourceStock beforeStock, Integer quantity) {
        ResourceStock afterStock = copyStock(beforeStock);
        afterStock.setLockedStock(beforeStock.getLockedStock() + quantity);
        return afterStock;
    }

    /**
     * 复制确认后的库存快照
     *
     * @param beforeStock 变更前库存
     * @param quantity    数量
     * @return 变更后库存
     */
    private ResourceStock copyAfterConfirm(ResourceStock beforeStock, Integer quantity) {
        ResourceStock afterStock = copyStock(beforeStock);
        afterStock.setLockedStock(beforeStock.getLockedStock() - quantity);
        afterStock.setSoldStock(beforeStock.getSoldStock() + quantity);
        return afterStock;
    }

    /**
     * 复制释放后的库存快照
     *
     * @param beforeStock 变更前库存
     * @param quantity    数量
     * @return 变更后库存
     */
    private ResourceStock copyAfterRelease(ResourceStock beforeStock, Integer quantity) {
        ResourceStock afterStock = copyStock(beforeStock);
        afterStock.setLockedStock(beforeStock.getLockedStock() - quantity);
        return afterStock;
    }

    /**
     * 复制库存对象
     *
     * @param stock 原库存
     * @return 库存副本
     */
    private ResourceStock copyStock(ResourceStock stock) {
        ResourceStock copy = new ResourceStock();
        copy.setId(stock.getId());
        copy.setResourceType(stock.getResourceType());
        copy.setResourceId(stock.getResourceId());
        copy.setResourceName(stock.getResourceName());
        copy.setTotalStock(stock.getTotalStock());
        copy.setLockedStock(stock.getLockedStock());
        copy.setSoldStock(stock.getSoldStock());
        copy.setStatus(stock.getStatus());
        copy.setVersion(stock.getVersion());
        return copy;
    }
}
```

超时释放可以直接用 Spring Scheduler。生产环境也可以替换为 XXL-JOB，业务逻辑仍然复用 `releaseExpiredStock()`。

文件位置：`src/main/java/io/github/atengk/stock/job/StockExpireReleaseJob.java`

```java
package io.github.atengk.stock.job;

import io.github.atengk.stock.service.StockLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 库存超时释放任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockExpireReleaseJob {

    private final StockLockService stockLockService;

    /**
     * 每分钟释放一次超时未支付库存
     */
    @Scheduled(fixedDelay = 60_000)
    public void releaseExpiredStock() {
        int count = stockLockService.releaseExpiredStock();
        if (count > 0) {
            log.info("库存超时释放任务执行完成，释放数量={}", count);
        }
    }
}
```

如果使用 Spring Scheduler，需要在启动类或配置类加上 `@EnableScheduling`。

文件位置：`src/main/java/io/github/atengk/stock/StockLockDemoApplication.java`

```java
package io.github.atengk.stock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 库存锁定案例启动类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@EnableScheduling
@SpringBootApplication
public class StockLockDemoApplication {

    /**
     * 启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(StockLockDemoApplication.class, args);
    }
}
```

补充一个 Redis 库存初始化方法，方便测试时把 MySQL 可用库存同步到 Redis。生产环境可以放在后台发布资源、活动预热或补偿任务里。

文件位置：`src/main/java/io/github/atengk/stock/service/StockCacheService.java`

```java
package io.github.atengk.stock.service;

/**
 * 库存缓存服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface StockCacheService {

    /**
     * 重建资源库存缓存
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     */
    void rebuildAvailableStock(String resourceType, Long resourceId);
}
```

文件位置：`src/main/java/io/github/atengk/stock/service/impl/StockCacheServiceImpl.java`

```java
package io.github.atengk.stock.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.stock.entity.ResourceStock;
import io.github.atengk.stock.exception.StockException;
import io.github.atengk.stock.mapper.ResourceStockMapper;
import io.github.atengk.stock.service.StockCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 库存缓存服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCacheServiceImpl implements StockCacheService {

    private static final String STOCK_AVAILABLE_KEY = "stock:available:{}:{}";

    private final ResourceStockMapper resourceStockMapper;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 重建资源库存缓存
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     */
    @Override
    public void rebuildAvailableStock(String resourceType, Long resourceId) {
        ResourceStock stock = resourceStockMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResourceStock>()
                        .eq(ResourceStock::getResourceType, resourceType)
                        .eq(ResourceStock::getResourceId, resourceId)
                        .eq(ResourceStock::getStatus, 1)
        );

        if (ObjectUtil.isNull(stock)) {
            throw new StockException("资源库存不存在");
        }

        int availableStock = stock.getTotalStock() - stock.getLockedStock() - stock.getSoldStock();
        String key = StrUtil.format(STOCK_AVAILABLE_KEY, resourceType, resourceId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(availableStock));

        log.info("Redis 可用库存重建完成，key={}，availableStock={}", key, availableStock);
    }
}
```

当前这一节已经具备完整核心链路：

```text
查询可用库存
-> 初始化 Redis 库存
-> Redis Lua 原子锁定
-> MySQL 锁定库存
-> 写入锁定记录
-> 写入库存流水
-> 支付成功确认扣减
-> 用户取消释放
-> 超时任务自动释放
```

## 接口设计

本节提供库存查询、资源锁定、确认扣减、释放资源 4 个核心接口。接口层只做参数校验、调用 Service、返回结果，不在 Controller 中写库存计算和状态流转逻辑。该模块对应 README 中“查询可用资源、用户发起占用、锁定库存、支付成功确认扣减、取消释放、超时释放”的核心链路。

### 查询库存接口

查询库存接口用于前端展示资源当前库存情况，例如课程详情页展示剩余名额、票务页面展示余票、优惠券活动页展示剩余数量。

接口定义：

| 项目     | 内容                                     |
| -------- | ---------------------------------------- |
| 请求方式 | `GET`                                    |
| 接口路径 | `/api/stock/{resourceType}/{resourceId}` |
| 说明     | 查询资源库存快照和可用库存               |
| 幂等性   | 天然幂等                                 |

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "resourceType": "COURSE",
    "resourceId": 1001,
    "resourceName": "Java 高并发实战课",
    "totalStock": 100,
    "lockedStock": 2,
    "soldStock": 10,
    "availableStock": 88
  }
}
```

### 锁定资源接口

锁定资源接口用于用户提交订单、预约、报名、领取权益等场景。调用成功后会生成锁定记录，并占用库存。

接口定义：

| 项目     | 内容                         |
| -------- | ---------------------------- |
| 请求方式 | `POST`                       |
| 接口路径 | `/api/stock/lock`            |
| 说明     | 根据业务单号锁定指定资源库存 |
| 幂等键   | `bizNo`                      |
| 成功结果 | 返回 `lockNo` 锁定单号       |

请求参数：

| 字段         | 类型    | 必填 | 说明                 |
| ------------ | ------- | ---- | -------------------- |
| bizNo        | String  | 是   | 业务单号，例如订单号 |
| userId       | Long    | 是   | 用户 ID              |
| resourceType | String  | 是   | 资源类型             |
| resourceId   | Long    | 是   | 资源 ID              |
| quantity     | Integer | 是   | 锁定数量             |

请求示例：

```json
{
  "bizNo": "ORDER202605150001",
  "userId": 10001,
  "resourceType": "COURSE",
  "resourceId": 1001,
  "quantity": 1
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "LK1934567890123456789"
}
```

### 确认扣减接口

确认扣减接口用于支付成功、预约确认、权益核销成功等场景。它不会再次扣 Redis 可用库存，因为锁定时已经从 Redis 可用库存中扣减；确认时只负责将数据库中的锁定库存转为已售库存。

接口定义：

| 项目     | 内容                       |
| -------- | -------------------------- |
| 请求方式 | `POST`                     |
| 接口路径 | `/api/stock/confirm`       |
| 说明     | 支付成功后确认扣减锁定库存 |
| 幂等键   | `bizNo`                    |
| 成功结果 | 无业务数据返回             |

请求示例：

```json
{
  "bizNo": "ORDER202605150001"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

### 释放资源接口

释放资源接口用于用户取消订单、取消预约、主动放弃权益等场景。只有 `LOCKED` 状态的锁定记录可以释放，已确认扣减的记录不能释放。

接口定义：

| 项目     | 内容                       |
| -------- | -------------------------- |
| 请求方式 | `POST`                     |
| 接口路径 | `/api/stock/release`       |
| 说明     | 用户主动取消后释放锁定库存 |
| 幂等键   | `bizNo`                    |
| 成功结果 | 无业务数据返回             |

请求示例：

```json
{
  "bizNo": "ORDER202605150001"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

下面给出接口层需要的 DTO、统一返回体、异常处理器和 Controller。

文件位置：`src/main/java/io/github/atengk/stock/common/ApiResult.java`

统一返回体用于封装接口响应，避免 Controller 直接返回裸数据。

```java
package io.github.atengk.stock.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一返回结果
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
     * 成功返回
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 返回结果
     */
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 失败返回
     *
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 返回结果
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null);
    }
}
```

文件位置：`src/main/java/io/github/atengk/stock/dto/LockStockRequest.java`

锁定资源请求 DTO 用于接收用户占用资源时提交的业务单号、资源信息和锁定数量。

```java
package io.github.atengk.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 锁定资源请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class LockStockRequest {

    @NotBlank(message = "业务单号不能为空")
    private String bizNo;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    @NotNull(message = "资源ID不能为空")
    private Long resourceId;

    @NotNull(message = "锁定数量不能为空")
    @Min(value = 1, message = "锁定数量必须大于0")
    private Integer quantity;
}
```

文件位置：`src/main/java/io/github/atengk/stock/dto/BizNoRequest.java`

业务单号请求 DTO 用于确认扣减和释放库存接口。

```java
package io.github.atengk.stock.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 业务单号请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class BizNoRequest {

    @NotBlank(message = "业务单号不能为空")
    private String bizNo;
}
```

文件位置：`src/main/java/io/github/atengk/stock/handler/GlobalExceptionHandler.java`

全局异常处理器用于统一处理业务异常和参数校验异常，返回清晰的错误信息。

```java
package io.github.atengk.stock.handler;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.stock.common.ApiResult;
import io.github.atengk.stock.exception.StockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
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
     * 处理库存业务异常
     *
     * @param ex 业务异常
     * @return 返回结果
     */
    @ExceptionHandler(StockException.class)
    public ApiResult<Boolean> handleStockException(StockException ex) {
        log.warn("库存业务处理失败，原因={}", ex.getMessage());
        return ApiResult.fail(ex.getMessage());
    }

    /**
     * 处理参数校验异常
     *
     * @param ex 参数校验异常
     * @return 返回结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Boolean> handleValidException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        String message = CollUtil.isEmpty(bindingResult.getFieldErrors())
                ? "参数校验失败"
                : bindingResult.getFieldErrors().get(0).getDefaultMessage();
        log.warn("接口参数校验失败，原因={}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理未知异常
     *
     * @param ex 异常
     * @return 返回结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Boolean> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ApiResult.fail("系统异常，请稍后重试");
    }
}
```

文件位置：`src/main/java/io/github/atengk/stock/controller/StockLockController.java`

Controller 提供库存查询、锁定、确认扣减和释放接口，具体库存变更逻辑全部委托给 `StockLockService`。

```java
package io.github.atengk.stock.controller;

import io.github.atengk.stock.common.ApiResult;
import io.github.atengk.stock.dto.BizNoRequest;
import io.github.atengk.stock.dto.LockStockRequest;
import io.github.atengk.stock.service.StockLockService;
import io.github.atengk.stock.vo.AvailableStockVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存锁定接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock")
public class StockLockController {

    private final StockLockService stockLockService;

    /**
     * 查询可用库存
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @return 可用库存
     */
    @GetMapping("/{resourceType}/{resourceId}")
    public ApiResult<AvailableStockVO> queryAvailableStock(@PathVariable String resourceType,
                                                           @PathVariable Long resourceId) {
        return ApiResult.ok(stockLockService.queryAvailableStock(resourceType, resourceId));
    }

    /**
     * 锁定资源库存
     *
     * @param request 锁定请求
     * @return 锁定单号
     */
    @PostMapping("/lock")
    public ApiResult<String> lockStock(@Valid @RequestBody LockStockRequest request) {
        String lockNo = stockLockService.lockStock(
                request.getBizNo(),
                request.getUserId(),
                request.getResourceType(),
                request.getResourceId(),
                request.getQuantity()
        );
        return ApiResult.ok(lockNo);
    }

    /**
     * 确认扣减库存
     *
     * @param request 业务单号请求
     * @return 处理结果
     */
    @PostMapping("/confirm")
    public ApiResult<Boolean> confirmStock(@Valid @RequestBody BizNoRequest request) {
        stockLockService.confirmStock(request.getBizNo());
        return ApiResult.ok(Boolean.TRUE);
    }

    /**
     * 释放锁定库存
     *
     * @param request 业务单号请求
     * @return 处理结果
     */
    @PostMapping("/release")
    public ApiResult<Boolean> releaseStock(@Valid @RequestBody BizNoRequest request) {
        stockLockService.releaseStock(request.getBizNo());
        return ApiResult.ok(Boolean.TRUE);
    }
}
```

接口调用示例：

```bash
# 查询库存
curl -X GET "http://localhost:8080/api/stock/COURSE/1001"

# 锁定资源
curl -X POST "http://localhost:8080/api/stock/lock" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "ORDER202605150001",
    "userId": 10001,
    "resourceType": "COURSE",
    "resourceId": 1001,
    "quantity": 1
  }'

# 支付成功确认扣减
curl -X POST "http://localhost:8080/api/stock/confirm" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "ORDER202605150001"
  }'

# 用户取消释放库存
curl -X POST "http://localhost:8080/api/stock/release" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "ORDER202605150001"
  }'
```

上面的命令分别验证查询、锁定、确认和释放。`bizNo` 在同一轮业务中必须唯一，真实项目里通常使用订单号、预约单号、报名单号或领取单号。

## 并发与幂等处理

库存模块最容易出问题的地方不是普通 CRUD，而是并发请求、重复提交、支付回调重复、取消和确认同时发生、定时任务重复扫描等情况。本案例用 Redis Lua、数据库条件更新、唯一索引、状态机和事务共同处理这些问题。

### 防超卖处理

防超卖不能只依赖 Java 代码中的 `if` 判断，例如下面这种写法在并发下是不安全的：

```java
if (availableStock >= quantity) {
    stock.setLockedStock(stock.getLockedStock() + quantity);
    resourceStockMapper.updateById(stock);
}
```

多个线程可能同时读到相同的可用库存，然后都更新成功，最终造成超卖。

本案例采用两层防超卖。

第一层是 Redis Lua 原子扣减：

```text
判断 Redis 可用库存是否充足
-> 充足则 decrby
-> 不充足直接失败
```

Redis Lua 保证“判断”和“扣减”在 Redis 内部一次执行完成，不会被其他请求插入。

第二层是 MySQL 条件更新兜底：

```sql
update resource_stock
set locked_stock = locked_stock + #{quantity},
    version = version + 1,
    update_time = now()
where resource_type = #{resourceType}
  and resource_id = #{resourceId}
  and status = 1
  and total_stock - locked_stock - sold_stock >= #{quantity};
```

这条 SQL 的关键点是：

```sql
and total_stock - locked_stock - sold_stock >= #{quantity}
```

即使 Redis 出现缓存不一致，数据库也不会允许可用库存不足时继续锁定。

推荐防超卖链路：

```text
请求锁定库存
-> Redis Lua 原子扣减
-> Redis 成功后进入数据库事务
-> MySQL 条件更新 locked_stock
-> MySQL 成功后创建锁定记录
-> MySQL 失败则回补 Redis
```

并发压力主要由 Redis 承担，最终正确性由 MySQL 条件更新保证。

### 重复锁定控制

重复锁定通常来自这些场景：

```text
用户重复点击提交按钮
前端请求超时后重试
网关 / Feign / MQ 自动重试
同一个订单重复调用锁定接口
```

本案例使用 `bizNo` 作为业务幂等键，控制同一个业务单号只能锁定一次。

数据库层唯一索引：

```sql
unique key uk_biz_no (biz_no)
```

Redis 层短期幂等 Key：

```text
stock:lock:biz:{bizNo}
```

重复锁定处理策略：

| 场景                   | 处理方式                                   |
| ---------------------- | ------------------------------------------ |
| 数据库已有锁定记录     | 直接返回已有 `lockNo`                      |
| Redis 已有业务幂等 Key | 拒绝重复提交，或稍后查询数据库返回已有结果 |
| 数据库唯一索引冲突     | 查询已有记录并返回，避免接口失败           |
| 首次锁定失败           | 回补 Redis，删除业务幂等 Key               |

如果希望重复请求也返回已有锁定单号，可以对 `lockStock` 中 Redis 返回 `-2` 的情况做增强处理：等待极短时间后查询数据库记录。

下面是可选增强方法，用于把“重复提交异常”转换为“幂等返回”。

```java
/**
 * 处理 Redis 锁定返回值，重复提交时尝试从数据库读取已有锁定记录
 *
 * @param redisResult Redis 返回值
 * @param bizNo       业务单号
 * @return 已存在的锁定单号，非重复提交时返回 null
 */
private String handleLockLuaResultWithIdempotent(Long redisResult, String bizNo) {
    if (ObjectUtil.isNull(redisResult)) {
        throw new StockException("Redis 库存锁定失败");
    }
    if (redisResult >= 0) {
        return null;
    }
    if (Objects.equals(redisResult, -1L)) {
        throw new StockException("库存不足");
    }
    if (Objects.equals(redisResult, -3L)) {
        throw new StockException("Redis 库存未初始化");
    }
    if (Objects.equals(redisResult, -2L)) {
        ResourceLockRecord record = resourceLockRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResourceLockRecord>()
                        .eq(ResourceLockRecord::getBizNo, bizNo)
        );
        if (ObjectUtil.isNotNull(record)) {
            log.info("重复锁定请求命中幂等记录，bizNo={}，lockNo={}", bizNo, record.getLockNo());
            return record.getLockNo();
        }
        throw new StockException("业务单号正在处理中，请稍后查询结果");
    }
    throw new StockException("未知库存锁定结果");
}
```

在生产环境中，更推荐接口返回明确状态：

```text
SUCCESS       锁定成功
PROCESSING    相同业务单正在处理中
DUPLICATE     重复请求，返回已有锁定结果
FAILED        锁定失败
```

本案例为了保持核心代码简洁，只返回 `lockNo` 或异常信息。

### 重复释放幂等

重复释放通常来自这些场景：

```text
用户多次点击取消订单
订单取消接口重试
超时释放任务重复扫描
MQ 重复消费取消消息
用户取消和超时任务同时执行
```

释放库存必须满足两个条件：

```text
锁定记录存在
并且状态必须是 LOCKED
```

如果锁定记录已经是 `RELEASED` 或 `EXPIRED_RELEASED`，说明库存已经释放过，应该直接幂等返回。

如果锁定记录已经是 `CONFIRMED`，说明支付成功并确认扣减，不能再释放库存，也应该直接结束或返回业务错误。

核心判断：

```java
if (Objects.equals(record.getStatus(), targetStatus.getCode())) {
    log.info("库存已释放，幂等返回，bizNo={}，status={}", bizNo, targetStatus.getCode());
    return;
}

if (!Objects.equals(record.getStatus(), ResourceLockStatusEnum.LOCKED.getCode())) {
    log.info("当前状态无需释放，幂等返回，bizNo={}，status={}", bizNo, record.getStatus());
    return;
}
```

数据库层建议使用状态条件更新，避免并发下重复推进状态：

```sql
update resource_lock_record
set status = 'RELEASED',
    release_time = now(),
    update_time = now()
where biz_no = #{bizNo}
  and status = 'LOCKED';
```

库存释放也必须带条件保护：

```sql
update resource_stock
set locked_stock = locked_stock - #{quantity},
    version = version + 1,
    update_time = now()
where resource_type = #{resourceType}
  and resource_id = #{resourceId}
  and locked_stock >= #{quantity};
```

这样即使释放接口被重复调用，也不会把 `locked_stock` 扣成负数。

释放和确认同时发生时，建议以锁定记录的行锁或状态条件更新决定胜负：

```text
支付确认先拿到锁：
LOCKED -> CONFIRMED
取消释放再执行时发现不是 LOCKED，不能释放

取消释放先拿到锁：
LOCKED -> RELEASED
支付确认再执行时发现不是 LOCKED，不能确认
```

这就是状态机在并发场景中的作用。

### 数据库乐观锁兜底

MyBatis-Plus 的 `@Version` 可以用于普通实体更新时的乐观锁控制，但库存扣减这类场景不能只依赖版本号。因为库存扣减真正要判断的是“库存是否足够”，所以必须保留业务条件：

```sql
and total_stock - locked_stock - sold_stock >= #{quantity}
```

推荐使用两种兜底方式。

第一种是业务条件更新，也是本案例主要采用的方式：

```sql
update resource_stock
set locked_stock = locked_stock + #{quantity},
    version = version + 1,
    update_time = now()
where resource_type = #{resourceType}
  and resource_id = #{resourceId}
  and status = 1
  and total_stock - locked_stock - sold_stock >= #{quantity};
```

这种方式不需要先查版本号，数据库会直接保证可用库存不足时更新失败。

第二种是版本号乐观锁，适合后台调整库存、资源初始化、低频管理操作：

```sql
update resource_stock
set total_stock = #{totalStock},
    version = version + 1,
    update_time = now()
where id = #{id}
  and version = #{version};
```

如果影响行数为 `0`，说明数据已经被其他线程修改，需要重新查询后再处理。

库存锁定场景中，建议组合策略如下：

| 场景             | 推荐策略                        |
| ---------------- | ------------------------------- |
| 高频用户锁定库存 | Redis Lua + MySQL 条件更新      |
| 支付确认扣减     | 锁定记录状态机 + MySQL 条件更新 |
| 用户取消释放     | 锁定记录状态机 + MySQL 条件更新 |
| 超时任务释放     | 状态机幂等 + 分批扫描           |
| 后台调整总库存   | MyBatis-Plus `@Version` 乐观锁  |
| Redis 缓存重建   | 以 MySQL 快照为准               |

一个典型的后台调整库存 Mapper 方法如下：

文件位置：`src/main/java/io/github/atengk/stock/mapper/ResourceStockMapper.java`

该方法适合后台调整总库存，使用 `version` 防止覆盖别人刚刚修改过的数据。

```java
/**
 * 根据版本号调整总库存
 *
 * @param id         库存ID
 * @param totalStock 新总库存
 * @param version    当前版本号
 * @return 影响行数
 */
@Update("""
        update resource_stock
        set total_stock = #{totalStock},
            version = version + 1,
            update_time = now()
        where id = #{id}
          and version = #{version}
        """)
int updateTotalStockByVersion(@Param("id") Long id,
                              @Param("totalStock") Integer totalStock,
                              @Param("version") Integer version);
```

后台调整总库存时还需要校验新库存不能小于已锁定加已售数量：

```text
new_total_stock >= locked_stock + sold_stock
```

否则会出现总库存小于已占用库存的脏数据。

总结一下，并发与幂等的核心不是某一个技术点，而是组合使用：

```text
Redis Lua：挡住高并发扣减
MySQL 条件更新：防止最终超卖
唯一索引：防止重复锁定
状态机：防止重复确认和重复释放
事务：保证库存、锁定记录、流水一起成功或一起失败
流水表：用于排查和补偿
```

## 定时补偿任务

定时补偿任务用于处理“锁定后长期未支付”的资源占用，以及 Redis 与 MySQL 可能出现的不一致问题。该能力对应 README 中库存场景的“超时未支付自动释放”和“最终一致性”要求。

### 扫描超时锁定记录

扫描条件很明确：只处理 `LOCKED` 状态，并且 `expire_time <= 当前时间` 的锁定记录。

核心 SQL 已在前文 Mapper 中给出：

```sql
select *
from resource_lock_record
where status = 'LOCKED'
  and expire_time <= now()
order by expire_time asc
limit 100;
```

生产环境建议按批次扫描，不要一次性拉取全部超时数据。否则在大量订单超时的场景下，定时任务可能导致数据库压力过高。

推荐扫描策略：

| 策略               | 说明                                   |
| ------------------ | -------------------------------------- |
| 分批扫描           | 每次处理 100 到 500 条                 |
| 按过期时间升序     | 优先释放最早过期的数据                 |
| 单条失败不影响整体 | 一条释放失败只记录日志，继续处理下一条 |
| 复用释放逻辑       | 不要为超时释放单独写一套库存扣减逻辑   |
| 保留幂等判断       | 重复扫描时不会重复释放                 |

前文 `StockLockServiceImpl#releaseExpiredStock()` 已经实现了基础扫描逻辑：

```java
@Override
public int releaseExpiredStock() {
    List<ResourceLockRecord> expiredRecords = resourceLockRecordMapper.selectExpiredLockedList(
            now(),
            stockLockProperties.getExpireScanLimit()
    );

    int successCount = 0;
    for (ResourceLockRecord record : expiredRecords) {
        try {
            releaseStockInternal(record.getBizNo(), ResourceLockStatusEnum.EXPIRED_RELEASED,
                    StockFlowTypeEnum.EXPIRE_RELEASE, "支付超时自动释放库存");
            successCount++;
        } catch (RuntimeException ex) {
            log.warn("超时释放库存失败，bizNo={}，原因={}", record.getBizNo(), ex.getMessage());
        }
    }

    log.info("超时库存释放完成，本次扫描={}，成功释放={}", expiredRecords.size(), successCount);
    return successCount;
}
```

### 自动释放过期资源

如果使用 Spring Scheduler，可以直接创建一个定时任务类，每分钟扫描一次超时锁定记录。

文件位置：`src/main/java/io/github/atengk/stock/job/StockCompensationJob.java`

该任务负责释放超时锁定库存，并定期修复 Redis 可用库存缓存。

```java
package io.github.atengk.stock.job;

import io.github.atengk.stock.service.StockCompensationService;
import io.github.atengk.stock.service.StockLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 库存补偿任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockCompensationJob {

    private final StockLockService stockLockService;

    private final StockCompensationService stockCompensationService;

    /**
     * 每分钟释放一次超时未支付库存
     */
    @Scheduled(fixedDelay = 60_000)
    public void releaseExpiredStock() {
        int count = stockLockService.releaseExpiredStock();
        if (count > 0) {
            log.info("超时库存释放任务完成，释放数量={}", count);
        }
    }

    /**
     * 每 10 分钟修复一次异常库存缓存
     */
    @Scheduled(fixedDelay = 600_000)
    public void repairStockCache() {
        int count = stockCompensationService.repairRedisAvailableStock();
        if (count > 0) {
            log.info("库存缓存修复任务完成，修复数量={}", count);
        }
    }
}
```

如果使用 XXL-JOB，核心业务代码不用变，只需要把定时入口换成 XXL-JOB Handler。

文件位置：`src/main/java/io/github/atengk/stock/job/XxlStockCompensationJob.java`

这段代码用于将库存补偿任务接入 XXL-JOB，适合生产环境统一调度。

```java
package io.github.atengk.stock.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.stock.service.StockCompensationService;
import io.github.atengk.stock.service.StockLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB 库存补偿任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XxlStockCompensationJob {

    private final StockLockService stockLockService;

    private final StockCompensationService stockCompensationService;

    /**
     * 释放超时未支付库存
     */
    @XxlJob("releaseExpiredStockJob")
    public void releaseExpiredStockJob() {
        int count = stockLockService.releaseExpiredStock();
        log.info("XXL-JOB 超时库存释放完成，释放数量={}", count);
    }

    /**
     * 修复 Redis 可用库存缓存
     */
    @XxlJob("repairRedisAvailableStockJob")
    public void repairRedisAvailableStockJob() {
        int count = stockCompensationService.repairRedisAvailableStock();
        log.info("XXL-JOB Redis 库存缓存修复完成，修复数量={}", count);
    }
}
```

使用 Spring Scheduler 时，启动类需要开启调度：

```java
@EnableScheduling
@SpringBootApplication
public class StockLockDemoApplication {
}
```

使用 XXL-JOB 时，建议关闭 Spring Scheduler 中同类任务，避免两个调度源重复执行。即使重复执行，当前释放逻辑也能通过状态机保证幂等，但生产环境仍不建议保留重复调度入口。

### 异常数据修复

异常修复主要处理两类问题。

第一类是 Redis 可用库存与 MySQL 快照不一致。比如 Redis 锁定成功后，MySQL 写入失败但 Redis 回滚失败；或者手工修改了数据库库存，但 Redis 没有同步。

第二类是数据库库存本身异常。比如 `locked_stock` 小于 0，或者 `locked_stock + sold_stock > total_stock`。这类问题不能自动盲修，应该记录告警并人工确认。

建议先实现 Redis 缓存修复，因为它可以完全以 MySQL 为准重建。

文件位置：`src/main/java/io/github/atengk/stock/service/StockCompensationService.java`

```java
package io.github.atengk.stock.service;

/**
 * 库存补偿服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface StockCompensationService {

    /**
     * 修复 Redis 可用库存缓存
     *
     * @return 修复数量
     */
    int repairRedisAvailableStock();

    /**
     * 检查数据库异常库存
     *
     * @return 异常数量
     */
    int checkDbAbnormalStock();
}
```

文件位置：`src/main/java/io/github/atengk/stock/mapper/ResourceStockMapper.java`

在原有 `ResourceStockMapper` 中补充两个查询方法：一个用于查询需要重建缓存的库存，一个用于检查数据库异常库存。

```java
/**
 * 查询启用状态的库存列表
 *
 * @param limit 查询数量
 * @return 库存列表
 */
@Select("""
        select *
        from resource_stock
        where status = 1
        order by id asc
        limit #{limit}
        """)
List<ResourceStock> selectEnabledStockList(@Param("limit") Integer limit);

/**
 * 查询异常库存列表
 *
 * @param limit 查询数量
 * @return 异常库存列表
 */
@Select("""
        select *
        from resource_stock
        where locked_stock < 0
           or sold_stock < 0
           or total_stock < 0
           or locked_stock + sold_stock > total_stock
        order by id asc
        limit #{limit}
        """)
List<ResourceStock> selectAbnormalStockList(@Param("limit") Integer limit);
```

文件位置：`src/main/java/io/github/atengk/stock/service/impl/StockCompensationServiceImpl.java`

该实现类会以 MySQL 库存快照为准重建 Redis 可用库存，并扫描数据库异常库存。

```java
package io.github.atengk.stock.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.stock.entity.ResourceStock;
import io.github.atengk.stock.mapper.ResourceStockMapper;
import io.github.atengk.stock.service.StockCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 库存补偿服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCompensationServiceImpl implements StockCompensationService {

    private static final String STOCK_AVAILABLE_KEY = "stock:available:{}:{}";

    private static final int DEFAULT_SCAN_LIMIT = 500;

    private final ResourceStockMapper resourceStockMapper;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 修复 Redis 可用库存缓存
     *
     * @return 修复数量
     */
    @Override
    public int repairRedisAvailableStock() {
        List<ResourceStock> stockList = resourceStockMapper.selectEnabledStockList(DEFAULT_SCAN_LIMIT);
        if (CollUtil.isEmpty(stockList)) {
            return 0;
        }

        int repairCount = 0;
        for (ResourceStock stock : stockList) {
            int availableStock = stock.getTotalStock() - stock.getLockedStock() - stock.getSoldStock();
            String key = StrUtil.format(STOCK_AVAILABLE_KEY, stock.getResourceType(), stock.getResourceId());

            stringRedisTemplate.opsForValue().set(key, String.valueOf(availableStock));
            repairCount++;

            log.info("重建 Redis 可用库存，key={}，availableStock={}", key, availableStock);
        }

        return repairCount;
    }

    /**
     * 检查数据库异常库存
     *
     * @return 异常数量
     */
    @Override
    public int checkDbAbnormalStock() {
        List<ResourceStock> abnormalList = resourceStockMapper.selectAbnormalStockList(DEFAULT_SCAN_LIMIT);
        if (CollUtil.isEmpty(abnormalList)) {
            return 0;
        }

        for (ResourceStock stock : abnormalList) {
            log.error("发现数据库异常库存，resourceType={}，resourceId={}，totalStock={}，lockedStock={}，soldStock={}",
                    stock.getResourceType(),
                    stock.getResourceId(),
                    stock.getTotalStock(),
                    stock.getLockedStock(),
                    stock.getSoldStock());
        }

        return abnormalList.size();
    }
}
```

数据库异常库存不建议自动修复，原因是库存数据通常与订单、支付、预约记录强相关，盲目修复可能掩盖真实业务问题。正确做法是先告警，再结合库存流水表排查。

常用排查 SQL：

```sql
-- 查询库存快照
select *
from resource_stock
where resource_type = 'COURSE'
  and resource_id = 1001;

-- 查询某个资源的库存流水
select *
from resource_stock_flow
where resource_type = 'COURSE'
  and resource_id = 1001
order by id desc;

-- 查询某个订单的锁定记录
select *
from resource_lock_record
where biz_no = 'ORDER202605150001';

-- 查询异常库存
select *
from resource_stock
where locked_stock < 0
   or sold_stock < 0
   or total_stock < 0
   or locked_stock + sold_stock > total_stock;
```

## 功能验证

功能验证建议按“初始化数据 -> 初始化 Redis -> 调接口 -> 查数据库结果”的顺序进行。不要只看接口返回成功，必须核对三张表的数据是否正确。

准备测试数据：

```sql
create database if not exists stock_demo default charset utf8mb4;

use stock_demo;

insert into resource_stock (
    resource_type,
    resource_id,
    resource_name,
    total_stock,
    locked_stock,
    sold_stock,
    status,
    version
) values (
    'COURSE',
    1001,
    'Java 高并发实战课',
    10,
    0,
    0,
    1,
    0
);
```

初始化 Redis 可用库存：

```bash
redis-cli set stock:available:COURSE:1001 10
```

启动项目：

```bash
mvn spring-boot:run
```

### 正常锁定与扣减验证

第一步，查询库存。

```bash
curl -X GET "http://localhost:8080/api/stock/COURSE/1001"
```

预期结果：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "resourceType": "COURSE",
    "resourceId": 1001,
    "resourceName": "Java 高并发实战课",
    "totalStock": 10,
    "lockedStock": 0,
    "soldStock": 0,
    "availableStock": 10
  }
}
```

第二步，锁定 1 个库存。

```bash
curl -X POST "http://localhost:8080/api/stock/lock" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "ORDER202605150001",
    "userId": 10001,
    "resourceType": "COURSE",
    "resourceId": 1001,
    "quantity": 1
  }'
```

锁定后查询数据库：

```sql
select total_stock, locked_stock, sold_stock,
       total_stock - locked_stock - sold_stock as available_stock
from resource_stock
where resource_type = 'COURSE'
  and resource_id = 1001;

select *
from resource_lock_record
where biz_no = 'ORDER202605150001';

select *
from resource_stock_flow
where biz_no = 'ORDER202605150001';
```

预期结果：

```text
resource_stock:
total_stock = 10
locked_stock = 1
sold_stock = 0
available_stock = 9

resource_lock_record:
status = LOCKED

resource_stock_flow:
change_type = LOCK
change_quantity = 1
```

第三步，模拟支付成功确认扣减。

```bash
curl -X POST "http://localhost:8080/api/stock/confirm" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "ORDER202605150001"
  }'
```

确认后查询数据库：

```sql
select total_stock, locked_stock, sold_stock,
       total_stock - locked_stock - sold_stock as available_stock
from resource_stock
where resource_type = 'COURSE'
  and resource_id = 1001;

select status, confirm_time, release_time
from resource_lock_record
where biz_no = 'ORDER202605150001';

select change_type, change_quantity
from resource_stock_flow
where biz_no = 'ORDER202605150001'
order by id asc;
```

预期结果：

```text
resource_stock:
total_stock = 10
locked_stock = 0
sold_stock = 1
available_stock = 9

resource_lock_record:
status = CONFIRMED
confirm_time 不为空
release_time 为空

resource_stock_flow:
第一条 change_type = LOCK
第二条 change_type = CONFIRM
```

确认扣减后，Redis 可用库存不需要回补，因为锁定阶段已经扣减过 Redis 可用库存。

### 取消释放验证

重新准备一笔订单，先锁定库存。

```bash
curl -X POST "http://localhost:8080/api/stock/lock" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "ORDER202605150002",
    "userId": 10002,
    "resourceType": "COURSE",
    "resourceId": 1001,
    "quantity": 2
  }'
```

然后释放库存。

```bash
curl -X POST "http://localhost:8080/api/stock/release" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "ORDER202605150002"
  }'
```

查询验证：

```sql
select total_stock, locked_stock, sold_stock,
       total_stock - locked_stock - sold_stock as available_stock
from resource_stock
where resource_type = 'COURSE'
  and resource_id = 1001;

select status, release_time
from resource_lock_record
where biz_no = 'ORDER202605150002';

select change_type, change_quantity
from resource_stock_flow
where biz_no = 'ORDER202605150002'
order by id asc;
```

预期结果：

```text
resource_lock_record:
status = RELEASED
release_time 不为空

resource_stock_flow:
第一条 change_type = LOCK
第二条 change_type = RELEASE
```

重复调用释放接口：

```bash
curl -X POST "http://localhost:8080/api/stock/release" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "ORDER202605150002"
  }'
```

预期结果：

```text
接口返回成功或幂等成功
locked_stock 不会继续减少
resource_stock_flow 不应重复新增 RELEASE 流水
```

### 超时释放验证

为了快速验证超时释放，可以手动插入一条已过期的锁定记录，或者将配置中的锁定时间改短。

推荐测试方式：先锁定一笔订单。

```bash
curl -X POST "http://localhost:8080/api/stock/lock" \
  -H "Content-Type: application/json" \
  -d '{
    "bizNo": "ORDER202605150003",
    "userId": 10003,
    "resourceType": "COURSE",
    "resourceId": 1001,
    "quantity": 1
  }'
```

手动把锁定记录改成已过期：

```sql
update resource_lock_record
set expire_time = date_sub(now(), interval 1 minute)
where biz_no = 'ORDER202605150003'
  and status = 'LOCKED';
```

等待 1 分钟，或者手动调用 `releaseExpiredStock()` 所在任务。

查询验证：

```sql
select status, expire_time, release_time
from resource_lock_record
where biz_no = 'ORDER202605150003';

select change_type, change_quantity
from resource_stock_flow
where biz_no = 'ORDER202605150003'
order by id asc;
```

预期结果：

```text
resource_lock_record:
status = EXPIRED_RELEASED
release_time 不为空

resource_stock_flow:
第一条 change_type = LOCK
第二条 change_type = EXPIRE_RELEASE
```

如果定时任务没有执行，检查启动类是否开启：

```java
@EnableScheduling
@SpringBootApplication
public class StockLockDemoApplication {
}
```

### 并发防超卖验证

并发测试前，建议单独准备一个库存较小的资源，例如总库存为 5。

```sql
insert into resource_stock (
    resource_type,
    resource_id,
    resource_name,
    total_stock,
    locked_stock,
    sold_stock,
    status,
    version
) values (
    'COURSE',
    2001,
    '并发防超卖测试课程',
    5,
    0,
    0,
    1,
    0
);
```

初始化 Redis：

```bash
redis-cli set stock:available:COURSE:2001 5
```

使用 shell 模拟 20 个并发请求抢 5 个库存：

```bash
for i in $(seq 1 20); do
  curl -s -X POST "http://localhost:8080/api/stock/lock" \
    -H "Content-Type: application/json" \
    -d "{
      \"bizNo\": \"ORDER_CONCURRENT_${i}\",
      \"userId\": ${i},
      \"resourceType\": \"COURSE\",
      \"resourceId\": 2001,
      \"quantity\": 1
    }" &
done

wait
echo "并发请求执行完成"
```

查询数据库结果：

```sql
select total_stock, locked_stock, sold_stock,
       total_stock - locked_stock - sold_stock as available_stock
from resource_stock
where resource_type = 'COURSE'
  and resource_id = 2001;

select status, count(*) as count
from resource_lock_record
where resource_type = 'COURSE'
  and resource_id = 2001
group by status;

select count(*) as lock_flow_count
from resource_stock_flow
where resource_type = 'COURSE'
  and resource_id = 2001
  and change_type = 'LOCK';
```

预期结果：

```text
resource_stock:
total_stock = 5
locked_stock = 5
sold_stock = 0
available_stock = 0

resource_lock_record:
LOCKED = 5

resource_stock_flow:
LOCK 流水数量 = 5
```

再查 Redis：

```bash
redis-cli get stock:available:COURSE:2001
```

预期结果：

```text
0
```

如果出现以下结果，说明防超卖成功：

```text
成功锁定请求数 = 5
失败请求数 = 15
locked_stock 不超过 total_stock
Redis 可用库存不小于 0
数据库可用库存不小于 0
```

最后可以批量确认或释放这些锁定记录，验证库存状态继续正确。

批量释放测试数据：

```sql
select biz_no
from resource_lock_record
where resource_type = 'COURSE'
  and resource_id = 2001
  and status = 'LOCKED';
```

逐个调用释放接口后，最终应满足：

```text
locked_stock = 0
sold_stock = 0
available_stock = 5
锁定记录状态变为 RELEASED
每条业务单有 LOCK 和 RELEASE 两条流水
```

本案例验证完成后，核心功能闭环如下：

```text
正常路径：查询库存 -> 锁定库存 -> 支付确认 -> 已售库存增加
取消路径：查询库存 -> 锁定库存 -> 用户取消 -> 锁定库存释放
超时路径：查询库存 -> 锁定库存 -> 超时任务 -> 锁定库存释放
并发路径：多个用户同时锁定 -> Redis Lua 拦截 -> MySQL 条件更新兜底 -> 不超卖
```