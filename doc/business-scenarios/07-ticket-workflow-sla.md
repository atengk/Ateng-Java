# 工单流转与 SLA 超时处理

本案例围绕客服、售后、运维、ITSM、缺陷处理、投诉处理等常见工单场景展开，重点实现“工单状态流转、处理人变更、SLA 到期时间计算、超时提醒、超时升级、处理记录留痕”这几类核心能力。技术栈参考原 README 中“工单流转与 SLA”章节推荐的 Spring Boot、MyBatis-Plus、Redis、RabbitMQ、XXL-JOB、WebSocket、MinIO、Sa-Token、Elasticsearch 等方案。

## 功能目标与业务边界

本案例不做完整 ITSM 平台，而是实现一个 Java 后端项目中最常见、最能体现业务复杂度的工单流转核心模块。重点不是堆功能，而是把工单状态、处理人、SLA、超时任务、流转记录这几条主线做清楚。

### 核心业务流程

工单从创建到关闭，核心流程如下：

```text
用户创建工单
-> 系统根据工单类型、优先级、服务组自动分派处理人
-> 处理人接单
-> 工单进入处理中
-> 处理人可以转交、升级、挂起、恢复
-> 处理完成后等待用户确认
-> 用户确认后关闭工单
-> 如果用户长时间未确认，系统可自动关闭
```

SLA 超时处理流程如下：

```text
工单创建
-> 根据优先级和类型匹配 SLA 规则
-> 计算首次响应截止时间和处理完成截止时间
-> 定时任务扫描即将超时和已经超时的工单
-> 即将超时发送提醒
-> 已经超时触发升级
-> 记录 SLA 超时事件
-> 推送站内信 / WebSocket / MQ 通知
```

核心状态流转可以简化为：

```text
待分派 ASSIGNING
-> 待接单 WAIT_ACCEPT
-> 处理中 PROCESSING
-> 已挂起 SUSPENDED
-> 待用户确认 WAIT_CONFIRM
-> 已关闭 CLOSED
```

异常或协作类流转包括：

```text
转交：PROCESSING -> WAIT_ACCEPT
升级：PROCESSING -> PROCESSING
挂起：PROCESSING -> SUSPENDED
恢复：SUSPENDED -> PROCESSING
用户拒绝：WAIT_CONFIRM -> PROCESSING
```

本案例的关键目标是让每一次状态变化都具备明确的业务动作、操作人、处理人、时间点和记录，避免只更新 `ticket.status` 却无法追踪过程。

### 本案例实现范围

本案例实现以下核心能力：

| 功能               | 是否实现   | 说明                                            |
| ------------------ | ---------- | ----------------------------------------------- |
| 工单创建           | 实现       | 用户提交标题、内容、类型、优先级                |
| 自动派单           | 实现       | 根据工单类型和优先级分配处理人                  |
| 接单               | 实现       | 处理人确认接收工单                              |
| 工单处理           | 实现       | 记录处理意见，推进状态                          |
| 转交               | 实现       | 更换处理人并记录流转                            |
| 升级               | 实现       | 提升优先级或转给高级处理组                      |
| 挂起 / 恢复        | 实现       | 挂起期间暂停处理动作，但 SLA 是否暂停由规则控制 |
| 用户确认           | 实现       | 用户确认后关闭工单                              |
| SLA 截止时间计算   | 实现       | 创建工单时计算响应和处理截止时间                |
| SLA 超时扫描       | 实现       | 使用 XXL-JOB 定时扫描超时工单                   |
| 超时提醒           | 实现       | 即将超时发送 MQ 通知                            |
| 超时升级           | 实现       | 超时后自动升级处理人或处理组                    |
| 流转记录           | 实现       | 每次动作写入工单记录表                          |
| 评论协作           | 简化实现   | 保存评论内容，不展开复杂会话能力                |
| 附件上传           | 预留字段   | 可对接 MinIO，不作为本案例重点                  |
| WebSocket 推送     | 给出集成点 | 不展开完整前端在线状态管理                      |
| Elasticsearch 查询 | 不实现     | 本案例使用 MySQL 查询，复杂搜索可后续扩展       |

本案例不覆盖以下内容：

```text
1. 不实现完整低代码工单表单。
2. 不实现复杂组织架构和排班系统。
3. 不实现完整客服 IM 会话。
4. 不实现多租户隔离。
5. 不实现附件分片上传。
6. 不实现 Elasticsearch 复杂检索。
7. 不实现完整 WebSocket 集群在线状态。
```

后续代码会重点落在以下几个核心对象上：

```text
Ticket              工单主表
TicketFlowRecord    工单流转记录
TicketComment       工单评论
TicketSlaRule       SLA 规则
TicketStatusEnum    工单状态枚举
TicketEventEnum     工单事件枚举
TicketService       工单核心业务服务
TicketSlaJob        SLA 超时扫描任务
TicketNotifyProducer 工单通知消息生产者
```

本案例默认采用以下基础约定：

```text
1. 使用 Spring Boot 3。
2. 使用 MyBatis-Plus 操作 MySQL。
3. 使用 Hutool 处理时间、集合、字符串和对象判断。
4. 使用 RabbitMQ 发送提醒和升级通知。
5. 使用 XXL-JOB 定时扫描 SLA 超时工单。
6. 使用 Sa-Token 获取当前登录用户。
7. 使用 Redis 做部分防重复和缓存扩展点。
8. 所有工单状态流转必须写入流转记录。
9. 所有核心更新必须校验当前状态，避免非法流转。
10. SLA 超时任务必须支持重复执行，避免重复升级和重复提醒。
```

## 技术栈与项目结构

本案例基于原文档中“工单流转与 SLA”推荐的技术方向展开，核心围绕 Spring Boot、MyBatis-Plus、Redis、RabbitMQ、XXL-JOB、WebSocket、MinIO、Sa-Token、Elasticsearch 等组件设计。这里优先实现工单流转与 SLA 超时处理的核心链路，搜索、附件、实时推送等能力保留可扩展接口。

### 技术选型

| 技术          | 用途                                     |
| ------------- | ---------------------------------------- |
| Spring Boot 3 | 后端基础框架                             |
| MyBatis-Plus  | 工单、流转记录、评论、SLA 规则的数据访问 |
| MySQL         | 业务主库                                 |
| Redis         | 防重复操作、缓存 SLA 规则、扩展分布式锁  |
| RabbitMQ      | 工单提醒、超时升级、状态变更通知         |
| XXL-JOB       | SLA 超时扫描任务                         |
| Sa-Token      | 获取当前登录用户和权限控制               |
| Hutool        | 时间、字符串、集合、对象工具类           |
| Lombok        | 简化实体类、DTO、VO                      |
| WebSocket     | 工单状态实时推送扩展点                   |
| MinIO         | 工单附件扩展点                           |
| Elasticsearch | 工单全文检索扩展点                       |

核心实现优先级如下：

```text
必须实现：
工单创建、自动派单、接单、处理、转交、升级、挂起、恢复、确认关闭、SLA 截止时间计算、SLA 超时扫描、流转记录。

简化实现：
评论协作、消息通知、用户身份获取。

预留扩展：
附件上传、WebSocket 实时推送、Elasticsearch 检索、复杂排班、节假日工作时间计算。
```

### 模块目录结构

下面目录结构按单体 Spring Boot 项目设计，后续也可以拆成独立的 `ticket-service` 微服务。

```text
src/main/java/io/github/atengk/ticket
├── TicketApplication.java
├── common
│   ├── enums
│   │   ├── TicketEventEnum.java
│   │   ├── TicketPriorityEnum.java
│   │   └── TicketStatusEnum.java
│   ├── exception
│   │   └── BizException.java
│   └── result
│       └── R.java
├── config
│   ├── RabbitConfig.java
│   └── MybatisPlusConfig.java
├── controller
│   └── TicketController.java
├── dto
│   ├── TicketCreateDTO.java
│   ├── TicketHandleDTO.java
│   ├── TicketTransferDTO.java
│   └── TicketUpgradeDTO.java
├── entity
│   ├── Ticket.java
│   ├── TicketComment.java
│   ├── TicketFlowRecord.java
│   └── TicketSlaRule.java
├── job
│   └── TicketSlaJob.java
├── mapper
│   ├── TicketCommentMapper.java
│   ├── TicketFlowRecordMapper.java
│   ├── TicketMapper.java
│   └── TicketSlaRuleMapper.java
├── mq
│   ├── TicketNotifyMessage.java
│   └── TicketNotifyProducer.java
├── service
│   ├── TicketAssignService.java
│   ├── TicketFlowRecordService.java
│   ├── TicketService.java
│   ├── TicketSlaService.java
│   └── impl
│       ├── TicketAssignServiceImpl.java
│       ├── TicketFlowRecordServiceImpl.java
│       ├── TicketServiceImpl.java
│       └── TicketSlaServiceImpl.java
└── statemachine
    └── TicketStateMachine.java
```

后续重点代码主要集中在：

```text
TicketServiceImpl          工单创建、接单、处理、转交、升级、挂起、恢复、关闭
TicketStateMachine         状态流转规则校验
TicketSlaServiceImpl       SLA 规则匹配和截止时间计算
TicketSlaJob               SLA 即将超时和已超时扫描
TicketFlowRecordService    工单流转记录落库
TicketNotifyProducer       工单消息通知
```

## 数据库表设计

数据库设计重点围绕“四张表”：工单主表、流转记录表、评论表、SLA 规则表。工单主表只保存当前状态和关键节点时间，完整过程通过流转记录表追溯。

### 工单主表

`t_ticket` 保存工单当前状态、处理人、SLA 截止时间和超时标记。状态流转时只更新当前快照，历史过程写入 `t_ticket_flow_record`。

```sql
CREATE TABLE t_ticket (
    id BIGINT NOT NULL COMMENT '主键ID',
    ticket_no VARCHAR(64) NOT NULL COMMENT '工单编号',
    title VARCHAR(128) NOT NULL COMMENT '工单标题',
    content TEXT NOT NULL COMMENT '工单内容',
    ticket_type VARCHAR(64) NOT NULL COMMENT '工单类型：AFTER_SALE-售后，OPS-运维，COMPLAINT-投诉',
    priority VARCHAR(32) NOT NULL COMMENT '优先级：LOW-低，MEDIUM-中，HIGH-高，URGENT-紧急',
    status VARCHAR(32) NOT NULL COMMENT '状态：ASSIGNING-待分派，WAIT_ACCEPT-待接单，PROCESSING-处理中，SUSPENDED-已挂起，WAIT_CONFIRM-待确认，CLOSED-已关闭',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    creator_name VARCHAR(64) NOT NULL COMMENT '创建人名称',
    handler_id BIGINT DEFAULT NULL COMMENT '当前处理人ID',
    handler_name VARCHAR(64) DEFAULT NULL COMMENT '当前处理人名称',
    service_group_id BIGINT DEFAULT NULL COMMENT '服务组ID',
    service_group_name VARCHAR(64) DEFAULT NULL COMMENT '服务组名称',
    response_deadline DATETIME DEFAULT NULL COMMENT '首次响应截止时间',
    handle_deadline DATETIME DEFAULT NULL COMMENT '处理完成截止时间',
    first_response_time DATETIME DEFAULT NULL COMMENT '首次响应时间',
    finish_time DATETIME DEFAULT NULL COMMENT '处理完成时间',
    close_time DATETIME DEFAULT NULL COMMENT '关闭时间',
    suspend_time DATETIME DEFAULT NULL COMMENT '挂起时间',
    response_timeout TINYINT NOT NULL DEFAULT 0 COMMENT '是否响应超时：0-否，1-是',
    handle_timeout TINYINT NOT NULL DEFAULT 0 COMMENT '是否处理超时：0-否，1-是',
    remind_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发送超时提醒：0-否，1-是',
    upgrade_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否已超时升级：0-否，1-是',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_creator_id (creator_id),
    KEY idx_handler_status (handler_id, status),
    KEY idx_status_handle_deadline (status, handle_deadline),
    KEY idx_response_deadline (response_deadline),
    KEY idx_handle_deadline (handle_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单主表';
```

关键字段说明：

| 字段                  | 说明                               |
| --------------------- | ---------------------------------- |
| `status`              | 当前工单状态                       |
| `handler_id`          | 当前处理人，转交和升级时会变化     |
| `response_deadline`   | 首次响应 SLA 截止时间              |
| `handle_deadline`     | 处理完成 SLA 截止时间              |
| `first_response_time` | 首次接单或首次处理时间             |
| `finish_time`         | 处理完成时间，进入待用户确认时写入 |
| `response_timeout`    | 是否首次响应超时                   |
| `handle_timeout`      | 是否处理完成超时                   |
| `remind_flag`         | 防止重复发送即将超时提醒           |
| `upgrade_flag`        | 防止重复执行超时升级               |
| `version`             | 并发更新控制，防止重复流转         |

### 工单流转记录表

`t_ticket_flow_record` 保存每一次工单动作，例如创建、分派、接单、处理、转交、升级、挂起、恢复、关闭。后续排查问题时，不看日志也能追溯完整业务过程。

```sql
CREATE TABLE t_ticket_flow_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    ticket_no VARCHAR(64) NOT NULL COMMENT '工单编号',
    event VARCHAR(32) NOT NULL COMMENT '事件：CREATE-创建，ASSIGN-分派，ACCEPT-接单，HANDLE-处理，TRANSFER-转交，UPGRADE-升级，SUSPEND-挂起，RESUME-恢复，CONFIRM-确认，CLOSE-关闭',
    from_status VARCHAR(32) DEFAULT NULL COMMENT '变更前状态',
    to_status VARCHAR(32) NOT NULL COMMENT '变更后状态',
    from_handler_id BIGINT DEFAULT NULL COMMENT '原处理人ID',
    from_handler_name VARCHAR(64) DEFAULT NULL COMMENT '原处理人名称',
    to_handler_id BIGINT DEFAULT NULL COMMENT '新处理人ID',
    to_handler_name VARCHAR(64) DEFAULT NULL COMMENT '新处理人名称',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) NOT NULL COMMENT '操作人名称',
    remark VARCHAR(512) DEFAULT NULL COMMENT '处理说明',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ticket_id (ticket_id),
    KEY idx_ticket_no (ticket_no),
    KEY idx_operator_id (operator_id),
    KEY idx_event (event),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流转记录表';
```

推荐写入规则：

```text
1. 创建工单后写入 CREATE 记录。
2. 自动派单后写入 ASSIGN 记录。
3. 接单、处理、转交、升级、挂起、恢复、确认、关闭全部写入记录。
4. 定时任务触发的 SLA 超时升级也必须写入 UPGRADE 记录。
5. 不允许只改工单主表而不写流转记录。
```

### 工单评论表

`t_ticket_comment` 用于工单内部协作。评论不负责改变工单状态，只负责协作留痕。

```sql
CREATE TABLE t_ticket_comment (
    id BIGINT NOT NULL COMMENT '主键ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    ticket_no VARCHAR(64) NOT NULL COMMENT '工单编号',
    user_id BIGINT NOT NULL COMMENT '评论人ID',
    user_name VARCHAR(64) NOT NULL COMMENT '评论人名称',
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    internal_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否内部评论：0-用户可见，1-内部可见',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ticket_id (ticket_id),
    KEY idx_ticket_no (ticket_no),
    KEY idx_user_id (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单评论表';
```

`internal_flag` 用于区分用户可见评论和内部协作备注：

```text
internal_flag = 0：用户、客服、处理人都可见。
internal_flag = 1：仅内部处理人、管理员、服务组成员可见。
```

### SLA 规则表

`t_ticket_sla_rule` 用于配置不同类型、不同优先级工单的响应时限和处理时限。创建工单时，根据 `ticket_type + priority` 匹配规则并计算截止时间。

```sql
CREATE TABLE t_ticket_sla_rule (
    id BIGINT NOT NULL COMMENT '主键ID',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    ticket_type VARCHAR(64) NOT NULL COMMENT '工单类型',
    priority VARCHAR(32) NOT NULL COMMENT '优先级',
    response_minutes INT NOT NULL COMMENT '首次响应时限，单位分钟',
    handle_minutes INT NOT NULL COMMENT '处理完成时限，单位分钟',
    remind_before_minutes INT NOT NULL DEFAULT 30 COMMENT '提前提醒分钟数',
    upgrade_handler_id BIGINT DEFAULT NULL COMMENT '超时升级处理人ID',
    upgrade_handler_name VARCHAR(64) DEFAULT NULL COMMENT '超时升级处理人名称',
    enable_flag TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_priority (ticket_type, priority, deleted),
    KEY idx_enable_flag (enable_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SLA规则表';
```

初始化几条 SLA 规则，便于后续验证创建工单和超时扫描。

```sql
INSERT INTO t_ticket_sla_rule (
    id, rule_name, ticket_type, priority,
    response_minutes, handle_minutes, remind_before_minutes,
    upgrade_handler_id, upgrade_handler_name,
    enable_flag, deleted, create_time, update_time
) VALUES
(1, '售后普通工单SLA', 'AFTER_SALE', 'MEDIUM', 30, 240, 30, 9001, '售后主管', 1, 0, NOW(), NOW()),
(2, '售后紧急工单SLA', 'AFTER_SALE', 'URGENT', 5, 60, 10, 9001, '售后主管', 1, 0, NOW(), NOW()),
(3, '运维高优工单SLA', 'OPS', 'HIGH', 10, 120, 20, 9002, '运维负责人', 1, 0, NOW(), NOW()),
(4, '投诉紧急工单SLA', 'COMPLAINT', 'URGENT', 5, 30, 5, 9003, '投诉处理主管', 1, 0, NOW(), NOW());
```

## 状态机设计

状态机用于限制非法流转，避免出现“已关闭工单继续处理”“待接单工单直接关闭”“挂起工单重复挂起”等问题。工单模块中所有状态变化都应该先经过状态机校验，再执行数据库更新。

### 工单状态枚举

状态枚举用于统一数据库状态值、接口返回值和业务判断条件。

文件位置：`src/main/java/io/github/atengk/ticket/common/enums/TicketStatusEnum.java`

下面代码定义工单状态枚举，所有数据库中的 `status` 字段都使用这里的 `code` 值。

```java
package io.github.atengk.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum TicketStatusEnum {

    /**
     * 待分派
     */
    ASSIGNING("ASSIGNING", "待分派"),

    /**
     * 待接单
     */
    WAIT_ACCEPT("WAIT_ACCEPT", "待接单"),

    /**
     * 处理中
     */
    PROCESSING("PROCESSING", "处理中"),

    /**
     * 已挂起
     */
    SUSPENDED("SUSPENDED", "已挂起"),

    /**
     * 待用户确认
     */
    WAIT_CONFIRM("WAIT_CONFIRM", "待用户确认"),

    /**
     * 已关闭
     */
    CLOSED("CLOSED", "已关闭");

    private final String code;

    private final String desc;
}
```

### 工单事件枚举

事件枚举用于描述触发状态变化的业务动作。状态是结果，事件是动作，不建议混在一起。

文件位置：`src/main/java/io/github/atengk/ticket/common/enums/TicketEventEnum.java`

下面代码定义工单事件枚举，后续流转记录表的 `event` 字段也使用这里的 `code` 值。

```java
package io.github.atengk.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单事件枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum TicketEventEnum {

    /**
     * 创建工单
     */
    CREATE("CREATE", "创建工单"),

    /**
     * 自动分派
     */
    ASSIGN("ASSIGN", "自动分派"),

    /**
     * 处理人接单
     */
    ACCEPT("ACCEPT", "处理人接单"),

    /**
     * 处理工单
     */
    HANDLE("HANDLE", "处理工单"),

    /**
     * 转交工单
     */
    TRANSFER("TRANSFER", "转交工单"),

    /**
     * 升级工单
     */
    UPGRADE("UPGRADE", "升级工单"),

    /**
     * 挂起工单
     */
    SUSPEND("SUSPEND", "挂起工单"),

    /**
     * 恢复工单
     */
    RESUME("RESUME", "恢复工单"),

    /**
     * 用户确认
     */
    CONFIRM("CONFIRM", "用户确认"),

    /**
     * 关闭工单
     */
    CLOSE("CLOSE", "关闭工单"),

    /**
     * 用户拒绝处理结果
     */
    REJECT("REJECT", "用户拒绝处理结果");

    private final String code;

    private final String desc;
}
```

优先级枚举在 SLA 规则匹配时会用到。

文件位置：`src/main/java/io/github/atengk/ticket/common/enums/TicketPriorityEnum.java`

下面代码定义工单优先级枚举，紧急程度越高，SLA 时限通常越短。

```java
package io.github.atengk.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单优先级枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum TicketPriorityEnum {

    /**
     * 低优先级
     */
    LOW("LOW", "低"),

    /**
     * 中优先级
     */
    MEDIUM("MEDIUM", "中"),

    /**
     * 高优先级
     */
    HIGH("HIGH", "高"),

    /**
     * 紧急
     */
    URGENT("URGENT", "紧急");

    private final String code;

    private final String desc;
}
```

### 状态流转规则

状态流转规则建议集中维护，不要散落在多个 Service 的 `if else` 中。这样后续新增状态或事件时，只需要调整状态机即可。

状态流转表如下：

| 当前状态       | 事件       | 目标状态       | 说明                   |
| -------------- | ---------- | -------------- | ---------------------- |
| `ASSIGNING`    | `ASSIGN`   | `WAIT_ACCEPT`  | 系统自动派单           |
| `WAIT_ACCEPT`  | `ACCEPT`   | `PROCESSING`   | 处理人接单             |
| `PROCESSING`   | `HANDLE`   | `WAIT_CONFIRM` | 处理完成，等待用户确认 |
| `PROCESSING`   | `TRANSFER` | `WAIT_ACCEPT`  | 转交给新处理人         |
| `PROCESSING`   | `UPGRADE`  | `PROCESSING`   | 升级处理人或服务组     |
| `PROCESSING`   | `SUSPEND`  | `SUSPENDED`    | 工单挂起               |
| `SUSPENDED`    | `RESUME`   | `PROCESSING`   | 恢复处理               |
| `WAIT_CONFIRM` | `CONFIRM`  | `CLOSED`       | 用户确认关闭           |
| `WAIT_CONFIRM` | `REJECT`   | `PROCESSING`   | 用户拒绝，退回处理     |
| `WAIT_CONFIRM` | `CLOSE`    | `CLOSED`       | 系统自动关闭           |
| `ASSIGNING`    | `CLOSE`    | `CLOSED`       | 管理员关闭异常工单     |
| `WAIT_ACCEPT`  | `CLOSE`    | `CLOSED`       | 管理员关闭未接单工单   |
| `PROCESSING`   | `CLOSE`    | `CLOSED`       | 管理员强制关闭         |
| `SUSPENDED`    | `CLOSE`    | `CLOSED`       | 管理员关闭挂起工单     |

文件位置：`src/main/java/io/github/atengk/ticket/statemachine/TicketStateMachine.java`

下面代码集中维护工单状态流转规则，并提供状态流转校验和目标状态计算能力。

```java
package io.github.atengk.ticket.statemachine;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.ticket.common.enums.TicketEventEnum;
import io.github.atengk.ticket.common.enums.TicketStatusEnum;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 工单状态机
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class TicketStateMachine {

    private static final Map<TicketStatusEnum, Map<TicketEventEnum, TicketStatusEnum>> FLOW_RULE_MAP = new EnumMap<>(TicketStatusEnum.class);

    static {
        put(TicketStatusEnum.ASSIGNING, TicketEventEnum.ASSIGN, TicketStatusEnum.WAIT_ACCEPT);
        put(TicketStatusEnum.WAIT_ACCEPT, TicketEventEnum.ACCEPT, TicketStatusEnum.PROCESSING);
        put(TicketStatusEnum.PROCESSING, TicketEventEnum.HANDLE, TicketStatusEnum.WAIT_CONFIRM);
        put(TicketStatusEnum.PROCESSING, TicketEventEnum.TRANSFER, TicketStatusEnum.WAIT_ACCEPT);
        put(TicketStatusEnum.PROCESSING, TicketEventEnum.UPGRADE, TicketStatusEnum.PROCESSING);
        put(TicketStatusEnum.PROCESSING, TicketEventEnum.SUSPEND, TicketStatusEnum.SUSPENDED);
        put(TicketStatusEnum.SUSPENDED, TicketEventEnum.RESUME, TicketStatusEnum.PROCESSING);
        put(TicketStatusEnum.WAIT_CONFIRM, TicketEventEnum.CONFIRM, TicketStatusEnum.CLOSED);
        put(TicketStatusEnum.WAIT_CONFIRM, TicketEventEnum.REJECT, TicketStatusEnum.PROCESSING);
        put(TicketStatusEnum.WAIT_CONFIRM, TicketEventEnum.CLOSE, TicketStatusEnum.CLOSED);

        // 管理员关闭异常工单的兜底规则
        put(TicketStatusEnum.ASSIGNING, TicketEventEnum.CLOSE, TicketStatusEnum.CLOSED);
        put(TicketStatusEnum.WAIT_ACCEPT, TicketEventEnum.CLOSE, TicketStatusEnum.CLOSED);
        put(TicketStatusEnum.PROCESSING, TicketEventEnum.CLOSE, TicketStatusEnum.CLOSED);
        put(TicketStatusEnum.SUSPENDED, TicketEventEnum.CLOSE, TicketStatusEnum.CLOSED);
    }

    private TicketStateMachine() {
    }

    /**
     * 添加状态流转规则
     *
     * @param fromStatus 来源状态
     * @param event      触发事件
     * @param toStatus   目标状态
     */
    private static void put(TicketStatusEnum fromStatus, TicketEventEnum event, TicketStatusEnum toStatus) {
        FLOW_RULE_MAP.computeIfAbsent(fromStatus, key -> new EnumMap<>(TicketEventEnum.class)).put(event, toStatus);
    }

    /**
     * 获取目标状态
     *
     * @param fromStatus 来源状态
     * @param event      触发事件
     * @return 目标状态
     */
    public static TicketStatusEnum getTargetStatus(TicketStatusEnum fromStatus, TicketEventEnum event) {
        Map<TicketEventEnum, TicketStatusEnum> eventMap = FLOW_RULE_MAP.get(fromStatus);
        if (CollUtil.isEmpty(eventMap)) {
            return null;
        }
        return eventMap.get(event);
    }

    /**
     * 判断状态事件是否允许流转
     *
     * @param fromStatus 来源状态
     * @param event      触发事件
     * @return 是否允许
     */
    public static boolean canTransit(TicketStatusEnum fromStatus, TicketEventEnum event) {
        return Objects.nonNull(getTargetStatus(fromStatus, event));
    }

    /**
     * 校验状态事件是否允许流转
     *
     * @param fromStatus 来源状态
     * @param event      触发事件
     * @return 目标状态
     */
    public static TicketStatusEnum checkAndGetTargetStatus(TicketStatusEnum fromStatus, TicketEventEnum event) {
        TicketStatusEnum targetStatus = getTargetStatus(fromStatus, event);
        if (Objects.isNull(targetStatus)) {
            throw new IllegalStateException("非法工单状态流转，当前状态：" + fromStatus.getDesc() + "，事件：" + event.getDesc());
        }
        return targetStatus;
    }
}
```

状态机使用示例：

```java
TicketStatusEnum targetStatus = TicketStateMachine.checkAndGetTargetStatus(
        TicketStatusEnum.PROCESSING,
        TicketEventEnum.TRANSFER
);

// targetStatus = WAIT_ACCEPT
```

在实际 Service 中，不建议直接根据前端传入的目标状态更新工单，而应该由后端根据事件计算目标状态：

```text
推荐：
前端传 event = TRANSFER，后端通过状态机计算 toStatus = WAIT_ACCEPT。

不推荐：
前端直接传 status = WAIT_ACCEPT，后端无校验更新。
```

下一节可继续实现实体类、Mapper、Service 基础结构和工单创建逻辑。

## 核心代码实现

本节实现工单流转的核心后端代码，围绕原 README 中“工单状态机、自动派单规则、处理人变更、SLA 计时、超时升级、处理记录、评论协作、消息通知”等难点展开。

### 实体类与枚举定义

上一节已经给出了 `TicketStatusEnum`、`TicketEventEnum`、`TicketPriorityEnum` 和 `TicketStateMachine`，这里补充工单类型枚举和四张核心表对应的实体类。

文件位置：`src/main/java/io/github/atengk/ticket/common/enums/TicketTypeEnum.java`

下面代码定义工单类型枚举，用于区分售后、运维、投诉等不同处理流程。

```java
package io.github.atengk.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum TicketTypeEnum {

    /**
     * 售后工单
     */
    AFTER_SALE("AFTER_SALE", "售后工单"),

    /**
     * 运维工单
     */
    OPS("OPS", "运维工单"),

    /**
     * 投诉工单
     */
    COMPLAINT("COMPLAINT", "投诉工单");

    private final String code;

    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/entity/Ticket.java`

下面代码定义工单主表实体，保存工单当前状态、处理人、SLA 时间和超时标记。

```java
package io.github.atengk.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工单主表实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ticket")
public class Ticket {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ticketNo;

    private String title;

    private String content;

    private String ticketType;

    private String priority;

    private String status;

    private Long creatorId;

    private String creatorName;

    private Long handlerId;

    private String handlerName;

    private Long serviceGroupId;

    private String serviceGroupName;

    private LocalDateTime responseDeadline;

    private LocalDateTime handleDeadline;

    private LocalDateTime firstResponseTime;

    private LocalDateTime finishTime;

    private LocalDateTime closeTime;

    private LocalDateTime suspendTime;

    private Integer responseTimeout;

    private Integer handleTimeout;

    private Integer remindFlag;

    private Integer upgradeFlag;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/entity/TicketFlowRecord.java`

下面代码定义工单流转记录实体，每次状态变化、处理人变化都需要写入该表。

```java
package io.github.atengk.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工单流转记录实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ticket_flow_record")
public class TicketFlowRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ticketId;

    private String ticketNo;

    private String event;

    private String fromStatus;

    private String toStatus;

    private Long fromHandlerId;

    private String fromHandlerName;

    private Long toHandlerId;

    private String toHandlerName;

    private Long operatorId;

    private String operatorName;

    private String remark;

    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/entity/TicketComment.java`

下面代码定义工单评论实体，评论只做协作留痕，不直接改变工单状态。

```java
package io.github.atengk.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工单评论实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ticket_comment")
public class TicketComment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ticketId;

    private String ticketNo;

    private Long userId;

    private String userName;

    private String content;

    private Integer internalFlag;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/entity/TicketSlaRule.java`

下面代码定义 SLA 规则实体，创建工单时根据工单类型和优先级匹配该规则。

```java
package io.github.atengk.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SLA 规则实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ticket_sla_rule")
public class TicketSlaRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleName;

    private String ticketType;

    private String priority;

    private Integer responseMinutes;

    private Integer handleMinutes;

    private Integer remindBeforeMinutes;

    private Long upgradeHandlerId;

    private String upgradeHandlerName;

    private Integer enableFlag;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/dto/TicketCreateDTO.java`

下面代码定义创建工单请求参数，接口层接收该对象后传入 Service。

```java
package io.github.atengk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建工单请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class TicketCreateDTO {

    @NotBlank(message = "工单标题不能为空")
    private String title;

    @NotBlank(message = "工单内容不能为空")
    private String content;

    @NotBlank(message = "工单类型不能为空")
    private String ticketType;

    @NotBlank(message = "工单优先级不能为空")
    private String priority;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/dto/TicketHandleDTO.java`

下面代码定义接单、处理、挂起、恢复、确认、关闭等通用处理参数。

```java
package io.github.atengk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工单处理请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class TicketHandleDTO {

    @NotBlank(message = "处理说明不能为空")
    private String remark;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/dto/TicketTransferDTO.java`

下面代码定义转交工单请求参数，转交后工单会重新进入待接单状态。

```java
package io.github.atengk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 工单转交请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class TicketTransferDTO {

    @NotNull(message = "新处理人ID不能为空")
    private Long newHandlerId;

    @NotBlank(message = "新处理人名称不能为空")
    private String newHandlerName;

    @NotBlank(message = "转交原因不能为空")
    private String remark;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/dto/TicketUpgradeDTO.java`

下面代码定义升级工单请求参数，升级后工单仍处于处理中，但处理人会发生变化。

```java
package io.github.atengk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 工单升级请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class TicketUpgradeDTO {

    @NotNull(message = "升级处理人ID不能为空")
    private Long upgradeHandlerId;

    @NotBlank(message = "升级处理人名称不能为空")
    private String upgradeHandlerName;

    @NotBlank(message = "升级原因不能为空")
    private String remark;
}
```

### Mapper 与 Service 基础结构

Mapper 使用 MyBatis-Plus 的 `BaseMapper` 即可，常规 CRUD 不需要额外 XML。复杂查询可以后续再加自定义 Mapper 方法。

文件位置：`src/main/java/io/github/atengk/ticket/mapper/TicketMapper.java`

下面代码定义工单主表 Mapper。

```java
package io.github.atengk.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单主表 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
}
```

文件位置：`src/main/java/io/github/atengk/ticket/mapper/TicketFlowRecordMapper.java`

下面代码定义工单流转记录 Mapper。

```java
package io.github.atengk.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.ticket.entity.TicketFlowRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单流转记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface TicketFlowRecordMapper extends BaseMapper<TicketFlowRecord> {
}
```

文件位置：`src/main/java/io/github/atengk/ticket/mapper/TicketCommentMapper.java`

下面代码定义工单评论 Mapper。

```java
package io.github.atengk.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.ticket.entity.TicketComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单评论 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface TicketCommentMapper extends BaseMapper<TicketComment> {
}
```

文件位置：`src/main/java/io/github/atengk/ticket/mapper/TicketSlaRuleMapper.java`

下面代码定义 SLA 规则 Mapper。

```java
package io.github.atengk.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.ticket.entity.TicketSlaRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * SLA 规则 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface TicketSlaRuleMapper extends BaseMapper<TicketSlaRule> {
}
```

文件位置：`src/main/java/io/github/atengk/ticket/common/exception/BizException.java`

下面代码定义业务异常，非法状态流转、无 SLA 规则、无权限处理等场景直接抛出该异常。

```java
package io.github.atengk.ticket.common.exception;

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

    /**
     * 创建业务异常
     *
     * @param message 异常信息
     * @return 业务异常
     */
    public static BizException of(String message) {
        return new BizException(message);
    }
}
```

文件位置：`src/main/java/io/github/atengk/ticket/dto/AssignResult.java`

下面代码定义自动派单结果，自动派单服务返回处理人和服务组信息。

```java
package io.github.atengk.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自动派单结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignResult {

    private Long handlerId;

    private String handlerName;

    private Long serviceGroupId;

    private String serviceGroupName;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/service/TicketService.java`

下面代码定义工单核心 Service 接口，覆盖创建、接单、处理、转交、升级、挂起、恢复、确认和关闭。

```java
package io.github.atengk.ticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.ticket.dto.TicketCreateDTO;
import io.github.atengk.ticket.dto.TicketHandleDTO;
import io.github.atengk.ticket.dto.TicketTransferDTO;
import io.github.atengk.ticket.dto.TicketUpgradeDTO;
import io.github.atengk.ticket.entity.Ticket;

/**
 * 工单核心 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface TicketService extends IService<Ticket> {

    /**
     * 创建工单
     *
     * @param dto 创建参数
     * @return 工单ID
     */
    Long createTicket(TicketCreateDTO dto);

    /**
     * 处理人接单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    void acceptTicket(Long ticketId, TicketHandleDTO dto);

    /**
     * 处理完成工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    void handleTicket(Long ticketId, TicketHandleDTO dto);

    /**
     * 转交工单
     *
     * @param ticketId 工单ID
     * @param dto      转交参数
     */
    void transferTicket(Long ticketId, TicketTransferDTO dto);

    /**
     * 升级工单
     *
     * @param ticketId 工单ID
     * @param dto      升级参数
     */
    void upgradeTicket(Long ticketId, TicketUpgradeDTO dto);

    /**
     * 挂起工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    void suspendTicket(Long ticketId, TicketHandleDTO dto);

    /**
     * 恢复工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    void resumeTicket(Long ticketId, TicketHandleDTO dto);

    /**
     * 用户确认工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    void confirmTicket(Long ticketId, TicketHandleDTO dto);

    /**
     * 关闭工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    void closeTicket(Long ticketId, TicketHandleDTO dto);
}
```

文件位置：`src/main/java/io/github/atengk/ticket/service/TicketAssignService.java`

下面代码定义自动派单 Service 接口，当前案例先实现简单规则，后续可替换为规则引擎或排班系统。

```java
package io.github.atengk.ticket.service;

import io.github.atengk.ticket.dto.AssignResult;
import io.github.atengk.ticket.entity.Ticket;

/**
 * 工单自动派单 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface TicketAssignService {

    /**
     * 自动分派工单
     *
     * @param ticket 工单
     * @return 分派结果
     */
    AssignResult assign(Ticket ticket);
}
```

文件位置：`src/main/java/io/github/atengk/ticket/service/TicketSlaService.java`

下面代码定义 SLA Service 接口，本节只用于创建工单时填充 SLA 截止时间，完整超时扫描放到下一节。

```java
package io.github.atengk.ticket.service;

import io.github.atengk.ticket.entity.Ticket;

/**
 * 工单 SLA Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface TicketSlaService {

    /**
     * 填充 SLA 截止时间
     *
     * @param ticket 工单
     */
    void fillSlaDeadline(Ticket ticket);
}
```

文件位置：`src/main/java/io/github/atengk/ticket/service/TicketFlowRecordService.java`

下面代码定义工单流转记录 Service 接口。

```java
package io.github.atengk.ticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.ticket.entity.TicketFlowRecord;

/**
 * 工单流转记录 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface TicketFlowRecordService extends IService<TicketFlowRecord> {
}
```

文件位置：`src/main/java/io/github/atengk/ticket/service/impl/TicketFlowRecordServiceImpl.java`

下面代码实现工单流转记录 Service。

```java
package io.github.atengk.ticket.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.ticket.entity.TicketFlowRecord;
import io.github.atengk.ticket.mapper.TicketFlowRecordMapper;
import io.github.atengk.ticket.service.TicketFlowRecordService;
import org.springframework.stereotype.Service;

/**
 * 工单流转记录 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Service
public class TicketFlowRecordServiceImpl extends ServiceImpl<TicketFlowRecordMapper, TicketFlowRecord>
        implements TicketFlowRecordService {
}
```

### 创建工单

创建工单时，需要做四件事：

```text
1. 生成工单编号。
2. 根据工单类型和优先级计算 SLA 截止时间。
3. 插入工单主表，初始状态为待分派。
4. 写入创建记录，并触发自动派单。
```

文件位置：`src/main/java/io/github/atengk/ticket/service/impl/TicketSlaServiceImpl.java`

下面代码实现创建工单时的 SLA 截止时间计算，根据 `ticket_type + priority` 匹配规则。

```java
package io.github.atengk.ticket.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.ticket.common.exception.BizException;
import io.github.atengk.ticket.entity.Ticket;
import io.github.atengk.ticket.entity.TicketSlaRule;
import io.github.atengk.ticket.mapper.TicketSlaRuleMapper;
import io.github.atengk.ticket.service.TicketSlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 工单 SLA Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketSlaServiceImpl implements TicketSlaService {

    private final TicketSlaRuleMapper ticketSlaRuleMapper;

    /**
     * 填充 SLA 截止时间
     *
     * @param ticket 工单
     */
    @Override
    public void fillSlaDeadline(Ticket ticket) {
        TicketSlaRule rule = ticketSlaRuleMapper.selectOne(
                new LambdaQueryWrapper<TicketSlaRule>()
                        .eq(TicketSlaRule::getTicketType, ticket.getTicketType())
                        .eq(TicketSlaRule::getPriority, ticket.getPriority())
                        .eq(TicketSlaRule::getEnableFlag, 1)
                        .last("LIMIT 1")
        );

        if (ObjectUtil.isNull(rule)) {
            throw BizException.of("未匹配到可用的 SLA 规则");
        }

        LocalDateTime now = LocalDateTime.now();
        ticket.setResponseDeadline(now.plusMinutes(rule.getResponseMinutes()));
        ticket.setHandleDeadline(now.plusMinutes(rule.getHandleMinutes()));

        log.info("工单匹配 SLA 规则成功，ticketType={}，priority={}，responseDeadline={}，handleDeadline={}",
                ticket.getTicketType(), ticket.getPriority(), ticket.getResponseDeadline(), ticket.getHandleDeadline());
    }
}
```

### 自动派单

自动派单可以先使用简单规则实现，后续再扩展为“技能组、排班、负载均衡、在线状态、地域、历史处理质量”等复杂策略。

文件位置：`src/main/java/io/github/atengk/ticket/service/impl/TicketAssignServiceImpl.java`

下面代码实现一个简化版自动派单规则，根据工单类型和优先级返回处理人。

```java
package io.github.atengk.ticket.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.ticket.common.enums.TicketPriorityEnum;
import io.github.atengk.ticket.common.enums.TicketTypeEnum;
import io.github.atengk.ticket.dto.AssignResult;
import io.github.atengk.ticket.entity.Ticket;
import io.github.atengk.ticket.service.TicketAssignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 工单自动派单 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class TicketAssignServiceImpl implements TicketAssignService {

    /**
     * 自动分派工单
     *
     * @param ticket 工单
     * @return 分派结果
     */
    @Override
    public AssignResult assign(Ticket ticket) {
        String type = ticket.getTicketType();
        String priority = ticket.getPriority();

        if (StrUtil.equals(priority, TicketPriorityEnum.URGENT.getCode())) {
            log.info("紧急工单进入高级处理组，ticketNo={}", ticket.getTicketNo());
            return AssignResult.builder()
                    .handlerId(9001L)
                    .handlerName("高级值班处理人")
                    .serviceGroupId(900L)
                    .serviceGroupName("高级支持组")
                    .build();
        }

        if (StrUtil.equals(type, TicketTypeEnum.OPS.getCode())) {
            return AssignResult.builder()
                    .handlerId(3001L)
                    .handlerName("运维处理人")
                    .serviceGroupId(300L)
                    .serviceGroupName("运维支持组")
                    .build();
        }

        if (StrUtil.equals(type, TicketTypeEnum.COMPLAINT.getCode())) {
            return AssignResult.builder()
                    .handlerId(4001L)
                    .handlerName("投诉处理人")
                    .serviceGroupId(400L)
                    .serviceGroupName("投诉处理组")
                    .build();
        }

        return AssignResult.builder()
                .handlerId(1001L)
                .handlerName("客服处理人")
                .serviceGroupId(100L)
                .serviceGroupName("客服支持组")
                .build();
    }
}
```

文件位置：`src/main/java/io/github/atengk/ticket/mq/TicketNotifyProducer.java`

下面代码提供工单通知生产者的简化实现，本节先保留日志和发送入口，下一节再展开 RabbitMQ 交换机和队列配置。

```java
package io.github.atengk.ticket.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工单通知消息生产者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketNotifyProducer {

    /**
     * 发送工单通知
     *
     * @param ticketId 工单ID
     * @param event    事件
     * @param content  内容
     */
    public void sendNotify(Long ticketId, String event, String content) {
        // 实际项目中这里可以使用 RabbitTemplate.convertAndSend(...)
        log.info("发送工单通知，ticketId={}，event={}，content={}", ticketId, event, content);
    }
}
```

### 处理人接单

接单时，工单必须处于 `WAIT_ACCEPT`。接单成功后，工单进入 `PROCESSING`，并写入首次响应时间 `first_response_time`。

### 工单处理中

处理完成时，工单必须处于 `PROCESSING`。处理成功后进入 `WAIT_CONFIRM`，等待用户确认。

### 转交工单

转交时，工单必须处于 `PROCESSING`。转交成功后，工单重新进入 `WAIT_ACCEPT`，由新处理人接单。

### 升级工单

升级时，工单仍保持 `PROCESSING`，但处理人会更新为高级处理人或主管。SLA 超时升级也可以复用同一套逻辑。

### 挂起与恢复

挂起时，工单从 `PROCESSING` 进入 `SUSPENDED`。恢复时，从 `SUSPENDED` 回到 `PROCESSING`。是否暂停 SLA 计时，可以在后续 SLA 章节扩展。

### 用户确认与关闭

处理完成后，用户确认工单，状态从 `WAIT_CONFIRM` 进入 `CLOSED`。管理员强制关闭、系统自动关闭也复用 `CLOSE` 事件。

文件位置：`src/main/java/io/github/atengk/ticket/service/impl/TicketServiceImpl.java`

下面代码实现工单创建、自动派单、接单、处理、转交、升级、挂起、恢复、确认和关闭的完整核心逻辑。

```java
package io.github.atengk.ticket.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.ticket.common.enums.TicketEventEnum;
import io.github.atengk.ticket.common.enums.TicketStatusEnum;
import io.github.atengk.ticket.common.exception.BizException;
import io.github.atengk.ticket.dto.AssignResult;
import io.github.atengk.ticket.dto.TicketCreateDTO;
import io.github.atengk.ticket.dto.TicketHandleDTO;
import io.github.atengk.ticket.dto.TicketTransferDTO;
import io.github.atengk.ticket.dto.TicketUpgradeDTO;
import io.github.atengk.ticket.entity.Ticket;
import io.github.atengk.ticket.entity.TicketFlowRecord;
import io.github.atengk.ticket.mapper.TicketMapper;
import io.github.atengk.ticket.mq.TicketNotifyProducer;
import io.github.atengk.ticket.service.TicketAssignService;
import io.github.atengk.ticket.service.TicketFlowRecordService;
import io.github.atengk.ticket.service.TicketService;
import io.github.atengk.ticket.service.TicketSlaService;
import io.github.atengk.ticket.statemachine.TicketStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 工单核心 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketService {

    private final TicketSlaService ticketSlaService;
    private final TicketAssignService ticketAssignService;
    private final TicketFlowRecordService ticketFlowRecordService;
    private final TicketNotifyProducer ticketNotifyProducer;

    /**
     * 创建工单
     *
     * @param dto 创建参数
     * @return 工单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTicket(TicketCreateDTO dto) {
        Operator operator = getCurrentOperator();
        LocalDateTime now = LocalDateTime.now();

        Ticket ticket = Ticket.builder()
                .ticketNo(generateTicketNo())
                .title(dto.getTitle())
                .content(dto.getContent())
                .ticketType(dto.getTicketType())
                .priority(dto.getPriority())
                .status(TicketStatusEnum.ASSIGNING.getCode())
                .creatorId(operator.userId())
                .creatorName(operator.userName())
                .responseTimeout(0)
                .handleTimeout(0)
                .remindFlag(0)
                .upgradeFlag(0)
                .version(0)
                .deleted(0)
                .createTime(now)
                .updateTime(now)
                .build();

        ticketSlaService.fillSlaDeadline(ticket);
        baseMapper.insert(ticket);

        saveFlowRecord(ticket, TicketEventEnum.CREATE, null, TicketStatusEnum.ASSIGNING.getCode(),
                null, null, null, null, operator, "用户创建工单");

        autoAssign(ticket, operator);

        log.info("创建工单成功，ticketId={}，ticketNo={}，creatorId={}", ticket.getId(), ticket.getTicketNo(), operator.userId());
        return ticket.getId();
    }

    /**
     * 处理人接单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptTicket(Long ticketId, TicketHandleDTO dto) {
        Operator operator = getCurrentOperator();
        Ticket ticket = getTicketOrThrow(ticketId);
        checkHandler(ticket, operator.userId());

        TicketStatusEnum fromStatus = TicketStatusEnum.valueOf(ticket.getStatus());
        TicketStatusEnum toStatus = TicketStateMachine.checkAndGetTargetStatus(fromStatus, TicketEventEnum.ACCEPT);

        LocalDateTime now = LocalDateTime.now();
        boolean success = lambdaUpdate()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getStatus, fromStatus.getCode())
                .eq(Ticket::getVersion, ticket.getVersion())
                .set(Ticket::getStatus, toStatus.getCode())
                .set(Ticket::getFirstResponseTime, now)
                .set(Ticket::getUpdateTime, now)
                .set(Ticket::getVersion, ticket.getVersion() + 1)
                .update();

        checkUpdateSuccess(success);

        saveFlowRecord(ticket, TicketEventEnum.ACCEPT, fromStatus.getCode(), toStatus.getCode(),
                ticket.getHandlerId(), ticket.getHandlerName(), ticket.getHandlerId(), ticket.getHandlerName(),
                operator, dto.getRemark());

        ticketNotifyProducer.sendNotify(ticket.getId(), TicketEventEnum.ACCEPT.getCode(), "处理人已接单");
        log.info("处理人接单成功，ticketId={}，handlerId={}", ticketId, operator.userId());
    }

    /**
     * 处理完成工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleTicket(Long ticketId, TicketHandleDTO dto) {
        Operator operator = getCurrentOperator();
        Ticket ticket = getTicketOrThrow(ticketId);
        checkHandler(ticket, operator.userId());

        TicketStatusEnum fromStatus = TicketStatusEnum.valueOf(ticket.getStatus());
        TicketStatusEnum toStatus = TicketStateMachine.checkAndGetTargetStatus(fromStatus, TicketEventEnum.HANDLE);

        LocalDateTime now = LocalDateTime.now();
        boolean success = lambdaUpdate()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getStatus, fromStatus.getCode())
                .eq(Ticket::getVersion, ticket.getVersion())
                .set(Ticket::getStatus, toStatus.getCode())
                .set(Ticket::getFinishTime, now)
                .set(Ticket::getUpdateTime, now)
                .set(Ticket::getVersion, ticket.getVersion() + 1)
                .update();

        checkUpdateSuccess(success);

        saveFlowRecord(ticket, TicketEventEnum.HANDLE, fromStatus.getCode(), toStatus.getCode(),
                ticket.getHandlerId(), ticket.getHandlerName(), ticket.getHandlerId(), ticket.getHandlerName(),
                operator, dto.getRemark());

        ticketNotifyProducer.sendNotify(ticket.getId(), TicketEventEnum.HANDLE.getCode(), "工单已处理完成，等待用户确认");
        log.info("工单处理完成，ticketId={}，handlerId={}", ticketId, operator.userId());
    }

    /**
     * 转交工单
     *
     * @param ticketId 工单ID
     * @param dto      转交参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTicket(Long ticketId, TicketTransferDTO dto) {
        Operator operator = getCurrentOperator();
        Ticket ticket = getTicketOrThrow(ticketId);
        checkHandler(ticket, operator.userId());

        TicketStatusEnum fromStatus = TicketStatusEnum.valueOf(ticket.getStatus());
        TicketStatusEnum toStatus = TicketStateMachine.checkAndGetTargetStatus(fromStatus, TicketEventEnum.TRANSFER);

        LocalDateTime now = LocalDateTime.now();
        boolean success = lambdaUpdate()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getStatus, fromStatus.getCode())
                .eq(Ticket::getVersion, ticket.getVersion())
                .set(Ticket::getStatus, toStatus.getCode())
                .set(Ticket::getHandlerId, dto.getNewHandlerId())
                .set(Ticket::getHandlerName, dto.getNewHandlerName())
                .set(Ticket::getUpdateTime, now)
                .set(Ticket::getVersion, ticket.getVersion() + 1)
                .update();

        checkUpdateSuccess(success);

        saveFlowRecord(ticket, TicketEventEnum.TRANSFER, fromStatus.getCode(), toStatus.getCode(),
                ticket.getHandlerId(), ticket.getHandlerName(), dto.getNewHandlerId(), dto.getNewHandlerName(),
                operator, dto.getRemark());

        ticketNotifyProducer.sendNotify(ticket.getId(), TicketEventEnum.TRANSFER.getCode(), "工单已转交给：" + dto.getNewHandlerName());
        log.info("工单转交成功，ticketId={}，fromHandlerId={}，toHandlerId={}", ticketId, ticket.getHandlerId(), dto.getNewHandlerId());
    }

    /**
     * 升级工单
     *
     * @param ticketId 工单ID
     * @param dto      升级参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upgradeTicket(Long ticketId, TicketUpgradeDTO dto) {
        Operator operator = getCurrentOperator();
        Ticket ticket = getTicketOrThrow(ticketId);

        TicketStatusEnum fromStatus = TicketStatusEnum.valueOf(ticket.getStatus());
        TicketStatusEnum toStatus = TicketStateMachine.checkAndGetTargetStatus(fromStatus, TicketEventEnum.UPGRADE);

        LocalDateTime now = LocalDateTime.now();
        boolean success = lambdaUpdate()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getStatus, fromStatus.getCode())
                .eq(Ticket::getVersion, ticket.getVersion())
                .set(Ticket::getStatus, toStatus.getCode())
                .set(Ticket::getHandlerId, dto.getUpgradeHandlerId())
                .set(Ticket::getHandlerName, dto.getUpgradeHandlerName())
                .set(Ticket::getUpgradeFlag, 1)
                .set(Ticket::getUpdateTime, now)
                .set(Ticket::getVersion, ticket.getVersion() + 1)
                .update();

        checkUpdateSuccess(success);

        saveFlowRecord(ticket, TicketEventEnum.UPGRADE, fromStatus.getCode(), toStatus.getCode(),
                ticket.getHandlerId(), ticket.getHandlerName(), dto.getUpgradeHandlerId(), dto.getUpgradeHandlerName(),
                operator, dto.getRemark());

        ticketNotifyProducer.sendNotify(ticket.getId(), TicketEventEnum.UPGRADE.getCode(), "工单已升级给：" + dto.getUpgradeHandlerName());
        log.info("工单升级成功，ticketId={}，upgradeHandlerId={}", ticketId, dto.getUpgradeHandlerId());
    }

    /**
     * 挂起工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendTicket(Long ticketId, TicketHandleDTO dto) {
        Operator operator = getCurrentOperator();
        Ticket ticket = getTicketOrThrow(ticketId);
        checkHandler(ticket, operator.userId());

        TicketStatusEnum fromStatus = TicketStatusEnum.valueOf(ticket.getStatus());
        TicketStatusEnum toStatus = TicketStateMachine.checkAndGetTargetStatus(fromStatus, TicketEventEnum.SUSPEND);

        LocalDateTime now = LocalDateTime.now();
        boolean success = lambdaUpdate()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getStatus, fromStatus.getCode())
                .eq(Ticket::getVersion, ticket.getVersion())
                .set(Ticket::getStatus, toStatus.getCode())
                .set(Ticket::getSuspendTime, now)
                .set(Ticket::getUpdateTime, now)
                .set(Ticket::getVersion, ticket.getVersion() + 1)
                .update();

        checkUpdateSuccess(success);

        saveFlowRecord(ticket, TicketEventEnum.SUSPEND, fromStatus.getCode(), toStatus.getCode(),
                ticket.getHandlerId(), ticket.getHandlerName(), ticket.getHandlerId(), ticket.getHandlerName(),
                operator, dto.getRemark());

        ticketNotifyProducer.sendNotify(ticket.getId(), TicketEventEnum.SUSPEND.getCode(), "工单已挂起");
        log.info("工单挂起成功，ticketId={}，handlerId={}", ticketId, operator.userId());
    }

    /**
     * 恢复工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeTicket(Long ticketId, TicketHandleDTO dto) {
        Operator operator = getCurrentOperator();
        Ticket ticket = getTicketOrThrow(ticketId);
        checkHandler(ticket, operator.userId());

        TicketStatusEnum fromStatus = TicketStatusEnum.valueOf(ticket.getStatus());
        TicketStatusEnum toStatus = TicketStateMachine.checkAndGetTargetStatus(fromStatus, TicketEventEnum.RESUME);

        LocalDateTime now = LocalDateTime.now();
        boolean success = lambdaUpdate()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getStatus, fromStatus.getCode())
                .eq(Ticket::getVersion, ticket.getVersion())
                .set(Ticket::getStatus, toStatus.getCode())
                .set(Ticket::getUpdateTime, now)
                .set(Ticket::getVersion, ticket.getVersion() + 1)
                .update();

        checkUpdateSuccess(success);

        saveFlowRecord(ticket, TicketEventEnum.RESUME, fromStatus.getCode(), toStatus.getCode(),
                ticket.getHandlerId(), ticket.getHandlerName(), ticket.getHandlerId(), ticket.getHandlerName(),
                operator, dto.getRemark());

        ticketNotifyProducer.sendNotify(ticket.getId(), TicketEventEnum.RESUME.getCode(), "工单已恢复处理");
        log.info("工单恢复成功，ticketId={}，handlerId={}", ticketId, operator.userId());
    }

    /**
     * 用户确认工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmTicket(Long ticketId, TicketHandleDTO dto) {
        Operator operator = getCurrentOperator();
        Ticket ticket = getTicketOrThrow(ticketId);

        if (!ObjectUtil.equal(ticket.getCreatorId(), operator.userId())) {
            throw BizException.of("只有工单创建人可以确认工单");
        }

        TicketStatusEnum fromStatus = TicketStatusEnum.valueOf(ticket.getStatus());
        TicketStatusEnum toStatus = TicketStateMachine.checkAndGetTargetStatus(fromStatus, TicketEventEnum.CONFIRM);

        closeByEvent(ticket, TicketEventEnum.CONFIRM, fromStatus, toStatus, operator, dto.getRemark());
        log.info("用户确认工单成功，ticketId={}，creatorId={}", ticketId, operator.userId());
    }

    /**
     * 关闭工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTicket(Long ticketId, TicketHandleDTO dto) {
        Operator operator = getCurrentOperator();
        Ticket ticket = getTicketOrThrow(ticketId);

        TicketStatusEnum fromStatus = TicketStatusEnum.valueOf(ticket.getStatus());
        TicketStatusEnum toStatus = TicketStateMachine.checkAndGetTargetStatus(fromStatus, TicketEventEnum.CLOSE);

        closeByEvent(ticket, TicketEventEnum.CLOSE, fromStatus, toStatus, operator, dto.getRemark());
        log.info("关闭工单成功，ticketId={}，operatorId={}", ticketId, operator.userId());
    }

    /**
     * 自动派单
     *
     * @param ticket   工单
     * @param operator 操作人
     */
    private void autoAssign(Ticket ticket, Operator operator) {
        AssignResult assignResult = ticketAssignService.assign(ticket);

        TicketStatusEnum fromStatus = TicketStatusEnum.ASSIGNING;
        TicketStatusEnum toStatus = TicketStateMachine.checkAndGetTargetStatus(fromStatus, TicketEventEnum.ASSIGN);

        LocalDateTime now = LocalDateTime.now();
        boolean success = lambdaUpdate()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getStatus, fromStatus.getCode())
                .eq(Ticket::getVersion, ticket.getVersion())
                .set(Ticket::getStatus, toStatus.getCode())
                .set(Ticket::getHandlerId, assignResult.getHandlerId())
                .set(Ticket::getHandlerName, assignResult.getHandlerName())
                .set(Ticket::getServiceGroupId, assignResult.getServiceGroupId())
                .set(Ticket::getServiceGroupName, assignResult.getServiceGroupName())
                .set(Ticket::getUpdateTime, now)
                .set(Ticket::getVersion, ticket.getVersion() + 1)
                .update();

        checkUpdateSuccess(success);

        saveFlowRecord(ticket, TicketEventEnum.ASSIGN, fromStatus.getCode(), toStatus.getCode(),
                null, null, assignResult.getHandlerId(), assignResult.getHandlerName(),
                operator, "系统自动派单");

        ticket.setStatus(toStatus.getCode());
        ticket.setHandlerId(assignResult.getHandlerId());
        ticket.setHandlerName(assignResult.getHandlerName());
        ticket.setServiceGroupId(assignResult.getServiceGroupId());
        ticket.setServiceGroupName(assignResult.getServiceGroupName());
        ticket.setVersion(ticket.getVersion() + 1);

        ticketNotifyProducer.sendNotify(ticket.getId(), TicketEventEnum.ASSIGN.getCode(), "工单已分派给：" + assignResult.getHandlerName());
        log.info("工单自动派单成功，ticketId={}，handlerId={}", ticket.getId(), assignResult.getHandlerId());
    }

    /**
     * 根据事件关闭工单
     *
     * @param ticket     工单
     * @param event      事件
     * @param fromStatus 来源状态
     * @param toStatus   目标状态
     * @param operator   操作人
     * @param remark     说明
     */
    private void closeByEvent(Ticket ticket, TicketEventEnum event, TicketStatusEnum fromStatus,
                              TicketStatusEnum toStatus, Operator operator, String remark) {
        LocalDateTime now = LocalDateTime.now();
        boolean success = lambdaUpdate()
                .eq(Ticket::getId, ticket.getId())
                .eq(Ticket::getStatus, fromStatus.getCode())
                .eq(Ticket::getVersion, ticket.getVersion())
                .set(Ticket::getStatus, toStatus.getCode())
                .set(Ticket::getCloseTime, now)
                .set(Ticket::getUpdateTime, now)
                .set(Ticket::getVersion, ticket.getVersion() + 1)
                .update();

        checkUpdateSuccess(success);

        saveFlowRecord(ticket, event, fromStatus.getCode(), toStatus.getCode(),
                ticket.getHandlerId(), ticket.getHandlerName(), ticket.getHandlerId(), ticket.getHandlerName(),
                operator, remark);

        ticketNotifyProducer.sendNotify(ticket.getId(), event.getCode(), "工单已关闭");
    }

    /**
     * 查询工单，不存在则抛出异常
     *
     * @param ticketId 工单ID
     * @return 工单
     */
    private Ticket getTicketOrThrow(Long ticketId) {
        Ticket ticket = baseMapper.selectById(ticketId);
        if (ObjectUtil.isNull(ticket)) {
            throw BizException.of("工单不存在");
        }
        return ticket;
    }

    /**
     * 校验当前操作人是否为处理人
     *
     * @param ticket   工单
     * @param operator 当前用户ID
     */
    private void checkHandler(Ticket ticket, Long operator) {
        if (!ObjectUtil.equal(ticket.getHandlerId(), operator)) {
            throw BizException.of("当前用户不是工单处理人，不能执行该操作");
        }
    }

    /**
     * 校验更新结果
     *
     * @param success 是否成功
     */
    private void checkUpdateSuccess(boolean success) {
        if (!success) {
            throw BizException.of("工单状态已变化，请刷新后重试");
        }
    }

    /**
     * 保存流转记录
     *
     * @param ticket          工单
     * @param event           事件
     * @param fromStatus      原状态
     * @param toStatus        新状态
     * @param fromHandlerId   原处理人ID
     * @param fromHandlerName 原处理人名称
     * @param toHandlerId     新处理人ID
     * @param toHandlerName   新处理人名称
     * @param operator        操作人
     * @param remark          说明
     */
    private void saveFlowRecord(Ticket ticket, TicketEventEnum event, String fromStatus, String toStatus,
                                Long fromHandlerId, String fromHandlerName,
                                Long toHandlerId, String toHandlerName,
                                Operator operator, String remark) {
        TicketFlowRecord record = TicketFlowRecord.builder()
                .ticketId(ticket.getId())
                .ticketNo(ticket.getTicketNo())
                .event(event.getCode())
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .fromHandlerId(fromHandlerId)
                .fromHandlerName(fromHandlerName)
                .toHandlerId(toHandlerId)
                .toHandlerName(toHandlerName)
                .operatorId(operator.userId())
                .operatorName(operator.userName())
                .remark(StrUtil.blankToDefault(remark, event.getDesc()))
                .createTime(LocalDateTime.now())
                .build();

        ticketFlowRecordService.save(record);
    }

    /**
     * 生成工单编号
     *
     * @return 工单编号
     */
    private String generateTicketNo() {
        return "TK" + DateUtil.format(new Date(), "yyyyMMdd") + IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * 获取当前登录用户
     *
     * @return 当前操作人
     */
    private Operator getCurrentOperator() {
        Long userId = StpUtil.getLoginIdAsLong();
        String userName = ObjectUtil.defaultIfNull(StpUtil.getSession().getString("userName"), "用户" + userId);
        return new Operator(userId, userName);
    }

    /**
     * 当前操作人
     *
     * @param userId   用户ID
     * @param userName 用户名称
     */
    private record Operator(Long userId, String userName) {
    }
}
```

这部分实现后，工单核心流转已经具备以下能力：

```text
1. 创建工单后自动计算 SLA 时间。
2. 创建工单后自动派单。
3. 每次状态变化都通过状态机校验。
4. 每次状态变化都写入流转记录。
5. 接单、处理、转交、升级、挂起、恢复、确认、关闭都有独立入口。
6. 使用状态 + version 做并发更新控制，避免重复接单、重复处理、重复关闭。
7. 通知逻辑通过 TicketNotifyProducer 解耦，后续可以接 RabbitMQ、站内信、WebSocket。
```

这一节先完成工单主流程。下一节继续实现 `SLA 超时处理`，包括 SLA 即将超时扫描、处理超时扫描、超时提醒、自动升级和定时任务幂等控制。

## SLA 超时处理

SLA 处理要解决两个问题：一是工单创建时就明确“首次响应截止时间”和“处理完成截止时间”；二是定时任务重复扫描时，不能重复提醒、重复升级或重复写入超时记录。该部分对应原 README 中“工单状态机、SLA 计时、超时升级、消息通知”等核心难点。

### SLA 到期时间计算

SLA 到期时间在创建工单时计算，不建议等定时任务扫描时再计算。因为后续查询、排序、提醒、升级都依赖这两个字段：

```text
response_deadline：首次响应截止时间
handle_deadline：处理完成截止时间
```

创建工单时按以下规则计算：

```text
response_deadline = 工单创建时间 + SLA 规则的 response_minutes
handle_deadline   = 工单创建时间 + SLA 规则的 handle_minutes
```

如果需要支持工作日、节假日、夜间暂停 SLA，可以把这里的 `plusMinutes` 替换为“工作时间计算器”。本案例先实现自然时间 SLA，便于直接落地。

先给 `TicketEventEnum` 补充两个 SLA 事件，用于流转记录和通知消息。

文件位置：`src/main/java/io/github/atengk/ticket/common/enums/TicketEventEnum.java`

在原枚举中补充以下两个枚举项：

```java
/**
 * SLA 超时提醒
 */
SLA_REMIND("SLA_REMIND", "SLA 超时提醒"),

/**
 * SLA 超时升级
 */
SLA_UPGRADE("SLA_UPGRADE", "SLA 超时升级");
```

文件位置：`src/main/java/io/github/atengk/ticket/service/TicketSlaService.java`

下面代码定义 SLA 服务接口，包含创建时计算截止时间、响应超时标记、即将超时提醒和处理超时升级。

```java
package io.github.atengk.ticket.service;

import io.github.atengk.ticket.entity.Ticket;

/**
 * 工单 SLA Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface TicketSlaService {

    /**
     * 填充 SLA 截止时间
     *
     * @param ticket 工单
     */
    void fillSlaDeadline(Ticket ticket);

    /**
     * 标记首次响应超时
     *
     * @return 处理数量
     */
    int markResponseTimeout();

    /**
     * 扫描即将处理超时的工单并发送提醒
     *
     * @return 提醒数量
     */
    int remindWillTimeout();

    /**
     * 扫描处理超时工单并执行升级
     *
     * @return 升级数量
     */
    int upgradeHandleTimeout();
}
```

文件位置：`src/main/java/io/github/atengk/ticket/service/impl/TicketSlaServiceImpl.java`

下面代码实现 SLA 规则匹配、即将超时提醒、首次响应超时标记和处理超时升级。

```java
package io.github.atengk.ticket.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.ticket.common.enums.TicketEventEnum;
import io.github.atengk.ticket.common.enums.TicketStatusEnum;
import io.github.atengk.ticket.common.exception.BizException;
import io.github.atengk.ticket.entity.Ticket;
import io.github.atengk.ticket.entity.TicketFlowRecord;
import io.github.atengk.ticket.entity.TicketSlaRule;
import io.github.atengk.ticket.mapper.TicketMapper;
import io.github.atengk.ticket.mapper.TicketSlaRuleMapper;
import io.github.atengk.ticket.mq.TicketNotifyProducer;
import io.github.atengk.ticket.service.TicketFlowRecordService;
import io.github.atengk.ticket.service.TicketSlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单 SLA Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketSlaServiceImpl implements TicketSlaService {

    private static final Long SYSTEM_OPERATOR_ID = 0L;

    private static final String SYSTEM_OPERATOR_NAME = "系统任务";

    private final TicketMapper ticketMapper;

    private final TicketSlaRuleMapper ticketSlaRuleMapper;

    private final TicketFlowRecordService ticketFlowRecordService;

    private final TicketNotifyProducer ticketNotifyProducer;

    /**
     * 填充 SLA 截止时间
     *
     * @param ticket 工单
     */
    @Override
    public void fillSlaDeadline(Ticket ticket) {
        TicketSlaRule rule = getEnabledRule(ticket.getTicketType(), ticket.getPriority());
        LocalDateTime now = LocalDateTime.now();

        ticket.setResponseDeadline(now.plusMinutes(rule.getResponseMinutes()));
        ticket.setHandleDeadline(now.plusMinutes(rule.getHandleMinutes()));

        log.info("工单匹配 SLA 规则成功，ticketType={}，priority={}，responseDeadline={}，handleDeadline={}",
                ticket.getTicketType(), ticket.getPriority(), ticket.getResponseDeadline(), ticket.getHandleDeadline());
    }

    /**
     * 标记首次响应超时
     *
     * @return 处理数量
     */
    @Override
    public int markResponseTimeout() {
        LocalDateTime now = LocalDateTime.now();

        List<Ticket> tickets = ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>()
                        .eq(Ticket::getStatus, TicketStatusEnum.WAIT_ACCEPT.getCode())
                        .eq(Ticket::getResponseTimeout, 0)
                        .le(Ticket::getResponseDeadline, now)
                        .last("LIMIT 200")
        );

        if (CollUtil.isEmpty(tickets)) {
            return 0;
        }

        int count = 0;
        for (Ticket ticket : tickets) {
            int updated = ticketMapper.update(null,
                    new LambdaUpdateWrapper<Ticket>()
                            .eq(Ticket::getId, ticket.getId())
                            .eq(Ticket::getResponseTimeout, 0)
                            .set(Ticket::getResponseTimeout, 1)
                            .set(Ticket::getUpdateTime, now)
            );

            if (updated > 0) {
                count++;
                saveSystemFlow(ticket, TicketEventEnum.SLA_REMIND,
                        ticket.getStatus(), ticket.getStatus(),
                        ticket.getHandlerId(), ticket.getHandlerName(),
                        ticket.getHandlerId(), ticket.getHandlerName(),
                        "工单首次响应已超时");

                ticketNotifyProducer.sendNotify(
                        ticket.getId(),
                        TicketEventEnum.SLA_REMIND.getCode(),
                        "工单首次响应已超时，请尽快接单处理"
                );

                log.info("标记工单首次响应超时成功，ticketId={}，ticketNo={}", ticket.getId(), ticket.getTicketNo());
            }
        }

        return count;
    }

    /**
     * 扫描即将处理超时的工单并发送提醒
     *
     * @return 提醒数量
     */
    @Override
    public int remindWillTimeout() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxScanTime = now.plusHours(1);

        List<Ticket> tickets = ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>()
                        .in(Ticket::getStatus, TicketStatusEnum.WAIT_ACCEPT.getCode(), TicketStatusEnum.PROCESSING.getCode())
                        .eq(Ticket::getRemindFlag, 0)
                        .eq(Ticket::getHandleTimeout, 0)
                        .le(Ticket::getHandleDeadline, maxScanTime)
                        .last("LIMIT 200")
        );

        if (CollUtil.isEmpty(tickets)) {
            return 0;
        }

        int count = 0;
        for (Ticket ticket : tickets) {
            TicketSlaRule rule = getEnabledRule(ticket.getTicketType(), ticket.getPriority());
            LocalDateTime remindTime = ticket.getHandleDeadline().minusMinutes(rule.getRemindBeforeMinutes());

            if (now.isBefore(remindTime)) {
                continue;
            }

            int updated = ticketMapper.update(null,
                    new LambdaUpdateWrapper<Ticket>()
                            .eq(Ticket::getId, ticket.getId())
                            .eq(Ticket::getRemindFlag, 0)
                            .eq(Ticket::getHandleTimeout, 0)
                            .set(Ticket::getRemindFlag, 1)
                            .set(Ticket::getUpdateTime, now)
            );

            if (updated > 0) {
                count++;
                saveSystemFlow(ticket, TicketEventEnum.SLA_REMIND,
                        ticket.getStatus(), ticket.getStatus(),
                        ticket.getHandlerId(), ticket.getHandlerName(),
                        ticket.getHandlerId(), ticket.getHandlerName(),
                        "工单即将处理超时");

                ticketNotifyProducer.sendNotify(
                        ticket.getId(),
                        TicketEventEnum.SLA_REMIND.getCode(),
                        "工单即将处理超时，请及时处理"
                );

                log.info("发送工单 SLA 即将超时提醒成功，ticketId={}，ticketNo={}", ticket.getId(), ticket.getTicketNo());
            }
        }

        return count;
    }

    /**
     * 扫描处理超时工单并执行升级
     *
     * @return 升级数量
     */
    @Override
    public int upgradeHandleTimeout() {
        LocalDateTime now = LocalDateTime.now();

        List<Ticket> tickets = ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>()
                        .in(Ticket::getStatus, TicketStatusEnum.WAIT_ACCEPT.getCode(), TicketStatusEnum.PROCESSING.getCode())
                        .eq(Ticket::getHandleTimeout, 0)
                        .eq(Ticket::getUpgradeFlag, 0)
                        .le(Ticket::getHandleDeadline, now)
                        .last("LIMIT 200")
        );

        if (CollUtil.isEmpty(tickets)) {
            return 0;
        }

        int count = 0;
        for (Ticket ticket : tickets) {
            TicketSlaRule rule = getEnabledRule(ticket.getTicketType(), ticket.getPriority());

            Long toHandlerId = ObjectUtil.defaultIfNull(rule.getUpgradeHandlerId(), ticket.getHandlerId());
            String toHandlerName = ObjectUtil.defaultIfNull(rule.getUpgradeHandlerName(), ticket.getHandlerName());

            int updated = ticketMapper.update(null,
                    new LambdaUpdateWrapper<Ticket>()
                            .eq(Ticket::getId, ticket.getId())
                            .eq(Ticket::getHandleTimeout, 0)
                            .eq(Ticket::getUpgradeFlag, 0)
                            .set(Ticket::getHandleTimeout, 1)
                            .set(Ticket::getUpgradeFlag, 1)
                            .set(Ticket::getHandlerId, toHandlerId)
                            .set(Ticket::getHandlerName, toHandlerName)
                            .set(Ticket::getUpdateTime, now)
            );

            if (updated > 0) {
                count++;
                saveSystemFlow(ticket, TicketEventEnum.SLA_UPGRADE,
                        ticket.getStatus(), ticket.getStatus(),
                        ticket.getHandlerId(), ticket.getHandlerName(),
                        toHandlerId, toHandlerName,
                        "工单处理已超时，系统自动升级");

                ticketNotifyProducer.sendNotify(
                        ticket.getId(),
                        TicketEventEnum.SLA_UPGRADE.getCode(),
                        "工单处理已超时，已自动升级给：" + toHandlerName
                );

                log.info("工单 SLA 超时升级成功，ticketId={}，fromHandlerId={}，toHandlerId={}",
                        ticket.getId(), ticket.getHandlerId(), toHandlerId);
            }
        }

        return count;
    }

    /**
     * 查询启用的 SLA 规则
     *
     * @param ticketType 工单类型
     * @param priority   优先级
     * @return SLA 规则
     */
    private TicketSlaRule getEnabledRule(String ticketType, String priority) {
        TicketSlaRule rule = ticketSlaRuleMapper.selectOne(
                new LambdaQueryWrapper<TicketSlaRule>()
                        .eq(TicketSlaRule::getTicketType, ticketType)
                        .eq(TicketSlaRule::getPriority, priority)
                        .eq(TicketSlaRule::getEnableFlag, 1)
                        .last("LIMIT 1")
        );

        if (ObjectUtil.isNull(rule)) {
            throw BizException.of("未匹配到可用的 SLA 规则");
        }

        return rule;
    }

    /**
     * 保存系统流转记录
     *
     * @param ticket          工单
     * @param event           事件
     * @param fromStatus      原状态
     * @param toStatus        新状态
     * @param fromHandlerId   原处理人ID
     * @param fromHandlerName 原处理人名称
     * @param toHandlerId     新处理人ID
     * @param toHandlerName   新处理人名称
     * @param remark          说明
     */
    private void saveSystemFlow(Ticket ticket, TicketEventEnum event,
                                String fromStatus, String toStatus,
                                Long fromHandlerId, String fromHandlerName,
                                Long toHandlerId, String toHandlerName,
                                String remark) {
        TicketFlowRecord record = TicketFlowRecord.builder()
                .ticketId(ticket.getId())
                .ticketNo(ticket.getTicketNo())
                .event(event.getCode())
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .fromHandlerId(fromHandlerId)
                .fromHandlerName(fromHandlerName)
                .toHandlerId(toHandlerId)
                .toHandlerName(toHandlerName)
                .operatorId(SYSTEM_OPERATOR_ID)
                .operatorName(SYSTEM_OPERATOR_NAME)
                .remark(remark)
                .createTime(LocalDateTime.now())
                .build();

        ticketFlowRecordService.save(record);
    }
}
```

这里的幂等控制点是：

```text
首次响应超时：response_timeout = 0 才允许更新
即将超时提醒：remind_flag = 0 才允许更新
处理超时升级：handle_timeout = 0 且 upgrade_flag = 0 才允许更新
```

即使 XXL-JOB 重复触发，数据库更新条件也会拦截重复提醒和重复升级。

### XXL-JOB 超时扫描任务

XXL-JOB 负责定时调用 SLA 扫描逻辑，建议 1 分钟执行一次。扫描任务本身只做调度，不写复杂业务，具体业务放在 `TicketSlaService` 中。

文件位置：`src/main/java/io/github/atengk/ticket/job/TicketSlaJob.java`

下面代码定义 XXL-JOB 任务入口，依次扫描首次响应超时、即将处理超时、处理超时升级。

```java
package io.github.atengk.ticket.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.ticket.service.TicketSlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工单 SLA 定时任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketSlaJob {

    private final TicketSlaService ticketSlaService;

    /**
     * 工单 SLA 超时扫描任务
     */
    @XxlJob("ticketSlaTimeoutJob")
    public void ticketSlaTimeoutJob() {
        log.info("开始执行工单 SLA 超时扫描任务");

        int responseTimeoutCount = ticketSlaService.markResponseTimeout();
        int remindCount = ticketSlaService.remindWillTimeout();
        int upgradeCount = ticketSlaService.upgradeHandleTimeout();

        log.info("工单 SLA 超时扫描任务执行完成，responseTimeoutCount={}，remindCount={}，upgradeCount={}",
                responseTimeoutCount, remindCount, upgradeCount);
    }
}
```

XXL-JOB 后台配置：

```text
JobHandler：ticketSlaTimeoutJob
调度类型：CRON
CRON：0 0/1 * * * ?
路由策略：轮询
阻塞处理策略：单机串行
失败重试次数：3
```

如果服务是多实例部署，建议：

```text
1. XXL-JOB 路由策略选择“轮询”或“故障转移”。
2. 阻塞策略使用“单机串行”。
3. 业务层必须保留 remind_flag、upgrade_flag 这类幂等字段。
4. 如果扫描量很大，可以按 ticket_id 分片或按服务组分片扫描。
```

### 超时提醒

超时提醒的触发条件是：

```text
当前时间 >= handle_deadline - remind_before_minutes
并且 remind_flag = 0
并且 handle_timeout = 0
并且工单状态仍在 WAIT_ACCEPT 或 PROCESSING
```

提醒不改变工单状态，只做三件事：

```text
1. 更新 remind_flag = 1。
2. 写入 SLA_REMIND 流转记录。
3. 发送 RabbitMQ 通知消息。
```

示例：

```text
售后紧急工单：
handle_deadline = 2026-05-15 10:30:00
remind_before_minutes = 10
则 2026-05-15 10:20:00 开始允许发送提醒。
```

提醒记录落库后，可以在工单详情中展示：

```text
2026-05-15 10:20:00 系统任务 触发 SLA 超时提醒：工单即将处理超时
```

### 超时升级

超时升级的触发条件是：

```text
当前时间 >= handle_deadline
并且 handle_timeout = 0
并且 upgrade_flag = 0
并且工单状态仍在 WAIT_ACCEPT 或 PROCESSING
```

升级后不强制改变工单状态，只变更处理人并写入记录：

```text
原处理人：客服处理人
新处理人：售后主管
事件：SLA_UPGRADE
状态：PROCESSING -> PROCESSING
说明：工单处理已超时，系统自动升级
```

这样设计有两个好处：

```text
1. 升级只改变责任人，不破坏原有状态机。
2. WAIT_ACCEPT 状态下也可以升级处理人，让主管重新接管未响应工单。
```

如果业务要求“超时后必须进入升级中状态”，可以增加一个 `ESCALATED` 状态。但多数项目中不建议过早增加状态，容易让流转复杂化。

## 消息通知与协作

消息通知负责把工单状态变化、SLA 提醒、SLA 升级推送给相关用户。这里使用 RabbitMQ 做异步解耦，WebSocket 只负责在线实时推送。离线用户可以通过站内信表扩展，本案例先保留消息入口。

### RabbitMQ 通知消息

RabbitMQ 的职责是削峰和解耦。工单主流程只发送消息，不直接关心用户是否在线、WebSocket 是否连接、短信是否发送成功。

文件位置：`src/main/java/io/github/atengk/ticket/config/TicketRabbitConfig.java`

下面代码定义工单通知交换机、队列和路由键。

```java
package io.github.atengk.ticket.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工单 RabbitMQ 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class TicketRabbitConfig {

    public static final String TICKET_EXCHANGE = "ticket.exchange";

    public static final String TICKET_NOTIFY_QUEUE = "ticket.notify.queue";

    public static final String TICKET_NOTIFY_ROUTING_KEY = "ticket.notify";

    /**
     * 工单交换机
     *
     * @return Direct 交换机
     */
    @Bean
    public DirectExchange ticketExchange() {
        return new DirectExchange(TICKET_EXCHANGE, true, false);
    }

    /**
     * 工单通知队列
     *
     * @return 通知队列
     */
    @Bean
    public Queue ticketNotifyQueue() {
        return new Queue(TICKET_NOTIFY_QUEUE, true);
    }

    /**
     * 工单通知绑定关系
     *
     * @return 绑定关系
     */
    @Bean
    public Binding ticketNotifyBinding() {
        return BindingBuilder.bind(ticketNotifyQueue())
                .to(ticketExchange())
                .with(TICKET_NOTIFY_ROUTING_KEY);
    }
}
```

文件位置：`src/main/java/io/github/atengk/ticket/mq/TicketNotifyMessage.java`

下面代码定义工单通知消息体，后续可扩展站内信、短信、邮件、企业微信等渠道字段。

```java
package io.github.atengk.ticket.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工单通知消息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketNotifyMessage {

    private String messageId;

    private Long ticketId;

    private String event;

    private String content;

    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/mq/TicketNotifyProducer.java`

下面代码将上一节的日志版通知生产者替换为 RabbitMQ 版本。

```java
package io.github.atengk.ticket.mq;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.ticket.config.TicketRabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 工单通知消息生产者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketNotifyProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送工单通知
     *
     * @param ticketId 工单ID
     * @param event    事件
     * @param content  内容
     */
    public void sendNotify(Long ticketId, String event, String content) {
        TicketNotifyMessage message = TicketNotifyMessage.builder()
                .messageId(IdUtil.fastSimpleUUID())
                .ticketId(ticketId)
                .event(event)
                .content(content)
                .createTime(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                TicketRabbitConfig.TICKET_EXCHANGE,
                TicketRabbitConfig.TICKET_NOTIFY_ROUTING_KEY,
                JSONUtil.toJsonStr(message)
        );

        log.info("发送工单通知消息成功，ticketId={}，event={}，messageId={}",
                ticketId, event, message.getMessageId());
    }
}
```

文件位置：`src/main/java/io/github/atengk/ticket/mq/TicketNotifyConsumer.java`

下面代码消费工单通知消息，并转发到 WebSocket 推送服务。

```java
package io.github.atengk.ticket.mq;

import cn.hutool.json.JSONUtil;
import io.github.atengk.ticket.config.TicketRabbitConfig;
import io.github.atengk.ticket.service.TicketWebSocketPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 工单通知消息消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketNotifyConsumer {

    private final TicketWebSocketPushService ticketWebSocketPushService;

    /**
     * 消费工单通知消息
     *
     * @param body 消息内容
     */
    @RabbitListener(queues = TicketRabbitConfig.TICKET_NOTIFY_QUEUE)
    public void consumeNotify(String body) {
        TicketNotifyMessage message = JSONUtil.toBean(body, TicketNotifyMessage.class);

        log.info("消费工单通知消息，ticketId={}，event={}，messageId={}",
                message.getTicketId(), message.getEvent(), message.getMessageId());

        // 本案例先广播给在线用户；实际项目应根据工单创建人、处理人、服务组成员精准推送
        ticketWebSocketPushService.broadcast(JSONUtil.toJsonStr(message));
    }
}
```

### WebSocket 实时推送

WebSocket 适合做“在线实时提醒”，不适合替代消息持久化。实际项目建议：

```text
RabbitMQ：保证通知事件异步流转
站内信表：保证离线消息可查询
WebSocket：在线用户实时收到提醒
```

文件位置：`src/main/java/io/github/atengk/ticket/service/TicketWebSocketPushService.java`

下面代码定义 WebSocket 推送服务接口。

```java
package io.github.atengk.ticket.service;

/**
 * 工单 WebSocket 推送 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface TicketWebSocketPushService {

    /**
     * 推送给指定用户
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    void sendToUser(Long userId, String message);

    /**
     * 广播消息
     *
     * @param message 消息内容
     */
    void broadcast(String message);
}
```

文件位置：`src/main/java/io/github/atengk/ticket/websocket/TicketWebSocketHandler.java`

下面代码实现工单 WebSocket 连接管理和消息推送。

```java
package io.github.atengk.ticket.websocket;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ticket.service.TicketWebSocketPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工单 WebSocket 处理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class TicketWebSocketHandler extends TextWebSocketHandler implements TicketWebSocketPushService {

    private static final Map<Long, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 建立连接
     *
     * @param session WebSocket 会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (ObjectUtil.isNull(userId)) {
            log.warn("WebSocket 连接缺少用户ID，sessionId={}", session.getId());
            return;
        }

        SESSION_MAP.put(userId, session);
        log.info("工单 WebSocket 连接成功，userId={}，sessionId={}", userId, session.getId());
    }

    /**
     * 连接关闭
     *
     * @param session WebSocket 会话
     * @param status  关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (ObjectUtil.isNotNull(userId)) {
            SESSION_MAP.remove(userId);
        }

        log.info("工单 WebSocket 连接关闭，userId={}，sessionId={}，status={}",
                userId, session.getId(), status.getCode());
    }

    /**
     * 推送给指定用户
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    @Override
    public void sendToUser(Long userId, String message) {
        WebSocketSession session = SESSION_MAP.get(userId);
        if (ObjectUtil.isNull(session) || !session.isOpen()) {
            log.info("用户不在线，跳过 WebSocket 推送，userId={}", userId);
            return;
        }

        try {
            session.sendMessage(new TextMessage(message));
            log.info("WebSocket 推送成功，userId={}", userId);
        } catch (Exception e) {
            log.error("WebSocket 推送失败，userId={}", userId, e);
        }
    }

    /**
     * 广播消息
     *
     * @param message 消息内容
     */
    @Override
    public void broadcast(String message) {
        SESSION_MAP.forEach((userId, session) -> {
            if (ObjectUtil.isNotNull(session) && session.isOpen()) {
                sendToUser(userId, message);
            }
        });
    }

    /**
     * 获取用户ID
     *
     * @param session WebSocket 会话
     * @return 用户ID
     */
    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (ObjectUtil.isNull(userId) || StrUtil.isBlank(String.valueOf(userId))) {
            return null;
        }
        return Long.valueOf(String.valueOf(userId));
    }
}
```

文件位置：`src/main/java/io/github/atengk/ticket/config/TicketWebSocketConfig.java`

下面代码注册 WebSocket 地址，并从连接参数中提取 `userId`。

```java
package io.github.atengk.ticket.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.ticket.websocket.TicketWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 工单 WebSocket 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class TicketWebSocketConfig implements WebSocketConfigurer {

    private final TicketWebSocketHandler ticketWebSocketHandler;

    /**
     * 注册 WebSocket 处理器
     *
     * @param registry 注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(ticketWebSocketHandler, "/ws/ticket")
                .addInterceptors(new TicketHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }

    /**
     * 工单 WebSocket 握手拦截器
     *
     * @author Ateng
     * @since 2026-05-15
     */
    public static class TicketHandshakeInterceptor implements HandshakeInterceptor {

        /**
         * 握手前处理
         *
         * @param request    请求
         * @param response   响应
         * @param wsHandler  处理器
         * @param attributes 属性
         * @return 是否允许连接
         */
        @Override
        public boolean beforeHandshake(ServerHttpRequest request,
                                       org.springframework.http.server.ServerHttpResponse response,
                                       WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
            String userId = UriComponentsBuilder.fromUri(request.getURI())
                    .build()
                    .getQueryParams()
                    .getFirst("userId");

            if (StrUtil.isBlank(userId)) {
                return false;
            }

            attributes.put("userId", userId);
            return true;
        }

        /**
         * 握手后处理
         *
         * @param request   请求
         * @param response  响应
         * @param wsHandler 处理器
         * @param exception 异常
         */
        @Override
        public void afterHandshake(ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Exception exception) {
            // 无需处理
        }
    }
}
```

前端连接示例：

```javascript
const socket = new WebSocket('ws://localhost:8080/ws/ticket?userId=1001')

socket.onmessage = function (event) {
  console.log('收到工单消息：', event.data)
}
```

单体项目这样即可使用。多节点部署时，WebSocket 连接分散在不同实例，需要用 Redis Pub/Sub、RabbitMQ 广播或专门的推送网关解决跨节点推送。

### 评论与处理记录留痕

评论用于协作，流转记录用于审计。两者不要混用：

```text
评论：用户或处理人的交流内容，不改变状态。
流转记录：系统记录状态、处理人、操作人的变化，不能随意删除。
```

文件位置：`src/main/java/io/github/atengk/ticket/service/TicketCommentService.java`

下面代码定义工单评论 Service。

```java
package io.github.atengk.ticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.ticket.entity.TicketComment;

/**
 * 工单评论 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface TicketCommentService extends IService<TicketComment> {

    /**
     * 添加评论
     *
     * @param ticketId     工单ID
     * @param content      评论内容
     * @param internalFlag 是否内部评论
     */
    void addComment(Long ticketId, String content, Integer internalFlag);
}
```

文件位置：`src/main/java/io/github/atengk/ticket/service/impl/TicketCommentServiceImpl.java`

下面代码实现工单评论保存逻辑，评论保存前会校验工单是否存在。

```java
package io.github.atengk.ticket.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.ticket.common.exception.BizException;
import io.github.atengk.ticket.entity.Ticket;
import io.github.atengk.ticket.entity.TicketComment;
import io.github.atengk.ticket.mapper.TicketCommentMapper;
import io.github.atengk.ticket.mapper.TicketMapper;
import io.github.atengk.ticket.service.TicketCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 工单评论 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCommentServiceImpl extends ServiceImpl<TicketCommentMapper, TicketComment>
        implements TicketCommentService {

    private final TicketMapper ticketMapper;

    /**
     * 添加评论
     *
     * @param ticketId     工单ID
     * @param content      评论内容
     * @param internalFlag 是否内部评论
     */
    @Override
    public void addComment(Long ticketId, String content, Integer internalFlag) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ObjectUtil.isNull(ticket)) {
            throw BizException.of("工单不存在");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        String userName = ObjectUtil.defaultIfNull(StpUtil.getSession().getString("userName"), "用户" + userId);
        LocalDateTime now = LocalDateTime.now();

        TicketComment comment = TicketComment.builder()
                .ticketId(ticket.getId())
                .ticketNo(ticket.getTicketNo())
                .userId(userId)
                .userName(userName)
                .content(content)
                .internalFlag(ObjectUtil.defaultIfNull(internalFlag, 0))
                .deleted(0)
                .createTime(now)
                .updateTime(now)
                .build();

        save(comment);

        log.info("添加工单评论成功，ticketId={}，userId={}，internalFlag={}",
                ticketId, userId, comment.getInternalFlag());
    }
}
```

处理记录留痕已经在前面的 `TicketServiceImpl.saveFlowRecord(...)` 和本节的 `TicketSlaServiceImpl.saveSystemFlow(...)` 中实现。推荐遵循以下规则：

```text
1. 所有状态变化必须写入 t_ticket_flow_record。
2. 所有处理人变化必须写入 from_handler_id 和 to_handler_id。
3. 系统任务操作人固定为 0 / 系统任务。
4. 用户评论只写 t_ticket_comment，不写流转记录。
5. SLA 提醒和 SLA 升级要写流转记录，便于追责和排查。
6. 流转记录不建议物理删除。
```

最终，工单详情页可以组合展示三类信息：

```text
工单主信息：t_ticket
流转时间线：t_ticket_flow_record
协作评论：t_ticket_comment
```

典型时间线效果：

```text
2026-05-15 10:00:00 用户1001 创建工单
2026-05-15 10:00:01 系统自动派单给 客服处理人
2026-05-15 10:02:00 客服处理人 接单
2026-05-15 10:20:00 系统任务 触发 SLA 超时提醒
2026-05-15 10:31:00 系统任务 触发 SLA 超时升级，转给 售后主管
2026-05-15 10:45:00 售后主管 处理完成，等待用户确认
2026-05-15 11:00:00 用户1001 确认关闭工单
```

## 接口设计

本节补齐 Controller、查询 DTO/VO、统一返回对象和接口调用示例。接口围绕工单创建、工单流转、工单查询、SLA 任务手动触发设计，对应 README 中“工单状态机、处理人变更、SLA 超时提醒和升级、处理记录、评论协作、消息通知”等核心点。

### 工单创建接口

工单创建接口用于用户提交工单。后端创建成功后会自动计算 SLA 截止时间、自动派单、写入创建记录和派单记录。

文件位置：`src/main/java/io/github/atengk/ticket/common/result/R.java`

下面代码定义统一接口返回对象，后续 Controller 统一返回该结构。

```java
package io.github.atengk.ticket.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 成功响应
     *
     * @param data 数据
     * @return 响应结果
     */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data);
    }

    /**
     * 成功响应
     *
     * @return 响应结果
     */
    public static R<Void> ok() {
        return new R<>(200, "操作成功", null);
    }

    /**
     * 失败响应
     *
     * @param message 失败信息
     * @return 响应结果
     */
    public static R<Void> fail(String message) {
        return new R<>(500, message, null);
    }
}
```

文件位置：`src/main/java/io/github/atengk/ticket/dto/TicketQueryDTO.java`

下面代码定义工单分页查询参数，支持按状态、类型、优先级、处理人和关键字查询。

```java
package io.github.atengk.ticket.dto;

import lombok.Data;

/**
 * 工单分页查询参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class TicketQueryDTO {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String ticketNo;

    private String keyword;

    private String ticketType;

    private String priority;

    private String status;

    private Long handlerId;

    private Long creatorId;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/dto/TicketCommentDTO.java`

下面代码定义添加评论参数，`internalFlag=1` 表示内部协作备注。

```java
package io.github.atengk.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工单评论请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class TicketCommentDTO {

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Integer internalFlag = 0;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/vo/TicketVO.java`

下面代码定义工单列表返回对象，避免直接把实体全部字段暴露给前端。

```java
package io.github.atengk.ticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单列表返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class TicketVO {

    private Long id;

    private String ticketNo;

    private String title;

    private String ticketType;

    private String priority;

    private String status;

    private Long creatorId;

    private String creatorName;

    private Long handlerId;

    private String handlerName;

    private String serviceGroupName;

    private LocalDateTime responseDeadline;

    private LocalDateTime handleDeadline;

    private Integer responseTimeout;

    private Integer handleTimeout;

    private Integer remindFlag;

    private Integer upgradeFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/vo/TicketDetailVO.java`

下面代码定义工单详情返回对象，详情页一次性返回工单主信息、流转记录和评论。

```java
package io.github.atengk.ticket.vo;

import io.github.atengk.ticket.entity.TicketComment;
import io.github.atengk.ticket.entity.TicketFlowRecord;
import lombok.Data;

import java.util.List;

/**
 * 工单详情返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class TicketDetailVO {

    private TicketVO ticket;

    private List<TicketFlowRecord> flowRecords;

    private List<TicketComment> comments;
}
```

### 工单流转接口

工单流转接口不允许前端直接传目标状态，而是由前端调用具体动作接口。后端根据动作事件和当前状态，通过状态机计算目标状态。

文件位置：`src/main/java/io/github/atengk/ticket/service/TicketService.java`

在原 `TicketService` 中补充分页查询和详情查询方法。

```java
/**
 * 分页查询工单
 *
 * @param dto 查询参数
 * @return 工单分页
 */
Page<TicketVO> pageTicket(TicketQueryDTO dto);

/**
 * 查询工单详情
 *
 * @param ticketId 工单ID
 * @return 工单详情
 */
TicketDetailVO getTicketDetail(Long ticketId);
```

需要同步补充导入：

```java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.ticket.dto.TicketQueryDTO;
import io.github.atengk.ticket.vo.TicketDetailVO;
import io.github.atengk.ticket.vo.TicketVO;
```

文件位置：`src/main/java/io/github/atengk/ticket/service/impl/TicketServiceImpl.java`

在原 `TicketServiceImpl` 中补充以下依赖字段。

```java
private final TicketFlowRecordMapper ticketFlowRecordMapper;

private final TicketCommentMapper ticketCommentMapper;
```

需要同步补充导入：

```java
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.ticket.dto.TicketQueryDTO;
import io.github.atengk.ticket.entity.TicketComment;
import io.github.atengk.ticket.mapper.TicketCommentMapper;
import io.github.atengk.ticket.mapper.TicketFlowRecordMapper;
import io.github.atengk.ticket.vo.TicketDetailVO;
import io.github.atengk.ticket.vo.TicketVO;
import java.util.List;
```

在原 `TicketServiceImpl` 中补充以下查询方法。

```java
/**
 * 分页查询工单
 *
 * @param dto 查询参数
 * @return 工单分页
 */
@Override
public Page<TicketVO> pageTicket(TicketQueryDTO dto) {
    LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<Ticket>()
            .eq(StrUtil.isNotBlank(dto.getTicketNo()), Ticket::getTicketNo, dto.getTicketNo())
            .like(StrUtil.isNotBlank(dto.getKeyword()), Ticket::getTitle, dto.getKeyword())
            .eq(StrUtil.isNotBlank(dto.getTicketType()), Ticket::getTicketType, dto.getTicketType())
            .eq(StrUtil.isNotBlank(dto.getPriority()), Ticket::getPriority, dto.getPriority())
            .eq(StrUtil.isNotBlank(dto.getStatus()), Ticket::getStatus, dto.getStatus())
            .eq(ObjectUtil.isNotNull(dto.getHandlerId()), Ticket::getHandlerId, dto.getHandlerId())
            .eq(ObjectUtil.isNotNull(dto.getCreatorId()), Ticket::getCreatorId, dto.getCreatorId())
            .orderByDesc(Ticket::getCreateTime);

    Page<Ticket> page = baseMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);

    Page<TicketVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
    List<TicketVO> records = BeanUtil.copyToList(page.getRecords(), TicketVO.class);
    voPage.setRecords(records);

    return voPage;
}

/**
 * 查询工单详情
 *
 * @param ticketId 工单ID
 * @return 工单详情
 */
@Override
public TicketDetailVO getTicketDetail(Long ticketId) {
    Ticket ticket = getTicketOrThrow(ticketId);

    List<TicketFlowRecord> flowRecords = ticketFlowRecordMapper.selectList(
            new LambdaQueryWrapper<TicketFlowRecord>()
                    .eq(TicketFlowRecord::getTicketId, ticketId)
                    .orderByAsc(TicketFlowRecord::getCreateTime)
    );

    List<TicketComment> comments = ticketCommentMapper.selectList(
            new LambdaQueryWrapper<TicketComment>()
                    .eq(TicketComment::getTicketId, ticketId)
                    .orderByAsc(TicketComment::getCreateTime)
    );

    TicketDetailVO detailVO = new TicketDetailVO();
    detailVO.setTicket(BeanUtil.copyProperties(ticket, TicketVO.class));
    detailVO.setFlowRecords(CollUtil.emptyIfNull(flowRecords));
    detailVO.setComments(CollUtil.emptyIfNull(comments));

    return detailVO;
}
```

文件位置：`src/main/java/io/github/atengk/ticket/controller/TicketController.java`

下面代码定义工单创建、流转、查询、评论和 SLA 手动扫描接口。

```java
package io.github.atengk.ticket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.ticket.common.result.R;
import io.github.atengk.ticket.dto.TicketCommentDTO;
import io.github.atengk.ticket.dto.TicketCreateDTO;
import io.github.atengk.ticket.dto.TicketHandleDTO;
import io.github.atengk.ticket.dto.TicketQueryDTO;
import io.github.atengk.ticket.dto.TicketTransferDTO;
import io.github.atengk.ticket.dto.TicketUpgradeDTO;
import io.github.atengk.ticket.service.TicketCommentService;
import io.github.atengk.ticket.service.TicketService;
import io.github.atengk.ticket.service.TicketSlaService;
import io.github.atengk.ticket.vo.TicketDetailVO;
import io.github.atengk.ticket.vo.TicketVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 工单接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    private final TicketCommentService ticketCommentService;

    private final TicketSlaService ticketSlaService;

    /**
     * 创建工单
     *
     * @param dto 创建参数
     * @return 工单ID
     */
    @PostMapping
    public R<Long> createTicket(@Valid @RequestBody TicketCreateDTO dto) {
        return R.ok(ticketService.createTicket(dto));
    }

    /**
     * 分页查询工单
     *
     * @param dto 查询参数
     * @return 工单分页
     */
    @GetMapping
    public R<Page<TicketVO>> pageTicket(TicketQueryDTO dto) {
        return R.ok(ticketService.pageTicket(dto));
    }

    /**
     * 查询工单详情
     *
     * @param ticketId 工单ID
     * @return 工单详情
     */
    @GetMapping("/{ticketId}")
    public R<TicketDetailVO> getTicketDetail(@PathVariable Long ticketId) {
        return R.ok(ticketService.getTicketDetail(ticketId));
    }

    /**
     * 处理人接单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     * @return 响应结果
     */
    @PostMapping("/{ticketId}/accept")
    public R<Void> acceptTicket(@PathVariable Long ticketId, @Valid @RequestBody TicketHandleDTO dto) {
        ticketService.acceptTicket(ticketId, dto);
        return R.ok();
    }

    /**
     * 处理完成工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     * @return 响应结果
     */
    @PostMapping("/{ticketId}/handle")
    public R<Void> handleTicket(@PathVariable Long ticketId, @Valid @RequestBody TicketHandleDTO dto) {
        ticketService.handleTicket(ticketId, dto);
        return R.ok();
    }

    /**
     * 转交工单
     *
     * @param ticketId 工单ID
     * @param dto      转交参数
     * @return 响应结果
     */
    @PostMapping("/{ticketId}/transfer")
    public R<Void> transferTicket(@PathVariable Long ticketId, @Valid @RequestBody TicketTransferDTO dto) {
        ticketService.transferTicket(ticketId, dto);
        return R.ok();
    }

    /**
     * 升级工单
     *
     * @param ticketId 工单ID
     * @param dto      升级参数
     * @return 响应结果
     */
    @PostMapping("/{ticketId}/upgrade")
    public R<Void> upgradeTicket(@PathVariable Long ticketId, @Valid @RequestBody TicketUpgradeDTO dto) {
        ticketService.upgradeTicket(ticketId, dto);
        return R.ok();
    }

    /**
     * 挂起工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     * @return 响应结果
     */
    @PostMapping("/{ticketId}/suspend")
    public R<Void> suspendTicket(@PathVariable Long ticketId, @Valid @RequestBody TicketHandleDTO dto) {
        ticketService.suspendTicket(ticketId, dto);
        return R.ok();
    }

    /**
     * 恢复工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     * @return 响应结果
     */
    @PostMapping("/{ticketId}/resume")
    public R<Void> resumeTicket(@PathVariable Long ticketId, @Valid @RequestBody TicketHandleDTO dto) {
        ticketService.resumeTicket(ticketId, dto);
        return R.ok();
    }

    /**
     * 用户确认工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     * @return 响应结果
     */
    @PostMapping("/{ticketId}/confirm")
    public R<Void> confirmTicket(@PathVariable Long ticketId, @Valid @RequestBody TicketHandleDTO dto) {
        ticketService.confirmTicket(ticketId, dto);
        return R.ok();
    }

    /**
     * 关闭工单
     *
     * @param ticketId 工单ID
     * @param dto      处理参数
     * @return 响应结果
     */
    @PostMapping("/{ticketId}/close")
    public R<Void> closeTicket(@PathVariable Long ticketId, @Valid @RequestBody TicketHandleDTO dto) {
        ticketService.closeTicket(ticketId, dto);
        return R.ok();
    }

    /**
     * 添加工单评论
     *
     * @param ticketId 工单ID
     * @param dto      评论参数
     * @return 响应结果
     */
    @PostMapping("/{ticketId}/comments")
    public R<Void> addComment(@PathVariable Long ticketId, @Valid @RequestBody TicketCommentDTO dto) {
        ticketCommentService.addComment(ticketId, dto.getContent(), dto.getInternalFlag());
        return R.ok();
    }

    /**
     * 手动触发 SLA 扫描
     *
     * @return 扫描结果
     */
    @PostMapping("/sla/scan")
    public R<String> scanSla() {
        int responseTimeoutCount = ticketSlaService.markResponseTimeout();
        int remindCount = ticketSlaService.remindWillTimeout();
        int upgradeCount = ticketSlaService.upgradeHandleTimeout();

        return R.ok("首次响应超时：" + responseTimeoutCount
                + "，即将超时提醒：" + remindCount
                + "，处理超时升级：" + upgradeCount);
    }
}
```

### 工单查询接口

工单查询分为列表查询和详情查询。

列表查询接口：

```text
GET /api/tickets
```

常用查询参数：

| 参数         | 说明          |
| ------------ | ------------- |
| `pageNum`    | 页码          |
| `pageSize`   | 每页条数      |
| `ticketNo`   | 工单编号      |
| `keyword`    | 标题关键字    |
| `ticketType` | 工单类型      |
| `priority`   | 优先级        |
| `status`     | 工单状态      |
| `handlerId`  | 当前处理人 ID |
| `creatorId`  | 创建人 ID     |

调用示例：

```bash
curl -X GET 'http://localhost:8080/api/tickets?pageNum=1&pageSize=10&status=PROCESSING&handlerId=1001' \
  -H 'satoken: your-token'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1850000000000000001,
        "ticketNo": "TK202605151850000000000000001",
        "title": "订单退款失败",
        "ticketType": "AFTER_SALE",
        "priority": "URGENT",
        "status": "PROCESSING",
        "creatorId": 2001,
        "creatorName": "用户2001",
        "handlerId": 1001,
        "handlerName": "客服处理人",
        "serviceGroupName": "客服支持组",
        "responseTimeout": 0,
        "handleTimeout": 0,
        "remindFlag": 0,
        "upgradeFlag": 0
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

详情查询接口：

```text
GET /api/tickets/{ticketId}
```

调用示例：

```bash
curl -X GET 'http://localhost:8080/api/tickets/1850000000000000001' \
  -H 'satoken: your-token'
```

详情接口适合工单详情页展示：

```text
1. ticket：工单当前状态。
2. flowRecords：完整流转时间线。
3. comments：评论协作记录。
```

### SLA 任务接口

SLA 正式环境建议使用 XXL-JOB 触发，不建议暴露公网接口。这里提供手动接口主要用于本地开发、联调和演示。

手动触发接口：

```text
POST /api/tickets/sla/scan
```

调用示例：

```bash
curl -X POST 'http://localhost:8080/api/tickets/sla/scan' \
  -H 'satoken: your-token'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "首次响应超时：1，即将超时提醒：2，处理超时升级：1"
}
```

生产环境建议加权限控制：

```text
1. 只允许管理员调用。
2. 只允许内网调用。
3. 生产环境可直接移除该接口。
4. 正式调度统一使用 XXL-JOB 的 ticketSlaTimeoutJob。
```

## 功能验证

本节给出核心链路验证方式。验证时建议先用数据库初始化 SLA 规则，再启动 MySQL、Redis、RabbitMQ、XXL-JOB 和后端服务。

### 正常流转验证

正常流转路径：

```text
创建工单
-> 自动派单
-> 处理人接单
-> 处理完成
-> 用户确认
-> 工单关闭
```

创建工单：

```bash
curl -X POST 'http://localhost:8080/api/tickets' \
  -H 'Content-Type: application/json' \
  -H 'satoken: creator-token' \
  -d '{
    "title": "订单退款失败",
    "content": "用户申请退款后，订单状态未更新，退款金额未到账",
    "ticketType": "AFTER_SALE",
    "priority": "URGENT"
  }'
```

预期结果：

```text
1. t_ticket 新增一条记录。
2. status 从 ASSIGNING 自动变为 WAIT_ACCEPT。
3. handler_id、handler_name 已自动填充。
4. response_deadline、handle_deadline 已自动填充。
5. t_ticket_flow_record 至少产生 CREATE 和 ASSIGN 两条记录。
```

处理人接单：

```bash
curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/accept' \
  -H 'Content-Type: application/json' \
  -H 'satoken: handler-token' \
  -d '{
    "remark": "已接单，开始核查退款链路"
  }'
```

预期结果：

```text
1. t_ticket.status = PROCESSING。
2. t_ticket.first_response_time 不为空。
3. t_ticket_flow_record 新增 ACCEPT 记录。
```

处理完成：

```bash
curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/handle' \
  -H 'Content-Type: application/json' \
  -H 'satoken: handler-token' \
  -d '{
    "remark": "已重新触发退款补偿任务，退款状态已同步"
  }'
```

预期结果：

```text
1. t_ticket.status = WAIT_CONFIRM。
2. t_ticket.finish_time 不为空。
3. t_ticket_flow_record 新增 HANDLE 记录。
```

用户确认：

```bash
curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/confirm' \
  -H 'Content-Type: application/json' \
  -H 'satoken: creator-token' \
  -d '{
    "remark": "问题已解决，确认关闭"
  }'
```

预期结果：

```text
1. t_ticket.status = CLOSED。
2. t_ticket.close_time 不为空。
3. t_ticket_flow_record 新增 CONFIRM 记录。
```

查询详情确认时间线：

```bash
curl -X GET 'http://localhost:8080/api/tickets/1850000000000000001' \
  -H 'satoken: creator-token'
```

详情中的 `flowRecords` 应该按时间包含：

```text
CREATE
ASSIGN
ACCEPT
HANDLE
CONFIRM
```

### 转交与升级验证

转交流程用于当前处理人无法继续处理，需要交给另一个处理人。转交后状态会回到 `WAIT_ACCEPT`。

转交工单：

```bash
curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/transfer' \
  -H 'Content-Type: application/json' \
  -H 'satoken: handler-token' \
  -d '{
    "newHandlerId": 3001,
    "newHandlerName": "运维处理人",
    "remark": "该问题涉及退款服务异常，转交运维处理"
  }'
```

预期结果：

```text
1. t_ticket.status = WAIT_ACCEPT。
2. t_ticket.handler_id = 3001。
3. t_ticket.handler_name = 运维处理人。
4. t_ticket_flow_record 新增 TRANSFER 记录。
5. 原处理人和新处理人字段都应该正确记录。
```

新处理人接单：

```bash
curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/accept' \
  -H 'Content-Type: application/json' \
  -H 'satoken: new-handler-token' \
  -d '{
    "remark": "已接收转交工单"
  }'
```

升级工单：

```bash
curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/upgrade' \
  -H 'Content-Type: application/json' \
  -H 'satoken: new-handler-token' \
  -d '{
    "upgradeHandlerId": 9001,
    "upgradeHandlerName": "售后主管",
    "remark": "问题影响范围较大，升级主管处理"
  }'
```

预期结果：

```text
1. t_ticket.status 仍为 PROCESSING。
2. t_ticket.handler_id = 9001。
3. t_ticket.upgrade_flag = 1。
4. t_ticket_flow_record 新增 UPGRADE 记录。
```

### SLA 超时验证

SLA 超时验证可以通过缩短 SLA 规则时间实现。建议本地把规则调整为 1 到 3 分钟，便于观察。

初始化一条测试规则：

```sql
UPDATE t_ticket_sla_rule
SET response_minutes = 1,
    handle_minutes = 2,
    remind_before_minutes = 1,
    update_time = NOW()
WHERE ticket_type = 'AFTER_SALE'
  AND priority = 'URGENT'
  AND deleted = 0;
```

创建紧急售后工单：

```bash
curl -X POST 'http://localhost:8080/api/tickets' \
  -H 'Content-Type: application/json' \
  -H 'satoken: creator-token' \
  -d '{
    "title": "SLA 超时测试工单",
    "content": "该工单用于验证 SLA 提醒和升级",
    "ticketType": "AFTER_SALE",
    "priority": "URGENT"
  }'
```

等待 1 分钟后，不接单，手动触发 SLA 扫描：

```bash
curl -X POST 'http://localhost:8080/api/tickets/sla/scan' \
  -H 'satoken: admin-token'
```

预期结果：

```text
1. 如果当前时间超过 response_deadline，response_timeout = 1。
2. t_ticket_flow_record 新增 SLA_REMIND 记录。
3. RabbitMQ 发送 SLA_REMIND 通知。
```

等待到处理截止时间后，再触发扫描：

```bash
curl -X POST 'http://localhost:8080/api/tickets/sla/scan' \
  -H 'satoken: admin-token'
```

预期结果：

```text
1. t_ticket.handle_timeout = 1。
2. t_ticket.upgrade_flag = 1。
3. t_ticket.handler_id 变为 SLA 规则中的 upgrade_handler_id。
4. t_ticket_flow_record 新增 SLA_UPGRADE 记录。
5. RabbitMQ 发送 SLA_UPGRADE 通知。
```

也可以直接启动 XXL-JOB，等待 `ticketSlaTimeoutJob` 每分钟自动执行：

```text
JobHandler：ticketSlaTimeoutJob
CRON：0 0/1 * * * ?
```

### 幂等与重复操作验证

幂等验证主要验证重复接单、重复处理、重复关闭、重复 SLA 扫描不会产生脏数据。

重复接单验证：

```bash
curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/accept' \
  -H 'Content-Type: application/json' \
  -H 'satoken: handler-token' \
  -d '{
    "remark": "第一次接单"
  }'

curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/accept' \
  -H 'Content-Type: application/json' \
  -H 'satoken: handler-token' \
  -d '{
    "remark": "第二次重复接单"
  }'
```

预期结果：

```text
1. 第一次成功。
2. 第二次失败，提示非法状态流转或工单状态已变化。
3. t_ticket_flow_record 只新增一条 ACCEPT 记录。
```

重复处理验证：

```bash
curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/handle' \
  -H 'Content-Type: application/json' \
  -H 'satoken: handler-token' \
  -d '{
    "remark": "第一次处理完成"
  }'

curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/handle' \
  -H 'Content-Type: application/json' \
  -H 'satoken: handler-token' \
  -d '{
    "remark": "第二次重复处理"
  }'
```

预期结果：

```text
1. 第一次从 PROCESSING 变为 WAIT_CONFIRM。
2. 第二次不允许从 WAIT_CONFIRM 再执行 HANDLE。
3. t_ticket_flow_record 只新增一条 HANDLE 记录。
```

重复关闭验证：

```bash
curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/confirm' \
  -H 'Content-Type: application/json' \
  -H 'satoken: creator-token' \
  -d '{
    "remark": "确认关闭"
  }'

curl -X POST 'http://localhost:8080/api/tickets/1850000000000000001/close' \
  -H 'Content-Type: application/json' \
  -H 'satoken: admin-token' \
  -d '{
    "remark": "重复关闭"
  }'
```

预期结果：

```text
1. 第一次关闭成功。
2. 第二次不允许从 CLOSED 再关闭。
3. close_time 不应被重复覆盖。
```

重复 SLA 扫描验证：

```bash
curl -X POST 'http://localhost:8080/api/tickets/sla/scan' \
  -H 'satoken: admin-token'

curl -X POST 'http://localhost:8080/api/tickets/sla/scan' \
  -H 'satoken: admin-token'

curl -X POST 'http://localhost:8080/api/tickets/sla/scan' \
  -H 'satoken: admin-token'
```

预期结果：

```text
1. remind_flag 已经为 1 的工单不会重复提醒。
2. handle_timeout 已经为 1 的工单不会重复标记超时。
3. upgrade_flag 已经为 1 的工单不会重复升级。
4. t_ticket_flow_record 不应重复出现多条相同 SLA_UPGRADE 记录。
```

最终核心校验 SQL：

```sql
-- 查询工单当前状态
SELECT id, ticket_no, status, handler_id, handler_name,
       response_timeout, handle_timeout, remind_flag, upgrade_flag,
       response_deadline, handle_deadline
FROM t_ticket
WHERE id = 1850000000000000001;

-- 查询工单流转时间线
SELECT event, from_status, to_status,
       from_handler_name, to_handler_name,
       operator_name, remark, create_time
FROM t_ticket_flow_record
WHERE ticket_id = 1850000000000000001
ORDER BY create_time ASC;

-- 检查是否重复 SLA 升级
SELECT ticket_id, event, COUNT(*) AS cnt
FROM t_ticket_flow_record
WHERE event = 'SLA_UPGRADE'
GROUP BY ticket_id, event
HAVING COUNT(*) > 1;
```

完成以上验证后，这个工单模块已经具备可落地的核心能力：状态机约束、自动派单、处理人变更、SLA 提醒、SLA 升级、消息通知、评论协作和流转留痕。