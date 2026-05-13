# 历史版本与数据变更模型

历史版本与数据变更模型用于解决业务数据在多次修改、发布、审核、回滚、追溯过程中的数据保留问题。该类模型通常不直接覆盖旧数据，而是通过版本表、快照字段或变更记录表保存历史状态，使业务系统具备可追溯、可回滚、可审计的能力。

## 历史版本模型

历史版本模型用于保存同一业务对象在不同时间点、不同发布批次或不同审核阶段下的完整数据状态。该模型的核心思想是：业务主表保存当前可用数据，历史版本表保存每一次形成版本时的业务快照。

### 适用场景

历史版本模型适用于业务数据存在“多次修改、历史追溯、版本对比、版本回滚、按版本展示”等需求的场景。它强调的是业务对象在不同版本下的完整状态，而不是单个字段的修改流水。

常见适用场景包括合同、协议、报价单、商品资料、配置方案、表单模板、规则配置、文档内容、审批单据等业务对象。例如合同在多次修订后仍然需要查看 V1、V2、V3 的完整内容；商品资料在每次发布后需要保留当时的标题、价格、规格和上下架状态；业务规则在调整后需要支持回滚到某个历史版本。

该模型一般适合以下业务特征：

| 业务特征             | 说明                                       |
| -------------------- | ------------------------------------------ |
| 数据会反复修改       | 同一个业务对象会经历多次编辑、审核、发布   |
| 历史状态需要完整保留 | 需要查看某个版本下的完整业务数据           |
| 支持版本对比         | 需要比较两个版本之间的字段差异             |
| 支持版本回滚         | 可以将当前数据恢复到历史版本               |
| 修改后不应破坏旧业务 | 老订单、老合同、老规则需要继续引用当时版本 |

不适合使用历史版本模型的场景是只需要知道“谁在什么时候改了哪个字段”。这种需求更适合使用数据变更记录模型或操作日志模型。

### 建模结构

历史版本模型通常由一张业务主表和一张历史版本表组成。业务主表保存当前最新状态，历史版本表保存每次形成版本时的完整快照。

推荐结构如下：

| 表名                   | 作用                                       |
| ---------------------- | ------------------------------------------ |
| `biz_contract`         | 合同主表，保存当前版本的核心业务数据       |
| `biz_contract_version` | 合同历史版本表，保存每个版本的完整数据快照 |

业务主表只保存当前业务对象的最新状态，并记录当前版本号。历史版本表以业务对象 ID 和版本号作为版本定位依据，每产生一个新版本，就向历史版本表插入一条不可变记录。

下面以合同为例给出基础表结构。该结构只描述表与字段，不包含普通查询索引，索引统一放在“索引设计”章节。

业务主表保存当前合同状态和当前版本号。

```sql
CREATE TABLE biz_contract (
    id BIGINT NOT NULL COMMENT '主键ID',
    contract_no VARCHAR(64) NOT NULL COMMENT '合同编号',
    contract_name VARCHAR(128) NOT NULL COMMENT '合同名称',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    customer_name VARCHAR(128) NOT NULL COMMENT '客户名称',
    contract_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '合同金额',
    contract_status TINYINT NOT NULL DEFAULT 0 COMMENT '合同状态：0草稿，1审核中，2已生效，3已终止',
    current_version_no INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
    effective_time DATETIME NULL COMMENT '生效时间',
    remark VARCHAR(512) NULL COMMENT '备注',
    create_user BIGINT NOT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_user BIGINT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '合同主表';
```

历史版本表保存每一个版本的合同快照。版本表中的业务字段可以根据查询频率选择部分结构化存储，同时使用 `snapshot_json` 保存完整快照。

```sql
CREATE TABLE biz_contract_version (
    id BIGINT NOT NULL COMMENT '主键ID',
    contract_id BIGINT NOT NULL COMMENT '合同主表ID',
    contract_no VARCHAR(64) NOT NULL COMMENT '合同编号',
    version_no INT NOT NULL COMMENT '版本号',
    version_name VARCHAR(64) NULL COMMENT '版本名称，例如V1、V2、正式版、修订版',
    version_status TINYINT NOT NULL DEFAULT 1 COMMENT '版本状态：1有效，2已回滚，3已废弃',
    change_type TINYINT NOT NULL DEFAULT 1 COMMENT '版本类型：1新增，2修订，3回滚，4导入',
    change_reason VARCHAR(512) NULL COMMENT '版本变更原因',
    contract_name VARCHAR(128) NOT NULL COMMENT '合同名称快照',
    customer_id BIGINT NOT NULL COMMENT '客户ID快照',
    customer_name VARCHAR(128) NOT NULL COMMENT '客户名称快照',
    contract_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '合同金额快照',
    contract_status TINYINT NOT NULL COMMENT '合同状态快照',
    effective_time DATETIME NULL COMMENT '生效时间快照',
    snapshot_json JSON NOT NULL COMMENT '完整业务快照JSON',
    previous_version_id BIGINT NULL COMMENT '上一个版本ID',
    create_user BIGINT NOT NULL COMMENT '创建版本的人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建版本时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '合同历史版本表';
```

该结构的重点是主表和版本表职责分离。主表面向当前业务读写，版本表面向历史追溯、版本对比和回滚。版本表中的历史数据原则上插入后不再修改，除非是逻辑废弃、数据修复或补充版本状态。

### 字段设计

字段设计需要明确哪些字段用于业务定位，哪些字段用于版本定位，哪些字段用于保存快照，哪些字段用于审计追踪。历史版本表不应只保存变更字段，否则无法直接还原某个历史版本的完整业务状态。

业务主表字段建议如下：

| 字段                 | 类型            | 必填 | 说明                   |
| -------------------- | --------------- | ---- | ---------------------- |
| `id`                 | `BIGINT`        | 是   | 主键ID                 |
| `contract_no`        | `VARCHAR(64)`   | 是   | 合同编号，业务唯一标识 |
| `contract_name`      | `VARCHAR(128)`  | 是   | 当前合同名称           |
| `customer_id`        | `BIGINT`        | 是   | 当前客户ID             |
| `customer_name`      | `VARCHAR(128)`  | 是   | 当前客户名称           |
| `contract_amount`    | `DECIMAL(18,2)` | 是   | 当前合同金额           |
| `contract_status`    | `TINYINT`       | 是   | 当前合同状态           |
| `current_version_no` | `INT`           | 是   | 当前版本号             |
| `effective_time`     | `DATETIME`      | 否   | 当前版本生效时间       |
| `remark`             | `VARCHAR(512)`  | 否   | 当前备注               |
| `create_user`        | `BIGINT`        | 是   | 创建人ID               |
| `create_time`        | `DATETIME`      | 是   | 创建时间               |
| `update_user`        | `BIGINT`        | 否   | 最后更新人ID           |
| `update_time`        | `DATETIME`      | 是   | 最后更新时间           |
| `deleted`            | `TINYINT`       | 是   | 逻辑删除标识           |

历史版本表字段建议如下：

| 字段                  | 类型            | 必填 | 说明                         |
| --------------------- | --------------- | ---- | ---------------------------- |
| `id`                  | `BIGINT`        | 是   | 版本记录主键ID               |
| `contract_id`         | `BIGINT`        | 是   | 关联业务主表ID               |
| `contract_no`         | `VARCHAR(64)`   | 是   | 合同编号快照                 |
| `version_no`          | `INT`           | 是   | 版本号，从 1 递增            |
| `version_name`        | `VARCHAR(64)`   | 否   | 版本名称                     |
| `version_status`      | `TINYINT`       | 是   | 版本状态                     |
| `change_type`         | `TINYINT`       | 是   | 新增、修订、回滚、导入等类型 |
| `change_reason`       | `VARCHAR(512)`  | 否   | 本次形成版本的原因           |
| `contract_name`       | `VARCHAR(128)`  | 是   | 合同名称快照                 |
| `customer_id`         | `BIGINT`        | 是   | 客户ID快照                   |
| `customer_name`       | `VARCHAR(128)`  | 是   | 客户名称快照                 |
| `contract_amount`     | `DECIMAL(18,2)` | 是   | 合同金额快照                 |
| `contract_status`     | `TINYINT`       | 是   | 合同状态快照                 |
| `effective_time`      | `DATETIME`      | 否   | 生效时间快照                 |
| `snapshot_json`       | `JSON`          | 是   | 完整业务快照                 |
| `previous_version_id` | `BIGINT`        | 否   | 上一个版本ID                 |
| `create_user`         | `BIGINT`        | 是   | 创建版本的人                 |
| `create_time`         | `DATETIME`      | 是   | 创建版本时间                 |
| `deleted`             | `TINYINT`       | 是   | 逻辑删除标识                 |

版本号建议使用业务对象内递增，而不是全局递增。也就是说，同一个 `contract_id` 下的 `version_no` 从 1、2、3 递增，不同合同之间的版本号互不影响。

`snapshot_json` 适合保存完整快照，但不建议所有查询都依赖 JSON 字段。高频查询字段应冗余为普通列，例如合同编号、客户ID、合同金额、版本号、版本状态、生效时间等。这样既能保留完整快照，又能保证常用查询性能。

### 索引设计

索引设计应围绕当前数据查询、历史版本列表、指定版本定位、最新版本定位、版本唯一性约束来设计。历史版本表通常数据增长较快，索引不能过多，否则会影响版本写入性能。

业务主表建议索引如下：

```sql
ALTER TABLE biz_contract
    ADD UNIQUE KEY uk_contract_no (contract_no),
    ADD KEY idx_customer_id (customer_id),
    ADD KEY idx_contract_status (contract_status),
    ADD KEY idx_update_time (update_time);
```

这些索引用于支持按合同编号定位、按客户查询合同、按状态筛选合同、按更新时间进行分页或同步。

历史版本表建议索引如下：

```sql
ALTER TABLE biz_contract_version
    ADD UNIQUE KEY uk_contract_version (contract_id, version_no),
    ADD KEY idx_contract_id_create_time (contract_id, create_time),
    ADD KEY idx_contract_no_version_no (contract_no, version_no),
    ADD KEY idx_create_time (create_time),
    ADD KEY idx_version_status (version_status);
```

`uk_contract_version` 用于保证同一业务对象下版本号唯一，是历史版本模型中最重要的约束。`idx_contract_id_create_time` 用于查询某个业务对象的版本列表。`idx_contract_no_version_no` 用于在只知道业务编号和版本号时快速定位版本。`idx_create_time` 适合做历史版本归档、清理或按时间统计。`idx_version_status` 适合筛选有效、废弃、回滚版本。

如果业务经常查询“某个合同的最新历史版本”，可以使用 `(contract_id, version_no)` 的唯一索引倒序扫描，不一定额外增加新索引。MySQL 8 支持降序索引，如果该查询非常高频，可以单独设计：

```sql
ALTER TABLE biz_contract_version
    ADD KEY idx_contract_id_version_no_desc (contract_id, version_no DESC);
```

是否增加降序索引应根据实际执行计划决定，不建议在初始建模阶段盲目添加过多索引。

### 常用查询

常用查询应优先使用结构化字段完成过滤和排序，只有在需要展示完整历史内容时再读取 `snapshot_json`。这样可以减少 JSON 解析成本，也能更好地利用索引。

#### 查询当前业务数据

查询当前合同数据时，只访问业务主表，不需要访问版本表。

```sql
SELECT
    id,
    contract_no,
    contract_name,
    customer_id,
    customer_name,
    contract_amount,
    contract_status,
    current_version_no,
    effective_time,
    update_time
FROM biz_contract
WHERE id = 10001
  AND deleted = 0;
```

该查询适合业务详情页、当前合同编辑页、当前合同审核页等只关心最新状态的场景。

#### 查询某个业务对象的版本列表

查询版本列表时，只返回版本摘要字段，避免列表页直接加载完整 `snapshot_json`。

```sql
SELECT
    id,
    contract_id,
    version_no,
    version_name,
    version_status,
    change_type,
    change_reason,
    create_user,
    create_time
FROM biz_contract_version
WHERE contract_id = 10001
  AND deleted = 0
ORDER BY version_no DESC;
```

该查询适合历史版本列表页。列表页通常只需要版本号、版本名称、变更原因、创建人和创建时间，不建议返回完整快照。

#### 查询指定版本详情

查询指定版本详情时，可以通过 `contract_id + version_no` 精确定位历史版本。

```sql
SELECT
    id,
    contract_id,
    contract_no,
    version_no,
    version_name,
    version_status,
    change_type,
    change_reason,
    contract_name,
    customer_id,
    customer_name,
    contract_amount,
    contract_status,
    effective_time,
    snapshot_json,
    previous_version_id,
    create_user,
    create_time
FROM biz_contract_version
WHERE contract_id = 10001
  AND version_no = 3
  AND deleted = 0;
```

该查询适合历史版本详情页、版本回滚预览、版本对比前的数据加载。

#### 查询最新历史版本

查询最新历史版本可以按版本号倒序取第一条，适用于确认主表当前版本是否已经写入版本表。

```sql
SELECT
    id,
    contract_id,
    version_no,
    version_status,
    snapshot_json,
    create_time
FROM biz_contract_version
WHERE contract_id = 10001
  AND deleted = 0
ORDER BY version_no DESC
LIMIT 1;
```

如果主表的 `current_version_no` 与版本表最大 `version_no` 不一致，需要排查是否存在版本写入失败、事务未提交、手工修复数据不完整等问题。

#### 查询两个版本用于对比

版本对比通常一次查询两个版本，再由应用层比较结构化字段或 `snapshot_json` 内容。

```sql
SELECT
    version_no,
    version_name,
    contract_name,
    customer_id,
    customer_name,
    contract_amount,
    contract_status,
    effective_time,
    snapshot_json,
    create_time
FROM biz_contract_version
WHERE contract_id = 10001
  AND version_no IN (2, 3)
  AND deleted = 0
ORDER BY version_no ASC;
```

对比逻辑建议放在应用层完成。数据库负责取出两个版本的完整快照，应用层负责按字段生成差异结果，例如合同金额由 100000.00 调整为 120000.00，生效时间由空调整为指定日期。

#### 按时间范围查询历史版本

按时间范围查询适合审计、统计、归档前扫描等场景。

```sql
SELECT
    id,
    contract_id,
    contract_no,
    version_no,
    change_type,
    create_user,
    create_time
FROM biz_contract_version
WHERE create_time >= '2026-01-01 00:00:00'
  AND create_time < '2026-02-01 00:00:00'
  AND deleted = 0
ORDER BY create_time ASC;
```

该查询依赖 `create_time` 索引。对于大数据量版本表，建议结合分页条件使用，避免一次性扫描过多数据。

### 常用写入

历史版本模型的写入重点是保证主表和版本表的一致性。更新当前业务数据并生成新版本时，应放在同一个事务中完成，避免主表已经更新但版本表没有写入成功。

#### 新增业务对象并生成初始版本

新增业务对象时，通常先写入主表，再写入版本表的 V1 版本。

```sql
START TRANSACTION;

INSERT INTO biz_contract (
    id,
    contract_no,
    contract_name,
    customer_id,
    customer_name,
    contract_amount,
    contract_status,
    current_version_no,
    effective_time,
    remark,
    create_user,
    update_user
) VALUES (
    10001,
    'HT202605130001',
    '年度采购合同',
    20001,
    '杭州示例科技有限公司',
    100000.00,
    2,
    1,
    '2026-05-13 00:00:00',
    '初始合同',
    30001,
    30001
);

INSERT INTO biz_contract_version (
    id,
    contract_id,
    contract_no,
    version_no,
    version_name,
    version_status,
    change_type,
    change_reason,
    contract_name,
    customer_id,
    customer_name,
    contract_amount,
    contract_status,
    effective_time,
    snapshot_json,
    previous_version_id,
    create_user
) VALUES (
    50001,
    10001,
    'HT202605130001',
    1,
    'V1',
    1,
    1,
    '创建初始版本',
    '年度采购合同',
    20001,
    '杭州示例科技有限公司',
    100000.00,
    2,
    '2026-05-13 00:00:00',
    JSON_OBJECT(
        'contractNo', 'HT202605130001',
        'contractName', '年度采购合同',
        'customerId', 20001,
        'customerName', '杭州示例科技有限公司',
        'contractAmount', 100000.00,
        'contractStatus', 2,
        'effectiveTime', '2026-05-13 00:00:00'
    ),
    NULL,
    30001
);

COMMIT;
```

该写法可以保证业务对象创建后立即拥有初始版本。对于需要审核后才形成版本的业务，可以先只写主表草稿，审核通过后再写版本表。

#### 修改业务对象并生成新版本

修改业务对象并生成新版本时，需要先计算下一个版本号，再更新主表并插入版本表。实际业务中建议使用事务和行级锁，避免并发修改导致版本号冲突。

```sql
START TRANSACTION;

SELECT
    id,
    current_version_no
FROM biz_contract
WHERE id = 10001
  AND deleted = 0
FOR UPDATE;

UPDATE biz_contract
SET
    contract_name = '年度采购合同-修订版',
    contract_amount = 120000.00,
    current_version_no = current_version_no + 1,
    update_user = 30002,
    update_time = NOW()
WHERE id = 10001
  AND deleted = 0;

INSERT INTO biz_contract_version (
    id,
    contract_id,
    contract_no,
    version_no,
    version_name,
    version_status,
    change_type,
    change_reason,
    contract_name,
    customer_id,
    customer_name,
    contract_amount,
    contract_status,
    effective_time,
    snapshot_json,
    previous_version_id,
    create_user
)
SELECT
    50002,
    c.id,
    c.contract_no,
    c.current_version_no,
    CONCAT('V', c.current_version_no),
    1,
    2,
    '调整合同金额并修订合同名称',
    c.contract_name,
    c.customer_id,
    c.customer_name,
    c.contract_amount,
    c.contract_status,
    c.effective_time,
    JSON_OBJECT(
        'contractNo', c.contract_no,
        'contractName', c.contract_name,
        'customerId', c.customer_id,
        'customerName', c.customer_name,
        'contractAmount', c.contract_amount,
        'contractStatus', c.contract_status,
        'effectiveTime', DATE_FORMAT(c.effective_time, '%Y-%m-%d %H:%i:%s')
    ),
    v.id,
    30002
FROM biz_contract c
LEFT JOIN biz_contract_version v
       ON v.contract_id = c.id
      AND v.version_no = c.current_version_no - 1
      AND v.deleted = 0
WHERE c.id = 10001
  AND c.deleted = 0;

COMMIT;
```

这里使用 `FOR UPDATE` 锁定主表记录，保证同一个合同在同一时刻只有一个事务可以生成新版本。版本表上的 `uk_contract_version(contract_id, version_no)` 仍然是最后一道防线，可以防止异常并发下产生重复版本号。

#### 回滚到历史版本

回滚不是删除当前版本，而是基于历史版本生成一个新的当前版本。这样可以保留“从 V4 回滚到 V2”的操作事实。

```sql
START TRANSACTION;

SELECT
    id,
    current_version_no
FROM biz_contract
WHERE id = 10001
  AND deleted = 0
FOR UPDATE;

SELECT
    contract_name,
    customer_id,
    customer_name,
    contract_amount,
    contract_status,
    effective_time,
    snapshot_json
FROM biz_contract_version
WHERE contract_id = 10001
  AND version_no = 2
  AND deleted = 0;

UPDATE biz_contract c
JOIN biz_contract_version v
  ON v.contract_id = c.id
 AND v.version_no = 2
 AND v.deleted = 0
SET
    c.contract_name = v.contract_name,
    c.customer_id = v.customer_id,
    c.customer_name = v.customer_name,
    c.contract_amount = v.contract_amount,
    c.contract_status = v.contract_status,
    c.effective_time = v.effective_time,
    c.current_version_no = c.current_version_no + 1,
    c.update_user = 30003,
    c.update_time = NOW()
WHERE c.id = 10001
  AND c.deleted = 0;

INSERT INTO biz_contract_version (
    id,
    contract_id,
    contract_no,
    version_no,
    version_name,
    version_status,
    change_type,
    change_reason,
    contract_name,
    customer_id,
    customer_name,
    contract_amount,
    contract_status,
    effective_time,
    snapshot_json,
    previous_version_id,
    create_user
)
SELECT
    50003,
    c.id,
    c.contract_no,
    c.current_version_no,
    CONCAT('V', c.current_version_no),
    1,
    3,
    '回滚到V2版本',
    c.contract_name,
    c.customer_id,
    c.customer_name,
    c.contract_amount,
    c.contract_status,
    c.effective_time,
    v.snapshot_json,
    pv.id,
    30003
FROM biz_contract c
JOIN biz_contract_version v
  ON v.contract_id = c.id
 AND v.version_no = 2
 AND v.deleted = 0
LEFT JOIN biz_contract_version pv
  ON pv.contract_id = c.id
 AND pv.version_no = c.current_version_no - 1
 AND pv.deleted = 0
WHERE c.id = 10001
  AND c.deleted = 0;

COMMIT;
```

回滚生成的是一个新版本，例如当前 V4 回滚到 V2 后，应形成 V5，而不是把当前版本号改回 V2。这样历史链路更完整，也方便审计。

### 常见问题

历史版本模型的常见问题主要集中在版本号生成、快照粒度、数据一致性、版本表膨胀和回滚语义上。建模时需要提前约束规则，避免后续业务复杂后难以修复。

| 问题                      | 说明                                       | 建议                                                |
| ------------------------- | ------------------------------------------ | --------------------------------------------------- |
| 是否每次编辑都生成版本    | 如果草稿频繁保存，版本表会快速膨胀         | 建议在提交、审核通过、发布、生效等关键节点生成版本  |
| 版本表是否保存完整字段    | 只保存变化字段会导致历史版本无法直接还原   | 建议保存完整快照，高频字段结构化，完整内容放入 JSON |
| 版本号如何生成            | 并发修改时容易出现重复版本号               | 使用事务、`FOR UPDATE` 和唯一约束共同保证           |
| 历史版本是否允许修改      | 修改历史版本会破坏可信度                   | 原则上历史版本不可变，只允许状态标记或数据修复      |
| 回滚是否覆盖旧版本        | 覆盖旧版本会丢失回滚过程                   | 回滚应生成新版本，并记录 `change_type = 3`          |
| 主表和版本表不一致怎么办  | 可能是事务拆分、手工修复或历史数据导入导致 | 以版本生成规则为准进行数据校验和补偿                |
| JSON 快照是否可以直接查询 | JSON 查询不适合高频复杂过滤                | 高频查询字段应冗余为普通列                          |
| 版本表数据过大怎么办      | 长期运行后版本表可能成为大表               | 可结合归档数据模型、分区表模型或冷热数据模型处理    |

版本生成节点需要根据业务稳定性决定。对于合同、商品、规则这类业务，通常不建议“每次保存草稿都生成版本”，否则版本数量过多且业务价值较低。更合理的做法是在审核通过、正式发布、生效、回滚、导入等关键动作后生成版本。

历史版本表最好做到逻辑不可变。业务上如果发现某个历史版本数据错误，可以通过补偿版本、修复标记或管理员修复流程处理，不建议普通业务流程直接更新历史版本内容。

### 总结

历史版本模型适合保存业务对象在不同版本下的完整状态。它的核心结构是“当前主表 + 历史版本表”，主表负责当前业务读写，版本表负责历史追溯、版本对比和版本回滚。

建模时需要重点关注四点。第一，版本号应在同一业务对象内递增，并通过唯一约束保证不重复。第二，历史版本应保存完整快照，不能只保存变化字段。第三，生成版本应与更新主表放在同一个事务中完成。第四，回滚应生成新版本，而不是覆盖或删除旧版本。

该模型与数据变更记录模型的区别在于：历史版本模型关注“某个版本下完整数据是什么”，数据变更记录模型关注“某次变更改了哪些字段”。在复杂业务系统中，两者可以同时使用，历史版本表用于业务回溯和回滚，数据变更记录表用于审计和字段级变更追踪。



## 数据变更记录模型

数据变更记录模型用于记录业务数据在一次写入操作中发生了什么变化。它关注的是“谁在什么时候对哪个业务对象做了什么修改，以及修改前后数据分别是什么”，常用于审计追踪、问题排查、字段级变更查看、敏感数据修改留痕和数据修复辅助。

### 适用场景

数据变更记录模型适用于需要追踪数据修改过程的业务场景。它不强调保存某个版本下的完整业务状态，而是强调一次数据变更行为的上下文、变更前数据、变更后数据和字段差异。

常见适用场景包括用户资料修改记录、订单状态变更记录、合同金额调整记录、商品价格修改记录、账户信息变更记录、后台配置变更记录、权限调整记录、敏感字段修改记录等。

该模型一般适合以下业务特征：

| 业务特征           | 说明                                           |
| ------------------ | ---------------------------------------------- |
| 需要追踪修改行为   | 需要知道谁修改了数据、什么时候修改、为什么修改 |
| 需要查看字段差异   | 需要知道哪些字段从什么值改成了什么值           |
| 需要辅助问题排查   | 线上数据异常时，可以通过变更记录定位操作来源   |
| 需要满足审计要求   | 敏感数据、财务数据、权限数据需要保留修改痕迹   |
| 不一定需要版本回滚 | 重点是记录变化过程，而不是管理业务版本         |

数据变更记录模型与历史版本模型的区别在于：历史版本模型保存“某个版本的完整业务状态”，数据变更记录模型保存“一次操作产生的字段变化”。如果业务既需要版本回滚，又需要字段级审计，可以同时使用两种模型。

### 建模结构

数据变更记录模型通常使用一张通用变更记录表，也可以根据业务复杂度拆分为“变更记录主表 + 变更字段明细表”。通用表结构实现简单，适合大多数后台业务系统；主从结构更适合字段级查询频繁、审计要求较高的系统。

推荐基础结构如下：

| 表名                     | 作用                                                         |
| ------------------------ | ------------------------------------------------------------ |
| `sys_data_change_record` | 数据变更记录表，保存一次数据变更的上下文、前后快照和字段差异 |

如果系统初期只需要审计和排查，建议先使用单表模型。单表中通过 `before_json`、`after_json` 和 `diff_json` 保存变更前、变更后和差异内容。高频查询字段使用普通列，例如业务类型、业务ID、业务编号、操作人、变更类型、创建时间等。

数据变更记录表用于保存一次完整的数据变更行为。

```sql
CREATE TABLE sys_data_change_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型，例如contract、order、product',
    biz_id BIGINT NOT NULL COMMENT '业务数据ID',
    biz_no VARCHAR(128) NULL COMMENT '业务编号，例如合同编号、订单编号',
    table_name VARCHAR(128) NOT NULL COMMENT '业务表名',
    change_type TINYINT NOT NULL COMMENT '变更类型：1新增，2修改，3删除，4状态变更，5导入，6修复',
    change_title VARCHAR(128) NULL COMMENT '变更标题',
    change_reason VARCHAR(512) NULL COMMENT '变更原因',
    changed_fields JSON NULL COMMENT '变更字段列表',
    before_json JSON NULL COMMENT '变更前数据JSON',
    after_json JSON NULL COMMENT '变更后数据JSON',
    diff_json JSON NULL COMMENT '字段差异JSON',
    request_id VARCHAR(128) NULL COMMENT '请求ID',
    trace_id VARCHAR(128) NULL COMMENT '链路追踪ID',
    operator_id BIGINT NULL COMMENT '操作人ID',
    operator_name VARCHAR(128) NULL COMMENT '操作人名称',
    operator_type TINYINT NOT NULL DEFAULT 1 COMMENT '操作人类型：1用户，2系统，3定时任务，4第三方',
    client_ip VARCHAR(64) NULL COMMENT '客户端IP',
    user_agent VARCHAR(512) NULL COMMENT '用户代理',
    remark VARCHAR(512) NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据变更记录表';
```

该结构的核心是把一次业务写入操作抽象为一条变更记录。业务数据本身仍然保存在原业务表中，变更记录表只负责记录写入行为和数据变化过程。

如果业务需要频繁按字段查询变更记录，例如查询“所有修改过 contract_amount 的操作”，可以增加字段明细表。

```sql
CREATE TABLE sys_data_change_field_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    change_record_id BIGINT NOT NULL COMMENT '数据变更记录ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    biz_id BIGINT NOT NULL COMMENT '业务数据ID',
    field_name VARCHAR(128) NOT NULL COMMENT '字段名',
    field_label VARCHAR(128) NULL COMMENT '字段中文名',
    before_value TEXT NULL COMMENT '变更前字段值',
    after_value TEXT NULL COMMENT '变更后字段值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据变更字段明细表';
```

字段明细表不是必须的。对于多数业务系统，单表的 `diff_json` 已经足够；只有当字段级检索、字段级统计、字段级审计非常频繁时，才建议增加明细表。

### 字段设计

字段设计应围绕业务定位、变更描述、数据快照、操作上下文和审计追踪展开。变更记录表的数据通常只追加，不建议被普通业务流程修改。

数据变更记录表字段建议如下：

| 字段             | 类型           | 必填 | 说明                                         |
| ---------------- | -------------- | ---- | -------------------------------------------- |
| `id`             | `BIGINT`       | 是   | 主键ID                                       |
| `biz_type`       | `VARCHAR(64)`  | 是   | 业务类型，用于区分合同、订单、商品等业务     |
| `biz_id`         | `BIGINT`       | 是   | 业务数据主键ID                               |
| `biz_no`         | `VARCHAR(128)` | 否   | 业务编号，方便人工检索                       |
| `table_name`     | `VARCHAR(128)` | 是   | 发生变更的业务表名                           |
| `change_type`    | `TINYINT`      | 是   | 新增、修改、删除、状态变更、导入、修复等类型 |
| `change_title`   | `VARCHAR(128)` | 否   | 变更标题，用于列表展示                       |
| `change_reason`  | `VARCHAR(512)` | 否   | 变更原因                                     |
| `changed_fields` | `JSON`         | 否   | 发生变化的字段名列表                         |
| `before_json`    | `JSON`         | 否   | 变更前数据                                   |
| `after_json`     | `JSON`         | 否   | 变更后数据                                   |
| `diff_json`      | `JSON`         | 否   | 字段差异数据                                 |
| `request_id`     | `VARCHAR(128)` | 否   | 请求ID，用于排查一次请求内的多条变更         |
| `trace_id`       | `VARCHAR(128)` | 否   | 链路追踪ID                                   |
| `operator_id`    | `BIGINT`       | 否   | 操作人ID                                     |
| `operator_name`  | `VARCHAR(128)` | 否   | 操作人名称快照                               |
| `operator_type`  | `TINYINT`      | 是   | 操作来源类型                                 |
| `client_ip`      | `VARCHAR(64)`  | 否   | 客户端IP                                     |
| `user_agent`     | `VARCHAR(512)` | 否   | 用户代理                                     |
| `remark`         | `VARCHAR(512)` | 否   | 备注                                         |
| `create_time`    | `DATETIME`     | 是   | 记录创建时间                                 |
| `deleted`        | `TINYINT`      | 是   | 逻辑删除标识                                 |

`biz_type` 建议使用稳定的英文编码，例如 `contract`、`order`、`product`、`user_account`。不要直接使用中文业务名称，避免后续系统集成、统计分析和代码判断不方便。

`changed_fields` 建议保存字段名数组，例如：

```json
[
  "contract_name",
  "contract_amount",
  "effective_time"
]
```

`diff_json` 建议保存字段级差异，格式保持统一，便于前端展示和后续解析。

```json
{
  "contract_name": {
    "label": "合同名称",
    "before": "年度采购合同",
    "after": "年度采购合同-修订版"
  },
  "contract_amount": {
    "label": "合同金额",
    "before": 100000.00,
    "after": 120000.00
  },
  "effective_time": {
    "label": "生效时间",
    "before": null,
    "after": "2026-05-13 00:00:00"
  }
}
```

对于新增操作，`before_json` 可以为空，`after_json` 保存新增后的完整数据。对于删除操作，`before_json` 保存删除前数据，`after_json` 可以为空。对于修改操作，`before_json` 和 `after_json` 都建议保存，`diff_json` 保存差异字段。

### 索引设计

索引设计应围绕业务对象追踪、操作人查询、时间范围查询、请求链路排查和变更类型筛选展开。数据变更记录表通常写入频率较高，索引数量需要控制，避免影响写入性能。

数据变更记录表建议索引如下：

```sql
ALTER TABLE sys_data_change_record
    ADD KEY idx_biz_type_biz_id_create_time (biz_type, biz_id, create_time),
    ADD KEY idx_biz_no_create_time (biz_no, create_time),
    ADD KEY idx_operator_id_create_time (operator_id, create_time),
    ADD KEY idx_change_type_create_time (change_type, create_time),
    ADD KEY idx_trace_id (trace_id),
    ADD KEY idx_request_id (request_id),
    ADD KEY idx_create_time (create_time);
```

`idx_biz_type_biz_id_create_time` 是最核心的查询索引，用于查询某个业务对象的全部变更记录。`idx_biz_no_create_time` 适合后台人员按业务编号检索。`idx_operator_id_create_time` 适合查询某个用户的操作记录。`idx_trace_id` 和 `idx_request_id` 适合线上问题排查。`idx_create_time` 适合按时间归档、清理和统计。

如果使用字段明细表，建议增加以下索引：

```sql
ALTER TABLE sys_data_change_field_record
    ADD KEY idx_change_record_id (change_record_id),
    ADD KEY idx_biz_type_biz_id_field_name (biz_type, biz_id, field_name),
    ADD KEY idx_field_name_create_time (field_name, create_time);
```

字段明细表的索引应谨慎添加。如果字段级查询不是核心需求，不建议创建字段明细表，也不建议为 JSON 字段设计复杂的函数索引。MySQL 8 支持 JSON 函数和生成列，但对于通用审计场景，优先使用普通结构化字段和必要的明细表更稳定。

### 常用查询

常用查询应优先使用普通列完成过滤，再按需读取 JSON 字段。列表页不建议直接返回完整 `before_json` 和 `after_json`，详情页再加载完整变更内容。

#### 查询某个业务对象的变更记录

该查询用于业务详情页中的“变更记录”或“操作历史”页签。

```sql
SELECT
    id,
    biz_type,
    biz_id,
    biz_no,
    change_type,
    change_title,
    change_reason,
    changed_fields,
    operator_id,
    operator_name,
    operator_type,
    create_time
FROM sys_data_change_record
WHERE biz_type = 'contract'
  AND biz_id = 10001
  AND deleted = 0
ORDER BY create_time DESC;
```

该查询依赖 `(biz_type, biz_id, create_time)` 索引，可以快速定位某个业务对象的变更历史。

#### 查询变更记录详情

该查询用于查看一次变更的完整前后数据和字段差异。

```sql
SELECT
    id,
    biz_type,
    biz_id,
    biz_no,
    table_name,
    change_type,
    change_title,
    change_reason,
    changed_fields,
    before_json,
    after_json,
    diff_json,
    request_id,
    trace_id,
    operator_id,
    operator_name,
    operator_type,
    client_ip,
    user_agent,
    remark,
    create_time
FROM sys_data_change_record
WHERE id = 90001
  AND deleted = 0;
```

详情查询可以返回完整 JSON。前端可以根据 `diff_json` 渲染字段差异，根据 `before_json` 和 `after_json` 提供完整数据查看能力。

#### 按业务编号查询变更记录

该查询适合后台管理人员根据合同编号、订单编号、商品编码等业务编号快速定位变更历史。

```sql
SELECT
    id,
    biz_type,
    biz_id,
    biz_no,
    change_type,
    change_title,
    operator_name,
    create_time
FROM sys_data_change_record
WHERE biz_no = 'HT202605130001'
  AND deleted = 0
ORDER BY create_time DESC;
```

如果不同业务类型可能出现相同编号，建议增加 `biz_type` 条件。

```sql
SELECT
    id,
    biz_type,
    biz_id,
    biz_no,
    change_type,
    change_title,
    operator_name,
    create_time
FROM sys_data_change_record
WHERE biz_type = 'contract'
  AND biz_no = 'HT202605130001'
  AND deleted = 0
ORDER BY create_time DESC;
```

#### 查询某个操作人的变更记录

该查询用于审计某个用户在一段时间内做过哪些数据修改。

```sql
SELECT
    id,
    biz_type,
    biz_id,
    biz_no,
    change_type,
    change_title,
    operator_id,
    operator_name,
    create_time
FROM sys_data_change_record
WHERE operator_id = 30001
  AND create_time >= '2026-05-01 00:00:00'
  AND create_time < '2026-06-01 00:00:00'
  AND deleted = 0
ORDER BY create_time DESC;
```

该查询依赖 `(operator_id, create_time)` 索引，适合审计后台、管理员操作追踪和安全排查。

#### 按请求ID查询一次请求内的数据变更

一次接口请求可能同时修改多张表或多条业务数据。通过 `request_id` 可以把这些变更记录关联起来。

```sql
SELECT
    id,
    biz_type,
    biz_id,
    biz_no,
    table_name,
    change_type,
    change_title,
    operator_name,
    create_time
FROM sys_data_change_record
WHERE request_id = 'REQ202605130001'
  AND deleted = 0
ORDER BY create_time ASC;
```

该查询适合排查一次业务操作产生了哪些数据写入，例如提交订单时同时修改订单表、库存表、账户流水表。

#### 按链路追踪ID查询变更记录

在微服务或多模块系统中，`trace_id` 可以把一次跨服务调用中的数据变更串联起来。

```sql
SELECT
    id,
    biz_type,
    biz_id,
    biz_no,
    table_name,
    change_type,
    change_title,
    request_id,
    trace_id,
    operator_name,
    create_time
FROM sys_data_change_record
WHERE trace_id = 'TRACE202605130001'
  AND deleted = 0
ORDER BY create_time ASC;
```

该查询适合线上问题排查，尤其是接口调用链路较长、多个服务共同参与数据写入的系统。

#### 查询指定字段的变更记录

如果只使用单表模型，可以通过 JSON 查询判断某个字段是否发生变化，但这种方式不适合高频大数据量查询。

```sql
SELECT
    id,
    biz_type,
    biz_id,
    biz_no,
    change_type,
    diff_json,
    operator_name,
    create_time
FROM sys_data_change_record
WHERE biz_type = 'contract'
  AND JSON_CONTAINS(changed_fields, JSON_QUOTE('contract_amount'))
  AND deleted = 0
ORDER BY create_time DESC;
```

如果字段级查询很频繁，建议使用字段明细表。

```sql
SELECT
    r.id,
    r.biz_type,
    r.biz_id,
    r.biz_no,
    r.change_type,
    f.field_name,
    f.field_label,
    f.before_value,
    f.after_value,
    r.operator_name,
    r.create_time
FROM sys_data_change_field_record f
JOIN sys_data_change_record r
  ON r.id = f.change_record_id
 AND r.deleted = 0
WHERE f.biz_type = 'contract'
  AND f.field_name = 'contract_amount'
  AND f.deleted = 0
ORDER BY f.create_time DESC;
```

对于审计平台、数据安全平台或字段敏感度较高的业务，字段明细表更适合长期维护。

#### 按时间范围查询变更记录

该查询适合按天审计、按月统计、归档扫描和数据排查。

```sql
SELECT
    id,
    biz_type,
    biz_id,
    biz_no,
    change_type,
    operator_id,
    operator_name,
    create_time
FROM sys_data_change_record
WHERE create_time >= '2026-05-01 00:00:00'
  AND create_time < '2026-05-14 00:00:00'
  AND deleted = 0
ORDER BY create_time ASC;
```

对于数据量较大的系统，应结合分页条件或按主键范围分批扫描，避免一次查询过多数据。

### 常用写入

数据变更记录的写入应尽量与业务数据写入保持同一事务，确保业务数据变更成功时记录也成功，业务数据回滚时记录也回滚。对于极高并发系统，也可以通过消息队列异步记录，但需要接受短暂延迟和补偿复杂度。

#### 新增数据时写入变更记录

新增业务数据时，`before_json` 为空，`after_json` 保存新增后的完整数据，`diff_json` 可以保存所有新增字段。

```sql
START TRANSACTION;

INSERT INTO biz_contract (
    id,
    contract_no,
    contract_name,
    customer_id,
    customer_name,
    contract_amount,
    contract_status,
    current_version_no,
    effective_time,
    remark,
    create_user,
    update_user
) VALUES (
    10001,
    'HT202605130001',
    '年度采购合同',
    20001,
    '杭州示例科技有限公司',
    100000.00,
    0,
    1,
    NULL,
    '初始创建',
    30001,
    30001
);

INSERT INTO sys_data_change_record (
    id,
    biz_type,
    biz_id,
    biz_no,
    table_name,
    change_type,
    change_title,
    change_reason,
    changed_fields,
    before_json,
    after_json,
    diff_json,
    request_id,
    trace_id,
    operator_id,
    operator_name,
    operator_type,
    client_ip
) VALUES (
    90001,
    'contract',
    10001,
    'HT202605130001',
    'biz_contract',
    1,
    '新增合同',
    '创建合同草稿',
    JSON_ARRAY('contract_no', 'contract_name', 'customer_id', 'customer_name', 'contract_amount', 'contract_status'),
    NULL,
    JSON_OBJECT(
        'id', 10001,
        'contractNo', 'HT202605130001',
        'contractName', '年度采购合同',
        'customerId', 20001,
        'customerName', '杭州示例科技有限公司',
        'contractAmount', 100000.00,
        'contractStatus', 0
    ),
    JSON_OBJECT(
        'contract_no', JSON_OBJECT('label', '合同编号', 'before', NULL, 'after', 'HT202605130001'),
        'contract_name', JSON_OBJECT('label', '合同名称', 'before', NULL, 'after', '年度采购合同'),
        'contract_amount', JSON_OBJECT('label', '合同金额', 'before', NULL, 'after', 100000.00)
    ),
    'REQ202605130001',
    'TRACE202605130001',
    30001,
    '张三',
    1,
    '192.168.1.10'
);

COMMIT;
```

新增记录的关键是把新增后的核心数据保存到 `after_json`，并记录操作人、请求ID和链路ID，方便后续追踪。

#### 修改数据时写入变更记录

修改业务数据时，建议先查询变更前数据，再执行更新，最后写入变更记录。实际应用层应先比较字段差异，只有发生实际变化时才写入记录。

```sql
START TRANSACTION;

SELECT
    id,
    contract_no,
    contract_name,
    customer_id,
    customer_name,
    contract_amount,
    contract_status,
    effective_time
FROM biz_contract
WHERE id = 10001
  AND deleted = 0
FOR UPDATE;

UPDATE biz_contract
SET
    contract_name = '年度采购合同-修订版',
    contract_amount = 120000.00,
    update_user = 30002,
    update_time = NOW()
WHERE id = 10001
  AND deleted = 0;

INSERT INTO sys_data_change_record (
    id,
    biz_type,
    biz_id,
    biz_no,
    table_name,
    change_type,
    change_title,
    change_reason,
    changed_fields,
    before_json,
    after_json,
    diff_json,
    request_id,
    trace_id,
    operator_id,
    operator_name,
    operator_type,
    client_ip
) VALUES (
    90002,
    'contract',
    10001,
    'HT202605130001',
    'biz_contract',
    2,
    '修改合同信息',
    '调整合同名称和合同金额',
    JSON_ARRAY('contract_name', 'contract_amount'),
    JSON_OBJECT(
        'contractName', '年度采购合同',
        'contractAmount', 100000.00
    ),
    JSON_OBJECT(
        'contractName', '年度采购合同-修订版',
        'contractAmount', 120000.00
    ),
    JSON_OBJECT(
        'contract_name', JSON_OBJECT('label', '合同名称', 'before', '年度采购合同', 'after', '年度采购合同-修订版'),
        'contract_amount', JSON_OBJECT('label', '合同金额', 'before', 100000.00, 'after', 120000.00)
    ),
    'REQ202605130002',
    'TRACE202605130002',
    30002,
    '李四',
    1,
    '192.168.1.11'
);

COMMIT;
```

该写法使用 `FOR UPDATE` 锁定业务记录，保证读取到的变更前数据与本次更新操作一致。实际业务中，`before_json` 和 `after_json` 可以保存完整对象，也可以只保存参与审计的关键字段，具体取决于审计要求和存储成本。

#### 状态变更时写入变更记录

状态变更通常是最常见的变更类型之一，例如订单支付、合同生效、商品上架、账号冻结等。

```sql
START TRANSACTION;

UPDATE biz_contract
SET
    contract_status = 2,
    effective_time = NOW(),
    update_user = 30003,
    update_time = NOW()
WHERE id = 10001
  AND contract_status = 0
  AND deleted = 0;

INSERT INTO sys_data_change_record (
    id,
    biz_type,
    biz_id,
    biz_no,
    table_name,
    change_type,
    change_title,
    change_reason,
    changed_fields,
    before_json,
    after_json,
    diff_json,
    request_id,
    trace_id,
    operator_id,
    operator_name,
    operator_type
) VALUES (
    90003,
    'contract',
    10001,
    'HT202605130001',
    'biz_contract',
    4,
    '合同生效',
    '合同审核通过后生效',
    JSON_ARRAY('contract_status', 'effective_time'),
    JSON_OBJECT(
        'contractStatus', 0,
        'effectiveTime', NULL
    ),
    JSON_OBJECT(
        'contractStatus', 2,
        'effectiveTime', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
    ),
    JSON_OBJECT(
        'contract_status', JSON_OBJECT('label', '合同状态', 'before', 0, 'after', 2),
        'effective_time', JSON_OBJECT('label', '生效时间', 'before', NULL, 'after', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'))
    ),
    'REQ202605130003',
    'TRACE202605130003',
    30003,
    '王五',
    1
);

COMMIT;
```

状态变更记录建议单独使用 `change_type = 4`，这样审计查询时可以快速筛选关键状态流转。

#### 删除数据时写入变更记录

删除业务数据时，建议优先使用逻辑删除，并保存删除前数据。

```sql
START TRANSACTION;

SELECT
    id,
    contract_no,
    contract_name,
    customer_id,
    customer_name,
    contract_amount,
    contract_status
FROM biz_contract
WHERE id = 10001
  AND deleted = 0
FOR UPDATE;

UPDATE biz_contract
SET
    deleted = 1,
    update_user = 30004,
    update_time = NOW()
WHERE id = 10001
  AND deleted = 0;

INSERT INTO sys_data_change_record (
    id,
    biz_type,
    biz_id,
    biz_no,
    table_name,
    change_type,
    change_title,
    change_reason,
    changed_fields,
    before_json,
    after_json,
    diff_json,
    request_id,
    trace_id,
    operator_id,
    operator_name,
    operator_type
) VALUES (
    90004,
    'contract',
    10001,
    'HT202605130001',
    'biz_contract',
    3,
    '删除合同',
    '管理员删除无效合同',
    JSON_ARRAY('deleted'),
    JSON_OBJECT(
        'id', 10001,
        'contractNo', 'HT202605130001',
        'contractName', '年度采购合同-修订版',
        'customerId', 20001,
        'customerName', '杭州示例科技有限公司',
        'contractAmount', 120000.00,
        'contractStatus', 2,
        'deleted', 0
    ),
    NULL,
    JSON_OBJECT(
        'deleted', JSON_OBJECT('label', '删除标识', 'before', 0, 'after', 1)
    ),
    'REQ202605130004',
    'TRACE202605130004',
    30004,
    '赵六',
    1
);

COMMIT;
```

删除记录的重点是保存删除前数据。对于重要业务，不建议物理删除后再尝试记录变更，因为物理删除后很难完整还原原始数据。

### 常见问题

数据变更记录模型的常见问题主要集中在记录粒度、写入时机、JSON 存储、敏感信息脱敏、性能影响和数据清理上。建模时需要提前定义统一规范，否则不同业务模块容易记录格式不一致。

| 问题                   | 说明                                       | 建议                                             |
| ---------------------- | ------------------------------------------ | ------------------------------------------------ |
| 是否所有字段都要记录   | 全量记录会增加存储成本，敏感字段也可能暴露 | 核心业务字段、敏感字段、状态字段优先记录         |
| 是否每次保存都写记录   | 没有实际变化也写记录会产生大量无效数据     | 应用层先比较差异，有变化再写入                   |
| JSON 是否适合长期查询  | JSON 适合保存快照，不适合高频复杂过滤      | 高频查询字段应设计为普通列或字段明细表           |
| 变更记录是否允许修改   | 修改审计数据会降低可信度                   | 原则上只追加，不允许普通业务修改                 |
| 敏感字段如何处理       | 身份证号、手机号、银行卡等不能直接明文扩散 | 写入前应脱敏或加密，必要时只记录摘要             |
| 写入性能是否会受影响   | 同步写审计记录会增加事务开销               | 普通业务同步写，极高并发场景可异步写并补偿       |
| 如何避免记录格式不统一 | 各模块手写 JSON 容易格式混乱               | 建议封装统一的数据变更记录组件                   |
| 记录保留多久           | 审计记录长期保存会形成大表                 | 根据合规要求设置保留周期，并结合归档数据模型处理 |

敏感字段需要重点处理。对于手机号、身份证号、银行卡号、密码、密钥、访问令牌等字段，不建议直接明文写入 `before_json`、`after_json` 或 `diff_json`。密码类字段通常不应记录原值，只记录“已修改”即可。

变更记录的写入方式需要结合业务等级选择。强审计业务建议与业务写入放在同一个事务中。普通操作日志类记录可以异步写入，但异步写入需要处理消息丢失、重复消费和补偿问题。

### 总结

数据变更记录模型适合记录一次业务写入操作产生的数据变化。它的核心是通过业务类型、业务ID、变更类型、操作人、请求ID、变更前数据、变更后数据和差异 JSON，还原一次数据修改的完整上下文。

建模时需要重点关注四点。第一，业务定位字段必须结构化，例如 `biz_type`、`biz_id`、`biz_no`。第二，变更差异格式必须统一，避免不同模块各写各的 JSON。第三，敏感字段必须脱敏、加密或避免记录原值。第四，变更记录原则上只追加，不应被普通业务流程修改。

该模型与历史版本模型可以互补使用。历史版本模型用于查看和恢复某个版本下的完整业务状态；数据变更记录模型用于审计一次操作改了哪些字段、谁改的、什么时候改的、为什么改的。对于合同、订单、账户、权限、配置等关键业务，建议同时保留历史版本和数据变更记录。