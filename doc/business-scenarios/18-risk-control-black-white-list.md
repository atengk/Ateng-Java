# 风控规则与黑白名单

本案例实现一个轻量级风控模块，用于在登录、下单、领取优惠券、接口调用等高频业务入口中，根据黑名单、白名单和动态规则判断当前请求是否允许继续执行。实现重点放在可落地的后端代码结构、数据库设计、Redis 缓存和业务接入方式上，不做复杂的规则引擎平台化建设。

## 功能目标

本模块的目标是提供一套可以直接集成到 Java 后端项目中的风控基础能力。它不追求一次性覆盖所有复杂风控场景，而是优先实现业务项目中最常见、最容易复用的核心功能。

最终实现的能力包括：

| 功能           | 说明                                                   |
| -------------- | ------------------------------------------------------ |
| 黑名单拦截     | 命中用户、IP、手机号、设备号等黑名单后直接拒绝请求     |
| 白名单放行     | 命中白名单后跳过后续风控规则，直接放行                 |
| 动态规则判断   | 支持基于用户、IP、设备、业务类型的简单规则匹配         |
| Redis 缓存加速 | 黑白名单和启用状态规则缓存到 Redis，减少数据库查询     |
| 命中记录落库   | 风控命中后记录命中类型、命中值、业务场景和处理结果     |
| 业务快速接入   | 在登录、下单、领券等接口中通过统一服务调用完成风控校验 |

核心目标不是“做一个完整风控中台”，而是实现一个业务系统可直接使用的风控组件：

```text
业务请求
  ↓
构建风控上下文
  ↓
检查白名单
  ↓
检查黑名单
  ↓
执行启用状态的风控规则
  ↓
返回放行或拒绝结果
  ↓
记录命中日志
```

这个流程后续会落到 Spring Boot 代码中，形成统一入口，例如：

```java
riskControlService.check(context);
```

业务接口只需要构建当前请求的风控上下文，不需要关心内部规则如何匹配。

### 业务场景说明

风控规则与黑白名单常见于交易、电商、支付、营销、账号体系等业务场景。它通常不是主业务流程，但会直接影响系统安全性、运营效率和异常请求拦截能力。

本案例主要选取以下几个典型场景：

| 场景     | 风控目标                         | 示例                   |
| -------- | -------------------------------- | ---------------------- |
| 登录风控 | 拦截异常账号、异常 IP、异常设备  | 黑名单 IP 禁止登录     |
| 下单风控 | 拦截恶意用户、刷单设备、异常来源 | 黑名单用户禁止下单     |
| 营销风控 | 拦截薅羊毛用户、批量注册设备     | 黑名单手机号禁止领券   |
| 接口风控 | 限制异常来源调用核心接口         | 指定 IP 或设备禁止访问 |

本案例会围绕“登录”和“下单”两个接口进行接入示例。

登录接口接入后，可以实现：

```text
用户发起登录
  ↓
检查 IP 是否在白名单
  ↓
检查用户 ID、手机号、IP、设备号是否在黑名单
  ↓
判断当前登录场景是否命中启用规则
  ↓
放行或拒绝登录
```

下单接口接入后，可以实现：

```text
用户提交订单
  ↓
检查用户是否在白名单
  ↓
检查用户 ID、手机号、设备号是否在黑名单
  ↓
检查当前业务场景 ORDER_CREATE 是否存在拒绝规则
  ↓
放行或拒绝下单
```

为了保持实现清晰，本案例不会引入复杂的机器学习模型、实时行为分析、画像系统或分布式规则编排。核心重点是把常见的黑白名单和规则匹配能力做成一个可复用、可扩展的后端模块。

### 核心能力范围

本案例实现的风控能力边界如下：

| 能力           | 是否实现 | 说明                                   |
| -------------- | -------- | -------------------------------------- |
| 用户黑名单     | 是       | 根据 userId 拦截                       |
| IP 黑名单      | 是       | 根据 requestIp 拦截                    |
| 手机号黑名单   | 是       | 根据 mobile 拦截                       |
| 设备黑名单     | 是       | 根据 deviceId 拦截                     |
| 用户白名单     | 是       | 命中后优先放行                         |
| IP 白名单      | 是       | 命中后优先放行                         |
| 业务场景规则   | 是       | 根据 sceneCode 匹配登录、下单等场景    |
| 规则启停       | 是       | 支持启用、停用规则                     |
| Redis 缓存     | 是       | 缓存黑白名单和规则数据                 |
| 命中记录       | 是       | 记录命中日志，便于排查                 |
| 后台管理接口   | 是       | 提供规则和名单的增删改查核心接口       |
| 复杂表达式规则 | 否       | 不实现 Aviator、QLExpress 等表达式引擎 |
| 分布式限流     | 否       | 可后续接入 Redis + Lua 或 Sentinel     |
| 用户行为评分   | 否       | 可后续接入画像、标签、模型分系统       |

本案例的核心判断顺序固定为：

```text
白名单 > 黑名单 > 风控规则
```

也就是说：

1. 命中白名单：直接放行。
2. 未命中白名单，但命中黑名单：直接拒绝。
3. 未命中黑白名单：继续执行启用状态的风控规则。
4. 规则未命中：默认放行。
5. 规则命中拒绝策略：返回拒绝结果并记录日志。

这种顺序适合大多数业务项目，因为白名单通常用于内部测试用户、运营豁免用户、可信 IP 等场景，应具备最高优先级。

### 技术栈选型

本案例基于 Spring Boot 3 实现，整体技术栈偏实用，适合直接集成到常规 Java 后端项目中。

| 技术               | 用途         | 说明                                   |
| ------------------ | ------------ | -------------------------------------- |
| Spring Boot 3.x    | 项目基础框架 | 提供 Web、配置、依赖管理能力           |
| MyBatis-Plus       | 数据库 CRUD  | 简化规则表、名单表、命中记录表操作     |
| MySQL 8.x          | 数据存储     | 保存风控规则、黑白名单、命中记录       |
| Redis              | 缓存加速     | 缓存启用规则、黑名单、白名单           |
| Redisson           | Redis 客户端 | 可用于后续扩展分布式锁、限流           |
| Hutool             | 工具类       | 用于字符串、集合、JSON、日期等常用处理 |
| Lombok             | 简化实体代码 | 减少 Getter、Setter、构造方法样板代码  |
| Knife4j / Swagger  | 接口文档     | 方便测试管理接口和业务接口             |
| Jakarta Validation | 参数校验     | 校验新增规则、名单参数合法性           |

推荐使用以下核心依赖组合：

```text
Spring Boot 3.x
MyBatis-Plus
MySQL Driver
Redis
Redisson
Hutool
Lombok
Knife4j
Validation
```

本案例不会使用复杂规则引擎，原因是当前目标是实现“核心功能可落地”，而不是构建高度动态化的规则平台。对于大多数中小型业务系统，数据库规则配置 + Java 策略类 + Redis 缓存已经足够支撑第一版风控能力。

后续如果业务规则变复杂，可以在当前结构上继续扩展：

| 扩展方向       | 可选技术                         |
| -------------- | -------------------------------- |
| 表达式规则     | Aviator、QLExpress               |
| 复杂规则编排   | LiteFlow                         |
| 实时风控流处理 | Kafka + Flink                    |
| 分布式限流     | Redis Lua、Sentinel              |
| 用户风险评分   | 标签系统、画像系统、规则评分模型 |

## 数据库表设计

本案例使用 MySQL 保存风控规则、黑白名单和命中记录。Redis 只作为缓存层，数据库仍然作为最终数据源，方便后台管理、问题排查和数据追溯。

### 风控规则表

风控规则表用于保存可启停的规则配置，例如禁止某个业务场景下指定 IP、用户、手机号或设备号继续操作。

```sql
-- 风控规则表：保存可配置、可启停的风控规则
CREATE TABLE risk_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    scene_code VARCHAR(50) NOT NULL COMMENT '业务场景编码，例如 LOGIN、ORDER_CREATE、COUPON_RECEIVE',
    rule_type VARCHAR(30) NOT NULL COMMENT '规则类型：USER、IP、MOBILE、DEVICE',
    match_value VARCHAR(200) NOT NULL COMMENT '匹配值，例如用户ID、IP、手机号、设备号',
    action VARCHAR(30) NOT NULL DEFAULT 'REJECT' COMMENT '处理动作：PASS、REJECT',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-停用，1-启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',

    INDEX idx_scene_type_enabled (scene_code, rule_type, enabled),
    INDEX idx_match_value (match_value),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控规则表';
```

字段说明：

| 字段        | 说明                                     |
| ----------- | ---------------------------------------- |
| scene_code  | 业务场景，例如登录、下单、领券           |
| rule_type   | 规则匹配维度，例如用户、IP、手机号、设备 |
| match_value | 实际匹配值                               |
| action      | 命中后的处理动作，本案例主要使用 REJECT  |
| enabled     | 是否启用，停用后不会参与匹配             |
| deleted     | MyBatis-Plus 逻辑删除字段                |

示例数据：

```sql
-- 登录场景下禁止指定 IP 登录
INSERT INTO risk_rule
(rule_name, scene_code, rule_type, match_value, action, enabled, remark)
VALUES
('禁止异常IP登录', 'LOGIN', 'IP', '192.168.10.20', 'REJECT', 1, '异常登录来源');

-- 下单场景下禁止指定用户下单
INSERT INTO risk_rule
(rule_name, scene_code, rule_type, match_value, action, enabled, remark)
VALUES
('禁止高风险用户下单', 'ORDER_CREATE', 'USER', '10001', 'REJECT', 1, '疑似刷单用户');
```

### 黑白名单表

黑白名单表用于保存白名单和黑名单数据。白名单优先级最高，命中后直接放行；黑名单命中后直接拒绝。

```sql
-- 黑白名单表：统一保存黑名单和白名单
CREATE TABLE risk_list (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    list_type VARCHAR(20) NOT NULL COMMENT '名单类型：BLACK、WHITE',
    target_type VARCHAR(30) NOT NULL COMMENT '目标类型：USER、IP、MOBILE、DEVICE',
    target_value VARCHAR(200) NOT NULL COMMENT '目标值，例如用户ID、IP、手机号、设备号',
    scene_code VARCHAR(50) DEFAULT NULL COMMENT '业务场景编码，为空表示全局生效',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-停用，1-启用',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间，为空表示永久有效',
    reason VARCHAR(500) DEFAULT NULL COMMENT '加入名单原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',

    UNIQUE KEY uk_list_target_scene (list_type, target_type, target_value, scene_code, deleted),
    INDEX idx_target (target_type, target_value),
    INDEX idx_scene_enabled (scene_code, enabled),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控黑白名单表';
```

字段说明：

| 字段         | 说明                                             |
| ------------ | ------------------------------------------------ |
| list_type    | BLACK 表示黑名单，WHITE 表示白名单               |
| target_type  | USER、IP、MOBILE、DEVICE                         |
| target_value | 实际名单值                                       |
| scene_code   | 为空表示所有场景生效，不为空表示只对指定场景生效 |
| expire_time  | 支持临时名单，例如临时封禁 24 小时               |

示例数据：

```sql
-- 全局 IP 白名单
INSERT INTO risk_list
(list_type, target_type, target_value, scene_code, enabled, reason)
VALUES
('WHITE', 'IP', '127.0.0.1', NULL, 1, '本地测试环境');

-- 登录场景用户黑名单
INSERT INTO risk_list
(list_type, target_type, target_value, scene_code, enabled, reason)
VALUES
('BLACK', 'USER', '10001', 'LOGIN', 1, '账号存在异常登录行为');

-- 下单场景设备黑名单
INSERT INTO risk_list
(list_type, target_type, target_value, scene_code, enabled, reason)
VALUES
('BLACK', 'DEVICE', 'device_abc_001', 'ORDER_CREATE', 1, '设备疑似批量刷单');
```

### 风控命中记录表

命中记录表用于保存风控拦截或放行的关键过程，方便后续排查为什么某个请求被拒绝。

```sql
-- 风控命中记录表：记录每一次风控命中的结果
CREATE TABLE risk_hit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    request_id VARCHAR(64) NOT NULL COMMENT '请求ID，用于链路追踪',
    scene_code VARCHAR(50) NOT NULL COMMENT '业务场景编码',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    mobile VARCHAR(30) DEFAULT NULL COMMENT '手机号',
    request_ip VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
    device_id VARCHAR(100) DEFAULT NULL COMMENT '设备ID',
    hit_type VARCHAR(30) NOT NULL COMMENT '命中类型：WHITE_LIST、BLACK_LIST、RULE',
    hit_value VARCHAR(200) DEFAULT NULL COMMENT '命中的值',
    hit_source_id BIGINT DEFAULT NULL COMMENT '命中的规则ID或名单ID',
    result VARCHAR(30) NOT NULL COMMENT '处理结果：PASS、REJECT',
    reason VARCHAR(500) DEFAULT NULL COMMENT '命中原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_request_id (request_id),
    INDEX idx_scene_user (scene_code, user_id),
    INDEX idx_ip (request_ip),
    INDEX idx_device_id (device_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控命中记录表';
```

示例数据：

```sql
-- 示例：用户登录时命中黑名单
INSERT INTO risk_hit_record
(request_id, scene_code, user_id, mobile, request_ip, device_id, hit_type, hit_value, hit_source_id, result, reason)
VALUES
('req_20260515100000001', 'LOGIN', 10001, '13800000000', '192.168.10.20', 'device_abc_001',
 'BLACK_LIST', '10001', 1, 'REJECT', '用户命中登录黑名单');
```

## 项目结构设计

本模块采用常规 Spring Boot 分层结构，重点是把风控上下文、名单判断、规则判断和命中记录保存拆开，避免业务接口中堆积大量判断逻辑。

### 包结构规划

推荐包路径如下：

```text
src/main/java/io/github/atengk/risk
├── RiskApplication.java
├── common
│   ├── constant
│   │   └── RiskCacheConstant.java
│   └── result
│       └── RiskCheckResult.java
├── controller
│   ├── RiskListController.java
│   └── RiskRuleController.java
├── dto
│   ├── RiskCheckContext.java
│   ├── RiskListCreateDTO.java
│   └── RiskRuleCreateDTO.java
├── entity
│   ├── RiskHitRecord.java
│   ├── RiskList.java
│   └── RiskRule.java
├── enums
│   ├── RiskActionEnum.java
│   ├── RiskHitTypeEnum.java
│   ├── RiskListTypeEnum.java
│   └── RiskTargetTypeEnum.java
├── mapper
│   ├── RiskHitRecordMapper.java
│   ├── RiskListMapper.java
│   └── RiskRuleMapper.java
├── service
│   ├── RiskControlService.java
│   ├── RiskListService.java
│   └── RiskRuleService.java
└── service
    └── impl
        ├── RiskControlServiceImpl.java
        ├── RiskListServiceImpl.java
        └── RiskRuleServiceImpl.java
```

如果项目已经有统一的 `common`、`domain`、`infrastructure` 分层，也可以将上述结构合并到现有规范中。核心原则是：风控模块内部自闭环，对外只暴露 `RiskControlService` 作为业务接入入口。

### 核心类说明

| 类名               | 类型     | 作用                                           |
| ------------------ | -------- | ---------------------------------------------- |
| RiskCheckContext   | DTO      | 风控请求上下文，封装用户、IP、设备、场景等信息 |
| RiskCheckResult    | Result   | 风控校验结果，返回是否放行、拒绝原因           |
| RiskRule           | Entity   | 风控规则实体                                   |
| RiskList           | Entity   | 黑白名单实体                                   |
| RiskHitRecord      | Entity   | 风控命中记录实体                               |
| RiskControlService | Service  | 风控校验统一入口                               |
| RiskListService    | Service  | 黑白名单管理与查询                             |
| RiskRuleService    | Service  | 风控规则管理与查询                             |
| RiskCacheConstant  | Constant | Redis Key 常量                                 |
| RiskTargetTypeEnum | Enum     | 目标类型枚举：用户、IP、手机号、设备           |
| RiskListTypeEnum   | Enum     | 名单类型枚举：黑名单、白名单                   |
| RiskActionEnum     | Enum     | 风控处理动作枚举：放行、拒绝                   |

对业务接口来说，最核心的调用方式应该保持简单：

```java
RiskCheckResult result = riskControlService.check(context);
if (!result.isPass()) {
    throw new BizException(result.getMessage());
}
```

## 依赖与配置

本案例使用 Spring Boot 3、MyBatis-Plus、Redis、Redisson、Hutool、Lombok 和 Knife4j。数据库连接、Redis 地址等配置需要根据实际环境修改。

### Maven 依赖

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST 接口能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于 DTO 入参校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Redis：用于缓存黑白名单和启用规则 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Redisson：后续可扩展分布式锁、限流和布隆过滤器 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.30.0</version>
    </dependency>

    <!-- MyBatis-Plus：简化 CRUD 和分页查询 -->
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

    <!-- Hutool：提供字符串、集合、日期、JSON 等常用工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.28</version>
    </dependency>

    <!-- Knife4j：接口文档和调试页面 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>4.5.0</version>
    </dependency>

    <!-- Lombok：简化实体、DTO、日志对象代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

如果项目已经统一管理依赖版本，可以把版本号移动到父工程的 `dependencyManagement` 中。

### Redis 配置

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: risk-control-demo

  datasource:
    # MySQL 连接地址，按实际环境修改库名、地址和参数
    url: jdbc:mysql://127.0.0.1:3306/risk_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      # Redis 单机地址，生产环境可改为哨兵或集群
      host: 127.0.0.1
      port: 6379
      database: 0
      timeout: 3s
      lettuce:
        pool:
          # 最大连接数
          max-active: 16
          # 最大空闲连接数
          max-idle: 8
          # 最小空闲连接数
          min-idle: 2
          # 获取连接最大等待时间
          max-wait: 3s

mybatis-plus:
  configuration:
    # 开发环境建议开启 SQL 日志，生产环境按需关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 逻辑删除字段配置
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

knife4j:
  enable: true
  setting:
    # 开启接口分组和调试增强
    language: zh_cn

risk:
  cache:
    # 风控名单缓存过期时间，单位秒
    list-ttl-seconds: 300
    # 风控规则缓存过期时间，单位秒
    rule-ttl-seconds: 300
```

这里使用较短 TTL 是为了方便演示。生产环境可以根据后台变更频率设置为 10 分钟到 1 小时，也可以在新增、修改、删除规则时主动删除缓存。

### 规则缓存配置

规则缓存配置主要包含两部分：缓存 Key 常量和配置项绑定类。后续服务层会基于这些配置读写 Redis。

文件位置：`src/main/java/io/github/atengk/risk/common/constant/RiskCacheConstant.java`

```java
package io.github.atengk.risk.common.constant;

/**
 * 风控缓存常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class RiskCacheConstant {

    /**
     * 风控缓存统一前缀
     */
    public static final String PREFIX = "risk:";

    /**
     * 黑白名单缓存 Key 前缀
     * 格式：risk:list:{listType}:{targetType}:{sceneCode}:{targetValue}
     */
    public static final String LIST_KEY_PREFIX = PREFIX + "list:";

    /**
     * 风控规则缓存 Key 前缀
     * 格式：risk:rule:{sceneCode}
     */
    public static final String RULE_KEY_PREFIX = PREFIX + "rule:";

    /**
     * 全局场景标识，用于 sceneCode 为空的名单
     */
    public static final String GLOBAL_SCENE = "GLOBAL";

    private RiskCacheConstant() {
    }
}
```

文件位置：`src/main/java/io/github/atengk/risk/config/RiskCacheProperties.java`

```java
package io.github.atengk.risk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 风控缓存配置属性
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Component
@ConfigurationProperties(prefix = "risk.cache")
public class RiskCacheProperties {

    /**
     * 黑白名单缓存过期时间，单位秒
     */
    private Long listTtlSeconds = 300L;

    /**
     * 风控规则缓存过期时间，单位秒
     */
    private Long ruleTtlSeconds = 300L;
}
```

Redis Key 设计如下：

| 数据           | Key 示例                           | 说明                 |
| -------------- | ---------------------------------- | -------------------- |
| 全局用户黑名单 | risk:list:BLACK:USER:GLOBAL:10001  | 所有场景生效         |
| 登录 IP 白名单 | risk:list:WHITE:IP:LOGIN:127.0.0.1 | 只在登录场景生效     |
| 下单规则缓存   | risk:rule:ORDER_CREATE             | 缓存下单场景启用规则 |

缓存设计原则：

1. 黑白名单按精确值缓存，适合快速判断。
2. 规则按业务场景缓存，适合一次性加载当前场景下的启用规则。
3. 后台变更规则或名单后，优先删除对应缓存，让下一次请求重新加载。
4. 白名单和黑名单不要共用同一个 Key，避免判断逻辑混乱。

## 枚举与常量定义

枚举用于约束数据库字段值，避免代码中出现大量硬编码字符串。数据库中保存枚举的 `code`，接口返回和日志记录时也使用 `code`。

### 风控规则类型枚举

这里将“规则类型”和“名单目标类型”统一抽象为 `RiskTargetTypeEnum`，表示风控要匹配的目标维度。

文件位置：`src/main/java/io/github/atengk/risk/enums/RiskTargetTypeEnum.java`

```java
package io.github.atengk.risk.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 风控目标类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum RiskTargetTypeEnum {

    /**
     * 用户ID
     */
    USER("USER", "用户"),

    /**
     * 请求IP
     */
    IP("IP", "IP地址"),

    /**
     * 手机号
     */
    MOBILE("MOBILE", "手机号"),

    /**
     * 设备ID
     */
    DEVICE("DEVICE", "设备");

    private final String code;

    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 目标类型枚举
     */
    public static RiskTargetTypeEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的风控目标类型：" + code));
    }

    /**
     * 判断编码是否合法
     *
     * @param code 编码
     * @return 是否合法
     */
    public static boolean valid(String code) {
        return Arrays.stream(values())
                .anyMatch(item -> StrUtil.equalsIgnoreCase(item.getCode(), code));
    }
}
```

后续规则表中的 `rule_type` 和名单表中的 `target_type` 都使用这个枚举值。

### 黑白名单类型枚举

黑白名单类型用于区分当前名单数据是放行名单还是拦截名单。

文件位置：`src/main/java/io/github/atengk/risk/enums/RiskListTypeEnum.java`

```java
package io.github.atengk.risk.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 风控名单类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum RiskListTypeEnum {

    /**
     * 黑名单
     */
    BLACK("BLACK", "黑名单"),

    /**
     * 白名单
     */
    WHITE("WHITE", "白名单");

    private final String code;

    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 名单类型枚举
     */
    public static RiskListTypeEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的名单类型：" + code));
    }

    /**
     * 判断是否为黑名单
     *
     * @param code 编码
     * @return 是否黑名单
     */
    public static boolean isBlack(String code) {
        return StrUtil.equalsIgnoreCase(BLACK.getCode(), code);
    }

    /**
     * 判断是否为白名单
     *
     * @param code 编码
     * @return 是否白名单
     */
    public static boolean isWhite(String code) {
        return StrUtil.equalsIgnoreCase(WHITE.getCode(), code);
    }
}
```

### 风控处理结果枚举

处理结果用于表示风控最终动作。当前案例只实现 `PASS` 和 `REJECT`，后续可以扩展 `VERIFY`，例如要求短信验证、人机验证、二次确认等。

文件位置：`src/main/java/io/github/atengk/risk/enums/RiskActionEnum.java`

```java
package io.github.atengk.risk.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 风控处理动作枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum RiskActionEnum {

    /**
     * 放行
     */
    PASS("PASS", "放行"),

    /**
     * 拒绝
     */
    REJECT("REJECT", "拒绝");

    private final String code;

    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 处理动作枚举
     */
    public static RiskActionEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的风控处理动作：" + code));
    }

    /**
     * 判断是否放行
     *
     * @param code 编码
     * @return 是否放行
     */
    public static boolean isPass(String code) {
        return StrUtil.equalsIgnoreCase(PASS.getCode(), code);
    }

    /**
     * 判断是否拒绝
     *
     * @param code 编码
     * @return 是否拒绝
     */
    public static boolean isReject(String code) {
        return StrUtil.equalsIgnoreCase(REJECT.getCode(), code);
    }
}
```

命中记录还需要一个命中来源枚举，用于区分是命中白名单、黑名单还是普通规则。

文件位置：`src/main/java/io/github/atengk/risk/enums/RiskHitTypeEnum.java`

```java
package io.github.atengk.risk.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 风控命中类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum RiskHitTypeEnum {

    /**
     * 白名单
     */
    WHITE_LIST("WHITE_LIST", "白名单"),

    /**
     * 黑名单
     */
    BLACK_LIST("BLACK_LIST", "黑名单"),

    /**
     * 风控规则
     */
    RULE("RULE", "风控规则");

    private final String code;

    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 命中类型枚举
     */
    public static RiskHitTypeEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的风控命中类型：" + code));
    }
}
```

当前阶段完成后，后续代码可以直接基于这些表、配置和枚举继续实现实体类、Mapper、DTO、Service 和具体风控判断逻辑。

## 实体类与 Mapper

这一部分先把数据库表对应的实体类和 Mapper 补齐。实体类字段与前面 SQL 表结构保持一致，后续 Service 层可以直接基于 MyBatis-Plus 操作数据库。

### 风控规则实体

`RiskRule` 对应 `risk_rule` 表，用于保存某个业务场景下的风控规则。

文件位置：`src/main/java/io/github/atengk/risk/entity/RiskRule.java`

```java
package io.github.atengk.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控规则实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("risk_rule")
public class RiskRule {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 业务场景编码，例如 LOGIN、ORDER_CREATE
     */
    private String sceneCode;

    /**
     * 规则类型：USER、IP、MOBILE、DEVICE
     */
    private String ruleType;

    /**
     * 匹配值
     */
    private String matchValue;

    /**
     * 处理动作：PASS、REJECT
     */
    private String action;

    /**
     * 是否启用：0-停用，1-启用
     */
    private Integer enabled;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
```

### 黑白名单实体

`RiskList` 对应 `risk_list` 表，黑名单和白名单共用一张表，通过 `listType` 区分。

文件位置：`src/main/java/io/github/atengk/risk/entity/RiskList.java`

```java
package io.github.atengk.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控黑白名单实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("risk_list")
public class RiskList {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 名单类型：BLACK、WHITE
     */
    private String listType;

    /**
     * 目标类型：USER、IP、MOBILE、DEVICE
     */
    private String targetType;

    /**
     * 目标值
     */
    private String targetValue;

    /**
     * 业务场景编码，为空表示全局生效
     */
    private String sceneCode;

    /**
     * 是否启用：0-停用，1-启用
     */
    private Integer enabled;

    /**
     * 过期时间，为空表示永久有效
     */
    private LocalDateTime expireTime;

    /**
     * 加入名单原因
     */
    private String reason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
```

### 命中记录实体

`RiskHitRecord` 对应 `risk_hit_record` 表，用于记录每次命中白名单、黑名单或风控规则的结果。

文件位置：`src/main/java/io/github/atengk/risk/entity/RiskHitRecord.java`

```java
package io.github.atengk.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控命中记录实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("risk_hit_record")
public class RiskHitRecord {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 业务场景编码
     */
    private String sceneCode;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 请求IP
     */
    private String requestIp;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 命中类型：WHITE_LIST、BLACK_LIST、RULE
     */
    private String hitType;

    /**
     * 命中的值
     */
    private String hitValue;

    /**
     * 命中的规则ID或名单ID
     */
    private Long hitSourceId;

    /**
     * 处理结果：PASS、REJECT
     */
    private String result;

    /**
     * 命中原因
     */
    private String reason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

### MyBatis-Plus Mapper

Mapper 只需要继承 `BaseMapper` 即可满足当前案例的 CRUD 和条件查询需求。复杂查询暂时不需要 XML。

文件位置：`src/main/java/io/github/atengk/risk/mapper/RiskRuleMapper.java`

```java
package io.github.atengk.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.risk.entity.RiskRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控规则 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface RiskRuleMapper extends BaseMapper<RiskRule> {
}
```

文件位置：`src/main/java/io/github/atengk/risk/mapper/RiskListMapper.java`

```java
package io.github.atengk.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.risk.entity.RiskList;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控黑白名单 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface RiskListMapper extends BaseMapper<RiskList> {
}
```

文件位置：`src/main/java/io/github/atengk/risk/mapper/RiskHitRecordMapper.java`

```java
package io.github.atengk.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.risk.entity.RiskHitRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控命中记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface RiskHitRecordMapper extends BaseMapper<RiskHitRecord> {
}
```

## 风控规则核心实现

风控规则核心实现分为两层：第一层是 `RiskCheckContext`，用于封装本次请求的上下文；第二层是不同维度的规则匹配器，例如 IP、用户、设备等。

### 风控请求上下文对象

`RiskCheckContext` 是风控判断的统一入参。业务接口只需要把用户、IP、设备、手机号、业务场景等信息放进上下文，再调用风控服务即可。

文件位置：`src/main/java/io/github/atengk/risk/dto/RiskCheckContext.java`

```java
package io.github.atengk.risk.dto;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.risk.enums.RiskTargetTypeEnum;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 风控请求上下文
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class RiskCheckContext {

    /**
     * 请求ID，用于链路追踪
     */
    private String requestId;

    /**
     * 业务场景编码，例如 LOGIN、ORDER_CREATE
     */
    private String sceneCode;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 请求IP
     */
    private String requestIp;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 扩展参数
     */
    private Map<String, Object> extra = new HashMap<>();

    /**
     * 初始化基础字段
     *
     * @return 当前上下文
     */
    public RiskCheckContext init() {
        if (StrUtil.isBlank(this.requestId)) {
            this.requestId = IdUtil.fastSimpleUUID();
        }
        return this;
    }

    /**
     * 根据风控目标类型获取对应值
     *
     * @param targetType 目标类型
     * @return 目标值
     */
    public String getTargetValue(RiskTargetTypeEnum targetType) {
        if (targetType == null) {
            return null;
        }
        return switch (targetType) {
            case USER -> this.userId == null ? null : String.valueOf(this.userId);
            case IP -> this.requestIp;
            case MOBILE -> this.mobile;
            case DEVICE -> this.deviceId;
        };
    }
}
```

业务接口构建上下文的方式类似下面这样：

```java
RiskCheckContext context = new RiskCheckContext();
context.setSceneCode("LOGIN");
context.setUserId(10001L);
context.setMobile("13800000000");
context.setRequestIp("192.168.10.20");
context.setDeviceId("device_abc_001");
context.init();
```

### 规则匹配接口设计

每种规则类型单独实现一个匹配器，避免在一个大方法中写大量 `if else`。后续新增手机号、城市、渠道、订单金额等规则，只需要增加新的匹配器。

文件位置：`src/main/java/io/github/atengk/risk/rule/RiskRuleMatcher.java`

```java
package io.github.atengk.risk.rule;

import io.github.atengk.risk.dto.RiskCheckContext;
import io.github.atengk.risk.entity.RiskRule;

/**
 * 风控规则匹配器
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface RiskRuleMatcher {

    /**
     * 是否支持当前规则类型
     *
     * @param ruleType 规则类型
     * @return 是否支持
     */
    boolean support(String ruleType);

    /**
     * 判断规则是否命中
     *
     * @param rule    风控规则
     * @param context 风控上下文
     * @return 是否命中
     */
    boolean match(RiskRule rule, RiskCheckContext context);
}
```

为了后续统一返回命中信息，可以定义一个规则命中结果对象。

文件位置：`src/main/java/io/github/atengk/risk/common/result/RiskRuleHitResult.java`

```java
package io.github.atengk.risk.common.result;

import io.github.atengk.risk.entity.RiskRule;
import lombok.Builder;
import lombok.Data;

/**
 * 风控规则命中结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class RiskRuleHitResult {

    /**
     * 是否命中
     */
    private boolean hit;

    /**
     * 命中的规则
     */
    private RiskRule rule;

    /**
     * 命中值
     */
    private String hitValue;

    /**
     * 命中原因
     */
    private String reason;

    /**
     * 未命中结果
     *
     * @return 未命中结果
     */
    public static RiskRuleHitResult notHit() {
        return RiskRuleHitResult.builder()
                .hit(false)
                .build();
    }
}
```

规则匹配服务负责加载指定场景下的启用规则，并按匹配器执行判断。

文件位置：`src/main/java/io/github/atengk/risk/service/RiskRuleService.java`

```java
package io.github.atengk.risk.service;

import io.github.atengk.risk.common.result.RiskRuleHitResult;
import io.github.atengk.risk.dto.RiskCheckContext;

/**
 * 风控规则服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface RiskRuleService {

    /**
     * 检查是否命中风控规则
     *
     * @param context 风控上下文
     * @return 规则命中结果
     */
    RiskRuleHitResult checkRule(RiskCheckContext context);
}
```

`RiskRuleServiceImpl` 会优先从 Redis 读取场景规则，缓存没有命中时再查询数据库。

文件位置：`src/main/java/io/github/atengk/risk/service/impl/RiskRuleServiceImpl.java`

```java
package io.github.atengk.risk.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.risk.common.constant.RiskCacheConstant;
import io.github.atengk.risk.common.result.RiskRuleHitResult;
import io.github.atengk.risk.config.RiskCacheProperties;
import io.github.atengk.risk.dto.RiskCheckContext;
import io.github.atengk.risk.entity.RiskRule;
import io.github.atengk.risk.enums.RiskTargetTypeEnum;
import io.github.atengk.risk.mapper.RiskRuleMapper;
import io.github.atengk.risk.rule.RiskRuleMatcher;
import io.github.atengk.risk.service.RiskRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 风控规则服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskRuleServiceImpl implements RiskRuleService {

    private final RiskRuleMapper riskRuleMapper;

    private final List<RiskRuleMatcher> riskRuleMatchers;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RiskCacheProperties riskCacheProperties;

    /**
     * 检查是否命中风控规则
     *
     * @param context 风控上下文
     * @return 规则命中结果
     */
    @Override
    public RiskRuleHitResult checkRule(RiskCheckContext context) {
        List<RiskRule> rules = this.loadEnabledRules(context.getSceneCode());
        if (CollUtil.isEmpty(rules)) {
            return RiskRuleHitResult.notHit();
        }

        for (RiskRule rule : rules) {
            RiskRuleMatcher matcher = this.getMatcher(rule.getRuleType());
            if (matcher == null) {
                log.warn("未找到风控规则匹配器，ruleId={}，ruleType={}", rule.getId(), rule.getRuleType());
                continue;
            }

            boolean hit = matcher.match(rule, context);
            if (!hit) {
                continue;
            }

            RiskTargetTypeEnum targetType = RiskTargetTypeEnum.of(rule.getRuleType());
            String hitValue = context.getTargetValue(targetType);
            log.info("风控规则命中，requestId={}，sceneCode={}，ruleId={}，hitValue={}",
                    context.getRequestId(), context.getSceneCode(), rule.getId(), hitValue);

            return RiskRuleHitResult.builder()
                    .hit(true)
                    .rule(rule)
                    .hitValue(hitValue)
                    .reason(StrUtil.blankToDefault(rule.getRemark(), rule.getRuleName()))
                    .build();
        }

        return RiskRuleHitResult.notHit();
    }

    /**
     * 加载指定场景下启用的风控规则
     *
     * @param sceneCode 业务场景编码
     * @return 风控规则列表
     */
    @SuppressWarnings("unchecked")
    private List<RiskRule> loadEnabledRules(String sceneCode) {
        if (StrUtil.isBlank(sceneCode)) {
            return Collections.emptyList();
        }

        String cacheKey = RiskCacheConstant.RULE_KEY_PREFIX + sceneCode;
        Object cacheValue = redisTemplate.opsForValue().get(cacheKey);
        if (cacheValue instanceof List<?>) {
            return (List<RiskRule>) cacheValue;
        }

        List<RiskRule> rules = riskRuleMapper.selectList(Wrappers.<RiskRule>lambdaQuery()
                .eq(RiskRule::getSceneCode, sceneCode)
                .eq(RiskRule::getEnabled, 1)
                .orderByDesc(RiskRule::getUpdateTime));

        redisTemplate.opsForValue().set(
                cacheKey,
                CollUtil.emptyIfNull(rules),
                riskCacheProperties.getRuleTtlSeconds(),
                TimeUnit.SECONDS
        );

        log.info("风控规则缓存加载完成，sceneCode={}，size={}", sceneCode, CollUtil.size(rules));
        return rules;
    }

    /**
     * 获取匹配器
     *
     * @param ruleType 规则类型
     * @return 匹配器
     */
    private RiskRuleMatcher getMatcher(String ruleType) {
        return riskRuleMatchers.stream()
                .filter(item -> item.support(ruleType))
                .findFirst()
                .orElse(null);
    }
}
```

### IP 规则匹配实现

IP 匹配器支持两种写法：精确匹配和简单通配符前缀匹配。

示例：

```text
192.168.10.20    精确匹配
192.168.10.*     前缀匹配
```

文件位置：`src/main/java/io/github/atengk/risk/rule/IpRiskRuleMatcher.java`

```java
package io.github.atengk.risk.rule;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.risk.dto.RiskCheckContext;
import io.github.atengk.risk.entity.RiskRule;
import io.github.atengk.risk.enums.RiskTargetTypeEnum;
import org.springframework.stereotype.Component;

/**
 * IP 风控规则匹配器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
public class IpRiskRuleMatcher implements RiskRuleMatcher {

    /**
     * 是否支持当前规则类型
     *
     * @param ruleType 规则类型
     * @return 是否支持
     */
    @Override
    public boolean support(String ruleType) {
        return StrUtil.equalsIgnoreCase(RiskTargetTypeEnum.IP.getCode(), ruleType);
    }

    /**
     * 判断 IP 规则是否命中
     *
     * @param rule    风控规则
     * @param context 风控上下文
     * @return 是否命中
     */
    @Override
    public boolean match(RiskRule rule, RiskCheckContext context) {
        String requestIp = context.getRequestIp();
        String matchValue = rule.getMatchValue();

        if (StrUtil.hasBlank(requestIp, matchValue)) {
            return false;
        }

        if (StrUtil.endWith(matchValue, "*")) {
            String prefix = StrUtil.removeSuffix(matchValue, "*");
            return StrUtil.startWith(requestIp, prefix);
        }

        return StrUtil.equals(requestIp, matchValue);
    }
}
```

### 用户规则匹配实现

用户规则匹配器基于 `userId` 判断，适合登录、下单、支付、领券等需要拦截指定用户的场景。

文件位置：`src/main/java/io/github/atengk/risk/rule/UserRiskRuleMatcher.java`

```java
package io.github.atengk.risk.rule;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.risk.dto.RiskCheckContext;
import io.github.atengk.risk.entity.RiskRule;
import io.github.atengk.risk.enums.RiskTargetTypeEnum;
import org.springframework.stereotype.Component;

/**
 * 用户风控规则匹配器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
public class UserRiskRuleMatcher implements RiskRuleMatcher {

    /**
     * 是否支持当前规则类型
     *
     * @param ruleType 规则类型
     * @return 是否支持
     */
    @Override
    public boolean support(String ruleType) {
        return StrUtil.equalsIgnoreCase(RiskTargetTypeEnum.USER.getCode(), ruleType);
    }

    /**
     * 判断用户规则是否命中
     *
     * @param rule    风控规则
     * @param context 风控上下文
     * @return 是否命中
     */
    @Override
    public boolean match(RiskRule rule, RiskCheckContext context) {
        if (context.getUserId() == null || StrUtil.isBlank(rule.getMatchValue())) {
            return false;
        }
        return StrUtil.equals(String.valueOf(context.getUserId()), rule.getMatchValue());
    }
}
```

### 设备规则匹配实现

设备规则匹配器基于 `deviceId` 判断，适合识别批量注册设备、异常下单设备、模拟器设备等场景。

文件位置：`src/main/java/io/github/atengk/risk/rule/DeviceRiskRuleMatcher.java`

```java
package io.github.atengk.risk.rule;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.risk.dto.RiskCheckContext;
import io.github.atengk.risk.entity.RiskRule;
import io.github.atengk.risk.enums.RiskTargetTypeEnum;
import org.springframework.stereotype.Component;

/**
 * 设备风控规则匹配器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Component
public class DeviceRiskRuleMatcher implements RiskRuleMatcher {

    /**
     * 是否支持当前规则类型
     *
     * @param ruleType 规则类型
     * @return 是否支持
     */
    @Override
    public boolean support(String ruleType) {
        return StrUtil.equalsIgnoreCase(RiskTargetTypeEnum.DEVICE.getCode(), ruleType);
    }

    /**
     * 判断设备规则是否命中
     *
     * @param rule    风控规则
     * @param context 风控上下文
     * @return 是否命中
     */
    @Override
    public boolean match(RiskRule rule, RiskCheckContext context) {
        if (StrUtil.hasBlank(context.getDeviceId(), rule.getMatchValue())) {
            return false;
        }
        return StrUtil.equals(context.getDeviceId(), rule.getMatchValue());
    }
}
```

手机号规则和用户规则写法类似，如果项目需要手机号维度规则，可以增加一个 `MobileRiskRuleMatcher`，判断 `context.getMobile()` 与 `rule.getMatchValue()` 是否一致。

## 黑白名单核心实现

黑白名单逻辑独立于普通规则逻辑。风控判断时先检查白名单，再检查黑名单，最后才执行普通风控规则。

核心顺序固定为：

```text
白名单命中 → 直接放行
黑名单命中 → 直接拒绝
未命中名单 → 继续执行风控规则
```

### 白名单优先放行逻辑

先定义黑白名单命中结果对象，方便 Service 返回命中详情。

文件位置：`src/main/java/io/github/atengk/risk/common/result/RiskListHitResult.java`

```java
package io.github.atengk.risk.common.result;

import io.github.atengk.risk.entity.RiskList;
import lombok.Builder;
import lombok.Data;

/**
 * 风控名单命中结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class RiskListHitResult {

    /**
     * 是否命中
     */
    private boolean hit;

    /**
     * 命中的名单
     */
    private RiskList riskList;

    /**
     * 命中值
     */
    private String hitValue;

    /**
     * 命中原因
     */
    private String reason;

    /**
     * 未命中结果
     *
     * @return 未命中结果
     */
    public static RiskListHitResult notHit() {
        return RiskListHitResult.builder()
                .hit(false)
                .build();
    }
}
```

黑白名单服务接口提供两个核心方法：检查白名单、检查黑名单。

文件位置：`src/main/java/io/github/atengk/risk/service/RiskListService.java`

```java
package io.github.atengk.risk.service;

import io.github.atengk.risk.common.result.RiskListHitResult;
import io.github.atengk.risk.dto.RiskCheckContext;

/**
 * 风控黑白名单服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface RiskListService {

    /**
     * 检查是否命中白名单
     *
     * @param context 风控上下文
     * @return 名单命中结果
     */
    RiskListHitResult checkWhiteList(RiskCheckContext context);

    /**
     * 检查是否命中黑名单
     *
     * @param context 风控上下文
     * @return 名单命中结果
     */
    RiskListHitResult checkBlackList(RiskCheckContext context);
}
```

`RiskListServiceImpl` 会按 USER、IP、MOBILE、DEVICE 四个维度检查白名单。只要有一个维度命中白名单，就直接返回命中结果。

文件位置：`src/main/java/io/github/atengk/risk/service/impl/RiskListServiceImpl.java`

```java
package io.github.atengk.risk.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.risk.common.constant.RiskCacheConstant;
import io.github.atengk.risk.common.result.RiskListHitResult;
import io.github.atengk.risk.config.RiskCacheProperties;
import io.github.atengk.risk.dto.RiskCheckContext;
import io.github.atengk.risk.entity.RiskList;
import io.github.atengk.risk.enums.RiskListTypeEnum;
import io.github.atengk.risk.enums.RiskTargetTypeEnum;
import io.github.atengk.risk.mapper.RiskListMapper;
import io.github.atengk.risk.service.RiskListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 风控黑白名单服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskListServiceImpl implements RiskListService {

    private final RiskListMapper riskListMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RiskCacheProperties riskCacheProperties;

    /**
     * 检查是否命中白名单
     *
     * @param context 风控上下文
     * @return 名单命中结果
     */
    @Override
    public RiskListHitResult checkWhiteList(RiskCheckContext context) {
        return this.checkList(context, RiskListTypeEnum.WHITE);
    }

    /**
     * 检查是否命中黑名单
     *
     * @param context 风控上下文
     * @return 名单命中结果
     */
    @Override
    public RiskListHitResult checkBlackList(RiskCheckContext context) {
        return this.checkList(context, RiskListTypeEnum.BLACK);
    }

    /**
     * 检查指定类型名单是否命中
     *
     * @param context  风控上下文
     * @param listType 名单类型
     * @return 名单命中结果
     */
    private RiskListHitResult checkList(RiskCheckContext context, RiskListTypeEnum listType) {
        for (RiskTargetTypeEnum targetType : RiskTargetTypeEnum.values()) {
            String targetValue = context.getTargetValue(targetType);
            if (StrUtil.isBlank(targetValue)) {
                continue;
            }

            RiskList sceneHit = this.loadRiskList(listType, targetType, context.getSceneCode(), targetValue);
            if (this.isValid(sceneHit)) {
                return this.buildHitResult(context, listType, targetType, targetValue, sceneHit);
            }

            RiskList globalHit = this.loadRiskList(listType, targetType, RiskCacheConstant.GLOBAL_SCENE, targetValue);
            if (this.isValid(globalHit)) {
                return this.buildHitResult(context, listType, targetType, targetValue, globalHit);
            }
        }

        return RiskListHitResult.notHit();
    }

    /**
     * 构建名单命中结果
     *
     * @param context     风控上下文
     * @param listType    名单类型
     * @param targetType  目标类型
     * @param targetValue 目标值
     * @param riskList    命中的名单
     * @return 名单命中结果
     */
    private RiskListHitResult buildHitResult(RiskCheckContext context,
                                             RiskListTypeEnum listType,
                                             RiskTargetTypeEnum targetType,
                                             String targetValue,
                                             RiskList riskList) {
        log.info("风控名单命中，requestId={}，sceneCode={}，listType={}，targetType={}，targetValue={}，listId={}",
                context.getRequestId(), context.getSceneCode(), listType.getCode(), targetType.getCode(),
                targetValue, riskList.getId());

        return RiskListHitResult.builder()
                .hit(true)
                .riskList(riskList)
                .hitValue(targetValue)
                .reason(StrUtil.blankToDefault(riskList.getReason(), listType.getDesc() + "命中"))
                .build();
    }

    /**
     * 判断名单是否有效
     *
     * @param riskList 名单记录
     * @return 是否有效
     */
    private boolean isValid(RiskList riskList) {
        if (riskList == null) {
            return false;
        }
        if (riskList.getEnabled() == null || riskList.getEnabled() != 1) {
            return false;
        }
        return riskList.getExpireTime() == null || LocalDateTimeUtil.compare(riskList.getExpireTime(), LocalDateTime.now()) > 0;
    }

    /**
     * 从缓存或数据库加载名单
     *
     * @param listType    名单类型
     * @param targetType  目标类型
     * @param sceneCode   业务场景编码
     * @param targetValue 目标值
     * @return 名单记录
     */
    private RiskList loadRiskList(RiskListTypeEnum listType,
                                  RiskTargetTypeEnum targetType,
                                  String sceneCode,
                                  String targetValue) {
        String finalSceneCode = StrUtil.blankToDefault(sceneCode, RiskCacheConstant.GLOBAL_SCENE);
        String cacheKey = this.buildListCacheKey(listType, targetType, finalSceneCode, targetValue);

        Object cacheValue = redisTemplate.opsForValue().get(cacheKey);
        if (cacheValue instanceof RiskList riskList) {
            return riskList;
        }

        RiskList riskList = this.queryRiskList(listType, targetType, finalSceneCode, targetValue);
        if (riskList != null) {
            redisTemplate.opsForValue().set(
                    cacheKey,
                    riskList,
                    riskCacheProperties.getListTtlSeconds(),
                    TimeUnit.SECONDS
            );
            return riskList;
        }

        redisTemplate.opsForValue().set(
                cacheKey,
                "",
                riskCacheProperties.getListTtlSeconds(),
                TimeUnit.SECONDS
        );
        return null;
    }

    /**
     * 查询数据库名单记录
     *
     * @param listType    名单类型
     * @param targetType  目标类型
     * @param sceneCode   业务场景编码
     * @param targetValue 目标值
     * @return 名单记录
     */
    private RiskList queryRiskList(RiskListTypeEnum listType,
                                   RiskTargetTypeEnum targetType,
                                   String sceneCode,
                                   String targetValue) {
        String querySceneCode = StrUtil.equals(sceneCode, RiskCacheConstant.GLOBAL_SCENE) ? null : sceneCode;

        return riskListMapper.selectOne(Wrappers.<RiskList>lambdaQuery()
                .eq(RiskList::getListType, listType.getCode())
                .eq(RiskList::getTargetType, targetType.getCode())
                .eq(RiskList::getTargetValue, targetValue)
                .eq(RiskList::getEnabled, 1)
                .and(wrapper -> wrapper
                        .isNull(RiskList::getExpireTime)
                        .or()
                        .gt(RiskList::getExpireTime, LocalDateTime.now()))
                .func(wrapper -> {
                    if (querySceneCode == null) {
                        wrapper.isNull(RiskList::getSceneCode);
                    } else {
                        wrapper.eq(RiskList::getSceneCode, querySceneCode);
                    }
                })
                .last("LIMIT 1"));
    }

    /**
     * 构建名单缓存 Key
     *
     * @param listType    名单类型
     * @param targetType  目标类型
     * @param sceneCode   业务场景编码
     * @param targetValue 目标值
     * @return Redis Key
     */
    private String buildListCacheKey(RiskListTypeEnum listType,
                                     RiskTargetTypeEnum targetType,
                                     String sceneCode,
                                     String targetValue) {
        return RiskCacheConstant.LIST_KEY_PREFIX
                + listType.getCode() + ":"
                + targetType.getCode() + ":"
                + sceneCode + ":"
                + targetValue;
    }
}
```

### 黑名单拦截逻辑

黑名单逻辑与白名单共用 `checkList` 方法，只是传入的名单类型不同。

实际风控总入口中应该按下面顺序调用：

```java
RiskListHitResult whiteHit = riskListService.checkWhiteList(context);
if (whiteHit.isHit()) {
    // 白名单命中，直接放行
    return;
}

RiskListHitResult blackHit = riskListService.checkBlackList(context);
if (blackHit.isHit()) {
    // 黑名单命中，直接拒绝
    throw new RuntimeException(blackHit.getReason());
}
```

在当前实现中，黑名单支持以下几类命中：

| 黑名单维度 | 上下文字段 | 示例                           |
| ---------- | ---------- | ------------------------------ |
| USER       | userId     | 用户 10001 被禁止登录或下单    |
| IP         | requestIp  | IP 192.168.10.20 被禁止访问    |
| MOBILE     | mobile     | 手机号 13800000000 被禁止领券  |
| DEVICE     | deviceId   | 设备 device_abc_001 被禁止下单 |

如果同一个请求同时命中多个黑名单，本案例默认返回第一个命中的结果。检查顺序取决于 `RiskTargetTypeEnum.values()` 的声明顺序，也就是：

```text
USER → IP → MOBILE → DEVICE
```

如果业务希望 IP 优先、设备优先或手机号优先，只需要调整枚举顺序，或者在 `checkList` 方法中改成固定列表即可。

### Redis 缓存加载逻辑

当前黑白名单缓存采用“精确 Key 查询”方式，每个名单值对应一个 Redis Key。

Key 格式如下：

```text
risk:list:{listType}:{targetType}:{sceneCode}:{targetValue}
```

示例：

```text
risk:list:WHITE:IP:GLOBAL:127.0.0.1
risk:list:BLACK:USER:LOGIN:10001
risk:list:BLACK:DEVICE:ORDER_CREATE:device_abc_001
```

加载逻辑如下：

```text
读取 Redis
  ↓
如果缓存中存在 RiskList 对象
  ↓
直接返回并判断是否有效
  ↓
如果 Redis 没有命中
  ↓
查询 MySQL
  ↓
查询到名单记录：写入 Redis
  ↓
查询不到名单记录：写入空字符串作为短期空缓存
```

空缓存的作用是减少不存在数据的数据库穿透。例如大量请求检查同一个未命中的 IP，如果不写空缓存，每次都会访问数据库。

当前代码中这段逻辑负责写入空缓存：

```java
redisTemplate.opsForValue().set(
        cacheKey,
        "",
        riskCacheProperties.getListTtlSeconds(),
        TimeUnit.SECONDS
);
```

需要注意的是，当前实现依赖 `RedisTemplate<String, Object>` 的对象序列化能力。如果项目中没有统一配置 Redis 序列化，建议增加下面这个配置，避免出现 JDK 默认序列化导致 Redis 中内容不可读的问题。

文件位置：`src/main/java/io/github/atengk/risk/config/RedisConfig.java`

```java
package io.github.atengk.risk.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.nio.charset.StandardCharsets;

/**
 * Redis 序列化配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate 序列化方式
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        RedisSerializer<String> stringSerializer = new StringRedisSerializer(StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }
}
```

到这里，实体、Mapper、规则匹配器、白名单优先放行、黑名单拦截和 Redis 缓存加载逻辑已经具备。下一部分可以继续把这些能力组装成统一的 `RiskControlService`，并实现命中记录保存、管理接口和业务接口接入示例。

## 风控决策流程

风控决策流程负责把“白名单判断、黑名单判断、规则判断、命中记录保存”串成一个统一入口。业务代码只调用 `RiskControlService#check`，不直接关心内部判断细节。

整体执行顺序固定为：

```text
构建风控上下文
  ↓
初始化 requestId
  ↓
检查白名单
  ↓
命中白名单：记录命中日志并放行
  ↓
检查黑名单
  ↓
命中黑名单：记录命中日志并拒绝
  ↓
检查风控规则
  ↓
命中拒绝规则：记录命中日志并拒绝
  ↓
未命中任何规则：默认放行
```

### 风控校验入口

先定义统一返回对象 `RiskCheckResult`。业务接口根据 `pass` 判断是否继续执行。

文件位置：`src/main/java/io/github/atengk/risk/common/result/RiskCheckResult.java`

```java
package io.github.atengk.risk.common.result;

import lombok.Builder;
import lombok.Data;

/**
 * 风控校验结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class RiskCheckResult {

    /**
     * 是否放行
     */
    private boolean pass;

    /**
     * 处理结果：PASS、REJECT
     */
    private String action;

    /**
     * 命中类型：WHITE_LIST、BLACK_LIST、RULE
     */
    private String hitType;

    /**
     * 命中值
     */
    private String hitValue;

    /**
     * 命中来源ID，可能是名单ID或规则ID
     */
    private Long hitSourceId;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 放行结果
     *
     * @param message 提示信息
     * @return 风控校验结果
     */
    public static RiskCheckResult pass(String message) {
        return RiskCheckResult.builder()
                .pass(true)
                .action("PASS")
                .message(message)
                .build();
    }

    /**
     * 拒绝结果
     *
     * @param hitType     命中类型
     * @param hitValue    命中值
     * @param hitSourceId 命中来源ID
     * @param message     提示信息
     * @return 风控校验结果
     */
    public static RiskCheckResult reject(String hitType, String hitValue, Long hitSourceId, String message) {
        return RiskCheckResult.builder()
                .pass(false)
                .action("REJECT")
                .hitType(hitType)
                .hitValue(hitValue)
                .hitSourceId(hitSourceId)
                .message(message)
                .build();
    }
}
```

定义统一风控服务接口。

文件位置：`src/main/java/io/github/atengk/risk/service/RiskControlService.java`

```java
package io.github.atengk.risk.service;

import io.github.atengk.risk.common.result.RiskCheckResult;
import io.github.atengk.risk.dto.RiskCheckContext;

/**
 * 风控校验服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface RiskControlService {

    /**
     * 执行风控校验
     *
     * @param context 风控上下文
     * @return 风控校验结果
     */
    RiskCheckResult check(RiskCheckContext context);
}
```

### 规则执行顺序

下面是风控总入口的完整实现。它会先检查白名单，再检查黑名单，最后检查普通风控规则。

文件位置：`src/main/java/io/github/atengk/risk/service/impl/RiskControlServiceImpl.java`

```java
package io.github.atengk.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.risk.common.result.RiskCheckResult;
import io.github.atengk.risk.common.result.RiskListHitResult;
import io.github.atengk.risk.common.result.RiskRuleHitResult;
import io.github.atengk.risk.dto.RiskCheckContext;
import io.github.atengk.risk.entity.RiskHitRecord;
import io.github.atengk.risk.enums.RiskActionEnum;
import io.github.atengk.risk.enums.RiskHitTypeEnum;
import io.github.atengk.risk.mapper.RiskHitRecordMapper;
import io.github.atengk.risk.service.RiskControlService;
import io.github.atengk.risk.service.RiskListService;
import io.github.atengk.risk.service.RiskRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 风控校验服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskControlServiceImpl implements RiskControlService {

    private final RiskListService riskListService;

    private final RiskRuleService riskRuleService;

    private final RiskHitRecordMapper riskHitRecordMapper;

    /**
     * 执行风控校验
     *
     * @param context 风控上下文
     * @return 风控校验结果
     */
    @Override
    public RiskCheckResult check(RiskCheckContext context) {
        context.init();

        if (StrUtil.isBlank(context.getSceneCode())) {
            log.warn("风控校验跳过，业务场景为空，requestId={}", context.getRequestId());
            return RiskCheckResult.pass("业务场景为空，默认放行");
        }

        RiskListHitResult whiteHit = riskListService.checkWhiteList(context);
        if (whiteHit.isHit()) {
            RiskCheckResult result = RiskCheckResult.builder()
                    .pass(true)
                    .action(RiskActionEnum.PASS.getCode())
                    .hitType(RiskHitTypeEnum.WHITE_LIST.getCode())
                    .hitValue(whiteHit.getHitValue())
                    .hitSourceId(whiteHit.getRiskList().getId())
                    .message("命中白名单，允许访问")
                    .build();

            this.saveHitRecord(context, result, whiteHit.getReason());
            log.info("风控白名单放行，requestId={}，sceneCode={}，hitValue={}",
                    context.getRequestId(), context.getSceneCode(), whiteHit.getHitValue());
            return result;
        }

        RiskListHitResult blackHit = riskListService.checkBlackList(context);
        if (blackHit.isHit()) {
            RiskCheckResult result = RiskCheckResult.reject(
                    RiskHitTypeEnum.BLACK_LIST.getCode(),
                    blackHit.getHitValue(),
                    blackHit.getRiskList().getId(),
                    StrUtil.blankToDefault(blackHit.getReason(), "命中黑名单，拒绝访问")
            );

            this.saveHitRecord(context, result, blackHit.getReason());
            log.warn("风控黑名单拦截，requestId={}，sceneCode={}，hitValue={}",
                    context.getRequestId(), context.getSceneCode(), blackHit.getHitValue());
            return result;
        }

        RiskRuleHitResult ruleHit = riskRuleService.checkRule(context);
        if (ruleHit.isHit()) {
            RiskCheckResult result = RiskCheckResult.reject(
                    RiskHitTypeEnum.RULE.getCode(),
                    ruleHit.getHitValue(),
                    ruleHit.getRule().getId(),
                    StrUtil.blankToDefault(ruleHit.getReason(), "命中风控规则，拒绝访问")
            );

            this.saveHitRecord(context, result, ruleHit.getReason());
            log.warn("风控规则拦截，requestId={}，sceneCode={}，ruleId={}，hitValue={}",
                    context.getRequestId(), context.getSceneCode(), ruleHit.getRule().getId(), ruleHit.getHitValue());
            return result;
        }

        log.info("风控校验通过，requestId={}，sceneCode={}", context.getRequestId(), context.getSceneCode());
        return RiskCheckResult.pass("风控校验通过");
    }

    /**
     * 保存风控命中记录
     *
     * @param context 风控上下文
     * @param result  风控结果
     * @param reason  命中原因
     */
    private void saveHitRecord(RiskCheckContext context, RiskCheckResult result, String reason) {
        RiskHitRecord record = new RiskHitRecord();
        record.setRequestId(context.getRequestId());
        record.setSceneCode(context.getSceneCode());
        record.setUserId(context.getUserId());
        record.setMobile(context.getMobile());
        record.setRequestIp(context.getRequestIp());
        record.setDeviceId(context.getDeviceId());
        record.setHitType(result.getHitType());
        record.setHitValue(result.getHitValue());
        record.setHitSourceId(result.getHitSourceId());
        record.setResult(result.getAction());
        record.setReason(StrUtil.blankToDefault(reason, result.getMessage()));
        record.setCreateTime(LocalDateTime.now());

        riskHitRecordMapper.insert(record);
    }
}
```

### 命中结果封装

当前风控结果分为三类：

| 命中类型   | 处理动作 | 说明                       |
| ---------- | -------- | -------------------------- |
| WHITE_LIST | PASS     | 命中白名单，直接放行       |
| BLACK_LIST | REJECT   | 命中黑名单，直接拒绝       |
| RULE       | REJECT   | 命中普通风控规则，拒绝请求 |

业务代码使用时不需要判断命中类型，只要判断是否放行即可：

```java
RiskCheckResult riskResult = riskControlService.check(context);
if (!riskResult.isPass()) {
    throw new RuntimeException(riskResult.getMessage());
}
```

如果项目中已有统一异常，例如 `BizException`，可以替换成：

```java
RiskCheckResult riskResult = riskControlService.check(context);
if (!riskResult.isPass()) {
    throw new BizException(riskResult.getMessage());
}
```

### 命中记录保存

命中记录只保存“发生命中”的请求，包括白名单放行、黑名单拦截、规则拦截。普通未命中放行请求不落库，避免数据量过大。

保存内容包括：

| 字段        | 来源             |
| ----------- | ---------------- |
| requestId   | RiskCheckContext |
| sceneCode   | RiskCheckContext |
| userId      | RiskCheckContext |
| mobile      | RiskCheckContext |
| requestIp   | RiskCheckContext |
| deviceId    | RiskCheckContext |
| hitType     | RiskCheckResult  |
| hitValue    | RiskCheckResult  |
| hitSourceId | 名单ID或规则ID   |
| result      | PASS 或 REJECT   |
| reason      | 命中原因         |

如果业务对审计要求较高，也可以把所有风控请求都保存下来，但需要注意表数据量增长，可以按月分表或定期归档。

## 管理接口实现

管理接口用于新增规则、启停规则、新增黑白名单、删除黑白名单。这里给出核心接口即可，适合后台管理页面或内部运营系统调用。

### 新增风控规则

先定义新增规则 DTO。

文件位置：`src/main/java/io/github/atengk/risk/dto/RiskRuleCreateDTO.java`

```java
package io.github.atengk.risk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增风控规则请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class RiskRuleCreateDTO {

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    /**
     * 业务场景编码
     */
    @NotBlank(message = "业务场景编码不能为空")
    private String sceneCode;

    /**
     * 规则类型：USER、IP、MOBILE、DEVICE
     */
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    /**
     * 匹配值
     */
    @NotBlank(message = "匹配值不能为空")
    private String matchValue;

    /**
     * 处理动作：PASS、REJECT
     */
    private String action = "REJECT";

    /**
     * 备注
     */
    private String remark;
}
```

新增规则和启停规则放在管理服务中实现，避免污染前面的规则匹配服务。

文件位置：`src/main/java/io/github/atengk/risk/service/RiskRuleManageService.java`

```java
package io.github.atengk.risk.service;

import io.github.atengk.risk.dto.RiskRuleCreateDTO;

/**
 * 风控规则管理服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface RiskRuleManageService {

    /**
     * 新增风控规则
     *
     * @param dto 新增规则请求
     * @return 规则ID
     */
    Long createRule(RiskRuleCreateDTO dto);

    /**
     * 更新规则启停状态
     *
     * @param id      规则ID
     * @param enabled 是否启用
     */
    void updateEnabled(Long id, Integer enabled);
}
```

文件位置：`src/main/java/io/github/atengk/risk/service/impl/RiskRuleManageServiceImpl.java`

```java
package io.github.atengk.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.risk.common.constant.RiskCacheConstant;
import io.github.atengk.risk.dto.RiskRuleCreateDTO;
import io.github.atengk.risk.entity.RiskRule;
import io.github.atengk.risk.enums.RiskActionEnum;
import io.github.atengk.risk.enums.RiskTargetTypeEnum;
import io.github.atengk.risk.mapper.RiskRuleMapper;
import io.github.atengk.risk.service.RiskRuleManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 风控规则管理服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskRuleManageServiceImpl implements RiskRuleManageService {

    private final RiskRuleMapper riskRuleMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 新增风控规则
     *
     * @param dto 新增规则请求
     * @return 规则ID
     */
    @Override
    public Long createRule(RiskRuleCreateDTO dto) {
        RiskTargetTypeEnum.of(dto.getRuleType());
        RiskActionEnum.of(dto.getAction());

        RiskRule rule = new RiskRule();
        rule.setRuleName(dto.getRuleName());
        rule.setSceneCode(dto.getSceneCode());
        rule.setRuleType(dto.getRuleType());
        rule.setMatchValue(dto.getMatchValue());
        rule.setAction(StrUtil.blankToDefault(dto.getAction(), RiskActionEnum.REJECT.getCode()));
        rule.setEnabled(1);
        rule.setRemark(dto.getRemark());
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        rule.setDeleted(0);

        riskRuleMapper.insert(rule);
        this.clearRuleCache(dto.getSceneCode());

        log.info("新增风控规则成功，ruleId={}，sceneCode={}，ruleType={}，matchValue={}",
                rule.getId(), dto.getSceneCode(), dto.getRuleType(), dto.getMatchValue());
        return rule.getId();
    }

    /**
     * 更新规则启停状态
     *
     * @param id      规则ID
     * @param enabled 是否启用
     */
    @Override
    public void updateEnabled(Long id, Integer enabled) {
        RiskRule oldRule = riskRuleMapper.selectById(id);
        if (oldRule == null) {
            throw new IllegalArgumentException("风控规则不存在");
        }

        RiskRule update = new RiskRule();
        update.setId(id);
        update.setEnabled(enabled);
        update.setUpdateTime(LocalDateTime.now());

        riskRuleMapper.updateById(update);
        this.clearRuleCache(oldRule.getSceneCode());

        log.info("更新风控规则状态成功，ruleId={}，enabled={}", id, enabled);
    }

    /**
     * 清理规则缓存
     *
     * @param sceneCode 业务场景编码
     */
    private void clearRuleCache(String sceneCode) {
        if (StrUtil.isBlank(sceneCode)) {
            return;
        }
        redisTemplate.delete(RiskCacheConstant.RULE_KEY_PREFIX + sceneCode);
    }
}
```

### 启用和停用规则

规则管理接口提供新增规则和启停规则两个接口。

文件位置：`src/main/java/io/github/atengk/risk/controller/RiskRuleController.java`

```java
package io.github.atengk.risk.controller;

import io.github.atengk.risk.dto.RiskRuleCreateDTO;
import io.github.atengk.risk.service.RiskRuleManageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 风控规则管理接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/risk/rules")
public class RiskRuleController {

    private final RiskRuleManageService riskRuleManageService;

    /**
     * 新增风控规则
     *
     * @param dto 新增规则请求
     * @return 规则ID
     */
    @PostMapping
    public Long createRule(@Valid @RequestBody RiskRuleCreateDTO dto) {
        return riskRuleManageService.createRule(dto);
    }

    /**
     * 启用或停用规则
     *
     * @param id      规则ID
     * @param enabled 是否启用：0-停用，1-启用
     */
    @PatchMapping("/{id}/enabled")
    public void updateEnabled(@PathVariable Long id,
                              @RequestParam @Min(0) @Max(1) Integer enabled) {
        riskRuleManageService.updateEnabled(id, enabled);
    }
}
```

接口调用示例：

```bash
# 新增登录 IP 拒绝规则
curl -X POST 'http://localhost:8080/risk/rules' \
  -H 'Content-Type: application/json' \
  -d '{
    "ruleName": "禁止异常IP登录",
    "sceneCode": "LOGIN",
    "ruleType": "IP",
    "matchValue": "192.168.10.20",
    "action": "REJECT",
    "remark": "异常登录来源"
  }'

# 停用规则
curl -X PATCH 'http://localhost:8080/risk/rules/1/enabled?enabled=0'

# 启用规则
curl -X PATCH 'http://localhost:8080/risk/rules/1/enabled?enabled=1'
```

### 新增黑白名单

先定义新增黑白名单 DTO。

文件位置：`src/main/java/io/github/atengk/risk/dto/RiskListCreateDTO.java`

```java
package io.github.atengk.risk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增黑白名单请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class RiskListCreateDTO {

    /**
     * 名单类型：BLACK、WHITE
     */
    @NotBlank(message = "名单类型不能为空")
    private String listType;

    /**
     * 目标类型：USER、IP、MOBILE、DEVICE
     */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    /**
     * 目标值
     */
    @NotBlank(message = "目标值不能为空")
    private String targetValue;

    /**
     * 业务场景编码，为空表示全局生效
     */
    private String sceneCode;

    /**
     * 过期时间，为空表示永久有效
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /**
     * 加入名单原因
     */
    private String reason;
}
```

定义名单管理服务。

文件位置：`src/main/java/io/github/atengk/risk/service/RiskListManageService.java`

```java
package io.github.atengk.risk.service;

import io.github.atengk.risk.dto.RiskListCreateDTO;

/**
 * 风控黑白名单管理服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface RiskListManageService {

    /**
     * 新增黑白名单
     *
     * @param dto 新增名单请求
     * @return 名单ID
     */
    Long createList(RiskListCreateDTO dto);

    /**
     * 删除黑白名单
     *
     * @param id 名单ID
     */
    void deleteList(Long id);
}
```

下面的服务实现负责新增名单、删除名单，并清理对应 Redis 缓存。

文件位置：`src/main/java/io/github/atengk/risk/service/impl/RiskListManageServiceImpl.java`

```java
package io.github.atengk.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.risk.common.constant.RiskCacheConstant;
import io.github.atengk.risk.dto.RiskListCreateDTO;
import io.github.atengk.risk.entity.RiskList;
import io.github.atengk.risk.enums.RiskListTypeEnum;
import io.github.atengk.risk.enums.RiskTargetTypeEnum;
import io.github.atengk.risk.mapper.RiskListMapper;
import io.github.atengk.risk.service.RiskListManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 风控黑白名单管理服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskListManageServiceImpl implements RiskListManageService {

    private final RiskListMapper riskListMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 新增黑白名单
     *
     * @param dto 新增名单请求
     * @return 名单ID
     */
    @Override
    public Long createList(RiskListCreateDTO dto) {
        RiskListTypeEnum.of(dto.getListType());
        RiskTargetTypeEnum.of(dto.getTargetType());

        RiskList oldList = riskListMapper.selectOne(Wrappers.<RiskList>lambdaQuery()
                .eq(RiskList::getListType, dto.getListType())
                .eq(RiskList::getTargetType, dto.getTargetType())
                .eq(RiskList::getTargetValue, dto.getTargetValue())
                .func(wrapper -> {
                    if (StrUtil.isBlank(dto.getSceneCode())) {
                        wrapper.isNull(RiskList::getSceneCode);
                    } else {
                        wrapper.eq(RiskList::getSceneCode, dto.getSceneCode());
                    }
                })
                .last("LIMIT 1"));

        if (oldList != null) {
            throw new IllegalArgumentException("名单已存在，请勿重复添加");
        }

        RiskList riskList = new RiskList();
        riskList.setListType(dto.getListType());
        riskList.setTargetType(dto.getTargetType());
        riskList.setTargetValue(dto.getTargetValue());
        riskList.setSceneCode(StrUtil.blankToNull(dto.getSceneCode()));
        riskList.setEnabled(1);
        riskList.setExpireTime(dto.getExpireTime());
        riskList.setReason(dto.getReason());
        riskList.setCreateTime(LocalDateTime.now());
        riskList.setUpdateTime(LocalDateTime.now());
        riskList.setDeleted(0);

        riskListMapper.insert(riskList);
        this.clearListCache(riskList);

        log.info("新增风控名单成功，listId={}，listType={}，targetType={}，targetValue={}，sceneCode={}",
                riskList.getId(), riskList.getListType(), riskList.getTargetType(),
                riskList.getTargetValue(), riskList.getSceneCode());

        return riskList.getId();
    }

    /**
     * 删除黑白名单
     *
     * @param id 名单ID
     */
    @Override
    public void deleteList(Long id) {
        RiskList riskList = riskListMapper.selectById(id);
        if (riskList == null) {
            throw new IllegalArgumentException("名单不存在");
        }

        riskListMapper.deleteById(id);
        this.clearListCache(riskList);

        log.info("删除风控名单成功，listId={}，listType={}，targetType={}，targetValue={}",
                id, riskList.getListType(), riskList.getTargetType(), riskList.getTargetValue());
    }

    /**
     * 清理名单缓存
     *
     * @param riskList 名单实体
     */
    private void clearListCache(RiskList riskList) {
        String sceneCode = StrUtil.blankToDefault(riskList.getSceneCode(), RiskCacheConstant.GLOBAL_SCENE);
        String cacheKey = RiskCacheConstant.LIST_KEY_PREFIX
                + riskList.getListType() + ":"
                + riskList.getTargetType() + ":"
                + sceneCode + ":"
                + riskList.getTargetValue();

        redisTemplate.delete(cacheKey);
    }
}
```

### 删除黑白名单

黑白名单管理接口提供新增和删除能力。删除时使用逻辑删除，MyBatis-Plus 会基于实体中的 `@TableLogic` 处理 `deleted` 字段。

文件位置：`src/main/java/io/github/atengk/risk/controller/RiskListController.java`

```java
package io.github.atengk.risk.controller;

import io.github.atengk.risk.dto.RiskListCreateDTO;
import io.github.atengk.risk.service.RiskListManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 风控黑白名单管理接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/risk/lists")
public class RiskListController {

    private final RiskListManageService riskListManageService;

    /**
     * 新增黑白名单
     *
     * @param dto 新增名单请求
     * @return 名单ID
     */
    @PostMapping
    public Long createList(@Valid @RequestBody RiskListCreateDTO dto) {
        return riskListManageService.createList(dto);
    }

    /**
     * 删除黑白名单
     *
     * @param id 名单ID
     */
    @DeleteMapping("/{id}")
    public void deleteList(@PathVariable Long id) {
        riskListManageService.deleteList(id);
    }
}
```

接口调用示例：

```bash
# 新增全局 IP 白名单
curl -X POST 'http://localhost:8080/risk/lists' \
  -H 'Content-Type: application/json' \
  -d '{
    "listType": "WHITE",
    "targetType": "IP",
    "targetValue": "127.0.0.1",
    "reason": "本地测试环境"
  }'

# 新增登录用户黑名单
curl -X POST 'http://localhost:8080/risk/lists' \
  -H 'Content-Type: application/json' \
  -d '{
    "listType": "BLACK",
    "targetType": "USER",
    "targetValue": "10001",
    "sceneCode": "LOGIN",
    "reason": "账号存在异常登录行为"
  }'

# 新增临时设备黑名单
curl -X POST 'http://localhost:8080/risk/lists' \
  -H 'Content-Type: application/json' \
  -d '{
    "listType": "BLACK",
    "targetType": "DEVICE",
    "targetValue": "device_abc_001",
    "sceneCode": "ORDER_CREATE",
    "expireTime": "2026-05-16 23:59:59",
    "reason": "设备疑似批量刷单"
  }'

# 删除名单
curl -X DELETE 'http://localhost:8080/risk/lists/1'
```

当前管理接口已经覆盖风控模块最核心的运营动作：

| 操作         | 接口                                       |
| ------------ | ------------------------------------------ |
| 新增风控规则 | `POST /risk/rules`                         |
| 启用规则     | `PATCH /risk/rules/{id}/enabled?enabled=1` |
| 停用规则     | `PATCH /risk/rules/{id}/enabled?enabled=0` |
| 新增黑白名单 | `POST /risk/lists`                         |
| 删除黑白名单 | `DELETE /risk/lists/{id}`                  |

到这里，风控决策主链路和后台管理入口已经具备。下一部分可以继续实现“业务接口接入示例”和“接口测试”，把登录、下单两个场景完整跑通。

## 业务接口接入示例

业务接口接入风控有两种方式：一种是在关键接口中手动构建 `RiskCheckContext` 并调用 `RiskControlService`；另一种是通过注解和拦截器统一接入。实际项目中，登录、支付、下单等核心链路建议显式调用，普通接口可以使用拦截器统一处理。

### 登录接口接入风控

登录接口通常还没有认证态，所以需要从请求参数、请求头和客户端 IP 中构建风控上下文。这里以手机号登录为例，校验维度包括手机号、IP、设备号。

先定义登录请求 DTO。

文件位置：`src/main/java/io/github/atengk/risk/dto/LoginRequestDTO.java`

```java
package io.github.atengk.risk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class LoginRequestDTO {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String mobile;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String verifyCode;

    /**
     * 设备ID
     */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;
}
```

定义一个简单的 IP 工具类，用于从请求头中获取真实客户端 IP。

文件位置：`src/main/java/io/github/atengk/risk/common/util/IpUtil.java`

```java
package io.github.atengk.risk.common.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class IpUtil {

    private static final String UNKNOWN = "unknown";

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    private IpUtil() {
    }

    /**
     * 获取客户端 IP
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (StrUtil.isNotBlank(ip) && !StrUtil.equalsIgnoreCase(ip, UNKNOWN)) {
                return StrUtil.splitTrim(ip, ",").get(0);
            }
        }
        return request.getRemoteAddr();
    }
}
```

登录接口中先执行风控校验，通过后再继续执行验证码校验、账号查询和 Token 生成等业务逻辑。

文件位置：`src/main/java/io/github/atengk/risk/controller/LoginController.java`

```java
package io.github.atengk.risk.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.risk.common.result.RiskCheckResult;
import io.github.atengk.risk.common.util.IpUtil;
import io.github.atengk.risk.dto.LoginRequestDTO;
import io.github.atengk.risk.dto.RiskCheckContext;
import io.github.atengk.risk.service.RiskControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 登录接口示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class LoginController {

    private final RiskControlService riskControlService;

    /**
     * 手机号登录
     *
     * @param dto     登录请求
     * @param request HTTP 请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        RiskCheckContext context = new RiskCheckContext();
        context.setSceneCode("LOGIN");
        context.setMobile(dto.getMobile());
        context.setRequestIp(IpUtil.getClientIp(request));
        context.setDeviceId(dto.getDeviceId());
        context.init();

        RiskCheckResult riskResult = riskControlService.check(context);
        if (!riskResult.isPass()) {
            log.warn("登录风控拦截，mobile={}，deviceId={}，message={}",
                    dto.getMobile(), dto.getDeviceId(), riskResult.getMessage());
            return MapUtil.builder(new java.util.HashMap<String, Object>())
                    .put("code", 403)
                    .put("message", riskResult.getMessage())
                    .build();
        }

        log.info("登录风控通过，mobile={}，deviceId={}", dto.getMobile(), dto.getDeviceId());

        return MapUtil.builder(new java.util.HashMap<String, Object>())
                .put("code", 200)
                .put("message", "登录成功")
                .put("token", "mock-token-" + dto.getMobile())
                .build();
    }
}
```

登录接口测试请求：

```bash
curl -X POST 'http://localhost:8080/auth/login' \
  -H 'Content-Type: application/json' \
  -H 'X-Real-IP: 192.168.10.20' \
  -d '{
    "mobile": "13800000000",
    "verifyCode": "123456",
    "deviceId": "device_abc_001"
  }'
```

### 下单接口接入风控

下单接口通常已经有登录态，可以从认证上下文中获取用户 ID。这里为了演示简单，直接从请求头 `X-User-Id` 获取用户 ID。

先定义下单请求 DTO。

文件位置：`src/main/java/io/github/atengk/risk/dto/OrderCreateDTO.java`

```java
package io.github.atengk.risk.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单请求参数
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderCreateDTO {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 下单数量
     */
    @NotNull(message = "下单数量不能为空")
    @Min(value = 1, message = "下单数量必须大于0")
    private Integer quantity;

    /**
     * 订单金额
     */
    @NotNull(message = "订单金额不能为空")
    private BigDecimal amount;

    /**
     * 设备ID
     */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;
}
```

下单接口接入风控时，场景编码使用 `ORDER_CREATE`。如果命中黑名单或规则，直接拒绝下单。

文件位置：`src/main/java/io/github/atengk/risk/controller/OrderController.java`

```java
package io.github.atengk.risk.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import io.github.atengk.risk.common.result.RiskCheckResult;
import io.github.atengk.risk.common.util.IpUtil;
import io.github.atengk.risk.dto.OrderCreateDTO;
import io.github.atengk.risk.dto.RiskCheckContext;
import io.github.atengk.risk.service.RiskControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单接口示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final RiskControlService riskControlService;

    /**
     * 创建订单
     *
     * @param dto     创建订单请求
     * @param request HTTP 请求
     * @return 下单结果
     */
    @PostMapping
    public Map<String, Object> createOrder(@Valid @RequestBody OrderCreateDTO dto, HttpServletRequest request) {
        Long userId = Convert.toLong(request.getHeader("X-User-Id"));

        RiskCheckContext context = new RiskCheckContext();
        context.setSceneCode("ORDER_CREATE");
        context.setUserId(userId);
        context.setRequestIp(IpUtil.getClientIp(request));
        context.setDeviceId(dto.getDeviceId());
        context.getExtra().put("productId", dto.getProductId());
        context.getExtra().put("amount", dto.getAmount());
        context.init();

        RiskCheckResult riskResult = riskControlService.check(context);
        if (!riskResult.isPass()) {
            log.warn("下单风控拦截，userId={}，productId={}，message={}",
                    userId, dto.getProductId(), riskResult.getMessage());
            return MapUtil.builder(new java.util.HashMap<String, Object>())
                    .put("code", 403)
                    .put("message", riskResult.getMessage())
                    .build();
        }

        log.info("下单风控通过，userId={}，productId={}，amount={}",
                userId, dto.getProductId(), dto.getAmount());

        return MapUtil.builder(new java.util.HashMap<String, Object>())
                .put("code", 200)
                .put("message", "下单成功")
                .put("orderNo", "ORDER202605150001")
                .build();
    }
}
```

下单接口测试请求：

```bash
curl -X POST 'http://localhost:8080/orders' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 10001' \
  -H 'X-Real-IP: 192.168.10.30' \
  -d '{
    "productId": 90001,
    "quantity": 1,
    "amount": 199.00,
    "deviceId": "device_abc_001"
  }'
```

### 使用拦截器统一接入

如果某些接口只需要基于用户 ID、IP、设备号进行通用风控，可以使用注解 + 拦截器统一处理。这样 Controller 中不需要重复写风控代码。

先定义风控注解。

文件位置：`src/main/java/io/github/atengk/risk/annotation/RiskProtected.java`

```java
package io.github.atengk.risk.annotation;

import java.lang.annotation.*;

/**
 * 风控保护注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RiskProtected {

    /**
     * 业务场景编码
     *
     * @return 业务场景编码
     */
    String sceneCode();
}
```

拦截器会读取注解中的 `sceneCode`，并从请求头中提取用户 ID、手机号、设备号和 IP。

文件位置：`src/main/java/io/github/atengk/risk/interceptor/RiskControlInterceptor.java`

```java
package io.github.atengk.risk.interceptor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.risk.annotation.RiskProtected;
import io.github.atengk.risk.common.result.RiskCheckResult;
import io.github.atengk.risk.common.util.IpUtil;
import io.github.atengk.risk.dto.RiskCheckContext;
import io.github.atengk.risk.service.RiskControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 风控拦截器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskControlInterceptor implements HandlerInterceptor {

    private final RiskControlService riskControlService;

    private final ObjectMapper objectMapper;

    /**
     * 请求前执行风控校验
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return 是否继续执行
     * @throws Exception 写出响应异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RiskProtected riskProtected = handlerMethod.getMethodAnnotation(RiskProtected.class);
        if (riskProtected == null) {
            return true;
        }

        RiskCheckContext context = new RiskCheckContext();
        context.setSceneCode(riskProtected.sceneCode());
        context.setUserId(Convert.toLong(request.getHeader("X-User-Id")));
        context.setMobile(request.getHeader("X-Mobile"));
        context.setDeviceId(request.getHeader("X-Device-Id"));
        context.setRequestIp(IpUtil.getClientIp(request));
        context.init();

        RiskCheckResult riskResult = riskControlService.check(context);
        if (riskResult.isPass()) {
            return true;
        }

        log.warn("统一风控拦截，requestId={}，uri={}，message={}",
                context.getRequestId(), request.getRequestURI(), riskResult.getMessage());

        this.writeRejectResponse(response, riskResult.getMessage());
        return false;
    }

    /**
     * 写出风控拒绝响应
     *
     * @param response HTTP 响应
     * @param message  提示信息
     * @throws Exception 写出异常
     */
    private void writeRejectResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new HashMap<>();
        body.put("code", 403);
        body.put("message", StrUtil.blankToDefault(message, "请求被风控拦截"));

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
```

注册拦截器。

文件位置：`src/main/java/io/github/atengk/risk/config/WebMvcConfig.java`

```java
package io.github.atengk.risk.config;

import io.github.atengk.risk.interceptor.RiskControlInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Web MVC 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RiskControlInterceptor riskControlInterceptor;

    /**
     * 注册拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(riskControlInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}
```

使用注解保护接口。

文件位置：`src/main/java/io/github/atengk/risk/controller/CouponController.java`

```java
package io.github.atengk.risk.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.risk.annotation.RiskProtected;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 优惠券接口示例
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@RestController
@RequestMapping("/coupons")
public class CouponController {

    /**
     * 领取优惠券
     *
     * @param couponId 优惠券ID
     * @return 领取结果
     */
    @RiskProtected(sceneCode = "COUPON_RECEIVE")
    @PostMapping("/{couponId}/receive")
    public Map<String, Object> receive(@PathVariable Long couponId) {
        log.info("领取优惠券成功，couponId={}", couponId);
        return MapUtil.builder(new java.util.HashMap<String, Object>())
                .put("code", 200)
                .put("message", "领取成功")
                .put("couponId", couponId)
                .build();
    }
}
```

统一拦截器测试请求：

```bash
curl -X POST 'http://localhost:8080/coupons/30001/receive' \
  -H 'X-User-Id: 10001' \
  -H 'X-Mobile: 13800000000' \
  -H 'X-Device-Id: device_abc_001' \
  -H 'X-Real-IP: 192.168.10.20'
```

这种方式适合通用接口风控，但如果需要从请求体中提取复杂参数，仍然建议在业务接口中手动构建 `RiskCheckContext`。

## 接口测试

接口测试主要验证三个核心行为：白名单优先放行、黑名单拦截、普通规则命中拦截。测试前需要先准备基础数据。

### 初始化测试数据

执行下面 SQL 初始化测试数据。

```sql
-- 清理旧数据
DELETE FROM risk_rule;
DELETE FROM risk_list;
DELETE FROM risk_hit_record;

-- 全局 IP 白名单：该 IP 所有场景直接放行
INSERT INTO risk_list
(list_type, target_type, target_value, scene_code, enabled, expire_time, reason)
VALUES
('WHITE', 'IP', '127.0.0.1', NULL, 1, NULL, '本地测试 IP 白名单');

-- 登录用户黑名单：用户 10001 在登录场景被拦截
INSERT INTO risk_list
(list_type, target_type, target_value, scene_code, enabled, expire_time, reason)
VALUES
('BLACK', 'USER', '10001', 'LOGIN', 1, NULL, '账号存在异常登录行为');

-- 下单设备黑名单：指定设备在下单场景被拦截
INSERT INTO risk_list
(list_type, target_type, target_value, scene_code, enabled, expire_time, reason)
VALUES
('BLACK', 'DEVICE', 'device_black_001', 'ORDER_CREATE', 1, NULL, '设备疑似批量刷单');

-- 登录 IP 风控规则：指定 IP 登录时被拒绝
INSERT INTO risk_rule
(rule_name, scene_code, rule_type, match_value, action, enabled, remark)
VALUES
('禁止异常IP登录', 'LOGIN', 'IP', '192.168.10.20', 'REJECT', 1, '异常登录来源');

-- 下单用户风控规则：用户 20002 下单时被拒绝
INSERT INTO risk_rule
(rule_name, scene_code, rule_type, match_value, action, enabled, remark)
VALUES
('禁止高风险用户下单', 'ORDER_CREATE', 'USER', '20002', 'REJECT', 1, '疑似刷单用户');
```

如果前面已经访问过接口，Redis 中可能存在旧缓存。测试前可以清理风控缓存：

```bash
redis-cli keys 'risk:*' | xargs redis-cli del
```

这条命令会删除 `risk:` 前缀的 Redis Key，只建议在本地测试环境使用。生产环境应通过后台变更逻辑精准删除对应 Key。

### 白名单放行测试

白名单优先级最高。即使同一个请求命中后续黑名单或规则，只要先命中白名单，就直接放行。

测试请求：

```bash
curl -X POST 'http://localhost:8080/auth/login' \
  -H 'Content-Type: application/json' \
  -H 'X-Real-IP: 127.0.0.1' \
  -d '{
    "mobile": "13800000000",
    "verifyCode": "123456",
    "deviceId": "device_abc_001"
  }'
```

预期响应：

```json
{
  "code": 200,
  "message": "登录成功",
  "token": "mock-token-13800000000"
}
```

查看命中记录：

```sql
SELECT request_id, scene_code, hit_type, hit_value, result, reason, create_time
FROM risk_hit_record
ORDER BY id DESC
LIMIT 5;
```

预期可以看到类似记录：

```text
scene_code = LOGIN
hit_type = WHITE_LIST
hit_value = 127.0.0.1
result = PASS
```

### 黑名单拦截测试

黑名单命中后会直接拒绝，不再继续执行普通规则。

测试登录用户黑名单：

```bash
curl -X POST 'http://localhost:8080/auth/login' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 10001' \
  -H 'X-Real-IP: 192.168.10.30' \
  -d '{
    "mobile": "13800000000",
    "verifyCode": "123456",
    "deviceId": "device_normal_001"
  }'
```

上面的登录接口示例没有从请求头设置 `userId`，所以如果要测试用户黑名单，可以临时在登录接口中根据手机号查询出用户 ID 后再设置到 `context`。如果只测试当前代码，可以改用手机号黑名单，或者测试下单场景的用户、设备黑名单。

测试下单设备黑名单：

```bash
curl -X POST 'http://localhost:8080/orders' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 30003' \
  -H 'X-Real-IP: 192.168.10.30' \
  -d '{
    "productId": 90001,
    "quantity": 1,
    "amount": 199.00,
    "deviceId": "device_black_001"
  }'
```

预期响应：

```json
{
  "code": 403,
  "message": "设备疑似批量刷单"
}
```

查看命中记录：

```sql
SELECT request_id, scene_code, user_id, device_id, hit_type, hit_value, result, reason
FROM risk_hit_record
ORDER BY id DESC
LIMIT 5;
```

预期可以看到：

```text
scene_code = ORDER_CREATE
hit_type = BLACK_LIST
hit_value = device_black_001
result = REJECT
```

### 规则命中测试

当请求未命中白名单、黑名单时，会继续执行普通风控规则。

测试登录 IP 规则命中：

```bash
curl -X POST 'http://localhost:8080/auth/login' \
  -H 'Content-Type: application/json' \
  -H 'X-Real-IP: 192.168.10.20' \
  -d '{
    "mobile": "13900000000",
    "verifyCode": "123456",
    "deviceId": "device_normal_002"
  }'
```

预期响应：

```json
{
  "code": 403,
  "message": "异常登录来源"
}
```

测试下单用户规则命中：

```bash
curl -X POST 'http://localhost:8080/orders' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 20002' \
  -H 'X-Real-IP: 192.168.10.30' \
  -d '{
    "productId": 90001,
    "quantity": 1,
    "amount": 199.00,
    "deviceId": "device_normal_003"
  }'
```

预期响应：

```json
{
  "code": 403,
  "message": "疑似刷单用户"
}
```

查看规则命中记录：

```sql
SELECT request_id, scene_code, user_id, request_ip, hit_type, hit_value, hit_source_id, result, reason
FROM risk_hit_record
ORDER BY id DESC
LIMIT 10;
```

预期可以看到：

```text
hit_type = RULE
result = REJECT
```

## 实现总结

本案例实现的是一个轻量级、可直接落地的风控规则与黑白名单模块。它不依赖复杂规则引擎，而是通过数据库配置、Redis 缓存、Java 策略匹配器和统一风控入口完成核心能力。

### 核心流程回顾

当前模块的核心流程如下：

```text
业务接口
  ↓
构建 RiskCheckContext
  ↓
调用 RiskControlService.check
  ↓
检查白名单
  ↓
检查黑名单
  ↓
检查启用状态的风控规则
  ↓
返回 PASS 或 REJECT
  ↓
保存命中记录
```

最终形成的能力包括：

| 能力           | 实现方式                               |
| -------------- | -------------------------------------- |
| 白名单优先放行 | `RiskListService#checkWhiteList`       |
| 黑名单直接拦截 | `RiskListService#checkBlackList`       |
| 普通规则判断   | `RiskRuleService#checkRule`            |
| 多维度匹配     | `RiskRuleMatcher` 策略接口             |
| Redis 缓存     | 名单精确 Key、规则按场景缓存           |
| 命中记录       | `risk_hit_record` 表                   |
| 业务接入       | 手动调用或注解拦截器                   |
| 后台管理       | 规则新增、规则启停、名单新增、名单删除 |

判断优先级固定为：

```text
白名单 > 黑名单 > 风控规则 > 默认放行
```

这个优先级适合多数业务系统。白名单用于测试账号、内部 IP、运营豁免对象；黑名单用于明确风险对象；普通规则用于可配置的业务场景拦截。

### 可扩展优化方向

当前实现已经覆盖核心功能，但如果要在生产项目中继续增强，可以从以下方向扩展。

| 方向         | 优化方案                                                  |
| ------------ | --------------------------------------------------------- |
| 规则表达式   | 引入 Aviator、QLExpress，支持金额、次数、时间窗口等表达式 |
| 规则编排     | 引入 LiteFlow，把多条规则编排成可配置流程                 |
| 限流风控     | 使用 Redis Lua 实现用户、IP、设备维度限流                 |
| 批量缓存     | 系统启动时预热高频黑白名单，减少首次请求数据库查询        |
| 缓存一致性   | 规则变更后通过 MQ 广播删除多实例缓存                      |
| 命中记录归档 | `risk_hit_record` 按月分表或定时归档                      |
| 管理后台     | 增加规则列表、命中记录查询、批量导入黑名单                |
| 操作审计     | 记录规则和名单的新增、修改、删除操作人                    |
| 人机验证     | 将 `REJECT` 扩展为 `VERIFY`，命中后要求验证码或二次确认   |
| 风险评分     | 不直接拒绝，而是按规则累计分数，超过阈值后拦截            |

如果只做第一版业务落地，建议优先补充这三个点：

1. 规则和名单的分页查询接口，便于后台页面管理。
2. 命中记录查询接口，便于客服和运营排查。
3. 缓存删除逻辑封装，避免多处手写 Redis Key。

到这里，`风控规则与黑白名单` 的核心实现已经闭环：有表结构、有实体、有缓存、有规则匹配、有风控决策、有管理接口，也有登录和下单两个真实业务接入示例。