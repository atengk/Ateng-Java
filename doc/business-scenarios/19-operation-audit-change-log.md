# 操作审计与关键数据变更留痕

## 场景目标

本案例实现一个适用于 Java 后端管理系统的操作审计与关键数据变更留痕功能，主要解决“谁在什么时间，对什么业务数据，做了什么操作，改了哪些字段”的问题。

在实际项目中，普通业务日志通常用于排查系统异常，而操作审计日志更偏向业务追踪、责任定位、数据回溯和合规留痕。本案例重点实现后台系统最常用的审计能力，不做复杂的审计平台设计，保证代码可以直接集成到现有 Spring Boot 项目中。

本案例的核心目标是：

| 目标         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 记录操作行为 | 记录操作人、操作时间、请求接口、操作模块、操作类型、执行结果 |
| 记录数据变化 | 记录关键业务字段修改前后的值                                 |
| 降低业务侵入 | 通过注解和 AOP 自动完成审计记录                              |
| 支持问题追踪 | 能根据业务类型、业务 ID 查询变更历史                         |
| 支持安全留痕 | 对密码、Token、密钥等敏感字段进行过滤或脱敏                  |

### 业务背景

在后台管理系统中，经常会出现一些难以追踪的问题。

例如，用户状态被禁用后，无法确认是谁操作的；订单金额或订单状态被修改后，只能看到当前结果，看不到修改前的值；管理员调整了用户角色、账户余额、审批状态等关键数据，但系统没有记录完整的操作过程。

常见问题包括：

| 问题                   | 影响                 |
| ---------------------- | -------------------- |
| 不知道是谁修改了数据   | 无法定位责任人       |
| 不知道修改前是什么值   | 无法判断变更是否合理 |
| 不知道通过哪个接口修改 | 无法定位具体业务入口 |
| 操作失败没有记录       | 无法还原异常操作过程 |
| 请求参数没有留痕       | 问题排查缺少上下文   |
| 删除操作没有记录       | 数据丢失后难以追踪   |

例如订单状态从 `待支付` 被修改为 `已取消`，仅通过订单表当前数据无法知道完整过程。系统至少需要记录以下信息：

| 信息     | 示例                  |
| -------- | --------------------- |
| 操作人   | admin                 |
| 操作时间 | 2026-05-15 10:30:00   |
| 操作模块 | 订单管理              |
| 操作类型 | 修改订单状态          |
| 业务类型 | ORDER                 |
| 业务 ID  | 10001                 |
| 修改字段 | status                |
| 修改前值 | WAIT_PAY              |
| 修改后值 | CANCELED              |
| 请求接口 | `/order/updateStatus` |
| 操作结果 | 成功                  |

因此，系统需要在执行业务操作时自动记录操作行为，并对关键数据字段进行变更留痕。

### 核心功能

本案例将审计能力拆成两类日志：操作审计日志和数据变更日志。

操作审计日志用于记录一次业务操作行为，重点回答“谁做了什么”。数据变更日志用于记录字段级的数据变化，重点回答“数据具体怎么变了”。

| 功能           | 说明                                               |
| -------------- | -------------------------------------------------- |
| 审计注解       | 使用 `@AuditLog` 标记需要审计的方法                |
| AOP 拦截       | 自动拦截带有审计注解的业务方法                     |
| 操作日志记录   | 记录操作人、请求路径、请求参数、操作结果、异常信息 |
| 修改前数据快照 | 在业务方法执行前查询原始数据                       |
| 修改后数据对比 | 在业务方法执行后查询最新数据                       |
| 字段级变更记录 | 记录字段名、修改前值、修改后值                     |
| 敏感字段脱敏   | 对密码、Token、密钥等字段进行过滤或脱敏            |
| 异常操作留痕   | 业务执行失败时也记录失败日志                       |
| 审计日志查询   | 支持分页查询操作审计记录                           |
| 变更历史查询   | 支持按业务类型和业务 ID 查询变更轨迹               |

核心流程如下：

```text
请求进入接口
  ↓
AOP 拦截 @AuditLog 方法
  ↓
解析操作模块、操作类型、业务类型、业务 ID
  ↓
记录请求上下文信息
  ↓
修改类操作先查询变更前数据
  ↓
执行业务方法
  ↓
查询变更后数据
  ↓
对比关键字段差异
  ↓
保存操作审计日志
  ↓
保存数据变更日志
```

这个流程可以覆盖后台系统中常见的新增、修改、删除、启用、禁用、审核、驳回、授权等操作场景。

### 实现边界

为了让案例聚焦核心实现，本案例只实现通用后台系统中最实用的审计能力，不引入过重的日志平台或数据库级审计方案。

| 范围                 | 是否实现 | 说明                       |
| -------------------- | -------- | -------------------------- |
| 操作审计日志入库     | 实现     | 记录一次接口操作行为       |
| 关键字段变更留痕     | 实现     | 记录字段修改前后的值       |
| 注解式接入           | 实现     | 业务方法添加注解即可接入   |
| AOP 自动拦截         | 实现     | 减少业务代码侵入           |
| 请求参数记录         | 实现     | 保存接口入参，方便排查     |
| 请求参数脱敏         | 实现     | 避免敏感字段直接入库       |
| 异常操作记录         | 实现     | 方法执行异常时记录失败日志 |
| 审计日志分页查询     | 实现     | 方便后台页面展示           |
| 业务数据变更历史查询 | 实现     | 方便查看单条数据变更轨迹   |
| 异步写入审计日志     | 可选扩展 | 核心案例先使用同步写入     |
| Canal / Binlog 监听  | 不实现   | 属于数据库级变更监听方案   |
| Elasticsearch 检索   | 不实现   | 日志量较大时再考虑接入     |
| 全量数据版本快照     | 不实现   | 本案例只记录关键字段变化   |
| 审计日志归档清理     | 不实现   | 后续可通过定时任务扩展     |

本案例适合直接用于以下系统：

| 系统类型       | 典型审计场景                       |
| -------------- | ---------------------------------- |
| 后台管理系统   | 用户启用、禁用、角色调整           |
| 订单系统       | 订单状态、金额、收货信息变更       |
| 审批系统       | 审核通过、驳回、撤回               |
| 权限系统       | 用户授权、角色授权、菜单权限调整   |
| 财务系统       | 账户余额、结算状态、发票状态变更   |
| CRM / ERP 系统 | 客户资料、合同状态、业务负责人变更 |

对于金融级合规审计、数据库全量变更监听、跨系统统一审计中心等更复杂场景，可以在本案例基础上扩展 MQ、ClickHouse、Elasticsearch、Canal 或日志归档策略。

## 技术方案

本案例采用“注解 + AOP + 数据对比 + 日志落库”的方式实现。业务方法只需要添加审计注解，审计逻辑统一在切面中处理，避免在 Controller 或 Service 中到处手写日志代码。

整体技术选型如下：

| 技术          | 作用                                       |
| ------------- | ------------------------------------------ |
| Spring Boot 3 | 后端基础框架                               |
| Spring AOP    | 拦截审计注解方法                           |
| MyBatis-Plus  | 操作审计表和业务表                         |
| MySQL         | 存储审计日志和变更日志                     |
| Jackson       | 对象转 JSON、字段值处理                    |
| Hutool        | Bean 工具、JSON 工具、字符串工具、脱敏辅助 |
| Lombok        | 简化实体、DTO、Service 代码                |

核心实现思路如下：

```text
业务接口请求
  ↓
进入带有 @AuditLog 的业务方法
  ↓
AOP 切面读取审计配置
  ↓
记录请求上下文
  ↓
修改类操作先查询原始业务数据
  ↓
执行业务逻辑
  ↓
查询修改后的业务数据
  ↓
对比关键字段变化
  ↓
写入操作审计日志
  ↓
写入数据变更日志
```

### 审计日志采集方式

审计日志采集采用注解驱动方式。需要记录审计的业务方法添加 `@AuditLog` 注解，注解中声明操作模块、操作类型、业务类型、是否记录请求参数、是否记录数据变更等配置。

采集内容主要来自三个位置：

| 来源         | 采集内容                                     |
| ------------ | -------------------------------------------- |
| 注解配置     | 操作模块、操作类型、业务类型、操作说明       |
| 请求上下文   | 请求路径、请求方式、请求参数、IP、User-Agent |
| 登录上下文   | 操作人 ID、操作人名称、租户 ID、部门 ID      |
| 方法执行结果 | 是否成功、异常信息、执行耗时                 |

审计日志建议在 Service 层或 Controller 层采集。

| 采集位置      | 优点                             | 缺点                         | 建议                 |
| ------------- | -------------------------------- | ---------------------------- | -------------------- |
| Controller 层 | 能直接拿到请求参数和接口信息     | 不一定能拿到完整业务数据     | 适合普通操作日志     |
| Service 层    | 更靠近业务逻辑，适合记录业务操作 | 获取请求信息需要从上下文读取 | 推荐用于关键业务审计 |
| Mapper 层     | 接近数据库操作                   | 难以识别业务语义             | 不推荐作为主要方案   |

本案例推荐在 Service 层添加审计注解，因为 Service 方法更能表达真实业务含义，例如“禁用用户”“取消订单”“审核通过”，而不是单纯的 HTTP 请求行为。

审计日志采集字段建议如下：

| 字段          | 说明                                 |
| ------------- | ------------------------------------ |
| traceId       | 请求链路 ID，方便关联应用日志        |
| moduleName    | 操作模块，例如用户管理、订单管理     |
| operationType | 操作类型，例如新增、修改、删除、审核 |
| businessType  | 业务类型，例如 USER、ORDER、ROLE     |
| businessId    | 业务数据 ID                          |
| operatorId    | 操作人 ID                            |
| operatorName  | 操作人名称                           |
| requestUri    | 请求地址                             |
| requestMethod | 请求方式                             |
| requestParams | 请求参数                             |
| success       | 是否执行成功                         |
| errorMessage  | 异常信息                             |
| costTime      | 执行耗时                             |
| operationTime | 操作时间                             |

### 关键数据变更留痕方式

关键数据变更留痕采用“修改前查询一次，修改后查询一次，然后对比字段差异”的方式实现。

这种方式比直接记录请求参数更可靠。因为请求参数不一定等于最终落库数据，例如业务代码中可能会自动填充状态、金额、审核人、更新时间等字段。通过查询数据库中的修改前后数据，可以记录真实的数据变化结果。

核心流程如下：

```text
读取 @AuditLog 注解配置
  ↓
判断是否需要记录数据变更
  ↓
根据业务 ID 查询修改前数据
  ↓
执行业务修改方法
  ↓
根据业务 ID 查询修改后数据
  ↓
按字段进行差异对比
  ↓
过滤无变化字段
  ↓
过滤敏感字段
  ↓
保存字段级变更日志
```

字段对比建议只对关键字段做留痕，不建议默认记录所有字段。

例如用户表中可以记录：

| 字段       | 是否建议留痕 | 原因             |
| ---------- | ------------ | ---------------- |
| username   | 建议         | 用户核心标识     |
| nickname   | 建议         | 用户展示信息     |
| status     | 建议         | 影响用户是否可用 |
| roleId     | 建议         | 影响权限         |
| password   | 不建议       | 敏感字段         |
| updateTime | 不建议       | 系统维护字段     |
| createTime | 不建议       | 通常不会变更     |

字段变更日志建议记录为一行一个字段变化，而不是把所有变化塞进一条 JSON。

例如一次修改用户状态和昵称，可以保存两条变更记录：

| businessType | businessId | fieldName | oldValue | newValue  |
| ------------ | ---------- | --------- | -------- | --------- |
| USER         | 10001      | status    | ENABLED  | DISABLED  |
| USER         | 10001      | nickname  | 张三     | 张三-停用 |

这样设计方便后续按照字段、业务 ID、操作人、时间范围进行查询和统计。

### 数据存储设计

本案例使用两张日志表：

| 表名                        | 作用                   |
| --------------------------- | ---------------------- |
| `sys_audit_log`             | 保存一次业务操作行为   |
| `sys_audit_data_change_log` | 保存字段级数据变更记录 |

两张表通过 `audit_log_id` 关联。

关系如下：

```text
sys_audit_log
  └── sys_audit_data_change_log
```

一次操作只会产生一条操作审计日志，但可能产生多条数据变更日志。

例如管理员修改订单状态、金额、备注：

```text
sys_audit_log
  id = 1
  operation_type = UPDATE
  business_type = ORDER
  business_id = 10001

sys_audit_data_change_log
  audit_log_id = 1, field_name = status, old_value = WAIT_PAY, new_value = CANCELED
  audit_log_id = 1, field_name = amount, old_value = 100.00, new_value = 80.00
  audit_log_id = 1, field_name = remark, old_value = null, new_value = 用户申请取消
```

日志表设计时需要注意：

| 设计点     | 建议                                       |
| ---------- | ------------------------------------------ |
| 主键       | 使用 bigint，兼容雪花 ID 或自增 ID         |
| 请求参数   | 使用 JSON 或 LONGTEXT                      |
| 字段值     | 使用 VARCHAR 或 TEXT，避免字段值过长       |
| 时间字段   | 添加索引，方便按时间查询                   |
| 业务字段   | `business_type + business_id` 添加联合索引 |
| 操作人字段 | 添加索引，方便按操作人查询                 |
| 日志删除   | 不建议物理删除，可以定期归档               |

## 表结构设计

表结构使用 MySQL 8 示例。若项目使用 PostgreSQL，可以将 `json`、`datetime`、`bigint` 等类型按项目规范替换。

### 操作审计日志表

`sys_audit_log` 用于记录一次完整的业务操作行为。它不负责记录每个字段怎么变化，只记录“谁在什么时候通过什么入口做了什么操作，以及执行结果如何”。

下面的 SQL 用于创建操作审计日志表。

```sql
CREATE TABLE sys_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    module_name VARCHAR(100) NOT NULL COMMENT '操作模块名称',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型：CREATE、UPDATE、DELETE、ENABLE、DISABLE、AUDIT',
    operation_desc VARCHAR(255) DEFAULT NULL COMMENT '操作说明',

    business_type VARCHAR(100) DEFAULT NULL COMMENT '业务类型：USER、ORDER、ROLE等',
    business_id VARCHAR(64) DEFAULT NULL COMMENT '业务数据ID',

    request_method VARCHAR(20) DEFAULT NULL COMMENT '请求方式：GET、POST、PUT、DELETE',
    request_uri VARCHAR(500) DEFAULT NULL COMMENT '请求地址',
    request_params JSON DEFAULT NULL COMMENT '请求参数，敏感字段需要提前脱敏',

    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) DEFAULT NULL COMMENT '操作人名称',
    client_ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '浏览器或客户端标识',

    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功：1成功，0失败',
    error_message TEXT DEFAULT NULL COMMENT '异常信息',
    cost_time BIGINT DEFAULT NULL COMMENT '执行耗时，单位毫秒',

    operation_time DATETIME NOT NULL COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    KEY idx_audit_log_operation_time (operation_time),
    KEY idx_audit_log_operator_id (operator_id),
    KEY idx_audit_log_business (business_type, business_id),
    KEY idx_audit_log_operation_type (operation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';
```

字段说明如下：

| 字段             | 说明                                          |
| ---------------- | --------------------------------------------- |
| `trace_id`       | 用于关联应用日志，排查问题时很有用            |
| `module_name`    | 操作所属模块，例如用户管理、订单管理          |
| `operation_type` | 操作类型，建议使用枚举统一管理                |
| `business_type`  | 业务类型，建议使用英文常量                    |
| `business_id`    | 业务数据 ID，为了兼容不同主键类型，使用字符串 |
| `request_params` | 请求参数，入库前需要脱敏                      |
| `success`        | 方法执行成功或失败                            |
| `error_message`  | 业务方法抛异常时记录异常摘要                  |
| `cost_time`      | 统计接口或业务操作耗时                        |

推荐的操作类型枚举如下：

| 类型      | 说明 |
| --------- | ---- |
| `CREATE`  | 新增 |
| `UPDATE`  | 修改 |
| `DELETE`  | 删除 |
| `ENABLE`  | 启用 |
| `DISABLE` | 禁用 |
| `AUDIT`   | 审核 |
| `IMPORT`  | 导入 |
| `EXPORT`  | 导出 |
| `LOGIN`   | 登录 |
| `LOGOUT`  | 登出 |

### 数据变更日志表

`sys_audit_data_change_log` 用于记录字段级数据变化。一条业务操作可能对应多条字段变更记录。

下面的 SQL 用于创建数据变更日志表。

```sql
CREATE TABLE sys_audit_data_change_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    audit_log_id BIGINT NOT NULL COMMENT '操作审计日志ID',
    business_type VARCHAR(100) NOT NULL COMMENT '业务类型：USER、ORDER、ROLE等',
    business_id VARCHAR(64) NOT NULL COMMENT '业务数据ID',

    field_name VARCHAR(100) NOT NULL COMMENT '字段名称，例如 status',
    field_label VARCHAR(100) DEFAULT NULL COMMENT '字段中文名称，例如 用户状态',
    old_value TEXT DEFAULT NULL COMMENT '修改前的值',
    new_value TEXT DEFAULT NULL COMMENT '修改后的值',

    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) DEFAULT NULL COMMENT '操作人名称',
    change_time DATETIME NOT NULL COMMENT '变更时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    KEY idx_change_log_audit_log_id (audit_log_id),
    KEY idx_change_log_business (business_type, business_id),
    KEY idx_change_log_field_name (field_name),
    KEY idx_change_log_change_time (change_time),
    KEY idx_change_log_operator_id (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据变更日志表';
```

字段说明如下：

| 字段            | 说明                                                |
| --------------- | --------------------------------------------------- |
| `audit_log_id`  | 关联操作审计日志表                                  |
| `business_type` | 业务类型，例如 USER、ORDER                          |
| `business_id`   | 业务数据 ID                                         |
| `field_name`    | Java 字段名或数据库字段名，建议统一使用 Java 字段名 |
| `field_label`   | 字段中文名称，方便页面展示                          |
| `old_value`     | 修改前的值                                          |
| `new_value`     | 修改后的值                                          |
| `change_time`   | 变更发生时间                                        |

页面展示时可以按照以下格式展示：

| 字段     | 修改前      | 修改后      |
| -------- | ----------- | ----------- |
| 用户状态 | 启用        | 禁用        |
| 用户昵称 | 张三        | 张三-停用   |
| 手机号   | 138****8888 | 139****9999 |

### 业务表改造建议

业务表不强制为了审计功能做大规模改造，但建议保留基础操作字段，方便审计日志和业务数据互相印证。

常见业务表建议包含以下字段：

| 字段          | 说明               |
| ------------- | ------------------ |
| `create_by`   | 创建人 ID          |
| `create_name` | 创建人名称         |
| `create_time` | 创建时间           |
| `update_by`   | 更新人 ID          |
| `update_name` | 更新人名称         |
| `update_time` | 更新时间           |
| `deleted`     | 逻辑删除标识       |
| `version`     | 乐观锁版本号，可选 |

如果业务表已经有类似字段，可以直接复用，不需要重复新增。

下面以用户表为例，展示建议的业务表结构。

```sql
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',

    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(100) DEFAULT NULL COMMENT '用户昵称',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用，DISABLED禁用',

    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_name VARCHAR(100) DEFAULT NULL COMMENT '创建人名称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    update_name VARCHAR(100) DEFAULT NULL COMMENT '更新人名称',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',

    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_status (status),
    KEY idx_user_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

如果是已有业务表，可以使用下面的 SQL 增加基础审计字段。

```sql
ALTER TABLE sys_user
    ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    ADD COLUMN create_name VARCHAR(100) DEFAULT NULL COMMENT '创建人名称',
    ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    ADD COLUMN update_name VARCHAR(100) DEFAULT NULL COMMENT '更新人名称',
    ADD COLUMN update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
```

业务表改造建议保持克制，只加通用字段，不建议在每张业务表中都新增大量审计字段。字段级变化统一写入 `sys_audit_data_change_log`，业务表只保留当前业务状态。

## 核心实现

本节实现操作审计与关键数据变更留痕的核心代码。整体采用注解声明审计信息，AOP 统一拦截业务方法，在方法执行前后查询业务数据快照，并将操作日志与字段变更日志写入数据库。

核心文件结构如下：

```text
src/main/java/io/github/atengk/audit
├── annotation/AuditLog.java
├── annotation/AuditField.java
├── aspect/AuditLogAspect.java
├── context/AuditOperatorContext.java
├── context/RequestContextHelper.java
├── enums/AuditOperationType.java
├── model/AuditFieldChange.java
├── service/AuditSnapshotProvider.java
├── service/AuditLogRecordService.java
├── service/impl/AuditLogRecordServiceImpl.java
├── util/AuditDiffUtil.java
├── util/AuditSensitiveUtil.java
├── entity/SysAuditLog.java
├── entity/SysAuditDataChangeLog.java
├── mapper/SysAuditLogMapper.java
└── mapper/SysAuditDataChangeLogMapper.java
```

### 审计注解定义

审计注解用于声明当前业务方法的审计信息，包括操作模块、操作类型、业务类型、业务 ID 表达式、是否记录字段变更、数据快照提供器等。

文件位置：`src/main/java/io/github/atengk/audit/annotation/AuditLog.java`

```java
package io.github.atengk.audit.annotation;

import io.github.atengk.audit.enums.AuditOperationType;

import java.lang.annotation.*;

/**
 * 操作审计注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作模块
     *
     * @return 模块名称
     */
    String module();

    /**
     * 操作类型
     *
     * @return 操作类型
     */
    AuditOperationType operationType();

    /**
     * 操作说明
     *
     * @return 操作说明
     */
    String operationDesc() default "";

    /**
     * 业务类型
     *
     * @return 业务类型
     */
    String businessType() default "";

    /**
     * 业务ID表达式，支持 SpEL，例如：#id、#request.id
     *
     * @return 业务ID表达式
     */
    String businessId() default "";

    /**
     * 是否记录请求参数
     *
     * @return true 记录，false 不记录
     */
    boolean recordParams() default true;

    /**
     * 是否记录数据变更
     *
     * @return true 记录，false 不记录
     */
    boolean recordDataChange() default false;

    /**
     * 数据快照提供器 Bean 名称
     *
     * @return Spring Bean 名称
     */
    String snapshotBean() default "";
}
```

字段注解用于标记需要参与变更留痕的字段，并配置字段中文名称、是否脱敏等信息。没有加 `@AuditField` 的字段默认不参与字段级对比。

文件位置：`src/main/java/io/github/atengk/audit/annotation/AuditField.java`

```java
package io.github.atengk.audit.annotation;

import java.lang.annotation.*;

/**
 * 审计字段注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditField {

    /**
     * 字段显示名称
     *
     * @return 字段名称
     */
    String label();

    /**
     * 是否脱敏
     *
     * @return true 脱敏，false 不脱敏
     */
    boolean sensitive() default false;
}
```

操作类型使用枚举统一管理，避免代码中到处写字符串。

文件位置：`src/main/java/io/github/atengk/audit/enums/AuditOperationType.java`

```java
package io.github.atengk.audit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计操作类型
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum AuditOperationType {

    CREATE("新增"),
    UPDATE("修改"),
    DELETE("删除"),
    ENABLE("启用"),
    DISABLE("禁用"),
    AUDIT("审核"),
    IMPORT("导入"),
    EXPORT("导出");

    private final String description;
}
```

### AOP 拦截操作行为

AOP 切面是本功能的核心入口。它负责读取 `@AuditLog` 注解，解析业务 ID，采集请求上下文，执行目标方法，并在执行完成后保存审计日志和数据变更日志。

文件位置：`src/main/java/io/github/atengk/audit/aspect/AuditLogAspect.java`

```java
package io.github.atengk.audit.aspect;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.audit.annotation.AuditLog;
import io.github.atengk.audit.context.RequestContextHelper;
import io.github.atengk.audit.model.AuditFieldChange;
import io.github.atengk.audit.service.AuditLogRecordService;
import io.github.atengk.audit.service.AuditSnapshotProvider;
import io.github.atengk.audit.util.AuditDiffUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * 操作审计切面
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final ApplicationContext applicationContext;
    private final AuditLogRecordService auditLogRecordService;
    private final RequestContextHelper requestContextHelper;

    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 拦截带有审计注解的方法
     *
     * @param joinPoint 切点
     * @param auditLog 审计注解
     * @return 方法执行结果
     * @throws Throwable 业务异常
     */
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        boolean success = true;
        String errorMessage = null;
        Object oldSnapshot = null;
        Object newSnapshot = null;
        Object result = null;

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String businessId = parseBusinessId(method, joinPoint.getArgs(), auditLog.businessId());

        try {
            if (auditLog.recordDataChange()) {
                oldSnapshot = querySnapshot(auditLog, businessId);
            }

            result = joinPoint.proceed();

            if (auditLog.recordDataChange()) {
                newSnapshot = querySnapshot(auditLog, businessId);
            }

            return result;
        } catch (Throwable ex) {
            success = false;
            errorMessage = StrUtil.sub(ex.getMessage(), 0, 1000);
            log.warn("业务操作执行失败，模块：{}，类型：{}，业务ID：{}，原因：{}",
                    auditLog.module(), auditLog.operationType().name(), businessId, errorMessage);
            throw ex;
        } finally {
            stopWatch.stop();

            List<AuditFieldChange> fieldChanges = Collections.emptyList();
            if (auditLog.recordDataChange() && success) {
                fieldChanges = AuditDiffUtil.compare(oldSnapshot, newSnapshot);
            }

            auditLogRecordService.record(
                    auditLog,
                    requestContextHelper.getRequestContext(),
                    businessId,
                    success,
                    errorMessage,
                    stopWatch.getTotalTimeMillis(),
                    fieldChanges
            );

            if (CollUtil.isNotEmpty(fieldChanges)) {
                log.info("数据变更审计记录完成，业务类型：{}，业务ID：{}，变更字段数：{}",
                        auditLog.businessType(), businessId, fieldChanges.size());
            }
        }
    }

    /**
     * 查询业务数据快照
     *
     * @param auditLog 审计注解
     * @param businessId 业务ID
     * @return 数据快照
     */
    private Object querySnapshot(AuditLog auditLog, String businessId) {
        if (StrUtil.isBlank(auditLog.snapshotBean()) || StrUtil.isBlank(businessId)) {
            return null;
        }

        AuditSnapshotProvider snapshotProvider = applicationContext.getBean(auditLog.snapshotBean(), AuditSnapshotProvider.class);
        return snapshotProvider.querySnapshot(businessId);
    }

    /**
     * 解析业务ID
     *
     * @param method 方法
     * @param args 方法参数
     * @param expression SpEL表达式
     * @return 业务ID
     */
    private String parseBusinessId(Method method, Object[] args, String expression) {
        if (StrUtil.isBlank(expression)) {
            return null;
        }

        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        EvaluationContext context = new StandardEvaluationContext();

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        Object value = new org.springframework.expression.spel.standard.SpelExpressionParser()
                .parseExpression(expression)
                .getValue(context);

        return value == null ? null : String.valueOf(value);
    }
}
```

### 请求上下文信息提取

请求上下文用于获取请求路径、请求方式、客户端 IP、User-Agent、请求参数等信息。操作人信息一般来自登录上下文，例如 Sa-Token、Spring Security 或自研登录组件。这里先用一个简单上下文类模拟。

文件位置：`src/main/java/io/github/atengk/audit/context/AuditOperatorContext.java`

```java
package io.github.atengk.audit.context;

/**
 * 审计操作人上下文
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AuditOperatorContext {

    /**
     * 获取当前操作人ID
     *
     * @return 操作人ID
     */
    public static Long getOperatorId() {
        // 实际项目中可替换为 StpUtil.getLoginIdAsLong() 或 SecurityContextHolder
        return 1L;
    }

    /**
     * 获取当前操作人名称
     *
     * @return 操作人名称
     */
    public static String getOperatorName() {
        // 实际项目中可从登录用户缓存、Token上下文或用户服务中获取
        return "admin";
    }
}
```

请求上下文帮助类负责从当前 HTTP 请求中提取审计所需信息，并对请求参数做脱敏处理。

文件位置：`src/main/java/io/github/atengk/audit/context/RequestContextHelper.java`

```java
package io.github.atengk.audit.context;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.audit.util.AuditSensitiveUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 请求上下文帮助类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
public class RequestContextHelper {

    /**
     * 获取当前请求上下文
     *
     * @return 请求上下文
     */
    public RequestContext getRequestContext() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return RequestContext.builder().build();
        }

        HttpServletRequest request = attributes.getRequest();
        return RequestContext.builder()
                .requestUri(request.getRequestURI())
                .requestMethod(request.getMethod())
                .clientIp(getClientIp(request))
                .userAgent(StrUtil.sub(request.getHeader("User-Agent"), 0, 500))
                .requestParams(AuditSensitiveUtil.desensitizeMap(getParameterMap(request)))
                .build();
    }

    /**
     * 获取请求参数
     *
     * @param request HTTP请求
     * @return 参数Map
     */
    private Map<String, Object> getParameterMap(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (MapUtil.isEmpty(parameterMap)) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        parameterMap.forEach((key, values) -> result.put(key, ArrayUtil.isEmpty(values) ? null : values[0]));
        return result;
    }

    /**
     * 获取客户端IP
     *
     * @param request HTTP请求
     * @return 客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip)) {
            return StrUtil.split(ip, ',').get(0);
        }

        ip = request.getHeader("X-Real-IP");
        return StrUtil.isNotBlank(ip) ? ip : request.getRemoteAddr();
    }

    /**
     * 请求上下文
     *
     * @author Ateng
     * @since 2026-05-15
     */
    @Data
    @Builder
    public static class RequestContext {
        private String requestMethod;
        private String requestUri;
        private String clientIp;
        private String userAgent;
        private Map<String, Object> requestParams;
    }
}
```

敏感字段脱敏工具类用于处理请求参数和字段变更值，避免密码、Token、密钥等敏感数据直接落库。

文件位置：`src/main/java/io/github/atengk/audit/util/AuditSensitiveUtil.java`

```java
package io.github.atengk.audit.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 审计敏感信息处理工具
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AuditSensitiveUtil {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "oldPassword", "newPassword", "token", "accessToken", "refreshToken", "secret", "privateKey"
    );

    /**
     * Map参数脱敏
     *
     * @param source 原始参数
     * @return 脱敏后的参数
     */
    public static Map<String, Object> desensitizeMap(Map<String, Object> source) {
        if (MapUtil.isEmpty(source)) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (isSensitiveKey(key)) {
                result.put(key, "******");
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

    /**
     * 字段值脱敏
     *
     * @param value 字段值
     * @return 脱敏结果
     */
    public static String desensitizeValue(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value);
        if (StrUtil.isBlank(text)) {
            return text;
        }

        if (StrUtil.length(text) <= 6) {
            return "******";
        }

        return DesensitizedUtil.idCardNum(text, 2, 2);
    }

    /**
     * 判断是否敏感字段
     *
     * @param key 字段名
     * @return true 是，false 否
     */
    private static boolean isSensitiveKey(String key) {
        return SENSITIVE_KEYS.stream().anyMatch(item -> StrUtil.equalsIgnoreCase(item, key));
    }
}
```

### 变更前数据查询

变更前数据查询通过统一接口 `AuditSnapshotProvider` 实现。每个业务模块只需要提供自己的快照查询逻辑即可。

文件位置：`src/main/java/io/github/atengk/audit/service/AuditSnapshotProvider.java`

```java
package io.github.atengk.audit.service;

/**
 * 审计数据快照提供器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface AuditSnapshotProvider {

    /**
     * 查询业务数据快照
     *
     * @param businessId 业务ID
     * @return 业务数据快照
     */
    Object querySnapshot(String businessId);
}
```

业务模块实现该接口，返回带有 `@AuditField` 注解的快照对象。这样审计模块不用关心具体业务表结构。

### 变更后数据对比

字段对比工具类负责比较修改前后的对象，只对标记了 `@AuditField` 的字段进行比较。这样可以避免更新时间、版本号等系统字段产生无意义日志。

文件位置：`src/main/java/io/github/atengk/audit/model/AuditFieldChange.java`

```java
package io.github.atengk.audit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计字段变更信息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditFieldChange {

    private String fieldName;

    private String fieldLabel;

    private String oldValue;

    private String newValue;
}
```

下面的工具类用于完成字段级差异对比。

文件位置：`src/main/java/io/github/atengk/audit/util/AuditDiffUtil.java`

```java
package io.github.atengk.audit.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.audit.annotation.AuditField;
import io.github.atengk.audit.model.AuditFieldChange;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 审计字段差异对比工具
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class AuditDiffUtil {

    /**
     * 对比两个对象的审计字段差异
     *
     * @param oldObject 修改前对象
     * @param newObject 修改后对象
     * @return 字段变更列表
     */
    public static List<AuditFieldChange> compare(Object oldObject, Object newObject) {
        if (oldObject == null || newObject == null) {
            return CollUtil.newArrayList();
        }

        Class<?> clazz = newObject.getClass();
        List<AuditFieldChange> changes = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            AuditField auditField = field.getAnnotation(AuditField.class);
            if (auditField == null) {
                continue;
            }

            field.setAccessible(true);

            try {
                Object oldValue = field.get(oldObject);
                Object newValue = field.get(newObject);

                if (ObjectUtil.equal(oldValue, newValue)) {
                    continue;
                }

                String oldText = auditField.sensitive()
                        ? AuditSensitiveUtil.desensitizeValue(oldValue)
                        : ObjectUtil.toString(oldValue, null);

                String newText = auditField.sensitive()
                        ? AuditSensitiveUtil.desensitizeValue(newValue)
                        : ObjectUtil.toString(newValue, null);

                changes.add(new AuditFieldChange(field.getName(), auditField.label(), oldText, newText));
            } catch (IllegalAccessException ex) {
                log.warn("审计字段对比失败，字段：{}，原因：{}", field.getName(), ex.getMessage());
            }
        }

        return changes;
    }
}
```

### 审计日志入库

审计日志实体与 Mapper 使用 MyBatis-Plus 实现。这里保持字段与前面表结构一致。

文件位置：`src/main/java/io/github/atengk/audit/entity/SysAuditLog.java`

```java
package io.github.atengk.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;

    private String moduleName;

    private String operationType;

    private String operationDesc;

    private String businessType;

    private String businessId;

    private String requestMethod;

    private String requestUri;

    private String requestParams;

    private Long operatorId;

    private String operatorName;

    private String clientIp;

    private String userAgent;

    private Boolean success;

    private String errorMessage;

    private Long costTime;

    private LocalDateTime operationTime;

    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/audit/mapper/SysAuditLogMapper.java`

```java
package io.github.atengk.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.audit.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
```

### 数据变更记录入库

数据变更日志实体与 Mapper 同样使用 MyBatis-Plus。一次操作可能产生多条字段变更记录，因此在记录服务中批量写入。

文件位置：`src/main/java/io/github/atengk/audit/entity/SysAuditDataChangeLog.java`

```java
package io.github.atengk.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据变更日志实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("sys_audit_data_change_log")
public class SysAuditDataChangeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long auditLogId;

    private String businessType;

    private String businessId;

    private String fieldName;

    private String fieldLabel;

    private String oldValue;

    private String newValue;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime changeTime;

    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/audit/mapper/SysAuditDataChangeLogMapper.java`

```java
package io.github.atengk.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.audit.entity.SysAuditDataChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据变更日志 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface SysAuditDataChangeLogMapper extends BaseMapper<SysAuditDataChangeLog> {
}
```

审计记录服务负责统一保存操作日志和数据变更日志。

文件位置：`src/main/java/io/github/atengk/audit/service/AuditLogRecordService.java`

```java
package io.github.atengk.audit.service;

import io.github.atengk.audit.annotation.AuditLog;
import io.github.atengk.audit.context.RequestContextHelper;
import io.github.atengk.audit.model.AuditFieldChange;

import java.util.List;

/**
 * 审计日志记录服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface AuditLogRecordService {

    /**
     * 记录审计日志
     *
     * @param auditLog 审计注解
     * @param requestContext 请求上下文
     * @param businessId 业务ID
     * @param success 是否成功
     * @param errorMessage 错误信息
     * @param costTime 执行耗时
     * @param fieldChanges 字段变更
     */
    void record(AuditLog auditLog,
                RequestContextHelper.RequestContext requestContext,
                String businessId,
                boolean success,
                String errorMessage,
                long costTime,
                List<AuditFieldChange> fieldChanges);
}
```

下面的实现类会先插入 `sys_audit_log`，再根据返回的审计日志 ID 批量插入 `sys_audit_data_change_log`。

文件位置：`src/main/java/io/github/atengk/audit/service/impl/AuditLogRecordServiceImpl.java`

```java
package io.github.atengk.audit.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.audit.annotation.AuditLog;
import io.github.atengk.audit.context.AuditOperatorContext;
import io.github.atengk.audit.context.RequestContextHelper;
import io.github.atengk.audit.entity.SysAuditDataChangeLog;
import io.github.atengk.audit.entity.SysAuditLog;
import io.github.atengk.audit.mapper.SysAuditDataChangeLogMapper;
import io.github.atengk.audit.mapper.SysAuditLogMapper;
import io.github.atengk.audit.model.AuditFieldChange;
import io.github.atengk.audit.service.AuditLogRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志记录服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogRecordServiceImpl implements AuditLogRecordService {

    private final SysAuditLogMapper sysAuditLogMapper;
    private final SysAuditDataChangeLogMapper sysAuditDataChangeLogMapper;

    /**
     * 记录审计日志
     *
     * @param auditLog 审计注解
     * @param requestContext 请求上下文
     * @param businessId 业务ID
     * @param success 是否成功
     * @param errorMessage 错误信息
     * @param costTime 执行耗时
     * @param fieldChanges 字段变更
     */
    @Override
    public void record(AuditLog auditLog,
                       RequestContextHelper.RequestContext requestContext,
                       String businessId,
                       boolean success,
                       String errorMessage,
                       long costTime,
                       List<AuditFieldChange> fieldChanges) {
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = AuditOperatorContext.getOperatorId();
        String operatorName = AuditOperatorContext.getOperatorName();

        SysAuditLog audit = new SysAuditLog();
        audit.setModuleName(auditLog.module());
        audit.setOperationType(auditLog.operationType().name());
        audit.setOperationDesc(auditLog.operationDesc());
        audit.setBusinessType(auditLog.businessType());
        audit.setBusinessId(businessId);
        audit.setRequestMethod(requestContext.getRequestMethod());
        audit.setRequestUri(requestContext.getRequestUri());
        audit.setRequestParams(auditLog.recordParams() ? JSONUtil.toJsonStr(requestContext.getRequestParams()) : null);
        audit.setOperatorId(operatorId);
        audit.setOperatorName(operatorName);
        audit.setClientIp(requestContext.getClientIp());
        audit.setUserAgent(requestContext.getUserAgent());
        audit.setSuccess(success);
        audit.setErrorMessage(errorMessage);
        audit.setCostTime(costTime);
        audit.setOperationTime(now);
        audit.setCreateTime(now);

        sysAuditLogMapper.insert(audit);

        if (success && CollUtil.isNotEmpty(fieldChanges)) {
            for (AuditFieldChange change : fieldChanges) {
                SysAuditDataChangeLog changeLog = new SysAuditDataChangeLog();
                changeLog.setAuditLogId(audit.getId());
                changeLog.setBusinessType(auditLog.businessType());
                changeLog.setBusinessId(businessId);
                changeLog.setFieldName(change.getFieldName());
                changeLog.setFieldLabel(change.getFieldLabel());
                changeLog.setOldValue(change.getOldValue());
                changeLog.setNewValue(change.getNewValue());
                changeLog.setOperatorId(operatorId);
                changeLog.setOperatorName(operatorName);
                changeLog.setChangeTime(now);
                changeLog.setCreateTime(now);

                sysAuditDataChangeLogMapper.insert(changeLog);
            }
        }

        log.info("操作审计记录完成，模块：{}，类型：{}，业务ID：{}，结果：{}",
                auditLog.module(), auditLog.operationType().name(), businessId, success ? "成功" : "失败");
    }
}
```

## 业务接入示例

业务模块接入时，只需要做三件事：定义用于对比的快照对象，实现快照查询服务，在关键业务方法上添加 `@AuditLog` 注解。

### 用户管理操作审计

下面以用户禁用为例。用户状态从 `ENABLED` 修改为 `DISABLED` 时，系统会自动记录操作日志，并记录 `status` 字段的修改前后值。

用户快照对象只保留需要审计的字段。

文件位置：`src/main/java/io/github/atengk/user/dto/UserAuditSnapshot.java`

```java
package io.github.atengk.user.dto;

import io.github.atengk.audit.annotation.AuditField;
import lombok.Data;

/**
 * 用户审计快照
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class UserAuditSnapshot {

    @AuditField(label = "用户名")
    private String username;

    @AuditField(label = "用户昵称")
    private String nickname;

    @AuditField(label = "手机号", sensitive = true)
    private String phone;

    @AuditField(label = "用户状态")
    private String status;
}
```

用户快照提供器根据业务 ID 查询当前数据库数据，并转换成快照对象。

文件位置：`src/main/java/io/github/atengk/user/audit/UserAuditSnapshotProvider.java`

```java
package io.github.atengk.user.audit;

import cn.hutool.core.bean.BeanUtil;
import io.github.atengk.audit.service.AuditSnapshotProvider;
import io.github.atengk.user.dto.UserAuditSnapshot;
import io.github.atengk.user.entity.SysUser;
import io.github.atengk.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户审计快照提供器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component("userAuditSnapshotProvider")
@RequiredArgsConstructor
public class UserAuditSnapshotProvider implements AuditSnapshotProvider {

    private final SysUserMapper sysUserMapper;

    /**
     * 查询用户审计快照
     *
     * @param businessId 用户ID
     * @return 用户快照
     */
    @Override
    public Object querySnapshot(String businessId) {
        SysUser user = sysUserMapper.selectById(Long.valueOf(businessId));
        if (user == null) {
            return null;
        }

        return BeanUtil.copyProperties(user, UserAuditSnapshot.class);
    }
}
```

用户业务方法加上 `@AuditLog` 注解即可接入审计。`businessId = "#id"` 表示从方法参数 `id` 中获取业务 ID。

文件位置：`src/main/java/io/github/atengk/user/service/impl/SysUserServiceImpl.java`

```java
package io.github.atengk.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.audit.annotation.AuditLog;
import io.github.atengk.audit.enums.AuditOperationType;
import io.github.atengk.user.entity.SysUser;
import io.github.atengk.user.mapper.SysUserMapper;
import io.github.atengk.user.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * 禁用用户
     *
     * @param id 用户ID
     */
    @Override
    @AuditLog(
            module = "用户管理",
            operationType = AuditOperationType.DISABLE,
            operationDesc = "禁用用户",
            businessType = "USER",
            businessId = "#id",
            recordDataChange = true,
            snapshotBean = "userAuditSnapshotProvider"
    )
    public void disableUser(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus("DISABLED");

        this.updateById(user);
        log.info("用户禁用成功，用户ID：{}", id);
    }
}
```

执行 `disableUser(10001L)` 后，会产生类似日志：

```text
sys_audit_log:
module_name = 用户管理
operation_type = DISABLE
business_type = USER
business_id = 10001
success = 1

sys_audit_data_change_log:
field_name = status
field_label = 用户状态
old_value = ENABLED
new_value = DISABLED
```

### 订单关键字段变更留痕

订单场景通常更适合字段级留痕，例如订单状态、支付金额、收货人、收货手机号等字段。下面以取消订单为例。

文件位置：`src/main/java/io/github/atengk/order/dto/OrderAuditSnapshot.java`

```java
package io.github.atengk.order.dto;

import io.github.atengk.audit.annotation.AuditField;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单审计快照
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderAuditSnapshot {

    @AuditField(label = "订单状态")
    private String status;

    @AuditField(label = "支付金额")
    private BigDecimal payAmount;

    @AuditField(label = "收货人")
    private String receiverName;

    @AuditField(label = "收货手机号", sensitive = true)
    private String receiverPhone;

    @AuditField(label = "订单备注")
    private String remark;
}
```

文件位置：`src/main/java/io/github/atengk/order/audit/OrderAuditSnapshotProvider.java`

```java
package io.github.atengk.order.audit;

import cn.hutool.core.bean.BeanUtil;
import io.github.atengk.audit.service.AuditSnapshotProvider;
import io.github.atengk.order.dto.OrderAuditSnapshot;
import io.github.atengk.order.entity.OrderInfo;
import io.github.atengk.order.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 订单审计快照提供器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component("orderAuditSnapshotProvider")
@RequiredArgsConstructor
public class OrderAuditSnapshotProvider implements AuditSnapshotProvider {

    private final OrderInfoMapper orderInfoMapper;

    /**
     * 查询订单审计快照
     *
     * @param businessId 订单ID
     * @return 订单快照
     */
    @Override
    public Object querySnapshot(String businessId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(Long.valueOf(businessId));
        if (orderInfo == null) {
            return null;
        }

        return BeanUtil.copyProperties(orderInfo, OrderAuditSnapshot.class);
    }
}
```

订单取消方法使用 `@AuditLog` 记录操作行为和字段变更。这里的业务 ID 从请求对象中获取，因此表达式写成 `#request.orderId`。

文件位置：`src/main/java/io/github/atengk/order/service/impl/OrderServiceImpl.java`

```java
package io.github.atengk.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.audit.annotation.AuditLog;
import io.github.atengk.audit.enums.AuditOperationType;
import io.github.atengk.order.dto.CancelOrderRequest;
import io.github.atengk.order.entity.OrderInfo;
import io.github.atengk.order.mapper.OrderInfoMapper;
import io.github.atengk.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    /**
     * 取消订单
     *
     * @param request 取消订单请求
     */
    @Override
    @AuditLog(
            module = "订单管理",
            operationType = AuditOperationType.UPDATE,
            operationDesc = "取消订单",
            businessType = "ORDER",
            businessId = "#request.orderId",
            recordDataChange = true,
            snapshotBean = "orderAuditSnapshotProvider"
    )
    public void cancelOrder(CancelOrderRequest request) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(request.getOrderId());
        orderInfo.setStatus("CANCELED");
        orderInfo.setRemark(request.getReason());

        this.updateById(orderInfo);
        log.info("订单取消成功，订单ID：{}，原因：{}", request.getOrderId(), request.getReason());
    }
}
```

取消订单请求 DTO 如下。

文件位置：`src/main/java/io/github/atengk/order/dto/CancelOrderRequest.java`

```java
package io.github.atengk.order.dto;

import lombok.Data;

/**
 * 取消订单请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CancelOrderRequest {

    private Long orderId;

    private String reason;
}
```

执行取消订单后，可能产生如下变更记录：

| 字段     | 修改前   | 修改后       |
| -------- | -------- | ------------ |
| 订单状态 | WAIT_PAY | CANCELED     |
| 订单备注 | 空       | 用户申请取消 |

### 字段变更内容格式化

数据库中通常保存原始枚举值，例如 `ENABLED`、`DISABLED`、`WAIT_PAY`、`CANCELED`。如果后台页面需要展示中文，可以在查询接口或 VO 转换时做格式化，不建议把中文值直接替代原始值写入数据库。

下面给出一个简单的字段值格式化工具，用于将审计日志中的枚举值转换为页面可读文本。

文件位置：`src/main/java/io/github/atengk/audit/util/AuditValueFormatUtil.java`

```java
package io.github.atengk.audit.util;

import cn.hutool.core.util.StrUtil;

import java.util.Map;

/**
 * 审计字段值格式化工具
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class AuditValueFormatUtil {

    private static final Map<String, Map<String, String>> VALUE_MAPPING = Map.of(
            "status", Map.of(
                    "ENABLED", "启用",
                    "DISABLED", "禁用",
                    "WAIT_PAY", "待支付",
                    "PAID", "已支付",
                    "CANCELED", "已取消"
            )
    );

    /**
     * 格式化字段值
     *
     * @param fieldName 字段名
     * @param value 字段值
     * @return 格式化后的值
     */
    public static String format(String fieldName, String value) {
        if (StrUtil.isBlank(value)) {
            return "空";
        }

        Map<String, String> mapping = VALUE_MAPPING.get(fieldName);
        if (mapping == null) {
            return value;
        }

        return mapping.getOrDefault(value, value);
    }
}
```

页面展示时可以在 VO 转换阶段使用该工具。

文件位置：`src/main/java/io/github/atengk/audit/vo/AuditDataChangeLogVO.java`

```java
package io.github.atengk.audit.vo;

import io.github.atengk.audit.entity.SysAuditDataChangeLog;
import io.github.atengk.audit.util.AuditValueFormatUtil;
import lombok.Data;

/**
 * 数据变更日志展示对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AuditDataChangeLogVO {

    private String fieldName;

    private String fieldLabel;

    private String oldValue;

    private String newValue;

    /**
     * 从实体转换为展示对象
     *
     * @param entity 数据变更日志
     * @return 展示对象
     */
    public static AuditDataChangeLogVO fromEntity(SysAuditDataChangeLog entity) {
        AuditDataChangeLogVO vo = new AuditDataChangeLogVO();
        vo.setFieldName(entity.getFieldName());
        vo.setFieldLabel(entity.getFieldLabel());
        vo.setOldValue(AuditValueFormatUtil.format(entity.getFieldName(), entity.getOldValue()));
        vo.setNewValue(AuditValueFormatUtil.format(entity.getFieldName(), entity.getNewValue()));
        return vo;
    }
}
```

格式化后的展示效果如下：

| 字段       | 修改前        | 修改后        |
| ---------- | ------------- | ------------- |
| 用户状态   | 启用          | 禁用          |
| 订单状态   | 待支付        | 已取消        |
| 收货手机号 | 138********88 | 139********99 |

这个实现方式保留了数据库中的原始值，同时又能在页面上展示更友好的中文内容。后续如果字段字典来自数据库，只需要把 `AuditValueFormatUtil` 中的静态映射替换为字典服务查询即可。

## 查询接口实现

查询接口主要用于后台页面查看审计记录和数据变更历史。本节基于前面已经创建的 `sys_audit_log`、`sys_audit_data_change_log` 两张表实现三个接口：

| 接口                    | 作用                           |
| ----------------------- | ------------------------------ |
| `/audit/log/page`       | 分页查询操作审计日志           |
| `/audit/change/history` | 查询某条业务数据的字段变更历史 |
| `/audit/change/track`   | 查询某条业务数据的完整变更轨迹 |

如果项目中已有统一返回对象、分页对象或权限控制，可以直接替换 Controller 中的返回类型和鉴权注解。

### 操作审计分页查询

操作审计分页查询用于展示后台操作日志列表，常见筛选条件包括模块、操作类型、业务类型、业务 ID、操作人、执行结果、操作时间范围。

查询参数 DTO 如下。

文件位置：`src/main/java/io/github/atengk/audit/dto/AuditLogPageQueryDTO.java`

```java
package io.github.atengk.audit.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志分页查询参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AuditLogPageQueryDTO {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String moduleName;

    private String operationType;

    private String businessType;

    private String businessId;

    private String operatorName;

    private Boolean success;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;
}
```

分页列表展示对象如下，只返回页面需要展示的核心字段，不直接暴露完整请求参数和异常堆栈。

文件位置：`src/main/java/io/github/atengk/audit/vo/AuditLogPageVO.java`

```java
package io.github.atengk.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志分页展示对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AuditLogPageVO {

    private Long id;

    private String moduleName;

    private String operationType;

    private String operationDesc;

    private String businessType;

    private String businessId;

    private String requestMethod;

    private String requestUri;

    private Long operatorId;

    private String operatorName;

    private String clientIp;

    private Boolean success;

    private String errorMessage;

    private Long costTime;

    private LocalDateTime operationTime;
}
```

查询服务接口统一放在 `AuditQueryService` 中，便于后续继续扩展审计详情、导出、统计等能力。

文件位置：`src/main/java/io/github/atengk/audit/service/AuditQueryService.java`

```java
package io.github.atengk.audit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.atengk.audit.dto.AuditLogPageQueryDTO;
import io.github.atengk.audit.vo.AuditBusinessChangeTrackVO;
import io.github.atengk.audit.vo.AuditDataChangeHistoryVO;
import io.github.atengk.audit.vo.AuditLogPageVO;

import java.util.List;

/**
 * 审计查询服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface AuditQueryService {

    /**
     * 分页查询操作审计日志
     *
     * @param query 查询参数
     * @return 分页结果
     */
    IPage<AuditLogPageVO> pageAuditLog(AuditLogPageQueryDTO query);

    /**
     * 查询业务数据变更历史
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 变更历史
     */
    List<AuditDataChangeHistoryVO> listChangeHistory(String businessType, String businessId);

    /**
     * 查询单条业务数据完整变更轨迹
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 变更轨迹
     */
    List<AuditBusinessChangeTrackVO> listBusinessChangeTrack(String businessType, String businessId);
}
```

服务实现类通过 MyBatis-Plus 的 `LambdaQueryWrapper` 动态拼接查询条件。

文件位置：`src/main/java/io/github/atengk/audit/service/impl/AuditQueryServiceImpl.java`

```java
package io.github.atengk.audit.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.audit.dto.AuditLogPageQueryDTO;
import io.github.atengk.audit.entity.SysAuditDataChangeLog;
import io.github.atengk.audit.entity.SysAuditLog;
import io.github.atengk.audit.mapper.SysAuditDataChangeLogMapper;
import io.github.atengk.audit.mapper.SysAuditLogMapper;
import io.github.atengk.audit.service.AuditQueryService;
import io.github.atengk.audit.vo.AuditBusinessChangeTrackVO;
import io.github.atengk.audit.vo.AuditDataChangeHistoryVO;
import io.github.atengk.audit.vo.AuditLogPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审计查询服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditQueryServiceImpl implements AuditQueryService {

    private final SysAuditLogMapper sysAuditLogMapper;
    private final SysAuditDataChangeLogMapper sysAuditDataChangeLogMapper;

    /**
     * 分页查询操作审计日志
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public IPage<AuditLogPageVO> pageAuditLog(AuditLogPageQueryDTO query) {
        Page<SysAuditLog> page = Page.of(query.getPageNum(), query.getPageSize());

        Page<SysAuditLog> resultPage = sysAuditLogMapper.selectPage(
                page,
                Wrappers.<SysAuditLog>lambdaQuery()
                        .like(StrUtil.isNotBlank(query.getModuleName()), SysAuditLog::getModuleName, query.getModuleName())
                        .eq(StrUtil.isNotBlank(query.getOperationType()), SysAuditLog::getOperationType, query.getOperationType())
                        .eq(StrUtil.isNotBlank(query.getBusinessType()), SysAuditLog::getBusinessType, query.getBusinessType())
                        .eq(StrUtil.isNotBlank(query.getBusinessId()), SysAuditLog::getBusinessId, query.getBusinessId())
                        .like(StrUtil.isNotBlank(query.getOperatorName()), SysAuditLog::getOperatorName, query.getOperatorName())
                        .eq(query.getSuccess() != null, SysAuditLog::getSuccess, query.getSuccess())
                        .ge(query.getBeginTime() != null, SysAuditLog::getOperationTime, query.getBeginTime())
                        .le(query.getEndTime() != null, SysAuditLog::getOperationTime, query.getEndTime())
                        .orderByDesc(SysAuditLog::getOperationTime)
        );

        IPage<AuditLogPageVO> voPage = resultPage.convert(item -> BeanUtil.copyProperties(item, AuditLogPageVO.class));
        log.info("操作审计日志分页查询完成，当前页：{}，每页数量：{}，总数：{}",
                query.getPageNum(), query.getPageSize(), voPage.getTotal());
        return voPage;
    }

    /**
     * 查询业务数据变更历史
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 变更历史
     */
    @Override
    public List<AuditDataChangeHistoryVO> listChangeHistory(String businessType, String businessId) {
        List<SysAuditDataChangeLog> list = sysAuditDataChangeLogMapper.selectList(
                Wrappers.<SysAuditDataChangeLog>lambdaQuery()
                        .eq(SysAuditDataChangeLog::getBusinessType, businessType)
                        .eq(SysAuditDataChangeLog::getBusinessId, businessId)
                        .orderByDesc(SysAuditDataChangeLog::getChangeTime)
                        .orderByAsc(SysAuditDataChangeLog::getId)
        );

        log.info("数据变更历史查询完成，业务类型：{}，业务ID：{}，记录数：{}",
                businessType, businessId, list.size());

        return list.stream()
                .map(AuditDataChangeHistoryVO::fromEntity)
                .toList();
    }

    /**
     * 查询单条业务数据完整变更轨迹
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 变更轨迹
     */
    @Override
    public List<AuditBusinessChangeTrackVO> listBusinessChangeTrack(String businessType, String businessId) {
        List<SysAuditLog> auditLogs = sysAuditLogMapper.selectList(
                Wrappers.<SysAuditLog>lambdaQuery()
                        .eq(SysAuditLog::getBusinessType, businessType)
                        .eq(SysAuditLog::getBusinessId, businessId)
                        .orderByDesc(SysAuditLog::getOperationTime)
        );

        if (CollUtil.isEmpty(auditLogs)) {
            return CollUtil.newArrayList();
        }

        List<Long> auditLogIds = auditLogs.stream()
                .map(SysAuditLog::getId)
                .toList();

        List<SysAuditDataChangeLog> changeLogs = sysAuditDataChangeLogMapper.selectList(
                Wrappers.<SysAuditDataChangeLog>lambdaQuery()
                        .in(SysAuditDataChangeLog::getAuditLogId, auditLogIds)
                        .orderByAsc(SysAuditDataChangeLog::getId)
        );

        Map<Long, List<AuditDataChangeHistoryVO>> changeMap = changeLogs.stream()
                .collect(Collectors.groupingBy(
                        SysAuditDataChangeLog::getAuditLogId,
                        Collectors.mapping(AuditDataChangeHistoryVO::fromEntity, Collectors.toList())
                ));

        return auditLogs.stream()
                .sorted(Comparator.comparing(SysAuditLog::getOperationTime).reversed())
                .map(auditLog -> {
                    AuditBusinessChangeTrackVO vo = BeanUtil.copyProperties(auditLog, AuditBusinessChangeTrackVO.class);
                    vo.setChanges(changeMap.getOrDefault(auditLog.getId(), CollUtil.newArrayList()));
                    return vo;
                })
                .toList();
    }
}
```

如果项目还没有配置 MyBatis-Plus 分页插件，需要补充分页配置。

文件位置：`src/main/java/io/github/atengk/config/MybatisPlusConfig.java`

```java
package io.github.atengk.config;

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
     * 配置分页插件
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // MySQL 分页插件，其他数据库需要替换 DbType
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

### 数据变更历史查询

数据变更历史查询用于查看某条业务数据的所有字段变更记录。例如查询用户 `10001` 的所有变更字段。

展示对象如下。

文件位置：`src/main/java/io/github/atengk/audit/vo/AuditDataChangeHistoryVO.java`

```java
package io.github.atengk.audit.vo;

import io.github.atengk.audit.entity.SysAuditDataChangeLog;
import io.github.atengk.audit.util.AuditValueFormatUtil;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据变更历史展示对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AuditDataChangeHistoryVO {

    private Long id;

    private Long auditLogId;

    private String businessType;

    private String businessId;

    private String fieldName;

    private String fieldLabel;

    private String oldValue;

    private String newValue;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime changeTime;

    /**
     * 从实体转换为展示对象
     *
     * @param entity 数据变更日志实体
     * @return 展示对象
     */
    public static AuditDataChangeHistoryVO fromEntity(SysAuditDataChangeLog entity) {
        AuditDataChangeHistoryVO vo = new AuditDataChangeHistoryVO();
        vo.setId(entity.getId());
        vo.setAuditLogId(entity.getAuditLogId());
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessId(entity.getBusinessId());
        vo.setFieldName(entity.getFieldName());
        vo.setFieldLabel(entity.getFieldLabel());
        vo.setOldValue(AuditValueFormatUtil.format(entity.getFieldName(), entity.getOldValue()));
        vo.setNewValue(AuditValueFormatUtil.format(entity.getFieldName(), entity.getNewValue()));
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setChangeTime(entity.getChangeTime());
        return vo;
    }
}
```

查询接口 Controller 如下。

文件位置：`src/main/java/io/github/atengk/audit/controller/AuditQueryController.java`

```java
package io.github.atengk.audit.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.atengk.audit.dto.AuditLogPageQueryDTO;
import io.github.atengk.audit.service.AuditQueryService;
import io.github.atengk.audit.vo.AuditBusinessChangeTrackVO;
import io.github.atengk.audit.vo.AuditDataChangeHistoryVO;
import io.github.atengk.audit.vo.AuditLogPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审计查询接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/audit")
public class AuditQueryController {

    private final AuditQueryService auditQueryService;

    /**
     * 分页查询操作审计日志
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @PostMapping("/log/page")
    public IPage<AuditLogPageVO> pageAuditLog(@RequestBody AuditLogPageQueryDTO query) {
        log.info("接收操作审计分页查询请求，业务类型：{}，业务ID：{}", query.getBusinessType(), query.getBusinessId());
        return auditQueryService.pageAuditLog(query);
    }

    /**
     * 查询数据变更历史
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 数据变更历史
     */
    @GetMapping("/change/history")
    public List<AuditDataChangeHistoryVO> listChangeHistory(@RequestParam String businessType,
                                                            @RequestParam String businessId) {
        log.info("接收数据变更历史查询请求，业务类型：{}，业务ID：{}", businessType, businessId);
        return auditQueryService.listChangeHistory(businessType, businessId);
    }

    /**
     * 查询单条业务数据变更轨迹
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 变更轨迹
     */
    @GetMapping("/change/track")
    public List<AuditBusinessChangeTrackVO> listBusinessChangeTrack(@RequestParam String businessType,
                                                                    @RequestParam String businessId) {
        log.info("接收业务数据变更轨迹查询请求，业务类型：{}，业务ID：{}", businessType, businessId);
        return auditQueryService.listBusinessChangeTrack(businessType, businessId);
    }
}
```

调用示例：

```bash
# 分页查询操作审计日志
curl -X POST 'http://localhost:8080/audit/log/page' \
  -H 'Content-Type: application/json' \
  -d '{
    "pageNum": 1,
    "pageSize": 10,
    "businessType": "USER",
    "operatorName": "admin",
    "success": true
  }'

# 查询用户 10001 的字段变更历史
curl 'http://localhost:8080/audit/change/history?businessType=USER&businessId=10001'
```

分页查询响应示例：

```json
{
  "records": [
    {
      "id": 1,
      "moduleName": "用户管理",
      "operationType": "DISABLE",
      "operationDesc": "禁用用户",
      "businessType": "USER",
      "businessId": "10001",
      "requestMethod": "POST",
      "requestUri": "/user/disable/10001",
      "operatorId": 1,
      "operatorName": "admin",
      "clientIp": "127.0.0.1",
      "success": true,
      "errorMessage": null,
      "costTime": 36,
      "operationTime": "2026-05-15T10:30:00"
    }
  ],
  "total": 1,
  "size": 10,
  "current": 1,
  "pages": 1
}
```

### 单条业务数据变更轨迹查询

单条业务数据变更轨迹查询用于按时间线展示某条数据从创建到多次修改的完整操作过程。它比字段变更历史更完整，因为它会同时展示操作行为和字段变化。

轨迹展示对象如下。

文件位置：`src/main/java/io/github/atengk/audit/vo/AuditBusinessChangeTrackVO.java`

```java
package io.github.atengk.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单条业务数据变更轨迹展示对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AuditBusinessChangeTrackVO {

    private Long id;

    private String moduleName;

    private String operationType;

    private String operationDesc;

    private String businessType;

    private String businessId;

    private Long operatorId;

    private String operatorName;

    private Boolean success;

    private String errorMessage;

    private Long costTime;

    private LocalDateTime operationTime;

    private List<AuditDataChangeHistoryVO> changes;
}
```

调用示例：

```bash
# 查询订单 20001 的完整变更轨迹
curl 'http://localhost:8080/audit/change/track?businessType=ORDER&businessId=20001'
```

响应示例：

```json
[
  {
    "id": 10,
    "moduleName": "订单管理",
    "operationType": "UPDATE",
    "operationDesc": "取消订单",
    "businessType": "ORDER",
    "businessId": "20001",
    "operatorId": 1,
    "operatorName": "admin",
    "success": true,
    "errorMessage": null,
    "costTime": 42,
    "operationTime": "2026-05-15T11:20:00",
    "changes": [
      {
        "fieldName": "status",
        "fieldLabel": "订单状态",
        "oldValue": "待支付",
        "newValue": "已取消"
      },
      {
        "fieldName": "remark",
        "fieldLabel": "订单备注",
        "oldValue": "空",
        "newValue": "用户申请取消"
      }
    ]
  }
]
```

这个接口适合做“数据详情页的操作轨迹”功能。例如用户详情、订单详情、审批详情页面底部展示一条时间线。

## 功能验证

功能验证主要验证四类场景：新增、修改、删除、异常。核心判断标准是：业务操作完成后，`sys_audit_log` 是否有记录；如果涉及关键字段修改，`sys_audit_data_change_log` 是否有对应字段变化。

### 新增操作审计验证

新增操作通常只需要记录操作行为，不一定需要字段级变更留痕。因为新增前不存在旧数据，字段对比意义不大。如果业务要求展示创建时的初始字段，也可以扩展为记录 `null -> 新值`。

新增用户方法示例：

```java
@AuditLog(
        module = "用户管理",
        operationType = AuditOperationType.CREATE,
        operationDesc = "新增用户",
        businessType = "USER",
        businessId = "#user.id",
        recordDataChange = false
)
public void createUser(SysUser user) {
    this.save(user);
    log.info("用户新增成功，用户ID：{}，用户名：{}", user.getId(), user.getUsername());
}
```

验证步骤：

```bash
# 新增用户
curl -X POST 'http://localhost:8080/user/create' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "audit_test",
    "nickname": "审计测试用户",
    "phone": "13888889999",
    "status": "ENABLED"
  }'

# 查询用户新增操作审计日志
curl -X POST 'http://localhost:8080/audit/log/page' \
  -H 'Content-Type: application/json' \
  -d '{
    "pageNum": 1,
    "pageSize": 10,
    "businessType": "USER",
    "operationType": "CREATE"
  }'
```

数据库验证 SQL：

```sql
SELECT id,
       module_name,
       operation_type,
       operation_desc,
       business_type,
       business_id,
       operator_name,
       success,
       operation_time
FROM sys_audit_log
WHERE business_type = 'USER'
  AND operation_type = 'CREATE'
ORDER BY operation_time DESC;
```

预期结果：

| 检查项                      | 预期                             |
| --------------------------- | -------------------------------- |
| `sys_audit_log`             | 存在一条 `CREATE` 记录           |
| `success`                   | 值为 `1`                         |
| `business_type`             | 值为 `USER`                      |
| `request_params`            | 保存新增接口参数，敏感字段已脱敏 |
| `sys_audit_data_change_log` | 不产生字段变更记录               |

### 修改操作留痕验证

修改操作是本案例的重点。以禁用用户为例，执行前用户状态为 `ENABLED`，执行后状态为 `DISABLED`，系统应该记录一条操作日志和一条字段变更日志。

验证步骤：

```bash
# 禁用用户
curl -X POST 'http://localhost:8080/user/disable/10001'

# 查询操作审计日志
curl -X POST 'http://localhost:8080/audit/log/page' \
  -H 'Content-Type: application/json' \
  -d '{
    "pageNum": 1,
    "pageSize": 10,
    "businessType": "USER",
    "businessId": "10001",
    "operationType": "DISABLE"
  }'

# 查询字段变更历史
curl 'http://localhost:8080/audit/change/history?businessType=USER&businessId=10001'
```

数据库验证 SQL：

```sql
SELECT id,
       module_name,
       operation_type,
       business_type,
       business_id,
       success
FROM sys_audit_log
WHERE business_type = 'USER'
  AND business_id = '10001'
ORDER BY operation_time DESC;

SELECT business_type,
       business_id,
       field_name,
       field_label,
       old_value,
       new_value,
       operator_name,
       change_time
FROM sys_audit_data_change_log
WHERE business_type = 'USER'
  AND business_id = '10001'
ORDER BY change_time DESC;
```

预期结果：

| 检查项       | 预期                    |
| ------------ | ----------------------- |
| 操作审计日志 | 存在一条 `DISABLE` 记录 |
| 数据变更日志 | 存在 `status` 字段变化  |
| `old_value`  | `ENABLED`               |
| `new_value`  | `DISABLED`              |
| 操作人       | 与当前登录用户一致      |
| 操作时间     | 与业务操作时间接近      |

### 删除操作审计验证

删除操作分为逻辑删除和物理删除。后台系统建议使用逻辑删除，这样删除前后仍然可以查询业务数据状态，也方便审计留痕。

逻辑删除方法示例：

```java
@AuditLog(
        module = "用户管理",
        operationType = AuditOperationType.DELETE,
        operationDesc = "删除用户",
        businessType = "USER",
        businessId = "#id",
        recordDataChange = true,
        snapshotBean = "userAuditSnapshotProvider"
)
public void deleteUser(Long id) {
    SysUser user = new SysUser();
    user.setId(id);
    user.setDeleted(1);
    this.updateById(user);

    log.info("用户删除成功，用户ID：{}", id);
}
```

如果希望记录 `deleted` 字段变化，需要在用户快照对象中加入该字段：

```java
@AuditField(label = "删除状态")
private Integer deleted;
```

验证步骤：

```bash
# 删除用户
curl -X DELETE 'http://localhost:8080/user/10001'

# 查询删除操作日志
curl -X POST 'http://localhost:8080/audit/log/page' \
  -H 'Content-Type: application/json' \
  -d '{
    "pageNum": 1,
    "pageSize": 10,
    "businessType": "USER",
    "businessId": "10001",
    "operationType": "DELETE"
  }'
```

数据库验证 SQL：

```sql
SELECT id,
       module_name,
       operation_type,
       operation_desc,
       business_type,
       business_id,
       success,
       operation_time
FROM sys_audit_log
WHERE business_type = 'USER'
  AND business_id = '10001'
  AND operation_type = 'DELETE'
ORDER BY operation_time DESC;

SELECT field_name,
       field_label,
       old_value,
       new_value
FROM sys_audit_data_change_log
WHERE business_type = 'USER'
  AND business_id = '10001'
ORDER BY change_time DESC;
```

预期结果：

| 检查项       | 预期                               |
| ------------ | ---------------------------------- |
| 操作审计日志 | 存在 `DELETE` 记录                 |
| 执行结果     | 成功                               |
| 字段变更日志 | 逻辑删除时可记录 `deleted: 0 -> 1` |
| 请求参数     | 不包含敏感信息                     |

如果是物理删除，建议至少在删除前记录完整关键字段快照，否则删除后无法再次查询变更后数据。实际项目中，关键数据不建议物理删除。

### 异常场景验证

异常场景用于验证业务方法抛出异常时，审计日志仍然可以落库，并且 `success = false`，`error_message` 有异常摘要。

异常方法示例：

```java
@AuditLog(
        module = "用户管理",
        operationType = AuditOperationType.UPDATE,
        operationDesc = "异常测试",
        businessType = "USER",
        businessId = "#id",
        recordDataChange = true,
        snapshotBean = "userAuditSnapshotProvider"
)
public void updateUserWithException(Long id) {
    log.info("开始执行异常测试，用户ID：{}", id);
    throw new IllegalArgumentException("用户状态不允许修改");
}
```

验证步骤：

```bash
# 调用异常接口
curl -X POST 'http://localhost:8080/user/exception-test/10001'

# 查询失败审计日志
curl -X POST 'http://localhost:8080/audit/log/page' \
  -H 'Content-Type: application/json' \
  -d '{
    "pageNum": 1,
    "pageSize": 10,
    "businessType": "USER",
    "businessId": "10001",
    "success": false
  }'
```

数据库验证 SQL：

```sql
SELECT id,
       module_name,
       operation_type,
       business_type,
       business_id,
       success,
       error_message,
       cost_time,
       operation_time
FROM sys_audit_log
WHERE business_type = 'USER'
  AND business_id = '10001'
  AND success = 0
ORDER BY operation_time DESC;
```

预期结果：

| 检查项                      | 预期           |
| --------------------------- | -------------- |
| `sys_audit_log`             | 存在失败记录   |
| `success`                   | 值为 `0`       |
| `error_message`             | 包含异常摘要   |
| `cost_time`                 | 有执行耗时     |
| `sys_audit_data_change_log` | 不记录字段变化 |

异常场景下不建议记录字段变更，因为业务操作失败时最终数据通常没有发生变化。前面的 AOP 实现中也只在 `success = true` 时才写入字段变更日志。

## 扩展优化

前面的实现已经可以满足大多数后台系统的基础审计需求。生产环境中通常还需要考虑写入性能、敏感信息保护、日志数据量增长等问题。

### 异步写入审计日志

同步写入审计日志实现简单，但会增加业务接口耗时。如果审计日志量较大，可以将日志写入改为异步执行。常见方案有三种：

| 方案                 | 说明                               | 适用场景       |
| -------------------- | ---------------------------------- | -------------- |
| Spring `@Async`      | 实现简单，改造成本低               | 中小型后台系统 |
| MQ                   | 业务服务投递消息，日志服务消费入库 | 高并发、多服务 |
| Disruptor / 本地队列 | 性能较高，但实现复杂               | 单体高吞吐场景 |

中小型项目可以先使用 `@Async`。

启用异步配置如下。

文件位置：`src/main/java/io/github/atengk/config/AsyncConfig.java`

```java
package io.github.atengk.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 异步任务配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 异步异常处理器
     *
     * @return 异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) -> log.error(
                "异步审计任务执行失败，方法：{}，参数：{}，原因：{}",
                method.getName(),
                Arrays.toString(params),
                ex.getMessage(),
                ex
        );
    }
}
```

然后在审计记录服务实现类的 `record` 方法上添加 `@Async`。

```java
@Async
@Override
public void record(AuditLog auditLog,
                   RequestContextHelper.RequestContext requestContext,
                   String businessId,
                   boolean success,
                   String errorMessage,
                   long costTime,
                   List<AuditFieldChange> fieldChanges) {
    // 复用前面的审计日志入库逻辑
}
```

使用异步写入时要注意：不要在异步方法里再读取 `HttpServletRequest`，因为请求线程结束后上下文可能已经失效。前面的实现已经在 AOP 线程中提前提取了 `RequestContext`，所以适合改成异步。

如果使用 MQ，建议消息体包含以下内容：

```json
{
  "auditLog": {
    "moduleName": "用户管理",
    "operationType": "DISABLE",
    "businessType": "USER",
    "businessId": "10001",
    "operatorId": 1,
    "operatorName": "admin",
    "success": true,
    "operationTime": "2026-05-15T10:30:00"
  },
  "fieldChanges": [
    {
      "fieldName": "status",
      "fieldLabel": "用户状态",
      "oldValue": "ENABLED",
      "newValue": "DISABLED"
    }
  ]
}
```

### 敏感字段脱敏

敏感字段脱敏需要同时覆盖请求参数和字段变更日志。常见敏感字段包括密码、手机号、身份证号、银行卡号、Token、密钥等。

前面已经通过 `AuditSensitiveUtil` 处理了部分场景。生产环境可以进一步增强为“字段名规则 + 注解配置 + 字段类型策略”。

增强版脱敏类型枚举如下。

文件位置：`src/main/java/io/github/atengk/audit/enums/AuditSensitiveType.java`

```java
package io.github.atengk.audit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审计脱敏类型
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum AuditSensitiveType {

    NONE("不脱敏"),
    PASSWORD("密码"),
    MOBILE("手机号"),
    ID_CARD("身份证号"),
    BANK_CARD("银行卡号"),
    TOKEN("令牌"),
    DEFAULT("默认脱敏");

    private final String description;
}
```

可以将前面的 `@AuditField` 注解从 `boolean sensitive` 升级为脱敏类型。

```java
AuditSensitiveType sensitiveType() default AuditSensitiveType.NONE;
```

增强版字段值脱敏方法如下。

```java
public static String desensitizeValue(Object value, AuditSensitiveType sensitiveType) {
    if (value == null) {
        return null;
    }

    String text = String.valueOf(value);
    if (StrUtil.isBlank(text) || sensitiveType == AuditSensitiveType.NONE) {
        return text;
    }

    return switch (sensitiveType) {
        case PASSWORD, TOKEN -> "******";
        case MOBILE -> DesensitizedUtil.mobilePhone(text);
        case ID_CARD -> DesensitizedUtil.idCardNum(text, 2, 2);
        case BANK_CARD -> DesensitizedUtil.bankCard(text);
        case DEFAULT -> StrUtil.length(text) <= 6 ? "******" : DesensitizedUtil.idCardNum(text, 2, 2);
        default -> text;
    };
}
```

业务快照字段可以这样配置：

```java
@AuditField(label = "手机号", sensitiveType = AuditSensitiveType.MOBILE)
private String phone;

@AuditField(label = "身份证号", sensitiveType = AuditSensitiveType.ID_CARD)
private String idCard;

@AuditField(label = "访问令牌", sensitiveType = AuditSensitiveType.TOKEN)
private String accessToken;
```

脱敏建议：

| 数据类型 | 存储建议                          |
| -------- | --------------------------------- |
| 密码     | 不记录原值和新值，只记录 `******` |
| Token    | 不记录原值和新值，只记录 `******` |
| 手机号   | 记录脱敏值，例如 `138****9999`    |
| 身份证号 | 记录脱敏值                        |
| 银行卡号 | 记录脱敏值                        |
| 普通枚举 | 可以记录原始值                    |
| 金额字段 | 通常可以记录，但要控制查询权限    |

审计日志查询接口建议加权限控制。不是所有管理员都应该能查看完整审计信息，尤其是包含手机号、金额、地址等数据的系统。

### 日志归档与清理

审计日志会持续增长，如果长期不清理，会影响查询性能和数据库存储。生产环境建议制定保留策略。

常见策略如下：

| 日志类型             | 建议保留时间   | 处理方式       |
| -------------------- | -------------- | -------------- |
| 普通后台操作日志     | 6 到 12 个月   | 定期归档后删除 |
| 关键业务变更日志     | 1 到 3 年      | 归档到历史表   |
| 财务、审批、合规日志 | 按公司合规要求 | 不建议直接删除 |
| 登录、导出类日志     | 6 到 12 个月   | 可按月归档     |

简单做法是按月份归档到历史表，例如：

| 当前表                      | 历史表                             |
| --------------------------- | ---------------------------------- |
| `sys_audit_log`             | `sys_audit_log_202605`             |
| `sys_audit_data_change_log` | `sys_audit_data_change_log_202605` |

如果不想动态建表，也可以使用固定历史表：

| 当前表                      | 历史表                              |
| --------------------------- | ----------------------------------- |
| `sys_audit_log`             | `sys_audit_log_history`             |
| `sys_audit_data_change_log` | `sys_audit_data_change_log_history` |

下面是固定历史表方式的归档 SQL 示例。

```sql
-- 归档 180 天以前的操作审计日志
INSERT INTO sys_audit_log_history
SELECT *
FROM sys_audit_log
WHERE operation_time < DATE_SUB(NOW(), INTERVAL 180 DAY);

-- 归档 180 天以前的数据变更日志
INSERT INTO sys_audit_data_change_log_history
SELECT c.*
FROM sys_audit_data_change_log c
INNER JOIN sys_audit_log l ON c.audit_log_id = l.id
WHERE l.operation_time < DATE_SUB(NOW(), INTERVAL 180 DAY);

-- 删除已归档的数据变更日志
DELETE c
FROM sys_audit_data_change_log c
INNER JOIN sys_audit_log l ON c.audit_log_id = l.id
WHERE l.operation_time < DATE_SUB(NOW(), INTERVAL 180 DAY);

-- 删除已归档的操作审计日志
DELETE FROM sys_audit_log
WHERE operation_time < DATE_SUB(NOW(), INTERVAL 180 DAY);
```

定时清理任务可以使用 XXL-JOB、Spring Scheduler 或数据库任务。下面给出 Spring Scheduler 示例。

文件位置：`src/main/java/io/github/atengk/audit/task/AuditLogArchiveTask.java`

```java
package io.github.atengk.audit.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审计日志归档任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogArchiveTask {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 每天凌晨3点归档180天以前的审计日志
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void archiveAuditLog() {
        log.info("开始执行审计日志归档任务");

        int changeArchiveCount = jdbcTemplate.update("""
                INSERT INTO sys_audit_data_change_log_history
                SELECT c.*
                FROM sys_audit_data_change_log c
                INNER JOIN sys_audit_log l ON c.audit_log_id = l.id
                WHERE l.operation_time < DATE_SUB(NOW(), INTERVAL 180 DAY)
                """);

        int auditArchiveCount = jdbcTemplate.update("""
                INSERT INTO sys_audit_log_history
                SELECT *
                FROM sys_audit_log
                WHERE operation_time < DATE_SUB(NOW(), INTERVAL 180 DAY)
                """);

        int changeDeleteCount = jdbcTemplate.update("""
                DELETE c
                FROM sys_audit_data_change_log c
                INNER JOIN sys_audit_log l ON c.audit_log_id = l.id
                WHERE l.operation_time < DATE_SUB(NOW(), INTERVAL 180 DAY)
                """);

        int auditDeleteCount = jdbcTemplate.update("""
                DELETE FROM sys_audit_log
                WHERE operation_time < DATE_SUB(NOW(), INTERVAL 180 DAY)
                """);

        log.info("审计日志归档任务完成，变更日志归档：{}，操作日志归档：{}，变更日志删除：{}，操作日志删除：{}",
                changeArchiveCount, auditArchiveCount, changeDeleteCount, auditDeleteCount);
    }
}
```

启用定时任务需要在启动类添加 `@EnableScheduling`。

```java
@EnableScheduling
@SpringBootApplication
public class AuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditApplication.class, args);
    }
}
```

归档注意事项：

| 注意点           | 说明                                       |
| ---------------- | ------------------------------------------ |
| 先归档变更日志   | 因为变更日志依赖操作日志 ID                |
| 先删变更日志     | 删除当前表数据时，应先删子表再删主表       |
| 归档前先备份     | 第一次上线建议先人工执行并核对数据         |
| 避免一次删除过多 | 大表建议按批次删除，例如每次 5000 条       |
| 关键日志谨慎删除 | 涉及财务、合同、审批的数据要按合规要求处理 |

对于数据量较大的系统，更推荐按月份分区或分表。查询当前日志走主表，历史查询走归档表，避免主表持续膨胀影响后台查询性能。