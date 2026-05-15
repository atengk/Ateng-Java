# 第三方平台接口对接与签名验签

本文基于“第三方平台接口对接与签名验签”场景展开，覆盖第三方平台参数配置、请求签名、接口调用、异常重试、回调验签、幂等落库和调用日志记录等核心能力。该场景适用于支付、短信、物流、地图、OCR、银行接口、电子合同、企业微信、钉钉等平台对接。

## 功能目标

本案例以“系统对接一个第三方开放平台”为目标，模拟完成一次业务请求调用和一次第三方回调处理。重点不是接入某一个真实厂商 SDK，而是沉淀一套通用的第三方接口对接模板，后续可以扩展到短信、物流、OCR、支付、电子合同等不同平台。

### 实现范围

本案例实现以下核心功能：

```text
1. 后台维护第三方平台配置
2. 业务系统发起第三方接口调用
3. 调用前生成请求签名
4. 发送 HTTP 请求到第三方平台
5. 解析第三方响应结果
6. 记录接口调用日志
7. 对失败调用进行重试或投递失败消息
8. 接收第三方平台回调
9. 对回调请求进行签名校验
10. 对回调事件做幂等处理并落库
```

不展开以下非核心内容：

```text
1. 完整后台管理页面
2. 多租户平台配置隔离
3. 真实支付或短信厂商 SDK
4. 复杂网关鉴权
5. 分布式链路追踪平台接入
6. 完整监控告警体系
```

本案例默认使用一个虚拟平台 `MockOpenPlatform` 作为第三方平台，接口形态如下：

| 类型         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 主动调用接口 | 本系统调用第三方平台，例如发送短信、创建电子合同、查询物流   |
| 回调通知接口 | 第三方平台调用本系统，例如支付成功通知、合同签署完成通知、物流状态变更通知 |
| 签名算法     | HMAC-SHA256                                                  |
| 数据格式     | JSON                                                         |
| 幂等依据     | 第三方回调事件号 `eventNo`                                   |
| 日志策略     | 每次请求和回调都记录请求报文、响应报文、状态和异常信息       |

### 核心流程

整体流程分为“主动调用第三方接口”和“接收第三方回调”两条链路。

主动调用第三方接口流程：

```text
业务系统发起请求
-> 查询第三方平台配置
-> 组装公共参数 appId、timestamp、nonce
-> 按规则生成签名 sign
-> 通过 OpenFeign 调用第三方接口
-> 解析第三方响应
-> 记录接口调用日志
-> 成功返回业务结果
-> 失败触发重试或投递 RabbitMQ 失败消息
```

第三方回调处理流程：

```text
第三方平台发送回调
-> 系统接收回调 JSON
-> 提取请求头中的 appId、timestamp、nonce、sign
-> 根据 appId 查询平台密钥
-> 使用相同算法重新生成签名
-> 对比签名是否一致
-> 校验 timestamp 防止重放请求
-> 根据 eventNo 做幂等判断
-> 落库保存回调记录
-> 推动后续业务状态变更
-> 返回 success
```

签名验签的核心原则：

```text
请求方：
业务参数 + 公共参数 + secret -> 生成 sign -> 发送请求

接收方：
收到参数 + 本地 secret -> 重新生成 sign -> 对比请求 sign
```

本案例采用如下签名原始串规则：

```text
1. 排除 sign 字段
2. 排除 null 值字段
3. 参数按照 key 字典序升序排列
4. 使用 key=value&key=value 方式拼接
5. 使用 HMAC-SHA256 加密
6. 输出 hex 小写字符串
```

示例：

```text
原始参数：
appId=mock-app-001
timestamp=1735200000000
nonce=8f3a9c12
bizNo=SMS202501010001
phone=13800138000
content=您的验证码是 123456

排序后签名原始串：
appId=mock-app-001&bizNo=SMS202501010001&content=您的验证码是 123456&nonce=8f3a9c12&phone=13800138000&timestamp=1735200000000

最终签名：
HMAC_SHA256(原始串, secret)
```

## 技术选型

本案例采用 Spring Boot 3 作为基础框架，MyBatis-Plus 负责数据访问，Hutool 处理 JSON、签名、时间、字符串和脱敏等通用能力，OpenFeign 负责第三方 HTTP 调用，Resilience4j 负责重试和熔断，Redis 负责限流和幂等缓存，RabbitMQ 用于失败消息异步补偿。

### Spring Boot 与 MyBatis-Plus

Spring Boot 负责提供 Web 接口、配置加载、依赖注入和业务编排。MyBatis-Plus 负责第三方配置、调用日志、回调记录等表的基础 CRUD。

本案例中主要使用三张表：

| 表名                    | 用途                                                   |
| ----------------------- | ------------------------------------------------------ |
| `third_platform_config` | 保存第三方平台配置，例如 appId、secret、接口地址、状态 |
| `third_api_log`         | 保存主动调用第三方接口的请求日志、响应日志和调用状态   |
| `third_callback_record` | 保存第三方回调事件，支持回调验签和幂等处理             |

选择 MyBatis-Plus 的原因是：

```text
1. 实体、Mapper、Service 结构清晰
2. 单表 CRUD 代码量少
3. 适合快速完成业务案例落地
4. 后续可以方便扩展分页查询、条件查询和状态更新
```

### Hutool 签名、JSON 与 HTTP 工具

Hutool 在本案例中主要用于减少通用工具代码，提高代码可读性。

使用点如下：

| Hutool 工具           | 使用场景                       |
| --------------------- | ------------------------------ |
| `JSONUtil`            | 对象与 JSON 字符串转换         |
| `StrUtil`             | 字符串判空、格式化             |
| `CollUtil`            | 集合判空                       |
| `MapUtil`             | Map 判空与构建                 |
| `IdUtil`              | 生成 nonce、请求流水号         |
| `DateUtil`            | 时间处理                       |
| `DigestUtil` / `HMac` | 生成 HMAC-SHA256 签名          |
| `DesensitizedUtil`    | 手机号、密钥、身份证等字段脱敏 |
| `BeanUtil`            | DTO 与 Map 转换                |

本案例优先使用 OpenFeign 调用第三方接口，因为它更适合 Spring Cloud 项目中的接口声明式调用。如果只是单体项目或临时脚本式调用，也可以使用 Hutool `HttpUtil` 快速发送请求。

推荐策略：

```text
正式业务接口：OpenFeign
简单临时调用：Hutool HttpUtil
签名与 JSON 处理：Hutool
复杂失败重试：Resilience4j
```

### Redis、RabbitMQ 与 Resilience4j

第三方平台通常存在接口超时、偶发失败、限流、重复回调等问题，因此本案例引入 Redis、RabbitMQ 和 Resilience4j 处理稳定性问题。

Redis 主要负责：

```text
1. 第三方平台配置缓存
2. 回调事件幂等 Key
3. 接口调用限流 Key
4. nonce 防重放 Key
```

RabbitMQ 主要负责：

```text
1. 第三方接口调用失败后的异步补偿
2. 回调业务处理失败后的延迟重试
3. 避免在回调接口中执行过重业务逻辑
```

Resilience4j 主要负责：

```text
1. 第三方接口超时重试
2. 第三方接口熔断保护
3. 避免外部平台异常拖垮本系统
4. 配合日志记录最终失败原因
```

整体选型后的职责划分如下：

| 技术          | 主要职责                           |
| ------------- | ---------------------------------- |
| Spring Boot 3 | Web 接口、配置、业务编排           |
| MyBatis-Plus  | 平台配置、调用日志、回调记录落库   |
| OpenFeign     | 声明式调用第三方 HTTP 接口         |
| Hutool        | 签名、JSON、时间、字符串、脱敏工具 |
| Redis         | 限流、幂等、防重放、配置缓存       |
| RabbitMQ      | 失败补偿、异步处理、削峰           |
| Resilience4j  | 重试、熔断、超时保护               |
| MySQL         | 持久化平台配置、日志和回调记录     |

## 项目结构

本案例采用常见 Spring Boot 分层结构，核心目标是把“平台配置、签名工具、第三方调用、回调验签、日志记录、幂等处理”拆分清楚，方便后续扩展短信、物流、OCR、电子合同等不同平台。技术栈沿用 README 中第 26 个场景推荐的 Spring Boot、OpenFeign、Resilience4j、Redis、RabbitMQ、Hutool、MyBatis-Plus 等组合。

### 包结构规划

推荐包结构如下：

```text
src/main/java/io/github/atengk/third
├── ThirdPlatformApplication.java
├── common
│   ├── constant
│   │   └── ThirdRedisKeyConstant.java
│   ├── exception
│   │   └── ThirdPlatformException.java
│   └── result
│       ├── Result.java
│       └── ThirdErrorCode.java
├── config
│   ├── FeignConfig.java
│   ├── RedisConfig.java
│   └── Resilience4jConfig.java
├── controller
│   ├── ThirdApiController.java
│   └── ThirdCallbackController.java
├── dto
│   ├── ThirdInvokeRequest.java
│   ├── ThirdInvokeResponse.java
│   ├── ThirdCallbackRequest.java
│   └── ThirdSignHeaders.java
├── entity
│   ├── ThirdPlatformConfig.java
│   ├── ThirdApiLog.java
│   └── ThirdCallbackRecord.java
├── enums
│   ├── ThirdApiLogStatusEnum.java
│   ├── ThirdCallbackStatusEnum.java
│   └── ThirdPlatformStatusEnum.java
├── feign
│   └── MockOpenPlatformClient.java
├── mapper
│   ├── ThirdPlatformConfigMapper.java
│   ├── ThirdApiLogMapper.java
│   └── ThirdCallbackRecordMapper.java
├── mq
│   ├── ThirdMqConstant.java
│   ├── ThirdRetryMessage.java
│   └── ThirdRetryProducer.java
├── service
│   ├── ThirdInvokeService.java
│   ├── ThirdCallbackService.java
│   ├── ThirdPlatformConfigService.java
│   └── impl
│       ├── ThirdInvokeServiceImpl.java
│       ├── ThirdCallbackServiceImpl.java
│       └── ThirdPlatformConfigServiceImpl.java
└── util
    ├── ThirdSignUtil.java
    ├── ThirdDesensitizeUtil.java
    └── ThirdRequestUtil.java
```

如果是微服务项目，可以把通用签名、DTO、错误码、Feign 接口抽到独立模块：

```text
third-platform-api      # 对外 DTO、VO、Feign 接口、枚举
third-platform-service  # 具体业务实现、数据库、MQ、Redis、回调处理
```

单体项目不需要过度拆分，按上面的 `controller/service/mapper/entity/util` 分层即可。

### 核心类职责

核心类职责如下：

| 类名                         | 职责                                                   |
| ---------------------------- | ------------------------------------------------------ |
| `ThirdApiController`         | 对业务系统提供主动调用第三方接口的入口                 |
| `ThirdCallbackController`    | 接收第三方平台回调                                     |
| `ThirdInvokeService`         | 编排第三方接口调用流程，包括签名、调用、日志、异常处理 |
| `ThirdCallbackService`       | 编排回调验签、幂等、落库和后续业务处理                 |
| `ThirdPlatformConfigService` | 管理第三方平台配置，提供缓存查询能力                   |
| `MockOpenPlatformClient`     | 使用 OpenFeign 声明第三方平台 HTTP 接口                |
| `ThirdSignUtil`              | 生成请求签名、校验回调签名                             |
| `ThirdDesensitizeUtil`       | 对请求日志、响应日志中的敏感字段脱敏                   |
| `ThirdRetryProducer`         | 第三方接口失败后发送补偿消息                           |
| `ThirdPlatformConfig`        | 第三方平台配置实体                                     |
| `ThirdApiLog`                | 第三方接口主动调用日志实体                             |
| `ThirdCallbackRecord`        | 第三方回调通知记录实体                                 |

本案例的核心调用链如下：

```text
ThirdApiController
-> ThirdInvokeService
-> ThirdPlatformConfigService
-> ThirdSignUtil
-> MockOpenPlatformClient
-> ThirdApiLogMapper
-> ThirdRetryProducer
```

回调处理链路如下：

```text
ThirdCallbackController
-> ThirdCallbackService
-> ThirdPlatformConfigService
-> ThirdSignUtil
-> Redis 幂等校验
-> ThirdCallbackRecordMapper
-> 后续业务处理
```

## 数据库设计

数据库主要保存三类数据：第三方平台配置、主动接口调用日志、第三方回调记录。真实项目中可以根据平台数量、日志量、回调量进行分库分表或冷热数据归档，本案例先使用 MySQL 单库单表实现核心能力。

### 第三方平台配置表

该表用于保存第三方平台的基础参数。一个平台可以对应一个 `app_id` 和 `app_secret`，也可以根据业务扩展为多商户、多租户、多渠道配置。

```sql
CREATE TABLE third_platform_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    platform_code VARCHAR(64) NOT NULL COMMENT '平台编码，例如 MOCK_OPEN、ALI_SMS、JD_LOGISTICS',
    platform_name VARCHAR(128) NOT NULL COMMENT '平台名称',
    app_id VARCHAR(128) NOT NULL COMMENT '第三方平台分配的AppId',
    app_secret VARCHAR(512) NOT NULL COMMENT '第三方平台密钥，生产环境建议加密存储',
    base_url VARCHAR(512) NOT NULL COMMENT '第三方平台接口基础地址',
    callback_url VARCHAR(512) DEFAULT NULL COMMENT '本系统提供给第三方平台的回调地址',
    sign_type VARCHAR(32) NOT NULL DEFAULT 'HMAC_SHA256' COMMENT '签名算法',
    timeout_ms INT NOT NULL DEFAULT 5000 COMMENT '接口超时时间，单位毫秒',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_code (platform_code),
    UNIQUE KEY uk_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方平台配置表';
```

关键设计点：

| 字段            | 说明                                                  |
| --------------- | ----------------------------------------------------- |
| `platform_code` | 系统内部平台编码，业务调用时优先使用它定位平台        |
| `app_id`        | 第三方平台分配的应用 ID，回调验签时也可通过它反查密钥 |
| `app_secret`    | 签名密钥，生产环境不建议明文存储                      |
| `base_url`      | 第三方接口地址，支持不同环境配置不同地址              |
| `callback_url`  | 回调地址，用于平台注册或排查配置                      |
| `sign_type`     | 支持后续扩展 RSA、MD5、SHA256 等算法                  |
| `timeout_ms`    | 不同平台接口超时时间可能不同                          |
| `status`        | 禁用后禁止主动调用和回调处理                          |

初始化一条模拟平台配置：

```sql
INSERT INTO third_platform_config (
    platform_code,
    platform_name,
    app_id,
    app_secret,
    base_url,
    callback_url,
    sign_type,
    timeout_ms,
    status,
    remark
) VALUES (
    'MOCK_OPEN',
    '模拟开放平台',
    'mock-app-001',
    'mock-secret-123456',
    'http://localhost:9090',
    'http://localhost:8080/api/third/callback/mock',
    'HMAC_SHA256',
    5000,
    1,
    '用于第三方接口签名验签案例'
);
```

### 接口调用日志表

该表记录本系统主动调用第三方平台的全过程，包括请求参数、签名、响应结果、耗时和异常信息。它主要用于问题排查、接口审计、失败补偿和对账分析。

```sql
CREATE TABLE third_api_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    request_no VARCHAR(64) NOT NULL COMMENT '本系统请求流水号',
    platform_code VARCHAR(64) NOT NULL COMMENT '平台编码',
    api_code VARCHAR(64) NOT NULL COMMENT '接口编码，例如 SEND_SMS、CREATE_CONTRACT',
    api_url VARCHAR(512) NOT NULL COMMENT '实际请求地址',
    http_method VARCHAR(16) NOT NULL DEFAULT 'POST' COMMENT 'HTTP方法',
    app_id VARCHAR(128) NOT NULL COMMENT '请求使用的AppId',
    nonce VARCHAR(64) NOT NULL COMMENT '请求随机串',
    sign VARCHAR(256) NOT NULL COMMENT '请求签名',
    request_body TEXT COMMENT '请求报文，建议脱敏后保存',
    response_body TEXT COMMENT '响应报文，建议脱敏后保存',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '调用状态：0-处理中，1-成功，2-失败',
    error_code VARCHAR(64) DEFAULT NULL COMMENT '错误码',
    error_msg VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
    cost_ms BIGINT DEFAULT NULL COMMENT '接口耗时，单位毫秒',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_request_no (request_no),
    KEY idx_platform_api (platform_code, api_code),
    KEY idx_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方接口调用日志表';
```

关键设计点：

| 字段            | 说明                                           |
| --------------- | ---------------------------------------------- |
| `request_no`    | 本系统请求流水号，全局唯一                     |
| `api_code`      | 业务接口编码，例如发送短信、创建合同、物流下单 |
| `request_body`  | 建议保存脱敏后的请求报文                       |
| `response_body` | 建议保存脱敏后的响应报文                       |
| `status`        | 失败数据可以被定时任务或 MQ 消费者扫描补偿     |
| `retry_count`   | 避免无限重试                                   |
| `cost_ms`       | 用于分析第三方接口性能                         |

调用状态建议统一定义：

```text
0：处理中
1：成功
2：失败
```

### 回调通知记录表

该表记录第三方平台回调通知。它的重点是支持验签、幂等和状态追踪。第三方回调通常可能重复推送，所以必须使用唯一索引控制重复处理。

```sql
CREATE TABLE third_callback_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    event_no VARCHAR(128) NOT NULL COMMENT '第三方回调事件号，用于幂等控制',
    platform_code VARCHAR(64) NOT NULL COMMENT '平台编码',
    app_id VARCHAR(128) NOT NULL COMMENT '第三方平台AppId',
    callback_type VARCHAR(64) NOT NULL COMMENT '回调类型，例如 PAY_SUCCESS、CONTRACT_SIGNED、LOGISTICS_CHANGE',
    nonce VARCHAR(64) NOT NULL COMMENT '回调随机串',
    sign VARCHAR(256) NOT NULL COMMENT '回调签名',
    request_body TEXT NOT NULL COMMENT '回调原始报文，建议脱敏后保存',
    verify_status TINYINT NOT NULL DEFAULT 0 COMMENT '验签状态：0-未验签，1-验签成功，2-验签失败',
    process_status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0-待处理，1-处理成功，2-处理失败',
    error_msg VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
    callback_time DATETIME DEFAULT NULL COMMENT '第三方回调发生时间',
    processed_at DATETIME DEFAULT NULL COMMENT '本系统处理完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_no (event_no),
    KEY idx_platform_type (platform_code, callback_type),
    KEY idx_process_status_created_at (process_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方回调通知记录表';
```

关键设计点：

| 字段             | 说明                                       |
| ---------------- | ------------------------------------------ |
| `event_no`       | 第三方回调事件号，必须唯一，用于幂等       |
| `callback_type`  | 不同回调类型进入不同业务分支               |
| `verify_status`  | 验签失败也建议落库，方便排查攻击或配置错误 |
| `process_status` | 验签成功但业务处理失败时，可后续补偿       |
| `request_body`   | 保留原始报文，便于排查和重放               |
| `processed_at`   | 标识业务处理完成时间                       |

回调状态建议统一定义：

```text
验签状态：
0：未验签
1：验签成功
2：验签失败

处理状态：
0：待处理
1：处理成功
2：处理失败
```

## 第三方平台配置管理

第三方平台配置管理是整个对接模块的基础。主动调用需要根据 `platform_code` 获取 `app_id`、`app_secret`、`base_url`；回调验签需要根据请求头中的 `app_id` 反查平台配置和密钥。

### 平台参数配置

建议把不会频繁变更的服务级参数放在 `application.yml`，把会因环境、平台、商户变化的参数放在数据库。

`application.yml` 中放模块级默认配置：

```yaml
server:
  port: 8080

spring:
  application:
    name: third-platform-demo

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/third_platform_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3s

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /

third-platform:
  sign:
    # 回调时间戳允许偏差，单位秒，用于防止重放请求
    timestamp-offset-seconds: 300

  invoke:
    # 单个平台接口默认最大重试次数
    max-retry-count: 3

    # 是否记录完整报文。生产环境建议结合脱敏策略开启
    record-payload: true

  cache:
    # 平台配置缓存时间，单位秒
    platform-config-ttl-seconds: 1800
```

数据库中放具体平台参数：

```text
platform_code：MOCK_OPEN
platform_name：模拟开放平台
app_id：mock-app-001
app_secret：mock-secret-123456
base_url：http://localhost:9090
callback_url：http://localhost:8080/api/third/callback/mock
sign_type：HMAC_SHA256
timeout_ms：5000
status：1
```

这种拆分方式的好处是：

```text
1. 应用级默认行为由配置文件控制
2. 平台级参数由数据库动态维护
3. 平台配置变更后可以刷新 Redis 缓存
4. 不需要每次新增平台都修改代码
```

### 密钥与回调地址配置

生产环境中，`app_secret` 不建议明文保存。最低要求是限制数据库权限并避免在日志中输出；更优方案是对密钥进行加密存储，启动时或使用时解密。

推荐密钥处理方式：

```text
开发环境：
app_secret 可以直接明文配置，方便调试

测试环境：
app_secret 建议脱敏展示，日志禁止打印完整值

生产环境：
app_secret 建议加密存储，配合 KMS、Jasypt 或企业密钥管理系统
```

回调地址配置需要注意以下事项：

```text
1. 回调地址必须是公网可访问地址
2. HTTPS 优先，避免明文传输
3. 回调接口路径尽量区分平台和业务类型
4. 第三方平台后台配置的回调地址必须和系统实际接口一致
5. 回调接口不要依赖用户登录态，应使用签名验签保证来源可信
```

推荐回调地址设计：

```text
/api/third/callback/{platformCode}
```

示例：

```text
/api/third/callback/mock
/api/third/callback/ali-sms
/api/third/callback/jd-logistics
/api/third/callback/e-contract
```

请求头建议统一设计：

| Header        | 说明             |
| ------------- | ---------------- |
| `X-App-Id`    | 第三方平台 AppId |
| `X-Timestamp` | 毫秒时间戳       |
| `X-Nonce`     | 随机串           |
| `X-Sign-Type` | 签名类型         |
| `X-Sign`      | 请求签名         |

主动调用第三方接口时，本系统也使用同样的请求头格式，便于统一签名工具。

### 配置缓存处理

第三方平台配置读取频率较高，尤其是回调验签链路中每次都需要根据 `app_id` 查询密钥。为了避免频繁查询数据库，建议使用 Redis 缓存平台配置。

Redis Key 推荐：

```text
third:platform:config:code:{platformCode}
third:platform:config:appid:{appId}
third:callback:idempotent:{eventNo}
third:callback:nonce:{appId}:{nonce}
third:invoke:limit:{platformCode}:{apiCode}
```

缓存策略建议：

| Key                                           | 用途                 | 过期时间      |
| --------------------------------------------- | -------------------- | ------------- |
| `third:platform:config:code:{platformCode}`   | 根据平台编码查询配置 | 30 分钟       |
| `third:platform:config:appid:{appId}`         | 根据 AppId 查询配置  | 30 分钟       |
| `third:callback:idempotent:{eventNo}`         | 回调事件幂等         | 24 小时或更长 |
| `third:callback:nonce:{appId}:{nonce}`        | 防止回调重放         | 5 分钟        |
| `third:invoke:limit:{platformCode}:{apiCode}` | 接口限流计数         | 1 秒或 1 分钟 |

配置缓存处理流程：

```text
根据 platformCode 查询平台配置
-> 先查 Redis
-> Redis 命中则直接返回
-> Redis 未命中则查询 MySQL
-> 校验配置是否存在、是否启用
-> 写入 Redis
-> 返回配置
```

配置更新后的处理流程：

```text
后台修改平台配置
-> 更新 MySQL
-> 删除 Redis 中 platformCode 缓存
-> 删除 Redis 中 appId 缓存
-> 下一次读取时重新加载
```

如果系统中没有后台管理页面，也可以通过 SQL 修改配置后手动删除 Redis Key：

```bash
redis-cli DEL third:platform:config:code:MOCK_OPEN
redis-cli DEL third:platform:config:appid:mock-app-001
```

上面的命令用于手动清理平台配置缓存。`DEL` 后面的参数是需要删除的 Redis Key，删除后系统会在下一次调用或回调时重新从 MySQL 加载最新配置。

## 请求签名实现

请求签名用于保证第三方接口调用的参数没有被篡改，同时让接收方能够确认请求来源。该案例使用 HMAC-SHA256 作为默认签名算法，符合第三方平台接口对接场景中“签名算法、接口超时、回调验签、请求日志脱敏”等核心难点。

### 签名参数组装

签名参数由“业务参数”和“公共参数”组成。业务参数是接口本身需要的数据，例如手机号、短信内容、业务单号；公共参数用于平台鉴权和防重放，例如 `appId`、`timestamp`、`nonce`。

建议公共参数如下：

| 参数        | 说明                    | 示例            |
| ----------- | ----------------------- | --------------- |
| `appId`     | 第三方平台分配的应用 ID | `mock-app-001`  |
| `timestamp` | 当前毫秒时间戳          | `1735200000000` |
| `nonce`     | 随机字符串              | `a3f9c21e8b`    |
| `signType`  | 签名算法                | `HMAC_SHA256`   |
| `sign`      | 最终签名                | `d8e4...`       |

签名字段建议放在请求头中，业务参数放在 JSON Body 中。这样可以避免业务参数和鉴权参数混杂，后续接入不同平台时也更容易维护。

文件位置：`src/main/java/io/github/atengk/third/dto/ThirdSignHeaders.java`

该类用于封装第三方接口调用和回调验签时统一使用的签名请求头。

```java
package io.github.atengk.third.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 第三方接口签名请求头
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdSignHeaders {

    /**
     * 第三方平台应用ID
     */
    private String appId;

    /**
     * 请求时间戳，毫秒
     */
    private String timestamp;

    /**
     * 请求随机串
     */
    private String nonce;

    /**
     * 签名算法
     */
    private String signType;

    /**
     * 请求签名
     */
    private String sign;
}
```

文件位置：`src/main/java/io/github/atengk/third/dto/ThirdInvokeRequest.java`

该类用于封装本系统主动调用第三方接口时的业务请求参数。

```java
package io.github.atengk.third.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 第三方接口调用请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdInvokeRequest {

    /**
     * 平台编码，例如 MOCK_OPEN
     */
    private String platformCode;

    /**
     * 接口编码，例如 SEND_SMS、CREATE_CONTRACT
     */
    private String apiCode;

    /**
     * 业务请求流水号
     */
    private String bizNo;

    /**
     * 业务请求参数
     */
    private Map<String, Object> bizParams;
}
```

文件位置：`src/main/java/io/github/atengk/third/dto/ThirdInvokeResponse.java`

该类用于统一封装第三方接口调用后的响应结果。

```java
package io.github.atengk.third.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 第三方接口调用响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdInvokeResponse {

    /**
     * 是否调用成功
     */
    private Boolean success;

    /**
     * 本系统请求流水号
     */
    private String requestNo;

    /**
     * 第三方响应码
     */
    private String thirdCode;

    /**
     * 第三方响应消息
     */
    private String thirdMessage;

    /**
     * 第三方响应数据，JSON 字符串
     */
    private String data;
}
```

签名参数组装时，不建议直接修改原始业务参数对象，而是复制一份 `Map` 后再补充公共参数。

文件位置：`src/main/java/io/github/atengk/third/util/ThirdRequestUtil.java`

该工具类用于组装签名参数和生成基础请求流水号。

```java
package io.github.atengk.third.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.third.dto.ThirdSignHeaders;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三方请求工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThirdRequestUtil {

    private ThirdRequestUtil() {
    }

    /**
     * 生成请求流水号
     *
     * @param apiCode 接口编码
     * @return 请求流水号
     */
    public static String generateRequestNo(String apiCode) {
        return apiCode + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * 构建签名基础请求头
     *
     * @param appId 应用ID
     * @return 签名请求头
     */
    public static ThirdSignHeaders buildBaseHeaders(String appId) {
        return ThirdSignHeaders.builder()
                .appId(appId)
                .timestamp(String.valueOf(System.currentTimeMillis()))
                .nonce(UUID.fastUUID().toString(true))
                .signType("HMAC_SHA256")
                .build();
    }

    /**
     * 组装参与签名的参数
     *
     * @param bizParams 业务参数
     * @param headers   签名请求头
     * @return 签名参数
     */
    public static Map<String, Object> buildSignParams(Map<String, Object> bizParams, ThirdSignHeaders headers) {
        Map<String, Object> signParams = new LinkedHashMap<>();
        if (MapUtil.isNotEmpty(bizParams)) {
            signParams.putAll(bizParams);
        }
        signParams.put("appId", headers.getAppId());
        signParams.put("timestamp", headers.getTimestamp());
        signParams.put("nonce", headers.getNonce());
        signParams.put("signType", headers.getSignType());
        return signParams;
    }
}
```

### 参数排序与摘要生成

签名生成规则建议固定下来，避免不同接口、不同开发人员实现不一致。

本案例规则如下：

```text
1. 排除 sign 字段
2. 排除值为 null 的字段
3. 参数按照 key 字典序升序排序
4. 使用 key=value&key=value 拼接原始串
5. 使用 appSecret 作为密钥执行 HMAC-SHA256
6. 输出 hex 小写字符串
```

文件位置：`src/main/java/io/github/atengk/third/util/ThirdSignUtil.java`

该工具类用于生成签名、验签和构建签名原始串。

```java
package io.github.atengk.third.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 第三方接口签名工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThirdSignUtil {

    private static final String SIGN_FIELD = "sign";

    private ThirdSignUtil() {
    }

    /**
     * 生成 HMAC-SHA256 签名
     *
     * @param params    待签名参数
     * @param appSecret 应用密钥
     * @return 签名字符串
     */
    public static String hmacSha256Sign(Map<String, Object> params, String appSecret) {
        if (MapUtil.isEmpty(params)) {
            throw new IllegalArgumentException("签名参数不能为空");
        }
        if (StrUtil.isBlank(appSecret)) {
            throw new IllegalArgumentException("签名密钥不能为空");
        }

        String signText = buildSignText(params);
        HMac hMac = SecureUtil.hmacSha256(appSecret.getBytes(CharsetUtil.CHARSET_UTF_8));
        return hMac.digestHex(signText);
    }

    /**
     * 校验 HMAC-SHA256 签名
     *
     * @param params    待验签参数
     * @param appSecret 应用密钥
     * @param sign      请求签名
     * @return 是否验签通过
     */
    public static boolean verifyHmacSha256(Map<String, Object> params, String appSecret, String sign) {
        if (StrUtil.isBlank(sign)) {
            return false;
        }
        String localSign = hmacSha256Sign(params, appSecret);
        return StrUtil.equalsIgnoreCase(localSign, sign);
    }

    /**
     * 构建签名原始串
     *
     * @param params 待签名参数
     * @return 签名原始串
     */
    public static String buildSignText(Map<String, Object> params) {
        TreeMap<String, Object> sortedMap = new TreeMap<>();
        params.forEach((key, value) -> {
            if (StrUtil.isNotBlank(key) && !StrUtil.equals(key, SIGN_FIELD) && Objects.nonNull(value)) {
                sortedMap.put(key, value);
            }
        });

        return sortedMap.entrySet()
                .stream()
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .map(entry -> entry.getKey() + "=" + normalizeValue(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * 规范化参数值
     *
     * @param value 参数值
     * @return 字符串值
     */
    private static String normalizeValue(Object value) {
        if (value instanceof String str) {
            return str;
        }
        return String.valueOf(value);
    }
}
```

签名生成示例：

```java
Map<String, Object> params = new HashMap<>();
params.put("appId", "mock-app-001");
params.put("timestamp", "1735200000000");
params.put("nonce", "8f3a9c12");
params.put("bizNo", "SMS202501010001");
params.put("phone", "13800138000");
params.put("content", "您的验证码是 123456");

String sign = ThirdSignUtil.hmacSha256Sign(params, "mock-secret-123456");
```

生成的签名原始串类似：

```text
appId=mock-app-001&bizNo=SMS202501010001&content=您的验证码是 123456&nonce=8f3a9c12&phone=13800138000&timestamp=1735200000000
```

### 请求头封装

主动调用第三方平台时，建议统一使用请求头传递签名信息。这样第三方平台可以从 Header 中读取鉴权信息，从 Body 中读取业务参数。

请求头格式如下：

```text
X-App-Id: mock-app-001
X-Timestamp: 1735200000000
X-Nonce: 8f3a9c12
X-Sign-Type: HMAC_SHA256
X-Sign: d8e4a2...
```

OpenFeign 方法参数中可以直接声明这些 Header。

```java
@PostMapping("/open/api/invoke")
Map<String, Object> invoke(
        @RequestHeader("X-App-Id") String appId,
        @RequestHeader("X-Timestamp") String timestamp,
        @RequestHeader("X-Nonce") String nonce,
        @RequestHeader("X-Sign-Type") String signType,
        @RequestHeader("X-Sign") String sign,
        @RequestBody Map<String, Object> body
);
```

封装请求头时，调用顺序如下：

```text
查询平台配置
-> 构建基础 Header
-> 合并 Header 参数和业务参数
-> 生成 sign
-> 设置到 Header
-> 发起 OpenFeign 调用
```

示例代码：

```java
ThirdSignHeaders headers = ThirdRequestUtil.buildBaseHeaders(config.getAppId());
Map<String, Object> signParams = ThirdRequestUtil.buildSignParams(request.getBizParams(), headers);
String sign = ThirdSignUtil.hmacSha256Sign(signParams, config.getAppSecret());
headers.setSign(sign);
```

## 第三方接口调用实现

第三方接口调用模块负责把业务请求转换成第三方平台要求的请求格式，并完成签名、请求发送、响应解析、错误码映射和日志记录。本节先实现“可调用、可解析、可映射”的核心逻辑，日志落库和失败补偿可以在后续章节继续扩展。

### 请求对象定义

本案例把第三方平台响应统一抽象成 `MockPlatformResponse`。真实项目中，不同平台返回结构可能不同，可以为每个平台定义独立响应对象，再在业务层统一转换成系统内部响应。

文件位置：`src/main/java/io/github/atengk/third/dto/MockPlatformResponse.java`

该类用于接收模拟第三方平台的统一响应结构。

```java
package io.github.atengk.third.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模拟第三方平台响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockPlatformResponse<T> {

    /**
     * 第三方响应码
     */
    private String code;

    /**
     * 第三方响应消息
     */
    private String message;

    /**
     * 第三方响应数据
     */
    private T data;

    /**
     * 第三方请求ID
     */
    private String requestId;
}
```

业务请求 Body 可以保持通用 Map，也可以定义强类型 DTO。为了案例更接近实际业务，这里给出一个短信发送请求对象。

文件位置：`src/main/java/io/github/atengk/third/dto/MockSmsSendRequest.java`

该类用于模拟调用第三方短信发送接口。

```java
package io.github.atengk.third.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模拟短信发送请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockSmsSendRequest {

    /**
     * 业务单号
     */
    private String bizNo;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 短信内容
     */
    private String content;
}
```

### OpenFeign 调用封装

OpenFeign 负责声明式调用第三方接口。案例中先使用固定 `url` 配置，实际项目可以结合数据库中的 `base_url` 动态路由，或者按平台拆分多个 Feign Client。

文件位置：`src/main/java/io/github/atengk/third/feign/MockOpenPlatformClient.java`

该 Feign Client 用于调用模拟第三方开放平台接口，并通过 Header 传递签名信息。

```java
package io.github.atengk.third.feign;

import io.github.atengk.third.dto.MockPlatformResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模拟第三方开放平台客户端
 *
 * @author Ateng
 * @since 2026-05-15
 */
@FeignClient(
        name = "mockOpenPlatformClient",
        url = "${third-platform.mock.base-url:http://localhost:9090}"
)
public interface MockOpenPlatformClient {

    /**
     * 调用模拟第三方接口
     *
     * @param appId     应用ID
     * @param timestamp 时间戳
     * @param nonce     随机串
     * @param signType  签名类型
     * @param sign      请求签名
     * @param body      请求体
     * @return 第三方响应
     */
    @PostMapping("/open/api/invoke")
    MockPlatformResponse<Map<String, Object>> invoke(
            @RequestHeader("X-App-Id") String appId,
            @RequestHeader("X-Timestamp") String timestamp,
            @RequestHeader("X-Nonce") String nonce,
            @RequestHeader("X-Sign-Type") String signType,
            @RequestHeader("X-Sign") String sign,
            @RequestBody Map<String, Object> body
    );
}
```

如果需要统一 Feign 日志和超时，可以增加基础配置。

文件位置：`src/main/java/io/github/atengk/third/config/FeignConfig.java`

该配置用于开启 Feign 基础日志，方便开发环境排查第三方接口请求问题。

```java
package io.github.atengk.third.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class FeignConfig {

    /**
     * 配置 Feign 日志级别
     *
     * @return 日志级别
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
```

对应配置示例：

```yaml
third-platform:
  mock:
    # 模拟第三方平台地址
    base-url: http://localhost:9090

spring:
  cloud:
    openfeign:
      client:
        config:
          mockOpenPlatformClient:
            # 连接超时时间
            connect-timeout: 3000
            # 读取超时时间
            read-timeout: 5000
            # 开发环境可配置 full，生产环境建议 basic 或 none
            logger-level: basic
```

### 统一响应解析

第三方接口响应不能直接返回给业务层，建议转换成系统内部统一响应。这样可以屏蔽不同平台的响应结构差异，也方便后续做错误码映射、日志记录和失败补偿。

文件位置：`src/main/java/io/github/atengk/third/service/ThirdInvokeService.java`

该接口定义第三方调用服务的入口。

```java
package io.github.atengk.third.service;

import io.github.atengk.third.dto.ThirdInvokeRequest;
import io.github.atengk.third.dto.ThirdInvokeResponse;

/**
 * 第三方接口调用服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ThirdInvokeService {

    /**
     * 调用第三方接口
     *
     * @param request 调用请求
     * @return 调用响应
     */
    ThirdInvokeResponse invoke(ThirdInvokeRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/third/service/impl/ThirdInvokeServiceImpl.java`

该实现类完成签名生成、Feign 调用、响应转换和异常映射。

```java
package io.github.atengk.third.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.third.common.exception.ThirdPlatformException;
import io.github.atengk.third.dto.MockPlatformResponse;
import io.github.atengk.third.dto.ThirdInvokeRequest;
import io.github.atengk.third.dto.ThirdInvokeResponse;
import io.github.atengk.third.dto.ThirdSignHeaders;
import io.github.atengk.third.entity.ThirdPlatformConfig;
import io.github.atengk.third.enums.ThirdErrorCode;
import io.github.atengk.third.feign.MockOpenPlatformClient;
import io.github.atengk.third.service.ThirdInvokeService;
import io.github.atengk.third.service.ThirdPlatformConfigService;
import io.github.atengk.third.util.ThirdRequestUtil;
import io.github.atengk.third.util.ThirdSignUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 第三方接口调用服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdInvokeServiceImpl implements ThirdInvokeService {

    private final MockOpenPlatformClient mockOpenPlatformClient;
    private final ThirdPlatformConfigService thirdPlatformConfigService;

    /**
     * 调用第三方接口
     *
     * @param request 调用请求
     * @return 调用响应
     */
    @Override
    public ThirdInvokeResponse invoke(ThirdInvokeRequest request) {
        validateRequest(request);

        String requestNo = ThirdRequestUtil.generateRequestNo(request.getApiCode());
        ThirdPlatformConfig config = thirdPlatformConfigService.getEnabledByPlatformCode(request.getPlatformCode());

        ThirdSignHeaders headers = ThirdRequestUtil.buildBaseHeaders(config.getAppId());
        Map<String, Object> signParams = ThirdRequestUtil.buildSignParams(request.getBizParams(), headers);
        String sign = ThirdSignUtil.hmacSha256Sign(signParams, config.getAppSecret());
        headers.setSign(sign);

        log.info("开始调用第三方接口，requestNo={}，platformCode={}，apiCode={}",
                requestNo, request.getPlatformCode(), request.getApiCode());

        try {
            MockPlatformResponse<Map<String, Object>> platformResponse = mockOpenPlatformClient.invoke(
                    headers.getAppId(),
                    headers.getTimestamp(),
                    headers.getNonce(),
                    headers.getSignType(),
                    headers.getSign(),
                    request.getBizParams()
            );

            ThirdInvokeResponse response = parseResponse(requestNo, platformResponse);
            log.info("第三方接口调用完成，requestNo={}，success={}，thirdCode={}",
                    requestNo, response.getSuccess(), response.getThirdCode());
            return response;
        } catch (Exception ex) {
            log.error("第三方接口调用异常，requestNo={}，platformCode={}，apiCode={}",
                    requestNo, request.getPlatformCode(), request.getApiCode(), ex);
            throw new ThirdPlatformException(ThirdErrorCode.THIRD_API_INVOKE_ERROR, "第三方接口调用异常");
        }
    }

    /**
     * 校验请求参数
     *
     * @param request 调用请求
     */
    private void validateRequest(ThirdInvokeRequest request) {
        if (request == null) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        if (StrUtil.isBlank(request.getPlatformCode())) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "平台编码不能为空");
        }
        if (StrUtil.isBlank(request.getApiCode())) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "接口编码不能为空");
        }
        if (MapUtil.isEmpty(request.getBizParams())) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "业务参数不能为空");
        }
    }

    /**
     * 解析第三方响应
     *
     * @param requestNo        请求流水号
     * @param platformResponse 第三方响应
     * @return 系统统一响应
     */
    private ThirdInvokeResponse parseResponse(String requestNo, MockPlatformResponse<Map<String, Object>> platformResponse) {
        if (platformResponse == null) {
            throw new ThirdPlatformException(ThirdErrorCode.THIRD_EMPTY_RESPONSE, "第三方平台响应为空");
        }

        boolean success = StrUtil.equals(platformResponse.getCode(), "SUCCESS");
        if (!success) {
            ThirdErrorCode errorCode = ThirdErrorCode.fromThirdCode(platformResponse.getCode());
            log.warn("第三方接口返回失败，requestNo={}，thirdCode={}，thirdMessage={}",
                    requestNo, platformResponse.getCode(), platformResponse.getMessage());
            throw new ThirdPlatformException(errorCode, platformResponse.getMessage());
        }

        return ThirdInvokeResponse.builder()
                .success(true)
                .requestNo(requestNo)
                .thirdCode(platformResponse.getCode())
                .thirdMessage(platformResponse.getMessage())
                .data(JSONUtil.toJsonStr(platformResponse.getData()))
                .build();
    }
}
```

上面的实现先完成核心主链路。日志落库可以在后续 `ThirdApiLogMapper` 章节中补充，失败补偿可以在 RabbitMQ 章节中补充。

### 错误码映射处理

不同第三方平台的错误码风格通常不一致，例如：

```text
SUCCESS：成功
INVALID_SIGN：签名错误
APP_DISABLED：应用已禁用
LIMIT_EXCEEDED：接口限流
SYSTEM_ERROR：第三方系统异常
```

业务系统不应该在代码中到处判断第三方原始错误码，建议统一映射成本系统内部错误码。

文件位置：`src/main/java/io/github/atengk/third/enums/ThirdErrorCode.java`

该枚举用于维护系统内部错误码，并提供第三方错误码映射能力。

```java
package io.github.atengk.third.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 第三方平台错误码
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
public enum ThirdErrorCode {

    PARAM_ERROR("THIRD_400", "请求参数错误"),

    PLATFORM_CONFIG_NOT_FOUND("THIRD_404", "第三方平台配置不存在"),

    PLATFORM_DISABLED("THIRD_405", "第三方平台已禁用"),

    THIRD_EMPTY_RESPONSE("THIRD_500", "第三方平台响应为空"),

    THIRD_API_INVOKE_ERROR("THIRD_501", "第三方接口调用异常"),

    THIRD_INVALID_SIGN("THIRD_601", "第三方签名错误"),

    THIRD_APP_DISABLED("THIRD_602", "第三方应用已禁用"),

    THIRD_LIMIT_EXCEEDED("THIRD_603", "第三方接口触发限流"),

    THIRD_SYSTEM_ERROR("THIRD_604", "第三方系统异常"),

    THIRD_UNKNOWN_ERROR("THIRD_699", "第三方未知异常");

    private final String code;

    private final String message;

    ThirdErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据第三方错误码映射系统错误码
     *
     * @param thirdCode 第三方错误码
     * @return 系统错误码
     */
    public static ThirdErrorCode fromThirdCode(String thirdCode) {
        if (StrUtil.isBlank(thirdCode)) {
            return THIRD_UNKNOWN_ERROR;
        }

        return switch (thirdCode) {
            case "INVALID_SIGN" -> THIRD_INVALID_SIGN;
            case "APP_DISABLED" -> THIRD_APP_DISABLED;
            case "LIMIT_EXCEEDED" -> THIRD_LIMIT_EXCEEDED;
            case "SYSTEM_ERROR" -> THIRD_SYSTEM_ERROR;
            default -> THIRD_UNKNOWN_ERROR;
        };
    }
}
```

文件位置：`src/main/java/io/github/atengk/third/common/exception/ThirdPlatformException.java`

该异常类用于在第三方接口调用、响应解析、验签失败等场景中统一抛出业务异常。

```java
package io.github.atengk.third.common.exception;

import io.github.atengk.third.enums.ThirdErrorCode;
import lombok.Getter;

/**
 * 第三方平台业务异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
public class ThirdPlatformException extends RuntimeException {

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构建第三方平台异常
     *
     * @param errorCode 错误码枚举
     */
    public ThirdPlatformException(ThirdErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 构建第三方平台异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public ThirdPlatformException(ThirdErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }
}
```

如果需要把异常统一返回给前端，可以增加全局异常处理器。

文件位置：`src/main/java/io/github/atengk/third/common/exception/GlobalExceptionHandler.java`

该异常处理器用于把第三方平台异常转换成统一 JSON 响应。

```java
package io.github.atengk.third.common.exception;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

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
     * 处理第三方平台业务异常
     *
     * @param ex 第三方平台异常
     * @return 错误响应
     */
    @ExceptionHandler(ThirdPlatformException.class)
    public Map<String, Object> handleThirdPlatformException(ThirdPlatformException ex) {
        log.warn("第三方平台业务异常，code={}，message={}", ex.getCode(), ex.getMessage());
        return MapUtil.builder("success", false)
                .put("code", ex.getCode())
                .put("message", ex.getMessage())
                .build();
    }

    /**
     * 处理系统未知异常
     *
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(Exception ex) {
        log.error("系统异常", ex);
        return MapUtil.builder("success", false)
                .put("code", "SYSTEM_ERROR")
                .put("message", "系统异常")
                .build();
    }
}
```

到这里，主动调用第三方接口的核心链路已经具备：

```text
业务参数
-> 组装公共参数
-> 生成 HMAC-SHA256 签名
-> 封装请求头
-> OpenFeign 发起调用
-> 解析第三方响应
-> 映射错误码
-> 返回系统统一结果
```

## 异常重试与限流控制

第三方平台接口不稳定是常态，常见问题包括超时、偶发 5xx、接口限流、网络抖动和第三方系统维护。本案例使用 `Resilience4j` 处理短时间异常重试，使用 `Redis` 做本系统侧限流，使用 `RabbitMQ` 投递最终失败消息，方便后续异步补偿。该部分对应 README 中第 26 个场景提到的“接口超时、失败重试、限流控制、错误码映射”等核心难点。

### Resilience4j 重试配置

Resilience4j 适合处理短时间、可恢复的异常，例如连接超时、读取超时、第三方偶发 500。不要对所有错误都重试，例如签名错误、参数错误、应用禁用这类业务错误不应该重试。

配置文件位置：`src/main/resources/application.yml`

```yaml
resilience4j:
  retry:
    instances:
      thirdApiRetry:
        # 最多执行 3 次：1 次原始调用 + 2 次重试
        max-attempts: 3
        # 每次重试间隔
        wait-duration: 1s
        # 只对这些异常触发重试
        retry-exceptions:
          - feign.RetryableException
          - java.net.SocketTimeoutException
          - java.io.IOException
        # 参数错误、签名错误等业务异常不重试
        ignore-exceptions:
          - io.github.atengk.third.common.exception.ThirdPlatformException

  circuitbreaker:
    instances:
      thirdApiCircuitBreaker:
        # 基于最近 20 次请求计算失败率
        sliding-window-size: 20
        # 最少请求数达到 10 次后才计算失败率
        minimum-number-of-calls: 10
        # 失败率达到 50% 后打开熔断
        failure-rate-threshold: 50
        # 熔断打开后等待 30 秒进入半开状态
        wait-duration-in-open-state: 30s
        # 半开状态允许 5 个请求探测恢复情况
        permitted-number-of-calls-in-half-open-state: 5
```

为了避免在 `ServiceImpl` 内部自调用导致注解不生效，建议把真正的 Feign 调用单独放到一个 Spring Bean 中。

文件位置：`src/main/java/io/github/atengk/third/service/ThirdRemoteInvoker.java`

该组件用于封装第三方远程调用，并通过 Resilience4j 对 Feign 调用增加重试和熔断保护。

```java
package io.github.atengk.third.service;

import io.github.atengk.third.dto.MockPlatformResponse;
import io.github.atengk.third.dto.ThirdSignHeaders;
import io.github.atengk.third.feign.MockOpenPlatformClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 第三方远程调用组件
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThirdRemoteInvoker {

    private final MockOpenPlatformClient mockOpenPlatformClient;

    /**
     * 调用模拟第三方平台接口
     *
     * @param requestNo 请求流水号
     * @param headers   签名请求头
     * @param body      请求体
     * @return 第三方响应
     */
    @Retry(name = "thirdApiRetry", fallbackMethod = "invokeFallback")
    @CircuitBreaker(name = "thirdApiCircuitBreaker", fallbackMethod = "invokeFallback")
    public MockPlatformResponse<Map<String, Object>> invokeMock(String requestNo,
                                                                ThirdSignHeaders headers,
                                                                Map<String, Object> body) {
        log.info("执行第三方远程调用，requestNo={}，appId={}", requestNo, headers.getAppId());
        return mockOpenPlatformClient.invoke(
                headers.getAppId(),
                headers.getTimestamp(),
                headers.getNonce(),
                headers.getSignType(),
                headers.getSign(),
                body
        );
    }

    /**
     * 第三方调用降级处理
     *
     * @param requestNo 请求流水号
     * @param headers   签名请求头
     * @param body      请求体
     * @param ex        异常
     * @return 降级响应
     */
    public MockPlatformResponse<Map<String, Object>> invokeFallback(String requestNo,
                                                                    ThirdSignHeaders headers,
                                                                    Map<String, Object> body,
                                                                    Throwable ex) {
        log.error("第三方接口调用触发降级，requestNo={}，appId={}", requestNo, headers.getAppId(), ex);
        return MockPlatformResponse.<Map<String, Object>>builder()
                .code("SYSTEM_ERROR")
                .message("第三方接口暂不可用")
                .requestId(requestNo)
                .build();
    }
}
```

原来的 `ThirdInvokeServiceImpl` 中，把直接调用 Feign 的代码替换为调用 `ThirdRemoteInvoker`：

```java
MockPlatformResponse<Map<String, Object>> platformResponse = thirdRemoteInvoker.invokeMock(
        requestNo,
        headers,
        request.getBizParams()
);
```

这样可以保证重试、熔断、降级逻辑在 Spring AOP 代理中正常生效。

### Redis 限流实现

本系统侧限流用于保护第三方平台和自身系统。例如某个短信平台限制每秒最多 20 次调用，本系统应该在调用前先做限流，而不是等第三方返回限流错误。

Redis Key 设计：

```text
third:invoke:limit:{platformCode}:{apiCode}
```

限流策略：

```text
1. 按 platformCode + apiCode 维度限流
2. 使用 Redis INCR 统计窗口内请求次数
3. 第一次请求时设置过期时间
4. 超过阈值直接拒绝，不再调用第三方
```

文件位置：`src/main/java/io/github/atengk/third/common/constant/ThirdRedisKeyConstant.java`

该类统一维护第三方平台相关 Redis Key，避免业务代码中硬编码。

```java
package io.github.atengk.third.common.constant;

/**
 * 第三方平台 Redis Key 常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThirdRedisKeyConstant {

    public static final String INVOKE_LIMIT_KEY = "third:invoke:limit:%s:%s";

    public static final String CALLBACK_IDEMPOTENT_KEY = "third:callback:idempotent:%s";

    public static final String CALLBACK_NONCE_KEY = "third:callback:nonce:%s:%s";

    private ThirdRedisKeyConstant() {
    }
}
```

文件位置：`src/main/java/io/github/atengk/third/service/ThirdRateLimitService.java`

该服务用于对第三方接口主动调用做 Redis 计数限流。

```java
package io.github.atengk.third.service;

/**
 * 第三方接口限流服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ThirdRateLimitService {

    /**
     * 检查是否允许调用
     *
     * @param platformCode 平台编码
     * @param apiCode      接口编码
     * @param limitCount   窗口最大请求数
     * @param windowSeconds 窗口秒数
     * @return 是否允许调用
     */
    boolean allowInvoke(String platformCode, String apiCode, long limitCount, long windowSeconds);
}
```

文件位置：`src/main/java/io/github/atengk/third/service/impl/ThirdRateLimitServiceImpl.java`

该实现使用 Redis `increment` 实现固定窗口限流。

```java
package io.github.atengk.third.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.third.common.constant.ThirdRedisKeyConstant;
import io.github.atengk.third.service.ThirdRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 第三方接口限流服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdRateLimitServiceImpl implements ThirdRateLimitService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 检查是否允许调用
     *
     * @param platformCode  平台编码
     * @param apiCode       接口编码
     * @param limitCount    窗口最大请求数
     * @param windowSeconds 窗口秒数
     * @return 是否允许调用
     */
    @Override
    public boolean allowInvoke(String platformCode, String apiCode, long limitCount, long windowSeconds) {
        String key = StrUtil.format(ThirdRedisKeyConstant.INVOKE_LIMIT_KEY, platformCode, apiCode);
        Long count = stringRedisTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        boolean allowed = count != null && count <= limitCount;
        if (!allowed) {
            log.warn("第三方接口触发本地限流，platformCode={}，apiCode={}，count={}，limit={}",
                    platformCode, apiCode, count, limitCount);
        }
        return allowed;
    }
}
```

在 `ThirdInvokeServiceImpl` 调用前增加限流判断：

```java
boolean allowed = thirdRateLimitService.allowInvoke(
        request.getPlatformCode(),
        request.getApiCode(),
        20,
        1
);
if (!allowed) {
    throw new ThirdPlatformException(ThirdErrorCode.THIRD_LIMIT_EXCEEDED, "第三方接口调用过于频繁");
}
```

### 失败消息投递 RabbitMQ

如果第三方接口经过短重试后仍然失败，不建议一直阻塞当前请求。可以把失败消息投递到 RabbitMQ，后续由消费者或定时补偿任务处理。

文件位置：`src/main/java/io/github/atengk/third/mq/ThirdMqConstant.java`

该类统一维护第三方接口失败补偿相关的交换机、队列和路由键。

```java
package io.github.atengk.third.mq;

/**
 * 第三方平台 MQ 常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThirdMqConstant {

    public static final String THIRD_RETRY_EXCHANGE = "third.retry.exchange";

    public static final String THIRD_RETRY_QUEUE = "third.retry.queue";

    public static final String THIRD_RETRY_ROUTING_KEY = "third.retry";

    private ThirdMqConstant() {
    }
}
```

文件位置：`src/main/java/io/github/atengk/third/mq/ThirdRetryMessage.java`

该消息对象用于保存第三方接口失败后的补偿参数。

```java
package io.github.atengk.third.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 第三方接口重试消息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdRetryMessage {

    /**
     * 请求流水号
     */
    private String requestNo;

    /**
     * 平台编码
     */
    private String platformCode;

    /**
     * 接口编码
     */
    private String apiCode;

    /**
     * 业务参数
     */
    private Map<String, Object> bizParams;

    /**
     * 当前重试次数
     */
    private Integer retryCount;

    /**
     * 失败原因
     */
    private String errorMsg;
}
```

文件位置：`src/main/java/io/github/atengk/third/config/RabbitConfig.java`

该配置声明失败补偿交换机和队列。

```java
package io.github.atengk.third.config;

import io.github.atengk.third.mq.ThirdMqConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 第三方平台 RabbitMQ 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class RabbitConfig {

    /**
     * 第三方失败补偿交换机
     *
     * @return 交换机
     */
    @Bean
    public DirectExchange thirdRetryExchange() {
        return new DirectExchange(ThirdMqConstant.THIRD_RETRY_EXCHANGE, true, false);
    }

    /**
     * 第三方失败补偿队列
     *
     * @return 队列
     */
    @Bean
    public Queue thirdRetryQueue() {
        return QueueBuilder.durable(ThirdMqConstant.THIRD_RETRY_QUEUE).build();
    }

    /**
     * 绑定失败补偿队列
     *
     * @return 绑定关系
     */
    @Bean
    public Binding thirdRetryBinding() {
        return BindingBuilder.bind(thirdRetryQueue())
                .to(thirdRetryExchange())
                .with(ThirdMqConstant.THIRD_RETRY_ROUTING_KEY);
    }
}
```

文件位置：`src/main/java/io/github/atengk/third/mq/ThirdRetryProducer.java`

该生产者用于在第三方接口最终失败后投递补偿消息。

```java
package io.github.atengk.third.mq;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 第三方接口重试消息生产者
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThirdRetryProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送第三方接口重试消息
     *
     * @param message 重试消息
     */
    public void sendRetryMessage(ThirdRetryMessage message) {
        rabbitTemplate.convertAndSend(
                ThirdMqConstant.THIRD_RETRY_EXCHANGE,
                ThirdMqConstant.THIRD_RETRY_ROUTING_KEY,
                JSONUtil.toJsonStr(message)
        );
        log.info("已投递第三方接口失败补偿消息，requestNo={}，platformCode={}，apiCode={}",
                message.getRequestNo(), message.getPlatformCode(), message.getApiCode());
    }
}
```

在调用异常处投递失败消息：

```java
thirdRetryProducer.sendRetryMessage(ThirdRetryMessage.builder()
        .requestNo(requestNo)
        .platformCode(request.getPlatformCode())
        .apiCode(request.getApiCode())
        .bizParams(request.getBizParams())
        .retryCount(0)
        .errorMsg(ex.getMessage())
        .build());
```

## 回调验签实现

第三方回调接口不能依赖登录态，必须通过签名验签确认请求来源。验签通过后，还需要做时间戳校验、nonce 防重放和事件号幂等处理。

### 回调参数接收

回调接口建议同时接收 Header 和 Body。Header 中放签名参数，Body 中放业务事件数据。

文件位置：`src/main/java/io/github/atengk/third/dto/ThirdCallbackRequest.java`

该类用于封装第三方回调业务参数。

```java
package io.github.atengk.third.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 第三方回调请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdCallbackRequest {

    /**
     * 第三方回调事件号
     */
    private String eventNo;

    /**
     * 回调类型
     */
    private String callbackType;

    /**
     * 回调发生时间，毫秒时间戳
     */
    private Long callbackTime;

    /**
     * 回调业务数据
     */
    private Map<String, Object> data;
}
```

文件位置：`src/main/java/io/github/atengk/third/controller/ThirdCallbackController.java`

该 Controller 用于接收第三方平台回调，并把 Header 与 Body 交给业务服务处理。

```java
package io.github.atengk.third.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.third.dto.ThirdCallbackRequest;
import io.github.atengk.third.dto.ThirdSignHeaders;
import io.github.atengk.third.service.ThirdCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 第三方回调接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/third/callback")
public class ThirdCallbackController {

    private final ThirdCallbackService thirdCallbackService;

    /**
     * 接收模拟平台回调
     *
     * @param platformCode 平台编码
     * @param appId        应用ID
     * @param timestamp    时间戳
     * @param nonce        随机串
     * @param signType     签名类型
     * @param sign         签名
     * @param request      回调请求
     * @return 处理结果
     */
    @PostMapping("/{platformCode}")
    public Map<String, Object> receiveCallback(@PathVariable String platformCode,
                                               @RequestHeader("X-App-Id") String appId,
                                               @RequestHeader("X-Timestamp") String timestamp,
                                               @RequestHeader("X-Nonce") String nonce,
                                               @RequestHeader("X-Sign-Type") String signType,
                                               @RequestHeader("X-Sign") String sign,
                                               @RequestBody ThirdCallbackRequest request) {
        ThirdSignHeaders headers = ThirdSignHeaders.builder()
                .appId(appId)
                .timestamp(timestamp)
                .nonce(nonce)
                .signType(signType)
                .sign(sign)
                .build();

        thirdCallbackService.handleCallback(platformCode, headers, request);

        return MapUtil.builder("code", "SUCCESS")
                .put("message", "success")
                .build();
    }
}
```

### 签名合法性校验

验签需要使用第三方平台配置中的 `app_secret`，通常根据 `X-App-Id` 查询平台配置。

文件位置：`src/main/java/io/github/atengk/third/service/ThirdCallbackService.java`

该接口定义第三方回调处理入口。

```java
package io.github.atengk.third.service;

import io.github.atengk.third.dto.ThirdCallbackRequest;
import io.github.atengk.third.dto.ThirdSignHeaders;

/**
 * 第三方回调服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ThirdCallbackService {

    /**
     * 处理第三方回调
     *
     * @param platformCode 平台编码
     * @param headers      签名请求头
     * @param request      回调请求
     */
    void handleCallback(String platformCode, ThirdSignHeaders headers, ThirdCallbackRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/third/service/impl/ThirdCallbackServiceImpl.java`

该实现类完成回调验签、时间戳校验、nonce 防重放、事件幂等和回调落库。

```java
package io.github.atengk.third.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.third.common.constant.ThirdRedisKeyConstant;
import io.github.atengk.third.common.exception.ThirdPlatformException;
import io.github.atengk.third.dto.ThirdCallbackRequest;
import io.github.atengk.third.dto.ThirdSignHeaders;
import io.github.atengk.third.entity.ThirdCallbackRecord;
import io.github.atengk.third.entity.ThirdPlatformConfig;
import io.github.atengk.third.enums.ThirdErrorCode;
import io.github.atengk.third.mapper.ThirdCallbackRecordMapper;
import io.github.atengk.third.service.ThirdCallbackService;
import io.github.atengk.third.service.ThirdPlatformConfigService;
import io.github.atengk.third.util.ThirdDesensitizeUtil;
import io.github.atengk.third.util.ThirdRequestUtil;
import io.github.atengk.third.util.ThirdSignUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * 第三方回调服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdCallbackServiceImpl implements ThirdCallbackService {

    private static final long CALLBACK_TIMESTAMP_OFFSET_MILLIS = 5 * 60 * 1000L;

    private final StringRedisTemplate stringRedisTemplate;
    private final ThirdPlatformConfigService thirdPlatformConfigService;
    private final ThirdCallbackRecordMapper thirdCallbackRecordMapper;

    /**
     * 处理第三方回调
     *
     * @param platformCode 平台编码
     * @param headers      签名请求头
     * @param request      回调请求
     */
    @Override
    public void handleCallback(String platformCode, ThirdSignHeaders headers, ThirdCallbackRequest request) {
        validateCallback(platformCode, headers, request);

        ThirdPlatformConfig config = thirdPlatformConfigService.getEnabledByAppId(headers.getAppId());
        if (!StrUtil.equals(config.getPlatformCode(), platformCode)) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "平台编码和AppId不匹配");
        }

        checkTimestamp(headers.getTimestamp());
        checkNonce(headers);

        Map<String, Object> bodyMap = BeanUtil.beanToMap(request, false, true);
        Map<String, Object> signParams = ThirdRequestUtil.buildSignParams(bodyMap, headers);
        boolean verified = ThirdSignUtil.verifyHmacSha256(signParams, config.getAppSecret(), headers.getSign());

        if (!verified) {
            saveCallbackRecord(platformCode, headers, request, 2, 2, "回调验签失败");
            log.warn("第三方回调验签失败，platformCode={}，appId={}，eventNo={}",
                    platformCode, headers.getAppId(), request.getEventNo());
            throw new ThirdPlatformException(ThirdErrorCode.THIRD_INVALID_SIGN, "回调验签失败");
        }

        boolean firstHandle = markIdempotent(request.getEventNo());
        if (!firstHandle) {
            log.info("第三方回调重复通知，platformCode={}，eventNo={}", platformCode, request.getEventNo());
            return;
        }

        saveCallbackRecord(platformCode, headers, request, 1, 1, null);
        log.info("第三方回调处理成功，platformCode={}，callbackType={}，eventNo={}",
                platformCode, request.getCallbackType(), request.getEventNo());
    }

    /**
     * 校验回调基础参数
     *
     * @param platformCode 平台编码
     * @param headers      签名请求头
     * @param request      回调请求
     */
    private void validateCallback(String platformCode, ThirdSignHeaders headers, ThirdCallbackRequest request) {
        if (StrUtil.isBlank(platformCode)) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "平台编码不能为空");
        }
        if (headers == null || StrUtil.hasBlank(headers.getAppId(), headers.getTimestamp(), headers.getNonce(), headers.getSign())) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "签名请求头不完整");
        }
        if (request == null || StrUtil.hasBlank(request.getEventNo(), request.getCallbackType())) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "回调参数不完整");
        }
    }

    /**
     * 校验时间戳偏差
     *
     * @param timestamp 时间戳
     */
    private void checkTimestamp(String timestamp) {
        long requestTime = Long.parseLong(timestamp);
        long now = System.currentTimeMillis();
        if (Math.abs(now - requestTime) > CALLBACK_TIMESTAMP_OFFSET_MILLIS) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "回调时间戳已过期");
        }
    }

    /**
     * 校验 nonce 防重放
     *
     * @param headers 签名请求头
     */
    private void checkNonce(ThirdSignHeaders headers) {
        String nonceKey = StrUtil.format(ThirdRedisKeyConstant.CALLBACK_NONCE_KEY, headers.getAppId(), headers.getNonce());
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(nonceKey, "1", Duration.ofMinutes(5));
        if (!Boolean.TRUE.equals(success)) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "重复回调请求");
        }
    }

    /**
     * 标记回调幂等
     *
     * @param eventNo 事件号
     * @return 是否首次处理
     */
    private boolean markIdempotent(String eventNo) {
        String idempotentKey = StrUtil.format(ThirdRedisKeyConstant.CALLBACK_IDEMPOTENT_KEY, eventNo);
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", Duration.ofDays(1));
        return Boolean.TRUE.equals(success);
    }

    /**
     * 保存回调记录
     *
     * @param platformCode  平台编码
     * @param headers       签名请求头
     * @param request       回调请求
     * @param verifyStatus  验签状态
     * @param processStatus 处理状态
     * @param errorMsg      错误信息
     */
    private void saveCallbackRecord(String platformCode,
                                    ThirdSignHeaders headers,
                                    ThirdCallbackRequest request,
                                    Integer verifyStatus,
                                    Integer processStatus,
                                    String errorMsg) {
        try {
            ThirdCallbackRecord record = new ThirdCallbackRecord();
            record.setEventNo(request.getEventNo());
            record.setPlatformCode(platformCode);
            record.setAppId(headers.getAppId());
            record.setCallbackType(request.getCallbackType());
            record.setNonce(headers.getNonce());
            record.setSign(headers.getSign());
            record.setRequestBody(ThirdDesensitizeUtil.desensitizeJson(JSONUtil.toJsonStr(request)));
            record.setVerifyStatus(verifyStatus);
            record.setProcessStatus(processStatus);
            record.setErrorMsg(errorMsg);
            record.setCallbackTime(request.getCallbackTime() == null ? null : DateUtil.date(request.getCallbackTime()).toJdkDate());
            record.setProcessedAt(DateUtil.date().toJdkDate());
            thirdCallbackRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            log.info("回调记录已存在，eventNo={}", request.getEventNo());
        }
    }
}
```

### 回调幂等处理

回调幂等建议使用两层保障：

```text
第一层：Redis SETNX 快速拦截重复 eventNo
第二层：数据库 uk_event_no 唯一索引兜底
```

Redis 幂等适合拦截短时间重复通知，数据库唯一索引适合兜底防并发穿透。

核心代码：

```java
String idempotentKey = StrUtil.format(ThirdRedisKeyConstant.CALLBACK_IDEMPOTENT_KEY, eventNo);
Boolean success = stringRedisTemplate.opsForValue()
        .setIfAbsent(idempotentKey, "1", Duration.ofDays(1));
return Boolean.TRUE.equals(success);
```

数据库兜底：

```sql
UNIQUE KEY uk_event_no (event_no)
```

真实业务中，如果第三方平台可能在多天后补发同一个事件，`eventNo` 的 Redis 过期时间可以设置为 7 天、30 天，或者完全依赖数据库唯一索引。

### 回调数据落库

回调落库建议在验签失败时也记录一条数据，原因是生产问题排查时经常需要确认是密钥错误、参数排序错误、时间戳过期，还是恶意请求。

文件位置：`src/main/java/io/github/atengk/third/entity/ThirdCallbackRecord.java`

该实体对应 `third_callback_record` 表。

```java
package io.github.atengk.third.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 第三方回调通知记录
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("third_callback_record")
public class ThirdCallbackRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventNo;

    private String platformCode;

    private String appId;

    private String callbackType;

    private String nonce;

    private String sign;

    private String requestBody;

    private Integer verifyStatus;

    private Integer processStatus;

    private String errorMsg;

    private Date callbackTime;

    private Date processedAt;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    @TableLogic
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/third/mapper/ThirdCallbackRecordMapper.java`

```java
package io.github.atengk.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.third.entity.ThirdCallbackRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方回调通知记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ThirdCallbackRecordMapper extends BaseMapper<ThirdCallbackRecord> {
}
```

## 请求日志与脱敏

第三方接口日志用于排查问题，但不能直接把手机号、身份证、银行卡、密钥、签名等敏感信息完整写入数据库或日志文件。本案例使用 Hutool 脱敏工具对常见字段做处理。

### 请求日志记录

主动调用第三方接口时，建议在调用前先插入一条“处理中”日志，调用完成后再更新为成功或失败。

文件位置：`src/main/java/io/github/atengk/third/entity/ThirdApiLog.java`

该实体对应 `third_api_log` 表，用于保存主动调用第三方接口日志。

```java
package io.github.atengk.third.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 第三方接口调用日志
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("third_api_log")
public class ThirdApiLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestNo;

    private String platformCode;

    private String apiCode;

    private String apiUrl;

    private String httpMethod;

    private String appId;

    private String nonce;

    private String sign;

    private String requestBody;

    private String responseBody;

    private Integer status;

    private String errorCode;

    private String errorMsg;

    private Long costMs;

    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    @TableLogic
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/third/mapper/ThirdApiLogMapper.java`

```java
package io.github.atengk.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.third.entity.ThirdApiLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方接口调用日志 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ThirdApiLogMapper extends BaseMapper<ThirdApiLog> {
}
```

在 `ThirdInvokeServiceImpl` 中新增调用前日志：

```java
ThirdApiLog logEntity = new ThirdApiLog();
logEntity.setRequestNo(requestNo);
logEntity.setPlatformCode(request.getPlatformCode());
logEntity.setApiCode(request.getApiCode());
logEntity.setApiUrl(config.getBaseUrl() + "/open/api/invoke");
logEntity.setHttpMethod("POST");
logEntity.setAppId(config.getAppId());
logEntity.setNonce(headers.getNonce());
logEntity.setSign(headers.getSign());
logEntity.setRequestBody(ThirdDesensitizeUtil.desensitizeJson(JSONUtil.toJsonStr(request.getBizParams())));
logEntity.setStatus(0);
logEntity.setRetryCount(0);
thirdApiLogMapper.insert(logEntity);
```

### 响应日志记录

响应日志在调用成功或失败后更新。成功时保存第三方响应，失败时保存错误码和错误信息。

成功更新示例：

```java
long costMs = System.currentTimeMillis() - startTime;

ThirdApiLog updateLog = new ThirdApiLog();
updateLog.setId(logEntity.getId());
updateLog.setStatus(1);
updateLog.setCostMs(costMs);
updateLog.setResponseBody(ThirdDesensitizeUtil.desensitizeJson(JSONUtil.toJsonStr(platformResponse)));
thirdApiLogMapper.updateById(updateLog);
```

失败更新示例：

```java
long costMs = System.currentTimeMillis() - startTime;

ThirdApiLog updateLog = new ThirdApiLog();
updateLog.setId(logEntity.getId());
updateLog.setStatus(2);
updateLog.setCostMs(costMs);
updateLog.setErrorCode("THIRD_501");
updateLog.setErrorMsg(ex.getMessage());
thirdApiLogMapper.updateById(updateLog);
```

建议记录的日志字段：

| 字段            | 说明                                    |
| --------------- | --------------------------------------- |
| `request_no`    | 用于串联业务日志、接口日志、MQ 补偿日志 |
| `platform_code` | 快速定位是哪个第三方平台                |
| `api_code`      | 快速定位是哪个接口                      |
| `request_body`  | 保存脱敏后的请求参数                    |
| `response_body` | 保存脱敏后的响应参数                    |
| `status`        | 支持失败日志查询和补偿                  |
| `cost_ms`       | 分析第三方接口性能                      |
| `error_msg`     | 保存异常摘要                            |

### 敏感字段脱敏

脱敏工具建议根据字段名识别敏感字段，而不是只按值判断。常见敏感字段包括 `phone`、`mobile`、`idCard`、`bankCard`、`appSecret`、`secret`、`sign`。

文件位置：`src/main/java/io/github/atengk/third/util/ThirdDesensitizeUtil.java`

该工具类用于对请求日志、响应日志和回调报文中的敏感字段进行脱敏。

```java
package io.github.atengk.third.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.Map;

/**
 * 第三方接口日志脱敏工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThirdDesensitizeUtil {

    private ThirdDesensitizeUtil() {
    }

    /**
     * 脱敏 JSON 字符串
     *
     * @param json JSON 字符串
     * @return 脱敏后的 JSON 字符串
     */
    public static String desensitizeJson(String json) {
        if (StrUtil.isBlank(json) || !JSONUtil.isTypeJSON(json)) {
            return json;
        }

        Object parsed = JSONUtil.parse(json);
        if (parsed instanceof cn.hutool.json.JSONObject jsonObject) {
            desensitizeMap(jsonObject);
            return jsonObject.toString();
        }
        return json;
    }

    /**
     * 脱敏 Map 数据
     *
     * @param map Map 数据
     */
    private static void desensitizeMap(Map<String, Object> map) {
        map.forEach((key, value) -> {
            if (value instanceof Map<?, ?> childMap) {
                desensitizeMap((Map<String, Object>) childMap);
                return;
            }

            if (value == null) {
                return;
            }

            String valueText = String.valueOf(value);
            if (StrUtil.containsAnyIgnoreCase(key, "phone", "mobile")) {
                map.put(key, DesensitizedUtil.mobilePhone(valueText));
            } else if (StrUtil.containsAnyIgnoreCase(key, "idCard", "certNo")) {
                map.put(key, DesensitizedUtil.idCardNum(valueText, 3, 4));
            } else if (StrUtil.containsAnyIgnoreCase(key, "bankCard", "cardNo")) {
                map.put(key, DesensitizedUtil.bankCard(valueText));
            } else if (StrUtil.containsAnyIgnoreCase(key, "secret", "token", "sign")) {
                map.put(key, StrUtil.hide(valueText, 4, Math.max(4, valueText.length() - 4)));
            }
        });
    }
}
```

脱敏前：

```json
{
  "phone": "13800138000",
  "content": "您的验证码是 123456",
  "sign": "d8e4a2f9c11a88776655"
}
```

脱敏后：

```json
{
  "phone": "138****8000",
  "content": "您的验证码是 123456",
  "sign": "d8e4************6655"
}
```

## 对外接口设计

本案例对外暴露三个核心接口：发起第三方调用、查询调用日志、接收第三方回调。生产环境中，主动调用接口通常只提供给内部服务使用；回调接口提供给第三方平台调用。

### 发起第三方调用接口

文件位置：`src/main/java/io/github/atengk/third/controller/ThirdApiController.java`

该 Controller 提供主动调用第三方平台的接口。

```java
package io.github.atengk.third.controller;

import io.github.atengk.third.dto.ThirdInvokeRequest;
import io.github.atengk.third.dto.ThirdInvokeResponse;
import io.github.atengk.third.service.ThirdInvokeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 第三方接口调用入口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/third")
public class ThirdApiController {

    private final ThirdInvokeService thirdInvokeService;

    /**
     * 发起第三方接口调用
     *
     * @param request 调用请求
     * @return 调用响应
     */
    @PostMapping("/invoke")
    public ThirdInvokeResponse invoke(@RequestBody ThirdInvokeRequest request) {
        return thirdInvokeService.invoke(request);
    }
}
```

接口说明：

| 项目         | 说明                           |
| ------------ | ------------------------------ |
| 请求方式     | `POST`                         |
| 请求路径     | `/api/third/invoke`            |
| Content-Type | `application/json`             |
| 用途         | 内部业务系统发起第三方接口调用 |

请求示例：

```json
{
  "platformCode": "MOCK_OPEN",
  "apiCode": "SEND_SMS",
  "bizNo": "SMS202605150001",
  "bizParams": {
    "bizNo": "SMS202605150001",
    "phone": "13800138000",
    "content": "您的验证码是 123456"
  }
}
```

调用命令：

```bash
curl -X POST 'http://localhost:8080/api/third/invoke' \
  -H 'Content-Type: application/json' \
  -d '{
    "platformCode": "MOCK_OPEN",
    "apiCode": "SEND_SMS",
    "bizNo": "SMS202605150001",
    "bizParams": {
      "bizNo": "SMS202605150001",
      "phone": "13800138000",
      "content": "您的验证码是 123456"
    }
  }'
```

成功响应：

```json
{
  "success": true,
  "requestNo": "SEND_SMS202605151230001234567890",
  "thirdCode": "SUCCESS",
  "thirdMessage": "处理成功",
  "data": "{\"thirdBizNo\":\"TP202605150001\"}"
}
```

失败响应：

```json
{
  "success": false,
  "code": "THIRD_603",
  "message": "第三方接口触发限流"
}
```

### 查询调用日志接口

调用日志查询用于排查第三方接口问题。这里给出按请求流水号查询的最小实现。

文件位置：`src/main/java/io/github/atengk/third/service/ThirdApiLogService.java`

```java
package io.github.atengk.third.service;

import io.github.atengk.third.entity.ThirdApiLog;

/**
 * 第三方接口调用日志服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ThirdApiLogService {

    /**
     * 根据请求流水号查询日志
     *
     * @param requestNo 请求流水号
     * @return 调用日志
     */
    ThirdApiLog getByRequestNo(String requestNo);
}
```

文件位置：`src/main/java/io/github/atengk/third/service/impl/ThirdApiLogServiceImpl.java`

```java
package io.github.atengk.third.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.third.entity.ThirdApiLog;
import io.github.atengk.third.mapper.ThirdApiLogMapper;
import io.github.atengk.third.service.ThirdApiLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 第三方接口调用日志服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Service
@RequiredArgsConstructor
public class ThirdApiLogServiceImpl implements ThirdApiLogService {

    private final ThirdApiLogMapper thirdApiLogMapper;

    /**
     * 根据请求流水号查询日志
     *
     * @param requestNo 请求流水号
     * @return 调用日志
     */
    @Override
    public ThirdApiLog getByRequestNo(String requestNo) {
        if (StrUtil.isBlank(requestNo)) {
            return null;
        }

        return thirdApiLogMapper.selectOne(
                new LambdaQueryWrapper<ThirdApiLog>()
                        .eq(ThirdApiLog::getRequestNo, requestNo)
                        .last("LIMIT 1")
        );
    }
}
```

在 `ThirdApiController` 中补充查询接口：

```java
private final ThirdApiLogService thirdApiLogService;

/**
 * 查询第三方接口调用日志
 *
 * @param requestNo 请求流水号
 * @return 调用日志
 */
@GetMapping("/logs/{requestNo}")
public ThirdApiLog getLog(@PathVariable String requestNo) {
    return thirdApiLogService.getByRequestNo(requestNo);
}
```

接口说明：

| 项目     | 说明                                 |
| -------- | ------------------------------------ |
| 请求方式 | `GET`                                |
| 请求路径 | `/api/third/logs/{requestNo}`        |
| 用途     | 根据请求流水号查询第三方接口调用日志 |

调用命令：

```bash
curl 'http://localhost:8080/api/third/logs/SEND_SMS202605151230001234567890'
```

响应示例：

```json
{
  "id": 1,
  "requestNo": "SEND_SMS202605151230001234567890",
  "platformCode": "MOCK_OPEN",
  "apiCode": "SEND_SMS",
  "apiUrl": "http://localhost:9090/open/api/invoke",
  "httpMethod": "POST",
  "appId": "mock-app-001",
  "nonce": "a3f9c21e8b",
  "sign": "d8e4************6655",
  "requestBody": "{\"phone\":\"138****8000\",\"content\":\"您的验证码是 123456\"}",
  "responseBody": "{\"code\":\"SUCCESS\",\"message\":\"处理成功\"}",
  "status": 1,
  "costMs": 156,
  "retryCount": 0
}
```

### 接收第三方回调接口

回调接口由第三方平台调用，调用方必须携带签名 Header。

接口说明：

| 项目         | 说明                                 |
| ------------ | ------------------------------------ |
| 请求方式     | `POST`                               |
| 请求路径     | `/api/third/callback/{platformCode}` |
| Content-Type | `application/json`                   |
| 用途         | 接收第三方平台回调通知               |

请求头示例：

```text
X-App-Id: mock-app-001
X-Timestamp: 1778817600000
X-Nonce: 8f3a9c12
X-Sign-Type: HMAC_SHA256
X-Sign: d8e4a2f9c11a88776655
```

请求体示例：

```json
{
  "eventNo": "EVT202605150001",
  "callbackType": "SMS_SEND_RESULT",
  "callbackTime": 1778817600000,
  "data": {
    "bizNo": "SMS202605150001",
    "sendStatus": "SUCCESS",
    "phone": "13800138000"
  }
}
```

调用命令示例：

```bash
curl -X POST 'http://localhost:8080/api/third/callback/MOCK_OPEN' \
  -H 'Content-Type: application/json' \
  -H 'X-App-Id: mock-app-001' \
  -H 'X-Timestamp: 1778817600000' \
  -H 'X-Nonce: 8f3a9c12' \
  -H 'X-Sign-Type: HMAC_SHA256' \
  -H 'X-Sign: d8e4a2f9c11a88776655' \
  -d '{
    "eventNo": "EVT202605150001",
    "callbackType": "SMS_SEND_RESULT",
    "callbackTime": 1778817600000,
    "data": {
      "bizNo": "SMS202605150001",
      "sendStatus": "SUCCESS",
      "phone": "13800138000"
    }
  }'
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "success"
}
```

重复回调响应仍然建议返回成功，避免第三方平台持续重试：

```json
{
  "code": "SUCCESS",
  "message": "success"
}
```

验签失败响应：

```json
{
  "success": false,
  "code": "THIRD_601",
  "message": "回调验签失败"
}
```

到这里，这一部分已经覆盖第三方平台对接中最关键的工程闭环：

```text
主动调用限流
-> Resilience4j 短重试和熔断
-> 最终失败投递 RabbitMQ
-> 回调 Header + Body 接收
-> HMAC-SHA256 验签
-> timestamp + nonce 防重放
-> eventNo 幂等控制
-> 回调记录落库
-> 请求和响应日志脱敏保存
```

## 核心代码实现

本节给出一套可以直接落地到 Spring Boot 3 项目中的核心代码骨架，覆盖依赖、配置、实体、Mapper、签名工具、Feign 客户端、业务服务和回调接口。代码重点实现 README 中第 26 个场景要求的“生成请求签名、调用第三方接口、解析响应结果、处理异常重试、接收第三方回调、回调验签并落库”。

### Maven 依赖配置

下面的依赖用于引入 Spring Boot Web、OpenFeign、MyBatis-Plus、Redis、RabbitMQ、Resilience4j、Hutool、Lombok 和 MySQL 驱动。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Web 接口能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- OpenFeign 第三方 HTTP 调用 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>

    <!-- Resilience4j 重试、熔断 -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
    </dependency>

    <!-- AOP 支持，Resilience4j 注解需要 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- MyBatis-Plus 数据访问 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.9</version>
    </dependency>

    <!-- Redis 限流、缓存、幂等 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- RabbitMQ 失败补偿消息 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- Hutool 工具类：签名、JSON、日期、字符串、脱敏 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok 简化实体和构造器代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

如果项目使用 Spring Cloud，需要在 `pom.xml` 中增加版本管理。

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Cloud 版本管理，用于 OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 配置文件示例

下面的配置包含数据库、Redis、RabbitMQ、Feign、Resilience4j 和第三方平台模块参数。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: third-platform-demo

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/third_platform_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3s

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /

  cloud:
    openfeign:
      client:
        config:
          mockOpenPlatformClient:
            # 连接超时，单位毫秒
            connect-timeout: 3000
            # 读取超时，单位毫秒
            read-timeout: 5000
            # 生产环境建议 basic 或 none
            logger-level: basic

mybatis-plus:
  configuration:
    # 开发环境可打开 SQL 日志，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 逻辑删除字段值
      logic-delete-value: 1
      # 逻辑未删除字段值
      logic-not-delete-value: 0

third-platform:
  mock:
    # 模拟第三方平台地址
    base-url: http://localhost:9090

  sign:
    # 回调时间戳允许偏差，单位秒
    timestamp-offset-seconds: 300

  invoke:
    # 本系统侧限流：每秒最多调用次数
    limit-count-per-second: 20
    # 失败补偿最大重试次数
    max-retry-count: 3

resilience4j:
  retry:
    instances:
      thirdApiRetry:
        # 1 次原始调用 + 2 次重试
        max-attempts: 3
        wait-duration: 1s
        retry-exceptions:
          - feign.RetryableException
          - java.net.SocketTimeoutException
          - java.io.IOException

  circuitbreaker:
    instances:
      thirdApiCircuitBreaker:
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 5
```

启动类需要开启 Feign。

文件位置：`src/main/java/io/github/atengk/third/ThirdPlatformApplication.java`

```java
package io.github.atengk.third;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 第三方平台接口对接示例启动类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@EnableFeignClients
@SpringBootApplication
public class ThirdPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThirdPlatformApplication.class, args);
    }
}
```

### 实体类与 Mapper

这里给出三张核心表对应的实体和 Mapper。字段和前面数据库设计保持一致。

文件位置：`src/main/java/io/github/atengk/third/entity/ThirdPlatformConfig.java`

```java
package io.github.atengk.third.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 第三方平台配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("third_platform_config")
public class ThirdPlatformConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platformCode;

    private String platformName;

    private String appId;

    private String appSecret;

    private String baseUrl;

    private String callbackUrl;

    private String signType;

    private Integer timeoutMs;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    @TableLogic
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/third/entity/ThirdApiLog.java`

```java
package io.github.atengk.third.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 第三方接口调用日志
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("third_api_log")
public class ThirdApiLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestNo;

    private String platformCode;

    private String apiCode;

    private String apiUrl;

    private String httpMethod;

    private String appId;

    private String nonce;

    private String sign;

    private String requestBody;

    private String responseBody;

    private Integer status;

    private String errorCode;

    private String errorMsg;

    private Long costMs;

    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    @TableLogic
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/third/entity/ThirdCallbackRecord.java`

```java
package io.github.atengk.third.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 第三方回调通知记录
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("third_callback_record")
public class ThirdCallbackRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventNo;

    private String platformCode;

    private String appId;

    private String callbackType;

    private String nonce;

    private String sign;

    private String requestBody;

    private Integer verifyStatus;

    private Integer processStatus;

    private String errorMsg;

    private Date callbackTime;

    private Date processedAt;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    @TableLogic
    private Integer deleted;
}
```

三个 Mapper 负责基础 CRUD。

文件位置：`src/main/java/io/github/atengk/third/mapper/ThirdPlatformConfigMapper.java`

```java
package io.github.atengk.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.third.entity.ThirdPlatformConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方平台配置 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ThirdPlatformConfigMapper extends BaseMapper<ThirdPlatformConfig> {
}
```

文件位置：`src/main/java/io/github/atengk/third/mapper/ThirdApiLogMapper.java`

```java
package io.github.atengk.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.third.entity.ThirdApiLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方接口调用日志 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ThirdApiLogMapper extends BaseMapper<ThirdApiLog> {
}
```

文件位置：`src/main/java/io/github/atengk/third/mapper/ThirdCallbackRecordMapper.java`

```java
package io.github.atengk.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.third.entity.ThirdCallbackRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方回调通知记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface ThirdCallbackRecordMapper extends BaseMapper<ThirdCallbackRecord> {
}
```

### 签名工具类

签名工具类负责生成 HMAC-SHA256 签名和验签。签名规则固定为：排除 `sign` 字段、排除空值、按 key 升序排序、按 `key=value&key=value` 拼接，然后使用 `appSecret` 做 HMAC-SHA256。

文件位置：`src/main/java/io/github/atengk/third/util/ThirdSignUtil.java`

```java
package io.github.atengk.third.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 第三方接口签名工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThirdSignUtil {

    private static final String SIGN_FIELD = "sign";

    private ThirdSignUtil() {
    }

    /**
     * 生成 HMAC-SHA256 签名
     *
     * @param params    待签名参数
     * @param appSecret 应用密钥
     * @return 签名
     */
    public static String hmacSha256Sign(Map<String, Object> params, String appSecret) {
        if (MapUtil.isEmpty(params)) {
            throw new IllegalArgumentException("签名参数不能为空");
        }
        if (StrUtil.isBlank(appSecret)) {
            throw new IllegalArgumentException("签名密钥不能为空");
        }

        String signText = buildSignText(params);
        HMac hMac = SecureUtil.hmacSha256(appSecret.getBytes(CharsetUtil.CHARSET_UTF_8));
        return hMac.digestHex(signText);
    }

    /**
     * 校验 HMAC-SHA256 签名
     *
     * @param params    待验签参数
     * @param appSecret 应用密钥
     * @param sign      请求签名
     * @return 是否通过
     */
    public static boolean verifyHmacSha256(Map<String, Object> params, String appSecret, String sign) {
        if (StrUtil.isBlank(sign)) {
            return false;
        }
        String localSign = hmacSha256Sign(params, appSecret);
        return StrUtil.equalsIgnoreCase(localSign, sign);
    }

    /**
     * 构建签名原始串
     *
     * @param params 参数
     * @return 签名原始串
     */
    public static String buildSignText(Map<String, Object> params) {
        TreeMap<String, Object> sortedMap = new TreeMap<>();
        params.forEach((key, value) -> {
            if (StrUtil.isNotBlank(key) && !StrUtil.equals(key, SIGN_FIELD) && Objects.nonNull(value)) {
                sortedMap.put(key, value);
            }
        });

        return sortedMap.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + normalizeValue(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * 规范化参数值
     *
     * @param value 参数值
     * @return 字符串值
     */
    private static String normalizeValue(Object value) {
        return String.valueOf(value);
    }
}
```

下面的工具类负责生成请求流水号、签名请求头和签名参数。

文件位置：`src/main/java/io/github/atengk/third/util/ThirdRequestUtil.java`

```java
package io.github.atengk.third.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.third.dto.ThirdSignHeaders;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三方请求工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class ThirdRequestUtil {

    private ThirdRequestUtil() {
    }

    /**
     * 生成请求流水号
     *
     * @param apiCode 接口编码
     * @return 请求流水号
     */
    public static String generateRequestNo(String apiCode) {
        return apiCode + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * 构建基础签名请求头
     *
     * @param appId 应用ID
     * @return 签名请求头
     */
    public static ThirdSignHeaders buildBaseHeaders(String appId) {
        return ThirdSignHeaders.builder()
                .appId(appId)
                .timestamp(String.valueOf(System.currentTimeMillis()))
                .nonce(UUID.fastUUID().toString(true))
                .signType("HMAC_SHA256")
                .build();
    }

    /**
     * 组装签名参数
     *
     * @param bizParams 业务参数
     * @param headers   签名请求头
     * @return 签名参数
     */
    public static Map<String, Object> buildSignParams(Map<String, Object> bizParams, ThirdSignHeaders headers) {
        Map<String, Object> signParams = new LinkedHashMap<>();
        if (MapUtil.isNotEmpty(bizParams)) {
            signParams.putAll(bizParams);
        }
        signParams.put("appId", headers.getAppId());
        signParams.put("timestamp", headers.getTimestamp());
        signParams.put("nonce", headers.getNonce());
        signParams.put("signType", headers.getSignType());
        return signParams;
    }
}
```

### 第三方客户端

第三方客户端使用 OpenFeign 声明 HTTP 接口。签名信息放在请求头，业务参数放在请求体。

文件位置：`src/main/java/io/github/atengk/third/dto/ThirdSignHeaders.java`

```java
package io.github.atengk.third.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 第三方接口签名请求头
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdSignHeaders {

    private String appId;

    private String timestamp;

    private String nonce;

    private String signType;

    private String sign;
}
```

文件位置：`src/main/java/io/github/atengk/third/dto/MockPlatformResponse.java`

```java
package io.github.atengk.third.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模拟第三方平台响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockPlatformResponse<T> {

    private String code;

    private String message;

    private T data;

    private String requestId;
}
```

文件位置：`src/main/java/io/github/atengk/third/feign/MockOpenPlatformClient.java`

```java
package io.github.atengk.third.feign;

import io.github.atengk.third.dto.MockPlatformResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模拟第三方开放平台客户端
 *
 * @author Ateng
 * @since 2026-05-15
 */
@FeignClient(
        name = "mockOpenPlatformClient",
        url = "${third-platform.mock.base-url:http://localhost:9090}"
)
public interface MockOpenPlatformClient {

    /**
     * 调用模拟第三方接口
     *
     * @param appId     应用ID
     * @param timestamp 时间戳
     * @param nonce     随机串
     * @param signType  签名类型
     * @param sign      签名
     * @param body      请求体
     * @return 第三方响应
     */
    @PostMapping("/open/api/invoke")
    MockPlatformResponse<Map<String, Object>> invoke(
            @RequestHeader("X-App-Id") String appId,
            @RequestHeader("X-Timestamp") String timestamp,
            @RequestHeader("X-Nonce") String nonce,
            @RequestHeader("X-Sign-Type") String signType,
            @RequestHeader("X-Sign") String sign,
            @RequestBody Map<String, Object> body
    );
}
```

### 业务服务类

业务服务类负责平台配置查询、接口调用、日志记录、限流、签名和响应解析。这里给出核心实现，能完整体现主动调用链路。

文件位置：`src/main/java/io/github/atengk/third/service/ThirdPlatformConfigService.java`

```java
package io.github.atengk.third.service;

import io.github.atengk.third.entity.ThirdPlatformConfig;

/**
 * 第三方平台配置服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ThirdPlatformConfigService {

    /**
     * 根据平台编码查询启用配置
     *
     * @param platformCode 平台编码
     * @return 平台配置
     */
    ThirdPlatformConfig getEnabledByPlatformCode(String platformCode);

    /**
     * 根据应用ID查询启用配置
     *
     * @param appId 应用ID
     * @return 平台配置
     */
    ThirdPlatformConfig getEnabledByAppId(String appId);
}
```

文件位置：`src/main/java/io/github/atengk/third/service/impl/ThirdPlatformConfigServiceImpl.java`

```java
package io.github.atengk.third.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.third.common.exception.ThirdPlatformException;
import io.github.atengk.third.entity.ThirdPlatformConfig;
import io.github.atengk.third.enums.ThirdErrorCode;
import io.github.atengk.third.mapper.ThirdPlatformConfigMapper;
import io.github.atengk.third.service.ThirdPlatformConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 第三方平台配置服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdPlatformConfigServiceImpl implements ThirdPlatformConfigService {

    private static final String CONFIG_CODE_KEY = "third:platform:config:code:%s";

    private static final String CONFIG_APP_ID_KEY = "third:platform:config:appid:%s";

    private final StringRedisTemplate stringRedisTemplate;

    private final ThirdPlatformConfigMapper thirdPlatformConfigMapper;

    /**
     * 根据平台编码查询启用配置
     *
     * @param platformCode 平台编码
     * @return 平台配置
     */
    @Override
    public ThirdPlatformConfig getEnabledByPlatformCode(String platformCode) {
        String key = StrUtil.format(CONFIG_CODE_KEY, platformCode);
        String cacheValue = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(cacheValue)) {
            return JSONUtil.toBean(cacheValue, ThirdPlatformConfig.class);
        }

        ThirdPlatformConfig config = thirdPlatformConfigMapper.selectOne(
                new LambdaQueryWrapper<ThirdPlatformConfig>()
                        .eq(ThirdPlatformConfig::getPlatformCode, platformCode)
                        .last("LIMIT 1")
        );

        checkConfig(config);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(config), Duration.ofMinutes(30));
        log.info("第三方平台配置已加载到缓存，platformCode={}", platformCode);
        return config;
    }

    /**
     * 根据应用ID查询启用配置
     *
     * @param appId 应用ID
     * @return 平台配置
     */
    @Override
    public ThirdPlatformConfig getEnabledByAppId(String appId) {
        String key = StrUtil.format(CONFIG_APP_ID_KEY, appId);
        String cacheValue = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(cacheValue)) {
            return JSONUtil.toBean(cacheValue, ThirdPlatformConfig.class);
        }

        ThirdPlatformConfig config = thirdPlatformConfigMapper.selectOne(
                new LambdaQueryWrapper<ThirdPlatformConfig>()
                        .eq(ThirdPlatformConfig::getAppId, appId)
                        .last("LIMIT 1")
        );

        checkConfig(config);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(config), Duration.ofMinutes(30));
        log.info("第三方平台配置已加载到缓存，appId={}", appId);
        return config;
    }

    /**
     * 校验平台配置
     *
     * @param config 平台配置
     */
    private void checkConfig(ThirdPlatformConfig config) {
        if (config == null) {
            throw new ThirdPlatformException(ThirdErrorCode.PLATFORM_CONFIG_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(config.getStatus())) {
            throw new ThirdPlatformException(ThirdErrorCode.PLATFORM_DISABLED);
        }
    }
}
```

下面是错误码和业务异常。

文件位置：`src/main/java/io/github/atengk/third/enums/ThirdErrorCode.java`

```java
package io.github.atengk.third.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 第三方平台错误码
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
public enum ThirdErrorCode {

    PARAM_ERROR("THIRD_400", "请求参数错误"),
    PLATFORM_CONFIG_NOT_FOUND("THIRD_404", "第三方平台配置不存在"),
    PLATFORM_DISABLED("THIRD_405", "第三方平台已禁用"),
    THIRD_EMPTY_RESPONSE("THIRD_500", "第三方平台响应为空"),
    THIRD_API_INVOKE_ERROR("THIRD_501", "第三方接口调用异常"),
    THIRD_INVALID_SIGN("THIRD_601", "第三方签名错误"),
    THIRD_LIMIT_EXCEEDED("THIRD_603", "第三方接口触发限流"),
    THIRD_SYSTEM_ERROR("THIRD_604", "第三方系统异常"),
    THIRD_UNKNOWN_ERROR("THIRD_699", "第三方未知异常");

    private final String code;

    private final String message;

    ThirdErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据第三方错误码映射系统错误码
     *
     * @param thirdCode 第三方错误码
     * @return 系统错误码
     */
    public static ThirdErrorCode fromThirdCode(String thirdCode) {
        if (StrUtil.isBlank(thirdCode)) {
            return THIRD_UNKNOWN_ERROR;
        }
        return switch (thirdCode) {
            case "INVALID_SIGN" -> THIRD_INVALID_SIGN;
            case "LIMIT_EXCEEDED" -> THIRD_LIMIT_EXCEEDED;
            case "SYSTEM_ERROR" -> THIRD_SYSTEM_ERROR;
            default -> THIRD_UNKNOWN_ERROR;
        };
    }
}
```

文件位置：`src/main/java/io/github/atengk/third/common/exception/ThirdPlatformException.java`

```java
package io.github.atengk.third.common.exception;

import io.github.atengk.third.enums.ThirdErrorCode;
import lombok.Getter;

/**
 * 第三方平台业务异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
public class ThirdPlatformException extends RuntimeException {

    private final String code;

    private final String message;

    /**
     * 构建业务异常
     *
     * @param errorCode 错误码
     */
    public ThirdPlatformException(ThirdErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 构建业务异常
     *
     * @param errorCode 错误码
     * @param message   错误消息
     */
    public ThirdPlatformException(ThirdErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }
}
```

下面是主动调用服务的请求和响应 DTO。

文件位置：`src/main/java/io/github/atengk/third/dto/ThirdInvokeRequest.java`

```java
package io.github.atengk.third.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 第三方接口调用请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdInvokeRequest {

    private String platformCode;

    private String apiCode;

    private String bizNo;

    private Map<String, Object> bizParams;
}
```

文件位置：`src/main/java/io/github/atengk/third/dto/ThirdInvokeResponse.java`

```java
package io.github.atengk.third.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 第三方接口调用响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdInvokeResponse {

    private Boolean success;

    private String requestNo;

    private String thirdCode;

    private String thirdMessage;

    private String data;
}
```

下面的远程调用组件负责接入 Resilience4j 重试和熔断。

文件位置：`src/main/java/io/github/atengk/third/service/ThirdRemoteInvoker.java`

```java
package io.github.atengk.third.service;

import io.github.atengk.third.dto.MockPlatformResponse;
import io.github.atengk.third.dto.ThirdSignHeaders;
import io.github.atengk.third.feign.MockOpenPlatformClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 第三方远程调用组件
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThirdRemoteInvoker {

    private final MockOpenPlatformClient mockOpenPlatformClient;

    /**
     * 调用模拟第三方平台
     *
     * @param requestNo 请求流水号
     * @param headers   签名请求头
     * @param body      请求体
     * @return 第三方响应
     */
    @Retry(name = "thirdApiRetry", fallbackMethod = "invokeFallback")
    @CircuitBreaker(name = "thirdApiCircuitBreaker", fallbackMethod = "invokeFallback")
    public MockPlatformResponse<Map<String, Object>> invokeMock(String requestNo,
                                                                ThirdSignHeaders headers,
                                                                Map<String, Object> body) {
        log.info("开始执行第三方远程调用，requestNo={}，appId={}", requestNo, headers.getAppId());
        return mockOpenPlatformClient.invoke(
                headers.getAppId(),
                headers.getTimestamp(),
                headers.getNonce(),
                headers.getSignType(),
                headers.getSign(),
                body
        );
    }

    /**
     * 第三方调用降级
     *
     * @param requestNo 请求流水号
     * @param headers   签名请求头
     * @param body      请求体
     * @param ex        异常
     * @return 降级响应
     */
    public MockPlatformResponse<Map<String, Object>> invokeFallback(String requestNo,
                                                                    ThirdSignHeaders headers,
                                                                    Map<String, Object> body,
                                                                    Throwable ex) {
        log.error("第三方远程调用失败，触发降级，requestNo={}，appId={}", requestNo, headers.getAppId(), ex);
        return MockPlatformResponse.<Map<String, Object>>builder()
                .code("SYSTEM_ERROR")
                .message("第三方接口暂不可用")
                .requestId(requestNo)
                .build();
    }
}
```

下面是主动调用服务实现，包含签名、限流、远程调用、响应解析和日志落库。

文件位置：`src/main/java/io/github/atengk/third/service/ThirdInvokeService.java`

```java
package io.github.atengk.third.service;

import io.github.atengk.third.dto.ThirdInvokeRequest;
import io.github.atengk.third.dto.ThirdInvokeResponse;

/**
 * 第三方接口调用服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ThirdInvokeService {

    /**
     * 调用第三方接口
     *
     * @param request 请求参数
     * @return 调用响应
     */
    ThirdInvokeResponse invoke(ThirdInvokeRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/third/service/impl/ThirdInvokeServiceImpl.java`

```java
package io.github.atengk.third.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.third.common.exception.ThirdPlatformException;
import io.github.atengk.third.dto.MockPlatformResponse;
import io.github.atengk.third.dto.ThirdInvokeRequest;
import io.github.atengk.third.dto.ThirdInvokeResponse;
import io.github.atengk.third.dto.ThirdSignHeaders;
import io.github.atengk.third.entity.ThirdApiLog;
import io.github.atengk.third.entity.ThirdPlatformConfig;
import io.github.atengk.third.enums.ThirdErrorCode;
import io.github.atengk.third.mapper.ThirdApiLogMapper;
import io.github.atengk.third.service.ThirdInvokeService;
import io.github.atengk.third.service.ThirdPlatformConfigService;
import io.github.atengk.third.service.ThirdRemoteInvoker;
import io.github.atengk.third.util.ThirdRequestUtil;
import io.github.atengk.third.util.ThirdSignUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * 第三方接口调用服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdInvokeServiceImpl implements ThirdInvokeService {

    private static final String LIMIT_KEY = "third:invoke:limit:%s:%s";

    private final StringRedisTemplate stringRedisTemplate;

    private final ThirdRemoteInvoker thirdRemoteInvoker;

    private final ThirdApiLogMapper thirdApiLogMapper;

    private final ThirdPlatformConfigService thirdPlatformConfigService;

    /**
     * 调用第三方接口
     *
     * @param request 请求参数
     * @return 调用响应
     */
    @Override
    public ThirdInvokeResponse invoke(ThirdInvokeRequest request) {
        validateRequest(request);
        checkRateLimit(request.getPlatformCode(), request.getApiCode());

        long startTime = System.currentTimeMillis();
        String requestNo = ThirdRequestUtil.generateRequestNo(request.getApiCode());
        ThirdPlatformConfig config = thirdPlatformConfigService.getEnabledByPlatformCode(request.getPlatformCode());

        ThirdSignHeaders headers = ThirdRequestUtil.buildBaseHeaders(config.getAppId());
        Map<String, Object> signParams = ThirdRequestUtil.buildSignParams(request.getBizParams(), headers);
        headers.setSign(ThirdSignUtil.hmacSha256Sign(signParams, config.getAppSecret()));

        ThirdApiLog apiLog = saveProcessingLog(requestNo, request, config, headers);

        try {
            MockPlatformResponse<Map<String, Object>> platformResponse =
                    thirdRemoteInvoker.invokeMock(requestNo, headers, request.getBizParams());

            ThirdInvokeResponse response = parseResponse(requestNo, platformResponse);
            updateSuccessLog(apiLog.getId(), platformResponse, System.currentTimeMillis() - startTime);

            log.info("第三方接口调用成功，requestNo={}，platformCode={}，apiCode={}",
                    requestNo, request.getPlatformCode(), request.getApiCode());
            return response;
        } catch (ThirdPlatformException ex) {
            updateFailLog(apiLog.getId(), ex.getCode(), ex.getMessage(), System.currentTimeMillis() - startTime);
            throw ex;
        } catch (Exception ex) {
            updateFailLog(apiLog.getId(), ThirdErrorCode.THIRD_API_INVOKE_ERROR.getCode(), ex.getMessage(), System.currentTimeMillis() - startTime);
            log.error("第三方接口调用异常，requestNo={}", requestNo, ex);
            throw new ThirdPlatformException(ThirdErrorCode.THIRD_API_INVOKE_ERROR, "第三方接口调用异常");
        }
    }

    /**
     * 校验请求参数
     *
     * @param request 请求参数
     */
    private void validateRequest(ThirdInvokeRequest request) {
        if (request == null) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        if (StrUtil.hasBlank(request.getPlatformCode(), request.getApiCode())) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "平台编码或接口编码不能为空");
        }
        if (MapUtil.isEmpty(request.getBizParams())) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "业务参数不能为空");
        }
    }

    /**
     * 检查本地限流
     *
     * @param platformCode 平台编码
     * @param apiCode      接口编码
     */
    private void checkRateLimit(String platformCode, String apiCode) {
        String key = StrUtil.format(LIMIT_KEY, platformCode, apiCode);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(1));
        }
        if (count != null && count > 20) {
            log.warn("第三方接口触发本地限流，platformCode={}，apiCode={}，count={}", platformCode, apiCode, count);
            throw new ThirdPlatformException(ThirdErrorCode.THIRD_LIMIT_EXCEEDED);
        }
    }

    /**
     * 保存处理中日志
     *
     * @param requestNo 请求流水号
     * @param request   请求参数
     * @param config    平台配置
     * @param headers   请求头
     * @return 日志实体
     */
    private ThirdApiLog saveProcessingLog(String requestNo, ThirdInvokeRequest request, ThirdPlatformConfig config, ThirdSignHeaders headers) {
        ThirdApiLog logEntity = new ThirdApiLog();
        logEntity.setRequestNo(requestNo);
        logEntity.setPlatformCode(request.getPlatformCode());
        logEntity.setApiCode(request.getApiCode());
        logEntity.setApiUrl(config.getBaseUrl() + "/open/api/invoke");
        logEntity.setHttpMethod("POST");
        logEntity.setAppId(config.getAppId());
        logEntity.setNonce(headers.getNonce());
        logEntity.setSign(headers.getSign());
        logEntity.setRequestBody(JSONUtil.toJsonStr(request.getBizParams()));
        logEntity.setStatus(0);
        logEntity.setRetryCount(0);
        thirdApiLogMapper.insert(logEntity);
        return logEntity;
    }

    /**
     * 解析第三方响应
     *
     * @param requestNo        请求流水号
     * @param platformResponse 第三方响应
     * @return 系统响应
     */
    private ThirdInvokeResponse parseResponse(String requestNo, MockPlatformResponse<Map<String, Object>> platformResponse) {
        if (platformResponse == null) {
            throw new ThirdPlatformException(ThirdErrorCode.THIRD_EMPTY_RESPONSE);
        }

        if (!StrUtil.equals(platformResponse.getCode(), "SUCCESS")) {
            ThirdErrorCode errorCode = ThirdErrorCode.fromThirdCode(platformResponse.getCode());
            throw new ThirdPlatformException(errorCode, platformResponse.getMessage());
        }

        return ThirdInvokeResponse.builder()
                .success(true)
                .requestNo(requestNo)
                .thirdCode(platformResponse.getCode())
                .thirdMessage(platformResponse.getMessage())
                .data(JSONUtil.toJsonStr(platformResponse.getData()))
                .build();
    }

    /**
     * 更新成功日志
     *
     * @param logId            日志ID
     * @param platformResponse 第三方响应
     * @param costMs           耗时
     */
    private void updateSuccessLog(Long logId, MockPlatformResponse<Map<String, Object>> platformResponse, long costMs) {
        ThirdApiLog updateLog = new ThirdApiLog();
        updateLog.setId(logId);
        updateLog.setStatus(1);
        updateLog.setCostMs(costMs);
        updateLog.setResponseBody(JSONUtil.toJsonStr(platformResponse));
        thirdApiLogMapper.updateById(updateLog);
    }

    /**
     * 更新失败日志
     *
     * @param logId     日志ID
     * @param errorCode 错误码
     * @param errorMsg  错误消息
     * @param costMs    耗时
     */
    private void updateFailLog(Long logId, String errorCode, String errorMsg, long costMs) {
        ThirdApiLog updateLog = new ThirdApiLog();
        updateLog.setId(logId);
        updateLog.setStatus(2);
        updateLog.setErrorCode(errorCode);
        updateLog.setErrorMsg(StrUtil.maxLength(errorMsg, 1000));
        updateLog.setCostMs(costMs);
        thirdApiLogMapper.updateById(updateLog);
    }
}
```

### 回调 Controller

回调 Controller 接收第三方平台通知，核心点是从 Header 提取签名参数，从 Body 提取业务回调参数，然后交给服务层验签、幂等和落库。

文件位置：`src/main/java/io/github/atengk/third/dto/ThirdCallbackRequest.java`

```java
package io.github.atengk.third.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 第三方回调请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdCallbackRequest {

    private String eventNo;

    private String callbackType;

    private Long callbackTime;

    private Map<String, Object> data;
}
```

文件位置：`src/main/java/io/github/atengk/third/service/ThirdCallbackService.java`

```java
package io.github.atengk.third.service;

import io.github.atengk.third.dto.ThirdCallbackRequest;
import io.github.atengk.third.dto.ThirdSignHeaders;

/**
 * 第三方回调服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface ThirdCallbackService {

    /**
     * 处理第三方回调
     *
     * @param platformCode 平台编码
     * @param headers      签名请求头
     * @param request      回调请求
     */
    void handleCallback(String platformCode, ThirdSignHeaders headers, ThirdCallbackRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/third/service/impl/ThirdCallbackServiceImpl.java`

```java
package io.github.atengk.third.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.third.common.exception.ThirdPlatformException;
import io.github.atengk.third.dto.ThirdCallbackRequest;
import io.github.atengk.third.dto.ThirdSignHeaders;
import io.github.atengk.third.entity.ThirdCallbackRecord;
import io.github.atengk.third.entity.ThirdPlatformConfig;
import io.github.atengk.third.enums.ThirdErrorCode;
import io.github.atengk.third.mapper.ThirdCallbackRecordMapper;
import io.github.atengk.third.service.ThirdCallbackService;
import io.github.atengk.third.service.ThirdPlatformConfigService;
import io.github.atengk.third.util.ThirdRequestUtil;
import io.github.atengk.third.util.ThirdSignUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * 第三方回调服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdCallbackServiceImpl implements ThirdCallbackService {

    private static final String CALLBACK_IDEMPOTENT_KEY = "third:callback:idempotent:%s";

    private static final String CALLBACK_NONCE_KEY = "third:callback:nonce:%s:%s";

    private static final long TIMESTAMP_OFFSET_MILLIS = 5 * 60 * 1000L;

    private final StringRedisTemplate stringRedisTemplate;

    private final ThirdCallbackRecordMapper thirdCallbackRecordMapper;

    private final ThirdPlatformConfigService thirdPlatformConfigService;

    /**
     * 处理第三方回调
     *
     * @param platformCode 平台编码
     * @param headers      签名请求头
     * @param request      回调请求
     */
    @Override
    public void handleCallback(String platformCode, ThirdSignHeaders headers, ThirdCallbackRequest request) {
        validateCallback(platformCode, headers, request);
        checkTimestamp(headers.getTimestamp());
        checkNonce(headers);

        ThirdPlatformConfig config = thirdPlatformConfigService.getEnabledByAppId(headers.getAppId());
        Map<String, Object> bodyMap = BeanUtil.beanToMap(request, false, true);
        Map<String, Object> signParams = ThirdRequestUtil.buildSignParams(bodyMap, headers);
        boolean verified = ThirdSignUtil.verifyHmacSha256(signParams, config.getAppSecret(), headers.getSign());

        if (!verified) {
            saveRecord(platformCode, headers, request, 2, 2, "回调验签失败");
            log.warn("第三方回调验签失败，platformCode={}，appId={}，eventNo={}",
                    platformCode, headers.getAppId(), request.getEventNo());
            throw new ThirdPlatformException(ThirdErrorCode.THIRD_INVALID_SIGN, "回调验签失败");
        }

        if (!markIdempotent(request.getEventNo())) {
            log.info("第三方回调重复通知，eventNo={}", request.getEventNo());
            return;
        }

        saveRecord(platformCode, headers, request, 1, 1, null);
        log.info("第三方回调处理成功，platformCode={}，eventNo={}，callbackType={}",
                platformCode, request.getEventNo(), request.getCallbackType());
    }

    /**
     * 校验回调基础参数
     *
     * @param platformCode 平台编码
     * @param headers      签名请求头
     * @param request      回调请求
     */
    private void validateCallback(String platformCode, ThirdSignHeaders headers, ThirdCallbackRequest request) {
        if (StrUtil.isBlank(platformCode)) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "平台编码不能为空");
        }
        if (headers == null || StrUtil.hasBlank(headers.getAppId(), headers.getTimestamp(), headers.getNonce(), headers.getSign())) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "签名请求头不完整");
        }
        if (request == null || StrUtil.hasBlank(request.getEventNo(), request.getCallbackType())) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "回调参数不完整");
        }
    }

    /**
     * 校验时间戳
     *
     * @param timestamp 时间戳
     */
    private void checkTimestamp(String timestamp) {
        long requestTime = Long.parseLong(timestamp);
        if (Math.abs(System.currentTimeMillis() - requestTime) > TIMESTAMP_OFFSET_MILLIS) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "回调时间戳已过期");
        }
    }

    /**
     * 校验 nonce 防重放
     *
     * @param headers 签名请求头
     */
    private void checkNonce(ThirdSignHeaders headers) {
        String key = StrUtil.format(CALLBACK_NONCE_KEY, headers.getAppId(), headers.getNonce());
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(5));
        if (!Boolean.TRUE.equals(success)) {
            throw new ThirdPlatformException(ThirdErrorCode.PARAM_ERROR, "重复回调请求");
        }
    }

    /**
     * 标记回调幂等
     *
     * @param eventNo 事件号
     * @return 是否首次处理
     */
    private boolean markIdempotent(String eventNo) {
        String key = StrUtil.format(CALLBACK_IDEMPOTENT_KEY, eventNo);
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofDays(1));
        return Boolean.TRUE.equals(success);
    }

    /**
     * 保存回调记录
     *
     * @param platformCode  平台编码
     * @param headers       签名请求头
     * @param request       回调请求
     * @param verifyStatus  验签状态
     * @param processStatus 处理状态
     * @param errorMsg      错误信息
     */
    private void saveRecord(String platformCode,
                            ThirdSignHeaders headers,
                            ThirdCallbackRequest request,
                            Integer verifyStatus,
                            Integer processStatus,
                            String errorMsg) {
        try {
            ThirdCallbackRecord record = new ThirdCallbackRecord();
            record.setEventNo(request.getEventNo());
            record.setPlatformCode(platformCode);
            record.setAppId(headers.getAppId());
            record.setCallbackType(request.getCallbackType());
            record.setNonce(headers.getNonce());
            record.setSign(headers.getSign());
            record.setRequestBody(JSONUtil.toJsonStr(request));
            record.setVerifyStatus(verifyStatus);
            record.setProcessStatus(processStatus);
            record.setErrorMsg(errorMsg);
            record.setCallbackTime(request.getCallbackTime() == null ? null : DateUtil.date(request.getCallbackTime()).toJdkDate());
            record.setProcessedAt(DateUtil.date().toJdkDate());
            thirdCallbackRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            log.info("回调记录已存在，eventNo={}", request.getEventNo());
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/third/controller/ThirdCallbackController.java`

```java
package io.github.atengk.third.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.third.dto.ThirdCallbackRequest;
import io.github.atengk.third.dto.ThirdSignHeaders;
import io.github.atengk.third.service.ThirdCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 第三方回调接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/third/callback")
public class ThirdCallbackController {

    private final ThirdCallbackService thirdCallbackService;

    /**
     * 接收第三方回调
     *
     * @param platformCode 平台编码
     * @param appId        应用ID
     * @param timestamp    时间戳
     * @param nonce        随机串
     * @param signType     签名类型
     * @param sign         签名
     * @param request      回调请求
     * @return 响应结果
     */
    @PostMapping("/{platformCode}")
    public Map<String, Object> receiveCallback(@PathVariable String platformCode,
                                               @RequestHeader("X-App-Id") String appId,
                                               @RequestHeader("X-Timestamp") String timestamp,
                                               @RequestHeader("X-Nonce") String nonce,
                                               @RequestHeader("X-Sign-Type") String signType,
                                               @RequestHeader("X-Sign") String sign,
                                               @RequestBody ThirdCallbackRequest request) {
        ThirdSignHeaders headers = ThirdSignHeaders.builder()
                .appId(appId)
                .timestamp(timestamp)
                .nonce(nonce)
                .signType(signType)
                .sign(sign)
                .build();

        thirdCallbackService.handleCallback(platformCode, headers, request);

        return MapUtil.builder("code", "SUCCESS")
                .put("message", "success")
                .build();
    }
}
```

主动调用 Controller 如下。

文件位置：`src/main/java/io/github/atengk/third/controller/ThirdApiController.java`

```java
package io.github.atengk.third.controller;

import io.github.atengk.third.dto.ThirdInvokeRequest;
import io.github.atengk.third.dto.ThirdInvokeResponse;
import io.github.atengk.third.entity.ThirdApiLog;
import io.github.atengk.third.mapper.ThirdApiLogMapper;
import io.github.atengk.third.service.ThirdInvokeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 第三方接口调用入口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/third")
public class ThirdApiController {

    private final ThirdInvokeService thirdInvokeService;

    private final ThirdApiLogMapper thirdApiLogMapper;

    /**
     * 发起第三方调用
     *
     * @param request 请求参数
     * @return 调用响应
     */
    @PostMapping("/invoke")
    public ThirdInvokeResponse invoke(@RequestBody ThirdInvokeRequest request) {
        return thirdInvokeService.invoke(request);
    }

    /**
     * 查询调用日志
     *
     * @param id 日志ID
     * @return 调用日志
     */
    @GetMapping("/logs/{id}")
    public ThirdApiLog getLog(@PathVariable Long id) {
        return thirdApiLogMapper.selectById(id);
    }
}
```

## 功能验证

本节使用 `curl` 验证主动调用、签名错误、重复回调和失败重试。验证前需要先启动 MySQL、Redis、RabbitMQ，并插入一条第三方平台配置。

初始化平台配置：

```sql
INSERT INTO third_platform_config (
    platform_code,
    platform_name,
    app_id,
    app_secret,
    base_url,
    callback_url,
    sign_type,
    timeout_ms,
    status,
    remark
) VALUES (
    'MOCK_OPEN',
    '模拟开放平台',
    'mock-app-001',
    'mock-secret-123456',
    'http://localhost:9090',
    'http://localhost:8080/api/third/callback/MOCK_OPEN',
    'HMAC_SHA256',
    5000,
    1,
    '第三方接口对接案例'
);
```

### 正常调用验证

正常调用验证的是“业务系统发起请求 -> 系统生成签名 -> OpenFeign 调用第三方 -> 解析响应 -> 写入日志”这条链路。

请求命令：

```bash
curl -X POST 'http://localhost:8080/api/third/invoke' \
  -H 'Content-Type: application/json' \
  -d '{
    "platformCode": "MOCK_OPEN",
    "apiCode": "SEND_SMS",
    "bizNo": "SMS202605150001",
    "bizParams": {
      "bizNo": "SMS202605150001",
      "phone": "13800138000",
      "content": "您的验证码是 123456"
    }
  }'
```

期望响应：

```json
{
  "success": true,
  "requestNo": "SEND_SMS202605151230001234567890",
  "thirdCode": "SUCCESS",
  "thirdMessage": "处理成功",
  "data": "{\"thirdBizNo\":\"TP202605150001\"}"
}
```

数据库验证：

```sql
SELECT request_no, platform_code, api_code, status, cost_ms, error_code, error_msg
FROM third_api_log
ORDER BY id DESC
LIMIT 1;
```

期望结果：

```text
status = 1
error_code = NULL
error_msg = NULL
cost_ms 有值
```

### 签名错误验证

签名错误主要验证回调验签能力。把 `X-Sign` 改成错误值即可。

请求命令：

```bash
curl -X POST 'http://localhost:8080/api/third/callback/MOCK_OPEN' \
  -H 'Content-Type: application/json' \
  -H 'X-App-Id: mock-app-001' \
  -H 'X-Timestamp: 1778817600000' \
  -H 'X-Nonce: nonce-sign-error-001' \
  -H 'X-Sign-Type: HMAC_SHA256' \
  -H 'X-Sign: wrong-sign' \
  -d '{
    "eventNo": "EVT_SIGN_ERROR_001",
    "callbackType": "SMS_SEND_RESULT",
    "callbackTime": 1778817600000,
    "data": {
      "bizNo": "SMS202605150001",
      "sendStatus": "SUCCESS",
      "phone": "13800138000"
    }
  }'
```

期望响应：

```json
{
  "success": false,
  "code": "THIRD_601",
  "message": "回调验签失败"
}
```

数据库验证：

```sql
SELECT event_no, verify_status, process_status, error_msg
FROM third_callback_record
WHERE event_no = 'EVT_SIGN_ERROR_001';
```

期望结果：

```text
verify_status = 2
process_status = 2
error_msg = 回调验签失败
```

### 重复回调验证

重复回调验证的是 `eventNo` 幂等。第三方平台可能因为网络超时重复推送同一个事件，本系统应保证只处理一次。

第一次请求需要携带正确签名。为了方便测试，可以先在本地写一个签名生成单元测试，生成 `X-Sign` 后替换到 curl 中。

签名参数示例：

```json
{
  "appId": "mock-app-001",
  "timestamp": "1778817600000",
  "nonce": "nonce-repeat-001",
  "signType": "HMAC_SHA256",
  "eventNo": "EVT_REPEAT_001",
  "callbackType": "SMS_SEND_RESULT",
  "callbackTime": 1778817600000,
  "data": {
    "bizNo": "SMS202605150001",
    "sendStatus": "SUCCESS",
    "phone": "13800138000"
  }
}
```

第一次请求成功后，再使用同一个 `eventNo` 重新请求。

```bash
curl -X POST 'http://localhost:8080/api/third/callback/MOCK_OPEN' \
  -H 'Content-Type: application/json' \
  -H 'X-App-Id: mock-app-001' \
  -H 'X-Timestamp: 1778817600000' \
  -H 'X-Nonce: nonce-repeat-002' \
  -H 'X-Sign-Type: HMAC_SHA256' \
  -H 'X-Sign: 替换为正确签名' \
  -d '{
    "eventNo": "EVT_REPEAT_001",
    "callbackType": "SMS_SEND_RESULT",
    "callbackTime": 1778817600000,
    "data": {
      "bizNo": "SMS202605150001",
      "sendStatus": "SUCCESS",
      "phone": "13800138000"
    }
  }'
```

期望响应仍然是成功：

```json
{
  "code": "SUCCESS",
  "message": "success"
}
```

数据库验证：

```sql
SELECT COUNT(*)
FROM third_callback_record
WHERE event_no = 'EVT_REPEAT_001';
```

期望结果：

```text
COUNT(*) = 1
```

重复回调不应该重复落库，也不应该重复推动业务状态变更。

### 接口失败重试验证

失败重试验证的是 Resilience4j。可以把模拟第三方平台地址改成一个不可访问地址，然后调用 `/api/third/invoke`。

修改配置：

```yaml
third-platform:
  mock:
    # 故意配置成不可访问地址，用于验证重试和降级
    base-url: http://localhost:9999
```

重新启动服务后执行调用：

```bash
curl -X POST 'http://localhost:8080/api/third/invoke' \
  -H 'Content-Type: application/json' \
  -d '{
    "platformCode": "MOCK_OPEN",
    "apiCode": "SEND_SMS",
    "bizNo": "SMS202605150002",
    "bizParams": {
      "bizNo": "SMS202605150002",
      "phone": "13800138000",
      "content": "您的验证码是 654321"
    }
  }'
```

期望日志中出现类似内容：

```text
开始执行第三方远程调用
第三方远程调用失败，触发降级
第三方接口调用异常
```

数据库验证：

```sql
SELECT request_no, status, error_code, error_msg, cost_ms
FROM third_api_log
ORDER BY id DESC
LIMIT 1;
```

期望结果：

```text
status = 2
error_code = THIRD_604 或 THIRD_501
error_msg 有失败原因
cost_ms 有值
```

如果需要验证 Redis 限流，可以快速循环请求：

```bash
for i in $(seq 1 30); do
  curl -s -X POST 'http://localhost:8080/api/third/invoke' \
    -H 'Content-Type: application/json' \
    -d '{
      "platformCode": "MOCK_OPEN",
      "apiCode": "SEND_SMS",
      "bizNo": "SMS_LIMIT_TEST",
      "bizParams": {
        "bizNo": "SMS_LIMIT_TEST",
        "phone": "13800138000",
        "content": "限流测试"
      }
    }'
  echo
done
```

期望部分请求返回：

```json
{
  "success": false,
  "code": "THIRD_603",
  "message": "第三方接口触发限流"
}
```

至此，核心功能完成：

```text
平台配置读取
-> 请求参数签名
-> OpenFeign 调用第三方
-> Resilience4j 重试熔断
-> Redis 限流
-> 接口日志落库
-> 第三方回调验签
-> nonce 防重放
-> eventNo 幂等
-> 回调记录落库
```