# 商品、交易与支付模型



## 商品-SPU-SKU模型

商品-SPU-SKU模型用于描述电商、零售、商城、订货系统中的商品基础资料。SPU表示标准化商品，SKU表示可销售、可库存、可定价的具体商品规格。该模型通常作为订单、库存、购物车、营销、搜索、结算等业务模型的基础数据来源。

### 适用场景

商品-SPU-SKU模型适合用于同一个商品存在多个销售规格的业务场景。SPU侧负责承载商品的通用信息，SKU侧负责承载规格、价格、库存、条码等可交易信息。

常见适用场景包括：

| 场景           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 电商商城       | 例如一件T恤有不同颜色、尺码，每个颜色和尺码组合对应一个SKU   |
| 零售门店       | 例如同一款饮料有不同容量、包装规格，每个规格单独售卖         |
| B2B订货        | 例如同一商品按箱、按件、按组销售，不同销售单位可以建成不同SKU |
| 多规格商品     | 例如手机有不同颜色、内存版本，每个版本价格和库存不同         |
| 商品搜索与展示 | SPU用于聚合展示，SKU用于下单、库存扣减和价格计算             |

不适合过度拆分的场景是商品本身没有规格差异，且不存在多价格、多库存、多条码管理需求。这类业务可以保留SPU和SKU结构，但一个SPU下只创建一个默认SKU，避免后续模型不兼容。

### 建模结构

商品-SPU-SKU模型通常由商品SPU表、商品SKU表、商品规格项表、商品规格值表、SKU规格关系表组成。SPU用于表达“这是什么商品”，SKU用于表达“具体卖哪一个规格”。

核心关系如下：

| 表名                 | 说明          | 关系                        |
| -------------------- | ------------- | --------------------------- |
| `product_spu`        | 商品SPU主表   | 一个SPU可以包含多个SKU      |
| `product_sku`        | 商品SKU表     | 每个SKU归属于一个SPU        |
| `product_spec_item`  | 商品规格项表  | 例如颜色、尺码、容量        |
| `product_spec_value` | 商品规格值表  | 例如红色、XL、500ml         |
| `product_sku_spec`   | SKU规格关系表 | 记录某个SKU由哪些规格值组成 |

推荐的业务关系为：

```text
product_spu 1 —— N product_sku

product_spu 1 —— N product_spec_item

product_spec_item 1 —— N product_spec_value

product_sku N —— N product_spec_value
```

其中，`product_sku_spec`用于解决SKU与规格值之间的多对多关系。例如“红色 + XL”是一个SKU，“黑色 + L”是另一个SKU。

建模时需要注意，SPU和SKU职责必须分离：

| 层级 | 应放字段                                          | 不建议放字段               |
| ---- | ------------------------------------------------- | -------------------------- |
| SPU  | 商品名称、类目、品牌、主图、详情、上下架状态      | 具体库存、具体销售价、条码 |
| SKU  | SKU编码、规格组合、销售价、成本价、库存单位、条码 | 长篇详情、品牌、类目       |
| 规格 | 颜色、尺码、容量、材质等规格项和值                | 订单价格、库存数量         |

### 字段设计

字段设计需要同时满足商品展示、交易下单、库存扣减、价格计算和后续扩展。金额字段建议使用`decimal`，状态字段建议使用`tinyint`，业务编码建议使用`varchar`。

SPU表用于保存商品公共信息。

```sql
CREATE TABLE product_spu (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    spu_code VARCHAR(64) NOT NULL COMMENT 'SPU编码',
    spu_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    category_id BIGINT UNSIGNED NOT NULL COMMENT '类目ID',
    brand_id BIGINT UNSIGNED DEFAULT NULL COMMENT '品牌ID',
    main_image VARCHAR(500) DEFAULT NULL COMMENT '商品主图',
    selling_point VARCHAR(500) DEFAULT NULL COMMENT '商品卖点',
    detail_html MEDIUMTEXT DEFAULT NULL COMMENT '商品详情HTML',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0草稿，1上架，2下架',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU表';
```

SKU表用于保存具体可交易商品信息。

```sql
CREATE TABLE product_sku (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    spu_id BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID',
    sku_code VARCHAR(64) NOT NULL COMMENT 'SKU编码',
    sku_name VARCHAR(200) NOT NULL COMMENT 'SKU名称',
    spec_text VARCHAR(500) NOT NULL COMMENT '规格描述，例如：颜色:红色;尺码:XL',
    barcode VARCHAR(64) DEFAULT NULL COMMENT '商品条码',
    sale_price DECIMAL(12,2) NOT NULL COMMENT '销售价',
    market_price DECIMAL(12,2) DEFAULT NULL COMMENT '市场价',
    cost_price DECIMAL(12,2) DEFAULT NULL COMMENT '成本价',
    stock_unit VARCHAR(32) NOT NULL DEFAULT '件' COMMENT '库存单位',
    weight DECIMAL(12,3) DEFAULT NULL COMMENT '重量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';
```

规格项表用于定义SPU下有哪些规格维度。

```sql
CREATE TABLE product_spec_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    spu_id BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID',
    spec_name VARCHAR(64) NOT NULL COMMENT '规格项名称，例如颜色、尺码',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格项表';
```

规格值表用于定义每个规格项下有哪些可选值。

```sql
CREATE TABLE product_spec_value (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    spu_id BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID',
    spec_item_id BIGINT UNSIGNED NOT NULL COMMENT '规格项ID',
    spec_value VARCHAR(64) NOT NULL COMMENT '规格值，例如红色、XL',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格值表';
```

SKU规格关系表用于记录一个SKU由哪些规格值组成。

```sql
CREATE TABLE product_sku_spec (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    sku_id BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    spu_id BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID',
    spec_item_id BIGINT UNSIGNED NOT NULL COMMENT '规格项ID',
    spec_value_id BIGINT UNSIGNED NOT NULL COMMENT '规格值ID',
    spec_name VARCHAR(64) NOT NULL COMMENT '规格项名称快照',
    spec_value VARCHAR(64) NOT NULL COMMENT '规格值快照',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU规格关系表';
```

字段设计建议如下：

| 字段类型 | 设计建议                                                     |
| -------- | ------------------------------------------------------------ |
| 主键     | 使用`BIGINT UNSIGNED`，便于分布式ID或自增ID扩展              |
| 编码     | `spu_code`、`sku_code`使用业务编码，便于对接外部系统         |
| 金额     | 使用`DECIMAL(12,2)`，避免浮点精度问题                        |
| 状态     | 使用`TINYINT`，避免直接使用字符串状态                        |
| 删除标识 | 使用`deleted`软删除字段，避免业务数据直接物理删除            |
| 时间字段 | 统一使用`created_at`、`updated_at`                           |
| 快照字段 | `product_sku_spec`保留规格名称和值快照，避免规格名称变更影响历史展示 |

### 索引设计

索引设计应围绕商品列表、商品详情、SKU选择、编码查询、状态过滤、规格组合校验等高频访问路径展开。建模结构中不直接描述索引，索引统一在本章节给出。

SPU表推荐索引如下：

```sql
ALTER TABLE product_spu
    ADD UNIQUE KEY uk_spu_code (spu_code),
    ADD KEY idx_category_status_deleted (category_id, status, deleted),
    ADD KEY idx_brand_status_deleted (brand_id, status, deleted),
    ADD KEY idx_status_sort_deleted (status, sort_no, deleted);
```

SKU表推荐索引如下：

```sql
ALTER TABLE product_sku
    ADD UNIQUE KEY uk_sku_code (sku_code),
    ADD UNIQUE KEY uk_barcode (barcode),
    ADD KEY idx_spu_status_deleted (spu_id, status, deleted),
    ADD KEY idx_price_status_deleted (sale_price, status, deleted);
```

规格项表推荐索引如下：

```sql
ALTER TABLE product_spec_item
    ADD UNIQUE KEY uk_spu_spec_name (spu_id, spec_name),
    ADD KEY idx_spu_deleted (spu_id, deleted);
```

规格值表推荐索引如下：

```sql
ALTER TABLE product_spec_value
    ADD UNIQUE KEY uk_item_value (spec_item_id, spec_value),
    ADD KEY idx_spu_item_deleted (spu_id, spec_item_id, deleted);
```

SKU规格关系表推荐索引如下：

```sql
ALTER TABLE product_sku_spec
    ADD UNIQUE KEY uk_sku_item (sku_id, spec_item_id),
    ADD KEY idx_spu_sku (spu_id, sku_id),
    ADD KEY idx_value_sku (spec_value_id, sku_id);
```

索引设计说明：

| 索引                          | 作用                             |
| ----------------------------- | -------------------------------- |
| `uk_spu_code`                 | 保证SPU业务编码唯一              |
| `uk_sku_code`                 | 保证SKU业务编码唯一              |
| `uk_barcode`                  | 支持通过条码快速查询SKU          |
| `idx_category_status_deleted` | 支持类目商品列表查询             |
| `idx_spu_status_deleted`      | 支持商品详情页加载SPU下可用SKU   |
| `uk_spu_spec_name`            | 防止同一个SPU下重复创建规格项    |
| `uk_item_value`               | 防止同一个规格项下重复创建规格值 |
| `uk_sku_item`                 | 防止同一个SKU绑定重复规格项      |

需要注意，`barcode`如果允许为空，MySQL唯一索引允许多个`NULL`值。如果业务要求条码必须唯一且不能为空，应将`barcode`设置为`NOT NULL`，或者在应用层校验空条码逻辑。

### 常用查询

常用查询需要根据实际业务访问路径设计。商品模型最常见的查询包括商品列表、商品详情、SKU列表、规格矩阵、编码查询和规格组合查询。

#### 查询上架商品列表

商品列表通常按类目、品牌、状态、关键字等条件过滤。列表页一般查询SPU，不直接展开全部SKU，避免数据量过大。

```sql
SELECT
    id,
    spu_code,
    spu_name,
    category_id,
    brand_id,
    main_image,
    selling_point,
    status,
    sort_no,
    created_at
FROM product_spu
WHERE deleted = 0
  AND status = 1
  AND category_id = 10001
ORDER BY sort_no DESC, id DESC
LIMIT 20 OFFSET 0;
```

如果需要商品名称模糊搜索，可以增加`spu_name LIKE`条件，但大数据量场景建议使用搜索辅助表或搜索引擎，不建议长期依赖`LIKE '%关键词%'`。

```sql
SELECT
    id,
    spu_code,
    spu_name,
    main_image,
    selling_point,
    status
FROM product_spu
WHERE deleted = 0
  AND status = 1
  AND spu_name LIKE CONCAT('%', '手机', '%')
ORDER BY id DESC
LIMIT 20;
```

#### 查询商品详情

商品详情页通常先查询SPU公共信息，再查询该SPU下的SKU列表和规格数据。

```sql
SELECT
    id,
    spu_code,
    spu_name,
    category_id,
    brand_id,
    main_image,
    selling_point,
    detail_html,
    status
FROM product_spu
WHERE id = 10001
  AND deleted = 0;
```

查询SPU下可销售SKU：

```sql
SELECT
    id,
    spu_id,
    sku_code,
    sku_name,
    spec_text,
    barcode,
    sale_price,
    market_price,
    stock_unit,
    status
FROM product_sku
WHERE spu_id = 10001
  AND status = 1
  AND deleted = 0
ORDER BY id ASC;
```

#### 查询商品规格项和值

规格选择器需要展示规格项和规格值，例如颜色、尺码以及对应的可选值。

```sql
SELECT
    i.id AS spec_item_id,
    i.spec_name,
    i.sort_no AS item_sort_no,
    v.id AS spec_value_id,
    v.spec_value,
    v.sort_no AS value_sort_no
FROM product_spec_item i
JOIN product_spec_value v ON v.spec_item_id = i.id
WHERE i.spu_id = 10001
  AND i.deleted = 0
  AND v.deleted = 0
ORDER BY i.sort_no ASC, v.sort_no ASC;
```

该查询适合用于前端渲染规格面板。后端可以按`spec_item_id`进行分组，形成规格项和值列表。

#### 查询SKU规格组合

下单前通常需要根据SKU查询完整规格组合，用于订单明细快照。

```sql
SELECT
    s.sku_id,
    s.spec_item_id,
    s.spec_value_id,
    s.spec_name,
    s.spec_value
FROM product_sku_spec s
WHERE s.sku_id = 20001
ORDER BY s.spec_item_id ASC;
```

返回结果可以拼接为`颜色:红色;尺码:XL`，也可以在创建SKU时提前写入`product_sku.spec_text`，下单时直接读取快照字段。

#### 根据SKU编码查询商品

外部系统、订单系统、库存系统经常通过SKU编码查询商品信息。

```sql
SELECT
    sku.id AS sku_id,
    sku.sku_code,
    sku.sku_name,
    sku.spec_text,
    sku.sale_price,
    sku.stock_unit,
    spu.id AS spu_id,
    spu.spu_code,
    spu.spu_name,
    spu.category_id,
    spu.brand_id
FROM product_sku sku
JOIN product_spu spu ON spu.id = sku.spu_id
WHERE sku.sku_code = 'SKU202601010001'
  AND sku.deleted = 0
  AND spu.deleted = 0;
```

该查询适合订单创建、价格校验、商品快照生成等业务。

#### 根据条码查询SKU

门店扫码、仓储入库、POS收银等场景通常通过条码查询SKU。

```sql
SELECT
    id,
    spu_id,
    sku_code,
    sku_name,
    spec_text,
    barcode,
    sale_price,
    stock_unit,
    status
FROM product_sku
WHERE barcode = '6900000000012'
  AND deleted = 0
LIMIT 1;
```

如果一个条码可能对应多个包装单位，需要重新设计条码模型，不建议仅依赖`product_sku.barcode`单字段。

#### 查询指定SPU下的最低销售价

商品列表页经常展示“起售价”，可以从SKU表中聚合得到。

```sql
SELECT
    spu_id,
    MIN(sale_price) AS min_sale_price,
    MAX(sale_price) AS max_sale_price
FROM product_sku
WHERE spu_id = 10001
  AND status = 1
  AND deleted = 0
GROUP BY spu_id;
```

高并发列表页不建议每次实时聚合SKU价格，可以将最低价、最高价冗余到SPU表中，并在SKU价格变更时同步更新。

### 常用写入

常用写入包括创建SPU、创建规格、生成SKU、上下架商品、修改价格和软删除商品。写入时需要保证SPU、SKU、规格项、规格值之间的一致性。

#### 创建SPU

创建SPU时先写入商品公共信息，此时商品可以是草稿状态。

```sql
INSERT INTO product_spu (
    spu_code,
    spu_name,
    category_id,
    brand_id,
    main_image,
    selling_point,
    detail_html,
    status,
    sort_no,
    created_by,
    updated_by
) VALUES (
    'SPU202601010001',
    '基础款纯棉T恤',
    10001,
    20001,
    'https://static.example.com/product/tshirt-main.png',
    '亲肤透气，日常百搭',
    '<p>基础款纯棉T恤详情</p>',
    0,
    100,
    1,
    1
);
```

创建后建议先维护规格和SKU，确认可销售信息完整后再上架。

#### 创建规格项和规格值

规格项和值建议在同一个事务中写入，避免只创建了规格项但没有规格值。

```sql
INSERT INTO product_spec_item (
    spu_id,
    spec_name,
    sort_no
) VALUES
    (10001, '颜色', 1),
    (10001, '尺码', 2);

INSERT INTO product_spec_value (
    spu_id,
    spec_item_id,
    spec_value,
    sort_no
) VALUES
    (10001, 30001, '红色', 1),
    (10001, 30001, '黑色', 2),
    (10001, 30002, 'L', 1),
    (10001, 30002, 'XL', 2);
```

实际应用中，`spec_item_id`应使用上一条插入后返回的主键ID，不建议硬编码。

#### 创建SKU和SKU规格关系

SKU创建时需要同时写入SKU主表和SKU规格关系表。一个SKU必须包含每个必选规格项的一个规格值。

```sql
INSERT INTO product_sku (
    spu_id,
    sku_code,
    sku_name,
    spec_text,
    barcode,
    sale_price,
    market_price,
    cost_price,
    stock_unit,
    status,
    created_by,
    updated_by
) VALUES (
    10001,
    'SKU202601010001',
    '基础款纯棉T恤 红色 L',
    '颜色:红色;尺码:L',
    '6900000000012',
    99.00,
    129.00,
    55.00,
    '件',
    1,
    1,
    1
);

INSERT INTO product_sku_spec (
    sku_id,
    spu_id,
    spec_item_id,
    spec_value_id,
    spec_name,
    spec_value
) VALUES
    (20001, 10001, 30001, 40001, '颜色', '红色'),
    (20001, 10001, 30002, 40003, '尺码', 'L');
```

写入SKU前需要校验同一个SPU下的规格组合不能重复。可以通过应用层将规格值ID排序后生成组合键，也可以增加额外字段保存规格组合编码。

#### 修改SKU价格

SKU价格变更应只影响后续订单，不应修改历史订单价格。订单明细中应保存下单时的商品名称、SKU名称、规格、单价等快照。

```sql
UPDATE product_sku
SET sale_price = 109.00,
    updated_by = 1,
    updated_at = NOW()
WHERE id = 20001
  AND deleted = 0;
```

如果SPU表冗余了最低价和最高价，修改SKU价格后需要同步刷新SPU价格区间。

#### 上架商品

商品上架前应校验至少存在一个启用SKU，并且SKU价格、规格、状态完整。

```sql
UPDATE product_spu
SET status = 1,
    updated_by = 1,
    updated_at = NOW()
WHERE id = 10001
  AND deleted = 0
  AND EXISTS (
      SELECT 1
      FROM product_sku
      WHERE product_sku.spu_id = product_spu.id
        AND product_sku.status = 1
        AND product_sku.deleted = 0
  );
```

上架校验建议优先放在应用层实现，SQL中的`EXISTS`可以作为最后一道保护。

#### 下架商品

下架SPU通常表示商品不再允许新增下单，但不影响历史订单、售后、结算等数据。

```sql
UPDATE product_spu
SET status = 2,
    updated_by = 1,
    updated_at = NOW()
WHERE id = 10001
  AND deleted = 0;
```

如果业务要求下架SPU后所有SKU不可售，可以同步禁用SKU。

```sql
UPDATE product_sku
SET status = 0,
    updated_by = 1,
    updated_at = NOW()
WHERE spu_id = 10001
  AND deleted = 0;
```

#### 软删除商品

商品被订单引用后不建议物理删除。可以使用软删除保留历史关联关系。

```sql
UPDATE product_spu
SET deleted = 1,
    updated_by = 1,
    updated_at = NOW()
WHERE id = 10001
  AND deleted = 0;

UPDATE product_sku
SET deleted = 1,
    updated_by = 1,
    updated_at = NOW()
WHERE spu_id = 10001
  AND deleted = 0;
```

软删除前需要检查是否存在未完成订单、未完成库存流水、未完成售后单等关联业务。

### 常见问题

商品-SPU-SKU模型在实际业务中容易出现职责边界不清、规格组合重复、价格库存混放、历史订单受商品变更影响等问题。

| 问题                 | 原因                           | 建议                                  |
| -------------------- | ------------------------------ | ------------------------------------- |
| SPU和SKU字段混乱     | 没有区分展示层和交易层         | SPU放公共展示信息，SKU放可交易信息    |
| SKU规格组合重复      | 缺少规格组合唯一校验           | 应用层生成规格组合键并校验唯一        |
| 商品改名影响历史订单 | 订单明细直接关联商品实时字段   | 订单明细保存商品快照                  |
| 商品列表查询慢       | 列表页实时聚合SKU价格或库存    | 将最低价、销量等字段冗余到SPU或读模型 |
| 条码不唯一           | 多包装、多单位共用条码设计不清 | 条码复杂场景应拆出独立条码表          |
| 删除商品导致订单异常 | 物理删除商品基础资料           | 使用软删除，订单读取快照字段          |
| 规格名称变更影响展示 | SKU规格只关联规格值ID          | SKU规格关系表保存规格名称和值快照     |

一个常见误区是把库存数量直接放在`product_sku`表中。对于简单系统可以这样做，但中大型系统建议拆分库存模型，由库存表和库存流水表负责库存余额、冻结库存、出入库记录等信息，SKU表只保存商品销售属性。

另一个常见误区是把SKU价格当作订单价格的唯一来源。正确做法是下单时读取SKU当前价格，生成订单后将价格写入订单明细快照。后续SKU调价不应影响历史订单。

### 总结

商品-SPU-SKU模型的核心是将商品公共信息和具体可交易规格拆分管理。SPU负责商品展示、分类、品牌、详情等公共属性，SKU负责规格组合、价格、条码、销售单位等交易属性。

建模时建议遵循以下原则：

| 原则             | 说明                                      |
| ---------------- | ----------------------------------------- |
| SPU负责展示      | 商品名称、类目、品牌、详情、主图等放在SPU |
| SKU负责交易      | 价格、条码、规格组合、销售单位等放在SKU   |
| 规格独立建模     | 规格项、规格值、SKU规格关系单独维护       |
| 历史业务使用快照 | 订单、售后、结算不要依赖商品实时字段      |
| 删除优先软删除   | 避免破坏历史订单和库存关联                |
| 高频查询适度冗余 | 起售价、销量、主SKU等字段可以按需冗余     |

该模型是订单、支付、库存、营销和搜索等后续模型的基础。只要SPU、SKU、规格、价格、快照职责划分清晰，后续交易链路会更稳定，也更容易扩展。



## 订单-订单明细模型

订单-订单明细模型用于描述交易系统中的主从订单结构。订单主表记录一次交易的整体信息，例如订单号、用户、金额、状态、收货信息；订单明细表记录本次交易中购买的具体商品、SKU、数量、价格和快照信息。

### 适用场景

订单-订单明细模型适合用于一笔订单中包含一个或多个商品明细的业务场景。订单主表负责表达“这是一笔什么交易”，订单明细表负责表达“这笔交易买了哪些商品”。

常见适用场景包括：

| 场景          | 说明                                                 |
| ------------- | ---------------------------------------------------- |
| 电商订单      | 一个订单包含多个商品SKU，每个SKU生成一条订单明细     |
| B2B订货       | 一个客户一次提交多个商品的订货需求                   |
| 门店收银      | 一张小票对应一个订单，每个商品行对应一条明细         |
| 外卖/生鲜订单 | 订单主表保存配送、支付、用户信息，明细表保存商品快照 |
| 虚拟商品订单  | 一个订单可以包含多个会员、服务、权益或课程明细       |
| 分销订单      | 订单明细可用于计算不同商品的佣金、利润和结算金额     |

该模型不适合将所有商品信息直接堆在订单主表中。只要一笔订单可能包含多个商品、多个SKU、多个价格行或多个可结算对象，就应该拆分订单主表和订单明细表。

### 建模结构

订单-订单明细模型通常由订单主表和订单明细表组成。订单主表保存交易整体信息，订单明细表保存商品行信息。订单主表与订单明细表是一对多关系。

核心关系如下：

| 表名               | 说明       | 关系                     |
| ------------------ | ---------- | ------------------------ |
| `trade_order`      | 订单主表   | 一个订单对应多条订单明细 |
| `trade_order_item` | 订单明细表 | 每条明细归属于一个订单   |

推荐的业务关系为：

```text
trade_order 1 —— N trade_order_item
```

订单主表通常承载以下信息：

| 信息类型 | 说明                                             |
| -------- | ------------------------------------------------ |
| 订单身份 | 订单ID、订单号、用户ID、租户ID                   |
| 订单金额 | 商品总金额、优惠金额、运费、应付金额、实付金额   |
| 订单状态 | 待支付、已支付、已发货、已完成、已取消、已关闭   |
| 支付信息 | 支付状态、支付时间、支付单号                     |
| 收货信息 | 收货人、手机号、地址                             |
| 生命周期 | 下单时间、支付时间、发货时间、完成时间、取消时间 |

订单明细表通常承载以下信息：

| 信息类型 | 说明                                           |
| -------- | ---------------------------------------------- |
| 商品身份 | SPU ID、SKU ID、SPU编码、SKU编码               |
| 商品快照 | 商品名称、SKU名称、规格描述、商品图片          |
| 价格数量 | 单价、购买数量、商品总金额、优惠金额、实付金额 |
| 售后状态 | 是否退款、退款数量、售后状态                   |
| 结算信息 | 成本价、佣金、供应商、利润等扩展字段           |

订单模型最重要的设计原则是“交易快照不可变”。下单成功后，订单明细中的商品名称、SKU名称、规格、价格、图片等信息应保存为快照，不应依赖商品表的实时字段。

### 字段设计

字段设计需要满足下单、支付、发货、退款、对账、售后、统计等交易链路。金额字段建议使用`DECIMAL`，状态字段建议使用`TINYINT`，订单号建议使用全局唯一业务单号。

订单主表用于保存订单整体信息。

```sql
CREATE TABLE trade_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    tenant_id BIGINT UNSIGNED DEFAULT NULL COMMENT '租户ID',
    order_source TINYINT NOT NULL DEFAULT 1 COMMENT '订单来源：1商城，2后台，3门店，4接口',
    order_type TINYINT NOT NULL DEFAULT 1 COMMENT '订单类型：1普通订单，2虚拟订单，3预售订单',
    order_status TINYINT NOT NULL DEFAULT 10 COMMENT '订单状态：10待支付，20已支付，30已发货，40已完成，50已取消，60已关闭',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0未支付，1部分支付，2已支付，3已退款',
    delivery_status TINYINT NOT NULL DEFAULT 0 COMMENT '发货状态：0未发货，1部分发货，2已发货，3已收货',
    product_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '商品总金额',
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    freight_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '运费金额',
    payable_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '应付金额',
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额',
    receiver_name VARCHAR(64) DEFAULT NULL COMMENT '收货人姓名',
    receiver_mobile VARCHAR(32) DEFAULT NULL COMMENT '收货人手机号',
    receiver_province VARCHAR(64) DEFAULT NULL COMMENT '收货省份',
    receiver_city VARCHAR(64) DEFAULT NULL COMMENT '收货城市',
    receiver_district VARCHAR(64) DEFAULT NULL COMMENT '收货区县',
    receiver_address VARCHAR(500) DEFAULT NULL COMMENT '详细收货地址',
    buyer_remark VARCHAR(500) DEFAULT NULL COMMENT '买家备注',
    seller_remark VARCHAR(500) DEFAULT NULL COMMENT '卖家备注',
    paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
    shipped_at DATETIME DEFAULT NULL COMMENT '发货时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    canceled_at DATETIME DEFAULT NULL COMMENT '取消时间',
    cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
```

订单明细表用于保存每个商品行的交易快照。

```sql
CREATE TABLE trade_order_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    spu_id BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID',
    sku_id BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    spu_code VARCHAR(64) NOT NULL COMMENT 'SPU编码快照',
    sku_code VARCHAR(64) NOT NULL COMMENT 'SKU编码快照',
    spu_name VARCHAR(200) NOT NULL COMMENT '商品名称快照',
    sku_name VARCHAR(200) NOT NULL COMMENT 'SKU名称快照',
    spec_text VARCHAR(500) DEFAULT NULL COMMENT '规格描述快照',
    product_image VARCHAR(500) DEFAULT NULL COMMENT '商品图片快照',
    unit_price DECIMAL(12,2) NOT NULL COMMENT '商品单价',
    quantity INT UNSIGNED NOT NULL COMMENT '购买数量',
    product_amount DECIMAL(12,2) NOT NULL COMMENT '商品金额：单价 * 数量',
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '明细优惠金额',
    payable_amount DECIMAL(12,2) NOT NULL COMMENT '明细应付金额',
    refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '明细已退款金额',
    refund_quantity INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已退款数量',
    item_status TINYINT NOT NULL DEFAULT 10 COMMENT '明细状态：10正常，20已取消，30部分退款，40已退款',
    cost_price DECIMAL(12,2) DEFAULT NULL COMMENT '成本价快照',
    supplier_id BIGINT UNSIGNED DEFAULT NULL COMMENT '供应商ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';
```

字段设计建议如下：

| 字段类型   | 设计建议                                        |
| ---------- | ----------------------------------------------- |
| 订单号     | 使用`order_no`作为业务单号，必须全局唯一        |
| 主键       | 使用`BIGINT UNSIGNED`，支持自增ID或分布式ID     |
| 金额       | 使用`DECIMAL(12,2)`，不使用`FLOAT`或`DOUBLE`    |
| 数量       | 使用`INT UNSIGNED`，避免负数数量                |
| 状态       | 使用`TINYINT`，通过字典或枚举维护含义           |
| 商品快照   | 商品名称、SKU名称、规格、图片、单价必须保存快照 |
| 用户字段   | 主表和明细表都建议保存`user_id`，便于明细级查询 |
| 订单号冗余 | 明细表保存`order_no`，便于对账、导出和排查问题  |
| 软删除     | 订单数据通常不建议物理删除，使用`deleted`标识   |

订单金额建议满足以下关系：

```text
product_amount = SUM(order_item.product_amount)

payable_amount = product_amount - discount_amount + freight_amount

paid_amount <= payable_amount

refund_amount <= paid_amount
```

订单明细金额建议满足以下关系：

```text
order_item.product_amount = order_item.unit_price * order_item.quantity

order_item.payable_amount = order_item.product_amount - order_item.discount_amount

order_item.refund_quantity <= order_item.quantity

order_item.refund_amount <= order_item.payable_amount
```

金额计算应在应用层使用定点数类型，例如Java中的`BigDecimal`，并统一舍入规则。

### 索引设计

索引设计应围绕订单列表、订单详情、订单号查询、用户订单查询、状态筛选、支付回调、售后查询和对账导出等访问路径展开。

订单主表推荐索引如下：

```sql
ALTER TABLE trade_order
    ADD UNIQUE KEY uk_order_no (order_no),
    ADD KEY idx_user_status_created (user_id, order_status, created_at),
    ADD KEY idx_status_created (order_status, created_at),
    ADD KEY idx_pay_status_created (pay_status, created_at),
    ADD KEY idx_created_deleted (created_at, deleted),
    ADD KEY idx_tenant_created (tenant_id, created_at);
```

订单明细表推荐索引如下：

```sql
ALTER TABLE trade_order_item
    ADD KEY idx_order_id (order_id),
    ADD KEY idx_order_no (order_no),
    ADD KEY idx_user_created (user_id, created_at),
    ADD KEY idx_sku_created (sku_id, created_at),
    ADD KEY idx_spu_created (spu_id, created_at),
    ADD KEY idx_item_status_created (item_status, created_at);
```

索引设计说明：

| 索引                      | 作用                                             |
| ------------------------- | ------------------------------------------------ |
| `uk_order_no`             | 保证订单号唯一，支持支付、售后、对账按订单号查询 |
| `idx_user_status_created` | 支持用户订单列表按状态分页查询                   |
| `idx_status_created`      | 支持后台按订单状态查询                           |
| `idx_pay_status_created`  | 支持未支付超时关闭、支付状态扫描                 |
| `idx_created_deleted`     | 支持按时间范围导出和归档                         |
| `idx_tenant_created`      | 支持多租户订单隔离查询                           |
| `idx_order_id`            | 支持订单详情加载明细                             |
| `idx_order_no`            | 支持通过订单号查询明细                           |
| `idx_sku_created`         | 支持按SKU统计销量或查询销售记录                  |
| `idx_item_status_created` | 支持售后、退款、异常明细筛选                     |

如果订单表数据量较大，后台订单列表不要只依赖单列状态索引。更常见的查询条件是状态加时间范围，因此组合索引通常比单列状态索引更有效。

### 常用查询

常用查询需要围绕前台用户订单、后台订单管理、订单详情、支付回调、售后处理、销售统计等场景设计。订单主表适合做整体筛选，订单明细表适合做商品维度分析。

#### 查询用户订单列表

用户订单列表通常按用户ID、订单状态、创建时间分页查询。

```sql
SELECT
    id,
    order_no,
    order_status,
    pay_status,
    delivery_status,
    product_amount,
    discount_amount,
    freight_amount,
    payable_amount,
    paid_amount,
    refund_amount,
    created_at
FROM trade_order
WHERE user_id = 10001
  AND order_status = 20
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;
```

如果用户订单列表需要展示商品缩略信息，可以先查订单主表分页，再根据订单ID批量查询明细，避免直接大表JOIN后分页不准确。

```sql
SELECT
    id,
    order_id,
    order_no,
    sku_id,
    spu_name,
    sku_name,
    spec_text,
    product_image,
    unit_price,
    quantity,
    payable_amount,
    item_status
FROM trade_order_item
WHERE order_id IN (90001, 90002, 90003)
  AND deleted = 0
ORDER BY order_id DESC, id ASC;
```

#### 查询订单详情

订单详情通常包含订单主表信息和订单明细信息。可以分两次查询，便于控制数据结构和避免重复字段。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_source,
    order_type,
    order_status,
    pay_status,
    delivery_status,
    product_amount,
    discount_amount,
    freight_amount,
    payable_amount,
    paid_amount,
    refund_amount,
    receiver_name,
    receiver_mobile,
    receiver_province,
    receiver_city,
    receiver_district,
    receiver_address,
    buyer_remark,
    seller_remark,
    paid_at,
    shipped_at,
    completed_at,
    canceled_at,
    cancel_reason,
    created_at
FROM trade_order
WHERE order_no = 'ORD202601010001'
  AND deleted = 0;
```

查询订单明细：

```sql
SELECT
    id,
    order_id,
    order_no,
    spu_id,
    sku_id,
    spu_code,
    sku_code,
    spu_name,
    sku_name,
    spec_text,
    product_image,
    unit_price,
    quantity,
    product_amount,
    discount_amount,
    payable_amount,
    refund_amount,
    refund_quantity,
    item_status
FROM trade_order_item
WHERE order_no = 'ORD202601010001'
  AND deleted = 0
ORDER BY id ASC;
```

#### 查询待支付订单

待支付订单通常用于用户继续支付、超时关闭任务、后台异常订单排查。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_status,
    payable_amount,
    created_at
FROM trade_order
WHERE order_status = 10
  AND pay_status = 0
  AND deleted = 0
ORDER BY created_at ASC
LIMIT 100;
```

如果业务要求订单30分钟未支付自动关闭，可以按创建时间筛选。

```sql
SELECT
    id,
    order_no,
    user_id,
    payable_amount,
    created_at
FROM trade_order
WHERE order_status = 10
  AND pay_status = 0
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE)
  AND deleted = 0
ORDER BY created_at ASC
LIMIT 100;
```

#### 根据订单号查询支付校验信息

支付前或支付回调时，通常需要根据订单号查询订单金额和状态。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_status,
    payable_amount,
    paid_amount
FROM trade_order
WHERE order_no = 'ORD202601010001'
  AND deleted = 0
LIMIT 1;
```

支付回调处理时必须校验订单存在、订单未支付、回调金额等于应付金额，并且回调处理需要保证幂等。

#### 查询SKU销售明细

商品销售分析、售后分析、供应商结算等场景通常按SKU查询订单明细。

```sql
SELECT
    id,
    order_no,
    user_id,
    sku_id,
    sku_code,
    sku_name,
    spec_text,
    unit_price,
    quantity,
    product_amount,
    payable_amount,
    created_at
FROM trade_order_item
WHERE sku_id = 20001
  AND item_status IN (10, 30)
  AND deleted = 0
ORDER BY created_at DESC
LIMIT 100;
```

如果只需要统计销量，可以使用聚合查询。

```sql
SELECT
    sku_id,
    sku_code,
    sku_name,
    SUM(quantity - refund_quantity) AS sale_quantity,
    SUM(payable_amount - refund_amount) AS sale_amount
FROM trade_order_item
WHERE sku_id = 20001
  AND item_status IN (10, 30)
  AND deleted = 0
GROUP BY sku_id, sku_code, sku_name;
```

大数据量场景不建议频繁在订单明细表上做实时聚合，应使用统计汇总表或离线数仓。

#### 查询时间范围内订单

后台导出、财务对账、运营统计通常按时间范围查询订单。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_status,
    product_amount,
    discount_amount,
    freight_amount,
    payable_amount,
    paid_amount,
    refund_amount,
    created_at,
    paid_at
FROM trade_order
WHERE created_at >= '2026-01-01 00:00:00'
  AND created_at < '2026-02-01 00:00:00'
  AND deleted = 0
ORDER BY created_at ASC, id ASC
LIMIT 1000;
```

导出场景建议使用基于游标的分页方式，避免大偏移量`OFFSET`导致性能下降。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    payable_amount,
    paid_amount,
    created_at
FROM trade_order
WHERE id > 900000
  AND created_at >= '2026-01-01 00:00:00'
  AND created_at < '2026-02-01 00:00:00'
  AND deleted = 0
ORDER BY id ASC
LIMIT 1000;
```

### 常用写入

订单写入通常涉及订单主表、订单明细表、库存扣减、优惠计算、支付单创建等多个步骤。订单创建必须放在事务中处理，保证订单主表和订单明细表一致。

#### 创建订单

创建订单时应先根据SKU实时信息生成商品快照，再计算金额，最后写入订单主表和订单明细表。

```sql
INSERT INTO trade_order (
    order_no,
    user_id,
    tenant_id,
    order_source,
    order_type,
    order_status,
    pay_status,
    delivery_status,
    product_amount,
    discount_amount,
    freight_amount,
    payable_amount,
    paid_amount,
    refund_amount,
    receiver_name,
    receiver_mobile,
    receiver_province,
    receiver_city,
    receiver_district,
    receiver_address,
    buyer_remark,
    created_by,
    updated_by
) VALUES (
    'ORD202601010001',
    10001,
    1,
    1,
    1,
    10,
    0,
    0,
    198.00,
    10.00,
    8.00,
    196.00,
    0.00,
    0.00,
    '张三',
    '13800000000',
    '浙江省',
    '杭州市',
    '西湖区',
    '文三路100号',
    '请尽快发货',
    10001,
    10001
);
```

写入订单明细：

```sql
INSERT INTO trade_order_item (
    order_id,
    order_no,
    user_id,
    spu_id,
    sku_id,
    spu_code,
    sku_code,
    spu_name,
    sku_name,
    spec_text,
    product_image,
    unit_price,
    quantity,
    product_amount,
    discount_amount,
    payable_amount,
    refund_amount,
    refund_quantity,
    item_status,
    cost_price,
    supplier_id
) VALUES (
    90001,
    'ORD202601010001',
    10001,
    10001,
    20001,
    'SPU202601010001',
    'SKU202601010001',
    '基础款纯棉T恤',
    '基础款纯棉T恤 红色 L',
    '颜色:红色;尺码:L',
    'https://static.example.com/product/tshirt-main.png',
    99.00,
    2,
    198.00,
    10.00,
    188.00,
    0.00,
    0,
    10,
    55.00,
    30001
);
```

创建订单时不建议只保存`sku_id`，否则商品名称、规格、图片、价格变更后，历史订单展示会被污染。

#### 支付成功更新订单

支付成功后，应更新订单主表的支付状态、订单状态、实付金额和支付时间。

```sql
UPDATE trade_order
SET order_status = 20,
    pay_status = 2,
    paid_amount = payable_amount,
    paid_at = NOW(),
    updated_at = NOW()
WHERE order_no = 'ORD202601010001'
  AND order_status = 10
  AND pay_status = 0
  AND deleted = 0;
```

该SQL天然具备一定幂等能力。只有待支付且未支付的订单才能更新成功。如果影响行数为0，需要进一步查询订单当前状态，判断是重复回调、订单不存在还是状态异常。

#### 取消待支付订单

用户主动取消或超时未支付关闭订单时，需要更新订单状态和取消原因。

```sql
UPDATE trade_order
SET order_status = 50,
    canceled_at = NOW(),
    cancel_reason = '用户主动取消',
    updated_at = NOW()
WHERE order_no = 'ORD202601010001'
  AND order_status = 10
  AND pay_status = 0
  AND deleted = 0;
```

如果订单创建时已经冻结库存，取消订单时需要释放冻结库存。库存释放应与订单取消放在同一事务或使用可靠消息保证最终一致。

#### 发货订单

发货后应更新发货状态和订单状态。简单场景可以在订单主表直接保存发货状态，复杂场景建议拆分物流包裹表。

```sql
UPDATE trade_order
SET order_status = 30,
    delivery_status = 2,
    shipped_at = NOW(),
    updated_at = NOW()
WHERE order_no = 'ORD202601010001'
  AND order_status = 20
  AND pay_status = 2
  AND deleted = 0;
```

如果一个订单支持拆单发货、多个包裹、部分发货，则不建议只依赖`trade_order.shipped_at`，应建立订单发货单或物流包裹模型。

#### 完成订单

用户确认收货或系统自动确认收货后，订单进入完成状态。

```sql
UPDATE trade_order
SET order_status = 40,
    delivery_status = 3,
    completed_at = NOW(),
    updated_at = NOW()
WHERE order_no = 'ORD202601010001'
  AND order_status = 30
  AND delivery_status = 2
  AND deleted = 0;
```

完成订单后通常会触发积分、佣金、结算、评价等后续业务，不建议在订单更新SQL中直接耦合所有业务逻辑。

#### 更新订单明细退款信息

发生退款后，需要同步订单明细的退款数量、退款金额和明细状态。

```sql
UPDATE trade_order_item
SET refund_quantity = refund_quantity + 1,
    refund_amount = refund_amount + 99.00,
    item_status = CASE
        WHEN refund_quantity + 1 >= quantity THEN 40
        ELSE 30
    END,
    updated_at = NOW()
WHERE id = 91001
  AND refund_quantity + 1 <= quantity
  AND deleted = 0;
```

同时需要更新订单主表的退款金额和支付状态。

```sql
UPDATE trade_order
SET refund_amount = refund_amount + 99.00,
    pay_status = CASE
        WHEN refund_amount + 99.00 >= paid_amount THEN 3
        ELSE pay_status
    END,
    updated_at = NOW()
WHERE order_no = 'ORD202601010001'
  AND paid_amount >= refund_amount + 99.00
  AND deleted = 0;
```

退款更新应以退款单为驱动，订单表和订单明细表只保存退款后的聚合结果。

### 常见问题

订单-订单明细模型在业务复杂后容易出现金额不一致、状态流转混乱、历史快照缺失、支付回调重复处理等问题。

| 问题                     | 原因                                       | 建议                                       |
| ------------------------ | ------------------------------------------ | ------------------------------------------ |
| 历史订单商品名称变化     | 订单明细只保存`sku_id`，展示时实时查商品表 | 订单明细必须保存商品快照                   |
| 订单金额和明细金额不一致 | 金额计算分散在多个地方                     | 统一金额计算规则，并在创建订单时固化金额   |
| 支付回调重复导致重复更新 | 缺少幂等条件                               | 更新时带上`order_status`和`pay_status`条件 |
| 大订单列表查询慢         | 订单表数据量大，索引不匹配                 | 按用户、状态、时间设计组合索引             |
| 订单详情JOIN结果重复     | 主表和明细表一对多直接JOIN                 | 详情页可以主表和明细分开查询               |
| 退款后订单状态混乱       | 退款状态和订单状态没有边界                 | 退款状态建议由退款单维护，订单保存聚合结果 |
| 物理删除订单影响对账     | 删除后财务和售后无法追踪                   | 订单数据应软删除或归档，不建议物理删除     |
| 明细表缺少`order_no`     | 排查和对账必须先查主表                     | 明细表冗余`order_no`提高可追溯性           |

一个常见误区是认为订单主表的金额可以随时根据订单明细重新计算。实际交易系统中，订单金额在下单时就应固化，后续退款、优惠、支付、结算都应基于已固化金额进行状态变更，而不是反复读取商品当前价格重新计算。

另一个常见误区是把订单状态设计得过细。订单状态应表达主流程状态，例如待支付、已支付、已发货、已完成、已取消。支付状态、发货状态、退款状态可以独立字段表达，避免一个状态字段承载过多含义。

### 总结

订单-订单明细模型的核心是主从结构和交易快照。订单主表保存交易整体信息，订单明细表保存商品行信息。订单创建后，商品名称、规格、价格、图片等快照字段应保持稳定，避免商品基础资料变更影响历史订单。

建模时建议遵循以下原则：

| 原则           | 说明                                           |
| -------------- | ---------------------------------------------- |
| 主从分离       | 订单主表保存整体交易，订单明细表保存商品行     |
| 快照固化       | 商品、规格、价格、图片等交易字段必须保存快照   |
| 金额定点计算   | 金额字段使用`DECIMAL`，应用层使用`BigDecimal`  |
| 状态边界清晰   | 订单状态、支付状态、发货状态、退款状态分开表达 |
| 写入保证事务   | 创建订单时主表和明细表必须同事务写入           |
| 回调保证幂等   | 支付、退款、发货等外部回调必须带状态条件更新   |
| 查询避免大JOIN | 列表先查主表分页，再批量查询明细               |
| 数据保留可追溯 | 订单不建议物理删除，应软删除、归档或分区管理   |

该模型是支付单、退款单、库存流水、账户流水、结算单等后续模型的核心来源。订单模型设计稳定后，交易链路中的支付、退款、发货、售后和财务对账都会更容易扩展。



## 支付单模型

支付单模型用于描述订单发起支付后的支付请求、支付状态、支付渠道、支付金额、第三方交易号和支付完成时间。订单表示业务交易，支付单表示资金支付动作。一个订单可以对应一次或多次支付尝试，但通常只允许一笔支付单最终成功。

### 适用场景

支付单模型适合用于订单和支付渠道之间需要解耦的业务场景。订单系统只关心业务交易是否已支付，支付系统负责对接微信、支付宝、银行卡、余额、积分、企业支付等具体支付渠道。

常见适用场景包括：

| 场景         | 说明                                                       |
| ------------ | ---------------------------------------------------------- |
| 电商订单支付 | 用户提交订单后生成支付单，再通过微信、支付宝等渠道完成支付 |
| 多渠道支付   | 同一订单可以选择不同支付方式，但最终只允许一个支付单成功   |
| 支付重试     | 用户第一次支付失败或取消后，可以重新生成支付单继续支付     |
| 支付回调     | 第三方支付平台异步通知支付结果，系统根据支付单完成幂等更新 |
| 财务对账     | 根据支付单和第三方交易号进行渠道对账                       |
| 超时关闭     | 支付单超过有效期后关闭，避免长期处于待支付状态             |

不建议将支付渠道字段、第三方交易号、支付回调状态全部直接写在订单主表中。订单主表表达业务状态，支付单表达支付状态。两者分离后，支付失败、重新支付、渠道切换、支付对账会更清晰。

### 建模结构

支付单模型通常以支付单表为核心。订单主表和支付单表之间通常是一对多关系，因为一个订单可能产生多次支付尝试，但业务上只能有一笔支付单最终支付成功。

核心关系如下：

| 表名                       | 说明           | 关系                       |
| -------------------------- | -------------- | -------------------------- |
| `trade_order`              | 订单主表       | 一个订单可以关联多个支付单 |
| `trade_payment`            | 支付单表       | 每个支付单归属于一个订单   |
| `trade_payment_notify_log` | 支付回调日志表 | 记录第三方支付回调原始信息 |

推荐的业务关系为：

```text
trade_order 1 —— N trade_payment

trade_payment 1 —— N trade_payment_notify_log
```

支付单表主要负责保存支付请求和支付结果。支付回调日志表主要用于排查问题、审计回调内容和处理重复通知。

支付单与订单的职责边界如下：

| 模型       | 职责                                       | 不建议承载                       |
| ---------- | ------------------------------------------ | -------------------------------- |
| 订单主表   | 订单状态、应付金额、实付金额、业务生命周期 | 第三方原始回调、支付渠道请求参数 |
| 支付单表   | 支付金额、支付渠道、支付状态、第三方交易号 | 商品明细、收货地址、订单商品快照 |
| 回调日志表 | 保存支付渠道通知原文、处理状态、异常信息   | 业务订单金额计算、订单状态主流程 |

一个订单可以创建多个支付单，但需要通过状态约束保证同一订单最终只有一个支付单成功。支付成功后，应同步更新订单支付状态和实付金额。

### 字段设计

字段设计需要满足支付创建、支付跳转、异步回调、主动查询、超时关闭、对账和问题排查等场景。支付金额建议使用`DECIMAL`，支付状态建议使用`TINYINT`，支付单号建议使用全局唯一业务单号。

支付单表用于保存支付请求和支付结果。

```sql
CREATE TABLE trade_payment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    tenant_id BIGINT UNSIGNED DEFAULT NULL COMMENT '租户ID',
    pay_channel TINYINT NOT NULL COMMENT '支付渠道：1微信，2支付宝，3银行卡，4余额',
    pay_scene TINYINT NOT NULL DEFAULT 1 COMMENT '支付场景：1PC，2H5，3小程序，4APP，5扫码',
    pay_status TINYINT NOT NULL DEFAULT 10 COMMENT '支付状态：10待支付，20支付中，30支付成功，40支付失败，50已关闭',
    pay_amount DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '实际支付金额',
    refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额',
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    subject VARCHAR(200) NOT NULL COMMENT '支付标题',
    body VARCHAR(500) DEFAULT NULL COMMENT '支付描述',
    client_ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    third_trade_no VARCHAR(128) DEFAULT NULL COMMENT '第三方交易号',
    third_buyer_id VARCHAR(128) DEFAULT NULL COMMENT '第三方买家ID',
    third_app_id VARCHAR(128) DEFAULT NULL COMMENT '第三方应用ID',
    request_body JSON DEFAULT NULL COMMENT '支付请求参数',
    response_body JSON DEFAULT NULL COMMENT '支付响应参数',
    failure_code VARCHAR(64) DEFAULT NULL COMMENT '失败编码',
    failure_msg VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    expired_at DATETIME NOT NULL COMMENT '支付过期时间',
    paid_at DATETIME DEFAULT NULL COMMENT '支付完成时间',
    closed_at DATETIME DEFAULT NULL COMMENT '关闭时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单表';
```

支付回调日志表用于保存第三方支付平台的异步通知记录。

```sql
CREATE TABLE trade_payment_notify_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    pay_channel TINYINT NOT NULL COMMENT '支付渠道：1微信，2支付宝，3银行卡，4余额',
    notify_type TINYINT NOT NULL DEFAULT 1 COMMENT '通知类型：1支付成功，2支付失败，3关闭通知',
    third_trade_no VARCHAR(128) DEFAULT NULL COMMENT '第三方交易号',
    notify_body JSON NOT NULL COMMENT '回调原始内容',
    process_status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理，1处理成功，2处理失败，3重复通知',
    process_msg VARCHAR(500) DEFAULT NULL COMMENT '处理说明',
    notified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
    processed_at DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调日志表';
```

字段设计建议如下：

| 字段类型     | 设计建议                                              |
| ------------ | ----------------------------------------------------- |
| 支付单号     | 使用`payment_no`作为支付业务单号，必须全局唯一        |
| 订单号       | 支付单中冗余`order_no`，便于支付回调、对账和排查问题  |
| 支付渠道     | 使用`pay_channel`区分微信、支付宝、银行卡、余额等渠道 |
| 支付场景     | 使用`pay_scene`区分PC、H5、小程序、APP、扫码等场景    |
| 支付状态     | 使用独立`pay_status`，不要直接复用订单状态            |
| 第三方交易号 | 使用`third_trade_no`保存支付渠道返回的交易流水号      |
| 金额字段     | 使用`DECIMAL(12,2)`，支付、实付、退款金额分开保存     |
| 请求响应     | 可使用`JSON`保存渠道请求和响应，便于排查支付问题      |
| 过期时间     | 使用`expired_at`支持支付单超时关闭                    |
| 回调日志     | 保存回调原文，避免支付问题无法追踪                    |

支付单金额建议满足以下关系：

```text
pay_amount = 订单本次需要支付的金额

paid_amount <= pay_amount

refund_amount <= paid_amount
```

如果订单只允许一次全额支付，`pay_amount`通常等于订单`payable_amount`。如果业务支持部分支付、组合支付或余额抵扣，则需要额外设计支付拆分模型，不建议仅依赖一个支付单表处理所有复杂资金结构。

### 索引设计

索引设计应围绕支付单号查询、订单号查询、支付回调、支付状态扫描、超时关闭、渠道对账等访问路径展开。

支付单表推荐索引如下：

```sql
ALTER TABLE trade_payment
    ADD UNIQUE KEY uk_payment_no (payment_no),
    ADD UNIQUE KEY uk_channel_third_trade_no (pay_channel, third_trade_no),
    ADD KEY idx_order_no_status (order_no, pay_status),
    ADD KEY idx_order_id_status (order_id, pay_status),
    ADD KEY idx_user_created (user_id, created_at),
    ADD KEY idx_status_expired (pay_status, expired_at),
    ADD KEY idx_channel_status_created (pay_channel, pay_status, created_at),
    ADD KEY idx_tenant_created (tenant_id, created_at);
```

支付回调日志表推荐索引如下：

```sql
ALTER TABLE trade_payment_notify_log
    ADD KEY idx_payment_no_created (payment_no, created_at),
    ADD KEY idx_order_no_created (order_no, created_at),
    ADD KEY idx_channel_third_trade_no (pay_channel, third_trade_no),
    ADD KEY idx_process_status_created (process_status, created_at);
```

索引设计说明：

| 索引                         | 作用                                     |
| ---------------------------- | ---------------------------------------- |
| `uk_payment_no`              | 保证支付单号唯一，支持支付创建和回调定位 |
| `uk_channel_third_trade_no`  | 防止同一渠道第三方交易号重复入库         |
| `idx_order_no_status`        | 支持通过订单号查询支付单状态             |
| `idx_order_id_status`        | 支持订单详情加载支付记录                 |
| `idx_user_created`           | 支持用户支付记录查询                     |
| `idx_status_expired`         | 支持扫描超时未支付的支付单               |
| `idx_channel_status_created` | 支持按支付渠道和状态进行对账             |
| `idx_process_status_created` | 支持回调日志失败重试和异常排查           |

需要注意，`third_trade_no`在支付成功前通常为空。MySQL唯一索引允许多个`NULL`值，因此`uk_channel_third_trade_no`不会阻止多条待支付记录存在。如果业务要求支付成功后第三方交易号必须唯一，应在回调更新时校验该字段。

### 常用查询

常用查询需要围绕支付创建、支付回调、订单详情、超时关闭、支付对账、异常排查等场景设计。支付单查询通常通过`payment_no`、`order_no`、`third_trade_no`和支付状态进行定位。

#### 根据支付单号查询支付单

支付单号是支付系统内部最重要的定位字段。发起支付、查询支付结果、处理支付回调时都可以通过支付单号查询。

```sql
SELECT
    id,
    payment_no,
    order_id,
    order_no,
    user_id,
    pay_channel,
    pay_scene,
    pay_status,
    pay_amount,
    paid_amount,
    refund_amount,
    currency,
    subject,
    third_trade_no,
    expired_at,
    paid_at,
    closed_at,
    created_at
FROM trade_payment
WHERE payment_no = 'PAY202601010001'
  AND deleted = 0
LIMIT 1;
```

该查询适合支付详情页、支付状态轮询、支付结果页等场景。

#### 根据订单号查询支付记录

订单详情页或后台排查时，通常需要查询某个订单下的所有支付尝试记录。

```sql
SELECT
    id,
    payment_no,
    order_no,
    pay_channel,
    pay_scene,
    pay_status,
    pay_amount,
    paid_amount,
    third_trade_no,
    failure_code,
    failure_msg,
    expired_at,
    paid_at,
    closed_at,
    created_at
FROM trade_payment
WHERE order_no = 'ORD202601010001'
  AND deleted = 0
ORDER BY created_at DESC;
```

如果一个订单多次发起支付，该查询可以清晰展示每一次支付尝试的状态。

#### 查询订单成功支付单

订单支付成功后，通常只需要查询最终成功的支付单，用于订单详情、财务入账和售后退款。

```sql
SELECT
    id,
    payment_no,
    order_no,
    pay_channel,
    pay_status,
    pay_amount,
    paid_amount,
    refund_amount,
    third_trade_no,
    paid_at
FROM trade_payment
WHERE order_no = 'ORD202601010001'
  AND pay_status = 30
  AND deleted = 0
LIMIT 1;
```

业务上应保证同一个订单最终只有一笔支付单成功。如果查询出多条成功记录，说明支付幂等或状态约束存在问题。

#### 根据第三方交易号查询支付单

支付渠道回调或财务对账时，经常通过第三方交易号反查内部支付单。

```sql
SELECT
    id,
    payment_no,
    order_no,
    user_id,
    pay_channel,
    pay_status,
    pay_amount,
    paid_amount,
    third_trade_no,
    paid_at
FROM trade_payment
WHERE pay_channel = 1
  AND third_trade_no = 'WX2026010100019999'
  AND deleted = 0
LIMIT 1;
```

如果第三方回调中同时包含内部支付单号和第三方交易号，应优先通过内部支付单号定位，再校验第三方交易号、金额和渠道。

#### 查询超时未支付支付单

定时任务可以扫描超过有效期仍未支付的支付单，并将其关闭。

```sql
SELECT
    id,
    payment_no,
    order_no,
    pay_channel,
    pay_status,
    pay_amount,
    expired_at
FROM trade_payment
WHERE pay_status IN (10, 20)
  AND expired_at < NOW()
  AND deleted = 0
ORDER BY expired_at ASC
LIMIT 100;
```

扫描结果应逐条关闭支付单，并根据订单是否仍处于待支付状态决定是否关闭订单。

#### 查询支付渠道对账数据

财务对账通常按支付渠道、支付成功状态和支付时间范围查询。

```sql
SELECT
    payment_no,
    order_no,
    pay_channel,
    pay_amount,
    paid_amount,
    refund_amount,
    third_trade_no,
    paid_at
FROM trade_payment
WHERE pay_channel = 1
  AND pay_status = 30
  AND paid_at >= '2026-01-01 00:00:00'
  AND paid_at < '2026-01-02 00:00:00'
  AND deleted = 0
ORDER BY paid_at ASC, id ASC;
```

大数据量对账导出建议使用游标分页，避免使用大偏移量`OFFSET`。

```sql
SELECT
    id,
    payment_no,
    order_no,
    pay_channel,
    paid_amount,
    refund_amount,
    third_trade_no,
    paid_at
FROM trade_payment
WHERE id > 100000
  AND pay_channel = 1
  AND pay_status = 30
  AND paid_at >= '2026-01-01 00:00:00'
  AND paid_at < '2026-01-02 00:00:00'
  AND deleted = 0
ORDER BY id ASC
LIMIT 1000;
```

#### 查询支付回调日志

支付异常排查时，需要查看第三方平台回调内容和系统处理结果。

```sql
SELECT
    id,
    payment_no,
    order_no,
    pay_channel,
    notify_type,
    third_trade_no,
    notify_body,
    process_status,
    process_msg,
    notified_at,
    processed_at
FROM trade_payment_notify_log
WHERE payment_no = 'PAY202601010001'
ORDER BY created_at DESC;
```

如果支付单状态和第三方状态不一致，回调日志通常是排查问题的第一入口。

### 常用写入

支付单写入主要包括创建支付单、更新为支付中、处理支付成功回调、处理支付失败、关闭超时支付单和记录回调日志。支付成功更新必须保证幂等。

#### 创建支付单

用户提交支付请求时，系统根据订单生成支付单。创建前需要校验订单存在、订单未支付、金额正确、订单未关闭。

```sql
INSERT INTO trade_payment (
    payment_no,
    order_id,
    order_no,
    user_id,
    tenant_id,
    pay_channel,
    pay_scene,
    pay_status,
    pay_amount,
    paid_amount,
    refund_amount,
    currency,
    subject,
    body,
    client_ip,
    expired_at,
    created_by,
    updated_by
) VALUES (
    'PAY202601010001',
    90001,
    'ORD202601010001',
    10001,
    1,
    1,
    3,
    10,
    196.00,
    0.00,
    0.00,
    'CNY',
    '订单支付-ORD202601010001',
    '基础款纯棉T恤等商品',
    '127.0.0.1',
    DATE_ADD(NOW(), INTERVAL 30 MINUTE),
    10001,
    10001
);
```

同一个订单重复发起支付时，可以创建新的支付单，也可以复用未过期的待支付支付单。实际选择取决于支付渠道能力和业务策略。

#### 更新支付请求响应

调用第三方支付渠道后，可以将支付单更新为支付中，并保存请求和响应信息。

```sql
UPDATE trade_payment
SET pay_status = 20,
    request_body = JSON_OBJECT(
        'paymentNo', 'PAY202601010001',
        'orderNo', 'ORD202601010001',
        'amount', 196.00
    ),
    response_body = JSON_OBJECT(
        'codeUrl', 'https://pay.example.com/qrcode/xxx',
        'prepayId', 'PREPAY202601010001'
    ),
    updated_at = NOW()
WHERE payment_no = 'PAY202601010001'
  AND pay_status = 10
  AND deleted = 0;
```

如果第三方支付请求失败，可以记录失败原因并将支付单更新为失败状态。

#### 记录支付回调日志

收到第三方支付回调后，应先保存回调日志，再执行业务状态更新。这样即使后续处理失败，也能保留原始通知内容。

```sql
INSERT INTO trade_payment_notify_log (
    payment_no,
    order_no,
    pay_channel,
    notify_type,
    third_trade_no,
    notify_body,
    process_status,
    process_msg,
    notified_at
) VALUES (
    'PAY202601010001',
    'ORD202601010001',
    1,
    1,
    'WX2026010100019999',
    JSON_OBJECT(
        'trade_state', 'SUCCESS',
        'transaction_id', 'WX2026010100019999',
        'out_trade_no', 'PAY202601010001',
        'amount', 196.00
    ),
    0,
    '待处理',
    NOW()
);
```

支付回调日志建议保留完整原文，便于后续与渠道侧排查差异。

#### 支付成功更新支付单

支付成功时，需要使用支付单号、状态和金额条件进行幂等更新。只有待支付或支付中的支付单允许更新为成功。

```sql
UPDATE trade_payment
SET pay_status = 30,
    paid_amount = 196.00,
    third_trade_no = 'WX2026010100019999',
    third_buyer_id = 'OPENID202601010001',
    third_app_id = 'WXAPP202601',
    paid_at = NOW(),
    response_body = JSON_OBJECT(
        'trade_state', 'SUCCESS',
        'transaction_id', 'WX2026010100019999'
    ),
    updated_at = NOW()
WHERE payment_no = 'PAY202601010001'
  AND pay_status IN (10, 20)
  AND pay_amount = 196.00
  AND deleted = 0;
```

如果影响行数为1，表示本次回调完成了支付单状态变更。如果影响行数为0，需要查询当前支付单状态，判断是否为重复回调、金额不一致、支付单关闭或支付单不存在。

#### 支付成功更新订单

支付单成功后，需要同步更新订单主表。订单更新也必须带状态条件，避免重复回调导致重复更新。

```sql
UPDATE trade_order
SET order_status = 20,
    pay_status = 2,
    paid_amount = 196.00,
    paid_at = NOW(),
    updated_at = NOW()
WHERE order_no = 'ORD202601010001'
  AND order_status = 10
  AND pay_status = 0
  AND payable_amount = 196.00
  AND deleted = 0;
```

支付单成功和订单支付成功建议放在同一个本地事务中。如果支付模块和订单模块拆分为独立服务，则需要通过可靠消息或事务消息保证最终一致。

#### 标记回调处理结果

业务处理完成后，需要更新回调日志处理状态。

```sql
UPDATE trade_payment_notify_log
SET process_status = 1,
    process_msg = '支付成功回调处理完成',
    processed_at = NOW()
WHERE id = 80001
  AND process_status = 0;
```

重复回调可以记录为重复通知，不需要再次变更支付单和订单。

```sql
UPDATE trade_payment_notify_log
SET process_status = 3,
    process_msg = '重复支付回调，支付单已成功',
    processed_at = NOW()
WHERE id = 80002
  AND process_status = 0;
```

#### 支付失败更新支付单

支付渠道明确返回失败时，可以将支付单更新为失败状态，并记录失败编码和失败原因。

```sql
UPDATE trade_payment
SET pay_status = 40,
    failure_code = 'PAY_FAILED',
    failure_msg = '用户支付失败或渠道返回失败',
    updated_at = NOW()
WHERE payment_no = 'PAY202601010001'
  AND pay_status IN (10, 20)
  AND deleted = 0;
```

支付失败通常不直接关闭订单。用户可以重新选择支付渠道或重新发起支付。

#### 关闭超时支付单

支付单超过有效期后，应关闭支付单，避免后续继续支付。

```sql
UPDATE trade_payment
SET pay_status = 50,
    closed_at = NOW(),
    updated_at = NOW()
WHERE payment_no = 'PAY202601010001'
  AND pay_status IN (10, 20)
  AND expired_at < NOW()
  AND deleted = 0;
```

关闭支付单后，如果订单仍处于待支付状态，可以根据业务规则关闭订单或允许用户重新发起支付。

### 常见问题

支付单模型的常见问题集中在支付幂等、订单支付状态一致性、重复回调、第三方交易号唯一性、支付超时和对账差异等方面。

| 问题                     | 原因                                   | 建议                                           |
| ------------------------ | -------------------------------------- | ---------------------------------------------- |
| 支付回调重复更新订单     | 第三方回调可能重复通知                 | 更新支付单和订单时必须带状态条件               |
| 一个订单出现多笔成功支付 | 多个支付单并发回调成功                 | 通过业务锁、订单状态条件和成功支付唯一约束控制 |
| 支付金额和订单金额不一致 | 回调时未校验金额                       | 支付成功前必须校验`pay_amount`和回调金额       |
| 第三方交易号重复         | 回调重复或渠道异常                     | 对`pay_channel + third_trade_no`建立唯一约束   |
| 支付成功但订单未更新     | 支付单和订单更新不在同一事务或消息丢失 | 本地事务或可靠消息保证最终一致                 |
| 支付单长期待支付         | 缺少超时关闭任务                       | 使用`expired_at`定时扫描关闭                   |
| 支付失败直接取消订单     | 支付失败不等于订单取消                 | 支付失败只更新支付单，订单仍可重新支付         |
| 对账查不到原始回调       | 未保存支付回调原文                     | 使用回调日志表保存完整通知内容                 |

一个常见误区是将支付状态完全放在订单表中。这样在支付失败、重新支付、多渠道切换、支付回调排查时会非常混乱。订单表可以保存聚合后的支付状态，但支付过程本身应由支付单表记录。

另一个常见误区是认为支付回调只会通知一次。实际第三方支付平台通常会进行多次异步通知，因此支付回调必须按幂等逻辑设计。判断标准不是“是否收到过回调”，而是“当前支付单状态是否允许从待支付或支付中变更为支付成功”。

### 总结

支付单模型的核心是将订单业务交易和支付资金动作解耦。订单表示用户购买行为，支付单表示对某个订单发起的一次支付请求。一个订单可以有多次支付尝试，但通常只允许一笔支付单最终成功。

建模时建议遵循以下原则：

| 原则           | 说明                                               |
| -------------- | -------------------------------------------------- |
| 订单支付分离   | 订单保存业务交易状态，支付单保存支付过程和支付结果 |
| 支付单号唯一   | `payment_no`必须全局唯一，作为支付系统核心定位字段 |
| 回调必须幂等   | 支付成功更新必须带支付状态和金额条件               |
| 金额必须校验   | 回调金额、支付单金额、订单应付金额必须一致         |
| 第三方流水唯一 | `pay_channel + third_trade_no`应保证唯一           |
| 回调原文留存   | 支付通知内容应写入回调日志，便于审计和排查         |
| 超时及时关闭   | 使用`expired_at`扫描关闭长期未支付支付单           |
| 对账按渠道设计 | 支付渠道、支付时间、第三方交易号是对账核心字段     |

该模型是退款单、账户流水、财务对账和结算模型的基础。支付单设计清晰后，支付回调、支付重试、渠道对账和退款处理都会更容易维护。



## 退款单模型

退款单模型用于描述订单支付成功后发生的退款申请、退款审核、退款执行、退款回调、退款金额和退款明细。订单表示交易结果，支付单表示付款动作，退款单表示资金原路或非原路退回动作。退款单通常依赖订单、订单明细和支付单，是售后、财务对账、支付渠道回调中的关键模型。

### 适用场景

退款单模型适合用于已支付订单需要发生部分退款、整单退款、售后退款、差额退款或人工退款的业务场景。退款单不应简单地作为订单状态字段存在，而应作为独立业务单据保存完整的退款生命周期。

常见适用场景包括：

| 场景         | 说明                                         |
| ------------ | -------------------------------------------- |
| 整单退款     | 用户取消已支付订单后，退还订单全部实付金额   |
| 部分退款     | 一个订单中只退某几个商品或某部分数量         |
| 售后退款     | 退货退款、仅退款、换货失败后退款等售后场景   |
| 差额退款     | 运费退还、价格保护、人工补差等非完整商品退款 |
| 支付渠道退款 | 通过微信、支付宝、银行卡等渠道发起原路退款   |
| 财务对账     | 根据退款单号、第三方退款号、退款时间进行对账 |
| 退款回调     | 第三方支付平台异步通知退款结果               |

不建议只在订单表中增加`refund_amount`和`refund_status`来表达完整退款过程。订单表可以保存退款聚合结果，但退款申请、审核、执行、失败原因、第三方退款号、退款回调原文都应由退款单模型承载。

### 建模结构

退款单模型通常由退款单表、退款明细表、退款回调日志表组成。退款单表保存一次退款的整体信息，退款明细表保存本次退款关联的订单明细和退款数量，退款回调日志表保存第三方支付渠道的退款通知。

核心关系如下：

| 表名                      | 说明           | 关系                             |
| ------------------------- | -------------- | -------------------------------- |
| `trade_order`             | 订单主表       | 一个订单可以产生多笔退款单       |
| `trade_order_item`        | 订单明细表     | 一个订单明细可以被多次部分退款   |
| `trade_payment`           | 支付单表       | 一个支付单可以对应多笔退款单     |
| `trade_refund`            | 退款单表       | 每笔退款单归属于一个订单和支付单 |
| `trade_refund_item`       | 退款明细表     | 每笔退款单可以包含多条退款明细   |
| `trade_refund_notify_log` | 退款回调日志表 | 记录第三方退款回调原始内容       |

推荐的业务关系为：

```text
trade_order 1 —— N trade_refund

trade_payment 1 —— N trade_refund

trade_refund 1 —— N trade_refund_item

trade_order_item 1 —— N trade_refund_item

trade_refund 1 —— N trade_refund_notify_log
```

退款单与订单、支付单的职责边界如下：

| 模型           | 职责                                           | 不建议承载                           |
| -------------- | ---------------------------------------------- | ------------------------------------ |
| 订单主表       | 保存订单状态、支付状态、已退款金额等聚合结果   | 退款审核记录、渠道退款请求和回调原文 |
| 订单明细表     | 保存商品行退款数量和退款金额聚合结果           | 每一次退款申请和渠道退款流水         |
| 支付单表       | 保存支付流水、支付金额、已退款金额聚合结果     | 具体退款商品、退款原因和售后明细     |
| 退款单表       | 保存退款申请、退款金额、退款状态、第三方退款号 | 商品完整快照和订单完整收货信息       |
| 退款明细表     | 保存本次退款对应的订单明细、退款数量和金额     | 支付渠道请求参数                     |
| 退款回调日志表 | 保存渠道退款通知原文和处理结果                 | 退款金额计算主逻辑                   |

退款单建模的核心原则是“退款过程独立、退款结果回写”。退款单负责保存完整退款生命周期，订单、订单明细和支付单只保存退款后的聚合结果。

### 字段设计

字段设计需要满足退款申请、退款审核、渠道退款、退款回调、退款失败重试、财务对账和售后追踪。退款金额建议使用`DECIMAL`，退款状态建议使用`TINYINT`，退款单号建议使用全局唯一业务单号。

退款单表用于保存一次退款的整体信息。

```sql
CREATE TABLE trade_refund (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    refund_no VARCHAR(64) NOT NULL COMMENT '退款单号',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    payment_id BIGINT UNSIGNED NOT NULL COMMENT '支付单ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    tenant_id BIGINT UNSIGNED DEFAULT NULL COMMENT '租户ID',
    refund_type TINYINT NOT NULL DEFAULT 1 COMMENT '退款类型：1整单退款，2部分退款，3仅退款，4退货退款，5差额退款',
    refund_source TINYINT NOT NULL DEFAULT 1 COMMENT '退款来源：1用户申请，2后台创建，3系统自动，4接口创建',
    refund_status TINYINT NOT NULL DEFAULT 10 COMMENT '退款状态：10待审核，20审核通过，30退款中，40退款成功，50退款失败，60已拒绝，70已取消',
    refund_channel TINYINT NOT NULL COMMENT '退款渠道：1微信，2支付宝，3银行卡，4余额',
    refund_reason VARCHAR(255) NOT NULL COMMENT '退款原因',
    refund_remark VARCHAR(500) DEFAULT NULL COMMENT '退款说明',
    apply_amount DECIMAL(12,2) NOT NULL COMMENT '申请退款金额',
    approved_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '审核通过退款金额',
    refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '实际退款金额',
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    third_refund_no VARCHAR(128) DEFAULT NULL COMMENT '第三方退款单号',
    third_trade_no VARCHAR(128) DEFAULT NULL COMMENT '第三方支付交易号',
    request_body JSON DEFAULT NULL COMMENT '退款请求参数',
    response_body JSON DEFAULT NULL COMMENT '退款响应参数',
    failure_code VARCHAR(64) DEFAULT NULL COMMENT '失败编码',
    failure_msg VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    approved_at DATETIME DEFAULT NULL COMMENT '审核通过时间',
    rejected_at DATETIME DEFAULT NULL COMMENT '拒绝时间',
    refunding_at DATETIME DEFAULT NULL COMMENT '发起退款时间',
    refunded_at DATETIME DEFAULT NULL COMMENT '退款成功时间',
    canceled_at DATETIME DEFAULT NULL COMMENT '取消时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款单表';
```

退款明细表用于保存本次退款涉及哪些订单明细、退多少数量、退多少金额。

```sql
CREATE TABLE trade_refund_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    refund_id BIGINT UNSIGNED NOT NULL COMMENT '退款单ID',
    refund_no VARCHAR(64) NOT NULL COMMENT '退款单号',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    order_item_id BIGINT UNSIGNED NOT NULL COMMENT '订单明细ID',
    spu_id BIGINT UNSIGNED NOT NULL COMMENT 'SPU ID',
    sku_id BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    spu_code VARCHAR(64) NOT NULL COMMENT 'SPU编码快照',
    sku_code VARCHAR(64) NOT NULL COMMENT 'SKU编码快照',
    spu_name VARCHAR(200) NOT NULL COMMENT '商品名称快照',
    sku_name VARCHAR(200) NOT NULL COMMENT 'SKU名称快照',
    spec_text VARCHAR(500) DEFAULT NULL COMMENT '规格描述快照',
    product_image VARCHAR(500) DEFAULT NULL COMMENT '商品图片快照',
    unit_price DECIMAL(12,2) NOT NULL COMMENT '商品单价快照',
    refund_quantity INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '本次退款数量',
    refundable_amount DECIMAL(12,2) NOT NULL COMMENT '本次可退金额',
    apply_amount DECIMAL(12,2) NOT NULL COMMENT '本次申请退款金额',
    approved_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '本次审核通过退款金额',
    refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '本次实际退款金额',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款明细表';
```

退款回调日志表用于保存第三方支付平台的退款通知。

```sql
CREATE TABLE trade_refund_notify_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    refund_no VARCHAR(64) NOT NULL COMMENT '退款单号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    refund_channel TINYINT NOT NULL COMMENT '退款渠道：1微信，2支付宝，3银行卡，4余额',
    notify_type TINYINT NOT NULL DEFAULT 1 COMMENT '通知类型：1退款成功，2退款失败，3退款关闭',
    third_refund_no VARCHAR(128) DEFAULT NULL COMMENT '第三方退款单号',
    third_trade_no VARCHAR(128) DEFAULT NULL COMMENT '第三方支付交易号',
    notify_body JSON NOT NULL COMMENT '回调原始内容',
    process_status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理，1处理成功，2处理失败，3重复通知',
    process_msg VARCHAR(500) DEFAULT NULL COMMENT '处理说明',
    notified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
    processed_at DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款回调日志表';
```

字段设计建议如下：

| 字段类型     | 设计建议                                               |
| ------------ | ------------------------------------------------------ |
| 退款单号     | 使用`refund_no`作为退款业务单号，必须全局唯一          |
| 订单号       | 退款单和退款明细都冗余`order_no`，便于售后、对账和排查 |
| 支付单号     | 退款单保存`payment_no`，明确本次退款基于哪笔支付       |
| 退款金额     | 申请金额、审核金额、实际退款金额分开保存               |
| 退款状态     | 使用独立`refund_status`，不要复用订单状态或支付状态    |
| 退款渠道     | 使用`refund_channel`记录实际退款渠道                   |
| 第三方退款号 | 使用`third_refund_no`保存支付渠道返回的退款流水        |
| 明细快照     | 退款明细保存商品名称、SKU名称、规格、图片、单价快照    |
| 回调原文     | 退款回调日志保存完整`notify_body`，便于审计和排查      |
| 软删除       | 退款数据涉及财务和售后，不建议物理删除                 |

退款金额建议满足以下关系：

```text
apply_amount <= 订单剩余可退金额

approved_amount <= apply_amount

refund_amount <= approved_amount

订单累计退款金额 <= 订单实付金额

支付单累计退款金额 <= 支付单实付金额

订单明细累计退款数量 <= 订单明细购买数量

订单明细累计退款金额 <= 订单明细可退金额
```

退款金额不应重新读取商品当前价格计算。退款应基于订单明细中的成交价、优惠分摊结果和已退款金额计算。

### 索引设计

索引设计应围绕退款单号查询、订单退款记录、支付单退款记录、用户售后列表、退款状态扫描、渠道退款对账、退款回调定位等访问路径展开。

退款单表推荐索引如下：

```sql
ALTER TABLE trade_refund
    ADD UNIQUE KEY uk_refund_no (refund_no),
    ADD UNIQUE KEY uk_channel_third_refund_no (refund_channel, third_refund_no),
    ADD KEY idx_order_no_status (order_no, refund_status),
    ADD KEY idx_payment_no_status (payment_no, refund_status),
    ADD KEY idx_user_status_created (user_id, refund_status, created_at),
    ADD KEY idx_status_created (refund_status, created_at),
    ADD KEY idx_channel_status_refunded (refund_channel, refund_status, refunded_at),
    ADD KEY idx_tenant_created (tenant_id, created_at);
```

退款明细表推荐索引如下：

```sql
ALTER TABLE trade_refund_item
    ADD KEY idx_refund_id (refund_id),
    ADD KEY idx_refund_no (refund_no),
    ADD KEY idx_order_item_id (order_item_id),
    ADD KEY idx_order_no (order_no),
    ADD KEY idx_sku_created (sku_id, created_at);
```

退款回调日志表推荐索引如下：

```sql
ALTER TABLE trade_refund_notify_log
    ADD KEY idx_refund_no_created (refund_no, created_at),
    ADD KEY idx_order_no_created (order_no, created_at),
    ADD KEY idx_payment_no_created (payment_no, created_at),
    ADD KEY idx_channel_third_refund_no (refund_channel, third_refund_no),
    ADD KEY idx_process_status_created (process_status, created_at);
```

索引设计说明：

| 索引                          | 作用                                           |
| ----------------------------- | ---------------------------------------------- |
| `uk_refund_no`                | 保证退款单号唯一，支持退款创建、查询和回调定位 |
| `uk_channel_third_refund_no`  | 防止同一渠道第三方退款号重复入库               |
| `idx_order_no_status`         | 支持查询某个订单下的退款记录                   |
| `idx_payment_no_status`       | 支持查询某笔支付下的退款记录                   |
| `idx_user_status_created`     | 支持用户退款/售后列表分页                      |
| `idx_status_created`          | 支持后台按退款状态查询和处理                   |
| `idx_channel_status_refunded` | 支持退款渠道对账                               |
| `idx_refund_id`               | 支持退款详情加载退款明细                       |
| `idx_order_item_id`           | 支持统计订单明细累计退款数量和金额             |
| `idx_process_status_created`  | 支持退款回调失败重试和异常排查                 |

需要注意，`third_refund_no`在渠道退款成功或受理前可能为空。MySQL唯一索引允许多个`NULL`值，因此`uk_channel_third_refund_no`不会阻止多条待退款记录存在。业务上应在渠道返回第三方退款号后校验其唯一性。

### 常用查询

常用查询需要围绕退款详情、订单退款记录、用户售后列表、后台审核、退款中任务、渠道对账和异常排查等场景设计。退款单表适合查询退款整体状态，退款明细表适合查询本次退款涉及的具体商品行。

#### 根据退款单号查询退款单

退款单号是退款系统内部最重要的定位字段。退款详情、退款回调、退款状态查询都可以通过退款单号定位。

```sql
SELECT
    id,
    refund_no,
    order_id,
    order_no,
    payment_id,
    payment_no,
    user_id,
    refund_type,
    refund_source,
    refund_status,
    refund_channel,
    refund_reason,
    refund_remark,
    apply_amount,
    approved_amount,
    refund_amount,
    currency,
    third_refund_no,
    third_trade_no,
    failure_code,
    failure_msg,
    applied_at,
    approved_at,
    refunding_at,
    refunded_at,
    created_at
FROM trade_refund
WHERE refund_no = 'REF202601010001'
  AND deleted = 0
LIMIT 1;
```

该查询适合退款详情页、售后详情页、退款状态轮询和退款回调处理前校验。

#### 查询退款详情和退款明细

退款详情通常先查询退款单，再查询退款明细。这样可以避免主表和明细表一对多JOIN造成字段重复和分页异常。

```sql
SELECT
    id,
    refund_id,
    refund_no,
    order_item_id,
    spu_id,
    sku_id,
    spu_code,
    sku_code,
    spu_name,
    sku_name,
    spec_text,
    product_image,
    unit_price,
    refund_quantity,
    refundable_amount,
    apply_amount,
    approved_amount,
    refund_amount
FROM trade_refund_item
WHERE refund_no = 'REF202601010001'
  AND deleted = 0
ORDER BY id ASC;
```

退款明细中的商品信息应来自订单明细快照，而不是商品SPU或SKU实时表。

#### 查询订单下的退款记录

订单详情页、后台订单售后页通常需要查询某个订单下的所有退款单。

```sql
SELECT
    id,
    refund_no,
    order_no,
    payment_no,
    refund_type,
    refund_status,
    refund_channel,
    refund_reason,
    apply_amount,
    approved_amount,
    refund_amount,
    third_refund_no,
    applied_at,
    refunded_at,
    created_at
FROM trade_refund
WHERE order_no = 'ORD202601010001'
  AND deleted = 0
ORDER BY created_at DESC;
```

如果一个订单支持多次部分退款，该查询可以展示完整退款历史。

#### 查询用户退款列表

用户中心通常需要按照用户ID、退款状态、申请时间分页查询退款单。

```sql
SELECT
    id,
    refund_no,
    order_no,
    refund_type,
    refund_status,
    refund_reason,
    apply_amount,
    approved_amount,
    refund_amount,
    applied_at,
    refunded_at,
    created_at
FROM trade_refund
WHERE user_id = 10001
  AND refund_status IN (10, 20, 30, 40, 50)
  AND deleted = 0
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;
```

用户退款列表如果需要展示商品缩略信息，可以先分页查询退款单，再批量查询退款明细。

#### 查询待审核退款单

后台售后审核通常按退款状态和申请时间查询待处理退款单。

```sql
SELECT
    id,
    refund_no,
    order_no,
    user_id,
    refund_type,
    refund_source,
    refund_reason,
    apply_amount,
    applied_at,
    created_at
FROM trade_refund
WHERE refund_status = 10
  AND deleted = 0
ORDER BY applied_at ASC, id ASC
LIMIT 100;
```

审核时需要校验订单是否已支付、退款金额是否未超过可退金额、退款明细数量是否未超过剩余可退数量。

#### 查询退款中的退款单

退款单审核通过并调用支付渠道后，可能进入退款中状态。定时任务可以扫描长时间未完成的退款单，主动查询渠道退款结果。

```sql
SELECT
    id,
    refund_no,
    order_no,
    payment_no,
    refund_channel,
    refund_status,
    approved_amount,
    third_refund_no,
    refunding_at
FROM trade_refund
WHERE refund_status = 30
  AND refunding_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE)
  AND deleted = 0
ORDER BY refunding_at ASC
LIMIT 100;
```

该查询适合退款结果补偿任务。补偿任务不应直接假定退款成功，而应调用渠道查询接口确认结果。

#### 查询订单明细累计退款金额和数量

创建新的部分退款前，需要查询某个订单明细已经退款的数量和金额。

```sql
SELECT
    order_item_id,
    SUM(refund_quantity) AS refunded_quantity,
    SUM(refund_amount) AS refunded_amount
FROM trade_refund_item
WHERE order_item_id = 91001
  AND deleted = 0
  AND refund_no IN (
      SELECT refund_no
      FROM trade_refund
      WHERE refund_status IN (20, 30, 40)
        AND deleted = 0
  )
GROUP BY order_item_id;
```

严格场景下，不建议只依赖查询结果做校验，还应在更新订单明细退款聚合字段时增加条件限制，避免并发超退。

#### 查询退款渠道对账数据

财务对账通常按退款渠道、退款成功状态和退款成功时间范围查询。

```sql
SELECT
    id,
    refund_no,
    order_no,
    payment_no,
    refund_channel,
    approved_amount,
    refund_amount,
    third_refund_no,
    third_trade_no,
    refunded_at
FROM trade_refund
WHERE refund_channel = 1
  AND refund_status = 40
  AND refunded_at >= '2026-01-01 00:00:00'
  AND refunded_at < '2026-01-02 00:00:00'
  AND deleted = 0
ORDER BY refunded_at ASC, id ASC;
```

大数据量对账导出建议使用游标分页，避免大偏移量`OFFSET`。

```sql
SELECT
    id,
    refund_no,
    order_no,
    payment_no,
    refund_channel,
    refund_amount,
    third_refund_no,
    refunded_at
FROM trade_refund
WHERE id > 100000
  AND refund_channel = 1
  AND refund_status = 40
  AND refunded_at >= '2026-01-01 00:00:00'
  AND refunded_at < '2026-01-02 00:00:00'
  AND deleted = 0
ORDER BY id ASC
LIMIT 1000;
```

#### 查询退款回调日志

退款异常排查时，需要查看第三方退款回调内容和系统处理结果。

```sql
SELECT
    id,
    refund_no,
    order_no,
    payment_no,
    refund_channel,
    notify_type,
    third_refund_no,
    third_trade_no,
    notify_body,
    process_status,
    process_msg,
    notified_at,
    processed_at
FROM trade_refund_notify_log
WHERE refund_no = 'REF202601010001'
ORDER BY created_at DESC;
```

如果退款单状态与支付渠道状态不一致，应优先检查退款回调日志，再结合渠道主动查询结果处理补偿。

### 常用写入

退款写入通常涉及退款单、退款明细、订单聚合退款金额、订单明细退款数量、支付单退款金额和渠道退款状态。退款创建、审核、发起渠道退款、处理回调都需要保证幂等和金额不超退。

#### 创建退款单

用户或后台发起退款申请时，先创建退款单和退款明细。创建前需要校验订单已支付、订单未关闭、申请金额不超过剩余可退金额、退款数量不超过剩余可退数量。

```sql
INSERT INTO trade_refund (
    refund_no,
    order_id,
    order_no,
    payment_id,
    payment_no,
    user_id,
    tenant_id,
    refund_type,
    refund_source,
    refund_status,
    refund_channel,
    refund_reason,
    refund_remark,
    apply_amount,
    approved_amount,
    refund_amount,
    currency,
    applied_at,
    created_by,
    updated_by
) VALUES (
    'REF202601010001',
    90001,
    'ORD202601010001',
    70001,
    'PAY202601010001',
    10001,
    1,
    2,
    1,
    10,
    1,
    '不想要了',
    '用户申请部分退款',
    99.00,
    0.00,
    0.00,
    'CNY',
    NOW(),
    10001,
    10001
);
```

写入退款明细：

```sql
INSERT INTO trade_refund_item (
    refund_id,
    refund_no,
    order_id,
    order_no,
    order_item_id,
    spu_id,
    sku_id,
    spu_code,
    sku_code,
    spu_name,
    sku_name,
    spec_text,
    product_image,
    unit_price,
    refund_quantity,
    refundable_amount,
    apply_amount,
    approved_amount,
    refund_amount
) VALUES (
    60001,
    'REF202601010001',
    90001,
    'ORD202601010001',
    91001,
    10001,
    20001,
    'SPU202601010001',
    'SKU202601010001',
    '基础款纯棉T恤',
    '基础款纯棉T恤 红色 L',
    '颜色:红色;尺码:L',
    'https://static.example.com/product/tshirt-main.png',
    99.00,
    1,
    99.00,
    99.00,
    0.00,
    0.00
);
```

创建退款单时，退款明细应从订单明细快照复制数据，不应从商品表实时读取名称、规格、图片和价格。

#### 审核通过退款单

审核通过后，将退款单状态改为审核通过，并写入审核通过金额。

```sql
UPDATE trade_refund
SET refund_status = 20,
    approved_amount = 99.00,
    approved_at = NOW(),
    updated_by = 20001,
    updated_at = NOW()
WHERE refund_no = 'REF202601010001'
  AND refund_status = 10
  AND apply_amount >= 99.00
  AND deleted = 0;
```

同步更新退款明细审核金额：

```sql
UPDATE trade_refund_item
SET approved_amount = apply_amount,
    updated_at = NOW()
WHERE refund_no = 'REF202601010001'
  AND deleted = 0;
```

审核通过后仍不代表资金已经退回成功，只表示业务允许发起退款。

#### 拒绝退款单

审核不通过时，将退款单更新为已拒绝，并保存拒绝原因。

```sql
UPDATE trade_refund
SET refund_status = 60,
    failure_code = 'REFUND_REJECTED',
    failure_msg = '商品已超过售后期限',
    rejected_at = NOW(),
    updated_by = 20001,
    updated_at = NOW()
WHERE refund_no = 'REF202601010001'
  AND refund_status = 10
  AND deleted = 0;
```

已拒绝的退款单不应继续发起支付渠道退款，也不应更新订单和支付单的已退款金额。

#### 发起渠道退款

审核通过后，调用支付渠道退款接口。调用前将退款单更新为退款中，并保存请求和响应内容。

```sql
UPDATE trade_refund
SET refund_status = 30,
    request_body = JSON_OBJECT(
        'refundNo', 'REF202601010001',
        'paymentNo', 'PAY202601010001',
        'orderNo', 'ORD202601010001',
        'refundAmount', 99.00
    ),
    response_body = JSON_OBJECT(
        'channelStatus', 'PROCESSING',
        'thirdRefundNo', 'WXREF2026010100019999'
    ),
    third_refund_no = 'WXREF2026010100019999',
    refunding_at = NOW(),
    updated_at = NOW()
WHERE refund_no = 'REF202601010001'
  AND refund_status = 20
  AND approved_amount = 99.00
  AND deleted = 0;
```

如果渠道退款接口同步返回成功，也不建议跳过回调日志。可以同时更新为退款成功，但仍要保存渠道响应内容，后续对账以渠道状态为准。

#### 记录退款回调日志

收到第三方退款回调后，应先写入退款回调日志，再处理退款状态。

```sql
INSERT INTO trade_refund_notify_log (
    refund_no,
    order_no,
    payment_no,
    refund_channel,
    notify_type,
    third_refund_no,
    third_trade_no,
    notify_body,
    process_status,
    process_msg,
    notified_at
) VALUES (
    'REF202601010001',
    'ORD202601010001',
    'PAY202601010001',
    1,
    1,
    'WXREF2026010100019999',
    'WX2026010100019999',
    JSON_OBJECT(
        'refund_status', 'SUCCESS',
        'refund_id', 'WXREF2026010100019999',
        'transaction_id', 'WX2026010100019999',
        'out_refund_no', 'REF202601010001',
        'refund_amount', 99.00
    ),
    0,
    '待处理',
    NOW()
);
```

退款回调日志应保存完整通知内容，避免后续退款争议时缺少渠道侧证据。

#### 退款成功更新退款单

退款成功时，使用退款单号、状态和金额条件进行幂等更新。只有审核通过或退款中的退款单允许更新为退款成功。

```sql
UPDATE trade_refund
SET refund_status = 40,
    refund_amount = 99.00,
    third_refund_no = 'WXREF2026010100019999',
    third_trade_no = 'WX2026010100019999',
    response_body = JSON_OBJECT(
        'refund_status', 'SUCCESS',
        'refund_id', 'WXREF2026010100019999'
    ),
    refunded_at = NOW(),
    updated_at = NOW()
WHERE refund_no = 'REF202601010001'
  AND refund_status IN (20, 30)
  AND approved_amount = 99.00
  AND deleted = 0;
```

如果影响行数为0，需要查询退款单当前状态，判断是否为重复回调、金额不一致、退款单不存在或退款单已失败。

#### 退款成功更新订单和支付单

退款单成功后，需要回写订单、支付单和订单明细的退款聚合结果。该过程应与退款单状态更新放在同一个事务中，或通过可靠消息保证最终一致。

更新订单主表：

```sql
UPDATE trade_order
SET refund_amount = refund_amount + 99.00,
    pay_status = CASE
        WHEN refund_amount + 99.00 >= paid_amount THEN 3
        ELSE pay_status
    END,
    updated_at = NOW()
WHERE order_no = 'ORD202601010001'
  AND paid_amount >= refund_amount + 99.00
  AND deleted = 0;
```

更新支付单：

```sql
UPDATE trade_payment
SET refund_amount = refund_amount + 99.00,
    updated_at = NOW()
WHERE payment_no = 'PAY202601010001'
  AND paid_amount >= refund_amount + 99.00
  AND deleted = 0;
```

更新订单明细：

```sql
UPDATE trade_order_item
SET refund_quantity = refund_quantity + 1,
    refund_amount = refund_amount + 99.00,
    item_status = CASE
        WHEN refund_quantity + 1 >= quantity THEN 40
        ELSE 30
    END,
    updated_at = NOW()
WHERE id = 91001
  AND refund_quantity + 1 <= quantity
  AND payable_amount >= refund_amount + 99.00
  AND deleted = 0;
```

这些更新条件可以防止并发退款导致超退。实际业务中还应结合分布式锁、唯一幂等键或状态机控制。

#### 标记回调处理结果

退款回调业务处理成功后，需要更新回调日志状态。

```sql
UPDATE trade_refund_notify_log
SET process_status = 1,
    process_msg = '退款成功回调处理完成',
    processed_at = NOW()
WHERE id = 80001
  AND process_status = 0;
```

重复回调可以标记为重复通知，不应重复累计退款金额。

```sql
UPDATE trade_refund_notify_log
SET process_status = 3,
    process_msg = '重复退款回调，退款单已成功',
    processed_at = NOW()
WHERE id = 80002
  AND process_status = 0;
```

#### 退款失败更新退款单

支付渠道明确返回退款失败时，可以将退款单更新为退款失败，并保存失败原因。

```sql
UPDATE trade_refund
SET refund_status = 50,
    failure_code = 'REFUND_FAILED',
    failure_msg = '支付渠道退款失败，请人工处理',
    response_body = JSON_OBJECT(
        'refund_status', 'FAILED',
        'reason', 'CHANNEL_REFUND_FAILED'
    ),
    updated_at = NOW()
WHERE refund_no = 'REF202601010001'
  AND refund_status IN (20, 30)
  AND deleted = 0;
```

退款失败后不应直接增加订单、支付单和订单明细的已退款金额。可以允许人工重试、重新发起渠道退款，或创建新的退款单处理。

#### 取消退款单

用户撤销申请或后台取消未执行的退款单时，可以将退款单更新为已取消。

```sql
UPDATE trade_refund
SET refund_status = 70,
    canceled_at = NOW(),
    updated_by = 10001,
    updated_at = NOW()
WHERE refund_no = 'REF202601010001'
  AND refund_status = 10
  AND deleted = 0;
```

已进入退款中状态的退款单通常不允许直接取消，除非支付渠道支持撤销退款。

### 常见问题

退款单模型的常见问题集中在超退、重复回调、退款金额计算错误、退款状态和订单状态混用、退款失败补偿不完整等方面。

| 问题                       | 原因                           | 建议                                                   |
| -------------------------- | ------------------------------ | ------------------------------------------------------ |
| 发生超额退款               | 并发退款时只在应用层校验金额   | 更新订单、支付单、订单明细时增加金额条件               |
| 退款回调重复累计金额       | 第三方退款回调可能重复通知     | 退款成功处理必须基于退款单状态幂等更新                 |
| 退款金额和订单金额不一致   | 退款时重新读取商品当前价格     | 退款金额应基于订单明细快照和已支付金额计算             |
| 退款状态污染订单状态       | 使用订单状态表达所有退款过程   | 退款过程放在退款单，订单只保存聚合退款结果             |
| 部分退款后明细状态错误     | 未维护明细退款数量和金额       | 订单明细保存`refund_quantity`和`refund_amount`         |
| 退款失败后无法追踪原因     | 未保存渠道请求、响应和回调原文 | 保存`request_body`、`response_body`和回调日志          |
| 一个支付单退款超过实付金额 | 支付单未维护累计退款金额       | 支付单保存`refund_amount`并加条件更新                  |
| 退款对账困难               | 缺少第三方退款号和退款成功时间 | 保存`third_refund_no`、`refund_channel`、`refunded_at` |
| 售后和退款强耦合           | 售后单直接操作支付状态         | 售后单负责业务审核，退款单负责资金退回                 |

一个常见误区是把退款单理解为订单的一个附属状态字段。实际交易系统中，退款是一笔独立资金动作，可能审核失败、渠道失败、重复通知、部分成功、人工补偿，因此必须独立建模。

另一个常见误区是退款成功后只更新订单表，不更新支付单和订单明细。这样会导致支付对账、明细售后、商品维度退款统计不准确。退款成功后应同步维护订单、支付单和订单明细的退款聚合字段。

### 总结

退款单模型的核心是将退款申请、业务审核、渠道退款、退款回调和退款结果回写拆分清楚。退款单保存退款生命周期，退款明细保存本次退款涉及的商品行，退款回调日志保存第三方通知原文，订单和支付单只保存退款后的聚合结果。

建模时建议遵循以下原则：

| 原则           | 说明                                                   |
| -------------- | ------------------------------------------------------ |
| 退款独立成单   | 退款不是订单状态字段，应使用独立退款单记录生命周期     |
| 金额严格防超退 | 订单、支付单、订单明细更新时都要校验剩余可退金额       |
| 明细保留快照   | 退款明细复制订单明细快照，不读取商品实时数据           |
| 回调必须幂等   | 退款成功处理必须带状态条件，避免重复累计退款金额       |
| 渠道流水可追踪 | 保存第三方退款号、第三方支付交易号和退款渠道           |
| 原始通知要留存 | 退款回调原文必须记录，便于审计、排查和对账             |
| 失败允许补偿   | 退款失败不代表业务结束，应支持重试、人工处理或重新退款 |
| 聚合结果回写   | 退款成功后同步更新订单、支付单和订单明细的退款聚合字段 |

该模型是交易闭环中的最后一段核心模型。订单、支付单和退款单职责清晰后，业务系统可以稳定支撑下单、支付、退款、售后、财务对账和资金追踪。