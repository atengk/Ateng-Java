# 审批流 / 流程流转

本案例基于 README 中“审批流 / 流程流转”场景展开，重点实现请假、报销、采购、合同等常见业务中的核心审批能力，包括发起审批、生成待办、审批通过、审批驳回、撤回、转交、审批记录留痕和待办 / 已办查询。

## 案例目标与技术选型

本案例不直接引入 Flowable / Camunda 这类完整 BPMN 工作流引擎，而是采用“轻量级自研审批流”的方式实现核心功能。这样更适合中小型后台系统、OA 模块、管理平台和面试项目展示，也更容易理解审批流底层的状态流转、节点推进和任务生成逻辑。

如果业务后续需要可视化流程设计器、复杂 BPMN 网关、子流程、定时边界事件、流程版本迁移等能力，可以再升级为 Flowable 或 Camunda。

### 实现范围

本案例实现一个“请假审批流”的核心流程，流程规则如下：

```text
员工提交请假申请
-> 系统创建审批单
-> 生成直属主管待办任务
-> 直属主管审批通过
-> 系统生成部门经理待办任务
-> 部门经理审批通过
-> 审批单结束
```

支持的核心能力：

| 功能     | 说明                             |
| -------- | -------------------------------- |
| 发起审批 | 用户提交业务单据后创建审批实例   |
| 生成待办 | 根据当前流程节点生成审批任务     |
| 审批通过 | 当前审批人同意后流转到下一节点   |
| 审批驳回 | 当前审批人驳回后流程终止         |
| 审批撤回 | 发起人在未完成前可以撤回审批     |
| 审批转交 | 当前审批人可以把任务转交给其他人 |
| 审批记录 | 记录每一次审批动作，形成时间线   |
| 我的待办 | 查询当前用户需要处理的审批任务   |
| 我的已办 | 查询当前用户已经处理过的审批任务 |
| 审批详情 | 查询审批单、当前节点、审批记录   |

暂不实现的扩展能力：

| 功能             | 说明                             |
| ---------------- | -------------------------------- |
| 会签             | 多人全部同意才进入下一节点       |
| 或签             | 多人任意一人同意即可进入下一节点 |
| 加签             | 审批过程中临时增加审批人         |
| 动态表单         | 不同审批类型配置不同表单字段     |
| 可视化流程设计器 | 通过页面拖拽配置审批节点         |
| Flowable 集成    | 使用 BPMN 引擎管理复杂流程       |

本案例先把审批流最核心的“实例、节点、任务、动作、记录、状态”打通，后续再扩展会签、或签、加签会更自然。

### 技术栈说明

本案例使用 Spring Boot 3 + MyBatis-Plus + MySQL 实现核心审批能力。Redis 和 RabbitMQ 不是最小闭环必需组件，但在实际项目中可以用于缓存审批人、发送审批通知、处理消息异步化。

| 技术               | 用途                                     |
| ------------------ | ---------------------------------------- |
| Spring Boot 3      | 后端主框架                               |
| MyBatis-Plus       | 数据库 CRUD 和分页查询                   |
| MySQL              | 存储审批单、审批任务、审批记录、流程节点 |
| Sa-Token           | 获取当前登录用户信息                     |
| Hutool             | 字符串、集合、日期、对象工具处理         |
| Lombok             | 简化实体类、DTO、VO 代码                 |
| Redis              | 可选，用于缓存流程配置和用户组织关系     |
| RabbitMQ           | 可选，用于异步发送审批通知               |
| Flowable / Camunda | 可选，用于后续复杂审批流升级             |

推荐采用的包结构如下：

```text
src/main/java/io/github/atengk/approval
├── controller
│   └── ApprovalController.java
├── service
│   ├── ApprovalService.java
│   └── impl
│       └── ApprovalServiceImpl.java
├── mapper
│   ├── ApprovalInstanceMapper.java
│   ├── ApprovalTaskMapper.java
│   ├── ApprovalRecordMapper.java
│   └── ApprovalNodeMapper.java
├── entity
│   ├── ApprovalInstance.java
│   ├── ApprovalTask.java
│   ├── ApprovalRecord.java
│   └── ApprovalNode.java
├── dto
│   ├── ApprovalStartDTO.java
│   ├── ApprovalHandleDTO.java
│   └── ApprovalTransferDTO.java
├── vo
│   ├── ApprovalDetailVO.java
│   └── ApprovalTaskVO.java
├── enums
│   ├── ApprovalStatusEnum.java
│   ├── ApprovalTaskStatusEnum.java
│   └── ApprovalActionEnum.java
└── component
    └── ApproverResolver.java
```

### 核心业务流程

审批流的关键不是“表单提交”，而是“任务流转”。本案例将审批过程拆成三类对象：

| 对象     | 作用                                                 |
| -------- | ---------------------------------------------------- |
| 审批实例 | 表示一次完整审批，比如一张请假单的审批流程           |
| 审批任务 | 表示当前需要某个审批人处理的待办                     |
| 审批记录 | 表示每一次操作留痕，比如提交、通过、驳回、撤回、转交 |

核心流转过程如下：

```text
用户发起审批
-> 创建审批实例 approval_instance
-> 写入提交记录 approval_record
-> 查询流程第一个节点 approval_node
-> 计算当前审批人
-> 创建审批任务 approval_task

审批人处理任务
-> 校验任务是否属于当前审批人
-> 校验任务是否待处理
-> 更新当前任务状态为已处理
-> 写入审批记录
-> 如果审批通过，判断是否存在下一节点
   -> 有下一节点：创建下一节点审批任务
   -> 无下一节点：审批实例变为已通过
-> 如果审批驳回，审批实例变为已驳回
```

正常审批通过流程：

```text
员工提交请假申请
-> 创建审批实例：状态 = 审批中
-> 创建主管审批任务：状态 = 待处理
-> 主管同意
-> 主管任务变为已通过
-> 创建部门经理审批任务：状态 = 待处理
-> 部门经理同意
-> 部门经理任务变为已通过
-> 审批实例变为已通过
```

审批驳回流程：

```text
员工提交请假申请
-> 创建审批实例：状态 = 审批中
-> 创建主管审批任务：状态 = 待处理
-> 主管驳回
-> 主管任务变为已驳回
-> 审批实例变为已驳回
-> 流程结束，不再生成后续任务
```

审批撤回流程：

```text
员工提交请假申请
-> 创建审批实例：状态 = 审批中
-> 创建主管审批任务：状态 = 待处理
-> 发起人撤回
-> 当前待办任务变为已取消
-> 审批实例变为已撤回
-> 写入撤回记录
```

审批转交流程：

```text
主管收到审批任务
-> 主管选择转交给其他审批人
-> 原任务审批人更新为新审批人
-> 写入转交记录
-> 新审批人在我的待办中看到该任务
```

本案例最终要保证三件事：

第一，审批实例状态只能按照合法路径变化，例如“审批中”可以变为“已通过、已驳回、已撤回”，但“已通过”不能再次变为“审批中”。

第二，每次审批动作都必须写入审批记录，方便后续做审批时间线、审计追踪和问题排查。

第三，待办任务必须和审批人强绑定，处理审批时必须校验当前登录用户是否就是该任务的审批人。

## 数据库表设计

数据库设计围绕“审批实例、审批任务、审批记录、流程节点配置”四张核心表展开，对应 README 中提到的“创建流程实例、生成当前审批任务、审批人处理、流转下一节点、驳回、撤回、转交、流程归档”等核心能力。

本案例使用 MySQL 8，主键使用 `BIGINT`，后续代码中可以通过 Hutool `IdUtil.getSnowflakeNextId()` 生成分布式 ID。

### 审批单主表

审批单主表用于保存一次完整审批流程的实例信息，例如一张请假单、报销单或采购单对应一条审批实例记录。

表设计重点是保存业务单据、发起人、当前状态、当前节点和结束时间。

```sql
-- 审批单主表：一条记录表示一次完整审批流程实例
CREATE TABLE approval_instance (
    id BIGINT NOT NULL COMMENT '主键ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型：LEAVE=请假，EXPENSE=报销，PURCHASE=采购',
    biz_id BIGINT NOT NULL COMMENT '业务单据ID，例如请假单ID',
    title VARCHAR(200) NOT NULL COMMENT '审批标题',
    applicant_id BIGINT NOT NULL COMMENT '发起人用户ID',
    status VARCHAR(32) NOT NULL COMMENT '审批状态：PROCESSING/PASSED/REJECTED/WITHDRAWN',
    current_node_code VARCHAR(64) DEFAULT NULL COMMENT '当前流程节点编码',
    current_node_name VARCHAR(100) DEFAULT NULL COMMENT '当前流程节点名称',
    started_at DATETIME NOT NULL COMMENT '发起时间',
    finished_at DATETIME DEFAULT NULL COMMENT '结束时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '发起说明',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_type_biz_id (biz_type, biz_id),
    KEY idx_applicant_status (applicant_id, status),
    KEY idx_status_create_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批单主表';
```

关键字段说明：

| 字段                 | 说明                                 |
| -------------------- | ------------------------------------ |
| `biz_type`           | 区分请假、报销、采购、合同等审批类型 |
| `biz_id`             | 关联具体业务表主键                   |
| `status`             | 当前审批实例状态                     |
| `current_node_code`  | 当前停留在哪个审批节点               |
| `version`            | 用于并发审批时做乐观锁控制           |
| `uk_biz_type_biz_id` | 防止同一业务单据重复发起审批         |

这里没有直接把请假单字段放进审批表，是为了让审批流模块保持通用。请假、报销、采购等业务数据由各自业务表保存，审批模块只负责流程流转。

### 审批任务表

审批任务表用于保存“谁需要审批”。每一个待办任务对应一个审批人和一个流程节点。

例如员工提交请假申请后，会生成一条直属主管的待办任务；直属主管审批通过后，再生成一条部门经理的待办任务。

```sql
-- 审批任务表：一条记录表示一个审批人需要处理的一项待办
CREATE TABLE approval_task (
    id BIGINT NOT NULL COMMENT '主键ID',
    instance_id BIGINT NOT NULL COMMENT '审批实例ID',
    node_code VARCHAR(64) NOT NULL COMMENT '节点编码',
    node_name VARCHAR(100) NOT NULL COMMENT '节点名称',
    approver_id BIGINT NOT NULL COMMENT '当前审批人用户ID',
    original_approver_id BIGINT DEFAULT NULL COMMENT '原审批人ID，转交场景使用',
    status VARCHAR(32) NOT NULL COMMENT '任务状态：PENDING/PASSED/REJECTED/CANCELED/TRANSFERRED',
    sort_no INT NOT NULL COMMENT '节点排序',
    due_time DATETIME DEFAULT NULL COMMENT '任务截止时间，可用于超时提醒',
    handle_time DATETIME DEFAULT NULL COMMENT '处理时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '处理意见',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    PRIMARY KEY (id),
    KEY idx_instance_id (instance_id),
    KEY idx_approver_status (approver_id, status),
    KEY idx_node_instance (instance_id, node_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批任务表';
```

关键字段说明：

| 字段                   | 说明                           |
| ---------------------- | ------------------------------ |
| `instance_id`          | 关联审批实例                   |
| `approver_id`          | 当前任务处理人                 |
| `original_approver_id` | 转交前的原审批人               |
| `status`               | 当前任务状态                   |
| `sort_no`              | 当前节点顺序，用于查找下一节点 |
| `due_time`             | 后续可扩展审批超时提醒         |

处理待办时，必须校验 `approver_id` 是否等于当前登录用户 ID。否则会出现用户越权审批的问题。

### 审批记录表

审批记录表用于保存审批过程中的所有操作留痕，包括提交、通过、驳回、撤回、转交等动作。

这张表一般只追加，不更新，用于时间线展示、审计追踪和问题排查。

```sql
-- 审批记录表：记录每一次审批动作，形成审批时间线
CREATE TABLE approval_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    instance_id BIGINT NOT NULL COMMENT '审批实例ID',
    task_id BIGINT DEFAULT NULL COMMENT '审批任务ID，发起和撤回场景可为空',
    node_code VARCHAR(64) DEFAULT NULL COMMENT '节点编码',
    node_name VARCHAR(100) DEFAULT NULL COMMENT '节点名称',
    operator_id BIGINT NOT NULL COMMENT '操作人用户ID',
    action VARCHAR(32) NOT NULL COMMENT '审批动作：SUBMIT/PASS/REJECT/WITHDRAW/TRANSFER',
    action_desc VARCHAR(100) NOT NULL COMMENT '动作描述',
    from_user_id BIGINT DEFAULT NULL COMMENT '来源用户ID，转交场景使用',
    to_user_id BIGINT DEFAULT NULL COMMENT '目标用户ID，转交场景使用',
    comment VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_instance_id (instance_id),
    KEY idx_operator_id (operator_id),
    KEY idx_instance_create_time (instance_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';
```

关键字段说明：

| 字段           | 说明                             |
| -------------- | -------------------------------- |
| `task_id`      | 关联审批任务，提交和撤回可以为空 |
| `operator_id`  | 当前操作人                       |
| `action`       | 操作动作                         |
| `from_user_id` | 转交前审批人                     |
| `to_user_id`   | 转交后审批人                     |
| `comment`      | 审批意见                         |

审批记录不要做物理删除。即使审批单撤回或驳回，也应该保留完整操作轨迹。

### 流程节点配置表

流程节点配置表用于定义某类审批的流转规则。

本案例使用数据库配置流程节点，不使用 BPMN XML。这样更轻量，也更容易结合普通后台管理页面维护。

```sql
-- 流程节点配置表：定义不同业务类型的审批节点顺序和审批人规则
CREATE TABLE approval_node (
    id BIGINT NOT NULL COMMENT '主键ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型：LEAVE=请假，EXPENSE=报销，PURCHASE=采购',
    node_code VARCHAR(64) NOT NULL COMMENT '节点编码',
    node_name VARCHAR(100) NOT NULL COMMENT '节点名称',
    node_type VARCHAR(32) NOT NULL COMMENT '节点类型：START/APPROVAL/END',
    approver_type VARCHAR(32) DEFAULT NULL COMMENT '审批人类型：MANAGER/ROLE/USER',
    approver_value VARCHAR(200) DEFAULT NULL COMMENT '审批人配置值，例如角色编码或用户ID',
    sort_no INT NOT NULL COMMENT '节点排序',
    reject_strategy VARCHAR(32) NOT NULL DEFAULT 'TERMINATE' COMMENT '驳回策略：TERMINATE=终止，BACK_PREVIOUS=退回上一节点',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0=禁用，1=启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_node (biz_type, node_code),
    KEY idx_biz_sort (biz_type, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点配置表';
```

插入一套请假审批流程配置。

```sql
-- 请假审批流节点配置：员工 -> 直属主管 -> 部门经理 -> 结束
INSERT INTO approval_node (
    id, biz_type, node_code, node_name, node_type,
    approver_type, approver_value, sort_no, reject_strategy
) VALUES
(1001, 'LEAVE', 'START', '发起申请', 'START', NULL, NULL, 1, 'TERMINATE'),
(1002, 'LEAVE', 'MANAGER_APPROVE', '直属主管审批', 'APPROVAL', 'MANAGER', NULL, 2, 'TERMINATE'),
(1003, 'LEAVE', 'DEPT_MANAGER_APPROVE', '部门经理审批', 'APPROVAL', 'ROLE', 'DEPT_MANAGER', 3, 'TERMINATE'),
(1004, 'LEAVE', 'END', '流程结束', 'END', NULL, NULL, 4, 'TERMINATE');
```

关键字段说明：

| 字段              | 说明                             |
| ----------------- | -------------------------------- |
| `biz_type`        | 同一种业务类型使用同一套流程配置 |
| `node_type`       | 区分开始节点、审批节点、结束节点 |
| `approver_type`   | 审批人计算方式                   |
| `approver_value`  | 角色编码、固定用户 ID 等配置值   |
| `sort_no`         | 决定审批流转顺序                 |
| `reject_strategy` | 控制驳回后是终止还是退回         |

当前案例只实现串行审批。后续要支持会签、或签，可以在节点表中增加 `approve_mode` 字段，例如 `SEQUENTIAL`、`COUNTERSIGN`、`OR_SIGN`。

## 流程模型设计

流程模型设计的重点是把审批过程抽象成可控状态机。实际项目中，不建议直接用中文状态写业务判断，应该统一使用枚举值，避免后期状态越来越多导致代码混乱。

### 审批状态设计

审批实例状态用于描述整条审批流程的最终或当前状态。

| 状态         | 说明   | 是否终态 |
| ------------ | ------ | -------- |
| `PROCESSING` | 审批中 | 否       |
| `PASSED`     | 已通过 | 是       |
| `REJECTED`   | 已驳回 | 是       |
| `WITHDRAWN`  | 已撤回 | 是       |

审批实例状态流转如下：

```text
PROCESSING
├── PASSED
├── REJECTED
└── WITHDRAWN
```

审批任务状态用于描述单个审批人的待办状态。

| 状态          | 说明   | 是否可处理 |
| ------------- | ------ | ---------- |
| `PENDING`     | 待处理 | 是         |
| `PASSED`      | 已通过 | 否         |
| `REJECTED`    | 已驳回 | 否         |
| `CANCELED`    | 已取消 | 否         |
| `TRANSFERRED` | 已转交 | 否         |

审批实例和审批任务的状态不要混用。审批实例表示整条流程，审批任务表示某个节点上的某个人。

### 节点类型设计

节点类型用于控制流程节点的用途。

| 节点类型   | 说明                                         |
| ---------- | -------------------------------------------- |
| `START`    | 开始节点，只用于标记流程开始，不生成审批任务 |
| `APPROVAL` | 审批节点，需要生成审批任务                   |
| `END`      | 结束节点，只用于标记流程结束，不生成审批任务 |

审批人类型用于控制审批人如何计算。

| 审批人类型 | 说明             | 示例           |
| ---------- | ---------------- | -------------- |
| `MANAGER`  | 发起人的直属主管 | 请假审批第一步 |
| `ROLE`     | 指定角色下的用户 | 部门经理审批   |
| `USER`     | 固定用户         | 指定财务负责人 |
| `SELF`     | 发起人本人       | 自确认节点     |

本案例会在后续代码中通过 `ApproverResolver` 统一计算审批人，避免审批人规则散落在 Service 里。

### 审批动作设计

审批动作表示用户对审批流程做了什么操作，主要用于审批记录表。

| 动作       | 说明         |
| ---------- | ------------ |
| `SUBMIT`   | 发起审批     |
| `PASS`     | 审批通过     |
| `REJECT`   | 审批驳回     |
| `WITHDRAW` | 发起人撤回   |
| `TRANSFER` | 审批任务转交 |

审批动作和审批状态的区别是：

| 概念     | 作用                       |
| -------- | -------------------------- |
| 审批动作 | 描述发生了什么操作         |
| 审批状态 | 描述操作后流程变成什么状态 |

例如用户点击“通过”，审批动作是 `PASS`。如果这是最后一个审批节点，审批实例状态变成 `PASSED`；如果后面还有节点，审批实例状态仍然是 `PROCESSING`。

### 驳回与撤回规则

本案例先采用最常见、最容易落地的规则。

驳回规则：

| 规则           | 说明                    |
| -------------- | ----------------------- |
| 谁可以驳回     | 当前待办任务的审批人    |
| 什么状态能驳回 | 任务必须是 `PENDING`    |
| 驳回后任务状态 | 当前任务变成 `REJECTED` |
| 驳回后实例状态 | 审批实例变成 `REJECTED` |
| 是否继续流转   | 不继续生成后续任务      |

撤回规则：

| 规则           | 说明                            |
| -------------- | ------------------------------- |
| 谁可以撤回     | 审批发起人                      |
| 什么状态能撤回 | 审批实例必须是 `PROCESSING`     |
| 撤回后任务状态 | 当前所有待办任务变成 `CANCELED` |
| 撤回后实例状态 | 审批实例变成 `WITHDRAWN`        |
| 是否保留记录   | 必须写入撤回记录                |

本案例的驳回采用“直接终止”。如果后续想做“驳回到发起人重新提交”或“驳回到上一节点”，可以在 `approval_node.reject_strategy` 中扩展：

| 驳回策略        | 说明             |
| --------------- | ---------------- |
| `TERMINATE`     | 直接终止流程     |
| `BACK_START`    | 退回发起人       |
| `BACK_PREVIOUS` | 退回上一审批节点 |
| `BACK_TARGET`   | 退回指定节点     |

## 项目依赖与基础配置

本节给出最小可运行的 Spring Boot 3 基础配置，包括 Maven 依赖、数据源、MyBatis-Plus 和 Sa-Token 用户上下文。

### Maven 依赖配置

下面依赖放在项目根目录 `pom.xml` 中，版本可以根据你的 Spring Boot 父工程统一管理。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MyBatis-Plus：简化 CRUD、分页、乐观锁、逻辑删除 -->
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

    <!-- Sa-Token：登录认证与获取当前用户 ID -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>1.39.0</version>
    </dependency>

    <!-- Hutool：常用工具类，后续用于 ID、日期、字符串、集合处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>

    <!-- Lombok：简化 Getter、Setter、构造器、日志对象等代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 参数校验：用于 Controller 入参校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- AOP：后续可扩展审批操作日志、幂等注解、权限拦截 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
</dependencies>
```

如果你后续要加审批通知，可以再补充 RabbitMQ；如果要缓存流程节点配置，可以再补充 Redis。

```xml
<!-- Redis：可选，用于缓存流程配置、审批人信息、用户组织关系 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- RabbitMQ：可选，用于异步发送审批通知 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 数据源配置

下面配置放在 `src/main/resources/application.yml`。

```yaml
server:
  port: 8080

spring:
  application:
    name: approval-demo

  datasource:
    # MySQL 8 连接地址，approval_demo 为示例数据库名
    url: jdbc:mysql://127.0.0.1:3306/approval_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    # 控制台打印 SQL，开发环境建议开启，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # 下划线字段自动映射为驼峰属性
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      # 逻辑删除字段配置
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      # 主键由代码手动生成，使用 Hutool Snowflake
      id-type: input

sa-token:
  # Token 名称
  token-name: Authorization
  # Token 有效期，单位秒，-1 表示永久有效
  timeout: 86400
  # 是否允许同一账号多地登录
  is-concurrent: true
  # 是否尝试从请求头读取 Token
  is-read-header: true
  # 是否从 Cookie 读取 Token，前后端分离项目通常关闭
  is-read-cookie: false
```

开发时可以先手动创建数据库。

```sql
-- 创建示例数据库
CREATE DATABASE IF NOT EXISTS approval_demo
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;
```

### MyBatis-Plus 配置

MyBatis-Plus 配置主要用于启用分页插件和乐观锁插件。审批场景中，乐观锁很重要，可以降低并发审批造成的重复处理风险。

文件位置：`src/main/java/io/github/atengk/approval/config/MyBatisPlusConfig.java`

下面配置类启用 MyBatis-Plus 分页插件和乐观锁插件。

```java
package io.github.atengk.approval.config;

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
     * 注册 MyBatis-Plus 插件
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 乐观锁插件：配合实体类中的 @Version 字段使用
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页插件：用于待办、已办、审批记录分页查询
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
```

后续实体类中需要在 `version` 字段上加 `@Version`，在 `deleted` 字段上加 `@TableLogic`。

示例：

```java
@Version
private Integer version;

@TableLogic
private Integer deleted;
```

### Sa-Token 用户上下文配置

审批操作必须依赖当前登录用户 ID，例如：

```text
发起审批 -> 当前用户就是 applicant_id
处理审批 -> 当前用户必须等于 task.approver_id
撤回审批 -> 当前用户必须等于 instance.applicant_id
转交审批 -> 当前用户必须等于 task.approver_id
```

为了避免业务代码里到处直接调用 `StpUtil.getLoginIdAsLong()`，建议封装一个用户上下文工具类。

文件位置：`src/main/java/io/github/atengk/approval/security/LoginUserContext.java`

下面工具类统一获取当前登录用户 ID，后续 Service 中直接使用它。

```java
package io.github.atengk.approval.security;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 登录用户上下文
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class LoginUserContext {

    private LoginUserContext() {
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 当前用户ID
     */
    public static Long getUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 校验当前用户是否已登录
     */
    public static void checkLogin() {
        StpUtil.checkLogin();
    }
}
```

开发阶段如果暂时没有接入真实登录，可以先提供一个测试登录接口，方便接口调试。

文件位置：`src/main/java/io/github/atengk/approval/controller/AuthTestController.java`

下面测试接口仅用于本地开发，通过传入用户 ID 模拟登录。

```java
package io.github.atengk.approval.controller;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 本地测试登录接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequestMapping("/auth/test")
public class AuthTestController {

    /**
     * 模拟登录
     *
     * @param userId 用户ID
     * @return Token 信息
     */
    @PostMapping("/login/{userId}")
    public String login(@PathVariable Long userId) {
        StpUtil.login(userId);
        String tokenValue = StpUtil.getTokenValue();
        log.info("用户模拟登录成功，userId={}，token={}", userId, tokenValue);
        return tokenValue;
    }

    /**
     * 退出登录
     *
     * @return 操作结果
     */
    @PostMapping("/logout")
    public String logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        log.info("用户退出登录成功，userId={}", userId);
        return "退出成功";
    }
}
```

本地测试登录命令如下。

```bash
# 模拟用户 10001 登录，返回的字符串就是 Token
curl -X POST http://localhost:8080/auth/test/login/10001
```

后续调用审批接口时，在请求头中携带 Token。

```bash
# Authorization 的值替换为上一步返回的 Token
curl -H "Authorization: your-token-value" \
  http://localhost:8080/approval/tasks/todo
```

到这里，审批流案例的数据库表、流程模型和基础工程配置已经具备。下一步可以继续实现枚举、实体类、Mapper、审批人计算组件和核心 Service 流转逻辑。

## 核心代码实现

本节实现审批流最小闭环，围绕 README 中“用户发起审批、创建流程实例、生成当前审批任务、审批人处理、同意后流转下一节点、驳回后终止、撤回、转交、记录留痕”的核心链路展开。

为了让代码更容易落地，这里不引入复杂 BPMN 引擎，而是通过数据库节点配置 + Service 状态流转实现审批核心能力。

### 枚举与常量定义

枚举用于统一审批实例状态、审批任务状态、审批动作、节点类型和审批人类型，避免业务代码中散落字符串判断。

文件位置：`src/main/java/io/github/atengk/approval/enums/ApprovalStatusEnum.java`

```java
package io.github.atengk.approval.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 审批实例状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@RequiredArgsConstructor
public enum ApprovalStatusEnum {

    PROCESSING("PROCESSING", "审批中"),
    PASSED("PASSED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    WITHDRAWN("WITHDRAWN", "已撤回");

    private final String code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/approval/enums/ApprovalTaskStatusEnum.java`

```java
package io.github.atengk.approval.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 审批任务状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@RequiredArgsConstructor
public enum ApprovalTaskStatusEnum {

    PENDING("PENDING", "待处理"),
    PASSED("PASSED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    CANCELED("CANCELED", "已取消"),
    TRANSFERRED("TRANSFERRED", "已转交");

    private final String code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/approval/enums/ApprovalActionEnum.java`

```java
package io.github.atengk.approval.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 审批动作枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@RequiredArgsConstructor
public enum ApprovalActionEnum {

    SUBMIT("SUBMIT", "发起审批"),
    PASS("PASS", "审批通过"),
    REJECT("REJECT", "审批驳回"),
    WITHDRAW("WITHDRAW", "审批撤回"),
    TRANSFER("TRANSFER", "审批转交");

    private final String code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/approval/enums/ApprovalNodeTypeEnum.java`

```java
package io.github.atengk.approval.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 审批节点类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@RequiredArgsConstructor
public enum ApprovalNodeTypeEnum {

    START("START", "开始节点"),
    APPROVAL("APPROVAL", "审批节点"),
    END("END", "结束节点");

    private final String code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/approval/enums/ApproverTypeEnum.java`

```java
package io.github.atengk.approval.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 审批人类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@RequiredArgsConstructor
public enum ApproverTypeEnum {

    MANAGER("MANAGER", "直属主管"),
    ROLE("ROLE", "指定角色"),
    USER("USER", "固定用户"),
    SELF("SELF", "发起人本人");

    private final String code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/approval/constant/ApprovalConstant.java`

```java
package io.github.atengk.approval.constant;

/**
 * 审批常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ApprovalConstant {

    private ApprovalConstant() {
    }

    /**
     * 请假审批业务类型
     */
    public static final String BIZ_TYPE_LEAVE = "LEAVE";

    /**
     * 部门经理角色编码
     */
    public static final String ROLE_DEPT_MANAGER = "DEPT_MANAGER";
}
```

### 实体类设计

实体类与前面数据库表一一对应。这里使用 MyBatis-Plus 注解完成主键、乐观锁和逻辑删除映射。

文件位置：`src/main/java/io/github/atengk/approval/entity/ApprovalInstance.java`

```java
package io.github.atengk.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批单主表实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("approval_instance")
public class ApprovalInstance {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String bizType;

    private Long bizId;

    private String title;

    private Long applicantId;

    private String status;

    private String currentNodeCode;

    private String currentNodeName;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String remark;

    @Version
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/approval/entity/ApprovalTask.java`

```java
package io.github.atengk.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批任务实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("approval_task")
public class ApprovalTask {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long instanceId;

    private String nodeCode;

    private String nodeName;

    private Long approverId;

    private Long originalApproverId;

    private String status;

    private Integer sortNo;

    private LocalDateTime dueTime;

    private LocalDateTime handleTime;

    private String remark;

    @Version
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/approval/entity/ApprovalRecord.java`

```java
package io.github.atengk.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批记录实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("approval_record")
public class ApprovalRecord {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long instanceId;

    private Long taskId;

    private String nodeCode;

    private String nodeName;

    private Long operatorId;

    private String action;

    private String actionDesc;

    private Long fromUserId;

    private Long toUserId;

    private String comment;

    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/approval/entity/ApprovalNode.java`

```java
package io.github.atengk.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程节点配置实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("approval_node")
public class ApprovalNode {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String bizType;

    private String nodeCode;

    private String nodeName;

    private String nodeType;

    private String approverType;

    private String approverValue;

    private Integer sortNo;

    private String rejectStrategy;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

下面几个 DTO 是核心 Service 入参，分别对应发起审批、处理审批和转交审批。

文件位置：`src/main/java/io/github/atengk/approval/dto/ApprovalStartDTO.java`

```java
package io.github.atengk.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发起审批请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ApprovalStartDTO {

    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @NotNull(message = "业务单据ID不能为空")
    private Long bizId;

    @NotBlank(message = "审批标题不能为空")
    private String title;

    private String remark;
}
```

文件位置：`src/main/java/io/github/atengk/approval/dto/ApprovalHandleDTO.java`

```java
package io.github.atengk.approval.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批处理请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ApprovalHandleDTO {

    @NotNull(message = "审批任务ID不能为空")
    private Long taskId;

    private String comment;
}
```

文件位置：`src/main/java/io/github/atengk/approval/dto/ApprovalTransferDTO.java`

```java
package io.github.atengk.approval.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批转交请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ApprovalTransferDTO {

    @NotNull(message = "审批任务ID不能为空")
    private Long taskId;

    @NotNull(message = "转交目标用户ID不能为空")
    private Long toUserId;

    private String comment;
}
```

### Mapper 层实现

Mapper 层先使用 MyBatis-Plus `BaseMapper` 即可满足核心 CRUD。后续待办、已办分页查询可以继续扩展自定义 SQL。

文件位置：`src/main/java/io/github/atengk/approval/mapper/ApprovalInstanceMapper.java`

```java
package io.github.atengk.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.approval.entity.ApprovalInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批单 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ApprovalInstanceMapper extends BaseMapper<ApprovalInstance> {
}
```

文件位置：`src/main/java/io/github/atengk/approval/mapper/ApprovalTaskMapper.java`

```java
package io.github.atengk.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.approval.entity.ApprovalTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批任务 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ApprovalTaskMapper extends BaseMapper<ApprovalTask> {
}
```

文件位置：`src/main/java/io/github/atengk/approval/mapper/ApprovalRecordMapper.java`

```java
package io.github.atengk.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.approval.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {
}
```

文件位置：`src/main/java/io/github/atengk/approval/mapper/ApprovalNodeMapper.java`

```java
package io.github.atengk.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.approval.entity.ApprovalNode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批节点 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ApprovalNodeMapper extends BaseMapper<ApprovalNode> {
}
```

### 审批人计算组件

审批人计算组件用于把节点配置转换成真实审批人。实际项目中这里通常会调用组织架构服务、用户服务或角色权限服务。

本案例为了让代码可直接运行，先使用简化规则：

| 审批人类型 | 计算方式                                  |
| ---------- | ----------------------------------------- |
| `MANAGER`  | 示例中返回固定主管用户 `20001`            |
| `ROLE`     | 示例中部门经理角色返回用户 `30001`        |
| `USER`     | 直接读取 `approver_value` 中配置的用户 ID |
| `SELF`     | 返回发起人本人                            |

文件位置：`src/main/java/io/github/atengk/approval/component/ApproverResolver.java`

下面组件根据流程节点配置计算当前节点审批人。

```java
package io.github.atengk.approval.component;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.approval.constant.ApprovalConstant;
import io.github.atengk.approval.entity.ApprovalNode;
import io.github.atengk.approval.enums.ApproverTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 审批人计算组件
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
public class ApproverResolver {

    /**
     * 根据节点配置计算审批人
     *
     * @param node        流程节点
     * @param applicantId 发起人ID
     * @return 审批人ID
     */
    public Long resolveApprover(ApprovalNode node, Long applicantId) {
        if (node == null || StrUtil.isBlank(node.getApproverType())) {
            throw new IllegalArgumentException("审批节点未配置审批人类型");
        }

        String approverType = node.getApproverType();

        if (StrUtil.equals(ApproverTypeEnum.MANAGER.getCode(), approverType)) {
            // 示例：真实项目应从组织架构中查询 applicantId 的直属主管
            Long managerId = 20001L;
            log.info("计算直属主管审批人，applicantId={}，approverId={}", applicantId, managerId);
            return managerId;
        }

        if (StrUtil.equals(ApproverTypeEnum.ROLE.getCode(), approverType)) {
            // 示例：真实项目应按角色编码查询用户，可按部门、租户进一步过滤
            if (StrUtil.equals(ApprovalConstant.ROLE_DEPT_MANAGER, node.getApproverValue())) {
                Long deptManagerId = 30001L;
                log.info("计算角色审批人，role={}，approverId={}", node.getApproverValue(), deptManagerId);
                return deptManagerId;
            }
            throw new IllegalArgumentException(StrUtil.format("未匹配到角色审批人，role={}", node.getApproverValue()));
        }

        if (StrUtil.equals(ApproverTypeEnum.USER.getCode(), approverType)) {
            if (StrUtil.isBlank(node.getApproverValue())) {
                throw new IllegalArgumentException("固定审批人节点未配置用户ID");
            }
            Long userId = Convert.toLong(node.getApproverValue());
            log.info("计算固定用户审批人，approverId={}", userId);
            return userId;
        }

        if (StrUtil.equals(ApproverTypeEnum.SELF.getCode(), approverType)) {
            log.info("计算发起人本人审批，applicantId={}", applicantId);
            return applicantId;
        }

        throw new IllegalArgumentException(StrUtil.format("不支持的审批人类型，approverType={}", approverType));
    }
}
```

### 发起审批实现

发起审批需要完成四件事：

第一，校验同一业务单据不能重复发起审批。

第二，创建审批实例。

第三，查找第一个审批节点并生成待办任务。

第四，写入发起审批记录。

文件位置：`src/main/java/io/github/atengk/approval/service/ApprovalService.java`

```java
package io.github.atengk.approval.service;

import io.github.atengk.approval.dto.ApprovalHandleDTO;
import io.github.atengk.approval.dto.ApprovalStartDTO;
import io.github.atengk.approval.dto.ApprovalTransferDTO;

/**
 * 审批服务接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ApprovalService {

    /**
     * 发起审批
     *
     * @param dto 发起审批参数
     * @return 审批实例ID
     */
    Long start(ApprovalStartDTO dto);

    /**
     * 审批通过
     *
     * @param dto 审批处理参数
     */
    void pass(ApprovalHandleDTO dto);

    /**
     * 审批驳回
     *
     * @param dto 审批处理参数
     */
    void reject(ApprovalHandleDTO dto);

    /**
     * 撤回审批
     *
     * @param instanceId 审批实例ID
     * @param comment    撤回原因
     */
    void withdraw(Long instanceId, String comment);

    /**
     * 转交审批
     *
     * @param dto 转交参数
     */
    void transfer(ApprovalTransferDTO dto);
}
```

文件位置：`src/main/java/io/github/atengk/approval/service/impl/ApprovalServiceImpl.java`

下面是审批流转的核心实现，包含发起、通过、驳回、撤回、转交。

```java
package io.github.atengk.approval.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.approval.component.ApproverResolver;
import io.github.atengk.approval.dto.ApprovalHandleDTO;
import io.github.atengk.approval.dto.ApprovalStartDTO;
import io.github.atengk.approval.dto.ApprovalTransferDTO;
import io.github.atengk.approval.entity.ApprovalInstance;
import io.github.atengk.approval.entity.ApprovalNode;
import io.github.atengk.approval.entity.ApprovalRecord;
import io.github.atengk.approval.entity.ApprovalTask;
import io.github.atengk.approval.enums.ApprovalActionEnum;
import io.github.atengk.approval.enums.ApprovalNodeTypeEnum;
import io.github.atengk.approval.enums.ApprovalStatusEnum;
import io.github.atengk.approval.enums.ApprovalTaskStatusEnum;
import io.github.atengk.approval.mapper.ApprovalInstanceMapper;
import io.github.atengk.approval.mapper.ApprovalNodeMapper;
import io.github.atengk.approval.mapper.ApprovalRecordMapper;
import io.github.atengk.approval.mapper.ApprovalTaskMapper;
import io.github.atengk.approval.security.LoginUserContext;
import io.github.atengk.approval.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审批服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalInstanceMapper approvalInstanceMapper;
    private final ApprovalTaskMapper approvalTaskMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalNodeMapper approvalNodeMapper;
    private final ApproverResolver approverResolver;

    /**
     * 发起审批
     *
     * @param dto 发起审批参数
     * @return 审批实例ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(ApprovalStartDTO dto) {
        Long currentUserId = LoginUserContext.getUserId();

        Long existsCount = approvalInstanceMapper.selectCount(Wrappers.<ApprovalInstance>lambdaQuery()
                .eq(ApprovalInstance::getBizType, dto.getBizType())
                .eq(ApprovalInstance::getBizId, dto.getBizId()));

        Assert.isTrue(existsCount == 0, "该业务单据已发起审批，请勿重复提交");

        ApprovalNode firstNode = getFirstApprovalNode(dto.getBizType());
        Long instanceId = IdUtil.getSnowflakeNextId();
        LocalDateTime now = LocalDateTime.now();

        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(instanceId);
        instance.setBizType(dto.getBizType());
        instance.setBizId(dto.getBizId());
        instance.setTitle(dto.getTitle());
        instance.setApplicantId(currentUserId);
        instance.setStatus(ApprovalStatusEnum.PROCESSING.getCode());
        instance.setCurrentNodeCode(firstNode.getNodeCode());
        instance.setCurrentNodeName(firstNode.getNodeName());
        instance.setStartedAt(now);
        instance.setRemark(dto.getRemark());
        instance.setVersion(0);
        instance.setDeleted(0);
        approvalInstanceMapper.insert(instance);

        createRecord(instanceId, null, firstNode.getNodeCode(), firstNode.getNodeName(), currentUserId,
                ApprovalActionEnum.SUBMIT, null, null, dto.getRemark());

        createTask(instanceId, firstNode, currentUserId);

        log.info("发起审批成功，instanceId={}，bizType={}，bizId={}，applicantId={}",
                instanceId, dto.getBizType(), dto.getBizId(), currentUserId);
        return instanceId;
    }

    /**
     * 获取第一个审批节点
     *
     * @param bizType 业务类型
     * @return 第一个审批节点
     */
    private ApprovalNode getFirstApprovalNode(String bizType) {
        ApprovalNode node = approvalNodeMapper.selectOne(Wrappers.<ApprovalNode>lambdaQuery()
                .eq(ApprovalNode::getBizType, bizType)
                .eq(ApprovalNode::getNodeType, ApprovalNodeTypeEnum.APPROVAL.getCode())
                .eq(ApprovalNode::getEnabled, 1)
                .orderByAsc(ApprovalNode::getSortNo)
                .last("LIMIT 1"));

        Assert.notNull(node, "未配置审批流程节点");
        return node;
    }

    /**
     * 创建审批任务
     *
     * @param instanceId  审批实例ID
     * @param node        审批节点
     * @param applicantId 发起人ID
     */
    private void createTask(Long instanceId, ApprovalNode node, Long applicantId) {
        Long approverId = approverResolver.resolveApprover(node, applicantId);

        ApprovalTask task = new ApprovalTask();
        task.setId(IdUtil.getSnowflakeNextId());
        task.setInstanceId(instanceId);
        task.setNodeCode(node.getNodeCode());
        task.setNodeName(node.getNodeName());
        task.setApproverId(approverId);
        task.setStatus(ApprovalTaskStatusEnum.PENDING.getCode());
        task.setSortNo(node.getSortNo());
        task.setVersion(0);
        task.setDeleted(0);
        approvalTaskMapper.insert(task);

        log.info("创建审批待办成功，instanceId={}，nodeCode={}，approverId={}",
                instanceId, node.getNodeCode(), approverId);
    }

    /**
     * 创建审批记录
     *
     * @param instanceId 审批实例ID
     * @param taskId     任务ID
     * @param nodeCode   节点编码
     * @param nodeName   节点名称
     * @param operatorId 操作人ID
     * @param actionEnum 操作枚举
     * @param fromUserId 来源用户ID
     * @param toUserId   目标用户ID
     * @param comment    操作意见
     */
    private void createRecord(Long instanceId, Long taskId, String nodeCode, String nodeName,
                              Long operatorId, ApprovalActionEnum actionEnum,
                              Long fromUserId, Long toUserId, String comment) {
        ApprovalRecord record = new ApprovalRecord();
        record.setId(IdUtil.getSnowflakeNextId());
        record.setInstanceId(instanceId);
        record.setTaskId(taskId);
        record.setNodeCode(nodeCode);
        record.setNodeName(nodeName);
        record.setOperatorId(operatorId);
        record.setAction(actionEnum.getCode());
        record.setActionDesc(actionEnum.getDesc());
        record.setFromUserId(fromUserId);
        record.setToUserId(toUserId);
        record.setComment(comment);
        record.setCreateTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);
    }
}
```

### 审批通过实现

审批通过是流程流转的核心。处理逻辑是：先完成当前任务，再判断是否存在下一个审批节点；如果有，创建下一节点任务；如果没有，审批实例变为已通过。

下面代码放入 `ApprovalServiceImpl` 中。

```java
/**
 * 审批通过
 *
 * @param dto 审批处理参数
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void pass(ApprovalHandleDTO dto) {
    Long currentUserId = LoginUserContext.getUserId();

    ApprovalTask task = getPendingTask(dto.getTaskId(), currentUserId);
    ApprovalInstance instance = getProcessingInstance(task.getInstanceId());

    task.setStatus(ApprovalTaskStatusEnum.PASSED.getCode());
    task.setHandleTime(LocalDateTime.now());
    task.setRemark(dto.getComment());
    int updateTaskCount = approvalTaskMapper.updateById(task);
    Assert.isTrue(updateTaskCount == 1, "审批任务已被处理，请刷新后重试");

    createRecord(instance.getId(), task.getId(), task.getNodeCode(), task.getNodeName(), currentUserId,
            ApprovalActionEnum.PASS, null, null, dto.getComment());

    ApprovalNode nextNode = getNextApprovalNode(instance.getBizType(), task.getSortNo());
    if (nextNode == null) {
        instance.setStatus(ApprovalStatusEnum.PASSED.getCode());
        instance.setCurrentNodeCode("END");
        instance.setCurrentNodeName("流程结束");
        instance.setFinishedAt(LocalDateTime.now());

        int updateInstanceCount = approvalInstanceMapper.updateById(instance);
        Assert.isTrue(updateInstanceCount == 1, "审批实例状态已变更，请刷新后重试");

        log.info("审批流程已通过，instanceId={}，operatorId={}", instance.getId(), currentUserId);
        return;
    }

    instance.setCurrentNodeCode(nextNode.getNodeCode());
    instance.setCurrentNodeName(nextNode.getNodeName());
    int updateInstanceCount = approvalInstanceMapper.updateById(instance);
    Assert.isTrue(updateInstanceCount == 1, "审批实例状态已变更，请刷新后重试");

    createTask(instance.getId(), nextNode, instance.getApplicantId());

    log.info("审批通过并流转下一节点，instanceId={}，fromNode={}，toNode={}，operatorId={}",
            instance.getId(), task.getNodeCode(), nextNode.getNodeCode(), currentUserId);
}

/**
 * 获取当前用户待处理任务
 *
 * @param taskId        任务ID
 * @param currentUserId 当前用户ID
 * @return 待处理任务
 */
private ApprovalTask getPendingTask(Long taskId, Long currentUserId) {
    ApprovalTask task = approvalTaskMapper.selectById(taskId);
    Assert.notNull(task, "审批任务不存在");

    Assert.isTrue(StrUtil.equals(task.getStatus(), ApprovalTaskStatusEnum.PENDING.getCode()),
            "审批任务不是待处理状态");
    Assert.isTrue(currentUserId.equals(task.getApproverId()),
            "当前用户不是该任务审批人，无权处理");

    return task;
}

/**
 * 获取审批中的实例
 *
 * @param instanceId 审批实例ID
 * @return 审批实例
 */
private ApprovalInstance getProcessingInstance(Long instanceId) {
    ApprovalInstance instance = approvalInstanceMapper.selectById(instanceId);
    Assert.notNull(instance, "审批实例不存在");

    Assert.isTrue(StrUtil.equals(instance.getStatus(), ApprovalStatusEnum.PROCESSING.getCode()),
            "审批实例不是审批中状态");

    return instance;
}

/**
 * 获取下一个审批节点
 *
 * @param bizType       业务类型
 * @param currentSortNo 当前节点排序号
 * @return 下一个审批节点，不存在则返回 null
 */
private ApprovalNode getNextApprovalNode(String bizType, Integer currentSortNo) {
    return approvalNodeMapper.selectOne(Wrappers.<ApprovalNode>lambdaQuery()
            .eq(ApprovalNode::getBizType, bizType)
            .eq(ApprovalNode::getNodeType, ApprovalNodeTypeEnum.APPROVAL.getCode())
            .eq(ApprovalNode::getEnabled, 1)
            .gt(ApprovalNode::getSortNo, currentSortNo)
            .orderByAsc(ApprovalNode::getSortNo)
            .last("LIMIT 1"));
}
```

### 审批驳回实现

审批驳回采用“直接终止流程”策略。当前审批人驳回后，当前任务变为已驳回，审批实例变为已驳回，并写入审批记录。

下面代码放入 `ApprovalServiceImpl` 中。

```java
/**
 * 审批驳回
 *
 * @param dto 审批处理参数
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void reject(ApprovalHandleDTO dto) {
    Long currentUserId = LoginUserContext.getUserId();

    ApprovalTask task = getPendingTask(dto.getTaskId(), currentUserId);
    ApprovalInstance instance = getProcessingInstance(task.getInstanceId());

    task.setStatus(ApprovalTaskStatusEnum.REJECTED.getCode());
    task.setHandleTime(LocalDateTime.now());
    task.setRemark(dto.getComment());
    int updateTaskCount = approvalTaskMapper.updateById(task);
    Assert.isTrue(updateTaskCount == 1, "审批任务已被处理，请刷新后重试");

    instance.setStatus(ApprovalStatusEnum.REJECTED.getCode());
    instance.setFinishedAt(LocalDateTime.now());
    int updateInstanceCount = approvalInstanceMapper.updateById(instance);
    Assert.isTrue(updateInstanceCount == 1, "审批实例状态已变更，请刷新后重试");

    createRecord(instance.getId(), task.getId(), task.getNodeCode(), task.getNodeName(), currentUserId,
            ApprovalActionEnum.REJECT, null, null, dto.getComment());

    log.info("审批驳回成功，instanceId={}，taskId={}，operatorId={}",
            instance.getId(), task.getId(), currentUserId);
}
```

### 审批撤回实现

审批撤回只能由发起人操作，且审批实例必须还在审批中。撤回后，需要取消当前所有待处理任务，并将审批实例改为已撤回。

下面代码放入 `ApprovalServiceImpl` 中。

```java
/**
 * 撤回审批
 *
 * @param instanceId 审批实例ID
 * @param comment    撤回原因
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void withdraw(Long instanceId, String comment) {
    Long currentUserId = LoginUserContext.getUserId();

    ApprovalInstance instance = getProcessingInstance(instanceId);
    Assert.isTrue(currentUserId.equals(instance.getApplicantId()), "只有审批发起人可以撤回审批");

    ApprovalTask updateTask = new ApprovalTask();
    updateTask.setStatus(ApprovalTaskStatusEnum.CANCELED.getCode());
    updateTask.setRemark(StrUtil.blankToDefault(comment, "发起人撤回审批"));
    updateTask.setHandleTime(LocalDateTime.now());

    approvalTaskMapper.update(updateTask, Wrappers.<ApprovalTask>lambdaUpdate()
            .eq(ApprovalTask::getInstanceId, instanceId)
            .eq(ApprovalTask::getStatus, ApprovalTaskStatusEnum.PENDING.getCode()));

    instance.setStatus(ApprovalStatusEnum.WITHDRAWN.getCode());
    instance.setFinishedAt(LocalDateTime.now());
    int updateInstanceCount = approvalInstanceMapper.updateById(instance);
    Assert.isTrue(updateInstanceCount == 1, "审批实例状态已变更，请刷新后重试");

    createRecord(instance.getId(), null, instance.getCurrentNodeCode(), instance.getCurrentNodeName(), currentUserId,
            ApprovalActionEnum.WITHDRAW, null, null, comment);

    log.info("审批撤回成功，instanceId={}，operatorId={}", instanceId, currentUserId);
}
```

### 审批转交实现

审批转交用于当前审批人把待办转给其他人。这里采用“直接修改当前任务审批人”的方式实现，并保留原审批人字段和转交记录。

这种方案简单、直观，适合轻量审批系统。如果后续需要展示“原任务已转出、新任务待处理”，也可以改成“关闭原任务 + 创建新任务”的模式。

下面代码放入 `ApprovalServiceImpl` 中。

```java
/**
 * 转交审批
 *
 * @param dto 转交参数
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void transfer(ApprovalTransferDTO dto) {
    Long currentUserId = LoginUserContext.getUserId();

    ApprovalTask task = getPendingTask(dto.getTaskId(), currentUserId);
    ApprovalInstance instance = getProcessingInstance(task.getInstanceId());

    Assert.isFalse(currentUserId.equals(dto.getToUserId()), "不能转交给自己");

    Long oldApproverId = task.getApproverId();
    task.setOriginalApproverId(oldApproverId);
    task.setApproverId(dto.getToUserId());
    task.setRemark(dto.getComment());

    int updateTaskCount = approvalTaskMapper.updateById(task);
    Assert.isTrue(updateTaskCount == 1, "审批任务已被处理，请刷新后重试");

    createRecord(instance.getId(), task.getId(), task.getNodeCode(), task.getNodeName(), currentUserId,
            ApprovalActionEnum.TRANSFER, oldApproverId, dto.getToUserId(), dto.getComment());

    log.info("审批转交成功，instanceId={}，taskId={}，fromUserId={}，toUserId={}",
            instance.getId(), task.getId(), oldApproverId, dto.getToUserId());
}
```

如果你希望转交后原任务状态也体现为 `TRANSFERRED`，可以采用下面这种增强版实现：关闭原任务，再创建一条新任务。

```java
/**
 * 转交审批增强版：关闭原任务并创建新任务
 *
 * @param dto 转交参数
 */
@Transactional(rollbackFor = Exception.class)
public void transferWithNewTask(ApprovalTransferDTO dto) {
    Long currentUserId = LoginUserContext.getUserId();

    ApprovalTask oldTask = getPendingTask(dto.getTaskId(), currentUserId);
    ApprovalInstance instance = getProcessingInstance(oldTask.getInstanceId());

    Assert.isFalse(currentUserId.equals(dto.getToUserId()), "不能转交给自己");

    oldTask.setStatus(ApprovalTaskStatusEnum.TRANSFERRED.getCode());
    oldTask.setHandleTime(LocalDateTime.now());
    oldTask.setRemark(dto.getComment());
    int updateTaskCount = approvalTaskMapper.updateById(oldTask);
    Assert.isTrue(updateTaskCount == 1, "审批任务已被处理，请刷新后重试");

    ApprovalTask newTask = new ApprovalTask();
    newTask.setId(IdUtil.getSnowflakeNextId());
    newTask.setInstanceId(oldTask.getInstanceId());
    newTask.setNodeCode(oldTask.getNodeCode());
    newTask.setNodeName(oldTask.getNodeName());
    newTask.setApproverId(dto.getToUserId());
    newTask.setOriginalApproverId(currentUserId);
    newTask.setStatus(ApprovalTaskStatusEnum.PENDING.getCode());
    newTask.setSortNo(oldTask.getSortNo());
    newTask.setVersion(0);
    newTask.setDeleted(0);
    approvalTaskMapper.insert(newTask);

    createRecord(instance.getId(), oldTask.getId(), oldTask.getNodeCode(), oldTask.getNodeName(), currentUserId,
            ApprovalActionEnum.TRANSFER, currentUserId, dto.getToUserId(), dto.getComment());

    log.info("审批转交成功，原任务关闭并创建新任务，instanceId={}，oldTaskId={}，newTaskId={}，toUserId={}",
            instance.getId(), oldTask.getId(), newTask.getId(), dto.getToUserId());
}
```

当前核心代码实现后，审批流已经具备完整主链路：

```text
发起审批
-> 创建审批实例
-> 创建第一个审批任务
-> 审批通过
-> 创建下一节点任务
-> 最后一个节点通过
-> 审批实例变为已通过
```

异常分支也已经覆盖：

```text
审批驳回 -> 审批实例终止
审批撤回 -> 待办取消，审批实例撤回
审批转交 -> 待办审批人变更，记录转交轨迹
```

下一步可以继续补充查询接口实现，包括我的待办、我的已办、审批详情和审批记录时间线。

## 查询接口实现

查询接口主要服务于审批后台页面，包括“我的待办”“我的已办”“审批详情”“审批记录时间线”。这些能力对应 README 中提到的“待办 / 已办查询”和“审批记录留痕”。

为了避免 Controller 直接暴露数据库实体，这里先定义 VO 对象，再在 Service 层组装返回数据。

### 我的待办查询

我的待办查询用于审批人查看当前需要自己处理的任务，只查询 `approval_task.status = PENDING` 且 `approver_id = 当前登录用户` 的任务。

文件位置：`src/main/java/io/github/atengk/approval/vo/ApprovalTaskVO.java`

下面 VO 用于返回待办和已办任务列表。

```java
package io.github.atengk.approval.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批任务展示对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ApprovalTaskVO {

    private Long taskId;

    private Long instanceId;

    private String bizType;

    private Long bizId;

    private String title;

    private Long applicantId;

    private String nodeCode;

    private String nodeName;

    private Long approverId;

    private String status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime handleTime;
}
```

在 `ApprovalService` 中增加查询方法。

文件位置：`src/main/java/io/github/atengk/approval/service/ApprovalService.java`

```java
package io.github.atengk.approval.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.approval.dto.ApprovalHandleDTO;
import io.github.atengk.approval.dto.ApprovalStartDTO;
import io.github.atengk.approval.dto.ApprovalTransferDTO;
import io.github.atengk.approval.vo.ApprovalDetailVO;
import io.github.atengk.approval.vo.ApprovalRecordVO;
import io.github.atengk.approval.vo.ApprovalTaskVO;

import java.util.List;

/**
 * 审批服务接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ApprovalService {

    /**
     * 发起审批
     *
     * @param dto 发起审批参数
     * @return 审批实例ID
     */
    Long start(ApprovalStartDTO dto);

    /**
     * 审批通过
     *
     * @param dto 审批处理参数
     */
    void pass(ApprovalHandleDTO dto);

    /**
     * 审批驳回
     *
     * @param dto 审批处理参数
     */
    void reject(ApprovalHandleDTO dto);

    /**
     * 撤回审批
     *
     * @param instanceId 审批实例ID
     * @param comment    撤回原因
     */
    void withdraw(Long instanceId, String comment);

    /**
     * 转交审批
     *
     * @param dto 转交参数
     */
    void transfer(ApprovalTransferDTO dto);

    /**
     * 查询我的待办
     *
     * @param current 当前页
     * @param size    每页数量
     * @return 待办分页
     */
    Page<ApprovalTaskVO> pageTodo(Long current, Long size);

    /**
     * 查询我的已办
     *
     * @param current 当前页
     * @param size    每页数量
     * @return 已办分页
     */
    Page<ApprovalTaskVO> pageDone(Long current, Long size);

    /**
     * 查询审批详情
     *
     * @param instanceId 审批实例ID
     * @return 审批详情
     */
    ApprovalDetailVO detail(Long instanceId);

    /**
     * 查询审批记录时间线
     *
     * @param instanceId 审批实例ID
     * @return 审批记录列表
     */
    List<ApprovalRecordVO> records(Long instanceId);
}
```

在 `ApprovalServiceImpl` 中增加待办查询实现。

文件位置：`src/main/java/io/github/atengk/approval/service/impl/ApprovalServiceImpl.java`

下面代码查询当前登录用户的待办任务，并补充审批实例标题、业务类型、业务单据 ID 等信息。

```java
/**
 * 查询我的待办
 *
 * @param current 当前页
 * @param size    每页数量
 * @return 待办分页
 */
@Override
public Page<ApprovalTaskVO> pageTodo(Long current, Long size) {
    Long currentUserId = LoginUserContext.getUserId();

    Page<ApprovalTask> taskPage = approvalTaskMapper.selectPage(
            Page.of(current, size),
            Wrappers.<ApprovalTask>lambdaQuery()
                    .eq(ApprovalTask::getApproverId, currentUserId)
                    .eq(ApprovalTask::getStatus, ApprovalTaskStatusEnum.PENDING.getCode())
                    .orderByDesc(ApprovalTask::getCreateTime)
    );

    log.info("查询我的待办，userId={}，current={}，size={}，total={}",
            currentUserId, current, size, taskPage.getTotal());

    return convertTaskPage(taskPage);
}
```

### 我的已办查询

我的已办查询用于审批人查看自己已经处理过的任务，只查询当前用户处理过且状态已经不是待处理的审批任务。

文件位置：`src/main/java/io/github/atengk/approval/service/impl/ApprovalServiceImpl.java`

下面代码查询当前登录用户已通过、已驳回或已转交的审批任务。

```java
/**
 * 查询我的已办
 *
 * @param current 当前页
 * @param size    每页数量
 * @return 已办分页
 */
@Override
public Page<ApprovalTaskVO> pageDone(Long current, Long size) {
    Long currentUserId = LoginUserContext.getUserId();

    Page<ApprovalTask> taskPage = approvalTaskMapper.selectPage(
            Page.of(current, size),
            Wrappers.<ApprovalTask>lambdaQuery()
                    .eq(ApprovalTask::getApproverId, currentUserId)
                    .in(ApprovalTask::getStatus,
                            ApprovalTaskStatusEnum.PASSED.getCode(),
                            ApprovalTaskStatusEnum.REJECTED.getCode(),
                            ApprovalTaskStatusEnum.TRANSFERRED.getCode())
                    .orderByDesc(ApprovalTask::getHandleTime)
    );

    log.info("查询我的已办，userId={}，current={}，size={}，total={}",
            currentUserId, current, size, taskPage.getTotal());

    return convertTaskPage(taskPage);
}
```

下面是待办 / 已办共用的分页转换方法，放在 `ApprovalServiceImpl` 中。

```java
/**
 * 转换审批任务分页
 *
 * @param taskPage 任务分页
 * @return 展示分页
 */
private Page<ApprovalTaskVO> convertTaskPage(Page<ApprovalTask> taskPage) {
    Page<ApprovalTaskVO> voPage = Page.of(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());

    if (CollUtil.isEmpty(taskPage.getRecords())) {
        voPage.setRecords(List.of());
        return voPage;
    }

    List<Long> instanceIds = taskPage.getRecords()
            .stream()
            .map(ApprovalTask::getInstanceId)
            .distinct()
            .toList();

    Map<Long, ApprovalInstance> instanceMap = approvalInstanceMapper.selectBatchIds(instanceIds)
            .stream()
            .collect(Collectors.toMap(ApprovalInstance::getId, item -> item));

    List<ApprovalTaskVO> records = taskPage.getRecords()
            .stream()
            .map(task -> {
                ApprovalInstance instance = instanceMap.get(task.getInstanceId());

                ApprovalTaskVO vo = new ApprovalTaskVO();
                vo.setTaskId(task.getId());
                vo.setInstanceId(task.getInstanceId());
                vo.setNodeCode(task.getNodeCode());
                vo.setNodeName(task.getNodeName());
                vo.setApproverId(task.getApproverId());
                vo.setStatus(task.getStatus());
                vo.setRemark(task.getRemark());
                vo.setCreateTime(task.getCreateTime());
                vo.setHandleTime(task.getHandleTime());

                if (instance != null) {
                    vo.setBizType(instance.getBizType());
                    vo.setBizId(instance.getBizId());
                    vo.setTitle(instance.getTitle());
                    vo.setApplicantId(instance.getApplicantId());
                }

                return vo;
            })
            .toList();

    voPage.setRecords(records);
    return voPage;
}
```

上面代码需要在 `ApprovalServiceImpl` 顶部补充导入。

```java
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
```

### 审批详情查询

审批详情用于展示某一条审批实例的完整信息，包括审批单主信息、当前待办任务和审批记录时间线。

文件位置：`src/main/java/io/github/atengk/approval/vo/ApprovalDetailVO.java`

下面 VO 用于返回审批详情。

```java
package io.github.atengk.approval.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批详情展示对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ApprovalDetailVO {

    private Long instanceId;

    private String bizType;

    private Long bizId;

    private String title;

    private Long applicantId;

    private String status;

    private String currentNodeCode;

    private String currentNodeName;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String remark;

    private List<ApprovalTaskVO> pendingTasks;

    private List<ApprovalRecordVO> records;
}
```

文件位置：`src/main/java/io/github/atengk/approval/service/impl/ApprovalServiceImpl.java`

下面代码查询审批单、当前待办和审批记录，组合成审批详情。

```java
/**
 * 查询审批详情
 *
 * @param instanceId 审批实例ID
 * @return 审批详情
 */
@Override
public ApprovalDetailVO detail(Long instanceId) {
    ApprovalInstance instance = approvalInstanceMapper.selectById(instanceId);
    Assert.notNull(instance, "审批实例不存在");

    List<ApprovalTask> pendingTasks = approvalTaskMapper.selectList(Wrappers.<ApprovalTask>lambdaQuery()
            .eq(ApprovalTask::getInstanceId, instanceId)
            .eq(ApprovalTask::getStatus, ApprovalTaskStatusEnum.PENDING.getCode())
            .orderByAsc(ApprovalTask::getCreateTime));

    List<ApprovalTaskVO> pendingTaskVOList = pendingTasks.stream()
            .map(task -> {
                ApprovalTaskVO vo = new ApprovalTaskVO();
                vo.setTaskId(task.getId());
                vo.setInstanceId(task.getInstanceId());
                vo.setBizType(instance.getBizType());
                vo.setBizId(instance.getBizId());
                vo.setTitle(instance.getTitle());
                vo.setApplicantId(instance.getApplicantId());
                vo.setNodeCode(task.getNodeCode());
                vo.setNodeName(task.getNodeName());
                vo.setApproverId(task.getApproverId());
                vo.setStatus(task.getStatus());
                vo.setRemark(task.getRemark());
                vo.setCreateTime(task.getCreateTime());
                vo.setHandleTime(task.getHandleTime());
                return vo;
            })
            .toList();

    ApprovalDetailVO vo = new ApprovalDetailVO();
    vo.setInstanceId(instance.getId());
    vo.setBizType(instance.getBizType());
    vo.setBizId(instance.getBizId());
    vo.setTitle(instance.getTitle());
    vo.setApplicantId(instance.getApplicantId());
    vo.setStatus(instance.getStatus());
    vo.setCurrentNodeCode(instance.getCurrentNodeCode());
    vo.setCurrentNodeName(instance.getCurrentNodeName());
    vo.setStartedAt(instance.getStartedAt());
    vo.setFinishedAt(instance.getFinishedAt());
    vo.setRemark(instance.getRemark());
    vo.setPendingTasks(pendingTaskVOList);
    vo.setRecords(records(instanceId));

    log.info("查询审批详情，instanceId={}，status={}", instanceId, instance.getStatus());
    return vo;
}
```

实际项目中建议在 `detail` 方法中增加权限控制，例如只允许发起人、当前审批人、历史审批人、管理员查看。

### 审批记录时间线查询

审批记录时间线用于展示审批过程，例如“谁在什么时候提交、谁审批通过、谁驳回、谁转交”。

文件位置：`src/main/java/io/github/atengk/approval/vo/ApprovalRecordVO.java`

下面 VO 用于返回审批记录。

```java
package io.github.atengk.approval.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批记录展示对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ApprovalRecordVO {

    private Long recordId;

    private Long instanceId;

    private Long taskId;

    private String nodeCode;

    private String nodeName;

    private Long operatorId;

    private String action;

    private String actionDesc;

    private Long fromUserId;

    private Long toUserId;

    private String comment;

    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/approval/service/impl/ApprovalServiceImpl.java`

下面代码按创建时间升序返回审批时间线。

```java
/**
 * 查询审批记录时间线
 *
 * @param instanceId 审批实例ID
 * @return 审批记录列表
 */
@Override
public List<ApprovalRecordVO> records(Long instanceId) {
    List<ApprovalRecord> recordList = approvalRecordMapper.selectList(Wrappers.<ApprovalRecord>lambdaQuery()
            .eq(ApprovalRecord::getInstanceId, instanceId)
            .orderByAsc(ApprovalRecord::getCreateTime));

    return recordList.stream()
            .map(record -> {
                ApprovalRecordVO vo = new ApprovalRecordVO();
                vo.setRecordId(record.getId());
                vo.setInstanceId(record.getInstanceId());
                vo.setTaskId(record.getTaskId());
                vo.setNodeCode(record.getNodeCode());
                vo.setNodeName(record.getNodeName());
                vo.setOperatorId(record.getOperatorId());
                vo.setAction(record.getAction());
                vo.setActionDesc(record.getActionDesc());
                vo.setFromUserId(record.getFromUserId());
                vo.setToUserId(record.getToUserId());
                vo.setComment(record.getComment());
                vo.setCreateTime(record.getCreateTime());
                return vo;
            })
            .toList();
}
```

## Controller 接口设计

Controller 层负责暴露 REST API，核心接口分为五类：发起审批、审批处理、撤回审批、转交审批、查询审批。

接口统一返回 `ApiResult<T>`。如果你的项目已经有统一返回对象，可以直接替换掉这里的 `ApiResult`。

文件位置：`src/main/java/io/github/atengk/approval/common/ApiResult.java`

下面类用于封装接口统一返回结构。

```java
package io.github.atengk.approval.common;

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
     * 返回成功结果
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一返回结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 返回成功结果
     *
     * @return 统一返回结果
     */
    public static ApiResult<Void> success() {
        return new ApiResult<>(200, "操作成功", null);
    }

    /**
     * 返回失败结果
     *
     * @param message 错误信息
     * @return 统一返回结果
     */
    public static ApiResult<Void> fail(String message) {
        return new ApiResult<>(500, message, null);
    }
}
```

### 发起审批接口

发起审批接口接收业务类型、业务单据 ID、标题和说明，创建审批实例并生成第一条待办任务。

### 处理审批接口

处理审批分为“通过”和“驳回”两个接口。两者都基于 `taskId` 操作，必须保证当前登录用户就是任务审批人。

### 撤回审批接口

撤回审批基于 `instanceId` 操作，只允许审批发起人撤回，且审批实例必须处于审批中。

### 转交审批接口

转交审批基于 `taskId` 操作，只允许当前审批人转交当前待办任务。

### 查询审批接口

查询接口包括我的待办、我的已办、审批详情和审批记录时间线。

文件位置：`src/main/java/io/github/atengk/approval/controller/ApprovalController.java`

下面 Controller 提供审批流核心 REST API。

```java
package io.github.atengk.approval.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.approval.common.ApiResult;
import io.github.atengk.approval.dto.ApprovalHandleDTO;
import io.github.atengk.approval.dto.ApprovalStartDTO;
import io.github.atengk.approval.dto.ApprovalTransferDTO;
import io.github.atengk.approval.service.ApprovalService;
import io.github.atengk.approval.vo.ApprovalDetailVO;
import io.github.atengk.approval.vo.ApprovalRecordVO;
import io.github.atengk.approval.vo.ApprovalTaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批流接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/approval")
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * 发起审批
     *
     * @param dto 发起审批参数
     * @return 审批实例ID
     */
    @PostMapping("/start")
    public ApiResult<Long> start(@Valid @RequestBody ApprovalStartDTO dto) {
        return ApiResult.success(approvalService.start(dto));
    }

    /**
     * 审批通过
     *
     * @param dto 审批处理参数
     * @return 操作结果
     */
    @PostMapping("/pass")
    public ApiResult<Void> pass(@Valid @RequestBody ApprovalHandleDTO dto) {
        approvalService.pass(dto);
        return ApiResult.success();
    }

    /**
     * 审批驳回
     *
     * @param dto 审批处理参数
     * @return 操作结果
     */
    @PostMapping("/reject")
    public ApiResult<Void> reject(@Valid @RequestBody ApprovalHandleDTO dto) {
        approvalService.reject(dto);
        return ApiResult.success();
    }

    /**
     * 撤回审批
     *
     * @param instanceId 审批实例ID
     * @param comment    撤回原因
     * @return 操作结果
     */
    @PostMapping("/{instanceId}/withdraw")
    public ApiResult<Void> withdraw(@PathVariable Long instanceId,
                                    @RequestParam(required = false) String comment) {
        approvalService.withdraw(instanceId, comment);
        return ApiResult.success();
    }

    /**
     * 转交审批
     *
     * @param dto 转交参数
     * @return 操作结果
     */
    @PostMapping("/transfer")
    public ApiResult<Void> transfer(@Valid @RequestBody ApprovalTransferDTO dto) {
        approvalService.transfer(dto);
        return ApiResult.success();
    }

    /**
     * 查询我的待办
     *
     * @param current 当前页
     * @param size    每页数量
     * @return 待办分页
     */
    @GetMapping("/tasks/todo")
    public ApiResult<Page<ApprovalTaskVO>> todo(@RequestParam(defaultValue = "1") Long current,
                                                @RequestParam(defaultValue = "10") Long size) {
        return ApiResult.success(approvalService.pageTodo(current, size));
    }

    /**
     * 查询我的已办
     *
     * @param current 当前页
     * @param size    每页数量
     * @return 已办分页
     */
    @GetMapping("/tasks/done")
    public ApiResult<Page<ApprovalTaskVO>> done(@RequestParam(defaultValue = "1") Long current,
                                                @RequestParam(defaultValue = "10") Long size) {
        return ApiResult.success(approvalService.pageDone(current, size));
    }

    /**
     * 查询审批详情
     *
     * @param instanceId 审批实例ID
     * @return 审批详情
     */
    @GetMapping("/{instanceId}")
    public ApiResult<ApprovalDetailVO> detail(@PathVariable Long instanceId) {
        return ApiResult.success(approvalService.detail(instanceId));
    }

    /**
     * 查询审批记录时间线
     *
     * @param instanceId 审批实例ID
     * @return 审批记录列表
     */
    @GetMapping("/{instanceId}/records")
    public ApiResult<List<ApprovalRecordVO>> records(@PathVariable Long instanceId) {
        return ApiResult.success(approvalService.records(instanceId));
    }
}
```

本地接口调用示例。

先用前面提供的测试登录接口模拟员工登录，发起请假审批。

```bash
# 员工 10001 登录
TOKEN_10001=$(curl -s -X POST http://localhost:8080/auth/test/login/10001)

# 发起请假审批
curl -X POST http://localhost:8080/approval/start \
  -H "Authorization: ${TOKEN_10001}" \
  -H "Content-Type: application/json" \
  -d '{
    "bizType": "LEAVE",
    "bizId": 90001,
    "title": "张三请假审批",
    "remark": "请假 2 天，回家处理事务"
  }'
```

主管用户查询待办并审批通过。

```bash
# 主管 20001 登录
TOKEN_20001=$(curl -s -X POST http://localhost:8080/auth/test/login/20001)

# 查询主管待办
curl -X GET "http://localhost:8080/approval/tasks/todo?current=1&size=10" \
  -H "Authorization: ${TOKEN_20001}"

# 审批通过，taskId 替换为待办查询返回的 taskId
curl -X POST http://localhost:8080/approval/pass \
  -H "Authorization: ${TOKEN_20001}" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": 你的任务ID,
    "comment": "同意，请注意工作交接"
  }'
```

部门经理查询待办并审批通过。

```bash
# 部门经理 30001 登录
TOKEN_30001=$(curl -s -X POST http://localhost:8080/auth/test/login/30001)

# 查询部门经理待办
curl -X GET "http://localhost:8080/approval/tasks/todo?current=1&size=10" \
  -H "Authorization: ${TOKEN_30001}"

# 部门经理审批通过
curl -X POST http://localhost:8080/approval/pass \
  -H "Authorization: ${TOKEN_30001}" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": 你的任务ID,
    "comment": "同意"
  }'
```

查询审批详情和审批时间线。

```bash
# 查询审批详情，instanceId 替换为发起审批返回的审批实例ID
curl -X GET http://localhost:8080/approval/你的审批实例ID \
  -H "Authorization: ${TOKEN_10001}"

# 查询审批记录时间线
curl -X GET http://localhost:8080/approval/你的审批实例ID/records \
  -H "Authorization: ${TOKEN_10001}"
```

撤回审批示例。只有发起人能撤回，且审批实例必须仍处于 `PROCESSING` 状态。

```bash
curl -X POST "http://localhost:8080/approval/你的审批实例ID/withdraw?comment=申请信息填写错误，重新提交" \
  -H "Authorization: ${TOKEN_10001}"
```

转交审批示例。只有当前待办审批人能转交。

```bash
curl -X POST http://localhost:8080/approval/transfer \
  -H "Authorization: ${TOKEN_20001}" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": 你的任务ID,
    "toUserId": 20002,
    "comment": "我近期出差，转交给同级主管处理"
  }'
```

到这里，审批流的核心接口已经闭环：

```text
发起审批
-> 查询待办
-> 审批通过 / 审批驳回
-> 撤回审批
-> 转交审批
-> 查询已办
-> 查询详情
-> 查询审批记录时间线
```

这一套实现已经可以支撑请假、报销、采购、合同等基础审批场景。后续要扩展会签、或签、加签时，主要改造点集中在 `approval_node` 节点配置、`ApproverResolver` 审批人计算和 `ApprovalServiceImpl#createTask` 任务生成逻辑。

## 接口测试示例

本节基于前面实现的测试登录接口、审批 Controller 和请假审批节点配置进行验证。测试链路覆盖“发起审批、一级主管通过、二级主管驳回、查询待办与审批记录”，对应审批流场景中的核心流转能力。

### 发起请假审批

先模拟员工 `10001` 登录，然后发起一条请假审批。这里的 `bizId` 要保证没有重复发起过审批，因为前面的 `approval_instance` 表对 `biz_type + biz_id` 做了唯一约束。

```bash
# 员工 10001 模拟登录，返回 Token
TOKEN_10001=$(curl -s -X POST http://localhost:8080/auth/test/login/10001)

# 发起请假审批
curl -X POST http://localhost:8080/approval/start \
  -H "Authorization: ${TOKEN_10001}" \
  -H "Content-Type: application/json" \
  -d '{
    "bizType": "LEAVE",
    "bizId": 90002,
    "title": "张三请假审批",
    "remark": "请假 2 天，回家处理个人事务"
  }'
```

正常返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1979012345678901248
}
```

返回的 `data` 就是审批实例 ID，后续查询详情和审批记录时会用到。

此时数据库中应该产生三类数据：

| 表                  | 预期数据                               |
| ------------------- | -------------------------------------- |
| `approval_instance` | 生成一条审批实例，状态为 `PROCESSING`  |
| `approval_task`     | 生成一条直属主管待办，审批人为 `20001` |
| `approval_record`   | 生成一条 `SUBMIT` 发起审批记录         |

可以用 SQL 快速验证。

```sql
-- 查询审批实例
SELECT id, biz_type, biz_id, title, applicant_id, status, current_node_code, current_node_name
FROM approval_instance
WHERE biz_type = 'LEAVE'
  AND biz_id = 90002;

-- 查询当前待办任务
SELECT id, instance_id, node_code, node_name, approver_id, status
FROM approval_task
WHERE instance_id = 1979012345678901248;

-- 查询审批记录
SELECT instance_id, node_name, operator_id, action, action_desc, comment, create_time
FROM approval_record
WHERE instance_id = 1979012345678901248
ORDER BY create_time ASC;
```

### 一级主管审批通过

根据前面的 `ApproverResolver` 示例规则，直属主管审批人是 `20001`。先模拟主管登录，再查询待办，取出返回结果中的 `taskId`。

```bash
# 直属主管 20001 模拟登录
TOKEN_20001=$(curl -s -X POST http://localhost:8080/auth/test/login/20001)

# 查询直属主管待办
curl -X GET "http://localhost:8080/approval/tasks/todo?current=1&size=10" \
  -H "Authorization: ${TOKEN_20001}"
```

待办返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "taskId": 1979012456789000001,
        "instanceId": 1979012345678901248,
        "bizType": "LEAVE",
        "bizId": 90002,
        "title": "张三请假审批",
        "applicantId": 10001,
        "nodeCode": "MANAGER_APPROVE",
        "nodeName": "直属主管审批",
        "approverId": 20001,
        "status": "PENDING"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

使用 `taskId` 执行审批通过。

```bash
# 直属主管审批通过
curl -X POST http://localhost:8080/approval/pass \
  -H "Authorization: ${TOKEN_20001}" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": 1979012456789000001,
    "comment": "同意，请提前做好工作交接"
  }'
```

正常返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

通过后预期结果：

| 对象         | 预期变化                            |
| ------------ | ----------------------------------- |
| 直属主管任务 | `status` 从 `PENDING` 变为 `PASSED` |
| 审批实例     | 仍然是 `PROCESSING`                 |
| 当前节点     | 流转到 `DEPT_MANAGER_APPROVE`       |
| 新待办       | 生成部门经理 `30001` 的待办任务     |
| 审批记录     | 新增一条 `PASS` 记录                |

可以继续用 SQL 验证。

```sql
-- 查看任务流转情况
SELECT id, node_code, node_name, approver_id, status, handle_time, remark
FROM approval_task
WHERE instance_id = 1979012345678901248
ORDER BY create_time ASC;

-- 查看审批实例当前节点
SELECT id, status, current_node_code, current_node_name
FROM approval_instance
WHERE id = 1979012345678901248;
```

### 二级主管审批驳回

根据前面的 `ApproverResolver` 示例规则，部门经理审批人是 `30001`。一级主管通过后，部门经理会收到新的待办。

```bash
# 部门经理 30001 模拟登录
TOKEN_30001=$(curl -s -X POST http://localhost:8080/auth/test/login/30001)

# 查询部门经理待办
curl -X GET "http://localhost:8080/approval/tasks/todo?current=1&size=10" \
  -H "Authorization: ${TOKEN_30001}"
```

待办返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "taskId": 1979012567890000002,
        "instanceId": 1979012345678901248,
        "bizType": "LEAVE",
        "bizId": 90002,
        "title": "张三请假审批",
        "applicantId": 10001,
        "nodeCode": "DEPT_MANAGER_APPROVE",
        "nodeName": "部门经理审批",
        "approverId": 30001,
        "status": "PENDING"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

部门经理执行驳回。

```bash
# 部门经理审批驳回
curl -X POST http://localhost:8080/approval/reject \
  -H "Authorization: ${TOKEN_30001}" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": 1979012567890000002,
    "comment": "请假时间较长，当前项目节点暂不批准"
  }'
```

正常返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

驳回后预期结果：

| 对象         | 预期变化                                 |
| ------------ | ---------------------------------------- |
| 部门经理任务 | `status` 从 `PENDING` 变为 `REJECTED`    |
| 审批实例     | `status` 从 `PROCESSING` 变为 `REJECTED` |
| 结束时间     | `finished_at` 写入当前时间               |
| 后续任务     | 不再生成                                 |
| 审批记录     | 新增一条 `REJECT` 记录                   |

验证 SQL：

```sql
-- 查看审批实例是否已驳回
SELECT id, status, current_node_code, current_node_name, finished_at
FROM approval_instance
WHERE id = 1979012345678901248;

-- 查看全部审批任务
SELECT id, node_name, approver_id, status, handle_time, remark
FROM approval_task
WHERE instance_id = 1979012345678901248
ORDER BY create_time ASC;

-- 查看完整审批记录
SELECT node_name, operator_id, action, action_desc, comment, create_time
FROM approval_record
WHERE instance_id = 1979012345678901248
ORDER BY create_time ASC;
```

### 查询待办与审批记录

员工、主管、部门经理分别可以查询自己的待办和已办。

员工查询审批详情：

```bash
# 查询审批详情
curl -X GET http://localhost:8080/approval/1979012345678901248 \
  -H "Authorization: ${TOKEN_10001}"
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "instanceId": 1979012345678901248,
    "bizType": "LEAVE",
    "bizId": 90002,
    "title": "张三请假审批",
    "applicantId": 10001,
    "status": "REJECTED",
    "currentNodeCode": "DEPT_MANAGER_APPROVE",
    "currentNodeName": "部门经理审批",
    "pendingTasks": [],
    "records": [
      {
        "operatorId": 10001,
        "action": "SUBMIT",
        "actionDesc": "发起审批",
        "comment": "请假 2 天，回家处理个人事务"
      },
      {
        "operatorId": 20001,
        "action": "PASS",
        "actionDesc": "审批通过",
        "comment": "同意，请提前做好工作交接"
      },
      {
        "operatorId": 30001,
        "action": "REJECT",
        "actionDesc": "审批驳回",
        "comment": "请假时间较长，当前项目节点暂不批准"
      }
    ]
  }
}
```

查询审批记录时间线：

```bash
# 查询审批记录时间线
curl -X GET http://localhost:8080/approval/1979012345678901248/records \
  -H "Authorization: ${TOKEN_10001}"
```

主管查询已办：

```bash
# 直属主管查询已办
curl -X GET "http://localhost:8080/approval/tasks/done?current=1&size=10" \
  -H "Authorization: ${TOKEN_20001}"
```

部门经理查询已办：

```bash
# 部门经理查询已办
curl -X GET "http://localhost:8080/approval/tasks/done?current=1&size=10" \
  -H "Authorization: ${TOKEN_30001}"
```

如果要重新测试同一个 `bizId`，需要先更换请求中的 `bizId`。不建议直接删除审批数据，因为审批记录属于业务留痕。开发环境确实需要清理时，可以按实例 ID 清理。

```sql
-- 仅开发环境使用：按审批实例清理测试数据
DELETE FROM approval_record WHERE instance_id = 1979012345678901248;
DELETE FROM approval_task WHERE instance_id = 1979012345678901248;
DELETE FROM approval_instance WHERE id = 1979012345678901248;
```

## 可扩展能力

当前实现已经覆盖串行审批的主链路。后续要支持更复杂的审批能力，核心改造点集中在三处：

| 改造点       | 说明                                                     |
| ------------ | -------------------------------------------------------- |
| 流程节点配置 | 增加节点审批模式、审批人数、加签策略等字段               |
| 任务生成逻辑 | 从“一节点一任务”扩展为“一节点多任务”                     |
| 任务完成判断 | 从“当前任务完成就流转”扩展为“当前节点满足通过条件才流转” |

### 会签扩展

会签指同一个节点有多个审批人，必须全部同意后才能进入下一节点。典型场景是合同审批需要法务、财务、业务负责人全部同意。

推荐在 `approval_node` 表增加审批模式字段。

```sql
-- 节点审批模式：SEQUENTIAL=串行，COUNTERSIGN=会签，OR_SIGN=或签
ALTER TABLE approval_node
ADD COLUMN approve_mode VARCHAR(32) NOT NULL DEFAULT 'SEQUENTIAL' COMMENT '审批模式：SEQUENTIAL/COUNTERSIGN/OR_SIGN';

-- 会签审批人配置，可以存多个用户ID，示例：20001,20002,20003
ALTER TABLE approval_node
ADD COLUMN approver_values VARCHAR(500) DEFAULT NULL COMMENT '多个审批人配置，英文逗号分隔';
```

会签节点配置示例：

```sql
-- 示例：合同会签节点，三个审批人全部通过后才流转
INSERT INTO approval_node (
    id, biz_type, node_code, node_name, node_type,
    approver_type, approver_value, approver_values,
    sort_no, reject_strategy, approve_mode
) VALUES (
    2001, 'CONTRACT', 'CONTRACT_COUNTERSIGN', '合同会签',
    'APPROVAL', 'USER', NULL, '20001,20002,20003',
    2, 'TERMINATE', 'COUNTERSIGN'
);
```

会签任务生成逻辑：

```text
进入会签节点
-> 解析 approver_values
-> 为每个审批人创建一条 PENDING 任务
-> 任意一人驳回，流程直接驳回
-> 每次有人通过后，检查该节点是否还有 PENDING 任务
-> 如果没有 PENDING 任务，说明全部通过，流转下一节点
```

会签通过判断 SQL：

```sql
-- 判断当前会签节点是否还有待处理任务
SELECT COUNT(1)
FROM approval_task
WHERE instance_id = 1979012345678901248
  AND node_code = 'CONTRACT_COUNTERSIGN'
  AND status = 'PENDING';
```

如果结果为 `0`，说明该会签节点全部处理完成，可以进入下一节点。

核心改造点在 `pass` 方法中：

```text
审批通过当前任务
-> 判断当前节点 approve_mode
-> 如果是 SEQUENTIAL，按原逻辑直接流转下一节点
-> 如果是 COUNTERSIGN，检查当前节点是否还有 PENDING 任务
   -> 有：实例仍然停留当前节点
   -> 没有：流转下一节点
```

### 或签扩展

或签指同一个节点有多个审批人，只要任意一个人同意，就进入下一节点。典型场景是多个部门负责人任意一人确认即可。

或签可以复用 `approve_mode = OR_SIGN` 和 `approver_values` 字段。

或签节点配置示例：

```sql
-- 示例：任意一个负责人同意即可通过
INSERT INTO approval_node (
    id, biz_type, node_code, node_name, node_type,
    approver_type, approver_value, approver_values,
    sort_no, reject_strategy, approve_mode
) VALUES (
    3001, 'PURCHASE', 'MANAGER_OR_SIGN', '负责人或签',
    'APPROVAL', 'USER', NULL, '20001,20002,20003',
    2, 'TERMINATE', 'OR_SIGN'
);
```

或签任务生成逻辑：

```text
进入或签节点
-> 为多个审批人同时生成 PENDING 任务
-> 任意一个审批人通过
-> 当前节点其他 PENDING 任务自动变为 CANCELED
-> 流转下一节点
```

或签通过后取消其他待办：

```sql
-- 任意一人通过后，取消当前节点其他待处理任务
UPDATE approval_task
SET status = 'CANCELED',
    remark = '或签节点已有审批人通过，自动取消',
    handle_time = NOW()
WHERE instance_id = 1979012345678901248
  AND node_code = 'MANAGER_OR_SIGN'
  AND status = 'PENDING'
  AND id <> 1979012456789000001;
```

或签驳回策略可以按业务选择：

| 策略               | 说明                       |
| ------------------ | -------------------------- |
| 任意一人驳回即驳回 | 风控、财务、合规类审批常用 |
| 全部驳回才驳回     | 普通协作审批可用           |
| 驳回只关闭个人任务 | 仍等待其他人处理           |

推荐先实现“任意一人通过即通过，任意一人驳回即驳回”，规则简单，用户理解成本低。

### 加签扩展

加签指审批过程中临时增加一个或多个审批人。常见模式有三种：

| 加签类型 | 说明                                   |
| -------- | -------------------------------------- |
| 前加签   | 新审批人先审批，通过后回到当前审批人   |
| 后加签   | 当前审批人先审批，通过后新增审批人审批 |
| 并行加签 | 当前审批人与新增审批人同时审批         |

建议新增一张加签关系表，避免污染原有流程节点配置。

```sql
-- 审批加签表：记录审批过程中临时增加的审批任务关系
CREATE TABLE approval_add_sign (
    id BIGINT NOT NULL COMMENT '主键ID',
    instance_id BIGINT NOT NULL COMMENT '审批实例ID',
    source_task_id BIGINT NOT NULL COMMENT '原审批任务ID',
    add_task_id BIGINT NOT NULL COMMENT '加签任务ID',
    add_user_id BIGINT NOT NULL COMMENT '加签审批人ID',
    add_type VARCHAR(32) NOT NULL COMMENT '加签类型：BEFORE/AFTER/PARALLEL',
    status VARCHAR(32) NOT NULL COMMENT '状态：PROCESSING/FINISHED/CANCELED',
    create_user_id BIGINT NOT NULL COMMENT '加签发起人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_instance_id (instance_id),
    KEY idx_source_task_id (source_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批加签表';
```

前加签核心流程：

```text
当前审批人点击前加签
-> 原审批任务暂时保持 PENDING，但标记为等待加签
-> 创建加签审批任务，审批人为新增用户
-> 加签人审批通过
-> 原审批人继续审批
```

后加签核心流程：

```text
当前审批人审批通过
-> 不立即流转下一正式节点
-> 创建后加签任务
-> 加签人审批通过
-> 再流转下一正式节点
```

并行加签核心流程：

```text
当前审批人点击并行加签
-> 创建加签任务
-> 当前审批人与加签人都需要处理
-> 两者都通过后才继续流转
```

如果要让当前表结构最小改动支持加签，可以在 `approval_task` 表中增加几个字段。

```sql
ALTER TABLE approval_task
ADD COLUMN task_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '任务类型：NORMAL/ADD_SIGN';

ALTER TABLE approval_task
ADD COLUMN parent_task_id BIGINT DEFAULT NULL COMMENT '父任务ID，加签任务关联原任务';

ALTER TABLE approval_task
ADD COLUMN add_sign_type VARCHAR(32) DEFAULT NULL COMMENT '加签类型：BEFORE/AFTER/PARALLEL';
```

加签接口可以设计为：

```text
POST /approval/add-sign

请求参数：
{
  "taskId": 1979012456789000001,
  "addUserId": 20005,
  "addType": "BEFORE",
  "comment": "需要 HR 先确认请假额度"
}
```

加签的关键不是创建任务，而是“加签任务完成后如何回到原流程”。因此实现时一定要明确：

| 问题                 | 建议规则                         |
| -------------------- | -------------------------------- |
| 加签人驳回怎么办     | 审批实例直接驳回                 |
| 前加签通过后怎么办   | 回到原审批人继续处理             |
| 后加签通过后怎么办   | 继续流转下一节点                 |
| 并行加签通过后怎么办 | 等原任务和加签任务都通过后再流转 |

### 消息通知扩展

审批流一般需要在以下时机发送通知：

| 触发时机   | 通知对象       | 示例                 |
| ---------- | -------------- | -------------------- |
| 发起审批后 | 当前审批人     | 你有一条新的审批待办 |
| 审批通过后 | 下一节点审批人 | 你有一条新的审批待办 |
| 审批驳回后 | 发起人         | 你的审批已被驳回     |
| 审批撤回后 | 当前审批人     | 发起人已撤回审批     |
| 审批转交后 | 新审批人       | 有一条审批转交给你   |
| 流程结束后 | 发起人         | 你的审批已通过       |

推荐通过 RabbitMQ 异步发送通知，不建议在审批事务中直接调用短信、邮件、企微、钉钉等外部接口。

推荐流程：

```text
审批业务事务提交
-> 发送审批通知事件到 MQ
-> 通知消费者读取事件
-> 根据模板生成消息内容
-> 写入站内信
-> 可选发送 WebSocket / 短信 / 邮件 / 企业微信
```

可以先设计一个消息事件结构。

```json
{
  "eventType": "APPROVAL_TASK_CREATED",
  "instanceId": 1979012345678901248,
  "taskId": 1979012456789000001,
  "receiverId": 20001,
  "title": "张三请假审批",
  "content": "你有一条新的请假审批待办，请及时处理"
}
```

RabbitMQ 队列建议：

```text
exchange: approval.event.exchange
routingKey: approval.task.created
queue: approval.task.notify.queue
```

通知发送的落地建议：

| 实现方式                               | 适用场景               |
| -------------------------------------- | ---------------------- |
| 事务内直接发 MQ                        | 简单项目可用，实现快   |
| 本地消息表 + 定时补偿                  | 更可靠，适合生产项目   |
| 事务事件 `@TransactionalEventListener` | 避免事务回滚后误发通知 |
| MQ 消费端幂等                          | 防止重复通知           |

如果使用 Spring 事务事件，建议在审批任务创建后发布领域事件，在事务提交后再发送 MQ。

```text
创建审批任务
-> 发布 ApprovalTaskCreatedEvent
-> 当前事务提交成功
-> 监听 AFTER_COMMIT 事件
-> 投递 MQ
-> 消费者发送站内信 / WebSocket
```

最小实现时，可以先在 `createTask` 方法末尾发布事件：

```text
createTask(...)
-> insert approval_task
-> publish ApprovalTaskCreatedEvent
```

后续再扩展站内信表：

```sql
-- 站内信表：用于保存审批通知消息
CREATE TABLE system_message (
    id BIGINT NOT NULL COMMENT '主键ID',
    receiver_id BIGINT NOT NULL COMMENT '接收人ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    biz_id BIGINT NOT NULL COMMENT '业务ID',
    title VARCHAR(200) NOT NULL COMMENT '消息标题',
    content VARCHAR(1000) NOT NULL COMMENT '消息内容',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '读取状态：0=未读，1=已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_receiver_read (receiver_id, read_status),
    KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信消息表';
```

审批通知建议和审批主流程解耦。审批能不能成功，不应该依赖短信、邮件、企微这类外部通道是否成功。通知失败可以补偿，审批数据必须先保证一致。