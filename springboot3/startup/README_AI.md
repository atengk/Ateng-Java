# Spring Boot 应用启动执行机制

## 概述

Spring Boot 应用启动执行机制用于描述应用从 `main` 方法执行开始，到 Spring 容器初始化完成、启动任务执行完毕、应用进入可用状态的完整过程。
在实际开发中，启动执行机制主要用于处理系统初始化、资源检查、缓存预热、启动任务编排和应用就绪后的业务触发逻辑。

理解启动执行机制，可以帮助开发者明确不同扩展点的执行时机，避免把耗时任务、外部依赖访问、业务初始化逻辑放在错误阶段，从而降低启动失败、启动变慢和状态不一致的风险。

### 启动执行机制的作用

启动执行机制的核心作用，是为应用启动过程中的初始化逻辑提供标准化、可控制、可扩展的执行入口。

Spring Boot 启动过程中会依次完成环境准备、配置加载、容器创建、Bean 注册、Bean 初始化、事件发布、Runner 执行和应用就绪通知。不同阶段对应不同的扩展点，开发者可以根据业务需求选择合适的执行位置。

主要作用包括：

1. **执行系统初始化逻辑**

   应用启动时可以加载系统参数、业务字典、本地缓存、权限配置、路由规则等基础数据，为后续业务请求提供运行基础。

2. **完成外部资源检查**

   启动阶段可以检查数据库、Redis、消息队列、对象存储、第三方接口等资源是否可用。核心依赖不可用时可以中断启动，非核心依赖不可用时可以记录日志并降级处理。

3. **控制启动任务执行顺序**

   当多个初始化任务之间存在依赖关系时，可以通过 `@Order`、`Ordered`、`SmartLifecycle#getPhase` 等方式控制执行顺序，避免后置任务提前执行。

4. **区分容器初始化与应用就绪**

   Bean 初始化完成不代表应用已经完全就绪。Spring Boot 通过不同事件区分容器刷新完成、应用启动完成和应用准备就绪等状态，便于在正确阶段执行对应逻辑。

5. **提高启动过程可观测性**

   启动任务中可以记录任务名称、执行阶段、耗时、结果和异常信息，便于定位启动慢、启动失败、初始化数据异常等问题。

### 常见使用场景

Spring Boot 启动执行机制常用于应用启动阶段的一次性任务。不同任务应选择不同扩展点，不建议将所有启动逻辑都堆叠在一个 `CommandLineRunner` 或 `ApplicationRunner` 中。

| 使用场景                   | 推荐扩展点                                     | 说明                                               |
| -------------------------- | ---------------------------------------------- | -------------------------------------------------- |
| 系统配置预加载             | `ApplicationRunner`、`CommandLineRunner`       | 容器初始化完成后加载数据库或配置中心中的系统参数   |
| 字典数据加载               | `ApplicationRunner`                            | 启动后加载业务字典、枚举映射、区域数据等           |
| 本地缓存初始化             | `ApplicationRunner`、`ApplicationReadyEvent`   | 核心缓存可同步加载，非核心缓存建议异步加载         |
| Redis 缓存预热             | `ApplicationReadyEvent`                        | 应用就绪后触发，避免阻塞主启动流程                 |
| 数据库连接检查             | `ApplicationStartedEvent`、`ApplicationRunner` | 用于确认核心数据源是否可用                         |
| 第三方服务检查             | `ApplicationReadyEvent`                        | 非核心外部依赖建议降级处理                         |
| 启动后台组件               | `SmartLifecycle`                               | 适合需要随容器启动和停止的组件，例如消费端、监听器 |
| Bean 内部初始化            | `@PostConstruct`、`InitializingBean`           | 适合单个 Bean 内部的轻量级初始化                   |
| 所有单例 Bean 初始化后执行 | `SmartInitializingSingleton`                   | 适合依赖多个单例 Bean 的初始化逻辑                 |
| 应用启动完成后通知外部系统 | `ApplicationReadyEvent`                        | 适合上报实例状态、发送上线通知、注册业务能力       |

选择扩展点时，通常需要判断三个问题：

第一，当前任务是否依赖 Spring Bean。
如果依赖业务 Service、Mapper、RedisTemplate 等组件，应放在 Spring 容器初始化完成之后执行。

第二，当前任务失败是否应该阻止应用启动。
核心配置、数据库连接等关键任务失败时可以中断启动；非核心缓存预热、统计任务、外部通知失败时通常不应阻止启动。

第三，当前任务是否耗时。
耗时任务不建议放在 Bean 初始化阶段执行，必要时应放到应用就绪阶段并使用线程池异步处理。

### Spring Boot 3 适配说明

Spring Boot 3 基于 Spring Framework 6 构建，启动执行机制整体上仍然沿用 Spring Boot 2 中常见的扩展方式，例如 `CommandLineRunner`、`ApplicationRunner`、`ApplicationReadyEvent`、`ApplicationStartedEvent`、`SmartLifecycle` 等仍然可用。

但是在 Spring Boot 3 项目中使用启动执行机制时，需要关注以下适配点。

1. **JDK 版本要求**

   Spring Boot 3 要求 Java 17 及以上版本。启动任务代码、构建环境、运行环境、Docker 镜像和 CI/CD 流程都需要统一适配 Java 17 或更高版本。

2. **Jakarta 命名空间变化**

   Spring Boot 3 使用 Jakarta EE 9 规范，原来的 `javax.*` 包名需要替换为 `jakarta.*`。

   例如使用 `@PostConstruct` 时，应使用：

   ```java
   import jakarta.annotation.PostConstruct;
   ```

   不应继续使用：

   ```java
   import javax.annotation.PostConstruct;
   ```

3. **生命周期扩展点保持兼容**

   Spring Boot 3 仍然支持常见 Bean 生命周期回调和启动扩展点，包括：

   - `@PostConstruct`
   - `InitializingBean`
   - `SmartInitializingSingleton`
   - `SmartLifecycle`
   - `CommandLineRunner`
   - `ApplicationRunner`
   - `ApplicationStartedEvent`
   - `ApplicationReadyEvent`
   - `ApplicationFailedEvent`

4. **AOT 与 Native Image 场景需要谨慎**

   如果项目使用 Spring AOT 或 GraalVM Native Image，启动任务中应尽量避免动态类加载、运行时反射扫描、动态代理生成等逻辑。
   如果确实需要使用反射，应通过 Runtime Hints 显式声明运行时元数据。

5. **更重视启动耗时控制**

   Spring Boot 3 常用于容器化和云原生部署场景，启动耗时会影响 Kubernetes 探针、滚动发布、自动扩缩容和故障恢复速度。
   对于非核心启动任务，应优先采用异步执行、延迟加载或按需加载。

6. **日志与可观测性要求更高**

   启动阶段发生的问题通常会导致应用无法对外提供服务，因此启动任务中应记录清晰的中文日志，包括任务名称、执行阶段、耗时、结果和异常原因。
   生产环境可以结合 Actuator、Micrometer、日志平台和告警系统对启动状态进行监控。

## 应用启动生命周期

Spring Boot 应用启动生命周期可以理解为从执行 `SpringApplication.run(...)` 到应用完全就绪的过程。
该过程不是单一动作，而是由环境准备、上下文创建、Bean 定义加载、容器刷新、Bean 初始化、Runner 执行、事件发布等多个阶段组成。

在开发启动任务时，需要明确当前代码处于哪个阶段，以及该阶段是否可以安全访问业务 Bean、数据库、缓存、消息队列和外部服务。

### SpringApplication 启动流程

`SpringApplication` 是 Spring Boot 应用启动的核心入口。标准启动类通常如下：

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootApplication
public class DemoApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
```

`SpringApplication.run(...)` 内部会完成应用启动的主要流程。简化后的执行顺序如下：

```text
main 方法执行
  ↓
创建 SpringApplication 实例
  ↓
推断应用类型
  ↓
加载 ApplicationListener 和 ApplicationContextInitializer
  ↓
发布 ApplicationStartingEvent
  ↓
准备 Environment
  ↓
发布 ApplicationEnvironmentPreparedEvent
  ↓
创建 ApplicationContext
  ↓
发布 ApplicationContextInitializedEvent
  ↓
加载 BeanDefinition
  ↓
发布 ApplicationPreparedEvent
  ↓
刷新 ApplicationContext
  ↓
初始化 Bean
  ↓
发布 ApplicationStartedEvent
  ↓
执行 ApplicationRunner 和 CommandLineRunner
  ↓
发布 ApplicationReadyEvent
  ↓
应用启动完成
```

从开发角度看，可以将启动流程拆分为四个阶段：

| 阶段             | 说明                                | 典型扩展点                                                   |
| ---------------- | ----------------------------------- | ------------------------------------------------------------ |
| 环境准备阶段     | 加载配置、环境变量、命令行参数      | `ApplicationEnvironmentPreparedEvent`                        |
| 容器创建阶段     | 创建并准备 `ApplicationContext`     | `ApplicationContextInitializer`                              |
| Bean 初始化阶段  | 创建 Bean、注入依赖、执行初始化回调 | `@PostConstruct`、`InitializingBean`                         |
| 应用启动完成阶段 | 执行 Runner，发布就绪事件           | `ApplicationRunner`、`CommandLineRunner`、`ApplicationReadyEvent` |

需要注意的是，越靠前的阶段，可使用的 Spring 能力越少。
例如在 `ApplicationStartingEvent` 阶段，Spring 容器尚未创建，不能直接通过依赖注入获取业务 Bean。
而在 `ApplicationReadyEvent` 阶段，应用已经完成启动流程，适合执行应用就绪后的业务动作。

### ApplicationContext 初始化阶段

`ApplicationContext` 初始化阶段是 Spring 容器启动的核心阶段，主要负责创建应用上下文、加载 Bean 定义、处理 Bean 工厂、注册后置处理器、实例化单例 Bean，并完成依赖注入和初始化回调。

该阶段的主要工作包括：

1. 创建具体类型的 `ApplicationContext`。
2. 设置 `Environment`、资源加载器、事件广播器等基础组件。
3. 加载配置类、自动配置类、组件扫描结果和 Bean 定义。
4. 执行 `BeanFactoryPostProcessor`，允许修改 Bean 定义。
5. 注册 `BeanPostProcessor`，参与 Bean 初始化前后的处理。
6. 实例化非懒加载单例 Bean。
7. 执行 Bean 初始化相关回调。

该阶段常见扩展点如下：

| 扩展点                          | 触发时机                            | 适用场景                 |
| ------------------------------- | ----------------------------------- | ------------------------ |
| `ApplicationContextInitializer` | `ApplicationContext` 创建后、刷新前 | 修改上下文环境或注册属性 |
| `BeanFactoryPostProcessor`      | Bean 实例化前                       | 修改 BeanDefinition      |
| `BeanPostProcessor`             | Bean 初始化前后                     | 增强 Bean 初始化过程     |
| `@PostConstruct`                | 依赖注入完成后                      | 单个 Bean 内部初始化     |
| `InitializingBean`              | 属性设置完成后                      | 单个 Bean 初始化逻辑     |
| `initMethod`                    | Bean 初始化阶段                     | 自定义初始化方法         |

该阶段适合处理轻量级、局部性的初始化逻辑，例如：

- 校验 Bean 必要属性；
- 初始化本地规则对象；
- 构建内存映射关系；
- 注册本地处理器；
- 初始化不依赖外部资源的组件。

不建议在该阶段执行以下逻辑：

- 大批量数据库查询；
- 长时间远程接口调用；
- Redis 或 MQ 大量数据预热；
- 阻塞式文件扫描；
- 非核心第三方资源检查；
- 耗时后台任务启动。

原因是该阶段仍然属于 Spring 容器刷新过程，如果任务执行过慢或发生异常，会直接影响应用启动。

### Bean 初始化完成阶段

Bean 初始化完成阶段指 Spring 容器已经完成大部分单例 Bean 的创建、依赖注入和初始化回调，此时应用上下文已经基本可用，但应用还不一定完全进入就绪状态。

该阶段适合执行依赖 Spring Bean 的启动逻辑，例如读取数据库配置、加载业务字典、初始化核心缓存、校验关键业务组件等。

常见扩展点如下：

| 扩展点                       | 执行时机                           | 适用场景                           |
| ---------------------------- | ---------------------------------- | ---------------------------------- |
| `SmartInitializingSingleton` | 所有非懒加载单例 Bean 初始化完成后 | 依赖多个单例 Bean 的初始化逻辑     |
| `ApplicationStartedEvent`    | 容器刷新完成后、Runner 执行前      | 记录启动状态、执行 Runner 前置逻辑 |
| `CommandLineRunner`          | 应用启动后执行                     | 获取原始命令行参数                 |
| `ApplicationRunner`          | 应用启动后执行                     | 使用结构化参数解析启动参数         |

该阶段可以安全访问大多数 Spring Bean，例如：

- `Service`
- `Mapper`
- `Repository`
- `RedisTemplate`
- `RedissonClient`
- `RabbitTemplate`
- `KafkaTemplate`
- 自定义配置类
- 自定义业务组件

但是该阶段仍然需要控制任务耗时。
如果任务属于核心初始化逻辑，可以同步执行；如果任务只是缓存预热、数据补充、外部通知等非核心逻辑，建议异步执行或移动到 `ApplicationReadyEvent` 阶段。

多个 Runner 之间存在顺序要求时，可以使用 `@Order` 控制执行顺序。数值越小，优先级越高。

```java
package io.github.atengk.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 系统配置启动加载任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(1)
@Component
public class SystemConfigRunner implements ApplicationRunner {

    /**
     * 执行系统配置加载
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("开始加载系统配置");
        log.info("系统配置加载完成");
    }

}
```

如果另一个任务依赖系统配置，则可以设置更大的顺序值。

```java
package io.github.atengk.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 字典数据启动加载任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(2)
@Component
public class DictDataRunner implements CommandLineRunner {

    /**
     * 执行字典数据加载
     *
     * @param args 原始启动参数
     */
    @Override
    public void run(String... args) {
        log.info("开始加载字典数据");
        log.info("字典数据加载完成");
    }

}
```

上述两个 Runner 的执行顺序为：

```text
SystemConfigRunner
  ↓
DictDataRunner
```

### 应用就绪阶段

应用就绪阶段表示 Spring Boot 已经完成容器刷新、Bean 初始化、Runner 执行和启动事件处理，应用可以认为已经具备对外提供服务的能力。

该阶段最常用的扩展点是 `ApplicationReadyEvent`。

`ApplicationReadyEvent` 适合处理应用启动完成后的补充逻辑，例如：

- 输出应用启动完成日志；
- 上报应用实例状态；
- 通知外部系统应用已上线；
- 触发非核心缓存预热；
- 执行异步健康检查；
- 启动非阻塞后台任务；
- 记录应用启动耗时。

需要注意的是，`ApplicationReadyEvent` 表示 Spring Boot 启动流程已经完成，但不代表所有业务数据都已经预热完成。
如果在该事件中执行同步耗时任务，仍然会拉长整体启动完成时间。因此，非核心任务建议使用线程池异步执行，并做好异常捕获和日志记录。

示例代码如下：

```java
package io.github.atengk.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用就绪事件监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class ApplicationReadyListener {

    /**
     * 监听应用就绪事件
     *
     * @param event 应用就绪事件
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        long startupTime = event.getTimeTaken() == null ? -1 : event.getTimeTaken().toMillis();
        log.info("应用启动完成，启动耗时：{} ms", startupTime);

        // 此处适合触发非核心初始化任务，例如异步缓存预热、状态上报等
        log.info("应用已进入就绪阶段，可以对外提供服务");
    }

}
```

应用启动生命周期中几个关键阶段的关系如下：

```text
ApplicationContext 初始化
  ↓
Bean 创建与初始化
  ↓
ApplicationStartedEvent
  ↓
ApplicationRunner / CommandLineRunner
  ↓
ApplicationReadyEvent
  ↓
应用对外提供服务
```

实际项目中推荐按照以下原则安排启动逻辑：

| 启动逻辑类型               | 推荐位置                                 |
| -------------------------- | ---------------------------------------- |
| 单个 Bean 内部轻量初始化   | `@PostConstruct`、`InitializingBean`     |
| 所有单例 Bean 初始化后处理 | `SmartInitializingSingleton`             |
| 依赖业务 Bean 的启动任务   | `ApplicationRunner`、`CommandLineRunner` |
| 应用就绪后的非核心任务     | `ApplicationReadyEvent`                  |
| 需要随容器启动和停止的组件 | `SmartLifecycle`                         |
| 启动失败处理               | `ApplicationFailedEvent`                 |

整体建议是：
Bean 自身初始化逻辑放在 Bean 生命周期回调中；核心启动任务放在 Runner 中；应用就绪后的补充任务放在 `ApplicationReadyEvent` 中；生命周期组件交给 `SmartLifecycle` 管理。



## 常用启动执行扩展点

Spring Boot 提供了多种启动执行扩展点，不同扩展点对应不同的生命周期阶段。开发启动任务时，应根据任务是否依赖 Spring Bean、是否需要等待容器初始化完成、是否允许阻塞启动流程、是否需要参与优雅停机等因素选择合适的实现方式。

常见启动执行扩展点可以分为四类：

| 类型            | 常用扩展点                                                   | 适用范围                         |
| --------------- | ------------------------------------------------------------ | -------------------------------- |
| Runner 启动任务 | `CommandLineRunner`、`ApplicationRunner`                     | 容器启动完成后执行业务初始化任务 |
| 启动事件监听    | `ApplicationStartedEvent`、`ApplicationReadyEvent`           | 监听应用启动过程中的关键事件     |
| Bean 初始化回调 | `@PostConstruct`、`InitializingBean`、`SmartInitializingSingleton` | Bean 创建和初始化阶段执行逻辑    |
| 生命周期组件    | `SmartLifecycle`                                             | 管理需要随容器启动和停止的组件   |

选择扩展点时，建议遵循以下原则：

1. 单个 Bean 内部的轻量初始化逻辑，优先使用 `@PostConstruct`。
2. 需要明确实现接口的 Bean 初始化逻辑，可以使用 `InitializingBean`。
3. 依赖所有单例 Bean 初始化完成后的逻辑，可以使用 `SmartInitializingSingleton`。
4. 应用启动完成后需要执行的业务初始化任务，可以使用 `ApplicationRunner` 或 `CommandLineRunner`。
5. 应用完全就绪后才执行的非核心任务，可以使用 `ApplicationReadyEvent`。
6. 需要随容器启动、停止并支持阶段控制的组件，可以使用 `SmartLifecycle`。

### CommandLineRunner

`CommandLineRunner` 是 Spring Boot 提供的启动任务接口，会在 Spring 容器启动完成后执行。它的核心特点是可以直接获取 `main` 方法传入的原始命令行参数。

接口定义如下：

```java
@FunctionalInterface
public interface CommandLineRunner {

    void run(String... args) throws Exception;

}
```

`CommandLineRunner` 适合处理简单启动参数、基础初始化任务、启动后检查任务等场景。它接收的是原始字符串数组，不会对参数进行结构化解析。

例如应用使用以下命令启动：

```bash
java -jar app.jar --init-cache=true --env=prod user-sync
```

在 `CommandLineRunner` 中获取到的参数大致为：

```text
--init-cache=true
--env=prod
user-sync
```

文件位置：`src/main/java/io/github/atengk/runner/StartupCommandLineRunner.java`

该类用于演示如何在应用启动后读取原始命令行参数并执行初始化逻辑。

```java
package io.github.atengk.runner;

import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 命令行参数启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(1)
@Component
public class StartupCommandLineRunner implements CommandLineRunner {

    /**
     * 执行启动任务
     *
     * @param args 原始命令行参数
     */
    @Override
    public void run(String... args) {
        log.info("CommandLineRunner 开始执行");

        if (ArrayUtil.isEmpty(args)) {
            log.info("未检测到命令行参数，跳过参数处理");
            return;
        }

        Arrays.stream(args).forEach(arg -> log.info("启动参数：{}", arg));

        log.info("CommandLineRunner 执行完成");
    }

}
```

`CommandLineRunner` 的优势是简单直接，适合快速读取原始启动参数。
但如果启动参数较多，或者需要区分选项参数和非选项参数，建议使用 `ApplicationRunner`。

适用场景：

| 场景               | 是否推荐       |
| ------------------ | -------------- |
| 打印启动参数       | 推荐           |
| 简单开关判断       | 推荐           |
| 复杂参数解析       | 不推荐         |
| 数据库配置加载     | 可以使用       |
| 大量缓存预热       | 谨慎使用       |
| 长时间远程接口调用 | 不推荐同步执行 |

使用注意事项：

1. `CommandLineRunner` 会影响应用启动完成时间，耗时任务应谨慎放入。
2. 多个 `CommandLineRunner` 可通过 `@Order` 控制执行顺序。
3. 如果 `run` 方法抛出异常，默认会导致应用启动失败。
4. 原始参数需要开发者自行解析，参数复杂时不够方便。
5. 不建议在 Runner 中编写大量分散业务逻辑，应按职责拆分多个启动任务类。

### ApplicationRunner

`ApplicationRunner` 也是 Spring Boot 提供的启动任务接口，执行时机与 `CommandLineRunner` 类似，都会在 Spring 容器启动完成后执行。

区别在于，`ApplicationRunner` 接收的是 `ApplicationArguments` 对象，可以更方便地解析命令行参数。

接口定义如下：

```java
@FunctionalInterface
public interface ApplicationRunner {

    void run(ApplicationArguments args) throws Exception;

}
```

`ApplicationArguments` 会将启动参数拆分为选项参数和非选项参数。

例如应用使用以下命令启动：

```bash
java -jar app.jar --init-cache=true --env=prod user-sync
```

解析结果大致如下：

| 参数类型     | 示例                |
| ------------ | ------------------- |
| 选项参数名称 | `init-cache`、`env` |
| 选项参数值   | `true`、`prod`      |
| 非选项参数   | `user-sync`         |

文件位置：`src/main/java/io/github/atengk/runner/StartupApplicationRunner.java`

该类用于演示如何使用 `ApplicationArguments` 解析启动参数，并根据参数决定是否执行缓存初始化。

```java
package io.github.atengk.runner;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用参数启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(2)
@Component
public class StartupApplicationRunner implements ApplicationRunner {

    /**
     * 执行启动任务
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("ApplicationRunner 开始执行");

        boolean initCache = args.containsOption("init-cache");
        String env = getFirstOptionValue(args, "env", "default");

        log.info("当前启动环境：{}", env);

        if (!initCache) {
            log.info("未启用缓存初始化参数，跳过缓存初始化");
            return;
        }

        log.info("检测到缓存初始化参数，开始执行缓存初始化");
        log.info("缓存初始化完成");
    }

    /**
     * 获取第一个选项参数值
     *
     * @param args         应用启动参数
     * @param optionName   参数名称
     * @param defaultValue 默认值
     * @return 参数值
     */
    private String getFirstOptionValue(ApplicationArguments args, String optionName, String defaultValue) {
        List<String> values = args.getOptionValues(optionName);
        if (CollUtil.isEmpty(values)) {
            return defaultValue;
        }

        String value = values.get(0);
        return StrUtil.blankToDefault(value, defaultValue);
    }

}
```

`ApplicationRunner` 更适合实际业务项目，因为它可以清晰区分参数名称、参数值和普通参数，代码可读性更好。

`ApplicationRunner` 与 `CommandLineRunner` 对比如下：

| 对比项   | CommandLineRunner  | ApplicationRunner      |
| -------- | ------------------ | ---------------------- |
| 参数类型 | `String... args`   | `ApplicationArguments` |
| 参数解析 | 手动解析           | Spring Boot 已解析     |
| 适合场景 | 简单参数处理       | 结构化参数处理         |
| 执行时机 | 容器启动完成后     | 容器启动完成后         |
| 顺序控制 | 支持 `@Order`      | 支持 `@Order`          |
| 异常影响 | 异常会导致启动失败 | 异常会导致启动失败     |

使用建议：

1. 新项目中优先使用 `ApplicationRunner`。
2. 参数逻辑简单时可以使用 `CommandLineRunner`。
3. 多个 Runner 之间需要明确顺序时，必须使用 `@Order` 或 `Ordered`。
4. Runner 中不应直接写复杂业务逻辑，应调用独立的 Service 或启动任务组件。
5. 非核心任务异常应自行捕获，避免影响应用启动。

### ApplicationReadyEvent

`ApplicationReadyEvent` 是 Spring Boot 应用完全启动完成后发布的事件。它表示应用已经完成容器刷新、Runner 执行和启动流程，可以认为已经进入就绪状态。

该事件通常用于执行应用启动完成后的补充动作，例如状态上报、非核心缓存预热、启动耗时记录、异步任务触发等。

`ApplicationReadyEvent` 的执行时机位于启动流程后段：

```text
ApplicationStartedEvent
  ↓
CommandLineRunner / ApplicationRunner
  ↓
ApplicationReadyEvent
  ↓
应用就绪
```

文件位置：`src/main/java/io/github/atengk/listener/ApplicationReadyListener.java`

该类用于监听应用就绪事件，并在应用启动完成后触发非核心初始化逻辑。

```java
package io.github.atengk.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 应用就绪事件监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationReadyListener {

    private final ThreadPoolTaskExecutor applicationTaskExecutor;

    /**
     * 监听应用就绪事件
     *
     * @param event 应用就绪事件
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        long startupTime = event.getTimeTaken() == null ? -1 : event.getTimeTaken().toMillis();
        log.info("应用已就绪，启动耗时：{} ms", startupTime);

        applicationTaskExecutor.execute(() -> {
            try {
                log.info("开始执行应用就绪后的非核心初始化任务");
                // 示例：缓存预热、状态上报、异步资源检查
                log.info("应用就绪后的非核心初始化任务执行完成");
            } catch (Exception e) {
                log.error("应用就绪后的非核心初始化任务执行异常", e);
            }
        });
    }

}
```

如果使用上述示例中的 `ThreadPoolTaskExecutor`，需要提供线程池配置。

文件位置：`src/main/java/io/github/atengk/config/AsyncExecutorConfig.java`

该配置类用于提供应用启动后异步任务使用的线程池。

```java
package io.github.atengk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class AsyncExecutorConfig {

    /**
     * 创建应用任务线程池
     *
     * @return 应用任务线程池
     */
    @Bean
    public ThreadPoolTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("application-task-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}
```

`ApplicationReadyEvent` 适用场景：

| 场景             | 说明                                 |
| ---------------- | ------------------------------------ |
| 应用启动完成日志 | 记录启动耗时、端口、环境信息         |
| 非核心缓存预热   | 建议异步执行，失败不影响启动         |
| 外部系统通知     | 例如通知监控平台、业务网关、注册中心 |
| 异步资源检查     | 检查第三方服务可用性                 |
| 延迟初始化任务   | 避免阻塞主启动流程                   |

使用注意事项：

1. `ApplicationReadyEvent` 是应用成功启动后的事件，启动失败时不会触发。
2. 不建议在该事件中同步执行长时间任务。
3. 非核心任务应捕获异常，避免影响事件监听链路。
4. 多个监听器之间可以使用 `@Order` 控制顺序。
5. 如果任务必须在对外提供服务之前完成，不应放在 `ApplicationReadyEvent` 异步执行。

### ApplicationStartedEvent

`ApplicationStartedEvent` 是 Spring Boot 在应用上下文刷新完成后、Runner 执行前发布的事件。此时 Spring 容器已经完成初始化，Bean 已经可用，但 `CommandLineRunner` 和 `ApplicationRunner` 尚未执行。

执行顺序如下：

```text
ApplicationContext refresh 完成
  ↓
ApplicationStartedEvent
  ↓
CommandLineRunner / ApplicationRunner
  ↓
ApplicationReadyEvent
```

`ApplicationStartedEvent` 适合在 Runner 执行前记录状态、准备上下文、检查基础环境，或者统计容器刷新完成时间。

文件位置：`src/main/java/io/github/atengk/listener/ApplicationStartedListener.java`

该类用于监听应用已启动事件，在 Runner 执行前输出启动阶段日志。

```java
package io.github.atengk.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用已启动事件监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class ApplicationStartedListener {

    /**
     * 监听应用已启动事件
     *
     * @param event 应用已启动事件
     */
    @EventListener
    public void onApplicationStarted(ApplicationStartedEvent event) {
        long startupTime = event.getTimeTaken() == null ? -1 : event.getTimeTaken().toMillis();
        log.info("应用上下文已刷新完成，Runner 即将执行，当前耗时：{} ms", startupTime);
    }

}
```

`ApplicationStartedEvent` 与 `ApplicationReadyEvent` 的区别如下：

| 对比项             | ApplicationStartedEvent | ApplicationReadyEvent |
| ------------------ | ----------------------- | --------------------- |
| 触发时机           | Runner 执行前           | Runner 执行后         |
| 容器状态           | 已刷新完成              | 已完全就绪            |
| 是否可访问 Bean    | 可以                    | 可以                  |
| 是否适合业务初始化 | 适合少量前置处理        | 适合就绪后补充处理    |
| 是否表示应用可用   | 不完全表示              | 基本表示应用可用      |

使用建议：

1. 需要在 Runner 前执行的逻辑，可以使用 `ApplicationStartedEvent`。
2. 只记录启动阶段状态时，可以使用该事件。
3. 不建议放置复杂业务初始化逻辑。
4. 如果任务依赖 Runner 执行结果，应使用 `ApplicationReadyEvent`。
5. 如果任务失败必须阻止启动，可以在事件监听中抛出异常，但生产环境应谨慎使用。

### InitializingBean

`InitializingBean` 是 Spring 提供的 Bean 生命周期接口。当 Bean 完成属性注入后，Spring 会调用其 `afterPropertiesSet()` 方法。

接口定义如下：

```java
public interface InitializingBean {

    void afterPropertiesSet() throws Exception;

}
```

`InitializingBean` 的执行时机早于 Runner 和应用就绪事件，属于 Bean 初始化阶段。它适合处理单个 Bean 内部的初始化逻辑，例如校验必要属性、构建本地缓存结构、初始化策略映射等。

文件位置：`src/main/java/io/github/atengk/component/RuleRegistry.java`

该类用于演示在 Bean 属性注入完成后初始化本地规则映射。

```java
package io.github.atengk.component;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 规则注册组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class RuleRegistry implements InitializingBean {

    private final Map<String, String> ruleMap = new HashMap<>();

    /**
     * Bean 属性设置完成后执行初始化
     */
    @Override
    public void afterPropertiesSet() {
        log.info("开始初始化本地规则注册表");

        ruleMap.put("USER_CREATE", "用户创建规则");
        ruleMap.put("ORDER_PAY", "订单支付规则");
        ruleMap.put("STOCK_DEDUCT", "库存扣减规则");

        if (MapUtil.isEmpty(ruleMap)) {
            throw new IllegalStateException("本地规则注册表初始化失败");
        }

        log.info("本地规则注册表初始化完成，规则数量：{}", ruleMap.size());
    }

    /**
     * 根据规则编码获取规则名称
     *
     * @param ruleCode 规则编码
     * @return 规则名称
     */
    public String getRuleName(String ruleCode) {
        return ruleMap.get(ruleCode);
    }

}
```

`InitializingBean` 适用场景：

| 场景               | 是否推荐 |
| ------------------ | -------- |
| 校验 Bean 必要属性 | 推荐     |
| 初始化本地 Map     | 推荐     |
| 构建策略映射       | 推荐     |
| 查询大量数据库数据 | 不推荐   |
| 调用远程服务       | 不推荐   |
| 启动异步后台任务   | 不推荐   |

使用注意事项：

1. `afterPropertiesSet()` 抛出异常会导致应用启动失败。
2. 不建议在该方法中执行耗时任务。
3. 不建议访问尚未完全初始化好的复杂上下文。
4. 该接口会让业务类与 Spring 生命周期接口产生耦合。
5. 如果只是简单初始化，也可以使用 `@PostConstruct`。

### @PostConstruct

`@PostConstruct` 是 Jakarta 注解提供的 Bean 初始化回调方式。Spring Boot 3 中应使用 `jakarta.annotation.PostConstruct`。

它会在 Bean 完成依赖注入后执行，通常用于单个 Bean 内部的轻量初始化逻辑。

Spring Boot 3 中正确导入方式如下：

```java
import jakarta.annotation.PostConstruct;
```

不应继续使用：

```java
import javax.annotation.PostConstruct;
```

文件位置：`src/main/java/io/github/atengk/component/LocalCacheHolder.java`

该类用于演示在 Bean 初始化完成后加载本地静态缓存。

```java
package io.github.atengk.component;

import cn.hutool.core.map.MapUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地缓存持有组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class LocalCacheHolder {

    private final Map<String, String> localCache = new ConcurrentHashMap<>();

    /**
     * 初始化本地缓存
     */
    @PostConstruct
    public void init() {
        log.info("开始初始化本地缓存");

        localCache.put("system_name", "Spring Boot 启动执行机制示例");
        localCache.put("system_version", "1.0.0");

        if (MapUtil.isEmpty(localCache)) {
            throw new IllegalStateException("本地缓存初始化失败");
        }

        log.info("本地缓存初始化完成，缓存数量：{}", localCache.size());
    }

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值
     */
    public String get(String key) {
        return localCache.get(key);
    }

}
```

`@PostConstruct` 与 `InitializingBean` 的区别如下：

| 对比项      | @PostConstruct | InitializingBean             |
| ----------- | -------------- | ---------------------------- |
| 来源        | Jakarta 注解   | Spring 接口                  |
| 侵入性      | 低             | 较高                         |
| 方法名称    | 自定义         | 固定为 `afterPropertiesSet`  |
| 适用场景    | 简单初始化     | 需要明确生命周期接口的初始化 |
| Spring 耦合 | 较低           | 较高                         |

使用建议：

1. 单个 Bean 内部轻量初始化，优先使用 `@PostConstruct`。
2. 初始化逻辑应尽量短小，不要包含复杂业务流程。
3. 不建议在 `@PostConstruct` 中查询大量数据库数据。
4. 不建议在 `@PostConstruct` 中执行远程接口调用。
5. 初始化失败确实应阻止启动时，才抛出异常。

### SmartInitializingSingleton

`SmartInitializingSingleton` 是 Spring 提供的扩展接口，会在所有非懒加载单例 Bean 初始化完成后执行。

接口定义如下：

```java
public interface SmartInitializingSingleton {

    void afterSingletonsInstantiated();

}
```

它与 `@PostConstruct`、`InitializingBean` 的区别在于，`@PostConstruct` 和 `InitializingBean` 关注单个 Bean 自身初始化，而 `SmartInitializingSingleton` 关注所有单例 Bean 初始化完成后的统一处理。

执行位置大致如下：

```text
单个 Bean 实例化
  ↓
依赖注入
  ↓
@PostConstruct
  ↓
InitializingBean
  ↓
所有非懒加载单例 Bean 初始化完成
  ↓
SmartInitializingSingleton
```

`SmartInitializingSingleton` 适合处理依赖多个 Bean 的初始化逻辑，例如收集所有策略实现类、构建处理器路由表、校验业务组件完整性等。

文件位置：`src/main/java/io/github/atengk/component/StrategyRegistry.java`

该类用于在所有单例 Bean 初始化完成后，收集所有策略实现并构建策略映射。

```java
package io.github.atengk.component;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略注册中心
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyRegistry implements SmartInitializingSingleton {

    private final List<StartupStrategy> startupStrategies;

    private final Map<String, StartupStrategy> strategyMap = new ConcurrentHashMap<>();

    /**
     * 所有非懒加载单例 Bean 初始化完成后执行
     */
    @Override
    public void afterSingletonsInstantiated() {
        log.info("开始注册启动策略");

        if (CollUtil.isEmpty(startupStrategies)) {
            log.warn("未发现启动策略实现，跳过策略注册");
            return;
        }

        for (StartupStrategy strategy : startupStrategies) {
            strategyMap.put(strategy.getStrategyCode(), strategy);
            log.info("启动策略注册完成，策略编码：{}", strategy.getStrategyCode());
        }

        log.info("启动策略全部注册完成，策略数量：{}", strategyMap.size());
    }

    /**
     * 获取启动策略
     *
     * @param strategyCode 策略编码
     * @return 启动策略
     */
    public StartupStrategy getStrategy(String strategyCode) {
        return strategyMap.get(strategyCode);
    }

}
```

文件位置：`src/main/java/io/github/atengk/component/StartupStrategy.java`

该接口用于定义启动策略的统一规范。

```java
package io.github.atengk.component;

/**
 * 启动策略接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface StartupStrategy {

    /**
     * 获取策略编码
     *
     * @return 策略编码
     */
    String getStrategyCode();

    /**
     * 执行策略
     */
    void execute();

}
```

`SmartInitializingSingleton` 适用场景：

| 场景                   | 是否推荐                          |
| ---------------------- | --------------------------------- |
| 收集所有接口实现类     | 推荐                              |
| 构建策略路由表         | 推荐                              |
| 检查多个 Bean 是否完整 | 推荐                              |
| 单个 Bean 属性初始化   | 不推荐                            |
| 应用就绪后异步任务     | 不推荐                            |
| 需要参与停机流程       | 不推荐，建议使用 `SmartLifecycle` |

使用注意事项：

1. 只会在非懒加载单例 Bean 初始化完成后触发。
2. 不适合处理懒加载 Bean 的完整性检查。
3. 不建议执行耗时外部调用。
4. 抛出异常会影响应用启动。
5. 适合做容器内组件整理，不适合做完整业务启动流程。

### SmartLifecycle

`SmartLifecycle` 是 Spring 提供的高级生命周期接口，用于管理需要随容器启动和停止的组件。它不仅可以在容器启动时执行逻辑，还可以在容器关闭时执行停止逻辑，并且支持自动启动、阶段顺序和优雅停机。

`SmartLifecycle` 常用于以下场景：

- 消息消费者启动与停止；
- 长连接客户端启动与关闭；
- 后台监听线程启动与关闭；
- 自定义任务调度器管理；
- 需要优雅停机的资源组件；
- 需要控制启动和停止顺序的组件。

核心方法如下：

| 方法                      | 说明                   |
| ------------------------- | ---------------------- |
| `start()`                 | 启动组件               |
| `stop()`                  | 停止组件               |
| `stop(Runnable callback)` | 停止组件并在完成后回调 |
| `isRunning()`             | 判断组件是否运行中     |
| `isAutoStartup()`         | 是否随容器自动启动     |
| `getPhase()`              | 控制启动和停止顺序     |

`SmartLifecycle` 的阶段顺序规则如下：

| 操作 | phase 规则           |
| ---- | -------------------- |
| 启动 | `phase` 越小越早启动 |
| 停止 | `phase` 越大越早停止 |

文件位置：`src/main/java/io/github/atengk/lifecycle/MessageConsumerLifecycle.java`

该类用于演示一个随 Spring 容器启动和停止的消息消费组件。

```java
package io.github.atengk.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 消息消费生命周期组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class MessageConsumerLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 启动生命周期组件
     */
    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.info("消息消费组件已启动，跳过重复启动");
            return;
        }

        log.info("消息消费组件开始启动");
        // 示例：启动消费者、注册监听器、建立长连接
        log.info("消息消费组件启动完成");
    }

    /**
     * 停止生命周期组件
     */
    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            log.info("消息消费组件未运行，跳过停止");
            return;
        }

        log.info("消息消费组件开始停止");
        // 示例：停止消费者、关闭连接、释放资源
        log.info("消息消费组件停止完成");
    }

    /**
     * 优雅停止生命周期组件
     *
     * @param callback 停止完成回调
     */
    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    /**
     * 判断组件是否正在运行
     *
     * @return 是否运行中
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 是否随容器自动启动
     *
     * @return 是否自动启动
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 获取生命周期阶段
     *
     * @return 阶段值
     */
    @Override
    public int getPhase() {
        return 100;
    }

}
```

`SmartLifecycle` 与 Runner、事件监听器的区别如下：

| 对比项           | SmartLifecycle         | Runner         | ApplicationReadyEvent |
| ---------------- | ---------------------- | -------------- | --------------------- |
| 是否支持启动     | 支持                   | 支持一次性执行 | 支持一次性监听        |
| 是否支持停止     | 支持                   | 不支持         | 不支持                |
| 是否支持优雅停机 | 支持                   | 不支持         | 不支持                |
| 是否支持阶段顺序 | 支持 `getPhase()`      | 支持 `@Order`  | 支持 `@Order`         |
| 适合长期组件     | 适合                   | 不适合         | 不适合                |
| 典型场景         | 消费者、监听器、长连接 | 初始化任务     | 就绪后通知或异步任务  |

使用建议：

1. 需要随容器启动和停止的组件，优先使用 `SmartLifecycle`。
2. 只执行一次的启动初始化任务，不建议使用 `SmartLifecycle`。
3. `start()` 方法中不建议执行长时间阻塞逻辑。
4. `stop(Runnable callback)` 中必须调用 `callback.run()`，否则可能影响容器关闭流程。
5. 使用 `getPhase()` 管理多个生命周期组件的启停顺序。
6. 组件内部应维护运行状态，避免重复启动或重复停止。
7. 涉及线程、连接、消费者、监听器时，应在停止阶段释放资源。

常用扩展点的整体选择建议如下：

| 扩展点                       | 执行阶段                           | 推荐用途                       |
| ---------------------------- | ---------------------------------- | ------------------------------ |
| `@PostConstruct`             | 单个 Bean 依赖注入完成后           | Bean 内部轻量初始化            |
| `InitializingBean`           | Bean 属性设置完成后                | Bean 初始化校验或本地结构构建  |
| `SmartInitializingSingleton` | 所有非懒加载单例 Bean 初始化完成后 | 收集多个 Bean、构建策略注册表  |
| `ApplicationStartedEvent`    | 容器刷新完成后、Runner 前          | Runner 前置状态记录            |
| `CommandLineRunner`          | 容器启动完成后                     | 原始命令行参数处理             |
| `ApplicationRunner`          | 容器启动完成后                     | 结构化启动参数处理、业务初始化 |
| `ApplicationReadyEvent`      | Runner 执行完成后                  | 应用就绪后补充任务             |
| `SmartLifecycle`             | 容器生命周期阶段                   | 长生命周期组件启动和停止管理   |



## CommandLineRunner 开发

`CommandLineRunner` 是 Spring Boot 提供的启动后执行接口，会在 Spring 容器初始化完成后执行。它适合处理简单启动任务，例如读取原始启动参数、执行基础数据加载、打印启动状态、触发一次性初始化逻辑等。

`CommandLineRunner` 的执行时机位于 `ApplicationStartedEvent` 之后、`ApplicationReadyEvent` 之前。如果 `run` 方法执行耗时较长，会延长应用进入就绪状态的时间；如果 `run` 方法抛出异常，默认会导致应用启动失败。

### 基本使用方式

使用 `CommandLineRunner` 时，只需要定义一个 Spring Bean，并实现 `run(String... args)` 方法。该方法会在应用启动过程中自动执行。

文件位置：`src/main/java/io/github/atengk/runner/SystemInitCommandLineRunner.java`

该类用于在应用启动后执行系统基础初始化逻辑。

```java
package io.github.atengk.runner;

import cn.hutool.core.date.StopWatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 系统初始化命令行启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(1)
@Component
public class SystemInitCommandLineRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        StopWatch stopWatch = new StopWatch("系统初始化启动任务");
        stopWatch.start("加载系统基础配置");

        log.info("开始执行系统初始化启动任务");

        // 示例：加载系统配置、初始化本地资源、检查基础状态
        loadSystemConfig();

        stopWatch.stop();
        log.info("系统初始化启动任务执行完成，耗时：{} ms", stopWatch.getTotalTimeMillis());
    }

    private void loadSystemConfig() {
        log.info("系统基础配置加载完成");
    }

}
```

该方式适合执行启动时必须完成的轻量任务。实际项目中不建议在 `run` 方法中直接编写复杂业务逻辑，而应将具体逻辑封装到独立的 Service 或启动任务组件中，Runner 只负责调度。

更推荐的写法如下：

文件位置：`src/main/java/io/github/atengk/runner/SystemConfigLoadRunner.java`

该类负责调用独立 Service 完成系统配置加载，避免 Runner 类承载过多业务逻辑。

```java
package io.github.atengk.runner;

import io.github.atengk.service.SystemConfigLoadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 系统配置加载启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class SystemConfigLoadRunner implements CommandLineRunner {

    private final SystemConfigLoadService systemConfigLoadService;

    @Override
    public void run(String... args) {
        log.info("开始执行系统配置加载启动任务");
        systemConfigLoadService.loadConfig();
        log.info("系统配置加载启动任务执行完成");
    }

}
```

文件位置：`src/main/java/io/github/atengk/service/SystemConfigLoadService.java`

该接口用于定义系统配置加载能力。

```java
package io.github.atengk.service;

/**
 * 系统配置加载服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface SystemConfigLoadService {

    void loadConfig();

}
```

文件位置：`src/main/java/io/github/atengk/service/impl/SystemConfigLoadServiceImpl.java`

该实现类用于封装具体的系统配置加载逻辑。

```java
package io.github.atengk.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import io.github.atengk.service.SystemConfigLoadService;

/**
 * 系统配置加载服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class SystemConfigLoadServiceImpl implements SystemConfigLoadService {

    @Override
    public void loadConfig() {
        log.info("开始加载系统配置");

        // 示例：从数据库、配置中心或本地文件加载系统配置
        // 实际项目中应根据业务场景实现

        log.info("系统配置加载完成");
    }

}
```

### 启动参数获取

`CommandLineRunner` 获取的是原始命令行参数，参数不会被 Spring Boot 结构化解析。所有参数都会以字符串数组形式传入 `run(String... args)` 方法。

例如应用启动命令如下：

```bash
java -jar app.jar --init-cache=true --env=prod --tenant-id=10001 sync-user
```

在 `CommandLineRunner` 中获取到的参数为：

```text
--init-cache=true
--env=prod
--tenant-id=10001
sync-user
```

如果需要解析这些参数，可以通过 Hutool 工具类进行基础判断和字符串处理。

文件位置：`src/main/java/io/github/atengk/runner/CommandArgsParseRunner.java`

该类用于演示如何解析 `CommandLineRunner` 中的原始启动参数。

```java
package io.github.atengk.runner;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 命令行参数解析启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(2)
@Component
public class CommandArgsParseRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("开始解析命令行启动参数");

        if (ArrayUtil.isEmpty(args)) {
            log.info("未传入命令行启动参数");
            return;
        }

        Map<String, String> optionMap = parseOptionArgs(args);

        String env = optionMap.getOrDefault("env", "default");
        String tenantId = optionMap.getOrDefault("tenant-id", "default");
        boolean initCache = Boolean.parseBoolean(optionMap.getOrDefault("init-cache", "false"));

        log.info("启动环境：{}", env);
        log.info("租户编号：{}", tenantId);
        log.info("是否初始化缓存：{}", initCache);

        if (initCache) {
            log.info("检测到 init-cache=true，开始执行缓存初始化");
            // 示例：执行缓存初始化
            log.info("缓存初始化完成");
        }
    }

    private Map<String, String> parseOptionArgs(String[] args) {
        Map<String, String> optionMap = new HashMap<>();

        for (String arg : args) {
            if (!StrUtil.startWith(arg, "--") || !StrUtil.contains(arg, "=")) {
                log.info("检测到非选项参数：{}", arg);
                continue;
            }

            String option = StrUtil.removePrefix(arg, "--");
            String key = StrUtil.subBefore(option, "=", false);
            String value = StrUtil.subAfter(option, "=", false);
            optionMap.put(key, value);
        }

        return optionMap;
    }

}
```

这种方式适合简单参数解析。如果参数较多、需要判断是否包含某个选项、需要处理多值参数，建议使用 `ApplicationRunner`，因为 `ApplicationArguments` 已经提供了结构化解析能力。

### 多个 Runner 执行顺序

一个 Spring Boot 应用中可以同时定义多个 `CommandLineRunner`。当多个 Runner 存在依赖关系时，必须明确执行顺序，否则不同任务之间可能出现数据未加载、缓存未初始化、配置未准备等问题。

控制 Runner 执行顺序的常用方式有两种：

| 方式           | 说明                                       |
| -------------- | ------------------------------------------ |
| `@Order`       | 使用注解设置顺序，简单直观                 |
| `Ordered` 接口 | 通过代码返回顺序值，适合需要动态控制的场景 |

顺序规则是：数值越小，优先级越高，越早执行。

文件位置：`src/main/java/io/github/atengk/runner/BaseConfigRunner.java`

该类用于优先加载基础配置。

```java
package io.github.atengk.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 基础配置启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(1)
@Component
public class BaseConfigRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("第 1 阶段：加载基础配置");
    }

}
```

文件位置：`src/main/java/io/github/atengk/runner/DictionaryDataRunner.java`

该类用于在基础配置加载完成后加载字典数据。

```java
package io.github.atengk.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 字典数据启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(2)
@Component
public class DictionaryDataRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("第 2 阶段：加载字典数据");
    }

}
```

文件位置：`src/main/java/io/github/atengk/runner/CacheWarmUpRunner.java`

该类用于在配置和字典数据加载完成后执行缓存预热。

```java
package io.github.atengk.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 缓存预热启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class CacheWarmUpRunner implements CommandLineRunner, Ordered {

    @Override
    public void run(String... args) {
        log.info("第 3 阶段：执行缓存预热");
    }

    @Override
    public int getOrder() {
        return 3;
    }

}
```

执行顺序如下：

```text
BaseConfigRunner
  ↓
DictionaryDataRunner
  ↓
CacheWarmUpRunner
```

使用多个 Runner 时，应避免通过类名、Bean 注册顺序或文件顺序推断执行顺序。只要任务之间存在依赖，就应显式声明顺序。

### 异常处理策略

`CommandLineRunner` 的 `run` 方法允许抛出异常。如果异常未被捕获，Spring Boot 会认为应用启动失败，并终止启动流程。

异常处理策略应根据任务重要性决定：

| 任务类型   | 推荐策略           | 示例                   |
| ---------- | ------------------ | ---------------------- |
| 核心任务   | 失败后中断启动     | 数据库核心配置加载失败 |
| 非核心任务 | 捕获异常并降级     | 非核心缓存预热失败     |
| 可恢复任务 | 记录日志并重试     | 第三方资源短暂不可用   |
| 异步任务   | 在线程内部捕获异常 | 启动后通知外部系统     |

核心任务失败时，可以直接抛出异常，阻止应用启动。

文件位置：`src/main/java/io/github/atengk/runner/CoreConfigCheckRunner.java`

该类用于演示核心配置检查失败时中断应用启动。

```java
package io.github.atengk.runner;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 核心配置检查启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(1)
@Component
public class CoreConfigCheckRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("开始检查核心配置");

        String datasourceUrl = System.getProperty("spring.datasource.url");
        if (StrUtil.isBlank(datasourceUrl)) {
            log.error("核心配置检查失败，数据源地址为空");
            throw new IllegalStateException("核心配置检查失败：数据源地址为空");
        }

        log.info("核心配置检查通过");
    }

}
```

非核心任务失败时，建议捕获异常并记录日志，不要影响应用启动。

文件位置：`src/main/java/io/github/atengk/runner/NonCoreCacheRunner.java`

该类用于演示非核心缓存预热失败时降级处理。

```java
package io.github.atengk.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 非核心缓存预热启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(10)
@Component
public class NonCoreCacheRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("开始执行非核心缓存预热");

        try {
            warmUpCache();
            log.info("非核心缓存预热完成");
        } catch (Exception e) {
            log.error("非核心缓存预热失败，已降级处理，不影响应用启动", e);
        }
    }

    private void warmUpCache() {
        // 示例：执行非核心缓存加载
    }

}
```

实际开发中，推荐将启动任务按重要性分级：

1. **强依赖任务**：失败后中断启动，例如数据源、核心配置、许可证、必要密钥检查。
2. **弱依赖任务**：失败后记录日志并降级，例如非核心缓存、统计数据预热。
3. **外部通知任务**：失败后告警或重试，例如应用上线通知、状态上报。
4. **耗时任务**：异步执行，并在线程内部捕获异常。

## ApplicationRunner 开发

`ApplicationRunner` 与 `CommandLineRunner` 的执行时机基本一致，都会在 Spring 容器初始化完成后执行。不同之处在于，`ApplicationRunner` 接收的是 `ApplicationArguments` 对象，可以更方便地解析启动参数。

在实际项目中，如果启动任务需要读取命令行参数，通常更推荐使用 `ApplicationRunner`，因为它能够区分选项参数和非选项参数，代码可读性更好。

### 基本使用方式

定义 `ApplicationRunner` 只需要实现 `run(ApplicationArguments args)` 方法，并将实现类交给 Spring 容器管理。

文件位置：`src/main/java/io/github/atengk/runner/SystemApplicationRunner.java`

该类用于演示 `ApplicationRunner` 的基础使用方式。

```java
package io.github.atengk.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 系统应用启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(1)
@Component
public class SystemApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        log.info("ApplicationRunner 开始执行");
        log.info("是否存在启动参数 init-cache：{}", args.containsOption("init-cache"));
        log.info("ApplicationRunner 执行完成");
    }

}
```

启动命令示例：

```bash
java -jar app.jar --init-cache=true --env=prod
```

启动后可以通过日志确认 `ApplicationRunner` 是否执行，以及参数是否被正确识别。

### ApplicationArguments 参数解析

`ApplicationArguments` 会将启动参数解析为两类：

| 参数类型       | 说明               | 示例                              |
| -------------- | ------------------ | --------------------------------- |
| Option 参数    | 以 `--` 开头的参数 | `--env=prod`、`--init-cache=true` |
| NonOption 参数 | 普通参数           | `sync-user`、`repair-data`        |

常用方法如下：

| 方法                           | 说明                     |
| ------------------------------ | ------------------------ |
| `containsOption(String name)`  | 判断是否包含指定选项参数 |
| `getOptionNames()`             | 获取所有选项参数名称     |
| `getOptionValues(String name)` | 获取指定选项参数值       |
| `getNonOptionArgs()`           | 获取非选项参数           |
| `getSourceArgs()`              | 获取原始启动参数         |

文件位置：`src/main/java/io/github/atengk/runner/ApplicationArgsParseRunner.java`

该类用于演示 `ApplicationArguments` 的常用参数解析方式。

```java
package io.github.atengk.runner;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 应用启动参数解析任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(2)
@Component
public class ApplicationArgsParseRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始解析 ApplicationArguments 启动参数");

        Set<String> optionNames = args.getOptionNames();
        List<String> nonOptionArgs = args.getNonOptionArgs();

        log.info("选项参数名称：{}", optionNames);
        log.info("非选项参数：{}", nonOptionArgs);

        String env = getFirstOptionValue(args, "env", "default");
        String tenantId = getFirstOptionValue(args, "tenant-id", "default");
        boolean initCache = Boolean.parseBoolean(getFirstOptionValue(args, "init-cache", "false"));

        log.info("启动环境：{}", env);
        log.info("租户编号：{}", tenantId);
        log.info("是否初始化缓存：{}", initCache);

        if (initCache) {
            log.info("检测到缓存初始化参数，开始执行缓存初始化");
            // 示例：调用缓存初始化服务
            log.info("缓存初始化完成");
        }
    }

    private String getFirstOptionValue(ApplicationArguments args, String optionName, String defaultValue) {
        List<String> values = args.getOptionValues(optionName);
        if (CollUtil.isEmpty(values)) {
            return defaultValue;
        }

        return StrUtil.blankToDefault(values.get(0), defaultValue);
    }

}
```

启动命令示例：

```bash
java -jar app.jar --env=prod --tenant-id=10001 --init-cache=true sync-user
```

解析结果示例：

```text
选项参数名称：[env, tenant-id, init-cache]
非选项参数：[sync-user]
启动环境：prod
租户编号：10001
是否初始化缓存：true
```

### 与 CommandLineRunner 的区别

`ApplicationRunner` 和 `CommandLineRunner` 的执行时机基本相同，主要区别在于参数接收方式不同。

| 对比项           | CommandLineRunner        | ApplicationRunner                  |
| ---------------- | ------------------------ | ---------------------------------- |
| 方法参数         | `String... args`         | `ApplicationArguments args`        |
| 参数结构         | 原始字符串数组           | 结构化参数对象                     |
| 参数解析         | 需要手动解析             | Spring Boot 自动解析               |
| 非选项参数       | 需要手动区分             | 直接通过 `getNonOptionArgs()` 获取 |
| 是否适合复杂参数 | 一般                     | 更适合                             |
| 执行顺序控制     | 支持 `@Order`、`Ordered` | 支持 `@Order`、`Ordered`           |
| 异常影响         | 未捕获异常会导致启动失败 | 未捕获异常会导致启动失败           |

选择建议如下：

1. 只需要获取原始启动参数时，可以使用 `CommandLineRunner`。
2. 需要解析 `--key=value` 形式的参数时，优先使用 `ApplicationRunner`。
3. 两者都适合应用启动后的业务初始化任务。
4. 两者都不适合执行长时间阻塞任务。
5. 两者可以共存，但应通过 `@Order` 明确执行顺序。
6. 新项目中更推荐优先使用 `ApplicationRunner`。

如果两种 Runner 同时存在，Spring Boot 会按照统一的排序规则执行。排序值越小，越早执行。

```text
@Order(1) ApplicationRunner
  ↓
@Order(2) CommandLineRunner
  ↓
@Order(3) ApplicationRunner
```

### 典型业务场景

`ApplicationRunner` 在业务项目中更常用于需要读取结构化启动参数的初始化任务。常见场景包括按环境加载配置、按租户初始化数据、根据启动参数触发修复任务、控制是否执行缓存预热等。

场景一：根据启动参数控制是否加载缓存。

文件位置：`src/main/java/io/github/atengk/runner/CacheInitApplicationRunner.java`

该类用于根据 `--cache-init=true` 参数控制缓存初始化。

```java
package io.github.atengk.runner;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 缓存初始化应用启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(5)
@Component
public class CacheInitApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        String cacheInitValue = getFirstValue(args, "cache-init", "false");
        boolean cacheInit = Boolean.parseBoolean(cacheInitValue);

        if (!cacheInit) {
            log.info("未启用缓存初始化，跳过缓存加载");
            return;
        }

        log.info("开始执行缓存初始化");
        // 示例：加载 Redis 缓存、本地缓存、热点数据
        log.info("缓存初始化完成");
    }

    private String getFirstValue(ApplicationArguments args, String optionName, String defaultValue) {
        List<String> values = args.getOptionValues(optionName);
        if (values == null || values.isEmpty()) {
            return defaultValue;
        }

        return StrUtil.blankToDefault(values.get(0), defaultValue);
    }

}
```

启动命令：

```bash
java -jar app.jar --cache-init=true
```

场景二：根据启动参数触发一次性数据修复任务。

文件位置：`src/main/java/io/github/atengk/runner/DataRepairApplicationRunner.java`

该类用于根据 `--repair-task=user-status` 参数执行指定数据修复任务。

```java
package io.github.atengk.runner;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据修复应用启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(20)
@Component
public class DataRepairApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        String repairTask = getFirstValue(args, "repair-task", "");

        if (StrUtil.isBlank(repairTask)) {
            log.info("未指定数据修复任务，跳过修复流程");
            return;
        }

        log.info("检测到数据修复任务：{}", repairTask);

        if (StrUtil.equals(repairTask, "user-status")) {
            repairUserStatus();
            return;
        }

        log.warn("未知数据修复任务：{}，已跳过", repairTask);
    }

    private void repairUserStatus() {
        log.info("开始修复用户状态数据");
        // 示例：调用 Service 执行一次性数据修复
        log.info("用户状态数据修复完成");
    }

    private String getFirstValue(ApplicationArguments args, String optionName, String defaultValue) {
        List<String> values = args.getOptionValues(optionName);
        if (values == null || values.isEmpty()) {
            return defaultValue;
        }

        return StrUtil.blankToDefault(values.get(0), defaultValue);
    }

}
```

启动命令：

```bash
java -jar app.jar --repair-task=user-status
```

这类一次性任务需要特别注意幂等性。生产环境中执行数据修复、补偿、迁移类 Runner 时，应具备重复执行保护、执行日志、影响范围控制和回滚方案。

## SmartLifecycle 开发

`SmartLifecycle` 是 Spring 提供的生命周期管理接口，适合管理需要跟随 Spring 容器启动和停止的组件。与 Runner 不同，Runner 更适合一次性启动任务，而 `SmartLifecycle` 更适合长生命周期组件，例如消息消费者、监听器、长连接客户端、后台工作线程等。

`SmartLifecycle` 的优势在于，它不仅支持启动阶段执行逻辑，还支持停止阶段释放资源，并可以通过 `phase` 控制多个生命周期组件的启停顺序。

### 生命周期接口说明

`SmartLifecycle` 继承自 `Lifecycle` 和 `Phased`，核心方法如下：

| 方法                      | 作用                             |
| ------------------------- | -------------------------------- |
| `start()`                 | 启动组件                         |
| `stop()`                  | 停止组件                         |
| `stop(Runnable callback)` | 优雅停止组件，停止完成后执行回调 |
| `isRunning()`             | 判断组件是否运行中               |
| `isAutoStartup()`         | 判断是否随容器自动启动           |
| `getPhase()`              | 获取生命周期阶段值               |

生命周期启动顺序规则如下：

| 操作 | 顺序规则               |
| ---- | ---------------------- |
| 启动 | `phase` 越小，越早启动 |
| 停止 | `phase` 越大，越早停止 |

例如存在三个生命周期组件：

```text
组件 A：phase = 0
组件 B：phase = 100
组件 C：phase = 200
```

启动顺序为：

```text
A → B → C
```

停止顺序为：

```text
C → B → A
```

这种机制适合处理有依赖关系的组件。例如基础连接组件应先启动，业务消费组件后启动；停止时业务消费组件应先停止，基础连接组件最后释放。

### start 与 stop 方法

`start()` 方法用于启动组件，`stop()` 方法用于停止组件。实现时应使用线程安全状态标识，避免重复启动或重复停止。

文件位置：`src/main/java/io/github/atengk/lifecycle/BusinessConsumerLifecycle.java`

该类用于演示业务消费组件如何跟随 Spring 容器启动和停止。

```java
package io.github.atengk.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 业务消费生命周期组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class BusinessConsumerLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.info("业务消费组件已处于运行状态，跳过启动");
            return;
        }

        log.info("业务消费组件开始启动");

        // 示例：启动消息消费者、注册监听器、创建后台工作线程
        startConsumer();

        log.info("业务消费组件启动完成");
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            log.info("业务消费组件未运行，跳过停止");
            return;
        }

        log.info("业务消费组件开始停止");

        // 示例：停止消费、关闭线程、释放连接
        stopConsumer();

        log.info("业务消费组件停止完成");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void startConsumer() {
        log.info("业务消费者已启动");
    }

    private void stopConsumer() {
        log.info("业务消费者已停止");
    }

}
```

实现 `start()` 和 `stop()` 时需要注意：

1. `start()` 不应长时间阻塞，否则会影响容器启动。
2. `stop()` 应释放组件持有的线程、连接、监听器和缓冲资源。
3. 内部应维护运行状态，避免重复启动或重复停止。
4. 启动失败时应记录清晰日志，核心组件可以抛出异常。
5. 停止逻辑应尽量容错，避免关闭流程被单个组件阻断。

### isAutoStartup 自动启动控制

`isAutoStartup()` 用于控制组件是否随 Spring 容器自动启动。

返回 `true` 时，容器启动过程中会自动调用 `start()`。
返回 `false` 时，容器不会自动启动该组件，需要开发者手动调用 `start()`。

文件位置：`src/main/java/io/github/atengk/lifecycle/ManualJobLifecycle.java`

该类用于演示需要手动启动的生命周期组件。

```java
package io.github.atengk.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 手动任务生命周期组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class ManualJobLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.info("手动任务组件已启动，跳过重复启动");
            return;
        }

        log.info("手动任务组件启动完成");
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            log.info("手动任务组件未运行，跳过停止");
            return;
        }

        log.info("手动任务组件停止完成");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

}
```

如果需要手动启动，可以在业务代码中注入该组件并调用 `start()`。不过实际项目中应谨慎暴露手动启动能力，避免组件状态被随意改变。

适合设置 `isAutoStartup()` 为 `false` 的场景包括：

| 场景                   | 说明                         |
| ---------------------- | ---------------------------- |
| 管理后台手动启停的组件 | 需要由管理员控制             |
| 灰度开关控制的消费者   | 启动后根据配置决定是否开启   |
| 调试组件               | 仅开发或测试环境手动启用     |
| 延迟启动组件           | 等待其他业务条件满足后再启动 |

多数生产组件建议保持自动启动，并通过配置项控制是否创建 Bean，而不是频繁手动调用 `start()`。

### getPhase 启停顺序控制

`getPhase()` 用于控制多个生命周期组件的启动和停止顺序。它返回一个整数，默认值通常可以理解为 `0`。

规则如下：

| 操作 | 规则                   |
| ---- | ---------------------- |
| 启动 | `phase` 越小，越早启动 |
| 停止 | `phase` 越大，越早停止 |

例如，一个系统中有连接管理组件、消费者组件和指标上报组件，可以按如下顺序设计：

| 组件         | phase | 启动顺序    | 停止顺序    |
| ------------ | ----- | ----------- | ----------- |
| 连接管理组件 | `0`   | 第 1 个启动 | 第 3 个停止 |
| 消费者组件   | `100` | 第 2 个启动 | 第 2 个停止 |
| 指标上报组件 | `200` | 第 3 个启动 | 第 1 个停止 |

文件位置：`src/main/java/io/github/atengk/lifecycle/ConnectionLifecycle.java`

该类用于模拟基础连接组件，优先启动，最后停止。

```java
package io.github.atengk.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基础连接生命周期组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class ConnectionLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("基础连接组件启动完成");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("基础连接组件停止完成");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 0;
    }

}
```

文件位置：`src/main/java/io/github/atengk/lifecycle/ConsumerLifecycle.java`

该类用于模拟业务消费者组件，在基础连接组件之后启动。

```java
package io.github.atengk.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 消费者生命周期组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class ConsumerLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("消费者组件启动完成");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("消费者组件停止完成");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 100;
    }

}
```

文件位置：`src/main/java/io/github/atengk/lifecycle/MetricsReportLifecycle.java`

该类用于模拟指标上报组件，最后启动，最先停止。

```java
package io.github.atengk.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 指标上报生命周期组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class MetricsReportLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("指标上报组件启动完成");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("指标上报组件停止完成");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 200;
    }

}
```

启动日志顺序大致如下：

```text
基础连接组件启动完成
消费者组件启动完成
指标上报组件启动完成
```

停止日志顺序大致如下：

```text
指标上报组件停止完成
消费者组件停止完成
基础连接组件停止完成
```

### 优雅停机处理

`SmartLifecycle` 提供了 `stop(Runnable callback)` 方法，用于支持优雅停机。容器关闭时会调用该方法，并等待组件执行停止逻辑。停止完成后必须调用 `callback.run()`，否则可能影响容器关闭流程。

文件位置：`src/main/java/io/github/atengk/lifecycle/GracefulConsumerLifecycle.java`

该类用于演示支持优雅停机的消费者生命周期组件。

```java
package io.github.atengk.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 优雅停机消费者生命周期组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class GracefulConsumerLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    private ExecutorService executorService;

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.info("优雅停机消费者已运行，跳过启动");
            return;
        }

        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::consume);

        log.info("优雅停机消费者启动完成");
    }

    @Override
    public void stop() {
        shutdownExecutor();
    }

    @Override
    public void stop(Runnable callback) {
        try {
            log.info("开始执行优雅停机消费者关闭流程");
            shutdownExecutor();
            log.info("优雅停机消费者关闭流程执行完成");
        } finally {
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return 100;
    }

    private void consume() {
        while (running.get()) {
            try {
                log.info("消费者正在处理业务消息");
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("消费者线程被中断，准备退出");
                break;
            } catch (Exception e) {
                log.error("消费者处理业务消息异常", e);
            }
        }

        log.info("消费者线程已退出");
    }

    private void shutdownExecutor() {
        if (!running.compareAndSet(true, false)) {
            log.info("优雅停机消费者未运行，跳过关闭");
            return;
        }

        if (executorService == null) {
            log.info("线程池未初始化，跳过关闭");
            return;
        }

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("消费者线程池未在指定时间内停止，执行强制关闭");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待消费者线程池停止时被中断，执行强制关闭");
            executorService.shutdownNow();
        }
    }

}
```

优雅停机处理建议：

1. `stop(Runnable callback)` 中必须调用 `callback.run()`。
2. 停止逻辑应设置超时时间，避免无限等待。
3. 关闭线程池时应先调用 `shutdown()`，超时后再调用 `shutdownNow()`。
4. 消费者应停止接收新任务，再处理或释放已有任务。
5. 对外部连接、监听器、文件句柄、网络资源应显式释放。
6. 停机异常应记录日志，但不应随意阻断关闭流程。
7. 多个组件之间存在依赖关系时，应通过 `getPhase()` 控制停止顺序。

`SmartLifecycle` 最适合管理长生命周期组件。如果只是应用启动时执行一次初始化逻辑，应优先使用 `ApplicationRunner`、`CommandLineRunner` 或 `ApplicationReadyEvent`，不要为了执行一次性任务而引入生命周期组件。



## 执行顺序与优先级

Spring Boot 启动过程中存在多类扩展点，包括 Bean 生命周期回调、Runner 启动任务、应用事件监听器和 `SmartLifecycle` 生命周期组件。不同扩展点的执行阶段不同，同一类扩展点内部也可以通过优先级控制执行顺序。

在实际项目中，只要启动任务之间存在依赖关系，就不应依赖类名顺序、Bean 扫描顺序或代码文件顺序，而应显式声明执行优先级。

常见执行顺序大致如下：

```text
ApplicationContext 创建
  ↓
BeanDefinition 加载
  ↓
Bean 实例化
  ↓
依赖注入
  ↓
@PostConstruct
  ↓
InitializingBean#afterPropertiesSet
  ↓
自定义 initMethod
  ↓
SmartInitializingSingleton#afterSingletonsInstantiated
  ↓
ApplicationStartedEvent
  ↓
CommandLineRunner / ApplicationRunner
  ↓
ApplicationReadyEvent
  ↓
应用就绪
```

需要注意的是，以上流程是简化后的主线顺序。不同自动配置、不同 Web 应用类型、不同监听器注册方式可能会影响细节，但不会改变主要阶段关系。

### Bean 初始化相关顺序

Bean 初始化相关顺序主要发生在 `ApplicationContext` 刷新阶段。该阶段负责创建 Bean、注入依赖、执行初始化回调和注册单例对象。

单个 Bean 的典型初始化顺序如下：

```text
实例化 Bean
  ↓
属性填充与依赖注入
  ↓
Aware 接口回调
  ↓
BeanPostProcessor 初始化前处理
  ↓
@PostConstruct
  ↓
InitializingBean#afterPropertiesSet
  ↓
自定义 initMethod
  ↓
BeanPostProcessor 初始化后处理
  ↓
Bean 可用
```

常见初始化方式的执行位置如下：

| 初始化方式                   | 执行时机                       | 适用场景                   |
| ---------------------------- | ------------------------------ | -------------------------- |
| 构造方法                     | Bean 实例化时                  | 初始化必要成员变量         |
| 依赖注入                     | 构造方法后                     | 注入其他 Bean 或配置对象   |
| `@PostConstruct`             | 依赖注入完成后                 | 单个 Bean 内部轻量初始化   |
| `InitializingBean`           | 属性设置完成后                 | 单个 Bean 初始化校验       |
| `initMethod`                 | `InitializingBean` 之后        | 自定义初始化方法           |
| `SmartInitializingSingleton` | 所有非懒加载单例 Bean 初始化后 | 依赖多个 Bean 的统一初始化 |

示例代码如下。

文件位置：`src/main/java/io/github/atengk/init/BeanInitOrderComponent.java`

该类用于观察单个 Bean 中不同初始化方式的执行顺序。

```java
package io.github.atengk.init;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Bean 初始化顺序观察组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class BeanInitOrderComponent implements InitializingBean {

    public BeanInitOrderComponent() {
        log.info("Bean 初始化顺序：1. 执行构造方法");
    }

    @PostConstruct
    public void postConstruct() {
        log.info("Bean 初始化顺序：2. 执行 @PostConstruct");
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Bean 初始化顺序：3. 执行 InitializingBean#afterPropertiesSet");
    }

}
```

如果需要使用自定义 `initMethod`，可以通过 `@Bean(initMethod = "...")` 配置。

文件位置：`src/main/java/io/github/atengk/config/BeanInitConfig.java`

该配置类用于演示自定义 `initMethod` 的执行位置。

```java
package io.github.atengk.config;

import io.github.atengk.init.CustomInitComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean 初始化配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class BeanInitConfig {

    /**
     * 创建自定义初始化组件
     *
     * @return 自定义初始化组件
     */
    @Bean(initMethod = "init")
    public CustomInitComponent customInitComponent() {
        return new CustomInitComponent();
    }

}
```

文件位置：`src/main/java/io/github/atengk/init/CustomInitComponent.java`

该类用于演示 `initMethod` 初始化方式。

```java
package io.github.atengk.init;

import lombok.extern.slf4j.Slf4j;

/**
 * 自定义初始化组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class CustomInitComponent {

    public void init() {
        log.info("Bean 初始化顺序：执行自定义 initMethod");
    }

}
```

Bean 初始化阶段的开发建议如下：

1. 单个 Bean 内部轻量初始化，优先使用 `@PostConstruct`。
2. 需要明确 Spring 生命周期接口时，可以使用 `InitializingBean`。
3. 多个 Bean 初始化完成后的统一处理，使用 `SmartInitializingSingleton`。
4. 不建议在 Bean 初始化阶段执行大量数据库查询、远程调用或阻塞任务。
5. Bean 初始化失败会影响应用启动，只有核心校验失败时才建议抛出异常。

### Runner 执行顺序

`CommandLineRunner` 和 `ApplicationRunner` 都属于应用启动后的 Runner 任务。它们会在 Spring 容器刷新完成后执行，执行位置位于 `ApplicationStartedEvent` 之后、`ApplicationReadyEvent` 之前。

Runner 执行顺序可以通过以下方式控制：

| 控制方式          | 说明                                               |
| ----------------- | -------------------------------------------------- |
| `@Order`          | 通过注解设置顺序                                   |
| `Ordered`         | 通过接口返回顺序值                                 |
| `PriorityOrdered` | 更高优先级的 Ordered 类型，较少直接用于业务 Runner |

排序规则是：数值越小，优先级越高，越早执行。

文件位置：`src/main/java/io/github/atengk/runner/FirstStartupRunner.java`

该类用于演示第一个执行的启动任务。

```java
package io.github.atengk.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 第一阶段启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(1)
@Component
public class FirstStartupRunner implements ApplicationRunner {

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        log.info("Runner 执行顺序：1. 第一阶段启动任务");
    }

}
```

文件位置：`src/main/java/io/github/atengk/runner/SecondStartupRunner.java`

该类用于演示第二个执行的启动任务。

```java
package io.github.atengk.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 第二阶段启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(2)
@Component
public class SecondStartupRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("Runner 执行顺序：2. 第二阶段启动任务");
    }

}
```

文件位置：`src/main/java/io/github/atengk/runner/ThirdStartupRunner.java`

该类用于演示通过 `Ordered` 接口控制执行顺序。

```java
package io.github.atengk.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 第三阶段启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class ThirdStartupRunner implements ApplicationRunner, Ordered {

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        log.info("Runner 执行顺序：3. 第三阶段启动任务");
    }

    @Override
    public int getOrder() {
        return 3;
    }

}
```

执行顺序如下：

```text
FirstStartupRunner
  ↓
SecondStartupRunner
  ↓
ThirdStartupRunner
```

Runner 顺序管理建议如下：

1. 有依赖关系的 Runner 必须显式声明顺序。
2. 不建议将所有初始化逻辑堆叠在一个 Runner 中。
3. 不建议使用过于紧密的顺序值，例如全部使用 `1`、`2`、`3`，可以按阶段使用 `100`、`200`、`300`。
4. 核心配置加载应早于缓存、字典、外部通知等任务。
5. 非核心任务应放在较后顺序，或者迁移到 `ApplicationReadyEvent` 异步执行。
6. 同一个顺序值下，多个 Runner 的相对顺序不应作为业务依赖。

推荐阶段顺序设计如下：

| 阶段     | 顺序值示例 | 任务类型       |
| -------- | ---------- | -------------- |
| 第一阶段 | `100`      | 核心配置加载   |
| 第二阶段 | `200`      | 字典数据加载   |
| 第三阶段 | `300`      | 核心缓存初始化 |
| 第四阶段 | `400`      | 资源连通性检查 |
| 第五阶段 | `500`      | 非核心启动任务 |

### 事件监听执行顺序

Spring 事件监听器可以通过 `@Order` 控制同一事件下多个监听器的执行顺序。排序规则同样是：数值越小，优先级越高，越早执行。

常见启动事件包括：

| 事件                                  | 触发时机                    |
| ------------------------------------- | --------------------------- |
| `ApplicationStartingEvent`            | 应用刚开始启动              |
| `ApplicationEnvironmentPreparedEvent` | 环境准备完成                |
| `ApplicationContextInitializedEvent`  | 上下文创建并初始化          |
| `ApplicationPreparedEvent`            | BeanDefinition 加载完成     |
| `ApplicationStartedEvent`             | 容器刷新完成，Runner 执行前 |
| `ApplicationReadyEvent`               | Runner 执行后，应用就绪     |
| `ApplicationFailedEvent`              | 应用启动失败                |

在 Spring 容器已经创建完成之后，可以使用 `@EventListener` 监听启动事件。

文件位置：`src/main/java/io/github/atengk/listener/FirstReadyListener.java`

该监听器用于在应用就绪后优先执行基础状态记录。

```java
package io.github.atengk.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.annotation.Order;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 第一应用就绪监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(1)
@Component
public class FirstReadyListener {

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("事件监听顺序：1. 记录应用基础就绪状态");
    }

}
```

文件位置：`src/main/java/io/github/atengk/listener/SecondReadyListener.java`

该监听器用于在基础状态记录后执行应用状态上报。

```java
package io.github.atengk.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.annotation.Order;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 第二应用就绪监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(2)
@Component
public class SecondReadyListener {

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("事件监听顺序：2. 上报应用就绪状态");
    }

}
```

如果要监听非常早期的事件，例如 `ApplicationStartingEvent` 和 `ApplicationEnvironmentPreparedEvent`，通常不能只使用 `@Component`，因为这些事件发生时 Spring 容器尚未完成创建。此时应在 `main` 方法中通过 `SpringApplication#addListeners` 注册。

文件位置：`src/main/java/io/github/atengk/DemoApplication.java`

该启动类用于注册早期启动事件监听器。

```java
package io.github.atengk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;

/**
 * Spring Boot 应用启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DemoApplication.class);

        application.addListeners(event -> {
            if (event instanceof ApplicationStartingEvent) {
                log.info("监听早期事件：ApplicationStartingEvent");
            }
            if (event instanceof ApplicationEnvironmentPreparedEvent) {
                log.info("监听早期事件：ApplicationEnvironmentPreparedEvent");
            }
        });

        application.run(args);
    }

}
```

事件监听顺序管理建议如下：

1. 同一事件存在多个监听器时，使用 `@Order` 明确顺序。
2. 不同事件之间，优先由事件生命周期决定顺序。
3. 早期事件不能依赖普通 Spring Bean 注入。
4. 就绪事件中不建议同步执行耗时任务。
5. 非核心监听逻辑应捕获异常，避免影响启动流程。
6. 需要异步执行时，应使用独立线程池，并在线程内部处理异常。

### SmartLifecycle 阶段顺序

`SmartLifecycle` 通过 `getPhase()` 控制生命周期组件的启动和停止顺序。它与 Runner 的 `@Order` 不同，`phase` 不只影响启动，还影响停机。

规则如下：

| 操作 | 规则                   |
| ---- | ---------------------- |
| 启动 | `phase` 越小，越早启动 |
| 停止 | `phase` 越大，越早停止 |

例如系统中存在三个组件：

| 组件           | phase | 启动顺序 | 停止顺序 |
| -------------- | ----- | -------- | -------- |
| 基础连接组件   | `0`   | 第 1 个  | 第 3 个  |
| 业务消费者组件 | `100` | 第 2 个  | 第 2 个  |
| 监控上报组件   | `200` | 第 3 个  | 第 1 个  |

示例代码如下。

文件位置：`src/main/java/io/github/atengk/lifecycle/BaseConnectionLifecycle.java`

该组件用于模拟基础连接，最先启动，最后停止。

```java
package io.github.atengk.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基础连接生命周期组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class BaseConnectionLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("SmartLifecycle 阶段顺序：1. 基础连接组件启动");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("SmartLifecycle 阶段顺序：3. 基础连接组件停止");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 0;
    }

}
```

文件位置：`src/main/java/io/github/atengk/lifecycle/BusinessConsumerLifecycle.java`

该组件用于模拟业务消费者，在基础连接之后启动，在基础连接之前停止。

```java
package io.github.atengk.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 业务消费者生命周期组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class BusinessConsumerLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("SmartLifecycle 阶段顺序：2. 业务消费者组件启动");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("SmartLifecycle 阶段顺序：2. 业务消费者组件停止");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 100;
    }

}
```

文件位置：`src/main/java/io/github/atengk/lifecycle/MetricsLifecycle.java`

该组件用于模拟监控上报，最后启动，最先停止。

```java
package io.github.atengk.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 监控上报生命周期组件
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class MetricsLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("SmartLifecycle 阶段顺序：3. 监控上报组件启动");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("SmartLifecycle 阶段顺序：1. 监控上报组件停止");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 200;
    }

}
```

`SmartLifecycle` 阶段顺序设计建议如下：

1. 基础资源组件使用较小 `phase`。
2. 依赖基础资源的业务组件使用较大 `phase`。
3. 监控、上报、清理类组件可以使用更大的 `phase`。
4. 停机时会反向执行，因此要确保业务组件先停止，基础资源后释放。
5. 同一 `phase` 下的组件不应存在强依赖关系。
6. 不建议随意使用极大或极小值，除非需要与框架内部组件协调。

## 启动任务实现场景

启动任务实现场景应围绕应用启动阶段真正需要完成的动作设计。并不是所有初始化逻辑都应该放到启动阶段，只有影响应用基础运行状态、请求处理能力或启动后立即需要使用的数据，才适合在启动过程中处理。

常见启动任务包括系统配置预加载、缓存初始化、字典数据加载、第三方资源连接检查和启动后异步任务触发。

### 系统配置预加载

系统配置预加载通常用于在应用启动后读取数据库、配置中心或本地配置文件中的业务参数，并将其放入内存缓存中，供后续业务逻辑快速访问。

适合预加载的配置包括：

| 配置类型     | 示例                             |
| ------------ | -------------------------------- |
| 系统基础参数 | 系统名称、默认时区、文件大小限制 |
| 业务开关     | 是否启用审核、是否启用风控       |
| 租户配置     | 租户状态、租户隔离规则           |
| 安全配置     | 登录失败次数、Token 过期时间     |
| 外部接口配置 | 第三方接口地址、超时时间         |

文件位置：`src/main/java/io/github/atengk/config/SystemConfigProperties.java`

该类用于定义示例系统配置属性。

```java
package io.github.atengk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "system")
public class SystemConfigProperties {

    /**
     * 系统名称
     */
    private String name;

    /**
     * 是否启用缓存预热
     */
    private Boolean cacheWarmUp;

    /**
     * 默认租户编号
     */
    private String defaultTenantId;

}
```

文件位置：`src/main/resources/application.yml`

该配置用于提供系统配置预加载示例参数。

```yaml
system:
  # 系统名称，用于启动日志和运行状态展示
  name: spring-boot-startup-demo
  # 是否启用缓存预热
  cache-warm-up: true
  # 默认租户编号
  default-tenant-id: default
```

文件位置：`src/main/java/io/github/atengk/holder/SystemConfigHolder.java`

该组件用于保存预加载后的系统配置。

```java
package io.github.atengk.holder;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置持有器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class SystemConfigHolder {

    private final Map<String, String> configMap = new ConcurrentHashMap<>();

    public void put(String key, String value) {
        configMap.put(key, value);
    }

    public String get(String key) {
        return configMap.get(key);
    }

    public boolean isEmpty() {
        return MapUtil.isEmpty(configMap);
    }

    public int size() {
        return configMap.size();
    }

    public void clear() {
        configMap.clear();
        log.info("系统配置缓存已清空");
    }

}
```

文件位置：`src/main/java/io/github/atengk/runner/SystemConfigPreloadRunner.java`

该启动任务用于在应用启动阶段预加载系统配置。

```java
package io.github.atengk.runner;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.config.SystemConfigProperties;
import io.github.atengk.holder.SystemConfigHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 系统配置预加载启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(100)
@Component
@RequiredArgsConstructor
public class SystemConfigPreloadRunner implements ApplicationRunner {

    private final SystemConfigProperties systemConfigProperties;
    private final SystemConfigHolder systemConfigHolder;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        log.info("开始预加载系统配置");

        String systemName = StrUtil.blankToDefault(systemConfigProperties.getName(), "unknown-system");
        String defaultTenantId = StrUtil.blankToDefault(systemConfigProperties.getDefaultTenantId(), "default");

        systemConfigHolder.put("system.name", systemName);
        systemConfigHolder.put("system.defaultTenantId", defaultTenantId);
        systemConfigHolder.put("system.cacheWarmUp", String.valueOf(Boolean.TRUE.equals(systemConfigProperties.getCacheWarmUp())));

        if (systemConfigHolder.isEmpty()) {
            throw new IllegalStateException("系统配置预加载失败：配置为空");
        }

        log.info("系统配置预加载完成，配置数量：{}", systemConfigHolder.size());
    }

}
```

系统配置预加载建议如下：

1. 核心配置缺失时可以中断启动。
2. 配置内容应支持刷新或重载，不应只依赖启动时加载。
3. 敏感配置不应直接打印到日志。
4. 多租户配置应考虑租户隔离。
5. 如果配置来自数据库，应控制查询范围，避免启动阶段加载过多数据。

### 缓存初始化

缓存初始化用于在应用启动时提前加载热点数据，减少首次请求时的查询压力。缓存可以是本地缓存，也可以是 Redis 缓存。

缓存初始化适合以下场景：

| 缓存类型      | 示例                           |
| ------------- | ------------------------------ |
| 本地缓存      | 系统枚举、规则映射、固定配置   |
| Redis 缓存    | 热点商品、区域数据、权限数据   |
| Caffeine 缓存 | 高频读取、低频变更的小规模数据 |
| 分布式缓存    | 多实例共享的业务基础数据       |

对于核心缓存，可以在 Runner 中同步初始化。对于非核心缓存，建议在 `ApplicationReadyEvent` 中异步初始化。

文件位置：`src/main/java/io/github/atengk/service/CacheWarmUpService.java`

该接口用于定义缓存预热能力。

```java
package io.github.atengk.service;

/**
 * 缓存预热服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface CacheWarmUpService {

    void warmUp();

}
```

文件位置：`src/main/java/io/github/atengk/service/impl/CacheWarmUpServiceImpl.java`

该实现类用于模拟缓存初始化逻辑。

```java
package io.github.atengk.service.impl;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import io.github.atengk.service.CacheWarmUpService;

import java.util.List;

/**
 * 缓存预热服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class CacheWarmUpServiceImpl implements CacheWarmUpService {

    @Override
    public void warmUp() {
        log.info("开始执行缓存预热");

        List<String> cacheKeys = List.of("system:config", "system:dict", "system:permission");
        if (CollUtil.isEmpty(cacheKeys)) {
            log.warn("缓存预热键为空，跳过缓存初始化");
            return;
        }

        for (String cacheKey : cacheKeys) {
            log.info("缓存预热完成，缓存键：{}", cacheKey);
        }

        log.info("缓存预热全部完成");
    }

}
```

文件位置：`src/main/java/io/github/atengk/listener/CacheWarmUpReadyListener.java`

该监听器用于在应用就绪后异步触发非核心缓存预热。

```java
package io.github.atengk.listener;

import io.github.atengk.service.CacheWarmUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 缓存预热就绪监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmUpReadyListener {

    private final CacheWarmUpService cacheWarmUpService;
    private final ThreadPoolTaskExecutor applicationTaskExecutor;

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("应用已就绪，准备异步执行缓存预热");

        applicationTaskExecutor.execute(() -> {
            try {
                cacheWarmUpService.warmUp();
            } catch (Exception e) {
                log.error("缓存预热执行失败，已降级处理", e);
            }
        });
    }

}
```

缓存初始化建议如下：

1. 核心缓存可以同步初始化，非核心缓存建议异步初始化。
2. 缓存初始化应具备幂等性，重复执行不应产生脏数据。
3. 缓存数据量较大时，应分页加载或分批加载。
4. Redis 缓存预热应设置合理过期时间。
5. 多实例部署时，应避免所有实例同时执行重型缓存预热。
6. 缓存预热失败时，应明确是否影响应用启动。

### 字典数据加载

字典数据加载用于将业务字典、枚举映射、状态码、区域数据等基础数据加载到内存或缓存中。字典数据通常读取频率高、变更频率低，适合在启动阶段加载。

常见字典类型包括：

| 字典类型     | 示例                         |
| ------------ | ---------------------------- |
| 业务状态字典 | 用户状态、订单状态、支付状态 |
| 枚举映射字典 | 性别、证件类型、账户类型     |
| 地区字典     | 国家、省份、城市、区域       |
| 系统参数字典 | 渠道编码、来源类型、终端类型 |
| 权限字典     | 角色类型、资源类型、操作类型 |

文件位置：`src/main/java/io/github/atengk/model/DictItem.java`

该类用于定义字典项数据结构。

```java
package io.github.atengk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典项
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItem {

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 字典编码
     */
    private String dictCode;

    /**
     * 字典名称
     */
    private String dictName;

}
```

文件位置：`src/main/java/io/github/atengk/holder/DictHolder.java`

该组件用于保存启动阶段加载的字典数据。

```java
package io.github.atengk.holder;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.model.DictItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 字典数据持有器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class DictHolder {

    private final Map<String, List<DictItem>> dictMap = new ConcurrentHashMap<>();

    public void load(List<DictItem> dictItems) {
        if (CollUtil.isEmpty(dictItems)) {
            log.warn("字典数据为空，跳过加载");
            return;
        }

        Map<String, List<DictItem>> groupedDict = dictItems.stream()
                .collect(Collectors.groupingBy(DictItem::getDictType));

        dictMap.clear();
        dictMap.putAll(groupedDict);

        log.info("字典数据加载完成，字典类型数量：{}", dictMap.size());
    }

    public List<DictItem> getByType(String dictType) {
        return dictMap.getOrDefault(dictType, List.of());
    }

    public int size() {
        return dictMap.size();
    }

}
```

文件位置：`src/main/java/io/github/atengk/runner/DictLoadRunner.java`

该启动任务用于在应用启动时加载字典数据。

```java
package io.github.atengk.runner;

import io.github.atengk.holder.DictHolder;
import io.github.atengk.model.DictItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典数据加载启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(200)
@Component
@RequiredArgsConstructor
public class DictLoadRunner implements ApplicationRunner {

    private final DictHolder dictHolder;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        log.info("开始加载字典数据");

        List<DictItem> dictItems = loadDictItems();
        dictHolder.load(dictItems);

        log.info("字典数据加载启动任务执行完成，字典类型数量：{}", dictHolder.size());
    }

    private List<DictItem> loadDictItems() {
        return List.of(
                new DictItem("user_status", "enabled", "启用"),
                new DictItem("user_status", "disabled", "禁用"),
                new DictItem("order_status", "created", "已创建"),
                new DictItem("order_status", "paid", "已支付")
        );
    }

}
```

字典数据加载建议如下：

1. 字典加载应早于依赖字典的业务缓存初始化。
2. 字典数据应支持后续刷新，避免只能通过重启更新。
3. 字典编码应稳定，不建议频繁变更。
4. 字典数据为空时，应区分正常空数据和加载失败。
5. 如果字典来源于数据库，应避免一次性加载过多无关字段。
6. 字典缓存建议按类型分组，便于业务查询。

### 第三方资源连接检查

第三方资源连接检查用于在应用启动阶段确认外部依赖是否可用，例如 Redis、消息队列、对象存储、搜索引擎、第三方 HTTP 服务等。

外部资源可以分为核心依赖和非核心依赖：

| 类型       | 示例                          | 失败策略           |
| ---------- | ----------------------------- | ------------------ |
| 核心依赖   | 主数据库、核心 Redis、核心 MQ | 可以中断启动       |
| 非核心依赖 | 短信服务、统计系统、通知平台  | 记录日志并降级     |
| 可恢复依赖 | 第三方接口、外部报表系统      | 重试或异步检查     |
| 延迟依赖   | 不影响启动的外部能力          | 应用就绪后异步检查 |

文件位置：`src/main/java/io/github/atengk/check/ThirdPartyResourceChecker.java`

该接口用于定义第三方资源检查规范。

```java
package io.github.atengk.check;

/**
 * 第三方资源检查器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface ThirdPartyResourceChecker {

    String resourceName();

    boolean core();

    void check();

}
```

文件位置：`src/main/java/io/github/atengk/check/RedisResourceChecker.java`

该检查器用于模拟核心 Redis 资源检查。

```java
package io.github.atengk.check;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Redis 资源检查器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class RedisResourceChecker implements ThirdPartyResourceChecker {

    @Override
    public String resourceName() {
        return "Redis";
    }

    @Override
    public boolean core() {
        return true;
    }

    @Override
    public void check() {
        log.info("Redis 资源连接检查通过");
    }

}
```

文件位置：`src/main/java/io/github/atengk/check/SmsResourceChecker.java`

该检查器用于模拟非核心短信服务资源检查。

```java
package io.github.atengk.check;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信服务资源检查器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class SmsResourceChecker implements ThirdPartyResourceChecker {

    @Override
    public String resourceName() {
        return "短信服务";
    }

    @Override
    public boolean core() {
        return false;
    }

    @Override
    public void check() {
        log.info("短信服务资源连接检查通过");
    }

}
```

文件位置：`src/main/java/io/github/atengk/runner/ThirdPartyResourceCheckRunner.java`

该启动任务用于统一检查第三方资源，核心资源失败时中断启动，非核心资源失败时降级处理。

```java
package io.github.atengk.runner;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.check.ThirdPartyResourceChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 第三方资源连接检查启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(400)
@Component
@RequiredArgsConstructor
public class ThirdPartyResourceCheckRunner implements ApplicationRunner {

    private final List<ThirdPartyResourceChecker> resourceCheckers;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        log.info("开始执行第三方资源连接检查");

        if (CollUtil.isEmpty(resourceCheckers)) {
            log.warn("未配置第三方资源检查器，跳过资源检查");
            return;
        }

        for (ThirdPartyResourceChecker checker : resourceCheckers) {
            checkResource(checker);
        }

        log.info("第三方资源连接检查执行完成");
    }

    private void checkResource(ThirdPartyResourceChecker checker) {
        try {
            checker.check();
            log.info("第三方资源检查成功，资源名称：{}", checker.resourceName());
        } catch (Exception e) {
            if (checker.core()) {
                log.error("核心第三方资源检查失败，应用启动中断，资源名称：{}", checker.resourceName(), e);
                throw e;
            }

            log.error("非核心第三方资源检查失败，已降级处理，资源名称：{}", checker.resourceName(), e);
        }
    }

}
```

第三方资源检查建议如下：

1. 核心资源失败时可以中断启动。
2. 非核心资源失败时应记录日志并降级，不建议阻断启动。
3. 外部接口检查应设置超时时间，避免启动过程长时间卡住。
4. 大量第三方资源检查不应全部串行阻塞执行。
5. 检查逻辑不应产生业务副作用，例如创建订单、扣减库存等。
6. 检查结果可上报到监控系统，便于生产环境排查。

### 启动后异步任务触发

启动后异步任务触发适合处理不要求在应用对外提供服务前完成的任务。此类任务通常放在 `ApplicationReadyEvent` 中，并通过线程池异步执行。

适合异步触发的任务包括：

| 任务类型         | 示例                     |
| ---------------- | ------------------------ |
| 非核心缓存预热   | 统计缓存、首页推荐缓存   |
| 外部状态通知     | 通知监控平台应用已上线   |
| 延迟资源检查     | 第三方接口健康检查       |
| 低优先级数据同步 | 同步外部系统基础数据     |
| 启动后补偿任务   | 扫描未完成任务并提交处理 |

异步任务必须使用受管理的线程池，不建议直接创建裸线程。线程池应配置线程名前缀、核心线程数、最大线程数、队列长度和拒绝策略。

文件位置：`src/main/java/io/github/atengk/config/StartupAsyncExecutorConfig.java`

该配置类用于提供启动后异步任务线程池。

```java
package io.github.atengk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 启动异步任务线程池配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class StartupAsyncExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor startupTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("startup-task-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}
```

文件位置：`src/main/java/io/github/atengk/service/StartupAsyncTaskService.java`

该接口用于定义启动后异步任务能力。

```java
package io.github.atengk.service;

/**
 * 启动异步任务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface StartupAsyncTaskService {

    void executeAfterReady();

}
```

文件位置：`src/main/java/io/github/atengk/service/impl/StartupAsyncTaskServiceImpl.java`

该实现类用于封装应用就绪后的异步任务逻辑。

```java
package io.github.atengk.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import io.github.atengk.service.StartupAsyncTaskService;

/**
 * 启动异步任务服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class StartupAsyncTaskServiceImpl implements StartupAsyncTaskService {

    @Override
    public void executeAfterReady() {
        log.info("开始执行启动后异步任务");

        // 示例：应用状态上报、非核心缓存预热、延迟资源检查
        reportApplicationStatus();
        preloadNonCoreCache();

        log.info("启动后异步任务执行完成");
    }

    private void reportApplicationStatus() {
        log.info("应用状态上报完成");
    }

    private void preloadNonCoreCache() {
        log.info("非核心缓存预热完成");
    }

}
```

文件位置：`src/main/java/io/github/atengk/listener/StartupAsyncTaskReadyListener.java`

该监听器用于在应用就绪后提交异步任务。

```java
package io.github.atengk.listener;

import io.github.atengk.service.StartupAsyncTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 启动后异步任务监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupAsyncTaskReadyListener {

    private final StartupAsyncTaskService startupAsyncTaskService;
    private final ThreadPoolTaskExecutor startupTaskExecutor;

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("应用已就绪，提交启动后异步任务");

        startupTaskExecutor.execute(() -> {
            try {
                startupAsyncTaskService.executeAfterReady();
            } catch (Exception e) {
                log.error("启动后异步任务执行失败，已记录异常并继续运行", e);
            }
        });
    }

}
```

启动后异步任务触发建议如下：

1. 必须在线程内部捕获异常，避免异常丢失或影响线程池。
2. 异步任务应具备幂等性，避免多实例部署时重复执行造成业务问题。
3. 不建议在异步任务中执行强依赖初始化，否则可能出现应用已对外服务但关键数据未准备好的问题。
4. 线程池参数应根据任务数量和资源消耗配置，不应无限制创建线程。
5. 异步任务应记录开始、完成、耗时和异常日志。
6. 生产环境中建议结合分布式锁，避免多实例同时执行同一重型任务。
7. 对外部系统调用应设置超时、重试和降级策略。

整体选择建议如下：

| 场景                 | 推荐扩展点                               | 是否阻塞启动     |
| -------------------- | ---------------------------------------- | ---------------- |
| 系统核心配置预加载   | `ApplicationRunner`                      | 可以阻塞         |
| 核心字典数据加载     | `ApplicationRunner`                      | 可以阻塞         |
| 核心缓存初始化       | `CommandLineRunner`、`ApplicationRunner` | 可以阻塞         |
| 非核心缓存预热       | `ApplicationReadyEvent` + 线程池         | 不建议阻塞       |
| 第三方核心资源检查   | `ApplicationRunner`                      | 可以阻塞         |
| 第三方非核心资源检查 | `ApplicationReadyEvent` + 线程池         | 不建议阻塞       |
| 后台消费者启动       | `SmartLifecycle`                         | 不建议长时间阻塞 |
| 启动后状态上报       | `ApplicationReadyEvent` + 线程池         | 不应阻塞         |



## 异步与阻塞控制

启动任务会直接影响 Spring Boot 应用的启动耗时。对于核心任务，可以在启动流程中同步执行；对于非核心任务，应尽量异步执行，避免阻塞主启动线程。启动任务设计不当时，容易导致应用长时间卡在启动阶段，甚至影响容器部署、健康检查和滚动发布。

在生产环境中，启动任务应重点控制三类风险：阻塞时间、线程资源、外部依赖超时。

### 启动任务阻塞风险

启动任务阻塞风险主要来自耗时操作和不可控外部依赖。`CommandLineRunner`、`ApplicationRunner`、`ApplicationReadyEvent` 同步监听器、`SmartLifecycle#start()` 等逻辑如果执行时间过长，都会影响应用启动过程。

常见阻塞风险如下：

| 风险类型          | 示例                               | 影响               |
| ----------------- | ---------------------------------- | ------------------ |
| 大批量数据库查询  | 启动时全量加载用户、订单、权限数据 | 启动耗时过长       |
| 远程接口无超时    | 调用第三方接口一直等待             | 应用卡死在启动阶段 |
| Redis/MQ 阻塞连接 | 外部中间件不可用但客户端长时间等待 | 启动失败或超时     |
| 同步缓存预热      | 一次性加载大量热点数据             | 容器就绪时间变长   |
| 启动任务串行执行  | 多个无依赖任务按顺序执行           | 总耗时累加         |
| 线程阻塞未释放    | 自定义线程、连接、任务未关闭       | 资源泄漏           |

不建议在启动阶段执行以下代码：

```java
Thread.sleep(60000);
```

不建议在启动阶段进行无超时远程调用：

```java
restTemplate.getForObject("https://example.com/api/check", String.class);
```

更合理的方式是：核心任务同步执行并设置超时，非核心任务在应用就绪后异步执行，外部依赖调用必须设置连接超时和读取超时。

启动任务可以按重要性分级：

| 任务级别     | 是否阻塞启动 | 示例                         |
| ------------ | ------------ | ---------------------------- |
| 核心任务     | 可以阻塞     | 核心配置加载、数据库连接检查 |
| 重要任务     | 短时间阻塞   | 字典加载、权限基础数据加载   |
| 非核心任务   | 不建议阻塞   | 首页缓存预热、统计数据加载   |
| 外部通知任务 | 不应阻塞     | 应用上线通知、监控状态上报   |

设计原则是：只有应用无法正常运行时，才允许启动任务阻塞启动流程。其他任务应异步化、降级化或延迟执行。

### 异步执行方案

启动阶段的异步执行通常有三种方式：`ApplicationReadyEvent` 配合线程池、`CompletableFuture` 配合自定义 Executor、`@Async` 配合 Spring 异步能力。

推荐优先使用 `ApplicationReadyEvent` 加受管理线程池。这样可以保证任务在应用就绪后触发，同时避免直接创建裸线程。

文件位置：`src/main/java/io/github/atengk/config/StartupTaskExecutorConfig.java`

该配置类用于定义启动后异步任务线程池。

```java
package io.github.atengk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 启动任务线程池配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class StartupTaskExecutorConfig {

    /**
     * 创建启动任务线程池
     *
     * @return 启动任务线程池
     */
    @Bean
    public ThreadPoolTaskExecutor startupTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("startup-task-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}
```

文件位置：`src/main/java/io/github/atengk/listener/StartupAsyncReadyListener.java`

该监听器用于在应用就绪后提交异步启动任务。

```java
package io.github.atengk.listener;

import cn.hutool.core.date.TimeInterval;
import io.github.atengk.service.StartupAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 启动异步任务监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupAsyncReadyListener {

    private final ThreadPoolTaskExecutor startupTaskExecutor;
    private final StartupAsyncService startupAsyncService;

    /**
     * 应用就绪后提交异步任务
     *
     * @param event 应用就绪事件
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("应用已就绪，准备提交启动后异步任务");

        startupTaskExecutor.execute(() -> {
            TimeInterval timer = new TimeInterval();
            try {
                startupAsyncService.execute();
                log.info("启动后异步任务执行完成，耗时：{} ms", timer.interval());
            } catch (Exception e) {
                log.error("启动后异步任务执行失败，已降级处理，耗时：{} ms", timer.interval(), e);
            }
        });
    }

}
```

文件位置：`src/main/java/io/github/atengk/service/StartupAsyncService.java`

该接口用于定义启动后异步任务。

```java
package io.github.atengk.service;

/**
 * 启动后异步任务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface StartupAsyncService {

    /**
     * 执行启动后异步任务
     */
    void execute();

}
```

文件位置：`src/main/java/io/github/atengk/service/impl/StartupAsyncServiceImpl.java`

该实现类用于封装启动后异步任务的具体业务逻辑。

```java
package io.github.atengk.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import io.github.atengk.service.StartupAsyncService;

/**
 * 启动后异步任务服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class StartupAsyncServiceImpl implements StartupAsyncService {

    /**
     * 执行启动后异步任务
     */
    @Override
    public void execute() {
        log.info("开始执行启动后异步任务");

        reportApplicationStatus();
        warmUpNonCoreCache();
        checkDelayResource();

        log.info("启动后异步任务全部完成");
    }

    private void reportApplicationStatus() {
        log.info("应用状态上报完成");
    }

    private void warmUpNonCoreCache() {
        log.info("非核心缓存预热完成");
    }

    private void checkDelayResource() {
        log.info("延迟资源检查完成");
    }

}
```

如果任务之间互不依赖，可以使用 `CompletableFuture` 并行执行，减少总耗时。

文件位置：`src/main/java/io/github/atengk/listener/StartupParallelReadyListener.java`

该监听器用于在应用就绪后并行执行多个非核心启动任务。

```java
package io.github.atengk.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 启动并行任务监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupParallelReadyListener {

    private final ThreadPoolTaskExecutor startupTaskExecutor;

    /**
     * 应用就绪后并行执行非核心任务
     *
     * @param event 应用就绪事件
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("应用已就绪，开始提交并行启动任务");

        CompletableFuture<Void> cacheTask = CompletableFuture.runAsync(this::warmUpCache, startupTaskExecutor);
        CompletableFuture<Void> reportTask = CompletableFuture.runAsync(this::reportStatus, startupTaskExecutor);
        CompletableFuture<Void> checkTask = CompletableFuture.runAsync(this::checkThirdParty, startupTaskExecutor);

        CompletableFuture.allOf(cacheTask, reportTask, checkTask)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("部分并行启动任务执行失败，已降级处理", throwable);
                        return;
                    }

                    log.info("并行启动任务全部执行完成");
                });
    }

    private void warmUpCache() {
        log.info("缓存预热完成");
    }

    private void reportStatus() {
        log.info("应用状态上报完成");
    }

    private void checkThirdParty() {
        log.info("第三方资源延迟检查完成");
    }

}
```

异步执行建议如下：

1. 不要直接 `new Thread()`，应使用受 Spring 管理的线程池。
2. 不要把核心强依赖任务异步化，否则应用可能在关键数据未准备好时对外提供服务。
3. 异步任务内部必须捕获异常并记录日志。
4. 多个任务之间无依赖时，可以并行执行。
5. 多实例部署时，应考虑分布式锁，避免多个实例重复执行重型任务。
6. 异步任务应具备幂等性，重复执行不应产生脏数据。

### 线程池配置

启动任务线程池需要根据任务类型、任务数量、执行耗时和机器资源进行配置。线程池参数过小会导致任务堆积，参数过大则可能抢占业务线程资源，影响应用正常服务。

常用参数如下：

| 参数                       | 说明             | 建议                    |
| -------------------------- | ---------------- | ----------------------- |
| `corePoolSize`             | 核心线程数       | 根据 CPU 和任务数量设置 |
| `maxPoolSize`              | 最大线程数       | 不宜过大，避免资源争抢  |
| `queueCapacity`            | 队列容量         | 防止瞬时任务过多        |
| `threadNamePrefix`         | 线程名前缀       | 便于日志排查            |
| `keepAliveSeconds`         | 空闲线程存活时间 | 根据任务频率设置        |
| `RejectedExecutionHandler` | 拒绝策略         | 根据任务重要性选择      |

推荐将线程池参数配置到 `application.yml`，便于不同环境调整。

文件位置：`src/main/resources/application.yml`

该配置用于定义启动任务线程池参数。

```yaml
startup:
  task:
    executor:
      # 核心线程数
      core-pool-size: 4
      # 最大线程数
      max-pool-size: 8
      # 队列容量
      queue-capacity: 200
      # 空闲线程存活时间，单位秒
      keep-alive-seconds: 60
      # 线程名前缀
      thread-name-prefix: startup-task-
```

文件位置：`src/main/java/io/github/atengk/config/StartupTaskExecutorProperties.java`

该配置属性类用于绑定启动任务线程池参数。

```java
package io.github.atengk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 启动任务线程池属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "startup.task.executor")
public class StartupTaskExecutorProperties {

    /**
     * 核心线程数
     */
    private Integer corePoolSize = 4;

    /**
     * 最大线程数
     */
    private Integer maxPoolSize = 8;

    /**
     * 队列容量
     */
    private Integer queueCapacity = 200;

    /**
     * 空闲线程存活时间
     */
    private Integer keepAliveSeconds = 60;

    /**
     * 线程名前缀
     */
    private String threadNamePrefix = "startup-task-";

}
```

文件位置：`src/main/java/io/github/atengk/config/StartupTaskExecutorConfig.java`

该配置类根据配置文件参数创建启动任务线程池。

```java
package io.github.atengk.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 启动任务线程池配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
@RequiredArgsConstructor
public class StartupTaskExecutorConfig {

    private final StartupTaskExecutorProperties properties;

    /**
     * 创建启动任务线程池
     *
     * @return 启动任务线程池
     */
    @Bean
    public ThreadPoolTaskExecutor startupTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}
```

线程池配置建议如下：

1. 启动任务线程池应与业务请求线程池隔离。
2. 线程名前缀必须明确，方便通过日志定位问题。
3. 非核心任务推荐使用有界队列，避免任务无限堆积。
4. 拒绝策略不建议使用静默丢弃，除非任务确实可丢弃。
5. 线程池参数应支持配置化，不应硬编码在代码中。
6. 对于 IO 密集型启动任务，线程数可以略高；对于 CPU 密集型任务，线程数不宜过高。

### 超时控制

启动任务访问外部资源时必须设置超时。没有超时控制的启动任务，在外部服务异常、网络抖动、DNS 异常或连接池耗尽时，可能导致应用长时间无法启动。

超时控制通常分为三类：

| 超时类型 | 说明                           |
| -------- | ------------------------------ |
| 连接超时 | 建立连接允许等待的最长时间     |
| 读取超时 | 请求发出后等待响应的最长时间   |
| 任务超时 | 整个启动任务允许执行的最长时间 |

对于 HTTP 调用，应在客户端层设置连接超时和读取超时。

文件位置：`src/main/java/io/github/atengk/config/RestClientConfig.java`

该配置类用于创建带超时控制的 `RestClient`。

```java
package io.github.atengk.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient 配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class RestClientConfig {

    /**
     * 创建带超时配置的 RestClient
     *
     * @return RestClient
     */
    @Bean
    public RestClient restClient() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

}
```

对于任意启动任务，可以使用 `CompletableFuture#orTimeout` 控制任务最长执行时间。

文件位置：`src/main/java/io/github/atengk/service/StartupTimeoutService.java`

该服务用于演示如何给启动任务增加整体超时控制。

```java
package io.github.atengk.service;

import cn.hutool.core.date.TimeInterval;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 启动任务超时控制服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StartupTimeoutService {

    private final ThreadPoolTaskExecutor startupTaskExecutor;

    /**
     * 执行带超时控制的启动任务
     */
    public void executeWithTimeout() {
        TimeInterval timer = new TimeInterval();

        CompletableFuture<Void> future = CompletableFuture
                .runAsync(this::doStartupTask, startupTaskExecutor)
                .orTimeout(10, TimeUnit.SECONDS);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("启动任务执行超时或失败，耗时：{} ms", timer.interval(), throwable);
                return;
            }

            log.info("启动任务在超时时间内执行完成，耗时：{} ms", timer.interval());
        });
    }

    private void doStartupTask() {
        log.info("开始执行带超时控制的启动任务");
        // 示例：资源检查、缓存预热、状态上报
        log.info("带超时控制的启动任务执行完成");
    }

}
```

超时控制建议如下：

1. 外部 HTTP 调用必须设置连接超时和读取超时。
2. 数据库、Redis、MQ 等客户端应配置合理连接超时。
3. 启动任务整体应设置最大执行时间。
4. 超时后应记录任务名称、耗时、资源名称和异常原因。
5. 核心任务超时可以中断启动，非核心任务超时应降级。
6. 超时任务要注意底层线程是否真正停止，避免后台任务继续占用资源。

## 异常处理与容错

启动阶段异常处理应遵循一个原则：核心任务失败可以阻止启动，非核心任务失败不应影响应用对外提供服务。所有启动任务都应明确失败策略，而不是简单地把异常吞掉或全部向外抛出。

异常处理策略应在开发阶段就确定，避免生产环境中出现启动失败原因不清晰、非核心依赖拖垮应用、异常日志缺失等问题。

### 启动失败策略

启动失败策略适用于核心依赖和强一致性任务。此类任务一旦失败，应用继续运行也无法正常处理请求，因此应直接中断启动。

适合中断启动的场景包括：

| 场景                 | 原因                       |
| -------------------- | -------------------------- |
| 核心数据库不可用     | 应用无法完成主要业务操作   |
| 必要配置缺失         | 应用运行状态不确定         |
| 加密密钥缺失         | 认证、签名、解密逻辑不可用 |
| License 校验失败     | 不允许继续运行             |
| 必要 Bean 初始化失败 | 核心组件不可用             |
| 数据结构不兼容       | 可能导致业务数据错误       |

文件位置：`src/main/java/io/github/atengk/runner/CoreStartupCheckRunner.java`

该启动任务用于检查核心启动条件，不满足时直接中断应用启动。

```java
package io.github.atengk.runner;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 核心启动条件检查任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(50)
@Component
public class CoreStartupCheckRunner implements ApplicationRunner {

    /**
     * 执行核心启动条件检查
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("开始检查核心启动条件");

        String secretKey = System.getenv("APP_SECRET_KEY");
        if (StrUtil.isBlank(secretKey)) {
            log.error("核心启动条件检查失败，环境变量 APP_SECRET_KEY 为空");
            throw new IllegalStateException("应用启动失败：必要环境变量 APP_SECRET_KEY 为空");
        }

        log.info("核心启动条件检查通过");
    }

}
```

启动失败策略建议如下：

1. 核心任务失败时应抛出明确异常。
2. 异常信息应包含任务名称和失败原因。
3. 不要在核心任务失败后继续静默启动。
4. 不要输出敏感配置明文，例如密钥、密码、Token。
5. 启动失败日志应便于运维直接定位问题。
6. 对于配置类失败，应说明缺失的配置项名称。

### 非核心任务降级

非核心任务失败时，不应导致应用启动失败。常见非核心任务包括非核心缓存预热、应用状态通知、统计数据加载、第三方非核心服务检查等。

非核心任务的处理方式通常是：捕获异常、记录日志、标记状态、允许后续补偿。

文件位置：`src/main/java/io/github/atengk/listener/NonCoreStartupTaskListener.java`

该监听器用于在应用就绪后执行非核心任务，任务失败时进行降级处理。

```java
package io.github.atengk.listener;

import cn.hutool.core.date.TimeInterval;
import io.github.atengk.service.NonCoreStartupTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 非核心启动任务监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NonCoreStartupTaskListener {

    private final ThreadPoolTaskExecutor startupTaskExecutor;
    private final NonCoreStartupTaskService nonCoreStartupTaskService;

    /**
     * 应用就绪后执行非核心任务
     *
     * @param event 应用就绪事件
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        startupTaskExecutor.execute(() -> {
            TimeInterval timer = new TimeInterval();

            try {
                log.info("开始执行非核心启动任务");
                nonCoreStartupTaskService.execute();
                log.info("非核心启动任务执行完成，耗时：{} ms", timer.interval());
            } catch (Exception e) {
                log.error("非核心启动任务执行失败，已降级处理，耗时：{} ms", timer.interval(), e);
                nonCoreStartupTaskService.markDegraded();
            }
        });
    }

}
```

文件位置：`src/main/java/io/github/atengk/service/NonCoreStartupTaskService.java`

该接口用于定义非核心启动任务能力。

```java
package io.github.atengk.service;

/**
 * 非核心启动任务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface NonCoreStartupTaskService {

    /**
     * 执行非核心启动任务
     */
    void execute();

    /**
     * 标记非核心任务降级
     */
    void markDegraded();

}
```

文件位置：`src/main/java/io/github/atengk/service/impl/NonCoreStartupTaskServiceImpl.java`

该实现类用于执行非核心启动任务并记录降级状态。

```java
package io.github.atengk.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import io.github.atengk.service.NonCoreStartupTaskService;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 非核心启动任务服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class NonCoreStartupTaskServiceImpl implements NonCoreStartupTaskService {

    private final AtomicBoolean degraded = new AtomicBoolean(false);

    /**
     * 执行非核心启动任务
     */
    @Override
    public void execute() {
        log.info("开始预热非核心缓存");
        log.info("非核心缓存预热完成");
    }

    /**
     * 标记非核心任务降级
     */
    @Override
    public void markDegraded() {
        degraded.set(true);
        log.warn("非核心启动任务已标记为降级状态");
    }

    /**
     * 判断是否已降级
     *
     * @return 是否降级
     */
    public boolean isDegraded() {
        return degraded.get();
    }

}
```

非核心任务降级建议如下：

1. 非核心任务必须捕获异常，不能影响应用启动。
2. 降级状态应可观测，例如日志、指标或健康检查。
3. 降级任务应支持后续手动重试或定时补偿。
4. 降级日志应包含任务名称、耗时和异常原因。
5. 不应将所有异常都吞掉，至少需要输出错误日志。
6. 降级逻辑不能产生新的严重副作用。

### 重试机制

启动任务重试适用于临时性故障，例如网络抖动、第三方服务短暂不可用、缓存连接暂时失败等。重试不适合用于配置缺失、认证失败、参数错误等确定性失败。

常见重试策略如下：

| 策略             | 说明                   |
| ---------------- | ---------------------- |
| 固定间隔重试     | 每次等待固定时间       |
| 指数退避重试     | 每次等待时间递增       |
| 最大次数限制     | 避免无限重试           |
| 只重试可恢复异常 | 配置错误不应重试       |
| 异步重试         | 非核心任务放到后台重试 |

文件位置：`src/main/java/io/github/atengk/util/StartupRetryTemplate.java`

该工具类用于提供简单的启动任务重试能力。

```java
package io.github.atengk.util;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * 启动任务重试模板
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class StartupRetryTemplate {

    private StartupRetryTemplate() {
    }

    /**
     * 执行带重试的任务
     *
     * @param taskName        任务名称
     * @param maxAttempts     最大尝试次数
     * @param intervalMillis  重试间隔毫秒数
     * @param supplier        任务逻辑
     * @return 执行结果
     */
    public static boolean execute(String taskName, int maxAttempts, long intervalMillis, Supplier<Boolean> supplier) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("开始执行启动任务，任务名称：{}，当前次数：{}/{}", taskName, attempt, maxAttempts);

                Boolean result = supplier.get();
                if (Boolean.TRUE.equals(result)) {
                    log.info("启动任务执行成功，任务名称：{}，执行次数：{}", taskName, attempt);
                    return true;
                }

                log.warn("启动任务执行未成功，任务名称：{}，当前次数：{}/{}", taskName, attempt, maxAttempts);
            } catch (Exception e) {
                log.error("启动任务执行异常，任务名称：{}，当前次数：{}/{}", taskName, attempt, maxAttempts, e);
            }

            if (attempt < maxAttempts) {
                ThreadUtil.sleep(intervalMillis);
            }
        }

        log.error("启动任务重试后仍失败，任务名称：{}，最大次数：{}", taskName, maxAttempts);
        return false;
    }

}
```

文件位置：`src/main/java/io/github/atengk/runner/RetryResourceCheckRunner.java`

该启动任务用于演示第三方资源检查失败后的有限重试。

```java
package io.github.atengk.runner;

import io.github.atengk.util.StartupRetryTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 带重试的资源检查启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(300)
@Component
public class RetryResourceCheckRunner implements ApplicationRunner {

    /**
     * 执行资源检查
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        boolean success = StartupRetryTemplate.execute("第三方资源连接检查", 3, 1000, this::checkResource);

        if (!success) {
            log.error("核心第三方资源连接检查失败，应用启动中断");
            throw new IllegalStateException("核心第三方资源连接检查失败");
        }

        log.info("核心第三方资源连接检查通过");
    }

    private boolean checkResource() {
        log.info("正在检查第三方资源连接");
        // 示例：调用 Redis、MQ、HTTP 接口等资源检查逻辑
        return true;
    }

}
```

对于非核心任务，重试失败后应降级，而不是中断启动。

文件位置：`src/main/java/io/github/atengk/listener/RetryNonCoreTaskListener.java`

该监听器用于在应用就绪后异步重试非核心启动任务。

```java
package io.github.atengk.listener;

import io.github.atengk.util.StartupRetryTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 非核心任务重试监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryNonCoreTaskListener {

    private final ThreadPoolTaskExecutor startupTaskExecutor;

    /**
     * 应用就绪后异步重试非核心任务
     *
     * @param event 应用就绪事件
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        startupTaskExecutor.execute(() -> {
            boolean success = StartupRetryTemplate.execute("非核心缓存预热", 3, 2000, this::warmUpCache);

            if (!success) {
                log.warn("非核心缓存预热重试失败，已降级处理");
                return;
            }

            log.info("非核心缓存预热重试成功");
        });
    }

    private boolean warmUpCache() {
        log.info("正在执行非核心缓存预热");
        return true;
    }

}
```

重试机制建议如下：

1. 必须设置最大重试次数，禁止无限重试。
2. 只对临时性故障进行重试。
3. 配置错误、权限错误、参数错误不应重试。
4. 重试间隔不宜过短，避免压垮外部服务。
5. 核心任务重试失败后可以中断启动。
6. 非核心任务重试失败后应降级处理。
7. 重试日志应记录任务名称、当前次数、最大次数和异常原因。

### 日志记录规范

启动任务日志是排查启动失败和启动慢问题的重要依据。日志应清晰表达任务名称、执行阶段、耗时、结果和异常原因。

推荐日志格式如下：

```text
开始执行启动任务，任务名称：系统配置预加载
启动任务执行完成，任务名称：系统配置预加载，耗时：120 ms
启动任务执行失败，任务名称：系统配置预加载，耗时：120 ms，失败原因：xxx
```

启动任务日志至少应包含以下信息：

| 信息     | 说明                             |
| -------- | -------------------------------- |
| 任务名称 | 明确是哪一个启动任务             |
| 执行阶段 | 开始、完成、失败、降级           |
| 耗时     | 便于分析启动慢问题               |
| 关键参数 | 环境、租户、资源名称等非敏感参数 |
| 异常堆栈 | 便于定位失败原因                 |
| 降级状态 | 明确失败是否影响应用启动         |

文件位置：`src/main/java/io/github/atengk/util/StartupLogTemplate.java`

该工具类用于统一记录启动任务执行日志。

```java
package io.github.atengk.util;

import cn.hutool.core.date.TimeInterval;
import lombok.extern.slf4j.Slf4j;

/**
 * 启动任务日志模板
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class StartupLogTemplate {

    private StartupLogTemplate() {
    }

    /**
     * 执行启动任务并记录日志
     *
     * @param taskName 任务名称
     * @param runnable 任务逻辑
     */
    public static void execute(String taskName, Runnable runnable) {
        TimeInterval timer = new TimeInterval();
        log.info("开始执行启动任务，任务名称：{}", taskName);

        try {
            runnable.run();
            log.info("启动任务执行完成，任务名称：{}，耗时：{} ms", taskName, timer.interval());
        } catch (Exception e) {
            log.error("启动任务执行失败，任务名称：{}，耗时：{} ms", taskName, timer.interval(), e);
            throw e;
        }
    }

    /**
     * 执行可降级启动任务并记录日志
     *
     * @param taskName 任务名称
     * @param runnable 任务逻辑
     */
    public static void executeDegradable(String taskName, Runnable runnable) {
        TimeInterval timer = new TimeInterval();
        log.info("开始执行可降级启动任务，任务名称：{}", taskName);

        try {
            runnable.run();
            log.info("可降级启动任务执行完成，任务名称：{}，耗时：{} ms", taskName, timer.interval());
        } catch (Exception e) {
            log.error("可降级启动任务执行失败，已降级处理，任务名称：{}，耗时：{} ms", taskName, timer.interval(), e);
        }
    }

}
```

文件位置：`src/main/java/io/github/atengk/runner/StartupLogDemoRunner.java`

该启动任务用于演示如何使用统一日志模板记录启动任务。

```java
package io.github.atengk.runner;

import io.github.atengk.util.StartupLogTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动日志示例任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(600)
@Component
public class StartupLogDemoRunner implements ApplicationRunner {

    /**
     * 执行启动日志示例任务
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        StartupLogTemplate.execute("核心配置检查", this::checkCoreConfig);
        StartupLogTemplate.executeDegradable("非核心缓存预热", this::warmUpNonCoreCache);
    }

    private void checkCoreConfig() {
        log.info("核心配置检查通过");
    }

    private void warmUpNonCoreCache() {
        log.info("非核心缓存预热完成");
    }

}
```

日志记录规范建议如下：

1. 启动任务必须记录开始日志和完成日志。
2. 重要任务必须记录耗时。
3. 异常日志必须包含堆栈，不要只打印异常消息。
4. 不允许打印密码、密钥、Token、身份证号等敏感信息。
5. 降级日志应明确说明“不影响应用启动”。
6. 重试日志应记录当前次数和最大次数。
7. 多实例环境中建议打印实例标识、应用名称和环境信息。
8. 日志内容应面向排障，不应写成无意义的“执行成功”“执行失败”。

整体建议如下：

| 问题               | 推荐做法                                |
| ------------------ | --------------------------------------- |
| 启动任务耗时长     | 拆分任务，非核心任务异步执行            |
| 外部服务不稳定     | 设置超时、重试和降级                    |
| 启动失败难排查     | 统一记录任务名称、耗时和异常堆栈        |
| 多实例重复执行     | 使用幂等设计或分布式锁                  |
| 线程资源失控       | 使用受管理线程池                        |
| 非核心任务拖垮应用 | 放到 `ApplicationReadyEvent` 后异步执行 |



## 开发规范

启动任务属于应用启动链路的一部分，直接影响应用是否能够正常启动、启动耗时是否可控、初始化状态是否一致。开发启动任务时，应明确任务职责、执行顺序、失败策略和适用环境，避免启动逻辑混乱、重复执行、阻塞启动或污染生产数据。

启动任务开发应遵循以下基本原则：

| 原则     | 说明                                         |
| -------- | -------------------------------------------- |
| 职责清晰 | 每个启动任务只负责一类初始化逻辑             |
| 顺序明确 | 存在依赖关系的任务必须声明执行顺序           |
| 失败可控 | 核心任务失败可中断启动，非核心任务失败应降级 |
| 执行幂等 | 重复执行不应产生脏数据或重复副作用           |
| 环境隔离 | 开发、测试、生产环境应按需启用启动任务       |
| 日志完整 | 记录任务开始、结束、耗时和异常信息           |

### 启动任务职责边界

启动任务不应承载过多业务逻辑。Runner、监听器和生命周期组件应主要负责调度，具体业务处理应下沉到独立的 Service、Component 或 Task 类中。

不推荐写法是将所有逻辑直接写在 Runner 中，例如在一个 Runner 中同时加载配置、初始化缓存、检查第三方资源、执行数据修复和发送通知。这样会导致职责不清晰、测试困难、异常处理混乱。

推荐做法如下：

| 层级                  | 职责                                     |
| --------------------- | ---------------------------------------- |
| Runner / Listener     | 控制执行时机、记录启动日志、处理异常策略 |
| StartupTask / Service | 封装具体启动任务逻辑                     |
| Repository / Client   | 访问数据库、缓存、第三方资源             |
| Holder / Cache        | 保存启动加载后的运行时数据               |

文件位置：`src/main/java/io/github/atengk/startup/StartupTask.java`

该接口用于统一定义启动任务的执行规范。

```java
package io.github.atengk.startup;

/**
 * 启动任务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface StartupTask {

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    String taskName();

    /**
     * 执行启动任务
     */
    void execute();

}
```

文件位置：`src/main/java/io/github/atengk/startup/SystemConfigStartupTask.java`

该类只负责系统配置预加载，不混入缓存、字典或外部通知逻辑。

```java
package io.github.atengk.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 系统配置启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class SystemConfigStartupTask implements StartupTask {

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    @Override
    public String taskName() {
        return "系统配置预加载";
    }

    /**
     * 执行系统配置预加载
     */
    @Override
    public void execute() {
        log.info("开始加载系统配置");
        // 示例：从数据库、配置中心或配置文件加载系统配置
        log.info("系统配置加载完成");
    }

}
```

文件位置：`src/main/java/io/github/atengk/runner/SystemConfigStartupRunner.java`

该 Runner 只负责调度系统配置启动任务，并记录执行日志。

```java
package io.github.atengk.runner;

import cn.hutool.core.date.TimeInterval;
import io.github.atengk.startup.SystemConfigStartupTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 系统配置启动任务调度器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(100)
@Component
@RequiredArgsConstructor
public class SystemConfigStartupRunner implements ApplicationRunner {

    private final SystemConfigStartupTask systemConfigStartupTask;

    /**
     * 调度系统配置启动任务
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        TimeInterval timer = new TimeInterval();
        log.info("开始执行启动任务，任务名称：{}", systemConfigStartupTask.taskName());

        systemConfigStartupTask.execute();

        log.info("启动任务执行完成，任务名称：{}，耗时：{} ms", systemConfigStartupTask.taskName(), timer.interval());
    }

}
```

职责边界建议如下：

1. Runner 不直接承载复杂业务逻辑。
2. 一个启动任务只处理一个明确职责。
3. 数据修复、迁移、补偿类逻辑不应和常规启动任务混在一起。
4. 非核心任务不要放入核心启动任务链路。
5. 启动任务应能独立测试，不依赖完整应用启动才能验证。

### 多任务拆分原则

当启动阶段存在多个任务时，应按照职责、依赖关系和失败策略进行拆分。拆分后的任务更容易排序、测试、监控和降级。

推荐拆分方式如下：

| 拆分维度       | 示例                                           |
| -------------- | ---------------------------------------------- |
| 按业务领域拆分 | 系统配置、字典数据、权限数据、租户数据         |
| 按资源类型拆分 | 数据库检查、Redis 检查、MQ 检查、HTTP 服务检查 |
| 按任务等级拆分 | 核心任务、重要任务、非核心任务                 |
| 按执行方式拆分 | 同步任务、异步任务、延迟任务                   |
| 按环境拆分     | 开发环境任务、测试环境任务、生产环境任务       |

文件位置：`src/main/java/io/github/atengk/startup/StartupTaskGroupRunner.java`

该 Runner 用于统一调度多个启动任务，并按照 `@Order` 或 `Ordered` 的排序结果执行。

```java
package io.github.atengk.startup;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.TimeInterval;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动任务组调度器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(100)
@Component
@RequiredArgsConstructor
public class StartupTaskGroupRunner implements ApplicationRunner {

    private final List<StartupTask> startupTasks;

    /**
     * 执行启动任务组
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        if (CollUtil.isEmpty(startupTasks)) {
            log.warn("未发现启动任务，跳过启动任务组调度");
            return;
        }

        log.info("开始执行启动任务组，任务数量：{}", startupTasks.size());

        for (StartupTask startupTask : startupTasks) {
            executeTask(startupTask);
        }

        log.info("启动任务组执行完成");
    }

    private void executeTask(StartupTask startupTask) {
        TimeInterval timer = new TimeInterval();
        log.info("开始执行启动任务，任务名称：{}", startupTask.taskName());

        startupTask.execute();

        log.info("启动任务执行完成，任务名称：{}，耗时：{} ms", startupTask.taskName(), timer.interval());
    }

}
```

多任务拆分建议如下：

1. 核心任务和非核心任务分开管理。
2. 同步任务和异步任务分开管理。
3. 数据加载任务和资源检查任务分开管理。
4. 常规启动任务和一次性修复任务分开管理。
5. 多任务之间存在依赖时，不应通过隐式顺序控制。
6. 启动任务数量较多时，建议定义统一任务接口和任务调度器。

### 执行顺序管理

启动任务之间存在依赖关系时，必须显式声明执行顺序。执行顺序不应依赖类名、文件顺序、Bean 扫描顺序或开发者经验。

推荐按阶段规划顺序值：

| 阶段           | 顺序范围    | 任务类型                              |
| -------------- | ----------- | ------------------------------------- |
| 基础检查阶段   | `0 - 99`    | 环境变量、核心配置、License、密钥检查 |
| 配置加载阶段   | `100 - 199` | 系统配置、租户配置、业务开关          |
| 基础数据阶段   | `200 - 299` | 字典、枚举、权限基础数据              |
| 缓存初始化阶段 | `300 - 399` | 核心缓存、本地缓存、Redis 缓存        |
| 资源检查阶段   | `400 - 499` | Redis、MQ、对象存储、第三方服务       |
| 非核心任务阶段 | `500+`      | 状态上报、非核心预热、异步触发        |

文件位置：`src/main/java/io/github/atengk/startup/OrderedStartupTask.java`

该接口用于定义带顺序的启动任务。

```java
package io.github.atengk.startup;

/**
 * 有序启动任务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface OrderedStartupTask extends StartupTask {

    /**
     * 获取执行顺序
     *
     * @return 执行顺序，数值越小越早执行
     */
    int order();

}
```

文件位置：`src/main/java/io/github/atengk/startup/OrderedStartupTaskRunner.java`

该 Runner 用于按照任务顺序执行多个启动任务。

```java
package io.github.atengk.startup;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.TimeInterval;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 有序启动任务调度器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(100)
@Component
@RequiredArgsConstructor
public class OrderedStartupTaskRunner implements ApplicationRunner {

    private final List<OrderedStartupTask> startupTasks;

    /**
     * 按顺序执行启动任务
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        if (CollUtil.isEmpty(startupTasks)) {
            log.warn("未发现有序启动任务，跳过执行");
            return;
        }

        startupTasks.stream()
                .sorted(Comparator.comparingInt(OrderedStartupTask::order))
                .forEach(this::executeTask);
    }

    private void executeTask(OrderedStartupTask startupTask) {
        TimeInterval timer = new TimeInterval();
        log.info("开始执行启动任务，任务名称：{}，顺序：{}", startupTask.taskName(), startupTask.order());

        startupTask.execute();

        log.info("启动任务执行完成，任务名称：{}，顺序：{}，耗时：{} ms",
                startupTask.taskName(), startupTask.order(), timer.interval());
    }

}
```

执行顺序管理建议如下：

1. 存在依赖关系的任务必须设置顺序。
2. 顺序值建议按阶段预留区间，不要全部连续编号。
3. 同一顺序值下的任务不应存在依赖关系。
4. Runner 使用 `@Order`，`SmartLifecycle` 使用 `getPhase()`。
5. 启动顺序和停止顺序要分开考虑，尤其是生命周期组件。
6. 顺序规则应写入开发文档，避免后续维护人员随意插入任务。

### 幂等性设计

启动任务可能因为应用重启、容器滚动发布、失败重试、多实例部署等原因重复执行。如果任务不具备幂等性，可能导致重复插入数据、重复发送通知、重复创建资源、重复扣减库存等问题。

启动任务幂等性设计的核心目标是：同一个任务执行一次和执行多次，最终结果应保持一致。

常见幂等设计方式如下：

| 方式       | 说明                             |
| ---------- | -------------------------------- |
| 唯一键约束 | 通过数据库唯一索引避免重复数据   |
| 状态标记   | 记录任务是否已执行               |
| 分布式锁   | 多实例环境下只允许一个实例执行   |
| 覆盖写入   | 使用 upsert、replace、put 等方式 |
| 版本号控制 | 根据版本判断是否需要执行         |
| 查询后执行 | 执行前先检查是否已存在           |

文件位置：`src/main/java/io/github/atengk/startup/IdempotentDictStartupTask.java`

该任务用于演示启动任务执行前先检查状态，避免重复加载。

```java
package io.github.atengk.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 幂等字典启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class IdempotentDictStartupTask implements OrderedStartupTask {

    private final AtomicBoolean loaded = new AtomicBoolean(false);

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    @Override
    public String taskName() {
        return "幂等字典数据加载";
    }

    /**
     * 获取执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 200;
    }

    /**
     * 执行幂等字典加载
     */
    @Override
    public void execute() {
        if (!loaded.compareAndSet(false, true)) {
            log.info("字典数据已加载，跳过重复执行");
            return;
        }

        log.info("开始加载字典数据");
        // 示例：加载字典数据到本地缓存或 Redis
        log.info("字典数据加载完成");
    }

}
```

在多实例部署环境中，仅使用本地 `AtomicBoolean` 不能解决多实例重复执行问题。如果任务会影响共享资源，应使用数据库唯一约束、Redis 分布式锁或任务执行记录表。

幂等性设计建议如下：

1. 所有启动任务默认都应按可重复执行设计。
2. 数据写入类任务必须具备幂等保护。
3. 外部通知类任务应避免重复发送或支持业务去重。
4. 缓存初始化推荐使用覆盖写入。
5. 多实例部署下的重型任务应使用分布式锁。
6. 一次性修复任务应记录执行批次、执行人、执行时间和执行结果。

### 环境隔离处理

不同环境对启动任务的要求不同。开发环境可能需要自动初始化测试数据，测试环境可能需要执行模拟资源检查，生产环境则需要严格控制数据修复、缓存预热和外部通知。

环境隔离可以通过以下方式实现：

| 方式       | 说明                                         |
| ---------- | -------------------------------------------- |
| `@Profile` | 按 Spring Profile 启用不同 Bean              |
| 配置开关   | 使用配置项控制任务是否执行                   |
| 条件注解   | 使用 `@ConditionalOnProperty` 控制 Bean 创建 |
| 启动参数   | 使用命令行参数临时启用任务                   |
| 环境变量   | 使用环境变量控制生产行为                     |

文件位置：`src/main/resources/application.yml`

该配置用于控制启动任务是否启用。

```yaml
startup:
  task:
    # 是否启用系统配置预加载任务
    system-config-enabled: true
    # 是否启用测试数据初始化任务，生产环境应关闭
    test-data-enabled: false
```

文件位置：`src/main/java/io/github/atengk/startup/TestDataStartupTask.java`

该任务仅在 `dev` 或 `test` 环境启用，用于初始化测试数据。

```java
package io.github.atengk.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 测试数据启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Profile({"dev", "test"})
@Component
public class TestDataStartupTask implements OrderedStartupTask {

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    @Override
    public String taskName() {
        return "测试数据初始化";
    }

    /**
     * 获取执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 900;
    }

    /**
     * 初始化测试数据
     */
    @Override
    public void execute() {
        log.info("当前为开发或测试环境，开始初始化测试数据");
        // 示例：初始化测试账号、测试配置、模拟字典
        log.info("测试数据初始化完成");
    }

}
```

文件位置：`src/main/java/io/github/atengk/startup/ProductionSafeStartupTask.java`

该任务通过配置开关控制是否创建 Bean。

```java
package io.github.atengk.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 生产安全启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "startup.task", name = "system-config-enabled", havingValue = "true")
public class ProductionSafeStartupTask implements OrderedStartupTask {

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    @Override
    public String taskName() {
        return "生产安全启动任务";
    }

    /**
     * 获取执行顺序
     *
     * @return 执行顺序
     */
    @Override
    public int order() {
        return 100;
    }

    /**
     * 执行生产安全启动任务
     */
    @Override
    public void execute() {
        log.info("生产安全启动任务执行完成");
    }

}
```

环境隔离建议如下：

1. 测试数据初始化任务禁止在生产环境启用。
2. 数据修复任务不应默认随应用启动执行。
3. 高风险启动任务必须配置开关。
4. 生产环境启动任务应尽量只执行必要初始化。
5. 本地开发环境可以放宽检查，生产环境应严格检查核心依赖。
6. 所有环境差异应通过配置表达，不应硬编码环境名称。

## 测试与验证

启动任务的测试目标是验证任务能否在正确阶段执行、执行顺序是否符合预期、异常策略是否生效、非核心任务是否能够降级。启动任务不是普通业务接口，测试时应重点关注生命周期、上下文加载和启动失败行为。

测试启动任务时，建议覆盖以下内容：

| 测试类型 | 验证目标                             |
| -------- | ------------------------------------ |
| 单元测试 | 验证单个启动任务逻辑是否正确         |
| 集成测试 | 验证 Spring 容器中任务是否能正常执行 |
| 顺序验证 | 验证多个任务执行顺序是否正确         |
| 异常验证 | 验证核心失败、非核心降级和重试行为   |

### 单元测试

单元测试用于验证单个启动任务的业务逻辑，不依赖完整 Spring Boot 启动流程。对于启动任务，推荐将业务逻辑封装到独立 Service 或 Task 中，再对该类进行单元测试。

文件位置：`src/test/java/io/github/atengk/startup/SystemConfigStartupTaskTest.java`

该测试类用于验证系统配置启动任务可以正常执行。

```java
package io.github.atengk.startup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 系统配置启动任务测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class SystemConfigStartupTaskTest {

    /**
     * 测试系统配置启动任务正常执行
     */
    @Test
    void shouldExecuteSystemConfigStartupTask() {
        SystemConfigStartupTask startupTask = new SystemConfigStartupTask();

        Assertions.assertDoesNotThrow(startupTask::execute);
        Assertions.assertEquals("系统配置预加载", startupTask.taskName());
    }

}
```

对于有状态任务，可以验证重复执行是否符合幂等预期。

文件位置：`src/test/java/io/github/atengk/startup/IdempotentDictStartupTaskTest.java`

该测试类用于验证字典启动任务重复执行不会抛出异常。

```java
package io.github.atengk.startup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 幂等字典启动任务测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class IdempotentDictStartupTaskTest {

    /**
     * 测试重复执行字典启动任务
     */
    @Test
    void shouldExecuteRepeatedlyWithoutException() {
        IdempotentDictStartupTask startupTask = new IdempotentDictStartupTask();

        Assertions.assertDoesNotThrow(startupTask::execute);
        Assertions.assertDoesNotThrow(startupTask::execute);
    }

}
```

单元测试建议如下：

1. 优先测试 Task 或 Service，不直接测试 Runner。
2. 核心逻辑应脱离 Spring 生命周期单独验证。
3. 幂等任务必须测试重复执行。
4. 异常任务必须测试失败路径。
5. 外部依赖应使用 Mock，不连接真实资源。

### 集成测试

集成测试用于验证启动任务在 Spring 容器中能否正常注册、注入和执行。对于 Runner、事件监听器、`SmartLifecycle` 等依赖 Spring 生命周期的组件，应使用 Spring Boot 测试环境验证。

如果项目中还未引入测试依赖，可以添加以下 Maven 依赖。

文件位置：`pom.xml`

该依赖用于提供 Spring Boot 测试能力。

```xml
<dependencies>
    <!-- Spring Boot 测试依赖，包含 JUnit Jupiter、Spring Test、AssertJ、Mockito 等 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

文件位置：`src/test/java/io/github/atengk/startup/StartupTaskIntegrationTest.java`

该测试类用于验证启动任务 Bean 能够被 Spring 容器正常加载。

```java
package io.github.atengk.startup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 启动任务集成测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
class StartupTaskIntegrationTest {

    private final ObjectProvider<OrderedStartupTask> startupTaskProvider;

    StartupTaskIntegrationTest(ObjectProvider<OrderedStartupTask> startupTaskProvider) {
        this.startupTaskProvider = startupTaskProvider;
    }

    /**
     * 测试启动任务 Bean 是否正常加载
     */
    @Test
    void shouldLoadStartupTaskBeans() {
        List<OrderedStartupTask> startupTasks = startupTaskProvider.stream().toList();

        Assertions.assertFalse(startupTasks.isEmpty());
    }

}
```

如果需要验证配置开关，可以使用 `@SpringBootTest(properties = "...")` 指定测试配置。

文件位置：`src/test/java/io/github/atengk/startup/StartupTaskPropertyIntegrationTest.java`

该测试类用于验证配置开关控制的启动任务是否生效。

```java
package io.github.atengk.startup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 启动任务配置开关集成测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest(properties = "startup.task.system-config-enabled=true")
class StartupTaskPropertyIntegrationTest {

    private final ObjectProvider<ProductionSafeStartupTask> taskProvider;

    StartupTaskPropertyIntegrationTest(ObjectProvider<ProductionSafeStartupTask> taskProvider) {
        this.taskProvider = taskProvider;
    }

    /**
     * 测试配置开启时启动任务 Bean 是否存在
     */
    @Test
    void shouldLoadTaskWhenPropertyEnabled() {
        Assertions.assertNotNull(taskProvider.getIfAvailable());
    }

}
```

集成测试建议如下：

1. 验证启动任务 Bean 是否能正常加载。
2. 验证配置属性是否正确绑定。
3. 验证 Profile 和 Conditional 是否生效。
4. 对真实外部依赖使用测试容器或 Mock。
5. 不建议在集成测试中执行高风险数据修复任务。

### 启动顺序验证

启动顺序验证用于确保多个任务按照预期顺序执行。推荐将任务执行记录写入内存列表，再在测试中断言顺序。

文件位置：`src/main/java/io/github/atengk/startup/StartupExecutionRecorder.java`

该组件用于记录启动任务执行顺序。

```java
package io.github.atengk.startup;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 启动任务执行记录器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
public class StartupExecutionRecorder {

    private final List<String> records = new CopyOnWriteArrayList<>();

    /**
     * 记录任务名称
     *
     * @param taskName 任务名称
     */
    public void record(String taskName) {
        records.add(taskName);
    }

    /**
     * 获取任务记录
     *
     * @return 任务记录
     */
    public List<String> records() {
        return records;
    }

    /**
     * 清理任务记录
     */
    public void clear() {
        records.clear();
    }

}
```

文件位置：`src/test/java/io/github/atengk/startup/StartupOrderTest.java`

该测试类用于验证启动任务的排序结果。

```java
package io.github.atengk.startup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

/**
 * 启动任务顺序测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class StartupOrderTest {

    /**
     * 测试启动任务按 order 从小到大排序
     */
    @Test
    void shouldSortStartupTasksByOrder() {
        OrderedStartupTask firstTask = new MockStartupTask("配置加载", 100);
        OrderedStartupTask secondTask = new MockStartupTask("字典加载", 200);
        OrderedStartupTask thirdTask = new MockStartupTask("缓存初始化", 300);

        List<OrderedStartupTask> tasks = List.of(thirdTask, firstTask, secondTask).stream()
                .sorted(Comparator.comparingInt(OrderedStartupTask::order))
                .toList();

        Assertions.assertEquals("配置加载", tasks.get(0).taskName());
        Assertions.assertEquals("字典加载", tasks.get(1).taskName());
        Assertions.assertEquals("缓存初始化", tasks.get(2).taskName());
    }

    /**
     * 模拟启动任务
     *
     * @author Ateng
     * @since 2026-05-06
     */
    private record MockStartupTask(String taskName, int order) implements OrderedStartupTask {

        /**
         * 执行模拟启动任务
         */
        @Override
        public void execute() {
            // 测试排序时无需执行具体逻辑
        }
    }

}
```

启动顺序验证建议如下：

1. 对有强依赖关系的任务编写顺序测试。
2. 顺序值应作为测试断言的一部分。
3. 同一阶段内无依赖任务不必强行验证细节顺序。
4. `SmartLifecycle` 应单独验证 `phase` 设计。
5. 不建议通过日志文本作为唯一顺序验证依据。

### 异常场景验证

异常场景验证用于确认启动任务失败时是否符合预期。核心任务失败应抛出异常，非核心任务失败应捕获异常并降级。

文件位置：`src/main/java/io/github/atengk/startup/CoreFailStartupTask.java`

该任务用于模拟核心启动任务失败。

```java
package io.github.atengk.startup;

import org.springframework.stereotype.Component;

/**
 * 核心失败启动任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
public class CoreFailStartupTask implements StartupTask {

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    @Override
    public String taskName() {
        return "核心失败启动任务";
    }

    /**
     * 执行失败任务
     */
    @Override
    public void execute() {
        throw new IllegalStateException("模拟核心启动任务失败");
    }

}
```

文件位置：`src/test/java/io/github/atengk/startup/CoreFailStartupTaskTest.java`

该测试类用于验证核心启动任务失败时会抛出异常。

```java
package io.github.atengk.startup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 核心失败启动任务测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class CoreFailStartupTaskTest {

    /**
     * 测试核心任务失败时抛出异常
     */
    @Test
    void shouldThrowExceptionWhenCoreTaskFailed() {
        CoreFailStartupTask startupTask = new CoreFailStartupTask();

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, startupTask::execute);
        Assertions.assertTrue(exception.getMessage().contains("模拟核心启动任务失败"));
    }

}
```

文件位置：`src/test/java/io/github/atengk/startup/DegradableStartupTaskTest.java`

该测试类用于验证非核心任务失败后可以被降级处理。

```java
package io.github.atengk.startup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 可降级启动任务测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class DegradableStartupTaskTest {

    /**
     * 测试非核心任务异常被捕获后不继续抛出
     */
    @Test
    void shouldNotThrowWhenNonCoreTaskDegraded() {
        Assertions.assertDoesNotThrow(() -> {
            try {
                throw new IllegalStateException("模拟非核心任务失败");
            } catch (Exception ignored) {
                // 非核心任务降级，不继续抛出异常
            }
        });
    }

}
```

异常场景验证建议如下：

1. 核心任务失败必须有测试覆盖。
2. 非核心任务失败必须验证不会导致应用启动失败。
3. 重试任务应验证成功、失败、达到最大次数三种场景。
4. 超时任务应验证超时后的降级行为。
5. 异步任务应验证线程内部异常是否被捕获。
6. 测试中不应连接真实生产资源。

## 最佳实践

Spring Boot 启动执行机制的最佳实践可以总结为：核心任务同步、非核心任务异步；职责清晰、顺序明确；失败可控、日志完整；高风险任务禁用默认启动；生产环境优先保证启动稳定性。

### Runner 使用建议

Runner 适合执行应用启动后的业务初始化任务，尤其是依赖 Spring Bean、数据库、Redis 或配置中心的逻辑。实际开发中更推荐使用 `ApplicationRunner`，因为它提供结构化参数解析能力。

Runner 使用建议如下：

| 建议                         | 说明                 |
| ---------------------------- | -------------------- |
| 优先使用 `ApplicationRunner` | 参数解析更清晰       |
| 使用 `@Order` 管理顺序       | 避免隐式顺序依赖     |
| 避免长时间阻塞               | 耗时任务应异步或延迟 |
| 区分核心与非核心任务         | 失败策略不同         |
| 业务逻辑下沉到 Service       | Runner 只负责调度    |
| 记录任务耗时                 | 便于排查启动慢问题   |

推荐 Runner 模板如下。

文件位置：`src/main/java/io/github/atengk/runner/RecommendedStartupRunner.java`

该模板用于规范 Runner 中的日志、耗时和异常处理方式。

```java
package io.github.atengk.runner;

import cn.hutool.core.date.TimeInterval;
import io.github.atengk.startup.StartupTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 推荐启动任务 Runner 模板
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Order(100)
@Component
@RequiredArgsConstructor
public class RecommendedStartupRunner implements ApplicationRunner {

    private final StartupTask startupTask;

    /**
     * 执行启动任务
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        TimeInterval timer = new TimeInterval();

        try {
            log.info("开始执行启动任务，任务名称：{}", startupTask.taskName());
            startupTask.execute();
            log.info("启动任务执行完成，任务名称：{}，耗时：{} ms", startupTask.taskName(), timer.interval());
        } catch (Exception e) {
            log.error("启动任务执行失败，任务名称：{}，耗时：{} ms", startupTask.taskName(), timer.interval(), e);
            throw e;
        }
    }

}
```

Runner 不适合以下场景：

1. 需要随容器停止释放资源的组件。
2. 长时间运行的后台消费者。
3. 周期性执行的任务。
4. 非核心且耗时较长的缓存预热。
5. 需要精细控制启动和停止顺序的生命周期组件。

这些场景应考虑使用 `SmartLifecycle`、调度任务、消息监听器或 `ApplicationReadyEvent` 异步任务。

### SmartLifecycle 使用建议

`SmartLifecycle` 适合管理长生命周期组件，例如消息消费者、长连接客户端、监听器、后台工作线程等。它的重点不是执行一次性启动任务，而是管理组件的启动、运行和停止。

SmartLifecycle 使用建议如下：

| 建议                           | 说明                     |
| ------------------------------ | ------------------------ |
| 用于长生命周期组件             | 消费者、监听器、连接组件 |
| 使用 `AtomicBoolean` 管理状态  | 避免重复启动或停止       |
| 实现 `stop(Runnable callback)` | 支持优雅停机             |
| 合理设置 `getPhase()`          | 控制启动和停止顺序       |
| 避免 `start()` 长时间阻塞      | 防止影响容器启动         |
| 停止时释放资源                 | 关闭线程池、连接、监听器 |

推荐 SmartLifecycle 模板如下。

文件位置：`src/main/java/io/github/atengk/lifecycle/RecommendedLifecycleComponent.java`

该模板用于规范生命周期组件的启动、停止和优雅停机处理。

```java
package io.github.atengk.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 推荐生命周期组件模板
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class RecommendedLifecycleComponent implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 启动生命周期组件
     */
    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.info("生命周期组件已运行，跳过重复启动");
            return;
        }

        log.info("生命周期组件开始启动");
        // 示例：启动消费者、监听器、长连接客户端
        log.info("生命周期组件启动完成");
    }

    /**
     * 停止生命周期组件
     */
    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            log.info("生命周期组件未运行，跳过停止");
            return;
        }

        log.info("生命周期组件开始停止");
        // 示例：停止消费者、关闭连接、释放资源
        log.info("生命周期组件停止完成");
    }

    /**
     * 优雅停止生命周期组件
     *
     * @param callback 停止完成回调
     */
    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    /**
     * 判断组件是否运行中
     *
     * @return 是否运行中
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 是否自动启动
     *
     * @return 是否自动启动
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /**
     * 获取生命周期阶段
     *
     * @return 阶段值
     */
    @Override
    public int getPhase() {
        return 100;
    }

}
```

SmartLifecycle 不适合以下场景：

1. 只执行一次的数据初始化。
2. 普通启动参数解析。
3. 非核心缓存预热。
4. 单个 Bean 内部属性初始化。
5. 不需要停机处理的简单任务。

这些场景优先使用 `@PostConstruct`、`ApplicationRunner` 或 `ApplicationReadyEvent`。

### 启动耗时优化

启动耗时优化的目标是减少应用从启动到就绪的时间，同时保证必要初始化逻辑完整执行。优化时应先识别耗时任务，再决定同步、异步、延迟或懒加载策略。

常见优化方式如下：

| 优化方式           | 说明                             |
| ------------------ | -------------------------------- |
| 减少同步启动任务   | 非核心任务迁移到异步执行         |
| 拆分重型任务       | 大任务拆分为多个小任务           |
| 并行执行无依赖任务 | 使用线程池或 `CompletableFuture` |
| 分批加载数据       | 避免一次性全量加载               |
| 增加超时控制       | 防止外部依赖卡住启动             |
| 延迟初始化         | 首次使用时再加载非核心数据       |
| 缓存持久化         | 避免每次启动都重新构建重型缓存   |

文件位置：`src/main/java/io/github/atengk/listener/StartupCostLogListener.java`

该监听器用于记录应用启动总耗时。

```java
package io.github.atengk.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动耗时日志监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class StartupCostLogListener {

    /**
     * 记录应用启动耗时
     *
     * @param event 应用就绪事件
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        long startupTime = event.getTimeTaken() == null ? -1 : event.getTimeTaken().toMillis();
        log.info("应用启动完成，总耗时：{} ms", startupTime);
    }

}
```

启动耗时优化建议如下：

1. 给每个启动任务记录耗时。
2. 大于 1 秒的启动任务应重点分析。
3. 非核心任务放到应用就绪后异步执行。
4. 无依赖任务可以并行执行。
5. 数据加载应分页、分批、限量。
6. 外部资源检查必须设置超时。
7. 避免在 Bean 初始化阶段执行重型逻辑。
8. 容器化环境中应结合健康检查和启动探针配置。

### 生产环境注意事项

生产环境启动任务应以稳定性和可恢复性为优先目标。启动任务越多，应用启动链路越复杂，启动失败概率越高。因此生产环境应严格控制启动任务范围，只保留应用运行所必需的初始化逻辑。

生产环境重点注意事项如下：

| 事项         | 建议                             |
| ------------ | -------------------------------- |
| 高风险任务   | 默认关闭，通过参数或开关显式启用 |
| 数据修复任务 | 不随应用常规启动自动执行         |
| 外部接口调用 | 必须设置超时和降级               |
| 缓存预热     | 控制数据量，避免打爆数据库       |
| 多实例部署   | 使用幂等设计和分布式锁           |
| 启动日志     | 记录任务名称、耗时、结果         |
| 敏感信息     | 禁止打印密码、密钥、Token        |
| 失败策略     | 区分核心失败和非核心降级         |

生产环境不建议的做法：

1. 应用每次启动都执行数据修复。
2. 启动时全量加载大表数据。
3. 所有实例同时预热同一批大缓存。
4. 无超时调用第三方接口。
5. 非核心任务失败导致应用启动失败。
6. 启动任务中打印敏感配置。
7. 通过启动任务修改生产业务数据但没有审计记录。
8. 高风险任务没有配置开关。

生产环境推荐启动任务分类如下：

| 类型                 | 推荐处理方式             |
| -------------------- | ------------------------ |
| 核心配置检查         | 同步执行，失败中断启动   |
| 核心资源检查         | 同步执行，设置超时       |
| 字典数据加载         | 同步或短时间阻塞         |
| 核心缓存初始化       | 限量加载，失败策略明确   |
| 非核心缓存预热       | 应用就绪后异步执行       |
| 第三方非核心资源检查 | 异步检查，失败降级       |
| 数据修复任务         | 禁止默认启动，手动触发   |
| 状态上报             | 异步执行，失败不影响启动 |

最终建议如下：

1. 启动任务应少而明确，不应把常规业务流程放入启动链路。
2. 所有启动任务都应具备日志、耗时、异常和降级策略。
3. 生产环境启动任务必须支持配置化开关。
4. 数据写入类启动任务必须具备幂等性。
5. 多实例部署环境必须考虑重复执行问题。
6. 影响应用可用性的任务可以阻塞启动，其他任务尽量异步。
7. `Runner` 用于一次性初始化，`SmartLifecycle` 用于生命周期组件，二者不要混用。
8. 启动任务设计完成后，应补充单元测试、集成测试、顺序验证和异常验证。