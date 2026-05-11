# MySQL 常用业务建模模型



- 单表模型
- 主从表模型
- 一对一模型
- 一对多模型
- 多对多模型
- 字典表模型
- 状态机模型
- 树形层级模型
- 用户-角色-权限模型
- 组织架构模型
- 商品-SPU-SKU模型
- 订单-订单明细模型
- 账户流水模型
- 标签模型
- 分类模型
- 附件资源模型
- 配置项模型
- 操作日志模型
- 审计日志模型
- 软删除模型
- 乐观锁模型
- 冗余字段模型
- 宽表模型
- JSON扩展字段模型
- 多租户模型
- 历史版本模型
- 统计汇总模型
- 归档数据模型
- 分区表模型
- 分库分表模型



## 单表模型

单表模型是业务建模中最基础、最常用的模型，适用于一个业务对象可以由一张表完整描述的场景。它的特点是结构简单、查询直接、维护成本低，常用于系统配置、通知公告、字典数据、标签、分类、操作日志等基础业务。

下面以“通知公告表”为例说明单表模型的建表方式和常用 SQL 实践。

### 适用场景

单表模型适合业务关系简单、字段结构稳定、查询条件清晰的业务对象。一般情况下，如果一个业务对象不存在明显的明细数据，也不需要复杂的多表关系，就可以优先使用单表模型。

常见场景包括：

- 通知公告
- 系统配置
- 字典数据
- 标签管理
- 分类管理
- 操作日志
- 登录日志
- Banner 管理
- 文章管理
- 基础资料维护

### 建表 SQL

下面的 SQL 创建一张通知公告表，用于演示单表模型的常见字段、索引和约束设计。

```sql
CREATE TABLE `sys_notice` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `notice_code` VARCHAR(64) NOT NULL COMMENT '公告编码',
  `notice_title` VARCHAR(200) NOT NULL COMMENT '公告标题',
  `notice_type` TINYINT NOT NULL DEFAULT 1 COMMENT '公告类型：1通知，2公告，3提醒',
  `notice_content` TEXT NULL COMMENT '公告内容',
  `notice_status` TINYINT NOT NULL DEFAULT 0 COMMENT '公告状态：0草稿，1发布，2下架',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0否，1是',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `publish_time` DATETIME NULL COMMENT '发布时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `extra_json` JSON NULL COMMENT '扩展信息，存放低频扩展字段',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_code` (`notice_code`),
  KEY `idx_status_deleted` (`notice_status`, `is_deleted`),
  KEY `idx_type_status_deleted` (`notice_type`, `notice_status`, `is_deleted`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_top_sort_time` (`is_top`, `sort_order`, `publish_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统通知公告表';
```

### 字段设计说明

单表模型的字段应围绕一个明确的业务对象展开，不建议把多个业务对象混在一张表中。常用字段可以直接设计为独立列，低频变化字段可以放到 JSON 扩展字段中。

| 字段            | 说明                                            |
| --------------- | ----------------------------------------------- |
| `id`            | 主键 ID，通常由雪花算法、号段模式或业务框架生成 |
| `notice_code`   | 业务编码，适合做唯一标识                        |
| `notice_title`  | 公告标题，用于展示和搜索                        |
| `notice_type`   | 业务类型字段，适合配合字典或枚举使用            |
| `notice_status` | 状态字段，用于控制草稿、发布、下架等流程        |
| `is_top`        | 是否置顶                                        |
| `sort_order`    | 排序字段                                        |
| `publish_time`  | 发布时间                                        |
| `extra_json`    | 扩展字段，不建议存放核心查询条件                |
| `is_deleted`    | 逻辑删除字段                                    |
| `created_at`    | 创建时间                                        |
| `updated_at`    | 更新时间                                        |

### 新增数据

下面的 SQL 用于新增一条通知公告数据，适合后台管理系统中的新增公告场景。

```sql
INSERT INTO `sys_notice` (
  `id`,
  `notice_code`,
  `notice_title`,
  `notice_type`,
  `notice_content`,
  `notice_status`,
  `is_top`,
  `sort_order`,
  `publish_time`,
  `remark`,
  `extra_json`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  'NOTICE_202605110001',
  '系统维护通知',
  1,
  '系统将于今晚 23:00 至 23:30 进行维护。',
  1,
  1,
  100,
  NOW(),
  '首页展示',
  JSON_OBJECT('source', 'admin', 'level', 'important'),
  1,
  1
);
```

### 根据主键查询

下面的 SQL 用于根据主键查询单条数据，通常用于详情页、编辑页或接口详情查询。

```sql
SELECT
  `id`,
  `notice_code`,
  `notice_title`,
  `notice_type`,
  `notice_content`,
  `notice_status`,
  `is_top`,
  `sort_order`,
  `publish_time`,
  `remark`,
  `extra_json`,
  `created_by`,
  `created_at`,
  `updated_by`,
  `updated_at`
FROM `sys_notice`
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 根据业务编码查询

下面的 SQL 用于根据唯一业务编码查询数据，适合对外接口、业务幂等校验或编码唯一性校验。

```sql
SELECT
  `id`,
  `notice_code`,
  `notice_title`,
  `notice_type`,
  `notice_status`,
  `publish_time`
FROM `sys_notice`
WHERE `notice_code` = 'NOTICE_202605110001'
  AND `is_deleted` = 0;
```

### 分页查询

下面的 SQL 用于后台列表分页查询，支持按状态、类型和发布时间排序。

```sql
SELECT
  `id`,
  `notice_code`,
  `notice_title`,
  `notice_type`,
  `notice_status`,
  `is_top`,
  `sort_order`,
  `publish_time`,
  `created_at`
FROM `sys_notice`
WHERE `is_deleted` = 0
  AND `notice_status` = 1
  AND `notice_type` = 1
ORDER BY
  `is_top` DESC,
  `sort_order` DESC,
  `publish_time` DESC
LIMIT 10 OFFSET 0;
```

### 标题模糊查询

下面的 SQL 用于根据标题进行模糊搜索，适合后台管理系统中的关键词查询。

```sql
SELECT
  `id`,
  `notice_code`,
  `notice_title`,
  `notice_type`,
  `notice_status`,
  `publish_time`
FROM `sys_notice`
WHERE `is_deleted` = 0
  AND `notice_title` LIKE CONCAT('%', '维护', '%')
ORDER BY `publish_time` DESC
LIMIT 10 OFFSET 0;
```

### 修改数据

下面的 SQL 用于修改通知公告的基础信息，适合后台编辑公告场景。

```sql
UPDATE `sys_notice`
SET
  `notice_title` = '系统维护时间调整通知',
  `notice_content` = '系统维护时间调整为今晚 23:30 至 24:00。',
  `notice_type` = 1,
  `is_top` = 1,
  `sort_order` = 200,
  `remark` = '维护时间已调整',
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 发布公告

下面的 SQL 用于将草稿公告发布上线，适合状态流转场景。

```sql
UPDATE `sys_notice`
SET
  `notice_status` = 1,
  `publish_time` = NOW(),
  `updated_by` = 1
WHERE `id` = 100001
  AND `notice_status` = 0
  AND `is_deleted` = 0;
```

### 下架公告

下面的 SQL 用于将已发布公告下架，适合公告不再展示的业务场景。

```sql
UPDATE `sys_notice`
SET
  `notice_status` = 2,
  `updated_by` = 1
WHERE `id` = 100001
  AND `notice_status` = 1
  AND `is_deleted` = 0;
```

### 逻辑删除

下面的 SQL 用于逻辑删除数据，保留历史记录，不直接物理删除。

```sql
UPDATE `sys_notice`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 批量更新状态

下面的 SQL 用于批量下架多条公告，适合后台批量操作场景。

```sql
UPDATE `sys_notice`
SET
  `notice_status` = 2,
  `updated_by` = 1
WHERE `id` IN (100001, 100002, 100003)
  AND `notice_status` = 1
  AND `is_deleted` = 0;
```

### 查询最新发布公告

下面的 SQL 用于查询最新发布的公告，适合首页、门户页或移动端公告栏展示。

```sql
SELECT
  `id`,
  `notice_title`,
  `notice_type`,
  `publish_time`
FROM `sys_notice`
WHERE `is_deleted` = 0
  AND `notice_status` = 1
ORDER BY
  `is_top` DESC,
  `sort_order` DESC,
  `publish_time` DESC
LIMIT 5;
```

### JSON 字段查询

下面的 SQL 用于查询 JSON 扩展字段中的属性。JSON 字段适合存放低频扩展数据，但不建议承载核心查询条件。

```sql
SELECT
  `id`,
  `notice_code`,
  `notice_title`,
  JSON_UNQUOTE(JSON_EXTRACT(`extra_json`, '$.level')) AS `notice_level`
FROM `sys_notice`
WHERE `is_deleted` = 0
  AND JSON_UNQUOTE(JSON_EXTRACT(`extra_json`, '$.level')) = 'important';
```

### 统计查询

下面的 SQL 用于按公告状态统计数量，适合后台仪表盘或管理端统计卡片。

```sql
SELECT
  `notice_status`,
  COUNT(*) AS `total_count`
FROM `sys_notice`
WHERE `is_deleted` = 0
GROUP BY `notice_status`;
```

### 索引设计建议

单表模型的索引应围绕高频查询条件设计，不建议给每个字段都建立索引。常见索引设计如下：

| 索引                                                         | 适用场景                     |
| ------------------------------------------------------------ | ---------------------------- |
| `PRIMARY KEY (id)`                                           | 主键查询、详情查询           |
| `UNIQUE KEY uk_notice_code (notice_code)`                    | 根据业务编码查询、唯一性校验 |
| `idx_status_deleted (notice_status, is_deleted)`             | 根据状态查询                 |
| `idx_type_status_deleted (notice_type, notice_status, is_deleted)` | 根据类型和状态筛选           |
| `idx_publish_time (publish_time)`                            | 根据发布时间排序或范围查询   |
| `idx_top_sort_time (is_top, sort_order, publish_time)`       | 首页置顶和排序查询           |

### 使用建议

单表模型优先用于结构简单、关系清晰、字段数量可控的业务对象。如果后续出现明显的一对多明细数据，应拆分为主从表模型；如果出现多对象关联，应拆分为关系表模型；如果字段持续膨胀，应考虑扩展表、JSON 扩展字段或重新划分业务边界。

使用单表模型时重点注意：

- 一张表只描述一个核心业务对象
- 高频查询字段尽量设计为独立字段
- JSON 字段只用于低频扩展，不承载核心查询
- 状态字段要有明确枚举含义
- 后台列表查询要配合组合索引
- 删除操作优先使用逻辑删除
- 创建时间和更新时间建议所有业务表统一保留

## 主从表模型

主从表模型是业务建模中最常用的复杂模型之一，适用于一个主业务对象下面包含多条明细数据的场景。主表保存业务主体信息，从表保存明细行信息，两者通常通过主表主键或业务单号进行关联。

下面以“订单表”和“订单明细表”为例说明主从表模型的建表方式和常用 SQL 实践。

### 适用场景

主从表模型适合一条主数据对应多条子数据的业务场景。主表通常代表业务单据、业务主体或流程对象，从表通常代表明细项、子记录或业务行项目。

常见场景包括：

- 订单与订单明细
- 采购单与采购明细
- 销售单与销售明细
- 入库单与入库明细
- 出库单与出库明细
- 发票与发票明细
- 问卷与问卷题目
- 表单与表单字段
- 审批单与审批记录
- 对账单与对账明细

### 建表 SQL

下面的 SQL 创建订单主表和订单明细表。订单主表保存订单整体信息，订单明细表保存订单中的商品明细。

```sql
CREATE TABLE `biz_order` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已发货，3已完成，4已取消',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0未支付，1已支付，2已退款',
  `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  `discount_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  `receiver_name` VARCHAR(64) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(32) NOT NULL COMMENT '收货人手机号',
  `receiver_address` VARCHAR(500) NOT NULL COMMENT '收货地址',
  `pay_time` DATETIME NULL COMMENT '支付时间',
  `cancel_time` DATETIME NULL COMMENT '取消时间',
  `finish_time` DATETIME NULL COMMENT '完成时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status_deleted` (`user_id`, `order_status`, `is_deleted`),
  KEY `idx_status_created` (`order_status`, `created_at`),
  KEY `idx_pay_status_created` (`pay_status`, `created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='业务订单表';

CREATE TABLE `biz_order_item` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号，冗余字段，便于查询和排查',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称快照',
  `sku_name` VARCHAR(200) NULL COMMENT 'SKU名称快照',
  `product_image` VARCHAR(500) NULL COMMENT '商品图片快照',
  `unit_price` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '商品单价',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '明细总金额',
  `discount_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '明细优惠金额',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '明细实付金额',
  `item_status` TINYINT NOT NULL DEFAULT 0 COMMENT '明细状态：0正常，1退款中，2已退款',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id_deleted` (`order_id`, `is_deleted`),
  KEY `idx_order_no_deleted` (`order_no`, `is_deleted`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='业务订单明细表';
```

### 字段设计说明

主从表模型中，主表应保存订单整体信息，从表应保存订单明细信息。主表和从表不要重复保存大量相同字段，但可以适当冗余关键业务字段，例如订单编号、商品名称快照、SKU 名称快照等。

| 表               | 字段           | 说明                           |
| ---------------- | -------------- | ------------------------------ |
| `biz_order`      | `id`           | 订单主键 ID                    |
| `biz_order`      | `order_no`     | 订单编号，通常作为业务唯一标识 |
| `biz_order`      | `user_id`      | 下单用户 ID                    |
| `biz_order`      | `order_status` | 订单状态                       |
| `biz_order`      | `pay_amount`   | 订单实付总金额                 |
| `biz_order_item` | `order_id`     | 关联订单主表 ID                |
| `biz_order_item` | `order_no`     | 冗余订单编号，便于查询和排查   |
| `biz_order_item` | `product_id`   | 商品 ID                        |
| `biz_order_item` | `sku_id`       | SKU ID                         |
| `biz_order_item` | `product_name` | 商品名称快照                   |
| `biz_order_item` | `unit_price`   | 下单时商品单价                 |
| `biz_order_item` | `quantity`     | 购买数量                       |
| `biz_order_item` | `pay_amount`   | 明细实付金额                   |

### 新增主表数据

下面的 SQL 用于新增订单主表数据，适合创建订单时先保存订单主体信息。

```sql
INSERT INTO `biz_order` (
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `receiver_name`,
  `receiver_phone`,
  `receiver_address`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  200001,
  'ORDER_202605110001',
  10001,
  0,
  0,
  299.00,
  20.00,
  279.00,
  '张三',
  '13800000000',
  '北京市朝阳区示例路 100 号',
  '用户下单',
  10001,
  10001
);
```

### 新增从表数据

下面的 SQL 用于新增订单明细数据。实际业务中，主表和从表通常应放在同一个事务中提交。

```sql
INSERT INTO `biz_order_item` (
  `id`,
  `order_id`,
  `order_no`,
  `product_id`,
  `sku_id`,
  `product_name`,
  `sku_name`,
  `product_image`,
  `unit_price`,
  `quantity`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `item_status`,
  `remark`
) VALUES
(
  300001,
  200001,
  'ORDER_202605110001',
  50001,
  60001,
  '机械键盘',
  '黑色 青轴',
  'https://example.com/keyboard.png',
  199.00,
  1,
  199.00,
  10.00,
  189.00,
  0,
  '订单明细1'
),
(
  300002,
  200001,
  'ORDER_202605110001',
  50002,
  60002,
  '无线鼠标',
  '白色',
  'https://example.com/mouse.png',
  100.00,
  1,
  100.00,
  10.00,
  90.00,
  0,
  '订单明细2'
);
```

### 查询订单详情

下面的 SQL 用于查询订单主表详情，适合订单详情页的基础信息展示。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `receiver_name`,
  `receiver_phone`,
  `receiver_address`,
  `pay_time`,
  `cancel_time`,
  `finish_time`,
  `remark`,
  `created_at`,
  `updated_at`
FROM `biz_order`
WHERE `id` = 200001
  AND `is_deleted` = 0;
```

### 查询订单明细

下面的 SQL 用于根据订单 ID 查询订单明细列表，适合订单详情页中的商品明细展示。

```sql
SELECT
  `id`,
  `order_id`,
  `order_no`,
  `product_id`,
  `sku_id`,
  `product_name`,
  `sku_name`,
  `product_image`,
  `unit_price`,
  `quantity`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `item_status`
FROM `biz_order_item`
WHERE `order_id` = 200001
  AND `is_deleted` = 0
ORDER BY `id` ASC;
```

### 关联查询订单详情

下面的 SQL 用于一次性查询订单和订单明细。该方式适合数据量较小的详情查询，不建议直接用于大分页列表。

```sql
SELECT
  o.`id` AS `order_id`,
  o.`order_no`,
  o.`user_id`,
  o.`order_status`,
  o.`pay_status`,
  o.`total_amount` AS `order_total_amount`,
  o.`discount_amount` AS `order_discount_amount`,
  o.`pay_amount` AS `order_pay_amount`,
  o.`receiver_name`,
  o.`receiver_phone`,
  o.`receiver_address`,
  i.`id` AS `item_id`,
  i.`product_id`,
  i.`sku_id`,
  i.`product_name`,
  i.`sku_name`,
  i.`unit_price`,
  i.`quantity`,
  i.`total_amount` AS `item_total_amount`,
  i.`discount_amount` AS `item_discount_amount`,
  i.`pay_amount` AS `item_pay_amount`
FROM `biz_order` o
LEFT JOIN `biz_order_item` i
  ON o.`id` = i.`order_id`
  AND i.`is_deleted` = 0
WHERE o.`id` = 200001
  AND o.`is_deleted` = 0
ORDER BY i.`id` ASC;
```

### 订单分页查询

下面的 SQL 用于后台订单分页查询，只查询主表信息，避免分页时被明细表放大数据量。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `created_at`
FROM `biz_order`
WHERE `is_deleted` = 0
  AND `user_id` = 10001
  AND `order_status` = 0
ORDER BY `created_at` DESC
LIMIT 10 OFFSET 0;
```

### 根据订单编号查询

下面的 SQL 用于根据订单编号查询订单，适合支付回调、售后处理、客服查询等业务场景。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `created_at`
FROM `biz_order`
WHERE `order_no` = 'ORDER_202605110001'
  AND `is_deleted` = 0;
```

### 支付订单

下面的 SQL 用于订单支付成功后更新订单状态，通常由支付回调或支付确认流程触发。

```sql
UPDATE `biz_order`
SET
  `order_status` = 1,
  `pay_status` = 1,
  `pay_time` = NOW(),
  `updated_by` = 10001
WHERE `order_no` = 'ORDER_202605110001'
  AND `order_status` = 0
  AND `pay_status` = 0
  AND `is_deleted` = 0;
```

### 取消订单

下面的 SQL 用于取消待支付订单，适合用户主动取消或订单超时关闭场景。

```sql
UPDATE `biz_order`
SET
  `order_status` = 4,
  `cancel_time` = NOW(),
  `updated_by` = 10001
WHERE `order_no` = 'ORDER_202605110001'
  AND `order_status` = 0
  AND `pay_status` = 0
  AND `is_deleted` = 0;
```

### 更新明细状态

下面的 SQL 用于更新某一条订单明细状态，适合售后、退款、部分退货等场景。

```sql
UPDATE `biz_order_item`
SET
  `item_status` = 1
WHERE `id` = 300001
  AND `order_id` = 200001
  AND `item_status` = 0
  AND `is_deleted` = 0;
```

### 统计订单金额

下面的 SQL 用于按用户统计订单金额，适合用户消费统计、后台报表和运营分析。

```sql
SELECT
  `user_id`,
  COUNT(*) AS `order_count`,
  SUM(`total_amount`) AS `total_amount`,
  SUM(`discount_amount`) AS `discount_amount`,
  SUM(`pay_amount`) AS `pay_amount`
FROM `biz_order`
WHERE `is_deleted` = 0
  AND `order_status` IN (1, 2, 3)
GROUP BY `user_id`;
```

### 汇总订单明细金额

下面的 SQL 用于根据订单明细重新汇总订单金额，适合对账、数据修复或一致性校验。

```sql
SELECT
  `order_id`,
  SUM(`total_amount`) AS `total_amount`,
  SUM(`discount_amount`) AS `discount_amount`,
  SUM(`pay_amount`) AS `pay_amount`
FROM `biz_order_item`
WHERE `order_id` = 200001
  AND `is_deleted` = 0
GROUP BY `order_id`;
```

### 校验主从金额一致性

下面的 SQL 用于检查订单主表金额和订单明细汇总金额是否一致，适合数据核对和异常排查。

```sql
SELECT
  o.`id`,
  o.`order_no`,
  o.`total_amount` AS `order_total_amount`,
  t.`item_total_amount`,
  o.`discount_amount` AS `order_discount_amount`,
  t.`item_discount_amount`,
  o.`pay_amount` AS `order_pay_amount`,
  t.`item_pay_amount`
FROM `biz_order` o
JOIN (
  SELECT
    `order_id`,
    SUM(`total_amount`) AS `item_total_amount`,
    SUM(`discount_amount`) AS `item_discount_amount`,
    SUM(`pay_amount`) AS `item_pay_amount`
  FROM `biz_order_item`
  WHERE `is_deleted` = 0
  GROUP BY `order_id`
) t ON o.`id` = t.`order_id`
WHERE o.`is_deleted` = 0
  AND (
    o.`total_amount` <> t.`item_total_amount`
    OR o.`discount_amount` <> t.`item_discount_amount`
    OR o.`pay_amount` <> t.`item_pay_amount`
  );
```

### 逻辑删除订单

下面的 SQL 用于逻辑删除订单主表数据。实际业务中，如果允许删除订单，通常需要同时处理订单明细。

```sql
UPDATE `biz_order`
SET
  `is_deleted` = 1,
  `updated_by` = 10001
WHERE `id` = 200001
  AND `is_deleted` = 0;
```

### 逻辑删除订单明细

下面的 SQL 用于逻辑删除订单明细数据，通常需要和主表逻辑删除放在同一个事务中执行。

```sql
UPDATE `biz_order_item`
SET
  `is_deleted` = 1
WHERE `order_id` = 200001
  AND `is_deleted` = 0;
```

### 索引设计建议

主从表模型的索引设计应优先满足主表列表查询、主表详情查询、从表按主表 ID 查询、业务编号查询等核心场景。

| 表               | 索引                                                         | 适用场景                             |
| ---------------- | ------------------------------------------------------------ | ------------------------------------ |
| `biz_order`      | `PRIMARY KEY (id)`                                           | 主键查询、订单详情                   |
| `biz_order`      | `uk_order_no (order_no)`                                     | 根据订单编号查询、支付回调、幂等处理 |
| `biz_order`      | `idx_user_status_deleted (user_id, order_status, is_deleted)` | 用户订单列表查询                     |
| `biz_order`      | `idx_status_created (order_status, created_at)`              | 后台按状态分页查询                   |
| `biz_order`      | `idx_pay_status_created (pay_status, created_at)`            | 支付状态查询和统计                   |
| `biz_order_item` | `idx_order_id_deleted (order_id, is_deleted)`                | 根据订单 ID 查询明细                 |
| `biz_order_item` | `idx_order_no_deleted (order_no, is_deleted)`                | 根据订单编号查询明细                 |
| `biz_order_item` | `idx_product_id (product_id)`                                | 商品维度统计                         |
| `biz_order_item` | `idx_sku_id (sku_id)`                                        | SKU 维度统计                         |

### 使用建议

主从表模型应明确主表和从表的职责。主表保存整体信息和状态，从表保存明细信息。列表查询一般查主表，详情查询再查从表，避免在分页查询中直接关联明细表导致数据重复和分页不准确。

使用主从表模型时重点注意：

- 主表和从表需要通过稳定字段关联，优先使用 `order_id`
- 可以适当冗余 `order_no`，便于查询、排查和对账
- 创建主表和从表数据时应放在同一个事务中
- 删除主表时需要同步处理从表
- 主表金额和从表金额需要保持一致
- 分页列表尽量只查主表，不要直接关联明细表分页
- 明细表建议保存商品名称、价格等快照字段，避免商品信息变更影响历史订单
- 高频查询字段需要建立组合索引，避免后期数据量增长后查询变慢

## 一对一模型

一对一模型适用于一个业务对象的基础信息和扩展信息需要拆分存储的场景。主表保存核心、高频访问字段，扩展表保存低频、敏感、可选或体积较大的字段。两张表通常通过主表主键关联，并在扩展表中对关联字段建立唯一约束，保证一条主数据最多对应一条扩展数据。

下面以“用户基础表”和“用户资料表”为例说明一对一模型的建表方式和常用 SQL 实践。

### 适用场景

一对一模型适合主对象和扩展对象生命周期基本一致，但访问频率、字段敏感性或字段体积明显不同的场景。

常见场景包括：

- 用户基础信息与用户详细资料
- 用户账号与实名认证信息
- 员工基础信息与员工档案信息
- 商品基础信息与商品扩展信息
- 企业基础信息与企业资质信息
- 订单基础信息与订单扩展信息
- 客户基础信息与客户画像信息

### 建表 SQL

下面的 SQL 创建用户基础表和用户资料表。用户基础表保存登录、状态等高频字段，用户资料表保存头像、性别、生日、地址等扩展字段。

```sql
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `user_no` VARCHAR(64) NOT NULL COMMENT '用户编号',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `nickname` VARCHAR(64) NULL COMMENT '昵称',
  `mobile` VARCHAR(32) NULL COMMENT '手机号',
  `email` VARCHAR(128) NULL COMMENT '邮箱',
  `user_status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用',
  `last_login_time` DATETIME NULL COMMENT '最后登录时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`user_no`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_status_deleted` (`user_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户表';

CREATE TABLE `sys_user_profile` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `avatar_url` VARCHAR(500) NULL COMMENT '头像地址',
  `gender` TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0未知，1男，2女',
  `birthday` DATE NULL COMMENT '生日',
  `real_name` VARCHAR(64) NULL COMMENT '真实姓名',
  `id_card_no` VARCHAR(64) NULL COMMENT '身份证号',
  `province_code` VARCHAR(32) NULL COMMENT '省份编码',
  `city_code` VARCHAR(32) NULL COMMENT '城市编码',
  `area_code` VARCHAR(32) NULL COMMENT '区县编码',
  `address` VARCHAR(500) NULL COMMENT '详细地址',
  `profile_json` JSON NULL COMMENT '用户资料扩展信息',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_real_name` (`real_name`),
  KEY `idx_region` (`province_code`, `city_code`, `area_code`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户资料表';
```

### 字段设计说明

一对一模型的关键点是扩展表中的关联字段必须唯一。以上示例中，`sys_user_profile.user_id` 建立了唯一索引 `uk_user_id`，用于保证一个用户只能有一条用户资料。

| 表                 | 字段           | 说明                   |
| ------------------ | -------------- | ---------------------- |
| `sys_user`         | `id`           | 用户主键 ID            |
| `sys_user`         | `user_no`      | 用户编号，业务唯一标识 |
| `sys_user`         | `username`     | 用户名                 |
| `sys_user`         | `user_status`  | 用户状态               |
| `sys_user_profile` | `user_id`      | 关联用户 ID，必须唯一  |
| `sys_user_profile` | `avatar_url`   | 头像地址               |
| `sys_user_profile` | `real_name`    | 真实姓名               |
| `sys_user_profile` | `profile_json` | 低频扩展资料           |

### 新增主表数据

下面的 SQL 用于新增用户基础信息。

```sql
INSERT INTO `sys_user` (
  `id`,
  `user_no`,
  `username`,
  `nickname`,
  `mobile`,
  `email`,
  `user_status`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  'USER_202605110001',
  'zhangsan',
  '张三',
  '13800000000',
  'zhangsan@example.com',
  1,
  1,
  1
);
```

### 新增扩展表数据

下面的 SQL 用于新增用户资料信息。实际业务中，用户基础表和用户资料表可以在同一个事务中保存。

```sql
INSERT INTO `sys_user_profile` (
  `id`,
  `user_id`,
  `avatar_url`,
  `gender`,
  `birthday`,
  `real_name`,
  `id_card_no`,
  `province_code`,
  `city_code`,
  `area_code`,
  `address`,
  `profile_json`
) VALUES (
  110001,
  100001,
  'https://example.com/avatar/100001.png',
  1,
  '1995-01-01',
  '张三',
  '110101199501010011',
  '110000',
  '110100',
  '110105',
  '北京市朝阳区示例路 100 号',
  JSON_OBJECT('education', '本科', 'occupation', 'Java开发工程师')
);
```

### 查询用户基础信息

下面的 SQL 用于只查询用户基础信息，适合登录、鉴权、状态判断等高频场景。

```sql
SELECT
  `id`,
  `user_no`,
  `username`,
  `nickname`,
  `mobile`,
  `email`,
  `user_status`,
  `last_login_time`,
  `created_at`
FROM `sys_user`
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 查询用户完整信息

下面的 SQL 用于关联查询用户基础信息和用户资料信息，适合用户详情页展示。

```sql
SELECT
  u.`id`,
  u.`user_no`,
  u.`username`,
  u.`nickname`,
  u.`mobile`,
  u.`email`,
  u.`user_status`,
  p.`avatar_url`,
  p.`gender`,
  p.`birthday`,
  p.`real_name`,
  p.`province_code`,
  p.`city_code`,
  p.`area_code`,
  p.`address`,
  p.`profile_json`
FROM `sys_user` u
LEFT JOIN `sys_user_profile` p
  ON u.`id` = p.`user_id`
  AND p.`is_deleted` = 0
WHERE u.`id` = 100001
  AND u.`is_deleted` = 0;
```

### 根据用户名查询

下面的 SQL 用于根据用户名查询用户，适合登录校验和用户名唯一性校验。

```sql
SELECT
  `id`,
  `user_no`,
  `username`,
  `nickname`,
  `mobile`,
  `user_status`
FROM `sys_user`
WHERE `username` = 'zhangsan'
  AND `is_deleted` = 0;
```

### 修改用户基础信息

下面的 SQL 用于修改用户基础信息。

```sql
UPDATE `sys_user`
SET
  `nickname` = '张三丰',
  `mobile` = '13900000000',
  `email` = 'zhangsanfeng@example.com',
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 修改用户资料信息

下面的 SQL 用于修改用户资料信息。

```sql
UPDATE `sys_user_profile`
SET
  `avatar_url` = 'https://example.com/avatar/100001-new.png',
  `gender` = 1,
  `birthday` = '1995-02-01',
  `real_name` = '张三丰',
  `address` = '北京市海淀区示例路 200 号',
  `profile_json` = JSON_SET(
    COALESCE(`profile_json`, JSON_OBJECT()),
    '$.occupation',
    '后端开发工程师'
  )
WHERE `user_id` = 100001
  AND `is_deleted` = 0;
```

### 查询未完善资料的用户

下面的 SQL 用于查询没有资料记录或资料不完整的用户，适合运营提醒和数据补全。

```sql
SELECT
  u.`id`,
  u.`user_no`,
  u.`username`,
  u.`nickname`,
  u.`mobile`
FROM `sys_user` u
LEFT JOIN `sys_user_profile` p
  ON u.`id` = p.`user_id`
  AND p.`is_deleted` = 0
WHERE u.`is_deleted` = 0
  AND (
    p.`id` IS NULL
    OR p.`real_name` IS NULL
    OR p.`birthday` IS NULL
  );
```

### 禁用用户

下面的 SQL 用于禁用用户账号，适合后台账号管理场景。

```sql
UPDATE `sys_user`
SET
  `user_status` = 0,
  `updated_by` = 1
WHERE `id` = 100001
  AND `user_status` = 1
  AND `is_deleted` = 0;
```

### 逻辑删除用户

下面的 SQL 用于逻辑删除用户基础信息。

```sql
UPDATE `sys_user`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 逻辑删除用户资料

下面的 SQL 用于逻辑删除用户资料。实际业务中，删除用户和删除用户资料通常应放在同一个事务中执行。

```sql
UPDATE `sys_user_profile`
SET
  `is_deleted` = 1
WHERE `user_id` = 100001
  AND `is_deleted` = 0;
```

### 索引设计建议

一对一模型的索引重点是主表唯一业务字段、扩展表唯一关联字段和高频查询字段。

| 表                 | 索引                                               | 适用场景             |
| ------------------ | -------------------------------------------------- | -------------------- |
| `sys_user`         | `PRIMARY KEY (id)`                                 | 用户详情查询         |
| `sys_user`         | `uk_user_no (user_no)`                             | 根据用户编号查询     |
| `sys_user`         | `uk_username (username)`                           | 登录查询、唯一性校验 |
| `sys_user`         | `idx_mobile (mobile)`                              | 根据手机号查询       |
| `sys_user_profile` | `uk_user_id (user_id)`                             | 保证一对一关系       |
| `sys_user_profile` | `idx_region (province_code, city_code, area_code)` | 按地区筛选用户资料   |

### 使用建议

一对一模型适合拆分高频字段和低频字段，也适合隔离敏感信息或大字段。设计时要注意扩展表关联字段必须唯一，否则会退化成一对多模型。

使用一对一模型时重点注意：

- 扩展表的关联字段必须建立唯一索引
- 高频字段放主表，低频字段放扩展表
- 敏感字段可以放扩展表，便于权限隔离
- 主表和扩展表生命周期通常保持一致
- 创建和删除主扩展数据时建议使用事务
- 查询列表时优先查主表，详情页再关联扩展表
- 不要因为字段稍多就盲目拆表，应根据访问频率和业务边界判断

## 一对多模型

一对多模型适用于一个主对象可以关联多个子对象，但子对象本身具有相对独立业务意义的场景。它和主从表模型相似，但主从表更偏业务单据与明细行，一对多模型更强调主体对象和从属对象之间的归属关系。

下面以“部门表”和“员工表”为例说明一对多模型的建表方式和常用 SQL 实践。

### 适用场景

一对多模型适合一个业务主体下面存在多条从属数据的场景。从属数据通常可以单独查询、单独维护，也可能被其他业务引用。

常见场景包括：

- 部门与员工
- 分类与商品
- 用户与地址
- 用户与银行卡
- 客户与联系人
- 文章与评论
- 课程与章节
- 店铺与商品
- 项目与任务
- 企业与员工

### 建表 SQL

下面的 SQL 创建部门表和员工表。一个部门可以有多个员工，一个员工只归属于一个部门。

```sql
CREATE TABLE `sys_department` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `dept_code` VARCHAR(64) NOT NULL COMMENT '部门编码',
  `dept_name` VARCHAR(128) NOT NULL COMMENT '部门名称',
  `dept_status` TINYINT NOT NULL DEFAULT 1 COMMENT '部门状态：0停用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_status_deleted` (`dept_status`, `is_deleted`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统部门表';

CREATE TABLE `sys_employee` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `employee_no` VARCHAR(64) NOT NULL COMMENT '员工编号',
  `dept_id` BIGINT NOT NULL COMMENT '部门ID',
  `employee_name` VARCHAR(64) NOT NULL COMMENT '员工姓名',
  `mobile` VARCHAR(32) NULL COMMENT '手机号',
  `email` VARCHAR(128) NULL COMMENT '邮箱',
  `job_title` VARCHAR(128) NULL COMMENT '岗位名称',
  `employee_status` TINYINT NOT NULL DEFAULT 1 COMMENT '员工状态：0离职，1在职，2冻结',
  `entry_date` DATE NULL COMMENT '入职日期',
  `leave_date` DATE NULL COMMENT '离职日期',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_no` (`employee_no`),
  KEY `idx_dept_status_deleted` (`dept_id`, `employee_status`, `is_deleted`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_entry_date` (`entry_date`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统员工表';
```

### 字段设计说明

一对多模型的关键点是多方表保存一方表的主键。以上示例中，`sys_employee.dept_id` 表示员工所属部门，一个部门可以关联多个员工。

| 表               | 字段              | 说明        |
| ---------------- | ----------------- | ----------- |
| `sys_department` | `id`              | 部门主键 ID |
| `sys_department` | `dept_code`       | 部门编码    |
| `sys_department` | `dept_name`       | 部门名称    |
| `sys_employee`   | `dept_id`         | 关联部门 ID |
| `sys_employee`   | `employee_no`     | 员工编号    |
| `sys_employee`   | `employee_name`   | 员工姓名    |
| `sys_employee`   | `employee_status` | 员工状态    |

### 新增部门

下面的 SQL 用于新增部门数据。

```sql
INSERT INTO `sys_department` (
  `id`,
  `dept_code`,
  `dept_name`,
  `dept_status`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  200001,
  'DEPT_RD',
  '研发部',
  1,
  100,
  '负责产品研发',
  1,
  1
);
```

### 新增员工

下面的 SQL 用于新增员工数据，并通过 `dept_id` 归属到指定部门。

```sql
INSERT INTO `sys_employee` (
  `id`,
  `employee_no`,
  `dept_id`,
  `employee_name`,
  `mobile`,
  `email`,
  `job_title`,
  `employee_status`,
  `entry_date`,
  `created_by`,
  `updated_by`
) VALUES
(
  210001,
  'EMP_202605110001',
  200001,
  '李四',
  '13800000001',
  'lisi@example.com',
  'Java开发工程师',
  1,
  '2024-03-01',
  1,
  1
),
(
  210002,
  'EMP_202605110002',
  200001,
  '王五',
  '13800000002',
  'wangwu@example.com',
  '前端开发工程师',
  1,
  '2024-04-01',
  1,
  1
);
```

### 查询部门详情

下面的 SQL 用于查询部门详情。

```sql
SELECT
  `id`,
  `dept_code`,
  `dept_name`,
  `dept_status`,
  `sort_order`,
  `remark`,
  `created_at`,
  `updated_at`
FROM `sys_department`
WHERE `id` = 200001
  AND `is_deleted` = 0;
```

### 查询部门员工列表

下面的 SQL 用于查询某个部门下的员工列表。

```sql
SELECT
  `id`,
  `employee_no`,
  `dept_id`,
  `employee_name`,
  `mobile`,
  `email`,
  `job_title`,
  `employee_status`,
  `entry_date`
FROM `sys_employee`
WHERE `dept_id` = 200001
  AND `employee_status` = 1
  AND `is_deleted` = 0
ORDER BY `entry_date` DESC;
```

### 关联查询员工和部门

下面的 SQL 用于查询员工列表并带出部门名称，适合后台员工管理列表。

```sql
SELECT
  e.`id`,
  e.`employee_no`,
  e.`employee_name`,
  e.`mobile`,
  e.`email`,
  e.`job_title`,
  e.`employee_status`,
  e.`entry_date`,
  d.`dept_code`,
  d.`dept_name`
FROM `sys_employee` e
JOIN `sys_department` d
  ON e.`dept_id` = d.`id`
  AND d.`is_deleted` = 0
WHERE e.`is_deleted` = 0
  AND e.`employee_status` = 1
ORDER BY e.`created_at` DESC
LIMIT 10 OFFSET 0;
```

### 查询部门员工数量

下面的 SQL 用于统计每个部门的员工数量，适合组织管理和报表统计。

```sql
SELECT
  d.`id`,
  d.`dept_code`,
  d.`dept_name`,
  COUNT(e.`id`) AS `employee_count`
FROM `sys_department` d
LEFT JOIN `sys_employee` e
  ON d.`id` = e.`dept_id`
  AND e.`employee_status` = 1
  AND e.`is_deleted` = 0
WHERE d.`is_deleted` = 0
GROUP BY
  d.`id`,
  d.`dept_code`,
  d.`dept_name`
ORDER BY d.`sort_order` DESC;
```

### 查询没有员工的部门

下面的 SQL 用于查询没有在职员工的部门，适合组织清理和数据检查。

```sql
SELECT
  d.`id`,
  d.`dept_code`,
  d.`dept_name`
FROM `sys_department` d
LEFT JOIN `sys_employee` e
  ON d.`id` = e.`dept_id`
  AND e.`employee_status` = 1
  AND e.`is_deleted` = 0
WHERE d.`is_deleted` = 0
  AND e.`id` IS NULL;
```

### 员工转部门

下面的 SQL 用于将员工调整到新的部门。

```sql
UPDATE `sys_employee`
SET
  `dept_id` = 200002,
  `updated_by` = 1
WHERE `id` = 210001
  AND `employee_status` = 1
  AND `is_deleted` = 0;
```

### 禁用部门

下面的 SQL 用于停用部门。实际业务中，停用部门前通常需要先检查是否存在在职员工。

```sql
UPDATE `sys_department`
SET
  `dept_status` = 0,
  `updated_by` = 1
WHERE `id` = 200001
  AND `dept_status` = 1
  AND `is_deleted` = 0;
```

### 员工离职

下面的 SQL 用于将员工状态改为离职。

```sql
UPDATE `sys_employee`
SET
  `employee_status` = 0,
  `leave_date` = CURDATE(),
  `updated_by` = 1
WHERE `id` = 210001
  AND `employee_status` = 1
  AND `is_deleted` = 0;
```

### 逻辑删除部门

下面的 SQL 用于逻辑删除部门。删除前应先确认部门下不存在有效员工。

```sql
UPDATE `sys_department`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 200001
  AND `is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_employee` e
    WHERE e.`dept_id` = 200001
      AND e.`employee_status` = 1
      AND e.`is_deleted` = 0
  );
```

### 逻辑删除员工

下面的 SQL 用于逻辑删除员工数据。

```sql
UPDATE `sys_employee`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 210001
  AND `is_deleted` = 0;
```

### 索引设计建议

一对多模型的索引重点是多方表的外键字段，以及一方表和多方表的高频查询条件。

| 表               | 索引                                                         | 适用场景             |
| ---------------- | ------------------------------------------------------------ | -------------------- |
| `sys_department` | `PRIMARY KEY (id)`                                           | 部门详情查询         |
| `sys_department` | `uk_dept_code (dept_code)`                                   | 根据部门编码查询     |
| `sys_department` | `idx_status_deleted (dept_status, is_deleted)`               | 查询启用部门         |
| `sys_employee`   | `PRIMARY KEY (id)`                                           | 员工详情查询         |
| `sys_employee`   | `uk_employee_no (employee_no)`                               | 根据员工编号查询     |
| `sys_employee`   | `idx_dept_status_deleted (dept_id, employee_status, is_deleted)` | 查询部门下员工       |
| `sys_employee`   | `idx_entry_date (entry_date)`                                | 按入职时间查询或排序 |

### 使用建议

一对多模型设计时，应把关联字段放在多方表中。查询一方数据时，不应默认总是关联多方表，只有详情、统计或业务明确需要时再关联查询。

使用一对多模型时重点注意：

- 多方表必须保存一方表主键作为关联字段
- 多方表的关联字段需要建立索引
- 删除一方数据前，应检查是否存在有效多方数据
- 一方列表查询不建议直接关联多方表
- 多方数据量较大时，应优先按关联字段分页查询
- 是否使用物理外键要结合团队规范和业务并发情况决定
- 需要保留历史快照时，可以在多方表冗余一方的关键名称字段

## 多对多模型

多对多模型适用于两个业务对象之间可以互相关联多条数据的场景。它通常需要通过一张中间关系表来表达关系，不能直接在任意一方表中保存多个 ID 字符串。

下面以“用户表”“角色表”和“用户角色关系表”为例说明多对多模型的建表方式和常用 SQL 实践。

### 适用场景

多对多模型适合两个对象之间彼此都可能存在多条关联关系的场景。

常见场景包括：

- 用户与角色
- 角色与权限
- 学生与课程
- 商品与标签
- 文章与标签
- 用户与岗位
- 员工与项目
- 菜单与按钮权限
- 客户与销售人员
- 活动与参与用户

### 建表 SQL

下面的 SQL 创建用户表、角色表和用户角色关系表。一个用户可以拥有多个角色，一个角色也可以分配给多个用户。

```sql
CREATE TABLE `sys_auth_user` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `user_no` VARCHAR(64) NOT NULL COMMENT '用户编号',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `nickname` VARCHAR(64) NULL COMMENT '昵称',
  `user_status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`user_no`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status_deleted` (`user_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='权限用户表';

CREATE TABLE `sys_auth_role` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(128) NOT NULL COMMENT '角色名称',
  `role_status` TINYINT NOT NULL DEFAULT 1 COMMENT '角色状态：0禁用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_status_deleted` (`role_status`, `is_deleted`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='权限角色表';

CREATE TABLE `sys_auth_user_role` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `relation_status` TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态：0禁用，1启用',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role_deleted` (`user_id`, `role_id`, `is_deleted`),
  KEY `idx_user_status_deleted` (`user_id`, `relation_status`, `is_deleted`),
  KEY `idx_role_status_deleted` (`role_id`, `relation_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='权限用户角色关系表';
```

### 字段设计说明

多对多模型的核心是关系表。关系表至少应包含两端对象的主键，并根据业务需要增加状态、排序、备注、创建时间等字段。

| 表                   | 字段              | 说明             |
| -------------------- | ----------------- | ---------------- |
| `sys_auth_user`      | `id`              | 用户主键 ID      |
| `sys_auth_role`      | `id`              | 角色主键 ID      |
| `sys_auth_user_role` | `user_id`         | 关联用户 ID      |
| `sys_auth_user_role` | `role_id`         | 关联角色 ID      |
| `sys_auth_user_role` | `relation_status` | 用户角色关系状态 |
| `sys_auth_user_role` | `is_deleted`      | 逻辑删除字段     |

### 新增用户

下面的 SQL 用于新增用户数据。

```sql
INSERT INTO `sys_auth_user` (
  `id`,
  `user_no`,
  `username`,
  `nickname`,
  `user_status`,
  `created_by`,
  `updated_by`
) VALUES (
  300001,
  'USER_202605110002',
  'lisi',
  '李四',
  1,
  1,
  1
);
```

### 新增角色

下面的 SQL 用于新增角色数据。

```sql
INSERT INTO `sys_auth_role` (
  `id`,
  `role_code`,
  `role_name`,
  `role_status`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  310001,
  'ADMIN',
  '系统管理员',
  1,
  100,
  '拥有系统管理权限',
  1,
  1
),
(
  310002,
  'DEVELOPER',
  '开发人员',
  1,
  90,
  '拥有开发相关权限',
  1,
  1
);
```

### 分配用户角色

下面的 SQL 用于给用户分配角色。关系表通过唯一索引避免同一个用户重复分配同一个有效角色。

```sql
INSERT INTO `sys_auth_user_role` (
  `id`,
  `user_id`,
  `role_id`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  320001,
  300001,
  310001,
  1,
  '分配系统管理员角色',
  1,
  1
),
(
  320002,
  300001,
  310002,
  1,
  '分配开发人员角色',
  1,
  1
);
```

### 查询用户拥有的角色

下面的 SQL 用于查询某个用户拥有的角色，适合登录后加载权限、后台用户详情展示等场景。

```sql
SELECT
  r.`id`,
  r.`role_code`,
  r.`role_name`,
  r.`role_status`,
  r.`sort_order`
FROM `sys_auth_user_role` ur
JOIN `sys_auth_role` r
  ON ur.`role_id` = r.`id`
  AND r.`is_deleted` = 0
WHERE ur.`user_id` = 300001
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
  AND r.`role_status` = 1
ORDER BY r.`sort_order` DESC;
```

### 查询角色下的用户

下面的 SQL 用于查询某个角色下的用户列表，适合角色详情页和权限管理页面。

```sql
SELECT
  u.`id`,
  u.`user_no`,
  u.`username`,
  u.`nickname`,
  u.`user_status`
FROM `sys_auth_user_role` ur
JOIN `sys_auth_user` u
  ON ur.`user_id` = u.`id`
  AND u.`is_deleted` = 0
WHERE ur.`role_id` = 310001
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
  AND u.`user_status` = 1
ORDER BY u.`created_at` DESC
LIMIT 10 OFFSET 0;
```

### 查询用户和角色完整关系

下面的 SQL 用于查询用户与角色的完整关联关系，适合后台关系维护页面。

```sql
SELECT
  ur.`id` AS `relation_id`,
  u.`id` AS `user_id`,
  u.`username`,
  u.`nickname`,
  r.`id` AS `role_id`,
  r.`role_code`,
  r.`role_name`,
  ur.`relation_status`,
  ur.`created_at`
FROM `sys_auth_user_role` ur
JOIN `sys_auth_user` u
  ON ur.`user_id` = u.`id`
  AND u.`is_deleted` = 0
JOIN `sys_auth_role` r
  ON ur.`role_id` = r.`id`
  AND r.`is_deleted` = 0
WHERE ur.`is_deleted` = 0
ORDER BY ur.`created_at` DESC
LIMIT 10 OFFSET 0;
```

### 判断用户是否拥有指定角色

下面的 SQL 用于判断用户是否拥有某个角色，适合接口鉴权和权限判断。

```sql
SELECT
  COUNT(*) AS `has_role`
FROM `sys_auth_user_role` ur
JOIN `sys_auth_role` r
  ON ur.`role_id` = r.`id`
  AND r.`is_deleted` = 0
WHERE ur.`user_id` = 300001
  AND r.`role_code` = 'ADMIN'
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
  AND r.`role_status` = 1;
```

### 批量分配角色

下面的 SQL 用于给一个用户批量分配多个角色。

```sql
INSERT INTO `sys_auth_user_role` (
  `id`,
  `user_id`,
  `role_id`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  320003,
  300001,
  310003,
  1,
  '批量分配角色',
  1,
  1
),
(
  320004,
  300001,
  310004,
  1,
  '批量分配角色',
  1,
  1
);
```

### 移除用户角色

下面的 SQL 用于移除用户和角色之间的关系，通常采用逻辑删除。

```sql
UPDATE `sys_auth_user_role`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `user_id` = 300001
  AND `role_id` = 310001
  AND `is_deleted` = 0;
```

### 禁用用户角色关系

下面的 SQL 用于临时禁用用户角色关系，不删除关系数据。

```sql
UPDATE `sys_auth_user_role`
SET
  `relation_status` = 0,
  `updated_by` = 1
WHERE `user_id` = 300001
  AND `role_id` = 310001
  AND `relation_status` = 1
  AND `is_deleted` = 0;
```

### 查询没有角色的用户

下面的 SQL 用于查询未分配任何有效角色的用户，适合权限初始化检查。

```sql
SELECT
  u.`id`,
  u.`user_no`,
  u.`username`,
  u.`nickname`
FROM `sys_auth_user` u
LEFT JOIN `sys_auth_user_role` ur
  ON u.`id` = ur.`user_id`
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
WHERE u.`is_deleted` = 0
  AND u.`user_status` = 1
  AND ur.`id` IS NULL;
```

### 统计角色用户数量

下面的 SQL 用于统计每个角色下的有效用户数量。

```sql
SELECT
  r.`id`,
  r.`role_code`,
  r.`role_name`,
  COUNT(ur.`user_id`) AS `user_count`
FROM `sys_auth_role` r
LEFT JOIN `sys_auth_user_role` ur
  ON r.`id` = ur.`role_id`
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
LEFT JOIN `sys_auth_user` u
  ON ur.`user_id` = u.`id`
  AND u.`user_status` = 1
  AND u.`is_deleted` = 0
WHERE r.`is_deleted` = 0
GROUP BY
  r.`id`,
  r.`role_code`,
  r.`role_name`
ORDER BY r.`sort_order` DESC;
```

### 查询同时拥有多个角色的用户

下面的 SQL 用于查询同时拥有指定多个角色的用户，适合权限筛选和运营分组。

```sql
SELECT
  u.`id`,
  u.`user_no`,
  u.`username`,
  u.`nickname`
FROM `sys_auth_user` u
JOIN `sys_auth_user_role` ur
  ON u.`id` = ur.`user_id`
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
JOIN `sys_auth_role` r
  ON ur.`role_id` = r.`id`
  AND r.`role_status` = 1
  AND r.`is_deleted` = 0
WHERE u.`user_status` = 1
  AND u.`is_deleted` = 0
  AND r.`role_code` IN ('ADMIN', 'DEVELOPER')
GROUP BY
  u.`id`,
  u.`user_no`,
  u.`username`,
  u.`nickname`
HAVING COUNT(DISTINCT r.`role_code`) = 2;
```

### 索引设计建议

多对多模型的索引重点在关系表。关系表通常需要同时支持从 A 查 B、从 B 查 A，因此两端关联字段都需要设计索引。

| 表                   | 索引                                                         | 适用场景             |
| -------------------- | ------------------------------------------------------------ | -------------------- |
| `sys_auth_user`      | `PRIMARY KEY (id)`                                           | 用户详情查询         |
| `sys_auth_user`      | `uk_username (username)`                                     | 登录查询、唯一性校验 |
| `sys_auth_role`      | `PRIMARY KEY (id)`                                           | 角色详情查询         |
| `sys_auth_role`      | `uk_role_code (role_code)`                                   | 根据角色编码查询     |
| `sys_auth_user_role` | `uk_user_role_deleted (user_id, role_id, is_deleted)`        | 防止重复分配有效关系 |
| `sys_auth_user_role` | `idx_user_status_deleted (user_id, relation_status, is_deleted)` | 查询用户角色         |
| `sys_auth_user_role` | `idx_role_status_deleted (role_id, relation_status, is_deleted)` | 查询角色用户         |

### 使用建议

多对多关系不要使用逗号分隔字符串保存多个 ID，例如 `role_ids = '1,2,3'`。这种方式难以建立有效索引，也不利于关联查询、去重、统计和数据维护。标准做法是使用中间关系表。

使用多对多模型时重点注意：

- 必须使用中间关系表表达多对多关系
- 关系表需要防止重复关系
- 关系表两端字段都需要建立索引
- 关系表可以保留状态、排序、备注等业务字段
- 查询用户角色和查询角色用户是两个不同方向，都要考虑索引
- 批量分配关系时要注意唯一约束冲突
- 删除关系优先使用逻辑删除，便于审计和恢复
- 不要用 JSON 数组或逗号字符串保存多个关联 ID

## 字典表模型

字典表模型用于管理系统中稳定、可枚举、可配置的业务值，例如状态、类型、来源、性别、支付方式、订单类型等。字典表通常分为字典类型表和字典数据表，类型表定义字典分类，数据表定义具体字典项。

下面以“字典类型表”和“字典数据表”为例说明字典表模型的建表方式和常用 SQL 实践。

### 适用场景

字典表模型适合管理业务中需要统一维护、统一展示、统一翻译的枚举值。它可以避免状态值、类型值散落在代码、SQL 和页面中。

常见场景包括：

- 用户状态
- 订单状态
- 支付方式
- 性别类型
- 客户来源
- 商品类型
- 审批状态
- 日志类型
- 是否标识
- 系统开关类型

### 建表 SQL

下面的 SQL 创建字典类型表和字典数据表。字典类型表用于定义字典分类，字典数据表用于定义具体字典项。

```sql
CREATE TABLE `sys_dict_type` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `dict_code` VARCHAR(64) NOT NULL COMMENT '字典编码',
  `dict_name` VARCHAR(128) NOT NULL COMMENT '字典名称',
  `dict_status` TINYINT NOT NULL DEFAULT 1 COMMENT '字典状态：0停用，1启用',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`),
  KEY `idx_status_deleted` (`dict_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统字典类型表';

CREATE TABLE `sys_dict_data` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `dict_type_id` BIGINT NOT NULL COMMENT '字典类型ID',
  `dict_code` VARCHAR(64) NOT NULL COMMENT '字典编码，冗余字段',
  `dict_label` VARCHAR(128) NOT NULL COMMENT '字典标签',
  `dict_value` VARCHAR(128) NOT NULL COMMENT '字典值',
  `dict_status` TINYINT NOT NULL DEFAULT 1 COMMENT '字典项状态：0停用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `css_class` VARCHAR(128) NULL COMMENT '样式类名',
  `list_class` VARCHAR(128) NULL COMMENT '回显样式',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认：0否，1是',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_value_deleted` (`dict_code`, `dict_value`, `is_deleted`),
  KEY `idx_type_status_deleted` (`dict_type_id`, `dict_status`, `is_deleted`),
  KEY `idx_code_status_deleted` (`dict_code`, `dict_status`, `is_deleted`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统字典数据表';
```

### 字段设计说明

字典表模型通常需要字典编码、字典标签、字典值、排序、状态等字段。字典编码用于区分不同字典类型，字典值用于业务存储，字典标签用于页面展示。

| 表              | 字段           | 说明                          |
| --------------- | -------------- | ----------------------------- |
| `sys_dict_type` | `dict_code`    | 字典编码，例如 `order_status` |
| `sys_dict_type` | `dict_name`    | 字典名称，例如订单状态        |
| `sys_dict_data` | `dict_type_id` | 字典类型 ID                   |
| `sys_dict_data` | `dict_code`    | 冗余字典编码，便于查询        |
| `sys_dict_data` | `dict_label`   | 页面展示文本                  |
| `sys_dict_data` | `dict_value`   | 业务实际存储值                |
| `sys_dict_data` | `sort_order`   | 排序字段                      |
| `sys_dict_data` | `is_default`   | 是否默认值                    |

### 新增字典类型

下面的 SQL 用于新增一个订单状态字典类型。

```sql
INSERT INTO `sys_dict_type` (
  `id`,
  `dict_code`,
  `dict_name`,
  `dict_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  400001,
  'order_status',
  '订单状态',
  1,
  '订单业务状态字典',
  1,
  1
);
```

### 新增字典数据

下面的 SQL 用于新增订单状态字典项。

```sql
INSERT INTO `sys_dict_data` (
  `id`,
  `dict_type_id`,
  `dict_code`,
  `dict_label`,
  `dict_value`,
  `dict_status`,
  `sort_order`,
  `list_class`,
  `is_default`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  410001,
  400001,
  'order_status',
  '待支付',
  '0',
  1,
  100,
  'warning',
  1,
  '订单已创建但未支付',
  1,
  1
),
(
  410002,
  400001,
  'order_status',
  '已支付',
  '1',
  1,
  90,
  'success',
  0,
  '订单已支付',
  1,
  1
),
(
  410003,
  400001,
  'order_status',
  '已取消',
  '2',
  1,
  80,
  'danger',
  0,
  '订单已取消',
  1,
  1
);
```

### 查询字典类型列表

下面的 SQL 用于分页查询字典类型，适合后台字典管理页面。

```sql
SELECT
  `id`,
  `dict_code`,
  `dict_name`,
  `dict_status`,
  `remark`,
  `created_at`,
  `updated_at`
FROM `sys_dict_type`
WHERE `is_deleted` = 0
ORDER BY `created_at` DESC
LIMIT 10 OFFSET 0;
```

### 根据字典编码查询字典项

下面的 SQL 用于根据字典编码查询启用的字典项，适合前端下拉框、单选框和状态回显。

```sql
SELECT
  `dict_label`,
  `dict_value`,
  `list_class`,
  `css_class`,
  `is_default`
FROM `sys_dict_data`
WHERE `dict_code` = 'order_status'
  AND `dict_status` = 1
  AND `is_deleted` = 0
ORDER BY `sort_order` DESC, `id` ASC;
```

### 根据字典值查询标签

下面的 SQL 用于根据业务值查询展示标签，适合列表页面状态翻译。

```sql
SELECT
  `dict_label`,
  `list_class`
FROM `sys_dict_data`
WHERE `dict_code` = 'order_status'
  AND `dict_value` = '1'
  AND `dict_status` = 1
  AND `is_deleted` = 0;
```

### 查询默认字典项

下面的 SQL 用于查询某个字典类型的默认值，适合新增表单初始化。

```sql
SELECT
  `dict_label`,
  `dict_value`
FROM `sys_dict_data`
WHERE `dict_code` = 'order_status'
  AND `is_default` = 1
  AND `dict_status` = 1
  AND `is_deleted` = 0
LIMIT 1;
```

### 修改字典类型

下面的 SQL 用于修改字典类型名称和状态。

```sql
UPDATE `sys_dict_type`
SET
  `dict_name` = '订单业务状态',
  `dict_status` = 1,
  `remark` = '订单全流程状态字典',
  `updated_by` = 1
WHERE `id` = 400001
  AND `is_deleted` = 0;
```

### 修改字典数据

下面的 SQL 用于修改字典标签、排序和样式。

```sql
UPDATE `sys_dict_data`
SET
  `dict_label` = '支付成功',
  `sort_order` = 95,
  `list_class` = 'success',
  `remark` = '订单支付成功状态',
  `updated_by` = 1
WHERE `id` = 410002
  AND `is_deleted` = 0;
```

### 停用字典项

下面的 SQL 用于停用某个字典项。停用后业务数据仍然保留原值，但前端通常不再展示该选项。

```sql
UPDATE `sys_dict_data`
SET
  `dict_status` = 0,
  `updated_by` = 1
WHERE `id` = 410003
  AND `dict_status` = 1
  AND `is_deleted` = 0;
```

### 逻辑删除字典类型

下面的 SQL 用于逻辑删除字典类型。实际业务中，删除字典类型前应先确认是否存在有效字典项。

```sql
UPDATE `sys_dict_type`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 400001
  AND `is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_dict_data` d
    WHERE d.`dict_type_id` = 400001
      AND d.`is_deleted` = 0
  );
```

### 逻辑删除字典数据

下面的 SQL 用于逻辑删除字典项。

```sql
UPDATE `sys_dict_data`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 410003
  AND `is_deleted` = 0;
```

### 查询业务数据并回显字典标签

下面的 SQL 演示业务表状态字段和字典表关联查询，用于把订单状态值翻译成页面展示文本。

```sql
SELECT
  o.`id`,
  o.`order_no`,
  o.`order_status`,
  d.`dict_label` AS `order_status_label`,
  d.`list_class` AS `order_status_class`,
  o.`pay_amount`,
  o.`created_at`
FROM `biz_order` o
LEFT JOIN `sys_dict_data` d
  ON d.`dict_code` = 'order_status'
  AND d.`dict_value` = CAST(o.`order_status` AS CHAR)
  AND d.`dict_status` = 1
  AND d.`is_deleted` = 0
WHERE o.`is_deleted` = 0
ORDER BY o.`created_at` DESC
LIMIT 10 OFFSET 0;
```

### 查询重复字典值

下面的 SQL 用于检查同一字典编码下是否存在重复字典值，适合数据治理和上线前检查。

```sql
SELECT
  `dict_code`,
  `dict_value`,
  COUNT(*) AS `repeat_count`
FROM `sys_dict_data`
WHERE `is_deleted` = 0
GROUP BY
  `dict_code`,
  `dict_value`
HAVING COUNT(*) > 1;
```

### 索引设计建议

字典表模型的索引重点是字典编码、字典值、状态和逻辑删除字段。字典查询频率较高，但数据量通常不大，索引设计应保持简洁。

| 表              | 索引                                                         | 适用场景                   |
| --------------- | ------------------------------------------------------------ | -------------------------- |
| `sys_dict_type` | `PRIMARY KEY (id)`                                           | 字典类型详情查询           |
| `sys_dict_type` | `uk_dict_code (dict_code)`                                   | 根据字典编码查询类型       |
| `sys_dict_type` | `idx_status_deleted (dict_status, is_deleted)`               | 查询启用字典类型           |
| `sys_dict_data` | `uk_code_value_deleted (dict_code, dict_value, is_deleted)`  | 防止同一字典下字典值重复   |
| `sys_dict_data` | `idx_type_status_deleted (dict_type_id, dict_status, is_deleted)` | 根据字典类型 ID 查询字典项 |
| `sys_dict_data` | `idx_code_status_deleted (dict_code, dict_status, is_deleted)` | 根据字典编码查询启用字典项 |

### 使用建议

字典表模型适合管理可枚举、可配置、变化频率不高的业务值。对于强业务流程状态，可以配合状态机模型使用；对于纯代码内部逻辑常量，不一定需要进入字典表。

使用字典表模型时重点注意：

- 字典编码应稳定，不要频繁修改
- 业务表通常保存字典值，不保存字典标签
- 字典标签用于展示，可以根据业务调整
- 字典值不要随意变更，否则会影响历史业务数据
- 字典数据适合缓存，减少频繁查询数据库
- 不建议用字典表承载复杂业务规则
- 状态流转规则不应只靠字典表表达，应使用状态机模型或代码规则控制

## 状态机模型

状态机模型用于管理业务对象的状态流转规则。它不仅记录当前状态，还明确哪些状态可以流转到哪些状态、由谁触发、是否需要校验、是否需要记录日志。状态机模型常用于订单、审批、工单、售后、任务、合同等具有明确生命周期的业务。

下面以“工单表”“状态流转规则表”和“状态变更日志表”为例说明状态机模型的建表方式和常用 SQL 实践。

### 适用场景

状态机模型适合业务状态较多、状态流转有约束、需要审计状态变更记录的场景。如果业务只是简单启用、停用，可以使用普通状态字段；如果状态之间存在明确流转路径，就适合使用状态机模型。

常见场景包括：

- 订单状态流转
- 审批流程状态流转
- 工单处理状态流转
- 售后单状态流转
- 任务状态流转
- 合同状态流转
- 发票状态流转
- 发布流程状态流转
- 内容审核状态流转

### 建表 SQL

下面的 SQL 创建工单表、状态流转规则表和状态变更日志表。工单表保存当前状态，状态流转规则表定义允许的状态变化，状态变更日志表记录每次状态变化。

```sql
CREATE TABLE `biz_work_order` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `work_order_no` VARCHAR(64) NOT NULL COMMENT '工单编号',
  `work_order_title` VARCHAR(200) NOT NULL COMMENT '工单标题',
  `work_order_type` TINYINT NOT NULL DEFAULT 1 COMMENT '工单类型：1咨询，2故障，3投诉',
  `current_status` TINYINT NOT NULL DEFAULT 0 COMMENT '当前状态：0待提交，1待处理，2处理中，3待确认，4已完成，5已关闭',
  `priority_level` TINYINT NOT NULL DEFAULT 2 COMMENT '优先级：1低，2中，3高，4紧急',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
  `handler_id` BIGINT NULL COMMENT '处理人ID',
  `submit_time` DATETIME NULL COMMENT '提交时间',
  `handle_time` DATETIME NULL COMMENT '开始处理时间',
  `finish_time` DATETIME NULL COMMENT '完成时间',
  `close_time` DATETIME NULL COMMENT '关闭时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order_no` (`work_order_no`),
  KEY `idx_status_deleted` (`current_status`, `is_deleted`),
  KEY `idx_applicant_status_deleted` (`applicant_id`, `current_status`, `is_deleted`),
  KEY `idx_handler_status_deleted` (`handler_id`, `current_status`, `is_deleted`),
  KEY `idx_type_priority_created` (`work_order_type`, `priority_level`, `created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='业务工单表';

CREATE TABLE `biz_status_transition` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型，例如 work_order',
  `from_status` TINYINT NOT NULL COMMENT '来源状态',
  `to_status` TINYINT NOT NULL COMMENT '目标状态',
  `action_code` VARCHAR(64) NOT NULL COMMENT '动作编码，例如 submit、accept、finish、close',
  `action_name` VARCHAR(128) NOT NULL COMMENT '动作名称',
  `transition_status` TINYINT NOT NULL DEFAULT 1 COMMENT '规则状态：0停用，1启用',
  `need_remark` TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要备注：0否，1是',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_from_action_deleted` (`biz_type`, `from_status`, `action_code`, `is_deleted`),
  KEY `idx_biz_from_status` (`biz_type`, `from_status`, `transition_status`, `is_deleted`),
  KEY `idx_biz_to_status` (`biz_type`, `to_status`, `transition_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='业务状态流转规则表';

CREATE TABLE `biz_status_log` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型，例如 work_order',
  `biz_id` BIGINT NOT NULL COMMENT '业务ID',
  `biz_no` VARCHAR(64) NOT NULL COMMENT '业务编号',
  `from_status` TINYINT NOT NULL COMMENT '来源状态',
  `to_status` TINYINT NOT NULL COMMENT '目标状态',
  `action_code` VARCHAR(64) NOT NULL COMMENT '动作编码',
  `action_name` VARCHAR(128) NOT NULL COMMENT '动作名称',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(128) NULL COMMENT '操作人名称',
  `operate_remark` VARCHAR(500) NULL COMMENT '操作备注',
  `operate_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz_id_time` (`biz_type`, `biz_id`, `operate_time`),
  KEY `idx_biz_no_time` (`biz_type`, `biz_no`, `operate_time`),
  KEY `idx_operator_time` (`operator_id`, `operate_time`),
  KEY `idx_action_time` (`action_code`, `operate_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='业务状态变更日志表';
```

### 字段设计说明

状态机模型通常由业务主表、状态流转规则表和状态日志表组成。业务主表保存当前状态，规则表定义允许的动作和状态变化，日志表记录每次状态变更过程。

| 表                      | 字段             | 说明         |
| ----------------------- | ---------------- | ------------ |
| `biz_work_order`        | `current_status` | 当前状态     |
| `biz_status_transition` | `biz_type`       | 业务类型     |
| `biz_status_transition` | `from_status`    | 来源状态     |
| `biz_status_transition` | `to_status`      | 目标状态     |
| `biz_status_transition` | `action_code`    | 状态动作编码 |
| `biz_status_log`        | `biz_id`         | 业务 ID      |
| `biz_status_log`        | `from_status`    | 变更前状态   |
| `biz_status_log`        | `to_status`      | 变更后状态   |
| `biz_status_log`        | `operator_id`    | 操作人 ID    |
| `biz_status_log`        | `operate_time`   | 操作时间     |

### 新增状态流转规则

下面的 SQL 用于初始化工单状态流转规则。

```sql
INSERT INTO `biz_status_transition` (
  `id`,
  `biz_type`,
  `from_status`,
  `to_status`,
  `action_code`,
  `action_name`,
  `transition_status`,
  `need_remark`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  500001,
  'work_order',
  0,
  1,
  'submit',
  '提交工单',
  1,
  0,
  100,
  '待提交流转为待处理',
  1,
  1
),
(
  500002,
  'work_order',
  1,
  2,
  'accept',
  '受理工单',
  1,
  0,
  90,
  '待处理流转为处理中',
  1,
  1
),
(
  500003,
  'work_order',
  2,
  3,
  'finish',
  '完成处理',
  1,
  1,
  80,
  '处理中流转为待确认',
  1,
  1
),
(
  500004,
  'work_order',
  3,
  4,
  'confirm',
  '确认完成',
  1,
  0,
  70,
  '待确认流转为已完成',
  1,
  1
),
(
  500005,
  'work_order',
  1,
  5,
  'close',
  '关闭工单',
  1,
  1,
  60,
  '待处理可以关闭',
  1,
  1
);
```

### 新增业务数据

下面的 SQL 用于新增一条待提交工单。

```sql
INSERT INTO `biz_work_order` (
  `id`,
  `work_order_no`,
  `work_order_title`,
  `work_order_type`,
  `current_status`,
  `priority_level`,
  `applicant_id`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  600001,
  'WO_202605110001',
  '系统无法登录',
  2,
  0,
  3,
  10001,
  '用户反馈系统无法登录',
  10001,
  10001
);
```

### 查询当前可执行动作

下面的 SQL 用于根据业务当前状态查询可以执行的状态动作，适合详情页按钮控制。

```sql
SELECT
  t.`action_code`,
  t.`action_name`,
  t.`to_status`,
  t.`need_remark`
FROM `biz_work_order` w
JOIN `biz_status_transition` t
  ON t.`biz_type` = 'work_order'
  AND t.`from_status` = w.`current_status`
  AND t.`transition_status` = 1
  AND t.`is_deleted` = 0
WHERE w.`id` = 600001
  AND w.`is_deleted` = 0
ORDER BY t.`sort_order` DESC;
```

### 校验状态流转是否合法

下面的 SQL 用于校验当前状态下是否允许执行指定动作。业务代码中通常先执行该查询，再执行状态更新。

```sql
SELECT
  t.`id`,
  t.`from_status`,
  t.`to_status`,
  t.`action_code`,
  t.`action_name`,
  t.`need_remark`
FROM `biz_work_order` w
JOIN `biz_status_transition` t
  ON t.`biz_type` = 'work_order'
  AND t.`from_status` = w.`current_status`
  AND t.`action_code` = 'submit'
  AND t.`transition_status` = 1
  AND t.`is_deleted` = 0
WHERE w.`id` = 600001
  AND w.`is_deleted` = 0;
```

### 执行状态流转

下面的 SQL 用于执行工单状态流转。更新时带上原状态条件，可以避免并发场景下重复流转或错乱流转。

```sql
UPDATE `biz_work_order`
SET
  `current_status` = 1,
  `submit_time` = NOW(),
  `updated_by` = 10001
WHERE `id` = 600001
  AND `current_status` = 0
  AND `is_deleted` = 0;
```

### 记录状态变更日志

下面的 SQL 用于记录状态变更日志。状态更新成功后，应同步插入一条状态日志。

```sql
INSERT INTO `biz_status_log` (
  `id`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `from_status`,
  `to_status`,
  `action_code`,
  `action_name`,
  `operator_id`,
  `operator_name`,
  `operate_remark`
) VALUES (
  610001,
  'work_order',
  600001,
  'WO_202605110001',
  0,
  1,
  'submit',
  '提交工单',
  10001,
  '张三',
  '提交工单，等待客服处理'
);
```

### 受理工单

下面的 SQL 用于将待处理工单流转为处理中，并记录处理人和开始处理时间。

```sql
UPDATE `biz_work_order`
SET
  `current_status` = 2,
  `handler_id` = 20001,
  `handle_time` = NOW(),
  `updated_by` = 20001
WHERE `id` = 600001
  AND `current_status` = 1
  AND `is_deleted` = 0;
```

### 完成工单处理

下面的 SQL 用于将处理中工单流转为待确认。

```sql
UPDATE `biz_work_order`
SET
  `current_status` = 3,
  `finish_time` = NOW(),
  `updated_by` = 20001
WHERE `id` = 600001
  AND `current_status` = 2
  AND `handler_id` = 20001
  AND `is_deleted` = 0;
```

### 关闭工单

下面的 SQL 用于关闭工单。关闭类动作通常建议要求填写操作备注。

```sql
UPDATE `biz_work_order`
SET
  `current_status` = 5,
  `close_time` = NOW(),
  `updated_by` = 10001
WHERE `id` = 600001
  AND `current_status` IN (1, 2, 3)
  AND `is_deleted` = 0;
```

### 查询状态变更日志

下面的 SQL 用于查询某个业务对象的完整状态流转历史。

```sql
SELECT
  `id`,
  `from_status`,
  `to_status`,
  `action_code`,
  `action_name`,
  `operator_id`,
  `operator_name`,
  `operate_remark`,
  `operate_time`
FROM `biz_status_log`
WHERE `biz_type` = 'work_order'
  AND `biz_id` = 600001
ORDER BY `operate_time` ASC;
```

### 查询待处理工单

下面的 SQL 用于查询待处理工单列表，适合客服或处理人员工作台。

```sql
SELECT
  `id`,
  `work_order_no`,
  `work_order_title`,
  `work_order_type`,
  `current_status`,
  `priority_level`,
  `applicant_id`,
  `created_at`
FROM `biz_work_order`
WHERE `current_status` = 1
  AND `is_deleted` = 0
ORDER BY
  `priority_level` DESC,
  `created_at` ASC
LIMIT 20 OFFSET 0;
```

### 查询处理人待办工单

下面的 SQL 用于查询指定处理人的处理中工单。

```sql
SELECT
  `id`,
  `work_order_no`,
  `work_order_title`,
  `work_order_type`,
  `current_status`,
  `priority_level`,
  `handle_time`
FROM `biz_work_order`
WHERE `handler_id` = 20001
  AND `current_status` = 2
  AND `is_deleted` = 0
ORDER BY `handle_time` ASC
LIMIT 20 OFFSET 0;
```

### 统计不同状态数量

下面的 SQL 用于统计不同状态下的工单数量，适合工作台统计卡片。

```sql
SELECT
  `current_status`,
  COUNT(*) AS `total_count`
FROM `biz_work_order`
WHERE `is_deleted` = 0
GROUP BY `current_status`;
```

### 查询异常状态日志

下面的 SQL 用于检查状态日志中不符合当前流转规则的历史记录，适合数据治理和问题排查。

```sql
SELECT
  l.`id`,
  l.`biz_type`,
  l.`biz_no`,
  l.`from_status`,
  l.`to_status`,
  l.`action_code`,
  l.`operate_time`
FROM `biz_status_log` l
LEFT JOIN `biz_status_transition` t
  ON l.`biz_type` = t.`biz_type`
  AND l.`from_status` = t.`from_status`
  AND l.`to_status` = t.`to_status`
  AND l.`action_code` = t.`action_code`
  AND t.`is_deleted` = 0
WHERE l.`biz_type` = 'work_order'
  AND t.`id` IS NULL;
```

### 索引设计建议

状态机模型的索引重点是当前状态查询、处理人待办查询、状态流转规则查询和状态日志查询。

| 表                      | 索引                                                         | 适用场景               |
| ----------------------- | ------------------------------------------------------------ | ---------------------- |
| `biz_work_order`        | `PRIMARY KEY (id)`                                           | 工单详情查询           |
| `biz_work_order`        | `uk_work_order_no (work_order_no)`                           | 根据工单编号查询       |
| `biz_work_order`        | `idx_status_deleted (current_status, is_deleted)`            | 按状态查询工单         |
| `biz_work_order`        | `idx_applicant_status_deleted (applicant_id, current_status, is_deleted)` | 查询申请人相关工单     |
| `biz_work_order`        | `idx_handler_status_deleted (handler_id, current_status, is_deleted)` | 查询处理人待办         |
| `biz_status_transition` | `uk_biz_from_action_deleted (biz_type, from_status, action_code, is_deleted)` | 校验动作是否合法       |
| `biz_status_transition` | `idx_biz_from_status (biz_type, from_status, transition_status, is_deleted)` | 查询当前可执行动作     |
| `biz_status_log`        | `idx_biz_id_time (biz_type, biz_id, operate_time)`           | 查询业务状态变更历史   |
| `biz_status_log`        | `idx_operator_time (operator_id, operate_time)`              | 查询操作人状态变更记录 |

### 使用建议

状态机模型的核心是“状态”和“动作”分离。状态表示当前业务所处阶段，动作表示用户或系统触发的流转行为。不要只在业务表中放一个状态字段却不控制流转规则，否则很容易出现非法状态变化。

使用状态机模型时重点注意：

- 业务主表保存当前状态
- 状态流转规则表定义允许的状态变化
- 状态变更日志表记录每次状态变化
- 状态更新时必须带上原状态条件，防止并发错乱
- 状态流转和日志写入应放在同一个事务中
- 终态数据通常不允许继续流转，例如已完成、已关闭
- 页面按钮应根据当前状态和可执行动作动态展示
- 简单启用、停用场景不需要状态机模型
- 复杂审批流程可以在状态机模型基础上继续扩展审批节点、审批人和审批意见表

## 树形层级模型

树形层级模型用于表达具有上下级关系的数据结构，例如分类、菜单、组织架构、区域、部门、评论楼层等。MySQL 8 中常用的树形建模方式是邻接表模型，也就是每条数据保存自己的 `parent_id`，通过父子关系形成树结构。

下面以“商品分类表”为例说明树形层级模型的建表方式和常用 SQL 实践。

### 适用场景

树形层级模型适合业务数据天然存在父子关系、上下级关系或层级归属关系的场景。

常见场景包括：

- 商品分类
- 系统菜单
- 组织架构
- 部门层级
- 行政区域
- 评论回复
- 知识库目录
- 文件夹目录
- 数据权限组织树
- 表单字段分组

### 建表 SQL

下面的 SQL 创建一张商品分类表。该表使用 `parent_id` 保存父级分类 ID，同时冗余 `ancestors` 和 `category_level`，便于查询祖级链路和控制层级。

```sql
CREATE TABLE `biz_category` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级ID，0表示根节点',
  `ancestors` VARCHAR(500) NOT NULL DEFAULT '0' COMMENT '祖级ID路径，例如 0,100001,100002',
  `category_code` VARCHAR(64) NOT NULL COMMENT '分类编码',
  `category_name` VARCHAR(128) NOT NULL COMMENT '分类名称',
  `category_level` INT NOT NULL DEFAULT 1 COMMENT '分类层级，从1开始',
  `category_status` TINYINT NOT NULL DEFAULT 1 COMMENT '分类状态：0禁用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `icon_url` VARCHAR(500) NULL COMMENT '分类图标地址',
  `description` VARCHAR(500) NULL COMMENT '分类描述',
  `is_leaf` TINYINT NOT NULL DEFAULT 1 COMMENT '是否叶子节点：0否，1是',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_parent_status_deleted` (`parent_id`, `category_status`, `is_deleted`),
  KEY `idx_parent_sort` (`parent_id`, `sort_order`),
  KEY `idx_level_status_deleted` (`category_level`, `category_status`, `is_deleted`),
  KEY `idx_status_deleted` (`category_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='业务商品分类表';
```

### 字段设计说明

树形层级模型的核心字段是 `id` 和 `parent_id`。`parent_id` 表示当前节点的父级节点，根节点通常使用 `0`。如果只使用 `parent_id`，查询直接子级很方便，但查询全部子孙节点或祖先节点时需要递归查询。MySQL 8 支持递归 CTE，可以较好地处理这类场景。

| 字段              | 说明                          |
| ----------------- | ----------------------------- |
| `id`              | 当前节点 ID                   |
| `parent_id`       | 父级节点 ID，根节点通常为 `0` |
| `ancestors`       | 祖级路径，便于展示和排查      |
| `category_code`   | 分类编码，业务唯一标识        |
| `category_name`   | 分类名称                      |
| `category_level`  | 分类层级                      |
| `category_status` | 分类状态                      |
| `sort_order`      | 同级节点排序                  |
| `is_leaf`         | 是否叶子节点                  |
| `is_deleted`      | 逻辑删除字段                  |

### 新增根节点

下面的 SQL 用于新增一级分类。一级分类的 `parent_id` 通常为 `0`，`ancestors` 通常为 `0`，`category_level` 为 `1`。

```sql
INSERT INTO `biz_category` (
  `id`,
  `parent_id`,
  `ancestors`,
  `category_code`,
  `category_name`,
  `category_level`,
  `category_status`,
  `sort_order`,
  `is_leaf`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  0,
  '0',
  'CAT_ELECTRONICS',
  '数码电器',
  1,
  1,
  100,
  0,
  1,
  1
);
```

### 新增子节点

下面的 SQL 用于新增二级分类。子节点需要保存父级 ID，同时维护祖级路径和层级。

```sql
INSERT INTO `biz_category` (
  `id`,
  `parent_id`,
  `ancestors`,
  `category_code`,
  `category_name`,
  `category_level`,
  `category_status`,
  `sort_order`,
  `is_leaf`,
  `created_by`,
  `updated_by`
) VALUES (
  100002,
  100001,
  '0,100001',
  'CAT_PHONE',
  '手机通讯',
  2,
  1,
  90,
  1,
  1,
  1
);
```

### 更新父节点叶子状态

下面的 SQL 用于新增子节点后，将父节点更新为非叶子节点。

```sql
UPDATE `biz_category`
SET
  `is_leaf` = 0,
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 查询根节点列表

下面的 SQL 用于查询所有一级分类，适合分类首页、后台树根节点加载等场景。

```sql
SELECT
  `id`,
  `parent_id`,
  `category_code`,
  `category_name`,
  `category_level`,
  `category_status`,
  `sort_order`,
  `is_leaf`
FROM `biz_category`
WHERE `parent_id` = 0
  AND `category_status` = 1
  AND `is_deleted` = 0
ORDER BY `sort_order` DESC, `id` ASC;
```

### 查询直接子节点

下面的 SQL 用于查询某个节点下的直接子分类。

```sql
SELECT
  `id`,
  `parent_id`,
  `category_code`,
  `category_name`,
  `category_level`,
  `category_status`,
  `sort_order`,
  `is_leaf`
FROM `biz_category`
WHERE `parent_id` = 100001
  AND `category_status` = 1
  AND `is_deleted` = 0
ORDER BY `sort_order` DESC, `id` ASC;
```

### 递归查询子孙节点

下面的 SQL 使用 MySQL 8 递归 CTE 查询某个分类下的全部子孙节点。

```sql
WITH RECURSIVE category_tree AS (
  SELECT
    `id`,
    `parent_id`,
    `category_code`,
    `category_name`,
    `category_level`,
    `sort_order`,
    `is_leaf`
  FROM `biz_category`
  WHERE `id` = 100001
    AND `is_deleted` = 0

  UNION ALL

  SELECT
    c.`id`,
    c.`parent_id`,
    c.`category_code`,
    c.`category_name`,
    c.`category_level`,
    c.`sort_order`,
    c.`is_leaf`
  FROM `biz_category` c
  JOIN category_tree t
    ON c.`parent_id` = t.`id`
  WHERE c.`is_deleted` = 0
)
SELECT
  `id`,
  `parent_id`,
  `category_code`,
  `category_name`,
  `category_level`,
  `sort_order`,
  `is_leaf`
FROM category_tree
ORDER BY `category_level` ASC, `sort_order` DESC, `id` ASC;
```

### 递归查询祖先节点

下面的 SQL 用于查询某个分类的所有祖先节点，适合面包屑导航和详情页路径展示。

```sql
WITH RECURSIVE parent_tree AS (
  SELECT
    `id`,
    `parent_id`,
    `category_code`,
    `category_name`,
    `category_level`,
    `sort_order`
  FROM `biz_category`
  WHERE `id` = 100002
    AND `is_deleted` = 0

  UNION ALL

  SELECT
    p.`id`,
    p.`parent_id`,
    p.`category_code`,
    p.`category_name`,
    p.`category_level`,
    p.`sort_order`
  FROM `biz_category` p
  JOIN parent_tree t
    ON p.`id` = t.`parent_id`
  WHERE p.`is_deleted` = 0
)
SELECT
  `id`,
  `parent_id`,
  `category_code`,
  `category_name`,
  `category_level`,
  `sort_order`
FROM parent_tree
ORDER BY `category_level` ASC;
```

### 查询完整分类树

下面的 SQL 用于查询完整分类树的平铺结果。应用层可以根据 `parent_id` 组装成树形结构。

```sql
SELECT
  `id`,
  `parent_id`,
  `ancestors`,
  `category_code`,
  `category_name`,
  `category_level`,
  `category_status`,
  `sort_order`,
  `is_leaf`
FROM `biz_category`
WHERE `category_status` = 1
  AND `is_deleted` = 0
ORDER BY
  `category_level` ASC,
  `parent_id` ASC,
  `sort_order` DESC,
  `id` ASC;
```

### 查询指定层级分类

下面的 SQL 用于查询指定层级的分类，例如只查询二级分类。

```sql
SELECT
  `id`,
  `parent_id`,
  `category_code`,
  `category_name`,
  `category_level`,
  `sort_order`
FROM `biz_category`
WHERE `category_level` = 2
  AND `category_status` = 1
  AND `is_deleted` = 0
ORDER BY `sort_order` DESC, `id` ASC;
```

### 查询叶子节点

下面的 SQL 用于查询所有叶子分类，适合商品绑定分类时只允许选择末级分类的场景。

```sql
SELECT
  `id`,
  `parent_id`,
  `category_code`,
  `category_name`,
  `category_level`
FROM `biz_category`
WHERE `is_leaf` = 1
  AND `category_status` = 1
  AND `is_deleted` = 0
ORDER BY `category_level` ASC, `sort_order` DESC;
```

### 修改分类基础信息

下面的 SQL 用于修改分类名称、排序、状态等基础字段。

```sql
UPDATE `biz_category`
SET
  `category_name` = '手机数码',
  `category_status` = 1,
  `sort_order` = 95,
  `description` = '手机及相关数码产品分类',
  `updated_by` = 1
WHERE `id` = 100002
  AND `is_deleted` = 0;
```

### 移动分类节点

下面的 SQL 用于将分类移动到新的父节点下。实际业务中，移动节点后还需要同步更新该节点及其所有子孙节点的 `ancestors` 和 `category_level`。

```sql
UPDATE `biz_category`
SET
  `parent_id` = 100003,
  `ancestors` = '0,100003',
  `category_level` = 2,
  `updated_by` = 1
WHERE `id` = 100002
  AND `is_deleted` = 0;
```

### 禁用分类及子分类

下面的 SQL 使用递归 CTE 查询某个分类及其全部子孙节点，并批量禁用这些分类。

```sql
WITH RECURSIVE category_tree AS (
  SELECT
    `id`
  FROM `biz_category`
  WHERE `id` = 100001
    AND `is_deleted` = 0

  UNION ALL

  SELECT
    c.`id`
  FROM `biz_category` c
  JOIN category_tree t
    ON c.`parent_id` = t.`id`
  WHERE c.`is_deleted` = 0
)
UPDATE `biz_category`
SET
  `category_status` = 0,
  `updated_by` = 1
WHERE `id` IN (
  SELECT `id`
  FROM category_tree
);
```

### 逻辑删除分类

下面的 SQL 用于删除叶子分类。一般不建议直接删除存在子节点的分类。

```sql
UPDATE `biz_category`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 100002
  AND `is_leaf` = 1
  AND `is_deleted` = 0;
```

### 查询存在子节点的分类

下面的 SQL 用于判断某个分类是否存在有效子分类，适合删除前校验。

```sql
SELECT
  COUNT(*) AS `child_count`
FROM `biz_category`
WHERE `parent_id` = 100001
  AND `is_deleted` = 0;
```

### 统计每个父级下的子节点数量

下面的 SQL 用于统计每个分类下的直接子分类数量。

```sql
SELECT
  p.`id`,
  p.`category_name`,
  COUNT(c.`id`) AS `child_count`
FROM `biz_category` p
LEFT JOIN `biz_category` c
  ON p.`id` = c.`parent_id`
  AND c.`is_deleted` = 0
WHERE p.`is_deleted` = 0
GROUP BY
  p.`id`,
  p.`category_name`
ORDER BY p.`sort_order` DESC;
```

### 索引设计建议

树形层级模型的索引重点是 `parent_id`，因为查询直接子节点、组装树结构、递归查询都会频繁使用该字段。

| 索引                                                         | 适用场景                     |
| ------------------------------------------------------------ | ---------------------------- |
| `PRIMARY KEY (id)`                                           | 根据节点 ID 查询             |
| `uk_category_code (category_code)`                           | 根据分类编码查询             |
| `idx_parent_status_deleted (parent_id, category_status, is_deleted)` | 查询某个父节点下的有效子节点 |
| `idx_parent_sort (parent_id, sort_order)`                    | 查询子节点并排序             |
| `idx_level_status_deleted (category_level, category_status, is_deleted)` | 查询指定层级分类             |
| `idx_status_deleted (category_status, is_deleted)`           | 查询启用分类                 |

### 使用建议

树形层级模型优先使用 `parent_id` 表达父子关系。对于中小规模树结构，MySQL 8 的递归 CTE 已经可以满足大多数查询需求。对于超大规模树或频繁查询全量子孙节点的场景，可以额外维护路径字段、闭包表或搜索引擎索引。

使用树形层级模型时重点注意：

- 根节点的 `parent_id` 建议统一使用 `0`
- 查询直接子节点必须给 `parent_id` 建索引
- `ancestors` 可以冗余保存祖级路径，但不要完全依赖它做复杂查询
- 移动节点时要同步处理子孙节点层级和路径
- 删除节点前要检查是否存在有效子节点
- 分类、菜单、部门这类数据建议限制最大层级
- 前端树结构通常由后端查询平铺数据后组装
- 数据量较大时，不建议每次都递归查询整棵树

## 用户-角色-权限模型

用户-角色-权限模型是后台管理系统中最常用的权限建模方式，通常也称为 RBAC 模型。它通过用户、角色、权限三类核心对象表达授权关系：用户绑定角色，角色绑定权限，最终用户通过角色获得权限。

下面以“用户表”“角色表”“权限表”“用户角色关系表”和“角色权限关系表”为例说明用户-角色-权限模型的建表方式和常用 SQL 实践。

### 适用场景

用户-角色-权限模型适合需要统一管理菜单权限、按钮权限、接口权限、数据权限的系统。

常见场景包括：

- 后台管理系统
- SaaS 管理平台
- 企业内部系统
- 运维平台
- 低代码平台
- 数据中台
- CRM 系统
- ERP 系统
- OA 系统
- 权限中心

### 建表 SQL

下面的 SQL 创建用户、角色、权限以及两张关系表。用户和角色是多对多关系，角色和权限也是多对多关系。

```sql
CREATE TABLE `sys_rbac_user` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `user_no` VARCHAR(64) NOT NULL COMMENT '用户编号',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `nickname` VARCHAR(64) NULL COMMENT '昵称',
  `mobile` VARCHAR(32) NULL COMMENT '手机号',
  `email` VARCHAR(128) NULL COMMENT '邮箱',
  `user_status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用',
  `last_login_time` DATETIME NULL COMMENT '最后登录时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`user_no`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_status_deleted` (`user_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='RBAC用户表';

CREATE TABLE `sys_rbac_role` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(128) NOT NULL COMMENT '角色名称',
  `role_status` TINYINT NOT NULL DEFAULT 1 COMMENT '角色状态：0禁用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_status_deleted` (`role_status`, `is_deleted`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='RBAC角色表';

CREATE TABLE `sys_rbac_permission` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级权限ID，0表示根节点',
  `permission_code` VARCHAR(128) NOT NULL COMMENT '权限编码',
  `permission_name` VARCHAR(128) NOT NULL COMMENT '权限名称',
  `permission_type` TINYINT NOT NULL COMMENT '权限类型：1目录，2菜单，3按钮，4接口',
  `permission_value` VARCHAR(255) NULL COMMENT '权限标识，例如 system:user:add',
  `route_path` VARCHAR(255) NULL COMMENT '前端路由地址',
  `component_path` VARCHAR(255) NULL COMMENT '前端组件路径',
  `api_method` VARCHAR(16) NULL COMMENT '接口请求方式，例如 GET、POST、PUT、DELETE',
  `api_path` VARCHAR(255) NULL COMMENT '接口路径',
  `icon` VARCHAR(128) NULL COMMENT '菜单图标',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示：0隐藏，1显示',
  `permission_status` TINYINT NOT NULL DEFAULT 1 COMMENT '权限状态：0禁用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_type_status` (`parent_id`, `permission_type`, `permission_status`, `is_deleted`),
  KEY `idx_permission_value` (`permission_value`),
  KEY `idx_api_method_path` (`api_method`, `api_path`),
  KEY `idx_status_deleted` (`permission_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='RBAC权限表';

CREATE TABLE `sys_rbac_user_role` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `relation_status` TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态：0禁用，1启用',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role_deleted` (`user_id`, `role_id`, `is_deleted`),
  KEY `idx_user_status_deleted` (`user_id`, `relation_status`, `is_deleted`),
  KEY `idx_role_status_deleted` (`role_id`, `relation_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='RBAC用户角色关系表';

CREATE TABLE `sys_rbac_role_permission` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  `relation_status` TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态：0禁用，1启用',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission_deleted` (`role_id`, `permission_id`, `is_deleted`),
  KEY `idx_role_status_deleted` (`role_id`, `relation_status`, `is_deleted`),
  KEY `idx_permission_status_deleted` (`permission_id`, `relation_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='RBAC角色权限关系表';
```

### 字段设计说明

用户-角色-权限模型的核心是两类授权关系。用户不直接绑定权限，而是通过角色间接获得权限。这样可以降低授权复杂度，方便统一维护权限。

| 表                         | 字段               | 说明                                 |
| -------------------------- | ------------------ | ------------------------------------ |
| `sys_rbac_user`            | `id`               | 用户 ID                              |
| `sys_rbac_role`            | `id`               | 角色 ID                              |
| `sys_rbac_permission`      | `id`               | 权限 ID                              |
| `sys_rbac_permission`      | `permission_type`  | 权限类型，区分目录、菜单、按钮、接口 |
| `sys_rbac_permission`      | `permission_value` | 权限标识                             |
| `sys_rbac_user_role`       | `user_id`          | 用户 ID                              |
| `sys_rbac_user_role`       | `role_id`          | 角色 ID                              |
| `sys_rbac_role_permission` | `role_id`          | 角色 ID                              |
| `sys_rbac_role_permission` | `permission_id`    | 权限 ID                              |

### 新增用户

下面的 SQL 用于新增系统用户。

```sql
INSERT INTO `sys_rbac_user` (
  `id`,
  `user_no`,
  `username`,
  `nickname`,
  `mobile`,
  `email`,
  `user_status`,
  `created_by`,
  `updated_by`
) VALUES (
  200001,
  'USER_202605110003',
  'admin',
  '系统管理员',
  '13800000000',
  'admin@example.com',
  1,
  1,
  1
);
```

### 新增角色

下面的 SQL 用于新增角色数据。

```sql
INSERT INTO `sys_rbac_role` (
  `id`,
  `role_code`,
  `role_name`,
  `role_status`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  210001,
  'SUPER_ADMIN',
  '超级管理员',
  1,
  100,
  '拥有系统全部权限',
  1,
  1
),
(
  210002,
  'SYSTEM_USER',
  '普通用户',
  1,
  90,
  '拥有基础系统权限',
  1,
  1
);
```

### 新增权限

下面的 SQL 用于新增目录、菜单、按钮和接口权限。

```sql
INSERT INTO `sys_rbac_permission` (
  `id`,
  `parent_id`,
  `permission_code`,
  `permission_name`,
  `permission_type`,
  `permission_value`,
  `route_path`,
  `component_path`,
  `api_method`,
  `api_path`,
  `icon`,
  `visible`,
  `permission_status`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  220001,
  0,
  'SYSTEM_MANAGE',
  '系统管理',
  1,
  'system',
  '/system',
  NULL,
  NULL,
  NULL,
  'setting',
  1,
  1,
  100,
  '系统管理目录',
  1,
  1
),
(
  220002,
  220001,
  'SYSTEM_USER_LIST',
  '用户管理',
  2,
  'system:user:list',
  '/system/user',
  'system/user/index',
  NULL,
  NULL,
  'user',
  1,
  1,
  90,
  '用户管理菜单',
  1,
  1
),
(
  220003,
  220002,
  'SYSTEM_USER_ADD',
  '用户新增',
  3,
  'system:user:add',
  NULL,
  NULL,
  NULL,
  NULL,
  NULL,
  1,
  1,
  80,
  '用户新增按钮',
  1,
  1
),
(
  220004,
  220002,
  'SYSTEM_USER_ADD_API',
  '用户新增接口',
  4,
  'system:user:add',
  NULL,
  NULL,
  'POST',
  '/system/user',
  NULL,
  0,
  1,
  70,
  '用户新增接口权限',
  1,
  1
);
```

### 给用户分配角色

下面的 SQL 用于给用户分配角色。

```sql
INSERT INTO `sys_rbac_user_role` (
  `id`,
  `user_id`,
  `role_id`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  230001,
  200001,
  210001,
  1,
  '分配超级管理员角色',
  1,
  1
);
```

### 给角色分配权限

下面的 SQL 用于给角色分配权限。

```sql
INSERT INTO `sys_rbac_role_permission` (
  `id`,
  `role_id`,
  `permission_id`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  240001,
  210001,
  220001,
  1,
  '分配系统管理目录',
  1,
  1
),
(
  240002,
  210001,
  220002,
  1,
  '分配用户管理菜单',
  1,
  1
),
(
  240003,
  210001,
  220003,
  1,
  '分配用户新增按钮',
  1,
  1
),
(
  240004,
  210001,
  220004,
  1,
  '分配用户新增接口',
  1,
  1
);
```

### 查询用户角色

下面的 SQL 用于查询某个用户拥有的角色，适合用户详情页和登录后加载角色信息。

```sql
SELECT
  r.`id`,
  r.`role_code`,
  r.`role_name`,
  r.`role_status`,
  r.`sort_order`
FROM `sys_rbac_user_role` ur
JOIN `sys_rbac_role` r
  ON ur.`role_id` = r.`id`
  AND r.`is_deleted` = 0
WHERE ur.`user_id` = 200001
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
  AND r.`role_status` = 1
ORDER BY r.`sort_order` DESC;
```

### 查询角色权限

下面的 SQL 用于查询某个角色拥有的权限，适合角色详情页和角色授权页面。

```sql
SELECT
  p.`id`,
  p.`parent_id`,
  p.`permission_code`,
  p.`permission_name`,
  p.`permission_type`,
  p.`permission_value`,
  p.`route_path`,
  p.`component_path`,
  p.`api_method`,
  p.`api_path`,
  p.`sort_order`
FROM `sys_rbac_role_permission` rp
JOIN `sys_rbac_permission` p
  ON rp.`permission_id` = p.`id`
  AND p.`is_deleted` = 0
WHERE rp.`role_id` = 210001
  AND rp.`relation_status` = 1
  AND rp.`is_deleted` = 0
  AND p.`permission_status` = 1
ORDER BY p.`parent_id` ASC, p.`sort_order` DESC;
```

### 查询用户全部权限

下面的 SQL 用于查询用户通过角色获得的全部权限。使用 `DISTINCT` 可以避免多个角色绑定同一权限时返回重复数据。

```sql
SELECT DISTINCT
  p.`id`,
  p.`parent_id`,
  p.`permission_code`,
  p.`permission_name`,
  p.`permission_type`,
  p.`permission_value`,
  p.`route_path`,
  p.`component_path`,
  p.`api_method`,
  p.`api_path`,
  p.`sort_order`
FROM `sys_rbac_user_role` ur
JOIN `sys_rbac_role` r
  ON ur.`role_id` = r.`id`
  AND r.`role_status` = 1
  AND r.`is_deleted` = 0
JOIN `sys_rbac_role_permission` rp
  ON r.`id` = rp.`role_id`
  AND rp.`relation_status` = 1
  AND rp.`is_deleted` = 0
JOIN `sys_rbac_permission` p
  ON rp.`permission_id` = p.`id`
  AND p.`permission_status` = 1
  AND p.`is_deleted` = 0
WHERE ur.`user_id` = 200001
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
ORDER BY p.`parent_id` ASC, p.`sort_order` DESC;
```

### 查询用户菜单权限

下面的 SQL 用于查询用户可访问的目录和菜单，适合登录后生成前端路由菜单。

```sql
SELECT DISTINCT
  p.`id`,
  p.`parent_id`,
  p.`permission_name`,
  p.`permission_type`,
  p.`permission_value`,
  p.`route_path`,
  p.`component_path`,
  p.`icon`,
  p.`visible`,
  p.`sort_order`
FROM `sys_rbac_user_role` ur
JOIN `sys_rbac_role` r
  ON ur.`role_id` = r.`id`
  AND r.`role_status` = 1
  AND r.`is_deleted` = 0
JOIN `sys_rbac_role_permission` rp
  ON r.`id` = rp.`role_id`
  AND rp.`relation_status` = 1
  AND rp.`is_deleted` = 0
JOIN `sys_rbac_permission` p
  ON rp.`permission_id` = p.`id`
  AND p.`permission_status` = 1
  AND p.`is_deleted` = 0
WHERE ur.`user_id` = 200001
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
  AND p.`permission_type` IN (1, 2)
  AND p.`visible` = 1
ORDER BY p.`parent_id` ASC, p.`sort_order` DESC;
```

### 查询用户按钮权限

下面的 SQL 用于查询用户拥有的按钮权限，适合前端控制新增、修改、删除、导出等按钮显示。

```sql
SELECT DISTINCT
  p.`permission_value`
FROM `sys_rbac_user_role` ur
JOIN `sys_rbac_role` r
  ON ur.`role_id` = r.`id`
  AND r.`role_status` = 1
  AND r.`is_deleted` = 0
JOIN `sys_rbac_role_permission` rp
  ON r.`id` = rp.`role_id`
  AND rp.`relation_status` = 1
  AND rp.`is_deleted` = 0
JOIN `sys_rbac_permission` p
  ON rp.`permission_id` = p.`id`
  AND p.`permission_status` = 1
  AND p.`is_deleted` = 0
WHERE ur.`user_id` = 200001
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
  AND p.`permission_type` = 3;
```

### 校验用户接口权限

下面的 SQL 用于校验用户是否拥有指定接口权限，适合网关、拦截器或权限校验逻辑。

```sql
SELECT
  COUNT(DISTINCT p.`id`) AS `permission_count`
FROM `sys_rbac_user_role` ur
JOIN `sys_rbac_role` r
  ON ur.`role_id` = r.`id`
  AND r.`role_status` = 1
  AND r.`is_deleted` = 0
JOIN `sys_rbac_role_permission` rp
  ON r.`id` = rp.`role_id`
  AND rp.`relation_status` = 1
  AND rp.`is_deleted` = 0
JOIN `sys_rbac_permission` p
  ON rp.`permission_id` = p.`id`
  AND p.`permission_status` = 1
  AND p.`is_deleted` = 0
WHERE ur.`user_id` = 200001
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
  AND p.`permission_type` = 4
  AND p.`api_method` = 'POST'
  AND p.`api_path` = '/system/user';
```

### 查询拥有指定权限的用户

下面的 SQL 用于查询拥有某个权限标识的用户，适合权限排查和审计。

```sql
SELECT DISTINCT
  u.`id`,
  u.`user_no`,
  u.`username`,
  u.`nickname`,
  u.`user_status`
FROM `sys_rbac_user` u
JOIN `sys_rbac_user_role` ur
  ON u.`id` = ur.`user_id`
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
JOIN `sys_rbac_role` r
  ON ur.`role_id` = r.`id`
  AND r.`role_status` = 1
  AND r.`is_deleted` = 0
JOIN `sys_rbac_role_permission` rp
  ON r.`id` = rp.`role_id`
  AND rp.`relation_status` = 1
  AND rp.`is_deleted` = 0
JOIN `sys_rbac_permission` p
  ON rp.`permission_id` = p.`id`
  AND p.`permission_status` = 1
  AND p.`is_deleted` = 0
WHERE u.`user_status` = 1
  AND u.`is_deleted` = 0
  AND p.`permission_value` = 'system:user:add';
```

### 重新分配用户角色

下面的 SQL 用于重新分配用户角色。常见做法是先逻辑删除原有关系，再插入新的关系数据，两个步骤应放在同一个事务中执行。

```sql
UPDATE `sys_rbac_user_role`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `user_id` = 200001
  AND `is_deleted` = 0;

INSERT INTO `sys_rbac_user_role` (
  `id`,
  `user_id`,
  `role_id`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  230002,
  200001,
  210002,
  1,
  '重新分配普通用户角色',
  1,
  1
);
```

### 重新分配角色权限

下面的 SQL 用于重新分配角色权限。常见做法是先逻辑删除角色原有权限，再插入新的权限关系。

```sql
UPDATE `sys_rbac_role_permission`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `role_id` = 210002
  AND `is_deleted` = 0;

INSERT INTO `sys_rbac_role_permission` (
  `id`,
  `role_id`,
  `permission_id`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  240005,
  210002,
  220001,
  1,
  '重新分配权限',
  1,
  1
),
(
  240006,
  210002,
  220002,
  1,
  '重新分配权限',
  1,
  1
);
```

### 禁用角色

下面的 SQL 用于禁用角色。角色禁用后，用户通过该角色获得的权限应同时失效。

```sql
UPDATE `sys_rbac_role`
SET
  `role_status` = 0,
  `updated_by` = 1
WHERE `id` = 210002
  AND `role_status` = 1
  AND `is_deleted` = 0;
```

### 禁用权限

下面的 SQL 用于禁用权限。权限禁用后，即使角色仍然绑定该权限，也不应继续生效。

```sql
UPDATE `sys_rbac_permission`
SET
  `permission_status` = 0,
  `updated_by` = 1
WHERE `id` = 220004
  AND `permission_status` = 1
  AND `is_deleted` = 0;
```

### 查询没有角色的用户

下面的 SQL 用于查询没有分配任何有效角色的用户。

```sql
SELECT
  u.`id`,
  u.`user_no`,
  u.`username`,
  u.`nickname`
FROM `sys_rbac_user` u
LEFT JOIN `sys_rbac_user_role` ur
  ON u.`id` = ur.`user_id`
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
WHERE u.`user_status` = 1
  AND u.`is_deleted` = 0
  AND ur.`id` IS NULL;
```

### 查询没有权限的角色

下面的 SQL 用于查询没有分配任何有效权限的角色。

```sql
SELECT
  r.`id`,
  r.`role_code`,
  r.`role_name`
FROM `sys_rbac_role` r
LEFT JOIN `sys_rbac_role_permission` rp
  ON r.`id` = rp.`role_id`
  AND rp.`relation_status` = 1
  AND rp.`is_deleted` = 0
WHERE r.`role_status` = 1
  AND r.`is_deleted` = 0
  AND rp.`id` IS NULL;
```

### 统计角色用户数量

下面的 SQL 用于统计每个角色下的有效用户数量。

```sql
SELECT
  r.`id`,
  r.`role_code`,
  r.`role_name`,
  COUNT(DISTINCT ur.`user_id`) AS `user_count`
FROM `sys_rbac_role` r
LEFT JOIN `sys_rbac_user_role` ur
  ON r.`id` = ur.`role_id`
  AND ur.`relation_status` = 1
  AND ur.`is_deleted` = 0
LEFT JOIN `sys_rbac_user` u
  ON ur.`user_id` = u.`id`
  AND u.`user_status` = 1
  AND u.`is_deleted` = 0
WHERE r.`is_deleted` = 0
GROUP BY
  r.`id`,
  r.`role_code`,
  r.`role_name`
ORDER BY r.`sort_order` DESC;
```

### 统计角色权限数量

下面的 SQL 用于统计每个角色下的有效权限数量。

```sql
SELECT
  r.`id`,
  r.`role_code`,
  r.`role_name`,
  COUNT(DISTINCT rp.`permission_id`) AS `permission_count`
FROM `sys_rbac_role` r
LEFT JOIN `sys_rbac_role_permission` rp
  ON r.`id` = rp.`role_id`
  AND rp.`relation_status` = 1
  AND rp.`is_deleted` = 0
LEFT JOIN `sys_rbac_permission` p
  ON rp.`permission_id` = p.`id`
  AND p.`permission_status` = 1
  AND p.`is_deleted` = 0
WHERE r.`is_deleted` = 0
GROUP BY
  r.`id`,
  r.`role_code`,
  r.`role_name`
ORDER BY r.`sort_order` DESC;
```

### 索引设计建议

用户-角色-权限模型的索引重点在两张关系表。系统通常需要频繁根据用户查询角色、根据角色查询权限、根据权限反查角色和用户。

| 表                         | 索引                                                         | 适用场景                 |
| -------------------------- | ------------------------------------------------------------ | ------------------------ |
| `sys_rbac_user`            | `uk_username (username)`                                     | 登录查询                 |
| `sys_rbac_user`            | `idx_status_deleted (user_status, is_deleted)`               | 查询启用用户             |
| `sys_rbac_role`            | `uk_role_code (role_code)`                                   | 根据角色编码查询         |
| `sys_rbac_permission`      | `uk_permission_code (permission_code)`                       | 根据权限编码查询         |
| `sys_rbac_permission`      | `idx_parent_type_status (parent_id, permission_type, permission_status, is_deleted)` | 查询菜单树、权限树       |
| `sys_rbac_permission`      | `idx_permission_value (permission_value)`                    | 根据权限标识校验         |
| `sys_rbac_permission`      | `idx_api_method_path (api_method, api_path)`                 | 根据接口路径校验         |
| `sys_rbac_user_role`       | `uk_user_role_deleted (user_id, role_id, is_deleted)`        | 防止用户重复分配同一角色 |
| `sys_rbac_user_role`       | `idx_user_status_deleted (user_id, relation_status, is_deleted)` | 查询用户角色             |
| `sys_rbac_user_role`       | `idx_role_status_deleted (role_id, relation_status, is_deleted)` | 查询角色下用户           |
| `sys_rbac_role_permission` | `uk_role_permission_deleted (role_id, permission_id, is_deleted)` | 防止角色重复分配同一权限 |
| `sys_rbac_role_permission` | `idx_role_status_deleted (role_id, relation_status, is_deleted)` | 查询角色权限             |
| `sys_rbac_role_permission` | `idx_permission_status_deleted (permission_id, relation_status, is_deleted)` | 查询权限被哪些角色使用   |

### 使用建议

用户-角色-权限模型的核心原则是不要让用户直接绑定大量权限，而是通过角色进行授权。这样可以降低权限维护成本，也便于审计和批量调整。

使用用户-角色-权限模型时重点注意：

- 用户和角色是多对多关系，需要用户角色关系表
- 角色和权限是多对多关系，需要角色权限关系表
- 权限表可以同时承载目录、菜单、按钮、接口权限
- 权限编码和权限标识要保持稳定
- 登录后可以缓存用户角色和权限，提高接口鉴权性能
- 修改角色权限后需要刷新相关用户的权限缓存
- 禁用角色后，该角色下的权限应立即失效
- 禁用权限后，即使角色已绑定该权限，也不应继续生效
- 后台菜单树通常根据用户拥有的目录和菜单权限生成
- 按钮权限通常由前端根据权限标识控制显示
- 接口权限应由后端或网关进行强制校验

## 组织架构模型

组织架构模型用于描述企业、部门、岗位、员工之间的层级和归属关系。它通常包含组织节点、岗位信息和员工信息，组织节点本身一般是树形结构，员工挂载到具体组织节点或岗位上。

下面以“组织表”“岗位表”和“员工表”为例说明组织架构模型的建表方式和常用 SQL 实践。

### 适用场景

组织架构模型适合需要表达企业内部层级、人员归属、岗位职责和组织权限范围的业务系统。

常见场景包括：

- 企业组织架构
- 部门层级管理
- 岗位管理
- 员工档案
- 数据权限范围
- 审批人查找
- 部门负责人管理
- 项目成员归属
- 多组织业务隔离
- 企业通讯录

### 建表 SQL

下面的 SQL 创建组织表、岗位表和员工表。组织表使用 `parent_id` 表达上下级关系，岗位表归属于组织，员工表归属于组织和岗位。

```sql
CREATE TABLE `sys_org` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级组织ID，0表示根节点',
  `ancestors` VARCHAR(500) NOT NULL DEFAULT '0' COMMENT '祖级ID路径，例如 0,100001,100002',
  `org_code` VARCHAR(64) NOT NULL COMMENT '组织编码',
  `org_name` VARCHAR(128) NOT NULL COMMENT '组织名称',
  `org_type` TINYINT NOT NULL DEFAULT 2 COMMENT '组织类型：1公司，2部门，3小组',
  `org_level` INT NOT NULL DEFAULT 1 COMMENT '组织层级，从1开始',
  `leader_employee_id` BIGINT NULL COMMENT '负责人员工ID',
  `org_status` TINYINT NOT NULL DEFAULT 1 COMMENT '组织状态：0停用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_code` (`org_code`),
  KEY `idx_parent_status_deleted` (`parent_id`, `org_status`, `is_deleted`),
  KEY `idx_level_status_deleted` (`org_level`, `org_status`, `is_deleted`),
  KEY `idx_leader_employee_id` (`leader_employee_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统组织表';

CREATE TABLE `sys_position` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `org_id` BIGINT NOT NULL COMMENT '所属组织ID',
  `position_code` VARCHAR(64) NOT NULL COMMENT '岗位编码',
  `position_name` VARCHAR(128) NOT NULL COMMENT '岗位名称',
  `position_status` TINYINT NOT NULL DEFAULT 1 COMMENT '岗位状态：0停用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_position_code` (`position_code`),
  KEY `idx_org_status_deleted` (`org_id`, `position_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统岗位表';

CREATE TABLE `sys_org_employee` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `employee_no` VARCHAR(64) NOT NULL COMMENT '员工编号',
  `org_id` BIGINT NOT NULL COMMENT '所属组织ID',
  `position_id` BIGINT NULL COMMENT '岗位ID',
  `employee_name` VARCHAR(64) NOT NULL COMMENT '员工姓名',
  `mobile` VARCHAR(32) NULL COMMENT '手机号',
  `email` VARCHAR(128) NULL COMMENT '邮箱',
  `employee_status` TINYINT NOT NULL DEFAULT 1 COMMENT '员工状态：0离职，1在职，2冻结',
  `entry_date` DATE NULL COMMENT '入职日期',
  `leave_date` DATE NULL COMMENT '离职日期',
  `is_leader` TINYINT NOT NULL DEFAULT 0 COMMENT '是否负责人：0否，1是',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_no` (`employee_no`),
  KEY `idx_org_status_deleted` (`org_id`, `employee_status`, `is_deleted`),
  KEY `idx_position_status_deleted` (`position_id`, `employee_status`, `is_deleted`),
  KEY `idx_mobile` (`mobile`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统组织员工表';
```

### 字段设计说明

组织架构模型的核心是组织层级和员工归属。组织表负责表达上下级结构，岗位表负责表达组织内的岗位定义，员工表负责表达人员实际归属。

| 表                 | 字段                 | 说明                           |
| ------------------ | -------------------- | ------------------------------ |
| `sys_org`          | `parent_id`          | 父级组织 ID                    |
| `sys_org`          | `ancestors`          | 祖级路径                       |
| `sys_org`          | `org_type`           | 组织类型，例如公司、部门、小组 |
| `sys_org`          | `leader_employee_id` | 组织负责人                     |
| `sys_position`     | `org_id`             | 岗位所属组织                   |
| `sys_position`     | `position_code`      | 岗位编码                       |
| `sys_org_employee` | `org_id`             | 员工所属组织                   |
| `sys_org_employee` | `position_id`        | 员工岗位                       |
| `sys_org_employee` | `is_leader`          | 是否负责人                     |

### 新增组织

下面的 SQL 用于新增组织节点。一级组织的 `parent_id` 通常为 `0`。

```sql
INSERT INTO `sys_org` (
  `id`,
  `parent_id`,
  `ancestors`,
  `org_code`,
  `org_name`,
  `org_type`,
  `org_level`,
  `org_status`,
  `sort_order`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  0,
  '0',
  'ORG_HEAD',
  '总部',
  1,
  1,
  1,
  100,
  1,
  1
);
```

### 新增下级组织

下面的 SQL 用于新增下级部门，并维护父级 ID、祖级路径和组织层级。

```sql
INSERT INTO `sys_org` (
  `id`,
  `parent_id`,
  `ancestors`,
  `org_code`,
  `org_name`,
  `org_type`,
  `org_level`,
  `org_status`,
  `sort_order`,
  `created_by`,
  `updated_by`
) VALUES (
  100002,
  100001,
  '0,100001',
  'ORG_RD',
  '研发部',
  2,
  2,
  1,
  90,
  1,
  1
);
```

### 新增岗位

下面的 SQL 用于给组织新增岗位。

```sql
INSERT INTO `sys_position` (
  `id`,
  `org_id`,
  `position_code`,
  `position_name`,
  `position_status`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  110001,
  100002,
  'POS_JAVA_DEV',
  'Java开发工程师',
  1,
  100,
  '负责后端服务开发',
  1,
  1
);
```

### 新增员工

下面的 SQL 用于新增员工，并绑定所属组织和岗位。

```sql
INSERT INTO `sys_org_employee` (
  `id`,
  `employee_no`,
  `org_id`,
  `position_id`,
  `employee_name`,
  `mobile`,
  `email`,
  `employee_status`,
  `entry_date`,
  `is_leader`,
  `created_by`,
  `updated_by`
) VALUES (
  120001,
  'EMP_202605110001',
  100002,
  110001,
  '张三',
  '13800000000',
  'zhangsan@example.com',
  1,
  '2024-03-01',
  0,
  1,
  1
);
```

### 设置组织负责人

下面的 SQL 用于设置组织负责人。负责人通常需要是该组织或下级组织中的有效员工，具体校验可以由业务代码控制。

```sql
UPDATE `sys_org`
SET
  `leader_employee_id` = 120001,
  `updated_by` = 1
WHERE `id` = 100002
  AND `org_status` = 1
  AND `is_deleted` = 0;
```

### 查询组织详情

下面的 SQL 用于查询组织基础信息。

```sql
SELECT
  `id`,
  `parent_id`,
  `ancestors`,
  `org_code`,
  `org_name`,
  `org_type`,
  `org_level`,
  `leader_employee_id`,
  `org_status`,
  `sort_order`,
  `created_at`,
  `updated_at`
FROM `sys_org`
WHERE `id` = 100002
  AND `is_deleted` = 0;
```

### 查询下级组织

下面的 SQL 用于查询某个组织下的直接子组织。

```sql
SELECT
  `id`,
  `parent_id`,
  `org_code`,
  `org_name`,
  `org_type`,
  `org_level`,
  `org_status`,
  `sort_order`
FROM `sys_org`
WHERE `parent_id` = 100001
  AND `org_status` = 1
  AND `is_deleted` = 0
ORDER BY `sort_order` DESC, `id` ASC;
```

### 递归查询组织树

下面的 SQL 使用 MySQL 8 递归 CTE 查询某个组织及其全部下级组织。

```sql
WITH RECURSIVE org_tree AS (
  SELECT
    `id`,
    `parent_id`,
    `org_code`,
    `org_name`,
    `org_type`,
    `org_level`,
    `sort_order`
  FROM `sys_org`
  WHERE `id` = 100001
    AND `is_deleted` = 0

  UNION ALL

  SELECT
    o.`id`,
    o.`parent_id`,
    o.`org_code`,
    o.`org_name`,
    o.`org_type`,
    o.`org_level`,
    o.`sort_order`
  FROM `sys_org` o
  JOIN org_tree t
    ON o.`parent_id` = t.`id`
  WHERE o.`is_deleted` = 0
)
SELECT
  `id`,
  `parent_id`,
  `org_code`,
  `org_name`,
  `org_type`,
  `org_level`,
  `sort_order`
FROM org_tree
ORDER BY `org_level` ASC, `sort_order` DESC, `id` ASC;
```

### 查询组织员工

下面的 SQL 用于查询某个组织下的在职员工。

```sql
SELECT
  e.`id`,
  e.`employee_no`,
  e.`employee_name`,
  e.`mobile`,
  e.`email`,
  e.`employee_status`,
  e.`entry_date`,
  p.`position_code`,
  p.`position_name`
FROM `sys_org_employee` e
LEFT JOIN `sys_position` p
  ON e.`position_id` = p.`id`
  AND p.`is_deleted` = 0
WHERE e.`org_id` = 100002
  AND e.`employee_status` = 1
  AND e.`is_deleted` = 0
ORDER BY e.`created_at` DESC
LIMIT 10 OFFSET 0;
```

### 查询组织及员工数量

下面的 SQL 用于统计每个组织下的直属员工数量。

```sql
SELECT
  o.`id`,
  o.`org_code`,
  o.`org_name`,
  COUNT(e.`id`) AS `employee_count`
FROM `sys_org` o
LEFT JOIN `sys_org_employee` e
  ON o.`id` = e.`org_id`
  AND e.`employee_status` = 1
  AND e.`is_deleted` = 0
WHERE o.`is_deleted` = 0
GROUP BY
  o.`id`,
  o.`org_code`,
  o.`org_name`
ORDER BY o.`sort_order` DESC;
```

### 查询组织负责人

下面的 SQL 用于查询组织负责人信息，适合审批、通知和通讯录场景。

```sql
SELECT
  o.`id` AS `org_id`,
  o.`org_name`,
  e.`id` AS `leader_employee_id`,
  e.`employee_no`,
  e.`employee_name`,
  e.`mobile`,
  e.`email`
FROM `sys_org` o
LEFT JOIN `sys_org_employee` e
  ON o.`leader_employee_id` = e.`id`
  AND e.`employee_status` = 1
  AND e.`is_deleted` = 0
WHERE o.`id` = 100002
  AND o.`is_deleted` = 0;
```

### 员工调岗

下面的 SQL 用于调整员工所属组织和岗位。

```sql
UPDATE `sys_org_employee`
SET
  `org_id` = 100003,
  `position_id` = 110002,
  `updated_by` = 1
WHERE `id` = 120001
  AND `employee_status` = 1
  AND `is_deleted` = 0;
```

### 员工离职

下面的 SQL 用于将员工状态改为离职，并记录离职日期。

```sql
UPDATE `sys_org_employee`
SET
  `employee_status` = 0,
  `leave_date` = CURDATE(),
  `updated_by` = 1
WHERE `id` = 120001
  AND `employee_status` = 1
  AND `is_deleted` = 0;
```

### 停用组织

下面的 SQL 用于停用组织。实际业务中，停用组织前通常需要检查是否存在启用的下级组织和在职员工。

```sql
UPDATE `sys_org`
SET
  `org_status` = 0,
  `updated_by` = 1
WHERE `id` = 100002
  AND `org_status` = 1
  AND `is_deleted` = 0;
```

### 删除空组织

下面的 SQL 用于逻辑删除没有下级组织、没有在职员工的组织。

```sql
UPDATE `sys_org`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 100002
  AND `is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_org` c
    WHERE c.`parent_id` = 100002
      AND c.`is_deleted` = 0
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_org_employee` e
    WHERE e.`org_id` = 100002
      AND e.`employee_status` = 1
      AND e.`is_deleted` = 0
  );
```

### 索引设计建议

组织架构模型的索引重点是组织父级字段、员工所属组织字段和岗位所属组织字段。

| 表                 | 索引                                                         | 适用场景         |
| ------------------ | ------------------------------------------------------------ | ---------------- |
| `sys_org`          | `PRIMARY KEY (id)`                                           | 查询组织详情     |
| `sys_org`          | `uk_org_code (org_code)`                                     | 根据组织编码查询 |
| `sys_org`          | `idx_parent_status_deleted (parent_id, org_status, is_deleted)` | 查询下级组织     |
| `sys_org`          | `idx_level_status_deleted (org_level, org_status, is_deleted)` | 查询指定层级组织 |
| `sys_position`     | `uk_position_code (position_code)`                           | 根据岗位编码查询 |
| `sys_position`     | `idx_org_status_deleted (org_id, position_status, is_deleted)` | 查询组织下岗位   |
| `sys_org_employee` | `uk_employee_no (employee_no)`                               | 根据员工编号查询 |
| `sys_org_employee` | `idx_org_status_deleted (org_id, employee_status, is_deleted)` | 查询组织下员工   |
| `sys_org_employee` | `idx_position_status_deleted (position_id, employee_status, is_deleted)` | 查询岗位下员工   |

### 使用建议

组织架构模型设计时要明确组织、岗位、员工三者的边界。组织表示管理层级，岗位表示职责定义，员工表示具体人员。不要把岗位和组织混成一张表，也不要只在员工表中用文本保存部门名称。

使用组织架构模型时重点注意：

- 组织表通常是树形结构，使用 `parent_id` 表达上下级
- 岗位归属于组织，员工可以绑定组织和岗位
- 删除组织前必须检查下级组织和在职员工
- 员工调岗只修改员工归属，不应修改历史业务单据
- 负责人字段可以放在组织表，也可以用岗位或关系表扩展
- 组织层级不宜无限扩展，建议限制最大层级
- 数据权限通常基于组织及其下级组织范围计算
- 查询整棵组织树时可以使用递归 CTE，也可以查询平铺数据后由应用层组装

## 商品-SPU-SKU模型

商品-SPU-SKU模型用于电商、库存、交易等业务系统中管理商品。SPU 表示标准产品单元，描述一类商品的公共信息；SKU 表示库存量单位，描述具体可销售规格，例如颜色、尺码、容量等。一个 SPU 通常对应多个 SKU。

下面以“商品 SPU 表”和“商品 SKU 表”为例说明商品-SPU-SKU模型的建表方式和常用 SQL 实践。

### 适用场景

商品-SPU-SKU模型适合商品存在多规格、多价格、多库存、多销售属性的业务场景。

常见场景包括：

- 电商商品管理
- 商品多规格销售
- 库存管理
- 订单下单
- 商品搜索
- 商品上下架
- 商品价格管理
- 商品图片管理
- 商品属性管理
- 商品活动管理

### 建表 SQL

下面的 SQL 创建商品 SPU 表和商品 SKU 表。SPU 保存商品公共信息，SKU 保存具体规格、价格和库存信息。

```sql
CREATE TABLE `mall_product_spu` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `spu_code` VARCHAR(64) NOT NULL COMMENT 'SPU编码',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `brand_id` BIGINT NULL COMMENT '品牌ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `product_subtitle` VARCHAR(500) NULL COMMENT '商品副标题',
  `main_image_url` VARCHAR(500) NULL COMMENT '商品主图',
  `album_json` JSON NULL COMMENT '商品相册JSON',
  `product_status` TINYINT NOT NULL DEFAULT 0 COMMENT '商品状态：0草稿，1上架，2下架',
  `sale_count` INT NOT NULL DEFAULT 0 COMMENT '销量',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `description` TEXT NULL COMMENT '商品详情',
  `attr_json` JSON NULL COMMENT '商品公共属性JSON',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_spu_code` (`spu_code`),
  KEY `idx_category_status_deleted` (`category_id`, `product_status`, `is_deleted`),
  KEY `idx_brand_status_deleted` (`brand_id`, `product_status`, `is_deleted`),
  KEY `idx_status_sort` (`product_status`, `sort_order`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='商城商品SPU表';

CREATE TABLE `mall_product_sku` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU编码',
  `spu_id` BIGINT NOT NULL COMMENT 'SPU ID',
  `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称',
  `sku_image_url` VARCHAR(500) NULL COMMENT 'SKU图片',
  `spec_json` JSON NOT NULL COMMENT '规格信息JSON，例如颜色、尺码、容量',
  `sale_price` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '销售价格',
  `market_price` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '市场价格',
  `cost_price` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '成本价格',
  `stock_quantity` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `locked_stock_quantity` INT NOT NULL DEFAULT 0 COMMENT '锁定库存数量',
  `sku_status` TINYINT NOT NULL DEFAULT 1 COMMENT 'SKU状态：0禁用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `weight` DECIMAL(18, 3) NULL COMMENT '重量',
  `volume` DECIMAL(18, 3) NULL COMMENT '体积',
  `barcode` VARCHAR(128) NULL COMMENT '商品条码',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_spu_status_deleted` (`spu_id`, `sku_status`, `is_deleted`),
  KEY `idx_price` (`sale_price`),
  KEY `idx_stock_quantity` (`stock_quantity`),
  KEY `idx_barcode` (`barcode`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='商城商品SKU表';
```

### 字段设计说明

SPU 表保存商品公共字段，SKU 表保存规格、价格、库存等销售字段。商品名称、分类、品牌、主图等通常属于 SPU；规格组合、售价、库存、条码等通常属于 SKU。

| 表                 | 字段                    | 说明          |
| ------------------ | ----------------------- | ------------- |
| `mall_product_spu` | `spu_code`              | SPU 编码      |
| `mall_product_spu` | `category_id`           | 商品分类 ID   |
| `mall_product_spu` | `brand_id`              | 品牌 ID       |
| `mall_product_spu` | `product_name`          | 商品名称      |
| `mall_product_spu` | `product_status`        | 商品状态      |
| `mall_product_sku` | `sku_code`              | SKU 编码      |
| `mall_product_sku` | `spu_id`                | 所属 SPU ID   |
| `mall_product_sku` | `spec_json`             | 销售规格 JSON |
| `mall_product_sku` | `sale_price`            | 销售价格      |
| `mall_product_sku` | `stock_quantity`        | 库存数量      |
| `mall_product_sku` | `locked_stock_quantity` | 锁定库存数量  |

### 新增 SPU

下面的 SQL 用于新增商品 SPU 信息。

```sql
INSERT INTO `mall_product_spu` (
  `id`,
  `spu_code`,
  `category_id`,
  `brand_id`,
  `product_name`,
  `product_subtitle`,
  `main_image_url`,
  `album_json`,
  `product_status`,
  `sale_count`,
  `sort_order`,
  `description`,
  `attr_json`,
  `created_by`,
  `updated_by`
) VALUES (
  200001,
  'SPU_202605110001',
  100002,
  300001,
  '机械键盘',
  '高性能办公游戏机械键盘',
  'https://example.com/product/keyboard-main.png',
  JSON_ARRAY(
    'https://example.com/product/keyboard-1.png',
    'https://example.com/product/keyboard-2.png'
  ),
  0,
  0,
  100,
  '机械键盘商品详情',
  JSON_OBJECT('brand', '示例品牌', 'material', 'ABS'),
  1,
  1
);
```

### 新增 SKU

下面的 SQL 用于给 SPU 新增多个 SKU。每个 SKU 保存具体规格、价格和库存。

```sql
INSERT INTO `mall_product_sku` (
  `id`,
  `sku_code`,
  `spu_id`,
  `sku_name`,
  `sku_image_url`,
  `spec_json`,
  `sale_price`,
  `market_price`,
  `cost_price`,
  `stock_quantity`,
  `locked_stock_quantity`,
  `sku_status`,
  `sort_order`,
  `weight`,
  `volume`,
  `barcode`,
  `created_by`,
  `updated_by`
) VALUES
(
  210001,
  'SKU_202605110001',
  200001,
  '机械键盘 黑色 青轴',
  'https://example.com/product/keyboard-black-blue.png',
  JSON_OBJECT('color', '黑色', 'switch', '青轴'),
  199.00,
  259.00,
  120.00,
  100,
  0,
  1,
  100,
  0.850,
  0.005,
  '690000000001',
  1,
  1
),
(
  210002,
  'SKU_202605110002',
  200001,
  '机械键盘 白色 红轴',
  'https://example.com/product/keyboard-white-red.png',
  JSON_OBJECT('color', '白色', 'switch', '红轴'),
  219.00,
  279.00,
  130.00,
  80,
  0,
  1,
  90,
  0.850,
  0.005,
  '690000000002',
  1,
  1
);
```

### 查询 SPU 详情

下面的 SQL 用于查询商品 SPU 基础信息。

```sql
SELECT
  `id`,
  `spu_code`,
  `category_id`,
  `brand_id`,
  `product_name`,
  `product_subtitle`,
  `main_image_url`,
  `album_json`,
  `product_status`,
  `sale_count`,
  `sort_order`,
  `description`,
  `attr_json`,
  `created_at`,
  `updated_at`
FROM `mall_product_spu`
WHERE `id` = 200001
  AND `is_deleted` = 0;
```

### 查询 SPU 下的 SKU

下面的 SQL 用于查询某个商品下的所有可用 SKU，适合商品详情页规格展示。

```sql
SELECT
  `id`,
  `sku_code`,
  `spu_id`,
  `sku_name`,
  `sku_image_url`,
  `spec_json`,
  `sale_price`,
  `market_price`,
  `stock_quantity`,
  `locked_stock_quantity`,
  `sku_status`,
  `sort_order`
FROM `mall_product_sku`
WHERE `spu_id` = 200001
  AND `sku_status` = 1
  AND `is_deleted` = 0
ORDER BY `sort_order` DESC, `id` ASC;
```

### 查询商品列表

下面的 SQL 用于分页查询商品列表。列表页一般只查询 SPU 信息，不直接展开全部 SKU。

```sql
SELECT
  `id`,
  `spu_code`,
  `category_id`,
  `brand_id`,
  `product_name`,
  `product_subtitle`,
  `main_image_url`,
  `product_status`,
  `sale_count`,
  `sort_order`,
  `created_at`
FROM `mall_product_spu`
WHERE `is_deleted` = 0
  AND `category_id` = 100002
  AND `product_status` = 1
ORDER BY `sort_order` DESC, `created_at` DESC
LIMIT 10 OFFSET 0;
```

### 查询商品价格区间

下面的 SQL 用于查询某个 SPU 下 SKU 的最低价和最高价，适合商品列表展示价格区间。

```sql
SELECT
  `spu_id`,
  MIN(`sale_price`) AS `min_sale_price`,
  MAX(`sale_price`) AS `max_sale_price`
FROM `mall_product_sku`
WHERE `spu_id` = 200001
  AND `sku_status` = 1
  AND `is_deleted` = 0
GROUP BY `spu_id`;
```

### 查询商品总库存

下面的 SQL 用于统计某个 SPU 下所有 SKU 的可用库存。

```sql
SELECT
  `spu_id`,
  SUM(`stock_quantity` - `locked_stock_quantity`) AS `available_stock_quantity`
FROM `mall_product_sku`
WHERE `spu_id` = 200001
  AND `sku_status` = 1
  AND `is_deleted` = 0
GROUP BY `spu_id`;
```

### 根据 SKU 查询商品

下面的 SQL 用于根据 SKU 查询商品和规格信息，适合下单、购物车、库存校验等场景。

```sql
SELECT
  s.`id` AS `spu_id`,
  s.`spu_code`,
  s.`product_name`,
  s.`product_status`,
  k.`id` AS `sku_id`,
  k.`sku_code`,
  k.`sku_name`,
  k.`spec_json`,
  k.`sale_price`,
  k.`stock_quantity`,
  k.`locked_stock_quantity`,
  k.`sku_status`
FROM `mall_product_sku` k
JOIN `mall_product_spu` s
  ON k.`spu_id` = s.`id`
  AND s.`is_deleted` = 0
WHERE k.`id` = 210001
  AND k.`is_deleted` = 0;
```

### 根据规格查询 SKU

下面的 SQL 用于根据 JSON 规格查询 SKU。JSON 查询适合低频场景，高频规格筛选建议拆分规格属性表。

```sql
SELECT
  `id`,
  `sku_code`,
  `sku_name`,
  `spec_json`,
  `sale_price`,
  `stock_quantity`
FROM `mall_product_sku`
WHERE `spu_id` = 200001
  AND JSON_UNQUOTE(JSON_EXTRACT(`spec_json`, '$.color')) = '黑色'
  AND JSON_UNQUOTE(JSON_EXTRACT(`spec_json`, '$.switch')) = '青轴'
  AND `sku_status` = 1
  AND `is_deleted` = 0;
```

### 商品上架

下面的 SQL 用于将商品 SPU 上架。实际业务中，上架前通常需要检查是否存在可用 SKU、价格是否有效、库存是否有效。

```sql
UPDATE `mall_product_spu`
SET
  `product_status` = 1,
  `updated_by` = 1
WHERE `id` = 200001
  AND `product_status` IN (0, 2)
  AND `is_deleted` = 0
  AND EXISTS (
    SELECT 1
    FROM `mall_product_sku` k
    WHERE k.`spu_id` = 200001
      AND k.`sku_status` = 1
      AND k.`stock_quantity` > 0
      AND k.`is_deleted` = 0
  );
```

### 商品下架

下面的 SQL 用于将商品 SPU 下架。商品下架后，前台通常不再展示和销售。

```sql
UPDATE `mall_product_spu`
SET
  `product_status` = 2,
  `updated_by` = 1
WHERE `id` = 200001
  AND `product_status` = 1
  AND `is_deleted` = 0;
```

### 修改 SKU 价格

下面的 SQL 用于修改 SKU 价格。

```sql
UPDATE `mall_product_sku`
SET
  `sale_price` = 189.00,
  `market_price` = 249.00,
  `updated_by` = 1
WHERE `id` = 210001
  AND `sku_status` = 1
  AND `is_deleted` = 0;
```

### 扣减库存

下面的 SQL 用于下单成功时扣减库存。更新条件中需要判断可用库存是否充足，避免超卖。

```sql
UPDATE `mall_product_sku`
SET
  `stock_quantity` = `stock_quantity` - 1,
  `updated_by` = 1
WHERE `id` = 210001
  AND (`stock_quantity` - `locked_stock_quantity`) >= 1
  AND `sku_status` = 1
  AND `is_deleted` = 0;
```

### 锁定库存

下面的 SQL 用于提交订单但尚未支付时锁定库存。锁定库存后，可用库存会减少，但总库存不变。

```sql
UPDATE `mall_product_sku`
SET
  `locked_stock_quantity` = `locked_stock_quantity` + 1,
  `updated_by` = 1
WHERE `id` = 210001
  AND (`stock_quantity` - `locked_stock_quantity`) >= 1
  AND `sku_status` = 1
  AND `is_deleted` = 0;
```

### 释放锁定库存

下面的 SQL 用于订单取消或支付超时时释放锁定库存。

```sql
UPDATE `mall_product_sku`
SET
  `locked_stock_quantity` = `locked_stock_quantity` - 1,
  `updated_by` = 1
WHERE `id` = 210001
  AND `locked_stock_quantity` >= 1
  AND `is_deleted` = 0;
```

### 支付成功扣减锁定库存

下面的 SQL 用于订单支付成功后，将锁定库存转为真实扣减库存。

```sql
UPDATE `mall_product_sku`
SET
  `stock_quantity` = `stock_quantity` - 1,
  `locked_stock_quantity` = `locked_stock_quantity` - 1,
  `updated_by` = 1
WHERE `id` = 210001
  AND `stock_quantity` >= 1
  AND `locked_stock_quantity` >= 1
  AND `sku_status` = 1
  AND `is_deleted` = 0;
```

### 查询低库存 SKU

下面的 SQL 用于查询库存不足的 SKU，适合库存预警。

```sql
SELECT
  k.`id`,
  k.`sku_code`,
  k.`sku_name`,
  k.`stock_quantity`,
  k.`locked_stock_quantity`,
  (k.`stock_quantity` - k.`locked_stock_quantity`) AS `available_stock_quantity`,
  s.`product_name`
FROM `mall_product_sku` k
JOIN `mall_product_spu` s
  ON k.`spu_id` = s.`id`
  AND s.`is_deleted` = 0
WHERE k.`sku_status` = 1
  AND k.`is_deleted` = 0
  AND (k.`stock_quantity` - k.`locked_stock_quantity`) <= 10
ORDER BY `available_stock_quantity` ASC;
```

### 逻辑删除 SPU

下面的 SQL 用于逻辑删除商品 SPU。实际业务中，删除 SPU 前通常需要先下架商品，并同步处理 SKU。

```sql
UPDATE `mall_product_spu`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 200001
  AND `product_status` <> 1
  AND `is_deleted` = 0;
```

### 逻辑删除 SKU

下面的 SQL 用于逻辑删除某个 SKU。已产生订单的 SKU 通常不建议物理删除。

```sql
UPDATE `mall_product_sku`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 210001
  AND `is_deleted` = 0;
```

### 索引设计建议

商品-SPU-SKU模型的索引重点是分类查询、商品状态查询、SPU 与 SKU 关联查询、SKU 编码查询和库存查询。

| 表                 | 索引                                                         | 适用场景          |
| ------------------ | ------------------------------------------------------------ | ----------------- |
| `mall_product_spu` | `PRIMARY KEY (id)`                                           | 查询 SPU 详情     |
| `mall_product_spu` | `uk_spu_code (spu_code)`                                     | 根据 SPU 编码查询 |
| `mall_product_spu` | `idx_category_status_deleted (category_id, product_status, is_deleted)` | 分类商品列表      |
| `mall_product_spu` | `idx_brand_status_deleted (brand_id, product_status, is_deleted)` | 品牌商品列表      |
| `mall_product_spu` | `idx_status_sort (product_status, sort_order)`               | 上架商品排序      |
| `mall_product_sku` | `PRIMARY KEY (id)`                                           | 查询 SKU 详情     |
| `mall_product_sku` | `uk_sku_code (sku_code)`                                     | 根据 SKU 编码查询 |
| `mall_product_sku` | `idx_spu_status_deleted (spu_id, sku_status, is_deleted)`    | 查询 SPU 下 SKU   |
| `mall_product_sku` | `idx_price (sale_price)`                                     | 价格筛选          |
| `mall_product_sku` | `idx_stock_quantity (stock_quantity)`                        | 库存查询          |
| `mall_product_sku` | `idx_barcode (barcode)`                                      | 根据条码查询      |

### 使用建议

商品-SPU-SKU模型的核心是把商品公共信息和具体销售规格拆开。SPU 面向展示和管理，SKU 面向销售、库存和下单。订单明细通常保存 SKU ID，同时冗余商品名称、规格、价格等快照字段。

使用商品-SPU-SKU模型时重点注意：

- SPU 保存商品公共信息，SKU 保存规格、价格和库存
- 一个 SPU 可以有多个 SKU
- 下单、库存、价格通常以 SKU 为准
- 商品详情页通常查询 SPU 和 SKU 列表
- 商品列表页通常只查 SPU，不展开全部 SKU
- 订单明细应保存 SKU 快照，避免商品修改影响历史订单
- 库存扣减必须带库存充足条件，避免超卖
- 锁定库存和实际库存扣减要区分清楚
- JSON 规格适合中小规模和低频筛选，高频筛选建议拆分规格属性表
- 已产生订单的 SKU 不建议物理删除，优先使用逻辑删除或禁用状态

## 订单-订单明细模型

订单-订单明细模型是交易类系统中最常用的业务模型之一。订单主表保存订单整体信息，订单明细表保存商品、数量、单价、优惠、实付金额等明细信息。主表体现一次交易的整体状态，从表体现这次交易包含的具体商品或服务。

下面以“订单表”和“订单明细表”为例说明订单-订单明细模型的建表方式和常用 SQL 实践。

### 适用场景

订单-订单明细模型适合一个业务单据下存在多条明细数据的交易场景。

常见场景包括：

- 电商订单
- 采购订单
- 销售订单
- 充值订单
- 服务订单
- 课程订单
- 会员订单
- 入库单
- 出库单
- 结算单

### 建表 SQL

下面的 SQL 创建订单主表和订单明细表。订单主表保存订单整体信息，订单明细表保存订单中的商品快照、价格、数量和金额。

```sql
CREATE TABLE `mall_order` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `order_source` TINYINT NOT NULL DEFAULT 1 COMMENT '订单来源：1后台，2小程序，3APP，4网页',
  `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已发货，3已完成，4已取消，5已关闭',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0未支付，1已支付，2已退款，3部分退款',
  `pay_type` TINYINT NULL COMMENT '支付方式：1余额，2微信，3支付宝，4银行卡',
  `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  `discount_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `freight_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '运费金额',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  `receiver_name` VARCHAR(64) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(32) NOT NULL COMMENT '收货人手机号',
  `receiver_address` VARCHAR(500) NOT NULL COMMENT '收货地址',
  `pay_time` DATETIME NULL COMMENT '支付时间',
  `delivery_time` DATETIME NULL COMMENT '发货时间',
  `finish_time` DATETIME NULL COMMENT '完成时间',
  `cancel_time` DATETIME NULL COMMENT '取消时间',
  `close_time` DATETIME NULL COMMENT '关闭时间',
  `remark` VARCHAR(500) NULL COMMENT '订单备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status_deleted` (`user_id`, `order_status`, `is_deleted`),
  KEY `idx_order_status_created` (`order_status`, `created_at`),
  KEY `idx_pay_status_created` (`pay_status`, `created_at`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='商城订单表';

CREATE TABLE `mall_order_item` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号，冗余字段',
  `spu_id` BIGINT NOT NULL COMMENT 'SPU ID',
  `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
  `spu_name` VARCHAR(200) NOT NULL COMMENT '商品名称快照',
  `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称快照',
  `sku_image_url` VARCHAR(500) NULL COMMENT 'SKU图片快照',
  `spec_json` JSON NULL COMMENT '规格信息快照',
  `unit_price` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '商品单价',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '明细总金额',
  `discount_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '明细优惠金额',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '明细实付金额',
  `item_status` TINYINT NOT NULL DEFAULT 0 COMMENT '明细状态：0正常，1退款中，2已退款，3已取消',
  `refund_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `remark` VARCHAR(500) NULL COMMENT '明细备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id_deleted` (`order_id`, `is_deleted`),
  KEY `idx_order_no_deleted` (`order_no`, `is_deleted`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_spu_id` (`spu_id`),
  KEY `idx_item_status` (`item_status`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='商城订单明细表';
```

### 字段设计说明

订单主表和订单明细表需要职责清晰。订单主表保存订单整体状态、用户、金额、收货信息和关键时间；订单明细表保存商品快照、规格快照、数量、单价和明细金额。

| 表                | 字段              | 说明                         |
| ----------------- | ----------------- | ---------------------------- |
| `mall_order`      | `order_no`        | 订单编号，业务唯一标识       |
| `mall_order`      | `user_id`         | 下单用户 ID                  |
| `mall_order`      | `order_status`    | 订单状态                     |
| `mall_order`      | `pay_status`      | 支付状态                     |
| `mall_order`      | `total_amount`    | 订单商品总金额               |
| `mall_order`      | `discount_amount` | 订单优惠金额                 |
| `mall_order`      | `freight_amount`  | 运费金额                     |
| `mall_order`      | `pay_amount`      | 订单实付金额                 |
| `mall_order_item` | `order_id`        | 关联订单主表 ID              |
| `mall_order_item` | `order_no`        | 冗余订单编号，便于排查和对账 |
| `mall_order_item` | `sku_id`          | 下单 SKU ID                  |
| `mall_order_item` | `spu_name`        | 商品名称快照                 |
| `mall_order_item` | `spec_json`       | 商品规格快照                 |
| `mall_order_item` | `quantity`        | 购买数量                     |
| `mall_order_item` | `pay_amount`      | 明细实付金额                 |

### 新增订单主表

下面的 SQL 用于新增订单主表数据。实际业务中，订单主表和订单明细表应放在同一个事务中创建。

```sql
INSERT INTO `mall_order` (
  `id`,
  `order_no`,
  `user_id`,
  `order_source`,
  `order_status`,
  `pay_status`,
  `total_amount`,
  `discount_amount`,
  `freight_amount`,
  `pay_amount`,
  `receiver_name`,
  `receiver_phone`,
  `receiver_address`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  'ORDER_202605110001',
  200001,
  2,
  0,
  0,
  399.00,
  30.00,
  10.00,
  379.00,
  '张三',
  '13800000000',
  '北京市朝阳区示例路 100 号',
  '用户提交订单',
  200001,
  200001
);
```

### 新增订单明细

下面的 SQL 用于新增订单明细数据。明细表中的商品名称、规格、价格应保存下单时的快照，避免商品后续变更影响历史订单。

```sql
INSERT INTO `mall_order_item` (
  `id`,
  `order_id`,
  `order_no`,
  `spu_id`,
  `sku_id`,
  `spu_name`,
  `sku_name`,
  `sku_image_url`,
  `spec_json`,
  `unit_price`,
  `quantity`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `item_status`,
  `remark`
) VALUES
(
  110001,
  100001,
  'ORDER_202605110001',
  300001,
  310001,
  '机械键盘',
  '机械键盘 黑色 青轴',
  'https://example.com/product/keyboard.png',
  JSON_OBJECT('color', '黑色', 'switch', '青轴'),
  299.00,
  1,
  299.00,
  20.00,
  279.00,
  0,
  '商品明细1'
),
(
  110002,
  100001,
  'ORDER_202605110001',
  300002,
  310002,
  '无线鼠标',
  '无线鼠标 白色',
  'https://example.com/product/mouse.png',
  JSON_OBJECT('color', '白色'),
  100.00,
  1,
  100.00,
  10.00,
  90.00,
  0,
  '商品明细2'
);
```

### 查询订单详情

下面的 SQL 用于查询订单主表详情，适合订单详情页展示订单基础信息。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_source`,
  `order_status`,
  `pay_status`,
  `pay_type`,
  `total_amount`,
  `discount_amount`,
  `freight_amount`,
  `pay_amount`,
  `receiver_name`,
  `receiver_phone`,
  `receiver_address`,
  `pay_time`,
  `delivery_time`,
  `finish_time`,
  `cancel_time`,
  `close_time`,
  `remark`,
  `created_at`,
  `updated_at`
FROM `mall_order`
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 查询订单明细

下面的 SQL 用于查询订单下的商品明细，适合订单详情页展示商品列表。

```sql
SELECT
  `id`,
  `order_id`,
  `order_no`,
  `spu_id`,
  `sku_id`,
  `spu_name`,
  `sku_name`,
  `sku_image_url`,
  `spec_json`,
  `unit_price`,
  `quantity`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `item_status`,
  `refund_amount`
FROM `mall_order_item`
WHERE `order_id` = 100001
  AND `is_deleted` = 0
ORDER BY `id` ASC;
```

### 关联查询订单和明细

下面的 SQL 用于一次性查询订单和订单明细，适合详情页，不建议用于大分页列表。

```sql
SELECT
  o.`id` AS `order_id`,
  o.`order_no`,
  o.`user_id`,
  o.`order_status`,
  o.`pay_status`,
  o.`total_amount` AS `order_total_amount`,
  o.`discount_amount` AS `order_discount_amount`,
  o.`freight_amount`,
  o.`pay_amount` AS `order_pay_amount`,
  i.`id` AS `item_id`,
  i.`spu_id`,
  i.`sku_id`,
  i.`spu_name`,
  i.`sku_name`,
  i.`spec_json`,
  i.`unit_price`,
  i.`quantity`,
  i.`total_amount` AS `item_total_amount`,
  i.`discount_amount` AS `item_discount_amount`,
  i.`pay_amount` AS `item_pay_amount`,
  i.`item_status`
FROM `mall_order` o
LEFT JOIN `mall_order_item` i
  ON o.`id` = i.`order_id`
  AND i.`is_deleted` = 0
WHERE o.`id` = 100001
  AND o.`is_deleted` = 0
ORDER BY i.`id` ASC;
```

### 分页查询用户订单

下面的 SQL 用于查询用户订单列表。订单列表通常只查询主表，避免关联明细表导致分页不准确。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_type`,
  `total_amount`,
  `discount_amount`,
  `freight_amount`,
  `pay_amount`,
  `created_at`
FROM `mall_order`
WHERE `user_id` = 200001
  AND `order_status` = 0
  AND `is_deleted` = 0
ORDER BY `created_at` DESC
LIMIT 10 OFFSET 0;
```

### 根据订单编号查询

下面的 SQL 用于根据订单编号查询订单，适合支付回调、客服查询、对账和幂等处理。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_type`,
  `pay_amount`,
  `created_at`
FROM `mall_order`
WHERE `order_no` = 'ORDER_202605110001'
  AND `is_deleted` = 0;
```

### 支付订单

下面的 SQL 用于支付成功后更新订单状态。更新条件中带上原状态，可以避免重复支付回调造成状态错乱。

```sql
UPDATE `mall_order`
SET
  `order_status` = 1,
  `pay_status` = 1,
  `pay_type` = 2,
  `pay_time` = NOW(),
  `updated_by` = 200001
WHERE `order_no` = 'ORDER_202605110001'
  AND `order_status` = 0
  AND `pay_status` = 0
  AND `is_deleted` = 0;
```

### 订单发货

下面的 SQL 用于订单发货，适合已支付订单进入物流发货状态。

```sql
UPDATE `mall_order`
SET
  `order_status` = 2,
  `delivery_time` = NOW(),
  `updated_by` = 1
WHERE `order_no` = 'ORDER_202605110001'
  AND `order_status` = 1
  AND `pay_status` = 1
  AND `is_deleted` = 0;
```

### 完成订单

下面的 SQL 用于确认收货或系统自动完成订单。

```sql
UPDATE `mall_order`
SET
  `order_status` = 3,
  `finish_time` = NOW(),
  `updated_by` = 200001
WHERE `order_no` = 'ORDER_202605110001'
  AND `order_status` = 2
  AND `pay_status` = 1
  AND `is_deleted` = 0;
```

### 取消订单

下面的 SQL 用于取消待支付订单。已支付订单通常不能直接取消，应走退款或售后流程。

```sql
UPDATE `mall_order`
SET
  `order_status` = 4,
  `cancel_time` = NOW(),
  `updated_by` = 200001
WHERE `order_no` = 'ORDER_202605110001'
  AND `order_status` = 0
  AND `pay_status` = 0
  AND `is_deleted` = 0;
```

### 关闭订单

下面的 SQL 用于关闭超时未支付订单。

```sql
UPDATE `mall_order`
SET
  `order_status` = 5,
  `close_time` = NOW(),
  `updated_by` = 1
WHERE `order_status` = 0
  AND `pay_status` = 0
  AND `created_at` < DATE_SUB(NOW(), INTERVAL 30 MINUTE)
  AND `is_deleted` = 0;
```

### 更新明细退款状态

下面的 SQL 用于将某条订单明细标记为退款中。

```sql
UPDATE `mall_order_item`
SET
  `item_status` = 1,
  `refund_amount` = 279.00
WHERE `id` = 110001
  AND `order_id` = 100001
  AND `item_status` = 0
  AND `is_deleted` = 0;
```

### 统计订单金额

下面的 SQL 用于按用户统计已支付或已完成订单金额。

```sql
SELECT
  `user_id`,
  COUNT(*) AS `order_count`,
  SUM(`total_amount`) AS `total_amount`,
  SUM(`discount_amount`) AS `discount_amount`,
  SUM(`freight_amount`) AS `freight_amount`,
  SUM(`pay_amount`) AS `pay_amount`
FROM `mall_order`
WHERE `order_status` IN (1, 2, 3)
  AND `pay_status` = 1
  AND `is_deleted` = 0
GROUP BY `user_id`;
```

### 按商品统计销量

下面的 SQL 用于按 SKU 统计销量，适合商品报表和运营分析。

```sql
SELECT
  `sku_id`,
  `sku_name`,
  SUM(`quantity`) AS `sale_quantity`,
  SUM(`pay_amount`) AS `sale_amount`
FROM `mall_order_item`
WHERE `item_status` = 0
  AND `is_deleted` = 0
GROUP BY
  `sku_id`,
  `sku_name`
ORDER BY `sale_quantity` DESC;
```

### 校验订单金额一致性

下面的 SQL 用于检查订单主表金额和明细汇总金额是否一致。

```sql
SELECT
  o.`id`,
  o.`order_no`,
  o.`total_amount` AS `order_total_amount`,
  t.`item_total_amount`,
  o.`discount_amount` AS `order_discount_amount`,
  t.`item_discount_amount`,
  o.`pay_amount` AS `order_pay_amount`,
  t.`item_pay_amount`
FROM `mall_order` o
JOIN (
  SELECT
    `order_id`,
    SUM(`total_amount`) AS `item_total_amount`,
    SUM(`discount_amount`) AS `item_discount_amount`,
    SUM(`pay_amount`) AS `item_pay_amount`
  FROM `mall_order_item`
  WHERE `is_deleted` = 0
  GROUP BY `order_id`
) t ON o.`id` = t.`order_id`
WHERE o.`is_deleted` = 0
  AND (
    o.`total_amount` <> t.`item_total_amount`
    OR o.`discount_amount` <> t.`item_discount_amount`
    OR o.`pay_amount` <> t.`item_pay_amount`
  );
```

### 逻辑删除订单

下面的 SQL 用于逻辑删除订单主表。订单类数据通常不建议物理删除。

```sql
UPDATE `mall_order`
SET
  `is_deleted` = 1,
  `updated_by` = 200001
WHERE `id` = 100001
  AND `order_status` IN (4, 5)
  AND `is_deleted` = 0;
```

### 逻辑删除订单明细

下面的 SQL 用于逻辑删除订单明细。实际业务中，主表和明细表的删除操作应放在同一个事务中。

```sql
UPDATE `mall_order_item`
SET
  `is_deleted` = 1
WHERE `order_id` = 100001
  AND `is_deleted` = 0;
```

### 索引设计建议

订单-订单明细模型的索引重点是订单编号、用户订单列表、订单状态查询、支付状态查询和明细表按订单查询。

| 表                | 索引                                                         | 适用场景                             |
| ----------------- | ------------------------------------------------------------ | ------------------------------------ |
| `mall_order`      | `PRIMARY KEY (id)`                                           | 查询订单详情                         |
| `mall_order`      | `uk_order_no (order_no)`                                     | 根据订单编号查询、支付回调、幂等处理 |
| `mall_order`      | `idx_user_status_deleted (user_id, order_status, is_deleted)` | 查询用户订单列表                     |
| `mall_order`      | `idx_order_status_created (order_status, created_at)`        | 后台按状态分页查询                   |
| `mall_order`      | `idx_pay_status_created (pay_status, created_at)`            | 支付状态查询和统计                   |
| `mall_order_item` | `idx_order_id_deleted (order_id, is_deleted)`                | 查询订单明细                         |
| `mall_order_item` | `idx_order_no_deleted (order_no, is_deleted)`                | 根据订单编号查询明细                 |
| `mall_order_item` | `idx_sku_id (sku_id)`                                        | 按 SKU 统计销量                      |
| `mall_order_item` | `idx_spu_id (spu_id)`                                        | 按 SPU 统计销量                      |

### 使用建议

订单-订单明细模型中，订单主表和订单明细表应保持金额一致、状态一致和生命周期一致。订单创建、库存锁定、优惠计算、金额落库通常需要放在同一个事务或可靠业务流程中处理。

使用订单-订单明细模型时重点注意：

- 订单主表保存整体信息，订单明细表保存商品快照
- 订单编号必须唯一，适合支付回调和对账
- 明细表建议冗余 `order_no`，便于排查和统计
- 商品名称、规格、价格必须保存下单时快照
- 订单列表分页尽量只查主表，不要直接关联明细表分页
- 订单金额应由明细金额、优惠金额、运费金额计算得出
- 支付、取消、发货、完成等状态更新必须带原状态条件
- 订单数据通常不物理删除，优先使用逻辑删除或归档
- 订单创建和明细创建应放在同一个事务中
- 支付成功、库存扣减、账户流水等操作应保证幂等

## 账户流水模型

账户流水模型用于记录账户余额的每一次变动。账户表保存当前余额，流水表记录余额变化明细。账户余额可以被更新，但账户流水原则上只新增、不修改、不删除，用于审计、对账、追踪和问题排查。

下面以“用户账户表”和“账户流水表”为例说明账户流水模型的建表方式和常用 SQL 实践。

### 适用场景

账户流水模型适合所有涉及余额、额度、积分、钱包、资金、虚拟币、库存额度等可增减数值的业务场景。

常见场景包括：

- 用户余额账户
- 钱包账户
- 积分账户
- 会员储值账户
- 商户结算账户
- 平台资金账户
- 冻结金额账户
- 授信额度账户
- 优惠券额度账户
- 虚拟币账户

### 建表 SQL

下面的 SQL 创建账户表和账户流水表。账户表保存当前余额、冻结金额和版本号；账户流水表记录每一次余额变动前后的金额。

```sql
CREATE TABLE `acct_user_account` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `account_no` VARCHAR(64) NOT NULL COMMENT '账户编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `account_type` TINYINT NOT NULL DEFAULT 1 COMMENT '账户类型：1余额，2积分，3赠送金',
  `available_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
  `frozen_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
  `total_income_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '累计收入金额',
  `total_expense_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '累计支出金额',
  `account_status` TINYINT NOT NULL DEFAULT 1 COMMENT '账户状态：0冻结，1正常',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号，用于乐观锁',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_no` (`account_no`),
  UNIQUE KEY `uk_user_type_deleted` (`user_id`, `account_type`, `is_deleted`),
  KEY `idx_status_deleted` (`account_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户账户表';

CREATE TABLE `acct_account_flow` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `flow_no` VARCHAR(64) NOT NULL COMMENT '流水编号',
  `account_id` BIGINT NOT NULL COMMENT '账户ID',
  `account_no` VARCHAR(64) NOT NULL COMMENT '账户编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `account_type` TINYINT NOT NULL COMMENT '账户类型：1余额，2积分，3赠送金',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型：recharge充值，consume消费，refund退款，freeze冻结，unfreeze解冻',
  `biz_no` VARCHAR(64) NOT NULL COMMENT '业务单号',
  `change_direction` TINYINT NOT NULL COMMENT '变动方向：1收入，2支出，3冻结，4解冻',
  `change_amount` DECIMAL(18, 2) NOT NULL COMMENT '变动金额',
  `before_available_amount` DECIMAL(18, 2) NOT NULL COMMENT '变动前可用余额',
  `after_available_amount` DECIMAL(18, 2) NOT NULL COMMENT '变动后可用余额',
  `before_frozen_amount` DECIMAL(18, 2) NOT NULL COMMENT '变动前冻结金额',
  `after_frozen_amount` DECIMAL(18, 2) NOT NULL COMMENT '变动后冻结金额',
  `flow_status` TINYINT NOT NULL DEFAULT 1 COMMENT '流水状态：0失败，1成功',
  `operate_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `remark` VARCHAR(500) NULL COMMENT '流水备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_no` (`flow_no`),
  UNIQUE KEY `uk_biz_type_no` (`biz_type`, `biz_no`),
  KEY `idx_account_time` (`account_id`, `operate_time`),
  KEY `idx_account_no_time` (`account_no`, `operate_time`),
  KEY `idx_user_type_time` (`user_id`, `account_type`, `operate_time`),
  KEY `idx_biz_no` (`biz_no`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='账户流水表';
```

### 字段设计说明

账户流水模型的核心是账户表和流水表。账户表记录当前结果，流水表记录变化过程。排查问题时，应优先通过流水表还原余额变化过程。

| 表                  | 字段                      | 说明           |
| ------------------- | ------------------------- | -------------- |
| `acct_user_account` | `account_no`              | 账户编号       |
| `acct_user_account` | `user_id`                 | 用户 ID        |
| `acct_user_account` | `available_amount`        | 可用余额       |
| `acct_user_account` | `frozen_amount`           | 冻结金额       |
| `acct_user_account` | `version`                 | 乐观锁版本号   |
| `acct_account_flow` | `flow_no`                 | 流水编号       |
| `acct_account_flow` | `biz_type`                | 业务类型       |
| `acct_account_flow` | `biz_no`                  | 业务单号       |
| `acct_account_flow` | `change_amount`           | 变动金额       |
| `acct_account_flow` | `before_available_amount` | 变动前可用余额 |
| `acct_account_flow` | `after_available_amount`  | 变动后可用余额 |

### 新增账户

下面的 SQL 用于给用户创建余额账户。

```sql
INSERT INTO `acct_user_account` (
  `id`,
  `account_no`,
  `user_id`,
  `account_type`,
  `available_amount`,
  `frozen_amount`,
  `total_income_amount`,
  `total_expense_amount`,
  `account_status`,
  `created_by`,
  `updated_by`
) VALUES (
  200001,
  'ACCT_202605110001',
  300001,
  1,
  0.00,
  0.00,
  0.00,
  0.00,
  1,
  1,
  1
);
```

### 查询账户余额

下面的 SQL 用于查询用户账户当前余额。

```sql
SELECT
  `id`,
  `account_no`,
  `user_id`,
  `account_type`,
  `available_amount`,
  `frozen_amount`,
  (`available_amount` + `frozen_amount`) AS `total_amount`,
  `account_status`,
  `version`,
  `updated_at`
FROM `acct_user_account`
WHERE `user_id` = 300001
  AND `account_type` = 1
  AND `is_deleted` = 0;
```

### 充值入账

下面的 SQL 用于账户充值入账。账户余额更新和流水写入必须放在同一个事务中。

```sql
START TRANSACTION;

SELECT
  `id`,
  `account_no`,
  `user_id`,
  `account_type`,
  `available_amount`,
  `frozen_amount`
FROM `acct_user_account`
WHERE `id` = 200001
  AND `account_status` = 1
  AND `is_deleted` = 0
FOR UPDATE;

UPDATE `acct_user_account`
SET
  `available_amount` = `available_amount` + 100.00,
  `total_income_amount` = `total_income_amount` + 100.00,
  `version` = `version` + 1,
  `updated_by` = 300001
WHERE `id` = 200001
  AND `account_status` = 1
  AND `is_deleted` = 0;

INSERT INTO `acct_account_flow` (
  `id`,
  `flow_no`,
  `account_id`,
  `account_no`,
  `user_id`,
  `account_type`,
  `biz_type`,
  `biz_no`,
  `change_direction`,
  `change_amount`,
  `before_available_amount`,
  `after_available_amount`,
  `before_frozen_amount`,
  `after_frozen_amount`,
  `flow_status`,
  `remark`
) VALUES (
  210001,
  'FLOW_202605110001',
  200001,
  'ACCT_202605110001',
  300001,
  1,
  'recharge',
  'RECHARGE_202605110001',
  1,
  100.00,
  0.00,
  100.00,
  0.00,
  0.00,
  1,
  '用户充值入账'
);

COMMIT;
```

### 消费扣款

下面的 SQL 用于账户消费扣款。扣款时必须判断余额是否充足，避免扣成负数。

```sql
START TRANSACTION;

SELECT
  `id`,
  `account_no`,
  `user_id`,
  `account_type`,
  `available_amount`,
  `frozen_amount`
FROM `acct_user_account`
WHERE `id` = 200001
  AND `account_status` = 1
  AND `is_deleted` = 0
FOR UPDATE;

UPDATE `acct_user_account`
SET
  `available_amount` = `available_amount` - 30.00,
  `total_expense_amount` = `total_expense_amount` + 30.00,
  `version` = `version` + 1,
  `updated_by` = 300001
WHERE `id` = 200001
  AND `available_amount` >= 30.00
  AND `account_status` = 1
  AND `is_deleted` = 0;

INSERT INTO `acct_account_flow` (
  `id`,
  `flow_no`,
  `account_id`,
  `account_no`,
  `user_id`,
  `account_type`,
  `biz_type`,
  `biz_no`,
  `change_direction`,
  `change_amount`,
  `before_available_amount`,
  `after_available_amount`,
  `before_frozen_amount`,
  `after_frozen_amount`,
  `flow_status`,
  `remark`
) VALUES (
  210002,
  'FLOW_202605110002',
  200001,
  'ACCT_202605110001',
  300001,
  1,
  'consume',
  'ORDER_202605110001',
  2,
  30.00,
  100.00,
  70.00,
  0.00,
  0.00,
  1,
  '订单消费扣款'
);

COMMIT;
```

### 冻结余额

下面的 SQL 用于冻结账户余额。冻结后可用余额减少，冻结金额增加，总金额不变。

```sql
START TRANSACTION;

SELECT
  `id`,
  `account_no`,
  `available_amount`,
  `frozen_amount`
FROM `acct_user_account`
WHERE `id` = 200001
  AND `account_status` = 1
  AND `is_deleted` = 0
FOR UPDATE;

UPDATE `acct_user_account`
SET
  `available_amount` = `available_amount` - 50.00,
  `frozen_amount` = `frozen_amount` + 50.00,
  `version` = `version` + 1,
  `updated_by` = 300001
WHERE `id` = 200001
  AND `available_amount` >= 50.00
  AND `account_status` = 1
  AND `is_deleted` = 0;

INSERT INTO `acct_account_flow` (
  `id`,
  `flow_no`,
  `account_id`,
  `account_no`,
  `user_id`,
  `account_type`,
  `biz_type`,
  `biz_no`,
  `change_direction`,
  `change_amount`,
  `before_available_amount`,
  `after_available_amount`,
  `before_frozen_amount`,
  `after_frozen_amount`,
  `flow_status`,
  `remark`
) VALUES (
  210003,
  'FLOW_202605110003',
  200001,
  'ACCT_202605110001',
  300001,
  1,
  'freeze',
  'FREEZE_202605110001',
  3,
  50.00,
  70.00,
  20.00,
  0.00,
  50.00,
  1,
  '冻结账户余额'
);

COMMIT;
```

### 解冻余额

下面的 SQL 用于解冻账户余额。解冻后可用余额增加，冻结金额减少，总金额不变。

```sql
START TRANSACTION;

SELECT
  `id`,
  `account_no`,
  `available_amount`,
  `frozen_amount`
FROM `acct_user_account`
WHERE `id` = 200001
  AND `account_status` = 1
  AND `is_deleted` = 0
FOR UPDATE;

UPDATE `acct_user_account`
SET
  `available_amount` = `available_amount` + 50.00,
  `frozen_amount` = `frozen_amount` - 50.00,
  `version` = `version` + 1,
  `updated_by` = 300001
WHERE `id` = 200001
  AND `frozen_amount` >= 50.00
  AND `account_status` = 1
  AND `is_deleted` = 0;

INSERT INTO `acct_account_flow` (
  `id`,
  `flow_no`,
  `account_id`,
  `account_no`,
  `user_id`,
  `account_type`,
  `biz_type`,
  `biz_no`,
  `change_direction`,
  `change_amount`,
  `before_available_amount`,
  `after_available_amount`,
  `before_frozen_amount`,
  `after_frozen_amount`,
  `flow_status`,
  `remark`
) VALUES (
  210004,
  'FLOW_202605110004',
  200001,
  'ACCT_202605110001',
  300001,
  1,
  'unfreeze',
  'UNFREEZE_202605110001',
  4,
  50.00,
  20.00,
  70.00,
  50.00,
  0.00,
  1,
  '解冻账户余额'
);

COMMIT;
```

### 退款入账

下面的 SQL 用于订单退款后将金额退回账户。

```sql
START TRANSACTION;

SELECT
  `id`,
  `account_no`,
  `available_amount`,
  `frozen_amount`
FROM `acct_user_account`
WHERE `id` = 200001
  AND `account_status` = 1
  AND `is_deleted` = 0
FOR UPDATE;

UPDATE `acct_user_account`
SET
  `available_amount` = `available_amount` + 30.00,
  `total_income_amount` = `total_income_amount` + 30.00,
  `version` = `version` + 1,
  `updated_by` = 300001
WHERE `id` = 200001
  AND `account_status` = 1
  AND `is_deleted` = 0;

INSERT INTO `acct_account_flow` (
  `id`,
  `flow_no`,
  `account_id`,
  `account_no`,
  `user_id`,
  `account_type`,
  `biz_type`,
  `biz_no`,
  `change_direction`,
  `change_amount`,
  `before_available_amount`,
  `after_available_amount`,
  `before_frozen_amount`,
  `after_frozen_amount`,
  `flow_status`,
  `remark`
) VALUES (
  210005,
  'FLOW_202605110005',
  200001,
  'ACCT_202605110001',
  300001,
  1,
  'refund',
  'REFUND_202605110001',
  1,
  30.00,
  70.00,
  100.00,
  0.00,
  0.00,
  1,
  '订单退款入账'
);

COMMIT;
```

### 查询账户流水列表

下面的 SQL 用于查询账户流水，适合用户账单页和后台账户明细页。

```sql
SELECT
  `id`,
  `flow_no`,
  `account_no`,
  `biz_type`,
  `biz_no`,
  `change_direction`,
  `change_amount`,
  `before_available_amount`,
  `after_available_amount`,
  `before_frozen_amount`,
  `after_frozen_amount`,
  `flow_status`,
  `operate_time`,
  `remark`
FROM `acct_account_flow`
WHERE `account_id` = 200001
ORDER BY `operate_time` DESC
LIMIT 20 OFFSET 0;
```

### 根据业务单号查询流水

下面的 SQL 用于根据业务类型和业务单号查询流水，适合幂等校验、问题排查和对账。

```sql
SELECT
  `id`,
  `flow_no`,
  `account_id`,
  `account_no`,
  `biz_type`,
  `biz_no`,
  `change_direction`,
  `change_amount`,
  `flow_status`,
  `operate_time`
FROM `acct_account_flow`
WHERE `biz_type` = 'consume'
  AND `biz_no` = 'ORDER_202605110001';
```

### 查询用户某段时间流水

下面的 SQL 用于查询用户在某段时间内的账户流水。

```sql
SELECT
  `flow_no`,
  `biz_type`,
  `biz_no`,
  `change_direction`,
  `change_amount`,
  `after_available_amount`,
  `after_frozen_amount`,
  `operate_time`,
  `remark`
FROM `acct_account_flow`
WHERE `user_id` = 300001
  AND `account_type` = 1
  AND `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
ORDER BY `operate_time` DESC;
```

### 统计账户收入支出

下面的 SQL 用于统计账户收入和支出金额，适合账单汇总。

```sql
SELECT
  `user_id`,
  `account_type`,
  SUM(CASE WHEN `change_direction` = 1 THEN `change_amount` ELSE 0 END) AS `income_amount`,
  SUM(CASE WHEN `change_direction` = 2 THEN `change_amount` ELSE 0 END) AS `expense_amount`,
  SUM(CASE WHEN `change_direction` = 3 THEN `change_amount` ELSE 0 END) AS `freeze_amount`,
  SUM(CASE WHEN `change_direction` = 4 THEN `change_amount` ELSE 0 END) AS `unfreeze_amount`
FROM `acct_account_flow`
WHERE `user_id` = 300001
  AND `account_type` = 1
  AND `flow_status` = 1
GROUP BY
  `user_id`,
  `account_type`;
```

### 校验账户余额和流水余额

下面的 SQL 用于查询账户最新一条流水的变动后余额，并和账户表当前余额进行对比。

```sql
SELECT
  a.`id`,
  a.`account_no`,
  a.`available_amount`,
  a.`frozen_amount`,
  f.`after_available_amount`,
  f.`after_frozen_amount`,
  f.`operate_time`
FROM `acct_user_account` a
JOIN (
  SELECT
    x.`account_id`,
    x.`after_available_amount`,
    x.`after_frozen_amount`,
    x.`operate_time`
  FROM `acct_account_flow` x
  JOIN (
    SELECT
      `account_id`,
      MAX(`operate_time`) AS `max_operate_time`
    FROM `acct_account_flow`
    GROUP BY `account_id`
  ) y
    ON x.`account_id` = y.`account_id`
    AND x.`operate_time` = y.`max_operate_time`
) f ON a.`id` = f.`account_id`
WHERE a.`is_deleted` = 0
  AND (
    a.`available_amount` <> f.`after_available_amount`
    OR a.`frozen_amount` <> f.`after_frozen_amount`
  );
```

### 冻结账户

下面的 SQL 用于冻结账户。账户冻结后不允许继续消费、冻结或提现。

```sql
UPDATE `acct_user_account`
SET
  `account_status` = 0,
  `updated_by` = 1
WHERE `id` = 200001
  AND `account_status` = 1
  AND `is_deleted` = 0;
```

### 解冻账户

下面的 SQL 用于解冻账户。

```sql
UPDATE `acct_user_account`
SET
  `account_status` = 1,
  `updated_by` = 1
WHERE `id` = 200001
  AND `account_status` = 0
  AND `is_deleted` = 0;
```

### 逻辑删除账户

下面的 SQL 用于逻辑删除账户。实际业务中，账户存在余额或流水时通常不允许删除。

```sql
UPDATE `acct_user_account`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 200001
  AND `available_amount` = 0.00
  AND `frozen_amount` = 0.00
  AND `is_deleted` = 0;
```

### 索引设计建议

账户流水模型的索引重点是账户编号、用户账户唯一性、业务单号幂等、账户流水时间查询。

| 表                  | 索引                                                       | 适用场景               |
| ------------------- | ---------------------------------------------------------- | ---------------------- |
| `acct_user_account` | `PRIMARY KEY (id)`                                         | 查询账户详情           |
| `acct_user_account` | `uk_account_no (account_no)`                               | 根据账户编号查询       |
| `acct_user_account` | `uk_user_type_deleted (user_id, account_type, is_deleted)` | 保证用户同类型账户唯一 |
| `acct_user_account` | `idx_status_deleted (account_status, is_deleted)`          | 查询正常或冻结账户     |
| `acct_account_flow` | `uk_flow_no (flow_no)`                                     | 根据流水编号查询       |
| `acct_account_flow` | `uk_biz_type_no (biz_type, biz_no)`                        | 业务幂等控制           |
| `acct_account_flow` | `idx_account_time (account_id, operate_time)`              | 查询账户流水           |
| `acct_account_flow` | `idx_user_type_time (user_id, account_type, operate_time)` | 查询用户账单           |
| `acct_account_flow` | `idx_biz_no (biz_no)`                                      | 根据业务单号排查流水   |

### 使用建议

账户流水模型的核心原则是账户余额可变，账户流水不可变。所有余额变化都必须产生流水，且余额更新和流水写入必须在同一个事务中完成。

使用账户流水模型时重点注意：

- 账户表保存当前余额，流水表保存每次变化过程
- 流水表原则上只新增，不修改，不删除
- 每次余额变化必须记录变动前余额和变动后余额
- 扣款时必须判断余额充足，避免余额为负
- 冻结和解冻不改变总金额，只改变可用金额和冻结金额
- 业务单号必须唯一，防止重复入账或重复扣款
- 余额更新和流水写入必须放在同一个事务中
- 高并发余额变动建议使用行锁或乐观锁控制
- 账户流水适合按时间归档，但不建议物理删除
- 对账时应以流水表作为重要依据，结合账户表当前余额校验一致性

## 标签模型

标签模型用于给业务对象打上一个或多个标识，便于搜索、筛选、分组、推荐和运营管理。标签通常不直接改变业务对象本身的数据结构，而是通过标签表和标签关系表进行扩展。

下面以“标签表”和“标签关系表”为例说明标签模型的建表方式和常用 SQL 实践。

### 适用场景

标签模型适合一个业务对象可以拥有多个标签，并且标签可以被多个业务对象复用的场景。标签通常比分类更灵活，不强调严格层级关系。

常见场景包括：

- 商品标签
- 文章标签
- 用户标签
- 客户标签
- 内容标签
- 活动标签
- 风控标签
- 会员标签
- 资源标签
- 运营标签

### 建表 SQL

下面的 SQL 创建标签表和标签关系表。标签表保存标签定义，标签关系表保存标签和业务对象之间的绑定关系。

```sql
CREATE TABLE `biz_tag` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tag_code` VARCHAR(64) NOT NULL COMMENT '标签编码',
  `tag_name` VARCHAR(128) NOT NULL COMMENT '标签名称',
  `tag_type` VARCHAR(64) NOT NULL COMMENT '标签类型：product商品，article文章，user用户，customer客户',
  `tag_color` VARCHAR(32) NULL COMMENT '标签颜色',
  `tag_status` TINYINT NOT NULL DEFAULT 1 COMMENT '标签状态：0停用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_code` (`tag_code`),
  UNIQUE KEY `uk_type_name_deleted` (`tag_type`, `tag_name`, `is_deleted`),
  KEY `idx_type_status_deleted` (`tag_type`, `tag_status`, `is_deleted`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='业务标签表';

CREATE TABLE `biz_tag_relation` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tag_id` BIGINT NOT NULL COMMENT '标签ID',
  `tag_code` VARCHAR(64) NOT NULL COMMENT '标签编码，冗余字段',
  `tag_type` VARCHAR(64) NOT NULL COMMENT '标签类型：product商品，article文章，user用户，customer客户',
  `biz_id` BIGINT NOT NULL COMMENT '业务对象ID',
  `biz_no` VARCHAR(64) NULL COMMENT '业务对象编号',
  `relation_status` TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态：0停用，1启用',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_biz_tag_deleted` (`tag_type`, `biz_id`, `tag_id`, `is_deleted`),
  KEY `idx_biz_type_status` (`tag_type`, `biz_id`, `relation_status`, `is_deleted`),
  KEY `idx_tag_type_status` (`tag_id`, `tag_type`, `relation_status`, `is_deleted`),
  KEY `idx_biz_no` (`biz_no`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='业务标签关系表';
```

### 字段设计说明

标签模型的核心是标签定义和标签绑定关系。标签表负责维护标签本身，关系表负责维护标签和业务对象的多对多关系。

| 表                 | 字段              | 说明                         |
| ------------------ | ----------------- | ---------------------------- |
| `biz_tag`          | `tag_code`        | 标签编码，业务唯一标识       |
| `biz_tag`          | `tag_name`        | 标签名称，用于展示           |
| `biz_tag`          | `tag_type`        | 标签类型，用于区分不同业务域 |
| `biz_tag`          | `tag_color`       | 标签颜色，适合前端展示       |
| `biz_tag_relation` | `tag_id`          | 标签 ID                      |
| `biz_tag_relation` | `tag_type`        | 标签类型                     |
| `biz_tag_relation` | `biz_id`          | 业务对象 ID                  |
| `biz_tag_relation` | `biz_no`          | 业务对象编号，便于排查       |
| `biz_tag_relation` | `relation_status` | 标签绑定关系状态             |

### 新增标签

下面的 SQL 用于新增商品标签。

```sql
INSERT INTO `biz_tag` (
  `id`,
  `tag_code`,
  `tag_name`,
  `tag_type`,
  `tag_color`,
  `tag_status`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  100001,
  'TAG_PRODUCT_HOT',
  '热销',
  'product',
  '#F56C6C',
  1,
  100,
  '热销商品标签',
  1,
  1
),
(
  100002,
  'TAG_PRODUCT_NEW',
  '新品',
  'product',
  '#67C23A',
  1,
  90,
  '新品商品标签',
  1,
  1
);
```

### 查询标签列表

下面的 SQL 用于查询某一类启用标签，适合后台标签管理和前端筛选条件展示。

```sql
SELECT
  `id`,
  `tag_code`,
  `tag_name`,
  `tag_type`,
  `tag_color`,
  `tag_status`,
  `sort_order`,
  `remark`
FROM `biz_tag`
WHERE `tag_type` = 'product'
  AND `tag_status` = 1
  AND `is_deleted` = 0
ORDER BY `sort_order` DESC, `id` ASC;
```

### 修改标签

下面的 SQL 用于修改标签名称、颜色和排序。

```sql
UPDATE `biz_tag`
SET
  `tag_name` = '爆款',
  `tag_color` = '#E6A23C',
  `sort_order` = 110,
  `remark` = '爆款商品标签',
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 停用标签

下面的 SQL 用于停用标签。停用标签后，业务对象已绑定的标签关系可以保留，但前台通常不再展示或不再允许新增绑定。

```sql
UPDATE `biz_tag`
SET
  `tag_status` = 0,
  `updated_by` = 1
WHERE `id` = 100001
  AND `tag_status` = 1
  AND `is_deleted` = 0;
```

### 绑定标签

下面的 SQL 用于给商品绑定标签。标签关系表通过唯一索引避免同一个业务对象重复绑定同一个标签。

```sql
INSERT INTO `biz_tag_relation` (
  `id`,
  `tag_id`,
  `tag_code`,
  `tag_type`,
  `biz_id`,
  `biz_no`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  110001,
  100001,
  'TAG_PRODUCT_HOT',
  'product',
  200001,
  'SPU_202605110001',
  1,
  '商品绑定热销标签',
  1,
  1
),
(
  110002,
  100002,
  'TAG_PRODUCT_NEW',
  'product',
  200001,
  'SPU_202605110001',
  1,
  '商品绑定新品标签',
  1,
  1
);
```

### 批量绑定标签

下面的 SQL 用于给一个业务对象批量绑定多个标签，适合后台编辑商品标签、用户标签等场景。

```sql
INSERT INTO `biz_tag_relation` (
  `id`,
  `tag_id`,
  `tag_code`,
  `tag_type`,
  `biz_id`,
  `biz_no`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  110003,
  100003,
  'TAG_PRODUCT_DISCOUNT',
  'product',
  200001,
  'SPU_202605110001',
  1,
  '批量绑定标签',
  1,
  1
),
(
  110004,
  100004,
  'TAG_PRODUCT_RECOMMEND',
  'product',
  200001,
  'SPU_202605110001',
  1,
  '批量绑定标签',
  1,
  1
);
```

### 查询业务对象标签

下面的 SQL 用于查询某个商品已绑定的标签。

```sql
SELECT
  t.`id`,
  t.`tag_code`,
  t.`tag_name`,
  t.`tag_type`,
  t.`tag_color`,
  r.`created_at` AS `bind_time`
FROM `biz_tag_relation` r
JOIN `biz_tag` t
  ON r.`tag_id` = t.`id`
  AND t.`is_deleted` = 0
WHERE r.`tag_type` = 'product'
  AND r.`biz_id` = 200001
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
  AND t.`tag_status` = 1
ORDER BY t.`sort_order` DESC, t.`id` ASC;
```

### 查询标签下的业务对象

下面的 SQL 用于查询绑定了某个标签的业务对象 ID，适合根据标签筛选商品、文章、用户等数据。

```sql
SELECT
  r.`biz_id`,
  r.`biz_no`,
  r.`created_at` AS `bind_time`
FROM `biz_tag_relation` r
WHERE r.`tag_type` = 'product'
  AND r.`tag_id` = 100001
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
ORDER BY r.`created_at` DESC
LIMIT 10 OFFSET 0;
```

### 查询商品并回显标签

下面的 SQL 演示商品表和标签关系表关联查询，用于商品列表中展示标签名称。

```sql
SELECT
  p.`id`,
  p.`spu_code`,
  p.`product_name`,
  GROUP_CONCAT(t.`tag_name` ORDER BY t.`sort_order` DESC SEPARATOR ',') AS `tag_names`
FROM `mall_product_spu` p
LEFT JOIN `biz_tag_relation` r
  ON p.`id` = r.`biz_id`
  AND r.`tag_type` = 'product'
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
LEFT JOIN `biz_tag` t
  ON r.`tag_id` = t.`id`
  AND t.`tag_status` = 1
  AND t.`is_deleted` = 0
WHERE p.`is_deleted` = 0
GROUP BY
  p.`id`,
  p.`spu_code`,
  p.`product_name`
ORDER BY p.`created_at` DESC
LIMIT 10 OFFSET 0;
```

### 根据多个标签筛选业务对象

下面的 SQL 用于查询同时拥有多个指定标签的业务对象。

```sql
SELECT
  r.`biz_id`,
  r.`biz_no`
FROM `biz_tag_relation` r
WHERE r.`tag_type` = 'product'
  AND r.`tag_id` IN (100001, 100002)
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
GROUP BY
  r.`biz_id`,
  r.`biz_no`
HAVING COUNT(DISTINCT r.`tag_id`) = 2;
```

### 解绑标签

下面的 SQL 用于解除业务对象和标签之间的关系。标签关系通常使用逻辑删除，便于审计和恢复。

```sql
UPDATE `biz_tag_relation`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `tag_type` = 'product'
  AND `biz_id` = 200001
  AND `tag_id` = 100001
  AND `is_deleted` = 0;
```

### 重新绑定标签

下面的 SQL 用于重新设置某个业务对象的标签。常见做法是先逻辑删除原标签关系，再插入新的标签关系，两个步骤应放在同一个事务中执行。

```sql
START TRANSACTION;

UPDATE `biz_tag_relation`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `tag_type` = 'product'
  AND `biz_id` = 200001
  AND `is_deleted` = 0;

INSERT INTO `biz_tag_relation` (
  `id`,
  `tag_id`,
  `tag_code`,
  `tag_type`,
  `biz_id`,
  `biz_no`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  110005,
  100002,
  'TAG_PRODUCT_NEW',
  'product',
  200001,
  'SPU_202605110001',
  1,
  '重新绑定标签',
  1,
  1
),
(
  110006,
  100004,
  'TAG_PRODUCT_RECOMMEND',
  'product',
  200001,
  'SPU_202605110001',
  1,
  '重新绑定标签',
  1,
  1
);

COMMIT;
```

### 统计标签使用次数

下面的 SQL 用于统计每个标签被绑定的业务对象数量，适合标签热度分析和标签治理。

```sql
SELECT
  t.`id`,
  t.`tag_code`,
  t.`tag_name`,
  COUNT(r.`biz_id`) AS `use_count`
FROM `biz_tag` t
LEFT JOIN `biz_tag_relation` r
  ON t.`id` = r.`tag_id`
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
WHERE t.`tag_type` = 'product'
  AND t.`is_deleted` = 0
GROUP BY
  t.`id`,
  t.`tag_code`,
  t.`tag_name`
ORDER BY `use_count` DESC, t.`sort_order` DESC;
```

### 查询未被使用的标签

下面的 SQL 用于查询没有被任何有效业务对象绑定的标签，适合标签清理。

```sql
SELECT
  t.`id`,
  t.`tag_code`,
  t.`tag_name`,
  t.`tag_type`
FROM `biz_tag` t
LEFT JOIN `biz_tag_relation` r
  ON t.`id` = r.`tag_id`
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
WHERE t.`is_deleted` = 0
  AND r.`id` IS NULL;
```

### 逻辑删除标签

下面的 SQL 用于逻辑删除标签。删除标签前通常应确认没有有效绑定关系。

```sql
UPDATE `biz_tag`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `biz_tag_relation` r
    WHERE r.`tag_id` = 100001
      AND r.`relation_status` = 1
      AND r.`is_deleted` = 0
  );
```

### 逻辑删除标签关系

下面的 SQL 用于批量删除某个业务对象下的所有标签关系。

```sql
UPDATE `biz_tag_relation`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `tag_type` = 'product'
  AND `biz_id` = 200001
  AND `is_deleted` = 0;
```

### 索引设计建议

标签模型的索引重点是标签类型、标签状态、业务对象 ID、标签 ID 和唯一绑定关系。

| 表                 | 索引                                                         | 适用场景                 |
| ------------------ | ------------------------------------------------------------ | ------------------------ |
| `biz_tag`          | `PRIMARY KEY (id)`                                           | 查询标签详情             |
| `biz_tag`          | `uk_tag_code (tag_code)`                                     | 根据标签编码查询         |
| `biz_tag`          | `uk_type_name_deleted (tag_type, tag_name, is_deleted)`      | 防止同类型标签名称重复   |
| `biz_tag`          | `idx_type_status_deleted (tag_type, tag_status, is_deleted)` | 查询某类启用标签         |
| `biz_tag_relation` | `uk_type_biz_tag_deleted (tag_type, biz_id, tag_id, is_deleted)` | 防止重复绑定标签         |
| `biz_tag_relation` | `idx_biz_type_status (tag_type, biz_id, relation_status, is_deleted)` | 查询业务对象标签         |
| `biz_tag_relation` | `idx_tag_type_status (tag_id, tag_type, relation_status, is_deleted)` | 查询标签下业务对象       |
| `biz_tag_relation` | `idx_biz_no (biz_no)`                                        | 根据业务编号排查标签关系 |

### 使用建议

标签模型适合灵活扩展业务对象的标识能力。它更强调灵活筛选和运营管理，不适合表达严格的层级归属关系。如果业务需要上下级结构，应优先使用分类模型或树形层级模型。

使用标签模型时重点注意：

- 标签适合多选，分类通常适合归属
- 标签和业务对象通常是多对多关系
- 不要在业务表中使用逗号字符串保存多个标签 ID
- 标签关系表需要防止重复绑定
- 标签名称可以调整，但标签编码应保持稳定
- 高频查询标签时可以适当缓存标签列表
- 大批量标签筛选时要关注关系表数据量和索引设计
- 删除标签前应检查是否存在有效绑定关系
- 标签适合做运营标识，不适合承载复杂业务规则

## 分类模型

分类模型用于对业务对象进行结构化归类。分类通常具有明确的业务边界和层级关系，一个业务对象可以归属于一个主分类，也可以根据业务需要支持多个分类。相比标签模型，分类更强调“归属”和“层级”。

下面以“内容分类表”和“内容分类关系表”为例说明分类模型的建表方式和常用 SQL 实践。

### 适用场景

分类模型适合业务对象需要按照固定结构进行归类、展示、筛选和统计的场景。分类通常比标签稳定，层级关系也更明确。

常见场景包括：

- 商品分类
- 文章分类
- 内容栏目
- 知识库分类
- 文件分类
- 课程分类
- 工单分类
- 客户分类
- 资源分类
- 问题分类

### 建表 SQL

下面的 SQL 创建分类表和分类关系表。分类表用于维护分类层级，分类关系表用于维护业务对象和分类的绑定关系。

```sql
CREATE TABLE `biz_category_group` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级分类ID，0表示根节点',
  `ancestors` VARCHAR(500) NOT NULL DEFAULT '0' COMMENT '祖级ID路径，例如 0,100001,100002',
  `category_code` VARCHAR(64) NOT NULL COMMENT '分类编码',
  `category_name` VARCHAR(128) NOT NULL COMMENT '分类名称',
  `category_type` VARCHAR(64) NOT NULL COMMENT '分类类型：product商品，article文章，course课程，file文件',
  `category_level` INT NOT NULL DEFAULT 1 COMMENT '分类层级，从1开始',
  `category_status` TINYINT NOT NULL DEFAULT 1 COMMENT '分类状态：0停用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `is_leaf` TINYINT NOT NULL DEFAULT 1 COMMENT '是否叶子节点：0否，1是',
  `icon_url` VARCHAR(500) NULL COMMENT '分类图标地址',
  `description` VARCHAR(500) NULL COMMENT '分类描述',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  UNIQUE KEY `uk_type_parent_name_deleted` (`category_type`, `parent_id`, `category_name`, `is_deleted`),
  KEY `idx_parent_status_deleted` (`parent_id`, `category_status`, `is_deleted`),
  KEY `idx_type_parent_status` (`category_type`, `parent_id`, `category_status`, `is_deleted`),
  KEY `idx_type_level_status` (`category_type`, `category_level`, `category_status`, `is_deleted`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='业务分类表';

CREATE TABLE `biz_category_relation` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `category_code` VARCHAR(64) NOT NULL COMMENT '分类编码，冗余字段',
  `category_type` VARCHAR(64) NOT NULL COMMENT '分类类型：product商品，article文章，course课程，file文件',
  `biz_id` BIGINT NOT NULL COMMENT '业务对象ID',
  `biz_no` VARCHAR(64) NULL COMMENT '业务对象编号',
  `is_primary` TINYINT NOT NULL DEFAULT 1 COMMENT '是否主分类：0否，1是',
  `relation_status` TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态：0停用，1启用',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_biz_category_deleted` (`category_type`, `biz_id`, `category_id`, `is_deleted`),
  KEY `idx_biz_type_status` (`category_type`, `biz_id`, `relation_status`, `is_deleted`),
  KEY `idx_category_type_status` (`category_id`, `category_type`, `relation_status`, `is_deleted`),
  KEY `idx_primary_category` (`category_type`, `biz_id`, `is_primary`, `is_deleted`),
  KEY `idx_biz_no` (`biz_no`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='业务分类关系表';
```

### 字段设计说明

分类模型的核心是分类层级和业务对象归属关系。分类表维护分类结构，关系表维护业务对象和分类之间的绑定。如果一个业务对象只允许一个分类，也可以直接在业务表中保存 `category_id`；如果需要支持多个分类，建议使用分类关系表。

| 表                      | 字段             | 说明         |
| ----------------------- | ---------------- | ------------ |
| `biz_category_group`    | `parent_id`      | 父级分类 ID  |
| `biz_category_group`    | `ancestors`      | 祖级路径     |
| `biz_category_group`    | `category_code`  | 分类编码     |
| `biz_category_group`    | `category_name`  | 分类名称     |
| `biz_category_group`    | `category_type`  | 分类类型     |
| `biz_category_group`    | `category_level` | 分类层级     |
| `biz_category_group`    | `is_leaf`        | 是否叶子节点 |
| `biz_category_relation` | `category_id`    | 分类 ID      |
| `biz_category_relation` | `biz_id`         | 业务对象 ID  |
| `biz_category_relation` | `is_primary`     | 是否主分类   |

### 新增一级分类

下面的 SQL 用于新增一级分类。

```sql
INSERT INTO `biz_category_group` (
  `id`,
  `parent_id`,
  `ancestors`,
  `category_code`,
  `category_name`,
  `category_type`,
  `category_level`,
  `category_status`,
  `sort_order`,
  `is_leaf`,
  `description`,
  `created_by`,
  `updated_by`
) VALUES (
  200001,
  0,
  '0',
  'ARTICLE_TECH',
  '技术文章',
  'article',
  1,
  1,
  100,
  0,
  '技术类文章分类',
  1,
  1
);
```

### 新增二级分类

下面的 SQL 用于新增二级分类，并维护父级 ID、祖级路径和分类层级。

```sql
INSERT INTO `biz_category_group` (
  `id`,
  `parent_id`,
  `ancestors`,
  `category_code`,
  `category_name`,
  `category_type`,
  `category_level`,
  `category_status`,
  `sort_order`,
  `is_leaf`,
  `description`,
  `created_by`,
  `updated_by`
) VALUES (
  200002,
  200001,
  '0,200001',
  'ARTICLE_JAVA',
  'Java',
  'article',
  2,
  1,
  90,
  1,
  'Java 技术文章分类',
  1,
  1
);
```

### 更新父级叶子状态

下面的 SQL 用于新增子分类后，将父级分类更新为非叶子节点。

```sql
UPDATE `biz_category_group`
SET
  `is_leaf` = 0,
  `updated_by` = 1
WHERE `id` = 200001
  AND `is_deleted` = 0;
```

### 查询一级分类

下面的 SQL 用于查询某一类业务下的一级分类。

```sql
SELECT
  `id`,
  `parent_id`,
  `category_code`,
  `category_name`,
  `category_type`,
  `category_level`,
  `category_status`,
  `sort_order`,
  `is_leaf`
FROM `biz_category_group`
WHERE `category_type` = 'article'
  AND `parent_id` = 0
  AND `category_status` = 1
  AND `is_deleted` = 0
ORDER BY `sort_order` DESC, `id` ASC;
```

### 查询下级分类

下面的 SQL 用于查询某个分类下的直接子分类。

```sql
SELECT
  `id`,
  `parent_id`,
  `category_code`,
  `category_name`,
  `category_type`,
  `category_level`,
  `category_status`,
  `sort_order`,
  `is_leaf`
FROM `biz_category_group`
WHERE `category_type` = 'article'
  AND `parent_id` = 200001
  AND `category_status` = 1
  AND `is_deleted` = 0
ORDER BY `sort_order` DESC, `id` ASC;
```

### 递归查询分类树

下面的 SQL 使用 MySQL 8 递归 CTE 查询某个分类及其全部下级分类。

```sql
WITH RECURSIVE category_tree AS (
  SELECT
    `id`,
    `parent_id`,
    `category_code`,
    `category_name`,
    `category_type`,
    `category_level`,
    `sort_order`,
    `is_leaf`
  FROM `biz_category_group`
  WHERE `id` = 200001
    AND `is_deleted` = 0

  UNION ALL

  SELECT
    c.`id`,
    c.`parent_id`,
    c.`category_code`,
    c.`category_name`,
    c.`category_type`,
    c.`category_level`,
    c.`sort_order`,
    c.`is_leaf`
  FROM `biz_category_group` c
  JOIN category_tree t
    ON c.`parent_id` = t.`id`
  WHERE c.`is_deleted` = 0
)
SELECT
  `id`,
  `parent_id`,
  `category_code`,
  `category_name`,
  `category_type`,
  `category_level`,
  `sort_order`,
  `is_leaf`
FROM category_tree
ORDER BY `category_level` ASC, `sort_order` DESC, `id` ASC;
```

### 查询叶子分类

下面的 SQL 用于查询可绑定业务对象的末级分类。很多业务只允许业务对象绑定叶子分类。

```sql
SELECT
  `id`,
  `category_code`,
  `category_name`,
  `category_type`,
  `category_level`
FROM `biz_category_group`
WHERE `category_type` = 'article'
  AND `is_leaf` = 1
  AND `category_status` = 1
  AND `is_deleted` = 0
ORDER BY `category_level` ASC, `sort_order` DESC;
```

### 绑定业务对象分类

下面的 SQL 用于将文章绑定到分类。一个业务对象可以有一个主分类，也可以有多个辅助分类。

```sql
INSERT INTO `biz_category_relation` (
  `id`,
  `category_id`,
  `category_code`,
  `category_type`,
  `biz_id`,
  `biz_no`,
  `is_primary`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  210001,
  200002,
  'ARTICLE_JAVA',
  'article',
  300001,
  'ARTICLE_202605110001',
  1,
  1,
  '文章绑定主分类',
  1,
  1
);
```

### 查询业务对象分类

下面的 SQL 用于查询某个业务对象绑定的分类。

```sql
SELECT
  c.`id`,
  c.`category_code`,
  c.`category_name`,
  c.`category_type`,
  c.`category_level`,
  r.`is_primary`,
  r.`created_at` AS `bind_time`
FROM `biz_category_relation` r
JOIN `biz_category_group` c
  ON r.`category_id` = c.`id`
  AND c.`is_deleted` = 0
WHERE r.`category_type` = 'article'
  AND r.`biz_id` = 300001
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
  AND c.`category_status` = 1
ORDER BY r.`is_primary` DESC, c.`sort_order` DESC;
```

### 查询分类下的业务对象

下面的 SQL 用于查询某个分类下直接绑定的业务对象。

```sql
SELECT
  r.`biz_id`,
  r.`biz_no`,
  r.`is_primary`,
  r.`created_at` AS `bind_time`
FROM `biz_category_relation` r
WHERE r.`category_type` = 'article'
  AND r.`category_id` = 200002
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
ORDER BY r.`created_at` DESC
LIMIT 10 OFFSET 0;
```

### 查询分类及子分类下的业务对象

下面的 SQL 用于查询某个分类及其全部子分类下的业务对象，适合内容栏目页、商品分类页等场景。

```sql
WITH RECURSIVE category_tree AS (
  SELECT
    `id`
  FROM `biz_category_group`
  WHERE `id` = 200001
    AND `is_deleted` = 0

  UNION ALL

  SELECT
    c.`id`
  FROM `biz_category_group` c
  JOIN category_tree t
    ON c.`parent_id` = t.`id`
  WHERE c.`is_deleted` = 0
)
SELECT
  r.`biz_id`,
  r.`biz_no`,
  r.`category_id`,
  r.`is_primary`,
  r.`created_at` AS `bind_time`
FROM `biz_category_relation` r
JOIN category_tree t
  ON r.`category_id` = t.`id`
WHERE r.`category_type` = 'article'
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
ORDER BY r.`created_at` DESC
LIMIT 10 OFFSET 0;
```

### 修改分类信息

下面的 SQL 用于修改分类名称、状态、排序等基础信息。

```sql
UPDATE `biz_category_group`
SET
  `category_name` = 'Java 后端',
  `category_status` = 1,
  `sort_order` = 95,
  `description` = 'Java 后端技术文章分类',
  `updated_by` = 1
WHERE `id` = 200002
  AND `is_deleted` = 0;
```

### 移动分类节点

下面的 SQL 用于将分类移动到新的父级分类下。实际业务中，移动分类后需要同步更新该分类及其所有子分类的 `ancestors` 和 `category_level`。

```sql
UPDATE `biz_category_group`
SET
  `parent_id` = 200003,
  `ancestors` = '0,200003',
  `category_level` = 2,
  `updated_by` = 1
WHERE `id` = 200002
  AND `is_deleted` = 0;
```

### 重新绑定业务对象分类

下面的 SQL 用于重新设置业务对象分类。常见做法是先逻辑删除原分类关系，再插入新的分类关系，两个步骤应放在同一个事务中执行。

```sql
START TRANSACTION;

UPDATE `biz_category_relation`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `category_type` = 'article'
  AND `biz_id` = 300001
  AND `is_deleted` = 0;

INSERT INTO `biz_category_relation` (
  `id`,
  `category_id`,
  `category_code`,
  `category_type`,
  `biz_id`,
  `biz_no`,
  `is_primary`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  210002,
  200004,
  'ARTICLE_MYSQL',
  'article',
  300001,
  'ARTICLE_202605110001',
  1,
  1,
  '重新绑定文章分类',
  1,
  1
);

COMMIT;
```

### 解绑业务对象分类

下面的 SQL 用于解除业务对象和分类之间的关系。

```sql
UPDATE `biz_category_relation`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `category_type` = 'article'
  AND `biz_id` = 300001
  AND `category_id` = 200002
  AND `is_deleted` = 0;
```

### 停用分类及子分类

下面的 SQL 使用递归 CTE 查询分类及其全部子分类，并批量停用这些分类。

```sql
WITH RECURSIVE category_tree AS (
  SELECT
    `id`
  FROM `biz_category_group`
  WHERE `id` = 200001
    AND `is_deleted` = 0

  UNION ALL

  SELECT
    c.`id`
  FROM `biz_category_group` c
  JOIN category_tree t
    ON c.`parent_id` = t.`id`
  WHERE c.`is_deleted` = 0
)
UPDATE `biz_category_group`
SET
  `category_status` = 0,
  `updated_by` = 1
WHERE `id` IN (
  SELECT `id`
  FROM category_tree
);
```

### 统计分类下业务对象数量

下面的 SQL 用于统计每个分类下直接绑定的业务对象数量。

```sql
SELECT
  c.`id`,
  c.`category_code`,
  c.`category_name`,
  COUNT(r.`biz_id`) AS `biz_count`
FROM `biz_category_group` c
LEFT JOIN `biz_category_relation` r
  ON c.`id` = r.`category_id`
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
WHERE c.`category_type` = 'article'
  AND c.`is_deleted` = 0
GROUP BY
  c.`id`,
  c.`category_code`,
  c.`category_name`
ORDER BY c.`sort_order` DESC;
```

### 查询空分类

下面的 SQL 用于查询没有子分类、也没有绑定业务对象的分类，适合分类清理。

```sql
SELECT
  c.`id`,
  c.`category_code`,
  c.`category_name`
FROM `biz_category_group` c
LEFT JOIN `biz_category_group` child
  ON c.`id` = child.`parent_id`
  AND child.`is_deleted` = 0
LEFT JOIN `biz_category_relation` r
  ON c.`id` = r.`category_id`
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
WHERE c.`category_type` = 'article'
  AND c.`is_deleted` = 0
  AND child.`id` IS NULL
  AND r.`id` IS NULL;
```

### 逻辑删除分类

下面的 SQL 用于逻辑删除空分类。删除前应确认该分类没有子分类，也没有有效业务绑定关系。

```sql
UPDATE `biz_category_group`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 200002
  AND `is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `biz_category_group` c
    WHERE c.`parent_id` = 200002
      AND c.`is_deleted` = 0
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `biz_category_relation` r
    WHERE r.`category_id` = 200002
      AND r.`relation_status` = 1
      AND r.`is_deleted` = 0
  );
```

### 索引设计建议

分类模型的索引重点是分类类型、父级分类、分类层级、业务对象 ID 和分类 ID。

| 表                      | 索引                                                         | 适用场景                 |
| ----------------------- | ------------------------------------------------------------ | ------------------------ |
| `biz_category_group`    | `PRIMARY KEY (id)`                                           | 查询分类详情             |
| `biz_category_group`    | `uk_category_code (category_code)`                           | 根据分类编码查询         |
| `biz_category_group`    | `uk_type_parent_name_deleted (category_type, parent_id, category_name, is_deleted)` | 防止同级分类重名         |
| `biz_category_group`    | `idx_parent_status_deleted (parent_id, category_status, is_deleted)` | 查询下级分类             |
| `biz_category_group`    | `idx_type_parent_status (category_type, parent_id, category_status, is_deleted)` | 查询指定业务类型下级分类 |
| `biz_category_group`    | `idx_type_level_status (category_type, category_level, category_status, is_deleted)` | 查询指定层级分类         |
| `biz_category_relation` | `uk_type_biz_category_deleted (category_type, biz_id, category_id, is_deleted)` | 防止重复绑定分类         |
| `biz_category_relation` | `idx_biz_type_status (category_type, biz_id, relation_status, is_deleted)` | 查询业务对象分类         |
| `biz_category_relation` | `idx_category_type_status (category_id, category_type, relation_status, is_deleted)` | 查询分类下业务对象       |
| `biz_category_relation` | `idx_primary_category (category_type, biz_id, is_primary, is_deleted)` | 查询业务对象主分类       |

### 使用建议

分类模型适合表达稳定、明确、有层级的业务归属。分类和标签不要混用：分类用于结构化归属，标签用于灵活标识。一个对象如果只能属于一个分类，可以直接在业务表中保存 `category_id`；如果需要多个分类或主辅分类，应使用分类关系表。

使用分类模型时重点注意：

- 分类适合表达归属，标签适合表达标识
- 分类通常有层级，标签通常没有层级
- 分类编码应保持稳定，不建议频繁修改
- 同一父级下分类名称应避免重复
- 删除分类前必须检查子分类和业务绑定关系
- 移动分类节点时要同步更新子孙节点层级和路径
- 分类树数据可以查询平铺结果后由应用层组装
- 高频分类查询可以缓存分类树
- 业务对象只允许一个分类时，可以直接在业务表保存 `category_id`
- 业务对象支持多个分类时，应使用分类关系表

## 附件资源模型

附件资源模型用于统一管理系统中的文件、图片、视频、文档等资源。它通常由资源表和业务关联表组成：资源表保存文件本身的元数据，业务关联表保存文件和具体业务对象之间的绑定关系。

下面以“附件资源表”和“附件业务关系表”为例说明附件资源模型的建表方式和常用 SQL 实践。

### 适用场景

附件资源模型适合业务系统中需要上传、管理、预览、下载、绑定文件的场景。它可以避免每个业务表都重复设计文件字段，也便于统一管理文件存储、访问地址、文件大小、文件类型和业务引用关系。

常见场景包括：

- 用户头像
- 商品图片
- 合同附件
- 工单附件
- 订单凭证
- 报销单附件
- 审批附件
- 文章封面
- 富文本图片
- 导入导出文件

### 建表 SQL

下面的 SQL 创建附件资源表和附件业务关系表。附件资源表保存文件元数据，附件业务关系表保存文件和业务对象的绑定关系。

```sql
CREATE TABLE `sys_attachment` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `file_no` VARCHAR(64) NOT NULL COMMENT '文件编号',
  `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
  `file_ext` VARCHAR(32) NULL COMMENT '文件扩展名',
  `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
  `mime_type` VARCHAR(128) NULL COMMENT '文件MIME类型',
  `file_hash` VARCHAR(128) NULL COMMENT '文件哈希值，用于去重或校验',
  `storage_type` VARCHAR(32) NOT NULL DEFAULT 'local' COMMENT '存储类型：local本地，minio对象存储，oss阿里云OSS，cos腾讯云COS',
  `bucket_name` VARCHAR(128) NULL COMMENT '存储桶名称',
  `object_key` VARCHAR(500) NOT NULL COMMENT '对象存储Key或本地相对路径',
  `file_url` VARCHAR(1000) NULL COMMENT '文件访问地址',
  `resource_type` VARCHAR(32) NOT NULL DEFAULT 'file' COMMENT '资源类型：image图片，video视频，audio音频，document文档，file文件',
  `file_status` TINYINT NOT NULL DEFAULT 1 COMMENT '文件状态：0临时，1有效，2禁用',
  `upload_user_id` BIGINT NULL COMMENT '上传用户ID',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_no` (`file_no`),
  KEY `idx_file_hash` (`file_hash`),
  KEY `idx_resource_status_deleted` (`resource_type`, `file_status`, `is_deleted`),
  KEY `idx_upload_user_time` (`upload_user_id`, `created_at`),
  KEY `idx_storage_object` (`storage_type`, `object_key`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统附件资源表';

CREATE TABLE `sys_attachment_relation` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `attachment_id` BIGINT NOT NULL COMMENT '附件ID',
  `file_no` VARCHAR(64) NOT NULL COMMENT '文件编号，冗余字段',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型：product商品，order订单，contract合同，work_order工单',
  `biz_id` BIGINT NOT NULL COMMENT '业务对象ID',
  `biz_no` VARCHAR(64) NULL COMMENT '业务对象编号',
  `usage_scene` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '使用场景：cover封面，detail详情，attachment附件，avatar头像',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `relation_status` TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态：0禁用，1启用',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_attachment_scene_deleted` (`biz_type`, `biz_id`, `attachment_id`, `usage_scene`, `is_deleted`),
  KEY `idx_biz_scene_status` (`biz_type`, `biz_id`, `usage_scene`, `relation_status`, `is_deleted`),
  KEY `idx_attachment_status` (`attachment_id`, `relation_status`, `is_deleted`),
  KEY `idx_biz_no` (`biz_no`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统附件业务关系表';
```

### 字段设计说明

附件资源模型的核心是把“文件本身”和“文件被哪个业务使用”分开。一个文件可以被多个业务对象引用，一个业务对象也可以绑定多个附件。

| 表                        | 字段            | 说明                               |
| ------------------------- | --------------- | ---------------------------------- |
| `sys_attachment`          | `file_no`       | 文件编号，业务唯一标识             |
| `sys_attachment`          | `original_name` | 用户上传时的原始文件名             |
| `sys_attachment`          | `file_name`     | 实际存储文件名                     |
| `sys_attachment`          | `file_hash`     | 文件哈希值，可用于去重和完整性校验 |
| `sys_attachment`          | `storage_type`  | 文件存储方式                       |
| `sys_attachment`          | `object_key`    | 文件在存储系统中的唯一路径         |
| `sys_attachment`          | `file_url`      | 文件访问地址                       |
| `sys_attachment_relation` | `attachment_id` | 附件 ID                            |
| `sys_attachment_relation` | `biz_type`      | 业务类型                           |
| `sys_attachment_relation` | `biz_id`        | 业务对象 ID                        |
| `sys_attachment_relation` | `usage_scene`   | 附件使用场景                       |

### 新增附件资源

下面的 SQL 用于保存上传后的附件元数据。文件上传到本地、MinIO、OSS 等存储系统后，需要把文件信息落库。

```sql
INSERT INTO `sys_attachment` (
  `id`,
  `file_no`,
  `original_name`,
  `file_name`,
  `file_ext`,
  `file_size`,
  `mime_type`,
  `file_hash`,
  `storage_type`,
  `bucket_name`,
  `object_key`,
  `file_url`,
  `resource_type`,
  `file_status`,
  `upload_user_id`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  'FILE_202605110001',
  '合同附件.pdf',
  '20260511_100001.pdf',
  'pdf',
  204800,
  'application/pdf',
  'b1946ac92492d2347c6235b4d2611184',
  'minio',
  'business-file',
  'contract/2026/05/11/20260511_100001.pdf',
  'https://example.com/business-file/contract/2026/05/11/20260511_100001.pdf',
  'document',
  1,
  200001,
  '合同附件上传',
  200001,
  200001
);
```

### 绑定业务对象

下面的 SQL 用于把附件绑定到具体业务对象，例如合同、订单、工单等。

```sql
INSERT INTO `sys_attachment_relation` (
  `id`,
  `attachment_id`,
  `file_no`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `usage_scene`,
  `sort_order`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  110001,
  100001,
  'FILE_202605110001',
  'contract',
  300001,
  'CONTRACT_202605110001',
  'attachment',
  100,
  1,
  '合同正文附件',
  200001,
  200001
);
```

### 查询附件详情

下面的 SQL 用于根据附件 ID 查询文件详情，适合下载、预览、文件校验等场景。

```sql
SELECT
  `id`,
  `file_no`,
  `original_name`,
  `file_name`,
  `file_ext`,
  `file_size`,
  `mime_type`,
  `file_hash`,
  `storage_type`,
  `bucket_name`,
  `object_key`,
  `file_url`,
  `resource_type`,
  `file_status`,
  `upload_user_id`,
  `created_at`
FROM `sys_attachment`
WHERE `id` = 100001
  AND `file_status` = 1
  AND `is_deleted` = 0;
```

### 查询业务附件列表

下面的 SQL 用于查询某个业务对象绑定的附件列表，适合详情页展示附件。

```sql
SELECT
  a.`id`,
  a.`file_no`,
  a.`original_name`,
  a.`file_ext`,
  a.`file_size`,
  a.`mime_type`,
  a.`file_url`,
  a.`resource_type`,
  r.`usage_scene`,
  r.`sort_order`,
  r.`created_at` AS `bind_time`
FROM `sys_attachment_relation` r
JOIN `sys_attachment` a
  ON r.`attachment_id` = a.`id`
  AND a.`is_deleted` = 0
WHERE r.`biz_type` = 'contract'
  AND r.`biz_id` = 300001
  AND r.`usage_scene` = 'attachment'
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
  AND a.`file_status` = 1
ORDER BY r.`sort_order` DESC, r.`id` ASC;
```

### 查询业务封面附件

下面的 SQL 用于查询业务对象的封面图片，适合商品封面、文章封面、用户头像等场景。

```sql
SELECT
  a.`id`,
  a.`file_no`,
  a.`original_name`,
  a.`file_url`,
  a.`resource_type`
FROM `sys_attachment_relation` r
JOIN `sys_attachment` a
  ON r.`attachment_id` = a.`id`
  AND a.`is_deleted` = 0
WHERE r.`biz_type` = 'product'
  AND r.`biz_id` = 300001
  AND r.`usage_scene` = 'cover'
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
  AND a.`file_status` = 1
ORDER BY r.`sort_order` DESC
LIMIT 1;
```

### 根据文件哈希查询

下面的 SQL 用于根据文件哈希查询是否已存在相同文件，适合上传去重和秒传场景。

```sql
SELECT
  `id`,
  `file_no`,
  `original_name`,
  `file_size`,
  `file_hash`,
  `storage_type`,
  `object_key`,
  `file_url`
FROM `sys_attachment`
WHERE `file_hash` = 'b1946ac92492d2347c6235b4d2611184'
  AND `file_status` = 1
  AND `is_deleted` = 0
LIMIT 1;
```

### 根据上传人查询文件

下面的 SQL 用于查询某个用户上传的文件列表。

```sql
SELECT
  `id`,
  `file_no`,
  `original_name`,
  `file_ext`,
  `file_size`,
  `resource_type`,
  `file_status`,
  `created_at`
FROM `sys_attachment`
WHERE `upload_user_id` = 200001
  AND `is_deleted` = 0
ORDER BY `created_at` DESC
LIMIT 20 OFFSET 0;
```

### 修改附件状态

下面的 SQL 用于修改附件状态，例如把临时文件转为有效文件。

```sql
UPDATE `sys_attachment`
SET
  `file_status` = 1,
  `updated_by` = 200001
WHERE `id` = 100001
  AND `file_status` = 0
  AND `is_deleted` = 0;
```

### 修改附件排序

下面的 SQL 用于调整某个业务对象下附件的展示顺序。

```sql
UPDATE `sys_attachment_relation`
SET
  `sort_order` = 200,
  `updated_by` = 200001
WHERE `biz_type` = 'contract'
  AND `biz_id` = 300001
  AND `attachment_id` = 100001
  AND `is_deleted` = 0;
```

### 解绑业务附件

下面的 SQL 用于解除业务对象和附件之间的关系。通常只删除关系，不删除附件资源本身。

```sql
UPDATE `sys_attachment_relation`
SET
  `is_deleted` = 1,
  `updated_by` = 200001
WHERE `biz_type` = 'contract'
  AND `biz_id` = 300001
  AND `attachment_id` = 100001
  AND `is_deleted` = 0;
```

### 批量绑定附件

下面的 SQL 用于给一个业务对象批量绑定多个附件，适合合同附件、工单附件、审批附件等场景。

```sql
INSERT INTO `sys_attachment_relation` (
  `id`,
  `attachment_id`,
  `file_no`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `usage_scene`,
  `sort_order`,
  `relation_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  110002,
  100002,
  'FILE_202605110002',
  'contract',
  300001,
  'CONTRACT_202605110001',
  'attachment',
  90,
  1,
  '合同补充附件',
  200001,
  200001
),
(
  110003,
  100003,
  'FILE_202605110003',
  'contract',
  300001,
  'CONTRACT_202605110001',
  'attachment',
  80,
  1,
  '合同签署附件',
  200001,
  200001
);
```

### 统计业务附件数量

下面的 SQL 用于统计每个业务对象绑定的附件数量。

```sql
SELECT
  `biz_type`,
  `biz_id`,
  `biz_no`,
  COUNT(*) AS `attachment_count`
FROM `sys_attachment_relation`
WHERE `biz_type` = 'contract'
  AND `relation_status` = 1
  AND `is_deleted` = 0
GROUP BY
  `biz_type`,
  `biz_id`,
  `biz_no`;
```

### 查询未绑定附件

下面的 SQL 用于查询没有被任何业务对象引用的有效附件，适合清理孤立文件前的数据检查。

```sql
SELECT
  a.`id`,
  a.`file_no`,
  a.`original_name`,
  a.`file_size`,
  a.`storage_type`,
  a.`object_key`,
  a.`created_at`
FROM `sys_attachment` a
LEFT JOIN `sys_attachment_relation` r
  ON a.`id` = r.`attachment_id`
  AND r.`relation_status` = 1
  AND r.`is_deleted` = 0
WHERE a.`file_status` = 1
  AND a.`is_deleted` = 0
  AND r.`id` IS NULL;
```

### 逻辑删除附件资源

下面的 SQL 用于逻辑删除附件资源。删除前通常应确认该附件没有有效业务引用。

```sql
UPDATE `sys_attachment`
SET
  `is_deleted` = 1,
  `updated_by` = 200001
WHERE `id` = 100001
  AND `is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_attachment_relation` r
    WHERE r.`attachment_id` = 100001
      AND r.`relation_status` = 1
      AND r.`is_deleted` = 0
  );
```

### 逻辑删除附件关系

下面的 SQL 用于批量删除某个业务对象下的附件关系。

```sql
UPDATE `sys_attachment_relation`
SET
  `is_deleted` = 1,
  `updated_by` = 200001
WHERE `biz_type` = 'contract'
  AND `biz_id` = 300001
  AND `is_deleted` = 0;
```

### 索引设计建议

附件资源模型的索引重点是文件编号、文件哈希、业务对象关联字段和附件 ID。

| 表                        | 索引                                                         | 适用场景               |
| ------------------------- | ------------------------------------------------------------ | ---------------------- |
| `sys_attachment`          | `PRIMARY KEY (id)`                                           | 查询附件详情           |
| `sys_attachment`          | `uk_file_no (file_no)`                                       | 根据文件编号查询       |
| `sys_attachment`          | `idx_file_hash (file_hash)`                                  | 文件去重、秒传         |
| `sys_attachment`          | `idx_resource_status_deleted (resource_type, file_status, is_deleted)` | 查询有效资源           |
| `sys_attachment`          | `idx_upload_user_time (upload_user_id, created_at)`          | 查询用户上传文件       |
| `sys_attachment_relation` | `uk_biz_attachment_scene_deleted (biz_type, biz_id, attachment_id, usage_scene, is_deleted)` | 防止重复绑定           |
| `sys_attachment_relation` | `idx_biz_scene_status (biz_type, biz_id, usage_scene, relation_status, is_deleted)` | 查询业务附件           |
| `sys_attachment_relation` | `idx_attachment_status (attachment_id, relation_status, is_deleted)` | 查询附件被哪些业务引用 |

### 使用建议

附件资源模型的核心原则是文件元数据统一管理，业务引用单独维护。业务表中不建议直接存多个附件 URL，更不建议用逗号字符串保存多个文件地址。

使用附件资源模型时重点注意：

- 文件资源和业务关系应拆分成两张表
- 文件上传成功后先保存附件资源记录
- 业务提交成功后再绑定附件关系
- 业务删除时通常只删除附件关系，不直接删除物理文件
- 删除附件资源前要检查是否存在有效业务引用
- 文件哈希可以用于去重、秒传和完整性校验
- `file_url` 可以存访问地址，但真正定位文件应以 `storage_type` 和 `object_key` 为准
- 临时文件应有清理机制，避免上传后未绑定业务造成垃圾文件
- 图片、文档、视频等资源类型可以通过 `resource_type` 区分
- 大文件和敏感文件建议使用带过期时间的临时访问链接

## 配置项模型

配置项模型用于统一管理系统运行过程中的业务参数、开关、阈值、默认值和可配置规则。它可以让部分业务参数不需要发版即可调整，常用于后台管理系统、平台配置中心和业务参数管理。

下面以“配置分组表”和“配置项表”为例说明配置项模型的建表方式和常用 SQL 实践。

### 适用场景

配置项模型适合管理变化频率较低、需要后台维护、需要系统运行时读取的参数。它不适合承载复杂业务规则，也不适合替代正式的配置中心。

常见场景包括：

- 系统开关
- 登录失败次数限制
- 短信验证码有效期
- 文件上传大小限制
- 默认头像地址
- 订单自动关闭时间
- 积分兑换比例
- 会员默认权益
- 平台客服电话
- 业务阈值参数

### 建表 SQL

下面的 SQL 创建配置分组表和配置项表。配置分组表用于管理配置分类，配置项表用于保存具体配置键和值。

```sql
CREATE TABLE `sys_config_group` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `group_code` VARCHAR(64) NOT NULL COMMENT '配置分组编码',
  `group_name` VARCHAR(128) NOT NULL COMMENT '配置分组名称',
  `group_status` TINYINT NOT NULL DEFAULT 1 COMMENT '分组状态：0停用，1启用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_code` (`group_code`),
  KEY `idx_status_deleted` (`group_status`, `is_deleted`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统配置分组表';

CREATE TABLE `sys_config_item` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `group_id` BIGINT NOT NULL COMMENT '配置分组ID',
  `group_code` VARCHAR(64) NOT NULL COMMENT '配置分组编码，冗余字段',
  `config_key` VARCHAR(128) NOT NULL COMMENT '配置键',
  `config_name` VARCHAR(128) NOT NULL COMMENT '配置名称',
  `config_value` TEXT NOT NULL COMMENT '配置值',
  `config_type` VARCHAR(32) NOT NULL DEFAULT 'string' COMMENT '配置类型：string字符串，number数字，boolean布尔，json对象，array数组',
  `config_status` TINYINT NOT NULL DEFAULT 1 COMMENT '配置状态：0停用，1启用',
  `is_system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统内置：0否，1是',
  `is_encrypted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否加密存储：0否，1是',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_group_status_deleted` (`group_id`, `config_status`, `is_deleted`),
  KEY `idx_group_code_status` (`group_code`, `config_status`, `is_deleted`),
  KEY `idx_system_status` (`is_system`, `config_status`, `is_deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统配置项表';
```

### 字段设计说明

配置项模型的核心是配置键和值。配置键应稳定且唯一，配置值可以根据配置类型解析为字符串、数字、布尔值、JSON 对象或数组。

| 表                 | 字段           | 说明                   |
| ------------------ | -------------- | ---------------------- |
| `sys_config_group` | `group_code`   | 配置分组编码           |
| `sys_config_group` | `group_name`   | 配置分组名称           |
| `sys_config_item`  | `config_key`   | 配置键，全局唯一       |
| `sys_config_item`  | `config_name`  | 配置名称，用于后台展示 |
| `sys_config_item`  | `config_value` | 配置值                 |
| `sys_config_item`  | `config_type`  | 配置值类型             |
| `sys_config_item`  | `is_system`    | 是否系统内置配置       |
| `sys_config_item`  | `is_encrypted` | 是否加密存储           |

### 新增配置分组

下面的 SQL 用于新增系统配置分组。

```sql
INSERT INTO `sys_config_group` (
  `id`,
  `group_code`,
  `group_name`,
  `group_status`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  200001,
  'system_base',
  '系统基础配置',
  1,
  100,
  '系统基础参数配置',
  1,
  1
);
```

### 新增配置项

下面的 SQL 用于新增系统配置项。

```sql
INSERT INTO `sys_config_item` (
  `id`,
  `group_id`,
  `group_code`,
  `config_key`,
  `config_name`,
  `config_value`,
  `config_type`,
  `config_status`,
  `is_system`,
  `is_encrypted`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES
(
  210001,
  200001,
  'system_base',
  'system.site.name',
  '系统名称',
  '业务管理平台',
  'string',
  1,
  1,
  0,
  100,
  '系统页面展示名称',
  1,
  1
),
(
  210002,
  200001,
  'system_base',
  'login.max.fail.count',
  '登录最大失败次数',
  '5',
  'number',
  1,
  1,
  0,
  90,
  '超过失败次数后锁定账号',
  1,
  1
),
(
  210003,
  200001,
  'system_base',
  'file.upload.max.size.mb',
  '文件上传最大大小',
  '20',
  'number',
  1,
  1,
  0,
  80,
  '文件上传大小限制，单位MB',
  1,
  1
);
```

### 新增 JSON 配置项

下面的 SQL 用于新增 JSON 类型配置项，适合保存结构化配置。

```sql
INSERT INTO `sys_config_item` (
  `id`,
  `group_id`,
  `group_code`,
  `config_key`,
  `config_name`,
  `config_value`,
  `config_type`,
  `config_status`,
  `is_system`,
  `is_encrypted`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  210004,
  200001,
  'system_base',
  'system.contact.info',
  '系统联系信息',
  JSON_OBJECT('phone', '400-000-0000', 'email', 'support@example.com'),
  'json',
  1,
  0,
  0,
  70,
  '系统客服联系方式',
  1,
  1
);
```

### 查询配置分组列表

下面的 SQL 用于查询启用的配置分组。

```sql
SELECT
  `id`,
  `group_code`,
  `group_name`,
  `group_status`,
  `sort_order`,
  `remark`
FROM `sys_config_group`
WHERE `group_status` = 1
  AND `is_deleted` = 0
ORDER BY `sort_order` DESC, `id` ASC;
```

### 根据配置键查询配置值

下面的 SQL 用于根据配置键查询配置值，适合业务代码读取单个配置。

```sql
SELECT
  `config_key`,
  `config_name`,
  `config_value`,
  `config_type`
FROM `sys_config_item`
WHERE `config_key` = 'login.max.fail.count'
  AND `config_status` = 1
  AND `is_deleted` = 0;
```

### 查询分组下的配置项

下面的 SQL 用于查询某个分组下的所有启用配置项，适合后台配置页面或启动时批量加载。

```sql
SELECT
  `id`,
  `config_key`,
  `config_name`,
  `config_value`,
  `config_type`,
  `is_system`,
  `is_encrypted`,
  `sort_order`,
  `remark`
FROM `sys_config_item`
WHERE `group_code` = 'system_base'
  AND `config_status` = 1
  AND `is_deleted` = 0
ORDER BY `sort_order` DESC, `id` ASC;
```

### 查询多个配置项

下面的 SQL 用于一次性查询多个配置项，适合业务启动或接口初始化。

```sql
SELECT
  `config_key`,
  `config_value`,
  `config_type`
FROM `sys_config_item`
WHERE `config_key` IN (
    'system.site.name',
    'login.max.fail.count',
    'file.upload.max.size.mb'
  )
  AND `config_status` = 1
  AND `is_deleted` = 0;
```

### 修改配置值

下面的 SQL 用于修改普通配置项的值。

```sql
UPDATE `sys_config_item`
SET
  `config_value` = '10',
  `updated_by` = 1
WHERE `config_key` = 'login.max.fail.count'
  AND `config_status` = 1
  AND `is_deleted` = 0;
```

### 修改 JSON 配置值

下面的 SQL 用于修改 JSON 配置项中的某个字段。

```sql
UPDATE `sys_config_item`
SET
  `config_value` = JSON_SET(
    CAST(`config_value` AS JSON),
    '$.phone',
    '400-111-2222'
  ),
  `updated_by` = 1
WHERE `config_key` = 'system.contact.info'
  AND `config_type` = 'json'
  AND `config_status` = 1
  AND `is_deleted` = 0;
```

### 停用配置项

下面的 SQL 用于停用某个配置项。停用后业务代码读取配置时应走默认值或降级逻辑。

```sql
UPDATE `sys_config_item`
SET
  `config_status` = 0,
  `updated_by` = 1
WHERE `config_key` = 'file.upload.max.size.mb'
  AND `config_status` = 1
  AND `is_deleted` = 0;
```

### 查询系统内置配置

下面的 SQL 用于查询系统内置配置项，适合限制删除、限制编辑或初始化配置。

```sql
SELECT
  `id`,
  `config_key`,
  `config_name`,
  `config_value`,
  `config_type`,
  `config_status`
FROM `sys_config_item`
WHERE `is_system` = 1
  AND `is_deleted` = 0
ORDER BY `group_code` ASC, `sort_order` DESC;
```

### 查询加密配置项

下面的 SQL 用于查询需要加密存储或脱敏展示的配置项，例如密钥、Token、第三方接口凭证等。

```sql
SELECT
  `id`,
  `config_key`,
  `config_name`,
  `config_type`,
  `is_encrypted`,
  `config_status`
FROM `sys_config_item`
WHERE `is_encrypted` = 1
  AND `is_deleted` = 0;
```

### 查询重复配置键

下面的 SQL 用于检查配置键是否重复，适合数据治理和上线前检查。正常情况下，唯一索引会阻止重复配置键。

```sql
SELECT
  `config_key`,
  COUNT(*) AS `repeat_count`
FROM `sys_config_item`
WHERE `is_deleted` = 0
GROUP BY `config_key`
HAVING COUNT(*) > 1;
```

### 逻辑删除配置项

下面的 SQL 用于逻辑删除非系统内置配置项。系统内置配置通常不允许删除。

```sql
UPDATE `sys_config_item`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `config_key` = 'file.upload.max.size.mb'
  AND `is_system` = 0
  AND `is_deleted` = 0;
```

### 逻辑删除配置分组

下面的 SQL 用于逻辑删除空配置分组。删除前应确认分组下不存在有效配置项。

```sql
UPDATE `sys_config_group`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 200001
  AND `is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_config_item` c
    WHERE c.`group_id` = 200001
      AND c.`is_deleted` = 0
  );
```

### 配置项分页查询

下面的 SQL 用于后台配置项管理页面分页查询。

```sql
SELECT
  c.`id`,
  c.`group_code`,
  g.`group_name`,
  c.`config_key`,
  c.`config_name`,
  c.`config_value`,
  c.`config_type`,
  c.`config_status`,
  c.`is_system`,
  c.`is_encrypted`,
  c.`updated_at`
FROM `sys_config_item` c
LEFT JOIN `sys_config_group` g
  ON c.`group_id` = g.`id`
  AND g.`is_deleted` = 0
WHERE c.`is_deleted` = 0
  AND c.`group_code` = 'system_base'
  AND c.`config_status` = 1
ORDER BY c.`sort_order` DESC, c.`updated_at` DESC
LIMIT 10 OFFSET 0;
```

### 索引设计建议

配置项模型的索引重点是配置键、配置分组、配置状态和系统内置标识。

| 表                 | 索引                                                         | 适用场景             |
| ------------------ | ------------------------------------------------------------ | -------------------- |
| `sys_config_group` | `PRIMARY KEY (id)`                                           | 查询配置分组详情     |
| `sys_config_group` | `uk_group_code (group_code)`                                 | 根据分组编码查询     |
| `sys_config_group` | `idx_status_deleted (group_status, is_deleted)`              | 查询启用分组         |
| `sys_config_item`  | `PRIMARY KEY (id)`                                           | 查询配置详情         |
| `sys_config_item`  | `uk_config_key (config_key)`                                 | 根据配置键查询配置值 |
| `sys_config_item`  | `idx_group_status_deleted (group_id, config_status, is_deleted)` | 查询分组下配置       |
| `sys_config_item`  | `idx_group_code_status (group_code, config_status, is_deleted)` | 根据分组编码查询配置 |
| `sys_config_item`  | `idx_system_status (is_system, config_status, is_deleted)`   | 查询系统内置配置     |

### 使用建议

配置项模型适合管理简单、稳定、可后台维护的业务参数。对于高频读取配置，建议在应用启动时或首次读取时加载到缓存中，修改配置后再刷新缓存。

使用配置项模型时重点注意：

- 配置键必须稳定且全局唯一
- 配置值统一以字符串或文本存储，读取时根据 `config_type` 转换
- 系统内置配置不建议随意删除
- 敏感配置应加密存储，展示时脱敏
- 高频读取配置应使用缓存，避免每次请求都查数据库
- 修改配置后需要刷新本地缓存或分布式缓存
- 配置项适合简单参数，不适合复杂规则引擎
- 配置变更建议记录操作日志，便于追踪问题
- JSON 配置适合结构化低频参数，不建议承载复杂业务数据
- 关键业务配置应提供默认值，避免配置缺失导致系统异常

## 操作日志模型

操作日志模型用于记录用户或系统在应用中的操作行为，例如登录、查询、新增、修改、删除、导入、导出、审批、发布等。它关注的是“谁在什么时候做了什么操作”，常用于问题排查、行为追踪、安全分析和后台管理审计。

下面以“系统操作日志表”为例说明操作日志模型的建表方式和常用 SQL 实践。

### 适用场景

操作日志模型适合记录系统接口调用、后台管理操作、用户行为和关键业务操作。它通常不用于还原数据变更前后的内容，而是用于追踪操作行为本身。

常见场景包括：

- 用户登录日志
- 后台操作日志
- 接口访问日志
- 数据新增日志
- 数据修改日志
- 数据删除日志
- 文件上传日志
- 导入导出日志
- 审批操作日志
- 系统异常操作日志

### 建表 SQL

下面的 SQL 创建系统操作日志表，用于记录操作人、操作模块、请求信息、响应状态、耗时和异常信息。

```sql
CREATE TABLE `sys_operation_log` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `trace_id` VARCHAR(128) NULL COMMENT '链路追踪ID',
  `operator_id` BIGINT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(128) NULL COMMENT '操作人名称',
  `operation_module` VARCHAR(128) NOT NULL COMMENT '操作模块，例如用户管理、订单管理',
  `operation_type` VARCHAR(64) NOT NULL COMMENT '操作类型：login登录，query查询，create新增，update修改，delete删除，export导出，import导入',
  `operation_desc` VARCHAR(500) NULL COMMENT '操作描述',
  `request_method` VARCHAR(16) NULL COMMENT '请求方式：GET、POST、PUT、DELETE',
  `request_uri` VARCHAR(500) NULL COMMENT '请求地址',
  `request_params` JSON NULL COMMENT '请求参数JSON',
  `response_body` JSON NULL COMMENT '响应结果JSON，建议只保存摘要',
  `client_ip` VARCHAR(64) NULL COMMENT '客户端IP',
  `user_agent` VARCHAR(1000) NULL COMMENT '用户代理',
  `success_status` TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功：0失败，1成功',
  `error_message` TEXT NULL COMMENT '异常信息',
  `cost_millis` BIGINT NOT NULL DEFAULT 0 COMMENT '耗时，单位毫秒',
  `operate_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_operator_time` (`operator_id`, `operate_time`),
  KEY `idx_module_type_time` (`operation_module`, `operation_type`, `operate_time`),
  KEY `idx_status_time` (`success_status`, `operate_time`),
  KEY `idx_uri_time` (`request_uri`, `operate_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统操作日志表';
```

### 字段设计说明

操作日志表应重点记录操作主体、操作对象、请求上下文和执行结果。对于请求参数和响应结果，不建议保存完整敏感数据，应保存必要摘要并做好脱敏处理。

| 字段               | 说明                              |
| ------------------ | --------------------------------- |
| `trace_id`         | 链路追踪 ID，用于串联一次请求链路 |
| `operator_id`      | 操作人 ID                         |
| `operator_name`    | 操作人名称快照                    |
| `operation_module` | 操作模块                          |
| `operation_type`   | 操作类型                          |
| `operation_desc`   | 操作描述                          |
| `request_method`   | HTTP 请求方式                     |
| `request_uri`      | 请求路径                          |
| `request_params`   | 请求参数 JSON                     |
| `response_body`    | 响应结果 JSON 摘要                |
| `client_ip`        | 客户端 IP                         |
| `success_status`   | 是否操作成功                      |
| `error_message`    | 失败或异常信息                    |
| `cost_millis`      | 操作耗时                          |
| `operate_time`     | 操作时间                          |

### 新增成功操作日志

下面的 SQL 用于记录一次成功的新增用户操作。

```sql
INSERT INTO `sys_operation_log` (
  `id`,
  `trace_id`,
  `operator_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `operation_desc`,
  `request_method`,
  `request_uri`,
  `request_params`,
  `response_body`,
  `client_ip`,
  `user_agent`,
  `success_status`,
  `cost_millis`,
  `operate_time`
) VALUES (
  100001,
  'TRACE_202605110001',
  200001,
  '系统管理员',
  '用户管理',
  'create',
  '新增系统用户',
  'POST',
  '/system/user',
  JSON_OBJECT('username', 'zhangsan', 'nickname', '张三'),
  JSON_OBJECT('code', 200, 'message', '操作成功'),
  '192.168.1.10',
  'Mozilla/5.0',
  1,
  128,
  NOW()
);
```

### 新增失败操作日志

下面的 SQL 用于记录一次失败的删除操作，适合异常排查和安全审计。

```sql
INSERT INTO `sys_operation_log` (
  `id`,
  `trace_id`,
  `operator_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `operation_desc`,
  `request_method`,
  `request_uri`,
  `request_params`,
  `response_body`,
  `client_ip`,
  `user_agent`,
  `success_status`,
  `error_message`,
  `cost_millis`,
  `operate_time`
) VALUES (
  100002,
  'TRACE_202605110002',
  200001,
  '系统管理员',
  '角色管理',
  'delete',
  '删除系统角色',
  'DELETE',
  '/system/role/300001',
  JSON_OBJECT('role_id', 300001),
  JSON_OBJECT('code', 500, 'message', '角色已被用户绑定，不能删除'),
  '192.168.1.10',
  'Mozilla/5.0',
  0,
  '角色已被用户绑定，不能删除',
  86,
  NOW()
);
```

### 查询操作日志详情

下面的 SQL 用于根据日志 ID 查询操作日志详情。

```sql
SELECT
  `id`,
  `trace_id`,
  `operator_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `operation_desc`,
  `request_method`,
  `request_uri`,
  `request_params`,
  `response_body`,
  `client_ip`,
  `user_agent`,
  `success_status`,
  `error_message`,
  `cost_millis`,
  `operate_time`
FROM `sys_operation_log`
WHERE `id` = 100001;
```

### 按操作人查询日志

下面的 SQL 用于查询某个用户的操作记录，适合后台操作追踪。

```sql
SELECT
  `id`,
  `trace_id`,
  `operator_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `operation_desc`,
  `success_status`,
  `cost_millis`,
  `operate_time`
FROM `sys_operation_log`
WHERE `operator_id` = 200001
ORDER BY `operate_time` DESC
LIMIT 20 OFFSET 0;
```

### 按模块和类型查询日志

下面的 SQL 用于查询某个模块下指定操作类型的日志，例如查询用户管理模块的新增操作。

```sql
SELECT
  `id`,
  `operator_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `operation_desc`,
  `request_uri`,
  `success_status`,
  `cost_millis`,
  `operate_time`
FROM `sys_operation_log`
WHERE `operation_module` = '用户管理'
  AND `operation_type` = 'create'
ORDER BY `operate_time` DESC
LIMIT 20 OFFSET 0;
```

### 按时间范围查询日志

下面的 SQL 用于查询某段时间内的操作日志，适合日志检索和问题排查。

```sql
SELECT
  `id`,
  `trace_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `operation_desc`,
  `success_status`,
  `operate_time`
FROM `sys_operation_log`
WHERE `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
ORDER BY `operate_time` DESC
LIMIT 50 OFFSET 0;
```

### 查询失败操作日志

下面的 SQL 用于查询失败操作，适合异常分析和安全监控。

```sql
SELECT
  `id`,
  `trace_id`,
  `operator_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `operation_desc`,
  `request_uri`,
  `error_message`,
  `operate_time`
FROM `sys_operation_log`
WHERE `success_status` = 0
ORDER BY `operate_time` DESC
LIMIT 20 OFFSET 0;
```

### 查询慢操作日志

下面的 SQL 用于查询耗时较长的操作，适合接口性能分析。

```sql
SELECT
  `id`,
  `trace_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `request_method`,
  `request_uri`,
  `cost_millis`,
  `operate_time`
FROM `sys_operation_log`
WHERE `cost_millis` >= 1000
ORDER BY `cost_millis` DESC, `operate_time` DESC
LIMIT 20 OFFSET 0;
```

### 根据链路追踪 ID 查询日志

下面的 SQL 用于根据 `trace_id` 查询同一次请求链路相关的操作日志。

```sql
SELECT
  `id`,
  `trace_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `operation_desc`,
  `request_uri`,
  `success_status`,
  `cost_millis`,
  `operate_time`
FROM `sys_operation_log`
WHERE `trace_id` = 'TRACE_202605110001'
ORDER BY `operate_time` ASC;
```

### 统计操作类型数量

下面的 SQL 用于统计不同操作类型的数量，适合后台报表和运维分析。

```sql
SELECT
  `operation_type`,
  COUNT(*) AS `total_count`
FROM `sys_operation_log`
WHERE `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
GROUP BY `operation_type`
ORDER BY `total_count` DESC;
```

### 统计操作成功率

下面的 SQL 用于按模块统计操作成功率。

```sql
SELECT
  `operation_module`,
  COUNT(*) AS `total_count`,
  SUM(CASE WHEN `success_status` = 1 THEN 1 ELSE 0 END) AS `success_count`,
  SUM(CASE WHEN `success_status` = 0 THEN 1 ELSE 0 END) AS `fail_count`,
  ROUND(SUM(CASE WHEN `success_status` = 1 THEN 1 ELSE 0 END) / COUNT(*) * 100, 2) AS `success_rate`
FROM `sys_operation_log`
WHERE `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
GROUP BY `operation_module`
ORDER BY `fail_count` DESC;
```

### 统计用户操作次数

下面的 SQL 用于统计用户操作次数，适合行为分析和安全监控。

```sql
SELECT
  `operator_id`,
  `operator_name`,
  COUNT(*) AS `operation_count`
FROM `sys_operation_log`
WHERE `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
GROUP BY
  `operator_id`,
  `operator_name`
ORDER BY `operation_count` DESC
LIMIT 20;
```

### 查询高频访问接口

下面的 SQL 用于统计接口访问次数，适合接口热点分析。

```sql
SELECT
  `request_method`,
  `request_uri`,
  COUNT(*) AS `request_count`,
  ROUND(AVG(`cost_millis`), 2) AS `avg_cost_millis`,
  MAX(`cost_millis`) AS `max_cost_millis`
FROM `sys_operation_log`
WHERE `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
GROUP BY
  `request_method`,
  `request_uri`
ORDER BY `request_count` DESC
LIMIT 20;
```

### 清理历史操作日志

下面的 SQL 用于清理指定时间之前的操作日志。生产环境通常建议先归档，再清理。

```sql
DELETE FROM `sys_operation_log`
WHERE `operate_time` < DATE_SUB(NOW(), INTERVAL 180 DAY);
```

### 索引设计建议

操作日志模型的索引重点是操作人、操作时间、操作模块、操作类型、成功状态和请求地址。日志表数据量增长较快，索引不宜过多。

| 索引                                                         | 适用场景             |
| ------------------------------------------------------------ | -------------------- |
| `PRIMARY KEY (id)`                                           | 查询日志详情         |
| `idx_trace_id (trace_id)`                                    | 根据链路追踪 ID 查询 |
| `idx_operator_time (operator_id, operate_time)`              | 查询用户操作记录     |
| `idx_module_type_time (operation_module, operation_type, operate_time)` | 按模块和操作类型查询 |
| `idx_status_time (success_status, operate_time)`             | 查询失败操作         |
| `idx_uri_time (request_uri, operate_time)`                   | 查询接口访问记录     |

### 使用建议

操作日志模型的核心是记录操作行为，不是记录完整数据变更。它可以记录请求参数和响应摘要，但不建议保存大体积响应内容或敏感字段明文。

使用操作日志模型时重点注意：

- 操作日志应尽量异步写入，避免影响主业务性能
- 请求参数和响应结果应脱敏，避免保存密码、Token、身份证号等敏感信息
- 不建议保存完整响应体，通常保存摘要即可
- 日志表数据量较大时，可以按月分区或定期归档
- 操作失败、权限拒绝、删除数据、导出数据等行为应重点记录
- `trace_id` 应贯穿请求链路，便于排查问题
- 操作日志可以物理归档，但不建议随意修改
- 高频查询字段需要索引，但不要给 JSON 字段随意建索引
- 操作日志关注“行为”，审计日志关注“数据变化”

## 审计日志模型

审计日志模型用于记录核心业务数据的变更过程，例如新增、修改、删除、状态流转、权限变更等。它关注的是“哪条数据发生了什么变化”，通常保存变更前数据、变更后数据、变更字段、操作人和操作原因。

下面以“系统审计日志表”为例说明审计日志模型的建表方式和常用 SQL 实践。

### 适用场景

审计日志模型适合对关键业务数据进行变更追踪和合规审计。它比操作日志更关注数据本身的变化细节。

常见场景包括：

- 用户资料变更审计
- 角色权限变更审计
- 订单金额变更审计
- 账户余额变更审计
- 合同信息变更审计
- 审批状态变更审计
- 配置项变更审计
- 商品价格变更审计
- 客户资料变更审计
- 数据删除审计

### 建表 SQL

下面的 SQL 创建系统审计日志表，用于记录业务数据变更前后的 JSON 快照、变更字段和操作上下文。

```sql
CREATE TABLE `sys_audit_log` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `audit_no` VARCHAR(64) NOT NULL COMMENT '审计编号',
  `trace_id` VARCHAR(128) NULL COMMENT '链路追踪ID',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型：user用户，role角色，order订单，config配置',
  `biz_id` BIGINT NOT NULL COMMENT '业务数据ID',
  `biz_no` VARCHAR(64) NULL COMMENT '业务数据编号',
  `table_name` VARCHAR(128) NOT NULL COMMENT '变更表名',
  `data_id` BIGINT NOT NULL COMMENT '变更数据主键ID',
  `audit_action` VARCHAR(32) NOT NULL COMMENT '审计动作：insert新增，update修改，delete删除，status状态变更',
  `before_data` JSON NULL COMMENT '变更前数据JSON',
  `after_data` JSON NULL COMMENT '变更后数据JSON',
  `changed_fields` JSON NULL COMMENT '变更字段JSON数组',
  `change_summary` VARCHAR(1000) NULL COMMENT '变更摘要',
  `operator_id` BIGINT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(128) NULL COMMENT '操作人名称',
  `operate_reason` VARCHAR(500) NULL COMMENT '操作原因',
  `client_ip` VARCHAR(64) NULL COMMENT '客户端IP',
  `operate_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_audit_no` (`audit_no`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_biz_time` (`biz_type`, `biz_id`, `operate_time`),
  KEY `idx_biz_no_time` (`biz_type`, `biz_no`, `operate_time`),
  KEY `idx_table_data_time` (`table_name`, `data_id`, `operate_time`),
  KEY `idx_action_time` (`audit_action`, `operate_time`),
  KEY `idx_operator_time` (`operator_id`, `operate_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统审计日志表';
```

### 字段设计说明

审计日志表应重点记录业务对象、数据表、变更动作、变更前后内容、变更字段和操作人。审计日志原则上只新增，不修改，不删除。

| 字段             | 说明                   |
| ---------------- | ---------------------- |
| `audit_no`       | 审计编号，业务唯一标识 |
| `trace_id`       | 链路追踪 ID            |
| `biz_type`       | 业务类型               |
| `biz_id`         | 业务对象 ID            |
| `biz_no`         | 业务对象编号           |
| `table_name`     | 被变更的表名           |
| `data_id`        | 被变更的数据主键 ID    |
| `audit_action`   | 审计动作               |
| `before_data`    | 变更前数据             |
| `after_data`     | 变更后数据             |
| `changed_fields` | 发生变化的字段         |
| `change_summary` | 变更摘要               |
| `operator_id`    | 操作人 ID              |
| `operate_reason` | 操作原因               |
| `operate_time`   | 操作时间               |

### 新增数据审计日志

下面的 SQL 用于记录新增用户时的审计日志。新增操作通常没有变更前数据，只记录变更后数据。

```sql
INSERT INTO `sys_audit_log` (
  `id`,
  `audit_no`,
  `trace_id`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `table_name`,
  `data_id`,
  `audit_action`,
  `before_data`,
  `after_data`,
  `changed_fields`,
  `change_summary`,
  `operator_id`,
  `operator_name`,
  `operate_reason`,
  `client_ip`,
  `operate_time`
) VALUES (
  200001,
  'AUDIT_202605110001',
  'TRACE_202605110001',
  'user',
  300001,
  'USER_202605110001',
  'sys_user',
  300001,
  'insert',
  NULL,
  JSON_OBJECT(
    'id', 300001,
    'username', 'zhangsan',
    'nickname', '张三',
    'mobile', '13800000000',
    'user_status', 1
  ),
  JSON_ARRAY('id', 'username', 'nickname', 'mobile', 'user_status'),
  '新增用户：zhangsan',
  200001,
  '系统管理员',
  '后台新增用户',
  '192.168.1.10',
  NOW()
);
```

### 修改数据审计日志

下面的 SQL 用于记录修改用户资料时的审计日志。修改操作应同时记录变更前数据、变更后数据和变更字段。

```sql
INSERT INTO `sys_audit_log` (
  `id`,
  `audit_no`,
  `trace_id`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `table_name`,
  `data_id`,
  `audit_action`,
  `before_data`,
  `after_data`,
  `changed_fields`,
  `change_summary`,
  `operator_id`,
  `operator_name`,
  `operate_reason`,
  `client_ip`,
  `operate_time`
) VALUES (
  200002,
  'AUDIT_202605110002',
  'TRACE_202605110002',
  'user',
  300001,
  'USER_202605110001',
  'sys_user',
  300001,
  'update',
  JSON_OBJECT(
    'nickname', '张三',
    'mobile', '13800000000',
    'user_status', 1
  ),
  JSON_OBJECT(
    'nickname', '张三丰',
    'mobile', '13900000000',
    'user_status', 1
  ),
  JSON_ARRAY('nickname', 'mobile'),
  '修改用户昵称和手机号',
  200001,
  '系统管理员',
  '用户资料修正',
  '192.168.1.10',
  NOW()
);
```

### 删除数据审计日志

下面的 SQL 用于记录逻辑删除业务数据时的审计日志。删除操作通常重点记录删除前数据。

```sql
INSERT INTO `sys_audit_log` (
  `id`,
  `audit_no`,
  `trace_id`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `table_name`,
  `data_id`,
  `audit_action`,
  `before_data`,
  `after_data`,
  `changed_fields`,
  `change_summary`,
  `operator_id`,
  `operator_name`,
  `operate_reason`,
  `client_ip`,
  `operate_time`
) VALUES (
  200003,
  'AUDIT_202605110003',
  'TRACE_202605110003',
  'user',
  300001,
  'USER_202605110001',
  'sys_user',
  300001,
  'delete',
  JSON_OBJECT(
    'id', 300001,
    'username', 'zhangsan',
    'nickname', '张三丰',
    'mobile', '13900000000',
    'is_deleted', 0
  ),
  JSON_OBJECT(
    'id', 300001,
    'username', 'zhangsan',
    'nickname', '张三丰',
    'mobile', '13900000000',
    'is_deleted', 1
  ),
  JSON_ARRAY('is_deleted'),
  '逻辑删除用户',
  200001,
  '系统管理员',
  '测试账号清理',
  '192.168.1.10',
  NOW()
);
```

### 状态变更审计日志

下面的 SQL 用于记录状态变更，例如用户从启用变为禁用。

```sql
INSERT INTO `sys_audit_log` (
  `id`,
  `audit_no`,
  `trace_id`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `table_name`,
  `data_id`,
  `audit_action`,
  `before_data`,
  `after_data`,
  `changed_fields`,
  `change_summary`,
  `operator_id`,
  `operator_name`,
  `operate_reason`,
  `client_ip`,
  `operate_time`
) VALUES (
  200004,
  'AUDIT_202605110004',
  'TRACE_202605110004',
  'user',
  300001,
  'USER_202605110001',
  'sys_user',
  300001,
  'status',
  JSON_OBJECT('user_status', 1),
  JSON_OBJECT('user_status', 0),
  JSON_ARRAY('user_status'),
  '用户状态由启用变更为禁用',
  200001,
  '系统管理员',
  '账号异常，临时禁用',
  '192.168.1.10',
  NOW()
);
```

### 查询业务对象审计记录

下面的 SQL 用于查询某个业务对象的所有审计日志，适合详情页变更历史展示。

```sql
SELECT
  `id`,
  `audit_no`,
  `trace_id`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `table_name`,
  `audit_action`,
  `changed_fields`,
  `change_summary`,
  `operator_id`,
  `operator_name`,
  `operate_reason`,
  `operate_time`
FROM `sys_audit_log`
WHERE `biz_type` = 'user'
  AND `biz_id` = 300001
ORDER BY `operate_time` DESC;
```

### 查询某张表的数据变更历史

下面的 SQL 用于查询某张表中某条数据的完整变更历史。

```sql
SELECT
  `id`,
  `audit_no`,
  `audit_action`,
  `before_data`,
  `after_data`,
  `changed_fields`,
  `change_summary`,
  `operator_name`,
  `operate_time`
FROM `sys_audit_log`
WHERE `table_name` = 'sys_user'
  AND `data_id` = 300001
ORDER BY `operate_time` ASC;
```

### 查询某个字段的变更记录

下面的 SQL 用于查询发生过指定字段变更的审计日志，例如查询手机号变更记录。

```sql
SELECT
  `id`,
  `audit_no`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `before_data`,
  `after_data`,
  `changed_fields`,
  `change_summary`,
  `operator_name`,
  `operate_time`
FROM `sys_audit_log`
WHERE JSON_CONTAINS(`changed_fields`, JSON_QUOTE('mobile'))
ORDER BY `operate_time` DESC
LIMIT 20 OFFSET 0;
```

### 查询字段变更前后值

下面的 SQL 用于提取 JSON 中某个字段的变更前值和变更后值。

```sql
SELECT
  `id`,
  `audit_no`,
  JSON_UNQUOTE(JSON_EXTRACT(`before_data`, '$.mobile')) AS `before_mobile`,
  JSON_UNQUOTE(JSON_EXTRACT(`after_data`, '$.mobile')) AS `after_mobile`,
  `operator_name`,
  `operate_time`
FROM `sys_audit_log`
WHERE `biz_type` = 'user'
  AND `biz_id` = 300001
  AND JSON_CONTAINS(`changed_fields`, JSON_QUOTE('mobile'))
ORDER BY `operate_time` DESC;
```

### 查询某个操作人的审计记录

下面的 SQL 用于查询某个操作人产生的审计日志。

```sql
SELECT
  `id`,
  `audit_no`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `table_name`,
  `audit_action`,
  `change_summary`,
  `operate_reason`,
  `operate_time`
FROM `sys_audit_log`
WHERE `operator_id` = 200001
ORDER BY `operate_time` DESC
LIMIT 20 OFFSET 0;
```

### 按审计动作查询

下面的 SQL 用于查询某类变更动作，例如删除操作或状态变更操作。

```sql
SELECT
  `id`,
  `audit_no`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `table_name`,
  `audit_action`,
  `change_summary`,
  `operator_name`,
  `operate_time`
FROM `sys_audit_log`
WHERE `audit_action` = 'delete'
ORDER BY `operate_time` DESC
LIMIT 20 OFFSET 0;
```

### 按时间范围查询审计日志

下面的 SQL 用于查询某段时间内的审计日志。

```sql
SELECT
  `id`,
  `audit_no`,
  `biz_type`,
  `biz_no`,
  `table_name`,
  `audit_action`,
  `change_summary`,
  `operator_name`,
  `operate_time`
FROM `sys_audit_log`
WHERE `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
ORDER BY `operate_time` DESC
LIMIT 50 OFFSET 0;
```

### 根据链路追踪 ID 查询审计日志

下面的 SQL 用于查询某次请求链路下产生的所有审计日志。

```sql
SELECT
  `id`,
  `audit_no`,
  `biz_type`,
  `biz_id`,
  `biz_no`,
  `table_name`,
  `audit_action`,
  `change_summary`,
  `operator_name`,
  `operate_time`
FROM `sys_audit_log`
WHERE `trace_id` = 'TRACE_202605110002'
ORDER BY `operate_time` ASC;
```

### 统计业务变更次数

下面的 SQL 用于统计不同业务类型的变更次数。

```sql
SELECT
  `biz_type`,
  `audit_action`,
  COUNT(*) AS `change_count`
FROM `sys_audit_log`
WHERE `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
GROUP BY
  `biz_type`,
  `audit_action`
ORDER BY `change_count` DESC;
```

### 查询频繁变更的数据

下面的 SQL 用于查询变更次数较多的业务数据，适合风险分析和数据治理。

```sql
SELECT
  `biz_type`,
  `biz_id`,
  `biz_no`,
  COUNT(*) AS `change_count`,
  MAX(`operate_time`) AS `last_operate_time`
FROM `sys_audit_log`
WHERE `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
GROUP BY
  `biz_type`,
  `biz_id`,
  `biz_no`
HAVING COUNT(*) >= 5
ORDER BY `change_count` DESC;
```

### 查询无操作原因的敏感变更

下面的 SQL 用于查询缺少操作原因的敏感变更，适合审计规范检查。

```sql
SELECT
  `id`,
  `audit_no`,
  `biz_type`,
  `biz_no`,
  `table_name`,
  `audit_action`,
  `changed_fields`,
  `change_summary`,
  `operator_name`,
  `operate_time`
FROM `sys_audit_log`
WHERE `audit_action` IN ('delete', 'status', 'update')
  AND (
    `operate_reason` IS NULL
    OR `operate_reason` = ''
  )
ORDER BY `operate_time` DESC;
```

### 归档历史审计日志

下面的 SQL 用于将历史审计日志写入归档表。实际生产中通常先创建同结构归档表，再迁移历史数据。

```sql
INSERT INTO `sys_audit_log_archive`
SELECT *
FROM `sys_audit_log`
WHERE `operate_time` < DATE_SUB(NOW(), INTERVAL 365 DAY);
```

### 清理已归档审计日志

下面的 SQL 用于清理已归档的历史审计日志。生产环境应确认归档成功后再执行。

```sql
DELETE FROM `sys_audit_log`
WHERE `operate_time` < DATE_SUB(NOW(), INTERVAL 365 DAY);
```

### 索引设计建议

审计日志模型的索引重点是业务对象、表名、数据 ID、操作人、审计动作和操作时间。

| 索引                                                      | 适用场景                 |
| --------------------------------------------------------- | ------------------------ |
| `PRIMARY KEY (id)`                                        | 查询审计日志详情         |
| `uk_audit_no (audit_no)`                                  | 根据审计编号查询         |
| `idx_trace_id (trace_id)`                                 | 查询请求链路审计记录     |
| `idx_biz_time (biz_type, biz_id, operate_time)`           | 查询业务对象变更历史     |
| `idx_biz_no_time (biz_type, biz_no, operate_time)`        | 根据业务编号查询变更历史 |
| `idx_table_data_time (table_name, data_id, operate_time)` | 查询表数据变更历史       |
| `idx_action_time (audit_action, operate_time)`            | 查询指定审计动作         |
| `idx_operator_time (operator_id, operate_time)`           | 查询操作人的审计记录     |

### 使用建议

审计日志模型的核心原则是可追溯、不可随意篡改。它应记录关键数据变更前后的内容，便于还原业务过程、分析责任边界和满足合规要求。

使用审计日志模型时重点注意：

- 审计日志原则上只新增，不修改，不删除
- 核心业务数据的新增、修改、删除、状态变更都应记录审计日志
- 修改操作应记录变更前数据、变更后数据和变更字段
- 删除操作应至少记录删除前数据
- 敏感变更应记录操作原因
- 审计日志中的敏感字段应脱敏或加密
- 审计日志和业务变更建议放在同一个事务中
- 审计日志数据量较大时，建议按月分区或定期归档
- 高频查询条件应依赖普通字段索引，不要依赖 JSON 全表扫描
- 操作日志关注用户行为，审计日志关注数据变化

## 软删除模型

软删除模型用于保留业务数据历史，不直接从数据库中物理删除记录。删除操作本质上是更新删除标识，例如将 `is_deleted` 从 `0` 改为 `1`，同时记录删除人、删除时间等信息。软删除常用于后台管理系统、订单、用户、商品、配置、合同等需要追踪历史数据的业务场景。

下面以“文章表”为例说明软删除模型的建表方式和常用 SQL 实践。

### 适用场景

软删除模型适合数据删除后仍需要保留历史记录、审计追踪、恢复数据或避免误删的业务场景。

常见场景包括：

- 用户数据
- 商品数据
- 文章数据
- 配置数据
- 订单数据
- 合同数据
- 字典数据
- 分类数据
- 标签数据
- 后台管理数据

### 建表 SQL

下面的 SQL 创建文章表。表中包含 `is_deleted`、`deleted_by`、`deleted_at` 和 `delete_marker` 字段，用于支持软删除和唯一索引复用。

```sql
CREATE TABLE `cms_article` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `article_code` VARCHAR(64) NOT NULL COMMENT '文章编码',
  `article_title` VARCHAR(200) NOT NULL COMMENT '文章标题',
  `article_content` TEXT NULL COMMENT '文章内容',
  `article_status` TINYINT NOT NULL DEFAULT 0 COMMENT '文章状态：0草稿，1发布，2下架',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，值越大越靠前',
  `publish_time` DATETIME NULL COMMENT '发布时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `delete_marker` BIGINT NOT NULL DEFAULT 0 COMMENT '删除标记：未删除为0，删除后写入主键ID或唯一值',
  `deleted_by` BIGINT NULL COMMENT '删除人ID',
  `deleted_at` DATETIME NULL COMMENT '删除时间',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_code_delete_marker` (`article_code`, `delete_marker`),
  KEY `idx_status_deleted` (`article_status`, `is_deleted`),
  KEY `idx_deleted_time` (`is_deleted`, `deleted_at`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='CMS文章表';
```

### 字段设计说明

软删除模型的核心字段是 `is_deleted`。如果业务字段存在唯一约束，建议额外增加 `delete_marker` 字段，用于解决软删除后重新创建同编码数据的问题。

| 字段            | 说明                                         |
| --------------- | -------------------------------------------- |
| `is_deleted`    | 逻辑删除标识，`0` 未删除，`1` 已删除         |
| `delete_marker` | 删除唯一标记，未删除为 `0`，删除后写入唯一值 |
| `deleted_by`    | 删除人 ID                                    |
| `deleted_at`    | 删除时间                                     |
| `updated_by`    | 最后更新人 ID                                |
| `updated_at`    | 最后更新时间                                 |

### 新增数据

下面的 SQL 用于新增一篇文章。新增数据时，`is_deleted` 默认为 `0`，`delete_marker` 默认为 `0`。

```sql
INSERT INTO `cms_article` (
  `id`,
  `article_code`,
  `article_title`,
  `article_content`,
  `article_status`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  'ARTICLE_202605110001',
  'MySQL 8 业务建模实践',
  '这里是文章正文内容。',
  0,
  100,
  '软删除模型示例数据',
  1,
  1
);
```

### 查询未删除数据

下面的 SQL 用于查询正常业务数据。软删除模型下，所有业务查询都应默认带上 `is_deleted = 0` 条件。

```sql
SELECT
  `id`,
  `article_code`,
  `article_title`,
  `article_status`,
  `sort_order`,
  `publish_time`,
  `created_at`,
  `updated_at`
FROM `cms_article`
WHERE `is_deleted` = 0
ORDER BY `created_at` DESC
LIMIT 10 OFFSET 0;
```

### 根据主键查询未删除数据

下面的 SQL 用于根据主键查询文章详情。

```sql
SELECT
  `id`,
  `article_code`,
  `article_title`,
  `article_content`,
  `article_status`,
  `publish_time`,
  `remark`,
  `created_at`,
  `updated_at`
FROM `cms_article`
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 根据唯一编码查询未删除数据

下面的 SQL 用于根据文章编码查询未删除数据。因为唯一索引使用了 `article_code` 和 `delete_marker`，所以查询未删除数据时也可以带上 `delete_marker = 0`。

```sql
SELECT
  `id`,
  `article_code`,
  `article_title`,
  `article_status`,
  `created_at`
FROM `cms_article`
WHERE `article_code` = 'ARTICLE_202605110001'
  AND `delete_marker` = 0
  AND `is_deleted` = 0;
```

### 软删除数据

下面的 SQL 用于逻辑删除文章。删除时将 `is_deleted` 改为 `1`，同时写入删除人、删除时间和删除标记。

```sql
UPDATE `cms_article`
SET
  `is_deleted` = 1,
  `delete_marker` = `id`,
  `deleted_by` = 1,
  `deleted_at` = NOW(),
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 批量软删除数据

下面的 SQL 用于批量逻辑删除多条文章。

```sql
UPDATE `cms_article`
SET
  `is_deleted` = 1,
  `delete_marker` = `id`,
  `deleted_by` = 1,
  `deleted_at` = NOW(),
  `updated_by` = 1
WHERE `id` IN (100001, 100002, 100003)
  AND `is_deleted` = 0;
```

### 查询已删除数据

下面的 SQL 用于查询回收站数据，适合后台恢复或清理历史数据。

```sql
SELECT
  `id`,
  `article_code`,
  `article_title`,
  `article_status`,
  `deleted_by`,
  `deleted_at`,
  `created_at`
FROM `cms_article`
WHERE `is_deleted` = 1
ORDER BY `deleted_at` DESC
LIMIT 10 OFFSET 0;
```

### 恢复软删除数据

下面的 SQL 用于恢复已删除文章。恢复前需要确认当前不存在相同 `article_code` 的未删除数据，否则会触发唯一索引冲突。

```sql
UPDATE `cms_article`
SET
  `is_deleted` = 0,
  `delete_marker` = 0,
  `deleted_by` = NULL,
  `deleted_at` = NULL,
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 1
  AND NOT EXISTS (
    SELECT 1
    FROM (
      SELECT `id`
      FROM `cms_article`
      WHERE `article_code` = 'ARTICLE_202605110001'
        AND `delete_marker` = 0
        AND `is_deleted` = 0
    ) t
  );
```

### 查询可恢复数据

下面的 SQL 用于查询可以恢复的数据，即当前没有同编码未删除数据的已删除记录。

```sql
SELECT
  a.`id`,
  a.`article_code`,
  a.`article_title`,
  a.`deleted_at`
FROM `cms_article` a
LEFT JOIN `cms_article` b
  ON a.`article_code` = b.`article_code`
  AND b.`delete_marker` = 0
  AND b.`is_deleted` = 0
WHERE a.`is_deleted` = 1
  AND b.`id` IS NULL
ORDER BY a.`deleted_at` DESC;
```

### 重新创建同编码数据

下面的 SQL 演示软删除后重新创建同编码数据。因为旧数据的 `delete_marker` 已经不再是 `0`，所以新数据可以继续使用相同的 `article_code`。

```sql
INSERT INTO `cms_article` (
  `id`,
  `article_code`,
  `article_title`,
  `article_content`,
  `article_status`,
  `sort_order`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  100002,
  'ARTICLE_202605110001',
  'MySQL 8 业务建模实践新版',
  '这里是新版文章正文内容。',
  0,
  100,
  '重新创建同编码文章',
  1,
  1
);
```

### 物理清理历史删除数据

下面的 SQL 用于物理清理已经软删除很久的数据。生产环境通常建议先归档，再清理。

```sql
DELETE FROM `cms_article`
WHERE `is_deleted` = 1
  AND `deleted_at` < DATE_SUB(NOW(), INTERVAL 365 DAY);
```

### 统计删除数据数量

下面的 SQL 用于统计正常数据和已删除数据数量，适合后台数据治理。

```sql
SELECT
  `is_deleted`,
  COUNT(*) AS `total_count`
FROM `cms_article`
GROUP BY `is_deleted`;
```

### 查询最近删除的数据

下面的 SQL 用于查询最近被删除的数据，适合回收站页面或误删恢复。

```sql
SELECT
  `id`,
  `article_code`,
  `article_title`,
  `deleted_by`,
  `deleted_at`
FROM `cms_article`
WHERE `is_deleted` = 1
  AND `deleted_at` >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY `deleted_at` DESC;
```

### 查询删除人删除数量

下面的 SQL 用于统计不同用户的删除操作数量，适合审计分析。

```sql
SELECT
  `deleted_by`,
  COUNT(*) AS `delete_count`
FROM `cms_article`
WHERE `is_deleted` = 1
  AND `deleted_at` >= '2026-05-01 00:00:00'
  AND `deleted_at` < '2026-06-01 00:00:00'
GROUP BY `deleted_by`
ORDER BY `delete_count` DESC;
```

### 索引设计建议

软删除模型的索引设计要同时考虑正常查询、回收站查询和唯一字段复用。

| 索引                                                         | 适用场景                                         |
| ------------------------------------------------------------ | ------------------------------------------------ |
| `PRIMARY KEY (id)`                                           | 根据主键查询                                     |
| `uk_article_code_delete_marker (article_code, delete_marker)` | 保证未删除数据编码唯一，同时支持软删除后复用编码 |
| `idx_status_deleted (article_status, is_deleted)`            | 查询未删除的指定状态数据                         |
| `idx_deleted_time (is_deleted, deleted_at)`                  | 查询回收站和清理历史删除数据                     |
| `idx_created_at (created_at)`                                | 按创建时间分页查询                               |

### 使用建议

软删除模型适合大多数后台管理类业务，但不能代替审计日志。软删除只能表示数据被删除，不能完整记录删除前后的业务上下文；关键业务仍应配合操作日志或审计日志使用。

使用软删除模型时重点注意：

- 所有正常业务查询都要默认带上 `is_deleted = 0`
- 删除操作使用 `UPDATE`，不直接使用 `DELETE`
- 建议记录 `deleted_by` 和 `deleted_at`
- 有唯一约束的业务字段，建议配合 `delete_marker` 解决软删除后重新创建的问题
- 不建议只使用 `UNIQUE KEY (code, is_deleted)`，因为多条已删除数据可能产生唯一冲突
- 已软删除数据可以进入回收站，支持恢复
- 恢复数据前必须检查唯一字段是否冲突
- 历史软删除数据可以定期归档和物理清理
- 软删除不等于审计，关键业务删除仍应记录审计日志
- 超大表使用软删除时，要关注索引膨胀和历史数据归档

## 乐观锁模型

乐观锁模型用于解决并发更新时的数据覆盖问题。它假设大多数情况下不会发生并发冲突，因此不提前加锁，而是在更新时通过版本号判断数据是否被其他事务修改过。常见做法是在表中增加 `version` 字段，更新时带上原版本号条件，更新成功后版本号加一。

下面以“库存表”为例说明乐观锁模型的建表方式和常用 SQL 实践。

### 适用场景

乐观锁模型适合读多写少、并发冲突概率不高，但又不能接受数据被覆盖的业务场景。

常见场景包括：

- 商品库存扣减
- 账户余额更新
- 订单状态更新
- 配置项修改
- 审批单状态变更
- 用户资料编辑
- 商品价格修改
- 优惠券领取
- 任务抢占
- 表单数据提交

### 建表 SQL

下面的 SQL 创建库存表。表中包含 `version` 字段，用于控制并发更新。

```sql
CREATE TABLE `mall_sku_stock` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
  `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU编码',
  `stock_quantity` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `locked_stock_quantity` INT NOT NULL DEFAULT 0 COMMENT '锁定库存数量',
  `stock_status` TINYINT NOT NULL DEFAULT 1 COMMENT '库存状态：0禁用，1启用',
  `version` INT NOT NULL DEFAULT 0 COMMENT '版本号，用于乐观锁',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_id` (`sku_id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_status_deleted` (`stock_status`, `is_deleted`),
  KEY `idx_version` (`version`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='商城SKU库存表';
```

### 字段设计说明

乐观锁模型的核心字段是 `version`。查询数据时读取当前版本号，更新数据时带上该版本号作为条件。如果更新影响行数为 `0`，说明数据已经被其他事务修改，需要业务代码重新查询并重试或提示用户刷新。

| 字段                    | 说明         |
| ----------------------- | ------------ |
| `stock_quantity`        | 当前库存数量 |
| `locked_stock_quantity` | 锁定库存数量 |
| `version`               | 乐观锁版本号 |
| `stock_status`          | 库存状态     |
| `updated_by`            | 更新人       |
| `updated_at`            | 更新时间     |

### 新增库存数据

下面的 SQL 用于新增一条 SKU 库存记录。

```sql
INSERT INTO `mall_sku_stock` (
  `id`,
  `sku_id`,
  `sku_code`,
  `stock_quantity`,
  `locked_stock_quantity`,
  `stock_status`,
  `version`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  200001,
  'SKU_202605110001',
  100,
  0,
  1,
  0,
  'SKU库存初始化',
  1,
  1
);
```

### 查询当前版本号

下面的 SQL 用于查询库存当前值和版本号。业务更新前通常需要先查询当前版本号。

```sql
SELECT
  `id`,
  `sku_id`,
  `sku_code`,
  `stock_quantity`,
  `locked_stock_quantity`,
  (`stock_quantity` - `locked_stock_quantity`) AS `available_stock_quantity`,
  `stock_status`,
  `version`
FROM `mall_sku_stock`
WHERE `sku_id` = 200001
  AND `stock_status` = 1
  AND `is_deleted` = 0;
```

### 使用乐观锁扣减库存

下面的 SQL 用于扣减库存。更新条件中带上 `version = 0`，只有数据版本未变化时才会更新成功。

```sql
UPDATE `mall_sku_stock`
SET
  `stock_quantity` = `stock_quantity` - 1,
  `version` = `version` + 1,
  `updated_by` = 1
WHERE `sku_id` = 200001
  AND `version` = 0
  AND `stock_quantity` >= 1
  AND `stock_status` = 1
  AND `is_deleted` = 0;
```

### 判断乐观锁是否更新成功

下面的 SQL 用于查看上一条更新语句影响的行数。影响行数为 `1` 表示更新成功，为 `0` 表示版本冲突或条件不满足。

```sql
SELECT ROW_COUNT() AS `affected_rows`;
```

### 使用乐观锁锁定库存

下面的 SQL 用于订单提交时锁定库存。锁定库存时，可用库存必须充足，并且版本号必须匹配。

```sql
UPDATE `mall_sku_stock`
SET
  `locked_stock_quantity` = `locked_stock_quantity` + 1,
  `version` = `version` + 1,
  `updated_by` = 1
WHERE `sku_id` = 200001
  AND `version` = 1
  AND (`stock_quantity` - `locked_stock_quantity`) >= 1
  AND `stock_status` = 1
  AND `is_deleted` = 0;
```

### 使用乐观锁释放锁定库存

下面的 SQL 用于订单取消或支付超时时释放锁定库存。

```sql
UPDATE `mall_sku_stock`
SET
  `locked_stock_quantity` = `locked_stock_quantity` - 1,
  `version` = `version` + 1,
  `updated_by` = 1
WHERE `sku_id` = 200001
  AND `version` = 2
  AND `locked_stock_quantity` >= 1
  AND `stock_status` = 1
  AND `is_deleted` = 0;
```

### 使用乐观锁完成库存扣减

下面的 SQL 用于支付成功后，将锁定库存转为真实扣减库存。

```sql
UPDATE `mall_sku_stock`
SET
  `stock_quantity` = `stock_quantity` - 1,
  `locked_stock_quantity` = `locked_stock_quantity` - 1,
  `version` = `version` + 1,
  `updated_by` = 1
WHERE `sku_id` = 200001
  AND `version` = 3
  AND `stock_quantity` >= 1
  AND `locked_stock_quantity` >= 1
  AND `stock_status` = 1
  AND `is_deleted` = 0;
```

### 使用乐观锁修改配置

下面的 SQL 演示配置项修改场景。修改前先读取版本号，更新时带上版本号，避免覆盖其他管理员刚刚修改过的配置。

```sql
UPDATE `sys_config_item`
SET
  `config_value` = '10',
  `updated_by` = 1,
  `updated_at` = NOW(),
  `version` = `version` + 1
WHERE `config_key` = 'login.max.fail.count'
  AND `version` = 5
  AND `config_status` = 1
  AND `is_deleted` = 0;
```

### 配置项乐观锁字段补充

如果配置项表需要使用乐观锁，可以增加 `version` 字段。

```sql
ALTER TABLE `sys_config_item`
ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '版本号，用于乐观锁' AFTER `config_status`;
```

### 使用乐观锁更新订单状态

下面的 SQL 用于订单状态更新。订单状态更新通常同时带上原状态和版本号，避免重复流转或并发覆盖。

```sql
UPDATE `mall_order`
SET
  `order_status` = 1,
  `pay_status` = 1,
  `pay_time` = NOW(),
  `version` = `version` + 1,
  `updated_by` = 200001
WHERE `order_no` = 'ORDER_202605110001'
  AND `order_status` = 0
  AND `pay_status` = 0
  AND `version` = 0
  AND `is_deleted` = 0;
```

### 订单表乐观锁字段补充

如果订单表需要使用乐观锁，可以增加 `version` 字段。

```sql
ALTER TABLE `mall_order`
ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '版本号，用于乐观锁' AFTER `pay_status`;
```

### 查询版本冲突后的最新数据

下面的 SQL 用于在乐观锁更新失败后重新查询最新数据，便于业务代码重试或提示用户刷新。

```sql
SELECT
  `id`,
  `sku_id`,
  `sku_code`,
  `stock_quantity`,
  `locked_stock_quantity`,
  (`stock_quantity` - `locked_stock_quantity`) AS `available_stock_quantity`,
  `version`,
  `updated_at`
FROM `mall_sku_stock`
WHERE `sku_id` = 200001
  AND `is_deleted` = 0;
```

### 批量更新时使用乐观锁

下面的 SQL 演示批量更新时如何使用版本号。实际业务中，批量更新需要分别携带每条数据的当前版本号。

```sql
UPDATE `mall_sku_stock`
SET
  `stock_status` = 0,
  `version` = `version` + 1,
  `updated_by` = 1
WHERE `id` = 100001
  AND `version` = 4
  AND `stock_status` = 1
  AND `is_deleted` = 0;
```

### 查询库存不足的数据

下面的 SQL 用于查询可用库存不足的数据，适合库存预警。

```sql
SELECT
  `id`,
  `sku_id`,
  `sku_code`,
  `stock_quantity`,
  `locked_stock_quantity`,
  (`stock_quantity` - `locked_stock_quantity`) AS `available_stock_quantity`,
  `version`
FROM `mall_sku_stock`
WHERE `stock_status` = 1
  AND `is_deleted` = 0
  AND (`stock_quantity` - `locked_stock_quantity`) <= 10
ORDER BY `available_stock_quantity` ASC;
```

### 统计库存总量

下面的 SQL 用于统计库存总量、锁定库存和可用库存。

```sql
SELECT
  SUM(`stock_quantity`) AS `total_stock_quantity`,
  SUM(`locked_stock_quantity`) AS `total_locked_stock_quantity`,
  SUM(`stock_quantity` - `locked_stock_quantity`) AS `total_available_stock_quantity`
FROM `mall_sku_stock`
WHERE `stock_status` = 1
  AND `is_deleted` = 0;
```

### 逻辑删除库存记录

下面的 SQL 用于逻辑删除库存记录。删除时也可以带上版本号，避免删除被其他事务修改过的数据。

```sql
UPDATE `mall_sku_stock`
SET
  `is_deleted` = 1,
  `version` = `version` + 1,
  `updated_by` = 1
WHERE `id` = 100001
  AND `version` = 5
  AND `is_deleted` = 0;
```

### 索引设计建议

乐观锁模型的索引重点仍然是业务查询字段。`version` 字段一般不单独作为主要查询条件，更多是配合主键或唯一业务字段一起使用。

| 索引                                            | 适用场景                       |
| ----------------------------------------------- | ------------------------------ |
| `PRIMARY KEY (id)`                              | 根据主键更新和查询             |
| `uk_sku_id (sku_id)`                            | 根据 SKU 更新库存              |
| `uk_sku_code (sku_code)`                        | 根据 SKU 编码查询库存          |
| `idx_status_deleted (stock_status, is_deleted)` | 查询有效库存                   |
| `idx_version (version)`                         | 低频版本排查，一般不是核心索引 |

### 使用建议

乐观锁模型适合控制并发覆盖，但不适合所有高并发扣减场景。如果冲突非常频繁，乐观锁会导致大量重试，此时应考虑行锁、库存队列、Redis 原子扣减、分段库存或异步化方案。

使用乐观锁模型时重点注意：

- 表中需要有 `version` 字段
- 查询数据时要返回当前版本号
- 更新数据时必须带上原版本号条件
- 更新成功后版本号必须加一
- 更新影响行数为 `0` 时，表示版本冲突或业务条件不满足
- 版本冲突后应重新查询数据，再重试或提示用户刷新
- 乐观锁适合读多写少、冲突较少的场景
- 高冲突库存扣减场景不一定适合纯乐观锁
- 状态更新时建议同时带上原状态和版本号
- 乐观锁只能防止并发覆盖，不能替代业务幂等控制
- 账户、订单、库存等关键业务通常还需要配合流水、状态机或幂等表使用

## 冗余字段模型

冗余字段模型用于在业务表中保存一部分来自其他表的字段快照或常用展示字段，以减少高频查询中的表关联，提高查询效率，或者保留业务发生当时的数据快照。冗余字段不是为了替代主数据表，而是为了查询性能、历史快照和业务展示便利。

下面以“订单表冗余用户信息”和“订单明细表冗余商品信息”为例说明冗余字段模型的建表方式和常用 SQL 实践。

### 适用场景

冗余字段模型适合读多写少、列表查询频繁、历史数据需要保持快照的业务场景。典型特点是某些字段来自其他主数据表，但在当前业务中需要高频展示或需要保留当时值。

常见场景包括：

- 订单冗余用户昵称
- 订单冗余收货人信息
- 订单明细冗余商品名称
- 订单明细冗余商品规格
- 商品冗余分类名称
- 商品冗余品牌名称
- 文章冗余作者名称
- 评论冗余用户昵称
- 工单冗余处理人名称
- 审批记录冗余审批人名称

### 建表 SQL

下面的 SQL 创建订单表和订单明细表。订单表中冗余用户昵称、手机号、收货信息；订单明细表中冗余商品名称、SKU 名称、规格和图片。

```sql
CREATE TABLE `mall_order_redundant` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `user_no` VARCHAR(64) NULL COMMENT '用户编号快照',
  `user_nickname` VARCHAR(128) NULL COMMENT '用户昵称快照',
  `user_mobile` VARCHAR(32) NULL COMMENT '用户手机号快照',
  `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已发货，3已完成，4已取消',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0未支付，1已支付，2已退款',
  `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  `discount_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  `receiver_name` VARCHAR(64) NOT NULL COMMENT '收货人姓名快照',
  `receiver_phone` VARCHAR(32) NOT NULL COMMENT '收货人手机号快照',
  `receiver_address` VARCHAR(500) NOT NULL COMMENT '收货地址快照',
  `pay_time` DATETIME NULL COMMENT '支付时间',
  `finish_time` DATETIME NULL COMMENT '完成时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status_deleted` (`user_id`, `order_status`, `is_deleted`),
  KEY `idx_user_mobile` (`user_mobile`),
  KEY `idx_status_created` (`order_status`, `created_at`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='商城订单冗余字段示例表';

CREATE TABLE `mall_order_item_redundant` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号，冗余字段',
  `spu_id` BIGINT NOT NULL COMMENT 'SPU ID',
  `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
  `spu_code` VARCHAR(64) NULL COMMENT 'SPU编码快照',
  `sku_code` VARCHAR(64) NULL COMMENT 'SKU编码快照',
  `spu_name` VARCHAR(200) NOT NULL COMMENT '商品名称快照',
  `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称快照',
  `sku_image_url` VARCHAR(500) NULL COMMENT 'SKU图片快照',
  `spec_json` JSON NULL COMMENT '规格信息快照',
  `category_id` BIGINT NULL COMMENT '分类ID快照',
  `category_name` VARCHAR(128) NULL COMMENT '分类名称快照',
  `brand_id` BIGINT NULL COMMENT '品牌ID快照',
  `brand_name` VARCHAR(128) NULL COMMENT '品牌名称快照',
  `unit_price` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '商品单价快照',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '明细总金额',
  `discount_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '明细优惠金额',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '明细实付金额',
  `item_status` TINYINT NOT NULL DEFAULT 0 COMMENT '明细状态：0正常，1退款中，2已退款',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id_deleted` (`order_id`, `is_deleted`),
  KEY `idx_order_no_deleted` (`order_no`, `is_deleted`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_brand_id` (`brand_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='商城订单明细冗余字段示例表';
```

### 字段设计说明

冗余字段应围绕高频查询和历史快照设计。订单中的用户昵称、手机号、收货地址，以及订单明细中的商品名称、规格、价格，都应该保存业务发生当时的值，避免后续用户资料或商品资料变化影响历史订单。

| 表                          | 字段            | 说明                        |
| --------------------------- | --------------- | --------------------------- |
| `mall_order_redundant`      | `user_id`       | 用户 ID，仍然保留主数据关联 |
| `mall_order_redundant`      | `user_nickname` | 用户昵称快照                |
| `mall_order_redundant`      | `user_mobile`   | 用户手机号快照              |
| `mall_order_redundant`      | `receiver_name` | 收货人姓名快照              |
| `mall_order_item_redundant` | `spu_id`        | 商品 SPU ID                 |
| `mall_order_item_redundant` | `sku_id`        | 商品 SKU ID                 |
| `mall_order_item_redundant` | `spu_name`      | 商品名称快照                |
| `mall_order_item_redundant` | `sku_name`      | SKU 名称快照                |
| `mall_order_item_redundant` | `category_name` | 分类名称快照                |
| `mall_order_item_redundant` | `brand_name`    | 品牌名称快照                |
| `mall_order_item_redundant` | `unit_price`    | 下单时商品单价快照          |

### 新增订单主表数据

下面的 SQL 用于创建订单时写入用户相关冗余字段。实际业务中，这些字段通常从用户表和地址表查询后写入订单表。

```sql
INSERT INTO `mall_order_redundant` (
  `id`,
  `order_no`,
  `user_id`,
  `user_no`,
  `user_nickname`,
  `user_mobile`,
  `order_status`,
  `pay_status`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `receiver_name`,
  `receiver_phone`,
  `receiver_address`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  'ORDER_202605110001',
  200001,
  'USER_202605110001',
  '张三',
  '13800000000',
  0,
  0,
  399.00,
  30.00,
  369.00,
  '张三',
  '13800000000',
  '北京市朝阳区示例路 100 号',
  '订单冗余用户和收货信息',
  200001,
  200001
);
```

### 新增订单明细数据

下面的 SQL 用于创建订单明细时写入商品相关冗余字段。商品名称、规格、品牌、分类和价格应保存下单时快照。

```sql
INSERT INTO `mall_order_item_redundant` (
  `id`,
  `order_id`,
  `order_no`,
  `spu_id`,
  `sku_id`,
  `spu_code`,
  `sku_code`,
  `spu_name`,
  `sku_name`,
  `sku_image_url`,
  `spec_json`,
  `category_id`,
  `category_name`,
  `brand_id`,
  `brand_name`,
  `unit_price`,
  `quantity`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `item_status`
) VALUES (
  110001,
  100001,
  'ORDER_202605110001',
  300001,
  310001,
  'SPU_202605110001',
  'SKU_202605110001',
  '机械键盘',
  '机械键盘 黑色 青轴',
  'https://example.com/product/keyboard.png',
  JSON_OBJECT('color', '黑色', 'switch', '青轴'),
  400001,
  '数码外设',
  500001,
  '示例品牌',
  399.00,
  1,
  399.00,
  30.00,
  369.00,
  0
);
```

### 查询订单列表

下面的 SQL 用于查询订单列表。由于订单表已经冗余用户昵称和手机号，列表查询不需要再关联用户表。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `user_nickname`,
  `user_mobile`,
  `order_status`,
  `pay_status`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `receiver_name`,
  `receiver_phone`,
  `created_at`
FROM `mall_order_redundant`
WHERE `is_deleted` = 0
  AND `order_status` = 0
ORDER BY `created_at` DESC
LIMIT 10 OFFSET 0;
```

### 查询订单明细

下面的 SQL 用于查询订单明细。由于订单明细已经保存商品快照，不需要关联商品、分类、品牌表即可展示历史订单商品信息。

```sql
SELECT
  `id`,
  `order_id`,
  `order_no`,
  `spu_id`,
  `sku_id`,
  `spu_name`,
  `sku_name`,
  `sku_image_url`,
  `spec_json`,
  `category_name`,
  `brand_name`,
  `unit_price`,
  `quantity`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `item_status`
FROM `mall_order_item_redundant`
WHERE `order_id` = 100001
  AND `is_deleted` = 0
ORDER BY `id` ASC;
```

### 根据冗余字段查询订单

下面的 SQL 用于根据冗余的用户手机号查询订单，适合客服后台快速检索。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `user_nickname`,
  `user_mobile`,
  `order_status`,
  `pay_amount`,
  `created_at`
FROM `mall_order_redundant`
WHERE `user_mobile` = '13800000000'
  AND `is_deleted` = 0
ORDER BY `created_at` DESC
LIMIT 20 OFFSET 0;
```

### 根据商品分类统计订单明细

下面的 SQL 用于根据冗余的分类信息统计销售情况。因为明细表已经保存 `category_id` 和 `category_name`，可以减少商品分类表关联。

```sql
SELECT
  `category_id`,
  `category_name`,
  SUM(`quantity`) AS `sale_quantity`,
  SUM(`pay_amount`) AS `sale_amount`
FROM `mall_order_item_redundant`
WHERE `is_deleted` = 0
  AND `item_status` = 0
GROUP BY
  `category_id`,
  `category_name`
ORDER BY `sale_amount` DESC;
```

### 根据品牌统计订单明细

下面的 SQL 用于根据冗余的品牌信息统计销售金额。

```sql
SELECT
  `brand_id`,
  `brand_name`,
  COUNT(*) AS `item_count`,
  SUM(`quantity`) AS `sale_quantity`,
  SUM(`pay_amount`) AS `sale_amount`
FROM `mall_order_item_redundant`
WHERE `is_deleted` = 0
  AND `item_status` = 0
GROUP BY
  `brand_id`,
  `brand_name`
ORDER BY `sale_amount` DESC;
```

### 更新冗余展示字段

下面的 SQL 用于同步更新非快照类冗余字段。例如用户昵称如果用于当前展示，可以在用户修改昵称后同步到订单表。历史快照类字段不建议随意同步更新。

```sql
UPDATE `mall_order_redundant`
SET
  `user_nickname` = '张三丰',
  `updated_by` = 1
WHERE `user_id` = 200001
  AND `is_deleted` = 0;
```

### 回填冗余字段

下面的 SQL 用于历史数据补充冗余字段。适合系统改造时，从主数据表回填订单表中的用户编号和昵称。

```sql
UPDATE `mall_order_redundant` o
JOIN `sys_rbac_user` u
  ON o.`user_id` = u.`id`
  AND u.`is_deleted` = 0
SET
  o.`user_no` = u.`user_no`,
  o.`user_nickname` = u.`nickname`,
  o.`user_mobile` = u.`mobile`,
  o.`updated_by` = 1
WHERE o.`is_deleted` = 0
  AND (
    o.`user_no` IS NULL
    OR o.`user_nickname` IS NULL
    OR o.`user_mobile` IS NULL
  );
```

### 校验冗余字段一致性

下面的 SQL 用于检查订单表中用户昵称冗余字段和用户表当前昵称是否不一致。是否需要修复取决于该字段是“当前展示字段”还是“历史快照字段”。

```sql
SELECT
  o.`id`,
  o.`order_no`,
  o.`user_id`,
  o.`user_nickname` AS `order_user_nickname`,
  u.`nickname` AS `current_user_nickname`,
  o.`created_at`
FROM `mall_order_redundant` o
JOIN `sys_rbac_user` u
  ON o.`user_id` = u.`id`
  AND u.`is_deleted` = 0
WHERE o.`is_deleted` = 0
  AND o.`user_nickname` <> u.`nickname`;
```

### 校验商品快照字段

下面的 SQL 用于检查订单明细中的商品名称快照和商品表当前名称是否不一致。订单明细中的商品名称通常是历史快照，不一定需要修复。

```sql
SELECT
  i.`id`,
  i.`order_no`,
  i.`spu_id`,
  i.`spu_name` AS `order_spu_name`,
  p.`product_name` AS `current_product_name`,
  i.`created_at`
FROM `mall_order_item_redundant` i
JOIN `mall_product_spu` p
  ON i.`spu_id` = p.`id`
  AND p.`is_deleted` = 0
WHERE i.`is_deleted` = 0
  AND i.`spu_name` <> p.`product_name`;
```

### 修复当前展示型冗余字段

下面的 SQL 用于修复当前展示型冗余字段。例如后台订单列表希望展示用户当前昵称，而不是下单时昵称，可以使用该方式同步。

```sql
UPDATE `mall_order_redundant` o
JOIN `sys_rbac_user` u
  ON o.`user_id` = u.`id`
  AND u.`is_deleted` = 0
SET
  o.`user_nickname` = u.`nickname`,
  o.`user_mobile` = u.`mobile`,
  o.`updated_by` = 1
WHERE o.`is_deleted` = 0
  AND (
    o.`user_nickname` <> u.`nickname`
    OR o.`user_mobile` <> u.`mobile`
  );
```

### 查询冗余字段为空的数据

下面的 SQL 用于查询冗余字段缺失的数据，适合数据治理和历史数据修复。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `user_no`,
  `user_nickname`,
  `user_mobile`,
  `created_at`
FROM `mall_order_redundant`
WHERE `is_deleted` = 0
  AND (
    `user_no` IS NULL
    OR `user_nickname` IS NULL
    OR `user_mobile` IS NULL
  )
ORDER BY `created_at` DESC;
```

### 索引设计建议

冗余字段模型的索引应围绕真实查询场景设计。不是所有冗余字段都需要建索引，只有用于筛选、排序、统计的字段才需要考虑索引。

| 表                          | 索引                                                         | 适用场景           |
| --------------------------- | ------------------------------------------------------------ | ------------------ |
| `mall_order_redundant`      | `PRIMARY KEY (id)`                                           | 查询订单详情       |
| `mall_order_redundant`      | `uk_order_no (order_no)`                                     | 根据订单编号查询   |
| `mall_order_redundant`      | `idx_user_status_deleted (user_id, order_status, is_deleted)` | 查询用户订单       |
| `mall_order_redundant`      | `idx_user_mobile (user_mobile)`                              | 根据手机号检索订单 |
| `mall_order_redundant`      | `idx_status_created (order_status, created_at)`              | 后台订单分页查询   |
| `mall_order_item_redundant` | `idx_order_id_deleted (order_id, is_deleted)`                | 查询订单明细       |
| `mall_order_item_redundant` | `idx_sku_id (sku_id)`                                        | SKU 维度统计       |
| `mall_order_item_redundant` | `idx_category_id (category_id)`                              | 分类维度统计       |
| `mall_order_item_redundant` | `idx_brand_id (brand_id)`                                    | 品牌维度统计       |

### 使用建议

冗余字段模型的核心是明确字段性质：有些字段是历史快照，有些字段是当前展示加速字段。历史快照不应随主数据变化而更新，当前展示字段可以通过同步任务或事件机制更新。

使用冗余字段模型时重点注意：

- 冗余字段不能替代主数据表
- 冗余字段应有明确用途，不要盲目冗余
- 历史快照字段不建议随主数据变化同步更新
- 当前展示型冗余字段可以异步同步
- 冗余字段会带来数据一致性维护成本
- 高频列表查询字段适合适当冗余
- 订单、流水、审计等历史业务适合保存快照字段
- 冗余字段变更应有回填和校验 SQL
- 不要对所有冗余字段都建立索引
- 冗余字段适合优化查询，不适合承载核心业务规则

## 宽表模型

宽表模型用于把多个业务维度、统计指标或展示字段集中到一张表中，减少查询时的多表关联。它通常服务于列表页、报表页、搜索页、数据看板或离线分析场景。宽表中的很多字段来自其他业务表，通常通过业务事件、定时任务、ETL 或异步任务生成。

下面以“订单统计宽表”为例说明宽表模型的建表方式和常用 SQL 实践。

### 适用场景

宽表模型适合查询字段多、关联表多、读多写少、对查询性能要求较高的场景。宽表通常不是核心交易表，而是面向查询、统计、展示和分析的派生表。

常见场景包括：

- 订单列表宽表
- 用户画像宽表
- 商品搜索宽表
- 商品销售统计宽表
- 客户分析宽表
- 会员权益宽表
- 工单看板宽表
- 运营报表宽表
- 财务对账宽表
- 数据大屏宽表

### 建表 SQL

下面的 SQL 创建订单统计宽表。该表集中保存订单、用户、商品、金额、支付、物流和统计相关字段，适合后台订单报表和运营分析查询。

```sql
CREATE TABLE `report_order_wide` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `user_no` VARCHAR(64) NULL COMMENT '用户编号',
  `user_nickname` VARCHAR(128) NULL COMMENT '用户昵称',
  `user_mobile` VARCHAR(32) NULL COMMENT '用户手机号',
  `order_status` TINYINT NOT NULL COMMENT '订单状态',
  `pay_status` TINYINT NOT NULL COMMENT '支付状态',
  `pay_type` TINYINT NULL COMMENT '支付方式',
  `order_source` TINYINT NULL COMMENT '订单来源',
  `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  `discount_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `freight_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '运费金额',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  `refund_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `item_count` INT NOT NULL DEFAULT 0 COMMENT '订单明细数量',
  `product_quantity` INT NOT NULL DEFAULT 0 COMMENT '商品购买总数量',
  `first_spu_id` BIGINT NULL COMMENT '首个SPU ID',
  `first_sku_id` BIGINT NULL COMMENT '首个SKU ID',
  `first_product_name` VARCHAR(200) NULL COMMENT '首个商品名称',
  `category_ids` VARCHAR(500) NULL COMMENT '分类ID集合，逗号分隔，仅用于展示或低频筛选',
  `category_names` VARCHAR(1000) NULL COMMENT '分类名称集合，逗号分隔',
  `brand_ids` VARCHAR(500) NULL COMMENT '品牌ID集合，逗号分隔',
  `brand_names` VARCHAR(1000) NULL COMMENT '品牌名称集合，逗号分隔',
  `receiver_name` VARCHAR(64) NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(32) NULL COMMENT '收货人手机号',
  `receiver_province` VARCHAR(64) NULL COMMENT '收货省份',
  `receiver_city` VARCHAR(64) NULL COMMENT '收货城市',
  `receiver_area` VARCHAR(64) NULL COMMENT '收货区县',
  `receiver_address` VARCHAR(500) NULL COMMENT '收货详细地址',
  `created_time` DATETIME NOT NULL COMMENT '下单时间',
  `pay_time` DATETIME NULL COMMENT '支付时间',
  `delivery_time` DATETIME NULL COMMENT '发货时间',
  `finish_time` DATETIME NULL COMMENT '完成时间',
  `cancel_time` DATETIME NULL COMMENT '取消时间',
  `stat_date` DATE NOT NULL COMMENT '统计日期，通常取下单日期',
  `sync_status` TINYINT NOT NULL DEFAULT 1 COMMENT '同步状态：0待同步，1已同步，2同步失败',
  `sync_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_time` (`user_id`, `created_time`),
  KEY `idx_status_time` (`order_status`, `created_time`),
  KEY `idx_pay_status_time` (`pay_status`, `pay_time`),
  KEY `idx_stat_date` (`stat_date`),
  KEY `idx_receiver_city` (`receiver_province`, `receiver_city`),
  KEY `idx_sync_status_time` (`sync_status`, `sync_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='订单统计宽表';
```

### 字段设计说明

宽表中的字段通常来自多个源表。订单宽表会从订单主表、订单明细表、用户表、商品表、分类表、品牌表等来源聚合字段，最终形成一张面向查询的表。

| 字段类型 | 示例字段                                              | 说明                   |
| -------- | ----------------------------------------------------- | ---------------------- |
| 主体字段 | `order_id`、`order_no`                                | 关联核心业务对象       |
| 用户字段 | `user_id`、`user_nickname`、`user_mobile`             | 来自用户表或订单快照   |
| 订单字段 | `order_status`、`pay_status`、`pay_amount`            | 来自订单主表           |
| 商品字段 | `first_product_name`、`category_names`、`brand_names` | 来自订单明细和商品维度 |
| 地址字段 | `receiver_province`、`receiver_city`                  | 来自订单收货信息       |
| 时间字段 | `created_time`、`pay_time`、`stat_date`               | 用于筛选、排序、统计   |
| 同步字段 | `sync_status`、`sync_time`                            | 用于控制宽表同步状态   |

### 初始化写入宽表

下面的 SQL 用于从订单主表和订单明细表初始化写入订单宽表。实际业务中可以由定时任务、消息消费或离线任务执行。

```sql
INSERT INTO `report_order_wide` (
  `id`,
  `order_id`,
  `order_no`,
  `user_id`,
  `user_no`,
  `user_nickname`,
  `user_mobile`,
  `order_status`,
  `pay_status`,
  `pay_type`,
  `order_source`,
  `total_amount`,
  `discount_amount`,
  `freight_amount`,
  `pay_amount`,
  `refund_amount`,
  `item_count`,
  `product_quantity`,
  `first_spu_id`,
  `first_sku_id`,
  `first_product_name`,
  `category_ids`,
  `category_names`,
  `brand_ids`,
  `brand_names`,
  `receiver_name`,
  `receiver_phone`,
  `receiver_address`,
  `created_time`,
  `pay_time`,
  `finish_time`,
  `stat_date`,
  `sync_status`,
  `sync_time`
)
SELECT
  o.`id` + 900000000000 AS `id`,
  o.`id` AS `order_id`,
  o.`order_no`,
  o.`user_id`,
  o.`user_no`,
  o.`user_nickname`,
  o.`user_mobile`,
  o.`order_status`,
  o.`pay_status`,
  o.`pay_type`,
  o.`order_source`,
  o.`total_amount`,
  o.`discount_amount`,
  0.00 AS `freight_amount`,
  o.`pay_amount`,
  IFNULL(SUM(i.`refund_amount`), 0.00) AS `refund_amount`,
  COUNT(i.`id`) AS `item_count`,
  IFNULL(SUM(i.`quantity`), 0) AS `product_quantity`,
  MIN(i.`spu_id`) AS `first_spu_id`,
  MIN(i.`sku_id`) AS `first_sku_id`,
  SUBSTRING_INDEX(GROUP_CONCAT(i.`spu_name` ORDER BY i.`id` ASC SEPARATOR ','), ',', 1) AS `first_product_name`,
  GROUP_CONCAT(DISTINCT i.`category_id` ORDER BY i.`category_id` ASC SEPARATOR ',') AS `category_ids`,
  GROUP_CONCAT(DISTINCT i.`category_name` ORDER BY i.`category_name` ASC SEPARATOR ',') AS `category_names`,
  GROUP_CONCAT(DISTINCT i.`brand_id` ORDER BY i.`brand_id` ASC SEPARATOR ',') AS `brand_ids`,
  GROUP_CONCAT(DISTINCT i.`brand_name` ORDER BY i.`brand_name` ASC SEPARATOR ',') AS `brand_names`,
  o.`receiver_name`,
  o.`receiver_phone`,
  o.`receiver_address`,
  o.`created_at` AS `created_time`,
  o.`pay_time`,
  o.`finish_time`,
  DATE(o.`created_at`) AS `stat_date`,
  1 AS `sync_status`,
  NOW() AS `sync_time`
FROM `mall_order_redundant` o
LEFT JOIN `mall_order_item_redundant` i
  ON o.`id` = i.`order_id`
  AND i.`is_deleted` = 0
WHERE o.`is_deleted` = 0
GROUP BY
  o.`id`,
  o.`order_no`,
  o.`user_id`,
  o.`user_no`,
  o.`user_nickname`,
  o.`user_mobile`,
  o.`order_status`,
  o.`pay_status`,
  o.`pay_type`,
  o.`order_source`,
  o.`total_amount`,
  o.`discount_amount`,
  o.`pay_amount`,
  o.`receiver_name`,
  o.`receiver_phone`,
  o.`receiver_address`,
  o.`created_at`,
  o.`pay_time`,
  o.`finish_time`;
```

### 使用 UPSERT 同步宽表

下面的 SQL 用于增量同步宽表。如果订单已经存在，则更新宽表字段；如果不存在，则插入新数据。

```sql
INSERT INTO `report_order_wide` (
  `id`,
  `order_id`,
  `order_no`,
  `user_id`,
  `user_nickname`,
  `user_mobile`,
  `order_status`,
  `pay_status`,
  `pay_type`,
  `order_source`,
  `total_amount`,
  `discount_amount`,
  `freight_amount`,
  `pay_amount`,
  `item_count`,
  `product_quantity`,
  `receiver_name`,
  `receiver_phone`,
  `receiver_address`,
  `created_time`,
  `pay_time`,
  `stat_date`,
  `sync_status`,
  `sync_time`
) VALUES (
  900000100001,
  100001,
  'ORDER_202605110001',
  200001,
  '张三',
  '13800000000',
  1,
  1,
  2,
  2,
  399.00,
  30.00,
  0.00,
  369.00,
  1,
  1,
  '张三',
  '13800000000',
  '北京市朝阳区示例路 100 号',
  '2026-05-11 10:00:00',
  NOW(),
  '2026-05-11',
  1,
  NOW()
)
ON DUPLICATE KEY UPDATE
  `user_nickname` = VALUES(`user_nickname`),
  `user_mobile` = VALUES(`user_mobile`),
  `order_status` = VALUES(`order_status`),
  `pay_status` = VALUES(`pay_status`),
  `pay_type` = VALUES(`pay_type`),
  `total_amount` = VALUES(`total_amount`),
  `discount_amount` = VALUES(`discount_amount`),
  `freight_amount` = VALUES(`freight_amount`),
  `pay_amount` = VALUES(`pay_amount`),
  `item_count` = VALUES(`item_count`),
  `product_quantity` = VALUES(`product_quantity`),
  `pay_time` = VALUES(`pay_time`),
  `sync_status` = 1,
  `sync_time` = NOW();
```

### 查询订单宽表列表

下面的 SQL 用于后台订单列表查询。宽表已经聚合了用户、金额、商品摘要等字段，不需要再关联多张业务表。

```sql
SELECT
  `order_id`,
  `order_no`,
  `user_id`,
  `user_nickname`,
  `user_mobile`,
  `order_status`,
  `pay_status`,
  `pay_type`,
  `pay_amount`,
  `item_count`,
  `product_quantity`,
  `first_product_name`,
  `receiver_name`,
  `receiver_phone`,
  `created_time`,
  `pay_time`
FROM `report_order_wide`
WHERE `is_deleted` = 0
  AND `order_status` = 1
ORDER BY `created_time` DESC
LIMIT 20 OFFSET 0;
```

### 按用户查询订单宽表

下面的 SQL 用于查询某个用户的订单统计视图。

```sql
SELECT
  `order_no`,
  `order_status`,
  `pay_status`,
  `pay_amount`,
  `item_count`,
  `product_quantity`,
  `first_product_name`,
  `created_time`
FROM `report_order_wide`
WHERE `user_id` = 200001
  AND `is_deleted` = 0
ORDER BY `created_time` DESC
LIMIT 20 OFFSET 0;
```

### 按日期统计订单金额

下面的 SQL 用于按日期统计订单数和实付金额，适合运营日报和数据看板。

```sql
SELECT
  `stat_date`,
  COUNT(*) AS `order_count`,
  SUM(`pay_amount`) AS `pay_amount`,
  SUM(`refund_amount`) AS `refund_amount`,
  SUM(`pay_amount` - `refund_amount`) AS `net_amount`
FROM `report_order_wide`
WHERE `stat_date` >= '2026-05-01'
  AND `stat_date` < '2026-06-01'
  AND `pay_status` = 1
  AND `is_deleted` = 0
GROUP BY `stat_date`
ORDER BY `stat_date` ASC;
```

### 按城市统计订单

下面的 SQL 用于按收货城市统计订单数量和金额。

```sql
SELECT
  `receiver_province`,
  `receiver_city`,
  COUNT(*) AS `order_count`,
  SUM(`pay_amount`) AS `pay_amount`
FROM `report_order_wide`
WHERE `pay_status` = 1
  AND `is_deleted` = 0
GROUP BY
  `receiver_province`,
  `receiver_city`
ORDER BY `pay_amount` DESC;
```

### 查询支付订单

下面的 SQL 用于查询已支付订单宽表数据。

```sql
SELECT
  `order_no`,
  `user_nickname`,
  `user_mobile`,
  `pay_type`,
  `pay_amount`,
  `pay_time`,
  `first_product_name`
FROM `report_order_wide`
WHERE `pay_status` = 1
  AND `pay_time` >= '2026-05-01 00:00:00'
  AND `pay_time` < '2026-06-01 00:00:00'
  AND `is_deleted` = 0
ORDER BY `pay_time` DESC
LIMIT 20 OFFSET 0;
```

### 查询同步失败数据

下面的 SQL 用于查询宽表同步失败的数据，适合任务重试和异常排查。

```sql
SELECT
  `id`,
  `order_id`,
  `order_no`,
  `sync_status`,
  `sync_time`,
  `updated_at`
FROM `report_order_wide`
WHERE `sync_status` = 2
ORDER BY `sync_time` DESC
LIMIT 50 OFFSET 0;
```

### 标记同步失败

下面的 SQL 用于宽表同步异常时标记失败状态。

```sql
UPDATE `report_order_wide`
SET
  `sync_status` = 2,
  `sync_time` = NOW()
WHERE `order_id` = 100001;
```

### 重新同步指定订单宽表

下面的 SQL 用于重新同步指定订单的宽表数据。实际业务中通常由任务程序重新聚合后写入。

```sql
UPDATE `report_order_wide` w
JOIN `mall_order_redundant` o
  ON w.`order_id` = o.`id`
  AND o.`is_deleted` = 0
SET
  w.`user_nickname` = o.`user_nickname`,
  w.`user_mobile` = o.`user_mobile`,
  w.`order_status` = o.`order_status`,
  w.`pay_status` = o.`pay_status`,
  w.`pay_amount` = o.`pay_amount`,
  w.`pay_time` = o.`pay_time`,
  w.`finish_time` = o.`finish_time`,
  w.`sync_status` = 1,
  w.`sync_time` = NOW()
WHERE w.`order_id` = 100001;
```

### 校验宽表和源表金额一致性

下面的 SQL 用于检查订单宽表和订单源表金额是否一致。

```sql
SELECT
  w.`order_id`,
  w.`order_no`,
  w.`pay_amount` AS `wide_pay_amount`,
  o.`pay_amount` AS `source_pay_amount`,
  w.`sync_time`
FROM `report_order_wide` w
JOIN `mall_order_redundant` o
  ON w.`order_id` = o.`id`
  AND o.`is_deleted` = 0
WHERE w.`is_deleted` = 0
  AND w.`pay_amount` <> o.`pay_amount`;
```

### 校验宽表明细数量一致性

下面的 SQL 用于检查宽表中的明细数量和订单明细表实际数量是否一致。

```sql
SELECT
  w.`order_id`,
  w.`order_no`,
  w.`item_count` AS `wide_item_count`,
  t.`source_item_count`,
  w.`sync_time`
FROM `report_order_wide` w
JOIN (
  SELECT
    `order_id`,
    COUNT(*) AS `source_item_count`
  FROM `mall_order_item_redundant`
  WHERE `is_deleted` = 0
  GROUP BY `order_id`
) t ON w.`order_id` = t.`order_id`
WHERE w.`is_deleted` = 0
  AND w.`item_count` <> t.`source_item_count`;
```

### 删除源订单后同步宽表

下面的 SQL 用于源订单逻辑删除后同步宽表删除标识。

```sql
UPDATE `report_order_wide` w
JOIN `mall_order_redundant` o
  ON w.`order_id` = o.`id`
SET
  w.`is_deleted` = o.`is_deleted`,
  w.`sync_status` = 1,
  w.`sync_time` = NOW()
WHERE o.`id` = 100001;
```

### 清理历史宽表数据

下面的 SQL 用于清理历史宽表数据。生产环境通常应先归档，再清理。

```sql
DELETE FROM `report_order_wide`
WHERE `stat_date` < DATE_SUB(CURDATE(), INTERVAL 2 YEAR);
```

### 索引设计建议

宽表模型的索引应围绕报表查询、列表查询、筛选条件和统计维度设计。宽表字段很多，但不代表每个字段都要建索引。

| 索引                                                   | 适用场景                 |
| ------------------------------------------------------ | ------------------------ |
| `PRIMARY KEY (id)`                                     | 查询宽表详情             |
| `uk_order_id (order_id)`                               | 根据源订单同步宽表       |
| `uk_order_no (order_no)`                               | 根据订单编号查询         |
| `idx_user_time (user_id, created_time)`                | 查询用户订单             |
| `idx_status_time (order_status, created_time)`         | 按订单状态分页           |
| `idx_pay_status_time (pay_status, pay_time)`           | 查询支付订单             |
| `idx_stat_date (stat_date)`                            | 按统计日期报表查询       |
| `idx_receiver_city (receiver_province, receiver_city)` | 按地区统计订单           |
| `idx_sync_status_time (sync_status, sync_time)`        | 查询同步失败或待同步数据 |

### 使用建议

宽表模型的核心是用空间换查询效率。它适合面向查询和统计，不适合作为核心交易源表。宽表数据通常允许存在短暂延迟，但必须具备校验、重建和补偿能力。

使用宽表模型时重点注意：

- 宽表通常是派生表，不是核心业务源表
- 宽表字段来自多个源表，需要明确数据来源
- 宽表适合查询和统计，不适合承载交易写入
- 宽表数据可以通过消息、任务或 ETL 同步
- 宽表允许短暂延迟，但需要支持重试和补偿
- 宽表必须能根据源表重新生成
- 字段很多不代表索引很多，索引应围绕查询场景设计
- 多值字段如 `category_ids` 适合展示，不适合高频精确筛选
- 数据一致性需要定期校验
- 数据量较大时建议按日期分区或归档
- 宽表可以明显降低复杂列表页和报表页的查询成本

## JSON扩展字段模型

JSON扩展字段模型用于保存结构不稳定、低频访问、非核心查询条件的扩展属性。它适合在不频繁调整表结构的前提下，为业务对象补充灵活字段。MySQL 8 原生支持 `JSON` 类型，并提供 `JSON_EXTRACT`、`JSON_SET`、`JSON_REMOVE`、`JSON_CONTAINS` 等函数。

下面以“客户表”为例说明 JSON扩展字段模型的建表方式和常用 SQL 实践。

### 适用场景

JSON扩展字段模型适合字段变化频繁、不同业务类型字段差异较大、但这些字段又不是核心查询条件的场景。

常见场景包括：

- 用户扩展资料
- 客户画像扩展信息
- 商品扩展属性
- 表单扩展字段
- 配置扩展参数
- 第三方接口返回数据
- 设备扩展属性
- 活动扩展规则
- 内容扩展元数据
- 临时业务扩展字段

### 建表 SQL

下面的 SQL 创建客户表。表中使用 `extend_json` 保存客户扩展信息，同时使用生成列提取常用 JSON 字段，便于建立索引和高频查询。

```sql
CREATE TABLE `crm_customer_json` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `customer_no` VARCHAR(64) NOT NULL COMMENT '客户编号',
  `customer_name` VARCHAR(128) NOT NULL COMMENT '客户名称',
  `customer_type` TINYINT NOT NULL DEFAULT 1 COMMENT '客户类型：1个人，2企业',
  `customer_status` TINYINT NOT NULL DEFAULT 1 COMMENT '客户状态：0禁用，1启用',
  `mobile` VARCHAR(32) NULL COMMENT '手机号',
  `email` VARCHAR(128) NULL COMMENT '邮箱',
  `extend_json` JSON NULL COMMENT '扩展信息JSON',
  `extend_source` VARCHAR(64) GENERATED ALWAYS AS (
    JSON_UNQUOTE(JSON_EXTRACT(`extend_json`, '$.source'))
  ) STORED COMMENT '客户来源，来自extend_json.source',
  `extend_level` VARCHAR(64) GENERATED ALWAYS AS (
    JSON_UNQUOTE(JSON_EXTRACT(`extend_json`, '$.level'))
  ) STORED COMMENT '客户等级，来自extend_json.level',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_no` (`customer_no`),
  KEY `idx_status_deleted` (`customer_status`, `is_deleted`),
  KEY `idx_mobile` (`mobile`),
  KEY `idx_extend_source` (`extend_source`),
  KEY `idx_extend_level` (`extend_level`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='CRM客户JSON扩展字段表';
```

### 字段设计说明

JSON扩展字段模型的核心是将稳定字段和扩展字段分开。稳定、高频、强约束字段应设计为普通列；不稳定、低频、弱约束字段可以放入 JSON 字段。

| 字段            | 说明                           |
| --------------- | ------------------------------ |
| `customer_no`   | 客户编号，稳定业务字段         |
| `customer_name` | 客户名称，稳定业务字段         |
| `customer_type` | 客户类型，稳定业务字段         |
| `extend_json`   | JSON 扩展字段                  |
| `extend_source` | 从 JSON 中提取的客户来源生成列 |
| `extend_level`  | 从 JSON 中提取的客户等级生成列 |
| `is_deleted`    | 逻辑删除字段                   |

### 新增 JSON 数据

下面的 SQL 用于新增客户数据，并写入 JSON 扩展信息。

```sql
INSERT INTO `crm_customer_json` (
  `id`,
  `customer_no`,
  `customer_name`,
  `customer_type`,
  `customer_status`,
  `mobile`,
  `email`,
  `extend_json`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  'CUS_202605110001',
  '张三',
  1,
  1,
  '13800000000',
  'zhangsan@example.com',
  JSON_OBJECT(
    'source', 'wechat',
    'level', 'vip',
    'tags', JSON_ARRAY('高意向', '已成交'),
    'profile', JSON_OBJECT(
      'age', 30,
      'city', '北京',
      'industry', '互联网'
    )
  ),
  'JSON扩展字段示例客户',
  1,
  1
);
```

### 查询完整 JSON 字段

下面的 SQL 用于查询客户扩展 JSON 信息。

```sql
SELECT
  `id`,
  `customer_no`,
  `customer_name`,
  `extend_json`
FROM `crm_customer_json`
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 查询 JSON 指定属性

下面的 SQL 用于从 JSON 字段中提取客户来源、客户等级和所在城市。

```sql
SELECT
  `id`,
  `customer_no`,
  `customer_name`,
  JSON_UNQUOTE(JSON_EXTRACT(`extend_json`, '$.source')) AS `source`,
  JSON_UNQUOTE(JSON_EXTRACT(`extend_json`, '$.level')) AS `level`,
  JSON_UNQUOTE(JSON_EXTRACT(`extend_json`, '$.profile.city')) AS `city`
FROM `crm_customer_json`
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 使用简写操作符查询 JSON

下面的 SQL 使用 MySQL 8 的 JSON 简写操作符查询扩展字段。`->` 返回 JSON 值，`->>` 返回去除引号后的字符串值。

```sql
SELECT
  `id`,
  `customer_no`,
  `customer_name`,
  `extend_json` ->> '$.source' AS `source`,
  `extend_json` ->> '$.level' AS `level`,
  `extend_json` ->> '$.profile.industry' AS `industry`
FROM `crm_customer_json`
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 根据 JSON 字段筛选

下面的 SQL 用于根据 JSON 中的客户来源和客户等级筛选客户。低频查询可以直接使用 JSON 函数，高频查询建议使用生成列。

```sql
SELECT
  `id`,
  `customer_no`,
  `customer_name`,
  `mobile`,
  `extend_json` ->> '$.source' AS `source`,
  `extend_json` ->> '$.level' AS `level`
FROM `crm_customer_json`
WHERE JSON_UNQUOTE(JSON_EXTRACT(`extend_json`, '$.source')) = 'wechat'
  AND JSON_UNQUOTE(JSON_EXTRACT(`extend_json`, '$.level')) = 'vip'
  AND `is_deleted` = 0
ORDER BY `created_at` DESC
LIMIT 10 OFFSET 0;
```

### 根据生成列筛选

下面的 SQL 用于根据生成列筛选 JSON 中的客户来源和客户等级。该方式可以使用普通索引，适合高频查询。

```sql
SELECT
  `id`,
  `customer_no`,
  `customer_name`,
  `mobile`,
  `extend_source`,
  `extend_level`
FROM `crm_customer_json`
WHERE `extend_source` = 'wechat'
  AND `extend_level` = 'vip'
  AND `is_deleted` = 0
ORDER BY `created_at` DESC
LIMIT 10 OFFSET 0;
```

### 查询 JSON 数组包含值

下面的 SQL 用于查询扩展标签中包含“高意向”的客户。

```sql
SELECT
  `id`,
  `customer_no`,
  `customer_name`,
  `extend_json` -> '$.tags' AS `tags`
FROM `crm_customer_json`
WHERE JSON_CONTAINS(`extend_json` -> '$.tags', JSON_QUOTE('高意向'))
  AND `is_deleted` = 0;
```

### 更新 JSON 指定属性

下面的 SQL 用于更新 JSON 中的客户等级，不会覆盖整个 JSON 字段。

```sql
UPDATE `crm_customer_json`
SET
  `extend_json` = JSON_SET(
    COALESCE(`extend_json`, JSON_OBJECT()),
    '$.level',
    'svip'
  ),
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 新增 JSON 嵌套属性

下面的 SQL 用于在 JSON 中新增嵌套字段，例如客户画像中的职业信息。

```sql
UPDATE `crm_customer_json`
SET
  `extend_json` = JSON_SET(
    COALESCE(`extend_json`, JSON_OBJECT()),
    '$.profile.occupation',
    'Java开发工程师'
  ),
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 追加 JSON 数组元素

下面的 SQL 用于向 JSON 数组中追加标签。

```sql
UPDATE `crm_customer_json`
SET
  `extend_json` = JSON_ARRAY_APPEND(
    COALESCE(`extend_json`, JSON_OBJECT('tags', JSON_ARRAY())),
    '$.tags',
    '重点跟进'
  ),
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 删除 JSON 指定属性

下面的 SQL 用于删除 JSON 中的某个属性。

```sql
UPDATE `crm_customer_json`
SET
  `extend_json` = JSON_REMOVE(
    `extend_json`,
    '$.profile.occupation'
  ),
  `updated_by` = 1
WHERE `id` = 100001
  AND `extend_json` IS NOT NULL
  AND `is_deleted` = 0;
```

### 合并 JSON 对象

下面的 SQL 用于把新的 JSON 对象合并到已有扩展字段中。

```sql
UPDATE `crm_customer_json`
SET
  `extend_json` = JSON_MERGE_PATCH(
    COALESCE(`extend_json`, JSON_OBJECT()),
    JSON_OBJECT(
      'level', 'vip',
      'profile', JSON_OBJECT(
        'city', '上海',
        'industry', '金融'
      )
    )
  ),
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 校验 JSON 类型

下面的 SQL 用于查询 JSON 中某个属性的类型，适合排查扩展字段结构异常。

```sql
SELECT
  `id`,
  `customer_no`,
  JSON_TYPE(JSON_EXTRACT(`extend_json`, '$.tags')) AS `tags_type`,
  JSON_TYPE(JSON_EXTRACT(`extend_json`, '$.profile')) AS `profile_type`
FROM `crm_customer_json`
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 查询 JSON 属性不存在的数据

下面的 SQL 用于查询缺少指定扩展属性的数据，适合数据治理和补全。

```sql
SELECT
  `id`,
  `customer_no`,
  `customer_name`,
  `extend_json`
FROM `crm_customer_json`
WHERE JSON_EXTRACT(`extend_json`, '$.source') IS NULL
  AND `is_deleted` = 0;
```

### 回填 JSON 扩展字段

下面的 SQL 用于给历史数据补充 JSON 扩展字段。

```sql
UPDATE `crm_customer_json`
SET
  `extend_json` = JSON_SET(
    COALESCE(`extend_json`, JSON_OBJECT()),
    '$.source',
    'unknown',
    '$.level',
    'normal'
  ),
  `updated_by` = 1
WHERE `is_deleted` = 0
  AND (
    JSON_EXTRACT(`extend_json`, '$.source') IS NULL
    OR JSON_EXTRACT(`extend_json`, '$.level') IS NULL
  );
```

### 查询 JSON 字段长度

下面的 SQL 用于查询 JSON 文档大小，适合排查 JSON 字段过大的数据。

```sql
SELECT
  `id`,
  `customer_no`,
  JSON_STORAGE_SIZE(`extend_json`) AS `json_size`
FROM `crm_customer_json`
WHERE `extend_json` IS NOT NULL
  AND `is_deleted` = 0
ORDER BY `json_size` DESC
LIMIT 20;
```

### 统计 JSON 属性值

下面的 SQL 用于按 JSON 中的客户来源统计客户数量。

```sql
SELECT
  JSON_UNQUOTE(JSON_EXTRACT(`extend_json`, '$.source')) AS `source`,
  COUNT(*) AS `customer_count`
FROM `crm_customer_json`
WHERE `is_deleted` = 0
GROUP BY JSON_UNQUOTE(JSON_EXTRACT(`extend_json`, '$.source'))
ORDER BY `customer_count` DESC;
```

### 使用生成列统计 JSON 属性

下面的 SQL 用于通过生成列统计客户等级，适合高频统计场景。

```sql
SELECT
  `extend_level`,
  COUNT(*) AS `customer_count`
FROM `crm_customer_json`
WHERE `is_deleted` = 0
GROUP BY `extend_level`
ORDER BY `customer_count` DESC;
```

### 索引设计建议

JSON扩展字段模型的索引设计重点是区分低频扩展字段和高频查询字段。低频字段可以直接使用 JSON 函数查询；高频字段应提取为普通字段或生成列后建立索引。

| 索引                                               | 适用场景               |
| -------------------------------------------------- | ---------------------- |
| `PRIMARY KEY (id)`                                 | 查询客户详情           |
| `uk_customer_no (customer_no)`                     | 根据客户编号查询       |
| `idx_status_deleted (customer_status, is_deleted)` | 查询启用客户           |
| `idx_mobile (mobile)`                              | 根据手机号查询         |
| `idx_extend_source (extend_source)`                | 根据 JSON 中的来源筛选 |
| `idx_extend_level (extend_level)`                  | 根据 JSON 中的等级筛选 |

### 使用建议

JSON扩展字段模型适合增强表结构弹性，但不能滥用。核心业务字段、高频查询字段、需要强约束的字段，应优先设计为普通列。

使用 JSON扩展字段模型时重点注意：

- 稳定字段不要放入 JSON，应设计为普通列
- 高频查询字段不要直接依赖 JSON 函数，应使用普通列或生成列
- JSON 字段适合保存低频、扩展、弱约束属性
- JSON 中的字段命名要稳定，避免不同业务随意命名
- JSON 不适合替代关系表，不适合表达复杂多对多关系
- JSON 字段过大会影响查询和更新性能
- JSON 更新是对整个 JSON 文档的修改，不能等同于普通列轻量更新
- 重要 JSON 结构应在应用层做格式校验
- 需要统计和排序的字段应尽量提取为普通列
- JSON 模型适合作为扩展能力，不适合作为核心建模方式

## 多租户模型

多租户模型用于让同一套系统同时服务多个租户，并保证不同租户之间的数据隔离。MySQL 8 中最常用的业务建模方式是在业务表中增加 `tenant_id` 字段，也就是共享数据库、共享表结构、按租户字段隔离数据。

下面以“租户表”和“租户用户表”为例说明多租户模型的建表方式和常用 SQL 实践。

### 适用场景

多租户模型适合 SaaS 平台、企业服务平台、低代码平台、运营后台等需要多个客户或组织共用同一套系统的场景。

常见场景包括：

- SaaS 管理平台
- 多企业后台系统
- 多商户平台
- 多学校平台
- 多门店系统
- 多组织协同系统
- 低代码平台
- 多客户 CRM
- 多租户权限中心
- 多租户配置中心

### 建表 SQL

下面的 SQL 创建租户表和租户用户表。租户表保存租户主体信息，租户用户表通过 `tenant_id` 隔离不同租户的数据。

```sql
CREATE TABLE `sys_tenant` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tenant_code` VARCHAR(64) NOT NULL COMMENT '租户编码',
  `tenant_name` VARCHAR(128) NOT NULL COMMENT '租户名称',
  `tenant_type` TINYINT NOT NULL DEFAULT 1 COMMENT '租户类型：1企业，2商户，3个人',
  `contact_name` VARCHAR(64) NULL COMMENT '联系人姓名',
  `contact_mobile` VARCHAR(32) NULL COMMENT '联系人手机号',
  `tenant_status` TINYINT NOT NULL DEFAULT 1 COMMENT '租户状态：0停用，1启用，2冻结',
  `expire_time` DATETIME NULL COMMENT '租户到期时间',
  `max_user_count` INT NOT NULL DEFAULT 0 COMMENT '最大用户数，0表示不限制',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`),
  KEY `idx_status_deleted` (`tenant_status`, `is_deleted`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统租户表';

CREATE TABLE `sys_tenant_user` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `user_no` VARCHAR(64) NOT NULL COMMENT '用户编号',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `nickname` VARCHAR(64) NULL COMMENT '昵称',
  `mobile` VARCHAR(32) NULL COMMENT '手机号',
  `email` VARCHAR(128) NULL COMMENT '邮箱',
  `user_status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用，2冻结',
  `last_login_time` DATETIME NULL COMMENT '最后登录时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `delete_marker` BIGINT NOT NULL DEFAULT 0 COMMENT '删除标记：未删除为0，删除后写入主键ID或唯一值',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user_no_deleted` (`tenant_id`, `user_no`, `delete_marker`),
  UNIQUE KEY `uk_tenant_username_deleted` (`tenant_id`, `username`, `delete_marker`),
  KEY `idx_tenant_status_deleted` (`tenant_id`, `user_status`, `is_deleted`),
  KEY `idx_tenant_mobile` (`tenant_id`, `mobile`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统租户用户表';
```

### 字段设计说明

多租户模型的核心字段是 `tenant_id`。除租户表、平台级配置表等全局表外，大部分业务表都应包含 `tenant_id`，并且唯一索引、查询索引和更新条件都应包含租户字段。

| 表                | 字段            | 说明                  |
| ----------------- | --------------- | --------------------- |
| `sys_tenant`      | `tenant_code`   | 租户编码，全局唯一    |
| `sys_tenant`      | `tenant_name`   | 租户名称              |
| `sys_tenant`      | `tenant_status` | 租户状态              |
| `sys_tenant`      | `expire_time`   | 到期时间              |
| `sys_tenant_user` | `tenant_id`     | 租户 ID，用于数据隔离 |
| `sys_tenant_user` | `username`      | 用户名，在租户内唯一  |
| `sys_tenant_user` | `delete_marker` | 配合软删除复用唯一键  |

### 新增租户

下面的 SQL 用于新增租户主体信息。

```sql
INSERT INTO `sys_tenant` (
  `id`,
  `tenant_code`,
  `tenant_name`,
  `tenant_type`,
  `contact_name`,
  `contact_mobile`,
  `tenant_status`,
  `expire_time`,
  `max_user_count`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  'TENANT_202605110001',
  '示例科技有限公司',
  1,
  '张三',
  '13800000000',
  1,
  '2027-05-11 23:59:59',
  100,
  'SaaS租户示例',
  1,
  1
);
```

### 新增租户用户

下面的 SQL 用于给指定租户新增用户。用户名唯一性通过 `(tenant_id, username, delete_marker)` 保证，同一个用户名可以存在于不同租户下。

```sql
INSERT INTO `sys_tenant_user` (
  `id`,
  `tenant_id`,
  `user_no`,
  `username`,
  `nickname`,
  `mobile`,
  `email`,
  `user_status`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  200001,
  100001,
  'USER_202605110001',
  'admin',
  '租户管理员',
  '13800000000',
  'admin@example.com',
  1,
  '租户初始化管理员',
  1,
  1
);
```

### 查询租户详情

下面的 SQL 用于查询租户基础信息。

```sql
SELECT
  `id`,
  `tenant_code`,
  `tenant_name`,
  `tenant_type`,
  `contact_name`,
  `contact_mobile`,
  `tenant_status`,
  `expire_time`,
  `max_user_count`,
  `created_at`,
  `updated_at`
FROM `sys_tenant`
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 根据租户编码查询租户

下面的 SQL 用于根据租户编码查询租户，适合登录时识别租户或接口解析租户。

```sql
SELECT
  `id`,
  `tenant_code`,
  `tenant_name`,
  `tenant_status`,
  `expire_time`
FROM `sys_tenant`
WHERE `tenant_code` = 'TENANT_202605110001'
  AND `is_deleted` = 0;
```

### 查询租户用户列表

下面的 SQL 用于查询指定租户下的用户列表。多租户业务查询必须带上 `tenant_id` 条件。

```sql
SELECT
  `id`,
  `tenant_id`,
  `user_no`,
  `username`,
  `nickname`,
  `mobile`,
  `email`,
  `user_status`,
  `last_login_time`,
  `created_at`
FROM `sys_tenant_user`
WHERE `tenant_id` = 100001
  AND `user_status` = 1
  AND `is_deleted` = 0
ORDER BY `created_at` DESC
LIMIT 10 OFFSET 0;
```

### 根据租户和用户名查询用户

下面的 SQL 用于登录校验。用户名只在租户内唯一，因此必须同时使用 `tenant_id` 和 `username` 查询。

```sql
SELECT
  `id`,
  `tenant_id`,
  `user_no`,
  `username`,
  `nickname`,
  `mobile`,
  `email`,
  `user_status`
FROM `sys_tenant_user`
WHERE `tenant_id` = 100001
  AND `username` = 'admin'
  AND `is_deleted` = 0;
```

### 修改租户信息

下面的 SQL 用于修改租户名称、联系人、到期时间和最大用户数。

```sql
UPDATE `sys_tenant`
SET
  `tenant_name` = '示例科技集团有限公司',
  `contact_name` = '李四',
  `contact_mobile` = '13900000000',
  `expire_time` = '2028-05-11 23:59:59',
  `max_user_count` = 200,
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 修改租户用户信息

下面的 SQL 用于修改租户用户资料。更新条件必须带上 `tenant_id`，防止误更新其他租户数据。

```sql
UPDATE `sys_tenant_user`
SET
  `nickname` = '平台管理员',
  `mobile` = '13900000000',
  `email` = 'admin-new@example.com',
  `updated_by` = 1
WHERE `tenant_id` = 100001
  AND `id` = 200001
  AND `is_deleted` = 0;
```

### 停用租户

下面的 SQL 用于停用租户。租户停用后，该租户下用户通常不允许登录，业务数据通常不删除。

```sql
UPDATE `sys_tenant`
SET
  `tenant_status` = 0,
  `updated_by` = 1
WHERE `id` = 100001
  AND `tenant_status` = 1
  AND `is_deleted` = 0;
```

### 冻结租户

下面的 SQL 用于冻结租户。冻结通常用于欠费、风险控制或合规处理。

```sql
UPDATE `sys_tenant`
SET
  `tenant_status` = 2,
  `updated_by` = 1
WHERE `id` = 100001
  AND `tenant_status` = 1
  AND `is_deleted` = 0;
```

### 停用租户下所有用户

下面的 SQL 用于租户停用后批量禁用该租户下的用户。

```sql
UPDATE `sys_tenant_user`
SET
  `user_status` = 0,
  `updated_by` = 1
WHERE `tenant_id` = 100001
  AND `user_status` = 1
  AND `is_deleted` = 0;
```

### 查询即将到期租户

下面的 SQL 用于查询 30 天内即将到期的租户，适合续费提醒和运营管理。

```sql
SELECT
  `id`,
  `tenant_code`,
  `tenant_name`,
  `contact_name`,
  `contact_mobile`,
  `expire_time`
FROM `sys_tenant`
WHERE `tenant_status` = 1
  AND `is_deleted` = 0
  AND `expire_time` >= NOW()
  AND `expire_time` < DATE_ADD(NOW(), INTERVAL 30 DAY)
ORDER BY `expire_time` ASC;
```

### 查询已过期租户

下面的 SQL 用于查询已经过期但仍处于启用状态的租户，适合定时任务冻结租户。

```sql
SELECT
  `id`,
  `tenant_code`,
  `tenant_name`,
  `expire_time`,
  `tenant_status`
FROM `sys_tenant`
WHERE `tenant_status` = 1
  AND `is_deleted` = 0
  AND `expire_time` IS NOT NULL
  AND `expire_time` < NOW();
```

### 统计租户用户数量

下面的 SQL 用于统计每个租户下的有效用户数量。

```sql
SELECT
  t.`id`,
  t.`tenant_code`,
  t.`tenant_name`,
  COUNT(u.`id`) AS `user_count`
FROM `sys_tenant` t
LEFT JOIN `sys_tenant_user` u
  ON t.`id` = u.`tenant_id`
  AND u.`is_deleted` = 0
WHERE t.`is_deleted` = 0
GROUP BY
  t.`id`,
  t.`tenant_code`,
  t.`tenant_name`
ORDER BY `user_count` DESC;
```

### 校验租户用户数量限制

下面的 SQL 用于查询租户当前用户数和最大用户数，适合新增用户前校验。

```sql
SELECT
  t.`id`,
  t.`tenant_code`,
  t.`tenant_name`,
  t.`max_user_count`,
  COUNT(u.`id`) AS `current_user_count`
FROM `sys_tenant` t
LEFT JOIN `sys_tenant_user` u
  ON t.`id` = u.`tenant_id`
  AND u.`is_deleted` = 0
WHERE t.`id` = 100001
  AND t.`is_deleted` = 0
GROUP BY
  t.`id`,
  t.`tenant_code`,
  t.`tenant_name`,
  t.`max_user_count`;
```

### 查询跨租户用户名重复情况

下面的 SQL 用于查询同一个用户名被多少个租户使用。多租户模型下，这种重复通常是允许的。

```sql
SELECT
  `username`,
  COUNT(DISTINCT `tenant_id`) AS `tenant_count`
FROM `sys_tenant_user`
WHERE `is_deleted` = 0
GROUP BY `username`
HAVING COUNT(DISTINCT `tenant_id`) > 1
ORDER BY `tenant_count` DESC;
```

### 逻辑删除租户用户

下面的 SQL 用于逻辑删除指定租户下的用户。删除时更新 `delete_marker`，支持同租户下重新创建同用户名。

```sql
UPDATE `sys_tenant_user`
SET
  `is_deleted` = 1,
  `delete_marker` = `id`,
  `updated_by` = 1
WHERE `tenant_id` = 100001
  AND `id` = 200001
  AND `is_deleted` = 0;
```

### 重新创建同名租户用户

下面的 SQL 演示用户软删除后，在同一租户下重新创建相同用户名。

```sql
INSERT INTO `sys_tenant_user` (
  `id`,
  `tenant_id`,
  `user_no`,
  `username`,
  `nickname`,
  `mobile`,
  `email`,
  `user_status`,
  `created_by`,
  `updated_by`
) VALUES (
  200002,
  100001,
  'USER_202605110002',
  'admin',
  '新租户管理员',
  '13700000000',
  'new-admin@example.com',
  1,
  1,
  1
);
```

### 逻辑删除租户

下面的 SQL 用于逻辑删除租户。实际业务中，删除租户前通常要确认租户已停用，并完成数据归档。

```sql
UPDATE `sys_tenant`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 100001
  AND `tenant_status` = 0
  AND `is_deleted` = 0;
```

### 多租户业务表设计示例

下面的 SQL 演示普通业务表如何加入 `tenant_id`。同一个商品编码在不同租户下可以重复，但在同一租户内必须唯一。

```sql
CREATE TABLE `tenant_product` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `product_code` VARCHAR(64) NOT NULL COMMENT '商品编码',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `product_status` TINYINT NOT NULL DEFAULT 1 COMMENT '商品状态：0下架，1上架',
  `price` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '商品价格',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `delete_marker` BIGINT NOT NULL DEFAULT 0 COMMENT '删除标记：未删除为0，删除后写入主键ID或唯一值',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_product_code_deleted` (`tenant_id`, `product_code`, `delete_marker`),
  KEY `idx_tenant_status_deleted` (`tenant_id`, `product_status`, `is_deleted`),
  KEY `idx_tenant_created` (`tenant_id`, `created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='租户商品表';
```

### 查询租户业务数据

下面的 SQL 用于查询指定租户下的商品列表。任何租户业务查询都应包含 `tenant_id`。

```sql
SELECT
  `id`,
  `tenant_id`,
  `product_code`,
  `product_name`,
  `product_status`,
  `price`,
  `created_at`
FROM `tenant_product`
WHERE `tenant_id` = 100001
  AND `product_status` = 1
  AND `is_deleted` = 0
ORDER BY `created_at` DESC
LIMIT 10 OFFSET 0;
```

### 更新租户业务数据

下面的 SQL 用于更新指定租户下的商品。更新条件必须带上 `tenant_id`，避免跨租户误更新。

```sql
UPDATE `tenant_product`
SET
  `product_name` = '租户商品新版名称',
  `price` = 199.00,
  `updated_by` = 1
WHERE `tenant_id` = 100001
  AND `id` = 300001
  AND `is_deleted` = 0;
```

### 索引设计建议

多租户模型的索引设计重点是把 `tenant_id` 放入高频查询索引和唯一约束中。业务表中的唯一字段通常不是全局唯一，而是租户内唯一。

| 表                | 索引                                                         | 适用场景               |
| ----------------- | ------------------------------------------------------------ | ---------------------- |
| `sys_tenant`      | `PRIMARY KEY (id)`                                           | 查询租户详情           |
| `sys_tenant`      | `uk_tenant_code (tenant_code)`                               | 根据租户编码识别租户   |
| `sys_tenant`      | `idx_status_deleted (tenant_status, is_deleted)`             | 查询启用租户           |
| `sys_tenant`      | `idx_expire_time (expire_time)`                              | 查询到期租户           |
| `sys_tenant_user` | `uk_tenant_user_no_deleted (tenant_id, user_no, delete_marker)` | 保证租户内用户编号唯一 |
| `sys_tenant_user` | `uk_tenant_username_deleted (tenant_id, username, delete_marker)` | 保证租户内用户名唯一   |
| `sys_tenant_user` | `idx_tenant_status_deleted (tenant_id, user_status, is_deleted)` | 查询租户用户           |
| `tenant_product`  | `uk_tenant_product_code_deleted (tenant_id, product_code, delete_marker)` | 保证租户内商品编码唯一 |
| `tenant_product`  | `idx_tenant_status_deleted (tenant_id, product_status, is_deleted)` | 查询租户商品列表       |
| `tenant_product`  | `idx_tenant_created (tenant_id, created_at)`                 | 租户内按时间分页       |

### 使用建议

多租户模型的核心原则是所有租户业务数据都必须带上 `tenant_id`，查询、更新、删除和唯一约束都必须考虑租户维度。不要只在应用层记住当前租户，而忘记在 SQL 条件中隔离租户数据。

使用多租户模型时重点注意：

- 大多数业务表都应包含 `tenant_id`
- 租户表本身通常不需要 `tenant_id`
- 唯一约束通常要包含 `tenant_id`
- 查询、更新、删除都必须带上 `tenant_id`
- 后端应统一注入租户条件，避免开发遗漏
- 租户管理员只能访问当前租户数据
- 平台管理员跨租户查询应走独立权限控制
- 租户停用后通常禁止登录和业务写入
- 租户删除前应先停用并完成数据归档
- `tenant_id` 应放入高频组合索引前缀
- 数据量很大时，可以按租户、时间或业务维度进一步分库分表
- 多租户隔离是安全边界，不能只依赖前端传参控制

## 历史版本模型

历史版本模型用于保存业务数据的多个版本，支持查看历史、版本对比、版本回滚、草稿编辑、正式发布等能力。它常用于文章、合同、配置、商品详情、流程模板、表单模板、规则配置等需要保留变更历史的业务。

下面以“文章主表”和“文章版本表”为例说明历史版本模型的建表方式和常用 SQL 实践。

### 适用场景

历史版本模型适合业务数据会被多次修改，并且需要保留每次修改结果的场景。它和审计日志不同，审计日志关注变更过程，历史版本模型关注某个时间点的完整业务快照。

常见场景包括：

- 文章版本管理
- 合同版本管理
- 商品详情版本管理
- 配置项版本管理
- 表单模板版本管理
- 流程模板版本管理
- 规则配置版本管理
- 页面装修版本管理
- 协议条款版本管理
- 知识库文档版本管理

### 建表 SQL

下面的 SQL 创建文章主表和文章版本表。主表保存当前生效版本的信息，版本表保存每一次版本快照。

```sql
CREATE TABLE `cms_article_version_main` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `article_code` VARCHAR(64) NOT NULL COMMENT '文章编码',
  `article_title` VARCHAR(200) NOT NULL COMMENT '当前文章标题',
  `article_status` TINYINT NOT NULL DEFAULT 0 COMMENT '文章状态：0草稿，1发布，2下架',
  `current_version_no` INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
  `published_version_no` INT NULL COMMENT '当前发布版本号',
  `publish_time` DATETIME NULL COMMENT '发布时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_code` (`article_code`),
  KEY `idx_status_deleted` (`article_status`, `is_deleted`),
  KEY `idx_current_version` (`current_version_no`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='CMS文章版本主表';

CREATE TABLE `cms_article_version_detail` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章主表ID',
  `article_code` VARCHAR(64) NOT NULL COMMENT '文章编码，冗余字段',
  `version_no` INT NOT NULL COMMENT '版本号',
  `version_title` VARCHAR(200) NOT NULL COMMENT '版本标题',
  `version_content` LONGTEXT NULL COMMENT '版本正文内容',
  `version_summary` VARCHAR(500) NULL COMMENT '版本摘要',
  `version_status` TINYINT NOT NULL DEFAULT 0 COMMENT '版本状态：0草稿，1已发布，2历史版本，3废弃',
  `change_type` VARCHAR(32) NOT NULL DEFAULT 'create' COMMENT '变更类型：create创建，update修改，publish发布，rollback回滚',
  `change_summary` VARCHAR(1000) NULL COMMENT '变更说明',
  `snapshot_json` JSON NULL COMMENT '版本快照JSON，保存扩展字段',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `published_by` BIGINT NULL COMMENT '发布人ID',
  `published_at` DATETIME NULL COMMENT '发布时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_version` (`article_id`, `version_no`),
  KEY `idx_article_status_time` (`article_id`, `version_status`, `created_at`),
  KEY `idx_article_code_version` (`article_code`, `version_no`),
  KEY `idx_created_by_time` (`created_by`, `created_at`),
  KEY `idx_published_time` (`published_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='CMS文章版本明细表';
```

### 字段设计说明

历史版本模型通常由主表和版本表组成。主表保存当前业务对象的状态和当前版本号，版本表保存每个版本的完整快照。

| 表                           | 字段                   | 说明                   |
| ---------------------------- | ---------------------- | ---------------------- |
| `cms_article_version_main`   | `article_code`         | 文章编码，业务唯一标识 |
| `cms_article_version_main`   | `current_version_no`   | 当前编辑版本号         |
| `cms_article_version_main`   | `published_version_no` | 当前发布版本号         |
| `cms_article_version_main`   | `article_status`       | 主业务状态             |
| `cms_article_version_detail` | `article_id`           | 文章主表 ID            |
| `cms_article_version_detail` | `version_no`           | 版本号                 |
| `cms_article_version_detail` | `version_status`       | 版本状态               |
| `cms_article_version_detail` | `change_type`          | 版本变更类型           |
| `cms_article_version_detail` | `snapshot_json`        | 版本扩展快照           |

### 新增主表数据

下面的 SQL 用于新增文章主表数据。

```sql
INSERT INTO `cms_article_version_main` (
  `id`,
  `article_code`,
  `article_title`,
  `article_status`,
  `current_version_no`,
  `published_version_no`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  100001,
  'ARTICLE_202605110001',
  'MySQL 8 业务建模实践',
  0,
  1,
  NULL,
  '历史版本模型示例文章',
  1,
  1
);
```

### 新增初始版本

下面的 SQL 用于新增文章的第一个版本。新增主表和新增初始版本通常应放在同一个事务中执行。

```sql
INSERT INTO `cms_article_version_detail` (
  `id`,
  `article_id`,
  `article_code`,
  `version_no`,
  `version_title`,
  `version_content`,
  `version_summary`,
  `version_status`,
  `change_type`,
  `change_summary`,
  `snapshot_json`,
  `created_by`
) VALUES (
  110001,
  100001,
  'ARTICLE_202605110001',
  1,
  'MySQL 8 业务建模实践',
  '这里是文章第一个版本的正文内容。',
  '第一个草稿版本',
  0,
  'create',
  '创建文章初始版本',
  JSON_OBJECT('tags', JSON_ARRAY('MySQL', '建模'), 'source', 'admin'),
  1
);
```

### 查询当前版本

下面的 SQL 用于查询文章当前编辑版本内容。

```sql
SELECT
  m.`id`,
  m.`article_code`,
  m.`article_title`,
  m.`article_status`,
  m.`current_version_no`,
  v.`version_title`,
  v.`version_content`,
  v.`version_summary`,
  v.`version_status`,
  v.`snapshot_json`,
  v.`created_at`
FROM `cms_article_version_main` m
JOIN `cms_article_version_detail` v
  ON m.`id` = v.`article_id`
  AND m.`current_version_no` = v.`version_no`
WHERE m.`id` = 100001
  AND m.`is_deleted` = 0;
```

### 查询发布版本

下面的 SQL 用于查询文章当前正式发布版本内容，适合前台展示。

```sql
SELECT
  m.`id`,
  m.`article_code`,
  m.`article_status`,
  m.`published_version_no`,
  v.`version_title`,
  v.`version_content`,
  v.`version_summary`,
  v.`snapshot_json`,
  v.`published_at`
FROM `cms_article_version_main` m
JOIN `cms_article_version_detail` v
  ON m.`id` = v.`article_id`
  AND m.`published_version_no` = v.`version_no`
WHERE m.`id` = 100001
  AND m.`article_status` = 1
  AND m.`is_deleted` = 0;
```

### 创建新版本

下面的 SQL 用于基于当前版本创建一个新版本。实际业务中通常先查询当前版本内容，再生成新的版本号。

```sql
INSERT INTO `cms_article_version_detail` (
  `id`,
  `article_id`,
  `article_code`,
  `version_no`,
  `version_title`,
  `version_content`,
  `version_summary`,
  `version_status`,
  `change_type`,
  `change_summary`,
  `snapshot_json`,
  `created_by`
)
SELECT
  110002,
  v.`article_id`,
  v.`article_code`,
  v.`version_no` + 1,
  'MySQL 8 业务建模实践新版',
  '这里是文章第二个版本的正文内容。',
  '第二个草稿版本',
  0,
  'update',
  '基于上一版本修改文章内容',
  JSON_SET(
    COALESCE(v.`snapshot_json`, JSON_OBJECT()),
    '$.tags',
    JSON_ARRAY('MySQL', '建模', '版本管理')
  ),
  1
FROM `cms_article_version_detail` v
JOIN `cms_article_version_main` m
  ON v.`article_id` = m.`id`
  AND v.`version_no` = m.`current_version_no`
WHERE m.`id` = 100001
  AND m.`is_deleted` = 0;
```

### 更新主表当前版本号

下面的 SQL 用于创建新版本后，同步更新主表的当前版本号。

```sql
UPDATE `cms_article_version_main`
SET
  `article_title` = 'MySQL 8 业务建模实践新版',
  `current_version_no` = `current_version_no` + 1,
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 修改草稿版本

下面的 SQL 用于修改草稿版本内容。已发布版本和历史版本通常不建议直接修改。

```sql
UPDATE `cms_article_version_detail`
SET
  `version_title` = 'MySQL 8 常用业务建模实践',
  `version_content` = '这里是修改后的草稿正文内容。',
  `version_summary` = '修改草稿标题和正文',
  `change_summary` = '调整文章标题和正文内容',
  `snapshot_json` = JSON_SET(
    COALESCE(`snapshot_json`, JSON_OBJECT()),
    '$.source',
    'editor'
  )
WHERE `article_id` = 100001
  AND `version_no` = 2
  AND `version_status` = 0;
```

### 发布版本

下面的 SQL 用于将指定草稿版本发布。发布时应将旧发布版本改为历史版本，再将目标版本改为已发布，并更新主表发布版本号。

```sql
START TRANSACTION;

UPDATE `cms_article_version_detail`
SET
  `version_status` = 2
WHERE `article_id` = 100001
  AND `version_status` = 1;

UPDATE `cms_article_version_detail`
SET
  `version_status` = 1,
  `change_type` = 'publish',
  `published_by` = 1,
  `published_at` = NOW()
WHERE `article_id` = 100001
  AND `version_no` = 2
  AND `version_status` = 0;

UPDATE `cms_article_version_main`
SET
  `article_title` = 'MySQL 8 常用业务建模实践',
  `article_status` = 1,
  `published_version_no` = 2,
  `publish_time` = NOW(),
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;

COMMIT;
```

### 查询版本列表

下面的 SQL 用于查询某篇文章的所有版本。

```sql
SELECT
  `id`,
  `article_id`,
  `article_code`,
  `version_no`,
  `version_title`,
  `version_status`,
  `change_type`,
  `change_summary`,
  `created_by`,
  `created_at`,
  `published_by`,
  `published_at`
FROM `cms_article_version_detail`
WHERE `article_id` = 100001
ORDER BY `version_no` DESC;
```

### 查询指定版本详情

下面的 SQL 用于查询某个指定版本的完整内容。

```sql
SELECT
  `id`,
  `article_id`,
  `article_code`,
  `version_no`,
  `version_title`,
  `version_content`,
  `version_summary`,
  `version_status`,
  `change_type`,
  `change_summary`,
  `snapshot_json`,
  `created_by`,
  `created_at`,
  `published_by`,
  `published_at`
FROM `cms_article_version_detail`
WHERE `article_id` = 100001
  AND `version_no` = 1;
```

### 对比两个版本

下面的 SQL 用于查询两个版本的核心字段，便于应用层进行内容对比。

```sql
SELECT
  `version_no`,
  `version_title`,
  `version_summary`,
  `version_content`,
  `snapshot_json`,
  `created_at`
FROM `cms_article_version_detail`
WHERE `article_id` = 100001
  AND `version_no` IN (1, 2)
ORDER BY `version_no` ASC;
```

### 查询版本字段差异

下面的 SQL 用于在 SQL 层简单判断两个版本标题、摘要和扩展信息是否不同。

```sql
SELECT
  old_v.`version_no` AS `old_version_no`,
  new_v.`version_no` AS `new_version_no`,
  CASE WHEN old_v.`version_title` <> new_v.`version_title` THEN 1 ELSE 0 END AS `title_changed`,
  CASE WHEN old_v.`version_summary` <> new_v.`version_summary` THEN 1 ELSE 0 END AS `summary_changed`,
  CASE WHEN JSON_PRETTY(old_v.`snapshot_json`) <> JSON_PRETTY(new_v.`snapshot_json`) THEN 1 ELSE 0 END AS `snapshot_changed`
FROM `cms_article_version_detail` old_v
JOIN `cms_article_version_detail` new_v
  ON old_v.`article_id` = new_v.`article_id`
WHERE old_v.`article_id` = 100001
  AND old_v.`version_no` = 1
  AND new_v.`version_no` = 2;
```

### 回滚到历史版本

下面的 SQL 用于把某个历史版本复制成新的草稿版本，而不是直接修改旧版本。这样可以保留完整版本链路。

```sql
START TRANSACTION;

INSERT INTO `cms_article_version_detail` (
  `id`,
  `article_id`,
  `article_code`,
  `version_no`,
  `version_title`,
  `version_content`,
  `version_summary`,
  `version_status`,
  `change_type`,
  `change_summary`,
  `snapshot_json`,
  `created_by`
)
SELECT
  110003,
  v.`article_id`,
  v.`article_code`,
  m.`current_version_no` + 1,
  v.`version_title`,
  v.`version_content`,
  v.`version_summary`,
  0,
  'rollback',
  CONCAT('从版本 ', v.`version_no`, ' 回滚生成新草稿'),
  v.`snapshot_json`,
  1
FROM `cms_article_version_detail` v
JOIN `cms_article_version_main` m
  ON v.`article_id` = m.`id`
WHERE v.`article_id` = 100001
  AND v.`version_no` = 1
  AND m.`is_deleted` = 0;

UPDATE `cms_article_version_main`
SET
  `current_version_no` = `current_version_no` + 1,
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;

COMMIT;
```

### 废弃草稿版本

下面的 SQL 用于废弃未发布的草稿版本。

```sql
UPDATE `cms_article_version_detail`
SET
  `version_status` = 3,
  `change_summary` = '废弃草稿版本'
WHERE `article_id` = 100001
  AND `version_no` = 3
  AND `version_status` = 0;
```

### 查询最新草稿版本

下面的 SQL 用于查询文章最新的草稿版本。

```sql
SELECT
  `id`,
  `article_id`,
  `version_no`,
  `version_title`,
  `version_summary`,
  `created_at`
FROM `cms_article_version_detail`
WHERE `article_id` = 100001
  AND `version_status` = 0
ORDER BY `version_no` DESC
LIMIT 1;
```

### 查询长期未发布草稿

下面的 SQL 用于查询创建超过 30 天仍未发布的草稿版本，适合内容治理。

```sql
SELECT
  v.`id`,
  v.`article_id`,
  v.`article_code`,
  v.`version_no`,
  v.`version_title`,
  v.`created_by`,
  v.`created_at`
FROM `cms_article_version_detail` v
WHERE v.`version_status` = 0
  AND v.`created_at` < DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY v.`created_at` ASC;
```

### 查询版本数量统计

下面的 SQL 用于统计每篇文章的版本数量。

```sql
SELECT
  `article_id`,
  `article_code`,
  COUNT(*) AS `version_count`,
  MAX(`version_no`) AS `max_version_no`
FROM `cms_article_version_detail`
GROUP BY
  `article_id`,
  `article_code`
ORDER BY `version_count` DESC;
```

### 清理废弃版本

下面的 SQL 用于清理废弃很久的版本。生产环境通常建议先归档，再物理删除。

```sql
DELETE FROM `cms_article_version_detail`
WHERE `version_status` = 3
  AND `created_at` < DATE_SUB(NOW(), INTERVAL 365 DAY);
```

### 逻辑删除主数据

下面的 SQL 用于逻辑删除文章主表。版本表通常不跟随物理删除，便于保留历史。

```sql
UPDATE `cms_article_version_main`
SET
  `is_deleted` = 1,
  `updated_by` = 1
WHERE `id` = 100001
  AND `is_deleted` = 0;
```

### 索引设计建议

历史版本模型的索引重点是主数据唯一编码、文章版本号、版本状态和发布时间。

| 表                           | 索引                                                         | 适用场景                 |
| ---------------------------- | ------------------------------------------------------------ | ------------------------ |
| `cms_article_version_main`   | `PRIMARY KEY (id)`                                           | 查询文章主数据           |
| `cms_article_version_main`   | `uk_article_code (article_code)`                             | 根据文章编码查询         |
| `cms_article_version_main`   | `idx_status_deleted (article_status, is_deleted)`            | 查询指定状态文章         |
| `cms_article_version_detail` | `uk_article_version (article_id, version_no)`                | 保证同一文章版本号唯一   |
| `cms_article_version_detail` | `idx_article_status_time (article_id, version_status, created_at)` | 查询文章版本列表         |
| `cms_article_version_detail` | `idx_article_code_version (article_code, version_no)`        | 根据文章编码和版本号查询 |
| `cms_article_version_detail` | `idx_created_by_time (created_by, created_at)`               | 查询用户创建的版本       |
| `cms_article_version_detail` | `idx_published_time (published_at)`                          | 查询已发布版本           |

### 使用建议

历史版本模型的核心原则是版本不可随意覆盖。已发布版本和历史版本应尽量只读，新的修改应生成新版本。这样可以保证版本链路完整，便于回滚、对比和审计。

使用历史版本模型时重点注意：

- 主表保存当前状态和当前版本号
- 版本表保存每个版本的完整快照
- 已发布版本不建议直接修改
- 修改内容时应生成新版本，而不是覆盖旧版本
- 回滚时建议复制历史版本生成新草稿
- 发布版本时应更新主表发布版本号
- 历史版本模型不能替代审计日志，关键操作仍应记录审计
- 大文本版本较多时，要关注存储空间增长
- 版本内容可以按业务需要归档
- 版本号应在同一业务对象内递增

## 统计汇总模型

统计汇总模型用于将明细业务数据按照日期、业务类型、用户、商品、租户、地区等维度提前汇总到统计表中，提升报表、看板和运营分析的查询性能。它通常不是核心交易模型，而是由明细表派生出来的查询模型。

下面以“订单日统计表”为例说明统计汇总模型的建表方式和常用 SQL 实践。

### 适用场景

统计汇总模型适合明细数据量大、报表查询频繁、聚合计算成本高的场景。它通过提前汇总减少实时扫描明细表的压力。

常见场景包括：

- 订单日统计
- 商品销售统计
- 用户增长统计
- 支付金额统计
- 退款金额统计
- 访问量统计
- 租户业务统计
- 门店销售统计
- 客户转化统计
- 运营数据看板

### 建表 SQL

下面的 SQL 创建订单日统计表。该表以统计日期、租户 ID、订单来源为维度，保存订单数、支付金额、退款金额、用户数等汇总指标。

```sql
CREATE TABLE `stat_order_daily` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID，0表示平台级统计',
  `order_source` TINYINT NOT NULL DEFAULT 0 COMMENT '订单来源：0全部，1后台，2小程序，3APP，4网页',
  `order_count` BIGINT NOT NULL DEFAULT 0 COMMENT '订单总数',
  `paid_order_count` BIGINT NOT NULL DEFAULT 0 COMMENT '已支付订单数',
  `cancel_order_count` BIGINT NOT NULL DEFAULT 0 COMMENT '取消订单数',
  `refund_order_count` BIGINT NOT NULL DEFAULT 0 COMMENT '退款订单数',
  `buyer_count` BIGINT NOT NULL DEFAULT 0 COMMENT '下单用户数',
  `paid_buyer_count` BIGINT NOT NULL DEFAULT 0 COMMENT '支付用户数',
  `product_quantity` BIGINT NOT NULL DEFAULT 0 COMMENT '商品销售数量',
  `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  `discount_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
  `refund_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `net_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '净收入金额，支付金额减退款金额',
  `avg_order_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '平均订单金额',
  `stat_status` TINYINT NOT NULL DEFAULT 1 COMMENT '统计状态：0待统计，1已统计，2统计失败',
  `stat_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_tenant_source` (`stat_date`, `tenant_id`, `order_source`),
  KEY `idx_tenant_date` (`tenant_id`, `stat_date`),
  KEY `idx_stat_status_time` (`stat_status`, `stat_time`),
  KEY `idx_pay_amount` (`pay_amount`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='订单日统计汇总表';
```

### 字段设计说明

统计汇总模型通常包含维度字段和指标字段。维度字段决定“按什么统计”，指标字段决定“统计什么”。

| 字段类型 | 字段               | 说明           |
| -------- | ------------------ | -------------- |
| 统计维度 | `stat_date`        | 按日期统计     |
| 统计维度 | `tenant_id`        | 按租户统计     |
| 统计维度 | `order_source`     | 按订单来源统计 |
| 统计指标 | `order_count`      | 订单总数       |
| 统计指标 | `paid_order_count` | 已支付订单数   |
| 统计指标 | `buyer_count`      | 下单用户数     |
| 统计指标 | `pay_amount`       | 支付金额       |
| 统计指标 | `refund_amount`    | 退款金额       |
| 统计指标 | `net_amount`       | 净收入金额     |
| 任务字段 | `stat_status`      | 统计状态       |
| 任务字段 | `stat_time`        | 统计时间       |

### 初始化统计数据

下面的 SQL 用于按天初始化订单统计数据。实际业务中可以由定时任务、离线任务或数据平台执行。

```sql
INSERT INTO `stat_order_daily` (
  `id`,
  `stat_date`,
  `tenant_id`,
  `order_source`,
  `order_count`,
  `paid_order_count`,
  `cancel_order_count`,
  `refund_order_count`,
  `buyer_count`,
  `paid_buyer_count`,
  `product_quantity`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `refund_amount`,
  `net_amount`,
  `avg_order_amount`,
  `stat_status`,
  `stat_time`
)
SELECT
  CAST(DATE_FORMAT(o.`created_at`, '%Y%m%d') AS UNSIGNED) * 1000 + o.`order_source` AS `id`,
  DATE(o.`created_at`) AS `stat_date`,
  0 AS `tenant_id`,
  o.`order_source`,
  COUNT(*) AS `order_count`,
  SUM(CASE WHEN o.`pay_status` = 1 THEN 1 ELSE 0 END) AS `paid_order_count`,
  SUM(CASE WHEN o.`order_status` = 4 THEN 1 ELSE 0 END) AS `cancel_order_count`,
  SUM(CASE WHEN o.`pay_status` IN (2, 3) THEN 1 ELSE 0 END) AS `refund_order_count`,
  COUNT(DISTINCT o.`user_id`) AS `buyer_count`,
  COUNT(DISTINCT CASE WHEN o.`pay_status` = 1 THEN o.`user_id` END) AS `paid_buyer_count`,
  IFNULL(SUM(i.`quantity`), 0) AS `product_quantity`,
  IFNULL(SUM(o.`total_amount`), 0.00) AS `total_amount`,
  IFNULL(SUM(o.`discount_amount`), 0.00) AS `discount_amount`,
  IFNULL(SUM(o.`pay_amount`), 0.00) AS `pay_amount`,
  IFNULL(SUM(i.`refund_amount`), 0.00) AS `refund_amount`,
  IFNULL(SUM(o.`pay_amount`), 0.00) - IFNULL(SUM(i.`refund_amount`), 0.00) AS `net_amount`,
  IFNULL(ROUND(AVG(o.`pay_amount`), 2), 0.00) AS `avg_order_amount`,
  1 AS `stat_status`,
  NOW() AS `stat_time`
FROM `mall_order` o
LEFT JOIN `mall_order_item` i
  ON o.`id` = i.`order_id`
  AND i.`is_deleted` = 0
WHERE o.`is_deleted` = 0
GROUP BY
  DATE(o.`created_at`),
  o.`order_source`;
```

### 增量汇总当天数据

下面的 SQL 用于统计当天订单数据，并通过 `ON DUPLICATE KEY UPDATE` 更新已存在的统计记录。

```sql
INSERT INTO `stat_order_daily` (
  `id`,
  `stat_date`,
  `tenant_id`,
  `order_source`,
  `order_count`,
  `paid_order_count`,
  `cancel_order_count`,
  `refund_order_count`,
  `buyer_count`,
  `paid_buyer_count`,
  `product_quantity`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `refund_amount`,
  `net_amount`,
  `avg_order_amount`,
  `stat_status`,
  `stat_time`
)
SELECT
  CAST(DATE_FORMAT(CURDATE(), '%Y%m%d') AS UNSIGNED) * 1000 + o.`order_source` AS `id`,
  CURDATE() AS `stat_date`,
  0 AS `tenant_id`,
  o.`order_source`,
  COUNT(*) AS `order_count`,
  SUM(CASE WHEN o.`pay_status` = 1 THEN 1 ELSE 0 END) AS `paid_order_count`,
  SUM(CASE WHEN o.`order_status` = 4 THEN 1 ELSE 0 END) AS `cancel_order_count`,
  SUM(CASE WHEN o.`pay_status` IN (2, 3) THEN 1 ELSE 0 END) AS `refund_order_count`,
  COUNT(DISTINCT o.`user_id`) AS `buyer_count`,
  COUNT(DISTINCT CASE WHEN o.`pay_status` = 1 THEN o.`user_id` END) AS `paid_buyer_count`,
  IFNULL(SUM(i.`quantity`), 0) AS `product_quantity`,
  IFNULL(SUM(o.`total_amount`), 0.00) AS `total_amount`,
  IFNULL(SUM(o.`discount_amount`), 0.00) AS `discount_amount`,
  IFNULL(SUM(o.`pay_amount`), 0.00) AS `pay_amount`,
  IFNULL(SUM(i.`refund_amount`), 0.00) AS `refund_amount`,
  IFNULL(SUM(o.`pay_amount`), 0.00) - IFNULL(SUM(i.`refund_amount`), 0.00) AS `net_amount`,
  IFNULL(ROUND(AVG(o.`pay_amount`), 2), 0.00) AS `avg_order_amount`,
  1 AS `stat_status`,
  NOW() AS `stat_time`
FROM `mall_order` o
LEFT JOIN `mall_order_item` i
  ON o.`id` = i.`order_id`
  AND i.`is_deleted` = 0
WHERE o.`is_deleted` = 0
  AND o.`created_at` >= CURDATE()
  AND o.`created_at` < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
GROUP BY o.`order_source`
ON DUPLICATE KEY UPDATE
  `order_count` = VALUES(`order_count`),
  `paid_order_count` = VALUES(`paid_order_count`),
  `cancel_order_count` = VALUES(`cancel_order_count`),
  `refund_order_count` = VALUES(`refund_order_count`),
  `buyer_count` = VALUES(`buyer_count`),
  `paid_buyer_count` = VALUES(`paid_buyer_count`),
  `product_quantity` = VALUES(`product_quantity`),
  `total_amount` = VALUES(`total_amount`),
  `discount_amount` = VALUES(`discount_amount`),
  `pay_amount` = VALUES(`pay_amount`),
  `refund_amount` = VALUES(`refund_amount`),
  `net_amount` = VALUES(`net_amount`),
  `avg_order_amount` = VALUES(`avg_order_amount`),
  `stat_status` = 1,
  `stat_time` = NOW();
```

### 查询日趋势

下面的 SQL 用于查询某段时间内的订单日趋势。

```sql
SELECT
  `stat_date`,
  SUM(`order_count`) AS `order_count`,
  SUM(`paid_order_count`) AS `paid_order_count`,
  SUM(`pay_amount`) AS `pay_amount`,
  SUM(`refund_amount`) AS `refund_amount`,
  SUM(`net_amount`) AS `net_amount`
FROM `stat_order_daily`
WHERE `stat_date` >= '2026-05-01'
  AND `stat_date` < '2026-06-01'
  AND `tenant_id` = 0
GROUP BY `stat_date`
ORDER BY `stat_date` ASC;
```

### 查询订单来源统计

下面的 SQL 用于按订单来源统计订单数量和金额。

```sql
SELECT
  `order_source`,
  SUM(`order_count`) AS `order_count`,
  SUM(`paid_order_count`) AS `paid_order_count`,
  SUM(`pay_amount`) AS `pay_amount`,
  SUM(`net_amount`) AS `net_amount`
FROM `stat_order_daily`
WHERE `stat_date` >= '2026-05-01'
  AND `stat_date` < '2026-06-01'
  AND `tenant_id` = 0
GROUP BY `order_source`
ORDER BY `pay_amount` DESC;
```

### 查询月汇总

下面的 SQL 用于基于日统计表计算月度汇总。

```sql
SELECT
  DATE_FORMAT(`stat_date`, '%Y-%m') AS `stat_month`,
  SUM(`order_count`) AS `order_count`,
  SUM(`paid_order_count`) AS `paid_order_count`,
  SUM(`cancel_order_count`) AS `cancel_order_count`,
  SUM(`refund_order_count`) AS `refund_order_count`,
  SUM(`buyer_count`) AS `buyer_count`,
  SUM(`paid_buyer_count`) AS `paid_buyer_count`,
  SUM(`product_quantity`) AS `product_quantity`,
  SUM(`total_amount`) AS `total_amount`,
  SUM(`discount_amount`) AS `discount_amount`,
  SUM(`pay_amount`) AS `pay_amount`,
  SUM(`refund_amount`) AS `refund_amount`,
  SUM(`net_amount`) AS `net_amount`
FROM `stat_order_daily`
WHERE `stat_date` >= '2026-01-01'
  AND `stat_date` < '2027-01-01'
  AND `tenant_id` = 0
GROUP BY DATE_FORMAT(`stat_date`, '%Y-%m')
ORDER BY `stat_month` ASC;
```

### 查询最近 7 天统计

下面的 SQL 用于查询最近 7 天订单统计。

```sql
SELECT
  `stat_date`,
  SUM(`order_count`) AS `order_count`,
  SUM(`paid_order_count`) AS `paid_order_count`,
  SUM(`pay_amount`) AS `pay_amount`,
  SUM(`net_amount`) AS `net_amount`
FROM `stat_order_daily`
WHERE `stat_date` >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
  AND `stat_date` < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
  AND `tenant_id` = 0
GROUP BY `stat_date`
ORDER BY `stat_date` ASC;
```

### 查询支付转化率

下面的 SQL 用于计算订单支付转化率。

```sql
SELECT
  `stat_date`,
  SUM(`order_count`) AS `order_count`,
  SUM(`paid_order_count`) AS `paid_order_count`,
  ROUND(
    SUM(`paid_order_count`) / NULLIF(SUM(`order_count`), 0) * 100,
    2
  ) AS `pay_conversion_rate`
FROM `stat_order_daily`
WHERE `stat_date` >= '2026-05-01'
  AND `stat_date` < '2026-06-01'
  AND `tenant_id` = 0
GROUP BY `stat_date`
ORDER BY `stat_date` ASC;
```

### 查询客单价

下面的 SQL 用于计算每日客单价。客单价通常等于支付金额除以已支付订单数。

```sql
SELECT
  `stat_date`,
  SUM(`pay_amount`) AS `pay_amount`,
  SUM(`paid_order_count`) AS `paid_order_count`,
  ROUND(
    SUM(`pay_amount`) / NULLIF(SUM(`paid_order_count`), 0),
    2
  ) AS `customer_unit_price`
FROM `stat_order_daily`
WHERE `stat_date` >= '2026-05-01'
  AND `stat_date` < '2026-06-01'
  AND `tenant_id` = 0
GROUP BY `stat_date`
ORDER BY `stat_date` ASC;
```

### 标记统计失败

下面的 SQL 用于统计任务异常时标记统计失败，便于后续重试。

```sql
UPDATE `stat_order_daily`
SET
  `stat_status` = 2,
  `stat_time` = NOW(),
  `remark` = '订单日统计任务执行失败'
WHERE `stat_date` = '2026-05-11'
  AND `tenant_id` = 0
  AND `order_source` = 2;
```

### 查询统计失败记录

下面的 SQL 用于查询统计失败的数据，适合任务重试和异常排查。

```sql
SELECT
  `id`,
  `stat_date`,
  `tenant_id`,
  `order_source`,
  `stat_status`,
  `stat_time`,
  `remark`
FROM `stat_order_daily`
WHERE `stat_status` = 2
ORDER BY `stat_time` DESC
LIMIT 50 OFFSET 0;
```

### 重算指定日期统计

下面的 SQL 用于先删除指定日期统计数据，再重新汇总。实际生产中建议放在同一个任务流程中执行。

```sql
START TRANSACTION;

DELETE FROM `stat_order_daily`
WHERE `stat_date` = '2026-05-11'
  AND `tenant_id` = 0;

INSERT INTO `stat_order_daily` (
  `id`,
  `stat_date`,
  `tenant_id`,
  `order_source`,
  `order_count`,
  `paid_order_count`,
  `cancel_order_count`,
  `refund_order_count`,
  `buyer_count`,
  `paid_buyer_count`,
  `product_quantity`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `refund_amount`,
  `net_amount`,
  `avg_order_amount`,
  `stat_status`,
  `stat_time`
)
SELECT
  CAST(DATE_FORMAT('2026-05-11', '%Y%m%d') AS UNSIGNED) * 1000 + o.`order_source` AS `id`,
  DATE('2026-05-11') AS `stat_date`,
  0 AS `tenant_id`,
  o.`order_source`,
  COUNT(*) AS `order_count`,
  SUM(CASE WHEN o.`pay_status` = 1 THEN 1 ELSE 0 END) AS `paid_order_count`,
  SUM(CASE WHEN o.`order_status` = 4 THEN 1 ELSE 0 END) AS `cancel_order_count`,
  SUM(CASE WHEN o.`pay_status` IN (2, 3) THEN 1 ELSE 0 END) AS `refund_order_count`,
  COUNT(DISTINCT o.`user_id`) AS `buyer_count`,
  COUNT(DISTINCT CASE WHEN o.`pay_status` = 1 THEN o.`user_id` END) AS `paid_buyer_count`,
  IFNULL(SUM(i.`quantity`), 0) AS `product_quantity`,
  IFNULL(SUM(o.`total_amount`), 0.00) AS `total_amount`,
  IFNULL(SUM(o.`discount_amount`), 0.00) AS `discount_amount`,
  IFNULL(SUM(o.`pay_amount`), 0.00) AS `pay_amount`,
  IFNULL(SUM(i.`refund_amount`), 0.00) AS `refund_amount`,
  IFNULL(SUM(o.`pay_amount`), 0.00) - IFNULL(SUM(i.`refund_amount`), 0.00) AS `net_amount`,
  IFNULL(ROUND(AVG(o.`pay_amount`), 2), 0.00) AS `avg_order_amount`,
  1 AS `stat_status`,
  NOW() AS `stat_time`
FROM `mall_order` o
LEFT JOIN `mall_order_item` i
  ON o.`id` = i.`order_id`
  AND i.`is_deleted` = 0
WHERE o.`is_deleted` = 0
  AND o.`created_at` >= '2026-05-11 00:00:00'
  AND o.`created_at` < '2026-05-12 00:00:00'
GROUP BY o.`order_source`;

COMMIT;
```

### 校验汇总订单数

下面的 SQL 用于对比统计表订单数和订单源表订单数，适合数据校验。

```sql
SELECT
  s.`stat_date`,
  s.`order_source`,
  s.`order_count` AS `stat_order_count`,
  t.`source_order_count`
FROM `stat_order_daily` s
JOIN (
  SELECT
    DATE(`created_at`) AS `stat_date`,
    `order_source`,
    COUNT(*) AS `source_order_count`
  FROM `mall_order`
  WHERE `is_deleted` = 0
    AND `created_at` >= '2026-05-01 00:00:00'
    AND `created_at` < '2026-06-01 00:00:00'
  GROUP BY
    DATE(`created_at`),
    `order_source`
) t
  ON s.`stat_date` = t.`stat_date`
  AND s.`order_source` = t.`order_source`
WHERE s.`tenant_id` = 0
  AND s.`order_count` <> t.`source_order_count`;
```

### 校验汇总支付金额

下面的 SQL 用于对比统计表支付金额和订单源表支付金额。

```sql
SELECT
  s.`stat_date`,
  s.`order_source`,
  s.`pay_amount` AS `stat_pay_amount`,
  t.`source_pay_amount`
FROM `stat_order_daily` s
JOIN (
  SELECT
    DATE(`created_at`) AS `stat_date`,
    `order_source`,
    SUM(`pay_amount`) AS `source_pay_amount`
  FROM `mall_order`
  WHERE `is_deleted` = 0
    AND `created_at` >= '2026-05-01 00:00:00'
    AND `created_at` < '2026-06-01 00:00:00'
  GROUP BY
    DATE(`created_at`),
    `order_source`
) t
  ON s.`stat_date` = t.`stat_date`
  AND s.`order_source` = t.`order_source`
WHERE s.`tenant_id` = 0
  AND s.`pay_amount` <> t.`source_pay_amount`;
```

### 删除历史统计数据

下面的 SQL 用于清理历史统计数据。生产环境通常建议按年归档后再清理。

```sql
DELETE FROM `stat_order_daily`
WHERE `stat_date` < DATE_SUB(CURDATE(), INTERVAL 3 YEAR);
```

### 索引设计建议

统计汇总模型的索引重点是统计维度和查询时间范围。统计表通常按日期查询较多，因此 `stat_date` 是核心字段。

| 索引                                                         | 适用场景                 |
| ------------------------------------------------------------ | ------------------------ |
| `PRIMARY KEY (id)`                                           | 查询统计记录详情         |
| `uk_date_tenant_source (stat_date, tenant_id, order_source)` | 防止同维度重复统计       |
| `idx_tenant_date (tenant_id, stat_date)`                     | 查询租户某段时间统计     |
| `idx_stat_status_time (stat_status, stat_time)`              | 查询统计失败或待统计记录 |
| `idx_pay_amount (pay_amount)`                                | 按支付金额排序或筛选     |

### 使用建议

统计汇总模型的核心原则是明细数据负责事实记录，统计表负责快速查询。统计表的数据可以被重算，不应作为唯一事实来源。

使用统计汇总模型时重点注意：

- 统计表是派生数据，不是核心源数据
- 统计维度要提前设计清楚，例如日期、租户、来源、地区
- 汇总粒度不要过细，否则统计表会快速膨胀
- 统计任务应支持重试和重算
- 统计结果应能通过明细表校验
- 当天数据可以多次刷新，历史数据可以固定
- 金额统计要统一口径，例如支付金额、退款金额、净收入
- 去重指标要谨慎，例如用户数、支付用户数
- 统计表适合按时间分区或归档
- 报表查询优先查统计表，不要每次扫描订单明细表
- 统计表异常时，应以明细源表为准重新生成数据

## 归档数据模型

归档数据模型用于把历史数据从在线业务表迁移到归档表，降低在线表数据量，提升核心业务查询和写入性能。归档后的数据通常只用于历史查询、审计、对账和数据分析，不再参与高频交易流程。

下面以“订单在线表”和“订单归档表”为例说明归档数据模型的建表方式和常用 SQL 实践。

### 适用场景

归档数据模型适合数据持续增长、历史数据访问频率低、在线表查询压力较大的业务场景。

常见场景包括：

- 历史订单归档
- 操作日志归档
- 审计日志归档
- 账户流水归档
- 支付流水归档
- 消息通知归档
- 登录日志归档
- 任务执行记录归档
- 工单历史归档
- 报表明细归档

### 建表 SQL

下面的 SQL 创建订单在线表和订单归档表。归档表字段通常和在线表保持一致，并额外增加归档时间、归档批次号等字段。

```sql
CREATE TABLE `mall_order_online` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已发货，3已完成，4已取消，5已关闭',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0未支付，1已支付，2已退款，3部分退款',
  `pay_type` TINYINT NULL COMMENT '支付方式：1余额，2微信，3支付宝，4银行卡',
  `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  `discount_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  `pay_time` DATETIME NULL COMMENT '支付时间',
  `finish_time` DATETIME NULL COMMENT '完成时间',
  `cancel_time` DATETIME NULL COMMENT '取消时间',
  `close_time` DATETIME NULL COMMENT '关闭时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status_deleted` (`user_id`, `order_status`, `is_deleted`),
  KEY `idx_status_created` (`order_status`, `created_at`),
  KEY `idx_finish_time` (`finish_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='商城订单在线表';

CREATE TABLE `mall_order_archive` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已发货，3已完成，4已取消，5已关闭',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0未支付，1已支付，2已退款，3部分退款',
  `pay_type` TINYINT NULL COMMENT '支付方式：1余额，2微信，3支付宝，4银行卡',
  `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  `discount_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  `pay_time` DATETIME NULL COMMENT '支付时间',
  `finish_time` DATETIME NULL COMMENT '完成时间',
  `cancel_time` DATETIME NULL COMMENT '取消时间',
  `close_time` DATETIME NULL COMMENT '关闭时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  `archive_batch_no` VARCHAR(64) NOT NULL COMMENT '归档批次号',
  `archived_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_status_created` (`order_status`, `created_at`),
  KEY `idx_archive_batch` (`archive_batch_no`),
  KEY `idx_archived_at` (`archived_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='商城订单归档表';
```

### 字段设计说明

归档数据模型的核心是区分在线表和归档表。在线表保存近期、高频访问、仍可能发生状态变化的数据；归档表保存历史、低频访问、基本不会再变更的数据。

| 表                   | 字段               | 说明                             |
| -------------------- | ------------------ | -------------------------------- |
| `mall_order_online`  | `id`               | 在线订单主键                     |
| `mall_order_online`  | `order_no`         | 在线订单编号                     |
| `mall_order_online`  | `finish_time`      | 订单完成时间，可作为归档判断条件 |
| `mall_order_archive` | `archive_batch_no` | 归档批次号                       |
| `mall_order_archive` | `archived_at`      | 归档时间                         |
| `mall_order_archive` | `created_at`       | 原始订单创建时间                 |

### 查询可归档数据

下面的 SQL 用于查询满足归档条件的订单。通常只归档已完成、已取消、已关闭且超过一定时间的数据。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_amount`,
  `created_at`,
  `finish_time`,
  `cancel_time`,
  `close_time`
FROM `mall_order_online`
WHERE `order_status` IN (3, 4, 5)
  AND `created_at` < DATE_SUB(NOW(), INTERVAL 1 YEAR)
ORDER BY `created_at` ASC
LIMIT 1000;
```

### 插入归档表

下面的 SQL 用于将满足条件的在线订单写入归档表。归档批次号用于后续校验、追踪和回滚。

```sql
INSERT INTO `mall_order_archive` (
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_type`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `pay_time`,
  `finish_time`,
  `cancel_time`,
  `close_time`,
  `remark`,
  `is_deleted`,
  `created_by`,
  `created_at`,
  `updated_by`,
  `updated_at`,
  `archive_batch_no`,
  `archived_at`
)
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_type`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `pay_time`,
  `finish_time`,
  `cancel_time`,
  `close_time`,
  `remark`,
  `is_deleted`,
  `created_by`,
  `created_at`,
  `updated_by`,
  `updated_at`,
  'ARCHIVE_202605110001',
  NOW()
FROM `mall_order_online`
WHERE `order_status` IN (3, 4, 5)
  AND `created_at` < DATE_SUB(NOW(), INTERVAL 1 YEAR)
ORDER BY `created_at` ASC
LIMIT 1000;
```

### 校验归档数量

下面的 SQL 用于校验某个归档批次写入归档表的数据量。

```sql
SELECT
  `archive_batch_no`,
  COUNT(*) AS `archive_count`,
  MIN(`created_at`) AS `min_created_at`,
  MAX(`created_at`) AS `max_created_at`,
  MIN(`archived_at`) AS `min_archived_at`,
  MAX(`archived_at`) AS `max_archived_at`
FROM `mall_order_archive`
WHERE `archive_batch_no` = 'ARCHIVE_202605110001'
GROUP BY `archive_batch_no`;
```

### 校验归档金额

下面的 SQL 用于校验某个归档批次的订单金额汇总。

```sql
SELECT
  `archive_batch_no`,
  COUNT(*) AS `order_count`,
  SUM(`total_amount`) AS `total_amount`,
  SUM(`discount_amount`) AS `discount_amount`,
  SUM(`pay_amount`) AS `pay_amount`
FROM `mall_order_archive`
WHERE `archive_batch_no` = 'ARCHIVE_202605110001'
GROUP BY `archive_batch_no`;
```

### 删除在线表已归档数据

下面的 SQL 用于删除已经成功归档的在线表数据。生产环境通常建议先完成归档校验，再执行删除。

```sql
DELETE o
FROM `mall_order_online` o
JOIN `mall_order_archive` a
  ON o.`id` = a.`id`
WHERE a.`archive_batch_no` = 'ARCHIVE_202605110001';
```

### 使用事务归档数据

下面的 SQL 演示小批量归档时的事务写法。大批量归档不建议单个事务过大，应分批执行。

```sql
START TRANSACTION;

INSERT INTO `mall_order_archive` (
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_type`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `pay_time`,
  `finish_time`,
  `cancel_time`,
  `close_time`,
  `remark`,
  `is_deleted`,
  `created_by`,
  `created_at`,
  `updated_by`,
  `updated_at`,
  `archive_batch_no`,
  `archived_at`
)
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_type`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `pay_time`,
  `finish_time`,
  `cancel_time`,
  `close_time`,
  `remark`,
  `is_deleted`,
  `created_by`,
  `created_at`,
  `updated_by`,
  `updated_at`,
  'ARCHIVE_202605110002',
  NOW()
FROM `mall_order_online`
WHERE `order_status` IN (3, 4, 5)
  AND `created_at` < '2025-01-01 00:00:00'
ORDER BY `created_at` ASC
LIMIT 500;

DELETE o
FROM `mall_order_online` o
JOIN `mall_order_archive` a
  ON o.`id` = a.`id`
WHERE a.`archive_batch_no` = 'ARCHIVE_202605110002';

COMMIT;
```

### 查询在线订单

下面的 SQL 用于查询近期在线订单。在线业务查询只查在线表，避免扫描归档表。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_amount`,
  `created_at`
FROM `mall_order_online`
WHERE `user_id` = 200001
  AND `is_deleted` = 0
ORDER BY `created_at` DESC
LIMIT 20 OFFSET 0;
```

### 查询历史归档订单

下面的 SQL 用于查询历史订单。历史查询走归档表。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_amount`,
  `created_at`,
  `archived_at`
FROM `mall_order_archive`
WHERE `user_id` = 200001
  AND `created_at` >= '2024-01-01 00:00:00'
  AND `created_at` < '2025-01-01 00:00:00'
ORDER BY `created_at` DESC
LIMIT 20 OFFSET 0;
```

### 查询在线和归档订单

下面的 SQL 用于同时查询在线表和归档表。该方式适合历史查询入口，不建议用于高频核心业务接口。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_amount`,
  `created_at`,
  'online' AS `data_source`
FROM `mall_order_online`
WHERE `user_id` = 200001
  AND `created_at` >= '2024-01-01 00:00:00'
  AND `created_at` < '2026-06-01 00:00:00'

UNION ALL

SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_amount`,
  `created_at`,
  'archive' AS `data_source`
FROM `mall_order_archive`
WHERE `user_id` = 200001
  AND `created_at` >= '2024-01-01 00:00:00'
  AND `created_at` < '2026-06-01 00:00:00'

ORDER BY `created_at` DESC
LIMIT 20 OFFSET 0;
```

### 根据订单编号查询历史订单

下面的 SQL 用于根据订单编号查询订单，优先查在线表，再查归档表。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_amount`,
  `created_at`,
  'online' AS `data_source`
FROM `mall_order_online`
WHERE `order_no` = 'ORDER_202405110001'

UNION ALL

SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_amount`,
  `created_at`,
  'archive' AS `data_source`
FROM `mall_order_archive`
WHERE `order_no` = 'ORDER_202405110001';
```

### 归档数据恢复

下面的 SQL 用于将某个归档批次恢复到在线表。恢复前需要确认在线表不存在相同主键或订单编号。

```sql
INSERT INTO `mall_order_online` (
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `pay_type`,
  `total_amount`,
  `discount_amount`,
  `pay_amount`,
  `pay_time`,
  `finish_time`,
  `cancel_time`,
  `close_time`,
  `remark`,
  `is_deleted`,
  `created_by`,
  `created_at`,
  `updated_by`,
  `updated_at`
)
SELECT
  a.`id`,
  a.`order_no`,
  a.`user_id`,
  a.`order_status`,
  a.`pay_status`,
  a.`pay_type`,
  a.`total_amount`,
  a.`discount_amount`,
  a.`pay_amount`,
  a.`pay_time`,
  a.`finish_time`,
  a.`cancel_time`,
  a.`close_time`,
  a.`remark`,
  a.`is_deleted`,
  a.`created_by`,
  a.`created_at`,
  a.`updated_by`,
  a.`updated_at`
FROM `mall_order_archive` a
LEFT JOIN `mall_order_online` o
  ON a.`id` = o.`id`
WHERE a.`archive_batch_no` = 'ARCHIVE_202605110001'
  AND o.`id` IS NULL;
```

### 删除已恢复归档数据

下面的 SQL 用于恢复确认成功后清理归档表中的指定批次数据。

```sql
DELETE FROM `mall_order_archive`
WHERE `archive_batch_no` = 'ARCHIVE_202605110001';
```

### 查询归档批次列表

下面的 SQL 用于查询归档批次，用于后台归档任务管理页面。

```sql
SELECT
  `archive_batch_no`,
  COUNT(*) AS `archive_count`,
  MIN(`created_at`) AS `min_created_at`,
  MAX(`created_at`) AS `max_created_at`,
  MIN(`archived_at`) AS `start_archived_at`,
  MAX(`archived_at`) AS `end_archived_at`
FROM `mall_order_archive`
GROUP BY `archive_batch_no`
ORDER BY `end_archived_at` DESC
LIMIT 20 OFFSET 0;
```

### 查询归档异常数据

下面的 SQL 用于检查在线表和归档表中同时存在的数据。

```sql
SELECT
  o.`id`,
  o.`order_no`,
  o.`created_at` AS `online_created_at`,
  a.`archived_at`
FROM `mall_order_online` o
JOIN `mall_order_archive` a
  ON o.`id` = a.`id`
ORDER BY a.`archived_at` DESC
LIMIT 50;
```

### 清理超长期归档数据

下面的 SQL 用于清理归档表中超过保留周期的数据。生产环境通常应先备份或转储到冷存储。

```sql
DELETE FROM `mall_order_archive`
WHERE `created_at` < DATE_SUB(NOW(), INTERVAL 5 YEAR);
```

### 索引设计建议

归档数据模型的索引要区分在线查询和历史查询。在线表索引服务高频业务，归档表索引服务历史检索、批次查询和审计查询。

| 表                   | 索引                                                         | 适用场景                 |
| -------------------- | ------------------------------------------------------------ | ------------------------ |
| `mall_order_online`  | `PRIMARY KEY (id)`                                           | 在线订单详情             |
| `mall_order_online`  | `uk_order_no (order_no)`                                     | 根据订单编号查询在线订单 |
| `mall_order_online`  | `idx_user_status_deleted (user_id, order_status, is_deleted)` | 用户订单列表             |
| `mall_order_online`  | `idx_status_created (order_status, created_at)`              | 查询可归档数据           |
| `mall_order_online`  | `idx_finish_time (finish_time)`                              | 按完成时间归档           |
| `mall_order_archive` | `PRIMARY KEY (id)`                                           | 归档订单详情             |
| `mall_order_archive` | `uk_order_no (order_no)`                                     | 根据订单编号查询归档订单 |
| `mall_order_archive` | `idx_user_created (user_id, created_at)`                     | 用户历史订单查询         |
| `mall_order_archive` | `idx_archive_batch (archive_batch_no)`                       | 按归档批次查询           |
| `mall_order_archive` | `idx_archived_at (archived_at)`                              | 查询归档任务历史         |

### 使用建议

归档数据模型的核心是让在线表保持轻量，让历史数据可查、可校验、可恢复。归档不是简单删除历史数据，而是一套完整的数据迁移、校验、清理和恢复机制。

使用归档数据模型时重点注意：

- 在线表只保留近期或高频访问数据
- 归档表保存历史低频访问数据
- 归档前必须明确归档条件，例如完成时间、订单状态、保留周期
- 归档任务建议分批执行，避免大事务
- 写入归档表后应先校验数量和金额，再删除在线表数据
- 归档表应保留归档批次号和归档时间
- 历史查询入口可以同时查在线表和归档表
- 核心业务接口不建议频繁 `UNION` 在线表和归档表
- 归档数据应支持恢复
- 超长期归档数据可以进一步转储到冷存储
- 归档数据清理前应确认合规和审计保留要求

## 分区表模型

分区表模型用于把一张大表按照一定规则拆分成多个物理分区，逻辑上仍然是一张表。MySQL 8 常用分区方式包括按时间范围分区、按数值范围分区、按哈希分区等。业务中最常见的是按时间范围分区，适合日志、流水、订单、统计明细等数据量持续增长的表。

下面以“操作日志分区表”为例说明分区表模型的建表方式和常用 SQL 实践。

### 适用场景

分区表模型适合单表数据量很大，并且查询、清理、归档经常按时间或固定范围进行的场景。

常见场景包括：

- 操作日志表
- 审计日志表
- 登录日志表
- 账户流水表
- 支付流水表
- 订单历史表
- 消息记录表
- 任务执行日志表
- 监控指标表
- 统计明细表

### 建表 SQL

下面的 SQL 创建按月分区的操作日志表。MySQL 分区表有一个重要限制：所有唯一键都必须包含分区字段。因此这里主键设计为 `(id, operate_time)`。

```sql
CREATE TABLE `sys_operation_log_partition` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `trace_id` VARCHAR(128) NULL COMMENT '链路追踪ID',
  `operator_id` BIGINT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(128) NULL COMMENT '操作人名称',
  `operation_module` VARCHAR(128) NOT NULL COMMENT '操作模块',
  `operation_type` VARCHAR(64) NOT NULL COMMENT '操作类型',
  `request_method` VARCHAR(16) NULL COMMENT '请求方式',
  `request_uri` VARCHAR(500) NULL COMMENT '请求地址',
  `client_ip` VARCHAR(64) NULL COMMENT '客户端IP',
  `success_status` TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功：0失败，1成功',
  `cost_millis` BIGINT NOT NULL DEFAULT 0 COMMENT '耗时，单位毫秒',
  `error_message` TEXT NULL COMMENT '异常信息',
  `operate_time` DATETIME NOT NULL COMMENT '操作时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`, `operate_time`),
  KEY `idx_operator_time` (`operator_id`, `operate_time`),
  KEY `idx_module_type_time` (`operation_module`, `operation_type`, `operate_time`),
  KEY `idx_status_time` (`success_status`, `operate_time`),
  KEY `idx_uri_time` (`request_uri`, `operate_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统操作日志分区表'
PARTITION BY RANGE COLUMNS (`operate_time`) (
  PARTITION p202601 VALUES LESS THAN ('2026-02-01'),
  PARTITION p202602 VALUES LESS THAN ('2026-03-01'),
  PARTITION p202603 VALUES LESS THAN ('2026-04-01'),
  PARTITION p202604 VALUES LESS THAN ('2026-05-01'),
  PARTITION p202605 VALUES LESS THAN ('2026-06-01'),
  PARTITION p202606 VALUES LESS THAN ('2026-07-01'),
  PARTITION pmax VALUES LESS THAN (MAXVALUE)
);
```

### 字段设计说明

分区表模型的核心是分区字段。分区字段应尽量出现在高频查询条件中，否则查询可能扫描多个分区，无法发挥分区裁剪效果。

| 字段               | 说明                            |
| ------------------ | ------------------------------- |
| `operate_time`     | 分区字段，按月范围分区          |
| `id`               | 日志 ID，和分区字段组成联合主键 |
| `operator_id`      | 操作人 ID                       |
| `operation_module` | 操作模块                        |
| `success_status`   | 成功失败状态                    |
| `created_at`       | 记录创建时间                    |

### 新增日志数据

下面的 SQL 用于插入操作日志。MySQL 会根据 `operate_time` 自动写入对应分区。

```sql
INSERT INTO `sys_operation_log_partition` (
  `id`,
  `trace_id`,
  `operator_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `request_method`,
  `request_uri`,
  `client_ip`,
  `success_status`,
  `cost_millis`,
  `error_message`,
  `operate_time`
) VALUES (
  100001,
  'TRACE_202605110001',
  200001,
  '系统管理员',
  '用户管理',
  'create',
  'POST',
  '/system/user',
  '192.168.1.10',
  1,
  126,
  NULL,
  '2026-05-11 10:00:00'
);
```

### 按分区字段查询

下面的 SQL 用于查询某个月的日志。查询条件包含 `operate_time`，可以触发分区裁剪。

```sql
SELECT
  `id`,
  `trace_id`,
  `operator_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `success_status`,
  `cost_millis`,
  `operate_time`
FROM `sys_operation_log_partition`
WHERE `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
ORDER BY `operate_time` DESC
LIMIT 50 OFFSET 0;
```

### 按操作人查询分区数据

下面的 SQL 用于查询指定操作人在某段时间内的操作日志。查询条件同时包含操作人和分区字段。

```sql
SELECT
  `id`,
  `operator_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `request_uri`,
  `success_status`,
  `operate_time`
FROM `sys_operation_log_partition`
WHERE `operator_id` = 200001
  AND `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
ORDER BY `operate_time` DESC
LIMIT 20 OFFSET 0;
```

### 查询失败日志

下面的 SQL 用于查询某段时间内的失败日志。因为包含 `operate_time`，可以避免扫描所有分区。

```sql
SELECT
  `id`,
  `trace_id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `request_uri`,
  `error_message`,
  `operate_time`
FROM `sys_operation_log_partition`
WHERE `success_status` = 0
  AND `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
ORDER BY `operate_time` DESC
LIMIT 50 OFFSET 0;
```

### 查看分区信息

下面的 SQL 用于查看表的分区信息，包括分区名称、分区行数估算和分区边界。

```sql
SELECT
  `TABLE_SCHEMA`,
  `TABLE_NAME`,
  `PARTITION_NAME`,
  `PARTITION_METHOD`,
  `PARTITION_EXPRESSION`,
  `PARTITION_DESCRIPTION`,
  `TABLE_ROWS`
FROM `information_schema`.`PARTITIONS`
WHERE `TABLE_SCHEMA` = DATABASE()
  AND `TABLE_NAME` = 'sys_operation_log_partition'
ORDER BY `PARTITION_ORDINAL_POSITION`;
```

### 查看查询分区裁剪

下面的 SQL 用于查看查询执行计划，确认是否只访问目标分区。

```sql
EXPLAIN
SELECT
  `id`,
  `operation_module`,
  `operation_type`,
  `operate_time`
FROM `sys_operation_log_partition`
WHERE `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00';
```

### 新增未来分区

下面的 SQL 用于新增未来月份分区。由于存在 `pmax` 分区，需要使用 `REORGANIZE PARTITION` 拆分最大分区。

```sql
ALTER TABLE `sys_operation_log_partition`
REORGANIZE PARTITION pmax INTO (
  PARTITION p202607 VALUES LESS THAN ('2026-08-01'),
  PARTITION pmax VALUES LESS THAN (MAXVALUE)
);
```

### 删除历史分区

下面的 SQL 用于快速删除历史分区。删除分区会直接删除该分区内所有数据，执行前必须确认数据已经归档或不再需要。

```sql
ALTER TABLE `sys_operation_log_partition`
DROP PARTITION p202601;
```

### 清空指定分区

下面的 SQL 用于清空指定分区中的数据，但保留分区结构。

```sql
ALTER TABLE `sys_operation_log_partition`
TRUNCATE PARTITION p202602;
```

### 查询指定分区数据

下面的 SQL 用于直接查询某个分区的数据，适合排查分区内数据情况。

```sql
SELECT
  `id`,
  `operator_name`,
  `operation_module`,
  `operation_type`,
  `operate_time`
FROM `sys_operation_log_partition` PARTITION (p202605)
ORDER BY `operate_time` DESC
LIMIT 20;
```

### 按天统计分区数据

下面的 SQL 用于按天统计某个月的操作日志数量。

```sql
SELECT
  DATE(`operate_time`) AS `stat_date`,
  COUNT(*) AS `log_count`,
  SUM(CASE WHEN `success_status` = 1 THEN 1 ELSE 0 END) AS `success_count`,
  SUM(CASE WHEN `success_status` = 0 THEN 1 ELSE 0 END) AS `fail_count`
FROM `sys_operation_log_partition`
WHERE `operate_time` >= '2026-05-01 00:00:00'
  AND `operate_time` < '2026-06-01 00:00:00'
GROUP BY DATE(`operate_time`)
ORDER BY `stat_date` ASC;
```

### 归档分区数据

下面的 SQL 用于将指定月份数据写入归档表。生产环境通常在确认归档成功后再删除分区。

```sql
INSERT INTO `sys_operation_log_archive`
SELECT *
FROM `sys_operation_log_partition`
WHERE `operate_time` >= '2026-01-01 00:00:00'
  AND `operate_time` < '2026-02-01 00:00:00';
```

### 校验分区归档数量

下面的 SQL 用于校验分区表和归档表在指定时间段内的数据数量是否一致。

```sql
SELECT
  'source' AS `data_source`,
  COUNT(*) AS `log_count`
FROM `sys_operation_log_partition`
WHERE `operate_time` >= '2026-01-01 00:00:00'
  AND `operate_time` < '2026-02-01 00:00:00'

UNION ALL

SELECT
  'archive' AS `data_source`,
  COUNT(*) AS `log_count`
FROM `sys_operation_log_archive`
WHERE `operate_time` >= '2026-01-01 00:00:00'
  AND `operate_time` < '2026-02-01 00:00:00';
```

### 哈希分区表示例

下面的 SQL 演示按用户 ID 哈希分区。哈希分区适合按某个 ID 均匀分布数据，但不适合按时间快速删除历史数据。

```sql
CREATE TABLE `user_event_hash_partition` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `event_type` VARCHAR(64) NOT NULL COMMENT '事件类型',
  `event_content` JSON NULL COMMENT '事件内容',
  `event_time` DATETIME NOT NULL COMMENT '事件时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`, `user_id`),
  KEY `idx_user_time` (`user_id`, `event_time`),
  KEY `idx_event_time` (`event_type`, `event_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户事件哈希分区表'
PARTITION BY HASH (`user_id`)
PARTITIONS 8;
```

### 索引设计建议

分区表的索引设计要同时考虑分区字段和普通查询字段。分区不等于索引，分区只能减少扫描分区范围，具体分区内部仍然需要合适索引。

| 索引                                                         | 适用场景                               |
| ------------------------------------------------------------ | -------------------------------------- |
| `PRIMARY KEY (id, operate_time)`                             | 满足分区表唯一键必须包含分区字段的要求 |
| `idx_operator_time (operator_id, operate_time)`              | 查询某个操作人在指定时间段内的日志     |
| `idx_module_type_time (operation_module, operation_type, operate_time)` | 按模块、类型和时间查询                 |
| `idx_status_time (success_status, operate_time)`             | 查询失败日志                           |
| `idx_uri_time (request_uri, operate_time)`                   | 查询接口访问日志                       |

### 使用建议

分区表模型的核心是选择合适的分区字段。分区字段必须和高频查询条件、归档条件、清理条件高度一致，否则分区效果有限。

使用分区表模型时重点注意：

- 分区字段应出现在高频查询条件中
- 时间类大表优先考虑按时间范围分区
- 分区表不是索引替代品，分区内仍需要索引
- MySQL 分区表的唯一键必须包含分区字段
- 删除历史数据可以通过 `DROP PARTITION` 快速完成
- 新增未来分区应提前规划，避免数据落入 `pmax`
- 分区数量不宜过多，避免维护成本过高
- 分区表适合按时间清理和归档，不适合解决所有慢查询
- 分区字段设计错误后，后续调整成本较高
- 使用分区前应先确认业务查询是否能触发分区裁剪

## 分库分表模型

分库分表模型用于解决单库、单表数据量过大或写入压力过高的问题。它通过分片键将数据拆分到多个数据库或多张物理表中。常见方式包括只分表、只分库、分库分表。业务中最常用的是按用户 ID、订单 ID、租户 ID、商户 ID 等稳定字段进行哈希分片。

下面以“订单分库分表”为例说明分库分表模型的建表方式和常用 SQL 实践。

### 适用场景

分库分表模型适合单表数据量持续增长、单库写入压力过高、单表索引膨胀、历史数据归档仍无法满足性能要求的场景。

常见场景包括：

- 超大订单表
- 超大订单明细表
- 支付流水表
- 账户流水表
- 消息记录表
- 用户行为表
- IoT 设备数据表
- 多租户超大业务表
- 日志明细表
- 高频交易明细表

### 建表 SQL

下面的 SQL 演示订单分表结构。假设逻辑表为 `sharding_order`，物理表拆成 `sharding_order_00`、`sharding_order_01`、`sharding_order_02`、`sharding_order_03` 四张表。

```sql
CREATE TABLE `sharding_order_00` (
  `id` BIGINT NOT NULL COMMENT '主键ID，全局唯一',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号，全局唯一',
  `user_id` BIGINT NOT NULL COMMENT '用户ID，分片键',
  `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已发货，3已完成，4已取消',
  `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0未支付，1已支付，2已退款',
  `total_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  `pay_time` DATETIME NULL COMMENT '支付时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` BIGINT NULL COMMENT '创建人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status_deleted` (`user_id`, `order_status`, `is_deleted`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='分片订单表00';

CREATE TABLE `sharding_order_01` LIKE `sharding_order_00`;
CREATE TABLE `sharding_order_02` LIKE `sharding_order_00`;
CREATE TABLE `sharding_order_03` LIKE `sharding_order_00`;
```

### 订单明细分表 SQL

下面的 SQL 演示订单明细分表结构。订单明细建议和订单主表使用相同分片键，例如 `user_id` 或 `order_id`，以便同一订单及明细尽量落在同一分片。

```sql
CREATE TABLE `sharding_order_item_00` (
  `id` BIGINT NOT NULL COMMENT '主键ID，全局唯一',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID，分片键',
  `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
  `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称快照',
  `unit_price` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '商品单价',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `pay_amount` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '明细实付金额',
  `item_status` TINYINT NOT NULL DEFAULT 0 COMMENT '明细状态：0正常，1退款中，2已退款',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id_deleted` (`order_id`, `is_deleted`),
  KEY `idx_order_no_deleted` (`order_no`, `is_deleted`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='分片订单明细表00';

CREATE TABLE `sharding_order_item_01` LIKE `sharding_order_item_00`;
CREATE TABLE `sharding_order_item_02` LIKE `sharding_order_item_00`;
CREATE TABLE `sharding_order_item_03` LIKE `sharding_order_item_00`;
```

### 分片路由表 SQL

下面的 SQL 创建订单路由表。路由表用于根据订单编号快速定位分片，适合订单编号查询、客服查询、支付回调等无法直接拿到分片键的场景。

```sql
CREATE TABLE `sharding_order_route` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `database_index` INT NOT NULL DEFAULT 0 COMMENT '数据库分片编号',
  `table_index` INT NOT NULL COMMENT '表分片编号',
  `table_name` VARCHAR(128) NOT NULL COMMENT '物理表名',
  `route_status` TINYINT NOT NULL DEFAULT 1 COMMENT '路由状态：0无效，1有效',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_table_index` (`database_index`, `table_index`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='订单分片路由表';
```

### 字段设计说明

分库分表模型的核心是分片键和路由规则。分片键决定数据落在哪个库、哪张表。分片键一旦选定，后续调整成本很高。

| 字段             | 说明                           |
| ---------------- | ------------------------------ |
| `id`             | 全局唯一主键，不能依赖单库自增 |
| `order_no`       | 全局唯一订单编号               |
| `user_id`        | 分片键示例                     |
| `database_index` | 数据库分片编号                 |
| `table_index`    | 表分片编号                     |
| `table_name`     | 物理表名                       |
| `route_status`   | 路由状态                       |

### 分片规则示例

下面的规则表示按 `user_id` 取模分表。实际项目中通常由分库分表中间件或应用层路由实现。

```sql
-- 表分片编号 = user_id % 4
-- user_id = 200001 时，200001 % 4 = 1
-- 数据写入 sharding_order_01 和 sharding_order_item_01
```

### 新增订单到指定分片

下面的 SQL 演示用户 ID 为 `200001` 的订单写入 `sharding_order_01`。

```sql
INSERT INTO `sharding_order_01` (
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `total_amount`,
  `pay_amount`,
  `remark`,
  `created_by`,
  `updated_by`
) VALUES (
  900000000001,
  'ORDER_202605110001',
  200001,
  0,
  0,
  399.00,
  369.00,
  '分库分表示例订单',
  200001,
  200001
);
```

### 新增订单明细到相同分片

下面的 SQL 演示订单明细写入和订单主表相同的分片表。

```sql
INSERT INTO `sharding_order_item_01` (
  `id`,
  `order_id`,
  `order_no`,
  `user_id`,
  `sku_id`,
  `sku_name`,
  `unit_price`,
  `quantity`,
  `pay_amount`,
  `item_status`
) VALUES (
  910000000001,
  900000000001,
  'ORDER_202605110001',
  200001,
  300001,
  '机械键盘 黑色 青轴',
  399.00,
  1,
  369.00,
  0
);
```

### 写入订单路由表

下面的 SQL 用于写入订单路由信息，便于后续根据订单编号定位物理表。

```sql
INSERT INTO `sharding_order_route` (
  `id`,
  `order_no`,
  `order_id`,
  `user_id`,
  `database_index`,
  `table_index`,
  `table_name`,
  `route_status`
) VALUES (
  920000000001,
  'ORDER_202605110001',
  900000000001,
  200001,
  0,
  1,
  'sharding_order_01',
  1
);
```

### 根据分片键查询订单

下面的 SQL 用于根据用户 ID 查询订单。因为已经知道分片键，可以直接路由到对应物理表。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `total_amount`,
  `pay_amount`,
  `created_at`
FROM `sharding_order_01`
WHERE `user_id` = 200001
  AND `is_deleted` = 0
ORDER BY `created_at` DESC
LIMIT 20 OFFSET 0;
```

### 根据订单编号查询路由

下面的 SQL 用于根据订单编号查询路由信息。

```sql
SELECT
  `order_no`,
  `order_id`,
  `user_id`,
  `database_index`,
  `table_index`,
  `table_name`
FROM `sharding_order_route`
WHERE `order_no` = 'ORDER_202605110001'
  AND `route_status` = 1;
```

### 根据路由查询订单

下面的 SQL 用于根据路由结果查询具体物理表中的订单数据。实际应用中表名通常由代码或中间件拼接，不建议直接让用户输入表名。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_status`,
  `total_amount`,
  `pay_amount`,
  `created_at`
FROM `sharding_order_01`
WHERE `order_no` = 'ORDER_202605110001'
  AND `is_deleted` = 0;
```

### 查询订单明细

下面的 SQL 用于查询订单明细。由于订单和明细使用相同分片键，明细查询也能路由到同一分片。

```sql
SELECT
  `id`,
  `order_id`,
  `order_no`,
  `sku_id`,
  `sku_name`,
  `unit_price`,
  `quantity`,
  `pay_amount`,
  `item_status`
FROM `sharding_order_item_01`
WHERE `order_id` = 900000000001
  AND `is_deleted` = 0
ORDER BY `id` ASC;
```

### 支付订单

下面的 SQL 用于更新指定分片中的订单状态。状态更新应带上原状态条件，防止重复支付。

```sql
UPDATE `sharding_order_01`
SET
  `order_status` = 1,
  `pay_status` = 1,
  `pay_time` = NOW(),
  `updated_by` = 200001
WHERE `order_no` = 'ORDER_202605110001'
  AND `order_status` = 0
  AND `pay_status` = 0
  AND `is_deleted` = 0;
```

### 查询多个分片汇总数据

下面的 SQL 演示手动查询多个分片并合并结果。实际项目中通常由分库分表中间件完成聚合。

```sql
SELECT
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_amount`,
  `created_at`
FROM `sharding_order_00`
WHERE `created_at` >= '2026-05-01 00:00:00'
  AND `created_at` < '2026-06-01 00:00:00'

UNION ALL

SELECT
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_amount`,
  `created_at`
FROM `sharding_order_01`
WHERE `created_at` >= '2026-05-01 00:00:00'
  AND `created_at` < '2026-06-01 00:00:00'

UNION ALL

SELECT
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_amount`,
  `created_at`
FROM `sharding_order_02`
WHERE `created_at` >= '2026-05-01 00:00:00'
  AND `created_at` < '2026-06-01 00:00:00'

UNION ALL

SELECT
  `order_no`,
  `user_id`,
  `order_status`,
  `pay_amount`,
  `created_at`
FROM `sharding_order_03`
WHERE `created_at` >= '2026-05-01 00:00:00'
  AND `created_at` < '2026-06-01 00:00:00';
```

### 统计各分片数据量

下面的 SQL 用于统计每张分片表的数据量，适合检查分片是否均衡。

```sql
SELECT 'sharding_order_00' AS `table_name`, COUNT(*) AS `row_count` FROM `sharding_order_00`
UNION ALL
SELECT 'sharding_order_01' AS `table_name`, COUNT(*) AS `row_count` FROM `sharding_order_01`
UNION ALL
SELECT 'sharding_order_02' AS `table_name`, COUNT(*) AS `row_count` FROM `sharding_order_02`
UNION ALL
SELECT 'sharding_order_03' AS `table_name`, COUNT(*) AS `row_count` FROM `sharding_order_03`;
```

### 统计各分片金额

下面的 SQL 用于统计每个分片中的订单金额，适合检查数据分布和对账。

```sql
SELECT 'sharding_order_00' AS `table_name`, SUM(`pay_amount`) AS `pay_amount` FROM `sharding_order_00`
UNION ALL
SELECT 'sharding_order_01' AS `table_name`, SUM(`pay_amount`) AS `pay_amount` FROM `sharding_order_01`
UNION ALL
SELECT 'sharding_order_02' AS `table_name`, SUM(`pay_amount`) AS `pay_amount` FROM `sharding_order_02`
UNION ALL
SELECT 'sharding_order_03' AS `table_name`, SUM(`pay_amount`) AS `pay_amount` FROM `sharding_order_03`;
```

### 校验路由表和分片表

下面的 SQL 用于校验路由表中的订单是否存在于对应物理表。这里以 `sharding_order_01` 为例。

```sql
SELECT
  r.`order_no`,
  r.`order_id`,
  r.`table_name`
FROM `sharding_order_route` r
LEFT JOIN `sharding_order_01` o
  ON r.`order_id` = o.`id`
WHERE r.`table_name` = 'sharding_order_01'
  AND r.`route_status` = 1
  AND o.`id` IS NULL;
```

### 查询缺失路由的数据

下面的 SQL 用于检查分片表中存在但路由表缺失的数据。这里以 `sharding_order_01` 为例。

```sql
SELECT
  o.`id`,
  o.`order_no`,
  o.`user_id`,
  o.`created_at`
FROM `sharding_order_01` o
LEFT JOIN `sharding_order_route` r
  ON o.`order_no` = r.`order_no`
WHERE r.`id` IS NULL;
```

### 逻辑删除分片订单

下面的 SQL 用于逻辑删除指定分片中的订单。

```sql
UPDATE `sharding_order_01`
SET
  `is_deleted` = 1,
  `updated_by` = 200001
WHERE `order_no` = 'ORDER_202605110001'
  AND `user_id` = 200001
  AND `is_deleted` = 0;
```

### 逻辑删除路由记录

下面的 SQL 用于订单逻辑删除后同步禁用路由记录。

```sql
UPDATE `sharding_order_route`
SET
  `route_status` = 0
WHERE `order_no` = 'ORDER_202605110001'
  AND `route_status` = 1;
```

### 分库表示例

下面的 SQL 用于说明分库场景下的物理库命名和物理表命名。实际建表需要分别在不同数据库中执行。

```sql
-- 逻辑库：mall_order_db
-- 物理库：mall_order_db_00、mall_order_db_01
-- 逻辑表：sharding_order
-- 物理表：sharding_order_00、sharding_order_01、sharding_order_02、sharding_order_03

-- 数据库分片编号 = user_id % 2
-- 表分片编号 = user_id % 4
```

### 索引设计建议

分库分表模型的索引设计仍然要围绕单个物理表的查询场景。分片键必须进入高频查询条件，否则容易出现广播查询。

| 表                       | 索引                                                         | 适用场景               |
| ------------------------ | ------------------------------------------------------------ | ---------------------- |
| `sharding_order_00`      | `PRIMARY KEY (id)`                                           | 根据全局 ID 查询       |
| `sharding_order_00`      | `uk_order_no (order_no)`                                     | 单表内订单编号唯一     |
| `sharding_order_00`      | `idx_user_status_deleted (user_id, order_status, is_deleted)` | 根据分片键查询用户订单 |
| `sharding_order_00`      | `idx_created_at (created_at)`                                | 单分片按时间查询       |
| `sharding_order_item_00` | `idx_order_id_deleted (order_id, is_deleted)`                | 查询订单明细           |
| `sharding_order_item_00` | `idx_user_created (user_id, created_at)`                     | 用户维度明细查询       |
| `sharding_order_route`   | `uk_order_no (order_no)`                                     | 根据订单编号查路由     |
| `sharding_order_route`   | `idx_order_id (order_id)`                                    | 根据订单 ID 查路由     |
| `sharding_order_route`   | `idx_user_id (user_id)`                                      | 根据用户 ID 查询路由   |

### 使用建议

分库分表模型的核心是分片键选择。分片键应稳定、离散度高、查询中经常出现，并且尽量能让相关数据落在同一分片。分库分表会明显增加系统复杂度，应在单表优化、归档、分区、读写分离都不足以解决问题时再使用。

使用分库分表模型时重点注意：

- 分片键一旦确定，后续调整成本很高
- 主键必须全局唯一，不能依赖单表自增
- 高频查询必须尽量带上分片键
- 不带分片键的查询可能变成广播查询
- 跨分片分页、排序、聚合成本较高
- 订单和订单明细应尽量使用同一分片键
- 根据订单编号查询时可以使用路由表
- 分库分表后数据库唯一约束只能保证单分片唯一，全局唯一需要业务保证
- 分布式事务成本较高，应尽量让同一业务操作落在同一分片
- 扩容和迁移需要提前设计路由策略
- 分库分表不是首选方案，优先考虑索引优化、归档、分区和缓存
- 分库分表后运维、排查、统计和数据修复都会更复杂
