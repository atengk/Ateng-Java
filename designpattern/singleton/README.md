# 单例模式

单例模式属于创建型模式，用于保证一个类在系统运行期间只有一个实例，并提供统一访问入口。在 Spring Boot 项目中，最常见、最推荐的单例落地方式不是手写 `getInstance()`，而是直接使用 Spring 容器默认的单例 Bean。该文档基于当前 29 个设计模式总览中的分类口径展开，单例模式位于创建型模式，模块名为 `singleton`。

## 基础配置

本示例基于 JDK 21、Spring Boot 3、Maven、Hutool、Lombok 编写。示例场景是维护一个应用运行上下文对象 `SystemRuntimeContext`，它在整个应用中只需要一个实例，用于保存应用实例标识、基础配置和请求序号。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证单例实例 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、对象、ID、日期等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok，用于减少 getter、setter、构造方法和日志样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于后续验证单例行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

demo:
  singleton:
    # 应用名称，用于演示单例 Bean 读取配置
    app-name: design-pattern-demo
    # 系统基础访问地址，用于演示配置默认值处理
    base-url: http://localhost:8080
```

## 模式说明

单例模式解决的是“对象实例唯一”的问题。它适合用于保存系统级共享对象，例如配置上下文、客户端连接管理器、缓存管理器、任务调度器、全局编号生成器、规则注册表等。

在 Spring Boot 中，默认情况下 `@Component`、`@Service`、`@Repository`、`@Controller` 注册到容器中的 Bean 都是单例作用域。也就是说，同一个 Bean 在容器中只有一个实例，不需要额外写双重检查锁、静态内部类或枚举单例。

单例模式在项目中要注意一个边界：单例对象如果保存可变状态，必须考虑线程安全。Spring Boot 的单例 Bean 会被多个请求线程共享，如果直接使用普通成员变量保存请求级数据，容易出现并发问题。请求级数据应放在方法局部变量、数据库、Redis、ThreadLocal 或专门的上下文对象中。

## 项目结构

本示例按照 Spring Boot 常规分层组织，重点展示单例 Bean 的声明、注入、使用和验证。

```text
src/main/java/io/github/atengk/pattern/singleton
├── SingletonApplication.java
├── config
│   └── SingletonDemoProperties.java
├── context
│   └── SystemRuntimeContext.java
├── controller
│   └── SingletonDemoController.java
├── service
│   └── SingletonDemoService.java
└── vo
    └── SingletonInfoVO.java
```

## Spring Boot 单例 Bean 实现

这一部分使用 Spring 容器管理单例实例。`SystemRuntimeContext` 被声明为 `@Component`，默认就是单例 Bean。每次接口调用时，Controller 和 Service 注入的都是同一个对象实例。

文件位置：`src/main/java/io/github/atengk/pattern/singleton/SingletonApplication.java`

```java
package io.github.atengk.pattern.singleton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 单例模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class SingletonApplication {

    /**
     * 启动单例模式示例应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SingletonApplication.class, args);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/singleton/config/SingletonDemoProperties.java`

```java
package io.github.atengk.pattern.singleton.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 单例模式示例配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Component
@ConfigurationProperties(prefix = "demo.singleton")
public class SingletonDemoProperties {

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 系统基础地址
     */
    private String baseUrl;

}
```

下面的类是核心单例对象。它由 Spring 容器创建和管理，应用启动后只会初始化一次。

文件位置：`src/main/java/io/github/atengk/pattern/singleton/context/SystemRuntimeContext.java`

```java
package io.github.atengk.pattern.singleton.context;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.singleton.config.SingletonDemoProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 系统运行上下文单例对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class SystemRuntimeContext implements InitializingBean {

    private final SingletonDemoProperties properties;

    /**
     * 当前应用实例标识，应用启动后只生成一次
     */
    private final String instanceCode = IdUtil.fastSimpleUUID();

    /**
     * 请求序号，单例对象中的可变状态需要使用线程安全类型
     */
    private final AtomicLong requestSequence = new AtomicLong(0);

    /**
     * Bean 初始化完成后执行校验和日志输出
     */
    @Override
    public void afterPropertiesSet() {
        if (StrUtil.isBlank(properties.getAppName())) {
            throw new IllegalArgumentException("应用名称不能为空，请检查 demo.singleton.app-name 配置");
        }

        log.info("初始化系统运行上下文单例对象，实例标识：{}，应用名称：{}", instanceCode, properties.getAppName());
    }

    /**
     * 获取下一个请求序号
     *
     * @return 请求序号
     */
    public long nextRequestSequence() {
        return requestSequence.incrementAndGet();
    }

    /**
     * 获取系统基础地址
     *
     * @return 系统基础地址
     */
    public String getBaseUrl() {
        return ObjectUtil.defaultIfNull(properties.getBaseUrl(), "http://localhost:8080");
    }

    /**
     * 获取应用名称
     *
     * @return 应用名称
     */
    public String getAppName() {
        return properties.getAppName();
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/singleton/vo/SingletonInfoVO.java`

```java
package io.github.atengk.pattern.singleton.vo;

/**
 * 单例模式运行信息响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record SingletonInfoVO(
        String instanceCode,
        Long requestSequence,
        String appName,
        String baseUrl,
        String requestTime
) {
}
```

下面的 Service 注入 `SystemRuntimeContext`。无论接口被调用多少次，`runtimeContext` 指向的都是同一个 Spring Bean 实例。

文件位置：`src/main/java/io/github/atengk/pattern/singleton/service/SingletonDemoService.java`

```java
package io.github.atengk.pattern.singleton.service;

import cn.hutool.core.date.DateUtil;
import io.github.atengk.pattern.singleton.context.SystemRuntimeContext;
import io.github.atengk.pattern.singleton.vo.SingletonInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 单例模式示例服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SingletonDemoService {

    private final SystemRuntimeContext runtimeContext;

    /**
     * 获取系统运行上下文信息
     *
     * @return 单例运行信息
     */
    public SingletonInfoVO getRuntimeInfo() {
        long sequence = runtimeContext.nextRequestSequence();

        log.info("获取单例运行信息，实例标识：{}，请求序号：{}", runtimeContext.getInstanceCode(), sequence);

        return new SingletonInfoVO(
                runtimeContext.getInstanceCode(),
                sequence,
                runtimeContext.getAppName(),
                runtimeContext.getBaseUrl(),
                DateUtil.now()
        );
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/singleton/controller/SingletonDemoController.java`

```java
package io.github.atengk.pattern.singleton.controller;

import io.github.atengk.pattern.singleton.service.SingletonDemoService;
import io.github.atengk.pattern.singleton.vo.SingletonInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 单例模式示例接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/api/patterns/singleton")
@RequiredArgsConstructor
public class SingletonDemoController {

    private final SingletonDemoService singletonDemoService;

    /**
     * 查询单例运行信息
     *
     * @return 单例运行信息
     */
    @GetMapping("/runtime")
    public SingletonInfoVO runtime() {
        return singletonDemoService.getRuntimeInfo();
    }

}
```

## 使用方式

启动项目后，访问接口即可验证 Spring 容器中的 Bean 是否为单例。重点观察 `instanceCode` 是否保持不变，以及 `requestSequence` 是否递增。

```bash
curl -X GET "http://localhost:8080/api/patterns/singleton/runtime"
```

响应示例：

```json
{
  "instanceCode": "9e52fcd256c84a8292f45123c45d9a21",
  "requestSequence": 1,
  "appName": "design-pattern-demo",
  "baseUrl": "http://localhost:8080",
  "requestTime": "2026-05-13 10:20:31"
}
```

再次请求：

```bash
curl -X GET "http://localhost:8080/api/patterns/singleton/runtime"
```

响应示例：

```json
{
  "instanceCode": "9e52fcd256c84a8292f45123c45d9a21",
  "requestSequence": 2,
  "appName": "design-pattern-demo",
  "baseUrl": "http://localhost:8080",
  "requestTime": "2026-05-13 10:20:35"
}
```

如果两次响应中的 `instanceCode` 一致，说明访问的是同一个单例对象。如果 `requestSequence` 持续递增，说明该对象中的线程安全状态也在同一个实例中维护。

## 非 Spring 场景的单例写法

在 Spring Boot 项目中，大部分业务类不需要手写单例。但有些对象不交给 Spring 管理，例如纯 Java SDK、轻量级本地注册表、静态工具上下文，可以使用静态内部类方式实现懒加载单例。

下面示例是一个非 Spring 管理的本地配置注册表。它使用静态内部类实现线程安全懒加载，并使用 `ConcurrentHashMap` 保存共享配置。

文件位置：`src/main/java/io/github/atengk/pattern/singleton/standalone/StandaloneConfigRegistry.java`

```java
package io.github.atengk.pattern.singleton.standalone;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 非 Spring 管理的本地配置注册表单例
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public final class StandaloneConfigRegistry {

    private final Map<String, String> configs = new ConcurrentHashMap<>();

    private StandaloneConfigRegistry() {
        log.info("初始化本地配置注册表单例对象");
    }

    /**
     * 获取单例实例
     *
     * @return 本地配置注册表
     */
    public static StandaloneConfigRegistry getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 写入配置项
     *
     * @param key   配置键
     * @param value 配置值
     */
    public void put(String key, String value) {
        if (StrUtil.hasBlank(key, value)) {
            throw new IllegalArgumentException("配置键和值不能为空");
        }

        configs.put(key, value);
        log.info("写入本地配置项，key：{}", key);
    }

    /**
     * 读取配置项
     *
     * @param key 配置键
     * @return 配置值
     */
    public String get(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }

        return configs.get(key);
    }

    /**
     * 静态内部类持有单例实例
     *
     * @author Ateng
     * @since 2026-05-13
     */
    private static class Holder {
        private static final StandaloneConfigRegistry INSTANCE = new StandaloneConfigRegistry();
    }

}
```

普通 Java 调用方式如下：

```java
StandaloneConfigRegistry registry = StandaloneConfigRegistry.getInstance();
registry.put("order.timeout", "30");

String timeout = StandaloneConfigRegistry.getInstance().get("order.timeout");
```

这种方式适合不依赖 Spring 容器的场景。如果类本身需要注入数据库、Redis、配置文件、消息队列、其他 Service，则不建议这样写，应该交给 Spring 容器管理。

## 验证方式

可以使用接口连续请求验证，也可以使用 Spring Boot 测试验证容器中只有一个 Bean 实例。

文件位置：`src/test/java/io/github/atengk/pattern/singleton/SystemRuntimeContextTest.java`

```java
package io.github.atengk.pattern.singleton;

import io.github.atengk.pattern.singleton.context.SystemRuntimeContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 系统运行上下文单例测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@SpringBootTest
class SystemRuntimeContextTest {

    private final SystemRuntimeContext runtimeContext;

    private final ObjectProvider<SystemRuntimeContext> runtimeContextProvider;

    SystemRuntimeContextTest(SystemRuntimeContext runtimeContext,
                             ObjectProvider<SystemRuntimeContext> runtimeContextProvider) {
        this.runtimeContext = runtimeContext;
        this.runtimeContextProvider = runtimeContextProvider;
    }

    /**
     * 验证 Spring 容器中的运行上下文是同一个实例
     */
    @Test
    void shouldUseSameSingletonBean() {
        SystemRuntimeContext anotherRuntimeContext = runtimeContextProvider.getObject();

        log.info("第一次获取的实例标识：{}", runtimeContext.getInstanceCode());
        log.info("第二次获取的实例标识：{}", anotherRuntimeContext.getInstanceCode());

        Assertions.assertSame(runtimeContext, anotherRuntimeContext);
        Assertions.assertEquals(runtimeContext.getInstanceCode(), anotherRuntimeContext.getInstanceCode());
    }

}
```

执行测试：

```bash
mvn test -Dtest=SystemRuntimeContextTest
```

命令说明：`-Dtest=SystemRuntimeContextTest` 表示只运行当前单例模式测试类，便于快速验证 Spring 容器中的 Bean 实例是否一致。

## 开发建议

在 Spring Boot 项目中，单例模式优先使用 Spring Bean 默认作用域，不要为了“使用设计模式”而手写复杂单例代码。业务 Service、配置对象、客户端对象、管理器对象一般都可以交给 Spring 容器管理。

适合使用单例的对象通常具备以下特点：创建成本较高、全局共享、没有请求级状态、生命周期和应用一致。例如支付客户端、短信客户端、规则注册表、缓存管理器、应用运行上下文等。

不适合使用单例的对象包括：保存用户登录信息的对象、保存单次请求参数的对象、保存临时计算结果的对象、需要频繁变化且没有并发保护的对象。这类数据应该放在方法局部变量、数据库、Redis、请求上下文或线程安全容器中。

在单例 Bean 中保存可变状态时，必须使用线程安全方案，例如 `AtomicLong`、`ConcurrentHashMap`、不可变对象、锁机制或外部存储。不要在单例 Service 中直接定义普通成员变量来保存请求参数。

## 总结

单例模式在 Spring Boot 项目中最常见的形态就是默认单例 Bean。它的核心价值不是写出某种固定模板，而是保证共享对象只有一个实例，避免重复创建和状态分裂。

实际开发中可以按以下原则使用：

```text
由 Spring 管理的业务对象：优先使用默认 singleton Bean
不依赖 Spring 的纯 Java 对象：可以使用静态内部类或枚举单例
单例对象不保存请求级状态：避免并发污染
必须保存共享状态时：使用线程安全结构
需要依赖其他组件时：不要手写单例，交给 Spring 容器管理
```

对于 Spring Boot 后端项目，单例模式的最佳实践是“让容器管理生命周期，让代码保持无状态或线程安全”。
