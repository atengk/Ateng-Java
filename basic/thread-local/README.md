# ThreadLocal

## ThreadLocal 概述

ThreadLocal 是 Java 提供的一种线程本地变量机制，用于在当前线程中保存独立的数据副本。它常用于 Java Web 项目中的用户上下文、请求上下文、链路追踪标识、数据源切换标识等场景，可以减少上下文参数在多层方法之间反复传递。

ThreadLocal 的核心特点是：同一个 ThreadLocal 对象在不同线程中访问时，获取到的是各自线程内部保存的数据，不同线程之间的数据互不影响。

### 基本概念

ThreadLocal 可以理解为“当前线程专属的变量容器”。普通变量通常由对象实例、方法参数或局部变量持有，而 ThreadLocal 变量与当前线程绑定。

ThreadLocal 对象本身并不直接保存业务数据，真正的数据存储在当前线程对象内部的 `ThreadLocalMap` 中。ThreadLocal 只是作为访问入口，通过 `set`、`get`、`remove` 等方法操作当前线程中的数据。

常用方法如下：

| 方法                                          | 说明                                        |
| --------------------------------------------- | ------------------------------------------- |
| `set(T value)`                                | 将数据保存到当前线程中                      |
| `get()`                                       | 从当前线程中获取当前 ThreadLocal 对应的数据 |
| `remove()`                                    | 删除当前线程中当前 ThreadLocal 对应的数据   |
| `withInitial(Supplier<? extends T> supplier)` | 创建带初始化默认值的 ThreadLocal            |

ThreadLocal 的基本使用方式如下：

```java
private static final ThreadLocal<String> USER_ID_LOCAL = new ThreadLocal<>();

// 设置当前线程的用户ID
USER_ID_LOCAL.set("10001");

// 获取当前线程的用户ID
String userId = USER_ID_LOCAL.get();

// 清理当前线程中的用户ID
USER_ID_LOCAL.remove();
```

在 Web 项目中，一个请求通常由一个工作线程处理。请求进入系统后，可以将当前用户 ID、租户 ID、TraceId 等信息放入 ThreadLocal。后续 Controller、Service、Mapper 或工具类只要仍在同一个线程中执行，就可以直接从 ThreadLocal 中获取这些上下文信息。

需要注意的是，ThreadLocal 的数据生命周期并不等同于一次请求的生命周期。尤其在 Tomcat、Undertow、线程池等线程复用场景中，请求处理完成后线程不会立即销毁，因此必须主动调用 `remove()` 清理数据，避免上下文残留。

### 典型使用场景

ThreadLocal 适合保存“当前线程执行链路内需要共享，但不适合作为方法参数层层传递”的数据。它不是全局缓存，也不适合做线程之间的数据共享。

常见使用场景如下：

| 使用场景              | 说明                                                     |
| --------------------- | -------------------------------------------------------- |
| 用户上下文传递        | 保存当前登录用户 ID、用户名、角色、租户编号等信息        |
| 请求链路 TraceId 传递 | 保存一次请求的 TraceId，便于日志追踪和问题排查           |
| 数据源切换上下文      | 保存当前线程需要使用的数据源标识，例如主库、从库、租户库 |
| 事务或会话上下文管理  | 保存当前线程中的数据库连接、Session 或事务状态           |
| 日志上下文传递        | 配合 MDC 保存 TraceId、UserId 等字段，输出到日志中       |
| 请求基础信息保存      | 保存客户端 IP、请求来源、语言标识、设备类型等请求级信息  |

在实际 Spring Boot 项目中，ThreadLocal 通常不会直接散落在业务代码中使用，而是封装成上下文工具类。例如 `UserContextHolder`、`TraceContextHolder`、`TenantContextHolder` 等。

典型流程如下：

1. 请求进入系统。
2. 过滤器或拦截器解析 Token、请求头或参数。
3. 获取当前用户 ID、租户 ID、TraceId 等上下文数据。
4. 将上下文数据写入 ThreadLocal。
5. Controller、Service、Mapper 等业务代码读取当前线程上下文。
6. 请求结束后，在 `finally` 或拦截器的 `afterCompletion` 中清理 ThreadLocal。

示例流程代码如下：

```java
try {
    USER_ID_LOCAL.set("10001");

    // 执行业务逻辑
    handleBusiness();
} finally {
    // 防止线程复用导致数据污染
    USER_ID_LOCAL.remove();
}
```

这种方式可以减少参数透传，使代码调用链更简洁。但它也会让部分数据来源变得隐式，因此应控制 ThreadLocal 的使用范围，只保存明确的线程上下文数据，避免滥用。

### 与普通局部变量的区别

普通局部变量定义在方法内部，生命周期通常随着方法执行结束而结束。ThreadLocal 中的数据绑定到当前线程，只要没有主动清理，并且线程仍然存在，同一个线程中的多个方法都可以获取到该数据。

普通局部变量强调“方法作用域”，ThreadLocal 强调“线程作用域”。

| 对比项     | 普通局部变量           | ThreadLocal                            |
| ---------- | ---------------------- | -------------------------------------- |
| 作用范围   | 当前方法内部           | 当前线程内部                           |
| 数据归属   | 属于当前方法调用       | 属于当前线程                           |
| 生命周期   | 方法执行结束后通常结束 | 跟随线程存在，直到 remove 或线程销毁   |
| 跨方法使用 | 需要通过参数传递       | 同一线程内可直接获取                   |
| 线程隔离   | 每次方法调用天然隔离   | 不同线程持有不同数据副本               |
| 使用复杂度 | 简单直接               | 需要注意设置、获取和清理               |
| 主要风险   | 风险较低               | 线程池环境下可能出现数据污染或内存泄漏 |

普通局部变量示例：

```java
public void handleRequest() {
    String userId = "10001";
    queryUserOrder(userId);
}

private void queryUserOrder(String userId) {
    System.out.println(userId);
}
```

在这个示例中，`userId` 是普通局部变量。如果后续多个方法都需要使用用户 ID，就必须通过方法参数逐层传递。

ThreadLocal 示例：

```java
private static final ThreadLocal<String> USER_ID_LOCAL = new ThreadLocal<>();

public void handleRequest() {
    try {
        USER_ID_LOCAL.set("10001");
        queryUserOrder();
    } finally {
        USER_ID_LOCAL.remove();
    }
}

private void queryUserOrder() {
    String userId = USER_ID_LOCAL.get();
    System.out.println(userId);
}
```

在这个示例中，`queryUserOrder()` 不需要显式接收 `userId` 参数，而是直接从当前线程的 ThreadLocal 中获取数据。这样可以减少参数传递，但也会增加代码对线程上下文的依赖。

因此，普通局部变量适合方法内部临时数据，ThreadLocal 适合请求级、线程级上下文数据。实际开发中应优先使用普通参数传递，只有当上下文数据需要在较长调用链中共享，并且明确限定在当前线程内使用时，才考虑使用 ThreadLocal。



## ThreadLocal 核心原理

本节用于说明 ThreadLocal 的底层存储方式、数据隔离机制以及弱引用设计。理解这些内容后，可以更准确地判断 ThreadLocal 为什么能够做到线程隔离，以及为什么在使用完成后必须调用 `remove()`。

### ThreadLocal 与 Thread 的关系

ThreadLocal 表面上是一个变量工具类，但它并不直接保存业务数据。真正保存数据的是当前线程对象 `Thread` 内部的 `ThreadLocalMap`。

每个线程内部都有自己的 ThreadLocalMap，ThreadLocal 只是作为访问当前线程本地变量的入口。调用 `set()` 方法时，数据会被保存到当前线程的 ThreadLocalMap 中；调用 `get()` 方法时，会从当前线程的 ThreadLocalMap 中取出当前 ThreadLocal 对应的数据。

核心关系可以理解为：

```text
Thread
 └── ThreadLocalMap
      ├── Entry(ThreadLocal A -> Value A)
      ├── Entry(ThreadLocal B -> Value B)
      └── Entry(ThreadLocal C -> Value C)
```

也就是说，数据不是存储在 ThreadLocal 对象中，而是存储在当前线程对象中。不同线程即使使用同一个 ThreadLocal 对象，也会访问各自线程内部的 ThreadLocalMap，因此数据天然隔离。

简单示例如下：

```java
private static final ThreadLocal<String> USER_LOCAL = new ThreadLocal<>();

public void test() {
    USER_LOCAL.set("10001");
    String userId = USER_LOCAL.get();
    USER_LOCAL.remove();
}
```

这段代码的实际存储逻辑可以理解为：

```text
当前线程 Thread.currentThread()
    -> threadLocals
        -> key = USER_LOCAL
        -> value = "10001"
```

因此，ThreadLocal 的核心并不是“一个变量保存多个线程的数据”，而是“每个线程自己保存一份与 ThreadLocal 关联的数据”。

### ThreadLocalMap 存储结构

ThreadLocalMap 是 ThreadLocal 的内部静态类，用于保存当前线程中的线程本地变量。它的结构类似 Map，但它不是常规的 `HashMap`，而是 ThreadLocal 专门实现的一套轻量级哈希表结构。

ThreadLocalMap 的核心特点如下：

| 特点       | 说明                                             |
| ---------- | ------------------------------------------------ |
| 所属位置   | 存储在 Thread 对象内部                           |
| Key        | ThreadLocal 对象                                 |
| Value      | 业务数据对象                                     |
| Entry 结构 | Entry 继承 WeakReference，Key 是弱引用           |
| 冲突处理   | 使用开放寻址法处理哈希冲突                       |
| 清理机制   | 在 set、get、remove 等操作中会尝试清理失效 Entry |

ThreadLocalMap 中的 Entry 可以简化理解为：

```java
static class Entry extends WeakReference<ThreadLocal<?>> {
    Object value;

    Entry(ThreadLocal<?> key, Object value) {
        super(key);
        this.value = value;
    }
}
```

这表示 ThreadLocalMap 中的 Key 是 ThreadLocal 对象的弱引用，Value 是业务数据的强引用。

当执行以下代码时：

```java
private static final ThreadLocal<String> USER_LOCAL = new ThreadLocal<>();

USER_LOCAL.set("10001");
```

当前线程内部大致会形成如下结构：

```text
Thread.currentThread()
 └── ThreadLocalMap
      └── Entry
           ├── key   -> USER_LOCAL
           └── value -> "10001"
```

如果同一个线程中使用多个 ThreadLocal，则当前线程的 ThreadLocalMap 中会有多个 Entry。

```java
private static final ThreadLocal<String> USER_LOCAL = new ThreadLocal<>();
private static final ThreadLocal<String> TRACE_LOCAL = new ThreadLocal<>();

USER_LOCAL.set("10001");
TRACE_LOCAL.set("trace-001");
```

对应结构如下：

```text
Thread.currentThread()
 └── ThreadLocalMap
      ├── Entry(USER_LOCAL  -> "10001")
      └── Entry(TRACE_LOCAL -> "trace-001")
```

需要注意的是，ThreadLocalMap 是当前线程私有的，不同线程之间不会共享同一个 ThreadLocalMap。这也是 ThreadLocal 能够实现线程隔离的基础。

### 弱引用 Key 与强引用 Value

ThreadLocalMap 中的 Entry 对 Key 使用弱引用，对 Value 使用强引用。这个设计是理解 ThreadLocal 内存泄漏风险的关键。

弱引用的特点是：当一个对象只被弱引用关联时，下一次 GC 发生时该对象可能会被回收。ThreadLocalMap 的 Key 是弱引用，因此如果外部不再强引用某个 ThreadLocal 对象，那么这个 ThreadLocal Key 可能会被 GC 回收。

但是 Entry 中的 Value 是强引用。即使 Key 被回收，Value 仍然可能被当前线程的 ThreadLocalMap 持有。

可能出现如下结构：

```text
Thread.currentThread()
 └── ThreadLocalMap
      └── Entry
           ├── key   -> null
           └── value -> UserContext对象
```

此时 Key 已经失效，但 Value 仍然存在。如果当前线程长期存活，例如线程池中的工作线程，那么这个 Value 就可能长期无法释放，形成内存泄漏风险。

典型风险代码如下：

```java
public void handleRequest() {
    ThreadLocal<UserContext> userContextLocal = new ThreadLocal<>();
    userContextLocal.set(new UserContext("10001", "admin"));

    // 方法结束后 userContextLocal 没有外部强引用，Key 可能被 GC 回收
    // 但 Value 仍可能残留在线程的 ThreadLocalMap 中
}
```

这类代码的问题在于：ThreadLocal 对象是局部变量，方法执行结束后就失去了强引用，但 Value 仍可能残留在当前线程中。

更推荐的写法是将 ThreadLocal 定义为静态常量，并在使用完成后主动清理：

```java
private static final ThreadLocal<UserContext> USER_CONTEXT_LOCAL = new ThreadLocal<>();

public void handleRequest() {
    try {
        USER_CONTEXT_LOCAL.set(new UserContext("10001", "admin"));
        // 执行业务逻辑
    } finally {
        USER_CONTEXT_LOCAL.remove();
    }
}
```

其中最重要的是 `remove()`。它会删除当前线程中当前 ThreadLocal 对应的 Entry，避免线程复用时出现数据污染或内存泄漏。

### 数据隔离机制

ThreadLocal 的数据隔离并不是通过锁实现的，也不是通过线程同步机制实现的，而是通过“每个线程持有自己的 ThreadLocalMap”实现的。

当多个线程访问同一个 ThreadLocal 对象时，实际访问路径如下：

```text
线程 A -> ThreadA.threadLocals -> Entry(ThreadLocal -> ValueA)
线程 B -> ThreadB.threadLocals -> Entry(ThreadLocal -> ValueB)
线程 C -> ThreadC.threadLocals -> Entry(ThreadLocal -> ValueC)
```

虽然 Key 是同一个 ThreadLocal 对象，但 Value 存储在不同线程自己的 ThreadLocalMap 中，因此线程之间互不影响。

示例代码如下：

```java
private static final ThreadLocal<String> USER_LOCAL = new ThreadLocal<>();

public void testThreadIsolation() {
    Thread threadA = new Thread(() -> {
        try {
            USER_LOCAL.set("user-A");
            System.out.println(Thread.currentThread().getName() + "：" + USER_LOCAL.get());
        } finally {
            USER_LOCAL.remove();
        }
    }, "thread-A");

    Thread threadB = new Thread(() -> {
        try {
            USER_LOCAL.set("user-B");
            System.out.println(Thread.currentThread().getName() + "：" + USER_LOCAL.get());
        } finally {
            USER_LOCAL.remove();
        }
    }, "thread-B");

    threadA.start();
    threadB.start();
}
```

可能输出如下：

```text
thread-A：user-A
thread-B：user-B
```

从结果可以看出，两个线程使用的是同一个 ThreadLocal 变量，但读取到的是各自线程中保存的数据。

ThreadLocal 的隔离机制适合处理线程内部上下文，但不适合做线程间通信。如果一个线程中设置了 ThreadLocal 数据，另一个线程默认无法读取该数据。涉及异步任务、线程池、CompletableFuture、消息消费等场景时，需要额外处理上下文传递问题。

## Java 项目中的常见应用

本节用于说明 ThreadLocal 在 Java 项目中的典型落地方式。实际开发中，ThreadLocal 通常不会直接暴露在业务代码中，而是封装为上下文工具类，由过滤器、拦截器、AOP 或框架组件统一设置和清理。

### 用户上下文传递

用户上下文传递是 ThreadLocal 最常见的应用场景。系统在请求入口解析登录凭证后，将当前用户信息保存到 ThreadLocal 中，后续业务代码可以直接获取当前登录用户，避免在 Controller、Service、Mapper 之间层层传递用户参数。

常见的用户上下文字段包括：

| 字段       | 说明             |
| ---------- | ---------------- |
| `userId`   | 当前登录用户 ID  |
| `username` | 当前登录用户名   |
| `tenantId` | 当前租户 ID      |
| `roleCode` | 当前用户角色编码 |
| `clientIp` | 当前客户端 IP    |

用户上下文一般会封装成独立对象，然后通过工具类统一管理。

文件位置：`src/main/java/io/github/atengk/common/context/UserContext.java`

下面的代码定义用户上下文对象，用于保存当前请求中的用户身份信息。

```java
package io.github.atengk.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户上下文信息
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 客户端IP
     */
    private String clientIp;
}
```

文件位置：`src/main/java/io/github/atengk/common/context/UserContextHolder.java`

下面的代码封装用户上下文的设置、获取和清理方法，业务代码只需要通过该工具类访问当前用户信息。

```java
package io.github.atengk.common.context;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文持有器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> USER_CONTEXT_LOCAL = new ThreadLocal<>();

    private UserContextHolder() {
    }

    /**
     * 设置当前线程用户上下文
     *
     * @param userContext 用户上下文
     */
    public static void set(UserContext userContext) {
        Assert.notNull(userContext, "用户上下文不能为空");
        USER_CONTEXT_LOCAL.set(userContext);
        log.debug("设置用户上下文，userId={}，tenantId={}", userContext.getUserId(), userContext.getTenantId());
    }

    /**
     * 获取当前线程用户上下文
     *
     * @return 用户上下文
     */
    public static UserContext get() {
        return USER_CONTEXT_LOCAL.get();
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        UserContext userContext = get();
        return userContext == null ? null : userContext.getUserId();
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID
     */
    public static Long getTenantId() {
        UserContext userContext = get();
        return userContext == null ? null : userContext.getTenantId();
    }

    /**
     * 清理当前线程用户上下文
     */
    public static void remove() {
        USER_CONTEXT_LOCAL.remove();
        log.debug("清理用户上下文");
    }
}
```

典型使用方式如下：

```java
try {
    UserContextHolder.set(new UserContext(10001L, "admin", 1L, "ADMIN", "127.0.0.1"));

    Long userId = UserContextHolder.getUserId();
    Long tenantId = UserContextHolder.getTenantId();

    // 执行业务逻辑
} finally {
    UserContextHolder.remove();
}
```

在 Spring Boot Web 项目中，通常在过滤器或拦截器中设置用户上下文，并在请求结束时清理。这样 Controller 和 Service 中就可以直接读取当前用户信息。

### 请求链路 TraceId 传递

TraceId 用于标识一次请求链路。一次请求从进入系统开始，到 Controller、Service、DAO、远程调用、异常日志输出，应该尽量使用同一个 TraceId，便于后续通过日志快速定位完整调用链路。

ThreadLocal 可以保存当前请求的 TraceId，MDC 可以将 TraceId 输出到日志中。两者经常配合使用。

文件位置：`src/main/java/io/github/atengk/common/context/TraceContextHolder.java`

下面的代码封装 TraceId 的生成、设置、获取和清理逻辑，并同步写入 MDC 日志上下文。

```java
package io.github.atengk.common.context;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * 请求链路上下文持有器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public final class TraceContextHolder {

    private static final String TRACE_ID_KEY = "traceId";

    private static final ThreadLocal<String> TRACE_ID_LOCAL = new ThreadLocal<>();

    private TraceContextHolder() {
    }

    /**
     * 设置TraceId
     *
     * @param traceId 链路追踪ID
     */
    public static void setTraceId(String traceId) {
        String currentTraceId = StrUtil.blankToDefault(traceId, IdUtil.fastSimpleUUID());
        TRACE_ID_LOCAL.set(currentTraceId);
        MDC.put(TRACE_ID_KEY, currentTraceId);
        log.debug("设置请求链路TraceId：{}", currentTraceId);
    }

    /**
     * 获取TraceId
     *
     * @return 链路追踪ID
     */
    public static String getTraceId() {
        return TRACE_ID_LOCAL.get();
    }

    /**
     * 获取TraceId，不存在时自动创建
     *
     * @return 链路追踪ID
     */
    public static String getOrCreateTraceId() {
        String traceId = TRACE_ID_LOCAL.get();
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
            setTraceId(traceId);
        }
        return traceId;
    }

    /**
     * 清理TraceId
     */
    public static void remove() {
        TRACE_ID_LOCAL.remove();
        MDC.remove(TRACE_ID_KEY);
        log.debug("清理请求链路TraceId");
    }
}
```

典型使用方式如下：

```java
try {
    TraceContextHolder.setTraceId(request.getHeader("X-Trace-Id"));

    String traceId = TraceContextHolder.getOrCreateTraceId();

    // 业务日志、异常日志、远程调用请求头中可以继续使用 traceId
} finally {
    TraceContextHolder.remove();
}
```

日志配置中可以将 MDC 中的 `traceId` 输出到日志格式中，例如 Logback 配置：

```xml
<!-- 输出日志时打印 traceId，便于按请求链路检索日志 -->
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId}] %logger{36} - %msg%n</pattern>
```

这样每条日志中都会带上当前请求的 TraceId，便于线上问题排查。

### 数据源切换上下文

在多数据源项目中，ThreadLocal 常用于保存当前线程需要使用的数据源标识。例如系统中同时存在主库、从库、租户库、报表库，不同业务方法需要切换到不同的数据源。

数据源切换通常与 AOP、动态数据源、注解配合使用。AOP 在方法执行前设置数据源标识，在方法执行完成后清理数据源标识，动态数据源组件再从 ThreadLocal 中读取当前数据源 Key。

文件位置：`src/main/java/io/github/atengk/common/context/DataSourceContextHolder.java`

下面的代码封装当前线程的数据源标识，用于动态数据源路由。

```java
package io.github.atengk.common.context;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据源上下文持有器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public final class DataSourceContextHolder {

    private static final String DEFAULT_DATASOURCE = "master";

    private static final ThreadLocal<String> DATASOURCE_LOCAL = new ThreadLocal<>();

    private DataSourceContextHolder() {
    }

    /**
     * 设置当前线程数据源
     *
     * @param dataSourceKey 数据源标识
     */
    public static void set(String dataSourceKey) {
        String currentDataSource = StrUtil.blankToDefault(dataSourceKey, DEFAULT_DATASOURCE);
        DATASOURCE_LOCAL.set(currentDataSource);
        log.debug("切换数据源：{}", currentDataSource);
    }

    /**
     * 获取当前线程数据源
     *
     * @return 数据源标识
     */
    public static String get() {
        return StrUtil.blankToDefault(DATASOURCE_LOCAL.get(), DEFAULT_DATASOURCE);
    }

    /**
     * 清理当前线程数据源
     */
    public static void remove() {
        DATASOURCE_LOCAL.remove();
        log.debug("清理数据源上下文");
    }
}
```

典型使用方式如下：

```java
try {
    DataSourceContextHolder.set("slave");

    // 执行查询逻辑，动态数据源根据当前线程的数据源标识路由到 slave
    queryReportData();
} finally {
    DataSourceContextHolder.remove();
}
```

动态数据源路由中通常会读取当前线程的数据源标识：

```java
@Override
protected Object determineCurrentLookupKey() {
    return DataSourceContextHolder.get();
}
```

这种方式的关键点是：数据源标识必须在方法执行前设置，并且必须在方法执行后清理。如果没有清理，线程被复用后，后续请求可能错误地使用上一次请求残留的数据源标识。

### 事务或会话上下文管理

ThreadLocal 也常用于事务、数据库连接、Session 等上下文管理。典型代表是 Spring 的事务管理机制。Spring 在事务执行过程中，会将当前线程绑定的数据库连接、事务状态、同步回调等信息保存在线程上下文中，保证同一个线程内的数据库操作可以复用同一个事务资源。

在业务开发中，一般不建议自己用 ThreadLocal 手写复杂事务管理，而是优先使用 Spring 的 `@Transactional`。但理解 ThreadLocal 在事务上下文中的作用，有助于理解为什么事务通常只在当前线程内生效。

事务上下文的简化流程如下：

```text
方法进入
 └── 开启事务
      └── 将数据库连接绑定到当前线程
           └── 执行业务SQL
                └── 从当前线程获取同一个数据库连接
                     └── 提交或回滚事务
                          └── 清理当前线程事务资源
```

如果在事务方法中开启新线程，默认情况下新线程无法获取原线程中的事务上下文。

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder() {
    // 当前线程存在事务上下文
    saveOrder();

    new Thread(() -> {
        // 新线程不会自动继承原线程事务上下文
        saveOrderLog();
    }).start();
}
```

上面的 `saveOrderLog()` 默认不会加入外层 `createOrder()` 的事务，因为它运行在另一个线程中。ThreadLocal 是线程隔离的，事务上下文不会自动跨线程传递。

如果只是保存简单的业务会话信息，可以使用 ThreadLocal 封装会话上下文。但如果涉及事务、连接、Session 生命周期，应尽量交给 Spring 或持久层框架管理。

文件位置：`src/main/java/io/github/atengk/common/context/SessionContextHolder.java`

下面的代码演示如何保存轻量级业务会话信息，例如当前请求来源、操作入口和业务流水号。

```java
package io.github.atengk.common.context;

import cn.hutool.core.lang.Assert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 业务会话上下文持有器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public final class SessionContextHolder {

    private static final ThreadLocal<SessionContext> SESSION_CONTEXT_LOCAL = new ThreadLocal<>();

    private SessionContextHolder() {
    }

    /**
     * 设置业务会话上下文
     *
     * @param sessionContext 业务会话上下文
     */
    public static void set(SessionContext sessionContext) {
        Assert.notNull(sessionContext, "业务会话上下文不能为空");
        SESSION_CONTEXT_LOCAL.set(sessionContext);
        log.debug("设置业务会话上下文，bizNo={}", sessionContext.getBizNo());
    }

    /**
     * 获取业务会话上下文
     *
     * @return 业务会话上下文
     */
    public static SessionContext get() {
        return SESSION_CONTEXT_LOCAL.get();
    }

    /**
     * 清理业务会话上下文
     */
    public static void remove() {
        SESSION_CONTEXT_LOCAL.remove();
        log.debug("清理业务会话上下文");
    }

    /**
     * 业务会话上下文信息
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionContext {

        /**
         * 业务流水号
         */
        private String bizNo;

        /**
         * 请求来源
         */
        private String source;

        /**
         * 操作入口
         */
        private String operation;
    }
}
```

典型使用方式如下：

```java
try {
    SessionContextHolder.set(new SessionContextHolder.SessionContext(
            "ORDER202605130001",
            "web",
            "createOrder"
    ));

    // 执行业务逻辑
    SessionContextHolder.SessionContext sessionContext = SessionContextHolder.get();
} finally {
    SessionContextHolder.remove();
}
```

事务或会话上下文使用 ThreadLocal 时，需要遵守两个原则：

| 原则         | 说明                                                       |
| ------------ | ---------------------------------------------------------- |
| 生命周期明确 | 在入口设置，在出口清理，避免线程复用导致上下文残留         |
| 职责边界清晰 | 轻量级业务上下文可以自定义，事务和连接资源优先交给框架管理 |

实际项目中，用户上下文、TraceId、数据源标识这类轻量上下文适合自己封装 ThreadLocal；数据库连接、事务状态、MyBatis SqlSession 等资源上下文则应优先由 Spring、MyBatis 等框架统一管理。



## ThreadLocal 使用方式

本节用于说明 ThreadLocal 在 Java 项目中的基础使用方式，包括变量定义、数据设置、数据获取和数据清理。实际开发中不建议在业务代码中零散创建 ThreadLocal，推荐统一封装为上下文工具类。

### 定义 ThreadLocal 变量

ThreadLocal 变量通常定义为 `private static final`，这样可以保证同一个上下文入口在项目中全局复用，避免在方法内部反复创建 ThreadLocal 对象。

定义 ThreadLocal 时，常见方式有两种：一种是普通定义，默认值为 `null`；另一种是通过 `withInitial` 指定默认值。

普通定义方式如下：

```java
private static final ThreadLocal<String> USER_ID_LOCAL = new ThreadLocal<>();
```

带默认值的定义方式如下：

```java
private static final ThreadLocal<String> TRACE_ID_LOCAL = ThreadLocal.withInitial(() -> "default-trace-id");
```

在实际项目中，更推荐将业务上下文封装成对象，而不是为每个字段单独定义一个 ThreadLocal。这样可以减少 ThreadLocal 数量，也便于统一管理和清理。

文件位置：`src/main/java/io/github/atengk/common/context/RequestContext.java`

下面的代码定义请求上下文对象，用于保存当前请求中的用户、租户和链路追踪信息。

```java
package io.github.atengk.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求上下文信息
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 请求链路ID
     */
    private String traceId;

    /**
     * 客户端IP
     */
    private String clientIp;
}
```

文件位置：`src/main/java/io/github/atengk/common/context/RequestContextHolder.java`

下面的代码封装 ThreadLocal 变量，统一提供请求上下文的设置、读取和清理方法。

```java
package io.github.atengk.common.context;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;

/**
 * 请求上下文持有器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public final class RequestContextHolder {

    private static final ThreadLocal<RequestContext> REQUEST_CONTEXT_LOCAL = new ThreadLocal<>();

    private RequestContextHolder() {
    }

    /**
     * 设置当前线程请求上下文
     *
     * @param requestContext 请求上下文
     */
    public static void set(RequestContext requestContext) {
        Assert.notNull(requestContext, "请求上下文不能为空");
        REQUEST_CONTEXT_LOCAL.set(requestContext);
        log.debug("设置请求上下文，userId={}，tenantId={}，traceId={}",
                requestContext.getUserId(),
                requestContext.getTenantId(),
                requestContext.getTraceId());
    }

    /**
     * 获取当前线程请求上下文
     *
     * @return 请求上下文
     */
    public static RequestContext get() {
        return REQUEST_CONTEXT_LOCAL.get();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        RequestContext requestContext = get();
        return requestContext == null ? null : requestContext.getUserId();
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名
     */
    public static String getUsername() {
        RequestContext requestContext = get();
        return requestContext == null ? null : requestContext.getUsername();
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID
     */
    public static Long getTenantId() {
        RequestContext requestContext = get();
        return requestContext == null ? null : requestContext.getTenantId();
    }

    /**
     * 获取当前TraceId
     *
     * @return TraceId
     */
    public static String getTraceId() {
        RequestContext requestContext = get();
        return requestContext == null ? null : requestContext.getTraceId();
    }

    /**
     * 清理当前线程请求上下文
     */
    public static void remove() {
        REQUEST_CONTEXT_LOCAL.remove();
        log.debug("清理请求上下文");
    }
}
```

这种封装方式有两个优点：第一，业务代码不需要直接操作 ThreadLocal；第二，后续如果要增加字段、增加校验或增加日志，只需要调整 Holder 类。

### 设置线程本地变量

设置线程本地变量使用 `set()` 方法。调用 `set()` 后，数据会写入当前线程内部的 ThreadLocalMap 中，只对当前线程可见。

基础写法如下：

```java
USER_ID_LOCAL.set("10001");
```

在项目中更推荐通过上下文工具类设置：

```java
RequestContext requestContext = new RequestContext();
requestContext.setUserId(10001L);
requestContext.setUsername("admin");
requestContext.setTenantId(1L);
requestContext.setTraceId("trace-001");
requestContext.setClientIp("127.0.0.1");

RequestContextHolder.set(requestContext);
```

在 Web 项目中，设置 ThreadLocal 的位置通常是请求入口，例如：

| 设置位置           | 适用场景                                        |
| ------------------ | ----------------------------------------------- |
| Filter             | 最靠近 Servlet 容器入口，适合处理全局请求上下文 |
| HandlerInterceptor | 适合 Spring MVC 请求上下文处理                  |
| AOP                | 适合方法级上下文处理，例如数据源切换、审计信息  |
| 消息监听器         | 适合 MQ 消费时设置消息处理上下文                |
| 定时任务入口       | 适合任务执行时设置任务上下文                    |

设置线程本地变量时要注意，ThreadLocal 保存的是当前线程数据。如果后续业务逻辑切换到了其他线程，例如异步任务、线程池、`CompletableFuture`，默认无法直接读取原线程中的 ThreadLocal 数据。

### 获取线程本地变量

获取线程本地变量使用 `get()` 方法。调用 `get()` 时，会从当前线程的 ThreadLocalMap 中查找当前 ThreadLocal 对应的数据。

基础写法如下：

```java
String userId = USER_ID_LOCAL.get();
```

通过上下文工具类获取更清晰：

```java
Long userId = RequestContextHolder.getUserId();
Long tenantId = RequestContextHolder.getTenantId();
String traceId = RequestContextHolder.getTraceId();
```

业务代码中读取 ThreadLocal 时，应当处理上下文不存在的情况。比如某些接口不需要登录，或者某些代码在非 Web 请求线程中执行，此时 ThreadLocal 可能为空。

推荐写法如下：

```java
RequestContext requestContext = RequestContextHolder.get();
if (requestContext == null) {
    log.warn("当前线程未获取到请求上下文");
    return;
}

Long userId = requestContext.getUserId();
```

不推荐在业务代码中直接假设 ThreadLocal 一定存在，否则容易出现空指针异常。

```java
// 不推荐：上下文为空时可能出现 NullPointerException
Long userId = RequestContextHolder.get().getUserId();
```

如果某些业务必须依赖用户上下文，可以在 Holder 中提供强校验方法。

文件位置：`src/main/java/io/github/atengk/common/context/RequestContextHolder.java`

下面的方法可追加到 `RequestContextHolder` 中，用于获取必需的请求上下文。

```java
/**
 * 获取必需的请求上下文
 *
 * @return 请求上下文
 */
public static RequestContext requireContext() {
    RequestContext requestContext = get();
    Assert.notNull(requestContext, "当前线程请求上下文不存在");
    return requestContext;
}

/**
 * 获取必需的用户ID
 *
 * @return 用户ID
 */
public static Long requireUserId() {
    Long userId = getUserId();
    Assert.notNull(userId, "当前线程用户ID不存在");
    return userId;
}
```

这样可以把上下文校验集中到工具类中，避免业务代码中重复判断。

### 清理线程本地变量

清理线程本地变量使用 `remove()` 方法。它会删除当前线程中当前 ThreadLocal 对应的数据。

基础写法如下：

```java
USER_ID_LOCAL.remove();
```

通过上下文工具类清理如下：

```java
RequestContextHolder.remove();
```

清理操作必须放在 `finally` 中，保证业务代码正常结束或异常结束时都能执行清理。

推荐写法如下：

```java
try {
    RequestContextHolder.set(requestContext);

    // 执行业务逻辑
    handleBusiness();
} finally {
    RequestContextHolder.remove();
}
```

在普通线程中，线程执行结束后 ThreadLocalMap 会随着线程对象一起释放。但在 Web 容器、线程池、异步任务中，线程通常会被复用。如果没有执行 `remove()`，上一次请求或任务的数据可能残留到下一次请求或任务中。

典型问题如下：

```text
请求 A 使用线程 thread-1，设置 userId = 10001，但没有 remove
请求 A 执行完成，thread-1 回到线程池
请求 B 复用 thread-1，如果未重新设置 userId，可能读取到 userId = 10001
```

因此，ThreadLocal 使用原则可以概括为：

| 操作 | 要求                                                         |
| ---- | ------------------------------------------------------------ |
| 定义 | 使用 `private static final` 定义，并封装到 Holder 类中       |
| 设置 | 在请求、任务、消息消费等入口处设置                           |
| 获取 | 在同一线程的业务链路中获取                                   |
| 清理 | 在 `finally`、拦截器 `afterCompletion` 或过滤器 finally 中清理 |

## Spring Boot 项目实践

本节通过 Spring Boot Web 项目演示 ThreadLocal 的实际使用方式。示例以请求拦截器为入口，从请求头中解析用户信息和 TraceId，写入 ThreadLocal 和 MDC，并在 Controller、Service 中读取上下文，最后在请求结束后统一清理。

示例基于以下约定：

| 请求头        | 说明                                          |
| ------------- | --------------------------------------------- |
| `X-User-Id`   | 当前用户 ID                                   |
| `X-Username`  | 当前用户名                                    |
| `X-Tenant-Id` | 当前租户 ID                                   |
| `X-Trace-Id`  | 请求链路 ID，可由调用方传入，也可后端自动生成 |

### 请求拦截器中设置上下文

请求拦截器适合在 Controller 执行前设置上下文。`preHandle()` 方法中可以解析请求头、Token 或网关透传信息，然后将当前请求上下文写入 ThreadLocal。

文件位置：`src/main/java/io/github/atengk/common/web/RequestContextInterceptor.java`

下面的代码在 Spring MVC 拦截器中解析请求头，设置 RequestContext，并将 TraceId 写入 MDC。

```java
package io.github.atengk.common.web;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.context.RequestContext;
import io.github.atengk.common.context.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求上下文拦截器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID_KEY = "traceId";

    private static final String HEADER_USER_ID = "X-User-Id";

    private static final String HEADER_USERNAME = "X-Username";

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";

    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * Controller执行前设置请求上下文
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @return 是否继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = StrUtil.blankToDefault(request.getHeader(HEADER_TRACE_ID), IdUtil.fastSimpleUUID());

        RequestContext requestContext = new RequestContext();
        requestContext.setUserId(Convert.toLong(request.getHeader(HEADER_USER_ID), null));
        requestContext.setUsername(request.getHeader(HEADER_USERNAME));
        requestContext.setTenantId(Convert.toLong(request.getHeader(HEADER_TENANT_ID), null));
        requestContext.setTraceId(traceId);
        requestContext.setClientIp(getClientIp(request));

        RequestContextHolder.set(requestContext);
        MDC.put(TRACE_ID_KEY, traceId);

        log.info("设置请求上下文，uri={}，userId={}，tenantId={}，traceId={}",
                request.getRequestURI(),
                requestContext.getUserId(),
                requestContext.getTenantId(),
                traceId);
        return true;
    }

    /**
     * 请求完成后清理上下文
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @param ex       异常信息
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            if (ex != null) {
                log.warn("请求执行异常，uri={}，message={}", request.getRequestURI(), ex.getMessage());
            }
        } finally {
            RequestContextHolder.remove();
            MDC.remove(TRACE_ID_KEY);
            log.debug("请求完成，已清理请求上下文，uri={}", request.getRequestURI());
        }
    }

    /**
     * 获取客户端IP
     *
     * @param request HTTP请求
     * @return 客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(xForwardedFor)) {
            return StrUtil.subBefore(xForwardedFor, ",", false);
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }

        return request.getRemoteAddr();
    }
}
```

文件位置：`src/main/java/io/github/atengk/common/config/WebMvcConfig.java`

下面的代码将请求上下文拦截器注册到 Spring MVC 中，使其对接口请求生效。

```java
package io.github.atengk.common.config;

import io.github.atengk.common.web.RequestContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * Web MVC配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestContextInterceptor requestContextInterceptor;

    /**
     * 注册拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/error",
                        "/favicon.ico"
                );
    }
}
```

拦截器注册后，请求进入 Controller 前会自动设置 ThreadLocal 上下文。请求结束后，无论接口正常返回还是抛出异常，`afterCompletion()` 都会执行清理逻辑。

### Controller 与 Service 中读取上下文

Controller 和 Service 中不需要再从请求头或 Token 中重复解析用户信息，可以直接通过 `RequestContextHolder` 获取当前线程的上下文数据。

文件位置：`src/main/java/io/github/atengk/module/demo/controller/DemoController.java`

下面的代码演示 Controller 中读取当前请求上下文，并调用 Service 完成业务处理。

```java
package io.github.atengk.module.demo.controller;

import io.github.atengk.common.context.RequestContext;
import io.github.atengk.common.context.RequestContextHolder;
import io.github.atengk.module.demo.service.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ThreadLocal示例接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
public class DemoController {

    private final DemoService demoService;

    /**
     * 获取当前请求上下文
     *
     * @return 当前请求上下文
     */
    @GetMapping("/demo/context")
    public Map<String, Object> getContext() {
        RequestContext requestContext = RequestContextHolder.requireContext();
        String serviceResult = demoService.getCurrentUserMessage();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", requestContext.getUserId());
        result.put("username", requestContext.getUsername());
        result.put("tenantId", requestContext.getTenantId());
        result.put("traceId", requestContext.getTraceId());
        result.put("clientIp", requestContext.getClientIp());
        result.put("serviceResult", serviceResult);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/module/demo/service/DemoService.java`

下面的代码定义 Service 接口。

```java
package io.github.atengk.module.demo.service;

/**
 * ThreadLocal示例服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface DemoService {

    /**
     * 获取当前用户提示信息
     *
     * @return 当前用户提示信息
     */
    String getCurrentUserMessage();
}
```

文件位置：`src/main/java/io/github/atengk/module/demo/service/impl/DemoServiceImpl.java`

下面的代码演示 Service 中读取 ThreadLocal 上下文，并输出业务日志。

```java
package io.github.atengk.module.demo.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.context.RequestContextHolder;
import io.github.atengk.module.demo.service.DemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ThreadLocal示例服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class DemoServiceImpl implements DemoService {

    /**
     * 获取当前用户提示信息
     *
     * @return 当前用户提示信息
     */
    @Override
    public String getCurrentUserMessage() {
        Long userId = RequestContextHolder.getUserId();
        Long tenantId = RequestContextHolder.getTenantId();
        String traceId = RequestContextHolder.getTraceId();

        log.info("Service读取请求上下文，userId={}，tenantId={}，traceId={}", userId, tenantId, traceId);

        if (userId == null) {
            return "当前请求未携带用户信息";
        }

        return StrUtil.format("当前用户ID：{}，当前租户ID：{}，当前TraceId：{}", userId, tenantId, traceId);
    }
}
```

接口调用示例如下：

```bash
curl -X GET "http://localhost:8080/demo/context" \
  -H "X-User-Id: 10001" \
  -H "X-Username: admin" \
  -H "X-Tenant-Id: 1" \
  -H "X-Trace-Id: trace-demo-001"
```

响应示例：

```json
{
  "userId": 10001,
  "username": "admin",
  "tenantId": 1,
  "traceId": "trace-demo-001",
  "clientIp": "127.0.0.1",
  "serviceResult": "当前用户ID：10001，当前租户ID：1，当前TraceId：trace-demo-001"
}
```

这个示例可以验证：请求头中的上下文信息在拦截器中写入 ThreadLocal 后，Controller 和 Service 可以在同一个请求线程中读取到相同的数据。

### 请求结束后清理上下文

ThreadLocal 的清理是项目实践中最重要的一步。由于 Spring Boot Web 项目通常运行在 Tomcat、Jetty 或 Undertow 这类线程池模型中，请求处理线程会被反复复用。如果请求结束后不清理 ThreadLocal，可能导致后续请求读取到上一次请求残留的数据。

在拦截器中，推荐使用 `afterCompletion()` 清理上下文：

```java
@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    try {
        if (ex != null) {
            log.warn("请求执行异常，uri={}，message={}", request.getRequestURI(), ex.getMessage());
        }
    } finally {
        RequestContextHolder.remove();
        MDC.remove("traceId");
        log.debug("请求完成，已清理请求上下文，uri={}", request.getRequestURI());
    }
}
```

如果使用 Filter，也应使用 `try-finally` 清理：

```java
try {
    RequestContextHolder.set(requestContext);
    filterChain.doFilter(request, response);
} finally {
    RequestContextHolder.remove();
    MDC.remove("traceId");
}
```

清理时机可以按场景归纳如下：

| 场景              | 推荐清理位置               |
| ----------------- | -------------------------- |
| Spring MVC 拦截器 | `afterCompletion()`        |
| Servlet Filter    | `finally` 代码块           |
| AOP 切面          | `finally` 代码块           |
| 线程池任务        | `finally` 代码块           |
| MQ 消费监听器     | 消费方法结束前的 `finally` |
| 定时任务          | 任务执行结束前的 `finally` |

需要特别注意的是，`postHandle()` 不适合作为唯一清理位置。因为当 Controller 抛出异常时，`postHandle()` 可能不会执行，而 `afterCompletion()` 在请求完成后会执行，更适合做资源清理。

### 与 MDC 日志上下文结合使用

MDC 是 SLF4J 提供的日志上下文机制，底层通常也是基于 ThreadLocal 实现。它可以将当前线程中的变量输出到日志中，例如 TraceId、UserId、TenantId 等。

ThreadLocal 保存业务上下文，MDC 保存日志输出上下文。两者配合使用时，通常在请求入口同时设置，在请求结束时同时清理。

在拦截器中设置 MDC：

```java
MDC.put("traceId", requestContext.getTraceId());

if (requestContext.getUserId() != null) {
    MDC.put("userId", String.valueOf(requestContext.getUserId()));
}

if (requestContext.getTenantId() != null) {
    MDC.put("tenantId", String.valueOf(requestContext.getTenantId()));
}
```

在请求结束时清理 MDC：

```java
MDC.remove("traceId");
MDC.remove("userId");
MDC.remove("tenantId");
```

也可以使用 `MDC.clear()` 清理当前线程全部 MDC 数据：

```java
MDC.clear();
```

如果当前线程中还存在其他框架写入的 MDC 数据，使用 `MDC.clear()` 可能会误删其他字段。因此在业务项目中，更推荐按 Key 精确移除。

Logback 日志格式可以配置如下：

```xml
<!-- 输出 traceId、userId、tenantId，便于按请求、用户和租户检索日志 -->
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [traceId=%X{traceId}] [userId=%X{userId}] [tenantId=%X{tenantId}] %logger{36} - %msg%n</pattern>
```

日志输出示例：

```text
2026-05-13 10:30:15.123 [http-nio-8080-exec-1] INFO  [traceId=trace-demo-001] [userId=10001] [tenantId=1] i.g.a.m.demo.service.impl.DemoServiceImpl - Service读取请求上下文，userId=10001，tenantId=1，traceId=trace-demo-001
```

结合 MDC 后，排查问题时可以直接通过 `traceId` 查询一次请求的完整日志，通过 `userId` 查询某个用户的操作日志，通过 `tenantId` 查询某个租户下的业务日志。

最终推荐的请求上下文处理流程如下：

```text
请求进入
  -> 拦截器 preHandle
      -> 解析用户信息、租户信息、TraceId
      -> 写入 RequestContextHolder
      -> 写入 MDC
  -> Controller 执行
      -> 读取 RequestContextHolder
  -> Service 执行
      -> 读取 RequestContextHolder
      -> 输出带 traceId、userId、tenantId 的日志
  -> 请求结束 afterCompletion
      -> 清理 RequestContextHolder
      -> 清理 MDC
```

在 Spring Boot 项目中，ThreadLocal 和 MDC 的使用边界应保持清晰：ThreadLocal 用于业务代码读取上下文，MDC 用于日志框架输出上下文。两者都依赖当前线程，因此都必须在请求结束后清理。



## 线程池环境下的问题

本节用于说明 ThreadLocal 在线程池环境中的典型风险。普通线程执行完成后会销毁，线程中的 ThreadLocal 数据也会随线程生命周期结束而释放；但线程池中的线程会被反复复用，如果没有正确清理 ThreadLocal，就可能出现数据污染和内存泄漏。

### 线程复用带来的数据污染

线程池为了减少线程创建和销毁成本，会复用已经创建好的工作线程。一次任务执行完成后，线程不会立即销毁，而是回到线程池中等待下一次任务。

如果上一次任务向 ThreadLocal 写入了数据，但没有执行 `remove()`，那么下一次任务复用同一个线程时，就可能读取到上一次任务残留的数据。

典型问题流程如下：

```text
任务 A 使用 thread-1
  -> 设置 userId = 10001
  -> 执行业务逻辑
  -> 未清理 ThreadLocal

任务 B 复用 thread-1
  -> 未设置 userId
  -> 读取 ThreadLocal
  -> 错误读取到 userId = 10001
```

下面的代码演示线程池复用导致的数据污染问题。

文件位置：`src/main/java/io/github/atengk/demo/ThreadPoolPollutionDemo.java`

这段代码演示线程池中未清理 ThreadLocal 时，后续任务可能读取到旧数据。

```java
package io.github.atengk.demo;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 线程池数据污染示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class ThreadPoolPollutionDemo {

    private static final ThreadLocal<String> USER_LOCAL = new ThreadLocal<>();

    /**
     * 演示线程池复用导致的ThreadLocal数据污染
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        executorService.execute(() -> {
            USER_LOCAL.set("10001");
            log.info("任务A设置用户ID：{}", USER_LOCAL.get());

            // 故意不清理，模拟错误写法
            // USER_LOCAL.remove();
        });

        ThreadUtil.sleep(1000);

        executorService.execute(() -> {
            String userId = USER_LOCAL.get();
            log.info("任务B读取用户ID：{}", userId);
        });

        executorService.shutdown();
    }
}
```

可能输出如下：

```text
任务A设置用户ID：10001
任务B读取用户ID：10001
```

任务 B 并没有设置用户 ID，但由于线程池只有一个线程，任务 B 复用了任务 A 的线程，因此读取到了任务 A 残留在 ThreadLocal 中的数据。这就是线程池环境下最常见的数据污染问题。

正确写法是：每次任务执行完成后都在 `finally` 中清理 ThreadLocal。

```java
executorService.execute(() -> {
    try {
        USER_LOCAL.set("10001");
        log.info("任务A设置用户ID：{}", USER_LOCAL.get());
    } finally {
        USER_LOCAL.remove();
        log.info("任务A清理用户ID");
    }
});
```

只要线程来自线程池，就应默认认为它会被复用，因此必须主动清理当前任务设置的 ThreadLocal 数据。

### 内存泄漏风险

ThreadLocal 的内存泄漏风险主要来自 ThreadLocalMap 的存储结构。ThreadLocalMap 中的 Entry 使用弱引用保存 Key，也就是 ThreadLocal 对象；但 Entry 的 Value 是强引用。

简化结构如下：

```text
Thread
  -> ThreadLocalMap
      -> Entry
          -> key   = ThreadLocal弱引用
          -> value = 业务对象强引用
```

如果 ThreadLocal 对象没有外部强引用，GC 后 Key 可能会被回收，此时 Entry 会变成：

```text
Thread
  -> ThreadLocalMap
      -> Entry
          -> key   = null
          -> value = 业务对象强引用
```

Key 已经为 `null`，但 Value 仍然被当前线程强引用。如果当前线程长期存活，例如线程池中的工作线程，那么 Value 可能长期无法释放，从而造成内存泄漏。

风险代码如下：

```java
public void handle() {
    ThreadLocal<byte[]> local = new ThreadLocal<>();
    local.set(new byte[1024 * 1024 * 10]);

    // 方法结束后 local 变量失效，ThreadLocal Key 可能被GC回收
    // 但10MB数组仍可能被线程的 ThreadLocalMap 持有
}
```

这段代码的问题有两个：

第一，ThreadLocal 被定义为局部变量，方法执行结束后失去强引用，Key 容易被 GC 回收。

第二，Value 是较大的对象，并且没有调用 `remove()` 主动清理，在线程池环境中可能长期残留。

更合理的写法如下：

```java
private static final ThreadLocal<byte[]> BUFFER_LOCAL = new ThreadLocal<>();

public void handle() {
    try {
        BUFFER_LOCAL.set(new byte[1024 * 1024]);
        // 执行业务逻辑
    } finally {
        BUFFER_LOCAL.remove();
    }
}
```

实际项目中不建议在 ThreadLocal 中保存大对象，例如大集合、大数组、文件内容、图片字节流、复杂聚合对象等。ThreadLocal 更适合保存轻量级上下文标识，例如用户 ID、租户 ID、TraceId、数据源 Key。

### remove 方法的重要性

`remove()` 是 ThreadLocal 使用中最关键的方法。它的作用是删除当前线程中当前 ThreadLocal 对应的 Entry。

调用 `remove()` 后，当前线程中该 ThreadLocal 对应的数据会被移除，后续再调用 `get()` 将返回 `null`，或者返回 `withInitial()` 设置的初始值。

基础示例如下：

```java
private static final ThreadLocal<String> USER_LOCAL = new ThreadLocal<>();

public void handle() {
    try {
        USER_LOCAL.set("10001");
        log.info("当前用户ID：{}", USER_LOCAL.get());
    } finally {
        USER_LOCAL.remove();
    }
}
```

`remove()` 至少解决两个问题：

| 问题     | 说明                                                    |
| -------- | ------------------------------------------------------- |
| 数据污染 | 防止线程被复用后，下一个任务读取到上一个任务的数据      |
| 内存泄漏 | 解除当前线程对 Value 的引用，降低对象长期无法释放的风险 |

在以下场景中，必须执行 `remove()`：

| 场景                          | 清理位置                   |
| ----------------------------- | -------------------------- |
| Servlet Filter                | `finally`                  |
| Spring MVC HandlerInterceptor | `afterCompletion()`        |
| AOP 切面                      | `finally`                  |
| 线程池任务                    | `finally`                  |
| MQ 消费者                     | 消费逻辑结束后的 `finally` |
| 定时任务                      | 任务执行结束后的 `finally` |
| 数据源切换                    | 方法执行结束后的 `finally` |

推荐的通用写法如下：

```java
try {
    ContextHolder.set(context);
    handleBusiness();
} finally {
    ContextHolder.remove();
}
```

不要只在正常流程结尾清理，因为业务逻辑可能抛出异常。如果异常发生后没有执行清理，线程池中的线程仍然会携带旧的 ThreadLocal 数据。

## InheritableThreadLocal 使用

本节用于说明 InheritableThreadLocal 的作用、适用场景和限制。InheritableThreadLocal 是 ThreadLocal 的子类，它可以在创建子线程时，将父线程中的变量值传递给子线程。

### 父子线程变量传递

普通 ThreadLocal 只在当前线程内有效，子线程默认无法读取父线程设置的 ThreadLocal 数据。

普通 ThreadLocal 示例：

```java
private static final ThreadLocal<String> TRACE_LOCAL = new ThreadLocal<>();

public void test() {
    TRACE_LOCAL.set("trace-001");

    new Thread(() -> {
        // 普通 ThreadLocal 无法直接获取父线程中的值
        log.info("子线程TraceId：{}", TRACE_LOCAL.get());
    }).start();
}
```

子线程中通常会输出：

```text
子线程TraceId：null
```

如果希望创建子线程时传递父线程上下文，可以使用 `InheritableThreadLocal`。

文件位置：`src/main/java/io/github/atengk/demo/InheritableThreadLocalDemo.java`

这段代码演示父线程设置 TraceId 后，子线程可以读取父线程传递过来的值。

```java
package io.github.atengk.demo;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * InheritableThreadLocal父子线程传递示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class InheritableThreadLocalDemo {

    private static final InheritableThreadLocal<String> TRACE_LOCAL = new InheritableThreadLocal<>();

    /**
     * 演示父子线程上下文传递
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            TRACE_LOCAL.set("trace-001");
            log.info("父线程TraceId：{}", TRACE_LOCAL.get());

            Thread thread = new Thread(() -> {
                try {
                    log.info("子线程TraceId：{}", TRACE_LOCAL.get());
                } finally {
                    TRACE_LOCAL.remove();
                }
            }, "child-thread");

            thread.start();
            ThreadUtil.sleep(1000);
        } finally {
            TRACE_LOCAL.remove();
        }
    }
}
```

可能输出如下：

```text
父线程TraceId：trace-001
子线程TraceId：trace-001
```

InheritableThreadLocal 的传递时机是“创建子线程时”。子线程创建时，会从父线程复制一份 inheritableThreadLocals 数据。之后父线程再修改变量，已经创建好的子线程不会自动同步更新。

### 使用限制

InheritableThreadLocal 只能解决简单的父子线程创建场景，不适合复杂的线程池、异步任务和任务编排场景。

主要限制如下：

| 限制                 | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| 只在创建子线程时传递 | 子线程创建完成后，父线程后续修改不会自动同步                 |
| 默认是引用传递       | 如果 Value 是可变对象，父子线程可能引用同一个对象            |
| 不适合线程池         | 线程池中的线程通常提前创建，不会在每次提交任务时重新继承上下文 |
| 仍然需要清理         | 子线程中使用完成后也应调用 `remove()`                        |
| 容易隐藏依赖         | 子线程依赖父线程上下文，代码可读性和可维护性下降             |

下面的代码演示 InheritableThreadLocal 的传递时机限制。

```java
private static final InheritableThreadLocal<String> TRACE_LOCAL = new InheritableThreadLocal<>();

public void test() {
    TRACE_LOCAL.set("trace-001");

    Thread thread = new Thread(() -> {
        // 子线程创建时继承的是 trace-001
        log.info("子线程TraceId：{}", TRACE_LOCAL.get());
    });

    TRACE_LOCAL.set("trace-002");

    thread.start();
}
```

虽然父线程在 `thread.start()` 前将 TraceId 改成了 `trace-002`，但 InheritableThreadLocal 的继承发生在 `new Thread()` 创建线程时，而不是 `start()` 启动线程时。因此子线程读取到的仍可能是创建线程时的上下文值。

如果 Value 是可变对象，还要注意父子线程之间的数据修改影响。

```java
private static final InheritableThreadLocal<UserContext> USER_LOCAL = new InheritableThreadLocal<>();
```

如果父线程和子线程持有同一个 `UserContext` 对象引用，子线程修改对象字段后，父线程看到的对象内容也可能被改变。因此 InheritableThreadLocal 更适合传递不可变对象或简单标识值，例如字符串 TraceId。

### 在线程池中的注意事项

InheritableThreadLocal 在线程池中经常无法达到预期效果。原因是线程池中的工作线程通常会提前创建，并且会被反复复用。

InheritableThreadLocal 的继承发生在创建线程时，而不是提交任务时。对于线程池来说，业务任务提交时，线程可能早已经存在，因此任务不会自动继承提交任务线程的上下文。

典型问题代码如下：

```java
private static final InheritableThreadLocal<String> TRACE_LOCAL = new InheritableThreadLocal<>();

ExecutorService executorService = Executors.newFixedThreadPool(1);

public void test() {
    TRACE_LOCAL.set("trace-001");

    executorService.execute(() -> {
        log.info("线程池任务TraceId：{}", TRACE_LOCAL.get());
    });

    TRACE_LOCAL.remove();
}
```

这段代码不可靠。线程池中的线程可能在 `TRACE_LOCAL.set("trace-001")` 之前已经创建完成，因此任务中不一定能读取到 `trace-001`。

更严重的问题是：线程池中的线程如果继承或残留了某个上下文，后续任务复用该线程时，可能继续读取到旧数据。

如果确实需要在线程池任务中传递上下文，推荐显式捕获父线程上下文，然后在任务执行时设置到子线程，最后清理。

文件位置：`src/main/java/io/github/atengk/common/context/ContextTaskWrapper.java`

下面的代码用于包装线程池任务，在提交任务时捕获请求上下文，在执行任务时设置上下文，并在执行完成后清理。

```java
package io.github.atengk.common.context;

import lombok.extern.slf4j.Slf4j;

/**
 * 上下文任务包装器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public final class ContextTaskWrapper {

    private ContextTaskWrapper() {
    }

    /**
     * 包装Runnable任务，传递父线程请求上下文
     *
     * @param task 原始任务
     * @return 包装后的任务
     */
    public static Runnable wrap(Runnable task) {
        RequestContext parentContext = RequestContextHolder.get();

        return () -> {
            try {
                if (parentContext != null) {
                    RequestContextHolder.set(parentContext);
                    log.debug("线程池任务设置父线程请求上下文，traceId={}", parentContext.getTraceId());
                }

                task.run();
            } finally {
                RequestContextHolder.remove();
                log.debug("线程池任务清理请求上下文");
            }
        };
    }
}
```

使用示例如下：

```java
executorService.execute(ContextTaskWrapper.wrap(() -> {
    Long userId = RequestContextHolder.getUserId();
    String traceId = RequestContextHolder.getTraceId();

    log.info("线程池任务读取上下文，userId={}，traceId={}", userId, traceId);
}));
```

这种写法比直接依赖 InheritableThreadLocal 更清晰，因为上下文传递发生在“提交任务时”，而不是依赖线程创建时机。

在生产项目中，如果大量使用线程池上下文传递，也可以考虑使用阿里开源的 TransmittableThreadLocal，但仍然要注意上下文清理和任务包装范围。

## 最佳实践

本节用于总结 ThreadLocal 在 Java 项目开发中的推荐使用方式。ThreadLocal 本身不是问题，问题通常出现在使用范围不清、生命周期不清、缺少清理以及在线程池中误用。

### 封装上下文工具类

不建议在业务代码中直接创建和操作 ThreadLocal。推荐将 ThreadLocal 封装到统一的 Holder 工具类中，由工具类提供 `set`、`get`、`remove`、`require` 等方法。

推荐封装结构如下：

```text
common
 └── context
      ├── RequestContext.java
      ├── RequestContextHolder.java
      ├── UserContextHolder.java
      ├── TraceContextHolder.java
      └── DataSourceContextHolder.java
```

封装后的优点如下：

| 优点     | 说明                                      |
| -------- | ----------------------------------------- |
| 统一入口 | 业务代码统一通过 Holder 访问上下文        |
| 便于清理 | 所有 ThreadLocal 都提供明确的 remove 方法 |
| 便于扩展 | 后续增加字段、校验、日志时只改工具类      |
| 降低误用 | 避免业务代码中到处 new ThreadLocal        |
| 便于排查 | 可以在 set/remove 中增加 debug 日志       |

文件位置：`src/main/java/io/github/atengk/common/context/TraceContextHolder.java`

下面的代码演示一个较规范的 TraceId 上下文工具类。

```java
package io.github.atengk.common.context;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * TraceId上下文持有器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public final class TraceContextHolder {

    private static final String TRACE_ID_KEY = "traceId";

    private static final ThreadLocal<String> TRACE_ID_LOCAL = new ThreadLocal<>();

    private TraceContextHolder() {
    }

    /**
     * 设置TraceId
     *
     * @param traceId 链路追踪ID
     */
    public static void set(String traceId) {
        String currentTraceId = StrUtil.blankToDefault(traceId, IdUtil.fastSimpleUUID());
        TRACE_ID_LOCAL.set(currentTraceId);
        MDC.put(TRACE_ID_KEY, currentTraceId);
        log.debug("设置TraceId：{}", currentTraceId);
    }

    /**
     * 获取TraceId
     *
     * @return 链路追踪ID
     */
    public static String get() {
        return TRACE_ID_LOCAL.get();
    }

    /**
     * 获取TraceId，不存在时自动创建
     *
     * @return 链路追踪ID
     */
    public static String getOrCreate() {
        String traceId = get();
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
            set(traceId);
        }
        return traceId;
    }

    /**
     * 清理TraceId
     */
    public static void remove() {
        TRACE_ID_LOCAL.remove();
        MDC.remove(TRACE_ID_KEY);
        log.debug("清理TraceId");
    }
}
```

这个类同时维护 ThreadLocal 和 MDC，适合在请求入口设置 TraceId，在业务日志中读取 TraceId，在请求结束时统一清理。

### 使用 try-finally 清理资源

ThreadLocal 的设置和清理应该成对出现。凡是调用 `set()` 的地方，都应该能明确找到对应的 `remove()`。

最推荐的写法是使用 `try-finally`：

```java
try {
    TraceContextHolder.set("trace-001");
    handleBusiness();
} finally {
    TraceContextHolder.remove();
}
```

如果是多个上下文，也应统一在 `finally` 中清理：

```java
try {
    TraceContextHolder.set("trace-001");
    DataSourceContextHolder.set("slave");
    UserContextHolder.set(userContext);

    handleBusiness();
} finally {
    UserContextHolder.remove();
    DataSourceContextHolder.remove();
    TraceContextHolder.remove();
}
```

清理顺序建议与设置顺序相反。这样更符合资源栈的释放习惯，尤其当某些上下文之间存在依赖关系时，更容易维护。

在 Spring MVC 拦截器中，推荐在 `afterCompletion()` 中清理：

```java
@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    try {
        if (ex != null) {
            log.warn("请求处理异常，uri={}，message={}", request.getRequestURI(), ex.getMessage());
        }
    } finally {
        UserContextHolder.remove();
        TraceContextHolder.remove();
        DataSourceContextHolder.remove();
    }
}
```

在 AOP 中，推荐写法如下：

```java
try {
    DataSourceContextHolder.set(dataSourceKey);
    return joinPoint.proceed();
} finally {
    DataSourceContextHolder.remove();
}
```

不要只在方法正常结束时清理。只要业务代码可能抛异常，就必须使用 `finally`。

### 避免存储大对象

ThreadLocal 适合保存轻量级上下文信息，不适合保存大对象或复杂对象图。在线程池环境中，如果大对象没有及时清理，可能造成明显的内存占用问题。

不推荐保存的数据包括：

| 不推荐内容       | 原因                                     |
| ---------------- | ---------------------------------------- |
| 大集合           | 数据量不可控，容易占用大量内存           |
| 文件字节流       | 对象体积大，生命周期不应绑定线程         |
| 图片、视频内容   | 内存占用高，不适合线程上下文保存         |
| 数据库连接       | 生命周期复杂，应交给连接池或事务框架管理 |
| HTTP 响应对象    | 容易造成引用链过长和资源释放不及时       |
| 复杂业务聚合对象 | 字段多、引用深，容易扩大内存泄漏影响     |

推荐保存的数据包括：

| 推荐内容   | 示例                 |
| ---------- | -------------------- |
| 用户标识   | `userId`、`username` |
| 租户标识   | `tenantId`           |
| 链路标识   | `traceId`            |
| 数据源标识 | `dataSourceKey`      |
| 请求来源   | `source`、`clientIp` |
| 语言标识   | `locale`             |

如果确实需要保存对象，也应尽量保存轻量 DTO，并且控制字段范围。

例如推荐的轻量上下文对象如下：

```java
package io.github.atengk.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 轻量请求上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LightweightRequestContext {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 请求链路ID
     */
    private String traceId;
}
```

这个对象只保存必要字段，不保存完整用户对象、权限集合、菜单树、请求体内容等大对象。

### 控制使用范围

ThreadLocal 应该用于明确的线程级上下文，不应该替代参数传递、缓存、权限系统或全局状态管理。

适合使用 ThreadLocal 的场景：

| 场景                 | 说明                                 |
| -------------------- | ------------------------------------ |
| 请求级上下文         | 当前用户、租户、TraceId、客户端 IP   |
| 数据源路由           | 当前线程需要使用的数据源 Key         |
| 日志增强             | 配合 MDC 输出 TraceId、UserId        |
| 框架级资源绑定       | 事务、连接、Session 等由框架统一管理 |
| 短生命周期任务上下文 | 单次任务执行过程中的轻量上下文       |

不适合使用 ThreadLocal 的场景：

| 场景                 | 原因                                                 |
| -------------------- | ---------------------------------------------------- |
| 跨线程共享数据       | ThreadLocal 天然线程隔离，不能作为线程通信工具       |
| 全局缓存             | 应使用 Redis、Caffeine、ConcurrentHashMap 等缓存方案 |
| 大对象临时存储       | 容易造成内存泄漏和内存占用过高                       |
| 长生命周期业务状态   | ThreadLocal 生命周期依赖线程，不等于业务生命周期     |
| 隐式传递核心业务参数 | 容易降低代码可读性和可测试性                         |

推荐使用原则如下：

```text
能用方法参数清晰表达的，不优先使用 ThreadLocal。
确实属于线程上下文的数据，可以使用 ThreadLocal。
使用 ThreadLocal 后，必须明确设置入口、读取范围和清理出口。
```

实际项目中可以按以下标准判断是否应该使用 ThreadLocal：

| 判断问题                     | 建议                         |
| ---------------------------- | ---------------------------- |
| 数据是否只属于当前线程？     | 是，才考虑使用               |
| 数据是否需要跨多层调用读取？ | 是，可以考虑使用             |
| 数据是否需要跨线程传递？     | 是，不应直接依赖 ThreadLocal |
| 数据是否体积较大？           | 是，不建议使用               |
| 是否能保证 finally 清理？    | 否，不建议使用               |
| 是否会隐藏关键业务参数？     | 是，应优先考虑显式传参       |

最终建议是：ThreadLocal 只用于保存短生命周期、轻量级、线程内可见的上下文数据，并且必须通过统一工具类封装、在入口设置、在出口清理。



## 功能验证

本节用于验证 ThreadLocal 在不同运行场景下的行为，包括单线程读写、多线程数据隔离以及线程池复用时的数据残留问题。验证代码默认项目已引入 Hutool、Lombok 和日志框架。

### 单线程场景验证

单线程场景主要验证 ThreadLocal 在同一个线程中可以正常完成 `set`、`get`、`remove` 操作。该场景可以确认 ThreadLocal 的基础使用流程是否正确。

文件位置：`src/main/java/io/github/atengk/demo/ThreadLocalSingleThreadDemo.java`

下面的代码演示单线程中设置、读取和清理 ThreadLocal 数据。

```java
package io.github.atengk.demo;

import lombok.extern.slf4j.Slf4j;

/**
 * ThreadLocal单线程场景验证
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class ThreadLocalSingleThreadDemo {

    private static final ThreadLocal<String> USER_LOCAL = new ThreadLocal<>();

    /**
     * 单线程验证入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        try {
            USER_LOCAL.set("10001");

            String userId = USER_LOCAL.get();
            log.info("当前线程：{}，读取用户ID：{}", Thread.currentThread().getName(), userId);
        } finally {
            USER_LOCAL.remove();
            log.info("当前线程：{}，已清理ThreadLocal", Thread.currentThread().getName());
        }

        String userIdAfterRemove = USER_LOCAL.get();
        log.info("清理后再次读取用户ID：{}", userIdAfterRemove);
    }
}
```

预期输出如下：

```text
当前线程：main，读取用户ID：10001
当前线程：main，已清理ThreadLocal
清理后再次读取用户ID：null
```

从结果可以看出，同一个线程中调用 `set()` 后可以通过 `get()` 读取到数据；调用 `remove()` 后，当前线程中的数据被清理，再次读取结果为 `null`。

### 多线程隔离验证

多线程场景主要验证不同线程访问同一个 ThreadLocal 对象时，读取到的是各自线程内部保存的数据。ThreadLocal 的线程隔离能力就是通过这种方式体现的。

文件位置：`src/main/java/io/github/atengk/demo/ThreadLocalMultiThreadDemo.java`

下面的代码创建多个线程，每个线程写入不同的用户 ID，并验证线程之间的数据互不影响。

```java
package io.github.atengk.demo;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

/**
 * ThreadLocal多线程隔离验证
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class ThreadLocalMultiThreadDemo {

    private static final ThreadLocal<String> USER_LOCAL = new ThreadLocal<>();

    /**
     * 多线程隔离验证入口
     *
     * @param args 启动参数
     * @throws InterruptedException 等待线程完成时可能抛出的异常
     */
    public static void main(String[] args) throws InterruptedException {
        int threadCount = 3;
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 1; i <= threadCount; i++) {
            int index = i;
            ThreadUtil.execAsync(() -> {
                try {
                    String userId = "user-" + index;
                    USER_LOCAL.set(userId);

                    ThreadUtil.sleep(500);
                    log.info("当前线程：{}，读取用户ID：{}", Thread.currentThread().getName(), USER_LOCAL.get());
                } finally {
                    USER_LOCAL.remove();
                    countDownLatch.countDown();
                    log.info("当前线程：{}，已清理ThreadLocal", Thread.currentThread().getName());
                }
            });
        }

        countDownLatch.await();
        log.info("多线程隔离验证完成");
    }
}
```

预期输出类似如下：

```text
当前线程：pool-1-thread-1，读取用户ID：user-1
当前线程：pool-1-thread-2，读取用户ID：user-2
当前线程：pool-1-thread-3，读取用户ID：user-3
当前线程：pool-1-thread-1，已清理ThreadLocal
当前线程：pool-1-thread-2，已清理ThreadLocal
当前线程：pool-1-thread-3，已清理ThreadLocal
多线程隔离验证完成
```

从结果可以看出，多个线程使用的是同一个 `USER_LOCAL` 对象，但每个线程读取到的值都不同。这说明 ThreadLocal 的数据不是保存在 ThreadLocal 对象本身，而是保存在各个线程自己的 ThreadLocalMap 中。

### 线程池复用验证

线程池场景主要验证两个问题：第一，线程复用时如果不清理 ThreadLocal，后续任务可能读取到旧数据；第二，使用 `finally` 调用 `remove()` 后，可以避免数据污染。

文件位置：`src/main/java/io/github/atengk/demo/ThreadLocalThreadPoolDemo.java`

下面的代码使用单线程线程池，先演示未清理 ThreadLocal 的错误场景，再演示正确清理后的安全场景。

```java
package io.github.atengk.demo;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ThreadLocal线程池复用验证
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class ThreadLocalThreadPoolDemo {

    private static final ThreadLocal<String> USER_LOCAL = new ThreadLocal<>();

    /**
     * 线程池复用验证入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        verifyWithoutRemove();
        ThreadUtil.sleep(1000);
        verifyWithRemove();
    }

    /**
     * 验证未清理ThreadLocal导致的数据污染
     */
    private static void verifyWithoutRemove() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        executorService.execute(() -> {
            USER_LOCAL.set("10001");
            log.info("未清理场景-任务A设置用户ID：{}", USER_LOCAL.get());

            // 错误示例：故意不调用 remove()
        });

        ThreadUtil.sleep(500);

        executorService.execute(() -> {
            String userId = USER_LOCAL.get();
            log.info("未清理场景-任务B读取用户ID：{}", userId);
        });

        executorService.shutdown();
    }

    /**
     * 验证finally中清理ThreadLocal后不会产生数据污染
     */
    private static void verifyWithRemove() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        executorService.execute(() -> {
            try {
                USER_LOCAL.set("20001");
                log.info("清理场景-任务A设置用户ID：{}", USER_LOCAL.get());
            } finally {
                USER_LOCAL.remove();
                log.info("清理场景-任务A已清理ThreadLocal");
            }
        });

        ThreadUtil.sleep(500);

        executorService.execute(() -> {
            String userId = USER_LOCAL.get();
            log.info("清理场景-任务B读取用户ID：{}", userId);
        });

        executorService.shutdown();
    }
}
```

预期输出类似如下：

```text
未清理场景-任务A设置用户ID：10001
未清理场景-任务B读取用户ID：10001
清理场景-任务A设置用户ID：20001
清理场景-任务A已清理ThreadLocal
清理场景-任务B读取用户ID：null
```

从结果可以看出，线程池中线程会被复用。如果任务 A 设置了 ThreadLocal 但没有清理，任务 B 复用同一个线程时就可能读取到任务 A 的旧数据。使用 `finally` 调用 `remove()` 后，任务 B 再次读取时结果为 `null`，数据污染问题被消除。

验证结论如下：

| 验证场景     | 验证结果                    | 结论                         |
| ------------ | --------------------------- | ---------------------------- |
| 单线程场景   | set 后可读取，remove 后为空 | ThreadLocal 在当前线程内生效 |
| 多线程场景   | 不同线程读取到不同值        | ThreadLocal 可以实现线程隔离 |
| 线程池未清理 | 后续任务读取到旧值          | 线程复用会导致数据污染       |
| 线程池已清理 | 后续任务读取为空            | remove 可以避免上下文残留    |

## 总结

本节用于总结 ThreadLocal 的适用场景和风险点。ThreadLocal 是 Java 项目中非常常见的线程上下文工具，适合解决线程内部上下文传递问题，但必须控制使用范围，并严格清理资源。

### 适用场景总结

ThreadLocal 适合保存当前线程执行链路中的轻量级上下文数据。它的价值不在于替代普通变量，而是在较长调用链中减少上下文参数的重复传递。

适合使用 ThreadLocal 的场景如下：

| 场景           | 说明                                                  |
| -------------- | ----------------------------------------------------- |
| 用户上下文传递 | 保存当前登录用户 ID、用户名、角色、租户 ID 等信息     |
| 请求链路追踪   | 保存 TraceId，便于日志检索和链路排查                  |
| 多租户上下文   | 保存当前租户标识，用于权限隔离或数据隔离              |
| 动态数据源切换 | 保存当前线程的数据源 Key，用于路由到不同数据源        |
| 日志上下文增强 | 配合 MDC 输出 TraceId、UserId、TenantId 等日志字段    |
| AOP 临时上下文 | 在切面中保存方法执行期间需要使用的上下文              |
| 框架级资源绑定 | Spring 事务、数据库连接、Session 等框架内部上下文管理 |

ThreadLocal 的推荐使用流程如下：

```text
请求或任务进入
  -> 解析上下文数据
  -> 写入 ThreadLocal
  -> 执行业务逻辑
  -> 在同一线程中读取上下文
  -> finally 中调用 remove 清理数据
```

在实际项目中，推荐采用以下设计方式：

| 设计点   | 推荐做法                                            |
| -------- | --------------------------------------------------- |
| 定义方式 | 使用 `private static final ThreadLocal<T>`          |
| 访问方式 | 通过 Holder 工具类统一访问                          |
| 设置位置 | Filter、Interceptor、AOP、任务入口、消息消费入口    |
| 读取位置 | Controller、Service、Mapper、工具类等同一线程调用链 |
| 清理位置 | `finally`、`afterCompletion()`、任务结束回调        |
| 保存内容 | 用户 ID、租户 ID、TraceId、数据源 Key 等轻量数据    |

如果只是一个方法内部使用的临时数据，应优先使用普通局部变量。如果数据需要明确传递给下游方法，并且调用链不复杂，应优先使用方法参数。只有当数据属于当前线程上下文，并且需要在多层调用中读取时，才适合使用 ThreadLocal。

### 风险点总结

ThreadLocal 的主要风险不是 API 使用复杂，而是生命周期容易被忽略。尤其在线程池环境中，线程生命周期通常长于一次请求或一次任务，如果没有及时清理，就可能造成数据污染和内存泄漏。

常见风险点如下：

| 风险点                      | 说明                                               | 处理方式                                     |
| --------------------------- | -------------------------------------------------- | -------------------------------------------- |
| 数据污染                    | 线程池复用线程时，后续任务读取到上一次任务的数据   | 每次使用后在 `finally` 中调用 `remove()`     |
| 内存泄漏                    | ThreadLocalMap 中 Key 被回收但 Value 仍被线程引用  | 避免局部 ThreadLocal，使用完成后主动清理     |
| 上下文丢失                  | 异步线程、线程池任务无法自动读取父线程 ThreadLocal | 显式传参或使用任务包装器传递上下文           |
| 隐式依赖                    | 业务代码依赖线程上下文，数据来源不明显             | 统一封装 Holder，并控制使用范围              |
| 大对象残留                  | ThreadLocal 保存大集合、大对象导致内存占用过高     | 只保存轻量级上下文标识                       |
| 清理不完整                  | 只清理 ThreadLocal，忘记清理 MDC 或其他上下文      | 在统一出口集中清理所有上下文                 |
| InheritableThreadLocal 误用 | 在线程池中继承时机不符合任务提交时机               | 不依赖线程池中的自动继承，改用显式上下文传递 |

ThreadLocal 使用时应遵守以下原则：

```text
能用局部变量解决的，不使用 ThreadLocal。
能用显式参数清晰表达的，不优先使用 ThreadLocal。
确实属于线程上下文的数据，可以使用 ThreadLocal。
只要调用 set，就必须明确 remove。
只要进入线程池，就必须假设线程会被复用。
```

最终建议如下：

| 建议     | 说明                                                  |
| -------- | ----------------------------------------------------- |
| 统一封装 | 不在业务代码中直接散落创建 ThreadLocal                |
| 轻量存储 | 只保存 ID、Key、TraceId 等轻量数据                    |
| 成对操作 | `set()` 和 `remove()` 必须成对出现                    |
| 异常安全 | 清理逻辑必须放在 `finally` 或可靠的请求结束回调中     |
| 谨慎异步 | 异步任务中不要默认认为可以读取父线程上下文            |
| 定期检查 | 对上下文 Holder、拦截器、切面、线程池任务进行代码审查 |

ThreadLocal 可以让上下文传递更简洁，但也会带来隐式依赖和生命周期管理成本。在 Java 项目开发中，应将 ThreadLocal 定位为“线程内上下文工具”，而不是缓存工具、全局变量工具或跨线程通信工具。
