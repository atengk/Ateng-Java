# 接口幂等与防重复业务

## 功能概述

接口幂等用于解决“同一个业务请求被重复提交时，系统只能成功处理一次”的问题。常见原因包括用户连续点击按钮、网络超时后前端重试、网关重放请求、第三方回调重复通知、MQ 消息重复消费等。

本案例以“创建订单接口”为业务示例，采用 `Spring Boot 3 + Redis + AOP + 自定义注解 + Lua 脚本 + 数据库唯一索引` 实现接口幂等控制。核心目标不是单纯限制接口访问频率，而是识别“同一个业务请求”，并保证重复请求不会重复执行业务逻辑。

整体方案分为两层：

| 层级           | 作用                             |
| -------------- | -------------------------------- |
| Redis 幂等控制 | 在接口进入业务逻辑前拦截重复请求 |
| 数据库唯一约束 | 在数据落库层面做最终兜底         |

Redis 负责提前拦截，减少业务代码和数据库压力；数据库唯一索引负责保证极端情况下数据不重复，两者组合使用更稳妥。

### 业务场景

本案例模拟“用户提交订单”的业务场景。用户在前端点击“提交订单”后，请求后端创建订单。如果网络延迟、页面卡顿、按钮未禁用，或者前端自动重试，就可能导致同一个订单请求被多次提交。

如果后端没有做幂等控制，可能会出现以下问题：

| 场景                   | 可能后果                             |
| ---------------------- | ------------------------------------ |
| 用户连续点击提交按钮   | 创建多条重复订单                     |
| 接口超时后前端自动重试 | 重复创建订单或重复扣减库存           |
| 网关重放请求           | 重复写入业务数据                     |
| 支付回调重复通知       | 重复修改订单状态或重复发放权益       |
| MQ 消息重复消费        | 重复执行发货、积分、优惠券发放等逻辑 |

在创建订单场景中，推荐由调用方生成一个唯一请求号 `requestNo`。同一次业务操作即使发生重试，也必须使用同一个 `requestNo`。后端根据 `用户ID + requestNo` 判断是否为重复请求。

示例请求参数：

```json
{
  "requestNo": "REQ202605150001",
  "productId": 10001,
  "quantity": 2,
  "remark": "测试创建订单"
}
```

其中：

| 字段        | 说明                     |
| ----------- | ------------------------ |
| `requestNo` | 请求唯一号，用于幂等判断 |
| `productId` | 商品 ID                  |
| `quantity`  | 购买数量                 |
| `remark`    | 订单备注                 |

同一个用户使用同一个 `requestNo` 重复请求时，后端只允许第一次请求进入创建订单逻辑，后续请求直接返回“请勿重复提交”之类的提示。

### 实现目标

本案例要实现一个可以复用到多个接口上的幂等组件，而不是在每个 Controller 方法中手动写 Redis 判断逻辑。

最终希望达到以下效果：

```java
@Idempotent(
        key = "#request.requestNo",
        expireSeconds = 30,
        message = "订单正在处理中，请勿重复提交"
)
@PostMapping("/orders")
public Result<OrderCreateVO> createOrder(@RequestBody OrderCreateRequest request) {
    return Result.success(orderService.createOrder(request));
}
```

接口方法只需要添加 `@Idempotent` 注解，即可开启幂等控制。业务代码仍然只关注订单创建，不需要关心 Redis Key 生成、重复请求判断、Lua 脚本执行、异常返回等通用逻辑。

本功能需要实现以下目标：

| 目标           | 说明                                                        |
| -------------- | ----------------------------------------------------------- |
| 注解化接入     | 通过 `@Idempotent` 标记需要防重复提交的接口                 |
| 支持自定义 Key | 通过 SpEL 表达式指定业务幂等字段，例如 `#request.requestNo` |
| 并发安全       | 使用 Redis Lua 脚本保证判断和写入操作的原子性               |
| 自动过期       | 幂等 Key 设置过期时间，避免 Redis 中长期残留无效数据        |
| 统一异常处理   | 重复请求时返回统一错误信息                                  |
| 数据库兜底     | 使用唯一索引防止绕过接口层导致重复落库                      |
| 业务无侵入     | Controller 和 Service 中不需要手动编写重复提交判断代码      |

### 核心思路

接口幂等的关键是先定义“什么请求算重复请求”。在本案例中，同一个用户、同一个接口、同一个 `requestNo` 被认为是同一次业务请求。

核心处理流程如下：

```text
请求进入 Controller
    ↓
AOP 拦截带有 @Idempotent 注解的方法
    ↓
解析注解中的 key 表达式
    ↓
生成 Redis 幂等 Key
    ↓
执行 Lua 脚本
    ↓
Key 不存在：写入 Key，放行请求
Key 已存在：拦截请求，抛出重复提交异常
    ↓
业务方法创建订单
    ↓
数据库唯一索引兜底
```

Redis 幂等 Key 推荐格式：

```text
idempotent:{业务模块}:{接口标识}:{用户ID}:{请求唯一号}
```

创建订单接口示例：

```text
idempotent:order:create:10001:REQ202605150001
```

这种 Key 设计可以避免不同用户、不同接口、不同业务请求之间互相影响。

Redis 判断逻辑需要保证原子性，不能先 `get` 再 `set`，否则高并发下可能多个请求同时判断 Key 不存在，然后都进入业务逻辑。推荐使用 Lua 脚本一次性完成“判断是否存在 + 写入 Key + 设置过期时间”。

Lua 脚本逻辑如下：

```text
如果 Key 不存在：
    写入 Key 并设置过期时间
    返回成功
否则：
    返回失败
```

数据库层面仍然需要增加唯一索引，例如订单表中对 `user_id + request_no` 建唯一索引：

```sql
UNIQUE KEY uk_order_user_request (user_id, request_no)
```

最终整体防护方式如下：

| 防护点             | 作用                                 |
| ------------------ | ------------------------------------ |
| 前端按钮防重复点击 | 提升用户体验，但不能作为后端可靠防线 |
| Redis 幂等 Key     | 拦截短时间内的重复请求               |
| AOP 注解封装       | 降低业务代码侵入性                   |
| 数据库唯一索引     | 保证最终数据不重复                   |
| 业务状态判断       | 适合支付回调、订单状态流转等场景     |

后续实现会围绕 `@Idempotent + Redis Lua + AOP + MySQL 唯一索引` 展开，完成一个轻量、可复用、适合实际 Java 后端项目的接口幂等处理方案。

## 技术方案

本案例采用组合式幂等方案：接口层使用 Redis 快速拦截重复请求，业务层通过注解和 AOP 降低侵入性，数据层使用数据库唯一约束做最终兜底。这样既能提升性能，又能保证极端情况下的数据一致性。

### Redis 幂等 Key 方案

Redis 幂等 Key 方案适合处理短时间内的重复提交，例如用户连续点击、前端超时重试、网关重放请求等。

核心做法是：每次请求进入业务方法前，先根据用户、接口、业务请求号生成一个唯一 Redis Key。如果 Key 不存在，就写入 Redis 并放行；如果 Key 已存在，说明请求正在处理或已经提交过，直接拦截。

推荐 Key 格式如下：

```text
idempotent:{业务模块}:{接口标识}:{用户ID}:{请求唯一号}
```

创建订单接口示例：

```text
idempotent:order:create:10001:REQ202605150001
```

其中：

| 组成部分          | 示例       | 说明                          |
| ----------------- | ---------- | ----------------------------- |
| `idempotent`      | 固定前缀   | 便于 Redis 中统一识别幂等 Key |
| `order`           | 业务模块   | 表示订单模块                  |
| `create`          | 接口标识   | 表示创建订单接口              |
| `10001`           | 用户 ID    | 区分不同用户的请求            |
| `REQ202605150001` | 请求唯一号 | 判断是否为同一次业务请求      |

Redis 操作必须保证原子性，不能使用普通的 `get` 后 `set`，因为高并发下多个线程可能同时判断 Key 不存在。推荐使用 Lua 脚本完成原子判断和写入。

Lua 脚本核心逻辑如下：

```lua
-- KEYS[1]：幂等 Key
-- ARGV[1]：Key 的值
-- ARGV[2]：过期时间，单位秒
if redis.call('exists', KEYS[1]) == 0 then
    redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2])
    return 1
else
    return 0
end
```

该方案的关键点是：Redis 只是提前拦截重复请求，不应该作为唯一可靠防线。因为 Redis Key 可能过期、被误删，或者请求绕过接口层直接进入其他链路，所以数据库仍然需要唯一约束兜底。

### Token 防重复提交方案

Token 防重复提交方案适合传统表单提交、管理后台新增数据、页面按钮重复点击等场景。

基本流程是：前端进入页面或提交前，先向后端申请一个一次性 Token；提交表单时携带该 Token；后端校验 Token 是否存在，存在则删除并放行，不存在则说明重复提交或非法请求。

流程如下：

```text
前端请求 Token
    ↓
后端生成 Token 并写入 Redis
    ↓
前端提交表单时携带 Token
    ↓
后端校验并删除 Token
    ↓
删除成功：放行请求
删除失败：重复提交
```

Token 方案和 Redis 幂等 Key 方案的区别如下：

| 方案             | 适用场景                     | 特点                                     |
| ---------------- | ---------------------------- | ---------------------------------------- |
| Redis 幂等 Key   | API 重试、订单创建、支付回调 | 不一定需要提前申请 Token，适合接口级幂等 |
| Token 防重复提交 | 表单提交、后台新增、页面操作 | 需要先申请 Token，适合页面级防重复       |
| 数据库唯一约束   | 所有关键写入场景             | 最终兜底，防止重复数据落库               |

Token 方案的 Redis Key 可以这样设计：

```text
submit:token:{用户ID}:{token}
```

示例：

```text
submit:token:10001:8f1c7bb6d4c144f6a4c7f99f2deaa001
```

提交时使用 Redis 的 `delete` 操作删除 Token。第一次请求可以删除成功，后续重复请求因为 Token 已经不存在，会被直接拦截。

不过在创建订单这类业务中，更推荐使用 `requestNo` 作为幂等依据。因为订单创建可能发生接口超时和客户端重试，此时重复请求应该使用同一个 `requestNo`，后端根据 `requestNo` 判断是否为同一次业务操作。

### 数据库唯一约束兜底方案

Redis 和 Token 都属于接口层或缓存层防护，不能替代数据库层约束。对于关键业务数据，例如订单、支付流水、消息消费记录、用户权益发放记录，都应该增加数据库唯一约束。

以订单表为例，可以对 `user_id + request_no` 建唯一索引，保证同一个用户的同一个请求号只能创建一条订单。

订单表核心结构如下：

```sql
CREATE TABLE `biz_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `request_no` varchar(64) NOT NULL COMMENT '请求唯一号',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `quantity` int NOT NULL COMMENT '购买数量',
  `status` varchar(32) NOT NULL COMMENT '订单状态',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_user_request_no` (`user_id`, `request_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务订单表';
```

唯一索引的作用是防止重复数据真正写入数据库。即使出现 Redis Key 失效、接口绕过、服务重启、并发异常等情况，数据库仍然可以阻止重复订单落库。

在业务代码中，需要捕获唯一索引冲突异常，并转换为业务异常。例如：

```text
同一个 user_id + request_no 已存在
    ↓
数据库抛出唯一索引冲突异常
    ↓
Service 捕获或由统一异常处理识别
    ↓
返回“订单已提交，请勿重复操作”
```

实际项目中推荐同时使用两种手段：

| 防护方式       | 作用                               |
| -------------- | ---------------------------------- |
| Redis 幂等 Key | 提前拦截重复请求，降低数据库压力   |
| 数据库唯一索引 | 最终保证数据不重复                 |
| 业务状态机判断 | 防止订单、支付、退款等状态重复流转 |

### 注解与 AOP 拦截方案

为了避免每个接口都手动编写 Redis 幂等判断逻辑，可以使用自定义注解和 AOP 统一封装。

接口层使用方式如下：

```java
@Idempotent(
        key = "#request.requestNo",
        expireSeconds = 30,
        message = "订单正在处理中，请勿重复提交"
)
@PostMapping("/orders")
public Result<OrderCreateVO> createOrder(@RequestBody OrderCreateRequest request) {
    return Result.success(orderService.createOrder(request));
}
```

这里的核心是 `@Idempotent` 注解：

| 属性            | 说明                                          |
| --------------- | --------------------------------------------- |
| `key`           | SpEL 表达式，用于从请求参数中提取业务唯一字段 |
| `expireSeconds` | Redis 幂等 Key 过期时间                       |
| `message`       | 重复请求时返回的提示信息                      |

AOP 拦截流程如下：

```text
请求进入 Controller
    ↓
AOP 判断方法上是否有 @Idempotent
    ↓
解析注解中的 SpEL 表达式
    ↓
生成 Redis 幂等 Key
    ↓
执行 Lua 脚本写入 Key
    ↓
成功：执行业务方法
失败：抛出重复提交异常
```

这种方案的优势是业务代码干净，复用性强。后续只要其他接口也需要幂等控制，例如创建退款单、发放优惠券、提交审批单，只需要添加注解即可。

## 项目准备

本案例基于 Spring Boot 3 实现，使用 Redis 作为幂等 Key 存储，使用 AOP 做统一拦截，使用 Hutool 简化字符串、ID、时间等工具处理，使用 MyBatis-Plus 模拟订单落库。

### Maven 依赖配置

下面配置 Spring Boot Web、AOP、Redis、MyBatis-Plus、MySQL、Hutool、Lombok 等依赖，后续代码会基于这些依赖实现。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Web 接口开发依赖，提供 Controller、JSON 序列化等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- AOP 依赖，用于拦截 @Idempotent 注解标记的方法 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- Redis 依赖，用于存储接口幂等 Key 和一次性 Token -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- 参数校验依赖，用于校验 requestNo、productId、quantity 等请求字段 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- MyBatis-Plus，简化订单表 CRUD 操作 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动，用于连接业务数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool 工具包，用于字符串、ID、JSON、日期等常用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok，减少 getter、setter、构造方法等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 单元测试依赖，后续可用于并发幂等测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目已经集成了 Web、Redis、MyBatis-Plus，只需要补充缺失的 AOP、Validation、Hutool 依赖即可。

### Redis 配置

下面配置 Redis、MySQL 和 MyBatis-Plus 基础参数，确保项目能够连接 Redis 执行 Lua 脚本，并能连接 MySQL 完成订单落库。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: idempotent-demo

  data:
    redis:
      # Redis 服务地址
      host: 127.0.0.1
      # Redis 服务端口
      port: 6379
      # Redis 密码，没有密码可留空或删除该配置
      password:
      # Redis 数据库索引
      database: 0
      # Redis 连接超时时间
      timeout: 3s

  datasource:
    # MySQL 连接地址，根据本地环境修改库名、地址和参数
    url: jdbc:mysql://127.0.0.1:3306/idempotent_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    # 数据库用户名
    username: root
    # 数据库密码
    password: root
    # MySQL 驱动类
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    # 控制台打印 SQL，开发测试环境可开启，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 主键自增策略
      id-type: auto

logging:
  level:
    # 当前示例项目包日志级别
    io.github.atengk: info
```

Redis 本地启动可以使用 Docker，适合快速验证案例。

```bash
docker run -d \
  --name redis-idempotent \
  -p 6379:6379 \
  redis:7.2
```

该命令会启动一个 Redis 7.2 容器，并将容器的 `6379` 端口映射到本机 `6379` 端口。项目中的 `spring.data.redis.host` 和 `spring.data.redis.port` 保持默认即可连接。

MySQL 可以提前创建测试库：

```sql
CREATE DATABASE IF NOT EXISTS idempotent_demo
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

该数据库用于存放订单表，后续会通过 `biz_order` 表验证数据库唯一索引兜底效果。

### 工程目录结构

下面是本案例推荐的核心目录结构，保留幂等组件、订单业务、统一返回、异常处理等关键代码位置。

```text
src
└── main
    ├── java
    │   └── io
    │       └── github
    │           └── atengk
    │               └── idempotent
    │                   ├── IdempotentApplication.java
    │                   ├── common
    │                   │   ├── domain
    │                   │   │   └── Result.java
    │                   │   └── exception
    │                   │       ├── BizException.java
    │                   │       ├── IdempotentException.java
    │                   │       └── GlobalExceptionHandler.java
    │                   ├── framework
    │                   │   └── idempotent
    │                   │       ├── annotation
    │                   │       │   └── Idempotent.java
    │                   │       ├── aspect
    │                   │       │   └── IdempotentAspect.java
    │                   │       ├── enums
    │                   │       │   └── IdempotentTypeEnum.java
    │                   │       └── support
    │                   │           ├── IdempotentKeyBuilder.java
    │                   │           └── IdempotentRedisExecutor.java
    │                   └── order
    │                       ├── controller
    │                       │   └── OrderController.java
    │                       ├── service
    │                       │   ├── OrderService.java
    │                       │   └── impl
    │                       │       └── OrderServiceImpl.java
    │                       ├── mapper
    │                       │   └── OrderMapper.java
    │                       ├── entity
    │                       │   └── Order.java
    │                       ├── dto
    │                       │   └── OrderCreateRequest.java
    │                       └── vo
    │                           └── OrderCreateVO.java
    └── resources
        ├── application.yml
        └── mapper
            └── OrderMapper.xml
```

核心目录说明如下：

| 目录                   | 说明                             |
| ---------------------- | -------------------------------- |
| `common/domain`        | 统一接口返回对象                 |
| `common/exception`     | 业务异常、幂等异常、全局异常处理 |
| `framework/idempotent` | 幂等组件核心代码                 |
| `order/controller`     | 创建订单接口                     |
| `order/service`        | 订单业务逻辑                     |
| `order/mapper`         | MyBatis-Plus Mapper              |
| `order/entity`         | 订单实体                         |
| `order/dto`            | 请求参数对象                     |
| `order/vo`             | 接口返回对象                     |
| `resources/mapper`     | XML Mapper 文件，可选            |

后续核心实现会优先完成 `framework/idempotent` 下的通用幂等组件，然后再实现 `order` 模块中的创建订单接口，用实际业务接口验证重复提交拦截效果。

## 核心实现

本章节实现接口幂等的核心组件，包括注解定义、幂等类型、Key 生成、Redis Lua 原子校验、AOP 拦截、异常定义和统一异常返回。代码放在 `framework/idempotent` 和 `common/exception` 目录下，后续业务接口只需要添加 `@Idempotent` 注解即可使用。

### 幂等注解定义

`@Idempotent` 用于标记需要做幂等控制的接口方法。这里支持通过 SpEL 表达式从方法参数中提取业务唯一字段，例如 `#request.requestNo`。

文件位置：`src/main/java/io/github/atengk/idempotent/framework/idempotent/annotation/Idempotent.java`

```java
package io.github.atengk.idempotent.framework.idempotent.annotation;

import io.github.atengk.idempotent.framework.idempotent.enums.IdempotentTypeEnum;

import java.lang.annotation.*;

/**
 * 接口幂等注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等类型
     */
    IdempotentTypeEnum type() default IdempotentTypeEnum.SPEL;

    /**
     * 幂等 Key 表达式，支持 SpEL，例如：#request.requestNo
     */
    String key() default "";

    /**
     * 业务模块
     */
    String module() default "common";

    /**
     * 业务场景
     */
    String scene() default "default";

    /**
     * Key 过期时间，单位秒
     */
    long expireSeconds() default 30;

    /**
     * 重复提交提示信息
     */
    String message() default "请求正在处理中，请勿重复提交";
}
```

使用示例：

```java
@Idempotent(
        key = "#request.requestNo",
        module = "order",
        scene = "create",
        expireSeconds = 30,
        message = "订单正在处理中，请勿重复提交"
)
```

### 幂等类型枚举

幂等类型用于区分不同的幂等处理方式。本案例重点使用 `SPEL` 方案，通过业务请求号生成幂等 Key；同时保留 `TOKEN` 类型，方便后续扩展一次性 Token 防重复提交。

文件位置：`src/main/java/io/github/atengk/idempotent/framework/idempotent/enums/IdempotentTypeEnum.java`

```java
package io.github.atengk.idempotent.framework.idempotent.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 幂等类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum IdempotentTypeEnum {

    /**
     * 使用 SpEL 表达式从请求参数中提取业务唯一值
     */
    SPEL("SPEL", "SpEL 表达式幂等"),

    /**
     * 使用一次性 Token 做防重复提交
     */
    TOKEN("TOKEN", "Token 防重复提交");

    private final String code;

    private final String desc;
}
```

### 幂等 Key 生成策略

幂等 Key 生成器负责把注解参数、当前接口路径、用户 ID、业务请求号组合成 Redis Key。这里默认从请求头 `X-User-Id` 中获取用户 ID，实际项目中可以改为从 Sa-Token、Spring Security 或网关上下文中获取。

文件位置：`src/main/java/io/github/atengk/idempotent/framework/idempotent/support/IdempotentKeyBuilder.java`

```java
package io.github.atengk.idempotent.framework.idempotent.support;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.idempotent.framework.idempotent.annotation.Idempotent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 幂等 Key 构建器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
@RequiredArgsConstructor
public class IdempotentKeyBuilder {

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent";

    private static final String USER_ID_HEADER = "X-User-Id";

    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public String buildKey(Method method, Object[] args, Idempotent idempotent, HttpServletRequest request) {
        String userId = getUserId(request);
        String businessKey = parseBusinessKey(method, args, idempotent.key());
        String uri = StrUtil.replace(request.getRequestURI(), "/", ":");

        return StrUtil.format(
                "{}:{}:{}:{}:{}:{}",
                IDEMPOTENT_KEY_PREFIX,
                idempotent.module(),
                idempotent.scene(),
                userId,
                uri,
                businessKey
        );
    }

    private String getUserId(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (StrUtil.isBlank(userId)) {
            return "anonymous";
        }
        return userId;
    }

    private String parseBusinessKey(Method method, Object[] args, String keyExpression) {
        if (StrUtil.isBlank(keyExpression)) {
            return "default";
        }

        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null,
                method,
                args,
                parameterNameDiscoverer
        );

        Expression expression = spelExpressionParser.parseExpression(keyExpression);
        Object value = expression.getValue(context);

        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            return "empty";
        }

        return String.valueOf(value);
    }
}
```

生成后的 Redis Key 示例：

```text
idempotent:order:create:10001::api:orders:REQ202605150001
```

其中 `10001` 来自请求头 `X-User-Id`，`REQ202605150001` 来自请求参数 `request.requestNo`。

### Redis Lua 原子校验脚本

Redis 执行器负责通过 Lua 脚本完成“判断 Key 是否存在 + 写入 Key + 设置过期时间”的原子操作，避免高并发下多个请求同时通过校验。

文件位置：`src/main/java/io/github/atengk/idempotent/framework/idempotent/support/IdempotentRedisExecutor.java`

```java
package io.github.atengk.idempotent.framework.idempotent.support;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Redis 幂等执行器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentRedisExecutor {

    private final RedisTemplate<String, String> redisTemplate;

    private static final DefaultRedisScript<Long> IDEMPOTENT_SCRIPT;

    static {
        String script = """
                if redis.call('exists', KEYS[1]) == 0 then
                    redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2])
                    return 1
                else
                    return 0
                end
                """;
        IDEMPOTENT_SCRIPT = new DefaultRedisScript<>(script, Long.class);
    }

    public boolean tryPass(String key, long expireSeconds) {
        String value = StrUtil.format("processing:{}", DateUtil.now());

        Long result = redisTemplate.execute(
                IDEMPOTENT_SCRIPT,
                Collections.singletonList(key),
                value,
                String.valueOf(expireSeconds)
        );

        boolean passed = Long.valueOf(1L).equals(result);
        if (!passed) {
            log.warn("接口幂等校验未通过，key={}", key);
        }

        return passed;
    }
}
```

Lua 脚本核心逻辑：

```lua
if redis.call('exists', KEYS[1]) == 0 then
    redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2])
    return 1
else
    return 0
end
```

这段脚本在 Redis 内部一次性执行，不会出现普通 `get` 后 `set` 带来的并发窗口问题。

### AOP 切面拦截处理

AOP 切面用于拦截所有带有 `@Idempotent` 注解的方法，在业务方法执行前完成幂等校验。校验成功则放行业务方法，校验失败则抛出幂等异常。

文件位置：`src/main/java/io/github/atengk/idempotent/framework/idempotent/aspect/IdempotentAspect.java`

```java
package io.github.atengk.idempotent.framework.idempotent.aspect;

import io.github.atengk.idempotent.common.exception.IdempotentException;
import io.github.atengk.idempotent.framework.idempotent.annotation.Idempotent;
import io.github.atengk.idempotent.framework.idempotent.support.IdempotentKeyBuilder;
import io.github.atengk.idempotent.framework.idempotent.support.IdempotentRedisExecutor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 接口幂等切面
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final IdempotentKeyBuilder idempotentKeyBuilder;

    private final IdempotentRedisExecutor idempotentRedisExecutor;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = getRequest();
        Method method = getMethod(joinPoint);

        String idempotentKey = idempotentKeyBuilder.buildKey(
                method,
                joinPoint.getArgs(),
                idempotent,
                request
        );

        boolean passed = idempotentRedisExecutor.tryPass(idempotentKey, idempotent.expireSeconds());
        if (!passed) {
            throw new IdempotentException(idempotent.message());
        }

        log.info("接口幂等校验通过，key={}", idempotentKey);
        return joinPoint.proceed();
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IdempotentException("当前请求上下文不存在，无法执行幂等校验");
        }
        return attributes.getRequest();
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }
}
```

执行流程如下：

```text
请求进入 Controller
    ↓
AOP 拦截 @Idempotent
    ↓
生成 Redis 幂等 Key
    ↓
执行 Redis Lua 脚本
    ↓
成功：joinPoint.proceed()
失败：抛出 IdempotentException
```

当前实现采用“请求进入后立即写入 Key”的方式。Key 会在 `expireSeconds` 后自动过期，适合防止短时间重复提交。对于需要长期幂等的业务，例如支付回调、消息消费，应额外使用业务状态和数据库唯一索引兜底。

### 幂等异常定义

幂等异常用于表示接口重复提交。这里先定义一个通用业务异常 `BizException`，再定义 `IdempotentException` 继承它，方便统一异常处理。

文件位置：`src/main/java/io/github/atengk/idempotent/common/exception/BizException.java`

```java
package io.github.atengk.idempotent.common.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

文件位置：`src/main/java/io/github/atengk/idempotent/common/exception/IdempotentException.java`

```java
package io.github.atengk.idempotent.common.exception;

/**
 * 幂等异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class IdempotentException extends BizException {

    public IdempotentException(String message) {
        super(409, message);
    }
}
```

这里使用 `409` 表示请求冲突，适合表达“重复提交、重复处理、资源状态冲突”等业务场景。

### 统一异常返回处理

统一异常处理负责把 `IdempotentException` 转换成固定格式的接口响应，避免把异常堆栈直接暴露给前端。

先定义统一返回对象。

文件位置：`src/main/java/io/github/atengk/idempotent/common/domain/Result.java`

```java
package io.github.atengk.idempotent.common.domain;

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

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
```

然后定义全局异常处理器。

文件位置：`src/main/java/io/github/atengk/idempotent/common/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.idempotent.common.exception;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.idempotent.common.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
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

    @ExceptionHandler(IdempotentException.class)
    public Result<Void> handleIdempotentException(IdempotentException exception) {
        log.warn("重复提交请求，message={}", exception.getMessage());
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException exception) {
        log.warn("业务异常，code={}，message={}", exception.getCode(), exception.getMessage());
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException exception) {
        log.warn("数据库唯一约束冲突，message={}", exception.getMessage());
        return Result.fail(409, "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> StrUtil.format("{} {}", fieldError.getField(), fieldError.getDefaultMessage()))
                .orElse("参数校验失败");

        log.warn("请求参数校验失败，message={}", message);
        return Result.fail(400, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> StrUtil.format("{} {}", fieldError.getField(), fieldError.getDefaultMessage()))
                .orElse("参数绑定失败");

        log.warn("请求参数绑定失败，message={}", message);
        return Result.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.fail(500, "系统异常，请稍后重试");
    }
}
```

重复提交时，接口返回示例：

```json
{
  "code": 409,
  "message": "订单正在处理中，请勿重复提交",
  "data": null
}
```

数据库唯一索引冲突时，接口返回示例：

```json
{
  "code": 409,
  "message": "数据已存在，请勿重复提交",
  "data": null
}
```

到这里，幂等组件的核心能力已经完成。后续业务接口只需要添加 `@Idempotent` 注解，并在数据库层增加唯一索引，即可实现接口层拦截和数据层兜底的双重防重复处理。

## 业务接口案例

本案例使用“创建订单”作为接口幂等的落地示例。接口层通过 `@Idempotent` 拦截重复请求，业务层正常创建订单，数据库层通过 `user_id + request_no` 唯一索引做兜底。

先创建订单表，保证数据库层不会重复落库。

```sql
CREATE TABLE `biz_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `request_no` varchar(64) NOT NULL COMMENT '请求唯一号',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `quantity` int NOT NULL COMMENT '购买数量',
  `status` varchar(32) NOT NULL COMMENT '订单状态',
  `remark` varchar(255) DEFAULT NULL COMMENT '订单备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_user_request_no` (`user_id`, `request_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务订单表';
```

### 创建订单接口

创建订单接口使用 `@Idempotent` 注解声明幂等规则。这里使用 `request.requestNo` 作为业务幂等字段，同一个用户使用相同 `requestNo` 重复提交时，只允许第一次请求进入业务方法。

文件位置：`src/main/java/io/github/atengk/idempotent/order/controller/OrderController.java`

```java
package io.github.atengk.idempotent.order.controller;

import io.github.atengk.idempotent.common.domain.Result;
import io.github.atengk.idempotent.framework.idempotent.annotation.Idempotent;
import io.github.atengk.idempotent.order.dto.OrderCreateRequest;
import io.github.atengk.idempotent.order.service.OrderService;
import io.github.atengk.idempotent.order.vo.OrderCreateVO;
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
     * @param request 创建订单请求参数
     * @return 订单创建结果
     */
    @Idempotent(
            key = "#request.requestNo",
            module = "order",
            scene = "create",
            expireSeconds = 30,
            message = "订单正在处理中，请勿重复提交"
    )
    @PostMapping
    public Result<OrderCreateVO> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.createOrder(request));
    }
}
```

接口说明：

| 项目         | 内容                           |
| ------------ | ------------------------------ |
| 请求地址     | `/api/orders`                  |
| 请求方式     | `POST`                         |
| 幂等字段     | `requestNo`                    |
| 用户维度     | 请求头 `X-User-Id`             |
| 幂等过期时间 | 30 秒                          |
| 重复提交提示 | `订单正在处理中，请勿重复提交` |

请求示例：

```json
{
  "requestNo": "REQ202605150001",
  "productId": 10001,
  "quantity": 2,
  "remark": "测试创建订单"
}
```

### 防重复提交接口

防重复提交接口用于演示 Token 方案。该方案适合后台管理页面、表单提交、新增数据按钮等场景。前端先获取一次性 Token，提交表单时携带 Token，后端校验成功后立即删除 Token。

文件位置：`src/main/java/io/github/atengk/idempotent/submit/controller/SubmitTokenController.java`

```java
package io.github.atengk.idempotent.submit.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.idempotent.common.domain.Result;
import io.github.atengk.idempotent.common.exception.IdempotentException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * 防重复提交 Token 接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequestMapping("/api/submit-token")
@RequiredArgsConstructor
public class SubmitTokenController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private static final String TOKEN_HEADER = "X-Submit-Token";

    private static final String TOKEN_KEY_PREFIX = "submit:token";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 获取一次性提交 Token
     *
     * @param request HTTP 请求对象
     * @return 一次性 Token
     */
    @GetMapping
    public Result<String> createToken(HttpServletRequest request) {
        String userId = getUserId(request);
        String token = IdUtil.fastSimpleUUID();
        String tokenKey = buildTokenKey(userId, token);

        redisTemplate.opsForValue().set(tokenKey, "1", Duration.ofMinutes(5));
        return Result.success(token);
    }

    /**
     * Token 防重复提交示例接口
     *
     * @param request HTTP 请求对象
     * @return 提交结果
     */
    @PostMapping("/demo")
    public Result<String> submitDemo(HttpServletRequest request) {
        String userId = getUserId(request);
        String token = request.getHeader(TOKEN_HEADER);

        if (StrUtil.isBlank(token)) {
            throw new IdempotentException("提交 Token 不能为空");
        }

        String tokenKey = buildTokenKey(userId, token);
        Boolean deleted = redisTemplate.delete(tokenKey);
        if (!Boolean.TRUE.equals(deleted)) {
            throw new IdempotentException("请勿重复提交");
        }

        return Result.success("提交成功");
    }

    private String getUserId(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        return StrUtil.blankToDefault(userId, "anonymous");
    }

    private String buildTokenKey(String userId, String token) {
        return StrUtil.format("{}:{}:{}", TOKEN_KEY_PREFIX, userId, token);
    }
}
```

Token 方案调用流程：

```text
GET /api/submit-token 获取 Token
    ↓
POST /api/submit-token/demo 携带 X-Submit-Token
    ↓
第一次提交：删除 Token 成功，放行
    ↓
重复提交：Token 不存在，直接拦截
```

`@Idempotent` 更适合订单创建、支付回调、接口重试等 API 幂等场景；Token 方案更适合页面表单的一次性提交场景。

### 请求参数对象

创建订单请求对象用于接收前端提交的数据。`requestNo` 是本案例的核心幂等字段，不能为空。

文件位置：`src/main/java/io/github/atengk/idempotent/order/dto/OrderCreateRequest.java`

```java
package io.github.atengk.idempotent.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建订单请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderCreateRequest {

    /**
     * 请求唯一号，同一次业务重试必须保持一致
     */
    @NotBlank(message = "请求唯一号不能为空")
    private String requestNo;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    /**
     * 订单备注
     */
    private String remark;
}
```

创建订单返回对象用于返回订单创建后的核心信息。

文件位置：`src/main/java/io/github/atengk/idempotent/order/vo/OrderCreateVO.java`

```java
package io.github.atengk.idempotent.order.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 创建订单返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class OrderCreateVO {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 请求唯一号
     */
    private String requestNo;

    /**
     * 订单状态
     */
    private String status;
}
```

订单实体映射 `biz_order` 表。

文件位置：`src/main/java/io/github/atengk/idempotent/order/entity/Order.java`

```java
package io.github.atengk.idempotent.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("biz_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private String requestNo;

    private Long productId;

    private Integer quantity;

    private String status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

订单 Mapper 使用 MyBatis-Plus 的 `BaseMapper` 即可完成基础新增操作。

文件位置：`src/main/java/io/github/atengk/idempotent/order/mapper/OrderMapper.java`

```java
package io.github.atengk.idempotent.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.idempotent.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
```

### 业务 Service 实现

业务 Service 负责创建订单。这里为了突出幂等核心逻辑，示例中不展开库存扣减、价格计算、优惠券核销等复杂业务。

文件位置：`src/main/java/io/github/atengk/idempotent/order/service/OrderService.java`

```java
package io.github.atengk.idempotent.order.service;

import io.github.atengk.idempotent.order.dto.OrderCreateRequest;
import io.github.atengk.idempotent.order.vo.OrderCreateVO;

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
     * @param request 创建订单请求参数
     * @return 订单创建结果
     */
    OrderCreateVO createOrder(OrderCreateRequest request);
}
```

Service 实现中使用 Hutool 生成订单号，并从请求头中读取用户 ID。实际项目中可以替换为 Sa-Token、Spring Security 或网关传入的用户上下文。

文件位置：`src/main/java/io/github/atengk/idempotent/order/service/impl/OrderServiceImpl.java`

```java
package io.github.atengk.idempotent.order.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.idempotent.common.exception.BizException;
import io.github.atengk.idempotent.order.dto.OrderCreateRequest;
import io.github.atengk.idempotent.order.entity.Order;
import io.github.atengk.idempotent.order.mapper.OrderMapper;
import io.github.atengk.idempotent.order.service.OrderService;
import io.github.atengk.idempotent.order.vo.OrderCreateVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

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

    private static final String USER_ID_HEADER = "X-User-Id";

    private static final String ORDER_STATUS_CREATED = "CREATED";

    private final OrderMapper orderMapper;

    /**
     * 创建订单
     *
     * @param request 创建订单请求参数
     * @return 订单创建结果
     */
    @Override
    public OrderCreateVO createOrder(OrderCreateRequest request) {
        Long userId = getCurrentUserId();
        String orderNo = buildOrderNo();
        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setRequestNo(request.getRequestNo());
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setStatus(ORDER_STATUS_CREATED);
        order.setRemark(request.getRemark());
        order.setCreateTime(now);
        order.setUpdateTime(now);

        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException exception) {
            log.warn("订单重复提交，userId={}，requestNo={}", userId, request.getRequestNo());
            throw new BizException(409, "订单已提交，请勿重复操作");
        }

        log.info("订单创建成功，userId={}，orderNo={}，requestNo={}", userId, orderNo, request.getRequestNo());

        return OrderCreateVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .requestNo(order.getRequestNo())
                .status(order.getStatus())
                .build();
    }

    private Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BizException("当前请求上下文不存在");
        }

        HttpServletRequest request = attributes.getRequest();
        String userId = request.getHeader(USER_ID_HEADER);
        if (StrUtil.isBlank(userId)) {
            throw new BizException(401, "用户ID不能为空");
        }

        return Long.valueOf(userId);
    }

    private String buildOrderNo() {
        return StrUtil.format("ORD{}", IdUtil.getSnowflakeNextIdStr());
    }
}
```

这里有两层防重复：

| 防护层               | 说明                                     |
| -------------------- | ---------------------------------------- |
| `@Idempotent`        | 重复请求进入业务方法前被 Redis 拦截      |
| `uk_user_request_no` | Redis 失效或绕过时，由数据库唯一索引兜底 |

## 接口测试

本章节使用 `curl` 验证接口幂等效果。测试前确保 Redis、MySQL 和 Spring Boot 项目已经启动。

### 正常请求验证

第一次请求使用新的 `requestNo`，应该创建订单成功。

```bash
curl -X POST 'http://localhost:8080/api/orders' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 10001' \
  -d '{
    "requestNo": "REQ202605150001",
    "productId": 10001,
    "quantity": 2,
    "remark": "正常创建订单"
  }'
```

正常返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "orderNo": "ORD1991234567890000001",
    "requestNo": "REQ202605150001",
    "status": "CREATED"
  }
}
```

数据库中应该新增一条订单记录：

```sql
SELECT id, order_no, user_id, request_no, product_id, quantity, status
FROM biz_order
WHERE user_id = 10001
  AND request_no = 'REQ202605150001';
```

### 重复请求验证

在 30 秒内使用同一个 `X-User-Id` 和同一个 `requestNo` 再次请求，应该被 Redis 幂等组件拦截。

```bash
curl -X POST 'http://localhost:8080/api/orders' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 10001' \
  -d '{
    "requestNo": "REQ202605150001",
    "productId": 10001,
    "quantity": 2,
    "remark": "重复创建订单"
  }'
```

重复请求返回示例：

```json
{
  "code": 409,
  "message": "订单正在处理中，请勿重复提交",
  "data": null
}
```

如果 Redis Key 已过期，再次使用同一个 `requestNo` 请求，可能会进入业务方法，但数据库唯一索引会兜底拦截。

返回示例：

```json
{
  "code": 409,
  "message": "订单已提交，请勿重复操作",
  "data": null
}
```

这说明 Redis 和数据库唯一索引都生效了。

### 并发请求验证

可以使用 `xargs` 模拟 10 个并发请求，同时提交相同的 `requestNo`。预期结果是只有一个请求成功，其余请求返回重复提交。

```bash
seq 1 10 | xargs -I {} -P 10 curl -s -X POST 'http://localhost:8080/api/orders' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 10002' \
  -d '{
    "requestNo": "REQ202605150002",
    "productId": 10001,
    "quantity": 1,
    "remark": "并发创建订单"
  }'
```

该命令中：

| 参数               | 说明                    |
| ------------------ | ----------------------- |
| `seq 1 10`         | 生成 10 次请求          |
| `xargs -P 10`      | 并发执行 10 个请求      |
| `X-User-Id: 10002` | 指定测试用户            |
| `requestNo`        | 10 个请求使用相同请求号 |

数据库验证：

```sql
SELECT COUNT(*) AS order_count
FROM biz_order
WHERE user_id = 10002
  AND request_no = 'REQ202605150002';
```

结果应该为：

```text
1
```

Token 防重复提交测试也可以这样验证。

先获取 Token：

```bash
TOKEN=$(curl -s -X GET 'http://localhost:8080/api/submit-token' \
  -H 'X-User-Id: 10001' | sed -n 's/.*"data":"\([^"]*\)".*/\1/p')

echo "$TOKEN"
```

第一次提交：

```bash
curl -X POST 'http://localhost:8080/api/submit-token/demo' \
  -H 'X-User-Id: 10001' \
  -H "X-Submit-Token: $TOKEN"
```

第二次提交：

```bash
curl -X POST 'http://localhost:8080/api/submit-token/demo' \
  -H 'X-User-Id: 10001' \
  -H "X-Submit-Token: $TOKEN"
```

第一次应该返回成功，第二次应该返回：

```json
{
  "code": 409,
  "message": "请勿重复提交",
  "data": null
}
```

### Redis Key 结果验证

订单接口请求后，可以通过 `redis-cli` 查看幂等 Key。

```bash
redis-cli keys 'idempotent:order:create:*'
```

结果示例：

```text
1) "idempotent:order:create:10001::api:orders:REQ202605150001"
```

查看 Key 剩余过期时间：

```bash
redis-cli ttl 'idempotent:order:create:10001::api:orders:REQ202605150001'
```

结果示例：

```text
25
```

查看 Key 内容：

```bash
redis-cli get 'idempotent:order:create:10001::api:orders:REQ202605150001'
```

结果示例：

```text
processing:2026-05-15 10:30:01
```

Token 方案可以查看：

```bash
redis-cli keys 'submit:token:*'
```

如果 Token 已经被提交接口消费，Key 会被删除，此时查询不到对应 Token Key。

## 实战补充

本案例实现的是轻量通用方案，适合多数后台管理系统、订单系统和业务写接口。真实项目中还需要根据业务语义调整幂等粒度、过期时间和兜底策略。

### 幂等 Key 过期时间设置

幂等 Key 的过期时间不能随意设置。时间太短，业务还没处理完 Key 就过期，重复请求可能再次进入业务逻辑；时间太长，会导致用户短时间内无法重新发起正常请求。

推荐设置如下：

| 场景             | 推荐过期时间     | 说明                     |
| ---------------- | ---------------- | ------------------------ |
| 普通表单提交     | 10 秒到 30 秒    | 防止连续点击             |
| 创建订单         | 30 秒到 120 秒   | 覆盖接口超时和短时间重试 |
| 支付回调         | 5 分钟到 30 分钟 | 重复通知可能间隔较长     |
| MQ 消息消费      | 30 分钟以上      | 更推荐使用消费记录表兜底 |
| 导入、批处理任务 | 任务预计耗时以上 | 避免任务未结束重复触发   |

创建订单示例中使用 30 秒：

```java
@Idempotent(
        key = "#request.requestNo",
        module = "order",
        scene = "create",
        expireSeconds = 30,
        message = "订单正在处理中，请勿重复提交"
)
```

如果订单接口存在库存锁定、优惠券核销、远程支付预下单等耗时操作，可以适当调大到 60 秒或 120 秒。

### 用户维度与接口维度隔离

幂等 Key 必须包含用户维度和接口维度，否则容易误伤正常请求。

不推荐：

```text
idempotent:REQ202605150001
```

这种 Key 只有请求号，如果两个用户刚好传了相同 `requestNo`，就会互相影响。

推荐：

```text
idempotent:order:create:10001:/api/orders:REQ202605150001
```

推荐至少包含以下信息：

| 维度         | 是否推荐 | 说明                               |
| ------------ | -------- | ---------------------------------- |
| 业务模块     | 推荐     | 例如 `order`、`pay`、`coupon`      |
| 业务场景     | 推荐     | 例如 `create`、`callback`、`grant` |
| 用户 ID      | 推荐     | 区分不同用户                       |
| 接口路径     | 推荐     | 区分不同接口                       |
| 业务请求号   | 必须     | 判断同一次业务请求                 |
| 请求参数摘要 | 可选     | 适合没有 requestNo 的接口          |

如果接口没有明确的 `requestNo`，可以基于核心请求参数生成摘要，例如 `userId + productId + quantity`，但这种方式容易因为参数变化导致幂等判断不稳定。关键写接口仍然推荐显式传入 `requestNo`。

### 与数据库唯一索引配合

Redis 幂等 Key 只能做接口层拦截，不能替代数据库唯一索引。真正关键的数据必须由数据库唯一约束做最终兜底。

订单场景推荐唯一索引：

```sql
UNIQUE KEY `uk_user_request_no` (`user_id`, `request_no`)
```

支付流水推荐唯一索引：

```sql
UNIQUE KEY `uk_pay_trade_no` (`trade_no`)
```

第三方回调推荐唯一索引：

```sql
UNIQUE KEY `uk_channel_notify_no` (`channel_code`, `notify_no`)
```

MQ 消费记录推荐唯一索引：

```sql
UNIQUE KEY `uk_consumer_message` (`consumer_group`, `message_id`)
```

常见组合方式：

| 场景       | Redis 幂等 Key                  | 数据库唯一约束                     |
| ---------- | ------------------------------- | ---------------------------------- |
| 创建订单   | `userId + requestNo`            | `user_id + request_no`             |
| 支付回调   | `channel + tradeNo`             | `trade_no`                         |
| 优惠券发放 | `userId + couponId + requestNo` | `user_id + coupon_id + request_no` |
| MQ 消费    | `consumerGroup + messageId`     | `consumer_group + message_id`      |

原则是：Redis 负责挡住大部分重复流量，数据库负责保证最终一致性。

### 常见问题与处理方式

下面整理接口幂等在实际项目中最常见的问题和处理方式。

| 问题                            | 原因                    | 处理方式                                 |
| ------------------------------- | ----------------------- | ---------------------------------------- |
| 重复请求仍然进入业务方法        | 幂等 Key 过期时间太短   | 调大 `expireSeconds`，并加数据库唯一索引 |
| 不同用户互相拦截                | Key 没有包含用户维度    | Key 中加入 `userId` 或租户 ID            |
| 不同接口互相拦截                | Key 没有包含接口维度    | Key 中加入 `module`、`scene`、接口 URI   |
| Redis 生效但数据库仍重复        | 数据库没有唯一约束      | 关键业务表必须增加唯一索引               |
| 接口失败后无法重新提交          | Key 在业务异常后仍存在  | 根据业务决定是否失败后删除 Key           |
| 支付回调用短期 Redis Key 不可靠 | 回调重复间隔可能很长    | 使用业务单号、状态机和数据库唯一索引     |
| MQ 消息重复消费                 | MQ 天然可能至少投递一次 | 使用消息消费记录表和唯一索引             |
| 没有 requestNo 怎么办           | 请求缺少业务唯一标识    | 前端或调用方生成 requestNo，后端强制校验 |

对于“业务失败后是否删除幂等 Key”，需要按业务类型决定：

| 业务类型               | 失败后是否删除 Key | 建议                             |
| ---------------------- | ------------------ | -------------------------------- |
| 参数校验失败           | 可以删除           | 请求未进入核心业务               |
| 库存不足               | 可以删除或保留     | 看是否允许用户立即重试           |
| 订单创建中远程调用超时 | 不建议立即删除     | 避免实际成功但前端重复提交       |
| 支付回调处理失败       | 不建议只依赖 Redis | 使用状态机和回调记录表           |
| MQ 消费失败            | 不建议简单删除     | 交给 MQ 重试机制和消费记录表处理 |

本案例当前实现采用“写入后等待自动过期”的方式，适合创建订单这类短时间防重复提交场景。如果需要“业务异常后自动删除 Key”，可以在 AOP 的 `catch` 分支中根据注解配置扩展一个 `deleteOnException` 属性。