# Spring Boot 集成 QLExpress 开发

本文主要说明在 Spring Boot 3 项目中集成 QLExpress 的基础方式，用于支持业务规则表达式、动态条件判断、金额计算、权限校验、活动规则配置等场景。本文先说明 QLExpress 的定位、适用场景和 Spring Boot 3 集成思路，然后给出 JDK、Spring Boot、Maven 依赖和基础配置项，为后续执行器封装、业务调用、接口设计和测试验证提供基础。

## 技术概述

本节用于说明 QLExpress 在 Spring Boot 业务系统中的定位。QLExpress 不应被理解为替代 Java 业务代码的通用脚本语言，而应作为可配置规则执行层嵌入到业务流程中，用于解决部分规则需要动态调整、配置化管理、快速验证和灵活发布的问题。

### QLExpress 简介

QLExpress 是一个 Java 表达式执行引擎，主要用于在 Java 应用运行时解析并执行动态表达式。业务系统可以将规则条件、计算公式、流程判断逻辑配置为表达式，再通过 Java 代码传入上下文参数，由 QLExpress 完成表达式执行并返回结果。

在 Spring Boot 项目中，QLExpress 通常不会直接暴露给前端调用，而是由后端封装统一的规则执行服务。业务侧只需要传入表达式内容或规则编码，以及本次执行所需的上下文变量，底层由统一执行器完成表达式解析、参数绑定、结果转换、异常处理和日志记录。

典型表达式如下：

```java
amount >= 100 && userLevel == "VIP"
```

该表达式可以用于判断订单金额和用户等级是否满足某个营销规则。表达式中的 `amount` 和 `userLevel` 不是 Java 局部变量，而是执行时从上下文中动态传入的参数。

QLExpress 的基础执行流程通常包括以下几步：

1. 初始化表达式执行器，例如 `ExpressRunner`。
2. 创建表达式上下文，例如 `DefaultContext<String, Object>`。
3. 将业务变量写入上下文，例如订单金额、用户等级、地区编码、活动类型。
4. 执行表达式并获取返回结果。
5. 对执行结果进行类型转换、异常封装和业务返回。

### 适用场景

QLExpress 适用于规则变化频繁、但业务主流程相对稳定的场景。它的主要价值不是替代 Java 代码，而是将部分容易变化的规则逻辑从硬编码中抽离出来，使规则可以配置、审核、测试和动态调整。

| 场景         | 说明                                           | 示例                                     |
| ------------ | ---------------------------------------------- | ---------------------------------------- |
| 业务规则判断 | 将业务判断条件配置为表达式，减少频繁改代码发版 | `amount >= 100 && city == "上海"`        |
| 营销活动规则 | 支持活动门槛、用户分层、商品范围动态调整       | `userLevel == "VIP" && categoryId == 10` |
| 金额计算     | 根据配置表达式计算折扣、积分、返现金额         | `amount * discountRate`                  |
| 权限校验     | 根据角色、组织、资源属性执行动态校验           | `role == "admin"                         |
| 流程分支     | 根据业务参数决定后续流程节点                   | `riskScore > 80 ? "REJECT" : "PASS"`     |
| 数据校验     | 对导入数据、表单数据进行动态规则校验           | `age >= 18 && mobile != null`            |

不建议将 QLExpress 用于以下场景：

| 不建议场景                 | 原因                                           |
| -------------------------- | ---------------------------------------------- |
| 核心交易主流程全部脚本化   | 可维护性、可观测性和问题定位成本较高           |
| 高复杂度业务编排           | 表达式过长后可读性较差，应回到 Java 服务编排   |
| 无权限控制的开放执行       | 可能带来误执行、越权调用或安全风险             |
| 强依赖编译期校验的核心逻辑 | 表达式错误通常在运行期或预校验阶段暴露         |
| 高频极致性能计算           | 动态表达式有额外解析和执行成本，需要缓存和压测 |

实际项目中，推荐将 QLExpress 用于规则层，而不是业务主流程层。例如订单创建、库存扣减、支付回调等主流程仍由 Java 代码控制；是否满足某个规则、如何计算某个配置值、是否进入某个分支，可以交给 QLExpress 处理。

### Spring Boot 3 集成方式

Spring Boot 3 集成 QLExpress 的核心思路是：将 QLExpress 执行器注册为 Spring Bean，再封装统一的规则执行服务，业务代码不直接操作底层执行器。

推荐的集成分层如下：

| 层级         | 作用                                                   |
| ------------ | ------------------------------------------------------ |
| `config`     | 初始化 `ExpressRunner`，绑定配置项                     |
| `properties` | 读取 `application.yml` 中的 QLExpress 配置             |
| `service`    | 封装表达式执行、语法校验、变量解析、结果转换           |
| `controller` | 对外提供表达式执行、规则校验、规则测试接口             |
| `exception`  | 统一处理表达式语法异常、运行时异常和参数异常           |
| `log`        | 记录表达式内容、变量摘要、执行耗时、执行结果和异常信息 |

建议的项目目录结构如下：

```text
src/main/java/io/github/atengk/qlexpress
├── config
│   ├── QLExpressConfig.java
│   └── QLExpressProperties.java
├── controller
│   └── QLExpressController.java
├── dto
│   └── QLExpressExecuteRequest.java
├── service
│   ├── QLExpressService.java
│   └── impl
│       └── QLExpressServiceImpl.java
└── exception
    └── QLExpressExecuteException.java
```

当前章节只需要先完成环境、依赖和基础配置。后续章节再继续补充执行器初始化、执行服务封装、接口设计、异常处理和测试验证。

## 环境准备

本节用于说明 Spring Boot 3 集成 QLExpress 前需要准备的基础环境，包括 JDK 版本、Spring Boot 版本、Maven 依赖和项目配置项。环境准备完成后，后续即可基于 Spring Bean 的方式初始化表达式执行器。

### JDK 与 Spring Boot 版本

Spring Boot 3 项目建议使用 JDK 17 或更高版本。实际开发中应根据公司基础镜像、CI/CD 环境、服务器运行环境和项目依赖统一选择 JDK 版本。

推荐环境如下：

| 组件        | 推荐版本       | 说明                                          |
| ----------- | -------------- | --------------------------------------------- |
| JDK         | 17 或 21       | Spring Boot 3 最低建议使用 JDK 17             |
| Spring Boot | 3.2.x 或 3.3.x | 根据公司基础框架版本统一选择                  |
| Maven       | 3.6.3+         | 用于项目构建和依赖管理                        |
| QLExpress   | 3.3.4          | 适用于使用 `ExpressRunner` 的集成方式         |
| Lombok      | 1.18.x         | 简化配置类、DTO、日志对象等样板代码           |
| Hutool      | 5.8.x          | 用于字符串、集合、Map、类型转换等常用工具处理 |

本开发文档默认使用以下组合：

```text
JDK：17
Spring Boot：3.2.x
QLExpress：3.3.4
Maven：3.6.3+
```

如果项目已经统一升级到 JDK 21，也可以使用 JDK 21 运行 Spring Boot 3 项目，但需要确保构建环境、运行环境、基础镜像和线上 JVM 参数保持一致。

### Maven 依赖配置

本节给出 Spring Boot 3 集成 QLExpress 的 Maven 依赖配置。由于后续章节使用的是 `ExpressRunner`，这里优先采用 QLExpress 3.x 的依赖方式。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web：提供表达式执行、规则校验、规则测试等 HTTP 接口能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation：用于接口参数校验，例如表达式不能为空、变量参数不能为空 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- QLExpress：表达式解析与执行引擎，适用于 ExpressRunner 集成方式 -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>QLExpress</artifactId>
        <version>3.3.4</version>
    </dependency>

    <!-- Hutool：用于字符串、集合、Map、类型转换等常用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：减少 Getter、Setter、构造方法和日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：用于表达式执行服务、规则校验接口的单元测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用的是 Spring Boot 父工程管理版本，通常不需要单独指定 `spring-boot-starter-web`、`spring-boot-starter-validation`、`spring-boot-starter-test` 的版本。QLExpress 和 Hutool 可以根据公司依赖管理规范统一放到父工程的 `dependencyManagement` 中管理。

父工程版本示例：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.12</version>
    <relativePath/>
</parent>

<properties>
    <!-- Java 编译版本 -->
    <java.version>17</java.version>

    <!-- QLExpress 表达式引擎版本 -->
    <qlexpress.version>3.3.4</qlexpress.version>

    <!-- Hutool 工具包版本 -->
    <hutool.version>5.8.35</hutool.version>
</properties>
```

对应依赖可以改为使用属性版本：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>QLExpress</artifactId>
    <version>${qlexpress.version}</version>
</dependency>

<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>${hutool.version}</version>
</dependency>
```

### 基础配置项

本节定义 QLExpress 在 Spring Boot 中的基础配置项。配置项不宜过多，前期建议只保留启用开关、缓存开关、执行超时时间、日志开关和安全控制相关参数。复杂函数扩展、操作符扩展和表达式白名单可以在后续章节继续补充。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot-qlexpress-demo

qlexpress:
  # 是否启用表达式执行功能，关闭后接口可直接拒绝执行
  enabled: true

  # 是否开启表达式缓存，生产环境建议开启，减少重复解析成本
  cache: true

  # 表达式执行超时时间，单位毫秒，用于避免复杂表达式长时间占用线程
  timeout-ms: 3000

  # 是否输出表达式执行日志，生产环境建议记录摘要，不建议记录敏感完整参数
  log-enabled: true

  # 是否允许脚本调用 Java 方法，生产环境应谨慎开启
  allow-java-method: false

  # 是否开启严格模式，建议生产环境开启，避免变量缺失时产生不可控结果
  strict-mode: true
```

为了让配置可以被 Spring Boot 自动绑定，需要定义一个配置属性类。该类只负责承载配置，不直接执行表达式。

文件位置：`src/main/java/io/github/atengk/qlexpress/config/QLExpressProperties.java`

```java
package io.github.atengk.qlexpress.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * QLExpress 基础配置属性。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@ConfigurationProperties(prefix = "qlexpress")
public class QLExpressProperties {

    /**
     * 是否启用 QLExpress 表达式执行功能。
     */
    private Boolean enabled = true;

    /**
     * 是否开启表达式缓存。
     */
    private Boolean cache = true;

    /**
     * 表达式执行超时时间，单位毫秒。
     */
    private Long timeoutMs = 3000L;

    /**
     * 是否记录表达式执行日志。
     */
    private Boolean logEnabled = true;

    /**
     * 是否允许表达式调用 Java 方法。
     */
    private Boolean allowJavaMethod = false;

    /**
     * 是否开启严格模式。
     */
    private Boolean strictMode = true;
}
```

接着在配置类中启用配置属性绑定，并初始化 `ExpressRunner`。这里先给出基础配置骨架，具体函数注册、操作符扩展和安全策略可放到后续“基础集成”章节继续完善。

文件位置：`src/main/java/io/github/atengk/qlexpress/config/QLExpressConfig.java`

```java
package io.github.atengk.qlexpress.config;

import com.ql.util.express.ExpressRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QLExpress 执行器配置。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(QLExpressProperties.class)
@ConditionalOnProperty(prefix = "qlexpress", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QLExpressConfig {

    private final QLExpressProperties qlExpressProperties;

    /**
     * 初始化 QLExpress 执行器。
     *
     * @return 表达式执行器
     */
    @Bean
    public ExpressRunner expressRunner() {
        ExpressRunner runner = new ExpressRunner(
                qlExpressProperties.getCache(),
                qlExpressProperties.getStrictMode()
        );

        log.info("初始化 QLExpress 执行器完成，cache={}，strictMode={}",
                qlExpressProperties.getCache(),
                qlExpressProperties.getStrictMode());

        return runner;
    }
}
```

基础配置项建议按以下原则管理：

| 配置项                        | 建议值  | 说明                                           |
| ----------------------------- | ------- | ---------------------------------------------- |
| `qlexpress.enabled`           | `true`  | 用于控制表达式执行功能是否启用                 |
| `qlexpress.cache`             | `true`  | 生产环境建议开启，避免表达式重复解析           |
| `qlexpress.timeout-ms`        | `3000`  | 建议设置执行超时，避免异常表达式长时间占用线程 |
| `qlexpress.log-enabled`       | `true`  | 建议开启执行日志，但敏感参数需要脱敏           |
| `qlexpress.allow-java-method` | `false` | 默认不开放 Java 方法调用，降低安全风险         |
| `qlexpress.strict-mode`       | `true`  | 建议开启，便于尽早发现变量缺失和表达式错误     |

配置完成后，可以先启动项目，通过启动日志确认 `ExpressRunner` 是否被成功初始化。后续在“基础集成”章节中，再基于该 Bean 封装统一的表达式执行方法，例如：

```java
Object execute(String expression, Map<String, Object> params);

Boolean executeAsBoolean(String expression, Map<String, Object> params);

<T> T execute(String expression, Map<String, Object> params, Class<T> resultType);
```

通过统一封装，可以避免业务代码直接依赖 QLExpress API，也便于后续统一增加日志记录、异常处理、参数校验、执行耗时统计和表达式安全控制。



## 核心设计

本节用于说明 QLExpress 在 Spring Boot 3 项目中的整体设计方式。推荐将 QLExpress 封装为独立的规则执行能力，不让业务代码直接操作 `ExpressRunner`，这样可以统一处理参数校验、上下文构建、执行超时、结果转换、异常封装和日志记录。

### 表达式执行流程

表达式执行流程应围绕“表达式内容 + 上下文参数 + 执行配置 + 返回结果”进行设计。QLExpress 3.x 的基础调用方式通常是初始化 `ExpressRunner`，创建 `DefaultContext<String, Object>`，然后调用 `runner.execute(expression, context, errorList, isCache, isTrace)` 执行表达式。([Libraries.io](https://libraries.io/maven/com.alibaba%3AQLExpress?utm_source=chatgpt.com))

推荐执行流程如下：

```text
接收表达式与参数
        ↓
校验表达式是否为空
        ↓
校验变量参数是否合法
        ↓
构建 QLExpress 上下文
        ↓
执行表达式
        ↓
转换执行结果
        ↓
记录执行日志
        ↓
返回业务结果
```

在项目中，表达式执行流程不建议散落在 Controller 或具体业务 Service 中，而应统一封装到 `QLExpressService` 中。业务模块只关注“执行什么规则”和“传入什么参数”，不关注底层表达式引擎的 API 细节。

推荐的核心执行步骤如下：

| 步骤       | 处理内容                                                     | 说明                                 |
| ---------- | ------------------------------------------------------------ | ------------------------------------ |
| 表达式校验 | 判断表达式是否为空、是否超长、是否包含禁用关键字             | 避免无效表达式进入执行阶段           |
| 参数校验   | 校验变量名、变量值、参数数量                                 | 防止变量缺失或非法变量名导致运行异常 |
| 上下文构建 | 将 `Map<String, Object>` 转换为 `DefaultContext`             | QLExpress 通过上下文读取变量         |
| 表达式执行 | 调用 `ExpressRunner.execute`                                 | 统一设置缓存、跟踪、超时等参数       |
| 结果转换   | 将 `Object` 转换为 `Boolean`、`String`、`BigDecimal` 等业务类型 | 避免业务层重复转换                   |
| 异常封装   | 将语法异常、执行异常、超时异常封装为业务异常                 | 统一错误响应                         |
| 日志记录   | 记录表达式摘要、参数摘要、耗时、结果类型                     | 方便问题排查和审计                   |

QLExpress 支持通过带 `timeoutMillis` 的 `execute` 重载方法设置脚本运行超时时间，可用于防止复杂表达式或循环表达式长时间占用线程。([Libraries.io](https://libraries.io/maven/com.alibaba%3AQLExpress?utm_source=chatgpt.com))

### 脚本上下文设计

脚本上下文用于承载表达式执行时需要访问的变量。QLExpress 的上下文对象可以使用 `DefaultContext<String, Object>`，业务系统通常从请求参数、数据库规则配置、当前登录用户、订单信息或风控数据中组装上下文变量。

上下文设计应遵循以下原则：

| 原则             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 变量名清晰       | 使用 `amount`、`userLevel`、`orderStatus` 等业务语义明确的变量名 |
| 避免过深对象访问 | 尽量减少 `order.user.profile.level` 这类深层访问             |
| 控制参数范围     | 只传入表达式需要的变量，不传入完整业务对象                   |
| 禁止敏感对象     | 不要传入 `HttpServletRequest`、`DataSource`、`ApplicationContext` 等对象 |
| 统一基础变量     | 可统一注入 `now`、`tenantId`、`userId`、`bizType` 等公共变量 |
| 结果类型明确     | 表达式应明确返回布尔值、数值、字符串或 Map，不建议返回复杂对象 |

推荐上下文变量示例：

```json
{
  "amount": 120.50,
  "userLevel": "VIP",
  "city": "上海",
  "orderStatus": "PAID",
  "riskScore": 20,
  "tenantId": 10001
}
```

对应表达式示例：

```java
amount >= 100 && userLevel == "VIP" && orderStatus == "PAID"
```

如果业务对象较复杂，建议在进入表达式执行前先做参数扁平化。例如不要直接把完整 `Order` 对象放入上下文，而是提取表达式真正需要的字段。

```java
orderAmount >= 100 && orderStatus == "PAID" && buyerLevel == "VIP"
```

上下文构建建议封装为独立方法，后续可以统一增加变量名校验、公共变量注入、敏感字段过滤和参数脱敏。

### 执行器封装设计

执行器封装的目标是屏蔽 QLExpress 原生 API，让业务侧通过稳定的服务接口调用表达式执行能力。推荐提供通用执行、布尔执行、字符串执行和指定类型执行几类方法。

文件位置：`src/main/java/io/github/atengk/qlexpress/service/QLExpressService.java`

```java
package io.github.atengk.qlexpress.service;

import java.util.Map;

/**
 * QLExpress 表达式执行服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface QLExpressService {

    /**
     * 执行表达式并返回原始结果。
     *
     * @param expression 表达式内容
     * @param params 上下文参数
     * @return 执行结果
     */
    Object execute(String expression, Map<String, Object> params);

    /**
     * 执行表达式并返回布尔结果。
     *
     * @param expression 表达式内容
     * @param params 上下文参数
     * @return 布尔执行结果
     */
    Boolean executeAsBoolean(String expression, Map<String, Object> params);

    /**
     * 执行表达式并返回字符串结果。
     *
     * @param expression 表达式内容
     * @param params 上下文参数
     * @return 字符串执行结果
     */
    String executeAsString(String expression, Map<String, Object> params);

    /**
     * 执行表达式并转换为指定类型。
     *
     * @param expression 表达式内容
     * @param params 上下文参数
     * @param resultType 返回结果类型
     * @param <T> 结果泛型
     * @return 指定类型结果
     */
    <T> T execute(String expression, Map<String, Object> params, Class<T> resultType);

    /**
     * 校验表达式语法是否正确。
     *
     * @param expression 表达式内容
     * @return 是否校验通过
     */
    Boolean checkSyntax(String expression);
}
```

表达式执行异常建议使用业务异常统一封装，避免底层异常直接透出到接口层。

文件位置：`src/main/java/io/github/atengk/qlexpress/exception/QLExpressExecuteException.java`

```java
package io.github.atengk.qlexpress.exception;

/**
 * QLExpress 表达式执行异常。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public class QLExpressExecuteException extends RuntimeException {

    public QLExpressExecuteException(String message) {
        super(message);
    }

    public QLExpressExecuteException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 基础集成

本节用于给出 QLExpress 在 Spring Boot 3 中的基础集成代码，包括 `ExpressRunner` 初始化、表达式执行方法封装、变量参数传递和执行结果处理。当前示例基于 QLExpress 3.3.4，Maven Central 当前也提供 `com.alibaba:QLExpress:3.3.4` 依赖。([Maven Central](https://central.sonatype.com/artifact/com.alibaba/QLExpress?utm_source=chatgpt.com))

### ExpressRunner 初始化

`ExpressRunner` 建议注册为单例 Spring Bean。表达式执行器本身由 Spring 容器统一管理，业务代码通过 `QLExpressService` 间接使用，不直接创建 `new ExpressRunner()`。

文件位置：`src/main/java/io/github/atengk/qlexpress/config/QLExpressConfig.java`

```java
package io.github.atengk.qlexpress.config;

import com.ql.util.express.ExpressRunner;
import io.github.atengk.qlexpress.function.QLExpressBizFunction;
import io.github.atengk.qlexpress.function.UserLevelMatchFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QLExpress 执行器配置。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(QLExpressProperties.class)
@ConditionalOnProperty(prefix = "qlexpress", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QLExpressConfig {

    /**
     * 初始化 QLExpress 执行器。
     *
     * @param qlExpressBizFunction 业务函数扩展
     * @return 表达式执行器
     * @throws Exception 函数注册异常
     */
    @Bean
    public ExpressRunner expressRunner(QLExpressBizFunction qlExpressBizFunction) throws Exception {
        ExpressRunner runner = new ExpressRunner();

        // 注册 Java 服务方法，表达式中可直接调用 hasText、calcDiscountAmount
        runner.addFunctionOfServiceMethod(
                "hasText",
                qlExpressBizFunction,
                "hasText",
                new Class[]{String.class},
                null
        );

        runner.addFunctionOfServiceMethod(
                "calcDiscountAmount",
                qlExpressBizFunction,
                "calcDiscountAmount",
                new Class[]{Object.class, Object.class},
                null
        );

        // 注册自定义函数
        runner.addFunction("matchUserLevel", new UserLevelMatchFunction());

        log.info("初始化 QLExpress 执行器完成，已注册基础业务函数");
        return runner;
    }
}
```

这里使用无参构造器初始化 `ExpressRunner`，表达式缓存、执行追踪和超时时间在实际执行时通过 `execute` 方法参数控制。这样配置更集中，也便于后续根据不同场景选择是否开启缓存。

### 表达式执行方法封装

表达式执行方法应集中封装在 `ServiceImpl` 中。这里会完成表达式校验、上下文构建、执行调用、结果转换和异常处理。

文件位置：`src/main/java/io/github/atengk/qlexpress/service/impl/QLExpressServiceImpl.java`

```java
package io.github.atengk.qlexpress.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.IExpressContext;
import io.github.atengk.qlexpress.config.QLExpressProperties;
import io.github.atengk.qlexpress.exception.QLExpressExecuteException;
import io.github.atengk.qlexpress.service.QLExpressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * QLExpress 表达式执行服务实现。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QLExpressServiceImpl implements QLExpressService {

    private final ExpressRunner expressRunner;
    private final QLExpressProperties qlExpressProperties;

    /**
     * 执行表达式并返回原始结果。
     *
     * @param expression 表达式内容
     * @param params 上下文参数
     * @return 执行结果
     */
    @Override
    public Object execute(String expression, Map<String, Object> params) {
        validateExpression(expression);

        Map<String, Object> safeParams = MapUtil.isEmpty(params) ? Collections.emptyMap() : params;
        IExpressContext<String, Object> context = buildContext(safeParams);
        List<String> errorList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        String expressionDigest = DigestUtil.md5Hex(expression);

        try {
            Object result = expressRunner.execute(
                    expression,
                    context,
                    errorList,
                    BooleanUtil.isTrue(qlExpressProperties.getCache()),
                    false,
                    getTimeoutMillis()
            );

            long cost = System.currentTimeMillis() - startTime;

            if (BooleanUtil.isTrue(qlExpressProperties.getLogEnabled())) {
                log.info("QLExpress表达式执行成功，表达式摘要={}，耗时={}ms，结果类型={}",
                        expressionDigest,
                        cost,
                        ObjectUtil.isNull(result) ? "null" : result.getClass().getSimpleName());
            }

            if (!errorList.isEmpty()) {
                log.warn("QLExpress表达式执行存在错误信息，表达式摘要={}，错误信息={}", expressionDigest, errorList);
            }

            return result;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("QLExpress表达式执行失败，表达式摘要={}，耗时={}ms", expressionDigest, cost, e);
            throw new QLExpressExecuteException("表达式执行失败：" + e.getMessage(), e);
        }
    }

    /**
     * 执行表达式并返回布尔结果。
     *
     * @param expression 表达式内容
     * @param params 上下文参数
     * @return 布尔执行结果
     */
    @Override
    public Boolean executeAsBoolean(String expression, Map<String, Object> params) {
        Boolean result = execute(expression, params, Boolean.class);
        return BooleanUtil.isTrue(result);
    }

    /**
     * 执行表达式并返回字符串结果。
     *
     * @param expression 表达式内容
     * @param params 上下文参数
     * @return 字符串执行结果
     */
    @Override
    public String executeAsString(String expression, Map<String, Object> params) {
        Object result = execute(expression, params);
        return ObjectUtil.isNull(result) ? null : Convert.toStr(result);
    }

    /**
     * 执行表达式并转换为指定类型。
     *
     * @param expression 表达式内容
     * @param params 上下文参数
     * @param resultType 返回结果类型
     * @param <T> 结果泛型
     * @return 指定类型结果
     */
    @Override
    public <T> T execute(String expression, Map<String, Object> params, Class<T> resultType) {
        Object result = execute(expression, params);
        if (ObjectUtil.isNull(result)) {
            return null;
        }

        try {
            return Convert.convert(resultType, result);
        } catch (Exception e) {
            log.error("QLExpress表达式结果转换失败，目标类型={}", resultType.getName(), e);
            throw new QLExpressExecuteException("表达式结果转换失败：" + e.getMessage(), e);
        }
    }

    /**
     * 校验表达式语法是否正确。
     *
     * @param expression 表达式内容
     * @return 是否校验通过
     */
    @Override
    public Boolean checkSyntax(String expression) {
        validateExpression(expression);

        try {
            expressRunner.parseInstructionSet(expression);
            return true;
        } catch (Exception e) {
            log.warn("QLExpress表达式语法校验失败，表达式摘要={}", DigestUtil.md5Hex(expression), e);
            return false;
        }
    }

    /**
     * 构建表达式上下文。
     *
     * @param params 上下文参数
     * @return QLExpress 上下文
     */
    private IExpressContext<String, Object> buildContext(Map<String, Object> params) {
        DefaultContext<String, Object> context = new DefaultContext<>();

        params.forEach((key, value) -> {
            if (StrUtil.isBlank(key)) {
                throw new QLExpressExecuteException("表达式变量名不能为空");
            }
            context.put(key, value);
        });

        return context;
    }

    /**
     * 校验表达式内容。
     *
     * @param expression 表达式内容
     */
    private void validateExpression(String expression) {
        if (StrUtil.isBlank(expression)) {
            throw new QLExpressExecuteException("表达式不能为空");
        }

        if (expression.length() > 5000) {
            throw new QLExpressExecuteException("表达式长度不能超过 5000 个字符");
        }
    }

    /**
     * 获取表达式执行超时时间。
     *
     * @return 超时时间，单位毫秒
     */
    private int getTimeoutMillis() {
        Long timeoutMs = qlExpressProperties.getTimeoutMs();
        return Convert.toInt(ObjectUtil.defaultIfNull(timeoutMs, 3000L));
    }
}
```

该实现中，表达式日志只记录 MD5 摘要，不直接打印完整表达式和完整参数。生产环境中，如果表达式或参数可能包含手机号、身份证号、金额、客户信息等敏感数据，应优先记录规则编码、表达式版本、参数摘要和执行耗时。

### 变量参数传递

变量参数通过 `Map<String, Object>` 传入，再转换为 QLExpress 的上下文对象。表达式中的变量名必须与 Map 中的 Key 保持一致。

普通规则判断示例：

```java
Map<String, Object> params = Map.of(
        "amount", 120,
        "userLevel", "VIP",
        "orderStatus", "PAID"
);

String expression = "amount >= 100 && userLevel == \"VIP\" && orderStatus == \"PAID\"";

Boolean matched = qlExpressService.executeAsBoolean(expression, params);
```

金额计算示例：

```java
Map<String, Object> params = Map.of(
        "amount", 200,
        "discountRate", 0.8
);

String expression = "amount * discountRate";

String result = qlExpressService.executeAsString(expression, params);
```

流程分支示例：

```java
Map<String, Object> params = Map.of(
        "riskScore", 85,
        "manualReview", false
);

String expression = "riskScore >= 80 ? \"REJECT\" : (manualReview ? \"REVIEW\" : \"PASS\")";

String decision = qlExpressService.executeAsString(expression, params);
```

变量参数设计时建议遵循以下规范：

| 类型 | 命名示例                                 | 说明                                 |
| ---- | ---------------------------------------- | ------------------------------------ |
| 金额 | `amount`、`payAmount`、`discountAmount`  | 建议统一使用 `BigDecimal` 或数值类型 |
| 用户 | `userId`、`userLevel`、`memberType`      | 不建议传入完整用户对象               |
| 订单 | `orderStatus`、`orderType`、`goodsCount` | 推荐传入规则需要的字段               |
| 风控 | `riskScore`、`blackUser`                 | 返回值通常用于流程分支               |
| 时间 | `now`、`createTime`、`expireTime`        | 建议由服务端统一注入                 |

如果需要传递复杂结构，可以传入 `Map`，但不建议过度依赖多层对象访问。复杂结构会增加表达式阅读和排错成本。

```java
Map<String, Object> order = Map.of(
        "amount", 120,
        "status", "PAID"
);

Map<String, Object> params = Map.of(
        "order", order,
        "userLevel", "VIP"
);

String expression = "order.amount >= 100 && order.status == \"PAID\" && userLevel == \"VIP\"";
```

### 执行结果处理

QLExpress 的原始执行结果是 `Object`，业务系统需要根据规则用途转换为明确类型。建议在执行服务中提供类型化方法，避免业务层重复写类型判断和转换逻辑。

常见结果类型如下：

| 表达式用途 | 推荐返回类型                    | 示例                                 |
| ---------- | ------------------------------- | ------------------------------------ |
| 条件判断   | `Boolean`                       | `amount >= 100`                      |
| 金额计算   | `BigDecimal`、`String`          | `amount * discountRate`              |
| 流程分支   | `String`                        | `riskScore > 80 ? "REJECT" : "PASS"` |
| 分值计算   | `Integer`、`Long`、`BigDecimal` | `baseScore + extraScore`             |
| 简单对象   | `Map`                           | 返回多个结果字段时使用               |

布尔结果处理示例：

```java
Boolean matched = qlExpressService.executeAsBoolean(
        "amount >= 100 && userLevel == \"VIP\"",
        Map.of("amount", 120, "userLevel", "VIP")
);

if (BooleanUtil.isTrue(matched)) {
    log.info("规则匹配成功，执行后续业务逻辑");
}
```

指定类型结果处理示例：

```java
Integer score = qlExpressService.execute(
        "baseScore + extraScore",
        Map.of("baseScore", 80, "extraScore", 10),
        Integer.class
);
```

字符串分支结果处理示例：

```java
String decision = qlExpressService.executeAsString(
        "riskScore >= 80 ? \"REJECT\" : \"PASS\"",
        Map.of("riskScore", 90)
);

log.info("规则决策结果：{}", decision);
```

结果处理建议集中在执行器封装层完成。Controller 或业务 Service 不应直接操作 QLExpress 返回的原始对象，除非该业务确实需要保留动态类型。

## 业务使用

本节用于说明业务开发中如何编写表达式、如何动态绑定参数、如何调用 Java 方法，以及如何扩展自定义函数。业务使用层应重点关注表达式可读性、参数稳定性、安全边界和可测试性。

### 规则表达式编写

规则表达式应尽量短小、明确、可测试。一个表达式只建议表达一个规则意图，不建议在单条表达式中堆叠过多业务逻辑。

推荐写法：

```java
amount >= 100 && userLevel == "VIP"
```

不推荐写法：

```java
amount >= 100 && userLevel == "VIP" && orderStatus == "PAID" && city == "上海" && riskScore < 60 && goodsCount > 0 && couponUsed == false
```

如果规则条件过多，建议拆分为多个规则项，由 Java 业务代码编排执行顺序，或者在规则配置层增加规则组、优先级和命中策略。

常见表达式示例：

| 业务场景       | 表达式                                |
| -------------- | ------------------------------------- |
| 判断订单金额   | `amount >= 100`                       |
| 判断会员等级   | `userLevel == "VIP"`                  |
| 判断订单状态   | `orderStatus == "PAID"`               |
| 多条件组合     | `amount >= 100 && userLevel == "VIP"` |
| 风控分支       | `riskScore >= 80 ? "REJECT" : "PASS"` |
| 折扣金额       | `amount * discountRate`               |
| 商品数量校验   | `goodsCount > 0`                      |
| 字符串非空判断 | `hasText(mobile)`                     |

表达式编写建议：

| 建议                     | 说明                                       |
| ------------------------ | ------------------------------------------ |
| 使用明确变量名           | 避免 `a`、`b`、`x` 这类无业务含义变量      |
| 字符串使用双引号         | 统一表达式风格                             |
| 条件不要过长             | 复杂条件拆为多个表达式                     |
| 避免直接调用任意 Java 类 | 优先通过受控函数扩展                       |
| 配置规则编码             | 表达式应绑定规则编码、版本、说明和启停状态 |
| 上线前先校验             | 保存表达式前先调用语法校验接口             |

### 动态参数绑定

动态参数绑定是指业务系统在运行时根据订单、用户、商品、风控、活动等数据组装表达式上下文。参数绑定逻辑建议放在业务规则适配层，不建议散落在多个 Controller 中。

以下示例展示订单优惠规则的参数绑定方式。

文件位置：`src/main/java/io/github/atengk/qlexpress/example/OrderRuleExampleService.java`

```java
package io.github.atengk.qlexpress.example;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.qlexpress.service.QLExpressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单规则示例服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRuleExampleService {

    private final QLExpressService qlExpressService;

    /**
     * 判断订单是否满足优惠规则。
     *
     * @param amount 订单金额
     * @param userLevel 用户等级
     * @param orderStatus 订单状态
     * @return 是否满足规则
     */
    public Boolean matchDiscountRule(BigDecimal amount, String userLevel, String orderStatus) {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("userLevel", userLevel);
        params.put("orderStatus", orderStatus);

        String expression = "amount >= 100 && userLevel == \"VIP\" && orderStatus == \"PAID\"";
        Boolean matched = qlExpressService.executeAsBoolean(expression, params);

        log.info("订单优惠规则匹配完成，matched={}，参数是否为空={}", matched, MapUtil.isEmpty(params));
        return matched;
    }

    /**
     * 计算订单折扣金额。
     *
     * @param amount 订单金额
     * @param discountRate 折扣比例
     * @return 折扣后金额
     */
    public BigDecimal calculateDiscountAmount(BigDecimal amount, BigDecimal discountRate) {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("discountRate", discountRate);

        String expression = "amount * discountRate";
        BigDecimal result = qlExpressService.execute(expression, params, BigDecimal.class);

        log.info("订单折扣金额计算完成，result={}", result);
        return result;
    }
}
```

动态参数绑定时，需要注意变量名稳定性。规则配置一旦使用了 `amount`、`userLevel`、`orderStatus` 等变量名，后续业务代码不能随意改名，否则会导致线上规则执行失败。

推荐建立规则变量清单，例如：

| 变量名        | 类型         | 说明     |
| ------------- | ------------ | -------- |
| `amount`      | `BigDecimal` | 订单金额 |
| `userLevel`   | `String`     | 用户等级 |
| `orderStatus` | `String`     | 订单状态 |
| `goodsCount`  | `Integer`    | 商品数量 |
| `riskScore`   | `Integer`    | 风控分值 |
| `tenantId`    | `Long`       | 租户 ID  |

### Java 方法调用

QLExpress 可以通过注册服务方法的方式，让表达式调用 Java 中受控开放的方法。官方示例中，`addFunctionOfServiceMethod` 可将 Java 对象方法注册为表达式函数。([Libraries.io](https://libraries.io/maven/com.alibaba%3AQLExpress?utm_source=chatgpt.com))

生产环境不建议开放表达式任意调用 Java 类或 Spring Bean。更推荐的方式是：只注册经过审核的业务函数，例如字符串非空判断、金额计算、枚举匹配、名单判断等。

文件位置：`src/main/java/io/github/atengk/qlexpress/function/QLExpressBizFunction.java`

```java
package io.github.atengk.qlexpress.function;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * QLExpress 业务函数扩展。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class QLExpressBizFunction {

    /**
     * 判断字符串是否有文本。
     *
     * @param value 字符串
     * @return 是否有文本
     */
    public Boolean hasText(String value) {
        return StrUtil.isNotBlank(value);
    }

    /**
     * 计算折扣后金额。
     *
     * @param amount 原始金额
     * @param discountRate 折扣比例
     * @return 折扣后金额
     */
    public BigDecimal calcDiscountAmount(Object amount, Object discountRate) {
        if (ObjectUtil.hasNull(amount, discountRate)) {
            log.warn("计算折扣金额失败，金额或折扣比例为空");
            return BigDecimal.ZERO;
        }

        BigDecimal amountValue = Convert.toBigDecimal(amount);
        BigDecimal discountRateValue = Convert.toBigDecimal(discountRate);

        return NumberUtil.mul(amountValue, discountRateValue);
    }
}
```

注册完成后，表达式中可以直接使用这些函数：

```java
hasText(mobile)
calcDiscountAmount(amount, discountRate)
```

调用示例：

```java
Map<String, Object> params = Map.of(
        "mobile", "13800138000",
        "amount", new BigDecimal("200"),
        "discountRate", new BigDecimal("0.8")
);

Boolean mobileValid = qlExpressService.executeAsBoolean("hasText(mobile)", params);

BigDecimal payAmount = qlExpressService.execute(
        "calcDiscountAmount(amount, discountRate)",
        params,
        BigDecimal.class
);
```

Java 方法调用建议只开放稳定、无副作用、可重复执行的方法。不要在表达式函数中执行数据库写入、远程调用、发送消息、扣减库存等有副作用的操作。

### 自定义函数扩展

除了注册 Java 服务方法，也可以通过继承 QLExpress 的 `Operator` 扩展自定义函数。`Operator` 需要实现 `executeInner(Object[] list)` 方法，然后通过 `ExpressRunner.addFunction` 注册到执行器中。([Libraries.io](https://libraries.io/maven/com.alibaba%3AQLExpress?utm_source=chatgpt.com))

下面示例实现一个用户等级匹配函数 `matchUserLevel(userLevel, targetLevel)`。

文件位置：`src/main/java/io/github/atengk/qlexpress/function/UserLevelMatchFunction.java`

```java
package io.github.atengk.qlexpress.function;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.Operator;

/**
 * 用户等级匹配函数。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public class UserLevelMatchFunction extends Operator {

    /**
     * 执行用户等级匹配。
     *
     * @param list 参数列表
     * @return 是否匹配
     */
    @Override
    public Object executeInner(Object[] list) {
        if (ArrayUtil.isEmpty(list) || list.length < 2) {
            return false;
        }

        String userLevel = StrUtil.toString(list[0]);
        String targetLevel = StrUtil.toString(list[1]);

        return StrUtil.equalsIgnoreCase(userLevel, targetLevel);
    }
}
```

注册位置已经在前面的 `QLExpressConfig` 中完成：

```java
runner.addFunction("matchUserLevel", new UserLevelMatchFunction());
```

表达式使用示例：

```java
matchUserLevel(userLevel, "VIP")
```

Java 调用示例：

```java
Map<String, Object> params = Map.of(
        "userLevel", "VIP"
);

Boolean matched = qlExpressService.executeAsBoolean(
        "matchUserLevel(userLevel, \"VIP\")",
        params
);
```

自定义函数扩展建议遵循以下规范：

| 规范         | 说明                                         |
| ------------ | -------------------------------------------- |
| 函数名稳定   | 函数名一旦被规则配置使用，不应随意改名       |
| 参数数量明确 | 参数不足时返回明确结果或抛出业务异常         |
| 避免副作用   | 函数内部不做写库、发消息、调用外部接口等操作 |
| 日志适度     | 高频函数不建议每次都打印日志                 |
| 统一注册     | 所有函数在 `QLExpressConfig` 中集中注册      |
| 单元测试覆盖 | 每个自定义函数都应覆盖正常、异常、空值等场景 |

业务中常见的自定义函数还可以包括：

| 函数名               | 用途                 | 示例                                       |
| -------------------- | -------------------- | ------------------------------------------ |
| `matchUserLevel`     | 判断用户等级         | `matchUserLevel(userLevel, "VIP")`         |
| `hasText`            | 判断字符串非空       | `hasText(mobile)`                          |
| `calcDiscountAmount` | 计算折扣后金额       | `calcDiscountAmount(amount, discountRate)` |
| `containsCity`       | 判断城市是否在范围内 | `containsCity(city, "上海,杭州,深圳")`     |
| `matchOrderStatus`   | 判断订单状态         | `matchOrderStatus(orderStatus, "PAID")`    |

在业务规则平台中，自定义函数应配套函数说明文档，至少包含函数名、参数列表、返回类型、使用示例和注意事项。这样运营、测试和后端开发在配置规则时可以保持一致理解。



## 接口设计

本节用于定义 QLExpress 对外提供的基础接口，包括表达式执行接口、规则校验接口和规则测试接口。接口层只负责接收请求、参数校验和返回结果，不直接操作 `ExpressRunner`，具体执行逻辑统一交给 `QLExpressService` 处理。

接口建议分为三类：

| 接口           | 作用                                 | 适用场景                   |
| -------------- | ------------------------------------ | -------------------------- |
| 表达式执行接口 | 直接执行表达式并返回结果             | 后端服务调用、规则平台调试 |
| 规则校验接口   | 校验表达式语法是否正确               | 保存规则前校验             |
| 规则测试接口   | 使用测试参数执行表达式并返回详细结果 | 规则配置页面调试           |

### 表达式执行接口

表达式执行接口用于接收表达式内容和上下文参数，执行后返回表达式结果。该接口适合内部系统、规则平台或测试工具调用，不建议直接开放给公网。

请求参数建议包含以下字段：

| 字段         | 类型                  | 是否必填 | 说明                                             |
| ------------ | --------------------- | -------- | ------------------------------------------------ |
| `expression` | `String`              | 是       | 表达式内容                                       |
| `params`     | `Map<String, Object>` | 否       | 表达式上下文变量                                 |
| `resultType` | `String`              | 否       | 期望返回类型，例如 `object`、`boolean`、`string` |

文件位置：`src/main/java/io/github/atengk/qlexpress/dto/QLExpressExecuteRequest.java`

```java
package io.github.atengk.qlexpress.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * QLExpress 表达式执行请求。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class QLExpressExecuteRequest {

    /**
     * 表达式内容。
     */
    @NotBlank(message = "表达式不能为空")
    private String expression;

    /**
     * 表达式上下文参数。
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 返回结果类型：object、boolean、string。
     */
    private String resultType = "object";
}
```

为了统一接口响应结构，可以定义一个简单的响应对象。实际项目中如果已有统一返回类，可以直接复用公司现有的 `Result`、`R` 或 `ApiResponse`。

文件位置：`src/main/java/io/github/atengk/qlexpress/dto/ApiResult.java`

```java
package io.github.atengk.qlexpress.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用接口响应结果。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 响应编码。
     */
    private String code;

    /**
     * 响应消息。
     */
    private String message;

    /**
     * 响应数据。
     */
    private T data;

    /**
     * 返回成功结果。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, "200", "操作成功", data);
    }

    /**
     * 返回失败结果。
     *
     * @param code 错误编码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 失败结果
     */
    public static <T> ApiResult<T> fail(String code, String message) {
        return new ApiResult<>(false, code, message, null);
    }
}
```

表达式执行接口放在 Controller 中，对外提供 HTTP 调用入口。

文件位置：`src/main/java/io/github/atengk/qlexpress/controller/QLExpressController.java`

```java
package io.github.atengk.qlexpress.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.qlexpress.dto.ApiResult;
import io.github.atengk.qlexpress.dto.QLExpressExecuteRequest;
import io.github.atengk.qlexpress.service.QLExpressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * QLExpress 表达式接口。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/qlexpress")
public class QLExpressController {

    private final QLExpressService qlExpressService;

    /**
     * 执行表达式。
     *
     * @param request 表达式执行请求
     * @return 表达式执行结果
     */
    @PostMapping("/execute")
    public ApiResult<Object> execute(@Valid @RequestBody QLExpressExecuteRequest request) {
        log.info("接收QLExpress表达式执行请求，resultType={}", request.getResultType());

        Object result;
        String resultType = StrUtil.blankToDefault(request.getResultType(), "object");

        if (StrUtil.equalsIgnoreCase(resultType, "boolean")) {
            result = qlExpressService.executeAsBoolean(request.getExpression(), request.getParams());
        } else if (StrUtil.equalsIgnoreCase(resultType, "string")) {
            result = qlExpressService.executeAsString(request.getExpression(), request.getParams());
        } else {
            result = qlExpressService.execute(request.getExpression(), request.getParams());
        }

        return ApiResult.success(result);
    }

    /**
     * 快速执行布尔表达式。
     *
     * @param request 表达式执行请求
     * @return 布尔执行结果
     */
    @PostMapping("/execute/boolean")
    public ApiResult<Boolean> executeAsBoolean(@Valid @RequestBody QLExpressExecuteRequest request) {
        Boolean result = qlExpressService.executeAsBoolean(request.getExpression(), request.getParams());
        return ApiResult.success(result);
    }

    /**
     * 快速执行字符串表达式。
     *
     * @param request 表达式执行请求
     * @return 字符串执行结果
     */
    @PostMapping("/execute/string")
    public ApiResult<String> executeAsString(@Valid @RequestBody QLExpressExecuteRequest request) {
        String result = qlExpressService.executeAsString(request.getExpression(), request.getParams());
        return ApiResult.success(result);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/api/qlexpress/execute/boolean" \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "amount >= 100 && userLevel == \"VIP\"",
    "params": {
      "amount": 120,
      "userLevel": "VIP"
    }
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": true
}
```

### 规则校验接口

规则校验接口用于在保存表达式前提前校验语法。该接口只校验表达式是否可以被 QLExpress 解析，不负责校验业务参数是否一定存在。

请求对象可以复用表达式执行请求，也可以单独定义规则校验请求。为了接口语义更清晰，建议单独定义。

文件位置：`src/main/java/io/github/atengk/qlexpress/dto/QLExpressCheckRequest.java`

```java
package io.github.atengk.qlexpress.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * QLExpress 表达式校验请求。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class QLExpressCheckRequest {

    /**
     * 表达式内容。
     */
    @NotBlank(message = "表达式不能为空")
    private String expression;
}
```

在 Controller 中增加规则校验接口。

文件位置：`src/main/java/io/github/atengk/qlexpress/controller/QLExpressController.java`

```java
/**
 * 校验表达式语法。
 *
 * @param request 表达式校验请求
 * @return 是否校验通过
 */
@PostMapping("/check")
public ApiResult<Boolean> check(@Valid @RequestBody QLExpressCheckRequest request) {
    Boolean checked = qlExpressService.checkSyntax(request.getExpression());
    return ApiResult.success(checked);
}
```

注意需要在 Controller 中补充导入：

```java
import io.github.atengk.qlexpress.dto.QLExpressCheckRequest;
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/api/qlexpress/check" \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "amount >= 100 && userLevel == \"VIP\""
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": true
}
```

规则校验接口通常用于规则管理页面的“保存前校验”或“表达式检查”按钮。校验通过只表示表达式语法可解析，不代表实际业务参数一定完整。因此在正式执行时仍然需要进行运行时异常处理。

### 规则测试接口

规则测试接口用于在规则配置页面中模拟执行表达式。它和普通执行接口的区别是：规则测试接口应该返回更详细的信息，例如执行结果、结果类型、是否通过、执行耗时、错误信息等，方便运营、测试或开发人员定位问题。

文件位置：`src/main/java/io/github/atengk/qlexpress/dto/QLExpressTestRequest.java`

```java
package io.github.atengk.qlexpress.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * QLExpress 规则测试请求。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class QLExpressTestRequest {

    /**
     * 规则编码。
     */
    private String ruleCode;

    /**
     * 表达式内容。
     */
    @NotBlank(message = "表达式不能为空")
    private String expression;

    /**
     * 测试参数。
     */
    private Map<String, Object> params = new HashMap<>();
}
```

文件位置：`src/main/java/io/github/atengk/qlexpress/dto/QLExpressTestResult.java`

```java
package io.github.atengk.qlexpress.dto;

import lombok.Builder;
import lombok.Data;

/**
 * QLExpress 规则测试结果。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Builder
public class QLExpressTestResult {

    /**
     * 规则编码。
     */
    private String ruleCode;

    /**
     * 是否执行成功。
     */
    private Boolean success;

    /**
     * 执行结果。
     */
    private Object result;

    /**
     * 结果类型。
     */
    private String resultType;

    /**
     * 执行耗时，单位毫秒。
     */
    private Long costMs;

    /**
     * 错误信息。
     */
    private String errorMessage;
}
```

在 Controller 中增加规则测试接口。该接口会捕获异常并把错误信息返回给调用方，便于页面展示测试失败原因。

文件位置：`src/main/java/io/github/atengk/qlexpress/controller/QLExpressController.java`

```java
/**
 * 测试规则表达式。
 *
 * @param request 规则测试请求
 * @return 规则测试结果
 */
@PostMapping("/test")
public ApiResult<QLExpressTestResult> test(@Valid @RequestBody QLExpressTestRequest request) {
    long startTime = System.currentTimeMillis();

    try {
        Object result = qlExpressService.execute(request.getExpression(), request.getParams());
        long costMs = System.currentTimeMillis() - startTime;

        QLExpressTestResult testResult = QLExpressTestResult.builder()
                .ruleCode(request.getRuleCode())
                .success(true)
                .result(result)
                .resultType(result == null ? "null" : result.getClass().getSimpleName())
                .costMs(costMs)
                .build();

        log.info("QLExpress规则测试成功，ruleCode={}，costMs={}", request.getRuleCode(), costMs);
        return ApiResult.success(testResult);
    } catch (Exception e) {
        long costMs = System.currentTimeMillis() - startTime;

        QLExpressTestResult testResult = QLExpressTestResult.builder()
                .ruleCode(request.getRuleCode())
                .success(false)
                .costMs(costMs)
                .errorMessage(e.getMessage())
                .build();

        log.warn("QLExpress规则测试失败，ruleCode={}，costMs={}，error={}",
                request.getRuleCode(), costMs, e.getMessage());

        return ApiResult.success(testResult);
    }
}
```

注意需要在 Controller 中补充导入：

```java
import io.github.atengk.qlexpress.dto.QLExpressTestRequest;
import io.github.atengk.qlexpress.dto.QLExpressTestResult;
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/api/qlexpress/test" \
  -H "Content-Type: application/json" \
  -d '{
    "ruleCode": "ORDER_DISCOUNT_001",
    "expression": "amount >= 100 && userLevel == \"VIP\"",
    "params": {
      "amount": 120,
      "userLevel": "VIP"
    }
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": {
    "ruleCode": "ORDER_DISCOUNT_001",
    "success": true,
    "result": true,
    "resultType": "Boolean",
    "costMs": 8,
    "errorMessage": null
  }
}
```

规则测试接口适合在规则管理后台使用。生产业务调用建议使用表达式执行接口或内部服务方法，不建议依赖测试接口作为正式业务入口。

## 异常与日志

本节用于说明 QLExpress 集成过程中的异常处理和日志记录方式。表达式引擎属于动态执行能力，运行时可能出现语法错误、变量缺失、类型转换失败、函数调用失败、执行超时等问题，因此必须统一异常边界和日志规范。

### 表达式语法异常

表达式语法异常通常发生在表达式保存、校验或首次执行阶段。常见原因包括括号不匹配、字符串引号缺失、操作符错误、函数名不存在、表达式结构不完整等。

常见错误示例：

| 错误表达式            | 问题说明             |
| --------------------- | -------------------- |
| `amount >=`           | 表达式不完整         |
| `amount > 100 &&`     | 逻辑操作符后缺少条件 |
| `userLevel == "VIP`   | 字符串引号未闭合     |
| `unknownFunc(amount)` | 函数未注册           |
| `(amount >= 100`      | 括号未闭合           |

语法异常建议在两个阶段处理：

| 阶段       | 处理方式                               |
| ---------- | -------------------------------------- |
| 保存规则前 | 调用 `/api/qlexpress/check` 校验表达式 |
| 执行业务前 | 执行服务再次捕获异常，避免异常直接穿透 |

在服务层中，`checkSyntax` 方法可以用于提前校验表达式：

```java
/**
 * 校验表达式语法是否正确。
 *
 * @param expression 表达式内容
 * @return 是否校验通过
 */
@Override
public Boolean checkSyntax(String expression) {
    validateExpression(expression);

    try {
        expressRunner.parseInstructionSet(expression);
        return true;
    } catch (Exception e) {
        log.warn("QLExpress表达式语法校验失败，表达式摘要={}", DigestUtil.md5Hex(expression), e);
        return false;
    }
}
```

对于规则管理后台，建议保存规则时必须先通过语法校验。校验失败时，应返回明确的错误提示，不允许保存为启用状态。

### 执行时异常处理

执行时异常通常发生在表达式语法可以解析，但实际执行时上下文参数、变量类型或函数调用不满足要求的场景。

常见执行异常包括：

| 异常类型     | 示例                                         | 处理建议                 |
| ------------ | -------------------------------------------- | ------------------------ |
| 变量缺失     | 表达式使用 `amount`，参数未传入              | 提示变量不存在或参数缺失 |
| 类型不匹配   | `amount > 100`，但 `amount` 是字符串 `"abc"` | 提示参数类型不正确       |
| 函数调用异常 | `calcDiscountAmount(amount, rate)` 参数为空  | 函数内部兜底或抛业务异常 |
| 结果转换失败 | 表达式返回字符串，却按 `Boolean` 转换        | 提示结果类型不匹配       |
| 执行超时     | 表达式复杂或存在循环                         | 终止执行并记录告警日志   |

建议通过全局异常处理器统一转换异常响应，避免 Controller 中到处写 `try-catch`。

文件位置：`src/main/java/io/github/atengk/qlexpress/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.qlexpress.exception;

import cn.hutool.core.exceptions.ExceptionUtil;
import io.github.atengk.qlexpress.dto.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 QLExpress 执行异常。
     *
     * @param e QLExpress 执行异常
     * @return 统一失败响应
     */
    @ExceptionHandler(QLExpressExecuteException.class)
    public ApiResult<Void> handleQLExpressExecuteException(QLExpressExecuteException e) {
        log.warn("QLExpress表达式处理失败，error={}", e.getMessage());
        return ApiResult.fail("QLEXPRESS_EXECUTE_ERROR", e.getMessage());
    }

    /**
     * 处理请求体参数校验异常。
     *
     * @param e 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .orElse("请求参数校验失败");

        log.warn("接口参数校验失败，error={}", message);
        return ApiResult.fail("PARAM_VALID_ERROR", message);
    }

    /**
     * 处理普通参数校验异常。
     *
     * @param e 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("接口参数约束校验失败，error={}", e.getMessage());
        return ApiResult.fail("PARAM_VALID_ERROR", e.getMessage());
    }

    /**
     * 处理未知异常。
     *
     * @param e 未知异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常，rootCause={}", ExceptionUtil.getRootCauseMessage(e), e);
        return ApiResult.fail("SYSTEM_ERROR", "系统异常，请联系管理员");
    }
}
```

执行时异常处理建议：

| 建议                   | 说明                                        |
| ---------------------- | ------------------------------------------- |
| 不直接返回底层堆栈     | 接口只返回简短错误信息，详细堆栈写入日志    |
| 区分语法错误和执行错误 | 便于前端提示用户修正表达式或参数            |
| 记录规则编码           | 正式规则执行时必须记录 `ruleCode` 或规则 ID |
| 记录表达式摘要         | 避免日志泄露完整表达式和敏感参数            |
| 参数需要脱敏           | 手机号、身份证号、银行卡号等不能完整打印    |
| 测试接口可返回更多错误 | 方便规则调试，但仍不应返回完整堆栈          |

### 执行日志记录

QLExpress 执行日志主要用于排查规则命中情况、定位表达式异常、统计执行耗时和审计规则变更影响。日志应记录关键上下文，但不能无控制地打印完整参数。

推荐记录字段如下：

| 字段               | 说明                                      |
| ------------------ | ----------------------------------------- |
| `ruleCode`         | 规则编码，没有规则编码时可为空            |
| `expressionDigest` | 表达式 MD5 摘要                           |
| `paramKeys`        | 参数 Key 列表                             |
| `resultType`       | 返回结果类型                              |
| `success`          | 是否执行成功                              |
| `costMs`           | 执行耗时                                  |
| `errorMessage`     | 失败原因                                  |
| `traceId`          | 链路追踪 ID，如项目已有链路追踪体系可接入 |

可以在 `QLExpressServiceImpl` 中抽取日志方法，统一记录成功和失败日志。

文件位置：`src/main/java/io/github/atengk/qlexpress/service/impl/QLExpressServiceImpl.java`

```java
/**
 * 记录表达式执行成功日志。
 *
 * @param expression 表达式内容
 * @param params 上下文参数
 * @param result 执行结果
 * @param costMs 执行耗时
 */
private void logExecuteSuccess(String expression, Map<String, Object> params, Object result, long costMs) {
    if (!BooleanUtil.isTrue(qlExpressProperties.getLogEnabled())) {
        return;
    }

    log.info("QLExpress表达式执行成功，表达式摘要={}，参数Keys={}，结果类型={}，耗时={}ms",
            DigestUtil.md5Hex(expression),
            MapUtil.isEmpty(params) ? "[]" : params.keySet(),
            ObjectUtil.isNull(result) ? "null" : result.getClass().getSimpleName(),
            costMs);
}

/**
 * 记录表达式执行失败日志。
 *
 * @param expression 表达式内容
 * @param params 上下文参数
 * @param costMs 执行耗时
 * @param e 异常信息
 */
private void logExecuteError(String expression, Map<String, Object> params, long costMs, Exception e) {
    log.error("QLExpress表达式执行失败，表达式摘要={}，参数Keys={}，耗时={}ms，错误={}",
            DigestUtil.md5Hex(expression),
            MapUtil.isEmpty(params) ? "[]" : params.keySet(),
            costMs,
            e.getMessage(),
            e);
}
```

然后在 `execute` 方法中调用：

```java
long startTime = System.currentTimeMillis();

try {
    Object result = expressRunner.execute(
            expression,
            context,
            errorList,
            BooleanUtil.isTrue(qlExpressProperties.getCache()),
            false,
            getTimeoutMillis()
    );

    long costMs = System.currentTimeMillis() - startTime;
    logExecuteSuccess(expression, safeParams, result, costMs);

    return result;
} catch (Exception e) {
    long costMs = System.currentTimeMillis() - startTime;
    logExecuteError(expression, safeParams, costMs, e);
    throw new QLExpressExecuteException("表达式执行失败：" + e.getMessage(), e);
}
```

日志记录需要避免以下问题：

| 问题                     | 风险                                   |
| ------------------------ | -------------------------------------- |
| 打印完整表达式           | 表达式中可能包含敏感业务规则           |
| 打印完整参数             | 参数中可能包含用户隐私数据             |
| 每个函数内部频繁打印日志 | 高频规则执行时会造成日志膨胀           |
| 异常被吞掉               | 规则失败但业务无感知，容易产生错误结果 |
| 不记录耗时               | 无法发现慢表达式和异常规则             |

推荐的日志策略是：正式业务执行记录摘要日志，规则测试接口记录更详细的调试信息，异常日志记录完整堆栈但不暴露给前端。这样可以兼顾可观测性、安全性和性能。



## 测试验证

本节用于说明 QLExpress 集成完成后的验证方式。测试验证应覆盖服务层单元测试、接口层调用测试和常见表达式执行结果验证，确保表达式执行、参数传递、结果转换、异常处理和自定义函数都符合预期。

建议至少覆盖以下内容：

| 测试类型       | 测试重点                                                   |
| -------------- | ---------------------------------------------------------- |
| 单元测试       | 验证 `QLExpressService` 的表达式执行、结果转换、自定义函数 |
| 接口测试       | 验证 `/execute`、`/check`、`/test` 接口是否可用            |
| 常见表达式验证 | 验证布尔判断、金额计算、三元表达式、函数调用、异常表达式   |

### 单元测试

单元测试主要验证服务层能力，不依赖前端页面，也不通过真实 HTTP 调用。推荐优先测试 `QLExpressService`，确保表达式执行能力稳定后，再测试 Controller 接口。

文件位置：`src/test/java/io/github/atengk/qlexpress/service/QLExpressServiceTest.java`

以下测试类用于验证布尔表达式、字符串结果、数值计算、自定义函数和语法校验。

```java
package io.github.atengk.qlexpress.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QLExpress 表达式执行服务测试。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@SpringBootTest
class QLExpressServiceTest {

    @Autowired
    private QLExpressService qlExpressService;

    /**
     * 测试布尔表达式执行。
     */
    @Test
    void testExecuteAsBoolean() {
        String expression = "amount >= 100 && userLevel == \"VIP\"";

        Map<String, Object> params = Map.of(
                "amount", 120,
                "userLevel", "VIP"
        );

        Boolean result = qlExpressService.executeAsBoolean(expression, params);

        log.info("布尔表达式执行结果：{}", result);
        assertThat(result).isTrue();
    }

    /**
     * 测试字符串分支表达式。
     */
    @Test
    void testExecuteAsString() {
        String expression = "riskScore >= 80 ? \"REJECT\" : \"PASS\"";

        Map<String, Object> params = Map.of(
                "riskScore", 90
        );

        String result = qlExpressService.executeAsString(expression, params);

        log.info("字符串表达式执行结果：{}", result);
        assertThat(result).isEqualTo("REJECT");
    }

    /**
     * 测试数值表达式执行。
     */
    @Test
    void testExecuteAsBigDecimal() {
        String expression = "amount * discountRate";

        Map<String, Object> params = Map.of(
                "amount", new BigDecimal("200"),
                "discountRate", new BigDecimal("0.8")
        );

        BigDecimal result = qlExpressService.execute(expression, params, BigDecimal.class);

        log.info("金额计算表达式执行结果：{}", result);
        assertThat(result).isEqualByComparingTo(new BigDecimal("160.0"));
    }

    /**
     * 测试 Java 服务函数调用。
     */
    @Test
    void testServiceFunction() {
        String expression = "hasText(mobile)";

        Map<String, Object> params = Map.of(
                "mobile", "13800138000"
        );

        Boolean result = qlExpressService.executeAsBoolean(expression, params);

        log.info("Java服务函数表达式执行结果：{}", result);
        assertThat(result).isTrue();
    }

    /**
     * 测试自定义函数调用。
     */
    @Test
    void testCustomFunction() {
        String expression = "matchUserLevel(userLevel, \"VIP\")";

        Map<String, Object> params = Map.of(
                "userLevel", "VIP"
        );

        Boolean result = qlExpressService.executeAsBoolean(expression, params);

        log.info("自定义函数表达式执行结果：{}", result);
        assertThat(result).isTrue();
    }

    /**
     * 测试表达式语法校验。
     */
    @Test
    void testCheckSyntax() {
        Boolean validResult = qlExpressService.checkSyntax("amount >= 100");
        Boolean invalidResult = qlExpressService.checkSyntax("amount >=");

        log.info("表达式语法校验结果，validResult={}，invalidResult={}", validResult, invalidResult);

        assertThat(validResult).isTrue();
        assertThat(invalidResult).isFalse();
    }
}
```

执行测试命令：

```bash
mvn test -Dtest=QLExpressServiceTest
```

命令说明：

| 命令                          | 说明                                              |
| ----------------------------- | ------------------------------------------------- |
| `mvn test`                    | 执行 Maven 测试阶段                               |
| `-Dtest=QLExpressServiceTest` | 只执行指定测试类，便于快速验证 QLExpress 服务能力 |

如果测试启动失败，应优先检查以下内容：

| 检查项                       | 说明                                                         |
| ---------------------------- | ------------------------------------------------------------ |
| QLExpress 依赖是否正确       | `com.alibaba:QLExpress:3.3.4` 是否已成功下载                 |
| `ExpressRunner` 是否注册成功 | 查看启动日志中是否有初始化日志                               |
| 自定义函数是否注册成功       | 检查 `QLExpressConfig` 中的 `addFunction` 和 `addFunctionOfServiceMethod` |
| 配置类是否绑定成功           | 检查 `@EnableConfigurationProperties(QLExpressProperties.class)` 是否存在 |
| 表达式字符串是否正确转义     | Java 字符串中的双引号需要使用 `\"`                           |

### 接口测试

接口测试用于验证 Controller 层是否正常接收请求、校验参数并返回统一响应。接口测试可以使用 `MockMvc` 编写自动化测试，也可以使用 `curl` 或 Postman 手动调用。

文件位置：`src/test/java/io/github/atengk/qlexpress/controller/QLExpressControllerTest.java`

以下测试类用于验证表达式执行接口、规则校验接口和规则测试接口。

```java
package io.github.atengk.qlexpress.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QLExpress 表达式接口测试。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class QLExpressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试布尔表达式执行接口。
     *
     * @throws Exception 接口调用异常
     */
    @Test
    void testExecuteAsBoolean() throws Exception {
        String body = """
                {
                  "expression": "amount >= 100 && userLevel == \\"VIP\\"",
                  "params": {
                    "amount": 120,
                    "userLevel": "VIP"
                  }
                }
                """;

        mockMvc.perform(post("/api/qlexpress/execute/boolean")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(true)));

        log.info("布尔表达式执行接口测试通过");
    }

    /**
     * 测试表达式语法校验接口。
     *
     * @throws Exception 接口调用异常
     */
    @Test
    void testCheck() throws Exception {
        String body = """
                {
                  "expression": "amount >= 100 && userLevel == \\"VIP\\""
                }
                """;

        mockMvc.perform(post("/api/qlexpress/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(true)));

        log.info("表达式语法校验接口测试通过");
    }

    /**
     * 测试规则测试接口。
     *
     * @throws Exception 接口调用异常
     */
    @Test
    void testRuleTest() throws Exception {
        String body = """
                {
                  "ruleCode": "ORDER_DISCOUNT_001",
                  "expression": "amount >= 100 && userLevel == \\"VIP\\"",
                  "params": {
                    "amount": 120,
                    "userLevel": "VIP"
                  }
                }
                """;

        mockMvc.perform(post("/api/qlexpress/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.success", is(true)))
                .andExpect(jsonPath("$.data.result", is(true)));

        log.info("规则测试接口测试通过");
    }

    /**
     * 测试表达式为空时的参数校验。
     *
     * @throws Exception 接口调用异常
     */
    @Test
    void testEmptyExpression() throws Exception {
        String body = """
                {
                  "expression": "",
                  "params": {
                    "amount": 120
                  }
                }
                """;

        mockMvc.perform(post("/api/qlexpress/execute/boolean")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("PARAM_VALID_ERROR")));

        log.info("表达式为空参数校验接口测试通过");
    }
}
```

执行接口测试命令：

```bash
mvn test -Dtest=QLExpressControllerTest
```

也可以启动项目后使用 `curl` 手动验证。

表达式执行接口：

```bash
curl -X POST "http://localhost:8080/api/qlexpress/execute/boolean" \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "amount >= 100 && userLevel == \"VIP\"",
    "params": {
      "amount": 120,
      "userLevel": "VIP"
    }
  }'
```

规则校验接口：

```bash
curl -X POST "http://localhost:8080/api/qlexpress/check" \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "amount >= 100 && userLevel == \"VIP\""
  }'
```

规则测试接口：

```bash
curl -X POST "http://localhost:8080/api/qlexpress/test" \
  -H "Content-Type: application/json" \
  -d '{
    "ruleCode": "ORDER_DISCOUNT_001",
    "expression": "amount >= 100 && userLevel == \"VIP\"",
    "params": {
      "amount": 120,
      "userLevel": "VIP"
    }
  }'
```

### 常见表达式验证

常见表达式验证用于覆盖业务中高频出现的规则类型。建议将这些表达式整理为测试用例或规则平台内置模板，便于开发、测试和运营统一理解表达式写法。

常见表达式示例：

| 场景         | 表达式                                | 参数示例                            | 预期结果     |
| ------------ | ------------------------------------- | ----------------------------------- | ------------ |
| 金额门槛判断 | `amount >= 100`                       | `{"amount":120}`                    | `true`       |
| 会员等级判断 | `userLevel == "VIP"`                  | `{"userLevel":"VIP"}`               | `true`       |
| 多条件判断   | `amount >= 100 && userLevel == "VIP"` | `{"amount":120,"userLevel":"VIP"}`  | `true`       |
| 风控分支     | `riskScore >= 80 ? "REJECT" : "PASS"` | `{"riskScore":90}`                  | `"REJECT"`   |
| 折扣计算     | `amount * discountRate`               | `{"amount":200,"discountRate":0.8}` | `160`        |
| 字符串非空   | `hasText(mobile)`                     | `{"mobile":"13800138000"}`          | `true`       |
| 用户等级匹配 | `matchUserLevel(userLevel, "VIP")`    | `{"userLevel":"VIP"}`               | `true`       |
| 订单状态判断 | `orderStatus == "PAID"`               | `{"orderStatus":"PAID"}`            | `true`       |
| 商品数量判断 | `goodsCount > 0`                      | `{"goodsCount":2}`                  | `true`       |
| 条件分支返回 | `amount >= 100 ? "DISCOUNT" : "NONE"` | `{"amount":120}`                    | `"DISCOUNT"` |

可以使用参数化测试统一验证这些表达式。

文件位置：`src/test/java/io/github/atengk/qlexpress/service/QLExpressCommonExpressionTest.java`

以下测试类用于批量验证常见表达式模板，适合后续扩展为规则模板回归测试。

```java
package io.github.atengk.qlexpress.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QLExpress 常见表达式验证测试。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@SpringBootTest
class QLExpressCommonExpressionTest {

    @Autowired
    private QLExpressService qlExpressService;

    /**
     * 批量验证常见表达式。
     *
     * @param caseItem 测试用例
     */
    @ParameterizedTest
    @MethodSource("expressionCases")
    void testCommonExpression(ExpressionCase caseItem) {
        Object result = qlExpressService.execute(caseItem.getExpression(), caseItem.getParams());

        log.info("常见表达式验证完成，caseName={}，result={}", caseItem.getName(), result);
        assertThat(String.valueOf(result)).isEqualTo(String.valueOf(caseItem.getExpected()));
    }

    /**
     * 构造表达式测试用例。
     *
     * @return 表达式测试用例流
     */
    static Stream<ExpressionCase> expressionCases() {
        return Stream.of(
                new ExpressionCase(
                        "金额门槛判断",
                        "amount >= 100",
                        Map.of("amount", 120),
                        true
                ),
                new ExpressionCase(
                        "会员等级判断",
                        "userLevel == \"VIP\"",
                        Map.of("userLevel", "VIP"),
                        true
                ),
                new ExpressionCase(
                        "风控分支判断",
                        "riskScore >= 80 ? \"REJECT\" : \"PASS\"",
                        Map.of("riskScore", 90),
                        "REJECT"
                ),
                new ExpressionCase(
                        "折扣金额计算",
                        "amount * discountRate",
                        Map.of(
                                "amount", new BigDecimal("200"),
                                "discountRate", new BigDecimal("0.8")
                        ),
                        new BigDecimal("160.0")
                ),
                new ExpressionCase(
                        "字符串非空判断",
                        "hasText(mobile)",
                        Map.of("mobile", "13800138000"),
                        true
                ),
                new ExpressionCase(
                        "用户等级匹配",
                        "matchUserLevel(userLevel, \"VIP\")",
                        Map.of("userLevel", "VIP"),
                        true
                )
        );
    }

    /**
     * 表达式测试用例。
     *
     * @author Ateng
     * @since 2026-05-08
     */
    @Getter
    @AllArgsConstructor
    static class ExpressionCase {

        /**
         * 用例名称。
         */
        private final String name;

        /**
         * 表达式内容。
         */
        private final String expression;

        /**
         * 表达式参数。
         */
        private final Map<String, Object> params;

        /**
         * 预期结果。
         */
        private final Object expected;
    }
}
```

执行常见表达式测试：

```bash
mvn test -Dtest=QLExpressCommonExpressionTest
```

常见表达式验证应作为规则平台的基础回归用例。每次新增自定义函数、调整执行器配置或升级 QLExpress 版本后，都建议执行一次完整回归。

## 部署与使用说明

本节用于说明 Spring Boot 集成 QLExpress 后的部署检查、接口调用示例和生产使用注意事项。QLExpress 属于动态执行能力，部署前需要重点检查配置、安全边界、日志策略和测试覆盖情况。

### 配置检查

部署前应先检查 Maven 依赖、配置文件、执行器初始化、自定义函数注册和接口访问权限。不要只确认项目能够启动，还需要确认表达式能够正常执行、异常能够被统一处理、日志不会泄露敏感信息。

部署前检查清单：

| 检查项           | 要求                                                   |
| ---------------- | ------------------------------------------------------ |
| JDK 版本         | 使用 JDK 17 或项目统一指定版本                         |
| Spring Boot 版本 | 使用 Spring Boot 3.x                                   |
| QLExpress 依赖   | 使用 `com.alibaba:QLExpress:3.3.4`                     |
| 配置项           | `qlexpress.enabled`、`cache`、`timeout-ms` 等配置完整  |
| 执行器 Bean      | `ExpressRunner` 能够正常注册                           |
| 服务封装         | 业务代码通过 `QLExpressService` 调用，不直接操作执行器 |
| 自定义函数       | 已在 `QLExpressConfig` 中集中注册                      |
| 异常处理         | 已接入 `GlobalExceptionHandler`                        |
| 日志策略         | 不打印完整敏感参数                                     |
| 接口权限         | 表达式执行接口不直接暴露公网                           |
| 单元测试         | 核心表达式执行测试通过                                 |
| 接口测试         | `/execute`、`/check`、`/test` 测试通过                 |

生产环境推荐配置：

文件位置：`src/main/resources/application-prod.yml`

```yaml
qlexpress:
  # 生产环境启用表达式执行功能
  enabled: true

  # 生产环境建议开启缓存，降低重复解析成本
  cache: true

  # 设置表达式执行超时时间，避免异常表达式占用线程
  timeout-ms: 3000

  # 生产环境建议开启摘要日志，便于排查问题
  log-enabled: true

  # 默认不允许表达式直接调用任意 Java 方法，只允许注册过的函数
  allow-java-method: false

  # 生产环境建议开启严格模式，尽早发现变量缺失问题
  strict-mode: true
```

启动前可以执行以下命令完成构建和测试：

```bash
mvn clean test
mvn clean package -DskipTests
```

命令说明：

| 命令                            | 说明                                           |
| ------------------------------- | ---------------------------------------------- |
| `mvn clean test`                | 清理并执行测试，确认服务层和接口层测试通过     |
| `mvn clean package -DskipTests` | 打包应用，跳过测试适合测试已单独执行通过的场景 |

启动项目：

```bash
java -jar target/springboot-qlexpress-demo.jar --spring.profiles.active=prod
```

启动后查看日志中是否包含类似内容：

```text
初始化 QLExpress 执行器完成，已注册基础业务函数
```

如果没有出现初始化日志，需要检查 `qlexpress.enabled` 是否为 `true`，以及 `QLExpressConfig` 是否被 Spring Boot 扫描到。

### 调用示例

部署完成后，可以通过接口验证表达式执行、规则校验和规则测试能力。以下示例默认服务端口为 `8080`。

表达式执行示例：

```bash
curl -X POST "http://localhost:8080/api/qlexpress/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "amount >= 100 && userLevel == \"VIP\"",
    "resultType": "boolean",
    "params": {
      "amount": 120,
      "userLevel": "VIP"
    }
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": true
}
```

金额计算示例：

```bash
curl -X POST "http://localhost:8080/api/qlexpress/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "calcDiscountAmount(amount, discountRate)",
    "resultType": "string",
    "params": {
      "amount": 200,
      "discountRate": 0.8
    }
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": "160.0"
}
```

表达式校验示例：

```bash
curl -X POST "http://localhost:8080/api/qlexpress/check" \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "amount >= 100 && userLevel == \"VIP\""
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": true
}
```

规则测试示例：

```bash
curl -X POST "http://localhost:8080/api/qlexpress/test" \
  -H "Content-Type: application/json" \
  -d '{
    "ruleCode": "ORDER_DISCOUNT_001",
    "expression": "amount >= 100 && userLevel == \"VIP\"",
    "params": {
      "amount": 120,
      "userLevel": "VIP"
    }
  }'
```

响应示例：

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": {
    "ruleCode": "ORDER_DISCOUNT_001",
    "success": true,
    "result": true,
    "resultType": "Boolean",
    "costMs": 5,
    "errorMessage": null
  }
}
```

异常调用示例：

```bash
curl -X POST "http://localhost:8080/api/qlexpress/execute/boolean" \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "amount >=",
    "params": {
      "amount": 120
    }
  }'
```

响应示例：

```json
{
  "success": false,
  "code": "QLEXPRESS_EXECUTE_ERROR",
  "message": "表达式执行失败：表达式语法错误",
  "data": null
}
```

实际错误消息可能会因 QLExpress 版本和底层异常内容不同而略有差异，接口层只需要保持统一错误编码和清晰提示即可。

### 注意事项

QLExpress 引入后，系统具备了动态执行表达式的能力，因此生产环境必须控制表达式来源、函数开放范围、执行超时和日志输出。

主要注意事项如下：

| 类型     | 注意事项                                                |
| -------- | ------------------------------------------------------- |
| 安全控制 | 不要将表达式执行接口直接开放公网                        |
| 函数开放 | 只注册经过审核的 Java 方法和自定义函数                  |
| 参数控制 | 不要把完整用户对象、请求对象、Spring 容器对象传入上下文 |
| 日志控制 | 不要在日志中打印完整敏感参数                            |
| 超时控制 | 必须配置 `timeout-ms`，避免异常表达式长时间执行         |
| 规则审核 | 生产规则启用前应经过语法校验、测试执行和人工审核        |
| 版本管理 | 表达式应记录规则编码、版本号、创建人、修改人和启停状态  |
| 灰度发布 | 重要规则建议支持灰度、回滚和禁用                        |
| 性能压测 | 高频规则需要压测执行耗时和并发表现                      |
| 结果兜底 | 表达式执行失败时，业务应有明确兜底策略                  |

业务规则配置建议至少包含以下字段：

| 字段          | 说明               |
| ------------- | ------------------ |
| `ruleCode`    | 规则编码，全局唯一 |
| `ruleName`    | 规则名称           |
| `expression`  | 表达式内容         |
| `version`     | 规则版本           |
| `enabled`     | 是否启用           |
| `description` | 规则说明           |
| `createdBy`   | 创建人             |
| `updatedBy`   | 修改人             |
| `createdTime` | 创建时间           |
| `updatedTime` | 修改时间           |

不建议在表达式中承载过多业务逻辑。表达式适合处理判断、计算和分支，不适合替代完整业务流程。推荐做法是：Java 代码控制主流程，QLExpress 只负责规则判断或结果计算。

生产使用时建议遵循以下规则：

1. 表达式保存前必须调用语法校验。
2. 表达式启用前必须使用测试参数验证。
3. 正式执行时必须设置超时时间。
4. 业务参数必须白名单化，不传无关对象。
5. 自定义函数必须无副作用，不写库、不发消息、不调用外部接口。
6. 执行日志记录规则编码、表达式摘要、参数 Key、耗时和结果类型。
7. 异常响应不返回完整堆栈，完整异常只写入服务端日志。
8. 高频场景必须开启表达式缓存并进行压测。
9. 重要规则需要具备版本管理和快速回滚能力。
10. 规则平台应限制只有授权人员可以新增、修改、启用和停用规则。

完成以上测试和部署检查后，Spring Boot 3 集成 QLExpress 的基础能力已经闭环。后续可以在此基础上继续扩展规则持久化、规则分组、规则版本管理、规则灰度发布、执行审计和后台管理页面。
