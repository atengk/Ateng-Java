# 大数据量架构模型

## 分区表模型

分区表模型用于处理单表数据量持续增长、按时间或业务范围访问特征明显的业务表。MySQL 8 的分区表本质上仍然是一张逻辑表，底层按照分区规则将数据拆分到多个物理分区中，业务访问表名不变，主要依靠分区裁剪减少扫描范围。

### 适用场景

分区表适合数据量较大、访问条件具有明显分区键特征的业务场景。常见分区键包括创建时间、业务日期、租户编号、区域编号、订单编号范围等。

适合使用分区表的场景包括：

- 单表数据量较大，例如千万级、亿级以上，并且数据仍在持续增长。
- 查询大多会带上时间范围、业务日期、租户编号等稳定过滤条件。
- 数据生命周期明确，例如按月保留、按季度归档、按年清理。
- 历史数据查询频率低，近期数据查询频率高。
- 需要按分区快速清理历史数据，避免大批量 `DELETE` 造成长事务和锁压力。
- 业务仍希望保持单表访问形态，不希望过早引入分库分表中间件或路由层。

不适合使用分区表的场景包括：

- 查询条件无法命中分区键，经常需要全表扫描。
- 数据分布严重不均，例如某个分区长期占用绝大多数数据。
- 业务需要跨库水平扩展写入能力，此时应优先考虑分库分表。
- 表中唯一约束无法包含分区键，导致约束设计受限。
- 高频点查完全依赖主键或唯一键，但主键设计无法结合分区键。

### 建模结构

分区表建模时，应先确定业务主表、分区键、分区粒度和数据生命周期。建模结构只描述表的业务字段、分区方式和分区维护策略，索引设计应放在后续“索引设计”章节中统一说明。

以订单日志表为例，订单日志通常数据量较大，查询多按时间范围、租户、订单号、状态筛选，并且历史数据可以按月归档或删除，因此适合按创建时间进行范围分区。

分区表的核心结构包括：

| 结构项   | 说明                                                  |
| -------- | ----------------------------------------------------- |
| 逻辑表   | 业务仍访问同一张表，例如 `biz_order_log`              |
| 分区键   | 用于决定数据落入哪个分区，例如 `created_at`           |
| 分区方式 | 常用 `RANGE COLUMNS`、`RANGE`、`LIST COLUMNS`、`HASH` |
| 分区粒度 | 按月、按季度、按年、按租户、按区域等                  |
| 生命周期 | 定义保留周期、归档周期、清理周期                      |
| 分区维护 | 定期新增未来分区、清理历史分区                        |

订单日志分区表示例结构如下。

```sql
CREATE TABLE biz_order_log (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    log_level VARCHAR(16) NOT NULL COMMENT '日志级别',
    status VARCHAR(32) NOT NULL COMMENT '处理状态',
    content JSON NULL COMMENT '日志内容',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id, created_at)
) COMMENT='订单日志分区表'
PARTITION BY RANGE COLUMNS (created_at) (
    PARTITION p202601 VALUES LESS THAN ('2026-02-01'),
    PARTITION p202602 VALUES LESS THAN ('2026-03-01'),
    PARTITION p202603 VALUES LESS THAN ('2026-04-01'),
    PARTITION pmax VALUES LESS THAN (MAXVALUE)
);
```

该结构采用 `created_at` 作为分区键，按月进行范围分区。`pmax` 分区用于兜底，避免未来分区未及时创建时写入失败，但生产环境不应长期依赖 `pmax`，应通过定时任务提前创建未来分区。

### 字段设计

分区表字段设计的重点是让分区键稳定、不可频繁变更，并且让业务查询条件能够自然包含分区键。分区键一旦选定，后续调整成本较高，因此应优先选择业务上稳定、查询频率高、生命周期明确的字段。

推荐字段设计如下：

| 字段         | 类型          | 是否必填 | 说明                                             |
| ------------ | ------------- | -------- | ------------------------------------------------ |
| `id`         | `BIGINT`      | 是       | 主键ID，建议使用雪花ID、号段ID或业务统一ID生成器 |
| `tenant_id`  | `BIGINT`      | 是       | 租户ID，多租户场景常用过滤字段                   |
| `order_no`   | `VARCHAR(64)` | 是       | 订单编号，业务查询常用字段                       |
| `biz_type`   | `VARCHAR(32)` | 是       | 业务类型，用于区分不同日志来源                   |
| `log_level`  | `VARCHAR(16)` | 是       | 日志级别，例如 `INFO`、`WARN`、`ERROR`           |
| `status`     | `VARCHAR(32)` | 是       | 处理状态，例如 `SUCCESS`、`FAILED`、`PENDING`    |
| `content`    | `JSON`        | 否       | 扩展内容，适合存放低频查询的日志明细             |
| `created_at` | `DATETIME`    | 是       | 创建时间，同时作为分区键                         |
| `updated_at` | `DATETIME`    | 是       | 更新时间                                         |

字段设计建议：

- 分区键字段必须稳定，避免频繁更新分区键。
- 分区键应尽量使用 `NOT NULL`，避免空值数据落入非预期分区。
- 时间分区建议使用 `DATETIME` 或 `DATE`，并统一业务时区。
- 大文本、JSON、扩展内容字段不应作为高频过滤条件。
- 如果表中存在唯一键，MySQL 分区表要求所有唯一键必须包含分区表达式中使用的字段。
- 主键通常需要包含分区键，例如 `PRIMARY KEY (id, created_at)`，否则在 MySQL 分区表中可能无法通过约束校验。

### 索引设计

分区表索引设计需要同时考虑分区裁剪和分区内查询效率。分区裁剪依赖分区键，索引则负责在命中的分区内部继续减少扫描范围。

推荐索引设计如下。

```sql
ALTER TABLE biz_order_log
    ADD INDEX idx_tenant_created (tenant_id, created_at),
    ADD INDEX idx_order_created (order_no, created_at),
    ADD INDEX idx_status_created (status, created_at),
    ADD INDEX idx_biz_type_created (biz_type, created_at);
```

索引设计说明：

| 索引                   | 适用查询             | 说明                       |
| ---------------------- | -------------------- | -------------------------- |
| `idx_tenant_created`   | 按租户和时间查询     | 多租户业务最常见查询路径   |
| `idx_order_created`    | 按订单号和时间查询   | 适合订单详情、订单日志追踪 |
| `idx_status_created`   | 按状态和时间查询     | 适合失败任务、异常日志检索 |
| `idx_biz_type_created` | 按业务类型和时间查询 | 适合按业务模块分析日志     |

索引设计建议：

- 高频查询条件应尽量带上分区键。
- 联合索引中应根据过滤选择性和排序需求安排字段顺序。
- 不要因为分区表而忽略索引，分区只能减少分区扫描范围，不能替代索引。
- 不建议创建过多低选择性索引，例如单独对 `status` 建索引通常收益有限。
- 对于按时间倒序分页的场景，可以考虑 `(tenant_id, created_at, id)` 这类联合索引。
- 如果查询长期不带分区键，即使有索引，也可能扫描多个分区，性能不一定稳定。

### 常用查询

常用查询应尽量显式带上分区键范围，让 MySQL 能够进行分区裁剪。对于按时间分区的表，查询条件中应包含 `created_at` 的范围条件。

#### 按时间范围查询

该查询适合查看某个时间段内的业务日志，是按时间分区表最基础的查询方式。

```sql
SELECT
    id,
    tenant_id,
    order_no,
    biz_type,
    log_level,
    status,
    created_at
FROM biz_order_log
WHERE created_at >= '2026-03-01 00:00:00'
  AND created_at < '2026-04-01 00:00:00'
ORDER BY created_at DESC
LIMIT 100;
```

该查询可以命中 `p202603` 分区，避免扫描全部历史数据。

#### 按租户和时间查询

该查询适合多租户系统中查看某个租户在指定时间范围内的业务日志。

```sql
SELECT
    id,
    order_no,
    biz_type,
    log_level,
    status,
    created_at
FROM biz_order_log
WHERE tenant_id = 10001
  AND created_at >= '2026-03-01 00:00:00'
  AND created_at < '2026-04-01 00:00:00'
ORDER BY created_at DESC
LIMIT 100;
```

该查询既可以利用分区裁剪，也可以利用 `idx_tenant_created` 在分区内快速过滤。

#### 按订单号和时间查询

该查询适合订单详情页、订单轨迹页、售后排查页等场景。

```sql
SELECT
    id,
    tenant_id,
    order_no,
    biz_type,
    log_level,
    status,
    content,
    created_at
FROM biz_order_log
WHERE order_no = 'ORD202603010001'
  AND created_at >= '2026-03-01 00:00:00'
  AND created_at < '2026-04-01 00:00:00'
ORDER BY created_at ASC;
```

即使订单号本身选择性较高，也建议带上时间范围，避免跨多个分区查找。

#### 按状态查询异常数据

该查询适合查看某个时间段内失败、异常或待处理的数据。

```sql
SELECT
    id,
    tenant_id,
    order_no,
    biz_type,
    status,
    created_at
FROM biz_order_log
WHERE status = 'FAILED'
  AND created_at >= '2026-03-01 00:00:00'
  AND created_at < '2026-03-08 00:00:00'
ORDER BY created_at DESC
LIMIT 200;
```

该查询适合配合 `idx_status_created` 使用。对于状态值较少的字段，应避免单独建索引，通常需要和时间字段组成联合索引。

#### 分页查询

该查询适合后台列表页。大数据量场景不建议使用过深的 `OFFSET` 分页，应优先使用游标分页。

```sql
SELECT
    id,
    tenant_id,
    order_no,
    biz_type,
    log_level,
    status,
    created_at
FROM biz_order_log
WHERE tenant_id = 10001
  AND created_at >= '2026-03-01 00:00:00'
  AND created_at < '2026-04-01 00:00:00'
  AND id < 1888888888888888888
ORDER BY id DESC
LIMIT 100;
```

游标分页通过上一次查询的最小 `id` 继续向后翻页，避免深分页带来的扫描和排序压力。

#### 查看分区命中情况

该查询用于检查 SQL 是否进行了分区裁剪。

```sql
EXPLAIN
SELECT
    id,
    tenant_id,
    order_no,
    created_at
FROM biz_order_log
WHERE tenant_id = 10001
  AND created_at >= '2026-03-01 00:00:00'
  AND created_at < '2026-04-01 00:00:00';
```

执行后重点查看 `partitions` 字段。如果只显示目标月份分区，说明分区裁剪生效；如果显示多个分区或全部分区，需要检查查询条件是否正确使用了分区键。

### 常用写入

分区表写入和普通表写入基本一致，业务侧仍然写入同一个逻辑表。需要注意的是，写入数据必须能够匹配到已有分区，否则会写入兜底分区，或者在没有兜底分区时直接失败。

常用单条写入如下。

```sql
INSERT INTO biz_order_log (
    id,
    tenant_id,
    order_no,
    biz_type,
    log_level,
    status,
    content,
    created_at,
    updated_at
) VALUES (
    1888888888888888888,
    10001,
    'ORD202603010001',
    'ORDER_CREATE',
    'INFO',
    'SUCCESS',
    JSON_OBJECT('message', '订单创建成功'),
    '2026-03-01 10:30:00',
    '2026-03-01 10:30:00'
);
```

常用批量写入如下。

```sql
INSERT INTO biz_order_log (
    id,
    tenant_id,
    order_no,
    biz_type,
    log_level,
    status,
    content,
    created_at,
    updated_at
) VALUES
(
    1888888888888888889,
    10001,
    'ORD202603010002',
    'ORDER_PAY',
    'INFO',
    'SUCCESS',
    JSON_OBJECT('message', '订单支付成功'),
    '2026-03-01 10:31:00',
    '2026-03-01 10:31:00'
),
(
    1888888888888888890,
    10001,
    'ORD202603010003',
    'ORDER_PAY',
    'ERROR',
    'FAILED',
    JSON_OBJECT('message', '订单支付失败', 'reason', '余额不足'),
    '2026-03-01 10:32:00',
    '2026-03-01 10:32:00'
);
```

新增未来分区如下。

```sql
ALTER TABLE biz_order_log
ADD PARTITION (
    PARTITION p202604 VALUES LESS THAN ('2026-05-01')
);
```

删除历史分区如下。

```sql
ALTER TABLE biz_order_log
DROP PARTITION p202601;
```

删除分区会直接删除该分区内的全部数据，适合历史数据清理场景。执行前必须确认该分区数据已经完成备份、归档或不再需要。

如果存在 `pmax` 兜底分区，后续需要将兜底分区重新拆分为未来分区。

```sql
ALTER TABLE biz_order_log
REORGANIZE PARTITION pmax INTO (
    PARTITION p202604 VALUES LESS THAN ('2026-05-01'),
    PARTITION pmax VALUES LESS THAN (MAXVALUE)
);
```

### 常见问题

分区表常见问题主要集中在分区键选择、唯一约束限制、查询未命中分区、分区维护不及时以及历史数据清理风险上。

| 问题             | 原因                         | 处理方式                                              |
| ---------------- | ---------------------------- | ----------------------------------------------------- |
| 查询仍然很慢     | SQL 未带分区键，扫描多个分区 | 查询条件中补充分区键范围，并检查 `EXPLAIN partitions` |
| 无法创建唯一索引 | 唯一键未包含分区键           | 将分区键加入唯一键，或调整唯一约束设计                |
| 写入失败         | 数据无法匹配任何分区         | 提前创建未来分区，或增加 `pmax` 兜底分区              |
| 某个分区过大     | 分区粒度过粗或数据倾斜       | 从按年改为按月，或重新评估分区键                      |
| 删除历史数据慢   | 使用大批量 `DELETE`          | 改用 `DROP PARTITION` 清理整段历史数据                |
| 分区太多         | 分区粒度过细                 | 控制分区数量，按月或按季度分区优先                    |
| 查询跨很多月份   | 时间范围过大                 | 限制查询时间跨度，必要时使用归档表或汇总表            |

使用分区表时需要特别注意唯一约束限制。MySQL 分区表要求唯一键中的字段必须包含分区表达式涉及的全部字段。例如使用 `created_at` 分区时，`PRIMARY KEY (id)` 可能不符合要求，通常需要改为 `PRIMARY KEY (id, created_at)`。

另外，分区表不是分库分表。分区表仍然属于单个 MySQL 实例内的单张逻辑表，不能解决单机写入瓶颈、实例容量瓶颈和跨实例水平扩展问题。如果数据增长已经超过单实例承载能力，应考虑分库分表模型。

### 总结

分区表模型适合处理大数据量单表中具有明显时间范围、租户范围或业务范围特征的数据。它的核心价值是通过分区裁剪减少扫描范围，并通过分区级维护提升历史数据清理效率。

建模时应优先确定分区键、分区粒度和数据生命周期。字段设计中要保证分区键稳定且必填，索引设计中要让常用查询同时满足分区裁剪和分区内索引过滤。常用查询必须尽量带上分区键范围，常用写入则要确保未来分区维护及时。

分区表适合作为单库单表向大数据量模型演进的第一步。当业务需要更强的写入扩展能力、更大的存储容量或跨实例水平扩展能力时，应进一步评估分库分表模型。



## 分库分表模型

分库分表模型用于解决单库或单表在数据量、写入吞吐、存储容量、索引维护、连接数和实例资源上的瓶颈。与分区表不同，分库分表通常会把数据拆分到多个数据库实例、多个物理库或多个物理表中，业务访问需要通过路由规则定位真实数据位置。

分库分表是大数据量架构中的重要演进模型，通常不作为系统早期默认方案。只有当单库单表、索引优化、冷热分离、归档表、读写分离、分区表等方案无法满足容量或性能要求时，才建议引入分库分表。

### 适用场景

分库分表适合数据量持续增长、单库单表已经接近容量或性能上限，并且业务可以接受一定路由复杂度的场景。该模型通常用于订单、支付、流水、消息、日志、账户明细、库存流水等高增长业务表。

适合使用分库分表的场景包括：

- 单表数据量过大，例如单表达到千万级、亿级，并且查询、写入、索引维护明显变慢。
- 单库写入压力过高，单实例 CPU、IO、连接数、锁等待已经成为瓶颈。
- 业务数据具备明确的拆分维度，例如用户ID、租户ID、订单ID、商户ID、区域ID。
- 高频查询大多可以根据分片键精准定位数据。
- 业务需要横向扩展存储容量和写入能力。
- 历史数据增长快，但不能简单归档或删除。
- 单库单表的备份、恢复、DDL、索引重建成本过高。

不适合使用分库分表的场景包括：

- 数据量不大，只是短期查询慢，应优先优化索引、SQL 和表结构。
- 查询条件无法稳定带上分片键。
- 业务大量依赖跨表、跨库 JOIN。
- 强一致事务范围经常跨多个分片。
- 需要频繁按任意字段检索全量数据。
- 系统团队暂时无法承担路由、扩容、迁移、聚合查询和运维复杂度。
- 主键、唯一约束、分页、排序、统计等能力还没有配套设计。

### 建模结构

分库分表建模时，应先确定拆分目标、分片键、分片算法、库表数量、主键生成方式和路由规则。建模结构只描述业务表如何被拆分、数据如何落库落表，以及逻辑表和物理表之间的关系，索引设计应放在后续“索引设计”章节中统一说明。

以订单表为例，订单数据通常具备高增长、高写入、高查询频率等特点。订单详情查询一般会根据订单号、用户ID、租户ID等条件定位数据，因此可以选择稳定字段作为分片键。

常见分库分表结构如下：

| 结构项   | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 逻辑表   | 业务层面访问的表名，例如 `biz_order`                         |
| 物理库   | 实际存储数据的数据库，例如 `order_db_00`、`order_db_01`      |
| 物理表   | 实际存储数据的数据表，例如 `biz_order_0000`、`biz_order_0001` |
| 分片键   | 用于计算路由位置的字段，例如 `user_id`、`order_no`、`tenant_id` |
| 分片算法 | 用于计算库表位置的规则，例如取模、范围、哈希、一致性哈希     |
| 主键策略 | 生成全局唯一ID，例如雪花ID、号段ID、发号器                   |
| 路由层   | 负责把逻辑 SQL 路由到真实库表，例如 ShardingSphere、业务自研路由 |
| 扩容策略 | 后续库表数量增长时的数据迁移和路由变更方案                   |

订单表按用户ID分库分表示例结构如下：

```sql
CREATE TABLE biz_order_0000 (
    id BIGINT NOT NULL COMMENT '订单ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    order_status VARCHAR(32) NOT NULL COMMENT '订单状态',
    pay_status VARCHAR(32) NOT NULL COMMENT '支付状态',
    total_amount DECIMAL(18, 2) NOT NULL COMMENT '订单总金额',
    pay_amount DECIMAL(18, 2) NOT NULL COMMENT '实付金额',
    remark VARCHAR(512) NULL COMMENT '订单备注',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) COMMENT='订单表_0000';

CREATE TABLE biz_order_0001 LIKE biz_order_0000;
CREATE TABLE biz_order_0002 LIKE biz_order_0000;
CREATE TABLE biz_order_0003 LIKE biz_order_0000;
```

该结构中，`biz_order` 是逻辑表，`biz_order_0000` 到 `biz_order_0003` 是物理表。业务层不应直接拼接物理表名，而应通过路由层根据分片键计算真实表。

如果同时分库和分表，可以采用如下结构：

```text
order_db_00
├── biz_order_0000
├── biz_order_0001
├── biz_order_0002
└── biz_order_0003

order_db_01
├── biz_order_0004
├── biz_order_0005
├── biz_order_0006
└── biz_order_0007
```

常见路由规则如下：

```text
db_index = user_id % 2
table_index = user_id % 8
```

例如 `user_id = 10009` 时：

```text
db_index = 10009 % 2 = 1
table_index = 10009 % 8 = 1
```

则数据路由到：

```text
order_db_01.biz_order_0001
```

实际生产中，库表数量应根据业务增长预估、单表容量上限、实例规格、扩容周期和迁移成本综合确定。不要盲目创建过多物理表，否则会增加连接管理、元数据管理、监控、备份和运维复杂度。

### 字段设计

分库分表字段设计的重点是确保分片键稳定、主键全局唯一、业务查询能够命中路由规则，并且关键业务字段能够支持后续对账、追踪和数据修复。

推荐字段设计如下：

| 字段           | 类型             | 是否必填 | 说明                                 |
| -------------- | ---------------- | -------- | ------------------------------------ |
| `id`           | `BIGINT`         | 是       | 全局唯一主键，建议使用雪花ID或号段ID |
| `tenant_id`    | `BIGINT`         | 是       | 租户ID，多租户系统必备字段           |
| `user_id`      | `BIGINT`         | 是       | 用户ID，可作为订单场景常用分片键     |
| `order_no`     | `VARCHAR(64)`    | 是       | 订单编号，业务唯一标识               |
| `order_status` | `VARCHAR(32)`    | 是       | 订单状态                             |
| `pay_status`   | `VARCHAR(32)`    | 是       | 支付状态                             |
| `total_amount` | `DECIMAL(18, 2)` | 是       | 订单总金额                           |
| `pay_amount`   | `DECIMAL(18, 2)` | 是       | 实付金额                             |
| `remark`       | `VARCHAR(512)`   | 否       | 订单备注                             |
| `created_at`   | `DATETIME`       | 是       | 创建时间                             |
| `updated_at`   | `DATETIME`       | 是       | 更新时间                             |

字段设计建议：

- 分片键必须稳定，避免后续更新导致数据需要跨库迁移。
- 分片键应尽量出现在大多数核心查询条件中。
- 主键必须全局唯一，不能依赖单库单表自增ID。
- 业务单号应全局唯一，便于跨库排查、对账和链路追踪。
- 分片键字段不建议允许为空，否则路由规则会变复杂。
- 高频筛选字段应尽量保持简单类型，避免使用大字段或 JSON 字段参与核心查询。
- 金额字段应使用 `DECIMAL`，不建议使用 `FLOAT` 或 `DOUBLE`。
- 时间字段应统一时区，并保留创建时间和更新时间。
- 多租户场景中，`tenant_id` 是否作为分片键需要结合租户数据规模判断，避免大租户导致热点分片。

分片键选择建议如下：

| 分片键        | 适用场景                             | 风险                           |
| ------------- | ------------------------------------ | ------------------------------ |
| `user_id`     | 用户维度查询多，例如订单、账户、消息 | 大用户可能导致热点             |
| `tenant_id`   | SaaS 多租户隔离明显                  | 大租户可能导致数据倾斜         |
| `order_no`    | 按订单号点查多                       | 用户订单列表查询可能需要辅助表 |
| `merchant_id` | 商户维度查询多                       | 头部商户可能形成热点           |
| `region_id`   | 区域天然隔离                         | 区域数据可能不均衡             |
| `created_at`  | 时间维度归档强                       | 高频查询可能跨多个时间片       |

### 索引设计

分库分表索引设计需要区分逻辑索引和物理索引。业务层看的是逻辑表，数据库实际维护的是每个物理表上的本地索引。分库分表后，索引只能在单个物理表内生效，不能天然形成跨库全局索引。

订单分表推荐索引如下：

```sql
ALTER TABLE biz_order_0000
    ADD UNIQUE KEY uk_order_no (order_no),
    ADD INDEX idx_user_created (user_id, created_at),
    ADD INDEX idx_tenant_created (tenant_id, created_at),
    ADD INDEX idx_order_status_created (order_status, created_at),
    ADD INDEX idx_pay_status_created (pay_status, created_at);
```

其他物理表应保持相同索引结构。实际生产中应通过建表模板、DDL 管理工具或迁移工具统一维护所有分表结构，避免不同物理表结构不一致。

索引设计说明：

| 索引                       | 适用查询           | 说明                                       |
| -------------------------- | ------------------ | ------------------------------------------ |
| `uk_order_no`              | 按订单号查询       | 单表内唯一，是否全局唯一依赖订单号生成规则 |
| `idx_user_created`         | 按用户查询订单列表 | 用户维度核心查询索引                       |
| `idx_tenant_created`       | 按租户查询订单     | 多租户运营后台常用                         |
| `idx_order_status_created` | 按订单状态查询     | 适合状态筛选和后台处理                     |
| `idx_pay_status_created`   | 按支付状态查询     | 适合支付对账和异常排查                     |

索引设计建议：

- 分片键相关查询应优先设计联合索引。
- 所有分表索引结构必须保持一致。
- 物理表上的唯一索引只能保证单表内唯一，不能自动保证全局唯一。
- 全局唯一性应通过 ID 生成器、业务单号生成器或全局索引表保证。
- 后台查询如果经常不带分片键，应设计冗余查询表、搜索表或汇总表。
- 不建议依赖跨库聚合查询支撑高频核心链路。
- 索引数量需要控制，分表数量越多，DDL 和索引维护成本越高。

如果需要保证订单号全局唯一，可以使用单独的全局唯一单号生成规则，而不是依赖每张分表的唯一索引。

```text
订单号生成规则示例：
ORD + yyyyMMddHHmmss + 机器号 + 序列号
```

这种方式可以在写入前保证订单号全局唯一，避免跨库检查唯一性。

### 常用查询

分库分表场景下，查询是否高效主要取决于是否能够根据分片键精准路由。能够精准路由的查询通常性能稳定；不能精准路由的查询可能需要广播到多个库表，再进行结果合并。

#### 按分片键查询订单列表

该查询适合用户订单列表页。由于查询条件包含 `user_id`，路由层可以直接定位到对应物理库表。

```sql
SELECT
    id,
    tenant_id,
    user_id,
    order_no,
    order_status,
    pay_status,
    total_amount,
    pay_amount,
    created_at
FROM biz_order
WHERE user_id = 10009
  AND created_at >= '2026-01-01 00:00:00'
  AND created_at < '2026-02-01 00:00:00'
ORDER BY created_at DESC
LIMIT 20;
```

该查询属于推荐查询方式。业务 SQL 使用逻辑表 `biz_order`，路由层根据 `user_id` 定位真实表。

#### 按订单号查询订单详情

该查询适合订单详情页。如果分片键不是 `order_no`，则需要通过订单号反查分片键，或者使用订单号本身包含路由信息。

```sql
SELECT
    id,
    tenant_id,
    user_id,
    order_no,
    order_status,
    pay_status,
    total_amount,
    pay_amount,
    remark,
    created_at,
    updated_at
FROM biz_order
WHERE order_no = 'ORD2026010100010001';
```

如果订单号无法推导分片位置，该查询可能会广播到所有分表。生产中建议通过以下方式避免广播：

- 订单号中包含路由因子，例如用户ID后几位、库表编号或雪花ID片段。
- 建立订单号到分片键的映射表。
- 建立订单查询辅助表，例如 `biz_order_route`。
- 将 `order_no` 设计为可解析路由的业务单号。

订单路由辅助表示例：

```sql
CREATE TABLE biz_order_route (
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    db_index INT NOT NULL COMMENT '库编号',
    table_index INT NOT NULL COMMENT '表编号',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (order_no)
) COMMENT='订单路由表';
```

查询时先查路由表，再根据路由结果查询订单主表。

```sql
SELECT
    order_no,
    user_id,
    db_index,
    table_index
FROM biz_order_route
WHERE order_no = 'ORD2026010100010001';
```

#### 按主键查询

如果主键使用雪花ID，并且雪花ID中不能直接解析库表位置，则只按 `id` 查询也可能无法精准路由。

```sql
SELECT
    id,
    tenant_id,
    user_id,
    order_no,
    order_status,
    pay_status,
    total_amount,
    pay_amount,
    created_at
FROM biz_order
WHERE id = 1888888888888888888;
```

推荐做法是让查询条件同时携带分片键。

```sql
SELECT
    id,
    tenant_id,
    user_id,
    order_no,
    order_status,
    pay_status,
    total_amount,
    pay_amount,
    created_at
FROM biz_order
WHERE id = 1888888888888888888
  AND user_id = 10009;
```

这样路由层可以根据 `user_id` 精准定位物理表，再在表内根据主键快速查询。

#### 按状态查询待处理订单

该查询适合运营后台或任务处理系统。如果不带分片键，通常会扫描多个分表。

```sql
SELECT
    id,
    tenant_id,
    user_id,
    order_no,
    order_status,
    pay_status,
    created_at
FROM biz_order
WHERE order_status = 'PENDING'
  AND created_at >= '2026-01-01 00:00:00'
  AND created_at < '2026-01-02 00:00:00'
ORDER BY created_at ASC
LIMIT 100;
```

该查询可能需要广播到多个分表后再合并排序。对于高频任务处理场景，不建议直接依赖订单主表扫描，可以考虑建立待处理任务表。

```sql
CREATE TABLE biz_order_pending_task (
    id BIGINT NOT NULL COMMENT '任务ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    task_status VARCHAR(32) NOT NULL COMMENT '任务状态',
    next_execute_time DATETIME NOT NULL COMMENT '下次执行时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_task_status_next_time (task_status, next_execute_time)
) COMMENT='订单待处理任务表';
```

通过任务表驱动处理，可以避免频繁扫描订单分表。

#### 按租户查询订单

多租户后台经常需要按租户查询订单。如果分片键是 `user_id`，而查询条件只有 `tenant_id`，则无法精准路由。

```sql
SELECT
    id,
    tenant_id,
    user_id,
    order_no,
    order_status,
    pay_status,
    total_amount,
    created_at
FROM biz_order
WHERE tenant_id = 10001
  AND created_at >= '2026-01-01 00:00:00'
  AND created_at < '2026-02-01 00:00:00'
ORDER BY created_at DESC
LIMIT 100;
```

如果该查询是高频核心查询，应考虑以下方案：

- 使用 `tenant_id` 作为分片键。
- 建立租户订单宽表或查询辅助表。
- 将运营后台查询走 Elasticsearch、ClickHouse 或统计汇总表。
- 限制查询时间范围，并异步导出大结果集。
- 对大租户进行单独分片或独立库隔离。

#### 分页查询

分库分表后的分页查询需要避免深分页和跨分片全局排序。推荐使用游标分页，并尽量带上分片键。

```sql
SELECT
    id,
    tenant_id,
    user_id,
    order_no,
    order_status,
    pay_status,
    total_amount,
    created_at
FROM biz_order
WHERE user_id = 10009
  AND id < 1888888888888888888
ORDER BY id DESC
LIMIT 20;
```

该查询可以精准路由到单个分片，性能稳定。

如果需要跨多个分片做全局分页，应避免使用过深的 `OFFSET`。

```sql
SELECT
    id,
    tenant_id,
    user_id,
    order_no,
    order_status,
    pay_status,
    total_amount,
    created_at
FROM biz_order
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at < '2026-01-02 00:00:00'
ORDER BY created_at DESC
LIMIT 100 OFFSET 10000;
```

这类查询需要多个分片分别扫描、排序、合并，数据量大时性能不可控。推荐改为异步导出、搜索引擎查询、汇总表查询或限定分片键查询。

### 常用写入

分库分表写入时，业务侧通常写入逻辑表，路由层根据分片键计算真实库表位置。写入前必须保证分片键已存在，并且主键、业务单号已经生成。

常用单条写入如下：

```sql
INSERT INTO biz_order (
    id,
    tenant_id,
    user_id,
    order_no,
    order_status,
    pay_status,
    total_amount,
    pay_amount,
    remark,
    created_at,
    updated_at
) VALUES (
    1888888888888888888,
    10001,
    10009,
    'ORD2026010100010001',
    'CREATED',
    'UNPAID',
    199.00,
    199.00,
    '用户下单',
    '2026-01-01 10:30:00',
    '2026-01-01 10:30:00'
);
```

根据路由规则：

```text
db_index = user_id % 2
table_index = user_id % 8
```

当 `user_id = 10009` 时，数据会被路由到：

```text
order_db_01.biz_order_0001
```

写入订单主表后，如果存在订单号查询场景，还应同步写入订单路由表。

```sql
INSERT INTO biz_order_route (
    order_no,
    user_id,
    db_index,
    table_index,
    created_at
) VALUES (
    'ORD2026010100010001',
    10009,
    1,
    1,
    '2026-01-01 10:30:00'
);
```

订单主表和路由表写入需要保证一致性。常见处理方式包括：

- 同库同事务写入，适合路由表和订单表在同一库的场景。
- 本地事务加可靠消息，适合路由表或辅助表异步写入场景。
- Outbox 事件表，适合订单创建后异步构建查询模型。
- 定时校验补偿，适合处理辅助表写入失败或消息消费失败。

批量写入时，必须注意批量数据可能属于不同分片。路由层通常会将数据拆分成多个真实 SQL 分发到不同库表。

```sql
INSERT INTO biz_order (
    id,
    tenant_id,
    user_id,
    order_no,
    order_status,
    pay_status,
    total_amount,
    pay_amount,
    remark,
    created_at,
    updated_at
) VALUES
(
    1888888888888888889,
    10001,
    10009,
    'ORD2026010100010002',
    'CREATED',
    'UNPAID',
    99.00,
    99.00,
    '用户下单',
    '2026-01-01 10:31:00',
    '2026-01-01 10:31:00'
),
(
    1888888888888888890,
    10001,
    10010,
    'ORD2026010100010003',
    'CREATED',
    'UNPAID',
    299.00,
    299.00,
    '用户下单',
    '2026-01-01 10:32:00',
    '2026-01-01 10:32:00'
);
```

批量写入建议：

- 单批次数据量不要过大，避免跨分片 SQL 过多。
- 批量数据最好按分片键预分组后再写入。
- 写入前生成全局唯一ID，避免依赖数据库自增。
- 写入链路中应记录分片键、业务单号和主键，方便问题排查。
- 不建议在分库分表主链路中执行复杂跨库事务。
- 写入辅助表、索引表、搜索表时应有补偿机制。

### 常见问题

分库分表常见问题主要集中在分片键选择、跨库查询、全局唯一、分布式事务、扩容迁移、聚合统计和运维复杂度上。

| 问题               | 原因                                 | 处理方式                                   |
| ------------------ | ------------------------------------ | ------------------------------------------ |
| 查询变慢           | 查询条件未带分片键，触发全库全表扫描 | 改造查询条件，增加分片键，或建设辅助查询表 |
| 订单号点查需要广播 | 订单号无法推导分片位置               | 设计可路由订单号，或增加订单路由表         |
| 唯一约束失效       | 唯一索引只能保证单个物理表内唯一     | 使用全局ID、全局单号或唯一索引表           |
| 跨库事务复杂       | 一次业务操作涉及多个分片             | 使用本地事务、可靠消息、Outbox、最终一致性 |
| 分片数据不均       | 分片键选择不合理或存在热点用户       | 更换分片键、增加扰动因子、大客户独立分片   |
| 扩容困难           | 初始库表数量不足或路由规则不可扩展   | 提前规划容量，使用一致性哈希或扩容迁移方案 |
| 后台查询复杂       | 后台查询条件多且不带分片键           | 使用宽表、搜索引擎、数据仓库或汇总表       |
| 分页不准确         | 跨分片排序和分页需要结果合并         | 限制查询范围，改用游标分页或异步导出       |
| DDL 成本高         | 所有物理表都需要同步变更             | 使用迁移工具统一执行，建立结构校验机制     |
| 数据修复困难       | 数据分散在多个库表                   | 修复脚本必须支持分片路由和幂等执行         |

分片键选择是分库分表中最关键的设计点。分片键一旦确定，后续调整通常需要全量数据迁移。选择分片键时，应重点评估以下问题：

- 核心查询是否大多带有该字段。
- 数据是否能均匀分布。
- 字段是否稳定且不可变。
- 是否会出现热点用户、热点租户或热点商户。
- 是否影响后续扩容。
- 是否影响后台运营查询和数据统计。
- 是否影响业务唯一约束设计。

跨库事务也是常见问题。分库分表后，不建议把强一致事务扩大到多个分片。应优先通过业务建模减少跨分片写入。例如订单创建、支付记录、库存扣减、账户流水如果必须跨多个系统，应通过业务状态机、可靠消息、幂等处理和补偿任务保证最终一致。

### 总结

分库分表模型适合解决单库单表在容量、写入吞吐、索引维护和实例资源上的瓶颈。它通过分片键和路由规则把数据拆分到多个库表中，从而获得更强的横向扩展能力。

建模时应优先确定分片键、分片算法、库表数量、主键生成方式和路由规则。字段设计中要保证分片键稳定、主键全局唯一、业务单号可追踪。索引设计中要明确物理表索引只能在单表内生效，不能天然解决跨库全局唯一和跨分片查询问题。

查询设计应尽量带上分片键，避免广播查询、跨分片深分页和高频跨库聚合。写入设计应提前生成全局ID，并确保路由表、辅助表、搜索表和主表之间具备一致性保障。

分库分表能显著提升系统容量和写入扩展能力，但也会带来路由、事务、查询、扩容、迁移、监控和运维复杂度。只有当单库单表优化、分区表、冷热分离、归档表、读写分离等方案无法继续支撑业务增长时，才建议引入分库分表模型。