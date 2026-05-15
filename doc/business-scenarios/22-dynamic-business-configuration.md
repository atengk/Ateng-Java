# 动态配置 / 业务规则配置

本文基于 Java 后端经典业务场景中的“动态配置 / 业务规则配置”场景展开，核心关注配置发布、实时生效、缓存刷新、版本回滚、变更审计和多租户隔离等能力。

## 场景说明

动态配置 / 业务规则配置的目标不是替代 Nacos、Apollo 这类配置中心，而是把“会被业务人员频繁调整的规则”从代码中抽离出来，通过后台维护、发布、缓存和热刷新，让业务逻辑可以在不重新发版的情况下调整执行策略。

本案例以“订单额度规则配置”为例实现核心功能：后台维护不同租户、不同业务场景下的规则配置，发布后写入 Redis 缓存，业务接口读取最新配置并执行校验逻辑。后续代码会重点实现数据库配置、版本发布、Redis 缓存、配置读取、规则执行和审计留痕。

### 业务背景

在实际后端项目中，很多业务规则并不适合硬编码在 Java 代码里。例如：

```text
普通用户单笔订单最大金额不能超过 5000
VIP 用户单笔订单最大金额不能超过 20000
某个租户可以临时关闭下单入口
某个活动期间新用户首单最低金额必须大于 99
某个审批场景需要根据金额动态选择审批链路
```

如果这些规则全部写死在代码中，每次调整都需要经历开发、测试、打包、发布流程，成本高且响应慢。对于营销、风控、审批、计费、额度、开关类规则，这种方式很难满足业务快速变化的要求。

动态配置模块要解决的问题就是：

```text
业务人员在后台维护规则
-> 开发人员定义规则结构和执行方式
-> 配置发布后生成正式版本
-> 服务端实时读取最新规则
-> Redis 缓存提升读取性能
-> 配置变更后主动刷新缓存
-> 业务接口基于配置执行判断
```

本案例不会实现复杂规则引擎，而是采用更适合中后台项目落地的方案：

```text
MySQL 存配置元数据和版本
Redis 存当前生效配置
Spring Boot 业务代码读取配置
Hutool JSONUtil 解析规则 JSON
AOP 记录配置变更审计
```

这种方案足够覆盖大多数中后台系统里的“业务参数配置”“业务开关配置”“额度规则配置”“营销门槛配置”等场景。

### 实现目标

本案例最终要实现一个轻量但完整的动态配置模块，重点覆盖核心链路，而不是堆叠复杂功能。

核心目标如下：

| 目标     | 说明                                       |
| -------- | ------------------------------------------ |
| 配置维护 | 支持新增、修改业务配置草稿                 |
| 配置发布 | 将草稿配置发布为正式生效版本               |
| 配置缓存 | 发布后将配置写入 Redis，业务读取优先走缓存 |
| 配置读取 | 业务代码通过配置 Key 获取当前生效配置      |
| 规则执行 | 根据配置中的 JSON 规则执行订单金额校验     |
| 配置回滚 | 支持回滚到历史已发布版本                   |
| 变更审计 | 记录配置新增、修改、发布、回滚等关键操作   |
| 租户隔离 | 同一个配置 Key 支持不同租户配置不同规则    |

本案例使用一个具体业务规则作为贯穿示例：

```json
{
  "enabled": true,
  "maxOrderAmount": 5000,
  "minOrderAmount": 10,
  "allowVipExceeded": true
}
```

该配置表示：

```text
enabled：是否启用订单金额规则
maxOrderAmount：普通用户单笔最大订单金额
minOrderAmount：单笔最小订单金额
allowVipExceeded：VIP 用户是否允许使用更高额度
```

业务接口创建订单时，会读取当前租户下的订单额度配置，并执行如下判断：

```text
配置未启用 -> 直接放行
订单金额小于最低金额 -> 拦截
普通用户订单金额超过最大金额 -> 拦截
VIP 用户超过普通额度但允许超额 -> 放行
```

### 核心功能边界

本案例只实现动态配置模块的核心闭环，重点放在后端可落地实现上。

包含的功能边界：

```text
配置基础信息管理
配置草稿保存
配置版本发布
当前生效配置查询
Redis 缓存读取
Redis 缓存刷新
历史版本回滚
配置变更审计
订单金额规则执行示例
```

暂不展开的高级能力：

```text
复杂可视化规则编排
Drools / Aviator 表达式规则引擎
多环境配置审批流
灰度人群圈选
配置发布审批
跨服务配置广播
大规模配置推送
配置依赖关系分析
```

后续代码会按照“最小可用闭环”来实现，整体链路如下：

```text
后台保存配置
-> 写入 config_item 表
-> 发布配置
-> 生成 config_version 版本记录
-> 更新当前生效版本
-> 写入 Redis 缓存
-> 业务服务读取 Redis 配置
-> 解析 JSON 规则
-> 执行业务校验
-> 记录配置审计日志
```

这个边界适合单体 Spring Boot 项目，也可以平滑扩展到微服务架构。后续如果接入 Nacos、Apollo 或 RabbitMQ，只需要替换“配置刷新通知”部分，不影响配置表结构和业务读取方式。

## 技术选型

本案例沿用原 README 中“动态配置 / 业务规则配置”推荐的核心技术栈：Spring Boot、Nacos Config、Redis、MyBatis-Plus、AOP、RabbitMQ、Hutool JSONUtil 等，用于实现配置发布、缓存、实时刷新、审计和规则执行能力。

### 基础技术栈

本案例采用 Spring Boot 单体项目结构实现，后续也可以平滑拆分成“配置管理服务”和“业务执行服务”。

| 技术            | 用途                                          |
| --------------- | --------------------------------------------- |
| Spring Boot 3   | 后端基础框架                                  |
| Java 17         | Spring Boot 3 推荐运行版本                    |
| MyBatis-Plus    | 配置表、版本表、审计表 CRUD                   |
| MySQL 8         | 持久化配置、版本、发布记录、审计日志          |
| Redis           | 缓存当前生效配置                              |
| Nacos Config    | 管理基础配置，例如 Redis、MySQL、业务默认参数 |
| Hutool JSONUtil | 解析业务规则 JSON                             |
| AOP             | 记录配置变更审计                              |
| Lombok          | 简化实体、DTO、VO 代码                        |
| Sa-Token        | 获取当前登录用户，后续用于操作人记录          |
| RabbitMQ        | 可选，用于多实例配置刷新广播                  |

本案例核心链路如下：

```text
后台保存配置草稿
-> MySQL 保存配置元数据
-> 发布配置
-> 生成版本快照
-> 更新当前生效版本
-> 写入 Redis 缓存
-> 业务接口读取配置
-> Hutool JSONUtil 解析规则
-> 执行业务判断
-> AOP 记录操作审计
```

### 配置存储方案

配置存储采用“MySQL 主存储 + Redis 热缓存”的方式。

MySQL 负责保存完整数据，包括配置草稿、版本快照、发布记录和审计日志。Redis 只保存当前生效配置，不作为最终数据源。

配置分为两类数据：

| 数据类型 | 存储位置                                  | 说明                     |
| -------- | ----------------------------------------- | ------------------------ |
| 配置草稿 | `biz_config_item.draft_value`             | 后台编辑但未发布的内容   |
| 生效配置 | `biz_config_version.config_value` + Redis | 发布后业务实际读取的内容 |

配置 Key 采用统一命名规范：

```text
业务域:场景:规则名称
```

示例：

```text
order:create:amount-rule
marketing:coupon:receive-rule
risk:login:black-rule
approval:expense:route-rule
```

多租户场景下，同一个配置 Key 可以在不同租户下存在不同规则：

```text
tenant_id = 1001, config_key = order:create:amount-rule
tenant_id = 1002, config_key = order:create:amount-rule
tenant_id = 0,    config_key = order:create:amount-rule
```

其中 `tenant_id = 0` 表示全局默认配置。当指定租户没有独立配置时，可以回退读取全局默认配置。

### 缓存与实时刷新方案

Redis 只缓存当前生效版本，缓存内容来自最近一次发布或回滚后的配置值。

Redis Key 设计如下：

```text
biz:config:{tenantId}:{configKey}
```

示例：

```text
biz:config:1001:order:create:amount-rule
biz:config:0:order:create:amount-rule
```

Redis Value 存储完整 JSON：

```json
{
  "versionNo": 3,
  "configKey": "order:create:amount-rule",
  "configValue": {
    "enabled": true,
    "maxOrderAmount": 5000,
    "minOrderAmount": 10,
    "allowVipExceeded": true
  }
}
```

缓存刷新策略采用“发布时主动刷新 + 查询时兜底加载”：

```text
发布配置
-> 更新 MySQL 当前版本号
-> 写入 Redis 当前配置
-> 返回发布成功

业务查询配置
-> 优先读 Redis
-> Redis 不存在则查 MySQL 当前版本
-> 回写 Redis
-> 返回配置
```

如果项目是多实例部署，可以增加 RabbitMQ 广播刷新消息：

```text
实例 A 发布配置
-> 写 MySQL
-> 写 Redis
-> 发送 config.refresh 消息
-> 实例 B / C 收到消息后清理本地缓存
```

本案例核心实现先以 Redis 刷新为主，RabbitMQ 广播作为后续扩展点。

### 规则表达式方案

本案例不直接引入 Drools 或 Aviator，而是采用 JSON 规则配置 + Java 规则执行器的方式。

适合原因：

```text
规则结构清晰
可读性强
方便后台表单化维护
调试成本低
适合中后台业务配置
不需要额外学习复杂规则语法
```

订单金额规则示例：

```json
{
  "enabled": true,
  "maxOrderAmount": 5000,
  "vipMaxOrderAmount": 20000,
  "minOrderAmount": 10,
  "allowVipExceeded": true
}
```

业务执行逻辑：

```text
enabled = false
-> 不启用规则，直接放行

amount < minOrderAmount
-> 拦截，订单金额不能低于最小金额

普通用户 amount > maxOrderAmount
-> 拦截，超过普通用户额度

VIP 用户 amount <= vipMaxOrderAmount
-> 放行

VIP 用户 amount > vipMaxOrderAmount
-> 拦截，超过 VIP 用户额度
```

这种方案的边界是：规则结构由开发人员定义，业务人员只维护规则值。如果后续需要支持复杂表达式，例如：

```text
用户等级 = VIP 且 城市 = 北京 且 下单次数 > 3
```

可以再引入 Aviator 或 Drools，本案例先不展开。

## 表结构设计

本案例共设计 4 张核心表：配置主表、配置版本表、配置发布记录表、配置变更审计表。

### 业务配置表

`biz_config_item` 用于保存配置基础信息和草稿内容。它不直接代表当前生效配置，当前生效版本由 `current_version_no` 指向。

`biz_config_item` 建表语句如下：

```sql
CREATE TABLE biz_config_item (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID，0表示全局默认配置',
    config_key VARCHAR(128) NOT NULL COMMENT '配置Key，例如 order:create:amount-rule',
    config_name VARCHAR(128) NOT NULL COMMENT '配置名称',
    config_type VARCHAR(32) NOT NULL COMMENT '配置类型：SWITCH、JSON_RULE、TEXT',
    draft_value JSON NOT NULL COMMENT '草稿配置值，后台编辑后先保存到这里',
    current_version_no INT NOT NULL DEFAULT 0 COMMENT '当前生效版本号，0表示未发布',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_name VARCHAR(64) DEFAULT NULL COMMENT '创建人名称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    update_name VARCHAR(64) DEFAULT NULL COMMENT '更新人名称',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_config_key (tenant_id, config_key),
    KEY idx_config_key (config_key),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务配置表';
```

关键设计点：

| 字段                 | 说明                                     |
| -------------------- | ---------------------------------------- |
| `tenant_id`          | 支持多租户配置隔离                       |
| `config_key`         | 业务读取配置的唯一标识                   |
| `draft_value`        | 保存后台编辑内容，未发布前不影响线上业务 |
| `current_version_no` | 指向当前生效版本                         |
| `version`            | MyBatis-Plus 乐观锁字段，避免并发覆盖    |
| `deleted`            | 逻辑删除字段                             |

### 配置版本表

`biz_config_version` 用于保存每次发布后的配置快照。业务回滚时，就是把当前版本号切换回某个历史版本。

`biz_config_version` 建表语句如下：

```sql
CREATE TABLE biz_config_version (
    id BIGINT NOT NULL COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '配置主表ID',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    config_key VARCHAR(128) NOT NULL COMMENT '配置Key',
    version_no INT NOT NULL COMMENT '版本号，从1开始递增',
    config_value JSON NOT NULL COMMENT '该版本的完整配置值',
    config_md5 VARCHAR(64) NOT NULL COMMENT '配置内容MD5，用于判断内容是否变化',
    publish_remark VARCHAR(500) DEFAULT NULL COMMENT '发布备注',
    publish_by BIGINT DEFAULT NULL COMMENT '发布人ID',
    publish_name VARCHAR(64) DEFAULT NULL COMMENT '发布人名称',
    publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    rollback_from_version_no INT DEFAULT NULL COMMENT '如果由回滚产生，记录来源版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_version (config_id, version_no),
    KEY idx_tenant_config_key (tenant_id, config_key),
    KEY idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务配置版本表';
```

关键设计点：

| 字段                       | 说明                     |
| -------------------------- | ------------------------ |
| `config_id`                | 关联配置主表             |
| `version_no`               | 配置版本号               |
| `config_value`             | 当前版本的完整配置内容   |
| `config_md5`               | 用于判断重复发布         |
| `rollback_from_version_no` | 标记该版本是否来源于回滚 |

发布配置时，不直接覆盖历史版本，而是新增版本快照：

```text
当前版本 1
-> 编辑草稿
-> 发布
-> 新增版本 2
-> biz_config_item.current_version_no = 2
```

### 配置发布记录表

`biz_config_release_record` 用于记录配置发布和回滚动作。它关注“发布行为”，而不是配置内容本身。

`biz_config_release_record` 建表语句如下：

```sql
CREATE TABLE biz_config_release_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '配置主表ID',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    config_key VARCHAR(128) NOT NULL COMMENT '配置Key',
    action_type VARCHAR(32) NOT NULL COMMENT '操作类型：PUBLISH发布、ROLLBACK回滚',
    before_version_no INT NOT NULL DEFAULT 0 COMMENT '操作前版本号',
    after_version_no INT NOT NULL COMMENT '操作后版本号',
    release_remark VARCHAR(500) DEFAULT NULL COMMENT '发布或回滚备注',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_config_id (config_id),
    KEY idx_tenant_config_key (tenant_id, config_key),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务配置发布记录表';
```

发布记录示例：

| action_type | before_version_no | after_version_no | 说明           |
| ----------- | ----------------- | ---------------- | -------------- |
| PUBLISH     | 0                 | 1                | 首次发布       |
| PUBLISH     | 1                 | 2                | 修改规则后发布 |
| ROLLBACK    | 2                 | 1                | 回滚到版本 1   |

### 配置变更审计表

`biz_config_audit_log` 用于记录配置新增、修改、发布、回滚、删除等关键操作，便于后续追责和排查问题。

`biz_config_audit_log` 建表语句如下：

```sql
CREATE TABLE biz_config_audit_log (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    config_id BIGINT DEFAULT NULL COMMENT '配置主表ID',
    config_key VARCHAR(128) DEFAULT NULL COMMENT '配置Key',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型：CREATE、UPDATE、PUBLISH、ROLLBACK、DELETE',
    before_value JSON DEFAULT NULL COMMENT '变更前内容',
    after_value JSON DEFAULT NULL COMMENT '变更后内容',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    request_ip VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
    trace_id VARCHAR(128) DEFAULT NULL COMMENT '链路追踪ID',
    operate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功：1成功，0失败',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    PRIMARY KEY (id),
    KEY idx_tenant_config_key (tenant_id, config_key),
    KEY idx_operator_id (operator_id),
    KEY idx_operate_time (operate_time),
    KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务配置变更审计表';
```

审计表不参与业务判断，只用于追踪：

```text
谁在什么时间修改了哪个配置
修改前是什么
修改后是什么
是否发布成功
失败原因是什么
对应 TraceId 是什么
```

## 项目依赖与配置

本节先给出后续代码需要的 Maven 依赖和基础配置。后续 Controller、Service、Mapper、AOP、规则执行器都会基于这些配置展开。

### Maven 依赖

以下依赖放在项目根目录 `pom.xml` 中，主要包含 Web、MyBatis-Plus、MySQL、Redis、Nacos、Hutool、Lombok 和 Sa-Token。

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot AOP：用于配置变更审计切面 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- MyBatis-Plus：简化配置表、版本表、审计表 CRUD -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 8 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Redis：缓存当前生效配置 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Nacos Config：统一管理应用基础配置 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>

    <!-- Nacos Discovery：可选，后续微服务注册发现使用 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- Hutool：JSON、字符串、集合、摘要等工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>

    <!-- Sa-Token：获取当前登录用户，记录配置操作人 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>1.39.0</version>
    </dependency>

    <!-- Lombok：简化实体、DTO、VO 编写 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Validation：接口参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Spring Boot Test：单元测试和接口测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Cloud Alibaba，需要在 `dependencyManagement` 中统一管理版本。

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Cloud 版本管理 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Spring Cloud Alibaba 版本管理 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2023.0.3.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Nacos 配置

Nacos 主要用于管理应用基础配置，不直接存业务规则。业务规则仍然存 MySQL，并同步到 Redis。

Nacos 中创建配置：

```text
Data ID: dynamic-config-demo.yml
Group: DEFAULT_GROUP
Format: YAML
```

配置内容如下：

```yaml
server:
  port: 8080

spring:
  application:
    # 应用名称，用于 Nacos 注册发现和日志识别
    name: dynamic-config-demo

  datasource:
    # MySQL 连接地址，根据实际环境修改
    url: jdbc:mysql://127.0.0.1:3306/dynamic_config_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      # Redis 地址，根据实际环境修改
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
          # 最大等待时间
          max-wait: 3s

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: io.github.atengk.config.entity
  global-config:
    db-config:
      # MyBatis-Plus 雪花算法主键
      id-type: assign_id
      # 逻辑删除字段
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    # 打印 SQL，生产环境建议关闭或改为日志级别控制
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # 下划线转驼峰
    map-underscore-to-camel-case: true

biz:
  config:
    # Redis Key 前缀，后续代码中统一使用
    redis-prefix: "biz:config"
    # 默认租户ID，0表示全局配置
    default-tenant-id: 0
    # 配置缓存过期时间，单位秒；发布时会主动刷新，此处主要用于兜底
    cache-ttl-seconds: 86400
```

本地 `bootstrap.yml` 用于连接 Nacos。

文件位置：`src/main/resources/bootstrap.yml`

```yaml
spring:
  application:
    name: dynamic-config-demo

  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      username: nacos
      password: nacos
      config:
        # Nacos 配置文件扩展名
        file-extension: yml
        # 配置分组
        group: DEFAULT_GROUP
        # 启用动态刷新
        refresh-enabled: true
      discovery:
        # 注册发现分组
        group: DEFAULT_GROUP
```

启动时会读取：

```text
dynamic-config-demo.yml
```

### Redis 配置

Redis 在本案例中只保存当前生效配置，Key 使用统一前缀，避免和其他业务缓存冲突。

Redis Key 规范：

```text
{redis-prefix}:{tenantId}:{configKey}
```

示例：

```text
biz:config:1001:order:create:amount-rule
biz:config:0:order:create:amount-rule
```

Redis Value 示例：

```json
{
  "tenantId": 1001,
  "configKey": "order:create:amount-rule",
  "versionNo": 2,
  "configValue": {
    "enabled": true,
    "maxOrderAmount": 5000,
    "vipMaxOrderAmount": 20000,
    "minOrderAmount": 10,
    "allowVipExceeded": true
  }
}
```

后续代码中 Redis 使用 `StringRedisTemplate` 操作，配置值统一序列化为 JSON 字符串：

```text
写入缓存：JSONUtil.toJsonStr(value)
读取缓存：JSONUtil.parseObj(json)
```

建议不要把所有配置塞进一个 Redis Hash 中。单 Key 设计更适合按租户、按配置独立刷新：

```text
发布 order:create:amount-rule
-> 只刷新 biz:config:1001:order:create:amount-rule
```

而不是刷新整个租户的全部配置。

### MyBatis-Plus 配置

MyBatis-Plus 需要启用分页插件、乐观锁插件和逻辑删除能力。分页用于配置列表查询，乐观锁用于避免多人同时编辑配置时发生覆盖。

文件位置：`src/main/java/io/github/atengk/config/config/MybatisPlusConfig.java`

```java
package io.github.atengk.config.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 拦截器
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 乐观锁插件：配合实体类中的 @Version 字段使用，防止并发覆盖配置
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页插件：用于配置列表、版本列表、审计日志分页查询
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
```

对应实体类后续需要包含以下字段：

```text
version -> 使用 @Version
deleted -> 使用 @TableLogic
create_time / update_time -> 可以使用自动填充
```

本案例后续实体类会按照 MyBatis-Plus 标准方式编写：

```text
Entity -> Mapper -> Service -> ServiceImpl -> Controller
```

其中 JSON 字段在实体中先使用 `String` 接收，业务层再使用 Hutool `JSONUtil` 转换，避免不同数据库 JSON 类型映射带来的兼容问题。

## 核心代码实现

本节实现动态配置模块的核心后端代码，围绕“配置草稿保存、配置发布、Redis 缓存刷新、配置读取、规则执行、审计留痕”展开，技术栈与前文 README 中的动态配置场景保持一致。

本节代码默认基础包名为：

```text
io.github.atengk.config
```

建议目录结构如下：

```text
src/main/java/io/github/atengk/config
├── annotation
│   └── ConfigAudit.java
├── aspect
│   └── ConfigAuditAspect.java
├── cache
│   └── BizConfigCache.java
├── config
│   └── BizConfigProperties.java
├── constant
│   └── BizConfigConstant.java
├── dto
│   ├── OrderAmountCheckDTO.java
│   ├── PublishConfigDTO.java
│   ├── RollbackConfigDTO.java
│   └── SaveConfigDTO.java
├── entity
│   ├── BizConfigAuditLog.java
│   ├── BizConfigItem.java
│   ├── BizConfigReleaseRecord.java
│   └── BizConfigVersion.java
├── event
│   ├── ConfigRefreshEvent.java
│   └── ConfigRefreshListener.java
├── executor
│   └── OrderAmountRuleExecutor.java
├── mapper
│   ├── BizConfigAuditLogMapper.java
│   ├── BizConfigItemMapper.java
│   ├── BizConfigReleaseRecordMapper.java
│   └── BizConfigVersionMapper.java
├── service
│   └── BizConfigService.java
├── service/impl
│   └── BizConfigServiceImpl.java
├── util
│   └── LoginUserUtil.java
└── vo
    ├── BizConfigVO.java
    ├── ConfigValueVO.java
    └── OrderAmountCheckResultVO.java
```

### 配置实体类

实体类对应前文 4 张表。JSON 字段这里统一使用 `String` 接收，业务层使用 Hutool `JSONUtil` 解析，避免 MySQL JSON 类型在不同驱动、不同数据库之间出现类型映射差异。

`BizConfigItem` 表示业务配置主表，保存配置基础信息、草稿内容和当前生效版本号。

文件位置：`src/main/java/io/github/atengk/config/entity/BizConfigItem.java`

```java
package io.github.atengk.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务配置主表
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("biz_config_item")
public class BizConfigItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String configKey;

    private String configName;

    private String configType;

    private String draftValue;

    private Integer currentVersionNo;

    private Integer status;

    private String remark;

    private Long createBy;

    private String createName;

    private LocalDateTime createTime;

    private Long updateBy;

    private String updateName;

    private LocalDateTime updateTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;
}
```

`BizConfigVersion` 保存每次发布后的配置快照，回滚时直接切换到历史版本。

文件位置：`src/main/java/io/github/atengk/config/entity/BizConfigVersion.java`

```java
package io.github.atengk.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务配置版本表
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("biz_config_version")
public class BizConfigVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long configId;

    private Long tenantId;

    private String configKey;

    private Integer versionNo;

    private String configValue;

    private String configMd5;

    private String publishRemark;

    private Long publishBy;

    private String publishName;

    private LocalDateTime publishTime;

    private Integer rollbackFromVersionNo;

    private LocalDateTime createTime;
}
```

`BizConfigReleaseRecord` 记录发布和回滚动作，用于追踪“哪次操作把版本从几切到了几”。

文件位置：`src/main/java/io/github/atengk/config/entity/BizConfigReleaseRecord.java`

```java
package io.github.atengk.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务配置发布记录表
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("biz_config_release_record")
public class BizConfigReleaseRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long configId;

    private Long tenantId;

    private String configKey;

    private String actionType;

    private Integer beforeVersionNo;

    private Integer afterVersionNo;

    private String releaseRemark;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime operateTime;
}
```

`BizConfigAuditLog` 记录配置变更审计日志，后续由 AOP 自动写入。

文件位置：`src/main/java/io/github/atengk/config/entity/BizConfigAuditLog.java`

```java
package io.github.atengk.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务配置审计日志表
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("biz_config_audit_log")
public class BizConfigAuditLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long configId;

    private String configKey;

    private String operationType;

    private String beforeValue;

    private String afterValue;

    private Long operatorId;

    private String operatorName;

    private String requestIp;

    private String traceId;

    private LocalDateTime operateTime;

    private Integer success;

    private String errorMessage;
}
```

### 配置 Mapper

Mapper 只需要继承 MyBatis-Plus 的 `BaseMapper`，复杂查询先放在 Service 中通过 `LambdaQueryWrapper` 完成。

以下 4 个 Mapper 分别对应配置主表、版本表、发布记录表和审计日志表。

文件位置：`src/main/java/io/github/atengk/config/mapper/BizConfigItemMapper.java`

```java
package io.github.atengk.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.config.entity.BizConfigItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务配置 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface BizConfigItemMapper extends BaseMapper<BizConfigItem> {
}
```

文件位置：`src/main/java/io/github/atengk/config/mapper/BizConfigVersionMapper.java`

```java
package io.github.atengk.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.config.entity.BizConfigVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务配置版本 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface BizConfigVersionMapper extends BaseMapper<BizConfigVersion> {
}
```

文件位置：`src/main/java/io/github/atengk/config/mapper/BizConfigReleaseRecordMapper.java`

```java
package io.github.atengk.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.config.entity.BizConfigReleaseRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务配置发布记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface BizConfigReleaseRecordMapper extends BaseMapper<BizConfigReleaseRecord> {
}
```

文件位置：`src/main/java/io/github/atengk/config/mapper/BizConfigAuditLogMapper.java`

```java
package io.github.atengk.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.config.entity.BizConfigAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务配置审计日志 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface BizConfigAuditLogMapper extends BaseMapper<BizConfigAuditLog> {
}
```

### 配置发布 DTO

DTO 用于接收后台管理端的请求参数。这里给出保存草稿、发布配置、回滚配置 3 个核心 DTO。

`SaveConfigDTO` 用于保存配置草稿，保存后不会立刻影响线上业务。

文件位置：`src/main/java/io/github/atengk/config/dto/SaveConfigDTO.java`

```java
package io.github.atengk.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存配置草稿请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class SaveConfigDTO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotBlank(message = "配置Key不能为空")
    private String configKey;

    @NotBlank(message = "配置名称不能为空")
    private String configName;

    @NotBlank(message = "配置类型不能为空")
    private String configType;

    @NotBlank(message = "配置内容不能为空")
    private String draftValue;

    private String remark;
}
```

`PublishConfigDTO` 用于将草稿发布为正式版本，并刷新 Redis 缓存。

文件位置：`src/main/java/io/github/atengk/config/dto/PublishConfigDTO.java`

```java
package io.github.atengk.config.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发布配置请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class PublishConfigDTO {

    @NotNull(message = "配置ID不能为空")
    private Long configId;

    private String publishRemark;
}
```

`RollbackConfigDTO` 用于将当前生效版本回滚到指定历史版本。

文件位置：`src/main/java/io/github/atengk/config/dto/RollbackConfigDTO.java`

```java
package io.github.atengk.config.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 回滚配置请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class RollbackConfigDTO {

    @NotNull(message = "配置ID不能为空")
    private Long configId;

    @NotNull(message = "目标版本号不能为空")
    private Integer targetVersionNo;

    private String rollbackRemark;
}
```

订单金额规则执行器使用该 DTO 接收业务侧校验参数。

文件位置：`src/main/java/io/github/atengk/config/dto/OrderAmountCheckDTO.java`

```java
package io.github.atengk.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单金额规则校验请求
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderAmountCheckDTO {

    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @NotNull(message = "订单金额不能为空")
    private BigDecimal orderAmount;

    @NotBlank(message = "用户类型不能为空")
    private String userType;
}
```

### 配置查询 VO

VO 用于返回配置详情、生效配置和规则执行结果。

`ConfigValueVO` 是业务读取配置时使用的统一返回对象，Redis 中也会存储该对象的 JSON 字符串。

文件位置：`src/main/java/io/github/atengk/config/vo/ConfigValueVO.java`

```java
package io.github.atengk.config.vo;

import lombok.Data;

/**
 * 当前生效配置值
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ConfigValueVO {

    private Long tenantId;

    private String configKey;

    private Integer versionNo;

    private String configValue;
}
```

`BizConfigVO` 用于后台配置详情查询。

文件位置：`src/main/java/io/github/atengk/config/vo/BizConfigVO.java`

```java
package io.github.atengk.config.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务配置查询结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class BizConfigVO {

    private Long id;

    private Long tenantId;

    private String configKey;

    private String configName;

    private String configType;

    private String draftValue;

    private Integer currentVersionNo;

    private Integer status;

    private String remark;

    private LocalDateTime updateTime;
}
```

`OrderAmountCheckResultVO` 用于返回订单金额规则校验结果。

文件位置：`src/main/java/io/github/atengk/config/vo/OrderAmountCheckResultVO.java`

```java
package io.github.atengk.config.vo;

import lombok.Data;

/**
 * 订单金额规则校验结果
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class OrderAmountCheckResultVO {

    private Boolean passed;

    private String message;

    private Integer versionNo;
}
```

### 配置服务接口

服务接口定义动态配置模块的核心能力：保存草稿、发布配置、回滚配置、读取当前生效配置。

文件位置：`src/main/java/io/github/atengk/config/service/BizConfigService.java`

```java
package io.github.atengk.config.service;

import io.github.atengk.config.dto.PublishConfigDTO;
import io.github.atengk.config.dto.RollbackConfigDTO;
import io.github.atengk.config.dto.SaveConfigDTO;
import io.github.atengk.config.vo.BizConfigVO;
import io.github.atengk.config.vo.ConfigValueVO;

/**
 * 业务配置服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface BizConfigService {

    /**
     * 保存配置草稿
     *
     * @param dto 保存配置草稿请求
     * @return 配置ID
     */
    Long saveDraft(SaveConfigDTO dto);

    /**
     * 发布配置
     *
     * @param dto 发布配置请求
     * @return 发布后的版本号
     */
    Integer publish(PublishConfigDTO dto);

    /**
     * 回滚配置
     *
     * @param dto 回滚配置请求
     */
    void rollback(RollbackConfigDTO dto);

    /**
     * 查询配置详情
     *
     * @param configId 配置ID
     * @return 配置详情
     */
    BizConfigVO getDetail(Long configId);

    /**
     * 获取当前生效配置
     *
     * @param tenantId 租户ID
     * @param configKey 配置Key
     * @return 当前生效配置
     */
    ConfigValueVO getActiveConfig(Long tenantId, String configKey);
}
```

### 配置服务实现

服务实现负责处理配置草稿、发布、回滚和读取。发布成功后不直接在事务中读缓存，而是发布 Spring 事件，事务提交后由监听器刷新 Redis。

文件位置：`src/main/java/io/github/atengk/config/constant/BizConfigConstant.java`

```java
package io.github.atengk.config.constant;

/**
 * 业务配置常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class BizConfigConstant {

    private BizConfigConstant() {
    }

    public static final long DEFAULT_TENANT_ID = 0L;

    public static final int ENABLED = 1;

    public static final int SUCCESS = 1;

    public static final int FAIL = 0;

    public static final String ACTION_PUBLISH = "PUBLISH";

    public static final String ACTION_ROLLBACK = "ROLLBACK";

    public static final String CONFIG_KEY_ORDER_AMOUNT_RULE = "order:create:amount-rule";

    public static final String USER_TYPE_VIP = "VIP";
}
```

`LoginUserUtil` 用于获取当前操作人信息。没有接入登录态时，默认返回系统用户，方便本地调试。

文件位置：`src/main/java/io/github/atengk/config/util/LoginUserUtil.java`

```java
package io.github.atengk.config.util;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录用户工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
public final class LoginUserUtil {

    private LoginUserUtil() {
    }

    /**
     * 获取当前用户ID
     *
     * @return 当前用户ID
     */
    public static Long getUserId() {
        try {
            if (StpUtil.isLogin()) {
                return Convert.toLong(StpUtil.getLoginId(), 0L);
            }
        } catch (Exception e) {
            log.debug("获取登录用户ID失败，使用系统用户");
        }
        return 0L;
    }

    /**
     * 获取当前用户名称
     *
     * @return 当前用户名称
     */
    public static String getUserName() {
        try {
            if (StpUtil.isLogin()) {
                Object nickname = StpUtil.getExtra("nickname");
                return ObjectUtil.defaultIfNull(nickname, "系统用户").toString();
            }
        } catch (Exception e) {
            log.debug("获取登录用户名称失败，使用系统用户");
        }
        return "系统用户";
    }
}
```

`BizConfigServiceImpl` 实现配置核心业务逻辑。

文件位置：`src/main/java/io/github/atengk/config/service/impl/BizConfigServiceImpl.java`

```java
package io.github.atengk.config.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.config.annotation.ConfigAudit;
import io.github.atengk.config.cache.BizConfigCache;
import io.github.atengk.config.constant.BizConfigConstant;
import io.github.atengk.config.dto.PublishConfigDTO;
import io.github.atengk.config.dto.RollbackConfigDTO;
import io.github.atengk.config.dto.SaveConfigDTO;
import io.github.atengk.config.entity.BizConfigItem;
import io.github.atengk.config.entity.BizConfigReleaseRecord;
import io.github.atengk.config.entity.BizConfigVersion;
import io.github.atengk.config.event.ConfigRefreshEvent;
import io.github.atengk.config.mapper.BizConfigItemMapper;
import io.github.atengk.config.mapper.BizConfigReleaseRecordMapper;
import io.github.atengk.config.mapper.BizConfigVersionMapper;
import io.github.atengk.config.service.BizConfigService;
import io.github.atengk.config.util.LoginUserUtil;
import io.github.atengk.config.vo.BizConfigVO;
import io.github.atengk.config.vo.ConfigValueVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 业务配置服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizConfigServiceImpl implements BizConfigService {

    private final BizConfigItemMapper bizConfigItemMapper;
    private final BizConfigVersionMapper bizConfigVersionMapper;
    private final BizConfigReleaseRecordMapper bizConfigReleaseRecordMapper;
    private final BizConfigCache bizConfigCache;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @ConfigAudit(operationType = "SAVE_DRAFT")
    public Long saveDraft(SaveConfigDTO dto) {
        if (!JSONUtil.isTypeJSON(dto.getDraftValue())) {
            throw new IllegalArgumentException("配置内容必须是合法 JSON");
        }

        BizConfigItem item = bizConfigItemMapper.selectOne(Wrappers.<BizConfigItem>lambdaQuery()
                .eq(BizConfigItem::getTenantId, dto.getTenantId())
                .eq(BizConfigItem::getConfigKey, dto.getConfigKey())
                .last("limit 1"));

        LocalDateTime now = LocalDateTime.now();
        Long userId = LoginUserUtil.getUserId();
        String userName = LoginUserUtil.getUserName();

        if (ObjectUtil.isNull(item)) {
            item = new BizConfigItem();
            item.setTenantId(dto.getTenantId());
            item.setConfigKey(dto.getConfigKey());
            item.setConfigName(dto.getConfigName());
            item.setConfigType(dto.getConfigType());
            item.setDraftValue(dto.getDraftValue());
            item.setCurrentVersionNo(0);
            item.setStatus(BizConfigConstant.ENABLED);
            item.setRemark(dto.getRemark());
            item.setCreateBy(userId);
            item.setCreateName(userName);
            item.setCreateTime(now);
            item.setUpdateBy(userId);
            item.setUpdateName(userName);
            item.setUpdateTime(now);
            item.setVersion(0);
            item.setDeleted(0);

            bizConfigItemMapper.insert(item);
            log.info("新增业务配置草稿成功，tenantId={}，configKey={}", dto.getTenantId(), dto.getConfigKey());
            return item.getId();
        }

        item.setConfigName(dto.getConfigName());
        item.setConfigType(dto.getConfigType());
        item.setDraftValue(dto.getDraftValue());
        item.setRemark(dto.getRemark());
        item.setUpdateBy(userId);
        item.setUpdateName(userName);
        item.setUpdateTime(now);

        int updated = bizConfigItemMapper.updateById(item);
        if (updated <= 0) {
            throw new IllegalStateException("配置已被其他用户修改，请刷新后重试");
        }

        log.info("更新业务配置草稿成功，configId={}，tenantId={}，configKey={}", item.getId(), item.getTenantId(), item.getConfigKey());
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @ConfigAudit(operationType = "PUBLISH")
    public Integer publish(PublishConfigDTO dto) {
        BizConfigItem item = getConfigItemOrThrow(dto.getConfigId());

        if (!JSONUtil.isTypeJSON(item.getDraftValue())) {
            throw new IllegalArgumentException("配置草稿不是合法 JSON，不能发布");
        }

        BizConfigVersion currentVersion = getVersion(item.getId(), item.getCurrentVersionNo());
        String configMd5 = SecureUtil.md5(item.getDraftValue());

        if (ObjectUtil.isNotNull(currentVersion) && configMd5.equals(currentVersion.getConfigMd5())) {
            throw new IllegalStateException("配置内容未变化，无需重复发布");
        }

        int beforeVersionNo = ObjectUtil.defaultIfNull(item.getCurrentVersionNo(), 0);
        int afterVersionNo = beforeVersionNo + 1;

        Long userId = LoginUserUtil.getUserId();
        String userName = LoginUserUtil.getUserName();
        LocalDateTime now = LocalDateTime.now();

        BizConfigVersion version = new BizConfigVersion();
        version.setConfigId(item.getId());
        version.setTenantId(item.getTenantId());
        version.setConfigKey(item.getConfigKey());
        version.setVersionNo(afterVersionNo);
        version.setConfigValue(item.getDraftValue());
        version.setConfigMd5(configMd5);
        version.setPublishRemark(dto.getPublishRemark());
        version.setPublishBy(userId);
        version.setPublishName(userName);
        version.setPublishTime(now);
        version.setCreateTime(now);
        bizConfigVersionMapper.insert(version);

        item.setCurrentVersionNo(afterVersionNo);
        item.setUpdateBy(userId);
        item.setUpdateName(userName);
        item.setUpdateTime(now);
        int updated = bizConfigItemMapper.updateById(item);
        if (updated <= 0) {
            throw new IllegalStateException("配置发布失败，配置已被其他用户修改");
        }

        saveReleaseRecord(item, BizConfigConstant.ACTION_PUBLISH, beforeVersionNo, afterVersionNo, dto.getPublishRemark());

        applicationEventPublisher.publishEvent(new ConfigRefreshEvent(this, item.getTenantId(), item.getConfigKey()));

        log.info("发布业务配置成功，configId={}，tenantId={}，configKey={}，versionNo={}",
                item.getId(), item.getTenantId(), item.getConfigKey(), afterVersionNo);
        return afterVersionNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @ConfigAudit(operationType = "ROLLBACK")
    public void rollback(RollbackConfigDTO dto) {
        BizConfigItem item = getConfigItemOrThrow(dto.getConfigId());
        BizConfigVersion targetVersion = getVersion(item.getId(), dto.getTargetVersionNo());

        if (ObjectUtil.isNull(targetVersion)) {
            throw new IllegalArgumentException("目标版本不存在，不能回滚");
        }

        int beforeVersionNo = ObjectUtil.defaultIfNull(item.getCurrentVersionNo(), 0);
        int afterVersionNo = dto.getTargetVersionNo();

        if (beforeVersionNo == afterVersionNo) {
            throw new IllegalStateException("当前已经是目标版本，无需回滚");
        }

        Long userId = LoginUserUtil.getUserId();
        String userName = LoginUserUtil.getUserName();
        LocalDateTime now = LocalDateTime.now();

        item.setCurrentVersionNo(afterVersionNo);
        item.setDraftValue(targetVersion.getConfigValue());
        item.setUpdateBy(userId);
        item.setUpdateName(userName);
        item.setUpdateTime(now);

        int updated = bizConfigItemMapper.updateById(item);
        if (updated <= 0) {
            throw new IllegalStateException("配置回滚失败，配置已被其他用户修改");
        }

        saveReleaseRecord(item, BizConfigConstant.ACTION_ROLLBACK, beforeVersionNo, afterVersionNo, dto.getRollbackRemark());

        applicationEventPublisher.publishEvent(new ConfigRefreshEvent(this, item.getTenantId(), item.getConfigKey()));

        log.info("回滚业务配置成功，configId={}，tenantId={}，configKey={}，fromVersion={}，toVersion={}",
                item.getId(), item.getTenantId(), item.getConfigKey(), beforeVersionNo, afterVersionNo);
    }

    @Override
    public BizConfigVO getDetail(Long configId) {
        BizConfigItem item = getConfigItemOrThrow(configId);
        return BeanUtil.copyProperties(item, BizConfigVO.class);
    }

    @Override
    public ConfigValueVO getActiveConfig(Long tenantId, String configKey) {
        ConfigValueVO tenantConfig = bizConfigCache.getActiveConfig(tenantId, configKey);
        if (ObjectUtil.isNotNull(tenantConfig)) {
            return tenantConfig;
        }

        if (!BizConfigConstant.DEFAULT_TENANT_ID == tenantId) {
            return bizConfigCache.getActiveConfig(BizConfigConstant.DEFAULT_TENANT_ID, configKey);
        }

        return null;
    }

    private BizConfigItem getConfigItemOrThrow(Long configId) {
        BizConfigItem item = bizConfigItemMapper.selectById(configId);
        if (ObjectUtil.isNull(item)) {
            throw new IllegalArgumentException("配置不存在");
        }
        return item;
    }

    private BizConfigVersion getVersion(Long configId, Integer versionNo) {
        if (ObjectUtil.isNull(versionNo) || versionNo <= 0) {
            return null;
        }

        return bizConfigVersionMapper.selectOne(Wrappers.<BizConfigVersion>lambdaQuery()
                .eq(BizConfigVersion::getConfigId, configId)
                .eq(BizConfigVersion::getVersionNo, versionNo)
                .last("limit 1"));
    }

    private void saveReleaseRecord(BizConfigItem item, String actionType, Integer beforeVersionNo, Integer afterVersionNo, String remark) {
        BizConfigReleaseRecord record = new BizConfigReleaseRecord();
        record.setConfigId(item.getId());
        record.setTenantId(item.getTenantId());
        record.setConfigKey(item.getConfigKey());
        record.setActionType(actionType);
        record.setBeforeVersionNo(beforeVersionNo);
        record.setAfterVersionNo(afterVersionNo);
        record.setReleaseRemark(remark);
        record.setOperatorId(LoginUserUtil.getUserId());
        record.setOperatorName(LoginUserUtil.getUserName());
        record.setOperateTime(LocalDateTime.now());
        bizConfigReleaseRecordMapper.insert(record);
    }
}
```

上面 `getActiveConfig` 中有一个 Java 写法需要注意，不能写成 `!BizConfigConstant.DEFAULT_TENANT_ID == tenantId`，应改为下面这种判断。

替换 `getActiveConfig` 方法中的租户回退逻辑：

```java
if (!BizConfigConstant.DEFAULT_TENANT_ID.equals(tenantId)) {
    return bizConfigCache.getActiveConfig(BizConfigConstant.DEFAULT_TENANT_ID, configKey);
}
```

### 配置缓存组件

缓存组件负责统一处理 Redis Key、缓存读取、缓存回源和缓存刷新。业务层不直接操作 Redis。

`BizConfigProperties` 读取前文 `application.yml` 中的 `biz.config` 配置。

文件位置：`src/main/java/io/github/atengk/config/config/BizConfigProperties.java`

```java
package io.github.atengk.config.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 业务配置属性
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "biz.config")
public class BizConfigProperties {

    private String redisPrefix = "biz:config";

    private Long defaultTenantId = 0L;

    private Long cacheTtlSeconds = 86400L;
}
```

`BizConfigCache` 先读 Redis，缓存不存在时从 MySQL 当前生效版本回源，并写回 Redis。

文件位置：`src/main/java/io/github/atengk/config/cache/BizConfigCache.java`

```java
package io.github.atengk.config.cache;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.config.config.BizConfigProperties;
import io.github.atengk.config.constant.BizConfigConstant;
import io.github.atengk.config.entity.BizConfigItem;
import io.github.atengk.config.entity.BizConfigVersion;
import io.github.atengk.config.mapper.BizConfigItemMapper;
import io.github.atengk.config.mapper.BizConfigVersionMapper;
import io.github.atengk.config.vo.ConfigValueVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 业务配置缓存组件
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BizConfigCache {

    private final StringRedisTemplate stringRedisTemplate;
    private final BizConfigItemMapper bizConfigItemMapper;
    private final BizConfigVersionMapper bizConfigVersionMapper;
    private final BizConfigProperties bizConfigProperties;

    /**
     * 获取当前生效配置
     *
     * @param tenantId 租户ID
     * @param configKey 配置Key
     * @return 当前生效配置
     */
    public ConfigValueVO getActiveConfig(Long tenantId, String configKey) {
        String redisKey = buildRedisKey(tenantId, configKey);
        String cacheValue = stringRedisTemplate.opsForValue().get(redisKey);

        if (StrUtil.isNotBlank(cacheValue)) {
            return JSONUtil.toBean(cacheValue, ConfigValueVO.class);
        }

        ConfigValueVO dbValue = loadFromDatabase(tenantId, configKey);
        if (ObjectUtil.isNotNull(dbValue)) {
            writeCache(dbValue);
        }
        return dbValue;
    }

    /**
     * 刷新指定配置缓存
     *
     * @param tenantId 租户ID
     * @param configKey 配置Key
     */
    public void refreshConfig(Long tenantId, String configKey) {
        ConfigValueVO dbValue = loadFromDatabase(tenantId, configKey);
        String redisKey = buildRedisKey(tenantId, configKey);

        if (ObjectUtil.isNull(dbValue)) {
            stringRedisTemplate.delete(redisKey);
            log.info("配置不存在，已清理缓存，tenantId={}，configKey={}", tenantId, configKey);
            return;
        }

        writeCache(dbValue);
        log.info("刷新配置缓存成功，tenantId={}，configKey={}，versionNo={}",
                tenantId, configKey, dbValue.getVersionNo());
    }

    /**
     * 删除指定配置缓存
     *
     * @param tenantId 租户ID
     * @param configKey 配置Key
     */
    public void evictConfig(Long tenantId, String configKey) {
        stringRedisTemplate.delete(buildRedisKey(tenantId, configKey));
        log.info("删除配置缓存成功，tenantId={}，configKey={}", tenantId, configKey);
    }

    private ConfigValueVO loadFromDatabase(Long tenantId, String configKey) {
        BizConfigItem item = bizConfigItemMapper.selectOne(Wrappers.<BizConfigItem>lambdaQuery()
                .eq(BizConfigItem::getTenantId, tenantId)
                .eq(BizConfigItem::getConfigKey, configKey)
                .eq(BizConfigItem::getStatus, BizConfigConstant.ENABLED)
                .last("limit 1"));

        if (ObjectUtil.isNull(item) || ObjectUtil.isNull(item.getCurrentVersionNo()) || item.getCurrentVersionNo() <= 0) {
            return null;
        }

        BizConfigVersion version = bizConfigVersionMapper.selectOne(Wrappers.<BizConfigVersion>lambdaQuery()
                .eq(BizConfigVersion::getConfigId, item.getId())
                .eq(BizConfigVersion::getVersionNo, item.getCurrentVersionNo())
                .last("limit 1"));

        if (ObjectUtil.isNull(version)) {
            return null;
        }

        ConfigValueVO vo = new ConfigValueVO();
        vo.setTenantId(tenantId);
        vo.setConfigKey(configKey);
        vo.setVersionNo(version.getVersionNo());
        vo.setConfigValue(version.getConfigValue());
        return vo;
    }

    private void writeCache(ConfigValueVO value) {
        String redisKey = buildRedisKey(value.getTenantId(), value.getConfigKey());
        stringRedisTemplate.opsForValue().set(
                redisKey,
                JSONUtil.toJsonStr(value),
                Duration.ofSeconds(bizConfigProperties.getCacheTtlSeconds())
        );
    }

    private String buildRedisKey(Long tenantId, String configKey) {
        return StrUtil.format("{}:{}:{}", bizConfigProperties.getRedisPrefix(), tenantId, configKey);
    }
}
```

### 配置刷新监听器

配置发布和回滚都在事务中完成。为了避免事务未提交就读取旧数据，缓存刷新使用 `@TransactionalEventListener(phase = AFTER_COMMIT)`。

`ConfigRefreshEvent` 表示配置刷新事件。

文件位置：`src/main/java/io/github/atengk/config/event/ConfigRefreshEvent.java`

```java
package io.github.atengk.config.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 配置刷新事件
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
public class ConfigRefreshEvent extends ApplicationEvent {

    private final Long tenantId;

    private final String configKey;

    public ConfigRefreshEvent(Object source, Long tenantId, String configKey) {
        super(source);
        this.tenantId = tenantId;
        this.configKey = configKey;
    }
}
```

`ConfigRefreshListener` 在事务提交后刷新 Redis 缓存。

文件位置：`src/main/java/io/github/atengk/config/event/ConfigRefreshListener.java`

```java
package io.github.atengk.config.event;

import io.github.atengk.config.cache.BizConfigCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 配置刷新监听器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigRefreshListener {

    private final BizConfigCache bizConfigCache;

    /**
     * 事务提交后刷新配置缓存
     *
     * @param event 配置刷新事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refreshAfterCommit(ConfigRefreshEvent event) {
        bizConfigCache.refreshConfig(event.getTenantId(), event.getConfigKey());
        log.info("事务提交后刷新配置完成，tenantId={}，configKey={}", event.getTenantId(), event.getConfigKey());
    }
}
```

如果后续是多实例部署，可以在这个监听器中额外发送 RabbitMQ 消息：

```text
当前实例：事务提交后刷新 Redis
其他实例：收到 MQ 消息后清理本地缓存
```

当前案例没有使用本地缓存，所以 Redis 刷新后所有实例都能读到最新配置。

### 业务规则执行器

业务规则执行器模拟“创建订单前校验订单金额”的场景。它通过配置 Key 读取当前生效规则，然后使用 Hutool `JSONUtil` 解析 JSON，最后执行金额判断。

订单金额规则结构如下：

```json
{
  "enabled": true,
  "maxOrderAmount": 5000,
  "vipMaxOrderAmount": 20000,
  "minOrderAmount": 10,
  "allowVipExceeded": true
}
```

`OrderAmountRuleExecutor` 实现订单金额规则校验。

文件位置：`src/main/java/io/github/atengk/config/executor/OrderAmountRuleExecutor.java`

```java
package io.github.atengk.config.executor;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.config.constant.BizConfigConstant;
import io.github.atengk.config.dto.OrderAmountCheckDTO;
import io.github.atengk.config.service.BizConfigService;
import io.github.atengk.config.vo.ConfigValueVO;
import io.github.atengk.config.vo.OrderAmountCheckResultVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单金额规则执行器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAmountRuleExecutor {

    private final BizConfigService bizConfigService;

    /**
     * 校验订单金额
     *
     * @param dto 校验请求
     * @return 校验结果
     */
    public OrderAmountCheckResultVO check(OrderAmountCheckDTO dto) {
        ConfigValueVO configValue = bizConfigService.getActiveConfig(
                dto.getTenantId(),
                BizConfigConstant.CONFIG_KEY_ORDER_AMOUNT_RULE
        );

        if (ObjectUtil.isNull(configValue)) {
            log.info("订单金额规则未配置，默认放行，tenantId={}", dto.getTenantId());
            return buildResult(true, "规则未配置，默认放行", null);
        }

        OrderAmountRule rule = JSONUtil.toBean(configValue.getConfigValue(), OrderAmountRule.class);

        if (Boolean.FALSE.equals(rule.getEnabled())) {
            log.info("订单金额规则未启用，默认放行，tenantId={}，versionNo={}", dto.getTenantId(), configValue.getVersionNo());
            return buildResult(true, "规则未启用，默认放行", configValue.getVersionNo());
        }

        BigDecimal orderAmount = dto.getOrderAmount();

        if (orderAmount.compareTo(rule.getMinOrderAmount()) < 0) {
            return buildResult(false, "订单金额低于最小下单金额", configValue.getVersionNo());
        }

        boolean vipUser = BizConfigConstant.USER_TYPE_VIP.equalsIgnoreCase(dto.getUserType());

        if (!vipUser && orderAmount.compareTo(rule.getMaxOrderAmount()) > 0) {
            return buildResult(false, "普通用户订单金额超过最大限制", configValue.getVersionNo());
        }

        if (vipUser) {
            if (Boolean.FALSE.equals(rule.getAllowVipExceeded())) {
                return checkWithCommonLimit(orderAmount, rule, configValue.getVersionNo());
            }

            if (orderAmount.compareTo(rule.getVipMaxOrderAmount()) > 0) {
                return buildResult(false, "VIP 用户订单金额超过最大限制", configValue.getVersionNo());
            }
        }

        return buildResult(true, "校验通过", configValue.getVersionNo());
    }

    private OrderAmountCheckResultVO checkWithCommonLimit(BigDecimal orderAmount, OrderAmountRule rule, Integer versionNo) {
        if (orderAmount.compareTo(rule.getMaxOrderAmount()) > 0) {
            return buildResult(false, "VIP 用户不允许超过普通额度", versionNo);
        }
        return buildResult(true, "校验通过", versionNo);
    }

    private OrderAmountCheckResultVO buildResult(Boolean passed, String message, Integer versionNo) {
        OrderAmountCheckResultVO result = new OrderAmountCheckResultVO();
        result.setPassed(passed);
        result.setMessage(message);
        result.setVersionNo(versionNo);
        return result;
    }

    /**
     * 订单金额规则配置
     *
     * @author Ateng
     * @since 2026-05-15
     */
    @Data
    public static class OrderAmountRule {

        private Boolean enabled = true;

        private BigDecimal maxOrderAmount = new BigDecimal("5000");

        private BigDecimal vipMaxOrderAmount = new BigDecimal("20000");

        private BigDecimal minOrderAmount = new BigDecimal("10");

        private Boolean allowVipExceeded = true;
    }
}
```

### 配置审计切面

审计切面用于记录配置保存、发布、回滚等关键动作。这里采用注解 + AOP 的方式，避免在每个业务方法中手写审计日志。

`ConfigAudit` 注解用于标记需要审计的方法。

文件位置：`src/main/java/io/github/atengk/config/annotation/ConfigAudit.java`

```java
package io.github.atengk.config.annotation;

import java.lang.annotation.*;

/**
 * 配置审计注解
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigAudit {

    /**
     * 操作类型
     *
     * @return 操作类型
     */
    String operationType();
}
```

`ConfigAuditAspect` 负责自动记录方法入参、返回值、异常信息、操作人、IP 和 TraceId。

文件位置：`src/main/java/io/github/atengk/config/aspect/ConfigAuditAspect.java`

```java
package io.github.atengk.config.aspect;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.config.annotation.ConfigAudit;
import io.github.atengk.config.constant.BizConfigConstant;
import io.github.atengk.config.entity.BizConfigAuditLog;
import io.github.atengk.config.mapper.BizConfigAuditLogMapper;
import io.github.atengk.config.util.LoginUserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 配置审计切面
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ConfigAuditAspect {

    private final BizConfigAuditLogMapper bizConfigAuditLogMapper;

    /**
     * 记录配置操作审计日志
     *
     * @param joinPoint 连接点
     * @param configAudit 审计注解
     * @return 方法执行结果
     * @throws Throwable 业务异常
     */
    @Around("@annotation(configAudit)")
    public Object around(ProceedingJoinPoint joinPoint, ConfigAudit configAudit) throws Throwable {
        LocalDateTime operateTime = LocalDateTime.now();
        String beforeValue = JSONUtil.toJsonStr(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();

            saveAuditLog(configAudit.operationType(), beforeValue, JSONUtil.toJsonStr(result),
                    BizConfigConstant.SUCCESS, null, operateTime);

            return result;
        } catch (Throwable throwable) {
            saveAuditLog(configAudit.operationType(), beforeValue, null,
                    BizConfigConstant.FAIL, ExceptionUtil.getSimpleMessage(throwable), operateTime);

            log.error("配置操作失败，operationType={}，error={}", configAudit.operationType(), throwable.getMessage(), throwable);
            throw throwable;
        }
    }

    private void saveAuditLog(String operationType, String beforeValue, String afterValue,
                              Integer success, String errorMessage, LocalDateTime operateTime) {
        try {
            BizConfigAuditLog auditLog = new BizConfigAuditLog();
            auditLog.setOperationType(operationType);
            auditLog.setBeforeValue(beforeValue);
            auditLog.setAfterValue(afterValue);
            auditLog.setOperatorId(LoginUserUtil.getUserId());
            auditLog.setOperatorName(LoginUserUtil.getUserName());
            auditLog.setRequestIp(getRequestIp());
            auditLog.setTraceId(StrUtil.blankToDefault(MDC.get("traceId"), ""));
            auditLog.setOperateTime(operateTime);
            auditLog.setSuccess(success);
            auditLog.setErrorMessage(errorMessage);

            bizConfigAuditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("保存配置审计日志失败，operationType={}，error={}", operationType, e.getMessage(), e);
        }
    }

    private String getRequestIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "";
        }

        HttpServletRequest request = attributes.getRequest();
        return JakartaServletUtil.getClientIP(request);
    }
}
```

这版审计切面记录的是“方法级审计”，重点是能追踪一次操作的入参、结果和异常。生产级项目可以继续增强为“字段级审计”，例如记录配置修改前后的 JSON Diff。

当前核心链路已经具备：

```text
保存草稿
-> 发布版本
-> 事务提交后刷新 Redis
-> 业务读取当前配置
-> 执行订单金额规则
-> 自动记录配置操作审计
```

后续继续写接口设计时，可以直接基于这些 Service 和 Executor 暴露 REST API。

## 接口设计

本节把前面实现的配置服务、缓存组件和业务规则执行器暴露为 REST 接口，方便后台管理端维护配置，也方便业务系统在下单前执行规则校验。该模块对应 README 中“动态配置 / 业务规则配置”的后台维护、发布、服务读取、缓存刷新和业务执行链路。

先新增配置管理接口 Controller。

文件位置：`src/main/java/io/github/atengk/config/controller/BizConfigController.java`

```java
package io.github.atengk.config.controller;

import io.github.atengk.config.dto.PublishConfigDTO;
import io.github.atengk.config.dto.RollbackConfigDTO;
import io.github.atengk.config.dto.SaveConfigDTO;
import io.github.atengk.config.service.BizConfigService;
import io.github.atengk.config.vo.BizConfigVO;
import io.github.atengk.config.vo.ConfigValueVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 业务配置接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/biz-config")
public class BizConfigController {

    private final BizConfigService bizConfigService;

    /**
     * 新增或修改配置草稿
     *
     * @param dto 配置草稿请求
     * @return 配置ID
     */
    @PostMapping("/save-draft")
    public Long saveDraft(@Valid @RequestBody SaveConfigDTO dto) {
        return bizConfigService.saveDraft(dto);
    }

    /**
     * 发布配置
     *
     * @param dto 发布配置请求
     * @return 发布后的版本号
     */
    @PostMapping("/publish")
    public Integer publish(@Valid @RequestBody PublishConfigDTO dto) {
        return bizConfigService.publish(dto);
    }

    /**
     * 回滚配置
     *
     * @param dto 回滚配置请求
     */
    @PostMapping("/rollback")
    public void rollback(@Valid @RequestBody RollbackConfigDTO dto) {
        bizConfigService.rollback(dto);
    }

    /**
     * 查询配置详情
     *
     * @param configId 配置ID
     * @return 配置详情
     */
    @GetMapping("/{configId}")
    public BizConfigVO getDetail(@PathVariable Long configId) {
        return bizConfigService.getDetail(configId);
    }

    /**
     * 查询当前生效配置
     *
     * @param tenantId 租户ID
     * @param configKey 配置Key
     * @return 当前生效配置
     */
    @GetMapping("/active")
    public ConfigValueVO getActiveConfig(@RequestParam Long tenantId,
                                         @RequestParam String configKey) {
        return bizConfigService.getActiveConfig(tenantId, configKey);
    }
}
```

再新增一个业务规则执行接口，用来模拟订单创建前的金额规则校验。

文件位置：`src/main/java/io/github/atengk/config/controller/OrderRuleController.java`

```java
package io.github.atengk.config.controller;

import io.github.atengk.config.dto.OrderAmountCheckDTO;
import io.github.atengk.config.executor.OrderAmountRuleExecutor;
import io.github.atengk.config.vo.OrderAmountCheckResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单规则接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-rule")
public class OrderRuleController {

    private final OrderAmountRuleExecutor orderAmountRuleExecutor;

    /**
     * 校验订单金额规则
     *
     * @param dto 订单金额校验请求
     * @return 校验结果
     */
    @PostMapping("/check-amount")
    public OrderAmountCheckResultVO checkAmount(@Valid @RequestBody OrderAmountCheckDTO dto) {
        return orderAmountRuleExecutor.check(dto);
    }
}
```

### 新增或修改配置

该接口只保存草稿，不影响线上当前生效配置。适合后台管理端“编辑后先保存，确认后再发布”的场景。

接口信息：

```text
POST /api/biz-config/save-draft
Content-Type: application/json
```

请求示例：

```json
{
  "tenantId": 1001,
  "configKey": "order:create:amount-rule",
  "configName": "订单创建金额规则",
  "configType": "JSON_RULE",
  "draftValue": "{\"enabled\":true,\"maxOrderAmount\":5000,\"vipMaxOrderAmount\":20000,\"minOrderAmount\":10,\"allowVipExceeded\":true}",
  "remark": "租户1001订单金额规则"
}
```

curl 调用：

```bash
curl -X POST "http://localhost:8080/api/biz-config/save-draft" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1001,
    "configKey": "order:create:amount-rule",
    "configName": "订单创建金额规则",
    "configType": "JSON_RULE",
    "draftValue": "{\"enabled\":true,\"maxOrderAmount\":5000,\"vipMaxOrderAmount\":20000,\"minOrderAmount\":10,\"allowVipExceeded\":true}",
    "remark": "租户1001订单金额规则"
  }'
```

返回示例：

```json
1890000000000000001
```

这个返回值就是 `configId`，后续发布和回滚都要使用它。

### 发布配置

发布接口会把当前草稿生成一个正式版本，并刷新 Redis 缓存。业务接口读取配置时，只会读取已发布版本，不会读取草稿。

接口信息：

```text
POST /api/biz-config/publish
Content-Type: application/json
```

请求示例：

```json
{
  "configId": 1890000000000000001,
  "publishRemark": "首次发布订单金额规则"
}
```

curl 调用：

```bash
curl -X POST "http://localhost:8080/api/biz-config/publish" \
  -H "Content-Type: application/json" \
  -d '{
    "configId": 1890000000000000001,
    "publishRemark": "首次发布订单金额规则"
  }'
```

返回示例：

```json
1
```

返回值表示发布后的版本号。首次发布通常是 `1`，后续修改草稿再发布会递增为 `2`、`3`。

### 回滚配置

回滚接口会把当前生效版本切换到指定历史版本，并同步刷新 Redis 缓存。

接口信息：

```text
POST /api/biz-config/rollback
Content-Type: application/json
```

请求示例：

```json
{
  "configId": 1890000000000000001,
  "targetVersionNo": 1,
  "rollbackRemark": "规则异常，回滚到版本1"
}
```

curl 调用：

```bash
curl -X POST "http://localhost:8080/api/biz-config/rollback" \
  -H "Content-Type: application/json" \
  -d '{
    "configId": 1890000000000000001,
    "targetVersionNo": 1,
    "rollbackRemark": "规则异常，回滚到版本1"
  }'
```

返回结果为空，HTTP 状态码为 `200` 表示回滚成功。

回滚成功后会发生两件事：

```text
biz_config_item.current_version_no 切换为目标版本号
Redis 中 biz:config:{tenantId}:{configKey} 被刷新为目标版本内容
```

### 查询配置

查询配置分为两类：后台查询配置详情、业务查询当前生效配置。

后台查询配置详情：

```text
GET /api/biz-config/{configId}
```

curl 调用：

```bash
curl "http://localhost:8080/api/biz-config/1890000000000000001"
```

返回示例：

```json
{
  "id": 1890000000000000001,
  "tenantId": 1001,
  "configKey": "order:create:amount-rule",
  "configName": "订单创建金额规则",
  "configType": "JSON_RULE",
  "draftValue": "{\"enabled\":true,\"maxOrderAmount\":5000,\"vipMaxOrderAmount\":20000,\"minOrderAmount\":10,\"allowVipExceeded\":true}",
  "currentVersionNo": 1,
  "status": 1,
  "remark": "租户1001订单金额规则",
  "updateTime": "2026-05-15T10:30:00"
}
```

业务查询当前生效配置：

```text
GET /api/biz-config/active?tenantId=1001&configKey=order:create:amount-rule
```

curl 调用：

```bash
curl "http://localhost:8080/api/biz-config/active?tenantId=1001&configKey=order:create:amount-rule"
```

返回示例：

```json
{
  "tenantId": 1001,
  "configKey": "order:create:amount-rule",
  "versionNo": 1,
  "configValue": "{\"enabled\":true,\"maxOrderAmount\":5000,\"vipMaxOrderAmount\":20000,\"minOrderAmount\":10,\"allowVipExceeded\":true}"
}
```

### 执行业务规则

业务规则执行接口模拟“创建订单前校验订单金额”。它会读取当前租户下的 `order:create:amount-rule` 配置，然后执行金额规则判断。

接口信息：

```text
POST /api/order-rule/check-amount
Content-Type: application/json
```

普通用户订单金额校验：

```bash
curl -X POST "http://localhost:8080/api/order-rule/check-amount" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1001,
    "orderAmount": 6000,
    "userType": "NORMAL"
  }'
```

返回示例：

```json
{
  "passed": false,
  "message": "普通用户订单金额超过最大限制",
  "versionNo": 1
}
```

VIP 用户订单金额校验：

```bash
curl -X POST "http://localhost:8080/api/order-rule/check-amount" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1001,
    "orderAmount": 12000,
    "userType": "VIP"
  }'
```

返回示例：

```json
{
  "passed": true,
  "message": "校验通过",
  "versionNo": 1
}
```

## 实操验证

本节通过 MySQL、Redis 和接口调用验证完整链路。

验证顺序如下：

```text
启动 MySQL、Redis、Nacos
-> 启动 Spring Boot 项目
-> 保存配置草稿
-> 发布配置
-> 检查 MySQL 版本表
-> 检查 Redis 缓存
-> 调用业务规则校验接口
-> 修改草稿并再次发布
-> 验证配置实时刷新
-> 回滚配置
-> 再次验证业务规则
```

### 初始化测试数据

先创建数据库。

```sql
CREATE DATABASE IF NOT EXISTS dynamic_config_demo
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;
```

确认 4 张表已经执行过前文 SQL：

```sql
SHOW TABLES;
```

预期能看到：

```text
biz_config_item
biz_config_version
biz_config_release_record
biz_config_audit_log
```

启动基础服务。

```bash
docker run -d --name dynamic-config-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=dynamic_config_demo \
  mysql:8.0

docker run -d --name dynamic-config-redis \
  -p 6379:6379 \
  redis:7.2

docker run -d --name dynamic-config-nacos \
  -p 8848:8848 \
  -e MODE=standalone \
  nacos/nacos-server:v2.3.2
```

上面命令分别启动 MySQL、Redis 和 Nacos。MySQL 初始化数据库名为 `dynamic_config_demo`，Redis 使用默认 0 号库，Nacos 使用单机模式。

启动项目后，先保存一条订单金额规则草稿。

```bash
curl -X POST "http://localhost:8080/api/biz-config/save-draft" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1001,
    "configKey": "order:create:amount-rule",
    "configName": "订单创建金额规则",
    "configType": "JSON_RULE",
    "draftValue": "{\"enabled\":true,\"maxOrderAmount\":5000,\"vipMaxOrderAmount\":20000,\"minOrderAmount\":10,\"allowVipExceeded\":true}",
    "remark": "初始化订单金额规则"
  }'
```

查询配置主表：

```sql
SELECT
    id,
    tenant_id,
    config_key,
    config_name,
    current_version_no,
    draft_value
FROM biz_config_item
WHERE tenant_id = 1001
  AND config_key = 'order:create:amount-rule';
```

预期结果：

```text
current_version_no = 0
draft_value 有值
```

这说明配置只保存为草稿，还没有发布生效。

### 发布配置验证

使用上一步返回的 `configId` 发布配置。

```bash
curl -X POST "http://localhost:8080/api/biz-config/publish" \
  -H "Content-Type: application/json" \
  -d '{
    "configId": 1890000000000000001,
    "publishRemark": "首次发布订单金额规则"
  }'
```

查询配置主表：

```sql
SELECT
    id,
    tenant_id,
    config_key,
    current_version_no
FROM biz_config_item
WHERE id = 1890000000000000001;
```

预期结果：

```text
current_version_no = 1
```

查询配置版本表：

```sql
SELECT
    config_id,
    tenant_id,
    config_key,
    version_no,
    config_value,
    publish_name,
    publish_time
FROM biz_config_version
WHERE config_id = 1890000000000000001
ORDER BY version_no DESC;
```

预期结果：

```text
version_no = 1
config_value = 发布时的完整规则 JSON
```

查询发布记录表：

```sql
SELECT
    config_id,
    action_type,
    before_version_no,
    after_version_no,
    release_remark,
    operate_time
FROM biz_config_release_record
WHERE config_id = 1890000000000000001
ORDER BY operate_time DESC;
```

预期结果：

```text
action_type = PUBLISH
before_version_no = 0
after_version_no = 1
```

### Redis 缓存验证

发布成功后，监听器会在事务提交后刷新 Redis。

进入 Redis 容器：

```bash
docker exec -it dynamic-config-redis redis-cli
```

查询配置缓存 Key：

```bash
GET biz:config:1001:order:create:amount-rule
```

预期返回类似：

```json
{
  "tenantId": 1001,
  "configKey": "order:create:amount-rule",
  "versionNo": 1,
  "configValue": "{\"enabled\":true,\"maxOrderAmount\":5000,\"vipMaxOrderAmount\":20000,\"minOrderAmount\":10,\"allowVipExceeded\":true}"
}
```

也可以查看过期时间：

```bash
TTL biz:config:1001:order:create:amount-rule
```

预期结果是一个大于 0 的秒数，例如：

```text
86390
```

如果 Redis 没有值，先检查服务日志中是否出现：

```text
事务提交后刷新配置完成
刷新配置缓存成功
```

### 配置实时刷新验证

先修改配置草稿，把普通用户最大订单金额从 `5000` 改成 `3000`。

```bash
curl -X POST "http://localhost:8080/api/biz-config/save-draft" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1001,
    "configKey": "order:create:amount-rule",
    "configName": "订单创建金额规则",
    "configType": "JSON_RULE",
    "draftValue": "{\"enabled\":true,\"maxOrderAmount\":3000,\"vipMaxOrderAmount\":20000,\"minOrderAmount\":10,\"allowVipExceeded\":true}",
    "remark": "调整普通用户最大订单金额为3000"
  }'
```

此时 Redis 仍然是旧配置，因为只是保存草稿，未发布。

```bash
GET biz:config:1001:order:create:amount-rule
```

预期仍然看到：

```text
maxOrderAmount = 5000
versionNo = 1
```

再次发布配置：

```bash
curl -X POST "http://localhost:8080/api/biz-config/publish" \
  -H "Content-Type: application/json" \
  -d '{
    "configId": 1890000000000000001,
    "publishRemark": "调整普通用户最大订单金额为3000"
  }'
```

再次查询 Redis：

```bash
GET biz:config:1001:order:create:amount-rule
```

预期看到：

```text
maxOrderAmount = 3000
versionNo = 2
```

这说明配置发布后已经实时刷新到缓存。

### 业务规则执行验证

验证普通用户金额超过配置限制。

```bash
curl -X POST "http://localhost:8080/api/order-rule/check-amount" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1001,
    "orderAmount": 4000,
    "userType": "NORMAL"
  }'
```

如果当前版本中 `maxOrderAmount = 3000`，预期返回：

```json
{
  "passed": false,
  "message": "普通用户订单金额超过最大限制",
  "versionNo": 2
}
```

验证 VIP 用户金额在 VIP 限制内。

```bash
curl -X POST "http://localhost:8080/api/order-rule/check-amount" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1001,
    "orderAmount": 12000,
    "userType": "VIP"
  }'
```

预期返回：

```json
{
  "passed": true,
  "message": "校验通过",
  "versionNo": 2
}
```

验证订单金额低于最小金额。

```bash
curl -X POST "http://localhost:8080/api/order-rule/check-amount" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1001,
    "orderAmount": 5,
    "userType": "NORMAL"
  }'
```

预期返回：

```json
{
  "passed": false,
  "message": "订单金额低于最小下单金额",
  "versionNo": 2
}
```

### 配置回滚验证

把配置回滚到版本 `1`。

```bash
curl -X POST "http://localhost:8080/api/biz-config/rollback" \
  -H "Content-Type: application/json" \
  -d '{
    "configId": 1890000000000000001,
    "targetVersionNo": 1,
    "rollbackRemark": "回滚到普通用户最大订单金额5000"
  }'
```

查询配置主表：

```sql
SELECT
    id,
    current_version_no,
    draft_value
FROM biz_config_item
WHERE id = 1890000000000000001;
```

预期结果：

```text
current_version_no = 1
draft_value 已恢复为版本1的配置内容
```

查询发布记录：

```sql
SELECT
    action_type,
    before_version_no,
    after_version_no,
    release_remark
FROM biz_config_release_record
WHERE config_id = 1890000000000000001
ORDER BY operate_time DESC;
```

预期最新记录：

```text
action_type = ROLLBACK
before_version_no = 2
after_version_no = 1
```

查询 Redis：

```bash
GET biz:config:1001:order:create:amount-rule
```

预期看到：

```text
versionNo = 1
maxOrderAmount = 5000
```

再次验证普通用户订单金额 `4000`。

```bash
curl -X POST "http://localhost:8080/api/order-rule/check-amount" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1001,
    "orderAmount": 4000,
    "userType": "NORMAL"
  }'
```

预期返回：

```json
{
  "passed": true,
  "message": "校验通过",
  "versionNo": 1
}
```

这说明回滚后的配置已经对业务规则实时生效。

## 常见扩展

当前案例实现的是最小可用闭环。实际项目中，可以继续扩展多租户隔离、灰度配置、MQ 广播刷新和 Apollo 替换 Nacos 等能力。

### 多租户配置隔离

当前表结构已经包含 `tenant_id` 字段，可以支持租户级配置隔离。

推荐读取策略：

```text
优先读取租户配置
-> 租户配置不存在
-> 读取 tenant_id = 0 的全局默认配置
-> 全局配置也不存在
-> 按业务默认策略执行
```

前面 `BizConfigService#getActiveConfig` 已经预留了全局配置回退逻辑。建议确认代码写法如下：

```java
@Override
public ConfigValueVO getActiveConfig(Long tenantId, String configKey) {
    ConfigValueVO tenantConfig = bizConfigCache.getActiveConfig(tenantId, configKey);
    if (tenantConfig != null) {
        return tenantConfig;
    }

    if (!BizConfigConstant.DEFAULT_TENANT_ID.equals(tenantId)) {
        return bizConfigCache.getActiveConfig(BizConfigConstant.DEFAULT_TENANT_ID, configKey);
    }

    return null;
}
```

多租户配置示例：

```sql
-- 全局默认配置
INSERT INTO biz_config_item (
    id, tenant_id, config_key, config_name, config_type, draft_value,
    current_version_no, status, create_time, update_time, version, deleted
) VALUES (
    1000000000000000000,
    0,
    'order:create:amount-rule',
    '全局订单金额规则',
    'JSON_RULE',
    '{"enabled":true,"maxOrderAmount":5000,"vipMaxOrderAmount":20000,"minOrderAmount":10,"allowVipExceeded":true}',
    0,
    1,
    NOW(),
    NOW(),
    0,
    0
);

-- 租户1001独立配置
INSERT INTO biz_config_item (
    id, tenant_id, config_key, config_name, config_type, draft_value,
    current_version_no, status, create_time, update_time, version, deleted
) VALUES (
    1000000000000000001,
    1001,
    'order:create:amount-rule',
    '租户1001订单金额规则',
    'JSON_RULE',
    '{"enabled":true,"maxOrderAmount":3000,"vipMaxOrderAmount":10000,"minOrderAmount":10,"allowVipExceeded":true}',
    0,
    1,
    NOW(),
    NOW(),
    0,
    0
);
```

这种设计适合 SaaS、多商户、多组织系统。

### 灰度配置

灰度配置用于让一部分用户、门店、地区、渠道先使用新规则，其他用户继续使用旧规则。

一种轻量方案是在配置 JSON 中增加灰度条件：

```json
{
  "enabled": true,
  "maxOrderAmount": 5000,
  "vipMaxOrderAmount": 20000,
  "minOrderAmount": 10,
  "allowVipExceeded": true,
  "gray": {
    "enabled": true,
    "userIds": [10001, 10002],
    "channels": ["APP", "MINI_PROGRAM"]
  }
}
```

业务执行时，先判断是否命中灰度条件：

```text
命中灰度条件
-> 使用灰度规则

未命中灰度条件
-> 使用普通规则
```

如果灰度逻辑变复杂，建议拆成单独的灰度配置表：

```sql
CREATE TABLE biz_config_gray_rule (
    id BIGINT NOT NULL COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '配置ID',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    gray_type VARCHAR(32) NOT NULL COMMENT '灰度类型：USER、ORG、CHANNEL、REGION',
    gray_value VARCHAR(256) NOT NULL COMMENT '灰度值，例如用户ID、组织ID、渠道编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_config_id (config_id),
    KEY idx_tenant_gray (tenant_id, gray_type, gray_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务配置灰度规则表';
```

灰度配置不建议一开始就做得太复杂。多数中后台系统可以先支持“按租户灰度、按用户白名单灰度、按渠道灰度”三类。

### MQ 广播刷新

当前案例只使用 Redis 缓存，没有本地缓存，所以发布后刷新 Redis 即可。如果后续每个服务实例内部还维护了 Caffeine 本地缓存，就需要通过 MQ 通知其他实例清理本地缓存。

推荐流程：

```text
实例 A 发布配置
-> 更新 MySQL
-> 刷新 Redis
-> 发送 MQ 消息 config.refresh
-> 实例 B / C / D 收到消息
-> 清理本地缓存
-> 下次读取从 Redis 加载最新配置
```

刷新消息结构：

```json
{
  "tenantId": 1001,
  "configKey": "order:create:amount-rule",
  "versionNo": 2,
  "eventTime": "2026-05-15 10:30:00"
}
```

如果使用 RabbitMQ，可以新增一个消息 DTO。

文件位置：`src/main/java/io/github/atengk/config/dto/ConfigRefreshMessageDTO.java`

```java
package io.github.atengk.config.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配置刷新消息
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class ConfigRefreshMessageDTO {

    private Long tenantId;

    private String configKey;

    private Integer versionNo;

    private LocalDateTime eventTime;
}
```

消费端收到消息后，只做一件事：清理本地缓存，不重复更新数据库。

```java
package io.github.atengk.config.listener;

import io.github.atengk.config.cache.BizConfigCache;
import io.github.atengk.config.dto.ConfigRefreshMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 配置刷新消息监听器
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigRefreshMessageListener {

    private final BizConfigCache bizConfigCache;

    /**
     * 处理配置刷新消息
     *
     * @param message 配置刷新消息
     */
    public void onMessage(ConfigRefreshMessageDTO message) {
        bizConfigCache.evictConfig(message.getTenantId(), message.getConfigKey());
        log.info("收到配置刷新消息，已清理本地或Redis缓存，tenantId={}，configKey={}，versionNo={}",
                message.getTenantId(), message.getConfigKey(), message.getVersionNo());
    }
}
```

注意：如果只用 Redis，不使用本地缓存，MQ 不是必须的。多实例服务都读同一个 Redis，发布时刷新 Redis 后，各实例自然能读取新值。

### Apollo 替换 Nacos方案

Nacos 和 Apollo 都适合做应用基础配置管理，例如数据库地址、Redis 地址、线程池参数、开关参数等。但本案例里的“业务规则配置”更推荐继续放在 MySQL 中，因为它需要版本、发布记录、回滚、审计、多租户隔离和后台管理。

替换思路：

```text
Nacos 管基础配置
-> Apollo 管基础配置

MySQL 管业务规则配置
-> 不变

Redis 管当前生效业务配置缓存
-> 不变

Service、规则执行器、审计、发布、回滚
-> 不变
```

也就是说，Apollo 替换 Nacos 时，主要改的是项目启动配置，不改业务规则核心代码。

Apollo 配置示例：

```yaml
app:
  id: dynamic-config-demo

apollo:
  meta: http://127.0.0.1:8080
  bootstrap:
    enabled: true
    namespaces: application
    eagerLoad:
      enabled: true
```

如果使用 Apollo 管理 `biz.config` 基础参数，可以维护如下配置项：

```properties
biz.config.redis-prefix=biz:config
biz.config.default-tenant-id=0
biz.config.cache-ttl-seconds=86400
```

应用层的 `BizConfigProperties` 不需要修改，仍然读取：

```java
@ConfigurationProperties(prefix = "biz.config")
```

这种设计可以保证配置中心可替换，而业务规则管理模块稳定不变。最终架构可以理解为：

```text
Nacos / Apollo
-> 管应用启动参数、基础开关、环境配置

MySQL + Redis
-> 管业务规则、版本、发布、回滚、审计、租户隔离
```