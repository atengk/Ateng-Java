# 排队叫号与窗口分配

本文档围绕“排队叫号与窗口分配”场景实现一个可落地的 Java 后端核心案例，适用于医院、政务大厅、银行网点、客服中心、线下门店、维修服务中心等排队办理类业务。该场景的核心链路包括用户取号、进入等待队列、窗口叫号、办理、过号、重新入队以及实时推送叫号信息。

## 功能目标与实现边界

本案例不追求完整商业系统，而是聚焦排队叫号系统最核心、最能体现后端能力的部分：队列建模、并发取号、窗口叫号、过号处理、重新入队和 WebSocket 实时推送。

实现目标是构建一个单体版 Spring Boot 后端模块，既能直接运行验证，也方便后续拆分为微服务或接入真实业务系统。

### 核心业务流程

排队叫号的核心流程如下：

```text
用户取号
-> 系统生成排队号码
-> 号码进入等待队列
-> 窗口发起叫号
-> 系统从队列中取出下一个号码
-> 绑定当前服务窗口
-> 推送叫号消息
-> 用户前往窗口办理
-> 窗口完成办理
-> 系统记录办理结果
```

异常分支主要包括：

```text
窗口叫号后用户未到
-> 窗口标记过号
-> 系统记录过号状态
-> 用户重新入队
-> 系统按规则重新排序
-> 后续窗口再次叫号
```

本案例中的号码状态流转设计如下：

```text
WAITING    等待中
-> CALLING 叫号中
-> SERVING 办理中
-> FINISHED 已完成
```

过号分支：

```text
WAITING
-> CALLING
-> MISSED
-> REQUEUE
-> WAITING
```

窗口状态流转设计如下：

```text
IDLE      空闲
-> CALLING 叫号中
-> SERVING 办理中
-> IDLE    办理完成后重新空闲
```

本案例会重点实现以下几个接口：

| 接口           | 说明                              |
| -------------- | --------------------------------- |
| 用户取号       | 生成排队号码并加入 Redis 等待队列 |
| 窗口叫号       | 窗口从队列中取出下一个号码        |
| 开始办理       | 将号码状态从叫号中变更为办理中    |
| 办理完成       | 完成当前号码并释放窗口            |
| 标记过号       | 将当前叫号标记为过号              |
| 重新入队       | 过号用户重新进入等待队列          |
| 查询队列状态   | 查询等待人数、当前叫号、窗口状态  |
| WebSocket 推送 | 实时推送叫号信息给大屏或客户端    |

### 本案例实现范围

本案例实现核心业务闭环，不引入过多外围系统，保证代码可以直接理解和复用。

包含内容：

| 模块         | 是否实现 | 说明                                 |
| ------------ | -------- | ------------------------------------ |
| 用户取号     | 是       | 支持普通号码、优先号码               |
| Redis 队列   | 是       | 使用 ZSet 维护等待队列               |
| 并发控制     | 是       | 使用 Redisson 分布式锁控制取号和叫号 |
| 窗口叫号     | 是       | 空闲窗口从队列中获取下一个号码       |
| 窗口办理     | 是       | 支持开始办理、办理完成               |
| 过号处理     | 是       | 支持窗口标记过号                     |
| 重新入队     | 是       | 支持过号用户重新进入等待队列         |
| 状态记录     | 是       | MySQL 保存号码、窗口、叫号记录       |
| 实时推送     | 是       | 使用 WebSocket 推送叫号事件          |
| 等待统计     | 是       | 查询当前等待人数、当前叫号信息       |
| 管理后台页面 | 否       | 只提供后端接口                       |
| 多网点隔离   | 轻量实现 | 使用 branchId 字段预留网点维度       |
| 复杂排班     | 否       | 不实现窗口班次和员工排班             |
| MQ 削峰      | 可选     | 核心实现先不依赖 MQ                  |
| XXL-JOB 补偿 | 可选     | 后续可扩展自动过号、数据统计         |

核心边界如下：

```text
本案例重点解决：
1. 如何设计排队号码和窗口模型
2. 如何使用 Redis ZSet 实现有序等待队列
3. 如何保证并发取号、叫号时不重复、不乱序
4. 如何处理过号和重新入队
5. 如何将叫号结果实时推送给前端大屏
```

不重点展开：

```text
1. 用户登录认证
2. 复杂组织权限
3. 多租户隔离
4. 大屏前端页面
5. 语音播报设备对接
6. 线下打印机取号
7. 第三方硬件 SDK
```

### 技术栈选型

本案例使用偏实战的 Spring Boot 单体架构实现，后续可以平滑拆分为排队服务、窗口服务、推送服务。

| 技术                | 用途                                     |
| ------------------- | ---------------------------------------- |
| Spring Boot 3       | 后端基础框架                             |
| MyBatis-Plus        | 数据库 CRUD 与分页                       |
| MySQL 8             | 持久化排队号码、窗口、叫号记录           |
| Redis               | 维护等待队列、当前叫号缓存、窗口占用缓存 |
| Redisson            | 分布式锁，保证并发叫号安全               |
| WebSocket           | 实时推送叫号消息                         |
| Hutool              | 日期、字符串、集合、JSON 等工具处理      |
| Lombok              | 简化实体类、DTO、VO 代码                 |
| Knife4j / Springdoc | 接口文档，可选                           |
| XXL-JOB             | 定时补偿，可选扩展                       |
| RabbitMQ            | 异步通知，可选扩展                       |

推荐核心方案：

```text
MySQL 负责状态最终落库
Redis ZSet 负责高性能等待队列
Redisson Lock 负责并发控制
WebSocket 负责实时叫号推送
```

为什么等待队列选择 Redis ZSet：

```text
1. 可以通过 score 控制排序
2. 支持普通号和优先号混合排序
3. 获取队首数据性能较好
4. 支持按业务维度拆分队列 Key
5. 比 Redis List 更适合做优先级队列
```

队列排序规则设计：

```text
score = 优先级权重 + 取号时间戳
```

示例：

```text
普通号：score = 当前时间戳
优先号：score = 当前时间戳 - 10年毫秒数
重新入队：score = 当前时间戳 + 重排延迟毫秒数
```

Redis Key 设计：

```text
queue:waiting:{branchId}:{serviceType}
queue:current:{branchId}:{windowId}
queue:window:lock:{branchId}:{windowId}
queue:number:lock:{branchId}:{serviceType}
```

示例：

```text
queue:waiting:1001:GENERAL
queue:current:1001:WINDOW_01
queue:window:lock:1001:1
queue:number:lock:1001:GENERAL
```

本案例的后续代码会按照以下包结构展开：

```text
io.github.atengk.queue
├── controller
│   ├── QueueTicketController.java
│   └── ServiceWindowController.java
├── service
│   ├── QueueTicketService.java
│   ├── ServiceWindowService.java
│   └── QueuePushService.java
├── service.impl
│   ├── QueueTicketServiceImpl.java
│   ├── ServiceWindowServiceImpl.java
│   └── QueuePushServiceImpl.java
├── entity
│   ├── QueueTicket.java
│   ├── ServiceWindow.java
│   └── QueueCallRecord.java
├── mapper
│   ├── QueueTicketMapper.java
│   ├── ServiceWindowMapper.java
│   └── QueueCallRecordMapper.java
├── dto
│   ├── TakeNumberDTO.java
│   ├── CallNextDTO.java
│   └── RequeueDTO.java
├── vo
│   ├── QueueTicketVO.java
│   ├── CallNextVO.java
│   └── QueueStatusVO.java
├── enums
│   ├── TicketStatusEnum.java
│   ├── WindowStatusEnum.java
│   └── ServiceTypeEnum.java
├── config
│   ├── RedisConfig.java
│   ├── RedissonConfig.java
│   └── WebSocketConfig.java
└── websocket
    ├── QueueWebSocketHandler.java
    └── QueueWebSocketSessionManager.java
```

## 业务模型设计

本节先定义排队叫号系统中的核心业务对象。后续数据库表、Redis 队列、接口和服务代码都围绕这几个模型展开。该场景的关键点是“队列顺序、优先级插队、窗口分配、过号重排、实时推送”，这些也是原 README 中提到的核心难点。

### 排队号码模型

排队号码是用户取号后生成的业务凭证，也是队列系统中最核心的数据对象。一个号码从取号开始，依次经历等待、叫号、办理、完成、过号等状态。

| 字段               | 说明                                                  |
| ------------------ | ----------------------------------------------------- |
| id                 | 主键 ID                                               |
| branch_id          | 网点 ID，用于区分医院、银行网点、政务大厅等不同办理点 |
| service_type       | 业务类型，例如综合业务、优先业务、维修业务            |
| ticket_no          | 排队号码，例如 A001、V001                             |
| user_id            | 用户 ID，可为空，线下取号机取号时可以不绑定用户       |
| priority           | 优先级，数值越大优先级越高                            |
| status             | 号码状态                                              |
| queue_score        | Redis ZSet 排序分值                                   |
| take_time          | 取号时间                                              |
| call_time          | 叫号时间                                              |
| start_service_time | 开始办理时间                                          |
| finish_time        | 办理完成时间                                          |
| missed_count       | 过号次数                                              |
| current_window_id  | 当前叫号或办理窗口 ID                                 |
| remark             | 备注                                                  |

号码示例：

```text
普通业务号：A001、A002、A003
优先业务号：V001、V002
维修业务号：R001、R002
```

在本案例中，号码生成规则保持简单：

```text
ticketNo = 业务前缀 + 当日递增序号
```

示例：

```text
A001
A002
V001
R001
```

其中：

| 业务类型 | 前缀 | 说明         |
| -------- | ---- | ------------ |
| GENERAL  | A    | 普通综合业务 |
| VIP      | V    | 优先业务     |
| REPAIR   | R    | 维修业务     |

### 服务窗口模型

服务窗口表示线下办理业务的柜台、诊室、工位或客服坐席。窗口负责从等待队列中叫号，并绑定当前正在处理的号码。

| 字段              | 说明                              |
| ----------------- | --------------------------------- |
| id                | 主键 ID                           |
| branch_id         | 网点 ID                           |
| window_no         | 窗口编号，例如 1 号窗口、2 号窗口 |
| window_name       | 窗口名称                          |
| service_type      | 当前窗口支持的业务类型            |
| status            | 窗口状态                          |
| current_ticket_id | 当前正在叫号或办理的号码 ID       |
| current_ticket_no | 当前正在叫号或办理的号码          |
| operator_id       | 操作员 ID                         |
| enabled           | 是否启用                          |
| last_call_time    | 最近叫号时间                      |

窗口状态设计：

| 状态    | 说明                   |
| ------- | ---------------------- |
| IDLE    | 空闲，可继续叫号       |
| CALLING | 已叫号，等待用户到窗口 |
| SERVING | 办理中                 |
| CLOSED  | 关闭，不参与叫号       |

窗口分配规则采用“窗口主动叫号”模式：

```text
窗口空闲
-> 工作人员点击叫号
-> 系统从对应业务队列中取出下一个号码
-> 绑定窗口和号码
-> 推送叫号消息
```

这种方式比“系统自动分配窗口”更适合线下大厅，因为工作人员可以根据真实办理节奏主动拉取下一个号码。

### 叫号记录模型

叫号记录用于保存每一次窗口叫号行为，方便后续查询、审计、统计平均等待时间、统计窗口办理量。

| 字段            | 说明             |
| --------------- | ---------------- |
| id              | 主键 ID          |
| branch_id       | 网点 ID          |
| ticket_id       | 排队号码 ID      |
| ticket_no       | 排队号码         |
| window_id       | 窗口 ID          |
| window_no       | 窗口编号         |
| call_status     | 叫号结果         |
| call_time       | 叫号时间         |
| arrive_time     | 用户到达窗口时间 |
| finish_time     | 办理完成时间     |
| wait_seconds    | 等待秒数         |
| service_seconds | 办理秒数         |
| operator_id     | 操作员 ID        |
| remark          | 备注             |

叫号结果设计：

| 状态      | 说明                 |
| --------- | -------------------- |
| CALLING   | 叫号中               |
| ARRIVED   | 用户已到达，开始办理 |
| FINISHED  | 办理完成             |
| MISSED    | 过号                 |
| CANCELLED | 取消                 |

### 号码状态流转

号码状态是排队叫号系统中最需要控制的部分。接口实现时不能随意更新状态，而应该严格按照状态流转规则处理。

正常流程：

```text
WAITING
-> CALLING
-> SERVING
-> FINISHED
```

过号流程：

```text
WAITING
-> CALLING
-> MISSED
```

重新入队流程：

```text
MISSED
-> REQUEUE
-> WAITING
```

取消流程：

```text
WAITING
-> CANCELLED
```

状态说明：

| 状态      | 说明                   | 是否在 Redis 等待队列中 |
| --------- | ---------------------- | ----------------------- |
| WAITING   | 等待叫号               | 是                      |
| CALLING   | 已叫号，等待用户到窗口 | 否                      |
| SERVING   | 办理中                 | 否                      |
| FINISHED  | 已完成                 | 否                      |
| MISSED    | 已过号                 | 否                      |
| REQUEUE   | 重新入队中             | 短暂中间态              |
| CANCELLED | 已取消                 | 否                      |

后续 Service 实现中需要重点保证：

```text
1. 只有 WAITING 状态可以被叫号
2. 只有 CALLING 状态可以开始办理
3. 只有 CALLING 状态可以标记过号
4. 只有 SERVING 状态可以办理完成
5. 只有 MISSED 状态可以重新入队
```

## 数据库表设计

数据库用于保存最终业务状态，Redis 用于高性能排队和临时状态缓存。不能只依赖 Redis 保存排队数据，否则 Redis 数据丢失后无法恢复业务现场。

本案例使用 MySQL 8，表设计保持核心字段完整，不做过度抽象。

### 排队号码表

`queue_ticket` 用于保存用户取号后的号码信息。Redis 队列中只保存 ticketId，完整业务信息以 MySQL 为准。

下面的 SQL 用于创建排队号码表，包含号码状态、窗口绑定、取号时间、叫号时间和过号次数等核心字段。

```sql
CREATE TABLE queue_ticket (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    branch_id BIGINT NOT NULL COMMENT '网点ID',
    service_type VARCHAR(32) NOT NULL COMMENT '业务类型：GENERAL-普通业务，VIP-优先业务，REPAIR-维修业务',
    ticket_no VARCHAR(32) NOT NULL COMMENT '排队号码，例如 A001',
    user_id BIGINT NULL COMMENT '用户ID，线下匿名取号可为空',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级，数值越大优先级越高',
    status VARCHAR(32) NOT NULL COMMENT '号码状态',
    queue_score BIGINT NOT NULL DEFAULT 0 COMMENT 'Redis队列排序分值',
    current_window_id BIGINT NULL COMMENT '当前窗口ID',
    current_window_no VARCHAR(32) NULL COMMENT '当前窗口编号',
    take_time DATETIME NOT NULL COMMENT '取号时间',
    call_time DATETIME NULL COMMENT '叫号时间',
    start_service_time DATETIME NULL COMMENT '开始办理时间',
    finish_time DATETIME NULL COMMENT '办理完成时间',
    missed_count INT NOT NULL DEFAULT 0 COMMENT '过号次数',
    remark VARCHAR(255) NULL COMMENT '备注',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_branch_ticket_no (branch_id, ticket_no),
    KEY idx_branch_service_status (branch_id, service_type, status),
    KEY idx_user_id (user_id),
    KEY idx_take_time (take_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排队号码表';
```

关键索引说明：

| 索引                      | 作用                                   |
| ------------------------- | -------------------------------------- |
| uk_branch_ticket_no       | 保证同一个网点内号码不重复             |
| idx_branch_service_status | 支持按网点、业务类型、状态查询等待号码 |
| idx_user_id               | 支持用户查询自己的取号记录             |
| idx_take_time             | 支持按取号时间做统计                   |

### 服务窗口表

`service_window` 用于维护窗口基础信息和窗口当前状态。窗口叫号前必须判断窗口是否启用、是否空闲、是否支持当前业务类型。

下面的 SQL 用于创建服务窗口表，记录窗口编号、支持业务类型、窗口状态和当前处理号码。

```sql
CREATE TABLE service_window (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    branch_id BIGINT NOT NULL COMMENT '网点ID',
    window_no VARCHAR(32) NOT NULL COMMENT '窗口编号，例如 1、2、A01',
    window_name VARCHAR(64) NOT NULL COMMENT '窗口名称，例如 1号窗口',
    service_type VARCHAR(32) NOT NULL COMMENT '支持的业务类型',
    status VARCHAR(32) NOT NULL DEFAULT 'IDLE' COMMENT '窗口状态：IDLE-空闲，CALLING-叫号中，SERVING-办理中，CLOSED-关闭',
    current_ticket_id BIGINT NULL COMMENT '当前号码ID',
    current_ticket_no VARCHAR(32) NULL COMMENT '当前号码',
    operator_id BIGINT NULL COMMENT '当前操作员ID',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    last_call_time DATETIME NULL COMMENT '最近叫号时间',
    remark VARCHAR(255) NULL COMMENT '备注',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_branch_window_no (branch_id, window_no),
    KEY idx_branch_service_status (branch_id, service_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务窗口表';
```

初始化窗口数据示例：

```sql
INSERT INTO service_window
(branch_id, window_no, window_name, service_type, status, enabled, remark)
VALUES
(1001, '1', '1号综合窗口', 'GENERAL', 'IDLE', 1, '普通业务窗口'),
(1001, '2', '2号综合窗口', 'GENERAL', 'IDLE', 1, '普通业务窗口'),
(1001, '3', '3号优先窗口', 'VIP', 'IDLE', 1, '优先业务窗口'),
(1001, '4', '4号维修窗口', 'REPAIR', 'IDLE', 1, '维修业务窗口');
```

### 叫号记录表

`queue_call_record` 用于记录每一次叫号过程。即使号码最终过号，也需要保存叫号记录。

下面的 SQL 用于创建叫号记录表，后续可用于办理统计、等待时间分析和窗口绩效统计。

```sql
CREATE TABLE queue_call_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    branch_id BIGINT NOT NULL COMMENT '网点ID',
    ticket_id BIGINT NOT NULL COMMENT '排队号码ID',
    ticket_no VARCHAR(32) NOT NULL COMMENT '排队号码',
    window_id BIGINT NOT NULL COMMENT '窗口ID',
    window_no VARCHAR(32) NOT NULL COMMENT '窗口编号',
    call_status VARCHAR(32) NOT NULL COMMENT '叫号结果：CALLING-叫号中，ARRIVED-已到达，FINISHED-已完成，MISSED-过号，CANCELLED-取消',
    call_time DATETIME NOT NULL COMMENT '叫号时间',
    arrive_time DATETIME NULL COMMENT '用户到达窗口时间',
    finish_time DATETIME NULL COMMENT '办理完成时间',
    wait_seconds BIGINT NOT NULL DEFAULT 0 COMMENT '等待秒数',
    service_seconds BIGINT NOT NULL DEFAULT 0 COMMENT '办理秒数',
    operator_id BIGINT NULL COMMENT '操作员ID',
    remark VARCHAR(255) NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ticket_id (ticket_id),
    KEY idx_window_id (window_id),
    KEY idx_branch_call_time (branch_id, call_time),
    KEY idx_call_status (call_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='叫号记录表';
```

统计查询示例：

```sql
-- 查询某个网点当天办理完成数量
SELECT COUNT(1)
FROM queue_call_record
WHERE branch_id = 1001
  AND call_status = 'FINISHED'
  AND call_time >= CURDATE()
  AND call_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY);

-- 查询窗口平均办理时长
SELECT window_no, AVG(service_seconds) AS avg_service_seconds
FROM queue_call_record
WHERE branch_id = 1001
  AND call_status = 'FINISHED'
GROUP BY window_no;
```

## Redis 队列设计

Redis 负责排队系统中的高频操作，例如等待队列排序、获取队首号码、缓存当前叫号、窗口占用判断。MySQL 负责最终状态，Redis 负责运行时队列。

本案例不建议使用单纯的 Redis List，因为 List 只能天然表达先进先出，不方便处理优先级插队和重新入队排序。因此等待队列统一使用 Redis ZSet。

### 普通等待队列

普通等待队列使用 Redis ZSet 保存 ticketId。

Key 格式：

```text
queue:waiting:{branchId}:{serviceType}
```

示例：

```text
queue:waiting:1001:GENERAL
queue:waiting:1001:REPAIR
```

Value：

```text
ticketId
```

Score：

```text
取号时间戳毫秒值
```

示例：

```text
ZADD queue:waiting:1001:GENERAL 1778812800000 10001
ZADD queue:waiting:1001:GENERAL 1778812803000 10002
ZADD queue:waiting:1001:GENERAL 1778812806000 10003
```

窗口叫号时取分值最小的号码：

```text
ZRANGE queue:waiting:1001:GENERAL 0 0
ZREM queue:waiting:1001:GENERAL ticketId
```

实际代码中不能直接分两步裸操作，必须加 Redisson 分布式锁，避免多个窗口同时取到同一个号码。

### 优先级等待队列

优先级队列仍然使用同一个 ZSet，不单独拆 Key。通过 score 控制排序即可。

推荐规则：

```text
score = 当前时间戳 - 优先级偏移量
```

示例：

| 类型       | priority | score 规则               | 排序效果 |
| ---------- | -------- | ------------------------ | -------- |
| 普通号     | 0        | 当前时间戳               | 正常排队 |
| 优先号     | 10       | 当前时间戳 - 10 * 10分钟 | 向前插队 |
| 特殊优先号 | 20       | 当前时间戳 - 20 * 10分钟 | 更靠前   |

不建议所有优先号都强制排到最前面，否则普通号可能长期饥饿。使用“时间偏移”可以做到优先但不过度抢占。

示例：

```text
普通号 A010：score = 1778812800000
优先号 V001：score = 1778812800000 - 10 * 600000
```

重新入队时可以适当降低优先级：

```text
score = 当前时间戳 + missedCount * 60000
```

含义是：过号次数越多，重新入队越靠后，避免反复过号用户影响正常队列。

### 当前叫号缓存

当前叫号缓存用于大屏展示、客户端刷新和窗口状态恢复。

Key 格式：

```text
queue:current:{branchId}:{windowId}
```

示例：

```text
queue:current:1001:1
```

Value 建议保存 JSON：

```json
{
  "ticketId": 10001,
  "ticketNo": "A001",
  "windowId": 1,
  "windowNo": "1",
  "serviceType": "GENERAL",
  "status": "CALLING",
  "callTime": "2026-05-15 09:30:00"
}
```

过期时间建议设置为 2 小时：

```text
2小时内可用于窗口刷新恢复
办理完成或过号后主动删除
```

当前叫号缓存不是最终数据来源，只是为了提升实时查询效率。

### 窗口占用缓存

窗口占用缓存用于防止同一个窗口重复叫号。

Key 格式：

```text
queue:window:busy:{branchId}:{windowId}
```

示例：

```text
queue:window:busy:1001:1
```

Value：

```text
ticketId
```

推荐过期时间：

```text
30分钟
```

设计目的：

```text
1. 窗口已经叫号但用户未到，不能再次叫号
2. 窗口正在办理业务，不能再次叫号
3. 服务重启后可以结合 MySQL 恢复窗口状态
```

窗口释放时机：

```text
1. 办理完成
2. 标记过号
3. 管理员强制释放
```

Redis Key 汇总：

| Key                                           | 类型        | 说明             |
| --------------------------------------------- | ----------- | ---------------- |
| queue:waiting:{branchId}:{serviceType}        | ZSet        | 等待队列         |
| queue:current:{branchId}:{windowId}           | String      | 当前窗口叫号信息 |
| queue:window:busy:{branchId}:{windowId}       | String      | 窗口占用标识     |
| queue:lock:take:{branchId}:{serviceType}      | Lock        | 取号锁           |
| queue:lock:call:{branchId}:{windowId}         | Lock        | 窗口叫号锁       |
| queue:seq:{branchId}:{serviceType}:{yyyyMMdd} | String/Incr | 当日号码序列     |

## 项目依赖与配置

本节给出项目运行所需的核心依赖和配置。这里以 Spring Boot 3、MyBatis-Plus、Redis、Redisson、WebSocket、Hutool、Lombok 为基础技术栈。

### Maven 依赖配置

下面的依赖配置放在项目 `pom.xml` 中，用于引入 Web、MyBatis-Plus、Redis、Redisson、WebSocket、MySQL、Hutool 和 Lombok。

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST 接口能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot WebSocket：提供叫号实时推送能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <!-- Spring Data Redis：操作 Redis 队列和缓存 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Redisson：提供分布式锁，控制并发取号和窗口叫号 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.27.2</version>
    </dependency>

    <!-- MyBatis-Plus Spring Boot 3 Starter：简化数据库 CRUD -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 8 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool：日期、字符串、JSON、集合等工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.28</version>
    </dependency>

    <!-- Lombok：减少实体类、DTO、VO 样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Validation：接口参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Knife4j：接口文档，可按需移除 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>4.5.0</version>
    </dependency>
</dependencies>
```

如果你的项目使用 Spring Boot 依赖管理，可以将常用版本放在 `<properties>` 中统一维护：

```xml
<properties>
    <!-- Java 17 是 Spring Boot 3 常用基线版本 -->
    <java.version>17</java.version>

    <!-- MyBatis-Plus 版本 -->
    <mybatis-plus.version>3.5.7</mybatis-plus.version>

    <!-- Redisson 版本 -->
    <redisson.version>3.27.2</redisson.version>

    <!-- Hutool 版本 -->
    <hutool.version>5.8.28</hutool.version>
</properties>
```

### Redis 配置

Redis 用于保存等待队列、当前叫号缓存、窗口占用缓存和号码序列。生产环境建议开启持久化，并为排队业务使用独立 Redis DB 或独立 Redis 实例。

下面的配置放在 `src/main/resources/application.yml` 中，用于配置 Redis、Redisson 和排队业务参数。

```yaml
server:
  port: 8080

spring:
  application:
    name: queue-call-demo

  datasource:
    # MySQL 连接地址
    url: jdbc:mysql://127.0.0.1:3306/queue_demo?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      # Redis 地址
      host: 127.0.0.1
      # Redis 端口
      port: 6379
      # Redis 密码，没有密码可留空
      password:
      # 使用 0 号库，生产建议为业务单独规划 DB 或实例
      database: 0
      # 连接超时时间
      timeout: 3s

queue:
  redis:
    # 等待队列 Key 前缀
    waiting-key-prefix: "queue:waiting"
    # 当前叫号缓存 Key 前缀
    current-key-prefix: "queue:current"
    # 窗口占用 Key 前缀
    window-busy-key-prefix: "queue:window:busy"
    # 号码序列 Key 前缀
    seq-key-prefix: "queue:seq"
    # 当前叫号缓存过期秒数
    current-expire-seconds: 7200
    # 窗口占用缓存过期秒数
    window-busy-expire-seconds: 1800
    # 优先级每增加 1 级，向前偏移的毫秒数
    priority-offset-millis: 600000
```

下面的配置类用于将 `queue.redis` 参数绑定到 Java 对象中，后续构建 Redis Key 时统一使用。

文件位置：`src/main/java/io/github/atengk/queue/config/QueueRedisProperties.java`

```java
package io.github.atengk.queue.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 排队叫号 Redis 配置属性
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "queue.redis")
public class QueueRedisProperties {

    /**
     * 等待队列 Key 前缀
     */
    private String waitingKeyPrefix;

    /**
     * 当前叫号缓存 Key 前缀
     */
    private String currentKeyPrefix;

    /**
     * 窗口占用 Key 前缀
     */
    private String windowBusyKeyPrefix;

    /**
     * 号码序列 Key 前缀
     */
    private String seqKeyPrefix;

    /**
     * 当前叫号缓存过期秒数
     */
    private Long currentExpireSeconds;

    /**
     * 窗口占用缓存过期秒数
     */
    private Long windowBusyExpireSeconds;

    /**
     * 优先级偏移毫秒数
     */
    private Long priorityOffsetMillis;
}
```

下面的工具类用于统一生成 Redis Key，避免业务代码中散落字符串拼接。

文件位置：`src/main/java/io/github/atengk/queue/config/QueueRedisKeyBuilder.java`

```java
package io.github.atengk.queue.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 排队叫号 Redis Key 构建器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
@RequiredArgsConstructor
public class QueueRedisKeyBuilder {

    private final QueueRedisProperties properties;

    /**
     * 构建等待队列 Key
     *
     * @param branchId 网点ID
     * @param serviceType 业务类型
     * @return Redis Key
     */
    public String waitingKey(Long branchId, String serviceType) {
        return CharSequenceUtil.format("{}:{}:{}", properties.getWaitingKeyPrefix(), branchId, serviceType);
    }

    /**
     * 构建当前叫号缓存 Key
     *
     * @param branchId 网点ID
     * @param windowId 窗口ID
     * @return Redis Key
     */
    public String currentKey(Long branchId, Long windowId) {
        return CharSequenceUtil.format("{}:{}:{}", properties.getCurrentKeyPrefix(), branchId, windowId);
    }

    /**
     * 构建窗口占用 Key
     *
     * @param branchId 网点ID
     * @param windowId 窗口ID
     * @return Redis Key
     */
    public String windowBusyKey(Long branchId, Long windowId) {
        return CharSequenceUtil.format("{}:{}:{}", properties.getWindowBusyKeyPrefix(), branchId, windowId);
    }

    /**
     * 构建取号序列 Key
     *
     * @param branchId 网点ID
     * @param serviceType 业务类型
     * @return Redis Key
     */
    public String seqKey(Long branchId, String serviceType) {
        String today = DateUtil.format(new Date(), "yyyyMMdd");
        return CharSequenceUtil.format("{}:{}:{}:{}", properties.getSeqKeyPrefix(), branchId, serviceType, today);
    }

    /**
     * 构建取号锁 Key
     *
     * @param branchId 网点ID
     * @param serviceType 业务类型
     * @return Redis Key
     */
    public String takeLockKey(Long branchId, String serviceType) {
        return CharSequenceUtil.format("queue:lock:take:{}:{}", branchId, serviceType);
    }

    /**
     * 构建叫号锁 Key
     *
     * @param branchId 网点ID
     * @param windowId 窗口ID
     * @return Redis Key
     */
    public String callLockKey(Long branchId, Long windowId) {
        return CharSequenceUtil.format("queue:lock:call:{}:{}", branchId, windowId);
    }
}
```

### WebSocket 配置

WebSocket 用于将叫号结果实时推送给大屏、窗口端或用户端。后端只负责推送消息，前端收到消息后可以展示“请 A001 到 1 号窗口办理”。

WebSocket 地址设计：

```text
ws://localhost:8080/ws/queue?branchId=1001
```

其中：

| 参数     | 说明                                |
| -------- | ----------------------------------- |
| branchId | 网点 ID，用于区分不同大厅的大屏连接 |

下面的配置类用于注册 WebSocket 处理器，允许前端通过 `/ws/queue` 建立连接。

文件位置：`src/main/java/io/github/atengk/queue/config/WebSocketConfig.java`

```java
package io.github.atengk.queue.config;

import io.github.atengk.queue.websocket.QueueWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 排队叫号 WebSocket 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final QueueWebSocketHandler queueWebSocketHandler;

    /**
     * 注册 WebSocket 处理器
     *
     * @param registry WebSocket处理器注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(queueWebSocketHandler, "/ws/queue")
                // 示例项目放开跨域，生产环境应配置为具体域名
                .setAllowedOrigins("*");
    }
}
```

下面的会话管理器用于按网点保存 WebSocket 连接，后续叫号时可以按 branchId 推送给对应大厅。

文件位置：`src/main/java/io/github/atengk/queue/websocket/QueueWebSocketSessionManager.java`

```java
package io.github.atengk.queue.websocket;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 排队叫号 WebSocket 会话管理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class QueueWebSocketSessionManager {

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();

    /**
     * 添加连接
     *
     * @param branchId 网点ID
     * @param session WebSocket连接
     */
    public void addSession(Long branchId, WebSocketSession session) {
        sessionMap.computeIfAbsent(branchId, key -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("排队叫号WebSocket连接已建立，网点ID：{}，连接ID：{}", branchId, session.getId());
    }

    /**
     * 移除连接
     *
     * @param branchId 网点ID
     * @param session WebSocket连接
     */
    public void removeSession(Long branchId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionMap.get(branchId);
        if (CollUtil.isNotEmpty(sessions)) {
            sessions.remove(session);
        }
        log.info("排队叫号WebSocket连接已移除，网点ID：{}，连接ID：{}", branchId, session.getId());
    }

    /**
     * 获取网点连接集合
     *
     * @param branchId 网点ID
     * @return 连接集合
     */
    public Set<WebSocketSession> getSessions(Long branchId) {
        return sessionMap.getOrDefault(branchId, Set.of());
    }
}
```

下面的处理器负责建立连接、解析 branchId，并处理连接关闭逻辑。

文件位置：`src/main/java/io/github/atengk/queue/websocket/QueueWebSocketHandler.java`

```java
package io.github.atengk.queue.websocket;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.net.url.UrlQuery;
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
 * 排队叫号 WebSocket 处理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueWebSocketHandler extends TextWebSocketHandler {

    private static final String BRANCH_ID_KEY = "branchId";

    private final QueueWebSocketSessionManager sessionManager;

    /**
     * 建立连接
     *
     * @param session WebSocket连接
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long branchId = parseBranchId(session);
        if (branchId == null) {
            log.warn("排队叫号WebSocket连接缺少branchId，连接ID：{}", session.getId());
            return;
        }

        session.getAttributes().put(BRANCH_ID_KEY, branchId);
        sessionManager.addSession(branchId, session);
    }

    /**
     * 接收客户端消息
     *
     * @param session WebSocket连接
     * @param message 文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("收到排队叫号WebSocket消息，连接ID：{}，内容：{}", session.getId(), message.getPayload());
    }

    /**
     * 关闭连接
     *
     * @param session WebSocket连接
     * @param status 关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long branchId = Convert.toLong(session.getAttributes().get(BRANCH_ID_KEY), null);
        if (branchId != null) {
            sessionManager.removeSession(branchId, session);
        }
    }

    /**
     * 解析网点ID
     *
     * @param session WebSocket连接
     * @return 网点ID
     */
    private Long parseBranchId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || StrUtil.isBlank(uri.getQuery())) {
            return null;
        }

        UrlQuery query = UrlQuery.of(uri.getQuery(), null);
        return Convert.toLong(query.get(BRANCH_ID_KEY), null);
    }
}
```

### MyBatis-Plus 配置

MyBatis-Plus 用于简化实体表映射、分页查询、乐观锁和逻辑删除。本案例中的号码表和窗口表都带有 `version` 字段，后续状态更新时可以使用乐观锁减少并发覆盖问题。

下面的配置放在 `application.yml` 中，用于开启 MyBatis-Plus 基础能力。

```yaml
mybatis-plus:
  # Mapper XML 位置，如果只用 BaseMapper 可不创建 XML
  mapper-locations: classpath*:/mapper/**/*.xml

  # 实体类包路径
  type-aliases-package: io.github.atengk.queue.entity

  configuration:
    # 控制台打印 SQL，开发环境开启，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # 开启下划线转驼峰
    map-underscore-to-camel-case: true

  global-config:
    db-config:
      # 主键自增
      id-type: auto
      # 逻辑删除字段
      logic-delete-field: deleted
      # 已删除值
      logic-delete-value: 1
      # 未删除值
      logic-not-delete-value: 0
```

下面的配置类启用分页插件和乐观锁插件。

文件位置：`src/main/java/io/github/atengk/queue/config/MyBatisPlusConfig.java`

```java
package io.github.atengk.queue.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
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
public class MyBatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 插件
     *
     * @return MyBatis-Plus拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 乐观锁插件：用于控制窗口状态、号码状态并发更新
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页插件：用于后续查询排队记录、叫号记录
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
```

后续实体类中的版本字段需要这样声明：

```java
@Version
private Integer version;
```

逻辑删除字段需要这样声明：

```java
@TableLogic
private Integer deleted;
```

这部分配置完成后，项目已经具备以下基础能力：

```text
1. MySQL 保存排队号码、窗口和叫号记录
2. Redis 保存等待队列和窗口运行时缓存
3. Redisson 提供并发锁
4. WebSocket 支持叫号实时推送
5. MyBatis-Plus 支持基础 CRUD、分页、乐观锁和逻辑删除
```

## 核心代码实现

下面进入核心业务实现。代码基于前文的数据库表、Redis Key 设计和 Spring Boot 3 技术栈展开，重点覆盖取号、叫号、办理完成、过号、重新入队、当前叫号查询和实时推送。该场景的核心难点包括队列顺序、优先级插队、窗口分配、过号重排、并发取号和实时推送。

本节默认基础包路径为：

```text
io.github.atengk.queue
```

先补齐核心支撑类，后续接口和业务服务都依赖这些类。

核心文件结构如下：

```text
src/main/java/io/github/atengk/queue
├── common
│   └── Result.java
├── controller
│   └── QueueTicketController.java
├── dto
│   ├── CallNextDTO.java
│   ├── RequeueDTO.java
│   ├── TakeNumberDTO.java
│   └── WindowActionDTO.java
├── entity
│   ├── QueueCallRecord.java
│   ├── QueueTicket.java
│   └── ServiceWindow.java
├── enums
│   ├── CallStatusEnum.java
│   ├── ServiceTypeEnum.java
│   ├── TicketStatusEnum.java
│   └── WindowStatusEnum.java
├── mapper
│   ├── QueueCallRecordMapper.java
│   ├── QueueTicketMapper.java
│   └── ServiceWindowMapper.java
├── service
│   ├── QueuePushService.java
│   └── QueueTicketService.java
├── service/impl
│   ├── QueuePushServiceImpl.java
│   └── QueueTicketServiceImpl.java
└── vo
    ├── CallNextVO.java
    ├── CurrentCallVO.java
    ├── QueueStatusVO.java
    └── QueueTicketVO.java
```

### 支撑枚举与通用返回

下面的枚举用于统一控制号码状态、窗口状态、叫号记录状态和业务类型，避免业务代码中散落硬编码字符串。

文件位置：`src/main/java/io/github/atengk/queue/enums/TicketStatusEnum.java`

```java
package io.github.atengk.queue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 排队号码状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum TicketStatusEnum {

    WAITING("WAITING", "等待中"),
    CALLING("CALLING", "叫号中"),
    SERVING("SERVING", "办理中"),
    FINISHED("FINISHED", "已完成"),
    MISSED("MISSED", "已过号"),
    REQUEUE("REQUEUE", "重新入队中"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/queue/enums/WindowStatusEnum.java`

```java
package io.github.atengk.queue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 服务窗口状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum WindowStatusEnum {

    IDLE("IDLE", "空闲"),
    CALLING("CALLING", "叫号中"),
    SERVING("SERVING", "办理中"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/queue/enums/CallStatusEnum.java`

```java
package io.github.atengk.queue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 叫号记录状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum CallStatusEnum {

    CALLING("CALLING", "叫号中"),
    ARRIVED("ARRIVED", "已到达"),
    FINISHED("FINISHED", "已完成"),
    MISSED("MISSED", "已过号"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/queue/enums/ServiceTypeEnum.java`

```java
package io.github.atengk.queue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum ServiceTypeEnum {

    GENERAL("GENERAL", "A", "普通业务"),
    VIP("VIP", "V", "优先业务"),
    REPAIR("REPAIR", "R", "维修业务");

    private final String code;
    private final String prefix;
    private final String desc;

    /**
     * 根据业务类型获取号码前缀
     *
     * @param code 业务类型
     * @return 号码前缀
     */
    public static String getPrefix(String code) {
        for (ServiceTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value.getPrefix();
            }
        }
        return GENERAL.getPrefix();
    }
}
```

下面的返回对象用于统一接口响应。

文件位置：`src/main/java/io/github/atengk/queue/common/Result.java`

```java
package io.github.atengk.queue.common;

import lombok.Data;

/**
 * 通用接口响应对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @return 响应对象
     * @param <T> 数据类型
     */
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 成功响应
     *
     * @return 响应对象
     */
    public static Result<Void> ok() {
        return ok(null);
    }
}
```

### 实体与 Mapper

下面是排队号码实体，对应前文的 `queue_ticket` 表。

文件位置：`src/main/java/io/github/atengk/queue/entity/QueueTicket.java`

```java
package io.github.atengk.queue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排队号码实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("queue_ticket")
public class QueueTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long branchId;

    private String serviceType;

    private String ticketNo;

    private Long userId;

    private Integer priority;

    private String status;

    private Long queueScore;

    private Long currentWindowId;

    private String currentWindowNo;

    private LocalDateTime takeTime;

    private LocalDateTime callTime;

    private LocalDateTime startServiceTime;

    private LocalDateTime finishTime;

    private Integer missedCount;

    private String remark;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

下面是服务窗口实体，对应 `service_window` 表。

文件位置：`src/main/java/io/github/atengk/queue/entity/ServiceWindow.java`

```java
package io.github.atengk.queue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务窗口实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("service_window")
public class ServiceWindow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long branchId;

    private String windowNo;

    private String windowName;

    private String serviceType;

    private String status;

    private Long currentTicketId;

    private String currentTicketNo;

    private Long operatorId;

    private Integer enabled;

    private LocalDateTime lastCallTime;

    private String remark;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

下面是叫号记录实体，对应 `queue_call_record` 表。

文件位置：`src/main/java/io/github/atengk/queue/entity/QueueCallRecord.java`

```java
package io.github.atengk.queue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 叫号记录实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("queue_call_record")
public class QueueCallRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long branchId;

    private Long ticketId;

    private String ticketNo;

    private Long windowId;

    private String windowNo;

    private String callStatus;

    private LocalDateTime callTime;

    private LocalDateTime arriveTime;

    private LocalDateTime finishTime;

    private Long waitSeconds;

    private Long serviceSeconds;

    private Long operatorId;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

下面是三个 Mapper 接口，使用 MyBatis-Plus 的 `BaseMapper` 即可完成本案例核心 CRUD。

文件位置：`src/main/java/io/github/atengk/queue/mapper/QueueTicketMapper.java`

```java
package io.github.atengk.queue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.queue.entity.QueueTicket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排队号码 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface QueueTicketMapper extends BaseMapper<QueueTicket> {
}
```

文件位置：`src/main/java/io/github/atengk/queue/mapper/ServiceWindowMapper.java`

```java
package io.github.atengk.queue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.queue.entity.ServiceWindow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务窗口 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ServiceWindowMapper extends BaseMapper<ServiceWindow> {
}
```

文件位置：`src/main/java/io/github/atengk/queue/mapper/QueueCallRecordMapper.java`

```java
package io.github.atengk.queue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.queue.entity.QueueCallRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 叫号记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface QueueCallRecordMapper extends BaseMapper<QueueCallRecord> {
}
```

### DTO 与 VO

下面的 DTO 用于接收取号、叫号、窗口动作和重新入队请求。

文件位置：`src/main/java/io/github/atengk/queue/dto/TakeNumberDTO.java`

```java
package io.github.atengk.queue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 取号请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class TakeNumberDTO {

    @NotNull(message = "网点ID不能为空")
    private Long branchId;

    @NotBlank(message = "业务类型不能为空")
    private String serviceType;

    private Long userId;

    private Integer priority;
}
```

文件位置：`src/main/java/io/github/atengk/queue/dto/CallNextDTO.java`

```java
package io.github.atengk.queue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 窗口叫号请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CallNextDTO {

    @NotNull(message = "网点ID不能为空")
    private Long branchId;

    @NotNull(message = "窗口ID不能为空")
    private Long windowId;

    private Long operatorId;
}
```

文件位置：`src/main/java/io/github/atengk/queue/dto/WindowActionDTO.java`

```java
package io.github.atengk.queue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 窗口操作请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class WindowActionDTO {

    @NotNull(message = "网点ID不能为空")
    private Long branchId;

    @NotNull(message = "窗口ID不能为空")
    private Long windowId;

    private Long operatorId;
}
```

文件位置：`src/main/java/io/github/atengk/queue/dto/RequeueDTO.java`

```java
package io.github.atengk.queue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 重新入队请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class RequeueDTO {

    @NotNull(message = "排队号码ID不能为空")
    private Long ticketId;
}
```

下面的 VO 用于返回取号结果、叫号结果和队列状态。

文件位置：`src/main/java/io/github/atengk/queue/vo/QueueTicketVO.java`

```java
package io.github.atengk.queue.vo;

import lombok.Data;

/**
 * 排队号码响应对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class QueueTicketVO {

    private Long ticketId;

    private String ticketNo;

    private String serviceType;

    private String status;

    private Long waitingCount;
}
```

文件位置：`src/main/java/io/github/atengk/queue/vo/CallNextVO.java`

```java
package io.github.atengk.queue.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 叫号响应对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CallNextVO {

    private Long ticketId;

    private String ticketNo;

    private Long windowId;

    private String windowNo;

    private String serviceType;

    private String status;

    private LocalDateTime callTime;
}
```

文件位置：`src/main/java/io/github/atengk/queue/vo/CurrentCallVO.java`

```java
package io.github.atengk.queue.vo;

import lombok.Data;

/**
 * 当前叫号展示对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CurrentCallVO {

    private Long windowId;

    private String windowNo;

    private Long ticketId;

    private String ticketNo;

    private String windowStatus;
}
```

文件位置：`src/main/java/io/github/atengk/queue/vo/QueueStatusVO.java`

```java
package io.github.atengk.queue.vo;

import lombok.Data;

import java.util.List;

/**
 * 队列状态响应对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class QueueStatusVO {

    private Long branchId;

    private String serviceType;

    private Long waitingCount;

    private List<CurrentCallVO> currentCalls;
}
```

### Service 接口

下面的接口定义排队叫号核心能力。

文件位置：`src/main/java/io/github/atengk/queue/service/QueueTicketService.java`

```java
package io.github.atengk.queue.service;

import io.github.atengk.queue.dto.CallNextDTO;
import io.github.atengk.queue.dto.RequeueDTO;
import io.github.atengk.queue.dto.TakeNumberDTO;
import io.github.atengk.queue.dto.WindowActionDTO;
import io.github.atengk.queue.vo.CallNextVO;
import io.github.atengk.queue.vo.QueueStatusVO;
import io.github.atengk.queue.vo.QueueTicketVO;

/**
 * 排队号码业务服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface QueueTicketService {

    /**
     * 用户取号
     *
     * @param dto 取号参数
     * @return 取号结果
     */
    QueueTicketVO takeNumber(TakeNumberDTO dto);

    /**
     * 窗口叫下一个号
     *
     * @param dto 叫号参数
     * @return 叫号结果
     */
    CallNextVO callNext(CallNextDTO dto);

    /**
     * 开始办理
     *
     * @param dto 窗口操作参数
     */
    void startService(WindowActionDTO dto);

    /**
     * 办理完成
     *
     * @param dto 窗口操作参数
     */
    void finishService(WindowActionDTO dto);

    /**
     * 标记过号
     *
     * @param dto 窗口操作参数
     */
    void markMissed(WindowActionDTO dto);

    /**
     * 重新入队
     *
     * @param dto 重新入队参数
     * @return 取号结果
     */
    QueueTicketVO requeue(RequeueDTO dto);

    /**
     * 查询当前队列状态
     *
     * @param branchId 网点ID
     * @param serviceType 业务类型
     * @return 队列状态
     */
    QueueStatusVO currentStatus(Long branchId, String serviceType);
}
```

### 取号接口实现

取号接口要解决两个问题：生成当天唯一号码、将号码加入 Redis 等待队列。这里使用 Redis `INCR` 生成业务序列，使用 Redis ZSet 保存等待队列。

下面是核心 Service 实现类，包含取号、叫号、办理完成、过号、重新入队和查询逻辑。

文件位置：`src/main/java/io/github/atengk/queue/service/impl/QueueTicketServiceImpl.java`

```java
package io.github.atengk.queue.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.queue.config.QueueRedisKeyBuilder;
import io.github.atengk.queue.config.QueueRedisProperties;
import io.github.atengk.queue.dto.CallNextDTO;
import io.github.atengk.queue.dto.RequeueDTO;
import io.github.atengk.queue.dto.TakeNumberDTO;
import io.github.atengk.queue.dto.WindowActionDTO;
import io.github.atengk.queue.entity.QueueCallRecord;
import io.github.atengk.queue.entity.QueueTicket;
import io.github.atengk.queue.entity.ServiceWindow;
import io.github.atengk.queue.enums.CallStatusEnum;
import io.github.atengk.queue.enums.ServiceTypeEnum;
import io.github.atengk.queue.enums.TicketStatusEnum;
import io.github.atengk.queue.enums.WindowStatusEnum;
import io.github.atengk.queue.mapper.QueueCallRecordMapper;
import io.github.atengk.queue.mapper.QueueTicketMapper;
import io.github.atengk.queue.mapper.ServiceWindowMapper;
import io.github.atengk.queue.service.QueuePushService;
import io.github.atengk.queue.service.QueueTicketService;
import io.github.atengk.queue.vo.CallNextVO;
import io.github.atengk.queue.vo.CurrentCallVO;
import io.github.atengk.queue.vo.QueueStatusVO;
import io.github.atengk.queue.vo.QueueTicketVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 排队号码业务服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueTicketServiceImpl extends ServiceImpl<QueueTicketMapper, QueueTicket> implements QueueTicketService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final QueueRedisKeyBuilder redisKeyBuilder;
    private final QueueRedisProperties redisProperties;
    private final ServiceWindowMapper serviceWindowMapper;
    private final QueueCallRecordMapper callRecordMapper;
    private final QueuePushService queuePushService;

    /**
     * 用户取号
     *
     * @param dto 取号参数
     * @return 取号结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QueueTicketVO takeNumber(TakeNumberDTO dto) {
        RLock lock = redissonClient.getLock(redisKeyBuilder.takeLockKey(dto.getBranchId(), dto.getServiceType()));
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("当前取号人数较多，请稍后重试");
            }

            String seqKey = redisKeyBuilder.seqKey(dto.getBranchId(), dto.getServiceType());
            Long seq = stringRedisTemplate.opsForValue().increment(seqKey);
            stringRedisTemplate.expire(seqKey, 2, TimeUnit.DAYS);

            Integer priority = ObjectUtil.defaultIfNull(dto.getPriority(), 0);
            long nowMillis = System.currentTimeMillis();
            long queueScore = nowMillis - priority.longValue() * redisProperties.getPriorityOffsetMillis();

            QueueTicket ticket = new QueueTicket();
            ticket.setBranchId(dto.getBranchId());
            ticket.setServiceType(dto.getServiceType());
            ticket.setTicketNo(buildTicketNo(dto.getServiceType(), seq));
            ticket.setUserId(dto.getUserId());
            ticket.setPriority(priority);
            ticket.setStatus(TicketStatusEnum.WAITING.getCode());
            ticket.setQueueScore(queueScore);
            ticket.setTakeTime(LocalDateTime.now());
            ticket.setMissedCount(0);
            ticket.setVersion(0);
            ticket.setDeleted(0);
            baseMapper.insert(ticket);

            String waitingKey = redisKeyBuilder.waitingKey(dto.getBranchId(), dto.getServiceType());
            stringRedisTemplate.opsForZSet().add(waitingKey, String.valueOf(ticket.getId()), queueScore);

            Long waitingCount = stringRedisTemplate.opsForZSet().zCard(waitingKey);
            log.info("用户取号成功，网点ID：{}，业务类型：{}，号码：{}", dto.getBranchId(), dto.getServiceType(), ticket.getTicketNo());

            return buildTicketVO(ticket, waitingCount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("取号操作被中断", e);
        } finally {
            unlock(lock, locked);
        }
    }

    /**
     * 窗口叫下一个号
     *
     * @param dto 叫号参数
     * @return 叫号结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallNextVO callNext(CallNextDTO dto) {
        RLock lock = redissonClient.getLock(redisKeyBuilder.callLockKey(dto.getBranchId(), dto.getWindowId()));
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("窗口正在叫号处理中，请稍后重试");
            }

            ServiceWindow window = getAndCheckWindow(dto.getBranchId(), dto.getWindowId());
            if (!StrUtil.equals(WindowStatusEnum.IDLE.getCode(), window.getStatus())) {
                throw new IllegalStateException("窗口不是空闲状态，不能继续叫号");
            }

            String busyKey = redisKeyBuilder.windowBusyKey(dto.getBranchId(), dto.getWindowId());
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(busyKey))) {
                throw new IllegalStateException("窗口已有正在处理的号码");
            }

            String waitingKey = redisKeyBuilder.waitingKey(dto.getBranchId(), window.getServiceType());
            QueueTicket ticket = pollWaitingTicket(waitingKey);
            if (ticket == null) {
                throw new IllegalStateException("当前队列暂无等待号码");
            }

            LocalDateTime now = LocalDateTime.now();
            ticket.setStatus(TicketStatusEnum.CALLING.getCode());
            ticket.setCurrentWindowId(window.getId());
            ticket.setCurrentWindowNo(window.getWindowNo());
            ticket.setCallTime(now);
            baseMapper.updateById(ticket);

            window.setStatus(WindowStatusEnum.CALLING.getCode());
            window.setCurrentTicketId(ticket.getId());
            window.setCurrentTicketNo(ticket.getTicketNo());
            window.setOperatorId(dto.getOperatorId());
            window.setLastCallTime(now);
            serviceWindowMapper.updateById(window);

            QueueCallRecord record = new QueueCallRecord();
            record.setBranchId(dto.getBranchId());
            record.setTicketId(ticket.getId());
            record.setTicketNo(ticket.getTicketNo());
            record.setWindowId(window.getId());
            record.setWindowNo(window.getWindowNo());
            record.setCallStatus(CallStatusEnum.CALLING.getCode());
            record.setCallTime(now);
            record.setWaitSeconds(calcSeconds(ticket.getTakeTime(), now));
            record.setServiceSeconds(0L);
            record.setOperatorId(dto.getOperatorId());
            record.setDeleted(0);
            callRecordMapper.insert(record);

            CallNextVO vo = buildCallNextVO(ticket, window, now);
            stringRedisTemplate.opsForValue().set(
                    redisKeyBuilder.currentKey(dto.getBranchId(), dto.getWindowId()),
                    JSONUtil.toJsonStr(vo),
                    redisProperties.getCurrentExpireSeconds(),
                    TimeUnit.SECONDS
            );
            stringRedisTemplate.opsForValue().set(
                    busyKey,
                    String.valueOf(ticket.getId()),
                    redisProperties.getWindowBusyExpireSeconds(),
                    TimeUnit.SECONDS
            );

            queuePushService.pushCallMessage(dto.getBranchId(), vo);
            log.info("窗口叫号成功，网点ID：{}，窗口：{}，号码：{}", dto.getBranchId(), window.getWindowNo(), ticket.getTicketNo());

            return vo;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("叫号操作被中断", e);
        } finally {
            unlock(lock, locked);
        }
    }

    /**
     * 开始办理
     *
     * @param dto 窗口操作参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startService(WindowActionDTO dto) {
        RLock lock = redissonClient.getLock(redisKeyBuilder.callLockKey(dto.getBranchId(), dto.getWindowId()));
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("窗口正在处理中，请稍后重试");
            }

            ServiceWindow window = getAndCheckWindow(dto.getBranchId(), dto.getWindowId());
            if (!StrUtil.equals(WindowStatusEnum.CALLING.getCode(), window.getStatus())) {
                throw new IllegalStateException("窗口不是叫号中状态，不能开始办理");
            }

            QueueTicket ticket = baseMapper.selectById(window.getCurrentTicketId());
            if (ticket == null || !StrUtil.equals(TicketStatusEnum.CALLING.getCode(), ticket.getStatus())) {
                throw new IllegalStateException("当前号码状态异常，不能开始办理");
            }

            LocalDateTime now = LocalDateTime.now();
            ticket.setStatus(TicketStatusEnum.SERVING.getCode());
            ticket.setStartServiceTime(now);
            baseMapper.updateById(ticket);

            window.setStatus(WindowStatusEnum.SERVING.getCode());
            window.setOperatorId(dto.getOperatorId());
            serviceWindowMapper.updateById(window);

            QueueCallRecord record = getLatestCallRecord(ticket.getId(), window.getId());
            if (record != null) {
                record.setCallStatus(CallStatusEnum.ARRIVED.getCode());
                record.setArriveTime(now);
                record.setOperatorId(dto.getOperatorId());
                callRecordMapper.updateById(record);
            }

            queuePushService.pushStatusMessage(dto.getBranchId(), "QUEUE_SERVING", ticket.getTicketNo(), window.getWindowNo());
            log.info("号码开始办理，网点ID：{}，窗口：{}，号码：{}", dto.getBranchId(), window.getWindowNo(), ticket.getTicketNo());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("开始办理操作被中断", e);
        } finally {
            unlock(lock, locked);
        }
    }

    /**
     * 办理完成
     *
     * @param dto 窗口操作参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishService(WindowActionDTO dto) {
        RLock lock = redissonClient.getLock(redisKeyBuilder.callLockKey(dto.getBranchId(), dto.getWindowId()));
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("窗口正在处理中，请稍后重试");
            }

            ServiceWindow window = getAndCheckWindow(dto.getBranchId(), dto.getWindowId());
            if (window.getCurrentTicketId() == null) {
                throw new IllegalStateException("窗口当前没有正在处理的号码");
            }

            QueueTicket ticket = baseMapper.selectById(window.getCurrentTicketId());
            if (ticket == null) {
                throw new IllegalStateException("当前号码不存在");
            }

            if (!StrUtil.equalsAny(ticket.getStatus(), TicketStatusEnum.CALLING.getCode(), TicketStatusEnum.SERVING.getCode())) {
                throw new IllegalStateException("当前号码状态不能办理完成");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = ObjectUtil.defaultIfNull(ticket.getStartServiceTime(), ticket.getCallTime());

            ticket.setStatus(TicketStatusEnum.FINISHED.getCode());
            ticket.setFinishTime(now);
            if (ticket.getStartServiceTime() == null) {
                ticket.setStartServiceTime(now);
            }
            baseMapper.updateById(ticket);

            QueueCallRecord record = getLatestCallRecord(ticket.getId(), window.getId());
            if (record != null) {
                record.setCallStatus(CallStatusEnum.FINISHED.getCode());
                record.setFinishTime(now);
                record.setServiceSeconds(calcSeconds(startTime, now));
                record.setOperatorId(dto.getOperatorId());
                callRecordMapper.updateById(record);
            }

            releaseWindow(dto.getBranchId(), window);
            queuePushService.pushStatusMessage(dto.getBranchId(), "QUEUE_FINISHED", ticket.getTicketNo(), window.getWindowNo());

            log.info("号码办理完成，网点ID：{}，窗口：{}，号码：{}", dto.getBranchId(), window.getWindowNo(), ticket.getTicketNo());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("办理完成操作被中断", e);
        } finally {
            unlock(lock, locked);
        }
    }

    /**
     * 标记过号
     *
     * @param dto 窗口操作参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMissed(WindowActionDTO dto) {
        RLock lock = redissonClient.getLock(redisKeyBuilder.callLockKey(dto.getBranchId(), dto.getWindowId()));
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("窗口正在处理中，请稍后重试");
            }

            ServiceWindow window = getAndCheckWindow(dto.getBranchId(), dto.getWindowId());
            if (!StrUtil.equals(WindowStatusEnum.CALLING.getCode(), window.getStatus())) {
                throw new IllegalStateException("只有叫号中状态可以标记过号");
            }

            QueueTicket ticket = baseMapper.selectById(window.getCurrentTicketId());
            if (ticket == null || !StrUtil.equals(TicketStatusEnum.CALLING.getCode(), ticket.getStatus())) {
                throw new IllegalStateException("当前号码状态异常，不能过号");
            }

            ticket.setStatus(TicketStatusEnum.MISSED.getCode());
            ticket.setMissedCount(ObjectUtil.defaultIfNull(ticket.getMissedCount(), 0) + 1);
            baseMapper.updateById(ticket);

            QueueCallRecord record = getLatestCallRecord(ticket.getId(), window.getId());
            if (record != null) {
                record.setCallStatus(CallStatusEnum.MISSED.getCode());
                callRecordMapper.updateById(record);
            }

            releaseWindow(dto.getBranchId(), window);
            queuePushService.pushStatusMessage(dto.getBranchId(), "QUEUE_MISSED", ticket.getTicketNo(), window.getWindowNo());

            log.info("号码已过号，网点ID：{}，窗口：{}，号码：{}", dto.getBranchId(), window.getWindowNo(), ticket.getTicketNo());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("过号操作被中断", e);
        } finally {
            unlock(lock, locked);
        }
    }

    /**
     * 重新入队
     *
     * @param dto 重新入队参数
     * @return 取号结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QueueTicketVO requeue(RequeueDTO dto) {
        RLock lock = redissonClient.getLock("queue:lock:requeue:" + dto.getTicketId());
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("重新入队处理中，请稍后重试");
            }

            QueueTicket ticket = baseMapper.selectById(dto.getTicketId());
            if (ticket == null) {
                throw new IllegalArgumentException("排队号码不存在");
            }
            if (!StrUtil.equals(TicketStatusEnum.MISSED.getCode(), ticket.getStatus())) {
                throw new IllegalStateException("只有过号号码可以重新入队");
            }

            int missedCount = ObjectUtil.defaultIfNull(ticket.getMissedCount(), 0);
            long score = System.currentTimeMillis() + missedCount * 60_000L;

            ticket.setStatus(TicketStatusEnum.WAITING.getCode());
            ticket.setQueueScore(score);
            ticket.setCurrentWindowId(null);
            ticket.setCurrentWindowNo(null);
            baseMapper.updateById(ticket);

            String waitingKey = redisKeyBuilder.waitingKey(ticket.getBranchId(), ticket.getServiceType());
            stringRedisTemplate.opsForZSet().add(waitingKey, String.valueOf(ticket.getId()), score);

            Long waitingCount = stringRedisTemplate.opsForZSet().zCard(waitingKey);
            queuePushService.pushStatusMessage(ticket.getBranchId(), "QUEUE_REQUEUE", ticket.getTicketNo(), null);

            log.info("号码重新入队成功，网点ID：{}，号码：{}，过号次数：{}", ticket.getBranchId(), ticket.getTicketNo(), missedCount);
            return buildTicketVO(ticket, waitingCount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("重新入队操作被中断", e);
        } finally {
            unlock(lock, locked);
        }
    }

    /**
     * 查询当前队列状态
     *
     * @param branchId 网点ID
     * @param serviceType 业务类型
     * @return 队列状态
     */
    @Override
    public QueueStatusVO currentStatus(Long branchId, String serviceType) {
        String waitingKey = redisKeyBuilder.waitingKey(branchId, serviceType);
        Long waitingCount = stringRedisTemplate.opsForZSet().zCard(waitingKey);

        List<ServiceWindow> windows = serviceWindowMapper.selectList(
                new LambdaQueryWrapper<ServiceWindow>()
                        .eq(ServiceWindow::getBranchId, branchId)
                        .eq(ServiceWindow::getServiceType, serviceType)
                        .isNotNull(ServiceWindow::getCurrentTicketId)
        );

        List<CurrentCallVO> currentCalls = windows.stream().map(window -> {
            CurrentCallVO vo = new CurrentCallVO();
            vo.setWindowId(window.getId());
            vo.setWindowNo(window.getWindowNo());
            vo.setTicketId(window.getCurrentTicketId());
            vo.setTicketNo(window.getCurrentTicketNo());
            vo.setWindowStatus(window.getStatus());
            return vo;
        }).toList();

        QueueStatusVO vo = new QueueStatusVO();
        vo.setBranchId(branchId);
        vo.setServiceType(serviceType);
        vo.setWaitingCount(ObjectUtil.defaultIfNull(waitingCount, 0L));
        vo.setCurrentCalls(currentCalls);
        return vo;
    }

    /**
     * 从等待队列中取出有效号码
     *
     * @param waitingKey 等待队列Key
     * @return 排队号码
     */
    private QueueTicket pollWaitingTicket(String waitingKey) {
        while (true) {
            Set<String> values = stringRedisTemplate.opsForZSet().range(waitingKey, 0, 0);
            if (CollUtil.isEmpty(values)) {
                return null;
            }

            String ticketIdText = CollUtil.getFirst(values);
            stringRedisTemplate.opsForZSet().remove(waitingKey, ticketIdText);

            QueueTicket ticket = baseMapper.selectById(Convert.toLong(ticketIdText));
            if (ticket != null && StrUtil.equals(TicketStatusEnum.WAITING.getCode(), ticket.getStatus())) {
                return ticket;
            }

            log.warn("等待队列中存在无效号码，已跳过，ticketId：{}", ticketIdText);
        }
    }

    /**
     * 获取并校验窗口
     *
     * @param branchId 网点ID
     * @param windowId 窗口ID
     * @return 服务窗口
     */
    private ServiceWindow getAndCheckWindow(Long branchId, Long windowId) {
        ServiceWindow window = serviceWindowMapper.selectById(windowId);
        if (window == null) {
            throw new IllegalArgumentException("窗口不存在");
        }
        if (!branchId.equals(window.getBranchId())) {
            throw new IllegalArgumentException("窗口不属于当前网点");
        }
        if (!Integer.valueOf(1).equals(window.getEnabled())) {
            throw new IllegalStateException("窗口未启用");
        }
        if (StrUtil.equals(WindowStatusEnum.CLOSED.getCode(), window.getStatus())) {
            throw new IllegalStateException("窗口已关闭");
        }
        return window;
    }

    /**
     * 查询最近一次叫号记录
     *
     * @param ticketId 号码ID
     * @param windowId 窗口ID
     * @return 叫号记录
     */
    private QueueCallRecord getLatestCallRecord(Long ticketId, Long windowId) {
        return callRecordMapper.selectOne(
                new LambdaQueryWrapper<QueueCallRecord>()
                        .eq(QueueCallRecord::getTicketId, ticketId)
                        .eq(QueueCallRecord::getWindowId, windowId)
                        .orderByDesc(QueueCallRecord::getId)
                        .last("LIMIT 1")
        );
    }

    /**
     * 释放窗口
     *
     * @param branchId 网点ID
     * @param window 窗口
     */
    private void releaseWindow(Long branchId, ServiceWindow window) {
        window.setStatus(WindowStatusEnum.IDLE.getCode());
        window.setCurrentTicketId(null);
        window.setCurrentTicketNo(null);
        serviceWindowMapper.updateById(window);

        stringRedisTemplate.delete(redisKeyBuilder.currentKey(branchId, window.getId()));
        stringRedisTemplate.delete(redisKeyBuilder.windowBusyKey(branchId, window.getId()));
    }

    /**
     * 生成排队号码
     *
     * @param serviceType 业务类型
     * @param seq 当日序列
     * @return 排队号码
     */
    private String buildTicketNo(String serviceType, Long seq) {
        String prefix = ServiceTypeEnum.getPrefix(serviceType);
        return prefix + StrUtil.padPre(String.valueOf(seq), 3, '0');
    }

    /**
     * 构建取号响应
     *
     * @param ticket 排队号码
     * @param waitingCount 等待人数
     * @return 取号响应
     */
    private QueueTicketVO buildTicketVO(QueueTicket ticket, Long waitingCount) {
        QueueTicketVO vo = new QueueTicketVO();
        vo.setTicketId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setServiceType(ticket.getServiceType());
        vo.setStatus(ticket.getStatus());
        vo.setWaitingCount(ObjectUtil.defaultIfNull(waitingCount, 0L));
        return vo;
    }

    /**
     * 构建叫号响应
     *
     * @param ticket 排队号码
     * @param window 服务窗口
     * @param callTime 叫号时间
     * @return 叫号响应
     */
    private CallNextVO buildCallNextVO(QueueTicket ticket, ServiceWindow window, LocalDateTime callTime) {
        CallNextVO vo = new CallNextVO();
        vo.setTicketId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setWindowId(window.getId());
        vo.setWindowNo(window.getWindowNo());
        vo.setServiceType(ticket.getServiceType());
        vo.setStatus(ticket.getStatus());
        vo.setCallTime(callTime);
        return vo;
    }

    /**
     * 计算两个时间之间的秒数
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 秒数
     */
    private Long calcSeconds(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return Math.max(Duration.between(startTime, endTime).getSeconds(), 0L);
    }

    /**
     * 释放分布式锁
     *
     * @param lock 锁对象
     * @param locked 是否已加锁
     */
    private void unlock(RLock lock, boolean locked) {
        if (locked && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

### 窗口叫号实现

窗口叫号的核心逻辑在 `callNext` 方法中，关键控制点如下：

```text
1. 使用窗口维度分布式锁，避免同一个窗口并发叫号
2. 校验窗口必须是 IDLE 状态
3. 从 Redis ZSet 中取出 score 最小的号码
4. 更新号码状态为 CALLING
5. 更新窗口状态为 CALLING
6. 写入叫号记录
7. 写入当前叫号缓存和窗口占用缓存
8. 通过 WebSocket 推送叫号消息
```

接口层通过 Controller 暴露给窗口端使用。

文件位置：`src/main/java/io/github/atengk/queue/controller/QueueTicketController.java`

```java
package io.github.atengk.queue.controller;

import io.github.atengk.queue.common.Result;
import io.github.atengk.queue.dto.CallNextDTO;
import io.github.atengk.queue.dto.RequeueDTO;
import io.github.atengk.queue.dto.TakeNumberDTO;
import io.github.atengk.queue.dto.WindowActionDTO;
import io.github.atengk.queue.service.QueueTicketService;
import io.github.atengk.queue.vo.CallNextVO;
import io.github.atengk.queue.vo.QueueStatusVO;
import io.github.atengk.queue.vo.QueueTicketVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 排队叫号接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/queue")
public class QueueTicketController {

    private final QueueTicketService queueTicketService;

    /**
     * 用户取号
     *
     * @param dto 取号参数
     * @return 取号结果
     */
    @PostMapping("/tickets/take")
    public Result<QueueTicketVO> takeNumber(@Valid @RequestBody TakeNumberDTO dto) {
        return Result.ok(queueTicketService.takeNumber(dto));
    }

    /**
     * 窗口叫号
     *
     * @param dto 叫号参数
     * @return 叫号结果
     */
    @PostMapping("/windows/call-next")
    public Result<CallNextVO> callNext(@Valid @RequestBody CallNextDTO dto) {
        return Result.ok(queueTicketService.callNext(dto));
    }

    /**
     * 开始办理
     *
     * @param dto 窗口操作参数
     * @return 操作结果
     */
    @PostMapping("/windows/start-service")
    public Result<Void> startService(@Valid @RequestBody WindowActionDTO dto) {
        queueTicketService.startService(dto);
        return Result.ok();
    }

    /**
     * 办理完成
     *
     * @param dto 窗口操作参数
     * @return 操作结果
     */
    @PostMapping("/windows/finish")
    public Result<Void> finishService(@Valid @RequestBody WindowActionDTO dto) {
        queueTicketService.finishService(dto);
        return Result.ok();
    }

    /**
     * 标记过号
     *
     * @param dto 窗口操作参数
     * @return 操作结果
     */
    @PostMapping("/windows/miss")
    public Result<Void> markMissed(@Valid @RequestBody WindowActionDTO dto) {
        queueTicketService.markMissed(dto);
        return Result.ok();
    }

    /**
     * 重新入队
     *
     * @param dto 重新入队参数
     * @return 重新入队结果
     */
    @PostMapping("/tickets/requeue")
    public Result<QueueTicketVO> requeue(@Valid @RequestBody RequeueDTO dto) {
        return Result.ok(queueTicketService.requeue(dto));
    }

    /**
     * 查询当前叫号状态
     *
     * @param branchId 网点ID
     * @param serviceType 业务类型
     * @return 队列状态
     */
    @GetMapping("/status")
    public Result<QueueStatusVO> currentStatus(@NotNull Long branchId, @NotBlank String serviceType) {
        return Result.ok(queueTicketService.currentStatus(branchId, serviceType));
    }
}
```

窗口叫号请求示例：

```bash
curl -X POST 'http://localhost:8080/api/queue/windows/call-next' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "windowId": 1,
    "operatorId": 90001
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "ticketId": 1,
    "ticketNo": "A001",
    "windowId": 1,
    "windowNo": "1",
    "serviceType": "GENERAL",
    "status": "CALLING",
    "callTime": "2026-05-15T09:30:00"
  }
}
```

### 办理完成实现

办理完成用于关闭当前号码的生命周期，并释放窗口。实际业务中通常会有“开始办理”和“办理完成”两个动作，本案例也保留了这两个接口。

开始办理：

```bash
curl -X POST 'http://localhost:8080/api/queue/windows/start-service' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "windowId": 1,
    "operatorId": 90001
  }'
```

办理完成：

```bash
curl -X POST 'http://localhost:8080/api/queue/windows/finish' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "windowId": 1,
    "operatorId": 90001
  }'
```

办理完成后会执行：

```text
1. 号码状态更新为 FINISHED
2. 记录 finish_time
3. 更新叫号记录为 FINISHED
4. 计算 service_seconds
5. 窗口状态恢复为 IDLE
6. 删除当前叫号缓存
7. 删除窗口占用缓存
8. 推送 QUEUE_FINISHED 消息
```

### 过号处理实现

过号处理用于窗口叫号后用户未到场的场景。只有 `CALLING` 状态的号码允许标记过号。

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/queue/windows/miss' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "windowId": 1,
    "operatorId": 90001
  }'
```

过号后会执行：

```text
1. 号码状态更新为 MISSED
2. missed_count 加 1
3. 叫号记录更新为 MISSED
4. 窗口恢复为 IDLE
5. 删除当前叫号缓存
6. 删除窗口占用缓存
7. 推送 QUEUE_MISSED 消息
```

这里不自动重新入队，因为实际线下业务通常需要用户主动回到取号机、App 或窗口端点击“重新排队”。

### 重新入队实现

重新入队用于过号用户重新进入等待队列。只有 `MISSED` 状态允许重新入队。

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/queue/tickets/requeue' \
  -H 'Content-Type: application/json' \
  -d '{
    "ticketId": 1
  }'
```

重新入队排序规则：

```text
score = 当前时间戳 + 过号次数 * 60秒
```

这样设计可以避免反复过号用户重新入队后仍然插到过前位置，影响正常排队用户。

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "ticketId": 1,
    "ticketNo": "A001",
    "serviceType": "GENERAL",
    "status": "WAITING",
    "waitingCount": 6
  }
}
```

### 当前叫号查询实现

当前叫号查询用于大屏初始化、窗口刷新、用户端查询等待人数等场景。它不直接扫描所有排队号码，而是组合 Redis 等待队列数量和窗口表当前处理状态。

请求示例：

```bash
curl 'http://localhost:8080/api/queue/status?branchId=1001&serviceType=GENERAL'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "branchId": 1001,
    "serviceType": "GENERAL",
    "waitingCount": 5,
    "currentCalls": [
      {
        "windowId": 1,
        "windowNo": "1",
        "ticketId": 1,
        "ticketNo": "A001",
        "windowStatus": "CALLING"
      },
      {
        "windowId": 2,
        "windowNo": "2",
        "ticketId": 2,
        "ticketNo": "A002",
        "windowStatus": "SERVING"
      }
    ]
  }
}
```

## 实时推送实现

实时推送用于将叫号结果、过号、办理中、办理完成等事件推送给大厅大屏或客户端。这里使用 Spring Boot WebSocket 实现，不依赖 MQ，适合单体项目和中小型网点场景。

如果后续部署多节点，需要引入 Redis Pub/Sub、RabbitMQ 或 Kafka 做跨节点广播。

### WebSocket 连接管理

WebSocket 连接管理在前文已经给出 `QueueWebSocketHandler` 和 `QueueWebSocketSessionManager`。这里的核心规则是：

```text
1. 大屏连接 ws://localhost:8080/ws/queue?branchId=1001
2. 后端按 branchId 保存连接
3. 某个网点发生叫号事件时，只推送给该网点的大屏
4. 断开连接后从内存连接池移除
```

前端连接示例：

```javascript
const socket = new WebSocket('ws://localhost:8080/ws/queue?branchId=1001')

socket.onmessage = (event) => {
  const message = JSON.parse(event.data)
  console.log('收到叫号消息：', message)
}

socket.onopen = () => {
  console.log('排队叫号 WebSocket 已连接')
}

socket.onclose = () => {
  console.log('排队叫号 WebSocket 已断开')
}
```

### 叫号消息推送

下面的推送服务用于统一封装 WebSocket 消息发送。叫号、过号、办理完成、大屏刷新都通过这个服务推送。

文件位置：`src/main/java/io/github/atengk/queue/service/QueuePushService.java`

```java
package io.github.atengk.queue.service;

import io.github.atengk.queue.vo.CallNextVO;

/**
 * 排队叫号推送服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface QueuePushService {

    /**
     * 推送叫号消息
     *
     * @param branchId 网点ID
     * @param vo 叫号数据
     */
    void pushCallMessage(Long branchId, CallNextVO vo);

    /**
     * 推送状态消息
     *
     * @param branchId 网点ID
     * @param eventType 事件类型
     * @param ticketNo 排队号码
     * @param windowNo 窗口编号
     */
    void pushStatusMessage(Long branchId, String eventType, String ticketNo, String windowNo);
}
```

下面是推送服务实现，使用 Hutool `JSONUtil` 统一序列化消息。

文件位置：`src/main/java/io/github/atengk/queue/service/impl/QueuePushServiceImpl.java`

```java
package io.github.atengk.queue.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.queue.service.QueuePushService;
import io.github.atengk.queue.vo.CallNextVO;
import io.github.atengk.queue.websocket.QueueWebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 排队叫号推送服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueuePushServiceImpl implements QueuePushService {

    private final QueueWebSocketSessionManager sessionManager;

    /**
     * 推送叫号消息
     *
     * @param branchId 网点ID
     * @param vo 叫号数据
     */
    @Override
    public void pushCallMessage(Long branchId, CallNextVO vo) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "QUEUE_CALL");
        message.put("time", DateUtil.formatDateTime(new Date()));
        message.put("data", vo);
        doPush(branchId, message);
    }

    /**
     * 推送状态消息
     *
     * @param branchId 网点ID
     * @param eventType 事件类型
     * @param ticketNo 排队号码
     * @param windowNo 窗口编号
     */
    @Override
    public void pushStatusMessage(Long branchId, String eventType, String ticketNo, String windowNo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ticketNo", ticketNo);
        data.put("windowNo", windowNo);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", eventType);
        message.put("time", DateUtil.formatDateTime(new Date()));
        message.put("data", data);

        doPush(branchId, message);
    }

    /**
     * 执行推送
     *
     * @param branchId 网点ID
     * @param message 消息体
     */
    private void doPush(Long branchId, Map<String, Object> message) {
        Set<WebSocketSession> sessions = sessionManager.getSessions(branchId);
        if (CollUtil.isEmpty(sessions)) {
            log.info("当前网点暂无WebSocket连接，跳过推送，网点ID：{}，消息类型：{}", branchId, message.get("type"));
            return;
        }

        String payload = JSONUtil.toJsonStr(message);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }

            try {
                session.sendMessage(new TextMessage(payload));
            } catch (Exception e) {
                log.warn("排队叫号WebSocket推送失败，网点ID：{}，连接ID：{}", branchId, session.getId(), e);
            }
        }

        log.info("排队叫号WebSocket推送完成，网点ID：{}，连接数：{}，消息类型：{}", branchId, sessions.size(), message.get("type"));
    }
}
```

叫号消息示例：

```json
{
  "type": "QUEUE_CALL",
  "time": "2026-05-15 09:30:00",
  "data": {
    "ticketId": 1,
    "ticketNo": "A001",
    "windowId": 1,
    "windowNo": "1",
    "serviceType": "GENERAL",
    "status": "CALLING",
    "callTime": "2026-05-15T09:30:00"
  }
}
```

过号消息示例：

```json
{
  "type": "QUEUE_MISSED",
  "time": "2026-05-15 09:35:00",
  "data": {
    "ticketNo": "A001",
    "windowNo": "1"
  }
}
```

办理完成消息示例：

```json
{
  "type": "QUEUE_FINISHED",
  "time": "2026-05-15 09:40:00",
  "data": {
    "ticketNo": "A001",
    "windowNo": "1"
  }
}
```

### 大屏叫号数据推送

大屏通常需要两类数据：

```text
1. 实时事件：谁被叫到哪个窗口
2. 当前状态：当前办理中号码、等待人数、窗口状态
```

实时事件通过 WebSocket 推送，当前状态通过 HTTP 接口查询：

```bash
curl 'http://localhost:8080/api/queue/status?branchId=1001&serviceType=GENERAL'
```

大屏推荐处理逻辑：

```text
页面初始化
-> 调用 /api/queue/status 查询当前状态
-> 建立 WebSocket 连接
-> 收到 QUEUE_CALL 后播放叫号并刷新当前叫号区
-> 收到 QUEUE_MISSED 后从当前叫号区移除或标记过号
-> 收到 QUEUE_FINISHED 后刷新窗口状态
```

前端大屏伪代码：

```javascript
const branchId = 1001
const serviceType = 'GENERAL'

async function loadQueueStatus() {
  const response = await fetch(`/api/queue/status?branchId=${branchId}&serviceType=${serviceType}`)
  const result = await response.json()
  renderQueueStatus(result.data)
}

function connectQueueSocket() {
  const socket = new WebSocket(`ws://localhost:8080/ws/queue?branchId=${branchId}`)

  socket.onmessage = async (event) => {
    const message = JSON.parse(event.data)

    if (message.type === 'QUEUE_CALL') {
      speak(`请 ${message.data.ticketNo} 到 ${message.data.windowNo} 号窗口办理`)
    }

    await loadQueueStatus()
  }
}

function speak(text) {
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'zh-CN'
  window.speechSynthesis.speak(utterance)
}

loadQueueStatus()
connectQueueSocket()
```

到这里，核心链路已经闭环：

```text
用户取号
-> Redis ZSet 入队
-> 窗口叫号
-> MySQL 状态更新
-> Redis 当前叫号缓存
-> WebSocket 推送
-> 开始办理
-> 办理完成或过号
-> 释放窗口
-> 过号号码可重新入队
```

## 并发控制与幂等处理

排队叫号的并发问题主要集中在取号、窗口叫号和过号处理。原场景中明确提到并发取号、窗口分配、过号重排、实时推送等核心难点，本节重点给出可直接落地的控制方式。

本案例采用三层控制：

```text
1. Redis / Redisson 分布式锁：控制同一业务动作的并发入口
2. MySQL 状态判断：控制业务状态只能按规则流转
3. Redis 缓存标记：控制窗口当前是否已经占用
```

推荐控制关系如下：

| 场景     | 锁粒度                 | 业务校验                           | Redis 标记                     |
| -------- | ---------------------- | ---------------------------------- | ------------------------------ |
| 并发取号 | branchId + serviceType | 号码唯一索引                       | 号码序列 Key                   |
| 窗口叫号 | branchId + windowId    | 窗口必须 IDLE，号码必须 WAITING    | windowBusyKey                  |
| 开始办理 | branchId + windowId    | 窗口必须 CALLING，号码必须 CALLING | currentKey                     |
| 办理完成 | branchId + windowId    | 号码必须 CALLING 或 SERVING        | 删除 currentKey、windowBusyKey |
| 标记过号 | branchId + windowId    | 窗口必须 CALLING，号码必须 CALLING | 删除 currentKey、windowBusyKey |
| 重新入队 | ticketId               | 号码必须 MISSED                    | 重新写入等待队列               |

### 并发取号控制

并发取号的目标是保证同一网点、同一业务类型下号码不重复，并且 Redis 等待队列和 MySQL 号码记录一致。

核心控制点：

```text
1. 使用 queue:lock:take:{branchId}:{serviceType} 加锁
2. 使用 Redis INCR 生成当日递增序号
3. MySQL 通过 uk_branch_ticket_no 保底防重复
4. MySQL 插入成功后再写入 Redis ZSet
```

前文 `takeNumber` 方法中的关键代码如下：

```java
RLock lock = redissonClient.getLock(redisKeyBuilder.takeLockKey(dto.getBranchId(), dto.getServiceType()));
boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
if (!locked) {
    throw new IllegalStateException("当前取号人数较多，请稍后重试");
}

String seqKey = redisKeyBuilder.seqKey(dto.getBranchId(), dto.getServiceType());
Long seq = stringRedisTemplate.opsForValue().increment(seqKey);
stringRedisTemplate.expire(seqKey, 2, TimeUnit.DAYS);

baseMapper.insert(ticket);

String waitingKey = redisKeyBuilder.waitingKey(dto.getBranchId(), dto.getServiceType());
stringRedisTemplate.opsForZSet().add(waitingKey, String.valueOf(ticket.getId()), queueScore);
```

这里需要注意一个边界：如果 MySQL 插入成功，但 Redis 写入失败，会出现“数据库中是等待中，但 Redis 队列没有该号码”的问题。生产项目可以使用补偿任务修复，本案例可以加一个轻量补偿方法。

下面的任务用于扫描 MySQL 中 `WAITING` 状态但不在 Redis 队列中的号码，并重新写入 Redis。

文件位置：`src/main/java/io/github/atengk/queue/job/QueueRepairJob.java`

```java
package io.github.atengk.queue.job;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.queue.config.QueueRedisKeyBuilder;
import io.github.atengk.queue.entity.QueueTicket;
import io.github.atengk.queue.enums.TicketStatusEnum;
import io.github.atengk.queue.mapper.QueueTicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 排队队列修复任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueRepairJob {

    private final QueueTicketMapper queueTicketMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final QueueRedisKeyBuilder redisKeyBuilder;

    /**
     * 修复等待中但未进入 Redis 队列的号码
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void repairWaitingQueue() {
        List<QueueTicket> tickets = queueTicketMapper.selectList(
                new LambdaQueryWrapper<QueueTicket>()
                        .eq(QueueTicket::getStatus, TicketStatusEnum.WAITING.getCode())
                        .orderByAsc(QueueTicket::getTakeTime)
                        .last("LIMIT 500")
        );

        if (CollUtil.isEmpty(tickets)) {
            return;
        }

        int repairCount = 0;
        for (QueueTicket ticket : tickets) {
            String waitingKey = redisKeyBuilder.waitingKey(ticket.getBranchId(), ticket.getServiceType());
            String ticketId = String.valueOf(ticket.getId());
            Double score = stringRedisTemplate.opsForZSet().score(waitingKey, ticketId);
            if (score == null) {
                stringRedisTemplate.opsForZSet().add(waitingKey, ticketId, ticket.getQueueScore());
                repairCount++;
            }
        }

        if (repairCount > 0) {
            log.info("排队等待队列修复完成，修复数量：{}", repairCount);
        }
    }
}
```

如果使用 `@Scheduled`，需要在启动类开启定时任务。

文件位置：`src/main/java/io/github/atengk/queue/QueueCallApplication.java`

```java
package io.github.atengk.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 排队叫号示例应用
 *
 * @author Ateng
 * @since 2026-05-15
 */
@EnableScheduling
@SpringBootApplication
public class QueueCallApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(QueueCallApplication.class, args);
    }
}
```

### 窗口重复叫号控制

窗口重复叫号的典型问题是：工作人员连续点击叫号按钮，或者前端重复提交，导致一个窗口同时叫出多个号码。

本案例通过三层方式避免：

```text
1. Redisson 窗口锁：queue:lock:call:{branchId}:{windowId}
2. 窗口状态校验：窗口必须是 IDLE
3. Redis 窗口占用缓存：queue:window:busy:{branchId}:{windowId}
```

前文 `callNext` 方法中的关键代码如下：

```java
ServiceWindow window = getAndCheckWindow(dto.getBranchId(), dto.getWindowId());
if (!StrUtil.equals(WindowStatusEnum.IDLE.getCode(), window.getStatus())) {
    throw new IllegalStateException("窗口不是空闲状态，不能继续叫号");
}

String busyKey = redisKeyBuilder.windowBusyKey(dto.getBranchId(), dto.getWindowId());
if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(busyKey))) {
    throw new IllegalStateException("窗口已有正在处理的号码");
}
```

如果想进一步增强数据库层面的防重，可以将窗口状态更新改成条件更新。下面是一个更稳妥的 Mapper 方法写法。

文件位置：`src/main/java/io/github/atengk/queue/mapper/ServiceWindowMapper.java`

```java
package io.github.atengk.queue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.queue.entity.ServiceWindow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 服务窗口 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ServiceWindowMapper extends BaseMapper<ServiceWindow> {

    /**
     * 从空闲状态占用窗口
     *
     * @param windowId 窗口ID
     * @param ticketId 号码ID
     * @param ticketNo 号码
     * @param operatorId 操作员ID
     * @return 影响行数
     */
    @Update("""
            UPDATE service_window
            SET status = 'CALLING',
                current_ticket_id = #{ticketId},
                current_ticket_no = #{ticketNo},
                operator_id = #{operatorId},
                last_call_time = NOW(),
                update_time = NOW()
            WHERE id = #{windowId}
              AND status = 'IDLE'
              AND enabled = 1
              AND deleted = 0
            """)
    int occupyIdleWindow(@Param("windowId") Long windowId,
                         @Param("ticketId") Long ticketId,
                         @Param("ticketNo") String ticketNo,
                         @Param("operatorId") Long operatorId);
}
```

在 `callNext` 中可以替换原来的 `serviceWindowMapper.updateById(window)`：

```java
int updated = serviceWindowMapper.occupyIdleWindow(
        window.getId(),
        ticket.getId(),
        ticket.getTicketNo(),
        dto.getOperatorId()
);

if (updated <= 0) {
    throw new IllegalStateException("窗口已被占用，请刷新后重试");
}
```

这能防止极端情况下 Redis 锁失效或代码误用导致的窗口状态覆盖。

### 过号重复处理控制

过号重复处理通常来自两种情况：

```text
1. 操作员多次点击过号按钮
2. 前端请求超时后自动重试
```

过号必须满足：

```text
窗口状态 = CALLING
号码状态 = CALLING
```

如果号码已经是 `MISSED`、`SERVING` 或 `FINISHED`，不允许再次过号。

前文 `markMissed` 已经有状态判断：

```java
if (!StrUtil.equals(WindowStatusEnum.CALLING.getCode(), window.getStatus())) {
    throw new IllegalStateException("只有叫号中状态可以标记过号");
}

QueueTicket ticket = baseMapper.selectById(window.getCurrentTicketId());
if (ticket == null || !StrUtil.equals(TicketStatusEnum.CALLING.getCode(), ticket.getStatus())) {
    throw new IllegalStateException("当前号码状态异常，不能过号");
}
```

如果想让重复过号请求“幂等返回成功”，可以改成下面的策略：

```text
1. 当前窗口已经 IDLE，且 currentTicketId 为空：认为重复请求已经处理完成，直接返回成功
2. 当前号码已经 MISSED：认为重复请求已经处理完成，直接返回成功
3. 当前号码是 SERVING 或 FINISHED：拒绝过号
```

示例代码片段：

```java
if (StrUtil.equals(WindowStatusEnum.IDLE.getCode(), window.getStatus())
        && window.getCurrentTicketId() == null) {
    log.info("重复过号请求已忽略，窗口已空闲，窗口ID：{}", window.getId());
    return;
}

QueueTicket ticket = baseMapper.selectById(window.getCurrentTicketId());
if (ticket != null && StrUtil.equals(TicketStatusEnum.MISSED.getCode(), ticket.getStatus())) {
    log.info("重复过号请求已忽略，号码已过号，号码：{}", ticket.getTicketNo());
    return;
}
```

建议在实战项目中使用“状态机 + 条件更新”做最终兜底。例如：

```sql
UPDATE queue_ticket
SET status = 'MISSED',
    missed_count = missed_count + 1,
    update_time = NOW()
WHERE id = #{ticketId}
  AND status = 'CALLING'
  AND deleted = 0;
```

只有影响行数为 1，才代表本次请求真正完成了过号动作。

## 定时任务与统计

定时任务主要处理两类问题：

```text
1. 叫号后长时间无人到达，自动标记过号
2. 定期修复 Redis 队列和 MySQL 状态不一致
```

统计主要处理：

```text
1. 当前等待人数
2. 平均等待时间
3. 平均办理时长
4. 窗口办理数量
```

本案例先实现过期号码自动处理、等待人数统计和平均等待时间统计。

### 过期号码自动处理

过期号码自动处理用于解决窗口叫号后用户长时间不到窗口的问题。例如叫号超过 3 分钟仍未开始办理，则自动标记过号并释放窗口。

先在配置中增加超时时间。

文件位置：`src/main/resources/application.yml`

```yaml
queue:
  timeout:
    # 叫号后超过 180 秒未开始办理，自动过号
    calling-timeout-seconds: 180
```

下面增加配置属性类。

文件位置：`src/main/java/io/github/atengk/queue/config/QueueTimeoutProperties.java`

```java
package io.github.atengk.queue.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 排队叫号超时配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "queue.timeout")
public class QueueTimeoutProperties {

    /**
     * 叫号超时秒数
     */
    private Long callingTimeoutSeconds;
}
```

下面的定时任务每分钟扫描一次超时叫号数据，并自动标记过号。

文件位置：`src/main/java/io/github/atengk/queue/job/QueueTimeoutJob.java`

```java
package io.github.atengk.queue.job;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.queue.config.QueueRedisKeyBuilder;
import io.github.atengk.queue.config.QueueTimeoutProperties;
import io.github.atengk.queue.entity.QueueCallRecord;
import io.github.atengk.queue.entity.QueueTicket;
import io.github.atengk.queue.entity.ServiceWindow;
import io.github.atengk.queue.enums.CallStatusEnum;
import io.github.atengk.queue.enums.TicketStatusEnum;
import io.github.atengk.queue.enums.WindowStatusEnum;
import io.github.atengk.queue.mapper.QueueCallRecordMapper;
import io.github.atengk.queue.mapper.QueueTicketMapper;
import io.github.atengk.queue.mapper.ServiceWindowMapper;
import io.github.atengk.queue.service.QueuePushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 排队叫号超时处理任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueTimeoutJob {

    private final QueueTicketMapper queueTicketMapper;
    private final ServiceWindowMapper serviceWindowMapper;
    private final QueueCallRecordMapper callRecordMapper;
    private final QueueTimeoutProperties timeoutProperties;
    private final QueueRedisKeyBuilder redisKeyBuilder;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final QueuePushService queuePushService;

    /**
     * 自动处理叫号超时号码
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void autoMarkCallingTimeout() {
        LocalDateTime timeoutTime = LocalDateTime.now().minusSeconds(timeoutProperties.getCallingTimeoutSeconds());

        List<QueueTicket> tickets = queueTicketMapper.selectList(
                new LambdaQueryWrapper<QueueTicket>()
                        .eq(QueueTicket::getStatus, TicketStatusEnum.CALLING.getCode())
                        .le(QueueTicket::getCallTime, timeoutTime)
                        .last("LIMIT 100")
        );

        if (CollUtil.isEmpty(tickets)) {
            return;
        }

        int count = 0;
        for (QueueTicket ticket : tickets) {
            if (handleTimeoutTicket(ticket)) {
                count++;
            }
        }

        if (count > 0) {
            log.info("自动过号任务执行完成，处理数量：{}", count);
        }
    }

    /**
     * 处理单个超时号码
     *
     * @param ticket 排队号码
     * @return 是否处理成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handleTimeoutTicket(QueueTicket ticket) {
        RLock lock = redissonClient.getLock(redisKeyBuilder.callLockKey(ticket.getBranchId(), ticket.getCurrentWindowId()));
        boolean locked = false;

        try {
            locked = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!locked) {
                return false;
            }

            QueueTicket latestTicket = queueTicketMapper.selectById(ticket.getId());
            if (latestTicket == null || !TicketStatusEnum.CALLING.getCode().equals(latestTicket.getStatus())) {
                return false;
            }

            ServiceWindow window = serviceWindowMapper.selectById(latestTicket.getCurrentWindowId());
            if (window == null || !WindowStatusEnum.CALLING.getCode().equals(window.getStatus())) {
                return false;
            }

            latestTicket.setStatus(TicketStatusEnum.MISSED.getCode());
            latestTicket.setMissedCount(latestTicket.getMissedCount() + 1);
            queueTicketMapper.updateById(latestTicket);

            QueueCallRecord record = callRecordMapper.selectOne(
                    new LambdaQueryWrapper<QueueCallRecord>()
                            .eq(QueueCallRecord::getTicketId, latestTicket.getId())
                            .eq(QueueCallRecord::getWindowId, window.getId())
                            .orderByDesc(QueueCallRecord::getId)
                            .last("LIMIT 1")
            );
            if (record != null) {
                record.setCallStatus(CallStatusEnum.MISSED.getCode());
                callRecordMapper.updateById(record);
            }

            window.setStatus(WindowStatusEnum.IDLE.getCode());
            window.setCurrentTicketId(null);
            window.setCurrentTicketNo(null);
            serviceWindowMapper.updateById(window);

            stringRedisTemplate.delete(redisKeyBuilder.currentKey(latestTicket.getBranchId(), window.getId()));
            stringRedisTemplate.delete(redisKeyBuilder.windowBusyKey(latestTicket.getBranchId(), window.getId()));

            queuePushService.pushStatusMessage(
                    latestTicket.getBranchId(),
                    "QUEUE_TIMEOUT_MISSED",
                    latestTicket.getTicketNo(),
                    window.getWindowNo()
            );

            log.info("叫号超时自动过号成功，网点ID：{}，号码：{}，窗口：{}",
                    latestTicket.getBranchId(), latestTicket.getTicketNo(), window.getWindowNo());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("自动过号任务被中断，号码ID：{}", ticket.getId(), e);
            return false;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

生产项目如果使用 XXL-JOB，可以将 `autoMarkCallingTimeout` 改造成 XXL-JOB 任务方法；核心逻辑不变。

### 等待人数统计

等待人数统计直接读取 Redis ZSet 的数量即可，性能比查 MySQL 更好。

核心代码在前文 `currentStatus` 方法中已经实现：

```java
String waitingKey = redisKeyBuilder.waitingKey(branchId, serviceType);
Long waitingCount = stringRedisTemplate.opsForZSet().zCard(waitingKey);
```

如果要单独提供统计接口，可以增加一个统计 VO。

文件位置：`src/main/java/io/github/atengk/queue/vo/QueueStatsVO.java`

```java
package io.github.atengk.queue.vo;

import lombok.Data;

/**
 * 排队统计响应对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class QueueStatsVO {

    private Long branchId;

    private String serviceType;

    private Long waitingCount;

    private Long todayFinishedCount;

    private Long avgWaitSeconds;

    private Long avgServiceSeconds;
}
```

下面是统计 Service 接口。

文件位置：`src/main/java/io/github/atengk/queue/service/QueueStatsService.java`

```java
package io.github.atengk.queue.service;

import io.github.atengk.queue.vo.QueueStatsVO;

/**
 * 排队统计服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface QueueStatsService {

    /**
     * 查询队列统计
     *
     * @param branchId 网点ID
     * @param serviceType 业务类型
     * @return 统计结果
     */
    QueueStatsVO getStats(Long branchId, String serviceType);
}
```

下面是统计 Service 实现。

文件位置：`src/main/java/io/github/atengk/queue/service/impl/QueueStatsServiceImpl.java`

```java
package io.github.atengk.queue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.queue.config.QueueRedisKeyBuilder;
import io.github.atengk.queue.entity.QueueCallRecord;
import io.github.atengk.queue.entity.QueueTicket;
import io.github.atengk.queue.enums.CallStatusEnum;
import io.github.atengk.queue.mapper.QueueCallRecordMapper;
import io.github.atengk.queue.mapper.QueueTicketMapper;
import io.github.atengk.queue.service.QueueStatsService;
import io.github.atengk.queue.vo.QueueStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 排队统计服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueStatsServiceImpl implements QueueStatsService {

    private final StringRedisTemplate stringRedisTemplate;
    private final QueueRedisKeyBuilder redisKeyBuilder;
    private final QueueTicketMapper queueTicketMapper;
    private final QueueCallRecordMapper callRecordMapper;

    /**
     * 查询队列统计
     *
     * @param branchId 网点ID
     * @param serviceType 业务类型
     * @return 统计结果
     */
    @Override
    public QueueStatsVO getStats(Long branchId, String serviceType) {
        String waitingKey = redisKeyBuilder.waitingKey(branchId, serviceType);
        Long waitingCount = stringRedisTemplate.opsForZSet().zCard(waitingKey);

        LocalDateTime startTime = LocalDate.now().atStartOfDay();
        LocalDateTime endTime = startTime.plusDays(1);

        List<QueueTicket> todayTickets = queueTicketMapper.selectList(
                new LambdaQueryWrapper<QueueTicket>()
                        .eq(QueueTicket::getBranchId, branchId)
                        .eq(QueueTicket::getServiceType, serviceType)
                        .ge(QueueTicket::getTakeTime, startTime)
                        .lt(QueueTicket::getTakeTime, endTime)
        );

        List<QueueCallRecord> finishedRecords = callRecordMapper.selectList(
                new LambdaQueryWrapper<QueueCallRecord>()
                        .eq(QueueCallRecord::getBranchId, branchId)
                        .eq(QueueCallRecord::getCallStatus, CallStatusEnum.FINISHED.getCode())
                        .ge(QueueCallRecord::getCallTime, startTime)
                        .lt(QueueCallRecord::getCallTime, endTime)
        );

        long avgWaitSeconds = Math.round(
                finishedRecords.stream()
                        .mapToLong(QueueCallRecord::getWaitSeconds)
                        .average()
                        .orElse(0)
        );

        long avgServiceSeconds = Math.round(
                finishedRecords.stream()
                        .mapToLong(QueueCallRecord::getServiceSeconds)
                        .average()
                        .orElse(0)
        );

        QueueStatsVO vo = new QueueStatsVO();
        vo.setBranchId(branchId);
        vo.setServiceType(serviceType);
        vo.setWaitingCount(waitingCount == null ? 0L : waitingCount);
        vo.setTodayFinishedCount((long) finishedRecords.size());
        vo.setAvgWaitSeconds(avgWaitSeconds);
        vo.setAvgServiceSeconds(avgServiceSeconds);

        log.info("查询排队统计完成，网点ID：{}，业务类型：{}，今日取号数：{}",
                branchId, serviceType, todayTickets.size());
        return vo;
    }
}
```

上面为了代码直观，直接查询当天记录后在内存聚合。生产环境数据量较大时，建议改成 SQL 聚合或按小时写入统计表。

### 平均等待时间统计

平均等待时间来自 `queue_call_record.wait_seconds` 字段。

计算口径：

```text
wait_seconds = call_time - take_time
```

也就是用户从取号到被窗口叫到的等待时间。

推荐统计 SQL：

```sql
SELECT
    AVG(wait_seconds) AS avg_wait_seconds,
    AVG(service_seconds) AS avg_service_seconds,
    COUNT(1) AS finished_count
FROM queue_call_record
WHERE branch_id = 1001
  AND call_status = 'FINISHED'
  AND call_time >= CURDATE()
  AND call_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY);
```

增加统计 Controller。

文件位置：`src/main/java/io/github/atengk/queue/controller/QueueStatsController.java`

```java
package io.github.atengk.queue.controller;

import io.github.atengk.queue.common.Result;
import io.github.atengk.queue.service.QueueStatsService;
import io.github.atengk.queue.vo.QueueStatsVO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 排队统计接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/queue/stats")
public class QueueStatsController {

    private final QueueStatsService queueStatsService;

    /**
     * 查询排队统计
     *
     * @param branchId 网点ID
     * @param serviceType 业务类型
     * @return 统计结果
     */
    @GetMapping
    public Result<QueueStatsVO> getStats(@NotNull Long branchId, @NotBlank String serviceType) {
        return Result.ok(queueStatsService.getStats(branchId, serviceType));
    }
}
```

请求示例：

```bash
curl 'http://localhost:8080/api/queue/stats?branchId=1001&serviceType=GENERAL'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "branchId": 1001,
    "serviceType": "GENERAL",
    "waitingCount": 8,
    "todayFinishedCount": 35,
    "avgWaitSeconds": 420,
    "avgServiceSeconds": 300
  }
}
```

## 接口测试与验证

本节按完整业务链路进行验证。建议先启动 MySQL、Redis，再启动 Spring Boot 项目。

启动前准备：

```text
1. 创建 queue_demo 数据库
2. 执行前文 queue_ticket、service_window、queue_call_record 建表 SQL
3. 插入 service_window 初始化窗口数据
4. 启动 Redis
5. 启动 Spring Boot 应用
```

### 取号接口测试

普通业务取号：

```bash
curl -X POST 'http://localhost:8080/api/queue/tickets/take' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "serviceType": "GENERAL",
    "userId": 10001,
    "priority": 0
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "ticketId": 1,
    "ticketNo": "A001",
    "serviceType": "GENERAL",
    "status": "WAITING",
    "waitingCount": 1
  }
}
```

优先业务取号：

```bash
curl -X POST 'http://localhost:8080/api/queue/tickets/take' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "serviceType": "VIP",
    "userId": 10002,
    "priority": 10
  }'
```

检查 Redis 等待队列：

```bash
redis-cli ZRANGE queue:waiting:1001:GENERAL 0 -1 WITHSCORES
redis-cli ZRANGE queue:waiting:1001:VIP 0 -1 WITHSCORES
```

`ZRANGE` 用于查看 ZSet 中的排队号码 ID 和排序分值，分值越小越靠前。

### 叫号接口测试

先确保 `service_window` 表中存在空闲窗口：

```sql
SELECT id, branch_id, window_no, service_type, status, current_ticket_id
FROM service_window
WHERE branch_id = 1001;
```

窗口叫号：

```bash
curl -X POST 'http://localhost:8080/api/queue/windows/call-next' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "windowId": 1,
    "operatorId": 90001
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "ticketId": 1,
    "ticketNo": "A001",
    "windowId": 1,
    "windowNo": "1",
    "serviceType": "GENERAL",
    "status": "CALLING",
    "callTime": "2026-05-15T09:30:00"
  }
}
```

验证 MySQL 状态：

```sql
SELECT id, ticket_no, status, current_window_id, current_window_no, call_time
FROM queue_ticket
WHERE id = 1;

SELECT id, window_no, status, current_ticket_id, current_ticket_no
FROM service_window
WHERE id = 1;

SELECT id, ticket_no, window_no, call_status, wait_seconds
FROM queue_call_record
WHERE ticket_id = 1
ORDER BY id DESC;
```

验证 Redis 当前叫号缓存：

```bash
redis-cli GET queue:current:1001:1
redis-cli GET queue:window:busy:1001:1
```

### 过号与重新入队测试

标记过号：

```bash
curl -X POST 'http://localhost:8080/api/queue/windows/miss' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "windowId": 1,
    "operatorId": 90001
  }'
```

验证号码已过号：

```sql
SELECT id, ticket_no, status, missed_count
FROM queue_ticket
WHERE id = 1;
```

预期：

```text
status = MISSED
missed_count = 1
```

重新入队：

```bash
curl -X POST 'http://localhost:8080/api/queue/tickets/requeue' \
  -H 'Content-Type: application/json' \
  -d '{
    "ticketId": 1
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "ticketId": 1,
    "ticketNo": "A001",
    "serviceType": "GENERAL",
    "status": "WAITING",
    "waitingCount": 1
  }
}
```

验证 Redis 队列：

```bash
redis-cli ZRANGE queue:waiting:1001:GENERAL 0 -1 WITHSCORES
```

验证窗口已经释放：

```sql
SELECT id, window_no, status, current_ticket_id, current_ticket_no
FROM service_window
WHERE id = 1;
```

预期：

```text
status = IDLE
current_ticket_id = NULL
current_ticket_no = NULL
```

### WebSocket 推送测试

可以使用 `wscat` 测试 WebSocket 推送。

安装命令：

```bash
npm install -g wscat
```

连接大屏 WebSocket：

```bash
wscat -c 'ws://localhost:8080/ws/queue?branchId=1001'
```

保持该终端不关闭，然后另开一个终端执行取号和叫号：

```bash
curl -X POST 'http://localhost:8080/api/queue/tickets/take' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "serviceType": "GENERAL",
    "userId": 10003,
    "priority": 0
  }'

curl -X POST 'http://localhost:8080/api/queue/windows/call-next' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "windowId": 1,
    "operatorId": 90001
  }'
```

WebSocket 终端应该收到类似消息：

```json
{
  "type": "QUEUE_CALL",
  "time": "2026-05-15 09:30:00",
  "data": {
    "ticketId": 2,
    "ticketNo": "A002",
    "windowId": 1,
    "windowNo": "1",
    "serviceType": "GENERAL",
    "status": "CALLING",
    "callTime": "2026-05-15T09:30:00"
  }
}
```

继续测试办理完成：

```bash
curl -X POST 'http://localhost:8080/api/queue/windows/start-service' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "windowId": 1,
    "operatorId": 90001
  }'

curl -X POST 'http://localhost:8080/api/queue/windows/finish' \
  -H 'Content-Type: application/json' \
  -d '{
    "branchId": 1001,
    "windowId": 1,
    "operatorId": 90001
  }'
```

WebSocket 终端应该陆续收到：

```json
{
  "type": "QUEUE_SERVING",
  "time": "2026-05-15 09:31:00",
  "data": {
    "ticketNo": "A002",
    "windowNo": "1"
  }
}
{
  "type": "QUEUE_FINISHED",
  "time": "2026-05-15 09:35:00",
  "data": {
    "ticketNo": "A002",
    "windowNo": "1"
  }
}
```

最后查询队列状态和统计结果：

```bash
curl 'http://localhost:8080/api/queue/status?branchId=1001&serviceType=GENERAL'

curl 'http://localhost:8080/api/queue/stats?branchId=1001&serviceType=GENERAL'
```

完整验证通过后，说明以下核心能力已经闭环：

```text
1. 用户可以正常取号
2. 号码可以进入 Redis 等待队列
3. 窗口可以按顺序叫号
4. 重复叫号会被窗口状态和 Redis 标记拦截
5. 过号后可以重新入队
6. 办理完成后窗口会自动释放
7. 大屏可以通过 WebSocket 收到实时叫号消息
8. 系统可以统计等待人数和平均等待时间
```