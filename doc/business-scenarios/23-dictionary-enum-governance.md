# 字典数据与枚举治理

本文围绕“字典数据与枚举治理”实现一个可直接集成到 Java 后端项目中的基础能力，核心目标是解决后台可配置字典、前端下拉选项、后端业务校验、枚举统一输出、缓存刷新以及前后端值域一致性问题。该场景来自 Java 后端经典业务场景中的第 23 项，推荐技术栈包括 Spring Boot、MyBatis-Plus、Redis、Sa-Token、Hutool、Nacos、MySQL 等。

## 功能目标与业务边界

字典数据通常用于维护系统中变化频率不高、但需要后台配置和前端展示的选项数据，例如用户状态、性别、订单类型、审批状态、客户等级、行业分类、证件类型等。枚举则更适合维护后端强约束、代码逻辑强相关、变更频率低的数据，例如订单状态流转、支付状态、审核状态、消息类型等。

本案例将字典数据和 Java 枚举分开治理：可运营配置的数据进入数据库字典表；强业务逻辑约束的数据使用 Java 枚举；前端通过统一接口获取字典项和枚举项，避免前后端各自硬编码。

### 字典类型管理

字典类型用于定义一组字典项的分类标识，相当于字典数据的分组。比如 `user_status` 表示用户状态，`gender` 表示性别，`customer_level` 表示客户等级。后续所有字典项都必须归属于某一个字典类型。

本案例中的字典类型需要支持以下核心能力：

| 能力         | 说明                                                 |
| ------------ | ---------------------------------------------------- |
| 新增字典类型 | 后台新增一个字典分组，例如 `user_status`             |
| 修改字典类型 | 修改字典名称、备注、状态等基础信息                   |
| 禁用字典类型 | 禁用后，前端不再返回该类型下的字典项                 |
| 查询字典类型 | 支持后台分页查询和详情查询                           |
| 唯一性控制   | `dict_code` 全局唯一，避免不同业务含义复用同一个编码 |

字典类型表重点字段如下：

| 字段        | 示例           | 说明                                     |
| ----------- | -------------- | ---------------------------------------- |
| `dict_code` | `user_status`  | 字典类型编码，后端和前端都使用这个值查询 |
| `dict_name` | `用户状态`     | 字典类型名称，用于后台展示               |
| `status`    | `1`            | 状态，1 启用，0 禁用                     |
| `remark`    | `用户启停状态` | 备注说明                                 |

实际项目中，`dict_code` 不建议频繁修改。因为它通常会被前端页面、后端接口、业务校验逻辑引用。如果一定要修改，需要先全局检索引用点，再做兼容处理。

### 字典项管理

字典项是某个字典类型下的具体选项。例如 `user_status` 下可以有 `enable`、`disable` 两个字典项；`gender` 下可以有 `male`、`female`、`unknown` 三个字典项。

本案例中的字典项需要支持以下核心能力：

| 能力       | 说明                                                 |
| ---------- | ---------------------------------------------------- |
| 新增字典项 | 为某个字典类型添加选项                               |
| 修改字典项 | 修改字典标签、排序、状态、扩展字段                   |
| 禁用字典项 | 禁用后默认不返回给前端，也不允许新增业务数据继续使用 |
| 查询字典项 | 根据 `dict_code` 查询启用项列表                      |
| 字典值校验 | 后端保存业务数据时校验传入值是否合法                 |
| 标签回显   | 查询业务数据时将字典值转换成展示标签                 |
| 缓存刷新   | 字典变更后删除 Redis 缓存，下次查询重新加载          |

字典项表重点字段如下：

| 字段         | 示例                | 说明                         |
| ------------ | ------------------- | ---------------------------- |
| `dict_code`  | `user_status`       | 归属字典类型                 |
| `item_value` | `enable`            | 字典项值，业务库存储该值     |
| `item_label` | `启用`              | 字典项显示名称，前端展示该值 |
| `sort_order` | `1`                 | 排序值                       |
| `status`     | `1`                 | 状态，1 启用，0 禁用         |
| `tag_type`   | `success`           | 前端标签样式，可选           |
| `extra_json` | `{"color":"green"}` | 扩展配置，可选               |

业务表中应存储 `item_value`，而不是 `item_label`。例如用户表中的状态字段应存储 `enable`，不应存储 `启用`。这样可以避免展示文案变更时影响历史业务数据。

### 枚举与字典的关系

字典和枚举都能表达“固定选项”，但使用边界不同。字典更偏配置和展示，枚举更偏代码约束和流程控制。

| 对比项         | 字典数据                           | Java 枚举                    |
| -------------- | ---------------------------------- | ---------------------------- |
| 数据来源       | 数据库维护                         | 代码定义                     |
| 是否可后台修改 | 可以                               | 不可以，需要发版             |
| 适用场景       | 下拉框、分类、标签、等级、来源渠道 | 状态机、业务分支、强约束类型 |
| 变更频率       | 中低频                             | 极低频                       |
| 前端获取方式   | 字典接口                           | 枚举接口                     |
| 后端校验方式   | 查缓存或数据库                     | 枚举静态方法校验             |

建议规则如下：

| 场景                               | 推荐方式   |
| ---------------------------------- | ---------- |
| 性别、客户等级、行业分类、证件类型 | 字典       |
| 订单状态、支付状态、审批状态       | 枚举       |
| 是否启用、是否删除、是否默认       | 枚举或常量 |
| 需要运营后台动态维护的选项         | 字典       |
| 影响核心业务流转的状态             | 枚举       |

例如订单状态不建议做成普通字典，因为订单状态会影响支付、发货、退款、关闭等核心业务流程。如果后台误改字典值，可能导致状态判断失效。订单状态更适合使用 Java 枚举，并通过统一枚举接口暴露给前端展示。

### 本案例实现范围

本案例实现一个轻量但完整的字典与枚举治理模块，重点覆盖后端核心能力，不展开复杂权限、多语言、多租户和低代码配置。

本案例将实现以下内容：

| 模块         | 实现内容                                |
| ------------ | --------------------------------------- |
| 数据库设计   | `sys_dict_type`、`sys_dict_item` 两张表 |
| 字典类型管理 | 新增、修改、删除、分页查询              |
| 字典项管理   | 新增、修改、删除、根据类型查询          |
| 字典缓存     | Redis 缓存字典项列表                    |
| 缓存刷新     | 字典类型或字典项变更后清理缓存          |
| 字典校验     | 后端校验某个字典值是否合法              |
| 字典回显     | 根据字典值获取展示标签                  |
| 枚举治理     | 定义统一枚举接口，统一输出枚举选项      |
| 前端接口     | 提供字典选项接口和枚举选项接口          |

本案例不实现以下扩展能力，只保留后续扩展点：

| 暂不实现       | 原因                                                         |
| -------------- | ------------------------------------------------------------ |
| 多租户字典     | 会引入租户上下文和租户隔离，本案例先聚焦单体基础能力         |
| 多语言字典     | 会增加语言表或国际化字段，本案例只实现中文标签               |
| 字典版本管理   | 适合复杂配置中心场景，本案例只做当前有效值                   |
| 字典发布审核   | 适合政企或低代码平台，本案例只做直接维护                     |
| Nacos 动态配置 | 字典数据主要存数据库和 Redis，Nacos 可用于配置缓存前缀等参数 |
| 前端页面实现   | 本案例以 Java 后端接口和核心代码为主                         |

最终效果是：前端通过 `dictCode` 获取下拉选项，后端保存业务数据时通过字典服务校验值是否合法，查询业务数据时可以通过字典服务完成标签回显；对于 Java 枚举，前端可以通过统一枚举接口获取枚举选项，避免重复硬编码。

## 技术栈与项目结构

本模块基于 Spring Boot 3、MyBatis-Plus、Redis、Hutool、MySQL 实现。技术栈与原 README 中第 23 个场景推荐方向保持一致，并补充了 Lombok、Spring Validation、Knife4j 等常用工程组件，便于后续直接写接口和联调。

### 技术栈选型

本案例按单体 Spring Boot 项目实现，后续如果拆成微服务，也可以把字典模块独立成基础服务，供其他业务服务通过 OpenFeign 调用。

| 技术              | 用途                                     |
| ----------------- | ---------------------------------------- |
| Spring Boot 3.x   | 后端基础框架                             |
| MyBatis-Plus      | 字典类型、字典项 CRUD                    |
| MySQL 8.x         | 存储字典类型和字典项                     |
| Redis             | 缓存字典项列表，减少数据库查询           |
| Hutool            | 字符串、集合、JSON 等工具处理            |
| Lombok            | 简化实体类、DTO、VO 代码                 |
| Spring Validation | 接口参数校验                             |
| Sa-Token          | 登录认证与后台接口权限扩展，本案例只预留 |
| Knife4j           | 接口文档调试，可选                       |
| Jackson           | JSON 序列化，Spring Boot 默认集成        |

核心设计取舍如下：

| 设计点       | 方案                                               |
| ------------ | -------------------------------------------------- |
| 字典类型编码 | 使用 `dict_code` 作为业务唯一标识                  |
| 字典项取值   | 业务表只存 `item_value`                            |
| 字典项展示   | 前端展示 `item_label`                              |
| 缓存粒度     | 按 `dict_code` 缓存字典项列表                      |
| 缓存刷新     | 新增、修改、删除字典类型或字典项后删除缓存         |
| 禁用处理     | 查询选项默认只返回启用数据，校验时默认只认启用数据 |
| 枚举治理     | 后续通过统一枚举接口暴露给前端                     |

### Maven 依赖配置

下面依赖放在项目根目录 `pom.xml` 中。版本号可以交给 Spring Boot Parent 和 dependencyManagement 管理，示例中保留关键依赖即可。

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST 接口能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Validation：用于 Controller 入参校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Redis：缓存字典数据 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- MyBatis-Plus：简化数据库 CRUD -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.7</version>
    </dependency>

    <!-- MySQL 驱动：连接 MySQL 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool：常用工具类，处理字符串、集合、JSON 等 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.32</version>
    </dependency>

    <!-- Lombok：减少实体类、DTO、VO 样板代码 -->
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

    <!-- Sa-Token：后台接口认证鉴权，本案例预留使用 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>1.39.0</version>
    </dependency>
</dependencies>
```

如果项目已经统一管理版本，建议把 MyBatis-Plus、Hutool、Knife4j、Sa-Token 的版本放到父工程的 `<dependencyManagement>` 中，业务模块只声明依赖，不直接写版本。

基础配置放在 `src/main/resources/application.yml` 中，后续字典缓存和数据库操作都会基于这份配置运行。

```yaml
server:
  port: 8080

spring:
  application:
    name: dict-demo

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/dict_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3s

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: io.github.atengk.dict.entity
  configuration:
    # 开发阶段输出 SQL，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 使用数据库自增主键
      id-type: auto
      # 逻辑删除字段
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

knife4j:
  enable: true
  setting:
    language: zh_cn
```

### 后端目录结构

本案例采用常规分层结构：Controller 负责接口入参和响应，Service 负责业务逻辑，Mapper 负责数据库访问，Entity 对应数据库表，DTO/VO 负责接口数据模型，Enum 负责枚举治理。

```text
src/main/java/io/github/atengk/dict
├── DictApplication.java
├── common
│   ├── constant
│   │   └── DictCacheConstant.java
│   ├── enums
│   │   ├── BaseEnum.java
│   │   ├── CommonStatusEnum.java
│   │   └── YesNoEnum.java
│   ├── exception
│   │   └── BusinessException.java
│   └── response
│       └── Result.java
├── config
│   ├── MybatisPlusConfig.java
│   └── RedisConfig.java
├── controller
│   ├── SysDictTypeController.java
│   ├── SysDictItemController.java
│   └── SysEnumController.java
├── dto
│   ├── DictItemCreateDTO.java
│   ├── DictItemUpdateDTO.java
│   ├── DictTypeCreateDTO.java
│   ├── DictTypeQueryDTO.java
│   └── DictTypeUpdateDTO.java
├── entity
│   ├── SysDictItem.java
│   └── SysDictType.java
├── mapper
│   ├── SysDictItemMapper.java
│   └── SysDictTypeMapper.java
├── service
│   ├── SysDictItemService.java
│   ├── SysDictTypeService.java
│   └── impl
│       ├── SysDictItemServiceImpl.java
│       └── SysDictTypeServiceImpl.java
└── vo
    ├── DictItemOptionVO.java
    ├── DictTypePageVO.java
    └── EnumOptionVO.java

src/main/resources
├── application.yml
└── mapper
    ├── SysDictItemMapper.xml
    └── SysDictTypeMapper.xml
```

核心包说明如下：

| 包路径             | 说明                       |
| ------------------ | -------------------------- |
| `common.constant`  | 存放 Redis Key 前缀等常量  |
| `common.enums`     | 存放统一枚举接口和通用枚举 |
| `common.exception` | 业务异常                   |
| `common.response`  | 统一响应对象               |
| `config`           | MyBatis-Plus、Redis 等配置 |
| `controller`       | 字典类型、字典项、枚举接口 |
| `dto`              | 新增、修改、查询入参       |
| `entity`           | 数据库实体                 |
| `mapper`           | MyBatis-Plus Mapper        |
| `service`          | 字典核心业务逻辑           |
| `vo`               | 返回给前端的数据结构       |

## 数据库表设计

数据库只设计两张核心表：`sys_dict_type` 和 `sys_dict_item`。字典类型控制分组，字典项控制具体选项。业务表只需要保存字典项的 `item_value`，不要保存 `item_label`。

### 字典类型表

`sys_dict_type` 用于维护字典分组，例如用户状态、性别、客户等级等。`dict_code` 是业务唯一编码，后端和前端都依赖这个字段查询字典项。

```sql
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_code` varchar(100) NOT NULL COMMENT '字典类型编码，如 user_status',
  `dict_name` varchar(100) NOT NULL COMMENT '字典类型名称，如 用户状态',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典类型表';
```

字段设计说明：

| 字段                        | 是否核心 | 说明                                         |
| --------------------------- | -------- | -------------------------------------------- |
| `dict_code`                 | 是       | 字典类型唯一编码，接口查询和业务校验都依赖它 |
| `dict_name`                 | 是       | 后台展示名称                                 |
| `status`                    | 是       | 字典类型禁用后，该类型下字典项默认不对外返回 |
| `sort_order`                | 是       | 后台列表排序使用                             |
| `deleted`                   | 是       | 配合 MyBatis-Plus 逻辑删除                   |
| `created_by` / `updated_by` | 否       | 可结合 Sa-Token 自动填充                     |

`dict_code` 一定要加唯一索引，否则不同人员可能重复创建同一个字典类型，导致前端查询和后端校验结果不可控。

### 字典项表

`sys_dict_item` 用于维护某个字典类型下的具体选项，例如 `user_status` 下的 `enable` 和 `disable`。

```sql
CREATE TABLE `sys_dict_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_code` varchar(100) NOT NULL COMMENT '字典类型编码，对应 sys_dict_type.dict_code',
  `item_value` varchar(100) NOT NULL COMMENT '字典项值，业务表存储该值',
  `item_label` varchar(100) NOT NULL COMMENT '字典项标签，前端展示该值',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `tag_type` varchar(50) DEFAULT NULL COMMENT '前端标签类型，如 success、warning、danger',
  `css_class` varchar(100) DEFAULT NULL COMMENT '前端自定义样式类',
  `extra_json` varchar(1000) DEFAULT NULL COMMENT '扩展JSON配置',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code_item_value` (`dict_code`, `item_value`),
  KEY `idx_dict_code_status_sort` (`dict_code`, `status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典项表';
```

字段设计说明：

| 字段         | 是否核心 | 说明                                       |
| ------------ | -------- | ------------------------------------------ |
| `dict_code`  | 是       | 归属字典类型                               |
| `item_value` | 是       | 业务库存储值，不能随意修改                 |
| `item_label` | 是       | 前端展示文案，可以按需修改                 |
| `status`     | 是       | 禁用后不返回给前端，也不允许新业务继续使用 |
| `sort_order` | 是       | 字典项展示排序                             |
| `tag_type`   | 否       | 适配 Element Plus 的 Tag 类型              |
| `css_class`  | 否       | 前端自定义样式                             |
| `extra_json` | 否       | 扩展配置，例如颜色、图标、默认选中等       |

这里使用联合唯一索引 `uk_dict_code_item_value`，保证同一个字典类型下不能出现重复字典值。不同字典类型之间可以使用相同的字典值，例如多个字典都可以有 `enable`、`disable`。

### 初始化 SQL

下面 SQL 用于初始化常用字典类型和字典项。建议放在 `src/main/resources/sql/init-dict.sql`，开发环境可以直接执行，生产环境应通过 Flyway、Liquibase 或数据库变更平台发布。

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `dict_demo`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `dict_demo`;

-- 删除旧表，开发环境可用；生产环境不要直接执行 DROP
DROP TABLE IF EXISTS `sys_dict_item`;
DROP TABLE IF EXISTS `sys_dict_type`;

-- 字典类型表
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_code` varchar(100) NOT NULL COMMENT '字典类型编码，如 user_status',
  `dict_name` varchar(100) NOT NULL COMMENT '字典类型名称，如 用户状态',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典类型表';

-- 字典项表
CREATE TABLE `sys_dict_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dict_code` varchar(100) NOT NULL COMMENT '字典类型编码，对应 sys_dict_type.dict_code',
  `item_value` varchar(100) NOT NULL COMMENT '字典项值，业务表存储该值',
  `item_label` varchar(100) NOT NULL COMMENT '字典项标签，前端展示该值',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `tag_type` varchar(50) DEFAULT NULL COMMENT '前端标签类型，如 success、warning、danger',
  `css_class` varchar(100) DEFAULT NULL COMMENT '前端自定义样式类',
  `extra_json` varchar(1000) DEFAULT NULL COMMENT '扩展JSON配置',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code_item_value` (`dict_code`, `item_value`),
  KEY `idx_dict_code_status_sort` (`dict_code`, `status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典项表';

-- 初始化字典类型
INSERT INTO `sys_dict_type`
(`dict_code`, `dict_name`, `status`, `remark`, `sort_order`, `created_by`, `updated_by`)
VALUES
('user_status', '用户状态', 1, '用户账号启用和禁用状态', 1, 'system', 'system'),
('gender', '性别', 1, '用户性别选项', 2, 'system', 'system'),
('customer_level', '客户等级', 1, 'CRM 客户等级', 3, 'system', 'system'),
('order_source', '订单来源', 1, '订单来源渠道', 4, 'system', 'system');

-- 初始化用户状态字典项
INSERT INTO `sys_dict_item`
(`dict_code`, `item_value`, `item_label`, `status`, `sort_order`, `tag_type`, `remark`, `created_by`, `updated_by`)
VALUES
('user_status', 'enable', '启用', 1, 1, 'success', '账号允许登录和使用', 'system', 'system'),
('user_status', 'disable', '禁用', 1, 2, 'danger', '账号不允许登录和使用', 'system', 'system');

-- 初始化性别字典项
INSERT INTO `sys_dict_item`
(`dict_code`, `item_value`, `item_label`, `status`, `sort_order`, `tag_type`, `remark`, `created_by`, `updated_by`)
VALUES
('gender', 'male', '男', 1, 1, 'primary', '男性', 'system', 'system'),
('gender', 'female', '女', 1, 2, 'danger', '女性', 'system', 'system'),
('gender', 'unknown', '未知', 1, 3, 'info', '未知或未填写', 'system', 'system');

-- 初始化客户等级字典项
INSERT INTO `sys_dict_item`
(`dict_code`, `item_value`, `item_label`, `status`, `sort_order`, `tag_type`, `remark`, `created_by`, `updated_by`)
VALUES
('customer_level', 'normal', '普通客户', 1, 1, 'info', '普通客户', 'system', 'system'),
('customer_level', 'vip', 'VIP客户', 1, 2, 'warning', '高价值客户', 'system', 'system'),
('customer_level', 'blacklist', '黑名单客户', 1, 3, 'danger', '限制交易客户', 'system', 'system');

-- 初始化订单来源字典项
INSERT INTO `sys_dict_item`
(`dict_code`, `item_value`, `item_label`, `status`, `sort_order`, `tag_type`, `remark`, `created_by`, `updated_by`)
VALUES
('order_source', 'pc', 'PC端', 1, 1, 'primary', 'PC 网站订单', 'system', 'system'),
('order_source', 'app', 'App端', 1, 2, 'success', 'App 下单', 'system', 'system'),
('order_source', 'wechat', '微信小程序', 1, 3, 'success', '微信小程序下单', 'system', 'system'),
('order_source', 'admin', '后台创建', 1, 4, 'warning', '运营后台创建订单', 'system', 'system');
```

执行后可以用下面 SQL 验证初始化结果。

```sql
-- 查询字典类型
SELECT id, dict_code, dict_name, status, sort_order
FROM sys_dict_type
WHERE deleted = 0
ORDER BY sort_order ASC, id ASC;

-- 查询用户状态字典项
SELECT id, dict_code, item_value, item_label, status, sort_order, tag_type
FROM sys_dict_item
WHERE deleted = 0
  AND dict_code = 'user_status'
ORDER BY sort_order ASC, id ASC;
```

预期查询结果中，`user_status` 应包含 `enable` 和 `disable` 两个字典项。后续接口实现时，前端通过 `GET /api/system/dict-items/options/user_status` 即可获取这组数据。

## 后端核心代码实现

本节实现字典模块的核心后端代码，包括实体类、枚举、Mapper、Service、Controller，以及缓存读写逻辑。代码以 `io.github.atengk.dict` 作为基础包名，基于 Spring Boot 3、MyBatis-Plus、Redis、Hutool 实现，技术栈与前文第 23 个业务场景保持一致。

### 字典实体类

字典实体类对应前面设计的 `sys_dict_type` 和 `sys_dict_item` 两张表。这里使用 MyBatis-Plus 注解映射数据库字段，并使用逻辑删除字段 `deleted`。

文件位置：`src/main/java/io/github/atengk/dict/entity/SysDictType.java`

```java
package io.github.atengk.dict.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统字典类型实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("sys_dict_type")
public class SysDictType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 字典类型编码，如 user_status
     */
    private String dictCode;

    /**
     * 字典类型名称，如 用户状态
     */
    private String dictName;

    /**
     * 状态：1启用，0禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序值
     */
    private Integer sortOrder;

    private String createdBy;

    private LocalDateTime createdTime;

    private String updatedBy;

    private LocalDateTime updatedTime;

    /**
     * 逻辑删除：0未删除，1已删除
     */
    @TableLogic
    private Integer deleted;
}
```

文件位置：`src/main/java/io/github/atengk/dict/entity/SysDictItem.java`

```java
package io.github.atengk.dict.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统字典项实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("sys_dict_item")
public class SysDictItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 字典类型编码
     */
    private String dictCode;

    /**
     * 字典项值，业务表存储该值
     */
    private String itemValue;

    /**
     * 字典项标签，前端展示该值
     */
    private String itemLabel;

    /**
     * 状态：1启用，0禁用
     */
    private Integer status;

    /**
     * 排序值
     */
    private Integer sortOrder;

    /**
     * 前端标签类型，如 success、warning、danger
     */
    private String tagType;

    /**
     * 前端自定义样式类
     */
    private String cssClass;

    /**
     * 扩展 JSON 配置
     */
    private String extraJson;

    private String remark;

    private String createdBy;

    private LocalDateTime createdTime;

    private String updatedBy;

    private LocalDateTime updatedTime;

    /**
     * 逻辑删除：0未删除，1已删除
     */
    @TableLogic
    private Integer deleted;
}
```

接口返回给前端时，不建议直接暴露实体类。字典选项使用轻量 VO，只返回前端需要的字段。

文件位置：`src/main/java/io/github/atengk/dict/vo/DictItemOptionVO.java`

```java
package io.github.atengk.dict.vo;

import lombok.Data;

/**
 * 字典选项返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class DictItemOptionVO {

    /**
     * 字典项值
     */
    private String value;

    /**
     * 字典项标签
     */
    private String label;

    /**
     * 标签类型
     */
    private String tagType;

    /**
     * 样式类
     */
    private String cssClass;

    /**
     * 扩展 JSON
     */
    private String extraJson;

    /**
     * 排序值
     */
    private Integer sortOrder;
}
```

文件位置：`src/main/java/io/github/atengk/dict/vo/EnumOptionVO.java`

```java
package io.github.atengk.dict.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 枚举选项返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnumOptionVO {

    /**
     * 枚举值
     */
    private Object value;

    /**
     * 枚举标签
     */
    private String label;
}
```

### 字典枚举定义

本案例使用统一枚举接口 `BaseEnum` 管理后端枚举。所有需要暴露给前端的枚举都实现该接口，后续枚举接口可以统一转换成 `{value,label}` 结构。

文件位置：`src/main/java/io/github/atengk/dict/common/enums/BaseEnum.java`

```java
package io.github.atengk.dict.common.enums;

/**
 * 通用枚举接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface BaseEnum<T> {

    /**
     * 获取枚举值
     *
     * @return 枚举值
     */
    T getValue();

    /**
     * 获取枚举标签
     *
     * @return 枚举标签
     */
    String getLabel();
}
```

通用状态枚举用于字典类型和字典项的启用、禁用状态判断。

文件位置：`src/main/java/io/github/atengk/dict/common/enums/CommonStatusEnum.java`

```java
package io.github.atengk.dict.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum CommonStatusEnum implements BaseEnum<Integer> {

    ENABLE(1, "启用"),
    DISABLE(0, "禁用");

    private final Integer value;

    private final String label;

    /**
     * 判断是否启用
     *
     * @param status 状态值
     * @return true 启用，false 非启用
     */
    public static boolean isEnable(Integer status) {
        return ENABLE.getValue().equals(status);
    }
}
```

是否枚举用于后续扩展，例如是否默认、是否系统内置、是否允许删除等字段。

文件位置：`src/main/java/io/github/atengk/dict/common/enums/YesNoEnum.java`

```java
package io.github.atengk.dict.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 是否枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum YesNoEnum implements BaseEnum<Integer> {

    YES(1, "是"),
    NO(0, "否");

    private final Integer value;

    private final String label;
}
```

公共异常和统一响应对象用于后续 Service、Controller 返回结果。

文件位置：`src/main/java/io/github/atengk/dict/common/exception/BusinessException.java`

```java
package io.github.atengk.dict.common.exception;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
```

文件位置：`src/main/java/io/github/atengk/dict/common/response/Result.java`

```java
package io.github.atengk.dict.common.response;

import lombok.Data;

/**
 * 统一接口响应
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @return 响应结果
     */
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 成功响应
     *
     * @return 响应结果
     */
    public static Result<Void> ok() {
        Result<Void> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    /**
     * 失败响应
     *
     * @param message 错误信息
     * @return 响应结果
     */
    public static Result<Void> fail(String message) {
        Result<Void> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
}
```

### Mapper 持久层

Mapper 层使用 MyBatis-Plus 的 `BaseMapper` 即可完成基础 CRUD。复杂查询可以后续放到 XML 中扩展，本案例暂时不需要写复杂 SQL。

文件位置：`src/main/java/io/github/atengk/dict/mapper/SysDictTypeMapper.java`

```java
package io.github.atengk.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.dict.entity.SysDictType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统字典类型 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {
}
```

文件位置：`src/main/java/io/github/atengk/dict/mapper/SysDictItemMapper.java`

```java
package io.github.atengk.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.dict.entity.SysDictItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统字典项 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {
}
```

如果项目配置了 `mapper-locations`，可以保留空 XML，方便后续扩展自定义 SQL。

文件位置：`src/main/resources/mapper/SysDictTypeMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="io.github.atengk.dict.mapper.SysDictTypeMapper">
    <!-- 当前案例使用 MyBatis-Plus BaseMapper，复杂 SQL 后续可在此扩展 -->
</mapper>
```

文件位置：`src/main/resources/mapper/SysDictItemMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="io.github.atengk.dict.mapper.SysDictItemMapper">
    <!-- 当前案例使用 MyBatis-Plus BaseMapper，复杂 SQL 后续可在此扩展 -->
</mapper>
```

### Service 业务层

Service 层负责字典维护、字典查询、缓存读写、字典值校验、字典标签回显。这里把核心查询入口放在 `SysDictItemService` 中，因为前端和业务系统最常用的是“根据字典类型查询字典项”。

先定义 Redis Key 常量，避免 Key 分散在业务代码里。

文件位置：`src/main/java/io/github/atengk/dict/common/constant/DictCacheConstant.java`

```java
package io.github.atengk.dict.common.constant;

/**
 * 字典缓存常量
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class DictCacheConstant {

    /**
     * 字典项列表缓存 Key 前缀
     */
    public static final String DICT_ITEM_LIST_KEY_PREFIX = "sys:dict:item:list:";

    /**
     * 字典值标签缓存 Key 前缀
     */
    public static final String DICT_ITEM_LABEL_KEY_PREFIX = "sys:dict:item:label:";

    private DictCacheConstant() {
    }
}
```

字典类型 Service 主要处理类型新增、修改、删除，并在变更后刷新对应字典项缓存。

文件位置：`src/main/java/io/github/atengk/dict/service/SysDictTypeService.java`

```java
package io.github.atengk.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.dict.entity.SysDictType;

/**
 * 系统字典类型 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SysDictTypeService extends IService<SysDictType> {

    /**
     * 新增字典类型
     *
     * @param dictType 字典类型
     */
    void createDictType(SysDictType dictType);

    /**
     * 修改字典类型
     *
     * @param dictType 字典类型
     */
    void updateDictType(SysDictType dictType);

    /**
     * 删除字典类型
     *
     * @param id 字典类型ID
     */
    void deleteDictType(Long id);
}
```

文件位置：`src/main/java/io/github/atengk/dict/service/SysDictItemService.java`

```java
package io.github.atengk.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.dict.entity.SysDictItem;
import io.github.atengk.dict.vo.DictItemOptionVO;

import java.util.List;

/**
 * 系统字典项 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SysDictItemService extends IService<SysDictItem> {

    /**
     * 新增字典项
     *
     * @param dictItem 字典项
     */
    void createDictItem(SysDictItem dictItem);

    /**
     * 修改字典项
     *
     * @param dictItem 字典项
     */
    void updateDictItem(SysDictItem dictItem);

    /**
     * 删除字典项
     *
     * @param id 字典项ID
     */
    void deleteDictItem(Long id);

    /**
     * 根据字典类型查询启用字典选项
     *
     * @param dictCode 字典类型编码
     * @return 字典选项列表
     */
    List<DictItemOptionVO> listOptions(String dictCode);

    /**
     * 校验字典值是否启用且合法
     *
     * @param dictCode 字典类型编码
     * @param itemValue 字典项值
     * @return true 合法，false 非法
     */
    boolean validateValue(String dictCode, String itemValue);

    /**
     * 根据字典值查询标签
     *
     * @param dictCode 字典类型编码
     * @param itemValue 字典项值
     * @return 字典标签
     */
    String getLabel(String dictCode, String itemValue);

    /**
     * 清理某个字典类型的缓存
     *
     * @param dictCode 字典类型编码
     */
    void clearCache(String dictCode);
}
```

字典类型实现类中，新增和修改都做唯一性校验；删除字典类型时，同时删除该类型下的字典项缓存。实际生产中，如果字典类型下存在字典项，通常不建议直接删除，可以改成“禁止删除”或“仅允许禁用”。

文件位置：`src/main/java/io/github/atengk/dict/service/impl/SysDictTypeServiceImpl.java`

```java
package io.github.atengk.dict.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.dict.common.enums.CommonStatusEnum;
import io.github.atengk.dict.common.exception.BusinessException;
import io.github.atengk.dict.entity.SysDictType;
import io.github.atengk.dict.mapper.SysDictTypeMapper;
import io.github.atengk.dict.service.SysDictItemService;
import io.github.atengk.dict.service.SysDictTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 系统字典类型 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType>
        implements SysDictTypeService {

    private final SysDictItemService sysDictItemService;

    /**
     * 新增字典类型
     *
     * @param dictType 字典类型
     */
    @Override
    public void createDictType(SysDictType dictType) {
        checkDictType(dictType, null);

        dictType.setStatus(dictType.getStatus() == null ? CommonStatusEnum.ENABLE.getValue() : dictType.getStatus());
        dictType.setSortOrder(dictType.getSortOrder() == null ? 0 : dictType.getSortOrder());
        dictType.setCreatedTime(LocalDateTime.now());
        dictType.setUpdatedTime(LocalDateTime.now());
        dictType.setDeleted(0);

        save(dictType);
        log.info("新增字典类型成功，dictCode={}", dictType.getDictCode());
    }

    /**
     * 修改字典类型
     *
     * @param dictType 字典类型
     */
    @Override
    public void updateDictType(SysDictType dictType) {
        if (dictType.getId() == null) {
            throw new BusinessException("字典类型ID不能为空");
        }

        SysDictType old = getById(dictType.getId());
        if (old == null) {
            throw new BusinessException("字典类型不存在");
        }

        checkDictType(dictType, dictType.getId());
        dictType.setUpdatedTime(LocalDateTime.now());

        updateById(dictType);
        sysDictItemService.clearCache(dictType.getDictCode());
        log.info("修改字典类型成功，dictCode={}", dictType.getDictCode());
    }

    /**
     * 删除字典类型
     *
     * @param id 字典类型ID
     */
    @Override
    public void deleteDictType(Long id) {
        SysDictType dictType = getById(id);
        if (dictType == null) {
            throw new BusinessException("字典类型不存在");
        }

        removeById(id);
        sysDictItemService.clearCache(dictType.getDictCode());
        log.info("删除字典类型成功，dictCode={}", dictType.getDictCode());
    }

    /**
     * 校验字典类型
     *
     * @param dictType 字典类型
     * @param excludeId 排除ID
     */
    private void checkDictType(SysDictType dictType, Long excludeId) {
        if (StrUtil.isBlank(dictType.getDictCode())) {
            throw new BusinessException("字典类型编码不能为空");
        }
        if (StrUtil.isBlank(dictType.getDictName())) {
            throw new BusinessException("字典类型名称不能为空");
        }

        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictCode, dictType.getDictCode());

        if (excludeId != null) {
            wrapper.ne(SysDictType::getId, excludeId);
        }

        long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("字典类型编码已存在");
        }
    }
}
```

字典项实现类是本模块的核心。它会优先从 Redis 读取字典选项；缓存不存在时查询数据库，再写入 Redis。校验和标签回显都基于缓存列表完成。

文件位置：`src/main/java/io/github/atengk/dict/service/impl/SysDictItemServiceImpl.java`

```java
package io.github.atengk.dict.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.dict.common.constant.DictCacheConstant;
import io.github.atengk.dict.common.enums.CommonStatusEnum;
import io.github.atengk.dict.common.exception.BusinessException;
import io.github.atengk.dict.entity.SysDictItem;
import io.github.atengk.dict.entity.SysDictType;
import io.github.atengk.dict.mapper.SysDictItemMapper;
import io.github.atengk.dict.service.SysDictItemService;
import io.github.atengk.dict.service.SysDictTypeService;
import io.github.atengk.dict.vo.DictItemOptionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import cn.hutool.json.JSONUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 系统字典项 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictItemServiceImpl extends ServiceImpl<SysDictItemMapper, SysDictItem>
        implements SysDictItemService {

    private static final Duration DICT_CACHE_TTL = Duration.ofHours(6);

    private final StringRedisTemplate stringRedisTemplate;

    private final SysDictTypeService sysDictTypeService;

    /**
     * 新增字典项
     *
     * @param dictItem 字典项
     */
    @Override
    public void createDictItem(SysDictItem dictItem) {
        checkDictItem(dictItem, null);

        dictItem.setStatus(dictItem.getStatus() == null ? CommonStatusEnum.ENABLE.getValue() : dictItem.getStatus());
        dictItem.setSortOrder(dictItem.getSortOrder() == null ? 0 : dictItem.getSortOrder());
        dictItem.setCreatedTime(LocalDateTime.now());
        dictItem.setUpdatedTime(LocalDateTime.now());
        dictItem.setDeleted(0);

        save(dictItem);
        clearCache(dictItem.getDictCode());
        log.info("新增字典项成功，dictCode={}，itemValue={}", dictItem.getDictCode(), dictItem.getItemValue());
    }

    /**
     * 修改字典项
     *
     * @param dictItem 字典项
     */
    @Override
    public void updateDictItem(SysDictItem dictItem) {
        if (dictItem.getId() == null) {
            throw new BusinessException("字典项ID不能为空");
        }

        SysDictItem old = getById(dictItem.getId());
        if (old == null) {
            throw new BusinessException("字典项不存在");
        }

        checkDictItem(dictItem, dictItem.getId());
        dictItem.setUpdatedTime(LocalDateTime.now());

        updateById(dictItem);
        clearCache(old.getDictCode());
        clearCache(dictItem.getDictCode());
        log.info("修改字典项成功，dictCode={}，itemValue={}", dictItem.getDictCode(), dictItem.getItemValue());
    }

    /**
     * 删除字典项
     *
     * @param id 字典项ID
     */
    @Override
    public void deleteDictItem(Long id) {
        SysDictItem dictItem = getById(id);
        if (dictItem == null) {
            throw new BusinessException("字典项不存在");
        }

        removeById(id);
        clearCache(dictItem.getDictCode());
        log.info("删除字典项成功，dictCode={}，itemValue={}", dictItem.getDictCode(), dictItem.getItemValue());
    }

    /**
     * 根据字典类型查询启用字典选项
     *
     * @param dictCode 字典类型编码
     * @return 字典选项列表
     */
    @Override
    public List<DictItemOptionVO> listOptions(String dictCode) {
        if (StrUtil.isBlank(dictCode)) {
            throw new BusinessException("字典类型编码不能为空");
        }

        String cacheKey = buildListKey(dictCode);
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);

        if (StrUtil.isNotBlank(cacheValue)) {
            return JSONUtil.toList(cacheValue, DictItemOptionVO.class);
        }

        List<SysDictItem> itemList = list(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictCode, dictCode)
                .eq(SysDictItem::getStatus, CommonStatusEnum.ENABLE.getValue())
                .orderByAsc(SysDictItem::getSortOrder)
                .orderByAsc(SysDictItem::getId));

        List<DictItemOptionVO> optionList = itemList.stream()
                .map(this::toOptionVO)
                .toList();

        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(optionList), DICT_CACHE_TTL);
        log.info("加载字典选项到缓存，dictCode={}，数量={}", dictCode, optionList.size());

        return optionList;
    }

    /**
     * 校验字典值是否启用且合法
     *
     * @param dictCode 字典类型编码
     * @param itemValue 字典项值
     * @return true 合法，false 非法
     */
    @Override
    public boolean validateValue(String dictCode, String itemValue) {
        if (StrUtil.hasBlank(dictCode, itemValue)) {
            return false;
        }

        List<DictItemOptionVO> optionList = listOptions(dictCode);
        if (CollUtil.isEmpty(optionList)) {
            return false;
        }

        return optionList.stream()
                .anyMatch(item -> Objects.equals(item.getValue(), itemValue));
    }

    /**
     * 根据字典值查询标签
     *
     * @param dictCode 字典类型编码
     * @param itemValue 字典项值
     * @return 字典标签
     */
    @Override
    public String getLabel(String dictCode, String itemValue) {
        if (StrUtil.hasBlank(dictCode, itemValue)) {
            return StrUtil.EMPTY;
        }

        return listOptions(dictCode).stream()
                .filter(item -> Objects.equals(item.getValue(), itemValue))
                .map(DictItemOptionVO::getLabel)
                .findFirst()
                .orElse(StrUtil.EMPTY);
    }

    /**
     * 清理某个字典类型的缓存
     *
     * @param dictCode 字典类型编码
     */
    @Override
    public void clearCache(String dictCode) {
        if (StrUtil.isBlank(dictCode)) {
            return;
        }

        String listKey = buildListKey(dictCode);
        stringRedisTemplate.delete(listKey);
        log.info("清理字典缓存成功，dictCode={}", dictCode);
    }

    /**
     * 校验字典项
     *
     * @param dictItem 字典项
     * @param excludeId 排除ID
     */
    private void checkDictItem(SysDictItem dictItem, Long excludeId) {
        if (StrUtil.isBlank(dictItem.getDictCode())) {
            throw new BusinessException("字典类型编码不能为空");
        }
        if (StrUtil.isBlank(dictItem.getItemValue())) {
            throw new BusinessException("字典项值不能为空");
        }
        if (StrUtil.isBlank(dictItem.getItemLabel())) {
            throw new BusinessException("字典项标签不能为空");
        }

        SysDictType dictType = sysDictTypeService.getOne(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictCode, dictItem.getDictCode())
                .last("LIMIT 1"));

        if (dictType == null) {
            throw new BusinessException("字典类型不存在");
        }
        if (!CommonStatusEnum.isEnable(dictType.getStatus())) {
            throw new BusinessException("字典类型已禁用");
        }

        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictCode, dictItem.getDictCode())
                .eq(SysDictItem::getItemValue, dictItem.getItemValue());

        if (excludeId != null) {
            wrapper.ne(SysDictItem::getId, excludeId);
        }

        long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("同一字典类型下字典项值已存在");
        }
    }

    /**
     * 转换字典选项
     *
     * @param item 字典项
     * @return 字典选项
     */
    private DictItemOptionVO toOptionVO(SysDictItem item) {
        DictItemOptionVO vo = new DictItemOptionVO();
        vo.setValue(item.getItemValue());
        vo.setLabel(item.getItemLabel());
        vo.setTagType(item.getTagType());
        vo.setCssClass(item.getCssClass());
        vo.setExtraJson(item.getExtraJson());
        vo.setSortOrder(item.getSortOrder());
        return vo;
    }

    /**
     * 构建字典列表缓存 Key
     *
     * @param dictCode 字典类型编码
     * @return 缓存 Key
     */
    private String buildListKey(String dictCode) {
        return DictCacheConstant.DICT_ITEM_LIST_KEY_PREFIX + dictCode;
    }
}
```

### Controller 接口层

Controller 提供字典类型维护、字典项维护、字典查询和枚举查询接口。为了让代码更精简，这里新增、修改直接接收实体类；生产项目中建议改成专门的 CreateDTO 和 UpdateDTO。

字典类型接口用于后台维护字典分组。

文件位置：`src/main/java/io/github/atengk/dict/controller/SysDictTypeController.java`

```java
package io.github.atengk.dict.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.dict.common.response.Result;
import io.github.atengk.dict.entity.SysDictType;
import io.github.atengk.dict.service.SysDictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统字典类型接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/dict-types")
public class SysDictTypeController {

    private final SysDictTypeService sysDictTypeService;

    /**
     * 查询字典类型列表
     *
     * @return 字典类型列表
     */
    @GetMapping
    public Result<List<SysDictType>> list() {
        List<SysDictType> list = sysDictTypeService.list(new LambdaQueryWrapper<SysDictType>()
                .orderByAsc(SysDictType::getSortOrder)
                .orderByDesc(SysDictType::getId));
        return Result.ok(list);
    }

    /**
     * 新增字典类型
     *
     * @param dictType 字典类型
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> create(@RequestBody SysDictType dictType) {
        sysDictTypeService.createDictType(dictType);
        return Result.ok();
    }

    /**
     * 修改字典类型
     *
     * @param dictType 字典类型
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody SysDictType dictType) {
        sysDictTypeService.updateDictType(dictType);
        return Result.ok();
    }

    /**
     * 删除字典类型
     *
     * @param id 字典类型ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysDictTypeService.deleteDictType(id);
        return Result.ok();
    }
}
```

字典项接口用于维护具体选项，并提供前端最常用的字典选项查询接口。

文件位置：`src/main/java/io/github/atengk/dict/controller/SysDictItemController.java`

```java
package io.github.atengk.dict.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.dict.common.response.Result;
import io.github.atengk.dict.entity.SysDictItem;
import io.github.atengk.dict.service.SysDictItemService;
import io.github.atengk.dict.vo.DictItemOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统字典项接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/dict-items")
public class SysDictItemController {

    private final SysDictItemService sysDictItemService;

    /**
     * 根据字典类型查询字典项列表
     *
     * @param dictCode 字典类型编码
     * @return 字典项列表
     */
    @GetMapping
    public Result<List<SysDictItem>> list(@RequestParam String dictCode) {
        List<SysDictItem> list = sysDictItemService.list(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictCode, dictCode)
                .orderByAsc(SysDictItem::getSortOrder)
                .orderByAsc(SysDictItem::getId));
        return Result.ok(list);
    }

    /**
     * 查询前端字典选项
     *
     * @param dictCode 字典类型编码
     * @return 字典选项
     */
    @GetMapping("/options/{dictCode}")
    public Result<List<DictItemOptionVO>> options(@PathVariable String dictCode) {
        return Result.ok(sysDictItemService.listOptions(dictCode));
    }

    /**
     * 校验字典值是否合法
     *
     * @param dictCode 字典类型编码
     * @param itemValue 字典项值
     * @return 校验结果
     */
    @GetMapping("/validate")
    public Result<Boolean> validate(@RequestParam String dictCode, @RequestParam String itemValue) {
        return Result.ok(sysDictItemService.validateValue(dictCode, itemValue));
    }

    /**
     * 获取字典标签
     *
     * @param dictCode 字典类型编码
     * @param itemValue 字典项值
     * @return 字典标签
     */
    @GetMapping("/label")
    public Result<String> label(@RequestParam String dictCode, @RequestParam String itemValue) {
        return Result.ok(sysDictItemService.getLabel(dictCode, itemValue));
    }

    /**
     * 新增字典项
     *
     * @param dictItem 字典项
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> create(@RequestBody SysDictItem dictItem) {
        sysDictItemService.createDictItem(dictItem);
        return Result.ok();
    }

    /**
     * 修改字典项
     *
     * @param dictItem 字典项
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody SysDictItem dictItem) {
        sysDictItemService.updateDictItem(dictItem);
        return Result.ok();
    }

    /**
     * 删除字典项
     *
     * @param id 字典项ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysDictItemService.deleteDictItem(id);
        return Result.ok();
    }
}
```

枚举接口用于把后端枚举转换成前端可用的选项。这里通过 `enumName` 映射具体枚举，避免前端知道 Java 类全路径。

文件位置：`src/main/java/io/github/atengk/dict/controller/SysEnumController.java`

```java
package io.github.atengk.dict.controller;

import io.github.atengk.dict.common.enums.BaseEnum;
import io.github.atengk.dict.common.enums.CommonStatusEnum;
import io.github.atengk.dict.common.enums.YesNoEnum;
import io.github.atengk.dict.common.exception.BusinessException;
import io.github.atengk.dict.common.response.Result;
import io.github.atengk.dict.vo.EnumOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 系统枚举接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/enums")
public class SysEnumController {

    /**
     * 查询枚举选项
     *
     * @param enumName 枚举名称
     * @return 枚举选项
     */
    @GetMapping("/{enumName}")
    public Result<List<EnumOptionVO>> options(@PathVariable String enumName) {
        Class<? extends Enum<?>> enumClass = resolveEnumClass(enumName);
        return Result.ok(toOptions(enumClass));
    }

    /**
     * 根据枚举名称解析枚举类
     *
     * @param enumName 枚举名称
     * @return 枚举类
     */
    private Class<? extends Enum<?>> resolveEnumClass(String enumName) {
        return switch (enumName) {
            case "commonStatus" -> CommonStatusEnum.class;
            case "yesNo" -> YesNoEnum.class;
            default -> throw new BusinessException("枚举不存在：" + enumName);
        };
    }

    /**
     * 转换枚举选项
     *
     * @param enumClass 枚举类
     * @return 枚举选项
     */
    private List<EnumOptionVO> toOptions(Class<? extends Enum<?>> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(item -> {
                    BaseEnum<?> baseEnum = (BaseEnum<?>) item;
                    return new EnumOptionVO(baseEnum.getValue(), baseEnum.getLabel());
                })
                .toList();
    }
}
```

建议再补一个全局异常处理器，避免业务异常直接返回 500 堆栈。

文件位置：`src/main/java/io/github/atengk/dict/common/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.dict.common.exception;

import io.github.atengk.dict.common.response.Result;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 处理业务异常
     *
     * @param exception 业务异常
     * @return 响应结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        log.warn("业务处理失败：{}", exception.getMessage());
        return Result.fail(exception.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return 响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统处理异常", exception);
        return Result.fail("系统异常，请联系管理员");
    }
}
```

## 字典缓存设计

字典数据属于典型的“读多写少”数据。前端页面加载、表格回显、业务参数校验都会频繁读取字典项，如果每次都查询数据库，会产生大量重复 SQL。因此本案例使用 Redis 缓存字典项列表，字典变更时删除缓存，下次查询时重新加载。

### Redis Key 设计

缓存 Key 按字典类型编码隔离，每个 `dictCode` 对应一份字典项列表。

```text
sys:dict:item:list:{dictCode}
```

示例：

```text
sys:dict:item:list:user_status
sys:dict:item:list:gender
sys:dict:item:list:customer_level
sys:dict:item:list:order_source
```

Key 设计规则：

| Key          | 说明           |
| ------------ | -------------- |
| `sys`        | 系统基础模块   |
| `dict`       | 字典业务       |
| `item`       | 字典项         |
| `list`       | 缓存内容为列表 |
| `{dictCode}` | 字典类型编码   |

缓存 Value 使用 JSON 数组，结构如下：

```json
[
  {
    "value": "enable",
    "label": "启用",
    "tagType": "success",
    "cssClass": null,
    "extraJson": null,
    "sortOrder": 1
  },
  {
    "value": "disable",
    "label": "禁用",
    "tagType": "danger",
    "cssClass": null,
    "extraJson": null,
    "sortOrder": 2
  }
]
```

本案例没有单独维护 `sys:dict:item:label:{dictCode}:{itemValue}` 这种标签缓存。原因是字典项列表通常不大，校验和回显可以直接复用列表缓存，减少缓存 Key 数量和一致性维护成本。

### 字典列表缓存

字典列表缓存采用 Cache Aside 模式，也就是先读缓存，缓存没有再查数据库，然后写入缓存。

核心代码已经在 `SysDictItemServiceImpl#listOptions` 中实现：

```java
@Override
public List<DictItemOptionVO> listOptions(String dictCode) {
    if (StrUtil.isBlank(dictCode)) {
        throw new BusinessException("字典类型编码不能为空");
    }

    String cacheKey = buildListKey(dictCode);
    String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);

    if (StrUtil.isNotBlank(cacheValue)) {
        return JSONUtil.toList(cacheValue, DictItemOptionVO.class);
    }

    List<SysDictItem> itemList = list(new LambdaQueryWrapper<SysDictItem>()
            .eq(SysDictItem::getDictCode, dictCode)
            .eq(SysDictItem::getStatus, CommonStatusEnum.ENABLE.getValue())
            .orderByAsc(SysDictItem::getSortOrder)
            .orderByAsc(SysDictItem::getId));

    List<DictItemOptionVO> optionList = itemList.stream()
            .map(this::toOptionVO)
            .toList();

    stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(optionList), DICT_CACHE_TTL);
    log.info("加载字典选项到缓存，dictCode={}，数量={}", dictCode, optionList.size());

    return optionList;
}
```

第一次查询 `user_status` 时，会执行数据库查询并写入 Redis。后续再查询相同 `dictCode` 时，直接从 Redis 返回。

接口调用示例：

```bash
curl -X GET "http://localhost:8080/api/system/dict-items/options/user_status"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "value": "enable",
      "label": "启用",
      "tagType": "success",
      "cssClass": null,
      "extraJson": null,
      "sortOrder": 1
    },
    {
      "value": "disable",
      "label": "禁用",
      "tagType": "danger",
      "cssClass": null,
      "extraJson": null,
      "sortOrder": 2
    }
  ]
}
```

### 字典值校验缓存

业务保存数据时，不应该直接相信前端传入的字典值。例如新增用户时传入 `status=xxx`，后端必须校验 `xxx` 是否属于 `user_status` 且处于启用状态。

本案例复用字典列表缓存完成校验：

```java
@Override
public boolean validateValue(String dictCode, String itemValue) {
    if (StrUtil.hasBlank(dictCode, itemValue)) {
        return false;
    }

    List<DictItemOptionVO> optionList = listOptions(dictCode);
    if (CollUtil.isEmpty(optionList)) {
        return false;
    }

    return optionList.stream()
            .anyMatch(item -> Objects.equals(item.getValue(), itemValue));
}
```

接口调用示例：

```bash
curl -X GET "http://localhost:8080/api/system/dict-items/validate?dictCode=user_status&itemValue=enable"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

业务中可以这样使用：

```java
if (!sysDictItemService.validateValue("user_status", userStatus)) {
    throw new BusinessException("用户状态不合法");
}
```

字典标签回显同样复用列表缓存：

```java
String statusLabel = sysDictItemService.getLabel("user_status", "enable");
```

返回结果：

```text
启用
```

这种设计的优点是简单、稳定、容易维护。对于大部分后台系统，一个字典类型下的字典项数量通常在几十条以内，列表缓存完全足够。

### 字典变更后刷新缓存

字典缓存刷新采用“删除缓存”策略，而不是“更新缓存”策略。原因是删除缓存更简单，避免数据库更新成功但缓存更新失败导致脏数据。

涉及缓存刷新的操作包括：

| 操作         | 刷新策略                                     |
| ------------ | -------------------------------------------- |
| 新增字典项   | 删除对应 `dictCode` 的列表缓存               |
| 修改字典项   | 删除旧 `dictCode` 和新 `dictCode` 的列表缓存 |
| 删除字典项   | 删除对应 `dictCode` 的列表缓存               |
| 修改字典类型 | 删除对应 `dictCode` 的列表缓存               |
| 删除字典类型 | 删除对应 `dictCode` 的列表缓存               |

核心代码如下：

```java
@Override
public void clearCache(String dictCode) {
    if (StrUtil.isBlank(dictCode)) {
        return;
    }

    String listKey = buildListKey(dictCode);
    stringRedisTemplate.delete(listKey);
    log.info("清理字典缓存成功，dictCode={}", dictCode);
}
```

新增字典项后刷新缓存：

```java
@Override
public void createDictItem(SysDictItem dictItem) {
    checkDictItem(dictItem, null);

    dictItem.setStatus(dictItem.getStatus() == null ? CommonStatusEnum.ENABLE.getValue() : dictItem.getStatus());
    dictItem.setSortOrder(dictItem.getSortOrder() == null ? 0 : dictItem.getSortOrder());
    dictItem.setCreatedTime(LocalDateTime.now());
    dictItem.setUpdatedTime(LocalDateTime.now());
    dictItem.setDeleted(0);

    save(dictItem);
    clearCache(dictItem.getDictCode());
    log.info("新增字典项成功，dictCode={}，itemValue={}", dictItem.getDictCode(), dictItem.getItemValue());
}
```

修改字典项后刷新缓存：

```java
@Override
public void updateDictItem(SysDictItem dictItem) {
    if (dictItem.getId() == null) {
        throw new BusinessException("字典项ID不能为空");
    }

    SysDictItem old = getById(dictItem.getId());
    if (old == null) {
        throw new BusinessException("字典项不存在");
    }

    checkDictItem(dictItem, dictItem.getId());
    dictItem.setUpdatedTime(LocalDateTime.now());

    updateById(dictItem);
    clearCache(old.getDictCode());
    clearCache(dictItem.getDictCode());
    log.info("修改字典项成功，dictCode={}，itemValue={}", dictItem.getDictCode(), dictItem.getItemValue());
}
```

验证缓存刷新可以按下面步骤操作：

```bash
# 1. 第一次查询，数据库加载并写入 Redis
curl -X GET "http://localhost:8080/api/system/dict-items/options/user_status"

# 2. 修改字典项标签
curl -X PUT "http://localhost:8080/api/system/dict-items" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "dictCode": "user_status",
    "itemValue": "enable",
    "itemLabel": "正常",
    "status": 1,
    "sortOrder": 1,
    "tagType": "success"
  }'

# 3. 再次查询，缓存已被删除，会重新从数据库加载
curl -X GET "http://localhost:8080/api/system/dict-items/options/user_status"
```

如果第二次查询返回的 `enable` 标签从 `启用` 变成 `正常`，说明缓存刷新生效。

## 枚举治理实现

枚举治理用于解决“后端枚举值、前端下拉选项、业务状态判断”之间不一致的问题。字典数据适合后台维护，枚举适合强业务约束；本节把后端枚举统一抽象成 `{value,label}` 结构，并提供统一查询和校验接口，避免前端硬编码。该实现仍然围绕第 23 个“字典数据与枚举治理”场景展开。

### 统一枚举接口

所有需要暴露给前端或参与业务校验的枚举，都实现 `BaseEnum<T>`。这样后续可以统一转换、统一校验、统一输出。

文件位置：`src/main/java/io/github/atengk/dict/common/enums/BaseEnum.java`

```java
package io.github.atengk.dict.common.enums;

/**
 * 通用枚举接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface BaseEnum<T> {

    /**
     * 获取枚举值
     *
     * @return 枚举值
     */
    T getValue();

    /**
     * 获取枚举标签
     *
     * @return 枚举标签
     */
    String getLabel();
}
```

下面以订单状态枚举为例。订单状态属于强业务约束，不建议放入数据库字典表，因为它会影响状态流转、取消、支付、履约等核心逻辑。

文件位置：`src/main/java/io/github/atengk/dict/common/enums/OrderStatusEnum.java`

```java
package io.github.atengk.dict.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 订单状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum implements BaseEnum<String> {

    WAIT_PAY("WAIT_PAY", "待支付"),
    PAID("PAID", "已支付"),
    DELIVERED("DELIVERED", "已发货"),
    FINISHED("FINISHED", "已完成"),
    CANCELED("CANCELED", "已取消");

    private final String value;

    private final String label;

    /**
     * 校验枚举值是否合法
     *
     * @param value 枚举值
     * @return true 合法，false 非法
     */
    public static boolean isValid(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(item -> StrUtil.equals(item.getValue(), value));
    }

    /**
     * 根据枚举值获取枚举
     *
     * @param value 枚举值
     * @return 订单状态枚举
     */
    public static OrderStatusEnum of(String value) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equals(item.getValue(), value))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断订单是否允许取消
     *
     * @param value 订单状态值
     * @return true 允许取消，false 不允许取消
     */
    public static boolean allowCancel(String value) {
        return WAIT_PAY.getValue().equals(value) || PAID.getValue().equals(value);
    }
}
```

再补一个支付状态枚举，用于展示多个枚举统一治理的效果。

文件位置：`src/main/java/io/github/atengk/dict/common/enums/PayStatusEnum.java`

```java
package io.github.atengk.dict.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 支付状态枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum PayStatusEnum implements BaseEnum<String> {

    UNPAID("UNPAID", "未支付"),
    PAYING("PAYING", "支付中"),
    SUCCESS("SUCCESS", "支付成功"),
    FAILED("FAILED", "支付失败"),
    CLOSED("CLOSED", "已关闭");

    private final String value;

    private final String label;

    /**
     * 校验枚举值是否合法
     *
     * @param value 枚举值
     * @return true 合法，false 非法
     */
    public static boolean isValid(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(item -> StrUtil.equals(item.getValue(), value));
    }
}
```

### 枚举转字典选项

为了让前端可以像使用字典一样使用枚举，后端需要把枚举统一转换成 `{value,label}` 结构。这里提供一个通用工具类，所有实现了 `BaseEnum` 的枚举都可以直接转换。

文件位置：`src/main/java/io/github/atengk/dict/common/utils/EnumOptionUtil.java`

```java
package io.github.atengk.dict.common.utils;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.dict.common.enums.BaseEnum;
import io.github.atengk.dict.vo.EnumOptionVO;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 枚举选项工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class EnumOptionUtil {

    private EnumOptionUtil() {
    }

    /**
     * 枚举转前端选项
     *
     * @param enumClass 枚举类型
     * @return 枚举选项列表
     */
    public static List<EnumOptionVO> toOptions(Class<? extends Enum<?>> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(item -> {
                    BaseEnum<?> baseEnum = (BaseEnum<?>) item;
                    return new EnumOptionVO(baseEnum.getValue(), baseEnum.getLabel());
                })
                .toList();
    }

    /**
     * 校验枚举值是否合法
     *
     * @param enumClass 枚举类型
     * @param value 枚举值
     * @return true 合法，false 非法
     */
    public static boolean isValid(Class<? extends Enum<?>> enumClass, String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }

        return Arrays.stream(enumClass.getEnumConstants())
                .map(item -> (BaseEnum<?>) item)
                .anyMatch(item -> Objects.equals(String.valueOf(item.getValue()), value));
    }

    /**
     * 根据枚举值获取标签
     *
     * @param enumClass 枚举类型
     * @param value 枚举值
     * @return 枚举标签
     */
    public static String getLabel(Class<? extends Enum<?>> enumClass, String value) {
        if (StrUtil.isBlank(value)) {
            return StrUtil.EMPTY;
        }

        return Arrays.stream(enumClass.getEnumConstants())
                .map(item -> (BaseEnum<?>) item)
                .filter(item -> Objects.equals(String.valueOf(item.getValue()), value))
                .map(BaseEnum::getLabel)
                .findFirst()
                .orElse(StrUtil.EMPTY);
    }
}
```

枚举选项返回对象沿用前面定义的 `EnumOptionVO`。

文件位置：`src/main/java/io/github/atengk/dict/vo/EnumOptionVO.java`

```java
package io.github.atengk.dict.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 枚举选项返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnumOptionVO {

    /**
     * 枚举值
     */
    private Object value;

    /**
     * 枚举标签
     */
    private String label;
}
```

为了避免前端传 Java 类全路径，后端维护一个枚举注册表。前端只需要传 `orderStatus`、`payStatus` 这类业务名称。

文件位置：`src/main/java/io/github/atengk/dict/common/enums/EnumRegistry.java`

```java
package io.github.atengk.dict.common.enums;

import io.github.atengk.dict.common.exception.BusinessException;

import java.util.Map;

/**
 * 枚举注册表
 *
 * @author Ateng
 * @since 2026-05-15
 */
public class EnumRegistry {

    private static final Map<String, Class<? extends Enum<?>>> ENUM_MAP = Map.of(
            "commonStatus", CommonStatusEnum.class,
            "yesNo", YesNoEnum.class,
            "orderStatus", OrderStatusEnum.class,
            "payStatus", PayStatusEnum.class
    );

    private EnumRegistry() {
    }

    /**
     * 根据枚举名称获取枚举类型
     *
     * @param enumName 枚举名称
     * @return 枚举类型
     */
    public static Class<? extends Enum<?>> getEnumClass(String enumName) {
        Class<? extends Enum<?>> enumClass = ENUM_MAP.get(enumName);
        if (enumClass == null) {
            throw new BusinessException("枚举不存在：" + enumName);
        }
        return enumClass;
    }

    /**
     * 查询所有已注册枚举名称
     *
     * @return 枚举名称集合
     */
    public static Map<String, Class<? extends Enum<?>>> getAll() {
        return ENUM_MAP;
    }
}
```

### 枚举值合法性校验

枚举值校验通常发生在新增、修改业务数据时。比如创建订单时，前端传入 `orderStatus=ABC`，后端必须拒绝；如果传入 `WAIT_PAY`，后端才允许继续处理。

可以封装一个枚举校验 Service，供 Controller 或业务 Service 调用。

文件位置：`src/main/java/io/github/atengk/dict/service/SysEnumService.java`

```java
package io.github.atengk.dict.service;

import io.github.atengk.dict.vo.EnumOptionVO;

import java.util.List;

/**
 * 系统枚举 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface SysEnumService {

    /**
     * 查询枚举选项
     *
     * @param enumName 枚举名称
     * @return 枚举选项列表
     */
    List<EnumOptionVO> listOptions(String enumName);

    /**
     * 校验枚举值是否合法
     *
     * @param enumName 枚举名称
     * @param value 枚举值
     * @return true 合法，false 非法
     */
    boolean validateValue(String enumName, String value);

    /**
     * 获取枚举标签
     *
     * @param enumName 枚举名称
     * @param value 枚举值
     * @return 枚举标签
     */
    String getLabel(String enumName, String value);
}
```

文件位置：`src/main/java/io/github/atengk/dict/service/impl/SysEnumServiceImpl.java`

```java
package io.github.atengk.dict.service.impl;

import io.github.atengk.dict.common.enums.EnumRegistry;
import io.github.atengk.dict.common.utils.EnumOptionUtil;
import io.github.atengk.dict.service.SysEnumService;
import io.github.atengk.dict.vo.EnumOptionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统枚举 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class SysEnumServiceImpl implements SysEnumService {

    /**
     * 查询枚举选项
     *
     * @param enumName 枚举名称
     * @return 枚举选项列表
     */
    @Override
    public List<EnumOptionVO> listOptions(String enumName) {
        Class<? extends Enum<?>> enumClass = EnumRegistry.getEnumClass(enumName);
        List<EnumOptionVO> optionList = EnumOptionUtil.toOptions(enumClass);
        log.info("查询枚举选项成功，enumName={}，数量={}", enumName, optionList.size());
        return optionList;
    }

    /**
     * 校验枚举值是否合法
     *
     * @param enumName 枚举名称
     * @param value 枚举值
     * @return true 合法，false 非法
     */
    @Override
    public boolean validateValue(String enumName, String value) {
        Class<? extends Enum<?>> enumClass = EnumRegistry.getEnumClass(enumName);
        return EnumOptionUtil.isValid(enumClass, value);
    }

    /**
     * 获取枚举标签
     *
     * @param enumName 枚举名称
     * @param value 枚举值
     * @return 枚举标签
     */
    @Override
    public String getLabel(String enumName, String value) {
        Class<? extends Enum<?>> enumClass = EnumRegistry.getEnumClass(enumName);
        return EnumOptionUtil.getLabel(enumClass, value);
    }
}
```

业务代码中可以直接校验枚举值：

```java
if (!sysEnumService.validateValue("orderStatus", orderStatus)) {
    throw new BusinessException("订单状态不合法");
}
```

如果业务逻辑更强，建议直接使用枚举自身的方法，而不是字符串判断：

```java
if (!OrderStatusEnum.allowCancel(order.getOrderStatus())) {
    throw new BusinessException("当前订单状态不允许取消");
}
```

### 前端枚举选项接口

枚举接口和字典接口保持类似风格，前端可以用统一方式处理下拉框、单选框、标签回显等场景。

文件位置：`src/main/java/io/github/atengk/dict/controller/SysEnumController.java`

```java
package io.github.atengk.dict.controller;

import io.github.atengk.dict.common.response.Result;
import io.github.atengk.dict.service.SysEnumService;
import io.github.atengk.dict.vo.EnumOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统枚举接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/enums")
public class SysEnumController {

    private final SysEnumService sysEnumService;

    /**
     * 查询枚举选项
     *
     * @param enumName 枚举名称
     * @return 枚举选项列表
     */
    @GetMapping("/{enumName}/options")
    public Result<List<EnumOptionVO>> options(@PathVariable String enumName) {
        return Result.ok(sysEnumService.listOptions(enumName));
    }

    /**
     * 校验枚举值是否合法
     *
     * @param enumName 枚举名称
     * @param value 枚举值
     * @return 校验结果
     */
    @GetMapping("/{enumName}/validate")
    public Result<Boolean> validate(@PathVariable String enumName, @RequestParam String value) {
        return Result.ok(sysEnumService.validateValue(enumName, value));
    }

    /**
     * 获取枚举标签
     *
     * @param enumName 枚举名称
     * @param value 枚举值
     * @return 枚举标签
     */
    @GetMapping("/{enumName}/label")
    public Result<String> label(@PathVariable String enumName, @RequestParam String value) {
        return Result.ok(sysEnumService.getLabel(enumName, value));
    }
}
```

查询订单状态枚举：

```bash
curl -X GET "http://localhost:8080/api/system/enums/orderStatus/options"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "value": "WAIT_PAY",
      "label": "待支付"
    },
    {
      "value": "PAID",
      "label": "已支付"
    },
    {
      "value": "DELIVERED",
      "label": "已发货"
    },
    {
      "value": "FINISHED",
      "label": "已完成"
    },
    {
      "value": "CANCELED",
      "label": "已取消"
    }
  ]
}
```

校验订单状态枚举值：

```bash
curl -X GET "http://localhost:8080/api/system/enums/orderStatus/validate?value=WAIT_PAY"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

获取订单状态标签：

```bash
curl -X GET "http://localhost:8080/api/system/enums/orderStatus/label?value=WAIT_PAY"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "待支付"
}
```

## 字典业务使用示例

本节用一个简单的客户资料场景演示字典的实际使用方式。客户等级、客户来源这类可配置选项适合使用字典；客户状态如果只是启用禁用，也可以使用字典；如果会影响复杂状态流转，则更建议使用枚举。

### 新增业务数据时校验字典值

新增客户时，前端传入客户等级 `customerLevel` 和订单来源 `orderSource`。后端保存前必须校验它们是否属于对应字典，并且是否处于启用状态。

先创建一个简单的客户表。

```sql
CREATE TABLE `crm_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `customer_name` varchar(100) NOT NULL COMMENT '客户名称',
  `customer_level` varchar(100) NOT NULL COMMENT '客户等级，取 sys_dict_item.item_value',
  `order_source` varchar(100) NOT NULL COMMENT '来源渠道，取 sys_dict_item.item_value',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_customer_level` (`customer_level`),
  KEY `idx_order_source` (`order_source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';
```

客户实体类对应 `crm_customer` 表。

文件位置：`src/main/java/io/github/atengk/dict/entity/CrmCustomer.java`

```java
package io.github.atengk.dict.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("crm_customer")
public class CrmCustomer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 客户等级，取 customer_level 字典
     */
    private String customerLevel;

    /**
     * 来源渠道，取 order_source 字典
     */
    private String orderSource;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    /**
     * 逻辑删除：0未删除，1已删除
     */
    @TableLogic
    private Integer deleted;
}
```

新增客户入参只接收业务需要的字段，不直接接收实体类，避免前端传入无关字段。

文件位置：`src/main/java/io/github/atengk/dict/dto/CustomerCreateDTO.java`

```java
package io.github.atengk.dict.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 客户新增入参
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CustomerCreateDTO {

    /**
     * 客户名称
     */
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    /**
     * 客户等级
     */
    @NotBlank(message = "客户等级不能为空")
    private String customerLevel;

    /**
     * 来源渠道
     */
    @NotBlank(message = "来源渠道不能为空")
    private String orderSource;
}
```

客户 Mapper 使用 MyBatis-Plus 的 `BaseMapper`。

文件位置：`src/main/java/io/github/atengk/dict/mapper/CrmCustomerMapper.java`

```java
package io.github.atengk.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.dict.entity.CrmCustomer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface CrmCustomerMapper extends BaseMapper<CrmCustomer> {
}
```

客户 Service 在保存前调用字典服务完成合法性校验。这里的关键点是：字典值校验必须放在后端，不能只依赖前端下拉框限制。

文件位置：`src/main/java/io/github/atengk/dict/service/CrmCustomerService.java`

```java
package io.github.atengk.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.dict.dto.CustomerCreateDTO;
import io.github.atengk.dict.entity.CrmCustomer;

/**
 * 客户 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface CrmCustomerService extends IService<CrmCustomer> {

    /**
     * 新增客户
     *
     * @param dto 新增客户入参
     */
    void createCustomer(CustomerCreateDTO dto);
}
```

文件位置：`src/main/java/io/github/atengk/dict/service/impl/CrmCustomerServiceImpl.java`

```java
package io.github.atengk.dict.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.dict.common.exception.BusinessException;
import io.github.atengk.dict.dto.CustomerCreateDTO;
import io.github.atengk.dict.entity.CrmCustomer;
import io.github.atengk.dict.mapper.CrmCustomerMapper;
import io.github.atengk.dict.service.CrmCustomerService;
import io.github.atengk.dict.service.SysDictItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 客户 Service 实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmCustomerServiceImpl extends ServiceImpl<CrmCustomerMapper, CrmCustomer>
        implements CrmCustomerService {

    private final SysDictItemService sysDictItemService;

    /**
     * 新增客户
     *
     * @param dto 新增客户入参
     */
    @Override
    public void createCustomer(CustomerCreateDTO dto) {
        if (!sysDictItemService.validateValue("customer_level", dto.getCustomerLevel())) {
            throw new BusinessException("客户等级不合法");
        }
        if (!sysDictItemService.validateValue("order_source", dto.getOrderSource())) {
            throw new BusinessException("来源渠道不合法");
        }

        CrmCustomer customer = BeanUtil.copyProperties(dto, CrmCustomer.class);
        customer.setCreatedTime(LocalDateTime.now());
        customer.setUpdatedTime(LocalDateTime.now());
        customer.setDeleted(0);

        save(customer);
        log.info("新增客户成功，customerId={}，customerName={}", customer.getId(), customer.getCustomerName());
    }
}
```

客户新增接口如下。

文件位置：`src/main/java/io/github/atengk/dict/controller/CrmCustomerController.java`

```java
package io.github.atengk.dict.controller;

import io.github.atengk.dict.common.response.Result;
import io.github.atengk.dict.dto.CustomerCreateDTO;
import io.github.atengk.dict.service.CrmCustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/crm/customers")
public class CrmCustomerController {

    private final CrmCustomerService crmCustomerService;

    /**
     * 新增客户
     *
     * @param dto 新增客户入参
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CustomerCreateDTO dto) {
        crmCustomerService.createCustomer(dto);
        return Result.ok();
    }
}
```

正常新增客户：

```bash
curl -X POST "http://localhost:8080/api/crm/customers" \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "测试客户A",
    "customerLevel": "vip",
    "orderSource": "app"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

传入非法字典值：

```bash
curl -X POST "http://localhost:8080/api/crm/customers" \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "测试客户B",
    "customerLevel": "super_vip",
    "orderSource": "app"
  }'
```

响应示例：

```json
{
  "code": 500,
  "message": "客户等级不合法",
  "data": null
}
```

### 查询业务数据时回显字典标签

业务表中只存储字典值，例如 `vip`、`app`，但前端列表通常需要展示 `VIP客户`、`App端`。这个回显逻辑可以在查询接口中完成。

先定义客户返回对象。

文件位置：`src/main/java/io/github/atengk/dict/vo/CustomerVO.java`

```java
package io.github.atengk.dict.vo;

import lombok.Data;

/**
 * 客户返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class CustomerVO {

    private Long id;

    private String customerName;

    private String customerLevel;

    private String customerLevelLabel;

    private String orderSource;

    private String orderSourceLabel;
}
```

在 Service 中查询客户列表，并通过字典服务完成标签回显。

文件位置：`src/main/java/io/github/atengk/dict/service/CrmCustomerService.java`

```java
package io.github.atengk.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.atengk.dict.dto.CustomerCreateDTO;
import io.github.atengk.dict.entity.CrmCustomer;
import io.github.atengk.dict.vo.CustomerVO;

import java.util.List;

/**
 * 客户 Service
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface CrmCustomerService extends IService<CrmCustomer> {

    /**
     * 新增客户
     *
     * @param dto 新增客户入参
     */
    void createCustomer(CustomerCreateDTO dto);

    /**
     * 查询客户列表
     *
     * @return 客户列表
     */
    List<CustomerVO> listCustomers();
}
```

下面是在原 `CrmCustomerServiceImpl` 基础上增加的查询方法，核心是复用 `getLabel` 方法完成回显。

```java
/**
 * 查询客户列表
 *
 * @return 客户列表
 */
@Override
public List<CustomerVO> listCustomers() {
    List<CrmCustomer> customerList = list();

    return customerList.stream().map(customer -> {
        CustomerVO vo = BeanUtil.copyProperties(customer, CustomerVO.class);
        vo.setCustomerLevelLabel(sysDictItemService.getLabel("customer_level", customer.getCustomerLevel()));
        vo.setOrderSourceLabel(sysDictItemService.getLabel("order_source", customer.getOrderSource()));
        return vo;
    }).toList();
}
```

在 Controller 中增加客户列表接口。

```java
/**
 * 查询客户列表
 *
 * @return 客户列表
 */
@GetMapping
public Result<List<CustomerVO>> list() {
    return Result.ok(crmCustomerService.listCustomers());
}
```

查询客户列表：

```bash
curl -X GET "http://localhost:8080/api/crm/customers"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "customerName": "测试客户A",
      "customerLevel": "vip",
      "customerLevelLabel": "VIP客户",
      "orderSource": "app",
      "orderSourceLabel": "App端"
    }
  ]
}
```

如果列表数据量较大，不建议每一行都单独调用 `getLabel` 并重复遍历字典列表。可以先把字典选项转成 Map，再批量回显。

```java
/**
 * 查询客户列表并批量回显字典标签
 *
 * @return 客户列表
 */
public List<CustomerVO> listCustomersWithDictMap() {
    List<CrmCustomer> customerList = list();

    Map<String, String> levelLabelMap = sysDictItemService.listOptions("customer_level")
            .stream()
            .collect(Collectors.toMap(DictItemOptionVO::getValue, DictItemOptionVO::getLabel));

    Map<String, String> sourceLabelMap = sysDictItemService.listOptions("order_source")
            .stream()
            .collect(Collectors.toMap(DictItemOptionVO::getValue, DictItemOptionVO::getLabel));

    return customerList.stream().map(customer -> {
        CustomerVO vo = BeanUtil.copyProperties(customer, CustomerVO.class);
        vo.setCustomerLevelLabel(levelLabelMap.getOrDefault(customer.getCustomerLevel(), ""));
        vo.setOrderSourceLabel(sourceLabelMap.getOrDefault(customer.getOrderSource(), ""));
        return vo;
    }).toList();
}
```

上面代码需要补充导入：

```java
import io.github.atengk.dict.vo.DictItemOptionVO;

import java.util.Map;
import java.util.stream.Collectors;
```

这种方式只读取两次字典缓存，适合列表分页、导出、批量数据回显等场景。

### 禁用字典项后的处理

字典项禁用后，需要区分“历史数据展示”和“新增数据校验”两个场景。

新增或修改业务数据时，禁用字典项不能继续使用。例如 `customer_level=blacklist` 被禁用后，新客户不能再选择黑名单客户等级。

由于 `validateValue` 只查询启用字典项，所以禁用后会自动校验失败：

```java
if (!sysDictItemService.validateValue("customer_level", dto.getCustomerLevel())) {
    throw new BusinessException("客户等级不合法");
}
```

禁用字典项示例：

```bash
curl -X PUT "http://localhost:8080/api/system/dict-items" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 7,
    "dictCode": "customer_level",
    "itemValue": "blacklist",
    "itemLabel": "黑名单客户",
    "status": 0,
    "sortOrder": 3,
    "tagType": "danger"
  }'
```

再次新增使用该字典值的数据：

```bash
curl -X POST "http://localhost:8080/api/crm/customers" \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "测试客户C",
    "customerLevel": "blacklist",
    "orderSource": "pc"
  }'
```

响应示例：

```json
{
  "code": 500,
  "message": "客户等级不合法",
  "data": null
}
```

但历史数据回显要更谨慎。如果 `getLabel` 只查询启用字典项，那么历史数据中的禁用值会回显为空。实际项目中有两种处理方式。

第一种方式是保持当前实现：禁用后前端历史数据标签显示为空或原始值。这种实现简单，适合非关键展示字段。

```java
vo.setCustomerLevelLabel(sysDictItemService.getLabel("customer_level", customer.getCustomerLevel()));
```

第二种方式是提供“包含禁用项”的标签查询方法，历史数据仍然能正常回显，但新增和修改仍然只能使用启用项。

在 `SysDictItemService` 中增加方法：

```java
/**
 * 根据字典值查询标签，包含禁用项
 *
 * @param dictCode 字典类型编码
 * @param itemValue 字典项值
 * @return 字典标签
 */
String getLabelIncludeDisabled(String dictCode, String itemValue);
```

在 `SysDictItemServiceImpl` 中增加实现。这个方法直接查数据库，不复用只包含启用项的前端选项缓存，避免把禁用项返回给前端下拉框。

```java
/**
 * 根据字典值查询标签，包含禁用项
 *
 * @param dictCode 字典类型编码
 * @param itemValue 字典项值
 * @return 字典标签
 */
@Override
public String getLabelIncludeDisabled(String dictCode, String itemValue) {
    if (StrUtil.hasBlank(dictCode, itemValue)) {
        return StrUtil.EMPTY;
    }

    SysDictItem dictItem = getOne(new LambdaQueryWrapper<SysDictItem>()
            .eq(SysDictItem::getDictCode, dictCode)
            .eq(SysDictItem::getItemValue, itemValue)
            .last("LIMIT 1"));

    return dictItem == null ? StrUtil.EMPTY : dictItem.getItemLabel();
}
```

历史数据回显时改用这个方法：

```java
vo.setCustomerLevelLabel(sysDictItemService.getLabelIncludeDisabled("customer_level", customer.getCustomerLevel()));
```

推荐最终策略如下：

| 场景             | 是否允许使用禁用字典项           | 推荐方法                  |
| ---------------- | -------------------------------- | ------------------------- |
| 前端新增下拉框   | 不允许                           | `listOptions`             |
| 新增业务数据校验 | 不允许                           | `validateValue`           |
| 修改业务数据校验 | 不允许，除非业务明确允许保留旧值 | `validateValue`           |
| 历史数据列表回显 | 允许回显                         | `getLabelIncludeDisabled` |
| 统计筛选条件     | 默认不允许                       | `listOptions`             |
| 数据导出         | 建议允许回显                     | `getLabelIncludeDisabled` |

这样处理后，字典禁用不会影响历史数据展示，同时也能阻止新数据继续使用已禁用字典项。

## 接口测试与验证

本节通过 `curl` 验证字典类型、字典项、字典查询、枚举选项接口是否可用。测试前需要先完成数据库初始化 SQL、启动 MySQL、Redis，并启动 Spring Boot 项目。本案例接口围绕第 23 个“字典数据与枚举治理”场景展开，重点验证前后端字典一致性、后端字典值校验、枚举统一输出和缓存刷新。

### 字典类型维护接口

字典类型维护接口用于后台管理字典分组，例如 `user_status`、`gender`、`customer_level`、`order_source`。测试重点是新增、查询、修改、删除，以及 `dictCode` 唯一性控制。

查询字典类型列表。

```bash
curl -X GET "http://localhost:8080/api/system/dict-types"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "dictCode": "user_status",
      "dictName": "用户状态",
      "status": 1,
      "remark": "用户账号启用和禁用状态",
      "sortOrder": 1
    },
    {
      "id": 2,
      "dictCode": "gender",
      "dictName": "性别",
      "status": 1,
      "remark": "用户性别选项",
      "sortOrder": 2
    }
  ]
}
```

新增一个字典类型，例如客户来源。

```bash
curl -X POST "http://localhost:8080/api/system/dict-types" \
  -H "Content-Type: application/json" \
  -d '{
    "dictCode": "customer_source",
    "dictName": "客户来源",
    "status": 1,
    "remark": "CRM客户来源渠道",
    "sortOrder": 10,
    "createdBy": "admin",
    "updatedBy": "admin"
  }'
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

验证唯一性控制，再次提交相同 `dictCode`。

```bash
curl -X POST "http://localhost:8080/api/system/dict-types" \
  -H "Content-Type: application/json" \
  -d '{
    "dictCode": "customer_source",
    "dictName": "客户来源重复",
    "status": 1,
    "sortOrder": 11
  }'
```

预期响应示例：

```json
{
  "code": 500,
  "message": "字典类型编码已存在",
  "data": null
}
```

修改字典类型。这里假设 `customer_source` 的主键 ID 为 `5`，实际测试时以数据库查询结果为准。

```bash
curl -X PUT "http://localhost:8080/api/system/dict-types" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 5,
    "dictCode": "customer_source",
    "dictName": "客户来源渠道",
    "status": 1,
    "remark": "客户首次进入系统的来源渠道",
    "sortOrder": 10,
    "updatedBy": "admin"
  }'
```

删除字典类型。

```bash
curl -X DELETE "http://localhost:8080/api/system/dict-types/5"
```

生产项目中不建议轻易物理删除或逻辑删除字典类型。如果该字典类型已被业务表引用，推荐只禁用，不删除；或者删除前检查是否存在字典项和业务引用。

### 字典项维护接口

字典项维护接口用于维护某个字典类型下的具体选项，例如 `customer_level` 下的 `normal`、`vip`、`blacklist`。测试重点是同一 `dictCode` 下的 `itemValue` 唯一性、启用禁用、排序和缓存刷新。

查询某个字典类型下的所有字典项。

```bash
curl -X GET "http://localhost:8080/api/system/dict-items?dictCode=customer_level"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 5,
      "dictCode": "customer_level",
      "itemValue": "normal",
      "itemLabel": "普通客户",
      "status": 1,
      "sortOrder": 1,
      "tagType": "info"
    },
    {
      "id": 6,
      "dictCode": "customer_level",
      "itemValue": "vip",
      "itemLabel": "VIP客户",
      "status": 1,
      "sortOrder": 2,
      "tagType": "warning"
    }
  ]
}
```

新增字典项。下面给 `customer_source` 增加一个 `ad` 选项。

```bash
curl -X POST "http://localhost:8080/api/system/dict-items" \
  -H "Content-Type: application/json" \
  -d '{
    "dictCode": "customer_source",
    "itemValue": "ad",
    "itemLabel": "广告投放",
    "status": 1,
    "sortOrder": 1,
    "tagType": "success",
    "remark": "广告渠道进入的客户",
    "createdBy": "admin",
    "updatedBy": "admin"
  }'
```

新增后再次查询前端选项，确认缓存已刷新并能返回新值。

```bash
curl -X GET "http://localhost:8080/api/system/dict-items/options/customer_source"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "value": "ad",
      "label": "广告投放",
      "tagType": "success",
      "cssClass": null,
      "extraJson": null,
      "sortOrder": 1
    }
  ]
}
```

修改字典项。这里假设 `ad` 字典项的主键 ID 为 `10`。

```bash
curl -X PUT "http://localhost:8080/api/system/dict-items" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 10,
    "dictCode": "customer_source",
    "itemValue": "ad",
    "itemLabel": "广告渠道",
    "status": 1,
    "sortOrder": 1,
    "tagType": "warning",
    "remark": "广告投放渠道进入的客户",
    "updatedBy": "admin"
  }'
```

验证标签是否更新。

```bash
curl -X GET "http://localhost:8080/api/system/dict-items/label?dictCode=customer_source&itemValue=ad"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "广告渠道"
}
```

禁用字典项。

```bash
curl -X PUT "http://localhost:8080/api/system/dict-items" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 10,
    "dictCode": "customer_source",
    "itemValue": "ad",
    "itemLabel": "广告渠道",
    "status": 0,
    "sortOrder": 1,
    "tagType": "warning",
    "remark": "暂时不允许继续选择",
    "updatedBy": "admin"
  }'
```

禁用后查询前端选项，默认不应返回该字典项。

```bash
curl -X GET "http://localhost:8080/api/system/dict-items/options/customer_source"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": []
}
```

删除字典项。

```bash
curl -X DELETE "http://localhost:8080/api/system/dict-items/10"
```

实际项目中，如果字典项已经被业务数据引用，建议优先禁用，不建议直接删除。删除会导致历史数据回显困难，尤其是报表、导出、审计记录等场景。

### 字典查询接口

字典查询接口主要给前端页面和后端业务校验使用。前端常用 `options` 接口渲染下拉框、单选框、标签；后端常用 `validate` 接口或 Service 方法校验业务入参。

查询用户状态选项。

```bash
curl -X GET "http://localhost:8080/api/system/dict-items/options/user_status"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "value": "enable",
      "label": "启用",
      "tagType": "success",
      "cssClass": null,
      "extraJson": null,
      "sortOrder": 1
    },
    {
      "value": "disable",
      "label": "禁用",
      "tagType": "danger",
      "cssClass": null,
      "extraJson": null,
      "sortOrder": 2
    }
  ]
}
```

校验合法字典值。

```bash
curl -X GET "http://localhost:8080/api/system/dict-items/validate?dictCode=user_status&itemValue=enable"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

校验非法字典值。

```bash
curl -X GET "http://localhost:8080/api/system/dict-items/validate?dictCode=user_status&itemValue=unknown_status"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": false
}
```

根据字典值获取标签。

```bash
curl -X GET "http://localhost:8080/api/system/dict-items/label?dictCode=user_status&itemValue=enable"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "启用"
}
```

验证 Redis 缓存是否写入，可以使用 Redis CLI 查询。

```bash
redis-cli GET "sys:dict:item:list:user_status"
```

预期可以看到 JSON 字符串。如果返回空，检查 Spring Boot 是否连接到了正确的 Redis database，以及 `listOptions` 接口是否已经被调用。

验证缓存刷新，可以先查询一次字典选项，再修改字典项标签，最后重新查询。

```bash
# 第一次查询，写入 Redis 缓存
curl -X GET "http://localhost:8080/api/system/dict-items/options/user_status"

# 修改字典项标签，假设 enable 的 ID 为 1
curl -X PUT "http://localhost:8080/api/system/dict-items" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "dictCode": "user_status",
    "itemValue": "enable",
    "itemLabel": "正常",
    "status": 1,
    "sortOrder": 1,
    "tagType": "success"
  }'

# 再次查询，应返回新标签
curl -X GET "http://localhost:8080/api/system/dict-items/options/user_status"
```

预期 `enable` 对应的 `label` 从 `启用` 变为 `正常`。如果仍然返回旧值，优先检查 `clearCache(dictCode)` 是否被调用，以及 Redis Key 是否与代码中的 Key 前缀一致。

### 枚举选项查询接口

枚举接口用于输出后端强约束枚举，例如订单状态、支付状态、通用启停状态等。前端不需要维护这些选项，也不需要硬编码枚举值。

查询订单状态枚举选项。

```bash
curl -X GET "http://localhost:8080/api/system/enums/orderStatus/options"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "value": "WAIT_PAY",
      "label": "待支付"
    },
    {
      "value": "PAID",
      "label": "已支付"
    },
    {
      "value": "DELIVERED",
      "label": "已发货"
    },
    {
      "value": "FINISHED",
      "label": "已完成"
    },
    {
      "value": "CANCELED",
      "label": "已取消"
    }
  ]
}
```

查询支付状态枚举选项。

```bash
curl -X GET "http://localhost:8080/api/system/enums/payStatus/options"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "value": "UNPAID",
      "label": "未支付"
    },
    {
      "value": "PAYING",
      "label": "支付中"
    },
    {
      "value": "SUCCESS",
      "label": "支付成功"
    },
    {
      "value": "FAILED",
      "label": "支付失败"
    },
    {
      "value": "CLOSED",
      "label": "已关闭"
    }
  ]
}
```

校验合法枚举值。

```bash
curl -X GET "http://localhost:8080/api/system/enums/orderStatus/validate?value=WAIT_PAY"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

校验非法枚举值。

```bash
curl -X GET "http://localhost:8080/api/system/enums/orderStatus/validate?value=UNKNOWN"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": false
}
```

获取枚举标签。

```bash
curl -X GET "http://localhost:8080/api/system/enums/orderStatus/label?value=WAIT_PAY"
```

预期响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "待支付"
}
```

查询不存在的枚举名称。

```bash
curl -X GET "http://localhost:8080/api/system/enums/notExistEnum/options"
```

预期响应示例：

```json
{
  "code": 500,
  "message": "枚举不存在：notExistEnum",
  "data": null
}
```

如果需要新增一个可暴露给前端的枚举，只需要完成两步：创建枚举类并实现 `BaseEnum`；在 `EnumRegistry` 中注册枚举名称和枚举类。前端后续即可通过统一接口查询，不需要新增 Controller 方法。

## 小结与扩展方向

本案例已经实现了一个可落地的字典数据与枚举治理模块，适合后台管理、CRM、OA、ERP、SaaS、低代码平台等常见 Java 后端系统。核心思路是：字典用于可配置选项，枚举用于强业务约束；前端通过统一接口取选项，后端通过统一服务做校验和回显。

### 当前案例能力

当前案例已经具备以下能力：

| 能力         | 实现情况                                                     |
| ------------ | ------------------------------------------------------------ |
| 字典类型管理 | 已实现新增、修改、删除、列表查询                             |
| 字典项管理   | 已实现新增、修改、删除、按类型查询                           |
| 前端字典选项 | 已实现 `/api/system/dict-items/options/{dictCode}`           |
| 字典值校验   | 已实现 `/api/system/dict-items/validate` 和 Service 校验方法 |
| 字典标签回显 | 已实现 `/api/system/dict-items/label` 和 Service 回显方法    |
| Redis 缓存   | 已实现按 `dictCode` 缓存字典项列表                           |
| 缓存刷新     | 已实现字典类型和字典项变更后删除缓存                         |
| 统一枚举接口 | 已实现 `BaseEnum<T>`                                         |
| 枚举选项输出 | 已实现 `/api/system/enums/{enumName}/options`                |
| 枚举值校验   | 已实现 `/api/system/enums/{enumName}/validate`               |
| 枚举标签回显 | 已实现 `/api/system/enums/{enumName}/label`                  |
| 业务集成示例 | 已实现客户新增校验和客户列表标签回显示例                     |

当前方案的关键收益是：前端不需要维护重复的选项常量，后端不需要在业务代码里散落大量字符串判断，字典项修改后能够通过缓存刷新及时生效。

### 可扩展方向

后续可以根据项目复杂度继续扩展以下能力。

| 扩展方向          | 说明                                                         |
| ----------------- | ------------------------------------------------------------ |
| DTO 参数校验完善  | 将字典类型、字典项新增修改全部改成 DTO，并增加 `@NotBlank`、`@NotNull`、长度校验 |
| 后台分页查询      | 字典类型和字典项列表接入 MyBatis-Plus 分页，适配后台管理页面 |
| 操作审计          | 字典类型和字典项新增、修改、禁用、删除时记录操作日志         |
| Sa-Token 权限控制 | 给维护接口增加权限注解，普通用户只能查询，管理员才能维护     |
| 多租户字典        | 在字典表增加 `tenant_id`，Redis Key 增加租户维度             |
| 多语言字典        | 增加 `locale` 字段或独立字典国际化表，支持 `zh_CN`、`en_US` 等语言 |
| 系统内置字典保护  | 增加 `system_flag` 字段，限制核心字典不允许删除或修改编码    |
| 字典导入导出      | 支持 Excel 批量导入字典项和导出配置                          |
| 字典变更消息通知  | 字典变更后通过 MQ 或 Redis Pub/Sub 通知其他服务清理本地缓存  |
| 本地缓存增强      | 在 Redis 前增加 Caffeine 本地缓存，提升高频读取性能          |
| 枚举自动扫描      | 启动时扫描实现 `BaseEnum` 的枚举类，减少手动维护 `EnumRegistry` |
| 前端统一 Hook     | 前端封装 `useDict(dictCode)` 和 `useEnum(enumName)`，统一处理选项加载 |
| 历史数据兼容      | 提供包含禁用项的回显接口，避免历史业务数据标签丢失           |
| 字典值变更保护    | 限制已被业务表引用的 `itemValue` 不允许修改，只允许修改 `itemLabel` |

生产环境中最建议优先补充三项：权限控制、操作审计、系统内置字典保护。字典数据虽然看起来是基础配置，但它会影响业务入参校验、前端选项展示、统计筛选和历史数据回显，一旦被误改，影响范围通常比较大。