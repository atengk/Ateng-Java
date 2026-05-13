# 数据生命周期模型

数据生命周期模型用于描述业务数据从创建、使用、删除、归档到冷热分层的完整管理方式。对于 MySQL 8 业务系统而言，生命周期建模的核心目标不是简单保存数据，而是在保证业务可追溯、查询性能、合规审计和数据治理成本之间取得平衡。

## 软删除模型

软删除模型是指业务删除数据时，不直接执行物理删除，而是通过删除标识、删除时间、删除人等字段标记数据已删除。该模型适合大多数需要保留业务痕迹、支持恢复、支持审计或避免误删风险的业务表。

### 适用场景

软删除适用于“业务上不可见，但数据库中仍需保留”的数据。常见于用户、订单、商品、配置、字典、组织、角色、菜单、附件、合同、审批单等业务表。

适合使用软删除的场景主要包括：

| 场景                       | 说明                                         |
| -------------------------- | -------------------------------------------- |
| 需要防止误删               | 用户删除后可通过后台恢复                     |
| 需要审计追踪               | 可记录删除时间、删除人和删除原因             |
| 存在业务关联               | 避免物理删除导致外键或业务引用断裂           |
| 需要历史查询               | 已删除数据仍可能用于报表、对账、统计         |
| 删除后仍需合规保留         | 合同、订单、支付、审计等数据不应立即物理删除 |
| 高频业务查询只关注有效数据 | 查询时统一过滤未删除数据                     |

不适合使用软删除的场景包括临时表、中间计算表、缓存型数据表、无业务追溯价值的日志明细表，以及数据量极大且删除后无恢复价值的数据表。这类数据更适合使用物理删除、分区清理或归档模型。

### 建模结构

软删除模型的核心结构是在业务主表中增加删除状态字段，并将所有业务查询默认限定为未删除数据。软删除字段属于生命周期控制字段，不应影响主键设计、业务单号设计和业务状态机设计。

典型结构如下：

```sql
CREATE TABLE biz_customer (
    id              BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    customer_no     VARCHAR(64)     NOT NULL COMMENT '客户编号',
    customer_name   VARCHAR(128)    NOT NULL COMMENT '客户名称',
    mobile          VARCHAR(32)     DEFAULT NULL COMMENT '手机号',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '业务状态：1正常，2停用',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    delete_time     DATETIME        DEFAULT NULL COMMENT '删除时间',
    delete_by       BIGINT UNSIGNED DEFAULT NULL COMMENT '删除人ID',
    delete_reason   VARCHAR(255)    DEFAULT NULL COMMENT '删除原因',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by       BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    update_by       BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '客户表';
```

该结构中，`deleted` 是软删除判断的核心字段。业务查询默认使用 `deleted = 0`，删除操作通过更新该字段完成。`delete_time`、`delete_by` 和 `delete_reason` 用于补充删除审计信息，便于后续恢复、排查和数据治理。

### 字段设计

软删除字段应保持简单、稳定、统一。建议所有需要软删除的业务表使用相同字段命名，避免不同表之间出现 `is_deleted`、`del_flag`、`delete_flag` 混用的问题。

| 字段名          | 类型              | 是否必填 | 默认值 | 说明                             |
| --------------- | ----------------- | -------- | ------ | -------------------------------- |
| `deleted`       | `TINYINT`         | 是       | `0`    | 删除标识：`0` 未删除，`1` 已删除 |
| `delete_time`   | `DATETIME`        | 否       | `NULL` | 删除时间                         |
| `delete_by`     | `BIGINT UNSIGNED` | 否       | `NULL` | 删除人 ID                        |
| `delete_reason` | `VARCHAR(255)`    | 否       | `NULL` | 删除原因                         |

字段设计建议如下：

| 设计项       | 建议                                      |
| ------------ | ----------------------------------------- |
| 删除标识类型 | 使用 `TINYINT`，避免使用字符串状态        |
| 未删除值     | 固定为 `0`                                |
| 已删除值     | 固定为 `1`                                |
| 删除时间     | 使用 `DATETIME`，由应用层或数据库函数写入 |
| 删除人       | 保存当前登录用户 ID，不建议保存用户名     |
| 删除原因     | 可选字段，后台管理、审批类业务建议保留    |
| 字段命名     | 全系统统一，便于 ORM、SQL 规范和查询封装  |

对于 MySQL 8，不建议使用 `NULL` 表示未删除、非 `NULL` 表示已删除作为唯一判断条件。虽然这种方式可以减少一个字段，但查询表达不如 `deleted = 0` 清晰，也不利于统一建模规范。

### 索引设计

软删除字段通常不会单独建立索引。原因是 `deleted` 的区分度较低，单独索引过滤效果有限。更常见的做法是将 `deleted` 放入业务查询相关的联合索引中。

常见索引设计如下：

```sql
ALTER TABLE biz_customer
    ADD UNIQUE KEY uk_customer_no_deleted (customer_no, deleted),
    ADD KEY idx_status_deleted_create_time (status, deleted, create_time),
    ADD KEY idx_deleted_delete_time (deleted, delete_time);
```

索引设计说明：

| 索引                             | 适用场景                         |
| -------------------------------- | -------------------------------- |
| `uk_customer_no_deleted`         | 控制未删除数据中的业务编号唯一性 |
| `idx_status_deleted_create_time` | 支持按业务状态查询有效数据列表   |
| `idx_deleted_delete_time`        | 支持后台查询已删除数据和定期清理 |

如果业务要求“客户编号在全表永久唯一”，则应使用 `UNIQUE KEY uk_customer_no (customer_no)`，不要将 `deleted` 放入唯一索引。

如果业务允许“删除后重新创建相同编号”，可以使用 `(customer_no, deleted)` 这类联合唯一索引，但要注意 MySQL 中已删除数据仍然只有一条 `deleted = 1` 记录时才能成立。如果同一个业务编号可能被多次删除，建议改用删除版本号或删除批次字段，例如：

```sql
ALTER TABLE biz_customer
    ADD delete_version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除版本号：未删除为0，删除后写入主键ID或时间戳',
    ADD UNIQUE KEY uk_customer_no_delete_version (customer_no, delete_version);
```

该方式可以支持同一个业务编号多次创建、删除，并避免 `(customer_no, deleted)` 在多次软删除场景下产生唯一键冲突。

### 常用查询

软删除模型的查询重点是统一过滤未删除数据，并为后台恢复、审计、清理提供已删除数据查询能力。业务代码中不应让每个查询随意决定是否过滤删除标识，应通过 DAO、Mapper、ORM 插件或统一 SQL 规范固定下来。

#### 查询未删除数据列表

业务前台、管理后台的普通列表查询应默认只查未删除数据。

```sql
SELECT
    id,
    customer_no,
    customer_name,
    mobile,
    status,
    create_time,
    update_time
FROM biz_customer
WHERE deleted = 0
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

该查询适合普通分页列表。实际业务中通常还会叠加状态、关键词、组织、租户等过滤条件。

#### 按业务编号查询未删除数据

根据业务编号查询时，必须带上 `deleted = 0`，避免查到已删除数据。

```sql
SELECT
    id,
    customer_no,
    customer_name,
    mobile,
    status
FROM biz_customer
WHERE customer_no = 'CUST202605130001'
  AND deleted = 0
LIMIT 1;
```

如果 `customer_no` 是业务唯一编号，应配合唯一索引或联合唯一索引保证查询稳定性。

#### 查询已删除数据

后台回收站、数据恢复、审计排查等功能需要查询已删除数据。

```sql
SELECT
    id,
    customer_no,
    customer_name,
    mobile,
    status,
    delete_time,
    delete_by,
    delete_reason
FROM biz_customer
WHERE deleted = 1
ORDER BY delete_time DESC
LIMIT 20 OFFSET 0;
```

已删除数据查询通常不应开放给普通业务接口，应限制在管理员、审计员或数据治理角色下使用。

#### 查询指定时间前删除的数据

该查询常用于定期清理或归档前筛选。

```sql
SELECT
    id,
    customer_no,
    customer_name,
    delete_time
FROM biz_customer
WHERE deleted = 1
  AND delete_time < '2026-01-01 00:00:00'
ORDER BY delete_time ASC
LIMIT 1000;
```

对于大批量清理任务，不建议一次性查询和处理全部数据，应使用小批量循环处理，降低锁等待和主从复制压力。

#### 统计有效数据数量

业务统计默认应统计未删除数据。

```sql
SELECT
    COUNT(*) AS valid_count
FROM biz_customer
WHERE deleted = 0;
```

如果统计逻辑中没有显式过滤 `deleted = 0`，很容易导致已删除数据继续参与报表计算。

### 常用写入

软删除模型的写入操作主要包括新增、软删除、恢复和物理清理。除新增外，其余操作都应带上明确的状态条件，避免重复删除、重复恢复或误操作。

#### 新增数据

新增数据时，`deleted` 应始终写入默认未删除状态。可以依赖字段默认值，也可以由应用层显式写入。

```sql
INSERT INTO biz_customer (
    id,
    customer_no,
    customer_name,
    mobile,
    status,
    deleted,
    create_by,
    update_by
) VALUES (
    10001,
    'CUST202605130001',
    '杭州示例科技有限公司',
    '13800000000',
    1,
    0,
    1,
    1
);
```

建议应用层明确使用统一基础字段填充策略，避免部分表漏写默认值。

#### 软删除数据

软删除本质是更新操作。删除时应同时写入删除时间、删除人和删除原因。

```sql
UPDATE biz_customer
SET
    deleted = 1,
    delete_time = NOW(),
    delete_by = 1,
    delete_reason = '用户主动删除',
    update_time = NOW(),
    update_by = 1
WHERE id = 10001
  AND deleted = 0;
```

`WHERE deleted = 0` 用于保证只有未删除数据才能被删除。如果影响行数为 `0`，通常表示数据不存在或已经被删除。

#### 恢复数据

恢复操作应将删除标识还原，并清空删除审计字段。

```sql
UPDATE biz_customer
SET
    deleted = 0,
    delete_time = NULL,
    delete_by = NULL,
    delete_reason = NULL,
    update_time = NOW(),
    update_by = 1
WHERE id = 10001
  AND deleted = 1;
```

恢复前应检查业务唯一约束。如果该客户编号已被重新创建，恢复可能导致唯一键冲突或业务语义冲突。

#### 批量软删除

批量软删除适合后台批量操作，但必须限制批量规模，并记录操作人。

```sql
UPDATE biz_customer
SET
    deleted = 1,
    delete_time = NOW(),
    delete_by = 1,
    delete_reason = '后台批量删除',
    update_time = NOW(),
    update_by = 1
WHERE id IN (10001, 10002, 10003)
  AND deleted = 0;
```

批量删除不建议一次处理过多主键。对于大批量数据，应拆分为多个小批次，避免长事务和大范围锁影响线上业务。

#### 物理清理软删除数据

软删除不是永久保留的唯一手段。对于超过保留期限的数据，可以在完成归档或确认无恢复需求后进行物理清理。

```sql
DELETE FROM biz_customer
WHERE deleted = 1
  AND delete_time < '2025-01-01 00:00:00'
LIMIT 1000;
```

物理清理应由定时任务分批执行，并配合数据备份、归档表或审计策略使用。核心业务表不建议直接无条件清理软删除数据。

### 常见问题

软删除模型常见问题集中在查询遗漏条件、唯一索引冲突、数据膨胀和删除语义混乱。建模时需要提前统一规范，而不是等业务表增多后再逐表修复。

| 问题                             | 原因                                  | 建议                                                    |
| -------------------------------- | ------------------------------------- | ------------------------------------------------------- |
| 查询结果包含已删除数据           | SQL 忘记添加 `deleted = 0`            | 使用统一 Mapper、ORM 逻辑删除插件或 SQL 审查规范        |
| 删除后无法重新创建相同业务编号   | 业务唯一索引仍限制全表唯一            | 根据业务规则决定使用全表唯一或带删除版本的联合唯一      |
| 多次删除同一业务编号报唯一键冲突 | 使用 `(biz_no, deleted)` 作为唯一索引 | 增加 `delete_version`，未删除为 `0`，删除后写入唯一版本 |
| 数据量持续膨胀                   | 软删除数据长期不清理                  | 配合归档数据模型或定期物理清理策略                      |
| 误把业务状态当删除状态           | `status` 同时表达启用、停用、删除     | 删除状态独立使用 `deleted` 字段                         |
| 报表统计不准确                   | 已删除数据仍参与统计                  | 统计 SQL 默认过滤 `deleted = 0`                         |
| 恢复数据失败                     | 唯一键或业务状态冲突                  | 恢复前检查唯一约束、业务状态和关联数据                  |
| 删除审计信息不足                 | 只记录删除标识                        | 增加 `delete_time`、`delete_by`、`delete_reason`        |

软删除不应替代业务状态。`status = 2` 表示停用、冻结、禁用等业务含义；`deleted = 1` 表示数据已经从正常业务视图中移除。两者语义不同，应同时存在、各自独立。

### 总结

软删除模型是 MySQL 8 业务建模中最常用的数据生命周期模型之一。它通过 `deleted`、`delete_time`、`delete_by`、`delete_reason` 等字段，将删除操作从物理删除转为状态变更，从而提升数据恢复、审计追踪和业务安全性。

建模时应重点关注四点：第一，所有业务查询默认过滤 `deleted = 0`；第二，删除操作必须写入删除审计信息；第三，唯一索引要根据是否允许删除后重建来设计；第四，软删除数据不能无限增长，应结合归档模型或定期清理策略管理数据生命周期。



## 归档数据模型

归档数据模型是指将低频访问、已完结、已过保留期或不再参与主业务流程的数据，从在线业务表迁移到归档表、历史库或归档存储中。它的核心目标是降低主业务表的数据量，提升在线查询和写入性能，同时保留历史数据的追溯能力。

归档不是简单删除数据。归档数据仍然属于业务数据，只是访问频率、存储位置和查询入口发生变化。对于订单、支付、合同、审批、消息、流水、日志等数据量持续增长的业务表，归档模型通常比长期堆积在主表中更稳定。

### 适用场景

归档数据模型适用于数据已经完成主要业务流转，但仍然需要保留、查询、审计或对账的场景。它通常与软删除模型、冷热数据模型、分区表模型配合使用。

| 场景                   | 说明                                             |
| ---------------------- | ------------------------------------------------ |
| 主表数据量持续增长     | 订单、消息、日志、流水等表长期写入，主表膨胀明显 |
| 历史数据访问频率低     | 最近数据高频访问，历史数据偶尔查询               |
| 业务状态已完结         | 已完成、已取消、已关闭、已过期的数据可归档       |
| 数据需要长期保留       | 合同、支付、交易、审计类数据不能直接删除         |
| 在线查询性能下降       | 主表分页、统计、关联查询受到历史数据影响         |
| 需要降低备份和维护成本 | 主业务表过大导致备份、DDL、索引维护成本升高      |
| 需要按时间周期管理数据 | 按月、季度、年度归档历史数据                     |

不适合立即归档的数据包括仍在业务流转中的数据、仍会频繁更新的数据、仍参与在线实时统计的数据，以及存在大量未完成关联流程的数据。归档前必须确认数据已进入稳定状态。

### 建模结构

归档数据模型通常由在线业务表、归档表和归档任务记录表组成。在线业务表保存当前仍然高频访问的数据，归档表保存历史数据，归档任务记录表用于记录每次归档的范围、数量、执行结果和异常信息。

常见结构如下：

```sql
CREATE TABLE biz_order (
    id              BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    order_no        VARCHAR(64)     NOT NULL COMMENT '订单编号',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    order_status    TINYINT         NOT NULL COMMENT '订单状态：1待支付，2已支付，3已完成，4已取消，5已关闭',
    pay_amount      DECIMAL(18, 2)  NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    pay_time        DATETIME        DEFAULT NULL COMMENT '支付时间',
    finish_time     DATETIME        DEFAULT NULL COMMENT '完成时间',
    archived        TINYINT         NOT NULL DEFAULT 0 COMMENT '归档标识：0未归档，1已归档',
    archive_time    DATETIME        DEFAULT NULL COMMENT '归档时间',
    archive_batch_no VARCHAR(64)    DEFAULT NULL COMMENT '归档批次号',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '订单表';
```

归档表通常与主表保持核心业务字段一致，并额外保留来源表、来源主键、归档批次、归档时间等字段。这样可以支持历史查询、恢复、审计和问题排查。

```sql
CREATE TABLE biz_order_archive (
    id               BIGINT UNSIGNED NOT NULL COMMENT '归档表主键ID',
    source_id        BIGINT UNSIGNED NOT NULL COMMENT '来源订单ID',
    source_table     VARCHAR(64)     NOT NULL DEFAULT 'biz_order' COMMENT '来源表名',
    order_no         VARCHAR(64)     NOT NULL COMMENT '订单编号',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    order_status     TINYINT         NOT NULL COMMENT '订单状态：1待支付，2已支付，3已完成，4已取消，5已关闭',
    pay_amount       DECIMAL(18, 2)  NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    pay_time         DATETIME        DEFAULT NULL COMMENT '支付时间',
    finish_time      DATETIME        DEFAULT NULL COMMENT '完成时间',
    archive_time     DATETIME        NOT NULL COMMENT '归档时间',
    archive_batch_no VARCHAR(64)     NOT NULL COMMENT '归档批次号',
    create_time      DATETIME        NOT NULL COMMENT '原始创建时间',
    update_time      DATETIME        NOT NULL COMMENT '原始更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '订单归档表';
```

归档任务记录表用于保存归档执行过程，尤其适合定时任务、批处理任务和后台运维操作。

```sql
CREATE TABLE sys_archive_task_log (
    id                BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    archive_batch_no  VARCHAR(64)     NOT NULL COMMENT '归档批次号',
    source_table      VARCHAR(64)     NOT NULL COMMENT '来源表名',
    archive_table     VARCHAR(64)     NOT NULL COMMENT '归档表名',
    archive_condition VARCHAR(1000)   NOT NULL COMMENT '归档条件描述',
    total_count       INT             NOT NULL DEFAULT 0 COMMENT '计划归档数量',
    success_count     INT             NOT NULL DEFAULT 0 COMMENT '成功归档数量',
    fail_count        INT             NOT NULL DEFAULT 0 COMMENT '失败数量',
    task_status       TINYINT         NOT NULL DEFAULT 1 COMMENT '任务状态：1执行中，2成功，3失败，4部分成功',
    start_time        DATETIME        NOT NULL COMMENT '开始时间',
    end_time          DATETIME        DEFAULT NULL COMMENT '结束时间',
    error_message     VARCHAR(2000)   DEFAULT NULL COMMENT '错误信息',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '归档任务日志表';
```

归档表是否与主表完全同构，需要根据业务决定。若归档后只做历史查询，归档表可以只保留必要字段；若需要支持恢复到主表，则归档表应尽量保留主表完整字段。

### 字段设计

归档模型的字段设计需要同时考虑来源追踪、批次管理、查询过滤和恢复能力。主表侧通常只需要少量归档标识字段，归档表侧则需要更多来源和归档元信息。

主表建议字段如下：

| 字段名             | 类型          | 是否必填 | 默认值 | 说明                             |
| ------------------ | ------------- | -------- | ------ | -------------------------------- |
| `archived`         | `TINYINT`     | 是       | `0`    | 归档标识：`0` 未归档，`1` 已归档 |
| `archive_time`     | `DATETIME`    | 否       | `NULL` | 归档时间                         |
| `archive_batch_no` | `VARCHAR(64)` | 否       | `NULL` | 归档批次号                       |

归档表建议字段如下：

| 字段名             | 类型              | 是否必填   | 默认值     | 说明                                     |
| ------------------ | ----------------- | ---------- | ---------- | ---------------------------------------- |
| `id`               | `BIGINT UNSIGNED` | 是         | 无         | 归档表主键 ID                            |
| `source_id`        | `BIGINT UNSIGNED` | 是         | 无         | 来源表原主键 ID                          |
| `source_table`     | `VARCHAR(64)`     | 是         | 无         | 来源表名                                 |
| `archive_time`     | `DATETIME`        | 是         | 无         | 归档时间                                 |
| `archive_batch_no` | `VARCHAR(64)`     | 是         | 无         | 归档批次号                               |
| 原业务字段         | 与主表一致        | 按业务决定 | 按业务决定 | 保留历史查询和恢复所需字段               |
| 原审计字段         | 与主表一致        | 按业务决定 | 按业务决定 | 保留原始创建时间、更新时间、操作人等信息 |

字段设计建议如下：

| 设计项   | 建议                                                       |
| -------- | ---------------------------------------------------------- |
| 归档标识 | 使用 `archived`，不要复用 `deleted`                        |
| 归档时间 | 使用 `archive_time`，记录实际迁移时间                      |
| 归档批次 | 使用 `archive_batch_no`，便于追踪和回滚                    |
| 来源主键 | 使用 `source_id` 保存原主表 ID                             |
| 来源表名 | 多来源归档表建议保留 `source_table`                        |
| 原始时间 | 归档表应保留原始 `create_time` 和 `update_time`            |
| 主键策略 | 归档表可以使用新主键，也可以复用来源主键，但必须全系统统一 |
| 金额字段 | 金额类字段必须保持原精度，不应在归档时改变类型             |
| 状态字段 | 保留归档时的最终业务状态，避免后续语义丢失                 |

不建议只把归档数据导出为无结构文件后删除主表数据。对于仍可能被业务系统查询的数据，应优先使用归档表或归档库保存。

### 索引设计

归档模型的索引设计要区分在线表和归档表。在线表索引服务于高频业务查询和归档筛选，归档表索引服务于历史查询、审计查询、批次追踪和恢复操作。

在线表常见索引如下：

```sql
ALTER TABLE biz_order
    ADD UNIQUE KEY uk_order_no (order_no),
    ADD KEY idx_status_finish_time_archived (order_status, finish_time, archived),
    ADD KEY idx_archived_archive_time (archived, archive_time),
    ADD KEY idx_user_create_time_archived (user_id, create_time, archived);
```

归档表常见索引如下：

```sql
ALTER TABLE biz_order_archive
    ADD UNIQUE KEY uk_source_table_source_id (source_table, source_id),
    ADD KEY idx_order_no (order_no),
    ADD KEY idx_user_create_time (user_id, create_time),
    ADD KEY idx_archive_batch_no (archive_batch_no),
    ADD KEY idx_archive_time (archive_time);
```

归档任务日志表常见索引如下：

```sql
ALTER TABLE sys_archive_task_log
    ADD UNIQUE KEY uk_archive_batch_no (archive_batch_no),
    ADD KEY idx_source_table_start_time (source_table, start_time),
    ADD KEY idx_task_status_start_time (task_status, start_time);
```

索引设计建议如下：

| 表             | 索引重点                                         |
| -------------- | ------------------------------------------------ |
| 在线业务表     | 支持业务查询、归档条件筛选、按时间范围批量扫描   |
| 归档表         | 支持订单号、用户、原主键、归档批次、归档时间查询 |
| 归档任务日志表 | 支持按批次、状态、来源表和执行时间查询           |

归档表不应简单复制主表的全部索引。主表索引用于在线交易，归档表索引用于历史检索，两者查询模式不同。归档表索引过多会降低归档写入性能，也会增加存储成本。

### 常用查询

归档模型的查询需要明确区分“在线查询”和“历史查询”。默认业务接口应查询在线表；只有用户主动查询历史数据、后台审计、对账或恢复时，才查询归档表。

#### 查询在线未归档订单

普通业务列表通常只查询在线业务表，并过滤未归档数据。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    create_time
FROM biz_order
WHERE archived = 0
  AND user_id = 10001
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

该查询适合用户订单列表、后台近期订单列表等高频场景。归档后的历史订单不应默认混入在线查询，避免影响接口性能。

#### 查询可归档订单

归档任务执行前，需要先筛选满足归档条件的数据。通常会同时判断业务状态、完成时间和归档标识。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    finish_time
FROM biz_order
WHERE archived = 0
  AND order_status IN (3, 4, 5)
  AND finish_time < '2025-01-01 00:00:00'
ORDER BY finish_time ASC
LIMIT 1000;
```

该查询适合定时任务小批量扫描。归档任务不建议一次性读取过多数据，应按主键或时间范围分批执行。

#### 按订单号查询归档订单

历史详情查询、客服查询和审计查询通常会根据业务编号查询归档表。

```sql
SELECT
    id,
    source_id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    archive_time,
    archive_batch_no
FROM biz_order_archive
WHERE order_no = 'ORD202605130001'
LIMIT 1;
```

如果订单编号全局唯一，该查询可以快速定位历史订单。如果订单号不是全局唯一，应同时带上租户、用户或业务类型条件。

#### 按用户查询历史订单

用户历史订单查询通常访问归档表，并按原始创建时间倒序展示。

```sql
SELECT
    order_no,
    order_status,
    pay_amount,
    create_time,
    archive_time
FROM biz_order_archive
WHERE user_id = 10001
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

该查询适合“查看更早订单”“历史订单”这类低频入口，不建议与在线订单列表混在同一个默认查询中。

#### 同时查询在线和归档数据

部分场景需要同时查询在线表和归档表，例如全量订单搜索、客服工单查询、审计追踪等。

```sql
SELECT
    order_no,
    user_id,
    order_status,
    pay_amount,
    create_time,
    'ONLINE' AS data_location
FROM biz_order
WHERE order_no = 'ORD202605130001'

UNION ALL

SELECT
    order_no,
    user_id,
    order_status,
    pay_amount,
    create_time,
    'ARCHIVE' AS data_location
FROM biz_order_archive
WHERE order_no = 'ORD202605130001';
```

`UNION ALL` 不会去重，性能通常优于 `UNION`。如果业务能够先判断数据位置，应优先只查一个表，避免每次都同时访问在线表和归档表。

#### 查询归档批次执行结果

归档批次查询用于排查任务执行情况和追踪归档范围。

```sql
SELECT
    archive_batch_no,
    source_table,
    archive_table,
    total_count,
    success_count,
    fail_count,
    task_status,
    start_time,
    end_time,
    error_message
FROM sys_archive_task_log
WHERE archive_batch_no = 'ARCH202605130001'
LIMIT 1;
```

归档任务日志应保留足够长时间，便于排查数据是否已经归档、归档是否完整、失败原因是什么。

### 常用写入

归档模型的写入包括生成归档批次、插入归档表、更新主表归档标识、删除或保留主表数据、记录任务日志等步骤。生产环境中应使用事务、小批量、幂等控制和失败重试机制。

#### 创建归档任务记录

归档任务开始前，先写入任务日志，生成唯一归档批次号。

```sql
INSERT INTO sys_archive_task_log (
    id,
    archive_batch_no,
    source_table,
    archive_table,
    archive_condition,
    total_count,
    success_count,
    fail_count,
    task_status,
    start_time
) VALUES (
    90001,
    'ARCH202605130001',
    'biz_order',
    'biz_order_archive',
    'order_status IN (3,4,5) AND finish_time < 2025-01-01 00:00:00',
    0,
    0,
    0,
    1,
    NOW()
);
```

任务日志应先于实际归档数据写入。即使后续任务失败，也能留下执行痕迹。

#### 插入归档表

将满足条件的数据写入归档表。归档写入应具备幂等能力，避免任务重试时重复插入。

```sql
INSERT INTO biz_order_archive (
    id,
    source_id,
    source_table,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    archive_time,
    archive_batch_no,
    create_time,
    update_time
)
SELECT
    id,
    id AS source_id,
    'biz_order' AS source_table,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    NOW() AS archive_time,
    'ARCH202605130001' AS archive_batch_no,
    create_time,
    update_time
FROM biz_order
WHERE archived = 0
  AND order_status IN (3, 4, 5)
  AND finish_time < '2025-01-01 00:00:00'
ORDER BY finish_time ASC
LIMIT 1000;
```

如果归档表使用 `(source_table, source_id)` 唯一索引，重复执行同一批次时会触发唯一键冲突。可以根据业务选择直接失败、忽略重复或更新已有归档记录。

#### 幂等插入归档表

对于允许任务重试的场景，可以使用 `ON DUPLICATE KEY UPDATE` 保证归档写入幂等。

```sql
INSERT INTO biz_order_archive (
    id,
    source_id,
    source_table,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    archive_time,
    archive_batch_no,
    create_time,
    update_time
)
SELECT
    id,
    id AS source_id,
    'biz_order' AS source_table,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    NOW() AS archive_time,
    'ARCH202605130001' AS archive_batch_no,
    create_time,
    update_time
FROM biz_order
WHERE archived = 0
  AND order_status IN (3, 4, 5)
  AND finish_time < '2025-01-01 00:00:00'
ORDER BY finish_time ASC
LIMIT 1000
ON DUPLICATE KEY UPDATE
    archive_time = VALUES(archive_time),
    archive_batch_no = VALUES(archive_batch_no);
```

幂等归档适合定时任务失败重试，但要避免在归档后修改历史业务字段。归档数据一般应保持归档时快照，不应被后续任务随意覆盖。

#### 标记主表已归档

归档表插入成功后，可以将主表数据标记为已归档。

```sql
UPDATE biz_order
SET
    archived = 1,
    archive_time = NOW(),
    archive_batch_no = 'ARCH202605130001',
    update_time = NOW()
WHERE archived = 0
  AND order_status IN (3, 4, 5)
  AND finish_time < '2025-01-01 00:00:00'
ORDER BY finish_time ASC
LIMIT 1000;
```

如果归档后仍保留主表数据，可以通过 `archived = 1` 将其排除在默认在线查询之外。如果归档后需要物理删除主表数据，则应先确认归档表写入成功。

#### 删除已归档主表数据

当业务确认归档表已经完整保存历史数据后，可以分批删除主表中的已归档数据。

```sql
DELETE FROM biz_order
WHERE archived = 1
  AND archive_batch_no = 'ARCH202605130001'
LIMIT 1000;
```

物理删除必须谨慎执行。建议先使用查询确认待删除数量，再按批次删除，并保留归档任务日志和备份策略。

#### 更新归档任务结果

归档完成后，需要更新任务日志，记录成功数量、失败数量和任务状态。

```sql
UPDATE sys_archive_task_log
SET
    total_count = 1000,
    success_count = 1000,
    fail_count = 0,
    task_status = 2,
    end_time = NOW(),
    update_time = NOW()
WHERE archive_batch_no = 'ARCH202605130001';
```

如果归档失败，应记录错误信息，便于后续人工处理或任务重试。

```sql
UPDATE sys_archive_task_log
SET
    fail_count = 1000,
    task_status = 3,
    end_time = NOW(),
    error_message = '归档写入失败：Duplicate entry',
    update_time = NOW()
WHERE archive_batch_no = 'ARCH202605130001';
```

### 常见问题

归档数据模型常见问题主要集中在归档边界不清晰、任务不幂等、历史查询入口混乱、归档后恢复困难和主表删除风险过高。

| 问题                         | 原因                           | 建议                                                    |
| ---------------------------- | ------------------------------ | ------------------------------------------------------- |
| 归档了仍在流转的数据         | 只按时间判断，没有判断业务状态 | 归档条件必须同时包含业务终态和时间条件                  |
| 归档任务重复执行产生重复数据 | 归档表缺少来源唯一约束         | 使用 `(source_table, source_id)` 唯一索引并设计幂等写入 |
| 归档后业务查询查不到数据     | 查询入口只查主表               | 区分在线查询和历史查询，必要时提供联合查询入口          |
| 主表删除后无法恢复           | 归档表字段不完整               | 归档表保留恢复所需字段和原始审计字段                    |
| 归档任务执行时间过长         | 单批处理数据过多               | 按主键或时间范围小批量归档                              |
| 归档影响线上写入             | 大事务、大范围扫描或锁等待     | 控制批次大小，低峰执行，避免长事务                      |
| 历史查询性能差               | 归档表没有按查询模式建索引     | 根据订单号、用户、时间、批次设计归档表索引              |
| 归档表索引过多               | 直接复制主表所有索引           | 只保留历史查询和恢复需要的索引                          |
| 归档批次无法追踪             | 没有任务日志                   | 使用归档任务日志记录批次、范围、数量和状态              |
| 归档数据口径不一致           | 主表和归档表字段含义变化       | 建立字段版本规范，变更表结构时同步评估归档表            |

归档边界应优先由业务状态决定，时间条件只能作为辅助条件。例如订单必须是已完成、已取消、已关闭后，才允许根据完成时间归档。只按创建时间归档，容易误归档仍在处理中的数据。

### 总结

归档数据模型用于将低频历史数据从在线业务表迁移到归档表或归档库中，降低主表数据量和查询压力，同时保留历史追溯、审计和恢复能力。它适合订单、支付、合同、审批、消息、流水、日志等持续增长且存在历史保留要求的业务数据。

设计归档模型时，需要重点关注五点：第一，归档条件必须明确，通常由业务终态和时间范围共同决定；第二，归档表应保留来源主键、来源表名、归档时间和归档批次；第三，归档任务必须支持批次追踪和失败排查；第四，归档写入应具备幂等能力，避免重复归档；第五，归档后是否删除主表数据必须根据恢复要求、查询要求和合规要求谨慎决定。



## 冷热数据模型

冷热数据模型是指按照数据访问频率、业务时效性和存储成本，将数据划分为热数据、温数据和冷数据，并采用不同的表结构、查询入口、存储介质或清理策略进行管理。它的核心目标是让高频数据保持轻量、快速，让低频数据降低对在线业务的影响。

冷热数据模型与归档数据模型相似，但侧重点不同。归档数据模型更关注“数据是否已经退出主业务流程”，冷热数据模型更关注“数据访问频率是否发生变化”。有些数据虽然已经很老，但仍可能被频繁访问，这类数据不应简单进入冷区；有些数据虽然未删除，也未归档，但访问频率很低，可以从热区迁移到冷区。

### 适用场景

冷热数据模型适用于数据量大、访问频率差异明显、近期数据高频访问、历史数据低频访问的业务表。它常见于订单、支付流水、账户流水、库存流水、消息通知、操作日志、接口调用日志、账单、报表明细、设备上报数据等场景。

| 场景                 | 说明                                               |
| -------------------- | -------------------------------------------------- |
| 最近数据访问频繁     | 最近 7 天、30 天或 90 天数据经常被查询和更新       |
| 历史数据访问较少     | 半年前或一年前的数据通常只在审计、客服、对账时查询 |
| 主表数据增长过快     | 长期堆积导致分页、统计、备份和索引维护成本升高     |
| 查询明显具有时间窗口 | 大多数业务查询都按最近时间范围查询                 |
| 数据仍需在线查询     | 冷数据不能直接删除或离线保存，仍需要低频查询入口   |
| 业务存在分层存储需求 | 热数据放在线库，冷数据放历史库、低成本实例或归档库 |
| 需要降低在线库压力   | 将低频历史数据迁移出高并发核心表                   |

不适合使用冷热数据模型的场景包括数据量很小、历史数据访问频率与近期数据差异不明显、业务查询完全随机、不具备明确冷热划分规则的数据表。冷热拆分会增加查询路由和数据迁移复杂度，如果数据规模不足，不建议过早引入。

### 建模结构

冷热数据模型通常由热数据表、冷数据表和冷热迁移任务记录表组成。热数据表服务于高频在线业务，冷数据表服务于历史查询、审计查询和低频查询，迁移任务记录表用于记录每次冷热迁移的范围、数量、状态和异常信息。

热数据表保存近期、高频、仍可能频繁更新的数据。

```sql
CREATE TABLE biz_order_hot (
    id               BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    order_no         VARCHAR(64)     NOT NULL COMMENT '订单编号',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    order_status     TINYINT         NOT NULL COMMENT '订单状态：1待支付，2已支付，3已完成，4已取消，5已关闭',
    pay_amount       DECIMAL(18, 2)  NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    pay_time         DATETIME        DEFAULT NULL COMMENT '支付时间',
    finish_time      DATETIME        DEFAULT NULL COMMENT '完成时间',
    data_temperature TINYINT         NOT NULL DEFAULT 1 COMMENT '数据温度：1热数据，2温数据，3冷数据',
    cold_time        DATETIME        DEFAULT NULL COMMENT '转冷时间',
    cold_batch_no    VARCHAR(64)     DEFAULT NULL COMMENT '转冷批次号',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '订单热数据表';
```

冷数据表保存历史、低频、通常不再更新的数据。冷表可以与热表字段基本一致，也可以额外保留来源表、来源主键、转冷批次和转冷时间。

```sql
CREATE TABLE biz_order_cold (
    id               BIGINT UNSIGNED NOT NULL COMMENT '冷数据表主键ID',
    source_id        BIGINT UNSIGNED NOT NULL COMMENT '来源热表主键ID',
    source_table     VARCHAR(64)     NOT NULL DEFAULT 'biz_order_hot' COMMENT '来源表名',
    order_no         VARCHAR(64)     NOT NULL COMMENT '订单编号',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    order_status     TINYINT         NOT NULL COMMENT '订单状态：1待支付，2已支付，3已完成，4已取消，5已关闭',
    pay_amount       DECIMAL(18, 2)  NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    pay_time         DATETIME        DEFAULT NULL COMMENT '支付时间',
    finish_time      DATETIME        DEFAULT NULL COMMENT '完成时间',
    data_temperature TINYINT         NOT NULL DEFAULT 3 COMMENT '数据温度：1热数据，2温数据，3冷数据',
    cold_time        DATETIME        NOT NULL COMMENT '转冷时间',
    cold_batch_no    VARCHAR(64)     NOT NULL COMMENT '转冷批次号',
    create_time      DATETIME        NOT NULL COMMENT '原始创建时间',
    update_time      DATETIME        NOT NULL COMMENT '原始更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '订单冷数据表';
```

冷热迁移任务记录表用于追踪冷热迁移执行过程。对于生产环境，冷热迁移通常由定时任务、批处理任务或数据治理平台执行，任务记录表可以帮助排查迁移是否完整、是否重复、是否失败。

```sql
CREATE TABLE sys_cold_data_task_log (
    id               BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    cold_batch_no    VARCHAR(64)     NOT NULL COMMENT '转冷批次号',
    source_table     VARCHAR(64)     NOT NULL COMMENT '来源热表名',
    target_table     VARCHAR(64)     NOT NULL COMMENT '目标冷表名',
    cold_condition   VARCHAR(1000)   NOT NULL COMMENT '转冷条件描述',
    total_count      INT             NOT NULL DEFAULT 0 COMMENT '计划转冷数量',
    success_count    INT             NOT NULL DEFAULT 0 COMMENT '成功转冷数量',
    fail_count       INT             NOT NULL DEFAULT 0 COMMENT '失败数量',
    task_status      TINYINT         NOT NULL DEFAULT 1 COMMENT '任务状态：1执行中，2成功，3失败，4部分成功',
    start_time       DATETIME        NOT NULL COMMENT '开始时间',
    end_time         DATETIME        DEFAULT NULL COMMENT '结束时间',
    error_message    VARCHAR(2000)   DEFAULT NULL COMMENT '错误信息',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '冷热迁移任务日志表';
```

冷热数据模型可以有三种常见落地方式。第一种是单表冷热标记，即在同一张表中使用 `data_temperature` 区分冷热；第二种是热表和冷表拆分，即近期数据在热表，历史数据在冷表；第三种是热库和冷库拆分，即热数据保存在在线库，冷数据保存在历史库或低成本实例。业务系统中最常见、最容易控制的是热表和冷表拆分。

### 字段设计

冷热数据模型的字段设计需要明确数据温度、转冷时间、转冷批次和来源关系。字段设计越统一，后续迁移任务、查询路由和问题排查越容易标准化。

热表建议字段如下：

| 字段名             | 类型          | 是否必填 | 默认值 | 说明                                         |
| ------------------ | ------------- | -------- | ------ | -------------------------------------------- |
| `data_temperature` | `TINYINT`     | 是       | `1`    | 数据温度：`1` 热数据，`2` 温数据，`3` 冷数据 |
| `cold_time`        | `DATETIME`    | 否       | `NULL` | 转冷时间                                     |
| `cold_batch_no`    | `VARCHAR(64)` | 否       | `NULL` | 转冷批次号                                   |

冷表建议字段如下：

| 字段名             | 类型              | 是否必填   | 默认值     | 说明                                     |
| ------------------ | ----------------- | ---------- | ---------- | ---------------------------------------- |
| `id`               | `BIGINT UNSIGNED` | 是         | 无         | 冷表主键 ID                              |
| `source_id`        | `BIGINT UNSIGNED` | 是         | 无         | 来源热表主键 ID                          |
| `source_table`     | `VARCHAR(64)`     | 是         | 无         | 来源热表名                               |
| `data_temperature` | `TINYINT`         | 是         | `3`        | 数据温度，冷表中通常固定为 `3`           |
| `cold_time`        | `DATETIME`        | 是         | 无         | 转冷时间                                 |
| `cold_batch_no`    | `VARCHAR(64)`     | 是         | 无         | 转冷批次号                               |
| 原业务字段         | 与热表一致        | 按业务决定 | 按业务决定 | 保留历史查询、审计和恢复所需字段         |
| 原审计字段         | 与热表一致        | 按业务决定 | 按业务决定 | 保留原始创建时间、更新时间、操作人等信息 |

字段设计建议如下：

| 设计项   | 建议                                                |
| -------- | --------------------------------------------------- |
| 数据温度 | 使用 `data_temperature`，不要用业务状态字段表达冷热 |
| 热数据值 | 固定为 `1`                                          |
| 温数据值 | 可选，固定为 `2`，适合过渡层或低频在线层            |
| 冷数据值 | 固定为 `3`                                          |
| 转冷时间 | 使用 `cold_time`，记录实际迁移时间                  |
| 转冷批次 | 使用 `cold_batch_no`，便于追踪和回滚                |
| 来源主键 | 使用 `source_id` 保存热表原主键                     |
| 来源表名 | 使用 `source_table` 记录数据来源                    |
| 原始时间 | 冷表保留原始 `create_time` 和 `update_time`         |
| 金额字段 | 冷热表金额字段类型和精度必须一致                    |
| 状态字段 | 冷表保留转冷时的业务状态                            |

`data_temperature` 不应替代 `order_status`、`deleted`、`archived` 等字段。业务状态描述业务流程，删除标识描述数据是否可见，归档标识描述数据是否进入归档周期，数据温度描述访问频率和存储层级，四者语义不同。

### 索引设计

冷热数据模型的索引需要分别服务于热数据高频查询、冷数据低频查询和冷热迁移任务。热表索引应尽量精简，优先服务在线业务；冷表索引应围绕历史查询、审计查询和来源追踪设计。

热表常见索引如下：

```sql
ALTER TABLE biz_order_hot
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_order_no (order_no),
    ADD KEY idx_user_create_time_temperature (user_id, create_time, data_temperature),
    ADD KEY idx_status_finish_time_temperature (order_status, finish_time, data_temperature),
    ADD KEY idx_temperature_cold_time (data_temperature, cold_time);
```

冷表常见索引如下：

```sql
ALTER TABLE biz_order_cold
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_source_table_source_id (source_table, source_id),
    ADD KEY idx_order_no (order_no),
    ADD KEY idx_user_create_time (user_id, create_time),
    ADD KEY idx_cold_batch_no (cold_batch_no),
    ADD KEY idx_cold_time (cold_time);
```

冷热迁移任务日志表常见索引如下：

```sql
ALTER TABLE sys_cold_data_task_log
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_cold_batch_no (cold_batch_no),
    ADD KEY idx_source_table_start_time (source_table, start_time),
    ADD KEY idx_task_status_start_time (task_status, start_time);
```

索引设计建议如下：

| 表         | 索引重点                                             |
| ---------- | ---------------------------------------------------- |
| 热数据表   | 高频用户查询、状态查询、最近时间范围查询、转冷筛选   |
| 冷数据表   | 订单号查询、用户历史查询、来源主键追踪、转冷批次查询 |
| 任务日志表 | 批次查询、任务状态查询、来源表查询、执行时间查询     |

不建议在热表上建立过多历史查询索引。热表的目标是承载高频在线业务，索引越多，写入和更新成本越高。历史查询应尽量落到冷表或专门的历史查询入口中。

### 常用查询

冷热数据模型的查询重点是路由清晰。普通在线业务默认查热表，历史查询查冷表，全量查询根据业务需要同时查热表和冷表。查询入口不应混乱，否则会抵消冷热拆分带来的性能收益。

#### 查询热数据列表

用户近期订单、后台近期订单和在线业务查询应优先查询热表。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    create_time
FROM biz_order_hot
WHERE data_temperature = 1
  AND user_id = 10001
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

该查询适合默认列表页。一般情况下，用户打开订单列表时只展示近期热数据，历史数据通过“查看更多历史订单”入口单独查询。

#### 按订单号查询热数据

订单详情页可以优先查询热表。如果订单号属于近期订单，大多数情况下能够直接命中热表。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    create_time
FROM biz_order_hot
WHERE order_no = 'ORD202605130001'
LIMIT 1;
```

如果热表查询不到，再根据业务规则决定是否查询冷表。高频接口不建议每次都无条件查询冷热两张表。

#### 查询冷数据列表

历史订单、审计查询、客服查询等低频入口可以查询冷表。

```sql
SELECT
    id,
    source_id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    create_time,
    cold_time
FROM biz_order_cold
WHERE user_id = 10001
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

冷数据查询一般允许响应时间略高于热数据查询，但仍应建立必要索引，避免冷表变成新的性能瓶颈。

#### 按订单号查询冷数据

当热表查询不到订单，或者用户明确查询历史订单时，可以访问冷表。

```sql
SELECT
    id,
    source_id,
    source_table,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    cold_time,
    cold_batch_no
FROM biz_order_cold
WHERE order_no = 'ORD202401010001'
LIMIT 1;
```

订单号查询通常需要在热表和冷表都建立索引。若订单号全局唯一，可以直接根据订单号定位；若订单号不是全局唯一，应增加租户、用户或业务类型条件。

#### 同时查询热数据和冷数据

客服、审计、全量搜索等场景可能需要同时查询热表和冷表。

```sql
SELECT
    order_no,
    user_id,
    order_status,
    pay_amount,
    create_time,
    'HOT' AS data_location
FROM biz_order_hot
WHERE user_id = 10001

UNION ALL

SELECT
    order_no,
    user_id,
    order_status,
    pay_amount,
    create_time,
    'COLD' AS data_location
FROM biz_order_cold
WHERE user_id = 10001

ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

`UNION ALL` 适合全量历史查询，但不适合作为普通业务默认查询。默认查询应优先走热表，只有用户明确查询历史数据时才访问冷表。

#### 查询待转冷数据

冷热迁移任务需要筛选满足转冷条件的数据。常见条件包括业务终态、完成时间、创建时间和数据温度。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    finish_time,
    create_time
FROM biz_order_hot
WHERE data_temperature = 1
  AND order_status IN (3, 4, 5)
  AND finish_time < '2025-11-13 00:00:00'
ORDER BY finish_time ASC
LIMIT 1000;
```

待转冷数据必须是业务稳定数据。对于订单类数据，一般需要满足已完成、已取消或已关闭等终态条件，不能只按创建时间判断。

#### 查询转冷批次结果

通过转冷批次号可以查看某次冷热迁移的执行情况。

```sql
SELECT
    cold_batch_no,
    source_table,
    target_table,
    cold_condition,
    total_count,
    success_count,
    fail_count,
    task_status,
    start_time,
    end_time,
    error_message
FROM sys_cold_data_task_log
WHERE cold_batch_no = 'COLD202605130001'
LIMIT 1;
```

批次查询主要用于任务排查、运维审计和失败重试。生产系统中应保留冷热迁移任务日志，不能只依赖定时任务日志文件。

### 常用写入

冷热数据模型的写入包括新增热数据、转冷写入、标记转冷、删除热表数据、冷数据回热和任务日志更新。生产环境中必须保证迁移过程可追踪、可重试、可校验。

#### 新增热数据

新创建的业务数据通常进入热表，数据温度默认为热数据。

```sql
INSERT INTO biz_order_hot (
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    data_temperature
) VALUES (
    10001,
    'ORD202605130001',
    10001,
    1,
    299.00,
    1
);
```

新增数据不应直接进入冷表。即使业务预期数据访问频率较低，也建议先进入统一在线写入链路，再通过规则转冷。

#### 创建转冷任务记录

冷热迁移开始前，应先写入任务记录，生成唯一转冷批次号。

```sql
INSERT INTO sys_cold_data_task_log (
    id,
    cold_batch_no,
    source_table,
    target_table,
    cold_condition,
    total_count,
    success_count,
    fail_count,
    task_status,
    start_time
) VALUES (
    80001,
    'COLD202605130001',
    'biz_order_hot',
    'biz_order_cold',
    'order_status IN (3,4,5) AND finish_time < 2025-11-13 00:00:00',
    0,
    0,
    0,
    1,
    NOW()
);
```

任务记录应先创建，后执行数据迁移。这样即使中途失败，也能留下可排查的执行痕迹。

#### 插入冷数据表

将满足条件的热数据复制到冷表。冷表应通过来源唯一约束避免重复转冷。

```sql
INSERT INTO biz_order_cold (
    id,
    source_id,
    source_table,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    data_temperature,
    cold_time,
    cold_batch_no,
    create_time,
    update_time
)
SELECT
    id,
    id AS source_id,
    'biz_order_hot' AS source_table,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    3 AS data_temperature,
    NOW() AS cold_time,
    'COLD202605130001' AS cold_batch_no,
    create_time,
    update_time
FROM biz_order_hot
WHERE data_temperature = 1
  AND order_status IN (3, 4, 5)
  AND finish_time < '2025-11-13 00:00:00'
ORDER BY finish_time ASC
LIMIT 1000;
```

该写入适合小批量迁移。单批数据量不宜过大，避免长事务、锁等待和主从复制延迟。

#### 幂等写入冷数据表

对于允许任务失败重试的场景，可以使用 `ON DUPLICATE KEY UPDATE` 做幂等控制。

```sql
INSERT INTO biz_order_cold (
    id,
    source_id,
    source_table,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    data_temperature,
    cold_time,
    cold_batch_no,
    create_time,
    update_time
)
SELECT
    id,
    id AS source_id,
    'biz_order_hot' AS source_table,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    3 AS data_temperature,
    NOW() AS cold_time,
    'COLD202605130001' AS cold_batch_no,
    create_time,
    update_time
FROM biz_order_hot
WHERE data_temperature = 1
  AND order_status IN (3, 4, 5)
  AND finish_time < '2025-11-13 00:00:00'
ORDER BY finish_time ASC
LIMIT 1000
ON DUPLICATE KEY UPDATE
    cold_time = VALUES(cold_time),
    cold_batch_no = VALUES(cold_batch_no);
```

幂等写入可以降低任务重试风险，但不建议反复覆盖冷表中的业务字段。冷数据应尽量保持转冷时的历史快照。

#### 标记热表数据已转冷

冷表写入成功后，可以将热表数据标记为冷数据，或者直接删除热表数据。若系统需要短期双写校验，可以先标记再延迟删除。

```sql
UPDATE biz_order_hot
SET
    data_temperature = 3,
    cold_time = NOW(),
    cold_batch_no = 'COLD202605130001',
    update_time = NOW()
WHERE data_temperature = 1
  AND order_status IN (3, 4, 5)
  AND finish_time < '2025-11-13 00:00:00'
ORDER BY finish_time ASC
LIMIT 1000;
```

标记转冷适合需要灰度验证的场景。业务查询可以通过 `data_temperature = 1` 过滤热数据，避免已转冷数据继续参与默认在线查询。

#### 删除热表已转冷数据

确认冷表数据完整后，可以从热表删除已转冷数据，降低热表数据量。

```sql
DELETE FROM biz_order_hot
WHERE data_temperature = 3
  AND cold_batch_no = 'COLD202605130001'
LIMIT 1000;
```

物理删除热表数据前，应先确认冷表记录数量、来源主键、核心金额字段和关键业务状态一致。核心业务表建议保留备份或延迟删除窗口。

#### 冷数据回热

部分业务可能需要将冷数据重新迁回热表，例如历史订单重新打开、历史工单重新处理、历史合同重新履约等。

```sql
INSERT INTO biz_order_hot (
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    data_temperature,
    cold_time,
    cold_batch_no,
    create_time,
    update_time
)
SELECT
    source_id AS id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    pay_time,
    finish_time,
    1 AS data_temperature,
    NULL AS cold_time,
    NULL AS cold_batch_no,
    create_time,
    NOW() AS update_time
FROM biz_order_cold
WHERE order_no = 'ORD202401010001'
LIMIT 1;
```

冷数据回热前必须检查热表是否已经存在同一业务数据，避免重复写入或唯一键冲突。回热后是否删除冷表数据，应根据业务审计要求决定。

#### 更新转冷任务结果

冷热迁移完成后，需要更新任务日志，记录执行结果。

```sql
UPDATE sys_cold_data_task_log
SET
    total_count = 1000,
    success_count = 1000,
    fail_count = 0,
    task_status = 2,
    end_time = NOW(),
    update_time = NOW()
WHERE cold_batch_no = 'COLD202605130001';
```

执行失败时，应记录失败状态和错误信息。

```sql
UPDATE sys_cold_data_task_log
SET
    fail_count = 1000,
    task_status = 3,
    end_time = NOW(),
    error_message = '冷数据写入失败：Duplicate entry',
    update_time = NOW()
WHERE cold_batch_no = 'COLD202605130001';
```

任务结果更新是冷热迁移闭环的一部分。没有任务结果记录，后续很难判断数据是否已经完成迁移。

### 常见问题

冷热数据模型常见问题主要集中在冷热边界不清晰、查询路由混乱、迁移任务不幂等、冷表索引不足和回热机制缺失。

| 问题                   | 原因                             | 建议                                      |
| ---------------------- | -------------------------------- | ----------------------------------------- |
| 热表仍然很大           | 转冷规则过于保守或任务未持续执行 | 明确转冷周期，使用定时任务分批迁移        |
| 冷数据被误迁移         | 只按时间判断，没有判断业务状态   | 转冷条件必须同时包含业务终态和时间条件    |
| 普通查询变慢           | 默认查询同时查热表和冷表         | 默认查询只查热表，历史查询单独入口        |
| 冷表查询很慢           | 冷表没有按历史查询模式建立索引   | 针对订单号、用户、时间、来源主键建立索引  |
| 转冷任务重复写入       | 冷表缺少来源唯一约束             | 使用 `(source_table, source_id)` 唯一约束 |
| 转冷任务失败后难以恢复 | 没有批次号和任务日志             | 使用 `cold_batch_no` 和任务日志记录全过程 |
| 回热时出现唯一键冲突   | 热表已存在相同业务数据           | 回热前检查订单号、来源主键和业务状态      |
| 冷热数据不一致         | 迁移后未校验数量和核心字段       | 转冷后校验记录数、金额、状态和来源主键    |
| 热表删除风险高         | 冷表未确认完整就删除热表         | 先复制、再校验、再标记、最后分批删除      |
| 冷热语义与归档语义混乱 | 把冷数据等同于归档数据           | 冷热描述访问频率，归档描述生命周期阶段    |

冷热边界不应只由时间决定。更稳妥的规则是“业务终态 + 时间窗口 + 访问频率”。例如订单完成超过 180 天且近 90 天无访问记录，才允许转冷。对于仍在处理中或近期频繁访问的数据，即使创建时间较早，也不应转冷。

### 总结

冷热数据模型通过热表、冷表和迁移任务，将高频在线数据与低频历史数据分层管理。热数据服务于核心业务链路，冷数据服务于历史查询、审计、客服和对账场景。该模型适合订单、流水、消息、日志、账单等数据持续增长且访问频率明显分化的业务。

设计冷热数据模型时，需要重点关注五点：第一，冷热边界要由业务状态、时间范围和访问频率共同决定；第二，普通业务查询默认只访问热表，历史查询单独访问冷表；第三，冷表需要保留来源主键、来源表名、转冷时间和转冷批次；第四，冷热迁移任务必须支持批次追踪、幂等写入和失败重试；第五，删除热表数据前必须完成冷表校验，避免历史数据丢失。