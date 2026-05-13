# 扩展字段与读模型

扩展字段与读模型用于解决业务系统中字段变化、查询性能、读写模型分离、历史数据固化等问题。该类模型通常不直接改变核心业务主表的职责，而是通过冗余字段、快照、宽表、JSON 扩展字段或动态属性表等方式，提高查询效率、降低跨表关联复杂度，并增强业务字段的可扩展性。

## 冗余字段模型

冗余字段模型是指在业务表中保存一部分来自其他表的字段副本，用于减少高频查询中的关联次数，提高列表页、详情页、统计查询或业务判断的效率。冗余字段不是数据的唯一来源，它通常依赖主数据表维护原始值，并通过业务写入、异步同步、定时修复或数据校验机制保持最终一致。

### 适用场景

冗余字段适合用于读多写少、查询链路频繁关联、字段变化频率较低、允许一定同步延迟或可以通过补偿机制修复的业务场景。

典型场景包括订单表冗余用户昵称、商品名称、店铺名称，订单明细表冗余商品 SKU 编码、规格名称、销售单价，支付单冗余订单号、用户编号、渠道名称，审批单冗余申请人姓名、部门名称，消息表冗余接收人昵称等。

冗余字段通常用于以下几类需求：

| 场景       | 说明                                                       |
| ---------- | ---------------------------------------------------------- |
| 列表查询   | 避免每次查询列表时关联用户表、商品表、组织表等基础表       |
| 条件筛选   | 将常用筛选字段提前写入业务表，减少复杂 JOIN                |
| 展示字段   | 保存页面展示所需的名称、编码、类型文案等                   |
| 历史保留   | 保存业务发生时的字段值，避免基础数据修改后影响历史单据展示 |
| 异步读模型 | 为后续搜索、统计、报表或宽表同步提供基础字段               |

不适合冗余的字段包括强一致要求极高、变化非常频繁、字段含义不稳定、数据体积过大、更新传播链路复杂且缺少补偿机制的字段。例如账户余额、实时库存、实时权限结果等字段一般不应仅依赖冗余值做最终判断。

### 建模结构

冗余字段模型通常由主数据表、业务单据表和冗余字段组成。主数据表保存权威数据，业务单据表保存业务发生时需要直接查询或展示的字段副本。

下面以订单表冗余用户和店铺信息为例。该结构只描述表关系和字段职责，不在本章节中混入索引设计，索引会在后续“索引设计”小节单独给出。

```sql
CREATE TABLE biz_user (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_no VARCHAR(32) NOT NULL COMMENT '用户编号',
    nickname VARCHAR(64) NOT NULL COMMENT '用户昵称',
    mobile VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE biz_shop (
    id BIGINT NOT NULL COMMENT '主键ID',
    shop_no VARCHAR(32) NOT NULL COMMENT '店铺编号',
    shop_name VARCHAR(128) NOT NULL COMMENT '店铺名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

CREATE TABLE trade_order (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_no VARCHAR(32) NOT NULL COMMENT '冗余用户编号',
    user_nickname VARCHAR(64) NOT NULL COMMENT '冗余用户昵称',
    shop_id BIGINT NOT NULL COMMENT '店铺ID',
    shop_no VARCHAR(32) NOT NULL COMMENT '冗余店铺编号',
    shop_name VARCHAR(128) NOT NULL COMMENT '冗余店铺名称',
    order_status TINYINT NOT NULL COMMENT '订单状态：10待支付，20已支付，30已发货，40已完成，50已取消',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';
```

该模型中，`trade_order.user_id` 和 `trade_order.shop_id` 仍然保留原始关联关系，便于追溯权威数据。`user_no`、`user_nickname`、`shop_no`、`shop_name` 属于冗余字段，用于订单列表、订单详情、运营查询等读场景。

冗余字段不能替代主数据字段的权威性。用户昵称、店铺名称等字段发生变更后，订单历史数据是否同步更新，需要根据业务语义决定。如果订单需要展示“下单时的名称”，则不应回刷历史订单；如果订单列表要求展示“当前最新名称”，则需要同步更新冗余字段。

### 字段设计

冗余字段设计的重点是明确字段来源、同步策略、业务语义和一致性要求。字段命名应尽量体现来源和含义，避免出现无法判断权威来源的通用字段名。

建议字段设计如下：

| 字段类型 | 命名示例                     | 设计说明                                         |
| -------- | ---------------------------- | ------------------------------------------------ |
| 关联主键 | `user_id`、`shop_id`         | 保存权威数据表主键，用于追溯和必要时关联         |
| 业务编号 | `user_no`、`shop_no`         | 冗余稳定业务编号，便于展示、查询和外部对账       |
| 展示名称 | `user_nickname`、`shop_name` | 冗余页面高频展示字段，减少 JOIN                  |
| 状态字段 | `user_status`、`shop_status` | 仅在查询强依赖状态时冗余，否则容易造成一致性问题 |
| 时间字段 | `source_updated_at`          | 可选，用于记录来源数据最后同步时间               |
| 同步标识 | `redundant_sync_status`      | 可选，用于复杂同步链路中的状态追踪               |

字段设计时需要注意以下原则：

第一，冗余字段应优先选择稳定字段。编号、名称、类型文案、组织名称等字段比较适合冗余；余额、库存、实时权限等字段不适合直接作为最终判断依据。

第二，冗余字段要明确历史语义。订单、合同、发票等历史单据通常需要保存业务发生时的字段值；用户列表、商品列表、运营看板等查询模型通常更偏向展示当前最新值。

第三，冗余字段长度应与来源字段保持一致或略大。避免来源字段长度调整后，冗余字段截断数据。

第四，涉及金额、状态、枚举等字段时，应冗余原始编码而不是只冗余展示文案。展示文案适合页面展示，原始编码适合查询、统计和程序判断。

第五，冗余字段应避免无限扩张。一个表中冗余字段过多时，需要考虑是否应该改为宽表模型、快照模型或独立读模型。

### 索引设计

冗余字段的索引设计应围绕真实查询条件展开，而不是因为字段被冗余就默认建立索引。索引应优先服务高频查询、分页排序、条件筛选和唯一性约束。

订单表常见索引可以按以下方式设计：

```sql
ALTER TABLE trade_order
    ADD UNIQUE KEY uk_order_no (order_no),
    ADD KEY idx_user_created (user_id, created_at),
    ADD KEY idx_shop_status_created (shop_id, order_status, created_at),
    ADD KEY idx_order_status_created (order_status, created_at),
    ADD KEY idx_user_no_created (user_no, created_at),
    ADD KEY idx_shop_no_created (shop_no, created_at);
```

索引设计说明如下：

| 索引                       | 适用查询             | 说明                                   |
| -------------------------- | -------------------- | -------------------------------------- |
| `uk_order_no`              | 根据订单号查询       | 保证订单号唯一，并支持详情查询         |
| `idx_user_created`         | 查询用户订单列表     | 支持按用户分页查询订单                 |
| `idx_shop_status_created`  | 查询店铺不同状态订单 | 适合商家后台订单列表                   |
| `idx_order_status_created` | 查询某状态订单       | 适合运营后台按状态筛选                 |
| `idx_user_no_created`      | 根据用户编号查询订单 | 适合外部系统或运营工具使用业务编号查询 |
| `idx_shop_no_created`      | 根据店铺编号查询订单 | 适合店铺编号作为查询入口的场景         |

如果冗余字段只是展示字段，例如 `user_nickname`、`shop_name`，通常不建议直接建立普通索引。名称字段容易重复、长度较长、筛选选择性可能较差，索引收益有限。若确实存在按名称模糊搜索需求，应优先考虑搜索辅助表、全文索引或 Elasticsearch，而不是在核心交易表上直接堆叠大量低选择性索引。

### 常用查询

常用查询应优先体现冗余字段减少 JOIN 的价值。查询时直接读取业务表中的冗余字段，避免列表页频繁关联用户表、店铺表或其他基础表。

#### 根据订单号查询订单详情

订单详情查询通常通过唯一订单号定位单条数据。由于订单表已经冗余了用户昵称、店铺名称等展示字段，详情页基础信息可以直接返回。

```sql
SELECT
    id,
    order_no,
    user_id,
    user_no,
    user_nickname,
    shop_id,
    shop_no,
    shop_name,
    order_status,
    pay_amount,
    paid_at,
    created_at
FROM trade_order
WHERE order_no = 'O202605130001'
  AND deleted = 0;
```

该查询适合订单详情页、客服查询、运营后台按订单号检索等场景。只有在需要展示用户最新资料、店铺最新资质或其他实时信息时，才需要额外关联主数据表。

#### 查询用户订单列表

用户订单列表是冗余字段模型最常见的使用场景。订单列表可以直接展示用户编号、用户昵称、店铺名称等字段，避免每页订单数据都进行多表关联。

```sql
SELECT
    id,
    order_no,
    user_no,
    user_nickname,
    shop_no,
    shop_name,
    order_status,
    pay_amount,
    created_at
FROM trade_order
WHERE user_id = 10001
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

如果使用游标分页，可以基于 `created_at` 和 `id` 继续向后查询，避免深分页带来的性能问题。

```sql
SELECT
    id,
    order_no,
    user_no,
    user_nickname,
    shop_no,
    shop_name,
    order_status,
    pay_amount,
    created_at
FROM trade_order
WHERE user_id = 10001
  AND deleted = 0
  AND (
      created_at < '2026-05-13 10:00:00'
      OR (created_at = '2026-05-13 10:00:00' AND id < 90001)
  )
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

游标分页适合数据量较大的订单列表。实际使用时，建议索引顺序与查询条件和排序方式保持一致。

#### 查询店铺订单列表

店铺后台通常需要按店铺、订单状态和创建时间查询订单。冗余店铺编号和店铺名称后，结果集可以直接用于列表展示。

```sql
SELECT
    id,
    order_no,
    user_no,
    user_nickname,
    shop_no,
    shop_name,
    order_status,
    pay_amount,
    paid_at,
    created_at
FROM trade_order
WHERE shop_id = 20001
  AND order_status = 20
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 50;
```

该查询通常匹配 `idx_shop_status_created` 这类联合索引。对于商家后台列表，建议优先使用主键 ID、店铺 ID、状态和时间范围过滤，避免只按名称字段筛选。

#### 根据冗余业务编号查询

部分系统中，外部接口、运营工具或客服系统更习惯使用业务编号查询，而不是内部主键 ID。此时冗余 `user_no`、`shop_no` 可以提升查询便利性。

```sql
SELECT
    id,
    order_no,
    user_no,
    user_nickname,
    shop_no,
    shop_name,
    order_status,
    pay_amount,
    created_at
FROM trade_order
WHERE user_no = 'U10000001'
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

业务编号通常比名称字段更稳定，也更适合作为查询条件。若业务编号会参与高频查询，应在索引设计中单独考虑。

#### 查询运营后台订单列表

运营后台经常按状态、时间范围、店铺编号、用户编号组合查询。此类查询应控制条件组合，避免任意字段自由组合导致索引难以命中。

```sql
SELECT
    id,
    order_no,
    user_no,
    user_nickname,
    shop_no,
    shop_name,
    order_status,
    pay_amount,
    paid_at,
    created_at
FROM trade_order
WHERE order_status = 20
  AND created_at >= '2026-05-01 00:00:00'
  AND created_at < '2026-06-01 00:00:00'
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 100;
```

运营后台查询应尽量要求时间范围。对于交易表、日志表、流水表等持续增长的大表，不建议允许无时间范围的全量查询。

### 常用写入

冗余字段写入的核心是保证创建业务数据时一次性写入必要的字段副本，并在来源数据变化时按业务规则决定是否同步更新。

#### 创建订单时写入冗余字段

创建订单时，应先读取用户和店铺的权威数据，再将需要冗余的字段写入订单表。这样订单创建完成后，列表和详情查询即可直接读取订单表。

```sql
INSERT INTO trade_order (
    id,
    order_no,
    user_id,
    user_no,
    user_nickname,
    shop_id,
    shop_no,
    shop_name,
    order_status,
    pay_amount,
    created_at,
    updated_at,
    deleted
)
SELECT
    90001,
    'O202605130001',
    u.id,
    u.user_no,
    u.nickname,
    s.id,
    s.shop_no,
    s.shop_name,
    10,
    199.00,
    NOW(),
    NOW(),
    0
FROM biz_user u
JOIN biz_shop s ON s.id = 20001
WHERE u.id = 10001
  AND u.status = 1
  AND s.status = 1;
```

该写法适合在 SQL 层直接完成字段读取和写入。实际业务代码中，也可以先查询用户和店铺信息，再通过应用层组装订单对象后写入。

#### 来源字段变更时同步冗余字段

当用户昵称发生变化时，是否同步历史订单取决于业务语义。如果订单需要展示最新用户昵称，可以回刷未删除订单中的冗余字段。

```sql
UPDATE trade_order
SET user_nickname = '新的用户昵称',
    updated_at = NOW()
WHERE user_id = 10001
  AND deleted = 0;
```

如果订单需要保留下单时的用户昵称，则不应执行该同步。此时用户昵称修改只影响用户表，不影响历史订单。

#### 店铺名称变更时同步部分订单

店铺名称变更后，部分业务只要求未完成订单展示最新店铺名称，已完成订单保留历史名称。可以按订单状态限定同步范围。

```sql
UPDATE trade_order
SET shop_name = '新的店铺名称',
    updated_at = NOW()
WHERE shop_id = 20001
  AND order_status IN (10, 20, 30)
  AND deleted = 0;
```

这种写法比全量回刷更安全，可以避免历史单据展示发生不可预期变化。

#### 批量修复冗余字段

当发现冗余字段与来源表不一致时，可以通过批量修复 SQL 进行补偿。修复前建议先查询差异数据，再分批更新。

```sql
SELECT
    o.id,
    o.order_no,
    o.user_nickname AS order_user_nickname,
    u.nickname AS current_user_nickname
FROM trade_order o
JOIN biz_user u ON u.id = o.user_id
WHERE o.user_nickname <> u.nickname
  AND o.deleted = 0
ORDER BY o.id
LIMIT 100;
```

确认差异后再执行批量修复：

```sql
UPDATE trade_order o
JOIN biz_user u ON u.id = o.user_id
SET o.user_nickname = u.nickname,
    o.updated_at = NOW()
WHERE o.user_nickname <> u.nickname
  AND o.deleted = 0
  AND o.id BETWEEN 1 AND 100000;
```

大表修复时不建议一次性全量更新。应按主键范围、创建时间范围或分页批次执行，避免长事务、锁等待和主从延迟。

### 常见问题

冗余字段模型的主要问题集中在一致性、字段膨胀、更新成本和业务语义不清晰上。设计时应先明确冗余字段的目的，再决定同步策略。

第一个常见问题是冗余字段与来源表不一致。该问题通常由异步同步失败、业务代码漏更新、历史数据未回刷或人工修改数据导致。解决方式包括在写入链路中统一封装冗余字段组装逻辑，使用消息事件同步变更，增加定时校验任务，以及提供可重复执行的修复脚本。

第二个常见问题是不清楚冗余字段应该保存历史值还是最新值。例如订单中的商品名称，到底展示下单时名称还是商品当前名称，需要由业务明确。若是交易凭证、合同、发票、账单等历史单据，通常应保留业务发生时的值；若是运营查询、用户视图、搜索列表，则可能更适合展示最新值。

第三个常见问题是冗余字段越来越多，导致主表膨胀。主表字段过多会增加存储成本、降低缓存命中率，也会让写入链路变复杂。若一个业务表需要冗余大量展示字段，应考虑拆出读模型表、宽表或搜索辅助表。

第四个常见问题是在冗余字段上建立过多索引。冗余字段不等于查询字段，更不等于索引字段。索引应服务明确的查询模式。对于低选择性、长文本、模糊搜索字段，应谨慎加索引。

第五个常见问题是同步更新影响性能。来源字段变更后，如果需要回刷大量历史数据，可能造成长事务、锁竞争、binlog 膨胀和主从延迟。此类同步应尽量异步化、分批化，并具备失败重试能力。

### 总结

冗余字段模型通过在业务表中保存常用字段副本，减少查询时的跨表关联，提高列表页、详情页、运营后台和外部查询的性能。它适合读多写少、字段相对稳定、查询路径明确的业务场景。

设计冗余字段时，应明确字段来源、历史语义、同步策略和一致性要求。建模结构中保留原始关联字段，字段设计中控制冗余范围，索引设计中只为高频查询建立必要索引，写入链路中保证冗余字段可创建、可同步、可校验、可修复。

冗余字段不是万能优化手段。如果字段数量持续膨胀、查询组合复杂、数据同步链路过长，应考虑使用快照模型、宽表模型、JSON 扩展字段模型或独立读模型来承接复杂读场景。





## 快照模型

快照模型是指在业务发生时，将当时的关键业务数据完整保存到业务单据或快照表中，用于保证历史记录的稳定性、可追溯性和不可变语义。快照数据通常不跟随来源数据变化而变化，重点解决“当时是什么”的问题，而不是解决“现在是什么”的问题。

### 适用场景

快照模型适合用于历史数据不能被来源数据变更影响的业务场景。它强调业务发生时的数据固化，常用于交易、合同、审批、账单、发票、物流、保险、会员权益、价格规则等领域。

典型场景包括订单明细保存下单时的商品名称、SKU 规格、销售价、图片；合同保存签署时的甲乙方信息、合同条款、公司名称；发票保存开票时的购方名称、税号、地址电话；审批单保存提交时的申请人部门、岗位、审批规则；账户流水保存交易发生时的账户名称、币种、渠道信息。

快照模型通常用于以下几类需求：

| 场景     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 历史凭证 | 保存业务发生时的数据，避免来源数据修改后影响历史凭证         |
| 交易留痕 | 订单、账单、支付、退款等场景需要保留当时的商品、价格、用户、地址信息 |
| 审计追溯 | 审批、合同、发票、结算等场景需要明确当时的数据依据           |
| 规则固化 | 保存当时命中的价格规则、优惠规则、审批规则、权益规则         |
| 法务合规 | 合同、发票、保单等数据需要长期保持稳定，不允许被主数据变更影响 |

不适合使用快照模型的场景包括实时状态判断、实时余额计算、实时库存扣减、实时权限判断等。这类数据需要读取当前最新值，不应依赖历史快照作为最终判断依据。

### 建模结构

快照模型通常有两种建模方式：一种是将快照字段直接保存在业务明细表中，另一种是将快照内容保存到独立快照表中。前者适合字段固定、查询频繁、结构清晰的场景；后者适合快照内容较多、结构复杂或需要多版本保留的场景。

下面以订单和订单明细保存商品快照为例。订单明细表保存下单时的商品名称、SKU 编码、规格、销售价和商品主图，即使后续商品信息被修改，历史订单仍然展示下单时的数据。

```sql
CREATE TABLE product_spu (
    id BIGINT NOT NULL COMMENT '主键ID',
    spu_no VARCHAR(32) NOT NULL COMMENT 'SPU编号',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    main_image VARCHAR(512) DEFAULT NULL COMMENT '商品主图',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架，1上架',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU表';

CREATE TABLE product_sku (
    id BIGINT NOT NULL COMMENT '主键ID',
    sku_no VARCHAR(32) NOT NULL COMMENT 'SKU编号',
    spu_id BIGINT NOT NULL COMMENT 'SPU ID',
    spec_text VARCHAR(256) NOT NULL COMMENT '规格描述',
    sale_price DECIMAL(18,2) NOT NULL COMMENT '销售价格',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

CREATE TABLE trade_order (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL COMMENT '订单状态：10待支付，20已支付，30已发货，40已完成，50已取消',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE trade_order_item (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    spu_id BIGINT NOT NULL COMMENT 'SPU ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    sku_no VARCHAR(32) NOT NULL COMMENT 'SKU编号快照',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称快照',
    spec_text VARCHAR(256) NOT NULL COMMENT '规格描述快照',
    main_image VARCHAR(512) DEFAULT NULL COMMENT '商品主图快照',
    sale_price DECIMAL(18,2) NOT NULL COMMENT '销售单价快照',
    buy_quantity INT NOT NULL COMMENT '购买数量',
    item_amount DECIMAL(18,2) NOT NULL COMMENT '明细金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';
```

该结构中，`product_spu` 和 `product_sku` 是商品权威数据表，`trade_order_item` 中的 `product_name`、`spec_text`、`main_image`、`sale_price` 是下单时的商品快照。后续商品名称、规格、图片或销售价修改，不应影响历史订单明细。

如果快照内容较多，可以拆分独立快照表。例如合同、审批、保单等场景，快照可能包含大量主体信息、规则信息和扩展信息，此时适合使用独立快照表承载。

```sql
CREATE TABLE contract_snapshot (
    id BIGINT NOT NULL COMMENT '主键ID',
    contract_id BIGINT NOT NULL COMMENT '合同ID',
    contract_no VARCHAR(32) NOT NULL COMMENT '合同编号',
    snapshot_version INT NOT NULL COMMENT '快照版本号',
    party_a_name VARCHAR(128) NOT NULL COMMENT '甲方名称快照',
    party_a_credit_code VARCHAR(64) DEFAULT NULL COMMENT '甲方统一社会信用代码快照',
    party_b_name VARCHAR(128) NOT NULL COMMENT '乙方名称快照',
    party_b_credit_code VARCHAR(64) DEFAULT NULL COMMENT '乙方统一社会信用代码快照',
    contract_title VARCHAR(256) NOT NULL COMMENT '合同标题快照',
    contract_amount DECIMAL(18,2) NOT NULL COMMENT '合同金额快照',
    snapshot_content JSON NOT NULL COMMENT '合同内容快照',
    snapshot_time DATETIME NOT NULL COMMENT '快照生成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同快照表';
```

独立快照表适合需要多版本、内容较大、字段变化较多的业务。对于交易订单明细这种高频查询场景，通常直接在明细表中保存关键快照字段更合适。

### 字段设计

快照字段设计的重点是表达“业务发生时的数据状态”。字段命名可以直接使用业务字段名，也可以增加 `_snapshot` 后缀，但同一个系统内应保持统一风格。

建议字段设计如下：

| 字段类型 | 命名示例                                  | 设计说明                             |
| -------- | ----------------------------------------- | ------------------------------------ |
| 来源主键 | `spu_id`、`sku_id`、`contract_id`         | 保存来源数据主键，用于追溯来源记录   |
| 来源编号 | `sku_no`、`contract_no`                   | 保存业务编号，便于展示、查询、对账   |
| 展示快照 | `product_name`、`spec_text`、`main_image` | 保存业务发生时页面展示需要的数据     |
| 金额快照 | `sale_price`、`contract_amount`           | 保存当时参与计算的金额               |
| 规则快照 | `rule_id`、`rule_name`、`rule_content`    | 保存当时命中的规则或规则内容         |
| 版本字段 | `snapshot_version`                        | 用于多次生成快照或合同版本管理       |
| 生成时间 | `snapshot_time`                           | 标记快照生成时间                     |
| 扩展内容 | `snapshot_content`                        | 保存复杂快照内容，通常使用 JSON 类型 |

字段设计时需要注意以下原则：

第一，快照字段应保存业务计算和展示所需的完整信息。订单商品快照不能只保存商品 ID，否则商品名称、规格、价格修改后，历史订单就无法还原当时内容。

第二，金额类字段必须保存快照值。商品当前售价、会员价、促销价、优惠金额、税额、结算金额等字段一旦参与交易计算，就应在业务单据中固化。

第三，快照字段应避免依赖实时关联才能展示。快照模型的目标是历史稳定，因此历史详情页应尽量直接读取快照字段，而不是重新关联当前主数据。

第四，快照内容较大时，应考虑拆分独立快照表或使用 JSON 字段。合同条款、审批规则、保单权益、价格规则等复杂结构，不适合全部摊平成大量固定列。

第五，快照数据原则上不更新。除非发生数据修复、脱敏处理或合规处理，否则快照应保持业务发生时的原始状态。

### 索引设计

快照模型的索引设计应围绕业务单据查询、来源追溯、版本定位和时间范围查询展开。快照字段主要用于展示和历史还原，不代表所有快照字段都需要建立索引。

订单明细快照表常见索引如下：

```sql
ALTER TABLE trade_order
    ADD UNIQUE KEY uk_order_no (order_no),
    ADD KEY idx_user_status_created (user_id, order_status, created_at);

ALTER TABLE trade_order_item
    ADD KEY idx_order_id (order_id),
    ADD KEY idx_order_no (order_no),
    ADD KEY idx_sku_id_created (sku_id, created_at),
    ADD KEY idx_spu_id_created (spu_id, created_at);
```

合同快照表常见索引如下：

```sql
ALTER TABLE contract_snapshot
    ADD UNIQUE KEY uk_contract_version (contract_id, snapshot_version),
    ADD KEY idx_contract_no (contract_no),
    ADD KEY idx_snapshot_time (snapshot_time);
```

索引设计说明如下：

| 索引                  | 适用查询              | 说明                             |
| --------------------- | --------------------- | -------------------------------- |
| `uk_order_no`         | 根据订单号查询订单    | 保证订单号唯一，支持订单详情查询 |
| `idx_order_id`        | 查询订单明细          | 订单详情页根据订单 ID 查询明细   |
| `idx_order_no`        | 根据订单号查询明细    | 适合外部系统只传订单号的场景     |
| `idx_sku_id_created`  | 追溯某 SKU 的历史订单 | 用于商品维度的交易追踪           |
| `idx_spu_id_created`  | 追溯某 SPU 的历史订单 | 用于商品维度统计或排查           |
| `uk_contract_version` | 查询合同指定版本快照  | 保证同一合同版本唯一             |
| `idx_snapshot_time`   | 按快照时间查询        | 适合审计、导出、批量处理         |

商品名称、规格描述、主图地址、合同内容等快照展示字段通常不建议建立普通索引。它们要么长度较大，要么选择性较差，要么主要用于展示而非筛选。若存在按快照内容搜索的需求，应考虑搜索辅助表、全文索引或专门的检索系统。

### 常用查询

常用查询应体现快照模型的核心价值：历史详情直接读取快照字段，避免被当前主数据变更影响。查询时通常先定位业务单据，再读取对应快照明细。

### 查询订单详情和商品快照

订单详情页需要展示下单时的商品名称、规格、图片和销售价。此时应直接读取订单明细表中的快照字段，而不是关联当前商品表获取最新商品信息。

```sql
SELECT
    o.id AS order_id,
    o.order_no,
    o.user_id,
    o.order_status,
    o.total_amount,
    o.pay_amount,
    o.created_at,
    i.id AS item_id,
    i.spu_id,
    i.sku_id,
    i.sku_no,
    i.product_name,
    i.spec_text,
    i.main_image,
    i.sale_price,
    i.buy_quantity,
    i.item_amount
FROM trade_order o
JOIN trade_order_item i ON i.order_id = o.id
WHERE o.order_no = 'O202605130001'
  AND o.deleted = 0
ORDER BY i.id ASC;
```

该查询返回的是下单时的商品快照。即使当前商品已经改名、改价或下架，历史订单详情仍然保持稳定。

#### 查询订单明细列表

订单明细列表通常用于后台查看某个订单的商品组成。由于订单明细中已经保存商品快照，不需要额外关联 `product_spu` 和 `product_sku`。

```sql
SELECT
    id,
    order_no,
    spu_id,
    sku_id,
    sku_no,
    product_name,
    spec_text,
    sale_price,
    buy_quantity,
    item_amount,
    created_at
FROM trade_order_item
WHERE order_id = 90001
ORDER BY id ASC;
```

该查询适合订单详情、售后申请、客服核对、对账查看等场景。

#### 追溯某个 SKU 的历史订单

当需要排查某个 SKU 曾经在哪些订单中出现过时，可以通过来源主键 `sku_id` 查询历史订单明细。此时返回的是历史订单中的快照值，而不是当前商品信息。

```sql
SELECT
    id,
    order_no,
    sku_id,
    sku_no,
    product_name,
    spec_text,
    sale_price,
    buy_quantity,
    item_amount,
    created_at
FROM trade_order_item
WHERE sku_id = 30001
  AND created_at >= '2026-05-01 00:00:00'
  AND created_at < '2026-06-01 00:00:00'
ORDER BY created_at DESC
LIMIT 100;
```

该查询适合商品问题排查、批次售后、运营分析和交易追踪。查询时建议带上时间范围，避免扫描过大的历史明细数据。

#### 查询合同指定版本快照

合同类业务常见需求是查看某个合同在指定版本下的完整快照。此时可以通过合同 ID 和快照版本号定位。

```sql
SELECT
    id,
    contract_id,
    contract_no,
    snapshot_version,
    party_a_name,
    party_a_credit_code,
    party_b_name,
    party_b_credit_code,
    contract_title,
    contract_amount,
    snapshot_content,
    snapshot_time
FROM contract_snapshot
WHERE contract_id = 80001
  AND snapshot_version = 2;
```

该查询适合合同版本查看、审批记录追溯、签署记录比对等场景。`snapshot_content` 中可以保存合同条款、付款计划、履约节点等复杂结构。

#### 查询最新合同快照

如果一个合同存在多个快照版本，常见需求是查询最新版本。可以按快照版本号倒序或快照时间倒序查询。

```sql
SELECT
    id,
    contract_id,
    contract_no,
    snapshot_version,
    party_a_name,
    party_b_name,
    contract_title,
    contract_amount,
    snapshot_time
FROM contract_snapshot
WHERE contract_id = 80001
ORDER BY snapshot_version DESC
LIMIT 1;
```

该查询适合合同详情页展示当前最新已固化版本。需要注意，最新快照并不一定等于合同当前编辑态数据，应根据业务状态区分草稿、审批中、已签署等不同阶段。

#### 对比两个合同快照版本

版本对比通常用于合同修订、审批差异查看或审计。可以一次查询两个版本，再由应用层对比字段和 JSON 内容差异。

```sql
SELECT
    id,
    contract_id,
    contract_no,
    snapshot_version,
    party_a_name,
    party_b_name,
    contract_title,
    contract_amount,
    snapshot_content,
    snapshot_time
FROM contract_snapshot
WHERE contract_id = 80001
  AND snapshot_version IN (1, 2)
ORDER BY snapshot_version ASC;
```

复杂 JSON 内容不建议完全依赖 SQL 做差异比较。更常见的做法是在应用层解析两个版本的 `snapshot_content`，输出字段级差异。

### 常用写入

快照模型写入的关键是在业务确认、提交、支付、签署或审批等关键节点生成快照。快照生成后通常不再修改，后续来源数据变化不应影响已经生成的快照。

#### 创建订单时写入商品快照

创建订单时，需要读取当前商品和 SKU 信息，并将当时的数据写入订单明细表。订单明细写入完成后，历史订单展示只依赖快照字段。

```sql
INSERT INTO trade_order_item (
    id,
    order_id,
    order_no,
    spu_id,
    sku_id,
    sku_no,
    product_name,
    spec_text,
    main_image,
    sale_price,
    buy_quantity,
    item_amount,
    created_at,
    updated_at
)
SELECT
    91001,
    90001,
    'O202605130001',
    p.id,
    s.id,
    s.sku_no,
    p.product_name,
    s.spec_text,
    p.main_image,
    s.sale_price,
    2,
    s.sale_price * 2,
    NOW(),
    NOW()
FROM product_sku s
JOIN product_spu p ON p.id = s.spu_id
WHERE s.id = 30001
  AND s.status = 1
  AND p.status = 1;
```

该写法适合在 SQL 层直接从商品表生成明细快照。实际业务代码中，通常还需要结合库存校验、价格校验、优惠计算和订单金额校验一起完成。

#### 创建订单主表和明细快照

在真实业务中，订单主表和明细表应在同一个事务中写入，避免出现订单已创建但明细快照缺失的问题。

```sql
START TRANSACTION;

INSERT INTO trade_order (
    id,
    order_no,
    user_id,
    order_status,
    total_amount,
    pay_amount,
    created_at,
    updated_at,
    deleted
)
VALUES (
    90001,
    'O202605130001',
    10001,
    10,
    398.00,
    398.00,
    NOW(),
    NOW(),
    0
);

INSERT INTO trade_order_item (
    id,
    order_id,
    order_no,
    spu_id,
    sku_id,
    sku_no,
    product_name,
    spec_text,
    main_image,
    sale_price,
    buy_quantity,
    item_amount,
    created_at,
    updated_at
)
SELECT
    91001,
    90001,
    'O202605130001',
    p.id,
    s.id,
    s.sku_no,
    p.product_name,
    s.spec_text,
    p.main_image,
    s.sale_price,
    2,
    s.sale_price * 2,
    NOW(),
    NOW()
FROM product_sku s
JOIN product_spu p ON p.id = s.spu_id
WHERE s.id = 30001
  AND s.status = 1
  AND p.status = 1;

COMMIT;
```

事务中需要确保订单金额与明细金额一致。实际业务中，如果商品价格在提交订单前发生变化，应返回价格变更提示，而不是静默使用旧价格或新价格。

#### 生成合同快照

合同快照通常在提交审批、审批通过、签署前或签署完成时生成。生成快照时，应固化合同主体、金额、条款和扩展内容。

```sql
INSERT INTO contract_snapshot (
    id,
    contract_id,
    contract_no,
    snapshot_version,
    party_a_name,
    party_a_credit_code,
    party_b_name,
    party_b_credit_code,
    contract_title,
    contract_amount,
    snapshot_content,
    snapshot_time,
    created_at
)
VALUES (
    100001,
    80001,
    'C202605130001',
    1,
    '甲方有限公司',
    '91330000123456789X',
    '乙方科技有限公司',
    '91330000987654321X',
    '软件服务合同',
    120000.00,
    JSON_OBJECT(
        'paymentPlan', JSON_ARRAY(
            JSON_OBJECT('stage', '首付款', 'amount', 60000.00),
            JSON_OBJECT('stage', '验收款', 'amount', 60000.00)
        ),
        'servicePeriod', '2026-06-01至2027-05-31',
        'terms', JSON_ARRAY('服务范围', '验收标准', '违约责任')
    ),
    NOW(),
    NOW()
);
```

该写法适合保存结构化合同内容。对于合同正文、附件、签章文件等大对象，通常不直接保存在 MySQL 行内，而是保存文件地址、文件摘要、版本号和元数据。

#### 生成新版本快照

当合同发生修订时，不应覆盖原快照，而应新增一个版本。版本号可以由应用层生成，也可以通过查询当前最大版本号后递增。

```sql
INSERT INTO contract_snapshot (
    id,
    contract_id,
    contract_no,
    snapshot_version,
    party_a_name,
    party_a_credit_code,
    party_b_name,
    party_b_credit_code,
    contract_title,
    contract_amount,
    snapshot_content,
    snapshot_time,
    created_at
)
SELECT
    100002,
    contract_id,
    contract_no,
    snapshot_version + 1,
    party_a_name,
    party_a_credit_code,
    party_b_name,
    party_b_credit_code,
    '软件服务合同-修订版',
    150000.00,
    JSON_SET(
        snapshot_content,
        '$.servicePeriod',
        '2026-06-01至2027-12-31'
    ),
    NOW(),
    NOW()
FROM contract_snapshot
WHERE contract_id = 80001
ORDER BY snapshot_version DESC
LIMIT 1;
```

该写法适合基于上一版快照生成新版本。实际业务中，应注意并发修订问题，避免两个事务生成相同版本号。可以通过唯一索引和重试机制保证版本唯一。

#### 快照数据修复

快照原则上不修改，但如果出现生成错误、敏感信息处理、历史数据迁移等情况，可以进行受控修复。修复前应保留修复记录，避免破坏审计链路。

```sql
UPDATE trade_order_item
SET main_image = 'https://static.example.com/product/default.png',
    updated_at = NOW()
WHERE order_no = 'O202605130001'
  AND sku_id = 30001
  AND main_image IS NULL;
```

快照修复应严格限制条件，避免批量误更新。对于合同、发票、支付、账务等高敏感数据，建议通过修复单、审批流程和操作日志记录修复原因。

### 常见问题

快照模型最常见的问题是把快照误当作实时数据，或者在来源数据变更后错误地回刷历史快照。快照的核心价值是历史稳定，因此是否允许更新必须非常谨慎。

第一个常见问题是快照字段不完整。只保存商品 ID、用户 ID 或合同 ID，而没有保存当时的名称、规格、价格、主体信息，会导致历史数据无法还原。解决方式是在业务关键节点明确快照字段清单，将展示字段、计算字段和规则字段一次性固化。

第二个常见问题是来源数据修改后同步更新快照。对于订单、合同、发票等历史凭证，来源数据修改通常不应影响已经生成的快照。若业务要求展示最新信息，应使用冗余字段模型或读模型，而不是修改历史快照。

第三个常见问题是快照内容过大。将大量合同正文、审批规则、商品描述、富文本内容都塞入主业务表，会导致行过大、查询变慢、缓存效率下降。解决方式是拆分独立快照表，或将大文件存储到对象存储中，在 MySQL 中保存文件地址和摘要。

第四个常见问题是快照版本混乱。合同、审批、报价等业务可能存在多次修订，如果没有版本号和生成时间，很难判断哪个快照对应哪个业务阶段。解决方式是增加 `snapshot_version`、`snapshot_time`、`snapshot_type` 等字段，并通过唯一约束保证版本不重复。

第五个常见问题是快照与当前主数据的查询边界不清晰。历史详情页应读取快照字段，当前管理页应读取主数据字段。如果页面混合展示历史值和当前值，应明确标识，避免用户误解。

第六个常见问题是 JSON 快照不可控。JSON 字段虽然灵活，但如果没有固定结构、版本说明和字段校验，后续解析、迁移和对比会变困难。复杂快照使用 JSON 时，应在应用层维护结构版本，并对关键字段进行校验。

### 总结

快照模型用于固化业务发生时的数据状态，核心目标是保证历史记录稳定、可追溯、不可被来源数据变更影响。它适合订单、合同、发票、账单、审批、支付、结算等历史凭证类场景。

设计快照模型时，应明确快照生成时机、字段范围、版本策略和更新边界。订单明细这类高频查询场景适合直接在明细表中保存快照字段；合同、审批、保单等复杂场景适合使用独立快照表或 JSON 快照内容。

快照模型与冗余字段模型的区别在于：冗余字段更偏向查询性能和当前展示，可以按业务需要同步更新；快照模型更偏向历史固化和审计追溯，生成后原则上不再修改。业务建模时应先判断字段语义是“当前值”还是“当时值”，再决定使用冗余字段还是快照。





## 宽表模型

宽表模型是指面向查询、展示、搜索、导出或统计场景，将多个业务表中的常用字段汇总到一张读模型表中。宽表通常不是核心交易事实的唯一来源，而是由订单、用户、商品、支付、物流、组织、字典等多张表同步生成，用于减少复杂 JOIN、降低查询链路复杂度，并提升读请求性能。

### 适用场景

宽表模型适合用于字段来源多、查询条件复杂、列表展示字段多、读请求远高于写请求、可以接受最终一致性的业务场景。它本质上是为读优化而设计的数据模型。

典型场景包括订单运营后台列表、交易对账列表、售后工单查询、用户画像查询、商品搜索列表、财务结算明细、审批待办列表、报表导出任务等。

宽表模型通常用于以下几类需求：

| 场景         | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 后台复杂列表 | 一个列表需要展示订单、用户、店铺、支付、物流等多方字段 |
| 多条件筛选   | 查询条件来自多个业务表，直接 JOIN 成本高               |
| 大批量导出   | 导出字段多、数据量大，不适合实时关联多张表             |
| 搜索读模型   | 为搜索、筛选、排序提前准备扁平化字段                   |
| 报表明细     | 为统计汇总或 BI 明细层提供基础数据                     |
| 读写分离     | 写入走核心业务表，查询走专用读模型表                   |

不适合使用宽表模型的场景包括强事务写入、实时余额判断、实时库存扣减、数据字段很少且 JOIN 成本可控的小表查询。如果业务查询本身非常简单，强行引入宽表会增加同步复杂度和维护成本。

### 建模结构

宽表模型通常由多个来源表和一张读模型宽表组成。来源表负责承载核心业务事实，宽表负责承载高频查询需要的扁平化字段。

下面以订单查询宽表为例。订单主表保存交易核心信息，用户表保存用户资料，店铺表保存店铺资料，支付单表保存支付信息，物流表保存发货信息，订单查询宽表统一保存列表查询和导出所需字段。

```sql
CREATE TABLE biz_user (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_no VARCHAR(32) NOT NULL COMMENT '用户编号',
    nickname VARCHAR(64) NOT NULL COMMENT '用户昵称',
    mobile VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    user_level TINYINT NOT NULL DEFAULT 1 COMMENT '用户等级',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE biz_shop (
    id BIGINT NOT NULL COMMENT '主键ID',
    shop_no VARCHAR(32) NOT NULL COMMENT '店铺编号',
    shop_name VARCHAR(128) NOT NULL COMMENT '店铺名称',
    shop_type TINYINT NOT NULL COMMENT '店铺类型',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

CREATE TABLE trade_order (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    shop_id BIGINT NOT NULL COMMENT '店铺ID',
    order_status TINYINT NOT NULL COMMENT '订单状态：10待支付，20已支付，30已发货，40已完成，50已取消',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE trade_payment (
    id BIGINT NOT NULL COMMENT '主键ID',
    payment_no VARCHAR(32) NOT NULL COMMENT '支付单号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    pay_channel VARCHAR(32) NOT NULL COMMENT '支付渠道',
    pay_status TINYINT NOT NULL COMMENT '支付状态：10待支付，20支付成功，30支付失败，40已关闭',
    paid_at DATETIME DEFAULT NULL COMMENT '支付完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单表';

CREATE TABLE trade_delivery (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    delivery_no VARCHAR(32) DEFAULT NULL COMMENT '物流单号',
    delivery_company VARCHAR(64) DEFAULT NULL COMMENT '物流公司',
    delivery_status TINYINT NOT NULL DEFAULT 10 COMMENT '物流状态：10待发货，20已发货，30已签收',
    delivered_at DATETIME DEFAULT NULL COMMENT '发货时间',
    received_at DATETIME DEFAULT NULL COMMENT '签收时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流表';

CREATE TABLE trade_order_read_model (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    user_no VARCHAR(32) NOT NULL COMMENT '用户编号',
    user_nickname VARCHAR(64) NOT NULL COMMENT '用户昵称',
    user_mobile VARCHAR(32) DEFAULT NULL COMMENT '用户手机号',
    user_level TINYINT NOT NULL COMMENT '用户等级',
    shop_id BIGINT NOT NULL COMMENT '店铺ID',
    shop_no VARCHAR(32) NOT NULL COMMENT '店铺编号',
    shop_name VARCHAR(128) NOT NULL COMMENT '店铺名称',
    shop_type TINYINT NOT NULL COMMENT '店铺类型',
    order_status TINYINT NOT NULL COMMENT '订单状态',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    payment_no VARCHAR(32) DEFAULT NULL COMMENT '支付单号',
    pay_channel VARCHAR(32) DEFAULT NULL COMMENT '支付渠道',
    pay_status TINYINT DEFAULT NULL COMMENT '支付状态',
    paid_at DATETIME DEFAULT NULL COMMENT '支付完成时间',
    delivery_no VARCHAR(32) DEFAULT NULL COMMENT '物流单号',
    delivery_company VARCHAR(64) DEFAULT NULL COMMENT '物流公司',
    delivery_status TINYINT DEFAULT NULL COMMENT '物流状态',
    delivered_at DATETIME DEFAULT NULL COMMENT '发货时间',
    received_at DATETIME DEFAULT NULL COMMENT '签收时间',
    order_created_at DATETIME NOT NULL COMMENT '订单创建时间',
    order_updated_at DATETIME NOT NULL COMMENT '订单更新时间',
    read_model_updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '读模型更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单查询宽表';
```

该结构中，`trade_order_read_model` 是典型宽表。它聚合了订单、用户、店铺、支付、物流等多张表中的字段，专门服务后台列表、筛选、导出和报表明细查询。

宽表通常不作为写入事实表。订单状态、支付状态、物流状态的权威来源仍然在原始业务表中。宽表只保存读模型数据，可以通过同步事件、定时任务、补偿任务或重建任务维护。

### 字段设计

宽表字段设计的核心是面向查询结果组织字段，而不是完全照搬所有来源表字段。字段应按业务主题、查询条件、展示字段、排序字段和同步字段进行归类。

建议字段设计如下：

| 字段类型   | 命名示例                                             | 设计说明                                               |
| ---------- | ---------------------------------------------------- | ------------------------------------------------------ |
| 读模型主键 | `id`                                                 | 宽表自身主键，可以使用雪花 ID 或与主业务表 ID 保持一致 |
| 来源主键   | `order_id`、`user_id`、`shop_id`                     | 保存来源表主键，便于追溯和修复                         |
| 来源编号   | `order_no`、`user_no`、`shop_no`                     | 支持业务查询、展示和外部对账                           |
| 用户字段   | `user_nickname`、`user_mobile`、`user_level`         | 面向订单列表展示和筛选                                 |
| 店铺字段   | `shop_name`、`shop_type`                             | 面向商家、运营后台查询                                 |
| 订单字段   | `order_status`、`total_amount`、`pay_amount`         | 订单核心查询字段                                       |
| 支付字段   | `payment_no`、`pay_channel`、`pay_status`、`paid_at` | 支付查询和对账字段                                     |
| 物流字段   | `delivery_no`、`delivery_company`、`delivery_status` | 物流查询字段                                           |
| 时间字段   | `order_created_at`、`paid_at`、`delivered_at`        | 用于范围查询、排序、导出                               |
| 同步字段   | `read_model_updated_at`                              | 标识读模型最后更新时间                                 |
| 删除字段   | `deleted`                                            | 与主业务表保持逻辑删除状态                             |

字段设计时需要注意以下原则：

第一，宽表字段应服务明确的查询场景。不要因为某个字段存在于来源表，就默认同步到宽表。宽表字段越多，写入同步成本越高，后续变更越复杂。

第二，宽表中应保留来源主键和业务编号。来源主键用于内部追溯和数据修复，业务编号用于运营、客服、财务、外部系统查询。

第三，常用筛选字段应使用原始编码，而不是只保存展示文案。例如 `order_status`、`pay_status`、`shop_type`、`user_level` 应保存数字或稳定编码，页面展示文案可以由应用层或字典表转换。

第四，时间字段要区分业务时间和同步时间。`order_created_at` 表示订单创建时间，`paid_at` 表示支付时间，`read_model_updated_at` 表示宽表更新时间，三者语义不同，不应混用。

第五，宽表不宜无限扩张。如果一个宽表同时承载订单列表、财务对账、物流监控、用户画像、商品分析等完全不同场景，字段会快速膨胀。此时应拆分为多个面向场景的读模型表。

### 索引设计

宽表的索引设计应直接服务查询入口、筛选组合、排序字段和导出任务。由于宽表字段较多，不能为每个字段都建立索引，否则会严重影响同步写入性能。

订单查询宽表常见索引如下：

```sql
ALTER TABLE trade_order_read_model
    ADD UNIQUE KEY uk_order_no (order_no),
    ADD KEY idx_order_id (order_id),
    ADD KEY idx_user_created (user_id, order_created_at),
    ADD KEY idx_shop_status_created (shop_id, order_status, order_created_at),
    ADD KEY idx_order_status_created (order_status, order_created_at),
    ADD KEY idx_pay_status_paid (pay_status, paid_at),
    ADD KEY idx_pay_channel_paid (pay_channel, paid_at),
    ADD KEY idx_delivery_status_created (delivery_status, order_created_at),
    ADD KEY idx_read_model_updated (read_model_updated_at);
```

索引设计说明如下：

| 索引                          | 适用查询               | 说明                             |
| ----------------------------- | ---------------------- | -------------------------------- |
| `uk_order_no`                 | 根据订单号查询         | 保证一个订单只有一条读模型记录   |
| `idx_order_id`                | 根据订单 ID 定位读模型 | 适合同步更新和修复               |
| `idx_user_created`            | 用户订单列表           | 支持用户维度按时间分页           |
| `idx_shop_status_created`     | 店铺订单列表           | 支持商家后台按状态筛选           |
| `idx_order_status_created`    | 运营订单列表           | 支持运营后台按订单状态和时间查询 |
| `idx_pay_status_paid`         | 支付状态查询           | 支持财务或支付后台按支付时间查询 |
| `idx_pay_channel_paid`        | 支付渠道对账           | 支持渠道维度对账查询             |
| `idx_delivery_status_created` | 物流状态查询           | 支持发货、签收等物流筛选         |
| `idx_read_model_updated`      | 增量同步或导出         | 支持按读模型更新时间扫描         |

宽表索引设计应优先考虑最常用的 3 到 5 类查询路径。对于低频查询、模糊查询、任意组合查询，不建议在 MySQL 宽表上堆叠大量组合索引。若查询条件高度自由，应考虑搜索辅助表、全文索引或 Elasticsearch。

对于后台列表，应尽量要求带时间范围。持续增长的宽表如果允许无时间范围查询，会导致大范围扫描，即使存在部分索引也容易出现性能问题。

### 常用查询

常用查询应体现宽表的核心价值：直接从读模型表返回完整列表字段，避免每次查询都关联订单、用户、店铺、支付、物流等多张表。

#### 根据订单号查询读模型详情

订单号通常是客服、运营和外部系统最常用的查询入口。宽表中保存了订单、用户、店铺、支付和物流字段，可以直接返回完整信息。

```sql
SELECT
    order_id,
    order_no,
    user_id,
    user_no,
    user_nickname,
    user_mobile,
    shop_id,
    shop_no,
    shop_name,
    order_status,
    total_amount,
    pay_amount,
    payment_no,
    pay_channel,
    pay_status,
    paid_at,
    delivery_no,
    delivery_company,
    delivery_status,
    delivered_at,
    received_at,
    order_created_at
FROM trade_order_read_model
WHERE order_no = 'O202605130001'
  AND deleted = 0;
```

该查询适合客服查询、运营后台详情入口、外部系统回查等场景。若页面需要订单明细、操作日志、售后记录等额外信息，可以再按订单 ID 查询对应子表。

#### 查询用户订单列表

用户订单列表可以直接从宽表读取订单和店铺展示字段，不需要再关联店铺表、支付表或物流表。

```sql
SELECT
    order_id,
    order_no,
    shop_no,
    shop_name,
    order_status,
    total_amount,
    pay_amount,
    pay_channel,
    pay_status,
    paid_at,
    delivery_status,
    order_created_at
FROM trade_order_read_model
WHERE user_id = 10001
  AND deleted = 0
ORDER BY order_created_at DESC
LIMIT 20;
```

该查询适合用户中心订单列表、会员后台订单查询等场景。对于数据量较大的用户，可以使用游标分页。

```sql
SELECT
    order_id,
    order_no,
    shop_no,
    shop_name,
    order_status,
    total_amount,
    pay_amount,
    order_created_at
FROM trade_order_read_model
WHERE user_id = 10001
  AND deleted = 0
  AND (
      order_created_at < '2026-05-13 10:00:00'
      OR (order_created_at = '2026-05-13 10:00:00' AND order_id < 90001)
  )
ORDER BY order_created_at DESC, order_id DESC
LIMIT 20;
```

游标分页适合订单量较大的场景，可以避免深分页带来的性能问题。

#### 查询店铺订单列表

商家后台通常按店铺、订单状态和下单时间查询。宽表可以直接返回用户昵称、手机号、支付状态、物流状态等字段。

```sql
SELECT
    order_id,
    order_no,
    user_no,
    user_nickname,
    user_mobile,
    order_status,
    total_amount,
    pay_amount,
    pay_status,
    paid_at,
    delivery_status,
    delivery_no,
    order_created_at
FROM trade_order_read_model
WHERE shop_id = 20001
  AND order_status = 20
  AND order_created_at >= '2026-05-01 00:00:00'
  AND order_created_at < '2026-06-01 00:00:00'
  AND deleted = 0
ORDER BY order_created_at DESC
LIMIT 50;
```

该查询适合商家后台订单管理。由于列表字段已经在宽表中准备好，查询复杂度明显低于多表 JOIN。

#### 查询运营后台订单列表

运营后台通常需要按订单状态、支付状态、支付渠道、店铺类型、用户等级等条件筛选。宽表可以承载这些跨表字段，减少动态 JOIN。

```sql
SELECT
    order_id,
    order_no,
    user_no,
    user_nickname,
    shop_no,
    shop_name,
    shop_type,
    user_level,
    order_status,
    pay_status,
    pay_channel,
    total_amount,
    pay_amount,
    paid_at,
    order_created_at
FROM trade_order_read_model
WHERE order_status = 20
  AND pay_status = 20
  AND pay_channel = 'WECHAT'
  AND order_created_at >= '2026-05-01 00:00:00'
  AND order_created_at < '2026-06-01 00:00:00'
  AND deleted = 0
ORDER BY order_created_at DESC
LIMIT 100;
```

运营后台查询条件较多时，应限制可组合条件，并根据最高频路径设计联合索引。不要为所有可能组合建立索引。

#### 查询支付对账列表

支付对账通常按支付渠道、支付状态和支付时间范围查询。宽表中已经保存支付字段，可以直接作为对账明细基础数据。

```sql
SELECT
    order_no,
    payment_no,
    user_no,
    shop_no,
    shop_name,
    pay_channel,
    pay_status,
    pay_amount,
    paid_at
FROM trade_order_read_model
WHERE pay_channel = 'ALIPAY'
  AND pay_status = 20
  AND paid_at >= '2026-05-01 00:00:00'
  AND paid_at < '2026-05-02 00:00:00'
  AND deleted = 0
ORDER BY paid_at ASC
LIMIT 1000;
```

该查询适合支付渠道日对账。若导出数据量很大，应使用分批查询或异步导出任务，不建议一次性导出全量数据。

#### 查询待发货订单

物流或仓储系统通常需要查询已支付但未发货订单。宽表中保存订单状态、支付状态和物流状态，可以快速定位待处理记录。

```sql
SELECT
    order_id,
    order_no,
    shop_id,
    shop_no,
    shop_name,
    user_no,
    user_nickname,
    user_mobile,
    pay_status,
    paid_at,
    delivery_status,
    order_created_at
FROM trade_order_read_model
WHERE order_status = 20
  AND pay_status = 20
  AND delivery_status = 10
  AND deleted = 0
ORDER BY paid_at ASC
LIMIT 100;
```

该查询适合仓储发货队列、商家待发货列表和物流监控任务。实际业务中通常还需要结合订单明细和收货地址表。

#### 按读模型更新时间增量导出

宽表可以通过 `read_model_updated_at` 支持增量导出或同步到搜索引擎、数据仓库、缓存系统。

```sql
SELECT
    order_id,
    order_no,
    user_no,
    user_nickname,
    shop_no,
    shop_name,
    order_status,
    pay_status,
    delivery_status,
    pay_amount,
    order_created_at,
    read_model_updated_at
FROM trade_order_read_model
WHERE read_model_updated_at >= '2026-05-13 00:00:00'
  AND read_model_updated_at < '2026-05-14 00:00:00'
ORDER BY read_model_updated_at ASC
LIMIT 1000;
```

该查询适合增量同步任务。为了避免相同时间点数据遗漏，实际同步时可以使用 `read_model_updated_at + order_id` 作为游标。

### 常用写入

宽表写入通常不是用户直接写入，而是由业务事件、同步任务或重建任务驱动。常见方式包括同步写入、异步事件更新、定时补偿更新和全量重建。

#### 创建订单时初始化宽表

订单创建后，可以立即初始化宽表记录。此时支付和物流信息可能还不存在，可以先写入订单、用户、店铺相关字段，支付字段和物流字段后续再更新。

```sql
INSERT INTO trade_order_read_model (
    id,
    order_id,
    order_no,
    user_id,
    user_no,
    user_nickname,
    user_mobile,
    user_level,
    shop_id,
    shop_no,
    shop_name,
    shop_type,
    order_status,
    total_amount,
    pay_amount,
    order_created_at,
    order_updated_at,
    read_model_updated_at,
    deleted
)
SELECT
    90001,
    o.id,
    o.order_no,
    u.id,
    u.user_no,
    u.nickname,
    u.mobile,
    u.user_level,
    s.id,
    s.shop_no,
    s.shop_name,
    s.shop_type,
    o.order_status,
    o.total_amount,
    o.pay_amount,
    o.created_at,
    o.updated_at,
    NOW(),
    o.deleted
FROM trade_order o
JOIN biz_user u ON u.id = o.user_id
JOIN biz_shop s ON s.id = o.shop_id
WHERE o.id = 90001;
```

该写入适合订单创建成功后立即生成读模型。若宽表同步失败，应通过补偿任务重新生成，避免订单存在但列表不可见。

#### 支付成功后更新宽表

支付成功后，支付单表是权威来源，宽表需要同步支付状态、支付渠道、支付单号和支付时间。

```sql
UPDATE trade_order_read_model rm
JOIN trade_payment p ON p.order_id = rm.order_id
SET rm.payment_no = p.payment_no,
    rm.pay_channel = p.pay_channel,
    rm.pay_status = p.pay_status,
    rm.paid_at = p.paid_at,
    rm.order_status = 20,
    rm.read_model_updated_at = NOW()
WHERE rm.order_id = 90001
  AND p.pay_status = 20;
```

该写法适合支付成功事件触发后的读模型更新。实际业务中，订单主表状态也应在同一个业务流程中更新，宽表只用于查询展示。

#### 发货后更新宽表

发货后，物流表是物流信息的权威来源，宽表需要同步物流单号、物流公司、物流状态和发货时间。

```sql
UPDATE trade_order_read_model rm
JOIN trade_delivery d ON d.order_id = rm.order_id
SET rm.delivery_no = d.delivery_no,
    rm.delivery_company = d.delivery_company,
    rm.delivery_status = d.delivery_status,
    rm.delivered_at = d.delivered_at,
    rm.order_status = 30,
    rm.read_model_updated_at = NOW()
WHERE rm.order_id = 90001
  AND d.delivery_status = 20;
```

该写法适合发货事件触发后的同步。若物流信息后续变化，例如签收、拒收、退回，也应继续更新宽表中的物流状态。

#### 用户信息变更后更新宽表

如果宽表中的用户昵称、手机号、用户等级要求展示最新值，则用户信息变更后需要同步更新相关宽表数据。

```sql
UPDATE trade_order_read_model rm
JOIN biz_user u ON u.id = rm.user_id
SET rm.user_no = u.user_no,
    rm.user_nickname = u.nickname,
    rm.user_mobile = u.mobile,
    rm.user_level = u.user_level,
    rm.read_model_updated_at = NOW()
WHERE rm.user_id = 10001
  AND rm.deleted = 0;
```

该同步是否执行取决于业务语义。如果订单列表要求展示下单时用户信息，则不应回刷；如果要求展示用户当前最新资料，则需要同步。

#### 重建单条宽表记录

当宽表数据缺失或不一致时，可以基于来源表重建单条读模型。建议使用 `INSERT ... ON DUPLICATE KEY UPDATE` 保证可重复执行。

```sql
INSERT INTO trade_order_read_model (
    id,
    order_id,
    order_no,
    user_id,
    user_no,
    user_nickname,
    user_mobile,
    user_level,
    shop_id,
    shop_no,
    shop_name,
    shop_type,
    order_status,
    total_amount,
    pay_amount,
    payment_no,
    pay_channel,
    pay_status,
    paid_at,
    delivery_no,
    delivery_company,
    delivery_status,
    delivered_at,
    received_at,
    order_created_at,
    order_updated_at,
    read_model_updated_at,
    deleted
)
SELECT
    o.id,
    o.id,
    o.order_no,
    u.id,
    u.user_no,
    u.nickname,
    u.mobile,
    u.user_level,
    s.id,
    s.shop_no,
    s.shop_name,
    s.shop_type,
    o.order_status,
    o.total_amount,
    o.pay_amount,
    p.payment_no,
    p.pay_channel,
    p.pay_status,
    p.paid_at,
    d.delivery_no,
    d.delivery_company,
    d.delivery_status,
    d.delivered_at,
    d.received_at,
    o.created_at,
    o.updated_at,
    NOW(),
    o.deleted
FROM trade_order o
JOIN biz_user u ON u.id = o.user_id
JOIN biz_shop s ON s.id = o.shop_id
LEFT JOIN trade_payment p ON p.order_id = o.id
LEFT JOIN trade_delivery d ON d.order_id = o.id
WHERE o.id = 90001
ON DUPLICATE KEY UPDATE
    user_no = VALUES(user_no),
    user_nickname = VALUES(user_nickname),
    user_mobile = VALUES(user_mobile),
    user_level = VALUES(user_level),
    shop_no = VALUES(shop_no),
    shop_name = VALUES(shop_name),
    shop_type = VALUES(shop_type),
    order_status = VALUES(order_status),
    total_amount = VALUES(total_amount),
    pay_amount = VALUES(pay_amount),
    payment_no = VALUES(payment_no),
    pay_channel = VALUES(pay_channel),
    pay_status = VALUES(pay_status),
    paid_at = VALUES(paid_at),
    delivery_no = VALUES(delivery_no),
    delivery_company = VALUES(delivery_company),
    delivery_status = VALUES(delivery_status),
    delivered_at = VALUES(delivered_at),
    received_at = VALUES(received_at),
    order_updated_at = VALUES(order_updated_at),
    read_model_updated_at = NOW(),
    deleted = VALUES(deleted);
```

该写法适合补偿任务、数据修复任务和单条读模型重建。前提是 `order_no` 或 `order_id` 上存在唯一约束。

#### 批量重建宽表数据

当宽表结构调整、字段新增、历史数据缺失时，可以按主键范围批量重建。大表场景不建议一次性全量重建，应分批执行。

```sql
INSERT INTO trade_order_read_model (
    id,
    order_id,
    order_no,
    user_id,
    user_no,
    user_nickname,
    user_mobile,
    user_level,
    shop_id,
    shop_no,
    shop_name,
    shop_type,
    order_status,
    total_amount,
    pay_amount,
    payment_no,
    pay_channel,
    pay_status,
    paid_at,
    delivery_no,
    delivery_company,
    delivery_status,
    delivered_at,
    received_at,
    order_created_at,
    order_updated_at,
    read_model_updated_at,
    deleted
)
SELECT
    o.id,
    o.id,
    o.order_no,
    u.id,
    u.user_no,
    u.nickname,
    u.mobile,
    u.user_level,
    s.id,
    s.shop_no,
    s.shop_name,
    s.shop_type,
    o.order_status,
    o.total_amount,
    o.pay_amount,
    p.payment_no,
    p.pay_channel,
    p.pay_status,
    p.paid_at,
    d.delivery_no,
    d.delivery_company,
    d.delivery_status,
    d.delivered_at,
    d.received_at,
    o.created_at,
    o.updated_at,
    NOW(),
    o.deleted
FROM trade_order o
JOIN biz_user u ON u.id = o.user_id
JOIN biz_shop s ON s.id = o.shop_id
LEFT JOIN trade_payment p ON p.order_id = o.id
LEFT JOIN trade_delivery d ON d.order_id = o.id
WHERE o.id BETWEEN 1 AND 100000
ON DUPLICATE KEY UPDATE
    order_status = VALUES(order_status),
    total_amount = VALUES(total_amount),
    pay_amount = VALUES(pay_amount),
    pay_status = VALUES(pay_status),
    paid_at = VALUES(paid_at),
    delivery_status = VALUES(delivery_status),
    delivered_at = VALUES(delivered_at),
    received_at = VALUES(received_at),
    order_updated_at = VALUES(order_updated_at),
    read_model_updated_at = NOW(),
    deleted = VALUES(deleted);
```

批量重建应按主键范围、创建时间范围或分片任务拆分执行。执行前应评估锁等待、binlog 体积、主从延迟和业务低峰时间窗口。

### 常见问题

宽表模型最常见的问题是把宽表当成核心业务表使用。宽表应该是读模型，不应该承载核心交易事实，也不应该成为金额、状态、库存等关键业务判断的唯一依据。

第一个常见问题是数据一致性延迟。宽表通常由异步事件或定时任务维护，可能短时间落后于来源表。解决方式是明确最终一致性边界，对关键详情页可回源查询，对列表页接受短暂延迟，并提供补偿任务修复异常数据。

第二个常见问题是字段无限膨胀。一个宽表如果不断承载新页面、新导出、新报表需求，字段会越来越多，写入同步越来越慢。解决方式是按业务场景拆分多个读模型，例如订单查询宽表、支付对账宽表、物流监控宽表、财务结算宽表。

第三个常见问题是索引过多。宽表字段多，但并不是每个字段都适合建索引。过多索引会降低写入和同步性能，也会增加存储成本。索引应围绕高频查询路径设计，低频组合查询可以通过异步导出或搜索系统处理。

第四个常见问题是同步链路不可追踪。宽表数据来自多个表，如果没有同步时间、事件记录、失败重试和修复工具，出现数据不一致时很难排查。建议保留 `read_model_updated_at` 字段，并在应用层记录同步事件和异常日志。

第五个常见问题是历史语义不清晰。宽表中的用户昵称、店铺名称、商品名称到底展示当前值还是业务发生时的值，需要提前定义。如果要求历史稳定，应使用快照字段；如果要求展示最新值，则需要在来源数据变更后同步宽表。

第六个常见问题是过度依赖实时 JOIN 重建。宽表的价值在于提前准备读模型。如果每次查询都临时 JOIN 多张表生成结果，就不是宽表模型，而是普通关联查询。宽表应通过写入链路或异步同步提前生成。

### 总结

宽表模型是一种典型的读优化建模方式，适合后台复杂列表、多条件查询、大批量导出、对账明细和搜索读模型等场景。它通过将多张来源表中的常用字段提前汇总到一张读模型表中，减少查询时的 JOIN 成本，提高读性能和查询稳定性。

设计宽表时，应明确它是读模型而不是事实表。来源主表仍然保存权威业务数据，宽表负责查询展示、筛选排序和导出同步。字段设计要围绕查询场景控制范围，索引设计要围绕高频查询路径控制数量，写入设计要支持初始化、增量更新、补偿修复和批量重建。

宽表模型与冗余字段模型、快照模型的区别在于：冗余字段通常是在单个业务表中少量保存关联字段；快照模型强调业务发生时数据固化；宽表模型则面向复杂读场景，将多个来源表字段系统性汇总为独立读模型。实际建模时，应根据查询复杂度、一致性要求和维护成本选择合适方案。





## JSON扩展字段模型

JSON扩展字段模型是指在 MySQL 8 中使用 `JSON` 类型保存结构相对灵活、变化频率较高、非核心查询条件的扩展属性。它适合在不频繁变更表结构的前提下，承载业务扩展字段、页面配置、渠道参数、外部系统原始数据、规则配置和低频补充信息。

JSON扩展字段不是替代关系型建模的万能方案。核心业务字段、高频查询字段、强约束字段、金额字段、状态字段和关联字段仍然应优先使用普通列建模。JSON字段更适合承载“扩展信息”，而不是承载“核心事实”。

### 适用场景

JSON扩展字段适合字段结构变化较快、不同业务类型字段差异较大、字段不适合频繁加列、查询频率不高、主要用于详情展示或配置保存的场景。

典型场景包括商品扩展参数、用户扩展资料、订单渠道扩展信息、支付回调原始参数、审批表单扩展字段、业务规则配置、第三方接口原始响应、设备属性、营销活动配置等。

JSON扩展字段通常用于以下几类需求：

| 场景       | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 扩展属性   | 不同业务类型拥有不同字段，例如商品参数、设备属性、用户资料补充项 |
| 外部数据   | 保存第三方接口回调、渠道参数、原始响应，便于排查问题         |
| 页面配置   | 保存表单配置、展示配置、规则配置等半结构化数据               |
| 低频查询   | 字段主要用于详情展示，偶尔按少量关键字段筛选                 |
| 快速迭代   | 字段变化频繁，不希望每次需求都修改表结构                     |
| 多类型差异 | 同一张表中不同类型记录拥有不同扩展字段集合                   |

不适合使用 JSON 扩展字段的场景包括高频筛选字段、核心状态字段、金额计算字段、强唯一约束字段、强关联字段、必须参与事务一致性判断的字段。例如订单状态、支付金额、用户手机号、库存数量、账户余额等字段不应只放在 JSON 中。

### 建模结构

JSON扩展字段模型通常是在核心业务表中保留稳定字段，将变化较快或差异较大的字段保存到 `JSON` 类型字段中。稳定字段用于查询、约束、排序和业务判断，JSON字段用于保存扩展信息。

下面以商品表为例。商品基础字段使用普通列建模，商品参数、售后配置、渠道扩展信息等保存到 JSON 字段中。

```sql
CREATE TABLE product_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    product_no VARCHAR(32) NOT NULL COMMENT '商品编号',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    brand_id BIGINT DEFAULT NULL COMMENT '品牌ID',
    product_type TINYINT NOT NULL COMMENT '商品类型：1实物商品，2虚拟商品，3服务商品',
    sale_status TINYINT NOT NULL DEFAULT 10 COMMENT '销售状态：10草稿，20上架，30下架',
    sale_price DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '销售价格',
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    extend_attrs JSON DEFAULT NULL COMMENT '扩展属性JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息表';
```

`extend_attrs` 可以保存不同商品类型的差异化属性。例如实物商品可以保存材质、重量、保质期；虚拟商品可以保存发货方式、有效期；服务商品可以保存预约规则、服务时长和可用城市。

```json
{
  "material": "棉",
  "weight": "500g",
  "shelfLifeDays": 365,
  "afterSale": {
    "supportRefund": true,
    "refundDays": 7
  },
  "channel": {
    "source": "JD",
    "externalProductNo": "JD10000001"
  }
}
```

如果 JSON 中某些字段逐渐变成高频查询条件，应将其提升为普通列，或使用生成列配合索引。JSON字段适合扩展，不适合作为长期高频查询字段的主要承载方式。

也可以将 JSON 扩展字段放在独立扩展表中，用于避免主表过宽或减少主表更新压力。

```sql
CREATE TABLE product_extend_attr (
    id BIGINT NOT NULL COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_no VARCHAR(32) NOT NULL COMMENT '商品编号',
    extend_attrs JSON NOT NULL COMMENT '扩展属性JSON',
    schema_version INT NOT NULL DEFAULT 1 COMMENT '结构版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品扩展属性表';
```

主表 JSON 字段适合扩展内容较小、详情页经常一起读取的场景。独立扩展表适合扩展内容较大、更新频率较高、主表查询不一定需要读取扩展内容的场景。

### 字段设计

JSON扩展字段设计的重点是控制边界。应明确哪些字段属于关系型核心字段，哪些字段属于扩展字段，避免把整个业务对象都塞进 JSON。

建议字段设计如下：

| 字段类型 | 命名示例                                    | 设计说明                           |
| -------- | ------------------------------------------- | ---------------------------------- |
| 主键字段 | `id`                                        | 表记录唯一标识                     |
| 业务编号 | `product_no`、`form_no`                     | 稳定业务编号，适合普通列           |
| 核心分类 | `product_type`、`category_id`               | 常用筛选字段，适合普通列           |
| 核心状态 | `sale_status`、`audit_status`               | 高频业务判断字段，适合普通列       |
| 金额数量 | `sale_price`、`stock_quantity`              | 参与计算和强校验，适合普通列       |
| 扩展JSON | `extend_attrs`、`extra_info`、`config_json` | 保存半结构化扩展字段               |
| 结构版本 | `schema_version`                            | 标识 JSON 结构版本，便于升级和兼容 |
| 更新时间 | `updated_at`                                | 标识记录更新时间                   |

JSON字段命名建议使用明确语义，例如 `extend_attrs`、`extra_info`、`config_json`、`request_payload`、`response_payload`。不要使用过于宽泛的字段名，例如 `data`、`json`、`content`，否则后续很难判断字段职责。

JSON内容设计时需要注意以下原则：

第一，核心字段不要放入 JSON。只要字段需要高频查询、排序、分组、唯一约束、外键关联或强业务判断，就应优先使用普通列。

第二，JSON结构应保持相对稳定。即使 JSON 允许灵活结构，也应在应用层维护字段清单、数据类型和版本说明，避免不同记录随意写入完全不同结构。

第三，JSON字段应控制大小。过大的 JSON 会增加行数据读取成本，也会影响更新性能。富文本、大附件、大原始报文不建议直接长期保存在主业务表 JSON 字段中。

第四，JSON字段中的金额、时间、布尔值、数字应使用正确类型，不要全部保存为字符串。类型不稳定会影响 JSON 查询和应用层解析。

第五，建议增加 `schema_version`。当 JSON 结构发生变化时，可以根据版本进行兼容解析、迁移和修复。

### 索引设计

JSON字段本身不适合直接作为整体索引。MySQL 8 中常见做法是将 JSON 中少量需要查询的字段提取为生成列，然后对生成列建立索引。索引设计仍然应围绕高频查询路径展开。

如果商品扩展属性中的外部商品编号需要经常查询，可以增加生成列并建立索引。

```sql
ALTER TABLE product_info
    ADD COLUMN external_product_no VARCHAR(64)
        GENERATED ALWAYS AS (
            JSON_UNQUOTE(JSON_EXTRACT(extend_attrs, '$.channel.externalProductNo'))
        ) STORED COMMENT '外部商品编号生成列',
    ADD KEY idx_external_product_no (external_product_no);
```

如果需要按是否支持售后退款筛选，可以增加布尔类生成列。

```sql
ALTER TABLE product_info
    ADD COLUMN support_refund TINYINT
        GENERATED ALWAYS AS (
            CAST(JSON_EXTRACT(extend_attrs, '$.afterSale.supportRefund') AS UNSIGNED)
        ) STORED COMMENT '是否支持退款生成列',
    ADD KEY idx_support_refund_status (support_refund, sale_status);
```

如果需要按保质期天数查询，可以增加数值生成列。

```sql
ALTER TABLE product_info
    ADD COLUMN shelf_life_days INT
        GENERATED ALWAYS AS (
            CAST(JSON_UNQUOTE(JSON_EXTRACT(extend_attrs, '$.shelfLifeDays')) AS UNSIGNED)
        ) STORED COMMENT '保质期天数生成列',
    ADD KEY idx_shelf_life_days (shelf_life_days);
```

索引设计说明如下：

| 索引方式   | 适用场景                       | 说明                                           |
| ---------- | ------------------------------ | ---------------------------------------------- |
| 普通列索引 | 核心字段查询                   | 商品编号、状态、分类、价格等字段仍用普通列索引 |
| 生成列索引 | JSON内少数字段高频查询         | 将 JSON 路径提取为生成列后建索引               |
| 联合索引   | JSON提取字段和普通字段组合查询 | 例如 `support_refund + sale_status`            |
| 不建索引   | 详情展示、低频读取             | 直接读取 JSON 内容即可                         |
| 搜索系统   | 多字段模糊搜索、复杂筛选       | 不建议完全依赖 MySQL JSON 查询                 |

不建议为大量 JSON 路径都创建生成列索引。如果 JSON 中越来越多字段都需要查询，说明该数据已经不再只是扩展字段，应考虑改为普通列、扩展属性表或搜索辅助表。

### 常用查询

常用查询应区分“读取完整 JSON”、“读取 JSON 局部字段”、“按 JSON 字段筛选”和“展开 JSON 数组”。JSON查询适合低频或中频场景，高频查询应尽量使用普通列或生成列索引。

### 查询完整扩展属性

详情页通常需要读取完整扩展属性，由应用层解析并展示。此类查询不需要对 JSON 字段做复杂处理。

```sql
SELECT
    id,
    product_no,
    product_name,
    product_type,
    sale_status,
    sale_price,
    stock_quantity,
    extend_attrs,
    created_at
FROM product_info
WHERE product_no = 'P202605130001'
  AND deleted = 0;
```

该查询适合商品详情页、配置详情页、外部接口回放等场景。应用层应根据 `product_type` 或 `schema_version` 解析 JSON 内容。

#### 查询 JSON 中的单个字段

如果只需要展示 JSON 中的某个字段，可以使用 `JSON_EXTRACT` 和 `JSON_UNQUOTE` 提取。

```sql
SELECT
    id,
    product_no,
    product_name,
    JSON_UNQUOTE(JSON_EXTRACT(extend_attrs, '$.material')) AS material,
    JSON_UNQUOTE(JSON_EXTRACT(extend_attrs, '$.weight')) AS weight,
    JSON_EXTRACT(extend_attrs, '$.shelfLifeDays') AS shelf_life_days
FROM product_info
WHERE product_no = 'P202605130001'
  AND deleted = 0;
```

该查询适合后台排查或少量字段展示。业务列表如果长期需要展示这些字段，可以考虑将其转为普通列或生成列。

#### 按 JSON 字段筛选

可以直接按 JSON 路径筛选，但该方式通常难以高效利用普通索引，适合低频查询或小数据量场景。

```sql
SELECT
    id,
    product_no,
    product_name,
    sale_status,
    extend_attrs
FROM product_info
WHERE JSON_UNQUOTE(JSON_EXTRACT(extend_attrs, '$.channel.source')) = 'JD'
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

如果该查询成为高频查询，应将 `$.channel.source` 提取为生成列并建立索引。

#### 使用生成列查询 JSON 字段

对于高频查询，应优先使用生成列查询，避免每次查询都在条件中解析 JSON。

```sql
SELECT
    id,
    product_no,
    product_name,
    sale_status,
    external_product_no
FROM product_info
WHERE external_product_no = 'JD10000001'
  AND deleted = 0;
```

该查询可以命中 `idx_external_product_no`，比直接在 `WHERE` 中写 `JSON_EXTRACT` 更适合高频业务查询。

#### 判断 JSON 字段是否存在

可以使用 `JSON_CONTAINS_PATH` 判断某个路径是否存在，适合数据排查、结构迁移和兼容处理。

```sql
SELECT
    id,
    product_no,
    product_name,
    extend_attrs
FROM product_info
WHERE JSON_CONTAINS_PATH(extend_attrs, 'one', '$.afterSale.refundDays')
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 50;
```

该查询适合检查某批数据是否已经写入指定扩展字段。大表中使用时建议增加普通列条件，例如分类、类型或时间范围。

#### 查询 JSON 数组内容

如果 JSON 中包含数组，可以使用 `JSON_CONTAINS` 判断数组中是否包含指定值。

```sql
SELECT
    id,
    product_no,
    product_name,
    extend_attrs
FROM product_info
WHERE JSON_CONTAINS(
        JSON_EXTRACT(extend_attrs, '$.serviceCities'),
        JSON_QUOTE('杭州')
    )
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 20;
```

该查询适合低频筛选。若数组内容需要高频查询，例如服务城市、商品标签、适用门店，应考虑拆成关系表，而不是长期依赖 JSON 数组查询。

#### 展开 JSON 数组查询

MySQL 8 可以使用 `JSON_TABLE` 将 JSON 数组展开成行，适合临时分析、数据迁移或报表处理。

```sql
SELECT
    p.id,
    p.product_no,
    p.product_name,
    city.city_name
FROM product_info p
JOIN JSON_TABLE(
    p.extend_attrs,
    '$.serviceCities[*]'
    COLUMNS (
        city_name VARCHAR(64) PATH '$'
    )
) AS city
WHERE p.product_type = 3
  AND p.deleted = 0;
```

该查询适合分析 JSON 数组内容，但不建议作为高频在线接口的主要查询方式。高频数组查询应拆分为子表，例如 `product_service_city`。

### 常用写入

JSON扩展字段写入包括插入完整 JSON、局部更新 JSON、删除 JSON 路径、合并 JSON 内容和迁移 JSON 结构。写入时应保证 JSON 结构合法，并尽量控制更新范围。

#### 新增记录时写入 JSON 扩展字段

新增商品时，可以将不同类型商品的扩展属性写入 `extend_attrs`。

```sql
INSERT INTO product_info (
    id,
    product_no,
    product_name,
    category_id,
    brand_id,
    product_type,
    sale_status,
    sale_price,
    stock_quantity,
    extend_attrs,
    created_at,
    updated_at,
    deleted
)
VALUES (
    10001,
    'P202605130001',
    '基础款T恤',
    20001,
    30001,
    1,
    20,
    99.00,
    1000,
    JSON_OBJECT(
        'material', '棉',
        'weight', '500g',
        'shelfLifeDays', 365,
        'afterSale', JSON_OBJECT(
            'supportRefund', true,
            'refundDays', 7
        ),
        'channel', JSON_OBJECT(
            'source', 'JD',
            'externalProductNo', 'JD10000001'
        )
    ),
    NOW(),
    NOW(),
    0
);
```

使用 `JSON_OBJECT` 可以减少手写 JSON 字符串带来的格式错误。应用层写入时也应进行字段类型校验，避免同一路径在不同记录中出现不同数据类型。

#### 局部更新 JSON 字段

当只需要修改 JSON 中某个路径时，可以使用 `JSON_SET`，避免应用层读取完整 JSON 后再整体覆盖。

```sql
UPDATE product_info
SET extend_attrs = JSON_SET(
        extend_attrs,
        '$.afterSale.refundDays',
        15,
        '$.afterSale.supportRefund',
        true
    ),
    updated_at = NOW()
WHERE product_no = 'P202605130001'
  AND deleted = 0;
```

`JSON_SET` 适合新增或更新指定路径。若路径不存在，它会创建对应路径；若路径存在，它会覆盖原值。

#### 删除 JSON 中的字段

如果某个扩展字段废弃，可以使用 `JSON_REMOVE` 删除指定路径。

```sql
UPDATE product_info
SET extend_attrs = JSON_REMOVE(
        extend_attrs,
        '$.weight'
    ),
    updated_at = NOW()
WHERE product_no = 'P202605130001'
  AND deleted = 0;
```

删除字段前应确认应用层已经不再依赖该路径。对于历史数据，建议结合 `schema_version` 做兼容处理，而不是直接假设所有记录都有相同字段。

#### 合并 JSON 扩展内容

当需要追加一组扩展字段时，可以使用 `JSON_MERGE_PATCH` 合并对象。后面的字段会覆盖前面同名字段。

```sql
UPDATE product_info
SET extend_attrs = JSON_MERGE_PATCH(
        extend_attrs,
        JSON_OBJECT(
            'promotion', JSON_OBJECT(
                'tag', '新品',
                'priority', 10
            )
        )
    ),
    updated_at = NOW()
WHERE product_no = 'P202605130001'
  AND deleted = 0;
```

该方式适合追加配置、营销信息、渠道信息等对象结构。对于数组追加，需要根据具体结构使用 `JSON_ARRAY_APPEND`。

#### 向 JSON 数组追加元素

如果扩展字段中维护数组，可以使用 `JSON_ARRAY_APPEND` 追加元素。

```sql
UPDATE product_info
SET extend_attrs = JSON_ARRAY_APPEND(
        extend_attrs,
        '$.serviceCities',
        '杭州'
    ),
    updated_at = NOW()
WHERE product_no = 'P202605130001'
  AND deleted = 0;
```

数组字段如果需要去重、排序、频繁查询，建议不要长期放在 JSON 中维护，应拆成子表来表达一对多关系。

#### 批量初始化 JSON 字段

历史表增加 JSON 扩展字段后，可以按业务类型批量初始化默认结构。

```sql
UPDATE product_info
SET extend_attrs = JSON_OBJECT(
        'afterSale',
        JSON_OBJECT(
            'supportRefund',
            true,
            'refundDays',
            7
        )
    ),
    updated_at = NOW()
WHERE extend_attrs IS NULL
  AND product_type = 1
  AND deleted = 0;
```

大表初始化时不建议一次性更新全表。应按主键范围或创建时间分批执行，避免长事务、锁等待和主从延迟。

### 常见问题

JSON扩展字段模型最常见的问题是边界失控。JSON字段一开始用于少量扩展，后续逐渐承载核心字段、查询条件、金额、状态和关联关系，最终导致查询困难、约束缺失和维护成本升高。

第一个常见问题是把核心字段放进 JSON。核心字段需要查询、校验、排序、统计和约束，应使用普通列建模。JSON只适合扩展信息，不适合承载核心业务事实。

第二个常见问题是 JSON 结构没有版本管理。不同时间写入的数据结构不一致，应用层解析时容易出现空值、类型错误和兼容问题。建议增加 `schema_version`，并在应用层维护不同版本的解析逻辑。

第三个常见问题是 JSON 查询性能差。直接在 `WHERE` 条件中大量使用 `JSON_EXTRACT`，通常不适合高频查询。高频字段应使用普通列，或通过生成列建立索引。

第四个常见问题是 JSON 字段过大。过大的 JSON 会增加行读取成本，更新时也容易造成额外开销。大报文、大富文本、大附件信息应考虑对象存储、日志表或归档表。

第五个常见问题是数组字段滥用。标签、城市、门店、适用范围等一对多关系如果需要高频查询，应拆成关系表。JSON数组适合低频展示，不适合复杂关系查询。

第六个常见问题是缺少数据校验。MySQL 可以保证字段是合法 JSON，但无法完整保证业务结构正确。字段必填、类型、枚举值、范围等校验应由应用层或写入服务统一处理。

第七个常见问题是局部更新覆盖异常。多个业务同时更新同一个 JSON 字段时，如果采用“读取完整 JSON 后整体覆盖”的方式，可能覆盖其他业务刚写入的路径。建议使用 `JSON_SET` 进行局部更新，并在必要时结合乐观锁控制并发。

### 总结

JSON扩展字段模型适合保存半结构化、低频查询、差异化明显、变化较快的扩展信息。它可以减少频繁加列带来的表结构变更成本，使业务在扩展属性、配置内容、外部参数和原始报文保存方面更加灵活。

设计 JSON 扩展字段时，应坚持“核心字段普通列、扩展字段 JSON”的原则。普通列负责查询、约束、排序、计算和业务判断；JSON字段负责补充信息、差异属性、配置内容和低频展示。对于逐渐变成高频查询条件的 JSON 路径，应及时提升为普通列或生成列索引。

JSON扩展字段模型与宽表模型、快照模型的区别在于：宽表模型面向复杂读查询，将多表字段扁平化；快照模型面向历史固化，保存业务发生时的数据状态；JSON扩展字段模型面向结构灵活性，解决字段变化和差异化扩展问题。实际建模时，应根据字段是否核心、是否高频查询、是否需要强约束来决定是否使用 JSON。



## EAV动态属性模型

EAV 动态属性模型是指将实体属性拆分为“实体、属性定义、属性值”三类数据进行建模。EAV 是 Entity-Attribute-Value 的缩写，适合处理不同实体拥有不同属性集合、属性数量较多、属性可配置、需要按动态属性查询或统计的业务场景。

EAV 模型与 JSON 扩展字段模型都可以解决字段动态扩展问题，但侧重点不同。JSON 更适合保存低频查询的半结构化扩展信息，EAV 更适合保存需要被配置、校验、查询、筛选、统计的动态属性。

### 适用场景

EAV 动态属性模型适合属性集合不固定、不同分类或类型拥有不同属性、属性需要后台配置、属性需要参与筛选查询的业务场景。

典型场景包括商品参数、设备属性、资产属性、用户画像标签、表单字段、问卷答案、客户扩展资料、车辆配置、房源特征、医疗检查指标等。

EAV 动态属性模型通常用于以下几类需求：

| 场景     | 说明                                                     |
| -------- | -------------------------------------------------------- |
| 动态属性 | 不同分类、不同类型实体拥有不同属性集合                   |
| 后台配置 | 属性由运营或管理员配置，不适合频繁改表                   |
| 条件筛选 | 动态属性需要参与查询，例如按颜色、尺寸、材质筛选商品     |
| 属性校验 | 属性需要配置数据类型、是否必填、取值范围、枚举选项       |
| 属性统计 | 需要统计某个属性值分布，例如设备型号、商品材质、客户等级 |
| 多值属性 | 一个属性可能有多个值，例如适用城市、商品标签、支持接口   |

不适合使用 EAV 的场景包括核心业务字段、高频强事务字段、金额字段、状态字段、主外键关系字段、强唯一约束字段。例如订单状态、支付金额、库存数量、账户余额、用户手机号等字段应使用普通列建模。

### 建模结构

EAV 模型通常由实体主表、属性定义表、属性选项表和属性值表组成。实体主表保存稳定业务字段，属性定义表描述有哪些动态属性，属性选项表保存枚举类属性可选值，属性值表保存某个实体在某个属性上的具体值。

下面以商品动态属性为例。商品基础字段使用普通列，商品参数使用 EAV 模型保存。

```sql
CREATE TABLE product_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    product_no VARCHAR(32) NOT NULL COMMENT '商品编号',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    brand_id BIGINT DEFAULT NULL COMMENT '品牌ID',
    sale_status TINYINT NOT NULL DEFAULT 10 COMMENT '销售状态：10草稿，20上架，30下架',
    sale_price DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '销售价格',
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息表';

CREATE TABLE product_attr_define (
    id BIGINT NOT NULL COMMENT '主键ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    attr_code VARCHAR(64) NOT NULL COMMENT '属性编码',
    attr_name VARCHAR(128) NOT NULL COMMENT '属性名称',
    attr_type VARCHAR(32) NOT NULL COMMENT '属性类型：STRING、NUMBER、DATETIME、BOOLEAN、OPTION、MULTI_OPTION',
    required_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填：0否，1是',
    searchable_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否可搜索：0否，1是',
    multiple_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否多值：0否，1是',
    unit_name VARCHAR(32) DEFAULT NULL COMMENT '单位名称',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    enabled_flag TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品属性定义表';

CREATE TABLE product_attr_option (
    id BIGINT NOT NULL COMMENT '主键ID',
    attr_id BIGINT NOT NULL COMMENT '属性定义ID',
    option_code VARCHAR(64) NOT NULL COMMENT '选项编码',
    option_name VARCHAR(128) NOT NULL COMMENT '选项名称',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    enabled_flag TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品属性选项表';

CREATE TABLE product_attr_value (
    id BIGINT NOT NULL COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_no VARCHAR(32) NOT NULL COMMENT '商品编号',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    attr_id BIGINT NOT NULL COMMENT '属性定义ID',
    attr_code VARCHAR(64) NOT NULL COMMENT '属性编码',
    attr_name VARCHAR(128) NOT NULL COMMENT '属性名称快照',
    attr_type VARCHAR(32) NOT NULL COMMENT '属性类型快照',
    string_value VARCHAR(512) DEFAULT NULL COMMENT '字符串值',
    number_value DECIMAL(18,6) DEFAULT NULL COMMENT '数值',
    datetime_value DATETIME DEFAULT NULL COMMENT '时间值',
    boolean_value TINYINT DEFAULT NULL COMMENT '布尔值：0否，1是',
    option_code VARCHAR(64) DEFAULT NULL COMMENT '选项编码',
    option_name VARCHAR(128) DEFAULT NULL COMMENT '选项名称快照',
    value_text VARCHAR(1024) DEFAULT NULL COMMENT '展示值',
    value_sort_no INT NOT NULL DEFAULT 0 COMMENT '多值排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品属性值表';
```

该结构中，`product_info` 保存商品核心字段，`product_attr_define` 保存属性定义，`product_attr_option` 保存枚举选项，`product_attr_value` 保存商品具体属性值。

例如手机分类下可以配置颜色、内存、屏幕尺寸、是否支持 5G 等属性；服装分类下可以配置尺码、材质、季节、版型等属性。不同分类的商品不需要修改商品主表结构，只需要维护属性定义和属性值。

属性值表中同时保存 `attr_id` 和 `attr_code`。`attr_id` 便于关联属性定义，`attr_code` 便于稳定查询和数据迁移。`attr_name`、`attr_type`、`option_name` 属于快照字段，可以避免属性名称或选项名称调整后影响历史展示。

### 字段设计

EAV 字段设计的重点是属性定义要可控，属性值要可查询，数据类型要清晰，不能把所有值都无差别地塞进一个字符串字段。

建议字段设计如下：

| 字段类型 | 命名示例                     | 设计说明                             |
| -------- | ---------------------------- | ------------------------------------ |
| 实体主键 | `product_id`                 | 表示属性值归属的业务实体             |
| 实体编号 | `product_no`                 | 便于外部系统查询和数据排查           |
| 分类字段 | `category_id`                | 用于限定属性定义范围                 |
| 属性主键 | `attr_id`                    | 关联属性定义表                       |
| 属性编码 | `attr_code`                  | 稳定编码，适合程序查询和配置引用     |
| 属性名称 | `attr_name`                  | 属性名称快照，用于展示               |
| 属性类型 | `attr_type`                  | 控制值写入哪个类型字段               |
| 字符串值 | `string_value`               | 保存文本类属性                       |
| 数值值   | `number_value`               | 保存数值类属性，支持范围查询         |
| 时间值   | `datetime_value`             | 保存日期时间类属性，支持时间范围查询 |
| 布尔值   | `boolean_value`              | 保存是否类属性                       |
| 枚举值   | `option_code`、`option_name` | 保存选项编码和选项名称               |
| 展示值   | `value_text`                 | 统一展示字段，便于列表或详情展示     |
| 多值排序 | `value_sort_no`              | 多值属性排序使用                     |

字段设计时需要注意以下原则：

第一，属性定义必须有稳定编码。`attr_code` 应由系统生成或配置规范控制，不建议使用属性名称作为程序判断依据。属性名称可以修改，属性编码应保持稳定。

第二，属性值应按类型拆列保存。数值属性写入 `number_value`，时间属性写入 `datetime_value`，布尔属性写入 `boolean_value`，枚举属性写入 `option_code`。不要把所有属性值都写入 `string_value`，否则范围查询、排序和统计都会变困难。

第三，属性值表可以保存属性名称和选项名称快照。这样即使属性定义或选项名称后续调整，历史商品详情仍然可以展示原始值。是否需要同步更新名称快照，应根据业务展示语义决定。

第四，多值属性可以使用多行表示。一个商品的同一个属性如果有多个值，例如支持城市、适用人群、商品标签，可以在属性值表中写多行，并使用 `value_sort_no` 控制展示顺序。

第五，属性定义表应控制是否必填、是否可搜索、是否多值、是否启用。应用层写入时应根据定义进行校验，避免属性值表出现无效属性或错误类型。

### 索引设计

EAV 模型的索引设计比普通表更重要。由于属性值被拆分为多行，常用查询通常会围绕实体、属性、属性值、分类和类型字段展开。

商品属性定义表常见索引如下：

```sql
ALTER TABLE product_attr_define
    ADD UNIQUE KEY uk_category_attr_code (category_id, attr_code),
    ADD KEY idx_category_enabled_sort (category_id, enabled_flag, sort_no);
```

商品属性选项表常见索引如下：

```sql
ALTER TABLE product_attr_option
    ADD UNIQUE KEY uk_attr_option_code (attr_id, option_code),
    ADD KEY idx_attr_enabled_sort (attr_id, enabled_flag, sort_no);
```

商品属性值表常见索引如下：

```sql
ALTER TABLE product_attr_value
    ADD UNIQUE KEY uk_product_attr_option_sort (product_id, attr_id, option_code, value_sort_no),
    ADD KEY idx_product_attr (product_id, attr_id),
    ADD KEY idx_attr_string (attr_code, string_value),
    ADD KEY idx_attr_number (attr_code, number_value),
    ADD KEY idx_attr_datetime (attr_code, datetime_value),
    ADD KEY idx_attr_boolean (attr_code, boolean_value),
    ADD KEY idx_attr_option (attr_code, option_code),
    ADD KEY idx_category_attr_option (category_id, attr_code, option_code),
    ADD KEY idx_category_attr_number (category_id, attr_code, number_value);
```

索引设计说明如下：

| 索引                        | 适用查询           | 说明                                 |
| --------------------------- | ------------------ | ------------------------------------ |
| `uk_category_attr_code`     | 查询分类下属性定义 | 保证同一分类下属性编码唯一           |
| `idx_category_enabled_sort` | 加载分类属性配置   | 支持按分类查询启用属性并排序         |
| `uk_attr_option_code`       | 查询属性选项       | 保证同一属性下选项编码唯一           |
| `idx_attr_enabled_sort`     | 加载属性可用选项   | 支持按属性查询启用选项并排序         |
| `idx_product_attr`          | 查询商品属性列表   | 商品详情页加载属性值                 |
| `idx_attr_string`           | 按文本属性查询     | 适合低频精确匹配，不适合大量模糊查询 |
| `idx_attr_number`           | 按数值属性范围查询 | 适合价格、尺寸、重量等范围筛选       |
| `idx_attr_datetime`         | 按时间属性范围查询 | 适合生产日期、到期时间等筛选         |
| `idx_attr_boolean`          | 按布尔属性查询     | 适合是否支持、是否开启等筛选         |
| `idx_attr_option`           | 按枚举属性查询     | 适合颜色、尺码、型号等筛选           |
| `idx_category_attr_option`  | 分类内枚举筛选     | 商品筛选页常用                       |
| `idx_category_attr_number`  | 分类内数值范围筛选 | 分类筛选页常用                       |

EAV 不适合无限制任意组合筛选。若商品筛选页需要同时按十几个动态属性组合查询，MySQL EAV 查询会变复杂，性能也难以稳定。此时可以考虑将 EAV 数据同步到搜索引擎、搜索辅助表或专门的宽表读模型。

### 常用查询

EAV 模型的常用查询主要包括加载属性定义、查询实体属性值、按属性筛选实体、按多个动态属性组合筛选、统计属性值分布等。查询时应尽量先限定分类、属性编码和有效状态，避免直接扫描整张属性值表。

#### 查询分类下的属性定义

商品编辑页或详情页通常需要先加载某个分类下启用的属性定义，用于渲染动态表单。

```sql
SELECT
    id,
    category_id,
    attr_code,
    attr_name,
    attr_type,
    required_flag,
    searchable_flag,
    multiple_flag,
    unit_name,
    sort_no
FROM product_attr_define
WHERE category_id = 10001
  AND enabled_flag = 1
  AND deleted = 0
ORDER BY sort_no ASC, id ASC;
```

该查询适合商品新增页、编辑页、属性配置页。前端可以根据 `attr_type` 渲染输入框、数字框、日期选择器、开关或下拉选项。

#### 查询属性的枚举选项

当属性类型为 `OPTION` 或 `MULTI_OPTION` 时，需要查询该属性下的可用选项。

```sql
SELECT
    id,
    attr_id,
    option_code,
    option_name,
    sort_no
FROM product_attr_option
WHERE attr_id = 50001
  AND enabled_flag = 1
  AND deleted = 0
ORDER BY sort_no ASC, id ASC;
```

该查询适合颜色、尺码、材质、适用人群等枚举属性。选项编码用于写入和查询，选项名称用于展示。

#### 查询商品完整动态属性

商品详情页通常需要查询某个商品的全部动态属性值，并按属性定义排序展示。

```sql
SELECT
    d.attr_code,
    d.attr_name,
    d.attr_type,
    d.unit_name,
    d.multiple_flag,
    v.string_value,
    v.number_value,
    v.datetime_value,
    v.boolean_value,
    v.option_code,
    v.option_name,
    v.value_text,
    v.value_sort_no
FROM product_attr_define d
LEFT JOIN product_attr_value v
    ON v.attr_id = d.id
   AND v.product_id = 100001
   AND v.deleted = 0
WHERE d.category_id = 10001
  AND d.enabled_flag = 1
  AND d.deleted = 0
ORDER BY d.sort_no ASC, v.value_sort_no ASC, d.id ASC;
```

该查询适合商品详情页和编辑页。使用属性定义表作为主表，可以展示未填写的属性，并根据 `required_flag` 做必填校验。

#### 按枚举属性筛选商品

商品列表经常需要按颜色、尺码、材质等枚举属性筛选。此类查询应使用 `attr_code` 和 `option_code`。

```sql
SELECT
    p.id,
    p.product_no,
    p.product_name,
    p.sale_price,
    p.sale_status
FROM product_info p
JOIN product_attr_value v
    ON v.product_id = p.id
   AND v.attr_code = 'color'
   AND v.option_code = 'black'
   AND v.deleted = 0
WHERE p.category_id = 10001
  AND p.sale_status = 20
  AND p.deleted = 0
ORDER BY p.created_at DESC
LIMIT 20;
```

该查询适合分类商品列表。枚举属性查询应优先使用稳定的 `option_code`，不要使用 `option_name` 作为查询条件。

#### 按数值属性范围筛选商品

数值属性适合范围筛选，例如屏幕尺寸、重量、功率、容量等。此类属性应写入 `number_value`。

```sql
SELECT
    p.id,
    p.product_no,
    p.product_name,
    p.sale_price,
    v.number_value AS screen_size
FROM product_info p
JOIN product_attr_value v
    ON v.product_id = p.id
   AND v.attr_code = 'screen_size'
   AND v.number_value >= 6.0
   AND v.number_value < 7.0
   AND v.deleted = 0
WHERE p.category_id = 10001
  AND p.sale_status = 20
  AND p.deleted = 0
ORDER BY p.sale_price ASC
LIMIT 20;
```

该查询适合按屏幕尺寸、重量、容量等数值型属性筛选。若将数值保存为字符串，会导致范围查询和排序不准确。

#### 按多个动态属性组合筛选商品

当需要同时按多个属性筛选时，可以多次关联属性值表，每个属性使用一个别名。

```sql
SELECT
    p.id,
    p.product_no,
    p.product_name,
    p.sale_price
FROM product_info p
JOIN product_attr_value color
    ON color.product_id = p.id
   AND color.attr_code = 'color'
   AND color.option_code = 'black'
   AND color.deleted = 0
JOIN product_attr_value memory
    ON memory.product_id = p.id
   AND memory.attr_code = 'memory'
   AND memory.option_code = '256g'
   AND memory.deleted = 0
JOIN product_attr_value screen
    ON screen.product_id = p.id
   AND screen.attr_code = 'screen_size'
   AND screen.number_value >= 6.0
   AND screen.number_value < 7.0
   AND screen.deleted = 0
WHERE p.category_id = 10001
  AND p.sale_status = 20
  AND p.deleted = 0
ORDER BY p.sale_price ASC
LIMIT 20;
```

该写法直观，但属性组合越多，JOIN 越复杂。对于高频复杂筛选页，建议将 EAV 属性同步到搜索引擎或搜索辅助表。

#### 使用聚合方式筛选多个属性

也可以先在属性值表中聚合出满足多个条件的商品 ID，再回表查询商品信息。

```sql
SELECT
    p.id,
    p.product_no,
    p.product_name,
    p.sale_price
FROM product_info p
JOIN (
    SELECT
        product_id
    FROM product_attr_value
    WHERE deleted = 0
      AND category_id = 10001
      AND (
          (attr_code = 'color' AND option_code = 'black')
          OR (attr_code = 'memory' AND option_code = '256g')
          OR (attr_code = 'screen_size' AND number_value >= 6.0 AND number_value < 7.0)
      )
    GROUP BY product_id
    HAVING COUNT(DISTINCT attr_code) = 3
) matched ON matched.product_id = p.id
WHERE p.sale_status = 20
  AND p.deleted = 0
ORDER BY p.sale_price ASC
LIMIT 20;
```

该方式适合条件由前端动态传入的场景，但需要注意不同属性条件的去重、同属性多值匹配和执行计划稳定性。

#### 统计某个属性值分布

商品筛选页常见需求是展示某个属性下各选项数量，例如颜色分布、内存分布、材质分布。

```sql
SELECT
    v.option_code,
    v.option_name,
    COUNT(DISTINCT v.product_id) AS product_count
FROM product_attr_value v
JOIN product_info p ON p.id = v.product_id
WHERE v.category_id = 10001
  AND v.attr_code = 'color'
  AND v.deleted = 0
  AND p.sale_status = 20
  AND p.deleted = 0
GROUP BY v.option_code, v.option_name
ORDER BY product_count DESC;
```

该查询适合筛选面板统计。若数据量较大，应考虑预聚合统计表，避免每次打开列表页都实时聚合。

#### 查询缺失必填属性的商品

属性定义启用必填后，可以查询某个分类下缺失必填属性的商品，用于数据质量检查。

```sql
SELECT
    p.id,
    p.product_no,
    p.product_name,
    d.attr_code,
    d.attr_name
FROM product_info p
JOIN product_attr_define d
    ON d.category_id = p.category_id
   AND d.required_flag = 1
   AND d.enabled_flag = 1
   AND d.deleted = 0
LEFT JOIN product_attr_value v
    ON v.product_id = p.id
   AND v.attr_id = d.id
   AND v.deleted = 0
WHERE p.category_id = 10001
  AND p.deleted = 0
  AND v.id IS NULL
ORDER BY p.id ASC, d.sort_no ASC
LIMIT 100;
```

该查询适合数据治理、商品发布前校验和批量修复任务。应用层在保存商品时也应做同样校验，避免无效数据进入数据库。

### 常用写入

EAV 模型写入通常分为属性定义维护、选项维护、实体属性值写入、属性值更新和批量修复。写入时应先校验属性定义，再写入属性值。

#### 新增属性定义

新增动态属性时，应先写入属性定义。属性编码应稳定且唯一，建议使用英文小写、下划线或业务统一编码规范。

```sql
INSERT INTO product_attr_define (
    id,
    category_id,
    attr_code,
    attr_name,
    attr_type,
    required_flag,
    searchable_flag,
    multiple_flag,
    unit_name,
    sort_no,
    enabled_flag,
    created_at,
    updated_at,
    deleted
)
VALUES (
    50001,
    10001,
    'color',
    '颜色',
    'OPTION',
    1,
    1,
    0,
    NULL,
    10,
    1,
    NOW(),
    NOW(),
    0
);
```

该写入适合分类属性配置。属性一旦被大量数据引用，不建议随意修改 `attr_code` 和 `attr_type`。

#### 新增属性选项

枚举属性需要维护可选项，例如颜色属性下的黑色、白色、蓝色。

```sql
INSERT INTO product_attr_option (
    id,
    attr_id,
    option_code,
    option_name,
    sort_no,
    enabled_flag,
    created_at,
    updated_at,
    deleted
)
VALUES
    (60001, 50001, 'black', '黑色', 10, 1, NOW(), NOW(), 0),
    (60002, 50001, 'white', '白色', 20, 1, NOW(), NOW(), 0),
    (60003, 50001, 'blue', '蓝色', 30, 1, NOW(), NOW(), 0);
```

选项编码应保持稳定，选项名称可以根据展示需求调整。已经被历史数据引用的选项，不建议物理删除，通常使用禁用或逻辑删除。

#### 写入商品单值属性

保存商品属性值时，应根据属性类型写入对应值字段。枚举属性写入 `option_code` 和 `option_name`，同时写入统一展示字段 `value_text`。

```sql
INSERT INTO product_attr_value (
    id,
    product_id,
    product_no,
    category_id,
    attr_id,
    attr_code,
    attr_name,
    attr_type,
    option_code,
    option_name,
    value_text,
    value_sort_no,
    created_at,
    updated_at,
    deleted
)
SELECT
    70001,
    100001,
    'P202605130001',
    10001,
    d.id,
    d.attr_code,
    d.attr_name,
    d.attr_type,
    o.option_code,
    o.option_name,
    o.option_name,
    0,
    NOW(),
    NOW(),
    0
FROM product_attr_define d
JOIN product_attr_option o ON o.attr_id = d.id
WHERE d.category_id = 10001
  AND d.attr_code = 'color'
  AND d.enabled_flag = 1
  AND d.deleted = 0
  AND o.option_code = 'black'
  AND o.enabled_flag = 1
  AND o.deleted = 0;
```

该写法可以保证写入时使用的是有效属性和有效选项。应用层通常还需要校验分类、属性类型和是否允许多值。

#### 写入商品数值属性

数值属性应写入 `number_value`，不要写入字符串字段。展示值可以按业务需要拼接单位。

```sql
INSERT INTO product_attr_value (
    id,
    product_id,
    product_no,
    category_id,
    attr_id,
    attr_code,
    attr_name,
    attr_type,
    number_value,
    value_text,
    value_sort_no,
    created_at,
    updated_at,
    deleted
)
SELECT
    70002,
    100001,
    'P202605130001',
    10001,
    d.id,
    d.attr_code,
    d.attr_name,
    d.attr_type,
    6.7,
    CONCAT('6.7', IFNULL(d.unit_name, '')),
    0,
    NOW(),
    NOW(),
    0
FROM product_attr_define d
WHERE d.category_id = 10001
  AND d.attr_code = 'screen_size'
  AND d.attr_type = 'NUMBER'
  AND d.enabled_flag = 1
  AND d.deleted = 0;
```

该写法适合屏幕尺寸、重量、容量、功率等属性。范围筛选和排序应基于 `number_value`。

#### 写入多值属性

多值属性可以使用多行保存。例如一个商品支持多个服务城市。

```sql
INSERT INTO product_attr_value (
    id,
    product_id,
    product_no,
    category_id,
    attr_id,
    attr_code,
    attr_name,
    attr_type,
    option_code,
    option_name,
    value_text,
    value_sort_no,
    created_at,
    updated_at,
    deleted
)
SELECT
    70010,
    100001,
    'P202605130001',
    10001,
    d.id,
    d.attr_code,
    d.attr_name,
    d.attr_type,
    o.option_code,
    o.option_name,
    o.option_name,
    10,
    NOW(),
    NOW(),
    0
FROM product_attr_define d
JOIN product_attr_option o ON o.attr_id = d.id
WHERE d.category_id = 10001
  AND d.attr_code = 'service_city'
  AND d.multiple_flag = 1
  AND o.option_code = 'hangzhou'
  AND d.deleted = 0
  AND o.deleted = 0;
```

多值属性不建议将多个值拼接成一个字符串保存。多行结构更利于查询、统计和删除单个值。

#### 更新商品属性值

更新单值属性时，可以先逻辑删除旧值，再插入新值；也可以基于唯一约束使用 upsert。下面示例为更新颜色属性。

```sql
UPDATE product_attr_value
SET option_code = 'white',
    option_name = '白色',
    value_text = '白色',
    updated_at = NOW()
WHERE product_id = 100001
  AND attr_code = 'color'
  AND deleted = 0;
```

如果属性类型发生变化，不建议直接原地更新已有值，应通过数据迁移脚本处理，并保留迁移记录。

#### 删除商品属性值

删除动态属性值通常使用逻辑删除，避免历史数据和审计链路丢失。

```sql
UPDATE product_attr_value
SET deleted = 1,
    updated_at = NOW()
WHERE product_id = 100001
  AND attr_code = 'service_city'
  AND option_code = 'hangzhou'
  AND deleted = 0;
```

对于多值属性，可以按 `option_code` 删除单个值；对于单值属性，可以按 `product_id + attr_code` 删除。

#### 批量修复属性名称快照

当属性名称变更后，如果业务要求历史展示同步为最新名称，可以批量回刷属性值表中的 `attr_name`。

```sql
UPDATE product_attr_value v
JOIN product_attr_define d ON d.id = v.attr_id
SET v.attr_name = d.attr_name,
    v.updated_at = NOW()
WHERE v.attr_code = 'color'
  AND v.deleted = 0
  AND d.deleted = 0;
```

是否回刷属性名称取决于业务语义。如果属性值表需要保留历史快照，则不应回刷；如果详情页要求展示最新属性名称，则可以回刷。

### 常见问题

EAV 动态属性模型最常见的问题是过度动态化。并不是所有字段都适合放入 EAV。核心字段进入 EAV 后，查询、约束、统计和代码维护都会变复杂。

第一个常见问题是所有值都用字符串保存。这样虽然写入简单，但数值范围查询、时间范围查询、排序和统计都会出现问题。应根据属性类型写入不同值字段。

第二个常见问题是属性编码不稳定。如果使用属性名称作为查询条件，属性改名后程序和报表容易失效。应使用稳定的 `attr_code` 和 `option_code`，名称只用于展示。

第三个常见问题是查询 JOIN 过多。多个动态属性组合筛选时，需要多次关联属性值表或使用聚合筛选。条件越多，SQL 越复杂。高频复杂筛选应同步到搜索引擎、搜索辅助表或宽表读模型。

第四个常见问题是缺少属性定义校验。属性值写入前必须校验属性是否存在、是否启用、类型是否匹配、是否必填、是否允许多值、选项是否合法。否则属性值表会很快变成不可控的脏数据集合。

第五个常见问题是属性删除影响历史数据。属性定义被删除后，历史属性值可能无法展示或解释。建议对属性定义和选项使用禁用或逻辑删除，不要轻易物理删除。

第六个常见问题是多值属性处理不规范。多个值拼接到一个字符串中会导致查询和统计困难。多值属性应使用多行保存，并通过 `value_sort_no` 控制顺序。

第七个常见问题是统计性能差。EAV 表数据量通常增长很快，实时统计属性分布可能成本较高。对于商品筛选面板、画像分析、报表统计等场景，应考虑异步预聚合。

第八个常见问题是 EAV 与 JSON 边界不清。需要查询、筛选、统计、校验的动态属性适合 EAV；只用于详情展示或保存原始扩展信息的字段更适合 JSON。

### 总结

EAV 动态属性模型适合属性集合不固定、属性需要配置、属性需要校验、属性需要查询和统计的业务场景。它通过属性定义表、属性选项表和属性值表，将动态字段从固定表结构中拆分出来，降低频繁改表的成本。

设计 EAV 模型时，应坚持“核心字段普通列、动态可查询属性 EAV、低频扩展信息 JSON”的原则。属性定义要有稳定编码，属性值要按类型拆列保存，枚举选项要使用稳定编码，多值属性要使用多行表达，查询索引要围绕实体、属性和值设计。

EAV 模型的优势是灵活、可配置、适合动态筛选；缺点是查询复杂、约束弱、数据量增长快、统计成本高。对于高频复杂查询，应结合宽表、搜索辅助表、缓存或搜索引擎，不要把所有动态查询压力都压在 EAV 表上。