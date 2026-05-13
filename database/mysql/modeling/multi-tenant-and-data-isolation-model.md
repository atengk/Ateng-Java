# 多租户与数据隔离模型

## 多租户模型

多租户模型用于在同一套系统中承载多个租户的数据与业务操作，并通过租户标识、数据库隔离策略、权限控制和查询约束实现数据边界。MySQL 8 中常见的多租户建模方式包括共享库共享表、共享库独立表、独立库独立表以及混合隔离模型，业务系统中最常用的是共享库共享表加 `tenant_id` 的方式。

### 适用场景

多租户模型适用于 SaaS 平台、集团化系统、多组织业务系统、开放平台、渠道系统以及需要按客户、企业、门店、机构或项目隔离数据的业务场景。

典型场景包括：

- SaaS 系统中，一个平台服务多个企业客户。
- 集团系统中，不同子公司、事业部或区域组织需要共享同一套系统。
- 平台型系统中，不同商户、供应商、渠道方需要隔离业务数据。
- 内部管理系统中，不同部门或组织需要独立维护业务数据。
- 同一业务表中需要按租户进行查询、统计、授权和数据清理。

常见隔离方式如下：

| 隔离方式     | 说明                                            | 适用场景                                |
| ------------ | ----------------------------------------------- | --------------------------------------- |
| 共享库共享表 | 所有租户共用同一套表，通过 `tenant_id` 区分数据 | 中小型 SaaS、标准化业务、多租户数量较多 |
| 共享库独立表 | 同一数据库中，不同租户使用不同表                | 租户较少、定制化较强、维护成本可接受    |
| 独立库独立表 | 每个租户使用独立数据库                          | 大客户隔离、强合规、强安全、独立运维    |
| 混合隔离     | 普通租户共享表，大客户独立库                    | SaaS 平台常见的商业化分层模型           |

多数业务系统优先使用共享库共享表模型，因为该模型结构简单、资源利用率高、开发维护成本低，适合标准化业务快速扩展。

### 建模结构

多租户模型的核心是将租户作为业务数据的归属边界。平台通常需要维护一张租户主表，业务表通过 `tenant_id` 字段与租户建立逻辑归属关系。

建模时可以将表分为三类：

| 表类型       | 是否需要 `tenant_id` | 说明                                             |
| ------------ | -------------------- | ------------------------------------------------ |
| 平台级基础表 | 不需要               | 平台全局配置、系统参数、租户套餐、租户信息       |
| 租户级业务表 | 需要                 | 订单、商品、客户、部门、角色、业务单据等         |
| 混合型配置表 | 视情况而定           | 既可能存在平台默认配置，也可能存在租户自定义配置 |

推荐的基础结构如下：

```sql
-- 租户主表：维护租户基础信息、状态和隔离策略
CREATE TABLE sys_tenant (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_code VARCHAR(64) NOT NULL COMMENT '租户编码',
    tenant_name VARCHAR(128) NOT NULL COMMENT '租户名称',
    tenant_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD' COMMENT '租户类型：STANDARD-标准租户，VIP-大客户租户',
    isolation_mode VARCHAR(32) NOT NULL DEFAULT 'SHARED_TABLE' COMMENT '隔离模式：SHARED_TABLE-共享表，DEDICATED_DB-独立库',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人姓名',
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系人手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0-正常，1-删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户表';
```

租户级业务表需要显式保存 `tenant_id`。下面以客户表为例：

```sql
-- 租户客户表：通过 tenant_id 标识客户数据归属租户
CREATE TABLE biz_customer (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    customer_no VARCHAR(64) NOT NULL COMMENT '客户编号',
    customer_name VARCHAR(128) NOT NULL COMMENT '客户名称',
    customer_level VARCHAR(32) DEFAULT NULL COMMENT '客户等级',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人姓名',
    contact_phone VARCHAR(32) DEFAULT NULL COMMENT '联系人手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0-正常，1-删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户表';
```

订单、商品、库存、流水、用户、角色等租户级表也应采用相同模式：

```sql
-- 租户订单表：所有订单数据都必须归属到具体租户
CREATE TABLE biz_order (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    order_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
    order_status VARCHAR(32) NOT NULL COMMENT '订单状态',
    pay_status VARCHAR(32) NOT NULL COMMENT '支付状态',
    order_time DATETIME NOT NULL COMMENT '下单时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0-正常，1-删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';
```

建模时需要遵循以下原则：

- 所有租户级业务表必须包含 `tenant_id`。
- 租户级数据查询、更新、删除必须带上 `tenant_id` 条件。
- 平台级表不应误加 `tenant_id`，避免全局配置被错误拆分。
- 租户表本身通常不需要 `tenant_id`，因为它是租户边界的来源。
- 租户隔离不应只依赖前端传参，后端必须从登录上下文或网关上下文中获取租户。
- 跨租户统计、平台运维、数据迁移等能力应走单独的管理权限链路。

### 字段设计

多租户字段设计的重点是保证租户归属清晰、业务唯一性明确、数据生命周期可控，并避免不同租户之间的数据互相污染。

租户主表常用字段如下：

| 字段名           | 类型           | 是否必填 | 说明                             |
| ---------------- | -------------- | -------- | -------------------------------- |
| `id`             | `BIGINT`       | 是       | 租户主键ID                       |
| `tenant_code`    | `VARCHAR(64)`  | 是       | 租户编码，通常用于外部识别       |
| `tenant_name`    | `VARCHAR(128)` | 是       | 租户名称                         |
| `tenant_type`    | `VARCHAR(32)`  | 是       | 租户类型，如标准租户、大客户租户 |
| `isolation_mode` | `VARCHAR(32)`  | 是       | 隔离模式，如共享表、独立库       |
| `status`         | `TINYINT`      | 是       | 租户状态                         |
| `expire_time`    | `DATETIME`     | 否       | 租户过期时间                     |
| `deleted`        | `TINYINT`      | 是       | 软删除标识                       |

租户级业务表通用字段如下：

| 字段名        | 类型                      | 是否必填   | 说明         |
| ------------- | ------------------------- | ---------- | ------------ |
| `id`          | `BIGINT`                  | 是         | 业务表主键ID |
| `tenant_id`   | `BIGINT`                  | 是         | 租户ID       |
| `create_time` | `DATETIME`                | 是         | 创建时间     |
| `update_time` | `DATETIME`                | 是         | 更新时间     |
| `deleted`     | `TINYINT`                 | 是         | 软删除标识   |
| `status`      | `TINYINT` / `VARCHAR(32)` | 视业务而定 | 业务状态     |

字段设计建议：

- `tenant_id` 使用 `BIGINT`，与租户主表主键类型保持一致。
- `tenant_id` 不允许为空，避免出现无法归属的数据。
- 业务编号通常需要按租户维度唯一，例如同一租户下客户编号唯一，不同租户可以重复。
- 租户状态建议使用 `status` 控制启用、禁用，不建议直接删除租户。
- 租户过期、套餐、容量限制等商业化字段可以放在租户扩展表中，避免主表过重。
- 独立库模式下，业务表也建议保留 `tenant_id`，便于迁移、审计和混合模式统一处理。

### 索引设计

多租户索引设计的核心原则是：租户级业务查询的联合索引通常以 `tenant_id` 作为前导字段，再组合业务查询条件。

租户主表索引示例：

```sql
-- 租户编码需要全局唯一，便于登录识别、外部系统对接和租户路由
ALTER TABLE sys_tenant
    ADD UNIQUE KEY uk_tenant_code (tenant_code);

-- 按状态和过期时间筛选租户，用于平台后台查询和过期租户扫描
ALTER TABLE sys_tenant
    ADD KEY idx_status_expire_time (status, expire_time);
```

客户表索引示例：

```sql
-- 同一租户下客户编号唯一，不同租户之间允许客户编号重复
ALTER TABLE biz_customer
    ADD UNIQUE KEY uk_tenant_customer_no (tenant_id, customer_no);

-- 租户内按客户名称模糊或前缀检索时使用
ALTER TABLE biz_customer
    ADD KEY idx_tenant_customer_name (tenant_id, customer_name);

-- 租户内按状态和创建时间分页查询
ALTER TABLE biz_customer
    ADD KEY idx_tenant_status_create_time (tenant_id, status, create_time);
```

订单表索引示例：

```sql
-- 同一租户下订单编号唯一
ALTER TABLE biz_order
    ADD UNIQUE KEY uk_tenant_order_no (tenant_id, order_no);

-- 租户内按客户查询订单列表
ALTER TABLE biz_order
    ADD KEY idx_tenant_customer_id (tenant_id, customer_id);

-- 租户内按订单状态和下单时间分页查询
ALTER TABLE biz_order
    ADD KEY idx_tenant_order_status_time (tenant_id, order_status, order_time);

-- 租户内按支付状态和下单时间统计或筛选
ALTER TABLE biz_order
    ADD KEY idx_tenant_pay_status_time (tenant_id, pay_status, order_time);
```

索引设计建议：

- 租户级查询索引优先使用 `(tenant_id, 业务字段)` 结构。
- 租户内唯一约束优先使用 `(tenant_id, 业务唯一字段)`。
- 不建议只给 `tenant_id` 单独建大量索引，除非确实存在只按租户全量扫描的稳定场景。
- 高频分页查询应将 `tenant_id`、状态字段、时间字段组合到同一个索引中。
- 跨租户平台统计不应破坏租户级索引设计，可以单独建设统计表或汇总表。
- 字段选择性较低时，应结合业务过滤条件设计联合索引，而不是孤立建索引。

### 常用查询

多租户查询必须确保租户条件不可绕过。普通租户用户的所有业务查询都应自动携带 `tenant_id`，平台管理员的跨租户查询应走明确的管理接口和权限控制。

#### 查询当前租户客户列表

该查询用于租户后台分页查看客户数据，必须同时过滤 `tenant_id` 和 `deleted`。

```sql
SELECT
    id,
    tenant_id,
    customer_no,
    customer_name,
    customer_level,
    contact_name,
    contact_phone,
    status,
    create_time
FROM biz_customer
WHERE tenant_id = 10001
  AND deleted = 0
  AND status = 1
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

#### 按客户编号查询当前租户客户

该查询用于根据业务编号查询客户详情，客户编号只在租户内唯一，所以必须带上 `tenant_id`。

```sql
SELECT
    id,
    tenant_id,
    customer_no,
    customer_name,
    customer_level,
    contact_name,
    contact_phone,
    status,
    remark,
    create_time,
    update_time
FROM biz_customer
WHERE tenant_id = 10001
  AND customer_no = 'CUST202605130001'
  AND deleted = 0
LIMIT 1;
```

#### 查询当前租户订单列表

该查询用于租户内按订单状态分页查询订单，适合订单中心、业务后台和运营查询。

```sql
SELECT
    id,
    tenant_id,
    order_no,
    customer_id,
    order_amount,
    order_status,
    pay_status,
    order_time,
    create_time
FROM biz_order
WHERE tenant_id = 10001
  AND deleted = 0
  AND order_status = 'CONFIRMED'
ORDER BY order_time DESC
LIMIT 20 OFFSET 0;
```

#### 查询当前租户指定客户的订单

该查询用于客户详情页查看客户订单记录，必须同时限制租户和客户ID。

```sql
SELECT
    id,
    tenant_id,
    order_no,
    customer_id,
    order_amount,
    order_status,
    pay_status,
    order_time
FROM biz_order
WHERE tenant_id = 10001
  AND customer_id = 90001
  AND deleted = 0
ORDER BY order_time DESC
LIMIT 20 OFFSET 0;
```

#### 查询当前租户订单金额汇总

该查询用于租户内统计指定时间范围内的订单金额，常用于租户首页看板和经营报表。

```sql
SELECT
    tenant_id,
    COUNT(*) AS order_count,
    COALESCE(SUM(order_amount), 0.00) AS total_order_amount
FROM biz_order
WHERE tenant_id = 10001
  AND deleted = 0
  AND order_status = 'CONFIRMED'
  AND order_time >= '2026-05-01 00:00:00'
  AND order_time < '2026-06-01 00:00:00'
GROUP BY tenant_id;
```

#### 平台管理员跨租户查询租户数据

该查询仅适用于平台管理员、审计人员或运维人员，不应开放给普通租户用户。

```sql
SELECT
    t.id AS tenant_id,
    t.tenant_code,
    t.tenant_name,
    COUNT(o.id) AS order_count,
    COALESCE(SUM(o.order_amount), 0.00) AS total_order_amount
FROM sys_tenant t
LEFT JOIN biz_order o
       ON o.tenant_id = t.id
      AND o.deleted = 0
      AND o.order_time >= '2026-05-01 00:00:00'
      AND o.order_time < '2026-06-01 00:00:00'
WHERE t.deleted = 0
  AND t.status = 1
GROUP BY
    t.id,
    t.tenant_code,
    t.tenant_name
ORDER BY total_order_amount DESC;
```

#### 查询租户是否可用

该查询用于登录、接口访问或租户路由前校验租户状态。

```sql
SELECT
    id,
    tenant_code,
    tenant_name,
    tenant_type,
    isolation_mode,
    status,
    expire_time
FROM sys_tenant
WHERE tenant_code = 'tenant_demo'
  AND deleted = 0
  AND status = 1
  AND (expire_time IS NULL OR expire_time > NOW())
LIMIT 1;
```

### 常用写入

多租户写入的关键是保证所有租户级业务数据都写入正确的 `tenant_id`。`tenant_id` 不建议由前端直接传入并完全信任，应由后端根据登录用户、访问域名、请求头、网关解析结果或租户上下文统一生成。

#### 创建租户

创建租户时需要先写入租户主表，再初始化租户默认配置、角色、管理员账号和基础数据。

```sql
INSERT INTO sys_tenant (
    id,
    tenant_code,
    tenant_name,
    tenant_type,
    isolation_mode,
    contact_name,
    contact_phone,
    status,
    expire_time,
    remark
) VALUES (
    10001,
    'tenant_demo',
    '演示租户',
    'STANDARD',
    'SHARED_TABLE',
    '张三',
    '13800000000',
    1,
    '2027-05-13 23:59:59',
    '演示租户初始化'
);
```

#### 创建当前租户客户

创建客户时，`tenant_id` 应由后端租户上下文写入，不能依赖前端手动填写。

```sql
INSERT INTO biz_customer (
    id,
    tenant_id,
    customer_no,
    customer_name,
    customer_level,
    contact_name,
    contact_phone,
    status,
    remark
) VALUES (
    90001,
    10001,
    'CUST202605130001',
    '杭州示例客户有限公司',
    'A',
    '李四',
    '13900000000',
    1,
    '重点客户'
);
```

#### 创建当前租户订单

创建订单时，订单、订单明细、库存流水、账户流水等关联数据必须使用同一个 `tenant_id`。

```sql
INSERT INTO biz_order (
    id,
    tenant_id,
    order_no,
    customer_id,
    order_amount,
    order_status,
    pay_status,
    order_time,
    remark
) VALUES (
    80001,
    10001,
    'ORD202605130001',
    90001,
    1999.00,
    'CONFIRMED',
    'UNPAID',
    NOW(),
    '租户内订单'
);
```

#### 更新当前租户客户

更新租户级业务数据时，必须在 `WHERE` 条件中带上 `tenant_id`，防止误更新其他租户数据。

```sql
UPDATE biz_customer
SET
    customer_name = '杭州示例客户集团有限公司',
    customer_level = 'S',
    update_time = NOW()
WHERE tenant_id = 10001
  AND id = 90001
  AND deleted = 0;
```

#### 软删除当前租户客户

删除租户级业务数据时，建议使用软删除，并带上租户条件。

```sql
UPDATE biz_customer
SET
    deleted = 1,
    update_time = NOW()
WHERE tenant_id = 10001
  AND id = 90001
  AND deleted = 0;
```

#### 禁用租户

禁用租户通常只修改租户状态，不直接删除租户数据。

```sql
UPDATE sys_tenant
SET
    status = 0,
    update_time = NOW()
WHERE id = 10001
  AND deleted = 0;
```

写入设计建议：

- 创建租户时应放在事务中处理租户、管理员账号、默认角色和初始化配置。
- 普通业务写入必须从上下文获取 `tenant_id`。
- 批量导入数据时，需要校验导入数据是否全部属于当前租户。
- 更新和删除必须带 `tenant_id` 条件。
- 租户禁用后，应在登录、接口鉴权、定时任务执行前统一拦截。
- 平台级运维操作需要单独记录操作日志和审计日志。

### 常见问题

多租户模型的问题通常集中在数据串租、索引失效、唯一约束设计错误、平台管理权限过大以及后期迁移困难。

| 问题                     | 原因                         | 建议                                               |
| ------------------------ | ---------------------------- | -------------------------------------------------- |
| 查询到了其他租户的数据   | SQL 未带 `tenant_id` 条件    | 后端统一注入租户条件，并对核心 SQL 做审查          |
| 不同租户业务编号冲突     | 唯一约束只使用了业务编号     | 使用 `(tenant_id, business_no)` 作为租户内唯一约束 |
| 租户数据量差异过大       | 大客户与普通客户共用同一套表 | 对大客户使用独立库或分表策略                       |
| 平台管理员误操作租户数据 | 管理端缺少权限边界           | 平台操作单独授权，并记录审计日志                   |
| 报表查询很慢             | 跨租户聚合直接扫业务明细表   | 建设统计汇总表或异步报表表                         |
| 后期迁移独立库困难       | 业务表缺少 `tenant_id`       | 即使独立库模式，也建议保留 `tenant_id` 字段        |
| 定时任务处理错租户       | 任务执行时没有租户上下文     | 定时任务按租户循环执行，并显式设置租户上下文       |

需要特别注意的是，多租户隔离不能只依赖数据库设计。应用层、权限层、缓存层、消息队列、搜索引擎、对象存储和日志系统都需要同步考虑租户边界。

例如缓存 Key 应包含租户标识：

```text
tenant:10001:customer:90001
tenant:10001:order:80001
```

对象存储路径也应包含租户标识：

```text
tenant/10001/customer/avatar/90001.png
tenant/10001/order/attachment/80001.pdf
```

消息队列事件中也应携带租户标识：

```json
{
  "tenantId": 10001,
  "eventType": "ORDER_CREATED",
  "orderId": 80001,
  "orderNo": "ORD202605130001"
}
```

### 总结

多租户模型是 SaaS 和平台型系统中最基础的数据隔离模型。共享库共享表加 `tenant_id` 是最常见的实现方式，适合多数标准化业务系统。

设计多租户模型时，应重点关注以下原则：

- 租户级业务表必须包含 `tenant_id`。
- 租户级查询、更新、删除必须带租户条件。
- 租户内唯一约束应使用 `(tenant_id, 业务唯一字段)`。
- 高频查询索引应优先围绕 `tenant_id` 和业务筛选字段设计。
- 平台级表和租户级表需要明确区分。
- 跨租户查询、统计和运维操作必须走独立权限链路。
- 缓存、消息、文件、日志和搜索数据也要包含租户边界。
- 对大客户、强合规客户或超大数据租户，可以升级为独立库或混合隔离模式。

如果业务处于早期阶段，推荐优先采用共享库共享表模型，并在所有租户级表中保留标准化的 `tenant_id` 字段。这样既能降低开发和运维成本，也能为后续独立库迁移、分库分表和混合隔离保留扩展空间。

