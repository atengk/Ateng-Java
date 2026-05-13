# 建模基础规范

## 建模基础规范

建模基础规范用于统一 MySQL 8 业务系统的数据表设计风格，重点约束表命名、字段命名、字段类型、字符集、存储引擎、注释、默认值、索引和基础设计原则。该部分是后续主键设计模型、业务单号模型、基础字段模型以及其他业务模型的通用前置规范。

数据库建模不应只服务于当前页面或当前接口，而应围绕稳定的业务对象进行设计。一个良好的基础模型应具备清晰的业务语义、稳定的字段结构、明确的数据约束和可维护的扩展能力。

### 设计原则

建模时应优先保证业务语义清晰，再考虑性能优化和扩展能力。表结构是业务规则在数据库层面的表达，不应只是临时数据的存储容器。

| 原则     | 说明                                           |
| -------- | ---------------------------------------------- |
| 语义清晰 | 表名和字段名应能直接表达业务含义               |
| 风格统一 | 同一系统内命名、字段顺序、注释风格保持一致     |
| 类型准确 | 根据业务含义选择字段类型，避免使用大字段兜底   |
| 约束明确 | 主键、唯一约束、非空约束、默认值应清晰定义     |
| 注释完整 | 表、字段、状态枚举、金额、时间字段应有明确注释 |
| 索引克制 | 根据真实查询场景设计索引，避免过度索引         |
| 扩展适度 | 预留必要扩展能力，但避免万能表和无边界扩展     |

建模时不建议直接按照接口返回值设计字段。对于核心业务对象，应结合业务生命周期、数据关系、查询场景、并发写入、数据归档和审计追踪等因素综合设计。

### 命名规范

数据库对象命名应统一使用小写字母和下划线，不建议使用驼峰命名、中文命名、拼音缩写或无业务含义的简写。MySQL 在不同操作系统下对大小写敏感性可能存在差异，统一小写可以减少环境差异带来的问题。

| 对象     | 推荐规则                       | 示例          |
| -------- | ------------------------------ | ------------- |
| 数据库名 | 系统名或业务模块名，小写下划线 | `mall_order`  |
| 表名     | 业务对象名，小写下划线         | `order_info`  |
| 字段名   | 业务属性名，小写下划线         | `order_no`    |
| 主键字段 | 统一使用 `id`                  | `id`          |
| 普通索引 | `idx_字段名` 或 `idx_字段组合` | `idx_user_id` |
| 唯一索引 | `uk_字段名` 或 `uk_字段组合`   | `uk_order_no` |

不推荐示例：

```sql
-- 不推荐：命名风格不统一，字段语义不清晰
CREATE TABLE orderInfo (
    oid BIGINT NOT NULL COMMENT '订单ID',
    orderNo VARCHAR(64) NOT NULL COMMENT '订单编号',
    ctime DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (oid)
);
```

推荐示例：

```sql
-- 推荐：命名统一，语义清晰
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

### 表设计规范

表设计应围绕稳定业务实体展开。一个业务表应表达一个主要业务对象，不建议将多个无直接关系的业务概念混入同一张表。

| 规则         | 说明                                             |
| ------------ | ------------------------------------------------ |
| 必须有主键   | 所有业务表必须显式定义主键                       |
| 必须有表注释 | 表注释应说明该表对应的业务对象                   |
| 字段应有注释 | 核心字段、枚举字段、金额字段、时间字段必须有注释 |
| 避免万能表   | 不建议用一张表承载多个不相关业务类型             |
| 避免字段堆积 | 字段过多时应考虑拆分扩展表、明细表或快照表       |
| 避免冗余失控 | 冗余字段必须明确来源和同步规则                   |
| 关注生命周期 | 核心表应考虑创建、变更、删除、归档等生命周期     |

基础建表示例：

```sql
-- 用户信息表：用于保存系统用户的基础身份信息
CREATE TABLE user_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    mobile VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    user_status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';
```

### 字段类型规范

字段类型应根据业务含义、数据范围、计算要求和查询方式选择。不要为了省事统一使用 `VARCHAR`，也不要为了避免溢出而无脑使用最大类型。

| 场景     | 推荐类型               | 说明                                 |
| -------- | ---------------------- | ------------------------------------ |
| 主键ID   | `BIGINT`               | 适合雪花ID、分布式ID或较大规模自增ID |
| 状态值   | `TINYINT`              | 适合少量枚举状态                     |
| 类型值   | `TINYINT` / `SMALLINT` | 根据枚举数量选择                     |
| 数量     | `INT` / `BIGINT`       | 根据业务上限选择                     |
| 金额     | `DECIMAL(18,2)`        | 避免浮点数精度问题                   |
| 比率     | `DECIMAL(10,4)`        | 适合费率、折扣率、比例               |
| 短文本   | `VARCHAR`              | 根据业务长度设置合理上限             |
| 长文本   | `TEXT`                 | 适合备注、详情、内容                 |
| 日期     | `DATE`                 | 只保存年月日                         |
| 时间     | `DATETIME`             | 适合保存业务时间                     |
| JSON扩展 | `JSON`                 | 适合低频、非核心扩展属性             |

金额字段推荐：

```sql
-- 推荐：金额字段使用 DECIMAL，避免 FLOAT / DOUBLE 精度问题
pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额'
```

状态字段推荐：

```sql
-- 推荐：状态字段使用 TINYINT，并在注释中说明枚举含义
order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成'
```

时间字段推荐：

```sql
-- 推荐：业务时间使用 DATETIME
pay_time DATETIME DEFAULT NULL COMMENT '支付时间'
```

### 字符集与存储引擎规范

MySQL 8 建议统一使用 `InnoDB` 存储引擎和 `utf8mb4` 字符集。`utf8mb4` 可以完整支持 Unicode 字符，包括中文、emoji 和特殊符号。

推荐建库语句：

```sql
-- 推荐：数据库层面统一字符集和排序规则
CREATE DATABASE mall_order
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

推荐建表语句：

```sql
-- 推荐：表级别显式指定存储引擎、字符集和表注释
CREATE TABLE product_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品信息表';
```

### 默认值规范

默认值用于保证数据写入时具备稳定行为。对于非空字段，应结合业务语义设置合理默认值；对于确实未知或可选字段，可以允许 `NULL`。

| 字段类型 | 默认值建议         | 示例                                                    |
| -------- | ------------------ | ------------------------------------------------------- |
| 状态字段 | 使用明确初始状态   | `DEFAULT 0`                                             |
| 数量字段 | 默认 `0`           | `DEFAULT 0`                                             |
| 金额字段 | 默认 `0.00`        | `DEFAULT 0.00`                                          |
| 删除标识 | 默认正常           | `DEFAULT 0`                                             |
| 创建时间 | 当前时间           | `DEFAULT CURRENT_TIMESTAMP`                             |
| 更新时间 | 当前时间并自动更新 | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` |
| 可选文本 | 允许 `NULL`        | `DEFAULT NULL`                                          |

示例：

```sql
-- 推荐：默认值表达明确业务含义
order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
```

### 注释规范

注释是数据库模型的重要组成部分，不应只依赖代码、接口文档或口头约定解释字段含义。

表注释应说明业务对象，字段注释应说明字段用途。枚举字段必须写明枚举值含义，金额字段应说明金额口径，时间字段应说明对应业务动作。

推荐示例：

```sql
-- 推荐：枚举值含义清晰
pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1支付成功，2支付失败，3已关闭',

-- 推荐：金额口径明确
pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',

-- 推荐：时间语义明确
pay_time DATETIME DEFAULT NULL COMMENT '支付成功时间'
```

不推荐示例：

```sql
-- 不推荐：注释过于模糊
status TINYINT NOT NULL DEFAULT 0 COMMENT '状态',
amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
time DATETIME DEFAULT NULL COMMENT '时间'
```

### 索引基础规范

索引应根据实际查询条件、排序方式和唯一性要求设计。不能因为字段经常出现就直接建索引，也不能忽略核心业务唯一约束。

| 场景             | 建议                     |
| ---------------- | ------------------------ |
| 业务唯一字段     | 建唯一索引               |
| 高频等值查询字段 | 可建普通索引             |
| 高频组合查询     | 建联合索引               |
| 分页排序查询     | 可将排序字段纳入联合索引 |
| 低区分度字段     | 不建议单独建索引         |
| 大文本字段       | 避免直接建立普通索引     |

示例：

```sql
-- 订单编号全局唯一
UNIQUE KEY uk_order_no (order_no),

-- 查询用户订单列表，并按创建时间分页
KEY idx_user_create_time (user_id, create_time),

-- 多租户场景下按租户、状态、创建时间查询
KEY idx_tenant_status_create_time (tenant_id, order_status, create_time)
```

低区分度字段如 `deleted`、`status` 不建议单独建索引，但可以结合业务主过滤条件组成联合索引。

```sql
-- 推荐：deleted 作为联合索引的一部分，而不是单独建索引
KEY idx_tenant_deleted_create_time (tenant_id, deleted, create_time)
```

### 字段顺序规范

字段顺序统一可以提升表结构可读性。建议将主键放在第一位，业务核心字段放在前面，审计字段和扩展字段放在后面。

| 顺序 | 字段类型                           |
| ---- | ---------------------------------- |
| 1    | 主键字段                           |
| 2    | 业务唯一标识                       |
| 3    | 业务关联字段                       |
| 4    | 业务核心属性                       |
| 5    | 状态、类型、金额、数量             |
| 6    | 业务时间字段                       |
| 7    | 扩展字段、备注字段                 |
| 8    | 软删除、版本、租户字段             |
| 9    | 创建人、更新人、创建时间、更新时间 |

示例：

```sql
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '订单备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_create_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

### 完整示例

下面给出一张符合基础建模规范的订单信息表，用于综合展示命名、字段类型、默认值、注释、主键、唯一索引、普通索引、字符集和存储引擎的使用方式。

```sql
-- 订单信息表：基础建模规范综合示例
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '订单备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_create_time (user_id, create_time),
    KEY idx_tenant_status_create_time (tenant_id, order_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单信息表';
```

### 检查清单

建表完成后，应使用检查清单确认模型是否符合基础规范。

| 检查项                     | 要求       |
| -------------------------- | ---------- |
| 表名是否使用小写下划线     | 必须       |
| 字段名是否使用小写下划线   | 必须       |
| 是否显式定义主键           | 必须       |
| 表是否有业务注释           | 必须       |
| 字段是否有清晰注释         | 必须       |
| 枚举字段是否说明枚举含义   | 必须       |
| 金额字段是否使用 `DECIMAL` | 必须       |
| 时间字段是否使用统一类型   | 必须       |
| 字符集是否使用 `utf8mb4`   | 必须       |
| 存储引擎是否使用 `InnoDB`  | 必须       |
| 业务唯一字段是否有唯一索引 | 必须       |
| 高频查询字段是否有合适索引 | 建议       |
| 是否避免无意义大字段       | 建议       |
| 是否避免过度索引           | 建议       |
| 是否保留必要基础字段       | 按业务决定 |

### 注意事项

建模基础规范是所有业务模型的通用约束，不负责展开主键生成、业务单号、基础字段等具体模型。这些内容应分别放入 `主键设计模型`、`业务单号模型`、`基础字段模型` 中单独说明。

在实际落地时，应优先保证核心业务表规范完整。对于简单配置表、临时导入表、统计中间表，可以适当简化，但不应破坏命名、注释、字段类型、字符集和存储引擎等基础一致性。



## 主键设计模型

主键设计模型用于规范 MySQL 8 业务表的主键选择、主键类型、主键生成方式和主键使用边界。主键是业务数据的唯一行标识，应保持稳定、唯一、不可变，并尽量避免承载复杂业务含义。

在业务系统中，主键不仅影响数据唯一性，也会影响索引结构、关联查询、分页性能、分库分表扩展、数据迁移和系统集成方式。因此，主键设计应在建模阶段明确统一规则，不建议不同表随意混用多种主键策略。

### 设计原则

主键应优先服务于数据库层面的唯一标识，而不是业务展示、业务编码或外部系统识别。业务编号、订单号、流水号、客户编码等应作为业务唯一字段单独设计，不应直接替代数据库主键。

| 原则       | 说明                                         |
| ---------- | -------------------------------------------- |
| 全局唯一   | 主键值在表内必须唯一，分布式场景下应避免冲突 |
| 稳定不可变 | 主键生成后不应因业务状态、业务编码变化而修改 |
| 无业务含义 | 推荐使用无业务含义的代理主键                 |
| 类型统一   | 核心业务表建议统一使用 `BIGINT`              |
| 查询友好   | 主键应适合索引查询、关联查询和分页定位       |
| 扩展友好   | 应考虑分库分表、数据迁移、异步同步等后续场景 |
| 避免过长   | 不建议使用过长字符串作为高频业务表主键       |

推荐将主键作为数据库内部行标识，将业务单号作为业务唯一标识。两者职责不同，不应混用。

### 主键类型选择

MySQL 业务建模中常见主键类型包括自增主键、雪花ID、UUID、业务自然主键和联合主键。不同类型适合的场景不同，核心业务表应优先选择稳定、可扩展、查询性能较好的方案。

| 主键类型          | 推荐程度 | 适用场景                   | 说明                      |
| ----------------- | -------- | -------------------------- | ------------------------- |
| `BIGINT` 自增ID   | 推荐     | 单库单表、简单后台系统     | 实现简单，写入局部递增    |
| `BIGINT` 雪花ID   | 推荐     | 分布式系统、微服务系统     | 全局唯一，适合分布式生成  |
| `CHAR(36)` UUID   | 谨慎使用 | 跨系统弱依赖唯一标识       | 字段长，索引成本高        |
| `BINARY(16)` UUID | 可选     | 对 UUID 有强需求且关注存储 | 比字符串 UUID 更紧凑      |
| 业务自然主键      | 不推荐   | 极少数稳定字典类数据       | 业务值变化会影响关联关系  |
| 联合主键          | 谨慎使用 | 关系表、明细表、强约束表   | 查询和 ORM 使用复杂度较高 |

核心业务表推荐使用 `BIGINT` 作为主键字段类型，并统一命名为 `id`。

```sql
-- 推荐：核心业务表使用 BIGINT 作为主键
id BIGINT NOT NULL COMMENT '主键ID'
```

不推荐在核心业务表中直接使用业务单号作为主键。

```sql
-- 不推荐：订单编号作为主键会混淆数据库标识和业务标识
order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
PRIMARY KEY (order_no)
```

推荐将业务单号设置为唯一索引。

```sql
-- 推荐：数据库主键和业务单号分离
id BIGINT NOT NULL COMMENT '主键ID',
order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
PRIMARY KEY (id),
UNIQUE KEY uk_order_no (order_no)
```

### 自增主键模型

自增主键模型适合单库单表、内部管理系统、数据规模可控且没有复杂分布式写入要求的场景。它由 MySQL 自动生成递增数字，使用简单，索引局部性较好。

自增主键示例：

```sql
-- 用户信息表：使用 MySQL 自增主键
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    user_status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0禁用，1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';
```

自增主键的优点是实现简单、查询高效、索引结构稳定。但在分布式系统中，自增主键可能暴露数据增长趋势，并且跨库合并、数据迁移、分库分表时容易产生冲突。

适用建议如下：

| 场景               | 是否适合 |
| ------------------ | -------- |
| 单体应用           | 适合     |
| 单库单表后台系统   | 适合     |
| 需要隐藏数据增长量 | 不太适合 |
| 多服务独立写入     | 不适合   |
| 未来可能分库分表   | 谨慎使用 |

### 雪花ID主键模型

雪花ID主键模型适合分布式业务系统。它通常由时间戳、机器标识、序列号等部分组成，可以在应用侧生成全局唯一的 `BIGINT` 主键。

雪花ID适合订单、支付、库存、账户流水、消息事件等核心业务表。与 UUID 相比，雪花ID使用 `BIGINT` 存储，索引空间更小，查询和关联成本更低。

雪花ID主键表示例：

```sql
-- 订单信息表：使用应用侧生成的雪花ID作为主键
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_create_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

雪花ID主键的字段不应设置 `AUTO_INCREMENT`，主键值由应用层、公共ID服务或基础框架生成。

```sql
-- 推荐：雪花ID由应用侧生成，数据库只负责存储
id BIGINT NOT NULL COMMENT '主键ID'
```

雪花ID适用建议如下：

| 场景                 | 是否适合 |
| -------------------- | -------- |
| 微服务系统           | 适合     |
| 分库分表系统         | 适合     |
| 高并发写入           | 适合     |
| 订单、支付、流水类表 | 适合     |
| 极简内部系统         | 可选     |
| 对主键连续性有强要求 | 不适合   |

需要注意，雪花ID通常趋势递增，但不保证严格连续。业务上不应依赖主键连续性判断数据是否缺失。

### UUID主键模型

UUID主键模型适合跨系统生成唯一标识、离线数据合并、多端本地生成数据等场景。但在 MySQL 高频业务表中，不建议直接使用 `CHAR(36)` UUID 作为主键。

字符串 UUID 较长，会增加主键索引、二级索引和关联字段的存储成本。由于 InnoDB 聚簇索引以主键组织数据，较长且随机的主键会影响写入局部性和索引维护成本。

不推荐示例：

```sql
-- 不推荐：CHAR(36) UUID 作为高频业务表主键，索引和存储成本较高
CREATE TABLE order_info (
    id CHAR(36) NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

如果业务确实需要 UUID，可以考虑将 UUID 作为外部唯一标识字段，而不是主键。

```sql
-- 推荐：内部主键使用 BIGINT，外部标识使用 UUID 字段
CREATE TABLE external_order_mapping (
    id BIGINT NOT NULL COMMENT '主键ID',
    external_id CHAR(36) NOT NULL COMMENT '外部系统唯一标识',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_external_id (external_id),
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部订单映射表';
```

UUID适用建议如下：

| 场景             | 是否适合 |
| ---------------- | -------- |
| 外部系统唯一标识 | 适合     |
| 离线数据合并     | 可选     |
| 低频配置类数据   | 可选     |
| 高频交易表主键   | 不推荐   |
| 大量关联表外键   | 不推荐   |

### 业务自然主键模型

业务自然主键是指使用业务字段作为主键，例如用户账号、商品编码、订单编号、组织编码等。该方式看似直观，但在业务系统中通常不推荐。

业务字段可能因规则调整、数据修正、系统迁移而发生变化。一旦业务字段作为主键，所有关联表都需要同步修改，维护成本和数据风险较高。

不推荐示例：

```sql
-- 不推荐：使用 username 作为主键，后续用户名变更会影响关联关系
CREATE TABLE user_info (
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';
```

推荐示例：

```sql
-- 推荐：使用无业务含义的 id 作为主键，username 作为唯一业务字段
CREATE TABLE user_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';
```

业务自然主键只适合极少数稳定字典类数据。例如行政区划编码、固定枚举编码等，但即使在这些场景中，也可以保留 `id` 主键，并将业务编码设置为唯一索引。

### 联合主键模型

联合主键是由多个字段共同组成的主键，常见于关系表或明细表。例如用户角色关系表可以使用 `user_id + role_id` 表示唯一关系。

联合主键可以强化数据唯一约束，但在应用开发、ORM映射、分页查询、外键引用和后续扩展方面会增加复杂度。因此，业务表不建议优先使用联合主键，关系表可以按场景选择。

联合主键示例：

```sql
-- 用户角色关系表：使用 user_id + role_id 作为联合主键
CREATE TABLE user_role_rel (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';
```

如果关系表后续可能增加审批状态、授权来源、版本、扩展字段、审计字段等复杂业务属性，建议使用单独 `id` 主键，并将关系字段设置为唯一索引。

```sql
-- 用户角色关系表：使用代理主键，并通过唯一索引保证关系唯一
CREATE TABLE user_role_rel (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    auth_source TINYINT NOT NULL DEFAULT 1 COMMENT '授权来源：1人工分配，2组织继承，3系统默认',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';
```

联合主键适用建议如下：

| 场景                   | 建议                    |
| ---------------------- | ----------------------- |
| 简单多对多关系表       | 可以使用联合主键        |
| 后续可能扩展业务属性   | 推荐代理主键 + 唯一索引 |
| 明细表需要独立定位记录 | 推荐代理主键            |
| ORM开发复杂度较高      | 谨慎使用联合主键        |
| 需要被其他表引用       | 推荐单字段主键          |

### 主键与业务单号分离

主键和业务单号应明确分离。主键用于数据库内部唯一标识，业务单号用于业务展示、用户查询、外部系统交互和业务流程流转。

以订单表为例，`id` 是数据库主键，`order_no` 是业务单号。两者都应唯一，但职责不同。

```sql
-- 推荐：主键和业务单号分离
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_create_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

主键和业务单号分离后，可以获得以下好处：

| 好处         | 说明                                   |
| ------------ | -------------------------------------- |
| 主键稳定     | 业务编号规则变化不影响主键             |
| 查询清晰     | 内部查询用 `id`，业务查询用 `order_no` |
| 扩展方便     | 可支持多种业务编号规则                 |
| 集成安全     | 外部系统不直接依赖内部主键             |
| 数据迁移友好 | 主键和业务编码可以独立处理             |

业务单号的详细规则应放入 `业务单号模型` 中展开，主键设计模型只负责说明二者分离原则。

### 主键字段规范

主键字段建议统一命名为 `id`，类型使用 `BIGINT`，并放在表结构第一列。主键注释统一使用 `主键ID`。

推荐写法：

```sql
id BIGINT NOT NULL COMMENT '主键ID',
PRIMARY KEY (id)
```

自增主键写法：

```sql
id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
PRIMARY KEY (id)
```

雪花ID主键写法：

```sql
id BIGINT NOT NULL COMMENT '主键ID',
PRIMARY KEY (id)
```

不推荐写法：

```sql
-- 不推荐：主键字段命名不统一
order_id BIGINT NOT NULL COMMENT '订单ID',
PRIMARY KEY (order_id)

-- 不推荐：使用无意义缩写
oid BIGINT NOT NULL COMMENT '订单ID',
PRIMARY KEY (oid)
```

如果表中存在多个关联ID，主键仍然使用 `id`，其他关联字段再使用具体业务对象名称。

```sql
-- 推荐：id 表示当前表主键，user_id 和 role_id 表示关联对象ID
id BIGINT NOT NULL COMMENT '主键ID',
user_id BIGINT NOT NULL COMMENT '用户ID',
role_id BIGINT NOT NULL COMMENT '角色ID'
```

### 主键索引与关联查询

MySQL InnoDB 表中，主键索引是聚簇索引，数据行按照主键组织存储。主键越短、越稳定，通常越有利于减少索引空间和关联查询成本。

在业务关联中，外键字段应与被关联表主键类型保持一致。例如用户表主键为 `BIGINT`，订单表中的 `user_id` 也应为 `BIGINT`。

```sql
-- 用户表
CREATE TABLE user_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 订单表：user_id 类型与 user_info.id 保持一致
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

是否使用数据库外键约束，应根据团队规范和业务系统情况决定。高并发互联网业务中，常见做法是不强制使用物理外键，而是在应用层保证数据一致性，同时保留字段命名和索引规范。

### 分库分表场景下的主键

分库分表场景下，不建议使用数据库自增主键作为全局主键。不同库表之间的自增值可能重复，数据合并、异步同步、离线分析和跨库定位都会变复杂。

分库分表场景推荐使用应用侧生成的全局唯一 `BIGINT` 主键，例如雪花ID。同时可以结合分片键设计索引。

```sql
-- 分库分表订单表：id 为全局唯一主键，user_id 可作为分片键或查询条件
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_create_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

需要注意，如果分片规则以 `user_id` 为分片键，而经常通过 `order_no` 查询订单，则需要在业务层具备路由能力，或设计订单号中包含可路由信息。该部分属于分库分表模型和业务单号模型的扩展内容。

### 完整示例

下面给出一张订单信息表，用于展示主键设计模型的推荐做法：使用 `BIGINT` 作为代理主键，业务单号单独设置唯一索引，关联字段类型与主键保持一致。

```sql
-- 订单信息表：主键设计模型综合示例
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '订单备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_create_time (user_id, create_time),
    KEY idx_tenant_status_create_time (tenant_id, order_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单信息表';
```

该表中，`id` 只负责数据库内部唯一标识，`order_no` 负责业务展示和业务查询，`user_id` 负责用户维度关联查询。主键、业务单号和关联字段职责清晰，便于后续扩展。

### 检查清单

设计主键时，可以使用以下清单确认是否符合主键设计规范。

| 检查项                                 | 要求 |
| -------------------------------------- | ---- |
| 主键字段是否统一命名为 `id`            | 建议 |
| 核心业务表主键是否使用 `BIGINT`        | 建议 |
| 主键是否无业务含义                     | 建议 |
| 主键生成后是否不可变                   | 必须 |
| 是否避免使用业务单号作为主键           | 建议 |
| 业务唯一字段是否使用唯一索引约束       | 必须 |
| 外键关联字段类型是否与主键一致         | 必须 |
| 分布式场景是否避免依赖自增ID全局唯一   | 必须 |
| 是否避免使用过长字符串作为高频表主键   | 建议 |
| 联合主键是否仅用于明确适合的关系表场景 | 建议 |

### 注意事项

主键设计应尽量保持简单、稳定和统一。对于大多数业务表，推荐使用 `BIGINT id` 作为代理主键，再通过唯一索引约束业务编号、业务编码或关系唯一性。

自增主键适合单库单表和简单系统；雪花ID适合分布式系统、微服务系统和分库分表场景；UUID适合作为外部唯一标识，不建议直接作为高频核心表主键；联合主键适合简单关系表，但如果关系本身会扩展业务属性，应优先考虑代理主键加唯一索引。



## 业务单号模型

业务单号模型用于规范 MySQL 8 业务系统中订单号、支付单号、退款单号、流水号、申请单号、工单号等业务编号的设计方式。业务单号是面向业务人员、用户、外部系统和运营查询的唯一标识，不应替代数据库主键。

业务单号通常具备可读性、可追踪性和业务识别能力，而数据库主键主要用于内部数据定位和表关系关联。两者职责不同，应在模型设计中明确分离。

### 设计原则

业务单号应保证唯一、稳定、可查询，并在必要时具备一定的业务识别能力。业务单号一旦生成，不应随着业务状态变化而修改。

| 原则       | 说明                                             |
| ---------- | ------------------------------------------------ |
| 全局唯一   | 同一业务类型下业务单号必须唯一                   |
| 稳定不可变 | 业务单号生成后不应修改                           |
| 可读可查   | 应便于业务人员、客服、运营和外部系统查询         |
| 不做主键   | 业务单号不应直接作为数据库主键                   |
| 长度可控   | 单号长度应稳定，不宜过长                         |
| 规则明确   | 单号组成规则应统一，避免不同模块随意拼接         |
| 并发安全   | 高并发生成时必须避免重复                         |
| 可扩展     | 应考虑多租户、多业务线、多系统接入和分库分表场景 |

推荐将数据库主键和业务单号分开设计：

```sql
-- 推荐：id 作为数据库主键，order_no 作为业务单号
id BIGINT NOT NULL COMMENT '主键ID',
order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
PRIMARY KEY (id),
UNIQUE KEY uk_order_no (order_no)
```

不推荐使用业务单号作为主键：

```sql
-- 不推荐：业务单号作为主键会增加关联成本，也会混淆主键职责
order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
PRIMARY KEY (order_no)
```

### 单号组成规范

业务单号通常由业务前缀、日期、序列号、随机位、校验位或租户标识等部分组成。具体组成应根据业务可读性、并发量、查询方式和安全要求选择。

常见组成如下：

| 组成部分 | 示例                | 说明                 |
| -------- | ------------------- | -------------------- |
| 业务前缀 | `ORD`、`PAY`、`REF` | 表示业务类型         |
| 日期部分 | `20260101`          | 便于按日期识别和排查 |
| 序列号   | `000001`            | 保证同一周期内递增   |
| 随机位   | `8392`              | 降低单号可枚举性     |
| 租户标识 | `T001`              | 多租户场景下可选     |
| 渠道标识 | `WX`、`ALI`、`API`  | 多渠道业务可选       |

推荐格式示例：

```text
ORD20260101000001
PAY20260101000001
REF20260101000001
```

如果需要降低单号可枚举性，可以增加随机位：

```text
ORD202601010000018392
PAY202601010000014725
```

如果是多租户系统，可以根据业务需要加入租户编码，但不建议让单号过长：

```text
T001ORD20260101000001
```

是否将租户标识、渠道标识放入单号，应根据查询和展示需求决定。若只是数据库过滤条件，优先使用独立字段，不一定需要放入业务单号。

### 常见业务单号类型

不同业务对象应使用不同前缀或不同生成规则，避免跨业务混淆。单号本身应能让使用者快速判断业务类型。

| 业务类型 | 字段名        | 前缀示例 | 单号示例            |
| -------- | ------------- | -------- | ------------------- |
| 订单     | `order_no`    | `ORD`    | `ORD20260101000001` |
| 支付单   | `payment_no`  | `PAY`    | `PAY20260101000001` |
| 退款单   | `refund_no`   | `REF`    | `REF20260101000001` |
| 出库单   | `outbound_no` | `OUT`    | `OUT20260101000001` |
| 入库单   | `inbound_no`  | `INB`    | `INB20260101000001` |
| 账户流水 | `flow_no`     | `FLW`    | `FLW20260101000001` |
| 工单     | `ticket_no`   | `TKT`    | `TKT20260101000001` |
| 审批单   | `approve_no`  | `APR`    | `APR20260101000001` |

字段命名应体现具体业务含义，不建议统一命名为 `no` 或 `code`。

```sql
-- 推荐：字段名明确表达业务含义
order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
refund_no VARCHAR(64) NOT NULL COMMENT '退款单号'
```

不推荐示例：

```sql
-- 不推荐：字段含义过于模糊
no VARCHAR(64) NOT NULL COMMENT '编号',
code VARCHAR(64) NOT NULL COMMENT '编码'
```

### 唯一约束规范

业务单号必须通过数据库唯一索引保证唯一性。应用层判断只能作为提前校验，不能替代数据库唯一约束。

订单表示例：

```sql
-- 订单信息表：order_no 作为业务唯一单号
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_create_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

支付单表示例：

```sql
-- 支付记录表：payment_no 作为支付单唯一编号
CREATE TABLE payment_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1支付成功，2支付失败',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';
```

如果业务要求同一租户内唯一，而不同租户可以重复，则可以使用联合唯一索引：

```sql
-- 多租户场景：业务单号在同一租户内唯一
UNIQUE KEY uk_tenant_order_no (tenant_id, order_no)
```

如果业务要求全局唯一，则直接对业务单号字段建立唯一索引：

```sql
-- 全局唯一业务单号
UNIQUE KEY uk_order_no (order_no)
```

### 日期序列号模型

日期序列号模型是常见业务单号模型，通常按天生成递增序列。它适合订单、支付、退款、出入库等业务单据。

常见格式如下：

```text
业务前缀 + yyyyMMdd + 固定位数序列号

示例：
ORD20260101000001
PAY20260101000001
REF20260101000001
```

该模型的优点是可读性强，便于通过单号识别业务类型和生成日期。缺点是并发生成时必须处理序列递增的原子性，否则容易出现重复单号。

可以通过业务单号序列表维护每日序列。

```sql
-- 业务单号序列表：用于维护不同业务类型、不同日期的当前序列值
CREATE TABLE business_no_sequence (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：ORDER订单，PAYMENT支付，REFUND退款',
    seq_date DATE NOT NULL COMMENT '序列日期',
    current_value BIGINT NOT NULL DEFAULT 0 COMMENT '当前序列值',
    step INT NOT NULL DEFAULT 1 COMMENT '递增步长',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_type_seq_date (biz_type, seq_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务单号序列表';
```

初始化当天订单序列：

```sql
-- 初始化订单业务在指定日期的序列
INSERT INTO business_no_sequence (
    biz_type,
    seq_date,
    current_value,
    step
) VALUES (
    'ORDER',
    '2026-01-01',
    0,
    1
);
```

并发生成序列时，可以使用 MySQL 的 `LAST_INSERT_ID()` 机制保证单行递增的原子性。

```sql
-- 原子递增当前序列值
UPDATE business_no_sequence
SET current_value = LAST_INSERT_ID(current_value + step)
WHERE biz_type = 'ORDER'
  AND seq_date = '2026-01-01';

-- 获取本次递增后的序列值
SELECT LAST_INSERT_ID() AS next_value;
```

该方式适合单库场景。如果是高并发分布式系统，应结合 Redis、自研号段服务、雪花算法或数据库号段模式处理。

### 号段模型

号段模型适合高并发单号生成场景。数据库不再每次生成一个序列值，而是一次分配一段号段给应用服务，应用服务在内存中递增使用。

号段模型可以减少数据库更新压力，但需要处理服务重启后的号段浪费问题。号段浪费通常可以接受，因为业务单号要求唯一，不要求连续。

号段表设计示例：

```sql
-- 业务单号号段表：用于按业务类型和日期分配号段
CREATE TABLE business_no_segment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：ORDER订单，PAYMENT支付，REFUND退款',
    seq_date DATE NOT NULL COMMENT '序列日期',
    current_max BIGINT NOT NULL DEFAULT 0 COMMENT '当前已分配最大值',
    step INT NOT NULL DEFAULT 1000 COMMENT '每次分配号段长度',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_type_seq_date (biz_type, seq_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务单号号段表';
```

分配号段时，可以先递增 `current_max`，再根据递增后的值计算号段范围。

```sql
-- 分配下一个号段
UPDATE business_no_segment
SET current_max = LAST_INSERT_ID(current_max + step)
WHERE biz_type = 'ORDER'
  AND seq_date = '2026-01-01';

-- 获取本次分配后的最大值
SELECT LAST_INSERT_ID() AS segment_max;
```

假设 `step = 1000`，本次返回 `segment_max = 5000`，则本次可使用的号段为：

```text
segment_start = 4001
segment_end   = 5000
```

号段模型适用建议如下：

| 场景                 | 是否适合 |
| -------------------- | -------- |
| 高并发订单生成       | 适合     |
| 多实例应用部署       | 适合     |
| 要求单号绝对连续     | 不适合   |
| 可接受少量号段浪费   | 适合     |
| 希望降低数据库写压力 | 适合     |

### 随机单号模型

随机单号模型通常由业务前缀、日期、随机数或随机字符串组成。它可以降低单号可枚举性，但如果随机空间不足，存在冲突概率。

随机单号示例：

```text
ORD2026010183927461
PAY2026010148261930
```

随机单号不应只依赖应用层判断唯一，仍然必须在数据库层建立唯一索引。如果发生唯一键冲突，应用层应重新生成并重试。

```sql
-- 随机业务单号仍然必须建立唯一索引
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

随机单号适合不希望暴露业务规模的场景，例如对外订单号、支付流水号、兑换码类编号等。但它不适合需要严格按单号判断生成顺序的场景。

### 雪花ID业务单号模型

部分系统会直接使用雪花ID或基于雪花ID转换后的字符串作为业务单号。该方式生成简单、全局唯一、性能高，适合分布式系统。

示例：

```text
ORD1893218750123456789
PAY1893218750123456790
```

雪花ID业务单号的优点是唯一性和性能较好，缺点是可读性一般，且单号较长。如果直接暴露完整雪花ID，可能暴露时间趋势或系统生成规律。

建表示例：

```sql
-- 使用雪花ID派生业务单号
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';
```

雪花ID业务单号适用建议如下：

| 场景               | 是否适合 |
| ------------------ | -------- |
| 分布式生成业务单号 | 适合     |
| 高并发写入         | 适合     |
| 对可读性要求较低   | 适合     |
| 对外展示需要短编号 | 不太适合 |
| 不希望暴露生成趋势 | 谨慎使用 |

### 单号长度规范

业务单号字段建议统一使用 `VARCHAR(64)`，可以兼容大多数业务单号规则。若业务单号长度明确较短，也可以设置为 `VARCHAR(32)`。

推荐字段定义：

```sql
-- 推荐：业务单号长度预留适度空间
order_no VARCHAR(64) NOT NULL COMMENT '订单编号'
```

如果单号规则固定，例如前缀 3 位、日期 8 位、序列 6 位，总长度固定为 17 位，也可以使用 `CHAR(17)`。

```sql
-- 可选：固定长度业务单号可以使用 CHAR
order_no CHAR(17) NOT NULL COMMENT '订单编号'
```

但在多数业务系统中，业务单号规则后续可能调整，例如增加租户、渠道、随机位或业务线标识，因此 `VARCHAR(64)` 更灵活。

### 单号与业务查询

业务单号通常用于精确查询，因此必须建立唯一索引或普通索引。核心业务单号建议直接建立唯一索引。

```sql
-- 推荐：业务单号用于精确查询，建立唯一索引
UNIQUE KEY uk_order_no (order_no)
```

如果业务查询经常带租户条件，应根据唯一性范围设计联合索引。

```sql
-- 同一租户内订单号唯一
UNIQUE KEY uk_tenant_order_no (tenant_id, order_no)
```

如果业务单号全局唯一，通常不需要再把 `tenant_id` 放入唯一索引，但可以根据查询条件增加普通联合索引。

```sql
-- order_no 全局唯一，同时支持租户维度列表查询
UNIQUE KEY uk_order_no (order_no),
KEY idx_tenant_create_time (tenant_id, create_time)
```

不建议对业务单号使用前缀模糊查询作为主要查询方式。

```sql
-- 不推荐作为核心查询方式：前缀模糊查询容易造成索引利用不充分
SELECT *
FROM order_info
WHERE order_no LIKE 'ORD202601%';
```

如果需要按日期查询，应使用独立的时间字段，例如 `create_time`、`pay_time`、`business_date`，不要依赖解析业务单号中的日期。

### 单号生成记录模型

对于资金、支付、退款、账户流水等关键业务，可以增加单号生成记录表，用于审计单号生成过程、排查重复生成、定位调用来源。

```sql
-- 业务单号生成记录表：用于记录关键业务单号的生成过程
CREATE TABLE business_no_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：ORDER订单，PAYMENT支付，REFUND退款',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务单号',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求ID',
    source_system VARCHAR(64) DEFAULT NULL COMMENT '来源系统',
    generate_status TINYINT NOT NULL DEFAULT 1 COMMENT '生成状态：1成功，2失败',
    fail_reason VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_no (biz_no),
    KEY idx_request_id (request_id),
    KEY idx_biz_type_create_time (biz_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务单号生成记录表';
```

该记录表不是所有业务都必须保留。对于高价值、强审计、强对账业务，建议保留生成记录；对于普通配置类、低价值业务，可以只保留目标业务表中的唯一单号字段。

### 幂等与防重设计

业务单号经常与幂等控制、防重复提交、外部请求追踪一起使用。对于外部系统请求，应保留外部请求号或幂等号，并建立唯一约束。

例如支付请求表可以同时保存内部支付单号和外部请求号：

```sql
-- 支付请求表：通过 request_no 防止外部重复请求
CREATE TABLE payment_request (
    id BIGINT NOT NULL COMMENT '主键ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    request_no VARCHAR(64) NOT NULL COMMENT '外部请求号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    request_status TINYINT NOT NULL DEFAULT 0 COMMENT '请求状态：0处理中，1成功，2失败',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    UNIQUE KEY uk_request_no (request_no),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表';
```

其中，`payment_no` 是系统内部支付单号，`request_no` 是外部调用方请求号。两者都可以唯一，但含义不同，不能混用。

### 完整示例

下面给出订单、支付、退款三个常见业务表的业务单号设计示例。每张表都使用 `id` 作为数据库主键，使用独立业务单号字段作为业务唯一标识。

```sql
-- 订单信息表：order_no 是订单业务单号
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_create_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';

-- 支付记录表：payment_no 是支付业务单号，order_no 关联订单
CREATE TABLE payment_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1支付成功，2支付失败',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- 退款记录表：refund_no 是退款业务单号，payment_no 关联支付单
CREATE TABLE refund_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    refund_no VARCHAR(64) NOT NULL COMMENT '退款单号',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    refund_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
    refund_status TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态：0待退款，1退款成功，2退款失败',
    refund_time DATETIME DEFAULT NULL COMMENT '退款时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_payment_no (payment_no),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';
```

该示例中，订单、支付、退款分别拥有独立业务单号。业务单号之间通过普通索引建立查询关系，但每张表仍然使用独立 `id` 作为数据库主键。

### 检查清单

设计业务单号时，可以使用以下清单确认是否符合规范。

| 检查项                         | 要求       |
| ------------------------------ | ---------- |
| 业务单号是否与数据库主键分离   | 必须       |
| 业务单号是否有明确业务含义     | 必须       |
| 字段名是否体现具体业务对象     | 必须       |
| 业务单号是否建立唯一索引       | 必须       |
| 单号生成后是否不可修改         | 必须       |
| 单号长度是否稳定且可扩展       | 建议       |
| 是否避免单号过长               | 建议       |
| 是否考虑高并发重复风险         | 必须       |
| 是否避免只依赖应用层判断唯一   | 必须       |
| 是否区分内部单号和外部请求号   | 建议       |
| 是否避免依赖解析单号做主要查询 | 建议       |
| 是否根据租户范围设计唯一约束   | 按业务决定 |

### 注意事项

业务单号是业务层面的唯一标识，不是数据库内部主键。建模时应坚持 `id` 和业务单号分离，避免后续因为编号规则变化、外部系统接入、数据迁移或分库分表导致主键和关联关系复杂化。

日期序列号模型适合可读性要求较高的业务；号段模型适合高并发生成；随机单号适合不希望暴露业务规模的场景；雪花ID派生单号适合分布式系统。无论采用哪种生成方式，最终都必须通过数据库唯一索引兜底保证唯一性。



## 基础字段模型

基础字段模型用于规范 MySQL 8 业务表中常见通用字段的设计方式，包括主键字段、创建时间、更新时间、创建人、更新人、删除标识、版本号、租户ID、备注字段等。基础字段不是所有表都必须完整包含，但核心业务表应尽量保持统一。

统一基础字段可以降低系统维护成本，使数据查询、审计追踪、软删除、乐观锁、多租户隔离和通用代码生成更加稳定。对于订单、支付、账户、库存、权限、组织等核心业务表，基础字段应作为建模时的默认组成部分。

### 设计原则

基础字段应服务于数据生命周期管理，而不仅是为了补齐表结构。字段是否保留，应根据业务表的重要性、变更频率、审计要求、数据隔离要求和并发控制要求决定。

| 原则     | 说明                                           |
| -------- | ---------------------------------------------- |
| 统一命名 | 同类字段在所有表中保持相同字段名               |
| 类型统一 | 同类字段尽量使用相同数据类型                   |
| 语义明确 | 字段注释应说明业务含义和取值规则               |
| 默认合理 | 状态、删除标识、版本号等字段应设置明确默认值   |
| 按需保留 | 简单关系表、临时表可适当简化                   |
| 便于扩展 | 核心业务表应预留审计、软删除、租户、版本等能力 |
| 避免滥用 | 不应为了统一而给所有表机械添加无用字段         |

基础字段推荐按固定顺序放在表结构后半部分，主键字段放在第一列，业务字段放在中间，审计字段和控制字段放在后面。

### 基础字段清单

常见基础字段如下：

| 字段名        | 类型           | 是否推荐 | 说明         |
| ------------- | -------------- | -------- | ------------ |
| `id`          | `BIGINT`       | 必须     | 主键ID       |
| `create_time` | `DATETIME`     | 必须     | 创建时间     |
| `update_time` | `DATETIME`     | 必须     | 更新时间     |
| `create_by`   | `BIGINT`       | 建议     | 创建人ID     |
| `update_by`   | `BIGINT`       | 建议     | 更新人ID     |
| `deleted`     | `TINYINT`      | 按需     | 删除标识     |
| `version`     | `INT`          | 按需     | 乐观锁版本号 |
| `tenant_id`   | `BIGINT`       | 按需     | 租户ID       |
| `remark`      | `VARCHAR(512)` | 按需     | 备注         |
| `sort_order`  | `INT`          | 按需     | 排序号       |

推荐的基础字段片段如下：

```sql
-- 基础字段片段：适合核心业务表复用
id BIGINT NOT NULL COMMENT '主键ID',
deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
version INT NOT NULL DEFAULT 0 COMMENT '版本号',
tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
```

### 主键字段

主键字段用于唯一标识表中的一行数据。基础字段模型中只规定主键字段的通用写法，具体主键生成方式应由主键设计模型展开。

推荐所有核心业务表统一使用 `id` 作为主键字段名，类型使用 `BIGINT`，注释使用 `主键ID`。

```sql
-- 推荐：统一主键字段命名和类型
id BIGINT NOT NULL COMMENT '主键ID',
PRIMARY KEY (id)
```

如果使用数据库自增主键，可以增加 `AUTO_INCREMENT`：

```sql
-- 自增主键写法：适合单库单表场景
id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
PRIMARY KEY (id)
```

如果使用应用侧生成的雪花ID，则不设置 `AUTO_INCREMENT`：

```sql
-- 雪花ID写法：适合分布式系统
id BIGINT NOT NULL COMMENT '主键ID',
PRIMARY KEY (id)
```

不推荐使用不统一的主键字段名：

```sql
-- 不推荐：主键字段命名不统一
order_id BIGINT NOT NULL COMMENT '订单ID',
user_pk BIGINT NOT NULL COMMENT '用户主键',
oid BIGINT NOT NULL COMMENT '订单ID'
```

### 创建时间与更新时间

创建时间和更新时间用于记录数据生命周期，是大多数业务表的必备字段。创建时间表示记录首次写入时间，更新时间表示记录最近一次变更时间。

推荐写法如下：

```sql
-- 推荐：创建时间和更新时间使用 DATETIME
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
```

对于核心业务表，`create_time` 和 `update_time` 建议设置为 `NOT NULL`。这样可以避免历史数据、导入数据或异常写入导致时间字段为空。

在高频查询场景中，`create_time` 常用于分页排序，可以结合业务字段建立联合索引：

```sql
-- 用户维度按创建时间分页查询
KEY idx_user_create_time (user_id, create_time)
```

多租户场景下，也可以结合 `tenant_id` 建立索引：

```sql
-- 租户维度按创建时间分页查询
KEY idx_tenant_create_time (tenant_id, create_time)
```

需要注意，`update_time` 会随着数据修改自动变化，不适合作为稳定业务时间。支付时间、取消时间、审核时间、生效时间等应单独建业务时间字段。

### 创建人与更新人

创建人和更新人用于记录数据由哪个用户创建、最后由哪个用户修改。它们通常保存用户主键ID，而不是用户名、昵称或手机号。

推荐写法如下：

```sql
-- 推荐：创建人和更新人保存用户ID
create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
update_by BIGINT DEFAULT NULL COMMENT '更新人ID'
```

不推荐直接保存用户名作为审计字段：

```sql
-- 不推荐：用户名可能变化，不适合作为稳定审计标识
create_by_name VARCHAR(64) DEFAULT NULL COMMENT '创建人名称',
update_by_name VARCHAR(64) DEFAULT NULL COMMENT '更新人名称'
```

如果业务需要展示创建人名称，可以在查询时关联用户表，也可以在特定快照场景中冗余保存名称。但基础字段中应优先保存稳定的用户ID。

在后台管理系统、权限系统、配置系统中，`create_by` 和 `update_by` 通常比较重要；在纯关系表、流水明细表、日志表中，可以根据写入来源决定是否保留。

### 删除标识

删除标识用于支持软删除。软删除并不会物理删除数据，而是通过字段标记数据是否有效。它适合需要保留历史数据、支持恢复、支持审计追踪的业务表。

推荐字段如下：

```sql
-- 推荐：软删除字段使用 TINYINT
deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除'
```

常见查询应过滤未删除数据：

```sql
-- 查询正常数据
SELECT id, order_no, user_id, order_status, create_time
FROM order_info
WHERE deleted = 0
  AND user_id = 10001
ORDER BY create_time DESC;
```

软删除更新示例：

```sql
-- 软删除数据
UPDATE order_info
SET deleted = 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1000001
  AND deleted = 0;
```

`deleted` 字段区分度通常较低，不建议单独建立索引。但在多租户、分页查询或高频过滤场景下，可以作为联合索引的一部分。

```sql
-- 推荐：deleted 作为联合索引的一部分
KEY idx_tenant_deleted_create_time (tenant_id, deleted, create_time)
```

不建议这样设计：

```sql
-- 不推荐：deleted 单独建索引通常收益较低
KEY idx_deleted (deleted)
```

### 版本号字段

版本号字段用于支持乐观锁控制，适合并发更新风险较高的业务表，例如库存、账户余额、订单状态、配置发布、审批状态等。

推荐字段如下：

```sql
-- 推荐：版本号字段用于乐观锁控制
version INT NOT NULL DEFAULT 0 COMMENT '版本号'
```

更新时应带上当前版本号作为条件，并在更新成功后递增版本号：

```sql
-- 乐观锁更新示例：只有版本号匹配时才允许更新
UPDATE inventory_stock
SET available_quantity = available_quantity - 1,
    version = version + 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 10001
  AND version = 5
  AND available_quantity > 0;
```

如果影响行数为 `0`，说明数据可能已被其他事务修改，应用层应重新查询后重试或返回并发失败提示。

版本号字段不一定需要所有表都保留。对于只新增不修改的流水表、日志表、事件表，通常不需要 `version`。对于会被多人或多流程并发修改的核心表，建议保留。

### 租户字段

租户字段用于支持多租户数据隔离。多租户系统中，业务表通常需要保存 `tenant_id`，用于区分不同租户的数据范围。

推荐字段如下：

```sql
-- 推荐：多租户表保留租户ID
tenant_id BIGINT NOT NULL COMMENT '租户ID'
```

如果系统中部分数据属于平台级公共数据，可以允许 `tenant_id` 为空，但需要明确业务规则：

```sql
-- 可选：允许为空表示平台公共数据
tenant_id BIGINT DEFAULT NULL COMMENT '租户ID，空表示平台公共数据'
```

多租户查询必须带租户条件：

```sql
-- 推荐：租户数据查询必须带 tenant_id
SELECT id, order_no, user_id, order_status, create_time
FROM order_info
WHERE tenant_id = 10001
  AND deleted = 0
ORDER BY create_time DESC;
```

常见多租户索引设计如下：

```sql
-- 租户维度列表查询
KEY idx_tenant_create_time (tenant_id, create_time)

-- 租户 + 状态 + 时间查询
KEY idx_tenant_status_create_time (tenant_id, order_status, create_time)

-- 租户 + 软删除 + 时间查询
KEY idx_tenant_deleted_create_time (tenant_id, deleted, create_time)
```

如果业务单号只要求租户内唯一，可以使用联合唯一索引：

```sql
-- 同一租户内订单编号唯一
UNIQUE KEY uk_tenant_order_no (tenant_id, order_no)
```

如果业务单号要求全局唯一，则应直接对业务单号建立唯一索引。

```sql
-- 订单编号全局唯一
UNIQUE KEY uk_order_no (order_no)
```

### 备注字段

备注字段用于保存人工补充说明、业务说明或操作说明。备注不是结构化业务字段，不应承载核心查询条件或重要业务状态。

推荐字段如下：

```sql
-- 推荐：普通备注字段
remark VARCHAR(512) DEFAULT NULL COMMENT '备注'
```

如果备注内容可能较长，可以使用 `TEXT`，但不建议在高频查询表中随意使用大字段。

```sql
-- 可选：较长备注内容
remark TEXT COMMENT '备注'
```

不推荐将业务状态、处理结果、审批结论等结构化数据全部塞入备注字段：

```sql
-- 不推荐：备注字段承载结构化业务状态
remark VARCHAR(512) DEFAULT NULL COMMENT '包含状态、原因、审批人、处理时间等信息'
```

如果某些信息具备明确业务含义，应拆成独立字段。例如失败原因、审核意见、取消原因可以单独设计。

```sql
-- 推荐：重要业务信息拆成独立字段
cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
audit_opinion VARCHAR(512) DEFAULT NULL COMMENT '审核意见',
fail_reason VARCHAR(512) DEFAULT NULL COMMENT '失败原因'
```

### 排序字段

排序字段适合字典、菜单、分类、配置项、展示规则等需要人工维护顺序的数据。排序字段通常不适合订单、流水、日志等天然按时间排序的数据。

推荐字段如下：

```sql
-- 推荐：排序字段用于人工维护展示顺序
sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号'
```

字典表中可以使用排序字段：

```sql
-- 字典项按排序号展示
SELECT id, dict_code, dict_label, sort_order
FROM dict_item
WHERE deleted = 0
ORDER BY sort_order ASC, id ASC;
```

如果排序字段参与高频查询，可以结合业务字段建立联合索引：

```sql
-- 按字典类型查询字典项并排序
KEY idx_dict_type_sort_order (dict_type, sort_order)
```

排序字段不建议使用 `sort` 作为字段名，因为它容易与 SQL 语义混淆。推荐使用 `sort_order`。

### 状态字段

状态字段用于表示业务对象当前所处阶段。虽然状态字段属于业务字段，但在多数业务表中非常常见，因此基础字段模型中也需要约定其基础风格。

推荐字段命名应体现业务对象，例如：

```sql
-- 推荐：字段名体现具体业务含义
order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1支付成功，2支付失败',
audit_status TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0待审核，1审核通过，2审核拒绝'
```

不推荐统一使用模糊字段名：

```sql
-- 不推荐：字段含义不明确
status TINYINT NOT NULL DEFAULT 0 COMMENT '状态'
```

状态字段必须在注释中写清楚枚举含义。对于复杂状态流转，应在状态机模型中单独展开，不应只依赖一个状态字段描述全部业务流程。

### 基础字段组合模板

不同类型表可以采用不同基础字段组合。并非所有表都需要完整字段集。

核心业务表推荐字段组合：

```sql
-- 核心业务表基础字段组合
id BIGINT NOT NULL COMMENT '主键ID',
deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
version INT NOT NULL DEFAULT 0 COMMENT '版本号',
tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
```

配置类表推荐字段组合：

```sql
-- 配置类表基础字段组合
id BIGINT NOT NULL COMMENT '主键ID',
config_status TINYINT NOT NULL DEFAULT 1 COMMENT '配置状态：0禁用，1启用',
sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
```

日志类表推荐字段组合：

```sql
-- 日志类表基础字段组合
id BIGINT NOT NULL COMMENT '主键ID',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
```

关系表推荐字段组合：

```sql
-- 简单关系表基础字段组合
id BIGINT NOT NULL COMMENT '主键ID',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
```

如果关系表需要审计授权来源、操作人或软删除，则可以增加更多基础字段。

```sql
-- 复杂关系表基础字段组合
id BIGINT NOT NULL COMMENT '主键ID',
deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
```

### 完整示例

下面给出一张订单信息表，用于展示基础字段模型在核心业务表中的完整使用方式。

```sql
-- 订单信息表：基础字段模型综合示例
CREATE TABLE order_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0正常，1删除',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    tenant_id BIGINT DEFAULT NULL COMMENT '租户ID',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_create_time (user_id, create_time),
    KEY idx_tenant_status_create_time (tenant_id, order_status, create_time),
    KEY idx_tenant_deleted_create_time (tenant_id, deleted, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单信息表';
```

该示例中，`id` 用于主键定位，`deleted` 用于软删除，`version` 用于乐观锁，`tenant_id` 用于租户隔离，`create_by` 和 `update_by` 用于人员审计，`create_time` 和 `update_time` 用于生命周期追踪。

### 检查清单

设计基础字段时，可以使用以下清单确认是否符合规范。

| 检查项                                          | 要求           |
| ----------------------------------------------- | -------------- |
| 主键字段是否统一命名为 `id`                     | 建议           |
| 是否保留 `create_time`                          | 核心业务表必须 |
| 是否保留 `update_time`                          | 核心业务表必须 |
| 时间字段是否使用 `DATETIME`                     | 建议           |
| 创建时间是否设置默认当前时间                    | 建议           |
| 更新时间是否设置自动更新时间                    | 建议           |
| 是否根据审计要求保留 `create_by` 和 `update_by` | 按业务决定     |
| 是否根据删除策略保留 `deleted`                  | 按业务决定     |
| 是否根据并发更新风险保留 `version`              | 按业务决定     |
| 多租户表是否保留 `tenant_id`                    | 必须           |
| 状态字段是否有明确枚举注释                      | 必须           |
| 备注字段是否避免承载结构化业务数据              | 必须           |
| 基础字段顺序是否统一                            | 建议           |
| 索引是否结合基础字段实际查询场景设计            | 建议           |

### 注意事项

基础字段模型的重点是统一通用字段风格，而不是要求所有表机械套用同一套字段。核心业务表应优先保留完整基础字段，简单关系表、日志表、临时表、导入表可以根据实际场景裁剪。

`create_time`、`update_time` 是数据生命周期字段；`create_by`、`update_by` 是人员审计字段；`deleted` 是逻辑删除字段；`version` 是并发控制字段；`tenant_id` 是数据隔离字段。每个字段都应有明确用途，避免字段存在但业务代码从不维护。