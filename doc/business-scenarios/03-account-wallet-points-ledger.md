# 账户余额 / 钱包 / 积分账户

本文实现一个可直接落地到 Java 后端项目中的账户余额模块，覆盖开户、入账、出账、冻结、解冻、冻结扣减、退款冲正、账户流水和幂等控制等核心能力。技术栈以 Spring Boot、MyBatis-Plus、MySQL、Redis、Redisson、BigDecimal、数据库乐观锁和唯一索引为主，贴合原 README 中“账户余额 / 钱包 / 积分账户”场景的能力要求。

## 案例目标与功能边界

本案例的目标不是做一个完整支付系统，也不是做复杂财务总账系统，而是实现业务系统中最常见的“账户余额核心层”。它可以用于钱包余额、会员积分、虚拟币、预付款账户、额度账户等场景。

核心设计原则是：账户余额必须准确，账户流水必须完整，重复请求必须幂等，并发扣减不能导致余额为负。

### 核心业务能力

本案例主要实现账户模块的核心交易能力，重点放在账户余额变更、冻结金额处理、账户流水记录和并发一致性控制。

具体能力如下：

| 功能         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 账户开户     | 根据用户 ID 和账户类型创建账户，保证同一用户同一账户类型只能创建一个账户 |
| 入账         | 增加账户可用余额，例如充值到账、积分发放、退款到账           |
| 出账         | 扣减账户可用余额，例如余额支付、积分兑换、额度消费           |
| 冻结余额     | 将可用余额转为冻结余额，例如下单后未支付确认前先冻结账户资金 |
| 解冻余额     | 将冻结余额退回可用余额，例如订单取消、支付超时、业务失败回滚 |
| 扣减冻结金额 | 从冻结余额中正式扣减，例如订单支付确认、服务履约完成         |
| 退款冲正     | 基于原业务单号进行反向入账，用于退款、撤销、补偿             |
| 账户流水     | 每一次账户变动都生成流水，记录变动前后余额、业务单号、流水类型 |
| 幂等控制     | 同一个业务单号重复请求时，不重复修改余额                     |
| 并发控制     | 使用 Redisson 锁、数据库乐观锁、唯一索引防止并发扣减错误     |
| 余额校验     | 出账、冻结、冻结扣减时校验余额，避免可用余额或冻结余额为负   |

核心账户模型只维护两个余额字段：

| 字段              | 含义                                           |
| ----------------- | ---------------------------------------------- |
| available_balance | 可用余额，可以直接消费、冻结或提现             |
| frozen_balance    | 冻结余额，已经被业务占用，但尚未最终扣减或释放 |

账户余额变更遵循以下规则：

```text
入账：
available_balance = available_balance + amount

出账：
available_balance = available_balance - amount

冻结：
available_balance = available_balance - amount
frozen_balance = frozen_balance + amount

解冻：
available_balance = available_balance + amount
frozen_balance = frozen_balance - amount

扣减冻结：
frozen_balance = frozen_balance - amount

退款冲正：
available_balance = available_balance + amount
```

本案例会重点保证以下业务约束：

```text
账户不存在不能操作余额
金额必须大于 0
可用余额不能扣成负数
冻结余额不能扣成负数
同一业务单号不能重复入账或重复扣减
账户余额变更和账户流水必须在同一个事务内完成
账户流水只新增，不修改，不删除
```

### 不在本案例实现的扩展能力

为了保持案例聚焦，本案例只实现账户余额核心能力，不展开支付、清结算、财务总账、复杂对账等外围系统。

以下能力不在本案例中实现：

| 不实现内容     | 说明                                                       |
| -------------- | ---------------------------------------------------------- |
| 第三方支付接入 | 不接入微信、支付宝、银行卡、支付渠道 SDK                   |
| 支付回调处理   | 支付成功后的回调处理属于支付模块，不放到账户核心模块中     |
| 复杂对账系统   | 不实现支付渠道对账、财务日切、差错账处理                   |
| 财务总账       | 不实现会计科目、借贷平衡、凭证、账期、结算单               |
| 多币种账户     | 默认所有金额使用同一种业务单位，不处理汇率换算             |
| 多级账户体系   | 不实现平台账户、商户账户、用户账户之间的清分结算           |
| 账户提现       | 不实现提现申请、审核、打款、提现手续费                     |
| 风控规则       | 不实现黑名单、限额、异常交易拦截等风控逻辑                 |
| MQ 最终一致性  | 本案例优先使用本地事务保证账户和流水一致，不展开可靠消息表 |
| 分库分表       | 默认单库单表实现，不处理海量账户流水分片                   |
| 后台管理页面   | 只实现后端接口和核心服务，不提供前端管理页面               |

本案例适合先作为单体 Spring Boot 项目的账户核心模块实现。后续如果接入订单、支付、积分商城或会员系统，可以让其他业务模块通过业务单号调用账户服务，账户模块只负责“钱或积分怎么变、流水怎么记、重复请求怎么防”。

## 技术栈与工程结构

本案例采用常见 Spring Boot 单体项目结构实现账户核心模块，后续可以平滑拆分为独立账户服务。账户余额属于强一致业务，核心余额变更优先使用 MySQL 本地事务、乐观锁和唯一索引控制，Redis / Redisson 主要用于降低并发冲突和防止同一账户并发写入。

### 技术栈选型

| 技术                | 用途                                    |
| ------------------- | --------------------------------------- |
| Spring Boot 3       | 项目基础框架                            |
| MyBatis-Plus        | 数据库 CRUD、分页、条件构造             |
| MySQL 8             | 存储账户、流水、幂等记录                |
| Redis               | 幂等缓存、热点账户辅助控制              |
| Redisson            | 分布式锁，控制同一账户并发写入          |
| BigDecimal          | 金额精确计算                            |
| Hutool              | 参数校验、字符串、日期、JSON 等工具处理 |
| Lombok              | 简化实体类、DTO、VO 代码                |
| Hibernate Validator | 请求参数校验                            |
| Knife4j / Springdoc | 接口文档，可选                          |
| Logback             | 业务日志记录                            |

本案例的核心一致性策略如下：

```text
接口请求
-> 参数校验
-> 生成幂等 Key
-> 获取账户维度 Redisson 锁
-> 开启数据库事务
-> 查询账户
-> 校验余额
-> 使用乐观锁更新账户余额
-> 写入账户流水
-> 写入幂等记录
-> 提交事务
-> 释放分布式锁
```

账户模块不依赖 MQ 才能完成核心交易，核心余额和流水必须在本地事务中同时成功或同时失败。MQ 更适合后续扩展通知、积分到账消息、账户变动事件等异步场景。

### 项目目录结构

下面是账户模块的推荐目录结构，包名统一使用 `io.github.atengk.wallet`，后续代码实现会基于该结构展开。

```text
src/main/java/io/github/atengk/wallet
├── WalletApplication.java
├── common
│   ├── enums
│   │   ├── AccountTypeEnum.java
│   │   ├── AccountFlowTypeEnum.java
│   │   └── IdempotentStatusEnum.java
│   ├── exception
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   └── result
│       └── Result.java
├── config
│   ├── MybatisPlusConfig.java
│   └── RedissonConfig.java
├── controller
│   └── AccountController.java
├── domain
│   ├── dto
│   │   ├── AccountOpenDTO.java
│   │   ├── AccountIncomeDTO.java
│   │   ├── AccountOutcomeDTO.java
│   │   ├── AccountFreezeDTO.java
│   │   ├── AccountUnfreezeDTO.java
│   │   ├── AccountFrozenDeductDTO.java
│   │   └── AccountReverseDTO.java
│   ├── entity
│   │   ├── AccountBalance.java
│   │   ├── AccountFlow.java
│   │   └── AccountIdempotent.java
│   └── vo
│       ├── AccountBalanceVO.java
│       └── AccountFlowVO.java
├── mapper
│   ├── AccountBalanceMapper.java
│   ├── AccountFlowMapper.java
│   └── AccountIdempotentMapper.java
└── service
    ├── AccountService.java
    └── impl
        └── AccountServiceImpl.java
```

核心文件职责如下：

| 文件                 | 作用                                         |
| -------------------- | -------------------------------------------- |
| `AccountBalance`     | 账户余额实体，保存可用余额、冻结余额、版本号 |
| `AccountFlow`        | 账户流水实体，记录每次余额变更               |
| `AccountIdempotent`  | 幂等记录实体，防止重复请求                   |
| `AccountServiceImpl` | 核心账户业务实现                             |
| `AccountController`  | 对外提供账户操作接口                         |
| `RedissonConfig`     | 初始化 Redisson 客户端                       |
| `MybatisPlusConfig`  | 配置 MyBatis-Plus 分页、乐观锁插件           |

### Maven 依赖配置

下面依赖放在项目根目录 `pom.xml` 中，版本可以统一交给 Spring Boot Parent 和 MyBatis-Plus 管理。Redisson、Hutool、Knife4j 等依赖根据项目实际版本维护即可。

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST 接口能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于 DTO 字段校验，例如金额必须大于 0 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- MyBatis-Plus：简化 CRUD、条件查询、分页和乐观锁 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 8 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Redisson：分布式锁，控制同一账户并发操作 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.32.0</version>
    </dependency>

    <!-- Hutool：常用工具类，处理字符串、日期、JSON、集合等 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.29</version>
    </dependency>

    <!-- Lombok：减少 getter、setter、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Knife4j：接口文档，可选 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>4.5.0</version>
    </dependency>

    <!-- Spring Boot Test：接口和 Service 测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目没有使用 Spring Boot Parent，需要额外声明 Spring Boot、MyBatis-Plus、Redisson 等版本管理。实际生产项目建议统一在父工程中集中维护版本，避免多个模块依赖版本不一致。

### Spring Boot 配置

下面配置放在 `src/main/resources/application.yml` 中，用于连接 MySQL、Redis，并开启 MyBatis-Plus 基础配置。

```yaml
server:
  port: 8080

spring:
  application:
    name: wallet-account-demo

  datasource:
    # MySQL 连接地址，根据本地环境修改库名、地址和时区
    url: jdbc:mysql://127.0.0.1:3306/wallet_demo?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      # Redis 用于 Redisson 分布式锁和幂等辅助缓存
      host: 127.0.0.1
      port: 6379
      database: 0
      timeout: 3000ms

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: io.github.atengk.wallet.domain.entity
  configuration:
    # 控制台打印 SQL，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # 数据库下划线字段自动映射 Java 驼峰字段
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      # 主键使用数据库自增
      id-type: auto
      # 逻辑删除字段，本案例暂不启用逻辑删除
      logic-delete-field: deleted

redisson:
  # 单机 Redis 配置，生产环境可替换为 sentinelServersConfig 或 clusterServersConfig
  config: |
    singleServerConfig:
      address: "redis://127.0.0.1:6379"
      database: 0
      connectionMinimumIdleSize: 8
      connectionPoolSize: 32
      idleConnectionTimeout: 10000
      connectTimeout: 3000
      timeout: 3000
    threads: 8
    nettyThreads: 16

knife4j:
  enable: true
  setting:
    # 接口文档基础增强配置
    language: zh_cn
```

账户模块建议统一约定 Redis Key 前缀，避免和其他模块冲突：

```text
wallet:lock:account:{accountId}        # 账户操作分布式锁
wallet:idem:{requestNo}                # 幂等请求缓存
wallet:account:snapshot:{accountId}    # 账户快照缓存，可选
```

## 数据库表设计

账户模块至少需要三张表：账户表、账户流水表、幂等记录表。账户表保存当前余额，账户流水表保存每次余额变化，幂等记录表保存业务请求处理结果。

### 账户表设计

账户表保存用户某一种账户的当前余额状态。同一用户同一账户类型只允许存在一条账户记录。

```sql
CREATE TABLE account_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    account_no VARCHAR(64) NOT NULL COMMENT '账户编号，全局唯一',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    account_type VARCHAR(32) NOT NULL COMMENT '账户类型：WALLET 钱包，POINT 积分',
    available_balance DECIMAL(20, 6) NOT NULL DEFAULT 0.000000 COMMENT '可用余额',
    frozen_balance DECIMAL(20, 6) NOT NULL DEFAULT 0.000000 COMMENT '冻结余额',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '账户状态：1 正常，0 禁用',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',

    UNIQUE KEY uk_account_no (account_no),
    UNIQUE KEY uk_user_account_type (user_id, account_type, deleted),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户余额表';
```

字段说明：

| 字段                | 说明                                           |
| ------------------- | ---------------------------------------------- |
| `account_no`        | 账户编号，建议使用业务编号生成器或雪花 ID 生成 |
| `user_id`           | 账户归属用户                                   |
| `account_type`      | 账户类型，例如钱包、积分                       |
| `available_balance` | 当前可用余额                                   |
| `frozen_balance`    | 当前冻结余额                                   |
| `status`            | 账户是否可用，禁用后不能入账、出账、冻结       |
| `version`           | 乐观锁版本号，防止并发覆盖                     |
| `deleted`           | 逻辑删除标识                                   |

余额字段使用 `DECIMAL(20, 6)`，兼容金额和积分。普通人民币金额可以保留两位小数，积分可以按业务规则保留零位或多位小数。

### 账户流水表设计

账户流水表记录每一次账户余额变化，是账户系统的核心审计依据。流水只允许新增，不允许修改和删除。

```sql
CREATE TABLE account_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    flow_no VARCHAR(64) NOT NULL COMMENT '流水编号，全局唯一',
    account_id BIGINT NOT NULL COMMENT '账户 ID',
    account_no VARCHAR(64) NOT NULL COMMENT '账户编号',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    account_type VARCHAR(32) NOT NULL COMMENT '账户类型',
    flow_type VARCHAR(32) NOT NULL COMMENT '流水类型',
    direction VARCHAR(16) NOT NULL COMMENT '资金方向：IN 入账，OUT 出账，FREEZE 冻结，UNFREEZE 解冻',
    amount DECIMAL(20, 6) NOT NULL COMMENT '本次变动金额',
    before_available_balance DECIMAL(20, 6) NOT NULL COMMENT '变动前可用余额',
    after_available_balance DECIMAL(20, 6) NOT NULL COMMENT '变动后可用余额',
    before_frozen_balance DECIMAL(20, 6) NOT NULL COMMENT '变动前冻结余额',
    after_frozen_balance DECIMAL(20, 6) NOT NULL COMMENT '变动后冻结余额',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务单号',
    origin_biz_no VARCHAR(64) DEFAULT NULL COMMENT '原业务单号，退款冲正时使用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_flow_no (flow_no),
    UNIQUE KEY uk_biz_flow_type (biz_no, flow_type),
    KEY idx_account_id_create_time (account_id, create_time),
    KEY idx_user_id_create_time (user_id, create_time),
    KEY idx_biz_no (biz_no),
    KEY idx_origin_biz_no (origin_biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户流水表';
```

关键设计点：

| 设计                      | 说明                                   |
| ------------------------- | -------------------------------------- |
| `flow_no` 唯一            | 每条流水全局唯一                       |
| `biz_no + flow_type` 唯一 | 防止同一业务单号重复生成同类流水       |
| 记录变动前后余额          | 方便排查账务问题，也方便做账户快照校验 |
| `origin_biz_no`           | 用于退款、冲正、撤销时关联原始业务单   |
| 流水只新增                | 禁止更新和删除，保证审计可追溯         |

流水表中的 `direction` 不直接等于收入或支出，它用于描述余额变化方向。比如冻结操作会同时减少可用余额、增加冻结余额，所以单独标记为 `FREEZE`。

### 幂等记录表设计

幂等记录表用于保存一次业务请求的处理状态。同一个幂等 Key 只能成功处理一次，重复请求直接返回已处理结果或提示处理中。

```sql
CREATE TABLE account_idempotent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    idem_key VARCHAR(128) NOT NULL COMMENT '幂等 Key',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务单号',
    request_type VARCHAR(32) NOT NULL COMMENT '请求类型：OPEN、INCOME、OUTCOME、FREEZE、UNFREEZE、FROZEN_DEDUCT、REVERSE',
    status TINYINT NOT NULL COMMENT '处理状态：0 处理中，1 成功，2 失败',
    response_body TEXT DEFAULT NULL COMMENT '成功响应结果 JSON',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_idem_key (idem_key),
    UNIQUE KEY uk_biz_request_type (biz_no, request_type),
    KEY idx_biz_no (biz_no),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户幂等记录表';
```

幂等 Key 推荐生成规则：

```text
账户类型 + 请求类型 + 业务单号

示例：
WALLET:INCOME:RECHARGE202605150001
WALLET:FREEZE:ORDER202605150001
POINT:OUTCOME:EXCHANGE202605150001
```

幂等状态处理规则：

| 状态       | 说明                                 | 重复请求处理               |
| ---------- | ------------------------------------ | -------------------------- |
| `0 处理中` | 请求已经进入处理流程，但事务尚未完成 | 返回“请求处理中”或稍后重试 |
| `1 成功`   | 请求已经成功处理                     | 直接返回上次处理结果       |
| `2 失败`   | 请求处理失败                         | 允许根据业务决定是否重试   |

核心余额变更操作更推荐以数据库唯一索引做最终幂等保障，而不是只依赖 Redis。Redis 可以减少重复请求进入数据库，但数据库唯一索引必须保留。

### 唯一索引与乐观锁设计

账户模块的并发安全不能只依赖单一手段，需要组合使用唯一索引、分布式锁和乐观锁。

唯一索引用于防重复：

```sql
-- 防止同一用户重复开户
UNIQUE KEY uk_user_account_type (user_id, account_type, deleted)

-- 防止同一流水编号重复
UNIQUE KEY uk_flow_no (flow_no)

-- 防止同一业务单号重复生成同类流水
UNIQUE KEY uk_biz_flow_type (biz_no, flow_type)

-- 防止同一幂等 Key 重复处理
UNIQUE KEY uk_idem_key (idem_key)

-- 防止同一业务单号重复执行同类请求
UNIQUE KEY uk_biz_request_type (biz_no, request_type)
```

乐观锁用于防止并发覆盖账户余额。账户更新时必须带上原版本号：

```sql
UPDATE account_balance
SET
    available_balance = available_balance - 100.000000,
    frozen_balance = frozen_balance + 100.000000,
    version = version + 1,
    update_time = NOW()
WHERE
    id = 1
    AND version = 3
    AND available_balance >= 100.000000
    AND deleted = 0;
```

如果返回影响行数为 `0`，说明可能出现以下情况：

```text
账户不存在
账户余额不足
账户版本号已经变化
账户已被删除
```

在 Service 中应统一将其转换为明确的业务异常，例如“账户余额不足或账户状态已变化，请重试”。

推荐并发控制组合：

| 场景             | 控制方式                               |
| ---------------- | -------------------------------------- |
| 重复开户         | 用户 ID + 账户类型唯一索引             |
| 重复入账         | 业务单号 + 流水类型唯一索引            |
| 重复出账         | 业务单号 + 流水类型唯一索引            |
| 同一账户并发扣减 | Redisson 账户锁 + 数据库乐观锁         |
| 余额不能为负     | SQL 条件 `available_balance >= amount` |
| 冻结余额不能为负 | SQL 条件 `frozen_balance >= amount`    |
| 请求重复提交     | 幂等表唯一索引 + Redis 辅助缓存        |

## 业务模型设计

业务模型主要围绕账户类型、流水类型、业务单号和金额规则展开。账户模块必须尽量保持通用，不直接绑定订单、支付、商城等具体业务。

### 账户类型

账户类型用于区分钱包余额、积分账户、虚拟币账户等不同资金或权益载体。

文件位置：`src/main/java/io/github/atengk/wallet/common/enums/AccountTypeEnum.java`

```java
package io.github.atengk.wallet.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账户类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum AccountTypeEnum {

    /**
     * 钱包余额账户
     */
    WALLET("钱包余额"),

    /**
     * 积分账户
     */
    POINT("积分账户"),

    /**
     * 虚拟币账户
     */
    COIN("虚拟币账户");

    private final String description;
}
```

账户类型建议通过枚举控制，不建议前端任意传字符串后直接入库。接口层接收账户类型后，应校验是否属于系统支持的类型。

典型使用场景：

```text
WALLET：余额充值、余额支付、余额退款
POINT：签到送积分、积分兑换、积分退回
COIN：平台虚拟币充值、消费、冻结
```

### 流水类型

流水类型用于描述账户余额变化的业务动作。流水类型越清晰，后续排查账务问题越方便。

文件位置：`src/main/java/io/github/atengk/wallet/common/enums/AccountFlowTypeEnum.java`

```java
package io.github.atengk.wallet.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账户流水类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum AccountFlowTypeEnum {

    /**
     * 开户初始化
     */
    OPEN("开户初始化"),

    /**
     * 入账
     */
    INCOME("入账"),

    /**
     * 出账
     */
    OUTCOME("出账"),

    /**
     * 冻结
     */
    FREEZE("冻结"),

    /**
     * 解冻
     */
    UNFREEZE("解冻"),

    /**
     * 扣减冻结金额
     */
    FROZEN_DEDUCT("扣减冻结金额"),

    /**
     * 退款冲正
     */
    REVERSE("退款冲正");

    private final String description;
}
```

流水类型和余额变更关系如下：

| 流水类型        | 可用余额变化 | 冻结余额变化 | 典型业务             |
| --------------- | ------------ | ------------ | -------------------- |
| `OPEN`          | 不变         | 不变         | 开户                 |
| `INCOME`        | 增加         | 不变         | 充值、积分发放       |
| `OUTCOME`       | 减少         | 不变         | 直接消费             |
| `FREEZE`        | 减少         | 增加         | 下单冻结余额         |
| `UNFREEZE`      | 增加         | 减少         | 取消订单释放冻结     |
| `FROZEN_DEDUCT` | 不变         | 减少         | 支付确认扣除冻结资金 |
| `REVERSE`       | 增加         | 不变         | 退款、撤销、补偿     |

冻结和出账的区别必须明确：

```text
出账：
钱直接从可用余额扣掉，适合简单消费场景。

冻结：
钱先从可用余额进入冻结余额，等业务确认后再从冻结余额扣掉。
适合订单、担保交易、预约占用等需要中间状态的场景。
```

### 业务单号

业务单号是账户模块实现幂等和追踪的关键字段。账户模块不应该自己理解订单、支付、退款等业务细节，而是通过业务单号关联外部系统。

业务单号设计建议：

| 来源业务   | 业务单号示例                  | 对应账户动作   |
| ---------- | ----------------------------- | -------------- |
| 充值单     | `RECHARGE202605150001`        | 入账           |
| 订单号     | `ORDER202605150001`           | 冻结、冻结扣减 |
| 退款单     | `REFUND202605150001`          | 退款冲正       |
| 积分任务   | `TASK_POINT_202605150001`     | 积分入账       |
| 积分兑换单 | `POINT_EXCHANGE_202605150001` | 出账           |

业务单号使用规则：

```text
同一个业务单号 + 同一个流水类型，只能生成一条账户流水
同一个业务单号 + 同一个请求类型，只能生成一条幂等记录
退款冲正必须记录 origin_biz_no，用于关联原始业务单
业务单号必须由调用方传入，账户模块不替调用方生成业务单号
```

示例：

```text
用户下单使用余额支付 100 元：

1. 订单系统创建订单：ORDER202605150001
2. 账户系统冻结余额：
   biz_no = ORDER202605150001
   flow_type = FREEZE

3. 支付确认成功：
   biz_no = ORDER202605150001
   flow_type = FROZEN_DEDUCT

4. 如果订单取消：
   biz_no = ORDER202605150001
   flow_type = UNFREEZE
```

如果是退款：

```text
原订单号：
ORDER202605150001

退款单号：
REFUND202605150001

账户冲正流水：
biz_no = REFUND202605150001
origin_biz_no = ORDER202605150001
flow_type = REVERSE
```

### 可用余额与冻结余额

账户余额拆分为可用余额和冻结余额，是为了支持“资金占用但未最终扣除”的业务场景。

余额字段含义：

| 字段                | 说明                             |
| ------------------- | -------------------------------- |
| `available_balance` | 当前可自由使用的余额             |
| `frozen_balance`    | 已被业务占用、暂时不可使用的余额 |

账户总余额可以通过下面公式得到：

```text
total_balance = available_balance + frozen_balance
```

不同业务动作对余额的影响如下：

```text
入账 100：
available_balance: 0 -> 100
frozen_balance:    0 -> 0

冻结 30：
available_balance: 100 -> 70
frozen_balance:    0 -> 30

解冻 30：
available_balance: 70 -> 100
frozen_balance:    30 -> 0

扣减冻结 30：
available_balance: 70 -> 70
frozen_balance:    30 -> 0

出账 20：
available_balance: 100 -> 80
frozen_balance:    0 -> 0
```

余额变更必须遵守以下约束：

```text
available_balance >= 0
frozen_balance >= 0
amount > 0
账户更新和流水写入在同一个事务中
不允许只改余额不记流水
不允许只记流水不改余额
```

### 金额精度规则

账户金额必须统一使用 `BigDecimal`，禁止使用 `double` 或 `float`。数据库字段使用 `DECIMAL(20, 6)`，Java 代码中使用统一工具方法处理金额精度。

推荐金额规则：

| 账户类型   | 精度            | 示例            |
| ---------- | --------------- | --------------- |
| 钱包余额   | 2 位小数        | `100.00`        |
| 积分账户   | 0 位或 2 位小数 | `100`、`100.50` |
| 虚拟币账户 | 6 位小数        | `10.123456`     |

金额处理原则：

```text
入库前统一 setScale
金额必须大于 0
比较金额使用 compareTo
禁止使用 equals 判断 BigDecimal 数值相等
禁止使用 new BigDecimal(double)
推荐使用字符串创建 BigDecimal
```

文件位置：`src/main/java/io/github/atengk/wallet/common/util/MoneyUtil.java`

```java
package io.github.atengk.wallet.common.util;

import cn.hutool.core.util.NumberUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MoneyUtil {

    /**
     * 默认金额精度
     */
    private static final int DEFAULT_SCALE = 6;

    private MoneyUtil() {
    }

    /**
     * 规范化金额，统一保留 6 位小数
     *
     * @param amount 金额
     * @return 规范化后的金额
     */
    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(DEFAULT_SCALE, RoundingMode.DOWN);
        }
        return amount.setScale(DEFAULT_SCALE, RoundingMode.DOWN);
    }

    /**
     * 判断金额是否大于 0
     *
     * @param amount 金额
     * @return 是否大于 0
     */
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && NumberUtil.isGreater(amount, BigDecimal.ZERO);
    }

    /**
     * 判断余额是否足够
     *
     * @param balance 当前余额
     * @param amount  扣减金额
     * @return 是否足够
     */
    public static boolean isEnough(BigDecimal balance, BigDecimal amount) {
        if (balance == null || amount == null) {
            return false;
        }
        return NumberUtil.isGreaterOrEqual(balance, amount);
    }
}
```

典型金额写法：

```java
BigDecimal amount = new BigDecimal("100.00");
BigDecimal normalizedAmount = MoneyUtil.normalize(amount);

if (!MoneyUtil.isPositive(normalizedAmount)) {
    throw new BusinessException("金额必须大于0");
}
```

错误写法：

```java
// 错误：double 可能存在精度问题
BigDecimal amount = new BigDecimal(100.01);

// 错误：equals 会比较精度，100.0 和 100.00 可能不相等
boolean same = new BigDecimal("100.0").equals(new BigDecimal("100.00"));
```

正确写法：

```java
BigDecimal amount = new BigDecimal("100.01");

// 正确：compareTo 只比较数值大小
boolean same = new BigDecimal("100.0").compareTo(new BigDecimal("100.00")) == 0;
```

## 核心接口设计

账户模块对外暴露的是“账户操作接口”，调用方只需要关心用户、账户类型、业务单号和金额。账户模块内部负责余额校验、幂等、防并发、流水记录和事务一致性。这里的接口设计对应原 README 中“账户开户、入账、出账、冻结、解冻、扣减冻结金额、退款冲正、生成账户流水”的核心链路。

接口统一前缀：

```text
/api/accounts
```

接口概览如下：

| 接口         | 请求方式 | 路径                          | 说明                     |
| ------------ | -------- | ----------------------------- | ------------------------ |
| 账户开户     | POST     | `/api/accounts/open`          | 为用户创建指定类型账户   |
| 入账         | POST     | `/api/accounts/income`        | 增加账户可用余额         |
| 出账         | POST     | `/api/accounts/outcome`       | 扣减账户可用余额         |
| 冻结余额     | POST     | `/api/accounts/freeze`        | 可用余额转冻结余额       |
| 解冻余额     | POST     | `/api/accounts/unfreeze`      | 冻结余额退回可用余额     |
| 扣减冻结金额 | POST     | `/api/accounts/frozen-deduct` | 从冻结余额正式扣减       |
| 退款冲正     | POST     | `/api/accounts/reverse`       | 反向入账，关联原业务单号 |
| 账户流水查询 | GET      | `/api/accounts/flows`         | 分页查询账户流水         |

### 账户开户接口

账户开户用于给用户初始化账户。同一个 `userId + accountType` 只能开户一次，重复开户直接返回已有账户。

```http
POST /api/accounts/open
Content-Type: application/json
```

请求参数：

```json
{
  "userId": 10001,
  "accountType": "WALLET"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "accountNo": "AC2026051510010001",
    "userId": 10001,
    "accountType": "WALLET",
    "availableBalance": "0.000000",
    "frozenBalance": "0.000000",
    "status": 1
  }
}
```

核心规则：

```text
userId 不能为空
accountType 必须是系统支持的账户类型
同一用户同一账户类型只能创建一个账户
重复开户直接返回已有账户，不抛异常
```

### 入账接口

入账用于充值到账、积分发放、退款到账等场景。入账只增加可用余额，不影响冻结余额。

```http
POST /api/accounts/income
Content-Type: application/json
```

请求参数：

```json
{
  "userId": 10001,
  "accountType": "WALLET",
  "amount": "100.00",
  "bizNo": "RECHARGE202605150001",
  "remark": "用户充值到账"
}
```

核心规则：

```text
amount 必须大于 0
bizNo 不能为空
同一个 bizNo + INCOME 只能入账一次
重复入账请求直接返回当前账户余额
账户余额和账户流水必须同时成功
```

### 出账接口

出账用于余额直接支付、积分兑换、额度消费等场景。出账会直接扣减可用余额。

```http
POST /api/accounts/outcome
Content-Type: application/json
```

请求参数：

```json
{
  "userId": 10001,
  "accountType": "WALLET",
  "amount": "30.00",
  "bizNo": "PAY202605150001",
  "remark": "余额直接支付"
}
```

核心规则：

```text
available_balance >= amount
同一个 bizNo + OUTCOME 只能出账一次
余额不足时拒绝扣减
并发扣减不能把余额扣成负数
```

### 冻结余额接口

冻结余额用于订单创建、预约占用、担保交易等场景。冻结时会减少可用余额，增加冻结余额。

```http
POST /api/accounts/freeze
Content-Type: application/json
```

请求参数：

```json
{
  "userId": 10001,
  "accountType": "WALLET",
  "amount": "50.00",
  "bizNo": "ORDER202605150001",
  "remark": "订单余额冻结"
}
```

余额变化：

```text
available_balance = available_balance - amount
frozen_balance = frozen_balance + amount
```

### 解冻余额接口

解冻余额用于订单取消、支付超时、业务失败回滚等场景。解冻时会减少冻结余额，增加可用余额。

```http
POST /api/accounts/unfreeze
Content-Type: application/json
```

请求参数：

```json
{
  "userId": 10001,
  "accountType": "WALLET",
  "amount": "50.00",
  "bizNo": "ORDER202605150001",
  "remark": "订单取消解冻余额"
}
```

核心规则：

```text
frozen_balance >= amount
同一个 bizNo + UNFREEZE 只能执行一次
解冻金额不能超过当前冻结余额
```

### 扣减冻结金额接口

扣减冻结金额用于订单确认、支付完成、服务履约完成等场景。它只扣减冻结余额，不影响可用余额。

```http
POST /api/accounts/frozen-deduct
Content-Type: application/json
```

请求参数：

```json
{
  "userId": 10001,
  "accountType": "WALLET",
  "amount": "50.00",
  "bizNo": "ORDER202605150001",
  "remark": "订单支付确认扣减冻结金额"
}
```

余额变化：

```text
available_balance 不变
frozen_balance = frozen_balance - amount
```

适用链路：

```text
创建订单
-> 冻结余额
-> 支付确认 / 履约确认
-> 扣减冻结金额
```

### 退款冲正接口

退款冲正用于退款、撤销、异常补偿等场景。冲正通常会增加可用余额，并通过 `originBizNo` 关联原始业务单号。

```http
POST /api/accounts/reverse
Content-Type: application/json
```

请求参数：

```json
{
  "userId": 10001,
  "accountType": "WALLET",
  "amount": "50.00",
  "bizNo": "REFUND202605150001",
  "originBizNo": "ORDER202605150001",
  "remark": "订单退款冲正"
}
```

核心规则：

```text
bizNo 是本次退款单号
originBizNo 是原订单号或原支付单号
同一个 bizNo + REVERSE 只能冲正一次
冲正默认增加可用余额
```

### 账户流水查询接口

账户流水查询用于用户账单、后台审计、问题排查等场景。建议按账户、用户、业务单号、流水类型和时间范围分页查询。

```http
GET /api/accounts/flows?userId=10001&accountType=WALLET&pageNo=1&pageSize=10
```

响应示例：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "records": [
      {
        "flowNo": "FL2026051510010001",
        "accountNo": "AC2026051510010001",
        "userId": 10001,
        "accountType": "WALLET",
        "flowType": "INCOME",
        "amount": "100.000000",
        "beforeAvailableBalance": "0.000000",
        "afterAvailableBalance": "100.000000",
        "beforeFrozenBalance": "0.000000",
        "afterFrozenBalance": "0.000000",
        "bizNo": "RECHARGE202605150001",
        "remark": "用户充值到账"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

## 核心代码实现

下面给出账户模块的关键代码。代码按 Spring Boot 常见分层组织：Entity、Mapper、DTO、VO、Service、ServiceImpl、Controller。为了控制篇幅，这里只实现账户核心链路，不展开全局异常、鉴权、审计、MQ 通知等外围能力。

### Entity 实体类

实体类对应前面设计的三张表：`account_balance`、`account_flow`、`account_idempotent`。账户表保存当前余额，流水表保存余额变更记录，幂等表保存请求处理记录。

`AccountBalance` 对应账户余额表，包含可用余额、冻结余额、状态和乐观锁版本号。

文件位置：`src/main/java/io/github/atengk/wallet/domain/entity/AccountBalance.java`

```java
package io.github.atengk.wallet.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户余额实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("account_balance")
public class AccountBalance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String accountNo;

    private Long userId;

    private String accountType;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private Integer status;

    @Version
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

`AccountFlow` 对应账户流水表，每次余额变化都写入一条流水。

文件位置：`src/main/java/io/github/atengk/wallet/domain/entity/AccountFlow.java`

```java
package io.github.atengk.wallet.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户流水实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("account_flow")
public class AccountFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String flowNo;

    private Long accountId;

    private String accountNo;

    private Long userId;

    private String accountType;

    private String flowType;

    private String direction;

    private BigDecimal amount;

    private BigDecimal beforeAvailableBalance;

    private BigDecimal afterAvailableBalance;

    private BigDecimal beforeFrozenBalance;

    private BigDecimal afterFrozenBalance;

    private String bizNo;

    private String originBizNo;

    private String remark;

    private LocalDateTime createTime;
}
```

`AccountIdempotent` 对应幂等记录表，用于记录请求处理状态。

文件位置：`src/main/java/io/github/atengk/wallet/domain/entity/AccountIdempotent.java`

```java
package io.github.atengk.wallet.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账户幂等记录实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("account_idempotent")
public class AccountIdempotent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String idemKey;

    private String bizNo;

    private String requestType;

    private Integer status;

    private String responseBody;

    private String errorMessage;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

### Mapper 持久层

Mapper 层继承 MyBatis-Plus 的 `BaseMapper`，同时为余额变更提供原子 SQL 更新方法。余额扣减类操作必须在 SQL 条件中判断余额是否足够，不能只在 Java 内存中判断。

`AccountBalanceMapper` 提供账户余额的原子更新能力。

文件位置：`src/main/java/io/github/atengk/wallet/mapper/AccountBalanceMapper.java`

```java
package io.github.atengk.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.wallet.domain.entity.AccountBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 账户余额 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface AccountBalanceMapper extends BaseMapper<AccountBalance> {

    /**
     * 增加可用余额
     *
     * @param accountId 账户 ID
     * @param amount    金额
     * @param version   当前版本号
     * @return 影响行数
     */
    @Update("""
            UPDATE account_balance
            SET available_balance = available_balance + #{amount},
                version = version + 1,
                update_time = NOW()
            WHERE id = #{accountId}
              AND version = #{version}
              AND status = 1
              AND deleted = 0
            """)
    int increaseAvailable(@Param("accountId") Long accountId,
                          @Param("amount") BigDecimal amount,
                          @Param("version") Integer version);

    /**
     * 扣减可用余额
     *
     * @param accountId 账户 ID
     * @param amount    金额
     * @param version   当前版本号
     * @return 影响行数
     */
    @Update("""
            UPDATE account_balance
            SET available_balance = available_balance - #{amount},
                version = version + 1,
                update_time = NOW()
            WHERE id = #{accountId}
              AND version = #{version}
              AND available_balance >= #{amount}
              AND status = 1
              AND deleted = 0
            """)
    int decreaseAvailable(@Param("accountId") Long accountId,
                          @Param("amount") BigDecimal amount,
                          @Param("version") Integer version);

    /**
     * 冻结可用余额
     *
     * @param accountId 账户 ID
     * @param amount    金额
     * @param version   当前版本号
     * @return 影响行数
     */
    @Update("""
            UPDATE account_balance
            SET available_balance = available_balance - #{amount},
                frozen_balance = frozen_balance + #{amount},
                version = version + 1,
                update_time = NOW()
            WHERE id = #{accountId}
              AND version = #{version}
              AND available_balance >= #{amount}
              AND status = 1
              AND deleted = 0
            """)
    int freezeBalance(@Param("accountId") Long accountId,
                      @Param("amount") BigDecimal amount,
                      @Param("version") Integer version);

    /**
     * 解冻冻结余额
     *
     * @param accountId 账户 ID
     * @param amount    金额
     * @param version   当前版本号
     * @return 影响行数
     */
    @Update("""
            UPDATE account_balance
            SET available_balance = available_balance + #{amount},
                frozen_balance = frozen_balance - #{amount},
                version = version + 1,
                update_time = NOW()
            WHERE id = #{accountId}
              AND version = #{version}
              AND frozen_balance >= #{amount}
              AND status = 1
              AND deleted = 0
            """)
    int unfreezeBalance(@Param("accountId") Long accountId,
                        @Param("amount") BigDecimal amount,
                        @Param("version") Integer version);

    /**
     * 扣减冻结余额
     *
     * @param accountId 账户 ID
     * @param amount    金额
     * @param version   当前版本号
     * @return 影响行数
     */
    @Update("""
            UPDATE account_balance
            SET frozen_balance = frozen_balance - #{amount},
                version = version + 1,
                update_time = NOW()
            WHERE id = #{accountId}
              AND version = #{version}
              AND frozen_balance >= #{amount}
              AND status = 1
              AND deleted = 0
            """)
    int deductFrozen(@Param("accountId") Long accountId,
                     @Param("amount") BigDecimal amount,
                     @Param("version") Integer version);
}
```

流水和幂等记录使用 MyBatis-Plus 标准 CRUD 即可。

文件位置：`src/main/java/io/github/atengk/wallet/mapper/AccountFlowMapper.java`

```java
package io.github.atengk.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.wallet.domain.entity.AccountFlow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户流水 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface AccountFlowMapper extends BaseMapper<AccountFlow> {
}
```

文件位置：`src/main/java/io/github/atengk/wallet/mapper/AccountIdempotentMapper.java`

```java
package io.github.atengk.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.wallet.domain.entity.AccountIdempotent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账户幂等记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface AccountIdempotentMapper extends BaseMapper<AccountIdempotent> {
}
```

### DTO 请求对象

DTO 用于接收接口请求参数。为了减少重复代码，入账、出账、冻结、解冻、扣减冻结金额共用 `AccountOperateDTO`，退款冲正单独使用 `AccountReverseDTO`。

`AccountOpenDTO` 用于账户开户。

文件位置：`src/main/java/io/github/atengk/wallet/domain/dto/AccountOpenDTO.java`

```java
package io.github.atengk.wallet.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 账户开户请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AccountOpenDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "账户类型不能为空")
    private String accountType;
}
```

`AccountOperateDTO` 用于金额操作类接口。

文件位置：`src/main/java/io/github/atengk/wallet/domain/dto/AccountOperateDTO.java`

```java
package io.github.atengk.wallet.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户金额操作请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AccountOperateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "账户类型不能为空")
    private String accountType;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.000001", message = "金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "业务单号不能为空")
    private String bizNo;

    private String remark;
}
```

`AccountReverseDTO` 用于退款冲正。

文件位置：`src/main/java/io/github/atengk/wallet/domain/dto/AccountReverseDTO.java`

```java
package io.github.atengk.wallet.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户退款冲正请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AccountReverseDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "账户类型不能为空")
    private String accountType;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.000001", message = "金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "退款业务单号不能为空")
    private String bizNo;

    @NotBlank(message = "原业务单号不能为空")
    private String originBizNo;

    private String remark;
}
```

`AccountFlowQueryDTO` 用于分页查询账户流水。

文件位置：`src/main/java/io/github/atengk/wallet/domain/dto/AccountFlowQueryDTO.java`

```java
package io.github.atengk.wallet.domain.dto;

import lombok.Data;

/**
 * 账户流水查询请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AccountFlowQueryDTO {

    private Long userId;

    private String accountNo;

    private String accountType;

    private String flowType;

    private String bizNo;

    private Long pageNo = 1L;

    private Long pageSize = 10L;
}
```

### VO 响应对象

VO 用于接口响应，避免直接把 Entity 暴露给前端。

`AccountBalanceVO` 返回账户当前余额。

文件位置：`src/main/java/io/github/atengk/wallet/domain/vo/AccountBalanceVO.java`

```java
package io.github.atengk.wallet.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户余额响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AccountBalanceVO {

    private String accountNo;

    private Long userId;

    private String accountType;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private Integer status;
}
```

`AccountFlowVO` 返回账户流水记录。

文件位置：`src/main/java/io/github/atengk/wallet/domain/vo/AccountFlowVO.java`

```java
package io.github.atengk.wallet.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户流水响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class AccountFlowVO {

    private String flowNo;

    private String accountNo;

    private Long userId;

    private String accountType;

    private String flowType;

    private String direction;

    private BigDecimal amount;

    private BigDecimal beforeAvailableBalance;

    private BigDecimal afterAvailableBalance;

    private BigDecimal beforeFrozenBalance;

    private BigDecimal afterFrozenBalance;

    private String bizNo;

    private String originBizNo;

    private String remark;

    private LocalDateTime createTime;
}
```

### Service 接口

Service 接口定义账户模块的业务能力。Controller 只负责接收请求和返回结果，核心规则全部下沉到 Service 层。

文件位置：`src/main/java/io/github/atengk/wallet/service/AccountService.java`

```java
package io.github.atengk.wallet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.wallet.domain.dto.AccountFlowQueryDTO;
import io.github.atengk.wallet.domain.dto.AccountOpenDTO;
import io.github.atengk.wallet.domain.dto.AccountOperateDTO;
import io.github.atengk.wallet.domain.dto.AccountReverseDTO;
import io.github.atengk.wallet.domain.vo.AccountBalanceVO;
import io.github.atengk.wallet.domain.vo.AccountFlowVO;

/**
 * 账户服务接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface AccountService {

    /**
     * 账户开户
     *
     * @param dto 开户请求
     * @return 账户余额
     */
    AccountBalanceVO open(AccountOpenDTO dto);

    /**
     * 入账
     *
     * @param dto 入账请求
     * @return 账户余额
     */
    AccountBalanceVO income(AccountOperateDTO dto);

    /**
     * 出账
     *
     * @param dto 出账请求
     * @return 账户余额
     */
    AccountBalanceVO outcome(AccountOperateDTO dto);

    /**
     * 冻结余额
     *
     * @param dto 冻结请求
     * @return 账户余额
     */
    AccountBalanceVO freeze(AccountOperateDTO dto);

    /**
     * 解冻余额
     *
     * @param dto 解冻请求
     * @return 账户余额
     */
    AccountBalanceVO unfreeze(AccountOperateDTO dto);

    /**
     * 扣减冻结金额
     *
     * @param dto 扣减请求
     * @return 账户余额
     */
    AccountBalanceVO deductFrozen(AccountOperateDTO dto);

    /**
     * 退款冲正
     *
     * @param dto 冲正请求
     * @return 账户余额
     */
    AccountBalanceVO reverse(AccountReverseDTO dto);

    /**
     * 分页查询账户流水
     *
     * @param dto 查询条件
     * @return 流水分页
     */
    Page<AccountFlowVO> pageFlows(AccountFlowQueryDTO dto);
}
```

### Service 实现类

Service 实现类负责账户核心逻辑：分布式锁、参数校验、账户查询、幂等判断、余额原子更新、流水写入和事务控制。

这里使用 `TransactionTemplate` 控制事务边界，确保“数据库事务提交完成后再释放 Redisson 锁”，避免锁先释放但事务尚未提交导致其他线程读到旧数据。

文件位置：`src/main/java/io/github/atengk/wallet/service/impl/AccountServiceImpl.java`

```java
package io.github.atengk.wallet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.wallet.common.enums.AccountFlowTypeEnum;
import io.github.atengk.wallet.common.exception.BusinessException;
import io.github.atengk.wallet.domain.dto.AccountFlowQueryDTO;
import io.github.atengk.wallet.domain.dto.AccountOpenDTO;
import io.github.atengk.wallet.domain.dto.AccountOperateDTO;
import io.github.atengk.wallet.domain.dto.AccountReverseDTO;
import io.github.atengk.wallet.domain.entity.AccountBalance;
import io.github.atengk.wallet.domain.entity.AccountFlow;
import io.github.atengk.wallet.domain.entity.AccountIdempotent;
import io.github.atengk.wallet.domain.vo.AccountBalanceVO;
import io.github.atengk.wallet.domain.vo.AccountFlowVO;
import io.github.atengk.wallet.mapper.AccountBalanceMapper;
import io.github.atengk.wallet.mapper.AccountFlowMapper;
import io.github.atengk.wallet.mapper.AccountIdempotentMapper;
import io.github.atengk.wallet.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 账户服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final int ACCOUNT_NORMAL_STATUS = 1;
    private static final int IDEM_SUCCESS_STATUS = 1;
    private static final String LOCK_PREFIX = "wallet:lock:account:";
    private static final String OPEN_LOCK_PREFIX = "wallet:lock:account:open:";
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    private final AccountBalanceMapper accountBalanceMapper;
    private final AccountFlowMapper accountFlowMapper;
    private final AccountIdempotentMapper accountIdempotentMapper;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    /**
     * 账户开户
     *
     * @param dto 开户请求
     * @return 账户余额
     */
    @Override
    public AccountBalanceVO open(AccountOpenDTO dto) {
        String lockKey = OPEN_LOCK_PREFIX + dto.getUserId() + ":" + dto.getAccountType();
        return executeWithLock(lockKey, () -> transactionTemplate.execute(status -> {
            AccountBalance existAccount = getAccount(dto.getUserId(), dto.getAccountType());
            if (existAccount != null) {
                log.info("账户已存在，直接返回已有账户，userId={}, accountType={}", dto.getUserId(), dto.getAccountType());
                return toBalanceVO(existAccount);
            }

            LocalDateTime now = LocalDateTime.now();
            AccountBalance account = new AccountBalance();
            account.setAccountNo(generateAccountNo());
            account.setUserId(dto.getUserId());
            account.setAccountType(dto.getAccountType());
            account.setAvailableBalance(normalize(BigDecimal.ZERO));
            account.setFrozenBalance(normalize(BigDecimal.ZERO));
            account.setStatus(ACCOUNT_NORMAL_STATUS);
            account.setVersion(0);
            account.setCreateTime(now);
            account.setUpdateTime(now);
            account.setDeleted(0);
            accountBalanceMapper.insert(account);

            AccountFlow flow = buildFlow(
                    account,
                    AccountFlowTypeEnum.OPEN.name(),
                    "IN",
                    normalize(BigDecimal.ZERO),
                    normalize(BigDecimal.ZERO),
                    normalize(BigDecimal.ZERO),
                    normalize(BigDecimal.ZERO),
                    normalize(BigDecimal.ZERO),
                    "OPEN:" + dto.getUserId() + ":" + dto.getAccountType(),
                    null,
                    "账户开户"
            );
            accountFlowMapper.insert(flow);

            log.info("账户开户成功，userId={}, accountType={}, accountNo={}",
                    dto.getUserId(), dto.getAccountType(), account.getAccountNo());
            return toBalanceVO(account);
        }));
    }

    /**
     * 入账
     *
     * @param dto 入账请求
     * @return 账户余额
     */
    @Override
    public AccountBalanceVO income(AccountOperateDTO dto) {
        return operate(
                dto,
                AccountFlowTypeEnum.INCOME.name(),
                "IN",
                account -> accountBalanceMapper.increaseAvailable(account.getId(), normalize(dto.getAmount()), account.getVersion()),
                account -> account.getAvailableBalance().add(normalize(dto.getAmount())),
                AccountBalance::getFrozenBalance,
                null
        );
    }

    /**
     * 出账
     *
     * @param dto 出账请求
     * @return 账户余额
     */
    @Override
    public AccountBalanceVO outcome(AccountOperateDTO dto) {
        return operate(
                dto,
                AccountFlowTypeEnum.OUTCOME.name(),
                "OUT",
                account -> accountBalanceMapper.decreaseAvailable(account.getId(), normalize(dto.getAmount()), account.getVersion()),
                account -> account.getAvailableBalance().subtract(normalize(dto.getAmount())),
                AccountBalance::getFrozenBalance,
                "可用余额不足，出账失败"
        );
    }

    /**
     * 冻结余额
     *
     * @param dto 冻结请求
     * @return 账户余额
     */
    @Override
    public AccountBalanceVO freeze(AccountOperateDTO dto) {
        return operate(
                dto,
                AccountFlowTypeEnum.FREEZE.name(),
                "FREEZE",
                account -> accountBalanceMapper.freezeBalance(account.getId(), normalize(dto.getAmount()), account.getVersion()),
                account -> account.getAvailableBalance().subtract(normalize(dto.getAmount())),
                account -> account.getFrozenBalance().add(normalize(dto.getAmount())),
                "可用余额不足，冻结失败"
        );
    }

    /**
     * 解冻余额
     *
     * @param dto 解冻请求
     * @return 账户余额
     */
    @Override
    public AccountBalanceVO unfreeze(AccountOperateDTO dto) {
        return operate(
                dto,
                AccountFlowTypeEnum.UNFREEZE.name(),
                "UNFREEZE",
                account -> accountBalanceMapper.unfreezeBalance(account.getId(), normalize(dto.getAmount()), account.getVersion()),
                account -> account.getAvailableBalance().add(normalize(dto.getAmount())),
                account -> account.getFrozenBalance().subtract(normalize(dto.getAmount())),
                "冻结余额不足，解冻失败"
        );
    }

    /**
     * 扣减冻结金额
     *
     * @param dto 扣减请求
     * @return 账户余额
     */
    @Override
    public AccountBalanceVO deductFrozen(AccountOperateDTO dto) {
        return operate(
                dto,
                AccountFlowTypeEnum.FROZEN_DEDUCT.name(),
                "OUT",
                account -> accountBalanceMapper.deductFrozen(account.getId(), normalize(dto.getAmount()), account.getVersion()),
                AccountBalance::getAvailableBalance,
                account -> account.getFrozenBalance().subtract(normalize(dto.getAmount())),
                "冻结余额不足，扣减失败"
        );
    }

    /**
     * 退款冲正
     *
     * @param dto 冲正请求
     * @return 账户余额
     */
    @Override
    public AccountBalanceVO reverse(AccountReverseDTO dto) {
        AccountOperateDTO operateDTO = new AccountOperateDTO();
        operateDTO.setUserId(dto.getUserId());
        operateDTO.setAccountType(dto.getAccountType());
        operateDTO.setAmount(dto.getAmount());
        operateDTO.setBizNo(dto.getBizNo());
        operateDTO.setRemark(dto.getRemark());

        return operate(
                operateDTO,
                AccountFlowTypeEnum.REVERSE.name(),
                "IN",
                account -> accountBalanceMapper.increaseAvailable(account.getId(), normalize(dto.getAmount()), account.getVersion()),
                account -> account.getAvailableBalance().add(normalize(dto.getAmount())),
                AccountBalance::getFrozenBalance,
                null,
                dto.getOriginBizNo()
        );
    }

    /**
     * 分页查询账户流水
     *
     * @param dto 查询条件
     * @return 流水分页
     */
    @Override
    public Page<AccountFlowVO> pageFlows(AccountFlowQueryDTO dto) {
        Page<AccountFlow> page = Page.of(dto.getPageNo(), dto.getPageSize());

        Page<AccountFlow> entityPage = accountFlowMapper.selectPage(
                page,
                Wrappers.<AccountFlow>lambdaQuery()
                        .eq(dto.getUserId() != null, AccountFlow::getUserId, dto.getUserId())
                        .eq(StrUtil.isNotBlank(dto.getAccountNo()), AccountFlow::getAccountNo, dto.getAccountNo())
                        .eq(StrUtil.isNotBlank(dto.getAccountType()), AccountFlow::getAccountType, dto.getAccountType())
                        .eq(StrUtil.isNotBlank(dto.getFlowType()), AccountFlow::getFlowType, dto.getFlowType())
                        .eq(StrUtil.isNotBlank(dto.getBizNo()), AccountFlow::getBizNo, dto.getBizNo())
                        .orderByDesc(AccountFlow::getCreateTime)
        );

        Page<AccountFlowVO> voPage = Page.of(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(BeanUtil.copyToList(entityPage.getRecords(), AccountFlowVO.class));
        return voPage;
    }

    /**
     * 执行账户金额操作
     *
     * @param dto                 操作请求
     * @param flowType            流水类型
     * @param direction           资金方向
     * @param updater             余额更新逻辑
     * @param afterAvailableFunc  变更后可用余额计算
     * @param afterFrozenFunc     变更后冻结余额计算
     * @param failMessage         更新失败提示
     * @return 账户余额
     */
    private AccountBalanceVO operate(AccountOperateDTO dto,
                                     String flowType,
                                     String direction,
                                     AccountUpdater updater,
                                     AccountAmountCalculator afterAvailableFunc,
                                     AccountAmountCalculator afterFrozenFunc,
                                     String failMessage) {
        return operate(dto, flowType, direction, updater, afterAvailableFunc, afterFrozenFunc, failMessage, null);
    }

    /**
     * 执行账户金额操作
     *
     * @param dto                 操作请求
     * @param flowType            流水类型
     * @param direction           资金方向
     * @param updater             余额更新逻辑
     * @param afterAvailableFunc  变更后可用余额计算
     * @param afterFrozenFunc     变更后冻结余额计算
     * @param failMessage         更新失败提示
     * @param originBizNo         原业务单号
     * @return 账户余额
     */
    private AccountBalanceVO operate(AccountOperateDTO dto,
                                     String flowType,
                                     String direction,
                                     AccountUpdater updater,
                                     AccountAmountCalculator afterAvailableFunc,
                                     AccountAmountCalculator afterFrozenFunc,
                                     String failMessage,
                                     String originBizNo) {
        BigDecimal amount = normalize(dto.getAmount());
        if (!NumberUtil.isGreater(amount, BigDecimal.ZERO)) {
            throw new BusinessException("金额必须大于0");
        }

        String lockKey = LOCK_PREFIX + dto.getUserId() + ":" + dto.getAccountType();
        return executeWithLock(lockKey, () -> transactionTemplate.execute(status -> {
            AccountBalance account = getAccount(dto.getUserId(), dto.getAccountType());
            if (account == null) {
                throw new BusinessException("账户不存在，请先开户");
            }
            if (!ACCOUNT_NORMAL_STATUS_EQUALS(account.getStatus())) {
                throw new BusinessException("账户状态不可用");
            }

            AccountFlow existFlow = getFlow(dto.getBizNo(), flowType);
            if (existFlow != null) {
                checkDuplicateAmount(existFlow, amount);
                log.info("账户操作重复请求，直接返回当前账户余额，bizNo={}, flowType={}", dto.getBizNo(), flowType);
                return toBalanceVO(account);
            }

            BigDecimal beforeAvailable = account.getAvailableBalance();
            BigDecimal beforeFrozen = account.getFrozenBalance();
            BigDecimal afterAvailable = normalize(afterAvailableFunc.calculate(account));
            BigDecimal afterFrozen = normalize(afterFrozenFunc.calculate(account));

            int updated = updater.update(account);
            if (updated <= 0) {
                String message = StrUtil.blankToDefault(failMessage, "账户余额更新失败，请重试");
                throw new BusinessException(message);
            }

            AccountFlow flow = buildFlow(
                    account,
                    flowType,
                    direction,
                    amount,
                    beforeAvailable,
                    afterAvailable,
                    beforeFrozen,
                    afterFrozen,
                    dto.getBizNo(),
                    originBizNo,
                    dto.getRemark()
            );
            accountFlowMapper.insert(flow);

            saveIdempotentSuccess(dto.getAccountType(), flowType, dto.getBizNo(), toBalanceVO(account));

            account.setAvailableBalance(afterAvailable);
            account.setFrozenBalance(afterFrozen);
            account.setVersion(account.getVersion() + 1);

            log.info("账户操作成功，userId={}, accountType={}, flowType={}, bizNo={}, amount={}",
                    dto.getUserId(), dto.getAccountType(), flowType, dto.getBizNo(), amount);
            return toBalanceVO(account);
        }));
    }

    /**
     * 使用 Redisson 锁执行逻辑
     *
     * @param lockKey  锁 Key
     * @param supplier 执行逻辑
     * @return 执行结果
     */
    private AccountBalanceVO executeWithLock(String lockKey, Supplier<AccountBalanceVO> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("账户操作繁忙，请稍后重试");
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("账户操作被中断，请重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询账户
     *
     * @param userId      用户 ID
     * @param accountType 账户类型
     * @return 账户
     */
    private AccountBalance getAccount(Long userId, String accountType) {
        return accountBalanceMapper.selectOne(
                Wrappers.<AccountBalance>lambdaQuery()
                        .eq(AccountBalance::getUserId, userId)
                        .eq(AccountBalance::getAccountType, accountType)
                        .last("LIMIT 1")
        );
    }

    /**
     * 查询账户流水
     *
     * @param bizNo    业务单号
     * @param flowType 流水类型
     * @return 账户流水
     */
    private AccountFlow getFlow(String bizNo, String flowType) {
        return accountFlowMapper.selectOne(
                Wrappers.<AccountFlow>lambdaQuery()
                        .eq(AccountFlow::getBizNo, bizNo)
                        .eq(AccountFlow::getFlowType, flowType)
                        .last("LIMIT 1")
        );
    }

    /**
     * 检查重复请求金额是否一致
     *
     * @param existFlow 已存在流水
     * @param amount    本次请求金额
     */
    private void checkDuplicateAmount(AccountFlow existFlow, BigDecimal amount) {
        if (existFlow.getAmount().compareTo(amount) != 0) {
            throw new BusinessException("业务单号已处理，但请求金额不一致");
        }
    }

    /**
     * 保存成功幂等记录
     *
     * @param accountType 账户类型
     * @param requestType 请求类型
     * @param bizNo       业务单号
     * @param response    响应结果
     */
    private void saveIdempotentSuccess(String accountType, String requestType, String bizNo, AccountBalanceVO response) {
        AccountIdempotent idempotent = new AccountIdempotent();
        idempotent.setIdemKey(accountType + ":" + requestType + ":" + bizNo);
        idempotent.setBizNo(bizNo);
        idempotent.setRequestType(requestType);
        idempotent.setStatus(IDEM_SUCCESS_STATUS);
        idempotent.setResponseBody(JSONUtil.toJsonStr(response));
        idempotent.setExpireTime(LocalDateTime.now().plusDays(30));
        idempotent.setCreateTime(LocalDateTime.now());
        idempotent.setUpdateTime(LocalDateTime.now());
        accountIdempotentMapper.insert(idempotent);
    }

    /**
     * 构建账户流水
     *
     * @param account         账户
     * @param flowType        流水类型
     * @param direction       资金方向
     * @param amount          金额
     * @param beforeAvailable 变更前可用余额
     * @param afterAvailable  变更后可用余额
     * @param beforeFrozen    变更前冻结余额
     * @param afterFrozen     变更后冻结余额
     * @param bizNo           业务单号
     * @param originBizNo     原业务单号
     * @param remark          备注
     * @return 账户流水
     */
    private AccountFlow buildFlow(AccountBalance account,
                                  String flowType,
                                  String direction,
                                  BigDecimal amount,
                                  BigDecimal beforeAvailable,
                                  BigDecimal afterAvailable,
                                  BigDecimal beforeFrozen,
                                  BigDecimal afterFrozen,
                                  String bizNo,
                                  String originBizNo,
                                  String remark) {
        AccountFlow flow = new AccountFlow();
        flow.setFlowNo(generateFlowNo());
        flow.setAccountId(account.getId());
        flow.setAccountNo(account.getAccountNo());
        flow.setUserId(account.getUserId());
        flow.setAccountType(account.getAccountType());
        flow.setFlowType(flowType);
        flow.setDirection(direction);
        flow.setAmount(normalize(amount));
        flow.setBeforeAvailableBalance(normalize(beforeAvailable));
        flow.setAfterAvailableBalance(normalize(afterAvailable));
        flow.setBeforeFrozenBalance(normalize(beforeFrozen));
        flow.setAfterFrozenBalance(normalize(afterFrozen));
        flow.setBizNo(bizNo);
        flow.setOriginBizNo(originBizNo);
        flow.setRemark(remark);
        flow.setCreateTime(LocalDateTime.now());
        return flow;
    }

    /**
     * 转换账户余额响应
     *
     * @param account 账户实体
     * @return 账户余额响应
     */
    private AccountBalanceVO toBalanceVO(AccountBalance account) {
        return BeanUtil.copyProperties(account, AccountBalanceVO.class);
    }

    /**
     * 生成账户编号
     *
     * @return 账户编号
     */
    private String generateAccountNo() {
        return "AC" + DateUtil.format(LocalDateTime.now(), "yyyyMMdd") + SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成流水编号
     *
     * @return 流水编号
     */
    private String generateFlowNo() {
        return "FL" + DateUtil.format(LocalDateTime.now(), "yyyyMMdd") + SNOWFLAKE.nextIdStr();
    }

    /**
     * 金额标准化
     *
     * @param amount 金额
     * @return 标准化金额
     */
    private BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.DOWN);
        }
        return amount.setScale(6, RoundingMode.DOWN);
    }

    /**
     * 判断账户状态是否正常
     *
     * @param status 账户状态
     * @return 是否正常
     */
    private boolean ACCOUNT_NORMAL_STATUS_EQUALS(Integer status) {
        return ACCOUNT_NORMAL_STATUS == status;
    }

    /**
     * 账户余额更新函数
     *
     * @author Ateng
     * @since 2026-05-15
     */
    @FunctionalInterface
    private interface AccountUpdater {

        /**
         * 更新账户余额
         *
         * @param account 账户
         * @return 影响行数
         */
        int update(AccountBalance account);
    }

    /**
     * 账户金额计算函数
     *
     * @author Ateng
     * @since 2026-05-15
     */
    @FunctionalInterface
    private interface AccountAmountCalculator {

        /**
         * 计算金额
         *
         * @param account 账户
         * @return 计算结果
         */
        BigDecimal calculate(AccountBalance account);
    }
}
```

上面实现中有几个关键点：

```text
1. Redisson 锁粒度是 userId + accountType，避免同一账户并发写入。
2. SQL 更新时带 version 和余额条件，防止并发覆盖和余额扣成负数。
3. 余额更新和流水写入在 TransactionTemplate 中完成。
4. 重复请求通过 bizNo + flowType 查流水，已处理则直接返回。
5. 数据库唯一索引 uk_biz_flow_type 是最终幂等保障。
```

注意：`saveIdempotentSuccess` 会写入幂等表，如果重复请求在流水检查前就被处理，也可以扩展为先查幂等表并直接反序列化返回结果。当前实现以流水唯一索引为主，幂等表作为请求追踪和后续扩展基础。

### Controller 接口层

Controller 层只做参数校验和接口转发，不写余额判断、锁控制、事务逻辑。核心业务必须放在 Service 层。

文件位置：`src/main/java/io/github/atengk/wallet/controller/AccountController.java`

```java
package io.github.atengk.wallet.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.wallet.common.result.Result;
import io.github.atengk.wallet.domain.dto.AccountFlowQueryDTO;
import io.github.atengk.wallet.domain.dto.AccountOpenDTO;
import io.github.atengk.wallet.domain.dto.AccountOperateDTO;
import io.github.atengk.wallet.domain.dto.AccountReverseDTO;
import io.github.atengk.wallet.domain.vo.AccountBalanceVO;
import io.github.atengk.wallet.domain.vo.AccountFlowVO;
import io.github.atengk.wallet.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 账户接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    /**
     * 账户开户
     *
     * @param dto 开户请求
     * @return 账户余额
     */
    @PostMapping("/open")
    public Result<AccountBalanceVO> open(@Valid @RequestBody AccountOpenDTO dto) {
        return Result.success(accountService.open(dto));
    }

    /**
     * 入账
     *
     * @param dto 入账请求
     * @return 账户余额
     */
    @PostMapping("/income")
    public Result<AccountBalanceVO> income(@Valid @RequestBody AccountOperateDTO dto) {
        return Result.success(accountService.income(dto));
    }

    /**
     * 出账
     *
     * @param dto 出账请求
     * @return 账户余额
     */
    @PostMapping("/outcome")
    public Result<AccountBalanceVO> outcome(@Valid @RequestBody AccountOperateDTO dto) {
        return Result.success(accountService.outcome(dto));
    }

    /**
     * 冻结余额
     *
     * @param dto 冻结请求
     * @return 账户余额
     */
    @PostMapping("/freeze")
    public Result<AccountBalanceVO> freeze(@Valid @RequestBody AccountOperateDTO dto) {
        return Result.success(accountService.freeze(dto));
    }

    /**
     * 解冻余额
     *
     * @param dto 解冻请求
     * @return 账户余额
     */
    @PostMapping("/unfreeze")
    public Result<AccountBalanceVO> unfreeze(@Valid @RequestBody AccountOperateDTO dto) {
        return Result.success(accountService.unfreeze(dto));
    }

    /**
     * 扣减冻结金额
     *
     * @param dto 扣减请求
     * @return 账户余额
     */
    @PostMapping("/frozen-deduct")
    public Result<AccountBalanceVO> deductFrozen(@Valid @RequestBody AccountOperateDTO dto) {
        return Result.success(accountService.deductFrozen(dto));
    }

    /**
     * 退款冲正
     *
     * @param dto 冲正请求
     * @return 账户余额
     */
    @PostMapping("/reverse")
    public Result<AccountBalanceVO> reverse(@Valid @RequestBody AccountReverseDTO dto) {
        return Result.success(accountService.reverse(dto));
    }

    /**
     * 分页查询账户流水
     *
     * @param dto 查询条件
     * @return 流水分页
     */
    @GetMapping("/flows")
    public Result<Page<AccountFlowVO>> pageFlows(AccountFlowQueryDTO dto) {
        return Result.success(accountService.pageFlows(dto));
    }
}
```

为了让 Controller 可以直接运行，补充一个最小版统一响应对象。

文件位置：`src/main/java/io/github/atengk/wallet/common/result/Result.java`

```java
package io.github.atengk.wallet.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 成功响应
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 响应结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(0, "成功", data);
    }

    /**
     * 失败响应
     *
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
```

补充业务异常，方便 Service 层抛出明确错误。

文件位置：`src/main/java/io/github/atengk/wallet/common/exception/BusinessException.java`

```java
package io.github.atengk.wallet.common.exception;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BusinessException extends RuntimeException {

    /**
     * 创建业务异常
     *
     * @param message 异常信息
     */
    public BusinessException(String message) {
        super(message);
    }
}
```

补充全局异常处理器，把参数校验异常和业务异常统一转成 JSON 响应。

文件位置：`src/main/java/io/github/atengk/wallet/common/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.wallet.common.exception;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.wallet.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param exception 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        log.warn("业务处理失败：{}", exception.getMessage());
        return Result.fail(exception.getMessage());
    }

    /**
     * 处理请求体参数校验异常
     *
     * @param exception 参数异常
     * @return 失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .toList();

        String message = CollUtil.isEmpty(messages) ? "请求参数不合法" : messages.get(0);
        log.warn("请求参数校验失败：{}", message);
        return Result.fail(message);
    }

    /**
     * 处理表单参数绑定异常
     *
     * @param exception 参数异常
     * @return 失败响应
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        List<String> messages = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .toList();

        String message = CollUtil.isEmpty(messages) ? "请求参数不合法" : messages.get(0);
        log.warn("表单参数校验失败：{}", message);
        return Result.fail(message);
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
```

接口调用示例：

```bash
# 账户开户
curl -X POST 'http://localhost:8080/api/accounts/open' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET"
  }'

# 入账
curl -X POST 'http://localhost:8080/api/accounts/income' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "100.00",
    "bizNo": "RECHARGE202605150001",
    "remark": "用户充值到账"
  }'

# 冻结余额
curl -X POST 'http://localhost:8080/api/accounts/freeze' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "50.00",
    "bizNo": "ORDER202605150001",
    "remark": "订单余额冻结"
  }'

# 查询流水
curl 'http://localhost:8080/api/accounts/flows?userId=10001&accountType=WALLET&pageNo=1&pageSize=10'
```

以上命令分别验证开户、入账、冻结和流水查询。执行顺序建议是：先开户，再入账，再冻结，最后查询流水确认余额变更记录是否完整。

## 核心业务实现

本节围绕账户模块最核心的 8 个业务动作展开：开户、入账、出账、冻结、解冻、扣减冻结金额、退款冲正和流水生成。实现重点不是堆业务概念，而是明确每个动作改哪些余额字段、如何防重复、如何防并发、如何保证账户表和流水表一致。该能力对应 README 中账户场景要求的“余额不能为负、可用余额和冻结余额分离、账户流水完整、并发扣减、重复请求幂等、冲正处理、账实一致”。

### 账户开户实现

账户开户的目标是为某个用户创建某一种账户，例如钱包账户、积分账户、虚拟币账户。同一个用户同一种账户类型只允许存在一个账户。

开户核心规则：

```text
1. 根据 userId + accountType 查询账户是否已存在
2. 如果账户已存在，直接返回已有账户
3. 如果账户不存在，创建账户余额记录
4. 初始化可用余额为 0
5. 初始化冻结余额为 0
6. 写入一条 OPEN 类型账户流水
7. userId + accountType 依赖唯一索引兜底防重复开户
```

开户时建议加开户维度锁，锁粒度使用：

```text
wallet:lock:account:open:{userId}:{accountType}
```

核心代码如下，放在 `AccountServiceImpl` 中。

```java
@Override
public AccountBalanceVO open(AccountOpenDTO dto) {
    String lockKey = OPEN_LOCK_PREFIX + dto.getUserId() + ":" + dto.getAccountType();

    return executeWithLock(lockKey, () -> transactionTemplate.execute(status -> {
        AccountBalance existAccount = getAccount(dto.getUserId(), dto.getAccountType());
        if (existAccount != null) {
            log.info("账户已存在，直接返回已有账户，userId={}, accountType={}", dto.getUserId(), dto.getAccountType());
            return toBalanceVO(existAccount);
        }

        LocalDateTime now = LocalDateTime.now();

        AccountBalance account = new AccountBalance();
        account.setAccountNo(generateAccountNo());
        account.setUserId(dto.getUserId());
        account.setAccountType(dto.getAccountType());
        account.setAvailableBalance(normalize(BigDecimal.ZERO));
        account.setFrozenBalance(normalize(BigDecimal.ZERO));
        account.setStatus(ACCOUNT_NORMAL_STATUS);
        account.setVersion(0);
        account.setCreateTime(now);
        account.setUpdateTime(now);
        account.setDeleted(0);
        accountBalanceMapper.insert(account);

        AccountFlow flow = buildFlow(
                account,
                AccountFlowTypeEnum.OPEN.name(),
                "IN",
                normalize(BigDecimal.ZERO),
                normalize(BigDecimal.ZERO),
                normalize(BigDecimal.ZERO),
                normalize(BigDecimal.ZERO),
                normalize(BigDecimal.ZERO),
                "OPEN:" + dto.getUserId() + ":" + dto.getAccountType(),
                null,
                "账户开户"
        );
        accountFlowMapper.insert(flow);

        log.info("账户开户成功，userId={}, accountType={}, accountNo={}",
                dto.getUserId(), dto.getAccountType(), account.getAccountNo());

        return toBalanceVO(account);
    }));
}
```

开户后的账户数据示例：

```text
userId = 10001
accountType = WALLET
availableBalance = 0.000000
frozenBalance = 0.000000
version = 0
status = 1
```

开户后会生成一条流水，方便后续追溯账户创建时间和初始状态。

### 入账实现

入账用于增加可用余额，常见场景包括充值到账、积分发放、退款到账、运营赠送额度等。

入账余额变化：

```text
available_balance = available_balance + amount
frozen_balance 不变
```

入账核心规则：

```text
1. 金额必须大于 0
2. 账户必须存在且状态正常
3. 同一个 bizNo + INCOME 只能入账一次
4. 更新账户可用余额
5. 写入 INCOME 类型账户流水
6. 账户余额更新和流水写入必须在同一个事务内完成
```

入账接口调用的是通用 `operate` 方法。

```java
@Override
public AccountBalanceVO income(AccountOperateDTO dto) {
    return operate(
            dto,
            AccountFlowTypeEnum.INCOME.name(),
            "IN",
            account -> accountBalanceMapper.increaseAvailable(
                    account.getId(),
                    normalize(dto.getAmount()),
                    account.getVersion()
            ),
            account -> account.getAvailableBalance().add(normalize(dto.getAmount())),
            AccountBalance::getFrozenBalance,
            null
    );
}
```

对应 SQL 更新逻辑：

```sql
UPDATE account_balance
SET available_balance = available_balance + #{amount},
    version = version + 1,
    update_time = NOW()
WHERE id = #{accountId}
  AND version = #{version}
  AND status = 1
  AND deleted = 0;
```

入账前后示例：

```text
入账前：
available_balance = 0.000000
frozen_balance = 0.000000

入账金额：
amount = 100.000000

入账后：
available_balance = 100.000000
frozen_balance = 0.000000
```

重复入账时，通过 `bizNo + flowType` 查询流水。如果流水已存在，说明该业务单已经处理过，直接返回当前账户余额，不再修改余额。

```java
AccountFlow existFlow = getFlow(dto.getBizNo(), flowType);
if (existFlow != null) {
    checkDuplicateAmount(existFlow, amount);
    log.info("账户操作重复请求，直接返回当前账户余额，bizNo={}, flowType={}", dto.getBizNo(), flowType);
    return toBalanceVO(account);
}
```

### 出账实现

出账用于直接扣减可用余额，常见场景包括余额支付、积分兑换、额度消费等。

出账余额变化：

```text
available_balance = available_balance - amount
frozen_balance 不变
```

出账核心规则：

```text
1. 金额必须大于 0
2. 账户必须存在且状态正常
3. 可用余额必须大于等于出账金额
4. 同一个 bizNo + OUTCOME 只能出账一次
5. SQL 更新条件中必须包含 available_balance >= amount
6. 出账成功后写入 OUTCOME 类型账户流水
```

出账实现：

```java
@Override
public AccountBalanceVO outcome(AccountOperateDTO dto) {
    return operate(
            dto,
            AccountFlowTypeEnum.OUTCOME.name(),
            "OUT",
            account -> accountBalanceMapper.decreaseAvailable(
                    account.getId(),
                    normalize(dto.getAmount()),
                    account.getVersion()
            ),
            account -> account.getAvailableBalance().subtract(normalize(dto.getAmount())),
            AccountBalance::getFrozenBalance,
            "可用余额不足，出账失败"
    );
}
```

对应 SQL 必须在数据库层判断余额是否足够：

```sql
UPDATE account_balance
SET available_balance = available_balance - #{amount},
    version = version + 1,
    update_time = NOW()
WHERE id = #{accountId}
  AND version = #{version}
  AND available_balance >= #{amount}
  AND status = 1
  AND deleted = 0;
```

不能只在 Java 中这样判断：

```java
if (account.getAvailableBalance().compareTo(amount) < 0) {
    throw new BusinessException("余额不足");
}
```

这类判断只能作为提前校验，不能作为最终并发控制。最终控制必须放在 SQL 条件中，否则高并发下可能出现多个线程同时看到余额足够，最终把余额扣成负数。

出账前后示例：

```text
出账前：
available_balance = 100.000000
frozen_balance = 0.000000

出账金额：
amount = 30.000000

出账后：
available_balance = 70.000000
frozen_balance = 0.000000
```

### 冻结余额实现

冻结余额用于“先占用，后确认”的业务场景，例如订单创建后先冻结余额，支付确认或履约完成后再扣减冻结金额。

冻结余额变化：

```text
available_balance = available_balance - amount
frozen_balance = frozen_balance + amount
```

冻结核心规则：

```text
1. 可用余额必须大于等于冻结金额
2. 冻结成功后，可用余额减少
3. 冻结成功后，冻结余额增加
4. 同一个 bizNo + FREEZE 只能冻结一次
5. 冻结失败不能生成流水
```

冻结实现：

```java
@Override
public AccountBalanceVO freeze(AccountOperateDTO dto) {
    return operate(
            dto,
            AccountFlowTypeEnum.FREEZE.name(),
            "FREEZE",
            account -> accountBalanceMapper.freezeBalance(
                    account.getId(),
                    normalize(dto.getAmount()),
                    account.getVersion()
            ),
            account -> account.getAvailableBalance().subtract(normalize(dto.getAmount())),
            account -> account.getFrozenBalance().add(normalize(dto.getAmount())),
            "可用余额不足，冻结失败"
    );
}
```

对应 SQL：

```sql
UPDATE account_balance
SET available_balance = available_balance - #{amount},
    frozen_balance = frozen_balance + #{amount},
    version = version + 1,
    update_time = NOW()
WHERE id = #{accountId}
  AND version = #{version}
  AND available_balance >= #{amount}
  AND status = 1
  AND deleted = 0;
```

冻结前后示例：

```text
冻结前：
available_balance = 100.000000
frozen_balance = 0.000000

冻结金额：
amount = 50.000000

冻结后：
available_balance = 50.000000
frozen_balance = 50.000000
```

冻结适合这种业务链路：

```text
用户提交订单
-> 账户冻结余额
-> 订单进入待确认状态
-> 业务成功则扣减冻结金额
-> 业务失败则解冻余额
```

冻结不是最终扣款，它只是把可用余额转移到冻结余额中，防止用户继续使用这部分金额。

### 解冻余额实现

解冻余额用于释放已经冻结但不再需要占用的金额，例如订单取消、支付超时、业务失败回滚。

解冻余额变化：

```text
available_balance = available_balance + amount
frozen_balance = frozen_balance - amount
```

解冻核心规则：

```text
1. 冻结余额必须大于等于解冻金额
2. 解冻成功后，冻结余额减少
3. 解冻成功后，可用余额增加
4. 同一个 bizNo + UNFREEZE 只能解冻一次
5. 解冻金额不能超过当前冻结余额
```

解冻实现：

```java
@Override
public AccountBalanceVO unfreeze(AccountOperateDTO dto) {
    return operate(
            dto,
            AccountFlowTypeEnum.UNFREEZE.name(),
            "UNFREEZE",
            account -> accountBalanceMapper.unfreezeBalance(
                    account.getId(),
                    normalize(dto.getAmount()),
                    account.getVersion()
            ),
            account -> account.getAvailableBalance().add(normalize(dto.getAmount())),
            account -> account.getFrozenBalance().subtract(normalize(dto.getAmount())),
            "冻结余额不足，解冻失败"
    );
}
```

对应 SQL：

```sql
UPDATE account_balance
SET available_balance = available_balance + #{amount},
    frozen_balance = frozen_balance - #{amount},
    version = version + 1,
    update_time = NOW()
WHERE id = #{accountId}
  AND version = #{version}
  AND frozen_balance >= #{amount}
  AND status = 1
  AND deleted = 0;
```

解冻前后示例：

```text
解冻前：
available_balance = 50.000000
frozen_balance = 50.000000

解冻金额：
amount = 50.000000

解冻后：
available_balance = 100.000000
frozen_balance = 0.000000
```

订单取消链路示例：

```text
1. 用户创建订单，冻结 50 元
2. 用户取消订单
3. 订单系统调用账户解冻接口
4. 账户系统将 50 元从冻结余额退回可用余额
5. 生成 UNFREEZE 流水
```

### 扣减冻结金额实现

扣减冻结金额用于把已经冻结的金额正式扣除。它通常发生在业务已经确认成功之后，例如订单支付确认、服务履约完成、担保交易确认收货。

扣减冻结金额变化：

```text
available_balance 不变
frozen_balance = frozen_balance - amount
```

扣减冻结金额核心规则：

```text
1. 冻结余额必须大于等于扣减金额
2. 扣减冻结金额不影响可用余额
3. 同一个 bizNo + FROZEN_DEDUCT 只能扣减一次
4. 扣减成功后写入 FROZEN_DEDUCT 流水
5. 扣减失败不能写流水
```

扣减冻结金额实现：

```java
@Override
public AccountBalanceVO deductFrozen(AccountOperateDTO dto) {
    return operate(
            dto,
            AccountFlowTypeEnum.FROZEN_DEDUCT.name(),
            "OUT",
            account -> accountBalanceMapper.deductFrozen(
                    account.getId(),
                    normalize(dto.getAmount()),
                    account.getVersion()
            ),
            AccountBalance::getAvailableBalance,
            account -> account.getFrozenBalance().subtract(normalize(dto.getAmount())),
            "冻结余额不足，扣减失败"
    );
}
```

对应 SQL：

```sql
UPDATE account_balance
SET frozen_balance = frozen_balance - #{amount},
    version = version + 1,
    update_time = NOW()
WHERE id = #{accountId}
  AND version = #{version}
  AND frozen_balance >= #{amount}
  AND status = 1
  AND deleted = 0;
```

扣减冻结金额前后示例：

```text
扣减前：
available_balance = 50.000000
frozen_balance = 50.000000

扣减金额：
amount = 50.000000

扣减后：
available_balance = 50.000000
frozen_balance = 0.000000
```

完整订单余额支付链路示例：

```text
账户初始余额：
available_balance = 100.000000
frozen_balance = 0.000000

创建订单冻结 50：
available_balance = 50.000000
frozen_balance = 50.000000

订单确认扣减冻结 50：
available_balance = 50.000000
frozen_balance = 0.000000
```

这里不要再次扣减可用余额，因为冻结阶段已经从可用余额中扣掉了金额。扣减冻结金额只是把冻结金额正式出账。

### 退款冲正实现

退款冲正用于反向修正已经完成的扣款，常见场景包括订单退款、支付撤销、异常补偿、人工调账等。

退款冲正余额变化：

```text
available_balance = available_balance + amount
frozen_balance 不变
```

退款冲正核心规则：

```text
1. 退款单号 bizNo 必须唯一
2. 原业务单号 originBizNo 必须记录
3. 同一个 bizNo + REVERSE 只能冲正一次
4. 冲正成功后增加可用余额
5. 冲正流水要能关联原始业务单
```

退款冲正实现：

```java
@Override
public AccountBalanceVO reverse(AccountReverseDTO dto) {
    AccountOperateDTO operateDTO = new AccountOperateDTO();
    operateDTO.setUserId(dto.getUserId());
    operateDTO.setAccountType(dto.getAccountType());
    operateDTO.setAmount(dto.getAmount());
    operateDTO.setBizNo(dto.getBizNo());
    operateDTO.setRemark(dto.getRemark());

    return operate(
            operateDTO,
            AccountFlowTypeEnum.REVERSE.name(),
            "IN",
            account -> accountBalanceMapper.increaseAvailable(
                    account.getId(),
                    normalize(dto.getAmount()),
                    account.getVersion()
            ),
            account -> account.getAvailableBalance().add(normalize(dto.getAmount())),
            AccountBalance::getFrozenBalance,
            null,
            dto.getOriginBizNo()
    );
}
```

退款冲正前后示例：

```text
退款前：
available_balance = 50.000000
frozen_balance = 0.000000

退款金额：
amount = 50.000000

退款后：
available_balance = 100.000000
frozen_balance = 0.000000
```

退款冲正流水示例：

```text
bizNo = REFUND202605150001
originBizNo = ORDER202605150001
flowType = REVERSE
direction = IN
amount = 50.000000
```

退款冲正必须使用新的退款业务单号，不能直接复用原订单号。原订单号应放到 `originBizNo` 中，这样可以同时保证“退款请求幂等”和“原业务可追溯”。

推荐调用方式：

```bash
curl -X POST 'http://localhost:8080/api/accounts/reverse' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "50.00",
    "bizNo": "REFUND202605150001",
    "originBizNo": "ORDER202605150001",
    "remark": "订单退款冲正"
  }'
```

### 账户流水生成实现

账户流水是账户系统中最重要的审计数据。每一次账户余额变化都必须生成流水，流水记录变动前余额、变动后余额、金额、业务单号、流水类型和备注。

流水生成核心规则：

```text
1. 账户余额不变更，不生成余额变更流水
2. 账户余额变更成功，必须生成流水
3. 流水记录变更前后的可用余额和冻结余额
4. 流水通过 bizNo + flowType 做唯一约束
5. 流水只新增，不修改，不删除
```

流水构建方法如下，放在 `AccountServiceImpl` 中。

```java
private AccountFlow buildFlow(AccountBalance account,
                              String flowType,
                              String direction,
                              BigDecimal amount,
                              BigDecimal beforeAvailable,
                              BigDecimal afterAvailable,
                              BigDecimal beforeFrozen,
                              BigDecimal afterFrozen,
                              String bizNo,
                              String originBizNo,
                              String remark) {
    AccountFlow flow = new AccountFlow();
    flow.setFlowNo(generateFlowNo());
    flow.setAccountId(account.getId());
    flow.setAccountNo(account.getAccountNo());
    flow.setUserId(account.getUserId());
    flow.setAccountType(account.getAccountType());
    flow.setFlowType(flowType);
    flow.setDirection(direction);
    flow.setAmount(normalize(amount));
    flow.setBeforeAvailableBalance(normalize(beforeAvailable));
    flow.setAfterAvailableBalance(normalize(afterAvailable));
    flow.setBeforeFrozenBalance(normalize(beforeFrozen));
    flow.setAfterFrozenBalance(normalize(afterFrozen));
    flow.setBizNo(bizNo);
    flow.setOriginBizNo(originBizNo);
    flow.setRemark(remark);
    flow.setCreateTime(LocalDateTime.now());
    return flow;
}
```

余额操作成功后，立即插入流水：

```java
int updated = updater.update(account);
if (updated <= 0) {
    String message = StrUtil.blankToDefault(failMessage, "账户余额更新失败，请重试");
    throw new BusinessException(message);
}

AccountFlow flow = buildFlow(
        account,
        flowType,
        direction,
        amount,
        beforeAvailable,
        afterAvailable,
        beforeFrozen,
        afterFrozen,
        dto.getBizNo(),
        originBizNo,
        dto.getRemark()
);
accountFlowMapper.insert(flow);
```

流水和账户余额必须处于同一个事务中：

```java
return executeWithLock(lockKey, () -> transactionTemplate.execute(status -> {
    // 1. 查询账户
    // 2. 判断重复流水
    // 3. 更新账户余额
    // 4. 写入账户流水
    // 5. 写入幂等记录
    // 6. 返回账户余额
}));
```

如果账户余额更新成功，但流水插入失败，整个事务必须回滚。不能允许出现“余额变了但没有流水”的情况。

流水数据示例：

```text
flowNo = FL2026051510010001
accountNo = AC2026051510010001
userId = 10001
accountType = WALLET
flowType = FREEZE
direction = FREEZE
amount = 50.000000
beforeAvailableBalance = 100.000000
afterAvailableBalance = 50.000000
beforeFrozenBalance = 0.000000
afterFrozenBalance = 50.000000
bizNo = ORDER202605150001
originBizNo = null
remark = 订单余额冻结
```

最终可以通过流水还原账户变化过程：

```text
OPEN：
available 0 -> 0，frozen 0 -> 0

INCOME 100：
available 0 -> 100，frozen 0 -> 0

FREEZE 50：
available 100 -> 50，frozen 0 -> 50

FROZEN_DEDUCT 50：
available 50 -> 50，frozen 50 -> 0

REVERSE 50：
available 50 -> 100，frozen 0 -> 0
```

账户流水不仅是查询账单的数据源，也是问题排查、对账补偿、审计追踪的基础。账户模块的最低要求是：任何余额变化都能在流水表中找到对应记录，任何流水都能清楚说明这次余额为什么变化。

## 并发控制与幂等处理

账户余额模块的核心风险集中在并发扣减、重复请求、余额扣成负数和流水重复生成。README 中也把“并发扣减、重复请求幂等、余额不能为负、账实一致”列为该场景核心难点，因此这里重点使用 Redisson 锁、数据库乐观锁、唯一索引和幂等表组合处理。

推荐控制链路如下：

```text
请求进入
-> 生成幂等 Key
-> 获取账户维度 Redisson 锁
-> 开启本地事务
-> 查询幂等记录
-> 查询账户
-> 校验账户状态
-> 使用乐观锁 SQL 更新余额
-> 写入账户流水
-> 写入或更新幂等记录
-> 提交事务
-> 释放 Redisson 锁
```

### Redisson 分布式锁

Redisson 分布式锁用于控制同一个账户在同一时间只有一个线程执行余额变更。锁不是最终一致性的唯一保障，但可以显著降低数据库乐观锁冲突，避免热点账户在高并发下频繁重试。

锁粒度建议使用：

```text
wallet:lock:account:{userId}:{accountType}
```

不要使用全局锁，例如：

```text
wallet:lock:account
```

全局锁会导致所有用户账户操作串行执行，吞吐量会非常差。

推荐封装一个账户锁执行方法，放在 `AccountServiceImpl` 中。

```java
/**
 * 使用账户维度分布式锁执行余额操作
 *
 * @param userId      用户 ID
 * @param accountType 账户类型
 * @param supplier    业务逻辑
 * @return 执行结果
 */
private AccountBalanceVO executeWithAccountLock(Long userId, String accountType, Supplier<AccountBalanceVO> supplier) {
    String lockKey = LOCK_PREFIX + userId + ":" + accountType;
    RLock lock = redissonClient.getLock(lockKey);

    boolean locked = false;
    try {
        locked = lock.tryLock(3, 15, TimeUnit.SECONDS);
        if (!locked) {
            log.warn("账户锁获取失败，userId={}, accountType={}", userId, accountType);
            throw new BusinessException("账户操作繁忙，请稍后重试");
        }

        return supplier.get();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("账户锁等待被中断，userId={}, accountType={}", userId, accountType);
        throw new BusinessException("账户操作被中断，请重试");
    } finally {
        if (locked && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

调用方式：

```java
@Override
public AccountBalanceVO freeze(AccountOperateDTO dto) {
    return executeWithAccountLock(dto.getUserId(), dto.getAccountType(), () ->
            operate(
                    dto,
                    AccountFlowTypeEnum.FREEZE.name(),
                    "FREEZE",
                    account -> accountBalanceMapper.freezeBalance(
                            account.getId(),
                            normalize(dto.getAmount()),
                            account.getVersion()
                    ),
                    account -> account.getAvailableBalance().subtract(normalize(dto.getAmount())),
                    account -> account.getFrozenBalance().add(normalize(dto.getAmount())),
                    "可用余额不足，冻结失败"
            )
    );
}
```

Redisson 锁的注意事项：

```text
1. 锁粒度控制到账户级别，不要锁全局。
2. 锁等待时间不宜过长，接口场景建议 1 到 3 秒。
3. 锁租约时间要覆盖正常业务执行时间。
4. 释放锁前必须判断 lock.isHeldByCurrentThread()。
5. 分布式锁只能降低并发冲突，不能替代数据库余额条件和唯一索引。
```

### 数据库乐观锁

数据库乐观锁是账户余额并发更新的最终保护之一。账户表中必须有 `version` 字段，每次更新余额都带上当前版本号。

账户表字段：

```sql
version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号'
```

扣减可用余额时，SQL 必须包含 `version` 和 `available_balance >= amount` 条件。

```sql
UPDATE account_balance
SET available_balance = available_balance - #{amount},
    version = version + 1,
    update_time = NOW()
WHERE id = #{accountId}
  AND version = #{version}
  AND available_balance >= #{amount}
  AND status = 1
  AND deleted = 0;
```

冻结余额时，同样要带余额条件：

```sql
UPDATE account_balance
SET available_balance = available_balance - #{amount},
    frozen_balance = frozen_balance + #{amount},
    version = version + 1,
    update_time = NOW()
WHERE id = #{accountId}
  AND version = #{version}
  AND available_balance >= #{amount}
  AND status = 1
  AND deleted = 0;
```

扣减冻结余额时，要判断冻结余额是否足够：

```sql
UPDATE account_balance
SET frozen_balance = frozen_balance - #{amount},
    version = version + 1,
    update_time = NOW()
WHERE id = #{accountId}
  AND version = #{version}
  AND frozen_balance >= #{amount}
  AND status = 1
  AND deleted = 0;
```

Java 中判断更新结果：

```java
int updated = updater.update(account);
if (updated <= 0) {
    log.warn("账户余额更新失败，accountId={}, version={}, flowType={}, bizNo={}",
            account.getId(), account.getVersion(), flowType, dto.getBizNo());
    throw new BusinessException(StrUtil.blankToDefault(failMessage, "账户余额更新失败，请重试"));
}
```

这里的 `updated <= 0` 通常代表以下情况之一：

```text
账户余额不足
冻结余额不足
账户状态不可用
账户版本号已经变化
账户被删除
```

如果系统并发量很高，可以在业务层对乐观锁失败做有限重试，但余额类接口不建议无限重试。推荐最多重试 1 到 3 次，失败后让调用方重新发起请求。

### 幂等 Key 设计

幂等 Key 用于识别“同一次业务请求”。账户系统不能简单用用户 ID 做幂等，因为同一个用户可以有多笔充值、支付、退款。推荐使用账户类型、请求类型、业务单号组合生成幂等 Key。

幂等 Key 格式：

```text
{accountType}:{requestType}:{bizNo}
```

示例：

```text
WALLET:INCOME:RECHARGE202605150001
WALLET:FREEZE:ORDER202605150001
WALLET:FROZEN_DEDUCT:ORDER202605150001
WALLET:REVERSE:REFUND202605150001
POINT:OUTCOME:POINT_EXCHANGE202605150001
```

幂等 Key 工具类可以单独封装，避免各业务方法手写字符串拼接。

文件位置：`src/main/java/io/github/atengk/wallet/common/util/IdempotentKeyUtil.java`

```java
package io.github.atengk.wallet.common.util;

import cn.hutool.core.text.CharSequenceUtil;

/**
 * 幂等 Key 工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class IdempotentKeyUtil {

    private IdempotentKeyUtil() {
    }

    /**
     * 构建账户幂等 Key
     *
     * @param accountType 账户类型
     * @param requestType 请求类型
     * @param bizNo       业务单号
     * @return 幂等 Key
     */
    public static String build(String accountType, String requestType, String bizNo) {
        return CharSequenceUtil.format("{}:{}:{}", accountType, requestType, bizNo);
    }
}
```

使用方式：

```java
String idemKey = IdempotentKeyUtil.build(dto.getAccountType(), flowType, dto.getBizNo());
```

幂等表建议保留两个唯一索引：

```sql
UNIQUE KEY uk_idem_key (idem_key),
UNIQUE KEY uk_biz_request_type (biz_no, request_type)
```

`uk_idem_key` 防止同一个幂等 Key 重复插入，`uk_biz_request_type` 防止同一个业务单号重复执行同类账户动作。

### 重复请求处理

重复请求分为三类：

```text
1. 完全重复：bizNo、flowType、amount 都一致
2. 异常重复：bizNo、flowType 一致，但 amount 不一致
3. 并发重复：多个相同请求同时进入系统
```

处理原则：

| 类型     | 处理方式                                           |
| -------- | -------------------------------------------------- |
| 完全重复 | 直接返回上次处理结果或当前账户余额                 |
| 异常重复 | 拒绝处理，提示业务单号已处理但金额不一致           |
| 并发重复 | 依赖 Redisson 锁、幂等表唯一索引、流水唯一索引兜底 |

推荐在账户操作开始时先检查流水：

```java
AccountFlow existFlow = getFlow(dto.getBizNo(), flowType);
if (existFlow != null) {
    checkDuplicateAmount(existFlow, amount);
    log.info("账户操作重复请求，直接返回当前账户余额，bizNo={}, flowType={}", dto.getBizNo(), flowType);
    return toBalanceVO(account);
}
```

金额一致性校验：

```java
/**
 * 检查重复请求金额是否一致
 *
 * @param existFlow 已存在流水
 * @param amount    本次请求金额
 */
private void checkDuplicateAmount(AccountFlow existFlow, BigDecimal amount) {
    if (existFlow.getAmount().compareTo(amount) != 0) {
        log.warn("重复业务单号金额不一致，bizNo={}, flowType={}, oldAmount={}, newAmount={}",
                existFlow.getBizNo(), existFlow.getFlowType(), existFlow.getAmount(), amount);
        throw new BusinessException("业务单号已处理，但请求金额不一致");
    }
}
```

更完整的做法是先插入“处理中”幂等记录，业务成功后更新为“成功”。下面方法可作为前面简化版幂等逻辑的增强。

```java
/**
 * 创建处理中幂等记录
 *
 * @param accountType 账户类型
 * @param requestType 请求类型
 * @param bizNo       业务单号
 * @return 幂等 Key
 */
private String createProcessingIdempotent(String accountType, String requestType, String bizNo) {
    String idemKey = IdempotentKeyUtil.build(accountType, requestType, bizNo);

    AccountIdempotent idempotent = new AccountIdempotent();
    idempotent.setIdemKey(idemKey);
    idempotent.setBizNo(bizNo);
    idempotent.setRequestType(requestType);
    idempotent.setStatus(0);
    idempotent.setExpireTime(LocalDateTime.now().plusDays(30));
    idempotent.setCreateTime(LocalDateTime.now());
    idempotent.setUpdateTime(LocalDateTime.now());

    try {
        accountIdempotentMapper.insert(idempotent);
        return idemKey;
    } catch (DuplicateKeyException e) {
        log.warn("幂等记录已存在，idemKey={}", idemKey);
        throw new BusinessException("请求已提交，请勿重复操作");
    }
}
```

业务成功后更新幂等记录：

```java
/**
 * 标记幂等请求处理成功
 *
 * @param idemKey  幂等 Key
 * @param response 响应数据
 */
private void markIdempotentSuccess(String idemKey, AccountBalanceVO response) {
    AccountIdempotent update = new AccountIdempotent();
    update.setStatus(1);
    update.setResponseBody(JSONUtil.toJsonStr(response));
    update.setUpdateTime(LocalDateTime.now());

    accountIdempotentMapper.update(
            update,
            Wrappers.<AccountIdempotent>lambdaUpdate()
                    .eq(AccountIdempotent::getIdemKey, idemKey)
    );
}
```

业务失败后更新幂等记录：

```java
/**
 * 标记幂等请求处理失败
 *
 * @param idemKey      幂等 Key
 * @param errorMessage 错误信息
 */
private void markIdempotentFailed(String idemKey, String errorMessage) {
    AccountIdempotent update = new AccountIdempotent();
    update.setStatus(2);
    update.setErrorMessage(StrUtil.subPre(errorMessage, 500));
    update.setUpdateTime(LocalDateTime.now());

    accountIdempotentMapper.update(
            update,
            Wrappers.<AccountIdempotent>lambdaUpdate()
                    .eq(AccountIdempotent::getIdemKey, idemKey)
    );
}
```

实际项目中可以根据请求状态选择不同响应策略：

```text
幂等状态 = 0：返回“请求处理中，请稍后查询”
幂等状态 = 1：返回上次成功响应
幂等状态 = 2：允许重试，或者返回上次失败原因
```

对于账户余额这类强一致业务，推荐同时保留“幂等表”和“流水唯一索引”。即使幂等表逻辑遗漏，流水表的 `uk_biz_flow_type` 也能防止重复记账。

### 余额不能为负控制

余额不能为负必须在数据库层控制，Java 层提前判断只能作为提示优化，不能作为最终保障。

错误做法：

```java
if (account.getAvailableBalance().compareTo(amount) < 0) {
    throw new BusinessException("余额不足");
}

account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
accountBalanceMapper.updateById(account);
```

这种写法在并发下有风险。两个线程可能同时读到余额 `100`，然后分别扣减 `80`，最终造成余额错误。

正确做法是把余额条件写进 SQL：

```sql
UPDATE account_balance
SET available_balance = available_balance - #{amount},
    version = version + 1,
    update_time = NOW()
WHERE id = #{accountId}
  AND version = #{version}
  AND available_balance >= #{amount}
  AND status = 1
  AND deleted = 0;
```

冻结余额也一样：

```sql
UPDATE account_balance
SET frozen_balance = frozen_balance - #{amount},
    version = version + 1,
    update_time = NOW()
WHERE id = #{accountId}
  AND version = #{version}
  AND frozen_balance >= #{amount}
  AND status = 1
  AND deleted = 0;
```

为防止异常数据直接进入数据库，还可以增加表级约束。MySQL 8 支持 `CHECK` 约束，但在实际项目中仍建议以 SQL 条件更新为主。

```sql
ALTER TABLE account_balance
ADD CONSTRAINT chk_available_balance_non_negative CHECK (available_balance >= 0);

ALTER TABLE account_balance
ADD CONSTRAINT chk_frozen_balance_non_negative CHECK (frozen_balance >= 0);
```

金额入参校验建议保留在 DTO 中：

```java
@NotNull(message = "金额不能为空")
@DecimalMin(value = "0.000001", message = "金额必须大于0")
private BigDecimal amount;
```

Service 层再做一次标准化和兜底判断：

```java
BigDecimal amount = normalize(dto.getAmount());
if (!NumberUtil.isGreater(amount, BigDecimal.ZERO)) {
    throw new BusinessException("金额必须大于0");
}
```

最终形成三层保护：

```text
DTO 参数校验：防止非法请求进入业务层
Service 金额校验：防止绕过 Controller 的内部调用传入非法金额
SQL 条件更新：防止并发场景余额扣成负数
```

## 事务一致性处理

账户模块必须保证账户余额和账户流水一致。不能出现“余额变了但没有流水”，也不能出现“流水生成了但余额没变”。因此，余额更新、流水插入、幂等状态更新必须放在同一个本地事务内。

### 本地事务边界

本案例优先使用 MySQL 本地事务，不引入分布式事务。账户余额表、账户流水表、幂等记录表在同一个数据库内，使用本地事务即可满足核心一致性。

事务边界建议放在 Service 层，不要放在 Controller 层。

推荐事务范围：

```text
开始事务
-> 查询账户
-> 查询重复流水或幂等记录
-> 更新账户余额
-> 插入账户流水
-> 更新幂等状态
提交事务
```

不应该放进事务的大操作：

```text
调用第三方支付接口
发送 MQ 消息
发送短信
调用远程服务
大文件处理
复杂报表统计
```

这些操作耗时长、失败率高，放在账户事务中会增加锁持有时间，影响账户并发性能。

当前示例使用 `TransactionTemplate`：

```java
return executeWithAccountLock(dto.getUserId(), dto.getAccountType(), () ->
        transactionTemplate.execute(status -> {
            AccountBalance account = getAccount(dto.getUserId(), dto.getAccountType());
            if (account == null) {
                throw new BusinessException("账户不存在，请先开户");
            }

            AccountFlow existFlow = getFlow(dto.getBizNo(), flowType);
            if (existFlow != null) {
                checkDuplicateAmount(existFlow, amount);
                return toBalanceVO(account);
            }

            int updated = updater.update(account);
            if (updated <= 0) {
                throw new BusinessException("账户余额更新失败，请重试");
            }

            AccountFlow flow = buildFlow(
                    account,
                    flowType,
                    direction,
                    amount,
                    beforeAvailable,
                    afterAvailable,
                    beforeFrozen,
                    afterFrozen,
                    dto.getBizNo(),
                    originBizNo,
                    dto.getRemark()
            );
            accountFlowMapper.insert(flow);

            AccountBalanceVO response = toBalanceVO(account);
            markIdempotentSuccess(idemKey, response);

            return response;
        })
);
```

如果使用声明式事务，也可以这样写：

```java
@Transactional(rollbackFor = Exception.class)
public AccountBalanceVO doOperateInTransaction(AccountOperateDTO dto) {
    // 查询账户
    // 校验幂等
    // 更新余额
    // 写入流水
    // 更新幂等状态
    return result;
}
```

不过在分布式锁场景下，`TransactionTemplate` 的事务边界更直观，能明确保证事务执行结束后再释放锁。

### 账户余额与流水一致性

账户余额表保存当前状态，账户流水表保存变更过程。两者必须保持一致。

一致性要求：

```text
1. 每一次余额变化必须有流水
2. 每一条余额变化流水必须能对应到账户
3. 流水中的变更前余额必须等于操作前账户余额
4. 流水中的变更后余额必须等于操作后账户余额
5. 账户更新失败时不能写流水
6. 流水写入失败时账户余额必须回滚
```

核心实现顺序：

```text
先记录操作前余额
-> 执行 SQL 更新账户余额
-> 根据操作类型计算操作后余额
-> 写入账户流水
-> 返回操作后余额
```

关键代码：

```java
BigDecimal beforeAvailable = account.getAvailableBalance();
BigDecimal beforeFrozen = account.getFrozenBalance();

BigDecimal afterAvailable = normalize(afterAvailableFunc.calculate(account));
BigDecimal afterFrozen = normalize(afterFrozenFunc.calculate(account));

int updated = updater.update(account);
if (updated <= 0) {
    throw new BusinessException(StrUtil.blankToDefault(failMessage, "账户余额更新失败，请重试"));
}

AccountFlow flow = buildFlow(
        account,
        flowType,
        direction,
        amount,
        beforeAvailable,
        afterAvailable,
        beforeFrozen,
        afterFrozen,
        dto.getBizNo(),
        originBizNo,
        dto.getRemark()
);
accountFlowMapper.insert(flow);
```

建议增加一个账户流水核对 SQL，用于排查账户不一致问题。

```sql
SELECT
    b.account_no,
    b.available_balance,
    b.frozen_balance,
    f.after_available_balance AS last_flow_available_balance,
    f.after_frozen_balance AS last_flow_frozen_balance,
    f.flow_no,
    f.create_time
FROM account_balance b
JOIN account_flow f ON b.account_no = f.account_no
WHERE b.account_no = 'AC2026051510010001'
ORDER BY f.create_time DESC
LIMIT 1;
```

如果账户当前余额和最后一条流水的变更后余额不一致，说明可能存在以下问题：

```text
有代码绕过账户服务直接修改了账户表
某次余额更新没有生成流水
历史数据被人工修改
事务配置错误，导致余额提交但流水回滚
```

生产项目中建议定时做轻量级巡检：

```text
按账户查询最后一条流水
对比 account_balance 当前余额
不一致则写入异常账务巡检表
人工确认后再做补偿
```

### 异常回滚策略

账户模块的异常回滚策略要明确：只要余额更新、流水插入、幂等记录更新任一核心步骤失败，就必须回滚整个事务。

推荐异常分类：

| 异常             | 是否回滚 | 处理方式                       |
| ---------------- | -------- | ------------------------------ |
| 账户不存在       | 回滚     | 抛业务异常                     |
| 账户状态不可用   | 回滚     | 抛业务异常                     |
| 余额不足         | 回滚     | 抛业务异常                     |
| 冻结余额不足     | 回滚     | 抛业务异常                     |
| 乐观锁更新失败   | 回滚     | 抛业务异常或有限重试           |
| 流水唯一索引冲突 | 回滚     | 查询已有流水后判断是否重复请求 |
| 幂等唯一索引冲突 | 回滚     | 返回重复请求提示或上次结果     |
| 数据库连接异常   | 回滚     | 抛系统异常                     |
| 运行时异常       | 回滚     | 抛系统异常                     |

业务异常默认继承 `RuntimeException`，可以触发 Spring 事务回滚：

```java
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
```

使用声明式事务时必须指定：

```java
@Transactional(rollbackFor = Exception.class)
```

使用 `TransactionTemplate` 时，只要内部抛出运行时异常，事务会自动回滚。

```java
transactionTemplate.execute(status -> {
    try {
        // 账户余额更新
        // 账户流水写入
        return result;
    } catch (BusinessException e) {
        log.warn("账户业务处理失败：{}", e.getMessage());
        throw e;
    } catch (Exception e) {
        log.error("账户系统处理异常", e);
        throw new BusinessException("账户操作失败，请稍后重试");
    }
});
```

如果需要在捕获异常后手动回滚，可以使用：

```java
transactionTemplate.execute(status -> {
    try {
        // 核心业务逻辑
        return result;
    } catch (Exception e) {
        status.setRollbackOnly();
        log.error("账户事务执行失败，已标记回滚", e);
        throw e;
    }
});
```

不建议在事务内吞掉异常：

```java
try {
    accountFlowMapper.insert(flow);
} catch (Exception e) {
    log.error("流水写入失败", e);
}
```

这种写法会导致账户余额已经更新，但流水没有写入，最终出现账实不一致。

正确写法是直接抛出异常，让事务回滚：

```java
try {
    accountFlowMapper.insert(flow);
} catch (Exception e) {
    log.error("账户流水写入失败，准备回滚事务，bizNo={}, flowType={}", dto.getBizNo(), flowType, e);
    throw new BusinessException("账户流水写入失败");
}
```

最终建议采用以下回滚策略：

```text
余额更新失败：抛异常，回滚
流水写入失败：抛异常，回滚
幂等记录失败：抛异常，回滚
重复请求且金额一致：不更新余额，直接返回
重复请求但金额不一致：抛异常，回滚
未知异常：记录日志，抛统一异常，回滚
```

账户模块不要为了“接口尽量成功”而吞异常。账户数据宁可失败重试，也不能产生不完整账务记录。

## 接口测试与验证

本节通过 `curl`、SQL 查询和一个并发测试类验证账户模块的核心能力。重点验证开户、入账、出账、冻结、解冻、冻结扣减、退款冲正、并发扣减和幂等重复请求，覆盖 README 中该场景强调的并发、幂等、流水完整、余额不能为负和事务一致性问题。

测试前先确认服务、MySQL、Redis 都已经启动，并且已经执行过前面章节的建表 SQL。

```bash
# 启动 Spring Boot 项目
mvn spring-boot:run

# 确认接口服务可访问
curl 'http://localhost:8080/api/accounts/flows?pageNo=1&pageSize=1'
```

建议每次测试前清理测试用户数据，避免历史数据影响验证结果。

```sql
-- 测试用户 ID：10001
DELETE FROM account_idempotent WHERE biz_no LIKE '%202605150001%' OR biz_no LIKE '%TEST%';

DELETE FROM account_flow
WHERE user_id = 10001;

DELETE FROM account_balance
WHERE user_id = 10001;
```

### 开户接口测试

开户测试用于验证同一用户同一账户类型只能创建一个账户。第一次请求创建账户，第二次请求应直接返回已有账户，不应重复插入账户记录。

```bash
curl -X POST 'http://localhost:8080/api/accounts/open' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET"
  }'
```

预期响应：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "accountNo": "AC20260515xxxxxxxx",
    "userId": 10001,
    "accountType": "WALLET",
    "availableBalance": 0.000000,
    "frozenBalance": 0.000000,
    "status": 1
  }
}
```

再次执行同一个开户请求：

```bash
curl -X POST 'http://localhost:8080/api/accounts/open' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET"
  }'
```

验证账户表只能有一条记录：

```sql
SELECT
    id,
    account_no,
    user_id,
    account_type,
    available_balance,
    frozen_balance,
    status,
    version
FROM account_balance
WHERE user_id = 10001
  AND account_type = 'WALLET';
```

验证开户流水：

```sql
SELECT
    flow_no,
    account_no,
    user_id,
    account_type,
    flow_type,
    amount,
    before_available_balance,
    after_available_balance,
    before_frozen_balance,
    after_frozen_balance,
    biz_no
FROM account_flow
WHERE user_id = 10001
ORDER BY create_time ASC;
```

预期结果：

```text
account_balance 只有 1 条 WALLET 账户记录
account_flow 至少有 1 条 OPEN 流水
available_balance = 0.000000
frozen_balance = 0.000000
```

### 入账接口测试

入账测试用于验证账户可用余额增加、流水生成和业务单号幂等。

```bash
curl -X POST 'http://localhost:8080/api/accounts/income' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "100.00",
    "bizNo": "RECHARGE202605150001",
    "remark": "用户充值到账"
  }'
```

验证账户余额：

```sql
SELECT
    account_no,
    available_balance,
    frozen_balance,
    version
FROM account_balance
WHERE user_id = 10001
  AND account_type = 'WALLET';
```

预期结果：

```text
available_balance = 100.000000
frozen_balance = 0.000000
version 比开户后增加 1
```

验证入账流水：

```sql
SELECT
    flow_type,
    direction,
    amount,
    before_available_balance,
    after_available_balance,
    before_frozen_balance,
    after_frozen_balance,
    biz_no,
    remark
FROM account_flow
WHERE biz_no = 'RECHARGE202605150001'
  AND flow_type = 'INCOME';
```

预期流水：

```text
flow_type = INCOME
direction = IN
amount = 100.000000
before_available_balance = 0.000000
after_available_balance = 100.000000
before_frozen_balance = 0.000000
after_frozen_balance = 0.000000
```

### 出账接口测试

出账测试用于验证可用余额扣减和余额不足拦截。

先执行一笔正常出账：

```bash
curl -X POST 'http://localhost:8080/api/accounts/outcome' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "30.00",
    "bizNo": "PAY202605150001",
    "remark": "余额直接支付"
  }'
```

验证余额：

```sql
SELECT
    available_balance,
    frozen_balance
FROM account_balance
WHERE user_id = 10001
  AND account_type = 'WALLET';
```

预期结果：

```text
available_balance = 70.000000
frozen_balance = 0.000000
```

再测试余额不足：

```bash
curl -X POST 'http://localhost:8080/api/accounts/outcome' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "9999.00",
    "bizNo": "PAY202605150002",
    "remark": "余额不足测试"
  }'
```

预期响应：

```json
{
  "code": 500,
  "message": "可用余额不足，出账失败",
  "data": null
}
```

验证失败请求没有生成出账流水：

```sql
SELECT COUNT(*) AS flow_count
FROM account_flow
WHERE biz_no = 'PAY202605150002'
  AND flow_type = 'OUTCOME';
```

预期结果：

```text
flow_count = 0
```

### 冻结与解冻测试

冻结与解冻测试用于验证可用余额和冻结余额之间的转换。冻结会减少可用余额并增加冻结余额；解冻会减少冻结余额并增加可用余额。

先冻结 50 元：

```bash
curl -X POST 'http://localhost:8080/api/accounts/freeze' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "50.00",
    "bizNo": "ORDER202605150001",
    "remark": "订单余额冻结"
  }'
```

验证冻结后余额：

```sql
SELECT
    available_balance,
    frozen_balance
FROM account_balance
WHERE user_id = 10001
  AND account_type = 'WALLET';
```

预期结果：

```text
冻结前余额：available_balance = 70.000000，frozen_balance = 0.000000
冻结后余额：available_balance = 20.000000，frozen_balance = 50.000000
```

如果要测试解冻，可以先使用新的订单号冻结一笔金额，再解冻同一笔金额。这里使用 `ORDER202605150002` 避免和后续冻结扣减测试冲突。

```bash
# 冻结 10 元
curl -X POST 'http://localhost:8080/api/accounts/freeze' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "10.00",
    "bizNo": "ORDER202605150002",
    "remark": "订单余额冻结-用于解冻测试"
  }'

# 解冻 10 元
curl -X POST 'http://localhost:8080/api/accounts/unfreeze' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "10.00",
    "bizNo": "ORDER202605150002",
    "remark": "订单取消解冻余额"
  }'
```

验证冻结与解冻流水：

```sql
SELECT
    flow_type,
    direction,
    amount,
    before_available_balance,
    after_available_balance,
    before_frozen_balance,
    after_frozen_balance,
    biz_no
FROM account_flow
WHERE biz_no = 'ORDER202605150002'
ORDER BY create_time ASC;
```

预期结果：

```text
第一条：FREEZE，available 减少 10，frozen 增加 10
第二条：UNFREEZE，available 增加 10，frozen 减少 10
```

### 扣减冻结金额测试

扣减冻结金额用于验证“冻结后确认扣款”的链路。它只扣减冻结余额，不再次扣减可用余额。

前面已经使用 `ORDER202605150001` 冻结了 50 元，现在执行冻结扣减：

```bash
curl -X POST 'http://localhost:8080/api/accounts/frozen-deduct' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "50.00",
    "bizNo": "ORDER202605150001",
    "remark": "订单支付确认扣减冻结金额"
  }'
```

验证余额：

```sql
SELECT
    available_balance,
    frozen_balance
FROM account_balance
WHERE user_id = 10001
  AND account_type = 'WALLET';
```

预期结果：

```text
扣减前：available_balance = 20.000000，frozen_balance = 50.000000
扣减后：available_balance = 20.000000，frozen_balance = 0.000000
```

验证订单完整流水：

```sql
SELECT
    flow_type,
    direction,
    amount,
    before_available_balance,
    after_available_balance,
    before_frozen_balance,
    after_frozen_balance,
    biz_no
FROM account_flow
WHERE biz_no = 'ORDER202605150001'
ORDER BY create_time ASC;
```

预期结果：

```text
FREEZE：available 70 -> 20，frozen 0 -> 50
FROZEN_DEDUCT：available 20 -> 20，frozen 50 -> 0
```

### 退款冲正测试

退款冲正用于验证反向入账能力。退款使用新的退款业务单号 `REFUND202605150001`，通过 `originBizNo` 关联原订单号 `ORDER202605150001`。

```bash
curl -X POST 'http://localhost:8080/api/accounts/reverse' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "50.00",
    "bizNo": "REFUND202605150001",
    "originBizNo": "ORDER202605150001",
    "remark": "订单退款冲正"
  }'
```

验证余额：

```sql
SELECT
    available_balance,
    frozen_balance
FROM account_balance
WHERE user_id = 10001
  AND account_type = 'WALLET';
```

预期结果：

```text
退款前：available_balance = 20.000000，frozen_balance = 0.000000
退款后：available_balance = 70.000000，frozen_balance = 0.000000
```

验证冲正流水：

```sql
SELECT
    flow_type,
    direction,
    amount,
    biz_no,
    origin_biz_no,
    before_available_balance,
    after_available_balance
FROM account_flow
WHERE biz_no = 'REFUND202605150001'
  AND flow_type = 'REVERSE';
```

预期结果：

```text
flow_type = REVERSE
direction = IN
amount = 50.000000
biz_no = REFUND202605150001
origin_biz_no = ORDER202605150001
available 增加 50
```

### 并发扣减测试

并发扣减测试用于验证高并发下余额不会被扣成负数。测试思路是先给测试用户入账固定金额，然后并发发起多次出账请求，最终成功次数不能超过账户余额可支持的次数。

先准备一个新的测试用户 `20001`：

```bash
# 开户
curl -X POST 'http://localhost:8080/api/accounts/open' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 20001,
    "accountType": "WALLET"
  }'

# 入账 100 元
curl -X POST 'http://localhost:8080/api/accounts/income' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 20001,
    "accountType": "WALLET",
    "amount": "100.00",
    "bizNo": "CONCURRENT_RECHARGE_TEST",
    "remark": "并发扣减测试入账"
  }'
```

下面使用 JUnit + Hutool HTTP 客户端模拟 20 个并发请求，每个请求扣减 10 元。理论上最多只能成功 10 次，最终余额不能小于 0。

这个测试类通过 Hutool `HttpRequest` 并发调用出账接口，用于验证账户扣减不会超扣。

文件位置：`src/test/java/io/github/atengk/wallet/AccountConcurrentTest.java`

```java
package io.github.atengk.wallet;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * 账户并发扣减测试
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public class AccountConcurrentTest {

    private static final String OUTCOME_URL = "http://localhost:8080/api/accounts/outcome";

    /**
     * 并发扣减测试
     *
     * @throws InterruptedException 线程等待异常
     */
    @Test
    void concurrentOutcomeTest() throws InterruptedException {
        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = ThreadUtil.newExecutor(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            executorService.execute(() -> {
                try {
                    startLatch.await();

                    Map<String, Object> body = Map.of(
                            "userId", 20001,
                            "accountType", "WALLET",
                            "amount", "10.00",
                            "bizNo", "CONCURRENT_OUTCOME_TEST_" + index,
                            "remark", "并发扣减测试-" + index
                    );

                    String response = HttpRequest.post(OUTCOME_URL)
                            .header("Content-Type", "application/json")
                            .body(JSONUtil.toJsonStr(body))
                            .timeout(5000)
                            .execute()
                            .body();

                    log.info("并发扣减响应，index={}，response={}", index, response);
                } catch (Exception e) {
                    log.warn("并发扣减请求异常，index={}", index, e);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await();
        executorService.shutdown();
    }
}
```

执行并发测试后，验证账户余额：

```sql
SELECT
    available_balance,
    frozen_balance
FROM account_balance
WHERE user_id = 20001
  AND account_type = 'WALLET';
```

预期结果：

```text
available_balance >= 0
frozen_balance = 0.000000
```

验证成功出账流水数量：

```sql
SELECT COUNT(*) AS success_count
FROM account_flow
WHERE user_id = 20001
  AND flow_type = 'OUTCOME'
  AND biz_no LIKE 'CONCURRENT_OUTCOME_TEST_%';
```

预期结果：

```text
success_count <= 10
```

如果初始余额是 100，每次扣减 10，并发 20 次，则最多成功 10 次。其余请求应该返回“可用余额不足，出账失败”或“账户操作繁忙，请稍后重试”，但不能出现余额为负。

### 幂等重复请求测试

幂等重复请求测试用于验证同一个业务单号重复请求不会重复修改余额，也不会重复生成流水。

先给用户 `10001` 执行一笔入账：

```bash
curl -X POST 'http://localhost:8080/api/accounts/income' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "20.00",
    "bizNo": "IDEM_INCOME_TEST_001",
    "remark": "幂等入账测试"
  }'
```

重复执行相同请求：

```bash
curl -X POST 'http://localhost:8080/api/accounts/income' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "20.00",
    "bizNo": "IDEM_INCOME_TEST_001",
    "remark": "幂等入账测试"
  }'
```

验证流水只生成一条：

```sql
SELECT COUNT(*) AS flow_count
FROM account_flow
WHERE biz_no = 'IDEM_INCOME_TEST_001'
  AND flow_type = 'INCOME';
```

预期结果：

```text
flow_count = 1
```

验证同一业务单号但金额不一致时应拒绝处理：

```bash
curl -X POST 'http://localhost:8080/api/accounts/income' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "accountType": "WALLET",
    "amount": "30.00",
    "bizNo": "IDEM_INCOME_TEST_001",
    "remark": "幂等入账金额不一致测试"
  }'
```

预期响应：

```json
{
  "code": 500,
  "message": "业务单号已处理，但请求金额不一致",
  "data": null
}
```

验证账户余额没有因为第三次请求增加：

```sql
SELECT
    available_balance,
    frozen_balance
FROM account_balance
WHERE user_id = 10001
  AND account_type = 'WALLET';
```

最终可以用下面 SQL 汇总检查账户和流水状态：

```sql
SELECT
    b.user_id,
    b.account_type,
    b.available_balance,
    b.frozen_balance,
    COUNT(f.id) AS flow_count
FROM account_balance b
LEFT JOIN account_flow f ON b.account_no = f.account_no
WHERE b.user_id IN (10001, 20001)
GROUP BY
    b.user_id,
    b.account_type,
    b.available_balance,
    b.frozen_balance;
```

最终验证目标：

```text
1. 开户重复请求不会重复创建账户
2. 入账重复请求不会重复增加余额
3. 出账余额不足不会生成流水
4. 冻结会同时修改可用余额和冻结余额
5. 解冻会释放冻结余额到可用余额
6. 扣减冻结金额不会再次扣减可用余额
7. 退款冲正会增加可用余额，并记录原业务单号
8. 并发扣减后余额不能为负
9. 每一次成功余额变更都有对应账户流水
10. 账户当前余额和最后一条流水的变更后余额应保持一致
```