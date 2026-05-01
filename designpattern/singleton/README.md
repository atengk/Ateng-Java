# 设计模式：单例模式

单例模式用于保证一个类在 JVM 进程内只有一个实例，并提供全局访问入口。在 JDK21 和 Spring Boot 3 项目中，单例模式通常用于全局配置、轻量级工具封装、缓存管理器、序列号生成器、客户端连接包装器等场景。

需要注意：在 Spring Boot 项目中，大部分业务组件不建议手写单例，而是交给 Spring 容器管理。Spring Bean 默认就是单例作用域。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证单例行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，项目中用于字符串、ID、日期等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、构造方法等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于单元测试验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果使用 Spring Boot 3，建议使用 JDK17 及以上版本。当前文档以 JDK21 为基准，示例代码可以直接用于 Spring Boot 3 项目。

## 核心概念

单例模式的目标是控制对象创建，避免同一个全局组件被重复实例化。

常见实现方式如下：

| 实现方式           |   是否推荐 | 适用场景                           |
| -------------- | -----: | ------------------------------ |
| 枚举单例           |     推荐 | 纯 Java 工具、全局管理器、无 Spring 依赖的组件 |
| 静态内部类单例        |     推荐 | 需要懒加载的普通 Java 类                |
| 双重检查锁单例        | 可用但不优先 | 需要延迟加载且构造成本较高的对象               |
| Spring 单例 Bean |   强烈推荐 | Spring Boot 项目中的业务组件、服务类、配置类   |
| 饿汉式单例          |     可用 | 对象轻量，启动即需要                     |
| 懒汉式非线程安全写法     |    不推荐 | 多线程环境下存在并发问题                   |

在 Spring Boot 项目中，优先级通常是：

```text
Spring Bean 单例 > 枚举单例 > 静态内部类单例 > 双重检查锁单例
```

## 枚举单例

枚举单例是 Java 中非常稳妥的单例实现方式，天然防止反射破坏，并且天然支持序列化安全。适合无状态工具、轻状态管理器、全局 ID 生成器等场景。

文件位置：`src/main/java/io/github/atengk/design/singleton/GlobalTraceIdGenerator.java`

下面的枚举单例用于生成全局追踪 ID，可在非 Spring 管理的工具场景中直接调用。

```java
package io.github.atengk.design.singleton;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局追踪ID生成器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public enum GlobalTraceIdGenerator {

    /**
     * 单例实例
     */
    INSTANCE;

    /**
     * 生成追踪ID
     *
     * @return 追踪ID
     */
    public String nextTraceId() {
        String traceId = IdUtil.fastSimpleUUID();
        log.debug("生成追踪ID：{}", traceId);
        return traceId;
    }
}
```

使用方式：

```java
String traceId = GlobalTraceIdGenerator.INSTANCE.nextTraceId();
```

这种方式适合工具层或基础设施层，不依赖 Spring 容器。缺点是不能直接使用 Spring 的依赖注入能力。

## 静态内部类单例

静态内部类单例利用 JVM 类加载机制实现延迟加载和线程安全。它适合需要懒加载，但又不希望使用同步锁的场景。

文件位置：`src/main/java/io/github/atengk/design/singleton/CacheKeyBuilder.java`

下面的静态内部类单例用于统一构建缓存 Key。

```java
package io.github.atengk.design.singleton;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存Key构建器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class CacheKeyBuilder {

    private static final String DEFAULT_SEPARATOR = ":";

    private CacheKeyBuilder() {
        log.info("初始化缓存Key构建器");
    }

    /**
     * 获取单例实例
     *
     * @return 缓存Key构建器
     */
    public static CacheKeyBuilder getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 构建缓存Key
     *
     * @param module 模块名称
     * @param bizId  业务ID
     * @return 缓存Key
     */
    public String build(String module, String bizId) {
        if (StrUtil.hasBlank(module, bizId)) {
            log.warn("构建缓存Key失败，模块名称或业务ID为空");
            throw new IllegalArgumentException("模块名称和业务ID不能为空");
        }

        String cacheKey = StrUtil.join(DEFAULT_SEPARATOR, module, bizId);
        log.debug("构建缓存Key：{}", cacheKey);
        return cacheKey;
    }

    /**
     * 单例持有者
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static class Holder {

        private static final CacheKeyBuilder INSTANCE = new CacheKeyBuilder();
    }
}
```

使用方式：

```java
String cacheKey = CacheKeyBuilder.getInstance().build("user", "10001");
```

静态内部类单例的优点是懒加载、线程安全、代码清晰。缺点是仍然属于手写单例，不适合需要 Spring 注入其他 Bean 的复杂业务类。

## 双重检查锁单例

双重检查锁，也就是 Double-Checked Locking，适合构造成本较高、需要延迟初始化的对象。JDK5 以后必须配合 `volatile` 使用，否则可能出现指令重排序问题。

文件位置：`src/main/java/io/github/atengk/design/singleton/HeavyClientHolder.java`

下面的示例模拟一个重量级客户端对象的单例持有器。

```java
package io.github.atengk.design.singleton;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 重量级客户端持有器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class HeavyClientHolder {

    private static volatile HeavyClientHolder instance;

    private final String initTime;

    private HeavyClientHolder() {
        this.initTime = DateUtil.now();
        log.info("初始化重量级客户端持有器，初始化时间：{}", initTime);
    }

    /**
     * 获取单例实例
     *
     * @return 重量级客户端持有器
     */
    public static HeavyClientHolder getInstance() {
        if (instance == null) {
            synchronized (HeavyClientHolder.class) {
                if (instance == null) {
                    instance = new HeavyClientHolder();
                }
            }
        }
        return instance;
    }

    /**
     * 执行客户端请求
     *
     * @param requestId 请求ID
     * @return 执行结果
     */
    public String execute(String requestId) {
        log.info("执行客户端请求，请求ID：{}，客户端初始化时间：{}", requestId, initTime);
        return "执行成功，requestId=" + requestId + ", initTime=" + initTime;
    }
}
```

这种方式代码比静态内部类复杂，除非确实需要更细粒度控制初始化逻辑，否则优先使用枚举单例、静态内部类单例或 Spring Bean 单例。

## Spring Boot 单例 Bean

在 Spring Boot 3 中，`@Component`、`@Service`、`@Repository`、`@Controller` 等组件默认都是单例 Bean。也就是说，Spring 容器启动时会创建一个 Bean 实例，后续注入的都是同一个对象。

这也是 Spring Boot 项目中最推荐的单例方式。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── SingletonApplication.java
├── controller/
│   └── SerialNumberController.java
└── service/
    ├── SerialNumberService.java
    └── impl/
        └── SerialNumberServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/SingletonApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 单例模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class SingletonApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SingletonApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/SerialNumberService.java`

下面是序列号服务接口。

```java
package io.github.atengk.design.service;

/**
 * 序列号服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface SerialNumberService {

    /**
     * 生成下一个序列号
     *
     * @param bizType 业务类型
     * @return 序列号
     */
    String nextSerialNumber(String bizType);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/SerialNumberServiceImpl.java`

下面是默认单例 Bean 实现，使用 `ConcurrentHashMap` 和 `AtomicLong` 保证并发场景下的序列号递增安全。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.service.SerialNumberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 序列号服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class SerialNumberServiceImpl implements SerialNumberService {

    private final ConcurrentHashMap<String, AtomicLong> counterMap = new ConcurrentHashMap<>();

    /**
     * 生成下一个序列号
     *
     * @param bizType 业务类型
     * @return 序列号
     */
    @Override
    public String nextSerialNumber(String bizType) {
        if (StrUtil.isBlank(bizType)) {
            log.warn("生成序列号失败，业务类型为空");
            throw new IllegalArgumentException("业务类型不能为空");
        }

        AtomicLong counter = counterMap.computeIfAbsent(bizType, key -> {
            log.info("初始化业务类型计数器，业务类型：{}", key);
            return new AtomicLong(0);
        });

        long sequence = counter.incrementAndGet();
        String date = DateUtil.format(DateUtil.date(), "yyyyMMdd");
        String serialNumber = StrUtil.format("{}-{}-{}", bizType.toUpperCase(), date, sequence);

        log.info("生成序列号成功，业务类型：{}，序列号：{}", bizType, serialNumber);
        return serialNumber;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/SerialNumberController.java`

下面是用于验证 Spring 单例 Bean 行为的接口层代码。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.service.SerialNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 序列号控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
public class SerialNumberController {

    private final SerialNumberService serialNumberService;

    /**
     * 获取下一个序列号
     *
     * @param bizType 业务类型
     * @return 序列号
     */
    @GetMapping("/singleton/serial-number")
    public String nextSerialNumber(@RequestParam String bizType) {
        return serialNumberService.nextSerialNumber(bizType);
    }
}
```

接口调用示例：

```bash
curl "http://localhost:8080/singleton/serial-number?bizType=order"
curl "http://localhost:8080/singleton/serial-number?bizType=order"
curl "http://localhost:8080/singleton/serial-number?bizType=pay"
```

可能返回：

```text
ORDER-20260430-1
ORDER-20260430-2
PAY-20260430-1
```

这里的 `SerialNumberServiceImpl` 是 Spring 管理的单例 Bean。由于 `counterMap` 是对象字段，因此同一个 Bean 实例内会持续维护不同业务类型的计数器。

## Spring 单例 Bean 与普通单例的区别

Spring 单例 Bean 的单例范围是 Spring 容器级别，不是 JVM 全局级别。

也就是说：

```text
一个 Spring ApplicationContext 中，同一个 Bean 默认只有一个实例。
多个 ApplicationContext 中，同一个 Bean 可以有多个实例。
多个 JVM 进程中，同一个 Bean 也会有多个实例。
```

普通 Java 单例通常是 JVM 级别的单例。在单 JVM 单类加载器场景下，一个类对应一个单例对象。

在 Spring Boot 项目中，推荐这样选择：

| 场景                                   | 推荐方式                           |
| ------------------------------------ | ------------------------------ |
| Service、Manager、Client、Handler 等业务组件 | Spring 单例 Bean                 |
| 无需依赖 Spring 的基础工具                    | 枚举单例                           |
| 需要懒加载的普通 Java 对象                     | 静态内部类单例                        |
| 复杂初始化且必须手动控制                         | 双重检查锁单例                        |
| 分布式全局唯一对象                            | 不应使用本地单例，应使用 Redis、数据库、分布式锁等方案 |

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行接口：

```bash
curl "http://localhost:8080/singleton/serial-number?bizType=order"
curl "http://localhost:8080/singleton/serial-number?bizType=order"
```

如果第二次请求返回的序号递增，说明 Spring 单例 Bean 中的状态被同一个实例持续维护。

也可以通过日志观察：

```text
初始化业务类型计数器，业务类型：order
生成序列号成功，业务类型：order，序列号：ORDER-20260430-1
生成序列号成功，业务类型：order，序列号：ORDER-20260430-2
```

`初始化业务类型计数器` 只打印一次，说明同一个业务类型的计数器没有被重复创建。

## 注意事项

单例对象中不要随意保存用户级、请求级、线程级状态。例如当前登录用户、请求参数、分页参数、临时表单数据等，不应该放在单例 Bean 的成员变量中。

错误示例：

```java
private Long currentUserId;
private String currentRequestId;
private List<String> currentImportRows;
```

这些字段在并发请求下会互相污染，导致线程安全问题。

Spring 单例 Bean 中可以保存线程安全的共享资源，例如：

```java
private final ConcurrentHashMap<String, AtomicLong> counterMap = new ConcurrentHashMap<>();
```

如果共享状态涉及分布式部署，不要依赖本地单例。比如订单号、库存扣减、优惠券发放、限流计数等场景，应使用 Redis、数据库唯一约束、分布式锁或专门的中间件来保证全局一致性。

## 总结

在 JDK21 和 Spring Boot 3 项目中，单例模式的实践重点不是“如何手写一个单例”，而是“如何选择合适的单例管理边界”。

普通 Java 工具类优先使用枚举单例或静态内部类单例。Spring Boot 业务组件优先交给 Spring 容器管理。只要组件由 Spring 扫描并注册为 Bean，默认就是单例作用域，不需要额外手写单例逻辑。
