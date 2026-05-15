# 订单交易履约链路

本文围绕 Java 后端项目中经典的“订单交易履约链路”展开，聚焦订单创建、资源锁定、支付确认、超时关闭、资源释放、幂等控制和最终一致性等核心能力。该场景适用于电商、外卖、票务、课程购买、SaaS 套餐购买、服务预约、采购系统等业务。

## 案例目标与功能边界

本案例目标不是实现一个完整商城系统，而是抽取订单交易中最核心、最能体现后端开发能力的主链路进行实现。

整体链路如下：

```text
用户创建订单
-> 校验商品 / 服务 / 资源是否可购买
-> 校验库存 / 名额 / 额度
-> 锁定资源
-> 创建订单
-> 创建支付单
-> 发起支付
-> 支付成功确认履约
-> 支付超时关闭订单
-> 释放锁定资源
```

本案例会以 Spring Boot 3 为基础，结合 MyBatis-Plus、MySQL、Redis、Redisson、RabbitMQ、XXL-JOB、Hutool 等技术实现核心功能。重点放在后端业务建模、状态流转、并发控制、幂等处理和异常补偿上。

### 核心业务目标

本案例需要实现一条稳定可靠的订单交易履约链路，确保订单、支付、库存三类核心数据在各种异常情况下仍然保持一致。

用户下单时，系统需要先校验商品是否存在、是否上架、库存是否充足。校验通过后，系统锁定库存并创建待支付订单，同时创建支付单。用户完成支付后，支付回调推动支付单变更为支付成功，并进一步推动订单进入已支付或履约完成状态，同时确认扣减之前锁定的库存。

如果用户长时间未支付，系统需要自动关闭订单，并释放之前锁定的库存。对于重复下单、重复支付回调、重复 MQ 消费、重复释放库存等情况，系统必须具备幂等处理能力。

核心目标可以概括为：

```text
防止超卖
防止重复下单
防止重复支付处理
防止订单状态错乱
防止库存重复扣减
防止库存锁定后无法释放
保证订单、支付、库存最终一致
```

本案例最终要实现的效果是：即使出现接口重试、网络超时、支付平台重复通知、MQ 重复投递、定时任务重复扫描，也不会导致订单状态异常、库存错误或资金订单不一致。

### 本案例实现范围

本案例只实现订单交易履约的核心主链路，避免引入过多外围功能影响理解和落地。

本案例包含以下内容：

一、商品库存模型。

系统会设计商品库存表，包含总库存、可用库存、锁定库存、已售库存等字段。下单时减少可用库存并增加锁定库存；支付成功后减少锁定库存并增加已售库存；订单超时关闭后减少锁定库存并恢复可用库存。

二、订单创建。

用户提交商品 ID 和购买数量后，系统校验商品状态和库存数量。为了防止并发超卖，会使用数据库条件更新作为核心兜底，并配合 Redisson 分布式锁降低热点商品并发冲突。

三、订单状态机。

订单会包含待支付、已支付、履约完成、已关闭等状态。系统只允许状态正向流转，禁止非法回退。例如，已关闭订单不能再变更为已支付，履约完成订单不能再关闭。

四、支付单创建。

订单创建成功后，系统创建对应支付单。支付单保存支付单号、订单号、支付金额、支付状态、第三方交易号等信息。支付单用于承接支付回调，避免直接操作订单导致职责混乱。

五、支付回调幂等。

支付平台可能多次通知同一笔支付结果，因此回调处理必须幂等。系统会根据支付单号或第三方交易号查询支付单，只允许待支付状态变更为支付成功。已经成功的回调直接返回成功结果。

六、支付成功履约。

支付成功后，系统推动订单状态变更，并发送 MQ 消息执行后续履约动作，例如确认扣减库存、记录库存流水、更新订单履约状态等。MQ 消费端也需要做幂等控制。

七、支付超时关闭。

订单创建时会设置支付截止时间。系统通过 XXL-JOB 或 Spring 定时任务扫描超过截止时间且仍为待支付状态的订单，将其关闭，并释放已锁定库存。

八、库存锁定与释放。

库存锁定、确认扣减、释放库存都需要有库存流水记录。释放库存时需要通过订单号或锁定流水判断是否已经释放，避免重复释放导致库存虚增。

九、本地消息表。

为了避免“业务执行成功但 MQ 消息发送失败”，支付成功、订单关闭等关键事件会先写入本地消息表，再由任务扫描投递 MQ，实现最终一致性。

十、异常补偿。

本案例会考虑以下异常补偿场景：

```text
支付成功但订单状态未更新
订单关闭但库存未释放
库存已锁定但订单创建失败
MQ 消息发送失败
MQ 消息消费失败
支付回调重复通知
定时任务重复关闭订单
```

### 不纳入本案例的扩展内容

为了保持案例聚焦，本案例不会实现完整交易系统中的所有外围能力。

不纳入本案例的内容如下：

一、不实现完整用户认证系统。

本案例默认可以从请求参数或模拟上下文中获取 userId，不展开 Sa-Token、Spring Security、JWT、多端登录、权限菜单、网关鉴权等完整认证授权能力。

二、不接入真实第三方支付平台。

本案例使用模拟支付回调接口代替微信支付、支付宝、Stripe 等真实支付渠道。重点是支付单建模、回调幂等、金额校验和订单状态推进，不展开真实签名、证书、退款、分账等能力。

三、不实现复杂营销规则。

本案例不处理优惠券、满减、积分抵扣、会员价、平台补贴、组合促销等逻辑。订单金额按商品单价和购买数量计算。

四、不实现复杂多商品拆单。

本案例可以设计订单明细表，但核心实现优先使用单商品下单。多商品订单、跨店铺拆单、仓库拆单、供应商拆单属于后续扩展。

五、不实现物流和发货系统。

支付成功后的履约只模拟为库存确认扣减和订单状态更新，不接入物流下单、发货单、物流轨迹、签收回调等功能。

六、不实现售后退款链路。

本案例不覆盖退款申请、退款审核、原路退回、库存回补、售后单状态机等能力。退款可以作为后续专项单独实现。

七、不强制使用 Seata。

本案例优先使用本地事务、本地消息表、MQ 最终一致性和补偿任务解决一致性问题。Seata 可作为微服务拆分后的扩展方案，不作为当前核心依赖。

八、不实现秒杀级高并发方案。

本案例会处理普通并发下单和防超卖，但不实现 Redis Lua 预扣库存、异步排队、削峰填谷、热点 Key 拆分、限流熔断等秒杀专项能力。

九、不实现后台管理页面。

商品管理、订单查询、支付单查询、库存流水查询等功能可以通过接口和数据库验证，不提供 Vue 后台页面。

十、不展开完整可观测体系。

本案例会在关键业务节点加入日志，但不展开 Prometheus、Grafana、SkyWalking、ELK、链路追踪和告警体系。

## 技术栈与项目结构

本案例基于原 README 中推荐的 Spring Boot 3、Redis、Redisson、RabbitMQ、MyBatis-Plus、MySQL、XXL-JOB 等技术栈实现订单交易履约核心链路。为了便于直接落地，本案例先采用单体项目结构实现核心能力，后续可以平滑拆分为订单服务、库存服务、支付服务和履约服务。

### 技术栈选型

本案例优先选择“简单可落地 + 能体现核心交易能力”的组合，不一开始强制上 Spring Cloud Alibaba、Seata 等复杂组件。

| 技术              | 作用                                       |
| ----------------- | ------------------------------------------ |
| Spring Boot 3     | 项目基础框架                               |
| MyBatis-Plus      | 数据库 CRUD、分页、条件更新                |
| MySQL 8           | 保存订单、支付单、库存、流水、本地消息     |
| Redis             | 缓存、幂等 Key、业务防重                   |
| Redisson          | 分布式锁，控制并发下单和库存释放           |
| RabbitMQ          | 订单支付成功、库存确认、订单关闭等异步事件 |
| XXL-JOB           | 超时订单关闭、消息补偿、异常数据扫描       |
| Hutool            | 日期、金额、字符串、JSON、ID 等工具处理    |
| Lombok            | 简化实体类、DTO、VO、日志对象              |
| Spring Validation | 接口参数校验                               |
| Knife4j / Swagger | 接口文档与调试，可选                       |
| Docker Compose    | 本地快速启动 MySQL、Redis、RabbitMQ        |

本案例中不强制引入 Seata。原因是当前案例可以通过“本地事务 + 本地消息表 + MQ 最终一致性 + 定时补偿”解决核心一致性问题。如果后续拆分为多个微服务，再考虑 Seata AT 模式或 Saga 模式。

推荐的核心方案如下：

```text
接口请求
-> Spring Boot Controller
-> Service 本地事务
-> MySQL 落订单、支付单、库存流水、本地消息
-> RabbitMQ 异步投递业务事件
-> Consumer 幂等消费
-> XXL-JOB 定时补偿异常数据
```

### Maven 依赖规划

下面是本案例核心依赖规划。真实项目可以按公司脚手架调整版本号和父工程结构。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Validation：接口参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- MyBatis-Plus：简化数据库 CRUD 与条件更新 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 8.x -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Redis：存储幂等 Key、缓存热点数据 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Redisson：分布式锁、并发控制 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.27.2</version>
    </dependency>

    <!-- RabbitMQ：订单履约事件异步处理 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- XXL-JOB：订单超时关闭、消息补偿、异常数据扫描 -->
    <dependency>
        <groupId>com.xuxueli</groupId>
        <artifactId>xxl-job-core</artifactId>
        <version>2.4.1</version>
    </dependency>

    <!-- Hutool：日期、金额、JSON、ID、字符串等工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、构造器等模板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Knife4j：接口文档与调试，可选 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>4.5.0</version>
    </dependency>

    <!-- Spring Boot Test：单元测试与集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果只做最小可运行版本，必须保留以下依赖：

```text
spring-boot-starter-web
spring-boot-starter-validation
mybatis-plus-spring-boot3-starter
mysql-connector-j
spring-boot-starter-data-redis
redisson-spring-boot-starter
spring-boot-starter-amqp
hutool-all
lombok
```

XXL-JOB 可以先用 Spring 定时任务替代；Knife4j 可以不接入，不影响核心业务逻辑。

### 项目包结构设计

本案例采用清晰的分层结构，避免把订单、库存、支付逻辑全部写在一个 Service 中。虽然是单体项目，但包结构按业务领域拆分，方便后续微服务化。

推荐项目结构如下：

```text
order-fulfillment-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               └── trade
        │                   ├── TradeApplication.java
        │                   ├── common
        │                   │   ├── constant
        │                   │   │   └── RedisKeyConstant.java
        │                   │   ├── enums
        │                   │   │   ├── OrderStatusEnum.java
        │                   │   │   ├── PayStatusEnum.java
        │                   │   │   ├── StockFlowTypeEnum.java
        │                   │   │   └── LocalMessageStatusEnum.java
        │                   │   ├── exception
        │                   │   │   ├── BizException.java
        │                   │   │   └── GlobalExceptionHandler.java
        │                   │   └── result
        │                   │       └── Result.java
        │                   ├── config
        │                   │   ├── MybatisPlusConfig.java
        │                   │   ├── RabbitMqConfig.java
        │                   │   └── RedissonConfig.java
        │                   ├── order
        │                   │   ├── controller
        │                   │   │   └── OrderController.java
        │                   │   ├── dto
        │                   │   │   └── OrderCreateDTO.java
        │                   │   ├── entity
        │                   │   │   ├── TradeOrder.java
        │                   │   │   └── TradeOrderItem.java
        │                   │   ├── mapper
        │                   │   │   ├── TradeOrderMapper.java
        │                   │   │   └── TradeOrderItemMapper.java
        │                   │   ├── service
        │                   │   │   ├── OrderService.java
        │                   │   │   └── impl
        │                   │   │       └── OrderServiceImpl.java
        │                   │   └── vo
        │                   │       └── OrderCreateVO.java
        │                   ├── pay
        │                   │   ├── controller
        │                   │   │   └── PayController.java
        │                   │   ├── dto
        │                   │   │   └── PayCallbackDTO.java
        │                   │   ├── entity
        │                   │   │   └── TradePayOrder.java
        │                   │   ├── mapper
        │                   │   │   └── TradePayOrderMapper.java
        │                   │   └── service
        │                   │       ├── PayService.java
        │                   │       └── impl
        │                   │           └── PayServiceImpl.java
        │                   ├── stock
        │                   │   ├── entity
        │                   │   │   ├── ProductStock.java
        │                   │   │   └── ProductStockFlow.java
        │                   │   ├── mapper
        │                   │   │   ├── ProductStockMapper.java
        │                   │   │   └── ProductStockFlowMapper.java
        │                   │   └── service
        │                   │       ├── StockService.java
        │                   │       └── impl
        │                   │           └── StockServiceImpl.java
        │                   ├── message
        │                   │   ├── entity
        │                   │   │   └── LocalMessage.java
        │                   │   ├── mapper
        │                   │   │   └── LocalMessageMapper.java
        │                   │   ├── producer
        │                   │   │   └── OrderEventProducer.java
        │                   │   ├── consumer
        │                   │   │   ├── PaySuccessConsumer.java
        │                   │   │   └── OrderCloseConsumer.java
        │                   │   └── service
        │                   │       ├── LocalMessageService.java
        │                   │       └── impl
        │                   │           └── LocalMessageServiceImpl.java
        │                   └── job
        │                       ├── OrderTimeoutCloseJob.java
        │                       └── LocalMessageRetryJob.java
        └── resources
            ├── application.yml
            └── mapper
                ├── ProductStockMapper.xml
                ├── TradeOrderMapper.xml
                └── TradePayOrderMapper.xml
```

核心包职责如下：

| 包名      | 职责                                       |
| --------- | ------------------------------------------ |
| `order`   | 订单创建、订单状态流转、订单关闭           |
| `pay`     | 支付单创建、支付回调、支付状态处理         |
| `stock`   | 库存锁定、库存确认扣减、库存释放、库存流水 |
| `message` | 本地消息表、MQ 生产者、MQ 消费者           |
| `job`     | 超时订单关闭、消息补偿、异常数据扫描       |
| `common`  | 通用返回、异常、枚举、常量                 |
| `config`  | MyBatis-Plus、RabbitMQ、Redisson 等配置    |

这种结构的好处是：订单、支付、库存之间边界清晰，但又能在一个项目里快速验证完整链路。

## 订单履约核心流程

订单履约链路的关键不是“能创建订单”，而是订单、支付、库存三者在并发、重复请求、超时、异常回调、消息重试场景下仍然保持数据一致。

本案例的主流程可以概括为：

```text
创建订单
-> 锁定库存
-> 创建订单和支付单
-> 等待支付
-> 支付成功
-> 确认履约
-> 确认扣减库存
-> 订单完成
```

异常流程可以概括为：

```text
创建订单
-> 锁定库存
-> 创建订单和支付单
-> 等待支付
-> 支付超时
-> 关闭订单
-> 释放库存
```

### 创建订单流程

创建订单是交易链路的入口，需要同时解决参数校验、商品校验、库存校验、重复下单、订单落库、支付单创建等问题。

推荐流程如下：

```text
接收创建订单请求
-> 校验 userId、productId、quantity
-> 根据 productId 查询商品库存
-> 校验商品是否存在
-> 校验商品是否可售
-> 使用 Redisson 获取商品维度分布式锁
-> 执行库存锁定
-> 生成订单号
-> 创建订单主表
-> 创建订单明细表
-> 创建支付单
-> 写入订单创建事件或延迟关闭事件
-> 提交事务
-> 返回订单号和支付单号
```

核心控制点如下：

| 控制点         | 说明                                                 |
| -------------- | ---------------------------------------------------- |
| 参数校验       | 购买数量必须大于 0，商品 ID 不能为空                 |
| 商品校验       | 商品不存在、下架、禁售时不允许下单                   |
| 库存校验       | 可用库存不足时直接失败                               |
| 分布式锁       | 降低同一商品高并发修改库存的冲突                     |
| 数据库条件更新 | 防止超卖的最终兜底                                   |
| 订单号唯一     | 使用雪花算法或业务编号生成器                         |
| 事务控制       | 库存锁定、订单、订单明细、支付单需要在同一事务中完成 |
| 超时关闭       | 创建订单后需要设置支付超时时间                       |

创建订单的状态初始化如下：

```text
订单状态：待支付
支付状态：待支付
库存状态：已锁定
支付截止时间：当前时间 + 15 分钟
```

创建订单时最重要的一点是：不能先创建订单再异步锁库存。否则可能出现订单已创建但库存不足的问题。普通交易场景下，推荐在本地事务内先锁定库存，再创建订单和支付单。

### 资源锁定流程

资源锁定是防止超卖的核心。本案例中的资源主要指商品库存，也可以扩展为课程名额、预约名额、票务座位、SaaS 套餐额度等。

库存字段建议拆成四类：

| 字段              | 说明     |
| ----------------- | -------- |
| `total_stock`     | 总库存   |
| `available_stock` | 可用库存 |
| `locked_stock`    | 锁定库存 |
| `sold_stock`      | 已售库存 |

库存锁定时，不直接增加已售库存，而是先从可用库存转移到锁定库存。

```text
下单锁定：
available_stock - quantity
locked_stock + quantity
sold_stock 不变
```

推荐使用数据库条件更新完成锁定：

```sql
update product_stock
set available_stock = available_stock - #{quantity},
    locked_stock = locked_stock + #{quantity},
    update_time = now()
where product_id = #{productId}
  and available_stock >= #{quantity};
```

这条 SQL 是防超卖的关键。即使没有 Redis 锁，只要数据库隔离级别和条件更新正确，也不会扣出负库存。

资源锁定成功后，需要写入库存流水：

```text
流水类型：LOCK
业务单号：订单号
商品 ID：productId
变更数量：quantity
变更前可用库存
变更后可用库存
变更前锁定库存
变更后锁定库存
```

库存锁定失败时，订单不能创建成功。业务上应该直接返回“库存不足”或“商品已售罄”。

资源锁定流程如下：

```text
接收锁定请求
-> 查询库存记录
-> 判断库存是否充足
-> 执行数据库条件更新
-> 判断影响行数
-> 影响行数为 0，说明库存不足或并发失败
-> 影响行数为 1，说明锁定成功
-> 写入库存锁定流水
```

在本案例中，Redisson 锁用于降低并发冲突，数据库条件更新用于最终兜底。两者不是互相替代关系。

### 支付成功履约流程

支付成功履约是交易链路中最容易出问题的部分。真实业务中，支付平台可能重复回调，MQ 可能重复投递，消费者可能重复消费，因此必须设计成幂等。

支付成功后的推荐流程如下：

```text
接收支付回调
-> 校验支付单是否存在
-> 校验支付金额是否一致
-> 校验支付单状态是否为待支付
-> 更新支付单为支付成功
-> 更新订单状态为已支付
-> 写入支付成功本地消息
-> 提交事务
-> 投递支付成功 MQ 消息
-> 消费者确认扣减库存
-> 更新订单为履约完成
```

支付回调阶段只做“支付确认”和“订单状态推进”，后续履约动作建议通过 MQ 异步处理。这样可以避免支付回调接口处理过重，也能降低第三方支付平台回调超时风险。

支付成功时状态变化如下：

```text
支付单：
待支付 -> 支付成功

订单：
待支付 -> 已支付

库存：
锁定库存暂不变，等待履约消费者确认扣减
```

履约消费者处理成功后：

```text
库存：
locked_stock - quantity
sold_stock + quantity

订单：
已支付 -> 履约完成
```

支付成功流程的关键控制点如下：

| 控制点     | 说明                                  |
| ---------- | ------------------------------------- |
| 金额校验   | 回调金额必须等于支付单金额            |
| 状态幂等   | 只有待支付支付单可以更新为支付成功    |
| 订单幂等   | 只有待支付订单可以更新为已支付        |
| 消息可靠   | 支付成功事件先写本地消息表，再投递 MQ |
| 消费幂等   | 库存确认扣减不能重复执行              |
| 状态不可逆 | 已关闭订单不能被支付成功回调重新激活  |

支付成功回调和订单超时关闭可能并发发生。处理原则是：谁先成功更新状态，谁获得业务结果。

例如：

```text
支付回调先把订单从待支付改为已支付
-> 超时关闭任务再执行时，发现订单不是待支付，不关闭

超时关闭任务先把订单从待支付改为已关闭
-> 支付回调再执行时，发现订单已关闭，需要进入异常补偿或人工处理
```

生产环境中，对于“支付成功但订单已关闭”的情况，一般不能静默忽略，需要记录异常支付单，进入退款或人工补偿流程。本案例先记录异常日志和异常状态，后续可扩展为自动退款。

### 支付超时关闭流程

订单创建后不能永久占用库存。如果用户在规定时间内没有完成支付，系统需要自动关闭订单并释放库存。

订单创建时建议保存支付截止时间：

```text
pay_expire_time = order_create_time + 15 分钟
```

超时关闭可以通过两种方式实现：

| 方式              | 说明                                         |
| ----------------- | -------------------------------------------- |
| RabbitMQ 延迟消息 | 下单后发送延迟消息，到期后检查订单状态并关闭 |
| XXL-JOB 定时扫描  | 定时扫描过期待支付订单，批量关闭             |

本案例推荐优先实现 XXL-JOB 定时扫描，因为更容易理解、调试和补偿。后续可以增加 RabbitMQ 延迟消息提升实时性。

支付超时关闭流程如下：

```text
定时任务启动
-> 查询 pay_expire_time < 当前时间 且状态为待支付的订单
-> 分批处理订单
-> 获取订单维度分布式锁
-> 再次查询订单状态
-> 如果仍然是待支付，则更新为已关闭
-> 写入订单关闭本地消息
-> 释放库存
-> 记录关闭日志
```

关闭订单时必须使用条件更新：

```sql
update trade_order
set order_status = 'CLOSED',
    close_time = now(),
    update_time = now()
where order_no = #{orderNo}
  and order_status = 'WAIT_PAY';
```

这条 SQL 可以保证只有待支付订单会被关闭。对于已支付、履约完成、已关闭的订单，定时任务重复扫描也不会影响数据。

支付超时关闭后状态变化如下：

```text
订单：
待支付 -> 已关闭

支付单：
待支付 -> 已关闭

库存：
locked_stock - quantity
available_stock + quantity
```

这里要注意：订单关闭和库存释放最好在同一个本地事务内完成。如果库存释放失败，需要保留异常记录，由补偿任务继续处理。

### 资源释放流程

资源释放主要发生在订单超时关闭、用户主动取消订单、订单创建失败回滚等场景。本案例重点实现支付超时关闭后的库存释放。

资源释放的本质是把锁定库存退回可用库存。

```text
释放库存：
locked_stock - quantity
available_stock + quantity
sold_stock 不变
```

推荐使用数据库条件更新：

```sql
update product_stock
set locked_stock = locked_stock - #{quantity},
    available_stock = available_stock + #{quantity},
    update_time = now()
where product_id = #{productId}
  and locked_stock >= #{quantity};
```

资源释放必须幂等。不能因为定时任务重复执行或 MQ 重复消费，导致库存被重复释放。

推荐使用库存流水做幂等控制：

```text
同一个 order_no + flow_type = RELEASE 只能存在一条记录
```

数据库可以增加唯一索引：

```sql
uk_order_flow_type(order_no, flow_type)
```

释放库存时流程如下：

```text
接收释放请求
-> 根据 orderNo 查询是否已有 RELEASE 流水
-> 如果已存在，直接返回成功
-> 查询订单明细，获取 productId 和 quantity
-> 执行库存释放条件更新
-> 写入 RELEASE 库存流水
-> 返回释放成功
```

资源释放的关键控制点如下：

| 控制点       | 说明                                                 |
| ------------ | ---------------------------------------------------- |
| 幂等流水     | 防止重复释放库存                                     |
| 条件更新     | 防止锁定库存被扣成负数                               |
| 订单状态校验 | 只有关闭或取消订单才允许释放                         |
| 事务控制     | 订单关闭、支付单关闭、库存释放、流水记录需要保持一致 |
| 异常补偿     | 释放失败时需要后续任务继续处理                       |

完整链路中，库存的三种变化如下：

```text
下单锁定：
available_stock - quantity
locked_stock + quantity

支付成功确认：
locked_stock - quantity
sold_stock + quantity

超时关闭释放：
locked_stock - quantity
available_stock + quantity
```

只要这三类库存动作保持幂等，并且所有库存变化都有流水记录，订单履约链路的库存一致性就比较稳定。

## 数据库表设计

本案例的数据库设计围绕“订单、支付、库存、流水、消息”五类核心数据展开。表结构不追求覆盖完整商城系统，而是保证交易履约链路可以闭环验证：下单锁库存、支付确认、履约扣减、超时关闭、释放库存、消息补偿。该设计对应 README 中“订单状态机、资源锁定与释放、支付回调幂等、超时关闭、MQ 最终一致性、异常补偿”等核心难点。

### 商品库存表

商品库存表用于保存商品库存的核心数量。库存字段拆分为总库存、可用库存、锁定库存和已售库存，避免下单时直接扣减总库存。

下单时只锁定库存：

```text
available_stock - quantity
locked_stock + quantity
```

支付成功后确认扣减：

```text
locked_stock - quantity
sold_stock + quantity
```

订单关闭后释放库存：

```text
locked_stock - quantity
available_stock + quantity
```

文件位置：`sql/product_stock.sql`

```sql
-- 商品库存表：保存商品库存核心数据
CREATE TABLE product_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称快照',
    sale_price DECIMAL(18, 2) NOT NULL COMMENT '销售单价',
    total_stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    locked_stock INT NOT NULL DEFAULT 0 COMMENT '锁定库存',
    sold_stock INT NOT NULL DEFAULT 0 COMMENT '已售库存',
    sale_status TINYINT NOT NULL DEFAULT 1 COMMENT '销售状态：1上架，0下架',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    UNIQUE KEY uk_product_id (product_id),
    KEY idx_sale_status (sale_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库存表';
```

推荐初始化一条测试数据：

```sql
-- 初始化测试商品库存
INSERT INTO product_stock (
    product_id,
    product_name,
    sale_price,
    total_stock,
    available_stock,
    locked_stock,
    sold_stock,
    sale_status
) VALUES (
    10001,
    'Java 后端高含金量项目课程',
    199.00,
    100,
    100,
    0,
    0,
    1
);
```

库存表最关键的是不要只保存一个 `stock` 字段。真实交易链路中，待支付订单会临时占用库存，如果没有 `locked_stock`，就很难准确区分“可卖库存”和“已被订单占用但还没支付的库存”。

### 订单主表

订单主表保存订单维度的核心信息，包括订单号、用户 ID、订单金额、订单状态、支付超时时间、支付时间、关闭时间等。

订单号使用业务唯一编号，例如：

```text
ORD20260515153000123456
```

订单状态建议使用数值枚举，便于存储和比较。业务展示时再通过枚举转换为中文。

文件位置：`sql/trade_order.sql`

```sql
-- 订单主表：保存订单交易主状态
CREATE TABLE trade_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(18, 2) NOT NULL COMMENT '订单总金额',
    pay_amount DECIMAL(18, 2) NOT NULL COMMENT '应付金额',
    order_status TINYINT NOT NULL COMMENT '订单状态：10待支付，20已支付，30履约完成，40已关闭',
    pay_expire_time DATETIME NOT NULL COMMENT '支付截止时间',
    pay_time DATETIME NULL COMMENT '支付成功时间',
    fulfill_time DATETIME NULL COMMENT '履约完成时间',
    close_time DATETIME NULL COMMENT '订单关闭时间',
    close_reason VARCHAR(255) NULL COMMENT '关闭原因',
    remark VARCHAR(255) NULL COMMENT '订单备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_order_status (order_status),
    KEY idx_pay_expire_time (pay_expire_time),
    KEY idx_status_expire_time (order_status, pay_expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
```

核心索引说明：

| 索引                     | 作用                       |
| ------------------------ | -------------------------- |
| `uk_order_no`            | 保证订单号唯一             |
| `idx_user_id`            | 用户订单列表查询           |
| `idx_order_status`       | 按订单状态查询             |
| `idx_pay_expire_time`    | 扫描超时订单               |
| `idx_status_expire_time` | 定时任务扫描待支付超时订单 |

定时任务关闭订单时，核心查询会使用：

```sql
SELECT *
FROM trade_order
WHERE order_status = 10
  AND pay_expire_time < NOW()
ORDER BY id ASC
LIMIT 100;
```

因此 `order_status + pay_expire_time` 的联合索引非常重要。

### 订单明细表

订单明细表保存订单下的商品快照。即使商品后续改名或改价，历史订单也不能受影响，所以订单明细中需要保存商品名称、下单单价、购买数量、实付金额等快照字段。

本案例核心代码优先实现单商品下单，但表结构保留多商品扩展能力。

文件位置：`sql/trade_order_item.sql`

```sql
-- 订单明细表：保存订单商品快照
CREATE TABLE trade_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称快照',
    sale_price DECIMAL(18, 2) NOT NULL COMMENT '下单单价',
    quantity INT NOT NULL COMMENT '购买数量',
    total_amount DECIMAL(18, 2) NOT NULL COMMENT '明细总金额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    KEY idx_order_no (order_no),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';
```

订单明细表不建议只保存 `product_id` 和 `quantity`。价格、名称这类字段必须做快照，否则后续商品信息变更会影响历史订单展示和对账。

### 支付单表

支付单表用于承接支付流程。订单是业务单，支付单是资金单，二者不要混在一张表里。

一个订单通常对应一张支付单。复杂场景下，一个订单可能多次发起支付，此时可以扩展为“一订单多支付单”，但同一时间只允许一张有效待支付支付单。本案例采用“一订单一支付单”。

文件位置：`sql/trade_pay_order.sql`

```sql
-- 支付单表：保存订单对应的支付状态
CREATE TABLE trade_pay_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    pay_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    pay_amount DECIMAL(18, 2) NOT NULL COMMENT '支付金额',
    pay_status TINYINT NOT NULL COMMENT '支付状态：10待支付，20支付成功，30已关闭，40支付异常',
    pay_channel VARCHAR(32) NULL COMMENT '支付渠道：MOCK、WECHAT、ALIPAY',
    third_trade_no VARCHAR(128) NULL COMMENT '第三方支付流水号',
    callback_time DATETIME NULL COMMENT '支付回调时间',
    pay_success_time DATETIME NULL COMMENT '支付成功时间',
    close_time DATETIME NULL COMMENT '支付单关闭时间',
    error_msg VARCHAR(512) NULL COMMENT '异常信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
    UNIQUE KEY uk_pay_no (pay_no),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_third_trade_no (third_trade_no),
    KEY idx_user_id (user_id),
    KEY idx_pay_status (pay_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单表';
```

这里有三个唯一索引：

| 唯一索引            | 作用                         |
| ------------------- | ---------------------------- |
| `uk_pay_no`         | 保证支付单号唯一             |
| `uk_order_no`       | 保证一个订单只有一个支付单   |
| `uk_third_trade_no` | 防止同一个第三方流水重复入库 |

如果使用模拟支付，`third_trade_no` 可以由接口传入或系统生成。真实支付接入时，该字段对应微信支付、支付宝等渠道返回的交易流水号。

### 资源锁定流水表

资源锁定流水表用于记录库存变化。所有库存变化都必须有流水，方便排查库存异常、做补偿、做幂等判断。

本案例中，库存流水包括三类：

```text
LOCK：下单锁定库存
CONFIRM：支付成功确认扣减
RELEASE：订单关闭释放库存
```

文件位置：`sql/product_stock_flow.sql`

```sql
-- 资源锁定流水表：记录库存锁定、确认扣减、释放流水
CREATE TABLE product_stock_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    flow_no VARCHAR(64) NOT NULL COMMENT '流水号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    flow_type TINYINT NOT NULL COMMENT '流水类型：10锁定，20确认扣减，30释放',
    quantity INT NOT NULL COMMENT '变更数量',
    before_available_stock INT NOT NULL COMMENT '变更前可用库存',
    after_available_stock INT NOT NULL COMMENT '变更后可用库存',
    before_locked_stock INT NOT NULL COMMENT '变更前锁定库存',
    after_locked_stock INT NOT NULL COMMENT '变更后锁定库存',
    before_sold_stock INT NOT NULL COMMENT '变更前已售库存',
    after_sold_stock INT NOT NULL COMMENT '变更后已售库存',
    remark VARCHAR(255) NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_flow_no (flow_no),
    UNIQUE KEY uk_order_product_type (order_no, product_id, flow_type),
    KEY idx_order_no (order_no),
    KEY idx_product_id (product_id),
    KEY idx_flow_type (flow_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源锁定流水表';
```

最关键的唯一索引是：

```sql
UNIQUE KEY uk_order_product_type (order_no, product_id, flow_type)
```

它可以保证同一个订单、同一个商品、同一种库存动作只执行一次。

例如：

```text
订单 ORD001 的商品 10001 已经执行过 RELEASE
再次释放时插入流水会冲突
业务层捕获后直接认为释放已完成
```

这就是库存释放幂等的数据库兜底。

### MQ 本地消息表

MQ 本地消息表用于解决“业务操作成功，但 MQ 消息发送失败”的问题。

比如支付回调成功后，系统需要发送“支付成功事件”。如果直接发送 MQ，可能出现数据库事务提交成功，但 MQ 发送失败。更稳妥的方式是：在本地事务中先写入本地消息表，事务提交后再投递 MQ。投递失败时，由补偿任务继续扫描发送。

文件位置：`sql/local_message.sql`

```sql
-- MQ 本地消息表：保存待发送、已发送、发送失败的业务事件
CREATE TABLE local_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_no VARCHAR(64) NOT NULL COMMENT '消息编号',
    biz_no VARCHAR(64) NOT NULL COMMENT '业务单号，如订单号或支付单号',
    message_type VARCHAR(64) NOT NULL COMMENT '消息类型',
    exchange_name VARCHAR(128) NOT NULL COMMENT '交换机名称',
    routing_key VARCHAR(128) NOT NULL COMMENT '路由键',
    message_body TEXT NOT NULL COMMENT '消息内容JSON',
    message_status TINYINT NOT NULL COMMENT '消息状态：10待发送，20已发送，30发送失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME NULL COMMENT '下次重试时间',
    sent_time DATETIME NULL COMMENT '发送成功时间',
    error_msg VARCHAR(512) NULL COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_message_no (message_no),
    UNIQUE KEY uk_biz_type (biz_no, message_type),
    KEY idx_status_retry_time (message_status, next_retry_time),
    KEY idx_biz_no (biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ本地消息表';
```

常见消息类型建议如下：

| 消息类型        | 说明             |
| --------------- | ---------------- |
| `PAY_SUCCESS`   | 支付成功事件     |
| `ORDER_CLOSED`  | 订单关闭事件     |
| `STOCK_CONFIRM` | 库存确认扣减事件 |
| `STOCK_RELEASE` | 库存释放事件     |

`uk_biz_type` 用于防止同一个业务单重复创建相同类型消息。例如同一个订单的 `PAY_SUCCESS` 消息只能创建一次。

## 订单状态机设计

订单状态机是交易系统的核心。状态机的目标不是“定义几个状态”，而是严格控制状态只能按合法路径流转，防止重复请求、异常回调、定时任务并发执行导致订单状态错乱。

本案例的订单状态机设计遵循三个原则：

```text
状态只能正向流转
关键状态不可逆
所有状态变更都必须带当前状态条件
```

### 订单状态枚举

订单状态建议使用数值存储，枚举在 Java 代码中维护。这样数据库存储紧凑，代码可读性也更好。

订单状态设计如下：

| 状态值 | 枚举        | 说明     |
| ------ | ----------- | -------- |
| `10`   | `WAIT_PAY`  | 待支付   |
| `20`   | `PAID`      | 已支付   |
| `30`   | `FULFILLED` | 履约完成 |
| `40`   | `CLOSED`    | 已关闭   |

文件位置：`src/main/java/io/github/atengk/trade/common/enums/OrderStatusEnum.java`

```java
package io.github.atengk.trade.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    /**
     * 待支付
     */
    WAIT_PAY(10, "待支付"),

    /**
     * 已支付
     */
    PAID(20, "已支付"),

    /**
     * 履约完成
     */
    FULFILLED(30, "履约完成"),

    /**
     * 已关闭
     */
    CLOSED(40, "已关闭");

    private final Integer code;

    private final String desc;

    /**
     * 根据状态值获取枚举
     *
     * @param code 状态值
     * @return 订单状态枚举
     */
    public static OrderStatusEnum of(Integer code) {
        for (OrderStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
```

订单状态说明：

| 状态     | 业务含义                             | 是否终态 |
| -------- | ------------------------------------ | -------- |
| 待支付   | 订单已创建，库存已锁定，等待用户支付 | 否       |
| 已支付   | 支付成功，等待履约处理               | 否       |
| 履约完成 | 库存已确认扣减，业务履约完成         | 是       |
| 已关闭   | 用户未支付超时关闭，或主动取消关闭   | 是       |

本案例中，`FULFILLED` 和 `CLOSED` 都属于终态。终态订单不允许再修改为其他状态。

### 支付状态枚举

支付状态和订单状态必须分开。订单状态描述业务履约进度，支付状态描述资金处理结果。

支付状态设计如下：

| 状态值 | 枚举       | 说明     |
| ------ | ---------- | -------- |
| `10`   | `WAIT_PAY` | 待支付   |
| `20`   | `SUCCESS`  | 支付成功 |
| `30`   | `CLOSED`   | 已关闭   |
| `40`   | `ERROR`    | 支付异常 |

文件位置：`src/main/java/io/github/atengk/trade/common/enums/PayStatusEnum.java`

```java
package io.github.atengk.trade.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum PayStatusEnum {

    /**
     * 待支付
     */
    WAIT_PAY(10, "待支付"),

    /**
     * 支付成功
     */
    SUCCESS(20, "支付成功"),

    /**
     * 已关闭
     */
    CLOSED(30, "已关闭"),

    /**
     * 支付异常
     */
    ERROR(40, "支付异常");

    private final Integer code;

    private final String desc;

    /**
     * 根据状态值获取枚举
     *
     * @param code 状态值
     * @return 支付状态枚举
     */
    public static PayStatusEnum of(Integer code) {
        for (PayStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
```

支付状态说明：

| 状态     | 业务含义                 | 典型场景                 |
| -------- | ------------------------ | ------------------------ |
| 待支付   | 支付单已创建，等待支付   | 订单刚创建               |
| 支付成功 | 三方支付平台确认支付成功 | 支付回调成功             |
| 已关闭   | 支付超时，支付单关闭     | 订单超时关闭             |
| 支付异常 | 支付结果和订单状态冲突   | 订单已关闭但收到成功回调 |

需要注意：支付成功不一定代表订单已经履约完成。支付成功只是资金确认，库存确认扣减、权益开通、发货等动作属于履约流程。

### 状态流转规则

订单和支付状态需要协同流转。推荐规则如下：

| 场景                 | 订单原状态 | 订单目标状态 | 支付原状态 | 支付目标状态             |
| -------------------- | ---------- | ------------ | ---------- | ------------------------ |
| 创建订单             | 无         | 待支付       | 无         | 待支付                   |
| 支付成功             | 待支付     | 已支付       | 待支付     | 支付成功                 |
| 履约完成             | 已支付     | 履约完成     | 支付成功   | 支付成功                 |
| 支付超时关闭         | 待支付     | 已关闭       | 待支付     | 已关闭                   |
| 支付成功但订单已关闭 | 已关闭     | 不变         | 待支付     | 支付异常或支付成功后退款 |

订单状态流转图如下：

```text
待支付
├── 支付成功 -> 已支付 -> 履约完成
└── 支付超时 / 用户取消 -> 已关闭
```

支付状态流转图如下：

```text
待支付
├── 支付回调成功 -> 支付成功
├── 支付超时关闭 -> 已关闭
└── 支付订单冲突 -> 支付异常
```

状态流转时必须使用条件更新，而不是先查再无条件更新。

支付成功更新订单：

```sql
UPDATE trade_order
SET order_status = 20,
    pay_time = NOW(),
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND order_status = 10;
```

履约完成更新订单：

```sql
UPDATE trade_order
SET order_status = 30,
    fulfill_time = NOW(),
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND order_status = 20;
```

超时关闭订单：

```sql
UPDATE trade_order
SET order_status = 40,
    close_time = NOW(),
    close_reason = '支付超时自动关闭',
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND order_status = 10;
```

支付成功更新支付单：

```sql
UPDATE trade_pay_order
SET pay_status = 20,
    third_trade_no = #{thirdTradeNo},
    callback_time = NOW(),
    pay_success_time = NOW(),
    update_time = NOW()
WHERE pay_no = #{payNo}
  AND pay_status = 10;
```

关闭支付单：

```sql
UPDATE trade_pay_order
SET pay_status = 30,
    close_time = NOW(),
    update_time = NOW()
WHERE order_no = #{orderNo}
  AND pay_status = 10;
```

这些 SQL 的共同点是：`WHERE` 条件中必须带上原状态。这样可以防止两个并发流程同时修改同一笔订单。

### 非法状态变更拦截

非法状态变更需要在两层拦截：

第一层是业务代码拦截。通过状态机规则判断当前状态是否允许流转到目标状态。

第二层是数据库条件更新兜底。即使业务代码判断通过，真正更新时也必须带上原状态条件。

建议增加一个订单状态机工具类，集中管理状态流转规则，避免状态判断散落在多个 Service 中。

文件位置：`src/main/java/io/github/atengk/trade/order/support/OrderStateMachine.java`

```java
package io.github.atengk.trade.order.support;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.trade.common.enums.OrderStatusEnum;

import java.util.List;
import java.util.Map;

/**
 * 订单状态机
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class OrderStateMachine {

    private static final Map<OrderStatusEnum, List<OrderStatusEnum>> ALLOW_TRANSFER_MAP = Map.of(
            OrderStatusEnum.WAIT_PAY, List.of(OrderStatusEnum.PAID, OrderStatusEnum.CLOSED),
            OrderStatusEnum.PAID, List.of(OrderStatusEnum.FULFILLED),
            OrderStatusEnum.FULFILLED, List.of(),
            OrderStatusEnum.CLOSED, List.of()
    );

    /**
     * 判断订单状态是否允许流转
     *
     * @param sourceStatus 原状态
     * @param targetStatus 目标状态
     * @return 是否允许流转
     */
    public static boolean canTransfer(OrderStatusEnum sourceStatus, OrderStatusEnum targetStatus) {
        if (sourceStatus == null || targetStatus == null) {
            return false;
        }
        List<OrderStatusEnum> targetStatusList = ALLOW_TRANSFER_MAP.get(sourceStatus);
        return CollUtil.isNotEmpty(targetStatusList) && targetStatusList.contains(targetStatus);
    }

    /**
     * 校验订单状态是否允许流转
     *
     * @param sourceStatus 原状态
     * @param targetStatus 目标状态
     */
    public static void checkTransfer(OrderStatusEnum sourceStatus, OrderStatusEnum targetStatus) {
        if (!canTransfer(sourceStatus, targetStatus)) {
            throw new IllegalStateException("订单状态不允许从【" + sourceStatus.getDesc() + "】流转到【" + targetStatus.getDesc() + "】");
        }
    }
}
```

在业务代码中使用状态机时，推荐这样处理：

```java
OrderStatusEnum sourceStatus = OrderStatusEnum.of(order.getOrderStatus());
OrderStatusEnum targetStatus = OrderStatusEnum.PAID;
OrderStateMachine.checkTransfer(sourceStatus, targetStatus);
```

但只做 Java 判断还不够，最终更新必须继续依赖条件更新。例如支付成功时：

```java
int updateRows = tradeOrderMapper.updateOrderPaid(orderNo, OrderStatusEnum.WAIT_PAY.getCode(), OrderStatusEnum.PAID.getCode());
if (updateRows == 0) {
    log.warn("订单支付状态推进失败，可能已被关闭或重复处理，订单号：{}", orderNo);
}
```

非法状态变更示例：

| 当前状态 | 目标状态 | 是否允许 | 原因                   |
| -------- | -------- | -------- | ---------------------- |
| 待支付   | 已支付   | 允许     | 用户支付成功           |
| 待支付   | 已关闭   | 允许     | 支付超时或用户取消     |
| 已支付   | 履约完成 | 允许     | 支付后完成履约         |
| 已支付   | 已关闭   | 不允许   | 已支付订单不能直接关闭 |
| 已关闭   | 已支付   | 不允许   | 关闭订单不能被重新激活 |
| 履约完成 | 已关闭   | 不允许   | 已完成订单不能关闭     |
| 履约完成 | 已支付   | 不允许   | 状态不能回退           |

订单状态拦截的最终原则是：

```text
业务代码负责判断“该不该改”
数据库条件更新负责保证“并发下只能改一次”
唯一索引和流水记录负责保证“重复请求不会重复产生结果”
```

## 核心代码实现

本节给出订单交易履约链路的核心代码，围绕“下单锁库存、创建订单、创建支付单、支付回调、确认履约、超时关闭、释放库存”实现。这里延续前文技术栈，使用 Spring Boot 3、MyBatis-Plus、MySQL、Redis、Redisson、RabbitMQ、Hutool 实现 README 中提到的订单状态机、资源锁定与释放、支付回调幂等、超时关闭和重复处理控制等核心点。

为控制篇幅，下面给出能串通主链路的核心代码。Controller、DTO、Mapper、自定义 SQL、Service 主逻辑都会覆盖；通用返回类、全局异常处理、MyBatis-Plus 基础配置可以按常规项目补充。

### 枚举与常量定义

枚举用于统一管理订单状态、支付状态、库存流水类型，避免业务代码中散落魔法值。

文件位置：`src/main/java/io/github/atengk/trade/common/enums/OrderStatusEnum.java`

```java
package io.github.atengk.trade.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    WAIT_PAY(10, "待支付"),
    PAID(20, "已支付"),
    FULFILLED(30, "履约完成"),
    CLOSED(40, "已关闭");

    private final Integer code;
    private final String desc;

    /**
     * 根据状态值获取订单状态
     *
     * @param code 状态值
     * @return 订单状态
     */
    public static OrderStatusEnum of(Integer code) {
        for (OrderStatusEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }
}
```

文件位置：`src/main/java/io/github/atengk/trade/common/enums/PayStatusEnum.java`

```java
package io.github.atengk.trade.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum PayStatusEnum {

    WAIT_PAY(10, "待支付"),
    SUCCESS(20, "支付成功"),
    CLOSED(30, "已关闭"),
    ERROR(40, "支付异常");

    private final Integer code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/trade/common/enums/StockFlowTypeEnum.java`

```java
package io.github.atengk.trade.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存流水类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum StockFlowTypeEnum {

    LOCK(10, "锁定库存"),
    CONFIRM(20, "确认扣减"),
    RELEASE(30, "释放库存");

    private final Integer code;
    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/trade/common/constant/RedisKeyConstant.java`

```java
package io.github.atengk.trade.common.constant;

/**
 * Redis Key 常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class RedisKeyConstant {

    public static final String ORDER_CREATE_LOCK = "trade:order:create:lock:";

    public static final String ORDER_CLOSE_LOCK = "trade:order:close:lock:";

    public static final String PAY_CALLBACK_LOCK = "trade:pay:callback:lock:";

    public static final String ORDER_IDEMPOTENT_KEY = "trade:order:idempotent:";

    private RedisKeyConstant() {
    }
}
```

### 实体类与 Mapper

实体类与前文表结构保持一致。这里给出核心字段，实际项目可以继续补充租户字段、操作人字段、版本字段等。

文件位置：`src/main/java/io/github/atengk/trade/stock/entity/ProductStock.java`

```java
package io.github.atengk.trade.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品库存实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("product_stock")
public class ProductStock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String productName;

    private BigDecimal salePrice;

    private Integer totalStock;

    private Integer availableStock;

    private Integer lockedStock;

    private Integer soldStock;

    private Integer saleStatus;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/trade/order/entity/TradeOrder.java`

```java
package io.github.atengk.trade.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("trade_order")
public class TradeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private Integer orderStatus;

    private LocalDateTime payExpireTime;

    private LocalDateTime payTime;

    private LocalDateTime fulfillTime;

    private LocalDateTime closeTime;

    private String closeReason;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/trade/order/entity/TradeOrderItem.java`

```java
package io.github.atengk.trade.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("trade_order_item")
public class TradeOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long productId;

    private String productName;

    private BigDecimal salePrice;

    private Integer quantity;

    private BigDecimal totalAmount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/trade/pay/entity/TradePayOrder.java`

```java
package io.github.atengk.trade.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("trade_pay_order")
public class TradePayOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payNo;

    private String orderNo;

    private Long userId;

    private BigDecimal payAmount;

    private Integer payStatus;

    private String payChannel;

    private String thirdTradeNo;

    private LocalDateTime callbackTime;

    private LocalDateTime paySuccessTime;

    private LocalDateTime closeTime;

    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/trade/stock/entity/ProductStockFlow.java`

```java
package io.github.atengk.trade.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品库存流水实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("product_stock_flow")
public class ProductStockFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String flowNo;

    private String orderNo;

    private Long productId;

    private Integer flowType;

    private Integer quantity;

    private Integer beforeAvailableStock;

    private Integer afterAvailableStock;

    private Integer beforeLockedStock;

    private Integer afterLockedStock;

    private Integer beforeSoldStock;

    private Integer afterSoldStock;

    private String remark;

    private LocalDateTime createTime;
}
```

下面是核心 Mapper。库存、订单、支付单都需要自定义条件更新，不能只依赖普通 `updateById`。

文件位置：`src/main/java/io/github/atengk/trade/stock/mapper/ProductStockMapper.java`

```java
package io.github.atengk.trade.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.trade.stock.entity.ProductStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品库存 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ProductStockMapper extends BaseMapper<ProductStock> {

    /**
     * 锁定库存
     *
     * @param productId 商品ID
     * @param quantity 数量
     * @return 影响行数
     */
    @Update("""
            UPDATE product_stock
            SET available_stock = available_stock - #{quantity},
                locked_stock = locked_stock + #{quantity},
                update_time = NOW()
            WHERE product_id = #{productId}
              AND sale_status = 1
              AND deleted = 0
              AND available_stock >= #{quantity}
            """)
    int lockStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 确认扣减锁定库存
     *
     * @param productId 商品ID
     * @param quantity 数量
     * @return 影响行数
     */
    @Update("""
            UPDATE product_stock
            SET locked_stock = locked_stock - #{quantity},
                sold_stock = sold_stock + #{quantity},
                update_time = NOW()
            WHERE product_id = #{productId}
              AND deleted = 0
              AND locked_stock >= #{quantity}
            """)
    int confirmStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 释放锁定库存
     *
     * @param productId 商品ID
     * @param quantity 数量
     * @return 影响行数
     */
    @Update("""
            UPDATE product_stock
            SET locked_stock = locked_stock - #{quantity},
                available_stock = available_stock + #{quantity},
                update_time = NOW()
            WHERE product_id = #{productId}
              AND deleted = 0
              AND locked_stock >= #{quantity}
            """)
    int releaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
```

文件位置：`src/main/java/io/github/atengk/trade/order/mapper/TradeOrderMapper.java`

```java
package io.github.atengk.trade.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.trade.order.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 订单 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {

    /**
     * 更新订单为已支付
     *
     * @param orderNo 订单号
     * @param sourceStatus 原状态
     * @param targetStatus 目标状态
     * @return 影响行数
     */
    @Update("""
            UPDATE trade_order
            SET order_status = #{targetStatus},
                pay_time = NOW(),
                update_time = NOW()
            WHERE order_no = #{orderNo}
              AND order_status = #{sourceStatus}
              AND deleted = 0
            """)
    int updateOrderPaid(@Param("orderNo") String orderNo,
                        @Param("sourceStatus") Integer sourceStatus,
                        @Param("targetStatus") Integer targetStatus);

    /**
     * 更新订单为履约完成
     *
     * @param orderNo 订单号
     * @param sourceStatus 原状态
     * @param targetStatus 目标状态
     * @return 影响行数
     */
    @Update("""
            UPDATE trade_order
            SET order_status = #{targetStatus},
                fulfill_time = NOW(),
                update_time = NOW()
            WHERE order_no = #{orderNo}
              AND order_status = #{sourceStatus}
              AND deleted = 0
            """)
    int updateOrderFulfilled(@Param("orderNo") String orderNo,
                             @Param("sourceStatus") Integer sourceStatus,
                             @Param("targetStatus") Integer targetStatus);

    /**
     * 关闭待支付订单
     *
     * @param orderNo 订单号
     * @param sourceStatus 原状态
     * @param targetStatus 目标状态
     * @param closeReason 关闭原因
     * @return 影响行数
     */
    @Update("""
            UPDATE trade_order
            SET order_status = #{targetStatus},
                close_time = NOW(),
                close_reason = #{closeReason},
                update_time = NOW()
            WHERE order_no = #{orderNo}
              AND order_status = #{sourceStatus}
              AND deleted = 0
            """)
    int closeWaitPayOrder(@Param("orderNo") String orderNo,
                          @Param("sourceStatus") Integer sourceStatus,
                          @Param("targetStatus") Integer targetStatus,
                          @Param("closeReason") String closeReason);
}
```

文件位置：`src/main/java/io/github/atengk/trade/pay/mapper/TradePayOrderMapper.java`

```java
package io.github.atengk.trade.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.trade.pay.entity.TradePayOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 支付单 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface TradePayOrderMapper extends BaseMapper<TradePayOrder> {

    /**
     * 更新支付单为支付成功
     *
     * @param payNo 支付单号
     * @param sourceStatus 原状态
     * @param targetStatus 目标状态
     * @param thirdTradeNo 第三方支付流水号
     * @return 影响行数
     */
    @Update("""
            UPDATE trade_pay_order
            SET pay_status = #{targetStatus},
                third_trade_no = #{thirdTradeNo},
                callback_time = NOW(),
                pay_success_time = NOW(),
                update_time = NOW()
            WHERE pay_no = #{payNo}
              AND pay_status = #{sourceStatus}
              AND deleted = 0
            """)
    int updatePaySuccess(@Param("payNo") String payNo,
                         @Param("sourceStatus") Integer sourceStatus,
                         @Param("targetStatus") Integer targetStatus,
                         @Param("thirdTradeNo") String thirdTradeNo);

    /**
     * 关闭待支付支付单
     *
     * @param orderNo 订单号
     * @param sourceStatus 原状态
     * @param targetStatus 目标状态
     * @return 影响行数
     */
    @Update("""
            UPDATE trade_pay_order
            SET pay_status = #{targetStatus},
                close_time = NOW(),
                update_time = NOW()
            WHERE order_no = #{orderNo}
              AND pay_status = #{sourceStatus}
              AND deleted = 0
            """)
    int closeWaitPayOrder(@Param("orderNo") String orderNo,
                          @Param("sourceStatus") Integer sourceStatus,
                          @Param("targetStatus") Integer targetStatus);
}
```

其他基础 Mapper 直接继承 `BaseMapper` 即可。

文件位置：`src/main/java/io/github/atengk/trade/order/mapper/TradeOrderItemMapper.java`

```java
package io.github.atengk.trade.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.trade.order.entity.TradeOrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface TradeOrderItemMapper extends BaseMapper<TradeOrderItem> {
}
```

文件位置：`src/main/java/io/github/atengk/trade/stock/mapper/ProductStockFlowMapper.java`

```java
package io.github.atengk.trade.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.trade.stock.entity.ProductStockFlow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存流水 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ProductStockFlowMapper extends BaseMapper<ProductStockFlow> {
}
```

### 创建订单接口

创建订单接口接收用户 ID、商品 ID、购买数量，返回订单号、支付单号和支付金额。

文件位置：`src/main/java/io/github/atengk/trade/order/dto/OrderCreateDTO.java`

```java
package io.github.atengk.trade.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建订单请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderCreateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;
}
```

文件位置：`src/main/java/io/github/atengk/trade/order/vo/OrderCreateVO.java`

```java
package io.github.atengk.trade.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单响应结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class OrderCreateVO {

    private String orderNo;

    private String payNo;

    private BigDecimal payAmount;

    private LocalDateTime payExpireTime;
}
```

文件位置：`src/main/java/io/github/atengk/trade/order/controller/OrderController.java`

```java
package io.github.atengk.trade.order.controller;

import io.github.atengk.trade.order.dto.OrderCreateDTO;
import io.github.atengk.trade.order.service.OrderService;
import io.github.atengk.trade.order.vo.OrderCreateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param dto 创建订单参数
     * @return 创建结果
     */
    @PostMapping
    public OrderCreateVO createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        return orderService.createOrder(dto);
    }

    /**
     * 关闭超时订单，测试环境可手动触发
     *
     * @param orderNo 订单号
     */
    @PostMapping("/{orderNo}/close-timeout")
    public void closeTimeoutOrder(@PathVariable String orderNo) {
        orderService.closeTimeoutOrder(orderNo);
    }
}
```

### 库存锁定实现

库存服务负责锁定库存、确认扣减库存、释放库存，并写入库存流水。库存动作必须幂等，尤其是确认扣减和释放库存。

文件位置：`src/main/java/io/github/atengk/trade/stock/service/StockService.java`

```java
package io.github.atengk.trade.stock.service;

/**
 * 库存服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface StockService {

    /**
     * 锁定库存
     *
     * @param orderNo 订单号
     * @param productId 商品ID
     * @param quantity 数量
     */
    void lockStock(String orderNo, Long productId, Integer quantity);

    /**
     * 支付成功后确认扣减库存
     *
     * @param orderNo 订单号
     */
    void confirmStock(String orderNo);

    /**
     * 释放库存
     *
     * @param orderNo 订单号
     */
    void releaseStock(String orderNo);
}
```

下面实现库存锁定、确认扣减和释放库存。库存流水唯一索引用于做数据库级幂等兜底。

文件位置：`src/main/java/io/github/atengk/trade/stock/service/impl/StockServiceImpl.java`

```java
package io.github.atengk.trade.stock.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.trade.common.enums.StockFlowTypeEnum;
import io.github.atengk.trade.order.entity.TradeOrderItem;
import io.github.atengk.trade.order.mapper.TradeOrderItemMapper;
import io.github.atengk.trade.stock.entity.ProductStock;
import io.github.atengk.trade.stock.entity.ProductStockFlow;
import io.github.atengk.trade.stock.mapper.ProductStockFlowMapper;
import io.github.atengk.trade.stock.mapper.ProductStockMapper;
import io.github.atengk.trade.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final ProductStockMapper productStockMapper;
    private final ProductStockFlowMapper productStockFlowMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;

    /**
     * 锁定库存
     *
     * @param orderNo 订单号
     * @param productId 商品ID
     * @param quantity 数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockStock(String orderNo, Long productId, Integer quantity) {
        ProductStock beforeStock = getProductStock(productId);

        int rows = productStockMapper.lockStock(productId, quantity);
        if (rows == 0) {
            log.warn("库存锁定失败，库存不足或商品不可售，订单号：{}，商品ID：{}，数量：{}", orderNo, productId, quantity);
            throw new IllegalStateException("库存不足");
        }

        ProductStock afterStock = getProductStock(productId);
        saveStockFlow(orderNo, productId, StockFlowTypeEnum.LOCK, quantity, beforeStock, afterStock, "下单锁定库存");
        log.info("库存锁定成功，订单号：{}，商品ID：{}，数量：{}", orderNo, productId, quantity);
    }

    /**
     * 确认扣减库存
     *
     * @param orderNo 订单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmStock(String orderNo) {
        TradeOrderItem item = getOrderItem(orderNo);
        Long productId = item.getProductId();
        Integer quantity = item.getQuantity();

        if (existStockFlow(orderNo, productId, StockFlowTypeEnum.CONFIRM)) {
            log.info("库存确认扣减已处理，直接返回，订单号：{}", orderNo);
            return;
        }

        ProductStock beforeStock = getProductStock(productId);
        int rows = productStockMapper.confirmStock(productId, quantity);
        if (rows == 0) {
            log.warn("库存确认扣减失败，锁定库存不足，订单号：{}，商品ID：{}", orderNo, productId);
            throw new IllegalStateException("库存确认扣减失败");
        }

        ProductStock afterStock = getProductStock(productId);
        saveStockFlow(orderNo, productId, StockFlowTypeEnum.CONFIRM, quantity, beforeStock, afterStock, "支付成功确认扣减库存");
        log.info("库存确认扣减成功，订单号：{}，商品ID：{}，数量：{}", orderNo, productId, quantity);
    }

    /**
     * 释放库存
     *
     * @param orderNo 订单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseStock(String orderNo) {
        TradeOrderItem item = getOrderItem(orderNo);
        Long productId = item.getProductId();
        Integer quantity = item.getQuantity();

        if (existStockFlow(orderNo, productId, StockFlowTypeEnum.RELEASE)) {
            log.info("库存释放已处理，直接返回，订单号：{}", orderNo);
            return;
        }

        ProductStock beforeStock = getProductStock(productId);
        int rows = productStockMapper.releaseStock(productId, quantity);
        if (rows == 0) {
            log.warn("库存释放失败，锁定库存不足，订单号：{}，商品ID：{}", orderNo, productId);
            throw new IllegalStateException("库存释放失败");
        }

        ProductStock afterStock = getProductStock(productId);
        saveStockFlow(orderNo, productId, StockFlowTypeEnum.RELEASE, quantity, beforeStock, afterStock, "订单关闭释放库存");
        log.info("库存释放成功，订单号：{}，商品ID：{}，数量：{}", orderNo, productId, quantity);
    }

    /**
     * 查询商品库存
     *
     * @param productId 商品ID
     * @return 商品库存
     */
    private ProductStock getProductStock(Long productId) {
        ProductStock stock = productStockMapper.selectOne(new LambdaQueryWrapper<ProductStock>()
                .eq(ProductStock::getProductId, productId)
                .eq(ProductStock::getDeleted, 0));
        if (stock == null) {
            throw new IllegalArgumentException("商品库存不存在");
        }
        return stock;
    }

    /**
     * 查询订单明细
     *
     * @param orderNo 订单号
     * @return 订单明细
     */
    private TradeOrderItem getOrderItem(String orderNo) {
        TradeOrderItem item = tradeOrderItemMapper.selectOne(new LambdaQueryWrapper<TradeOrderItem>()
                .eq(TradeOrderItem::getOrderNo, orderNo)
                .eq(TradeOrderItem::getDeleted, 0)
                .last("LIMIT 1"));
        if (item == null) {
            throw new IllegalArgumentException("订单明细不存在");
        }
        return item;
    }

    /**
     * 判断库存流水是否存在
     *
     * @param orderNo 订单号
     * @param productId 商品ID
     * @param flowType 流水类型
     * @return 是否存在
     */
    private boolean existStockFlow(String orderNo, Long productId, StockFlowTypeEnum flowType) {
        Long count = productStockFlowMapper.selectCount(new LambdaQueryWrapper<ProductStockFlow>()
                .eq(ProductStockFlow::getOrderNo, orderNo)
                .eq(ProductStockFlow::getProductId, productId)
                .eq(ProductStockFlow::getFlowType, flowType.getCode()));
        return count != null && count > 0;
    }

    /**
     * 保存库存流水
     *
     * @param orderNo 订单号
     * @param productId 商品ID
     * @param flowType 流水类型
     * @param quantity 数量
     * @param beforeStock 变更前库存
     * @param afterStock 变更后库存
     * @param remark 备注
     */
    private void saveStockFlow(String orderNo,
                               Long productId,
                               StockFlowTypeEnum flowType,
                               Integer quantity,
                               ProductStock beforeStock,
                               ProductStock afterStock,
                               String remark) {
        ProductStockFlow flow = new ProductStockFlow();
        flow.setFlowNo("SF" + IdUtil.getSnowflakeNextIdStr());
        flow.setOrderNo(orderNo);
        flow.setProductId(productId);
        flow.setFlowType(flowType.getCode());
        flow.setQuantity(quantity);
        flow.setBeforeAvailableStock(beforeStock.getAvailableStock());
        flow.setAfterAvailableStock(afterStock.getAvailableStock());
        flow.setBeforeLockedStock(beforeStock.getLockedStock());
        flow.setAfterLockedStock(afterStock.getLockedStock());
        flow.setBeforeSoldStock(beforeStock.getSoldStock());
        flow.setAfterSoldStock(afterStock.getSoldStock());
        flow.setRemark(remark);

        try {
            productStockFlowMapper.insert(flow);
        } catch (DuplicateKeyException e) {
            log.info("库存流水已存在，忽略重复写入，订单号：{}，商品ID：{}，流水类型：{}", orderNo, productId, flowType.getDesc());
        }
    }
}
```

### 支付单创建实现

支付单创建由订单创建流程调用。支付单状态初始为待支付，支付金额必须和订单应付金额一致。

文件位置：`src/main/java/io/github/atengk/trade/pay/service/PayService.java`

```java
package io.github.atengk.trade.pay.service;

import io.github.atengk.trade.pay.dto.PayCallbackDTO;

/**
 * 支付服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayService {

    /**
     * 创建支付单
     *
     * @param orderNo 订单号
     * @param userId 用户ID
     * @param payAmount 支付金额
     * @return 支付单号
     */
    String createPayOrder(String orderNo, Long userId, java.math.BigDecimal payAmount);

    /**
     * 处理支付回调
     *
     * @param dto 支付回调参数
     */
    void handlePayCallback(PayCallbackDTO dto);
}
```

文件位置：`src/main/java/io/github/atengk/trade/pay/service/impl/PayServiceImpl.java`

```java
package io.github.atengk.trade.pay.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.trade.common.constant.RedisKeyConstant;
import io.github.atengk.trade.common.enums.OrderStatusEnum;
import io.github.atengk.trade.common.enums.PayStatusEnum;
import io.github.atengk.trade.order.entity.TradeOrder;
import io.github.atengk.trade.order.mapper.TradeOrderMapper;
import io.github.atengk.trade.pay.dto.PayCallbackDTO;
import io.github.atengk.trade.pay.entity.TradePayOrder;
import io.github.atengk.trade.pay.mapper.TradePayOrderMapper;
import io.github.atengk.trade.pay.service.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final TradePayOrderMapper tradePayOrderMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final RedissonClient redissonClient;

    /**
     * 创建支付单
     *
     * @param orderNo 订单号
     * @param userId 用户ID
     * @param payAmount 支付金额
     * @return 支付单号
     */
    @Override
    public String createPayOrder(String orderNo, Long userId, BigDecimal payAmount) {
        String payNo = "PAY" + IdUtil.getSnowflakeNextIdStr();

        TradePayOrder payOrder = new TradePayOrder();
        payOrder.setPayNo(payNo);
        payOrder.setOrderNo(orderNo);
        payOrder.setUserId(userId);
        payOrder.setPayAmount(payAmount);
        payOrder.setPayStatus(PayStatusEnum.WAIT_PAY.getCode());
        payOrder.setPayChannel("MOCK");
        payOrder.setCreateTime(LocalDateTime.now());
        payOrder.setUpdateTime(LocalDateTime.now());
        payOrder.setDeleted(0);

        tradePayOrderMapper.insert(payOrder);
        log.info("支付单创建成功，订单号：{}，支付单号：{}，金额：{}", orderNo, payNo, payAmount);
        return payNo;
    }

    /**
     * 处理支付回调
     *
     * @param dto 支付回调参数
     */
    @Override
    public void handlePayCallback(PayCallbackDTO dto) {
        String lockKey = RedisKeyConstant.PAY_CALLBACK_LOCK + dto.getPayNo();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("支付回调处理中，请勿重复提交");
            }
            doHandlePayCallback(dto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("支付回调获取锁被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行支付回调核心逻辑
     *
     * @param dto 支付回调参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void doHandlePayCallback(PayCallbackDTO dto) {
        TradePayOrder payOrder = tradePayOrderMapper.selectOne(new LambdaQueryWrapper<TradePayOrder>()
                .eq(TradePayOrder::getPayNo, dto.getPayNo())
                .eq(TradePayOrder::getDeleted, 0));

        if (payOrder == null) {
            throw new IllegalArgumentException("支付单不存在");
        }

        if (PayStatusEnum.SUCCESS.getCode().equals(payOrder.getPayStatus())) {
            log.info("支付回调重复通知，支付单已成功，支付单号：{}", dto.getPayNo());
            return;
        }

        if (!PayStatusEnum.WAIT_PAY.getCode().equals(payOrder.getPayStatus())) {
            log.warn("支付单状态不允许处理成功回调，支付单号：{}，当前状态：{}", dto.getPayNo(), payOrder.getPayStatus());
            return;
        }

        if (payOrder.getPayAmount().compareTo(dto.getPayAmount()) != 0) {
            log.warn("支付金额不一致，支付单号：{}，应付金额：{}，回调金额：{}", dto.getPayNo(), payOrder.getPayAmount(), dto.getPayAmount());
            throw new IllegalStateException("支付金额不一致");
        }

        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, payOrder.getOrderNo())
                .eq(TradeOrder::getDeleted, 0));

        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        if (OrderStatusEnum.CLOSED.getCode().equals(order.getOrderStatus())) {
            log.warn("订单已关闭但收到支付成功回调，订单号：{}，支付单号：{}", order.getOrderNo(), dto.getPayNo());
            throw new IllegalStateException("订单已关闭，需进入退款或人工补偿");
        }

        int payRows = tradePayOrderMapper.updatePaySuccess(
                dto.getPayNo(),
                PayStatusEnum.WAIT_PAY.getCode(),
                PayStatusEnum.SUCCESS.getCode(),
                dto.getThirdTradeNo()
        );

        if (payRows == 0) {
            log.info("支付单状态已被其他流程处理，支付单号：{}", dto.getPayNo());
            return;
        }

        int orderRows = tradeOrderMapper.updateOrderPaid(
                order.getOrderNo(),
                OrderStatusEnum.WAIT_PAY.getCode(),
                OrderStatusEnum.PAID.getCode()
        );

        if (orderRows == 0) {
            log.warn("订单支付状态推进失败，订单号：{}，可能已关闭或已处理", order.getOrderNo());
            throw new IllegalStateException("订单支付状态推进失败");
        }

        log.info("支付回调处理成功，订单号：{}，支付单号：{}", order.getOrderNo(), dto.getPayNo());

        // 简化处理：本案例后续可在这里写本地消息表，再投递 PAY_SUCCESS MQ。
        // 当前核心流程中，可以由调用方或 MQ 消费者继续执行 orderService.confirmFulfillment(orderNo)。
    }
}
```

### 支付回调幂等处理

支付回调参数需要包含支付单号、第三方流水号、支付金额。真实场景还需要签名、支付渠道、支付时间等字段。

文件位置：`src/main/java/io/github/atengk/trade/pay/dto/PayCallbackDTO.java`

```java
package io.github.atengk.trade.pay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付回调请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class PayCallbackDTO {

    @NotBlank(message = "支付单号不能为空")
    private String payNo;

    @NotBlank(message = "第三方支付流水号不能为空")
    private String thirdTradeNo;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal payAmount;
}
```

文件位置：`src/main/java/io/github/atengk/trade/pay/controller/PayController.java`

```java
package io.github.atengk.trade.pay.controller;

import io.github.atengk.trade.pay.dto.PayCallbackDTO;
import io.github.atengk.trade.pay.service.PayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    /**
     * 模拟支付成功回调
     *
     * @param dto 支付回调参数
     */
    @PostMapping("/mock/callback")
    public void mockPayCallback(@Valid @RequestBody PayCallbackDTO dto) {
        payService.handlePayCallback(dto);
    }
}
```

幂等控制点如下：

```text
1. Redisson 按 payNo 加锁，避免同一支付单并发回调。
2. 支付单状态必须是 WAIT_PAY 才能更新为 SUCCESS。
3. update SQL 带 pay_status = WAIT_PAY 条件。
4. third_trade_no 唯一索引防止第三方流水重复入库。
5. 已经 SUCCESS 的支付单重复回调直接返回成功。
```

### 订单履约确认实现

履约确认发生在支付成功之后。它负责确认扣减锁定库存，并将订单从已支付更新为履约完成。

文件位置：`src/main/java/io/github/atengk/trade/order/service/OrderService.java`

```java
package io.github.atengk.trade.order.service;

import io.github.atengk.trade.order.dto.OrderCreateDTO;
import io.github.atengk.trade.order.vo.OrderCreateVO;

/**
 * 订单服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface OrderService {

    /**
     * 创建订单
     *
     * @param dto 创建订单参数
     * @return 创建订单结果
     */
    OrderCreateVO createOrder(OrderCreateDTO dto);

    /**
     * 确认订单履约
     *
     * @param orderNo 订单号
     */
    void confirmFulfillment(String orderNo);

    /**
     * 关闭超时订单
     *
     * @param orderNo 订单号
     */
    void closeTimeoutOrder(String orderNo);
}
```

下面是订单服务核心实现，覆盖创建订单、确认履约、超时关闭三条主路径。

文件位置：`src/main/java/io/github/atengk/trade/order/service/impl/OrderServiceImpl.java`

```java
package io.github.atengk.trade.order.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.trade.common.constant.RedisKeyConstant;
import io.github.atengk.trade.common.enums.OrderStatusEnum;
import io.github.atengk.trade.common.enums.PayStatusEnum;
import io.github.atengk.trade.order.dto.OrderCreateDTO;
import io.github.atengk.trade.order.entity.TradeOrder;
import io.github.atengk.trade.order.entity.TradeOrderItem;
import io.github.atengk.trade.order.mapper.TradeOrderItemMapper;
import io.github.atengk.trade.order.mapper.TradeOrderMapper;
import io.github.atengk.trade.order.service.OrderService;
import io.github.atengk.trade.order.vo.OrderCreateVO;
import io.github.atengk.trade.pay.mapper.TradePayOrderMapper;
import io.github.atengk.trade.pay.service.PayService;
import io.github.atengk.trade.stock.entity.ProductStock;
import io.github.atengk.trade.stock.mapper.ProductStockMapper;
import io.github.atengk.trade.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int PAY_TIMEOUT_MINUTES = 15;

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final ProductStockMapper productStockMapper;
    private final TradePayOrderMapper tradePayOrderMapper;
    private final StockService stockService;
    private final PayService payService;
    private final RedissonClient redissonClient;

    /**
     * 创建订单
     *
     * @param dto 创建订单参数
     * @return 创建订单结果
     */
    @Override
    public OrderCreateVO createOrder(OrderCreateDTO dto) {
        String lockKey = RedisKeyConstant.ORDER_CREATE_LOCK + dto.getProductId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("当前商品下单人数较多，请稍后再试");
            }
            return doCreateOrder(dto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("创建订单获取锁被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行创建订单核心逻辑
     *
     * @param dto 创建订单参数
     * @return 创建订单结果
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO doCreateOrder(OrderCreateDTO dto) {
        ProductStock stock = productStockMapper.selectOne(new LambdaQueryWrapper<ProductStock>()
                .eq(ProductStock::getProductId, dto.getProductId())
                .eq(ProductStock::getDeleted, 0));

        if (stock == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!Integer.valueOf(1).equals(stock.getSaleStatus())) {
            throw new IllegalStateException("商品已下架");
        }
        if (stock.getAvailableStock() < dto.getQuantity()) {
            throw new IllegalStateException("库存不足");
        }

        String orderNo = "ORD" + IdUtil.getSnowflakeNextIdStr();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime payExpireTime = LocalDateTimeUtil.offset(now, java.time.Duration.ofMinutes(PAY_TIMEOUT_MINUTES));
        BigDecimal totalAmount = stock.getSalePrice().multiply(BigDecimal.valueOf(dto.getQuantity()));

        stockService.lockStock(orderNo, dto.getProductId(), dto.getQuantity());

        TradeOrder order = new TradeOrder();
        order.setOrderNo(orderNo);
        order.setUserId(dto.getUserId());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setOrderStatus(OrderStatusEnum.WAIT_PAY.getCode());
        order.setPayExpireTime(payExpireTime);
        order.setRemark(StrUtil.format("用户{}创建订单", dto.getUserId()));
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setDeleted(0);
        tradeOrderMapper.insert(order);

        TradeOrderItem item = new TradeOrderItem();
        item.setOrderNo(orderNo);
        item.setProductId(stock.getProductId());
        item.setProductName(stock.getProductName());
        item.setSalePrice(stock.getSalePrice());
        item.setQuantity(dto.getQuantity());
        item.setTotalAmount(totalAmount);
        item.setCreateTime(now);
        item.setUpdateTime(now);
        item.setDeleted(0);
        tradeOrderItemMapper.insert(item);

        String payNo = payService.createPayOrder(orderNo, dto.getUserId(), totalAmount);

        log.info("订单创建成功，订单号：{}，支付单号：{}，用户ID：{}，商品ID：{}，数量：{}",
                orderNo, payNo, dto.getUserId(), dto.getProductId(), dto.getQuantity());

        return OrderCreateVO.builder()
                .orderNo(orderNo)
                .payNo(payNo)
                .payAmount(totalAmount)
                .payExpireTime(payExpireTime)
                .build();
    }

    /**
     * 确认订单履约
     *
     * @param orderNo 订单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmFulfillment(String orderNo) {
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, orderNo)
                .eq(TradeOrder::getDeleted, 0));

        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        if (OrderStatusEnum.FULFILLED.getCode().equals(order.getOrderStatus())) {
            log.info("订单已履约完成，直接返回，订单号：{}", orderNo);
            return;
        }

        if (!OrderStatusEnum.PAID.getCode().equals(order.getOrderStatus())) {
            throw new IllegalStateException("订单状态不是已支付，不能确认履约");
        }

        stockService.confirmStock(orderNo);

        int rows = tradeOrderMapper.updateOrderFulfilled(
                orderNo,
                OrderStatusEnum.PAID.getCode(),
                OrderStatusEnum.FULFILLED.getCode()
        );

        if (rows == 0) {
            log.warn("订单履约状态更新失败，订单号：{}", orderNo);
            throw new IllegalStateException("订单履约状态更新失败");
        }

        log.info("订单履约完成，订单号：{}", orderNo);
    }

    /**
     * 关闭超时订单
     *
     * @param orderNo 订单号
     */
    @Override
    public void closeTimeoutOrder(String orderNo) {
        String lockKey = RedisKeyConstant.ORDER_CLOSE_LOCK + orderNo;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.info("订单正在关闭处理中，跳过本次关闭，订单号：{}", orderNo);
                return;
            }
            doCloseTimeoutOrder(orderNo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("关闭订单获取锁被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行关闭超时订单逻辑
     *
     * @param orderNo 订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public void doCloseTimeoutOrder(String orderNo) {
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, orderNo)
                .eq(TradeOrder::getDeleted, 0));

        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        if (!OrderStatusEnum.WAIT_PAY.getCode().equals(order.getOrderStatus())) {
            log.info("订单不是待支付状态，不需要关闭，订单号：{}，当前状态：{}", orderNo, order.getOrderStatus());
            return;
        }

        if (order.getPayExpireTime().isAfter(LocalDateTime.now())) {
            log.info("订单未到支付超时时间，不关闭，订单号：{}", orderNo);
            return;
        }

        int orderRows = tradeOrderMapper.closeWaitPayOrder(
                orderNo,
                OrderStatusEnum.WAIT_PAY.getCode(),
                OrderStatusEnum.CLOSED.getCode(),
                "支付超时自动关闭"
        );

        if (orderRows == 0) {
            log.info("订单关闭失败，可能已被支付或关闭，订单号：{}", orderNo);
            return;
        }

        tradePayOrderMapper.closeWaitPayOrder(
                orderNo,
                PayStatusEnum.WAIT_PAY.getCode(),
                PayStatusEnum.CLOSED.getCode()
        );

        stockService.releaseStock(orderNo);
        log.info("超时订单关闭完成，订单号：{}", orderNo);
    }
}
```

### 超时订单关闭实现

生产环境建议由 XXL-JOB 触发扫描。下面给出 Spring 定时任务版本，便于本地直接运行；接入 XXL-JOB 时，只需要把 `scanTimeoutOrders` 方法迁移到 XXL-JOB Handler 中即可。

文件位置：`src/main/java/io/github/atengk/trade/job/OrderTimeoutCloseJob.java`

```java
package io.github.atengk.trade.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.trade.common.enums.OrderStatusEnum;
import io.github.atengk.trade.order.entity.TradeOrder;
import io.github.atengk.trade.order.mapper.TradeOrderMapper;
import io.github.atengk.trade.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时订单关闭任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutCloseJob {

    private final TradeOrderMapper tradeOrderMapper;
    private final OrderService orderService;

    /**
     * 扫描并关闭超时未支付订单
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void scanTimeoutOrders() {
        List<TradeOrder> timeoutOrders = tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderStatus, OrderStatusEnum.WAIT_PAY.getCode())
                .lt(TradeOrder::getPayExpireTime, LocalDateTime.now())
                .eq(TradeOrder::getDeleted, 0)
                .orderByAsc(TradeOrder::getId)
                .last("LIMIT 100"));

        if (timeoutOrders.isEmpty()) {
            return;
        }

        log.info("开始扫描超时订单，本次数量：{}", timeoutOrders.size());

        for (TradeOrder order : timeoutOrders) {
            try {
                orderService.closeTimeoutOrder(order.getOrderNo());
            } catch (Exception e) {
                log.error("关闭超时订单失败，订单号：{}", order.getOrderNo(), e);
            }
        }
    }
}
```

启动类需要开启定时任务。

文件位置：`src/main/java/io/github/atengk/trade/TradeApplication.java`

```java
package io.github.atengk.trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单交易履约应用
 *
 * @author Ateng
 * @since 2026-05-15
 */
@EnableScheduling
@SpringBootApplication
public class TradeApplication {

    /**
     * 启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class, args);
    }
}
```

### 库存释放实现

库存释放已经在 `StockServiceImpl#releaseStock` 中实现。这里单独强调释放库存的调用时机和幂等要求。

释放库存只应该发生在以下场景：

```text
订单超时关闭
用户主动取消待支付订单
订单创建失败后的事务回滚不需要手动释放，因为锁库存和建订单在同一事务内
```

释放库存的核心代码为：

```java
if (existStockFlow(orderNo, productId, StockFlowTypeEnum.RELEASE)) {
    log.info("库存释放已处理，直接返回，订单号：{}", orderNo);
    return;
}

ProductStock beforeStock = getProductStock(productId);
int rows = productStockMapper.releaseStock(productId, quantity);
if (rows == 0) {
    log.warn("库存释放失败，锁定库存不足，订单号：{}，商品ID：{}", orderNo, productId);
    throw new IllegalStateException("库存释放失败");
}

ProductStock afterStock = getProductStock(productId);
saveStockFlow(orderNo, productId, StockFlowTypeEnum.RELEASE, quantity, beforeStock, afterStock, "订单关闭释放库存");
```

库存释放要同时依赖两层保护：

```text
1. 业务层先查 RELEASE 流水，已释放直接返回。
2. 数据库唯一索引 uk_order_product_type 防止重复写入 RELEASE 流水。
```

如果出现并发释放，最多只有一个线程能成功插入 `RELEASE` 流水。另一个线程即使执行到流水插入，也会被唯一索引拦截。

## 幂等与并发控制

订单履约链路中，幂等和并发控制不能只靠 Redis，也不能只靠数据库。推荐组合方式是：

```text
入口层：Redis / Redisson 控制重复请求
业务层：状态机判断当前状态是否允许处理
数据库层：条件更新 + 唯一索引兜底
补偿层：任务重复扫描但业务幂等
```

这样即使某一层失效，仍然有下一层保护。

### 创建订单防重复提交

创建订单防重复提交有两类策略。

第一类是前端传入幂等 Key，例如 `requestId`。服务端使用 Redis 保存该 Key，短时间内重复提交直接拒绝或返回原结果。

第二类是业务唯一约束，例如同一用户对同一活动商品只能存在一笔待支付订单。普通电商下单不一定限制重复购买，但秒杀、预约、课程购买通常需要限制。

本案例先给出 Redis 幂等 Key 方案。请求 DTO 增加 `requestId` 字段即可。

```java
@NotBlank(message = "请求流水号不能为空")
private String requestId;
```

创建订单前先判断 Redis Key。

```java
String idemKey = RedisKeyConstant.ORDER_IDEMPOTENT_KEY + dto.getUserId() + ":" + dto.getRequestId();
Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(idemKey, "PROCESSING", Duration.ofMinutes(10));
if (Boolean.FALSE.equals(success)) {
    throw new IllegalStateException("请勿重复提交订单");
}
```

订单创建成功后，可以把 Redis 值改成订单号。

```java
stringRedisTemplate.opsForValue().set(idemKey, orderNo, Duration.ofMinutes(30));
```

这种方式可以防止用户因为网络超时、按钮连点、接口重试导致重复下单。

如果业务要求“一人一单”，还需要增加数据库唯一索引。例如活动订单可以建立：

```sql
UNIQUE KEY uk_user_product_unpaid (user_id, product_id, biz_status)
```

普通订单不建议直接对 `user_id + product_id` 做唯一索引，因为用户可能合法购买多次。

### 支付回调幂等

支付回调幂等是交易系统必须重点处理的部分。第三方支付平台重复通知是正常现象，不能把重复通知当异常。

本案例中支付回调幂等通过以下方式保证：

```text
1. Redisson 按 payNo 加分布式锁。
2. 支付单 SUCCESS 状态重复回调直接返回。
3. 支付单只有 WAIT_PAY 可以更新为 SUCCESS。
4. updatePaySuccess SQL 带 pay_status = WAIT_PAY 条件。
5. third_trade_no 建唯一索引，避免同一三方流水重复处理。
```

关键判断逻辑：

```java
if (PayStatusEnum.SUCCESS.getCode().equals(payOrder.getPayStatus())) {
    log.info("支付回调重复通知，支付单已成功，支付单号：{}", dto.getPayNo());
    return;
}

if (!PayStatusEnum.WAIT_PAY.getCode().equals(payOrder.getPayStatus())) {
    log.warn("支付单状态不允许处理成功回调，支付单号：{}，当前状态：{}", dto.getPayNo(), payOrder.getPayStatus());
    return;
}
```

关键数据库更新：

```sql
UPDATE trade_pay_order
SET pay_status = 20,
    third_trade_no = #{thirdTradeNo},
    callback_time = NOW(),
    pay_success_time = NOW(),
    update_time = NOW()
WHERE pay_no = #{payNo}
  AND pay_status = 10
  AND deleted = 0;
```

重复回调时，如果第一次已经把支付单更新为成功，第二次执行该 SQL 的影响行数就是 `0`，业务直接返回成功即可。

### Redis 分布式锁

Redisson 分布式锁用于降低同一业务资源的并发冲突。本案例主要有三类锁：

| 锁 Key                                | 使用场景           |
| ------------------------------------- | ------------------ |
| `trade:order:create:lock:{productId}` | 同一商品并发下单   |
| `trade:pay:callback:lock:{payNo}`     | 同一支付单并发回调 |
| `trade:order:close:lock:{orderNo}`    | 同一订单并发关闭   |

创建订单锁示例：

```java
String lockKey = RedisKeyConstant.ORDER_CREATE_LOCK + dto.getProductId();
RLock lock = redissonClient.getLock(lockKey);

try {
    boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
    if (!locked) {
        throw new IllegalStateException("当前商品下单人数较多，请稍后再试");
    }
    return doCreateOrder(dto);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new IllegalStateException("创建订单获取锁被中断");
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

Redisson 锁不是防超卖的唯一手段。真正防超卖的兜底必须是数据库条件更新：

```sql
WHERE available_stock >= #{quantity}
```

原因是 Redis 锁可能因为配置、网络、锁超时等问题产生边界风险，而数据库条件更新是库存扣减的最终防线。

### 数据库唯一索引兜底

交易链路不能完全相信应用层判断。凡是涉及“只能处理一次”的动作，都应该有唯一索引兜底。

本案例推荐的唯一索引如下：

```sql
-- 订单号唯一
UNIQUE KEY uk_order_no (order_no)

-- 支付单号唯一
UNIQUE KEY uk_pay_no (pay_no)

-- 一个订单一张支付单
UNIQUE KEY uk_order_no (order_no)

-- 第三方支付流水唯一
UNIQUE KEY uk_third_trade_no (third_trade_no)

-- 同一订单、同一商品、同一库存动作只能执行一次
UNIQUE KEY uk_order_product_type (order_no, product_id, flow_type)

-- 同一业务单、同一消息类型只能生成一条本地消息
UNIQUE KEY uk_biz_type (biz_no, message_type)
```

这些唯一索引分别解决以下问题：

| 唯一索引                 | 解决的问题                   |
| ------------------------ | ---------------------------- |
| `uk_order_no`            | 防止订单号重复               |
| `uk_pay_no`              | 防止支付单号重复             |
| `uk_order_no` 支付单索引 | 防止一个订单重复创建支付单   |
| `uk_third_trade_no`      | 防止同一三方流水重复处理     |
| `uk_order_product_type`  | 防止库存重复锁定、扣减、释放 |
| `uk_biz_type`            | 防止重复创建 MQ 本地消息     |

最终实现原则是：

```text
Redis 锁提升并发体验
状态机控制业务合法性
条件更新保证并发正确性
唯一索引保证重复请求不产生重复数据
定时补偿保证异常场景最终一致
```

## MQ 最终一致性

订单交易履约链路中，MQ 的作用不是简单“异步化”，而是把支付成功后的履约动作从支付回调接口中解耦出来。支付回调只负责确认支付结果和推进订单到“已支付”，库存确认扣减、履约完成、通知等动作通过 MQ 消费处理。

这样可以避免支付回调接口过重，也能解决 README 中提到的“MQ 最终一致性、异常补偿、支付回调幂等、资源锁定与释放”等问题。

本案例采用“本地消息表 + RabbitMQ + 定时补偿”的方式保证最终一致性：

```text
支付回调成功
-> 本地事务内更新支付单
-> 本地事务内更新订单为已支付
-> 本地事务内写入本地消息表
-> 事务提交后投递 MQ
-> 消费者确认扣减库存
-> 消费者更新订单为履约完成
-> 失败时由本地消息补偿任务重新投递
```

### 订单事件消息设计

订单事件消息不直接传完整订单对象，只传业务处理所需的最小字段，避免消息体过大，也避免订单字段变更影响消费者。

本案例定义一类核心消息：

```text
PAY_SUCCESS：支付成功事件
```

后续可以继续扩展：

```text
ORDER_CLOSED：订单关闭事件
STOCK_RELEASE：库存释放事件
ORDER_FULFILLED：订单履约完成事件
```

文件位置：`src/main/java/io/github/atengk/trade/common/enums/LocalMessageStatusEnum.java`

```java
package io.github.atengk.trade.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 本地消息状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum LocalMessageStatusEnum {

    WAIT_SEND(10, "待发送"),
    SENT(20, "已发送"),
    SEND_FAIL(30, "发送失败");

    private final Integer code;

    private final String desc;
}
```

文件位置：`src/main/java/io/github/atengk/trade/common/constant/MqConstant.java`

```java
package io.github.atengk.trade.common.constant;

/**
 * MQ 常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class MqConstant {

    /**
     * 订单事件交换机
     */
    public static final String ORDER_EVENT_EXCHANGE = "trade.order.event.exchange";

    /**
     * 支付成功队列
     */
    public static final String PAY_SUCCESS_QUEUE = "trade.pay.success.queue";

    /**
     * 支付成功路由键
     */
    public static final String PAY_SUCCESS_ROUTING_KEY = "trade.pay.success";

    /**
     * 支付成功消息类型
     */
    public static final String PAY_SUCCESS_MESSAGE_TYPE = "PAY_SUCCESS";

    private MqConstant() {
    }
}
```

文件位置：`src/main/java/io/github/atengk/trade/message/dto/OrderEventMessage.java`

```java
package io.github.atengk.trade.message.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单事件消息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderEventMessage {

    /**
     * 消息编号
     */
    private String messageNo;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付单号
     */
    private String payNo;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;
}
```

文件位置：`src/main/java/io/github/atengk/trade/config/RabbitMqConfig.java`

```java
package io.github.atengk.trade.config;

import io.github.atengk.trade.common.constant.MqConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 订单事件交换机
     *
     * @return Direct 交换机
     */
    @Bean
    public DirectExchange orderEventExchange() {
        return ExchangeBuilder
                .directExchange(MqConstant.ORDER_EVENT_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 支付成功队列
     *
     * @return 支付成功队列
     */
    @Bean
    public Queue paySuccessQueue() {
        return QueueBuilder
                .durable(MqConstant.PAY_SUCCESS_QUEUE)
                .build();
    }

    /**
     * 支付成功队列绑定
     *
     * @return 绑定关系
     */
    @Bean
    public Binding paySuccessBinding() {
        return BindingBuilder
                .bind(paySuccessQueue())
                .to(orderEventExchange())
                .with(MqConstant.PAY_SUCCESS_ROUTING_KEY);
    }
}
```

建议 RabbitMQ 消费端使用手动确认，避免业务未执行完成但消息被提前确认。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1 # RabbitMQ 地址
    port: 5672 # RabbitMQ 端口
    username: guest # RabbitMQ 用户名
    password: guest # RabbitMQ 密码
    virtual-host: / # RabbitMQ 虚拟主机
    publisher-confirm-type: correlated # 开启发布确认
    publisher-returns: true # 开启消息退回
    listener:
      simple:
        acknowledge-mode: manual # 消费端手动 ACK
        retry:
          enabled: true # 开启消费端重试
          max-attempts: 3 # 最大重试次数
          initial-interval: 1000ms # 首次重试间隔
          multiplier: 2 # 重试间隔倍数
          max-interval: 10000ms # 最大重试间隔
```

### 支付成功消息发送

支付成功消息必须和支付状态、订单状态在同一个本地事务内落库。不能先提交支付成功，再直接发 MQ；否则可能出现数据库成功但 MQ 失败。

本地消息实体如下。

文件位置：`src/main/java/io/github/atengk/trade/message/entity/LocalMessage.java`

```java
package io.github.atengk.trade.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("local_message")
public class LocalMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageNo;

    private String bizNo;

    private String messageType;

    private String exchangeName;

    private String routingKey;

    private String messageBody;

    private Integer messageStatus;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime sentTime;

    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/trade/message/mapper/LocalMessageMapper.java`

```java
package io.github.atengk.trade.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.trade.message.entity.LocalMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 本地消息 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface LocalMessageMapper extends BaseMapper<LocalMessage> {

    /**
     * 标记消息发送成功
     *
     * @param messageNo 消息编号
     * @return 影响行数
     */
    @Update("""
            UPDATE local_message
            SET message_status = 20,
                sent_time = NOW(),
                update_time = NOW(),
                error_msg = NULL
            WHERE message_no = #{messageNo}
              AND message_status IN (10, 30)
            """)
    int markSent(@Param("messageNo") String messageNo);

    /**
     * 标记消息发送失败
     *
     * @param messageNo 消息编号
     * @param nextRetryTime 下次重试时间
     * @param errorMsg 错误信息
     * @return 影响行数
     */
    @Update("""
            UPDATE local_message
            SET message_status = 30,
                retry_count = retry_count + 1,
                next_retry_time = #{nextRetryTime},
                error_msg = #{errorMsg},
                update_time = NOW()
            WHERE message_no = #{messageNo}
            """)
    int markSendFail(@Param("messageNo") String messageNo,
                     @Param("nextRetryTime") java.time.LocalDateTime nextRetryTime,
                     @Param("errorMsg") String errorMsg);
}
```

本地消息服务负责创建支付成功消息和发送消息。

文件位置：`src/main/java/io/github/atengk/trade/message/service/LocalMessageService.java`

```java
package io.github.atengk.trade.message.service;

/**
 * 本地消息服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface LocalMessageService {

    /**
     * 创建支付成功消息
     *
     * @param orderNo 订单号
     * @param payNo 支付单号
     */
    void createPaySuccessMessage(String orderNo, String payNo);

    /**
     * 发送指定消息
     *
     * @param messageNo 消息编号
     */
    void sendMessage(String messageNo);

    /**
     * 发送待补偿消息
     */
    void retrySendMessages();
}
```

下面的实现使用 Hutool `JSONUtil` 序列化消息体，并通过本地消息表记录发送状态。

文件位置：`src/main/java/io/github/atengk/trade/message/service/impl/LocalMessageServiceImpl.java`

```java
package io.github.atengk.trade.message.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.trade.common.constant.MqConstant;
import io.github.atengk.trade.common.enums.LocalMessageStatusEnum;
import io.github.atengk.trade.message.dto.OrderEventMessage;
import io.github.atengk.trade.message.entity.LocalMessage;
import io.github.atengk.trade.message.mapper.LocalMessageMapper;
import io.github.atengk.trade.message.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalMessageServiceImpl implements LocalMessageService {

    private final LocalMessageMapper localMessageMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 创建支付成功消息
     *
     * @param orderNo 订单号
     * @param payNo 支付单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPaySuccessMessage(String orderNo, String payNo) {
        String messageNo = "MSG" + IdUtil.getSnowflakeNextIdStr();

        OrderEventMessage eventMessage = new OrderEventMessage();
        eventMessage.setMessageNo(messageNo);
        eventMessage.setMessageType(MqConstant.PAY_SUCCESS_MESSAGE_TYPE);
        eventMessage.setOrderNo(orderNo);
        eventMessage.setPayNo(payNo);
        eventMessage.setEventTime(LocalDateTime.now());

        LocalMessage localMessage = new LocalMessage();
        localMessage.setMessageNo(messageNo);
        localMessage.setBizNo(orderNo);
        localMessage.setMessageType(MqConstant.PAY_SUCCESS_MESSAGE_TYPE);
        localMessage.setExchangeName(MqConstant.ORDER_EVENT_EXCHANGE);
        localMessage.setRoutingKey(MqConstant.PAY_SUCCESS_ROUTING_KEY);
        localMessage.setMessageBody(JSONUtil.toJsonStr(eventMessage));
        localMessage.setMessageStatus(LocalMessageStatusEnum.WAIT_SEND.getCode());
        localMessage.setRetryCount(0);
        localMessage.setNextRetryTime(LocalDateTime.now());
        localMessage.setCreateTime(LocalDateTime.now());
        localMessage.setUpdateTime(LocalDateTime.now());

        try {
            localMessageMapper.insert(localMessage);
            log.info("支付成功本地消息创建成功，订单号：{}，消息编号：{}", orderNo, messageNo);
        } catch (DuplicateKeyException e) {
            log.info("支付成功本地消息已存在，跳过重复创建，订单号：{}", orderNo);
        }
    }

    /**
     * 发送指定消息
     *
     * @param messageNo 消息编号
     */
    @Override
    public void sendMessage(String messageNo) {
        LocalMessage localMessage = localMessageMapper.selectOne(new LambdaQueryWrapper<LocalMessage>()
                .eq(LocalMessage::getMessageNo, messageNo));

        if (localMessage == null) {
            throw new IllegalArgumentException("本地消息不存在");
        }

        doSend(localMessage);
    }

    /**
     * 发送待补偿消息
     */
    @Override
    public void retrySendMessages() {
        List<LocalMessage> messages = localMessageMapper.selectList(new LambdaQueryWrapper<LocalMessage>()
                .in(LocalMessage::getMessageStatus,
                        LocalMessageStatusEnum.WAIT_SEND.getCode(),
                        LocalMessageStatusEnum.SEND_FAIL.getCode())
                .le(LocalMessage::getNextRetryTime, LocalDateTime.now())
                .lt(LocalMessage::getRetryCount, 5)
                .orderByAsc(LocalMessage::getId)
                .last("LIMIT 100"));

        if (messages.isEmpty()) {
            return;
        }

        log.info("开始补偿投递本地消息，本次数量：{}", messages.size());

        for (LocalMessage message : messages) {
            doSend(message);
        }
    }

    /**
     * 执行消息发送
     *
     * @param localMessage 本地消息
     */
    private void doSend(LocalMessage localMessage) {
        try {
            rabbitTemplate.convertAndSend(
                    localMessage.getExchangeName(),
                    localMessage.getRoutingKey(),
                    localMessage.getMessageBody()
            );

            localMessageMapper.markSent(localMessage.getMessageNo());
            log.info("本地消息发送成功，消息编号：{}，业务单号：{}", localMessage.getMessageNo(), localMessage.getBizNo());
        } catch (Exception e) {
            LocalDateTime nextRetryTime = LocalDateTimeUtil.offset(LocalDateTime.now(), Duration.ofMinutes(1));
            String errorMsg = StrUtil.sub(e.getMessage(), 0, 500);

            localMessageMapper.markSendFail(localMessage.getMessageNo(), nextRetryTime, errorMsg);
            log.error("本地消息发送失败，消息编号：{}，业务单号：{}", localMessage.getMessageNo(), localMessage.getBizNo(), e);
        }
    }
}
```

支付回调成功后，需要在同一个事务内创建本地消息。修改前文 `PayServiceImpl#doHandlePayCallback`，在订单状态更新成功后增加一行：

```java
localMessageService.createPaySuccessMessage(order.getOrderNo(), dto.getPayNo());
```

也就是：

```java
int orderRows = tradeOrderMapper.updateOrderPaid(
        order.getOrderNo(),
        OrderStatusEnum.WAIT_PAY.getCode(),
        OrderStatusEnum.PAID.getCode()
);

if (orderRows == 0) {
    log.warn("订单支付状态推进失败，订单号：{}，可能已关闭或已处理", order.getOrderNo());
    throw new IllegalStateException("订单支付状态推进失败");
}

localMessageService.createPaySuccessMessage(order.getOrderNo(), dto.getPayNo());

log.info("支付回调处理成功，订单号：{}，支付单号：{}", order.getOrderNo(), dto.getPayNo());
```

为了让支付回调后尽快发送消息，可以在事务提交后主动调用发送，也可以完全依赖补偿任务扫描。简单起见，本案例推荐：支付回调只写本地消息，定时任务负责投递。这样代码更稳，不依赖事务后置回调。

### 库存确认扣减消费

库存确认扣减消费者接收 `PAY_SUCCESS` 消息后，调用 `OrderService#confirmFulfillment`。该方法内部会先确认扣减库存，再把订单状态从“已支付”更新为“履约完成”。

消费端必须幂等，因为 RabbitMQ 可能重复投递，业务代码也可能抛异常后重试。

文件位置：`src/main/java/io/github/atengk/trade/message/consumer/PaySuccessConsumer.java`

```java
package io.github.atengk.trade.message.consumer;

import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import io.github.atengk.trade.common.constant.MqConstant;
import io.github.atengk.trade.message.dto.OrderEventMessage;
import io.github.atengk.trade.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 支付成功消息消费者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaySuccessConsumer {

    private final OrderService orderService;

    /**
     * 消费支付成功消息
     *
     * @param body 消息体
     * @param message 原始消息
     * @param channel RabbitMQ 通道
     */
    @RabbitListener(queues = MqConstant.PAY_SUCCESS_QUEUE)
    public void consumePaySuccess(String body, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            OrderEventMessage eventMessage = JSONUtil.toBean(body, OrderEventMessage.class);
            log.info("收到支付成功消息，订单号：{}，消息编号：{}", eventMessage.getOrderNo(), eventMessage.getMessageNo());

            orderService.confirmFulfillment(eventMessage.getOrderNo());

            channel.basicAck(deliveryTag, false);
            log.info("支付成功消息消费完成，订单号：{}，消息编号：{}", eventMessage.getOrderNo(), eventMessage.getMessageNo());
        } catch (Exception e) {
            log.error("支付成功消息消费失败，消息体：{}", body, e);

            try {
                // requeue=false：避免异常消息无限立即重回队列；生产环境可配死信队列承接
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ackException) {
                log.error("支付成功消息 NACK 失败", ackException);
            }
        }
    }
}
```

这里的幂等依赖 `OrderService#confirmFulfillment` 和 `StockService#confirmStock` 两层：

```text
1. 订单已是 FULFILLED 时直接返回。
2. 库存 CONFIRM 流水已存在时直接返回。
3. 更新订单履约完成时 SQL 带 order_status = PAID 条件。
4. 库存流水表有 uk_order_product_type 唯一索引。
```

因此即使同一条支付成功消息被消费多次，也不会重复扣减库存。

### 消费失败重试与补偿

消费失败分两类：

```text
消息未投递到 MQ
消息已投递，但消费者处理失败
```

第一类由本地消息补偿任务处理，扫描 `local_message` 中待发送或发送失败的数据重新投递。

第二类建议使用 RabbitMQ 重试和死信队列处理。为了保持案例简洁，前文消费者中 `basicNack(..., false)` 不重新入队，生产环境可以绑定死信交换机，把失败消息投递到死信队列，再由人工或补偿任务处理。

建议生产规则如下：

| 失败场景     | 处理方式                             |
| ------------ | ------------------------------------ |
| MQ 发送失败  | 本地消息表标记发送失败，定时任务重发 |
| 消费临时失败 | RabbitMQ listener retry 自动重试     |
| 消费多次失败 | 进入死信队列                         |
| 死信堆积     | 后台人工处理或补偿任务重新投递       |
| 消费重复     | 依赖订单状态和库存流水幂等           |

如果要补充死信队列，可以扩展以下常量：

```java
public static final String PAY_SUCCESS_DLX = "trade.pay.success.dlx";
public static final String PAY_SUCCESS_DLQ = "trade.pay.success.dlq";
public static final String PAY_SUCCESS_DLK = "trade.pay.success.dead";
```

本案例核心是保证业务幂等。只要业务幂等成立，MQ 重试和补偿任务就可以放心重复执行。

## 定时补偿任务

定时补偿任务是交易系统稳定性的兜底。它的目标不是替代正常流程，而是在 MQ 失败、网络异常、服务重启、第三方回调异常等场景下，把数据推进到最终一致状态。

本案例需要三类补偿任务：

```text
扫描超时未支付订单
扫描未投递本地消息
扫描异常支付单
```

### 扫描超时未支付订单

该任务前文已经给出基础实现。这里补充它在完整链路中的职责。

扫描条件：

```sql
SELECT *
FROM trade_order
WHERE order_status = 10
  AND pay_expire_time < NOW()
  AND deleted = 0
ORDER BY id ASC
LIMIT 100;
```

处理逻辑：

```text
查询超时待支付订单
-> 按订单号加分布式锁
-> 再次确认订单仍为待支付
-> 关闭订单
-> 关闭支付单
-> 释放库存
-> 记录日志
```

文件位置：`src/main/java/io/github/atengk/trade/job/OrderTimeoutCloseJob.java`

```java
package io.github.atengk.trade.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.trade.common.enums.OrderStatusEnum;
import io.github.atengk.trade.order.entity.TradeOrder;
import io.github.atengk.trade.order.mapper.TradeOrderMapper;
import io.github.atengk.trade.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时订单关闭任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutCloseJob {

    private final TradeOrderMapper tradeOrderMapper;
    private final OrderService orderService;

    /**
     * 每分钟扫描超时未支付订单
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void scanTimeoutOrders() {
        List<TradeOrder> timeoutOrders = tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderStatus, OrderStatusEnum.WAIT_PAY.getCode())
                .lt(TradeOrder::getPayExpireTime, LocalDateTime.now())
                .eq(TradeOrder::getDeleted, 0)
                .orderByAsc(TradeOrder::getId)
                .last("LIMIT 100"));

        if (timeoutOrders.isEmpty()) {
            return;
        }

        log.info("扫描到超时未支付订单，本次数量：{}", timeoutOrders.size());

        for (TradeOrder order : timeoutOrders) {
            try {
                orderService.closeTimeoutOrder(order.getOrderNo());
            } catch (Exception e) {
                log.error("超时订单关闭失败，订单号：{}", order.getOrderNo(), e);
            }
        }
    }
}
```

这个任务可以重复执行，因为 `closeTimeoutOrder` 内部有以下幂等保护：

```text
1. 订单号维度 Redisson 锁。
2. 只关闭 WAIT_PAY 订单。
3. 关闭订单 SQL 带 order_status = WAIT_PAY 条件。
4. 释放库存依赖 RELEASE 流水幂等。
```

### 扫描未投递本地消息

该任务负责扫描 `local_message` 表中未发送或发送失败的消息，重新投递到 RabbitMQ。

扫描条件：

```sql
SELECT *
FROM local_message
WHERE message_status IN (10, 30)
  AND next_retry_time <= NOW()
  AND retry_count < 5
ORDER BY id ASC
LIMIT 100;
```

文件位置：`src/main/java/io/github/atengk/trade/job/LocalMessageRetryJob.java`

```java
package io.github.atengk.trade.job;

import io.github.atengk.trade.message.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 本地消息重试任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMessageRetryJob {

    private final LocalMessageService localMessageService;

    /**
     * 每分钟补偿投递未发送成功的本地消息
     */
    @Scheduled(cron = "10 */1 * * * ?")
    public void retryLocalMessages() {
        log.info("开始执行本地消息补偿投递任务");
        localMessageService.retrySendMessages();
    }
}
```

为了避免频繁重试，可以使用递增退避策略。上文示例固定 1 分钟后重试，生产环境可以按重试次数计算：

```text
第 1 次失败：1 分钟后重试
第 2 次失败：3 分钟后重试
第 3 次失败：5 分钟后重试
第 4 次失败：10 分钟后重试
第 5 次失败：转人工或告警
```

可将 `LocalMessageServiceImpl#doSend` 中的下一次重试时间改成：

```java
int nextMinutes = Math.min((localMessage.getRetryCount() + 1) * 2, 10);
LocalDateTime nextRetryTime = LocalDateTimeUtil.offset(LocalDateTime.now(), Duration.ofMinutes(nextMinutes));
```

### 扫描异常支付单

异常支付单补偿用于处理支付成功后订单状态没有被正确推进的场景。

典型异常包括：

```text
支付单已成功，但订单仍是待支付
支付单已成功，但订单已关闭
订单已支付，但未生成支付成功本地消息
订单已支付，但长时间未履约完成
```

本案例先处理最常见的两类：

```text
1. 支付单 SUCCESS + 订单 WAIT_PAY：尝试推进订单到 PAID，并补发 PAY_SUCCESS 消息。
2. 订单 PAID 长时间未 FULFILLED：补发 PAY_SUCCESS 消息，让消费者重新确认履约。
```

先给支付补偿服务接口。

文件位置：`src/main/java/io/github/atengk/trade/pay/service/PayCompensateService.java`

```java
package io.github.atengk.trade.pay.service;

/**
 * 支付补偿服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface PayCompensateService {

    /**
     * 补偿异常支付单
     */
    void compensateAbnormalPayOrders();
}
```

下面的实现扫描支付成功但订单状态未正确推进的数据。

文件位置：`src/main/java/io/github/atengk/trade/pay/service/impl/PayCompensateServiceImpl.java`

```java
package io.github.atengk.trade.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.trade.common.enums.OrderStatusEnum;
import io.github.atengk.trade.common.enums.PayStatusEnum;
import io.github.atengk.trade.message.service.LocalMessageService;
import io.github.atengk.trade.order.entity.TradeOrder;
import io.github.atengk.trade.order.mapper.TradeOrderMapper;
import io.github.atengk.trade.pay.entity.TradePayOrder;
import io.github.atengk.trade.pay.mapper.TradePayOrderMapper;
import io.github.atengk.trade.pay.service.PayCompensateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 支付补偿服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayCompensateServiceImpl implements PayCompensateService {

    private final TradePayOrderMapper tradePayOrderMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final LocalMessageService localMessageService;

    /**
     * 补偿异常支付单
     */
    @Override
    public void compensateAbnormalPayOrders() {
        List<TradePayOrder> payOrders = tradePayOrderMapper.selectList(new LambdaQueryWrapper<TradePayOrder>()
                .eq(TradePayOrder::getPayStatus, PayStatusEnum.SUCCESS.getCode())
                .eq(TradePayOrder::getDeleted, 0)
                .orderByAsc(TradePayOrder::getId)
                .last("LIMIT 100"));

        if (payOrders.isEmpty()) {
            return;
        }

        log.info("开始扫描异常支付单，本次数量：{}", payOrders.size());

        for (TradePayOrder payOrder : payOrders) {
            compensateOne(payOrder);
        }
    }

    /**
     * 补偿单笔支付单
     *
     * @param payOrder 支付单
     */
    private void compensateOne(TradePayOrder payOrder) {
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, payOrder.getOrderNo())
                .eq(TradeOrder::getDeleted, 0));

        if (order == null) {
            log.warn("支付单对应订单不存在，支付单号：{}，订单号：{}", payOrder.getPayNo(), payOrder.getOrderNo());
            return;
        }

        if (OrderStatusEnum.WAIT_PAY.getCode().equals(order.getOrderStatus())) {
            int rows = tradeOrderMapper.updateOrderPaid(
                    order.getOrderNo(),
                    OrderStatusEnum.WAIT_PAY.getCode(),
                    OrderStatusEnum.PAID.getCode()
            );

            if (rows > 0) {
                localMessageService.createPaySuccessMessage(order.getOrderNo(), payOrder.getPayNo());
                log.info("支付成功订单状态补偿完成，订单号：{}，支付单号：{}", order.getOrderNo(), payOrder.getPayNo());
            }
            return;
        }

        if (OrderStatusEnum.PAID.getCode().equals(order.getOrderStatus())) {
            localMessageService.createPaySuccessMessage(order.getOrderNo(), payOrder.getPayNo());
            log.info("已支付未履约订单补发支付成功消息，订单号：{}，支付单号：{}", order.getOrderNo(), payOrder.getPayNo());
            return;
        }

        if (OrderStatusEnum.CLOSED.getCode().equals(order.getOrderStatus())) {
            log.warn("支付成功但订单已关闭，需要退款或人工处理，订单号：{}，支付单号：{}", order.getOrderNo(), payOrder.getPayNo());
        }
    }
}
```

对应定时任务如下。

文件位置：`src/main/java/io/github/atengk/trade/job/PayAbnormalCompensateJob.java`

```java
package io.github.atengk.trade.job;

import io.github.atengk.trade.pay.service.PayCompensateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 异常支付单补偿任务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayAbnormalCompensateJob {

    private final PayCompensateService payCompensateService;

    /**
     * 每 5 分钟扫描异常支付单
     */
    @Scheduled(cron = "30 */5 * * * ?")
    public void compensateAbnormalPayOrders() {
        log.info("开始执行异常支付单补偿任务");
        payCompensateService.compensateAbnormalPayOrders();
    }
}
```

补偿任务的核心原则是：可以重复扫描、可以重复补发消息，但业务处理必须幂等。

本案例的最终一致性闭环如下：

```text
支付回调成功
-> 支付单 SUCCESS
-> 订单 PAID
-> 本地消息 WAIT_SEND
-> 本地消息补偿任务投递 MQ
-> 消费者确认扣减库存
-> 订单 FULFILLED
```

如果中间任意一步失败：

```text
MQ 未发送：LocalMessageRetryJob 重发
MQ 消费失败：RabbitMQ 重试或死信补偿
订单未推进：PayAbnormalCompensateJob 补偿
库存未扣减：重复消费 PAY_SUCCESS，StockService 幂等确认扣减
订单未履约：重复调用 confirmFulfillment，订单状态条件更新兜底
```

## 接口测试与验证

本节用于验证订单交易履约链路是否能正常闭环，重点测试创建订单、支付回调、超时关闭、重复回调幂等和并发下单。测试目标不是覆盖所有边界场景，而是证明前文实现的订单状态机、库存锁定释放、支付回调幂等、超时关闭和并发控制能够正常工作。

测试前先准备一条商品库存数据：

```sql
-- 清理测试数据
DELETE FROM product_stock_flow;
DELETE FROM trade_pay_order;
DELETE FROM trade_order_item;
DELETE FROM trade_order;
DELETE FROM local_message;
DELETE FROM product_stock WHERE product_id = 10001;

-- 初始化测试商品
INSERT INTO product_stock (
    product_id,
    product_name,
    sale_price,
    total_stock,
    available_stock,
    locked_stock,
    sold_stock,
    sale_status
) VALUES (
    10001,
    'Java 后端高含金量项目课程',
    199.00,
    100,
    100,
    0,
    0,
    1
);
```

启动项目后，默认接口地址如下：

```text
创建订单接口：POST http://localhost:8080/api/orders
模拟支付回调：POST http://localhost:8080/api/pay/mock/callback
手动关闭超时订单：POST http://localhost:8080/api/orders/{orderNo}/close-timeout
```

### 创建订单接口测试

创建订单测试用于验证用户下单后是否能正确锁定库存、创建订单、创建订单明细和支付单。

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 1001,
    "productId": 10001,
    "quantity": 2
  }'
```

预期响应示例：

```json
{
  "orderNo": "ORD123456789001",
  "payNo": "PAY123456789002",
  "payAmount": 398.00,
  "payExpireTime": "2026-05-15T15:45:00"
}
```

验证订单数据：

```sql
SELECT
    order_no,
    user_id,
    total_amount,
    pay_amount,
    order_status,
    pay_expire_time
FROM trade_order
ORDER BY id DESC
LIMIT 1;
```

预期结果：

```text
order_status = 10
表示订单处于待支付状态
```

验证订单明细：

```sql
SELECT
    order_no,
    product_id,
    product_name,
    sale_price,
    quantity,
    total_amount
FROM trade_order_item
ORDER BY id DESC
LIMIT 1;
```

预期结果：

```text
product_id = 10001
quantity = 2
total_amount = 398.00
```

验证支付单：

```sql
SELECT
    pay_no,
    order_no,
    user_id,
    pay_amount,
    pay_status
FROM trade_pay_order
ORDER BY id DESC
LIMIT 1;
```

预期结果：

```text
pay_status = 10
表示支付单处于待支付状态
```

验证库存锁定：

```sql
SELECT
    product_id,
    total_stock,
    available_stock,
    locked_stock,
    sold_stock
FROM product_stock
WHERE product_id = 10001;
```

如果初始库存是 100，下单数量是 2，预期结果如下：

```text
total_stock = 100
available_stock = 98
locked_stock = 2
sold_stock = 0
```

验证库存流水：

```sql
SELECT
    order_no,
    product_id,
    flow_type,
    quantity,
    before_available_stock,
    after_available_stock,
    before_locked_stock,
    after_locked_stock
FROM product_stock_flow
ORDER BY id DESC
LIMIT 1;
```

预期结果：

```text
flow_type = 10
表示已生成锁定库存流水
```

### 模拟支付回调测试

模拟支付回调用于验证支付成功后，支付单是否变更为支付成功，订单是否变更为已支付，并通过 MQ 或补偿任务推进库存确认扣减和订单履约完成。

先查询上一节创建订单返回的 `payNo` 和 `payAmount`，然后调用模拟支付回调接口。

请求示例：

```bash
curl -X POST 'http://localhost:8080/api/pay/mock/callback' \
  -H 'Content-Type: application/json' \
  -d '{
    "payNo": "PAY123456789002",
    "thirdTradeNo": "MOCK202605150001",
    "payAmount": 398.00
  }'
```

验证支付单状态：

```sql
SELECT
    pay_no,
    order_no,
    pay_amount,
    pay_status,
    third_trade_no,
    callback_time,
    pay_success_time
FROM trade_pay_order
WHERE pay_no = 'PAY123456789002';
```

预期结果：

```text
pay_status = 20
third_trade_no = MOCK202605150001
callback_time 不为空
pay_success_time 不为空
```

验证订单状态：

```sql
SELECT
    order_no,
    order_status,
    pay_time,
    fulfill_time
FROM trade_order
WHERE order_no = 'ORD123456789001';
```

如果支付回调后只完成支付确认，预期结果为：

```text
order_status = 20
pay_time 不为空
fulfill_time 为空
```

如果本地消息已投递且 MQ 消费成功，预期结果为：

```text
order_status = 30
pay_time 不为空
fulfill_time 不为空
```

验证本地消息：

```sql
SELECT
    message_no,
    biz_no,
    message_type,
    message_status,
    retry_count,
    sent_time
FROM local_message
WHERE biz_no = 'ORD123456789001';
```

预期结果：

```text
message_type = PAY_SUCCESS
message_status = 20
表示支付成功消息已发送
```

验证库存确认扣减：

```sql
SELECT
    product_id,
    total_stock,
    available_stock,
    locked_stock,
    sold_stock
FROM product_stock
WHERE product_id = 10001;
```

如果订单数量为 2 且 MQ 消费完成，预期结果如下：

```text
total_stock = 100
available_stock = 98
locked_stock = 0
sold_stock = 2
```

验证库存确认流水：

```sql
SELECT
    order_no,
    product_id,
    flow_type,
    quantity
FROM product_stock_flow
WHERE order_no = 'ORD123456789001'
ORDER BY id ASC;
```

预期至少有两条流水：

```text
flow_type = 10 表示锁定库存
flow_type = 20 表示确认扣减库存
```

### 超时关闭订单测试

超时关闭订单用于验证待支付订单超过支付截止时间后，系统是否能自动关闭订单、关闭支付单并释放库存。

为了便于测试，可以先创建一笔订单，然后手动修改支付截止时间为过去时间。

创建订单：

```bash
curl -X POST 'http://localhost:8080/api/orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 1002,
    "productId": 10001,
    "quantity": 3
  }'
```

假设返回订单号为：

```text
ORD123456789003
```

手动修改订单支付截止时间：

```sql
UPDATE trade_order
SET pay_expire_time = DATE_SUB(NOW(), INTERVAL 1 MINUTE)
WHERE order_no = 'ORD123456789003'
  AND order_status = 10;
```

等待定时任务执行，或者手动调用关闭接口：

```bash
curl -X POST 'http://localhost:8080/api/orders/ORD123456789003/close-timeout'
```

验证订单状态：

```sql
SELECT
    order_no,
    order_status,
    close_time,
    close_reason
FROM trade_order
WHERE order_no = 'ORD123456789003';
```

预期结果：

```text
order_status = 40
close_time 不为空
close_reason = 支付超时自动关闭
```

验证支付单状态：

```sql
SELECT
    pay_no,
    order_no,
    pay_status,
    close_time
FROM trade_pay_order
WHERE order_no = 'ORD123456789003';
```

预期结果：

```text
pay_status = 30
close_time 不为空
```

验证库存释放：

```sql
SELECT
    product_id,
    total_stock,
    available_stock,
    locked_stock,
    sold_stock
FROM product_stock
WHERE product_id = 10001;
```

如果这笔订单数量为 3，关闭前锁定库存增加 3，关闭后应释放回可用库存。最终应满足：

```text
locked_stock 不包含该订单占用数量
available_stock 已恢复该订单占用数量
sold_stock 不增加
```

验证释放流水：

```sql
SELECT
    order_no,
    product_id,
    flow_type,
    quantity
FROM product_stock_flow
WHERE order_no = 'ORD123456789003'
ORDER BY id ASC;
```

预期结果：

```text
flow_type = 10 表示锁定库存
flow_type = 30 表示释放库存
```

### 重复回调幂等测试

重复回调测试用于验证同一支付单收到多次支付成功通知时，不会重复更新支付单、不会重复推进订单、不会重复扣减库存。

先创建一笔订单并完成第一次支付回调：

```bash
curl -X POST 'http://localhost:8080/api/orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 1003,
    "productId": 10001,
    "quantity": 1
  }'
```

假设返回：

```text
orderNo = ORD123456789004
payNo = PAY123456789005
payAmount = 199.00
```

第一次回调：

```bash
curl -X POST 'http://localhost:8080/api/pay/mock/callback' \
  -H 'Content-Type: application/json' \
  -d '{
    "payNo": "PAY123456789005",
    "thirdTradeNo": "MOCK202605150002",
    "payAmount": 199.00
  }'
```

第二次使用相同参数重复回调：

```bash
curl -X POST 'http://localhost:8080/api/pay/mock/callback' \
  -H 'Content-Type: application/json' \
  -d '{
    "payNo": "PAY123456789005",
    "thirdTradeNo": "MOCK202605150002",
    "payAmount": 199.00
  }'
```

验证支付单只有一笔成功状态：

```sql
SELECT
    pay_no,
    pay_status,
    third_trade_no,
    pay_success_time
FROM trade_pay_order
WHERE pay_no = 'PAY123456789005';
```

预期结果：

```text
pay_status = 20
状态保持支付成功
```

验证库存确认扣减流水不会重复生成：

```sql
SELECT
    flow_type,
    COUNT(*) AS total
FROM product_stock_flow
WHERE order_no = 'ORD123456789004'
GROUP BY flow_type;
```

预期结果：

```text
flow_type = 10，total = 1
flow_type = 20，total = 1
```

验证本地消息不会重复创建：

```sql
SELECT
    message_type,
    COUNT(*) AS total
FROM local_message
WHERE biz_no = 'ORD123456789004'
GROUP BY message_type;
```

预期结果：

```text
message_type = PAY_SUCCESS，total = 1
```

再验证库存数值没有重复扣减。假设这笔订单购买数量是 1，则该订单最多只会让 `sold_stock` 增加 1，不会因为重复回调增加 2 次。

```sql
SELECT
    available_stock,
    locked_stock,
    sold_stock
FROM product_stock
WHERE product_id = 10001;
```

重复回调通过的关键判断标准是：

```text
支付单状态不回退
订单状态不重复推进
库存 CONFIRM 流水只有一条
本地消息 PAY_SUCCESS 只有一条
库存不会重复扣减
```

### 并发下单测试

并发下单测试用于验证库存不会超卖。这里给出两种方式：一种使用 Java 单元测试，另一种使用 Apache Bench 或 JMeter 这类压测工具。

先准备一个小库存商品，便于观察是否超卖：

```sql
DELETE FROM product_stock_flow;
DELETE FROM trade_pay_order;
DELETE FROM trade_order_item;
DELETE FROM trade_order;
DELETE FROM local_message;
DELETE FROM product_stock WHERE product_id = 20001;

INSERT INTO product_stock (
    product_id,
    product_name,
    sale_price,
    total_stock,
    available_stock,
    locked_stock,
    sold_stock,
    sale_status
) VALUES (
    20001,
    '并发测试商品',
    99.00,
    10,
    10,
    0,
    0,
    1
);
```

下面的单元测试模拟 50 个线程同时购买同一个商品，每个线程购买 1 件。理论上最多只能成功 10 单。

文件位置：`src/test/java/io/github/atengk/trade/order/OrderConcurrentTest.java`

```java
package io.github.atengk.trade.order;

import cn.hutool.json.JSONUtil;
import io.github.atengk.trade.order.dto.OrderCreateDTO;
import io.github.atengk.trade.order.service.OrderService;
import io.github.atengk.trade.order.vo.OrderCreateVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单并发测试
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@SpringBootTest
public class OrderConcurrentTest {

    @Resource
    private OrderService orderService;

    /**
     * 并发创建订单，验证库存不会超卖
     *
     * @throws InterruptedException 线程等待异常
     */
    @Test
    void testConcurrentCreateOrder() throws InterruptedException {
        int threadCount = 50;
        Long productId = 20001L;

        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            long userId = 2000L + i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    OrderCreateDTO dto = new OrderCreateDTO();
                    dto.setUserId(userId);
                    dto.setProductId(productId);
                    dto.setQuantity(1);

                    OrderCreateVO result = orderService.createOrder(dto);
                    successCount.incrementAndGet();
                    log.info("并发下单成功，用户ID：{}，结果：{}", userId, JSONUtil.toJsonStr(result));
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.warn("并发下单失败，用户ID：{}，原因：{}", userId, e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        log.info("并发下单测试完成，成功数量：{}，失败数量：{}", successCount.get(), failCount.get());
    }
}
```

测试完成后验证订单数量：

```sql
SELECT COUNT(*) AS wait_pay_order_count
FROM trade_order o
JOIN trade_order_item i ON o.order_no = i.order_no
WHERE i.product_id = 20001
  AND o.order_status = 10;
```

预期结果：

```text
wait_pay_order_count <= 10
```

验证库存结果：

```sql
SELECT
    total_stock,
    available_stock,
    locked_stock,
    sold_stock
FROM product_stock
WHERE product_id = 20001;
```

预期结果：

```text
total_stock = 10
available_stock = 0
locked_stock = 10
sold_stock = 0
```

验证锁定流水数量：

```sql
SELECT
    flow_type,
    COUNT(*) AS total
FROM product_stock_flow
WHERE product_id = 20001
GROUP BY flow_type;
```

预期结果：

```text
flow_type = 10，total = 10
```

如果出现以下情况，说明并发控制有问题：

```text
available_stock 小于 0
locked_stock 大于 total_stock
成功订单数大于 10
LOCK 库存流水大于 10
```

使用命令行工具也可以进行简单并发测试。先保存请求体：

```bash
cat > create-order.json <<'EOF'
{
  "userId": 3001,
  "productId": 20001,
  "quantity": 1
}
EOF
```

使用 `ab` 执行并发请求：

```bash
ab -n 50 -c 20 \
  -p create-order.json \
  -T application/json \
  http://localhost:8080/api/orders
```

`-n 50` 表示总请求数为 50，`-c 20` 表示并发数为 20，`-p` 指定请求体文件，`-T` 指定请求内容类型。由于请求体里的 `userId` 固定，这种方式更适合测试库存并发扣减；如果要测试不同用户并发下单，建议使用 JMeter 或 Java 单元测试动态生成请求参数。

## 本案例小结

本案例完成了一条可运行、可验证、可扩展的订单交易履约核心链路。它没有追求完整商城能力，而是集中实现交易系统中最关键的后端能力：订单状态机、库存锁定与释放、支付回调幂等、超时关闭、MQ 最终一致性和异常补偿。

### 核心能力总结

本案例覆盖的核心能力如下：

| 能力          | 实现方式                                     |
| ------------- | -------------------------------------------- |
| 订单创建      | 创建订单主表、订单明细和支付单               |
| 防超卖        | 数据库条件更新 `available_stock >= quantity` |
| 库存锁定      | 可用库存减少，锁定库存增加                   |
| 支付回调幂等  | 支付单状态判断、条件更新、第三方流水唯一索引 |
| 订单状态机    | 待支付、已支付、履约完成、已关闭             |
| 超时关闭      | 定时扫描待支付超时订单                       |
| 库存释放      | 关闭订单后释放锁定库存                       |
| MQ 最终一致性 | 本地消息表、RabbitMQ、补偿投递               |
| 消费幂等      | 订单状态判断、库存流水唯一索引               |
| 异常补偿      | 扫描未发送消息、异常支付单和超时订单         |
| 并发控制      | Redisson 分布式锁 + 数据库条件更新           |
| 数据库兜底    | 唯一索引、状态条件更新、流水表幂等           |

整个链路的关键不是某一个技术点，而是多层防护组合：

```text
Redisson 锁降低并发冲突
数据库条件更新防止超卖
状态机防止非法流转
唯一索引防止重复数据
MQ 解耦履约动作
本地消息表防止消息丢失
定时任务补偿异常数据
```

最终形成的主链路如下：

```text
用户下单
-> 校验商品和库存
-> 锁定库存
-> 创建待支付订单
-> 创建待支付支付单
-> 支付成功回调
-> 支付单变更为支付成功
-> 订单变更为已支付
-> 写入支付成功本地消息
-> 投递 MQ
-> 消费者确认扣减库存
-> 订单变更为履约完成
```

异常关闭链路如下：

```text
用户下单
-> 锁定库存
-> 创建待支付订单
-> 到达支付超时时间
-> 定时任务关闭订单
-> 关闭支付单
-> 释放锁定库存
-> 记录释放流水
```

判断这个案例是否实现正确，可以看四个结果：

```text
库存不会扣成负数
重复回调不会重复扣库存
超时订单能释放库存
支付成功订单最终能履约完成
```

### 可继续扩展方向

当前案例已经覆盖订单交易履约主链路。后续可以基于这个基础继续扩展为更接近生产系统的版本。

一、接入真实支付渠道。

可以接入微信支付、支付宝或 Stripe，补充签名验签、证书配置、支付参数生成、异步通知验签、退款接口、支付状态主动查询等能力。

二、增加支付对账补偿。

可以新增支付渠道流水表，定时拉取第三方账单，与本地支付单做对账，处理本地成功三方失败、本地失败三方成功、金额不一致等异常。

三、支持多商品订单。

当前核心实现以单商品下单为主。后续可以支持多商品下单、批量锁库存、部分商品库存不足回滚、订单明细批量写入等能力。

四、引入 RabbitMQ 延迟消息。

超时关闭订单目前使用定时任务扫描。后续可以增加 RabbitMQ 延迟消息，下单后发送延迟关闭消息，到期后精准关闭订单。定时任务继续作为兜底补偿。

五、增加死信队列和后台重放。

支付成功消息消费失败后，可以进入死信队列。后台提供死信消息查询、重新投递、标记忽略等能力。

六、拆分为微服务架构。

可以拆分为：

```text
订单服务
库存服务
支付服务
履约服务
消息服务
```

拆分后需要处理跨服务调用、分布式事务、接口幂等、服务降级和链路追踪等问题。

七、引入 Seata 或 Saga。

如果业务强依赖强一致，可以考虑 Seata AT 模式。如果业务流程较长，例如支付、库存、权益、物流、通知多步骤协作，可以考虑 Saga 状态机。

八、增加优惠券和权益核销。

可以在订单创建前增加优惠券锁定，支付成功后核销优惠券，订单关闭后释放优惠券。它和库存锁定释放模型类似，但需要额外处理使用门槛和优惠金额计算。

九、增加退款和售后链路。

支付成功后如果用户申请退款，需要新增退款单、退款状态机、退款回调、库存回补、订单关闭或售后完成等能力。

十、增加可观测和审计。

可以接入 TraceId、MDC、操作审计、业务日志表、Prometheus、Grafana、SkyWalking、ELK，用于排查支付回调、MQ 消费、库存变更和订单状态异常。

这个案例最适合作为 Java 后端项目中的交易系统专项基础。掌握这个链路后，再扩展支付对账、账户流水、库存资源锁定、秒杀削峰、MQ 可靠消息等专题会更顺畅。