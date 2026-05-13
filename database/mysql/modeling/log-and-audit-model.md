# 日志与审计模型

日志与审计模型用于记录系统运行过程中的用户行为、权限变更、登录访问、接口调用和关键业务数据变更。该类数据通常不参与核心业务事务计算，但对问题排查、安全追踪、合规审计、运营分析和风控判断非常重要。

日志表通常具备数据量大、写入频繁、查询条件相对固定、保留周期明确等特点。建模时需要重点关注字段可追溯性、写入成本、查询效率、数据归档和敏感信息脱敏。

## 操作日志模型

操作日志模型用于记录用户在系统中的业务操作行为，例如新增订单、修改商品、删除附件、导出报表、审核流程、调整配置等。它关注的是“谁在什么时间，对什么对象，执行了什么操作，结果如何”。

操作日志通常面向业务排查和后台管理查询，不应承担完整的数据审计职责。完整的字段级变更追踪应放在审计日志模型中处理。

### 适用场景

操作日志适用于需要追踪用户业务行为的系统功能。它通常用于后台管理系统、业务运营系统、审批系统、配置中心、订单系统、商品系统、权限系统等场景。

常见适用场景包括：

| 场景         | 说明                                       |
| ------------ | ------------------------------------------ |
| 用户操作追踪 | 查询某个用户最近做过哪些业务操作           |
| 业务对象追踪 | 查询某个订单、商品、客户、配置项被谁操作过 |
| 问题排查     | 根据操作时间、操作人、操作结果定位异常行为 |
| 安全追溯     | 追踪删除、导出、审核、授权等高风险操作     |
| 管理后台展示 | 在后台页面展示操作日志列表                 |
| 简单合规留痕 | 保留关键业务操作记录，满足基础审计要求     |

不建议把操作日志模型用于记录字段级变更明细。字段修改前后的完整差异、审批链路快照、数据版本对比等内容应拆分到审计日志模型或历史版本模型中。

### 建模结构

操作日志表以单表为主，记录一次业务操作对应一条日志。核心建模思路是将操作主体、操作对象、操作行为、操作结果、请求上下文和扩展信息统一保存。

下面是操作日志表的基础建表结构。该结构只描述表和字段，不在此处定义普通索引，索引统一放在“索引设计”章节中说明。

```sql
CREATE TABLE biz_operation_log (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '链路追踪ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，非多租户系统可固定为0',

    operator_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '操作人ID',
    operator_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作人名称',
    operator_type VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '操作人类型：USER-普通用户，ADMIN-管理员，SYSTEM-系统任务，OPEN_API-开放接口',

    module_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '模块编码',
    module_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '模块名称',
    operation_type VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作类型，例如CREATE、UPDATE、DELETE、EXPORT、AUDIT',
    operation_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '操作名称',

    biz_type VARCHAR(64) NOT NULL DEFAULT '' COMMENT '业务类型，例如ORDER、PRODUCT、CUSTOMER',
    biz_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '业务对象ID',
    biz_no VARCHAR(64) NOT NULL DEFAULT '' COMMENT '业务单号',
    biz_desc VARCHAR(255) NOT NULL DEFAULT '' COMMENT '业务对象描述',

    request_method VARCHAR(16) NOT NULL DEFAULT '' COMMENT '请求方法',
    request_uri VARCHAR(255) NOT NULL DEFAULT '' COMMENT '请求URI',
    client_ip VARCHAR(64) NOT NULL DEFAULT '' COMMENT '客户端IP',
    user_agent VARCHAR(512) NOT NULL DEFAULT '' COMMENT '浏览器或客户端标识',

    request_params JSON NULL COMMENT '请求参数，建议脱敏后写入',
    response_data JSON NULL COMMENT '响应摘要，建议只保存必要字段',
    extra_data JSON NULL COMMENT '扩展数据',

    operation_result TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '操作结果：1-成功，2-失败',
    error_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '错误码',
    error_msg VARCHAR(512) NOT NULL DEFAULT '' COMMENT '错误信息',
    cost_ms INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '耗时，单位毫秒',

    operation_time DATETIME(3) NOT NULL COMMENT '操作时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '操作日志表';
```

该模型默认使用 `BIGINT UNSIGNED` 作为主键，适合雪花算法、号段模式或其他分布式 ID 生成方式。日志表通常不建议使用自增主键作为唯一方案，因为在高并发写入、分库分表或冷热归档场景下，外部分布式 ID 更容易扩展。

`request_params`、`response_data` 和 `extra_data` 使用 MySQL 8 的 `JSON` 类型，适合保存不固定的扩展上下文。需要注意的是，JSON 字段不适合承载主要查询条件，常用查询字段应提升为独立字段。

### 字段设计

字段设计需要保证操作日志能够回答几个核心问题：谁操作、何时操作、操作了什么对象、执行了什么动作、结果如何、请求来源是什么。

| 字段               | 类型               | 设计说明                                                   |
| ------------------ | ------------------ | ---------------------------------------------------------- |
| `id`               | `BIGINT UNSIGNED`  | 主键ID，建议由业务侧生成分布式ID                           |
| `trace_id`         | `VARCHAR(64)`      | 链路追踪ID，用于串联接口日志、操作日志和异常日志           |
| `tenant_id`        | `BIGINT UNSIGNED`  | 租户ID，多租户系统必须保留                                 |
| `operator_id`      | `BIGINT UNSIGNED`  | 操作人ID，系统任务可填0                                    |
| `operator_name`    | `VARCHAR(64)`      | 操作人名称，建议冗余保存，避免用户改名后无法还原当时上下文 |
| `operator_type`    | `VARCHAR(32)`      | 操作人类型，用于区分用户、管理员、系统任务和开放接口       |
| `module_code`      | `VARCHAR(64)`      | 模块编码，便于按系统模块筛选                               |
| `module_name`      | `VARCHAR(64)`      | 模块名称，便于后台直接展示                                 |
| `operation_type`   | `VARCHAR(64)`      | 操作类型，建议使用枚举编码                                 |
| `operation_name`   | `VARCHAR(128)`     | 操作名称，例如“新增订单”“导出客户列表”                     |
| `biz_type`         | `VARCHAR(64)`      | 业务对象类型，例如订单、商品、客户、合同                   |
| `biz_id`           | `VARCHAR(64)`      | 业务对象ID，使用字符串可兼容数字ID、UUID和外部系统编号     |
| `biz_no`           | `VARCHAR(64)`      | 业务单号，适合用户按订单号、合同号、申请单号查询           |
| `biz_desc`         | `VARCHAR(255)`     | 业务对象摘要描述，便于列表展示                             |
| `request_method`   | `VARCHAR(16)`      | HTTP 请求方法                                              |
| `request_uri`      | `VARCHAR(255)`     | 请求地址，不建议保存完整 QueryString                       |
| `client_ip`        | `VARCHAR(64)`      | 客户端IP，兼容 IPv4 和 IPv6                                |
| `user_agent`       | `VARCHAR(512)`     | 客户端标识，字段长度不宜过短                               |
| `request_params`   | `JSON`             | 请求参数，必须做敏感字段脱敏                               |
| `response_data`    | `JSON`             | 响应摘要，不建议保存完整响应体                             |
| `extra_data`       | `JSON`             | 扩展字段，例如菜单ID、客户端版本、来源系统                 |
| `operation_result` | `TINYINT UNSIGNED` | 操作结果，建议使用小整数枚举                               |
| `error_code`       | `VARCHAR(64)`      | 失败时记录错误码                                           |
| `error_msg`        | `VARCHAR(512)`     | 失败时记录错误摘要，不建议保存完整堆栈                     |
| `cost_ms`          | `INT UNSIGNED`     | 操作耗时，可用于分析慢操作                                 |
| `operation_time`   | `DATETIME(3)`      | 真实操作发生时间                                           |
| `created_at`       | `DATETIME(3)`      | 日志落库时间                                               |

字段设计时需要避免两个常见问题。第一，不要只保存用户ID而不保存用户名称，否则历史日志展示会受用户资料变化影响。第二，不要把所有业务上下文都塞进 JSON 字段，否则后续按业务对象、模块、操作类型查询时会很难优化。

### 索引设计

操作日志表的索引设计应围绕后台列表查询、用户行为追踪、业务对象追踪、异常排查和链路追踪展开。日志表写入频繁，索引不宜过多，优先覆盖高频查询条件。

下面是推荐的基础索引设计。

```sql
ALTER TABLE biz_operation_log
    ADD INDEX idx_tenant_time (tenant_id, operation_time),
    ADD INDEX idx_operator_time (operator_id, operation_time),
    ADD INDEX idx_biz_object_time (biz_type, biz_id, operation_time),
    ADD INDEX idx_biz_no_time (biz_no, operation_time),
    ADD INDEX idx_module_operation_time (module_code, operation_type, operation_time),
    ADD INDEX idx_result_time (operation_result, operation_time),
    ADD INDEX idx_trace_id (trace_id);
```

各索引的用途如下：

| 索引                        | 适用查询                         |
| --------------------------- | -------------------------------- |
| `idx_tenant_time`           | 按租户查询最近操作日志           |
| `idx_operator_time`         | 查询某个用户的操作记录           |
| `idx_biz_object_time`       | 查询某个业务对象的操作轨迹       |
| `idx_biz_no_time`           | 按业务单号查询日志               |
| `idx_module_operation_time` | 按模块和操作类型筛选日志         |
| `idx_result_time`           | 查询失败操作或异常操作           |
| `idx_trace_id`              | 根据链路ID定位一次请求产生的日志 |

如果系统没有多租户，可以保留 `tenant_id` 字段并固定为0，也可以在索引设计中去掉 `tenant_id` 前缀。但对于后续可能扩展多租户的系统，建议一开始保留该字段。

如果操作日志数据量非常大，建议结合分区表模型，按 `operation_time` 做月度或季度分区。此时查询条件中应尽量带上时间范围，避免扫描过多历史数据。

### 常用查询

常用查询应尽量使用明确的时间范围。日志表数据量通常增长很快，后台页面不应默认查询全量历史数据。

#### 查询最近操作日志

该查询适合后台操作日志列表页，按操作时间倒序展示最近日志。

```sql
SELECT
    id,
    operator_id,
    operator_name,
    module_name,
    operation_type,
    operation_name,
    biz_type,
    biz_id,
    biz_no,
    operation_result,
    cost_ms,
    operation_time
FROM biz_operation_log
WHERE tenant_id = 10001
  AND operation_time >= '2026-05-01 00:00:00.000'
  AND operation_time < '2026-06-01 00:00:00.000'
ORDER BY operation_time DESC
LIMIT 20;
```

#### 查询某个用户的操作记录

该查询用于排查用户近期做过哪些操作，适合用户行为追踪和安全排查。

```sql
SELECT
    id,
    operator_id,
    operator_name,
    module_name,
    operation_name,
    biz_type,
    biz_id,
    biz_no,
    operation_result,
    operation_time
FROM biz_operation_log
WHERE operator_id = 20001
  AND operation_time >= '2026-05-01 00:00:00.000'
  AND operation_time < '2026-06-01 00:00:00.000'
ORDER BY operation_time DESC
LIMIT 50;
```

#### 查询某个业务对象的操作轨迹

该查询用于查看某个订单、商品、客户或配置项被谁操作过。

```sql
SELECT
    id,
    operator_id,
    operator_name,
    operation_type,
    operation_name,
    biz_type,
    biz_id,
    biz_no,
    operation_result,
    error_msg,
    operation_time
FROM biz_operation_log
WHERE biz_type = 'ORDER'
  AND biz_id = '900000001'
ORDER BY operation_time ASC;
```

#### 按业务单号查询日志

该查询适合后台客服、运营人员根据业务单号排查问题。

```sql
SELECT
    id,
    operator_name,
    module_name,
    operation_name,
    biz_no,
    operation_result,
    cost_ms,
    operation_time
FROM biz_operation_log
WHERE biz_no = 'ORD202605130001'
ORDER BY operation_time ASC;
```

#### 查询失败操作记录

该查询用于排查业务操作失败、权限不足、参数错误、外部接口异常等问题。

```sql
SELECT
    id,
    trace_id,
    operator_id,
    operator_name,
    module_name,
    operation_name,
    biz_type,
    biz_id,
    error_code,
    error_msg,
    operation_time
FROM biz_operation_log
WHERE operation_result = 2
  AND operation_time >= '2026-05-13 00:00:00.000'
  AND operation_time < '2026-05-14 00:00:00.000'
ORDER BY operation_time DESC
LIMIT 100;
```

#### 根据链路追踪ID查询日志

该查询用于将接口调用日志、业务操作日志、异常日志串联起来，定位一次完整请求链路。

```sql
SELECT
    id,
    trace_id,
    operator_name,
    module_name,
    operation_name,
    biz_type,
    biz_id,
    operation_result,
    error_code,
    error_msg,
    cost_ms,
    operation_time
FROM biz_operation_log
WHERE trace_id = 'trace-202605130001'
ORDER BY operation_time ASC;
```

#### 查询高耗时操作

该查询用于定位慢操作，例如导出、批量修改、复杂审批等业务行为。

```sql
SELECT
    id,
    operator_name,
    module_name,
    operation_name,
    biz_type,
    biz_id,
    cost_ms,
    operation_time
FROM biz_operation_log
WHERE operation_time >= '2026-05-13 00:00:00.000'
  AND operation_time < '2026-05-14 00:00:00.000'
  AND cost_ms >= 3000
ORDER BY cost_ms DESC
LIMIT 100;
```

### 常用写入

操作日志通常由业务系统在操作完成后写入。写入方式可以分为同步写入和异步写入两类。

同步写入适合关键操作，例如删除、授权、审核、导出等场景。它可以保证业务操作完成后日志立即可查，但会增加主链路耗时。

```sql
INSERT INTO biz_operation_log (
    id,
    trace_id,
    tenant_id,
    operator_id,
    operator_name,
    operator_type,
    module_code,
    module_name,
    operation_type,
    operation_name,
    biz_type,
    biz_id,
    biz_no,
    biz_desc,
    request_method,
    request_uri,
    client_ip,
    user_agent,
    request_params,
    response_data,
    extra_data,
    operation_result,
    error_code,
    error_msg,
    cost_ms,
    operation_time,
    created_at
) VALUES (
    1900000000000000001,
    'trace-202605130001',
    10001,
    20001,
    '张三',
    'ADMIN',
    'ORDER',
    '订单管理',
    'UPDATE',
    '修改订单收货地址',
    'ORDER',
    '900000001',
    'ORD202605130001',
    '修改订单收货地址',
    'POST',
    '/admin/order/update-address',
    '192.168.1.10',
    'Mozilla/5.0',
    JSON_OBJECT('orderId', '900000001', 'receiverMobile', '138****8000'),
    JSON_OBJECT('success', true),
    JSON_OBJECT('source', 'admin-web'),
    1,
    '',
    '',
    86,
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
);
```

失败日志也应写入，尤其是权限不足、参数校验失败、业务规则拒绝、外部系统异常等场景。失败日志不应保存完整异常堆栈，完整堆栈应进入应用日志平台。

```sql
INSERT INTO biz_operation_log (
    id,
    trace_id,
    tenant_id,
    operator_id,
    operator_name,
    operator_type,
    module_code,
    module_name,
    operation_type,
    operation_name,
    biz_type,
    biz_id,
    biz_no,
    biz_desc,
    request_method,
    request_uri,
    client_ip,
    user_agent,
    request_params,
    response_data,
    extra_data,
    operation_result,
    error_code,
    error_msg,
    cost_ms,
    operation_time,
    created_at
) VALUES (
    1900000000000000002,
    'trace-202605130002',
    10001,
    20002,
    '李四',
    'ADMIN',
    'PRODUCT',
    '商品管理',
    'DELETE',
    '删除商品',
    'PRODUCT',
    '300000001',
    'SPU202605130001',
    '删除商品失败',
    'POST',
    '/admin/product/delete',
    '192.168.1.11',
    'Mozilla/5.0',
    JSON_OBJECT('productId', '300000001'),
    JSON_OBJECT('success', false),
    JSON_OBJECT('source', 'admin-web'),
    2,
    'PRODUCT_HAS_ACTIVE_ORDER',
    '商品存在未完成订单，禁止删除',
    42,
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
);
```

对于普通操作日志，建议采用异步写入。业务系统可以先将日志事件写入消息队列、Redis Stream、Outbox 事件表或本地异步队列，再由消费者批量落库。这样可以降低主业务链路延迟。

批量写入时可以使用多值 `INSERT`，减少数据库交互次数。

```sql
INSERT INTO biz_operation_log (
    id,
    trace_id,
    tenant_id,
    operator_id,
    operator_name,
    operator_type,
    module_code,
    module_name,
    operation_type,
    operation_name,
    biz_type,
    biz_id,
    biz_no,
    biz_desc,
    request_method,
    request_uri,
    client_ip,
    user_agent,
    request_params,
    response_data,
    extra_data,
    operation_result,
    error_code,
    error_msg,
    cost_ms,
    operation_time,
    created_at
) VALUES
(
    1900000000000000011,
    'trace-202605130011',
    10001,
    20001,
    '张三',
    'ADMIN',
    'ORDER',
    '订单管理',
    'AUDIT',
    '审核订单',
    'ORDER',
    '900000011',
    'ORD202605130011',
    '审核订单通过',
    'POST',
    '/admin/order/audit',
    '192.168.1.10',
    'Mozilla/5.0',
    JSON_OBJECT('orderId', '900000011', 'auditStatus', 'PASS'),
    JSON_OBJECT('success', true),
    JSON_OBJECT('source', 'admin-web'),
    1,
    '',
    '',
    55,
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
),
(
    1900000000000000012,
    'trace-202605130012',
    10001,
    20001,
    '张三',
    'ADMIN',
    'ORDER',
    '订单管理',
    'AUDIT',
    '审核订单',
    'ORDER',
    '900000012',
    'ORD202605130012',
    '审核订单拒绝',
    'POST',
    '/admin/order/audit',
    '192.168.1.10',
    'Mozilla/5.0',
    JSON_OBJECT('orderId', '900000012', 'auditStatus', 'REJECT'),
    JSON_OBJECT('success', true),
    JSON_OBJECT('source', 'admin-web'),
    1,
    '',
    '',
    63,
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
);
```

### 常见问题

操作日志表在实际使用中最常见的问题是字段过少、JSON 滥用、敏感信息泄露和查询不带时间范围。

第一，操作日志不能只记录操作人、操作名称和操作时间。缺少业务对象字段后，后续无法回答“这个订单被谁操作过”这类问题。因此应保留 `biz_type`、`biz_id`、`biz_no` 和 `biz_desc`。

第二，不要把主要查询字段只放在 `request_params` 中。例如订单ID、商品ID、客户ID、业务单号等字段，应提升为独立列。JSON 字段适合保存补充上下文，不适合作为核心检索入口。

第三，操作日志必须做敏感信息脱敏。手机号、身份证号、银行卡号、邮箱、详细地址、Token、密码、密钥等内容不应明文保存。请求参数写入前应由应用层统一脱敏。

第四，失败日志不能只记录“操作失败”。应至少记录 `error_code` 和简短的 `error_msg`，否则后续很难区分是权限问题、参数问题、业务规则问题还是外部系统问题。

第五，不建议长期在主业务库中保存全量操作日志。日志数据增长速度较快，应根据业务要求设置保留周期，例如在线保留 3 到 6 个月，历史数据归档到冷库、对象存储或日志分析平台。

第六，后台查询必须限制时间范围。默认查询最近 7 天、30 天或当前月份，不应允许无时间条件直接扫描全表。

第七，操作日志不适合频繁更新。日志表原则上只追加、不修改、不删除。确需纠正数据时，应通过补偿日志或管理动作记录，而不是直接覆盖原日志。

### 总结

操作日志模型的核心目标是记录用户业务行为，重点解决“谁在什么时候对什么对象做了什么操作，结果如何”的问题。

建模时应将操作主体、业务对象、操作行为、请求上下文和操作结果拆成清晰字段。常用查询字段必须独立建列，扩展信息再放入 JSON。索引设计应围绕操作人、业务对象、业务单号、模块操作、操作结果和链路追踪展开。

对于写入频繁的大型系统，操作日志应优先采用异步写入和批量落库，并结合分区、归档和冷热数据策略控制数据规模。操作日志只负责业务行为留痕，字段级变更审计、完整数据快照和版本追踪应交由审计日志模型、历史版本模型或数据变更记录模型处理。



## 审计日志模型

审计日志模型用于记录关键业务数据的变更过程，重点关注“数据在什么时候、被谁、通过什么方式、从什么值变成了什么值”。它比操作日志更细，通常用于字段级变更追踪、合规审计、责任定位、数据回溯和异常变更排查。

操作日志偏向记录业务动作，例如“张三修改了订单地址”。审计日志偏向记录数据变化，例如“订单收货手机号从 `138****8000` 修改为 `139****9000`”。两者可以通过 `trace_id`、`biz_type`、`biz_id` 关联起来。

### 适用场景

审计日志适用于需要保留关键数据变更痕迹的业务系统。它通常用于金融、订单、合同、权限、配置、客户资料、审批、库存、账户、支付等对数据完整性和责任追踪要求较高的场景。

常见适用场景包括：

| 场景           | 说明                                           |
| -------------- | ---------------------------------------------- |
| 字段级变更追踪 | 记录字段修改前后的值                           |
| 关键数据审计   | 追踪金额、状态、权限、配置、客户资料等敏感变更 |
| 数据回溯       | 排查某条业务数据在某个时间点为什么变成当前状态 |
| 合规留痕       | 满足内部审计、安全审计、监管检查等要求         |
| 异常变更排查   | 定位异常修改、误操作、越权操作、批量变更       |
| 责任归属       | 明确变更来源、操作人、操作入口和请求链路       |
| 数据修复依据   | 为人工修复、补偿任务、异常回滚提供参考         |

审计日志不适合记录普通页面访问、查询行为、接口耗时等信息。这些内容应分别放到操作日志、登录日志或接口调用日志中。

### 建模结构

审计日志建议采用“主表 + 明细表”的结构。主表记录一次审计事件，明细表记录本次事件中具体发生变化的字段。

主表适合保存操作人、业务对象、审计动作、来源系统、请求链路、审计时间等信息。明细表适合保存字段名称、字段标题、旧值、新值、值类型和脱敏标记等信息。

下面是审计日志主表结构。该表只定义字段和主键，普通索引统一在“索引设计”章节中给出。

```sql
CREATE TABLE biz_audit_log (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '链路追踪ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，非多租户系统可固定为0',

    auditor_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '审计操作人ID',
    auditor_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '审计操作人名称',
    auditor_type VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '操作人类型：USER-普通用户，ADMIN-管理员，SYSTEM-系统任务，OPEN_API-开放接口',

    source_system VARCHAR(64) NOT NULL DEFAULT '' COMMENT '来源系统',
    source_module VARCHAR(64) NOT NULL DEFAULT '' COMMENT '来源模块',
    source_channel VARCHAR(64) NOT NULL DEFAULT '' COMMENT '来源渠道，例如ADMIN_WEB、APP、OPEN_API、JOB',

    audit_action VARCHAR(64) NOT NULL DEFAULT '' COMMENT '审计动作，例如CREATE、UPDATE、DELETE、ENABLE、DISABLE、APPROVE',
    audit_action_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '审计动作名称',
    audit_level TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '审计级别：1-普通，2-重要，3-敏感，4-高危',

    biz_type VARCHAR(64) NOT NULL DEFAULT '' COMMENT '业务类型，例如ORDER、CUSTOMER、CONTRACT、ROLE',
    biz_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '业务对象ID',
    biz_no VARCHAR(64) NOT NULL DEFAULT '' COMMENT '业务单号',
    biz_name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '业务对象名称或摘要',

    table_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '被审计业务表名',
    data_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '被审计数据主键ID',
    data_version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '变更后的数据版本号',

    before_snapshot JSON NULL COMMENT '变更前数据快照，建议只保存关键字段',
    after_snapshot JSON NULL COMMENT '变更后数据快照，建议只保存关键字段',
    change_summary VARCHAR(512) NOT NULL DEFAULT '' COMMENT '变更摘要',

    request_uri VARCHAR(255) NOT NULL DEFAULT '' COMMENT '请求URI',
    request_method VARCHAR(16) NOT NULL DEFAULT '' COMMENT '请求方法',
    client_ip VARCHAR(64) NOT NULL DEFAULT '' COMMENT '客户端IP',
    user_agent VARCHAR(512) NOT NULL DEFAULT '' COMMENT '客户端标识',

    audit_result TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '审计结果：1-成功，2-失败',
    error_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '错误码',
    error_msg VARCHAR(512) NOT NULL DEFAULT '' COMMENT '错误信息',

    audit_time DATETIME(3) NOT NULL COMMENT '审计发生时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '审计日志主表';
```

下面是审计日志明细表结构。明细表按字段粒度记录变化内容，一次主表审计事件可以对应多条字段变更明细。

```sql
CREATE TABLE biz_audit_log_detail (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    audit_log_id BIGINT UNSIGNED NOT NULL COMMENT '审计日志主表ID',

    field_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '字段名',
    field_title VARCHAR(128) NOT NULL DEFAULT '' COMMENT '字段标题',
    field_type VARCHAR(64) NOT NULL DEFAULT '' COMMENT '字段类型，例如STRING、NUMBER、DATE、DATETIME、BOOLEAN、JSON',
    old_value TEXT NULL COMMENT '变更前字段值',
    new_value TEXT NULL COMMENT '变更后字段值',
    old_display_value VARCHAR(512) NOT NULL DEFAULT '' COMMENT '变更前展示值',
    new_display_value VARCHAR(512) NOT NULL DEFAULT '' COMMENT '变更后展示值',

    value_changed TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否发生变化：1-是，2-否',
    sensitive_flag TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '是否敏感字段：1-是，2-否',
    remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',

    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '审计日志明细表';
```

这种结构可以同时满足两类查询：主表用于快速定位某个业务对象在某段时间内发生了哪些审计事件，明细表用于查看某次事件具体修改了哪些字段。

`before_snapshot` 和 `after_snapshot` 不建议保存整行完整数据，除非业务确实需要。更常见的做法是只保存关键字段快照，把字段级差异放到明细表中，避免日志数据过大。

### 字段设计

审计日志字段设计应围绕数据变更的可追溯性展开。主表负责描述一次变更事件，明细表负责描述具体字段差异。

审计日志主表核心字段如下：

| 字段                | 类型               | 设计说明                                             |
| ------------------- | ------------------ | ---------------------------------------------------- |
| `id`                | `BIGINT UNSIGNED`  | 主键ID，建议使用分布式ID                             |
| `trace_id`          | `VARCHAR(64)`      | 链路追踪ID，用于关联操作日志、接口日志和异常日志     |
| `tenant_id`         | `BIGINT UNSIGNED`  | 租户ID，多租户系统必须保留                           |
| `auditor_id`        | `BIGINT UNSIGNED`  | 触发数据变更的用户ID或系统操作ID                     |
| `auditor_name`      | `VARCHAR(64)`      | 操作人名称，建议冗余保存                             |
| `auditor_type`      | `VARCHAR(32)`      | 操作人类型，用于区分用户、管理员、系统任务和开放接口 |
| `source_system`     | `VARCHAR(64)`      | 来源系统，适合多系统集成场景                         |
| `source_module`     | `VARCHAR(64)`      | 来源模块，便于按业务模块筛选                         |
| `source_channel`    | `VARCHAR(64)`      | 来源渠道，例如后台、App、开放接口、定时任务          |
| `audit_action`      | `VARCHAR(64)`      | 审计动作编码，例如 `CREATE`、`UPDATE`、`DELETE`      |
| `audit_action_name` | `VARCHAR(128)`     | 审计动作名称，用于后台展示                           |
| `audit_level`       | `TINYINT UNSIGNED` | 审计级别，用于区分普通、重要、敏感和高危变更         |
| `biz_type`          | `VARCHAR(64)`      | 业务类型，例如订单、客户、合同、角色                 |
| `biz_id`            | `VARCHAR(64)`      | 业务对象ID                                           |
| `biz_no`            | `VARCHAR(64)`      | 业务单号，便于运营、客服按单号查询                   |
| `biz_name`          | `VARCHAR(255)`     | 业务对象名称或摘要                                   |
| `table_name`        | `VARCHAR(128)`     | 被审计的业务表名                                     |
| `data_id`           | `VARCHAR(64)`      | 被审计数据主键ID                                     |
| `data_version`      | `BIGINT UNSIGNED`  | 变更后的数据版本号，适合与乐观锁模型配合             |
| `before_snapshot`   | `JSON`             | 变更前关键字段快照                                   |
| `after_snapshot`    | `JSON`             | 变更后关键字段快照                                   |
| `change_summary`    | `VARCHAR(512)`     | 变更摘要，例如“修改客户等级、手机号、地址”           |
| `audit_result`      | `TINYINT UNSIGNED` | 审计结果，通常记录成功变更，失败场景可按需记录       |
| `audit_time`        | `DATETIME(3)`      | 审计发生时间                                         |
| `created_at`        | `DATETIME(3)`      | 日志落库时间                                         |

审计日志明细表核心字段如下：

| 字段                | 类型               | 设计说明                               |
| ------------------- | ------------------ | -------------------------------------- |
| `id`                | `BIGINT UNSIGNED`  | 主键ID                                 |
| `audit_log_id`      | `BIGINT UNSIGNED`  | 审计日志主表ID                         |
| `field_name`        | `VARCHAR(128)`     | 数据库字段名或业务字段名               |
| `field_title`       | `VARCHAR(128)`     | 字段中文名称，便于页面展示             |
| `field_type`        | `VARCHAR(64)`      | 字段类型，用于前端展示和差异解析       |
| `old_value`         | `TEXT`             | 原始旧值，可保存脱敏后的原始值         |
| `new_value`         | `TEXT`             | 原始新值，可保存脱敏后的原始值         |
| `old_display_value` | `VARCHAR(512)`     | 旧展示值，例如状态码转换后的中文       |
| `new_display_value` | `VARCHAR(512)`     | 新展示值，例如状态码转换后的中文       |
| `value_changed`     | `TINYINT UNSIGNED` | 是否发生变化，默认只保存发生变化的字段 |
| `sensitive_flag`    | `TINYINT UNSIGNED` | 是否敏感字段                           |
| `remark`            | `VARCHAR(255)`     | 字段变更备注                           |
| `created_at`        | `DATETIME(3)`      | 创建时间                               |

字段设计时需要特别注意敏感数据处理。审计日志并不意味着可以保存所有原始值。密码、Token、密钥、银行卡号、身份证号、手机号、邮箱、详细地址等字段应按安全要求进行脱敏、加密或不落库。

对于枚举字段，建议同时保存原始值和展示值。例如订单状态从 `WAIT_PAY` 修改为 `PAID`，展示值可以保存为“待支付”到“已支付”。这样既方便机器处理，也方便审计人员查看。

### 索引设计

审计日志的索引应围绕业务对象追踪、字段变更查询、操作人追踪、审计级别筛选、时间范围查询和链路追踪展开。由于审计日志写入频繁且数据量较大，索引数量应控制在必要范围内。

下面是推荐的基础索引设计。

```sql
ALTER TABLE biz_audit_log
    ADD INDEX idx_tenant_time (tenant_id, audit_time),
    ADD INDEX idx_biz_object_time (biz_type, biz_id, audit_time),
    ADD INDEX idx_biz_no_time (biz_no, audit_time),
    ADD INDEX idx_table_data_time (table_name, data_id, audit_time),
    ADD INDEX idx_auditor_time (auditor_id, audit_time),
    ADD INDEX idx_action_time (audit_action, audit_time),
    ADD INDEX idx_level_time (audit_level, audit_time),
    ADD INDEX idx_trace_id (trace_id);

ALTER TABLE biz_audit_log_detail
    ADD INDEX idx_audit_log_id (audit_log_id),
    ADD INDEX idx_field_name (field_name);
```

各索引的用途如下：

| 索引                  | 适用查询                           |
| --------------------- | ---------------------------------- |
| `idx_tenant_time`     | 按租户查询审计日志列表             |
| `idx_biz_object_time` | 查询某个业务对象的完整变更历史     |
| `idx_biz_no_time`     | 根据业务单号查询审计记录           |
| `idx_table_data_time` | 根据业务表和数据ID定位变更历史     |
| `idx_auditor_time`    | 查询某个操作人的数据变更行为       |
| `idx_action_time`     | 按新增、修改、删除、审批等动作筛选 |
| `idx_level_time`      | 查询敏感或高危审计事件             |
| `idx_trace_id`        | 根据链路ID串联一次请求中的审计记录 |
| `idx_audit_log_id`    | 查询某次审计事件的字段明细         |
| `idx_field_name`      | 查询某个字段的变更记录             |

如果需要频繁查询“某个业务对象的某个字段变更历史”，可以在明细表中冗余 `biz_type`、`biz_id`、`audit_time`，并建立组合索引。但这种设计会增加明细表冗余和写入成本，应根据实际查询压力决定。

对于大数据量审计日志，建议主表按 `audit_time` 做分区，明细表可以按 `created_at` 分区，或者与主表使用相同归档策略。后台查询必须带时间范围，避免跨多年扫描。

### 常用查询

审计日志查询通常分为审计事件查询和字段变更明细查询。查询时应优先通过主表定位审计事件，再按 `audit_log_id` 查询明细表。

#### 查询某个业务对象的审计历史

该查询用于查看某个订单、客户、合同、角色等业务对象的完整变更轨迹。

```sql
SELECT
    id,
    trace_id,
    auditor_id,
    auditor_name,
    auditor_type,
    source_module,
    source_channel,
    audit_action,
    audit_action_name,
    audit_level,
    biz_type,
    biz_id,
    biz_no,
    biz_name,
    change_summary,
    audit_time
FROM biz_audit_log
WHERE biz_type = 'ORDER'
  AND biz_id = '900000001'
ORDER BY audit_time ASC;
```

#### 查询某次审计事件的字段变更明细

该查询用于查看一次数据变更中具体修改了哪些字段，以及字段修改前后的值。

```sql
SELECT
    field_name,
    field_title,
    field_type,
    old_value,
    new_value,
    old_display_value,
    new_display_value,
    sensitive_flag,
    remark,
    created_at
FROM biz_audit_log_detail
WHERE audit_log_id = 1900000000000001001
  AND value_changed = 1
ORDER BY id ASC;
```

#### 查询某个字段的变更记录

该查询用于排查某个关键字段的历史变更，例如订单金额、客户等级、账户状态、角色权限等。

```sql
SELECT
    l.id,
    l.auditor_name,
    l.biz_type,
    l.biz_id,
    l.biz_no,
    l.audit_action_name,
    d.field_name,
    d.field_title,
    d.old_display_value,
    d.new_display_value,
    l.audit_time
FROM biz_audit_log l
JOIN biz_audit_log_detail d ON d.audit_log_id = l.id
WHERE l.biz_type = 'CUSTOMER'
  AND l.biz_id = 'CUST10001'
  AND d.field_name = 'customer_level'
ORDER BY l.audit_time ASC;
```

#### 查询某个操作人的审计记录

该查询用于安全排查和责任定位，查看某个用户在指定时间范围内修改过哪些数据。

```sql
SELECT
    id,
    auditor_id,
    auditor_name,
    source_module,
    source_channel,
    audit_action,
    audit_action_name,
    audit_level,
    biz_type,
    biz_id,
    biz_no,
    change_summary,
    audit_time
FROM biz_audit_log
WHERE auditor_id = 20001
  AND audit_time >= '2026-05-01 00:00:00.000'
  AND audit_time < '2026-06-01 00:00:00.000'
ORDER BY audit_time DESC
LIMIT 100;
```

#### 查询敏感或高危审计事件

该查询用于安全审计，重点查看敏感字段修改、权限调整、状态强制变更、金额修改等高风险行为。

```sql
SELECT
    id,
    trace_id,
    auditor_id,
    auditor_name,
    source_module,
    source_channel,
    audit_action_name,
    audit_level,
    biz_type,
    biz_id,
    biz_no,
    change_summary,
    audit_time
FROM biz_audit_log
WHERE audit_level >= 3
  AND audit_time >= '2026-05-13 00:00:00.000'
  AND audit_time < '2026-05-14 00:00:00.000'
ORDER BY audit_time DESC
LIMIT 100;
```

#### 根据业务表和数据ID查询变更历史

该查询适合在通用审计后台中使用，不依赖具体业务类型，直接通过表名和数据主键查询变更历史。

```sql
SELECT
    id,
    auditor_name,
    audit_action,
    audit_action_name,
    table_name,
    data_id,
    data_version,
    change_summary,
    audit_time
FROM biz_audit_log
WHERE table_name = 'biz_order'
  AND data_id = '900000001'
ORDER BY audit_time ASC;
```

#### 根据链路追踪ID查询审计日志

该查询用于将一次请求中的接口调用日志、操作日志、审计日志串联起来。

```sql
SELECT
    id,
    trace_id,
    auditor_name,
    source_module,
    audit_action_name,
    biz_type,
    biz_id,
    biz_no,
    change_summary,
    audit_time
FROM biz_audit_log
WHERE trace_id = 'trace-202605130001'
ORDER BY audit_time ASC;
```

### 常用写入

审计日志通常在业务数据写入成功后生成。对于强一致要求较高的系统，可以在同一个事务中写入业务数据和审计日志。对于写入压力较大的系统，可以通过 Outbox 事件表或消息队列异步生成审计日志，但必须保证事件不丢失。

下面示例表示一次订单金额变更产生的审计主表记录。

```sql
INSERT INTO biz_audit_log (
    id,
    trace_id,
    tenant_id,
    auditor_id,
    auditor_name,
    auditor_type,
    source_system,
    source_module,
    source_channel,
    audit_action,
    audit_action_name,
    audit_level,
    biz_type,
    biz_id,
    biz_no,
    biz_name,
    table_name,
    data_id,
    data_version,
    before_snapshot,
    after_snapshot,
    change_summary,
    request_uri,
    request_method,
    client_ip,
    user_agent,
    audit_result,
    error_code,
    error_msg,
    audit_time,
    created_at
) VALUES (
    1900000000000001001,
    'trace-202605130101',
    10001,
    20001,
    '张三',
    'ADMIN',
    'order-center',
    '订单管理',
    'ADMIN_WEB',
    'UPDATE',
    '修改订单金额',
    3,
    'ORDER',
    '900000001',
    'ORD202605130001',
    '订单金额调整',
    'biz_order',
    '900000001',
    8,
    JSON_OBJECT('orderAmount', '100.00', 'payAmount', '100.00', 'orderStatus', 'WAIT_PAY'),
    JSON_OBJECT('orderAmount', '90.00', 'payAmount', '90.00', 'orderStatus', 'WAIT_PAY'),
    '修改订单金额、应付金额',
    '/admin/order/update-amount',
    'POST',
    '192.168.1.10',
    'Mozilla/5.0',
    1,
    '',
    '',
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
);
```

下面示例写入本次审计事件的字段级变更明细。

```sql
INSERT INTO biz_audit_log_detail (
    id,
    audit_log_id,
    field_name,
    field_title,
    field_type,
    old_value,
    new_value,
    old_display_value,
    new_display_value,
    value_changed,
    sensitive_flag,
    remark,
    created_at
) VALUES
(
    1900000000000001101,
    1900000000000001001,
    'order_amount',
    '订单金额',
    'NUMBER',
    '100.00',
    '90.00',
    '100.00',
    '90.00',
    1,
    2,
    '运营后台手动调整订单金额',
    CURRENT_TIMESTAMP(3)
),
(
    1900000000000001102,
    1900000000000001001,
    'pay_amount',
    '应付金额',
    'NUMBER',
    '100.00',
    '90.00',
    '100.00',
    '90.00',
    1,
    2,
    '订单金额调整后同步修改应付金额',
    CURRENT_TIMESTAMP(3)
);
```

对于新增数据，可以把旧值记为空，新值记录新增后的关键字段。

```sql
INSERT INTO biz_audit_log_detail (
    id,
    audit_log_id,
    field_name,
    field_title,
    field_type,
    old_value,
    new_value,
    old_display_value,
    new_display_value,
    value_changed,
    sensitive_flag,
    remark,
    created_at
) VALUES
(
    1900000000000001201,
    1900000000000001002,
    'customer_name',
    '客户名称',
    'STRING',
    NULL,
    '杭州某某科技有限公司',
    '',
    '杭州某某科技有限公司',
    1,
    2,
    '新增客户资料',
    CURRENT_TIMESTAMP(3)
);
```

对于删除数据，可以把旧值记录为删除前的值，新值记为空。删除操作建议在主表中保存删除前关键字段快照，避免后续业务表数据被物理删除后无法还原审计上下文。

```sql
INSERT INTO biz_audit_log (
    id,
    trace_id,
    tenant_id,
    auditor_id,
    auditor_name,
    auditor_type,
    source_system,
    source_module,
    source_channel,
    audit_action,
    audit_action_name,
    audit_level,
    biz_type,
    biz_id,
    biz_no,
    biz_name,
    table_name,
    data_id,
    data_version,
    before_snapshot,
    after_snapshot,
    change_summary,
    request_uri,
    request_method,
    client_ip,
    user_agent,
    audit_result,
    error_code,
    error_msg,
    audit_time,
    created_at
) VALUES (
    1900000000000001003,
    'trace-202605130103',
    10001,
    20002,
    '李四',
    'ADMIN',
    'system-center',
    '角色管理',
    'ADMIN_WEB',
    'DELETE',
    '删除角色',
    4,
    'ROLE',
    '30001',
    'ROLE_ADMIN_TEST',
    '测试管理员角色',
    'sys_role',
    '30001',
    3,
    JSON_OBJECT('roleCode', 'ROLE_ADMIN_TEST', 'roleName', '测试管理员角色', 'status', 'ENABLE'),
    NULL,
    '删除测试管理员角色',
    '/admin/role/delete',
    'POST',
    '192.168.1.11',
    'Mozilla/5.0',
    1,
    '',
    '',
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
);
```

### 常见问题

审计日志最常见的问题是和操作日志边界不清。操作日志记录业务动作，审计日志记录数据变化。一个业务操作可以产生一条操作日志，同时产生一条或多条审计日志。

第二个问题是保存过多快照数据。审计日志不是业务表备份，不应无差别保存整行数据。建议只保存关键字段快照，并把真正发生变化的字段写入明细表。

第三个问题是敏感数据明文落库。审计日志经常保存修改前后的值，如果不做脱敏，反而会形成新的敏感数据泄露点。敏感字段应根据安全要求选择脱敏、加密、摘要化或不记录原值。

第四个问题是审计日志缺少来源信息。只记录字段变化，但不记录操作人、来源系统、来源渠道、请求地址、链路ID，会导致后续无法判断变更是后台人工操作、开放接口调用、定时任务还是系统自动补偿。

第五个问题是没有记录展示值。枚举、字典、状态类字段如果只保存编码，审计页面可读性较差。建议同时保存原始值和展示值，例如 `WAIT_PAY` 与“待支付”。

第六个问题是审计日志被修改或删除。审计日志应遵循追加写入原则，不建议提供普通修改和删除能力。确需纠正审计内容时，应追加一条更正记录，而不是覆盖原始记录。

第七个问题是查询不带时间范围。审计日志数据增长通常比操作日志更快，因为一次操作可能产生多条字段变更明细。后台查询应默认限制最近 7 天、30 天或当前月份。

第八个问题是明细表过度索引。明细表数据量可能远大于主表，索引越多写入成本越高。除 `audit_log_id` 外，其他索引应根据真实查询场景谨慎添加。

### 总结

审计日志模型的核心目标是记录关键业务数据的变化过程，重点解决“数据从什么值变成了什么值，由谁在什么时候通过什么入口触发”的问题。

推荐采用审计主表加审计明细表的结构。主表记录一次审计事件，明细表记录字段级变更差异。主表适合按业务对象、操作人、审计动作、审计级别和时间范围查询，明细表适合展示字段修改前后的详细内容。

建模时应明确操作日志和审计日志的边界。操作日志关注行为，审计日志关注数据变化。对于敏感字段，需要在写入前完成脱敏、加密或过滤。对于大数据量场景，应结合分区、归档、冷热数据和异步写入策略，避免审计日志影响核心业务链路。



## 登录日志模型

登录日志模型用于记录用户、管理员、开放平台账号或系统账号的认证行为，重点关注“谁在什么时间、从什么来源、使用什么方式登录，登录结果如何”。它主要服务于账号安全、异常登录排查、登录历史展示、风控识别和合规审计。

登录日志与操作日志的区别在于，登录日志记录的是认证入口行为，例如登录成功、登录失败、退出登录、刷新令牌、密码错误、账号锁定等；操作日志记录的是登录后的业务操作行为。

### 适用场景

登录日志适用于所有存在账号体系、认证体系或访问控制体系的系统。只要系统需要判断用户是否安全登录、是否存在异常访问、是否有暴力破解风险，就应记录登录日志。

常见适用场景包括：

| 场景         | 说明                                                       |
| ------------ | ---------------------------------------------------------- |
| 登录历史展示 | 用户或管理员查看最近登录时间、登录地点、登录设备           |
| 登录失败排查 | 查询账号密码错误、验证码错误、账号禁用、账号锁定等失败原因 |
| 异常登录识别 | 根据 IP、设备、地区、时间段识别异常登录                    |
| 安全审计     | 追踪敏感账号、管理员账号、开放平台账号的登录行为           |
| 风控限制     | 统计短时间内失败次数，用于账号锁定、验证码升级、IP 限制    |
| 多端登录管理 | 记录 Web、App、小程序、开放接口等不同端的登录行为          |
| 合规留痕     | 满足系统安全、账号安全、访问审计等基础要求                 |

登录日志不建议记录完整密码、Token、Cookie、Session 内容。认证相关敏感数据只应记录摘要信息、脱敏信息或安全状态，不应明文落库。

### 建模结构

登录日志通常使用单表建模，一次登录、退出、认证失败或令牌刷新行为对应一条日志。核心字段围绕登录主体、登录方式、客户端信息、认证结果和安全上下文展开。

下面是登录日志表的基础结构。该结构只定义字段和主键，普通索引统一放在“索引设计”章节中说明。

```sql
CREATE TABLE sys_login_log (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '链路追踪ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，非多租户系统可固定为0',

    account_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '账号ID，登录失败且未识别账号时可为0',
    account_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '登录账号',
    user_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID，未绑定用户或登录失败时可为0',
    user_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户名称',
    user_type VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '用户类型：USER-普通用户，ADMIN-管理员，MERCHANT-商户，SYSTEM-系统账号',

    login_type VARCHAR(32) NOT NULL DEFAULT 'PASSWORD' COMMENT '登录类型：PASSWORD-密码，SMS-短信，EMAIL-邮箱，OAUTH-三方登录，TOKEN-令牌',
    login_action VARCHAR(32) NOT NULL DEFAULT 'LOGIN' COMMENT '登录动作：LOGIN-登录，LOGOUT-退出，REFRESH_TOKEN-刷新令牌',
    login_channel VARCHAR(64) NOT NULL DEFAULT '' COMMENT '登录渠道，例如ADMIN_WEB、APP、H5、MINI_PROGRAM、OPEN_API',
    auth_provider VARCHAR(64) NOT NULL DEFAULT '' COMMENT '认证提供方，例如LOCAL、WECHAT、GITHUB、LDAP、OIDC',

    login_result TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '登录结果：1-成功，2-失败',
    fail_reason VARCHAR(64) NOT NULL DEFAULT '' COMMENT '失败原因编码',
    fail_msg VARCHAR(255) NOT NULL DEFAULT '' COMMENT '失败原因说明',

    client_ip VARCHAR(64) NOT NULL DEFAULT '' COMMENT '客户端IP',
    client_port INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '客户端端口',
    login_region VARCHAR(128) NOT NULL DEFAULT '' COMMENT '登录地区',
    user_agent VARCHAR(512) NOT NULL DEFAULT '' COMMENT '浏览器或客户端标识',
    device_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '设备ID',
    device_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '设备名称',
    os_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作系统',
    browser_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '浏览器名称',
    app_version VARCHAR(64) NOT NULL DEFAULT '' COMMENT '客户端版本',

    session_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '会话ID，建议保存摘要值',
    token_id VARCHAR(128) NOT NULL DEFAULT '' COMMENT '令牌ID，建议保存摘要值',
    risk_level TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '风险等级：1-正常，2-低风险，3-中风险，4-高风险',
    risk_tags JSON NULL COMMENT '风险标签，例如异地登录、陌生设备、失败过多',

    request_uri VARCHAR(255) NOT NULL DEFAULT '' COMMENT '认证请求URI',
    extra_data JSON NULL COMMENT '扩展数据',
    login_time DATETIME(3) NOT NULL COMMENT '登录发生时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '登录日志表';
```

该模型默认把账号信息和用户信息分开保存。`account_id`、`account_name` 表示认证账号，`user_id`、`user_name` 表示账号绑定的业务用户。在部分系统中，一个用户可能有多个登录账号，例如手机号、邮箱、员工号、第三方账号，因此建议保留这两组字段。

`session_id` 和 `token_id` 不建议保存原始值。实际建模时可以保存哈希摘要、短标识或令牌编号，避免日志表泄露后导致会话被盗用。

### 字段设计

登录日志字段设计需要保证能够回答几个问题：哪个账号登录、通过什么方式登录、来自哪个客户端、登录是否成功、失败原因是什么、是否存在安全风险。

| 字段            | 类型               | 设计说明                                           |
| --------------- | ------------------ | -------------------------------------------------- |
| `id`            | `BIGINT UNSIGNED`  | 主键ID，建议使用分布式ID                           |
| `trace_id`      | `VARCHAR(64)`      | 链路追踪ID，用于关联接口调用日志和异常日志         |
| `tenant_id`     | `BIGINT UNSIGNED`  | 租户ID，多租户系统必须保留                         |
| `account_id`    | `BIGINT UNSIGNED`  | 登录账号ID，失败且未识别账号时可填0                |
| `account_name`  | `VARCHAR(128)`     | 登录账号，例如手机号、邮箱、用户名、员工号         |
| `user_id`       | `BIGINT UNSIGNED`  | 业务用户ID，登录成功后应写入                       |
| `user_name`     | `VARCHAR(64)`      | 用户名称，建议冗余保存                             |
| `user_type`     | `VARCHAR(32)`      | 用户类型，用于区分普通用户、管理员、商户、系统账号 |
| `login_type`    | `VARCHAR(32)`      | 登录方式，例如密码、短信、邮箱、OAuth、令牌        |
| `login_action`  | `VARCHAR(32)`      | 登录动作，例如登录、退出、刷新令牌                 |
| `login_channel` | `VARCHAR(64)`      | 登录渠道，例如后台、App、H5、小程序、开放接口      |
| `auth_provider` | `VARCHAR(64)`      | 认证提供方，例如本地认证、LDAP、OIDC、微信、GitHub |
| `login_result`  | `TINYINT UNSIGNED` | 登录结果，建议使用小整数枚举                       |
| `fail_reason`   | `VARCHAR(64)`      | 登录失败原因编码，便于统计和风控                   |
| `fail_msg`      | `VARCHAR(255)`     | 登录失败说明，避免保存过长异常堆栈                 |
| `client_ip`     | `VARCHAR(64)`      | 客户端 IP，兼容 IPv4 和 IPv6                       |
| `client_port`   | `INT UNSIGNED`     | 客户端端口，可按需记录                             |
| `login_region`  | `VARCHAR(128)`     | IP 解析后的地区信息                                |
| `user_agent`    | `VARCHAR(512)`     | 客户端原始 UA，字段长度不宜过短                    |
| `device_id`     | `VARCHAR(128)`     | 设备ID，用于识别陌生设备                           |
| `device_name`   | `VARCHAR(128)`     | 设备名称，便于页面展示                             |
| `os_name`       | `VARCHAR(64)`      | 操作系统名称                                       |
| `browser_name`  | `VARCHAR(64)`      | 浏览器名称                                         |
| `app_version`   | `VARCHAR(64)`      | App 或客户端版本                                   |
| `session_id`    | `VARCHAR(128)`     | 会话标识，建议保存摘要或内部编号                   |
| `token_id`      | `VARCHAR(128)`     | 令牌标识，建议保存摘要或内部编号                   |
| `risk_level`    | `TINYINT UNSIGNED` | 风险等级，用于安全审计和风控处理                   |
| `risk_tags`     | `JSON`             | 风险标签，例如陌生设备、异地登录、失败过多         |
| `request_uri`   | `VARCHAR(255)`     | 登录请求地址                                       |
| `extra_data`    | `JSON`             | 扩展信息，例如验证码类型、登录入口、客户端包名     |
| `login_time`    | `DATETIME(3)`      | 登录行为发生时间                                   |
| `created_at`    | `DATETIME(3)`      | 日志落库时间                                       |

字段设计时应特别注意失败场景。登录失败时可能无法识别 `account_id` 和 `user_id`，但应尽量记录 `account_name`、`client_ip`、`login_type`、`login_channel` 和 `fail_reason`，否则无法进行失败次数统计和安全排查。

对于管理员账号、财务账号、运营账号等高敏感账号，可以提高 `risk_level` 的默认判断级别，或在 `risk_tags` 中记录更多安全上下文。

### 索引设计

登录日志索引应围绕账号登录历史、用户登录历史、失败次数统计、IP 风控、设备风控、风险等级筛选和时间范围查询设计。登录日志写入频繁，索引数量需要控制，避免影响认证链路性能。

下面是推荐的基础索引设计。

```sql
ALTER TABLE sys_login_log
    ADD INDEX idx_tenant_time (tenant_id, login_time),
    ADD INDEX idx_account_time (account_name, login_time),
    ADD INDEX idx_user_time (user_id, login_time),
    ADD INDEX idx_ip_time (client_ip, login_time),
    ADD INDEX idx_device_time (device_id, login_time),
    ADD INDEX idx_result_time (login_result, login_time),
    ADD INDEX idx_fail_reason_time (fail_reason, login_time),
    ADD INDEX idx_risk_time (risk_level, login_time),
    ADD INDEX idx_trace_id (trace_id);
```

各索引的用途如下：

| 索引                   | 适用查询                         |
| ---------------------- | -------------------------------- |
| `idx_tenant_time`      | 按租户查询登录日志列表           |
| `idx_account_time`     | 按登录账号查询登录历史或失败记录 |
| `idx_user_time`        | 按用户ID查询登录历史             |
| `idx_ip_time`          | 按客户端 IP 查询登录行为         |
| `idx_device_time`      | 按设备ID查询登录行为             |
| `idx_result_time`      | 查询成功或失败登录记录           |
| `idx_fail_reason_time` | 按失败原因统计或筛选             |
| `idx_risk_time`        | 查询中高风险登录行为             |
| `idx_trace_id`         | 根据链路ID定位一次认证请求       |

如果系统需要按 `account_id` 查询登录记录，可以额外增加 `(account_id, login_time)` 索引。但很多失败登录无法识别账号ID，因此 `account_name` 仍然需要保留并建立索引。

如果登录日志数据量较大，可以按 `login_time` 进行月度分区。风控查询通常只关注最近几分钟、几小时或几天的数据，因此查询条件必须带明确时间范围。

### 常用查询

登录日志查询通常围绕账号、用户、IP、设备、失败原因和风险等级展开。查询时应始终携带时间范围，避免扫描长期历史数据。

#### 查询最近登录日志

该查询适合后台登录日志列表页，按登录时间倒序展示最近登录行为。

```sql
SELECT
    id,
    account_name,
    user_id,
    user_name,
    user_type,
    login_type,
    login_action,
    login_channel,
    login_result,
    fail_reason,
    client_ip,
    login_region,
    device_name,
    risk_level,
    login_time
FROM sys_login_log
WHERE tenant_id = 10001
  AND login_time >= '2026-05-01 00:00:00.000'
  AND login_time < '2026-06-01 00:00:00.000'
ORDER BY login_time DESC
LIMIT 50;
```

#### 查询某个账号的登录历史

该查询用于查看某个账号近期登录情况，包括登录成功和失败记录。

```sql
SELECT
    id,
    account_name,
    user_name,
    login_type,
    login_channel,
    auth_provider,
    login_result,
    fail_reason,
    fail_msg,
    client_ip,
    login_region,
    device_name,
    os_name,
    browser_name,
    risk_level,
    login_time
FROM sys_login_log
WHERE account_name = 'admin@example.com'
  AND login_time >= '2026-05-01 00:00:00.000'
  AND login_time < '2026-06-01 00:00:00.000'
ORDER BY login_time DESC
LIMIT 100;
```

#### 查询某个用户最近成功登录记录

该查询适合在用户中心或后台用户详情页展示最近成功登录历史。

```sql
SELECT
    id,
    user_id,
    user_name,
    login_type,
    login_channel,
    client_ip,
    login_region,
    device_name,
    os_name,
    browser_name,
    app_version,
    login_time
FROM sys_login_log
WHERE user_id = 20001
  AND login_action = 'LOGIN'
  AND login_result = 1
ORDER BY login_time DESC
LIMIT 10;
```

#### 查询某个账号最近失败登录次数

该查询适合用于风控判断，例如连续失败达到阈值后锁定账号或要求验证码。

```sql
SELECT
    COUNT(*) AS fail_count
FROM sys_login_log
WHERE account_name = 'admin@example.com'
  AND login_action = 'LOGIN'
  AND login_result = 2
  AND login_time >= DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 15 MINUTE);
```

#### 查询某个 IP 的登录失败记录

该查询用于识别同一 IP 是否存在批量尝试登录、撞库或暴力破解行为。

```sql
SELECT
    id,
    account_name,
    user_type,
    login_type,
    login_channel,
    fail_reason,
    fail_msg,
    client_ip,
    login_region,
    device_id,
    device_name,
    login_time
FROM sys_login_log
WHERE client_ip = '192.168.1.10'
  AND login_result = 2
  AND login_time >= DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 1 HOUR)
ORDER BY login_time DESC
LIMIT 200;
```

#### 按失败原因统计登录失败次数

该查询用于统计登录失败原因分布，例如密码错误、验证码错误、账号禁用、账号锁定等。

```sql
SELECT
    fail_reason,
    COUNT(*) AS fail_count
FROM sys_login_log
WHERE login_result = 2
  AND login_time >= '2026-05-13 00:00:00.000'
  AND login_time < '2026-05-14 00:00:00.000'
GROUP BY fail_reason
ORDER BY fail_count DESC;
```

#### 查询高风险登录记录

该查询用于安全审计，重点查看异地登录、陌生设备、短时间失败过多等风险行为。

```sql
SELECT
    id,
    trace_id,
    account_name,
    user_id,
    user_name,
    user_type,
    login_type,
    login_channel,
    client_ip,
    login_region,
    device_id,
    device_name,
    risk_level,
    risk_tags,
    login_result,
    login_time
FROM sys_login_log
WHERE risk_level >= 3
  AND login_time >= '2026-05-13 00:00:00.000'
  AND login_time < '2026-05-14 00:00:00.000'
ORDER BY login_time DESC
LIMIT 100;
```

#### 查询某个设备的登录账号列表

该查询用于排查同一设备是否登录过多个账号，适合风控和账号安全分析。

```sql
SELECT
    device_id,
    account_name,
    COUNT(*) AS login_count,
    MIN(login_time) AS first_login_time,
    MAX(login_time) AS last_login_time
FROM sys_login_log
WHERE device_id = 'device-202605130001'
  AND login_result = 1
  AND login_time >= '2026-05-01 00:00:00.000'
  AND login_time < '2026-06-01 00:00:00.000'
GROUP BY device_id, account_name
ORDER BY last_login_time DESC;
```

#### 根据链路追踪ID查询登录日志

该查询用于将认证请求与接口调用日志、异常日志串联起来，排查登录链路问题。

```sql
SELECT
    id,
    trace_id,
    account_name,
    user_id,
    user_name,
    login_type,
    login_action,
    login_channel,
    login_result,
    fail_reason,
    fail_msg,
    client_ip,
    risk_level,
    login_time
FROM sys_login_log
WHERE trace_id = 'trace-202605130001'
ORDER BY login_time ASC;
```

### 常用写入

登录日志通常在认证流程结束后写入。无论认证成功还是失败，都建议记录日志。成功日志用于登录历史展示和安全审计，失败日志用于风控统计和问题排查。

下面示例表示一次密码登录成功的日志写入。

```sql
INSERT INTO sys_login_log (
    id,
    trace_id,
    tenant_id,
    account_id,
    account_name,
    user_id,
    user_name,
    user_type,
    login_type,
    login_action,
    login_channel,
    auth_provider,
    login_result,
    fail_reason,
    fail_msg,
    client_ip,
    client_port,
    login_region,
    user_agent,
    device_id,
    device_name,
    os_name,
    browser_name,
    app_version,
    session_id,
    token_id,
    risk_level,
    risk_tags,
    request_uri,
    extra_data,
    login_time,
    created_at
) VALUES (
    1900000000000002001,
    'trace-202605130201',
    10001,
    30001,
    'admin@example.com',
    20001,
    '张三',
    'ADMIN',
    'PASSWORD',
    'LOGIN',
    'ADMIN_WEB',
    'LOCAL',
    1,
    '',
    '',
    '192.168.1.10',
    52341,
    '浙江省杭州市',
    'Mozilla/5.0',
    'device-202605130001',
    'Chrome on Windows',
    'Windows',
    'Chrome',
    '',
    'session_hash_7f8e9a',
    'token_hash_1a2b3c',
    1,
    JSON_ARRAY(),
    '/admin/auth/login',
    JSON_OBJECT('captchaVerified', true),
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
);
```

下面示例表示一次密码错误导致的登录失败日志。失败日志应尽量记录账号、IP、设备、渠道和失败原因，但不能记录用户输入的原始密码。

```sql
INSERT INTO sys_login_log (
    id,
    trace_id,
    tenant_id,
    account_id,
    account_name,
    user_id,
    user_name,
    user_type,
    login_type,
    login_action,
    login_channel,
    auth_provider,
    login_result,
    fail_reason,
    fail_msg,
    client_ip,
    client_port,
    login_region,
    user_agent,
    device_id,
    device_name,
    os_name,
    browser_name,
    app_version,
    session_id,
    token_id,
    risk_level,
    risk_tags,
    request_uri,
    extra_data,
    login_time,
    created_at
) VALUES (
    1900000000000002002,
    'trace-202605130202',
    10001,
    30001,
    'admin@example.com',
    20001,
    '张三',
    'ADMIN',
    'PASSWORD',
    'LOGIN',
    'ADMIN_WEB',
    'LOCAL',
    2,
    'PASSWORD_ERROR',
    '账号或密码错误',
    '192.168.1.10',
    52342,
    '浙江省杭州市',
    'Mozilla/5.0',
    'device-202605130001',
    'Chrome on Windows',
    'Windows',
    'Chrome',
    '',
    '',
    '',
    2,
    JSON_ARRAY('PASSWORD_ERROR', 'SAME_IP_RETRY'),
    '/admin/auth/login',
    JSON_OBJECT('captchaVerified', true, 'remainingRetryCount', 4),
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
);
```

下面示例表示账号退出登录。退出日志可以帮助判断用户是否主动退出、会话是否正常结束。

```sql
INSERT INTO sys_login_log (
    id,
    trace_id,
    tenant_id,
    account_id,
    account_name,
    user_id,
    user_name,
    user_type,
    login_type,
    login_action,
    login_channel,
    auth_provider,
    login_result,
    fail_reason,
    fail_msg,
    client_ip,
    client_port,
    login_region,
    user_agent,
    device_id,
    device_name,
    os_name,
    browser_name,
    app_version,
    session_id,
    token_id,
    risk_level,
    risk_tags,
    request_uri,
    extra_data,
    login_time,
    created_at
) VALUES (
    1900000000000002003,
    'trace-202605130203',
    10001,
    30001,
    'admin@example.com',
    20001,
    '张三',
    'ADMIN',
    'TOKEN',
    'LOGOUT',
    'ADMIN_WEB',
    'LOCAL',
    1,
    '',
    '',
    '192.168.1.10',
    52343,
    '浙江省杭州市',
    'Mozilla/5.0',
    'device-202605130001',
    'Chrome on Windows',
    'Windows',
    'Chrome',
    '',
    'session_hash_7f8e9a',
    'token_hash_1a2b3c',
    1,
    JSON_ARRAY(),
    '/admin/auth/logout',
    JSON_OBJECT('logoutType', 'USER_ACTIVE'),
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
);
```

下面示例表示短信验证码登录失败。此类场景应记录失败原因，但验证码原文不应落库。

```sql
INSERT INTO sys_login_log (
    id,
    trace_id,
    tenant_id,
    account_id,
    account_name,
    user_id,
    user_name,
    user_type,
    login_type,
    login_action,
    login_channel,
    auth_provider,
    login_result,
    fail_reason,
    fail_msg,
    client_ip,
    client_port,
    login_region,
    user_agent,
    device_id,
    device_name,
    os_name,
    browser_name,
    app_version,
    session_id,
    token_id,
    risk_level,
    risk_tags,
    request_uri,
    extra_data,
    login_time,
    created_at
) VALUES (
    1900000000000002004,
    'trace-202605130204',
    10001,
    0,
    '138****8000',
    0,
    '',
    'USER',
    'SMS',
    'LOGIN',
    'APP',
    'LOCAL',
    2,
    'SMS_CODE_ERROR',
    '短信验证码错误或已过期',
    '192.168.1.20',
    60421,
    '浙江省杭州市',
    'App/1.8.0',
    'device-202605130002',
    'iPhone 15',
    'iOS',
    '',
    '1.8.0',
    '',
    '',
    2,
    JSON_ARRAY('SMS_CODE_ERROR'),
    '/app/auth/sms-login',
    JSON_OBJECT('smsScene', 'LOGIN'),
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
);
```

对于高并发登录系统，登录日志可以异步写入。认证主链路只负责生成登录日志事件，后续由消息队列、Redis Stream、Outbox 事件表或异步线程批量落库。但账号锁定、失败次数统计等实时风控逻辑不能只依赖异步日志表，应优先使用 Redis 计数器或专门的风控存储。

### 常见问题

登录日志最常见的问题是只记录成功登录，不记录失败登录。失败登录对风控、安全审计和暴力破解识别更重要，密码错误、验证码错误、账号禁用、账号锁定、设备异常等场景都应记录。

第二个问题是保存敏感认证数据。登录日志中不能保存明文密码、短信验证码、邮箱验证码、Token、Cookie、完整 Session 值、客户端密钥等内容。确需关联会话时，应保存摘要值、令牌编号或内部会话ID。

第三个问题是账号字段设计过于简单。只保存 `user_id` 会导致登录失败时无法记录有效主体，因为失败登录可能还没有完成用户识别。应同时保存 `account_name`，并允许 `account_id`、`user_id` 为空或为0。

第四个问题是没有区分登录方式和登录渠道。登录方式表示如何认证，例如密码、短信、OAuth；登录渠道表示从哪里登录，例如后台、App、小程序、开放接口。两者应分开建模。

第五个问题是没有记录设备信息。对于账号安全系统，设备ID、设备名称、操作系统、浏览器、客户端版本都很重要，可用于识别陌生设备、多账号共用设备和异常客户端版本。

第六个问题是 IP 地区解析强依赖实时查询。建议登录时将解析后的地区冗余写入 `login_region`，避免后台展示时频繁调用 IP 解析服务。

第七个问题是风控统计直接扫描登录日志表。短时间失败次数、IP 失败次数、账号锁定判断等实时风控逻辑应优先使用 Redis 等高性能存储，登录日志表主要用于留痕和离线分析。

第八个问题是日志查询不限制时间范围。登录日志会随着用户访问持续增长，后台查询应默认限制最近 7 天、30 天或当前月份，避免无时间条件扫描全表。

第九个问题是退出登录不记录。对于安全要求较高的系统，退出登录、令牌刷新、会话过期、强制下线都可以作为登录相关动作记录，便于还原完整会话生命周期。

### 总结

登录日志模型的核心目标是记录认证行为，重点解决“哪个账号在什么时间、通过什么方式、从什么客户端登录，结果是否成功，是否存在风险”的问题。

建模时应同时保留账号信息、用户信息、认证方式、登录渠道、客户端信息、认证结果和风险上下文。成功登录用于登录历史展示和安全审计，失败登录用于风控识别和异常排查。

登录日志表应避免保存任何明文认证敏感数据。对于短时间失败次数、账号锁定、IP 限制等实时风控逻辑，不建议直接依赖 MySQL 日志表扫描，而应结合 Redis 或专门风控组件处理。对于大数据量场景，应通过时间范围查询、分区、归档和冷热数据策略控制表规模。



## 接口调用日志模型

接口调用日志模型用于记录系统接口的请求与响应过程，重点关注“哪个接口在什么时间被谁调用、请求来源是什么、响应结果如何、耗时多少、是否出现异常”。它主要服务于接口排障、性能分析、调用链追踪、开放接口对账、安全审计和系统稳定性分析。

接口调用日志与操作日志、审计日志的边界需要明确。接口调用日志关注技术层面的请求与响应，操作日志关注业务动作，审计日志关注数据变更。一次接口请求可以同时产生接口调用日志、操作日志和审计日志，并通过 `trace_id` 串联。

### 适用场景

接口调用日志适用于需要追踪接口访问、排查接口异常、分析接口性能和统计接口调用量的系统。它既可以用于内部系统接口，也可以用于开放平台、网关服务、后台管理接口、App 接口和第三方回调接口。

常见适用场景包括：

| 场景         | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 接口异常排查 | 根据接口地址、状态码、错误码、链路ID定位失败请求       |
| 接口性能分析 | 统计接口耗时、慢请求、超时请求和高峰期调用量           |
| 开放接口审计 | 记录第三方应用调用接口的行为                           |
| 接口调用对账 | 对接支付、物流、短信、供应商等外部服务时记录请求与响应 |
| 网关访问日志 | 在 API 网关层记录统一入口请求                          |
| 安全风控     | 识别高频调用、异常来源 IP、非法请求、签名失败          |
| 灰度发布验证 | 对比新版本接口错误率、耗时和调用量                     |
| 问题复现     | 保留请求摘要和响应摘要，辅助还原问题现场               |

接口调用日志不建议保存完整请求体和完整响应体，尤其是文件上传、导出下载、大对象响应、敏感字段较多的接口。应优先保存摘要、关键字段、脱敏后的参数和错误信息。

### 建模结构

接口调用日志通常使用单表建模，一次接口请求对应一条日志。核心字段围绕调用主体、接口信息、请求信息、响应信息、耗时信息、异常信息和链路追踪展开。

下面是接口调用日志表的基础结构。该结构只定义字段和主键，普通索引统一放在“索引设计”章节中说明。

```sql
CREATE TABLE sys_api_call_log (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    trace_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '链路追踪ID',
    span_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '链路Span ID',
    parent_span_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '父级Span ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，非多租户系统可固定为0',

    caller_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '调用人ID，未登录或外部调用时可为0',
    caller_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '调用人名称',
    caller_type VARCHAR(32) NOT NULL DEFAULT 'ANONYMOUS' COMMENT '调用人类型：ANONYMOUS-匿名，USER-用户，ADMIN-管理员，APP-应用，SYSTEM-系统',
    app_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '调用方应用ID，适合开放平台或内部服务调用',
    app_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '调用方应用名称',

    system_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '系统编码',
    service_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '服务名称',
    module_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '模块编码',
    api_code VARCHAR(128) NOT NULL DEFAULT '' COMMENT '接口编码',
    api_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '接口名称',

    request_method VARCHAR(16) NOT NULL DEFAULT '' COMMENT '请求方法',
    request_uri VARCHAR(512) NOT NULL DEFAULT '' COMMENT '请求URI，不建议保存完整QueryString',
    route_pattern VARCHAR(255) NOT NULL DEFAULT '' COMMENT '路由模式，例如/admin/order/{id}',
    protocol VARCHAR(32) NOT NULL DEFAULT 'HTTP' COMMENT '协议类型，例如HTTP、HTTPS、RPC、MQ',
    content_type VARCHAR(128) NOT NULL DEFAULT '' COMMENT '请求Content-Type',

    client_ip VARCHAR(64) NOT NULL DEFAULT '' COMMENT '客户端IP',
    client_port INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '客户端端口',
    user_agent VARCHAR(512) NOT NULL DEFAULT '' COMMENT '客户端标识',
    referer VARCHAR(512) NOT NULL DEFAULT '' COMMENT '来源页面',
    request_headers JSON NULL COMMENT '请求头摘要，必须过滤敏感Header',
    request_params JSON NULL COMMENT '请求参数摘要，必须脱敏',
    request_body JSON NULL COMMENT '请求体摘要，建议只保存关键字段',
    request_size BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '请求大小，单位字节',

    http_status SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'HTTP状态码',
    business_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '业务响应码',
    business_msg VARCHAR(255) NOT NULL DEFAULT '' COMMENT '业务响应消息',
    response_headers JSON NULL COMMENT '响应头摘要',
    response_body JSON NULL COMMENT '响应体摘要，建议只保存关键字段',
    response_size BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '响应大小，单位字节',

    call_result TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '调用结果：1-成功，2-失败，3-超时，4-拒绝',
    error_type VARCHAR(64) NOT NULL DEFAULT '' COMMENT '错误类型，例如VALIDATION、AUTH、BIZ、SYSTEM、TIMEOUT',
    error_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '错误码',
    error_msg VARCHAR(512) NOT NULL DEFAULT '' COMMENT '错误信息',
    exception_name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '异常类名，不保存完整堆栈',

    start_time DATETIME(3) NOT NULL COMMENT '请求开始时间',
    end_time DATETIME(3) NULL COMMENT '请求结束时间',
    cost_ms INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '接口耗时，单位毫秒',

    gateway_flag TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '是否网关记录：1-是，2-否',
    external_flag TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '是否外部调用：1-是，2-否',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '重试次数',
    risk_level TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '风险等级：1-正常，2-低风险，3-中风险，4-高风险',
    risk_tags JSON NULL COMMENT '风险标签，例如高频调用、签名失败、非法来源',

    extra_data JSON NULL COMMENT '扩展数据',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '接口调用日志表';
```

该模型将接口元数据、调用方信息、请求摘要、响应摘要、错误信息和耗时信息放在同一张表中，适合后台列表查询和问题排查。对于请求体、响应体较大的系统，不建议直接保存完整内容，可以只保存摘要字段，完整报文进入对象存储或日志平台，并在 `extra_data` 中保存引用地址。

`trace_id` 用于串联一次完整请求链路，`span_id` 和 `parent_span_id` 用于描述调用层级。对于简单单体系统，可以只使用 `trace_id`；对于微服务或网关场景，建议保留三个字段。

### 字段设计

接口调用日志字段设计需要保证能够回答几个核心问题：谁调用了接口、调用哪个接口、从哪里调用、请求内容是什么、响应结果是什么、耗时多少、异常原因是什么。

| 字段               | 类型                | 设计说明                                                   |
| ------------------ | ------------------- | ---------------------------------------------------------- |
| `id`               | `BIGINT UNSIGNED`   | 主键ID，建议使用分布式ID                                   |
| `trace_id`         | `VARCHAR(64)`       | 链路追踪ID，用于关联接口日志、操作日志、审计日志和异常日志 |
| `span_id`          | `VARCHAR(64)`       | 当前调用节点ID，适合微服务调用链                           |
| `parent_span_id`   | `VARCHAR(64)`       | 父级调用节点ID，适合还原调用树                             |
| `tenant_id`        | `BIGINT UNSIGNED`   | 租户ID，多租户系统必须保留                                 |
| `caller_id`        | `BIGINT UNSIGNED`   | 调用人ID，匿名请求或系统调用可为0                          |
| `caller_name`      | `VARCHAR(64)`       | 调用人名称，建议冗余保存                                   |
| `caller_type`      | `VARCHAR(32)`       | 调用人类型，用于区分匿名用户、普通用户、管理员、应用和系统 |
| `app_id`           | `VARCHAR(64)`       | 调用方应用ID，开放平台和内部服务调用建议保留               |
| `app_name`         | `VARCHAR(128)`      | 调用方应用名称                                             |
| `system_code`      | `VARCHAR(64)`       | 当前系统编码                                               |
| `service_name`     | `VARCHAR(128)`      | 当前服务名称，例如 `order-service`                         |
| `module_code`      | `VARCHAR(64)`       | 模块编码，例如订单、商品、支付                             |
| `api_code`         | `VARCHAR(128)`      | 接口编码，适合稳定统计，不建议直接依赖 URL                 |
| `api_name`         | `VARCHAR(128)`      | 接口名称，用于后台展示                                     |
| `request_method`   | `VARCHAR(16)`       | 请求方法，例如 GET、POST、PUT、DELETE                      |
| `request_uri`      | `VARCHAR(512)`      | 请求 URI，不建议保存完整 QueryString                       |
| `route_pattern`    | `VARCHAR(255)`      | 路由模板，例如 `/admin/order/{id}`，适合聚合统计           |
| `protocol`         | `VARCHAR(32)`       | 协议类型，兼容 HTTP、RPC、MQ 等场景                        |
| `content_type`     | `VARCHAR(128)`      | 请求内容类型                                               |
| `client_ip`        | `VARCHAR(64)`       | 客户端 IP，兼容 IPv4 和 IPv6                               |
| `client_port`      | `INT UNSIGNED`      | 客户端端口，可按需记录                                     |
| `user_agent`       | `VARCHAR(512)`      | 客户端标识                                                 |
| `referer`          | `VARCHAR(512)`      | 来源页面                                                   |
| `request_headers`  | `JSON`              | 请求头摘要，必须过滤 Authorization、Cookie 等敏感字段      |
| `request_params`   | `JSON`              | Query 或表单参数摘要，必须脱敏                             |
| `request_body`     | `JSON`              | 请求体摘要，建议只保存关键字段                             |
| `request_size`     | `BIGINT UNSIGNED`   | 请求大小，便于分析大请求                                   |
| `http_status`      | `SMALLINT UNSIGNED` | HTTP 状态码                                                |
| `business_code`    | `VARCHAR(64)`       | 业务响应码                                                 |
| `business_msg`     | `VARCHAR(255)`      | 业务响应消息                                               |
| `response_headers` | `JSON`              | 响应头摘要                                                 |
| `response_body`    | `JSON`              | 响应体摘要，不建议保存完整响应                             |
| `response_size`    | `BIGINT UNSIGNED`   | 响应大小，便于分析大响应                                   |
| `call_result`      | `TINYINT UNSIGNED`  | 调用结果，例如成功、失败、超时、拒绝                       |
| `error_type`       | `VARCHAR(64)`       | 错误类型，便于区分参数、鉴权、业务、系统、超时             |
| `error_code`       | `VARCHAR(64)`       | 错误码                                                     |
| `error_msg`        | `VARCHAR(512)`      | 错误摘要                                                   |
| `exception_name`   | `VARCHAR(255)`      | 异常类名，不建议保存完整堆栈                               |
| `start_time`       | `DATETIME(3)`       | 请求开始时间                                               |
| `end_time`         | `DATETIME(3)`       | 请求结束时间                                               |
| `cost_ms`          | `INT UNSIGNED`      | 请求耗时，单位毫秒                                         |
| `gateway_flag`     | `TINYINT UNSIGNED`  | 是否由网关层记录                                           |
| `external_flag`    | `TINYINT UNSIGNED`  | 是否外部调用                                               |
| `retry_count`      | `INT UNSIGNED`      | 重试次数                                                   |
| `risk_level`       | `TINYINT UNSIGNED`  | 风险等级                                                   |
| `risk_tags`        | `JSON`              | 风险标签                                                   |
| `extra_data`       | `JSON`              | 扩展数据                                                   |
| `created_at`       | `DATETIME(3)`       | 日志落库时间                                               |

字段设计时需要重点处理两个问题。第一，URL 与路由模板要分开保存。`request_uri` 适合定位具体请求，`route_pattern` 适合做接口维度统计，例如 `/admin/order/10001` 和 `/admin/order/10002` 应归类到 `/admin/order/{id}`。

第二，请求与响应内容必须做摘要化和脱敏处理。密码、Token、Cookie、Authorization、手机号、身份证号、银行卡号、密钥、验证码等内容不应明文保存。

### 索引设计

接口调用日志索引应围绕时间范围、接口维度、调用方、状态码、错误码、耗时、IP 和链路追踪设计。由于接口调用日志写入量通常很大，索引数量应严格控制，避免影响高并发写入。

下面是推荐的基础索引设计。

```sql
ALTER TABLE sys_api_call_log
    ADD INDEX idx_tenant_time (tenant_id, start_time),
    ADD INDEX idx_api_time (api_code, start_time),
    ADD INDEX idx_route_time (route_pattern, start_time),
    ADD INDEX idx_caller_time (caller_id, start_time),
    ADD INDEX idx_app_time (app_id, start_time),
    ADD INDEX idx_ip_time (client_ip, start_time),
    ADD INDEX idx_status_time (http_status, start_time),
    ADD INDEX idx_result_time (call_result, start_time),
    ADD INDEX idx_error_code_time (error_code, start_time),
    ADD INDEX idx_cost_time (cost_ms, start_time),
    ADD INDEX idx_trace_id (trace_id);
```

各索引的用途如下：

| 索引                  | 适用查询                       |
| --------------------- | ------------------------------ |
| `idx_tenant_time`     | 按租户查询接口调用日志         |
| `idx_api_time`        | 按接口编码查询调用历史         |
| `idx_route_time`      | 按路由模板统计接口调用量       |
| `idx_caller_time`     | 查询某个用户的接口调用记录     |
| `idx_app_time`        | 查询某个应用的开放接口调用记录 |
| `idx_ip_time`         | 按来源 IP 查询接口调用记录     |
| `idx_status_time`     | 按 HTTP 状态码筛选请求         |
| `idx_result_time`     | 查询成功、失败、超时或拒绝请求 |
| `idx_error_code_time` | 按错误码排查异常请求           |
| `idx_cost_time`       | 查询慢接口或高耗时请求         |
| `idx_trace_id`        | 根据链路ID定位一次完整请求     |

如果接口调用日志只用于技术排障，且已经接入专业日志平台或 APM 系统，MySQL 表中的索引可以更少，只保留 `trace_id`、`start_time`、`api_code`、`call_result` 等核心索引。

如果该表用于开放平台计费、接口调用量统计或调用方对账，可以增加 `(app_id, api_code, start_time)` 组合索引。但该索引写入成本较高，应根据实际查询频率决定。

### 常用查询

接口调用日志查询通常围绕接口、时间、调用方、状态码、错误码、耗时和链路ID展开。查询时必须带时间范围，避免扫描大量历史日志。

#### 查询最近接口调用日志

该查询适合后台接口调用日志列表页，按请求开始时间倒序展示最近请求。

```sql
SELECT
    id,
    trace_id,
    caller_id,
    caller_name,
    caller_type,
    app_id,
    service_name,
    api_code,
    api_name,
    request_method,
    request_uri,
    http_status,
    business_code,
    call_result,
    cost_ms,
    client_ip,
    start_time
FROM sys_api_call_log
WHERE tenant_id = 10001
  AND start_time >= '2026-05-01 00:00:00.000'
  AND start_time < '2026-06-01 00:00:00.000'
ORDER BY start_time DESC
LIMIT 50;
```

#### 根据链路追踪ID查询接口调用

该查询用于排查一次请求的完整接口调用记录，并可以继续关联操作日志、审计日志和异常日志。

```sql
SELECT
    id,
    trace_id,
    span_id,
    parent_span_id,
    service_name,
    api_code,
    api_name,
    request_method,
    request_uri,
    http_status,
    business_code,
    business_msg,
    call_result,
    error_code,
    error_msg,
    exception_name,
    cost_ms,
    start_time,
    end_time
FROM sys_api_call_log
WHERE trace_id = 'trace-202605130301'
ORDER BY start_time ASC;
```

#### 查询某个接口的调用历史

该查询用于查看某个接口在指定时间范围内的调用明细。

```sql
SELECT
    id,
    trace_id,
    caller_name,
    app_id,
    request_method,
    request_uri,
    http_status,
    business_code,
    call_result,
    error_code,
    cost_ms,
    client_ip,
    start_time
FROM sys_api_call_log
WHERE api_code = 'ORDER_QUERY_DETAIL'
  AND start_time >= '2026-05-13 00:00:00.000'
  AND start_time < '2026-05-14 00:00:00.000'
ORDER BY start_time DESC
LIMIT 100;
```

#### 查询失败接口调用

该查询用于排查业务失败、系统异常、鉴权失败、参数错误等请求。

```sql
SELECT
    id,
    trace_id,
    service_name,
    api_code,
    api_name,
    request_method,
    request_uri,
    http_status,
    business_code,
    business_msg,
    call_result,
    error_type,
    error_code,
    error_msg,
    exception_name,
    caller_id,
    app_id,
    client_ip,
    start_time
FROM sys_api_call_log
WHERE call_result = 2
  AND start_time >= '2026-05-13 00:00:00.000'
  AND start_time < '2026-05-14 00:00:00.000'
ORDER BY start_time DESC
LIMIT 100;
```

#### 查询慢接口调用

该查询用于定位高耗时请求，例如数据库慢查询、外部服务超时、复杂导出、批量处理等。

```sql
SELECT
    id,
    trace_id,
    service_name,
    api_code,
    api_name,
    request_method,
    route_pattern,
    http_status,
    call_result,
    cost_ms,
    caller_name,
    app_id,
    client_ip,
    start_time
FROM sys_api_call_log
WHERE start_time >= '2026-05-13 00:00:00.000'
  AND start_time < '2026-05-14 00:00:00.000'
  AND cost_ms >= 3000
ORDER BY cost_ms DESC
LIMIT 100;
```

#### 统计接口调用量和平均耗时

该查询用于接口维度的基础统计，适合后台报表或接口治理页面。

```sql
SELECT
    api_code,
    api_name,
    route_pattern,
    COUNT(*) AS call_count,
    SUM(CASE WHEN call_result = 1 THEN 1 ELSE 0 END) AS success_count,
    SUM(CASE WHEN call_result <> 1 THEN 1 ELSE 0 END) AS fail_count,
    ROUND(AVG(cost_ms), 2) AS avg_cost_ms,
    MAX(cost_ms) AS max_cost_ms
FROM sys_api_call_log
WHERE start_time >= '2026-05-13 00:00:00.000'
  AND start_time < '2026-05-14 00:00:00.000'
GROUP BY api_code, api_name, route_pattern
ORDER BY call_count DESC
LIMIT 100;
```

#### 统计接口错误码分布

该查询用于分析某段时间内接口异常的主要原因。

```sql
SELECT
    error_type,
    error_code,
    COUNT(*) AS error_count
FROM sys_api_call_log
WHERE call_result <> 1
  AND start_time >= '2026-05-13 00:00:00.000'
  AND start_time < '2026-05-14 00:00:00.000'
GROUP BY error_type, error_code
ORDER BY error_count DESC;
```

#### 查询某个调用方应用的接口调用记录

该查询适用于开放平台、内部服务调用、第三方系统对接等场景。

```sql
SELECT
    id,
    trace_id,
    app_id,
    app_name,
    api_code,
    api_name,
    request_method,
    request_uri,
    http_status,
    business_code,
    call_result,
    error_code,
    cost_ms,
    retry_count,
    start_time
FROM sys_api_call_log
WHERE app_id = 'third-app-10001'
  AND start_time >= '2026-05-01 00:00:00.000'
  AND start_time < '2026-06-01 00:00:00.000'
ORDER BY start_time DESC
LIMIT 100;
```

#### 查询某个 IP 的高频调用接口

该查询用于识别异常来源、爬虫访问、暴力请求或接口刷量行为。

```sql
SELECT
    client_ip,
    route_pattern,
    COUNT(*) AS call_count,
    COUNT(DISTINCT caller_id) AS caller_count,
    ROUND(AVG(cost_ms), 2) AS avg_cost_ms,
    MAX(start_time) AS last_call_time
FROM sys_api_call_log
WHERE client_ip = '192.168.1.10'
  AND start_time >= DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 1 HOUR)
GROUP BY client_ip, route_pattern
ORDER BY call_count DESC
LIMIT 50;
```

#### 查询接口状态码分布

该查询用于分析 HTTP 层面的接口健康状况，例如 2xx、4xx、5xx 的分布。

```sql
SELECT
    http_status,
    COUNT(*) AS status_count
FROM sys_api_call_log
WHERE start_time >= '2026-05-13 00:00:00.000'
  AND start_time < '2026-05-14 00:00:00.000'
GROUP BY http_status
ORDER BY http_status ASC;
```

### 常用写入

接口调用日志通常由网关过滤器、Web 拦截器、AOP 切面、RPC 拦截器或统一异常处理组件写入。写入时应尽量避免阻塞主请求链路，普通接口调用日志建议异步写入，关键开放接口、支付回调、订单回调等可以同步或通过可靠事件方式写入。

下面示例表示一次接口调用成功的日志写入。

```sql
INSERT INTO sys_api_call_log (
    id,
    trace_id,
    span_id,
    parent_span_id,
    tenant_id,
    caller_id,
    caller_name,
    caller_type,
    app_id,
    app_name,
    system_code,
    service_name,
    module_code,
    api_code,
    api_name,
    request_method,
    request_uri,
    route_pattern,
    protocol,
    content_type,
    client_ip,
    client_port,
    user_agent,
    referer,
    request_headers,
    request_params,
    request_body,
    request_size,
    http_status,
    business_code,
    business_msg,
    response_headers,
    response_body,
    response_size,
    call_result,
    error_type,
    error_code,
    error_msg,
    exception_name,
    start_time,
    end_time,
    cost_ms,
    gateway_flag,
    external_flag,
    retry_count,
    risk_level,
    risk_tags,
    extra_data,
    created_at
) VALUES (
    1900000000000003001,
    'trace-202605130301',
    'span-001',
    '',
    10001,
    20001,
    '张三',
    'ADMIN',
    '',
    '',
    'order-center',
    'order-service',
    'ORDER',
    'ORDER_QUERY_DETAIL',
    '查询订单详情',
    'GET',
    '/admin/order/900000001',
    '/admin/order/{id}',
    'HTTPS',
    'application/json',
    '192.168.1.10',
    52341,
    'Mozilla/5.0',
    'https://admin.example.com/order/list',
    JSON_OBJECT('x-request-id', 'trace-202605130301'),
    JSON_OBJECT('id', '900000001'),
    NULL,
    128,
    200,
    'SUCCESS',
    '成功',
    JSON_OBJECT('content-type', 'application/json'),
    JSON_OBJECT('orderId', '900000001', 'orderNo', 'ORD202605130001'),
    512,
    1,
    '',
    '',
    '',
    '',
    '2026-05-13 10:15:30.123',
    '2026-05-13 10:15:30.209',
    86,
    2,
    2,
    0,
    1,
    JSON_ARRAY(),
    JSON_OBJECT('logSource', 'web-interceptor'),
    CURRENT_TIMESTAMP(3)
);
```

下面示例表示一次业务失败的接口调用日志。业务失败不一定是 HTTP 失败，例如 HTTP 状态码为 200，但业务响应码表示失败。

```sql
INSERT INTO sys_api_call_log (
    id,
    trace_id,
    span_id,
    parent_span_id,
    tenant_id,
    caller_id,
    caller_name,
    caller_type,
    app_id,
    app_name,
    system_code,
    service_name,
    module_code,
    api_code,
    api_name,
    request_method,
    request_uri,
    route_pattern,
    protocol,
    content_type,
    client_ip,
    client_port,
    user_agent,
    referer,
    request_headers,
    request_params,
    request_body,
    request_size,
    http_status,
    business_code,
    business_msg,
    response_headers,
    response_body,
    response_size,
    call_result,
    error_type,
    error_code,
    error_msg,
    exception_name,
    start_time,
    end_time,
    cost_ms,
    gateway_flag,
    external_flag,
    retry_count,
    risk_level,
    risk_tags,
    extra_data,
    created_at
) VALUES (
    1900000000000003002,
    'trace-202605130302',
    'span-001',
    '',
    10001,
    20001,
    '张三',
    'ADMIN',
    '',
    '',
    'order-center',
    'order-service',
    'ORDER',
    'ORDER_CANCEL',
    '取消订单',
    'POST',
    '/admin/order/cancel',
    '/admin/order/cancel',
    'HTTPS',
    'application/json',
    '192.168.1.10',
    52342,
    'Mozilla/5.0',
    'https://admin.example.com/order/detail',
    JSON_OBJECT('x-request-id', 'trace-202605130302'),
    NULL,
    JSON_OBJECT('orderId', '900000001', 'cancelReason', '用户申请取消'),
    256,
    200,
    'ORDER_STATUS_NOT_ALLOWED',
    '当前订单状态不允许取消',
    JSON_OBJECT('content-type', 'application/json'),
    JSON_OBJECT('success', false, 'code', 'ORDER_STATUS_NOT_ALLOWED'),
    256,
    2,
    'BIZ',
    'ORDER_STATUS_NOT_ALLOWED',
    '当前订单状态不允许取消',
    '',
    '2026-05-13 10:18:10.100',
    '2026-05-13 10:18:10.142',
    42,
    2,
    2,
    0,
    1,
    JSON_ARRAY(),
    JSON_OBJECT('logSource', 'web-interceptor'),
    CURRENT_TIMESTAMP(3)
);
```

下面示例表示一次系统异常的接口调用日志。异常日志只保存异常类名和错误摘要，完整堆栈应进入应用日志平台。

```sql
INSERT INTO sys_api_call_log (
    id,
    trace_id,
    span_id,
    parent_span_id,
    tenant_id,
    caller_id,
    caller_name,
    caller_type,
    app_id,
    app_name,
    system_code,
    service_name,
    module_code,
    api_code,
    api_name,
    request_method,
    request_uri,
    route_pattern,
    protocol,
    content_type,
    client_ip,
    client_port,
    user_agent,
    referer,
    request_headers,
    request_params,
    request_body,
    request_size,
    http_status,
    business_code,
    business_msg,
    response_headers,
    response_body,
    response_size,
    call_result,
    error_type,
    error_code,
    error_msg,
    exception_name,
    start_time,
    end_time,
    cost_ms,
    gateway_flag,
    external_flag,
    retry_count,
    risk_level,
    risk_tags,
    extra_data,
    created_at
) VALUES (
    1900000000000003003,
    'trace-202605130303',
    'span-001',
    '',
    10001,
    20002,
    '李四',
    'ADMIN',
    '',
    '',
    'report-center',
    'report-service',
    'REPORT',
    'REPORT_EXPORT',
    '导出报表',
    'POST',
    '/admin/report/export',
    '/admin/report/export',
    'HTTPS',
    'application/json',
    '192.168.1.11',
    52343,
    'Mozilla/5.0',
    'https://admin.example.com/report/list',
    JSON_OBJECT('x-request-id', 'trace-202605130303'),
    NULL,
    JSON_OBJECT('reportType', 'ORDER_SUMMARY', 'dateRange', '2026-05'),
    300,
    500,
    'SYSTEM_ERROR',
    '系统异常',
    JSON_OBJECT('content-type', 'application/json'),
    JSON_OBJECT('success', false, 'code', 'SYSTEM_ERROR'),
    180,
    2,
    'SYSTEM',
    'SYSTEM_ERROR',
    '报表导出失败',
    'java.lang.RuntimeException',
    '2026-05-13 10:20:00.000',
    '2026-05-13 10:20:05.230',
    5230,
    2,
    2,
    0,
    2,
    JSON_ARRAY('SLOW_REQUEST'),
    JSON_OBJECT('logSource', 'global-exception-handler'),
    CURRENT_TIMESTAMP(3)
);
```

下面示例表示一次开放平台接口调用。开放接口通常需要记录 `app_id`、签名结果、调用来源和外部调用标记。

```sql
INSERT INTO sys_api_call_log (
    id,
    trace_id,
    span_id,
    parent_span_id,
    tenant_id,
    caller_id,
    caller_name,
    caller_type,
    app_id,
    app_name,
    system_code,
    service_name,
    module_code,
    api_code,
    api_name,
    request_method,
    request_uri,
    route_pattern,
    protocol,
    content_type,
    client_ip,
    client_port,
    user_agent,
    referer,
    request_headers,
    request_params,
    request_body,
    request_size,
    http_status,
    business_code,
    business_msg,
    response_headers,
    response_body,
    response_size,
    call_result,
    error_type,
    error_code,
    error_msg,
    exception_name,
    start_time,
    end_time,
    cost_ms,
    gateway_flag,
    external_flag,
    retry_count,
    risk_level,
    risk_tags,
    extra_data,
    created_at
) VALUES (
    1900000000000003004,
    'trace-202605130304',
    'span-001',
    '',
    10001,
    0,
    '',
    'APP',
    'third-app-10001',
    '第三方订单系统',
    'open-platform',
    'open-api-service',
    'OPEN_ORDER',
    'OPEN_ORDER_PUSH',
    '开放订单推送',
    'POST',
    '/open/order/push',
    '/open/order/push',
    'HTTPS',
    'application/json',
    '203.0.113.10',
    44321,
    'ThirdClient/2.1.0',
    '',
    JSON_OBJECT('x-app-id', 'third-app-10001', 'x-timestamp', '1778640000000'),
    NULL,
    JSON_OBJECT('orderNo', 'EXT202605130001', 'amount', '168.00'),
    512,
    200,
    'SUCCESS',
    '成功',
    JSON_OBJECT('content-type', 'application/json'),
    JSON_OBJECT('success', true, 'requestId', 'trace-202605130304'),
    180,
    1,
    '',
    '',
    '',
    '',
    '2026-05-13 10:25:00.000',
    '2026-05-13 10:25:00.135',
    135,
    1,
    1,
    0,
    1,
    JSON_ARRAY(),
    JSON_OBJECT('signatureVerified', true, 'logSource', 'api-gateway'),
    CURRENT_TIMESTAMP(3)
);
```

对于高并发系统，接口调用日志建议异步写入。常见方式包括网关异步队列、应用内事件、消息队列、Redis Stream、Outbox 事件表或日志采集组件。实时限流、黑名单、接口熔断、鉴权校验等逻辑不应依赖 MySQL 日志表实时扫描，而应由网关、Redis、限流组件或风控服务完成。

### 常见问题

接口调用日志最常见的问题是保存完整请求体和完整响应体。这样会导致日志表快速膨胀，并可能泄露敏感数据。建议只保存摘要、关键业务字段、错误码和必要上下文，大报文进入日志平台或对象存储。

第二个问题是没有脱敏请求头。`Authorization`、`Cookie`、`Set-Cookie`、`X-Api-Key`、签名密钥、访问令牌等 Header 不应明文落库。保存请求头时必须使用白名单或敏感字段过滤策略。

第三个问题是只记录失败请求，不记录成功请求。只记录失败请求虽然节省空间，但无法统计接口调用量、成功率、平均耗时和调用趋势。可以根据业务重要性决定采样比例，但关键接口和开放接口建议完整记录。

第四个问题是使用具体 URL 做统计维度。带 ID 的 URL 会导致统计维度爆炸，例如 `/admin/order/1`、`/admin/order/2` 会被识别为两个接口。应额外保存 `route_pattern` 或 `api_code`。

第五个问题是没有区分 HTTP 状态码和业务响应码。HTTP 200 不代表业务成功，业务失败也可能返回 HTTP 200。因此应同时保存 `http_status`、`business_code` 和 `call_result`。

第六个问题是没有记录耗时。缺少 `start_time`、`end_time` 和 `cost_ms` 后，接口日志只能用于错误排查，无法用于性能治理。建议至少保存 `start_time` 和 `cost_ms`。

第七个问题是接口调用日志与应用日志割裂。接口调用日志应保存 `trace_id`，应用日志、操作日志、审计日志、异常日志应使用同一个 `trace_id`，否则排查链路会被打断。

第八个问题是把接口日志表当作实时风控表。接口日志适合留痕和查询，不适合作为高频实时判断的数据源。限流、频控、黑名单、签名失败计数等应使用 Redis、网关状态或专门风控组件。

第九个问题是索引过多。接口调用日志写入量通常远高于操作日志和审计日志，过多索引会明显影响写入性能。索引应基于真实查询场景保留，低频统计可以交给离线数仓或日志平台。

第十个问题是长期保留全量明细。接口调用日志增长极快，应设置明确保留周期，例如在线保留 7 天、30 天或 90 天，历史数据归档到日志平台、对象存储、ClickHouse、Elasticsearch 或数据仓库。

### 总结

接口调用日志模型的核心目标是记录接口请求与响应过程，重点解决“哪个接口被谁调用、调用来源是什么、响应是否成功、耗时多少、异常原因是什么”的问题。

建模时应将调用主体、接口元数据、请求摘要、响应摘要、错误信息、耗时信息和链路追踪字段拆分清楚。`api_code` 和 `route_pattern` 用于稳定统计，`request_uri` 用于定位具体请求，`trace_id` 用于串联完整调用链。

对于普通业务系统，接口调用日志可以异步写入 MySQL，并结合时间范围查询、分区和归档控制数据规模。对于高并发、大流量或多服务系统，MySQL 更适合保存关键接口摘要，完整访问日志、链路追踪和性能分析应交给日志平台、APM、ClickHouse、Elasticsearch 或数据仓库处理。