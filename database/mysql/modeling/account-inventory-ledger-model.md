# 账户、库存与流水模型



## 账户流水模型

账户流水模型用于记录账户余额的每一次变动。账户表保存当前余额状态，账户流水表保存余额变动明细。业务系统中应优先保证流水不可随意修改、写入具备幂等性、余额变动与流水写入在同一个事务中完成。

### 适用场景

账户流水模型适用于需要记录资金、积分、余额、额度、保证金、钱包、会员储值、平台账户等余额变动的业务场景。

典型场景包括用户钱包充值与消费、会员积分增加与扣减、商户账户入账与出账、平台佣金结算、冻结金额与解冻金额、账户调账、退款退回、活动赠送额度、保证金扣除等。

该模型适合处理“当前余额 + 变动明细”的业务，不适合只关心最终状态、不需要追溯变更过程的简单配置类数据。只要业务需要审计、对账、追踪余额来源，通常都应该建立账户流水表。

### 建模结构

账户流水模型通常由账户表和账户流水表组成。账户表保存账户当前状态，账户流水表保存每一次账户金额变动记录。

账户表负责回答“当前账户余额是多少”，账户流水表负责回答“余额为什么变成这样”。两张表需要通过账户主键建立业务关联，但账户流水表不应该依赖账户表余额反推历史余额，而应在流水中保存本次变动后的余额快照。

常见建模结构如下：

```text
biz_account
  ├── id                  账户主键
  ├── account_no          账户编号
  ├── owner_id            账户归属对象ID
  ├── owner_type          账户归属对象类型
  ├── available_amount    可用余额
  ├── frozen_amount       冻结余额
  ├── total_amount        总余额
  ├── account_status      账户状态
  └── version             乐观锁版本号

biz_account_flow
  ├── id                  流水主键
  ├── flow_no             流水编号
  ├── account_id          账户ID
  ├── account_no          账户编号
  ├── request_no          幂等请求号
  ├── biz_type            业务类型
  ├── biz_no              业务单号
  ├── change_type         变动类型
  ├── change_amount       变动金额
  ├── balance_before      变动前可用余额
  ├── balance_after       变动后可用余额
  ├── frozen_before       变动前冻结余额
  ├── frozen_after        变动后冻结余额
  └── remark              备注
```

账户表保存当前余额，账户流水表只追加写入。正常情况下，账户流水不做物理删除，不直接更新核心金额字段。如果确实发生业务冲正，应通过新增一条反向流水完成，而不是修改原流水。

### 字段设计

字段设计需要重点关注金额精度、幂等标识、业务来源、余额快照和审计字段。金额字段建议使用最小货币单位保存，例如分、厘、积分最小单位，字段类型使用 `BIGINT`，避免使用浮点类型。

账户表示例：

```sql
CREATE TABLE biz_account (
    id BIGINT NOT NULL COMMENT '主键ID',
    account_no VARCHAR(64) NOT NULL COMMENT '账户编号',
    owner_id BIGINT NOT NULL COMMENT '账户归属对象ID，如用户ID、商户ID、平台主体ID',
    owner_type VARCHAR(32) NOT NULL COMMENT '账户归属对象类型：USER、MERCHANT、PLATFORM',
    currency_code VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '币种或计量单位，如CNY、POINT',
    available_amount BIGINT NOT NULL DEFAULT 0 COMMENT '可用余额，使用最小单位，如分',
    frozen_amount BIGINT NOT NULL DEFAULT 0 COMMENT '冻结余额，使用最小单位，如分',
    total_amount BIGINT NOT NULL DEFAULT 0 COMMENT '总余额，一般等于可用余额 + 冻结余额',
    account_status TINYINT NOT NULL DEFAULT 1 COMMENT '账户状态：1正常，2冻结，9注销',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';
```

账户流水表示例：

```sql
CREATE TABLE biz_account_flow (
    id BIGINT NOT NULL COMMENT '主键ID',
    flow_no VARCHAR(64) NOT NULL COMMENT '流水编号',
    account_id BIGINT NOT NULL COMMENT '账户ID',
    account_no VARCHAR(64) NOT NULL COMMENT '账户编号',
    request_no VARCHAR(128) NOT NULL COMMENT '幂等请求号，同一业务请求必须唯一',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：RECHARGE、CONSUME、REFUND、FREEZE、UNFREEZE、ADJUST',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务单号，如订单号、支付单号、退款单号',
    change_type VARCHAR(32) NOT NULL COMMENT '变动类型：IN、OUT、FREEZE、UNFREEZE',
    change_amount BIGINT NOT NULL COMMENT '变动金额，正数表示增加，负数表示减少',
    balance_before BIGINT NOT NULL COMMENT '变动前可用余额',
    balance_after BIGINT NOT NULL COMMENT '变动后可用余额',
    frozen_before BIGINT NOT NULL COMMENT '变动前冻结余额',
    frozen_after BIGINT NOT NULL COMMENT '变动后冻结余额',
    currency_code VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '币种或计量单位',
    flow_status TINYINT NOT NULL DEFAULT 1 COMMENT '流水状态：1有效，2已冲正',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户流水表';
```

核心字段说明：

| 字段               | 所属表             | 设计说明                                 |
| ------------------ | ------------------ | ---------------------------------------- |
| `account_no`       | 账户表、账户流水表 | 账户业务编号，便于外部系统识别账户       |
| `owner_id`         | 账户表             | 账户归属对象ID，例如用户ID、商户ID       |
| `owner_type`       | 账户表             | 区分不同账户主体类型                     |
| `available_amount` | 账户表             | 当前可用余额                             |
| `frozen_amount`    | 账户表             | 当前冻结余额                             |
| `total_amount`     | 账户表             | 当前总余额，通常等于可用余额加冻结余额   |
| `flow_no`          | 账户流水表         | 流水编号，全局唯一                       |
| `request_no`       | 账户流水表         | 幂等请求号，防止重复入账或重复扣款       |
| `biz_type`         | 账户流水表         | 标识业务来源类型                         |
| `biz_no`           | 账户流水表         | 关联业务单号，用于对账和追踪             |
| `change_amount`    | 账户流水表         | 本次变动金额，建议入账为正数，出账为负数 |
| `balance_before`   | 账户流水表         | 变动前可用余额快照                       |
| `balance_after`    | 账户流水表         | 变动后可用余额快照                       |
| `frozen_before`    | 账户流水表         | 变动前冻结余额快照                       |
| `frozen_after`     | 账户流水表         | 变动后冻结余额快照                       |

金额字段建议统一使用 `BIGINT` 存储最小单位。比如人民币 10.25 元存储为 `1025` 分。展示给前端时再转换为元，避免数据库层出现精度误差。

### 索引设计

索引设计需要围绕账户定位、幂等写入、流水查询、业务对账和时间范围查询展开。账户流水通常是高频写入、高频按账户分页查询的数据表，因此索引不宜过多，否则会增加写入成本。

账户表索引示例：

```sql
ALTER TABLE biz_account
    ADD UNIQUE KEY uk_account_no (account_no),
    ADD UNIQUE KEY uk_owner_type_owner_id_currency (owner_type, owner_id, currency_code),
    ADD KEY idx_account_status (account_status);
```

账户流水表索引示例：

```sql
ALTER TABLE biz_account_flow
    ADD UNIQUE KEY uk_flow_no (flow_no),
    ADD UNIQUE KEY uk_request_no (request_no),
    ADD KEY idx_account_id_created_at (account_id, created_at),
    ADD KEY idx_account_no_created_at (account_no, created_at),
    ADD KEY idx_biz_type_biz_no (biz_type, biz_no),
    ADD KEY idx_created_at (created_at);
```

索引设计说明：

| 索引                              | 作用                               |
| --------------------------------- | ---------------------------------- |
| `uk_account_no`                   | 保证账户编号唯一                   |
| `uk_owner_type_owner_id_currency` | 保证同一主体同一币种只存在一个账户 |
| `uk_flow_no`                      | 保证流水编号唯一                   |
| `uk_request_no`                   | 保证同一业务请求只写入一次流水     |
| `idx_account_id_created_at`       | 支持按账户查询流水列表             |
| `idx_account_no_created_at`       | 支持通过账户编号查询流水列表       |
| `idx_biz_type_biz_no`             | 支持按业务单号对账                 |
| `idx_created_at`                  | 支持按时间范围归档、统计或巡检     |

如果账户流水表数据量很大，应结合分区表、归档表或分库分表设计。普通业务初期可以先使用单表加合理索引，避免过早引入复杂架构。

### 常用查询

常用查询主要围绕账户余额查询、账户流水分页、业务单号追踪、对账统计和异常检查展开。查询时应优先使用已经设计好的业务索引，避免对流水大表做无条件扫描。

#### 查询账户当前余额

该查询用于获取账户当前可用余额、冻结余额和总余额，适合账户详情页、支付前校验、后台账户管理等场景。

```sql
SELECT
    id,
    account_no,
    owner_id,
    owner_type,
    currency_code,
    available_amount,
    frozen_amount,
    total_amount,
    account_status,
    version,
    updated_at
FROM biz_account
WHERE account_no = 'ACC202501010001';
```

如果业务侧通过用户ID查询账户，可以使用主体维度查询：

```sql
SELECT
    id,
    account_no,
    owner_id,
    owner_type,
    currency_code,
    available_amount,
    frozen_amount,
    total_amount,
    account_status
FROM biz_account
WHERE owner_type = 'USER'
  AND owner_id = 10001
  AND currency_code = 'CNY';
```

#### 分页查询账户流水

该查询用于账户明细页，通常按照创建时间倒序展示。大表分页时不建议长期使用深分页，可以结合游标分页优化。

```sql
SELECT
    flow_no,
    account_no,
    biz_type,
    biz_no,
    change_type,
    change_amount,
    balance_before,
    balance_after,
    frozen_before,
    frozen_after,
    flow_status,
    remark,
    created_at
FROM biz_account_flow
WHERE account_id = 1000001
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;
```

游标分页示例：

```sql
SELECT
    flow_no,
    account_no,
    biz_type,
    biz_no,
    change_type,
    change_amount,
    balance_before,
    balance_after,
    created_at
FROM biz_account_flow
WHERE account_id = 1000001
  AND (
      created_at < '2025-01-01 12:00:00'
      OR (created_at = '2025-01-01 12:00:00' AND id < 9000001)
  )
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

#### 查询业务单号对应流水

该查询用于支付单、订单、退款单、调账单等业务单据反查账户流水，适合对账、客服排查和业务链路追踪。

```sql
SELECT
    flow_no,
    account_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_amount,
    balance_before,
    balance_after,
    flow_status,
    created_at
FROM biz_account_flow
WHERE biz_type = 'CONSUME'
  AND biz_no = 'ORDER202501010001';
```

#### 查询指定时间范围内的入账总额

该查询用于简单运营统计或日终核对。统计类查询建议限制时间范围，避免直接扫描全量流水。

```sql
SELECT
    account_id,
    SUM(change_amount) AS total_in_amount,
    COUNT(*) AS flow_count
FROM biz_account_flow
WHERE account_id = 1000001
  AND change_type = 'IN'
  AND created_at >= '2025-01-01 00:00:00'
  AND created_at < '2025-01-02 00:00:00'
  AND flow_status = 1
GROUP BY account_id;
```

#### 查询账户余额与最后一条流水是否一致

该查询用于巡检账户表余额和流水余额快照是否一致。它可以发现部分事务异常、人工修改或历史脏数据问题。

```sql
SELECT
    a.id AS account_id,
    a.account_no,
    a.available_amount,
    f.balance_after AS last_flow_balance,
    a.frozen_amount,
    f.frozen_after AS last_flow_frozen,
    f.flow_no AS last_flow_no,
    f.created_at AS last_flow_time
FROM biz_account a
JOIN biz_account_flow f ON f.id = (
    SELECT f2.id
    FROM biz_account_flow f2
    WHERE f2.account_id = a.id
    ORDER BY f2.created_at DESC, f2.id DESC
    LIMIT 1
)
WHERE a.available_amount <> f.balance_after
   OR a.frozen_amount <> f.frozen_after;
```

### 常用写入

常用写入必须保证账户余额更新和流水写入在同一个数据库事务中完成。业务写入前应先通过 `request_no` 做幂等控制，避免重试、MQ重复消费、接口重复提交导致重复变动余额。

#### 创建账户

创建账户时，需要保证同一主体、同一币种、同一账户类型不重复创建。可以通过唯一索引控制并发。

```sql
INSERT INTO biz_account (
    id,
    account_no,
    owner_id,
    owner_type,
    currency_code,
    available_amount,
    frozen_amount,
    total_amount,
    account_status,
    version,
    remark
) VALUES (
    1000001,
    'ACC202501010001',
    10001,
    'USER',
    'CNY',
    0,
    0,
    0,
    1,
    0,
    '用户钱包账户'
);
```

如果存在并发创建账户的场景，应依赖唯一索引 `uk_owner_type_owner_id_currency`，捕获唯一键冲突后重新查询账户，不建议先查再插作为唯一防线。

#### 账户入账

账户入账一般用于充值到账、退款到账、奖励发放等场景。写入流程是先更新账户余额，再插入账户流水。

```sql
START TRANSACTION;

-- 1. 更新账户余额
UPDATE biz_account
SET
    available_amount = available_amount + 1000,
    total_amount = total_amount + 1000,
    version = version + 1,
    updated_at = NOW()
WHERE id = 1000001
  AND account_status = 1;

-- 2. 插入账户流水
INSERT INTO biz_account_flow (
    id,
    flow_no,
    account_id,
    account_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_amount,
    balance_before,
    balance_after,
    frozen_before,
    frozen_after,
    currency_code,
    flow_status,
    remark
) VALUES (
    2000001,
    'FLOW202501010001',
    1000001,
    'ACC202501010001',
    'REQ_RECHARGE_202501010001',
    'RECHARGE',
    'PAY202501010001',
    'IN',
    1000,
    0,
    1000,
    0,
    0,
    'CNY',
    1,
    '充值入账'
);

COMMIT;
```

实际业务中，`balance_before` 和 `balance_after` 不应手工猜测，建议在事务中先锁定账户行，读取当前余额，再计算并写入流水。

#### 账户出账

账户出账一般用于支付、提现、扣款等场景。扣减余额时必须加上余额充足条件，避免并发下余额被扣成负数。

```sql
START TRANSACTION;

-- 1. 扣减账户余额，余额不足时该语句影响行数为0
UPDATE biz_account
SET
    available_amount = available_amount - 800,
    total_amount = total_amount - 800,
    version = version + 1,
    updated_at = NOW()
WHERE id = 1000001
  AND account_status = 1
  AND available_amount >= 800;

-- 2. 确认上一步影响行数为1后，再插入账户流水
INSERT INTO biz_account_flow (
    id,
    flow_no,
    account_id,
    account_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_amount,
    balance_before,
    balance_after,
    frozen_before,
    frozen_after,
    currency_code,
    flow_status,
    remark
) VALUES (
    2000002,
    'FLOW202501010002',
    1000001,
    'ACC202501010001',
    'REQ_CONSUME_202501010001',
    'CONSUME',
    'ORDER202501010001',
    'OUT',
    -800,
    1000,
    200,
    0,
    0,
    'CNY',
    1,
    '订单消费扣款'
);

COMMIT;
```

如果更新账户余额影响行数为 `0`，应回滚事务并返回余额不足、账户冻结或账户不存在等业务错误。不能在扣减失败时继续插入流水。

#### 冻结与解冻金额

冻结金额适合提现审核、担保交易、保证金占用等场景。冻结时从可用余额转入冻结余额，总余额不变。

```sql
START TRANSACTION;

-- 1. 冻结金额：减少可用余额，增加冻结余额
UPDATE biz_account
SET
    available_amount = available_amount - 500,
    frozen_amount = frozen_amount + 500,
    version = version + 1,
    updated_at = NOW()
WHERE id = 1000001
  AND account_status = 1
  AND available_amount >= 500;

-- 2. 记录冻结流水
INSERT INTO biz_account_flow (
    id,
    flow_no,
    account_id,
    account_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_amount,
    balance_before,
    balance_after,
    frozen_before,
    frozen_after,
    currency_code,
    flow_status,
    remark
) VALUES (
    2000003,
    'FLOW202501010003',
    1000001,
    'ACC202501010001',
    'REQ_FREEZE_202501010001',
    'FREEZE',
    'WITHDRAW202501010001',
    'FREEZE',
    -500,
    200,
    -300,
    0,
    500,
    'CNY',
    1,
    '提现申请冻结金额'
);

COMMIT;
```

上面示例中的余额快照仅用于说明字段含义。真实写入时必须基于事务内读取的账户余额计算，不能写出负数快照。冻结成功后，可用余额减少，冻结余额增加，总余额不变。

解冻时从冻结余额转回可用余额：

```sql
START TRANSACTION;

UPDATE biz_account
SET
    available_amount = available_amount + 500,
    frozen_amount = frozen_amount - 500,
    version = version + 1,
    updated_at = NOW()
WHERE id = 1000001
  AND account_status = 1
  AND frozen_amount >= 500;

INSERT INTO biz_account_flow (
    id,
    flow_no,
    account_id,
    account_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_amount,
    balance_before,
    balance_after,
    frozen_before,
    frozen_after,
    currency_code,
    flow_status,
    remark
) VALUES (
    2000004,
    'FLOW202501010004',
    1000001,
    'ACC202501010001',
    'REQ_UNFREEZE_202501010001',
    'UNFREEZE',
    'WITHDRAW202501010001',
    'UNFREEZE',
    500,
    200,
    700,
    500,
    0,
    'CNY',
    1,
    '提现取消解冻金额'
);

COMMIT;
```

#### 幂等写入

幂等写入是账户流水模型的核心能力。相同业务请求重复到达时，只能成功写入一次账户流水，也只能变更一次账户余额。

常见做法是使用 `request_no` 建唯一索引。业务请求进入后，先检查或直接插入流水。如果发生唯一键冲突，说明该请求已经处理过，应查询原流水并返回原处理结果。

```sql
SELECT
    flow_no,
    account_id,
    account_no,
    biz_type,
    biz_no,
    change_type,
    change_amount,
    flow_status,
    created_at
FROM biz_account_flow
WHERE request_no = 'REQ_RECHARGE_202501010001';
```

应用层处理建议：

```text
1. 接收业务请求，生成或获取 request_no。
2. 开启数据库事务。
3. 锁定账户行或执行带条件的余额更新。
4. 插入账户流水。
5. 如果插入流水发生 uk_request_no 唯一键冲突，回滚事务并查询原流水。
6. 提交事务。
```

### 常见问题

账户流水模型的问题通常集中在金额精度、并发扣款、重复写入、余额和流水不一致、流水修改、分页查询性能等方面。

| 问题                         | 原因                               | 建议                                               |
| ---------------------------- | ---------------------------------- | -------------------------------------------------- |
| 余额被扣成负数               | 扣减时没有加余额充足条件           | `UPDATE` 语句中增加 `available_amount >= 扣减金额` |
| 重复入账或重复扣款           | 接口重试、MQ重复消费、用户重复提交 | 使用 `request_no` 唯一索引做幂等                   |
| 账户余额和流水最后余额不一致 | 账户更新和流水写入不在同一事务     | 余额更新与流水插入必须同事务提交                   |
| 金额出现精度误差             | 使用 `FLOAT` 或 `DOUBLE` 存金额    | 使用 `BIGINT` 存最小单位，或使用 `DECIMAL`         |
| 流水分页越来越慢             | 大表深分页或索引不匹配             | 使用账户ID加时间索引，必要时使用游标分页           |
| 无法追踪业务来源             | 流水缺少业务类型和业务单号         | 保留 `biz_type`、`biz_no`、`request_no`            |
| 历史流水被修改               | 业务直接更新流水核心字段           | 流水表只追加，冲正通过新增反向流水处理             |
| 对账困难                     | 流水没有保存变动前后余额           | 保存 `balance_before` 和 `balance_after`           |

账户流水表不建议只保存变动金额而不保存变动后余额。虽然余额可以通过所有历史流水累加计算出来，但在数据量大、对账频繁、问题排查复杂的情况下，缺少余额快照会显著增加排查成本。

### 总结

账户流水模型的核心是“账户表保存当前状态，流水表保存每次变动”。账户余额更新和账户流水写入必须在同一个事务中完成，金额字段应避免浮点类型，业务请求必须具备幂等控制。

建模时应重点关注四个点：第一，账户表只表达当前余额；第二，流水表只追加记录；第三，写入链路保证幂等和并发安全；第四，查询链路围绕账户、业务单号和时间范围设计索引。

该模型可以作为钱包、积分、储值、商户账户、平台结算账户等业务的基础模型。后续库存模型和库存流水模型可以沿用类似思想：库存表保存当前库存状态，库存流水表保存每一次库存变动明细。



## 库存模型

库存模型用于记录商品、SKU、仓库、批次、渠道等维度下的当前库存状态。它重点解决“当前还有多少库存”“可售库存是否充足”“库存是否被冻结或占用”等问题。库存流水模型会在后续章节中记录每一次库存变动明细，本节只关注库存当前状态表的建模方式。

### 适用场景

库存模型适用于需要维护商品当前库存数量的业务系统。典型场景包括电商商品库存、仓库库存、门店库存、批次库存、渠道库存、活动库存、虚拟商品库存、权益库存、卡券库存等。

该模型适合处理高频库存查询和库存扣减场景，例如下单前校验库存、支付前锁定库存、订单取消释放库存、发货扣减库存、采购入库增加库存、后台调整库存等。

库存模型一般不单独承担库存审计能力。如果业务需要追踪库存为什么增加或减少，应配合库存流水模型使用。库存表负责保存当前库存状态，库存流水表负责保存库存变化过程。

### 建模结构

库存模型的核心是库存表。库存表通常按照业务库存维度唯一确定一条库存记录，例如 `sku_id + warehouse_id`，或者 `sku_id + warehouse_id + batch_no`。库存维度越细，库存控制越精准，但查询、扣减和汇总成本也会增加。

常见建模结构如下：

```text
biz_inventory
  ├── id                    库存主键
  ├── inventory_no          库存编号
  ├── spu_id                商品SPU ID
  ├── sku_id                商品SKU ID
  ├── warehouse_id          仓库ID
  ├── warehouse_code        仓库编码
  ├── batch_no              批次号
  ├── total_quantity        总库存
  ├── available_quantity    可用库存
  ├── locked_quantity       锁定库存
  ├── sold_quantity         已售库存
  ├── warning_quantity      预警库存
  ├── inventory_status      库存状态
  └── version               乐观锁版本号
```

库存数量之间通常需要满足以下关系：

```text
总库存 = 可用库存 + 锁定库存 + 已售库存
```

不同业务也可以简化该关系。例如部分系统不关心已售库存，只维护：

```text
总库存 = 可用库存 + 锁定库存
```

如果系统已有订单、发货、售后等独立统计表，库存表中的 `sold_quantity` 可以不作为强依赖字段，只作为冗余统计字段使用。核心扣减逻辑应以 `available_quantity` 和 `locked_quantity` 为准。

### 字段设计

库存字段设计需要重点关注库存维度、库存数量、库存状态、并发控制和审计字段。库存数量建议使用 `BIGINT`，即使当前业务库存数量较小，也可以避免后续扩展时出现字段范围不足的问题。

库存表示例：

```sql
CREATE TABLE biz_inventory (
    id BIGINT NOT NULL COMMENT '主键ID',
    inventory_no VARCHAR(64) NOT NULL COMMENT '库存编号',
    spu_id BIGINT NOT NULL COMMENT '商品SPU ID',
    sku_id BIGINT NOT NULL COMMENT '商品SKU ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    warehouse_code VARCHAR(64) NOT NULL COMMENT '仓库编码',
    batch_no VARCHAR(64) NOT NULL DEFAULT '' COMMENT '批次号，无批次时为空字符串',
    total_quantity BIGINT NOT NULL DEFAULT 0 COMMENT '总库存',
    available_quantity BIGINT NOT NULL DEFAULT 0 COMMENT '可用库存',
    locked_quantity BIGINT NOT NULL DEFAULT 0 COMMENT '锁定库存',
    sold_quantity BIGINT NOT NULL DEFAULT 0 COMMENT '已售库存',
    warning_quantity BIGINT NOT NULL DEFAULT 0 COMMENT '预警库存',
    inventory_status TINYINT NOT NULL DEFAULT 1 COMMENT '库存状态：1正常，2冻结，9停用',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';
```

核心字段说明：

| 字段                 | 设计说明                                           |
| -------------------- | -------------------------------------------------- |
| `inventory_no`       | 库存编号，便于外部系统引用库存记录                 |
| `spu_id`             | 商品SPU ID，用于商品维度汇总                       |
| `sku_id`             | 商品SKU ID，库存扣减通常以SKU为最小商品单位        |
| `warehouse_id`       | 仓库ID，用于区分不同仓库库存                       |
| `warehouse_code`     | 仓库编码，便于和WMS、ERP等外部系统对接             |
| `batch_no`           | 批次号，适合食品、药品、批次入库等场景             |
| `total_quantity`     | 总库存，一般表示当前库存总量                       |
| `available_quantity` | 可用库存，可用于销售、分配或出库                   |
| `locked_quantity`    | 锁定库存，表示已被订单或业务占用但未最终扣减的库存 |
| `sold_quantity`      | 已售库存，可用于统计，也可以由订单侧统计替代       |
| `warning_quantity`   | 库存预警数量，低于该值时触发补货或告警             |
| `inventory_status`   | 库存状态，冻结或停用后不允许正常扣减               |
| `version`            | 乐观锁版本号，用于并发更新控制                     |

库存数量字段不建议使用 `INT`。在多仓、多渠道、批量导入、历史迁移或虚拟库存场景中，库存数量可能超过预期。统一使用 `BIGINT` 可以减少后续调整成本。

### 索引设计

库存表的索引应围绕库存唯一性、库存查询、库存预警、仓库维度查询和商品维度查询设计。库存表属于高频更新表，索引数量不宜过多，否则会增加扣减库存、锁定库存和释放库存时的写入成本。

库存表索引示例：

```sql
ALTER TABLE biz_inventory
    ADD UNIQUE KEY uk_inventory_no (inventory_no),
    ADD UNIQUE KEY uk_sku_warehouse_batch (sku_id, warehouse_id, batch_no),
    ADD KEY idx_spu_id (spu_id),
    ADD KEY idx_warehouse_id (warehouse_id),
    ADD KEY idx_inventory_status (inventory_status),
    ADD KEY idx_warning_quantity (available_quantity, warning_quantity);
```

索引设计说明：

| 索引                     | 作用                                            |
| ------------------------ | ----------------------------------------------- |
| `uk_inventory_no`        | 保证库存编号唯一                                |
| `uk_sku_warehouse_batch` | 保证同一SKU、同一仓库、同一批次只有一条库存记录 |
| `idx_spu_id`             | 支持按SPU汇总或查询SKU库存                      |
| `idx_warehouse_id`       | 支持按仓库查询库存列表                          |
| `idx_inventory_status`   | 支持后台按库存状态筛选                          |
| `idx_warning_quantity`   | 支持库存预警扫描                                |

如果业务没有批次维度，可以将唯一索引改为：

```sql
ALTER TABLE biz_inventory
    ADD UNIQUE KEY uk_sku_warehouse (sku_id, warehouse_id);
```

如果业务只有单仓库存，可以进一步简化为：

```sql
ALTER TABLE biz_inventory
    ADD UNIQUE KEY uk_sku_id (sku_id);
```

索引设计应与真实库存维度一致，不要为了未来可能存在的维度提前设计复杂唯一索引。库存唯一维度一旦确定，后续变更成本较高，因此建模初期需要明确库存是按 SKU、仓库、批次、渠道，还是多个维度共同管理。

### 常用查询

常用查询主要围绕库存详情、SKU库存、仓库库存、库存预警、库存充足校验和库存汇总展开。库存表通常数据量小于流水表，但更新频率更高，查询条件应尽量命中唯一索引或普通索引。

#### 查询SKU在指定仓库的库存

该查询用于商品详情页、下单前库存校验、后台库存管理等场景。使用 `sku_id + warehouse_id + batch_no` 可以直接命中唯一索引。

```sql
SELECT
    id,
    inventory_no,
    spu_id,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    total_quantity,
    available_quantity,
    locked_quantity,
    sold_quantity,
    warning_quantity,
    inventory_status,
    version,
    updated_at
FROM biz_inventory
WHERE sku_id = 10010001
  AND warehouse_id = 20001
  AND batch_no = '';
```

#### 查询SKU在所有仓库的库存

该查询用于商品库存汇总、调拨建议、库存分布展示等场景。

```sql
SELECT
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    total_quantity,
    available_quantity,
    locked_quantity,
    sold_quantity,
    inventory_status
FROM biz_inventory
WHERE sku_id = 10010001
ORDER BY warehouse_id ASC, batch_no ASC;
```

#### 查询SPU下所有SKU库存

该查询用于商品后台管理页，展示一个商品下所有SKU的库存情况。

```sql
SELECT
    spu_id,
    sku_id,
    SUM(total_quantity) AS total_quantity,
    SUM(available_quantity) AS available_quantity,
    SUM(locked_quantity) AS locked_quantity,
    SUM(sold_quantity) AS sold_quantity
FROM biz_inventory
WHERE spu_id = 10001
  AND inventory_status = 1
GROUP BY spu_id, sku_id
ORDER BY sku_id ASC;
```

#### 查询仓库库存列表

该查询用于仓库管理后台、WMS库存同步、库存盘点等场景。

```sql
SELECT
    inventory_no,
    spu_id,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    total_quantity,
    available_quantity,
    locked_quantity,
    sold_quantity,
    warning_quantity,
    inventory_status,
    updated_at
FROM biz_inventory
WHERE warehouse_id = 20001
ORDER BY updated_at DESC
LIMIT 20 OFFSET 0;
```

#### 查询低库存预警数据

该查询用于补货提醒、运营巡检、库存告警任务等场景。

```sql
SELECT
    inventory_no,
    spu_id,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    available_quantity,
    warning_quantity,
    inventory_status,
    updated_at
FROM biz_inventory
WHERE inventory_status = 1
  AND available_quantity <= warning_quantity
ORDER BY available_quantity ASC, updated_at DESC
LIMIT 100;
```

#### 查询库存是否充足

该查询用于下单前库存校验。只查询不扣减时，该结果只能作为提示，不能作为最终扣减依据。真正扣减库存时仍然需要在 `UPDATE` 中增加库存充足条件。

```sql
SELECT
    id,
    inventory_no,
    sku_id,
    warehouse_id,
    batch_no,
    available_quantity,
    inventory_status
FROM biz_inventory
WHERE sku_id = 10010001
  AND warehouse_id = 20001
  AND batch_no = ''
  AND inventory_status = 1
  AND available_quantity >= 3;
```

### 常用写入

库存写入主要包括初始化库存、增加库存、锁定库存、释放库存、扣减库存和调整库存。库存写入必须保证并发安全，尤其是锁定库存和扣减库存，不能只依赖“先查再改”的应用层判断。

#### 初始化库存

初始化库存用于商品上架、仓库启用、批次入库前创建库存记录等场景。同一个库存维度只能创建一条库存记录，应通过唯一索引保证。

```sql
INSERT INTO biz_inventory (
    id,
    inventory_no,
    spu_id,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    total_quantity,
    available_quantity,
    locked_quantity,
    sold_quantity,
    warning_quantity,
    inventory_status,
    version,
    remark
) VALUES (
    3000001,
    'INV202501010001',
    10001,
    10010001,
    20001,
    'WH_SH_001',
    '',
    100,
    100,
    0,
    0,
    10,
    1,
    0,
    '商品初始化库存'
);
```

如果存在重复初始化的可能，应依赖 `uk_sku_warehouse_batch` 唯一索引拦截，并在应用层捕获唯一键冲突后转为查询或库存调整流程。

#### 增加库存

增加库存通常用于采购入库、退货入库、人工补货、库存盘盈等场景。增加库存时，一般同时增加总库存和可用库存。

```sql
UPDATE biz_inventory
SET
    total_quantity = total_quantity + 50,
    available_quantity = available_quantity + 50,
    version = version + 1,
    updated_at = NOW()
WHERE sku_id = 10010001
  AND warehouse_id = 20001
  AND batch_no = ''
  AND inventory_status = 1;
```

如果业务要求记录库存变动原因，应在同一个事务中写入库存流水。库存流水模型将在后续章节展开。

#### 锁定库存

锁定库存通常发生在订单创建成功后、支付前或履约前。锁定库存表示库存已经被业务占用，但还没有最终出库或扣减。

```sql
UPDATE biz_inventory
SET
    available_quantity = available_quantity - 3,
    locked_quantity = locked_quantity + 3,
    version = version + 1,
    updated_at = NOW()
WHERE sku_id = 10010001
  AND warehouse_id = 20001
  AND batch_no = ''
  AND inventory_status = 1
  AND available_quantity >= 3;
```

执行后必须检查影响行数。如果影响行数为 `0`，通常表示库存不存在、库存状态不可用或可用库存不足，业务应返回库存不足或库存不可售。

#### 释放库存

释放库存通常发生在订单取消、支付超时、审核拒绝、履约失败等场景。释放库存表示将已锁定库存重新转回可用库存。

```sql
UPDATE biz_inventory
SET
    available_quantity = available_quantity + 3,
    locked_quantity = locked_quantity - 3,
    version = version + 1,
    updated_at = NOW()
WHERE sku_id = 10010001
  AND warehouse_id = 20001
  AND batch_no = ''
  AND inventory_status = 1
  AND locked_quantity >= 3;
```

释放库存时必须增加 `locked_quantity >= 释放数量` 条件，避免并发或重复释放导致锁定库存变成负数。

#### 扣减库存

扣减库存通常发生在支付成功、发货确认、出库完成等场景。根据业务流程不同，可以直接从可用库存扣减，也可以先锁定库存，再从锁定库存转为已售库存。

如果业务没有锁库存流程，可以直接扣减可用库存：

```sql
UPDATE biz_inventory
SET
    total_quantity = total_quantity - 3,
    available_quantity = available_quantity - 3,
    sold_quantity = sold_quantity + 3,
    version = version + 1,
    updated_at = NOW()
WHERE sku_id = 10010001
  AND warehouse_id = 20001
  AND batch_no = ''
  AND inventory_status = 1
  AND available_quantity >= 3;
```

如果业务存在锁库存流程，支付或出库成功后应从锁定库存扣减：

```sql
UPDATE biz_inventory
SET
    total_quantity = total_quantity - 3,
    locked_quantity = locked_quantity - 3,
    sold_quantity = sold_quantity + 3,
    version = version + 1,
    updated_at = NOW()
WHERE sku_id = 10010001
  AND warehouse_id = 20001
  AND batch_no = ''
  AND inventory_status = 1
  AND locked_quantity >= 3;
```

带锁定库存的流程更适合订单链路较长、支付可能延迟、履约需要异步处理的业务系统。直接扣减流程更简单，但对订单取消、支付失败、超卖控制和库存恢复的要求更高。

#### 调整库存

库存调整用于人工修正、盘点差异、系统迁移修复等场景。调整库存属于高风险操作，应限制权限，并记录调整原因。

```sql
UPDATE biz_inventory
SET
    total_quantity = 120,
    available_quantity = 110,
    locked_quantity = 5,
    sold_quantity = 5,
    version = version + 1,
    remark = '盘点后调整库存',
    updated_at = NOW()
WHERE id = 3000001
  AND version = 6;
```

调整库存建议使用乐观锁版本号控制。如果影响行数为 `0`，说明库存记录已被其他事务修改，应用层应重新查询库存后再确认是否继续调整。

### 常见问题

库存模型的常见问题通常集中在超卖、重复释放、库存维度混乱、库存状态缺失、库存汇总不准确、库存字段关系不一致等方面。

| 问题                    | 原因                                                  | 建议                                                         |
| ----------------------- | ----------------------------------------------------- | ------------------------------------------------------------ |
| 商品发生超卖            | 扣减库存时只在应用层判断，没有在SQL中增加库存充足条件 | 在 `UPDATE` 中增加 `available_quantity >= 扣减数量`          |
| 锁定库存变成负数        | 释放库存时没有校验锁定库存是否充足                    | 在释放SQL中增加 `locked_quantity >= 释放数量`                |
| 同一SKU出现多条库存记录 | 库存唯一维度没有唯一索引                              | 根据业务维度建立唯一索引，如 `sku_id + warehouse_id + batch_no` |
| 库存查询结果不一致      | 总库存、可用库存、锁定库存之间缺少约束关系            | 写入时统一维护数量关系，并增加巡检任务                       |
| 库存预警不准确          | 预警库存字段缺失或没有按仓库、SKU配置                 | 在库存表中维护 `warning_quantity`，必要时建立独立预警配置表  |
| 库存扣减性能下降        | 库存表索引过多或热点SKU并发过高                       | 减少非必要索引，对热点SKU考虑库存分片或队列化扣减            |
| 无法追踪库存变化原因    | 只更新库存表，没有记录变动明细                        | 配合库存流水表记录每次库存变动                               |
| 批次库存混乱            | 批次号可空且唯一索引处理不一致                        | 无批次时使用空字符串，避免多个 `NULL` 破坏唯一性预期         |

库存表中的 `batch_no` 不建议直接使用 `NULL` 参与唯一约束。MySQL 唯一索引允许多行 `NULL`，如果希望“无批次库存”也保持唯一，应使用空字符串或固定默认值表示无批次。

### 总结

库存模型的核心是保存当前库存状态。它应清晰表达库存属于哪个 SKU、哪个仓库、哪个批次，以及当前总库存、可用库存、锁定库存和已售库存分别是多少。

库存写入必须依赖数据库条件更新保证并发安全。下单锁库存、支付扣库存、取消释放库存等操作，不能只依赖应用层先查后改。正确做法是在 `UPDATE` 语句中直接增加库存充足或锁定库存充足条件，并通过影响行数判断业务是否成功。

库存模型解决的是当前库存状态问题。库存变化过程、库存审计、库存对账和库存异常追踪，应交给库存流水模型处理。



## 库存流水模型

库存流水模型用于记录库存数量的每一次变动过程。库存表保存当前库存状态，库存流水表保存库存增加、锁定、释放、扣减、调整等变动明细。本文档继续承接首页大纲中的“账户、库存与流水模型”部分。

库存流水的核心价值不是替代库存表，而是为库存变更提供可追踪、可对账、可审计的明细依据。库存表回答“当前库存是多少”，库存流水表回答“库存为什么变成这样”。

### 适用场景

库存流水模型适用于需要追踪库存变化过程的业务场景。只要库存变化会影响订单、履约、财务、仓储、对账或运营分析，就应建立库存流水。

典型场景包括采购入库、销售出库、订单锁库存、订单取消释放库存、支付成功扣减库存、退货入库、库存盘盈、库存盘亏、仓库调拨、批次库存调整、人工修正库存、WMS库存同步、ERP库存同步等。

库存流水模型尤其适合以下业务要求：

| 场景     | 说明                                                     |
| -------- | -------------------------------------------------------- |
| 库存审计 | 追踪每一次库存变更的来源、操作人、业务单号和变动前后数量 |
| 库存对账 | 和订单、发货单、退货单、WMS、ERP等系统核对库存变化       |
| 异常排查 | 排查超卖、重复释放、重复扣减、库存不一致等问题           |
| 异步补偿 | 在库存变更失败、MQ重复消费、接口重试时提供幂等依据       |
| 运营统计 | 统计入库、出库、锁定、释放、调整等库存变动趋势           |

库存流水不建议用于直接计算实时可售库存。实时库存应读取库存表，流水表主要用于追溯、审计和统计。

### 建模结构

库存流水模型通常由库存表和库存流水表配合使用。库存表保存当前库存数量，库存流水表保存每次库存变动明细。

常见建模结构如下：

```text
biz_inventory
  ├── id                    库存主键
  ├── inventory_no          库存编号
  ├── spu_id                商品SPU ID
  ├── sku_id                商品SKU ID
  ├── warehouse_id          仓库ID
  ├── batch_no              批次号
  ├── total_quantity        总库存
  ├── available_quantity    可用库存
  ├── locked_quantity       锁定库存
  └── version               乐观锁版本号

biz_inventory_flow
  ├── id                    流水主键
  ├── flow_no               流水编号
  ├── inventory_id          库存ID
  ├── inventory_no          库存编号
  ├── request_no            幂等请求号
  ├── biz_type              业务类型
  ├── biz_no                业务单号
  ├── change_type           变动类型
  ├── change_quantity       变动数量
  ├── total_before          变动前总库存
  ├── total_after           变动后总库存
  ├── available_before      变动前可用库存
  ├── available_after       变动后可用库存
  ├── locked_before         变动前锁定库存
  ├── locked_after          变动后锁定库存
  └── remark                备注
```

库存流水表应保存变动前数量和变动后数量。这样即使库存表当前值已经发生变化，也能通过流水还原某一次变更发生时的库存状态。

库存流水通常采用追加写入模式。已经生成的流水不建议直接修改核心数量字段。如果发生业务冲正、撤销、补偿或修复，应新增一条反向流水，而不是修改历史流水。

### 字段设计

字段设计需要重点关注库存维度、变动类型、业务来源、幂等请求号、变动前后快照和审计字段。库存数量字段建议使用 `BIGINT`，避免后续业务规模扩大后字段范围不足。

库存流水表示例：

```sql
CREATE TABLE biz_inventory_flow (
    id BIGINT NOT NULL COMMENT '主键ID',
    flow_no VARCHAR(64) NOT NULL COMMENT '流水编号',
    inventory_id BIGINT NOT NULL COMMENT '库存ID',
    inventory_no VARCHAR(64) NOT NULL COMMENT '库存编号',
    spu_id BIGINT NOT NULL COMMENT '商品SPU ID',
    sku_id BIGINT NOT NULL COMMENT '商品SKU ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    warehouse_code VARCHAR(64) NOT NULL COMMENT '仓库编码',
    batch_no VARCHAR(64) NOT NULL DEFAULT '' COMMENT '批次号，无批次时为空字符串',
    request_no VARCHAR(128) NOT NULL COMMENT '幂等请求号，同一业务请求必须唯一',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：PURCHASE_IN、ORDER_LOCK、ORDER_RELEASE、SALE_OUT、RETURN_IN、ADJUST',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务单号，如采购单号、订单号、出库单号、退货单号',
    change_type VARCHAR(32) NOT NULL COMMENT '变动类型：IN、LOCK、RELEASE、OUT、ADJUST',
    change_quantity BIGINT NOT NULL COMMENT '变动数量，增加为正数，减少为负数',
    total_before BIGINT NOT NULL COMMENT '变动前总库存',
    total_after BIGINT NOT NULL COMMENT '变动后总库存',
    available_before BIGINT NOT NULL COMMENT '变动前可用库存',
    available_after BIGINT NOT NULL COMMENT '变动后可用库存',
    locked_before BIGINT NOT NULL COMMENT '变动前锁定库存',
    locked_after BIGINT NOT NULL COMMENT '变动后锁定库存',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID，系统自动处理时可为空',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    flow_status TINYINT NOT NULL DEFAULT 1 COMMENT '流水状态：1有效，2已冲正',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';
```

核心字段说明：

| 字段               | 设计说明                                           |
| ------------------ | -------------------------------------------------- |
| `flow_no`          | 库存流水编号，全局唯一                             |
| `inventory_id`     | 关联库存表主键                                     |
| `inventory_no`     | 冗余库存编号，便于外部系统查询                     |
| `spu_id`           | 商品SPU ID，支持商品维度统计                       |
| `sku_id`           | 商品SKU ID，支持SKU维度查询和对账                  |
| `warehouse_id`     | 仓库ID，支持仓库维度查询                           |
| `batch_no`         | 批次号，适合批次库存追踪                           |
| `request_no`       | 幂等请求号，防止重复写入流水和重复变更库存         |
| `biz_type`         | 业务类型，用于区分采购、订单、退货、调账等来源     |
| `biz_no`           | 业务单号，用于和上游单据对账                       |
| `change_type`      | 库存变动类型，用于表达入库、锁定、释放、出库、调整 |
| `change_quantity`  | 本次变动数量，入库通常为正数，出库通常为负数       |
| `total_before`     | 变动前总库存快照                                   |
| `total_after`      | 变动后总库存快照                                   |
| `available_before` | 变动前可用库存快照                                 |
| `available_after`  | 变动后可用库存快照                                 |
| `locked_before`    | 变动前锁定库存快照                                 |
| `locked_after`     | 变动后锁定库存快照                                 |
| `flow_status`      | 流水状态，冲正时不修改原流水核心数量字段           |

`change_quantity` 的正负号应保持业务约定一致。建议入库、释放可售、退货入库为正数，销售出库、锁定库存、盘亏为负数。对于锁定和释放这类只在可用库存与锁定库存之间转换的操作，可以通过 `change_type` 解释业务含义，通过前后快照表达具体变化。

### 索引设计

库存流水表通常写入量大，查询场景集中在库存维度、业务单号、幂等请求号、时间范围和对账统计。索引设计需要兼顾查询效率和写入成本，不建议为每个字段都建立索引。

库存流水表索引示例：

```sql
ALTER TABLE biz_inventory_flow
    ADD UNIQUE KEY uk_flow_no (flow_no),
    ADD UNIQUE KEY uk_request_no (request_no),
    ADD KEY idx_inventory_id_created_at (inventory_id, created_at),
    ADD KEY idx_sku_warehouse_batch_created_at (sku_id, warehouse_id, batch_no, created_at),
    ADD KEY idx_biz_type_biz_no (biz_type, biz_no),
    ADD KEY idx_change_type_created_at (change_type, created_at),
    ADD KEY idx_created_at (created_at);
```

索引设计说明：

| 索引                                 | 作用                                           |
| ------------------------------------ | ---------------------------------------------- |
| `uk_flow_no`                         | 保证库存流水编号唯一                           |
| `uk_request_no`                      | 保证同一业务请求只处理一次                     |
| `idx_inventory_id_created_at`        | 支持按库存记录分页查询流水                     |
| `idx_sku_warehouse_batch_created_at` | 支持按SKU、仓库、批次查询库存流水              |
| `idx_biz_type_biz_no`                | 支持通过业务单号反查库存流水                   |
| `idx_change_type_created_at`         | 支持按变动类型统计入库、出库、锁定、释放等数据 |
| `idx_created_at`                     | 支持按时间范围归档、统计和巡检                 |

如果库存流水数据量很大，可以按 `created_at` 做时间分区，或者按 `sku_id`、`warehouse_id`、`inventory_id` 做分库分表路由。具体方案应结合查询模式决定，不能只按写入量盲目拆分。

### 常用查询

常用查询主要围绕库存流水分页、业务单据追踪、SKU库存变动查询、库存对账统计、异常流水检查和最后一次流水校验展开。库存流水表通常数据量较大，查询必须尽量带上库存ID、SKU、仓库、业务单号或时间范围。

#### 分页查询库存流水

该查询用于后台库存明细页，按库存记录查看每一次库存变化。适合通过 `inventory_id + created_at` 索引查询。

```sql
SELECT
    flow_no,
    inventory_no,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    biz_type,
    biz_no,
    change_type,
    change_quantity,
    total_before,
    total_after,
    available_before,
    available_after,
    locked_before,
    locked_after,
    flow_status,
    remark,
    created_at
FROM biz_inventory_flow
WHERE inventory_id = 3000001
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;
```

大表场景建议使用游标分页，避免深分页扫描过多数据。

```sql
SELECT
    flow_no,
    inventory_no,
    sku_id,
    warehouse_id,
    batch_no,
    biz_type,
    biz_no,
    change_type,
    change_quantity,
    available_before,
    available_after,
    locked_before,
    locked_after,
    created_at
FROM biz_inventory_flow
WHERE inventory_id = 3000001
  AND (
      created_at < '2025-01-01 12:00:00'
      OR (created_at = '2025-01-01 12:00:00' AND id < 8000001)
  )
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

#### 查询业务单号对应库存流水

该查询用于通过订单号、采购单号、出库单号、退货单号反查库存变动记录，适合客服排查、对账和业务链路追踪。

```sql
SELECT
    flow_no,
    inventory_no,
    sku_id,
    warehouse_id,
    batch_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_quantity,
    total_before,
    total_after,
    available_before,
    available_after,
    locked_before,
    locked_after,
    flow_status,
    created_at
FROM biz_inventory_flow
WHERE biz_type = 'ORDER_LOCK'
  AND biz_no = 'ORDER202501010001';
```

如果一个订单包含多个SKU，该查询会返回多条库存流水。业务侧应结合订单明细中的 SKU 和数量进行核对。

#### 查询SKU在指定仓库的库存变动

该查询用于查看某个 SKU 在某个仓库、某个批次下的库存变化历史。

```sql
SELECT
    flow_no,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    biz_type,
    biz_no,
    change_type,
    change_quantity,
    available_before,
    available_after,
    locked_before,
    locked_after,
    created_at
FROM biz_inventory_flow
WHERE sku_id = 10010001
  AND warehouse_id = 20001
  AND batch_no = ''
  AND created_at >= '2025-01-01 00:00:00'
  AND created_at < '2025-02-01 00:00:00'
ORDER BY created_at DESC, id DESC
LIMIT 100;
```

库存流水查询应尽量限制时间范围。对于长期运行的系统，全量查询某个 SKU 的所有历史流水通常成本较高。

#### 统计指定时间范围内的入库数量

该查询用于统计采购入库、退货入库、人工增加库存等入库类变动。

```sql
SELECT
    sku_id,
    warehouse_id,
    batch_no,
    SUM(change_quantity) AS total_in_quantity,
    COUNT(*) AS flow_count
FROM biz_inventory_flow
WHERE change_type = 'IN'
  AND flow_status = 1
  AND created_at >= '2025-01-01 00:00:00'
  AND created_at < '2025-01-02 00:00:00'
GROUP BY sku_id, warehouse_id, batch_no
ORDER BY total_in_quantity DESC;
```

如果入库类型较多，也可以按 `biz_type` 进一步细分。

```sql
SELECT
    biz_type,
    sku_id,
    warehouse_id,
    SUM(change_quantity) AS total_quantity,
    COUNT(*) AS flow_count
FROM biz_inventory_flow
WHERE change_type = 'IN'
  AND flow_status = 1
  AND created_at >= '2025-01-01 00:00:00'
  AND created_at < '2025-01-02 00:00:00'
GROUP BY biz_type, sku_id, warehouse_id
ORDER BY biz_type ASC, total_quantity DESC;
```

#### 统计指定时间范围内的出库数量

该查询用于统计销售出库、人工扣减、盘亏等库存减少类变动。由于 `change_quantity` 对出库通常为负数，统计展示时可以取绝对值。

```sql
SELECT
    sku_id,
    warehouse_id,
    batch_no,
    ABS(SUM(change_quantity)) AS total_out_quantity,
    COUNT(*) AS flow_count
FROM biz_inventory_flow
WHERE change_type = 'OUT'
  AND flow_status = 1
  AND created_at >= '2025-01-01 00:00:00'
  AND created_at < '2025-01-02 00:00:00'
GROUP BY sku_id, warehouse_id, batch_no
ORDER BY total_out_quantity DESC;
```

统计结果适合用于日结、仓库出库量报表、商品销量辅助分析等场景。正式财务或订单统计仍应以订单、支付、履约等主业务表为准。

#### 查询重复业务请求

该查询用于检查是否存在相同业务单号多次触发库存变更的情况。它不能替代 `request_no` 唯一索引，但可以作为巡检 SQL 使用。

```sql
SELECT
    biz_type,
    biz_no,
    sku_id,
    warehouse_id,
    batch_no,
    change_type,
    COUNT(*) AS flow_count
FROM biz_inventory_flow
WHERE created_at >= '2025-01-01 00:00:00'
  AND created_at < '2025-01-02 00:00:00'
GROUP BY biz_type, biz_no, sku_id, warehouse_id, batch_no, change_type
HAVING COUNT(*) > 1
ORDER BY flow_count DESC;
```

业务上是否允许同一业务单号生成多条流水，需要根据业务规则判断。例如一个订单包含多个 SKU 时，多条流水是正常的；同一个订单同一个 SKU 重复锁库存通常需要重点排查。

#### 查询库存表与最后一条流水是否一致

该查询用于巡检库存表当前数量与最后一条库存流水快照是否一致。它可以发现事务异常、人工改库、补偿遗漏等问题。

```sql
SELECT
    i.id AS inventory_id,
    i.inventory_no,
    i.sku_id,
    i.warehouse_id,
    i.batch_no,
    i.total_quantity,
    f.total_after AS last_total_quantity,
    i.available_quantity,
    f.available_after AS last_available_quantity,
    i.locked_quantity,
    f.locked_after AS last_locked_quantity,
    f.flow_no AS last_flow_no,
    f.created_at AS last_flow_time
FROM biz_inventory i
JOIN biz_inventory_flow f ON f.id = (
    SELECT f2.id
    FROM biz_inventory_flow f2
    WHERE f2.inventory_id = i.id
    ORDER BY f2.created_at DESC, f2.id DESC
    LIMIT 1
)
WHERE i.total_quantity <> f.total_after
   OR i.available_quantity <> f.available_after
   OR i.locked_quantity <> f.locked_after;
```

该查询适合低频巡检，不适合在高并发交易链路中实时执行。大数据量场景应按库存ID范围或时间分批执行。

### 常用写入

库存流水写入必须和库存表更新放在同一个事务中。常见流程是先读取或锁定库存记录，计算变动前后数量，更新库存表，再插入库存流水。不能只写流水不更新库存表，也不能只更新库存表不写流水。

#### 采购入库写入流水

采购入库会增加总库存和可用库存。写入时应先更新库存表，再插入库存流水，二者必须在同一个事务中提交。

```sql
START TRANSACTION;

-- 1. 锁定库存行，获取变动前库存快照
SELECT
    id,
    inventory_no,
    total_quantity,
    available_quantity,
    locked_quantity
FROM biz_inventory
WHERE sku_id = 10010001
  AND warehouse_id = 20001
  AND batch_no = ''
  AND inventory_status = 1
FOR UPDATE;

-- 2. 更新库存表
UPDATE biz_inventory
SET
    total_quantity = total_quantity + 50,
    available_quantity = available_quantity + 50,
    version = version + 1,
    updated_at = NOW()
WHERE id = 3000001;

-- 3. 插入库存流水
INSERT INTO biz_inventory_flow (
    id,
    flow_no,
    inventory_id,
    inventory_no,
    spu_id,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_quantity,
    total_before,
    total_after,
    available_before,
    available_after,
    locked_before,
    locked_after,
    operator_id,
    operator_name,
    flow_status,
    remark
) VALUES (
    4000001,
    'IFLOW202501010001',
    3000001,
    'INV202501010001',
    10001,
    10010001,
    20001,
    'WH_SH_001',
    '',
    'REQ_PURCHASE_IN_202501010001',
    'PURCHASE_IN',
    'PURCHASE202501010001',
    'IN',
    50,
    100,
    150,
    100,
    150,
    0,
    0,
    90001,
    '仓库管理员',
    1,
    '采购入库'
);

COMMIT;
```

示例中的 `total_before`、`available_before` 和 `locked_before` 应来自事务中 `SELECT ... FOR UPDATE` 查询到的真实库存值。实际业务代码不能硬编码这些快照值。

#### 订单锁库存写入流水

订单锁库存会减少可用库存、增加锁定库存，总库存不变。该操作必须防止并发超卖。

```sql
START TRANSACTION;

-- 1. 锁定库存行
SELECT
    id,
    inventory_no,
    total_quantity,
    available_quantity,
    locked_quantity
FROM biz_inventory
WHERE sku_id = 10010001
  AND warehouse_id = 20001
  AND batch_no = ''
  AND inventory_status = 1
FOR UPDATE;

-- 2. 更新库存表，确保可用库存充足
UPDATE biz_inventory
SET
    available_quantity = available_quantity - 3,
    locked_quantity = locked_quantity + 3,
    version = version + 1,
    updated_at = NOW()
WHERE id = 3000001
  AND available_quantity >= 3;

-- 3. 影响行数为1时插入库存流水
INSERT INTO biz_inventory_flow (
    id,
    flow_no,
    inventory_id,
    inventory_no,
    spu_id,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_quantity,
    total_before,
    total_after,
    available_before,
    available_after,
    locked_before,
    locked_after,
    operator_id,
    operator_name,
    flow_status,
    remark
) VALUES (
    4000002,
    'IFLOW202501010002',
    3000001,
    'INV202501010001',
    10001,
    10010001,
    20001,
    'WH_SH_001',
    '',
    'REQ_ORDER_LOCK_202501010001_10010001',
    'ORDER_LOCK',
    'ORDER202501010001',
    'LOCK',
    -3,
    150,
    150,
    150,
    147,
    0,
    3,
    NULL,
    NULL,
    1,
    '订单创建锁定库存'
);

COMMIT;
```

锁库存失败时不能插入库存流水。应用层应根据库存更新影响行数判断是否成功，影响行数为 `0` 时回滚事务并返回库存不足。

#### 订单取消释放库存写入流水

订单取消、支付超时、审核拒绝等场景需要释放锁定库存。释放库存会增加可用库存、减少锁定库存，总库存不变。

```sql
START TRANSACTION;

SELECT
    id,
    inventory_no,
    total_quantity,
    available_quantity,
    locked_quantity
FROM biz_inventory
WHERE id = 3000001
FOR UPDATE;

UPDATE biz_inventory
SET
    available_quantity = available_quantity + 3,
    locked_quantity = locked_quantity - 3,
    version = version + 1,
    updated_at = NOW()
WHERE id = 3000001
  AND locked_quantity >= 3;

INSERT INTO biz_inventory_flow (
    id,
    flow_no,
    inventory_id,
    inventory_no,
    spu_id,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_quantity,
    total_before,
    total_after,
    available_before,
    available_after,
    locked_before,
    locked_after,
    operator_id,
    operator_name,
    flow_status,
    remark
) VALUES (
    4000003,
    'IFLOW202501010003',
    3000001,
    'INV202501010001',
    10001,
    10010001,
    20001,
    'WH_SH_001',
    '',
    'REQ_ORDER_RELEASE_202501010001_10010001',
    'ORDER_RELEASE',
    'ORDER202501010001',
    'RELEASE',
    3,
    150,
    150,
    147,
    150,
    3,
    0,
    NULL,
    NULL,
    1,
    '订单取消释放库存'
);

COMMIT;
```

释放库存必须具备幂等控制。订单取消消息、支付超时任务、人工取消操作可能重复触发，如果没有 `request_no` 唯一约束，容易导致重复释放。

#### 销售出库写入流水

销售出库通常发生在支付成功、发货确认或WMS出库完成后。如果业务已经提前锁定库存，出库时应从锁定库存扣减，并减少总库存。

```sql
START TRANSACTION;

SELECT
    id,
    inventory_no,
    total_quantity,
    available_quantity,
    locked_quantity
FROM biz_inventory
WHERE id = 3000001
FOR UPDATE;

UPDATE biz_inventory
SET
    total_quantity = total_quantity - 3,
    locked_quantity = locked_quantity - 3,
    version = version + 1,
    updated_at = NOW()
WHERE id = 3000001
  AND locked_quantity >= 3;

INSERT INTO biz_inventory_flow (
    id,
    flow_no,
    inventory_id,
    inventory_no,
    spu_id,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_quantity,
    total_before,
    total_after,
    available_before,
    available_after,
    locked_before,
    locked_after,
    operator_id,
    operator_name,
    flow_status,
    remark
) VALUES (
    4000004,
    'IFLOW202501010004',
    3000001,
    'INV202501010001',
    10001,
    10010001,
    20001,
    'WH_SH_001',
    '',
    'REQ_SALE_OUT_202501010001_10010001',
    'SALE_OUT',
    'DELIVERY202501010001',
    'OUT',
    -3,
    150,
    147,
    147,
    147,
    3,
    0,
    NULL,
    NULL,
    1,
    '销售出库扣减库存'
);

COMMIT;
```

如果业务没有锁库存流程，销售出库也可以直接从可用库存扣减。但订单链路较长、支付异步、履约异步的系统，通常建议先锁库存，再出库扣减。

#### 退货入库写入流水

退货入库会增加总库存和可用库存，通常关联退货单、售后单或入库单。

```sql
START TRANSACTION;

SELECT
    id,
    inventory_no,
    total_quantity,
    available_quantity,
    locked_quantity
FROM biz_inventory
WHERE id = 3000001
FOR UPDATE;

UPDATE biz_inventory
SET
    total_quantity = total_quantity + 2,
    available_quantity = available_quantity + 2,
    version = version + 1,
    updated_at = NOW()
WHERE id = 3000001;

INSERT INTO biz_inventory_flow (
    id,
    flow_no,
    inventory_id,
    inventory_no,
    spu_id,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_quantity,
    total_before,
    total_after,
    available_before,
    available_after,
    locked_before,
    locked_after,
    operator_id,
    operator_name,
    flow_status,
    remark
) VALUES (
    4000005,
    'IFLOW202501010005',
    3000001,
    'INV202501010001',
    10001,
    10010001,
    20001,
    'WH_SH_001',
    '',
    'REQ_RETURN_IN_202501010001_10010001',
    'RETURN_IN',
    'RETURN202501010001',
    'IN',
    2,
    147,
    149,
    147,
    149,
    0,
    0,
    90002,
    '售后管理员',
    1,
    '退货入库'
);

COMMIT;
```

退货入库是否直接进入可用库存，需要根据质检流程决定。如果退货商品需要质检，应先进入不可售库存或独立库存状态，而不是直接增加可用库存。

#### 库存调整写入流水

库存调整用于盘点差异、历史数据修复、人工调账等场景。库存调整风险较高，应限制权限，并记录清楚调整原因。

```sql
START TRANSACTION;

SELECT
    id,
    inventory_no,
    total_quantity,
    available_quantity,
    locked_quantity
FROM biz_inventory
WHERE id = 3000001
FOR UPDATE;

UPDATE biz_inventory
SET
    total_quantity = 160,
    available_quantity = 155,
    locked_quantity = 5,
    version = version + 1,
    remark = '盘点后调整库存',
    updated_at = NOW()
WHERE id = 3000001;

INSERT INTO biz_inventory_flow (
    id,
    flow_no,
    inventory_id,
    inventory_no,
    spu_id,
    sku_id,
    warehouse_id,
    warehouse_code,
    batch_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_quantity,
    total_before,
    total_after,
    available_before,
    available_after,
    locked_before,
    locked_after,
    operator_id,
    operator_name,
    flow_status,
    remark
) VALUES (
    4000006,
    'IFLOW202501010006',
    3000001,
    'INV202501010001',
    10001,
    10010001,
    20001,
    'WH_SH_001',
    '',
    'REQ_ADJUST_202501010001_10010001',
    'ADJUST',
    'ADJUST202501010001',
    'ADJUST',
    11,
    149,
    160,
    149,
    155,
    0,
    5,
    90003,
    '库存管理员',
    1,
    '盘点差异调整库存'
);

COMMIT;
```

调整库存时，`change_quantity` 可以表示总库存变化量，也可以表示可用库存变化量。建议在团队规范中固定含义，避免后续统计口径混乱。

#### 幂等写入

库存流水必须支持幂等写入。订单锁库存、订单释放、销售出库、退货入库等动作都可能因为接口重试、MQ重复投递、任务补偿而重复执行。

幂等查询示例：

```sql
SELECT
    flow_no,
    inventory_id,
    inventory_no,
    sku_id,
    warehouse_id,
    batch_no,
    request_no,
    biz_type,
    biz_no,
    change_type,
    change_quantity,
    flow_status,
    created_at
FROM biz_inventory_flow
WHERE request_no = 'REQ_ORDER_LOCK_202501010001_10010001';
```

应用层建议流程：

```text
1. 根据业务动作生成 request_no。
2. 开启数据库事务。
3. 查询 request_no 是否已有流水，已有则直接返回原处理结果。
4. 锁定库存行，读取变动前库存快照。
5. 校验库存状态和库存数量。
6. 更新库存表。
7. 插入库存流水。
8. 提交事务。
```

如果采用“先更新库存，再插入流水”的方式，必须保证插入流水发生唯一键冲突时回滚整个事务，否则会出现库存已变更但流水没有新增的问题。

### 常见问题

库存流水模型的常见问题集中在重复写入、流水和库存不一致、变动类型混乱、快照缺失、批次维度错误和大表查询性能等方面。

| 问题               | 原因                                     | 建议                                                   |
| ------------------ | ---------------------------------------- | ------------------------------------------------------ |
| 重复锁库存         | MQ重复消费或接口重试没有幂等控制         | 使用 `request_no` 唯一索引                             |
| 重复释放库存       | 取消订单、超时任务、人工操作重复触发     | 每个释放动作生成稳定的幂等请求号                       |
| 库存表和流水不一致 | 库存更新和流水写入不在同一事务           | 库存更新与流水插入必须同事务提交                       |
| 无法还原历史库存   | 流水只记录变动数量，没有记录变动前后快照 | 保存 `before` 和 `after` 快照字段                      |
| 变动类型统计混乱   | `biz_type` 和 `change_type` 使用不规范   | 明确业务类型和变动类型的枚举含义                       |
| 查询库存流水很慢   | 缺少库存ID、SKU、仓库、时间范围索引      | 按常用查询条件建立组合索引                             |
| 批次库存对不上     | 流水和库存表批次号不一致                 | 库存表与流水表统一使用同一批次字段                     |
| 历史流水被修改     | 后台直接更新流水核心数量字段             | 流水只追加，冲正通过新增反向流水处理                   |
| 对账口径不一致     | 订单、WMS、ERP使用不同业务单号           | 保留 `biz_type`、`biz_no`、`request_no` 并统一映射规则 |
| 大表归档困难       | 流水表没有时间索引或分区策略             | 建立 `created_at` 索引，必要时按月归档或分区           |

库存流水中的 `biz_type` 和 `change_type` 不应混用。`biz_type` 表示业务来源，例如订单锁定、采购入库、销售出库；`change_type` 表示库存动作，例如入库、锁定、释放、出库、调整。两者职责不同，分开设计有利于查询、统计和排查问题。

### 总结

库存流水模型的核心是记录库存每一次变化的业务来源、变动数量和变动前后快照。库存表保存当前库存，库存流水表保存库存变化过程，两者必须在同一个事务中写入，不能只更新其中一方。

库存流水表应采用追加写入模式，并通过 `request_no` 唯一索引保证幂等。对于订单锁库存、释放库存、销售出库、退货入库等高频业务，必须通过数据库条件更新和事务保证并发安全。

库存流水模型与库存模型配合后，可以支撑库存查询、库存审计、库存对账、异常排查和后续归档统计。账户流水、库存、库存流水三类模型的共同原则是：状态表保存当前值，流水表保存变化过程，写入链路保证事务一致性和幂等安全。