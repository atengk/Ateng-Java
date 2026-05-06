# Spring Boot AOP

Spring Boot AOP 用于在 Spring Boot 项目中实现面向切面编程，常见用途包括接口日志、操作审计、权限校验、异常记录和接口耗时统计。AOP 主要用于抽离多个业务模块中重复出现的横切逻辑，使业务代码保持清晰，通用逻辑集中维护。

## AOP 基础概述

本节主要说明 AOP 的基本作用、核心概念以及 Spring AOP 的适用范围。理解这些内容后，再编写切点表达式、切面类和通知方法时，可以更准确地判断哪些方法应该被增强、增强逻辑应该放在哪里，以及哪些场景不适合使用 Spring AOP。

### AOP 的作用

AOP，全称 Aspect Oriented Programming，即面向切面编程。它是对面向对象编程的一种补充，用于将分散在多个业务模块中的通用逻辑抽取出来，统一放到切面中处理。

在实际项目中，业务方法通常应该只关注核心业务。例如订单创建方法主要处理订单参数校验、库存扣减、订单保存等逻辑，不应该混入大量请求日志、操作审计、权限判断和耗时统计代码。如果这些通用逻辑散落在每个业务方法中，会导致代码重复、维护困难，并且容易出现日志格式不统一、审计字段缺失等问题。

使用 AOP 后，可以将这类通用逻辑集中到切面中。例如在接口执行前记录请求参数，在接口执行后记录返回结果，在接口异常时记录异常信息，在接口执行完成后统计耗时。Spring 官方文档也将事务管理这类跨多个类型和对象的逻辑称为典型的横切关注点。([Home](https://docs.spring.io/spring-framework/reference/core/aop.html?utm_source=chatgpt.com))

AOP 在 Spring Boot 项目中的主要作用如下：

| 作用         | 说明                                                 |
| ------------ | ---------------------------------------------------- |
| 减少重复代码 | 将日志、审计、权限、耗时统计等重复逻辑统一封装       |
| 保持业务清晰 | 业务方法只关注核心业务流程，减少非业务逻辑干扰       |
| 统一系统行为 | 对指定包、指定类、指定注解或指定方法统一增强         |
| 便于后期维护 | 通用规则集中在切面中，修改日志格式或审计规则更加方便 |
| 提升扩展能力 | 新增横切能力时，尽量减少对原业务代码的侵入           |

### 核心概念

AOP 的概念较多，但在 Spring Boot AOP 开发中，重点掌握切面、连接点、切点、通知、目标对象、代理对象和织入即可。Spring AOP 中的连接点始终表示方法执行，这也是 Spring AOP 和完整 AspectJ 的重要区别之一。([Home](https://docs.spring.io/spring-framework/reference/core/aop/introduction-defn.html?utm_source=chatgpt.com))

| 概念     | 英文          | 说明                                                         |
| -------- | ------------- | ------------------------------------------------------------ |
| 切面     | Aspect        | 横切逻辑的模块化封装，通常是一个使用 `@Aspect` 标注的 Spring Bean |
| 连接点   | Join Point    | 程序执行过程中的某个点，在 Spring AOP 中表示方法执行         |
| 切点     | Pointcut      | 用于匹配连接点的规则，决定哪些方法需要被增强                 |
| 通知     | Advice        | 在切点匹配的方法上执行的增强逻辑                             |
| 目标对象 | Target Object | 被增强的原始业务对象                                         |
| 代理对象 | AOP Proxy     | Spring 创建的代理对象，方法调用通过代理对象完成增强          |
| 织入     | Weaving       | 将切面逻辑应用到目标对象的过程，Spring AOP 在运行时完成织入  |

Spring AOP 常用通知类型如下：

| 通知类型 | 注解              | 执行时机                               |
| -------- | ----------------- | -------------------------------------- |
| 前置通知 | `@Before`         | 目标方法执行前                         |
| 后置通知 | `@After`          | 目标方法结束后，无论成功或异常都会执行 |
| 返回通知 | `@AfterReturning` | 目标方法正常返回后                     |
| 异常通知 | `@AfterThrowing`  | 目标方法抛出异常后                     |
| 环绕通知 | `@Around`         | 包围整个目标方法执行过程               |

实际开发中，应优先选择刚好满足需求的通知类型。如果只是记录方法正常返回结果，使用 `@AfterReturning` 即可；如果只是记录异常信息，使用 `@AfterThrowing` 即可；如果需要控制目标方法是否继续执行、修改返回值、统计完整耗时，才优先使用 `@Around`。Spring 官方也建议使用能够满足需求的最小通知类型，以降低使用复杂度和出错概率。([Home](https://docs.spring.io/spring-framework/reference/core/aop/introduction-defn.html?utm_source=chatgpt.com))

### Spring AOP 的适用场景

Spring AOP 适合处理 Spring 容器管理的 Bean 方法增强，尤其适合对 Controller、Service、Component 等 Spring Bean 进行统一拦截。Spring Boot 会为 AOP 提供自动配置，默认使用 CGLIB 代理；如果需要使用 JDK 动态代理，可以通过 `spring.aop.proxy-target-class=false` 调整。([Home](https://docs.spring.io/spring-boot/reference/features/aop.html?utm_source=chatgpt.com))

Spring AOP 常见适用场景如下：

| 场景         | 说明                                                     |
| ------------ | -------------------------------------------------------- |
| 接口日志记录 | 统一记录请求路径、请求方式、请求参数、响应结果和执行耗时 |
| 操作审计     | 记录用户新增、修改、删除、导出等关键操作                 |
| 权限校验     | 对指定注解或指定接口执行权限判断                         |
| 参数校验     | 在进入业务方法前做统一业务参数检查                       |
| 耗时统计     | 统计接口或 Service 方法执行耗时                          |
| 异常记录     | 在方法抛出异常时统一记录上下文信息                       |
| 幂等控制     | 对重复提交、重复请求等场景进行统一拦截                   |

Spring AOP 不适合以下场景：

| 场景                    | 原因                                                |
| ----------------------- | --------------------------------------------------- |
| 拦截非 Spring Bean 对象 | Spring AOP 只对 Spring 容器管理的 Bean 生效         |
| 拦截字段读写            | Spring AOP 只支持方法执行连接点                     |
| 拦截构造方法            | 构造方法不属于 Spring AOP 的常规增强范围            |
| 同类内部方法调用        | `this.xxx()` 调用不会经过代理对象，切面通常不会生效 |
| 极细粒度增强            | 字段、构造器、对象创建等场景更适合使用完整 AspectJ  |
| final 方法增强          | 基于代理机制时，final 方法无法被正常覆盖增强        |

需要特别注意的是，Spring AOP 是基于代理的机制。目标对象内部方法调用不会经过代理对象，因此同一个类中的方法相互调用时，切面通常不会生效。Spring 官方文档也明确说明，由于 Spring AOP 的代理特性，目标对象内部调用不会被拦截。([Home](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/pointcuts.html?utm_source=chatgpt.com))

## 环境准备

本节用于准备 Spring Boot 3 AOP 示例项目的基础环境，包括版本要求、Maven 依赖和项目结构。后续自定义注解、切面类、接口日志和功能测试都基于本节配置展开。

### Spring Boot 版本说明

Spring Boot 3 要求使用 Java 17 或更高版本。以 Spring Boot 3.2.x 官方文档为例，Spring Boot 3.2.12 要求 Java 17，并支持 Maven 3.6.3 或更高版本；Spring Boot 3.5 的安装文档同样说明需要 Java 17 或更高版本，并支持 Maven 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/docs/3.2.12/reference/htmlsingle/?utm_source=chatgpt.com))

建议开发环境如下：

| 环境        | 建议版本             |
| ----------- | -------------------- |
| JDK         | Java 17 或 Java 21   |
| Spring Boot | 3.x 稳定版本         |
| Maven       | 3.6.3 或以上         |
| IDE         | IntelliJ IDEA 2023+  |
| 构建方式    | Maven                |
| 项目类型    | Spring Boot Web 项目 |

开发前可以先检查本地 Java 和 Maven 版本：

```bash
java -version
mvn -version
```

如果 `java -version` 输出低于 17，需要先升级 JDK。生产项目中建议统一开发、测试和生产环境的 JDK 主版本，例如统一使用 Java 17 或 Java 21，避免本地正常、服务器异常的问题。

### Maven 依赖配置

Spring Boot AOP 开发需要引入 `spring-boot-starter-aop`。该依赖会提供 Spring AOP 和 AspectJ 注解风格支持。Spring Boot 官方文档说明，当 AspectJ 在 classpath 中时，Spring Boot 会自动启用 AspectJ 自动代理，因此通常不需要额外添加 `@EnableAspectJAutoProxy`。([Home](https://docs.spring.io/spring-boot/reference/features/aop.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

下面配置用于创建一个 Spring Boot 3 AOP 示例项目，包含 Web、AOP、Hutool、Lombok 和测试依赖。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- 使用 Spring Boot 父工程统一管理依赖版本和插件版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.12</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-boot-aop-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-boot-aop-demo</name>
    <description>Spring Boot 3 AOP 开发示例</description>

    <properties>
        <!-- Spring Boot 3 要求 Java 17 或以上版本 -->
        <java.version>17</java.version>

        <!-- Hutool 工具类库版本 -->
        <hutool.version>5.8.36</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 开发依赖，用于编写 Controller 接口并验证 AOP 拦截效果 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring AOP 依赖，提供 @Aspect、@Pointcut、@Around 等切面能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- Hutool 工具类库，用于字符串、JSON、日期等常用工具处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化实体类、日志对象和构造方法代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖，用于后续接口和切面测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件，用于打包和运行可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <!-- 打包时排除 Lombok，避免运行环境引入无用依赖 -->
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

文件位置：`src/main/resources/application.yml`

下面配置用于声明应用名称、启动端口和 AOP 代理策略。

```yaml
server:
  # 示例项目启动端口
  port: 8080

spring:
  application:
    # 应用名称，便于日志和链路追踪中识别服务
    name: spring-boot-aop-demo

  aop:
    # true 表示使用 CGLIB 代理；false 表示使用 JDK 动态代理
    # Spring Boot 默认使用 CGLIB，常规项目保持默认即可
    proxy-target-class: true
```

一般情况下，`spring.aop.proxy-target-class` 保持默认值即可。只有项目明确要求基于接口代理，并希望使用 JDK 动态代理时，才建议设置为 `false`。

### 项目基础结构

项目结构建议按职责拆分。AOP 相关类统一放在 `aop` 包下，自定义注解统一放在 `annotation` 包下，业务接口和实现类按 Controller、Service、ServiceImpl 分层。这样后续扩展接口日志、操作审计、权限校验和耗时统计时，不会和业务代码混杂在一起。

推荐项目结构如下：

```text
spring-boot-aop-demo
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── io
    │   │       └── github
    │   │           └── atengk
    │   │               └── aopdemo
    │   │                   ├── AopDemoApplication.java
    │   │                   ├── annotation
    │   │                   │   └── OperationLog.java
    │   │                   ├── aop
    │   │                   │   └── OperationLogAspect.java
    │   │                   ├── controller
    │   │                   │   └── OrderController.java
    │   │                   ├── service
    │   │                   │   ├── OrderService.java
    │   │                   │   └── impl
    │   │                   │       └── OrderServiceImpl.java
    │   │                   └── model
    │   │                       ├── dto
    │   │                       │   └── OrderCreateDTO.java
    │   │                       └── vo
    │   │                           └── OrderVO.java
    │   └── resources
    │       └── application.yml
    └── test
        └── java
            └── io
                └── github
                    └── atengk
                        └── aopdemo
                            └── AopDemoApplicationTests.java
```

各目录职责如下：

| 路径           | 说明                                             |
| -------------- | ------------------------------------------------ |
| `annotation`   | 存放自定义注解，例如操作日志注解、权限注解       |
| `aop`          | 存放切面类，例如日志切面、权限切面、耗时统计切面 |
| `controller`   | 存放接口入口，用于触发 AOP 拦截                  |
| `service`      | 存放业务接口                                     |
| `service.impl` | 存放业务实现类                                   |
| `model.dto`    | 存放请求参数对象                                 |
| `model.vo`     | 存放接口响应对象                                 |
| `resources`    | 存放配置文件，例如 `application.yml`             |

后续开发时，建议优先使用自定义注解作为切点入口，例如 `@OperationLog`。这种方式比直接拦截整个包更可控，适合在实际业务项目中逐步落地。对于统一接口耗时统计、全局请求日志等场景，可以再使用 `execution` 表达式按包路径进行拦截。



## AOP 核心注解

本节主要说明 Spring AOP 中常用的核心注解，包括切面声明、切点定义和不同执行时机的通知注解。实际开发中，通常会先使用 `@Aspect` 定义切面类，再使用 `@Pointcut` 定义拦截规则，最后通过 `@Before`、`@After`、`@AfterReturning`、`@AfterThrowing`、`@Around` 编写具体增强逻辑。

### @Aspect

`@Aspect` 用于声明一个切面类。切面类用于集中编写横切逻辑，例如接口日志、权限校验、操作审计和接口耗时统计。仅添加 `@Aspect` 还不够，切面类还需要交给 Spring 容器管理，因此通常会同时添加 `@Component`。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/OperationLogAspect.java`

下面示例定义了一个基础切面类，用于后续扩展日志记录、异常记录和耗时统计。

```java
package io.github.atengk.aopdemo.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

}
```

`@Aspect` 本身只表示这是一个切面类，不负责创建 Bean。`@Component` 才负责将该类注册到 Spring 容器中。实际项目中，如果切面类没有被 Spring 扫描到，即使添加了 `@Aspect`，切面也不会生效。

### @Pointcut

`@Pointcut` 用于定义切点规则。切点决定哪些类、哪些方法会被 AOP 拦截。为了避免多个通知方法重复编写相同的切点表达式，通常会将切点单独提取为一个空方法，然后在其他通知中引用该方法。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/OperationLogAspect.java`

下面示例使用 `@Pointcut` 定义了一个基于自定义注解的切点，表示只拦截标注了 `@OperationLog` 的方法。

```java
package io.github.atengk.aopdemo.aop;

import io.github.atengk.aopdemo.annotation.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 操作日志切点
     */
    @Pointcut("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

}
```

`operationLogPointcut()` 方法本身不需要编写业务代码，它只是一个切点标识。后续通知方法可以通过 `operationLogPointcut()` 复用这个切点规则。

### @Before

`@Before` 表示前置通知，会在目标方法执行之前执行。它适合处理参数记录、权限校验、前置参数检查等逻辑。如果前置通知中抛出异常，目标方法不会继续执行。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/OperationLogAspect.java`

下面示例在目标方法执行前记录方法签名和请求参数。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 操作日志切点
     */
    @Pointcut("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

    /**
     * 方法执行前记录请求参数
     *
     * @param joinPoint 连接点
     */
    @Before("operationLogPointcut()")
    public void before(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("方法开始执行，方法：{}，参数：{}", methodName, JSONUtil.toJsonStr(args));
    }

}
```

`JoinPoint` 可以获取目标方法的签名、参数、目标对象等信息。前置通知无法获取方法返回值，因为此时目标方法还没有执行。

### @After

`@After` 表示后置通知，会在目标方法执行结束后执行。无论目标方法是正常返回还是抛出异常，`@After` 都会执行，因此它适合做统一收尾、资源清理、基础日志标记等操作。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/OperationLogAspect.java`

下面示例在目标方法执行完成后输出结束日志。

```java
package io.github.atengk.aopdemo.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 操作日志切点
     */
    @Pointcut("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

    /**
     * 方法执行完成后记录日志
     *
     * @param joinPoint 连接点
     */
    @After("operationLogPointcut()")
    public void after(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        log.info("方法执行结束，方法：{}", methodName);
    }

}
```

`@After` 不关心目标方法是否执行成功，也不能直接获取返回值或异常对象。如果需要获取返回结果，应使用 `@AfterReturning`；如果需要获取异常信息，应使用 `@AfterThrowing`。

### @AfterReturning

`@AfterReturning` 表示返回通知，会在目标方法正常返回后执行。它可以获取目标方法的返回值，适合记录响应结果、成功审计日志、业务成功后的辅助处理等。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/OperationLogAspect.java`

下面示例在目标方法正常返回后记录返回结果。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 操作日志切点
     */
    @Pointcut("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

    /**
     * 方法正常返回后记录返回值
     *
     * @param joinPoint 连接点
     * @param result    返回结果
     */
    @AfterReturning(pointcut = "operationLogPointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().toShortString();
        log.info("方法执行成功，方法：{}，返回值：{}", methodName, JSONUtil.toJsonStr(result));
    }

}
```

`returning = "result"` 中的 `result` 必须和方法参数名保持一致。只有目标方法正常返回时，该通知才会执行；如果目标方法抛出异常，该通知不会执行。

### @AfterThrowing

`@AfterThrowing` 表示异常通知，会在目标方法抛出异常后执行。它适合记录异常日志、保存异常审计记录、统计异常次数等。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/OperationLogAspect.java`

下面示例在目标方法抛出异常后记录异常类型和异常信息。

```java
package io.github.atengk.aopdemo.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 操作日志切点
     */
    @Pointcut("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

    /**
     * 方法抛出异常后记录异常信息
     *
     * @param joinPoint 连接点
     * @param throwable 异常对象
     */
    @AfterThrowing(pointcut = "operationLogPointcut()", throwing = "throwable")
    public void afterThrowing(JoinPoint joinPoint, Throwable throwable) {
        String methodName = joinPoint.getSignature().toShortString();
        log.error("方法执行异常，方法：{}，异常类型：{}，异常信息：{}",
                methodName,
                throwable.getClass().getName(),
                throwable.getMessage(),
                throwable);
    }

}
```

`throwing = "throwable"` 中的 `throwable` 必须和方法参数名保持一致。异常通知通常只负责记录异常，不建议在这里吞掉异常。异常应该继续交给全局异常处理器或上层调用方处理。

### @Around

`@Around` 表示环绕通知，是功能最强的通知类型。它可以在目标方法执行前后添加逻辑，也可以决定是否执行目标方法，还可以修改返回值或捕获异常。接口耗时统计、统一日志记录、权限控制、限流和幂等控制通常会使用环绕通知。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/OperationLogAspect.java`

下面示例使用 `@Around` 统一记录方法入参、返回值、异常信息和执行耗时。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.core.date.StopWatch;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 操作日志切点
     */
    @Pointcut("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

    /**
     * 环绕通知，记录方法执行过程
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 目标方法异常
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        StopWatch stopWatch = new StopWatch(methodName);
        stopWatch.start();

        try {
            log.info("方法开始执行，方法：{}，参数：{}", methodName, JSONUtil.toJsonStr(args));

            Object result = joinPoint.proceed();

            log.info("方法执行成功，方法：{}，返回值：{}", methodName, JSONUtil.toJsonStr(result));
            return result;
        } catch (Throwable throwable) {
            log.error("方法执行异常，方法：{}，异常信息：{}", methodName, throwable.getMessage(), throwable);
            throw throwable;
        } finally {
            stopWatch.stop();
            log.info("方法执行完成，方法：{}，耗时：{}ms", methodName, stopWatch.getTotalTimeMillis());
        }
    }

}
```

环绕通知中必须调用 `joinPoint.proceed()`，目标方法才会真正执行。如果忘记调用，目标业务方法不会执行。环绕通知虽然灵活，但也更容易引入问题，因此不需要控制执行流程时，优先使用 `@Before`、`@AfterReturning` 或 `@AfterThrowing`。

## 切点表达式

切点表达式用于描述 AOP 应该拦截哪些连接点。Spring AOP 常用的切点表达式包括 `execution`、`@annotation`、`within` 和 `args`。实际项目中，推荐优先使用 `@annotation` 控制精确拦截范围，使用 `execution` 做包路径或方法级拦截，使用 `within` 限制类范围，使用 `args` 根据参数类型匹配方法。

### execution 表达式

`execution` 是最常用的切点表达式，用于按照方法修饰符、返回值、包名、类名、方法名和参数列表匹配方法。它适合拦截某个包下的所有 Controller 方法、Service 方法，或者拦截指定命名规则的方法。

`execution` 基础格式如下：

```text
execution(修饰符 返回值类型 包名.类名.方法名(参数列表))
```

常用写法如下：

```java
// 匹配 io.github.atengk.aopdemo.controller 包下所有类的所有方法
@Pointcut("execution(* io.github.atengk.aopdemo.controller.*.*(..))")
public void controllerPointcut() {
}

// 匹配 io.github.atengk.aopdemo.service 包及其子包下所有类的所有方法
@Pointcut("execution(* io.github.atengk.aopdemo.service..*.*(..))")
public void servicePointcut() {
}

// 匹配所有以 create 开头的方法
@Pointcut("execution(* io.github.atengk.aopdemo..*.create*(..))")
public void createMethodPointcut() {
}

// 匹配返回值为 String 的所有 Controller 方法
@Pointcut("execution(String io.github.atengk.aopdemo.controller.*.*(..))")
public void stringReturnPointcut() {
}
```

`execution` 中常用通配符说明如下：

| 通配符 | 说明                                             |
| ------ | ------------------------------------------------ |
| `*`    | 匹配任意返回值、任意类名、任意方法名或一层包路径 |
| `..`   | 匹配任意层级包路径，或匹配任意数量参数           |
| `+`    | 匹配指定类型及其子类型，较少在普通业务中使用     |

实际项目中，拦截 Controller 层可以使用：

```java
@Pointcut("execution(* io.github.atengk.aopdemo.controller..*.*(..))")
public void controllerPointcut() {
}
```

拦截 Service 层可以使用：

```java
@Pointcut("execution(* io.github.atengk.aopdemo.service..*.*(..))")
public void servicePointcut() {
}
```

需要注意，`execution` 范围不要写得过大。例如不建议直接写成 `execution(* *(..))`，否则可能拦截大量无关 Bean 方法，增加排查难度和运行开销。

### annotation 表达式

`annotation` 表达式用于匹配标注了指定注解的方法。它适合做操作日志、权限校验、幂等控制、数据权限等需要精确控制的场景。

文件位置：`src/main/java/io/github/atengk/aopdemo/annotation/OperationLog.java`

下面定义一个操作日志注解，用于标记需要记录操作日志的方法。

```java
package io.github.atengk.aopdemo.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作名称
     *
     * @return 操作名称
     */
    String value();

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/OperationLogAspect.java`

下面切点只匹配标注了 `@OperationLog` 的方法。

```java
package io.github.atengk.aopdemo.aop;

import io.github.atengk.aopdemo.annotation.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 匹配标注了 OperationLog 注解的方法
     */
    @Pointcut("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

}
```

在接口方法上使用注解：

```java
@OperationLog("创建订单")
@PostMapping("/create")
public OrderVO create(@RequestBody OrderCreateDTO orderCreateDTO) {
    return orderService.create(orderCreateDTO);
}
```

如果需要在通知方法中获取注解内容，可以直接将注解作为参数绑定。

```java
@Around("@annotation(operationLog)")
public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
    log.info("操作名称：{}", operationLog.value());
    return joinPoint.proceed();
}
```

`@annotation` 的优点是控制精确，不会因为包路径调整或方法命名变化而误拦截无关方法。实际业务开发中，操作日志、权限校验、幂等控制建议优先使用这种方式。

### within 表达式

`within` 表达式用于限制匹配的类型范围，也就是匹配指定类或指定包下的类。它关注的是目标对象所在类型，而不是具体方法签名。

常用写法如下：

```java
// 匹配 OrderController 类中的所有方法
@Pointcut("within(io.github.atengk.aopdemo.controller.OrderController)")
public void orderControllerPointcut() {
}

// 匹配 controller 包下所有类中的方法
@Pointcut("within(io.github.atengk.aopdemo.controller.*)")
public void controllerPointcut() {
}

// 匹配 controller 包及其子包下所有类中的方法
@Pointcut("within(io.github.atengk.aopdemo.controller..*)")
public void controllerAndSubPackagePointcut() {
}
```

`within` 和 `execution` 都可以用于按包路径拦截，但侧重点不同。`within` 更关注类是否在指定范围内；`execution` 更关注方法签名是否符合规则。如果只需要按类或包范围拦截，`within` 写法更简洁。如果需要控制返回值、方法名或参数列表，应使用 `execution`。

例如，只拦截 Controller 包下所有 Bean 方法，可以写成：

```java
@Pointcut("within(io.github.atengk.aopdemo.controller..*)")
public void controllerWithinPointcut() {
}
```

如果还要限制方法名以 `create` 开头，则更适合使用 `execution`：

```java
@Pointcut("execution(* io.github.atengk.aopdemo.controller..*.create*(..))")
public void createControllerMethodPointcut() {
}
```

### args 表达式

`args` 表达式用于按照方法运行时参数类型匹配连接点。它适合对某类参数对象进行统一处理，例如所有第一个参数为 `OrderCreateDTO` 的方法，或者所有接收指定业务上下文对象的方法。

常用写法如下：

```java
// 匹配只有一个参数，并且参数类型为 OrderCreateDTO 的方法
@Pointcut("args(io.github.atengk.aopdemo.model.dto.OrderCreateDTO)")
public void orderCreateArgsPointcut() {
}

// 匹配第一个参数为 OrderCreateDTO，后面还有任意数量参数的方法
@Pointcut("args(io.github.atengk.aopdemo.model.dto.OrderCreateDTO, ..)")
public void orderCreateFirstArgsPointcut() {
}

// 匹配任意参数数量，但其中参数规则由其他表达式组合限制
@Pointcut("execution(* io.github.atengk.aopdemo.service..*.*(..)) && args(io.github.atengk.aopdemo.model.dto.OrderCreateDTO)")
public void serviceOrderCreateArgsPointcut() {
}
```

`args` 按运行时参数类型匹配，和 `execution` 中按方法声明参数匹配并不完全相同。普通业务开发中，`args` 使用频率低于 `execution` 和 `@annotation`，但在需要根据参数对象统一增强时比较有用。

例如，对所有接收 `OrderCreateDTO` 参数的 Service 方法记录参数日志：

```java
@Before("execution(* io.github.atengk.aopdemo.service..*.*(..)) && args(orderCreateDTO)")
public void beforeOrderCreate(OrderCreateDTO orderCreateDTO) {
    log.info("创建订单参数：{}", JSONUtil.toJsonStr(orderCreateDTO));
}
```

这种写法可以直接在通知方法中获取匹配到的参数对象。需要注意，参数名 `orderCreateDTO` 必须与 `args(orderCreateDTO)` 中绑定的名称一致。

实际项目中，切点表达式可以组合使用。常见组合如下：

```java
// 拦截 Controller 包下，并且标注了 OperationLog 注解的方法
@Pointcut("within(io.github.atengk.aopdemo.controller..*) && @annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
public void controllerOperationLogPointcut() {
}

// 拦截 Service 包下 create 开头，并且第一个参数为 OrderCreateDTO 的方法
@Pointcut("execution(* io.github.atengk.aopdemo.service..*.create*(..)) && args(io.github.atengk.aopdemo.model.dto.OrderCreateDTO, ..)")
public void createOrderServicePointcut() {
}
```

切点表达式建议遵循以下原则：优先使用自定义注解控制业务增强范围；需要统一拦截某一层时使用 `execution` 或 `within`；需要根据参数对象增强时再使用 `args`；复杂表达式应拆分为多个命名清晰的 `@Pointcut` 方法，避免直接把过长表达式写在通知注解上。



## AOP 开发实现

本节通过一个完整的操作日志切面示例，说明 Spring Boot AOP 的实际开发流程。整体实现包括自定义注解、切面类、切点规则、方法参数获取、返回值获取和异常信息处理。后续接口日志记录、操作审计、权限校验、参数校验和耗时统计都可以基于这一套方式扩展。

### 定义自定义注解

自定义注解用于标记哪些方法需要被 AOP 增强。相比直接使用 `execution` 表达式拦截整个包，注解方式更精确，也更适合业务场景。例如只有标注了 `@OperationLog` 的接口才记录操作日志，没有标注的接口不会被切面处理。

文件位置：`src/main/java/io/github/atengk/aopdemo/annotation/OperationLog.java`

下面的注解用于标记需要记录操作日志的方法，可配置操作名称、操作类型、是否记录请求参数和是否记录返回结果。

```java
package io.github.atengk.aopdemo.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作名称
     *
     * @return 操作名称
     */
    String value();

    /**
     * 操作类型
     *
     * @return 操作类型
     */
    String type() default "业务操作";

    /**
     * 是否记录请求参数
     *
     * @return true：记录，false：不记录
     */
    boolean recordArgs() default true;

    /**
     * 是否记录返回结果
     *
     * @return true：记录，false：不记录
     */
    boolean recordResult() default true;

}
```

注解定义完成后，可以直接标注在 Controller 或 Service 方法上。建议优先标注在 Controller 方法上，这样可以更直观地记录接口入口信息；如果需要记录核心业务行为，也可以标注在 Service 方法上。

使用示例：

```java
@OperationLog(value = "创建订单", type = "新增")
@PostMapping("/create")
public OrderVO create(@RequestBody OrderCreateDTO orderCreateDTO) {
    return orderService.create(orderCreateDTO);
}
```

### 编写切面类

切面类用于承载具体增强逻辑。常规写法是使用 `@Aspect` 声明切面，使用 `@Component` 交给 Spring 容器管理，再通过通知注解编写增强逻辑。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/OperationLogAspect.java`

下面的切面类通过环绕通知统一记录操作名称、请求参数、返回结果、异常信息和执行耗时。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aopdemo.annotation.OperationLog;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 环绕通知，记录操作日志
     *
     * @param joinPoint    连接点
     * @param operationLog 操作日志注解
     * @return 方法返回值
     * @throws Throwable 目标方法异常
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = getMethodName(joinPoint);
        String operationName = operationLog.value();
        String operationType = operationLog.type();

        try {
            if (operationLog.recordArgs()) {
                log.info("操作开始，操作名称：{}，操作类型：{}，方法：{}，参数：{}",
                        operationName, operationType, methodName, buildArgsJson(joinPoint.getArgs()));
            } else {
                log.info("操作开始，操作名称：{}，操作类型：{}，方法：{}",
                        operationName, operationType, methodName);
            }

            Object result = joinPoint.proceed();

            long costTime = System.currentTimeMillis() - startTime;
            if (operationLog.recordResult()) {
                log.info("操作成功，操作名称：{}，方法：{}，耗时：{}ms，返回值：{}",
                        operationName, methodName, costTime, JSONUtil.toJsonStr(result));
            } else {
                log.info("操作成功，操作名称：{}，方法：{}，耗时：{}ms",
                        operationName, methodName, costTime);
            }

            return result;
        } catch (Throwable throwable) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("操作异常，操作名称：{}，方法：{}，耗时：{}ms，异常类型：{}，异常信息：{}",
                    operationName,
                    methodName,
                    costTime,
                    throwable.getClass().getName(),
                    throwable.getMessage(),
                    throwable);
            throw throwable;
        }
    }

    /**
     * 获取方法名称
     *
     * @param joinPoint 连接点
     * @return 方法名称
     */
    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return StrUtil.format("{}.{}", method.getDeclaringClass().getSimpleName(), method.getName());
    }

    /**
     * 构建参数 JSON
     *
     * @param args 方法参数
     * @return 参数 JSON
     */
    private String buildArgsJson(Object[] args) {
        if (ArrayUtil.isEmpty(args)) {
            return "[]";
        }

        Object[] filteredArgs = Arrays.stream(args)
                .filter(Objects::nonNull)
                .filter(arg -> !(arg instanceof ServletRequest))
                .filter(arg -> !(arg instanceof ServletResponse))
                .filter(arg -> !(arg instanceof MultipartFile))
                .toArray();

        return JSONUtil.toJsonStr(filteredArgs);
    }

}
```

这里使用 `@Around("@annotation(operationLog)")` 直接绑定自定义注解对象，因此通知方法中可以读取 `operationLog.value()`、`operationLog.type()` 等配置。相比在切面内部再通过反射获取注解，这种方式更简洁。

### 配置切点规则

切点规则用于决定哪些方法会被切面拦截。实际项目中常见方式有两种：一种是基于自定义注解拦截，另一种是基于包路径拦截。业务日志、权限校验、幂等控制建议优先使用注解切点；统一接口耗时统计、Controller 请求日志可以使用包路径切点。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/RequestLogAspect.java`

下面的切面通过 `execution` 表达式拦截 Controller 包下的所有接口方法，用于统一记录接口耗时。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 接口请求日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class RequestLogAspect {

    /**
     * Controller 接口切点
     */
    @Pointcut("execution(* io.github.atengk.aopdemo.controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 记录接口耗时
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 目标方法异常
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        try {
            Object result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;
            log.info("接口执行完成，方法：{}，耗时：{}", methodName, StrUtil.format("{}ms", costTime));
            return result;
        } catch (Throwable throwable) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("接口执行异常，方法：{}，耗时：{}ms，异常信息：{}",
                    methodName, costTime, throwable.getMessage(), throwable);
            throw throwable;
        }
    }

}
```

如果使用注解切点，可以写成：

```java
@Pointcut("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
public void operationLogPointcut() {
}
```

如果使用包路径切点，可以写成：

```java
@Pointcut("execution(* io.github.atengk.aopdemo.service..*.*(..))")
public void servicePointcut() {
}
```

如果需要组合多个条件，可以写成：

```java
@Pointcut("execution(* io.github.atengk.aopdemo.controller..*.*(..)) && @annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
public void controllerOperationLogPointcut() {
}
```

组合切点适合在大型项目中限制拦截范围。例如只拦截 Controller 层中标注了 `@OperationLog` 的接口，避免误拦截 Service 层内部方法。

### 获取方法参数

AOP 中可以通过 `JoinPoint` 或 `ProceedingJoinPoint` 获取目标方法参数。普通通知使用 `JoinPoint`，环绕通知使用 `ProceedingJoinPoint`。参数通常用于日志记录、参数校验、权限上下文判断和操作审计。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/ParamLogAspect.java`

下面的切面用于演示如何获取方法参数名称和参数值。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 参数日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class ParamLogAspect {

    /**
     * 方法执行前记录参数
     *
     * @param joinPoint 连接点
     */
    @Before("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (ArrayUtil.isEmpty(args)) {
            log.info("方法参数为空，方法：{}", signature.toShortString());
            return;
        }

        for (int i = 0; i < args.length; i++) {
            String parameterName = ArrayUtil.isNotEmpty(parameterNames) ? parameterNames[i] : StrUtil.format("arg{}", i);
            log.info("方法参数，方法：{}，参数名：{}，参数值：{}",
                    signature.toShortString(), parameterName, JSONUtil.toJsonStr(args[i]));
        }
    }

}
```

如果只需要获取所有参数值，可以直接使用：

```java
Object[] args = joinPoint.getArgs();
```

如果需要获取参数名，则需要通过 `MethodSignature` 获取：

```java
MethodSignature signature = (MethodSignature) joinPoint.getSignature();
String[] parameterNames = signature.getParameterNames();
```

需要注意，参数中如果包含 `HttpServletRequest`、`HttpServletResponse`、文件流、输入流等对象，不建议直接序列化到日志中，应先过滤或单独处理。

### 获取方法返回值

方法返回值可以通过 `@AfterReturning` 或 `@Around` 获取。`@AfterReturning` 只在目标方法正常返回后执行，适合记录成功结果；`@Around` 可以同时处理成功结果、异常和耗时，适合统一日志场景。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/ResultLogAspect.java`

下面的切面通过 `@AfterReturning` 获取方法返回值。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 返回值日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class ResultLogAspect {

    /**
     * 方法正常返回后记录结果
     *
     * @param joinPoint 连接点
     * @param result    返回结果
     */
    @AfterReturning(
            pointcut = "@annotation(io.github.atengk.aopdemo.annotation.OperationLog)",
            returning = "result"
    )
    public void afterReturning(JoinPoint joinPoint, Object result) {
        log.info("方法返回结果，方法：{}，返回值：{}",
                joinPoint.getSignature().toShortString(), JSONUtil.toJsonStr(result));
    }

}
```

`returning = "result"` 必须和方法参数中的 `result` 名称一致，否则 Spring 无法完成返回值绑定。

如果需要在获取返回值后继续返回给调用方，使用 `@Around` 更合适：

```java
@Around("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    Object result = joinPoint.proceed();
    log.info("方法返回值：{}", JSONUtil.toJsonStr(result));
    return result;
}
```

实际开发中，不建议在日志中无条件打印完整返回值。对于分页列表、大对象、文件内容、敏感字段，应做脱敏、截断或关闭返回值记录。

### 处理异常信息

异常信息可以通过 `@AfterThrowing` 或 `@Around` 捕获。`@AfterThrowing` 适合单独记录异常日志；`@Around` 适合统一处理成功、失败和耗时。无论哪种方式，都不建议在切面中随意吞掉异常，否则调用方和全局异常处理器可能无法感知真实错误。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/ExceptionLogAspect.java`

下面的切面通过 `@AfterThrowing` 记录异常信息。

```java
package io.github.atengk.aopdemo.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 异常日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class ExceptionLogAspect {

    /**
     * 方法抛出异常后记录异常信息
     *
     * @param joinPoint 连接点
     * @param throwable 异常对象
     */
    @AfterThrowing(
            pointcut = "@annotation(io.github.atengk.aopdemo.annotation.OperationLog)",
            throwing = "throwable"
    )
    public void afterThrowing(JoinPoint joinPoint, Throwable throwable) {
        log.error("方法执行异常，方法：{}，异常类型：{}，异常信息：{}",
                joinPoint.getSignature().toShortString(),
                throwable.getClass().getName(),
                throwable.getMessage(),
                throwable);
    }

}
```

如果使用 `@Around` 处理异常，必须在记录日志后重新抛出异常：

```java
try {
    return joinPoint.proceed();
} catch (Throwable throwable) {
    log.error("方法执行异常，异常信息：{}", throwable.getMessage(), throwable);
    throw throwable;
}
```

这种写法可以保证异常继续传递给全局异常处理器，例如 `@RestControllerAdvice`。如果业务要求切面中转换异常，也应明确抛出新的业务异常，而不是直接返回 `null` 或空对象。

## 常见应用场景

本节说明 Spring Boot AOP 在实际项目中的常见用法，包括接口日志记录、操作审计、权限校验、参数校验和接口耗时统计。这些场景本质上都是对方法调用过程进行统一增强，只是切点范围和处理逻辑不同。

### 接口日志记录

接口日志记录通常用于排查问题和分析接口调用情况。常见记录内容包括请求地址、请求方式、请求参数、响应结果、执行耗时和异常信息。接口日志建议拦截 Controller 层，因为 Controller 是 HTTP 请求进入系统的入口。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/ApiLogAspect.java`

下面的切面统一记录 Controller 接口请求信息。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 接口日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    /**
     * Controller 接口切点
     */
    @Pointcut("execution(* io.github.atengk.aopdemo.controller..*.*(..))")
    public void apiPointcut() {
    }

    /**
     * 记录接口请求日志
     *
     * @param joinPoint 连接点
     * @return 接口返回值
     * @throws Throwable 接口异常
     */
    @Around("apiPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getRequest();

        try {
            log.info("接口请求开始，请求方式：{}，请求地址：{}，方法：{}，参数：{}",
                    request.getMethod(),
                    request.getRequestURI(),
                    joinPoint.getSignature().toShortString(),
                    JSONUtil.toJsonStr(joinPoint.getArgs()));

            Object result = joinPoint.proceed();

            log.info("接口请求成功，请求地址：{}，耗时：{}ms，返回值：{}",
                    request.getRequestURI(),
                    System.currentTimeMillis() - startTime,
                    JSONUtil.toJsonStr(result));

            return result;
        } catch (Throwable throwable) {
            log.error("接口请求异常，请求地址：{}，耗时：{}ms，异常信息：{}",
                    request.getRequestURI(),
                    System.currentTimeMillis() - startTime,
                    throwable.getMessage(),
                    throwable);
            throw throwable;
        }
    }

    /**
     * 获取当前请求对象
     *
     * @return 当前请求对象
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

}
```

接口日志切面适合开发环境、测试环境和问题排查场景。生产环境中应避免打印过大的参数和返回值，也要避免记录密码、Token、身份证号、手机号等敏感信息。

### 操作审计

操作审计用于记录用户执行过的关键业务操作，例如新增订单、删除用户、导出报表、修改配置等。它和普通接口日志不同，接口日志偏技术排查，操作审计偏业务追踪，通常需要记录操作人、操作类型、业务名称、执行结果和操作时间。

文件位置：`src/main/java/io/github/atengk/aopdemo/annotation/AuditLog.java`

下面的注解用于标记需要审计的业务操作。

```java
package io.github.atengk.aopdemo.annotation;

import java.lang.annotation.*;

/**
 * 操作审计注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 审计名称
     *
     * @return 审计名称
     */
    String value();

    /**
     * 操作类型
     *
     * @return 操作类型
     */
    String type();

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/AuditLogAspect.java`

下面的切面演示如何记录审计日志。示例中使用固定用户，实际项目应从登录上下文、Sa-Token、Spring Security 或网关请求头中获取当前用户。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aopdemo.annotation.AuditLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 操作审计切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    /**
     * 记录操作审计
     *
     * @param joinPoint 连接点
     * @param auditLog  审计注解
     * @return 方法返回值
     * @throws Throwable 目标方法异常
     */
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String operator = "admin";
        String operationTime = DateUtil.now();

        try {
            Object result = joinPoint.proceed();

            log.info("操作审计成功，操作人：{}，操作名称：{}，操作类型：{}，操作时间：{}，返回值：{}",
                    operator,
                    auditLog.value(),
                    auditLog.type(),
                    operationTime,
                    JSONUtil.toJsonStr(result));

            return result;
        } catch (Throwable throwable) {
            log.error("操作审计失败，操作人：{}，操作名称：{}，操作类型：{}，操作时间：{}，异常信息：{}",
                    operator,
                    auditLog.value(),
                    auditLog.type(),
                    operationTime,
                    throwable.getMessage(),
                    throwable);
            throw throwable;
        }
    }

}
```

使用示例：

```java
@AuditLog(value = "删除订单", type = "删除")
@DeleteMapping("/{id}")
public Boolean delete(@PathVariable Long id) {
    return orderService.delete(id);
}
```

实际项目中，审计日志通常不只输出到控制台，而是写入数据库、消息队列或日志平台。如果写入数据库，建议采用异步方式，避免审计逻辑影响主业务接口性能。

### 权限校验

权限校验可以通过 AOP 对标注了权限注解的方法进行统一拦截。常见做法是在方法上标注所需权限编码，切面中获取当前用户权限列表并判断是否包含该权限。

文件位置：`src/main/java/io/github/atengk/aopdemo/annotation/PermissionCheck.java`

下面的注解用于声明接口所需权限编码。

```java
package io.github.atengk.aopdemo.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PermissionCheck {

    /**
     * 权限编码
     *
     * @return 权限编码
     */
    String value();

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/PermissionCheckAspect.java`

下面的切面用于校验当前用户是否具备指定权限。示例中使用固定权限集合，实际项目应从登录态或权限服务中获取。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.aopdemo.annotation.PermissionCheck;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限校验切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class PermissionCheckAspect {

    /**
     * 方法执行前校验权限
     *
     * @param permissionCheck 权限注解
     */
    @Before("@annotation(permissionCheck)")
    public void before(PermissionCheck permissionCheck) {
        List<String> currentPermissions = List.of("order:create", "order:list");
        String requiredPermission = permissionCheck.value();

        if (!CollUtil.contains(currentPermissions, requiredPermission)) {
            log.warn("权限校验失败，缺少权限：{}", requiredPermission);
            throw new IllegalArgumentException("无操作权限");
        }

        log.info("权限校验通过，权限：{}", requiredPermission);
    }

}
```

使用示例：

```java
@PermissionCheck("order:create")
@PostMapping("/create")
public OrderVO create(@RequestBody OrderCreateDTO orderCreateDTO) {
    return orderService.create(orderCreateDTO);
}
```

在真实系统中，权限异常不建议直接使用 `IllegalArgumentException`，应定义统一业务异常，例如 `BusinessException`，并由全局异常处理器转换为标准响应结构。

### 参数校验

参数校验可以分为两类：一类是 Spring Validation 提供的标准参数校验，例如 `@Valid`、`@NotBlank`、`@NotNull`；另一类是业务型参数校验，例如检查金额是否大于 0、订单状态是否允许操作、用户是否属于当前租户等。AOP 更适合处理第二类业务型参数校验。

文件位置：`src/main/java/io/github/atengk/aopdemo/annotation/OrderParamCheck.java`

下面的注解用于标记需要执行订单参数校验的方法。

```java
package io.github.atengk.aopdemo.annotation;

import java.lang.annotation.*;

/**
 * 订单参数校验注解
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OrderParamCheck {

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/OrderParamCheckAspect.java`

下面的切面从方法参数中查找订单创建参数，并进行基础业务校验。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aopdemo.model.dto.OrderCreateDTO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * 订单参数校验切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class OrderParamCheckAspect {

    /**
     * 方法执行前校验订单参数
     *
     * @param joinPoint 连接点
     */
    @Before("@annotation(io.github.atengk.aopdemo.annotation.OrderParamCheck)")
    public void before(JoinPoint joinPoint) {
        OrderCreateDTO orderCreateDTO = Arrays.stream(joinPoint.getArgs())
                .filter(OrderCreateDTO.class::isInstance)
                .map(OrderCreateDTO.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("订单参数不能为空"));

        if (StrUtil.isBlank(orderCreateDTO.getProductName())) {
            log.warn("订单参数校验失败，商品名称为空");
            throw new IllegalArgumentException("商品名称不能为空");
        }

        BigDecimal amount = orderCreateDTO.getAmount();
        if (amount == null || !NumberUtil.isGreater(amount, BigDecimal.ZERO)) {
            log.warn("订单参数校验失败，订单金额不合法，金额：{}", amount);
            throw new IllegalArgumentException("订单金额必须大于0");
        }

        log.info("订单参数校验通过，商品名称：{}，金额：{}",
                orderCreateDTO.getProductName(), orderCreateDTO.getAmount());
    }

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/model/dto/OrderCreateDTO.java`

下面是订单创建参数对象，用于配合参数校验切面使用。

```java
package io.github.atengk.aopdemo.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderCreateDTO {

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 订单金额
     */
    private BigDecimal amount;

}
```

使用示例：

```java
@OrderParamCheck
@OperationLog(value = "创建订单", type = "新增")
@PostMapping("/create")
public OrderVO create(@RequestBody OrderCreateDTO orderCreateDTO) {
    return orderService.create(orderCreateDTO);
}
```

参数校验切面适合处理跨多个接口重复出现的业务校验。如果校验逻辑只属于某一个业务方法，直接写在 Service 中会更清晰，不需要强行使用 AOP。

### 接口耗时统计

接口耗时统计用于观察接口性能，辅助定位慢接口。常见做法是拦截 Controller 层或 Service 层方法，在方法执行前记录开始时间，在方法执行后计算耗时并输出日志。

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/CostTimeAspect.java`

下面的切面用于统计 Controller 接口执行耗时，并对超过阈值的接口输出慢接口日志。

```java
package io.github.atengk.aopdemo.aop;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 接口耗时统计切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Component
public class CostTimeAspect {

    /**
     * 慢接口阈值，单位毫秒
     */
    private static final long SLOW_API_THRESHOLD = 1000L;

    /**
     * Controller 接口切点
     */
    @Pointcut("execution(* io.github.atengk.aopdemo.controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 统计接口耗时
     *
     * @param joinPoint 连接点
     * @return 接口返回值
     * @throws Throwable 接口异常
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        try {
            return joinPoint.proceed();
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            String costText = StrUtil.format("{}ms", costTime);

            if (costTime >= SLOW_API_THRESHOLD) {
                log.warn("接口耗时较高，方法：{}，耗时：{}，阈值：{}ms",
                        methodName, costText, SLOW_API_THRESHOLD);
            } else {
                log.info("接口耗时统计，方法：{}，耗时：{}", methodName, costText);
            }
        }
    }

}
```

接口耗时统计可以放在 Controller 层，也可以放在 Service 层。Controller 层耗时更接近接口整体耗时，Service 层耗时更接近核心业务执行耗时。实际项目中可以根据排查目标选择不同切点。

如果多个切面同时作用于同一个方法，例如接口日志切面、权限校验切面、耗时统计切面同时生效，建议使用 `@Order` 明确切面顺序，避免执行顺序不可控。



## 开发注意事项

本节说明 Spring Boot AOP 开发中容易出现问题的几个关键点，包括代理机制、内部方法调用失效、多个切面执行顺序和异常处理边界。AOP 代码通常不复杂，但如果不了解代理机制，很容易出现切面不生效、日志重复、异常被吞掉、执行顺序不符合预期等问题。

### 代理机制说明

Spring AOP 是基于代理机制实现的。业务方法被调用时，实际调用的是 Spring 创建出来的代理对象，代理对象在目标方法执行前后插入切面逻辑，然后再调用真正的目标对象方法。

Spring AOP 常见代理方式如下：

| 代理方式     | 说明                       | 适用情况                                      |
| ------------ | -------------------------- | --------------------------------------------- |
| JDK 动态代理 | 基于接口创建代理对象       | 目标类实现了接口，并且配置为使用 JDK 代理     |
| CGLIB 代理   | 基于目标类创建子类代理对象 | 目标类没有接口，或 Spring Boot 默认使用类代理 |

在 Spring Boot 项目中，默认通常使用 CGLIB 代理。前面配置过的 `spring.aop.proxy-target-class: true` 表示使用基于类的代理方式。

文件位置：`src/main/resources/application.yml`

下面配置用于明确指定 Spring AOP 使用 CGLIB 代理。

```yaml
spring:
  aop:
    # true 表示使用 CGLIB 代理；false 表示使用 JDK 动态代理
    proxy-target-class: true
```

代理机制带来的直接影响如下：

| 注意点                 | 说明                                                       |
| ---------------------- | ---------------------------------------------------------- |
| 只对 Spring Bean 生效  | 普通 `new` 出来的对象不会被 AOP 增强                       |
| 通过代理对象调用才生效 | 绕过代理对象调用目标方法时，切面不会执行                   |
| 内部方法调用通常不生效 | 同一个类中的 `this.xxx()` 不会经过代理对象                 |
| final 方法不适合增强   | CGLIB 需要通过子类覆盖方法实现增强，`final` 方法无法被覆盖 |
| private 方法不适合增强 | 私有方法无法作为外部代理调用入口                           |

因此，实际开发中建议将需要被 AOP 增强的方法定义为 `public` 方法，并放在 Spring 管理的 Bean 中，例如 `@Service`、`@Component` 或 `@RestController` 标注的类。

### 方法调用失效场景

AOP 最常见的失效场景是同类内部方法调用。因为内部方法调用使用的是当前对象的 `this`，不会经过 Spring 代理对象，所以切面不会执行。

下面代码中，`create()` 方法直接调用同一个类中的 `saveLog()` 方法，即使 `saveLog()` 标注了 `@OperationLog`，切面通常也不会生效。

文件位置：`src/main/java/io/github/atengk/aopdemo/service/impl/OrderServiceImpl.java`

下面代码演示了同类内部调用导致 AOP 不生效的场景。

```java
package io.github.atengk.aopdemo.service.impl;

import io.github.atengk.aopdemo.annotation.OperationLog;
import io.github.atengk.aopdemo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    /**
     * 创建订单
     */
    @Override
    public void create() {
        log.info("开始创建订单");

        // 同类内部调用，不经过 Spring 代理对象，saveLog 方法上的切面通常不会生效
        this.saveLog();

        log.info("订单创建完成");
    }

    /**
     * 保存日志
     */
    @OperationLog(value = "保存订单日志", type = "新增")
    public void saveLog() {
        log.info("保存订单日志");
    }

}
```

推荐做法是将需要被增强的方法拆分到另一个 Spring Bean 中，通过 Bean 之间的调用触发代理。

文件位置：`src/main/java/io/github/atengk/aopdemo/service/OrderLogService.java`

下面接口用于拆分订单日志能力。

```java
package io.github.atengk.aopdemo.service;

/**
 * 订单日志服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface OrderLogService {

    /**
     * 保存订单日志
     */
    void saveLog();

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/service/impl/OrderLogServiceImpl.java`

下面实现类中的方法由独立 Bean 暴露，外部调用时会经过 Spring 代理对象。

```java
package io.github.atengk.aopdemo.service.impl;

import io.github.atengk.aopdemo.annotation.OperationLog;
import io.github.atengk.aopdemo.service.OrderLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单日志服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class OrderLogServiceImpl implements OrderLogService {

    /**
     * 保存订单日志
     */
    @Override
    @OperationLog(value = "保存订单日志", type = "新增")
    public void saveLog() {
        log.info("保存订单日志");
    }

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/service/impl/OrderServiceImpl.java`

下面代码通过注入另一个 Spring Bean 调用日志方法，可以正常触发切面。

```java
package io.github.atengk.aopdemo.service.impl;

import io.github.atengk.aopdemo.service.OrderLogService;
import io.github.atengk.aopdemo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderLogService orderLogService;

    /**
     * 创建订单
     */
    @Override
    public void create() {
        log.info("开始创建订单");

        // 通过其他 Spring Bean 调用，能够经过代理对象
        orderLogService.saveLog();

        log.info("订单创建完成");
    }

}
```

实际项目中，推荐优先使用“拆分 Bean”的方式解决内部调用失效问题。不要为了触发 AOP 把业务结构设计得过于绕，也不要在业务代码中大量使用代理对象自调用，否则会增加维护成本。

### 切面执行顺序

当多个切面同时作用于同一个方法时，需要明确切面的执行顺序。例如权限校验、参数校验、操作日志、耗时统计可能同时拦截一个接口。如果不指定顺序，执行结果可能不符合预期。

Spring 中可以通过 `@Order` 控制切面顺序。数值越小，优先级越高。对于环绕通知而言，优先级高的切面会先进入，最后退出；优先级低的切面会后进入，先退出。

常见顺序建议如下：

| 顺序 | 切面         | 说明                                   |
| ---- | ------------ | -------------------------------------- |
| 1    | 权限校验切面 | 最先判断用户是否有权限，无权限直接拒绝 |
| 2    | 参数校验切面 | 权限通过后再校验业务参数               |
| 3    | 操作日志切面 | 记录业务操作过程                       |
| 4    | 耗时统计切面 | 统计整体执行耗时                       |

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/PermissionCheckAspect.java`

下面代码将权限校验切面设置为最高优先级。

```java
package io.github.atengk.aopdemo.aop;

import io.github.atengk.aopdemo.annotation.PermissionCheck;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 权限校验切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Order(1)
@Component
public class PermissionCheckAspect {

    /**
     * 方法执行前校验权限
     *
     * @param permissionCheck 权限校验注解
     */
    @Before("@annotation(permissionCheck)")
    public void before(PermissionCheck permissionCheck) {
        log.info("执行权限校验，权限编码：{}", permissionCheck.value());
    }

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/aop/OperationLogAspect.java`

下面代码将操作日志切面设置为较低优先级。

```java
package io.github.atengk.aopdemo.aop;

import io.github.atengk.aopdemo.annotation.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Aspect
@Order(3)
@Component
public class OperationLogAspect {

    /**
     * 记录操作日志
     *
     * @param joinPoint    连接点
     * @param operationLog 操作日志注解
     * @return 方法返回值
     * @throws Throwable 目标方法异常
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        log.info("操作日志开始，操作名称：{}", operationLog.value());
        try {
            return joinPoint.proceed();
        } finally {
            log.info("操作日志结束，操作名称：{}", operationLog.value());
        }
    }

}
```

假设一个方法同时被两个切面拦截，执行顺序大致如下：

```text
权限校验切面开始
操作日志切面开始
目标业务方法执行
操作日志切面结束
权限校验切面结束
```

如果切面中存在异常拦截、返回值修改或权限阻断，顺序会直接影响最终结果。权限、认证、参数校验类切面通常应放在前面；日志、审计、耗时统计类切面通常可以放在后面。

### 异常处理边界

AOP 可以记录异常，但不建议随意吞掉异常。切面中捕获异常后，如果不继续抛出，调用方和全局异常处理器将无法感知真实错误，接口可能返回错误的成功结果。

错误写法如下：

```java
@Around("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
public Object around(ProceedingJoinPoint joinPoint) {
    try {
        return joinPoint.proceed();
    } catch (Throwable throwable) {
        log.error("方法执行异常，异常信息：{}", throwable.getMessage(), throwable);

        // 错误示例：吞掉异常，调用方会收到 null
        return null;
    }
}
```

推荐写法是在记录异常后继续抛出异常，让全局异常处理器统一处理响应。

```java
@Around("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
        return joinPoint.proceed();
    } catch (Throwable throwable) {
        log.error("方法执行异常，异常信息：{}", throwable.getMessage(), throwable);

        // 推荐写法：继续抛出异常
        throw throwable;
    }
}
```

如果业务要求在切面中转换异常，也应该转换为明确的业务异常，而不是返回 `null`、空字符串或空集合。

```java
@Around("@annotation(io.github.atengk.aopdemo.annotation.OperationLog)")
public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
        return joinPoint.proceed();
    } catch (IllegalArgumentException exception) {
        log.warn("业务参数异常，异常信息：{}", exception.getMessage());

        // 根据项目规范转换为统一业务异常
        throw new IllegalArgumentException("请求参数不合法：" + exception.getMessage());
    } catch (Throwable throwable) {
        log.error("系统异常，异常信息：{}", throwable.getMessage(), throwable);
        throw throwable;
    }
}
```

异常处理建议遵循以下边界：

| 场景                 | 建议                                    |
| -------------------- | --------------------------------------- |
| 只记录异常           | 记录后继续 `throw`                      |
| 转换业务异常         | 明确抛出新的业务异常                    |
| 统计异常次数         | 统计后继续抛出异常                      |
| 保存异常审计         | 审计失败不能影响原异常抛出              |
| `finally` 中处理日志 | 不要在 `finally` 中抛出新异常覆盖原异常 |

如果项目中已经有 `@RestControllerAdvice` 全局异常处理器，AOP 切面只负责补充日志、审计和统计，不应替代全局异常处理器。

## 功能测试

本节通过正常请求、异常请求和日志输出验证 AOP 是否生效。测试前需要确保项目已经引入 `spring-boot-starter-web`、`spring-boot-starter-aop`，并且切面类、注解类、Controller 和 Service 都在 Spring Boot 启动类扫描路径下。

### 正常请求测试

正常请求测试用于验证目标方法可以执行、切面可以进入、请求参数和返回结果可以正常记录。这里使用一个订单创建接口作为测试入口。

文件位置：`src/main/java/io/github/atengk/aopdemo/model/dto/OrderCreateDTO.java`

下面参数对象用于接收创建订单请求参数。

```java
package io.github.atengk.aopdemo.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class OrderCreateDTO {

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 订单金额
     */
    private BigDecimal amount;

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/model/vo/OrderVO.java`

下面响应对象用于返回订单创建结果。

```java
package io.github.atengk.aopdemo.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单响应结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    /**
     * 订单编号
     */
    private Long id;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 订单状态
     */
    private String status;

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/service/OrderService.java`

下面接口定义订单创建和异常测试方法。

```java
package io.github.atengk.aopdemo.service;

import io.github.atengk.aopdemo.model.dto.OrderCreateDTO;
import io.github.atengk.aopdemo.model.vo.OrderVO;

/**
 * 订单服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface OrderService {

    /**
     * 创建订单
     *
     * @param orderCreateDTO 订单创建参数
     * @return 订单信息
     */
    OrderVO create(OrderCreateDTO orderCreateDTO);

    /**
     * 创建异常订单
     *
     * @param orderCreateDTO 订单创建参数
     * @return 订单信息
     */
    OrderVO createError(OrderCreateDTO orderCreateDTO);

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/service/impl/OrderServiceImpl.java`

下面实现类用于模拟订单创建成功和订单创建失败两种情况。

```java
package io.github.atengk.aopdemo.service.impl;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.aopdemo.model.dto.OrderCreateDTO;
import io.github.atengk.aopdemo.model.vo.OrderVO;
import io.github.atengk.aopdemo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    /**
     * 创建订单
     *
     * @param orderCreateDTO 订单创建参数
     * @return 订单信息
     */
    @Override
    public OrderVO create(OrderCreateDTO orderCreateDTO) {
        log.info("执行业务逻辑，创建订单，商品名称：{}", orderCreateDTO.getProductName());

        return new OrderVO(
                IdUtil.getSnowflakeNextId(),
                orderCreateDTO.getProductName(),
                orderCreateDTO.getAmount(),
                "CREATED"
        );
    }

    /**
     * 创建异常订单
     *
     * @param orderCreateDTO 订单创建参数
     * @return 订单信息
     */
    @Override
    public OrderVO createError(OrderCreateDTO orderCreateDTO) {
        log.info("执行业务逻辑，模拟创建订单异常，商品名称：{}", orderCreateDTO.getProductName());
        throw new IllegalArgumentException("库存不足，无法创建订单");
    }

}
```

文件位置：`src/main/java/io/github/atengk/aopdemo/controller/OrderController.java`

下面 Controller 提供正常请求和异常请求两个测试接口。

```java
package io.github.atengk.aopdemo.controller;

import io.github.atengk.aopdemo.annotation.OperationLog;
import io.github.atengk.aopdemo.model.dto.OrderCreateDTO;
import io.github.atengk.aopdemo.model.vo.OrderVO;
import io.github.atengk.aopdemo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param orderCreateDTO 订单创建参数
     * @return 订单信息
     */
    @OperationLog(value = "创建订单", type = "新增")
    @PostMapping("/create")
    public OrderVO create(@RequestBody OrderCreateDTO orderCreateDTO) {
        return orderService.create(orderCreateDTO);
    }

    /**
     * 创建异常订单
     *
     * @param orderCreateDTO 订单创建参数
     * @return 订单信息
     */
    @OperationLog(value = "创建异常订单", type = "新增")
    @PostMapping("/create-error")
    public OrderVO createError(@RequestBody OrderCreateDTO orderCreateDTO) {
        return orderService.createError(orderCreateDTO);
    }

}
```

启动项目后，执行正常请求测试：

```bash
curl -X POST "http://localhost:8080/orders/create" \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "机械键盘",
    "amount": 299.00
  }'
```

预期响应示例：

```json
{
  "id": 123456789012345678,
  "productName": "机械键盘",
  "amount": 299.00,
  "status": "CREATED"
}
```

如果接口正常返回，并且控制台输出了操作开始、业务执行、操作成功、耗时统计等日志，说明 AOP 正常生效。

### 异常请求测试

异常请求测试用于验证目标方法抛出异常时，切面能否正确记录异常信息，并且异常能够继续交给全局异常处理器或 Spring MVC 默认异常处理流程。

如果项目中没有全局异常处理器，可以先添加一个简单的统一异常处理类，便于观察接口返回结果。

文件位置：`src/main/java/io/github/atengk/aopdemo/handler/GlobalExceptionHandler.java`

下面异常处理器用于将业务异常转换为统一 JSON 响应。

```java
package io.github.atengk.aopdemo.handler;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数异常
     *
     * @param exception 参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("请求参数异常，异常信息：{}", exception.getMessage());
        return MapUtil.builder(new java.util.HashMap<String, Object>())
                .put("code", 400)
                .put("message", exception.getMessage())
                .put("success", false)
                .build();
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception exception) {
        log.error("系统异常，异常信息：{}", exception.getMessage(), exception);
        return MapUtil.builder(new java.util.HashMap<String, Object>())
                .put("code", 500)
                .put("message", "系统异常，请联系管理员")
                .put("success", false)
                .build();
    }

}
```

执行异常请求测试：

```bash
curl -X POST "http://localhost:8080/orders/create-error" \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "显示器",
    "amount": 1299.00
  }'
```

预期响应示例：

```json
{
  "code": 400,
  "message": "库存不足，无法创建订单",
  "success": false
}
```

异常测试需要重点确认两点：第一，切面中可以记录到异常类型和异常信息；第二，异常没有被切面吞掉，而是继续传递给全局异常处理器。如果控制台只有切面异常日志，但接口返回 `200` 或返回 `null`，说明切面中错误地吞掉了异常。

### 日志输出验证

日志输出验证用于确认 AOP 是否按照预期执行。验证时可以从正常请求日志、异常请求日志和执行顺序日志三个方面检查。

正常请求预期日志类似如下：

```text
操作开始，操作名称：创建订单，操作类型：新增，方法：OrderController.create，参数：[{"productName":"机械键盘","amount":299.00}]
执行业务逻辑，创建订单，商品名称：机械键盘
操作成功，操作名称：创建订单，方法：OrderController.create，耗时：35ms，返回值：{"id":123456789012345678,"productName":"机械键盘","amount":299.00,"status":"CREATED"}
```

异常请求预期日志类似如下：

```text
操作开始，操作名称：创建异常订单，操作类型：新增，方法：OrderController.createError，参数：[{"productName":"显示器","amount":1299.00}]
执行业务逻辑，模拟创建订单异常，商品名称：显示器
操作异常，操作名称：创建异常订单，方法：OrderController.createError，耗时：12ms，异常类型：java.lang.IllegalArgumentException，异常信息：库存不足，无法创建订单
请求参数异常，异常信息：库存不足，无法创建订单
```

如果配置了多个切面，并使用 `@Order` 控制顺序，可以通过日志检查执行顺序。例如权限切面、操作日志切面同时生效时，预期顺序如下：

```text
执行权限校验，权限编码：order:create
操作日志开始，操作名称：创建订单
执行业务逻辑，创建订单，商品名称：机械键盘
操作日志结束，操作名称：创建订单
```

如果日志没有输出，优先检查以下问题：

| 问题                  | 检查方式                                                 |
| --------------------- | -------------------------------------------------------- |
| 切面类没有被扫描      | 确认切面类包路径在启动类扫描范围内                       |
| 缺少 AOP 依赖         | 确认 `pom.xml` 已引入 `spring-boot-starter-aop`          |
| 缺少 `@Aspect`        | 确认切面类上存在 `@Aspect`                               |
| 缺少 Spring Bean 注解 | 确认切面类上存在 `@Component`                            |
| 切点表达式不匹配      | 检查包路径、注解路径、方法签名是否正确                   |
| 内部方法调用          | 检查是否通过 `this.xxx()` 调用了目标方法                 |
| 方法不是 public       | 建议将需要增强的方法定义为 `public`                      |
| 注解没有运行时保留    | 确认自定义注解存在 `@Retention(RetentionPolicy.RUNTIME)` |

最终验证标准如下：

| 测试项     | 预期结果                                 |
| ---------- | ---------------------------------------- |
| 正常请求   | 接口正常返回，切面记录参数、返回值和耗时 |
| 异常请求   | 接口返回错误响应，切面记录异常信息       |
| 方法参数   | 日志中能看到请求参数                     |
| 方法返回值 | 日志中能看到正常返回结果                 |
| 异常信息   | 日志中能看到异常类型和异常消息           |
| 执行顺序   | 多个切面按 `@Order` 顺序执行             |
| 异常传递   | 异常能够继续进入全局异常处理器           |