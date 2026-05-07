# Spring 定时调度开发

本文档用于说明在 Spring Boot 3 项目中如何使用 Spring 原生定时调度能力完成周期性任务开发。内容包括定时调度的作用、适用业务场景、Spring Boot 3 中的支持方式，以及项目开发前需要准备的依赖、配置和目录结构。

## 功能概述

本章节主要说明 Spring 定时调度在后端系统中的定位。定时调度通常用于处理不需要用户手动触发、但需要按照固定时间规则自动执行的后台任务。

### 定时调度的作用

定时调度是指系统按照指定的时间规则，自动触发某段业务逻辑执行。它不依赖用户请求，也不需要人工干预，适合处理周期性、批量化、后台化的业务任务。

在 Spring Boot 3 项目中，定时调度通常用于以下目标：

| 作用             | 说明                                         |
| ---------------- | -------------------------------------------- |
| 自动执行业务任务 | 按照固定时间或固定间隔自动执行指定方法       |
| 减少人工操作     | 将重复性后台操作交给系统自动完成             |
| 维护系统数据     | 定期清理、刷新、同步或修复业务数据           |
| 提升系统稳定性   | 通过定时检查、补偿、告警等方式发现和处理异常 |
| 支撑异步处理     | 对非实时任务进行延迟处理或周期性批量处理     |

例如，在订单系统中，可以定时关闭超时未支付订单；在缓存系统中，可以定时刷新热点缓存；在日志系统中，可以定期清理历史日志数据。

### 适用业务场景

Spring 定时调度适合执行周期明确、业务逻辑相对固定、调度复杂度不高的后台任务。它的优势是接入简单，不需要额外部署调度中心，适合单体服务或轻量级后台任务。

常见业务场景如下：

| 业务场景 | 示例                                           |
| -------- | ---------------------------------------------- |
| 数据清理 | 定时删除过期验证码、临时文件、历史日志         |
| 订单处理 | 定时关闭超时订单、自动确认收货、自动结算       |
| 缓存维护 | 定时刷新字典缓存、首页缓存、配置缓存           |
| 数据同步 | 定时同步第三方接口数据、组织架构数据、基础资料 |
| 状态检测 | 定时检查设备状态、任务状态、接口连通性         |
| 报表生成 | 定时生成日报、周报、月报                       |
| 失败补偿 | 定时扫描失败记录并重新处理                     |
| 告警通知 | 定时检测异常数据并发送告警消息                 |

需要注意的是，Spring 原生定时调度默认适合单节点任务。如果项目采用多实例部署，同一个定时任务可能会在多个节点同时执行。对于分布式部署场景，需要结合 Redis 分布式锁、数据库锁、ShedLock、XXL-JOB 等方式避免重复执行。

### Spring Boot 3 中的支持方式

Spring Boot 3 基于 Spring Framework 的调度能力提供定时任务支持。项目引入 Spring Boot 基础依赖后，可以直接通过注解完成定时任务开发。

Spring Boot 3 中常用的调度方式如下：

| 支持方式                  | 说明                                       |
| ------------------------- | ------------------------------------------ |
| `@EnableScheduling`       | 启用 Spring 定时调度能力                   |
| `@Scheduled`              | 定义具体的定时任务方法                     |
| `cron`                    | 使用 cron 表达式配置复杂执行周期           |
| `fixedRate`               | 按固定频率执行任务                         |
| `fixedDelay`              | 上一次任务执行完成后，延迟指定时间再次执行 |
| `initialDelay`            | 应用启动后延迟指定时间再首次执行           |
| `TaskScheduler`           | 自定义任务调度器                           |
| `ThreadPoolTaskScheduler` | 配置定时任务线程池                         |
| `SchedulingConfigurer`    | 动态注册或自定义定时任务                   |

简单场景下，只需要使用 `@EnableScheduling` 和 `@Scheduled` 即可完成开发。复杂场景下，可以进一步扩展线程池、动态 cron、任务开关、分布式锁和任务监控。

## 环境准备

本章节用于说明开发 Spring 定时调度前需要准备的基础依赖、启用配置和项目结构。完成本章节配置后，项目即可支持基础的注解式定时任务开发。

### 基础依赖说明

Spring 定时调度能力由 Spring Framework 提供，通常不需要单独引入调度框架。只要项目中已经包含 `spring-boot-starter` 或 `spring-boot-starter-web`，就可以使用 `@EnableScheduling` 和 `@Scheduled`。

推荐基础环境如下：

| 环境        | 建议版本                               |
| ----------- | -------------------------------------- |
| JDK         | 17 或以上                              |
| Spring Boot | 3.x                                    |
| Maven       | 3.8 或以上                             |
| 编码        | UTF-8                                  |
| 时区        | 根据业务统一配置，例如 `Asia/Shanghai` |

文件位置：`pom.xml`

下面的依赖用于提供 Spring Boot 基础能力、Web 能力、Lombok 日志支持、Hutool 工具类支持和测试能力。

```xml
<dependencies>
    <!-- Spring Boot 基础依赖，包含 Spring Context，支持 @Scheduled 定时调度能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>

    <!-- Web 项目常用依赖，如果项目不提供接口服务，可以不引入 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Lombok：简化日志对象、构造方法、Getter、Setter 等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool：提供日期、字符串、集合、JSON 等常用工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Spring Boot 测试依赖，用于后续定时任务测试与验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果当前项目已经是 Spring Boot Web 项目，通常只需要确认项目中存在 `spring-boot-starter-web`，不需要重复添加 `spring-boot-starter`。

### 启用定时任务配置

Spring Boot 项目默认不会自动启用定时任务扫描，需要显式添加 `@EnableScheduling` 注解。该注解可以放在启动类上，也可以放在独立配置类上。

推荐使用独立配置类启用定时调度，这样后续扩展线程池、动态任务或分布式控制时结构更清晰。

文件位置：`src/main/java/io/github/atengk/schedule/config/SchedulingConfig.java`

该配置类用于启用 Spring 定时调度能力，项目启动后会自动扫描容器中带有 `@Scheduled` 注解的方法。

```java
package io.github.atengk.schedule.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring 定时调度配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * 初始化调度配置
     */
    @PostConstruct
    public void init() {
        log.info("Spring 定时调度已启用");
    }

}
```

如果项目结构较简单，也可以直接在启动类上添加 `@EnableScheduling`。

文件位置：`src/main/java/io/github/atengk/ScheduleApplication.java`

该启动类直接启用定时调度，适合小型项目或示例项目使用。

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时调度示例启动类
 *
 * @author Ateng
 * @since 2026-05-08
 */
@EnableScheduling
@SpringBootApplication
public class ScheduleApplication {

    /**
     * 项目启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ScheduleApplication.class, args);
    }

}
```

两种方式二选一即可。实际项目中更建议使用独立配置类，避免启动类承担过多配置职责。

文件位置：`src/main/resources/application.yml`

该配置文件用于设置应用名称、日志级别和定时任务基础开关，便于后续扩展配置化定时任务。

```yaml
spring:
  application:
    # 应用名称，便于日志、监控和链路追踪中识别服务
    name: spring-schedule-demo

logging:
  level:
    # 当前项目包日志级别，开发阶段建议使用 info
    io.github.atengk: info

schedule:
  # 示例任务开关，后续可以在任务中读取该配置控制是否执行
  enabled: true
```

### 项目结构规划

定时任务代码虽然通常不复杂，但仍建议按照职责进行分层。任务类只作为调度入口，不建议直接编写大量业务逻辑。具体业务处理应放到 Service 层，便于复用、测试和维护。

推荐项目结构如下：

```text
spring-schedule-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               ├── ScheduleApplication.java
        │               └── schedule
        │                   ├── config
        │                   │   └── SchedulingConfig.java
        │                   ├── task
        │                   │   └── OrderTimeoutTask.java
        │                   └── service
        │                       ├── OrderTimeoutService.java
        │                       └── impl
        │                           └── OrderTimeoutServiceImpl.java
        └── resources
            └── application.yml
```

各目录职责说明如下：

| 路径                        | 说明                                        |
| --------------------------- | ------------------------------------------- |
| `config`                    | 存放定时调度配置，例如启用调度、线程池配置  |
| `task`                      | 存放定时任务入口类，只负责触发任务          |
| `service`                   | 存放业务接口，定义任务需要执行的业务能力    |
| `service/impl`              | 存放业务实现类，处理具体业务逻辑            |
| `resources/application.yml` | 存放任务开关、cron 表达式、线程池参数等配置 |

建议遵循以下开发约定：

1. `task` 类只负责定时触发、日志记录和调用业务服务。
2. 具体业务逻辑放在 `service` 层，不直接堆在定时任务方法中。
3. cron 表达式、任务开关、线程池参数优先放在配置文件中。
4. 定时任务需要记录开始、结束和异常日志，方便线上排查。
5. 涉及数据修改的任务必须考虑幂等性，避免重复执行导致数据异常。

文件位置：`src/main/java/io/github/atengk/schedule/task/OrderTimeoutTask.java`

该任务类用于作为定时任务入口，后续可以在该类的方法上添加 `@Scheduled` 注解配置执行周期。

```java
package io.github.atengk.schedule.task;

import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单超时处理定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderTimeoutService orderTimeoutService;

    /**
     * 处理超时订单
     */
    public void handleTimeoutOrder() {
        log.info("开始执行超时订单处理任务");

        try {
            orderTimeoutService.closeTimeoutOrders();
            log.info("超时订单处理任务执行完成");
        } catch (Exception e) {
            log.error("超时订单处理任务执行异常", e);
        }
    }

}
```

文件位置：`src/main/java/io/github/atengk/schedule/service/OrderTimeoutService.java`

该接口用于定义超时订单处理能力，任务类通过接口调用业务逻辑，降低调度代码与业务代码之间的耦合。

```java
package io.github.atengk.schedule.service;

/**
 * 超时订单处理服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface OrderTimeoutService {

    /**
     * 关闭超时未支付订单
     */
    void closeTimeoutOrders();

}
```

文件位置：`src/main/java/io/github/atengk/schedule/service/impl/OrderTimeoutServiceImpl.java`

该实现类用于编写具体业务逻辑。示例中使用 Hutool 获取当前时间，实际项目中可以替换为数据库查询、订单状态更新和消息通知等逻辑。

```java
package io.github.atengk.schedule.service.impl;

import cn.hutool.core.date.DateUtil;
import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 超时订单处理服务实现
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    /**
     * 关闭超时未支付订单
     */
    @Override
    public void closeTimeoutOrders() {
        String currentTime = DateUtil.now();
        log.info("当前处理时间：{}，开始扫描超时未支付订单", currentTime);

        // TODO 实际项目中可在此处查询超时未支付订单，并批量更新订单状态
        log.info("超时未支付订单扫描完成");
    }

}
```

完成以上环境准备后，项目已经具备 Spring 定时调度开发基础。后续只需要在任务方法上添加 `@Scheduled` 注解，即可按照固定周期或 cron 表达式自动执行任务。



## 核心注解使用

本章节主要说明 Spring 定时调度开发中最核心的几个注解和参数，包括如何启用调度、如何定义任务、如何配置 cron 表达式，以及如何使用固定频率和固定延迟执行任务。

### @EnableScheduling 启用调度

`@EnableScheduling` 用于开启 Spring 的定时调度能力。只有添加该注解后，Spring 才会扫描容器中带有 `@Scheduled` 注解的方法，并按照配置的时间规则自动执行。

推荐将 `@EnableScheduling` 放在独立配置类中，便于后续统一扩展调度线程池、动态任务和任务开关。

文件位置：`src/main/java/io/github/atengk/schedule/config/SchedulingConfig.java`

该配置类用于启用 Spring 定时调度能力，并在项目启动时输出初始化日志。

```java
package io.github.atengk.schedule.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring 定时调度配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * 初始化调度配置
     */
    @PostConstruct
    public void init() {
        log.info("Spring 定时调度功能已启用");
    }

}
```

也可以直接在启动类上添加 `@EnableScheduling`。

文件位置：`src/main/java/io/github/atengk/ScheduleApplication.java`

该启动类用于启动 Spring Boot 项目，并直接启用定时调度能力。

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时调度示例应用
 *
 * @author Ateng
 * @since 2026-05-08
 */
@EnableScheduling
@SpringBootApplication
public class ScheduleApplication {

    /**
     * 项目启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ScheduleApplication.class, args);
    }

}
```

实际项目中，两种方式选择一种即可。对于中大型项目，更建议使用独立配置类，避免启动类中堆积过多配置注解。

### @Scheduled 定义定时任务

`@Scheduled` 用于标记一个方法为定时任务。被标记的方法必须交给 Spring 容器管理，也就是说该方法所在类需要使用 `@Component`、`@Service` 等注解注册为 Bean。

`@Scheduled` 常用配置方式如下：

| 配置方式       | 示例                                                       | 说明                                   |
| -------------- | ---------------------------------------------------------- | -------------------------------------- |
| `cron`         | `@Scheduled(cron = "0 0/5 * * * ?")`                       | 使用 cron 表达式配置复杂执行周期       |
| `fixedRate`    | `@Scheduled(fixedRate = 5000)`                             | 按固定频率执行，以上一次开始时间为基准 |
| `fixedDelay`   | `@Scheduled(fixedDelay = 5000)`                            | 按固定延迟执行，以上一次结束时间为基准 |
| `initialDelay` | `@Scheduled(initialDelay = 10000, fixedRate = 5000)`       | 项目启动后延迟执行首次任务             |
| `zone`         | `@Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Shanghai")` | 指定 cron 表达式使用的时区             |

定时任务方法通常建议满足以下约定：

1. 方法返回值使用 `void`。
2. 方法不定义入参。
3. 方法权限可以是 `public`。
4. 方法中需要处理异常，避免异常影响后续调度。
5. 方法中不要直接写大量业务逻辑，建议调用 Service 层。

文件位置：`src/main/java/io/github/atengk/schedule/task/SystemStatusTask.java`

该任务类演示使用 `@Scheduled` 定义一个简单的固定频率任务。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 系统状态定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class SystemStatusTask {

    /**
     * 每 10 秒输出一次系统状态日志
     */
    @Scheduled(fixedRate = 10_000)
    public void printSystemStatus() {
        log.info("系统状态检查任务执行，当前时间：{}", DateUtil.now());
    }

}
```

启动项目后，可以在控制台看到该方法按照固定频率自动执行。

### cron 表达式配置

`cron` 表达式适合描述比较复杂的时间规则，例如每天凌晨执行、每周一执行、每月 1 号执行等。

Spring `@Scheduled` 中的 cron 表达式通常使用 6 位格式：

```text
秒 分 时 日 月 周
```

字段说明如下：

| 字段 | 允许值              | 说明                                |
| ---- | ------------------- | ----------------------------------- |
| 秒   | `0-59`              | 表示第几秒执行                      |
| 分   | `0-59`              | 表示第几分钟执行                    |
| 时   | `0-23`              | 表示第几小时执行                    |
| 日   | `1-31`              | 表示每月第几天执行                  |
| 月   | `1-12` 或 `JAN-DEC` | 表示月份                            |
| 周   | `0-7` 或 `SUN-SAT`  | 表示星期，`0` 和 `7` 都可以表示周日 |

常用特殊字符如下：

| 字符 | 说明     | 示例                                   |
| ---- | -------- | -------------------------------------- |
| `*`  | 任意值   | `*` 表示每一秒、每一分钟等             |
| `?`  | 不指定值 | 日和周字段中常用                       |
| `/`  | 步长     | `0/5` 表示从 0 开始每 5 个单位执行一次 |
| `,`  | 枚举     | `1,3,5` 表示指定多个值                 |
| `-`  | 范围     | `1-5` 表示从 1 到 5                    |
| `L`  | 最后     | 常用于月末或周末场景                   |

常用 cron 示例：

| cron 表达式         | 说明                      |
| ------------------- | ------------------------- |
| `0/5 * * * * ?`     | 每 5 秒执行一次           |
| `0 0/1 * * * ?`     | 每 1 分钟执行一次         |
| `0 0 2 * * ?`       | 每天凌晨 2 点执行         |
| `0 30 1 * * ?`      | 每天凌晨 1 点 30 分执行   |
| `0 0 9 ? * MON-FRI` | 每周一到周五上午 9 点执行 |
| `0 0 0 1 * ?`       | 每月 1 号 0 点执行        |
| `0 0 23 L * ?`      | 每月最后一天 23 点执行    |

文件位置：`src/main/java/io/github/atengk/schedule/task/ReportGenerateTask.java`

该任务类演示使用 cron 表达式配置每天凌晨 2 点执行报表生成任务。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 报表生成定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class ReportGenerateTask {

    /**
     * 每天凌晨 2 点生成业务报表
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Shanghai")
    public void generateDailyReport() {
        log.info("开始执行日报生成任务，当前时间：{}", DateUtil.now());

        try {
            // TODO 查询业务数据并生成日报
            log.info("日报生成任务执行完成");
        } catch (Exception e) {
            log.error("日报生成任务执行异常", e);
        }
    }

}
```

cron 表达式建议优先放到配置文件中，避免硬编码在代码中。后续需要调整执行周期时，只需要修改配置，不需要重新修改 Java 代码。

### fixedRate 与 fixedDelay 配置

`fixedRate` 和 `fixedDelay` 都用于配置固定间隔任务，但两者的时间计算基准不同。

| 配置方式       | 时间基准                   | 适用场景                                         |
| -------------- | -------------------------- | ------------------------------------------------ |
| `fixedRate`    | 以上一次任务开始时间为基准 | 适合固定频率执行，例如每 10 秒采集一次状态       |
| `fixedDelay`   | 以上一次任务结束时间为基准 | 适合任务执行完成后再等待，例如同步任务、补偿任务 |
| `initialDelay` | 应用启动后延迟首次执行     | 适合避免项目刚启动就立即执行任务                 |

`fixedRate` 示例：

```java
@Scheduled(fixedRate = 10_000)
public void executeFixedRateTask() {
    log.info("fixedRate 任务执行");
}
```

该任务会尽量按照每 10 秒一次的频率执行，时间基准是上一次任务开始的时间。

`fixedDelay` 示例：

```java
@Scheduled(fixedDelay = 10_000)
public void executeFixedDelayTask() {
    log.info("fixedDelay 任务执行");
}
```

该任务会在上一次任务执行完成后，再等待 10 秒执行下一次。

`initialDelay` 示例：

```java
@Scheduled(initialDelay = 30_000, fixedRate = 10_000)
public void executeInitialDelayTask() {
    log.info("initialDelay 任务执行");
}
```

该任务会在应用启动 30 秒后首次执行，之后每 10 秒执行一次。

Spring Boot 3 中也可以使用 `timeUnit` 提升可读性。

```java
@Scheduled(fixedRate = 10, timeUnit = java.util.concurrent.TimeUnit.SECONDS)
public void executeReadableFixedRateTask() {
    log.info("使用 timeUnit 配置 fixedRate 任务执行");
}
```

相比直接写毫秒值，`timeUnit` 更适合较长时间间隔的任务，例如分钟、小时级别的定时任务。

## 定时任务开发

本章节说明一个定时任务从创建类、编写执行方法、配置执行周期到输出日志的完整开发方式。建议定时任务类只作为调度入口，具体业务逻辑放到 Service 层处理。

### 创建定时任务类

定时任务类必须交给 Spring 容器管理，通常使用 `@Component` 注解。类名建议以 `Task`、`Job` 或 `Scheduler` 结尾，便于识别其用途。

文件位置：`src/main/java/io/github/atengk/schedule/task/OrderTimeoutTask.java`

该任务类用于定时处理超时未支付订单，任务入口负责记录日志和调用业务服务。

```java
package io.github.atengk.schedule.task;

import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单超时处理定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderTimeoutService orderTimeoutService;

    /**
     * 处理超时未支付订单
     */
    public void handleTimeoutOrder() {
        log.info("开始执行超时订单处理任务");

        try {
            orderTimeoutService.closeTimeoutOrders();
            log.info("超时订单处理任务执行完成");
        } catch (Exception e) {
            log.error("超时订单处理任务执行异常", e);
        }
    }

}
```

对应的 Service 接口如下。

文件位置：`src/main/java/io/github/atengk/schedule/service/OrderTimeoutService.java`

该接口用于定义关闭超时订单的业务能力，便于任务类调用。

```java
package io.github.atengk.schedule.service;

/**
 * 超时订单处理服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface OrderTimeoutService {

    /**
     * 关闭超时未支付订单
     */
    void closeTimeoutOrders();

}
```

Service 实现类如下。

文件位置：`src/main/java/io/github/atengk/schedule/service/impl/OrderTimeoutServiceImpl.java`

该实现类用于处理超时订单业务逻辑，示例中使用 Hutool 获取当前时间并输出业务日志。

```java
package io.github.atengk.schedule.service.impl;

import cn.hutool.core.date.DateUtil;
import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 超时订单处理服务实现
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    /**
     * 关闭超时未支付订单
     */
    @Override
    public void closeTimeoutOrders() {
        String currentTime = DateUtil.now();
        log.info("当前处理时间：{}，开始扫描超时未支付订单", currentTime);

        // TODO 实际项目中可查询超时未支付订单，并批量更新订单状态
        log.info("超时未支付订单扫描完成");
    }

}
```

### 编写定时执行方法

定时执行方法就是被 `@Scheduled` 标记的方法。该方法通常不接收入参，也不返回结果。方法内部应尽量保持清晰，只做调度入口处理，不建议直接堆积复杂业务逻辑。

文件位置：`src/main/java/io/github/atengk/schedule/task/OrderTimeoutTask.java`

该任务方法每 1 分钟执行一次，用于扫描并关闭超时未支付订单。

```java
package io.github.atengk.schedule.task;

import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时处理定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderTimeoutService orderTimeoutService;

    /**
     * 每 1 分钟处理一次超时未支付订单
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void handleTimeoutOrder() {
        log.info("开始执行超时订单处理任务");

        try {
            orderTimeoutService.closeTimeoutOrders();
            log.info("超时订单处理任务执行完成");
        } catch (Exception e) {
            log.error("超时订单处理任务执行异常", e);
        }
    }

}
```

如果任务较多，建议每个业务域单独创建任务类，例如：

```text
schedule
├── task
│   ├── OrderTimeoutTask.java
│   ├── ReportGenerateTask.java
│   ├── CacheRefreshTask.java
│   └── DataSyncTask.java
```

这样可以避免多个无关任务堆在同一个类中，后续维护和排查更清晰。

### 配置任务执行周期

任务执行周期可以直接写在 `@Scheduled` 注解中，也可以从配置文件读取。实际项目中更推荐配置化，便于不同环境使用不同执行频率。

#### 方式一：直接在注解中配置

这种方式适合简单任务或固定不变的任务周期。

```java
@Scheduled(cron = "0 0/5 * * * ?")
public void refreshCache() {
    log.info("开始刷新缓存");
}
```

表示每 5 分钟执行一次。

#### 方式二：从配置文件读取 cron

文件位置：`src/main/resources/application.yml`

该配置用于统一管理定时任务开关和 cron 表达式。

```yaml
schedule:
  order-timeout:
    # 是否启用超时订单任务
    enabled: true
    # 每 1 分钟执行一次
    cron: "0 0/1 * * * ?"
```

文件位置：`src/main/java/io/github/atengk/schedule/task/OrderTimeoutTask.java`

该任务类通过占位符读取配置文件中的 cron 表达式，便于不同环境灵活调整执行周期。

```java
package io.github.atengk.schedule.task;

import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时处理定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderTimeoutService orderTimeoutService;

    @Value("${schedule.order-timeout.enabled:true}")
    private Boolean enabled;

    /**
     * 根据配置文件中的 cron 表达式处理超时订单
     */
    @Scheduled(cron = "${schedule.order-timeout.cron}")
    public void handleTimeoutOrder() {
        if (Boolean.FALSE.equals(enabled)) {
            log.info("超时订单处理任务未启用，跳过执行");
            return;
        }

        log.info("开始执行超时订单处理任务");

        try {
            orderTimeoutService.closeTimeoutOrders();
            log.info("超时订单处理任务执行完成");
        } catch (Exception e) {
            log.error("超时订单处理任务执行异常", e);
        }
    }

}
```

这种配置方式的优点是简单直接。需要注意的是，普通配置文件修改后通常不会自动刷新到已经运行的应用中，修改 cron 后一般需要重启服务才能生效。如果需要运行时动态修改周期，应使用动态任务注册方式。

#### 方式三：配置 fixedRate 或 fixedDelay

文件位置：`src/main/resources/application.yml`

该配置用于设置固定频率任务的执行间隔。

```yaml
schedule:
  cache-refresh:
    # 每 30 秒刷新一次缓存
    fixed-rate: 30000
```

文件位置：`src/main/java/io/github/atengk/schedule/task/CacheRefreshTask.java`

该任务类通过配置文件读取固定频率参数，并按照指定间隔刷新缓存。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 缓存刷新定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class CacheRefreshTask {

    /**
     * 按固定频率刷新缓存
     */
    @Scheduled(fixedRateString = "${schedule.cache-refresh.fixed-rate}")
    public void refreshCache() {
        log.info("开始刷新缓存，当前时间：{}", DateUtil.now());

        try {
            // TODO 实际项目中可在此处刷新字典、配置、首页等缓存
            log.info("缓存刷新完成");
        } catch (Exception e) {
            log.error("缓存刷新异常", e);
        }
    }

}
```

`fixedRateString` 和 `fixedDelayString` 支持从配置文件读取字符串形式的时间间隔，适合配置化管理。

### 任务日志输出

定时任务必须输出关键日志。因为定时任务通常没有用户请求链路，如果缺少日志，线上排查会非常困难。

建议至少记录以下日志：

| 日志位置 | 说明                                       |
| -------- | ------------------------------------------ |
| 任务开始 | 记录任务名称、开始时间                     |
| 任务结束 | 记录任务名称、结束时间、耗时               |
| 任务跳过 | 记录任务因开关、参数、条件不满足而跳过     |
| 任务异常 | 记录异常信息和堆栈                         |
| 关键结果 | 记录处理数量、成功数量、失败数量等核心指标 |

文件位置：`src/main/java/io/github/atengk/schedule/task/DataSyncTask.java`

该任务类演示一个包含完整日志输出的定时任务，包括开始日志、耗时日志、跳过日志和异常日志。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据同步定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class DataSyncTask {

    @Value("${schedule.data-sync.enabled:true}")
    private Boolean enabled;

    /**
     * 每 5 分钟同步一次外部数据
     */
    @Scheduled(cron = "${schedule.data-sync.cron:0 0/5 * * * ?}")
    public void syncData() {
        if (Boolean.FALSE.equals(enabled)) {
            log.info("数据同步任务未启用，跳过执行");
            return;
        }

        TimeInterval timer = DateUtil.timer();
        log.info("开始执行数据同步任务");

        try {
            // TODO 实际项目中可在此处调用第三方接口并保存同步结果
            int successCount = 0;
            int failCount = 0;

            log.info("数据同步任务执行完成，成功数量：{}，失败数量：{}，耗时：{}ms",
                    successCount, failCount, timer.interval());
        } catch (Exception e) {
            log.error("数据同步任务执行异常，耗时：{}ms", timer.interval(), e);
        }
    }

}
```

对应配置如下。

文件位置：`src/main/resources/application.yml`

该配置用于控制数据同步任务是否启用，以及配置任务执行周期。

```yaml
schedule:
  data-sync:
    # 是否启用数据同步任务
    enabled: true
    # 每 5 分钟执行一次
    cron: "0 0/5 * * * ?"
```

日志输出建议保持专业、简洁、可检索。任务名称建议固定，例如“超时订单处理任务”“数据同步任务”“缓存刷新任务”，便于在日志平台中按关键词查询。



## cron 表达式说明

本章节用于说明 Spring 定时调度中 cron 表达式的基本格式、常用写法和配置注意事项。cron 表达式适合描述固定时间点、固定日期、工作日、月末等较复杂的任务执行规则。

### cron 表达式格式

Spring `@Scheduled` 中常用的是 6 位 cron 表达式，格式如下：

```text
秒 分 时 日 月 周
```

每一位表示一个时间字段，Spring 会根据这些字段计算下一次任务执行时间。

| 字段 | 取值范围            | 示例      | 说明                                      |
| ---- | ------------------- | --------- | ----------------------------------------- |
| 秒   | `0-59`              | `0`       | 表示在第几秒执行                          |
| 分   | `0-59`              | `0/5`     | 表示在第几分钟执行                        |
| 时   | `0-23`              | `2`       | 表示在第几小时执行                        |
| 日   | `1-31`              | `1`       | 表示每月第几天执行                        |
| 月   | `1-12` 或 `JAN-DEC` | `*`       | 表示在哪个月执行                          |
| 周   | `0-7` 或 `SUN-SAT`  | `MON-FRI` | 表示星期几执行，`0` 和 `7` 通常都表示周日 |

常用特殊字符如下：

| 字符 | 含义         | 示例          | 说明                                     |
| ---- | ------------ | ------------- | ---------------------------------------- |
| `*`  | 任意值       | `* * * * * ?` | 表示匹配当前字段的所有值                 |
| `?`  | 不指定值     | `0 0 2 * * ?` | 通常用于“日”和“周”字段，表示不关心该字段 |
| `/`  | 步长         | `0/10`        | 表示从指定值开始，每隔固定步长执行       |
| `,`  | 枚举         | `1,3,5`       | 表示匹配多个指定值                       |
| `-`  | 范围         | `MON-FRI`     | 表示匹配一个连续范围                     |
| `L`  | 最后         | `L`           | 常用于表示月末或周末                     |
| `W`  | 工作日       | `1W`          | 表示距离指定日期最近的工作日             |
| `#`  | 第几个星期几 | `MON#1`       | 表示每月第一个周一                       |

例如：

```text
0 0 2 * * ?
```

表示每天凌晨 2 点执行一次。

```text
0 0/5 * * * ?
```

表示每 5 分钟执行一次。

cron 表达式中，“日”和“周”两个字段通常不要同时指定具体值。如果指定了“日”，一般将“周”写为 `?`；如果指定了“周”，一般将“日”写为 `?`。

### 常用 cron 示例

本节给出后端开发中常见的 cron 表达式示例。实际项目中建议将 cron 表达式放到配置文件中统一管理，避免直接硬编码在任务方法上。

| cron 表达式         | 执行说明                     |
| ------------------- | ---------------------------- |
| `0/5 * * * * ?`     | 每 5 秒执行一次              |
| `0 0/1 * * * ?`     | 每 1 分钟执行一次            |
| `0 0/5 * * * ?`     | 每 5 分钟执行一次            |
| `0 0 0/1 * * ?`     | 每 1 小时执行一次            |
| `0 0 2 * * ?`       | 每天凌晨 2 点执行            |
| `0 30 1 * * ?`      | 每天凌晨 1 点 30 分执行      |
| `0 0 9 ? * MON-FRI` | 每周一到周五上午 9 点执行    |
| `0 0 0 1 * ?`       | 每月 1 号 0 点执行           |
| `0 0 23 L * ?`      | 每月最后一天 23 点执行       |
| `0 0 10 ? * MON#1`  | 每月第一个周一上午 10 点执行 |

文件位置：`src/main/java/io/github/atengk/schedule/task/CronExampleTask.java`

该任务类演示几种常见 cron 写法，适合用于开发阶段验证 cron 表达式是否符合预期。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * cron 表达式示例任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class CronExampleTask {

    /**
     * 每 5 分钟执行一次缓存检查
     */
    @Scheduled(cron = "0 0/5 * * * ?", zone = "Asia/Shanghai")
    public void checkCache() {
        log.info("执行缓存检查任务，当前时间：{}", DateUtil.now());
    }

    /**
     * 每天凌晨 2 点执行数据清理
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Shanghai")
    public void cleanData() {
        log.info("执行数据清理任务，当前时间：{}", DateUtil.now());
    }

    /**
     * 每周一到周五上午 9 点执行工作日报初始化
     */
    @Scheduled(cron = "0 0 9 ? * MON-FRI", zone = "Asia/Shanghai")
    public void initWorkdayReport() {
        log.info("执行工作日报初始化任务，当前时间：{}", DateUtil.now());
    }

}
```

如果只是验证 cron 表达式，建议先使用短周期表达式，例如 `0/10 * * * * ?`，确认任务可以正常触发后，再改成真实业务周期。

### cron 配置注意事项

cron 表达式虽然灵活，但也容易因为字段含义、时区、执行耗时等问题导致任务不符合预期。开发时应重点关注表达式合法性、执行频率、时区和分布式重复执行问题。

常见注意事项如下：

| 注意事项               | 说明                                                  |
| ---------------------- | ----------------------------------------------------- |
| 不要配置过高频率       | 例如每秒执行一次的任务可能造成数据库、缓存或接口压力  |
| 明确配置时区           | 对跨地区部署或服务器时区不一致的项目，建议指定 `zone` |
| 避免“日”和“周”同时指定 | 通常一个指定具体值，另一个使用 `?`                    |
| 避免任务执行时间过长   | 如果任务耗时超过执行间隔，可能产生任务堆积或延迟      |
| 配置尽量外置           | cron 表达式建议放到 `application.yml` 中统一管理      |
| 关注分布式部署         | 多实例部署时，同一个 cron 任务可能被多个节点同时执行  |
| 记录执行日志           | 必须记录开始、结束、耗时和异常，便于线上排查          |

推荐在 `@Scheduled` 中显式配置时区。

```java
@Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Shanghai")
public void executeTask() {
    log.info("定时任务执行");
}
```

如果任务部署在多个实例中，不能只依赖 cron 表达式控制执行次数，还需要额外处理分布式重复执行问题。例如使用 Redis 分布式锁、数据库唯一约束、任务执行记录表、ShedLock 或 XXL-JOB 等方式保证同一时间只有一个节点执行核心逻辑。

## 动态配置定时任务

本章节用于说明如何将定时任务的执行周期从代码中抽离到配置文件中，并通过配置属性读取 cron 表达式。对于需要运行时修改执行周期的任务，还需要理解不同配置方式的生效范围。

### 配置文件管理调度周期

最简单的配置化方式是将 cron 表达式、任务开关、执行参数放到 `application.yml` 中。这样可以避免在 Java 代码中硬编码执行周期，方便不同环境配置不同任务频率。

文件位置：`src/main/resources/application.yml`

该配置用于统一管理订单超时任务、缓存刷新任务和数据同步任务的调度周期。

```yaml
schedule:
  order-timeout:
    # 是否启用超时订单处理任务
    enabled: true
    # 每 1 分钟执行一次
    cron: "0 0/1 * * * ?"

  cache-refresh:
    # 是否启用缓存刷新任务
    enabled: true
    # 每 30 秒执行一次
    fixed-rate: 30000

  data-sync:
    # 是否启用数据同步任务
    enabled: true
    # 每 5 分钟执行一次
    cron: "0 0/5 * * * ?"
```

这种方式适合大部分项目。开发、测试、生产环境可以使用不同配置文件，例如：

```text
application-dev.yml
application-test.yml
application-prod.yml
```

例如生产环境可以将任务执行频率配置得更低，测试环境可以使用更短周期便于验证。

### 使用配置属性读取 cron

读取 cron 表达式有两种常见方式：一种是直接在 `@Scheduled` 中使用占位符；另一种是使用 `@ConfigurationProperties` 统一绑定配置。简单任务可以使用占位符，配置项较多时更推荐使用配置属性类。

#### 方式一：使用占位符读取 cron

文件位置：`src/main/java/io/github/atengk/schedule/task/OrderTimeoutTask.java`

该任务类通过 `${schedule.order-timeout.cron}` 读取配置文件中的 cron 表达式，并通过任务开关控制是否执行。

```java
package io.github.atengk.schedule.task;

import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时处理定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderTimeoutService orderTimeoutService;

    @Value("${schedule.order-timeout.enabled:true}")
    private Boolean enabled;

    /**
     * 按配置文件中的 cron 表达式处理超时订单
     */
    @Scheduled(cron = "${schedule.order-timeout.cron}", zone = "Asia/Shanghai")
    public void handleTimeoutOrder() {
        if (Boolean.FALSE.equals(enabled)) {
            log.info("超时订单处理任务未启用，跳过执行");
            return;
        }

        log.info("开始执行超时订单处理任务");

        try {
            orderTimeoutService.closeTimeoutOrders();
            log.info("超时订单处理任务执行完成");
        } catch (Exception e) {
            log.error("超时订单处理任务执行异常", e);
        }
    }

}
```

这种方式代码较少，适合配置项简单的任务。缺点是配置分散在任务类字段中，任务多时不利于统一维护。

#### 方式二：使用配置属性类读取 cron

文件位置：`src/main/java/io/github/atengk/schedule/properties/ScheduleProperties.java`

该配置属性类用于统一绑定 `schedule` 开头的定时任务配置，便于在多个任务中复用。

```java
package io.github.atengk.schedule.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 定时任务配置属性
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Component
@ConfigurationProperties(prefix = "schedule")
public class ScheduleProperties {

    /**
     * 超时订单处理任务配置
     */
    private TaskConfig orderTimeout = new TaskConfig();

    /**
     * 数据同步任务配置
     */
    private TaskConfig dataSync = new TaskConfig();

    /**
     * 通用任务配置
     *
     * @author Ateng
     * @since 2026-05-08
     */
    @Data
    public static class TaskConfig {

        /**
         * 是否启用任务
         */
        private Boolean enabled = true;

        /**
         * cron 表达式
         */
        private String cron;

    }

}
```

文件位置：`src/main/java/io/github/atengk/schedule/task/DataSyncTask.java`

该任务类通过 `ScheduleProperties` 读取任务开关。cron 表达式仍由 `@Scheduled` 占位符读取，任务执行逻辑中通过配置属性控制是否跳过。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import io.github.atengk.schedule.properties.ScheduleProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据同步定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncTask {

    private final ScheduleProperties scheduleProperties;

    /**
     * 按配置文件中的 cron 表达式同步外部数据
     */
    @Scheduled(cron = "${schedule.data-sync.cron}", zone = "Asia/Shanghai")
    public void syncData() {
        ScheduleProperties.TaskConfig config = scheduleProperties.getDataSync();

        if (Boolean.FALSE.equals(config.getEnabled())) {
            log.info("数据同步任务未启用，跳过执行");
            return;
        }

        TimeInterval timer = DateUtil.timer();
        log.info("开始执行数据同步任务，cron：{}", config.getCron());

        try {
            // TODO 实际项目中可调用第三方接口并保存同步结果
            int successCount = 0;
            int failCount = 0;

            log.info("数据同步任务执行完成，成功数量：{}，失败数量：{}，耗时：{}ms",
                    successCount, failCount, timer.interval());
        } catch (Exception e) {
            log.error("数据同步任务执行异常，耗时：{}ms", timer.interval(), e);
        }
    }

}
```

如果配置类较多，也可以在启动类上启用配置属性扫描。

文件位置：`src/main/java/io/github/atengk/ScheduleApplication.java`

该启动类用于启动项目，并扫描 `@ConfigurationProperties` 配置属性类。

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时调度示例应用
 *
 * @author Ateng
 * @since 2026-05-08
 */
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class ScheduleApplication {

    /**
     * 项目启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ScheduleApplication.class, args);
    }

}
```

如果使用了 `@ConfigurationPropertiesScan`，配置属性类可以不加 `@Component`，只保留 `@ConfigurationProperties`。项目中选择一种注册方式即可，不要重复注册。

### 修改配置后的生效方式

定时任务配置修改后的生效方式，取决于任务是静态注册还是动态注册。普通 `@Scheduled` 注解任务属于静态注册，应用启动时会解析 cron 表达式，运行过程中直接修改 `application.yml` 通常不会自动生效。

常见生效方式如下：

| 配置方式                                 | 修改后是否自动生效 | 说明                                      |
| ---------------------------------------- | ------------------ | ----------------------------------------- |
| `@Scheduled(cron = "${...}")`            | 否                 | 修改配置文件后通常需要重启应用            |
| `@Scheduled(fixedRateString = "${...}")` | 否                 | 固定频率在任务注册时确定                  |
| 配置中心 + 刷新机制                      | 视配置中心能力而定 | 需要配合 Spring Cloud 或配置刷新机制      |
| `SchedulingConfigurer` 动态注册          | 可以支持           | cron 每次计算下一次执行时间时可读取最新值 |
| XXL-JOB 等调度平台                       | 可以支持           | 调度周期由外部平台统一管理                |

如果只是不同环境配置不同周期，推荐使用 `application-dev.yml`、`application-test.yml`、`application-prod.yml` 并在发布时重启服务。

如果需要运行时修改 cron 后立即生效，可以使用 `SchedulingConfigurer` 动态注册任务。

文件位置：`src/main/java/io/github/atengk/schedule/config/DynamicScheduleConfig.java`

该配置类通过 `SchedulingConfigurer` 注册动态定时任务，每次计算下一次执行时间时都会读取当前配置中的 cron 表达式。

```java
package io.github.atengk.schedule.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.schedule.properties.ScheduleProperties;
import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Date;

/**
 * 动态定时任务配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DynamicScheduleConfig implements SchedulingConfigurer {

    private final ScheduleProperties scheduleProperties;
    private final OrderTimeoutService orderTimeoutService;

    /**
     * 注册动态定时任务
     *
     * @param taskRegistrar 定时任务注册器
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                this::executeOrderTimeoutTask,
                triggerContext -> {
                    String cron = scheduleProperties.getOrderTimeout().getCron();
                    if (StrUtil.isBlank(cron)) {
                        log.warn("超时订单处理任务 cron 未配置，使用默认周期：0 0/1 * * * ?");
                        cron = "0 0/1 * * * ?";
                    }

                    CronTrigger cronTrigger = new CronTrigger(cron);
                    Date nextExecutionTime = cronTrigger.nextExecutionTime(triggerContext);
                    log.debug("超时订单处理任务下一次执行时间：{}", nextExecutionTime);
                    return nextExecutionTime;
                }
        );
    }

    /**
     * 执行超时订单处理任务
     */
    private void executeOrderTimeoutTask() {
        ScheduleProperties.TaskConfig config = scheduleProperties.getOrderTimeout();

        if (Boolean.FALSE.equals(config.getEnabled())) {
            log.info("超时订单处理任务未启用，跳过执行");
            return;
        }

        log.info("开始执行动态超时订单处理任务");

        try {
            orderTimeoutService.closeTimeoutOrders();
            log.info("动态超时订单处理任务执行完成");
        } catch (Exception e) {
            log.error("动态超时订单处理任务执行异常", e);
        }
    }

}
```

需要注意的是，上面的动态注册方式只是支持从内存中的配置对象读取最新 cron。如果 `application.yml` 文件被手动修改，Spring Boot 默认不会自动重新加载该文件。因此要实现“运行时修改后立即生效”，还需要提供配置刷新来源，例如数据库、Redis、配置中心或管理接口。

一种常见做法是将 cron 存到数据库或 Redis 中，动态任务每次计算下一次执行时间时读取最新值。示例逻辑如下：

```text
读取最新 cron -> 校验 cron 是否为空 -> 创建 CronTrigger -> 计算下一次执行时间 -> 执行业务任务
```

对于一般业务系统，推荐按以下规则选择方案：

| 业务需求                   | 推荐方案                                           |
| -------------------------- | -------------------------------------------------- |
| 周期固定，很少修改         | `@Scheduled` + 配置文件                            |
| 不同环境周期不同           | 多环境配置文件 + 重启生效                          |
| 需要临时关闭任务           | 配置任务开关 + 重启，或任务内部读取动态开关        |
| 需要运行时修改 cron        | `SchedulingConfigurer` + 数据库 / Redis / 配置中心 |
| 需要可视化管理和分布式调度 | XXL-JOB、PowerJob、ElasticJob 等调度平台           |

实际项目中，如果只是希望“把 cron 写到配置文件中”，使用 `@Scheduled(cron = "${...}")` 就足够。如果明确要求“不重启服务就修改执行周期”，则不建议继续使用普通 `@Scheduled` 静态注解方式，应改为动态注册任务或接入独立调度平台。



## 多任务与线程池配置

本章节用于说明 Spring 定时任务在多个任务同时存在时的执行行为，以及如何通过线程池提升任务并发执行能力。实际项目中，如果存在多个定时任务，或者单个任务执行时间较长，建议显式配置调度线程池，避免任务之间互相阻塞。

### 默认单线程调度行为

Spring 定时调度默认情况下通常使用单线程执行任务。也就是说，多个 `@Scheduled` 任务会共用同一个调度线程，如果某个任务执行时间较长，可能会影响其他任务的执行时机。

例如项目中同时存在以下两个任务：

```java
@Scheduled(cron = "0/10 * * * * ?")
public void syncData() {
    log.info("执行数据同步任务");
}

@Scheduled(cron = "0/10 * * * * ?")
public void refreshCache() {
    log.info("执行缓存刷新任务");
}
```

如果没有配置调度线程池，当两个任务在同一时间触发时，可能会出现一个任务先执行，另一个任务等待的情况。如果前一个任务执行耗时较长，后一个任务就会被延迟执行。

默认单线程行为的影响如下：

| 问题             | 说明                                 |
| ---------------- | ------------------------------------ |
| 任务互相阻塞     | 一个任务执行时间过长，会影响其他任务 |
| 执行时间不准确   | 任务可能无法严格按照 cron 时间点执行 |
| 异常排查困难     | 多个任务共用线程时，日志链路不够清晰 |
| 不适合多任务项目 | 任务数量较多时，单线程调度能力不足   |

因此，只要项目中存在多个定时任务，或者任务涉及数据库、Redis、第三方接口、文件处理等耗时操作，就建议配置独立的调度线程池。

### 自定义任务调度线程池

Spring Boot 3 项目中可以通过 `ThreadPoolTaskScheduler` 自定义定时任务线程池。配置线程池后，多个定时任务可以使用多个线程并发执行，降低任务之间互相阻塞的风险。

文件位置：`src/main/resources/application.yml`

该配置用于统一管理定时任务线程池参数。

```yaml
schedule:
  thread-pool:
    # 调度线程池核心线程数
    pool-size: 8
    # 线程名称前缀，便于日志中识别定时任务线程
    thread-name-prefix: schedule-task-
    # 应用关闭时是否等待任务执行完成
    wait-for-tasks-to-complete-on-shutdown: true
    # 应用关闭时等待任务完成的最大秒数
    await-termination-seconds: 30
```

文件位置：`src/main/java/io/github/atengk/schedule/properties/ScheduleThreadPoolProperties.java`

该配置属性类用于读取定时任务线程池参数，避免将线程池配置硬编码到 Java 代码中。

```java
package io.github.atengk.schedule.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 定时任务线程池配置属性
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Component
@ConfigurationProperties(prefix = "schedule.thread-pool")
public class ScheduleThreadPoolProperties {

    /**
     * 调度线程池核心线程数
     */
    private Integer poolSize = 8;

    /**
     * 线程名称前缀
     */
    private String threadNamePrefix = "schedule-task-";

    /**
     * 应用关闭时是否等待任务执行完成
     */
    private Boolean waitForTasksToCompleteOnShutdown = true;

    /**
     * 应用关闭时等待任务完成的最大秒数
     */
    private Integer awaitTerminationSeconds = 30;

}
```

文件位置：`src/main/java/io/github/atengk/schedule/config/SchedulingThreadPoolConfig.java`

该配置类用于创建 `ThreadPoolTaskScheduler`，并设置线程池大小、线程名前缀、关闭等待策略和异常处理器。

```java
package io.github.atengk.schedule.config;

import io.github.atengk.schedule.properties.ScheduleThreadPoolProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务线程池配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SchedulingThreadPoolConfig {

    private final ScheduleThreadPoolProperties threadPoolProperties;

    /**
     * 创建定时任务线程池
     *
     * @return 定时任务线程池
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(threadPoolProperties.getPoolSize());
        scheduler.setThreadNamePrefix(threadPoolProperties.getThreadNamePrefix());
        scheduler.setWaitForTasksToCompleteOnShutdown(threadPoolProperties.getWaitForTasksToCompleteOnShutdown());
        scheduler.setAwaitTerminationSeconds(threadPoolProperties.getAwaitTerminationSeconds());

        scheduler.setErrorHandler(throwable -> log.error("定时任务线程池捕获到未处理异常", throwable));

        scheduler.initialize();

        log.info("定时任务线程池初始化完成，线程数：{}，线程名前缀：{}",
                threadPoolProperties.getPoolSize(),
                threadPoolProperties.getThreadNamePrefix());

        return scheduler;
    }

}
```

配置完成后，所有 `@Scheduled` 定时任务会优先使用该线程池执行。日志中可以通过线程名前缀 `schedule-task-` 判断任务是否使用了自定义线程池。

### 多个任务并发执行

配置调度线程池后，多个任务可以在不同线程中并发执行。并发执行可以减少任务之间的等待时间，但也要求业务逻辑具备线程安全和幂等能力。

文件位置：`src/main/java/io/github/atengk/schedule/task/MultiTaskExample.java`

该任务类演示多个任务在同一时间触发时，通过线程池并发执行。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 多任务并发执行示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class MultiTaskExample {

    /**
     * 每 10 秒执行一次数据同步任务
     */
    @Scheduled(cron = "0/10 * * * * ?", zone = "Asia/Shanghai")
    public void syncData() {
        log.info("数据同步任务开始，当前线程：{}，时间：{}",
                Thread.currentThread().getName(), DateUtil.now());

        try {
            ThreadUtil.sleep(5_000);
            log.info("数据同步任务完成，当前线程：{}", Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("数据同步任务异常", e);
        }
    }

    /**
     * 每 10 秒执行一次缓存刷新任务
     */
    @Scheduled(cron = "0/10 * * * * ?", zone = "Asia/Shanghai")
    public void refreshCache() {
        log.info("缓存刷新任务开始，当前线程：{}，时间：{}",
                Thread.currentThread().getName(), DateUtil.now());

        try {
            ThreadUtil.sleep(3_000);
            log.info("缓存刷新任务完成，当前线程：{}", Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("缓存刷新任务异常", e);
        }
    }

    /**
     * 每 10 秒执行一次状态检查任务
     */
    @Scheduled(cron = "0/10 * * * * ?", zone = "Asia/Shanghai")
    public void checkStatus() {
        log.info("状态检查任务开始，当前线程：{}，时间：{}",
                Thread.currentThread().getName(), DateUtil.now());

        try {
            ThreadUtil.sleep(2_000);
            log.info("状态检查任务完成，当前线程：{}", Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("状态检查任务异常", e);
        }
    }

}
```

启动项目后，如果线程池配置生效，可以在日志中看到不同任务使用类似以下线程名执行：

```text
schedule-task-1
schedule-task-2
schedule-task-3
```

如果仍然只有一个线程在执行，需要检查是否存在以下问题：

| 检查项                                    | 说明                                   |
| ----------------------------------------- | -------------------------------------- |
| 是否创建了 `ThreadPoolTaskScheduler` Bean | 没有线程池 Bean 时可能仍使用默认调度器 |
| 线程池大小是否大于 1                      | `pool-size` 为 1 时仍然是单线程效果    |
| 任务是否在同一时间触发                    | 不同触发时间不一定能观察到并发效果     |
| 任务是否执行过快                          | 执行时间太短时，日志上可能不明显       |
| 是否存在重复配置                          | 多个调度器 Bean 可能导致使用不符合预期 |

需要注意的是，并发执行不代表同一个任务方法一定会并发执行多次。如果某个任务执行时间超过下一次触发间隔，需要结合任务类型、线程池、执行耗时和业务锁来判断是否会出现重叠执行风险。对于不允许重复执行的任务，应增加任务锁或状态控制。

### 线程池参数配置建议

线程池大小不是越大越好，应根据任务数量、任务耗时、系统资源和下游服务承载能力综合设置。定时任务线程池过小会导致任务阻塞，过大则可能造成数据库、Redis、第三方接口或 CPU 压力。

常见配置建议如下：

| 项目规模       | 建议线程数      | 适用情况                            |
| -------------- | --------------- | ----------------------------------- |
| 少量轻任务     | `2-4`           | 任务数量少，执行耗时短              |
| 中等任务量     | `4-8`           | 多个任务并行，存在少量 IO 操作      |
| 较多 IO 任务   | `8-16`          | 任务多，主要访问数据库、接口、Redis |
| CPU 密集任务   | 接近 CPU 核心数 | 大量计算、压缩、加密、报表生成      |
| 高并发复杂任务 | 独立调度平台    | 建议使用 XXL-JOB、PowerJob 等方案   |

线程池配置建议：

1. 线程名称必须设置，便于日志排查。
2. 生产环境不要使用过高线程数，避免瞬时压垮数据库或第三方接口。
3. 执行时间较长的任务应拆分批次，避免长期占用调度线程。
4. 不同类型任务可以拆分不同调度器，但普通项目通常一个调度线程池即可。
5. 对不允许重复执行的任务，需要使用分布式锁或业务状态控制。
6. 应用关闭时建议等待任务执行完成，避免任务中断导致数据不一致。
7. 任务线程池只解决并发执行问题，不解决分布式多节点重复执行问题。

推荐生产配置示例：

```yaml
schedule:
  thread-pool:
    # 中小型业务系统建议先配置 4 到 8 个线程，再根据监控调整
    pool-size: 8
    # 线程名前缀用于日志检索
    thread-name-prefix: schedule-task-
    # 停机时等待任务完成，降低数据处理中断风险
    wait-for-tasks-to-complete-on-shutdown: true
    # 等待时间不宜过长，避免应用停机卡住
    await-termination-seconds: 30
```

## 异常处理

本章节用于说明定时任务执行异常后的影响，以及如何通过 `try-catch`、日志记录和告警扩展提升任务稳定性。定时任务通常运行在后台，没有用户请求直接感知执行结果，因此异常处理和日志记录非常重要。

### 定时任务异常影响

定时任务方法中如果抛出未捕获异常，当前这一次任务执行会失败。Spring 调度器通常会记录异常，并在下一次触发时间继续执行任务，但本次任务中的后续业务逻辑不会继续执行。

例如：

```java
@Scheduled(cron = "0 0/1 * * * ?")
public void executeTask() {
    log.info("任务开始执行");

    int result = 1 / 0;

    log.info("任务执行完成");
}
```

上述代码会在执行到 `1 / 0` 时抛出异常，后面的“任务执行完成”日志不会输出。

定时任务异常的常见影响如下：

| 影响             | 说明                                                 |
| ---------------- | ---------------------------------------------------- |
| 当前执行失败     | 本次任务未完成，后续代码不会继续执行                 |
| 数据可能处理中断 | 批量处理过程中可能只处理了一部分数据                 |
| 日志可能不完整   | 如果没有捕获异常，缺少业务上下文信息                 |
| 可能重复处理     | 下次任务再次扫描数据时，可能重复处理上一轮未完成数据 |
| 告警不及时       | 只打印日志但没有告警，线上问题可能长期未发现         |

因此，定时任务中不建议让异常直接向外抛出。应在任务入口统一捕获异常，并记录任务名称、执行参数、耗时和异常堆栈。

### try-catch 捕获异常

推荐在每个定时任务入口方法中使用 `try-catch` 捕获异常。这样可以保证日志完整，也方便后续扩展告警通知。

文件位置：`src/main/java/io/github/atengk/schedule/task/OrderTimeoutTask.java`

该任务类在任务入口中捕获异常，并使用 Hutool 的 `TimeInterval` 记录任务耗时。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时处理定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderTimeoutService orderTimeoutService;

    /**
     * 每 1 分钟处理一次超时未支付订单
     */
    @Scheduled(cron = "${schedule.order-timeout.cron:0 0/1 * * * ?}", zone = "Asia/Shanghai")
    public void handleTimeoutOrder() {
        TimeInterval timer = DateUtil.timer();
        log.info("超时订单处理任务开始执行");

        try {
            int closeCount = orderTimeoutService.closeTimeoutOrders();
            log.info("超时订单处理任务执行完成，关闭订单数量：{}，耗时：{}ms",
                    closeCount, timer.interval());
        } catch (Exception e) {
            log.error("超时订单处理任务执行异常，耗时：{}ms", timer.interval(), e);
        }
    }

}
```

对应 Service 接口可以返回处理数量，方便任务日志记录处理结果。

文件位置：`src/main/java/io/github/atengk/schedule/service/OrderTimeoutService.java`

该接口用于定义关闭超时订单的业务能力，并返回本次关闭订单数量。

```java
package io.github.atengk.schedule.service;

/**
 * 超时订单处理服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface OrderTimeoutService {

    /**
     * 关闭超时未支付订单
     *
     * @return 关闭订单数量
     */
    int closeTimeoutOrders();

}
```

文件位置：`src/main/java/io/github/atengk/schedule/service/impl/OrderTimeoutServiceImpl.java`

该实现类用于模拟超时订单处理逻辑，实际项目中可以替换为数据库查询和批量更新逻辑。

```java
package io.github.atengk.schedule.service.impl;

import cn.hutool.core.date.DateUtil;
import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 超时订单处理服务实现
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    /**
     * 关闭超时未支付订单
     *
     * @return 关闭订单数量
     */
    @Override
    public int closeTimeoutOrders() {
        log.info("开始扫描超时未支付订单，当前时间：{}", DateUtil.now());

        // TODO 实际项目中可查询超时未支付订单，并批量更新订单状态
        int closeCount = 0;

        log.info("超时未支付订单扫描完成，关闭订单数量：{}", closeCount);
        return closeCount;
    }

}
```

如果定时任务中包含循环批量处理，建议对单条数据也做异常隔离，避免一条异常数据导致整个批次中断。

文件位置：`src/main/java/io/github/atengk/schedule/task/BatchCompensateTask.java`

该任务类演示批量补偿任务中对单条数据进行异常隔离。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 批量补偿定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class BatchCompensateTask {

    /**
     * 每 5 分钟执行一次失败数据补偿
     */
    @Scheduled(cron = "${schedule.compensate.cron:0 0/5 * * * ?}", zone = "Asia/Shanghai")
    public void compensateFailedData() {
        TimeInterval timer = DateUtil.timer();
        log.info("失败数据补偿任务开始执行");

        int successCount = 0;
        int failCount = 0;

        try {
            List<Long> failedIds = queryFailedIds();

            if (CollUtil.isEmpty(failedIds)) {
                log.info("失败数据补偿任务无待处理数据，耗时：{}ms", timer.interval());
                return;
            }

            for (Long failedId : failedIds) {
                try {
                    compensateOne(failedId);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("失败数据补偿单条处理异常，数据ID：{}", failedId, e);
                }
            }

            log.info("失败数据补偿任务执行完成，成功数量：{}，失败数量：{}，耗时：{}ms",
                    successCount, failCount, timer.interval());
        } catch (Exception e) {
            log.error("失败数据补偿任务执行异常，成功数量：{}，失败数量：{}，耗时：{}ms",
                    successCount, failCount, timer.interval(), e);
        }
    }

    /**
     * 查询失败数据 ID
     *
     * @return 失败数据 ID 列表
     */
    private List<Long> queryFailedIds() {
        // TODO 实际项目中可从数据库查询待补偿记录
        return List.of(1001L, 1002L, 1003L);
    }

    /**
     * 补偿单条失败数据
     *
     * @param failedId 失败数据 ID
     */
    private void compensateOne(Long failedId) {
        log.info("开始补偿失败数据，数据ID：{}", failedId);

        // TODO 实际项目中可重新调用接口、发送消息或更新状态
        log.info("失败数据补偿完成，数据ID：{}", failedId);
    }

}
```

这种写法适合补偿类、同步类、批处理类任务。即使某一条数据处理失败，也不会影响其他数据继续执行。

### 日志记录与告警扩展

定时任务需要记录足够完整的运行日志。对于生产环境中的核心任务，只记录异常日志还不够，建议进一步扩展告警能力，例如发送企业微信、钉钉、邮件、短信或写入告警表。

定时任务日志建议包含以下信息：

| 日志内容 | 说明                             |
| -------- | -------------------------------- |
| 任务名称 | 方便快速定位是哪个任务           |
| 开始时间 | 判断任务是否按时触发             |
| 结束时间 | 判断任务是否完整执行             |
| 执行耗时 | 判断任务是否变慢                 |
| 处理数量 | 记录成功数、失败数、跳过数       |
| 关键参数 | 记录批次号、时间范围、业务类型等 |
| 异常堆栈 | 排查具体错误原因                 |
| 告警结果 | 记录是否发送告警成功             |

可以抽象一个简单的告警服务，供多个定时任务复用。

文件位置：`src/main/java/io/github/atengk/schedule/service/TaskAlertService.java`

该接口用于定义定时任务告警能力，后续可以对接企业微信、钉钉、邮件或短信。

```java
package io.github.atengk.schedule.service;

/**
 * 定时任务告警服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface TaskAlertService {

    /**
     * 发送任务异常告警
     *
     * @param taskName 任务名称
     * @param message 告警内容
     * @param throwable 异常对象
     */
    void sendErrorAlert(String taskName, String message, Throwable throwable);

}
```

文件位置：`src/main/java/io/github/atengk/schedule/service/impl/TaskAlertServiceImpl.java`

该实现类用于记录任务告警日志，实际项目中可以替换为企业微信、钉钉、邮件等通知逻辑。

```java
package io.github.atengk.schedule.service.impl;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.schedule.service.TaskAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 定时任务告警服务实现
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class TaskAlertServiceImpl implements TaskAlertService {

    /**
     * 发送任务异常告警
     *
     * @param taskName 任务名称
     * @param message 告警内容
     * @param throwable 异常对象
     */
    @Override
    public void sendErrorAlert(String taskName, String message, Throwable throwable) {
        String errorMessage = ExceptionUtil.getMessage(throwable);
        String alertContent = StrUtil.format("任务名称：{}，告警内容：{}，异常信息：{}",
                taskName, message, errorMessage);

        // TODO 实际项目中可对接企业微信、钉钉、邮件、短信或告警平台
        log.warn("发送定时任务异常告警：{}", alertContent);
    }

}
```

文件位置：`src/main/java/io/github/atengk/schedule/task/DataSyncTask.java`

该任务类在捕获异常后调用告警服务，适合核心业务任务使用。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import io.github.atengk.schedule.service.TaskAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据同步定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncTask {

    private final TaskAlertService taskAlertService;

    /**
     * 每 5 分钟同步一次外部数据
     */
    @Scheduled(cron = "${schedule.data-sync.cron:0 0/5 * * * ?}", zone = "Asia/Shanghai")
    public void syncData() {
        String taskName = "数据同步任务";
        TimeInterval timer = DateUtil.timer();

        log.info("{}开始执行", taskName);

        try {
            // TODO 实际项目中可调用第三方接口并保存同步结果
            int successCount = 0;
            int failCount = 0;

            log.info("{}执行完成，成功数量：{}，失败数量：{}，耗时：{}ms",
                    taskName, successCount, failCount, timer.interval());
        } catch (Exception e) {
            log.error("{}执行异常，耗时：{}ms", taskName, timer.interval(), e);
            taskAlertService.sendErrorAlert(taskName, "任务执行异常，请及时处理", e);
        }
    }

}
```

对于核心任务，建议进一步将任务执行结果落库，形成可查询的任务执行记录。

推荐记录字段如下：

| 字段            | 说明                               |
| --------------- | ---------------------------------- |
| `task_name`     | 任务名称                           |
| `start_time`    | 开始时间                           |
| `end_time`      | 结束时间                           |
| `cost_ms`       | 执行耗时                           |
| `status`        | 执行状态，例如成功、失败、部分失败 |
| `success_count` | 成功数量                           |
| `fail_count`    | 失败数量                           |
| `error_message` | 异常摘要                           |
| `created_at`    | 记录创建时间                       |

异常处理建议总结如下：

1. 每个任务入口方法都要捕获异常。
2. 日志中必须包含任务名称和执行耗时。
3. 批量任务建议隔离单条数据异常。
4. 核心任务异常后应发送告警。
5. 任务异常不应只打印 `e.getMessage()`，必须输出完整异常堆栈。
6. 涉及数据修改的任务应设计幂等逻辑，避免失败重试导致重复处理。
7. 对高价值任务建议记录任务执行历史，便于审计和排查。



## 开发示例

本章节给出 Spring Boot 3 中常见定时任务开发示例，包括固定频率任务、cron 定时任务、配置化 cron 任务和多线程定时任务。示例以可直接集成到项目中的写法为主。

### 简单固定频率任务

固定频率任务适合执行周期简单、间隔固定的场景，例如定时输出系统状态、刷新本地缓存、检测服务健康状态等。

文件位置：`src/main/java/io/github/atengk/schedule/task/SimpleFixedRateTask.java`

该任务类每 10 秒执行一次，用于演示 `fixedRate` 的基本使用方式。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 简单固定频率定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class SimpleFixedRateTask {

    /**
     * 每 10 秒执行一次系统状态检查
     */
    @Scheduled(fixedRate = 10_000)
    public void checkSystemStatus() {
        TimeInterval timer = DateUtil.timer();
        log.info("系统状态检查任务开始执行，当前时间：{}", DateUtil.now());

        try {
            // TODO 实际项目中可检查 JVM、磁盘、缓存、接口状态等
            log.info("系统状态检查任务执行完成，耗时：{}ms", timer.interval());
        } catch (Exception e) {
            log.error("系统状态检查任务执行异常，耗时：{}ms", timer.interval(), e);
        }
    }

}
```

如果使用 Spring Boot 3，也可以通过 `timeUnit` 提升可读性。

文件位置：`src/main/java/io/github/atengk/schedule/task/ReadableFixedRateTask.java`

该任务类使用 `timeUnit` 指定时间单位，避免直接编写毫秒值。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 可读性更高的固定频率定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class ReadableFixedRateTask {

    /**
     * 每 30 秒刷新一次本地缓存
     */
    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    public void refreshLocalCache() {
        log.info("本地缓存刷新任务执行，当前时间：{}", DateUtil.now());

        // TODO 实际项目中可刷新字典缓存、配置缓存或热点数据缓存
    }

}
```

`fixedRate` 以上一次任务开始时间为基准。如果任务执行时间较长，下一次触发时间可能会接近或延迟。对于不希望任务连续执行过密的场景，更适合使用 `fixedDelay`。

### cron 定时任务

cron 定时任务适合描述固定时间点执行的业务任务，例如每天凌晨清理数据、每周生成报表、每月执行结算等。

文件位置：`src/main/java/io/github/atengk/schedule/task/CronReportTask.java`

该任务类每天凌晨 2 点执行一次日报生成任务。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * cron 报表生成定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class CronReportTask {

    /**
     * 每天凌晨 2 点生成业务日报
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Shanghai")
    public void generateDailyReport() {
        TimeInterval timer = DateUtil.timer();
        log.info("业务日报生成任务开始执行，当前时间：{}", DateUtil.now());

        try {
            // TODO 实际项目中可查询昨日业务数据并生成日报
            log.info("业务日报生成任务执行完成，耗时：{}ms", timer.interval());
        } catch (Exception e) {
            log.error("业务日报生成任务执行异常，耗时：{}ms", timer.interval(), e);
        }
    }

}
```

也可以定义工作日任务。

文件位置：`src/main/java/io/github/atengk/schedule/task/WorkdayTask.java`

该任务类每周一到周五上午 9 点执行，用于处理工作日业务初始化。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 工作日定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class WorkdayTask {

    /**
     * 每周一到周五上午 9 点执行工作日初始化
     */
    @Scheduled(cron = "0 0 9 ? * MON-FRI", zone = "Asia/Shanghai")
    public void initWorkdayData() {
        log.info("工作日初始化任务执行，当前时间：{}", DateUtil.now());

        // TODO 实际项目中可初始化日报、检查待办、同步工作日数据等
    }

}
```

cron 任务建议显式配置 `zone`。如果服务器系统时区、JVM 时区和业务时区不一致，任务实际执行时间可能与预期不一致。

### 配置化 cron 任务

配置化 cron 任务适合生产项目使用。将 cron 表达式、任务开关等参数放入 `application.yml`，可以减少硬编码，便于不同环境使用不同执行周期。

文件位置：`src/main/resources/application.yml`

该配置用于管理订单超时任务的开关和执行周期。

```yaml
schedule:
  order-timeout:
    # 是否启用超时订单处理任务
    enabled: true
    # 每 1 分钟执行一次
    cron: "0 0/1 * * * ?"
```

文件位置：`src/main/java/io/github/atengk/schedule/properties/OrderTimeoutScheduleProperties.java`

该配置属性类用于读取订单超时任务配置。

```java
package io.github.atengk.schedule.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 订单超时定时任务配置属性
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Component
@ConfigurationProperties(prefix = "schedule.order-timeout")
public class OrderTimeoutScheduleProperties {

    /**
     * 是否启用任务
     */
    private Boolean enabled = true;

    /**
     * cron 表达式
     */
    private String cron = "0 0/1 * * * ?";

}
```

文件位置：`src/main/java/io/github/atengk/schedule/service/OrderTimeoutService.java`

该接口用于定义超时订单处理能力。

```java
package io.github.atengk.schedule.service;

/**
 * 超时订单处理服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface OrderTimeoutService {

    /**
     * 关闭超时未支付订单
     *
     * @return 关闭订单数量
     */
    int closeTimeoutOrders();

}
```

文件位置：`src/main/java/io/github/atengk/schedule/service/impl/OrderTimeoutServiceImpl.java`

该实现类用于处理超时订单业务逻辑。示例中保留模拟逻辑，实际项目中可替换为数据库查询和批量更新。

```java
package io.github.atengk.schedule.service.impl;

import cn.hutool.core.date.DateUtil;
import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 超时订单处理服务实现
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    /**
     * 关闭超时未支付订单
     *
     * @return 关闭订单数量
     */
    @Override
    public int closeTimeoutOrders() {
        log.info("开始扫描超时未支付订单，当前时间：{}", DateUtil.now());

        // TODO 实际项目中可查询超时未支付订单，并批量更新订单状态
        int closeCount = 0;

        log.info("超时未支付订单扫描完成，关闭订单数量：{}", closeCount);
        return closeCount;
    }

}
```

文件位置：`src/main/java/io/github/atengk/schedule/task/OrderTimeoutConfigTask.java`

该任务类通过配置文件读取 cron 表达式，并通过配置属性控制任务是否执行。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import io.github.atengk.schedule.properties.OrderTimeoutScheduleProperties;
import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 配置化超时订单定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConfigTask {

    private final OrderTimeoutScheduleProperties properties;
    private final OrderTimeoutService orderTimeoutService;

    /**
     * 按配置文件中的 cron 表达式处理超时订单
     */
    @Scheduled(cron = "${schedule.order-timeout.cron}", zone = "Asia/Shanghai")
    public void handleTimeoutOrder() {
        if (Boolean.FALSE.equals(properties.getEnabled())) {
            log.info("超时订单处理任务未启用，跳过执行");
            return;
        }

        TimeInterval timer = DateUtil.timer();
        log.info("超时订单处理任务开始执行，cron：{}", properties.getCron());

        try {
            int closeCount = orderTimeoutService.closeTimeoutOrders();
            log.info("超时订单处理任务执行完成，关闭订单数量：{}，耗时：{}ms",
                    closeCount, timer.interval());
        } catch (Exception e) {
            log.error("超时订单处理任务执行异常，耗时：{}ms", timer.interval(), e);
        }
    }

}
```

这种方式适合大多数业务系统。需要注意，普通 `@Scheduled(cron = "${...}")` 在应用启动时完成任务注册，修改 `application.yml` 后通常需要重启应用才能生效。

### 多线程定时任务

当项目中存在多个定时任务，或者任务执行时间较长时，建议配置定时任务线程池，避免任务之间互相阻塞。

文件位置：`src/main/resources/application.yml`

该配置用于设置定时任务线程池参数。

```yaml
schedule:
  thread-pool:
    # 调度线程池线程数
    pool-size: 8
    # 线程名称前缀，便于日志排查
    thread-name-prefix: schedule-task-
    # 应用关闭时是否等待任务执行完成
    wait-for-tasks-to-complete-on-shutdown: true
    # 应用关闭时最多等待任务完成的秒数
    await-termination-seconds: 30
```

文件位置：`src/main/java/io/github/atengk/schedule/properties/ScheduleThreadPoolProperties.java`

该配置属性类用于读取定时任务线程池配置。

```java
package io.github.atengk.schedule.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 定时任务线程池配置属性
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Component
@ConfigurationProperties(prefix = "schedule.thread-pool")
public class ScheduleThreadPoolProperties {

    /**
     * 调度线程池线程数
     */
    private Integer poolSize = 8;

    /**
     * 线程名称前缀
     */
    private String threadNamePrefix = "schedule-task-";

    /**
     * 应用关闭时是否等待任务执行完成
     */
    private Boolean waitForTasksToCompleteOnShutdown = true;

    /**
     * 应用关闭时最多等待任务完成的秒数
     */
    private Integer awaitTerminationSeconds = 30;

}
```

文件位置：`src/main/java/io/github/atengk/schedule/config/SchedulingThreadPoolConfig.java`

该配置类用于创建定时任务线程池，并统一处理未捕获异常。

```java
package io.github.atengk.schedule.config;

import io.github.atengk.schedule.properties.ScheduleThreadPoolProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务线程池配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SchedulingThreadPoolConfig {

    private final ScheduleThreadPoolProperties properties;

    /**
     * 创建定时任务线程池
     *
     * @return 定时任务线程池
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(properties.getPoolSize());
        scheduler.setThreadNamePrefix(properties.getThreadNamePrefix());
        scheduler.setWaitForTasksToCompleteOnShutdown(properties.getWaitForTasksToCompleteOnShutdown());
        scheduler.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
        scheduler.setErrorHandler(throwable -> log.error("定时任务线程池捕获到未处理异常", throwable));

        scheduler.initialize();

        log.info("定时任务线程池初始化完成，线程数：{}，线程名前缀：{}",
                properties.getPoolSize(), properties.getThreadNamePrefix());

        return scheduler;
    }

}
```

文件位置：`src/main/java/io/github/atengk/schedule/task/MultiThreadScheduleTask.java`

该任务类用于验证多个定时任务是否可以并发执行。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 多线程定时任务示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class MultiThreadScheduleTask {

    /**
     * 每 10 秒执行一次数据同步任务
     */
    @Scheduled(cron = "0/10 * * * * ?", zone = "Asia/Shanghai")
    public void syncData() {
        log.info("数据同步任务开始，线程：{}，时间：{}",
                Thread.currentThread().getName(), DateUtil.now());

        try {
            ThreadUtil.sleep(5_000);
            log.info("数据同步任务完成，线程：{}", Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("数据同步任务异常", e);
        }
    }

    /**
     * 每 10 秒执行一次缓存刷新任务
     */
    @Scheduled(cron = "0/10 * * * * ?", zone = "Asia/Shanghai")
    public void refreshCache() {
        log.info("缓存刷新任务开始，线程：{}，时间：{}",
                Thread.currentThread().getName(), DateUtil.now());

        try {
            ThreadUtil.sleep(3_000);
            log.info("缓存刷新任务完成，线程：{}", Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("缓存刷新任务异常", e);
        }
    }

    /**
     * 每 10 秒执行一次状态检查任务
     */
    @Scheduled(cron = "0/10 * * * * ?", zone = "Asia/Shanghai")
    public void checkStatus() {
        log.info("状态检查任务开始，线程：{}，时间：{}",
                Thread.currentThread().getName(), DateUtil.now());

        try {
            ThreadUtil.sleep(2_000);
            log.info("状态检查任务完成，线程：{}", Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("状态检查任务异常", e);
        }
    }

}
```

启动项目后，如果日志中出现多个不同的线程名，例如 `schedule-task-1`、`schedule-task-2`、`schedule-task-3`，说明自定义线程池已经生效。

## 测试与验证

本章节用于说明定时任务开发完成后的验证方式。定时任务通常没有接口入口，不能只依赖代码编译通过，还需要从启动日志、执行频率和异常场景三个方面确认任务行为是否符合预期。

### 启动日志验证

启动日志验证用于确认定时调度功能是否已启用、任务 Bean 是否已被 Spring 容器扫描、线程池是否初始化成功。

启动项目前，需要确认启动类或配置类中已经启用调度。

文件位置：`src/main/java/io/github/atengk/schedule/config/SchedulingConfig.java`

该配置类用于开启定时调度，并在应用启动时输出确认日志。

```java
package io.github.atengk.schedule.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring 定时调度配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * 初始化调度配置
     */
    @PostConstruct
    public void init() {
        log.info("Spring 定时调度功能已启用");
    }

}
```

启动项目后，控制台应能看到类似日志：

```text
Spring 定时调度功能已启用
定时任务线程池初始化完成，线程数：8，线程名前缀：schedule-task-
```

如果没有看到定时任务执行日志，可以按以下顺序检查：

| 检查项                       | 说明                                        |
| ---------------------------- | ------------------------------------------- |
| 是否添加 `@EnableScheduling` | 未启用调度时，`@Scheduled` 不会执行         |
| 任务类是否加 `@Component`    | 未注册为 Spring Bean 时，任务不会被扫描     |
| 方法是否添加 `@Scheduled`    | 没有调度注解时，方法不会自动执行            |
| cron 表达式是否正确          | cron 错误会导致任务无法注册或无法触发       |
| 日志级别是否过高             | 如果日志级别高于 `info`，可能看不到任务日志 |
| 任务开关是否关闭             | 配置了 `enabled=false` 时，任务会跳过执行   |

也可以通过 Maven 启动项目进行验证。

```bash
mvn spring-boot:run
```

执行命令后，观察控制台是否出现定时任务初始化日志和周期性执行日志。

### 执行频率验证

执行频率验证用于确认任务是否按照预期周期执行。开发阶段建议先使用较短周期，例如每 5 秒或每 10 秒执行一次，确认任务可以正常触发后，再改为真实业务周期。

文件位置：`src/main/java/io/github/atengk/schedule/task/FrequencyVerifyTask.java`

该任务类每 5 秒执行一次，并打印当前时间和线程名称，便于观察执行频率。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 执行频率验证任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class FrequencyVerifyTask {

    /**
     * 每 5 秒执行一次，用于验证任务执行频率
     */
    @Scheduled(cron = "0/5 * * * * ?", zone = "Asia/Shanghai")
    public void verifyFrequency() {
        log.info("执行频率验证任务触发，线程：{}，当前时间：{}",
                Thread.currentThread().getName(), DateUtil.now());
    }

}
```

预期日志类似如下：

```text
执行频率验证任务触发，线程：schedule-task-1，当前时间：2026-05-08 10:00:00
执行频率验证任务触发，线程：schedule-task-2，当前时间：2026-05-08 10:00:05
执行频率验证任务触发，线程：schedule-task-1，当前时间：2026-05-08 10:00:10
```

如果实际间隔明显不符合预期，需要检查以下问题：

| 问题               | 可能原因                                 |
| ------------------ | ---------------------------------------- |
| 任务没有执行       | 未启用调度、任务未注册、cron 不正确      |
| 执行间隔变长       | 任务耗时过长、线程池太小、任务阻塞       |
| 执行时间点不对     | 时区配置不一致、cron 表达式写错          |
| 多任务互相影响     | 没有配置线程池或线程池数量过小           |
| 修改 cron 后未生效 | 普通 `@Scheduled` 修改配置后需要重启应用 |

开发验证完成后，应将短周期 cron 修改为真实业务周期，避免测试配置进入生产环境。

### 异常场景验证

异常场景验证用于确认定时任务发生异常时，系统是否能够记录完整日志、继续后续调度，并按需触发告警。

文件位置：`src/main/resources/application.yml`

该配置用于控制异常验证任务是否启用，避免测试任务误进入生产执行。

```yaml
schedule:
  exception-verify:
    # 是否启用异常验证任务，生产环境必须关闭
    enabled: false
    # 每 15 秒执行一次，仅用于开发测试
    cron: "0/15 * * * * ?"
```

文件位置：`src/main/java/io/github/atengk/schedule/properties/ExceptionVerifyScheduleProperties.java`

该配置属性类用于读取异常验证任务配置。

```java
package io.github.atengk.schedule.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 异常验证定时任务配置属性
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Component
@ConfigurationProperties(prefix = "schedule.exception-verify")
public class ExceptionVerifyScheduleProperties {

    /**
     * 是否启用任务
     */
    private Boolean enabled = false;

    /**
     * cron 表达式
     */
    private String cron = "0/15 * * * * ?";

}
```

文件位置：`src/main/java/io/github/atengk/schedule/task/ExceptionVerifyTask.java`

该任务类用于模拟异常场景，验证任务异常日志是否完整，以及下一次调度是否还能继续执行。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import io.github.atengk.schedule.properties.ExceptionVerifyScheduleProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 异常场景验证定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionVerifyTask {

    private final ExceptionVerifyScheduleProperties properties;

    /**
     * 每 15 秒执行一次异常验证任务
     */
    @Scheduled(cron = "${schedule.exception-verify.cron}", zone = "Asia/Shanghai")
    public void verifyException() {
        if (Boolean.FALSE.equals(properties.getEnabled())) {
            log.info("异常场景验证任务未启用，跳过执行");
            return;
        }

        TimeInterval timer = DateUtil.timer();
        log.info("异常场景验证任务开始执行，当前时间：{}", DateUtil.now());

        try {
            mockBusinessException();
            log.info("异常场景验证任务执行完成，耗时：{}ms", timer.interval());
        } catch (Exception e) {
            log.error("异常场景验证任务捕获到异常，耗时：{}ms", timer.interval(), e);
        }
    }

    /**
     * 模拟业务异常
     */
    private void mockBusinessException() {
        throw new IllegalStateException("模拟定时任务业务异常");
    }

}
```

验证时，将配置临时修改为：

```yaml
schedule:
  exception-verify:
    # 开发环境临时开启，用于验证异常处理
    enabled: true
    # 每 15 秒执行一次
    cron: "0/15 * * * * ?"
```

预期日志应包含以下内容：

```text
异常场景验证任务开始执行，当前时间：2026-05-08 10:00:00
异常场景验证任务捕获到异常，耗时：3ms
java.lang.IllegalStateException: 模拟定时任务业务异常
```

继续观察下一个 15 秒周期，如果任务仍然可以再次执行，说明异常不会导致整个调度停止。

异常验证重点如下：

| 验证项           | 预期结果                               |
| ---------------- | -------------------------------------- |
| 异常是否被捕获   | 日志中有完整异常堆栈                   |
| 后续调度是否继续 | 下一次触发周期仍然执行                 |
| 耗时是否记录     | 异常日志中包含耗时                     |
| 任务开关是否生效 | `enabled=false` 时任务跳过执行         |
| 告警是否触发     | 核心任务异常后能发送告警或记录告警日志 |

异常验证任务只能用于开发和测试环境。生产环境不应保留主动抛异常的验证任务，或者必须通过配置开关默认关闭。



## 开发注意事项

本章节用于说明 Spring 定时任务在实际开发和生产运行中需要重点关注的问题。定时任务通常运行在后台，如果缺少执行控制、日志监控和幂等设计，容易出现任务阻塞、重复执行、数据重复处理和分布式多节点并发执行等问题。

### 避免任务执行时间过长

定时任务执行时间过长会影响后续任务调度。如果使用默认单线程调度，一个任务长时间占用调度线程，其他任务可能会被延迟执行。即使配置了线程池，长时间任务也会持续占用线程资源，严重时可能导致任务堆积。

常见原因如下：

| 原因               | 说明                                 |
| ------------------ | ------------------------------------ |
| 单次处理数据量过大 | 一次扫描大量数据，导致执行时间不可控 |
| 外部接口响应慢     | 第三方接口超时或响应慢，阻塞任务线程 |
| 数据库慢查询       | 查询条件缺少索引，批量更新效率低     |
| 文件处理耗时       | 大文件导入、导出、压缩、上传耗时较长 |
| 任务逻辑过重       | 在定时任务中串行执行过多业务步骤     |

建议处理方式如下：

1. 控制单次任务处理数量，采用分页或分批处理。
2. 对外部接口设置超时时间，避免无限等待。
3. 数据库查询字段需要建立合适索引。
4. 大批量数据处理建议拆分批次执行。
5. 任务入口只负责调度，复杂业务放到 Service 中。
6. 记录任务耗时，长期观察任务是否变慢。
7. 对耗时任务配置独立线程池或接入专业调度平台。

文件位置：`src/main/java/io/github/atengk/schedule/task/BatchCleanTask.java`

该任务类演示分批清理数据，避免一次性处理过多记录导致任务执行时间过长。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 分批数据清理定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class BatchCleanTask {

    /**
     * 每 10 分钟清理一次过期数据
     */
    @Scheduled(cron = "${schedule.batch-clean.cron:0 0/10 * * * ?}", zone = "Asia/Shanghai")
    public void cleanExpiredData() {
        TimeInterval timer = DateUtil.timer();
        log.info("过期数据清理任务开始执行");

        int totalCount = 0;
        int batchSize = 500;

        try {
            while (true) {
                int cleanCount = cleanOneBatch(batchSize);
                totalCount += cleanCount;

                if (cleanCount < batchSize) {
                    break;
                }
            }

            log.info("过期数据清理任务执行完成，清理总数：{}，耗时：{}ms", totalCount, timer.interval());
        } catch (Exception e) {
            log.error("过期数据清理任务执行异常，已清理数量：{}，耗时：{}ms", totalCount, timer.interval(), e);
        }
    }

    /**
     * 清理一批过期数据
     *
     * @param batchSize 每批处理数量
     * @return 本批清理数量
     */
    private int cleanOneBatch(int batchSize) {
        // TODO 实际项目中可按 batchSize 查询并删除过期数据
        log.info("执行单批过期数据清理，批次大小：{}", batchSize);
        return 0;
    }

}
```

该写法通过批次控制任务执行压力，避免一次性加载大量数据。实际项目中还可以结合最大执行时长，例如任务超过 5 分钟后主动结束，等待下一轮继续处理。

### 避免重复执行风险

定时任务重复执行是生产环境中较常见的问题。重复执行可能来自任务执行时间过长、多线程并发、应用重启、多实例部署或手动补偿任务重复触发。

常见重复执行场景如下：

| 场景                       | 说明                                         |
| -------------------------- | -------------------------------------------- |
| 多实例部署                 | 每个服务实例都会执行同一个 `@Scheduled` 任务 |
| 任务执行时间超过周期       | 上一轮未结束，下一轮又开始执行               |
| 手动补偿与定时任务同时执行 | 人工触发补偿时，定时任务也在处理同一批数据   |
| 应用重启后重新扫描         | 未记录处理状态，导致历史数据再次被处理       |
| 消息或接口重复调用         | 定时任务重试时重复调用下游系统               |

避免重复执行的常见方案如下：

1. 使用任务执行状态字段控制处理流程。
2. 使用数据库唯一约束避免重复插入。
3. 使用 Redis 或 Redisson 分布式锁控制同一时间只有一个节点执行。
4. 对任务处理的数据加状态机，例如 `待处理`、`处理中`、`处理成功`、`处理失败`。
5. 每次处理前检查数据当前状态，只有符合条件的数据才允许处理。
6. 记录任务批次号，防止同一批数据重复处理。
7. 对外部接口调用使用业务唯一号，避免重复请求造成重复扣款、重复通知或重复生成记录。

文件位置：`src/main/java/io/github/atengk/schedule/task/NonRepeatTask.java`

该任务类演示使用本地运行标记避免单实例内同一个任务重复执行。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单实例防重复执行定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class NonRepeatTask {

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 每 30 秒执行一次任务，同一实例内避免重复执行
     */
    @Scheduled(cron = "${schedule.non-repeat.cron:0/30 * * * * ?}", zone = "Asia/Shanghai")
    public void executeTask() {
        if (!running.compareAndSet(false, true)) {
            log.info("任务正在执行中，本次调度跳过");
            return;
        }

        TimeInterval timer = DateUtil.timer();
        log.info("防重复执行任务开始，当前时间：{}", DateUtil.now());

        try {
            // TODO 实际项目中编写具体业务逻辑
            log.info("防重复执行任务完成，耗时：{}ms", timer.interval());
        } catch (Exception e) {
            log.error("防重复执行任务异常，耗时：{}ms", timer.interval(), e);
        } finally {
            running.set(false);
        }
    }

}
```

这种方式只能解决单个应用实例内部的重复执行问题，不能解决多实例部署下的重复执行问题。只要应用部署了多个节点，就需要使用分布式锁或调度平台控制。

### 分布式部署下的调度问题

在分布式部署环境中，每个服务实例都会启动自己的 Spring 容器，也会各自扫描并执行 `@Scheduled` 任务。因此，如果同一个应用部署了 3 个实例，同一个定时任务默认会执行 3 次。

例如：

```text
order-service-1 执行超时订单关闭任务
order-service-2 执行超时订单关闭任务
order-service-3 执行超时订单关闭任务
```

如果任务没有分布式控制，可能会导致同一批订单被多个节点同时处理，产生重复更新、重复通知、重复推送、重复生成报表等问题。

常见解决方案如下：

| 方案                  | 说明                                       | 适用场景                  |
| --------------------- | ------------------------------------------ | ------------------------- |
| Redis 分布式锁        | 通过 Redis 锁控制同一时间只有一个节点执行  | 中小型业务系统            |
| Redisson 分布式锁     | 基于 Redis，封装更完整，支持自动续期       | 推荐用于 Spring Boot 项目 |
| 数据库锁              | 通过任务表、唯一索引、状态字段控制执行权   | 不想引入 Redis 的项目     |
| ShedLock              | 专门解决 Spring 定时任务多节点重复执行问题 | 保留 `@Scheduled` 写法    |
| XXL-JOB               | 外部调度平台统一分发任务                   | 任务多、需要可视化管理    |
| PowerJob / ElasticJob | 分布式任务调度框架                         | 复杂任务、分片任务        |

如果项目已经使用 Redis，推荐使用 Redisson 分布式锁。

文件位置：`pom.xml`

以下依赖用于引入 Redisson 分布式锁能力。

```xml
<!-- Redisson：提供 Redis 分布式锁、限流器、延迟队列等能力 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.31.0</version>
</dependency>
```

文件位置：`src/main/resources/application.yml`

以下配置用于连接 Redis，并配置定时任务执行周期。

```yaml
spring:
  data:
    redis:
      # Redis 地址
      host: 127.0.0.1
      # Redis 端口
      port: 6379
      # Redis 密码，没有密码可删除该配置
      password:
      # Redis 数据库索引
      database: 0

schedule:
  order-timeout:
    # 每 1 分钟执行一次
    cron: "0 0/1 * * * ?"
```

文件位置：`src/main/java/io/github/atengk/schedule/task/DistributedOrderTimeoutTask.java`

该任务类使用 Redisson 分布式锁，确保多实例部署时同一时间只有一个节点执行超时订单处理任务。

```java
package io.github.atengk.schedule.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分布式超时订单定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedOrderTimeoutTask {

    private static final String LOCK_KEY = "lock:schedule:order-timeout";

    private final RedissonClient redissonClient;
    private final OrderTimeoutService orderTimeoutService;

    /**
     * 每 1 分钟处理一次超时未支付订单，多实例下通过分布式锁避免重复执行
     */
    @Scheduled(cron = "${schedule.order-timeout.cron:0 0/1 * * * ?}", zone = "Asia/Shanghai")
    public void handleTimeoutOrder() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        TimeInterval timer = DateUtil.timer();
        boolean locked = false;

        try {
            locked = lock.tryLock(0, 10, TimeUnit.MINUTES);
            if (!locked) {
                log.info("超时订单处理任务未获取到分布式锁，本次跳过");
                return;
            }

            log.info("超时订单处理任务获取分布式锁成功，开始执行，当前时间：{}", DateUtil.now());

            int closeCount = orderTimeoutService.closeTimeoutOrders();

            log.info("超时订单处理任务执行完成，关闭订单数量：{}，耗时：{}ms",
                    closeCount, timer.interval());
        } catch (Exception e) {
            log.error("超时订单处理任务执行异常，耗时：{}ms", timer.interval(), e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("超时订单处理任务释放分布式锁");
            }
        }
    }

}
```

分布式锁使用注意事项：

1. 锁的 key 要具有业务唯一性，例如 `lock:schedule:order-timeout`。
2. 锁等待时间不要过长，定时任务一般获取不到锁就跳过。
3. 锁释放必须放在 `finally` 中。
4. 释放锁前需要判断是否由当前线程持有。
5. 锁过期时间要大于任务正常执行时间。
6. 分布式锁只能防止多个节点同时执行，不能替代业务幂等设计。

### 任务幂等设计

任务幂等是指定时任务重复执行一次或多次，最终产生的业务结果仍然是正确的。生产环境中，即使已经使用分布式锁，也仍然需要设计幂等逻辑，因为任务可能因为重试、补偿、重启、接口超时等原因被再次执行。

幂等设计的核心原则是：任务执行前先判断当前数据状态，只有符合处理条件的数据才允许处理。

常见幂等设计方式如下：

| 方式           | 说明                                                 |
| -------------- | ---------------------------------------------------- |
| 状态判断       | 只处理特定状态的数据，例如只关闭 `待支付` 订单       |
| 唯一约束       | 使用业务唯一号防止重复插入                           |
| 乐观锁         | 更新时携带版本号，避免并发覆盖                       |
| 条件更新       | SQL 中增加状态条件，例如 `where status = 'WAIT_PAY'` |
| 执行记录       | 记录任务批次和处理结果，避免重复处理                 |
| 去重表         | 使用业务唯一键记录已处理数据                         |
| 外部请求幂等号 | 调用第三方接口时传入唯一业务请求号                   |

以关闭超时订单为例，不建议直接根据订单 ID 更新状态：

```sql
update order_info set status = 'CLOSED' where id = 1001;
```

更推荐增加状态条件，只关闭仍处于待支付状态的订单：

```sql
update order_info
set status = 'CLOSED'
where id = 1001
  and status = 'WAIT_PAY';
```

这样即使任务重复执行，已经关闭、已支付或已取消的订单也不会被错误更新。

文件位置：`src/main/java/io/github/atengk/schedule/service/impl/IdempotentOrderTimeoutServiceImpl.java`

该实现类演示超时订单关闭时的幂等处理思路，实际项目中可以替换为 MyBatis-Plus、JPA 或 JdbcTemplate 的真实数据库操作。

```java
package io.github.atengk.schedule.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import io.github.atengk.schedule.service.OrderTimeoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 幂等超时订单处理服务实现
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class IdempotentOrderTimeoutServiceImpl implements OrderTimeoutService {

    /**
     * 关闭超时未支付订单
     *
     * @return 关闭订单数量
     */
    @Override
    public int closeTimeoutOrders() {
        log.info("开始扫描超时未支付订单，当前时间：{}", DateUtil.now());

        List<Long> timeoutOrderIds = queryTimeoutWaitPayOrderIds();
        if (CollUtil.isEmpty(timeoutOrderIds)) {
            log.info("当前无超时未支付订单");
            return 0;
        }

        int closeCount = 0;
        for (Long orderId : timeoutOrderIds) {
            boolean closed = closeOrderIfWaitPay(orderId);
            if (closed) {
                closeCount++;
            }
        }

        log.info("超时未支付订单处理完成，扫描数量：{}，成功关闭数量：{}",
                timeoutOrderIds.size(), closeCount);
        return closeCount;
    }

    /**
     * 查询超时且仍处于待支付状态的订单 ID
     *
     * @return 订单 ID 列表
     */
    private List<Long> queryTimeoutWaitPayOrderIds() {
        // TODO 实际项目中查询 status = WAIT_PAY 且 create_time 超过支付时限的订单
        return List.of();
    }

    /**
     * 仅当订单仍处于待支付状态时关闭订单
     *
     * @param orderId 订单 ID
     * @return 是否关闭成功
     */
    private boolean closeOrderIfWaitPay(Long orderId) {
        // TODO 实际项目中执行条件更新：where id = ? and status = 'WAIT_PAY'
        log.info("执行订单关闭幂等处理，订单ID：{}", orderId);
        return true;
    }

}
```

幂等设计建议：

1. 查询条件和更新条件都要包含业务状态。
2. 不要只根据主键直接更新关键业务状态。
3. 对通知、推送、回调等操作增加发送记录。
4. 对生成类任务增加业务唯一键，例如报表日期、账单月份、用户 ID。
5. 对批处理任务记录处理批次和处理状态。
6. 失败重试时要能识别哪些数据已成功、哪些数据待重试。
7. 分布式锁和幂等设计应同时使用，不能只依赖其中一种。

## 总结

本章节对 Spring Boot 3 中 Spring 原生定时调度的开发流程、适用边界和后续扩展方向进行总结。Spring 定时调度适合轻量级、周期明确、接入成本低的后台任务，但在多实例部署、动态调度、任务监控和高可靠场景下需要结合其他方案增强。

### 核心开发流程

Spring 定时任务的核心开发流程可以概括为：引入依赖、启用调度、创建任务类、配置执行周期、编写业务逻辑、记录运行日志、验证执行效果。

推荐开发步骤如下：

| 步骤             | 说明                                       |
| ---------------- | ------------------------------------------ |
| 1. 准备依赖      | 确认项目已引入 Spring Boot 基础依赖        |
| 2. 启用调度      | 使用 `@EnableScheduling` 开启定时调度能力  |
| 3. 创建任务类    | 使用 `@Component` 将任务类交给 Spring 管理 |
| 4. 定义任务方法  | 使用 `@Scheduled` 配置执行周期             |
| 5. 编写业务逻辑  | 任务入口调用 Service，不直接堆积复杂业务   |
| 6. 配置任务周期  | 优先将 cron、开关、线程池参数放到配置文件  |
| 7. 增加异常处理  | 使用 `try-catch` 捕获异常并记录完整堆栈    |
| 8. 配置线程池    | 多任务场景下配置 `ThreadPoolTaskScheduler` |
| 9. 验证任务执行  | 通过启动日志、执行频率、异常场景验证       |
| 10. 处理生产风险 | 增加幂等、分布式锁、监控和告警             |

推荐基础代码结构如下：

```text
src/main/java/io/github/atengk
├── ScheduleApplication.java
└── schedule
    ├── config
    │   ├── SchedulingConfig.java
    │   └── SchedulingThreadPoolConfig.java
    ├── properties
    │   ├── ScheduleProperties.java
    │   └── ScheduleThreadPoolProperties.java
    ├── task
    │   ├── OrderTimeoutTask.java
    │   ├── DataSyncTask.java
    │   └── CacheRefreshTask.java
    └── service
        ├── OrderTimeoutService.java
        └── impl
            └── OrderTimeoutServiceImpl.java
```

一个生产可用的定时任务通常至少应具备以下能力：

| 能力         | 是否建议           |
| ------------ | ------------------ |
| 配置化 cron  | 建议               |
| 任务开关     | 建议               |
| 执行日志     | 必须               |
| 异常捕获     | 必须               |
| 执行耗时记录 | 必须               |
| 线程池配置   | 多任务时必须       |
| 幂等处理     | 涉及数据修改时必须 |
| 分布式锁     | 多实例部署时必须   |
| 告警通知     | 核心任务建议       |
| 执行记录落库 | 核心任务建议       |

### 适用边界

Spring 原生定时调度适合轻量级任务，不适合所有调度场景。开发前应根据任务复杂度、部署架构、可靠性要求和运维要求判断是否继续使用 `@Scheduled`。

适合使用 Spring 原生定时调度的场景如下：

| 场景           | 说明                                     |
| -------------- | ---------------------------------------- |
| 单体项目       | 应用只有一个实例，任务不会多节点重复执行 |
| 轻量后台任务   | 任务逻辑简单，执行耗时可控               |
| 周期固定       | 执行周期较少变化                         |
| 无复杂运维需求 | 不需要可视化管理、暂停、重试、分片       |
| 开发成本敏感   | 希望快速实现定时能力                     |

不太适合直接使用 Spring 原生定时调度的场景如下：

| 场景                   | 原因                                     |
| ---------------------- | ---------------------------------------- |
| 多实例部署且无锁控制   | 容易重复执行                             |
| 任务数量很多           | 缺少统一管理和监控                       |
| 需要动态修改执行周期   | 普通 `@Scheduled` 修改配置后通常需要重启 |
| 需要失败重试和补偿管理 | 原生能力较弱                             |
| 需要任务分片           | 不支持分布式分片执行                     |
| 需要可视化运维         | 没有任务控制台                           |
| 任务依赖关系复杂       | 不适合编排复杂任务流                     |

选型建议如下：

| 需求               | 推荐方案                                           |
| ------------------ | -------------------------------------------------- |
| 简单周期任务       | Spring `@Scheduled`                                |
| 多实例但任务简单   | `@Scheduled` + Redisson / ShedLock                 |
| 需要动态 cron      | `SchedulingConfigurer` + 数据库 / Redis / 配置中心 |
| 需要任务可视化管理 | XXL-JOB / PowerJob                                 |
| 需要分布式分片     | ElasticJob / PowerJob                              |
| 需要复杂任务编排   | 专业任务调度平台或工作流引擎                       |

### 后续扩展方向

当 Spring 原生定时调度无法满足生产要求时，可以从动态配置、分布式控制、任务监控、执行记录和调度平台几个方向扩展。

常见扩展方向如下：

| 扩展方向     | 说明                                         |
| ------------ | -------------------------------------------- |
| 动态 cron    | 将任务周期存入数据库或 Redis，运行时动态读取 |
| 任务开关     | 支持在线启用或停用某个任务                   |
| 分布式锁     | 多实例环境下保证同一时间只有一个节点执行     |
| 任务执行记录 | 将每次任务执行结果落库，便于审计和排查       |
| 失败告警     | 任务失败后发送企业微信、钉钉、邮件或短信     |
| 失败重试     | 对失败任务进行自动重试或人工补偿             |
| 任务分片     | 大任务拆分到多个节点并行执行                 |
| 可视化管理   | 使用调度平台统一管理任务                     |
| 监控指标     | 统计任务耗时、成功率、失败率、处理数量       |
| 链路追踪     | 将任务执行纳入统一日志和链路系统             |

如果继续基于 Spring 原生调度扩展，可以优先补充以下能力：

1. 统一任务配置属性。
2. 统一线程池配置。
3. 统一任务日志格式。
4. 统一异常告警服务。
5. 统一任务执行记录表。
6. Redis 或 Redisson 分布式锁。
7. 管理接口控制任务开关。
8. 动态 cron 配置读取。

如果任务规模持续扩大，建议逐步迁移到专门的调度平台，例如 XXL-JOB、PowerJob 或 ElasticJob。这样可以获得任务注册、可视化配置、执行日志、失败重试、任务分片、人工触发和调度监控等能力。

最终建议如下：

| 项目阶段       | 推荐做法                                |
| -------------- | --------------------------------------- |
| 开发验证阶段   | 使用 `@Scheduled` 快速实现              |
| 单体生产阶段   | `@Scheduled` + 线程池 + 日志 + 异常处理 |
| 多实例生产阶段 | `@Scheduled` + 分布式锁 + 幂等设计      |
| 多任务复杂阶段 | 引入 XXL-JOB、PowerJob 等调度平台       |
| 高可靠任务阶段 | 调度平台 + 任务记录 + 告警 + 补偿机制   |

Spring 定时调度的核心优势是简单、轻量、接入成本低。实际开发中，应优先保证任务可控、可观察、可重试和可幂等，避免只关注“能定时执行”，而忽略生产环境中的重复执行、异常中断、分布式并发和数据一致性问题。
