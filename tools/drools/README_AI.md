# Spring Boot 集成 Drools 规则引擎开发

Drools 是 Java 生态中常用的业务规则引擎，适合将经常变化、条件组合复杂、需要与业务代码解耦的判断逻辑抽离为规则文件。本文档面向 Spring Boot 3 项目，说明如何在后端服务中集成 Drools，用于规则加载、规则执行和业务决策处理。Apache KIE 当前是 Drools、jBPM、Kogito 等业务自动化开源技术的项目主页，Drools 相关依赖仍可通过 Maven Central 引入。([drools.org](https://www.drools.org/learn/documentation.html))

## 模块概述

本模块用于在 Spring Boot 3 项目中集成 Drools 规则引擎，将业务判断逻辑从 Java 代码中抽离到 DRL 规则文件中。应用启动后加载规则文件，业务请求进入系统后转换为 Fact 对象，交由 Drools 执行匹配，最终返回规则命中结果、计算结果或业务处理建议。

### 功能定位

该模块定位为业务规则执行层，不直接承担数据库持久化、权限认证、流程编排等职责。它主要负责接收业务输入、构造规则执行上下文、执行 Drools 规则、收集规则输出，并将结果返回给上层业务服务。

在典型分层中，Drools 模块可以放在 `rule` 或 `drools` 业务包下，由 `Controller` 接收外部请求，由 `Service` 组织入参和出参，由 Drools 配置类负责初始化 `KieContainer`、`KieSession` 等核心对象。规则文件统一放在 `src/main/resources/rules` 目录，便于版本管理和后续热更新扩展。

该模块的核心价值是降低硬编码判断逻辑对业务代码的侵入。例如价格计算、风控审核、优惠活动、等级评定、告警判定等规则经常变化，如果全部写在 Java `if else` 中，会导致代码难以维护、测试成本高、上线风险大。通过 Drools，可以将规则表达和规则执行拆开，使业务代码保持稳定，规则内容可以独立维护。

### 使用场景

Drools 适合规则多、条件组合复杂、变更频繁、需要可解释命中结果的业务场景。它不适合替代普通 CRUD，也不适合处理非常简单且长期稳定的判断逻辑。

常见使用场景包括：

| 场景     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 风控规则 | 根据用户等级、交易金额、交易频次、设备信息、地区信息判断是否拦截或人工审核 |
| 优惠规则 | 根据用户类型、订单金额、商品类别、活动时间计算优惠金额或可用优惠券 |
| 会员等级 | 根据消费金额、积分、活跃天数、订单数量计算会员等级           |
| 审批规则 | 根据申请金额、部门、岗位、历史记录判断审批流向或审批级别     |
| 计费规则 | 根据套餐、用量、阶梯价格、折扣条件计算费用                   |
| 告警规则 | 根据监控指标、阈值、持续时间、业务状态判断是否触发告警       |
| 推荐策略 | 根据标签、行为、权重和业务约束生成推荐结果或过滤结果         |

使用 Drools 时需要注意边界：规则引擎适合处理“判断、匹配、推导、计算”类逻辑，不建议在规则文件中直接执行复杂数据库操作、远程接口调用或事务处理。规则文件中应尽量只表达业务条件和结果赋值，外部依赖调用应放在 Spring Service 层完成。

### 技术选型

本模块采用 Spring Boot 3 + Drools + Maven 的集成方式。Spring Boot 3 官方要求至少 Java 17，并依赖 Spring Framework 6.x；当前 Spring Boot 3.4 系列文档中也明确了 Maven 3.6.3+、Gradle 7.x/8.x 的构建工具要求。([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-boot/system-requirements.html))

推荐技术组合如下：

| 技术              | 推荐版本                       | 用途                                   |
| ----------------- | ------------------------------ | -------------------------------------- |
| Java              | 17 或 21                       | Spring Boot 3 的基础运行环境           |
| Spring Boot       | 3.4.x 或 3.5.x                 | Web 服务、配置管理、依赖注入、测试支持 |
| Drools            | 10.1.0 或按企业稳定版本锁定    | 规则引擎核心能力                       |
| Maven             | 3.6.3+                         | 项目构建和依赖管理                     |
| Lombok            | 与 Spring Boot 3 兼容版本      | 简化 DTO、VO、配置类代码               |
| Hutool            | 5.8.x                          | 日期、集合、字符串、对象校验等工具能力 |
| Spring Validation | Spring Boot Starter Validation | 请求参数校验                           |

Drools 依赖可以选择两种方式：一种是 `drools-engine-classic`，偏传统 DRL 使用方式；另一种是 `drools-engine`，它是基于 executable model 的 Drools 引擎聚合模块。Maven Central 中 `drools-engine` 的描述是基于 executable model 进行规则评估的引擎聚合模块，`drools-engine-classic` 和 `drools-model-compiler` 均已有 10.1.0 版本目录。([Maven Central](https://central.sonatype.com/artifact/org.drools/drools-engine/10.0.0))

对于普通 Spring Boot 3 后端项目，如果主要编写 `.drl` 文件并通过 `KieSession` 执行规则，建议先使用 `drools-engine-classic`，学习成本更低，集成路径更直接。

## 环境与依赖

本节定义项目运行环境、Maven 依赖和推荐目录结构。后续规则文件设计、Drools 配置、业务规则实现和接口设计都基于本节约定展开。

### Spring Boot 3 环境要求

Spring Boot 3 项目建议统一使用 Java 17 或 Java 21。Java 17 是 Spring Boot 3 的最低要求，Java 21 更适合新项目长期维护。构建工具建议使用 Maven 3.6.3 及以上版本，避免由于插件版本过旧导致构建失败。([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-boot/system-requirements.html))

推荐基础环境如下：

| 环境项      | 推荐值              | 说明                                       |
| ----------- | ------------------- | ------------------------------------------ |
| JDK         | 17 或 21            | Spring Boot 3 最低要求 Java 17             |
| Spring Boot | 3.4.x / 3.5.x       | 保持 Spring Framework 6.x、Jakarta EE 兼容 |
| Maven       | 3.6.3+              | 用于依赖管理和打包                         |
| IDE         | IntelliJ IDEA 2024+ | 便于识别 Spring Boot 3 和 Java 17+         |
| 编码        | UTF-8               | 避免 DRL 中文注释或中文字符串乱码          |
| 包路径      | `io.github.atengk`  | 示例项目默认基础包路径                     |

可以通过以下命令检查本地环境版本：

```bash
java -version
mvn -version
```

`java -version` 用于确认当前 JDK 主版本是否为 17 或 21；`mvn -version` 用于确认 Maven 版本以及 Maven 当前绑定的 Java 版本。如果 Maven 显示的 Java 版本不是 17 及以上，需要检查 `JAVA_HOME` 和系统环境变量配置。

### Drools 核心依赖

下面给出 Maven 依赖配置，适合 Spring Boot 3 Web 项目直接集成 Drools。示例中锁定 Drools 版本，避免多个 Drools 子模块版本不一致导致运行时冲突。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Java 版本：Spring Boot 3 最低要求 Java 17 -->
    <java.version>17</java.version>

    <!-- Drools 版本：统一管理 Drools 相关依赖版本 -->
    <drools.version>10.1.0</drools.version>

    <!-- Hutool 工具类版本：用于字符串、集合、对象、日期等通用处理 -->
    <hutool.version>5.8.36</hutool.version>
</properties>

<dependencies>
    <!-- Spring Boot Web：提供 REST 接口、JSON 序列化、内嵌 Tomcat 等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于 Controller 请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Drools 经典规则引擎：适合 DRL 文件 + KieSession 的常规集成方式 -->
    <dependency>
        <groupId>org.drools</groupId>
        <artifactId>drools-engine-classic</artifactId>
        <version>${drools.version}</version>
    </dependency>

    <!-- Drools 模型编译器：支持规则模型编译，建议与 Drools 核心版本保持一致 -->
    <dependency>
        <groupId>org.drools</groupId>
        <artifactId>drools-model-compiler</artifactId>
        <version>${drools.version}</version>
    </dependency>

    <!-- KIE API：提供 KieServices、KieContainer、KieSession 等核心接口 -->
    <dependency>
        <groupId>org.kie</groupId>
        <artifactId>kie-api</artifactId>
        <version>${drools.version}</version>
    </dependency>

    <!-- Hutool：常用 Java 工具类，便于处理字符串、集合、日期、对象判空等逻辑 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：简化 DTO、VO、配置类中的 getter、setter、构造器等代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：提供单元测试、集成测试、MockMvc 等测试能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目希望使用 executable model 方式，也可以将 `drools-engine-classic` 替换为 `drools-engine`。但对于以 DRL 文件为主的入门集成，`drools-engine-classic` 更符合常见开发习惯。

基础配置文件可以先保留规则路径、是否启用动态加载、默认规则包等配置项，后续 Drools 配置类会读取这些参数。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    # 应用名称：用于日志、监控和服务识别
    name: spring-boot-drools-demo

drools:
  # 规则文件所在目录，默认从 classpath 下读取
  rule-path: rules

  # 默认规则文件，后续可扩展为多规则文件加载
  default-rule-file: rules/order-discount.drl

  # 是否启用动态规则加载；本地 classpath 规则建议先关闭
  dynamic-enabled: false

logging:
  level:
    # 项目业务日志级别
    io.github.atengk: info

    # Drools 相关日志，排查规则加载问题时可临时调整为 debug
    org.drools: info
    org.kie: info
```

### 项目目录结构

项目结构建议按 Spring Boot 标准分层组织，同时将规则文件与 Java 代码分离。Java 代码负责输入输出、规则执行流程和异常处理，DRL 文件只负责表达规则条件和规则动作。

推荐目录结构如下：

```text
spring-boot-drools-demo
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               ├── DroolsApplication.java
│   │   │               ├── common
│   │   │               │   ├── exception
│   │   │               │   │   ├── GlobalExceptionHandler.java
│   │   │               │   │   └── RuleExecuteException.java
│   │   │               │   └── result
│   │   │               │       └── Result.java
│   │   │               └── rule
│   │   │                   ├── config
│   │   │                   │   ├── DroolsProperties.java
│   │   │                   │   └── DroolsConfig.java
│   │   │                   ├── controller
│   │   │                   │   └── RuleExecuteController.java
│   │   │                   ├── dto
│   │   │                   │   └── OrderRuleRequest.java
│   │   │                   ├── fact
│   │   │                   │   └── OrderFact.java
│   │   │                   ├── service
│   │   │                   │   └── RuleExecuteService.java
│   │   │                   ├── service
│   │   │                   │   └── impl
│   │   │                   │       └── RuleExecuteServiceImpl.java
│   │   │                   └── vo
│   │   │                       └── OrderRuleResult.java
│   │   └── resources
│   │       ├── application.yml
│   │       └── rules
│   │           ├── order-discount.drl
│   │           ├── risk-check.drl
│   │           └── member-level.drl
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── rule
│                           └── RuleExecuteServiceTest.java
```

目录职责说明：

| 目录 / 文件        | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| `rule/config`      | Drools 配置类，负责创建 `KieServices`、`KieContainer`、`KieSession` |
| `rule/controller`  | 对外提供规则执行接口                                         |
| `rule/dto`         | 接收接口请求参数                                             |
| `rule/fact`        | Drools 规则执行时插入 Working Memory 的事实对象              |
| `rule/service`     | 封装规则执行流程                                             |
| `rule/vo`          | 返回规则执行结果                                             |
| `resources/rules`  | 存放 `.drl` 规则文件                                         |
| `common/exception` | 统一异常处理和规则执行异常                                   |
| `common/result`    | 统一接口返回结构                                             |
| `test/java`        | 规则加载、规则命中、异常场景测试                             |

后续开发中建议遵循一个原则：业务输入先转换为 `Fact`，规则命中结果写入 `Fact` 或结果对象，规则执行结束后由 Service 层统一组装响应结果。不要让 DRL 文件直接依赖 Controller、Mapper 或远程接口，这样规则文件更稳定，也更容易测试。



## 规则文件设计

规则文件设计是 Drools 集成中最核心的部分。Java 代码负责提供事实对象和执行上下文，DRL 文件负责表达业务条件、命中规则和输出动作。建议将 DRL 文件设计为“可读、可测、可版本化”的业务规则载体，不要在规则文件中写复杂外部调用或数据库访问逻辑。

### DRL 规则文件结构

DRL 文件通常由 `package`、`import`、`global`、`rule` 等部分组成。一个规则文件可以包含多条规则，但建议按业务主题拆分，例如订单优惠规则、风控规则、会员等级规则分别放在不同文件中。

文件位置：`src/main/resources/rules/order-discount.drl`

```java
package rules.order

import io.github.atengk.rule.fact.OrderFact;
import io.github.atengk.rule.fact.RuleHit;
import java.math.BigDecimal;

/**
 * 订单优惠规则文件
 * 用于根据订单金额、会员等级、是否首单等条件计算优惠命中结果
 */

rule "满1000订单优惠"
    salience 100
    no-loop true
when
    $order : OrderFact(
        orderAmount != null
    )
    eval($order.getOrderAmount().compareTo(new BigDecimal("1000")) >= 0)
then
    $order.addHit(new RuleHit(
        "ORDER_AMOUNT_DISCOUNT",
        drools.getRule().getName(),
        "订单金额满1000，优惠100元",
        new BigDecimal("100"),
        100
    ));
end

rule "黄金会员优惠"
    salience 90
    no-loop true
when
    $order : OrderFact(
        memberLevel == "GOLD",
        orderAmount != null
    )
then
    $order.addHit(new RuleHit(
        "GOLD_MEMBER_DISCOUNT",
        drools.getRule().getName(),
        "黄金会员额外优惠50元",
        new BigDecimal("50"),
        90
    ));
end

rule "首单优惠"
    salience 80
    no-loop true
when
    $order : OrderFact(
        firstOrder == true,
        orderAmount != null
    )
then
    $order.addHit(new RuleHit(
        "FIRST_ORDER_DISCOUNT",
        drools.getRule().getName(),
        "用户首单优惠30元",
        new BigDecimal("30"),
        80
    ));
end
```

该规则文件使用 `OrderFact` 作为事实对象，规则命中后通过 `addHit` 方法写入命中结果。`salience` 用于控制规则执行优先级，数值越大越优先执行；`no-loop true` 用于避免规则动作修改对象后导致自身重复触发。

### 规则包与命名规范

规则包和规则名称需要能体现业务归属，便于排查日志、定位规则和做版本管理。建议不要使用过于宽泛的名称，例如 `rule1`、`testRule`、`discount`，而应使用具备业务语义的名称。

推荐命名规范如下：

| 类型        | 规范                       | 示例                                   |
| ----------- | -------------------------- | -------------------------------------- |
| 规则文件    | 业务域-规则主题.drl        | `order-discount.drl`、`risk-check.drl` |
| DRL package | `rules.业务域`             | `rules.order`、`rules.risk`            |
| 规则名称    | 中文业务含义或英文业务编码 | `满1000订单优惠`、`高风险订单拦截`     |
| 规则编码    | 大写下划线                 | `ORDER_AMOUNT_DISCOUNT`                |
| Fact 类     | 业务名 + `Fact`            | `OrderFact`、`RiskFact`                |
| 规则结果    | 业务名 + `RuleResult`      | `OrderRuleResult`                      |

如果一个业务模块规则较多，建议按业务能力拆分文件：

```text
src/main/resources/rules
├── order-discount.drl      # 订单优惠规则
├── order-limit.drl         # 订单限购规则
├── risk-check.drl          # 风控审核规则
├── member-level.drl        # 会员等级规则
└── price-calculate.drl     # 价格计算规则
```

实际项目中建议规则编码保持全局唯一。规则命中结果、审计日志、接口响应、规则版本管理都应使用规则编码作为主要识别字段，而不是仅依赖规则名称。

### Fact 对象设计

Fact 是 Drools 规则执行时插入 Working Memory 的事实对象。它应尽量保持纯数据结构，不直接依赖数据库、HTTP 客户端、Mapper 或复杂业务 Service。规则需要判断的数据应在进入规则引擎之前准备完成。

文件位置：`src/main/java/io/github/atengk/rule/fact/RuleHit.java`

```java
package io.github.atengk.rule.fact;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 规则命中结果
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleHit {

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 命中说明
     */
    private String message;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 优先级
     */
    private Integer priority;
}
```

下面的 Fact 对象用于承载订单规则判断所需数据，并保存规则命中结果。

文件位置：`src/main/java/io/github/atengk/rule/fact/OrderFact.java`

```java
package io.github.atengk.rule.fact;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单规则事实对象
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class OrderFact {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 会员等级，例如 NORMAL、SILVER、GOLD、PLATINUM
     */
    private String memberLevel;

    /**
     * 订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 是否首单
     */
    private Boolean firstOrder;

    /**
     * 规则命中结果
     */
    private List<RuleHit> hits = new ArrayList<>();

    /**
     * 添加规则命中结果
     *
     * @param hit 规则命中结果
     */
    public void addHit(RuleHit hit) {
        if (hit == null) {
            return;
        }
        if (CollUtil.isEmpty(this.hits)) {
            this.hits = new ArrayList<>();
        }
        this.hits.add(hit);
    }
}
```

Fact 对象设计时建议遵循以下原则：

| 原则         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 字段稳定     | Fact 字段应围绕规则判断设计，避免频繁重构                    |
| 类型明确     | 金额使用 `BigDecimal`，时间使用 `LocalDateTime`，状态使用明确编码 |
| 避免外部依赖 | 不在 Fact 中注入 Service、Mapper 或 Feign Client             |
| 保留结果容器 | 可在 Fact 中保留 `hits`、`riskTags`、`actions` 等结果集合    |
| 便于测试     | Fact 能直接在单元测试中构造，不依赖 Spring 容器              |

### 规则条件与执行动作

规则条件写在 `when` 部分，执行动作写在 `then` 部分。规则条件应只表达判断逻辑，执行动作应只做结果赋值、命中记录、简单计算，不建议在 `then` 中执行复杂业务操作。

规则条件示例：

```java
when
    $order : OrderFact(
        memberLevel == "GOLD",
        firstOrder == true,
        orderAmount != null
    )
    eval($order.getOrderAmount().compareTo(new BigDecimal("500")) >= 0)
then
    $order.addHit(new RuleHit(
        "GOLD_FIRST_ORDER_DISCOUNT",
        drools.getRule().getName(),
        "黄金会员首单满500优惠80元",
        new BigDecimal("80"),
        110
    ));
end
```

常用规则写法说明：

| 写法                         | 说明                                      |
| ---------------------------- | ----------------------------------------- |
| `$order : OrderFact(...)`    | 匹配一个订单事实对象，并绑定变量 `$order` |
| `memberLevel == "GOLD"`      | 判断字段值                                |
| `firstOrder == true`         | 判断布尔值                                |
| `orderAmount != null`        | 避免空指针                                |
| `eval(...)`                  | 执行 Java 表达式，适合 BigDecimal 比较    |
| `drools.getRule().getName()` | 获取当前命中的规则名称                    |
| `$order.addHit(...)`         | 将规则命中结果写入 Fact                   |

如果规则之间是互斥关系，可以使用 `activation-group`。同一个 `activation-group` 中只会执行一条规则，通常配合 `salience` 使用。

```java
rule "白金会员优惠"
    salience 100
    activation-group "member-discount"
when
    $order : OrderFact(memberLevel == "PLATINUM")
then
    $order.addHit(new RuleHit(
        "PLATINUM_MEMBER_DISCOUNT",
        drools.getRule().getName(),
        "白金会员优惠100元",
        new BigDecimal("100"),
        100
    ));
end

rule "黄金会员优惠"
    salience 90
    activation-group "member-discount"
when
    $order : OrderFact(memberLevel == "GOLD")
then
    $order.addHit(new RuleHit(
        "GOLD_MEMBER_DISCOUNT",
        drools.getRule().getName(),
        "黄金会员优惠50元",
        new BigDecimal("50"),
        90
    ));
end
```

如果规则是可叠加关系，不要设置同一个 `activation-group`，让多条规则都能命中，再由 Java Service 统一汇总结果。

## Drools 核心配置

Drools 核心配置负责将 DRL 文件加载到规则引擎中，并向业务层提供可执行的 `KieSession`。在 Spring Boot 项目中，推荐将 Drools 初始化逻辑封装为配置类，通过 Bean 方式注入业务服务。

### KieServices 初始化

`KieServices` 是 Drools 的入口对象，用于创建 `KieFileSystem`、`KieBuilder`、`KieContainer` 等核心组件。项目中通常不需要将 `KieServices` 暴露给业务层，配置类内部使用即可。

先定义 Drools 配置属性，便于后续调整规则目录和加载方式。

文件位置：`src/main/java/io/github/atengk/rule/config/DroolsProperties.java`

```java
package io.github.atengk.rule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Drools 配置属性
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@ConfigurationProperties(prefix = "drools")
public class DroolsProperties {

    /**
     * 规则文件目录，默认读取 classpath:rules
     */
    private String rulePath = "rules";

    /**
     * 是否启用动态加载
     */
    private Boolean dynamicEnabled = false;
}
```

配置文件中增加对应配置。

文件位置：`src/main/resources/application.yml`

```yaml
drools:
  # 规则文件目录，加载 classpath 下 rules 目录中的 DRL 文件
  rule-path: rules

  # 是否启用动态规则加载；当前示例先使用启动时加载
  dynamic-enabled: false
```

### KieContainer 配置

`KieContainer` 是规则容器，负责承载已经编译完成的规则资源。Spring Boot 项目中建议将 `KieContainer` 注册为单例 Bean，业务执行时再按请求创建 `KieSession`。

下面配置类会扫描 `classpath:rules/**/*.drl`，将所有规则文件写入 `KieFileSystem`，然后构建 `KieContainer`。

文件位置：`src/main/java/io/github/atengk/rule/config/DroolsConfig.java`

```java
package io.github.atengk.rule.config;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.runtime.KieContainer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Drools 核心配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(DroolsProperties.class)
public class DroolsConfig {

    private final DroolsProperties droolsProperties;

    /**
     * 创建 KieContainer
     *
     * @return KieContainer 规则容器
     * @throws IOException 规则文件读取异常
     */
    @Bean
    public KieContainer kieContainer() throws IOException {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        String rulePath = StrUtil.blankToDefault(droolsProperties.getRulePath(), "rules");
        String locationPattern = StrUtil.format("classpath*:{}/**/*.drl", rulePath);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(locationPattern);

        if (resources.length == 0) {
            log.warn("未扫描到Drools规则文件，规则路径：{}", locationPattern);
        }

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (StrUtil.isBlank(filename)) {
                continue;
            }

            String kiePath = StrUtil.format("src/main/resources/{}/{}", rulePath, filename);
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            kieFileSystem.write(kiePath, content);

            log.info("加载Drools规则文件：{}", kiePath);
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll();
        Results results = kieBuilder.getResults();

        if (results.hasMessages(Message.Level.ERROR)) {
            log.error("Drools规则文件编译失败：{}", results.getMessages());
            throw new IllegalStateException("Drools规则文件编译失败：" + results.getMessages());
        }

        ReleaseId releaseId = kieServices.getRepository().getDefaultReleaseId();
        KieContainer kieContainer = kieServices.newKieContainer(releaseId);

        log.info("Drools规则容器初始化完成，规则文件数量：{}", resources.length);
        return kieContainer;
    }
}
```

该配置方式不依赖 `kmodule.xml`，适合 Spring Boot 项目直接从 `resources/rules` 目录加载 DRL 文件。规则文件变多后，可以继续按业务目录拆分，例如 `rules/order`、`rules/risk`、`rules/member`。

### KieSession 创建与释放

`KieSession` 是规则执行会话，用于插入 Fact、触发规则、获取规则执行结果。`KieSession` 不建议作为单例 Bean 复用，推荐在每次规则执行时创建，执行完成后调用 `dispose()` 释放资源。

规则执行基本流程如下：

```java
KieSession kieSession = kieContainer.newKieSession();
try {
    kieSession.insert(orderFact);
    int count = kieSession.fireAllRules();
} finally {
    kieSession.dispose();
}
```

关键点说明：

| 步骤              | 说明                              |
| ----------------- | --------------------------------- |
| `newKieSession()` | 创建一次规则执行会话              |
| `insert(fact)`    | 将业务事实对象插入 Working Memory |
| `fireAllRules()`  | 执行所有满足条件的规则            |
| `dispose()`       | 释放会话资源，避免内存泄漏        |

如果某些业务场景不需要状态会话，也可以考虑 `StatelessKieSession`。但在需要多条规则叠加、规则之间存在结果传递、执行后需要读取 Fact 变化的场景中，`KieSession` 更直观。

### 规则文件加载方式

Spring Boot 集成 Drools 常见规则加载方式有三类：classpath 启动加载、本地文件动态加载、数据库动态加载。初期项目建议先使用 classpath 启动加载，稳定后再扩展动态加载能力。

| 加载方式           | 适用场景                       | 说明                                    |
| ------------------ | ------------------------------ | --------------------------------------- |
| classpath 启动加载 | 中小型项目、规则变更随版本发布 | 规则文件放在 `src/main/resources/rules` |
| 本地文件动态加载   | 规则由运维或配置中心下发       | 从外部目录读取 `.drl` 文件              |
| 数据库动态加载     | 规则平台化管理                 | 规则内容存储在数据库，运行时编译        |
| 配置中心加载       | 多环境规则统一管理             | 从 Nacos、Apollo 等配置中心读取         |

当前示例采用 classpath 启动加载：

```text
src/main/resources/rules/order-discount.drl
src/main/resources/rules/risk-check.drl
src/main/resources/rules/member-level.drl
```

如果后续需要热更新，可以新增 `DroolsRuleLoader` 组件，在规则文件变化或管理后台发布规则后重新构建 `KieContainer`。热更新时不建议修改正在执行的 `KieSession`，而应构建新的 `KieContainer`，再通过引用切换的方式替换旧容器。

## 业务规则实现

业务规则实现层负责将接口请求转换为 Fact，调用 Drools 执行规则，然后将规则命中结果转换为接口响应。该层不应把大量业务判断重新写回 Java 代码，否则会削弱规则引擎的价值。

### 规则输入模型

输入模型用于接收规则执行请求。DTO 中应包含规则判断所需的最小字段，并通过参数校验保证关键字段不为空。

文件位置：`src/main/java/io/github/atengk/rule/dto/OrderRuleRequest.java`

```java
package io.github.atengk.rule.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单规则执行请求
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class OrderRuleRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 会员等级：NORMAL、SILVER、GOLD、PLATINUM
     */
    @NotBlank(message = "会员等级不能为空")
    private String memberLevel;

    /**
     * 订单金额
     */
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal orderAmount;

    /**
     * 是否首单
     */
    @NotNull(message = "是否首单不能为空")
    private Boolean firstOrder;
}
```

DTO 不建议直接作为 Drools Fact 使用。接口入参更关注外部请求格式，Fact 更关注规则执行上下文。两者分开有利于后续补充内部字段，例如用户标签、历史订单数、风控状态等。

### 规则输出模型

输出模型用于返回规则执行后的业务结果，包括规则命中数量、命中规则列表、总优惠金额、最终应付金额等。

文件位置：`src/main/java/io/github/atengk/rule/vo/OrderRuleResult.java`

```java
package io.github.atengk.rule.vo;

import io.github.atengk.rule.fact.RuleHit;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单规则执行结果
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Builder
public class OrderRuleResult {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 原始订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 总优惠金额
     */
    private BigDecimal totalDiscountAmount;

    /**
     * 最终应付金额
     */
    private BigDecimal payableAmount;

    /**
     * 命中规则数量
     */
    private Integer hitCount;

    /**
     * 命中规则列表
     */
    private List<RuleHit> hits;
}
```

输出模型应尽量保持明确，不建议直接返回整个 Fact。Fact 中可能包含内部字段，直接暴露给接口调用方会增加接口不稳定性。

### 规则执行服务

规则执行服务负责完成 DTO 到 Fact 的转换、调用 Drools、汇总命中结果和组装响应。这里使用 `KieContainer` 创建临时 `KieSession`，每次请求独立执行，执行完成立即释放。

文件位置：`src/main/java/io/github/atengk/rule/service/RuleExecuteService.java`

```java
package io.github.atengk.rule.service;

import io.github.atengk.rule.dto.OrderRuleRequest;
import io.github.atengk.rule.vo.OrderRuleResult;

/**
 * 规则执行服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface RuleExecuteService {

    /**
     * 执行订单规则
     *
     * @param request 订单规则执行请求
     * @return 订单规则执行结果
     */
    OrderRuleResult executeOrderRule(OrderRuleRequest request);
}
```

下面是订单规则执行的核心实现，包含规则执行、命中结果排序、优惠金额汇总和最终金额计算。

文件位置：`src/main/java/io/github/atengk/rule/service/impl/RuleExecuteServiceImpl.java`

```java
package io.github.atengk.rule.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.rule.dto.OrderRuleRequest;
import io.github.atengk.rule.fact.OrderFact;
import io.github.atengk.rule.fact.RuleHit;
import io.github.atengk.rule.service.RuleExecuteService;
import io.github.atengk.rule.vo.OrderRuleResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 规则执行服务实现
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExecuteServiceImpl implements RuleExecuteService {

    private final KieContainer kieContainer;

    /**
     * 执行订单规则
     *
     * @param request 订单规则执行请求
     * @return 订单规则执行结果
     */
    @Override
    public OrderRuleResult executeOrderRule(OrderRuleRequest request) {
        OrderFact orderFact = buildOrderFact(request);

        KieSession kieSession = kieContainer.newKieSession();
        try {
            kieSession.insert(orderFact);
            int fireCount = kieSession.fireAllRules();

            log.info("订单规则执行完成，订单号：{}，触发规则数量：{}", request.getOrderNo(), fireCount);
            return buildOrderRuleResult(orderFact);
        } catch (Exception e) {
            log.error("订单规则执行失败，订单号：{}", request.getOrderNo(), e);
            throw new IllegalStateException("订单规则执行失败", e);
        } finally {
            kieSession.dispose();
        }
    }

    /**
     * 构建订单事实对象
     *
     * @param request 订单规则执行请求
     * @return 订单事实对象
     */
    private OrderFact buildOrderFact(OrderRuleRequest request) {
        OrderFact orderFact = new OrderFact();
        orderFact.setUserId(request.getUserId());
        orderFact.setOrderNo(request.getOrderNo());
        orderFact.setMemberLevel(request.getMemberLevel());
        orderFact.setOrderAmount(request.getOrderAmount());
        orderFact.setFirstOrder(request.getFirstOrder());
        return orderFact;
    }

    /**
     * 构建订单规则执行结果
     *
     * @param orderFact 订单事实对象
     * @return 订单规则执行结果
     */
    private OrderRuleResult buildOrderRuleResult(OrderFact orderFact) {
        List<RuleHit> hits = CollUtil.emptyIfNull(orderFact.getHits())
                .stream()
                .sorted(Comparator.comparing(RuleHit::getPriority, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        BigDecimal totalDiscountAmount = hits.stream()
                .map(RuleHit::getDiscountAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, NumberUtil::add);

        BigDecimal payableAmount = NumberUtil.sub(orderFact.getOrderAmount(), totalDiscountAmount);
        if (payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            payableAmount = BigDecimal.ZERO;
        }

        return OrderRuleResult.builder()
                .orderNo(orderFact.getOrderNo())
                .orderAmount(orderFact.getOrderAmount())
                .totalDiscountAmount(totalDiscountAmount)
                .payableAmount(payableAmount)
                .hitCount(hits.size())
                .hits(hits)
                .build();
    }
}
```

这里的执行流程是：

```text
OrderRuleRequest
    -> buildOrderFact
    -> kieSession.insert(orderFact)
    -> kieSession.fireAllRules()
    -> orderFact.hits
    -> buildOrderRuleResult
    -> OrderRuleResult
```

这种方式适合多条规则叠加命中的场景。如果规则结果是互斥的，可以在 DRL 中使用 `activation-group` 控制只命中其中一条。

### 多规则匹配处理

多规则匹配是 Drools 常见场景。比如一个订单既满足“满1000优惠”，又满足“黄金会员优惠”，还满足“首单优惠”。此时需要明确业务规则是“叠加命中”还是“互斥命中”。

叠加命中适合优惠、标签、提示、风险因子累加等场景。多个规则都执行，执行结果统一写入 `hits` 集合，再由 Java 代码汇总。

```java
rule "满1000订单优惠"
    salience 100
when
    $order : OrderFact(orderAmount != null)
    eval($order.getOrderAmount().compareTo(new BigDecimal("1000")) >= 0)
then
    $order.addHit(new RuleHit(
        "ORDER_AMOUNT_DISCOUNT",
        drools.getRule().getName(),
        "订单金额满1000，优惠100元",
        new BigDecimal("100"),
        100
    ));
end

rule "首单优惠"
    salience 80
when
    $order : OrderFact(firstOrder == true)
then
    $order.addHit(new RuleHit(
        "FIRST_ORDER_DISCOUNT",
        drools.getRule().getName(),
        "用户首单优惠30元",
        new BigDecimal("30"),
        80
    ));
end
```

互斥命中适合会员等级优惠、风控决策、审批等级等场景。多个规则满足条件时，只执行优先级最高的一条。

```java
rule "高风险订单拦截"
    salience 100
    activation-group "risk-decision"
when
    $order : OrderFact(orderAmount != null)
    eval($order.getOrderAmount().compareTo(new BigDecimal("10000")) >= 0)
then
    $order.addHit(new RuleHit(
        "HIGH_RISK_REJECT",
        drools.getRule().getName(),
        "高金额订单，需要拦截审核",
        BigDecimal.ZERO,
        100
    ));
end

rule "普通订单放行"
    salience 10
    activation-group "risk-decision"
when
    $order : OrderFact(orderAmount != null)
    eval($order.getOrderAmount().compareTo(new BigDecimal("10000")) < 0)
then
    $order.addHit(new RuleHit(
        "NORMAL_PASS",
        drools.getRule().getName(),
        "普通订单，允许放行",
        BigDecimal.ZERO,
        10
    ));
end
```

多规则处理建议按以下方式设计：

| 规则类型     | 处理方式                | 示例                             |
| ------------ | ----------------------- | -------------------------------- |
| 可叠加规则   | 多条规则全部执行        | 多个优惠、多个标签、多个提示     |
| 互斥规则     | 使用 `activation-group` | 会员等级优惠、风控最终决策       |
| 优先级规则   | 使用 `salience`         | 高优先级规则先执行               |
| 规则结果汇总 | Java Service 汇总       | 汇总优惠金额、命中数量、命中说明 |
| 规则结果排序 | 根据 priority 排序      | 前端展示命中规则列表             |

实际开发中建议不要让 DRL 同时承担“规则命中”和“最终结果汇总”两个职责。DRL 更适合写规则判断和命中结果，最终金额、最终状态、接口响应建议放在 Java Service 中汇总，这样更容易调试、测试和维护。



## 接口设计

接口设计用于将 Drools 规则执行能力暴露给外部调用方。接口层只负责参数接收、参数校验、响应封装和异常转换，不直接编写复杂规则判断逻辑。规则判断应交给 Service 层和 DRL 文件完成。

### 规则执行接口

规则执行接口建议按业务场景拆分，而不是设计成一个过度通用的 `/execute` 接口。比如订单优惠规则、风控审核规则、会员等级规则可以分别提供独立接口。这样接口语义更清晰，参数模型也更稳定。

订单规则执行接口设计如下：

| 项目     | 内容                                     |
| -------- | ---------------------------------------- |
| 请求路径 | `/api/rules/order/execute`               |
| 请求方式 | `POST`                                   |
| 请求类型 | `application/json`                       |
| 接口说明 | 执行订单优惠规则，返回命中规则和优惠结果 |
| 入参对象 | `OrderRuleRequest`                       |
| 出参对象 | `Result<OrderRuleResult>`                |

先定义统一接口返回结构，便于 Controller 统一返回成功或失败结果。

文件位置：`src/main/java/io/github/atengk/common/result/Result.java`

```java
package io.github.atengk.common.result;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结果
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功返回
     *
     * @param data 返回数据
     * @param <T> 数据类型
     * @return 统一结果
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败返回
     *
     * @param code 状态码
     * @param message 返回消息
     * @param <T> 数据类型
     * @return 统一结果
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(StrUtil.blankToDefault(message, "操作失败"))
                .data(null)
                .build();
    }
}
```

下面是订单规则执行接口 Controller。它只做请求接收和响应封装，具体规则执行交给 `RuleExecuteService`。

文件位置：`src/main/java/io/github/atengk/rule/controller/RuleExecuteController.java`

```java
package io.github.atengk.rule.controller;

import io.github.atengk.common.result.Result;
import io.github.atengk.rule.dto.OrderRuleRequest;
import io.github.atengk.rule.service.RuleExecuteService;
import io.github.atengk.rule.vo.OrderRuleResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 规则执行接口
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rules")
public class RuleExecuteController {

    private final RuleExecuteService ruleExecuteService;

    /**
     * 执行订单规则
     *
     * @param request 订单规则执行请求
     * @return 订单规则执行结果
     */
    @PostMapping("/order/execute")
    public Result<OrderRuleResult> executeOrderRule(@Valid @RequestBody OrderRuleRequest request) {
        log.info("收到订单规则执行请求，订单号：{}，用户ID：{}", request.getOrderNo(), request.getUserId());
        OrderRuleResult result = ruleExecuteService.executeOrderRule(request);
        return Result.success(result);
    }
}
```

接口调用示例：

```bash
curl -X POST 'http://localhost:8080/api/rules/order/execute' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "orderNo": "ORDER202605080001",
    "memberLevel": "GOLD",
    "orderAmount": 1288.00,
    "firstOrder": true
  }'
```

该命令会向订单规则执行接口提交一笔订单数据。服务端会将请求参数转换为 `OrderFact`，插入 `KieSession` 执行 DRL 规则，然后返回命中的规则列表和最终计算结果。

### 请求参数设计

请求参数应围绕规则判断所需字段设计。DTO 不应直接承载过多数据库实体字段，也不建议把整个订单对象、用户对象原样传给规则接口。接口入参越清晰，规则边界越容易维护。

订单规则请求参数如下：

| 字段          | 类型         | 必填 | 说明                                                  |
| ------------- | ------------ | ---- | ----------------------------------------------------- |
| `userId`      | `Long`       | 是   | 用户ID                                                |
| `orderNo`     | `String`     | 是   | 订单号                                                |
| `memberLevel` | `String`     | 是   | 会员等级，例如 `NORMAL`、`SILVER`、`GOLD`、`PLATINUM` |
| `orderAmount` | `BigDecimal` | 是   | 订单金额                                              |
| `firstOrder`  | `Boolean`    | 是   | 是否首单                                              |

请求示例：

```json
{
  "userId": 10001,
  "orderNo": "ORDER202605080001",
  "memberLevel": "GOLD",
  "orderAmount": 1288.00,
  "firstOrder": true
}
```

参数校验建议放在 DTO 上完成。对于业务枚举值，例如会员等级，可以先用字符串处理，后续再扩展为枚举校验或字典校验。

文件位置：`src/main/java/io/github/atengk/rule/dto/OrderRuleRequest.java`

```java
package io.github.atengk.rule.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单规则执行请求
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class OrderRuleRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 会员等级：NORMAL、SILVER、GOLD、PLATINUM
     */
    @NotBlank(message = "会员等级不能为空")
    private String memberLevel;

    /**
     * 订单金额
     */
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal orderAmount;

    /**
     * 是否首单
     */
    @NotNull(message = "是否首单不能为空")
    private Boolean firstOrder;
}
```

请求参数设计时建议遵循以下原则：

| 原则             | 说明                                        |
| ---------------- | ------------------------------------------- |
| 参数最小化       | 只传规则判断所需字段                        |
| 类型明确         | 金额使用 `BigDecimal`，布尔值使用 `Boolean` |
| 必填校验         | 关键字段必须使用 Validation 注解校验        |
| 业务编码稳定     | 状态、等级、类型等字段建议使用稳定编码      |
| DTO 与 Fact 分离 | DTO 面向接口，Fact 面向规则执行             |

### 响应结果设计

响应结果应能清晰体现规则执行后的业务结果，至少包括命中规则数量、命中规则列表、原始金额、优惠金额和最终金额。对于风控类规则，可以返回风险等级、处置动作、命中标签等信息。

订单规则响应字段如下：

| 字段                  | 类型            | 说明         |
| --------------------- | --------------- | ------------ |
| `orderNo`             | `String`        | 订单号       |
| `orderAmount`         | `BigDecimal`    | 原始订单金额 |
| `totalDiscountAmount` | `BigDecimal`    | 总优惠金额   |
| `payableAmount`       | `BigDecimal`    | 最终应付金额 |
| `hitCount`            | `Integer`       | 命中规则数量 |
| `hits`                | `List<RuleHit>` | 命中规则明细 |

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "ORDER202605080001",
    "orderAmount": 1288.00,
    "totalDiscountAmount": 180.00,
    "payableAmount": 1108.00,
    "hitCount": 3,
    "hits": [
      {
        "ruleCode": "ORDER_AMOUNT_DISCOUNT",
        "ruleName": "满1000订单优惠",
        "message": "订单金额满1000，优惠100元",
        "discountAmount": 100.00,
        "priority": 100
      },
      {
        "ruleCode": "GOLD_MEMBER_DISCOUNT",
        "ruleName": "黄金会员优惠",
        "message": "黄金会员额外优惠50元",
        "discountAmount": 50.00,
        "priority": 90
      },
      {
        "ruleCode": "FIRST_ORDER_DISCOUNT",
        "ruleName": "首单优惠",
        "message": "用户首单优惠30元",
        "discountAmount": 30.00,
        "priority": 80
      }
    ]
  }
}
```

响应结果不建议直接返回 `KieSession` 执行细节，也不建议将完整 Fact 暴露给接口调用方。接口调用方只需要业务结果，不需要感知 Drools 的内部执行过程。

### 异常返回设计

规则执行过程中常见异常包括参数校验异常、规则文件编译异常、规则执行异常、规则结果计算异常。建议通过统一异常处理器转换为稳定响应结构。

先定义规则执行异常类。

文件位置：`src/main/java/io/github/atengk/common/exception/RuleExecuteException.java`

```java
package io.github.atengk.common.exception;

import cn.hutool.core.util.StrUtil;

/**
 * 规则执行异常
 *
 * @author Ateng
 * @since 2026-05-08
 */
public class RuleExecuteException extends RuntimeException {

    /**
     * 创建规则执行异常
     *
     * @param message 异常消息
     */
    public RuleExecuteException(String message) {
        super(StrUtil.blankToDefault(message, "规则执行失败"));
    }

    /**
     * 创建规则执行异常
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public RuleExecuteException(String message, Throwable cause) {
        super(StrUtil.blankToDefault(message, "规则执行失败"), cause);
    }
}
```

在规则执行服务中建议抛出业务异常，而不是直接抛出 `IllegalStateException`。

文件位置：`src/main/java/io/github/atengk/rule/service/impl/RuleExecuteServiceImpl.java`

```java
package io.github.atengk.rule.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.common.exception.RuleExecuteException;
import io.github.atengk.rule.dto.OrderRuleRequest;
import io.github.atengk.rule.fact.OrderFact;
import io.github.atengk.rule.fact.RuleHit;
import io.github.atengk.rule.service.RuleExecuteService;
import io.github.atengk.rule.vo.OrderRuleResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 规则执行服务实现
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExecuteServiceImpl implements RuleExecuteService {

    private final KieContainer kieContainer;

    /**
     * 执行订单规则
     *
     * @param request 订单规则执行请求
     * @return 订单规则执行结果
     */
    @Override
    public OrderRuleResult executeOrderRule(OrderRuleRequest request) {
        OrderFact orderFact = buildOrderFact(request);
        KieSession kieSession = kieContainer.newKieSession();

        try {
            kieSession.insert(orderFact);
            int fireCount = kieSession.fireAllRules();

            log.info("订单规则执行完成，订单号：{}，触发规则数量：{}", request.getOrderNo(), fireCount);
            return buildOrderRuleResult(orderFact);
        } catch (Exception e) {
            log.error("订单规则执行失败，订单号：{}", request.getOrderNo(), e);
            throw new RuleExecuteException("订单规则执行失败", e);
        } finally {
            kieSession.dispose();
        }
    }

    /**
     * 构建订单事实对象
     *
     * @param request 订单规则执行请求
     * @return 订单事实对象
     */
    private OrderFact buildOrderFact(OrderRuleRequest request) {
        OrderFact orderFact = new OrderFact();
        orderFact.setUserId(request.getUserId());
        orderFact.setOrderNo(request.getOrderNo());
        orderFact.setMemberLevel(request.getMemberLevel());
        orderFact.setOrderAmount(request.getOrderAmount());
        orderFact.setFirstOrder(request.getFirstOrder());
        return orderFact;
    }

    /**
     * 构建订单规则执行结果
     *
     * @param orderFact 订单事实对象
     * @return 订单规则执行结果
     */
    private OrderRuleResult buildOrderRuleResult(OrderFact orderFact) {
        List<RuleHit> hits = CollUtil.emptyIfNull(orderFact.getHits())
                .stream()
                .sorted(Comparator.comparing(RuleHit::getPriority, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        BigDecimal totalDiscountAmount = hits.stream()
                .map(RuleHit::getDiscountAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, NumberUtil::add);

        BigDecimal payableAmount = NumberUtil.sub(orderFact.getOrderAmount(), totalDiscountAmount);
        if (payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            payableAmount = BigDecimal.ZERO;
        }

        return OrderRuleResult.builder()
                .orderNo(orderFact.getOrderNo())
                .orderAmount(orderFact.getOrderAmount())
                .totalDiscountAmount(totalDiscountAmount)
                .payableAmount(payableAmount)
                .hitCount(hits.size())
                .hits(hits)
                .build();
    }
}
```

统一异常处理器用于将 Validation 异常、规则执行异常和其他未知异常转换为统一 JSON 响应。

文件位置：`src/main/java/io/github/atengk/common/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.common.exception;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求体参数校验异常
     *
     * @param e 参数校验异常
     * @return 统一返回结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String message = CollUtil.emptyIfNull(fieldErrors)
                .stream()
                .map(error -> StrUtil.format("{}：{}", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining("；"));

        log.warn("请求参数校验失败：{}", message);
        return Result.fail(400, StrUtil.blankToDefault(message, "请求参数校验失败"));
    }

    /**
     * 处理普通参数校验异常
     *
     * @param e 参数校验异常
     * @return 统一返回结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("请求参数校验失败：{}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    /**
     * 处理规则执行异常
     *
     * @param e 规则执行异常
     * @return 统一返回结果
     */
    @ExceptionHandler(RuleExecuteException.class)
    public Result<Void> handleRuleExecuteException(RuleExecuteException e) {
        log.error("规则执行异常：{}", e.getMessage(), e);
        return Result.fail(500, e.getMessage());
    }

    /**
     * 处理未知异常
     *
     * @param e 未知异常
     * @return 统一返回结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return Result.fail(500, "系统异常，请联系管理员");
    }
}
```

参数异常返回示例：

```json
{
  "code": 400,
  "message": "orderAmount：订单金额必须大于0",
  "data": null
}
```

规则执行异常返回示例：

```json
{
  "code": 500,
  "message": "订单规则执行失败",
  "data": null
}
```

异常返回设计建议：

| 异常类型     | HTTP 语义      | 返回 code | 处理方式                       |
| ------------ | -------------- | --------- | ------------------------------ |
| 参数校验异常 | 客户端请求错误 | `400`     | 返回具体字段错误               |
| 规则编译异常 | 服务端配置错误 | `500`     | 记录完整日志，返回简化提示     |
| 规则执行异常 | 服务端执行错误 | `500`     | 记录订单号、规则上下文和异常栈 |
| 未知异常     | 服务端未知错误 | `500`     | 返回统一兜底提示               |

## 规则管理

规则管理用于解决规则文件如何存放、如何加载、如何变更、如何回滚和如何热更新的问题。初期项目可以采用 classpath 本地规则文件管理，随着规则数量增加，再扩展到外部目录、数据库或规则管理后台。

### 本地规则文件管理

本地规则文件管理是最简单、最稳定的方式。规则文件随应用版本一起发布，适合规则变更频率不高、需要代码评审和发版管控的业务场景。

推荐目录结构如下：

```text
src/main/resources/rules
├── order
│   ├── order-discount.drl
│   └── order-limit.drl
├── risk
│   └── risk-check.drl
└── member
    └── member-level.drl
```

对应的规则包命名：

```java
package rules.order
package rules.risk
package rules.member
```

本地规则文件管理建议遵循以下规范：

| 规范                   | 说明                                          |
| ---------------------- | --------------------------------------------- |
| 按业务域分目录         | 订单、风控、会员、价格等规则分开管理          |
| 文件名表达业务含义     | 避免使用 `rule.drl`、`test.drl` 这类名称      |
| 规则编码全局唯一       | 便于日志检索、接口返回、问题定位              |
| 提交前执行测试         | 修改 DRL 后必须执行规则命中测试               |
| 禁止规则中调用外部系统 | 外部数据应在 Java Service 层准备好后传入 Fact |
| 保留规则变更记录       | Git 提交信息说明规则变更原因和影响范围        |

如果使用本地 classpath 规则，规则变更需要重新打包和发布应用。这种方式牺牲了灵活性，但换来更强的可控性和可审计性。

### 动态规则加载

动态规则加载用于在应用运行期间从外部目录、数据库或配置中心读取规则内容，并重新构建 Drools 规则容器。动态加载不应直接修改已经创建的 `KieSession`，而应重新构建新的 `KieContainer`。

先扩展 Drools 配置属性，增加外部规则路径和动态加载开关。

文件位置：`src/main/java/io/github/atengk/rule/config/DroolsProperties.java`

```java
package io.github.atengk.rule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Drools 配置属性
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@ConfigurationProperties(prefix = "drools")
public class DroolsProperties {

    /**
     * classpath 规则文件目录
     */
    private String rulePath = "rules";

    /**
     * 外部规则文件目录
     */
    private String externalRulePath;

    /**
     * 是否启用动态加载
     */
    private Boolean dynamicEnabled = false;

    /**
     * 规则扫描间隔，单位秒
     */
    private Long scanIntervalSeconds = 30L;
}
```

配置文件中增加动态加载相关参数。

文件位置：`src/main/resources/application.yml`

```yaml
drools:
  # classpath 规则目录
  rule-path: rules

  # 外部规则文件目录，启用动态规则加载时使用
  external-rule-path: /data/app/spring-boot-drools-demo/rules

  # 是否启用动态加载
  dynamic-enabled: false

  # 规则文件扫描间隔，单位秒
  scan-interval-seconds: 30
```

为了支持热更新，需要用一个容器持有类保存当前生效的 `KieContainer`。业务执行时不再直接注入固定的 `KieContainer`，而是从 `KieContainerHolder` 获取最新容器。

文件位置：`src/main/java/io/github/atengk/rule/config/KieContainerHolder.java`

```java
package io.github.atengk.rule.config;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * KieContainer 持有器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class KieContainerHolder {

    private final AtomicReference<KieContainer> containerReference = new AtomicReference<>();

    /**
     * 获取当前规则容器
     *
     * @return 当前规则容器
     */
    public KieContainer getContainer() {
        KieContainer kieContainer = containerReference.get();
        if (kieContainer == null) {
            throw new IllegalStateException("Drools规则容器未初始化");
        }
        return kieContainer;
    }

    /**
     * 设置当前规则容器
     *
     * @param newContainer 新规则容器
     */
    public void setContainer(KieContainer newContainer) {
        KieContainer oldContainer = containerReference.getAndSet(newContainer);
        if (oldContainer != null) {
            try {
                oldContainer.dispose();
            } catch (Exception e) {
                log.warn("旧Drools规则容器释放失败", e);
            }
        }
        log.info("Drools规则容器已切换");
    }
}
```

下面提供一个规则加载器，支持从 classpath 或外部目录读取 DRL 文件并构建新的 `KieContainer`。

文件位置：`src/main/java/io/github/atengk/rule/config/DroolsRuleLoader.java`

```java
package io.github.atengk.rule.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.runtime.KieContainer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Drools 规则加载器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class DroolsRuleLoader {

    /**
     * 从 classpath 加载规则容器
     *
     * @param rulePath 规则目录
     * @return 规则容器
     */
    public KieContainer loadFromClasspath(String rulePath) {
        try {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

            String path = StrUtil.blankToDefault(rulePath, "rules");
            String locationPattern = StrUtil.format("classpath*:{}/**/*.drl", path);

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(locationPattern);

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (StrUtil.isBlank(filename)) {
                    continue;
                }

                String kiePath = StrUtil.format("src/main/resources/{}/{}", path, filename);
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                kieFileSystem.write(kiePath, content);

                log.info("加载classpath规则文件：{}", kiePath);
            }

            return buildKieContainer(kieServices, kieFileSystem);
        } catch (IOException e) {
            log.error("classpath规则文件读取失败，规则目录：{}", rulePath, e);
            throw new IllegalStateException("classpath规则文件读取失败", e);
        }
    }

    /**
     * 从外部目录加载规则容器
     *
     * @param externalRulePath 外部规则目录
     * @return 规则容器
     */
    public KieContainer loadFromExternalPath(String externalRulePath) {
        if (StrUtil.isBlank(externalRulePath)) {
            throw new IllegalArgumentException("外部规则目录不能为空");
        }

        File ruleDir = FileUtil.file(externalRulePath);
        if (!FileUtil.exist(ruleDir) || !FileUtil.isDirectory(ruleDir)) {
            throw new IllegalArgumentException("外部规则目录不存在：" + externalRulePath);
        }

        List<File> ruleFiles = FileUtil.loopFiles(ruleDir, file -> StrUtil.endWithIgnoreCase(file.getName(), ".drl"));
        if (CollUtil.isEmpty(ruleFiles)) {
            throw new IllegalArgumentException("外部规则目录下未找到DRL文件：" + externalRulePath);
        }

        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        for (File ruleFile : ruleFiles) {
            String relativePath = FileUtil.subPath(ruleDir.getAbsolutePath(), ruleFile);
            String kiePath = StrUtil.format("src/main/resources/rules/{}", relativePath.replace("\\", "/"));
            String content = FileUtil.readString(ruleFile, StandardCharsets.UTF_8);
            kieFileSystem.write(kiePath, content);

            log.info("加载外部规则文件：{}", ruleFile.getAbsolutePath());
        }

        return buildKieContainer(kieServices, kieFileSystem);
    }

    /**
     * 构建规则容器
     *
     * @param kieServices KieServices
     * @param kieFileSystem KieFileSystem
     * @return 规则容器
     */
    private KieContainer buildKieContainer(KieServices kieServices, KieFileSystem kieFileSystem) {
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll();
        Results results = kieBuilder.getResults();

        if (results.hasMessages(Message.Level.ERROR)) {
            log.error("Drools规则编译失败：{}", results.getMessages());
            throw new IllegalStateException("Drools规则编译失败：" + results.getMessages());
        }

        ReleaseId releaseId = kieServices.getRepository().getDefaultReleaseId();
        KieContainer kieContainer = kieServices.newKieContainer(releaseId);

        log.info("Drools规则容器构建完成");
        return kieContainer;
    }
}
```

应用启动时初始化当前规则容器。

文件位置：`src/main/java/io/github/atengk/rule/config/DroolsInitializer.java`

```java
package io.github.atengk.rule.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Drools 初始化器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DroolsInitializer implements CommandLineRunner {

    private final DroolsProperties droolsProperties;
    private final DroolsRuleLoader droolsRuleLoader;
    private final KieContainerHolder kieContainerHolder;

    /**
     * 应用启动后初始化规则容器
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        KieContainer kieContainer;

        if (Boolean.TRUE.equals(droolsProperties.getDynamicEnabled())) {
            kieContainer = droolsRuleLoader.loadFromExternalPath(droolsProperties.getExternalRulePath());
            log.info("Drools已从外部目录初始化规则容器");
        } else {
            kieContainer = droolsRuleLoader.loadFromClasspath(droolsProperties.getRulePath());
            log.info("Drools已从classpath初始化规则容器");
        }

        kieContainerHolder.setContainer(kieContainer);
    }
}
```

如果使用 `KieContainerHolder`，规则执行服务应改为从 holder 获取当前容器。

文件位置：`src/main/java/io/github/atengk/rule/service/impl/RuleExecuteServiceImpl.java`

```java
package io.github.atengk.rule.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.common.exception.RuleExecuteException;
import io.github.atengk.rule.config.KieContainerHolder;
import io.github.atengk.rule.dto.OrderRuleRequest;
import io.github.atengk.rule.fact.OrderFact;
import io.github.atengk.rule.fact.RuleHit;
import io.github.atengk.rule.service.RuleExecuteService;
import io.github.atengk.rule.vo.OrderRuleResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 规则执行服务实现
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExecuteServiceImpl implements RuleExecuteService {

    private final KieContainerHolder kieContainerHolder;

    /**
     * 执行订单规则
     *
     * @param request 订单规则执行请求
     * @return 订单规则执行结果
     */
    @Override
    public OrderRuleResult executeOrderRule(OrderRuleRequest request) {
        OrderFact orderFact = buildOrderFact(request);
        KieSession kieSession = kieContainerHolder.getContainer().newKieSession();

        try {
            kieSession.insert(orderFact);
            int fireCount = kieSession.fireAllRules();

            log.info("订单规则执行完成，订单号：{}，触发规则数量：{}", request.getOrderNo(), fireCount);
            return buildOrderRuleResult(orderFact);
        } catch (Exception e) {
            log.error("订单规则执行失败，订单号：{}", request.getOrderNo(), e);
            throw new RuleExecuteException("订单规则执行失败", e);
        } finally {
            kieSession.dispose();
        }
    }

    /**
     * 构建订单事实对象
     *
     * @param request 订单规则执行请求
     * @return 订单事实对象
     */
    private OrderFact buildOrderFact(OrderRuleRequest request) {
        OrderFact orderFact = new OrderFact();
        orderFact.setUserId(request.getUserId());
        orderFact.setOrderNo(request.getOrderNo());
        orderFact.setMemberLevel(request.getMemberLevel());
        orderFact.setOrderAmount(request.getOrderAmount());
        orderFact.setFirstOrder(request.getFirstOrder());
        return orderFact;
    }

    /**
     * 构建订单规则执行结果
     *
     * @param orderFact 订单事实对象
     * @return 订单规则执行结果
     */
    private OrderRuleResult buildOrderRuleResult(OrderFact orderFact) {
        List<RuleHit> hits = CollUtil.emptyIfNull(orderFact.getHits())
                .stream()
                .sorted(Comparator.comparing(RuleHit::getPriority, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        BigDecimal totalDiscountAmount = hits.stream()
                .map(RuleHit::getDiscountAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, NumberUtil::add);

        BigDecimal payableAmount = NumberUtil.sub(orderFact.getOrderAmount(), totalDiscountAmount);
        if (payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            payableAmount = BigDecimal.ZERO;
        }

        return OrderRuleResult.builder()
                .orderNo(orderFact.getOrderNo())
                .orderAmount(orderFact.getOrderAmount())
                .totalDiscountAmount(totalDiscountAmount)
                .payableAmount(payableAmount)
                .hitCount(hits.size())
                .hits(hits)
                .build();
    }
}
```

### 规则版本控制

规则版本控制用于保证规则变更可追踪、可回滚、可审计。即使使用本地 DRL 文件，也建议对每次规则变更建立明确的版本号和变更记录。

如果规则文件随代码发布，可以直接使用 Git 管理版本：

```text
rules/order/order-discount.drl
rules/order/order-limit.drl
rules/risk/risk-check.drl
```

推荐 Git 提交信息格式：

```text
rule(order): 调整满1000订单优惠金额

变更内容：
- ORDER_AMOUNT_DISCOUNT 优惠金额由 80 调整为 100
- 适用条件保持不变

影响范围：
- 订单优惠计算
- 订单结算页应付金额

回滚方式：
- 回滚本次提交
```

如果规则存储在数据库中，可以设计规则版本表。以下是简化表结构示例。

```sql
CREATE TABLE drools_rule_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_code VARCHAR(100) NOT NULL COMMENT '规则编码',
    rule_name VARCHAR(200) NOT NULL COMMENT '规则名称',
    rule_group VARCHAR(100) NOT NULL COMMENT '规则分组',
    rule_version VARCHAR(50) NOT NULL COMMENT '规则版本',
    rule_content TEXT NOT NULL COMMENT '规则内容',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_by VARCHAR(100) DEFAULT NULL COMMENT '创建人',
    created_time DATETIME NOT NULL COMMENT '创建时间',
    updated_by VARCHAR(100) DEFAULT NULL COMMENT '更新人',
    updated_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_rule_code_version (rule_code, rule_version)
) COMMENT='Drools规则版本表';
```

规则版本管理建议：

| 项目       | 建议                               |
| ---------- | ---------------------------------- |
| 规则编码   | 全局唯一，不随名称变化             |
| 版本号     | 使用 `v1.0.0`、`v1.0.1` 或日期版本 |
| 变更说明   | 说明条件、动作、影响范围           |
| 启用状态   | 同一规则编码建议只有一个启用版本   |
| 发布前校验 | 先编译 DRL，再切换启用状态         |
| 回滚方式   | 保留上一版本规则内容，支持快速回滚 |

如果没有规则管理后台，至少应通过 Git 提交记录和发布记录管理规则版本。不要在线上服务器直接修改 `.drl` 文件且不留痕。

### 规则热更新方案

规则热更新的目标是在不重启应用的情况下加载新规则。推荐实现方式是“先构建新容器，编译成功后再替换旧容器”。如果新规则编译失败，应保留旧容器继续提供服务。

热更新流程如下：

```text
上传或修改规则文件
    -> 触发规则重新加载
    -> 构建新的 KieContainer
    -> 编译规则文件
    -> 编译成功后切换 KieContainer
    -> 释放旧 KieContainer
    -> 新请求使用新规则
```

手动热更新接口适合后台管理或运维触发。下面提供一个简单的刷新接口，只在 `dynamic-enabled=true` 时从外部规则目录重新加载规则。

文件位置：`src/main/java/io/github/atengk/rule/controller/RuleManageController.java`

```java
package io.github.atengk.rule.controller;

import io.github.atengk.common.result.Result;
import io.github.atengk.rule.config.DroolsProperties;
import io.github.atengk.rule.config.DroolsRuleLoader;
import io.github.atengk.rule.config.KieContainerHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * 规则管理接口
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rules/manage")
public class RuleManageController {

    private final DroolsProperties droolsProperties;
    private final DroolsRuleLoader droolsRuleLoader;
    private final KieContainerHolder kieContainerHolder;

    /**
     * 手动刷新规则
     *
     * @return 刷新结果
     */
    @PostMapping("/reload")
    public Result<String> reloadRules() {
        if (!Boolean.TRUE.equals(droolsProperties.getDynamicEnabled())) {
            return Result.fail(400, "未启用动态规则加载");
        }

        KieContainer kieContainer = droolsRuleLoader.loadFromExternalPath(droolsProperties.getExternalRulePath());
        kieContainerHolder.setContainer(kieContainer);

        log.info("Drools规则手动刷新完成");
        return Result.success("规则刷新成功");
    }
}
```

调用热更新接口：

```bash
curl -X POST 'http://localhost:8080/api/rules/manage/reload'
```

该命令会触发服务端从 `drools.external-rule-path` 指定目录重新读取 `.drl` 文件。如果规则编译成功，会切换当前生效的 `KieContainer`；如果编译失败，接口会返回失败，旧规则仍可继续使用。

如果需要定时扫描规则目录，可以增加定时任务。启用前需要在启动类上添加 `@EnableScheduling`。

文件位置：`src/main/java/io/github/atengk/DroolsApplication.java`

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Drools 示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-08
 */
@EnableScheduling
@SpringBootApplication
public class DroolsApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DroolsApplication.class, args);
    }
}
```

下面的定时任务会按配置间隔扫描外部规则目录文件的最后修改时间。如果发现变更，则重新加载规则容器。

文件位置：`src/main/java/io/github/atengk/rule/config/DroolsRuleRefreshScheduler.java`

```java
package io.github.atengk.rule.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Comparator;
import java.util.List;

/**
 * Drools 规则刷新定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DroolsRuleRefreshScheduler {

    private final DroolsProperties droolsProperties;
    private final DroolsRuleLoader droolsRuleLoader;
    private final KieContainerHolder kieContainerHolder;

    private volatile long lastModified = -1L;

    /**
     * 定时刷新规则
     */
    @Scheduled(fixedDelayString = "#{@droolsProperties.scanIntervalSeconds * 1000}")
    public void refreshRules() {
        if (!Boolean.TRUE.equals(droolsProperties.getDynamicEnabled())) {
            return;
        }

        String externalRulePath = droolsProperties.getExternalRulePath();
        if (StrUtil.isBlank(externalRulePath)) {
            log.warn("未配置外部规则目录，跳过规则刷新");
            return;
        }

        File ruleDir = FileUtil.file(externalRulePath);
        if (!FileUtil.exist(ruleDir) || !FileUtil.isDirectory(ruleDir)) {
            log.warn("外部规则目录不存在，跳过规则刷新：{}", externalRulePath);
            return;
        }

        List<File> ruleFiles = FileUtil.loopFiles(ruleDir, file -> StrUtil.endWithIgnoreCase(file.getName(), ".drl"));
        if (CollUtil.isEmpty(ruleFiles)) {
            log.warn("外部规则目录下未找到DRL文件，跳过规则刷新：{}", externalRulePath);
            return;
        }

        long currentLastModified = ruleFiles.stream()
                .max(Comparator.comparingLong(File::lastModified))
                .map(File::lastModified)
                .orElse(-1L);

        if (currentLastModified <= lastModified) {
            return;
        }

        try {
            KieContainer newContainer = droolsRuleLoader.loadFromExternalPath(externalRulePath);
            kieContainerHolder.setContainer(newContainer);
            lastModified = currentLastModified;

            log.info("检测到Drools规则文件变更，规则已自动刷新");
        } catch (Exception e) {
            log.error("Drools规则自动刷新失败，继续使用旧规则", e);
        }
    }
}
```

热更新方案的关键注意事项：

| 注意项                        | 说明                                          |
| ----------------------------- | --------------------------------------------- |
| 不更新正在执行的 `KieSession` | 每次请求创建独立 `KieSession`，执行后立即释放 |
| 先编译后切换                  | 新规则编译成功后再替换旧容器                  |
| 失败不影响旧规则              | 新规则加载失败时继续使用旧 `KieContainer`     |
| 控制刷新入口                  | 生产环境建议热更新接口增加权限控制            |
| 保留规则版本                  | 支持问题追溯和快速回滚                        |
| 避免高频刷新                  | 规则编译有成本，扫描间隔不宜过短              |
| 记录刷新日志                  | 记录规则路径、刷新时间、失败原因              |

生产环境更推荐使用“管理后台发布 + 编译校验 + 人工确认 + 容器切换”的方式，而不是单纯依赖文件扫描。文件扫描适合中小项目或内部系统，规则数量较多、规则审批严格的系统应建设规则版本表和发布流程。



## 测试与验证

测试与验证用于确认规则文件能正常加载、规则条件能正确命中、接口调用能返回预期结果、异常场景能被统一处理。Drools 项目中测试非常重要，因为规则文件通常变化频繁，如果没有自动化测试，规则调整很容易引入隐蔽问题。

### 单元测试

单元测试主要验证 Service 层规则执行逻辑是否正确。测试重点不是 Drools 引擎本身，而是确认输入 DTO 转换、规则执行、命中结果汇总和最终响应计算是否符合预期。

建议在测试环境中使用固定的 DRL 文件，并通过 `RuleExecuteService` 直接调用规则执行逻辑。

文件位置：`src/test/java/io/github/atengk/rule/RuleExecuteServiceTest.java`

```java
package io.github.atengk.rule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.rule.dto.OrderRuleRequest;
import io.github.atengk.rule.service.RuleExecuteService;
import io.github.atengk.rule.vo.OrderRuleResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

/**
 * 规则执行服务测试
 *
 * @author Ateng
 * @since 2026-05-08
 */
@SpringBootTest
class RuleExecuteServiceTest {

    @Autowired
    private RuleExecuteService ruleExecuteService;

    /**
     * 测试订单规则正常命中
     */
    @Test
    void testExecuteOrderRuleSuccess() {
        OrderRuleRequest request = new OrderRuleRequest();
        request.setUserId(10001L);
        request.setOrderNo("ORDER202605080001");
        request.setMemberLevel("GOLD");
        request.setOrderAmount(new BigDecimal("1288.00"));
        request.setFirstOrder(true);

        OrderRuleResult result = ruleExecuteService.executeOrderRule(request);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("ORDER202605080001", result.getOrderNo());
        Assertions.assertTrue(result.getHitCount() > 0);
        Assertions.assertTrue(CollUtil.isNotEmpty(result.getHits()));
        Assertions.assertTrue(NumberUtil.isGreater(result.getTotalDiscountAmount(), BigDecimal.ZERO));
        Assertions.assertTrue(NumberUtil.isGreaterOrEqual(result.getPayableAmount(), BigDecimal.ZERO));
    }

    /**
     * 测试普通订单未命中高额优惠规则
     */
    @Test
    void testExecuteOrderRuleNormalOrder() {
        OrderRuleRequest request = new OrderRuleRequest();
        request.setUserId(10002L);
        request.setOrderNo("ORDER202605080002");
        request.setMemberLevel("NORMAL");
        request.setOrderAmount(new BigDecimal("99.00"));
        request.setFirstOrder(false);

        OrderRuleResult result = ruleExecuteService.executeOrderRule(request);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("ORDER202605080002", result.getOrderNo());
        Assertions.assertTrue(NumberUtil.equals(result.getOrderAmount(), new BigDecimal("99.00")));
        Assertions.assertTrue(NumberUtil.isGreaterOrEqual(result.getPayableAmount(), BigDecimal.ZERO));
    }
}
```

该测试会加载 Spring 容器并执行真实规则，适合作为集成级单元测试。测试前需要保证 `src/main/resources/rules` 或测试专用规则目录中存在可加载的 `.drl` 文件。

如果希望测试环境使用独立配置，可以增加测试配置文件。

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  application:
    # 测试环境应用名称
    name: spring-boot-drools-demo-test

drools:
  # 测试时默认加载 classpath 下 rules 目录
  rule-path: rules

  # 测试环境关闭动态加载，避免依赖外部文件目录
  dynamic-enabled: false

logging:
  level:
    # 测试时可以打开项目 DEBUG 日志，便于排查规则命中
    io.github.atengk: debug
    org.drools: info
    org.kie: info
```

如果启用该配置，需要在测试类上增加：

```java
@ActiveProfiles("test")
```

### 接口测试

接口测试用于验证 Controller 层参数校验、JSON 序列化、统一返回结构和规则执行链路是否正常。建议使用 `MockMvc` 完成接口级测试，不需要真实启动 HTTP 端口。

文件位置：`src/test/java/io/github/atengk/rule/RuleExecuteControllerTest.java`

```java
package io.github.atengk.rule;

import cn.hutool.json.JSONUtil;
import io.github.atengk.rule.dto.OrderRuleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 规则执行接口测试
 *
 * @author Ateng
 * @since 2026-05-08
 */
@SpringBootTest
@AutoConfigureMockMvc
class RuleExecuteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试订单规则执行接口成功返回
     *
     * @throws Exception 接口调用异常
     */
    @Test
    void testExecuteOrderRuleApiSuccess() throws Exception {
        OrderRuleRequest request = new OrderRuleRequest();
        request.setUserId(10001L);
        request.setOrderNo("ORDER202605080003");
        request.setMemberLevel("GOLD");
        request.setOrderAmount(new BigDecimal("1288.00"));
        request.setFirstOrder(true);

        mockMvc.perform(post("/api/rules/order/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.orderNo").value("ORDER202605080003"))
                .andExpect(jsonPath("$.data.hitCount").value(greaterThanOrEqualTo(0)));
    }

    /**
     * 测试订单规则执行接口参数校验失败
     *
     * @throws Exception 接口调用异常
     */
    @Test
    void testExecuteOrderRuleApiValidateFail() throws Exception {
        OrderRuleRequest request = new OrderRuleRequest();
        request.setUserId(10001L);
        request.setOrderNo("");
        request.setMemberLevel("GOLD");
        request.setOrderAmount(new BigDecimal("0.00"));
        request.setFirstOrder(true);

        mockMvc.perform(post("/api/rules/order/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
```

接口测试重点关注以下内容：

| 测试项    | 说明                             |
| --------- | -------------------------------- |
| HTTP 路径 | 请求路径是否正确                 |
| 请求方式  | 是否使用 `POST`                  |
| JSON 参数 | 入参字段是否能正确反序列化       |
| 参数校验  | 必填、金额范围等校验是否生效     |
| 响应结构  | 是否返回统一 `Result`            |
| 规则结果  | 是否包含命中数量、金额、命中列表 |

如果项目要求参数错误返回标准 HTTP 400，而不是业务 JSON 中的 `code=400`，可以在全局异常处理器中配合 `@ResponseStatus` 或 `ResponseEntity` 调整响应状态码。

### 规则命中测试

规则命中测试用于验证某一条或某一组 DRL 规则是否按预期触发。相比接口测试，它更关注规则本身，例如“满1000优惠是否命中”“普通会员是否不命中黄金会员优惠”。

文件位置：`src/test/java/io/github/atengk/rule/DroolsRuleHitTest.java`

```java
package io.github.atengk.rule;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.rule.config.KieContainerHolder;
import io.github.atengk.rule.fact.OrderFact;
import io.github.atengk.rule.fact.RuleHit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

/**
 * Drools 规则命中测试
 *
 * @author Ateng
 * @since 2026-05-08
 */
@SpringBootTest
class DroolsRuleHitTest {

    @Autowired
    private KieContainerHolder kieContainerHolder;

    /**
     * 测试满1000订单优惠规则命中
     */
    @Test
    void testOrderAmountDiscountRuleHit() {
        OrderFact orderFact = new OrderFact();
        orderFact.setUserId(10001L);
        orderFact.setOrderNo("ORDER202605080004");
        orderFact.setMemberLevel("NORMAL");
        orderFact.setOrderAmount(new BigDecimal("1000.00"));
        orderFact.setFirstOrder(false);

        KieSession kieSession = kieContainerHolder.getContainer().newKieSession();
        try {
            kieSession.insert(orderFact);
            kieSession.fireAllRules();
        } finally {
            kieSession.dispose();
        }

        List<RuleHit> hits = orderFact.getHits();
        Assertions.assertTrue(CollUtil.isNotEmpty(hits));

        boolean matched = hits.stream()
                .anyMatch(hit -> "ORDER_AMOUNT_DISCOUNT".equals(hit.getRuleCode()));

        Assertions.assertTrue(matched, "应命中满1000订单优惠规则");
    }

    /**
     * 测试黄金会员优惠规则命中
     */
    @Test
    void testGoldMemberDiscountRuleHit() {
        OrderFact orderFact = new OrderFact();
        orderFact.setUserId(10002L);
        orderFact.setOrderNo("ORDER202605080005");
        orderFact.setMemberLevel("GOLD");
        orderFact.setOrderAmount(new BigDecimal("300.00"));
        orderFact.setFirstOrder(false);

        KieSession kieSession = kieContainerHolder.getContainer().newKieSession();
        try {
            kieSession.insert(orderFact);
            kieSession.fireAllRules();
        } finally {
            kieSession.dispose();
        }

        boolean matched = CollUtil.emptyIfNull(orderFact.getHits())
                .stream()
                .anyMatch(hit -> "GOLD_MEMBER_DISCOUNT".equals(hit.getRuleCode()));

        Assertions.assertTrue(matched, "应命中黄金会员优惠规则");
    }
}
```

规则命中测试建议按规则编码编写，不建议只判断命中数量。命中数量可能随着规则增加发生变化，但规则编码更稳定，能更准确地表达测试意图。

### 异常场景测试

异常场景测试用于验证参数错误、规则执行异常、规则加载异常能否被正确处理。对于接口层，可以重点测试参数校验；对于规则加载异常，可以通过单独构造非法 DRL 内容验证编译失败。

参数校验异常测试示例：

文件位置：`src/test/java/io/github/atengk/rule/RuleExceptionTest.java`

```java
package io.github.atengk.rule;

import cn.hutool.json.JSONUtil;
import io.github.atengk.rule.dto.OrderRuleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 规则异常场景测试
 *
 * @author Ateng
 * @since 2026-05-08
 */
@SpringBootTest
@AutoConfigureMockMvc
class RuleExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试订单号为空时返回参数校验异常
     *
     * @throws Exception 接口调用异常
     */
    @Test
    void testOrderNoBlankValidateFail() throws Exception {
        OrderRuleRequest request = new OrderRuleRequest();
        request.setUserId(10001L);
        request.setOrderNo("");
        request.setMemberLevel("GOLD");
        request.setOrderAmount(new BigDecimal("100.00"));
        request.setFirstOrder(true);

        mockMvc.perform(post("/api/rules/order/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("订单号不能为空")));
    }

    /**
     * 测试订单金额非法时返回参数校验异常
     *
     * @throws Exception 接口调用异常
     */
    @Test
    void testOrderAmountInvalidValidateFail() throws Exception {
        OrderRuleRequest request = new OrderRuleRequest();
        request.setUserId(10001L);
        request.setOrderNo("ORDER202605080006");
        request.setMemberLevel("GOLD");
        request.setOrderAmount(BigDecimal.ZERO);
        request.setFirstOrder(true);

        mockMvc.perform(post("/api/rules/order/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("订单金额必须大于0")));
    }
}
```

异常场景测试建议覆盖以下内容：

| 场景               | 验证点                               |
| ------------------ | ------------------------------------ |
| 必填参数为空       | 返回 `code=400` 和具体字段提示       |
| 金额非法           | 返回金额校验错误                     |
| 规则文件语法错误   | 应用启动失败或刷新接口返回失败       |
| 外部规则目录不存在 | 记录错误日志，返回规则加载失败       |
| 规则执行异常       | 统一返回规则执行失败，不暴露内部堆栈 |
| 热更新失败         | 保留旧规则容器继续服务               |

## 日志与异常处理

日志与异常处理用于保障规则执行过程可追踪、问题可定位、异常可恢复。Drools 规则引擎通常会承载关键业务判断，因此需要记录规则加载、规则执行、规则刷新、规则异常等关键事件。

### 规则执行日志

规则执行日志建议记录请求标识、业务主键、触发规则数量、命中规则编码、执行耗时等信息。不要在日志中输出过多敏感数据，例如完整用户信息、证件号、手机号等。

可以在 Service 层记录规则执行耗时。

文件位置：`src/main/java/io/github/atengk/rule/service/impl/RuleExecuteServiceImpl.java`

```java
package io.github.atengk.rule.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.RuleExecuteException;
import io.github.atengk.rule.config.KieContainerHolder;
import io.github.atengk.rule.dto.OrderRuleRequest;
import io.github.atengk.rule.fact.OrderFact;
import io.github.atengk.rule.fact.RuleHit;
import io.github.atengk.rule.service.RuleExecuteService;
import io.github.atengk.rule.vo.OrderRuleResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 规则执行服务实现
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExecuteServiceImpl implements RuleExecuteService {

    private final KieContainerHolder kieContainerHolder;

    /**
     * 执行订单规则
     *
     * @param request 订单规则执行请求
     * @return 订单规则执行结果
     */
    @Override
    public OrderRuleResult executeOrderRule(OrderRuleRequest request) {
        StopWatch stopWatch = new StopWatch("订单规则执行");
        stopWatch.start();

        OrderFact orderFact = buildOrderFact(request);
        KieSession kieSession = kieContainerHolder.getContainer().newKieSession();

        try {
            kieSession.insert(orderFact);
            int fireCount = kieSession.fireAllRules();

            OrderRuleResult result = buildOrderRuleResult(orderFact);
            String hitRuleCodes = CollUtil.emptyIfNull(result.getHits())
                    .stream()
                    .map(RuleHit::getRuleCode)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.joining(","));

            stopWatch.stop();
            log.info("订单规则执行完成，订单号：{}，触发规则数量：{}，命中规则：{}，耗时：{}ms",
                    request.getOrderNo(), fireCount, hitRuleCodes, stopWatch.getTotalTimeMillis());

            return result;
        } catch (Exception e) {
            stopWatch.stop();
            log.error("订单规则执行失败，订单号：{}，耗时：{}ms", request.getOrderNo(), stopWatch.getTotalTimeMillis(), e);
            throw new RuleExecuteException("订单规则执行失败", e);
        } finally {
            kieSession.dispose();
        }
    }

    /**
     * 构建订单事实对象
     *
     * @param request 订单规则执行请求
     * @return 订单事实对象
     */
    private OrderFact buildOrderFact(OrderRuleRequest request) {
        OrderFact orderFact = new OrderFact();
        orderFact.setUserId(request.getUserId());
        orderFact.setOrderNo(request.getOrderNo());
        orderFact.setMemberLevel(request.getMemberLevel());
        orderFact.setOrderAmount(request.getOrderAmount());
        orderFact.setFirstOrder(request.getFirstOrder());
        return orderFact;
    }

    /**
     * 构建订单规则执行结果
     *
     * @param orderFact 订单事实对象
     * @return 订单规则执行结果
     */
    private OrderRuleResult buildOrderRuleResult(OrderFact orderFact) {
        List<RuleHit> hits = CollUtil.emptyIfNull(orderFact.getHits())
                .stream()
                .sorted(Comparator.comparing(RuleHit::getPriority, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        BigDecimal totalDiscountAmount = hits.stream()
                .map(RuleHit::getDiscountAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, NumberUtil::add);

        BigDecimal payableAmount = NumberUtil.sub(orderFact.getOrderAmount(), totalDiscountAmount);
        if (payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            payableAmount = BigDecimal.ZERO;
        }

        return OrderRuleResult.builder()
                .orderNo(orderFact.getOrderNo())
                .orderAmount(orderFact.getOrderAmount())
                .totalDiscountAmount(totalDiscountAmount)
                .payableAmount(payableAmount)
                .hitCount(hits.size())
                .hits(hits)
                .build();
    }
}
```

如果需要记录更细粒度的规则激活日志，可以添加 Drools 监听器。

文件位置：`src/main/java/io/github/atengk/rule/listener/DroolsAgendaEventListener.java`

```java
package io.github.atengk.rule.listener;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;

/**
 * Drools 规则触发监听器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class DroolsAgendaEventListener extends DefaultAgendaEventListener {

    /**
     * 规则触发后记录日志
     *
     * @param event 规则触发事件
     */
    @Override
    public void afterMatchFired(AfterMatchFiredEvent event) {
        String ruleName = event.getMatch().getRule().getName();
        log.debug("Drools规则已触发，规则名称：{}", ruleName);
    }
}
```

在规则执行时注册监听器：

```java
kieSession.addEventListener(new DroolsAgendaEventListener());
```

生产环境通常不建议长期开启每条规则触发的 DEBUG 日志，否则规则量大时日志量会明显增加。建议仅在排查规则命中问题时打开。

### 规则加载异常

规则加载异常通常发生在应用启动、手动刷新、定时热更新阶段。常见原因包括 DRL 语法错误、Fact 类路径错误、规则文件编码错误、外部规则目录不存在等。

规则加载异常处理原则如下：

| 场景             | 处理方式                               |
| ---------------- | -------------------------------------- |
| 应用启动加载失败 | 直接启动失败，避免应用带着无效规则运行 |
| 手动热更新失败   | 返回失败信息，旧规则继续生效           |
| 定时热更新失败   | 记录错误日志，旧规则继续生效           |
| 部分规则编译失败 | 整批规则不切换，避免新旧规则混用       |
| 外部目录不存在   | 记录错误并返回明确提示                 |

在 `DroolsRuleLoader` 中需要对编译结果做强校验：

```java
Results results = kieBuilder.getResults();

if (results.hasMessages(Message.Level.ERROR)) {
    log.error("Drools规则编译失败：{}", results.getMessages());
    throw new IllegalStateException("Drools规则编译失败：" + results.getMessages());
}
```

规则加载日志建议包含以下信息：

```text
加载Drools规则文件：src/main/resources/rules/order-discount.drl
Drools规则容器构建完成
Drools规则容器已切换
Drools规则编译失败：[ERROR ...]
```

### 规则执行异常

规则执行异常通常发生在 `fireAllRules()` 阶段。常见原因包括 DRL 中调用方法异常、Fact 字段为空、BigDecimal 比较未判空、规则动作中对象写入失败等。

执行异常应在 Service 层捕获并转换为业务异常，不建议将 Drools 原始异常直接返回给调用方。

建议使用统一异常：

```java
catch (Exception e) {
    log.error("订单规则执行失败，订单号：{}", request.getOrderNo(), e);
    throw new RuleExecuteException("订单规则执行失败", e);
}
```

DRL 中应尽量先做空值判断，再执行比较或方法调用。例如金额比较要先判断 `orderAmount != null`：

```java
rule "满1000订单优惠"
    salience 100
when
    $order : OrderFact(orderAmount != null)
    eval($order.getOrderAmount().compareTo(new BigDecimal("1000")) >= 0)
then
    $order.addHit(new RuleHit(
        "ORDER_AMOUNT_DISCOUNT",
        drools.getRule().getName(),
        "订单金额满1000，优惠100元",
        new BigDecimal("100"),
        100
    ));
end
```

规则执行异常排查建议：

| 排查项          | 说明                                 |
| --------------- | ------------------------------------ |
| 检查 Fact 字段  | 是否存在空值、类型错误、字段名错误   |
| 检查 import     | DRL 中 Java 类路径是否正确           |
| 检查 BigDecimal | 比较前是否判空，是否使用 `compareTo` |
| 检查规则动作    | `then` 中是否调用了可能抛异常的方法  |
| 检查日志        | 根据订单号、规则名称、异常堆栈定位   |

### 参数校验异常

参数校验异常应在请求进入规则引擎前处理。这样可以避免非法数据进入 Drools，减少规则文件中的防御性判断。

DTO 使用 Jakarta Validation 注解：

```java
@NotNull(message = "订单金额不能为空")
@DecimalMin(value = "0.01", message = "订单金额必须大于0")
private BigDecimal orderAmount;
```

全局异常处理器统一处理 `MethodArgumentNotValidException`：

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
    String message = CollUtil.emptyIfNull(fieldErrors)
            .stream()
            .map(error -> StrUtil.format("{}：{}", error.getField(), error.getDefaultMessage()))
            .collect(Collectors.joining("；"));

    log.warn("请求参数校验失败：{}", message);
    return Result.fail(400, StrUtil.blankToDefault(message, "请求参数校验失败"));
}
```

参数校验建议放在以下层次：

| 层次       | 职责                                 |
| ---------- | ------------------------------------ |
| DTO 注解   | 必填、长度、金额范围、格式           |
| Controller | 使用 `@Valid` 触发校验               |
| Service    | 处理业务级参数约束                   |
| DRL        | 处理规则条件判断，不承担基础参数校验 |

## 部署与运行

部署与运行部分用于说明 Spring Boot + Drools 项目的打包方式、配置方式、启动方式和生产运行注意事项。Drools 规则文件可以随应用一起打包，也可以放在外部目录通过动态加载机制读取。

### 打包配置

Spring Boot 项目一般使用 Maven 打包为可执行 JAR。需要确保 `src/main/resources/rules` 下的 `.drl` 文件被打入 JAR 包。

文件位置：`pom.xml`

```xml
<build>
    <resources>
        <!-- 打包应用配置和规则文件 -->
        <resource>
            <directory>src/main/resources</directory>
            <includes>
                <include>**/*.yml</include>
                <include>**/*.yaml</include>
                <include>**/*.properties</include>
                <include>**/*.drl</include>
                <include>**/*.xml</include>
            </includes>
        </resource>
    </resources>

    <plugins>
        <!-- Spring Boot 打包插件：生成可执行 JAR -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <!-- 排除 Lombok，避免打包无关编译期依赖 -->
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

打包命令：

```bash
mvn clean package -DskipTests
```

该命令会清理旧构建产物并打包项目，`-DskipTests` 表示跳过测试执行。生产发布前不建议长期跳过测试，至少应在 CI 流程中执行规则命中测试和接口测试。

如果希望打包前执行测试：

```bash
mvn clean test
mvn clean package
```

打包完成后检查规则文件是否进入 JAR：

```bash
jar tf target/spring-boot-drools-demo.jar | grep 'rules/.*\.drl'
```

该命令会列出 JAR 包中的规则文件。如果没有输出，说明 `.drl` 文件没有被正确打包，需要检查 Maven resources 配置和规则文件路径。

### 环境变量配置

生产环境建议通过环境变量覆盖关键配置，例如服务端口、规则目录、是否启用动态加载、日志级别等。Spring Boot 支持将环境变量映射到配置项。

生产配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
server:
  # 服务端口，可通过 SERVER_PORT 覆盖
  port: ${SERVER_PORT:8080}

spring:
  application:
    # 应用名称
    name: spring-boot-drools-demo

drools:
  # classpath 规则目录
  rule-path: ${DROOLS_RULE_PATH:rules}

  # 外部规则目录，启用动态加载时使用
  external-rule-path: ${DROOLS_EXTERNAL_RULE_PATH:/data/app/spring-boot-drools-demo/rules}

  # 是否启用动态加载
  dynamic-enabled: ${DROOLS_DYNAMIC_ENABLED:false}

  # 规则扫描间隔，单位秒
  scan-interval-seconds: ${DROOLS_SCAN_INTERVAL_SECONDS:30}

logging:
  file:
    # 日志文件路径
    name: ${LOG_FILE:/data/logs/spring-boot-drools-demo/app.log}

  level:
    # 业务日志级别
    io.github.atengk: ${APP_LOG_LEVEL:info}

    # Drools 日志级别
    org.drools: ${DROOLS_LOG_LEVEL:info}
    org.kie: ${KIE_LOG_LEVEL:info}
```

Linux 启动前可以配置环境变量：

```bash
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=prod
export DROOLS_DYNAMIC_ENABLED=true
export DROOLS_EXTERNAL_RULE_PATH=/data/app/spring-boot-drools-demo/rules
export DROOLS_SCAN_INTERVAL_SECONDS=30
export LOG_FILE=/data/logs/spring-boot-drools-demo/app.log
```

这些环境变量用于控制应用运行配置。`SPRING_PROFILES_ACTIVE=prod` 表示启用生产配置；`DROOLS_DYNAMIC_ENABLED=true` 表示从外部目录加载规则；`DROOLS_EXTERNAL_RULE_PATH` 指定外部 DRL 文件目录。

外部规则目录初始化命令：

```bash
mkdir -p /data/app/spring-boot-drools-demo/rules
mkdir -p /data/logs/spring-boot-drools-demo

cp src/main/resources/rules/*.drl /data/app/spring-boot-drools-demo/rules/
```

`mkdir -p` 用于创建应用规则目录和日志目录；`cp` 用于将本地规则文件复制到外部规则目录。生产环境应通过发布脚本、配置中心或规则管理后台下发规则文件，不建议手工复制后不留发布记录。

### 启动验证

启动应用：

```bash
java -jar target/spring-boot-drools-demo.jar \
  --spring.profiles.active=prod
```

如果需要临时指定外部规则目录：

```bash
java -jar target/spring-boot-drools-demo.jar \
  --spring.profiles.active=prod \
  --drools.dynamic-enabled=true \
  --drools.external-rule-path=/data/app/spring-boot-drools-demo/rules
```

启动后先查看日志，确认规则加载成功：

```bash
tail -f /data/logs/spring-boot-drools-demo/app.log
```

重点检查以下日志：

```text
加载Drools规则文件：src/main/resources/rules/order-discount.drl
Drools规则容器构建完成
Drools规则容器已切换
Drools已从外部目录初始化规则容器
```

调用接口验证规则执行：

```bash
curl -X POST 'http://localhost:8080/api/rules/order/execute' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "orderNo": "ORDER202605080007",
    "memberLevel": "GOLD",
    "orderAmount": 1288.00,
    "firstOrder": true
  }'
```

预期返回：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "orderNo": "ORDER202605080007",
    "orderAmount": 1288.00,
    "totalDiscountAmount": 180.00,
    "payableAmount": 1108.00,
    "hitCount": 3,
    "hits": [
      {
        "ruleCode": "ORDER_AMOUNT_DISCOUNT",
        "ruleName": "满1000订单优惠",
        "message": "订单金额满1000，优惠100元",
        "discountAmount": 100.00,
        "priority": 100
      }
    ]
  }
}
```

实际命中数量和优惠金额以当前 DRL 文件为准。如果返回结果中 `hitCount=0`，需要检查请求参数是否满足规则条件，以及 DRL 文件是否已被正确加载。

如果启用了动态规则加载，可以验证刷新接口：

```bash
curl -X POST 'http://localhost:8080/api/rules/manage/reload'
```

预期返回：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "规则刷新成功"
}
```

启动验证清单：

| 验证项           | 检查方式                        |
| ---------------- | ------------------------------- |
| JAR 是否启动     | 查看进程或端口                  |
| 规则是否加载     | 查看启动日志                    |
| 接口是否可用     | 使用 curl 调用规则接口          |
| 参数校验是否生效 | 传入非法参数                    |
| 规则是否命中     | 检查 `hitCount` 和 `hits`       |
| 热更新是否可用   | 调用 `/api/rules/manage/reload` |
| 日志是否正常落盘 | 查看日志文件路径                |

### 运行注意事项

生产环境运行 Spring Boot + Drools 时，需要重点关注规则变更、内存释放、日志量、异常兜底和发布回滚。

运行注意事项如下：

| 项目                  | 注意事项                                    |
| --------------------- | ------------------------------------------- |
| `KieSession` 生命周期 | 每次执行创建，用完必须 `dispose()`          |
| `KieContainer` 热更新 | 新容器编译成功后再切换，失败继续使用旧容器  |
| 规则文件编码          | 统一使用 UTF-8，避免中文注释或字符串乱码    |
| 规则文件变更          | 必须经过测试、评审、发布记录                |
| DRL 中避免外部调用    | 不要在规则中直接查库、调接口或处理事务      |
| 日志级别              | 生产环境不建议长期打开 Drools DEBUG 日志    |
| 规则命中记录          | 关键业务建议记录命中规则编码和业务主键      |
| 参数校验              | 请求进入规则引擎前完成基础校验              |
| 金额计算              | 使用 `BigDecimal`，避免浮点误差             |
| 回滚方案              | 保留上一版本规则文件或规则版本记录          |
| 并发执行              | 不复用同一个 `KieSession`，避免线程安全问题 |
| 安全控制              | 规则刷新接口必须增加认证和权限控制          |

推荐生产发布流程：

```text
开发修改 DRL
    -> 本地规则命中测试
    -> 单元测试和接口测试
    -> 代码评审或规则评审
    -> 构建 JAR 或发布规则文件
    -> 预发环境验证
    -> 生产发布
    -> 检查规则加载日志
    -> 调用接口验证
    -> 观察业务日志和异常日志
```

如果启用动态规则加载，推荐发布流程：

```text
上传新规则文件到临时目录
    -> 执行规则语法校验
    -> 执行规则命中测试
    -> 备份当前线上规则
    -> 替换外部规则目录
    -> 调用规则刷新接口
    -> 验证规则命中结果
    -> 如果失败，恢复旧规则并重新刷新
```

不建议直接在线上修改正在使用的 `.drl` 文件后等待定时扫描自动刷新。更稳妥的方式是先在临时目录准备完整规则集，校验通过后再原子替换目录或通过规则管理后台发布。
