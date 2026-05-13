# 统计、搜索与查询优化模型

统计、搜索与查询优化模型用于解决业务系统中高频统计、复杂搜索、查询性能优化和读写压力拆分问题。该类模型通常不直接替代核心业务表，而是在核心业务数据之外构建面向查询、统计或读取性能的辅助数据结构。

## 统计汇总模型

统计汇总模型用于将明细业务数据按照业务维度、时间维度或状态维度提前聚合，减少在线查询时对大表的实时扫描、分组和计算压力。该模型常用于订单统计、账户统计、库存统计、用户行为统计、报表看板和运营分析场景。

### 适用场景

统计汇总模型适合读多写少、查询口径相对稳定、实时性要求可控的统计类业务。它的核心目标不是保存每一条业务明细，而是保存已经计算好的汇总结果，让前端看板、管理后台、报表接口可以直接读取汇总表。

常见适用场景包括：

| 场景     | 说明                                                   |
| -------- | ------------------------------------------------------ |
| 日统计   | 按天统计订单数、支付金额、退款金额、新增用户数等       |
| 月统计   | 按月统计销售额、活跃用户、库存消耗、接口调用量等       |
| 用户统计 | 按用户统计订单数、消费金额、积分、登录次数等           |
| 商品统计 | 按商品统计销量、浏览量、收藏量、退款量等               |
| 门店统计 | 按门店统计销售额、订单量、客单价、退款率等             |
| 平台看板 | 后台首页展示今日订单、今日支付、累计用户、累计成交额等 |

该模型不适合强一致实时明细查询。例如用户需要查看每一笔订单明细时，应查询订单主表或订单明细表，而不是统计汇总表。统计汇总表主要服务于“按维度看结果”的场景。

### 建模结构

统计汇总模型通常由业务明细表和统计汇总表组成。业务明细表负责保存原始业务数据，统计汇总表负责保存按照固定口径计算后的结果。

以订单日统计为例，可以将订单主表作为明细数据来源，将订单日统计表作为汇总结果表。订单主表记录每一笔订单，订单日统计表记录某一天、某个租户、某个店铺或某个业务维度下的统计结果。

典型结构如下：

```sql
-- 订单主表：保存原始订单明细数据
CREATE TABLE `biz_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `order_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '订单金额',
  `pay_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '支付金额',
  `refund_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '退款金额',
  `order_status` varchar(32) NOT NULL COMMENT '订单状态',
  `pay_status` varchar(32) NOT NULL COMMENT '支付状态',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
-- 订单日统计表：保存按天、租户、店铺聚合后的订单统计数据
CREATE TABLE `stat_order_day` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `order_count` bigint NOT NULL DEFAULT '0' COMMENT '订单数',
  `paid_order_count` bigint NOT NULL DEFAULT '0' COMMENT '已支付订单数',
  `refund_order_count` bigint NOT NULL DEFAULT '0' COMMENT '退款订单数',
  `order_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '订单总金额',
  `pay_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '支付总金额',
  `refund_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '退款总金额',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date_tenant_shop` (`stat_date`, `tenant_id`, `shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单日统计表';
```

统计汇总表通常按照“统计周期 + 业务维度 + 指标字段”的方式建模。

其中，统计周期可以是天、周、月、小时或自然时间段。业务维度可以是租户、店铺、用户、商品、渠道、地区、组织、分类等。指标字段通常是数量、金额、次数、比例、最大值、最小值、平均值等。

### 字段设计

统计汇总表的字段设计应尽量稳定、明确和可解释。字段名称要直接表达统计口径，避免使用含义模糊的字段，例如 `num1`、`amount1`、`value1`。

推荐字段分类如下：

| 字段类型     | 示例字段                                                | 说明                           |
| ------------ | ------------------------------------------------------- | ------------------------------ |
| 主键字段     | `id`                                                    | 汇总表主键                     |
| 统计周期字段 | `stat_date`、`stat_month`、`stat_hour`                  | 表示统计数据所属时间周期       |
| 业务维度字段 | `tenant_id`、`shop_id`、`user_id`、`product_id`         | 表示按什么维度聚合             |
| 数量指标字段 | `order_count`、`paid_order_count`、`refund_order_count` | 保存数量类统计结果             |
| 金额指标字段 | `order_amount`、`pay_amount`、`refund_amount`           | 保存金额类统计结果             |
| 时间字段     | `created_at`、`updated_at`                              | 保存记录创建和更新时间         |
| 版本字段     | `version`                                               | 并发更新时可选，用于乐观锁控制 |

字段设计建议如下：

1. 金额字段使用 `decimal(18,2)` 或根据业务精度调整，不建议使用 `double` 或 `float`。
2. 数量字段使用 `bigint`，避免后期数据量增长导致整型溢出。
3. 统计日期字段优先使用 `date`，不要使用字符串保存日期。
4. 月统计可以使用 `char(7)` 保存 `yyyy-MM`，也可以使用 `date` 保存每月第一天。
5. 汇总表中的指标字段应尽量使用非空字段，并设置默认值。
6. 不建议在统计汇总表中保存过多展示字段，例如店铺名称、商品名称等，除非该表明确承担读模型职责。
7. 统计口径发生变化时，应优先新增字段或新增统计表，谨慎直接修改已有字段含义。

一个更完整的用户日统计表示例如下：

```sql
-- 用户日统计表：保存用户每天的行为与交易统计数据
CREATE TABLE `stat_user_day` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `login_count` bigint NOT NULL DEFAULT '0' COMMENT '登录次数',
  `browse_count` bigint NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `order_count` bigint NOT NULL DEFAULT '0' COMMENT '下单次数',
  `pay_count` bigint NOT NULL DEFAULT '0' COMMENT '支付次数',
  `pay_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '支付金额',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date_tenant_user` (`stat_date`, `tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户日统计表';
```

### 索引设计

统计汇总模型的索引设计应围绕统计周期、业务维度和查询入口设计。汇总表通常数据量比明细表小，但在多租户、多门店、多商品、多用户场景下，长期累积后仍然可能成为大表，因此索引设计不能忽略。

订单日统计表推荐索引如下：

```sql
-- 按统计日期、租户、店铺保证一条唯一统计记录
ALTER TABLE `stat_order_day`
  ADD UNIQUE KEY `uk_stat_date_tenant_shop` (`stat_date`, `tenant_id`, `shop_id`);

-- 按租户、店铺、日期范围查询统计趋势
ALTER TABLE `stat_order_day`
  ADD KEY `idx_tenant_shop_date` (`tenant_id`, `shop_id`, `stat_date`);

-- 按租户、日期范围查询租户整体统计
ALTER TABLE `stat_order_day`
  ADD KEY `idx_tenant_date` (`tenant_id`, `stat_date`);
```

索引设计建议如下：

1. 唯一索引用于防止同一统计周期、同一业务维度重复生成统计记录。
2. 日期范围查询较多时，常用查询条件中的等值字段应放在前面，范围字段通常放在后面。
3. 多租户系统中，`tenant_id` 通常应作为业务查询索引的前导字段。
4. 后台看板如果经常按日期查询所有店铺统计，可以补充以 `stat_date` 开头的索引。
5. 不要为每一个指标字段建立索引，统计指标字段通常用于展示和排序，不一定适合作为高频过滤条件。
6. 如果经常按照金额、数量排序取 Top N，可以针对具体查询单独设计索引或使用独立的排行榜表。
7. 汇总表索引数量不宜过多，否则会增加统计写入和批量重算成本。

### 常用查询

统计汇总模型的常用查询主要集中在看板统计、趋势分析、维度排名和区间汇总。查询时应优先读取汇总表，避免直接对明细大表执行复杂聚合。

#### 查询某个店铺的日统计趋势

该查询用于展示某个店铺在一段时间内的订单数、支付金额和退款金额趋势，常用于折线图或柱状图。

```sql
SELECT
  `stat_date`,
  `order_count`,
  `paid_order_count`,
  `pay_amount`,
  `refund_amount`
FROM `stat_order_day`
WHERE `tenant_id` = 10001
  AND `shop_id` = 20001
  AND `stat_date` BETWEEN '2025-01-01' AND '2025-01-31'
ORDER BY `stat_date` ASC;
```

#### 查询租户某天的整体统计

该查询用于后台首页展示某个租户当天所有店铺的整体经营数据。

```sql
SELECT
  `stat_date`,
  SUM(`order_count`) AS `order_count`,
  SUM(`paid_order_count`) AS `paid_order_count`,
  SUM(`refund_order_count`) AS `refund_order_count`,
  SUM(`order_amount`) AS `order_amount`,
  SUM(`pay_amount`) AS `pay_amount`,
  SUM(`refund_amount`) AS `refund_amount`
FROM `stat_order_day`
WHERE `tenant_id` = 10001
  AND `stat_date` = '2025-01-15'
GROUP BY `stat_date`;
```

#### 查询租户指定日期范围内的累计统计

该查询用于统计某个租户在一段时间内的累计订单数、累计支付金额和累计退款金额。

```sql
SELECT
  SUM(`order_count`) AS `total_order_count`,
  SUM(`paid_order_count`) AS `total_paid_order_count`,
  SUM(`refund_order_count`) AS `total_refund_order_count`,
  SUM(`order_amount`) AS `total_order_amount`,
  SUM(`pay_amount`) AS `total_pay_amount`,
  SUM(`refund_amount`) AS `total_refund_amount`
FROM `stat_order_day`
WHERE `tenant_id` = 10001
  AND `stat_date` BETWEEN '2025-01-01' AND '2025-01-31';
```

#### 查询店铺销售额排行榜

该查询用于统计某一天或某个时间范围内店铺支付金额排名，常用于运营后台排行榜。

```sql
SELECT
  `shop_id`,
  SUM(`paid_order_count`) AS `paid_order_count`,
  SUM(`pay_amount`) AS `pay_amount`
FROM `stat_order_day`
WHERE `tenant_id` = 10001
  AND `stat_date` BETWEEN '2025-01-01' AND '2025-01-31'
GROUP BY `shop_id`
ORDER BY `pay_amount` DESC
LIMIT 10;
```

#### 查询用户日行为统计

该查询用于查看某个用户在一段时间内的登录、浏览、下单和支付行为。

```sql
SELECT
  `stat_date`,
  `login_count`,
  `browse_count`,
  `order_count`,
  `pay_count`,
  `pay_amount`,
  `last_login_at`
FROM `stat_user_day`
WHERE `tenant_id` = 10001
  AND `user_id` = 30001
  AND `stat_date` BETWEEN '2025-01-01' AND '2025-01-31'
ORDER BY `stat_date` ASC;
```

#### 查询近 7 天支付趋势

该查询用于首页看板展示最近 7 天支付金额趋势。实际业务中，日期范围建议由应用层计算后传入，避免在 SQL 中隐藏复杂逻辑。

```sql
SELECT
  `stat_date`,
  SUM(`pay_amount`) AS `pay_amount`
FROM `stat_order_day`
WHERE `tenant_id` = 10001
  AND `stat_date` BETWEEN '2025-01-09' AND '2025-01-15'
GROUP BY `stat_date`
ORDER BY `stat_date` ASC;
```

### 常用写入

统计汇总模型的写入方式主要有三类：实时增量写入、定时批量汇总、重新计算覆盖。不同业务对实时性、一致性和性能要求不同，写入方式也不同。

#### 实时增量写入

实时增量写入适合对实时性要求较高的统计场景。例如订单支付成功后，立即更新当天的订单日统计数据。

可以使用 `INSERT ... ON DUPLICATE KEY UPDATE` 实现存在则更新、不存在则插入。

```sql
-- 支付成功后，实时累加订单日统计
INSERT INTO `stat_order_day` (
  `id`,
  `stat_date`,
  `tenant_id`,
  `shop_id`,
  `order_count`,
  `paid_order_count`,
  `refund_order_count`,
  `order_amount`,
  `pay_amount`,
  `refund_amount`,
  `created_at`,
  `updated_at`
) VALUES (
  1000001,
  '2025-01-15',
  10001,
  20001,
  1,
  1,
  0,
  199.00,
  199.00,
  0.00,
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE
  `order_count` = `order_count` + VALUES(`order_count`),
  `paid_order_count` = `paid_order_count` + VALUES(`paid_order_count`),
  `order_amount` = `order_amount` + VALUES(`order_amount`),
  `pay_amount` = `pay_amount` + VALUES(`pay_amount`),
  `updated_at` = NOW();
```

该方式依赖唯一索引防重。唯一索引通常由统计周期和业务维度组成，例如 `stat_date + tenant_id + shop_id`。

#### 定时批量汇总

定时批量汇总适合实时性要求不高的报表场景。例如每 5 分钟、每小时或每天凌晨从订单表中计算统计数据。

```sql
-- 按天、租户、店铺从订单主表汇总生成订单日统计
INSERT INTO `stat_order_day` (
  `id`,
  `stat_date`,
  `tenant_id`,
  `shop_id`,
  `order_count`,
  `paid_order_count`,
  `refund_order_count`,
  `order_amount`,
  `pay_amount`,
  `refund_amount`,
  `created_at`,
  `updated_at`
)
SELECT
  -- 示例中使用固定值占位，实际业务中应由应用层或数据库函数生成分布式ID
  FLOOR(RAND() * 1000000000000) AS `id`,
  DATE(`created_at`) AS `stat_date`,
  `tenant_id`,
  `shop_id`,
  COUNT(*) AS `order_count`,
  SUM(CASE WHEN `pay_status` = 'PAID' THEN 1 ELSE 0 END) AS `paid_order_count`,
  SUM(CASE WHEN `refund_amount` > 0 THEN 1 ELSE 0 END) AS `refund_order_count`,
  SUM(`order_amount`) AS `order_amount`,
  SUM(`pay_amount`) AS `pay_amount`,
  SUM(`refund_amount`) AS `refund_amount`,
  NOW() AS `created_at`,
  NOW() AS `updated_at`
FROM `biz_order`
WHERE `created_at` >= '2025-01-15 00:00:00'
  AND `created_at` < '2025-01-16 00:00:00'
GROUP BY
  DATE(`created_at`),
  `tenant_id`,
  `shop_id`
ON DUPLICATE KEY UPDATE
  `order_count` = VALUES(`order_count`),
  `paid_order_count` = VALUES(`paid_order_count`),
  `refund_order_count` = VALUES(`refund_order_count`),
  `order_amount` = VALUES(`order_amount`),
  `pay_amount` = VALUES(`pay_amount`),
  `refund_amount` = VALUES(`refund_amount`),
  `updated_at` = NOW();
```

生产环境中不建议使用 `RAND()` 生成主键。主键应由应用层雪花算法、数据库号段服务或其他统一 ID 生成器生成。

#### 重新计算覆盖

重新计算覆盖适合修复统计口径、补偿历史数据或处理异常统计数据。常见做法是先删除指定统计范围的数据，再重新汇总写入。

```sql
-- 删除指定日期范围内的旧统计数据
DELETE FROM `stat_order_day`
WHERE `tenant_id` = 10001
  AND `stat_date` BETWEEN '2025-01-01' AND '2025-01-31';
-- 删除后重新写入指定日期范围内的统计数据
INSERT INTO `stat_order_day` (
  `id`,
  `stat_date`,
  `tenant_id`,
  `shop_id`,
  `order_count`,
  `paid_order_count`,
  `refund_order_count`,
  `order_amount`,
  `pay_amount`,
  `refund_amount`,
  `created_at`,
  `updated_at`
)
SELECT
  FLOOR(RAND() * 1000000000000) AS `id`,
  DATE(`created_at`) AS `stat_date`,
  `tenant_id`,
  `shop_id`,
  COUNT(*) AS `order_count`,
  SUM(CASE WHEN `pay_status` = 'PAID' THEN 1 ELSE 0 END) AS `paid_order_count`,
  SUM(CASE WHEN `refund_amount` > 0 THEN 1 ELSE 0 END) AS `refund_order_count`,
  SUM(`order_amount`) AS `order_amount`,
  SUM(`pay_amount`) AS `pay_amount`,
  SUM(`refund_amount`) AS `refund_amount`,
  NOW() AS `created_at`,
  NOW() AS `updated_at`
FROM `biz_order`
WHERE `tenant_id` = 10001
  AND `created_at` >= '2025-01-01 00:00:00'
  AND `created_at` < '2025-02-01 00:00:00'
GROUP BY
  DATE(`created_at`),
  `tenant_id`,
  `shop_id`;
```

重新计算覆盖时要注意操作范围，避免误删其他租户或其他日期的数据。大范围重算建议分批执行，并在低峰期操作。

### 常见问题

统计汇总模型最常见的问题不是表结构本身，而是统计口径、一致性、补偿机制和数据膨胀控制。

| 问题                     | 原因                                       | 处理建议                                           |
| ------------------------ | ------------------------------------------ | -------------------------------------------------- |
| 统计结果和明细数据不一致 | 实时写入失败、消息丢失、重复消费或口径变化 | 增加补偿任务，定期从明细表重新校准                 |
| 统计数据重复             | 缺少唯一约束或维度设计不清晰               | 使用统计周期和业务维度建立唯一约束                 |
| 查询趋势数据很慢         | 日期范围过大或索引不匹配                   | 按查询条件设计组合索引，必要时拆分日表、月表       |
| 写入冲突严重             | 多个业务事件同时更新同一统计行             | 使用增量消息队列、分片统计或异步汇总               |
| 历史统计口径变更困难     | 原字段含义被直接修改                       | 新增统计字段或新建统计表，保留旧口径               |
| 汇总表字段越来越多       | 多个报表需求混在同一张统计表               | 按主题拆分统计表，例如订单统计、用户统计、商品统计 |
| 批量汇总影响线上业务     | 汇总任务扫描明细大表                       | 使用时间范围、状态条件、必要索引和低峰调度         |

对于高并发实时统计，不建议所有请求都直接更新同一行统计数据。例如平台当天总访问量这种热点指标，如果每次访问都更新同一行，容易造成行锁竞争。可以采用 Redis 预聚合、消息队列异步消费、分片统计表或定时合并等方式降低数据库写入压力。

对于财务类、账户类、结算类统计，不应只依赖汇总表作为最终依据。汇总表可以用于查询和展示，但最终对账仍应以明细流水或原始业务单据为准。

### 总结

统计汇总模型通过提前聚合明细数据，将复杂的实时统计查询转化为简单的汇总表查询，能够显著降低大表扫描、实时分组和报表查询压力。

设计该模型时，应重点关注四个方面：第一，统计周期和业务维度要清晰；第二，指标字段要有明确口径；第三，唯一约束要防止重复统计；第四，写入方式要根据实时性和一致性要求选择。

对于实时性要求高的场景，可以使用实时增量写入；对于报表和看板场景，可以使用定时批量汇总；对于历史修复和口径调整场景，可以使用重新计算覆盖。无论采用哪种方式，都应保留从明细数据重新计算统计结果的能力，避免统计表成为无法校准的数据孤岛。



## 搜索辅助表模型

搜索辅助表模型用于解决业务主表字段分散、查询条件复杂、模糊搜索性能差、跨表搜索成本高的问题。该模型通过额外建立一张面向搜索场景的辅助表，将业务主表中常用的搜索字段、冗余展示字段、拼接搜索字段和搜索状态集中存储，从而降低主业务表的查询压力。

### 适用场景

搜索辅助表模型适合查询条件多、模糊搜索多、跨表字段搜索多、列表页查询频繁的业务场景。它不是业务主表的替代品，而是为了提升搜索和筛选性能构建的查询辅助结构。

常见适用场景包括：

| 场景           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 后台列表搜索   | 根据名称、手机号、编号、状态、时间范围等条件组合查询         |
| 跨表字段搜索   | 订单列表需要同时按订单号、用户手机号、收货人姓名、商品名称搜索 |
| 模糊搜索       | 对名称、标题、手机号、编码、备注等字段进行 `LIKE` 查询       |
| 多字段统一搜索 | 一个关键字同时匹配多个字段，例如订单号、用户昵称、手机号     |
| 冗余查询字段   | 将用户名称、店铺名称、分类名称等字段冗余到搜索表             |
| 查询读模型     | 面向管理后台、运营后台、客服系统等构建列表查询表             |
| 降低主表压力   | 避免复杂搜索直接影响核心业务表的写入和事务性能               |

该模型适合业务查询口径相对稳定、搜索字段明确、允许一定字段冗余的系统。如果搜索需求非常复杂，例如分词、相关性排序、高亮、拼写纠错、近实时全文检索等，应优先考虑 Elasticsearch、OpenSearch、Solr 等专业搜索引擎。

### 建模结构

搜索辅助表通常围绕一个核心业务对象建立。例如订单业务可以有订单主表、订单明细表、用户表、收货地址表，同时额外建立一张订单搜索辅助表。订单搜索辅助表只服务搜索和列表查询，不承载核心交易逻辑。

以订单搜索为例，业务主表保存订单核心数据，搜索辅助表保存订单列表页常用查询字段和展示字段。

```sql
-- 订单主表：保存订单核心业务数据
CREATE TABLE `biz_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `order_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '订单金额',
  `pay_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '支付金额',
  `order_status` varchar(32) NOT NULL COMMENT '订单状态',
  `pay_status` varchar(32) NOT NULL COMMENT '支付状态',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
-- 订单搜索辅助表：保存订单列表搜索和展示所需的冗余字段
CREATE TABLE `search_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `shop_name` varchar(128) NOT NULL DEFAULT '' COMMENT '店铺名称',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(128) NOT NULL DEFAULT '' COMMENT '用户名称',
  `user_mobile` varchar(32) NOT NULL DEFAULT '' COMMENT '用户手机号',
  `receiver_name` varchar(128) NOT NULL DEFAULT '' COMMENT '收货人姓名',
  `receiver_mobile` varchar(32) NOT NULL DEFAULT '' COMMENT '收货人手机号',
  `product_keywords` varchar(1024) NOT NULL DEFAULT '' COMMENT '商品搜索关键字',
  `search_text` varchar(2048) NOT NULL DEFAULT '' COMMENT '综合搜索文本',
  `order_status` varchar(32) NOT NULL COMMENT '订单状态',
  `pay_status` varchar(32) NOT NULL COMMENT '支付状态',
  `order_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '订单金额',
  `pay_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '支付金额',
  `order_created_at` datetime NOT NULL COMMENT '订单创建时间',
  `order_paid_at` datetime DEFAULT NULL COMMENT '订单支付时间',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单搜索辅助表';
```

搜索辅助表的数据通常来自多张业务表。应用层在订单创建、订单支付、订单取消、用户信息变更、商品信息变更时，同步更新搜索辅助表。也可以通过定时任务或消息队列异步构建搜索辅助数据。

搜索辅助表不建议参与核心业务事务判断。例如订单是否可支付、库存是否可扣减、退款是否可发起，应以业务主表和业务明细表为准。搜索辅助表只用于查询、筛选和展示。

### 字段设计

搜索辅助表字段设计的核心原则是面向查询场景，而不是完全复制业务主表。字段应围绕“查询条件、排序字段、列表展示字段、综合搜索字段、同步控制字段”进行设计。

推荐字段分类如下：

| 字段类型     | 示例字段                                     | 说明                         |
| ------------ | -------------------------------------------- | ---------------------------- |
| 关联字段     | `order_id`、`user_id`、`shop_id`             | 关联原始业务对象             |
| 租户字段     | `tenant_id`                                  | 多租户系统中用于数据隔离     |
| 精确搜索字段 | `order_no`、`user_mobile`、`receiver_mobile` | 用于等值查询                 |
| 模糊搜索字段 | `user_name`、`receiver_name`、`shop_name`    | 用于名称类搜索               |
| 综合搜索字段 | `search_text`                                | 将多个字段拼接成统一搜索文本 |
| 状态字段     | `order_status`、`pay_status`                 | 用于业务状态筛选             |
| 金额字段     | `order_amount`、`pay_amount`                 | 用于列表展示或范围筛选       |
| 时间字段     | `order_created_at`、`order_paid_at`          | 用于时间范围查询和排序       |
| 同步字段     | `created_at`、`updated_at`                   | 用于辅助表维护和排查         |

字段设计建议如下：

1. 搜索辅助表字段应以列表页和搜索接口为中心设计，不要无节制复制所有业务字段。
2. 精确匹配字段和模糊匹配字段应分开设计，避免所有搜索都依赖一个大字段。
3. 手机号、编号、编码类字段建议保留原始字段，便于精确查询。
4. 多字段关键字搜索可以使用 `search_text` 保存拼接后的搜索内容。
5. `search_text` 中可以拼接订单号、用户名称、手机号、收货人、商品名称等字段。
6. 冗余名称类字段时，应接受短时间内与原表不一致，并设计同步补偿机制。
7. 金额、状态、时间字段如果用于筛选或排序，应直接冗余到搜索辅助表。
8. 不建议在搜索辅助表中保存大文本字段，例如完整备注、完整地址、完整描述等。
9. 对于敏感字段，例如手机号、身份证号，应根据业务合规要求进行脱敏、加密或限制查询权限。

一个商品搜索辅助表示例如下：

```sql
-- 商品搜索辅助表：保存商品列表搜索和筛选所需字段
CREATE TABLE `search_product` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `brand_id` bigint DEFAULT NULL COMMENT '品牌ID',
  `product_code` varchar(64) NOT NULL COMMENT '商品编码',
  `product_name` varchar(256) NOT NULL COMMENT '商品名称',
  `category_name` varchar(128) NOT NULL DEFAULT '' COMMENT '分类名称',
  `brand_name` varchar(128) NOT NULL DEFAULT '' COMMENT '品牌名称',
  `search_text` varchar(2048) NOT NULL DEFAULT '' COMMENT '综合搜索文本',
  `sale_price` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '销售价格',
  `sale_status` varchar(32) NOT NULL COMMENT '销售状态',
  `stock_quantity` bigint NOT NULL DEFAULT '0' COMMENT '库存数量',
  `product_created_at` datetime NOT NULL COMMENT '商品创建时间',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品搜索辅助表';
```

### 索引设计

搜索辅助表的索引设计应围绕高频查询入口进行。常见查询入口包括租户、状态、时间范围、编号、手机号、名称关键字和排序字段。

订单搜索辅助表推荐索引如下：

```sql
-- 主键索引
ALTER TABLE `search_order`
  ADD PRIMARY KEY (`id`);

-- 订单ID唯一索引，保证一个订单只有一条搜索辅助记录
ALTER TABLE `search_order`
  ADD UNIQUE KEY `uk_order_id` (`order_id`);

-- 按租户、订单编号精确查询
ALTER TABLE `search_order`
  ADD KEY `idx_tenant_order_no` (`tenant_id`, `order_no`);

-- 按租户、用户手机号查询
ALTER TABLE `search_order`
  ADD KEY `idx_tenant_user_mobile` (`tenant_id`, `user_mobile`);

-- 按租户、订单状态、订单创建时间查询列表
ALTER TABLE `search_order`
  ADD KEY `idx_tenant_status_created` (`tenant_id`, `order_status`, `order_created_at`);

-- 按租户、支付状态、支付时间查询已支付订单
ALTER TABLE `search_order`
  ADD KEY `idx_tenant_pay_paid` (`tenant_id`, `pay_status`, `order_paid_at`);

-- 按租户、店铺、订单创建时间查询店铺订单列表
ALTER TABLE `search_order`
  ADD KEY `idx_tenant_shop_created` (`tenant_id`, `shop_id`, `order_created_at`);

-- 综合搜索文本全文索引，适合 MySQL 8 InnoDB 全文检索场景
ALTER TABLE `search_order`
  ADD FULLTEXT KEY `ft_search_text` (`search_text`);
```

商品搜索辅助表推荐索引如下：

```sql
-- 主键索引
ALTER TABLE `search_product`
  ADD PRIMARY KEY (`id`);

-- 商品ID唯一索引，保证一个商品只有一条搜索辅助记录
ALTER TABLE `search_product`
  ADD UNIQUE KEY `uk_product_id` (`product_id`);

-- 按租户、商品编码精确查询
ALTER TABLE `search_product`
  ADD KEY `idx_tenant_product_code` (`tenant_id`, `product_code`);

-- 按租户、分类、销售状态查询商品列表
ALTER TABLE `search_product`
  ADD KEY `idx_tenant_category_status` (`tenant_id`, `category_id`, `sale_status`);

-- 按租户、店铺、商品创建时间查询商品列表
ALTER TABLE `search_product`
  ADD KEY `idx_tenant_shop_created` (`tenant_id`, `shop_id`, `product_created_at`);

-- 商品名称全文索引
ALTER TABLE `search_product`
  ADD FULLTEXT KEY `ft_product_name` (`product_name`);

-- 综合搜索文本全文索引
ALTER TABLE `search_product`
  ADD FULLTEXT KEY `ft_search_text` (`search_text`);
```

索引设计建议如下：

1. 多租户系统中，搜索辅助表的大多数业务索引应以 `tenant_id` 作为前导字段。
2. 编号、手机号、编码类字段优先使用普通索引进行精确匹配。
3. 状态加时间范围是后台列表页最常见的组合查询条件，应重点设计组合索引。
4. 排序字段如果经常与筛选条件一起使用，应放入组合索引中。
5. `LIKE '%关键词%'` 很难有效利用普通 B+Tree 索引，数据量较大时应考虑全文索引或搜索引擎。
6. MySQL 全文索引适合中等复杂度的站内搜索，但不适合替代专业搜索引擎完成复杂检索。
7. 搜索辅助表索引数量不宜过多，否则会增加同步写入、批量重建和更新成本。
8. 对于低基数字段，例如状态字段，单独建索引价值通常不高，应和租户、时间、业务维度组合使用。

### 常用查询

搜索辅助表的常用查询主要包括精确查询、条件筛选、关键字搜索、范围查询和列表分页查询。查询时应优先在搜索辅助表中完成筛选，再根据需要回表查询业务主表详情。

#### 根据订单编号精确查询

该查询适合订单号、业务单号、流水号等精确搜索场景。编号类字段通常选择等值查询，不建议使用模糊查询作为默认方式。

```sql
SELECT
  `order_id`,
  `order_no`,
  `user_name`,
  `user_mobile`,
  `order_status`,
  `pay_status`,
  `pay_amount`,
  `order_created_at`
FROM `search_order`
WHERE `tenant_id` = 10001
  AND `order_no` = 'OD202501150001';
```

#### 根据手机号查询订单

该查询适合客服系统根据用户手机号或收货人手机号查找订单。多个手机号字段可以使用 `OR`，但在数据量较大时应根据实际执行计划优化。

```sql
SELECT
  `order_id`,
  `order_no`,
  `user_name`,
  `user_mobile`,
  `receiver_name`,
  `receiver_mobile`,
  `order_status`,
  `pay_status`,
  `order_created_at`
FROM `search_order`
WHERE `tenant_id` = 10001
  AND (
    `user_mobile` = '13800000000'
    OR `receiver_mobile` = '13800000000'
  )
ORDER BY `order_created_at` DESC
LIMIT 20;
```

如果手机号查询非常高频，可以拆分为两次查询，分别命中不同索引，再由应用层合并结果。

```sql
SELECT
  `order_id`,
  `order_no`,
  `user_name`,
  `user_mobile`,
  `order_status`,
  `pay_status`,
  `order_created_at`
FROM `search_order`
WHERE `tenant_id` = 10001
  AND `user_mobile` = '13800000000'
ORDER BY `order_created_at` DESC
LIMIT 20;
SELECT
  `order_id`,
  `order_no`,
  `receiver_name`,
  `receiver_mobile`,
  `order_status`,
  `pay_status`,
  `order_created_at`
FROM `search_order`
WHERE `tenant_id` = 10001
  AND `receiver_mobile` = '13800000000'
ORDER BY `order_created_at` DESC
LIMIT 20;
```

#### 查询订单列表

该查询适合后台订单列表页。列表页通常包含租户、状态、时间范围、分页排序等条件。

```sql
SELECT
  `order_id`,
  `order_no`,
  `shop_name`,
  `user_name`,
  `user_mobile`,
  `order_status`,
  `pay_status`,
  `order_amount`,
  `pay_amount`,
  `order_created_at`
FROM `search_order`
WHERE `tenant_id` = 10001
  AND `order_status` = 'COMPLETED'
  AND `order_created_at` >= '2025-01-01 00:00:00'
  AND `order_created_at` < '2025-02-01 00:00:00'
ORDER BY `order_created_at` DESC
LIMIT 20 OFFSET 0;
```

对于深分页场景，不建议长期使用大偏移量 `OFFSET`。可以改用游标分页，基于上一页最后一条记录的排序字段继续查询。

```sql
SELECT
  `order_id`,
  `order_no`,
  `shop_name`,
  `user_name`,
  `order_status`,
  `pay_status`,
  `pay_amount`,
  `order_created_at`
FROM `search_order`
WHERE `tenant_id` = 10001
  AND `order_status` = 'COMPLETED'
  AND `order_created_at` < '2025-01-15 10:30:00'
ORDER BY `order_created_at` DESC
LIMIT 20;
```

#### 根据关键字模糊搜索

该查询适合小数据量或低频搜索场景。`LIKE '%关键词%'` 对普通索引不友好，数据量较大时应谨慎使用。

```sql
SELECT
  `order_id`,
  `order_no`,
  `user_name`,
  `user_mobile`,
  `receiver_name`,
  `receiver_mobile`,
  `order_status`,
  `pay_status`,
  `order_created_at`
FROM `search_order`
WHERE `tenant_id` = 10001
  AND `search_text` LIKE CONCAT('%', '张三', '%')
ORDER BY `order_created_at` DESC
LIMIT 20;
```

如果业务只需要前缀匹配，可以使用 `LIKE '关键词%'`，这种方式比前后模糊更容易利用普通索引。

```sql
SELECT
  `product_id`,
  `product_code`,
  `product_name`,
  `category_name`,
  `brand_name`,
  `sale_price`,
  `sale_status`
FROM `search_product`
WHERE `tenant_id` = 10001
  AND `product_name` LIKE CONCAT('苹果', '%')
ORDER BY `product_created_at` DESC
LIMIT 20;
```

#### 使用全文索引搜索

该查询适合使用 MySQL 8 InnoDB 全文索引的场景。全文索引用于提升文本搜索能力，但查询效果和分词方式、字段内容、停用词配置等有关。

```sql
SELECT
  `order_id`,
  `order_no`,
  `user_name`,
  `user_mobile`,
  `receiver_name`,
  `receiver_mobile`,
  `order_status`,
  `pay_status`,
  `order_created_at`,
  MATCH(`search_text`) AGAINST('张三 手机' IN NATURAL LANGUAGE MODE) AS `score`
FROM `search_order`
WHERE `tenant_id` = 10001
  AND MATCH(`search_text`) AGAINST('张三 手机' IN NATURAL LANGUAGE MODE)
ORDER BY `score` DESC, `order_created_at` DESC
LIMIT 20;
```

布尔模式适合需要包含、排除或前缀匹配的搜索场景。

```sql
SELECT
  `product_id`,
  `product_code`,
  `product_name`,
  `category_name`,
  `brand_name`,
  `sale_price`,
  `sale_status`,
  MATCH(`search_text`) AGAINST('+苹果 +手机' IN BOOLEAN MODE) AS `score`
FROM `search_product`
WHERE `tenant_id` = 10001
  AND MATCH(`search_text`) AGAINST('+苹果 +手机' IN BOOLEAN MODE)
ORDER BY `score` DESC, `product_created_at` DESC
LIMIT 20;
```

#### 先查搜索表再查主表详情

该查询方式适合搜索辅助表只返回业务对象 ID，然后由业务主表查询详情。这样可以避免搜索辅助表冗余过多详情字段。

```sql
SELECT
  `order_id`
FROM `search_order`
WHERE `tenant_id` = 10001
  AND `pay_status` = 'PAID'
  AND `order_created_at` >= '2025-01-01 00:00:00'
  AND `order_created_at` < '2025-02-01 00:00:00'
ORDER BY `order_created_at` DESC
LIMIT 20;
SELECT
  `id`,
  `order_no`,
  `tenant_id`,
  `user_id`,
  `shop_id`,
  `order_amount`,
  `pay_amount`,
  `order_status`,
  `pay_status`,
  `created_at`,
  `paid_at`
FROM `biz_order`
WHERE `id` IN (100001, 100002, 100003);
```

应用层需要注意保持结果顺序。如果第二次查询使用 `IN`，数据库不一定按传入 ID 顺序返回，可以在应用层按第一次查询结果重新排序。

### 常用写入

搜索辅助表的写入方式通常包括同步写入、异步写入、字段变更同步和批量重建。写入方案应根据业务一致性要求和搜索实时性要求选择。

#### 创建业务数据时同步写入搜索辅助表

该方式适合搜索实时性要求高、写入链路可控的场景。例如订单创建成功后，同步写入订单搜索辅助表。

```sql
-- 订单创建成功后写入订单搜索辅助表
INSERT INTO `search_order` (
  `id`,
  `order_id`,
  `tenant_id`,
  `shop_id`,
  `shop_name`,
  `order_no`,
  `user_id`,
  `user_name`,
  `user_mobile`,
  `receiver_name`,
  `receiver_mobile`,
  `product_keywords`,
  `search_text`,
  `order_status`,
  `pay_status`,
  `order_amount`,
  `pay_amount`,
  `order_created_at`,
  `order_paid_at`,
  `created_at`,
  `updated_at`
) VALUES (
  9000001,
  1000001,
  10001,
  20001,
  '杭州西湖店',
  'OD202501150001',
  30001,
  '张三',
  '13800000000',
  '李四',
  '13900000000',
  '苹果手机 iPhone 保护壳',
  'OD202501150001 张三 13800000000 李四 13900000000 苹果手机 iPhone 保护壳 杭州西湖店',
  'CREATED',
  'UNPAID',
  199.00,
  0.00,
  '2025-01-15 10:00:00',
  NULL,
  NOW(),
  NOW()
);
```

同步写入的优点是数据实时性好。缺点是主业务写入链路变长，搜索辅助表异常可能影响业务事务。生产环境中可以将搜索辅助表写入放在本地事务内，也可以通过事件表或消息队列异步处理。

#### 业务状态变更时更新搜索辅助表

订单支付、取消、退款等状态变更后，需要同步更新搜索辅助表中的状态、金额和时间字段。

```sql
-- 订单支付成功后更新搜索辅助表
UPDATE `search_order`
SET
  `pay_status` = 'PAID',
  `order_status` = 'PAID',
  `pay_amount` = 199.00,
  `order_paid_at` = '2025-01-15 10:05:00',
  `updated_at` = NOW()
WHERE `tenant_id` = 10001
  AND `order_id` = 1000001;
-- 订单取消后更新搜索辅助表
UPDATE `search_order`
SET
  `order_status` = 'CANCELED',
  `updated_at` = NOW()
WHERE `tenant_id` = 10001
  AND `order_id` = 1000001;
```

#### 关联字段变更时更新搜索辅助表

如果用户昵称、手机号、店铺名称、商品名称等冗余字段发生变化，需要根据业务要求决定是否同步历史搜索辅助数据。

```sql
-- 用户手机号变更后，同步更新订单搜索辅助表中的用户手机号和综合搜索文本
UPDATE `search_order`
SET
  `user_mobile` = '13700000000',
  `search_text` = CONCAT(
    `order_no`, ' ',
    `user_name`, ' ',
    '13700000000', ' ',
    `receiver_name`, ' ',
    `receiver_mobile`, ' ',
    `product_keywords`, ' ',
    `shop_name`
  ),
  `updated_at` = NOW()
WHERE `tenant_id` = 10001
  AND `user_id` = 30001;
```

是否同步历史数据取决于业务语义。如果订单快照要求保留当时的用户信息，则不应同步历史订单搜索辅助表中的用户名称和手机号。如果后台搜索要求按照最新用户信息搜索历史订单，则可以同步更新。

#### 使用异步消息写入搜索辅助表

异步写入适合高并发写入场景。业务主流程只发布订单事件，搜索辅助表由消费者异步构建。

```sql
-- 事件表：保存待处理的搜索辅助表同步事件
CREATE TABLE `event_search_sync` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `biz_id` bigint NOT NULL COMMENT '业务ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `event_status` varchar(32) NOT NULL COMMENT '事件状态',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `error_message` varchar(1024) NOT NULL DEFAULT '' COMMENT '错误信息',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索同步事件表';
-- 业务变更后写入搜索同步事件
INSERT INTO `event_search_sync` (
  `id`,
  `tenant_id`,
  `biz_type`,
  `biz_id`,
  `event_type`,
  `event_status`,
  `retry_count`,
  `error_message`,
  `created_at`,
  `updated_at`
) VALUES (
  8000001,
  10001,
  'ORDER',
  1000001,
  'ORDER_PAID',
  'WAITING',
  0,
  '',
  NOW(),
  NOW()
);
```

消费者读取事件后，根据 `biz_type`、`biz_id` 和 `event_type` 查询业务主表，重新构建或更新搜索辅助表。

#### 批量重建搜索辅助表

当搜索字段调整、冗余口径变化、历史数据缺失或搜索辅助表异常时，可以通过批量任务重建辅助表。

```sql
-- 删除指定租户的订单搜索辅助数据
DELETE FROM `search_order`
WHERE `tenant_id` = 10001;
-- 从订单、用户、店铺等业务表重新构建订单搜索辅助表
INSERT INTO `search_order` (
  `id`,
  `order_id`,
  `tenant_id`,
  `shop_id`,
  `shop_name`,
  `order_no`,
  `user_id`,
  `user_name`,
  `user_mobile`,
  `receiver_name`,
  `receiver_mobile`,
  `product_keywords`,
  `search_text`,
  `order_status`,
  `pay_status`,
  `order_amount`,
  `pay_amount`,
  `order_created_at`,
  `order_paid_at`,
  `created_at`,
  `updated_at`
)
SELECT
  -- 示例中使用固定值占位，生产环境应由应用层或统一ID服务生成
  FLOOR(RAND() * 1000000000000) AS `id`,
  o.`id` AS `order_id`,
  o.`tenant_id`,
  o.`shop_id`,
  s.`shop_name`,
  o.`order_no`,
  o.`user_id`,
  u.`user_name`,
  u.`mobile` AS `user_mobile`,
  a.`receiver_name`,
  a.`receiver_mobile`,
  IFNULL(p.`product_keywords`, '') AS `product_keywords`,
  CONCAT_WS(
    ' ',
    o.`order_no`,
    u.`user_name`,
    u.`mobile`,
    a.`receiver_name`,
    a.`receiver_mobile`,
    IFNULL(p.`product_keywords`, ''),
    s.`shop_name`
  ) AS `search_text`,
  o.`order_status`,
  o.`pay_status`,
  o.`order_amount`,
  o.`pay_amount`,
  o.`created_at` AS `order_created_at`,
  o.`paid_at` AS `order_paid_at`,
  NOW() AS `created_at`,
  NOW() AS `updated_at`
FROM `biz_order` o
LEFT JOIN `biz_user` u ON u.`id` = o.`user_id`
LEFT JOIN `biz_shop` s ON s.`id` = o.`shop_id`
LEFT JOIN `biz_order_address` a ON a.`order_id` = o.`id`
LEFT JOIN (
  SELECT
    `order_id`,
    GROUP_CONCAT(`product_name` ORDER BY `id` SEPARATOR ' ') AS `product_keywords`
  FROM `biz_order_item`
  GROUP BY `order_id`
) p ON p.`order_id` = o.`id`
WHERE o.`tenant_id` = 10001;
```

批量重建应尽量分批处理，避免一次性扫描和写入过多数据。生产环境可以按租户、日期范围、业务 ID 范围分片执行。

### 常见问题

搜索辅助表模型的主要问题集中在数据一致性、冗余字段维护、模糊搜索性能和搜索口径控制。

| 问题                   | 原因                                 | 处理建议                         |
| ---------------------- | ------------------------------------ | -------------------------------- |
| 搜索结果和主表不一致   | 辅助表同步失败或异步延迟             | 增加重试、补偿任务和定期校验     |
| 搜索辅助表字段越来越多 | 所有列表需求都堆到一张表             | 按业务主题拆分搜索辅助表         |
| 模糊搜索很慢           | 使用 `LIKE '%keyword%'` 扫描大量数据 | 使用全文索引、前缀匹配或搜索引擎 |
| 关联名称更新后搜不到   | 冗余字段未同步更新                   | 明确冗余字段同步策略             |
| 深分页查询慢           | 使用大 `OFFSET` 分页                 | 使用游标分页或限制最大翻页深度   |
| 写入成本变高           | 辅助表索引过多、同步逻辑复杂         | 精简索引，异步写入，批量更新     |
| 搜索结果重复           | 一个业务对象生成多条辅助记录         | 使用业务对象 ID 唯一约束         |
| 搜索字段语义混乱       | `search_text` 拼接字段无规范         | 统一拼接顺序、分隔符和字段来源   |

使用搜索辅助表时，需要特别注意数据一致性边界。搜索辅助表通常可以接受短暂延迟，但不能长期与主业务表不一致。对于订单、支付、退款等关键业务状态，必须有可靠的同步、重试和补偿机制。

对于模糊搜索，不要默认认为搜索辅助表一定能解决性能问题。如果数据量达到百万级、千万级，并且存在大量复杂文本搜索，应考虑将搜索能力迁移到专业搜索引擎。MySQL 搜索辅助表更适合中等规模、查询条件明确、搜索复杂度可控的业务系统。

### 总结

搜索辅助表模型通过冗余和重组业务字段，将复杂的跨表查询、列表筛选和关键字搜索转化为对单张辅助表的查询，从而提升后台列表、运营查询和客服检索的性能。

设计该模型时，应重点关注三个方面：第一，搜索字段要围绕真实查询场景设计；第二，辅助表只服务查询，不替代业务主表；第三，必须建立可靠的同步和补偿机制。

对于简单精确查询，应优先使用普通组合索引；对于低频小规模模糊查询，可以使用 `LIKE`；对于中等复杂度文本搜索，可以使用 MySQL 全文索引；对于高复杂度搜索，应使用专业搜索引擎。搜索辅助表的价值在于让业务查询更稳定、更清晰、更可控，而不是把所有搜索问题都堆到一张冗余表中。



## 读写分离模型

读写分离模型用于将业务系统中的写入请求和读取请求拆分到不同的数据库节点上。主库负责写入和强一致读，从库负责普通查询和报表查询，从而降低主库查询压力，提高系统整体读取能力和稳定性。

### 适用场景

读写分离模型适合读请求明显多于写请求，并且部分读取场景可以接受短暂主从延迟的业务系统。它通常用于后台管理系统、订单查询系统、用户中心、商品中心、内容系统、报表查询和运营看板等场景。

常见适用场景包括：

| 场景           | 说明                                                 |
| -------------- | ---------------------------------------------------- |
| 读多写少业务   | 商品、文章、配置、字典、用户资料等查询频繁但写入较少 |
| 列表查询压力大 | 后台分页列表、条件筛选、运营查询消耗大量数据库资源   |
| 报表查询较重   | 报表、统计、导出等查询容易影响主库写入               |
| 多端查询并发高 | App、小程序、Web、后台同时读取同一批业务数据         |
| 主库写入敏感   | 订单、支付、库存、账户等核心写入链路不能被慢查询拖慢 |
| 可接受短暂延迟 | 普通列表、历史记录、非关键状态查询可以接受秒级延迟   |

读写分离不适合所有查询都要求强一致的场景。例如支付后立即查询支付状态、库存扣减后立即校验库存、账户余额变更后立即展示最新余额等场景，应优先走主库或使用强一致读取策略。

### 建模结构

读写分离模型通常不是单纯依赖某一张表完成，而是由主库、从库、业务表、读写路由规则和一致性控制机制共同组成。

在数据建模层面，业务表仍然以主库为写入源。从库通过 MySQL 主从复制同步数据。应用层根据请求类型决定访问主库还是从库。

典型结构如下：

```text
应用服务
  ├── 写入请求：INSERT、UPDATE、DELETE
  │       └── 主库
  │
  ├── 强一致读取：写后立即读、支付状态、账户余额、库存校验
  │       └── 主库
  │
  └── 普通读取：列表、详情、历史记录、报表、搜索
          └── 从库

主库
  └── binlog 复制
          └── 从库
```

以订单业务为例，订单主表仍然只有一份逻辑模型。写入订单、更新订单状态、支付回调、取消订单等操作进入主库。订单列表、订单历史、客服查询、运营查询等普通读取可以进入从库。

```sql
-- 订单主表：逻辑上只有一张业务表，物理上由主库写入、从库复制
CREATE TABLE `biz_order` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `order_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '订单金额',
  `pay_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '支付金额',
  `order_status` varchar(32) NOT NULL COMMENT '订单状态',
  `pay_status` varchar(32) NOT NULL COMMENT '支付状态',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '版本号',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
```

为了控制写后读一致性，可以增加读写分离一致性标记表。该表不保存业务明细，只保存用户、业务对象或请求维度的最近写入时间，用于判断短时间内是否强制走主库。

```sql
-- 读写分离一致性标记表：记录业务对象最近写入时间，用于写后读走主库判断
CREATE TABLE `rw_consistency_marker` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `biz_id` bigint NOT NULL COMMENT '业务ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `last_write_at` datetime NOT NULL COMMENT '最近写入时间',
  `expire_at` datetime NOT NULL COMMENT '强制主库读取过期时间',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读写分离一致性标记表';
```

如果系统需要记录读写路由策略，也可以建立读写路由规则表，用于管理不同业务接口、业务类型或查询场景的读写策略。

```sql
-- 读写路由规则表：保存不同业务场景的读写路由策略
CREATE TABLE `rw_route_rule` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型',
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型',
  `route_type` varchar(32) NOT NULL COMMENT '路由类型',
  `force_master_seconds` int NOT NULL DEFAULT '0' COMMENT '写入后强制走主库秒数',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读写路由规则表';
```

`operation_type` 可以取值为 `READ`、`WRITE`、`READ_AFTER_WRITE`、`REPORT` 等。`route_type` 可以取值为 `MASTER`、`SLAVE`、`AUTO`。其中 `MASTER` 表示强制主库，`SLAVE` 表示优先从库，`AUTO` 表示由应用层根据一致性标记和请求上下文自动判断。

### 字段设计

读写分离模型中的业务表字段不需要因为读写分离而大幅改变，但应补充有利于一致性判断、并发控制和复制排查的字段。

推荐字段分类如下：

| 字段类型         | 示例字段                         | 说明                                         |
| ---------------- | -------------------------------- | -------------------------------------------- |
| 主键字段         | `id`                             | 业务表主键，主从库保持一致                   |
| 租户字段         | `tenant_id`                      | 多租户系统中用于数据隔离                     |
| 业务标识字段     | `order_no`、`user_id`、`shop_id` | 支撑业务查询和路由判断                       |
| 状态字段         | `order_status`、`pay_status`     | 读写分离场景中经常用于判断是否需要强一致读取 |
| 版本字段         | `version`                        | 用于乐观锁更新，减少并发覆盖                 |
| 创建时间字段     | `created_at`                     | 用于查询、排序和复制排查                     |
| 更新时间字段     | `updated_at`                     | 用于判断数据新旧和同步延迟                   |
| 最近写入时间字段 | `last_write_at`                  | 用于一致性标记表                             |
| 过期时间字段     | `expire_at`                      | 用于控制强制主库读取窗口                     |

字段设计建议如下：

1. 核心业务表建议保留 `updated_at` 字段，便于排查从库数据是否落后。
2. 高并发更新表建议保留 `version` 字段，用于乐观锁控制。
3. 多租户系统中，业务表和辅助表都应保留 `tenant_id` 字段。
4. 一致性标记表应保存 `biz_type` 和 `biz_id`，避免不同业务对象之间互相影响。
5. 如果写后读按用户维度控制，可以在一致性标记表中保存 `user_id`。
6. `expire_at` 应由应用层根据业务重要程度设置，例如写入后 3 秒、5 秒或 10 秒强制走主库。
7. 路由规则表中的 `route_type` 不应设计得过于复杂，避免运行时判断不可控。
8. 不建议在业务表中保存数据库节点信息，例如主库标识、从库标识。节点选择应由数据源路由层处理。

### 索引设计

读写分离模型的索引设计仍然以业务查询为核心，但需要同时考虑主库写入成本和从库查询性能。主库索引过多会影响写入，从库索引不足会导致查询慢，因此需要根据读写分离后的访问特点进行平衡。

订单主表推荐索引如下：

```sql
-- 主键索引
ALTER TABLE `biz_order`
  ADD PRIMARY KEY (`id`);

-- 订单编号唯一索引，支持订单号精确查询
ALTER TABLE `biz_order`
  ADD UNIQUE KEY `uk_order_no` (`order_no`);

-- 按租户、用户、创建时间查询用户订单列表
ALTER TABLE `biz_order`
  ADD KEY `idx_tenant_user_created` (`tenant_id`, `user_id`, `created_at`);

-- 按租户、店铺、创建时间查询店铺订单列表
ALTER TABLE `biz_order`
  ADD KEY `idx_tenant_shop_created` (`tenant_id`, `shop_id`, `created_at`);

-- 按租户、订单状态、创建时间查询后台订单列表
ALTER TABLE `biz_order`
  ADD KEY `idx_tenant_status_created` (`tenant_id`, `order_status`, `created_at`);

-- 按租户、支付状态、支付时间查询支付订单
ALTER TABLE `biz_order`
  ADD KEY `idx_tenant_pay_paid` (`tenant_id`, `pay_status`, `paid_at`);
```

一致性标记表推荐索引如下：

```sql
-- 主键索引
ALTER TABLE `rw_consistency_marker`
  ADD PRIMARY KEY (`id`);

-- 按业务类型和业务ID唯一定位最近写入标记
ALTER TABLE `rw_consistency_marker`
  ADD UNIQUE KEY `uk_tenant_biz` (`tenant_id`, `biz_type`, `biz_id`);

-- 按用户维度查询最近写入标记
ALTER TABLE `rw_consistency_marker`
  ADD KEY `idx_tenant_user_expire` (`tenant_id`, `user_id`, `expire_at`);

-- 按过期时间清理历史标记
ALTER TABLE `rw_consistency_marker`
  ADD KEY `idx_expire_at` (`expire_at`);
```

读写路由规则表推荐索引如下：

```sql
-- 主键索引
ALTER TABLE `rw_route_rule`
  ADD PRIMARY KEY (`id`);

-- 规则编码唯一索引
ALTER TABLE `rw_route_rule`
  ADD UNIQUE KEY `uk_rule_code` (`rule_code`);

-- 按业务类型和操作类型查询路由规则
ALTER TABLE `rw_route_rule`
  ADD KEY `idx_biz_operation_enabled` (`biz_type`, `operation_type`, `enabled`);
```

索引设计建议如下：

1. 主库写入频繁的表不宜建立过多低价值索引，否则会增加写入成本和复制压力。
2. 从库承担大量查询时，应重点优化列表查询、时间范围查询和状态筛选查询。
3. 如果从库用于报表或导出，可以考虑单独建设报表从库，并在报表从库上增加查询型索引。
4. 强一致读取走主库时，应确保主库上的关键查询也有必要索引，避免强一致查询拖慢写入链路。
5. 一致性标记表通常写入频繁、生命周期短，应建立必要索引并定期清理过期数据。
6. 不建议为了读写分离增加无业务价值的索引。索引仍然应服务明确的查询条件。
7. 对于超大表，应结合分区表、冷热数据、归档表或搜索辅助表共同优化。

### 常用查询

读写分离模型的常用查询重点不是 SQL 写法本身，而是查询应该进入主库还是从库。不同业务场景应明确读路由策略，避免所有查询都默认走从库或所有查询都强制走主库。

#### 写后立即查询订单详情

订单创建、支付、取消、退款后立即查询详情，通常需要读取最新状态，应强制走主库。

```sql
SELECT
  `id`,
  `tenant_id`,
  `order_no`,
  `user_id`,
  `shop_id`,
  `order_amount`,
  `pay_amount`,
  `order_status`,
  `pay_status`,
  `version`,
  `created_at`,
  `paid_at`,
  `updated_at`
FROM `biz_order`
WHERE `tenant_id` = 10001
  AND `id` = 900001;
```

该查询适合以下场景：

- 创建订单后立即进入订单详情页。
- 支付成功后立即刷新订单状态。
- 取消订单后立即查看订单状态。
- 退款申请后立即查看退款相关状态。

这些场景不建议走从库，否则可能出现用户刚操作成功但页面仍显示旧状态的问题。

#### 查询用户订单列表

用户订单列表通常可以走从库。如果用户刚刚创建或支付订单，可以在短时间内强制走主库，过了强一致窗口后再走从库。

```sql
SELECT
  `id`,
  `order_no`,
  `shop_id`,
  `order_amount`,
  `pay_amount`,
  `order_status`,
  `pay_status`,
  `created_at`,
  `paid_at`
FROM `biz_order`
WHERE `tenant_id` = 10001
  AND `user_id` = 30001
ORDER BY `created_at` DESC
LIMIT 20;
```

应用层可以先检查一致性标记：

```sql
SELECT
  `id`,
  `last_write_at`,
  `expire_at`
FROM `rw_consistency_marker`
WHERE `tenant_id` = 10001
  AND `biz_type` = 'ORDER'
  AND `user_id` = 30001
  AND `expire_at` > NOW()
ORDER BY `expire_at` DESC
LIMIT 1;
```

如果存在未过期标记，则订单列表走主库；如果不存在，则订单列表走从库。

#### 查询后台订单列表

后台订单列表通常属于普通管理查询，可以优先走从库，避免影响主库写入。

```sql
SELECT
  `id`,
  `order_no`,
  `user_id`,
  `shop_id`,
  `order_amount`,
  `pay_amount`,
  `order_status`,
  `pay_status`,
  `created_at`,
  `paid_at`
FROM `biz_order`
WHERE `tenant_id` = 10001
  AND `order_status` = 'PAID'
  AND `created_at` >= '2025-01-01 00:00:00'
  AND `created_at` < '2025-02-01 00:00:00'
ORDER BY `created_at` DESC
LIMIT 20;
```

如果后台操作人员刚刚修改订单状态，并且需要立即确认修改结果，可以将该次查询切换到主库。

#### 查询报表数据

报表查询、导出查询和统计查询通常应走从库或专用报表库。如果数据量较大，不建议直接在主库执行。

```sql
SELECT
  DATE(`created_at`) AS `stat_date`,
  COUNT(*) AS `order_count`,
  SUM(`pay_amount`) AS `pay_amount`
FROM `biz_order`
WHERE `tenant_id` = 10001
  AND `pay_status` = 'PAID'
  AND `created_at` >= '2025-01-01 00:00:00'
  AND `created_at` < '2025-02-01 00:00:00'
GROUP BY DATE(`created_at`)
ORDER BY `stat_date` ASC;
```

如果报表查询频繁，应优先考虑统计汇总模型，而不是长期在从库上直接扫描明细表。

#### 查询路由规则

该查询用于应用启动、配置刷新或运行时判断不同业务场景的读写路由策略。

```sql
SELECT
  `rule_code`,
  `biz_type`,
  `operation_type`,
  `route_type`,
  `force_master_seconds`,
  `enabled`
FROM `rw_route_rule`
WHERE `biz_type` = 'ORDER'
  AND `operation_type` = 'READ_AFTER_WRITE'
  AND `enabled` = 1;
```

路由规则通常可以缓存到应用内存或 Redis 中，不建议每次业务查询都访问规则表。

#### 清理过期一致性标记

一致性标记表中的数据生命周期较短，应定期清理过期数据，避免表无限增长。

```sql
DELETE FROM `rw_consistency_marker`
WHERE `expire_at` < DATE_SUB(NOW(), INTERVAL 1 DAY)
LIMIT 1000;
```

该清理 SQL 可以由定时任务分批执行。不要一次性删除大量数据，避免产生长事务和主从复制延迟。

### 常用写入

读写分离模型中的写入请求必须进入主库。写入完成后，可以根据业务需要写入一致性标记，用于控制后续短时间内的读取路由。

#### 创建订单

创建订单属于核心写入操作，应进入主库。写入订单后，可以写入一致性标记，使用户短时间内查询订单详情或订单列表时强制走主库。

```sql
INSERT INTO `biz_order` (
  `id`,
  `tenant_id`,
  `order_no`,
  `user_id`,
  `shop_id`,
  `order_amount`,
  `pay_amount`,
  `order_status`,
  `pay_status`,
  `version`,
  `created_at`,
  `paid_at`,
  `updated_at`
) VALUES (
  900001,
  10001,
  'OD202501150001',
  30001,
  20001,
  199.00,
  0.00,
  'CREATED',
  'UNPAID',
  0,
  NOW(),
  NULL,
  NOW()
);
INSERT INTO `rw_consistency_marker` (
  `id`,
  `tenant_id`,
  `biz_type`,
  `biz_id`,
  `user_id`,
  `last_write_at`,
  `expire_at`,
  `created_at`,
  `updated_at`
) VALUES (
  700001,
  10001,
  'ORDER',
  900001,
  30001,
  NOW(),
  DATE_ADD(NOW(), INTERVAL 5 SECOND),
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE
  `user_id` = VALUES(`user_id`),
  `last_write_at` = VALUES(`last_write_at`),
  `expire_at` = VALUES(`expire_at`),
  `updated_at` = NOW();
```

#### 支付成功后更新订单

支付成功属于核心状态变更，应进入主库。更新订单后，应刷新一致性标记，让后续订单详情和订单列表短时间内走主库。

```sql
UPDATE `biz_order`
SET
  `pay_amount` = 199.00,
  `order_status` = 'PAID',
  `pay_status` = 'PAID',
  `paid_at` = NOW(),
  `version` = `version` + 1,
  `updated_at` = NOW()
WHERE `tenant_id` = 10001
  AND `id` = 900001
  AND `version` = 0;
INSERT INTO `rw_consistency_marker` (
  `id`,
  `tenant_id`,
  `biz_type`,
  `biz_id`,
  `user_id`,
  `last_write_at`,
  `expire_at`,
  `created_at`,
  `updated_at`
) VALUES (
  700002,
  10001,
  'ORDER',
  900001,
  30001,
  NOW(),
  DATE_ADD(NOW(), INTERVAL 10 SECOND),
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE
  `last_write_at` = VALUES(`last_write_at`),
  `expire_at` = VALUES(`expire_at`),
  `updated_at` = NOW();
```

支付、账户、库存等关键业务可以设置更长的强制主库读取时间，避免主从延迟导致用户看到旧状态。

#### 更新路由规则

路由规则属于配置类写入，应进入主库。更新后可以通知应用刷新缓存。

```sql
UPDATE `rw_route_rule`
SET
  `route_type` = 'MASTER',
  `force_master_seconds` = 10,
  `remark` = '订单写后读强制走主库10秒',
  `updated_at` = NOW()
WHERE `rule_code` = 'ORDER_READ_AFTER_WRITE';
```

如果规则表由配置中心或后台管理维护，应限制修改权限，并保留操作日志，避免误改导致大量读请求打到主库。

#### 写入读写路由规则

初始化系统时，可以写入常见读写路由规则。

```sql
INSERT INTO `rw_route_rule` (
  `id`,
  `rule_code`,
  `biz_type`,
  `operation_type`,
  `route_type`,
  `force_master_seconds`,
  `enabled`,
  `remark`,
  `created_at`,
  `updated_at`
) VALUES
(
  600001,
  'ORDER_WRITE',
  'ORDER',
  'WRITE',
  'MASTER',
  0,
  1,
  '订单写入强制走主库',
  NOW(),
  NOW()
),
(
  600002,
  'ORDER_READ',
  'ORDER',
  'READ',
  'SLAVE',
  0,
  1,
  '订单普通读取优先走从库',
  NOW(),
  NOW()
),
(
  600003,
  'ORDER_READ_AFTER_WRITE',
  'ORDER',
  'READ_AFTER_WRITE',
  'AUTO',
  10,
  1,
  '订单写后读自动判断，必要时走主库',
  NOW(),
  NOW()
);
```

### 常见问题

读写分离模型最常见的问题是主从延迟引起的数据不一致，其次是路由规则混乱、慢查询转移、从库不可用和事务边界处理不当。

| 问题                 | 原因                           | 处理建议                                         |
| -------------------- | ------------------------------ | ------------------------------------------------ |
| 写入后查询不到数据   | 查询走了从库，从库尚未同步     | 写后读短时间强制走主库                           |
| 页面显示旧状态       | 主从延迟导致读取到旧数据       | 对关键状态查询走主库                             |
| 主库压力没有下降     | 大量查询被错误路由到主库       | 梳理强一致查询范围，普通查询走从库               |
| 从库压力过高         | 报表、导出、复杂搜索集中在从库 | 增加报表库、统计汇总表或搜索辅助表               |
| 从库慢查询影响复制   | 从库执行长查询占用资源         | 控制报表查询范围，拆分任务，使用专用分析库       |
| 事务内读到旧数据     | 写入后同一业务流程读取走了从库 | 事务内读取强制走主库                             |
| 路由规则难维护       | 规则维度过多或业务侵入严重     | 规则保持简单，按业务类型和操作类型控制           |
| 从库故障导致查询失败 | 缺少从库降级策略               | 从库不可用时降级到主库或返回明确错误             |
| 数据库连接混乱       | 应用层数据源切换不清晰         | 使用统一数据源路由组件，禁止业务代码手动拼接连接 |

对于主从延迟，不能只依赖固定等待时间解决。固定等待会降低用户体验，也不能保证复制一定完成。更合理的方式是按照业务重要程度分级：

| 读取类型                     | 推荐路由             |
| ---------------------------- | -------------------- |
| 支付状态、账户余额、库存校验 | 主库                 |
| 写入后立即读取详情           | 主库                 |
| 用户刚操作后的列表刷新       | 短时间主库，之后从库 |
| 普通分页列表                 | 从库                 |
| 后台历史查询                 | 从库                 |
| 大报表、导出                 | 专用从库或统计表     |

读写分离也不能替代 SQL 优化。如果一个查询在主库很慢，迁移到从库后通常只是把压力转移到从库，并不会从根本上解决问题。复杂统计应结合统计汇总模型，复杂搜索应结合搜索辅助表或搜索引擎，大数据量查询应结合分页、归档、分区或分库分表。

### 总结

读写分离模型通过将写入请求和读取请求拆分到不同数据库节点，降低主库查询压力，提高系统读取能力和整体稳定性。它适合读多写少、查询压力明显、部分读取场景可接受短暂延迟的业务系统。

设计该模型时，应重点关注三个方面：第一，写入和强一致读取必须走主库；第二，普通列表、历史查询和报表查询可以走从库；第三，必须处理主从延迟带来的写后读一致性问题。

读写分离不是简单地把所有 `SELECT` 都路由到从库。事务内读取、写后立即读取、支付状态、账户余额、库存校验等关键场景仍然应走主库。对于普通查询，可以优先走从库；对于复杂报表和搜索，应结合统计汇总模型、搜索辅助表模型或专用分析库共同设计。

