# 并发控制与防重模型

并发控制与防重模型用于解决业务系统中的并发修改、重复提交、重复创建、重复消费等问题。本章节属于 MySQL 8 常用业务建模模型中的并发控制与防重模型分类。

## 乐观锁模型

乐观锁模型适用于读多写少、并发冲突概率较低的业务场景。它不依赖长时间持有数据库锁，而是在写入时通过版本号、更新时间或状态条件判断数据是否仍然符合预期，从而避免并发覆盖。

### 适用场景

乐观锁适合业务允许重试、冲突概率可控、需要避免后提交覆盖先提交的场景。它的核心思想是：读取数据时记录版本，提交修改时校验版本，版本一致才允许更新。

常见适用场景包括：

| 场景         | 说明                                         |
| ------------ | -------------------------------------------- |
| 订单状态变更 | 防止多个操作同时修改订单状态，导致状态被覆盖 |
| 库存扣减     | 防止并发扣减时出现超卖或覆盖库存             |
| 账户余额更新 | 防止并发更新余额时丢失变更                   |
| 审批流处理   | 防止多人同时处理同一审批节点                 |
| 用户资料编辑 | 防止后保存的数据覆盖前一次修改               |
| 配置项修改   | 防止多个管理员同时修改配置造成覆盖           |

不适合使用乐观锁的场景：

| 场景             | 原因                                 |
| ---------------- | ------------------------------------ |
| 高冲突热点数据   | 大量写入会导致频繁失败和重试         |
| 强顺序写入       | 需要严格串行处理时更适合队列或悲观锁 |
| 长事务处理       | 版本读取和提交间隔过长，冲突概率较高 |
| 写入必须一次成功 | 乐观锁失败后需要业务侧处理重试或提示 |

### 建模结构

乐观锁模型通常在业务主表中增加 `version` 字段。每次更新时，只有当前版本号匹配才允许写入，并在同一次更新中将版本号递增。

下面以订单表为例，展示乐观锁模型的基础建表结构。该结构只描述字段和主键，不在建模结构中定义业务索引。

```sql
CREATE TABLE biz_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    order_status TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待支付，1已支付，2已取消，3已完成',
    pay_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT UNSIGNED NULL COMMENT '创建人',
    updated_by BIGINT UNSIGNED NULL COMMENT '更新人',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '订单表';
```

如果业务不希望使用整型版本号，也可以使用 `updated_at` 作为轻量版本字段。但在高并发写入中，推荐使用独立的 `version` 字段，因为它语义明确、更新条件稳定、不会受时间精度或自动更新时间影响。

### 字段设计

乐观锁字段设计的重点是版本字段必须稳定、单调递增，并且所有需要防止并发覆盖的写入都必须携带版本条件。

| 字段           | 类型              | 是否必填 | 默认值              | 说明         |
| -------------- | ----------------- | -------- | ------------------- | ------------ |
| `id`           | `BIGINT UNSIGNED` | 是       | 自增                | 主键ID       |
| `order_no`     | `VARCHAR(64)`     | 是       | 无                  | 业务订单号   |
| `order_status` | `TINYINT`         | 是       | `0`                 | 当前订单状态 |
| `pay_amount`   | `DECIMAL(18, 2)`  | 是       | `0.00`              | 支付金额     |
| `version`      | `INT UNSIGNED`    | 是       | `0`                 | 乐观锁版本号 |
| `created_at`   | `DATETIME`        | 是       | `CURRENT_TIMESTAMP` | 创建时间     |
| `updated_at`   | `DATETIME`        | 是       | `CURRENT_TIMESTAMP` | 更新时间     |

`version` 字段建议使用 `INT UNSIGNED`。多数业务系统中，单行数据更新次数很难达到无符号整型上限。如果业务存在极高频更新，可以使用 `BIGINT UNSIGNED`。

字段设计建议：

| 规则     | 建议                                            |
| -------- | ----------------------------------------------- |
| 初始版本 | 新增数据时设置为 `0` 或 `1`，同一系统内保持统一 |
| 更新方式 | 每次成功更新时执行 `version = version + 1`      |
| 更新条件 | `WHERE id = ? AND version = ?`                  |
| 失败判断 | 影响行数为 `0` 表示版本冲突或数据不存在         |
| 前端传参 | 修改类接口需要携带当前版本号                    |
| 返回结果 | 查询详情时返回版本号，供下一次提交使用          |

不建议将 `version` 设置为允许为空。空值会导致更新条件复杂化，也容易在历史数据迁移时产生不可预期的并发判断问题。

### 索引设计

乐观锁更新通常通过主键定位数据，再通过版本字段判断是否允许更新。因此，常见写法 `WHERE id = ? AND version = ?` 可以优先利用主键索引定位单行数据，版本判断在单行记录上完成即可。

常用索引设计如下：

```sql
ALTER TABLE biz_order
    ADD UNIQUE KEY uk_order_no (order_no);
```

如果业务中存在根据用户查询订单列表的场景，可以增加用户维度查询索引：

```sql
ALTER TABLE biz_order
    ADD KEY idx_user_status_created (user_id, order_status, created_at);
```

如果存在后台按状态分页处理订单的场景，可以增加状态维度查询索引：

```sql
ALTER TABLE biz_order
    ADD KEY idx_status_created (order_status, created_at);
```

通常不需要单独为 `version` 建索引。因为乐观锁更新一般先通过主键命中单行，再判断版本号，单独索引 `version` 选择性差，实际收益较低。

### 常用查询

常用查询需要保证能够读取当前版本号。业务侧提交修改时，必须使用查询结果中的 `version` 作为写入条件。

#### 查询单条数据并读取版本号

该查询用于编辑详情页、审批处理页、订单操作页等场景。读取数据时需要带出 `version`，后续提交修改时原样传回。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    version,
    remark,
    created_at,
    updated_at
FROM biz_order
WHERE id = ?;
```

业务处理要点：

| 要点               | 说明                             |
| ------------------ | -------------------------------- |
| 必须返回 `version` | 修改提交时需要使用               |
| 不要只查业务字段   | 否则无法进行乐观锁校验           |
| 查询后不要缓存过久 | 数据越旧，提交时版本冲突概率越高 |

#### 根据订单号查询并读取版本号

该查询适用于外部系统、支付回调、客服后台等通过业务单号定位订单的场景。

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    pay_amount,
    version,
    updated_at
FROM biz_order
WHERE order_no = ?;
```

订单号需要具备唯一性，避免一个业务单号命中多条数据。

#### 查询用户订单列表

列表查询通常用于展示，不一定用于直接更新。但如果列表页支持快捷操作，例如取消订单、确认收货，也需要返回 `version`。

```sql
SELECT
    id,
    order_no,
    order_status,
    pay_amount,
    version,
    created_at,
    updated_at
FROM biz_order
WHERE user_id = ?
  AND order_status = ?
ORDER BY created_at DESC
LIMIT ?, ?;
```

如果列表页只展示数据，不允许直接修改，可以不返回 `version`。如果列表页存在操作按钮，建议返回 `version`，减少再次查询。

#### 查询可操作状态的数据

该查询用于执行状态变更前的业务判断，例如只允许待支付订单取消，只允许已支付订单发货。

```sql
SELECT
    id,
    order_no,
    order_status,
    version
FROM biz_order
WHERE id = ?
  AND order_status IN (0, 1);
```

状态条件用于判断业务是否允许操作，版本字段用于后续写入时判断数据是否被其他事务修改。

### 常用写入

乐观锁写入的核心是：更新条件中带上旧版本号，更新内容中递增版本号。执行后需要检查影响行数，影响行数为 `1` 表示成功，影响行数为 `0` 表示失败。

#### 新增数据

新增数据时需要初始化版本号。建议所有表统一使用 `0` 作为初始版本。

```sql
INSERT INTO biz_order (
    order_no,
    user_id,
    order_status,
    pay_amount,
    version,
    remark,
    created_by,
    updated_by
) VALUES (
    ?,
    ?,
    0,
    ?,
    0,
    ?,
    ?,
    ?
);
```

新增操作本身通常不需要乐观锁，因为数据尚未被并发修改。但新增后如果要进入编辑、支付、审批等流程，后续更新必须使用版本号。

#### 根据版本号更新普通字段

该写入用于普通编辑场景，例如修改备注、金额、扩展信息等。

```sql
UPDATE biz_order
SET
    remark = ?,
    pay_amount = ?,
    version = version + 1,
    updated_by = ?,
    updated_at = CURRENT_TIMESTAMP
WHERE id = ?
  AND version = ?;
```

执行后需要判断影响行数：

| 影响行数 | 结果                                         |
| -------- | -------------------------------------------- |
| `1`      | 更新成功                                     |
| `0`      | 数据不存在、版本不一致或数据已被其他事务修改 |

#### 根据版本号更新订单状态

状态变更建议同时携带旧状态和旧版本号。旧状态用于保证业务流转正确，旧版本号用于防止并发覆盖。

```sql
UPDATE biz_order
SET
    order_status = ?,
    version = version + 1,
    updated_by = ?,
    updated_at = CURRENT_TIMESTAMP
WHERE id = ?
  AND order_status = ?
  AND version = ?;
```

这种写法可以同时解决两个问题：

| 条件               | 作用             |
| ------------------ | ---------------- |
| `order_status = ?` | 防止非法状态流转 |
| `version = ?`      | 防止并发修改覆盖 |

#### 库存扣减场景

乐观锁也常用于库存扣减。库存表可以通过版本号和库存余额条件共同控制并发。

```sql
UPDATE product_stock
SET
    available_stock = available_stock - ?,
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE product_id = ?
  AND available_stock >= ?
  AND version = ?;
```

库存扣减需要同时判断 `available_stock >= ?`，否则即使版本匹配，也可能出现库存不足时继续扣减的问题。

#### 使用更新时间作为版本条件

如果表中没有 `version` 字段，也可以使用 `updated_at` 作为版本判断条件。

```sql
UPDATE biz_order
SET
    remark = ?,
    updated_by = ?,
    updated_at = CURRENT_TIMESTAMP
WHERE id = ?
  AND updated_at = ?;
```

这种方式适合低并发、改造成本较低的历史表。新表仍建议优先使用独立 `version` 字段。

### 常见问题

乐观锁问题通常不是数据库语法问题，而是业务流程没有完整执行版本读取、版本提交、影响行数判断和失败处理。

| 问题                             | 原因                               | 处理方式                                     |
| -------------------------------- | ---------------------------------- | -------------------------------------------- |
| 更新一直失败                     | 前端传入的版本号过旧               | 重新查询最新数据后再提交                     |
| 更新成功但数据被覆盖             | 更新语句没有携带 `version` 条件    | 所有修改类 SQL 必须加版本条件                |
| 批量更新难处理                   | 多行数据版本不同                   | 按行更新并分别判断结果，或使用批处理返回明细 |
| 高并发下失败率高                 | 热点数据冲突严重                   | 改用悲观锁、队列串行化或拆分热点数据         |
| 状态被错误流转                   | 只判断版本，没有判断旧状态         | 状态更新同时判断旧状态和版本                 |
| 前端没有版本号                   | 查询接口未返回 `version`           | 修改类页面、详情接口需要返回版本号           |
| 使用 `updated_at` 冲突判断不稳定 | 时间精度、自动更新时间或中间件影响 | 使用独立 `version` 字段                      |

乐观锁失败不应该直接当作系统异常。更合理的处理方式是返回明确的业务提示，例如“数据已被其他用户修改，请刷新后重试”。

对于支付回调、消息消费、定时任务等自动化流程，乐观锁失败后可以根据业务规则进行有限次数重试。重试前应重新读取最新版本，不要使用旧版本号重复提交。

### 总结

乐观锁模型通过 `version` 字段在写入时校验数据是否被修改，适合读多写少、冲突概率较低、允许失败重试的业务场景。

建模时建议在核心业务表中增加独立的 `version` 字段，并在所有修改类 SQL 中使用 `WHERE id = ? AND version = ?` 或叠加业务状态条件。写入成功后递增版本号，写入失败后根据影响行数判断是否发生并发冲突。

乐观锁不是为了阻塞并发，而是为了发现并发冲突。它的关键不是字段本身，而是完整的业务闭环：查询返回版本号、提交携带版本号、更新递增版本号、失败明确处理。



## 悲观锁模型

悲观锁模型用于处理并发冲突概率较高、写入顺序敏感、必须串行处理的数据修改场景。本节属于“并发控制与防重模型”中的悲观锁部分，整体章节结构与首页大纲保持一致。

### 适用场景

悲观锁适合写多读少、并发修改冲突明显、业务不允许并发覆盖或失败重试成本较高的场景。它的核心思想是：在读取待修改数据时立即加锁，其他事务必须等待当前事务提交或回滚后才能继续操作。

常见适用场景包括：

| 场景           | 说明                                   |
| -------------- | -------------------------------------- |
| 库存扣减       | 防止多个事务同时扣减同一商品库存       |
| 账户余额变更   | 防止余额并发扣减、充值、冻结时出现错账 |
| 抢单、派单     | 防止同一任务被多个操作方同时领取       |
| 审批节点处理   | 防止同一审批节点被多人同时处理         |
| 订单状态强控制 | 防止支付、取消、发货等状态并发流转     |
| 队列任务消费   | 多个消费者并发拉取任务时避免重复处理   |

不适合使用悲观锁的场景：

| 场景           | 原因                                             |
| -------------- | ------------------------------------------------ |
| 长时间人工操作 | 锁持有时间过长，容易阻塞其他事务                 |
| 大范围批量扫描 | 可能锁住大量记录，影响系统吞吐                   |
| 低冲突读多写少 | 乐观锁成本更低                                   |
| 跨服务长流程   | 数据库事务不能覆盖完整链路，容易造成锁等待和超时 |

悲观锁依赖数据库事务。使用 MySQL 8 InnoDB 时，常见加锁语句包括 `SELECT ... FOR UPDATE`、`SELECT ... FOR SHARE`、`NOWAIT` 和 `SKIP LOCKED`。

### 建模结构

悲观锁模型本身不一定需要额外增加锁字段，它主要依赖 InnoDB 行锁和事务控制。建模时更关注业务主表是否具备明确的单行定位条件、状态字段和金额或数量字段。

下面以库存表为例，展示悲观锁模型的基础建表结构。该结构只描述字段和主键，不在建模结构中定义业务索引。

```sql
CREATE TABLE product_stock (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    sku_id BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    frozen_stock INT NOT NULL DEFAULT 0 COMMENT '冻结库存',
    sold_stock INT NOT NULL DEFAULT 0 COMMENT '已售库存',
    stock_status TINYINT NOT NULL DEFAULT 1 COMMENT '库存状态：0停用，1启用',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT UNSIGNED NULL COMMENT '创建人',
    updated_by BIGINT UNSIGNED NULL COMMENT '更新人',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '商品库存表';
```

如果是账户类业务，可以使用账户余额表作为悲观锁承载对象。账户余额、冻结金额、可用金额等字段需要在同一个事务内读取、校验和更新。

```sql
CREATE TABLE user_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    account_type TINYINT NOT NULL DEFAULT 1 COMMENT '账户类型：1余额账户，2积分账户',
    available_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '可用金额',
    frozen_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '总金额',
    account_status TINYINT NOT NULL DEFAULT 1 COMMENT '账户状态：0停用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户账户表';
```

悲观锁建模的重点不是增加一个“锁字段”，而是保证业务能够通过主键或唯一业务键精准定位需要锁定的行。

### 字段设计

悲观锁字段设计应围绕“可被精确锁定、可被明确校验、可被安全更新”展开。被锁定的数据行通常需要包含业务标识、状态字段、金额或数量字段以及基础审计字段。

库存表常用字段设计如下：

| 字段              | 类型              | 是否必填 | 默认值              | 说明     |
| ----------------- | ----------------- | -------- | ------------------- | -------- |
| `id`              | `BIGINT UNSIGNED` | 是       | 自增                | 主键ID   |
| `product_id`      | `BIGINT UNSIGNED` | 是       | 无                  | 商品ID   |
| `sku_id`          | `BIGINT UNSIGNED` | 是       | 无                  | SKU ID   |
| `available_stock` | `INT`             | 是       | `0`                 | 可用库存 |
| `frozen_stock`    | `INT`             | 是       | `0`                 | 冻结库存 |
| `sold_stock`      | `INT`             | 是       | `0`                 | 已售库存 |
| `stock_status`    | `TINYINT`         | 是       | `1`                 | 库存状态 |
| `created_at`      | `DATETIME`        | 是       | `CURRENT_TIMESTAMP` | 创建时间 |
| `updated_at`      | `DATETIME`        | 是       | `CURRENT_TIMESTAMP` | 更新时间 |

账户表常用字段设计如下：

| 字段               | 类型              | 是否必填 | 默认值              | 说明     |
| ------------------ | ----------------- | -------- | ------------------- | -------- |
| `id`               | `BIGINT UNSIGNED` | 是       | 自增                | 主键ID   |
| `user_id`          | `BIGINT UNSIGNED` | 是       | 无                  | 用户ID   |
| `account_type`     | `TINYINT`         | 是       | `1`                 | 账户类型 |
| `available_amount` | `DECIMAL(18, 2)`  | 是       | `0.00`              | 可用金额 |
| `frozen_amount`    | `DECIMAL(18, 2)`  | 是       | `0.00`              | 冻结金额 |
| `total_amount`     | `DECIMAL(18, 2)`  | 是       | `0.00`              | 总金额   |
| `account_status`   | `TINYINT`         | 是       | `1`                 | 账户状态 |
| `updated_at`       | `DATETIME`        | 是       | `CURRENT_TIMESTAMP` | 更新时间 |

字段设计建议：

| 规则         | 建议                                             |
| ------------ | ------------------------------------------------ |
| 精确定位     | 使用主键或唯一业务键定位锁定行                   |
| 状态校验     | 更新前检查状态是否允许操作                       |
| 数值校验     | 扣减库存、金额时必须判断余额是否充足             |
| 事务内处理   | 查询、校验、更新必须在同一个事务中完成           |
| 控制锁时长   | 不要在持锁事务中调用外部接口或执行耗时逻辑       |
| 明确失败原因 | 区分数据不存在、状态不可用、余额不足、锁等待超时 |

金额字段建议使用 `DECIMAL`，不要使用 `FLOAT` 或 `DOUBLE`。库存字段可以使用 `INT` 或 `BIGINT`，具体取决于业务规模。

### 索引设计

悲观锁依赖索引精确命中记录。加锁查询如果没有命中合适索引，可能导致扫描范围过大，从而扩大锁范围，增加锁等待、死锁和性能抖动风险。

库存表常用索引如下：

```sql
ALTER TABLE product_stock
    ADD UNIQUE KEY uk_sku_id (sku_id);
```

如果业务需要通过商品维度查询库存列表，可以增加商品维度索引：

```sql
ALTER TABLE product_stock
    ADD KEY idx_product_status (product_id, stock_status);
```

账户表常用索引如下：

```sql
ALTER TABLE user_account
    ADD UNIQUE KEY uk_user_account_type (user_id, account_type);
```

索引设计建议：

| 查询方式                                            | 推荐索引               | 说明                           |
| --------------------------------------------------- | ---------------------- | ------------------------------ |
| `WHERE id = ? FOR UPDATE`                           | 主键索引               | 最推荐，锁定范围最小           |
| `WHERE sku_id = ? FOR UPDATE`                       | `UNIQUE KEY uk_sku_id` | 适合库存按 SKU 锁定            |
| `WHERE user_id = ? AND account_type = ? FOR UPDATE` | 唯一联合索引           | 适合账户按用户和账户类型锁定   |
| `WHERE status = ? LIMIT ? FOR UPDATE`               | 状态和时间联合索引     | 适合任务领取，但需谨慎控制范围 |

悲观锁查询应尽量避免无索引条件、低选择性条件和大范围条件。尤其在默认隔离级别 `REPEATABLE READ` 下，范围查询可能引入间隙锁或临键锁，导致比预期更多的数据被阻塞。

### 常用查询

悲观锁查询必须在事务中执行。单独执行 `SELECT ... FOR UPDATE` 但不显式开启事务时，锁会在语句结束后很快释放，无法保护后续业务更新。

#### 根据主键加排他锁查询

该查询用于精确锁定一行数据，适合订单、账户、库存等核心业务对象。

```sql
START TRANSACTION;

SELECT
    id,
    product_id,
    sku_id,
    available_stock,
    frozen_stock,
    sold_stock,
    stock_status
FROM product_stock
WHERE id = ?
FOR UPDATE;

-- 在同一事务内完成业务校验和更新

COMMIT;
```

`FOR UPDATE` 会对查询命中的记录加排他锁。其他事务如果也要对同一行执行 `FOR UPDATE` 或更新操作，需要等待当前事务提交或回滚。

#### 根据唯一业务键加排他锁查询

该查询适用于通过业务唯一键定位数据的场景，例如根据 `sku_id` 锁定库存行。

```sql
START TRANSACTION;

SELECT
    id,
    product_id,
    sku_id,
    available_stock,
    frozen_stock,
    sold_stock,
    stock_status
FROM product_stock
WHERE sku_id = ?
FOR UPDATE;

-- 校验库存是否充足，并执行扣减

COMMIT;
```

该写法要求 `sku_id` 具备唯一索引。如果 `sku_id` 没有唯一索引，数据库可能扫描多行，锁范围会扩大。

#### 根据账户唯一键加排他锁查询

该查询适用于账户余额扣减、冻结、解冻、充值等场景。

```sql
START TRANSACTION;

SELECT
    id,
    user_id,
    account_type,
    available_amount,
    frozen_amount,
    total_amount,
    account_status
FROM user_account
WHERE user_id = ?
  AND account_type = ?
FOR UPDATE;

-- 校验账户状态和余额后执行金额变更

COMMIT;
```

账户类数据应尽量通过唯一键锁定单行，避免同一个用户同一种账户类型出现多条余额记录。

#### 使用共享锁查询

该查询适合需要防止数据被其他事务修改，但当前事务只读取不更新的场景。

```sql
START TRANSACTION;

SELECT
    id,
    user_id,
    account_type,
    available_amount,
    frozen_amount,
    total_amount,
    account_status
FROM user_account
WHERE user_id = ?
  AND account_type = ?
FOR SHARE;

-- 当前事务只读取和校验，不执行更新

COMMIT;
```

`FOR SHARE` 会加共享锁。其他事务可以读取，但不能对被锁定记录执行冲突更新。对于明确要修改数据的场景，应使用 `FOR UPDATE`。

#### 使用 NOWAIT 快速失败

该查询适合不希望等待锁释放的场景。如果目标行已被其他事务锁定，当前语句会立即失败。

```sql
START TRANSACTION;

SELECT
    id,
    sku_id,
    available_stock,
    stock_status
FROM product_stock
WHERE sku_id = ?
FOR UPDATE NOWAIT;

-- 成功获得锁后继续处理

COMMIT;
```

`NOWAIT` 适合接口需要快速响应的业务，例如后台人工操作、抢单按钮、避免用户长时间等待的管理端操作。

#### 使用 SKIP LOCKED 跳过已锁定数据

该查询适合多消费者并发领取任务。被其他事务锁定的记录会被跳过，当前事务只处理未被锁定的数据。

```sql
START TRANSACTION;

SELECT
    id,
    task_no,
    task_status,
    retry_count,
    created_at
FROM task_job
WHERE task_status = 0
ORDER BY created_at ASC
LIMIT 10
FOR UPDATE SKIP LOCKED;

-- 将本事务选中的任务更新为处理中

COMMIT;
```

`SKIP LOCKED` 适合任务队列、补偿任务、异步消费等场景。不适合要求严格顺序处理的业务，因为被锁定的数据会被跳过。

### 常用写入

悲观锁写入通常分为三步：开启事务、加锁读取、校验并更新。所有步骤必须在同一个事务中完成。

#### 库存扣减

库存扣减需要先锁定库存行，再判断可用库存是否充足，最后执行扣减。

```sql
START TRANSACTION;

SELECT
    id,
    sku_id,
    available_stock,
    frozen_stock,
    sold_stock,
    stock_status
FROM product_stock
WHERE sku_id = ?
FOR UPDATE;

UPDATE product_stock
SET
    available_stock = available_stock - ?,
    sold_stock = sold_stock + ?,
    updated_by = ?,
    updated_at = CURRENT_TIMESTAMP
WHERE sku_id = ?
  AND stock_status = 1
  AND available_stock >= ?;

COMMIT;
```

虽然已经通过 `FOR UPDATE` 锁定数据，`UPDATE` 中仍建议保留 `available_stock >= ?` 条件，避免业务代码遗漏校验时产生负库存。

#### 库存冻结

库存冻结常用于下单未支付场景。冻结时从可用库存转入冻结库存。

```sql
START TRANSACTION;

SELECT
    id,
    sku_id,
    available_stock,
    frozen_stock,
    stock_status
FROM product_stock
WHERE sku_id = ?
FOR UPDATE;

UPDATE product_stock
SET
    available_stock = available_stock - ?,
    frozen_stock = frozen_stock + ?,
    updated_by = ?,
    updated_at = CURRENT_TIMESTAMP
WHERE sku_id = ?
  AND stock_status = 1
  AND available_stock >= ?;

COMMIT;
```

库存冻结成功后，订单支付成功可以将冻结库存转为已售库存；订单取消或超时未支付时，应释放冻结库存。

#### 账户余额扣减

账户扣减需要先锁定账户行，再判断账户状态和可用金额。

```sql
START TRANSACTION;

SELECT
    id,
    user_id,
    account_type,
    available_amount,
    frozen_amount,
    total_amount,
    account_status
FROM user_account
WHERE user_id = ?
  AND account_type = ?
FOR UPDATE;

UPDATE user_account
SET
    available_amount = available_amount - ?,
    total_amount = total_amount - ?,
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = ?
  AND account_type = ?
  AND account_status = 1
  AND available_amount >= ?;

COMMIT;
```

账户扣减通常还需要配套账户流水表。余额表负责当前值，流水表负责记录每一次变更明细。

#### 任务领取

任务领取可以使用 `FOR UPDATE SKIP LOCKED` 支持多个消费者并发处理不同任务。

```sql
START TRANSACTION;

SELECT
    id,
    task_no,
    task_status,
    retry_count
FROM task_job
WHERE task_status = 0
ORDER BY created_at ASC
LIMIT 10
FOR UPDATE SKIP LOCKED;

UPDATE task_job
SET
    task_status = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (?, ?, ?);

COMMIT;
```

实际业务中，`UPDATE task_job WHERE id IN (...)` 的 ID 列表应来自当前事务中已经锁定的任务结果，不能重新查询一批数据。

#### 订单状态变更

订单状态变更可以先锁定订单，再根据当前状态执行安全流转。

```sql
START TRANSACTION;

SELECT
    id,
    order_no,
    order_status,
    pay_amount
FROM biz_order
WHERE order_no = ?
FOR UPDATE;

UPDATE biz_order
SET
    order_status = ?,
    updated_by = ?,
    updated_at = CURRENT_TIMESTAMP
WHERE order_no = ?
  AND order_status = ?;

COMMIT;
```

状态变更类写入仍建议保留旧状态条件，避免业务代码判断遗漏导致非法流转。

### 常见问题

悲观锁常见问题主要集中在事务边界、索引命中、锁等待、死锁和锁持有时间上。

| 问题                  | 原因                                         | 处理方式                                             |
| --------------------- | -------------------------------------------- | ---------------------------------------------------- |
| `FOR UPDATE` 没有效果 | 没有显式开启事务，或查询和更新不在同一事务中 | 使用 `START TRANSACTION`，并确保同连接执行           |
| 锁住的数据比预期多    | 查询条件没有命中索引或使用范围查询           | 优化索引，优先通过主键或唯一键锁定                   |
| 接口响应很慢          | 等待其他事务释放锁                           | 缩短事务时间，必要时使用 `NOWAIT`                    |
| 出现死锁              | 多个事务锁定资源顺序不一致                   | 统一加锁顺序，减少事务内操作                         |
| 库存仍然扣成负数      | 只加锁但更新时未保留库存条件                 | `UPDATE` 中保留 `available_stock >= ?`               |
| 任务重复领取          | 查询和更新不在同一事务中                     | 使用 `FOR UPDATE SKIP LOCKED` 并在同一事务内更新状态 |
| 锁等待超时            | 长事务、慢 SQL 或外部调用持锁                | 禁止在持锁事务中调用远程接口                         |

使用悲观锁时，需要特别注意事务中不要执行以下操作：

| 操作                 | 风险                             |
| -------------------- | -------------------------------- |
| 调用第三方接口       | 外部耗时不可控，导致锁长时间持有 |
| 等待用户输入         | 人工操作时间不可控               |
| 执行大批量统计       | 增加事务时长和锁等待             |
| 混合多个资源无序加锁 | 容易产生死锁                     |
| 加锁后再分页扫描     | 容易扩大锁范围                   |

悲观锁失败或超时时，应返回明确业务提示，例如“当前数据正在处理中，请稍后重试”。对于后台任务，可以记录失败原因并延迟重试。

### 总结

悲观锁模型通过数据库事务和行锁保证同一时间只有一个事务可以修改目标数据，适合库存、账户、任务领取、订单状态等高冲突或强一致场景。

建模时不一定需要增加专门的锁字段，关键是通过主键或唯一业务键精确定位记录，并确保加锁查询命中有效索引。常用写法是 `START TRANSACTION` 后执行 `SELECT ... FOR UPDATE`，随后在同一事务内完成业务校验和更新，最后提交或回滚事务。

悲观锁不是简单地给 SQL 加上 `FOR UPDATE`。它要求事务边界清晰、索引设计合理、锁顺序稳定、锁持有时间短。只有同时控制好这些条件，才能在保证数据一致性的同时避免严重的锁等待和死锁问题。



## 唯一约束防重模型

唯一约束防重模型用于从数据库层面保证业务数据不被重复创建。它通过 `UNIQUE KEY` 约束业务唯一标识，使重复写入在数据库层直接失败，从而避免应用层并发判断不可靠的问题。本节属于“并发控制与防重模型”中的唯一约束防重部分，整体章节结构与首页大纲保持一致。

### 适用场景

唯一约束防重适合业务上天然存在唯一标识的数据。它的核心思想是：只要业务语义要求“同一个业务键只能有一条记录”，就应该在数据库中建立唯一约束，而不是只依赖应用代码查询判断。

常见适用场景包括：

| 场景         | 唯一业务键                        | 说明                 |
| ------------ | --------------------------------- | -------------------- |
| 用户注册     | 手机号、邮箱、用户名              | 防止同一账号重复注册 |
| 订单创建     | 订单号                            | 防止重复生成同一订单 |
| 支付单创建   | 支付单号、商户订单号              | 防止重复创建支付请求 |
| 退款单创建   | 退款单号、原支付单号 + 退款请求号 | 防止重复退款         |
| 第三方回调   | 平台交易号、回调事件号            | 防止重复入库处理     |
| 优惠券领取   | 用户ID + 优惠券模板ID             | 防止同一用户重复领取 |
| 用户角色授权 | 用户ID + 角色ID                   | 防止重复授权         |
| 商品收藏     | 用户ID + 商品ID                   | 防止重复收藏         |
| 点赞记录     | 用户ID + 目标类型 + 目标ID        | 防止重复点赞         |

不适合只依赖唯一约束防重的场景：

| 场景                     | 原因                                           |
| ------------------------ | ---------------------------------------------- |
| 需要复杂状态判断         | 唯一约束只能判断唯一性，不能表达复杂业务流转   |
| 需要重复请求返回相同结果 | 更适合使用幂等模型                             |
| 需要串行修改已有数据     | 更适合使用乐观锁或悲观锁                       |
| 重复规则经常变化         | 唯一索引变更成本较高，需要谨慎设计             |
| 软删除后允许重新创建     | 需要额外设计唯一键规则，不能直接使用普通唯一键 |

唯一约束防重不是为了替代业务校验，而是提供最后一道数据一致性防线。应用层仍然可以提前校验，但最终是否重复应以数据库唯一约束为准。

### 建模结构

唯一约束防重模型通常围绕业务主表或关系表设计。表中需要包含业务唯一标识字段、业务主体字段、状态字段和基础审计字段。

下面以支付单表为例，展示唯一约束防重模型的基础建表结构。该结构只描述字段和主键，不在建模结构中定义业务唯一约束和普通索引。

```sql
CREATE TABLE pay_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    pay_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    out_trade_no VARCHAR(64) NOT NULL COMMENT '商户订单号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    pay_channel TINYINT NOT NULL COMMENT '支付渠道：1支付宝，2微信，3银行卡',
    channel_trade_no VARCHAR(128) NULL COMMENT '第三方支付平台交易号',
    pay_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '支付金额',
    pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付，1支付成功，2支付失败，3已关闭',
    paid_at DATETIME NULL COMMENT '支付成功时间',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT UNSIGNED NULL COMMENT '创建人',
    updated_by BIGINT UNSIGNED NULL COMMENT '更新人',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '支付单表';
```

如果是用户优惠券领取场景，可以使用关系表承载防重规则。每个用户对同一个优惠券模板只能领取一次。

```sql
CREATE TABLE user_coupon (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    coupon_template_id BIGINT UNSIGNED NOT NULL COMMENT '优惠券模板ID',
    coupon_no VARCHAR(64) NOT NULL COMMENT '优惠券编号',
    coupon_status TINYINT NOT NULL DEFAULT 0 COMMENT '优惠券状态：0未使用，1已使用，2已过期',
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    used_at DATETIME NULL COMMENT '使用时间',
    expired_at DATETIME NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户优惠券表';
```

如果是用户角色授权场景，关系表可以直接通过两个业务字段表达唯一关系。

```sql
CREATE TABLE user_role_rel (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    rel_status TINYINT NOT NULL DEFAULT 1 COMMENT '关系状态：0禁用，1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT UNSIGNED NULL COMMENT '创建人',
    updated_by BIGINT UNSIGNED NULL COMMENT '更新人',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户角色关系表';
```

建模结构中需要先明确“什么字段组合代表业务重复”。唯一约束不应该随意建立在展示字段或不稳定字段上，而应该建立在业务真正认可的唯一标识上。

### 字段设计

唯一约束防重的字段设计重点是业务唯一键的稳定性。唯一字段一旦进入线上业务，后续调整成本较高，因此建模时需要提前区分主键、业务单号、外部单号和联合唯一键。

支付单常用字段设计如下：

| 字段               | 类型              | 是否必填 | 默认值 | 说明                   |
| ------------------ | ----------------- | -------- | ------ | ---------------------- |
| `id`               | `BIGINT UNSIGNED` | 是       | 自增   | 数据库主键             |
| `pay_no`           | `VARCHAR(64)`     | 是       | 无     | 系统内部支付单号       |
| `out_trade_no`     | `VARCHAR(64)`     | 是       | 无     | 商户订单号或业务订单号 |
| `user_id`          | `BIGINT UNSIGNED` | 是       | 无     | 用户ID                 |
| `pay_channel`      | `TINYINT`         | 是       | 无     | 支付渠道               |
| `channel_trade_no` | `VARCHAR(128)`    | 否       | `NULL` | 第三方平台交易号       |
| `pay_amount`       | `DECIMAL(18, 2)`  | 是       | `0.00` | 支付金额               |
| `pay_status`       | `TINYINT`         | 是       | `0`    | 支付状态               |
| `paid_at`          | `DATETIME`        | 否       | `NULL` | 支付成功时间           |

唯一字段设计建议：

| 规则       | 建议                                                         |
| ---------- | ------------------------------------------------------------ |
| 内部单号   | 使用系统生成的稳定业务单号，例如 `pay_no`、`order_no`、`refund_no` |
| 外部单号   | 保存第三方平台唯一标识，例如 `channel_trade_no`、`event_id`  |
| 联合唯一键 | 用多个字段共同表达业务唯一性，例如 `user_id + role_id`       |
| 字段长度   | 给业务单号和外部单号预留足够长度，避免后续平台变更           |
| 字符集     | 建议使用 `utf8mb4`，业务单号比较敏感时可以考虑区分大小写的排序规则 |
| 空值处理   | 唯一字段尽量 `NOT NULL`，避免多个 `NULL` 绕过唯一约束语义    |
| 软删除处理 | 如果软删除后允许重建，需要把删除标记或有效状态纳入唯一规则   |

需要特别注意 MySQL 中唯一索引对 `NULL` 的处理。普通唯一索引允许多条记录在唯一字段上为 `NULL`。因此，如果字段参与防重，通常应设置为 `NOT NULL`。

### 索引设计

唯一约束防重的核心是唯一索引。唯一索引既是查询优化手段，也是数据一致性约束。它应该根据业务防重规则设计，而不是根据单次查询习惯随意添加。

支付单表常用唯一约束如下：

```sql
ALTER TABLE pay_order
    ADD UNIQUE KEY uk_pay_no (pay_no);
```

如果一个业务订单只允许创建一笔支付单，可以对商户订单号建立唯一约束：

```sql
ALTER TABLE pay_order
    ADD UNIQUE KEY uk_out_trade_no (out_trade_no);
```

如果同一个商户订单在不同支付渠道可以分别创建支付单，可以使用联合唯一约束：

```sql
ALTER TABLE pay_order
    ADD UNIQUE KEY uk_out_trade_channel (out_trade_no, pay_channel);
```

如果第三方交易号在支付成功后才返回，并且可能为空，不建议直接用普通唯一索引处理全部防重逻辑。可以根据业务情况将其设计为非空后唯一，或在回调落库表中使用独立事件编号防重。

优惠券领取防重常用唯一约束如下：

```sql
ALTER TABLE user_coupon
    ADD UNIQUE KEY uk_user_coupon_template (user_id, coupon_template_id);
```

用户角色关系防重常用唯一约束如下：

```sql
ALTER TABLE user_role_rel
    ADD UNIQUE KEY uk_user_role (user_id, role_id);
```

常用查询索引可以根据业务访问方式补充。支付单按用户和状态分页查询时，可以增加普通索引：

```sql
ALTER TABLE pay_order
    ADD KEY idx_user_status_created (user_id, pay_status, created_at);
```

支付单按状态和创建时间进行后台处理时，可以增加普通索引：

```sql
ALTER TABLE pay_order
    ADD KEY idx_status_created (pay_status, created_at);
```

索引设计建议：

| 类型             | 示例                                                         | 说明                         |
| ---------------- | ------------------------------------------------------------ | ---------------------------- |
| 单字段唯一索引   | `UNIQUE KEY uk_pay_no (pay_no)`                              | 适合业务单号全局唯一         |
| 联合唯一索引     | `UNIQUE KEY uk_user_role (user_id, role_id)`                 | 适合关系表防重               |
| 渠道维度唯一索引 | `UNIQUE KEY uk_out_trade_channel (out_trade_no, pay_channel)` | 适合不同渠道可重复的业务单号 |
| 普通查询索引     | `KEY idx_user_status_created (...)`                          | 用于列表查询，不承担防重语义 |

唯一索引字段顺序应按照业务唯一语义设计。对于联合唯一键，字段顺序不会改变唯一性判断结果，但会影响普通查询是否能利用最左前缀。

### 常用查询

常用查询需要围绕“写入前展示、写入后确认、重复冲突后回查、列表查询”展开。唯一约束可以防止重复写入，但业务侧通常仍需要查询已有记录并返回明确结果。

#### 根据内部业务单号查询

该查询用于根据系统内部单号定位唯一记录，例如订单号、支付单号、退款单号。

```sql
SELECT
    id,
    pay_no,
    out_trade_no,
    user_id,
    pay_channel,
    pay_amount,
    pay_status,
    paid_at,
    created_at,
    updated_at
FROM pay_order
WHERE pay_no = ?;
```

该查询应命中 `uk_pay_no`。业务上可以用于写入成功后的确认，也可以用于重复写入失败后的回查。

#### 根据商户订单号查询

该查询用于根据上游业务单号查询支付单，适合订单详情页、支付结果页、支付状态轮询等场景。

```sql
SELECT
    id,
    pay_no,
    out_trade_no,
    user_id,
    pay_channel,
    pay_amount,
    pay_status,
    paid_at,
    created_at,
    updated_at
FROM pay_order
WHERE out_trade_no = ?;
```

如果表中允许同一个商户订单在不同渠道创建支付单，则查询时应追加支付渠道条件。

```sql
SELECT
    id,
    pay_no,
    out_trade_no,
    user_id,
    pay_channel,
    pay_amount,
    pay_status,
    paid_at,
    created_at,
    updated_at
FROM pay_order
WHERE out_trade_no = ?
  AND pay_channel = ?;
```

#### 查询用户是否已领取优惠券

该查询用于发券前判断用户是否已经领取过同一优惠券模板。即使应用层先查，也仍然需要依赖唯一约束兜底。

```sql
SELECT
    id,
    user_id,
    coupon_template_id,
    coupon_no,
    coupon_status,
    received_at,
    used_at,
    expired_at
FROM user_coupon
WHERE user_id = ?
  AND coupon_template_id = ?;
```

该查询应命中 `uk_user_coupon_template`，通常只返回一条记录。

#### 查询用户角色是否已存在

该查询用于授权前判断关系是否已经存在，也可以用于重复授权失败后的回查。

```sql
SELECT
    id,
    user_id,
    role_id,
    rel_status,
    created_at,
    updated_at
FROM user_role_rel
WHERE user_id = ?
  AND role_id = ?;
```

对于授权类关系表，建议保留关系状态字段。重复授权时，如果记录存在但状态为禁用，可以选择重新启用，而不是插入新记录。

#### 查询用户支付单列表

该查询用于用户端或后台按用户维度查看支付记录。

```sql
SELECT
    id,
    pay_no,
    out_trade_no,
    pay_channel,
    pay_amount,
    pay_status,
    paid_at,
    created_at
FROM pay_order
WHERE user_id = ?
  AND pay_status = ?
ORDER BY created_at DESC
LIMIT ?, ?;
```

该查询不承担防重职责，但需要配合普通查询索引提升列表性能。

### 常用写入

唯一约束防重的写入方式主要有三类：直接插入、插入失败后回查、冲突时更新。不同写法适合不同业务语义，不能混用。

#### 直接插入并依赖唯一约束防重

该写法最简单，适合订单、支付单、退款单等创建类业务。应用层可以先生成业务单号，然后直接插入。

```sql
INSERT INTO pay_order (
    pay_no,
    out_trade_no,
    user_id,
    pay_channel,
    pay_amount,
    pay_status,
    remark,
    created_by,
    updated_by
) VALUES (
    ?,
    ?,
    ?,
    ?,
    ?,
    0,
    ?,
    ?,
    ?
);
```

如果插入成功，说明当前业务键未重复。如果插入失败并返回唯一键冲突，说明相同业务键已经存在。

#### 插入失败后回查已有记录

该写法适合重复请求需要给出明确响应的场景。写入失败后，根据唯一业务键查询已有记录，并返回已有数据。

```sql
SELECT
    id,
    pay_no,
    out_trade_no,
    user_id,
    pay_channel,
    pay_amount,
    pay_status,
    paid_at,
    created_at,
    updated_at
FROM pay_order
WHERE out_trade_no = ?
  AND pay_channel = ?;
```

业务处理流程如下：

| 步骤 | 处理                                     |
| ---- | ---------------------------------------- |
| 1    | 执行 `INSERT`                            |
| 2    | 插入成功，返回新记录                     |
| 3    | 捕获唯一键冲突                           |
| 4    | 根据唯一业务键查询已有记录               |
| 5    | 判断已有记录是否与本次请求参数一致       |
| 6    | 一致则返回已有记录，不一致则返回业务冲突 |

重复请求不一定等于合法请求。如果同一个 `out_trade_no` 第二次传入了不同金额或不同用户，需要返回参数冲突，而不是简单认为成功。

#### 使用 `INSERT IGNORE` 忽略重复插入

该写法适合关系表防重，例如用户收藏、点赞、用户角色授权等。重复插入时不会报错，但需要通过影响行数判断是否真正插入。

```sql
INSERT IGNORE INTO user_role_rel (
    user_id,
    role_id,
    rel_status,
    created_by,
    updated_by
) VALUES (
    ?,
    ?,
    1,
    ?,
    ?
);
```

影响行数判断：

| 影响行数 | 说明                             |
| -------- | -------------------------------- |
| `1`      | 插入成功                         |
| `0`      | 已存在相同唯一键记录，插入被忽略 |

`INSERT IGNORE` 会忽略部分错误，不只包括唯一键冲突。关键业务不建议滥用，应确保字段校验在应用层已经完成。

#### 使用 `ON DUPLICATE KEY UPDATE` 冲突时更新

该写法适合重复写入时需要刷新状态或更新时间的场景。例如用户角色关系已存在但被禁用时，重新授权可以将其启用。

```sql
INSERT INTO user_role_rel (
    user_id,
    role_id,
    rel_status,
    created_by,
    updated_by
) VALUES (
    ?,
    ?,
    1,
    ?,
    ?
)
ON DUPLICATE KEY UPDATE
    rel_status = VALUES(rel_status),
    updated_by = VALUES(updated_by),
    updated_at = CURRENT_TIMESTAMP;
```

该写法适合“存在则更新，不存在则插入”的关系类数据。对于支付单、订单、退款单等强业务单据，不建议默认使用该方式覆盖已有数据，否则可能掩盖重复请求参数不一致的问题。

#### 优惠券领取写入

优惠券领取通常使用联合唯一键防止同一用户重复领取同一模板。

```sql
INSERT INTO user_coupon (
    user_id,
    coupon_template_id,
    coupon_no,
    coupon_status,
    received_at,
    expired_at
) VALUES (
    ?,
    ?,
    ?,
    0,
    CURRENT_TIMESTAMP,
    ?
);
```

如果出现 `uk_user_coupon_template` 唯一键冲突，说明用户已经领取过该优惠券模板。业务侧可以返回“已领取”，也可以查询原优惠券返回给前端展示。

#### 第三方回调事件防重写入

第三方平台回调通常可能重复发送。可以单独建立回调事件表，以第三方事件号作为唯一键，先落库再处理。

```sql
CREATE TABLE pay_callback_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    event_no VARCHAR(128) NOT NULL COMMENT '第三方回调事件号',
    pay_channel TINYINT NOT NULL COMMENT '支付渠道：1支付宝，2微信，3银行卡',
    channel_trade_no VARCHAR(128) NOT NULL COMMENT '第三方支付平台交易号',
    callback_body JSON NOT NULL COMMENT '回调原始报文',
    process_status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理，1处理成功，2处理失败',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '支付回调事件表';
```

回调事件表的唯一约束放在索引设计中定义：

```sql
ALTER TABLE pay_callback_event
    ADD UNIQUE KEY uk_event_no (event_no);
```

回调写入示例：

```sql
INSERT INTO pay_callback_event (
    event_no,
    pay_channel,
    channel_trade_no,
    callback_body,
    process_status
) VALUES (
    ?,
    ?,
    ?,
    CAST(? AS JSON),
    0
);
```

如果事件号重复，说明该回调已经接收过。后续是否重复处理，应根据事件表中的处理状态决定。

### 常见问题

唯一约束防重常见问题主要来自唯一键选择不准确、空值处理不当、软删除冲突和错误使用冲突更新语句。

| 问题                                     | 原因                                           | 处理方式                                   |
| ---------------------------------------- | ---------------------------------------------- | ------------------------------------------ |
| 并发下仍然重复插入                       | 没有建立数据库唯一约束，只做了应用层查询判断   | 在数据库建立唯一索引                       |
| 唯一约束没有拦住重复数据                 | 唯一字段允许 `NULL`，多条 `NULL` 不冲突        | 防重字段设置为 `NOT NULL`                  |
| 软删除后无法重新创建                     | 唯一键仍然包含已删除数据                       | 将删除标记纳入唯一规则，或改用归档转移策略 |
| 重复请求返回异常                         | 只捕获数据库异常，没有回查已有记录             | 捕获唯一键冲突后按业务键查询               |
| 重复请求参数不一致也返回成功             | 只判断唯一键存在，没有比较核心参数             | 回查后校验金额、用户、渠道等关键字段       |
| `ON DUPLICATE KEY UPDATE` 覆盖了历史数据 | 冲突时无条件更新所有字段                       | 只更新允许变化的字段，单据类数据慎用       |
| 联合唯一键过长                           | 使用过多大字段参与唯一索引                     | 使用稳定短字段，必要时引入业务摘要字段     |
| 大小写重复判断不符合预期                 | 字符集排序规则大小写不敏感或敏感设置不符合业务 | 根据业务选择合适的 collation               |

软删除场景需要特别谨慎。假设用户表中 `mobile` 唯一，如果用户注销后允许重新注册，可以选择以下设计之一：

| 方案         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 不允许复用   | `mobile` 保持全局唯一，历史账号保留                          |
| 允许复用     | 软删除时迁移历史数据或清理唯一字段                           |
| 条件唯一语义 | 使用 `mobile + deleted_flag` 等组合约束，但要确认是否符合业务 |
| 归档历史数据 | 将删除数据转入归档表，主表释放唯一键                         |

在 MySQL 中没有直接的部分唯一索引语法，不能像部分数据库那样直接创建“仅未删除数据唯一”的索引。业务上如果需要这种能力，需要通过字段设计、归档策略或生成列等方式实现。

### 总结

唯一约束防重模型通过数据库唯一索引保证业务键不重复，是防止重复创建、重复授权、重复领取、重复回调入库的基础建模方式。

建模时需要先明确业务上的唯一语义，再选择单字段唯一键或联合唯一键。对于订单号、支付单号、退款单号等全局唯一标识，可以使用单字段唯一索引；对于用户角色、用户优惠券、点赞收藏等关系类数据，通常使用联合唯一索引。

唯一约束防重的关键不是“插入前先查一次”，而是“数据库必须有唯一约束”。应用层查询只能改善用户体验，不能作为并发防重的最终依据。真正可靠的处理方式是：插入时依赖唯一约束兜底，冲突后回查已有记录，并根据业务参数一致性决定返回成功、已存在或业务冲突。



## 幂等模型

幂等模型用于保证同一个业务请求被重复提交、重复调用或重复消费时，最终只产生一次有效业务结果。本节属于“并发控制与防重模型”中的幂等部分，整体章节结构与首页大纲保持一致。

### 适用场景

幂等模型适合请求可能被重复触发，但业务结果只能生效一次的场景。它的核心思想是：为每一次业务请求分配一个稳定的幂等键，首次请求正常处理，重复请求返回首次处理结果或明确的重复状态。

常见适用场景包括：

| 场景         | 幂等键                   | 说明                                 |
| ------------ | ------------------------ | ------------------------------------ |
| 订单创建     | 客户端请求号、业务流水号 | 防止用户重复点击导致重复下单         |
| 支付请求     | 商户订单号、支付请求号   | 防止重复创建支付单或重复发起支付     |
| 退款请求     | 退款请求号               | 防止同一笔退款重复提交               |
| 账户转账     | 交易流水号               | 防止重复扣款或重复入账               |
| 消息消费     | 消息ID、事件ID           | 防止 MQ 消息重复消费                 |
| 第三方回调   | 回调事件号、平台交易号   | 防止重复处理支付成功、退款成功等回调 |
| 表单提交     | 表单令牌、请求流水号     | 防止重复提交相同业务数据             |
| 定时任务补偿 | 任务批次号、业务对象ID   | 防止补偿任务重复执行                 |

不适合只使用幂等模型解决的场景：

| 场景                 | 原因                                       |
| -------------------- | ------------------------------------------ |
| 数据并发修改冲突     | 应优先使用乐观锁或悲观锁控制更新一致性     |
| 业务唯一性约束       | 应使用唯一约束防重作为数据库兜底           |
| 长流程强一致事务     | 幂等只能控制重复请求，不能替代分布式事务   |
| 无法生成稳定请求标识 | 幂等键不稳定会导致无法识别重复请求         |
| 请求参数允许变化     | 同一个幂等键下参数不一致时需要判定业务冲突 |

幂等模型通常不会单独存在。关键业务中常见组合是：幂等表识别重复请求，业务表唯一约束兜底防重，乐观锁或悲观锁控制并发修改。

### 建模结构

幂等模型通常使用独立的幂等记录表保存请求处理状态、请求摘要、响应结果和业务关联信息。业务请求进入系统后，先尝试写入幂等记录；写入成功表示首次请求，写入冲突表示重复请求。

下面给出通用幂等记录表的基础建表结构。该结构只描述字段和主键，不在建模结构中定义业务索引。

```sql
CREATE TABLE idempotent_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    idempotent_key VARCHAR(128) NOT NULL COMMENT '幂等键',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    business_no VARCHAR(128) NULL COMMENT '业务单号',
    request_hash CHAR(64) NOT NULL COMMENT '请求参数摘要',
    process_status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0处理中，1处理成功，2处理失败',
    response_code VARCHAR(64) NULL COMMENT '响应编码',
    response_message VARCHAR(500) NULL COMMENT '响应消息',
    response_body JSON NULL COMMENT '响应结果',
    expire_at DATETIME NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '幂等记录表';
```

如果业务希望记录更完整的请求来源，可以增加请求方、接口路径、请求方法等字段。

```sql
CREATE TABLE api_idempotent_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    idempotent_key VARCHAR(128) NOT NULL COMMENT '幂等键',
    app_id VARCHAR(64) NOT NULL COMMENT '调用方应用ID',
    api_path VARCHAR(255) NOT NULL COMMENT '接口路径',
    request_method VARCHAR(16) NOT NULL COMMENT '请求方法',
    request_hash CHAR(64) NOT NULL COMMENT '请求参数摘要',
    process_status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0处理中，1处理成功，2处理失败',
    response_body JSON NULL COMMENT '响应结果',
    expire_at DATETIME NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '接口幂等记录表';
```

如果是消息消费场景，可以使用消息消费记录表。它与接口幂等表的差异在于，幂等键通常来自消息ID、事件ID或业务事件编号。

```sql
CREATE TABLE message_consume_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(128) NOT NULL COMMENT '消息ID',
    topic VARCHAR(128) NOT NULL COMMENT '消息主题',
    consumer_group VARCHAR(128) NOT NULL COMMENT '消费者组',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    business_no VARCHAR(128) NULL COMMENT '业务单号',
    consume_status TINYINT NOT NULL DEFAULT 0 COMMENT '消费状态：0处理中，1消费成功，2消费失败',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '重试次数',
    error_message VARCHAR(1000) NULL COMMENT '错误信息',
    consumed_at DATETIME NULL COMMENT '消费成功时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '消息消费记录表';
```

幂等建模的重点是把“请求是否已经处理过”沉淀为数据库记录，而不是只依赖内存、缓存或短期 Token。缓存可以提升性能，但数据库记录更适合作为关键业务的最终依据。

### 字段设计

幂等字段设计需要同时满足三个目标：识别重复请求、判断请求是否一致、返回历史处理结果。

通用幂等记录字段设计如下：

| 字段               | 类型              | 是否必填 | 默认值              | 说明                           |
| ------------------ | ----------------- | -------- | ------------------- | ------------------------------ |
| `id`               | `BIGINT UNSIGNED` | 是       | 自增                | 主键ID                         |
| `idempotent_key`   | `VARCHAR(128)`    | 是       | 无                  | 幂等键                         |
| `business_type`    | `VARCHAR(64)`     | 是       | 无                  | 业务类型                       |
| `business_no`      | `VARCHAR(128)`    | 否       | `NULL`              | 业务单号                       |
| `request_hash`     | `CHAR(64)`        | 是       | 无                  | 请求参数摘要，通常使用 SHA-256 |
| `process_status`   | `TINYINT`         | 是       | `0`                 | 处理状态                       |
| `response_code`    | `VARCHAR(64)`     | 否       | `NULL`              | 业务响应编码                   |
| `response_message` | `VARCHAR(500)`    | 否       | `NULL`              | 业务响应消息                   |
| `response_body`    | `JSON`            | 否       | `NULL`              | 首次请求的响应结果             |
| `expire_at`        | `DATETIME`        | 否       | `NULL`              | 幂等记录过期时间               |
| `created_at`       | `DATETIME`        | 是       | `CURRENT_TIMESTAMP` | 创建时间                       |
| `updated_at`       | `DATETIME`        | 是       | `CURRENT_TIMESTAMP` | 更新时间                       |

幂等键字段设计建议：

| 字段             | 建议                                                     |
| ---------------- | -------------------------------------------------------- |
| `idempotent_key` | 由调用方生成，必须在重试时保持不变                       |
| `business_type`  | 区分不同业务场景，避免不同业务共用相同幂等键时误判       |
| `request_hash`   | 根据核心请求参数生成，用于判断同一个幂等键下参数是否一致 |
| `process_status` | 区分处理中、成功、失败，便于重复请求时返回不同结果       |
| `response_body`  | 保存首次成功响应，重复请求可以直接返回                   |
| `expire_at`      | 控制幂等记录保留周期，避免表无限增长                     |

`request_hash` 不建议直接保存完整请求参数作为唯一判断依据。完整请求可能包含敏感信息，也可能过大。更合理的方式是对规范化后的核心参数生成摘要。

请求摘要生成规则建议：

| 规则           | 说明                                           |
| -------------- | ---------------------------------------------- |
| 参数规范化     | 字段排序、去除无关字段、统一金额和时间格式     |
| 只包含核心字段 | 不包含时间戳、随机数、签名等每次都会变化的字段 |
| 使用稳定算法   | 推荐 SHA-256，字段长度可用 `CHAR(64)`          |
| 同业务统一规则 | 同一个 `business_type` 下摘要生成规则必须稳定  |
| 冲突时校验     | 幂等键相同但摘要不同，应返回参数冲突           |

处理状态建议如下：

| 状态值 | 状态     | 说明                                 |
| ------ | -------- | ------------------------------------ |
| `0`    | 处理中   | 首次请求已占位，但业务尚未完成       |
| `1`    | 处理成功 | 业务已完成，重复请求可返回首次结果   |
| `2`    | 处理失败 | 业务处理失败，是否允许重试由业务决定 |

如果业务需要支持失败后再次处理，可以增加 `lock_version`、`last_retry_at` 或 `retry_count` 字段，但不要把失败重试和重复请求简单混为一类。

### 索引设计

幂等模型的索引核心是唯一约束。唯一键必须能够表达“同一个调用方、同一个业务、同一个幂等键只能有一条记录”。

通用幂等记录表建议使用以下唯一约束：

```sql
ALTER TABLE idempotent_record
    ADD UNIQUE KEY uk_biz_idempotent_key (business_type, idempotent_key);
```

如果幂等键在全系统内已经保证唯一，也可以只对 `idempotent_key` 建唯一约束：

```sql
ALTER TABLE idempotent_record
    ADD UNIQUE KEY uk_idempotent_key (idempotent_key);
```

但在多业务、多调用方场景下，更推荐使用 `business_type + idempotent_key`，避免不同业务之间的幂等键碰撞。

接口幂等记录表建议按调用方、接口和幂等键建立联合唯一约束：

```sql
ALTER TABLE api_idempotent_record
    ADD UNIQUE KEY uk_app_api_idempotent (
        app_id,
        api_path,
        request_method,
        idempotent_key
    );
```

消息消费记录表建议按消费者组和消息ID建立联合唯一约束：

```sql
ALTER TABLE message_consume_record
    ADD UNIQUE KEY uk_group_message (
        consumer_group,
        message_id
    );
```

如果同一个消息会被多个消费者组分别消费，不能只对 `message_id` 建全局唯一约束，否则会阻止其他消费者组正常消费。

幂等记录常用普通查询索引如下：

```sql
ALTER TABLE idempotent_record
    ADD KEY idx_biz_no (business_type, business_no);
```

按状态和时间扫描异常记录时，可以增加如下索引：

```sql
ALTER TABLE idempotent_record
    ADD KEY idx_status_updated (process_status, updated_at);
```

按过期时间清理幂等记录时，可以增加如下索引：

```sql
ALTER TABLE idempotent_record
    ADD KEY idx_expire_at (expire_at);
```

索引设计建议：

| 类型             | 示例                                     | 说明                         |
| ---------------- | ---------------------------------------- | ---------------------------- |
| 幂等唯一索引     | `UNIQUE KEY uk_biz_idempotent_key (...)` | 防止重复请求重复占位         |
| 消息消费唯一索引 | `UNIQUE KEY uk_group_message (...)`      | 防止同一消费者组重复消费     |
| 业务单号索引     | `KEY idx_biz_no (...)`                   | 用于根据业务单号回查幂等记录 |
| 状态时间索引     | `KEY idx_status_updated (...)`           | 用于扫描处理中超时或失败记录 |
| 过期清理索引     | `KEY idx_expire_at (...)`                | 用于定期清理过期幂等数据     |

幂等键索引字段不宜过长。如果调用方传入的幂等键非常长，可以在表中额外保存 `idempotent_key_hash`，并对摘要字段建立唯一约束。

### 常用查询

常用查询需要围绕幂等记录的状态判断、重复请求回查、异常扫描和消息消费确认展开。幂等查询通常发生在业务写入之前或重复请求冲突之后。

#### 根据幂等键查询处理结果

该查询用于重复请求进入时，查询首次请求的处理状态和响应结果。

```sql
SELECT
    id,
    idempotent_key,
    business_type,
    business_no,
    request_hash,
    process_status,
    response_code,
    response_message,
    response_body,
    expire_at,
    created_at,
    updated_at
FROM idempotent_record
WHERE business_type = ?
  AND idempotent_key = ?;
```

业务处理建议：

| 状态     | 处理方式                                       |
| -------- | ---------------------------------------------- |
| 处理中   | 返回“处理中，请稍后重试”，或等待短时间后再查询 |
| 处理成功 | 校验请求摘要一致后返回历史响应                 |
| 处理失败 | 根据业务规则决定返回失败结果或允许重新发起     |
| 不存在   | 说明是首次请求，可以尝试插入幂等记录           |

#### 根据业务单号查询幂等记录

该查询用于排查同一个业务对象的请求处理情况，例如某个订单号对应的创建请求、支付请求或退款请求。

```sql
SELECT
    id,
    idempotent_key,
    business_type,
    business_no,
    process_status,
    response_code,
    response_message,
    created_at,
    updated_at
FROM idempotent_record
WHERE business_type = ?
  AND business_no = ?
ORDER BY created_at DESC;
```

该查询适合管理后台、问题排查和补偿任务使用，不应替代幂等键判断。

#### 查询处理中超时记录

该查询用于发现长时间处于处理中的幂等记录。处理中超时可能来自服务宕机、事务异常、外部接口超时或代码未正确更新状态。

```sql
SELECT
    id,
    idempotent_key,
    business_type,
    business_no,
    process_status,
    created_at,
    updated_at
FROM idempotent_record
WHERE process_status = 0
  AND updated_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE)
ORDER BY updated_at ASC
LIMIT 100;
```

处理中超时记录不能简单删除后重试。需要先根据业务表确认真实处理结果，再决定补偿、标记失败或恢复成功状态。

#### 查询失败记录

该查询用于后台补偿任务扫描失败请求。

```sql
SELECT
    id,
    idempotent_key,
    business_type,
    business_no,
    request_hash,
    response_code,
    response_message,
    updated_at
FROM idempotent_record
WHERE process_status = 2
  AND updated_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY updated_at ASC
LIMIT 100;
```

失败记录是否可以重试，需要由业务类型决定。例如网络调用失败可以重试，参数校验失败不应重试。

#### 查询消息消费记录

该查询用于判断某条消息是否已经被当前消费者组处理过。

```sql
SELECT
    id,
    message_id,
    topic,
    consumer_group,
    business_type,
    business_no,
    consume_status,
    retry_count,
    consumed_at,
    created_at,
    updated_at
FROM message_consume_record
WHERE consumer_group = ?
  AND message_id = ?;
```

如果消费状态为成功，当前消息应直接确认，不再重复执行业务逻辑。

### 常用写入

幂等写入通常分为四步：插入幂等占位记录、执行业务逻辑、更新幂等成功结果、重复请求回查结果。关键点是幂等占位和业务写入需要有清晰的事务边界。

#### 首次请求插入幂等占位

首次请求进入后，先插入幂等记录，状态设置为处理中。

```sql
INSERT INTO idempotent_record (
    idempotent_key,
    business_type,
    business_no,
    request_hash,
    process_status,
    expire_at
) VALUES (
    ?,
    ?,
    ?,
    ?,
    0,
    DATE_ADD(NOW(), INTERVAL 7 DAY)
);
```

如果插入成功，表示当前请求获得处理权。如果插入失败并发生唯一键冲突，表示该幂等键已经被使用，需要进入重复请求处理流程。

#### 业务处理成功后更新幂等结果

业务写入成功后，将幂等记录更新为处理成功，并保存响应结果。

```sql
UPDATE idempotent_record
SET
    process_status = 1,
    response_code = ?,
    response_message = ?,
    response_body = CAST(? AS JSON),
    updated_at = CURRENT_TIMESTAMP
WHERE business_type = ?
  AND idempotent_key = ?
  AND process_status = 0;
```

建议只允许从处理中更新为成功，避免重复请求或补偿逻辑误覆盖历史结果。

#### 业务处理失败后更新失败状态

业务处理失败时，需要记录失败原因。是否允许后续重试，由业务规则决定。

```sql
UPDATE idempotent_record
SET
    process_status = 2,
    response_code = ?,
    response_message = ?,
    updated_at = CURRENT_TIMESTAMP
WHERE business_type = ?
  AND idempotent_key = ?
  AND process_status = 0;
```

参数错误、余额不足、状态不允许等确定性失败可以直接记录为失败。外部接口超时、服务异常等不确定性失败，需要谨慎处理，避免实际业务已成功但幂等表记录为失败。

#### 重复请求回查并返回首次结果

当插入幂等占位发生唯一键冲突时，需要查询已有记录。

```sql
SELECT
    id,
    idempotent_key,
    business_type,
    business_no,
    request_hash,
    process_status,
    response_code,
    response_message,
    response_body,
    updated_at
FROM idempotent_record
WHERE business_type = ?
  AND idempotent_key = ?;
```

重复请求处理逻辑如下：

| 判断                            | 处理方式                                       |
| ------------------------------- | ---------------------------------------------- |
| `request_hash` 一致，状态成功   | 返回 `response_body`                           |
| `request_hash` 一致，状态处理中 | 返回处理中，或短暂等待后重查                   |
| `request_hash` 一致，状态失败   | 返回首次失败结果，或按业务规则允许重试         |
| `request_hash` 不一致           | 返回幂等键冲突，提示同一幂等键不能提交不同参数 |

幂等模型必须校验 `request_hash`。如果只判断幂等键，不判断请求摘要，就可能把不同业务请求错误地当成重复请求。

#### 使用幂等模型创建订单

订单创建场景中，建议先插入幂等占位，再创建订单，最后更新幂等结果。

```sql
START TRANSACTION;

INSERT INTO idempotent_record (
    idempotent_key,
    business_type,
    business_no,
    request_hash,
    process_status,
    expire_at
) VALUES (
    ?,
    'ORDER_CREATE',
    ?,
    ?,
    0,
    DATE_ADD(NOW(), INTERVAL 7 DAY)
);

INSERT INTO biz_order (
    order_no,
    user_id,
    order_status,
    pay_amount,
    version,
    remark,
    created_by,
    updated_by
) VALUES (
    ?,
    ?,
    0,
    ?,
    0,
    ?,
    ?,
    ?
);

UPDATE idempotent_record
SET
    process_status = 1,
    response_code = 'SUCCESS',
    response_message = '订单创建成功',
    response_body = JSON_OBJECT(
        'orderNo', ?,
        'orderStatus', 0
    ),
    updated_at = CURRENT_TIMESTAMP
WHERE business_type = 'ORDER_CREATE'
  AND idempotent_key = ?
  AND process_status = 0;

COMMIT;
```

如果事务中任意一步失败，应回滚事务。对于关键业务，订单表仍然需要对 `order_no` 建立唯一约束，避免幂等表异常时重复创建订单。

#### 使用幂等模型消费消息

消息消费场景中，先插入消费记录。插入成功后执行业务逻辑，插入冲突说明当前消费者组已经处理过或正在处理该消息。

```sql
START TRANSACTION;

INSERT INTO message_consume_record (
    message_id,
    topic,
    consumer_group,
    business_type,
    business_no,
    consume_status,
    retry_count
) VALUES (
    ?,
    ?,
    ?,
    ?,
    ?,
    0,
    0
);

-- 在这里执行业务写入，例如更新订单状态、生成流水、发送通知等

UPDATE message_consume_record
SET
    consume_status = 1,
    consumed_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE consumer_group = ?
  AND message_id = ?
  AND consume_status = 0;

COMMIT;
```

如果插入消费记录时出现唯一键冲突，消费者应查询已有消费状态。已成功则直接确认消息；处理中或失败则根据业务策略决定重试、延迟或告警。

#### 清理过期幂等记录

幂等记录不一定需要永久保留。对于接口请求幂等，可以根据业务保留周期定期清理。

```sql
DELETE FROM idempotent_record
WHERE expire_at IS NOT NULL
  AND expire_at < NOW()
LIMIT 1000;
```

清理任务建议分批执行，避免一次删除大量数据造成锁等待和主从延迟。对于支付、账户、退款等审计要求较高的业务，不建议过早删除幂等记录，可以转入归档表。

### 常见问题

幂等模型常见问题主要集中在幂等键不稳定、请求摘要不一致、处理中状态悬挂、事务边界错误和失败重试策略不清晰。

| 问题                   | 原因                               | 处理方式                            |
| ---------------------- | ---------------------------------- | ----------------------------------- |
| 重复请求仍然重复处理   | 没有唯一约束，或幂等键每次都不同   | 使用稳定幂等键并建立唯一索引        |
| 不同请求被当成同一请求 | 只判断幂等键，没有校验请求摘要     | 使用 `request_hash` 校验核心参数    |
| 一直返回处理中         | 服务异常后没有更新幂等状态         | 扫描处理中超时记录并补偿            |
| 重复请求返回不同结果   | 没有保存首次响应，重复请求重新计算 | 保存 `response_body` 并返回首次结果 |
| 幂等记录成功但业务失败 | 幂等表和业务表不在同一事务内       | 关键业务建议同库同事务处理          |
| 业务成功但幂等记录失败 | 更新幂等结果失败或事务边界不一致   | 根据业务表反查并修复幂等状态        |
| 失败请求无法重试       | 失败状态没有区分可重试和不可重试   | 增加错误码或重试状态字段            |
| 幂等表数据量过大       | 没有过期时间和清理策略             | 增加 `expire_at` 并定期归档或清理   |
| 消息重复消费           | 只依赖 MQ 投递语义，没有消费记录   | 使用消息ID和消费者组建立唯一约束    |

处理中悬挂是幂等模型中最常见的问题。例如请求插入幂等记录后服务宕机，记录会停留在处理中。处理方式不能简单删除，因为业务可能已经部分完成。正确流程是先根据业务单号查询业务表，再修复幂等状态。

处理建议如下：

| 业务表状态       | 幂等记录处理                         |
| ---------------- | ------------------------------------ |
| 业务已成功       | 将幂等记录修复为成功，并补充响应结果 |
| 业务未创建       | 将幂等记录标记为失败或允许重新处理   |
| 业务状态不确定   | 进入人工核查或补偿任务               |
| 外部系统状态未知 | 先查询外部系统结果，再决定本地状态   |

幂等模型也不能替代唯一约束。订单创建时即使已经有幂等表，订单号仍然应建立唯一约束。幂等表防止重复请求，业务表唯一约束防止重复数据，二者职责不同。

### 总结

幂等模型通过幂等键、请求摘要、处理状态和响应结果记录，保证重复请求不会重复执行业务结果。它适用于订单创建、支付退款、账户交易、消息消费、第三方回调等容易发生重复提交或重复投递的场景。

建模时建议使用独立幂等记录表，并对 `business_type + idempotent_key` 建立唯一约束。首次请求插入处理中记录，处理成功后保存响应结果；重复请求命中同一幂等键时，先校验请求摘要，再根据处理状态返回历史结果、处理中提示或失败结果。

幂等模型的关键不是简单地保存一个 Token，而是形成完整闭环：稳定生成幂等键、唯一约束抢占处理权、请求摘要防止参数污染、状态字段表达处理进度、响应结果支持重复返回、超时记录支持补偿修复。与唯一约束、乐观锁、悲观锁组合使用时，才能覆盖重复提交、并发修改和业务防重等完整问题。