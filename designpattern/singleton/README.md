# 单例模式

单例模式确保一个类在整个应用中只有一个实例，并提供全局访问点。在 Spring 中最常见的体现是 Bean 默认是单例作用域（`@Scope("singleton")`）。这种模式常用于管理全局资源，如配置类、工具类、线程池等。


 下面包含三种实用变体（推荐在企业项目中优先使用前两种）：

- **1）Spring 管理的单例 Bean（推荐）** — 最符合 Spring 生态、简单且线程安全，适合大多数场景（配置、工具、ID 生成器等）。
- **2）枚举单例（Enum Singleton）** — JDK 推荐的线程安全单例实现，适合独立工具类或非 Spring 管理的全局单例。
- **3）双重检查锁（DCL）懒加载单例** — 经典写法，适合需要懒初始化并且不使用 Spring 管理时的场景（示范用途）。

下面直接给出可复制到项目的完整代码（每个文件的包名均为 `io.github.atengk.designpattern.singleton`）。你可以把它们放进 `src/main/java`，启动 Spring Boot 后访问演示接口。

------

## Spring 管理的单例服务

```java
package io.github.atengk.designpattern.singleton;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Spring 管理的单例服务。
 *
 * <p>说明：
 * 1. Spring 容器中默认的 Bean 作用域是单例（singleton），因此使用 {@code @Service} 注解的类在容器中是单例的。
 * 2. 本类演示一个线程安全的 ID 生成器，适合作为应用级全局服务（例如订单号、流水号等）。
 * 3. 推荐在业务中优先使用 Spring 管理的单例 Bean，而非手动实现单例，便于集成 AOP、配置、测试等功能。
 * </p>
 */
@Service
public class SpringSingletonService {

    /**
     * 线程安全的自增计数器，用于生成唯一 ID。
     */
    private final AtomicLong idCounter = new AtomicLong(0L);

    /**
     * 生成下一个唯一 ID。
     *
     * @return 下一个唯一 ID（long 类型）
     */
    public long nextId() {
        return idCounter.incrementAndGet();
    }

    /**
     * 获取当前计数值（不会改变计数器）。
     *
     * @return 当前计数值
     */
    public long currentId() {
        return idCounter.get();
    }
}
```

------

## 枚举单例实现

```java
package io.github.atengk.designpattern.singleton;

/**
 * 枚举单例实现。
 *
 * <p>说明：
 * 1. 使用枚举实现单例是最简单、最安全的单例实现方式，能防止反射、序列化导致的多实例问题。
 * 2. 适用于独立工具类或在非 Spring 管理环境下需要单例的场景。
 * </p>
 */
public enum EnumSingleton {
    /**
     * 单例实例。
     */
    INSTANCE;

    /**
     * 示例状态或配置字段，演示可在单例中持有状态。
     */
    private String configValue = "default";

    /**
     * 获取配置值。
     *
     * @return 当前配置值
     */
    public String getConfigValue() {
        return configValue;
    }

    /**
     * 设置配置值。
     *
     * @param configValue 新的配置值
     */
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
```

------

## 双重检查锁（Double-Checked Locking，DCL）单例实现

```java
package io.github.atengk.designpattern.singleton;

/**
 * 双重检查锁（Double-Checked Locking，DCL）单例实现。
 *
 * <p>说明：
 * 1. 这是经典的懒加载单例实现，适合不使用 Spring 管理、但想在第一次使用时才初始化的场景。
 * 2. 该实现使用 volatile 修饰实例变量以保证可见性，避免指令重排问题。
 * 3. 在 Spring 管理环境中通常不推荐使用手写单例，除非有特殊理由。
 * </p>
 */
public final class DoubleCheckedLockingSingleton {

    /**
     * 单例实例，使用 volatile 防止指令重排导致的安全问题。
     */
    private static volatile DoubleCheckedLockingSingleton instance;

    /**
     * 私有构造函数，防止外部直接实例化。
     */
    private DoubleCheckedLockingSingleton() {
        // Prevent instantiation
    }

    /**
     * 获取单例实例。使用双重检查锁以降低同步开销。
     *
     * @return 单例实例
     */
    public static DoubleCheckedLockingSingleton getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckedLockingSingleton.class) {
                if (instance == null) {
                    instance = new DoubleCheckedLockingSingleton();
                }
            }
        }
        return instance;
    }

    /**
     * 示例方法，返回一个字符串以示作用。
     *
     * @return 示例字符串
     */
    public String hello() {
        return "hello from DCL singleton";
    }
}
```

------

## 单例模式演示控制器

```java
package io.github.atengk.designpattern.singleton;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 单例模式演示控制器。
 *
 * <p>提供简单的 HTTP 接口以验证三种单例用法在运行时的行为。</p>
 */
@RestController
@RequestMapping("/designpattern/singleton")
public class SingletonDemoController {

    private final SpringSingletonService springSingletonService;

    /**
     * Spring 会注入 {@link SpringSingletonService}，该 Bean 在容器中为单例。
     *
     * @param springSingletonService 注入的单例服务
     */
    public SingletonDemoController(SpringSingletonService springSingletonService) {
        this.springSingletonService = springSingletonService;
    }

    /**
     * 演示 Spring 单例 Bean 的 ID 生成功能。
     *
     * @return 包含生成 ID 的 JSON 对象
     */
    @GetMapping("/spring/id")
    public Map<String, Object> springId() {
        Map<String, Object> result = new HashMap<>(4);
        long id = springSingletonService.nextId();
        result.put("type", "spring-singleton");
        result.put("id", id);
        result.put("currentId", springSingletonService.currentId());
        return result;
    }

    /**
     * 演示枚举单例的读取与修改状态。
     *
     * @return 当前枚举单例的状态
     */
    @GetMapping("/enum/value")
    public Map<String, Object> enumValue() {
        Map<String, Object> result = new HashMap<>(4);
        EnumSingleton singleton = EnumSingleton.INSTANCE;
        String before = singleton.getConfigValue();
        singleton.setConfigValue(before + "-updated");
        result.put("type", "enum-singleton");
        result.put("before", before);
        result.put("after", singleton.getConfigValue());
        return result;
    }

    /**
     * 演示 DCL 单例的简单方法调用。
     *
     * @return DCL 单例返回的示例字符串
     */
    @GetMapping("/dcl/hello")
    public Map<String, Object> dclHello() {
        Map<String, Object> result = new HashMap<>(3);
        DoubleCheckedLockingSingleton instance = DoubleCheckedLockingSingleton.getInstance();
        result.put("type", "dcl-singleton");
        result.put("message", instance.hello());
        return result;
    }
}
```

------

## 实践建议

- **优先使用 Spring 单例 Bean**：绝大多数服务类、资源管理、工具类都应该交给 Spring 管理。这样可以享受依赖注入、AOP、生命周期管理、测试替换等优点。
- **枚举单例**：当你需要一个**与 Spring 无关**的、抵抗反射与序列化破坏的单例（例如某些低依赖的工具或库代码）时使用。
- **DCL 懒加载单例**：仅在确实需要延迟初始化但又不使用 Spring 管理的场景中使用，且实现时必须使用 `volatile` 保证线程安全。

