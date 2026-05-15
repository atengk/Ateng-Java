# 业务编号生成器

## 功能概述

业务编号生成器用于统一生成系统中的业务单号，例如订单编号、合同编号、工单编号、支付流水号、出库单号等。它的核心目标是让不同业务模块不再各自手写编号规则，而是通过统一的编号生成服务完成编号生成，保证编号格式一致、全局唯一、并发安全、易于扩展。

本案例采用 `Spring Boot 3 + Redis + Redisson + Hutool` 实现核心功能，使用 Redis 原子自增能力生成流水号，使用 Redisson 分布式锁保证规则初始化、日期切换等场景下的数据一致性。

### 业务场景

在实际 Java 后端项目中，很多业务数据都需要一个可读性较强的业务编号。它不同于数据库主键 ID，通常会展示给用户、运营、财务、客服或外部系统使用。

常见业务场景如下：

| 业务类型   | 编号示例                | 说明                         |
| ---------- | ----------------------- | ---------------------------- |
| 订单编号   | `ORD20260515000001`     | 用于用户下单、售后、支付关联 |
| 合同编号   | `HT202605150001`        | 用于合同审批、归档、打印     |
| 工单编号   | `WO20260515000001`      | 用于客服、运维、内部流程流转 |
| 支付流水号 | `PAY202605151430250001` | 用于支付请求、回调、对账     |
| 出库单号   | `OUT202605150001`       | 用于仓储出库、物流发货       |

业务编号一般需要满足以下要求：

| 要求     | 说明                               |
| -------- | ---------------------------------- |
| 唯一性   | 同一业务类型下编号不能重复         |
| 可读性   | 通过前缀和日期可以快速识别业务含义 |
| 有序性   | 同一天或同一时间段内编号按流水递增 |
| 并发安全 | 多个请求同时生成编号时不能冲突     |
| 可扩展   | 后续可以支持更多业务类型和编号格式 |

在本案例中，业务编号生成器主要用于解决“高并发下生成可读业务单号”的问题，不直接替代数据库主键，也不负责雪花 ID、UUID 这类技术 ID 的生成。

### 编号规则

本案例采用经典的业务编号格式：

```text
业务前缀 + 日期字符串 + 固定位数流水号
```

例如：

```text
ORD20260515000001
```

对应拆解如下：

| 组成部分   | 示例       | 说明                      |
| ---------- | ---------- | ------------------------- |
| 业务前缀   | `ORD`      | 表示订单业务              |
| 日期字符串 | `20260515` | 表示编号生成日期          |
| 流水号     | `000001`   | 当天递增流水号，固定 6 位 |

默认规则如下：

| 配置项         | 默认值          | 说明                              |
| -------------- | --------------- | --------------------------------- |
| 编号前缀       | 按业务类型配置  | 例如 `ORD`、`HT`、`WO`            |
| 日期格式       | `yyyyMMdd`      | 按天重置流水                      |
| 流水位数       | `6`             | 不足位数左侧补 `0`                |
| Redis Key 维度 | 业务类型 + 日期 | 每个业务每天独立递增              |
| 过期时间       | `2 天`          | 避免 Redis 中长期堆积历史流水 Key |

Redis 中的流水 Key 可以设计为：

```text
biz:no:{业务类型}:{日期}
```

例如订单编号在 Redis 中对应的 Key：

```text
biz:no:ORDER:20260515
```

每次生成订单编号时，对该 Key 执行一次原子递增：

```text
INCR biz:no:ORDER:20260515
```

如果 Redis 返回 `1`，说明这是当天该业务类型的第一个编号，最终编号为：

```text
ORD20260515000001
```

如果 Redis 返回 `25`，最终编号为：

```text
ORD20260515000025
```

这种方案简单、稳定，适合大多数中后台系统、订单系统、审批系统、仓储系统和 CRM 系统中的业务编号生成需求。

### 实现目标

本案例最终实现一个可直接接入 Spring Boot 项目的业务编号生成模块，核心目标如下：

| 目标             | 说明                                                 |
| ---------------- | ---------------------------------------------------- |
| 支持多业务类型   | 订单、合同、工单等业务可以使用不同编号前缀           |
| 支持日期维度流水 | 默认按天生成流水号，日期变化后流水重新从 1 开始      |
| 支持固定位数补零 | 例如 `1` 格式化为 `000001`                           |
| 支持高并发生成   | 基于 Redis 原子自增保证并发安全                      |
| 支持统一接口调用 | 业务模块只需要传入业务类型即可生成编号               |
| 支持规则扩展     | 后续可以扩展不同日期格式、不同流水位数、不同前缀规则 |

最终希望业务代码中只需要这样调用：

```java
String orderNo = businessNoService.generate("ORDER");
```

生成结果示例：

```text
ORD20260515000001
```

对于不同业务类型，可以生成不同格式的编号：

```text
ORD20260515000001
HT202605150001
WO20260515000001
PAY202605151430250001
```

本案例先实现核心编号生成能力，不引入复杂的后台配置页面，也不展开编号规则的动态维护功能。规则可以先通过枚举或配置类维护，后续再根据项目需要扩展为数据库配置或管理端维护。

## 技术选型

本案例采用成熟、轻量、容易落地的后端技术栈。编号生成的核心逻辑放在服务端完成，通过 Redis 的原子自增能力解决并发冲突问题，通过数据库维护编号规则和流水记录，便于后续扩展后台配置、问题排查和审计追踪。

### 核心技术栈

| 技术              | 用途                                    |
| ----------------- | --------------------------------------- |
| Spring Boot 3     | 提供 Web 接口、配置管理、依赖注入       |
| MyBatis-Plus      | 操作编号规则表、编号流水记录表          |
| MySQL 8           | 存储编号规则、流水记录                  |
| Redis             | 存储每日递增流水号                      |
| Redisson          | 提供 Redis 分布式能力，辅助处理并发场景 |
| Hutool            | 日期格式化、字符串处理、对象校验        |
| Lombok            | 简化实体类、DTO、VO 代码                |
| Knife4j / Swagger | 接口调试，可选                          |

Maven 依赖可以先准备如下内容，后续代码会基于这些依赖实现。

```xml
<!-- Spring Boot Web：提供接口能力 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
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

<!-- Redis：用于存储业务编号流水计数器 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Redisson：提供分布式锁和 Redis 增强能力 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.30.0</version>
</dependency>

<!-- Hutool：日期、字符串、数字格式化等工具类 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.29</version>
</dependency>

<!-- Lombok：减少 getter、setter、构造方法等样板代码 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

其中 Redis 是编号生成的关键组件。数据库主要负责保存规则和流水记录，不建议在高并发场景下直接通过数据库行锁生成流水号，否则吞吐量会明显下降。

### 编号生成方案

本案例使用“Redis 原子自增 + 日期维度 Key + 固定位数补零”的方案生成编号。

核心流程如下：

```text
读取业务编号规则
        ↓
根据日期格式生成日期字符串
        ↓
拼接 Redis 流水 Key
        ↓
Redis INCR 原子递增
        ↓
设置 Redis Key 过期时间
        ↓
格式化流水号，左侧补 0
        ↓
拼接前缀 + 日期 + 流水号
        ↓
返回业务编号
```

以订单编号为例，规则如下：

| 配置项    | 示例                    |
| --------- | ----------------------- |
| 业务类型  | `ORDER`                 |
| 编号前缀  | `ORD`                   |
| 日期格式  | `yyyyMMdd`              |
| 流水位数  | `6`                     |
| Redis Key | `biz:no:ORDER:20260515` |

当 Redis 自增值为 `1` 时：

```text
ORD + 20260515 + 000001 = ORD20260515000001
```

当 Redis 自增值为 `238` 时：

```text
ORD + 20260515 + 000238 = ORD20260515000238
```

Redis Key 建议按业务类型和日期拆分：

```text
biz:no:{bizType}:{dateText}
```

例如：

```text
biz:no:ORDER:20260515
biz:no:CONTRACT:20260515
biz:no:WORK_ORDER:20260515
```

这种 Key 设计可以做到不同业务类型的流水号互不影响，并且每天自动重新从 `1` 开始递增。过期时间建议设置为 `2 ~ 7 天`，既能支持短期排查，又不会长期占用 Redis 内存。

编号规则建议优先从数据库读取，然后在本地缓存或 Redis 中做短时间缓存。对于本案例，为了突出核心功能，可以先直接查询数据库，后续再扩展缓存。

## 表结构设计

表结构分为两类：编号规则表和编号流水记录表。编号规则表用于维护不同业务类型的编号格式，编号流水记录表用于保存已经生成过的编号，便于排查、审计和幂等控制。

实际生成编号时，流水递增仍然以 Redis 为准，数据库流水记录不参与并发抢号。

### 编号规则表

编号规则表用于配置不同业务类型的编号生成规则，例如订单使用 `ORD` 前缀，合同使用 `HT` 前缀，工单使用 `WO` 前缀。

```sql
CREATE TABLE biz_no_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型，例如 ORDER、CONTRACT、WORK_ORDER',
    biz_name VARCHAR(128) NOT NULL COMMENT '业务名称，例如 订单、合同、工单',
    prefix VARCHAR(32) NOT NULL COMMENT '编号前缀，例如 ORD、HT、WO',
    date_pattern VARCHAR(32) NOT NULL DEFAULT 'yyyyMMdd' COMMENT '日期格式，例如 yyyyMMdd、yyyyMMddHHmmss',
    serial_length INT NOT NULL DEFAULT 6 COMMENT '流水号长度，不足位数左侧补0',
    expire_days INT NOT NULL DEFAULT 2 COMMENT 'Redis流水Key过期天数',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_type (biz_type),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务编号规则表';
```

可以初始化几条常用业务规则：

```sql
INSERT INTO biz_no_rule
(biz_type, biz_name, prefix, date_pattern, serial_length, expire_days, enabled, remark)
VALUES
('ORDER', '订单编号', 'ORD', 'yyyyMMdd', 6, 3, 1, '订单业务编号'),
('CONTRACT', '合同编号', 'HT', 'yyyyMMdd', 4, 3, 1, '合同业务编号'),
('WORK_ORDER', '工单编号', 'WO', 'yyyyMMdd', 6, 3, 1, '客服或运维工单编号'),
('PAYMENT', '支付流水号', 'PAY', 'yyyyMMddHHmmss', 4, 3, 1, '支付请求流水号');
```

这张表的核心字段是 `biz_type`、`prefix`、`date_pattern` 和 `serial_length`。后续如果需要做后台管理，只需要维护这张表即可，不需要修改代码中的编号规则。

### 编号流水记录表

编号流水记录表用于保存每次生成出的业务编号。它不是生成编号的核心依赖，而是用于业务追踪、问题排查、审计记录和幂等扩展。

```sql
CREATE TABLE biz_no_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型，例如 ORDER、CONTRACT、WORK_ORDER',
    biz_no VARCHAR(128) NOT NULL COMMENT '生成后的业务编号',
    serial_date VARCHAR(32) NOT NULL COMMENT '流水日期字符串，例如 20260515',
    serial_value BIGINT NOT NULL COMMENT 'Redis自增后的流水值',
    redis_key VARCHAR(255) NOT NULL COMMENT 'Redis流水Key',
    request_id VARCHAR(128) DEFAULT NULL COMMENT '请求ID，用于幂等扩展',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_no (biz_no),
    KEY idx_biz_type_date (biz_type, serial_date),
    KEY idx_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务编号流水记录表';
```

是否每次生成编号都写入 `biz_no_record`，需要根据业务量决定：

| 场景             | 建议                                     |
| ---------------- | ---------------------------------------- |
| 中低并发后台系统 | 可以同步写入流水记录表                   |
| 高并发订单系统   | 建议异步写入，或只在核心业务表中保存编号 |
| 对账、审计要求高 | 建议保留流水记录                         |
| 只需要生成编号   | 可以不写流水记录表                       |

本案例为了便于观察生成结果，会保留流水记录表。后续实现代码时，可以先同步写入，保证逻辑清晰；如果项目并发量较高，再改为 MQ 或异步线程池写入。

## 项目结构

本案例按照常见 Spring Boot 分层结构组织代码，核心代码集中在 `bizno` 模块下，方便后续复制到其他业务系统中。

### 后端目录结构

建议项目结构如下：

```text
src/main/java/io/github/atengk/bizno
├── controller
│   └── BusinessNoController.java
├── dto
│   └── BusinessNoGenerateDTO.java
├── entity
│   ├── BizNoRule.java
│   └── BizNoRecord.java
├── enums
│   └── BizNoTypeEnum.java
├── mapper
│   ├── BizNoRuleMapper.java
│   └── BizNoRecordMapper.java
├── service
│   ├── BusinessNoService.java
│   ├── BizNoRuleService.java
│   └── BizNoRecordService.java
├── service.impl
│   ├── BusinessNoServiceImpl.java
│   ├── BizNoRuleServiceImpl.java
│   └── BizNoRecordServiceImpl.java
└── vo
    └── BusinessNoGenerateVO.java
```

如果项目中已经有统一的 `common`、`domain`、`infrastructure` 等分层结构，也可以按项目规范调整。核心原则是：编号生成服务要独立，不要散落在订单、合同、工单等具体业务模块中。

### 核心类职责

核心类职责如下：

| 类名                    | 职责                                         |
| ----------------------- | -------------------------------------------- |
| `BusinessNoController`  | 提供业务编号生成接口，方便前端或其他服务调用 |
| `BusinessNoGenerateDTO` | 接收编号生成请求参数，例如业务类型、请求ID   |
| `BusinessNoGenerateVO`  | 返回生成后的业务编号、业务类型、流水值等信息 |
| `BizNoRule`             | 对应编号规则表 `biz_no_rule`                 |
| `BizNoRecord`           | 对应编号流水记录表 `biz_no_record`           |
| `BizNoTypeEnum`         | 定义常用业务类型，避免魔法字符串             |
| `BizNoRuleMapper`       | 操作编号规则表                               |
| `BizNoRecordMapper`     | 操作编号流水记录表                           |
| `BusinessNoService`     | 定义业务编号生成核心接口                     |
| `BusinessNoServiceImpl` | 实现 Redis 自增、编号拼接、流水记录保存      |
| `BizNoRuleService`      | 查询和维护编号规则                           |
| `BizNoRecordService`    | 保存和查询编号流水记录                       |

其中最核心的是 `BusinessNoServiceImpl`，它负责完成以下动作：

```text
1. 校验业务类型
2. 查询编号规则
3. 按规则生成日期字符串
4. 拼接 Redis 流水 Key
5. 调用 Redis 原子自增
6. 设置 Redis Key 过期时间
7. 格式化流水号
8. 拼接完整业务编号
9. 保存流水记录
10. 返回编号结果
```

业务模块后续只需要依赖 `BusinessNoService`，不需要关心 Redis Key、流水号补零、规则读取等细节。例如订单模块创建订单时，只需要调用：

```java
String orderNo = businessNoService.generateNo("ORDER");
```

合同模块创建合同时，只需要调用：

```java
String contractNo = businessNoService.generateNo("CONTRACT");
```

这样可以保证所有业务编号都由统一入口生成，减少重复代码，也方便后续统一调整编号规则。

## 核心实现

核心实现围绕“业务类型读取规则、Redis 生成流水、格式化编号、保存记录、返回结果”展开。下面代码默认包路径为 `io.github.atengk.bizno`，可以直接放入 Spring Boot 项目中使用。

### 编号规则枚举

编号规则枚举用于定义系统中常见的业务类型，避免业务代码中到处出现 `ORDER`、`CONTRACT` 这类魔法字符串。实际编号规则仍然以数据库中的 `biz_no_rule` 表为准，枚举主要用于代码层面的规范约束和调用提示。

文件位置：`src/main/java/io/github/atengk/bizno/enums/BizNoTypeEnum.java`

```java
package io.github.atengk.bizno.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务编号类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
@AllArgsConstructor
public enum BizNoTypeEnum {

    ORDER("ORDER", "订单编号"),

    CONTRACT("CONTRACT", "合同编号"),

    WORK_ORDER("WORK_ORDER", "工单编号"),

    PAYMENT("PAYMENT", "支付流水号");

    private final String code;

    private final String name;

    /**
     * 判断业务类型是否存在于枚举中
     *
     * @param bizType 业务类型
     * @return 是否存在
     */
    public static boolean contains(String bizType) {
        if (StrUtil.isBlank(bizType)) {
            return false;
        }
        for (BizNoTypeEnum item : values()) {
            if (StrUtil.equalsIgnoreCase(item.getCode(), bizType)) {
                return true;
            }
        }
        return false;
    }
}
```

### 编号规则配置实体

编号规则配置实体对应 `biz_no_rule` 表，用于维护不同业务类型的编号前缀、日期格式、流水号长度和 Redis Key 过期时间。

文件位置：`src/main/java/io/github/atengk/bizno/entity/BizNoRule.java`

```java
package io.github.atengk.bizno.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务编号规则实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("biz_no_rule")
public class BizNoRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务类型，例如 ORDER、CONTRACT、WORK_ORDER
     */
    private String bizType;

    /**
     * 业务名称，例如 订单编号、合同编号
     */
    private String bizName;

    /**
     * 编号前缀，例如 ORD、HT、WO
     */
    private String prefix;

    /**
     * 日期格式，例如 yyyyMMdd、yyyyMMddHHmmss
     */
    private String datePattern;

    /**
     * 流水号长度，不足位数左侧补0
     */
    private Integer serialLength;

    /**
     * Redis流水Key过期天数
     */
    private Integer expireDays;

    /**
     * 是否启用：0-禁用，1-启用
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

流水记录实体用于保存已经生成的编号，便于排查、审计和幂等扩展。

文件位置：`src/main/java/io/github/atengk/bizno/entity/BizNoRecord.java`

```java
package io.github.atengk.bizno.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务编号流水记录实体
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@TableName("biz_no_record")
public class BizNoRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 生成后的业务编号
     */
    private String bizNo;

    /**
     * 流水日期字符串
     */
    private String serialDate;

    /**
     * Redis自增后的流水值
     */
    private Long serialValue;

    /**
     * Redis流水Key
     */
    private String redisKey;

    /**
     * 请求ID，用于幂等扩展
     */
    private String requestId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

Mapper 直接继承 MyBatis-Plus 的 `BaseMapper`，用于规则查询和流水记录保存。

文件位置：`src/main/java/io/github/atengk/bizno/mapper/BizNoRuleMapper.java`

```java
package io.github.atengk.bizno.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.bizno.entity.BizNoRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务编号规则 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface BizNoRuleMapper extends BaseMapper<BizNoRule> {
}
```

文件位置：`src/main/java/io/github/atengk/bizno/mapper/BizNoRecordMapper.java`

```java
package io.github.atengk.bizno.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.bizno.entity.BizNoRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务编号流水记录 Mapper
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Mapper
public interface BizNoRecordMapper extends BaseMapper<BizNoRecord> {
}
```

### 编号生成请求对象

编号生成请求对象用于接收业务类型和请求 ID。`requestId` 不是必填字段，但在订单创建、支付请求、外部系统回调等场景中建议传入，用于防止同一次业务请求重复生成编号。

文件位置：`src/main/java/io/github/atengk/bizno/dto/BusinessNoGenerateDTO.java`

```java
package io.github.atengk.bizno.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 业务编号生成请求对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class BusinessNoGenerateDTO {

    /**
     * 业务类型，例如 ORDER、CONTRACT、WORK_ORDER
     */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    /**
     * 请求ID，用于幂等控制，可不传
     */
    private String requestId;
}
```

### 编号生成结果对象

编号生成结果对象用于返回完整业务编号、业务类型、Redis 流水值和 Redis Key，方便接口调用方调试和排查问题。

文件位置：`src/main/java/io/github/atengk/bizno/vo/BusinessNoGenerateVO.java`

```java
package io.github.atengk.bizno.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务编号生成结果对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessNoGenerateVO {

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 生成后的业务编号
     */
    private String bizNo;

    /**
     * 流水日期字符串
     */
    private String serialDate;

    /**
     * Redis自增后的流水值
     */
    private Long serialValue;

    /**
     * Redis流水Key
     */
    private String redisKey;

    /**
     * 是否命中幂等记录
     */
    private Boolean idempotent;
}
```

### 编号生成服务接口

编号生成服务接口是业务模块调用编号生成器的统一入口。业务模块一般只需要调用 `generateNo` 获取字符串编号，接口或调试场景可以调用 `generate` 获取完整生成结果。

文件位置：`src/main/java/io/github/atengk/bizno/service/BusinessNoService.java`

```java
package io.github.atengk.bizno.service;

import io.github.atengk.bizno.dto.BusinessNoGenerateDTO;
import io.github.atengk.bizno.entity.BizNoRule;
import io.github.atengk.bizno.vo.BusinessNoGenerateVO;

/**
 * 业务编号生成服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface BusinessNoService {

    /**
     * 生成业务编号
     *
     * @param dto 编号生成请求
     * @return 编号生成结果
     */
    BusinessNoGenerateVO generate(BusinessNoGenerateDTO dto);

    /**
     * 生成业务编号字符串
     *
     * @param bizType 业务类型
     * @return 业务编号
     */
    String generateNo(String bizType);

    /**
     * 查询业务编号规则
     *
     * @param bizType 业务类型
     * @return 编号规则
     */
    BizNoRule getRule(String bizType);
}
```

### 基于 Redis 的流水号生成

流水号生成的关键是 Redis 的 `INCR` 原子自增。每个业务类型、每个日期使用独立的 Redis Key，保证不同业务、不同日期之间互不影响。

Redis Key 格式如下：

```text
biz:no:{bizType}:{serialDate}
```

示例：

```text
biz:no:ORDER:20260515
biz:no:CONTRACT:20260515
biz:no:PAYMENT:20260515143025
```

核心逻辑会在服务实现中通过下面方式完成：

```java
Long serialValue = redisTemplate.opsForValue().increment(redisKey);
```

`increment` 是 Redis 原子操作，多线程、多实例并发调用时也不会生成重复流水值。编号生成完成后，对 Redis Key 设置过期时间，避免历史流水长期占用 Redis 内存。

### 编号格式化处理

流水值从 Redis 获取后是普通数字，例如 `1`、`25`、`1024`，需要按规则格式化为固定位数的字符串。

格式化规则如下：

| Redis流水值 | 流水位数 | 格式化结果 |
| ----------- | -------- | ---------- |
| `1`         | `6`      | `000001`   |
| `25`        | `6`      | `000025`   |
| `1024`      | `6`      | `001024`   |
| `1000000`   | `6`      | `1000000`  |

当流水值超过固定位数时，本案例默认不截断，直接返回完整流水值，避免因为截断导致编号重复。

### 编号生成服务实现

编号生成服务实现包含完整的规则查询、幂等判断、Redis 自增、编号拼接、流水记录保存逻辑。普通业务直接调用 `generateNo("ORDER")` 即可，接口调用可以使用 `generate(dto)` 获取完整结果。

文件位置：`src/main/java/io/github/atengk/bizno/service/impl/BusinessNoServiceImpl.java`

```java
package io.github.atengk.bizno.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.bizno.dto.BusinessNoGenerateDTO;
import io.github.atengk.bizno.entity.BizNoRecord;
import io.github.atengk.bizno.entity.BizNoRule;
import io.github.atengk.bizno.mapper.BizNoRecordMapper;
import io.github.atengk.bizno.mapper.BizNoRuleMapper;
import io.github.atengk.bizno.service.BusinessNoService;
import io.github.atengk.bizno.vo.BusinessNoGenerateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 业务编号生成服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessNoServiceImpl implements BusinessNoService {

    /**
     * Redis流水Key模板
     */
    private static final String REDIS_KEY_TEMPLATE = "biz:no:{}:{}";

    /**
     * 幂等锁Key模板
     */
    private static final String IDEMPOTENT_LOCK_KEY_TEMPLATE = "biz:no:lock:{}:{}";

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedissonClient redissonClient;

    private final BizNoRuleMapper bizNoRuleMapper;

    private final BizNoRecordMapper bizNoRecordMapper;

    /**
     * 生成业务编号
     *
     * @param dto 编号生成请求
     * @return 编号生成结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessNoGenerateVO generate(BusinessNoGenerateDTO dto) {
        Assert.notNull(dto, "编号生成请求不能为空");

        String bizType = normalizeBizType(dto.getBizType());
        String requestId = StrUtil.trimToEmpty(dto.getRequestId());

        if (StrUtil.isBlank(requestId)) {
            return doGenerate(bizType, null);
        }

        String lockKey = StrUtil.format(IDEMPOTENT_LOCK_KEY_TEMPLATE, bizType, requestId);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("业务编号生成繁忙，请稍后重试");
            }

            BusinessNoGenerateVO exists = getByRequestId(bizType, requestId);
            if (ObjectUtil.isNotNull(exists)) {
                log.info("业务编号命中幂等记录，bizType={}，requestId={}，bizNo={}", bizType, requestId, exists.getBizNo());
                return exists;
            }

            return doGenerate(bizType, requestId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("业务编号生成被中断", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 生成业务编号字符串
     *
     * @param bizType 业务类型
     * @return 业务编号
     */
    @Override
    public String generateNo(String bizType) {
        BusinessNoGenerateDTO dto = new BusinessNoGenerateDTO();
        dto.setBizType(bizType);
        return this.generate(dto).getBizNo();
    }

    /**
     * 查询业务编号规则
     *
     * @param bizType 业务类型
     * @return 编号规则
     */
    @Override
    public BizNoRule getRule(String bizType) {
        String normalizedBizType = normalizeBizType(bizType);

        BizNoRule rule = bizNoRuleMapper.selectOne(
                Wrappers.<BizNoRule>lambdaQuery()
                        .eq(BizNoRule::getBizType, normalizedBizType)
                        .eq(BizNoRule::getEnabled, 1)
                        .last("LIMIT 1")
        );

        if (ObjectUtil.isNull(rule)) {
            throw new IllegalArgumentException(StrUtil.format("业务编号规则不存在或未启用：{}", normalizedBizType));
        }

        return rule;
    }

    /**
     * 执行编号生成
     *
     * @param bizType   业务类型
     * @param requestId 请求ID
     * @return 编号生成结果
     */
    private BusinessNoGenerateVO doGenerate(String bizType, String requestId) {
        BizNoRule rule = this.getRule(bizType);

        String datePattern = ObjectUtil.defaultIfNull(rule.getDatePattern(), "yyyyMMdd");
        Integer serialLength = ObjectUtil.defaultIfNull(rule.getSerialLength(), 6);
        Integer expireDays = ObjectUtil.defaultIfNull(rule.getExpireDays(), 3);

        String serialDate = DateUtil.format(new Date(), datePattern);
        String redisKey = StrUtil.format(REDIS_KEY_TEMPLATE, bizType, serialDate);

        Long serialValue = redisTemplate.opsForValue().increment(redisKey);
        if (ObjectUtil.isNull(serialValue)) {
            throw new IllegalStateException("Redis流水号生成失败");
        }

        redisTemplate.expire(redisKey, Duration.ofDays(expireDays));

        String serialText = formatSerial(serialValue, serialLength);
        String bizNo = StrUtil.format("{}{}{}", rule.getPrefix(), serialDate, serialText);

        BizNoRecord record = new BizNoRecord();
        record.setBizType(bizType);
        record.setBizNo(bizNo);
        record.setSerialDate(serialDate);
        record.setSerialValue(serialValue);
        record.setRedisKey(redisKey);
        record.setRequestId(requestId);
        record.setCreateTime(LocalDateTime.now());
        bizNoRecordMapper.insert(record);

        log.info("业务编号生成成功，bizType={}，bizNo={}，serialValue={}，redisKey={}",
                bizType, bizNo, serialValue, redisKey);

        return BusinessNoGenerateVO.builder()
                .bizType(bizType)
                .bizNo(bizNo)
                .serialDate(serialDate)
                .serialValue(serialValue)
                .redisKey(redisKey)
                .idempotent(false)
                .build();
    }

    /**
     * 根据请求ID查询已生成编号
     *
     * @param bizType   业务类型
     * @param requestId 请求ID
     * @return 编号生成结果
     */
    private BusinessNoGenerateVO getByRequestId(String bizType, String requestId) {
        if (StrUtil.isBlank(requestId)) {
            return null;
        }

        BizNoRecord record = bizNoRecordMapper.selectOne(
                Wrappers.<BizNoRecord>lambdaQuery()
                        .eq(BizNoRecord::getBizType, bizType)
                        .eq(BizNoRecord::getRequestId, requestId)
                        .last("LIMIT 1")
        );

        if (ObjectUtil.isNull(record)) {
            return null;
        }

        return BusinessNoGenerateVO.builder()
                .bizType(record.getBizType())
                .bizNo(record.getBizNo())
                .serialDate(record.getSerialDate())
                .serialValue(record.getSerialValue())
                .redisKey(record.getRedisKey())
                .idempotent(true)
                .build();
    }

    /**
     * 格式化流水号
     *
     * @param serialValue  Redis自增值
     * @param serialLength 流水号长度
     * @return 格式化后的流水号
     */
    private String formatSerial(Long serialValue, Integer serialLength) {
        Assert.isTrue(serialValue > 0, "流水号必须大于0");

        int length = ObjectUtil.defaultIfNull(serialLength, 6);
        String serialText = StrUtil.fillBefore(String.valueOf(serialValue), '0', length);

        if (serialText.length() > length) {
            log.warn("业务编号流水号超过配置长度，serialValue={}，serialLength={}", serialValue, length);
        }

        return serialText;
    }

    /**
     * 标准化业务类型
     *
     * @param bizType 业务类型
     * @return 标准化后的业务类型
     */
    private String normalizeBizType(String bizType) {
        String normalizedBizType = StrUtil.upperCase(StrUtil.trimToEmpty(bizType));
        Assert.notBlank(normalizedBizType, "业务类型不能为空");
        return normalizedBizType;
    }
}
```

这段实现里有两个关键点：

第一，Redis `increment` 负责并发安全的流水递增。即使应用部署多个实例，只要连接的是同一个 Redis，就不会生成重复流水。

第二，`requestId` 用于简单幂等控制。调用方传入同一个 `requestId` 时，会优先查询历史生成结果，避免同一次业务请求重复生成多个业务编号。

## 接口实现

接口层主要提供两个能力：生成业务编号、查询编号规则。实际业务系统内部调用时，推荐直接注入 `BusinessNoService`，不一定要通过 HTTP 接口调用。

### 生成业务编号接口

生成业务编号接口使用 `POST` 请求，接收业务类型和可选请求 ID，返回完整编号生成结果。

文件位置：`src/main/java/io/github/atengk/bizno/controller/BusinessNoController.java`

```java
package io.github.atengk.bizno.controller;

import io.github.atengk.bizno.dto.BusinessNoGenerateDTO;
import io.github.atengk.bizno.entity.BizNoRule;
import io.github.atengk.bizno.service.BusinessNoService;
import io.github.atengk.bizno.vo.BusinessNoGenerateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 业务编号接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/business-no")
public class BusinessNoController {

    private final BusinessNoService businessNoService;

    /**
     * 生成业务编号
     *
     * @param dto 编号生成请求
     * @return 编号生成结果
     */
    @PostMapping("/generate")
    public BusinessNoGenerateVO generate(@Valid @RequestBody BusinessNoGenerateDTO dto) {
        return businessNoService.generate(dto);
    }

    /**
     * 根据业务类型快速生成业务编号
     *
     * @param bizType 业务类型
     * @return 编号生成结果
     */
    @GetMapping("/generate/{bizType}")
    public BusinessNoGenerateVO generateByBizType(@PathVariable String bizType) {
        BusinessNoGenerateDTO dto = new BusinessNoGenerateDTO();
        dto.setBizType(bizType);
        return businessNoService.generate(dto);
    }

    /**
     * 查询编号规则
     *
     * @param bizType 业务类型
     * @return 编号规则
     */
    @GetMapping("/rules/{bizType}")
    public BizNoRule getRule(@PathVariable String bizType) {
        return businessNoService.getRule(bizType);
    }
}
```

调用示例：

```bash
curl -X POST 'http://localhost:8080/api/business-no/generate' \
  -H 'Content-Type: application/json' \
  -d '{
    "bizType": "ORDER",
    "requestId": "order-create-10001"
  }'
```

响应示例：

```json
{
  "bizType": "ORDER",
  "bizNo": "ORD20260515000001",
  "serialDate": "20260515",
  "serialValue": 1,
  "redisKey": "biz:no:ORDER:20260515",
  "idempotent": false
}
```

再次使用相同 `requestId` 调用时，会返回同一个业务编号：

```json
{
  "bizType": "ORDER",
  "bizNo": "ORD20260515000001",
  "serialDate": "20260515",
  "serialValue": 1,
  "redisKey": "biz:no:ORDER:20260515",
  "idempotent": true
}
```

也可以直接通过业务类型快速生成：

```bash
curl 'http://localhost:8080/api/business-no/generate/CONTRACT'
```

响应示例：

```json
{
  "bizType": "CONTRACT",
  "bizNo": "HT202605150001",
  "serialDate": "20260515",
  "serialValue": 1,
  "redisKey": "biz:no:CONTRACT:20260515",
  "idempotent": false
}
```

### 查询编号规则接口

查询编号规则接口用于确认某个业务类型当前使用的编号前缀、日期格式、流水号长度和 Redis Key 过期时间。

调用示例：

```bash
curl 'http://localhost:8080/api/business-no/rules/ORDER'
```

响应示例：

```json
{
  "id": 1,
  "bizType": "ORDER",
  "bizName": "订单编号",
  "prefix": "ORD",
  "datePattern": "yyyyMMdd",
  "serialLength": 6,
  "expireDays": 3,
  "enabled": 1,
  "remark": "订单业务编号",
  "createTime": "2026-05-15T10:00:00",
  "updateTime": "2026-05-15T10:00:00",
  "deleted": 0
}
```

业务代码中推荐这样使用：

```java
package io.github.atengk.order.service.impl;

import io.github.atengk.bizno.service.BusinessNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl {

    private final BusinessNoService businessNoService;

    /**
     * 创建订单
     *
     * @return 订单编号
     */
    public String createOrder() {
        String orderNo = businessNoService.generateNo("ORDER");

        // 这里继续保存订单数据
        return orderNo;
    }
}
```

到这里，业务编号生成器的核心功能已经完成：编号规则由数据库维护，流水号由 Redis 原子递增生成，业务代码通过统一服务获取编号。

## 使用示例

使用示例主要演示业务模块如何调用 `BusinessNoService` 生成编号。实际项目中，订单、合同、工单等模块不需要关心 Redis、流水号、补零规则，只需要传入业务类型即可。

### 订单编号生成

订单编号通常在创建订单时生成，建议在订单数据入库前生成业务编号，然后和订单主数据一起保存。

下面示例演示订单服务中生成订单编号的方式。

文件位置：`src/main/java/io/github/atengk/order/service/impl/OrderServiceImpl.java`

```java
package io.github.atengk.order.service.impl;

import io.github.atengk.bizno.service.BusinessNoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl {

    private final BusinessNoService businessNoService;

    /**
     * 创建订单
     *
     * @return 订单编号
     */
    public String createOrder() {
        String orderNo = businessNoService.generateNo("ORDER");

        // 这里可以继续执行订单落库逻辑
        log.info("订单创建成功，订单编号={}", orderNo);

        return orderNo;
    }
}
```

调用后生成的订单编号示例：

```text
ORD20260515000001
ORD20260515000002
ORD20260515000003
```

如果订单创建接口需要防止重复提交，可以传入 `requestId`：

```java
BusinessNoGenerateDTO dto = new BusinessNoGenerateDTO();
dto.setBizType("ORDER");
dto.setRequestId("order-submit-user-10001-20260515120000");

BusinessNoGenerateVO result = businessNoService.generate(dto);
String orderNo = result.getBizNo();
```

相同 `requestId` 重复调用时，会返回同一个编号，适合订单提交、支付请求等不能重复生成业务编号的场景。

### 合同编号生成

合同编号通常用于审批、归档、下载、打印等场景。合同编号的流水位数可以比订单编号短，例如 `4` 位流水号。

合同编号规则示例：

```sql
INSERT INTO biz_no_rule
(biz_type, biz_name, prefix, date_pattern, serial_length, expire_days, enabled, remark)
VALUES
('CONTRACT', '合同编号', 'HT', 'yyyyMMdd', 4, 3, 1, '合同业务编号');
```

合同服务中可以直接调用编号生成服务。

文件位置：`src/main/java/io/github/atengk/contract/service/impl/ContractServiceImpl.java`

```java
package io.github.atengk.contract.service.impl;

import io.github.atengk.bizno.service.BusinessNoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 合同服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl {

    private final BusinessNoService businessNoService;

    /**
     * 创建合同
     *
     * @return 合同编号
     */
    public String createContract() {
        String contractNo = businessNoService.generateNo("CONTRACT");

        // 这里可以继续执行合同保存、审批流发起等逻辑
        log.info("合同创建成功，合同编号={}", contractNo);

        return contractNo;
    }
}
```

生成结果示例：

```text
HT202605150001
HT202605150002
HT202605150003
```

合同编号使用 `HT` 作为前缀，流水号长度为 `4`，所以编号比订单编号更短。

### 工单编号生成

工单编号适合客服、售后、运维、内部流程等场景。工单通常需要快速检索，所以保留日期和递增流水号会比较直观。

工单编号规则示例：

```sql
INSERT INTO biz_no_rule
(biz_type, biz_name, prefix, date_pattern, serial_length, expire_days, enabled, remark)
VALUES
('WORK_ORDER', '工单编号', 'WO', 'yyyyMMdd', 6, 3, 1, '客服或运维工单编号');
```

工单服务中生成编号的方式如下。

文件位置：`src/main/java/io/github/atengk/workorder/service/impl/WorkOrderServiceImpl.java`

```java
package io.github.atengk.workorder.service.impl;

import io.github.atengk.bizno.dto.BusinessNoGenerateDTO;
import io.github.atengk.bizno.service.BusinessNoService;
import io.github.atengk.bizno.vo.BusinessNoGenerateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 工单服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl {

    private final BusinessNoService businessNoService;

    /**
     * 创建工单
     *
     * @param userId 用户ID
     * @return 工单编号
     */
    public String createWorkOrder(Long userId) {
        BusinessNoGenerateDTO dto = new BusinessNoGenerateDTO();
        dto.setBizType("WORK_ORDER");
        dto.setRequestId("work-order-" + userId + "-" + System.currentTimeMillis());

        BusinessNoGenerateVO result = businessNoService.generate(dto);

        // 这里可以继续执行工单保存、通知客服等逻辑
        log.info("工单创建成功，用户ID={}，工单编号={}", userId, result.getBizNo());

        return result.getBizNo();
    }
}
```

生成结果示例：

```text
WO20260515000001
WO20260515000002
WO20260515000003
```

也可以通过 HTTP 接口直接生成工单编号：

```bash
curl -X POST 'http://localhost:8080/api/business-no/generate' \
  -H 'Content-Type: application/json' \
  -d '{
    "bizType": "WORK_ORDER",
    "requestId": "work-order-user-10001"
  }'
```

响应示例：

```json
{
  "bizType": "WORK_ORDER",
  "bizNo": "WO20260515000001",
  "serialDate": "20260515",
  "serialValue": 1,
  "redisKey": "biz:no:WORK_ORDER:20260515",
  "idempotent": false
}
```

## 功能验证

功能验证主要验证三件事：编号能正常生成、高并发下不会重复、日期维度变化后流水号会重新开始。

验证前需要确认 MySQL 中已经初始化编号规则，并且 Redis 可以正常连接。

### 正常生成验证

先通过接口生成一个订单编号。

```bash
curl -X POST 'http://localhost:8080/api/business-no/generate' \
  -H 'Content-Type: application/json' \
  -d '{
    "bizType": "ORDER"
  }'
```

正常响应示例：

```json
{
  "bizType": "ORDER",
  "bizNo": "ORD20260515000001",
  "serialDate": "20260515",
  "serialValue": 1,
  "redisKey": "biz:no:ORDER:20260515",
  "idempotent": false
}
```

继续调用一次：

```bash
curl -X POST 'http://localhost:8080/api/business-no/generate' \
  -H 'Content-Type: application/json' \
  -d '{
    "bizType": "ORDER"
  }'
```

响应中的流水值应该递增：

```json
{
  "bizType": "ORDER",
  "bizNo": "ORD20260515000002",
  "serialDate": "20260515",
  "serialValue": 2,
  "redisKey": "biz:no:ORDER:20260515",
  "idempotent": false
}
```

可以通过 Redis 查看当前流水值：

```bash
redis-cli GET biz:no:ORDER:20260515
```

如果返回：

```text
2
```

说明 Redis 自增流水正常。

也可以查询数据库流水记录：

```sql
SELECT
    biz_type,
    biz_no,
    serial_date,
    serial_value,
    redis_key,
    request_id,
    create_time
FROM biz_no_record
WHERE biz_type = 'ORDER'
ORDER BY id DESC
LIMIT 10;
```

正常情况下可以看到刚才生成的业务编号记录。

### 并发生成验证

并发验证用于确认多个线程同时生成编号时不会出现重复编号。这里使用 JUnit 测试直接调用 `BusinessNoService`，模拟 100 个线程同时生成订单编号。

文件位置：`src/test/java/io/github/atengk/bizno/BusinessNoConcurrentTest.java`

```java
package io.github.atengk.bizno;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.bizno.service.BusinessNoService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 业务编号并发生成测试
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@SpringBootTest
class BusinessNoConcurrentTest {

    @Resource
    private BusinessNoService businessNoService;

    /**
     * 并发生成订单编号
     *
     * @throws InterruptedException 中断异常
     */
    @Test
    void testConcurrentGenerateOrderNo() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        Set<String> noSet = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    String orderNo = businessNoService.generateNo("ORDER");
                    noSet.add(orderNo);
                    log.info("生成订单编号：{}", orderNo);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        boolean completed = countDownLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        List<String> noList = CollUtil.newArrayList(noSet);
        log.info("并发生成完成，预期数量={}，实际去重数量={}", threadCount, noList.size());

        Assertions.assertTrue(completed, "并发生成任务未在规定时间内完成");
        Assertions.assertEquals(threadCount, noSet.size(), "存在重复业务编号");
    }
}
```

运行测试：

```bash
mvn test -Dtest=BusinessNoConcurrentTest
```

验证结果重点看两个地方：

```text
预期数量=100，实际去重数量=100
```

如果预期数量和实际去重数量一致，说明并发生成没有重复编号。

也可以通过 SQL 验证最近生成的订单编号数量：

```sql
SELECT
    COUNT(*) AS total_count,
    COUNT(DISTINCT biz_no) AS distinct_count
FROM biz_no_record
WHERE biz_type = 'ORDER'
  AND create_time >= DATE_SUB(NOW(), INTERVAL 10 MINUTE);
```

正常情况下 `total_count` 和 `distinct_count` 应该一致。

### 日期切换验证

日期切换验证用于确认不同日期使用不同 Redis Key，日期变化后流水号会从 `1` 重新开始。

生产环境中订单编号一般使用 `yyyyMMdd`，每天一个流水 Key：

```text
biz:no:ORDER:20260515
biz:no:ORDER:20260516
```

日期从 `20260515` 切换到 `20260516` 后，Redis Key 发生变化，所以新日期的流水会重新从 `1` 开始。

为了方便本地快速验证，不建议修改服务器系统时间，可以临时把测试业务类型的 `date_pattern` 改成 `yyyyMMddHHmmss`，让每秒生成一个新的 Redis Key，用“秒级切换”模拟“日期切换”。

先新增一个测试规则：

```sql
INSERT INTO biz_no_rule
(biz_type, biz_name, prefix, date_pattern, serial_length, expire_days, enabled, remark)
VALUES
('DATE_TEST', '日期切换测试编号', 'DT', 'yyyyMMddHHmmss', 4, 1, 1, '用于模拟日期切换验证');
```

第一次调用：

```bash
curl -X POST 'http://localhost:8080/api/business-no/generate' \
  -H 'Content-Type: application/json' \
  -d '{
    "bizType": "DATE_TEST"
  }'
```

响应示例：

```json
{
  "bizType": "DATE_TEST",
  "bizNo": "DT202605151430010001",
  "serialDate": "20260515143001",
  "serialValue": 1,
  "redisKey": "biz:no:DATE_TEST:20260515143001",
  "idempotent": false
}
```

等待 1 秒后再次调用：

```bash
curl -X POST 'http://localhost:8080/api/business-no/generate' \
  -H 'Content-Type: application/json' \
  -d '{
    "bizType": "DATE_TEST"
  }'
```

响应示例：

```json
{
  "bizType": "DATE_TEST",
  "bizNo": "DT202605151430020001",
  "serialDate": "20260515143002",
  "serialValue": 1,
  "redisKey": "biz:no:DATE_TEST:20260515143002",
  "idempotent": false
}
```

如果两次响应中的 `serialDate` 不同，并且 `serialValue` 都是 `1`，说明编号生成器会按日期维度切换 Redis Key，日期变化后流水会重新开始。

验证 Redis Key：

```bash
redis-cli KEYS 'biz:no:DATE_TEST:*'
```

可能返回：

```text
biz:no:DATE_TEST:20260515143001
biz:no:DATE_TEST:20260515143002
```

验证完成后，可以删除测试规则，避免误用：

```sql
UPDATE biz_no_rule
SET deleted = 1,
    enabled = 0
WHERE biz_type = 'DATE_TEST';
```

也可以删除测试 Redis Key：

```bash
redis-cli DEL biz:no:DATE_TEST:20260515143001
redis-cli DEL biz:no:DATE_TEST:20260515143002
```

对于正式业务规则，例如 `ORDER`，只要 `date_pattern = 'yyyyMMdd'`，系统会自然按天生成不同 Redis Key：

```text
biz:no:ORDER:20260515
biz:no:ORDER:20260516
```

因此不需要额外定时任务重置流水号，日期字符串变化后，Redis Key 自动变化，流水号自然重新从 `1` 开始。