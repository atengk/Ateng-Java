# 优惠券 / 权益发放与核销

本案例围绕“配置权益规则、用户领取、库存扣减、下单锁定、支付核销、取消回退、过期失效”这条主链路实现，重点覆盖优惠券场景中最常见的并发扣减、重复领取、状态流转和核销幂等问题。

## 场景目标与功能边界

优惠券 / 权益发放与核销模块主要用于营销活动、会员权益、平台补贴、兑换券、服务权益等业务。它的核心目标不是简单地给用户插入一条优惠券记录，而是保证在高并发领取、下单锁定、支付核销、订单取消回退等场景下，优惠券状态准确、库存不超发、用户不能重复领取、核销不会重复执行。

本案例按一个标准电商优惠券场景实现：后台创建优惠券模板，用户领取优惠券，下单时选择优惠券并锁定，支付成功后核销，订单取消后回退。系统同时通过定时任务处理过期优惠券和长时间锁定未核销的异常数据。

### 核心业务流程

优惠券主流程如下：

```text
后台创建优惠券模板
-> 启用优惠券模板
-> 预热优惠券库存到 Redis
-> 用户领取优惠券
-> 校验活动时间
-> 校验领取资格
-> 校验是否重复领取
-> Redis 原子扣减库存
-> 创建用户优惠券记录
-> 下单时选择优惠券
-> 校验优惠券状态
-> 校验订单金额门槛
-> 锁定用户优惠券
-> 支付成功核销优惠券
-> 订单取消回退优惠券
-> 到期后自动置为失效
```

核心状态流转如下：

```text
用户优惠券状态：

未使用
  -> 已锁定
  -> 已核销

未使用
  -> 已过期

已锁定
  -> 已核销

已锁定
  -> 未使用

已锁定
  -> 已过期
```

其中：

| 操作     | 前置状态        | 目标状态 | 说明                           |
| -------- | --------------- | -------- | ------------------------------ |
| 用户领取 | 无              | 未使用   | 创建用户优惠券记录             |
| 下单锁定 | 未使用          | 已锁定   | 防止同一张券被多个订单同时使用 |
| 支付核销 | 已锁定          | 已核销   | 支付成功后优惠券最终消费       |
| 订单取消 | 已锁定          | 未使用   | 用户未支付或主动取消时回退     |
| 到期处理 | 未使用 / 已锁定 | 已过期   | 超过有效期后不可再使用         |

领取流程重点解决“库存并发扣减”和“重复领取”：

```text
用户请求领取
-> 查询优惠券模板
-> 判断模板是否启用
-> 判断当前时间是否在领取时间内
-> 判断用户是否已领取
-> 执行 Redis Lua 脚本
   -> 判断库存是否充足
   -> 判断用户是否已领取
   -> 扣减库存
   -> 记录用户领取标识
-> 写入用户优惠券表
-> 返回领取成功
```

锁定流程重点解决“下单时优惠券被重复使用”：

```text
用户提交订单
-> 查询用户优惠券
-> 判断优惠券是否属于当前用户
-> 判断状态是否为未使用
-> 判断是否过期
-> 判断订单金额是否满足门槛
-> 使用数据库乐观锁更新状态为已锁定
-> 绑定订单号
-> 返回可抵扣金额
```

核销流程重点解决“支付回调重复通知”：

```text
支付成功回调
-> 根据订单号查询锁定的优惠券
-> 判断优惠券是否已核销
-> 已核销则直接返回成功
-> 未核销则更新状态为已核销
-> 写入核销流水
-> 返回处理成功
```

回退流程重点解决“订单取消后权益释放”：

```text
订单取消 / 支付超时
-> 根据订单号查询锁定的优惠券
-> 判断优惠券状态是否为已锁定
-> 判断优惠券是否已过期
-> 未过期则回退为未使用
-> 已过期则置为已过期
-> 清空订单绑定关系
-> 写入回退流水
```

### 本案例实现范围

本案例只实现优惠券 / 权益系统的核心链路，重点放在可落地的后端代码和数据库设计上，不展开复杂营销规则引擎。

实现内容包括：

| 模块           | 是否实现 | 说明                                        |
| -------------- | -------- | ------------------------------------------- |
| 优惠券模板创建 | 实现     | 后台创建固定金额满减券                      |
| 优惠券模板启用 | 实现     | 启用后允许用户领取                          |
| Redis 库存预热 | 实现     | 将模板库存加载到 Redis                      |
| 用户领取优惠券 | 实现     | 使用 Lua 保证库存扣减和重复领取控制的原子性 |
| 用户优惠券查询 | 实现     | 查询当前用户可用券                          |
| 下单锁定优惠券 | 实现     | 校验门槛并锁定券                            |
| 支付成功核销   | 实现     | 幂等核销优惠券                              |
| 订单取消回退   | 实现     | 将已锁定券回退为未使用或过期                |
| 优惠券过期任务 | 实现     | 定时将过期券置为失效                        |
| 长时间锁定回退 | 实现     | 处理支付超时未回调的数据                    |
| 复杂叠加规则   | 不实现   | 不处理多券叠加、平台券与商家券组合          |
| 商品范围限制   | 简化实现 | 仅预留字段，不展开 SKU / 类目匹配           |
| 会员等级权益   | 不实现   | 后续可扩展到权益中心                        |
| 分布式事务     | 不强依赖 | 本案例以本地事务、幂等和补偿为主            |
| MQ 异步发放    | 可扩展   | 核心案例先使用同步链路实现                  |

本案例默认使用以下技术栈：

```text
Spring Boot 3
MyBatis-Plus
MySQL 8
Redis
Redisson
Lua
XXL-JOB
Sa-Token
Hutool
Lombok
```

核心约束如下：

```text
1. 同一个用户对同一个优惠券模板只能领取一次。
2. 优惠券模板库存不能超发。
3. 用户优惠券只有“未使用”状态才能被锁定。
4. 用户优惠券只有“已锁定”状态才能被核销。
5. 支付回调重复触发时，核销接口必须幂等。
6. 订单取消时，只能回退仍处于“已锁定”的优惠券。
7. 已过期优惠券不能被锁定、核销或回退为可用。
8. 所有关键状态变更必须写入流水，方便排查问题。
```

本案例后续代码会按“最小可运行核心链路”组织，不追求大而全，优先保证领取、锁定、核销、回退这几类高频高风险操作完整闭环。

## 技术栈与工程结构

本案例延续 README 中对“优惠券 / 权益发放与核销”场景推荐的 Spring Boot、MyBatis-Plus、Redis、Redisson、RabbitMQ、XXL-JOB、Lua、MySQL、Sa-Token 技术栈，并在实现上优先保证核心链路闭环：领取、锁定、核销、回退、过期处理。

### 技术栈选型

| 技术          | 用途                                           |
| ------------- | ---------------------------------------------- |
| Spring Boot 3 | 后端基础框架                                   |
| MyBatis-Plus  | 数据库 CRUD、乐观锁、分页                      |
| MySQL 8       | 存储优惠券模板、用户券、流水、幂等记录         |
| Redis         | 优惠券库存、领取限制、热点数据缓存             |
| Redisson      | 分布式锁、辅助并发控制                         |
| Lua           | Redis 原子扣减库存、重复领取校验               |
| XXL-JOB       | 优惠券过期、锁定超时回退、补偿任务             |
| Sa-Token      | 获取当前登录用户                               |
| Hutool        | 日期、集合、字符串、JSON、ID 等工具类          |
| Lombok        | 简化实体、DTO、VO 代码                         |
| RabbitMQ      | 可选，用于后续异步发放、核销通知、营销事件扩展 |

核心设计取舍：

```text
1. 领取阶段使用 Redis + Lua 保证库存扣减和重复领取的原子性。
2. 锁定、核销、回退阶段以 MySQL 状态机为准，使用数据库条件更新保证幂等。
3. Redis 用于高并发前置拦截，MySQL 用于最终业务事实落库。
4. 定时任务负责兜底处理过期券、长时间锁定券和异常状态。
5. 本案例不引入复杂规则引擎，先实现固定金额满减券的核心链路。
```

### 项目包结构

项目默认基础包名为 `io.github.atengk.coupon`，按 Controller、Service、Mapper、Entity、DTO、VO、Enum、Job、Lua 脚本分层。

```text
coupon-service
├── pom.xml
├── src/main/java/io/github/atengk/coupon
│   ├── CouponApplication.java
│   ├── controller
│   │   ├── CouponTemplateController.java
│   │   └── UserCouponController.java
│   ├── service
│   │   ├── CouponTemplateService.java
│   │   ├── UserCouponService.java
│   │   └── CouponWriteOffService.java
│   ├── service/impl
│   │   ├── CouponTemplateServiceImpl.java
│   │   ├── UserCouponServiceImpl.java
│   │   └── CouponWriteOffServiceImpl.java
│   ├── mapper
│   │   ├── CouponTemplateMapper.java
│   │   ├── UserCouponMapper.java
│   │   ├── CouponFlowMapper.java
│   │   └── IdempotentRecordMapper.java
│   ├── entity
│   │   ├── CouponTemplate.java
│   │   ├── UserCoupon.java
│   │   ├── CouponFlow.java
│   │   └── IdempotentRecord.java
│   ├── dto
│   │   ├── CouponTemplateCreateDTO.java
│   │   ├── CouponReceiveDTO.java
│   │   ├── CouponLockDTO.java
│   │   ├── CouponWriteOffDTO.java
│   │   └── CouponRollbackDTO.java
│   ├── vo
│   │   ├── UserCouponVO.java
│   │   └── CouponLockVO.java
│   ├── enums
│   │   ├── CouponTemplateStatusEnum.java
│   │   ├── UserCouponStatusEnum.java
│   │   ├── CouponFlowTypeEnum.java
│   │   └── IdempotentStatusEnum.java
│   ├── job
│   │   ├── CouponExpireJob.java
│   │   └── CouponLockTimeoutRollbackJob.java
│   └── common
│       ├── RedisKeyConstants.java
│       ├── Result.java
│       └── BizException.java
└── src/main/resources
    ├── application.yml
    ├── mapper
    │   ├── CouponTemplateMapper.xml
    │   ├── UserCouponMapper.xml
    │   ├── CouponFlowMapper.xml
    │   └── IdempotentRecordMapper.xml
    └── lua
        └── coupon_receive.lua
```

建议模块边界如下：

| 模块             | 职责                                           |
| ---------------- | ---------------------------------------------- |
| CouponTemplate   | 管理优惠券模板、总库存、领取时间、使用门槛     |
| UserCoupon       | 管理用户维度的券实例，处理领取、锁定、回退     |
| CouponWriteOff   | 处理支付成功后的核销幂等                       |
| CouponFlow       | 记录领取、锁定、核销、回退、过期等流水         |
| IdempotentRecord | 记录请求幂等，主要用于核销、回退等外部回调场景 |
| Job              | 处理过期失效和锁定超时补偿                     |

### 核心依赖配置

下面是本案例需要的 Maven 核心依赖，放在 `coupon-service/pom.xml` 中。

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation：接口参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- MyBatis-Plus：简化数据库 CRUD 和分页操作 -->
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

    <!-- Redis：缓存库存、领取标记、热点数据 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Redisson：分布式锁和高级 Redis 能力 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.31.0</version>
    </dependency>

    <!-- Sa-Token：登录认证和用户上下文获取 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>1.39.0</version>
    </dependency>

    <!-- Hutool：日期、集合、字符串、JSON、ID 等常用工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.29</version>
    </dependency>

    <!-- XXL-JOB：定时任务调度，用于过期券和锁定超时补偿 -->
    <dependency>
        <groupId>com.xuxueli</groupId>
        <artifactId>xxl-job-core</artifactId>
        <version>2.4.1</version>
    </dependency>

    <!-- Lombok：简化实体类、DTO、VO 的 Getter、Setter、Builder -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring AMQP：后续扩展 MQ 异步发放、核销消息时使用 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
</dependencies>
```

基础配置放在 `src/main/resources/application.yml`。

```yaml
server:
  port: 8080

spring:
  application:
    name: coupon-service

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/coupon_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000ms

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

mybatis-plus:
  mapper-locations: classpath:/mapper/*.xml
  type-aliases-package: io.github.atengk.coupon.entity
  configuration:
    # 开发阶段便于查看 SQL，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 统一使用雪花 ID
      id-type: assign_id
      # 逻辑删除字段可按需扩展，本案例暂不启用
      logic-delete-field: deleted

sa-token:
  # Token 名称
  token-name: Authorization
  # Token 有效期，单位秒
  timeout: 86400
  # 允许从 Header 中读取 Token
  is-read-header: true

xxl:
  job:
    admin:
      addresses: http://localhost:8088/xxl-job-admin
    executor:
      appname: coupon-service
      address:
      ip:
      port: 9999
      logpath: ./logs/xxl-job
      logretentiondays: 7
    accessToken: default_token
```

## 数据模型设计

优惠券模块的数据模型要同时支撑“规则配置”和“用户券实例”两个维度。模板表负责描述券的规则和库存，用户券表负责记录用户真正持有的券，流水表负责审计每一次状态变更，幂等表负责处理回调、补偿等重复请求。

### 优惠券模板表

优惠券模板表表示一类优惠券，例如“满 100 减 20”。模板控制领取时间、使用时间、总库存、剩余库存、领取限制和使用门槛。

```sql
CREATE TABLE coupon_template (
    id BIGINT NOT NULL COMMENT '主键ID',
    template_name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    coupon_type TINYINT NOT NULL COMMENT '优惠券类型：1-满减券，2-折扣券，3-兑换券',
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    discount_rate DECIMAL(5, 2) DEFAULT NULL COMMENT '折扣比例，例如 8.50 表示 8.5 折',
    threshold_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛金额',
    total_stock INT NOT NULL COMMENT '总库存',
    available_stock INT NOT NULL COMMENT '剩余库存',
    receive_limit INT NOT NULL DEFAULT 1 COMMENT '每个用户可领取数量',
    receive_start_time DATETIME NOT NULL COMMENT '领取开始时间',
    receive_end_time DATETIME NOT NULL COMMENT '领取结束时间',
    use_start_time DATETIME NOT NULL COMMENT '使用开始时间',
    use_end_time DATETIME NOT NULL COMMENT '使用结束时间',
    status TINYINT NOT NULL COMMENT '状态：0-草稿，1-启用，2-停用，3-结束',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status_receive_time (status, receive_start_time, receive_end_time),
    KEY idx_use_time (use_start_time, use_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';
```

字段说明：

| 字段                                  | 说明                                   |
| ------------------------------------- | -------------------------------------- |
| total_stock                           | 模板总库存，创建后原则上不随领取变化   |
| available_stock                       | 数据库剩余库存，用于最终校验和后台展示 |
| receive_limit                         | 用户领取限制，本案例固定为 1           |
| receive_start_time / receive_end_time | 控制用户是否允许领取                   |
| use_start_time / use_end_time         | 控制用户券是否允许使用                 |
| status                                | 控制模板是否可领取                     |
| version                               | 后续如果走数据库扣库存，可用于乐观锁   |

### 用户优惠券表

用户优惠券表记录某个用户领取到的一张具体优惠券。下单锁定、支付核销、订单取消回退都围绕这张用户券进行状态变更。

```sql
CREATE TABLE user_coupon (
    id BIGINT NOT NULL COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '优惠券模板ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coupon_name VARCHAR(100) NOT NULL COMMENT '优惠券名称冗余',
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    threshold_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛金额',
    use_start_time DATETIME NOT NULL COMMENT '使用开始时间',
    use_end_time DATETIME NOT NULL COMMENT '使用结束时间',
    status TINYINT NOT NULL COMMENT '状态：1-未使用，2-已锁定，3-已核销，4-已过期',
    order_no VARCHAR(64) DEFAULT NULL COMMENT '绑定订单号',
    lock_time DATETIME DEFAULT NULL COMMENT '锁定时间',
    use_time DATETIME DEFAULT NULL COMMENT '核销时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_user (template_id, user_id),
    KEY idx_user_status_time (user_id, status, use_start_time, use_end_time),
    KEY idx_order_no (order_no),
    KEY idx_status_use_end_time (status, use_end_time),
    KEY idx_status_lock_time (status, lock_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';
```

关键索引说明：

| 索引                    | 作用                             |
| ----------------------- | -------------------------------- |
| uk_template_user        | 防止同一用户重复领取同一模板     |
| idx_user_status_time    | 查询用户可用券                   |
| idx_order_no            | 根据订单号查询锁定或核销的优惠券 |
| idx_status_use_end_time | 定时任务扫描过期券               |
| idx_status_lock_time    | 定时任务扫描长时间锁定券         |

`uk_template_user` 是数据库层面的最终防线。即使 Redis 领取标记异常，数据库也可以阻止重复插入。

### 优惠券使用流水表

优惠券使用流水表记录用户券的关键状态变化，方便排查“为什么这张券不可用了”“什么时候被锁定了”“是否被订单回退过”等问题。

```sql
CREATE TABLE coupon_flow (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_coupon_id BIGINT NOT NULL COMMENT '用户优惠券ID',
    template_id BIGINT NOT NULL COMMENT '优惠券模板ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(64) DEFAULT NULL COMMENT '订单号',
    flow_type TINYINT NOT NULL COMMENT '流水类型：1-领取，2-锁定，3-核销，4-回退，5-过期',
    before_status TINYINT DEFAULT NULL COMMENT '变更前状态',
    after_status TINYINT NOT NULL COMMENT '变更后状态',
    biz_no VARCHAR(100) DEFAULT NULL COMMENT '业务单号，例如订单号、支付单号、任务批次号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '流水备注',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_coupon_id (user_coupon_id),
    KEY idx_user_id (user_id),
    KEY idx_order_no (order_no),
    KEY idx_biz_no (biz_no),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券使用流水表';
```

流水类型建议固定为枚举：

| 类型 | 说明               |
| ---- | ------------------ |
| 1    | 用户领取优惠券     |
| 2    | 下单锁定优惠券     |
| 3    | 支付成功核销优惠券 |
| 4    | 订单取消回退优惠券 |
| 5    | 优惠券过期失效     |

### 幂等记录表

幂等记录表主要用于支付核销、订单取消回退、定时补偿等场景。外部系统可能重复通知，定时任务也可能重复扫描，因此关键业务入口需要通过业务幂等号防重复处理。

```sql
CREATE TABLE idempotent_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    idem_key VARCHAR(128) NOT NULL COMMENT '幂等Key',
    biz_type VARCHAR(50) NOT NULL COMMENT '业务类型：COUPON_WRITE_OFF、COUPON_ROLLBACK 等',
    biz_no VARCHAR(100) NOT NULL COMMENT '业务单号，例如订单号、支付单号',
    status TINYINT NOT NULL COMMENT '状态：1-处理中，2-处理成功，3-处理失败',
    request_params TEXT DEFAULT NULL COMMENT '请求参数JSON',
    response_result TEXT DEFAULT NULL COMMENT '响应结果JSON',
    error_msg VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    expire_time DATETIME DEFAULT NULL COMMENT '幂等记录过期时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_idem_key (idem_key),
    KEY idx_biz_type_biz_no (biz_type, biz_no),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='幂等记录表';
```

幂等 Key 推荐设计：

```text
核销优惠券：
COUPON_WRITE_OFF:{orderNo}

订单取消回退：
COUPON_ROLLBACK:{orderNo}

过期任务处理：
COUPON_EXPIRE:{userCouponId}

锁定超时回退：
COUPON_LOCK_TIMEOUT:{userCouponId}
```

## 状态流转设计

状态流转是优惠券模块的核心。领取、锁定、核销、回退都必须基于明确的状态机执行，不能直接无条件更新数据，否则支付回调重复、订单取消重复、定时任务重复扫描时很容易导致状态错乱。

### 优惠券模板状态

优惠券模板状态控制“这类优惠券是否允许被领取”。

| 状态值 | 状态 | 说明               | 是否可领取 |
| ------ | ---- | ------------------ | ---------- |
| 0      | 草稿 | 后台创建后未启用   | 否         |
| 1      | 启用 | 当前模板可参与领取 | 是         |
| 2      | 停用 | 人工下架或活动暂停 | 否         |
| 3      | 结束 | 活动结束，不再领取 | 否         |

模板状态流转：

```text
草稿
  -> 启用
  -> 停用

启用
  -> 结束

停用
  -> 启用

停用
  -> 结束
```

领取时必须同时满足以下条件：

```text
1. 模板状态 = 启用
2. 当前时间 >= receive_start_time
3. 当前时间 <= receive_end_time
4. Redis 库存 > 0
5. 当前用户未领取过该模板
```

模板状态本身不决定用户券是否能使用。用户券能否使用还要看用户券状态和使用有效期。

### 用户优惠券状态

用户优惠券状态控制“某个用户手里的这张券是否可用”。

| 状态值 | 状态   | 说明                           |
| ------ | ------ | ------------------------------ |
| 1      | 未使用 | 用户已领取，尚未下单使用       |
| 2      | 已锁定 | 下单时已绑定订单，等待支付结果 |
| 3      | 已核销 | 支付成功后优惠券已经消费       |
| 4      | 已过期 | 超过使用有效期，不可再使用     |

用户券状态流转：

```text
未使用
  -> 已锁定
  -> 已核销

未使用
  -> 已过期

已锁定
  -> 未使用

已锁定
  -> 已核销

已锁定
  -> 已过期
```

非法流转必须拦截：

| 当前状态 | 禁止操作     | 原因                       |
| -------- | ------------ | -------------------------- |
| 未使用   | 核销         | 未绑定订单，不能直接核销   |
| 已锁定   | 再次锁定     | 防止一张券绑定多个订单     |
| 已核销   | 回退为未使用 | 已支付成功，优惠券不可恢复 |
| 已过期   | 锁定         | 已超过使用有效期           |
| 已过期   | 核销         | 已失效，不可消费           |
| 已过期   | 回退为未使用 | 不能恢复过期权益           |

### 领取、锁定、核销、回退流程

领取流程以 Redis 原子脚本为前置控制，MySQL 唯一索引为最终防线。

```text
领取优惠券：

请求参数：templateId
核心校验：
1. 模板存在
2. 模板状态为启用
3. 当前时间处于领取时间范围内
4. 用户未领取过该模板
5. Redis 库存充足

状态变化：
无用户券 -> 未使用

核心保障：
1. Redis Lua：库存扣减和领取标记原子执行
2. MySQL uk_template_user：防止重复领取落库
3. coupon_flow：记录领取流水
```

锁定流程必须通过条件更新保证只有“未使用”的券可以被锁定。

```text
锁定优惠券：

请求参数：userCouponId、orderNo、orderAmount
核心校验：
1. 用户券属于当前用户
2. 用户券状态为未使用
3. 当前时间处于使用有效期内
4. 订单金额满足 threshold_amount
5. 当前券未绑定其他订单

状态变化：
未使用 -> 已锁定

核心保障：
1. update where id = ? and status = 未使用
2. 写入 order_no 和 lock_time
3. coupon_flow：记录锁定流水
```

核销流程主要面对支付成功回调，必须支持重复调用。

```text
核销优惠券：

请求参数：orderNo、payNo
核心校验：
1. 根据 orderNo 查询用户券
2. 如果状态为已核销，直接返回成功
3. 如果状态不是已锁定，拒绝核销
4. 如果已超过使用有效期，拒绝核销或进入人工补偿

状态变化：
已锁定 -> 已核销

核心保障：
1. 幂等 Key：COUPON_WRITE_OFF:{orderNo}
2. update where order_no = ? and status = 已锁定
3. coupon_flow：记录核销流水
```

回退流程主要面对订单取消、支付超时关闭订单。只有“已锁定”的券允许回退。

```text
回退优惠券：

请求参数：orderNo、cancelReason
核心校验：
1. 根据 orderNo 查询用户券
2. 如果没有绑定优惠券，直接返回成功
3. 如果状态为未使用，说明已经回退过，直接返回成功
4. 如果状态为已核销，禁止回退
5. 如果状态为已锁定，判断是否过期

状态变化：
未过期：已锁定 -> 未使用
已过期：已锁定 -> 已过期

核心保障：
1. 幂等 Key：COUPON_ROLLBACK:{orderNo}
2. update where order_no = ? and status = 已锁定
3. coupon_flow：记录回退流水
```

整体链路中的关键原则：

```text
1. Redis 控制高并发入口。
2. MySQL 状态字段控制最终业务事实。
3. 所有状态更新必须带前置状态条件。
4. 外部回调和定时补偿必须做幂等。
5. 状态变更必须写流水。
6. 已核销状态不可逆。
7. 已过期权益不可恢复为可用。
```

## Redis Key 与 Lua 脚本设计

Redis 在本案例中主要用于解决高并发领取时的两个问题：库存不能超发、同一用户不能重复领取。真正的业务状态仍然以 MySQL 为准，Redis 只负责领取入口的原子控制。

Redis 领取链路遵循以下原则：先校验模板状态和领取时间，再执行 Lua 脚本扣减库存和写入用户领取标记，最后落库生成用户优惠券记录。该设计对应 README 中“库存并发扣减、重复领取控制、Lua、Redis”的核心要求。

### 库存扣减 Key

优惠券模板启用后，需要将模板库存预热到 Redis。领取时直接从 Redis 扣减库存，避免高并发请求直接打到 MySQL。

Key 设计如下：

```text
coupon:template:stock:{templateId}
```

示例：

```text
coupon:template:stock:10001
```

Value 说明：

```text
Redis String
值为当前优惠券模板的可领取库存数量
例如：5000
```

库存 Key 使用建议：

| 项目             | 说明                                      |
| ---------------- | ----------------------------------------- |
| Key              | `coupon:template:stock:{templateId}`      |
| 类型             | String                                    |
| 写入时机         | 优惠券模板启用或库存预热时                |
| 扣减时机         | 用户领取优惠券时                          |
| 是否设置过期时间 | 建议设置到领取结束时间之后 1 到 3 天      |
| 是否作为最终库存 | 否，最终业务事实仍以 MySQL 用户券记录为准 |

库存预热逻辑：

```text
后台创建模板
-> 模板状态为草稿
-> 后台启用模板
-> 查询模板 available_stock
-> 写入 Redis 库存 Key
-> 设置过期时间
-> 用户开始领取
```

Redis 库存和 MySQL 库存的关系：

```text
Redis 库存：
用于高并发领取时快速扣减，承担入口限流和防超发作用。

MySQL 库存：
用于后台展示、数据对账、最终核验和补偿修正。
```

在核心实现中，领取成功后建议同步扣减 MySQL 的 `available_stock`。即使 Redis 已经完成扣减，也不要完全放弃数据库库存字段，否则后台运营无法直观看到剩余库存，也不利于后续对账。

### 用户领取限制 Key

为了限制同一用户对同一个优惠券模板只能领取一次，需要为每个用户维护领取标记。

Key 设计如下：

```text
coupon:template:user:{templateId}
```

示例：

```text
coupon:template:user:10001
```

Redis 数据结构建议使用 Set：

```text
Key：coupon:template:user:10001
Type：Set
Member：userId
```

示例：

```text
SADD coupon:template:user:10001 20001
SISMEMBER coupon:template:user:10001 20001
```

领取限制 Key 使用建议：

| 项目             | 说明                                               |
| ---------------- | -------------------------------------------------- |
| Key              | `coupon:template:user:{templateId}`                |
| 类型             | Set                                                |
| Member           | 用户 ID                                            |
| 写入时机         | 用户成功领取时                                     |
| 校验时机         | 用户领取前                                         |
| 是否设置过期时间 | 建议设置到使用结束时间之后 7 到 30 天              |
| 是否作为最终防重 | 否，MySQL 唯一索引 `uk_template_user` 才是最终防线 |

为什么 Redis 和 MySQL 都要做防重：

```text
Redis 防重：
用于高并发入口快速拦截，降低数据库压力。

MySQL 唯一索引防重：
用于兜底，防止 Redis 数据丢失、脚本异常、并发落库等情况导致重复领取。
```

推荐 Redis Key 常量：

```text
优惠券库存：
coupon:template:stock:{templateId}

用户领取集合：
coupon:template:user:{templateId}

领取锁：
coupon:receive:lock:{templateId}:{userId}

模板缓存：
coupon:template:cache:{templateId}
```

### 原子领取 Lua 脚本

领取优惠券时，库存判断、重复领取判断、库存扣减、写入领取标记必须在 Redis 中原子完成。如果拆成多条 Redis 命令，在高并发场景下会出现并发穿透或重复领取风险。

该脚本放在 `src/main/resources/lua/coupon_receive.lua` 中。

```lua
-- 优惠券领取 Lua 脚本
-- KEYS[1]：库存 Key，例如 coupon:template:stock:10001
-- KEYS[2]：用户领取 Set Key，例如 coupon:template:user:10001
-- ARGV[1]：用户 ID
-- ARGV[2]：领取标记过期秒数
--
-- 返回值：
--  1：领取成功
-- -1：库存不存在
-- -2：库存不足
-- -3：用户已领取

local stockKey = KEYS[1]
local userSetKey = KEYS[2]
local userId = ARGV[1]
local expireSeconds = tonumber(ARGV[2])

local stock = redis.call('GET', stockKey)
if not stock then
    return -1
end

stock = tonumber(stock)
if stock <= 0 then
    return -2
end

local received = redis.call('SISMEMBER', userSetKey, userId)
if received == 1 then
    return -3
end

redis.call('DECR', stockKey)
redis.call('SADD', userSetKey, userId)

if expireSeconds and expireSeconds > 0 then
    redis.call('EXPIRE', userSetKey, expireSeconds)
end

return 1
```

脚本返回值建议统一封装为业务错误：

| 返回值 | 含义            | 接口提示                 |
| ------ | --------------- | ------------------------ |
| `1`    | 领取成功        | 领取成功                 |
| `-1`   | 库存 Key 不存在 | 活动未开始或库存未初始化 |
| `-2`   | 库存不足        | 优惠券已领完             |
| `-3`   | 用户已领取      | 请勿重复领取             |

Java 中加载 Lua 脚本时，推荐使用 `DefaultRedisScript<Long>`。后续代码实现中会在 `CouponReceiveService` 或 `UserCouponServiceImpl` 中调用该脚本。

调用逻辑示意：

```text
1. 查询优惠券模板。
2. 校验模板状态、领取时间。
3. 组装库存 Key 和用户领取 Set Key。
4. 执行 Lua 脚本。
5. Lua 返回成功后，写入 user_coupon。
6. 写入 coupon_flow 领取流水。
7. 同步扣减 MySQL coupon_template.available_stock。
```

注意：Lua 执行成功但 MySQL 落库失败时，需要做 Redis 回补。核心处理方式有两种：

```text
方案一：同步回补
MySQL 插入失败时，立即 INCR 库存 Key，并从用户领取 Set 中移除 userId。

方案二：异步补偿
记录失败日志，由定时任务扫描 Redis 与 MySQL 差异后修复。
```

本案例后续核心代码优先使用“同步回补”，实现简单，适合单体服务或中小型营销活动。

## 核心接口设计

本案例接口按“模板管理”和“用户券业务”划分。模板接口通常由运营后台调用，领取、锁定、核销、回退接口由用户端、订单系统、支付系统或任务系统调用。

接口统一返回结构建议如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

错误返回示例：

```json
{
  "code": 400,
  "message": "优惠券已领完",
  "data": null
}
```

### 创建优惠券模板接口

该接口用于后台创建优惠券模板。创建后默认是草稿状态，不能直接领取。运营确认无误后再调用启用逻辑，将库存预热到 Redis。

接口定义：

| 项目     | 内容                    |
| -------- | ----------------------- |
| 请求方式 | `POST`                  |
| 接口路径 | `/api/coupon/templates` |
| 调用方   | 运营后台                |
| 是否登录 | 是                      |
| 核心动作 | 创建优惠券模板          |
| 默认状态 | 草稿                    |

请求参数：

```json
{
  "templateName": "新人满100减20优惠券",
  "couponType": 1,
  "discountAmount": 20.00,
  "thresholdAmount": 100.00,
  "totalStock": 5000,
  "receiveLimit": 1,
  "receiveStartTime": "2026-06-01 00:00:00",
  "receiveEndTime": "2026-06-30 23:59:59",
  "useStartTime": "2026-06-01 00:00:00",
  "useEndTime": "2026-07-07 23:59:59",
  "remark": "新人活动优惠券"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "templateId": 10001
  }
}
```

核心校验：

```text
1. 优惠券名称不能为空。
2. 优惠金额必须大于 0。
3. 使用门槛金额不能小于优惠金额。
4. 总库存必须大于 0。
5. 领取开始时间不能晚于领取结束时间。
6. 使用开始时间不能晚于使用结束时间。
7. 使用结束时间不能早于领取结束时间。
```

curl 示例：

```bash
curl -X POST 'http://localhost:8080/api/coupon/templates' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer test-token' \
  -d '{
    "templateName": "新人满100减20优惠券",
    "couponType": 1,
    "discountAmount": 20.00,
    "thresholdAmount": 100.00,
    "totalStock": 5000,
    "receiveLimit": 1,
    "receiveStartTime": "2026-06-01 00:00:00",
    "receiveEndTime": "2026-06-30 23:59:59",
    "useStartTime": "2026-06-01 00:00:00",
    "useEndTime": "2026-07-07 23:59:59",
    "remark": "新人活动优惠券"
  }'
```

### 用户领取优惠券接口

该接口用于用户主动领取优惠券。接口内部先校验模板状态和领取时间，再执行 Redis Lua 脚本扣减库存，最后创建用户优惠券记录。

接口定义：

| 项目     | 内容                         |
| -------- | ---------------------------- |
| 请求方式 | `POST`                       |
| 接口路径 | `/api/user-coupons/receive`  |
| 调用方   | 用户端                       |
| 是否登录 | 是                           |
| 核心动作 | 领取优惠券                   |
| 关键保障 | Lua 原子扣减库存、防重复领取 |

请求参数：

```json
{
  "templateId": 10001
}
```

响应示例：

```json
{
  "code": 200,
  "message": "领取成功",
  "data": {
    "userCouponId": 90001,
    "templateId": 10001,
    "couponName": "新人满100减20优惠券",
    "discountAmount": 20.00,
    "thresholdAmount": 100.00,
    "useStartTime": "2026-06-01 00:00:00",
    "useEndTime": "2026-07-07 23:59:59"
  }
}
```

失败响应示例：

```json
{
  "code": 400,
  "message": "请勿重复领取",
  "data": null
}
```

核心校验：

```text
1. 用户必须登录。
2. 优惠券模板必须存在。
3. 模板状态必须为启用。
4. 当前时间必须在领取时间范围内。
5. 当前用户不能重复领取。
6. Redis 库存必须大于 0。
```

curl 示例：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/receive' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer test-token' \
  -d '{
    "templateId": 10001
  }'
```

接口内部流程：

```text
receive(templateId)
-> getLoginUserId()
-> query coupon_template
-> validate template status and receive time
-> execute coupon_receive.lua
-> insert user_coupon
-> insert coupon_flow
-> update coupon_template.available_stock = available_stock - 1
-> return userCouponId
```

### 下单锁定优惠券接口

该接口通常由订单系统在创建订单或提交订单时调用，用于校验优惠券是否可用，并将用户券状态从“未使用”更新为“已锁定”。

接口定义：

| 项目     | 内容                     |
| -------- | ------------------------ |
| 请求方式 | `POST`                   |
| 接口路径 | `/api/user-coupons/lock` |
| 调用方   | 订单系统 / 用户下单流程  |
| 是否登录 | 是                       |
| 核心动作 | 锁定用户优惠券           |
| 关键保障 | 条件更新、状态机控制     |

请求参数：

```json
{
  "userCouponId": 90001,
  "orderNo": "ORDER202606010001",
  "orderAmount": 128.50
}
```

响应示例：

```json
{
  "code": 200,
  "message": "锁定成功",
  "data": {
    "userCouponId": 90001,
    "orderNo": "ORDER202606010001",
    "discountAmount": 20.00,
    "payAmount": 108.50
  }
}
```

核心校验：

```text
1. 用户券必须存在。
2. 用户券必须属于当前登录用户。
3. 用户券状态必须为未使用。
4. 当前时间必须在使用有效期内。
5. 订单金额必须满足优惠券使用门槛。
6. 用户券不能已经绑定其他订单。
```

curl 示例：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/lock' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer test-token' \
  -d '{
    "userCouponId": 90001,
    "orderNo": "ORDER202606010001",
    "orderAmount": 128.50
  }'
```

接口内部流程：

```text
lock(userCouponId, orderNo, orderAmount)
-> getLoginUserId()
-> query user_coupon
-> validate ownership
-> validate status = 未使用
-> validate use time
-> validate threshold amount
-> update user_coupon set status = 已锁定, order_no = ?, lock_time = now()
   where id = ? and status = 未使用
-> insert coupon_flow
-> return discountAmount and payAmount
```

锁定接口要点：

```text
1. 不要先查后无条件更新。
2. 更新 SQL 必须带 status = 未使用。
3. 如果更新影响行数为 0，说明券状态已变化，需要返回锁定失败。
4. 订单取消前，该券不能再被其他订单使用。
```

### 支付成功核销接口

该接口通常由支付系统在支付成功后调用。由于支付回调可能重复通知，因此核销接口必须天然幂等。

接口定义：

| 项目     | 内容                           |
| -------- | ------------------------------ |
| 请求方式 | `POST`                         |
| 接口路径 | `/api/user-coupons/write-off`  |
| 调用方   | 支付系统 / 订单系统            |
| 是否登录 | 内部接口可使用服务鉴权         |
| 核心动作 | 核销已锁定优惠券               |
| 关键保障 | 幂等 Key、条件更新、状态不可逆 |

请求参数：

```json
{
  "orderNo": "ORDER202606010001",
  "payNo": "PAY202606010001",
  "payTime": "2026-06-01 10:30:00"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "核销成功",
  "data": true
}
```

重复核销响应示例：

```json
{
  "code": 200,
  "message": "优惠券已核销",
  "data": true
}
```

核心校验：

```text
1. orderNo 不能为空。
2. payNo 不能为空。
3. 根据 orderNo 查询用户券。
4. 如果未使用优惠券，直接返回成功。
5. 如果优惠券已核销，直接返回成功。
6. 如果优惠券不是已锁定状态，拒绝核销。
7. 如果当前时间超过使用结束时间，进入异常补偿或拒绝核销。
```

curl 示例：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/write-off' \
  -H 'Content-Type: application/json' \
  -H 'X-Internal-Token: test-internal-token' \
  -d '{
    "orderNo": "ORDER202606010001",
    "payNo": "PAY202606010001",
    "payTime": "2026-06-01 10:30:00"
  }'
```

接口内部流程：

```text
writeOff(orderNo, payNo)
-> build idemKey = COUPON_WRITE_OFF:{orderNo}
-> create or check idempotent_record
-> query user_coupon by orderNo
-> if not found return success
-> if status = 已核销 return success
-> if status != 已锁定 throw error
-> update user_coupon set status = 已核销, use_time = now()
   where order_no = ? and status = 已锁定
-> insert coupon_flow
-> update idempotent_record status = 成功
-> return success
```

核销接口要点：

```text
1. 已核销状态不可逆。
2. 重复核销要返回成功，不能返回异常。
3. 核销更新必须带 status = 已锁定。
4. 幂等记录和业务更新建议放在同一个本地事务中。
```

### 订单取消回退接口

该接口用于订单取消、支付超时、用户主动关闭订单等场景。只有处于“已锁定”的优惠券才允许回退。

接口定义：

| 项目     | 内容                         |
| -------- | ---------------------------- |
| 请求方式 | `POST`                       |
| 接口路径 | `/api/user-coupons/rollback` |
| 调用方   | 订单系统                     |
| 是否登录 | 内部接口可使用服务鉴权       |
| 核心动作 | 回退已锁定优惠券             |
| 关键保障 | 幂等 Key、状态判断、过期判断 |

请求参数：

```json
{
  "orderNo": "ORDER202606010001",
  "reason": "订单支付超时自动关闭"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "回退成功",
  "data": true
}
```

重复回退响应示例：

```json
{
  "code": 200,
  "message": "优惠券已回退",
  "data": true
}
```

核心校验：

```text
1. orderNo 不能为空。
2. 根据 orderNo 查询用户券。
3. 如果订单未使用优惠券，直接返回成功。
4. 如果优惠券状态为未使用，说明已经回退过，直接返回成功。
5. 如果优惠券状态为已核销，禁止回退。
6. 如果优惠券状态为已锁定，需要判断是否过期。
7. 未过期回退为未使用，已过期置为已过期。
```

curl 示例：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/rollback' \
  -H 'Content-Type: application/json' \
  -H 'X-Internal-Token: test-internal-token' \
  -d '{
    "orderNo": "ORDER202606010001",
    "reason": "订单支付超时自动关闭"
  }'
```

接口内部流程：

```text
rollback(orderNo, reason)
-> build idemKey = COUPON_ROLLBACK:{orderNo}
-> create or check idempotent_record
-> query user_coupon by orderNo
-> if not found return success
-> if status = 未使用 return success
-> if status = 已核销 throw error
-> if status != 已锁定 throw error
-> if now <= use_end_time
      update status = 未使用, order_no = null, lock_time = null
   else
      update status = 已过期, order_no = null, lock_time = null
-> insert coupon_flow
-> update idempotent_record status = 成功
-> return success
```

回退接口要点：

```text
1. 只能回退已锁定的优惠券。
2. 已核销优惠券不能回退。
3. 已过期优惠券不能恢复为未使用。
4. 重复回退应该返回成功。
5. 回退时要清空 order_no 和 lock_time，避免后续查询误判。
```

核心接口汇总如下：

| 接口     | 方法 | 路径                          | 主要职责             |
| -------- | ---- | ----------------------------- | -------------------- |
| 创建模板 | POST | `/api/coupon/templates`       | 后台创建优惠券模板   |
| 用户领取 | POST | `/api/user-coupons/receive`   | 用户领取优惠券       |
| 下单锁定 | POST | `/api/user-coupons/lock`      | 下单时锁定用户券     |
| 支付核销 | POST | `/api/user-coupons/write-off` | 支付成功后核销用户券 |
| 取消回退 | POST | `/api/user-coupons/rollback`  | 订单取消后回退用户券 |

## 核心代码实现

本节给出优惠券核心链路的可运行骨架代码，覆盖枚举、常量、实体、Mapper、DTO、Service、Controller。实现重点对应 README 中“领取资格校验、重复领取控制、库存并发扣减、权益锁定、核销幂等、取消回滚”的核心难点。

### 枚举与常量定义

优惠券模板状态枚举，用于控制模板是否允许领取。

文件位置：`src/main/java/io/github/atengk/coupon/enums/CouponTemplateStatusEnum.java`

```java
package io.github.atengk.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券模板状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum CouponTemplateStatusEnum {

    DRAFT(0, "草稿"),
    ENABLED(1, "启用"),
    DISABLED(2, "停用"),
    FINISHED(3, "结束");

    private final Integer code;
    private final String desc;
}
```

用户优惠券状态枚举，用于控制用户券的锁定、核销、回退状态流转。

文件位置：`src/main/java/io/github/atengk/coupon/enums/UserCouponStatusEnum.java`

```java
package io.github.atengk.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户优惠券状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum UserCouponStatusEnum {

    UNUSED(1, "未使用"),
    LOCKED(2, "已锁定"),
    USED(3, "已核销"),
    EXPIRED(4, "已过期");

    private final Integer code;
    private final String desc;
}
```

优惠券流水类型枚举，用于记录领取、锁定、核销、回退、过期等关键操作。

文件位置：`src/main/java/io/github/atengk/coupon/enums/CouponFlowTypeEnum.java`

```java
package io.github.atengk.coupon.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券流水类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum CouponFlowTypeEnum {

    RECEIVE(1, "领取"),
    LOCK(2, "锁定"),
    WRITE_OFF(3, "核销"),
    ROLLBACK(4, "回退"),
    EXPIRE(5, "过期");

    private final Integer code;
    private final String desc;
}
```

Redis Key 常量集中管理，避免业务代码中散落硬编码字符串。

文件位置：`src/main/java/io/github/atengk/coupon/common/RedisKeyConstants.java`

```java
package io.github.atengk.coupon.common;

/**
 * Redis Key 常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class RedisKeyConstants {

    public static final String COUPON_TEMPLATE_STOCK = "coupon:template:stock:%s";

    public static final String COUPON_TEMPLATE_USER = "coupon:template:user:%s";

    private RedisKeyConstants() {
    }

    public static String couponTemplateStock(Long templateId) {
        return String.format(COUPON_TEMPLATE_STOCK, templateId);
    }

    public static String couponTemplateUser(Long templateId) {
        return String.format(COUPON_TEMPLATE_USER, templateId);
    }
}
```

业务异常和统一返回对象用于简化 Controller 返回。

文件位置：`src/main/java/io/github/atengk/coupon/common/BizException.java`

```java
package io.github.atengk.coupon.common;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
```

文件位置：`src/main/java/io/github/atengk/coupon/common/Result.java`

```java
package io.github.atengk.coupon.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回对象
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

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(200, message, data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(400, message, null);
    }
}
```

### Entity 实体类

优惠券模板实体，对应 `coupon_template` 表。

文件位置：`src/main/java/io/github/atengk/coupon/entity/CouponTemplate.java`

```java
package io.github.atengk.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String templateName;

    private Integer couponType;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal thresholdAmount;

    private Integer totalStock;

    private Integer availableStock;

    private Integer receiveLimit;

    private LocalDateTime receiveStartTime;

    private LocalDateTime receiveEndTime;

    private LocalDateTime useStartTime;

    private LocalDateTime useEndTime;

    private Integer status;

    private String remark;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

用户优惠券实体，对应 `user_coupon` 表。

文件位置：`src/main/java/io/github/atengk/coupon/entity/UserCoupon.java`

```java
package io.github.atengk.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("user_coupon")
public class UserCoupon {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long templateId;

    private Long userId;

    private String couponName;

    private BigDecimal discountAmount;

    private BigDecimal thresholdAmount;

    private LocalDateTime useStartTime;

    private LocalDateTime useEndTime;

    private Integer status;

    private String orderNo;

    private LocalDateTime lockTime;

    private LocalDateTime useTime;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

优惠券流水实体，对应 `coupon_flow` 表。

文件位置：`src/main/java/io/github/atengk/coupon/entity/CouponFlow.java`

```java
package io.github.atengk.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券流水实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("coupon_flow")
public class CouponFlow {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userCouponId;

    private Long templateId;

    private Long userId;

    private String orderNo;

    private Integer flowType;

    private Integer beforeStatus;

    private Integer afterStatus;

    private String bizNo;

    private String remark;

    private LocalDateTime createTime;
}
```

幂等记录实体，对应 `idempotent_record` 表，后续核销和回退可扩展使用。

文件位置：`src/main/java/io/github/atengk/coupon/entity/IdempotentRecord.java`

```java
package io.github.atengk.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 幂等记录实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("idempotent_record")
public class IdempotentRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String idemKey;

    private String bizType;

    private String bizNo;

    private Integer status;

    private String requestParams;

    private String responseResult;

    private String errorMsg;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

### Mapper 数据访问层

Mapper 使用 MyBatis-Plus，普通 CRUD 直接继承 `BaseMapper`，核心状态变更使用自定义 `@Update`，保证更新语句带前置状态条件。

文件位置：`src/main/java/io/github/atengk/coupon/mapper/CouponTemplateMapper.java`

```java
package io.github.atengk.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.coupon.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券模板 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    @Update("""
            UPDATE coupon_template
            SET available_stock = available_stock - 1,
                update_time = NOW()
            WHERE id = #{templateId}
              AND available_stock > 0
            """)
    int deductStock(@Param("templateId") Long templateId);

    @Update("""
            UPDATE coupon_template
            SET available_stock = available_stock + 1,
                update_time = NOW()
            WHERE id = #{templateId}
            """)
    int rollbackStock(@Param("templateId") Long templateId);
}
```

用户优惠券 Mapper 承载锁定、核销、回退等状态机更新。

文件位置：`src/main/java/io/github/atengk/coupon/mapper/UserCouponMapper.java`

```java
package io.github.atengk.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 用户优惠券 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    @Update("""
            UPDATE user_coupon
            SET status = #{targetStatus},
                order_no = #{orderNo},
                lock_time = #{lockTime},
                update_time = NOW()
            WHERE id = #{userCouponId}
              AND user_id = #{userId}
              AND status = #{sourceStatus}
              AND use_start_time <= #{nowTime}
              AND use_end_time >= #{nowTime}
            """)
    int lockCoupon(@Param("userCouponId") Long userCouponId,
                   @Param("userId") Long userId,
                   @Param("orderNo") String orderNo,
                   @Param("sourceStatus") Integer sourceStatus,
                   @Param("targetStatus") Integer targetStatus,
                   @Param("lockTime") LocalDateTime lockTime,
                   @Param("nowTime") LocalDateTime nowTime);

    @Update("""
            UPDATE user_coupon
            SET status = #{targetStatus},
                use_time = #{useTime},
                update_time = NOW()
            WHERE order_no = #{orderNo}
              AND status = #{sourceStatus}
            """)
    int writeOffCoupon(@Param("orderNo") String orderNo,
                       @Param("sourceStatus") Integer sourceStatus,
                       @Param("targetStatus") Integer targetStatus,
                       @Param("useTime") LocalDateTime useTime);

    @Update("""
            UPDATE user_coupon
            SET status = #{targetStatus},
                order_no = NULL,
                lock_time = NULL,
                update_time = NOW()
            WHERE order_no = #{orderNo}
              AND status = #{sourceStatus}
            """)
    int rollbackCoupon(@Param("orderNo") String orderNo,
                       @Param("sourceStatus") Integer sourceStatus,
                       @Param("targetStatus") Integer targetStatus);
}
```

流水和幂等记录 Mapper 使用标准 MyBatis-Plus CRUD。

文件位置：`src/main/java/io/github/atengk/coupon/mapper/CouponFlowMapper.java`

```java
package io.github.atengk.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.coupon.entity.CouponFlow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券流水 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface CouponFlowMapper extends BaseMapper<CouponFlow> {
}
```

文件位置：`src/main/java/io/github/atengk/coupon/mapper/IdempotentRecordMapper.java`

```java
package io.github.atengk.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.coupon.entity.IdempotentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 幂等记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface IdempotentRecordMapper extends BaseMapper<IdempotentRecord> {
}
```

### DTO 与请求参数

创建优惠券模板请求 DTO，负责接收后台创建模板参数。

文件位置：`src/main/java/io/github/atengk/coupon/dto/CouponTemplateCreateDTO.java`

```java
package io.github.atengk.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建优惠券模板请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CouponTemplateCreateDTO {

    @NotBlank(message = "优惠券名称不能为空")
    private String templateName;

    @NotNull(message = "优惠券类型不能为空")
    private Integer couponType;

    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.01", message = "优惠金额必须大于0")
    private BigDecimal discountAmount;

    @NotNull(message = "使用门槛不能为空")
    @DecimalMin(value = "0.00", message = "使用门槛不能小于0")
    private BigDecimal thresholdAmount;

    @NotNull(message = "总库存不能为空")
    @Min(value = 1, message = "总库存必须大于0")
    private Integer totalStock;

    @NotNull(message = "每人领取限制不能为空")
    @Min(value = 1, message = "每人领取限制必须大于0")
    private Integer receiveLimit;

    @NotNull(message = "领取开始时间不能为空")
    private LocalDateTime receiveStartTime;

    @NotNull(message = "领取结束时间不能为空")
    private LocalDateTime receiveEndTime;

    @NotNull(message = "使用开始时间不能为空")
    private LocalDateTime useStartTime;

    @NotNull(message = "使用结束时间不能为空")
    private LocalDateTime useEndTime;

    private String remark;
}
```

领取、锁定、核销、回退请求 DTO。

文件位置：`src/main/java/io/github/atengk/coupon/dto/CouponReceiveDTO.java`

```java
package io.github.atengk.coupon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 领取优惠券请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CouponReceiveDTO {

    @NotNull(message = "优惠券模板ID不能为空")
    private Long templateId;
}
```

文件位置：`src/main/java/io/github/atengk/coupon/dto/CouponLockDTO.java`

```java
package io.github.atengk.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 锁定优惠券请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CouponLockDTO {

    @NotNull(message = "用户优惠券ID不能为空")
    private Long userCouponId;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal orderAmount;
}
```

文件位置：`src/main/java/io/github/atengk/coupon/dto/CouponWriteOffDTO.java`

```java
package io.github.atengk.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 核销优惠券请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CouponWriteOffDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "支付单号不能为空")
    private String payNo;

    private LocalDateTime payTime;
}
```

文件位置：`src/main/java/io/github/atengk/coupon/dto/CouponRollbackDTO.java`

```java
package io.github.atengk.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 回退优惠券请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CouponRollbackDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    private String reason;
}
```

锁定返回 VO，用于告诉订单系统优惠金额和应付金额。

文件位置：`src/main/java/io/github/atengk/coupon/vo/CouponLockVO.java`

```java
package io.github.atengk.coupon.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 优惠券锁定结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class CouponLockVO {

    private Long userCouponId;

    private String orderNo;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;
}
```

### Service 业务实现

模板 Service 负责创建模板和启用模板。启用时将库存预热到 Redis。

文件位置：`src/main/java/io/github/atengk/coupon/service/CouponTemplateService.java`

```java
package io.github.atengk.coupon.service;

import io.github.atengk.coupon.dto.CouponTemplateCreateDTO;

/**
 * 优惠券模板 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface CouponTemplateService {

    Long createTemplate(CouponTemplateCreateDTO dto);

    Boolean enableTemplate(Long templateId);
}
```

文件位置：`src/main/java/io/github/atengk/coupon/service/impl/CouponTemplateServiceImpl.java`

```java
package io.github.atengk.coupon.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.coupon.common.BizException;
import io.github.atengk.coupon.common.RedisKeyConstants;
import io.github.atengk.coupon.dto.CouponTemplateCreateDTO;
import io.github.atengk.coupon.entity.CouponTemplate;
import io.github.atengk.coupon.enums.CouponTemplateStatusEnum;
import io.github.atengk.coupon.mapper.CouponTemplateMapper;
import io.github.atengk.coupon.service.CouponTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 优惠券模板 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(CouponTemplateCreateDTO dto) {
        validateCreateTemplate(dto);

        LocalDateTime now = LocalDateTime.now();
        CouponTemplate template = new CouponTemplate();
        template.setTemplateName(dto.getTemplateName());
        template.setCouponType(dto.getCouponType());
        template.setDiscountAmount(dto.getDiscountAmount());
        template.setThresholdAmount(dto.getThresholdAmount());
        template.setTotalStock(dto.getTotalStock());
        template.setAvailableStock(dto.getTotalStock());
        template.setReceiveLimit(dto.getReceiveLimit());
        template.setReceiveStartTime(dto.getReceiveStartTime());
        template.setReceiveEndTime(dto.getReceiveEndTime());
        template.setUseStartTime(dto.getUseStartTime());
        template.setUseEndTime(dto.getUseEndTime());
        template.setStatus(CouponTemplateStatusEnum.DRAFT.getCode());
        template.setRemark(dto.getRemark());
        template.setVersion(0);
        template.setCreateTime(now);
        template.setUpdateTime(now);

        couponTemplateMapper.insert(template);
        log.info("创建优惠券模板成功，templateId={}", template.getId());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableTemplate(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (ObjectUtil.isNull(template)) {
            throw new BizException("优惠券模板不存在");
        }
        if (!CouponTemplateStatusEnum.DRAFT.getCode().equals(template.getStatus())
                && !CouponTemplateStatusEnum.DISABLED.getCode().equals(template.getStatus())) {
            throw new BizException("当前模板状态不允许启用");
        }

        template.setStatus(CouponTemplateStatusEnum.ENABLED.getCode());
        template.setUpdateTime(LocalDateTime.now());
        couponTemplateMapper.updateById(template);

        String stockKey = RedisKeyConstants.couponTemplateStock(templateId);
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(template.getAvailableStock()));

        long expireSeconds = Duration.between(LocalDateTime.now(), template.getReceiveEndTime().plusDays(3)).getSeconds();
        if (expireSeconds > 0) {
            stringRedisTemplate.expire(stockKey, Duration.ofSeconds(expireSeconds));
        }

        log.info("启用优惠券模板并预热库存成功，templateId={}, stock={}", templateId, template.getAvailableStock());
        return true;
    }

    private void validateCreateTemplate(CouponTemplateCreateDTO dto) {
        if (dto.getThresholdAmount().compareTo(dto.getDiscountAmount()) < 0) {
            throw new BizException("使用门槛不能小于优惠金额");
        }
        if (LocalDateTimeUtil.isIn(dto.getReceiveEndTime(), dto.getReceiveStartTime(), dto.getReceiveStartTime())) {
            throw new BizException("领取结束时间必须晚于领取开始时间");
        }
        if (!dto.getReceiveEndTime().isAfter(dto.getReceiveStartTime())) {
            throw new BizException("领取结束时间必须晚于领取开始时间");
        }
        if (!dto.getUseEndTime().isAfter(dto.getUseStartTime())) {
            throw new BizException("使用结束时间必须晚于使用开始时间");
        }
        if (dto.getUseEndTime().isBefore(dto.getReceiveEndTime())) {
            throw new BizException("使用结束时间不能早于领取结束时间");
        }
    }
}
```

用户优惠券 Service 负责领取、锁定、核销、回退。这里是核心实现。

文件位置：`src/main/java/io/github/atengk/coupon/service/UserCouponService.java`

```java
package io.github.atengk.coupon.service;

import io.github.atengk.coupon.dto.CouponLockDTO;
import io.github.atengk.coupon.dto.CouponReceiveDTO;
import io.github.atengk.coupon.dto.CouponRollbackDTO;
import io.github.atengk.coupon.dto.CouponWriteOffDTO;
import io.github.atengk.coupon.vo.CouponLockVO;

/**
 * 用户优惠券 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface UserCouponService {

    Long receive(CouponReceiveDTO dto);

    CouponLockVO lock(CouponLockDTO dto);

    Boolean writeOff(CouponWriteOffDTO dto);

    Boolean rollback(CouponRollbackDTO dto);
}
```

用户优惠券核心业务实现，包含 Lua 扣减、数据库状态机更新和流水记录。

文件位置：`src/main/java/io/github/atengk/coupon/service/impl/UserCouponServiceImpl.java`

```java
package io.github.atengk.coupon.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.coupon.common.BizException;
import io.github.atengk.coupon.common.RedisKeyConstants;
import io.github.atengk.coupon.dto.CouponLockDTO;
import io.github.atengk.coupon.dto.CouponReceiveDTO;
import io.github.atengk.coupon.dto.CouponRollbackDTO;
import io.github.atengk.coupon.dto.CouponWriteOffDTO;
import io.github.atengk.coupon.entity.CouponFlow;
import io.github.atengk.coupon.entity.CouponTemplate;
import io.github.atengk.coupon.entity.UserCoupon;
import io.github.atengk.coupon.enums.CouponFlowTypeEnum;
import io.github.atengk.coupon.enums.CouponTemplateStatusEnum;
import io.github.atengk.coupon.enums.UserCouponStatusEnum;
import io.github.atengk.coupon.mapper.CouponFlowMapper;
import io.github.atengk.coupon.mapper.CouponTemplateMapper;
import io.github.atengk.coupon.mapper.UserCouponMapper;
import io.github.atengk.coupon.service.UserCouponService;
import io.github.atengk.coupon.vo.CouponLockVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scripting.support.ResourceScriptSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户优惠券 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl implements UserCouponService {

    private final CouponTemplateMapper couponTemplateMapper;

    private final UserCouponMapper userCouponMapper;

    private final CouponFlowMapper couponFlowMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long receive(CouponReceiveDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        CouponTemplate template = couponTemplateMapper.selectById(dto.getTemplateId());
        validateReceiveTemplate(template);

        String stockKey = RedisKeyConstants.couponTemplateStock(dto.getTemplateId());
        String userSetKey = RedisKeyConstants.couponTemplateUser(dto.getTemplateId());

        long expireSeconds = Duration.between(LocalDateTime.now(), template.getUseEndTime().plusDays(7)).getSeconds();
        Long luaResult = executeReceiveLua(stockKey, userSetKey, userId, expireSeconds);
        handleReceiveLuaResult(luaResult);

        try {
            UserCoupon userCoupon = buildUserCoupon(template, userId);
            userCouponMapper.insert(userCoupon);

            int deductDbRows = couponTemplateMapper.deductStock(template.getId());
            if (deductDbRows <= 0) {
                throw new BizException("优惠券库存不足");
            }

            saveFlow(userCoupon, null, UserCouponStatusEnum.UNUSED.getCode(), CouponFlowTypeEnum.RECEIVE, null, "用户领取优惠券");
            log.info("用户领取优惠券成功，userId={}, templateId={}, userCouponId={}", userId, template.getId(), userCoupon.getId());
            return userCoupon.getId();
        } catch (Exception ex) {
            rollbackRedisReceive(stockKey, userSetKey, userId);
            log.error("用户领取优惠券落库失败，已回补 Redis，userId={}, templateId={}", userId, dto.getTemplateId(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponLockVO lock(CouponLockDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserCoupon userCoupon = userCouponMapper.selectById(dto.getUserCouponId());
        validateLockCoupon(userCoupon, userId, dto);

        LocalDateTime now = LocalDateTime.now();
        int rows = userCouponMapper.lockCoupon(
                dto.getUserCouponId(),
                userId,
                dto.getOrderNo(),
                UserCouponStatusEnum.UNUSED.getCode(),
                UserCouponStatusEnum.LOCKED.getCode(),
                now,
                now
        );
        if (rows <= 0) {
            throw new BizException("优惠券状态已变化，请刷新后重试");
        }

        userCoupon.setStatus(UserCouponStatusEnum.LOCKED.getCode());
        userCoupon.setOrderNo(dto.getOrderNo());
        saveFlow(userCoupon, UserCouponStatusEnum.UNUSED.getCode(), UserCouponStatusEnum.LOCKED.getCode(),
                CouponFlowTypeEnum.LOCK, dto.getOrderNo(), "下单锁定优惠券");

        BigDecimal payAmount = dto.getOrderAmount().subtract(userCoupon.getDiscountAmount());
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }

        log.info("锁定优惠券成功，userId={}, userCouponId={}, orderNo={}", userId, dto.getUserCouponId(), dto.getOrderNo());
        return CouponLockVO.builder()
                .userCouponId(dto.getUserCouponId())
                .orderNo(dto.getOrderNo())
                .discountAmount(userCoupon.getDiscountAmount())
                .payAmount(payAmount)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean writeOff(CouponWriteOffDTO dto) {
        UserCoupon userCoupon = lambdaQuery()
                .eq(UserCoupon::getOrderNo, dto.getOrderNo())
                .one();

        if (ObjectUtil.isNull(userCoupon)) {
            log.info("订单未使用优惠券，无需核销，orderNo={}", dto.getOrderNo());
            return true;
        }

        if (UserCouponStatusEnum.USED.getCode().equals(userCoupon.getStatus())) {
            log.info("优惠券已核销，重复通知直接返回成功，orderNo={}", dto.getOrderNo());
            return true;
        }

        if (!UserCouponStatusEnum.LOCKED.getCode().equals(userCoupon.getStatus())) {
            throw new BizException("当前优惠券状态不允许核销");
        }

        LocalDateTime useTime = ObjectUtil.defaultIfNull(dto.getPayTime(), LocalDateTime.now());
        int rows = userCouponMapper.writeOffCoupon(
                dto.getOrderNo(),
                UserCouponStatusEnum.LOCKED.getCode(),
                UserCouponStatusEnum.USED.getCode(),
                useTime
        );
        if (rows <= 0) {
            throw new BizException("优惠券核销失败，请稍后重试");
        }

        userCoupon.setStatus(UserCouponStatusEnum.USED.getCode());
        saveFlow(userCoupon, UserCouponStatusEnum.LOCKED.getCode(), UserCouponStatusEnum.USED.getCode(),
                CouponFlowTypeEnum.WRITE_OFF, dto.getPayNo(), "支付成功核销优惠券");

        log.info("优惠券核销成功，orderNo={}, payNo={}", dto.getOrderNo(), dto.getPayNo());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean rollback(CouponRollbackDTO dto) {
        UserCoupon userCoupon = lambdaQuery()
                .eq(UserCoupon::getOrderNo, dto.getOrderNo())
                .one();

        if (ObjectUtil.isNull(userCoupon)) {
            log.info("订单未绑定优惠券，无需回退，orderNo={}", dto.getOrderNo());
            return true;
        }

        if (UserCouponStatusEnum.UNUSED.getCode().equals(userCoupon.getStatus())) {
            log.info("优惠券已回退，重复通知直接返回成功，orderNo={}", dto.getOrderNo());
            return true;
        }

        if (UserCouponStatusEnum.USED.getCode().equals(userCoupon.getStatus())) {
            throw new BizException("优惠券已核销，不能回退");
        }

        if (!UserCouponStatusEnum.LOCKED.getCode().equals(userCoupon.getStatus())) {
            throw new BizException("当前优惠券状态不允许回退");
        }

        Integer targetStatus = LocalDateTime.now().isAfter(userCoupon.getUseEndTime())
                ? UserCouponStatusEnum.EXPIRED.getCode()
                : UserCouponStatusEnum.UNUSED.getCode();

        int rows = userCouponMapper.rollbackCoupon(
                dto.getOrderNo(),
                UserCouponStatusEnum.LOCKED.getCode(),
                targetStatus
        );
        if (rows <= 0) {
            throw new BizException("优惠券回退失败，请稍后重试");
        }

        userCoupon.setStatus(targetStatus);
        saveFlow(userCoupon, UserCouponStatusEnum.LOCKED.getCode(), targetStatus,
                CouponFlowTypeEnum.ROLLBACK, dto.getOrderNo(), StrUtil.blankToDefault(dto.getReason(), "订单取消回退优惠券"));

        log.info("优惠券回退成功，orderNo={}, targetStatus={}", dto.getOrderNo(), targetStatus);
        return true;
    }

    private void validateReceiveTemplate(CouponTemplate template) {
        if (ObjectUtil.isNull(template)) {
            throw new BizException("优惠券模板不存在");
        }
        if (!CouponTemplateStatusEnum.ENABLED.getCode().equals(template.getStatus())) {
            throw new BizException("优惠券活动未启用");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(template.getReceiveStartTime())) {
            throw new BizException("优惠券领取活动未开始");
        }
        if (now.isAfter(template.getReceiveEndTime())) {
            throw new BizException("优惠券领取活动已结束");
        }
    }

    private Long executeReceiveLua(String stockKey, String userSetKey, Long userId, long expireSeconds) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/coupon_receive.lua")));

        return stringRedisTemplate.execute(
                redisScript,
                List.of(stockKey, userSetKey),
                String.valueOf(userId),
                String.valueOf(expireSeconds)
        );
    }

    private void handleReceiveLuaResult(Long luaResult) {
        if (ObjectUtil.isNull(luaResult)) {
            throw new BizException("领取失败，请稍后重试");
        }
        if (luaResult == -1) {
            throw new BizException("活动未开始或库存未初始化");
        }
        if (luaResult == -2) {
            throw new BizException("优惠券已领完");
        }
        if (luaResult == -3) {
            throw new BizException("请勿重复领取");
        }
        if (luaResult != 1) {
            throw new BizException("领取失败，请稍后重试");
        }
    }

    private UserCoupon buildUserCoupon(CouponTemplate template, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setTemplateId(template.getId());
        userCoupon.setUserId(userId);
        userCoupon.setCouponName(template.getTemplateName());
        userCoupon.setDiscountAmount(template.getDiscountAmount());
        userCoupon.setThresholdAmount(template.getThresholdAmount());
        userCoupon.setUseStartTime(template.getUseStartTime());
        userCoupon.setUseEndTime(template.getUseEndTime());
        userCoupon.setStatus(UserCouponStatusEnum.UNUSED.getCode());
        userCoupon.setVersion(0);
        userCoupon.setCreateTime(now);
        userCoupon.setUpdateTime(now);
        return userCoupon;
    }

    private void rollbackRedisReceive(String stockKey, String userSetKey, Long userId) {
        stringRedisTemplate.opsForValue().increment(stockKey);
        stringRedisTemplate.opsForSet().remove(userSetKey, String.valueOf(userId));
    }

    private void validateLockCoupon(UserCoupon userCoupon, Long userId, CouponLockDTO dto) {
        if (ObjectUtil.isNull(userCoupon)) {
            throw new BizException("用户优惠券不存在");
        }
        if (!userId.equals(userCoupon.getUserId())) {
            throw new BizException("不能使用他人的优惠券");
        }
        if (!UserCouponStatusEnum.UNUSED.getCode().equals(userCoupon.getStatus())) {
            throw new BizException("优惠券当前不可使用");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(userCoupon.getUseStartTime())) {
            throw new BizException("优惠券未到可用时间");
        }
        if (now.isAfter(userCoupon.getUseEndTime())) {
            throw new BizException("优惠券已过期");
        }
        if (dto.getOrderAmount().compareTo(userCoupon.getThresholdAmount()) < 0) {
            throw new BizException("订单金额未达到优惠券使用门槛");
        }
    }

    private void saveFlow(UserCoupon userCoupon,
                          Integer beforeStatus,
                          Integer afterStatus,
                          CouponFlowTypeEnum flowType,
                          String bizNo,
                          String remark) {
        CouponFlow flow = new CouponFlow();
        flow.setUserCouponId(userCoupon.getId());
        flow.setTemplateId(userCoupon.getTemplateId());
        flow.setUserId(userCoupon.getUserId());
        flow.setOrderNo(userCoupon.getOrderNo());
        flow.setFlowType(flowType.getCode());
        flow.setBeforeStatus(beforeStatus);
        flow.setAfterStatus(afterStatus);
        flow.setBizNo(bizNo);
        flow.setRemark(remark);
        flow.setCreateTime(LocalDateTime.now());
        couponFlowMapper.insert(flow);
    }

    private com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<UserCoupon> lambdaQuery() {
        return new com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<>(userCouponMapper);
    }
}
```

> 上面代码中 `lambdaQuery()` 是为了让示例在不继承 MyBatis-Plus `ServiceImpl` 的情况下仍能查询。实际项目中也可以让 `UserCouponServiceImpl` 继承 `ServiceImpl<UserCouponMapper, UserCoupon>`，再直接使用 `lambdaQuery()`。

### Controller 接口层

模板 Controller 提供创建和启用接口。

文件位置：`src/main/java/io/github/atengk/coupon/controller/CouponTemplateController.java`

```java
package io.github.atengk.coupon.controller;

import io.github.atengk.coupon.common.Result;
import io.github.atengk.coupon.dto.CouponTemplateCreateDTO;
import io.github.atengk.coupon.service.CouponTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 优惠券模板接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupon/templates")
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;

    @PostMapping
    public Result<Long> createTemplate(@Valid @RequestBody CouponTemplateCreateDTO dto) {
        return Result.ok("创建成功", couponTemplateService.createTemplate(dto));
    }

    @PostMapping("/{templateId}/enable")
    public Result<Boolean> enableTemplate(@PathVariable Long templateId) {
        return Result.ok("启用成功", couponTemplateService.enableTemplate(templateId));
    }
}
```

用户优惠券 Controller 提供领取、锁定、核销、回退接口。

文件位置：`src/main/java/io/github/atengk/coupon/controller/UserCouponController.java`

```java
package io.github.atengk.coupon.controller;

import io.github.atengk.coupon.common.Result;
import io.github.atengk.coupon.dto.CouponLockDTO;
import io.github.atengk.coupon.dto.CouponReceiveDTO;
import io.github.atengk.coupon.dto.CouponRollbackDTO;
import io.github.atengk.coupon.dto.CouponWriteOffDTO;
import io.github.atengk.coupon.service.UserCouponService;
import io.github.atengk.coupon.vo.CouponLockVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户优惠券接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-coupons")
public class UserCouponController {

    private final UserCouponService userCouponService;

    @PostMapping("/receive")
    public Result<Long> receive(@Valid @RequestBody CouponReceiveDTO dto) {
        return Result.ok("领取成功", userCouponService.receive(dto));
    }

    @PostMapping("/lock")
    public Result<CouponLockVO> lock(@Valid @RequestBody CouponLockDTO dto) {
        return Result.ok("锁定成功", userCouponService.lock(dto));
    }

    @PostMapping("/write-off")
    public Result<Boolean> writeOff(@Valid @RequestBody CouponWriteOffDTO dto) {
        return Result.ok("核销成功", userCouponService.writeOff(dto));
    }

    @PostMapping("/rollback")
    public Result<Boolean> rollback(@Valid @RequestBody CouponRollbackDTO dto) {
        return Result.ok("回退成功", userCouponService.rollback(dto));
    }
}
```

异常处理器用于将 `BizException` 转成统一错误响应。

文件位置：`src/main/java/io/github/atengk/coupon/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.coupon.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        log.warn("业务异常：{}", ex.getMessage());
        return Result.fail(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("参数校验失败");
        return Result.fail(message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
```

## 关键业务实现

### 领取资格校验

领取资格校验主要在 `validateReceiveTemplate()` 中完成。这里不直接扣库存，只校验模板是否具备领取条件。

核心校验点：

```text
1. 模板必须存在。
2. 模板状态必须是启用。
3. 当前时间必须在领取开始和领取结束之间。
4. 后续可扩展会员等级、用户标签、新老用户、渠道来源等规则。
```

代码位置：

```java
private void validateReceiveTemplate(CouponTemplate template) {
    if (ObjectUtil.isNull(template)) {
        throw new BizException("优惠券模板不存在");
    }
    if (!CouponTemplateStatusEnum.ENABLED.getCode().equals(template.getStatus())) {
        throw new BizException("优惠券活动未启用");
    }

    LocalDateTime now = LocalDateTime.now();
    if (now.isBefore(template.getReceiveStartTime())) {
        throw new BizException("优惠券领取活动未开始");
    }
    if (now.isAfter(template.getReceiveEndTime())) {
        throw new BizException("优惠券领取活动已结束");
    }
}
```

### 防重复领取

防重复领取使用两层保障。

第一层是 Redis Set：

```text
coupon:template:user:{templateId}
```

Lua 脚本中通过 `SISMEMBER` 判断用户是否领取过：

```lua
local received = redis.call('SISMEMBER', userSetKey, userId)
if received == 1 then
    return -3
end
```

第二层是 MySQL 唯一索引：

```sql
UNIQUE KEY uk_template_user (template_id, user_id)
```

两层都需要保留。Redis 负责高并发快速拦截，MySQL 唯一索引负责最终兜底。

### 库存并发扣减

库存并发扣减通过 Lua 原子脚本完成，避免多条 Redis 命令之间出现并发空隙。

核心逻辑：

```lua
local stock = redis.call('GET', stockKey)
if not stock then
    return -1
end

stock = tonumber(stock)
if stock <= 0 then
    return -2
end

redis.call('DECR', stockKey)
redis.call('SADD', userSetKey, userId)
```

领取成功后再扣减 MySQL 库存：

```java
int deductDbRows = couponTemplateMapper.deductStock(template.getId());
if (deductDbRows <= 0) {
    throw new BizException("优惠券库存不足");
}
```

如果 Redis 扣减成功但数据库落库失败，需要同步回补 Redis：

```java
private void rollbackRedisReceive(String stockKey, String userSetKey, Long userId) {
    stringRedisTemplate.opsForValue().increment(stockKey);
    stringRedisTemplate.opsForSet().remove(userSetKey, String.valueOf(userId));
}
```

### 下单使用门槛校验

锁定前必须校验订单金额是否满足优惠券门槛。

核心代码：

```java
if (dto.getOrderAmount().compareTo(userCoupon.getThresholdAmount()) < 0) {
    throw new BizException("订单金额未达到优惠券使用门槛");
}
```

金额比较必须使用 `BigDecimal.compareTo()`，不要使用 `equals()`。因为 `100.0` 和 `100.00` 使用 `equals()` 可能不相等，但金额语义上相等。

### 优惠券锁定

优惠券锁定必须是条件更新，不能先查询后无条件更新。

关键 SQL：

```sql
UPDATE user_coupon
SET status = #{targetStatus},
    order_no = #{orderNo},
    lock_time = #{lockTime},
    update_time = NOW()
WHERE id = #{userCouponId}
  AND user_id = #{userId}
  AND status = #{sourceStatus}
  AND use_start_time <= #{nowTime}
  AND use_end_time >= #{nowTime}
```

核心调用：

```java
int rows = userCouponMapper.lockCoupon(
        dto.getUserCouponId(),
        userId,
        dto.getOrderNo(),
        UserCouponStatusEnum.UNUSED.getCode(),
        UserCouponStatusEnum.LOCKED.getCode(),
        now,
        now
);
if (rows <= 0) {
    throw new BizException("优惠券状态已变化，请刷新后重试");
}
```

这可以保证同一张券在并发下只能被一个订单锁定成功。

### 支付成功核销

支付成功核销要支持重复通知。

核心判断：

```java
if (UserCouponStatusEnum.USED.getCode().equals(userCoupon.getStatus())) {
    log.info("优惠券已核销，重复通知直接返回成功，orderNo={}", dto.getOrderNo());
    return true;
}
```

核销更新必须带状态条件：

```sql
UPDATE user_coupon
SET status = #{targetStatus},
    use_time = #{useTime},
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND status = #{sourceStatus}
```

核心调用：

```java
int rows = userCouponMapper.writeOffCoupon(
        dto.getOrderNo(),
        UserCouponStatusEnum.LOCKED.getCode(),
        UserCouponStatusEnum.USED.getCode(),
        useTime
);
if (rows <= 0) {
    throw new BizException("优惠券核销失败，请稍后重试");
}
```

实际生产建议进一步接入 `idempotent_record`，用 `COUPON_WRITE_OFF:{orderNo}` 作为幂等 Key，避免支付系统高频重复回调造成无意义查询和日志噪音。

### 订单取消回退

订单取消回退只允许处理“已锁定”的优惠券。

核心判断：

```java
if (UserCouponStatusEnum.USED.getCode().equals(userCoupon.getStatus())) {
    throw new BizException("优惠券已核销，不能回退");
}

if (!UserCouponStatusEnum.LOCKED.getCode().equals(userCoupon.getStatus())) {
    throw new BizException("当前优惠券状态不允许回退");
}
```

回退时要判断是否过期：

```java
Integer targetStatus = LocalDateTime.now().isAfter(userCoupon.getUseEndTime())
        ? UserCouponStatusEnum.EXPIRED.getCode()
        : UserCouponStatusEnum.UNUSED.getCode();
```

回退更新 SQL：

```sql
UPDATE user_coupon
SET status = #{targetStatus},
    order_no = NULL,
    lock_time = NULL,
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND status = #{sourceStatus}
```

这能保证：

```text
1. 未支付取消：已锁定 -> 未使用。
2. 已过期取消：已锁定 -> 已过期。
3. 已核销订单：禁止回退。
4. 重复取消通知：已经回退过则直接返回成功。
```

当前代码已经具备核心闭环：模板创建、模板启用、Redis 库存预热、用户领取、下单锁定、支付核销、订单取消回退。后续补上定时任务后，就能覆盖过期失效和长时间锁定补偿。

## 定时任务与补偿处理

优惠券模块不能只依赖接口同步链路。支付回调可能丢失，订单取消消息可能重复，用户券也可能因为异常一直停留在“已锁定”状态。因此需要定时任务做兜底补偿，这也是该场景中“过期处理、取消回滚、核销幂等、异常补偿”的核心部分。

本案例使用 XXL-JOB 实现三个任务：

| 任务                     | 作用                                               | 建议频率        |
| ------------------------ | -------------------------------------------------- | --------------- |
| 过期优惠券失效任务       | 将超过使用有效期的未使用券、锁定券置为已过期       | 每 5 到 10 分钟 |
| 长时间锁定优惠券回退任务 | 将支付超时仍处于已锁定状态的券回退为未使用或已过期 | 每 1 到 5 分钟  |
| 核销异常补偿任务         | 扫描已支付但优惠券未核销的订单并补偿核销           | 每 5 分钟       |

### 过期优惠券失效任务

过期任务处理两类数据：

```text
1. 未使用 + 已过期：
   未使用 -> 已过期

2. 已锁定 + 已过期：
   已锁定 -> 已过期
```

实际项目中，已锁定券是否允许直接过期，需要结合订单状态判断。如果订单已经支付但核销失败，不能简单置为过期，应该先走核销补偿。本案例为了聚焦优惠券模块，过期任务只处理未使用券；已锁定券优先交给“长时间锁定回退任务”和“核销异常补偿任务”。

先补充 Mapper 方法。

文件位置：`src/main/java/io/github/atengk/coupon/mapper/UserCouponMapper.java`

```java
package io.github.atengk.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户优惠券 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    @Select("""
            SELECT *
            FROM user_coupon
            WHERE status = #{unusedStatus}
              AND use_end_time < #{nowTime}
            ORDER BY use_end_time ASC
            LIMIT #{limit}
            """)
    List<UserCoupon> selectExpiredUnusedCoupons(@Param("unusedStatus") Integer unusedStatus,
                                                @Param("nowTime") LocalDateTime nowTime,
                                                @Param("limit") Integer limit);

    @Update("""
            UPDATE user_coupon
            SET status = #{targetStatus},
                update_time = NOW()
            WHERE id = #{userCouponId}
              AND status = #{sourceStatus}
              AND use_end_time < #{nowTime}
            """)
    int expireUnusedCoupon(@Param("userCouponId") Long userCouponId,
                           @Param("sourceStatus") Integer sourceStatus,
                           @Param("targetStatus") Integer targetStatus,
                           @Param("nowTime") LocalDateTime nowTime);

    @Select("""
            SELECT *
            FROM user_coupon
            WHERE status = #{lockedStatus}
              AND lock_time IS NOT NULL
              AND lock_time < #{lockDeadline}
            ORDER BY lock_time ASC
            LIMIT #{limit}
            """)
    List<UserCoupon> selectTimeoutLockedCoupons(@Param("lockedStatus") Integer lockedStatus,
                                                @Param("lockDeadline") LocalDateTime lockDeadline,
                                                @Param("limit") Integer limit);

    @Update("""
            UPDATE user_coupon
            SET status = #{targetStatus},
                order_no = NULL,
                lock_time = NULL,
                update_time = NOW()
            WHERE id = #{userCouponId}
              AND status = #{sourceStatus}
            """)
    int rollbackLockedCouponById(@Param("userCouponId") Long userCouponId,
                                 @Param("sourceStatus") Integer sourceStatus,
                                 @Param("targetStatus") Integer targetStatus);
}
```

补偿 Service 用于承载定时任务的业务逻辑，避免 XXL-JOB Handler 写太多业务代码。

文件位置：`src/main/java/io/github/atengk/coupon/service/CouponCompensateService.java`

```java
package io.github.atengk.coupon.service;

/**
 * 优惠券补偿 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface CouponCompensateService {

    Integer expireUnusedCoupons(Integer limit);

    Integer rollbackTimeoutLockedCoupons(Integer timeoutMinutes, Integer limit);

    Integer compensateWriteOffCoupons(Integer limit);
}
```

过期、锁定超时、核销异常补偿的核心实现如下。

文件位置：`src/main/java/io/github/atengk/coupon/service/impl/CouponCompensateServiceImpl.java`

```java
package io.github.atengk.coupon.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.coupon.dto.CouponWriteOffDTO;
import io.github.atengk.coupon.entity.CouponFlow;
import io.github.atengk.coupon.entity.UserCoupon;
import io.github.atengk.coupon.enums.CouponFlowTypeEnum;
import io.github.atengk.coupon.enums.UserCouponStatusEnum;
import io.github.atengk.coupon.mapper.CouponFlowMapper;
import io.github.atengk.coupon.mapper.UserCouponMapper;
import io.github.atengk.coupon.service.CouponCompensateService;
import io.github.atengk.coupon.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券补偿 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponCompensateServiceImpl implements CouponCompensateService {

    private final UserCouponMapper userCouponMapper;

    private final CouponFlowMapper couponFlowMapper;

    private final UserCouponService userCouponService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer expireUnusedCoupons(Integer limit) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> expiredCoupons = userCouponMapper.selectExpiredUnusedCoupons(
                UserCouponStatusEnum.UNUSED.getCode(),
                now,
                limit
        );

        if (CollUtil.isEmpty(expiredCoupons)) {
            log.info("未发现需要过期处理的优惠券");
            return 0;
        }

        int successCount = 0;
        for (UserCoupon userCoupon : expiredCoupons) {
            int rows = userCouponMapper.expireUnusedCoupon(
                    userCoupon.getId(),
                    UserCouponStatusEnum.UNUSED.getCode(),
                    UserCouponStatusEnum.EXPIRED.getCode(),
                    now
            );

            if (rows > 0) {
                saveFlow(userCoupon, UserCouponStatusEnum.UNUSED.getCode(), UserCouponStatusEnum.EXPIRED.getCode(),
                        CouponFlowTypeEnum.EXPIRE, "优惠券超过使用有效期自动失效");
                successCount++;
            }
        }

        log.info("优惠券过期任务执行完成，本次扫描={}，成功过期={}", expiredCoupons.size(), successCount);
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer rollbackTimeoutLockedCoupons(Integer timeoutMinutes, Integer limit) {
        LocalDateTime lockDeadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<UserCoupon> lockedCoupons = userCouponMapper.selectTimeoutLockedCoupons(
                UserCouponStatusEnum.LOCKED.getCode(),
                lockDeadline,
                limit
        );

        if (CollUtil.isEmpty(lockedCoupons)) {
            log.info("未发现需要回退的超时锁定优惠券");
            return 0;
        }

        int successCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (UserCoupon userCoupon : lockedCoupons) {
            Integer targetStatus = now.isAfter(userCoupon.getUseEndTime())
                    ? UserCouponStatusEnum.EXPIRED.getCode()
                    : UserCouponStatusEnum.UNUSED.getCode();

            int rows = userCouponMapper.rollbackLockedCouponById(
                    userCoupon.getId(),
                    UserCouponStatusEnum.LOCKED.getCode(),
                    targetStatus
            );

            if (rows > 0) {
                saveFlow(userCoupon, UserCouponStatusEnum.LOCKED.getCode(), targetStatus,
                        CouponFlowTypeEnum.ROLLBACK,
                        StrUtil.format("锁定超过 {} 分钟自动回退", timeoutMinutes));
                successCount++;
            }
        }

        log.info("锁定超时回退任务执行完成，本次扫描={}，成功回退={}", lockedCoupons.size(), successCount);
        return successCount;
    }

    @Override
    public Integer compensateWriteOffCoupons(Integer limit) {
        /*
         * 这里给出补偿入口。真实项目中通常需要调用订单系统或支付系统查询：
         * 1. 订单已支付；
         * 2. 优惠券仍为已锁定；
         * 3. order_no 不为空；
         * 然后调用 userCouponService.writeOff() 进行幂等核销。
         *
         * 本案例不接入真实订单库，因此这里只保留标准补偿结构。
         */
        log.info("核销异常补偿任务已触发，当前示例未接入订单系统，limit={}", limit);

        // 示例：查到已支付订单后，可以这样调用
        // CouponWriteOffDTO dto = new CouponWriteOffDTO();
        // dto.setOrderNo("ORDER202606010001");
        // dto.setPayNo("PAY_COMPENSATE_202606010001");
        // dto.setPayTime(LocalDateTime.now());
        // userCouponService.writeOff(dto);

        return 0;
    }

    private void saveFlow(UserCoupon userCoupon,
                          Integer beforeStatus,
                          Integer afterStatus,
                          CouponFlowTypeEnum flowType,
                          String remark) {
        CouponFlow flow = new CouponFlow();
        flow.setUserCouponId(userCoupon.getId());
        flow.setTemplateId(userCoupon.getTemplateId());
        flow.setUserId(userCoupon.getUserId());
        flow.setOrderNo(userCoupon.getOrderNo());
        flow.setFlowType(flowType.getCode());
        flow.setBeforeStatus(beforeStatus);
        flow.setAfterStatus(afterStatus);
        flow.setBizNo(userCoupon.getOrderNo());
        flow.setRemark(remark);
        flow.setCreateTime(LocalDateTime.now());
        couponFlowMapper.insert(flow);
    }
}
```

XXL-JOB Handler 负责调度入口，每个任务只解析参数并调用 Service。

文件位置：`src/main/java/io/github/atengk/coupon/job/CouponCompensateJob.java`

```java
package io.github.atengk.coupon.job;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.coupon.service.CouponCompensateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 优惠券补偿定时任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponCompensateJob {

    private final CouponCompensateService couponCompensateService;

    @XxlJob("couponExpireJob")
    public void couponExpireJob() {
        String param = XxlJobHelper.getJobParam();
        Integer limit = Convert.toInt(StrUtil.blankToDefault(param, "500"));

        Integer count = couponCompensateService.expireUnusedCoupons(limit);
        log.info("优惠券过期任务完成，处理数量={}", count);

        XxlJobHelper.handleSuccess(StrUtil.format("处理数量={}", count));
    }

    @XxlJob("couponLockTimeoutRollbackJob")
    public void couponLockTimeoutRollbackJob() {
        String param = XxlJobHelper.getJobParam();

        /*
         * 参数格式：timeoutMinutes,limit
         * 示例：30,500
         */
        String[] args = StrUtil.splitToArray(StrUtil.blankToDefault(param, "30,500"), ',');
        Integer timeoutMinutes = Convert.toInt(args[0], 30);
        Integer limit = args.length > 1 ? Convert.toInt(args[1], 500) : 500;

        Integer count = couponCompensateService.rollbackTimeoutLockedCoupons(timeoutMinutes, limit);
        log.info("优惠券锁定超时回退任务完成，timeoutMinutes={}，处理数量={}", timeoutMinutes, count);

        XxlJobHelper.handleSuccess(StrUtil.format("处理数量={}", count));
    }

    @XxlJob("couponWriteOffCompensateJob")
    public void couponWriteOffCompensateJob() {
        String param = XxlJobHelper.getJobParam();
        Integer limit = Convert.toInt(StrUtil.blankToDefault(param, "500"));

        Integer count = couponCompensateService.compensateWriteOffCoupons(limit);
        log.info("优惠券核销异常补偿任务完成，处理数量={}", count);

        XxlJobHelper.handleSuccess(StrUtil.format("处理数量={}", count));
    }
}
```

### 长时间锁定优惠券回退任务

长时间锁定任务主要处理这种异常：

```text
用户下单
-> 优惠券已锁定
-> 订单未支付
-> 订单超时关闭
-> 取消回退通知丢失
-> 用户券一直停留在已锁定状态
```

任务规则建议：

```text
1. 扫描 status = 已锁定 的用户券。
2. lock_time 早于当前时间减去超时时间。
3. 如果优惠券未过期，则回退为未使用。
4. 如果优惠券已过期，则置为已过期。
5. 每次限制处理数量，避免一次扫描过多数据。
6. 每条状态变化写入 coupon_flow。
```

XXL-JOB 参数建议：

```text
30,500
```

参数含义：

```text
30：锁定超过 30 分钟未核销则回退
500：每次最多处理 500 条
```

生产环境中建议不要只靠优惠券模块自己判断锁定超时，最好调用订单系统确认订单确实已关闭：

```text
优惠券扫描到超时锁定
-> 调用订单系统查询订单状态
-> 订单未支付 / 已取消：回退优惠券
-> 订单已支付：走核销补偿
-> 订单仍待支付：暂不处理
```

### 核销异常补偿任务

核销异常一般来自支付成功后消息丢失或服务异常：

```text
订单支付成功
-> 支付系统回调订单系统
-> 订单状态已变成已支付
-> 优惠券核销接口调用失败 / 消息丢失
-> 用户券仍处于已锁定
```

补偿任务的推荐逻辑：

```text
1. 查询最近一段时间内 status = 已锁定 且 order_no 不为空的用户券。
2. 批量调用订单系统查询订单支付状态。
3. 如果订单已支付，调用 writeOff() 做幂等核销。
4. 如果订单已取消，调用 rollback() 做幂等回退。
5. 如果订单待支付，跳过等待下一轮。
```

伪代码如下：

```java
for (UserCoupon coupon : lockedCoupons) {
    OrderStatusDTO orderStatus = orderClient.queryStatus(coupon.getOrderNo());

    if (orderStatus.isPaid()) {
        CouponWriteOffDTO dto = new CouponWriteOffDTO();
        dto.setOrderNo(coupon.getOrderNo());
        dto.setPayNo(orderStatus.getPayNo());
        dto.setPayTime(orderStatus.getPayTime());
        userCouponService.writeOff(dto);
        continue;
    }

    if (orderStatus.isClosed()) {
        CouponRollbackDTO dto = new CouponRollbackDTO();
        dto.setOrderNo(coupon.getOrderNo());
        dto.setReason("补偿任务发现订单已关闭，自动回退优惠券");
        userCouponService.rollback(dto);
    }
}
```

注意：核销补偿必须调用已有的 `writeOff()`，不要单独写一套更新逻辑。这样可以复用核销接口里的幂等判断、状态判断和流水记录。

## 接口测试与验证

下面使用 `curl` 进行核心链路验证。为了方便测试，假设服务地址为：

```text
http://localhost:8080
```

假设登录 Token 为：

```text
test-token
```

如果本地没有接入 Sa-Token 登录流程，可以先在开发环境临时 Mock 当前用户 ID，或使用测试登录接口生成 Token。

### 创建模板测试

创建一个“满 100 减 20”的优惠券模板。

```bash
curl -X POST 'http://localhost:8080/api/coupon/templates' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer test-token' \
  -d '{
    "templateName": "新人满100减20优惠券",
    "couponType": 1,
    "discountAmount": 20.00,
    "thresholdAmount": 100.00,
    "totalStock": 5000,
    "receiveLimit": 1,
    "receiveStartTime": "2026-06-01T00:00:00",
    "receiveEndTime": "2026-06-30T23:59:59",
    "useStartTime": "2026-06-01T00:00:00",
    "useEndTime": "2026-07-07T23:59:59",
    "remark": "新人活动优惠券"
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "创建成功",
  "data": 10001
}
```

创建后检查数据库：

```sql
SELECT id, template_name, total_stock, available_stock, status
FROM coupon_template
ORDER BY create_time DESC
LIMIT 1;
```

预期结果：

```text
status = 0
available_stock = total_stock
```

启用模板并预热 Redis 库存：

```bash
curl -X POST 'http://localhost:8080/api/coupon/templates/10001/enable' \
  -H 'Authorization: Bearer test-token'
```

检查 Redis 库存：

```bash
redis-cli GET coupon:template:stock:10001
```

预期结果：

```text
5000
```

### 用户领取测试

用户领取优惠券。

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/receive' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer test-token' \
  -d '{
    "templateId": 10001
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "领取成功",
  "data": 90001
}
```

检查用户券记录：

```sql
SELECT id, template_id, user_id, coupon_name, status, order_no
FROM user_coupon
WHERE template_id = 10001
ORDER BY create_time DESC
LIMIT 1;
```

预期结果：

```text
status = 1
order_no = NULL
```

检查流水：

```sql
SELECT user_coupon_id, flow_type, before_status, after_status, remark
FROM coupon_flow
WHERE template_id = 10001
ORDER BY create_time DESC
LIMIT 5;
```

预期结果：

```text
flow_type = 1
after_status = 1
```

重复领取测试：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/receive' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer test-token' \
  -d '{
    "templateId": 10001
  }'
```

预期响应：

```json
{
  "code": 400,
  "message": "请勿重复领取",
  "data": null
}
```

### 下单锁定测试

订单金额满足满减门槛时，锁定优惠券。

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/lock' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer test-token' \
  -d '{
    "userCouponId": 90001,
    "orderNo": "ORDER202606010001",
    "orderAmount": 128.50
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "锁定成功",
  "data": {
    "userCouponId": 90001,
    "orderNo": "ORDER202606010001",
    "discountAmount": 20.00,
    "payAmount": 108.50
  }
}
```

检查用户券状态：

```sql
SELECT id, status, order_no, lock_time
FROM user_coupon
WHERE id = 90001;
```

预期结果：

```text
status = 2
order_no = ORDER202606010001
lock_time 不为空
```

订单金额不满足门槛测试：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/lock' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer test-token' \
  -d '{
    "userCouponId": 90001,
    "orderNo": "ORDER202606010002",
    "orderAmount": 88.00
  }'
```

预期响应：

```json
{
  "code": 400,
  "message": "订单金额未达到优惠券使用门槛",
  "data": null
}
```

### 支付核销测试

支付成功后调用核销接口。

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/write-off' \
  -H 'Content-Type: application/json' \
  -H 'X-Internal-Token: test-internal-token' \
  -d '{
    "orderNo": "ORDER202606010001",
    "payNo": "PAY202606010001",
    "payTime": "2026-06-01T10:30:00"
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "核销成功",
  "data": true
}
```

检查状态：

```sql
SELECT id, status, order_no, use_time
FROM user_coupon
WHERE id = 90001;
```

预期结果：

```text
status = 3
use_time 不为空
```

重复核销测试：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/write-off' \
  -H 'Content-Type: application/json' \
  -H 'X-Internal-Token: test-internal-token' \
  -d '{
    "orderNo": "ORDER202606010001",
    "payNo": "PAY202606010001",
    "payTime": "2026-06-01T10:30:00"
  }'
```

预期结果：

```text
接口返回成功，不新增重复核销效果。
```

### 取消回退测试

取消回退需要使用一张“已锁定但未核销”的优惠券测试。可以重新领取一张券，或准备另一个用户领取后锁定。

锁定后调用回退接口：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/rollback' \
  -H 'Content-Type: application/json' \
  -H 'X-Internal-Token: test-internal-token' \
  -d '{
    "orderNo": "ORDER202606010002",
    "reason": "订单支付超时自动关闭"
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "回退成功",
  "data": true
}
```

检查状态：

```sql
SELECT id, status, order_no, lock_time
FROM user_coupon
WHERE order_no = 'ORDER202606010002'
   OR id = 90002;
```

预期结果：

```text
未过期：status = 1，order_no = NULL，lock_time = NULL
已过期：status = 4，order_no = NULL，lock_time = NULL
```

已核销优惠券回退测试：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/rollback' \
  -H 'Content-Type: application/json' \
  -H 'X-Internal-Token: test-internal-token' \
  -d '{
    "orderNo": "ORDER202606010001",
    "reason": "测试已核销订单回退"
  }'
```

预期响应：

```json
{
  "code": 400,
  "message": "优惠券已核销，不能回退",
  "data": null
}
```

### 并发领取测试

并发领取重点验证两个结果：

```text
1. 优惠券库存不能扣成负数。
2. 同一个用户不能重复领取。
3. 多用户同时领取时，成功数量不能超过库存。
```

如果只想快速压测接口，可以用 ApacheBench：

```bash
ab -n 1000 -c 100 \
  -p receive.json \
  -T application/json \
  -H 'Authorization: Bearer test-token' \
  http://localhost:8080/api/user-coupons/receive
```

`receive.json` 内容：

```json
{
  "templateId": 10001
}
```

上面这个测试使用同一个 Token，预期只有第一次成功，其余返回“请勿重复领取”。

如果要验证“多用户抢同一个模板库存”，建议用 JMeter 设置不同 Token 或临时开放一个测试接口传入 `userId`。压测后执行以下 SQL：

```sql
SELECT COUNT(*) AS receive_count
FROM user_coupon
WHERE template_id = 10001;
```

检查 Redis 库存：

```bash
redis-cli GET coupon:template:stock:10001
```

检查数据库库存：

```sql
SELECT total_stock, available_stock
FROM coupon_template
WHERE id = 10001;
```

检查是否存在重复领取：

```sql
SELECT template_id, user_id, COUNT(*) AS cnt
FROM user_coupon
WHERE template_id = 10001
GROUP BY template_id, user_id
HAVING cnt > 1;
```

预期结果：

```text
1. receive_count <= total_stock
2. Redis 库存 >= 0
3. available_stock >= 0
4. 重复领取 SQL 查询结果为空
```

如果压测时出现 Redis 库存和 MySQL 库存不一致，可以通过以下 SQL 定位：

```sql
SELECT
    t.id AS template_id,
    t.total_stock,
    t.available_stock,
    COUNT(uc.id) AS user_coupon_count,
    t.total_stock - COUNT(uc.id) AS expected_available_stock
FROM coupon_template t
LEFT JOIN user_coupon uc ON uc.template_id = t.id
WHERE t.id = 10001
GROUP BY t.id, t.total_stock, t.available_stock;
```

理想情况下：

```text
available_stock = total_stock - user_coupon_count
Redis 库存 = available_stock
```

最终建议在测试完成后重点验证四张表：

| 表                  | 验证重点                                 |
| ------------------- | ---------------------------------------- |
| `coupon_template`   | 库存是否正确扣减                         |
| `user_coupon`       | 用户券状态是否符合预期                   |
| `coupon_flow`       | 是否完整记录领取、锁定、核销、回退、过期 |
| `idempotent_record` | 接入幂等后是否能拦截重复核销、重复回退   |

到这里，优惠券 / 权益发放与核销案例已经形成核心闭环：模板配置、库存预热、用户领取、并发扣减、下单锁定、支付核销、取消回退、过期失效和异常补偿。