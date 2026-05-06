# Spring Boot 线程池

Spring Boot 线程池主要用于承载异步任务、并发业务处理、耗时任务拆分、批量任务执行等场景。在线上项目中，不建议直接使用 `new Thread()` 或无界线程池，而应通过统一的线程池配置控制线程数量、队列容量、拒绝策略、线程命名和关闭行为，从而提升系统稳定性和可观测性。

## 线程池概述

本章节用于说明线程池在 Spring Boot 项目中的定位、适用场景、设计目标和核心参数。线程池不是单纯提升并发量的工具，而是用于限制并发资源、隔离任务类型、保护应用稳定性的基础组件。

### 使用场景

线程池适合处理“可以异步执行、可以并发执行、对响应链路不要求强同步完成”的任务。常见场景如下：

| 场景             | 说明                                     | 示例                                 |
| ---------------- | ---------------------------------------- | ------------------------------------ |
| 异步业务处理     | 主流程快速返回，非核心逻辑异步执行       | 登录日志记录、操作日志落库、消息通知 |
| 批量数据处理     | 将大任务拆分为多个小任务并发执行         | 批量导入、批量同步、批量计算         |
| 第三方接口调用   | 并发调用外部系统，减少整体等待时间       | 查询多个供应商价格、同步多平台状态   |
| 定时任务并发执行 | 定时任务内部拆分子任务，提高吞吐         | 每天凌晨批量生成报表                 |
| 文件处理         | 文件解析、上传后处理、图片压缩等耗时任务 | Excel 导入、图片转码、文件扫描       |
| 消息消费         | 控制消息处理并发度                       | MQ 消费后异步处理业务逻辑            |
| 非核心通知       | 不影响主交易链路的通知任务               | 邮件、短信、站内信、Webhook 推送     |

使用线程池时需要注意，线程池并不能无限提升性能。当数据库连接池、Redis 连接池、HTTP 连接池、CPU、内存或磁盘 IO 已经达到瓶颈时，盲目增加线程数会导致上下文切换增加、响应时间变长，甚至引发雪崩。

### 设计目标

线程池设计的核心目标是“可控并发、任务隔离、稳定运行、便于观测”。在 Spring Boot 3 项目中，建议将线程池作为基础设施能力统一配置，而不是在业务代码中零散创建。

主要设计目标如下：

| 目标         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 控制并发规模 | 通过核心线程数、最大线程数、队列容量限制并发任务数量         |
| 保护系统资源 | 避免瞬时流量导致线程无限创建，保护 CPU、内存、数据库连接池和外部接口 |
| 隔离业务任务 | 不同类型任务使用不同线程池，避免低优先级任务拖垮核心业务     |
| 统一线程命名 | 通过线程名前缀快速定位任务来源，便于日志排查                 |
| 明确拒绝策略 | 当线程池满载时按业务要求降级、抛错、回退或调用方执行         |
| 支持优雅关闭 | 应用停止时尽量等待已提交任务执行完成，避免数据丢失           |
| 支持监控排查 | 暴露线程池运行状态，如活跃线程数、队列长度、已完成任务数等   |

在 Spring Boot 中，如果上下文中没有自定义 `Executor`，框架会自动配置一个 `AsyncTaskExecutor`；未启用虚拟线程时通常使用 `ThreadPoolTaskExecutor`，该执行器可用于 `@EnableAsync` 异步任务、Spring MVC 异步请求处理、WebFlux 阻塞任务支持等集成场景。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

### 核心参数说明

线程池参数决定了任务提交后的执行路径。Spring 的 `ThreadPoolTaskExecutor` 以 JavaBean 风格封装了 JDK `ThreadPoolExecutor`，支持配置 `corePoolSize`、`maxPoolSize`、`keepAliveSeconds`、`queueCapacity` 等参数，并可暴露运行期状态用于管理和监控。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/concurrent/ThreadPoolTaskExecutor.html?utm_source=chatgpt.com))

| 参数                               | 说明                                           | 建议                                                      |
| ---------------------------------- | ---------------------------------------------- | --------------------------------------------------------- |
| `corePoolSize`                     | 核心线程数，线程池长期保留的基础线程数量       | CPU 密集型任务可接近 CPU 核心数，IO 密集型任务可适当放大  |
| `maxPoolSize`                      | 最大线程数，当队列满后可扩容到该数量           | 不应设置过大，需要结合数据库连接池、HTTP 连接池等资源上限 |
| `queueCapacity`                    | 队列容量，核心线程都在忙时，新任务进入队列等待 | 不建议无限大，否则问题会被堆积到内存和延迟上              |
| `keepAliveSeconds`                 | 非核心线程空闲存活时间                         | 突发流量场景可设置 30~120 秒                              |
| `threadNamePrefix`                 | 线程名前缀                                     | 建议带业务含义，如 `order-async-`、`notify-async-`        |
| `rejectedExecutionHandler`         | 拒绝策略，线程和队列都满时触发                 | 按业务选择抛错、调用方执行、丢弃或自定义降级              |
| `waitForTasksToCompleteOnShutdown` | 应用关闭时是否等待任务完成                     | 对数据一致性要求高的任务建议开启                          |
| `awaitTerminationSeconds`          | 关闭时最大等待秒数                             | 根据任务最长执行时间设置，避免应用长期无法退出            |

任务提交后的典型执行规则如下：

1. 当前运行线程数小于 `corePoolSize` 时，优先创建核心线程执行任务。
2. 当前运行线程数达到 `corePoolSize` 后，新任务进入队列等待。
3. 队列已满且运行线程数小于 `maxPoolSize` 时，创建非核心线程执行任务。
4. 线程数达到 `maxPoolSize` 且队列已满时，触发拒绝策略。

常见拒绝策略如下：

| 拒绝策略              | 说明                             | 适用场景                       |
| --------------------- | -------------------------------- | ------------------------------ |
| `AbortPolicy`         | 直接抛出异常                     | 默认推荐，便于快速发现系统过载 |
| `CallerRunsPolicy`    | 由提交任务的线程自己执行         | 希望反压调用方，降低提交速度   |
| `DiscardPolicy`       | 直接丢弃新任务，不抛异常         | 非关键任务，如低价值埋点       |
| `DiscardOldestPolicy` | 丢弃队列中最旧任务，再提交新任务 | 只关心最新任务的场景           |
| 自定义策略            | 记录日志、告警、降级、落库补偿   | 关键异步任务、需要审计的任务   |

## 环境与依赖

本章节用于定义项目运行环境、Maven 依赖和配置文件结构。后续线程池初始化、异步任务、异常处理和优雅关闭章节都应基于这里的环境和配置展开。

### Spring Boot 版本说明

本文档建议使用 Spring Boot 3.x 作为基础版本。Spring 官方参考文档当前同时列出了稳定版本线，其中包含 `3.5.13`、`3.4.13`、`3.3.13` 等 Spring Boot 3 稳定版本线；如果项目仍处于 Spring Boot 3 技术栈内，建议优先选择当前维护中的 3.x 小版本，而不是混用 Spring Boot 2.x 配置方式。([Home](https://docs.spring.io/spring-boot/reference/index.html?utm_source=chatgpt.com))

推荐环境如下：

| 组件             | 推荐版本           | 说明                                                         |
| ---------------- | ------------------ | ------------------------------------------------------------ |
| JDK              | 17 或更高          | Spring Boot 3.x 基于 Java 17 作为最低门槛；Spring Boot 3.0 文档已要求 Java 17，3.3 企业文档也说明至少需要 Java 17。([Home](https://docs.spring.io/spring-boot/docs/3.0.5/reference/html/getting-started.html?utm_source=chatgpt.com)) |
| Spring Boot      | 3.5.x              | 适合新项目或仍停留在 Spring Boot 3 体系的项目                |
| Spring Framework | 6.x                | Spring Boot 3 对应 Spring Framework 6 体系                   |
| 构建工具         | Maven 3.6.3+       | Spring Boot 3.3 系统要求中明确支持 Maven 3.6.3 或更高版本。([Spring 企业文档](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com)) |
| 包名规范         | `io.github.atengk` | 示例代码默认使用该基础包路径                                 |
| 配置格式         | `application.yml`  | 相比 properties 更适合表达多层线程池配置                     |

建议在项目中统一约定：

```text
JDK 17+
Spring Boot 3.5.x
Maven 3.6.3+
配置文件：application.yml
基础包名：io.github.atengk
```

### 基础依赖配置

线程池本身不需要额外引入复杂依赖。Spring Boot 基础依赖已经包含 Spring Core、Spring Context 等能力，可以直接使用 `ThreadPoolTaskExecutor`、`@Async`、`@EnableAsync` 等组件。

文件位置：`pom.xml`

下面的 Maven 配置用于 Spring Boot 3 项目基础运行、接口验证、参数校验、监控和工具类支持。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <!-- Spring Boot 3 父工程，统一管理依赖版本 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.13</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-boot-thread-pool-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-boot-thread-pool-demo</name>
    <description>Spring Boot 3 线程池示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低建议使用 JDK 17 -->
        <java.version>17</java.version>
        <!-- Hutool 工具类版本，可用于字符串、集合、时间、异常等通用处理 -->
        <hutool.version>5.8.40</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 场景依赖，用于提供 Controller 接口验证线程池效果 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验依赖，用于后续接口参数和配置参数校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Actuator 监控依赖，可用于后续暴露应用健康检查和运行状态 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Spring Boot 配置元数据生成器，便于自定义配置类在 IDE 中提示 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Hutool 工具类，后续业务示例可用于日期、集合、字符串和异常处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok 简化日志对象、构造方法、Getter、Setter 等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖，用于后续线程池参数和异步任务验证 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <!-- Spring Boot Maven 插件，用于打包可执行 Jar -->
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

如果项目只需要异步任务能力，不需要提供 HTTP 接口，可以去掉 `spring-boot-starter-web`。如果需要通过接口触发异步任务并观察日志，保留 `spring-boot-starter-web` 更方便。

### 配置文件结构

线程池配置建议集中放在 `application.yml` 中，并使用业务前缀进行隔离，例如 `app.thread-pool`。这样可以避免业务线程池参数与 Spring Boot 内置的 `spring.task.execution` 配置混在一起，也便于后续扩展多个线程池。

推荐目录结构如下：

```text
spring-boot-thread-pool-demo
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── io
    │   │       └── github
    │   │           └── atengk
    │   │               ├── ThreadPoolApplication.java
    │   │               ├── config
    │   │               │   ├── ThreadPoolProperties.java
    │   │               │   └── ThreadPoolConfig.java
    │   │               ├── controller
    │   │               │   └── AsyncTaskController.java
    │   │               └── service
    │   │                   ├── AsyncTaskService.java
    │   │                   └── impl
    │   │                       └── AsyncTaskServiceImpl.java
    │   └── resources
    │       └── application.yml
    └── test
        └── java
            └── io
                └── github
                    └── atengk
                        └── ThreadPoolApplicationTests.java
```

文件位置：`src/main/resources/application.yml`

下面的配置用于定义应用端口、日志级别、线程池参数和优雅关闭参数，后续 `ThreadPoolProperties` 可直接绑定 `app.thread-pool` 节点。

```yaml
server:
  # 应用服务端口
  port: 8080

spring:
  application:
    # 应用名称，建议和日志、链路追踪中的服务名保持一致
    name: spring-boot-thread-pool-demo

  lifecycle:
    # Spring 容器关闭时等待生命周期组件完成的最长时间
    timeout-per-shutdown-phase: 30s

management:
  endpoints:
    web:
      exposure:
        # 暴露基础监控端点，后续可用于观察应用健康状态
        include: health,info,metrics
  endpoint:
    health:
      # 展示健康检查明细
      show-details: always

logging:
  level:
    # 示例项目基础日志级别
    io.github.atengk: info
    org.springframework.scheduling: info

app:
  thread-pool:
    # 通用异步线程池，适合普通异步业务任务
    common:
      thread-name-prefix: common-async-
      core-pool-size: 8
      max-pool-size: 16
      queue-capacity: 200
      keep-alive-seconds: 60
      await-termination-seconds: 30
      wait-for-tasks-to-complete-on-shutdown: true

    # 通知线程池，适合短信、邮件、Webhook 等非核心通知任务
    notify:
      thread-name-prefix: notify-async-
      core-pool-size: 4
      max-pool-size: 8
      queue-capacity: 500
      keep-alive-seconds: 60
      await-termination-seconds: 20
      wait-for-tasks-to-complete-on-shutdown: true

    # 批处理线程池，适合批量导入、批量计算、批量同步等任务
    batch:
      thread-name-prefix: batch-async-
      core-pool-size: 6
      max-pool-size: 12
      queue-capacity: 100
      keep-alive-seconds: 120
      await-termination-seconds: 60
      wait-for-tasks-to-complete-on-shutdown: true
```

配置结构建议遵循以下原则：

| 配置节点                                 | 说明                                 |
| ---------------------------------------- | ------------------------------------ |
| `app.thread-pool.common`                 | 通用异步任务线程池                   |
| `app.thread-pool.notify`                 | 通知类任务线程池                     |
| `app.thread-pool.batch`                  | 批处理任务线程池                     |
| `thread-name-prefix`                     | 线程名前缀，必须体现业务含义         |
| `core-pool-size`                         | 核心线程数                           |
| `max-pool-size`                          | 最大线程数                           |
| `queue-capacity`                         | 队列容量                             |
| `keep-alive-seconds`                     | 非核心线程空闲存活时间               |
| `await-termination-seconds`              | 应用关闭时等待任务完成的最长秒数     |
| `wait-for-tasks-to-complete-on-shutdown` | 关闭应用时是否等待已提交任务执行完成 |

不建议直接将所有任务都丢到一个默认线程池中。对于通知、批处理、报表、外部接口调用等耗时或不稳定任务，应拆分独立线程池，避免某一类任务阻塞后影响其他异步业务。



## 线程池配置设计

本章节用于完成线程池配置类、参数绑定类、线程池 Bean 初始化、线程命名和拒绝策略设计。Spring 的 `ThreadPoolTaskExecutor` 本质上是对 JDK `ThreadPoolExecutor` 的 Spring 风格封装，支持通过 `corePoolSize`、`maxPoolSize`、`keepAliveSeconds`、`queueCapacity` 等属性进行配置，并可暴露 `poolSize`、`activeCount` 等运行状态用于监控排查。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/concurrent/ThreadPoolTaskExecutor.html?utm_source=chatgpt.com))

### 线程池参数配置

线程池参数建议从配置文件读取，不建议在配置类中写死。这样可以在不同环境中通过 `application-dev.yml`、`application-prod.yml` 或配置中心调整线程池参数，避免每次修改线程池容量都重新改代码。

文件位置：`src/main/java/io/github/atengk/config/ThreadPoolProperties.java`

下面的配置属性类用于绑定前面 `application.yml` 中的 `app.thread-pool` 节点，并对线程池核心参数进行基础校验。

```java
package io.github.atengk.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 线程池配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.thread-pool")
public class ThreadPoolProperties {

    /**
     * 通用异步线程池
     */
    @Valid
    private Pool common = new Pool();

    /**
     * 通知线程池
     */
    @Valid
    private Pool notify = new Pool();

    /**
     * 批处理线程池
     */
    @Valid
    private Pool batch = new Pool();

    /**
     * 单个线程池参数
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Pool {

        /**
         * 线程名前缀
         */
        @NotBlank(message = "线程名前缀不能为空")
        private String threadNamePrefix = "async-";

        /**
         * 核心线程数
         */
        @Min(value = 1, message = "核心线程数必须大于 0")
        private int corePoolSize = 4;

        /**
         * 最大线程数
         */
        @Min(value = 1, message = "最大线程数必须大于 0")
        private int maxPoolSize = 8;

        /**
         * 队列容量
         */
        @Min(value = 1, message = "队列容量必须大于 0")
        private int queueCapacity = 100;

        /**
         * 非核心线程空闲存活秒数
         */
        @Min(value = 1, message = "线程空闲存活时间必须大于 0")
        private int keepAliveSeconds = 60;

        /**
         * 关闭应用时等待任务完成的最长秒数
         */
        @Min(value = 1, message = "关闭等待时间必须大于 0")
        private int awaitTerminationSeconds = 30;

        /**
         * 关闭应用时是否等待已提交任务执行完成
         */
        private boolean waitForTasksToCompleteOnShutdown = true;

        @AssertTrue(message = "最大线程数必须大于等于核心线程数")
        public boolean isPoolSizeValid() {
            return maxPoolSize >= corePoolSize;
        }
    }
}
```

参数配置建议遵循以下约束：

| 参数                        | 约束建议                      | 说明                                       |
| --------------------------- | ----------------------------- | ------------------------------------------ |
| `core-pool-size`            | 必须大于 0                    | 过小会导致任务长期排队，过大会增加资源占用 |
| `max-pool-size`             | 必须大于等于 `core-pool-size` | 队列满后才会扩容到最大线程数               |
| `queue-capacity`            | 建议设置有限值                | 不建议使用无界队列，避免任务无限堆积       |
| `thread-name-prefix`        | 必须有业务语义                | 便于通过日志定位任务来源                   |
| `await-termination-seconds` | 根据任务耗时设置              | 防止应用关闭时任务丢失或长期阻塞           |

### ThreadPoolTaskExecutor 初始化

线程池初始化建议集中放在 `ThreadPoolConfig` 中，统一创建多个 `ThreadPoolTaskExecutor` Bean。Spring Boot 在没有自定义执行器时会自动配置 `AsyncTaskExecutor`；如果需要明确控制 `@Async` 使用的默认执行器，可以通过 `AsyncConfigurer` 指定。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/config/ThreadPoolConfig.java`

下面的配置类用于初始化通用线程池、通知线程池和批处理线程池，并将 `commonTaskExecutor` 作为默认异步执行器。

```java
package io.github.atengk.config;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolConfig implements AsyncConfigurer {

    private final ThreadPoolProperties threadPoolProperties;

    @Bean(name = {"taskExecutor", "commonTaskExecutor"})
    public ThreadPoolTaskExecutor commonTaskExecutor() {
        return buildExecutor("commonTaskExecutor", threadPoolProperties.getCommon());
    }

    @Bean(name = "notifyTaskExecutor")
    public ThreadPoolTaskExecutor notifyTaskExecutor() {
        return buildExecutor("notifyTaskExecutor", threadPoolProperties.getNotify());
    }

    @Bean(name = "batchTaskExecutor")
    public ThreadPoolTaskExecutor batchTaskExecutor() {
        return buildExecutor("batchTaskExecutor", threadPoolProperties.getBatch());
    }

    @Override
    public Executor getAsyncExecutor() {
        return commonTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> log.error(
                "异步任务执行异常，method={}，params={}",
                method.getName(),
                Arrays.toString(params),
                throwable
        );
    }

    private ThreadPoolTaskExecutor buildExecutor(String beanName, ThreadPoolProperties.Pool pool) {
        Assert.notNull(pool, "线程池配置不能为空");
        Assert.isTrue(pool.getMaxPoolSize() >= pool.getCorePoolSize(), "最大线程数必须大于等于核心线程数");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(StrUtil.blankToDefault(pool.getThreadNamePrefix(), beanName + "-"));
        executor.setCorePoolSize(pool.getCorePoolSize());
        executor.setMaxPoolSize(pool.getMaxPoolSize());
        executor.setQueueCapacity(pool.getQueueCapacity());
        executor.setKeepAliveSeconds(pool.getKeepAliveSeconds());
        executor.setWaitForTasksToCompleteOnShutdown(pool.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(pool.getAwaitTerminationSeconds());
        executor.setRejectedExecutionHandler(callerRunsWithLogPolicy(beanName));

        executor.initialize();

        log.info(
                "线程池初始化完成，beanName={}，corePoolSize={}，maxPoolSize={}，queueCapacity={}，threadNamePrefix={}",
                beanName,
                pool.getCorePoolSize(),
                pool.getMaxPoolSize(),
                pool.getQueueCapacity(),
                pool.getThreadNamePrefix()
        );
        return executor;
    }

    private RejectedExecutionHandler callerRunsWithLogPolicy(String beanName) {
        return (runnable, executor) -> {
            log.warn(
                    "线程池任务被拒绝，执行调用方线程回退策略，beanName={}，activeCount={}，poolSize={}，queueSize={}",
                    beanName,
                    executor.getActiveCount(),
                    executor.getPoolSize(),
                    executor.getQueue().size()
            );

            if (!executor.isShutdown()) {
                runnable.run();
            }
        };
    }
}
```

该配置类完成以下工作：

| 配置项                           | 说明                                     |
| -------------------------------- | ---------------------------------------- |
| `@EnableAsync`                   | 启用 Spring 异步方法执行能力             |
| `@EnableConfigurationProperties` | 启用 `ThreadPoolProperties` 配置绑定     |
| `taskExecutor`                   | 作为默认异步执行器                       |
| `commonTaskExecutor`             | 通用业务异步任务线程池                   |
| `notifyTaskExecutor`             | 通知类任务线程池                         |
| `batchTaskExecutor`              | 批处理任务线程池                         |
| `AsyncUncaughtExceptionHandler`  | 处理 `void` 返回值异步方法中的未捕获异常 |
| `RejectedExecutionHandler`       | 在线程池满载时记录日志并执行回退策略     |

### 线程命名规则

线程命名规则用于提升日志排查效率。线上排查异步任务问题时，通常需要从日志中的线程名判断任务来源，因此线程名前缀必须具备业务语义，而不是使用默认线程名。

推荐命名规则如下：

```text
业务模块-任务类型-
```

常见示例：

| 线程名前缀      | 适用任务                     |
| --------------- | ---------------------------- |
| `common-async-` | 通用异步任务                 |
| `notify-async-` | 短信、邮件、Webhook、站内信  |
| `batch-async-`  | 批量导入、批量同步、批量计算 |
| `report-async-` | 报表生成                     |
| `order-async-`  | 订单异步处理                 |
| `file-async-`   | 文件解析、文件转换、文件扫描 |

日志输出示例：

```text
2026-05-06 10:30:15.123  INFO 12345 --- [common-async-1] i.g.a.service.impl.AsyncTaskServiceImpl  : 开始记录操作日志，用户=ateng，操作=登录系统
2026-05-06 10:30:15.456  INFO 12345 --- [notify-async-2] i.g.a.service.impl.AsyncTaskServiceImpl  : 通知发送完成，userId=10001
2026-05-06 10:30:16.789  INFO 12345 --- [batch-async-1]  i.g.a.service.impl.AsyncTaskServiceImpl  : 报表生成完成，reportId=RPT202605060001
```

线程命名建议：

| 建议            | 说明                                        |
| --------------- | ------------------------------------------- |
| 不使用默认前缀  | 默认线程名缺少业务语义，不利于排查          |
| 前缀以 `-` 结尾 | 便于和线程编号自然拼接                      |
| 包含任务类型    | 如 `notify`、`batch`、`report`              |
| 不包含环境名称  | 环境信息应由日志字段或部署平台提供          |
| 不包含动态参数  | 用户 ID、订单号等动态数据不应进入线程名前缀 |

### 拒绝策略配置

拒绝策略用于处理线程池已经达到最大线程数且队列已满时的新任务。拒绝策略不能随意选择，需要结合任务的重要性、是否允许丢弃、是否允许阻塞调用方、是否需要告警来决定。

常见拒绝策略如下：

| 策略                  | 行为                           | 适用场景                   |
| --------------------- | ------------------------------ | -------------------------- |
| `AbortPolicy`         | 直接抛出异常                   | 核心任务、必须感知失败     |
| `CallerRunsPolicy`    | 调用方线程直接执行任务         | 希望通过阻塞调用方形成反压 |
| `DiscardPolicy`       | 直接丢弃任务，不抛异常         | 可丢弃的低价值任务         |
| `DiscardOldestPolicy` | 丢弃队列中最旧任务             | 只关心最新任务的场景       |
| 自定义策略            | 记录日志、告警、降级、落库补偿 | 重要异步任务、生产环境推荐 |

本文示例使用的是“记录日志 + 调用方执行”的自定义策略。它的特点是不会静默丢任务，同时可以通过调用方线程执行形成一定反压，避免任务继续无限制提交。

如果需要改成直接抛出异常，可以将初始化代码中的拒绝策略替换为：

```java
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
```

如果是通知类、埋点类等允许丢弃的任务，也可以使用：

```java
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
```

生产环境建议至少记录拒绝日志。对于核心异步任务，拒绝后应结合业务设计补偿机制，例如保存失败任务、发送告警、写入重试表或投递到 MQ 延迟重试队列。

## 异步任务支持

本章节用于说明如何启用异步执行、如何使用 `@Async`、如何指定线程池以及如何处理异步方法返回值。`@Async` 可以标注在方法或类上，被标注的方法会作为异步执行候选方法；其返回值类型主要支持 `void` 和 `Future`，实际项目中通常使用 `CompletableFuture` 处理有返回值的异步任务。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Async.html?utm_source=chatgpt.com))

### 启用异步执行

Spring 异步执行需要通过 `@EnableAsync` 开启。本文已经在 `ThreadPoolConfig` 中添加了 `@EnableAsync`，因此不需要在启动类中重复添加。

文件位置：`src/main/java/io/github/atengk/config/ThreadPoolConfig.java`

```java
@Configuration
@EnableAsync
public class ThreadPoolConfig implements AsyncConfigurer {
}
```

启用异步后，Spring 会通过代理拦截带有 `@Async` 的方法调用，并将方法提交到对应的执行器中运行。需要注意，异步方法必须通过 Spring Bean 代理对象调用才会生效，同一个类中的方法直接互相调用不会触发异步代理。

推荐调用方式：

```text
Controller -> Service Bean -> @Async 方法
```

不推荐调用方式：

```text
同一个 Service 类内部 this.xxxAsyncMethod()
```

### @Async 使用方式

`@Async` 可以直接标注在 Service 方法上。无参数的 `@Async` 使用默认异步执行器；带参数的 `@Async("beanName")` 会使用指定名称的线程池。`@Async` 的 `value` 可以用于匹配指定的 `Executor` 或 `TaskExecutor` Bean 名称。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Async.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/service/AsyncTaskService.java`

下面的接口定义了三类异步任务：无返回值任务、有返回值任务和批量通知任务。

```java
package io.github.atengk.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步任务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface AsyncTaskService {

    void recordOperationLog(String username, String action);

    CompletableFuture<String> buildReport(String reportId);

    void sendNotify(List<String> userIds, String message);
}
```

文件位置：`src/main/java/io/github/atengk/service/impl/AsyncTaskServiceImpl.java`

下面的实现类演示了默认线程池、批处理线程池和通知线程池的使用方式。

```java
package io.github.atengk.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.service.AsyncTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步任务服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class AsyncTaskServiceImpl implements AsyncTaskService {

    @Override
    @Async
    public void recordOperationLog(String username, String action) {
        String threadName = Thread.currentThread().getName();
        log.info("开始记录操作日志，用户={}，操作={}，线程={}", username, action, threadName);

        if (StrUtil.isBlank(username)) {
            log.warn("操作日志记录跳过，用户名为空，操作={}", action);
            return;
        }

        ThreadUtil.sleep(500);
        log.info("操作日志记录完成，用户={}，操作={}，时间={}", username, action, DateUtil.now());
    }

    @Override
    @Async("batchTaskExecutor")
    public CompletableFuture<String> buildReport(String reportId) {
        String threadName = Thread.currentThread().getName();
        log.info("开始生成报表，reportId={}，线程={}", reportId, threadName);

        if (StrUtil.isBlank(reportId)) {
            log.warn("报表生成失败，报表ID为空");
            return CompletableFuture.failedFuture(new IllegalArgumentException("报表ID不能为空"));
        }

        ThreadUtil.sleep(2000);
        String result = StrUtil.format("报表生成完成，reportId={}，完成时间={}", reportId, DateUtil.now());
        log.info(result);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    @Async("notifyTaskExecutor")
    public void sendNotify(List<String> userIds, String message) {
        String threadName = Thread.currentThread().getName();
        log.info("开始发送通知，用户数量={}，线程={}", CollUtil.size(userIds), threadName);

        if (CollUtil.isEmpty(userIds)) {
            log.warn("通知发送跳过，用户列表为空");
            return;
        }

        for (String userId : userIds) {
            ThreadUtil.sleep(100);
            log.info("通知发送完成，userId={}，message={}", userId, message);
        }
    }
}
```

`@Async` 使用注意事项：

| 注意事项                           | 说明                                                |
| ---------------------------------- | --------------------------------------------------- |
| 建议标注在 Service 层              | Controller 层只负责接收请求，不建议承载异步业务逻辑 |
| 避免同类内部调用                   | 同类内部调用不会经过 Spring 代理                    |
| 避免标注在 `private` 方法上        | 异步代理无法正常拦截私有方法                        |
| 不建议用于强一致主链路             | 异步任务失败可能不会影响主请求返回                  |
| 有返回值时使用 `CompletableFuture` | 便于组合、回调和异常处理                            |
| 无返回值时记录异常日志             | `void` 异步方法异常需要统一处理                     |

### 指定线程池执行任务

指定线程池的核心方式是在 `@Async` 中写入线程池 Bean 名称。本文中已经定义了三个线程池 Bean：`commonTaskExecutor`、`notifyTaskExecutor`、`batchTaskExecutor`。

示例：

```java
@Async("notifyTaskExecutor")
public void sendNotify(List<String> userIds, String message) {
    // 使用通知线程池执行
}
@Async("batchTaskExecutor")
public CompletableFuture<String> buildReport(String reportId) {
    // 使用批处理线程池执行
}
@Async
public void recordOperationLog(String username, String action) {
    // 使用默认线程池执行，即 commonTaskExecutor
}
```

线程池选择建议如下：

| 任务类型            | 推荐线程池                                      | 原因                             |
| ------------------- | ----------------------------------------------- | -------------------------------- |
| 操作日志、审计日志  | `commonTaskExecutor`                            | 任务较轻，通用线程池即可         |
| 短信、邮件、Webhook | `notifyTaskExecutor`                            | 外部接口不稳定，应和核心任务隔离 |
| 批量导入、批量同步  | `batchTaskExecutor`                             | 任务耗时长，应独立控制并发       |
| 报表生成            | `batchTaskExecutor` 或独立 `reportTaskExecutor` | 报表任务通常 CPU 和 IO 消耗较高  |
| 文件解析            | 独立 `fileTaskExecutor`                         | 文件任务耗时不可控，建议隔离     |

### 异步方法返回值处理

异步方法如果不需要返回结果，可以使用 `void`；如果需要返回执行结果，建议使用 `CompletableFuture<T>`。Spring `@Async` 对返回值有约束，主要支持 `void` 或 `Future`，其中 `CompletableFuture` 是更常用的具体类型。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Async.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/controller/AsyncTaskController.java`

下面的 Controller 用于验证异步任务提交、指定线程池执行和异步返回值处理。

```java
package io.github.atengk.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import io.github.atengk.dto.NotifyRequest;
import io.github.atengk.service.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * 异步任务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/async-tasks")
public class AsyncTaskController {

    private final AsyncTaskService asyncTaskService;

    @PostMapping("/logs")
    public Dict recordLog(@RequestParam(defaultValue = "ateng") String username,
                          @RequestParam(defaultValue = "登录系统") String action) {
        asyncTaskService.recordOperationLog(username, action);
        log.info("操作日志异步任务已提交，username={}，action={}", username, action);

        return Dict.create()
                .set("message", "操作日志异步任务已提交")
                .set("time", DateUtil.now());
    }

    @PostMapping("/notify")
    public Dict sendNotify(@RequestBody NotifyRequest request) {
        asyncTaskService.sendNotify(request.userIds(), request.message());
        log.info("通知异步任务已提交，userCount={}", request.userIds() == null ? 0 : request.userIds().size());

        return Dict.create()
                .set("message", "通知异步任务已提交")
                .set("time", DateUtil.now());
    }

    @GetMapping("/reports/{reportId}")
    public CompletableFuture<Dict> buildReport(@PathVariable String reportId) {
        log.info("报表异步任务已提交，reportId={}", reportId);

        return asyncTaskService.buildReport(reportId)
                .thenApply(result -> Dict.create()
                        .set("message", result)
                        .set("time", DateUtil.now()))
                .exceptionally(throwable -> Dict.create()
                        .set("message", "报表生成失败")
                        .set("error", throwable.getMessage())
                        .set("time", DateUtil.now()));
    }
}
```

文件位置：`src/main/java/io/github/atengk/dto/NotifyRequest.java`

下面的请求对象用于接收通知任务参数。

```java
package io.github.atengk.dto;

import java.util.List;

/**
 * 通知请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
public record NotifyRequest(
        List<String> userIds,
        String message
) {
}
```

接口调用示例：

```bash
# 提交操作日志异步任务
curl -X POST "http://localhost:8080/api/async-tasks/logs?username=ateng&action=登录系统"

# 提交通知异步任务
curl -X POST "http://localhost:8080/api/async-tasks/notify" \
  -H "Content-Type: application/json" \
  -d '{"userIds":["10001","10002","10003"],"message":"系统维护通知"}'

# 提交报表生成异步任务，并等待 CompletableFuture 返回结果
curl -X GET "http://localhost:8080/api/async-tasks/reports/RPT202605060001"
```

预期返回示例：

```json
{
  "message": "操作日志异步任务已提交",
  "time": "2026-05-06 10:30:15"
}
{
  "message": "通知异步任务已提交",
  "time": "2026-05-06 10:30:16"
}
{
  "message": "报表生成完成，reportId=RPT202605060001，完成时间=2026-05-06 10:30:18",
  "time": "2026-05-06 10:30:18"
}
```

验证时重点观察日志中的线程名前缀：

```text
common-async-1
notify-async-1
batch-async-1
```

如果日志中出现上述线程名前缀，说明异步任务已经进入对应线程池执行。



## 业务代码集成

本章节用于说明线程池在真实业务代码中的集成方式。推荐做法是 Controller 只负责接收请求和返回结果，Service 负责主业务流程，异步任务统一下沉到独立的异步服务中，避免在 Controller 中直接编写线程池提交逻辑。

### Service 层异步调用

Service 层异步调用通常用于主业务完成后触发非核心任务。例如订单创建成功后，可以异步记录操作日志、发送通知、生成统计任务或同步外部系统。主流程只保证核心数据处理成功，异步任务失败不应直接影响主接口返回。

推荐调用链路如下：

```text
Controller
  -> OrderService.createOrder()
      -> 保存订单核心数据
      -> asyncTaskService.recordOperationLog()
      -> asyncTaskService.sendNotify()
      -> 返回订单创建结果
```

注意不要在同一个 Service 类内部通过 `this.xxxAsyncMethod()` 调用异步方法，因为这种调用不会经过 Spring 代理，`@Async` 不会生效。建议将异步方法放到独立的 `AsyncTaskService` 中，由业务 Service 注入后调用。

文件位置：`src/main/java/io/github/atengk/dto/OrderCreateRequest.java`

下面的请求对象用于接收订单创建参数。

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * 订单创建请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
public record OrderCreateRequest(

        @NotBlank(message = "用户ID不能为空")
        String userId,

        @NotBlank(message = "商品名称不能为空")
        String productName,

        @Min(value = 1, message = "购买数量必须大于0")
        Integer quantity,

        BigDecimal amount
) {
}
```

文件位置：`src/main/java/io/github/atengk/vo/OrderCreateVO.java`

下面的返回对象用于返回订单创建结果。

```java
package io.github.atengk.vo;

import java.math.BigDecimal;

/**
 * 订单创建返回结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
public record OrderCreateVO(
        String orderId,
        String userId,
        String productName,
        Integer quantity,
        BigDecimal amount,
        String status
) {
}
```

文件位置：`src/main/java/io/github/atengk/service/OrderService.java`

下面的业务接口定义订单创建能力，异步任务由实现类内部触发。

```java
package io.github.atengk.service;

import io.github.atengk.dto.OrderCreateRequest;
import io.github.atengk.vo.OrderCreateVO;

/**
 * 订单业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface OrderService {

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建结果
     */
    OrderCreateVO createOrder(OrderCreateRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/service/impl/OrderServiceImpl.java`

下面的实现类演示主业务流程完成后，异步调用日志任务和通知任务。

```java
package io.github.atengk.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.dto.OrderCreateRequest;
import io.github.atengk.service.AsyncTaskService;
import io.github.atengk.service.OrderService;
import io.github.atengk.vo.OrderCreateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单业务服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final AsyncTaskService asyncTaskService;

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单创建结果
     */
    @Override
    public OrderCreateVO createOrder(OrderCreateRequest request) {
        Assert.notNull(request, "订单创建参数不能为空");
        Assert.notBlank(request.userId(), "用户ID不能为空");
        Assert.notBlank(request.productName(), "商品名称不能为空");

        String orderId = StrUtil.format("ORD{}", IdUtil.getSnowflakeNextId());
        BigDecimal amount = request.amount() == null ? BigDecimal.ZERO : request.amount();

        log.info("开始创建订单，orderId={}，userId={}，productName={}，quantity={}",
                orderId, request.userId(), request.productName(), request.quantity());

        // 示例中省略数据库落库逻辑，实际项目应在这里保存订单主表、明细表、流水表等核心数据
        OrderCreateVO result = new OrderCreateVO(
                orderId,
                request.userId(),
                request.productName(),
                request.quantity(),
                amount,
                "CREATED"
        );

        log.info("订单核心流程处理完成，orderId={}，time={}", orderId, DateUtil.now());

        // 异步记录操作日志，使用默认 commonTaskExecutor
        asyncTaskService.recordOperationLog(request.userId(), StrUtil.format("创建订单：{}", orderId));

        // 异步发送通知，使用 notifyTaskExecutor
        asyncTaskService.sendNotify(
                List.of(request.userId()),
                StrUtil.format("您的订单 {} 已创建成功", orderId)
        );

        return result;
    }
}
```

这种集成方式的关键点是：订单创建作为主流程同步执行，日志和通知作为异步任务执行。即使通知任务较慢，也不会拖慢订单创建接口的响应时间。

### Controller 层调用示例

Controller 层不直接关心线程池，也不直接调用 `ThreadPoolTaskExecutor`。它只调用业务 Service，由 Service 决定是否触发异步任务。这样可以保持接口层简洁，避免线程池细节泄漏到 Controller。

文件位置：`src/main/java/io/github/atengk/controller/OrderController.java`

下面的接口用于验证订单创建后触发异步任务的效果。

```java
package io.github.atengk.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import io.github.atengk.dto.OrderCreateRequest;
import io.github.atengk.service.OrderService;
import io.github.atengk.vo.OrderCreateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 创建结果
     */
    @PostMapping
    public Dict createOrder(@Valid @RequestBody OrderCreateRequest request) {
        log.info("收到创建订单请求，userId={}，productName={}", request.userId(), request.productName());

        OrderCreateVO result = orderService.createOrder(request);

        return Dict.create()
                .set("message", "订单创建成功")
                .set("data", result)
                .set("time", DateUtil.now());
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "10001",
    "productName": "Spring Boot 线程池实战课程",
    "quantity": 1,
    "amount": 99.00
  }'
```

预期响应示例：

```json
{
  "message": "订单创建成功",
  "data": {
    "orderId": "ORD1985563320012345344",
    "userId": "10001",
    "productName": "Spring Boot 线程池实战课程",
    "quantity": 1,
    "amount": 99.00,
    "status": "CREATED"
  },
  "time": "2026-05-06 10:30:15"
}
```

预期日志中可以观察到主线程和异步线程分离：

```text
http-nio-8080-exec-1 : 收到创建订单请求，userId=10001，productName=Spring Boot 线程池实战课程
http-nio-8080-exec-1 : 订单核心流程处理完成，orderId=ORD1985563320012345344
common-async-1       : 开始记录操作日志，用户=10001，操作=创建订单：ORD1985563320012345344
notify-async-1       : 开始发送通知，用户数量=1
```

如果接口已经返回，但日志和通知仍在异步线程中继续执行，说明业务代码已经正确接入线程池。

### 多线程池场景划分

多线程池的核心价值是业务隔离。不同任务的耗时、失败概率、资源占用和优先级不同，不应全部放入同一个线程池。否则批量任务可能占满队列，导致通知、日志、状态同步等任务全部阻塞。

推荐划分如下：

| 线程池         | Bean 名称            | 适用场景                         | 特点                         |
| -------------- | -------------------- | -------------------------------- | ---------------------------- |
| 通用线程池     | `commonTaskExecutor` | 操作日志、审计记录、普通异步任务 | 任务轻量，执行频率高         |
| 通知线程池     | `notifyTaskExecutor` | 短信、邮件、Webhook、站内信      | 依赖外部系统，耗时不稳定     |
| 批处理线程池   | `batchTaskExecutor`  | 批量导入、报表生成、批量同步     | 任务较重，耗时较长           |
| 文件线程池     | `fileTaskExecutor`   | 文件解析、图片压缩、文件扫描     | IO 密集，容易阻塞            |
| 外部接口线程池 | `remoteTaskExecutor` | 第三方接口调用、跨系统同步       | 失败率受外部系统影响         |
| 核心业务线程池 | `coreTaskExecutor`   | 高优先级核心异步任务             | 队列容量应小，拒绝策略应严格 |

划分线程池时可以参考以下规则：

| 判断维度         | 处理建议                                     |
| ---------------- | -------------------------------------------- |
| 任务是否核心     | 核心任务单独线程池，拒绝后应记录并补偿       |
| 任务是否耗时     | 耗时任务单独线程池，避免阻塞轻量任务         |
| 是否依赖外部系统 | 外部接口任务单独隔离，防止外部故障拖垮本系统 |
| 是否允许丢弃     | 可丢弃任务可使用较低优先级线程池             |
| 是否需要顺序执行 | 严格顺序任务不适合普通并发线程池             |
| 是否需要限流     | 高并发任务应配合限流、队列容量和拒绝策略     |

在中小型项目中，通常先定义 `common`、`notify`、`batch` 三类线程池即可。随着业务增长，再拆分 `file`、`remote`、`report` 等更细粒度的线程池。

## 异常处理

本章节用于说明异步任务异常捕获、线程池统一异常处理和日志记录规范。异步任务的异常不能只依赖全局 Web 异常处理器，因为异步方法通常不在原始 HTTP 请求线程中执行，异常传播路径和同步接口不同。

### 异步任务异常捕获

异步任务异常处理分为两类：`void` 返回值异步方法和 `CompletableFuture` 返回值异步方法。

`void` 返回值异步方法如果抛出未捕获异常，会进入 `AsyncUncaughtExceptionHandler`。`CompletableFuture` 返回值异步方法则建议在方法内部返回 `CompletableFuture.failedFuture()`，或者在调用方通过 `exceptionally()`、`handle()`、`whenComplete()` 处理异常。

文件位置：`src/main/java/io/github/atengk/service/impl/AsyncTaskServiceImpl.java`

下面的代码演示异步方法中的异常捕获方式，包含无返回值任务和有返回值任务两种场景。

```java
package io.github.atengk.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.service.AsyncTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步任务服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class AsyncTaskServiceImpl implements AsyncTaskService {

    /**
     * 记录操作日志
     *
     * @param username 用户名
     * @param action 操作内容
     */
    @Override
    @Async
    public void recordOperationLog(String username, String action) {
        String threadName = Thread.currentThread().getName();

        try {
            log.info("开始记录操作日志，用户={}，操作={}，线程={}", username, action, threadName);

            if (StrUtil.isBlank(username)) {
                log.warn("操作日志记录跳过，用户名为空，操作={}", action);
                return;
            }

            ThreadUtil.sleep(500);
            log.info("操作日志记录完成，用户={}，操作={}，时间={}", username, action, DateUtil.now());
        } catch (Exception exception) {
            log.error("操作日志异步任务执行失败，用户={}，操作={}，线程={}",
                    username, action, threadName, exception);

            // 根据业务需要可在这里写入失败任务表、发送告警或投递 MQ 重试
        }
    }

    /**
     * 生成报表
     *
     * @param reportId 报表ID
     * @return 报表生成结果
     */
    @Override
    @Async("batchTaskExecutor")
    public CompletableFuture<String> buildReport(String reportId) {
        String threadName = Thread.currentThread().getName();

        try {
            log.info("开始生成报表，reportId={}，线程={}", reportId, threadName);

            if (StrUtil.isBlank(reportId)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("报表ID不能为空"));
            }

            ThreadUtil.sleep(2000);

            String result = StrUtil.format("报表生成完成，reportId={}，完成时间={}", reportId, DateUtil.now());
            log.info(result);
            return CompletableFuture.completedFuture(result);
        } catch (Exception exception) {
            log.error("报表生成异步任务执行失败，reportId={}，线程={}", reportId, threadName, exception);
            return CompletableFuture.failedFuture(exception);
        }
    }

    /**
     * 发送通知
     *
     * @param userIds 用户ID列表
     * @param message 通知内容
     */
    @Override
    @Async("notifyTaskExecutor")
    public void sendNotify(List<String> userIds, String message) {
        String threadName = Thread.currentThread().getName();

        try {
            log.info("开始发送通知，用户数量={}，线程={}", CollUtil.size(userIds), threadName);

            if (CollUtil.isEmpty(userIds)) {
                log.warn("通知发送跳过，用户列表为空");
                return;
            }

            for (String userId : userIds) {
                ThreadUtil.sleep(100);
                log.info("通知发送完成，userId={}，message={}", userId, message);
            }
        } catch (Exception exception) {
            log.error("通知异步任务执行失败，用户数量={}，线程={}",
                    CollUtil.size(userIds), threadName, exception);

            // 通知类任务建议记录失败明细，后续由定时任务或 MQ 进行补偿
        }
    }
}
```

调用方处理 `CompletableFuture` 异常的示例：

```java
return asyncTaskService.buildReport(reportId)
        .thenApply(result -> Dict.create()
                .set("message", result)
                .set("time", DateUtil.now()))
        .exceptionally(throwable -> {
            log.error("报表接口处理异步结果失败，reportId={}", reportId, throwable);
            return Dict.create()
                    .set("message", "报表生成失败")
                    .set("error", throwable.getMessage())
                    .set("time", DateUtil.now());
        });
```

异常处理建议如下：

| 异步任务类型                 | 处理方式                                                    |
| ---------------------------- | ----------------------------------------------------------- |
| `void` 异步方法              | 方法内部 `try-catch` 或统一 `AsyncUncaughtExceptionHandler` |
| `CompletableFuture` 异步方法 | 返回 `failedFuture`，调用方使用 `exceptionally` 或 `handle` |
| 核心异步任务                 | 记录失败原因，并设计补偿机制                                |
| 非核心异步任务               | 记录日志即可，必要时做采样告警                              |
| 批处理异步任务               | 记录任务 ID、批次号、失败数据范围                           |

### 线程池统一异常处理

统一异常处理主要覆盖两类问题：异步方法未捕获异常和线程池拒绝任务。未捕获异常由 `AsyncUncaughtExceptionHandler` 处理，拒绝任务由 `RejectedExecutionHandler` 处理。两者都应记录线程池名称、方法名、参数、线程状态等关键上下文。

文件位置：`src/main/java/io/github/atengk/config/AsyncExceptionConfig.java`

下面的配置类用于统一处理 `void` 返回值异步方法中的未捕获异常。

```java
package io.github.atengk.config;

import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * 异步异常配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class AsyncExceptionConfig {

    /**
     * 异步未捕获异常处理器
     *
     * @return 异步异常处理器
     */
    @Bean
    public AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler() {
        return new LoggingAsyncUncaughtExceptionHandler();
    }

    /**
     * 日志型异步未捕获异常处理器
     *
     * @author Ateng
     * @since 2026-05-06
     */
    public static class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

        /**
         * 处理异步未捕获异常
         *
         * @param throwable 异常信息
         * @param method 异步方法
         * @param params 方法参数
         */
        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
            log.error(
                    "异步方法未捕获异常，class={}，method={}，params={}",
                    method.getDeclaringClass().getName(),
                    method.getName(),
                    ArrayUtil.toString(params),
                    throwable
            );
        }
    }
}
```

如果使用上面的独立异常处理器，需要在 `ThreadPoolConfig` 中注入并返回该处理器。

文件位置：`src/main/java/io/github/atengk/config/ThreadPoolConfig.java`

下面只展示需要调整的关键代码，其他线程池初始化逻辑保持不变。

```java
package io.github.atengk.config;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * 异步执行器配置适配
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
@RequiredArgsConstructor
public class AsyncExecutorConfigurer implements AsyncConfigurer {

    private final ThreadPoolConfig threadPoolConfig;
    private final AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler;

    /**
     * 获取默认异步执行器
     *
     * @return 默认异步执行器
     */
    @Override
    public Executor getAsyncExecutor() {
        return threadPoolConfig.commonTaskExecutor();
    }

    /**
     * 获取异步未捕获异常处理器
     *
     * @return 异步未捕获异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncUncaughtExceptionHandler;
    }
}
```

线程池拒绝策略也应统一处理，避免任务被静默丢弃。前面线程池初始化中使用的自定义拒绝策略可继续保留：

```java
private RejectedExecutionHandler callerRunsWithLogPolicy(String beanName) {
    return (runnable, executor) -> {
        log.warn(
                "线程池任务被拒绝，执行调用方线程回退策略，beanName={}，activeCount={}，poolSize={}，queueSize={}",
                beanName,
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getQueue().size()
        );

        if (!executor.isShutdown()) {
            runnable.run();
        }
    };
}
```

统一异常处理建议：

| 异常类型                  | 处理入口                                  | 建议动作                         |
| ------------------------- | ----------------------------------------- | -------------------------------- |
| `void` 异步方法未捕获异常 | `AsyncUncaughtExceptionHandler`           | 记录类名、方法名、参数、异常栈   |
| `CompletableFuture` 异常  | `exceptionally`、`handle`、`whenComplete` | 返回失败结果，记录业务 ID        |
| 线程池拒绝任务            | `RejectedExecutionHandler`                | 记录线程池状态，触发降级或告警   |
| 业务可预期异常            | 方法内部捕获                              | 记录业务上下文，避免污染全局异常 |
| 系统不可预期异常          | 统一异常处理器兜底                        | 记录完整异常栈，必要时告警       |

需要注意，`AsyncUncaughtExceptionHandler` 只处理 `void` 返回值异步方法中的未捕获异常；如果异步方法返回 `CompletableFuture`，异常应该通过 `CompletableFuture` 的异常回调处理。

### 日志记录规范

线程池相关日志应重点记录任务来源、业务标识、线程名称、线程池名称、执行结果和异常堆栈。日志不能只写“执行失败”，否则线上排查时无法定位是哪一个任务、哪一个用户、哪一个订单或哪一个批次失败。

推荐日志字段如下：

| 字段           | 说明             | 示例                             |
| -------------- | ---------------- | -------------------------------- |
| `traceId`      | 链路追踪 ID      | `T202605061030150001`            |
| `threadName`   | 当前线程名称     | `notify-async-1`                 |
| `executorName` | 线程池 Bean 名称 | `notifyTaskExecutor`             |
| `businessId`   | 业务 ID          | `orderId`、`reportId`、`batchId` |
| `taskType`     | 任务类型         | `SEND_NOTIFY`、`BUILD_REPORT`    |
| `status`       | 执行状态         | `START`、`SUCCESS`、`FAILED`     |
| `costMs`       | 执行耗时         | `530`                            |
| `errorMessage` | 异常摘要         | `连接第三方短信服务超时`         |

推荐日志写法：

```java
long startTime = System.currentTimeMillis();
String threadName = Thread.currentThread().getName();

try {
    log.info("异步任务开始，taskType={}，businessId={}，thread={}", taskType, businessId, threadName);

    // 执行业务逻辑

    log.info("异步任务完成，taskType={}，businessId={}，thread={}，costMs={}",
            taskType, businessId, threadName, System.currentTimeMillis() - startTime);
} catch (Exception exception) {
    log.error("异步任务失败，taskType={}，businessId={}，thread={}，costMs={}",
            taskType, businessId, threadName, System.currentTimeMillis() - startTime, exception);
}
```

日志记录建议：

| 场景             | 日志级别              | 说明                            |
| ---------------- | --------------------- | ------------------------------- |
| 任务提交成功     | `INFO`                | 记录任务类型和业务 ID           |
| 任务开始执行     | `INFO`                | 记录线程名和关键参数            |
| 任务执行完成     | `INFO`                | 记录耗时和结果摘要              |
| 参数为空但可跳过 | `WARN`                | 记录跳过原因                    |
| 线程池拒绝任务   | `WARN` 或 `ERROR`     | 核心任务建议使用 `ERROR` 并告警 |
| 异步任务异常     | `ERROR`               | 必须打印异常堆栈                |
| 高频低价值任务   | `DEBUG` 或采样 `INFO` | 避免日志量过大                  |

不推荐的日志写法：

```java
log.error("执行失败");
```

推荐的日志写法：

```java
log.error("通知异步任务执行失败，userId={}，message={}，thread={}",
        userId, message, Thread.currentThread().getName(), exception);
```

对于生产环境，建议结合日志平台按以下条件建立检索或告警规则：

```text
线程名前缀 = notify-async- AND 日志级别 = ERROR
线程名前缀 = batch-async- AND 关键词 = 线程池任务被拒绝
关键词 = 异步方法未捕获异常
关键词 = 线程池任务被拒绝
```

这样可以快速定位异步任务失败、线程池满载、队列堆积和拒绝策略触发等问题。



## 优雅关闭

本章节用于说明 Spring Boot 应用停止时线程池任务的处理方式。线程池中的任务可能正在执行，也可能还在队列中等待执行，如果应用直接退出，容易造成日志丢失、通知未发送、批处理任务中断或数据状态不一致。因此，生产环境中的线程池必须配置优雅关闭策略。

### 应用关闭时任务处理

应用关闭时，线程池需要处理三类任务：正在执行的任务、队列中等待的任务、新提交的任务。推荐策略是：应用关闭后不再接收新任务，已提交任务尽量执行完成，并设置最大等待时间，避免应用无限期阻塞。

Spring 的 `ThreadPoolTaskExecutor` 支持通过以下两个参数控制关闭行为：

| 参数                               | 说明                                 |
| ---------------------------------- | ------------------------------------ |
| `waitForTasksToCompleteOnShutdown` | 应用关闭时是否等待已提交任务执行完成 |
| `awaitTerminationSeconds`          | 等待任务完成的最大秒数               |

推荐配置如下：

```yaml
app:
  thread-pool:
    common:
      thread-name-prefix: common-async-
      core-pool-size: 8
      max-pool-size: 16
      queue-capacity: 200
      keep-alive-seconds: 60
      # 应用关闭时等待已提交任务执行完成
      wait-for-tasks-to-complete-on-shutdown: true
      # 最多等待 30 秒，避免应用长期无法退出
      await-termination-seconds: 30

    notify:
      thread-name-prefix: notify-async-
      core-pool-size: 4
      max-pool-size: 8
      queue-capacity: 500
      keep-alive-seconds: 60
      wait-for-tasks-to-complete-on-shutdown: true
      await-termination-seconds: 20

    batch:
      thread-name-prefix: batch-async-
      core-pool-size: 6
      max-pool-size: 12
      queue-capacity: 100
      keep-alive-seconds: 120
      wait-for-tasks-to-complete-on-shutdown: true
      await-termination-seconds: 60
```

在 `ThreadPoolConfig` 初始化线程池时，需要将配置项设置到 `ThreadPoolTaskExecutor` 中。

文件位置：`src/main/java/io/github/atengk/config/ThreadPoolConfig.java`

下面只展示线程池关闭相关的关键配置，完整类可沿用前面章节中的 `ThreadPoolConfig`。

```java
executor.setWaitForTasksToCompleteOnShutdown(pool.isWaitForTasksToCompleteOnShutdown());
executor.setAwaitTerminationSeconds(pool.getAwaitTerminationSeconds());
```

关闭行为建议如下：

| 任务类型     | 是否等待完成 | 建议等待时间         |
| ------------ | ------------ | -------------------- |
| 操作日志     | 是           | 10~30 秒             |
| 通知任务     | 是           | 20~60 秒             |
| 批处理任务   | 是           | 60~300 秒            |
| 报表生成     | 视业务决定   | 60~300 秒            |
| 可丢弃埋点   | 否           | 0~5 秒               |
| 核心补偿任务 | 是           | 根据最长任务耗时设置 |

如果任务执行时间较长，不建议完全依赖应用关闭时等待完成。更稳妥的方式是将任务状态持久化到数据库、Redis、MQ 或任务调度系统中，应用重启后可以继续补偿处理。

### 等待任务执行完成

为了在应用关闭时观察线程池状态，可以监听 `ContextClosedEvent`，在 Spring 容器关闭前输出线程池运行信息。这样可以判断关闭时是否仍有活跃任务或队列堆积。

文件位置：`src/main/java/io/github/atengk/listener/ThreadPoolShutdownListener.java`

下面的监听器用于在应用关闭时打印线程池状态，便于排查任务是否正常结束。

```java
package io.github.atengk.listener;

import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池关闭监听器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThreadPoolShutdownListener {

    private final Map<String, ThreadPoolTaskExecutor> taskExecutorMap;

    /**
     * 监听应用关闭事件
     *
     * @param event Spring 容器关闭事件
     */
    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        log.info("应用开始关闭，准备输出线程池状态，time={}", DateUtil.now());

        taskExecutorMap.forEach((beanName, executor) -> {
            ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();

            log.info(
                    "应用关闭前线程池状态，beanName={}，activeCount={}，poolSize={}，queueSize={}，completedTaskCount={}，taskCount={}",
                    beanName,
                    threadPoolExecutor.getActiveCount(),
                    threadPoolExecutor.getPoolSize(),
                    threadPoolExecutor.getQueue().size(),
                    threadPoolExecutor.getCompletedTaskCount(),
                    threadPoolExecutor.getTaskCount()
            );
        });
    }
}
```

应用关闭时，推荐观察以下日志：

```text
应用开始关闭，准备输出线程池状态，time=2026-05-06 10:40:00
应用关闭前线程池状态，beanName=commonTaskExecutor，activeCount=2，poolSize=8，queueSize=0，completedTaskCount=120，taskCount=122
应用关闭前线程池状态，beanName=notifyTaskExecutor，activeCount=1，poolSize=4，queueSize=3，completedTaskCount=40，taskCount=44
应用关闭前线程池状态，beanName=batchTaskExecutor，activeCount=1，poolSize=6，queueSize=0，completedTaskCount=8，taskCount=9
```

如果关闭时 `queueSize` 长期不为 0，说明仍有任务堆积，需要检查线程池容量、任务耗时、拒绝策略和业务补偿机制。

### 超时时间配置

超时时间不能随意设置过大。设置太短会导致任务来不及执行完成，设置太长会导致应用发布、重启、扩缩容时间变长。建议根据任务类型、最长执行时间、发布窗口和容器平台退出超时时间综合确定。

推荐配置策略如下：

| 任务类型     | 配置建议  | 说明                         |
| ------------ | --------- | ---------------------------- |
| 轻量异步任务 | 10~30 秒  | 如日志、状态同步             |
| 外部通知任务 | 20~60 秒  | 外部接口可能存在超时         |
| 批处理任务   | 60~300 秒 | 任务耗时较长，应结合补偿机制 |
| 报表任务     | 60~300 秒 | 不建议完全依赖关闭等待       |
| 可丢弃任务   | 0~5 秒    | 如低价值埋点、采样日志       |

除了线程池自身的关闭等待时间，还建议配置 Spring 容器关闭阶段超时时间。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  lifecycle:
    # Spring 容器关闭阶段的最大等待时间，应大于或等于主要线程池的 await-termination-seconds
    timeout-per-shutdown-phase: 60s
```

如果项目运行在 Docker、Kubernetes 或其他容器平台中，还需要确保平台给应用预留足够的退出时间。例如 Kubernetes 中的 `terminationGracePeriodSeconds` 应大于应用内部的关闭等待时间，否则容器可能被强制终止。

示例：

```yaml
spec:
  # Pod 终止时的优雅退出等待时间
  terminationGracePeriodSeconds: 90
```

配置建议：

| 配置项                                        | 建议                                   |
| --------------------------------------------- | -------------------------------------- |
| `await-termination-seconds`                   | 不应大于平台强制退出时间               |
| `spring.lifecycle.timeout-per-shutdown-phase` | 应覆盖主要生命周期组件关闭时间         |
| `terminationGracePeriodSeconds`               | 应大于 Spring 应用关闭等待时间         |
| 长任务处理                                    | 应持久化任务状态，避免只依赖内存线程池 |

## 验证与测试

本章节用于验证线程池是否按预期生效。验证重点包括接口是否能正常提交异步任务、多个请求是否并发执行、日志线程名是否符合命名规则、线程池参数是否正确加载。

### 接口调用验证

接口调用验证用于确认业务代码已经正确触发异步任务。可以通过订单创建接口、通知接口和报表生成接口分别验证默认线程池、通知线程池和批处理线程池。

启动应用：

```bash
mvn spring-boot:run
```

该命令会启动 Spring Boot 应用，默认读取 `src/main/resources/application.yml` 中的线程池配置。启动成功后，观察控制台是否输出线程池初始化日志。

调用订单创建接口：

```bash
curl -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "10001",
    "productName": "Spring Boot 线程池实战课程",
    "quantity": 1,
    "amount": 99.00
  }'
```

调用通知接口：

```bash
curl -X POST "http://localhost:8080/api/async-tasks/notify" \
  -H "Content-Type: application/json" \
  -d '{
    "userIds": ["10001", "10002", "10003"],
    "message": "系统维护通知"
  }'
```

调用报表生成接口：

```bash
curl -X GET "http://localhost:8080/api/async-tasks/reports/RPT202605060001"
```

预期结果如下：

| 接口                                  | 预期结果                                                     |
| ------------------------------------- | ------------------------------------------------------------ |
| `/api/orders`                         | 主接口快速返回，日志和通知异步执行                           |
| `/api/async-tasks/notify`             | 返回任务已提交，通知任务在 `notify-async-` 线程中执行        |
| `/api/async-tasks/reports/{reportId}` | 等待 `CompletableFuture` 返回报表生成结果                    |
| 控制台日志                            | 出现 `common-async-`、`notify-async-`、`batch-async-` 线程名 |

### 并发执行验证

并发执行验证用于确认线程池可以同时处理多个异步任务，并观察任务是否进入指定线程池。可以使用 `seq` 和 `xargs` 快速发起并发请求。

并发提交 20 个通知任务：

```bash
seq 1 20 | xargs -I {} -P 10 curl -s -X POST "http://localhost:8080/api/async-tasks/notify" \
  -H "Content-Type: application/json" \
  -d '{"userIds":["10001","10002","10003"],"message":"并发通知测试-{}"}'
```

命令说明：

| 参数          | 说明                                    |
| ------------- | --------------------------------------- |
| `seq 1 20`    | 生成 1 到 20 的数字，用于模拟 20 次请求 |
| `xargs -I {}` | 将数字替换到命令中的 `{}` 占位符        |
| `-P 10`       | 同时并发执行 10 个请求                  |
| `curl -s`     | 静默输出，减少终端噪音                  |

并发提交 30 个报表任务：

```bash
seq 1 30 | xargs -I {} -P 10 curl -s -X GET "http://localhost:8080/api/async-tasks/reports/RPT20260506{}"
```

预期观察点：

| 观察项     | 预期                                                      |
| ---------- | --------------------------------------------------------- |
| 响应结果   | 接口正常返回，无大量超时                                  |
| 日志线程名 | 通知任务进入 `notify-async-`，报表任务进入 `batch-async-` |
| 执行顺序   | 异步任务执行顺序可能与提交顺序不同                        |
| 队列堆积   | 高并发下可能出现队列等待                                  |
| 拒绝策略   | 超过线程池容量时应输出拒绝策略日志                        |

如果需要触发拒绝策略，可以临时调小配置：

```yaml
app:
  thread-pool:
    notify:
      thread-name-prefix: notify-async-
      core-pool-size: 1
      max-pool-size: 1
      queue-capacity: 1
      keep-alive-seconds: 60
      wait-for-tasks-to-complete-on-shutdown: true
      await-termination-seconds: 20
```

然后重新启动应用，再发起较高并发请求。如果日志中出现“线程池任务被拒绝”，说明拒绝策略已经生效。

### 日志输出验证

日志输出验证用于确认线程命名、任务状态、异常堆栈和拒绝策略日志是否符合规范。线程池相关日志必须能帮助定位问题，而不是只输出模糊的成功或失败信息。

推荐启动后观察初始化日志：

```text
线程池初始化完成，beanName=commonTaskExecutor，corePoolSize=8，maxPoolSize=16，queueCapacity=200，threadNamePrefix=common-async-
线程池初始化完成，beanName=notifyTaskExecutor，corePoolSize=4，maxPoolSize=8，queueCapacity=500，threadNamePrefix=notify-async-
线程池初始化完成，beanName=batchTaskExecutor，corePoolSize=6，maxPoolSize=12，queueCapacity=100，threadNamePrefix=batch-async-
```

推荐观察任务执行日志：

```text
common-async-1 : 开始记录操作日志，用户=10001，操作=创建订单：ORD1985563320012345344
notify-async-1 : 开始发送通知，用户数量=3，线程=notify-async-1
batch-async-1  : 开始生成报表，reportId=RPT202605060001，线程=batch-async-1
```

推荐观察异常日志：

```text
ERROR notify-async-1 : 通知异步任务执行失败，用户数量=3，线程=notify-async-1
ERROR batch-async-1  : 报表生成异步任务执行失败，reportId=RPT202605060001，线程=batch-async-1
WARN  http-nio-8080-exec-5 : 线程池任务被拒绝，执行调用方线程回退策略，beanName=notifyTaskExecutor，activeCount=1，poolSize=1，queueSize=1
```

日志验证清单：

| 验证项       | 判断标准                                                  |
| ------------ | --------------------------------------------------------- |
| 线程名前缀   | 是否符合 `common-async-`、`notify-async-`、`batch-async-` |
| 任务开始日志 | 是否包含任务类型、业务 ID、线程名                         |
| 任务完成日志 | 是否包含耗时和结果摘要                                    |
| 异常日志     | 是否打印异常堆栈                                          |
| 拒绝策略日志 | 是否包含线程池名称、活跃线程数、队列长度                  |
| 关闭日志     | 是否输出应用关闭前线程池状态                              |

### 线程池参数验证

线程池参数验证用于确认配置文件中的参数已经正确绑定到线程池实例。可以通过测试类、监控接口或日志三种方式验证。生产环境更建议暴露只读监控接口或接入 Micrometer 指标平台，测试环境可以直接使用 JUnit 验证。

文件位置：`src/main/java/io/github/atengk/vo/ThreadPoolStatusVO.java`

下面的返回对象用于展示线程池运行状态。

```java
package io.github.atengk.vo;

/**
 * 线程池状态返回结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
public record ThreadPoolStatusVO(
        String beanName,
        int corePoolSize,
        int maximumPoolSize,
        int poolSize,
        int activeCount,
        int queueSize,
        long completedTaskCount,
        long taskCount
) {
}
```

文件位置：`src/main/java/io/github/atengk/controller/ThreadPoolMonitorController.java`

下面的接口用于查看当前应用内所有 `ThreadPoolTaskExecutor` 的运行状态。

```java
package io.github.atengk.controller;

import io.github.atengk.vo.ThreadPoolStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池监控接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/thread-pools")
public class ThreadPoolMonitorController {

    private final Map<String, ThreadPoolTaskExecutor> taskExecutorMap;

    /**
     * 查询线程池状态
     *
     * @return 线程池状态列表
     */
    @GetMapping
    public List<ThreadPoolStatusVO> listThreadPoolStatus() {
        log.info("查询线程池状态，count={}", taskExecutorMap.size());

        return taskExecutorMap.entrySet()
                .stream()
                .map(entry -> buildStatus(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ThreadPoolStatusVO::beanName))
                .toList();
    }

    /**
     * 构建线程池状态
     *
     * @param beanName 线程池 Bean 名称
     * @param executor Spring 线程池执行器
     * @return 线程池状态
     */
    private ThreadPoolStatusVO buildStatus(String beanName, ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();

        return new ThreadPoolStatusVO(
                beanName,
                threadPoolExecutor.getCorePoolSize(),
                threadPoolExecutor.getMaximumPoolSize(),
                threadPoolExecutor.getPoolSize(),
                threadPoolExecutor.getActiveCount(),
                threadPoolExecutor.getQueue().size(),
                threadPoolExecutor.getCompletedTaskCount(),
                threadPoolExecutor.getTaskCount()
        );
    }
}
```

调用监控接口：

```bash
curl -X GET "http://localhost:8080/api/thread-pools"
```

预期响应示例：

```json
[
  {
    "beanName": "batchTaskExecutor",
    "corePoolSize": 6,
    "maximumPoolSize": 12,
    "poolSize": 1,
    "activeCount": 1,
    "queueSize": 0,
    "completedTaskCount": 8,
    "taskCount": 9
  },
  {
    "beanName": "commonTaskExecutor",
    "corePoolSize": 8,
    "maximumPoolSize": 16,
    "poolSize": 1,
    "activeCount": 0,
    "queueSize": 0,
    "completedTaskCount": 20,
    "taskCount": 20
  },
  {
    "beanName": "notifyTaskExecutor",
    "corePoolSize": 4,
    "maximumPoolSize": 8,
    "poolSize": 4,
    "activeCount": 2,
    "queueSize": 6,
    "completedTaskCount": 50,
    "taskCount": 58
  }
]
```

文件位置：`src/test/java/io/github/atengk/ThreadPoolApplicationTests.java`

下面的测试类用于验证线程池 Bean 是否存在，以及核心参数是否符合配置。

```java
package io.github.atengk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 线程池配置测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
class ThreadPoolApplicationTests {

    private final ThreadPoolTaskExecutor commonTaskExecutor;
    private final ThreadPoolTaskExecutor notifyTaskExecutor;
    private final ThreadPoolTaskExecutor batchTaskExecutor;

    ThreadPoolApplicationTests(@Qualifier("commonTaskExecutor") ThreadPoolTaskExecutor commonTaskExecutor,
                               @Qualifier("notifyTaskExecutor") ThreadPoolTaskExecutor notifyTaskExecutor,
                               @Qualifier("batchTaskExecutor") ThreadPoolTaskExecutor batchTaskExecutor) {
        this.commonTaskExecutor = commonTaskExecutor;
        this.notifyTaskExecutor = notifyTaskExecutor;
        this.batchTaskExecutor = batchTaskExecutor;
    }

    /**
     * 验证线程池 Bean 是否创建成功
     */
    @Test
    void shouldCreateThreadPoolBeans() {
        assertThat(commonTaskExecutor).isNotNull();
        assertThat(notifyTaskExecutor).isNotNull();
        assertThat(batchTaskExecutor).isNotNull();
    }

    /**
     * 验证通用线程池参数
     */
    @Test
    void shouldLoadCommonThreadPoolProperties() {
        assertThat(commonTaskExecutor.getCorePoolSize()).isEqualTo(8);
        assertThat(commonTaskExecutor.getMaxPoolSize()).isEqualTo(16);
        assertThat(commonTaskExecutor.getThreadNamePrefix()).isEqualTo("common-async-");
    }

    /**
     * 验证通知线程池参数
     */
    @Test
    void shouldLoadNotifyThreadPoolProperties() {
        assertThat(notifyTaskExecutor.getCorePoolSize()).isEqualTo(4);
        assertThat(notifyTaskExecutor.getMaxPoolSize()).isEqualTo(8);
        assertThat(notifyTaskExecutor.getThreadNamePrefix()).isEqualTo("notify-async-");
    }

    /**
     * 验证批处理线程池参数
     */
    @Test
    void shouldLoadBatchThreadPoolProperties() {
        assertThat(batchTaskExecutor.getCorePoolSize()).isEqualTo(6);
        assertThat(batchTaskExecutor.getMaxPoolSize()).isEqualTo(12);
        assertThat(batchTaskExecutor.getThreadNamePrefix()).isEqualTo("batch-async-");
    }
}
```

执行测试：

```bash
mvn test
```

验证通过标准如下：

| 验证项      | 通过标准                                                     |
| ----------- | ------------------------------------------------------------ |
| 应用启动    | 无配置绑定异常                                               |
| 线程池 Bean | `commonTaskExecutor`、`notifyTaskExecutor`、`batchTaskExecutor` 均存在 |
| 参数绑定    | 核心线程数、最大线程数、线程名前缀与 `application.yml` 一致  |
| 接口调用    | 异步接口正常返回                                             |
| 并发执行    | 多个任务可并发执行                                           |
| 日志输出    | 能看到指定线程名前缀                                         |
| 优雅关闭    | 应用关闭时输出线程池状态并等待任务结束                       |

最终上线前建议至少完成以下检查：

```text
1. 所有线程池均设置有限队列容量
2. 所有线程池均设置明确线程名前缀
3. 所有线程池均设置拒绝策略
4. 重要异步任务具备失败日志和补偿机制
5. 应用关闭时启用任务等待策略
6. 线程池参数经过并发压测验证
7. 生产环境接入日志检索或监控告警
```