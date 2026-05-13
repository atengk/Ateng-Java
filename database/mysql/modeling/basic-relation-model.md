# 基础关系模型

基础关系模型用于描述业务实体之间最常见的数据组织方式，包括单表、主从表、一对一、一对多、多对多等模型。该类模型是 MySQL 8 业务建模中最基础、最常用的部分，适合用于规范表结构设计、字段边界、查询方式、写入方式以及后续扩展策略。

## 单表模型

单表模型是指一个业务对象的核心数据全部存储在一张表中，不依赖其他业务明细表或关系表即可完成主要业务表达。它通常用于结构简单、关系稳定、数据规模可控的业务场景，是大多数业务系统中最基础的数据建模方式。

### 适用场景

单表模型适合用于业务实体相对独立、字段数量可控、生命周期清晰、数据访问路径简单的场景。此类数据通常不需要复杂的明细拆分，也不需要通过中间表维护多方关系。

常见适用场景包括：

| 场景         | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 基础资料     | 如客户、供应商、员工、部门、岗位、门店等基础信息       |
| 简单业务对象 | 如公告、文章、分类、配置项、数据字典等                 |
| 状态型数据   | 如任务、工单、审批单、发布记录等具备明确状态流转的数据 |
| 配置类数据   | 如系统参数、业务规则、开关配置等低频变更数据           |
| 查询型数据   | 如列表页、详情页、筛选页主要依赖单表即可完成的数据     |

单表模型不适合用于明细数据较多、子记录变化频繁、字段高度动态、存在复杂多对多关系或单表数据量增长过快的场景。此类场景应优先考虑主从表模型、一对多模型、多对多模型或扩展字段模型。

### 建模结构

单表模型的核心结构是一张业务主表。表中通常包含主键字段、业务唯一字段、核心业务字段、状态字段、审计字段和删除标识字段。

以下示例以 `customer` 客户表为例，展示一个常见的单表建模结构。建模结构中只描述字段组织方式，不在本节展开索引设计。

```sql
CREATE TABLE customer (
    id BIGINT NOT NULL COMMENT '主键ID',
    customer_no VARCHAR(32) NOT NULL COMMENT '客户编号',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    customer_type TINYINT NOT NULL COMMENT '客户类型：1个人，2企业',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_user BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_user BIGINT DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户表';
```

该结构适合表达一个独立客户对象。业务主键使用 `id`，业务编号使用 `customer_no`，核心业务信息集中在客户名称、客户类型、联系方式、状态等字段中，审计字段用于记录创建和更新时间，`deleted` 字段用于支持软删除。

### 字段设计

字段设计应优先保证语义清晰、类型合理、边界明确。单表模型虽然结构简单，但字段设计不规范会直接影响查询、写入、扩展和维护成本。

| 字段类型 | 常用字段                                                   | 设计说明                                                |
| -------- | ---------------------------------------------------------- | ------------------------------------------------------- |
| 主键字段 | `id`                                                       | 建议使用 `BIGINT`，由雪花算法、号段模式或数据库自增生成 |
| 业务编号 | `customer_no`                                              | 用于业务识别，通常需要具备唯一性和可读性                |
| 名称字段 | `customer_name`                                            | 存储业务对象名称，长度按业务最大值预估                  |
| 类型字段 | `customer_type`                                            | 使用 `TINYINT` 表示枚举类型，避免直接存储中文含义       |
| 状态字段 | `status`                                                   | 用于控制启用、禁用、草稿、发布等状态                    |
| 联系字段 | `mobile`、`email`                                          | 根据业务需要设置唯一性、脱敏规则和校验规则              |
| 备注字段 | `remark`                                                   | 适合存储短文本说明，不建议承载复杂扩展数据              |
| 审计字段 | `create_user`、`create_time`、`update_user`、`update_time` | 用于记录数据创建和变更信息                              |
| 删除字段 | `deleted`                                                  | 用于软删除，避免业务数据被物理删除                      |

字段设计时应注意以下原则：

- 字段名使用小写字母和下划线，避免使用 MySQL 关键字。
- 金额字段建议使用 `DECIMAL`，不要使用 `FLOAT` 或 `DOUBLE`。
- 状态、类型、来源等枚举字段建议使用 `TINYINT` 或 `SMALLINT`。
- 时间字段建议使用 `DATETIME`，并明确业务时区处理方式。
- 字符串字段应根据业务长度设置，不建议所有字段都使用 `VARCHAR(255)`。
- 是否类字段建议使用 `TINYINT`，例如 `enabled`、`deleted`、`locked`。
- 大文本、JSON 扩展、附件列表等字段不宜直接塞入单表核心结构中，应结合扩展字段模型或附件资源模型设计。

### 索引设计

索引设计应围绕业务唯一性、查询条件、排序字段和数据过滤方式展开。单表模型不代表只需要主键索引，实际业务中通常还需要唯一索引和普通组合索引。

常见索引设计如下：

```sql
ALTER TABLE customer
    ADD UNIQUE KEY uk_customer_no (customer_no),
    ADD KEY idx_status_deleted (status, deleted),
    ADD KEY idx_customer_type_status (customer_type, status),
    ADD KEY idx_create_time (create_time);
```

索引说明：

| 索引                                               | 作用                             |
| -------------------------------------------------- | -------------------------------- |
| `PRIMARY KEY (id)`                                 | 按主键查询详情、更新、删除       |
| `uk_customer_no (customer_no)`                     | 保证客户编号唯一，支持按编号查询 |
| `idx_status_deleted (status, deleted)`             | 支持按状态和删除标识过滤         |
| `idx_customer_type_status (customer_type, status)` | 支持按客户类型和状态筛选         |
| `idx_create_time (create_time)`                    | 支持按创建时间排序或范围查询     |

设计索引时应避免以下问题：

- 不要给每个字段都单独建索引。
- 不要为了低频查询创建过多索引。
- 不要忽略软删除字段对查询条件的影响。
- 不要在高频写入表上创建大量冗余索引。
- 组合索引应遵循最左前缀原则，并结合实际 `WHERE` 条件顺序设计。
- 分页查询如果经常按时间倒序展示，应考虑时间字段和过滤字段的组合索引。

### 常用查询

常用查询应围绕详情查询、列表查询、条件筛选、分页查询、唯一校验和状态过滤展开。单表模型的查询通常较直接，但仍需要注意查询条件是否命中索引，以及是否过滤软删除数据。

#### 按主键查询详情

按主键查询是单表模型最常见的详情查询方式，通常用于编辑页、详情页、数据回显等场景。

```sql
SELECT
    id,
    customer_no,
    customer_name,
    customer_type,
    mobile,
    email,
    status,
    remark,
    create_time,
    update_time
FROM customer
WHERE id = 10001
  AND deleted = 0;
```

该查询依赖主键定位数据，同时增加 `deleted = 0` 过滤已删除记录。

#### 按业务编号查询

业务编号通常用于接口幂等、外部系统对接、业务单据识别等场景。

```sql
SELECT
    id,
    customer_no,
    customer_name,
    customer_type,
    status
FROM customer
WHERE customer_no = 'CUST202605130001'
  AND deleted = 0;
```

如果 `customer_no` 设置了唯一索引，该查询可以快速定位单条业务数据。

#### 按状态查询可用数据

状态过滤常用于下拉选择、启用数据列表、后台管理筛选等场景。

```sql
SELECT
    id,
    customer_no,
    customer_name
FROM customer
WHERE status = 1
  AND deleted = 0
ORDER BY create_time DESC
LIMIT 20;
```

该查询适合配合 `status`、`deleted`、`create_time` 等字段设计组合索引。

#### 按条件分页查询

分页查询是后台管理页面最常见的查询方式。查询条件通常包括名称模糊匹配、类型筛选、状态筛选和时间范围筛选。

```sql
SELECT
    id,
    customer_no,
    customer_name,
    customer_type,
    mobile,
    status,
    create_time
FROM customer
WHERE deleted = 0
  AND customer_type = 2
  AND status = 1
  AND create_time >= '2026-01-01 00:00:00'
  AND create_time < '2026-02-01 00:00:00'
ORDER BY create_time DESC
LIMIT 0, 20;
```

当数据量较大时，应避免深分页。如果页码很深，可以改用基于游标的分页方式。

```sql
SELECT
    id,
    customer_no,
    customer_name,
    customer_type,
    status,
    create_time
FROM customer
WHERE deleted = 0
  AND status = 1
  AND id < 100000
ORDER BY id DESC
LIMIT 20;
```

游标分页适合移动端滚动加载、消息列表、日志列表等场景。

#### 名称模糊查询

名称模糊查询常用于后台检索，但需要注意 `LIKE '%关键词%'` 通常无法有效使用普通 BTree 索引。

```sql
SELECT
    id,
    customer_no,
    customer_name,
    customer_type,
    status
FROM customer
WHERE deleted = 0
  AND customer_name LIKE CONCAT('%', '科技', '%')
ORDER BY create_time DESC
LIMIT 20;
```

如果模糊查询频率较高，应考虑搜索辅助表、全文索引或 Elasticsearch 等搜索方案，不建议只依赖普通索引解决所有模糊搜索问题。

#### 唯一性校验查询

新增或修改数据前，经常需要校验业务编号、手机号、名称等字段是否已存在。

```sql
SELECT COUNT(1)
FROM customer
WHERE customer_no = 'CUST202605130001'
  AND deleted = 0;
```

修改时应排除当前记录。

```sql
SELECT COUNT(1)
FROM customer
WHERE customer_no = 'CUST202605130001'
  AND id <> 10001
  AND deleted = 0;
```

如果字段有唯一性要求，应优先通过数据库唯一索引保证最终一致性，应用层校验只能作为友好提示。

### 常用写入

常用写入包括新增、修改、状态变更、软删除和批量写入。单表模型写入逻辑通常比较简单，但仍应保证唯一性、状态合法性和并发更新安全。

#### 新增数据

新增数据时应写入业务编号、核心字段、状态字段和审计字段。

```sql
INSERT INTO customer (
    id,
    customer_no,
    customer_name,
    customer_type,
    mobile,
    email,
    status,
    remark,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES (
    10001,
    'CUST202605130001',
    '杭州示例科技有限公司',
    2,
    '13800000000',
    'contact@example.com',
    1,
    '企业客户',
    1,
    NOW(),
    1,
    NOW(),
    0
);
```

新增时应避免依赖应用层生成重复业务编号。如果业务编号必须唯一，应配合唯一索引兜底。

#### 修改基础信息

修改数据时建议只更新允许变更的字段，不要无差别覆盖整行数据。

```sql
UPDATE customer
SET customer_name = '杭州示例科技股份有限公司',
    mobile = '13900000000',
    email = 'service@example.com',
    remark = '客户名称已变更',
    update_user = 1,
    update_time = NOW()
WHERE id = 10001
  AND deleted = 0;
```

该写法可以避免误更新已删除数据。

#### 修改状态

状态变更应单独处理，避免和普通编辑逻辑混在一起。状态变更前应校验当前状态是否允许流转。

```sql
UPDATE customer
SET status = 0,
    update_user = 1,
    update_time = NOW()
WHERE id = 10001
  AND status = 1
  AND deleted = 0;
```

通过在 `WHERE` 条件中加入原状态，可以降低并发状态变更导致的数据异常。

#### 软删除数据

业务系统中通常优先使用软删除，以便保留历史数据、审计记录和关联数据完整性。

```sql
UPDATE customer
SET deleted = 1,
    update_user = 1,
    update_time = NOW()
WHERE id = 10001
  AND deleted = 0;
```

软删除后，所有业务查询都应默认追加 `deleted = 0` 条件。

#### 批量写入数据

批量写入适合初始化数据、导入数据或同步数据场景。批量写入时应控制单批数据量，避免单条 SQL 过大。

```sql
INSERT INTO customer (
    id,
    customer_no,
    customer_name,
    customer_type,
    status,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES
    (10002, 'CUST202605130002', '上海示例贸易有限公司', 2, 1, 1, NOW(), 1, NOW(), 0),
    (10003, 'CUST202605130003', '北京示例服务有限公司', 2, 1, 1, NOW(), 1, NOW(), 0),
    (10004, 'CUST202605130004', '深圳示例信息有限公司', 2, 1, 1, NOW(), 1, NOW(), 0);
```

批量导入时应优先在应用层进行基础校验，并通过数据库唯一索引处理并发或重复导入问题。

### 常见问题

单表模型虽然简单，但在业务演进过程中容易出现字段膨胀、索引混乱、查询性能下降和职责边界不清等问题。

| 问题                   | 说明                                 | 建议                                          |
| ---------------------- | ------------------------------------ | --------------------------------------------- |
| 字段越来越多           | 不同业务不断向同一张表追加字段       | 区分核心字段和扩展字段，必要时拆分扩展表      |
| 查询条件越来越复杂     | 一个列表页承载过多筛选条件           | 梳理高频查询场景，针对核心查询设计索引        |
| 模糊查询性能差         | 使用 `LIKE '%keyword%'` 检索大表     | 使用搜索辅助表、全文索引或搜索引擎            |
| 状态字段含义混乱       | 一个状态字段表达多个业务维度         | 拆分为状态、审核状态、发布状态等独立字段      |
| 软删除数据影响唯一约束 | 删除后再次新增相同业务编号失败       | 根据业务决定是否允许复用，谨慎设计唯一索引    |
| 大字段拖慢查询         | 单表中包含大文本、JSON、附件信息     | 将大字段拆到扩展表，列表查询只查必要字段      |
| 深分页性能下降         | 使用 `LIMIT 100000, 20` 查询大偏移量 | 使用游标分页或基于业务条件限制查询范围        |
| 写入冲突               | 多个请求同时更新同一行数据           | 使用乐观锁字段或在 `WHERE` 条件中加入状态约束 |

单表模型出现明显复杂化后，应及时评估是否需要演进为主从表模型、一对多模型、宽表模型、JSON 扩展字段模型或历史版本模型。

### 总结

单表模型是 MySQL 8 业务建模中最基础的关系模型，适合表达结构清晰、关系简单、生命周期明确的业务对象。设计单表时，应重点关注字段语义、主键策略、业务唯一性、状态表达、审计字段和软删除规则。

在实际业务中，单表模型不应被过度使用。当字段持续膨胀、查询条件复杂、数据规模快速增长或业务关系变得复杂时，应及时拆分模型边界，选择更合适的关系模型。良好的单表设计应做到结构简单、职责单一、查询清晰、写入安全，并为后续模型演进保留空间。



## 主从表模型

主从表模型用于表达一个主业务对象与一组明细数据之间的关系。主表保存业务单据、业务对象或聚合根的核心信息，从表保存与主记录强绑定的明细行数据。该模型常用于订单、采购单、入库单、出库单、报销单、审批单等业务场景，是业务系统中非常常见的关系建模方式。

### 适用场景

主从表模型适合用于一个主业务对象下存在多条明细记录，且明细记录通常不脱离主记录单独存在的场景。主表负责表达整体业务状态，从表负责表达具体行项目、明细项、子数据或过程数据。

常见适用场景包括：

| 场景             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 订单与订单明细   | 订单表保存订单号、客户、金额、状态，订单明细表保存商品、数量、单价 |
| 采购单与采购明细 | 采购单表保存供应商、采购日期、状态，采购明细表保存物料、数量、价格 |
| 入库单与入库明细 | 入库单表保存仓库、入库类型、经办人，入库明细表保存具体入库商品 |
| 报销单与费用明细 | 报销单表保存申请人、总金额、审批状态，费用明细表保存费用项目 |
| 审批单与审批记录 | 审批单表保存业务主体，审批记录表保存每个节点的审批动作       |
| 发票与发票明细   | 发票主表保存发票抬头、金额、状态，明细表保存商品或服务项目   |

主从表模型不适合用于从表数据可以独立存在、从表需要被多个主表共享、或者主从关系本质上是多对多关系的场景。此类场景应优先考虑一对多模型、多对多模型或独立业务表模型。

### 建模结构

主从表模型通常由一张主表和一张或多张从表组成。主表使用主键标识业务主体，从表通过主表主键建立归属关系。主表负责保存整体状态、汇总金额、业务编号和审计字段，从表负责保存明细行数据。

以下示例以订单和订单明细为例，展示主从表模型的常见结构。

```sql
CREATE TABLE sales_order (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    order_status TINYINT NOT NULL DEFAULT 10 COMMENT '订单状态：10待确认，20已确认，30已完成，40已取消',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '应付金额',
    order_time DATETIME NOT NULL COMMENT '下单时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_user BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_user BIGINT DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售订单表';

CREATE TABLE sales_order_item (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_code VARCHAR(32) NOT NULL COMMENT '商品编码',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    quantity DECIMAL(18, 4) NOT NULL COMMENT '购买数量',
    unit_price DECIMAL(18, 2) NOT NULL COMMENT '商品单价',
    item_amount DECIMAL(18, 2) NOT NULL COMMENT '明细金额',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售订单明细表';
```

该结构中，`sales_order` 是主表，负责保存订单整体信息；`sales_order_item` 是从表，负责保存订单中的商品明细。`order_id` 是从表关联主表的核心字段，`order_no` 属于冗余字段，便于排查问题、导出数据和部分业务查询。

### 字段设计

主从表模型的字段设计应明确主表和从表的职责边界。主表不要保存过多明细级字段，从表也不要重复保存大量主表字段，必要的冗余字段应有明确的查询或审计价值。

主表常见字段如下：

| 字段类型 | 常用字段                                                   | 设计说明                                         |
| -------- | ---------------------------------------------------------- | ------------------------------------------------ |
| 主键字段 | `id`                                                       | 主表唯一标识，供从表关联使用                     |
| 业务编号 | `order_no`                                                 | 面向业务的唯一编号，适合展示、查询和外部系统对接 |
| 业务主体 | `customer_id`、`customer_name`                             | 保存主业务对象关联方信息                         |
| 状态字段 | `order_status`                                             | 表达整张单据或主对象的生命周期状态               |
| 汇总字段 | `total_amount`、`pay_amount`                               | 保存从表计算后的汇总结果                         |
| 时间字段 | `order_time`                                               | 业务发生时间，与创建时间区分                     |
| 审计字段 | `create_user`、`create_time`、`update_user`、`update_time` | 记录数据创建和修改信息                           |
| 删除字段 | `deleted`                                                  | 支持软删除                                       |

从表常见字段如下：

| 字段类型 | 常用字段                                     | 设计说明                         |
| -------- | -------------------------------------------- | -------------------------------- |
| 主键字段 | `id`                                         | 明细行唯一标识                   |
| 关联字段 | `order_id`                                   | 关联主表主键                     |
| 冗余编号 | `order_no`                                   | 冗余主表业务编号，便于查询和排查 |
| 明细对象 | `product_id`、`product_code`、`product_name` | 保存明细行对应的商品或资源信息   |
| 数量金额 | `quantity`、`unit_price`、`item_amount`      | 保存明细行计算数据               |
| 排序字段 | `sort_no`                                    | 控制明细展示顺序                 |
| 审计字段 | `create_time`、`update_time`                 | 记录明细行创建和更新时间         |
| 删除字段 | `deleted`                                    | 支持明细行软删除                 |

字段设计时应注意以下原则：

- 主表保存整体信息，从表保存明细信息，避免职责混乱。
- 从表必须保存主表关联字段，例如 `order_id`。
- 从表可以适度冗余主表业务编号，例如 `order_no`，但不应大量复制主表字段。
- 金额字段使用 `DECIMAL`，并明确精度和小数位。
- 数量字段如果涉及小数，应使用 `DECIMAL`，不要使用 `FLOAT` 或 `DOUBLE`。
- 主表汇总字段应由从表计算得出，写入时要保证主从数据一致。
- 状态字段通常放在主表中，从表只有在明细独立存在状态流转时才需要状态字段。
- 从表数量较大时，应避免在从表中存储大文本、JSON 或无边界扩展字段。

### 索引设计

主从表模型的索引设计应同时考虑主表查询和从表关联查询。主表重点关注业务编号、状态、时间和业务主体；从表重点关注主表关联字段、明细对象字段和排序字段。

常见索引设计如下：

```sql
ALTER TABLE sales_order
    ADD UNIQUE KEY uk_order_no (order_no),
    ADD KEY idx_customer_status (customer_id, order_status),
    ADD KEY idx_order_status_time (order_status, order_time),
    ADD KEY idx_create_time (create_time);

ALTER TABLE sales_order_item
    ADD KEY idx_order_id (order_id),
    ADD KEY idx_order_id_sort (order_id, sort_no),
    ADD KEY idx_product_id (product_id),
    ADD KEY idx_order_no (order_no);
```

索引说明：

| 表                 | 索引                    | 作用                                 |
| ------------------ | ----------------------- | ------------------------------------ |
| `sales_order`      | `uk_order_no`           | 保证订单编号唯一，支持按订单编号查询 |
| `sales_order`      | `idx_customer_status`   | 支持查询某个客户在指定状态下的订单   |
| `sales_order`      | `idx_order_status_time` | 支持按状态和订单时间筛选             |
| `sales_order`      | `idx_create_time`       | 支持后台按创建时间排序或范围查询     |
| `sales_order_item` | `idx_order_id`          | 支持根据主表 ID 查询明细             |
| `sales_order_item` | `idx_order_id_sort`     | 支持根据主表 ID 查询并按明细顺序展示 |
| `sales_order_item` | `idx_product_id`        | 支持根据商品反查订单明细             |
| `sales_order_item` | `idx_order_no`          | 支持根据订单编号定位明细数据         |

设计索引时应注意以下问题：

- 从表必须为主表关联字段建立索引，否则主从关联查询会出现性能问题。
- 如果明细需要按固定顺序展示，可以设计 `(order_id, sort_no)` 组合索引。
- 如果经常从商品、物料、费用项目等维度反查明细，应为对应字段建立索引。
- 主表列表查询应结合状态、业务主体、时间范围设计组合索引。
- 不要为从表所有字段都建索引，明细表写入频率通常较高，过多索引会降低写入性能。
- 如果主从数据量很大，应结合分区表、归档模型或分库分表模型进一步设计。

### 常用查询

主从表模型的查询通常包括主表详情查询、从表明细查询、主从联合查询、分页查询、汇总查询和反向查询。实际业务中应尽量避免一次查询返回过多明细数据，尤其是主表分页查询时，不建议直接关联大明细表进行分页。

#### 按主键查询主表详情

该查询用于获取订单主信息，常见于详情页、编辑页和接口回显。

```sql
SELECT
    id,
    order_no,
    customer_id,
    customer_name,
    order_status,
    total_amount,
    pay_amount,
    order_time,
    remark,
    create_time,
    update_time
FROM sales_order
WHERE id = 10001
  AND deleted = 0;
```

主表详情查询应优先通过主键或业务编号定位单条数据。

#### 按订单编号查询主表详情

业务编号适合对外接口、运营后台和客服系统使用。

```sql
SELECT
    id,
    order_no,
    customer_id,
    customer_name,
    order_status,
    total_amount,
    pay_amount,
    order_time
FROM sales_order
WHERE order_no = 'SO202605130001'
  AND deleted = 0;
```

如果订单编号具有唯一性，应通过唯一索引保证查询效率和数据一致性。

#### 查询订单明细

该查询用于订单详情页展示明细列表。

```sql
SELECT
    id,
    order_id,
    product_id,
    product_code,
    product_name,
    quantity,
    unit_price,
    item_amount,
    sort_no
FROM sales_order_item
WHERE order_id = 10001
  AND deleted = 0
ORDER BY sort_no ASC, id ASC;
```

从表查询必须带上 `order_id`，避免扫描整张明细表。

#### 查询主表并关联明细

当需要一次性返回主表和明细数据时，可以使用关联查询。该方式适合查询单个主表对象，不适合直接用于大分页列表。

```sql
SELECT
    o.id AS order_id,
    o.order_no,
    o.customer_id,
    o.customer_name,
    o.order_status,
    o.total_amount,
    o.pay_amount,
    o.order_time,
    i.id AS item_id,
    i.product_id,
    i.product_code,
    i.product_name,
    i.quantity,
    i.unit_price,
    i.item_amount,
    i.sort_no
FROM sales_order o
LEFT JOIN sales_order_item i ON i.order_id = o.id AND i.deleted = 0
WHERE o.id = 10001
  AND o.deleted = 0
ORDER BY i.sort_no ASC, i.id ASC;
```

如果主表对应的明细行较多，应考虑分两次查询：先查主表，再按 `order_id` 查询从表。

#### 主表分页查询

后台订单列表通常只查询主表，不直接关联从表，避免分页结果被明细行放大。

```sql
SELECT
    id,
    order_no,
    customer_id,
    customer_name,
    order_status,
    total_amount,
    pay_amount,
    order_time,
    create_time
FROM sales_order
WHERE deleted = 0
  AND order_status = 20
  AND order_time >= '2026-05-01 00:00:00'
  AND order_time < '2026-06-01 00:00:00'
ORDER BY order_time DESC, id DESC
LIMIT 0, 20;
```

主表分页查询应只返回列表页需要的字段，详情和明细应在用户进入详情页后再查询。

#### 查询某个客户的订单

该查询用于客户详情页、客户交易记录和对账场景。

```sql
SELECT
    id,
    order_no,
    order_status,
    total_amount,
    pay_amount,
    order_time
FROM sales_order
WHERE customer_id = 20001
  AND deleted = 0
ORDER BY order_time DESC
LIMIT 20;
```

如果该查询频率较高，应结合 `customer_id`、`order_status`、`order_time` 设计组合索引。

#### 根据明细反查主表

该查询用于根据商品、物料或费用项目反查关联的主业务单据。

```sql
SELECT DISTINCT
    o.id,
    o.order_no,
    o.customer_id,
    o.customer_name,
    o.order_status,
    o.order_time
FROM sales_order_item i
INNER JOIN sales_order o ON o.id = i.order_id AND o.deleted = 0
WHERE i.product_id = 30001
  AND i.deleted = 0
ORDER BY o.order_time DESC
LIMIT 20;
```

该类查询应谨慎使用。如果从表数据量很大，应确保从表反查字段具备合适索引。

#### 汇总明细金额

该查询用于校验主表汇总金额是否与从表明细金额一致。

```sql
SELECT
    order_id,
    SUM(item_amount) AS total_item_amount
FROM sales_order_item
WHERE order_id = 10001
  AND deleted = 0
GROUP BY order_id;
```

主表中的汇总金额通常在写入时计算并落库，查询时不建议每次都实时聚合大明细表。

#### 查询主从金额不一致的数据

该查询用于数据巡检、对账和修复前排查。

```sql
SELECT
    o.id,
    o.order_no,
    o.total_amount,
    IFNULL(t.total_item_amount, 0.00) AS total_item_amount
FROM sales_order o
LEFT JOIN (
    SELECT
        order_id,
        SUM(item_amount) AS total_item_amount
    FROM sales_order_item
    WHERE deleted = 0
    GROUP BY order_id
) t ON t.order_id = o.id
WHERE o.deleted = 0
  AND o.total_amount <> IFNULL(t.total_item_amount, 0.00);
```

该查询不适合高频在线接口，应更多用于离线巡检或后台管理。

### 常用写入

主从表模型的写入必须关注主表和从表的一致性。新增、修改、删除通常需要在同一个事务中完成，避免出现主表存在但明细缺失，或明细存在但主表写入失败的情况。

#### 新增主从数据

新增订单时，应先写入主表，再批量写入从表。主表汇总金额应由明细计算得出。

```sql
START TRANSACTION;

INSERT INTO sales_order (
    id,
    order_no,
    customer_id,
    customer_name,
    order_status,
    total_amount,
    pay_amount,
    order_time,
    remark,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES (
    10001,
    'SO202605130001',
    20001,
    '杭州示例科技有限公司',
    10,
    299.00,
    299.00,
    NOW(),
    '首次下单',
    1,
    NOW(),
    1,
    NOW(),
    0
);

INSERT INTO sales_order_item (
    id,
    order_id,
    order_no,
    product_id,
    product_code,
    product_name,
    quantity,
    unit_price,
    item_amount,
    sort_no,
    create_time,
    update_time,
    deleted
) VALUES
    (50001, 10001, 'SO202605130001', 30001, 'SKU001', '示例商品A', 2.0000, 99.50, 199.00, 1, NOW(), NOW(), 0),
    (50002, 10001, 'SO202605130001', 30002, 'SKU002', '示例商品B', 1.0000, 100.00, 100.00, 2, NOW(), NOW(), 0);

COMMIT;
```

实际业务代码中应使用数据库事务控制主从写入，任意一步失败都应回滚。

#### 修改主表基础信息

修改主表信息时，只更新主表自身字段，不应影响从表明细。

```sql
UPDATE sales_order
SET remark = '客户要求尽快发货',
    update_user = 1,
    update_time = NOW()
WHERE id = 10001
  AND order_status = 10
  AND deleted = 0;
```

在 `WHERE` 条件中加入状态限制，可以避免已确认、已完成或已取消订单被错误修改。

#### 替换明细数据

当编辑订单明细时，常见做法是先软删除原明细，再重新插入新明细，并同步更新主表汇总金额。

```sql
START TRANSACTION;

UPDATE sales_order_item
SET deleted = 1,
    update_time = NOW()
WHERE order_id = 10001
  AND deleted = 0;

INSERT INTO sales_order_item (
    id,
    order_id,
    order_no,
    product_id,
    product_code,
    product_name,
    quantity,
    unit_price,
    item_amount,
    sort_no,
    create_time,
    update_time,
    deleted
) VALUES
    (50003, 10001, 'SO202605130001', 30001, 'SKU001', '示例商品A', 3.0000, 99.50, 298.50, 1, NOW(), NOW(), 0),
    (50004, 10001, 'SO202605130001', 30003, 'SKU003', '示例商品C', 1.0000, 80.00, 80.00, 2, NOW(), NOW(), 0);

UPDATE sales_order
SET total_amount = 378.50,
    pay_amount = 378.50,
    update_user = 1,
    update_time = NOW()
WHERE id = 10001
  AND order_status = 10
  AND deleted = 0;

COMMIT;
```

该方式实现简单，适合明细数量不大、编辑频率不高的场景。如果明细数量很大，应改为对比新增、修改和删除差异后增量更新。

#### 修改订单状态

状态变更通常只发生在主表。状态变更时应校验当前状态，避免重复提交或非法流转。

```sql
UPDATE sales_order
SET order_status = 20,
    update_user = 1,
    update_time = NOW()
WHERE id = 10001
  AND order_status = 10
  AND deleted = 0;
```

如果影响行数为 `0`，说明订单不存在、已删除或状态不满足变更条件，业务层应返回明确提示。

#### 软删除主从数据

删除主从数据时，应在同一个事务中软删除主表和从表。

```sql
START TRANSACTION;

UPDATE sales_order
SET deleted = 1,
    update_user = 1,
    update_time = NOW()
WHERE id = 10001
  AND deleted = 0;

UPDATE sales_order_item
SET deleted = 1,
    update_time = NOW()
WHERE order_id = 10001
  AND deleted = 0;

COMMIT;
```

不建议只删除主表而保留有效明细，否则会形成业务上的孤儿数据。

#### 根据明细重新计算主表汇总金额

当需要修复或重新计算汇总字段时，可以通过从表聚合结果更新主表。

```sql
UPDATE sales_order o
INNER JOIN (
    SELECT
        order_id,
        SUM(item_amount) AS total_item_amount
    FROM sales_order_item
    WHERE order_id = 10001
      AND deleted = 0
    GROUP BY order_id
) t ON t.order_id = o.id
SET o.total_amount = t.total_item_amount,
    o.pay_amount = t.total_item_amount,
    o.update_time = NOW()
WHERE o.id = 10001
  AND o.deleted = 0;
```

该方式适合数据修复、后台重算和部分异常补偿场景，不建议替代正常业务写入流程。

### 常见问题

主从表模型的主要问题通常集中在一致性、查询性能、明细膨胀、汇总字段维护和误用关联查询上。

| 问题                 | 说明                                         | 建议                                               |
| -------------------- | -------------------------------------------- | -------------------------------------------------- |
| 主表和从表数据不一致 | 主表写入成功，从表写入失败，或汇总金额不一致 | 主从写入必须放在同一个事务中                       |
| 从表缺少关联索引     | 查询明细时扫描整张从表                       | 为 `order_id` 或其他主表关联字段建立索引           |
| 主表分页关联从表     | 一条主表记录被多条明细放大，导致分页异常     | 主表分页和明细查询分开处理                         |
| 汇总金额实时计算     | 每次查询都聚合从表金额                       | 写入时计算汇总金额并保存到主表                     |
| 明细被物理删除       | 删除后无法审计和追溯                         | 优先使用软删除，必要时保留变更记录                 |
| 从表冗余字段过多     | 主表字段大量复制到从表                       | 只冗余查询、审计或排查问题必要的字段               |
| 状态控制混乱         | 主表和从表都维护状态，但含义不清             | 默认由主表维护整体状态，从表仅在必要时维护独立状态 |
| 大明细查询慢         | 单个主表下明细数量过大                       | 分页查询明细，或按业务拆分明细类型                 |
| 并发修改覆盖         | 多人同时编辑主从数据导致后提交覆盖先提交     | 使用状态条件、版本号或业务锁控制并发               |
| 删除主表后明细仍有效 | 查询明细时仍能查到已删除主表对应数据         | 删除主表时同步软删除从表，查询时校验主表状态       |

当主从表模型开始承载复杂流程、频繁变更、历史追踪或多维查询时，应结合状态机模型、历史版本模型、数据变更记录模型、库存流水模型或搜索辅助表模型进行扩展。

### 总结

主从表模型适合表达一个主业务对象和多条明细数据之间的强绑定关系。主表负责保存业务主体、状态、汇总和审计信息，从表负责保存具体明细行。该模型结构清晰、表达能力强，是订单、采购、库存、报销、审批等业务中最常用的建模方式之一。

设计主从表模型时，应重点关注主从职责边界、关联字段、事务一致性、汇总字段维护、从表索引和查询方式。主表分页不应直接关联大明细表，主从写入必须通过事务保证一致性。从表数据量持续增长后，应进一步考虑归档、分区、分库分表或读模型优化。

## 一对一模型

一对一模型用于表达一个主业务对象与一个扩展对象之间的唯一对应关系。主表保存高频访问、核心业务字段；扩展表保存低频访问、敏感信息、大字段、可选信息或变化边界不同的数据。该模型常用于用户与用户资料、员工与员工档案、客户与客户资质、订单与订单扩展信息等场景。

### 适用场景

一对一模型适合用于同一个业务对象的数据可以被拆分为核心信息和扩展信息的场景。它不是为了表达多个业务实体之间的复杂关系，而是为了让同一个业务对象在存储结构上具备更清晰的职责边界。

常见适用场景包括：

| 场景           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 用户与用户资料 | 用户表保存账号、手机号、状态，资料表保存头像、性别、生日、简介 |
| 员工与员工档案 | 员工表保存工号、姓名、部门，档案表保存身份证、住址、紧急联系人 |
| 客户与客户资质 | 客户表保存客户名称、类型、状态，资质表保存营业执照、法人信息、认证信息 |
| 订单与订单扩展 | 订单表保存核心交易数据，扩展表保存发票、配送、备注、渠道扩展字段 |
| 商品与商品详情 | 商品表保存名称、价格、状态，详情表保存富文本介绍、规格说明、售后说明 |
| 账号与安全配置 | 账号表保存登录身份，安全表保存密码策略、二次验证、登录限制   |

一对一模型通常用于解决单表字段过多、冷热字段混杂、敏感字段隔离、大字段影响查询性能、业务扩展字段边界不清等问题。

不适合使用一对一模型的情况包括：

- 扩展数据会出现多条记录，应使用一对多模型。
- 两个对象本身可以独立存在，应设计为独立业务表。
- 两个对象之间存在多方绑定关系，应使用多对多模型。
- 只是为了过度拆表，但字段数量和访问压力都很小，不需要强行拆分。

### 建模结构

一对一模型通常由一张主表和一张扩展表组成。主表保存核心字段，扩展表使用与主表相同的主键值，形成一条主表记录最多对应一条扩展表记录的结构。

以下示例以会员和会员资料为例，展示一对一模型的常见结构。`member` 表保存高频访问的会员核心信息，`member_profile` 表保存低频访问的会员资料信息。

```sql
CREATE TABLE member (
    id BIGINT NOT NULL COMMENT '主键ID',
    member_no VARCHAR(32) NOT NULL COMMENT '会员编号',
    nickname VARCHAR(100) NOT NULL COMMENT '会员昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    member_level TINYINT NOT NULL DEFAULT 1 COMMENT '会员等级：1普通，2银卡，3金卡，4黑卡',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    register_time DATETIME NOT NULL COMMENT '注册时间',
    create_user BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_user BIGINT DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员表';

CREATE TABLE member_profile (
    member_id BIGINT NOT NULL COMMENT '会员ID，与会员表主键一致',
    real_name VARCHAR(100) DEFAULT NULL COMMENT '真实姓名',
    gender TINYINT DEFAULT NULL COMMENT '性别：0未知，1男，2女',
    birthday DATE DEFAULT NULL COMMENT '生日',
    avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    province_code VARCHAR(20) DEFAULT NULL COMMENT '省份编码',
    city_code VARCHAR(20) DEFAULT NULL COMMENT '城市编码',
    address VARCHAR(300) DEFAULT NULL COMMENT '详细地址',
    introduction VARCHAR(1000) DEFAULT NULL COMMENT '个人简介',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员资料表';
```

该结构采用共享主键方式建模。`member_profile.member_id` 既是扩展表主键，也是关联 `member.id` 的字段。这样可以天然保证一个会员最多只有一条资料记录，结构清晰，查询路径稳定。

另一种常见结构是扩展表使用独立主键，并通过 `member_id` 关联主表。该方式更灵活，但需要额外保证 `member_id` 的唯一性。

```sql
CREATE TABLE member_security (
    id BIGINT NOT NULL COMMENT '主键ID',
    member_id BIGINT NOT NULL COMMENT '会员ID',
    login_password VARCHAR(255) DEFAULT NULL COMMENT '登录密码密文',
    password_update_time DATETIME DEFAULT NULL COMMENT '密码更新时间',
    two_factor_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否开启二次验证：0否，1是',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员安全配置表';
```

共享主键方式更适合强一对一扩展数据，独立主键方式更适合后续可能演进为多条记录或需要单独管理扩展对象生命周期的场景。

### 字段设计

一对一模型的字段设计重点在于拆分边界，而不是简单把一张表拆成两张表。主表字段应稳定、高频、核心，扩展表字段应低频、可选、敏感或体积较大。

主表常见字段如下：

| 字段类型 | 常用字段                                                   | 设计说明                               |
| -------- | ---------------------------------------------------------- | -------------------------------------- |
| 主键字段 | `id`                                                       | 主业务对象唯一标识，也是扩展表关联依据 |
| 业务编号 | `member_no`                                                | 面向业务展示和外部系统对接             |
| 核心字段 | `nickname`、`mobile`、`status`                             | 高频查询、列表展示、状态控制字段       |
| 分级字段 | `member_level`                                             | 表示会员等级、客户等级等核心分类       |
| 业务时间 | `register_time`                                            | 表示业务发生时间                       |
| 审计字段 | `create_user`、`create_time`、`update_user`、`update_time` | 记录主表创建和变更信息                 |
| 删除字段 | `deleted`                                                  | 控制主表软删除                         |

扩展表常见字段如下：

| 字段类型 | 常用字段                                | 设计说明                               |
| -------- | --------------------------------------- | -------------------------------------- |
| 关联主键 | `member_id`                             | 共享主键方式下同时作为扩展表主键       |
| 资料字段 | `real_name`、`gender`、`birthday`       | 低频访问的个人资料字段                 |
| 资源字段 | `avatar_url`                            | 存储资源地址，不建议直接存储二进制文件 |
| 地区字段 | `province_code`、`city_code`、`address` | 用户地址或地区扩展信息                 |
| 描述字段 | `introduction`                          | 可选说明字段，长度应控制               |
| 审计字段 | `create_time`、`update_time`            | 记录扩展表创建和变更信息               |
| 删除字段 | `deleted`                               | 控制扩展表软删除                       |

字段拆分时应遵循以下原则：

- 高频列表字段放主表，低频详情字段放扩展表。
- 状态、等级、类型等核心业务控制字段优先放主表。
- 大字段、富文本、长备注、JSON 扩展字段优先放扩展表。
- 敏感字段可拆分到独立扩展表，便于权限控制和脱敏处理。
- 扩展表不应重复保存大量主表字段，避免数据不一致。
- 共享主键扩展表中不需要再设计独立 `id` 字段。
- 如果扩展数据可能从一条变多条，不应使用共享主键模型，应提前考虑一对多模型。
- 主表和扩展表的软删除策略应保持一致，避免主表已删除但扩展表仍被业务查询命中。

### 索引设计

一对一模型的索引设计应围绕主表高频查询、业务唯一性和扩展表关联查询展开。主表通常承载列表页、筛选页和详情入口，扩展表通常通过主表主键或关联字段查询。

常见索引设计如下：

```sql
ALTER TABLE member
    ADD UNIQUE KEY uk_member_no (member_no),
    ADD UNIQUE KEY uk_mobile (mobile),
    ADD KEY idx_status_deleted (status, deleted),
    ADD KEY idx_level_status (member_level, status),
    ADD KEY idx_register_time (register_time);

ALTER TABLE member_profile
    ADD KEY idx_city_code (city_code),
    ADD KEY idx_birthday (birthday);

ALTER TABLE member_security
    ADD UNIQUE KEY uk_member_id (member_id),
    ADD KEY idx_last_login_time (last_login_time);
```

索引说明：

| 表                | 索引                      | 作用                                         |
| ----------------- | ------------------------- | -------------------------------------------- |
| `member`          | `uk_member_no`            | 保证会员编号唯一，支持按编号查询             |
| `member`          | `uk_mobile`               | 保证手机号唯一，支持登录或手机号查询         |
| `member`          | `idx_status_deleted`      | 支持按状态和删除标识过滤                     |
| `member`          | `idx_level_status`        | 支持按会员等级和状态筛选                     |
| `member`          | `idx_register_time`       | 支持按注册时间范围查询或排序                 |
| `member_profile`  | `PRIMARY KEY (member_id)` | 支持通过会员 ID 查询资料，并保证强一对一关系 |
| `member_profile`  | `idx_city_code`           | 支持按城市筛选会员资料                       |
| `member_profile`  | `idx_birthday`            | 支持生日提醒、生日营销等查询                 |
| `member_security` | `uk_member_id`            | 独立主键扩展表中保证一个会员只有一条安全配置 |
| `member_security` | `idx_last_login_time`     | 支持按最后登录时间统计或筛选                 |

设计索引时应注意以下问题：

- 共享主键扩展表一般不需要再为关联字段单独建立普通索引，因为关联字段本身就是主键。
- 独立主键扩展表必须为主表关联字段建立唯一索引，才能保证一对一关系。
- 主表列表查询索引应优先满足后台筛选、状态过滤、时间排序等高频场景。
- 扩展表只为真实高频查询字段建索引，不要因为字段被拆分到扩展表就盲目加索引。
- 如果扩展表主要通过主表 ID 查询详情，通常只需要主键或关联唯一索引。
- 对敏感字段、长文本字段、头像地址、简介等字段通常不建议建立普通索引。

### 常用查询

一对一模型的查询通常包括主表查询、扩展表查询、主扩展联合查询、资料完整性查询、扩展字段筛选和缺失扩展数据查询。实际业务中应根据页面需要选择是否关联扩展表，不要所有列表查询都强制关联扩展表。

#### 查询主表列表

主表列表查询适合后台管理、会员中心、运营筛选等场景。列表页应优先查询主表字段，避免无意义关联扩展表。

```sql
SELECT
    id,
    member_no,
    nickname,
    mobile,
    member_level,
    status,
    register_time,
    create_time
FROM member
WHERE deleted = 0
  AND status = 1
ORDER BY register_time DESC
LIMIT 0, 20;
```

该查询只访问主表，适合高频列表页和基础筛选页。

#### 按主键查询主表详情

按主键查询主表详情用于获取会员核心信息。

```sql
SELECT
    id,
    member_no,
    nickname,
    mobile,
    email,
    member_level,
    status,
    register_time,
    create_time,
    update_time
FROM member
WHERE id = 10001
  AND deleted = 0;
```

该查询适合只需要核心信息的接口，例如账号状态校验、会员基础信息回显等。

#### 查询扩展资料

当页面只需要会员资料信息时，可以直接根据 `member_id` 查询扩展表。

```sql
SELECT
    member_id,
    real_name,
    gender,
    birthday,
    avatar_url,
    province_code,
    city_code,
    address,
    introduction,
    update_time
FROM member_profile
WHERE member_id = 10001
  AND deleted = 0;
```

共享主键模型下，`member_id` 是扩展表主键，该查询可以直接定位单条扩展资料。

#### 查询主表和扩展表详情

详情页通常需要同时返回主表核心信息和扩展表资料信息，可以使用 `LEFT JOIN` 查询。

```sql
SELECT
    m.id,
    m.member_no,
    m.nickname,
    m.mobile,
    m.email,
    m.member_level,
    m.status,
    m.register_time,
    p.real_name,
    p.gender,
    p.birthday,
    p.avatar_url,
    p.province_code,
    p.city_code,
    p.address,
    p.introduction
FROM member m
LEFT JOIN member_profile p ON p.member_id = m.id AND p.deleted = 0
WHERE m.id = 10001
  AND m.deleted = 0;
```

这里使用 `LEFT JOIN`，是为了允许主表存在但扩展资料暂未填写的情况。

#### 按手机号查询会员及资料

手机号查询常用于登录、客服检索和运营后台检索。查询时可以先通过主表定位会员，再按需关联资料表。

```sql
SELECT
    m.id,
    m.member_no,
    m.nickname,
    m.mobile,
    m.status,
    p.real_name,
    p.avatar_url,
    p.city_code
FROM member m
LEFT JOIN member_profile p ON p.member_id = m.id AND p.deleted = 0
WHERE m.mobile = '13800000000'
  AND m.deleted = 0;
```

如果手机号具有登录身份属性，应使用唯一索引保证唯一性。

#### 按扩展字段筛选主表数据

当筛选条件位于扩展表中，而展示字段主要来自主表时，可以从扩展表关联主表查询。

```sql
SELECT
    m.id,
    m.member_no,
    m.nickname,
    m.mobile,
    m.member_level,
    m.status,
    p.city_code,
    p.birthday
FROM member_profile p
INNER JOIN member m ON m.id = p.member_id AND m.deleted = 0
WHERE p.deleted = 0
  AND p.city_code = '330100'
  AND m.status = 1
ORDER BY m.register_time DESC
LIMIT 0, 20;
```

如果该类查询频率较高，需要结合扩展表筛选字段和主表状态字段评估索引设计。

#### 查询未填写扩展资料的主表数据

该查询用于资料补全提醒、运营筛选或数据巡检。

```sql
SELECT
    m.id,
    m.member_no,
    m.nickname,
    m.mobile,
    m.register_time
FROM member m
LEFT JOIN member_profile p ON p.member_id = m.id AND p.deleted = 0
WHERE m.deleted = 0
  AND p.member_id IS NULL
ORDER BY m.register_time DESC
LIMIT 20;
```

该查询可以找出有会员主记录但没有资料扩展记录的数据。

#### 查询资料完整但主表可用的数据

该查询用于筛选已经完善资料且账号启用的会员。

```sql
SELECT
    m.id,
    m.member_no,
    m.nickname,
    m.mobile,
    p.real_name,
    p.gender,
    p.birthday,
    p.city_code
FROM member m
INNER JOIN member_profile p ON p.member_id = m.id AND p.deleted = 0
WHERE m.deleted = 0
  AND m.status = 1
  AND p.real_name IS NOT NULL
  AND p.birthday IS NOT NULL
ORDER BY m.register_time DESC
LIMIT 20;
```

使用 `INNER JOIN` 可以只返回存在有效扩展资料的数据。

### 常用写入

一对一模型的写入包括新增主表、新增扩展表、主扩展同时写入、更新扩展资料、补建扩展记录和软删除。主表和扩展表是否必须在同一事务中写入，应根据扩展数据是否为业务必填决定。

#### 新增主表数据

当扩展资料不是必填项时，可以只新增主表，后续再补充扩展表。

```sql
INSERT INTO member (
    id,
    member_no,
    nickname,
    mobile,
    email,
    member_level,
    status,
    register_time,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES (
    10001,
    'MB202605130001',
    '示例会员',
    '13800000000',
    'member@example.com',
    1,
    1,
    NOW(),
    1,
    NOW(),
    1,
    NOW(),
    0
);
```

这种方式适合注册时只采集账号信息，资料信息后续由用户自行完善。

#### 新增主表和扩展表

当扩展资料是业务必填项时，应在同一个事务中写入主表和扩展表。

```sql
START TRANSACTION;

INSERT INTO member (
    id,
    member_no,
    nickname,
    mobile,
    email,
    member_level,
    status,
    register_time,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES (
    10002,
    'MB202605130002',
    '企业联系人',
    '13900000000',
    'contact@example.com',
    2,
    1,
    NOW(),
    1,
    NOW(),
    1,
    NOW(),
    0
);

INSERT INTO member_profile (
    member_id,
    real_name,
    gender,
    birthday,
    avatar_url,
    province_code,
    city_code,
    address,
    introduction,
    create_time,
    update_time,
    deleted
) VALUES (
    10002,
    '张三',
    1,
    '1995-05-13',
    'https://example.com/avatar/10002.png',
    '330000',
    '330100',
    '浙江省杭州市示例路100号',
    '企业联系人资料',
    NOW(),
    NOW(),
    0
);

COMMIT;
```

主扩展同时写入时，应保证任意一张表写入失败后整体回滚。

#### 补充扩展资料

如果主表已存在但扩展资料尚未创建，可以后续插入扩展记录。

```sql
INSERT INTO member_profile (
    member_id,
    real_name,
    gender,
    birthday,
    avatar_url,
    province_code,
    city_code,
    address,
    introduction,
    create_time,
    update_time,
    deleted
) VALUES (
    10001,
    '李四',
    2,
    '1998-08-20',
    'https://example.com/avatar/10001.png',
    '310000',
    '310100',
    '上海市示例区示例路200号',
    '普通会员资料',
    NOW(),
    NOW(),
    0
);
```

共享主键模型下，如果同一个 `member_id` 已存在，再次插入会触发主键冲突，应改为更新。

#### 更新主表核心信息

主表更新应只处理核心字段，例如昵称、邮箱、等级、状态等。

```sql
UPDATE member
SET nickname = '示例会员A',
    email = 'new-member@example.com',
    member_level = 2,
    update_user = 1,
    update_time = NOW()
WHERE id = 10001
  AND deleted = 0;
```

不建议在主表更新语句中混入扩展表字段，避免业务职责不清。

#### 更新扩展资料

扩展资料更新应只处理资料字段，例如真实姓名、头像、地址、简介等。

```sql
UPDATE member_profile
SET real_name = '李四',
    gender = 2,
    birthday = '1998-08-20',
    avatar_url = 'https://example.com/avatar/new-10001.png',
    province_code = '310000',
    city_code = '310100',
    address = '上海市示例区新地址',
    introduction = '资料已完善',
    update_time = NOW()
WHERE member_id = 10001
  AND deleted = 0;
```

如果更新影响行数为 `0`，可能是扩展资料不存在或已被软删除，业务层需要判断是否改为补建扩展记录。

#### 新增或更新扩展资料

对于共享主键扩展表，可以使用 `INSERT ... ON DUPLICATE KEY UPDATE` 实现资料保存。

```sql
INSERT INTO member_profile (
    member_id,
    real_name,
    gender,
    birthday,
    avatar_url,
    province_code,
    city_code,
    address,
    introduction,
    create_time,
    update_time,
    deleted
) VALUES (
    10001,
    '王五',
    1,
    '1992-03-15',
    'https://example.com/avatar/10001.png',
    '440000',
    '440300',
    '广东省深圳市示例路300号',
    '更新后的会员资料',
    NOW(),
    NOW(),
    0
)
ON DUPLICATE KEY UPDATE
    real_name = VALUES(real_name),
    gender = VALUES(gender),
    birthday = VALUES(birthday),
    avatar_url = VALUES(avatar_url),
    province_code = VALUES(province_code),
    city_code = VALUES(city_code),
    address = VALUES(address),
    introduction = VALUES(introduction),
    update_time = NOW(),
    deleted = 0;
```

该写法适合资料保存接口，但应谨慎处理 `create_time` 和已软删除数据的恢复逻辑。

#### 软删除主表和扩展表

删除主业务对象时，应同步软删除扩展表，避免扩展表残留有效数据。

```sql
START TRANSACTION;

UPDATE member
SET deleted = 1,
    update_user = 1,
    update_time = NOW()
WHERE id = 10001
  AND deleted = 0;

UPDATE member_profile
SET deleted = 1,
    update_time = NOW()
WHERE member_id = 10001
  AND deleted = 0;

COMMIT;
```

如果存在多个一对一扩展表，例如资料表、安全配置表、认证表，应在同一事务中统一处理。

### 常见问题

一对一模型的问题通常集中在拆分边界、唯一性约束、扩展数据缺失、查询关联过度和软删除一致性上。

| 问题                 | 说明                                     | 建议                                                       |
| -------------------- | ---------------------------------------- | ---------------------------------------------------------- |
| 拆表过度             | 字段很少且访问频率一致，却拆成多张表     | 只有在冷热分离、敏感隔离、大字段隔离或扩展边界明确时才拆分 |
| 一对一变成一对多     | 扩展表中同一个主表 ID 出现多条记录       | 使用共享主键或对关联字段建立唯一索引                       |
| 扩展资料缺失         | 主表存在，但扩展表没有对应记录           | 查询时使用 `LEFT JOIN`，写入时按业务决定是否强制创建       |
| 列表查询关联过多     | 所有列表都关联扩展表，导致查询变慢       | 列表页优先查询主表，详情页再查询扩展表                     |
| 软删除状态不一致     | 主表已删除，扩展表仍是有效状态           | 主扩展删除应放在同一事务中处理                             |
| 主表和扩展表字段重复 | 扩展表冗余大量主表字段                   | 只冗余必要字段，优先通过主键关联查询                       |
| 敏感字段未隔离       | 身份证、密码配置、安全信息混在主表       | 敏感信息拆到独立扩展表，并加强权限控制                     |
| 大字段影响主表性能   | 富文本、简介、JSON 等字段放在高频主表    | 大字段放入扩展表，主表保留列表必需字段                     |
| 扩展表生命周期不清   | 不知道扩展表是否可以单独新增、删除或恢复 | 在业务规则中明确扩展表依附主表存在                         |
| 联表查询返回重复数据 | 独立主键扩展表没有保证 `member_id` 唯一  | 对关联字段建立唯一索引，或改为共享主键模型                 |

当扩展数据开始出现多条历史记录、多个认证记录、多个地址记录或多个配置版本时，应及时从一对一模型演进为一对多模型、历史版本模型或数据变更记录模型。

### 总结

一对一模型适合将同一个业务对象拆分为核心表和扩展表。主表保存高频、核心、稳定字段，扩展表保存低频、可选、敏感、大字段或扩展性较强的字段。该模型可以减少主表字段膨胀，提高列表查询效率，并让数据职责边界更加清晰。

设计一对一模型时，应优先考虑共享主键方式，它结构简单，天然保证一对一关系。如果扩展表需要独立生命周期或未来可能演进为多条记录，可以使用独立主键加关联唯一索引的方式。实际使用中，应避免过度拆表、避免列表页无意义关联扩展表，并通过事务保证主表和扩展表的数据一致性。



## 一对多模型

一对多模型用于表达一个主业务对象对应多条子业务数据的关系。与主从表模型相比，一对多模型中的子表数据通常具备更明确的业务含义，可能被单独新增、修改、查询、排序、启用、禁用或软删除。该模型常用于客户与地址、用户与登录设备、分类与商品、部门与员工、文章与评论等场景。

### 适用场景

一对多模型适合用于一个主对象下存在多条同类型子记录，且子记录需要被单独管理的业务场景。主对象和子对象之间存在明确归属关系，子对象通常通过主对象 ID 进行关联。

常见适用场景包括：

| 场景           | 说明                           |
| -------------- | ------------------------------ |
| 客户与收货地址 | 一个客户可以维护多个收货地址   |
| 用户与登录设备 | 一个用户可以绑定多个登录设备   |
| 部门与员工     | 一个部门下可以有多个员工       |
| 分类与商品     | 一个分类下可以有多个商品       |
| 文章与评论     | 一篇文章可以有多条评论         |
| 门店与员工     | 一个门店可以关联多个员工       |
| 项目与任务     | 一个项目可以拆分为多个任务     |
| 主账号与子账号 | 一个主账号下可以创建多个子账号 |

一对多模型不适合用于以下情况：

- 子记录只是主记录的明细行，且不具备独立管理价值，优先使用主从表模型。
- 子记录可以同时属于多个主对象，优先使用多对多模型。
- 主对象和子对象本质上是同一类数据的上下级结构，优先使用树形层级模型。
- 子记录只是动态字段扩展，优先考虑 JSON 扩展字段模型或 EAV 动态属性模型。
- 子记录数量极大且写入频率很高，应结合分区表、归档表或分库分表模型设计。

### 建模结构

一对多模型通常由一张主表和一张子表组成。主表保存主业务对象的核心信息，子表保存多条归属于主对象的子记录。子表通过主表 ID 建立归属关系。

以下示例以客户和客户收货地址为例。`customer` 表保存客户核心信息，`customer_address` 表保存客户的多个收货地址。

```sql
CREATE TABLE customer (
    id BIGINT NOT NULL COMMENT '主键ID',
    customer_no VARCHAR(32) NOT NULL COMMENT '客户编号',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    customer_type TINYINT NOT NULL COMMENT '客户类型：1个人，2企业',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_user BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_user BIGINT DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户表';

CREATE TABLE customer_address (
    id BIGINT NOT NULL COMMENT '主键ID',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    receiver_name VARCHAR(100) NOT NULL COMMENT '收货人姓名',
    receiver_mobile VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    province_code VARCHAR(20) NOT NULL COMMENT '省份编码',
    province_name VARCHAR(100) NOT NULL COMMENT '省份名称',
    city_code VARCHAR(20) NOT NULL COMMENT '城市编码',
    city_name VARCHAR(100) NOT NULL COMMENT '城市名称',
    district_code VARCHAR(20) DEFAULT NULL COMMENT '区县编码',
    district_name VARCHAR(100) DEFAULT NULL COMMENT '区县名称',
    detail_address VARCHAR(300) NOT NULL COMMENT '详细地址',
    default_flag TINYINT NOT NULL DEFAULT 0 COMMENT '默认地址标识：0否，1是',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    create_user BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_user BIGINT DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户收货地址表';
```

该结构中，`customer` 是一方，`customer_address` 是多方。`customer_address.customer_id` 表示子记录归属于哪个客户。一个客户可以有多条地址记录，但每条地址记录只归属于一个客户。

在业务系统中，是否使用数据库外键需要根据团队规范决定。高并发、分库分表、复杂发布流程或跨服务业务中，通常更倾向于使用逻辑外键，由应用层和事务保证一致性；单体系统或强一致性要求较高的内部系统，可以评估使用物理外键。

### 字段设计

一对多模型的字段设计重点在于明确主对象字段和子对象字段的职责。主表字段应描述主对象本身，子表字段应描述某一条子记录，不能把多条子数据压缩存储到主表字段中。

主表常见字段如下：

| 字段类型 | 常用字段                                                   | 设计说明                         |
| -------- | ---------------------------------------------------------- | -------------------------------- |
| 主键字段 | `id`                                                       | 主对象唯一标识，供子表关联       |
| 业务编号 | `customer_no`                                              | 面向业务展示、查询和外部系统对接 |
| 核心字段 | `customer_name`、`customer_type`                           | 描述主对象核心属性               |
| 联系字段 | `mobile`、`email`                                          | 主对象自身联系方式               |
| 状态字段 | `status`                                                   | 控制主对象是否可用               |
| 审计字段 | `create_user`、`create_time`、`update_user`、`update_time` | 记录主对象创建和修改信息         |
| 删除字段 | `deleted`                                                  | 支持主对象软删除                 |

子表常见字段如下：

| 字段类型   | 常用字段                                                   | 设计说明                     |
| ---------- | ---------------------------------------------------------- | ---------------------------- |
| 主键字段   | `id`                                                       | 子记录唯一标识               |
| 关联字段   | `customer_id`                                              | 关联主表主键，表示归属关系   |
| 子对象字段 | `receiver_name`、`receiver_mobile`、`detail_address`       | 描述具体子记录内容           |
| 区域字段   | `province_code`、`city_code`、`district_code`              | 用于地区筛选、展示和配送判断 |
| 默认标识   | `default_flag`                                             | 表示是否为默认地址           |
| 状态字段   | `status`                                                   | 控制子记录是否可用           |
| 排序字段   | `sort_no`                                                  | 控制子记录展示顺序           |
| 审计字段   | `create_user`、`create_time`、`update_user`、`update_time` | 记录子记录创建和修改信息     |
| 删除字段   | `deleted`                                                  | 支持子记录软删除             |

字段设计时应注意以下原则：

- 子表必须有主表关联字段，例如 `customer_id`。
- 子表不要只使用主表业务编号关联，优先使用主表主键关联。
- 可以适度冗余主表业务编号，但不建议大量复制主表字段。
- 子表应有独立主键，便于单条子记录更新、删除和排序。
- 子表如果需要独立启停，应设计自己的 `status` 字段。
- 子表如果需要前端展示顺序，应设计 `sort_no` 字段。
- 默认标识类字段应明确同一个主对象下是否只允许一条默认记录。
- 子表数量不应存储在主表字段中作为强一致数据，除非存在明确的性能优化需求。
- 主表软删除时，应明确子表是否同步软删除。
- 子表的生命周期如果完全独立于主表，应重新评估是否仍属于一对多模型。

### 索引设计

一对多模型的索引设计应重点围绕主表查询、子表归属查询、子表状态过滤和子表排序展开。子表的关联字段是该模型中最重要的索引字段。

常见索引设计如下：

```sql
ALTER TABLE customer
    ADD UNIQUE KEY uk_customer_no (customer_no),
    ADD KEY idx_status_deleted (status, deleted),
    ADD KEY idx_customer_type_status (customer_type, status),
    ADD KEY idx_create_time (create_time);

ALTER TABLE customer_address
    ADD KEY idx_customer_id (customer_id),
    ADD KEY idx_customer_status_deleted (customer_id, status, deleted),
    ADD KEY idx_customer_default (customer_id, default_flag, deleted),
    ADD KEY idx_customer_sort (customer_id, sort_no, id),
    ADD KEY idx_city_code (city_code);
```

索引说明：

| 表                 | 索引                          | 作用                                 |
| ------------------ | ----------------------------- | ------------------------------------ |
| `customer`         | `uk_customer_no`              | 保证客户编号唯一，支持按客户编号查询 |
| `customer`         | `idx_status_deleted`          | 支持按状态和删除标识过滤             |
| `customer`         | `idx_customer_type_status`    | 支持按客户类型和状态筛选             |
| `customer`         | `idx_create_time`             | 支持按创建时间排序或范围查询         |
| `customer_address` | `idx_customer_id`             | 支持根据客户 ID 查询全部地址         |
| `customer_address` | `idx_customer_status_deleted` | 支持查询客户下有效地址               |
| `customer_address` | `idx_customer_default`        | 支持查询客户默认地址                 |
| `customer_address` | `idx_customer_sort`           | 支持客户地址按排序号展示             |
| `customer_address` | `idx_city_code`               | 支持按城市反查地址数据               |

如果业务要求同一个客户只能有一个默认地址，可以使用生成列或业务事务控制来保证。MySQL 普通唯一索引无法直接表达“同一个客户下 `default_flag = 1` 且 `deleted = 0` 的记录只能有一条”这种部分唯一约束，通常由应用层事务控制，或通过额外字段、约束设计、触发器等方式实现。

设计索引时应注意以下问题：

- 子表必须为主表关联字段建立索引。
- 高频查询如果总是带 `customer_id`、`status`、`deleted`，应设计组合索引。
- 子表排序查询可以考虑 `(customer_id, sort_no, id)` 组合索引。
- 反向查询子记录时，应为反查字段建立索引，例如 `city_code`。
- 不要为子表所有字段建立索引，子表通常写入频率更高，索引过多会影响写入。
- 如果子表数据量远大于主表，应优先关注子表索引和归档策略。
- 如果经常通过子表条件筛选主表，需要评估查询路径是否稳定，必要时设计搜索辅助表或冗余字段。

### 常用查询

一对多模型的查询通常包括主表详情查询、子表列表查询、主子联合查询、默认子记录查询、子记录分页查询、按子记录反查主表等。查询时应避免在主表分页场景中直接关联大量子表数据。

#### 查询主表详情

该查询用于客户详情页、编辑页和接口回显，通常只返回主表核心信息。

```sql
SELECT
    id,
    customer_no,
    customer_name,
    customer_type,
    mobile,
    email,
    status,
    remark,
    create_time,
    update_time
FROM customer
WHERE id = 10001
  AND deleted = 0;
```

主表详情查询应优先使用主键或业务编号定位单条数据。

#### 查询主对象下的子记录列表

该查询用于展示某个客户下的全部有效地址。

```sql
SELECT
    id,
    customer_id,
    receiver_name,
    receiver_mobile,
    province_name,
    city_name,
    district_name,
    detail_address,
    default_flag,
    status,
    sort_no
FROM customer_address
WHERE customer_id = 10001
  AND deleted = 0
ORDER BY default_flag DESC, sort_no ASC, id ASC;
```

子表列表查询必须带上主表关联字段，避免扫描整张子表。

#### 查询主对象下可用子记录

该查询用于业务下单、配送地址选择等只允许选择有效地址的场景。

```sql
SELECT
    id,
    receiver_name,
    receiver_mobile,
    province_name,
    city_name,
    district_name,
    detail_address,
    default_flag
FROM customer_address
WHERE customer_id = 10001
  AND status = 1
  AND deleted = 0
ORDER BY default_flag DESC, sort_no ASC, id ASC;
```

如果该查询是高频接口，应使用包含 `customer_id`、`status`、`deleted` 的组合索引。

#### 查询默认子记录

默认地址、默认设备、默认配置等场景都属于一对多模型中的默认子记录查询。

```sql
SELECT
    id,
    customer_id,
    receiver_name,
    receiver_mobile,
    province_name,
    city_name,
    district_name,
    detail_address
FROM customer_address
WHERE customer_id = 10001
  AND default_flag = 1
  AND status = 1
  AND deleted = 0
LIMIT 1;
```

业务上应保证同一个客户下最多只有一条默认地址，否则该查询结果会不稳定。

#### 查询主表并关联子记录

该查询适合查询单个主对象及其子记录，不适合用于主表分页列表。

```sql
SELECT
    c.id AS customer_id,
    c.customer_no,
    c.customer_name,
    c.customer_type,
    c.mobile,
    c.status AS customer_status,
    a.id AS address_id,
    a.receiver_name,
    a.receiver_mobile,
    a.province_name,
    a.city_name,
    a.district_name,
    a.detail_address,
    a.default_flag,
    a.status AS address_status
FROM customer c
LEFT JOIN customer_address a ON a.customer_id = c.id AND a.deleted = 0
WHERE c.id = 10001
  AND c.deleted = 0
ORDER BY a.default_flag DESC, a.sort_no ASC, a.id ASC;
```

如果子记录数量较多，建议先查主表，再按 `customer_id` 查询子表，避免单条接口返回过多数据。

#### 主表分页查询

主表分页查询应只查询主表字段，避免直接关联子表导致分页数据重复或性能下降。

```sql
SELECT
    id,
    customer_no,
    customer_name,
    customer_type,
    mobile,
    status,
    create_time
FROM customer
WHERE deleted = 0
  AND status = 1
  AND customer_type = 2
ORDER BY create_time DESC, id DESC
LIMIT 0, 20;
```

列表页如果需要展示子记录数量或默认子记录，应优先评估是否通过冗余字段、异步统计或单独批量查询实现。

#### 批量查询多个主对象的子记录

主表分页后，如果需要展示每个客户的地址数量或默认地址，可以根据主表 ID 列表批量查询子表。

```sql
SELECT
    customer_id,
    id,
    receiver_name,
    receiver_mobile,
    province_name,
    city_name,
    district_name,
    detail_address,
    default_flag
FROM customer_address
WHERE customer_id IN (10001, 10002, 10003)
  AND deleted = 0
ORDER BY customer_id ASC, default_flag DESC, sort_no ASC, id ASC;
```

该方式比在主表分页 SQL 中直接关联子表更容易控制分页结果和返回数据量。

#### 统计主对象下的子记录数量

该查询用于统计某个客户下有效地址数量。

```sql
SELECT
    customer_id,
    COUNT(1) AS address_count
FROM customer_address
WHERE customer_id = 10001
  AND deleted = 0
GROUP BY customer_id;
```

如果需要批量统计多个客户，可以使用 `IN` 查询。

```sql
SELECT
    customer_id,
    COUNT(1) AS address_count
FROM customer_address
WHERE customer_id IN (10001, 10002, 10003)
  AND deleted = 0
GROUP BY customer_id;
```

如果统计结果高频展示且实时性要求不高，可以考虑在主表冗余数量字段，并通过写入流程或异步任务维护。

#### 根据子记录反查主对象

该查询用于根据地址地区、设备类型、评论状态等子记录条件反查主对象。

```sql
SELECT DISTINCT
    c.id,
    c.customer_no,
    c.customer_name,
    c.customer_type,
    c.mobile,
    c.status
FROM customer_address a
INNER JOIN customer c ON c.id = a.customer_id AND c.deleted = 0
WHERE a.deleted = 0
  AND a.city_code = '330100'
  AND c.status = 1
ORDER BY c.create_time DESC
LIMIT 20;
```

当该类查询频率较高时，需要为子表反查字段建立索引，并评估是否需要搜索辅助表。

#### 查询没有子记录的主对象

该查询用于数据巡检、资料补全提醒或运营筛选。

```sql
SELECT
    c.id,
    c.customer_no,
    c.customer_name,
    c.mobile,
    c.create_time
FROM customer c
LEFT JOIN customer_address a ON a.customer_id = c.id AND a.deleted = 0
WHERE c.deleted = 0
  AND a.id IS NULL
ORDER BY c.create_time DESC
LIMIT 20;
```

该查询可以找出没有维护任何有效地址的客户。

### 常用写入

一对多模型的写入包括新增主表、新增子表、更新子表、设置默认子记录、删除子记录、删除主表并同步处理子表。写入时需要重点保证归属关系正确、默认标识唯一、主子数据状态一致。

#### 新增主表数据

新增主对象时，可以只写入主表，后续再维护子表数据。

```sql
INSERT INTO customer (
    id,
    customer_no,
    customer_name,
    customer_type,
    mobile,
    email,
    status,
    remark,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES (
    10001,
    'CUST202605130001',
    '杭州示例科技有限公司',
    2,
    '13800000000',
    'contact@example.com',
    1,
    '企业客户',
    1,
    NOW(),
    1,
    NOW(),
    0
);
```

该方式适合客户创建后再补充地址、联系人、设备等子数据的场景。

#### 新增子记录

新增子记录时，必须明确归属的主表 ID。业务层应先校验主表是否存在且状态允许新增子数据。

```sql
INSERT INTO customer_address (
    id,
    customer_id,
    receiver_name,
    receiver_mobile,
    province_code,
    province_name,
    city_code,
    city_name,
    district_code,
    district_name,
    detail_address,
    default_flag,
    status,
    sort_no,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES (
    20001,
    10001,
    '张三',
    '13800000000',
    '330000',
    '浙江省',
    '330100',
    '杭州市',
    '330106',
    '西湖区',
    '示例路100号',
    0,
    1,
    1,
    1,
    NOW(),
    1,
    NOW(),
    0
);
```

新增子记录前，应避免只检查 `customer_id` 是否非空，还应确认对应主记录未删除、未禁用。

#### 新增默认子记录

如果新增的子记录需要设置为默认地址，应在同一个事务中先取消旧默认，再插入新默认。

```sql
START TRANSACTION;

UPDATE customer_address
SET default_flag = 0,
    update_user = 1,
    update_time = NOW()
WHERE customer_id = 10001
  AND default_flag = 1
  AND deleted = 0;

INSERT INTO customer_address (
    id,
    customer_id,
    receiver_name,
    receiver_mobile,
    province_code,
    province_name,
    city_code,
    city_name,
    district_code,
    district_name,
    detail_address,
    default_flag,
    status,
    sort_no,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES (
    20002,
    10001,
    '李四',
    '13900000000',
    '310000',
    '上海市',
    '310100',
    '上海市',
    '310101',
    '黄浦区',
    '示例路200号',
    1,
    1,
    1,
    1,
    NOW(),
    1,
    NOW(),
    0
);

COMMIT;
```

该事务可以保证同一个客户下默认地址不会因为普通写入流程产生明显冲突。高并发场景下还应结合行锁、业务锁或唯一约束方案控制并发。

#### 更新子记录

更新子记录时，应同时带上子记录 ID 和主表 ID，防止越权修改其他主对象下的数据。

```sql
UPDATE customer_address
SET receiver_name = '王五',
    receiver_mobile = '13700000000',
    province_code = '440000',
    province_name = '广东省',
    city_code = '440300',
    city_name = '深圳市',
    district_code = '440305',
    district_name = '南山区',
    detail_address = '示例大道300号',
    sort_no = 2,
    update_user = 1,
    update_time = NOW()
WHERE id = 20001
  AND customer_id = 10001
  AND deleted = 0;
```

多租户系统中还应在 `WHERE` 条件中加入 `tenant_id`，避免跨租户数据修改。

#### 设置默认子记录

设置默认地址时，应先确认目标地址属于当前客户，然后在同一个事务中取消旧默认并设置新默认。

```sql
START TRANSACTION;

UPDATE customer_address
SET default_flag = 0,
    update_user = 1,
    update_time = NOW()
WHERE customer_id = 10001
  AND default_flag = 1
  AND deleted = 0;

UPDATE customer_address
SET default_flag = 1,
    update_user = 1,
    update_time = NOW()
WHERE id = 20001
  AND customer_id = 10001
  AND status = 1
  AND deleted = 0;

COMMIT;
```

第二个 `UPDATE` 应检查影响行数。如果影响行数为 `0`，说明目标地址不存在、已删除、已禁用或不属于当前客户。

#### 删除子记录

删除子记录通常使用软删除。删除时应带上主表 ID，避免误删其他主对象下的数据。

```sql
UPDATE customer_address
SET deleted = 1,
    default_flag = 0,
    update_user = 1,
    update_time = NOW()
WHERE id = 20001
  AND customer_id = 10001
  AND deleted = 0;
```

如果删除的是默认地址，应根据业务规则决定是否自动选择另一条地址作为默认地址。

#### 删除主表并同步删除子表

删除主对象时，如果子记录依附主对象存在，应在同一个事务中同步软删除子表数据。

```sql
START TRANSACTION;

UPDATE customer
SET deleted = 1,
    update_user = 1,
    update_time = NOW()
WHERE id = 10001
  AND deleted = 0;

UPDATE customer_address
SET deleted = 1,
    update_user = 1,
    update_time = NOW()
WHERE customer_id = 10001
  AND deleted = 0;

COMMIT;
```

如果子记录需要保留历史，应明确查询时是否继续展示，并在业务层区分有效数据和历史数据。

#### 批量调整子记录排序

一对多子记录经常需要支持排序，例如地址、菜单项、配置项、任务列表等。

```sql
UPDATE customer_address
SET sort_no = CASE id
    WHEN 20001 THEN 1
    WHEN 20002 THEN 2
    WHEN 20003 THEN 3
    ELSE sort_no
END,
update_user = 1,
update_time = NOW()
WHERE customer_id = 10001
  AND id IN (20001, 20002, 20003)
  AND deleted = 0;
```

批量排序时应限制 `customer_id`，防止跨主对象调整排序。

### 常见问题

一对多模型的问题通常集中在关联字段缺失、子表查询性能、默认记录冲突、主子状态不一致和误用联表分页上。

| 问题                   | 说明                                     | 建议                                       |
| ---------------------- | ---------------------------------------- | ------------------------------------------ |
| 子表没有关联索引       | 根据主表 ID 查询子记录时扫描整张子表     | 为子表关联字段建立索引                     |
| 子记录归属混乱         | 更新或删除子记录时只按子记录 ID 操作     | 写入时同时校验子记录 ID 和主表 ID          |
| 默认记录出现多条       | 同一个主对象下多个子记录都被标记为默认   | 使用事务、业务锁或约束方案保证唯一默认     |
| 主表删除后子表仍有效   | 主对象已删除，但子记录仍能被查询到       | 删除主表时同步处理子表，查询时校验主表状态 |
| 主表分页直接关联子表   | 一条主记录被多条子记录放大，导致分页错误 | 主表分页和子表查询分开处理                 |
| 子表数据量过大         | 单个主对象下子记录过多，详情页查询很慢   | 子表单独分页，必要时归档或拆分             |
| 子表状态与主表状态冲突 | 主表禁用后，子表仍被业务使用             | 查询和写入时同时校验主表状态               |
| 统计字段不一致         | 主表冗余子记录数量，但维护不及时         | 通过事务同步维护或使用异步重算             |
| 反向查询性能差         | 经常根据子表条件查询主表，但缺少子表索引 | 为子表反查字段建索引，必要时使用搜索辅助表 |
| 一对多误建成 JSON      | 多条子记录被压缩存储在主表 JSON 字段中   | 需要独立查询、更新、排序、统计时应使用子表 |

当一对多子表承担复杂状态流转、历史版本、审计追踪、全文搜索或高频统计时，应结合状态机模型、历史版本模型、审计日志模型、搜索辅助表模型或统计汇总模型进行扩展。

### 总结

一对多模型适合表达一个主业务对象下存在多条子业务数据的关系。主表保存主对象核心信息，子表保存多条归属主对象的子记录。该模型比单表模型更适合表达可独立管理的子数据，也比主从表模型更强调子记录自身的业务属性和生命周期。

设计一对多模型时，应重点关注主子职责边界、子表关联字段、子表索引、默认记录控制、主子状态一致性和查询方式。主表分页应避免直接关联子表，子表写入应校验归属关系。随着子表数据规模增长，应及时评估分页、归档、分区、分库分表和读模型优化方案。



## 多对多模型

多对多模型用于表达两类业务对象之间互相拥有多条关联关系的场景。通常通过一张中间关系表维护两端对象的绑定关系，两端业务表只保存自身数据，不直接在字段中存储对方的多个 ID。该模型常用于用户与角色、角色与权限、商品与标签、学生与课程、员工与岗位、文章与标签等业务场景。

### 适用场景

多对多模型适合用于两个业务对象都可以关联多个对方对象，并且关联关系本身需要被查询、维护、启用、禁用、排序或审计的场景。该模型的核心不是简单保存两个 ID，而是将“关系”本身作为一类数据进行管理。

常见适用场景包括：

| 场景       | 说明                                                   |
| ---------- | ------------------------------------------------------ |
| 用户与角色 | 一个用户可以拥有多个角色，一个角色也可以分配给多个用户 |
| 角色与权限 | 一个角色可以拥有多个权限，一个权限也可以被多个角色使用 |
| 商品与标签 | 一个商品可以有多个标签，一个标签也可以绑定多个商品     |
| 学生与课程 | 一个学生可以选择多门课程，一门课程也可以被多个学生选择 |
| 员工与岗位 | 一个员工可以兼任多个岗位，一个岗位也可以分配给多个员工 |
| 文章与标签 | 一篇文章可以配置多个标签，一个标签也可以关联多篇文章   |
| 项目与成员 | 一个项目可以有多个成员，一个成员也可以参与多个项目     |

多对多模型不适合以下情况：

- 关联关系只是一对多，例如一个订单只属于一个客户，应使用一对多模型。
- 关系数据没有独立维护价值，且只存在少量固定枚举，可以考虑字段或字典模型。
- 关系数量极少且不会查询关系本身，不需要过度设计中间表。
- 两端对象本质上是上下级层级关系，应使用树形层级模型。
- 关系表数据量极大且写入频繁，应提前结合分区表、归档表或分库分表模型设计。

### 建模结构

多对多模型通常由两张业务主表和一张中间关系表组成。两张业务主表分别保存各自对象的核心信息，中间关系表保存两端对象的关联关系。

以下示例以用户和角色为例。`sys_user` 表保存用户信息，`sys_role` 表保存角色信息，`sys_user_role` 表保存用户与角色之间的多对多关系。

```sql
CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_no VARCHAR(32) NOT NULL COMMENT '用户编号',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    real_name VARCHAR(100) DEFAULT NULL COMMENT '真实姓名',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    create_user BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_user BIGINT DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '主键ID',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_type TINYINT NOT NULL DEFAULT 1 COMMENT '角色类型：1系统角色，2业务角色',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    create_user BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_user BIGINT DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE sys_user_role (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    relation_status TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态：0禁用，1启用',
    bind_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    create_user BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_user BIGINT DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关系表';
```

该结构中，`sys_user` 和 `sys_role` 是两个独立业务对象，`sys_user_role` 是中间关系表。一个用户可以通过中间表关联多个角色，一个角色也可以通过中间表关联多个用户。

中间关系表可以只保存两个关联字段，也可以保存关系状态、绑定时间、过期时间、创建人等关系属性。当关系本身具备业务含义时，应将这些属性放在中间表，而不是放在任意一端主表中。

### 字段设计

多对多模型的字段设计重点在于明确两端业务表和中间关系表的职责。两端业务表只保存自身对象信息，中间关系表保存关联关系和关系属性。

用户表常见字段如下：

| 字段类型 | 常用字段                                                   | 设计说明                             |
| -------- | ---------------------------------------------------------- | ------------------------------------ |
| 主键字段 | `id`                                                       | 用户唯一标识，供关系表关联           |
| 业务编号 | `user_no`                                                  | 用户业务编号，适合展示和外部系统对接 |
| 登录字段 | `username`                                                 | 用户登录名或账号名                   |
| 基础信息 | `real_name`、`mobile`、`email`                             | 用户基础资料                         |
| 状态字段 | `status`                                                   | 控制用户是否可用                     |
| 审计字段 | `create_user`、`create_time`、`update_user`、`update_time` | 记录用户创建和修改信息               |
| 删除字段 | `deleted`                                                  | 支持用户软删除                       |

角色表常见字段如下：

| 字段类型 | 常用字段                                                   | 设计说明                           |
| -------- | ---------------------------------------------------------- | ---------------------------------- |
| 主键字段 | `id`                                                       | 角色唯一标识，供关系表关联         |
| 业务编码 | `role_code`                                                | 角色编码，适合权限判断和配置引用   |
| 名称字段 | `role_name`                                                | 角色名称，用于展示                 |
| 类型字段 | `role_type`                                                | 区分系统角色、业务角色、租户角色等 |
| 状态字段 | `status`                                                   | 控制角色是否可用                   |
| 排序字段 | `sort_no`                                                  | 控制角色展示顺序                   |
| 审计字段 | `create_user`、`create_time`、`update_user`、`update_time` | 记录角色创建和修改信息             |
| 删除字段 | `deleted`                                                  | 支持角色软删除                     |

关系表常见字段如下：

| 字段类型     | 常用字段                                                   | 设计说明                     |
| ------------ | ---------------------------------------------------------- | ---------------------------- |
| 主键字段     | `id`                                                       | 关系记录唯一标识             |
| 左侧关联字段 | `user_id`                                                  | 关联用户表主键               |
| 右侧关联字段 | `role_id`                                                  | 关联角色表主键               |
| 关系状态     | `relation_status`                                          | 控制当前关系是否可用         |
| 绑定时间     | `bind_time`                                                | 记录关系建立时间             |
| 过期时间     | `expire_time`                                              | 用于临时授权、限时绑定等场景 |
| 审计字段     | `create_user`、`create_time`、`update_user`、`update_time` | 记录关系创建和修改信息       |
| 删除字段     | `deleted`                                                  | 支持关系软删除               |

字段设计时应注意以下原则：

- 两端业务表不要用逗号拼接字段保存多个对方 ID。
- 中间关系表必须保存两端业务表的主键 ID。
- 中间关系表应有独立主键，便于关系记录审计、更新和软删除。
- 如果关系不需要独立主键，也可以使用联合主键，但业务系统中更常用独立主键加唯一约束。
- 关系表可以保存关系属性，例如绑定时间、过期时间、状态、来源、创建人等。
- 两端对象的状态和关系本身的状态应分开设计，不要混用。
- 如果关系需要排序，可以在关系表中增加 `sort_no` 字段。
- 如果关系支持租户隔离，应在三张表中统一设计 `tenant_id`。
- 如果关系支持历史追溯，应保留软删除字段或单独设计关系变更记录表。
- 如果关系表数据量很大，应控制字段宽度，避免存储大文本或无边界扩展字段。

### 索引设计

多对多模型的索引设计重点在中间关系表。关系表通常要同时支持从左查右、从右查左、唯一性校验、状态过滤和批量查询。

常见索引设计如下：

```sql
ALTER TABLE sys_user
    ADD UNIQUE KEY uk_user_no (user_no),
    ADD UNIQUE KEY uk_username (username),
    ADD KEY idx_status_deleted (status, deleted);

ALTER TABLE sys_role
    ADD UNIQUE KEY uk_role_code (role_code),
    ADD KEY idx_role_type_status (role_type, status),
    ADD KEY idx_status_sort (status, sort_no);

ALTER TABLE sys_user_role
    ADD UNIQUE KEY uk_user_role_deleted (user_id, role_id, deleted),
    ADD KEY idx_user_status (user_id, relation_status, deleted),
    ADD KEY idx_role_status (role_id, relation_status, deleted),
    ADD KEY idx_expire_time (expire_time);
```

索引说明：

| 表              | 索引                   | 作用                                 |
| --------------- | ---------------------- | ------------------------------------ |
| `sys_user`      | `uk_user_no`           | 保证用户编号唯一，支持按用户编号查询 |
| `sys_user`      | `uk_username`          | 保证用户名唯一，支持登录查询         |
| `sys_user`      | `idx_status_deleted`   | 支持用户状态和删除标识过滤           |
| `sys_role`      | `uk_role_code`         | 保证角色编码唯一，支持权限配置引用   |
| `sys_role`      | `idx_role_type_status` | 支持按角色类型和状态筛选             |
| `sys_role`      | `idx_status_sort`      | 支持角色按状态和排序号展示           |
| `sys_user_role` | `uk_user_role_deleted` | 限制同一用户和同一角色的有效关系重复 |
| `sys_user_role` | `idx_user_status`      | 支持根据用户查询角色列表             |
| `sys_user_role` | `idx_role_status`      | 支持根据角色查询用户列表             |
| `sys_user_role` | `idx_expire_time`      | 支持清理或查询过期关系               |

设计索引时应注意以下问题：

- 关系表必须同时支持 `user_id` 查 `role_id` 和 `role_id` 查 `user_id`。
- 如果只设计 `(user_id, role_id)` 索引，反向按 `role_id` 查询可能无法高效命中索引。
- 关系唯一性通常通过 `(user_id, role_id, deleted)` 或业务层约束保证。
- 如果业务不允许删除后重复绑定，需要改为 `(user_id, role_id)` 唯一索引。
- 软删除字段参与唯一索引时，要明确是否允许删除后重新绑定。
- 高频状态过滤查询应将 `relation_status`、`deleted` 放入组合索引。
- 关系表数据量通常增长较快，不应为低频字段创建过多索引。
- 如果关系表按租户隔离，应将 `tenant_id` 放入高频组合索引的前部。
- 如果关系表需要按时间清理，应为 `expire_time`、`create_time` 等字段建立必要索引。

### 常用查询

多对多模型的常用查询主要围绕两类方向展开：从左侧对象查询右侧对象，从右侧对象查询左侧对象。除此之外，还包括关系存在性校验、批量查询、未绑定数据查询、关系数量统计和过期关系查询。

#### 查询用户拥有的角色

该查询用于用户详情页、权限加载和登录授权场景。

```sql
SELECT
    r.id,
    r.role_code,
    r.role_name,
    r.role_type,
    r.status,
    ur.bind_time,
    ur.expire_time
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
WHERE ur.user_id = 10001
  AND ur.relation_status = 1
  AND ur.deleted = 0
  AND r.status = 1
ORDER BY r.sort_no ASC, r.id ASC;
```

该查询应优先命中关系表中的 `user_id` 相关索引，再通过角色主键查询角色表。

#### 查询角色下的用户

该查询用于角色详情页、权限管理后台和角色成员管理。

```sql
SELECT
    u.id,
    u.user_no,
    u.username,
    u.real_name,
    u.mobile,
    u.status,
    ur.bind_time,
    ur.expire_time
FROM sys_user_role ur
INNER JOIN sys_user u ON u.id = ur.user_id AND u.deleted = 0
WHERE ur.role_id = 20001
  AND ur.relation_status = 1
  AND ur.deleted = 0
  AND u.status = 1
ORDER BY ur.bind_time DESC, u.id DESC
LIMIT 0, 20;
```

如果角色下用户数量较大，应对子结果进行分页，不要一次性返回全部用户。

#### 查询用户的角色编码

该查询常用于接口鉴权、权限缓存构建和登录上下文初始化。

```sql
SELECT
    r.role_code
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
WHERE ur.user_id = 10001
  AND ur.relation_status = 1
  AND ur.deleted = 0
  AND r.status = 1;
```

业务系统中通常会将该查询结果缓存到 Redis、本地缓存或登录会话中，避免每次接口请求都查询数据库。

#### 批量查询多个用户的角色

该查询用于用户列表页展示角色信息，或批量构建权限上下文。

```sql
SELECT
    ur.user_id,
    r.id AS role_id,
    r.role_code,
    r.role_name
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
WHERE ur.user_id IN (10001, 10002, 10003)
  AND ur.relation_status = 1
  AND ur.deleted = 0
  AND r.status = 1
ORDER BY ur.user_id ASC, r.sort_no ASC, r.id ASC;
```

主表分页后再批量查询关系数据，通常比在主表分页 SQL 中直接聚合角色更稳定。

#### 校验用户是否拥有指定角色

该查询用于权限判断、操作拦截和业务规则校验。

```sql
SELECT
    COUNT(1)
FROM sys_user_role ur
INNER JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
WHERE ur.user_id = 10001
  AND r.role_code = 'ADMIN'
  AND ur.relation_status = 1
  AND ur.deleted = 0
  AND r.status = 1;
```

如果该校验非常高频，应优先使用缓存，不建议在每个请求中实时查询数据库。

#### 查询未绑定指定角色的用户

该查询用于给某个角色添加用户时，展示尚未绑定该角色的用户列表。

```sql
SELECT
    u.id,
    u.user_no,
    u.username,
    u.real_name,
    u.mobile,
    u.status
FROM sys_user u
LEFT JOIN sys_user_role ur ON ur.user_id = u.id
    AND ur.role_id = 20001
    AND ur.deleted = 0
WHERE u.deleted = 0
  AND u.status = 1
  AND ur.id IS NULL
ORDER BY u.create_time DESC
LIMIT 0, 20;
```

该查询适合后台管理页面使用。如果用户量很大，应增加用户名、手机号、部门等筛选条件。

#### 查询用户关系数量

该查询用于统计用户拥有的角色数量。

```sql
SELECT
    user_id,
    COUNT(1) AS role_count
FROM sys_user_role
WHERE user_id IN (10001, 10002, 10003)
  AND relation_status = 1
  AND deleted = 0
GROUP BY user_id;
```

如果统计值高频展示，可以考虑冗余角色数量字段，或通过异步任务维护统计汇总表。

#### 查询角色绑定人数

该查询用于角色管理列表展示每个角色下的用户数量。

```sql
SELECT
    role_id,
    COUNT(1) AS user_count
FROM sys_user_role
WHERE role_id IN (20001, 20002, 20003)
  AND relation_status = 1
  AND deleted = 0
GROUP BY role_id;
```

当角色关系表数据量很大时，该类统计不建议在高频列表中实时执行。

#### 查询即将过期的关系

如果关系表包含过期时间，可以按过期时间查询即将失效的授权关系。

```sql
SELECT
    id,
    user_id,
    role_id,
    bind_time,
    expire_time
FROM sys_user_role
WHERE relation_status = 1
  AND deleted = 0
  AND expire_time IS NOT NULL
  AND expire_time >= NOW()
  AND expire_time < DATE_ADD(NOW(), INTERVAL 7 DAY)
ORDER BY expire_time ASC
LIMIT 100;
```

该查询适合定时任务、授权提醒、临时权限清理等场景。

### 常用写入

多对多模型的写入重点在关系表。常见操作包括新增关系、批量绑定、取消绑定、替换绑定关系、启用禁用关系、清理过期关系等。写入时应保证两端对象存在且可用，并通过唯一约束或事务避免重复关系。

#### 新增单条关系

新增关系前，业务层应校验用户和角色都存在且未删除、未禁用。

```sql
INSERT INTO sys_user_role (
    id,
    user_id,
    role_id,
    relation_status,
    bind_time,
    expire_time,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES (
    30001,
    10001,
    20001,
    1,
    NOW(),
    NULL,
    1,
    NOW(),
    1,
    NOW(),
    0
);
```

如果存在唯一索引，应处理重复绑定导致的唯一键冲突，并返回明确业务提示。

#### 批量绑定关系

批量绑定适合给用户分配多个角色，或给角色批量添加多个用户。

```sql
INSERT INTO sys_user_role (
    id,
    user_id,
    role_id,
    relation_status,
    bind_time,
    expire_time,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES
    (30002, 10001, 20001, 1, NOW(), NULL, 1, NOW(), 1, NOW(), 0),
    (30003, 10001, 20002, 1, NOW(), NULL, 1, NOW(), 1, NOW(), 0),
    (30004, 10001, 20003, 1, NOW(), NULL, 1, NOW(), 1, NOW(), 0);
```

批量写入时应控制单批数量，避免 SQL 过大。对可能重复的数据，可以先查询已有关系，也可以使用唯一索引兜底。

#### 忽略重复关系写入

如果业务允许重复绑定请求被幂等处理，可以使用 `INSERT IGNORE`。

```sql
INSERT IGNORE INTO sys_user_role (
    id,
    user_id,
    role_id,
    relation_status,
    bind_time,
    expire_time,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES
    (30005, 10001, 20001, 1, NOW(), NULL, 1, NOW(), 1, NOW(), 0),
    (30006, 10001, 20004, 1, NOW(), NULL, 1, NOW(), 1, NOW(), 0);
```

该写法依赖唯一索引实现幂等。使用时应注意，被忽略的数据不会报错，业务层如果需要知道哪些绑定失败，需要额外查询或比对。

#### 替换用户角色

替换关系是后台权限管理中最常见的操作。通常在同一个事务中先软删除旧关系，再插入新关系。

```sql
START TRANSACTION;

UPDATE sys_user_role
SET deleted = 1,
    relation_status = 0,
    update_user = 1,
    update_time = NOW()
WHERE user_id = 10001
  AND deleted = 0;

INSERT INTO sys_user_role (
    id,
    user_id,
    role_id,
    relation_status,
    bind_time,
    expire_time,
    create_user,
    create_time,
    update_user,
    update_time,
    deleted
) VALUES
    (30007, 10001, 20002, 1, NOW(), NULL, 1, NOW(), 1, NOW(), 0),
    (30008, 10001, 20003, 1, NOW(), NULL, 1, NOW(), 1, NOW(), 0);

COMMIT;
```

该方式简单直接，适合关系数量不大、后台管理操作频率不高的场景。如果关系数量很大，应改为差异比对，只新增缺失关系、删除多余关系。

#### 取消绑定关系

取消绑定通常使用软删除，便于后续审计和追溯。

```sql
UPDATE sys_user_role
SET deleted = 1,
    relation_status = 0,
    update_user = 1,
    update_time = NOW()
WHERE user_id = 10001
  AND role_id = 20001
  AND deleted = 0;
```

取消绑定后，如果权限信息被缓存，应同步清理或刷新权限缓存。

#### 禁用关系

禁用关系与软删除不同。禁用表示关系仍存在，但暂时不可用；软删除表示关系被移除。

```sql
UPDATE sys_user_role
SET relation_status = 0,
    update_user = 1,
    update_time = NOW()
WHERE user_id = 10001
  AND role_id = 20001
  AND deleted = 0;
```

如果业务只需要移除关系，使用软删除即可；如果需要临时停用授权，可以使用关系状态字段。

#### 恢复关系

如果关系被禁用但未软删除，可以直接恢复状态。

```sql
UPDATE sys_user_role
SET relation_status = 1,
    update_user = 1,
    update_time = NOW()
WHERE user_id = 10001
  AND role_id = 20001
  AND deleted = 0;
```

如果关系已软删除，是否允许恢复应由业务规则决定。允许恢复时，需要注意唯一索引与历史记录冲突问题。

#### 清理过期关系

临时授权、限时角色、短期标签等场景可以通过过期时间控制关系有效期。

```sql
UPDATE sys_user_role
SET relation_status = 0,
    update_user = 0,
    update_time = NOW()
WHERE relation_status = 1
  AND deleted = 0
  AND expire_time IS NOT NULL
  AND expire_time < NOW();
```

该操作适合定时任务执行。若数据量较大，应分批处理，避免一次更新过多数据。

### 常见问题

多对多模型的问题通常集中在关系重复、查询方向单一、关系表膨胀、权限缓存不一致和软删除唯一约束设计上。

| 问题                   | 说明                                               | 建议                                           |
| ---------------------- | -------------------------------------------------- | ---------------------------------------------- |
| 使用逗号拼接 ID        | 在用户表中保存 `role_ids` 这类字段                 | 使用中间关系表，避免查询、更新和约束困难       |
| 关系重复               | 同一用户和角色出现多条有效关系                     | 使用唯一索引或业务事务控制                     |
| 只能从一端高效查询     | 只建了 `user_id` 索引，按 `role_id` 查询很慢       | 关系表两端字段都要设计查询索引                 |
| 主表分页直接关联关系表 | 一条主记录被多条关系记录放大，分页结果不稳定       | 主表分页和关系查询分开处理                     |
| 软删除影响唯一性       | 删除后重新绑定时出现唯一键冲突，或允许重复历史数据 | 明确是否允许重复绑定，再设计唯一索引           |
| 权限缓存不一致         | 修改用户角色后，缓存仍保存旧角色                   | 关系变更后同步删除或刷新权限缓存               |
| 关系表数据量过大       | 关系表增长速度远高于两端主表                       | 设计归档、分区、分库分表或按租户拆分           |
| 关系状态和对象状态混淆 | 用户禁用、角色禁用、关系禁用语义不清               | 三类状态分别设计，查询时同时校验               |
| 批量替换覆盖异常       | 多人同时编辑同一用户角色，后提交覆盖先提交         | 使用版本号、业务锁或差异更新                   |
| 删除主对象后关系残留   | 用户或角色已删除，但关系仍有效                     | 删除主对象时同步软删除关系，查询时校验两端状态 |
| 关系属性放错位置       | 绑定时间、过期时间、来源等字段放到用户表或角色表   | 关系属性应放在中间关系表                       |

当关系本身需要完整审计、审批、版本控制或事件通知时，可以结合操作日志模型、审计日志模型、历史版本模型或 Outbox 事件表模型进一步扩展。

### 总结

多对多模型适合表达两个业务对象之间互相拥有多条关联关系的场景。该模型通常由两张业务主表和一张中间关系表组成，两端主表保存自身数据，中间关系表保存绑定关系和关系属性。

设计多对多模型时，应重点关注中间关系表的字段设计、唯一性约束、双向查询索引、关系状态、软删除策略和缓存一致性。关系表不能简单视为临时辅助表，它通常是多对多模型中最关键的数据表。随着关系数据量增长，应及时评估批量查询、缓存、归档、分区、分库分表和权限读模型优化方案。