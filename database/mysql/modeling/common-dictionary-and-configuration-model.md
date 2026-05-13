# 通用字典与配置模型

通用字典与配置模型用于沉淀系统中变化频率较低、业务含义稳定、需要统一维护和复用的数据表达。常见内容包括下拉选项、枚举映射、业务状态、系统参数、灰度配置、草稿发布配置等。本章节属于《MySQL 8 常用业务建模模型》中的“通用字典与配置模型”部分。

## 字典表模型

字典表模型用于管理业务系统中的通用选项数据，例如性别、启用状态、订单状态、支付方式、证件类型、渠道来源等。它的核心目标是把硬编码枚举从业务代码中抽离出来，统一维护字典类型和字典项，保证前端展示、后端校验、报表统计和接口返回使用同一套业务含义。

### 适用场景

字典表模型适合存储“数量有限、含义稳定、需要展示名称、需要排序、可能需要启停”的业务选项数据。它通常服务于表单下拉框、查询筛选项、状态展示、数据导入校验、接口枚举映射等场景。

典型适用场景如下：

| 场景           | 示例                               | 说明                                   |
| -------------- | ---------------------------------- | -------------------------------------- |
| 表单下拉选项   | 性别、证件类型、学历、行业类型     | 前端需要展示名称，后端需要保存编码或值 |
| 业务状态展示   | 订单状态、支付状态、审核状态       | 状态值参与业务判断，展示名称可统一维护 |
| 筛选条件选项   | 渠道来源、客户等级、商品类型       | 查询页面需要动态加载筛选项             |
| 导入导出映射   | Excel 中的中文名称与数据库编码转换 | 导入时按字典校验，导出时按字典翻译     |
| 多端展示统一   | Web、App、管理后台展示一致         | 避免不同端维护多份枚举说明             |
| 低频配置型枚举 | 业务类型、操作类型、来源类型       | 允许后台维护，但不适合高频动态变更     |

不建议把强业务流程控制、金额规则、权限规则、复杂审批流直接建成普通字典表。此类数据通常需要独立业务表、状态机模型或规则配置模型承载。

### 建模结构

字典表模型通常拆分为“字典类型表”和“字典项表”。字典类型表定义一组字典的业务分类，字典项表定义该分类下的具体选项。业务表通常只保存字典项的 `item_value` 或稳定编码，不直接保存展示名称。

下面只给出表结构，不包含唯一索引和普通索引；索引集中放在后文索引设计部分。

字典类型表用于定义字典分类，例如 `gender`、`order_status`、`payment_type`。

```sql
CREATE TABLE sys_dict_type (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，0表示平台级公共字典',
    dict_code VARCHAR(64) NOT NULL COMMENT '字典编码，例如 gender、order_status',
    dict_name VARCHAR(128) NOT NULL COMMENT '字典名称，例如 性别、订单状态',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    deleted_at BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳，0表示未删除',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '字典类型表';
```

字典项表用于定义字典类型下的具体选项，例如 `gender` 下的 `male`、`female`，或者 `order_status` 下的 `created`、`paid`、`shipped`。

```sql
CREATE TABLE sys_dict_item (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，0表示平台级公共字典',
    dict_type_id BIGINT UNSIGNED NOT NULL COMMENT '字典类型ID',
    dict_code VARCHAR(64) NOT NULL COMMENT '冗余字典编码，便于按编码直接查询字典项',
    item_value VARCHAR(128) NOT NULL COMMENT '字典项值，业务表通常保存该值',
    item_label VARCHAR(128) NOT NULL COMMENT '字典项展示名称',
    item_alias VARCHAR(128) DEFAULT NULL COMMENT '字典项别名，用于导入识别或兼容历史名称',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号，值越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认项：0否，1是',
    color VARCHAR(32) DEFAULT NULL COMMENT '展示颜色，例如 success、warning、#67C23A',
    extra_json JSON DEFAULT NULL COMMENT '扩展属性，例如图标、国际化键、前端样式配置',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    deleted_at BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳，0表示未删除',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '字典项表';
```

该模型的核心关系是：一个字典类型对应多个字典项，业务表保存字典项值，页面展示时再通过字典项翻译为名称。`dict_code` 在字典项表中属于冗余字段，目的是减少常用查询中的关联成本；写入或修改字典类型编码时，需要同步维护字典项表中的 `dict_code`，实际业务中更推荐字典编码创建后不允许修改。

### 字段设计

字段设计需要区分“稳定业务值”和“展示属性”。业务系统应优先保存稳定的 `item_value`，而不是保存 `item_label`，因为展示名称可能调整，但业务值一旦被订单、客户、商品等核心业务表引用，就不应该随意变更。

字典类型表字段说明如下：

| 字段                          | 类型              | 设计说明                                        |
| ----------------------------- | ----------------- | ----------------------------------------------- |
| `id`                          | `BIGINT UNSIGNED` | 主键ID，建议使用雪花ID或号段ID                  |
| `tenant_id`                   | `BIGINT UNSIGNED` | 租户ID，单租户系统可固定为 `0`                  |
| `dict_code`                   | `VARCHAR(64)`     | 字典编码，全局或租户内唯一，例如 `order_status` |
| `dict_name`                   | `VARCHAR(128)`    | 字典名称，用于后台管理展示                      |
| `status`                      | `TINYINT`         | 字典类型状态，禁用后通常不再对外返回字典项      |
| `remark`                      | `VARCHAR(500)`    | 字典说明，记录业务含义和维护边界                |
| `deleted_at`                  | `BIGINT UNSIGNED` | 软删除时间戳，配合唯一索引支持删除后重建        |
| `version`                     | `INT UNSIGNED`    | 乐观锁版本号，防止后台并发编辑覆盖              |
| `create_by` / `update_by`     | `BIGINT UNSIGNED` | 创建人和更新人                                  |
| `create_time` / `update_time` | `DATETIME`        | 创建时间和更新时间                              |

字典项表字段说明如下：

| 字段                          | 类型              | 设计说明                                       |
| ----------------------------- | ----------------- | ---------------------------------------------- |
| `id`                          | `BIGINT UNSIGNED` | 主键ID                                         |
| `tenant_id`                   | `BIGINT UNSIGNED` | 租户ID，需要与字典类型表保持一致               |
| `dict_type_id`                | `BIGINT UNSIGNED` | 关联字典类型表主键                             |
| `dict_code`                   | `VARCHAR(64)`     | 冗余字典编码，用于高频按编码查询               |
| `item_value`                  | `VARCHAR(128)`    | 字典项值，建议作为业务表保存值                 |
| `item_label`                  | `VARCHAR(128)`    | 展示名称，可调整，不建议作为业务判断依据       |
| `item_alias`                  | `VARCHAR(128)`    | 别名，用于兼容导入、历史数据或第三方名称       |
| `sort_no`                     | `INT`             | 排序字段，值越小越靠前                         |
| `status`                      | `TINYINT`         | 字典项状态，禁用后不应作为新增业务数据选项     |
| `is_default`                  | `TINYINT`         | 默认项标识，一个字典类型下通常只允许一个默认项 |
| `color`                       | `VARCHAR(32)`     | 展示颜色或前端状态标签类型                     |
| `extra_json`                  | `JSON`            | 低频扩展属性，不建议放高频查询条件             |
| `remark`                      | `VARCHAR(500)`    | 字典项说明                                     |
| `deleted_at`                  | `BIGINT UNSIGNED` | 软删除时间戳，0表示未删除                      |
| `version`                     | `INT UNSIGNED`    | 乐观锁版本号                                   |
| `create_by` / `update_by`     | `BIGINT UNSIGNED` | 创建人和更新人                                 |
| `create_time` / `update_time` | `DATETIME`        | 创建时间和更新时间                             |

`item_value` 的设计要保持稳定。对于订单状态、支付状态这类强业务枚举，推荐使用英文编码，例如 `created`、`paid`、`cancelled`；对于简单的性别、开关等场景，也可以使用 `0`、`1`、`2`。一旦业务表已经引用，不建议直接修改 `item_value`，可以修改 `item_label` 或新增字典项替代旧值。

### 索引设计

索引设计需要优先覆盖三类查询：按字典编码查询启用字典项、按字典项值翻译展示名称、后台分页维护字典类型和字典项。由于模型使用 `deleted_at` 表示软删除，唯一索引中需要带上 `deleted_at`，避免软删除后无法重建同编码数据。

字典类型表推荐索引如下：

```sql
ALTER TABLE sys_dict_type
    ADD UNIQUE KEY uk_tenant_dict_code_deleted (
        tenant_id,
        dict_code,
        deleted_at
    );

ALTER TABLE sys_dict_type
    ADD KEY idx_tenant_status_sort (
        tenant_id,
        status,
        update_time
    );
```

`uk_tenant_dict_code_deleted` 用于保证同一租户下未删除的字典编码唯一。`idx_tenant_status_sort` 用于后台按租户、状态筛选并按更新时间排序的查询场景。

字典项表推荐索引如下：

```sql
ALTER TABLE sys_dict_item
    ADD UNIQUE KEY uk_tenant_dict_value_deleted (
        tenant_id,
        dict_code,
        item_value,
        deleted_at
    );

ALTER TABLE sys_dict_item
    ADD KEY idx_tenant_dict_status_sort (
        tenant_id,
        dict_code,
        status,
        sort_no,
        id
    );

ALTER TABLE sys_dict_item
    ADD KEY idx_dict_type_id (
        dict_type_id
    );

ALTER TABLE sys_dict_item
    ADD KEY idx_tenant_label_alias (
        tenant_id,
        dict_code,
        item_label,
        item_alias
    );
```

`uk_tenant_dict_value_deleted` 用于保证同一字典类型下启用或禁用状态的数据不会出现重复业务值。`idx_tenant_dict_status_sort` 是最常用的读取索引，适合根据 `tenant_id`、`dict_code`、`status` 查询字典项并按 `sort_no` 排序。`idx_dict_type_id` 适合后台按字典类型ID管理字典项。`idx_tenant_label_alias` 可用于导入时根据名称或别名匹配字典项，但如果导入量很大，建议在应用层提前加载字典缓存后再匹配，避免逐行查库。

### 常用查询

常用查询应围绕“前端加载选项、后端校验字典值、业务列表翻译展示、后台分页维护”展开。高频查询建议优先走 `tenant_id + dict_code + status + sort_no` 组合索引，并尽量避免在查询条件中对字段使用函数。

#### 查询启用字典项列表

该查询用于前端加载下拉框、单选框、状态标签等场景，只返回未删除且启用的数据。

```sql
SELECT
    item_value,
    item_label,
    color,
    is_default,
    extra_json
FROM sys_dict_item
WHERE tenant_id = 0
  AND dict_code = 'order_status'
  AND status = 1
  AND deleted_at = 0
ORDER BY sort_no ASC, id ASC;
```

业务接口通常可以返回 `value`、`label`、`color` 等字段，前端不需要感知字典表的内部主键。

#### 查询指定字典值的展示名称

该查询用于详情页、导出、消息模板渲染等场景，把业务表中的字典值翻译成展示名称。

```sql
SELECT
    item_label
FROM sys_dict_item
WHERE tenant_id = 0
  AND dict_code = 'order_status'
  AND item_value = 'paid'
  AND deleted_at = 0
LIMIT 1;
```

如果该查询出现在批量列表中，不建议对每一行循环查询。更好的做法是一次性加载整个字典类型到内存或缓存中，然后在应用层按 `item_value` 映射 `item_label`。

#### 查询默认字典项

该查询用于创建业务数据时自动选择默认值，例如新增用户默认状态、新增订单默认来源。

```sql
SELECT
    item_value,
    item_label
FROM sys_dict_item
WHERE tenant_id = 0
  AND dict_code = 'user_status'
  AND is_default = 1
  AND status = 1
  AND deleted_at = 0
ORDER BY sort_no ASC, id ASC
LIMIT 1;
```

默认项不建议完全依赖数据库返回多条后再由应用层随机选择。后台维护时应限制同一个字典类型下只有一个默认项，避免创建业务数据时出现不确定结果。

#### 查询业务列表并翻译字典名称

该查询用于业务列表展示，例如订单列表中展示订单状态名称。适合数据量不大或后台管理查询场景。

```sql
SELECT
    o.id,
    o.order_no,
    o.order_status,
    d.item_label AS order_status_name,
    d.color AS order_status_color,
    o.create_time
FROM biz_order o
LEFT JOIN sys_dict_item d
       ON d.tenant_id = o.tenant_id
      AND d.dict_code = 'order_status'
      AND d.item_value = o.order_status
      AND d.deleted_at = 0
WHERE o.tenant_id = 0
  AND o.deleted_at = 0
ORDER BY o.create_time DESC
LIMIT 20;
```

如果业务列表是高频接口，推荐业务服务启动时或请求前批量读取字典缓存，在应用层完成翻译，减少列表查询中的字典表关联。

#### 后台分页查询字典类型

该查询用于字典管理页面，支持按编码、名称和状态筛选。

```sql
SELECT
    id,
    dict_code,
    dict_name,
    status,
    remark,
    update_time
FROM sys_dict_type
WHERE tenant_id = 0
  AND deleted_at = 0
  AND (
      dict_code LIKE CONCAT('%', 'status', '%')
      OR dict_name LIKE CONCAT('%', '状态', '%')
  )
  AND status = 1
ORDER BY update_time DESC, id DESC
LIMIT 20 OFFSET 0;
```

后台模糊查询通常不是最高频场景。如果字典类型数量较少，可以接受普通索引覆盖不足的问题；如果数量很大，应考虑限制前缀查询、增加搜索辅助字段，或引入搜索辅助表模型。

#### 根据名称或别名匹配字典项

该查询常用于 Excel 导入，把用户填写的中文名称转换成系统内部字典值。

```sql
SELECT
    item_value,
    item_label
FROM sys_dict_item
WHERE tenant_id = 0
  AND dict_code = 'gender'
  AND status = 1
  AND deleted_at = 0
  AND (
      item_label = '男'
      OR item_alias = '男性'
  )
LIMIT 1;
```

导入场景更推荐先加载完整字典项，然后在应用层构建 `item_label`、`item_alias` 到 `item_value` 的映射，避免每一行数据都访问数据库。

### 常用写入

常用写入主要包括新增字典类型、新增字典项、修改展示信息、调整排序、禁用字典项和软删除字典数据。写入时需要重点保证字典编码和值的稳定性，避免影响已经引用字典值的历史业务数据。

新增字典类型时，应先确定 `dict_code` 的命名规范。推荐使用小写英文和下划线，例如 `order_status`、`payment_type`、`customer_level`。

```sql
INSERT INTO sys_dict_type (
    id,
    tenant_id,
    dict_code,
    dict_name,
    status,
    remark,
    create_by,
    update_by
) VALUES (
    1000000000000000001,
    0,
    'order_status',
    '订单状态',
    1,
    '订单主流程状态字典',
    1,
    1
);
```

新增字典项时，`item_value` 应优先选择稳定的业务编码。展示名称后续可以调整，但业务编码不建议修改。

```sql
INSERT INTO sys_dict_item (
    id,
    tenant_id,
    dict_type_id,
    dict_code,
    item_value,
    item_label,
    sort_no,
    status,
    is_default,
    color,
    remark,
    create_by,
    update_by
) VALUES
(
    1000000000000000101,
    0,
    1000000000000000001,
    'order_status',
    'created',
    '待支付',
    10,
    1,
    1,
    'warning',
    '订单已创建，等待用户支付',
    1,
    1
),
(
    1000000000000000102,
    0,
    1000000000000000001,
    'order_status',
    'paid',
    '已支付',
    20,
    1,
    0,
    'success',
    '订单已完成支付',
    1,
    1
),
(
    1000000000000000103,
    0,
    1000000000000000001,
    'order_status',
    'cancelled',
    '已取消',
    90,
    1,
    0,
    'info',
    '订单已取消',
    1,
    1
);
```

修改字典项展示名称时，只更新展示字段，不修改已经被业务表引用的 `item_value`。

```sql
UPDATE sys_dict_item
SET item_label = '支付完成',
    color = 'success',
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 0
  AND dict_code = 'order_status'
  AND item_value = 'paid'
  AND deleted_at = 0;
```

调整字典项排序时，只更新 `sort_no`。排序字段建议预留间隔，例如 10、20、30，后续插入新项时不需要大范围重排。

```sql
UPDATE sys_dict_item
SET sort_no = 15,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 0
  AND dict_code = 'order_status'
  AND item_value = 'paid'
  AND deleted_at = 0;
```

禁用字典项适用于不允许新增业务数据继续使用该选项，但历史数据仍需要正常展示的场景。

```sql
UPDATE sys_dict_item
SET status = 0,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 0
  AND dict_code = 'order_status'
  AND item_value = 'cancelled'
  AND deleted_at = 0;
```

软删除字典项适用于维护错误、测试数据清理等场景。对已经被业务表引用的字典项，不建议直接删除，优先使用禁用。

```sql
UPDATE sys_dict_item
SET deleted_at = UNIX_TIMESTAMP(),
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 0
  AND dict_code = 'order_status'
  AND item_value = 'cancelled'
  AND deleted_at = 0;
```

设置默认项时，应在事务中先取消同字典类型下的其他默认项，再设置新的默认项。

```sql
START TRANSACTION;

UPDATE sys_dict_item
SET is_default = 0,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 0
  AND dict_code = 'order_status'
  AND deleted_at = 0;

UPDATE sys_dict_item
SET is_default = 1,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 0
  AND dict_code = 'order_status'
  AND item_value = 'created'
  AND deleted_at = 0;

COMMIT;
```

### 常见问题

字典值应该保存 `item_value` 还是 `item_label`？业务表应保存 `item_value`。`item_label` 是展示名称，可能会随着产品文案调整而变化；`item_value` 是稳定业务值，适合参与查询、统计、流程判断和接口传输。

字典项被业务数据引用后还能不能修改？可以修改 `item_label`、`sort_no`、`color`、`remark` 等展示或维护字段，但不建议修改 `item_value`。如果确实需要修改，应先评估所有引用表，并通过数据变更脚本统一迁移。

字典项不用了应该删除还是禁用？优先禁用。禁用后不再作为新增业务数据的可选项，但历史数据仍可以通过字典表翻译展示。只有维护错误、初始化错误或测试数据才建议软删除。

为什么使用 `deleted_at` 而不是只使用 `is_deleted`？MySQL 不支持通用的部分唯一索引。如果唯一索引只包含 `is_deleted`，同一条数据多次删除或删除后重建时容易发生唯一冲突。使用 `deleted_at = 0` 表示未删除，删除时写入时间戳，可以更好地配合唯一索引。

字典表是否可以完全替代代码枚举？不能。强业务流程、核心状态流转、权限判断等仍应在代码中有明确约束。字典表主要解决展示、维护、校验和选项管理问题，不应该让后台随意新增状态后直接影响核心业务流程。

是否需要缓存字典数据？需要。字典数据通常读多写少，适合缓存。常见做法是按 `tenant_id + dict_code` 缓存字典项列表，后台修改字典后主动清理缓存或发布字典变更事件。

多租户系统如何设计字典？如果所有租户共用字典，`tenant_id` 可以固定为 `0`。如果租户允许自定义字典，应使用 `tenant_id` 隔离，并明确平台公共字典和租户私有字典的优先级，例如先查租户字典，查不到再回退平台字典。

`extra_json` 能不能随便放扩展字段？不建议。`extra_json` 适合存放低频展示属性，例如图标、国际化键、前端样式。如果字段需要频繁查询、排序或统计，应拆成独立列。

字典项排序为什么要使用 `sort_no`？字典项通常需要在前端按业务顺序展示，而不是按创建时间或字典值排序。`sort_no` 可以明确控制展示顺序，并且建议使用 10、20、30 这类间隔值，方便后续插入。

### 总结

字典表模型适合管理稳定、有限、读多写少的业务选项数据。建模时应拆分字典类型和字典项，业务表保存稳定的 `item_value`，展示时通过字典项转换为 `item_label`。索引设计应围绕按租户、字典编码、状态和排序字段的高频读取场景展开。

实际落地时，需要重点控制三件事：第一，字典编码和字典项值创建后尽量不修改；第二，历史业务数据引用过的字典项优先禁用而不是删除；第三，高频读取场景应使用缓存，避免每次列表查询都关联字典表。这样可以在保持模型通用性的同时，兼顾业务稳定性、查询性能和后台维护效率。



## 配置项模型

配置项模型用于管理系统运行参数、业务开关、阈值参数、第三方接口参数、页面展示参数等数据。它与字典表模型同属于“通用字典与配置模型”章节，但两者职责不同：字典表偏向“选项数据”，配置项偏向“参数控制”。

### 适用场景

配置项模型适合存储“需要后台维护、读取频率较高、写入频率较低、会影响业务行为”的参数数据。它通常用于减少硬编码，让部分业务规则可以通过后台配置调整，而不是每次都修改代码发布。

典型适用场景如下：

| 场景         | 示例                                 | 说明                     |
| ------------ | ------------------------------------ | ------------------------ |
| 业务开关     | 是否开启新人礼包、是否启用短信验证   | 控制某个业务能力是否生效 |
| 阈值参数     | 最大登录失败次数、订单自动关闭分钟数 | 控制业务规则中的数值边界 |
| 三方参数     | 回调地址、渠道编码、接口超时时间     | 存储外部系统对接参数     |
| 展示配置     | 首页推荐数量、默认头像、活动文案     | 控制页面展示或默认内容   |
| 灰度参数     | 是否开启新流程、指定租户启用新规则   | 用于小范围启用能力       |
| 系统运行参数 | 文件上传大小、验证码有效期           | 控制系统运行行为         |

不建议把高频变化的交易数据、复杂规则引擎、审批流程、状态流转逻辑直接塞进配置项表。配置项适合控制参数，不适合承载完整业务模型。

### 建模结构

配置项模型通常采用“配置项表 + 配置变更记录表”的结构。配置项表保存当前生效的配置值，配置变更记录表保存每次修改前后的值，便于审计、排查问题和回滚。

配置项表用于保存当前配置值。该表只给出建模字段，不包含唯一索引和普通索引，索引统一放在后文索引设计部分。

```sql
CREATE TABLE sys_config_item (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，0表示平台级配置',
    app_code VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '应用编码，例如 admin、mall、job',
    env_code VARCHAR(32) NOT NULL DEFAULT 'prod' COMMENT '环境编码，例如 dev、test、prod',
    group_code VARCHAR(64) NOT NULL COMMENT '配置分组编码，例如 order、sms、security',
    config_key VARCHAR(128) NOT NULL COMMENT '配置键，例如 order.auto_close_minutes',
    config_name VARCHAR(128) NOT NULL COMMENT '配置名称，例如 订单自动关闭分钟数',
    config_value TEXT NOT NULL COMMENT '配置值，统一以字符串形式存储',
    default_value TEXT DEFAULT NULL COMMENT '默认值，用于缺省回退',
    value_type VARCHAR(32) NOT NULL DEFAULT 'string' COMMENT '值类型：string、int、decimal、boolean、json、date、datetime',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    is_sensitive TINYINT NOT NULL DEFAULT 0 COMMENT '是否敏感配置：0否，1是',
    is_encrypted TINYINT NOT NULL DEFAULT 0 COMMENT '是否加密存储：0否，1是',
    reload_type VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '刷新方式：manual手动刷新，auto自动刷新，restart重启生效',
    description VARCHAR(500) DEFAULT NULL COMMENT '配置说明',
    deleted_at BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳，0表示未删除',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '配置项表';
```

配置变更记录表用于保存配置修改历史。配置项属于影响业务行为的数据，建议保留修改前后的值，方便定位“某个时间点之后业务行为发生变化”的问题。

```sql
CREATE TABLE sys_config_change_log (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，0表示平台级配置',
    config_id BIGINT UNSIGNED NOT NULL COMMENT '配置项ID',
    app_code VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '应用编码',
    env_code VARCHAR(32) NOT NULL DEFAULT 'prod' COMMENT '环境编码',
    group_code VARCHAR(64) NOT NULL COMMENT '配置分组编码',
    config_key VARCHAR(128) NOT NULL COMMENT '配置键',
    old_value TEXT DEFAULT NULL COMMENT '修改前配置值',
    new_value TEXT DEFAULT NULL COMMENT '修改后配置值',
    change_type VARCHAR(32) NOT NULL COMMENT '变更类型：create、update、disable、enable、delete',
    change_reason VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    operator_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(128) DEFAULT NULL COMMENT '操作人名称',
    operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '配置变更记录表';
```

配置项模型的读取路径通常是：应用根据 `tenant_id + app_code + env_code + group_code + config_key` 查询配置项，若租户级配置不存在，则回退平台级配置；若当前配置不存在，则回退代码默认值或 `default_value`。如果配置项需要审批、草稿、发布、回滚等复杂流程，应结合后续“草稿-发布模型”处理，而不是把所有状态都堆到配置项表中。

### 字段设计

配置项字段设计的核心是保证配置键稳定、配置值可解析、配置作用域清晰。配置值可以统一使用 `TEXT` 保存，再通过 `value_type` 决定应用层如何转换，这样可以兼容字符串、数字、布尔值、JSON 等不同类型。

配置项表字段说明如下：

| 字段                          | 类型              | 设计说明                            |
| ----------------------------- | ----------------- | ----------------------------------- |
| `id`                          | `BIGINT UNSIGNED` | 主键ID，建议使用雪花ID或号段ID      |
| `tenant_id`                   | `BIGINT UNSIGNED` | 租户ID，`0` 表示平台级配置          |
| `app_code`                    | `VARCHAR(64)`     | 应用编码，用于区分不同服务或业务端  |
| `env_code`                    | `VARCHAR(32)`     | 环境编码，避免测试配置影响生产      |
| `group_code`                  | `VARCHAR(64)`     | 配置分组，便于后台维护和批量加载    |
| `config_key`                  | `VARCHAR(128)`    | 配置键，建议使用小写英文和点分隔    |
| `config_name`                 | `VARCHAR(128)`    | 配置名称，用于后台展示              |
| `config_value`                | `TEXT`            | 当前配置值，统一字符串存储          |
| `default_value`               | `TEXT`            | 默认值，当前值异常或为空时可回退    |
| `value_type`                  | `VARCHAR(32)`     | 值类型，用于应用层解析和后台校验    |
| `status`                      | `TINYINT`         | 配置状态，禁用后不参与业务读取      |
| `is_sensitive`                | `TINYINT`         | 是否敏感配置，例如密钥、Token、密码 |
| `is_encrypted`                | `TINYINT`         | 是否加密存储，敏感配置建议开启      |
| `reload_type`                 | `VARCHAR(32)`     | 配置刷新方式，决定修改后如何生效    |
| `description`                 | `VARCHAR(500)`    | 配置说明，描述用途、范围和风险      |
| `deleted_at`                  | `BIGINT UNSIGNED` | 软删除时间戳，0表示未删除           |
| `version`                     | `INT UNSIGNED`    | 乐观锁版本号，防止并发覆盖          |
| `create_by` / `update_by`     | `BIGINT UNSIGNED` | 创建人和更新人                      |
| `create_time` / `update_time` | `DATETIME`        | 创建时间和更新时间                  |

配置变更记录表字段说明如下：

| 字段                            | 类型                 | 设计说明                     |
| ------------------------------- | -------------------- | ---------------------------- |
| `id`                            | `BIGINT UNSIGNED`    | 主键ID                       |
| `tenant_id`                     | `BIGINT UNSIGNED`    | 租户ID                       |
| `config_id`                     | `BIGINT UNSIGNED`    | 配置项ID                     |
| `app_code` / `env_code`         | `VARCHAR`            | 冗余应用和环境，便于历史查询 |
| `group_code`                    | `VARCHAR(64)`        | 冗余配置分组                 |
| `config_key`                    | `VARCHAR(128)`       | 冗余配置键                   |
| `old_value`                     | `TEXT`               | 修改前值                     |
| `new_value`                     | `TEXT`               | 修改后值                     |
| `change_type`                   | `VARCHAR(32)`        | 变更类型                     |
| `change_reason`                 | `VARCHAR(500)`       | 变更原因                     |
| `operator_id` / `operator_name` | `BIGINT` / `VARCHAR` | 操作人信息                   |
| `operate_time`                  | `DATETIME`           | 操作时间                     |

`config_key` 应保持稳定，不建议直接使用中文、页面文案或容易变化的业务名称。推荐格式如下：

| 配置键                           | 说明                 |
| -------------------------------- | -------------------- |
| `order.auto_close_minutes`       | 订单自动关闭分钟数   |
| `security.login_fail_max_count`  | 登录失败最大次数     |
| `sms.verify_code_expire_seconds` | 短信验证码有效秒数   |
| `upload.image_max_size_mb`       | 图片最大上传大小     |
| `feature.new_checkout_enabled`   | 是否启用新版结算流程 |

`value_type` 应在后台保存前完成校验。例如 `int` 必须能转换为整数，`boolean` 只能使用 `true` 或 `false`，`json` 必须是合法 JSON。不要把无法校验的任意文本配置直接用于核心业务判断。

### 索引设计

索引设计需要覆盖配置读取、后台维护、变更记录查询三类场景。配置读取通常按作用域和配置键精确查询，后台维护通常按分组、状态、更新时间分页，变更记录通常按配置项或配置键查询历史。

配置项表推荐索引如下：

```sql
ALTER TABLE sys_config_item
    ADD UNIQUE KEY uk_scope_config_key_deleted (
        tenant_id,
        app_code,
        env_code,
        config_key,
        deleted_at
    );

ALTER TABLE sys_config_item
    ADD KEY idx_scope_group_status (
        tenant_id,
        app_code,
        env_code,
        group_code,
        status,
        update_time
    );

ALTER TABLE sys_config_item
    ADD KEY idx_scope_status_update_time (
        tenant_id,
        app_code,
        env_code,
        status,
        update_time
    );
```

`uk_scope_config_key_deleted` 用于保证同一作用域下未删除的配置键唯一。`idx_scope_group_status` 适合按分组批量加载配置项，也适合后台按分组维护。`idx_scope_status_update_time` 适合应用启动时按作用域加载全部启用配置。

配置变更记录表推荐索引如下：

```sql
ALTER TABLE sys_config_change_log
    ADD KEY idx_config_id_operate_time (
        config_id,
        operate_time
    );

ALTER TABLE sys_config_change_log
    ADD KEY idx_scope_config_key_operate_time (
        tenant_id,
        app_code,
        env_code,
        config_key,
        operate_time
    );

ALTER TABLE sys_config_change_log
    ADD KEY idx_operator_operate_time (
        operator_id,
        operate_time
    );
```

`idx_config_id_operate_time` 用于查看某个配置项的变更历史。`idx_scope_config_key_operate_time` 用于配置项被软删除或重建后仍能按配置键追踪历史。`idx_operator_operate_time` 用于审计某个操作人在某段时间内的配置修改行为。

### 常用查询

常用查询主要围绕配置读取、分组加载、默认值回退、后台维护和变更追踪展开。高频读取场景建议使用缓存，不建议每次业务判断都直接访问数据库。

#### 查询单个配置项

该查询用于业务代码读取指定配置，例如订单自动关闭时间、短信验证码有效期等。

```sql
SELECT
    config_key,
    config_value,
    default_value,
    value_type,
    reload_type
FROM sys_config_item
WHERE tenant_id = 0
  AND app_code = 'mall'
  AND env_code = 'prod'
  AND config_key = 'order.auto_close_minutes'
  AND status = 1
  AND deleted_at = 0
LIMIT 1;
```

读取后，应用层需要根据 `value_type` 把 `config_value` 转换为目标类型。如果转换失败，可以回退 `default_value` 或代码内置默认值。

#### 查询租户级配置并回退平台配置

该查询用于多租户系统。优先读取租户级配置，租户级不存在时使用平台级配置。

```sql
SELECT
    tenant_id,
    config_key,
    config_value,
    default_value,
    value_type
FROM sys_config_item
WHERE tenant_id IN (10001, 0)
  AND app_code = 'mall'
  AND env_code = 'prod'
  AND config_key = 'feature.new_checkout_enabled'
  AND status = 1
  AND deleted_at = 0
ORDER BY tenant_id DESC
LIMIT 1;
```

该写法要求平台级配置使用 `tenant_id = 0`，租户级配置使用真实租户ID。排序时真实租户ID大于 `0`，因此可以优先命中租户级配置。

#### 按分组加载配置项

该查询用于应用启动、配置缓存预热或后台分组展示。

```sql
SELECT
    config_key,
    config_name,
    config_value,
    default_value,
    value_type,
    reload_type,
    description
FROM sys_config_item
WHERE tenant_id = 0
  AND app_code = 'mall'
  AND env_code = 'prod'
  AND group_code = 'order'
  AND status = 1
  AND deleted_at = 0
ORDER BY config_key ASC;
```

按分组加载后，应用层可以构建 `config_key -> config_value` 的缓存映射，避免后续业务流程重复查询数据库。

#### 查询所有启用配置

该查询用于应用启动时加载当前应用、当前环境下所有启用配置。

```sql
SELECT
    group_code,
    config_key,
    config_value,
    default_value,
    value_type,
    is_sensitive,
    is_encrypted,
    reload_type
FROM sys_config_item
WHERE tenant_id = 0
  AND app_code = 'mall'
  AND env_code = 'prod'
  AND status = 1
  AND deleted_at = 0
ORDER BY group_code ASC, config_key ASC;
```

如果存在敏感配置，返回到后台页面时应脱敏展示；如果应用服务内部读取敏感配置，则应在服务端完成解密，不应把明文返回给前端。

#### 后台分页查询配置项

该查询用于配置管理页面，支持按分组、状态、配置键或配置名称检索。

```sql
SELECT
    id,
    group_code,
    config_key,
    config_name,
    config_value,
    value_type,
    status,
    reload_type,
    update_time
FROM sys_config_item
WHERE tenant_id = 0
  AND app_code = 'mall'
  AND env_code = 'prod'
  AND deleted_at = 0
  AND group_code = 'order'
  AND status = 1
  AND (
      config_key LIKE CONCAT('%', 'auto_close', '%')
      OR config_name LIKE CONCAT('%', '自动关闭', '%')
  )
ORDER BY update_time DESC, id DESC
LIMIT 20 OFFSET 0;
```

后台模糊查询通常不是配置项模型的高频路径。如果配置项数量较大，可以限制为前缀查询，或者额外设计搜索辅助字段。

#### 查询配置变更历史

该查询用于查看某个配置项的修改记录，排查配置变化导致的业务问题。

```sql
SELECT
    config_key,
    old_value,
    new_value,
    change_type,
    change_reason,
    operator_name,
    operate_time
FROM sys_config_change_log
WHERE config_id = 1000000000000000001
ORDER BY operate_time DESC, id DESC
LIMIT 20;
```

如果配置项被软删除后重建，`config_id` 会变化，此时可以按 `tenant_id + app_code + env_code + config_key` 查询历史记录。

```sql
SELECT
    config_id,
    old_value,
    new_value,
    change_type,
    change_reason,
    operator_name,
    operate_time
FROM sys_config_change_log
WHERE tenant_id = 0
  AND app_code = 'mall'
  AND env_code = 'prod'
  AND config_key = 'order.auto_close_minutes'
ORDER BY operate_time DESC, id DESC
LIMIT 20;
```

### 常用写入

常用写入主要包括新增配置项、修改配置值、启用或禁用配置、软删除配置、写入配置变更记录。配置项写入应尽量放在事务中完成，保证配置表和变更记录表一致。

新增配置项时，应明确应用、环境、分组、配置键和值类型。

```sql
START TRANSACTION;

INSERT INTO sys_config_item (
    id,
    tenant_id,
    app_code,
    env_code,
    group_code,
    config_key,
    config_name,
    config_value,
    default_value,
    value_type,
    status,
    is_sensitive,
    is_encrypted,
    reload_type,
    description,
    create_by,
    update_by
) VALUES (
    1000000000000000001,
    0,
    'mall',
    'prod',
    'order',
    'order.auto_close_minutes',
    '订单自动关闭分钟数',
    '30',
    '30',
    'int',
    1,
    0,
    0,
    'auto',
    '未支付订单超过该分钟数后自动关闭',
    1,
    1
);

INSERT INTO sys_config_change_log (
    id,
    tenant_id,
    config_id,
    app_code,
    env_code,
    group_code,
    config_key,
    old_value,
    new_value,
    change_type,
    change_reason,
    operator_id,
    operator_name
) VALUES (
    1000000000000000101,
    0,
    1000000000000000001,
    'mall',
    'prod',
    'order',
    'order.auto_close_minutes',
    NULL,
    '30',
    'create',
    '初始化订单自动关闭配置',
    1,
    'admin'
);

COMMIT;
```

修改配置值时，应使用乐观锁版本号，防止后台多人同时编辑导致覆盖。

```sql
START TRANSACTION;

UPDATE sys_config_item
SET config_value = '45',
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000001
  AND version = 0
  AND deleted_at = 0;

INSERT INTO sys_config_change_log (
    id,
    tenant_id,
    config_id,
    app_code,
    env_code,
    group_code,
    config_key,
    old_value,
    new_value,
    change_type,
    change_reason,
    operator_id,
    operator_name
) VALUES (
    1000000000000000102,
    0,
    1000000000000000001,
    'mall',
    'prod',
    'order',
    'order.auto_close_minutes',
    '30',
    '45',
    'update',
    '运营要求延长未支付订单保留时间',
    1,
    'admin'
);

COMMIT;
```

启用或禁用配置项时，需要记录变更原因。禁用后，业务读取逻辑应回退默认值或代码内置值。

```sql
START TRANSACTION;

UPDATE sys_config_item
SET status = 0,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 0
  AND app_code = 'mall'
  AND env_code = 'prod'
  AND config_key = 'feature.new_checkout_enabled'
  AND deleted_at = 0;

INSERT INTO sys_config_change_log (
    id,
    tenant_id,
    config_id,
    app_code,
    env_code,
    group_code,
    config_key,
    old_value,
    new_value,
    change_type,
    change_reason,
    operator_id,
    operator_name
)
SELECT
    1000000000000000103,
    tenant_id,
    id,
    app_code,
    env_code,
    group_code,
    config_key,
    config_value,
    config_value,
    'disable',
    '临时关闭新版结算流程',
    1,
    'admin'
FROM sys_config_item
WHERE tenant_id = 0
  AND app_code = 'mall'
  AND env_code = 'prod'
  AND config_key = 'feature.new_checkout_enabled'
  AND deleted_at = 0;

COMMIT;
```

软删除配置项适合废弃配置清理。对于仍可能被历史代码读取的配置，不建议立即删除，应先禁用并观察一段时间。

```sql
START TRANSACTION;

UPDATE sys_config_item
SET deleted_at = UNIX_TIMESTAMP(),
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 0
  AND app_code = 'mall'
  AND env_code = 'prod'
  AND config_key = 'old.feature.enabled'
  AND deleted_at = 0;

INSERT INTO sys_config_change_log (
    id,
    tenant_id,
    config_id,
    app_code,
    env_code,
    group_code,
    config_key,
    old_value,
    new_value,
    change_type,
    change_reason,
    operator_id,
    operator_name
)
SELECT
    1000000000000000104,
    tenant_id,
    id,
    app_code,
    env_code,
    group_code,
    config_key,
    config_value,
    NULL,
    'delete',
    '旧功能已下线，清理废弃配置',
    1,
    'admin'
FROM sys_config_item
WHERE tenant_id = 0
  AND app_code = 'mall'
  AND env_code = 'prod'
  AND config_key = 'old.feature.enabled'
  AND deleted_at > 0;

COMMIT;
```

批量初始化配置项时，应提前规划配置键命名和分组，避免后续出现同一类配置散落在多个分组中的问题。

```sql
INSERT INTO sys_config_item (
    id,
    tenant_id,
    app_code,
    env_code,
    group_code,
    config_key,
    config_name,
    config_value,
    default_value,
    value_type,
    status,
    reload_type,
    description,
    create_by,
    update_by
) VALUES
(
    1000000000000000201,
    0,
    'mall',
    'prod',
    'security',
    'security.login_fail_max_count',
    '登录失败最大次数',
    '5',
    '5',
    'int',
    1,
    'auto',
    '用户连续登录失败达到该次数后锁定账号',
    1,
    1
),
(
    1000000000000000202,
    0,
    'mall',
    'prod',
    'sms',
    'sms.verify_code_expire_seconds',
    '短信验证码有效秒数',
    '300',
    '300',
    'int',
    1,
    'auto',
    '短信验证码从发送成功开始计算的有效时间',
    1,
    1
),
(
    1000000000000000203,
    0,
    'mall',
    'prod',
    'upload',
    'upload.image_max_size_mb',
    '图片最大上传大小',
    '10',
    '10',
    'int',
    1,
    'restart',
    '图片上传大小限制，部分网关配置可能需要同步调整',
    1,
    1
);
```

### 常见问题

配置项应该存字符串还是按类型拆字段？推荐统一使用 `TEXT` 存储配置值，再通过 `value_type` 做类型转换和校验。这样模型更通用，也便于保存 JSON、日期、数字、布尔等不同类型配置。

配置项能不能直接存 JSON？可以，但要控制边界。JSON 适合保存结构化但低频查询的配置，例如页面展示规则、第三方渠道参数。如果 JSON 内的字段需要频繁作为查询条件或排序条件，应拆成独立字段或独立业务表。

敏感配置如何处理？敏感配置应设置 `is_sensitive = 1`，必要时设置 `is_encrypted = 1`。后台页面展示时应脱敏，服务端读取时再解密。不要把数据库密码、三方密钥、Token 明文返回给前端。

配置修改后如何生效？可以通过 `reload_type` 区分生效方式。`auto` 表示修改后清理缓存或发布配置变更事件即可生效，`manual` 表示需要人工触发刷新，`restart` 表示必须重启应用或重新加载上下文后生效。

配置项是否需要缓存？需要。配置项通常读多写少，高频业务判断不应每次查库。常见做法是按 `tenant_id + app_code + env_code` 或 `group_code` 缓存配置集合，配置修改后清理相关缓存。

配置不存在时应该怎么处理？应定义明确的回退策略。常见顺序是：租户级配置、平台级配置、配置表默认值、代码内置默认值。如果是关键配置缺失，应记录错误日志并拒绝启动或拒绝执行相关业务。

配置项和字典项有什么区别？字典项用于表示有限选项，例如订单状态、支付方式；配置项用于控制参数，例如订单自动关闭时间、是否启用某个功能。字典项通常给用户选择，配置项通常给系统读取。

配置项能否控制核心业务流程？可以控制简单开关和参数，但不建议把复杂流程逻辑放进配置项。复杂审批、状态流转、规则计算应使用状态机模型、规则表或独立业务模型。

为什么需要配置变更记录？配置会直接影响系统行为。如果没有变更记录，线上问题排查时很难确认某个业务异常是否由配置调整导致。变更记录还可以用于审计、回滚和责任定位。

配置键可以修改吗？不建议修改。`config_key` 应被视为稳定契约，应用代码通常会直接按配置键读取。修改配置键可能导致旧代码读取不到配置，从而触发默认值或业务异常。

### 总结

配置项模型适合管理读多写少、影响业务行为、需要后台维护的系统参数。建模时应明确配置作用域，包括租户、应用、环境、分组和配置键；配置值可以统一字符串存储，再通过 `value_type` 完成解析和校验。

实际落地时，需要重点控制四件事：第一，配置键创建后保持稳定；第二，敏感配置必须脱敏和加密；第三，高频读取必须走缓存；第四，配置修改必须记录变更历史。这样可以在提升系统灵活性的同时，降低配置误改、读取异常和线上排查困难的风险。



## 状态机模型

状态机模型用于管理业务对象在不同状态之间的流转规则，例如订单状态、审核状态、工单状态、退款状态、任务状态等。它与字典表模型不同：字典表只描述“有哪些状态”，状态机模型还描述“状态之间能不能流转、通过什么事件流转、谁能触发、是否需要原因、是否需要记录轨迹”。该章节属于《MySQL 8 常用业务建模模型》中的“通用字典与配置模型”部分。

### 适用场景

状态机模型适合管理存在明确生命周期、状态流转有约束、需要记录流转轨迹的业务数据。它的重点不是展示状态名称，而是控制状态变化是否合法。

典型适用场景如下：

| 场景     | 示例                                     | 说明                         |
| -------- | ---------------------------------------- | ---------------------------- |
| 订单流程 | 待支付、已支付、已发货、已完成、已取消   | 状态之间有固定流转顺序       |
| 审核流程 | 待提交、待审核、审核通过、审核驳回       | 不同动作触发不同状态变化     |
| 工单流程 | 待处理、处理中、已解决、已关闭           | 需要记录处理人和处理轨迹     |
| 退款流程 | 退款申请、退款审核中、退款成功、退款失败 | 状态变化会触发资金或消息动作 |
| 任务流程 | 待执行、执行中、执行成功、执行失败       | 状态变化可能由系统自动触发   |
| 发布流程 | 草稿、待发布、已发布、已下线             | 和草稿发布模型组合使用       |

如果业务只是简单展示状态名称，可以使用字典表模型。如果业务需要校验“当前状态是否允许变更到目标状态”，并且需要记录状态变更日志，就应该使用状态机模型。

### 建模结构

状态机模型通常由“状态机定义表、状态节点表、状态流转规则表、状态变更记录表”组成。业务表自身保存当前状态，状态机表负责定义合法状态和合法流转，状态变更记录表负责保存每次状态变化的轨迹。

状态机定义表用于定义一套状态机，例如订单状态机、退款状态机、审核状态机。

```sql
CREATE TABLE sys_state_machine (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，0表示平台级状态机',
    machine_code VARCHAR(64) NOT NULL COMMENT '状态机编码，例如 order_status、refund_status',
    machine_name VARCHAR(128) NOT NULL COMMENT '状态机名称，例如 订单状态机',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型，例如 order、refund、audit',
    initial_state_code VARCHAR(64) NOT NULL COMMENT '初始状态编码',
    terminal_state_codes JSON DEFAULT NULL COMMENT '终态编码集合，例如 ["completed","cancelled"]',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    deleted_at BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳，0表示未删除',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '状态机定义表';
```

状态节点表用于定义状态机中的所有状态节点，例如 `created`、`paid`、`shipped`、`completed`、`cancelled`。

```sql
CREATE TABLE sys_state_node (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，0表示平台级状态节点',
    machine_id BIGINT UNSIGNED NOT NULL COMMENT '状态机ID',
    machine_code VARCHAR(64) NOT NULL COMMENT '状态机编码，冗余字段',
    state_code VARCHAR(64) NOT NULL COMMENT '状态编码，例如 created、paid、completed',
    state_name VARCHAR(128) NOT NULL COMMENT '状态名称，例如 待支付、已支付、已完成',
    state_type VARCHAR(32) NOT NULL DEFAULT 'normal' COMMENT '状态类型：initial初始，normal普通，terminal终态',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号，值越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    color VARCHAR(32) DEFAULT NULL COMMENT '展示颜色，例如 success、warning、danger、#67C23A',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    deleted_at BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳，0表示未删除',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '状态节点表';
```

状态流转规则表用于定义状态之间的合法流转关系。业务代码在变更状态前，应先根据当前状态、事件编码和目标状态校验流转规则是否存在且启用。

```sql
CREATE TABLE sys_state_transition (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，0表示平台级流转规则',
    machine_id BIGINT UNSIGNED NOT NULL COMMENT '状态机ID',
    machine_code VARCHAR(64) NOT NULL COMMENT '状态机编码，冗余字段',
    from_state_code VARCHAR(64) NOT NULL COMMENT '来源状态编码',
    to_state_code VARCHAR(64) NOT NULL COMMENT '目标状态编码',
    event_code VARCHAR(64) NOT NULL COMMENT '事件编码，例如 pay、ship、cancel',
    event_name VARCHAR(128) NOT NULL COMMENT '事件名称，例如 支付、发货、取消',
    action_type VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '动作类型：manual人工，auto自动，system系统',
    require_reason TINYINT NOT NULL DEFAULT 0 COMMENT '是否要求填写变更原因：0否，1是',
    guard_expression VARCHAR(500) DEFAULT NULL COMMENT '守卫表达式或条件标识，由应用层解释',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    deleted_at BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳，0表示未删除',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '状态流转规则表';
```

状态变更记录表用于记录业务对象的每一次状态变化。它不只记录当前状态，还应记录事件、操作人、原因、来源、请求号等信息，便于审计和问题追踪。

```sql
CREATE TABLE sys_state_change_log (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    machine_code VARCHAR(64) NOT NULL COMMENT '状态机编码',
    biz_id BIGINT UNSIGNED NOT NULL COMMENT '业务数据ID',
    biz_no VARCHAR(128) DEFAULT NULL COMMENT '业务单号，例如订单号、工单号',
    from_state_code VARCHAR(64) DEFAULT NULL COMMENT '来源状态编码，初始创建时可为空',
    to_state_code VARCHAR(64) NOT NULL COMMENT '目标状态编码',
    event_code VARCHAR(64) NOT NULL COMMENT '事件编码',
    event_name VARCHAR(128) NOT NULL COMMENT '事件名称',
    action_type VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '动作类型：manual人工，auto自动，system系统',
    change_reason VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    operator_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(128) DEFAULT NULL COMMENT '操作人名称',
    request_id VARCHAR(128) DEFAULT NULL COMMENT '请求ID，用于幂等和链路追踪',
    source_type VARCHAR(64) DEFAULT NULL COMMENT '来源类型，例如 admin、api、job、mq',
    extra_json JSON DEFAULT NULL COMMENT '扩展信息，例如审批意见、失败原因、外部回执',
    operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '状态变更记录表';
```

业务表需要保存当前状态。以下以订单表中的状态字段为例，实际项目中可以根据业务表结构补充。

```sql
CREATE TABLE biz_order (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    order_status VARCHAR(64) NOT NULL COMMENT '订单状态，对应状态机中的 state_code',
    paid_time DATETIME DEFAULT NULL COMMENT '支付时间',
    completed_time DATETIME DEFAULT NULL COMMENT '完成时间',
    cancelled_time DATETIME DEFAULT NULL COMMENT '取消时间',
    deleted_at BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳，0表示未删除',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '订单表示例';
```

状态机模型的核心关系是：状态机定义一组状态节点，状态流转规则定义节点之间允许通过哪些事件流转，业务表保存当前状态，状态变更记录表保存每次变更轨迹。业务状态变更时，应在同一个事务中完成“校验流转规则、更新业务表当前状态、写入状态变更记录”。

### 字段设计

字段设计需要重点区分“状态、事件、动作、记录”。状态表示业务对象当前所处位置，事件表示触发流转的原因，动作类型表示由谁或由什么机制触发，变更记录表示已经发生的历史事实。

状态机定义表字段说明如下：

| 字段                          | 类型              | 设计说明                               |
| ----------------------------- | ----------------- | -------------------------------------- |
| `id`                          | `BIGINT UNSIGNED` | 主键ID                                 |
| `tenant_id`                   | `BIGINT UNSIGNED` | 租户ID，`0` 表示平台级状态机           |
| `machine_code`                | `VARCHAR(64)`     | 状态机编码，例如 `order_status`        |
| `machine_name`                | `VARCHAR(128)`    | 状态机名称，用于后台展示               |
| `biz_type`                    | `VARCHAR(64)`     | 业务类型，用于归类状态机               |
| `initial_state_code`          | `VARCHAR(64)`     | 初始状态编码，创建业务数据时使用       |
| `terminal_state_codes`        | `JSON`            | 终态集合，进入终态后通常不允许继续流转 |
| `status`                      | `TINYINT`         | 状态机是否启用                         |
| `remark`                      | `VARCHAR(500)`    | 状态机说明                             |
| `deleted_at`                  | `BIGINT UNSIGNED` | 软删除时间戳                           |
| `version`                     | `INT UNSIGNED`    | 乐观锁版本号                           |
| `create_by` / `update_by`     | `BIGINT UNSIGNED` | 创建人和更新人                         |
| `create_time` / `update_time` | `DATETIME`        | 创建时间和更新时间                     |

状态节点表字段说明如下：

| 字段           | 类型              | 设计说明                               |
| -------------- | ----------------- | -------------------------------------- |
| `machine_id`   | `BIGINT UNSIGNED` | 状态机ID                               |
| `machine_code` | `VARCHAR(64)`     | 冗余状态机编码，便于查询和日志展示     |
| `state_code`   | `VARCHAR(64)`     | 状态编码，业务表保存该值               |
| `state_name`   | `VARCHAR(128)`    | 状态名称，用于页面展示                 |
| `state_type`   | `VARCHAR(32)`     | 状态类型，区分初始状态、普通状态、终态 |
| `sort_no`      | `INT`             | 展示排序                               |
| `status`       | `TINYINT`         | 状态节点是否启用                       |
| `color`        | `VARCHAR(32)`     | 前端标签颜色                           |
| `remark`       | `VARCHAR(500)`    | 状态说明                               |

状态流转规则表字段说明如下：

| 字段               | 类型           | 设计说明                           |
| ------------------ | -------------- | ---------------------------------- |
| `from_state_code`  | `VARCHAR(64)`  | 来源状态                           |
| `to_state_code`    | `VARCHAR(64)`  | 目标状态                           |
| `event_code`       | `VARCHAR(64)`  | 触发事件，例如 `pay`、`cancel`     |
| `event_name`       | `VARCHAR(128)` | 事件名称                           |
| `action_type`      | `VARCHAR(32)`  | 动作类型，区分人工、自动和系统触发 |
| `require_reason`   | `TINYINT`      | 是否必须填写流转原因               |
| `guard_expression` | `VARCHAR(500)` | 守卫条件标识或表达式               |
| `sort_no`          | `INT`          | 展示排序                           |
| `status`           | `TINYINT`      | 流转规则是否启用                   |

状态变更记录表字段说明如下：

| 字段                            | 类型                 | 设计说明                   |
| ------------------------------- | -------------------- | -------------------------- |
| `machine_code`                  | `VARCHAR(64)`        | 状态机编码                 |
| `biz_id`                        | `BIGINT UNSIGNED`    | 业务数据ID                 |
| `biz_no`                        | `VARCHAR(128)`       | 业务单号，便于人工排查     |
| `from_state_code`               | `VARCHAR(64)`        | 变更前状态                 |
| `to_state_code`                 | `VARCHAR(64)`        | 变更后状态                 |
| `event_code`                    | `VARCHAR(64)`        | 触发事件                   |
| `event_name`                    | `VARCHAR(128)`       | 事件名称                   |
| `action_type`                   | `VARCHAR(32)`        | 动作类型                   |
| `change_reason`                 | `VARCHAR(500)`       | 变更原因                   |
| `operator_id` / `operator_name` | `BIGINT` / `VARCHAR` | 操作人信息                 |
| `request_id`                    | `VARCHAR(128)`       | 请求ID，用于幂等和链路追踪 |
| `source_type`                   | `VARCHAR(64)`        | 来源类型                   |
| `extra_json`                    | `JSON`               | 扩展信息                   |
| `operate_time`                  | `DATETIME`           | 操作时间                   |

`state_code` 应使用稳定英文编码，不建议使用中文名称。业务表保存 `state_code`，页面展示再通过状态节点表翻译为 `state_name`。状态编码创建后不建议修改，否则会影响业务表历史数据、状态日志和接口兼容性。

### 索引设计

索引设计需要覆盖状态机定义查询、状态节点展示、流转规则校验、业务状态日志查询四类场景。状态流转规则是状态机模型的核心高频读取点，应重点优化 `machine_code + from_state_code + event_code` 的查询路径。

状态机定义表推荐索引如下：

```sql
ALTER TABLE sys_state_machine
    ADD UNIQUE KEY uk_tenant_machine_code_deleted (
        tenant_id,
        machine_code,
        deleted_at
    );

ALTER TABLE sys_state_machine
    ADD KEY idx_tenant_biz_status (
        tenant_id,
        biz_type,
        status
    );
```

状态节点表推荐索引如下：

```sql
ALTER TABLE sys_state_node
    ADD UNIQUE KEY uk_tenant_machine_state_deleted (
        tenant_id,
        machine_code,
        state_code,
        deleted_at
    );

ALTER TABLE sys_state_node
    ADD KEY idx_machine_status_sort (
        machine_id,
        status,
        sort_no,
        id
    );

ALTER TABLE sys_state_node
    ADD KEY idx_tenant_machine_status_sort (
        tenant_id,
        machine_code,
        status,
        sort_no,
        id
    );
```

状态流转规则表推荐索引如下：

```sql
ALTER TABLE sys_state_transition
    ADD UNIQUE KEY uk_tenant_machine_transition_deleted (
        tenant_id,
        machine_code,
        from_state_code,
        event_code,
        to_state_code,
        deleted_at
    );

ALTER TABLE sys_state_transition
    ADD KEY idx_tenant_machine_from_event (
        tenant_id,
        machine_code,
        from_state_code,
        event_code,
        status
    );

ALTER TABLE sys_state_transition
    ADD KEY idx_tenant_machine_from_status (
        tenant_id,
        machine_code,
        from_state_code,
        status,
        sort_no
    );

ALTER TABLE sys_state_transition
    ADD KEY idx_tenant_machine_to_status (
        tenant_id,
        machine_code,
        to_state_code,
        status
    );
```

状态变更记录表推荐索引如下：

```sql
ALTER TABLE sys_state_change_log
    ADD KEY idx_tenant_machine_biz_time (
        tenant_id,
        machine_code,
        biz_id,
        operate_time
    );

ALTER TABLE sys_state_change_log
    ADD KEY idx_tenant_machine_biz_no_time (
        tenant_id,
        machine_code,
        biz_no,
        operate_time
    );

ALTER TABLE sys_state_change_log
    ADD KEY idx_tenant_machine_event_time (
        tenant_id,
        machine_code,
        event_code,
        operate_time
    );

ALTER TABLE sys_state_change_log
    ADD KEY idx_request_id (
        request_id
    );
```

业务表应根据实际查询场景设计状态索引。以订单表为例，常见查询是按租户、订单状态、创建时间分页。

```sql
ALTER TABLE biz_order
    ADD UNIQUE KEY uk_tenant_order_no_deleted (
        tenant_id,
        order_no,
        deleted_at
    );

ALTER TABLE biz_order
    ADD KEY idx_tenant_status_create_time (
        tenant_id,
        order_status,
        create_time,
        id
    );
```

### 常用查询

常用查询主要围绕“获取状态节点、校验流转规则、查询可执行动作、查询状态日志、业务列表状态展示”展开。状态机查询通常读多写少，状态定义和流转规则适合缓存。

#### 查询状态机节点列表

该查询用于后台展示状态机节点，也可用于前端展示状态筛选项。

```sql
SELECT
    state_code,
    state_name,
    state_type,
    color,
    sort_no
FROM sys_state_node
WHERE tenant_id = 0
  AND machine_code = 'order_status'
  AND status = 1
  AND deleted_at = 0
ORDER BY sort_no ASC, id ASC;
```

该查询与字典项查询类似，但状态节点具备状态机语义，例如初始状态、普通状态和终态。

#### 查询当前状态允许执行的动作

该查询用于页面按钮控制。例如订单处于 `created` 状态时，查询当前可执行的动作可能包括支付、取消。

```sql
SELECT
    event_code,
    event_name,
    to_state_code,
    action_type,
    require_reason,
    guard_expression
FROM sys_state_transition
WHERE tenant_id = 0
  AND machine_code = 'order_status'
  AND from_state_code = 'created'
  AND status = 1
  AND deleted_at = 0
ORDER BY sort_no ASC, id ASC;
```

前端可以根据该结果展示操作按钮，但真正的状态流转校验必须在后端执行，不能只依赖前端按钮控制。

#### 校验指定状态流转是否合法

该查询用于业务执行前的状态流转校验。例如订单从 `created` 通过 `pay` 事件流转到 `paid`。

```sql
SELECT
    id,
    event_code,
    event_name,
    from_state_code,
    to_state_code,
    action_type,
    require_reason,
    guard_expression
FROM sys_state_transition
WHERE tenant_id = 0
  AND machine_code = 'order_status'
  AND from_state_code = 'created'
  AND event_code = 'pay'
  AND to_state_code = 'paid'
  AND status = 1
  AND deleted_at = 0
LIMIT 1;
```

如果查询不到记录，说明该状态流转不合法。业务层应直接拒绝操作，并返回明确错误信息，例如“当前订单状态不允许支付”。

#### 查询业务对象状态流转历史

该查询用于详情页展示状态轨迹，例如订单状态变更记录、工单处理记录、审核操作记录。

```sql
SELECT
    from_state_code,
    to_state_code,
    event_code,
    event_name,
    action_type,
    change_reason,
    operator_name,
    source_type,
    operate_time
FROM sys_state_change_log
WHERE tenant_id = 0
  AND machine_code = 'order_status'
  AND biz_id = 1000000000000000001
ORDER BY operate_time ASC, id ASC;
```

状态日志是历史事实，一般不建议物理删除。即使业务表被软删除，状态变更记录也应保留一段时间，便于审计。

#### 查询业务列表并展示状态名称

该查询用于后台业务列表，将业务表中的状态编码翻译为状态名称和颜色。

```sql
SELECT
    o.id,
    o.order_no,
    o.order_status,
    n.state_name AS order_status_name,
    n.color AS order_status_color,
    o.create_time
FROM biz_order o
LEFT JOIN sys_state_node n
       ON n.tenant_id = o.tenant_id
      AND n.machine_code = 'order_status'
      AND n.state_code = o.order_status
      AND n.deleted_at = 0
WHERE o.tenant_id = 0
  AND o.deleted_at = 0
ORDER BY o.create_time DESC
LIMIT 20;
```

如果业务列表是高频接口，推荐提前缓存状态节点映射，在应用层完成状态名称翻译，避免频繁关联状态节点表。

#### 查询进入终态的业务数据

该查询用于统计已完成、已取消、已关闭等终态数据，也可用于归档任务筛选。

```sql
SELECT
    o.id,
    o.order_no,
    o.order_status,
    o.update_time
FROM biz_order o
WHERE o.tenant_id = 0
  AND o.order_status IN ('completed', 'cancelled')
  AND o.deleted_at = 0
ORDER BY o.update_time DESC
LIMIT 100;
```

终态集合可以来自状态机定义表中的 `terminal_state_codes`，应用层读取后再拼装业务查询条件。对于高频统计，建议在业务表中直接按状态字段建立合适索引。

#### 查询某类事件的状态变更记录

该查询用于审计和排查，例如查询所有取消订单事件。

```sql
SELECT
    biz_id,
    biz_no,
    from_state_code,
    to_state_code,
    event_code,
    change_reason,
    operator_name,
    source_type,
    operate_time
FROM sys_state_change_log
WHERE tenant_id = 0
  AND machine_code = 'order_status'
  AND event_code = 'cancel'
  AND operate_time >= '2026-01-01 00:00:00'
  AND operate_time < '2026-02-01 00:00:00'
ORDER BY operate_time DESC, id DESC
LIMIT 100;
```

事件维度查询适合做运营审计、异常排查和统计分析。如果数据量很大，可以结合历史版本与数据变更模型、日志与审计模型或归档数据模型处理。

### 常用写入

常用写入包括初始化状态机、维护状态节点、维护流转规则、执行业务状态变更、写入状态变更记录。状态定义和规则写入通常由后台管理或初始化脚本完成；业务状态变更通常由业务接口、定时任务或消息消费触发。

初始化状态机定义时，应先创建状态机，再创建状态节点和流转规则。

```sql
INSERT INTO sys_state_machine (
    id,
    tenant_id,
    machine_code,
    machine_name,
    biz_type,
    initial_state_code,
    terminal_state_codes,
    status,
    remark,
    create_by,
    update_by
) VALUES (
    1000000000000000001,
    0,
    'order_status',
    '订单状态机',
    'order',
    'created',
    JSON_ARRAY('completed', 'cancelled'),
    1,
    '订单从创建到完成或取消的状态流转规则',
    1,
    1
);
```

初始化状态节点时，建议明确初始状态、普通状态和终态。

```sql
INSERT INTO sys_state_node (
    id,
    tenant_id,
    machine_id,
    machine_code,
    state_code,
    state_name,
    state_type,
    sort_no,
    status,
    color,
    remark,
    create_by,
    update_by
) VALUES
(
    1000000000000000101,
    0,
    1000000000000000001,
    'order_status',
    'created',
    '待支付',
    'initial',
    10,
    1,
    'warning',
    '订单已创建，等待用户支付',
    1,
    1
),
(
    1000000000000000102,
    0,
    1000000000000000001,
    'order_status',
    'paid',
    '已支付',
    'normal',
    20,
    1,
    'success',
    '订单已完成支付，等待发货',
    1,
    1
),
(
    1000000000000000103,
    0,
    1000000000000000001,
    'order_status',
    'shipped',
    '已发货',
    'normal',
    30,
    1,
    'primary',
    '订单已发货，等待确认收货',
    1,
    1
),
(
    1000000000000000104,
    0,
    1000000000000000001,
    'order_status',
    'completed',
    '已完成',
    'terminal',
    40,
    1,
    'success',
    '订单已完成',
    1,
    1
),
(
    1000000000000000105,
    0,
    1000000000000000001,
    'order_status',
    'cancelled',
    '已取消',
    'terminal',
    90,
    1,
    'info',
    '订单已取消',
    1,
    1
);
```

初始化流转规则时，应通过事件明确状态变化原因。一个来源状态可以对应多个事件，不同事件可以流转到不同目标状态。

```sql
INSERT INTO sys_state_transition (
    id,
    tenant_id,
    machine_id,
    machine_code,
    from_state_code,
    to_state_code,
    event_code,
    event_name,
    action_type,
    require_reason,
    guard_expression,
    sort_no,
    status,
    remark,
    create_by,
    update_by
) VALUES
(
    1000000000000000201,
    0,
    1000000000000000001,
    'order_status',
    'created',
    'paid',
    'pay',
    '支付',
    'system',
    0,
    'PAY_AMOUNT_MATCH',
    10,
    1,
    '支付成功后订单从待支付流转为已支付',
    1,
    1
),
(
    1000000000000000202,
    0,
    1000000000000000001,
    'order_status',
    'created',
    'cancelled',
    'cancel',
    '取消',
    'manual',
    1,
    NULL,
    20,
    1,
    '用户或后台取消待支付订单',
    1,
    1
),
(
    1000000000000000203,
    0,
    1000000000000000001,
    'order_status',
    'paid',
    'shipped',
    'ship',
    '发货',
    'manual',
    0,
    'HAS_DELIVERY_INFO',
    30,
    1,
    '订单发货后从已支付流转为已发货',
    1,
    1
),
(
    1000000000000000204,
    0,
    1000000000000000001,
    'order_status',
    'shipped',
    'completed',
    'confirm_receipt',
    '确认收货',
    'manual',
    0,
    NULL,
    40,
    1,
    '用户确认收货后订单完成',
    1,
    1
);
```

执行业务状态变更时，应在事务中先校验流转规则，再使用当前状态作为更新条件，最后写入状态变更记录。下面示例表示订单支付成功后从 `created` 流转到 `paid`。

```sql
START TRANSACTION;

SELECT
    id,
    require_reason,
    guard_expression
FROM sys_state_transition
WHERE tenant_id = 0
  AND machine_code = 'order_status'
  AND from_state_code = 'created'
  AND event_code = 'pay'
  AND to_state_code = 'paid'
  AND status = 1
  AND deleted_at = 0
LIMIT 1
FOR UPDATE;

UPDATE biz_order
SET order_status = 'paid',
    paid_time = CURRENT_TIMESTAMP,
    version = version + 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000001
  AND tenant_id = 0
  AND order_status = 'created'
  AND deleted_at = 0;

INSERT INTO sys_state_change_log (
    id,
    tenant_id,
    machine_code,
    biz_id,
    biz_no,
    from_state_code,
    to_state_code,
    event_code,
    event_name,
    action_type,
    change_reason,
    operator_id,
    operator_name,
    request_id,
    source_type,
    extra_json
) VALUES (
    1000000000000000301,
    0,
    'order_status',
    1000000000000000001,
    'O202601010001',
    'created',
    'paid',
    'pay',
    '支付',
    'system',
    '支付回调确认成功',
    NULL,
    'system',
    'REQ202601010001',
    'api',
    JSON_OBJECT('payNo', 'P202601010001')
);

COMMIT;
```

取消订单时，如果流转规则要求填写原因，业务层应先校验 `require_reason = 1`，再要求用户填写取消原因。

```sql
START TRANSACTION;

UPDATE biz_order
SET order_status = 'cancelled',
    cancelled_time = CURRENT_TIMESTAMP,
    version = version + 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000001
  AND tenant_id = 0
  AND order_status = 'created'
  AND deleted_at = 0;

INSERT INTO sys_state_change_log (
    id,
    tenant_id,
    machine_code,
    biz_id,
    biz_no,
    from_state_code,
    to_state_code,
    event_code,
    event_name,
    action_type,
    change_reason,
    operator_id,
    operator_name,
    request_id,
    source_type
) VALUES (
    1000000000000000302,
    0,
    'order_status',
    1000000000000000001,
    'O202601010001',
    'created',
    'cancelled',
    'cancel',
    '取消',
    'manual',
    '用户主动取消订单',
    10001,
    '张三',
    'REQ202601010002',
    'admin'
);

COMMIT;
```

禁用流转规则适合临时关闭某个状态动作，例如临时不允许后台取消已支付订单。

```sql
UPDATE sys_state_transition
SET status = 0,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 0
  AND machine_code = 'order_status'
  AND from_state_code = 'paid'
  AND event_code = 'cancel'
  AND deleted_at = 0;
```

新增流转规则时，要先确认来源状态和目标状态都存在且启用，再插入规则。不要让流转规则指向不存在的状态节点。

```sql
INSERT INTO sys_state_transition (
    id,
    tenant_id,
    machine_id,
    machine_code,
    from_state_code,
    to_state_code,
    event_code,
    event_name,
    action_type,
    require_reason,
    sort_no,
    status,
    remark,
    create_by,
    update_by
) VALUES (
    1000000000000000205,
    0,
    1000000000000000001,
    'order_status',
    'paid',
    'cancelled',
    'refund_cancel',
    '退款取消',
    'manual',
    1,
    50,
    1,
    '已支付订单退款成功后取消订单',
    1,
    1
);
```

软删除状态节点或流转规则时，需要先确认业务表中不存在仍在使用的状态，或者确认该状态已经被完整迁移。不建议直接删除已经产生历史数据的状态。

```sql
UPDATE sys_state_transition
SET deleted_at = UNIX_TIMESTAMP(),
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE tenant_id = 0
  AND machine_code = 'order_status'
  AND from_state_code = 'paid'
  AND event_code = 'refund_cancel'
  AND deleted_at = 0;
```

### 常见问题

状态机模型和字典表模型有什么区别？字典表只定义状态值和展示名称，状态机模型还定义状态之间的合法流转关系。只需要展示状态时用字典表，需要控制流程和记录轨迹时用状态机。

业务表是否还需要保存状态字段？需要。状态机表用于定义规则，业务表必须保存当前状态。否则每次查询业务列表都要从日志中推导当前状态，查询成本高，逻辑也复杂。

状态变更记录表能不能替代业务表状态字段？不建议。状态变更记录表适合审计和追踪，不适合作为高频业务查询的主数据。业务表保存当前状态，日志表保存历史轨迹，是更稳定的设计。

状态编码可以修改吗？不建议修改。状态编码会被业务表、日志表、接口参数、消息事件和前端逻辑引用。确实需要调整时，应新增状态编码，并通过数据迁移脚本处理历史数据。

终态是否允许继续流转？通常不允许。比如订单已完成、已取消后一般不应继续流转。但如果业务允许售后、撤销、重开，应明确新增流转规则，不要在代码中绕过状态机。

是否需要缓存状态机规则？需要。状态节点和流转规则通常读多写少，适合按 `tenant_id + machine_code` 缓存。后台修改状态机后，应主动清理缓存或发布规则变更事件。

能不能把复杂条件都放在 `guard_expression`？不建议。`guard_expression` 适合保存条件标识或轻量表达式，例如 `HAS_DELIVERY_INFO`、`PAY_AMOUNT_MATCH`。复杂条件仍应在业务代码或规则引擎中实现，并由状态机流转前统一校验。

状态变更时为什么要使用当前状态作为更新条件？这是为了防止并发更新导致状态错乱。例如一个订单同时收到支付回调和取消请求，更新条件中带上 `order_status = 'created'` 可以保证只有一个操作成功。

状态流转失败后如何处理？如果流转规则不存在，应返回“当前状态不允许该操作”。如果业务守卫条件不满足，应返回具体业务原因，例如“未填写物流信息，不能发货”。如果并发更新失败，应重新查询业务当前状态后再判断是否重试。

状态机是否适合所有流程？不适合。状态机适合状态数量有限、流转关系清晰的流程。如果流程包含复杂审批节点、动态处理人、会签、或签、回退、多实例任务，应考虑工作流模型或审批流模型。

### 总结

状态机模型适合管理有明确生命周期、状态流转受控、需要完整轨迹的业务对象。它通常由状态机定义表、状态节点表、状态流转规则表和状态变更记录表组成，业务表保存当前状态，状态日志保存历史变化。

实际落地时，需要重点控制四件事：第一，状态编码创建后保持稳定；第二，状态变更必须先校验流转规则；第三，业务状态更新和状态日志写入必须放在同一事务；第四，高频读取的状态节点和流转规则应使用缓存。这样可以让业务流程既具备灵活配置能力，又能保证状态流转的可控性和可追踪性。



## 草稿-发布模型

草稿-发布模型用于管理需要“先编辑、后审核或确认、再对外生效”的业务数据，例如内容管理、商品信息、页面配置、活动规则、协议模板、公告、配置变更等。它与配置项模型的区别在于：配置项通常直接修改当前值，而草稿-发布模型强调“编辑态”和“生效态”隔离，避免未完成内容影响线上业务。该章节属于《MySQL 8 常用业务建模模型》中的“通用字典与配置模型”部分。

### 适用场景

草稿-发布模型适合处理内容复杂、修改频繁、上线前需要预览或审核、线上读取必须稳定的业务数据。它的核心目标是让编辑过程不影响当前线上生效版本。

典型适用场景如下：

| 场景     | 示例                           | 说明                                   |
| -------- | ------------------------------ | -------------------------------------- |
| 内容管理 | 文章、公告、帮助文档、协议模板 | 编辑草稿后再发布，线上只读取已发布版本 |
| 商品资料 | 商品详情、营销文案、售卖规则   | 商品信息调整需要预览或审核后生效       |
| 页面配置 | 首页楼层、Banner、导航配置     | 后台编辑不应立即影响前台页面           |
| 活动配置 | 满减规则、优惠券规则、秒杀活动 | 发布后按生效时间对外执行               |
| 表单模板 | 问卷、报名表、审批表单         | 草稿可反复调整，发布后用户填写固定版本 |
| 规则配置 | 风控规则、推荐规则、运营规则   | 需要灰度、审核、回滚和版本追踪         |

不建议把所有普通业务表都做成草稿-发布模型。只有当“编辑态”和“线上态”必须隔离，或者需要版本回溯、发布审核、定时生效时，才适合使用该模型。

### 建模结构

草稿-发布模型常见有两种设计：单表多状态模式和主表加版本表模式。单表多状态模式适合简单内容，主表加版本表模式适合复杂业务和长期维护。这里推荐使用“主表 + 版本表 + 发布记录表”的结构。

主表保存业务对象的稳定身份和当前发布状态，不直接承载完整可变内容。版本表保存每一次草稿、审核、发布版本的完整内容。发布记录表保存每次发布动作，便于审计、回滚和问题排查。

草稿发布主表用于保存业务对象当前状态、当前发布版本和当前草稿版本。

```sql
CREATE TABLE biz_publish_subject (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型，例如 article、product_page、activity_rule',
    subject_code VARCHAR(128) NOT NULL COMMENT '业务对象编码，例如 article_001、home_banner',
    subject_name VARCHAR(128) NOT NULL COMMENT '业务对象名称',
    current_version_id BIGINT UNSIGNED DEFAULT NULL COMMENT '当前已发布版本ID',
    draft_version_id BIGINT UNSIGNED DEFAULT NULL COMMENT '当前草稿版本ID',
    publish_status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '发布状态：draft草稿，reviewing审核中，published已发布，offline已下线',
    publish_time DATETIME DEFAULT NULL COMMENT '最近发布时间',
    offline_time DATETIME DEFAULT NULL COMMENT '最近下线时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    deleted_at BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳，0表示未删除',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '草稿发布主表';
```

版本表用于保存草稿版本、审核版本和已发布版本的具体内容。内容可以根据业务拆成具体字段，也可以使用 JSON 保存结构化配置。对于核心查询字段，建议拆成独立列，不要全部放进 JSON。

```sql
CREATE TABLE biz_publish_version (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    subject_id BIGINT UNSIGNED NOT NULL COMMENT '业务对象ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    subject_code VARCHAR(128) NOT NULL COMMENT '业务对象编码，冗余字段',
    version_no INT UNSIGNED NOT NULL COMMENT '版本号，从1开始递增',
    version_status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '版本状态：draft草稿，reviewing审核中，published已发布，rejected已驳回，offline已下线',
    title VARCHAR(255) DEFAULT NULL COMMENT '标题或名称',
    content_json JSON DEFAULT NULL COMMENT '版本内容JSON，保存结构化内容',
    content_text LONGTEXT DEFAULT NULL COMMENT '版本文本内容，适合文章、协议、公告等',
    effective_time DATETIME DEFAULT NULL COMMENT '计划生效时间，为空表示立即生效',
    expire_time DATETIME DEFAULT NULL COMMENT '计划失效时间，为空表示不自动失效',
    review_status VARCHAR(32) NOT NULL DEFAULT 'none' COMMENT '审核状态：none无需审核，pending待审核，approved已通过，rejected已驳回',
    review_reason VARCHAR(500) DEFAULT NULL COMMENT '审核意见或驳回原因',
    published_at DATETIME DEFAULT NULL COMMENT '发布时间',
    offline_at DATETIME DEFAULT NULL COMMENT '下线时间',
    is_current TINYINT NOT NULL DEFAULT 0 COMMENT '是否当前线上版本：0否，1是',
    change_summary VARCHAR(500) DEFAULT NULL COMMENT '版本变更说明',
    deleted_at BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳，0表示未删除',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '草稿发布版本表';
```

发布记录表用于记录每次发布、下线、回滚等动作。它不是当前状态的唯一来源，而是用于审计和追踪。

```sql
CREATE TABLE biz_publish_log (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
    tenant_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    subject_id BIGINT UNSIGNED NOT NULL COMMENT '业务对象ID',
    version_id BIGINT UNSIGNED NOT NULL COMMENT '版本ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    subject_code VARCHAR(128) NOT NULL COMMENT '业务对象编码',
    version_no INT UNSIGNED NOT NULL COMMENT '版本号',
    action_type VARCHAR(32) NOT NULL COMMENT '操作类型：submit提交审核，approve审核通过，reject审核驳回，publish发布，offline下线，rollback回滚',
    from_status VARCHAR(32) DEFAULT NULL COMMENT '操作前状态',
    to_status VARCHAR(32) NOT NULL COMMENT '操作后状态',
    action_reason VARCHAR(500) DEFAULT NULL COMMENT '操作原因',
    operator_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(128) DEFAULT NULL COMMENT '操作人名称',
    request_id VARCHAR(128) DEFAULT NULL COMMENT '请求ID，用于幂等和链路追踪',
    extra_json JSON DEFAULT NULL COMMENT '扩展信息',
    operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '草稿发布操作记录表';
```

如果业务存在审批流、多人协作、定时发布、灰度发布、回滚等需求，可以在此基础上继续扩展审批任务表、发布任务表、灰度规则表。普通场景不建议一开始就过度设计，主表、版本表和发布记录表已经能覆盖大部分业务。

### 字段设计

字段设计需要区分“业务对象身份”“版本内容”“发布状态”和“操作轨迹”。主表负责定位业务对象，版本表负责保存每一版内容，日志表负责记录操作过程。

主表字段说明如下：

| 字段                          | 类型              | 设计说明                                   |
| ----------------------------- | ----------------- | ------------------------------------------ |
| `id`                          | `BIGINT UNSIGNED` | 业务对象主键ID                             |
| `tenant_id`                   | `BIGINT UNSIGNED` | 租户ID，单租户可固定为 `0`                 |
| `biz_type`                    | `VARCHAR(64)`     | 业务类型，用于区分文章、商品页、活动规则等 |
| `subject_code`                | `VARCHAR(128)`    | 业务对象编码，应保持稳定                   |
| `subject_name`                | `VARCHAR(128)`    | 业务对象名称，用于后台展示                 |
| `current_version_id`          | `BIGINT UNSIGNED` | 当前线上生效版本ID                         |
| `draft_version_id`            | `BIGINT UNSIGNED` | 当前草稿版本ID                             |
| `publish_status`              | `VARCHAR(32)`     | 当前发布状态                               |
| `publish_time`                | `DATETIME`        | 最近发布时间                               |
| `offline_time`                | `DATETIME`        | 最近下线时间                               |
| `status`                      | `TINYINT`         | 主体是否启用                               |
| `remark`                      | `VARCHAR(500)`    | 备注说明                                   |
| `deleted_at`                  | `BIGINT UNSIGNED` | 软删除时间戳                               |
| `version`                     | `INT UNSIGNED`    | 乐观锁版本号                               |
| `create_by` / `update_by`     | `BIGINT UNSIGNED` | 创建人和更新人                             |
| `create_time` / `update_time` | `DATETIME`        | 创建时间和更新时间                         |

版本表字段说明如下：

| 字段             | 类型              | 设计说明                           |
| ---------------- | ----------------- | ---------------------------------- |
| `subject_id`     | `BIGINT UNSIGNED` | 关联主表ID                         |
| `biz_type`       | `VARCHAR(64)`     | 冗余业务类型，便于查询             |
| `subject_code`   | `VARCHAR(128)`    | 冗余业务对象编码，便于读取和审计   |
| `version_no`     | `INT UNSIGNED`    | 版本号，同一业务对象下递增         |
| `version_status` | `VARCHAR(32)`     | 版本状态                           |
| `title`          | `VARCHAR(255)`    | 标题或名称，适合后台列表展示       |
| `content_json`   | `JSON`            | 结构化内容，例如页面配置、活动规则 |
| `content_text`   | `LONGTEXT`        | 长文本内容，例如公告、协议、文章   |
| `effective_time` | `DATETIME`        | 生效时间，支持定时发布             |
| `expire_time`    | `DATETIME`        | 失效时间，支持自动下线             |
| `review_status`  | `VARCHAR(32)`     | 审核状态                           |
| `review_reason`  | `VARCHAR(500)`    | 审核意见或驳回原因                 |
| `published_at`   | `DATETIME`        | 版本发布时间                       |
| `offline_at`     | `DATETIME`        | 版本下线时间                       |
| `is_current`     | `TINYINT`         | 是否当前线上版本                   |
| `change_summary` | `VARCHAR(500)`    | 版本变更说明                       |

发布记录表字段说明如下：

| 字段                            | 类型                 | 设计说明               |
| ------------------------------- | -------------------- | ---------------------- |
| `subject_id`                    | `BIGINT UNSIGNED`    | 业务对象ID             |
| `version_id`                    | `BIGINT UNSIGNED`    | 版本ID                 |
| `biz_type`                      | `VARCHAR(64)`        | 业务类型               |
| `subject_code`                  | `VARCHAR(128)`       | 业务对象编码           |
| `version_no`                    | `INT UNSIGNED`       | 版本号                 |
| `action_type`                   | `VARCHAR(32)`        | 操作类型               |
| `from_status`                   | `VARCHAR(32)`        | 操作前状态             |
| `to_status`                     | `VARCHAR(32)`        | 操作后状态             |
| `action_reason`                 | `VARCHAR(500)`       | 操作原因               |
| `operator_id` / `operator_name` | `BIGINT` / `VARCHAR` | 操作人信息             |
| `request_id`                    | `VARCHAR(128)`       | 请求ID，用于幂等和追踪 |
| `extra_json`                    | `JSON`               | 扩展信息               |
| `operate_time`                  | `DATETIME`           | 操作时间               |

`version_status` 和 `publish_status` 的含义需要保持清晰。`publish_status` 是业务对象当前对外状态，`version_status` 是某个版本自身的状态。一个业务对象可以有一个已发布版本，同时也可以存在一个正在编辑的草稿版本。

常见状态建议如下：

| 状态        | 适用字段                            | 说明                           |
| ----------- | ----------------------------------- | ------------------------------ |
| `draft`     | `publish_status` / `version_status` | 草稿态，允许编辑，不对外生效   |
| `reviewing` | `publish_status` / `version_status` | 审核中，通常不允许继续直接修改 |
| `published` | `publish_status` / `version_status` | 已发布，对外生效               |
| `rejected`  | `version_status`                    | 审核驳回，可重新编辑           |
| `offline`   | `publish_status` / `version_status` | 已下线，不再对外生效           |

### 索引设计

索引设计需要覆盖后台列表、当前发布版本读取、草稿读取、版本历史查询、发布记录审计等场景。线上读取通常只关心当前生效版本，应重点优化 `tenant_id + biz_type + subject_code + current_version_id` 相关路径。

主表推荐索引如下：

```sql
ALTER TABLE biz_publish_subject
    ADD UNIQUE KEY uk_tenant_biz_subject_deleted (
        tenant_id,
        biz_type,
        subject_code,
        deleted_at
    );

ALTER TABLE biz_publish_subject
    ADD KEY idx_tenant_biz_status_update_time (
        tenant_id,
        biz_type,
        publish_status,
        update_time,
        id
    );

ALTER TABLE biz_publish_subject
    ADD KEY idx_current_version_id (
        current_version_id
    );

ALTER TABLE biz_publish_subject
    ADD KEY idx_draft_version_id (
        draft_version_id
    );
```

版本表推荐索引如下：

```sql
ALTER TABLE biz_publish_version
    ADD UNIQUE KEY uk_subject_version_deleted (
        subject_id,
        version_no,
        deleted_at
    );

ALTER TABLE biz_publish_version
    ADD KEY idx_tenant_biz_subject_status (
        tenant_id,
        biz_type,
        subject_code,
        version_status,
        update_time
    );

ALTER TABLE biz_publish_version
    ADD KEY idx_subject_status_version (
        subject_id,
        version_status,
        version_no
    );

ALTER TABLE biz_publish_version
    ADD KEY idx_subject_current (
        subject_id,
        is_current
    );

ALTER TABLE biz_publish_version
    ADD KEY idx_effective_expire_time (
        effective_time,
        expire_time
    );
```

发布记录表推荐索引如下：

```sql
ALTER TABLE biz_publish_log
    ADD KEY idx_subject_operate_time (
        subject_id,
        operate_time
    );

ALTER TABLE biz_publish_log
    ADD KEY idx_version_operate_time (
        version_id,
        operate_time
    );

ALTER TABLE biz_publish_log
    ADD KEY idx_tenant_biz_subject_time (
        tenant_id,
        biz_type,
        subject_code,
        operate_time
    );

ALTER TABLE biz_publish_log
    ADD KEY idx_operator_operate_time (
        operator_id,
        operate_time
    );

ALTER TABLE biz_publish_log
    ADD KEY idx_request_id (
        request_id
    );
```

如果线上高频按业务编码读取内容，可以考虑建立只包含线上读取必要字段的读模型表，例如 `biz_publish_online`，把当前生效内容单独冗余出来，减少主表和版本表关联读取成本。

### 常用查询

常用查询主要围绕“读取线上版本、查询草稿、查询版本历史、后台分页、发布记录审计、定时发布扫描”展开。线上查询应只读取当前已发布版本，不应误读草稿内容。

#### 查询当前线上生效版本

该查询用于前台页面、接口服务或业务流程读取当前已发布内容。

```sql
SELECT
    s.id AS subject_id,
    s.subject_code,
    s.subject_name,
    v.id AS version_id,
    v.version_no,
    v.title,
    v.content_json,
    v.content_text,
    v.published_at
FROM biz_publish_subject s
INNER JOIN biz_publish_version v
        ON v.id = s.current_version_id
       AND v.deleted_at = 0
WHERE s.tenant_id = 0
  AND s.biz_type = 'article'
  AND s.subject_code = 'help_center_001'
  AND s.publish_status = 'published'
  AND s.status = 1
  AND s.deleted_at = 0
  AND v.version_status = 'published'
LIMIT 1;
```

前台读取时不应根据最新版本号读取，因为最新版本可能是草稿或审核中版本。正确方式是通过主表的 `current_version_id` 定位当前线上版本。

#### 查询当前草稿版本

该查询用于后台编辑页面打开当前草稿。如果没有草稿，可以基于当前已发布版本复制生成草稿。

```sql
SELECT
    s.id AS subject_id,
    s.subject_code,
    s.subject_name,
    v.id AS draft_version_id,
    v.version_no,
    v.version_status,
    v.title,
    v.content_json,
    v.content_text,
    v.change_summary,
    v.update_time
FROM biz_publish_subject s
LEFT JOIN biz_publish_version v
       ON v.id = s.draft_version_id
      AND v.deleted_at = 0
WHERE s.tenant_id = 0
  AND s.biz_type = 'article'
  AND s.subject_code = 'help_center_001'
  AND s.deleted_at = 0
LIMIT 1;
```

如果 `draft_version_id` 为空，说明当前没有正在编辑的草稿。后台可以提示用户“从线上版本创建草稿”或“创建空白草稿”。

#### 查询版本历史

该查询用于后台查看某个业务对象的所有历史版本。

```sql
SELECT
    id,
    version_no,
    version_status,
    review_status,
    title,
    is_current,
    effective_time,
    expire_time,
    published_at,
    offline_at,
    change_summary,
    create_time,
    update_time
FROM biz_publish_version
WHERE tenant_id = 0
  AND subject_id = 1000000000000000001
  AND deleted_at = 0
ORDER BY version_no DESC;
```

版本历史不建议物理删除。即使版本已经下线，也应保留必要历史记录，方便回滚和审计。

#### 后台分页查询发布对象

该查询用于管理页面查询文章、页面、活动或规则等发布对象。

```sql
SELECT
    id,
    biz_type,
    subject_code,
    subject_name,
    current_version_id,
    draft_version_id,
    publish_status,
    publish_time,
    offline_time,
    status,
    update_time
FROM biz_publish_subject
WHERE tenant_id = 0
  AND biz_type = 'article'
  AND deleted_at = 0
  AND (
      subject_code LIKE CONCAT('%', 'help', '%')
      OR subject_name LIKE CONCAT('%', '帮助', '%')
  )
ORDER BY update_time DESC, id DESC
LIMIT 20 OFFSET 0;
```

后台模糊查询通常不是最高频路径。如果发布对象数量很大，可以增加搜索辅助表或限制为前缀查询。

#### 查询待审核版本

该查询用于审核列表，展示已经提交但尚未审核的版本。

```sql
SELECT
    v.id AS version_id,
    v.subject_id,
    v.subject_code,
    s.subject_name,
    v.version_no,
    v.title,
    v.change_summary,
    v.create_by,
    v.create_time
FROM biz_publish_version v
INNER JOIN biz_publish_subject s
        ON s.id = v.subject_id
       AND s.deleted_at = 0
WHERE v.tenant_id = 0
  AND v.biz_type = 'article'
  AND v.version_status = 'reviewing'
  AND v.review_status = 'pending'
  AND v.deleted_at = 0
ORDER BY v.create_time ASC, v.id ASC
LIMIT 50;
```

如果审核流程复杂，例如多级审核、会签、转交，应将审核能力拆到独立审批模型，不建议只靠版本表字段承载全部流程。

#### 查询发布操作记录

该查询用于查看某个发布对象的完整操作轨迹。

```sql
SELECT
    version_no,
    action_type,
    from_status,
    to_status,
    action_reason,
    operator_name,
    request_id,
    operate_time
FROM biz_publish_log
WHERE tenant_id = 0
  AND biz_type = 'article'
  AND subject_code = 'help_center_001'
ORDER BY operate_time ASC, id ASC;
```

操作记录用于还原发布过程，例如谁创建草稿、谁提交审核、谁审核通过、谁发布、谁回滚。

#### 查询可定时发布的版本

该查询用于定时任务扫描到达生效时间的版本。

```sql
SELECT
    id,
    tenant_id,
    subject_id,
    biz_type,
    subject_code,
    version_no,
    effective_time
FROM biz_publish_version
WHERE version_status = 'reviewing'
  AND review_status = 'approved'
  AND deleted_at = 0
  AND effective_time IS NOT NULL
  AND effective_time <= CURRENT_TIMESTAMP
ORDER BY effective_time ASC, id ASC
LIMIT 100;
```

定时发布任务需要做好幂等控制。即使任务重复执行，也不能重复发布同一个版本或重复写入多条含义相同的发布记录。

#### 查询已过期待下线版本

该查询用于定时任务扫描已经到达失效时间的线上版本。

```sql
SELECT
    s.id AS subject_id,
    s.subject_code,
    s.current_version_id,
    v.version_no,
    v.expire_time
FROM biz_publish_subject s
INNER JOIN biz_publish_version v
        ON v.id = s.current_version_id
       AND v.deleted_at = 0
WHERE s.publish_status = 'published'
  AND s.deleted_at = 0
  AND v.version_status = 'published'
  AND v.expire_time IS NOT NULL
  AND v.expire_time <= CURRENT_TIMESTAMP
ORDER BY v.expire_time ASC, s.id ASC
LIMIT 100;
```

自动下线应记录发布日志，并根据业务需要决定是否将主表状态变为 `offline`。

### 常用写入

常用写入主要包括创建发布对象、创建草稿、保存草稿、提交审核、审核通过、发布版本、下线版本和回滚版本。涉及状态切换的写入应使用事务，并使用乐观锁或当前状态条件防止并发覆盖。

创建发布对象时，可以同时创建初始草稿版本。主表中的 `draft_version_id` 指向草稿版本。

```sql
START TRANSACTION;

INSERT INTO biz_publish_subject (
    id,
    tenant_id,
    biz_type,
    subject_code,
    subject_name,
    draft_version_id,
    publish_status,
    status,
    remark,
    create_by,
    update_by
) VALUES (
    1000000000000000001,
    0,
    'article',
    'help_center_001',
    '帮助中心首页',
    1000000000000000101,
    'draft',
    1,
    '帮助中心首页内容',
    1,
    1
);

INSERT INTO biz_publish_version (
    id,
    tenant_id,
    subject_id,
    biz_type,
    subject_code,
    version_no,
    version_status,
    title,
    content_json,
    content_text,
    review_status,
    is_current,
    change_summary,
    create_by,
    update_by
) VALUES (
    1000000000000000101,
    0,
    1000000000000000001,
    'article',
    'help_center_001',
    1,
    'draft',
    '帮助中心首页',
    JSON_OBJECT('layout', 'default', 'showSearch', true),
    '这里是帮助中心首页内容。',
    'none',
    0,
    '创建初始草稿',
    1,
    1
);

INSERT INTO biz_publish_log (
    id,
    tenant_id,
    subject_id,
    version_id,
    biz_type,
    subject_code,
    version_no,
    action_type,
    from_status,
    to_status,
    action_reason,
    operator_id,
    operator_name,
    request_id
) VALUES (
    1000000000000000201,
    0,
    1000000000000000001,
    1000000000000000101,
    'article',
    'help_center_001',
    1,
    'create_draft',
    NULL,
    'draft',
    '创建初始草稿',
    1,
    'admin',
    'REQ202601010001'
);

COMMIT;
```

保存草稿时，只允许修改草稿态或驳回态版本。已经发布的版本不应直接修改，否则会破坏历史版本的不可变性。

```sql
UPDATE biz_publish_version
SET title = '帮助中心首页新版',
    content_json = JSON_OBJECT('layout', 'default', 'showSearch', true, 'theme', 'simple'),
    content_text = '这里是帮助中心首页新版内容。',
    change_summary = '调整页面主题和首页文案',
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000101
  AND tenant_id = 0
  AND version_status IN ('draft', 'rejected')
  AND deleted_at = 0;
```

提交审核时，应将版本状态从 `draft` 或 `rejected` 更新为 `reviewing`，并写入操作记录。

```sql
START TRANSACTION;

UPDATE biz_publish_version
SET version_status = 'reviewing',
    review_status = 'pending',
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000101
  AND tenant_id = 0
  AND version_status IN ('draft', 'rejected')
  AND deleted_at = 0;

UPDATE biz_publish_subject
SET publish_status = 'reviewing',
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000001
  AND tenant_id = 0
  AND draft_version_id = 1000000000000000101
  AND deleted_at = 0;

INSERT INTO biz_publish_log (
    id,
    tenant_id,
    subject_id,
    version_id,
    biz_type,
    subject_code,
    version_no,
    action_type,
    from_status,
    to_status,
    action_reason,
    operator_id,
    operator_name,
    request_id
) VALUES (
    1000000000000000202,
    0,
    1000000000000000001,
    1000000000000000101,
    'article',
    'help_center_001',
    1,
    'submit',
    'draft',
    'reviewing',
    '提交内容审核',
    1,
    'admin',
    'REQ202601010002'
);

COMMIT;
```

审核通过时，只改变版本审核状态，不一定立即发布。如果存在定时发布，可以保持待发布状态，等待定时任务触发。

```sql
START TRANSACTION;

UPDATE biz_publish_version
SET review_status = 'approved',
    review_reason = '审核通过',
    version = version + 1,
    update_by = 2,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000101
  AND tenant_id = 0
  AND version_status = 'reviewing'
  AND review_status = 'pending'
  AND deleted_at = 0;

INSERT INTO biz_publish_log (
    id,
    tenant_id,
    subject_id,
    version_id,
    biz_type,
    subject_code,
    version_no,
    action_type,
    from_status,
    to_status,
    action_reason,
    operator_id,
    operator_name,
    request_id
) VALUES (
    1000000000000000203,
    0,
    1000000000000000001,
    1000000000000000101,
    'article',
    'help_center_001',
    1,
    'approve',
    'reviewing',
    'reviewing',
    '审核通过',
    2,
    'reviewer',
    'REQ202601010003'
);

COMMIT;
```

审核驳回时，应将版本状态改为 `rejected`，并允许编辑人员继续修改该版本。

```sql
START TRANSACTION;

UPDATE biz_publish_version
SET version_status = 'rejected',
    review_status = 'rejected',
    review_reason = '内容描述不完整，请补充使用说明',
    version = version + 1,
    update_by = 2,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000101
  AND tenant_id = 0
  AND version_status = 'reviewing'
  AND review_status = 'pending'
  AND deleted_at = 0;

UPDATE biz_publish_subject
SET publish_status = 'draft',
    version = version + 1,
    update_by = 2,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000001
  AND tenant_id = 0
  AND draft_version_id = 1000000000000000101
  AND deleted_at = 0;

INSERT INTO biz_publish_log (
    id,
    tenant_id,
    subject_id,
    version_id,
    biz_type,
    subject_code,
    version_no,
    action_type,
    from_status,
    to_status,
    action_reason,
    operator_id,
    operator_name,
    request_id
) VALUES (
    1000000000000000204,
    0,
    1000000000000000001,
    1000000000000000101,
    'article',
    'help_center_001',
    1,
    'reject',
    'reviewing',
    'rejected',
    '内容描述不完整，请补充使用说明',
    2,
    'reviewer',
    'REQ202601010004'
);

COMMIT;
```

发布版本时，应将旧线上版本取消当前标识，将新版本设置为当前版本，并更新主表的 `current_version_id`。该过程必须放在同一个事务中。

```sql
START TRANSACTION;

UPDATE biz_publish_version
SET is_current = 0,
    version_status = 'offline',
    offline_at = CURRENT_TIMESTAMP,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE subject_id = 1000000000000000001
  AND tenant_id = 0
  AND is_current = 1
  AND deleted_at = 0;

UPDATE biz_publish_version
SET version_status = 'published',
    review_status = 'approved',
    published_at = CURRENT_TIMESTAMP,
    is_current = 1,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000101
  AND tenant_id = 0
  AND review_status = 'approved'
  AND deleted_at = 0;

UPDATE biz_publish_subject
SET current_version_id = 1000000000000000101,
    draft_version_id = NULL,
    publish_status = 'published',
    publish_time = CURRENT_TIMESTAMP,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000001
  AND tenant_id = 0
  AND draft_version_id = 1000000000000000101
  AND deleted_at = 0;

INSERT INTO biz_publish_log (
    id,
    tenant_id,
    subject_id,
    version_id,
    biz_type,
    subject_code,
    version_no,
    action_type,
    from_status,
    to_status,
    action_reason,
    operator_id,
    operator_name,
    request_id
) VALUES (
    1000000000000000205,
    0,
    1000000000000000001,
    1000000000000000101,
    'article',
    'help_center_001',
    1,
    'publish',
    'reviewing',
    'published',
    '审核通过后正式发布',
    1,
    'admin',
    'REQ202601010005'
);

COMMIT;
```

从当前线上版本创建新草稿时，应复制当前版本内容，并生成新的版本号。不要直接修改已发布版本。

```sql
START TRANSACTION;

INSERT INTO biz_publish_version (
    id,
    tenant_id,
    subject_id,
    biz_type,
    subject_code,
    version_no,
    version_status,
    title,
    content_json,
    content_text,
    effective_time,
    expire_time,
    review_status,
    is_current,
    change_summary,
    create_by,
    update_by
)
SELECT
    1000000000000000102,
    tenant_id,
    subject_id,
    biz_type,
    subject_code,
    version_no + 1,
    'draft',
    title,
    content_json,
    content_text,
    NULL,
    NULL,
    'none',
    0,
    '基于当前线上版本创建草稿',
    1,
    1
FROM biz_publish_version
WHERE id = 1000000000000000101
  AND tenant_id = 0
  AND version_status = 'published'
  AND is_current = 1
  AND deleted_at = 0;

UPDATE biz_publish_subject
SET draft_version_id = 1000000000000000102,
    publish_status = 'draft',
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000001
  AND tenant_id = 0
  AND current_version_id = 1000000000000000101
  AND draft_version_id IS NULL
  AND deleted_at = 0;

COMMIT;
```

下线当前版本时，应更新主表发布状态，同时更新当前版本状态。下线后前台读取应返回空数据、默认内容或业务定义的兜底内容。

```sql
START TRANSACTION;

UPDATE biz_publish_version
SET version_status = 'offline',
    is_current = 0,
    offline_at = CURRENT_TIMESTAMP,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000101
  AND tenant_id = 0
  AND version_status = 'published'
  AND is_current = 1
  AND deleted_at = 0;

UPDATE biz_publish_subject
SET publish_status = 'offline',
    offline_time = CURRENT_TIMESTAMP,
    current_version_id = NULL,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000001
  AND tenant_id = 0
  AND current_version_id = 1000000000000000101
  AND deleted_at = 0;

INSERT INTO biz_publish_log (
    id,
    tenant_id,
    subject_id,
    version_id,
    biz_type,
    subject_code,
    version_no,
    action_type,
    from_status,
    to_status,
    action_reason,
    operator_id,
    operator_name,
    request_id
) VALUES (
    1000000000000000206,
    0,
    1000000000000000001,
    1000000000000000101,
    'article',
    'help_center_001',
    1,
    'offline',
    'published',
    'offline',
    '内容过期下线',
    1,
    'admin',
    'REQ202601010006'
);

COMMIT;
```

回滚版本时，本质是把历史版本重新设置为当前线上版本。推荐不要修改历史版本原记录，而是复制历史版本生成一个新版本再发布，这样回滚行为也有独立版本号。

```sql
START TRANSACTION;

INSERT INTO biz_publish_version (
    id,
    tenant_id,
    subject_id,
    biz_type,
    subject_code,
    version_no,
    version_status,
    title,
    content_json,
    content_text,
    review_status,
    published_at,
    is_current,
    change_summary,
    create_by,
    update_by
)
SELECT
    1000000000000000103,
    tenant_id,
    subject_id,
    biz_type,
    subject_code,
    (
        SELECT MAX(v2.version_no) + 1
        FROM biz_publish_version v2
        WHERE v2.subject_id = biz_publish_version.subject_id
          AND v2.deleted_at = 0
    ),
    'published',
    title,
    content_json,
    content_text,
    'approved',
    CURRENT_TIMESTAMP,
    1,
    CONCAT('回滚自版本：', version_no),
    1,
    1
FROM biz_publish_version
WHERE id = 1000000000000000101
  AND tenant_id = 0
  AND deleted_at = 0;

UPDATE biz_publish_version
SET is_current = 0,
    version_status = 'offline',
    offline_at = CURRENT_TIMESTAMP,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE subject_id = 1000000000000000001
  AND tenant_id = 0
  AND id <> 1000000000000000103
  AND is_current = 1
  AND deleted_at = 0;

UPDATE biz_publish_subject
SET current_version_id = 1000000000000000103,
    draft_version_id = NULL,
    publish_status = 'published',
    publish_time = CURRENT_TIMESTAMP,
    version = version + 1,
    update_by = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000000000000000001
  AND tenant_id = 0
  AND deleted_at = 0;

INSERT INTO biz_publish_log (
    id,
    tenant_id,
    subject_id,
    version_id,
    biz_type,
    subject_code,
    version_no,
    action_type,
    from_status,
    to_status,
    action_reason,
    operator_id,
    operator_name,
    request_id
) VALUES (
    1000000000000000207,
    0,
    1000000000000000001,
    1000000000000000103,
    'article',
    'help_center_001',
    3,
    'rollback',
    'published',
    'published',
    '线上内容异常，回滚到历史稳定版本',
    1,
    'admin',
    'REQ202601010007'
);

COMMIT;
```

### 常见问题

草稿和已发布版本应该放一张表还是两张表？多数场景推荐放在同一张版本表中，通过 `version_status` 区分草稿、审核中、已发布、已下线。这样版本历史完整，查询和回滚也更统一。如果草稿字段和线上字段差异极大，可以拆分草稿表和发布表，但维护成本会更高。

已发布版本能不能直接修改？不建议。已发布版本应视为不可变历史。修改内容时应创建新草稿，编辑完成后再发布为新版本。这样可以保留每次发布的完整快照，方便回滚和审计。

为什么主表还要保存 `current_version_id`？因为线上读取需要快速定位当前生效版本。如果每次都按版本状态、版本号、发布时间推导当前版本，查询复杂度和并发风险都会增加。

为什么还要保存 `draft_version_id`？因为同一个业务对象可能已经有线上版本，同时后台又在编辑新草稿。`draft_version_id` 可以明确当前正在编辑的版本，避免误改已发布版本。

一个对象能不能同时有多个草稿？默认不建议。同一业务对象同时存在多个草稿会增加合并、审核和发布冲突。如果确实需要多人协作或分支编辑，应引入更复杂的协作模型或分支版本模型。

发布时如何防止并发冲突？应在发布事务中使用当前状态、当前草稿版本、乐观锁版本号作为更新条件。发布成功后清空 `draft_version_id`，并确保同一业务对象下只有一个 `is_current = 1` 的版本。

定时发布如何保证幂等？定时任务发布前应再次校验版本状态、审核状态和当前主表状态。更新语句应带上状态条件，发布日志可使用 `request_id` 防重，避免重复发布。

回滚应该复用历史版本还是复制新版本？推荐复制历史版本生成新版本再发布。这样回滚本身也是一次明确的发布行为，有新的版本号和操作记录，不会破坏历史版本语义。

草稿发布模型是否需要审核？不一定。如果业务风险低，可以直接从草稿发布；如果内容会影响线上交易、营销活动、法律协议或财务规则，建议增加审核状态或接入审批流。

内容字段应该使用 JSON 还是具体字段？页面配置、活动规则这类结构灵活的内容可以使用 `content_json`。文章、公告、协议可以使用 `content_text`。如果字段需要高频查询、排序或统计，应拆成独立列，不建议放在 JSON 中。

发布日志和版本历史有什么区别？版本历史记录“内容是什么”，发布日志记录“谁在什么时候做了什么动作”。两者都需要，不能互相完全替代。

### 总结

草稿-发布模型适合管理需要编辑态和线上态隔离的业务数据。它通常由主表、版本表和发布记录表组成：主表保存业务对象身份和当前版本指针，版本表保存每一版完整内容，发布记录表保存每次提交、审核、发布、下线和回滚操作。

实际落地时，需要重点控制四件事：第一，已发布版本保持不可变；第二，线上读取只能读取 `current_version_id` 指向的版本；第三，发布、下线、回滚必须放在事务中完成；第四，重要业务必须保留版本历史和发布日志。这样可以让后台编辑更安全，也能保证线上内容稳定、可追踪、可回滚。