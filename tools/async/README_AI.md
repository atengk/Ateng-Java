# JDK 21 与 Spring Boot 3 并发编程开发

## 基础概念

本章节用于建立并发编程的基本认知。后续的 `Thread`、`ExecutorService`、`CompletableFuture`、`@Async`、线程池、虚拟线程和线程安全问题，本质上都围绕这些概念展开。实际开发中，是否使用异步、是否拆分线程池、是否使用虚拟线程、线程池参数如何设置，都应先判断任务类型、调用链路、阻塞情况和资源瓶颈。

### 同步与异步编程模型

同步和异步描述的是“调用方是否需要等待任务执行完成”。同步调用中，调用方发起任务后会一直等待结果返回，后续逻辑必须在当前任务完成后才能继续执行。异步调用中，调用方发起任务后不会立即阻塞等待，可以继续执行其他逻辑，任务结果通常通过回调、`Future`、`CompletableFuture`、消息通知或事件机制返回。

在 Java 和 Spring Boot 开发中，同步模型最常见，例如 Controller 调用 Service，Service 调用 Mapper，调用链按顺序执行。异步模型常用于耗时但不要求立即返回结果的场景，例如发送短信、发送邮件、记录操作日志、生成报表、批量导入、远程接口并行调用等。

同步模型的优点是调用链清晰、异常传播直接、事务边界明确、调试简单。缺点是如果任务耗时较长，请求线程会被长时间占用，容易降低系统吞吐量。异步模型的优点是可以释放调用方线程，提高响应速度，并发处理多个独立任务。缺点是代码复杂度更高，异常处理、上下文传递、事务边界、日志追踪和结果一致性都需要额外设计。

在 Spring Boot 3 中，常见异步方式包括 `@Async`、`CompletableFuture`、事件发布、消息队列、定时任务和虚拟线程。选择异步不应只看“能不能让接口更快返回”，还要看业务是否允许延后执行、失败是否需要补偿、结果是否必须立即可见，以及是否会引入数据一致性问题。

下面的代码演示同步调用和异步调用的基本差异。

文件位置：`src/main/java/io/github/atengk/concurrent/basic/SyncAsyncDemo.java`

该代码用于演示同步调用会等待任务执行完成，而异步调用会先返回 `CompletableFuture`，再由调用方按需获取结果。

```java
package io.github.atengk.concurrent.basic;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * 同步与异步调用示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class SyncAsyncDemo {

    /**
     * 同步获取用户积分
     *
     * @return 用户积分
     */
    public Integer queryScoreSync() {
        log.info("开始同步查询用户积分");
        ThreadUtil.sleep(1000);
        log.info("同步查询用户积分完成");
        return 100;
    }

    /**
     * 异步获取用户积分
     *
     * @return 异步积分结果
     */
    public CompletableFuture<Integer> queryScoreAsync() {
        log.info("提交异步查询用户积分任务");
        return CompletableFuture.supplyAsync(() -> {
            log.info("异步线程开始查询用户积分");
            ThreadUtil.sleep(1000);
            log.info("异步线程查询用户积分完成");
            return 100;
        });
    }

    public static void main(String[] args) {
        SyncAsyncDemo demo = new SyncAsyncDemo();

        Integer syncScore = demo.queryScoreSync();
        log.info("同步结果：{}", syncScore);

        CompletableFuture<Integer> asyncFuture = demo.queryScoreAsync();
        log.info("主线程继续执行其他逻辑");

        Integer asyncScore = asyncFuture.join();
        log.info("异步结果：{}", asyncScore);
    }
}
```

在实际项目中，同步方法适合必须立即返回结果的核心业务流程，例如下单、支付、扣减库存、保存关键数据。异步方法适合非主链路任务，例如通知、审计日志、缓存预热、数据同步、报表生成等。不要为了“看起来性能更高”盲目异步化核心事务逻辑，否则容易出现接口已返回成功但异步任务失败的问题。

### 并发与并行的区别

并发和并行都和“同时处理多个任务”有关，但二者含义不同。并发强调的是系统具备同时处理多个任务的能力，多个任务可以在同一时间段内交替推进。并行强调的是多个任务在同一时刻真正同时执行，通常依赖多核 CPU 或多台机器。

在单核 CPU 上，也可以存在并发。操作系统通过线程调度在多个任务之间快速切换，使用户感觉多个任务都在执行，但同一时刻只有一个任务真正占用 CPU。在多核 CPU 上，如果多个线程分别运行在不同 CPU 核心上，就可以实现真正的并行。

在 Java 开发中，多个请求同时进入 Spring Boot 应用，Tomcat、Undertow 或 Jetty 的工作线程会并发处理这些请求。如果机器有多个 CPU 核心，不同请求线程还可能并行执行。线程池、异步任务和虚拟线程解决的是并发处理能力问题，而 CPU 核心数量决定了 CPU 计算任务能达到的真实并行上限。

需要注意的是，并发数量不等于处理能力。并发过高时，如果任务主要消耗 CPU，线程数量超过 CPU 核心过多会导致频繁上下文切换，反而降低性能。如果任务主要等待数据库、Redis、HTTP 接口、文件系统等 IO 资源，适当提高并发可以提升吞吐量，但也必须考虑下游系统承载能力。

下面是并发与并行在开发中的常见理解：

| 概念 | 关注点                 | 典型表现             | 依赖条件           | 开发关注点                     |
| ---- | ---------------------- | -------------------- | ------------------ | ------------------------------ |
| 并发 | 一段时间内处理多个任务 | 多个请求交替执行     | 线程调度、任务切换 | 线程安全、资源隔离、限流       |
| 并行 | 同一时刻执行多个任务   | 多个任务真正同时运行 | 多核 CPU、多机器   | CPU 核心数、任务拆分、合并结果 |

实际开发中，接口能够同时接收 1000 个请求，说明系统具备并发处理能力；但这 1000 个请求不代表都在同一时刻真正占用 CPU 执行。对 IO 密集型接口，提高并发通常能提升吞吐量；对 CPU 密集型任务，应更关注 CPU 核心数、任务拆分粒度和线程数控制。

### 阻塞与非阻塞

阻塞与非阻塞描述的是“当前线程在等待结果时是否被挂起”。阻塞调用中，线程发起操作后，如果结果没有准备好，线程会暂停继续执行，直到操作完成或超时。非阻塞调用中，线程发起操作后，如果结果没有准备好，不会一直等待，可以立即返回并继续执行其他逻辑。

常见阻塞操作包括数据库查询、远程 HTTP 调用、文件读写、Redis 请求、消息发送等待确认、`Thread.sleep()`、`Future.get()`、锁等待等。阻塞本身不是错误，大多数传统 Java Web 应用都是基于阻塞 IO 编程模型构建的。问题在于阻塞会占用线程，如果大量线程都在等待 IO，平台线程资源会被快速耗尽。

非阻塞通常依赖事件循环、回调、响应式编程或底层非阻塞 IO。例如 Spring WebFlux、Netty、异步 HTTP Client 等可以通过较少线程处理大量连接。但非阻塞编程模型对代码组织、调用链、调试方式和团队能力要求更高，并不适合所有业务场景。

JDK 21 虚拟线程改变了传统阻塞模型的使用成本。虚拟线程允许开发者继续使用同步阻塞风格编写代码，但虚拟线程在阻塞等待时不会长期占用昂贵的平台线程。也就是说，虚拟线程不是把阻塞调用变成非阻塞调用，而是降低了大量阻塞等待带来的线程资源成本。

下面的代码演示阻塞等待和非阻塞编排的差异。

文件位置：`src/main/java/io/github/atengk/concurrent/basic/BlockingDemo.java`

该代码用于演示 `Future.get()` 会阻塞当前线程，而 `CompletableFuture.thenAccept()` 可以注册后续动作，不强制当前线程立即等待结果。

```java
package io.github.atengk.concurrent.basic;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * 阻塞与非阻塞调用示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class BlockingDemo {

    /**
     * 模拟远程接口调用
     *
     * @return 接口结果
     */
    public CompletableFuture<String> requestRemoteApi() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("开始调用远程接口");
            ThreadUtil.sleep(1500);
            log.info("远程接口调用完成");
            return "SUCCESS";
        });
    }

    public static void main(String[] args) {
        BlockingDemo demo = new BlockingDemo();

        CompletableFuture<String> future = demo.requestRemoteApi();

        future.thenAccept(result -> log.info("异步回调处理结果：{}", result));

        log.info("主线程不立即阻塞等待，可以继续执行其他逻辑");

        ThreadUtil.sleep(2000);
    }
}
```

在 Spring Boot 3 项目中，如果使用传统 Spring MVC，大多数数据库和 HTTP 调用仍然是阻塞式调用。此时应重点做好线程池隔离、超时控制、连接池配置和下游限流。如果使用 WebFlux 或 Netty，则要避免在事件循环线程中执行阻塞操作，否则会破坏非阻塞模型。如果使用 JDK 21 虚拟线程，可以继续保留同步阻塞代码风格，但仍然需要控制数据库连接池、HTTP 连接池和外部服务容量，因为虚拟线程只减少线程成本，不会减少下游资源消耗。

### CPU 密集型与 IO 密集型任务

CPU 密集型和 IO 密集型描述的是任务主要消耗哪类资源。CPU 密集型任务主要消耗 CPU 计算能力，例如加解密、压缩解压、图片处理、复杂规则计算、大批量数据聚合、报表计算等。IO 密集型任务主要花费时间等待外部资源返回，例如数据库查询、Redis 请求、HTTP 调用、文件读写、消息队列交互等。

区分任务类型是设计线程池和异步方案的前提。CPU 密集型任务的线程数通常不宜远大于 CPU 核心数，因为线程太多会增加上下文切换。IO 密集型任务中，线程经常处于等待状态，可以适当增加线程数，提高并发处理能力，但必须结合数据库连接池、HTTP 连接池、Redis 连接池、消息队列吞吐和下游服务限流综合评估。

常见判断方式如下：

| 任务类型   | 主要瓶颈     | 常见场景                      | 线程数设计倾向    | 风险                   |
| ---------- | ------------ | ----------------------------- | ----------------- | ---------------------- |
| CPU 密集型 | CPU 核心     | 加密、压缩、计算、图片处理    | 接近 CPU 核心数   | 线程过多导致上下文切换 |
| IO 密集型  | 外部资源等待 | 数据库、HTTP、Redis、文件、MQ | 可大于 CPU 核心数 | 下游被打满、连接池耗尽 |
| 混合型     | CPU + IO     | 查询后计算、文件解析后入库    | 分阶段隔离线程池  | 单线程池互相拖垮       |

在 Spring Boot 项目中，不建议所有异步任务共用一个线程池。发送短信、批量导入、报表生成、文件处理、远程接口调用等任务的资源特征不同，应按业务场景隔离线程池。这样可以避免某类慢任务占满线程池后影响其他业务。

例如，报表计算属于偏 CPU 密集型，可以设置较小线程数并限制队列长度。批量调用外部 HTTP 接口属于 IO 密集型，可以设置更大的线程数，但必须配置超时时间、连接池上限和限流策略。文件上传后的异步解析可能是混合型任务，可以拆成文件读取、数据校验、批量入库几个阶段分别控制。

下面的代码演示 CPU 密集型和 IO 密集型任务的基本差异。

文件位置：`src/main/java/io/github/atengk/concurrent/basic/TaskTypeDemo.java`

该代码用于模拟 CPU 密集型计算和 IO 密集型等待，帮助理解两类任务在线程池设计上的差异。

```java
package io.github.atengk.concurrent.basic;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

/**
 * CPU 密集型与 IO 密集型任务示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class TaskTypeDemo {

    /**
     * 模拟 CPU 密集型任务
     *
     * @return 计算结果
     */
    public BigInteger calculateCpuTask() {
        log.info("开始执行 CPU 密集型计算任务");
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < 200000; i++) {
            result = result.add(BigInteger.valueOf(i).multiply(BigInteger.valueOf(i)));
        }
        log.info("CPU 密集型计算任务完成");
        return result;
    }

    /**
     * 模拟 IO 密集型任务
     *
     * @return 查询结果
     */
    public String queryIoTask() {
        log.info("开始执行 IO 密集型等待任务");
        ThreadUtil.sleep(1000);
        log.info("IO 密集型等待任务完成");
        return "DATA";
    }

    public static void main(String[] args) {
        TaskTypeDemo demo = new TaskTypeDemo();

        CompletableFuture<BigInteger> cpuFuture = CompletableFuture.supplyAsync(demo::calculateCpuTask);
        CompletableFuture<String> ioFuture = CompletableFuture.supplyAsync(demo::queryIoTask);

        log.info("CPU 任务结果：{}", cpuFuture.join());
        log.info("IO 任务结果：{}", ioFuture.join());
    }
}
```

实际开发中可以按以下原则处理：

第一，CPU 密集型任务不要盲目开大线程池。线程数可以参考 `CPU 核心数` 或 `CPU 核心数 + 1`，重点减少上下文切换，并避免长时间占满业务请求线程。

第二，IO 密集型任务可以适当增加线程数，但必须设置超时、队列长度和拒绝策略。没有超时控制的 IO 任务会导致线程长期挂起，最终拖垮线程池。

第三，不同任务应使用不同线程池。例如订单异步通知、文件解析、报表计算、第三方接口同步不应全部放入同一个 `commonPool` 或默认 `@Async` 线程池。

第四，虚拟线程更适合大量阻塞式 IO 场景，例如大量 HTTP 调用、数据库查询、文件读取等。它不适合替代 CPU 密集型任务的计算线程池，因为 CPU 核心数量不会因为虚拟线程增加而增加。

第五，线程池设计必须结合外部资源容量。即使应用能创建 1000 个并发任务，如果数据库连接池只有 50 个连接，大量任务仍然会阻塞在获取连接上。线程池、连接池、超时时间、限流策略必须一起设计。

以下内容继续补充到你当前大纲的两个章节中。你上传的原始大纲中，这两部分位于“JDK 21 并发能力”和“Spring Boot 3 异步编程”。

## JDK 21 并发能力

本章节用于说明 JDK 21 原生并发 API 的使用方式。JDK 21 中虚拟线程已经成为正式特性，而 `StructuredTaskScope` 和 `ScopedValue` 在 JDK 21 中仍属于预览 API，使用时需要显式开启 `--enable-preview`。`StructuredTaskScope` 用于把一组并发子任务作为一个整体管理，`ScopedValue` 用于在线程调用链中安全传递只读上下文，尤其适合大量虚拟线程场景。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

### Thread 基础使用

`Thread` 是 Java 中线程的基础抽象，表示一个可被调度执行的任务单元。JDK 21 中线程可以分为平台线程和虚拟线程。平台线程通常对应操作系统线程，创建成本较高；虚拟线程由 JDK 管理，适合大量阻塞式 IO 任务。Oracle 官方文档也明确将 Java 线程分为 platform thread 和 virtual thread 两类。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html?utm_source=chatgpt.com))

传统平台线程适合少量、生命周期明确的任务。开发中不建议频繁手动创建大量平台线程，而应优先使用线程池或虚拟线程。手动创建线程更多用于学习、简单工具、框架底层封装或极少量后台任务。

文件位置：`src/main/java/io/github/atengk/concurrent/jdk/ThreadDemo.java`

该代码演示 JDK 21 中平台线程和虚拟线程的基本创建方式。

```java
package io.github.atengk.concurrent.jdk;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class ThreadDemo {

    /**
     * 启动平台线程
     */
    public void startPlatformThread() {
        Thread thread = Thread.ofPlatform()
                .name("platform-worker-", 1)
                .start(() -> {
                    log.info("平台线程开始执行，线程名称：{}", Thread.currentThread().getName());
                    ThreadUtil.sleep(500);
                    log.info("平台线程执行完成");
                });

        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待平台线程执行完成时被中断", e);
        }
    }

    /**
     * 启动虚拟线程
     */
    public void startVirtualThread() {
        Thread thread = Thread.ofVirtual()
                .name("virtual-worker-", 1)
                .start(() -> {
                    log.info("虚拟线程开始执行，线程名称：{}", Thread.currentThread().getName());
                    ThreadUtil.sleep(500);
                    log.info("虚拟线程执行完成");
                });

        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待虚拟线程执行完成时被中断", e);
        }
    }

    public static void main(String[] args) {
        ThreadDemo demo = new ThreadDemo();
        demo.startPlatformThread();
        demo.startVirtualThread();
    }
}
```

实际开发中，不建议在 Controller 或 Service 中直接 `new Thread()`。直接创建线程会绕开 Spring 生命周期管理，也不利于统一命名、监控、异常处理、优雅停机和上下文传递。业务代码中应优先选择 `ExecutorService`、`ThreadPoolTaskExecutor`、`@Async`、`CompletableFuture` 或虚拟线程执行器。

### Runnable 与 Callable

`Runnable` 和 `Callable` 都表示可提交执行的任务。`Runnable` 没有返回值，不能直接抛出受检异常；`Callable` 有返回值，可以抛出异常，通常配合 `Future` 或 `ExecutorService.submit()` 使用。`Executors` 官方文档中也说明它提供了用于创建 `ExecutorService`、`ScheduledExecutorService`、`ThreadFactory` 和把其他闭包形式转换为 `Callable` 的工具方法。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Executors.html?utm_source=chatgpt.com))

`Runnable` 适合日志记录、消息发送、缓存清理、文件删除等不关心返回值的任务。`Callable` 适合远程查询、批量计算、数据聚合等需要返回结果或捕获异常的任务。

文件位置：`src/main/java/io/github/atengk/concurrent/jdk/RunnableCallableDemo.java`

该代码演示 `Runnable` 与 `Callable` 的使用差异。

```java
package io.github.atengk.concurrent.jdk;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Runnable 与 Callable 示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class RunnableCallableDemo {

    /**
     * 执行 Runnable 任务
     */
    public void runRunnableTask() {
        Runnable task = () -> {
            log.info("Runnable 任务开始执行");
            ThreadUtil.sleep(500);
            log.info("Runnable 任务执行完成");
        };

        Thread.ofPlatform()
                .name("runnable-worker-", 1)
                .start(task);
    }

    /**
     * 执行 Callable 任务
     *
     * @return 任务结果
     */
    public String runCallableTask() {
        Callable<String> task = () -> {
            log.info("Callable 任务开始执行");
            ThreadUtil.sleep(500);
            return "Callable 执行结果";
        };

        FutureTask<String> futureTask = new FutureTask<>(task);

        Thread.ofPlatform()
                .name("callable-worker-", 1)
                .start(futureTask);

        try {
            return futureTask.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取 Callable 结果时线程被中断", e);
            return "INTERRUPTED";
        } catch (ExecutionException e) {
            log.error("Callable 任务执行异常", e);
            return "ERROR";
        }
    }

    public static void main(String[] args) {
        RunnableCallableDemo demo = new RunnableCallableDemo();
        demo.runRunnableTask();

        String result = demo.runCallableTask();
        log.info("Callable 返回结果：{}", result);
    }
}
```

开发选择上，任务没有返回值时使用 `Runnable`；任务需要结果、异常或后续编排时使用 `Callable` 或 `CompletableFuture`。在 Spring Boot 项目中，简单异步任务可以用 `@Async void`，需要返回异步结果时建议使用 `CompletableFuture<T>`。

### Future 与 CompletableFuture

`Future` 表示异步任务的未来结果，可以通过 `get()` 获取结果，但 `get()` 会阻塞当前线程。`CompletableFuture` 同时实现了 `Future` 和 `CompletionStage`，既可以作为异步结果容器，也可以进行任务编排、异常处理、组合执行和超时控制。Oracle JDK 21 API 文档说明，`CompletableFuture` 是一个可显式完成的 `Future`，并且可作为 `CompletionStage` 使用。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html?source=%3Aso%3Atw%3Aor%3Aawr%3Aosec%3A%2C%3Aso%3Atw%3Aor%3Aawr%3Aosec%3A&utm_source=chatgpt.com))

`Future` 的主要问题是编排能力弱。多个异步任务之间如果存在串行依赖、并行合并、异常兜底、超时返回等逻辑，使用 `Future.get()` 会导致代码退化成阻塞式等待。`CompletableFuture` 更适合现代 Java 异步编程。

文件位置：`src/main/java/io/github/atengk/concurrent/jdk/FutureCompletableFutureDemo.java`

该代码演示 `Future` 阻塞获取结果，以及 `CompletableFuture` 的异步编排和异常兜底。

```java
package io.github.atengk.concurrent.jdk;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * Future 与 CompletableFuture 示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class FutureCompletableFutureDemo {

    /**
     * 使用 Future 获取异步结果
     *
     * @return 查询结果
     */
    public String queryByFuture() {
        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            Future<String> future = executorService.submit(() -> {
                log.info("Future 任务开始执行");
                ThreadUtil.sleep(800);
                return "Future Result";
            });

            return future.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Future 获取结果时线程被中断", e);
            return "INTERRUPTED";
        } catch (ExecutionException e) {
            log.error("Future 任务执行异常", e);
            return "ERROR";
        } catch (TimeoutException e) {
            log.error("Future 任务执行超时", e);
            return "TIMEOUT";
        }
    }

    /**
     * 使用 CompletableFuture 编排异步任务
     *
     * @return 编排后的异步结果
     */
    public CompletableFuture<String> queryByCompletableFuture() {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        return CompletableFuture.supplyAsync(() -> {
                    log.info("开始查询用户基础信息");
                    ThreadUtil.sleep(500);
                    return "用户A";
                }, executorService)
                .thenCombine(CompletableFuture.supplyAsync(() -> {
                    log.info("开始查询用户积分");
                    ThreadUtil.sleep(700);
                    return 100;
                }, executorService), (userName, score) -> userName + "，积分：" + score)
                .orTimeout(2, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("CompletableFuture 任务执行异常", e);
                    return "默认用户信息";
                })
                .whenComplete((result, e) -> executorService.shutdown());
    }

    public static void main(String[] args) {
        FutureCompletableFutureDemo demo = new FutureCompletableFutureDemo();

        String futureResult = demo.queryByFuture();
        log.info("Future 返回结果：{}", futureResult);

        String completableResult = demo.queryByCompletableFuture().join();
        log.info("CompletableFuture 返回结果：{}", completableResult);
    }
}
```

使用 `CompletableFuture` 时，不建议依赖默认的 `ForkJoinPool.commonPool()` 执行业务 IO 任务。实际项目中应传入自定义线程池，按业务隔离任务类型，避免某类慢任务占满公共线程池。

### Executor 与 ExecutorService

`Executor` 是最基础的任务执行接口，只定义了 `execute(Runnable)` 方法。`ExecutorService` 在此基础上增加了任务提交、结果获取、批量执行、关闭线程池等能力。JDK 21 官方文档中，`ExecutorService.submit(Callable<T>)` 会提交有返回值的任务，并返回代表任务结果的 `Future<T>`。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ExecutorService.html?utm_source=chatgpt.com))

`Executor` 关注“如何执行任务”，`ExecutorService` 关注“如何管理任务生命周期”。实际开发中，手动使用 JDK 线程池时通常使用 `ExecutorService`，因为它支持 `submit()`、`shutdown()`、`invokeAll()`、`invokeAny()` 等管理能力。

文件位置：`src/main/java/io/github/atengk/concurrent/jdk/ExecutorServiceDemo.java`

该代码演示 `ExecutorService` 提交多个任务、获取结果并关闭线程池。

```java
package io.github.atengk.concurrent.jdk;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

/**
 * Executor 与 ExecutorService 示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class ExecutorServiceDemo {

    /**
     * 批量执行查询任务
     *
     * @return 查询结果列表
     */
    public List<String> queryBatchData() {
        try (ExecutorService executorService = Executors.newFixedThreadPool(3)) {
            List<Callable<String>> tasks = List.of(
                    () -> queryRemoteData("订单服务"),
                    () -> queryRemoteData("库存服务"),
                    () -> queryRemoteData("用户服务")
            );

            return executorService.invokeAll(tasks)
                    .stream()
                    .map(this::getFutureResult)
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("批量任务执行时线程被中断", e);
            return List.of();
        }
    }

    /**
     * 模拟远程服务查询
     *
     * @param serviceName 服务名称
     * @return 查询结果
     */
    private String queryRemoteData(String serviceName) {
        log.info("开始查询：{}", serviceName);
        ThreadUtil.sleep(500);
        return serviceName + "返回成功";
    }

    /**
     * 获取 Future 结果
     *
     * @param future 异步结果
     * @return 任务结果
     */
    private String getFutureResult(Future<String> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取任务结果时线程被中断", e);
            return "INTERRUPTED";
        } catch (ExecutionException e) {
            log.error("任务执行异常", e);
            return "ERROR";
        }
    }

    public static void main(String[] args) {
        ExecutorServiceDemo demo = new ExecutorServiceDemo();
        List<String> results = demo.queryBatchData();
        log.info("批量查询结果：{}", results);
    }
}
```

`Executors.newFixedThreadPool()`、`Executors.newCachedThreadPool()` 等工厂方法适合示例和简单工具，但在生产环境中更推荐直接使用 `ThreadPoolExecutor` 或 Spring 的 `ThreadPoolTaskExecutor`，显式设置核心线程数、最大线程数、队列长度、线程名称和拒绝策略。

### ScheduledExecutorService

`ScheduledExecutorService` 用于延迟执行或周期性执行任务。它适合在普通 Java 程序中实现轻量级定时任务，例如延迟清理缓存、周期检查状态、定时刷新本地配置等。Spring Boot 项目中，如果是业务定时任务，通常优先使用 `@Scheduled`、XXL-JOB、Quartz 或消息队列延迟任务；如果只是组件内部的轻量调度，可以使用 `ScheduledExecutorService`。

`ScheduledExecutorService` 需要重点关注异常处理。周期性任务如果未捕获异常，后续调度可能受到影响。因此周期任务内部必须主动捕获异常并记录日志。

文件位置：`src/main/java/io/github/atengk/concurrent/jdk/ScheduledExecutorServiceDemo.java`

该代码演示延迟任务和周期任务的基本使用方式。

```java
package io.github.atengk.concurrent.jdk;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * ScheduledExecutorService 示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class ScheduledExecutorServiceDemo {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            2,
            ThreadFactoryBuilder.create()
                    .setNamePrefix("schedule-worker-")
                    .build()
    );

    /**
     * 执行延迟任务
     */
    public void scheduleDelayTask() {
        scheduler.schedule(() -> {
            log.info("延迟任务开始执行");
            ThreadUtil.sleep(300);
            log.info("延迟任务执行完成");
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * 执行周期任务
     */
    public void scheduleFixedRateTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("周期任务开始执行");
                ThreadUtil.sleep(500);
                log.info("周期任务执行完成");
            } catch (Exception e) {
                log.error("周期任务执行异常", e);
            }
        }, 1, 2, TimeUnit.SECONDS);
    }

    /**
     * 关闭调度线程池
     */
    public void shutdown() {
        scheduler.shutdown();
        log.info("调度线程池已关闭");
    }

    public static void main(String[] args) {
        ScheduledExecutorServiceDemo demo = new ScheduledExecutorServiceDemo();
        demo.scheduleDelayTask();
        demo.scheduleFixedRateTask();

        ThreadUtil.sleep(6000);
        demo.shutdown();
    }
}
```

`scheduleAtFixedRate()` 以上一次任务的计划开始时间为基准，适合固定频率调度；`scheduleWithFixedDelay()` 以上一次任务结束时间为基准，适合避免任务堆积。任务执行时间可能超过调度间隔时，应优先考虑 `scheduleWithFixedDelay()`。

### Structured Concurrency

结构化并发用于把一组相关的并发子任务作为一个整体处理。它强调子任务的生命周期必须被限制在父任务作用域内，父任务可以统一等待、统一取消、统一处理异常。JDK 21 的 `StructuredTaskScope` 是预览 API，使用时必须开启预览特性。官方 API 文档说明，它支持把任务拆分为多个并发子任务，并要求这些子任务在主任务继续执行前完成。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/StructuredTaskScope.html?utm_source=chatgpt.com))

它适合“一个请求需要并行查询多个资源，并且这些资源共同组成最终结果”的场景。例如订单详情页需要同时查询订单、用户、库存、优惠券，如果任意关键查询失败，就取消其他子任务并统一失败。

文件位置：`src/main/java/io/github/atengk/concurrent/jdk/StructuredConcurrencyDemo.java`

该代码演示使用 `StructuredTaskScope.ShutdownOnFailure` 并行查询多个结果，任意子任务失败时自动取消未完成任务。

```java
package io.github.atengk.concurrent.jdk;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.StructuredTaskScope;
import java.util.function.Supplier;

/**
 * Structured Concurrency 示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class StructuredConcurrencyDemo {

    /**
     * 并行查询订单详情
     *
     * @return 订单详情
     * @throws Exception 查询异常
     */
    public String queryOrderDetail() throws Exception {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            Supplier<String> orderSupplier = scope.fork(this::queryOrder);
            Supplier<String> userSupplier = scope.fork(this::queryUser);
            Supplier<String> stockSupplier = scope.fork(this::queryStock);

            scope.join();
            scope.throwIfFailed();

            return orderSupplier.get() + "，" + userSupplier.get() + "，" + stockSupplier.get();
        }
    }

    /**
     * 查询订单信息
     *
     * @return 订单信息
     */
    private String queryOrder() {
        log.info("开始查询订单信息");
        ThreadUtil.sleep(500);
        return "订单：NO1001";
    }

    /**
     * 查询用户信息
     *
     * @return 用户信息
     */
    private String queryUser() {
        log.info("开始查询用户信息");
        ThreadUtil.sleep(800);
        return "用户：Ateng";
    }

    /**
     * 查询库存信息
     *
     * @return 库存信息
     */
    private String queryStock() {
        log.info("开始查询库存信息");
        ThreadUtil.sleep(600);
        return "库存：充足";
    }

    public static void main(String[] args) throws Exception {
        StructuredConcurrencyDemo demo = new StructuredConcurrencyDemo();
        String result = demo.queryOrderDetail();
        log.info("订单详情：{}", result);
    }
}
```

如果使用 Maven 编译 JDK 21 预览 API，需要开启 `--enable-preview`。

文件位置：`pom.xml`

该配置用于让 Maven 编译和运行测试时开启 JDK 21 预览特性。

```xml
<build>
    <plugins>
        <!-- 使用 JDK 21 编译，并开启预览特性 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <release>21</release>
                <compilerArgs>
                    <arg>--enable-preview</arg>
                </compilerArgs>
            </configuration>
        </plugin>

        <!-- 测试阶段同样需要开启预览特性 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <argLine>--enable-preview</argLine>
            </configuration>
        </plugin>
    </plugins>
</build>
```

结构化并发适合替代部分手写 `CompletableFuture.allOf()` 的场景。相比手动管理多个 future，它的生命周期边界更清晰，异常取消策略更明确，代码结构更接近同步代码。

### Scoped Value

`ScopedValue` 是 JDK 21 中的预览 API，用于在调用链中传递只读上下文。它可以理解为一种受作用域限制的上下文变量，适合传递请求 ID、租户 ID、用户 ID、链路信息等不可变上下文。OpenJDK JEP 446 明确说明，Scoped Values 用于安全、高效地向方法共享值而不需要使用方法参数，并且在大量虚拟线程场景下相比 `ThreadLocal` 更适合。([OpenJDK](https://openjdk.org/jeps/446?utm_source=chatgpt.com))

`ThreadLocal` 的问题是生命周期容易失控，在线程池复用场景下如果忘记清理，可能造成上下文污染。`ScopedValue` 的值绑定在明确的作用域中，作用域结束后绑定自动失效，更符合结构化并发和虚拟线程的使用模型。

文件位置：`src/main/java/io/github/atengk/concurrent/jdk/ScopedValueDemo.java`

该代码演示使用 `ScopedValue` 在调用链中传递请求 ID。

```java
package io.github.atengk.concurrent.jdk;

import lombok.extern.slf4j.Slf4j;

/**
 * ScopedValue 示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class ScopedValueDemo {

    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    /**
     * 在请求作用域内执行业务逻辑
     *
     * @param requestId 请求 ID
     */
    public void executeWithRequestId(String requestId) {
        ScopedValue.where(REQUEST_ID, requestId)
                .run(() -> {
                    log.info("开始处理请求，请求ID：{}", REQUEST_ID.get());
                    doBusiness();
                    log.info("请求处理完成，请求ID：{}", REQUEST_ID.get());
                });
    }

    /**
     * 执行业务逻辑
     */
    private void doBusiness() {
        log.info("业务处理中，请求ID：{}", REQUEST_ID.get());
    }

    public static void main(String[] args) {
        ScopedValueDemo demo = new ScopedValueDemo();
        demo.executeWithRequestId("REQ-10001");
    }
}
```

`ScopedValue` 更适合只读上下文，不适合频繁修改的变量。对于需要跨线程池、跨服务、跨消息链路传递的上下文，仍然需要结合日志 MDC、HTTP Header、消息 Header 或链路追踪组件设计。

## Spring Boot 3 异步编程

本章节用于说明 Spring Boot 3 中异步任务的配置、调用方式、返回值、异常处理和上下文传递。Spring 官方文档说明，`@EnableAsync` 用于启用 Spring 的异步方法执行能力，默认会查找上下文中的 `TaskExecutor` 或名为 `taskExecutor` 的 `Executor`，否则使用 `SimpleAsyncTaskExecutor` 执行异步方法。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/EnableAsync.html?utm_source=chatgpt.com))

### @EnableAsync 启用异步能力

`@EnableAsync` 用于开启 Spring 异步方法代理能力。开启后，Spring 会扫描 Bean 中标注 `@Async` 的方法，并通过代理对象把方法调用提交到异步执行器中执行。Spring Boot 官方文档说明，如果上下文中没有 `Executor` Bean，Spring Boot 会自动配置一个 `AsyncTaskExecutor`；如果在 Java 21+ 中开启 `spring.threads.virtual.enabled=true`，该自动执行器会使用虚拟线程。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

生产项目中建议显式配置业务线程池，不要完全依赖默认执行器。显式线程池便于设置线程名称、核心线程数、最大线程数、队列长度、拒绝策略、异常处理和上下文传递。

文件位置：`pom.xml`

该依赖用于 Spring Boot 3 Web 项目、异步示例、日志、Lombok 和 Hutool 工具类。

```xml
<dependencies>
    <!-- Spring Boot Web 基础能力，包含 Controller、JSON、内嵌容器等 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于线程休眠、字符串处理、集合处理等常见开发场景 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok 简化日志对象、构造方法、Getter、Setter 等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于异步任务单元测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

文件位置：`src/main/java/io/github/atengk/concurrent/config/AsyncConfig.java`

该配置类用于启用 `@Async`，并提供一个业务异步线程池和异步异常处理器。

```java
package io.github.atengk.concurrent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 配置业务异步线程池
     *
     * @return 业务异步线程池
     */
    @Bean("bizAsyncExecutor")
    public ThreadPoolTaskExecutor bizAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("biz-async-");

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * 指定默认异步执行器
     *
     * @return 异步执行器
     */
    @Override
    public Executor getAsyncExecutor() {
        return bizAsyncExecutor();
    }

    /**
     * 配置 void 异步方法的未捕获异常处理器
     *
     * @return 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("异步方法执行异常，方法：{}，参数：{}", method.getName(), params, ex);
    }
}
```

`@EnableAsync` 的配置类会在 Spring 容器启动早期初始化。Spring 官方文档也提醒，实现 `AsyncConfigurer` 的配置类会较早初始化，如果依赖其他 Bean，应谨慎处理依赖关系。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/EnableAsync.html?utm_source=chatgpt.com))

### @Async 基础使用

`@Async` 可以标注在 Spring Bean 的方法上，使该方法通过异步执行器执行。调用方调用异步方法后，方法体会在线程池线程中执行，调用方线程可以继续处理后续逻辑。

`@Async` 可以直接指定线程池 Bean 名称。例如 `@Async("bizAsyncExecutor")` 表示该方法提交到名为 `bizAsyncExecutor` 的执行器中执行。业务项目中建议按任务类型拆分线程池，例如通知线程池、报表线程池、文件处理线程池、远程调用线程池，避免所有异步任务互相影响。

文件位置：`src/main/java/io/github/atengk/concurrent/service/NotifyAsyncService.java`

该服务用于演示普通异步通知任务。

```java
package io.github.atengk.concurrent.service;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步通知服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class NotifyAsyncService {

    /**
     * 异步发送短信
     *
     * @param phone 手机号
     * @param content 短信内容
     */
    @Async("bizAsyncExecutor")
    public void sendSms(String phone, String content) {
        log.info("开始异步发送短信，手机号：{}，内容：{}", phone, content);
        ThreadUtil.sleep(1000);
        log.info("异步短信发送完成，手机号：{}", phone);
    }

    /**
     * 异步发送邮件
     *
     * @param email 邮箱
     * @param title 标题
     */
    @Async("bizAsyncExecutor")
    public void sendEmail(String email, String title) {
        log.info("开始异步发送邮件，邮箱：{}，标题：{}", email, title);
        ThreadUtil.sleep(1200);
        log.info("异步邮件发送完成，邮箱：{}", email);
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/controller/NotifyController.java`

该接口用于触发异步通知任务，接口会先返回，短信和邮件在异步线程中执行。

```java
package io.github.atengk.concurrent.controller;

import io.github.atengk.concurrent.service.NotifyAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 异步通知接口
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/notify")
public class NotifyController {

    private final NotifyAsyncService notifyAsyncService;

    /**
     * 发送通知
     *
     * @param phone 手机号
     * @param email 邮箱
     * @return 提交结果
     */
    @PostMapping("/send")
    public String sendNotify(@RequestParam String phone, @RequestParam String email) {
        log.info("接收到发送通知请求，手机号：{}，邮箱：{}", phone, email);

        notifyAsyncService.sendSms(phone, "您的验证码是 123456");
        notifyAsyncService.sendEmail(email, "系统通知");

        return "通知任务已提交";
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/notify/send?phone=13800000000&email=ateng@example.com"
```

预期结果是接口快速返回 `通知任务已提交`，控制台日志中可以看到 `biz-async-` 开头的线程继续执行短信和邮件发送逻辑。

### 异步方法返回值

异步方法可以返回 `void`，也可以返回 `CompletableFuture<T>`。如果调用方不关心结果，可以使用 `void`；如果调用方需要等待、组合、超时控制或异常兜底，建议使用 `CompletableFuture<T>`。`CompletableFuture` 支持 `supplyAsync()`、`thenApply()`、`thenCombine()`、`allOf()`、`exceptionally()`、`orTimeout()` 等编排能力。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html?source=%3Aso%3Atw%3Aor%3Aawr%3Aosec%3A%2C%3Aso%3Atw%3Aor%3Aawr%3Aosec%3A&utm_source=chatgpt.com))

在 Spring `@Async` 方法中，推荐返回 `CompletableFuture.completedFuture(result)` 或在方法内部直接返回一个完成后的 `CompletableFuture`。异步执行由 Spring 代理负责，不需要在 `@Async` 方法内部再次调用 `CompletableFuture.supplyAsync()`，否则容易出现嵌套异步和线程池混乱。

文件位置：`src/main/java/io/github/atengk/concurrent/service/UserQueryAsyncService.java`

该服务用于演示异步查询用户信息和积分信息，并返回 `CompletableFuture`。

```java
package io.github.atengk.concurrent.service;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 用户异步查询服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class UserQueryAsyncService {

    /**
     * 异步查询用户名称
     *
     * @param userId 用户ID
     * @return 用户名称
     */
    @Async("bizAsyncExecutor")
    public CompletableFuture<String> queryUserName(Long userId) {
        log.info("开始异步查询用户名称，用户ID：{}", userId);
        ThreadUtil.sleep(700);
        return CompletableFuture.completedFuture("Ateng");
    }

    /**
     * 异步查询用户积分
     *
     * @param userId 用户ID
     * @return 用户积分
     */
    @Async("bizAsyncExecutor")
    public CompletableFuture<Integer> queryUserScore(Long userId) {
        log.info("开始异步查询用户积分，用户ID：{}", userId);
        ThreadUtil.sleep(900);
        return CompletableFuture.completedFuture(100);
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/controller/UserQueryController.java`

该接口用于组合两个异步查询结果，并设置整体超时和异常兜底。

```java
package io.github.atengk.concurrent.controller;

import io.github.atengk.concurrent.service.UserQueryAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 用户异步查询接口
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserQueryController {

    private final UserQueryAsyncService userQueryAsyncService;

    /**
     * 查询用户聚合信息
     *
     * @param userId 用户ID
     * @return 用户聚合信息
     */
    @GetMapping("/{userId}/summary")
    public CompletableFuture<String> queryUserSummary(@PathVariable Long userId) {
        CompletableFuture<String> userNameFuture = userQueryAsyncService.queryUserName(userId);
        CompletableFuture<Integer> scoreFuture = userQueryAsyncService.queryUserScore(userId);

        return userNameFuture
                .thenCombine(scoreFuture, (userName, score) -> "用户：" + userName + "，积分：" + score)
                .orTimeout(3, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("查询用户聚合信息异常，用户ID：{}", userId, e);
                    return "用户信息查询失败";
                });
    }
}
```

接口调用示例：

```bash
curl "http://localhost:8080/users/10001/summary"
```

返回示例：

```json
"用户：Ateng，积分：100"
```

### 异步异常处理

异步异常处理取决于方法返回值类型。`void` 异步方法中的异常无法传递给调用方，需要通过 `AsyncUncaughtExceptionHandler` 统一处理。返回 `CompletableFuture<T>` 的异步方法，可以通过 `exceptionally()`、`handle()`、`whenComplete()` 等方式处理异常。Spring 官方文档也说明，`void` 返回类型的异步方法不能把异常传回调用方，默认情况下未捕获异常只会被记录。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/EnableAsync.html?utm_source=chatgpt.com))

生产项目中，不建议异步方法静默吞异常。异步任务失败后至少要记录关键业务参数、任务类型、异常堆栈。关键异步任务还应配合重试、补偿表、消息队列或人工告警。

文件位置：`src/main/java/io/github/atengk/concurrent/service/AsyncExceptionDemoService.java`

该服务演示 `void` 异步异常和 `CompletableFuture` 异步异常的处理差异。

```java
package io.github.atengk.concurrent.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 异步异常处理示例服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class AsyncExceptionDemoService {

    /**
     * void 异步方法异常会进入 AsyncUncaughtExceptionHandler
     *
     * @param taskNo 任务编号
     */
    @Async("bizAsyncExecutor")
    public void executeVoidTask(String taskNo) {
        log.info("开始执行 void 异步任务，任务编号：{}", taskNo);

        if (StrUtil.isBlank(taskNo)) {
            throw new IllegalArgumentException("任务编号不能为空");
        }

        log.info("void 异步任务执行完成，任务编号：{}", taskNo);
    }

    /**
     * CompletableFuture 异步方法异常可由调用方编排处理
     *
     * @param taskNo 任务编号
     * @return 执行结果
     */
    @Async("bizAsyncExecutor")
    public CompletableFuture<String> executeFutureTask(String taskNo) {
        log.info("开始执行 CompletableFuture 异步任务，任务编号：{}", taskNo);

        if (StrUtil.isBlank(taskNo)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("任务编号不能为空"));
        }

        return CompletableFuture.completedFuture("任务执行成功：" + taskNo);
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/controller/AsyncExceptionDemoController.java`

该接口用于验证两类异步异常处理方式。

```java
package io.github.atengk.concurrent.controller;

import io.github.atengk.concurrent.service.AsyncExceptionDemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * 异步异常处理示例接口
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/async-exception")
public class AsyncExceptionDemoController {

    private final AsyncExceptionDemoService asyncExceptionDemoService;

    /**
     * 触发 void 异步异常
     *
     * @return 提交结果
     */
    @PostMapping("/void")
    public String triggerVoidException() {
        asyncExceptionDemoService.executeVoidTask("");
        return "void 异步任务已提交";
    }

    /**
     * 触发 CompletableFuture 异步异常
     *
     * @return 异步结果
     */
    @PostMapping("/future")
    public CompletableFuture<String> triggerFutureException() {
        return asyncExceptionDemoService.executeFutureTask("")
                .exceptionally(e -> {
                    log.error("CompletableFuture 异步任务异常", e);
                    return "异步任务执行失败";
                });
    }
}
```

测试命令：

```bash
curl -X POST "http://localhost:8080/async-exception/void"
curl -X POST "http://localhost:8080/async-exception/future"
```

`void` 方法异常应在 `AsyncConfig#getAsyncUncaughtExceptionHandler()` 中被记录；`CompletableFuture` 方法异常应由 Controller 中的 `exceptionally()` 处理并返回兜底结果。

### 异步方法调用限制

`@Async` 的核心限制来自 Spring 代理机制。只有通过 Spring 代理对象调用异步方法，异步增强才会生效。默认代理模式下，同一个类内部方法互相调用，也就是 self-invocation，不会经过代理，因此 `@Async` 不生效。Spring `@EnableAsync` 文档中也提供了 `mode`、`proxyTargetClass` 等代理相关配置，说明异步能力依赖代理增强。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/EnableAsync.html?utm_source=chatgpt.com))

常见限制如下：

| 限制点                   | 说明                                    | 推荐做法                                     |
| ------------------------ | --------------------------------------- | -------------------------------------------- |
| 同类内部调用不生效       | `this.asyncMethod()` 不经过 Spring 代理 | 把异步方法放到独立 Service                   |
| 非 Spring Bean 不生效    | 对象不是容器管理对象                    | 使用 `@Service`、`@Component` 管理           |
| 私有方法不适合           | 代理无法正常增强私有方法                | 使用 public 方法作为异步入口                 |
| 异常不能直接抛回原调用方 | 异步线程和请求线程不同                  | 使用 `CompletableFuture` 或异常处理器        |
| 事务边界不同             | 异步方法在新线程中执行                  | 明确事务范围，必要时异步方法内部重新开启事务 |

下面是一个错误示例。

文件位置：`src/main/java/io/github/atengk/concurrent/service/WrongAsyncCallService.java`

该代码演示同类内部调用 `@Async` 方法时异步不生效的问题。

```java
package io.github.atengk.concurrent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 错误的异步调用示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class WrongAsyncCallService {

    /**
     * 外部入口方法
     */
    public void submitTask() {
        log.info("准备调用异步方法，当前线程：{}", Thread.currentThread().getName());

        this.doAsyncTask();

        log.info("异步方法调用结束，当前线程：{}", Thread.currentThread().getName());
    }

    /**
     * 同类内部调用时，@Async 不会生效
     */
    @Async("bizAsyncExecutor")
    public void doAsyncTask() {
        log.info("执行异步任务，当前线程：{}", Thread.currentThread().getName());
    }
}
```

正确做法是把异步方法放到另一个 Spring Bean 中，通过 Bean 之间调用触发代理。

文件位置：`src/main/java/io/github/atengk/concurrent/service/RightAsyncTaskService.java`

该服务提供真正的异步任务入口。

```java
package io.github.atengk.concurrent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 正确的异步任务服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class RightAsyncTaskService {

    /**
     * 执行异步任务
     */
    @Async("bizAsyncExecutor")
    public void doAsyncTask() {
        log.info("执行异步任务，当前线程：{}", Thread.currentThread().getName());
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/service/RightAsyncCallService.java`

该服务通过注入另一个 Bean 的方式调用异步方法。

```java
package io.github.atengk.concurrent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 正确的异步调用服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RightAsyncCallService {

    private final RightAsyncTaskService rightAsyncTaskService;

    /**
     * 提交异步任务
     */
    public void submitTask() {
        log.info("准备提交异步任务，当前线程：{}", Thread.currentThread().getName());

        rightAsyncTaskService.doAsyncTask();

        log.info("异步任务提交完成，当前线程：{}", Thread.currentThread().getName());
    }
}
```

判断 `@Async` 是否生效，最直接的方法是观察日志中的线程名称。如果异步方法中的线程名称不是请求线程，而是类似 `biz-async-1`，说明异步执行器生效。

### 异步任务上下文传递

异步任务会切换线程，请求线程中的 `ThreadLocal`、MDC、登录用户、安全上下文、租户 ID、链路追踪 ID 等信息不会自动传递到异步线程。传统线程池会复用线程，如果上下文没有正确复制和清理，还可能造成上下文串号。Spring Boot 的异步任务执行器可以通过 `TaskDecorator` 装饰任务，在提交任务时捕获上下文，并在异步线程执行前设置、执行后清理。

Spring Boot 官方文档说明，自动配置的 `AsyncTaskExecutor` 会用于 `@EnableAsync` 异步任务执行；当自定义执行器时，应显式配置上下文传递、线程命名和关闭策略。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

下面示例使用 MDC 传递 `traceId`，便于异步任务日志和请求日志串联。

文件位置：`src/main/java/io/github/atengk/concurrent/config/MdcTaskDecorator.java`

该装饰器用于把提交任务线程中的 MDC 上下文复制到异步线程，并在任务结束后清理。

```java
package io.github.atengk.concurrent.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * MDC 上下文传递装饰器
 *
 * @author Ateng
 * @since 2026-05-08
 */
public class MdcTaskDecorator implements TaskDecorator {

    /**
     * 装饰异步任务
     *
     * @param runnable 原始任务
     * @return 包装后的任务
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/config/AsyncConfig.java`

在前面的异步线程池配置中增加 `TaskDecorator`。

```java
@Bean("bizAsyncExecutor")
public ThreadPoolTaskExecutor bizAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(8);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(200);
    executor.setKeepAliveSeconds(60);
    executor.setThreadNamePrefix("biz-async-");

    // 传递 MDC 上下文，保证异步日志能关联请求链路
    executor.setTaskDecorator(new MdcTaskDecorator());

    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);

    executor.initialize();
    return executor;
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/config/TraceIdFilter.java`

该过滤器用于给每个请求生成 `traceId` 并放入 MDC。

```java
package io.github.atengk.concurrent.config;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 请求链路 ID 过滤器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID = "traceId";

    /**
     * 过滤请求并设置 traceId
     *
     * @param request 请求
     * @param response 响应
     * @param chain 过滤器链
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        String traceId = httpServletRequest.getHeader(TRACE_ID);
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
        }

        try {
            MDC.put(TRACE_ID, traceId);
            log.info("请求开始，traceId：{}，uri：{}", traceId, httpServletRequest.getRequestURI());
            chain.doFilter(request, response);
        } finally {
            log.info("请求结束，traceId：{}", traceId);
            MDC.clear();
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/service/ContextAsyncService.java`

该服务用于验证异步线程中能否读取到 MDC 中的 `traceId`。

```java
package io.github.atengk.concurrent.service;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步上下文验证服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class ContextAsyncService {

    /**
     * 异步打印链路 ID
     */
    @Async("bizAsyncExecutor")
    public void printTraceId() {
        log.info("异步任务开始执行，traceId：{}，线程：{}", MDC.get("traceId"), Thread.currentThread().getName());
        ThreadUtil.sleep(500);
        log.info("异步任务执行完成，traceId：{}", MDC.get("traceId"));
    }
}
```

上下文传递的核心原则是“提交时复制，执行前设置，执行后清理”。不要只设置不清理，尤其是平台线程池会复用线程，未清理的 `ThreadLocal` 或 MDC 很容易污染下一次任务。虚拟线程减少了线程复用带来的上下文污染风险，但对于跨线程、跨服务、跨消息链路的上下文传递，仍然需要显式设计。



## 线程池设计与使用

线程池用于复用线程、限制并发、削峰填谷和统一管理异步任务。生产项目中不建议直接无限制创建线程，也不建议所有业务共用一个默认线程池。线程池设计应围绕任务类型、任务耗时、下游资源容量、队列堆积风险、拒绝策略和优雅停机展开。

### 线程池核心参数

`ThreadPoolExecutor` 是 JDK 线程池的核心实现。它通过 `corePoolSize`、`maximumPoolSize`、`keepAliveTime`、`workQueue`、`threadFactory`、`RejectedExecutionHandler` 等参数控制任务执行行为。JDK 文档说明，当运行线程数小于核心线程数时，新任务会优先创建新线程；当核心线程已满时，任务进入队列；当队列满且线程数小于最大线程数时，才会继续创建非核心线程；当线程和队列都达到上限时，触发拒绝策略。([Oracle 文档](https://docs.oracle.com/javase/jp/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html?utm_source=chatgpt.com))

| 参数              | 作用                   | 生产建议                                                     |
| ----------------- | ---------------------- | ------------------------------------------------------------ |
| `corePoolSize`    | 核心线程数             | 按任务类型设置，CPU 密集型接近 CPU 核心数，IO 密集型可适当放大 |
| `maximumPoolSize` | 最大线程数             | 必须设置上限，避免线程无限增长                               |
| `keepAliveTime`   | 非核心线程空闲存活时间 | IO 波动型任务可设置 30 到 120 秒                             |
| `workQueue`       | 任务等待队列           | 推荐有界队列，避免内存被任务堆积打满                         |
| `threadFactory`   | 线程创建工厂           | 必须设置业务化线程名前缀，方便日志和排查                     |
| `handler`         | 拒绝策略               | 核心业务建议自定义记录日志，必要时降级或回退                 |

线程池配置不是越大越好。线程数过小会导致任务排队，接口响应变慢；线程数过大可能导致 CPU 上下文切换增加、数据库连接池耗尽、HTTP 连接池耗尽、Redis 被打满或下游接口雪崩。队列也不是越大越好，过大的队列会掩盖系统过载，最终表现为延迟越来越高、任务越来越旧、内存越来越高。

常用估算方式如下：

```text
CPU 密集型线程数 ≈ CPU 核心数 或 CPU 核心数 + 1

IO 密集型线程数 ≈ CPU 核心数 × (1 + IO 等待时间 / CPU 计算时间)
```

这个公式只能作为初始估算，最终仍要通过压测、监控和生产数据调整。实际项目中还要结合数据库连接池、HTTP 连接池、MQ 消费能力、Redis QPS、第三方接口限流等外部约束。

### ThreadPoolTaskExecutor

`ThreadPoolTaskExecutor` 是 Spring 对 `ThreadPoolExecutor` 的封装，适合在 Spring Boot 项目中作为 `@Async`、异步任务和业务线程池使用。Spring 官方文档说明，它支持通过 Bean 风格配置 `corePoolSize`、`maxPoolSize`、`keepAliveSeconds`、`queueCapacity`，并且适合管理和监控，可查看 `poolSize`、`activeCount` 等运行状态。它的默认配置是核心线程数 1、最大线程数不受限、队列容量不受限，近似于单线程执行器，因此生产中必须显式配置。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/concurrent/ThreadPoolTaskExecutor.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/config/BusinessThreadPoolConfig.java`

该配置类提供 IO 任务线程池和 CPU 任务线程池，适合直接在 Spring Boot 3 项目中使用。

```java
package io.github.atengk.concurrent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 业务线程池配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
public class BusinessThreadPoolConfig {

    /**
     * IO 密集型任务线程池
     *
     * @return IO 任务线程池
     */
    @Bean("ioTaskExecutor")
    public ThreadPoolTaskExecutor ioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(64);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("io-task-");

        // 队列和线程都满时，由提交任务的线程执行，形成反压
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 应用关闭时等待队列任务执行完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        log.info("IO 密集型线程池初始化完成");
        return executor;
    }

    /**
     * CPU 密集型任务线程池
     *
     * @return CPU 任务线程池
     */
    @Bean("cpuTaskExecutor")
    public ThreadPoolTaskExecutor cpuTaskExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(processors);
        executor.setMaxPoolSize(processors + 1);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("cpu-task-");

        // CPU 任务不宜无限堆积，满载时直接拒绝，便于上层快速失败或降级
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        log.info("CPU 密集型线程池初始化完成，核心线程数：{}", processors);
        return executor;
    }
}
```

使用时在 `@Async` 上指定线程池名称即可。

文件位置：`src/main/java/io/github/atengk/concurrent/service/BusinessAsyncService.java`

该服务演示 IO 任务和 CPU 任务使用不同线程池隔离执行。

```java
package io.github.atengk.concurrent.service;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

/**
 * 业务异步任务服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class BusinessAsyncService {

    /**
     * 执行 IO 密集型任务
     *
     * @param orderNo 订单号
     * @return 执行结果
     */
    @Async("ioTaskExecutor")
    public CompletableFuture<String> executeIoTask(String orderNo) {
        log.info("开始执行 IO 任务，订单号：{}", orderNo);
        ThreadUtil.sleep(1000);
        return CompletableFuture.completedFuture("IO 任务完成：" + orderNo);
    }

    /**
     * 执行 CPU 密集型任务
     *
     * @param count 计算次数
     * @return 计算结果
     */
    @Async("cpuTaskExecutor")
    public CompletableFuture<BigInteger> executeCpuTask(int count) {
        log.info("开始执行 CPU 任务，计算次数：{}", count);

        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < count; i++) {
            result = result.add(BigInteger.valueOf(i).multiply(BigInteger.valueOf(i)));
        }

        log.info("CPU 任务执行完成，计算次数：{}", count);
        return CompletableFuture.completedFuture(result);
    }
}
```

`ThreadPoolTaskExecutor` 的优势是和 Spring 生命周期、Bean 管理、`@Async`、监控体系更容易整合。普通业务项目中，优先使用它，而不是在每个 Service 中手动创建 `ThreadPoolExecutor`。

### ThreadPoolExecutor

`ThreadPoolExecutor` 是 JDK 原生线程池，适合在非 Spring 环境、底层工具类、独立 SDK 或对线程池行为有更精细控制的场景使用。它提供了 `beforeExecute`、`afterExecute`、`terminated` 等钩子方法，可用于统计耗时、记录异常、清理上下文。JDK 文档也说明，`ThreadPoolExecutor` 提供这些可覆盖的钩子方法，可用于重新初始化 `ThreadLocal`、收集统计信息或添加日志。([Oracle 文档](https://docs.oracle.com/javase/jp/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/pool/MonitorThreadPoolExecutor.java`

该线程池扩展类用于统计任务执行耗时，并记录任务异常。

```java
package io.github.atengk.concurrent.pool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 带监控日志的线程池
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class MonitorThreadPoolExecutor extends ThreadPoolExecutor {

    private final ThreadLocal<Long> startTimeHolder = new ThreadLocal<>();

    /**
     * 创建线程池
     *
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveTime 空闲存活时间
     * @param unit 时间单位
     * @param workQueue 工作队列
     * @param threadFactory 线程工厂
     * @param handler 拒绝策略
     */
    public MonitorThreadPoolExecutor(int corePoolSize,
                                     int maximumPoolSize,
                                     long keepAliveTime,
                                     TimeUnit unit,
                                     BlockingQueue<Runnable> workQueue,
                                     ThreadFactory threadFactory,
                                     RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * 任务执行前记录开始时间
     *
     * @param thread 当前线程
     * @param runnable 当前任务
     */
    @Override
    protected void beforeExecute(Thread thread, Runnable runnable) {
        startTimeHolder.set(System.currentTimeMillis());
        log.debug("线程池任务开始执行，线程：{}", thread.getName());
        super.beforeExecute(thread, runnable);
    }

    /**
     * 任务执行后统计耗时
     *
     * @param runnable 当前任务
     * @param throwable 异常信息
     */
    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        try {
            Long startTime = startTimeHolder.get();
            long cost = startTime == null ? -1 : System.currentTimeMillis() - startTime;

            if (throwable != null) {
                log.error("线程池任务执行异常，耗时：{}ms", cost, throwable);
            } else {
                log.info("线程池任务执行完成，耗时：{}ms", cost);
            }
        } finally {
            startTimeHolder.remove();
            super.afterExecute(runnable, throwable);
        }
    }

    /**
     * 线程池终止后记录日志
     */
    @Override
    protected void terminated() {
        log.info("线程池已终止，完成任务数：{}", getCompletedTaskCount());
        super.terminated();
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/pool/ThreadPoolFactory.java`

该工厂类用于创建带名称、队列、拒绝策略和监控日志的 JDK 原生线程池。

```java
package io.github.atengk.concurrent.pool;

import cn.hutool.core.thread.ThreadFactoryBuilder;

import java.util.concurrent.*;

/**
 * JDK 线程池工厂
 *
 * @author Ateng
 * @since 2026-05-08
 */
public final class ThreadPoolFactory {

    private ThreadPoolFactory() {
    }

    /**
     * 创建 IO 任务线程池
     *
     * @return IO 任务线程池
     */
    public static ExecutorService createIoExecutor() {
        ThreadFactory threadFactory = ThreadFactoryBuilder.create()
                .setNamePrefix("jdk-io-task-")
                .setUncaughtExceptionHandler((thread, throwable) ->
                        System.err.printf("线程执行异常，线程：%s，异常：%s%n", thread.getName(), throwable.getMessage()))
                .build();

        return new MonitorThreadPoolExecutor(
                16,
                64,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

JDK 原生线程池必须手动关闭。长期运行的 Spring Bean 可以交给 Spring 管理；普通工具类或临时任务应使用 `try-finally` 或 `try-with-resources`。JDK 21 中 `ExecutorService` 继承 `AutoCloseable`，可以配合 `try-with-resources` 使用。

### 任务队列选择

任务队列决定线程池在核心线程满之后如何缓存任务。队列选型会直接影响线程增长、延迟、内存和拒绝行为。JDK 文档指出，无界队列在任务到达速度长期超过处理速度时可能不断增长；有界队列可以避免资源耗尽，但队列大小和最大线程数之间需要权衡。([Oracle 文档](https://docs.oracle.com/javase/jp/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html?utm_source=chatgpt.com))

| 队列                    | 特点                 | 适用场景             | 风险                 |
| ----------------------- | -------------------- | -------------------- | -------------------- |
| `ArrayBlockingQueue`    | 有界数组队列         | 固定容量、强控制     | 容量过小容易拒绝     |
| `LinkedBlockingQueue`   | 可有界也可无界       | 通用业务线程池       | 无界时可能 OOM       |
| `SynchronousQueue`      | 不存储任务，直接交付 | 短任务、高弹性线程池 | 容易创建大量线程     |
| `PriorityBlockingQueue` | 优先级队列           | 任务有优先级         | 默认无界，需额外限制 |
| `DelayQueue`            | 延迟队列             | 延迟任务             | 不适合普通业务线程池 |

生产环境通常建议使用有界队列，例如 `LinkedBlockingQueue<>(500)` 或 `ArrayBlockingQueue<>(500)`。不要使用无界队列承接不可控流量，因为它会把问题从“请求被拒绝”变成“系统延迟升高和内存耗尽”。

队列容量设置可以参考任务平均耗时和可接受延迟。例如每秒处理 100 个任务，平均耗时 200ms，如果允许最多排队 5 秒，则队列容量可以初步设置为 `100 × 5 = 500`。这只是估算值，必须结合压测和监控调整。

### 拒绝策略

拒绝策略用于处理线程数达到最大值且队列已满时的新任务。JDK 内置了 4 种常见策略：`AbortPolicy` 抛出 `RejectedExecutionException`，`CallerRunsPolicy` 由提交任务的线程执行，`DiscardPolicy` 静默丢弃任务，`DiscardOldestPolicy` 丢弃队列中最旧任务后重试。([Oracle 文档](https://docs.oracle.com/javase/jp/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html?utm_source=chatgpt.com))

| 策略                  | 行为               | 适合场景                       |
| --------------------- | ------------------ | ------------------------------ |
| `AbortPolicy`         | 直接抛异常         | 核心任务，要求调用方感知失败   |
| `CallerRunsPolicy`    | 调用方线程执行任务 | 需要反压，不希望直接丢任务     |
| `DiscardPolicy`       | 静默丢弃           | 极少数允许丢弃的非关键任务     |
| `DiscardOldestPolicy` | 丢弃最旧任务       | 一般不推荐，容易破坏任务完整性 |

关键业务不建议使用静默丢弃策略。异步发送消息、生成报表、同步数据等任务即使失败，也应记录任务编号、业务参数和失败原因，便于补偿。

文件位置：`src/main/java/io/github/atengk/concurrent/pool/LogRejectedExecutionHandler.java`

该拒绝策略用于记录线程池满载时的关键运行状态，并抛出异常通知调用方。

```java
package io.github.atengk.concurrent.pool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 记录日志的拒绝策略
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class LogRejectedExecutionHandler implements RejectedExecutionHandler {

    /**
     * 处理被拒绝的任务
     *
     * @param runnable 被拒绝的任务
     * @param executor 线程池
     */
    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        log.error("线程池任务被拒绝，poolSize：{}，activeCount：{}，queueSize：{}，completedTaskCount：{}",
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount());

        throw new RejectedTaskException("线程池任务已满，请稍后重试");
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/pool/RejectedTaskException.java`

该异常用于向上层明确表达任务被拒绝。

```java
package io.github.atengk.concurrent.pool;

/**
 * 线程池拒绝任务异常
 *
 * @author Ateng
 * @since 2026-05-08
 */
public class RejectedTaskException extends RuntimeException {

    /**
     * 创建线程池拒绝任务异常
     *
     * @param message 异常消息
     */
    public RejectedTaskException(String message) {
        super(message);
    }
}
```

### 线程命名与线程工厂

线程命名是并发问题排查的基础。没有业务含义的线程名会严重影响日志定位、线程 dump 分析、监控聚合和告警识别。线程名前缀应能表达业务域、任务类型和线程池用途，例如 `order-notify-`、`report-calc-`、`file-parse-`、`remote-call-`。

文件位置：`src/main/java/io/github/atengk/concurrent/pool/NamedThreadFactoryDemo.java`

该示例演示使用 Hutool 创建带业务名称的线程工厂。

```java
package io.github.atengk.concurrent.pool;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;

/**
 * 线程命名与线程工厂示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class NamedThreadFactoryDemo {

    /**
     * 创建业务线程工厂
     *
     * @return 线程工厂
     */
    public ThreadFactory createThreadFactory() {
        return ThreadFactoryBuilder.create()
                .setNamePrefix("order-notify-")
                .setDaemon(false)
                .setUncaughtExceptionHandler((thread, throwable) ->
                        log.error("线程发生未捕获异常，线程：{}", thread.getName(), throwable))
                .build();
    }

    public static void main(String[] args) {
        NamedThreadFactoryDemo demo = new NamedThreadFactoryDemo();

        Thread thread = demo.createThreadFactory().newThread(() -> {
            log.info("业务线程开始执行");
            ThreadUtil.sleep(500);
            log.info("业务线程执行完成");
        });

        thread.start();
    }
}
```

线程命名建议统一规范：

```text
业务域-任务类型-序号

例如：
order-notify-1
report-calc-1
file-parse-1
remote-call-1
```

不要使用 `pool-1-thread-1` 这类默认名称作为生产线程池名称。发生 CPU 飙高、死锁、线程泄漏或队列堆积时，线程名通常是第一定位入口。

### 线程池隔离设计

线程池隔离是指不同业务、不同任务类型、不同资源依赖使用独立线程池，避免互相拖垮。例如短信通知慢，不应影响订单支付；报表计算耗 CPU，不应影响文件上传；第三方接口超时，不应占满所有异步线程。

常见隔离方式如下：

| 线程池                | 任务类型             | 设计重点                              |
| --------------------- | -------------------- | ------------------------------------- |
| `orderNotifyExecutor` | 订单通知、短信、邮件 | IO 线程数适当放大，必须设置超时和重试 |
| `reportExecutor`      | 报表、统计、导出     | CPU 线程数较小，队列有限              |
| `fileExecutor`        | 文件解析、导入       | 控制队列和单文件处理耗时              |
| `remoteCallExecutor`  | 第三方接口调用       | 强制超时、限流、熔断                  |
| `logExecutor`         | 操作日志、审计日志   | 可以低优先级，失败可补偿              |

线程池隔离不是线程池越多越好。线程池过多会导致配置复杂、线程总数失控、监控困难。建议按“资源瓶颈”和“业务重要性”隔离，而不是每个方法一个线程池。

文件位置：`src/main/java/io/github/atengk/concurrent/config/IsolatedThreadPoolConfig.java`

该配置类演示订单通知、报表计算、远程调用三类线程池隔离。

```java
package io.github.atengk.concurrent.config;

import io.github.atengk.concurrent.pool.LogRejectedExecutionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 线程池隔离配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Configuration
public class IsolatedThreadPoolConfig {

    /**
     * 订单通知线程池
     *
     * @return 订单通知线程池
     */
    @Bean("orderNotifyExecutor")
    public ThreadPoolTaskExecutor orderNotifyExecutor() {
        return buildExecutor("order-notify-", 8, 32, 300);
    }

    /**
     * 报表计算线程池
     *
     * @return 报表计算线程池
     */
    @Bean("reportCalcExecutor")
    public ThreadPoolTaskExecutor reportCalcExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        return buildExecutor("report-calc-", processors, processors + 1, 50);
    }

    /**
     * 远程调用线程池
     *
     * @return 远程调用线程池
     */
    @Bean("remoteCallExecutor")
    public ThreadPoolTaskExecutor remoteCallExecutor() {
        return buildExecutor("remote-call-", 16, 64, 200);
    }

    /**
     * 构建线程池
     *
     * @param threadNamePrefix 线程名前缀
     * @param corePoolSize 核心线程数
     * @param maxPoolSize 最大线程数
     * @param queueCapacity 队列容量
     * @return 线程池
     */
    private ThreadPoolTaskExecutor buildExecutor(String threadNamePrefix,
                                                 int corePoolSize,
                                                 int maxPoolSize,
                                                 int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new LogRejectedExecutionHandler());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
```

隔离后的调用示例：

```java
@Async("orderNotifyExecutor")
public void sendOrderNotify(String orderNo) {
    log.info("异步发送订单通知，订单号：{}", orderNo);
}

@Async("reportCalcExecutor")
public CompletableFuture<String> calculateReport(String reportNo) {
    log.info("异步计算报表，报表编号：{}", reportNo);
    return CompletableFuture.completedFuture("报表计算完成：" + reportNo);
}

@Async("remoteCallExecutor")
public CompletableFuture<String> callRemoteApi(String requestNo) {
    log.info("异步调用远程接口，请求编号：{}", requestNo);
    return CompletableFuture.completedFuture("远程接口调用完成：" + requestNo);
}
```

### 线程池关闭与优雅停机

线程池关闭分为平滑关闭和立即关闭。`shutdown()` 表示不再接收新任务，但会继续执行已提交任务；`shutdownNow()` 会尝试中断正在执行的任务，并返回等待队列中尚未执行的任务。JDK 文档说明，`shutdown()` 不会等待任务执行完成，如果需要等待，应配合 `awaitTermination()` 使用。([Oracle 文档](https://docs.oracle.com/javase/jp/21/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html?utm_source=chatgpt.com))

Spring Boot 中使用 `ThreadPoolTaskExecutor` 时，可以设置：

```java
executor.setWaitForTasksToCompleteOnShutdown(true);
executor.setAwaitTerminationSeconds(30);
```

这表示应用关闭时等待已提交任务完成，最多等待 30 秒。适合通知、入库、文件处理等不希望中途被强制中断的任务。

文件位置：`src/main/java/io/github/atengk/concurrent/pool/ExecutorShutdownUtil.java`

该工具类用于优雅关闭 JDK 原生线程池。

```java
package io.github.atengk.concurrent.pool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 线程池关闭工具
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public final class ExecutorShutdownUtil {

    private ExecutorShutdownUtil() {
    }

    /**
     * 优雅关闭线程池
     *
     * @param executorService 线程池
     * @param timeoutSeconds 等待超时时间
     */
    public static void shutdownGracefully(ExecutorService executorService, long timeoutSeconds) {
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("线程池未在指定时间内关闭，准备强制关闭，等待时间：{} 秒", timeoutSeconds);
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
            log.error("等待线程池关闭时被中断，已触发强制关闭", e);
        }
    }
}
```

优雅停机的前提是任务本身支持中断。任务内部如果存在循环处理，应定期检查 `Thread.currentThread().isInterrupted()`；如果调用外部接口，应设置连接超时和读取超时；如果执行数据库任务，应避免超长事务。

## 虚拟线程

虚拟线程是 JDK 21 的正式特性，目标是在保持同步阻塞代码风格的同时，显著降低大量并发阻塞任务的线程成本。它适合大量 IO 阻塞场景，但不是 CPU 性能加速器，也不能替代限流、连接池、超时控制和资源隔离。

### 虚拟线程基本概念

虚拟线程是由 JVM 调度的轻量级线程，而不是由操作系统直接调度的平台线程。JDK 21 的 `Thread#isVirtual()` 文档说明，虚拟线程由 Java 虚拟机调度，而不是由操作系统调度。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html?utm_source=chatgpt.com))

虚拟线程的主要价值是降低“线程等待”的成本。传统平台线程在等待数据库、HTTP、Redis、文件 IO 时会占用昂贵的操作系统线程；虚拟线程在很多阻塞点可以从承载它的平台线程上卸载，使平台线程继续执行其他虚拟线程。这样就可以用同步阻塞写法处理更高并发的 IO 任务。

需要明确的是，虚拟线程不是非阻塞 IO。代码仍然可以是阻塞式写法，例如 JDBC 查询、HTTP 调用、文件读取。虚拟线程降低的是线程资源成本，不会减少数据库连接、HTTP 连接、Redis 连接、磁盘 IO、第三方接口限流等外部资源消耗。

### 平台线程与虚拟线程对比

平台线程适合需要长期运行、数量有限、与操作系统线程强绑定的任务。虚拟线程适合数量很多、生命周期短、经常阻塞等待 IO 的任务。

| 对比项           | 平台线程               | 虚拟线程              |
| ---------------- | ---------------------- | --------------------- |
| 调度者           | 操作系统               | JVM                   |
| 创建成本         | 高                     | 低                    |
| 推荐数量         | 有限                   | 可大量创建            |
| 适合场景         | CPU 任务、长期后台任务 | 大量阻塞 IO 任务      |
| 是否需要池化     | 通常需要线程池         | 通常不需要池化        |
| ThreadLocal 使用 | 可用但需清理           | 可用但不建议滥用      |
| CPU 密集型提升   | 依赖 CPU 核心数        | 不会突破 CPU 核心上限 |

OpenJDK 的 JEP 444 明确建议不要池化虚拟线程。线程池是为了复用昂贵资源，而虚拟线程并不昂贵；如果目的是限制并发访问某个受限资源，应使用 `Semaphore` 等并发控制结构，而不是把虚拟线程池化。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

### JDK 21 虚拟线程 API

JDK 21 提供了多种创建虚拟线程的方式。可以使用 `Thread.startVirtualThread()`，也可以使用 `Thread.ofVirtual()` 构建线程，还可以使用 `Executors.newVirtualThreadPerTaskExecutor()` 为每个任务创建一个新的虚拟线程。Oracle JDK 21 文档说明，`newVirtualThreadPerTaskExecutor()` 会为每个任务启动新的虚拟线程，并且创建的线程数量不受固定线程池大小限制。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Executors.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/virtual/VirtualThreadDemo.java`

该示例演示 JDK 21 创建虚拟线程的三种常见方式。

```java
package io.github.atengk.concurrent.virtual;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JDK 21 虚拟线程示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class VirtualThreadDemo {

    /**
     * 使用 Thread.startVirtualThread 创建虚拟线程
     */
    public void startVirtualThread() {
        Thread thread = Thread.startVirtualThread(() -> {
            log.info("startVirtualThread 执行，是否虚拟线程：{}", Thread.currentThread().isVirtual());
            ThreadUtil.sleep(500);
        });

        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待虚拟线程结束时被中断", e);
        }
    }

    /**
     * 使用 Thread.ofVirtual 创建虚拟线程
     */
    public void createByBuilder() {
        Thread thread = Thread.ofVirtual()
                .name("virtual-demo-", 1)
                .start(() -> {
                    log.info("Thread.ofVirtual 执行，线程名：{}", Thread.currentThread().getName());
                    ThreadUtil.sleep(500);
                });

        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待虚拟线程结束时被中断", e);
        }
    }

    /**
     * 使用虚拟线程执行器执行任务
     */
    public void executeByVirtualExecutor() {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 5; i++) {
                int taskNo = i;
                executorService.submit(() -> {
                    log.info("虚拟线程任务执行，任务编号：{}，线程：{}", taskNo, Thread.currentThread());
                    ThreadUtil.sleep(500);
                    return taskNo;
                });
            }
        }
    }

    public static void main(String[] args) {
        VirtualThreadDemo demo = new VirtualThreadDemo();
        demo.startVirtualThread();
        demo.createByBuilder();
        demo.executeByVirtualExecutor();
    }
}
```

`Executors.newVirtualThreadPerTaskExecutor()` 适合“一任务一虚拟线程”的模型。不要把它理解为传统线程池，也不要用固定大小虚拟线程池来限制并发。需要限制并发时，应使用 `Semaphore`。

文件位置：`src/main/java/io/github/atengk/concurrent/virtual/VirtualThreadLimitDemo.java`

该示例演示使用虚拟线程处理大量任务，同时用 `Semaphore` 限制对下游资源的并发访问。

```java
package io.github.atengk.concurrent.virtual;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 虚拟线程并发限制示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class VirtualThreadLimitDemo {

    private final Semaphore remoteApiSemaphore = new Semaphore(20);

    /**
     * 批量调用远程接口
     */
    public void batchCallRemoteApi() {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 100; i++) {
                int requestNo = i;
                executorService.submit(() -> callRemoteApi(requestNo));
            }
        }
    }

    /**
     * 调用远程接口
     *
     * @param requestNo 请求编号
     */
    private void callRemoteApi(int requestNo) {
        boolean acquired = false;

        try {
            remoteApiSemaphore.acquire();
            acquired = true;

            log.info("开始调用远程接口，请求编号：{}", requestNo);
            ThreadUtil.sleep(500);
            log.info("远程接口调用完成，请求编号：{}", requestNo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("远程接口调用被中断，请求编号：{}", requestNo, e);
        } finally {
            if (acquired) {
                remoteApiSemaphore.release();
            }
        }
    }

    public static void main(String[] args) {
        VirtualThreadLimitDemo demo = new VirtualThreadLimitDemo();
        demo.batchCallRemoteApi();
    }
}
```

### Spring Boot 3 虚拟线程配置

Spring Boot 3 在 Java 21+ 环境下支持通过配置启用虚拟线程。Spring Boot 官方文档说明，在没有自定义 `Executor` Bean 的情况下，Spring Boot 会自动配置 `AsyncTaskExecutor`；当 `spring.threads.virtual.enabled=true` 且运行在 Java 21+ 时，该执行器会是使用虚拟线程的 `SimpleAsyncTaskExecutor`。同时，虚拟线程启用后，调度器会使用基于虚拟线程的 `SimpleAsyncTaskScheduler`，并且会忽略池化相关属性。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

该配置用于在 Spring Boot 3 中启用虚拟线程，并配置普通任务执行的前缀属性。

```yaml
spring:
  threads:
    virtual:
      # Java 21+ 环境下启用虚拟线程
      enabled: true

  task:
    execution:
      thread-name-prefix: "app-task-"
      pool:
        # 未启用虚拟线程时生效；启用虚拟线程后，默认执行器不再按传统线程池池化
        max-size: 16
        queue-capacity: 100
        keep-alive: 10s
```

需要注意：如果你自己定义了 `Executor`、`AsyncConfigurer` 或指定了 `@Async("xxxExecutor")`，就会覆盖或绕过 Spring Boot 自动配置的默认执行器。此时是否使用虚拟线程，取决于你自定义的执行器。

文件位置：`src/main/java/io/github/atengk/concurrent/config/VirtualThreadExecutorConfig.java`

该配置类显式提供一个虚拟线程执行器，适合给指定业务方法使用。

```java
package io.github.atengk.concurrent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 虚拟线程执行器配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Configuration
public class VirtualThreadExecutorConfig {

    /**
     * 创建虚拟线程执行器
     *
     * @return 虚拟线程执行器
     */
    @Bean("virtualTaskExecutor")
    public ExecutorService virtualTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/service/VirtualAsyncService.java`

该服务演示在 Spring `@Async` 中显式使用虚拟线程执行器。

```java
package io.github.atengk.concurrent.service;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 虚拟线程异步服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class VirtualAsyncService {

    /**
     * 使用虚拟线程执行阻塞式 IO 任务
     *
     * @param requestNo 请求编号
     * @return 执行结果
     */
    @Async("virtualTaskExecutor")
    public CompletableFuture<String> executeBlockingIo(String requestNo) {
        log.info("虚拟线程开始执行阻塞式 IO 任务，请求编号：{}，是否虚拟线程：{}",
                requestNo,
                Thread.currentThread().isVirtual());

        ThreadUtil.sleep(1000);

        log.info("虚拟线程阻塞式 IO 任务执行完成，请求编号：{}", requestNo);
        return CompletableFuture.completedFuture("执行成功：" + requestNo);
    }
}
```

### 虚拟线程适用场景

虚拟线程最适合“大量并发 + 阻塞等待 + 任务生命周期较短”的场景。典型场景包括：

| 场景                | 是否适合             | 原因                                     |
| ------------------- | -------------------- | ---------------------------------------- |
| 大量 HTTP 接口调用  | 适合                 | 大量时间在等待网络 IO                    |
| 数据库查询          | 适合，但受连接池限制 | 虚拟线程降低线程等待成本，但连接数仍受限 |
| Redis 请求          | 适合，但需限流       | 等待网络 IO，仍需控制 Redis 压力         |
| 文件读写            | 适合部分场景         | 取决于文件系统和磁盘 IO 能力             |
| Spring MVC 阻塞接口 | 适合                 | 可保持同步写法，提高阻塞场景并发能力     |
| 批量远程同步        | 适合                 | 配合 `Semaphore`、超时和重试效果更好     |

虚拟线程让“同步代码风格 + 高并发阻塞 IO”变得更可行。原来需要复杂响应式链路或大量平台线程才能支撑的场景，现在可以优先评估虚拟线程。但这不代表可以取消超时、限流和连接池配置。

文件位置：`src/main/java/io/github/atengk/concurrent/service/VirtualRemoteCallService.java`

该服务演示虚拟线程适合的批量远程调用模型。

```java
package io.github.atengk.concurrent.service;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * 虚拟线程远程调用服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class VirtualRemoteCallService {

    private final Semaphore semaphore = new Semaphore(20);

    /**
     * 批量查询远程数据
     *
     * @return 查询结果
     */
    public List<String> batchQueryRemoteData() {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = IntStream.rangeClosed(1, 100)
                    .mapToObj(index -> executorService.submit(() -> queryRemoteData(index)))
                    .toList();

            return futures.stream()
                    .map(this::getResult)
                    .toList();
        }
    }

    /**
     * 查询远程数据
     *
     * @param index 请求序号
     * @return 查询结果
     */
    private String queryRemoteData(int index) {
        boolean acquired = false;

        try {
            semaphore.acquire();
            acquired = true;

            log.info("开始查询远程数据，序号：{}", index);
            ThreadUtil.sleep(300);
            return "DATA-" + index;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("查询远程数据被中断，序号：{}", index, e);
            return "INTERRUPTED-" + index;
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }

    /**
     * 获取异步结果
     *
     * @param future 异步结果
     * @return 查询结果
     */
    private String getResult(Future<String> future) {
        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取远程数据结果时被中断", e);
            return "INTERRUPTED";
        } catch (ExecutionException e) {
            log.error("远程数据任务执行异常", e);
            return "ERROR";
        } catch (TimeoutException e) {
            log.error("获取远程数据结果超时", e);
            return "TIMEOUT";
        }
    }
}
```

### 虚拟线程不适用场景

虚拟线程不适合所有并发问题。它不会让 CPU 计算变快，也不能让数据库、Redis、MQ、HTTP 下游承载无限请求。它解决的是线程等待成本，不是所有资源瓶颈。

不适合场景包括：

| 场景                            | 原因                                             |
| ------------------------------- | ------------------------------------------------ |
| CPU 密集型计算                  | CPU 核心数固定，虚拟线程不能突破硬件上限         |
| 大量无节制访问数据库            | 连接池仍然有限，可能造成连接等待和数据库压力     |
| 需要严格控制并发的下游接口      | 应使用 `Semaphore`、限流器或舱壁隔离             |
| 长生命周期后台任务              | 平台线程或专用调度线程池更清晰                   |
| 依赖大量 ThreadLocal 缓存重对象 | 虚拟线程数量大，ThreadLocal 滥用可能放大内存压力 |
| 需要精确线程池队列语义          | 虚拟线程执行器是一任务一线程，不适合用队列削峰   |

JEP 444 明确指出，不应为了限制并发而池化虚拟线程；限制并发应使用专门的并发控制结构，例如信号量。([OpenJDK](https://openjdk.org/jeps/444?utm_source=chatgpt.com))

### 虚拟线程与线程池的关系

虚拟线程和传统线程池不是简单替代关系。传统线程池用于复用昂贵的平台线程，并通过线程数和队列控制任务执行；虚拟线程通常采用“一任务一虚拟线程”，不需要通过池化复用线程。JDK 21 的 `newVirtualThreadPerTaskExecutor()` 就是为每个任务创建新的虚拟线程。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Executors.html?utm_source=chatgpt.com))

可以按以下原则选择：

| 场景                          | 推荐方案                                                     |
| ----------------------------- | ------------------------------------------------------------ |
| CPU 密集型计算                | 固定大小平台线程池                                           |
| 普通 Spring `@Async` 业务任务 | `ThreadPoolTaskExecutor`                                     |
| 大量阻塞式 IO 请求            | 虚拟线程                                                     |
| 需要队列削峰                  | 平台线程池 + 有界队列                                        |
| 需要限制下游并发              | 虚拟线程 + `Semaphore` 或平台线程池隔离                      |
| 需要定时任务                  | `ThreadPoolTaskScheduler`、`ScheduledExecutorService` 或任务调度框架 |

一个比较稳妥的实践是：保留传统线程池处理 CPU 密集型任务、关键异步任务和需要队列控制的任务；使用虚拟线程处理大量短生命周期阻塞 IO 任务；对数据库、Redis、HTTP、MQ 等外部资源继续使用连接池、超时、限流和熔断控制。

在 Spring Boot 3 项目中可以形成这样的组合：

```text
CPU 计算任务       -> cpuTaskExecutor
订单通知任务      -> orderNotifyExecutor
报表导出任务      -> reportCalcExecutor
大量 HTTP 查询    -> virtualTaskExecutor + Semaphore
默认 @Async       -> 明确指定线程池，不依赖隐式默认值
```

最终原则是：线程池解决“平台线程复用和任务排队”，虚拟线程解决“大量阻塞等待时的平台线程占用”。二者可以共存，但不要混用职责。



## 线程安全

线程安全关注的是多个线程同时访问共享数据时，程序结果是否仍然正确、稳定、可预期。并发编程中常见问题包括共享变量被覆盖、读取到旧值、指令重排序导致状态异常、锁使用不当导致死锁、`ThreadLocal` 未清理导致上下文污染等。解决线程安全问题不能只依赖某一个关键字，应根据共享数据类型、读写频率、业务一致性要求和性能要求选择合适方案。

### 共享变量与竞态条件

共享变量是指多个线程可以同时访问或修改的变量。竞态条件是指程序执行结果依赖多个线程的执行时序，当线程调度顺序变化时，结果可能不同。最典型的问题是多个线程同时执行 `count++`，这个操作看起来是一行代码，实际包含读取、加一、写回三个步骤，并不是天然原子操作。

文件位置：`src/main/java/io/github/atengk/concurrent/safety/RaceConditionDemo.java`

该代码演示普通共享变量在多线程累加时会出现结果不准确的问题，并给出 `AtomicInteger` 的修复方式。

```java
package io.github.atengk.concurrent.safety;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 共享变量与竞态条件示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class RaceConditionDemo {

    private int unsafeCount = 0;

    private final AtomicInteger safeCount = new AtomicInteger(0);

    /**
     * 非线程安全累加
     */
    public void incrementUnsafe() {
        unsafeCount++;
    }

    /**
     * 线程安全累加
     */
    public void incrementSafe() {
        safeCount.incrementAndGet();
    }

    /**
     * 执行并发累加测试
     */
    public void runTest() {
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Thread thread = Thread.ofPlatform().name("race-worker-", i).start(() -> {
                for (int j = 0; j < 1000; j++) {
                    incrementUnsafe();
                    incrementSafe();
                }
            });
            threads.add(thread);
        }

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待线程执行完成时被中断", e);
            }
        });

        log.info("非线程安全计数结果：{}", unsafeCount);
        log.info("线程安全计数结果：{}", safeCount.get());
    }

    public static void main(String[] args) {
        RaceConditionDemo demo = new RaceConditionDemo();
        demo.runTest();

        ThreadUtil.sleep(500);
    }
}
```

共享变量不一定都有问题。只读共享变量、不可变对象、线程封闭变量通常是安全的。真正危险的是“多个线程同时写”或“一个线程写、多个线程读，并且没有同步机制”的变量。

### 原子性、可见性与有序性

线程安全通常从三个角度分析：原子性、可见性和有序性。原子性表示一个操作不可被中断；可见性表示一个线程修改共享变量后，其他线程能及时看到结果；有序性表示程序执行顺序不会因为编译器优化或 CPU 重排序破坏业务语义。

| 问题   | 含义                             | 常见表现                     | 解决方式                              |
| ------ | -------------------------------- | ---------------------------- | ------------------------------------- |
| 原子性 | 操作要么全部完成，要么完全不执行 | `count++` 丢失更新           | `synchronized`、`Lock`、Atomic 原子类 |
| 可见性 | 一个线程修改后，其他线程能看到   | 线程无法感知停止标记变化     | `volatile`、锁、并发容器              |
| 有序性 | 执行顺序符合并发语义             | 初始化对象被读取到半完成状态 | `volatile`、锁、安全发布              |

实际开发中，不要孤立理解这三个概念。`synchronized` 和 `Lock` 通常同时解决原子性、可见性和有序性；`volatile` 主要解决可见性和一定程度的有序性，但不解决复合操作的原子性；Atomic 原子类适合单变量原子更新，`AtomicInteger` 官方文档也说明它用于可原子更新的 `int` 值，常用于原子递增计数器。([Oracle 文档](https://docs.oracle.com/javase/jp/21/docs/api/java.base/java/util/concurrent/atomic/AtomicInteger.html?utm_source=chatgpt.com))

### synchronized

`synchronized` 是 Java 内置锁机制，可以修饰实例方法、静态方法或代码块。它适合保护临界区，保证同一时刻只有一个线程进入被保护的代码。对于简单同步需求，`synchronized` 代码更直接，异常退出时会自动释放锁，不需要手动 unlock。

文件位置：`src/main/java/io/github/atengk/concurrent/safety/SynchronizedAccountService.java`

该代码模拟账户扣款，通过 `synchronized` 保证余额检查和扣减是一个完整的临界区。

```java
package io.github.atengk.concurrent.safety;

import cn.hutool.core.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * synchronized 账户扣款示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class SynchronizedAccountService {

    private BigDecimal balance = new BigDecimal("1000.00");

    /**
     * 扣减余额
     *
     * @param amount 扣款金额
     * @return 是否扣款成功
     */
    public synchronized boolean deduct(BigDecimal amount) {
        if (amount == null || NumberUtil.isLessOrEqual(amount, BigDecimal.ZERO)) {
            log.warn("扣款金额非法，金额：{}", amount);
            return false;
        }

        if (NumberUtil.isLess(balance, amount)) {
            log.warn("余额不足，当前余额：{}，扣款金额：{}", balance, amount);
            return false;
        }

        balance = balance.subtract(amount);
        log.info("扣款成功，扣款金额：{}，剩余余额：{}", amount, balance);
        return true;
    }

    /**
     * 获取账户余额
     *
     * @return 账户余额
     */
    public synchronized BigDecimal getBalance() {
        return balance;
    }

    public static void main(String[] args) {
        SynchronizedAccountService accountService = new SynchronizedAccountService();

        for (int i = 0; i < 10; i++) {
            Thread.ofPlatform().name("deduct-worker-", i).start(() ->
                    accountService.deduct(new BigDecimal("150.00")));
        }
    }
}
```

`synchronized` 适合临界区较短、锁竞争不复杂的场景。它不支持尝试加锁、超时加锁、公平锁、多个条件队列等高级能力。如果需要这些能力，应使用 `Lock` 或 `ReentrantLock`。

### volatile

`volatile` 用于保证变量修改对其他线程可见，并限制相关指令重排序。它适合状态标记、开关、配置刷新标识等场景。它不适合保护 `count++` 这类复合操作，因为 `volatile` 不能保证读取、修改、写回三步整体原子。

文件位置：`src/main/java/io/github/atengk/concurrent/safety/VolatileStopDemo.java`

该代码演示使用 `volatile` 控制后台线程停止。

```java
package io.github.atengk.concurrent.safety;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * volatile 停止标记示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class VolatileStopDemo {

    private volatile boolean running = true;

    /**
     * 启动后台任务
     */
    public void startTask() {
        Thread.ofPlatform().name("volatile-worker-", 1).start(() -> {
            while (running) {
                log.info("后台任务执行中");
                ThreadUtil.sleep(500);
            }
            log.info("后台任务已停止");
        });
    }

    /**
     * 停止后台任务
     */
    public void stopTask() {
        running = false;
        log.info("已发送停止信号");
    }

    public static void main(String[] args) {
        VolatileStopDemo demo = new VolatileStopDemo();
        demo.startTask();

        ThreadUtil.sleep(2000);
        demo.stopTask();
    }
}
```

`volatile` 的典型使用原则是：一个线程写，多个线程读；变量赋值本身不依赖旧值；不需要多个变量之间保持一致性。如果需要复合状态一致性，例如余额判断加扣减、库存判断加扣减、订单状态流转，应使用锁、数据库事务或 CAS 方案。

### Lock 与 ReentrantLock

`Lock` 是显式锁接口，`ReentrantLock` 是常用实现。`ReentrantLock` 具备和 `synchronized` 类似的互斥语义，同时提供可中断加锁、尝试加锁、超时加锁、公平锁、条件队列等扩展能力。Oracle JDK 21 文档说明，`ReentrantLock` 与 `synchronized` 访问的隐式监视器锁具有相同的基本行为和语义，但具备扩展能力。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html?source=ad%3Anat%3A%3A%3Arc_lamk190221p00018c0002%3Ataboola_iaas_pt_b%2Cad%3Anat%3A%3A%3Arc_lamk190221p00018c0002%3Ataboola_iaas_pt_b&utm_source=chatgpt.com))

使用 `ReentrantLock` 时必须在 `finally` 中释放锁，否则异常会导致锁无法释放，后续线程永久等待。

文件位置：`src/main/java/io/github/atengk/concurrent/safety/ReentrantLockOrderService.java`

该代码演示使用 `tryLock` 避免线程无限等待。

```java
package io.github.atengk.concurrent.safety;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock 订单处理示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class ReentrantLockOrderService {

    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * 处理订单
     *
     * @param orderNo 订单号
     * @return 是否处理成功
     */
    public boolean processOrder(String orderNo) {
        boolean locked = false;

        try {
            locked = lock.tryLock(2, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取订单处理锁超时，订单号：{}", orderNo);
                return false;
            }

            log.info("开始处理订单，订单号：{}", orderNo);
            ThreadUtil.sleep(1000);
            log.info("订单处理完成，订单号：{}", orderNo);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取订单处理锁时被中断，订单号：{}", orderNo, e);
            return false;
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) {
        ReentrantLockOrderService orderService = new ReentrantLockOrderService();

        for (int i = 0; i < 5; i++) {
            int index = i;
            Thread.ofPlatform().name("order-lock-worker-", i).start(() ->
                    orderService.processOrder("ORDER-" + index));
        }
    }
}
```

公平锁可以减少线程饥饿，但吞吐量通常低于非公平锁。默认情况下，`ReentrantLock` 是非公平锁；只有在明确需要先到先得的业务语义时，才建议开启公平锁。

### Atomic 原子类

Atomic 原子类基于 CAS 等底层机制提供无锁原子更新能力，适合单变量计数、状态切换、序号生成等场景。`AtomicInteger` 可用于原子递增计数器，但官方文档也说明它不能作为 `Integer` 的完全替代品。([Oracle 文档](https://docs.oracle.com/javase/jp/21/docs/api/java.base/java/util/concurrent/atomic/AtomicInteger.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/safety/AtomicInventoryService.java`

该代码演示使用 `AtomicInteger` 实现库存扣减。

```java
package io.github.atengk.concurrent.safety;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Atomic 原子库存服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class AtomicInventoryService {

    private final AtomicInteger stock = new AtomicInteger(100);

    /**
     * 扣减库存
     *
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    public boolean deductStock(int quantity) {
        while (true) {
            int currentStock = stock.get();

            if (quantity <= 0) {
                log.warn("扣减数量非法，数量：{}", quantity);
                return false;
            }

            if (currentStock < quantity) {
                log.warn("库存不足，当前库存：{}，扣减数量：{}", currentStock, quantity);
                return false;
            }

            int newStock = currentStock - quantity;
            if (stock.compareAndSet(currentStock, newStock)) {
                log.info("库存扣减成功，扣减数量：{}，剩余库存：{}", quantity, newStock);
                return true;
            }

            log.debug("库存扣减 CAS 失败，准备重试，当前库存快照：{}", currentStock);
        }
    }

    /**
     * 获取当前库存
     *
     * @return 当前库存
     */
    public int getStock() {
        return stock.get();
    }

    public static void main(String[] args) {
        AtomicInventoryService inventoryService = new AtomicInventoryService();

        for (int i = 0; i < 20; i++) {
            Thread.ofPlatform().name("stock-worker-", i).start(() -> inventoryService.deductStock(8));
        }
    }
}
```

Atomic 适合单变量原子更新，不适合多个变量之间的一致性维护。例如同时更新账户余额、积分、流水状态时，Atomic 不能替代数据库事务或业务锁。

### ThreadLocal

`ThreadLocal` 用于保存线程本地变量。每个线程访问同一个 `ThreadLocal` 实例时，都拥有自己的独立副本。Oracle JDK 21 文档说明，`ThreadLocal` 变量和普通变量不同，每个访问它的线程都通过 `get` 或 `set` 拥有一个独立初始化的副本，典型场景包括用户 ID、事务 ID 等与线程关联的状态。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ThreadLocal.html?utm_source=chatgpt.com))

`ThreadLocal` 的核心风险是线程池复用。线程执行完一个任务后不会销毁，如果没有调用 `remove()` 清理数据，下一个任务可能读取到上一个任务残留的上下文。官方文档也说明，线程存活且 `ThreadLocal` 实例可访问时，线程会持有其线程本地变量副本的隐式引用。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ThreadLocal.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/safety/UserContextHolder.java`

该工具类用于保存当前线程的用户上下文，并提供清理方法。

```java
package io.github.atengk.concurrent.safety;

import cn.hutool.core.util.StrUtil;

/**
 * 用户上下文持有器
 *
 * @author Ateng
 * @since 2026-05-08
 */
public final class UserContextHolder {

    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    /**
     * 设置用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return;
        }
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 清理用户上下文
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/safety/UserContextTaskDemo.java`

该代码演示 `ThreadLocal` 必须在 `finally` 中清理。

```java
package io.github.atengk.concurrent.safety;

import lombok.extern.slf4j.Slf4j;

/**
 * ThreadLocal 上下文使用示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class UserContextTaskDemo {

    /**
     * 执行业务任务
     *
     * @param userId 用户ID
     */
    public void executeTask(String userId) {
        try {
            UserContextHolder.setUserId(userId);
            log.info("开始处理用户任务，用户ID：{}", UserContextHolder.getUserId());
        } finally {
            UserContextHolder.clear();
            log.info("用户上下文已清理");
        }
    }

    public static void main(String[] args) {
        UserContextTaskDemo demo = new UserContextTaskDemo();
        demo.executeTask("10001");
    }
}
```

在 Spring Boot Web 项目中，用户 ID、租户 ID、traceId 可以通过过滤器或拦截器写入 `ThreadLocal`，但必须在请求结束时清理。异步任务、线程池、虚拟线程场景下，不要默认假设 `ThreadLocal` 会自动跨线程传递。

### 不可变对象

不可变对象是天然线程安全的重要手段。对象创建后状态不再变化，多个线程只能读取，不能修改，因此不需要加锁。Java 中常见不可变对象包括 `String`、包装类型、`BigDecimal`、`LocalDateTime`，以及使用 `record` 或 `final` 字段设计的业务对象。

文件位置：`src/main/java/io/github/atengk/concurrent/safety/OrderSnapshot.java`

该记录类用于表示订单快照，创建后字段不可修改。

```java
package io.github.atengk.concurrent.safety;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 不可变订单快照
 *
 * @author Ateng
 * @since 2026-05-08
 */
public record OrderSnapshot(
        String orderNo,
        Long userId,
        BigDecimal amount,
        String status,
        LocalDateTime createTime
) {
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/safety/ImmutableObjectDemo.java`

该代码演示多个线程读取不可变对象时不需要加锁。

```java
package io.github.atengk.concurrent.safety;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 不可变对象使用示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class ImmutableObjectDemo {

    /**
     * 并发读取订单快照
     */
    public void readSnapshot() {
        OrderSnapshot snapshot = new OrderSnapshot(
                "ORDER-10001",
                10001L,
                new BigDecimal("99.90"),
                "PAID",
                LocalDateTime.now()
        );

        for (int i = 0; i < 5; i++) {
            Thread.ofPlatform().name("snapshot-reader-", i).start(() ->
                    log.info("读取订单快照：{}", snapshot));
        }
    }

    public static void main(String[] args) {
        ImmutableObjectDemo demo = new ImmutableObjectDemo();
        demo.readSnapshot();
    }
}
```

不可变对象适合配置快照、查询结果、事件消息、DTO、缓存值等场景。如果对象内部包含集合字段，应注意集合本身也要不可变，可以使用 `List.copyOf()`、`Map.copyOf()` 或返回不可修改视图，避免外部修改内部状态。

### 并发集合

普通集合如 `ArrayList`、`HashMap` 在多线程并发修改时不是线程安全的。并发集合位于 `java.util.concurrent` 包中，JDK 文档说明该包提供并发编程中常用的工具类，包括并发集合、执行器、同步器等。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-use.html?utm_source=chatgpt.com))

常见并发集合如下：

| 集合                    | 适用场景         |
| ----------------------- | ---------------- |
| `ConcurrentHashMap`     | 高并发 Map 读写  |
| `CopyOnWriteArrayList`  | 读多写少的列表   |
| `ConcurrentLinkedQueue` | 高并发非阻塞队列 |
| `LinkedBlockingQueue`   | 生产者消费者队列 |
| `ConcurrentSkipListMap` | 并发有序 Map     |

文件位置：`src/main/java/io/github/atengk/concurrent/safety/ConcurrentCollectionDemo.java`

该代码演示使用 `ConcurrentHashMap` 统计接口访问次数。

```java
package io.github.atengk.concurrent.safety;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 并发集合示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class ConcurrentCollectionDemo {

    private final Map<String, LongAdder> apiCounterMap = new ConcurrentHashMap<>();

    /**
     * 记录接口访问次数
     *
     * @param apiPath 接口路径
     */
    public void recordApiVisit(String apiPath) {
        apiCounterMap.computeIfAbsent(apiPath, key -> new LongAdder()).increment();
    }

    /**
     * 获取接口访问次数
     *
     * @param apiPath 接口路径
     * @return 访问次数
     */
    public long getApiVisitCount(String apiPath) {
        LongAdder counter = apiCounterMap.get(apiPath);
        return counter == null ? 0L : counter.sum();
    }

    public static void main(String[] args) {
        ConcurrentCollectionDemo demo = new ConcurrentCollectionDemo();

        for (int i = 0; i < 100; i++) {
            Thread.ofPlatform().name("api-counter-", i).start(() ->
                    demo.recordApiVisit("/orders/create"));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待统计任务完成时被中断", e);
        }

        log.info("接口访问次数：{}", demo.getApiVisitCount("/orders/create"));
    }
}
```

并发集合只能保证集合自身操作的线程安全，不会自动保证业务复合逻辑的原子性。例如“先判断 Map 中是否存在，再插入，再更新数据库”仍然需要额外同步、锁、事务或原子方法。

## CompletableFuture 实战

`CompletableFuture` 是 JDK 中最常用的异步编排工具之一。它既是可以显式完成的 `Future`，也可以作为 `CompletionStage` 使用，支持任务完成后的依赖动作、组合动作、异常处理和超时控制。Oracle JDK 21 文档说明，`CompletableFuture` 可作为 `CompletionStage` 使用，并支持在完成时触发依赖函数和动作。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html?utm_source=chatgpt.com))

本章节使用一个“订单详情聚合”场景说明常见实战用法：查询订单基础信息、查询用户等级、查询优惠券、查询物流信息，然后组合为订单详情结果。

### 异步任务编排

异步任务编排用于把多个独立或有依赖关系的任务组织成清晰的执行流程。`CompletableFuture.supplyAsync()` 可以提交有返回值的异步任务；如果不指定执行器，默认会使用公共线程池；如果指定 `Executor`，任务会在给定执行器中执行。JDK 21 文档也提供了 `supplyAsync(Supplier, Executor)` 这种使用指定执行器的方法。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/future/config/CompletableFutureExecutorConfig.java`

该配置类提供订单聚合专用线程池，避免业务异步任务使用默认公共线程池。

```java
package io.github.atengk.concurrent.future.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * CompletableFuture 线程池配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
public class CompletableFutureExecutorConfig {

    /**
     * 订单聚合线程池
     *
     * @return 订单聚合线程池
     */
    @Bean("orderAggregateExecutor")
    public ExecutorService orderAggregateExecutor() {
        ThreadFactory threadFactory = Thread.ofPlatform()
                .name("order-aggregate-", 0)
                .factory();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                8,
                32,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(300),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        log.info("订单聚合线程池初始化完成");
        return executor;
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/future/model/OrderDetailVO.java`

该对象用于承载订单聚合后的返回结果。

```java
package io.github.atengk.concurrent.future.model;

import java.math.BigDecimal;

/**
 * 订单详情返回对象
 *
 * @author Ateng
 * @since 2026-05-08
 */
public record OrderDetailVO(
        String orderNo,
        String orderStatus,
        BigDecimal amount,
        String userLevel,
        String couponName,
        String logisticsStatus
) {
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/future/service/OrderAggregateService.java`

该服务提供订单详情聚合的基础异步任务。

```java
package io.github.atengk.concurrent.future.service;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.concurrent.future.model.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 订单聚合服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAggregateService {

    private final ExecutorService orderAggregateExecutor;

    /**
     * 查询订单详情
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    public CompletableFuture<OrderDetailVO> queryOrderDetail(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("订单号不能为空"));
        }

        CompletableFuture<OrderBaseInfo> orderFuture = queryOrderBaseInfo(orderNo);
        CompletableFuture<String> userLevelFuture = queryUserLevel(orderNo);
        CompletableFuture<String> couponFuture = queryCoupon(orderNo);
        CompletableFuture<String> logisticsFuture = queryLogistics(orderNo);

        return CompletableFuture.allOf(orderFuture, userLevelFuture, couponFuture, logisticsFuture)
                .thenApplyAsync(unused -> new OrderDetailVO(
                        orderFuture.join().orderNo(),
                        orderFuture.join().status(),
                        orderFuture.join().amount(),
                        userLevelFuture.join(),
                        couponFuture.join(),
                        logisticsFuture.join()
                ), orderAggregateExecutor);
    }

    /**
     * 查询订单基础信息
     *
     * @param orderNo 订单号
     * @return 订单基础信息
     */
    private CompletableFuture<OrderBaseInfo> queryOrderBaseInfo(String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("开始查询订单基础信息，订单号：{}", orderNo);
            ThreadUtil.sleep(300);
            return new OrderBaseInfo(orderNo, "PAID", new BigDecimal("199.90"));
        }, orderAggregateExecutor);
    }

    /**
     * 查询用户等级
     *
     * @param orderNo 订单号
     * @return 用户等级
     */
    private CompletableFuture<String> queryUserLevel(String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("开始查询用户等级，订单号：{}", orderNo);
            ThreadUtil.sleep(500);
            return "VIP";
        }, orderAggregateExecutor);
    }

    /**
     * 查询优惠券信息
     *
     * @param orderNo 订单号
     * @return 优惠券名称
     */
    private CompletableFuture<String> queryCoupon(String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("开始查询优惠券信息，订单号：{}", orderNo);
            ThreadUtil.sleep(400);
            return "满100减20";
        }, orderAggregateExecutor);
    }

    /**
     * 查询物流状态
     *
     * @param orderNo 订单号
     * @return 物流状态
     */
    private CompletableFuture<String> queryLogistics(String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("开始查询物流状态，订单号：{}", orderNo);
            ThreadUtil.sleep(600);
            return "运输中";
        }, orderAggregateExecutor);
    }

    /**
     * 订单基础信息
     *
     * @author Ateng
     * @since 2026-05-08
     */
    private record OrderBaseInfo(String orderNo, String status, BigDecimal amount) {
    }
}
```

`CompletableFuture.allOf()` 适合等待多个任务全部完成，但它的返回值是 `CompletableFuture<Void>`，不会直接返回每个任务的结果。因此通常需要在 `thenApply()` 中通过各个子任务的 `join()` 取结果。`join()` 在异常时抛出未检查异常，适合在异步编排内部使用；如果需要受检异常处理，可以使用 `get()`。

### 串行任务组合

串行组合用于处理后一个任务依赖前一个任务结果的场景。例如先查询订单得到用户 ID，再根据用户 ID 查询用户等级，再根据等级计算权益。`thenApply()` 用于同步转换结果，`thenCompose()` 用于把上一步结果继续展开为新的异步任务，避免出现 `CompletableFuture<CompletableFuture<T>>` 嵌套。

文件位置：`src/main/java/io/github/atengk/concurrent/future/service/SerialFutureService.java`

该服务演示使用 `thenCompose()` 实现串行异步任务。

```java
package io.github.atengk.concurrent.future.service;

import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * CompletableFuture 串行任务服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SerialFutureService {

    private final ExecutorService orderAggregateExecutor;

    /**
     * 串行查询用户权益
     *
     * @param orderNo 订单号
     * @return 用户权益
     */
    public CompletableFuture<String> queryUserBenefit(String orderNo) {
        return queryUserIdByOrderNo(orderNo)
                .thenCompose(this::queryUserLevel)
                .thenApply(userLevel -> "VIP".equals(userLevel) ? "包邮 + 专属客服" : "普通权益");
    }

    /**
     * 根据订单号查询用户ID
     *
     * @param orderNo 订单号
     * @return 用户ID
     */
    private CompletableFuture<Long> queryUserIdByOrderNo(String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("根据订单查询用户ID，订单号：{}", orderNo);
            ThreadUtil.sleep(300);
            return 10001L;
        }, orderAggregateExecutor);
    }

    /**
     * 查询用户等级
     *
     * @param userId 用户ID
     * @return 用户等级
     */
    private CompletableFuture<String> queryUserLevel(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("查询用户等级，用户ID：{}", userId);
            ThreadUtil.sleep(300);
            return "VIP";
        }, orderAggregateExecutor);
    }
}
```

串行任务不要用多个 `join()` 强行拼接，否则容易让异步链退化成阻塞代码。只要后续步骤依赖前一步异步结果，优先使用 `thenCompose()`。

### 并行任务组合

并行组合适合多个任务互不依赖的场景。例如订单详情页同时查询订单、用户、优惠券和物流。`thenCombine()` 适合组合两个任务；`allOf()` 适合等待多个任务完成；`anyOf()` 适合多个任务中任意一个完成后继续处理。JDK 21 文档中，`CompletableFuture` 提供 `thenCombine`、`allOf`、`anyOf` 等方法用于组合不同阶段的任务。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/future/service/ParallelFutureService.java`

该服务演示两个任务用 `thenCombine()` 合并，多个任务用 `allOf()` 合并。

```java
package io.github.atengk.concurrent.future.service;

import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * CompletableFuture 并行任务服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParallelFutureService {

    private final ExecutorService orderAggregateExecutor;

    /**
     * 查询订单展示标题
     *
     * @param orderNo 订单号
     * @return 订单展示标题
     */
    public CompletableFuture<String> queryOrderTitle(String orderNo) {
        CompletableFuture<String> orderNameFuture = CompletableFuture.supplyAsync(() -> {
            log.info("查询订单商品名称，订单号：{}", orderNo);
            ThreadUtil.sleep(300);
            return "机械键盘";
        }, orderAggregateExecutor);

        CompletableFuture<String> userNameFuture = CompletableFuture.supplyAsync(() -> {
            log.info("查询用户名称，订单号：{}", orderNo);
            ThreadUtil.sleep(300);
            return "Ateng";
        }, orderAggregateExecutor);

        return orderNameFuture.thenCombine(userNameFuture,
                (orderName, userName) -> userName + " 购买了 " + orderName);
    }

    /**
     * 并行查询订单标签
     *
     * @param orderNo 订单号
     * @return 订单标签
     */
    public CompletableFuture<List<String>> queryOrderTags(String orderNo) {
        CompletableFuture<String> paymentTagFuture = queryTag(orderNo, "已支付");
        CompletableFuture<String> logisticsTagFuture = queryTag(orderNo, "运输中");
        CompletableFuture<String> riskTagFuture = queryTag(orderNo, "低风险");

        return CompletableFuture.allOf(paymentTagFuture, logisticsTagFuture, riskTagFuture)
                .thenApply(unused -> List.of(
                        paymentTagFuture.join(),
                        logisticsTagFuture.join(),
                        riskTagFuture.join()
                ));
    }

    /**
     * 查询标签
     *
     * @param orderNo 订单号
     * @param tag 标签
     * @return 标签
     */
    private CompletableFuture<String> queryTag(String orderNo, String tag) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("查询订单标签，订单号：{}，标签：{}", orderNo, tag);
            ThreadUtil.sleep(200);
            return tag;
        }, orderAggregateExecutor);
    }
}
```

并行任务组合要注意下游容量。并行查询虽然能降低接口耗时，但会同时放大数据库、Redis、HTTP 接口的瞬时压力。生产环境应结合线程池大小、连接池大小、超时时间和限流策略一起设计。

### 任务超时控制

异步任务必须设置超时。没有超时的异步任务可能长期占用线程、连接和内存，最终导致队列堆积和请求雪崩。`CompletableFuture` 提供 `orTimeout()` 和 `completeOnTimeout()`：`orTimeout()` 在超时时以异常方式完成；`completeOnTimeout()` 在超时时返回默认值。JDK 21 文档说明，`orTimeout` 会在指定时间内未完成时以 `TimeoutException` 异常完成，而 `completeOnTimeout` 会在超时时用给定值完成。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/future/service/TimeoutFutureService.java`

该服务演示异常超时和默认值超时两种处理方式。

```java
package io.github.atengk.concurrent.future.service;

import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * CompletableFuture 超时控制服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeoutFutureService {

    private final ExecutorService orderAggregateExecutor;

    /**
     * 查询物流状态，超时后返回异常兜底
     *
     * @param orderNo 订单号
     * @return 物流状态
     */
    public CompletableFuture<String> queryLogisticsWithTimeout(String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
                    log.info("开始查询物流状态，订单号：{}", orderNo);
                    ThreadUtil.sleep(3000);
                    return "已签收";
                }, orderAggregateExecutor)
                .orTimeout(1, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("查询物流状态超时或异常，订单号：{}", orderNo, e);
                    return "物流状态暂不可用";
                });
    }

    /**
     * 查询优惠券信息，超时后直接返回默认值
     *
     * @param orderNo 订单号
     * @return 优惠券信息
     */
    public CompletableFuture<String> queryCouponWithDefault(String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
                    log.info("开始查询优惠券信息，订单号：{}", orderNo);
                    ThreadUtil.sleep(3000);
                    return "满100减20";
                }, orderAggregateExecutor)
                .completeOnTimeout("暂无优惠券", 1, TimeUnit.SECONDS);
    }
}
```

需要注意，`CompletableFuture` 超时完成不等于底层任务一定被物理中断。对于 HTTP、数据库、Redis 等调用，还必须在客户端层面设置连接超时、读取超时和请求超时。

### 异常处理

`CompletableFuture` 常用异常处理方法包括 `exceptionally()`、`handle()` 和 `whenComplete()`。`exceptionally()` 适合异常兜底并返回默认值；`handle()` 可以同时处理成功和失败，并转换结果；`whenComplete()` 适合记录日志、释放资源、统计指标，但不适合改变最终结果。

文件位置：`src/main/java/io/github/atengk/concurrent/future/service/ExceptionFutureService.java`

该服务演示三种常见异常处理方式。

```java
package io.github.atengk.concurrent.future.service;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * CompletableFuture 异常处理服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionFutureService {

    private final ExecutorService orderAggregateExecutor;

    /**
     * 使用 exceptionally 兜底异常
     *
     * @param orderNo 订单号
     * @return 查询结果
     */
    public CompletableFuture<String> queryWithExceptionally(String orderNo) {
        return mockRemoteQuery(orderNo)
                .exceptionally(e -> {
                    log.error("远程查询异常，使用默认值，订单号：{}", orderNo, e);
                    return "默认结果";
                });
    }

    /**
     * 使用 handle 同时处理成功和失败
     *
     * @param orderNo 订单号
     * @return 查询结果
     */
    public CompletableFuture<String> queryWithHandle(String orderNo) {
        return mockRemoteQuery(orderNo)
                .handle((result, e) -> {
                    if (e != null) {
                        log.error("远程查询失败，订单号：{}", orderNo, e);
                        return "失败兜底结果";
                    }

                    log.info("远程查询成功，订单号：{}，结果：{}", orderNo, result);
                    return "处理后结果：" + result;
                });
    }

    /**
     * 使用 whenComplete 记录结果
     *
     * @param orderNo 订单号
     * @return 查询结果
     */
    public CompletableFuture<String> queryWithWhenComplete(String orderNo) {
        return mockRemoteQuery(orderNo)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        log.error("远程查询完成但发生异常，订单号：{}", orderNo, e);
                    } else {
                        log.info("远程查询完成，订单号：{}，结果：{}", orderNo, result);
                    }
                });
    }

    /**
     * 模拟远程查询
     *
     * @param orderNo 订单号
     * @return 查询结果
     */
    private CompletableFuture<String> mockRemoteQuery(String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("开始模拟远程查询，订单号：{}", orderNo);
            ThreadUtil.sleep(300);

            if (RandomUtil.randomBoolean()) {
                throw new IllegalStateException("远程服务异常");
            }

            return "远程结果：" + orderNo;
        }, orderAggregateExecutor);
    }
}
```

异常处理建议按业务重要性分层处理。可降级的查询类任务可以返回默认值；不可降级的交易类任务应抛出异常并中止流程；需要补偿的异步任务应记录失败任务、业务参数和异常原因，后续通过重试任务或人工处理恢复。

### 自定义线程池

`CompletableFuture` 的异步方法如果不指定执行器，通常会使用默认异步执行设施，例如公共线程池。公共线程池不适合承载所有业务 IO 任务，因为不同业务会互相影响。生产项目应为核心异步链路指定自定义线程池。JDK 21 文档中，`CompletableFuture` 明确提供了带 `Executor` 参数的异步方法，例如 `supplyAsync(Supplier, Executor)`、`thenAcceptAsync(..., Executor)` 等。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/future/controller/OrderAggregateController.java`

该接口用于验证订单聚合、串行组合、并行组合、超时控制和异常处理。

```java
package io.github.atengk.concurrent.future.controller;

import io.github.atengk.concurrent.future.model.OrderDetailVO;
import io.github.atengk.concurrent.future.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CompletableFuture 订单聚合接口
 *
 * @author Ateng
 * @since 2026-05-08
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/future/orders")
public class OrderAggregateController {

    private final OrderAggregateService orderAggregateService;
    private final SerialFutureService serialFutureService;
    private final ParallelFutureService parallelFutureService;
    private final TimeoutFutureService timeoutFutureService;
    private final ExceptionFutureService exceptionFutureService;

    /**
     * 查询订单聚合详情
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    @GetMapping("/{orderNo}/detail")
    public CompletableFuture<OrderDetailVO> queryOrderDetail(@PathVariable String orderNo) {
        return orderAggregateService.queryOrderDetail(orderNo);
    }

    /**
     * 查询用户权益
     *
     * @param orderNo 订单号
     * @return 用户权益
     */
    @GetMapping("/{orderNo}/benefit")
    public CompletableFuture<String> queryUserBenefit(@PathVariable String orderNo) {
        return serialFutureService.queryUserBenefit(orderNo);
    }

    /**
     * 查询订单标题
     *
     * @param orderNo 订单号
     * @return 订单标题
     */
    @GetMapping("/{orderNo}/title")
    public CompletableFuture<String> queryOrderTitle(@PathVariable String orderNo) {
        return parallelFutureService.queryOrderTitle(orderNo);
    }

    /**
     * 查询订单标签
     *
     * @param orderNo 订单号
     * @return 订单标签
     */
    @GetMapping("/{orderNo}/tags")
    public CompletableFuture<List<String>> queryOrderTags(@PathVariable String orderNo) {
        return parallelFutureService.queryOrderTags(orderNo);
    }

    /**
     * 查询物流状态
     *
     * @param orderNo 订单号
     * @return 物流状态
     */
    @GetMapping("/{orderNo}/logistics")
    public CompletableFuture<String> queryLogistics(@PathVariable String orderNo) {
        return timeoutFutureService.queryLogisticsWithTimeout(orderNo);
    }

    /**
     * 查询异常兜底结果
     *
     * @param orderNo 订单号
     * @return 查询结果
     */
    @GetMapping("/{orderNo}/exception")
    public CompletableFuture<String> queryExceptionResult(@PathVariable String orderNo) {
        return exceptionFutureService.queryWithExceptionally(orderNo);
    }
}
```

接口验证命令如下：

```bash
curl "http://localhost:8080/future/orders/ORDER-10001/detail"
curl "http://localhost:8080/future/orders/ORDER-10001/benefit"
curl "http://localhost:8080/future/orders/ORDER-10001/title"
curl "http://localhost:8080/future/orders/ORDER-10001/tags"
curl "http://localhost:8080/future/orders/ORDER-10001/logistics"
curl "http://localhost:8080/future/orders/ORDER-10001/exception"
```

`CompletableFuture` 实战中的关键原则是：所有业务异步任务显式指定线程池；所有外部调用设置超时；所有异常必须有日志和兜底策略；并行任务数量必须受下游资源容量约束；不要在异步链中随意 `get()` 阻塞；需要串行依赖时优先用 `thenCompose()`，需要并行组合时优先用 `thenCombine()` 或 `allOf()`。



## Spring Web 场景并发处理

Spring Web 场景中的并发处理主要分为两类：一类是 Controller 层异步请求处理，目标是释放 Servlet 容器线程，让请求结果稍后返回；另一类是业务异步任务，目标是把非核心链路任务交给线程池执行。二者经常被混淆，但设计目标不同。Spring MVC 官方文档明确支持 `DeferredResult`、`Callable`、`WebAsyncTask` 等异步返回值，并说明 `Callable` 会提交到配置的 `AsyncTaskExecutor` 中执行，而 Servlet 容器线程会先释放。([Home](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html?utm_source=chatgpt.com))

### Controller 异步处理

Controller 异步处理适合接口本身耗时较长，但仍然需要把最终结果返回给客户端的场景。例如文件解析进度查询、长耗时统计、远程接口聚合、慢查询报表等。它和 `@Async void` 不同：Controller 异步处理不是“接口立即成功”，而是“释放请求线程，等异步结果完成后再写回响应”。

Spring MVC 常见异步返回方式如下：

| 返回类型               | 适用场景                       | 特点                           |
| ---------------------- | ------------------------------ | ------------------------------ |
| `Callable<T>`          | 简单异步返回                   | 由 Spring MVC 提交到异步执行器 |
| `WebAsyncTask<T>`      | 需要指定超时、执行器           | 比 `Callable` 控制能力更强     |
| `DeferredResult<T>`    | 结果由外部线程、消息、事件触发 | 适合长轮询、异步回调           |
| `CompletableFuture<T>` | JDK 异步编排                   | 适合多个任务组合               |

文件位置：`src/main/java/io/github/atengk/concurrent/web/config/WebAsyncConfig.java`

该配置类用于配置 Spring MVC 异步请求执行器和默认超时时间。

```java
package io.github.atengk.concurrent.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Spring MVC 异步请求配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
public class WebAsyncConfig implements WebMvcConfigurer {

    /**
     * 配置 MVC 异步请求支持
     *
     * @param configurer 异步配置器
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("mvc-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        configurer.setTaskExecutor(executor);
        configurer.setDefaultTimeout(5000);

        log.info("Spring MVC 异步请求线程池初始化完成");
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/web/controller/WebAsyncController.java`

该接口演示 `Callable`、`WebAsyncTask` 和 `DeferredResult` 三种 Controller 异步处理方式。

```java
package io.github.atengk.concurrent.web.controller;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.util.concurrent.Callable;

/**
 * Spring MVC 异步请求接口
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestController
@RequestMapping("/web-async")
public class WebAsyncController {

    /**
     * 使用 Callable 异步返回
     *
     * @return 异步结果
     */
    @GetMapping("/callable")
    public Callable<String> callable() {
        log.info("接收到 Callable 请求，当前线程：{}", Thread.currentThread().getName());

        return () -> {
            log.info("Callable 任务开始执行，当前线程：{}", Thread.currentThread().getName());
            ThreadUtil.sleep(1000);
            return "Callable 异步结果";
        };
    }

    /**
     * 使用 WebAsyncTask 异步返回
     *
     * @return 异步任务
     */
    @GetMapping("/web-async-task")
    public WebAsyncTask<String> webAsyncTask() {
        log.info("接收到 WebAsyncTask 请求，当前线程：{}", Thread.currentThread().getName());

        WebAsyncTask<String> task = new WebAsyncTask<>(3000L, () -> {
            log.info("WebAsyncTask 任务开始执行，当前线程：{}", Thread.currentThread().getName());
            ThreadUtil.sleep(1000);
            return "WebAsyncTask 异步结果";
        });

        task.onTimeout(() -> {
            log.warn("WebAsyncTask 执行超时");
            return "请求处理超时";
        });

        task.onCompletion(() -> log.info("WebAsyncTask 请求处理完成"));

        return task;
    }

    /**
     * 使用 DeferredResult 异步返回
     *
     * @return 延迟结果
     */
    @GetMapping("/deferred")
    public DeferredResult<String> deferredResult() {
        log.info("接收到 DeferredResult 请求，当前线程：{}", Thread.currentThread().getName());

        DeferredResult<String> deferredResult = new DeferredResult<>(3000L, "DeferredResult 请求超时");

        Thread.ofVirtual()
                .name("deferred-worker-", 1)
                .start(() -> {
                    log.info("DeferredResult 后台任务开始执行，当前线程：{}", Thread.currentThread().getName());
                    ThreadUtil.sleep(1000);
                    deferredResult.setResult("DeferredResult 异步结果");
                });

        deferredResult.onCompletion(() -> log.info("DeferredResult 请求处理完成"));
        deferredResult.onTimeout(() -> log.warn("DeferredResult 请求处理超时"));

        return deferredResult;
    }
}
```

验证命令如下：

```bash
curl "http://localhost:8080/web-async/callable"
curl "http://localhost:8080/web-async/web-async-task"
curl "http://localhost:8080/web-async/deferred"
```

需要注意，Controller 异步处理只是释放 Servlet 容器线程，并不代表业务任务不消耗资源。异步任务仍然会占用业务线程池、数据库连接、HTTP 连接和内存。Spring MVC 官方文档也提醒，MVC 异步处理使用的默认执行器不适合生产高负载场景，应显式配置。([Home](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html?utm_source=chatgpt.com))

### Web 请求中的异步任务

Web 请求中的异步任务通常用于非主链路操作，例如发送短信、发送邮件、写审计日志、通知第三方系统、刷新缓存等。此类任务一般不影响接口主结果，可以通过 `@Async`、消息队列或事件机制异步执行。

如果异步任务失败会影响业务正确性，不建议简单使用 `@Async void`。更稳妥的方式是写入任务表或发送消息，由独立消费者执行并支持重试。`@Async` 更适合轻量级、允许失败兜底或可补偿的任务。

文件位置：`src/main/java/io/github/atengk/concurrent/web/service/WebOrderService.java`

该服务演示请求主流程保存订单后，异步发送订单通知。

```java
package io.github.atengk.concurrent.web.service;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Web 订单服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebOrderService {

    private final WebOrderAsyncService webOrderAsyncService;

    /**
     * 创建订单
     *
     * @param userId 用户ID
     * @return 订单号
     */
    public String createOrder(Long userId) {
        String orderNo = "ORDER-" + IdUtil.fastSimpleUUID();

        log.info("订单创建成功，用户ID：{}，订单号：{}", userId, orderNo);

        webOrderAsyncService.sendOrderNotify(orderNo, userId);

        return orderNo;
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/web/service/WebOrderAsyncService.java`

该异步服务用于发送订单通知。

```java
package io.github.atengk.concurrent.web.service;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Web 订单异步服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class WebOrderAsyncService {

    /**
     * 异步发送订单通知
     *
     * @param orderNo 订单号
     * @param userId 用户ID
     */
    @Async("ioTaskExecutor")
    public void sendOrderNotify(String orderNo, Long userId) {
        try {
            log.info("开始异步发送订单通知，订单号：{}，用户ID：{}", orderNo, userId);
            ThreadUtil.sleep(800);
            log.info("订单通知发送完成，订单号：{}", orderNo);
        } catch (Exception e) {
            log.error("订单通知发送失败，订单号：{}，用户ID：{}", orderNo, userId, e);
        }
    }
}
```

Web 请求中的异步任务要明确三个边界：第一，接口响应是否依赖异步结果；第二，异步失败是否需要补偿；第三，异步任务是否需要继承请求上下文。没有这些边界设计，异步化会把问题从同步链路隐藏到后台线程中。

### 异步任务与事务边界

`@Transactional` 的事务上下文绑定在线程上，异步任务切换线程后不会自动继承原事务。Spring 声明式事务由 AOP 代理驱动，只有通过代理进入的方法才会被事务拦截；Spring 官方文档说明，代理模式下只有外部方法调用经过代理时才会被拦截。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html?utm_source=chatgpt.com))

常见错误是：在事务方法中保存订单，然后立即调用异步方法读取订单或发送通知。此时主事务可能还没有提交，异步线程可能读不到数据，或者读到未完成状态。更严重的是，主事务后续回滚了，但异步通知已经发出，造成数据和外部动作不一致。

推荐方式是：主事务提交后再触发异步任务。可以使用 `TransactionSynchronizationManager.registerSynchronization()` 注册事务提交后的回调。

文件位置：`src/main/java/io/github/atengk/concurrent/web/service/OrderAfterCommitService.java`

该服务演示事务提交后再触发异步通知。

```java
package io.github.atengk.concurrent.web.service;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务提交后异步处理订单服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAfterCommitService {

    private final WebOrderAsyncService webOrderAsyncService;

    /**
     * 创建订单并在事务提交后发送通知
     *
     * @param userId 用户ID
     * @return 订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public String createOrderAfterCommit(Long userId) {
        String orderNo = "ORDER-" + IdUtil.fastSimpleUUID();

        log.info("保存订单数据，用户ID：{}，订单号：{}", userId, orderNo);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /**
             * 事务提交后执行异步通知
             */
            @Override
            public void afterCommit() {
                log.info("事务已提交，准备异步发送订单通知，订单号：{}", orderNo);
                webOrderAsyncService.sendOrderNotify(orderNo, userId);
            }
        });

        return orderNo;
    }
}
```

Spring 事务默认只在运行时异常和 `Error` 时回滚，受检异常默认不触发回滚；Spring Framework 6.1 还对返回时已经异常完成的 `CompletableFuture` 或 `Future` 做了特殊回滚处理，适合部分 `@Async` 和事务组合场景。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html?utm_source=chatgpt.com))

### 异步任务与安全上下文

Spring Security 的登录态保存在 `SecurityContextHolder` 中，默认通常依赖 `ThreadLocal`。异步任务切换线程后，安全上下文不会天然传递。如果异步任务中需要获取当前登录用户，需要显式传递用户 ID，或使用 Spring Security 提供的上下文包装执行器。

Spring Security 提供 `DelegatingSecurityContextAsyncTaskExecutor`，可以包装实际的 `AsyncTaskExecutor`，在异步执行时携带当前 `SecurityContext`。官方 API 文档说明，该执行器会把任务包装为携带 `SecurityContext` 的 `Runnable` 或 `Callable`。([Home](https://docs.spring.io/spring-security/site/docs/4.2.3.RELEASE/apidocs/org/springframework/security/task/DelegatingSecurityContextAsyncTaskExecutor.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/web/config/SecurityAsyncConfig.java`

该配置类演示包装异步执行器，使安全上下文能传递到异步线程。

```java
package io.github.atengk.concurrent.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

/**
 * 安全上下文异步执行器配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Configuration
public class SecurityAsyncConfig {

    /**
     * 包装异步执行器，支持 SecurityContext 传递
     *
     * @param ioTaskExecutor 原始 IO 异步执行器
     * @return 携带安全上下文的异步执行器
     */
    @Bean("securityContextAsyncExecutor")
    public AsyncTaskExecutor securityContextAsyncExecutor(AsyncTaskExecutor ioTaskExecutor) {
        return new DelegatingSecurityContextAsyncTaskExecutor(ioTaskExecutor);
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/web/service/SecurityContextAsyncService.java`

该服务演示异步线程中读取当前认证用户。

```java
package io.github.atengk.concurrent.web.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 安全上下文异步服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class SecurityContextAsyncService {

    /**
     * 异步打印当前登录用户
     */
    @Async("securityContextAsyncExecutor")
    public void printCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.warn("异步线程未获取到登录用户信息");
            return;
        }

        log.info("异步线程获取到登录用户，用户名：{}", authentication.getName());
    }
}
```

安全上下文传递不应滥用。对于关键业务异步任务，推荐显式传递 `userId`、`tenantId`、`requestId` 等业务字段，而不是强依赖线程上下文。显式参数更容易测试、重试和补偿。

### 异步任务与链路追踪

链路追踪的核心是把一次请求在同步线程、异步线程、远程调用、消息队列之间串起来。Web 请求进入时通常会生成或读取 `traceId`，然后放入 MDC。异步任务切换线程后，如果没有上下文传递，异步日志就会丢失 `traceId`。

Spring MVC 官方文档也提到，`ThreadLocal` 上下文在跨线程异步处理时需要额外工作，Micrometer Context Propagation 可以帮助在线程和上下文机制之间传播数据。([Home](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/web/config/TraceMdcTaskDecorator.java`

该装饰器用于复制 MDC 到异步线程，并在任务结束后清理。

```java
package io.github.atengk.concurrent.web.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 链路追踪 MDC 任务装饰器
 *
 * @author Ateng
 * @since 2026-05-08
 */
public class TraceMdcTaskDecorator implements TaskDecorator {

    /**
     * 装饰异步任务
     *
     * @param runnable 原始任务
     * @return 包装任务
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/web/config/TraceAsyncExecutorConfig.java`

该配置类为业务异步线程池增加 MDC 传递能力。

```java
package io.github.atengk.concurrent.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 链路追踪异步线程池配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
public class TraceAsyncExecutorConfig {

    /**
     * 创建支持 MDC 传递的线程池
     *
     * @return 异步线程池
     */
    @Bean("traceAsyncExecutor")
    public ThreadPoolTaskExecutor traceAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("trace-async-");
        executor.setTaskDecorator(new TraceMdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("链路追踪异步线程池初始化完成");
        return executor;
    }
}
```

链路追踪要遵循“入口生成、调用传递、异步复制、结束清理”的原则。尤其是平台线程池会复用线程，MDC、`ThreadLocal` 和安全上下文必须在 `finally` 中清理，否则会污染下一次任务。

## 数据一致性与事务

并发和异步会放大数据一致性问题。同步代码中依赖调用顺序保证的数据状态，进入异步、线程池、多实例或分布式环境后，可能出现脏读、丢失更新、重复提交、通知先于事务提交、任务执行成功但主事务回滚等问题。本章节重点说明 Spring 事务边界、事务传播、并发更新和常见锁方案。

### 异步方法中的事务问题

异步方法和调用方方法运行在不同线程中，调用方事务不会自动传播到异步线程。异步方法如果需要数据库事务，应在异步方法内部显式声明 `@Transactional`。但这会开启一个新的事务边界，它和调用方原事务不是同一个物理事务。

常见问题如下：

| 场景                                      | 风险                         | 推荐处理                           |
| ----------------------------------------- | ---------------------------- | ---------------------------------- |
| 事务内调用异步方法读取刚写入数据          | 主事务未提交，异步线程读不到 | 事务提交后再触发异步               |
| 事务回滚但异步通知已发送                  | 外部系统状态不一致           | 使用 afterCommit、事务消息、Outbox |
| 异步方法内部异常                          | 不会自动回滚调用方事务       | 异步方法内部独立处理事务和补偿     |
| 同类内部调用 `@Async` 或 `@Transactional` | 代理不生效                   | 拆到独立 Bean，通过代理调用        |

文件位置：`src/main/java/io/github/atengk/concurrent/tx/service/AsyncTransactionService.java`

该服务演示异步方法内部开启独立事务。

```java
package io.github.atengk.concurrent.tx.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * 异步事务服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class AsyncTransactionService {

    /**
     * 异步保存操作日志
     *
     * @param bizNo 业务编号
     * @return 保存结果
     */
    @Async("ioTaskExecutor")
    @Transactional(rollbackFor = Exception.class)
    public CompletableFuture<String> saveOperationLog(String bizNo) {
        if (StrUtil.isBlank(bizNo)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("业务编号不能为空"));
        }

        log.info("异步保存操作日志，业务编号：{}", bizNo);

        return CompletableFuture.completedFuture("操作日志保存成功：" + bizNo);
    }
}
```

这个事务只覆盖异步线程中的数据库操作，不覆盖调用方线程中的主业务操作。真正要求主业务和异步任务强一致时，不应使用普通 `@Async`，应考虑本地事务表、事务消息、Outbox Pattern 或最终一致性补偿。

### 事务传播行为

事务传播行为用于定义一个事务方法调用另一个事务方法时，内部方法如何参与事务。Spring 官方文档说明，`PROPAGATION_REQUIRED` 是常见默认模式，会加入当前物理事务或新建事务；`PROPAGATION_REQUIRES_NEW` 会创建独立物理事务，并且外部事务资源仍然绑定，内部事务会获取新连接，因此连接池需要足够大，否则可能耗尽甚至死锁。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html?utm_source=chatgpt.com))

常见传播行为如下：

| 传播行为        | 含义                           | 常见用途               |
| --------------- | ------------------------------ | ---------------------- |
| `REQUIRED`      | 有事务就加入，没有就新建       | 默认业务事务           |
| `REQUIRES_NEW`  | 挂起外部事务，新建独立事务     | 审计日志、失败记录     |
| `NESTED`        | 使用保存点实现嵌套回滚         | JDBC 局部回滚          |
| `SUPPORTS`      | 有事务就加入，没有就非事务执行 | 查询方法               |
| `NOT_SUPPORTED` | 挂起事务，非事务执行           | 大文件处理、非事务查询 |
| `MANDATORY`     | 必须存在事务，否则报错         | 强制由上层控制事务     |
| `NEVER`         | 必须没有事务，否则报错         | 禁止事务场景           |

文件位置：`src/main/java/io/github/atengk/concurrent/tx/service/TransactionPropagationService.java`

该服务演示主事务和独立日志事务的传播行为。

```java
package io.github.atengk.concurrent.tx.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事务传播行为示例服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionPropagationService {

    private final TransactionLogService transactionLogService;

    /**
     * 执行业务操作
     *
     * @param orderNo 订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeBusiness(String orderNo) {
        log.info("开始执行业务操作，订单号：{}", orderNo);

        transactionLogService.saveLogRequiresNew(orderNo, "业务操作开始");

        log.info("业务操作执行完成，订单号：{}", orderNo);
    }
}

/**
 * 独立事务日志服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
class TransactionLogService {

    /**
     * 使用独立事务保存日志
     *
     * @param bizNo 业务编号
     * @param content 日志内容
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void saveLogRequiresNew(String bizNo, String content) {
        log.info("独立事务保存日志，业务编号：{}，内容：{}", bizNo, content);
    }
}
```

`REQUIRES_NEW` 不应滥用。它会额外占用数据库连接，如果外层并发事务很多，而连接池容量不足，可能出现所有线程持有外层连接并等待内层连接的情况。事务传播行为必须和连接池容量一起评估。

### 并发更新问题

并发更新是指多个线程或多个请求同时修改同一条数据。典型问题是丢失更新：两个请求同时读取库存 10，分别扣减 8，最后都认为扣减成功，库存结果错误。

常见解决方案如下：

| 方案           | 适用场景           | 特点                                        |
| -------------- | ------------------ | ------------------------------------------- |
| 数据库原子更新 | 简单库存、余额扣减 | `update stock = stock - ? where stock >= ?` |
| 乐观锁         | 冲突不高，读多写少 | 通过 `version` 判断是否被修改               |
| 悲观锁         | 冲突高，必须串行   | `select ... for update`                     |
| 分布式锁       | 跨实例互斥业务流程 | 依赖 Redis、ZooKeeper 等中间件              |
| 唯一索引       | 防重复创建         | 数据库层兜底最可靠                          |
| 幂等表         | 防重复请求         | 适合支付、回调、消息消费                    |

文件位置：`src/main/java/io/github/atengk/concurrent/tx/mapper/StockMapper.java`

该 Mapper 使用数据库原子更新避免库存扣减超卖。

```java
package io.github.atengk.concurrent.tx.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存 Mapper
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Mapper
public interface StockMapper {

    /**
     * 原子扣减库存
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 影响行数
     */
    @Update("""
            UPDATE product_stock
            SET stock = stock - #{quantity}
            WHERE product_id = #{productId}
              AND stock >= #{quantity}
            """)
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/tx/service/StockDeductService.java`

该服务根据数据库更新影响行数判断扣减是否成功。

```java
package io.github.atengk.concurrent.tx.service;

import io.github.atengk.concurrent.tx.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存扣减服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockDeductService {

    private final StockMapper stockMapper;

    /**
     * 扣减库存
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long productId, Integer quantity) {
        int rows = stockMapper.deductStock(productId, quantity);

        if (rows == 0) {
            log.warn("库存扣减失败，库存不足或并发冲突，商品ID：{}，数量：{}", productId, quantity);
            return false;
        }

        log.info("库存扣减成功，商品ID：{}，数量：{}", productId, quantity);
        return true;
    }
}
```

数据库原子更新是库存扣减的常用基础方案。它比“先查库存再扣减”更安全，因为判断和更新在一条 SQL 中完成。

### 乐观锁

乐观锁适合读多写少、冲突概率不高的场景。它不会在读取时加锁，而是在更新时通过版本号判断数据是否被其他事务修改。MyBatis-Plus 提供 `OptimisticLockerInnerInterceptor` 插件，并要求在实体字段上添加 `@Version` 注解；整数版本字段更新时会按旧版本加一，并在版本不匹配时更新失败。([MyBatis-Plus](https://baomidou.com/plugins/optimistic-locker/?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/tx/config/MybatisPlusConfig.java`

该配置类启用 MyBatis-Plus 乐观锁插件。

```java
package io.github.atengk.concurrent.tx.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 插件
     *
     * @return 插件拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 乐观锁插件，用于防止并发更新覆盖
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/tx/entity/ProductStock.java`

该实体使用 `@Version` 标识乐观锁版本字段。

```java
package io.github.atengk.concurrent.tx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/**
 * 商品库存实体
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@TableName("product_stock")
public class ProductStock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Integer stock;

    @Version
    private Integer version;
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/tx/mapper/ProductStockMapper.java`

该 Mapper 继承 MyBatis-Plus 基础能力。

```java
package io.github.atengk.concurrent.tx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.concurrent.tx.entity.ProductStock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存 Mapper
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Mapper
public interface ProductStockMapper extends BaseMapper<ProductStock> {
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/tx/service/OptimisticStockService.java`

该服务使用乐观锁扣减库存，更新失败时说明发生并发冲突或库存不足。

```java
package io.github.atengk.concurrent.tx.service;

import io.github.atengk.concurrent.tx.entity.ProductStock;
import io.github.atengk.concurrent.tx.mapper.ProductStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 乐观锁库存服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimisticStockService {

    private final ProductStockMapper productStockMapper;

    /**
     * 使用乐观锁扣减库存
     *
     * @param id 库存记录ID
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStockByOptimisticLock(Long id, Integer quantity) {
        ProductStock stock = productStockMapper.selectById(id);

        if (stock == null) {
            log.warn("库存记录不存在，ID：{}", id);
            return false;
        }

        if (stock.getStock() < quantity) {
            log.warn("库存不足，ID：{}，当前库存：{}，扣减数量：{}", id, stock.getStock(), quantity);
            return false;
        }

        stock.setStock(stock.getStock() - quantity);

        int rows = productStockMapper.updateById(stock);
        if (rows == 0) {
            log.warn("乐观锁更新失败，发生并发冲突，ID：{}", id);
            return false;
        }

        log.info("乐观锁扣减库存成功，ID：{}，扣减数量：{}", id, quantity);
        return true;
    }
}
```

乐观锁失败后可以让调用方重试，但不能无限重试。建议限制重试次数，并在高冲突热点数据上改用数据库原子更新、悲观锁或分布式锁。

### 悲观锁

悲观锁适合并发冲突高、必须串行修改的数据场景。常见实现是数据库 `select ... for update`。它会在事务中锁住查询到的行，直到事务提交或回滚。Spring Data JPA 可以通过 `@Lock` 指定锁模式，官方文档说明可在查询方法上添加 `@Lock` 以使用指定的 `LockModeType`。([Home](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html?utm_source=chatgpt.com))

使用 MyBatis 时，可以直接写 `for update`。注意：悲观锁必须在事务内使用，否则锁会很快释放，起不到保护作用。

文件位置：`src/main/java/io/github/atengk/concurrent/tx/mapper/PessimisticStockMapper.java`

该 Mapper 使用 `for update` 查询库存记录。

```java
package io.github.atengk.concurrent.tx.mapper;

import io.github.atengk.concurrent.tx.entity.ProductStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 悲观锁库存 Mapper
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Mapper
public interface PessimisticStockMapper {

    /**
     * 查询库存并加行锁
     *
     * @param id 库存记录ID
     * @return 库存记录
     */
    @Select("""
            SELECT id, product_id, stock, version
            FROM product_stock
            WHERE id = #{id}
            FOR UPDATE
            """)
    ProductStock selectByIdForUpdate(@Param("id") Long id);
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/tx/service/PessimisticStockService.java`

该服务在事务中使用悲观锁扣减库存。

```java
package io.github.atengk.concurrent.tx.service;

import io.github.atengk.concurrent.tx.entity.ProductStock;
import io.github.atengk.concurrent.tx.mapper.PessimisticStockMapper;
import io.github.atengk.concurrent.tx.mapper.ProductStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 悲观锁库存服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PessimisticStockService {

    private final PessimisticStockMapper pessimisticStockMapper;
    private final ProductStockMapper productStockMapper;

    /**
     * 使用悲观锁扣减库存
     *
     * @param id 库存记录ID
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStockByPessimisticLock(Long id, Integer quantity) {
        ProductStock stock = pessimisticStockMapper.selectByIdForUpdate(id);

        if (stock == null) {
            log.warn("库存记录不存在，ID：{}", id);
            return false;
        }

        if (stock.getStock() < quantity) {
            log.warn("库存不足，ID：{}，当前库存：{}，扣减数量：{}", id, stock.getStock(), quantity);
            return false;
        }

        stock.setStock(stock.getStock() - quantity);
        productStockMapper.updateById(stock);

        log.info("悲观锁扣减库存成功，ID：{}，扣减数量：{}", id, quantity);
        return true;
    }
}
```

悲观锁会降低并发能力，锁持有时间必须尽量短。不要在持有数据库行锁期间调用远程 HTTP、发送 MQ、处理大文件或执行复杂计算，否则容易造成锁等待、死锁和连接池耗尽。

### 分布式锁

分布式锁用于多个应用实例之间的互斥控制。例如同一个订单只能被一个实例处理，同一个用户的积分结算不能并发执行，同一个定时任务不能被多个节点重复跑。Redis 分布式锁常用 Redisson 实现。Redisson 官方文档说明，`RLock` 是基于 Redis 的可重入锁，并实现 Java `Lock` 接口；如果持锁实例崩溃，Redisson 会通过 watchdog 机制延长锁过期时间，默认 watchdog 超时时间为 30 秒，也可以通过 `leaseTime` 指定自动释放时间。([Redisson](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

该依赖用于集成 Redisson 分布式锁。

```xml
<dependencies>
    <!-- Redisson Spring Boot Starter，用于 Redis 分布式锁、限流器、信号量等能力 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>3.39.0</version>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

该配置用于连接 Redis。

```yaml
spring:
  data:
    redis:
      # Redis 地址
      host: 127.0.0.1
      # Redis 端口
      port: 6379
      # Redis 密码，没有密码可删除该项
      password:
```

文件位置：`src/main/java/io/github/atengk/concurrent/tx/service/DistributedLockOrderService.java`

该服务使用 Redisson 分布式锁防止同一个订单被并发处理。

```java
package io.github.atengk.concurrent.tx.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁订单服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockOrderService {

    private final RedissonClient redissonClient;

    /**
     * 使用分布式锁处理订单
     *
     * @param orderNo 订单号
     * @return 是否处理成功
     */
    public boolean processOrderWithLock(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            log.warn("订单号不能为空");
            return false;
        }

        String lockKey = "lock:order:process:" + orderNo;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取订单分布式锁失败，订单号：{}", orderNo);
                return false;
            }

            log.info("获取订单分布式锁成功，订单号：{}", orderNo);

            // 执行业务处理，例如检查订单状态、扣减库存、更新订单状态
            doProcessOrder(orderNo);

            log.info("订单处理完成，订单号：{}", orderNo);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取订单分布式锁被中断，订单号：{}", orderNo, e);
            return false;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("订单分布式锁已释放，订单号：{}", orderNo);
            }
        }
    }

    /**
     * 执行订单处理
     *
     * @param orderNo 订单号
     */
    private void doProcessOrder(String orderNo) {
        log.info("执行业务订单处理，订单号：{}", orderNo);
    }
}
```

分布式锁只能保证某段业务逻辑的互斥执行，不能替代数据库唯一索引、事务和幂等控制。关键业务建议组合使用：唯一索引防重复数据，事务保证本地一致性，分布式锁降低并发冲突，幂等表处理重复请求，补偿任务处理异常中断。

这一章节的实践原则是：Web 异步要区分“请求异步返回”和“后台异步任务”；事务要明确线程边界和提交时机；并发更新优先使用数据库原子能力兜底；锁的粒度要小、持有时间要短；跨实例互斥可以用分布式锁，但最终一致性仍要靠事务、幂等和补偿机制闭环。



## 常见开发场景

常见开发场景关注的是并发编程在业务系统中的落地方式。实际项目中，不应只从“能不能异步执行”判断方案，而应同时考虑任务是否允许延后执行、失败是否需要补偿、是否需要事务提交后触发、是否会压垮下游资源，以及是否需要限流、超时和监控。

### 批量任务并发处理

批量任务并发处理适合数据量较大、单条处理相对独立、总体耗时较长的场景，例如批量同步订单、批量补偿失败任务、批量刷新缓存、批量处理用户标签等。核心原则是“分批、限流、隔离、可重试”，不要一次性把全部任务提交到线程池。

批量任务常见错误是直接对全量数据执行 `parallelStream()` 或一次性提交几万条任务。这样会导致线程池队列堆积、内存上涨、数据库连接池耗尽，甚至拖垮下游接口。更稳妥的方式是按批次分页读取，每批内部有限并发处理，处理完一批再处理下一批。

文件位置：`src/main/java/io/github/atengk/concurrent/scenario/BatchTaskService.java`

该服务演示批量任务按批次拆分，并使用 `Semaphore` 控制单批并发度。

```java
package io.github.atengk.concurrent.scenario;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * 批量任务并发处理服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTaskService {

    private final ExecutorService orderAggregateExecutor;

    private final Semaphore semaphore = new Semaphore(10);

    /**
     * 批量处理任务
     *
     * @param taskIds 任务ID列表
     */
    public void processBatchTasks(List<Long> taskIds) {
        List<List<Long>> partitions = ListUtil.partition(taskIds, 100);

        for (List<Long> partition : partitions) {
            log.info("开始处理批次任务，批次大小：{}", partition.size());

            List<CompletableFuture<Void>> futures = partition.stream()
                    .map(taskId -> CompletableFuture.runAsync(() -> processSingleTask(taskId), orderAggregateExecutor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("批次任务处理完成，批次大小：{}", partition.size());
        }
    }

    /**
     * 处理单个任务
     *
     * @param taskId 任务ID
     */
    private void processSingleTask(Long taskId) {
        boolean acquired = false;

        try {
            semaphore.acquire();
            acquired = true;

            log.info("开始处理任务，任务ID：{}", taskId);
            ThreadUtil.sleep(300);
            log.info("任务处理完成，任务ID：{}", taskId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("任务处理被中断，任务ID：{}", taskId, e);
        } catch (Exception e) {
            log.error("任务处理异常，任务ID：{}", taskId, e);
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }
}
```

批量任务要记录处理进度。对于可能失败的任务，建议有任务表字段：`task_id`、`biz_no`、`status`、`retry_count`、`error_msg`、`next_retry_time`、`create_time`、`update_time`。不要只靠内存中的 `CompletableFuture` 状态判断任务是否成功，否则应用重启后无法恢复。

### 多接口并行调用

多接口并行调用适合多个下游接口互不依赖、最终结果需要聚合返回的场景。例如订单详情接口同时查询订单、用户、优惠券、库存、物流。并行调用可以降低整体响应耗时，但会放大下游瞬时压力，因此必须配合线程池隔离、超时控制和降级兜底。

文件位置：`src/main/java/io/github/atengk/concurrent/scenario/MultiApiAggregateService.java`

该服务演示多个接口并行调用，并对每个子任务设置超时和降级结果。

```java
package io.github.atengk.concurrent.scenario;

import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 多接口并行调用聚合服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiApiAggregateService {

    private final ExecutorService orderAggregateExecutor;

    /**
     * 聚合查询订单展示信息
     *
     * @param orderNo 订单号
     * @return 订单展示信息
     */
    public CompletableFuture<String> aggregateOrderView(String orderNo) {
        CompletableFuture<String> orderFuture = queryOrder(orderNo)
                .completeOnTimeout("订单信息暂不可用", 800, TimeUnit.MILLISECONDS)
                .exceptionally(e -> {
                    log.error("查询订单信息异常，订单号：{}", orderNo, e);
                    return "订单信息查询失败";
                });

        CompletableFuture<String> userFuture = queryUser(orderNo)
                .completeOnTimeout("用户信息暂不可用", 800, TimeUnit.MILLISECONDS)
                .exceptionally(e -> {
                    log.error("查询用户信息异常，订单号：{}", orderNo, e);
                    return "用户信息查询失败";
                });

        CompletableFuture<String> logisticsFuture = queryLogistics(orderNo)
                .completeOnTimeout("物流信息暂不可用", 800, TimeUnit.MILLISECONDS)
                .exceptionally(e -> {
                    log.error("查询物流信息异常，订单号：{}", orderNo, e);
                    return "物流信息查询失败";
                });

        return CompletableFuture.allOf(orderFuture, userFuture, logisticsFuture)
                .thenApply(unused -> String.join(" | ",
                        orderFuture.join(),
                        userFuture.join(),
                        logisticsFuture.join()));
    }

    /**
     * 查询订单信息
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    private CompletableFuture<String> queryOrder(String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("查询订单信息，订单号：{}", orderNo);
            ThreadUtil.sleep(300);
            return "订单已支付";
        }, orderAggregateExecutor);
    }

    /**
     * 查询用户信息
     *
     * @param orderNo 订单号
     * @return 用户信息
     */
    private CompletableFuture<String> queryUser(String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("查询用户信息，订单号：{}", orderNo);
            ThreadUtil.sleep(400);
            return "用户等级：VIP";
        }, orderAggregateExecutor);
    }

    /**
     * 查询物流信息
     *
     * @param orderNo 订单号
     * @return 物流信息
     */
    private CompletableFuture<String> queryLogistics(String orderNo) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("查询物流信息，订单号：{}", orderNo);
            ThreadUtil.sleep(600);
            return "物流状态：运输中";
        }, orderAggregateExecutor);
    }
}
```

`CompletableFuture` 支持 `completeOnTimeout()` 和 `orTimeout()`：前者在超时时返回默认值，后者在超时时以 `TimeoutException` 异常完成。对于页面展示类接口，通常使用默认值降级；对于交易类接口，通常让异常向上抛出并中止流程。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html?utm_source=chatgpt.com))

### 消息通知异步发送

消息通知适合异步化，例如短信、邮件、站内信、Webhook、企业微信通知等。但通知任务通常存在失败重试、频率限制和幂等问题。简单通知可以用 `@Async`，关键通知建议写入通知任务表，再由定时任务或 MQ 消费者发送。

文件位置：`src/main/java/io/github/atengk/concurrent/scenario/NotifyTask.java`

该记录类用于表示通知任务。

```java
package io.github.atengk.concurrent.scenario;

/**
 * 通知任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
public record NotifyTask(
        String taskNo,
        String receiver,
        String content
) {
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/scenario/NotifyAsyncService.java`

该服务演示异步发送通知，并在失败时记录必要业务参数。

```java
package io.github.atengk.concurrent.scenario;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 消息通知异步发送服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class NotifyAsyncService {

    /**
     * 异步发送通知
     *
     * @param notifyTask 通知任务
     */
    @Async("ioTaskExecutor")
    public void sendNotify(NotifyTask notifyTask) {
        try {
            if (notifyTask == null || StrUtil.isBlank(notifyTask.receiver())) {
                log.warn("通知任务参数非法，任务：{}", notifyTask);
                return;
            }

            log.info("开始发送通知，任务编号：{}，接收人：{}", notifyTask.taskNo(), notifyTask.receiver());
            ThreadUtil.sleep(500);
            log.info("通知发送完成，任务编号：{}", notifyTask.taskNo());
        } catch (Exception e) {
            log.error("通知发送失败，任务编号：{}，接收人：{}",
                    notifyTask == null ? null : notifyTask.taskNo(),
                    notifyTask == null ? null : notifyTask.receiver(),
                    e);
        }
    }
}
```

通知类任务要注意幂等。短信、邮件、Webhook 一旦发出，数据库事务回滚也无法撤销。因此关键通知建议在主事务提交后再创建通知任务，或者采用本地消息表与补偿机制，避免“业务失败但通知已发送”。

### 定时任务并发执行

Spring 支持 `@Scheduled` 定时任务，需要通过 `@EnableScheduling` 开启定时任务能力。Spring 官方文档说明，`@EnableScheduling` 会启用对 Spring 管理 Bean 中 `@Scheduled` 注解的检测；Spring Framework 同时支持调度和异步方法执行注解。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/EnableScheduling.html?utm_source=chatgpt.com))

默认定时任务容易出现两个问题：第一，单线程调度导致任务互相阻塞；第二，多实例部署时，同一个定时任务可能被多个节点重复执行。单应用内可以配置调度线程池，多实例环境需要引入分布式锁、XXL-JOB、Quartz、ShedLock 或 MQ 任务分发。

文件位置：`src/main/java/io/github/atengk/concurrent/scenario/config/ScheduleConfig.java`

该配置类启用定时任务，并配置独立调度线程池。

```java
package io.github.atengk.concurrent.scenario.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 定时任务配置
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Configuration
@EnableScheduling
public class ScheduleConfig implements SchedulingConfigurer {

    /**
     * 配置定时任务调度器
     *
     * @param taskRegistrar 定时任务注册器
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("schedule-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();

        taskRegistrar.setTaskScheduler(scheduler);

        log.info("定时任务调度线程池初始化完成");
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/scenario/RetryNotifySchedule.java`

该定时任务演示定期扫描失败通知，并把具体发送逻辑交给异步线程池处理，避免调度线程被长时间占用。

```java
package io.github.atengk.concurrent.scenario;

import cn.hutool.core.collection.ListUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通知重试定时任务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryNotifySchedule {

    private final NotifyAsyncService notifyAsyncService;

    /**
     * 扫描失败通知并重试
     */
    @Scheduled(fixedDelay = 30_000)
    public void retryFailedNotify() {
        log.info("开始扫描失败通知任务");

        List<NotifyTask> failedTasks = ListUtil.toList(
                new NotifyTask("NT-10001", "13800000000", "订单通知"),
                new NotifyTask("NT-10002", "13900000000", "支付通知")
        );

        failedTasks.forEach(notifyAsyncService::sendNotify);

        log.info("失败通知任务扫描完成，数量：{}", failedTasks.size());
    }
}
```

Spring Boot 在没有自定义执行器时会自动配置任务执行器；调度器也可以自动配置，非虚拟线程模式下默认使用 `ThreadPoolTaskScheduler`，并可通过 `spring.task.scheduling` 调整线程名前缀和线程池大小。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

### 文件处理异步化

文件处理异步化适合上传后解析、图片压缩、Excel 导入、视频转码、日志文件分析等场景。关键点是：不要把 `MultipartFile` 直接传给异步线程长期处理。请求结束后，临时文件可能被清理。正确做法是先把文件保存到稳定存储，例如本地临时目录、MinIO、OSS，再把文件路径或对象 key 交给异步任务。

文件位置：`src/main/java/io/github/atengk/concurrent/scenario/FileProcessController.java`

该接口接收上传文件，先落盘，再提交异步解析任务。

```java
package io.github.atengk.concurrent.scenario;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件异步处理接口
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileProcessController {

    private final FileProcessAsyncService fileProcessAsyncService;

    /**
     * 上传文件并异步处理
     *
     * @param file 上传文件
     * @return 任务编号
     * @throws Exception 文件保存异常
     */
    @PostMapping("/upload")
    public String upload(@RequestPart("file") MultipartFile file) throws Exception {
        String taskNo = "FILE-" + IdUtil.fastSimpleUUID();
        String originalFilename = file.getOriginalFilename();
        File targetFile = FileUtil.file(System.getProperty("java.io.tmpdir"), taskNo + "-" + originalFilename);

        file.transferTo(targetFile);

        log.info("文件上传成功，任务编号：{}，文件路径：{}", taskNo, targetFile.getAbsolutePath());

        fileProcessAsyncService.processFile(taskNo, targetFile.getAbsolutePath());

        return taskNo;
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/scenario/FileProcessAsyncService.java`

该服务异步处理已落盘文件。

```java
package io.github.atengk.concurrent.scenario;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 文件异步处理服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class FileProcessAsyncService {

    /**
     * 异步处理文件
     *
     * @param taskNo 任务编号
     * @param filePath 文件路径
     */
    @Async("ioTaskExecutor")
    public void processFile(String taskNo, String filePath) {
        try {
            log.info("开始异步处理文件，任务编号：{}，文件路径：{}", taskNo, filePath);

            if (!FileUtil.exist(filePath)) {
                log.warn("文件不存在，任务编号：{}，文件路径：{}", taskNo, filePath);
                return;
            }

            ThreadUtil.sleep(1000);

            long fileSize = FileUtil.size(FileUtil.file(filePath));
            log.info("文件处理完成，任务编号：{}，文件大小：{} bytes", taskNo, fileSize);
        } catch (Exception e) {
            log.error("文件处理失败，任务编号：{}，文件路径：{}", taskNo, filePath, e);
        }
    }
}
```

文件处理通常耗时长、失败概率高，应设计任务状态表。接口返回任务编号后，前端通过任务编号查询处理进度。不要让上传接口一直等待完整解析完成，否则容易出现请求超时和用户体验问题。

### 大量 IO 请求处理

大量 IO 请求通常包括批量 HTTP 调用、数据库查询、Redis 请求、文件读取等。JDK 21 虚拟线程适合大量阻塞式 IO 场景，但虚拟线程不会增加下游资源容量。因此仍然必须使用 `Semaphore`、超时、连接池和降级策略控制并发。

文件位置：`src/main/java/io/github/atengk/concurrent/scenario/MassiveIoService.java`

该服务演示使用虚拟线程处理大量 IO 任务，并用 `Semaphore` 限制实际并发访问数量。

```java
package io.github.atengk.concurrent.scenario;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * 大量 IO 请求处理服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class MassiveIoService {

    private final Semaphore ioSemaphore = new Semaphore(50);

    /**
     * 批量执行 IO 请求
     *
     * @return 请求结果
     */
    public List<String> batchRequest() {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = IntStream.rangeClosed(1, 500)
                    .mapToObj(index -> executorService.submit(() -> requestIo(index)))
                    .toList();

            return futures.stream()
                    .map(this::getResult)
                    .toList();
        }
    }

    /**
     * 执行单个 IO 请求
     *
     * @param index 请求序号
     * @return 请求结果
     */
    private String requestIo(int index) {
        boolean acquired = false;

        try {
            acquired = ioSemaphore.tryAcquire(1, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("IO 请求限流，序号：{}", index);
                return "LIMITED-" + index;
            }

            log.info("开始执行 IO 请求，序号：{}", index);
            ThreadUtil.sleep(300);
            return "SUCCESS-" + index;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("IO 请求被中断，序号：{}", index, e);
            return "INTERRUPTED-" + index;
        } finally {
            if (acquired) {
                ioSemaphore.release();
            }
        }
    }

    /**
     * 获取任务结果
     *
     * @param future 异步结果
     * @return 任务结果
     */
    private String getResult(Future<String> future) {
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取 IO 请求结果被中断", e);
            return "INTERRUPTED";
        } catch (ExecutionException e) {
            log.error("IO 请求执行异常", e);
            return "ERROR";
        } catch (TimeoutException e) {
            log.error("获取 IO 请求结果超时", e);
            return "TIMEOUT";
        }
    }
}
```

大量 IO 场景的控制点不在线程数量本身，而在数据库连接池、HTTP 客户端连接池、Redis 连接池、第三方限流额度和系统内存。虚拟线程可以降低线程等待成本，但不能消除这些资源瓶颈。

## 性能与稳定性

性能与稳定性关注系统在高并发、慢任务、下游异常、队列堆积、锁竞争和线程泄漏时是否能稳定运行。并发代码上线前应具备可观测性、超时控制、拒绝策略、降级策略、线程池隔离和问题排查手段。

### 线程池参数评估

线程池参数评估应从任务类型开始。CPU 密集型任务关注 CPU 核心数和上下文切换；IO 密集型任务关注等待时间、连接池容量和下游吞吐。JDK `ThreadPoolExecutor` 的任务执行策略和核心参数直接决定线程创建、队列排队和拒绝行为。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

常见评估项如下：

| 指标                 | 说明                 | 建议                               |
| -------------------- | -------------------- | ---------------------------------- |
| `activeCount`        | 正在执行任务的线程数 | 长期接近最大线程数说明线程池压力大 |
| `poolSize`           | 当前线程数           | 观察是否频繁扩容                   |
| `queueSize`          | 队列堆积数量         | 长期增长说明处理速度不足           |
| `completedTaskCount` | 完成任务数           | 用于判断吞吐趋势                   |
| 任务平均耗时         | 单个任务执行时间     | 耗时上升通常会造成队列堆积         |
| 拒绝次数             | 触发拒绝策略次数     | 说明系统已过载                     |

文件位置：`src/main/java/io/github/atengk/concurrent/stability/ThreadPoolMonitorService.java`

该服务演示读取 `ThreadPoolTaskExecutor` 的运行指标。

```java
package io.github.atengk.concurrent.stability;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池监控服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThreadPoolMonitorService {

    private final ThreadPoolTaskExecutor ioTaskExecutor;

    /**
     * 定时打印线程池指标
     */
    @Scheduled(fixedDelay = 10_000)
    public void printThreadPoolMetrics() {
        ThreadPoolExecutor executor = ioTaskExecutor.getThreadPoolExecutor();

        log.info("线程池指标，poolSize：{}，activeCount：{}，queueSize：{}，completedTaskCount：{}，taskCount：{}",
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount(),
                executor.getTaskCount());
    }
}
```

评估线程池时，不要只看平均值。应重点观察高峰期、慢接口、下游抖动、任务积压后的恢复速度。线程池参数需要配合压测逐步调整，而不是一次性拍脑袋确定。

### 队列堆积风险

队列堆积表示任务提交速度长期高于任务处理速度。它的风险不是立即失败，而是延迟持续变大、内存持续上升、任务处理时效性下降。JDK `ThreadPoolExecutor` 文档指出，无界队列在任务持续到达且超过处理能力时可能不断增长；有界队列能防止资源耗尽，但需要合理设置队列大小和最大线程数。([Home](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html?utm_source=chatgpt.com))

队列堆积常见原因如下：

| 原因         | 表现                         | 处理方式                       |
| ------------ | ---------------------------- | ------------------------------ |
| 下游接口变慢 | 活跃线程长期占满             | 设置超时、降级、熔断           |
| 任务提交过快 | 队列快速增长                 | 限流、削峰、批量控制           |
| 线程数过小   | CPU 和下游还有余量但队列堆积 | 适当增加核心线程和最大线程     |
| 队列过大     | 请求不失败但延迟很高         | 缩小队列，使用拒绝策略暴露压力 |
| 任务内部死等 | 活跃线程不释放               | 排查锁、连接池和阻塞点         |

文件位置：`src/main/java/io/github/atengk/concurrent/stability/QueueGuardRejectedHandler.java`

该拒绝策略用于在队列满时记录关键状态，并向上抛出异常。

```java
package io.github.atengk.concurrent.stability;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 队列保护拒绝策略
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class QueueGuardRejectedHandler implements RejectedExecutionHandler {

    /**
     * 处理被拒绝的任务
     *
     * @param runnable 任务
     * @param executor 线程池
     */
    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        log.error("线程池队列已满，任务被拒绝，poolSize：{}，activeCount：{}，queueSize：{}，remainingCapacity：{}",
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getQueue().remainingCapacity());

        throw new IllegalStateException("系统繁忙，请稍后重试");
    }
}
```

队列容量建议根据“可接受等待时间”反推，而不是盲目设置很大。例如任务平均处理能力是每秒 200 个，最多允许排队 3 秒，则队列初始容量可以设为 600 左右，再通过压测修正。

### 任务超时与取消

任务超时用于防止慢任务无限占用资源。`Future#get(timeout, unit)` 可以限制等待结果的时间；`Future#cancel()` 用于尝试取消任务。JDK `Future` 文档说明，`Future` 表示异步计算结果，`get()` 会在必要时阻塞等待，取消通过 `cancel` 方法执行。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Future.html?utm_source=chatgpt.com))

`CompletableFuture` 提供 `orTimeout()`、`completeOnTimeout()` 和 `cancel()`。其中 `cancel()` 会让未完成的 `CompletableFuture` 以 `CancellationException` 完成，依赖它的阶段也会异常完成。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/stability/TaskTimeoutService.java`

该服务演示任务超时、取消和中断检查。

```java
package io.github.atengk.concurrent.stability;

import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * 任务超时与取消服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskTimeoutService {

    private final ExecutorService orderAggregateExecutor;

    /**
     * 执行带超时的任务
     *
     * @return 执行结果
     */
    public String executeWithTimeout() {
        Future<String> future = orderAggregateExecutor.submit(this::slowTask);

        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            boolean cancelled = future.cancel(true);
            log.warn("任务执行超时，已尝试取消，取消结果：{}", cancelled);
            return "TIMEOUT";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待任务结果时被中断", e);
            return "INTERRUPTED";
        } catch (ExecutionException e) {
            log.error("任务执行异常", e);
            return "ERROR";
        }
    }

    /**
     * 慢任务
     *
     * @return 执行结果
     */
    private String slowTask() {
        for (int i = 0; i < 10; i++) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("检测到任务中断，停止执行");
                return "CANCELLED";
            }

            log.info("慢任务执行中，进度：{}", i);
            ThreadUtil.sleep(300);
        }

        return "SUCCESS";
    }
}
```

取消任务不等于所有阻塞操作都会立刻停止。数据库查询、HTTP 请求、文件 IO 是否能及时退出，取决于底层客户端是否支持中断和是否设置了超时。因此外部调用必须配置连接超时、读取超时、请求总超时。

### 限流与降级

限流用于控制进入系统或下游资源的请求数量，降级用于在系统压力过大或下游异常时返回可接受的兜底结果。限流解决“不要进来太多”，降级解决“失败时给什么结果”。对于大量 IO、第三方接口、文件处理和高成本计算，限流与降级通常必须同时存在。

文件位置：`src/main/java/io/github/atengk/concurrent/stability/LimitAndFallbackService.java`

该服务使用 `Semaphore` 实现简单并发限流，并在无法获取许可时返回降级结果。

```java
package io.github.atengk.concurrent.stability;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 限流与降级服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class LimitAndFallbackService {

    private final Semaphore remoteCallSemaphore = new Semaphore(20);

    /**
     * 调用受限下游服务
     *
     * @param requestNo 请求编号
     * @return 调用结果
     */
    public String callLimitedRemoteService(String requestNo) {
        boolean acquired = false;

        try {
            acquired = remoteCallSemaphore.tryAcquire(300, TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("下游服务并发达到上限，触发降级，请求编号：{}", requestNo);
                return fallback(requestNo);
            }

            log.info("开始调用下游服务，请求编号：{}", requestNo);
            ThreadUtil.sleep(500);
            return "下游服务调用成功：" + requestNo;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("调用下游服务被中断，请求编号：{}", requestNo, e);
            return fallback(requestNo);
        } catch (Exception e) {
            log.error("调用下游服务异常，请求编号：{}", requestNo, e);
            return fallback(requestNo);
        } finally {
            if (acquired) {
                remoteCallSemaphore.release();
            }
        }
    }

    /**
     * 降级处理
     *
     * @param requestNo 请求编号
     * @return 降级结果
     */
    private String fallback(String requestNo) {
        return "服务繁忙，已返回降级结果：" + requestNo;
    }
}
```

限流建议放在资源入口处，例如 Controller、任务提交入口、远程调用入口。降级结果要结合业务设计：商品推荐可以返回默认推荐，物流状态可以返回“暂不可用”，支付扣款不能随意降级为成功。

### 死锁排查

死锁通常发生在多个线程互相等待对方持有的锁。典型场景是线程 A 持有锁 1 等待锁 2，线程 B 持有锁 2 等待锁 1。JDK `ThreadMXBean` 提供 `findDeadlockedThreads()` 方法，可用于查找等待对象监视器或 ownable synchronizer 的平台线程死锁；文档也说明它不支持检测包含虚拟线程的死锁环。([Oracle 文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.management/java/lang/management/ThreadMXBean.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/stability/DeadlockDemo.java`

该代码用于制造死锁，便于演示排查方式。

```java
package io.github.atengk.concurrent.stability;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 死锁示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class DeadlockDemo {

    private final Object lockA = new Object();

    private final Object lockB = new Object();

    /**
     * 制造死锁
     */
    public void createDeadlock() {
        Thread.ofPlatform().name("deadlock-worker-a").start(() -> {
            synchronized (lockA) {
                log.info("线程A获取 lockA");
                ThreadUtil.sleep(500);

                synchronized (lockB) {
                    log.info("线程A获取 lockB");
                }
            }
        });

        Thread.ofPlatform().name("deadlock-worker-b").start(() -> {
            synchronized (lockB) {
                log.info("线程B获取 lockB");
                ThreadUtil.sleep(500);

                synchronized (lockA) {
                    log.info("线程B获取 lockA");
                }
            }
        });
    }

    public static void main(String[] args) {
        DeadlockDemo demo = new DeadlockDemo();
        demo.createDeadlock();
    }
}
```

文件位置：`src/main/java/io/github/atengk/concurrent/stability/DeadlockCheckService.java`

该服务通过 `ThreadMXBean` 检测平台线程死锁。

```java
package io.github.atengk.concurrent.stability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * 死锁检测服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
public class DeadlockCheckService {

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    /**
     * 定时检测死锁
     */
    @Scheduled(fixedDelay = 30_000)
    public void checkDeadlock() {
        long[] threadIds = threadMXBean.findDeadlockedThreads();

        if (threadIds == null || threadIds.length == 0) {
            log.debug("未检测到死锁");
            return;
        }

        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds, true, true);
        for (ThreadInfo threadInfo : threadInfos) {
            log.error("检测到死锁线程，线程信息：{}", threadInfo);
        }
    }
}
```

常用排查命令如下：

```bash
# 查看 Java 进程
jps -l

# 导出线程栈
jstack -l <pid> > thread-dump.txt

# 使用 jcmd 导出线程信息
jcmd <pid> Thread.print -l > thread-dump.txt

# 连续导出多份线程栈，观察线程状态是否持续不变
jstack -l <pid> > thread-dump-1.txt
sleep 5
jstack -l <pid> > thread-dump-2.txt
sleep 5
jstack -l <pid> > thread-dump-3.txt
```

排查死锁时重点看线程状态、锁对象、等待链路和业务线程名。预防死锁的原则是统一加锁顺序、减少锁粒度、避免持锁调用外部接口、使用 `tryLock` 超时退出。

### 线程泄漏排查

线程泄漏是指线程创建后长期不退出，数量持续增长，最终导致内存、CPU 和系统线程资源耗尽。常见原因包括循环任务没有退出条件、线程池未关闭、定时任务重复创建线程池、阻塞 IO 没有超时、`CompletableFuture` 链路无界提交任务等。

线程泄漏常见表现如下：

| 表现                             | 可能原因                             |
| -------------------------------- | ------------------------------------ |
| JVM 线程数持续增长               | 频繁 `new Thread()` 或重复创建线程池 |
| `pool-*` 线程很多                | 默认线程池命名，无法识别来源         |
| 大量 `WAITING` / `TIMED_WAITING` | 阻塞等待、连接池等待、sleep          |
| 大量业务线程堆积                 | 下游慢、任务无超时、队列过大         |
| 应用关闭很慢                     | 线程池未优雅关闭或非守护线程未退出   |

文件位置：`src/main/java/io/github/atengk/concurrent/stability/ThreadLeakDemo.java`

该代码演示错误做法：每次调用都创建线程池且不关闭。

```java
package io.github.atengk.concurrent.stability;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 线程泄漏错误示例
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public class ThreadLeakDemo {

    /**
     * 错误示例：每次调用都创建线程池且不关闭
     */
    public void wrongCreateExecutorEveryTime() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        executorService.submit(() -> {
            log.info("执行任务");
            ThreadUtil.sleep(1000);
        });
    }
}
```

正确做法是把线程池定义为 Spring Bean 或单例组件，由 Spring 生命周期统一管理。

文件位置：`src/main/java/io/github/atengk/concurrent/stability/SafeExecutorUseService.java`

该服务使用注入的线程池，不在业务方法中重复创建线程池。

```java
package io.github.atengk.concurrent.stability;

import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

/**
 * 安全使用线程池服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SafeExecutorUseService {

    private final ExecutorService orderAggregateExecutor;

    /**
     * 提交业务任务
     *
     * @param taskNo 任务编号
     */
    public void submitTask(String taskNo) {
        orderAggregateExecutor.submit(() -> {
            log.info("开始执行任务，任务编号：{}", taskNo);
            ThreadUtil.sleep(500);
            log.info("任务执行完成，任务编号：{}", taskNo);
        });
    }
}
```

线程泄漏排查命令如下：

```bash
# 查看线程数量
jcmd <pid> Thread.print | grep -E "^\"" | wc -l

# 按线程名粗略统计
jcmd <pid> Thread.print | grep -E "^\"" | awk -F '"' '{print $2}' | sed 's/-[0-9][0-9]*$//' | sort | uniq -c | sort -nr | head

# 查看 JVM 运行时信息
jcmd <pid> VM.info

# 通过 top 查看线程级 CPU
top -H -p <pid>
```

线程泄漏预防原则是：线程池必须有业务化名称；线程池必须统一管理和关闭；任务必须有超时；循环任务必须有退出条件；禁止在循环、请求方法或定时任务中反复创建线程池；出现线程数异常增长时，优先通过线程名定位来源。



## 日志与监控

并发代码的日志与监控重点不是“多打印日志”，而是保证异步任务、线程池、虚拟线程、任务耗时、异常和上下文能够被定位。Spring Boot Actuator 会自动配置 Micrometer，并支持 JVM、系统、日志、任务执行与调度等指标；对于 `ThreadPoolTaskExecutor` 和 `ThreadPoolTaskScheduler`，只要底层 `ThreadPoolExecutor` 可用，Spring Boot 会自动进行指标采集，指标会按执行器 Bean 名称打标签。([Home](https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com))

### 异步任务日志规范

异步任务日志必须包含任务编号、业务编号、线程名称、traceId、任务阶段、耗时和异常堆栈。同步请求日志通常可以通过一次调用链定位问题，但异步任务会切换线程，调用入口、任务执行、任务完成和任务失败可能分散在不同线程中。如果日志缺少业务编号或 traceId，排查会非常困难。

推荐日志字段如下：

| 字段         | 说明         | 示例                         |
| ------------ | ------------ | ---------------------------- |
| `traceId`    | 请求链路 ID  | `REQ-xxx`                    |
| `taskNo`     | 异步任务编号 | `TASK-xxx`                   |
| `bizNo`      | 业务编号     | `ORDER-10001`                |
| `threadName` | 当前线程名   | `order-notify-1`             |
| `stage`      | 任务阶段     | `START`、`SUCCESS`、`FAILED` |
| `costMs`     | 执行耗时     | `152`                        |
| `error`      | 异常堆栈     | 完整异常对象                 |

文件位置：`src/main/java/io/github/atengk/concurrent/log/AsyncLogService.java`

该服务演示异步任务的标准日志格式，包含任务编号、业务编号、线程名称和耗时。

```java
package io.github.atengk.concurrent.log;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步任务日志服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class AsyncLogService {

    /**
     * 异步发送订单通知
     *
     * @param taskNo 任务编号
     * @param orderNo 订单号
     */
    @Async("orderNotifyExecutor")
    public void sendOrderNotify(String taskNo, String orderNo) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String threadName = Thread.currentThread().getName();

        try {
            log.info("异步任务开始，taskNo：{}，orderNo：{}，thread：{}", taskNo, orderNo, threadName);

            ThreadUtil.sleep(500);

            log.info("异步任务成功，taskNo：{}，orderNo：{}，thread：{}，costMs：{}",
                    taskNo, orderNo, threadName, stopWatch.getTotalTimeMillis());
        } catch (Exception e) {
            log.error("异步任务失败，taskNo：{}，orderNo：{}，thread：{}，costMs：{}",
                    taskNo, orderNo, threadName, stopWatch.getTotalTimeMillis(), e);
        }
    }
}
```

日志规范中最重要的是“开始和结束必须成对出现”。如果只有开始日志没有结束日志，通常说明任务卡住、线程中断、应用关闭或异常被吞掉。异步任务失败时必须打印完整异常对象，不要只打印 `e.getMessage()`。

### 线程池运行指标

线程池运行指标用于判断任务是否正在堆积、线程是否被打满、线程池是否频繁扩容、是否触发拒绝策略。Micrometer 的 `ExecutorServiceMetrics` 可以监控 `ExecutorService`，对于 `ThreadPoolExecutor`，它可以提供任务执行耗时、排队耗时、完成任务数、活跃线程数、队列任务数、队列剩余容量、当前线程数、核心线程数和最大线程数等指标。([Micrometer Docs](https://docs.micrometer.io/micrometer/reference/reference/jvm.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

该依赖用于启用 Spring Boot Actuator、Prometheus 指标导出和 Java 21 虚拟线程指标支持。

```xml
<dependencies>
    <!-- Spring Boot Actuator，用于暴露健康检查、指标、线程等运维端点 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Prometheus 指标导出，用于对接 Prometheus + Grafana -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- Java 21 虚拟线程指标支持，用于采集虚拟线程相关指标 -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-java21</artifactId>
    </dependency>
</dependencies>
```

文件位置：`src/main/resources/application.yml`

该配置用于暴露 Actuator 指标端点和 Prometheus 端点。

```yaml
management:
  endpoints:
    web:
      exposure:
        # 生产环境建议按需暴露，并通过网关、内网或鉴权保护
        include: health,info,metrics,prometheus,threaddump
  endpoint:
    health:
      # 展示详细健康信息，生产环境按安全要求调整
      show-details: when_authorized
  metrics:
    tags:
      # 为所有指标增加应用名标签，便于多应用聚合查询
      application: concurrent-demo
```

文件位置：`src/main/java/io/github/atengk/concurrent/log/ThreadPoolMetricLogger.java`

该组件定时打印线程池关键指标，适合在没有接入 Prometheus 前先进行本地排查。

```java
package io.github.atengk.concurrent.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池运行指标日志组件
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThreadPoolMetricLogger {

    private final Map<String, ThreadPoolTaskExecutor> taskExecutorMap;

    /**
     * 定时打印线程池指标
     */
    @Scheduled(fixedDelay = 10_000)
    public void printThreadPoolMetrics() {
        taskExecutorMap.forEach((beanName, taskExecutor) -> {
            ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();

            log.info("线程池指标，executor：{}，poolSize：{}，activeCount：{}，queueSize：{}，remainingCapacity：{}，completedTaskCount：{}，taskCount：{}",
                    beanName,
                    executor.getPoolSize(),
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getQueue().remainingCapacity(),
                    executor.getCompletedTaskCount(),
                    executor.getTaskCount());
        });
    }
}
```

接口验证命令如下：

```bash
# 查看所有指标名称
curl "http://localhost:8080/actuator/metrics"

# 查看 JVM 线程指标
curl "http://localhost:8080/actuator/metrics/jvm.threads.live"

# 查看 Prometheus 格式指标
curl "http://localhost:8080/actuator/prometheus"
```

Spring Boot 的 `/actuator/metrics` 端点默认不会暴露，需要显式配置暴露；访问 `/actuator/metrics` 可以查看可用指标名称，也可以通过 `/actuator/metrics/{meterName}` 查看指定指标。([Home](https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com))

### 任务执行耗时统计

任务耗时统计用于判断异步任务是否变慢、是否存在长尾任务、是否需要调整线程池或下游超时。耗时统计建议使用 Micrometer 的 `Timer`，它可以被 Actuator 和 Prometheus 采集。

文件位置：`src/main/java/io/github/atengk/concurrent/log/TaskMetricsService.java`

该服务使用 Micrometer `Timer` 统计任务耗时。

```java
package io.github.atengk.concurrent.log;

import cn.hutool.core.thread.ThreadUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 任务耗时指标服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskMetricsService {

    private final MeterRegistry meterRegistry;

    /**
     * 执行带耗时统计的任务
     *
     * @param taskType 任务类型
     * @param bizNo 业务编号
     */
    public void executeWithMetrics(String taskType, String bizNo) {
        Timer timer = Timer.builder("async.task.duration")
                .description("异步任务执行耗时")
                .tag("task.type", taskType)
                .publishPercentileHistogram()
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(meterRegistry);

        timer.record(() -> {
            log.info("开始执行异步任务，taskType：{}，bizNo：{}", taskType, bizNo);
            ThreadUtil.sleep(300);
            log.info("异步任务执行完成，taskType：{}，bizNo：{}", taskType, bizNo);
        });
    }
}
```

查询命令如下：

```bash
curl "http://localhost:8080/actuator/metrics/async.task.duration"
curl "http://localhost:8080/actuator/metrics/async.task.duration?tag=task.type:orderNotify"
```

耗时指标建议关注平均值、P95、P99 和最大值。平均值正常不代表系统健康，异步任务常见问题往往体现在长尾耗时上。

### 异常告警

异常告警用于把异步任务失败、线程池拒绝、队列堆积、任务超时等问题及时暴露出来。Spring Boot Actuator 会自动提供日志事件指标，例如 Logback 事件指标；但业务异常告警仍然建议显式记录计数器，便于按任务类型、异常类型和业务域聚合。([Home](https://docs.spring.io/spring-boot/reference/actuator/metrics.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/concurrent/log/AsyncAlertService.java`

该服务使用 Micrometer `Counter` 统计异步任务异常次数。

```java
package io.github.atengk.concurrent.log;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 异步异常告警服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncAlertService {

    private final MeterRegistry meterRegistry;

    /**
     * 记录异步任务异常
     *
     * @param taskType 任务类型
     * @param exceptionType 异常类型
     * @param message 异常消息
     */
    public void recordAsyncError(String taskType, String exceptionType, String message) {
        Counter counter = Counter.builder("async.task.error.total")
                .description("异步任务异常总数")
                .tag("task.type", taskType)
                .tag("exception.type", exceptionType)
                .register(meterRegistry);

        counter.increment();

        log.error("异步任务异常告警，taskType：{}，exceptionType：{}，message：{}",
                taskType, exceptionType, message);
    }
}
```

Prometheus 告警规则可以按业务阈值设计，例如 5 分钟内异步任务异常数超过阈值、线程池队列长度持续大于 80%、线程池拒绝任务次数大于 0、P99 执行耗时超过 SLA。

### JVM 线程分析

JVM 线程分析用于排查死锁、线程泄漏、CPU 飙高、线程池耗尽、虚拟线程 pinning、长时间阻塞等问题。Micrometer 提供 JVM 线程相关指标，包括活跃线程、守护线程、峰值线程以及死锁线程数；Java 21 场景下增加 `micrometer-java21` 后，还可以采集虚拟线程相关指标。([Micrometer Docs](https://docs.micrometer.io/micrometer/reference/reference/jvm.html?utm_source=chatgpt.com))

常用命令如下：

```bash
# 查看 Java 进程
jps -l

# 打印线程栈
jcmd <pid> Thread.print -l > thread-dump.txt

# 使用 jstack 导出线程栈
jstack -l <pid> > thread-dump.txt

# 查看线程级 CPU 占用
top -H -p <pid>

# 把十进制线程 ID 转为十六进制，便于在线程栈中查找 nid
printf "%x\n" <tid>
```

分析线程栈时重点看线程名、线程状态、锁等待、阻塞点和业务方法栈。`RUNNABLE` 不一定代表 CPU 正在执行，也可能是正在执行 native IO；`WAITING` 和 `TIMED_WAITING` 不一定是问题，但如果大量业务线程长期停留在同一个下游调用、锁等待或连接池获取方法上，就需要重点排查。

## 测试与验证

并发代码测试不能只验证“单次调用成功”。还要验证高并发下结果是否正确、异步结果是否最终完成、超时和异常是否按预期触发、线程池是否被正确隔离、任务是否会泄漏、事务边界是否符合预期。

### 单元测试

单元测试用于验证异步方法、工具类、并发控制组件的基本行为。对于异步结果，可以使用 `CompletableFuture#get(timeout, unit)` 或 JUnit 的 `assertTimeout()` 控制测试时间。JUnit 5 的 `assertTimeout` 用于断言代码在指定时间内完成，但它会在调用线程中执行，不会在超时时抢占式中断被测代码。([junit.org](https://junit.org/junit5/docs/5.5.0/api/org/junit/jupiter/api/Assertions.html?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/concurrent/test/CompletableFutureUnitTest.java`

该测试验证异步结果能在指定时间内返回。

```java
package io.github.atengk.concurrent.test;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * CompletableFuture 单元测试
 *
 * @author Ateng
 * @since 2026-05-08
 */
class CompletableFutureUnitTest {

    /**
     * 验证异步任务正常返回
     */
    @Test
    void shouldReturnAsyncResult() {
        assertTimeout(Duration.ofSeconds(2), () -> {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "SUCCESS");
            String result = future.get(1, TimeUnit.SECONDS);
            assertEquals("SUCCESS", result);
        });
    }
}
```

异步单元测试必须设置超时时间。没有超时的测试在异步任务卡死时会阻塞 CI 流水线，导致问题定位困难。

### 并发测试

并发测试用于模拟多个线程同时访问同一个方法，验证线程安全、锁、库存扣减、幂等逻辑是否正确。常用工具包括 `CountDownLatch`、`CyclicBarrier`、`ExecutorService` 和 `AtomicInteger`。测试时通常使用一个统一起跑线，让多个线程尽可能同时开始执行。

文件位置：`src/test/java/io/github/atengk/concurrent/test/ConcurrentCounterTest.java`

该测试使用 `CountDownLatch` 验证并发计数结果。

```java
package io.github.atengk.concurrent.test;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 并发计数测试
 *
 * @author Ateng
 * @since 2026-05-08
 */
class ConcurrentCounterTest {

    /**
     * 验证 AtomicInteger 并发累加结果正确
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldIncrementSafely() throws Exception {
        int threadCount = 20;
        int loopCount = 1000;

        AtomicInteger counter = new AtomicInteger();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();

                        for (int j = 0; j < loopCount; j++) {
                            counter.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            startLatch.countDown();

            boolean completed = finishLatch.await(3, TimeUnit.SECONDS);
            assertEquals(true, completed);
            assertEquals(threadCount * loopCount, counter.get());
        }
    }
}
```

并发测试要多次运行。某些竞态条件不是每次都能复现，可以在测试中增加线程数、循环次数和重复执行次数，但不要让单元测试过慢。高强度并发验证应放到专门的压力测试或基准测试中。

### 压力测试

压力测试用于验证接口在高并发下的吞吐、响应时间、错误率、线程池队列、CPU、内存、GC 和下游连接池表现。JMeter 官方文档建议负载测试使用 CLI 模式，也就是非 GUI 模式，以获得更好的测试效果；常用参数包括 `-n` 指定 CLI 模式、`-t` 指定 JMX 测试计划、`-l` 指定结果文件、`-e -o` 生成报告。([jmeter.apache.org](https://jmeter.apache.org/usermanual/get-started.html?utm_source=chatgpt.com))

常用压测命令如下：

```bash
# 使用 JMeter CLI 模式执行压测，并生成 HTML 报告
jmeter -n \
  -t concurrent-api-test.jmx \
  -l result.jtl \
  -j jmeter.log \
  -e \
  -o report
```

压测期间建议同步观察以下指标：

```bash
# 查看应用指标
curl "http://localhost:8080/actuator/metrics"

# 查看线程 dump
curl "http://localhost:8080/actuator/threaddump"

# 查看 Prometheus 指标
curl "http://localhost:8080/actuator/prometheus"

# 观察 Java 进程线程级 CPU
top -H -p <pid>
```

压测报告不要只看平均响应时间。并发接口应重点看 P95、P99、错误率、吞吐量、线程池队列长度、拒绝次数、下游连接池等待和 GC 暂停时间。

### 线程安全验证

线程安全验证用于证明共享变量、锁、Atomic、并发集合或数据库并发更新逻辑在高并发下结果正确。线程安全测试不能只测试“没有异常”，还要验证最终状态是否符合预期。

文件位置：`src/test/java/io/github/atengk/concurrent/test/InventoryConcurrentTest.java`

该测试模拟并发扣减库存，验证不会超扣。

```java
package io.github.atengk.concurrent.test;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 库存并发安全测试
 *
 * @author Ateng
 * @since 2026-05-08
 */
class InventoryConcurrentTest {

    /**
     * 验证库存不会被超扣
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldNotOverDeductStock() throws Exception {
        AtomicInteger stock = new AtomicInteger(100);
        AtomicInteger successCount = new AtomicInteger();

        int threadCount = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();

                        boolean success = deductStock(stock, 3);
                        if (success) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = finishLatch.await(5, TimeUnit.SECONDS);

            assertTrue(completed);
            assertTrue(stock.get() >= 0);
            assertTrue(successCount.get() <= 33);
        }
    }

    /**
     * CAS 扣减库存
     *
     * @param stock 库存
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    private boolean deductStock(AtomicInteger stock, int quantity) {
        while (true) {
            int current = stock.get();

            if (current < quantity) {
                return false;
            }

            if (stock.compareAndSet(current, current - quantity)) {
                return true;
            }
        }
    }
}
```

对于数据库并发更新，单元测试只能验证部分逻辑。真正的超卖、死锁、锁等待和连接池耗尽问题，需要结合数据库事务、隔离级别和压测环境验证。

### 异步结果验证

异步结果验证要关注最终一致性。异步任务通常不会立即完成，测试时不能简单调用后立刻断言。可以使用 `CompletableFuture#get(timeout)`，也可以使用 Awaitility 等工具等待条件满足。Awaitility 4.x 的官方文档提供了等待条件、断言条件、异常处理和线程处理等用法，适合测试异步状态最终变化。([GitHub](https://github.com/awaitility/awaitility/wiki/Usage?utm_source=chatgpt.com))

文件位置：`pom.xml`

该依赖用于异步结果等待验证。

```xml
<dependencies>
    <!-- Awaitility 用于异步结果、最终一致性和延迟状态验证 -->
    <dependency>
        <groupId>org.awaitility</groupId>
        <artifactId>awaitility</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

文件位置：`src/test/java/io/github/atengk/concurrent/test/AsyncResultVerifyTest.java`

该测试演示异步状态最终完成后的断言方式。

```java
package io.github.atengk.concurrent.test;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 异步结果验证测试
 *
 * @author Ateng
 * @since 2026-05-08
 */
class AsyncResultVerifyTest {

    /**
     * 验证异步任务最终完成
     */
    @Test
    void shouldVerifyAsyncResultEventually() {
        AtomicReference<String> resultHolder = new AtomicReference<>("INIT");

        CompletableFuture.runAsync(() -> resultHolder.set("SUCCESS"));

        await()
                .atMost(Duration.ofSeconds(2))
                .pollInterval(Duration.ofMillis(50))
                .untilAsserted(() -> assertEquals("SUCCESS", resultHolder.get()));
    }
}
```

异步测试的关键是设置最大等待时间和轮询间隔。最大等待时间过短会导致测试不稳定，过长会拖慢 CI；轮询间隔过短会制造额外 CPU 压力。

## 最佳实践

并发编程最佳实践的目标是降低不确定性。线程池要可命名、可隔离、可监控；异步任务要有边界、有超时、有异常处理；虚拟线程要用于合适场景；`ThreadLocal` 要严格控制生命周期；代码 Review 要重点检查共享状态、事务边界和下游资源压力。

### 线程池命名规范

线程池命名必须能表达业务含义。生产环境排查问题时，线程名通常是定位入口。不要使用默认的 `pool-1-thread-1`，否则线程 dump、日志和监控都难以定位来源。

推荐命名规则：

```text
业务域-任务类型-序号

示例：
order-notify-1
report-calc-1
file-parse-1
remote-call-1
schedule-task-1
mvc-async-1
```

配置示例：

```java
executor.setThreadNamePrefix("order-notify-");
```

命名规范建议固定在项目脚手架或公共配置类中，不要让每个开发随意命名。线程池 Bean 名称也要和线程名前缀保持一致，例如 `orderNotifyExecutor` 对应 `order-notify-`。

### 线程池隔离原则

线程池隔离的核心是避免不同任务互相拖垮。CPU 计算、远程接口、文件解析、通知发送、报表导出不应混用同一个线程池。隔离维度应根据资源瓶颈和业务重要性确定，而不是按方法数量机械拆分。

推荐隔离方式：

| 任务类型 | 推荐线程池            | 设计重点                         |
| -------- | --------------------- | -------------------------------- |
| CPU 计算 | `cpuTaskExecutor`     | 小线程数、短队列、避免上下文切换 |
| 远程接口 | `remoteCallExecutor`  | 超时、限流、降级                 |
| 文件处理 | `fileParseExecutor`   | 控制队列、记录进度、失败可重试   |
| 消息通知 | `orderNotifyExecutor` | 事务提交后触发、失败补偿         |
| 报表导出 | `reportCalcExecutor`  | 限制并发、防止拖垮数据库         |

线程池隔离后必须配套监控。如果只拆分线程池但没有指标和告警，问题会从“一个大线程池不可控”变成“多个小线程池不可见”。

### 异步方法设计原则

异步方法设计要避免“看起来成功”。接口返回成功只代表任务提交成功，不代表异步任务执行成功。异步方法必须明确任务状态、失败处理和结果可见性。

建议原则如下：

| 原则         | 说明                                               |
| ------------ | -------------------------------------------------- |
| 明确语义     | 区分“任务已提交”和“任务已完成”                     |
| 参数完整     | 异步方法参数中携带必要业务字段，不强依赖请求上下文 |
| 事务清晰     | 事务提交后再触发外部通知或异步任务                 |
| 异常可见     | 异常必须记录，关键任务要有补偿                     |
| 指定线程池   | 不使用隐式默认线程池处理核心业务                   |
| 设置超时     | 外部调用和异步等待都要有超时                       |
| 避免同类调用 | `@Async` 方法通过 Spring 代理调用才生效            |

异步方法返回值选择建议：

```text
不关心结果、允许失败补偿：void + 完整日志 + 补偿任务
需要组合结果：CompletableFuture<T>
需要可靠执行：任务表 / MQ / 事务消息
需要立即返回最终结果：不要后台异步，使用 Controller 异步或同步等待
```

### 虚拟线程使用建议

虚拟线程适合大量短生命周期阻塞 IO 任务，不适合 CPU 密集型计算，也不应用“虚拟线程池大小”限制并发。JDK 21 的 `Executors.newVirtualThreadPerTaskExecutor()` 是“一任务一虚拟线程”模型，虚拟线程本身不昂贵；限制下游并发应使用 `Semaphore`、限流器或连接池，而不是池化虚拟线程。JDK 文档说明该执行器会为每个任务创建新的虚拟线程。([Micrometer Docs](https://docs.micrometer.io/micrometer/reference/reference/jvm.html?utm_source=chatgpt.com))

建议用法：

```java
try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
    executorService.submit(() -> {
        log.info("使用虚拟线程执行阻塞 IO，virtual：{}", Thread.currentThread().isVirtual());
        return "SUCCESS";
    });
}
```

虚拟线程使用边界：

| 适合                | 不适合                          |
| ------------------- | ------------------------------- |
| 大量 HTTP 调用      | CPU 密集型计算                  |
| 阻塞式数据库查询    | 无限制访问数据库                |
| 文件 IO             | 长生命周期后台线程              |
| Spring MVC 阻塞接口 | 依赖大量 ThreadLocal 缓存重对象 |
| 批量短任务          | 需要队列削峰的任务              |

虚拟线程上线前应重点观察虚拟线程 pinning、数据库连接池等待、HTTP 连接池等待、下游 QPS 和任务超时。Micrometer Java 21 模块可以提供虚拟线程相关指标，包括虚拟线程 pinning 事件以及部分调度器指标。([Micrometer Docs](https://docs.micrometer.io/micrometer/reference/reference/jvm.html?utm_source=chatgpt.com))

### ThreadLocal 使用边界

`ThreadLocal` 适合线程内临时上下文，不适合作为业务数据传递的核心机制。它最大的问题是生命周期不直观，在线程池复用、异步任务、任务编排和测试中容易造成上下文污染。

使用边界如下：

| 场景         | 建议                                    |
| ------------ | --------------------------------------- |
| 请求 traceId | 可用 MDC，但必须清理                    |
| 当前登录用户 | 优先显式传 userId，必要时包装安全上下文 |
| 租户 ID      | 可用 ThreadLocal，但跨线程必须显式传递  |
| 大对象缓存   | 不建议，容易放大内存占用                |
| 异步任务参数 | 不建议依赖 ThreadLocal，应显式传参      |
| 虚拟线程     | 减少 ThreadLocal 使用，避免大量副本     |

标准写法：

```java
try {
    UserContextHolder.setUserId(userId);
    log.info("处理用户请求，用户ID：{}", UserContextHolder.getUserId());
} finally {
    UserContextHolder.clear();
}
```

`ThreadLocal` 的清理必须放在 `finally` 中。只要使用平台线程池，就必须假设线程会被复用；只要线程会被复用，就必须清理上下文。

### 并发代码 Review 要点

并发代码 Review 应重点检查线程、锁、共享变量、异步边界、事务边界、上下文传递、超时和监控。不要只看代码能否编译，还要判断在高并发、慢下游、异常、应用关闭和重复请求下是否仍然可控。

Review 清单如下：

| 检查项   | 重点问题                                           |
| -------- | -------------------------------------------------- |
| 线程池   | 是否命名、隔离、有界队列、拒绝策略、优雅停机       |
| 异步方法 | 是否通过代理调用、是否指定线程池、是否处理异常     |
| 共享变量 | 是否存在竞态条件，是否需要锁或 Atomic              |
| 锁       | 是否可能死锁，是否持锁调用外部接口                 |
| 事务     | 异步任务是否依赖未提交数据，是否存在回滚后通知已发 |
| 超时     | HTTP、数据库、Future、CompletableFuture 是否有超时 |
| 下游保护 | 是否有限流、降级、熔断或信号量                     |
| 上下文   | traceId、userId、tenantId 是否跨线程传递并清理     |
| 监控     | 是否有线程池指标、任务耗时、异常计数               |
| 测试     | 是否有并发测试、异步结果验证、压测记录             |

可以把下面这段作为并发代码合并前的最小门槛：

```text
1. 异步任务必须指定线程池
2. 线程池必须有业务名称、有界队列和拒绝策略
3. 外部调用必须设置超时
4. 异步异常必须记录完整上下文
5. 事务提交前不得发送不可回滚的外部通知
6. ThreadLocal / MDC 必须在 finally 中清理
7. 共享变量必须说明线程安全策略
8. 高并发入口必须有限流或队列容量控制
9. 关键任务必须具备失败补偿或重试机制
10. 上线前必须能通过指标看到线程池、耗时和异常
```

整体上，JDK 21 与 Spring Boot 3 的并发开发应遵循一个基本判断：能同步清晰完成的核心事务不要盲目异步；必须异步的任务要可观测、可补偿、可限流；大量阻塞 IO 可以评估虚拟线程；线程池不是性能魔法，而是资源边界；并发正确性最终要靠测试、压测、监控和故障预案共同保证。
